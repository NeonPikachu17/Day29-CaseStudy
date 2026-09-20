package com.ewb.payment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ewb.payment.config.DataSeeder;
import com.ewb.payment.entity.LedgerEntry;
import com.ewb.payment.entity.LedgerEntryType;
import com.ewb.payment.repository.AccountRepository;
import com.ewb.payment.repository.IdempotencyRecordRepository;
import com.ewb.payment.repository.LedgerEntryRepository;
import com.ewb.payment.repository.TransferRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentServiceTest {

    private static final String ASUNA_MAIN = "EWB-ASU-1001";
    private static final String ASUNA_SAVINGS = "EWB-ASU-2001";
    private static final String KLEIN = "EWB-KLN-4001";
    private static final String HEATHCLIFF = "EWB-HTH-3001";

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired AccountRepository accounts;
    @Autowired LedgerEntryRepository ledger;
    @Autowired TransferRepository transfers;
    @Autowired IdempotencyRecordRepository idempotency;
    @Autowired DataSeeder seeder;

    @BeforeEach
    void resetData() {
        seeder.reset();
    }

    @Test
    void testSuccessfulTransfer_AtomicBalanceAndLedgerUpdate() throws Exception {
        post("key-success", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.reference").value("key-success"));

        assertBalance(ASUNA_MAIN, "15000.00");
        assertBalance(ASUNA_SAVINGS, "6000.00");

        List<LedgerEntry> entries = ledger.findByReference("key-success");
        assertEquals(2, entries.size());
        LedgerEntry debit = entries.stream().filter(e -> e.getEntryType() == LedgerEntryType.DEBIT).findFirst().orElseThrow();
        LedgerEntry credit = entries.stream().filter(e -> e.getEntryType() == LedgerEntryType.CREDIT).findFirst().orElseThrow();
        assertEquals(ASUNA_MAIN, debit.getAccountId());
        assertEquals(ASUNA_SAVINGS, credit.getAccountId());
        assertEquals(0, new BigDecimal("5000.00").compareTo(debit.getAmount()));
        assertEquals(0, new BigDecimal("5000.00").compareTo(credit.getAmount()));
    }

    @Test
    void testInsufficientFunds_KleinAccount_NoLedgerMovement() throws Exception {
        post("key-klein", KLEIN, ASUNA_SAVINGS, "5000.00", false)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_FUNDS"));

        assertBalance(KLEIN, "2000.00");
        assertBalance(ASUNA_SAVINGS, "1000.00");
        assertEquals(0, ledger.count());
        assertEquals(0, transfers.count());
    }

    @Test
    void testFrozenAccount_HeathcliffAccount_Rejected() throws Exception {
        post("key-frozen", HEATHCLIFF, ASUNA_SAVINGS, "1000.00", false)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_FROZEN"));

        assertBalance(HEATHCLIFF, "10000.00");
        assertEquals(0, ledger.count());
    }

    @Test
    void testIdempotency_DuplicateRequest_ReturnsCachedResultWithoutSecondDebit() throws Exception {
        String first = post("key-1", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String second = post("key-1", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(objectMapper.readTree(first), objectMapper.readTree(second), "replay must return the original result");
        assertBalance(ASUNA_MAIN, "15000.00");
        assertBalance(ASUNA_SAVINGS, "6000.00");
        assertEquals(2, ledger.count());
        assertEquals(1, transfers.count());
        assertEquals(1, idempotency.count());
    }

    @Test
    void testIdempotency_KeyReusedWithDifferentAmount_ThrowsConflict() throws Exception {
        post("key-2", ASUNA_MAIN, ASUNA_SAVINGS, "1000.00", false).andExpect(status().isOk());

        post("key-2", ASUNA_MAIN, ASUNA_SAVINGS, "2000.00", false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_MISMATCH"));

        assertBalance(ASUNA_MAIN, "19000.00");
        assertEquals(2, ledger.count());
    }

    @Test
    void testQueryByReference_ReturnsExistingTransfer() throws Exception {
        String body = post("key-6", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false)
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String reference = objectMapper.readTree(body).get("reference").asText();

        mvc.perform(get("/transfers/by-reference/{ref}", reference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(reference))
                .andExpect(jsonPath("$.sourceAccountId").value(ASUNA_MAIN))
                .andExpect(jsonPath("$.destinationAccountId").value(ASUNA_SAVINGS))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        mvc.perform(get("/transfers/by-reference/{ref}", "does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("TRANSFER_NOT_FOUND"));
    }

    @Test
    void testSimulatedTimeout_PaymentCommits_ThenRecoveryFindsSuccess_NoSecondDebit() throws Exception {
        post("key-timeout", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", true)
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.errorCode").value("SIMULATED_TIMEOUT"));

        assertBalance(ASUNA_MAIN, "15000.00");
        assertBalance(ASUNA_SAVINGS, "6000.00");

        mvc.perform(get("/transfers/by-reference/{ref}", "key-timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        post("key-timeout", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false).andExpect(status().isOk());
        assertBalance(ASUNA_MAIN, "15000.00");
        assertEquals(2, ledger.count());
    }

    @Test
    void testInvalidRequests_AreRejectedWithoutLedgerMovement() throws Exception {
        post("key-zero", ASUNA_MAIN, ASUNA_SAVINGS, "0", false).andExpect(status().isBadRequest());
        post("key-negative", ASUNA_MAIN, ASUNA_SAVINGS, "-5", false).andExpect(status().isBadRequest());
        post("key-same", ASUNA_MAIN, ASUNA_MAIN, "10.00", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SAME_ACCOUNT"));
        post("key-missing-acct", ASUNA_MAIN, "EWB-XXX-0000", "10.00", false)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"));

        mvc.perform(MockMvcRequestBuilders.post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(ASUNA_MAIN, ASUNA_SAVINGS, "10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("IDEMPOTENCY_KEY_REQUIRED"));

        assertEquals(0, ledger.count());
        assertBalance(ASUNA_MAIN, "20000.00");
    }

    @Test
    void testConcurrentDuplicateRequests_OnlyOneDebit() throws Exception {
        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                return post("key-concurrent", ASUNA_MAIN, ASUNA_SAVINGS, "5000.00", false)
                        .andReturn().getResponse().getStatus();
            }));
        }
        start.countDown();
        for (Future<Integer> result : results) {
            assertEquals(200, result.get(30, TimeUnit.SECONDS));
        }
        pool.shutdown();

        assertBalance(ASUNA_MAIN, "15000.00");
        assertBalance(ASUNA_SAVINGS, "6000.00");
        assertEquals(1, transfers.count());
        assertEquals(2, ledger.count());
    }

    // ---------- helpers ----------

    private ResultActions post(String key, String source, String destination, String amount, boolean simulateTimeout) throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/transfers")
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body(source, destination, amount));
        if (simulateTimeout) {
            request.header("X-Simulate-Timeout", "true");
        }
        return mvc.perform(request);
    }

    private static String body(String source, String destination, String amount) {
        return """
                {"sourceAccountId":"%s","destinationAccountId":"%s","amount":%s,"currency":"PHP"}
                """.formatted(source, destination, amount);
    }

    private void assertBalance(String accountId, String expected) {
        BigDecimal actual = accounts.findById(accountId).orElseThrow().getBalance();
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                accountId + " expected " + expected + " but was " + actual);
    }
}
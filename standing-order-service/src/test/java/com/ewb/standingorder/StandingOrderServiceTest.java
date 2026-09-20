package com.ewb.standingorder;

import com.ewb.common.dto.CreateStandingOrderRequest;
import com.ewb.common.dto.DueStandingOrderDto;
import com.ewb.common.dto.StandingOrderResponse;
import com.ewb.common.model.Frequency;
import com.ewb.common.model.OrderStatus;
import com.ewb.standingorder.service.StandingOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.config.enabled=false"
})
public class StandingOrderServiceTest {

    @Autowired
    private StandingOrderService service;

    @Test
    void testSeedData_PreSeedsSo9001() {
        StandingOrderResponse order = service.getOrderById("so-9001");
        assertNotNull(order);
        assertEquals("asuna", order.getCustomerId());
        assertEquals("EWB-ASU-1001", order.getSourceAccountId());
        assertEquals("EWB-ASU-2001", order.getDestinationAccountId());
        assertEquals(0, new BigDecimal("5000.00").compareTo(order.getAmount()));
        assertEquals(OrderStatus.ACTIVE, order.getStatus());
        assertEquals(1, order.getVersion());
    }

    @Test
    void testCreateStandingOrder_GeneratesOrderAndVersion() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-ASU-1001");
        req.setDestinationAccountId("EWB-ASU-2001");
        req.setAmount(new BigDecimal("3000.00"));
        req.setCurrency("PHP");
        req.setFrequency(Frequency.MONTHLY);
        req.setDayOfMonth(15);
        req.setExecutionTime("10:00");
        req.setTimeZone("Asia/Manila");
        req.setStartDate("2026-11-15");

        StandingOrderResponse created = service.createStandingOrder(req, "asuna");
        assertNotNull(created);
        assertNotNull(created.getId());
        assertEquals("asuna", created.getCustomerId());
        assertEquals(0, new BigDecimal("3000.00").compareTo(created.getAmount()));
        assertEquals(OrderStatus.ACTIVE, created.getStatus());
        assertEquals(1, created.getVersion());
        assertTrue(created.getNextOccurrence().contains("2026-10-15T10:00"));
    }

    @Test
    void testLifecycle_PauseResumeCancel() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-ASU-1001");
        req.setDestinationAccountId("EWB-ASU-2001");
        req.setAmount(new BigDecimal("1500.00"));

        StandingOrderResponse created = service.createStandingOrder(req, "asuna");
        String id = created.getId();

        StandingOrderResponse paused = service.pauseOrder(id, "asuna");
        assertEquals(OrderStatus.PAUSED, paused.getStatus());

        StandingOrderResponse resumed = service.resumeOrder(id, "asuna");
        assertEquals(OrderStatus.ACTIVE, resumed.getStatus());

        StandingOrderResponse cancelled = service.cancelOrder(id, "asuna");
        assertEquals(OrderStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    void testPatchStandingOrder_IncrementsVersion() {
        CreateStandingOrderRequest req = new CreateStandingOrderRequest();
        req.setSourceAccountId("EWB-ASU-1001");
        req.setDestinationAccountId("EWB-ASU-2001");
        req.setAmount(new BigDecimal("2000.00"));

        StandingOrderResponse created = service.createStandingOrder(req, "asuna");
        String id = created.getId();

        StandingOrderResponse patched = service.patchOrder(id, Map.of("amount", 2500.00, "dayOfMonth", 28), "asuna");
        assertEquals(2, patched.getVersion());
        assertEquals(0, new BigDecimal("2500.00").compareTo(patched.getAmount()));
        assertEquals(28, patched.getDayOfMonth());
    }

    @Test
    void testInternalDueOrders_ReturnsActiveOrders() {
        List<DueStandingOrderDto> due = service.getDueOrders("2026-10-25T01:00:00Z");
        assertNotNull(due);
        assertFalse(due.isEmpty());
        assertTrue(due.stream().anyMatch(d -> "so-9001".equals(d.getStandingOrderId())));
    }
}

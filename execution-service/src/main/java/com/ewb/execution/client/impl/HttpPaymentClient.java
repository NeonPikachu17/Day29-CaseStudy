package com.ewb.execution.client.impl;

import com.ewb.common.dto.TransferRequest;
import com.ewb.common.dto.TransferResponse;
import com.ewb.execution.client.PaymentClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "ewb.clients.mock", havingValue = "false", matchIfMissing = true)
public class HttpPaymentClient implements PaymentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpPaymentClient(RestTemplate restTemplate,
                             @Value("${ewb.services.payment.url:http://localhost:8083}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public TransferResponse submitTransfer(TransferRequest request, String idempotencyKey) {
        String url = baseUrl + "/transfers";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Idempotency-Key", idempotencyKey);
        HttpEntity<TransferRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<TransferResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    TransferResponse.class
            );
            return response.getBody();
        } catch (HttpStatusCodeException ex) {
            // E.g. 422 Unprocessable Entity, 409 Conflict, 504 Timeout
            TransferResponse errorBody = ex.getResponseBodyAs(TransferResponse.class);
            if (errorBody != null) {
                return errorBody;
            }
            throw ex;
        }
    }

    @Override
    public TransferResponse getTransferByReference(String reference) {
        try {
            String url = baseUrl + "/transfers/by-reference/" + reference;
            return restTemplate.getForObject(url, TransferResponse.class);
        } catch (Exception e) {
            return null;
        }
    }
}

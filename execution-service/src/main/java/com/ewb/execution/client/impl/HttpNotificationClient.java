package com.ewb.execution.client.impl;

import com.ewb.common.dto.NotificationEventDto;
import com.ewb.execution.client.NotificationClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@ConditionalOnProperty(name = "ewb.clients.mock", havingValue = "false", matchIfMissing = true)
public class HttpNotificationClient implements NotificationClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public HttpNotificationClient(RestTemplate restTemplate,
                                  @Value("${ewb.services.notification.url:http://localhost:8084}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    @Override
    public void sendNotification(NotificationEventDto eventDto) {
        String url = baseUrl + "/notifications/events";
        restTemplate.postForLocation(url, eventDto);
    }
}

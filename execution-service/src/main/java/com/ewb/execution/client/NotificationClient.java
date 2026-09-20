package com.ewb.execution.client;

import com.ewb.common.dto.NotificationEventDto;

public interface NotificationClient {
    void sendNotification(NotificationEventDto eventDto);
}

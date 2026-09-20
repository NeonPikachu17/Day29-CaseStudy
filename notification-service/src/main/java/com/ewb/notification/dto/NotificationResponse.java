package com.ewb.notification.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationResponse {

    private String status;
    private String deliveryId;
    private String message;

    public NotificationResponse() {
    }

    public NotificationResponse(String status, String deliveryId, String message) {
        this.status = status;
        this.deliveryId = deliveryId;
        this.message = message;
    }

    public static NotificationResponse delivered(String deliveryId) {
        return new NotificationResponse("DELIVERED", deliveryId, null);
    }

    public static NotificationResponse skipped(String message) {
        return new NotificationResponse("SKIPPED", null, message);
    }

    public static NotificationResponse failed(String message) {
        return new NotificationResponse("FAILED", null, message);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

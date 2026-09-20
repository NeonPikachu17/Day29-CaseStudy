package com.ewb.notification.entity;

import com.ewb.common.model.NotificationStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notifications_event_id", columnList = "event_id"),
    @Index(name = "idx_notifications_customer_id", columnList = "customer_id"),
    @Index(name = "idx_notifications_delivery_id", columnList = "delivery_id", unique = true)
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "delivery_id", nullable = false, unique = true, length = 100)
    private String deliveryId;

    @Column(name = "event_id", nullable = false, length = 100)
    private String eventId;

    @Column(name = "execution_id", length = 100)
    private String executionId;

    @Column(name = "standing_order_id", length = 100)
    private String standingOrderId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "event_type", length = 100)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private NotificationStatus status;

    @Column(name = "amount", precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 10)
    private String currency;

    @Column(name = "source_account_id", length = 100)
    private String sourceAccountId;

    @Column(name = "destination_account_id", length = 100)
    private String destinationAccountId;

    @Column(name = "payment_reference", length = 100)
    private String paymentReference;

    @Column(name = "channel", length = 50)
    private String channel;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(String deliveryId, String eventId, String executionId, String standingOrderId,
                        String customerId, String eventType, NotificationStatus status, BigDecimal amount,
                        String currency, String sourceAccountId, String destinationAccountId,
                        String paymentReference, String channel, String message, Instant createdAt) {
        this.deliveryId = deliveryId;
        this.eventId = eventId;
        this.executionId = executionId;
        this.standingOrderId = standingOrderId;
        this.customerId = customerId;
        this.eventType = eventType;
        this.status = status;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.paymentReference = paymentReference;
        this.channel = channel;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId) {
        this.deliveryId = deliveryId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }

    public String getStandingOrderId() {
        return standingOrderId;
    }

    public void setStandingOrderId(String standingOrderId) {
        this.standingOrderId = standingOrderId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(String sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public String getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(String destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

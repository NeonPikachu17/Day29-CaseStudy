package com.ewb.common.dto;

import com.ewb.common.model.OrderStatus;

import java.math.BigDecimal;

public class DueStandingOrderDto {

    private String standingOrderId;
    private String customerId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;
    private String scheduledOccurrenceUtc;
    private Integer version;
    private OrderStatus status;

    public DueStandingOrderDto() {
    }

    public DueStandingOrderDto(String standingOrderId, String customerId, String sourceAccountId,
                               String destinationAccountId, BigDecimal amount, String currency,
                               String scheduledOccurrenceUtc, Integer version, OrderStatus status) {
        this.standingOrderId = standingOrderId;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.scheduledOccurrenceUtc = scheduledOccurrenceUtc;
        this.version = version;
        this.status = status;
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

    public String getScheduledOccurrenceUtc() {
        return scheduledOccurrenceUtc;
    }

    public void setScheduledOccurrenceUtc(String scheduledOccurrenceUtc) {
        this.scheduledOccurrenceUtc = scheduledOccurrenceUtc;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }
}

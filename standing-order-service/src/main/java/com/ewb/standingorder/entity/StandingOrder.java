package com.ewb.standingorder.entity;

import com.ewb.common.model.Frequency;
import com.ewb.common.model.OrderStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "standing_orders")
public class StandingOrder {

    @Id
    private String id;

    private String customerId;
    private String sourceAccountId;
    private String destinationAccountId;
    private BigDecimal amount;
    private String currency;

    @Enumerated(EnumType.STRING)
    private Frequency frequency = Frequency.MONTHLY;

    private Integer dayOfMonth;
    private String executionTime;
    private String timeZone;
    private String startDate;
    private String nextOccurrence;

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.ACTIVE;

    private Integer version = 1;
    private String createdAt;

    public StandingOrder() {
    }

    public StandingOrder(String id, String customerId, String sourceAccountId, String destinationAccountId,
                         BigDecimal amount, String currency, Frequency frequency, Integer dayOfMonth,
                         String executionTime, String timeZone, String startDate, String nextOccurrence,
                         OrderStatus status, Integer version, String createdAt) {
        this.id = id;
        this.customerId = customerId;
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.currency = currency;
        this.frequency = frequency;
        this.dayOfMonth = dayOfMonth;
        this.executionTime = executionTime;
        this.timeZone = timeZone;
        this.startDate = startDate;
        this.nextOccurrence = nextOccurrence;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public String getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(String executionTime) {
        this.executionTime = executionTime;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getNextOccurrence() {
        return nextOccurrence;
    }

    public void setNextOccurrence(String nextOccurrence) {
        this.nextOccurrence = nextOccurrence;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}

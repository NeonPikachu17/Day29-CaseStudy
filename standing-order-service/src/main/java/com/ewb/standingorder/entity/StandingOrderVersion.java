package com.ewb.standingorder.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "standing_order_versions")
public class StandingOrderVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String standingOrderId;
    private Integer version;
    private BigDecimal amount;
    private Integer dayOfMonth;
    private LocalDateTime createdAt;

    public StandingOrderVersion() {
    }

    public StandingOrderVersion(String standingOrderId, Integer version, BigDecimal amount, Integer dayOfMonth, LocalDateTime createdAt) {
        this.standingOrderId = standingOrderId;
        this.version = version;
        this.amount = amount;
        this.dayOfMonth = dayOfMonth;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStandingOrderId() {
        return standingOrderId;
    }

    public void setStandingOrderId(String standingOrderId) {
        this.standingOrderId = standingOrderId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

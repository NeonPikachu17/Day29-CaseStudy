package com.activity.standingorderservice.dto;

import lombok.Data;

@Data
public class CreateStandingOrderRequest {

    private String sourceAccountId;

    private String destinationAccountId;

    private Double amount;

    private String currency;
}
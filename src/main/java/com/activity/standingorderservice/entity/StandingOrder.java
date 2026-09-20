package com.activity.standingorderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "standing_orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandingOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceAccountId;
    private String destinationAccountId;

    private Double amount;

    private String status;
}
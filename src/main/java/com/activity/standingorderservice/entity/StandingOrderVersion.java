package com.activity.standingorderservice.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StandingOrderVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long standingOrderId;

    private Integer version;

    private Double amount;

    private Integer dayOfMonth;
}
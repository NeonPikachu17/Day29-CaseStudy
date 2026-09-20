package com.activity.standingorderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String changedBy;

    private String fieldName;

    private String oldValue;

    private String newValue;

    private LocalDateTime timestamp;
}
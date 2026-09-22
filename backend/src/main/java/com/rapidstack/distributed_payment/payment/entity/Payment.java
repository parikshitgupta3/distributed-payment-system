package com.rapidstack.distributed_payment.payment.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private BigDecimal amount;

    private String currency;

    private String status;

    @Column(name = "idempotency_key", unique = true, nullable = false)
    private String idempotencyKey;

    private LocalDateTime createdAt;
}
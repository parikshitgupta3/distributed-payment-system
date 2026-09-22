package com.rapidstack.distributed_payment.payment.service;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.rapidstack.distributed_payment.payment.entity.OutboxEvent;
import com.rapidstack.distributed_payment.payment.entity.Payment;
import com.rapidstack.distributed_payment.payment.repository.OutboxEventRepository;
import com.rapidstack.distributed_payment.payment.repository.PaymentRepository;

import jakarta.transaction.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentService self;
    private final ObjectMapper objectMapper;
    public PaymentService(PaymentRepository paymentRepository,
                            OutboxEventRepository outboxEventRepository,
                            @Lazy PaymentService self,
                            ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.self = self;
    }
    
    @Transactional
    public Payment createPayment(Payment payment, String idempotencyKey) {
        Payment existing = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            return existing;
        }
        payment.setId(null);
        payment.setStatus("PENDING");
        payment.setIdempotencyKey(idempotencyKey);
        payment.setCreatedAt(LocalDateTime.now());
        try {
            return self.insertNewPayment(payment);
        } catch (DataIntegrityViolationException e) {
            return paymentRepository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }

    public Payment insertNewPayment(Payment payment) {
        Payment saved = paymentRepository.save(payment);
        outboxEventRepository.save(paymentCreatedEvent(saved));
        return saved;
    }

    public Payment getPayment(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found: " + id));
    }

    private OutboxEvent paymentCreatedEvent(Payment payment) {
        String payload = objectMapper.writeValueAsString(Map.of(
                "paymentId", payment.getId(),
                "userId", payment.getUserId(),
                "amount", payment.getAmount(),
                "currency", payment.getCurrency()));
        OutboxEvent event = new OutboxEvent();
        event.setAggregateId(String.valueOf(payment.getId()));
        event.setEventType("PaymentCreated");
        event.setPayload(payload);
        event.setCreatedAt(LocalDateTime.now());
        return event;
    }
}

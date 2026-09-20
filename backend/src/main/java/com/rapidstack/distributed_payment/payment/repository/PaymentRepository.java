package com.rapidstack.distributed_payment.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rapidstack.distributed_payment.payment.entity.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

}

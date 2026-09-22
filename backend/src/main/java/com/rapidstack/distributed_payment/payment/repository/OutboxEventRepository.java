package com.rapidstack.distributed_payment.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rapidstack.distributed_payment.payment.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

}

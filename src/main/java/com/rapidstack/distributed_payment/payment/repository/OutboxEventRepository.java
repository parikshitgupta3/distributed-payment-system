package com.rapidstack.distributed_payment.payment.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rapidstack.distributed_payment.payment.entity.OutboxEvent;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findByProcessedAtIsNull();
}

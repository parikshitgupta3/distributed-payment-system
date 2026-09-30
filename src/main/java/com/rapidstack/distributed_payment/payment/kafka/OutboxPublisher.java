package com.rapidstack.distributed_payment.payment.kafka;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.rapidstack.distributed_payment.payment.entity.OutboxEvent;
import com.rapidstack.distributed_payment.payment.repository.OutboxEventRepository;

@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private static final String TOPIC = "payment-events";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 3000)
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByProcessedAtIsNull();
        for (OutboxEvent event : pending) {
            try {
                ProducerRecord<String, String> record =
                        new ProducerRecord<>(TOPIC, event.getAggregateId(), event.getPayload());
                record.headers().add("eventType", event.getEventType().getBytes(StandardCharsets.UTF_8));

                kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);

                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("Published {} for aggregate {}", event.getEventType(), event.getAggregateId());
            } catch (Exception e) {
                // processedAt stays null, so this event is retried on the next cycle
                log.warn("Failed to publish outbox event id={}: {}", event.getId(), e.getMessage());
            }
        }
    }
}

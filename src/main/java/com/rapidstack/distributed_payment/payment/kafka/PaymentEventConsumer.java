package com.rapidstack.distributed_payment.payment.kafka;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final List<String> receivedPayloads = new CopyOnWriteArrayList<>();

    @KafkaListener(topics = "payment-events")
    public void onPaymentEvent(ConsumerRecord<String, String> record) {
        String eventType = record.headers().lastHeader("eventType") != null
                ? new String(record.headers().lastHeader("eventType").value(), StandardCharsets.UTF_8)
                : "unknown";
        receivedPayloads.add(record.value());
        log.info("Kafka event [{}] key={} payload={}", eventType, record.key(), record.value());
    }

    public List<String> getReceivedPayloads() {
        return receivedPayloads;
    }
}

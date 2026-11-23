package com.hospital.common.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes integration events to Kafka only after the surrounding database transaction commits.
 * If the transaction rolls back, the event is dropped, so consumers never see data that does not exist.
 */
@Component
public class DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(DomainEventPublisher.class);

    private final ApplicationEventPublisher applicationEvents;
    private final KafkaTemplate<String, Object> kafka;

    public DomainEventPublisher(ApplicationEventPublisher applicationEvents, KafkaTemplate<String, Object> kafka) {
        this.applicationEvents = applicationEvents;
        this.kafka = kafka;
    }

    /** Queue an event; it is sent when the current transaction commits (or immediately if there is none). */
    public void publish(String topic, Object key, Object payload) {
        applicationEvents.publishEvent(new OutboundEvent(topic, String.valueOf(key), payload));
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void sendAfterCommit(OutboundEvent event) {
        kafka.send(event.topic(), event.key(), event.payload()).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish to {} key={}: {}", event.topic(), event.key(), ex.getMessage());
            } else {
                log.info("Published {} key={}", event.topic(), event.key());
            }
        });
    }

    public record OutboundEvent(String topic, String key, Object payload) {
    }
}

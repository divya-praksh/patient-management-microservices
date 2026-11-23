package com.hospital.common.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.events.Topics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.kafka.DefaultKafkaProducerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Shared Kafka setup:
 * <ul>
 *   <li>Events are serialized with Spring Boot's ObjectMapper (ISO-8601 dates) and no Java type headers,
 *       so the JSON on the wire matches docs/03-event-contracts.md and any language can consume it.</li>
 *   <li>All topics are declared in one place with 3 partitions each.</li>
 * </ul>
 */
@Configuration
public class KafkaConfig {

    @Bean
    @SuppressWarnings({"unchecked", "rawtypes"})
    public DefaultKafkaProducerFactoryCustomizer jsonEventSerializer(ObjectMapper objectMapper) {
        return factory -> {
            JsonSerializer<Object> serializer = new JsonSerializer<>(objectMapper);
            serializer.setAddTypeInfo(false);
            ((DefaultKafkaProducerFactory) factory).setValueSerializer(serializer);
        };
    }

    @Bean
    public NewTopic patientRegisteredTopic() {
        return topic(Topics.PATIENT_REGISTERED);
    }

    @Bean
    public NewTopic appointmentBookedTopic() {
        return topic(Topics.APPOINTMENT_BOOKED);
    }

    @Bean
    public NewTopic appointmentCancelledTopic() {
        return topic(Topics.APPOINTMENT_CANCELLED);
    }

    @Bean
    public NewTopic appointmentCompletedTopic() {
        return topic(Topics.APPOINTMENT_COMPLETED);
    }

    @Bean
    public NewTopic invoiceCreatedTopic() {
        return topic(Topics.INVOICE_CREATED);
    }

    @Bean
    public NewTopic invoicePaidTopic() {
        return topic(Topics.INVOICE_PAID);
    }

    private static NewTopic topic(String name) {
        return TopicBuilder.name(name).partitions(3).replicas(1).build();
    }
}

package com.hospital.patient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.events.PatientRegisteredEvent;
import com.hospital.common.events.Topics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full slice: HTTP → validation → JPA (H2) → after-commit Kafka publish (embedded broker). */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class PatientApiIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    ObjectMapper mapper;

    @Test
    void registeringPatientReturns201AndPublishesContractEvent() throws Exception {
        String body = """
                {"firstName":"Ravi","lastName":"Kumar","email":"ravi@example.com",
                 "phone":"+91-9000000001","dateOfBirth":"1985-02-20","gender":"MALE"}""";

        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ravi@example.com"));

        try (Consumer<String, String> consumer = consumer()) {
            var record = KafkaTestUtils.getSingleRecord(consumer, Topics.PATIENT_REGISTERED, Duration.ofSeconds(15));
            var event = mapper.readValue(record.value(), PatientRegisteredEvent.class);
            assertThat(event.fullName()).isEqualTo("Ravi Kumar");
            assertThat(record.key()).isEqualTo(String.valueOf(event.patientId()));
            assertThat(record.value()).doesNotContain("__TypeId__").contains("\"occurredAt\":\"");
            assertThat(record.headers().lastHeader("__TypeId__")).isNull();
        }

        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidRequestReturnsProblemDetailWithFieldErrors() throws Exception {
        String body = """
                {"firstName":"","lastName":"X","email":"not-an-email",
                 "phone":"abc","dateOfBirth":"2999-01-01","gender":"FEMALE"}""";

        mvc.perform(post("/api/patients").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.dateOfBirth").exists());
    }

    @Test
    void unknownPatientReturns404() throws Exception {
        mvc.perform(get("/api/patients/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Patient 999999 not found"));
    }

    private Consumer<String, String> consumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps("patient-it", "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumer.subscribe(java.util.List.of(Topics.PATIENT_REGISTERED));
        return consumer;
    }
}

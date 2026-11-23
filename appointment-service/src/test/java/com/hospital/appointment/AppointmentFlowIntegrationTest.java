package com.hospital.appointment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCompletedEvent;
import com.hospital.common.events.Department;
import com.hospital.common.events.Topics;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class AppointmentFlowIntegrationTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    EmbeddedKafkaBroker broker;
    @Autowired
    ObjectMapper mapper;
    @MockBean
    PatientClient patients;

    @Test
    void bookThenCompletePublishesBothEventsKeyedByAppointment() throws Exception {
        when(patients.patientExists(anyLong())).thenReturn(true);
        LocalDateTime slot = LocalDateTime.now().plusDays(5).withHour(10).withMinute(0).withSecond(0).withNano(0);
        String body = """
                {"patientId":1,"doctorName":"Dr. Rao","department":"CARDIOLOGY","scheduledAt":"%s"}"""
                .formatted(slot);

        String created = mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("BOOKED"))
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).get("id").asLong();

        // Same doctor, same slot → 409
        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/appointments/{id}/complete", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Completed visits cannot be cancelled
        mvc.perform(post("/api/appointments/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"late\"}"))
                .andExpect(status().isConflict());

        try (Consumer<String, String> consumer = consumer(Topics.APPOINTMENT_BOOKED, Topics.APPOINTMENT_COMPLETED)) {
            var booked = KafkaTestUtils.getSingleRecord(consumer, Topics.APPOINTMENT_BOOKED, Duration.ofSeconds(15));
            var bookedEvent = mapper.readValue(booked.value(), AppointmentBookedEvent.class);
            assertThat(booked.key()).isEqualTo(String.valueOf(id));
            assertThat(bookedEvent.scheduledAt()).isEqualTo(slot);
            assertThat(bookedEvent.department()).isEqualTo(Department.CARDIOLOGY);

            var completed = KafkaTestUtils.getSingleRecord(consumer, Topics.APPOINTMENT_COMPLETED, Duration.ofSeconds(15));
            assertThat(mapper.readValue(completed.value(), AppointmentCompletedEvent.class).appointmentId()).isEqualTo(id);
        }
    }

    @Test
    void pastSlotFailsValidationAndUnknownPatientReturns422() throws Exception {
        when(patients.patientExists(anyLong())).thenReturn(false);

        mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"patientId":1,"doctorName":"Dr. Rao","department":"CARDIOLOGY","scheduledAt":"2020-01-01T10:00:00"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.scheduledAt").exists());

        String future = LocalDateTime.now().plusDays(2).withNano(0).toString();
        String response = mvc.perform(post("/api/appointments").contentType(MediaType.APPLICATION_JSON).content("""
                        {"patientId":404,"doctorName":"Dr. Iyer","department":"NEUROLOGY","scheduledAt":"%s"}"""
                        .formatted(future)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();
        JsonNode problem = mapper.readTree(response);
        assertThat(problem.get("detail").asText()).contains("Patient 404");
    }

    private Consumer<String, String> consumer(String... topics) {
        Map<String, Object> props = KafkaTestUtils.consumerProps("appointment-it", "true", broker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new StringDeserializer()).createConsumer();
        consumer.subscribe(java.util.List.of(topics));
        return consumer;
    }
}

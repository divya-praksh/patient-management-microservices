package com.hospital.notification;

import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.Department;
import com.hospital.common.events.InvoiceEvent;
import com.hospital.common.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class NotificationIntegrationTest {

    @Autowired
    KafkaTemplate<String, Object> kafka;
    @Autowired
    NotificationRepository repository;
    @Autowired
    MockMvc mvc;

    @Test
    void sameEventDeliveredTwiceNotifiesPatientOnce() throws Exception {
        long patientId = 77L;
        var booked = AppointmentBookedEvent.of(5L, patientId, "Dr. Rao", Department.CARDIOLOGY,
                LocalDateTime.of(2026, 12, 1, 10, 0));
        var invoice = InvoiceEvent.of(3L, 5L, patientId, new BigDecimal("1500.00"));

        kafka.send(Topics.APPOINTMENT_BOOKED, "5", booked);
        kafka.send(Topics.APPOINTMENT_BOOKED, "5", booked); // duplicate delivery
        kafka.send(Topics.INVOICE_CREATED, "5", invoice);

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(repository.findByPatientIdOrderByCreatedAtDescIdDesc(patientId)).hasSize(2));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(repository.findByPatientIdOrderByCreatedAtDescIdDesc(patientId)).hasSize(2));

        mvc.perform(get("/api/notifications").param("patientId", String.valueOf(patientId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.eventType=='APPOINTMENT_BOOKED')].message")
                        .value(org.hamcrest.Matchers.hasItem(containsString("Dr. Rao"))))
                .andExpect(jsonPath("$[?(@.eventType=='INVOICE_CREATED')].message")
                        .value(org.hamcrest.Matchers.hasItem(containsString("INR 1500.00"))));
    }
}

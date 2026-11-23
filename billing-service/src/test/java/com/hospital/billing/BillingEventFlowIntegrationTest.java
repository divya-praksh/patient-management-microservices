package com.hospital.billing;

import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.Department;
import com.hospital.common.events.Topics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Consumes real Kafka messages (embedded broker) and checks billing's consistency guarantees. */
@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
class BillingEventFlowIntegrationTest {

    private static final AtomicLong IDS = new AtomicLong(1000);

    @Autowired
    KafkaTemplate<String, Object> kafka;
    @Autowired
    InvoiceRepository invoices;
    @Autowired
    MockMvc mvc;

    @Test
    void duplicateBookingEventsCreateExactlyOneInvoice() throws Exception {
        long appointmentId = IDS.incrementAndGet();
        var booked = AppointmentBookedEvent.of(appointmentId, 1L, "Dr. Rao", Department.CARDIOLOGY,
                LocalDateTime.now().plusDays(1));

        kafka.send(Topics.APPOINTMENT_BOOKED, String.valueOf(appointmentId), booked);
        kafka.send(Topics.APPOINTMENT_BOOKED, String.valueOf(appointmentId), booked); // redelivery

        Invoice invoice = awaitInvoice(appointmentId, Invoice.Status.PENDING);
        assertThat(invoice.getAmount()).isEqualByComparingTo("1500.00");

        // Give the duplicate time to be processed, then confirm there is still exactly one invoice.
        Thread.sleep(1500);
        assertThat(invoices.findAll().stream().filter(i -> i.getAppointmentId() == appointmentId)).hasSize(1);

        mvc.perform(post("/api/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(post("/api/invoices/{id}/pay", invoice.getId()))
                .andExpect(status().isConflict());

        // Cancelling after payment must flag a refund, not lose the money.
        kafka.send(Topics.APPOINTMENT_CANCELLED, String.valueOf(appointmentId),
                AppointmentCancelledEvent.of(appointmentId, 1L, "Doctor unavailable"));
        awaitInvoice(appointmentId, Invoice.Status.REFUND_DUE);
    }

    @Test
    void cancellationArrivingBeforeBookingPreventsPayableInvoice() {
        long appointmentId = IDS.incrementAndGet();

        kafka.send(Topics.APPOINTMENT_CANCELLED, String.valueOf(appointmentId),
                AppointmentCancelledEvent.of(appointmentId, 2L, "Patient request"));
        awaitInvoice(appointmentId, Invoice.Status.VOID);

        kafka.send(Topics.APPOINTMENT_BOOKED, String.valueOf(appointmentId), AppointmentBookedEvent.of(
                appointmentId, 2L, "Dr. Iyer", Department.NEUROLOGY, LocalDateTime.now().plusDays(1)));

        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(invoices.findByAppointmentId(appointmentId))
                        .get().extracting(Invoice::getStatus).isEqualTo(Invoice.Status.VOID));
    }

    private Invoice awaitInvoice(long appointmentId, Invoice.Status expected) {
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(invoices.findByAppointmentId(appointmentId))
                        .get().extracting(Invoice::getStatus).isEqualTo(expected));
        return invoices.findByAppointmentId(appointmentId).orElseThrow();
    }
}

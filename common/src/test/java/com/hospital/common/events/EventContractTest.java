package com.hospital.common.events;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Contract tests: the JSON documented in docs/03-event-contracts.md must round-trip through the shared records. */
class EventContractTest {

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    void appointmentBookedRoundTrips() throws Exception {
        var original = AppointmentBookedEvent.of(7L, 1L, "Dr. Rao", Department.CARDIOLOGY,
                LocalDateTime.of(2026, 12, 1, 10, 0));
        var json = mapper.writeValueAsString(original);

        assertThat(json).contains("\"scheduledAt\":\"2026-12-01T10:00:00\"", "\"department\":\"CARDIOLOGY\"");
        assertThat(mapper.readValue(json, AppointmentBookedEvent.class)).isEqualTo(original);
    }

    @Test
    void consumersIgnoreFieldsAddedLater() throws Exception {
        String futureVersion = """
                {"eventId":"3f1c2a7e-8a4b-4b8e-9a8f-1d2c3b4a5e6f","occurredAt":"2026-11-23T09:00:00Z",
                 "appointmentId":7,"patientId":1,"department":"NEUROLOGY","roomNumber":"B-12"}
                """;
        var event = mapper.readValue(futureVersion, AppointmentCompletedEvent.class);

        assertThat(event.department()).isEqualTo(Department.NEUROLOGY);
        assertThat(event.appointmentId()).isEqualTo(7L);
    }

    @Test
    void invoiceAmountKeepsValue() throws Exception {
        var json = mapper.writeValueAsString(InvoiceEvent.of(3L, 7L, 1L, new BigDecimal("1500.00")));
        assertThat(mapper.readValue(json, InvoiceEvent.class).amount()).isEqualByComparingTo("1500.00");
    }
}

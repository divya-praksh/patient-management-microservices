package com.hospital.common.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Payload for both {@link Topics#INVOICE_CREATED} and {@link Topics#INVOICE_PAID}. */
public record InvoiceEvent(UUID eventId, Instant occurredAt,
                           Long invoiceId, Long appointmentId, Long patientId, BigDecimal amount) {

    public static InvoiceEvent of(Long invoiceId, Long appointmentId, Long patientId, BigDecimal amount) {
        return new InvoiceEvent(UUID.randomUUID(), Instant.now(), invoiceId, appointmentId, patientId, amount);
    }
}

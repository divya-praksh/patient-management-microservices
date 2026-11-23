package com.hospital.common.events;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelledEvent(UUID eventId, Instant occurredAt,
                                        Long appointmentId, Long patientId, String reason) {

    public static AppointmentCancelledEvent of(Long appointmentId, Long patientId, String reason) {
        return new AppointmentCancelledEvent(UUID.randomUUID(), Instant.now(), appointmentId, patientId, reason);
    }
}

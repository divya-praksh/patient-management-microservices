package com.hospital.common.events;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCompletedEvent(UUID eventId, Instant occurredAt,
                                        Long appointmentId, Long patientId, Department department) {

    public static AppointmentCompletedEvent of(Long appointmentId, Long patientId, Department department) {
        return new AppointmentCompletedEvent(UUID.randomUUID(), Instant.now(), appointmentId, patientId, department);
    }
}

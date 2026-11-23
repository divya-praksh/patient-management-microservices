package com.hospital.common.events;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record AppointmentBookedEvent(UUID eventId, Instant occurredAt,
                                     Long appointmentId, Long patientId, String doctorName,
                                     Department department, LocalDateTime scheduledAt) {

    public static AppointmentBookedEvent of(Long appointmentId, Long patientId, String doctorName,
                                            Department department, LocalDateTime scheduledAt) {
        return new AppointmentBookedEvent(UUID.randomUUID(), Instant.now(),
                appointmentId, patientId, doctorName, department, scheduledAt);
    }
}

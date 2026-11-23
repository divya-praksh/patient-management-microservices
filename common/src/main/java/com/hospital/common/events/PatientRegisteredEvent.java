package com.hospital.common.events;

import java.time.Instant;
import java.util.UUID;

public record PatientRegisteredEvent(UUID eventId, Instant occurredAt,
                                     Long patientId, String fullName, String email) {

    public static PatientRegisteredEvent of(Long patientId, String fullName, String email) {
        return new PatientRegisteredEvent(UUID.randomUUID(), Instant.now(), patientId, fullName, email);
    }
}

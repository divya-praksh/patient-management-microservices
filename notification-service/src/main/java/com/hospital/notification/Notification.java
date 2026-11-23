package com.hospital.notification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification", indexes = @Index(name = "idx_notification_patient", columnList = "patientId"))
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique: the same event delivered twice can never produce two messages to the patient. */
    @Column(nullable = false, unique = true)
    private UUID eventId;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String channel = "EMAIL";

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Notification() {
    }

    public Notification(UUID eventId, Long patientId, String eventType, String message) {
        this.eventId = eventId;
        this.patientId = patientId;
        this.eventType = eventType;
        this.message = message;
    }

    public Long getId() { return id; }
    public UUID getEventId() { return eventId; }
    public Long getPatientId() { return patientId; }
    public String getEventType() { return eventType; }
    public String getChannel() { return channel; }
    public String getMessage() { return message; }
    public Instant getCreatedAt() { return createdAt; }
}

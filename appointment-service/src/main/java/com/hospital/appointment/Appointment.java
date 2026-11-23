package com.hospital.appointment;

import com.hospital.common.events.Department;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "appointment", indexes = {
        @Index(name = "idx_appointment_patient", columnList = "patientId"),
        @Index(name = "idx_appointment_doctor_time", columnList = "doctorName,scheduledAt")
})
public class Appointment {

    public enum Status { BOOKED, CANCELLED, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private String doctorName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Department department;

    @Column(nullable = false)
    private LocalDateTime scheduledAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.BOOKED;

    private String cancelReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Optimistic locking: two concurrent cancel/complete requests cannot both succeed. */
    @Version
    private Long version;

    protected Appointment() {
    }

    public Appointment(Long patientId, String doctorName, Department department, LocalDateTime scheduledAt) {
        this.patientId = patientId;
        this.doctorName = doctorName;
        this.department = department;
        this.scheduledAt = scheduledAt;
    }

    public void cancel(String reason) {
        requireBooked("cancel");
        this.status = Status.CANCELLED;
        this.cancelReason = reason;
    }

    public void complete() {
        requireBooked("complete");
        this.status = Status.COMPLETED;
    }

    private void requireBooked(String action) {
        if (status != Status.BOOKED) {
            throw new IllegalStateException("Cannot " + action + " appointment " + id + " in status " + status);
        }
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public String getDoctorName() { return doctorName; }
    public Department getDepartment() { return department; }
    public LocalDateTime getScheduledAt() { return scheduledAt; }
    public Status getStatus() { return status; }
    public String getCancelReason() { return cancelReason; }
    public Instant getCreatedAt() { return createdAt; }
}

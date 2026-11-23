package com.hospital.billing;

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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice", indexes = @Index(name = "idx_invoice_patient", columnList = "patientId"))
public class Invoice {

    public enum Status { PENDING, PAID, VOID, REFUND_DUE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique: guarantees at most one invoice per appointment, even under duplicate event delivery. */
    @Column(nullable = false, unique = true)
    private Long appointmentId;

    @Column(nullable = false)
    private Long patientId;

    /** Null only for a placeholder created when a cancellation arrived before the booking. */
    @Enumerated(EnumType.STRING)
    private Department department;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    private Instant paidAt;

    @Version
    private Long version;

    protected Invoice() {
    }

    private Invoice(Long appointmentId, Long patientId, Department department, BigDecimal amount, Status status) {
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.department = department;
        this.amount = amount;
        this.status = status;
    }

    public static Invoice pending(Long appointmentId, Long patientId, Department department, BigDecimal amount) {
        return new Invoice(appointmentId, patientId, department, amount, Status.PENDING);
    }

    /** Records that the appointment was cancelled before its booking event was processed. */
    public static Invoice voidPlaceholder(Long appointmentId, Long patientId) {
        return new Invoice(appointmentId, patientId, null, BigDecimal.ZERO, Status.VOID);
    }

    public void pay() {
        if (status != Status.PENDING) {
            throw new IllegalStateException("Invoice " + id + " cannot be paid in status " + status);
        }
        status = Status.PAID;
        paidAt = Instant.now();
    }

    /**
     * Applies an appointment cancellation. PENDING becomes VOID; PAID becomes REFUND_DUE (money is never
     * silently dropped). Returns false if the cancellation had already been applied.
     */
    public boolean applyCancellation() {
        switch (status) {
            case PENDING -> status = Status.VOID;
            case PAID -> status = Status.REFUND_DUE;
            default -> {
                return false;
            }
        }
        return true;
    }

    public Long getId() { return id; }
    public Long getAppointmentId() { return appointmentId; }
    public Long getPatientId() { return patientId; }
    public Department getDepartment() { return department; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getPaidAt() { return paidAt; }
}

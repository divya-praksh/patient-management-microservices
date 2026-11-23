package com.hospital.common.events;

/** Kafka topic names. The single source of truth for every producer and consumer. */
public final class Topics {
    public static final String PATIENT_REGISTERED = "patient.registered";
    public static final String APPOINTMENT_BOOKED = "appointment.booked";
    public static final String APPOINTMENT_CANCELLED = "appointment.cancelled";
    public static final String APPOINTMENT_COMPLETED = "appointment.completed";
    public static final String INVOICE_CREATED = "invoice.created";
    public static final String INVOICE_PAID = "invoice.paid";

    private Topics() {
    }
}

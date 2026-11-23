package com.hospital.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.AppointmentCompletedEvent;
import com.hospital.common.events.InvoiceEvent;
import com.hospital.common.events.PatientRegisteredEvent;
import com.hospital.common.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Turns every hospital event into a patient-facing message. */
@Component
public class HospitalEventsListener {

    private static final Logger log = LoggerFactory.getLogger(HospitalEventsListener.class);

    private final NotificationService notifications;
    private final ObjectMapper mapper;

    public HospitalEventsListener(NotificationService notifications, ObjectMapper mapper) {
        this.notifications = notifications;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.PATIENT_REGISTERED)
    public void onPatientRegistered(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, PatientRegisteredEvent.class);
        send(e.eventId(), e.patientId(), "PATIENT_REGISTERED",
                "Welcome " + e.fullName() + ", your patient ID is " + e.patientId() + ".");
    }

    @KafkaListener(topics = Topics.APPOINTMENT_BOOKED)
    public void onAppointmentBooked(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, AppointmentBookedEvent.class);
        send(e.eventId(), e.patientId(), "APPOINTMENT_BOOKED",
                "Your " + e.department() + " appointment with " + e.doctorName()
                        + " is confirmed for " + e.scheduledAt() + ".");
    }

    @KafkaListener(topics = Topics.APPOINTMENT_CANCELLED)
    public void onAppointmentCancelled(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, AppointmentCancelledEvent.class);
        send(e.eventId(), e.patientId(), "APPOINTMENT_CANCELLED",
                "Appointment #" + e.appointmentId() + " was cancelled. Reason: " + e.reason());
    }

    @KafkaListener(topics = Topics.APPOINTMENT_COMPLETED)
    public void onAppointmentCompleted(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, AppointmentCompletedEvent.class);
        send(e.eventId(), e.patientId(), "APPOINTMENT_COMPLETED",
                "Thank you for visiting " + e.department() + " today (appointment #" + e.appointmentId() + ").");
    }

    @KafkaListener(topics = Topics.INVOICE_CREATED)
    public void onInvoiceCreated(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, InvoiceEvent.class);
        send(e.eventId(), e.patientId(), "INVOICE_CREATED",
                "Invoice #" + e.invoiceId() + " for INR " + e.amount() + " has been issued.");
    }

    @KafkaListener(topics = Topics.INVOICE_PAID)
    public void onInvoicePaid(String payload) throws JsonProcessingException {
        var e = mapper.readValue(payload, InvoiceEvent.class);
        send(e.eventId(), e.patientId(), "INVOICE_PAID",
                "Payment of INR " + e.amount() + " received for invoice #" + e.invoiceId() + ". Thank you.");
    }

    private void send(UUID eventId, Long patientId, String type, String message) {
        try {
            notifications.notify(eventId, patientId, type, message);
        } catch (DataIntegrityViolationException ex) {
            log.info("Concurrent duplicate of event {} rejected by unique key", eventId);
        }
    }
}

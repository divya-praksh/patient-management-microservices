package com.hospital.billing;

import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.InvoiceEvent;
import com.hospital.common.events.Topics;
import com.hospital.common.messaging.DomainEventPublisher;
import com.hospital.common.web.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);

    private final InvoiceRepository repository;
    private final FeeSchedule fees;
    private final DomainEventPublisher events;

    public BillingService(InvoiceRepository repository, FeeSchedule fees, DomainEventPublisher events) {
        this.repository = repository;
        this.fees = fees;
        this.events = events;
    }

    /**
     * Creates the consultation invoice for a new booking. Idempotent: a redelivered event, or a booking whose
     * cancellation was processed first, finds an existing invoice and changes nothing.
     */
    @Transactional
    public Optional<Invoice> onAppointmentBooked(AppointmentBookedEvent event) {
        Optional<Invoice> existing = repository.findByAppointmentId(event.appointmentId());
        if (existing.isPresent()) {
            log.info("Invoice for appointment {} already exists (status {}); ignoring event {}",
                    event.appointmentId(), existing.get().getStatus(), event.eventId());
            return Optional.empty();
        }
        Invoice invoice = repository.save(Invoice.pending(event.appointmentId(), event.patientId(),
                event.department(), fees.feeFor(event.department())));
        events.publish(Topics.INVOICE_CREATED, invoice.getAppointmentId(), toEvent(invoice));
        return Optional.of(invoice);
    }

    /**
     * booked and cancelled travel on different topics, so Kafka does not guarantee their order. If the cancel
     * arrives first, a VOID placeholder is stored so the late booking event cannot create a payable invoice.
     */
    @Transactional
    public Invoice onAppointmentCancelled(AppointmentCancelledEvent event) {
        return repository.findByAppointmentId(event.appointmentId())
                .map(invoice -> {
                    if (invoice.applyCancellation()) {
                        log.info("Appointment {} cancelled: invoice {} is now {}",
                                event.appointmentId(), invoice.getId(), invoice.getStatus());
                    }
                    return invoice;
                })
                .orElseGet(() -> {
                    log.warn("Cancellation for appointment {} arrived before its booking; storing VOID placeholder",
                            event.appointmentId());
                    return repository.save(Invoice.voidPlaceholder(event.appointmentId(), event.patientId()));
                });
    }

    @Transactional
    public Invoice pay(Long invoiceId) {
        Invoice invoice = get(invoiceId);
        try {
            invoice.pay();
        } catch (IllegalStateException e) {
            throw ApiException.conflict(e.getMessage());
        }
        events.publish(Topics.INVOICE_PAID, invoice.getAppointmentId(), toEvent(invoice));
        return invoice;
    }

    @Transactional(readOnly = true)
    public Invoice get(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Invoice " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public Invoice getByAppointment(Long appointmentId) {
        return repository.findByAppointmentId(appointmentId)
                .orElseThrow(() -> ApiException.notFound("No invoice for appointment " + appointmentId));
    }

    @Transactional(readOnly = true)
    public List<Invoice> forPatient(Long patientId) {
        return repository.findByPatientIdOrderByCreatedAtDesc(patientId);
    }

    private static InvoiceEvent toEvent(Invoice i) {
        return InvoiceEvent.of(i.getId(), i.getAppointmentId(), i.getPatientId(), i.getAmount());
    }
}

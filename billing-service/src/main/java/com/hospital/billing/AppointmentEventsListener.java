package com.hospital.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.Topics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AppointmentEventsListener {

    private static final Logger log = LoggerFactory.getLogger(AppointmentEventsListener.class);

    private final BillingService billing;
    private final ObjectMapper mapper;

    public AppointmentEventsListener(BillingService billing, ObjectMapper mapper) {
        this.billing = billing;
        this.mapper = mapper;
    }

    @KafkaListener(topics = Topics.APPOINTMENT_BOOKED)
    public void onBooked(String payload) throws JsonProcessingException {
        AppointmentBookedEvent event = mapper.readValue(payload, AppointmentBookedEvent.class);
        try {
            billing.onAppointmentBooked(event);
        } catch (DataIntegrityViolationException e) {
            // Two copies of the same event raced; the unique key on appointment_id let exactly one win.
            log.info("Duplicate booking event {} for appointment {} rejected by unique key",
                    event.eventId(), event.appointmentId());
        }
    }

    @KafkaListener(topics = Topics.APPOINTMENT_CANCELLED)
    public void onCancelled(String payload) throws JsonProcessingException {
        billing.onAppointmentCancelled(mapper.readValue(payload, AppointmentCancelledEvent.class));
    }
}

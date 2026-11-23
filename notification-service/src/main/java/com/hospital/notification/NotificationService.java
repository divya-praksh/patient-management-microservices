package com.hospital.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    /**
     * Records and "sends" a notification exactly once per event.
     *
     * @return false if this event was already handled (duplicate delivery)
     */
    @Transactional
    public boolean notify(UUID eventId, Long patientId, String eventType, String message) {
        if (repository.existsByEventId(eventId)) {
            log.info("Duplicate event {} ({}) ignored", eventId, eventType);
            return false;
        }
        repository.save(new Notification(eventId, patientId, eventType, message));
        // A real deployment would hand off to an email/SMS gateway here.
        log.info("EMAIL to patient {}: {}", patientId, message);
        return true;
    }

    @Transactional(readOnly = true)
    public List<Notification> forPatient(Long patientId) {
        return repository.findByPatientIdOrderByCreatedAtDescIdDesc(patientId);
    }
}

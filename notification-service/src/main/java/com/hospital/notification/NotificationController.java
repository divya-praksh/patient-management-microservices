package com.hospital.notification;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "Notifications", description = "Audit log of messages sent to patients")
public class NotificationController {

    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List a patient's notifications, newest first")
    public List<NotificationResponse> forPatient(@RequestParam Long patientId) {
        return service.forPatient(patientId).stream().map(NotificationResponse::from).toList();
    }

    public record NotificationResponse(Long id, Long patientId, String eventType, String channel,
                                       String message, Instant createdAt) {
        static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getPatientId(), n.getEventType(), n.getChannel(),
                    n.getMessage(), n.getCreatedAt());
        }
    }
}

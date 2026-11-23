package com.hospital.appointment;

import com.hospital.appointment.AppointmentDtos.AppointmentRequest;
import com.hospital.appointment.AppointmentDtos.AppointmentResponse;
import com.hospital.appointment.AppointmentDtos.CancelRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Booking, cancellation and completion (WF-2, WF-3, WF-4)")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Book an appointment",
            description = "Verifies the patient with patient-service, rejects double booking (409), publishes appointment.booked.")
    public ResponseEntity<AppointmentResponse> book(@Valid @RequestBody AppointmentRequest request) {
        Appointment a = service.book(request);
        return ResponseEntity.created(URI.create("/api/appointments/" + a.getId()))
                .body(AppointmentResponse.from(a));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an appointment")
    public AppointmentResponse get(@PathVariable Long id) {
        return AppointmentResponse.from(service.get(id));
    }

    @GetMapping
    @Operation(summary = "List a patient's appointments, newest first")
    public List<AppointmentResponse> forPatient(@RequestParam Long patientId) {
        return service.forPatient(patientId).stream().map(AppointmentResponse::from).toList();
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a booked appointment", description = "Publishes appointment.cancelled; billing voids or flags the invoice.")
    public AppointmentResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelRequest request) {
        return AppointmentResponse.from(service.cancel(id, request.reason().trim()));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Mark a consultation as completed", description = "Publishes appointment.completed.")
    public AppointmentResponse complete(@PathVariable Long id) {
        return AppointmentResponse.from(service.complete(id));
    }
}

package com.hospital.patient;

import com.hospital.patient.PatientDtos.PatientRequest;
import com.hospital.patient.PatientDtos.PatientResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Patients", description = "Patient registration and demographics (WF-1)")
public class PatientController {

    private final PatientService service;

    public PatientController(PatientService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a patient", description = "Publishes patient.registered after commit. 409 if the email is taken.")
    public ResponseEntity<PatientResponse> register(@Valid @RequestBody PatientRequest request) {
        Patient patient = service.register(request);
        return ResponseEntity.created(URI.create("/api/patients/" + patient.getId()))
                .body(PatientResponse.from(patient));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a patient by id")
    public PatientResponse get(@PathVariable Long id) {
        return PatientResponse.from(service.get(id));
    }

    @GetMapping
    @Operation(summary = "List all patients")
    public List<PatientResponse> list() {
        return service.list().stream().map(PatientResponse::from).toList();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update patient details")
    public PatientResponse update(@PathVariable Long id, @Valid @RequestBody PatientRequest request) {
        return PatientResponse.from(service.update(id, request));
    }
}

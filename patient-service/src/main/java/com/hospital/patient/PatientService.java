package com.hospital.patient;

import com.hospital.common.events.PatientRegisteredEvent;
import com.hospital.common.events.Topics;
import com.hospital.common.messaging.DomainEventPublisher;
import com.hospital.common.web.ApiException;
import com.hospital.patient.PatientDtos.PatientRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PatientService {

    private final PatientRepository repository;
    private final DomainEventPublisher events;

    public PatientService(PatientRepository repository, DomainEventPublisher events) {
        this.repository = repository;
        this.events = events;
    }

    @Transactional
    public Patient register(PatientRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email())) {
            throw ApiException.conflict("A patient with email " + request.email() + " is already registered");
        }
        Patient patient = repository.save(apply(new Patient(), request));
        events.publish(Topics.PATIENT_REGISTERED, patient.getId(),
                PatientRegisteredEvent.of(patient.getId(), patient.fullName(), patient.getEmail()));
        return patient;
    }

    @Transactional(readOnly = true)
    public Patient get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Patient " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Patient> list() {
        return repository.findAll();
    }

    @Transactional
    public Patient update(Long id, PatientRequest request) {
        Patient patient = get(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw ApiException.conflict("Email " + request.email() + " belongs to another patient");
        }
        return apply(patient, request);
    }

    private static Patient apply(Patient p, PatientRequest r) {
        p.setFirstName(r.firstName().trim());
        p.setLastName(r.lastName().trim());
        p.setEmail(r.email().trim().toLowerCase());
        p.setPhone(r.phone().trim());
        p.setDateOfBirth(r.dateOfBirth());
        p.setGender(r.gender());
        return p;
    }
}

package com.hospital.appointment;

import com.hospital.appointment.AppointmentDtos.AppointmentRequest;
import com.hospital.common.events.AppointmentBookedEvent;
import com.hospital.common.events.AppointmentCancelledEvent;
import com.hospital.common.events.AppointmentCompletedEvent;
import com.hospital.common.events.Topics;
import com.hospital.common.messaging.DomainEventPublisher;
import com.hospital.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;
    private final PatientClient patients;
    private final DomainEventPublisher events;

    public AppointmentService(AppointmentRepository repository, PatientClient patients, DomainEventPublisher events) {
        this.repository = repository;
        this.patients = patients;
        this.events = events;
    }

    @Transactional
    public Appointment book(AppointmentRequest request) {
        if (!patients.patientExists(request.patientId())) {
            throw ApiException.unprocessable("Patient " + request.patientId() + " does not exist");
        }
        String doctor = request.doctorName().trim();
        if (repository.existsByDoctorNameIgnoreCaseAndScheduledAtAndStatus(
                doctor, request.scheduledAt(), Appointment.Status.BOOKED)) {
            throw ApiException.conflict("Doctor " + doctor + " already has an appointment at " + request.scheduledAt());
        }
        Appointment a = repository.save(
                new Appointment(request.patientId(), doctor, request.department(), request.scheduledAt()));
        events.publish(Topics.APPOINTMENT_BOOKED, a.getId(), AppointmentBookedEvent.of(
                a.getId(), a.getPatientId(), a.getDoctorName(), a.getDepartment(), a.getScheduledAt()));
        return a;
    }

    @Transactional
    public Appointment cancel(Long id, String reason) {
        Appointment a = get(id);
        transition(() -> a.cancel(reason));
        events.publish(Topics.APPOINTMENT_CANCELLED, a.getId(),
                AppointmentCancelledEvent.of(a.getId(), a.getPatientId(), reason));
        return a;
    }

    @Transactional
    public Appointment complete(Long id) {
        Appointment a = get(id);
        transition(a::complete);
        events.publish(Topics.APPOINTMENT_COMPLETED, a.getId(),
                AppointmentCompletedEvent.of(a.getId(), a.getPatientId(), a.getDepartment()));
        return a;
    }

    @Transactional(readOnly = true)
    public Appointment get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Appointment " + id + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Appointment> forPatient(Long patientId) {
        return repository.findByPatientIdOrderByScheduledAtDesc(patientId);
    }

    private static void transition(Runnable change) {
        try {
            change.run();
        } catch (IllegalStateException e) {
            throw ApiException.conflict(e.getMessage());
        }
    }
}

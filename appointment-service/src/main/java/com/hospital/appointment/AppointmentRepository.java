package com.hospital.appointment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorNameIgnoreCaseAndScheduledAtAndStatus(String doctorName, LocalDateTime scheduledAt,
                                                                Appointment.Status status);

    List<Appointment> findByPatientIdOrderByScheduledAtDesc(Long patientId);
}

package com.hospital.appointment;

import com.hospital.common.events.Department;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public final class AppointmentDtos {

    private AppointmentDtos() {
    }

    public record AppointmentRequest(
            @NotNull @Positive Long patientId,
            @NotBlank @Size(max = 100) String doctorName,
            @NotNull Department department,
            @NotNull @Future LocalDateTime scheduledAt) {
    }

    public record CancelRequest(@NotBlank @Size(max = 255) String reason) {
    }

    public record AppointmentResponse(Long id, Long patientId, String doctorName, Department department,
                                      LocalDateTime scheduledAt, Appointment.Status status, String cancelReason) {

        static AppointmentResponse from(Appointment a) {
            return new AppointmentResponse(a.getId(), a.getPatientId(), a.getDoctorName(), a.getDepartment(),
                    a.getScheduledAt(), a.getStatus(), a.getCancelReason());
        }
    }
}

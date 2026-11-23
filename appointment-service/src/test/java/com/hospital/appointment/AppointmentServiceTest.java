package com.hospital.appointment;

import com.hospital.appointment.AppointmentDtos.AppointmentRequest;
import com.hospital.common.events.Department;
import com.hospital.common.events.Topics;
import com.hospital.common.messaging.DomainEventPublisher;
import com.hospital.common.web.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    AppointmentRepository repository;
    @Mock
    PatientClient patients;
    @Mock
    DomainEventPublisher events;
    @InjectMocks
    AppointmentService service;

    private final LocalDateTime slot = LocalDateTime.now().plusDays(3).withNano(0);
    private final AppointmentRequest request = new AppointmentRequest(1L, "Dr. Rao", Department.CARDIOLOGY, slot);

    @Test
    void unknownPatientIsRejectedWith422() {
        when(patients.patientExists(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.book(request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        verify(repository, never()).save(any());
    }

    @Test
    void doctorCannotBeDoubleBooked() {
        when(patients.patientExists(1L)).thenReturn(true);
        when(repository.existsByDoctorNameIgnoreCaseAndScheduledAtAndStatus("Dr. Rao", slot, Appointment.Status.BOOKED))
                .thenReturn(true);

        assertThatThrownBy(() -> service.book(request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(events, never()).publish(anyString(), any(), any());
    }

    @Test
    void cancelPublishesEventAndCompletedAppointmentCannotBeCancelled() {
        Appointment a = new Appointment(1L, "Dr. Rao", Department.CARDIOLOGY, slot);
        when(repository.findById(7L)).thenReturn(Optional.of(a));

        service.cancel(7L, "Patient request");
        assertThat(a.getStatus()).isEqualTo(Appointment.Status.CANCELLED);
        verify(events).publish(eq(Topics.APPOINTMENT_CANCELLED), any(), any());

        assertThatThrownBy(() -> service.complete(7L))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }
}

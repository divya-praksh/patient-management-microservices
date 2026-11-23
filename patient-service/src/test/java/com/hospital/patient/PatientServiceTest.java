package com.hospital.patient;

import com.hospital.common.events.PatientRegisteredEvent;
import com.hospital.common.events.Topics;
import com.hospital.common.messaging.DomainEventPublisher;
import com.hospital.common.web.ApiException;
import com.hospital.patient.PatientDtos.PatientRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    PatientRepository repository;
    @Mock
    DomainEventPublisher events;
    @InjectMocks
    PatientService service;

    private final PatientRequest request = new PatientRequest(" Asha ", "Menon", "Asha@Example.com",
            "+91-9876543210", LocalDate.of(1990, 4, 12), Patient.Gender.FEMALE);

    @Test
    void registerNormalisesDataAndPublishesEvent() {
        when(repository.existsByEmailIgnoreCase("Asha@Example.com")).thenReturn(false);
        when(repository.save(any(Patient.class))).thenAnswer(inv -> {
            Patient p = inv.getArgument(0);
            p.setId(42L);
            return p;
        });

        Patient saved = service.register(request);

        assertThat(saved.getFirstName()).isEqualTo("Asha");
        assertThat(saved.getEmail()).isEqualTo("asha@example.com");
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(events).publish(eq(Topics.PATIENT_REGISTERED), eq(42L), payload.capture());
        assertThat((PatientRegisteredEvent) payload.getValue())
                .extracting(PatientRegisteredEvent::patientId, PatientRegisteredEvent::fullName)
                .containsExactly(42L, "Asha Menon");
    }

    @Test
    void duplicateEmailIsRejectedWithoutPublishing() {
        when(repository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(repository, never()).save(any());
        verify(events, never()).publish(anyString(), any(), any());
    }
}

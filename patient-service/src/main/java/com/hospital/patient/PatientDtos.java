package com.hospital.patient;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;

public final class PatientDtos {

    private PatientDtos() {
    }

    public record PatientRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^\\+?[0-9 \\-]{7,20}$", message = "must be a valid phone number") String phone,
            @NotNull @Past LocalDate dateOfBirth,
            @NotNull Patient.Gender gender) {
    }

    public record PatientResponse(Long id, String firstName, String lastName, String email, String phone,
                                  LocalDate dateOfBirth, Patient.Gender gender, Instant registeredAt) {

        static PatientResponse from(Patient p) {
            return new PatientResponse(p.getId(), p.getFirstName(), p.getLastName(), p.getEmail(), p.getPhone(),
                    p.getDateOfBirth(), p.getGender(), p.getRegisteredAt());
        }
    }
}

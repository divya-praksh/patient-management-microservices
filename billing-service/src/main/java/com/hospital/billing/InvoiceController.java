package com.hospital.billing;

import com.hospital.common.events.Department;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
@Tag(name = "Invoices", description = "Consultation invoices and payment (WF-2, WF-3, WF-5). Invoices are created from events only.")
public class InvoiceController {

    private final BillingService billing;

    public InvoiceController(BillingService billing) {
        this.billing = billing;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an invoice")
    public InvoiceResponse get(@PathVariable Long id) {
        return InvoiceResponse.from(billing.get(id));
    }

    @GetMapping("/by-appointment/{appointmentId}")
    @Operation(summary = "Get the invoice for an appointment")
    public InvoiceResponse byAppointment(@PathVariable Long appointmentId) {
        return InvoiceResponse.from(billing.getByAppointment(appointmentId));
    }

    @GetMapping
    @Operation(summary = "List a patient's invoices, newest first")
    public List<InvoiceResponse> forPatient(@RequestParam Long patientId) {
        return billing.forPatient(patientId).stream().map(InvoiceResponse::from).toList();
    }

    @PostMapping("/{id}/pay")
    @Operation(summary = "Record payment", description = "PENDING to PAID only (409 otherwise). Publishes invoice.paid.")
    public InvoiceResponse pay(@PathVariable Long id) {
        return InvoiceResponse.from(billing.pay(id));
    }

    public record InvoiceResponse(Long id, Long appointmentId, Long patientId, Department department,
                                  BigDecimal amount, Invoice.Status status, Instant createdAt, Instant paidAt) {
        static InvoiceResponse from(Invoice i) {
            return new InvoiceResponse(i.getId(), i.getAppointmentId(), i.getPatientId(), i.getDepartment(),
                    i.getAmount(), i.getStatus(), i.getCreatedAt(), i.getPaidAt());
        }
    }
}

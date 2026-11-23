package com.hospital.billing;

import com.hospital.common.events.Department;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Invoice state machine: PENDING → PAID | VOID, PAID → REFUND_DUE. */
class InvoiceTest {

    private Invoice pending() {
        return Invoice.pending(7L, 1L, Department.CARDIOLOGY, new BigDecimal("1500.00"));
    }

    @Test
    void cancellingPendingInvoiceVoidsIt() {
        Invoice invoice = pending();
        assertThat(invoice.applyCancellation()).isTrue();
        assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.VOID);
        assertThat(invoice.applyCancellation()).as("second cancel is a no-op").isFalse();
    }

    @Test
    void cancellingPaidInvoiceFlagsRefundInsteadOfDroppingMoney() {
        Invoice invoice = pending();
        invoice.pay();
        assertThat(invoice.getPaidAt()).isNotNull();

        assertThat(invoice.applyCancellation()).isTrue();
        assertThat(invoice.getStatus()).isEqualTo(Invoice.Status.REFUND_DUE);
    }

    @Test
    void onlyPendingInvoicesCanBePaid() {
        Invoice voided = pending();
        voided.applyCancellation();
        assertThatThrownBy(voided::pay).isInstanceOf(IllegalStateException.class);

        Invoice paid = pending();
        paid.pay();
        assertThatThrownBy(paid::pay).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void feeScheduleCoversEveryDepartment() {
        FeeSchedule fees = new FeeSchedule();
        for (Department d : Department.values()) {
            assertThat(fees.feeFor(d)).isPositive();
        }
        assertThat(fees.feeFor(Department.CARDIOLOGY)).isEqualByComparingTo("1500.00");
    }
}

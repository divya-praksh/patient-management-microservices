package com.hospital.billing;

import com.hospital.common.events.Department;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/** Consultation fees per department (INR). See docs/01-requirements.md section 1.4. */
@Component
public class FeeSchedule {

    private final Map<Department, BigDecimal> fees = new EnumMap<>(Map.of(
            Department.GENERAL_MEDICINE, new BigDecimal("500.00"),
            Department.PEDIATRICS, new BigDecimal("700.00"),
            Department.ORTHOPEDICS, new BigDecimal("1200.00"),
            Department.CARDIOLOGY, new BigDecimal("1500.00"),
            Department.NEUROLOGY, new BigDecimal("1800.00")));

    public BigDecimal feeFor(Department department) {
        BigDecimal fee = fees.get(department);
        if (fee == null) {
            throw new IllegalArgumentException("No fee configured for department " + department);
        }
        return fee;
    }
}

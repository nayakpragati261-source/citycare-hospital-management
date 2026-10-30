package com.citycare.hospital.dto

import com.citycare.hospital.model.PaymentStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class BillRequest(
    @field:NotNull(message = "Patient is required")
    val patientId: Long? = null,

    @field:NotNull(message = "Bill date is required")
    val billDate: LocalDate? = null,

    @field:NotNull(message = "Consultation fee is required")
    @field:DecimalMin(value = "0.00", message = "Consultation fee cannot be negative")
    val consultationFee: BigDecimal? = null,

    @field:NotNull(message = "Test charges are required")
    @field:DecimalMin(value = "0.00", message = "Test charges cannot be negative")
    val testCharges: BigDecimal? = null,

    @field:NotNull(message = "Medicine charges are required")
    @field:DecimalMin(value = "0.00", message = "Medicine charges cannot be negative")
    val medicineCharges: BigDecimal? = null,

    @field:NotNull(message = "Payment status is required")
    val paymentStatus: PaymentStatus? = null
)

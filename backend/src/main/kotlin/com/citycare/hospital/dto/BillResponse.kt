package com.citycare.hospital.dto

import com.citycare.hospital.model.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class BillResponse(
    val id: Long,
    val billId: String,
    val patientId: Long,
    val patientName: String,
    val patientCode: String,
    val billDate: LocalDate,
    val consultationFee: BigDecimal,
    val testCharges: BigDecimal,
    val medicineCharges: BigDecimal,
    val totalAmount: BigDecimal,
    val paymentStatus: PaymentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

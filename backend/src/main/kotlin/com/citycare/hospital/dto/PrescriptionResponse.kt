package com.citycare.hospital.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class PrescriptionResponse(
    val id: Long,
    val prescriptionId: String,
    val patientId: Long,
    val patientName: String,
    val doctorId: Long,
    val doctorName: String,
    val appointmentId: Long? = null,
    val appointmentCode: String? = null,
    val prescriptionDate: LocalDate,
    val medicine: String,
    val dosage: String,
    val duration: String,
    val instructions: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

package com.citycare.hospital.dto

import java.time.LocalDate
import java.time.LocalDateTime

data class MedicalRecordResponse(
    val id: Long,
    val recordId: String,
    val patientId: Long,
    val patientName: String,
    val doctorId: Long,
    val doctorName: String,
    val appointmentId: Long? = null,
    val appointmentCode: String? = null,
    val previousDisease: String? = null,
    val diagnosis: String,
    val treatment: String,
    val recordDate: LocalDate,
    val notes: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

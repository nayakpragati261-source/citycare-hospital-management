package com.citycare.hospital.dto

import com.citycare.hospital.model.MedicalTestStatus
import java.time.LocalDate
import java.time.LocalDateTime

data class MedicalTestResponse(
    val id: Long,
    val testId: String,
    val patientId: Long,
    val patientName: String,
    val doctorId: Long,
    val doctorName: String,
    val appointmentId: Long? = null,
    val appointmentCode: String? = null,
    val testName: String,
    val testDate: LocalDate,
    val result: String? = null,
    val status: MedicalTestStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

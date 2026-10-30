package com.citycare.hospital.dto

import com.citycare.hospital.model.AppointmentStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AppointmentResponse(
    val id: Long,
    val appointmentId: String,
    val patientId: Long,
    val patientName: String,
    val patientCode: String,
    val doctorId: Long,
    val doctorName: String,
    val doctorCode: String,
    val departmentId: Long,
    val departmentName: String,
    val departmentCode: String,
    val appointmentDate: LocalDate,
    val appointmentTime: LocalTime,
    val reason: String,
    val status: AppointmentStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

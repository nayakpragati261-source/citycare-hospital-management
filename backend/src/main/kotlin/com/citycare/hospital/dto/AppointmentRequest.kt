package com.citycare.hospital.dto

import com.citycare.hospital.model.AppointmentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.time.LocalTime

data class AppointmentRequest(
    @field:NotNull(message = "Patient is required")
    val patientId: Long? = null,

    @field:NotNull(message = "Doctor is required")
    val doctorId: Long? = null,

    @field:NotNull(message = "Department is required")
    val departmentId: Long? = null,

    @field:NotNull(message = "Appointment date is required")
    val appointmentDate: LocalDate? = null,

    @field:NotNull(message = "Appointment time is required")
    val appointmentTime: LocalTime? = null,

    @field:NotBlank(message = "Reason for appointment is required")
    val reason: String = "",

    val status: AppointmentStatus? = AppointmentStatus.SCHEDULED
)

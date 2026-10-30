package com.citycare.hospital.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class MedicalRecordRequest(
    @field:NotNull(message = "Patient is required")
    val patientId: Long? = null,

    @field:NotNull(message = "Doctor is required")
    val doctorId: Long? = null,

    val appointmentId: Long? = null,

    val previousDisease: String? = null,

    @field:NotBlank(message = "Diagnosis is required")
    val diagnosis: String = "",

    @field:NotBlank(message = "Treatment details are required")
    val treatment: String = "",

    @field:NotNull(message = "Record date is required")
    val recordDate: LocalDate? = null,

    val notes: String? = null
)

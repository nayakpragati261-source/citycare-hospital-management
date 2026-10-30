package com.citycare.hospital.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class PrescriptionRequest(
    @field:NotNull(message = "Patient is required")
    val patientId: Long? = null,

    @field:NotNull(message = "Doctor is required")
    val doctorId: Long? = null,

    val appointmentId: Long? = null,

    @field:NotNull(message = "Prescription date is required")
    val prescriptionDate: LocalDate? = null,

    @field:NotBlank(message = "Medicine details are required")
    val medicine: String = "",

    @field:NotBlank(message = "Dosage details are required")
    val dosage: String = "",

    @field:NotBlank(message = "Duration details are required")
    val duration: String = "",

    val instructions: String? = null
)

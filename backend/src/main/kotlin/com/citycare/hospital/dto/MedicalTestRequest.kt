package com.citycare.hospital.dto

import com.citycare.hospital.model.MedicalTestStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class MedicalTestRequest(
    @field:NotNull(message = "Patient is required")
    val patientId: Long? = null,

    @field:NotNull(message = "Doctor is required")
    val doctorId: Long? = null,

    val appointmentId: Long? = null,

    @field:NotBlank(message = "Test name is required")
    val testName: String = "",

    @field:NotNull(message = "Test date is required")
    val testDate: LocalDate? = null,

    val result: String? = null,

    val status: MedicalTestStatus? = MedicalTestStatus.PENDING
)

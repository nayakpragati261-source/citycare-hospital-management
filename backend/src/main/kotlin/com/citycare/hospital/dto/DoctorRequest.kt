package com.citycare.hospital.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class DoctorRequest(
    @field:NotBlank(message = "User email is required")
    val userEmail: String = "",

    @field:NotBlank(message = "Specialization is required")
    val specialization: String = "",

    @field:NotNull(message = "Experience is required")
    @field:Min(value = 0, message = "Experience must be at least 0")
    @field:Max(value = 60, message = "Experience cannot exceed 60 years")
    val experience: Int? = null,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    val phone: String = "",

    @field:NotBlank(message = "Availability schedule is required")
    val availability: String = ""
)

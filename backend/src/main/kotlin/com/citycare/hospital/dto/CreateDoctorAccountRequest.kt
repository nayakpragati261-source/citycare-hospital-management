package com.citycare.hospital.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class CreateDoctorAccountRequest(
    @field:NotBlank(message = "Doctor name is required")
    val name: String = "",

    @field:NotBlank(message = "Email address is required")
    @field:Email(message = "Valid email address is required")
    val email: String = "",

    @field:NotBlank(message = "Password is required")
    val password: String = "",

    @field:NotBlank(message = "Specialization is required")
    val specialization: String = "",

    @field:NotNull(message = "Experience is required")
    @field:Min(value = 0, message = "Experience must be at least 0")
    @field:Max(value = 60, message = "Experience cannot exceed 60 years")
    val experience: Int? = null,

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    val phone: String = "",

    val departmentId: Long? = null,
    val departmentName: String? = null,

    @field:NotBlank(message = "Availability schedule is required")
    val availability: String = ""
)

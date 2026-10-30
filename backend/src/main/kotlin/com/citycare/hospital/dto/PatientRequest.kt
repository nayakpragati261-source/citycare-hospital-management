package com.citycare.hospital.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern

data class PatientRequest(
    @field:NotBlank(message = "Patient name is required")
    val name: String = "",

    @field:NotNull(message = "Age is required")
    @field:Min(value = 0, message = "Age must be at least 0")
    @field:Max(value = 120, message = "Age cannot exceed 120")
    val age: Int? = null,

    @field:NotBlank(message = "Gender is required")
    @field:Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    val gender: String = "",

    @field:NotBlank(message = "Disease/Condition is required")
    val disease: String = "",

    @field:NotBlank(message = "Phone number is required")
    @field:Pattern(regexp = "^[6-9]\\d{9}$", message = "Phone must be a valid 10-digit Indian mobile number starting with 6-9")
    val phone: String = "",

    @field:NotBlank(message = "Address is required")
    val address: String = "",

    val departmentId: Long? = null
)

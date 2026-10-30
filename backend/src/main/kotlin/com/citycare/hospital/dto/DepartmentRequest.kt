package com.citycare.hospital.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class DepartmentRequest(
    @field:NotBlank(message = "Department name is required")
    @field:Size(max = 100, message = "Department name cannot exceed 100 characters")
    val name: String = "",

    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    val description: String = ""
)

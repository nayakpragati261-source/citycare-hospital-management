package com.citycare.hospital.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ResetPasswordRequest(
    @field:NotBlank(message = "New password is required")
    @field:Size(min = 6, message = "Password must be at least 6 characters long")
    val newPassword: String = "",

    @field:NotBlank(message = "Confirm password is required")
    val confirmPassword: String = ""
)

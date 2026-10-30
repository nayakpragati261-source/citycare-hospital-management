package com.citycare.hospital.dto

import java.time.LocalDateTime

data class DepartmentResponse(
    val id: Long,
    val departmentId: String,
    val name: String,
    val description: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val doctorCount: Long = 0,
    val patientCount: Long = 0
)

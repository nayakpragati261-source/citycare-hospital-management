package com.citycare.hospital.dto

import java.time.LocalDateTime

data class DoctorResponse(
    val id: Long,
    val doctorId: String,
    val userId: Long,
    val name: String,
    val email: String,
    val specialization: String,
    val departmentId: Long? = null,
    val departmentName: String? = null,
    val experience: Int,
    val phone: String,
    val availability: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

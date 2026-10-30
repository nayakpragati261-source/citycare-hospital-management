package com.citycare.hospital.dto

import java.time.LocalDateTime

data class PatientResponse(
    val id: Long,
    val patientId: String,
    val name: String,
    val age: Int,
    val gender: String,
    val disease: String,
    val phone: String,
    val address: String,
    val departmentId: Long? = null,
    val departmentName: String? = null,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

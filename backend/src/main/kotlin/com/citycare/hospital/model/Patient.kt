package com.citycare.hospital.model

import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(name = "patients")
class Patient(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "patient_id", nullable = false, unique = true)
    var patientId: String = "",

    @field:NotBlank(message = "Name is required")
    @Column(nullable = false)
    var name: String = "",

    @field:NotNull(message = "Age is required")
    @field:Min(value = 0, message = "Age must be at least 0")
    @field:Max(value = 120, message = "Age cannot exceed 120")
    @Column(nullable = false)
    var age: Int = 0,

    @field:NotBlank(message = "Gender is required")
    @Column(nullable = false)
    var gender: String = "",

    @field:NotBlank(message = "Disease is required")
    @Column(nullable = false)
    var disease: String = "",

    @field:NotBlank(message = "Phone is required")
    @Column(nullable = false)
    var phone: String = "",

    @field:NotBlank(message = "Address is required")
    @Column(nullable = false, length = 500)
    var address: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = true)
    var department: Department? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

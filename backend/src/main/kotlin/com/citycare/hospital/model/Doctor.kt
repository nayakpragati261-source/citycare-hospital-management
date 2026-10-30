package com.citycare.hospital.model

import jakarta.persistence.*
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(name = "doctors")
class Doctor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "doctor_id", nullable = false, unique = true)
    var doctorId: String = "",

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    var user: User? = null,

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var email: String = "",

    @field:NotBlank(message = "Specialization is required")
    @Column(nullable = false)
    var specialization: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id", nullable = true)
    var department: Department? = null,

    @field:NotNull(message = "Experience is required")
    @field:Min(value = 0, message = "Experience must be at least 0")
    @field:Max(value = 60, message = "Experience cannot exceed 60 years")
    @Column(nullable = false)
    var experience: Int = 0,

    @field:NotBlank(message = "Phone is required")
    @Column(nullable = false)
    var phone: String = "",

    @field:NotBlank(message = "Availability is required")
    @Column(nullable = false)
    var availability: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

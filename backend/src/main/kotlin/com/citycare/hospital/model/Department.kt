package com.citycare.hospital.model

import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

@Entity
@Table(name = "departments")
class Department(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "department_id", nullable = false, unique = true)
    var departmentId: String = "",

    @field:NotBlank(message = "Department name is required")
    @Column(nullable = false, unique = true)
    var name: String = "",

    @Column(length = 1000)
    var description: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

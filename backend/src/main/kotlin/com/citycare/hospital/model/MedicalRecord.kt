package com.citycare.hospital.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "medical_records")
class MedicalRecord(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "record_id", nullable = false, unique = true)
    var recordId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    var doctor: Doctor? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = true)
    var appointment: Appointment? = null,

    @Column(name = "previous_disease", length = 500)
    var previousDisease: String? = null,

    @Column(nullable = false, length = 1000)
    var diagnosis: String = "",

    @Column(nullable = false, length = 1000)
    var treatment: String = "",

    @Column(name = "record_date", nullable = false)
    var recordDate: LocalDate = LocalDate.now(),

    @Column(length = 1000)
    var notes: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

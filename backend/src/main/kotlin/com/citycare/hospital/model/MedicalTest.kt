package com.citycare.hospital.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "medical_tests")
class MedicalTest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "test_id", nullable = false, unique = true)
    var testId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    var doctor: Doctor? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = true)
    var appointment: Appointment? = null,

    @Column(name = "test_name", nullable = false)
    var testName: String = "",

    @Column(name = "test_date", nullable = false)
    var testDate: LocalDate = LocalDate.now(),

    @Column(length = 1000)
    var result: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MedicalTestStatus = MedicalTestStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

package com.citycare.hospital.model

import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "prescriptions")
class Prescription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "prescription_id", nullable = false, unique = true)
    var prescriptionId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "doctor_id", nullable = false)
    var doctor: Doctor? = null,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "appointment_id", nullable = true)
    var appointment: Appointment? = null,

    @Column(name = "prescription_date", nullable = false)
    var prescriptionDate: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var medicine: String = "",

    @Column(nullable = false)
    var dosage: String = "",

    @Column(nullable = false)
    var duration: String = "",

    @Column(length = 1000)
    var instructions: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

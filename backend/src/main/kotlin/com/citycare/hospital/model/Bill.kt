package com.citycare.hospital.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(name = "bills")
class Bill(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "bill_id", nullable = false, unique = true)
    var billId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    var patient: Patient? = null,

    @Column(name = "bill_date", nullable = false)
    var billDate: LocalDate = LocalDate.now(),

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    var consultationFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "test_charges", nullable = false, precision = 10, scale = 2)
    var testCharges: BigDecimal = BigDecimal.ZERO,

    @Column(name = "medicine_charges", nullable = false, precision = 10, scale = 2)
    var medicineCharges: BigDecimal = BigDecimal.ZERO,

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    var totalAmount: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    var paymentStatus: PaymentStatus = PaymentStatus.PENDING,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)

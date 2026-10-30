package com.citycare.hospital.repository

import com.citycare.hospital.model.Bill
import com.citycare.hospital.model.PaymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.Optional

@Repository
interface BillRepository : JpaRepository<Bill, Long> {
    fun findByBillId(billId: String): Optional<Bill>
    fun existsByBillId(billId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<Bill>

    fun countByPaymentStatus(paymentStatus: PaymentStatus): Long
    fun findByPatientId(patientId: Long): List<Bill>

    @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.paymentStatus = com.citycare.hospital.model.PaymentStatus.PAID")
    fun sumPaidTotalAmount(): BigDecimal?
}

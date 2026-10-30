package com.citycare.hospital.service

import com.citycare.hospital.dto.BillRequest
import com.citycare.hospital.dto.BillResponse
import com.citycare.hospital.model.Bill
import com.citycare.hospital.model.PaymentStatus
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.BillRepository
import com.citycare.hospital.repository.PatientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BillService(
    private val billRepository: BillRepository,
    private val patientRepository: PatientRepository
) {

    fun getAllBills(status: PaymentStatus? = null, patientId: Long? = null): List<BillResponse> {
        var list = billRepository.findAll()
        if (status != null) {
            list = list.filter { it.paymentStatus == status }
        }
        if (patientId != null) {
            list = list.filter { it.patient?.id == patientId }
        }
        return list
            .sortedByDescending { it.billDate }
            .map { toBillResponse(it) }
    }

    fun getBillsForDoctor(userEmail: String, status: PaymentStatus? = null, patientId: Long? = null): List<BillResponse> {
        return getAllBills(status, patientId)
    }

    fun getBillById(id: Long): BillResponse {
        val bill = billRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found.") }
        return toBillResponse(bill)
    }

    @Transactional
    fun createBill(request: BillRequest, currentUserRole: Role): BillResponse {
        if (currentUserRole != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required to create bills.")
        }

        val patientId = request.patientId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is required.")
        val billDate = request.billDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Bill date is required.")
        val consultationFee = request.consultationFee ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation fee is required.")
        val testCharges = request.testCharges ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test charges are required.")
        val medicineCharges = request.medicineCharges ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine charges are required.")
        val paymentStatus = request.paymentStatus ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Payment status is required.")

        if (consultationFee < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation fee cannot be negative.")
        if (testCharges < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test charges cannot be negative.")
        if (medicineCharges < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine charges cannot be negative.")

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val totalAmount = consultationFee.add(testCharges).add(medicineCharges)
        val billIdStr = generateUniqueBillId()
        val now = LocalDateTime.now()

        val bill = Bill(
            billId = billIdStr,
            patient = patient,
            billDate = billDate,
            consultationFee = consultationFee,
            testCharges = testCharges,
            medicineCharges = medicineCharges,
            totalAmount = totalAmount,
            paymentStatus = paymentStatus,
            createdAt = now,
            updatedAt = now
        )

        val saved = billRepository.save(bill)
        return toBillResponse(saved)
    }

    @Transactional
    fun updateBill(id: Long, request: BillRequest, currentUserRole: Role): BillResponse {
        if (currentUserRole != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required to update bills.")
        }

        val bill = billRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found.") }

        val patientId = request.patientId ?: bill.patient!!.id!!
        val billDate = request.billDate ?: bill.billDate
        val consultationFee = request.consultationFee ?: bill.consultationFee
        val testCharges = request.testCharges ?: bill.testCharges
        val medicineCharges = request.medicineCharges ?: bill.medicineCharges
        val paymentStatus = request.paymentStatus ?: bill.paymentStatus

        if (consultationFee < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Consultation fee cannot be negative.")
        if (testCharges < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test charges cannot be negative.")
        if (medicineCharges < BigDecimal.ZERO) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine charges cannot be negative.")

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val totalAmount = consultationFee.add(testCharges).add(medicineCharges)

        bill.patient = patient
        bill.billDate = billDate
        bill.consultationFee = consultationFee
        bill.testCharges = testCharges
        bill.medicineCharges = medicineCharges
        bill.totalAmount = totalAmount
        bill.paymentStatus = paymentStatus
        bill.updatedAt = LocalDateTime.now()

        val saved = billRepository.save(bill)
        return toBillResponse(saved)
    }

    @Transactional
    fun deleteBill(id: Long, currentUserRole: Role): Boolean {
        if (currentUserRole != Role.ADMIN) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Admin role required to delete bills.")
        }

        val bill = billRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Bill not found.") }

        billRepository.delete(bill)
        return true
    }

    fun getTotalBillCount(): Long {
        return billRepository.count()
    }

    fun getCountByPaymentStatus(status: PaymentStatus): Long {
        return billRepository.countByPaymentStatus(status)
    }

    fun getTotalPaidRevenue(): BigDecimal {
        return billRepository.sumPaidTotalAmount() ?: BigDecimal.ZERO
    }

    @Synchronized
    private fun generateUniqueBillId(): String {
        val lastBill = billRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastBill.isPresent) {
            val lastIdStr = lastBill.get().billId
            if (lastIdStr.startsWith("BILL-")) {
                val numPart = lastIdStr.removePrefix("BILL-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastBill.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("BILL-%04d", nextIdNum)
        while (billRepository.existsByBillId(candidateId)) {
            nextIdNum++
            candidateId = String.format("BILL-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toBillResponse(bill: Bill): BillResponse {
        val patient = bill.patient!!
        return BillResponse(
            id = bill.id ?: 0L,
            billId = bill.billId,
            patientId = patient.id ?: 0L,
            patientName = patient.name,
            patientCode = patient.patientId,
            billDate = bill.billDate,
            consultationFee = bill.consultationFee,
            testCharges = bill.testCharges,
            medicineCharges = bill.medicineCharges,
            totalAmount = bill.totalAmount,
            paymentStatus = bill.paymentStatus,
            createdAt = bill.createdAt,
            updatedAt = bill.updatedAt
        )
    }
}

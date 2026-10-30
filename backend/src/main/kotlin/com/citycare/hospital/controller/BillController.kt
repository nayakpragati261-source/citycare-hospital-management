package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.BillRequest
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.model.PaymentStatus
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.BillService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bills")
class BillController(private val billService: BillService) {

    @GetMapping
    fun getAllBills(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) patientId: Long?,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val bills = if (userRole == Role.ADMIN) {
            billService.getAllBills(status, patientId)
        } else {
            billService.getBillsForDoctor(authUser.email, status, patientId)
        }
        return ResponseEntity.ok(bills)
    }

    @GetMapping("/{id}")
    fun getBillById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val bill = billService.getBillById(id)
        return ResponseEntity.ok(bill)
    }

    @PostMapping
    fun createBill(
        @Valid @RequestBody request: BillRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val created = billService.createBill(request, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateBill(
        @PathVariable id: Long,
        @Valid @RequestBody request: BillRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val updated = billService.updateBill(id, request, userRole)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteBill(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        billService.deleteBill(id, userRole)
        return ResponseEntity.ok(mapOf("message" to "Bill deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

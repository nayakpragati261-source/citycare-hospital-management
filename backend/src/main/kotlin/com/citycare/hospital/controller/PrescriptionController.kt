package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.PrescriptionRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.PrescriptionService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/prescriptions")
class PrescriptionController(private val prescriptionService: PrescriptionService) {

    @GetMapping
    fun getAllPrescriptions(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val prescriptions = if (userRole == Role.ADMIN) {
            prescriptionService.getAllPrescriptions()
        } else {
            prescriptionService.getPrescriptionsForDoctor(authUser.email)
        }
        return ResponseEntity.ok(prescriptions)
    }

    @GetMapping("/{id}")
    fun getPrescriptionById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val prescription = prescriptionService.getPrescriptionById(id, authUser.email, userRole)
        return ResponseEntity.ok(prescription)
    }

    @PostMapping
    fun createPrescription(
        @Valid @RequestBody request: PrescriptionRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val created = prescriptionService.createPrescription(request, authUser.email, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updatePrescription(
        @PathVariable id: Long,
        @Valid @RequestBody request: PrescriptionRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val updated = prescriptionService.updatePrescription(id, request, authUser.email, userRole)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deletePrescription(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        prescriptionService.deletePrescription(id, authUser.email, userRole)
        return ResponseEntity.ok(mapOf("message" to "Prescription deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.PatientRequest
import com.citycare.hospital.dto.PatientResponse
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.PatientService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/patients")
class PatientController(private val patientService: PatientService) {

    @GetMapping
    fun getAllPatients(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val patients = patientService.getPatientsForUser(authUser.email, userRole)
        return ResponseEntity.ok(patients)
    }

    @GetMapping("/{id}")
    fun getPatientById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val patient = patientService.getPatientByIdForUser(id, authUser.email, userRole)
        return ResponseEntity.ok(patient)
    }

    @PostMapping
    fun createPatient(
        @Valid @RequestBody request: PatientRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val createdPatient = patientService.createPatient(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPatient)
    }

    @PutMapping("/{id}")
    fun updatePatient(
        @PathVariable id: Long,
        @Valid @RequestBody request: PatientRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val updatedPatient = patientService.updatePatient(id, request)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Patient not found with ID: $id"))

        return ResponseEntity.ok(updatedPatient)
    }

    @DeleteMapping("/{id}")
    fun deletePatient(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val deleted = patientService.deletePatient(id)
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Patient not found with ID: $id"))
        }

        return ResponseEntity.ok(mapOf("message" to "Patient deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.MedicalRecordRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.MedicalRecordService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/medical-records")
class MedicalRecordController(private val medicalRecordService: MedicalRecordService) {

    @GetMapping
    fun getAllMedicalRecords(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val records = if (userRole == Role.ADMIN) {
            medicalRecordService.getAllMedicalRecords()
        } else {
            medicalRecordService.getMedicalRecordsForDoctor(authUser.email)
        }
        return ResponseEntity.ok(records)
    }

    @GetMapping("/{id}")
    fun getMedicalRecordById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val record = medicalRecordService.getMedicalRecordById(id, authUser.email, userRole)
        return ResponseEntity.ok(record)
    }

    @PostMapping
    fun createMedicalRecord(
        @Valid @RequestBody request: MedicalRecordRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val created = medicalRecordService.createMedicalRecord(request, authUser.email, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateMedicalRecord(
        @PathVariable id: Long,
        @Valid @RequestBody request: MedicalRecordRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val updated = medicalRecordService.updateMedicalRecord(id, request, authUser.email, userRole)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteMedicalRecord(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        medicalRecordService.deleteMedicalRecord(id, authUser.email, userRole)
        return ResponseEntity.ok(mapOf("message" to "Medical record deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

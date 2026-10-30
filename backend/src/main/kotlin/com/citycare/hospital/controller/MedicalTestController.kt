package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.MedicalTestRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.MedicalTestService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/medical-tests")
class MedicalTestController(private val medicalTestService: MedicalTestService) {

    @GetMapping
    fun getAllMedicalTests(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val tests = if (userRole == Role.ADMIN) {
            medicalTestService.getAllMedicalTests()
        } else {
            medicalTestService.getMedicalTestsForDoctor(authUser.email)
        }
        return ResponseEntity.ok(tests)
    }

    @GetMapping("/{id}")
    fun getMedicalTestById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val test = medicalTestService.getMedicalTestById(id, authUser.email, userRole)
        return ResponseEntity.ok(test)
    }

    @PostMapping
    fun createMedicalTest(
        @Valid @RequestBody request: MedicalTestRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val created = medicalTestService.createMedicalTest(request, authUser.email, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateMedicalTest(
        @PathVariable id: Long,
        @Valid @RequestBody request: MedicalTestRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val updated = medicalTestService.updateMedicalTest(id, request, authUser.email, userRole)
        return ResponseEntity.ok(updated)
    }

    @DeleteMapping("/{id}")
    fun deleteMedicalTest(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        medicalTestService.deleteMedicalTest(id, authUser.email, userRole)
        return ResponseEntity.ok(mapOf("message" to "Medical test deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

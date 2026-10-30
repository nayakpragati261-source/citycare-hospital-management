package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.CreateDoctorAccountRequest
import com.citycare.hospital.dto.DoctorRequest
import com.citycare.hospital.dto.DoctorResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.ResetPasswordRequest
import com.citycare.hospital.dto.UserSummaryResponse
import com.citycare.hospital.service.DoctorService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping
class DoctorController(private val doctorService: DoctorService) {

    @PostMapping("/api/doctors/{id}/reset-password")
    fun resetDoctorPassword(
        @PathVariable id: Long,
        @Valid @RequestBody request: ResetPasswordRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        return try {
            doctorService.resetDoctorPassword(id, request)
            ResponseEntity.ok(mapOf("message" to "Doctor password reset successfully"))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Failed to reset doctor password"))
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Failed to reset doctor password"))
        }
    }

    @GetMapping("/api/doctors")
    fun getAllDoctors(): ResponseEntity<List<DoctorResponse>> {
        val doctors = doctorService.getAllDoctors()
        return ResponseEntity.ok(doctors)
    }

    @GetMapping("/api/doctors/{id}")
    fun getDoctorById(@PathVariable id: Long): ResponseEntity<*> {
        val doctor = doctorService.getDoctorById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Doctor profile not found with ID: $id"))

        return ResponseEntity.ok(doctor)
    }

    @GetMapping("/api/users/available-doctors")
    fun getAvailableDoctors(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val availableUsers = doctorService.getAvailableDoctorUsers()
        return ResponseEntity.ok(availableUsers)
    }

    @PostMapping("/api/doctors/create-account")
    fun createDoctorAccount(
        @Valid @RequestBody request: CreateDoctorAccountRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        return try {
            val createdDoctor = doctorService.createDoctorAccount(request)
            ResponseEntity.status(HttpStatus.CREATED).body(createdDoctor)
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Invalid doctor request"))
        }
    }

    @PostMapping("/api/doctors")
    fun createDoctor(
        @Valid @RequestBody request: DoctorRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        return try {
            val createdDoctor = doctorService.createDoctor(request)
            ResponseEntity.status(HttpStatus.CREATED).body(createdDoctor)
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Invalid doctor request"))
        }
    }

    @PutMapping("/api/doctors/{id}")
    fun updateDoctor(
        @PathVariable id: Long,
        @Valid @RequestBody request: DoctorRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        return try {
            val updatedDoctor = doctorService.updateDoctor(id, request)
                ?: return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Doctor profile not found with ID: $id"))

            ResponseEntity.ok(updatedDoctor)
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Invalid update request"))
        }
    }

    @DeleteMapping("/api/doctors/{id}")
    fun deleteDoctor(
        @PathVariable id: Long,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val deleted = doctorService.deleteDoctor(id)
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse("Doctor profile not found with ID: $id"))
        }

        return ResponseEntity.ok(mapOf("message" to "Doctor profile deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

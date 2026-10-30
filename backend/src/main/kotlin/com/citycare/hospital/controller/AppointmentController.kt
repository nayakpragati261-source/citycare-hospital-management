package com.citycare.hospital.controller

import com.citycare.hospital.dto.AppointmentRequest
import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.AppointmentService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/appointments")
class AppointmentController(private val appointmentService: AppointmentService) {

    @GetMapping
    fun getAllAppointments(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val appointments = if (userRole == Role.ADMIN) {
            appointmentService.getAllAppointments()
        } else {
            appointmentService.getAppointmentsForDoctor(authUser.email)
        }
        return ResponseEntity.ok(appointments)
    }

    @GetMapping("/{id}")
    fun getAppointmentById(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val appointment = appointmentService.getAppointmentById(id, authUser.email, userRole)
        return ResponseEntity.ok(appointment)
    }

    @PostMapping
    fun createAppointment(
        @Valid @RequestBody request: AppointmentRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val created = appointmentService.createAppointment(request, authUser.email, userRole)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    fun updateAppointment(
        @PathVariable id: Long,
        @Valid @RequestBody request: AppointmentRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val updated = appointmentService.updateAppointment(id, request, authUser.email, userRole)
        return ResponseEntity.ok(updated)
    }

    @PutMapping("/{id}/cancel")
    fun cancelAppointment(
        @PathVariable id: Long,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val cancelled = appointmentService.cancelAppointment(id, authUser.email, userRole)
        return ResponseEntity.ok(cancelled)
    }

    @PutMapping("/{id}/complete")
    fun completeAppointment(
        @PathVariable id: Long,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val userRole = Role.valueOf(authUser.role)
        val completed = appointmentService.completeAppointment(id, authUser.email, userRole)
        return ResponseEntity.ok(completed)
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

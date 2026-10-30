package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ChangePasswordRequest
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.service.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(private val userService: UserService) {

    @PostMapping("/login")
    fun login(
        @Valid @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        val authUser = userService.authenticateUser(loginRequest.email, loginRequest.password)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid email or password"))

        // Create or retrieve server-side session
        val session = request.getSession(true)
        session.setAttribute("user", authUser)

        return ResponseEntity.ok(authUser)
    }

    @PostMapping("/admin-login")
    fun adminLogin(
        @Valid @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        val userOpt = userService.findByEmail(loginRequest.email)
        if (userOpt.isEmpty) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid email or password"))
        }

        val user = userOpt.get()
        if (!user.active || !userService.verifyPassword(loginRequest.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid email or password"))
        }

        if (user.role != Role.ADMIN) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse("This account is not authorized for Admin access."))
        }

        val authUser = userService.toAuthUserResponse(user)
        val session = request.getSession(true)
        session.setAttribute("user", authUser)

        return ResponseEntity.ok(authUser)
    }

    @PostMapping("/doctor-login")
    fun doctorLogin(
        @Valid @RequestBody loginRequest: LoginRequest,
        request: HttpServletRequest
    ): ResponseEntity<*> {
        val userOpt = userService.findByEmail(loginRequest.email)
        if (userOpt.isEmpty) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid email or password"))
        }

        val user = userOpt.get()
        if (!user.active || !userService.verifyPassword(loginRequest.password, user.passwordHash)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid email or password"))
        }

        if (user.role != Role.DOCTOR) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse("This account is not authorized for Doctor access."))
        }

        val authUser = userService.toAuthUserResponse(user)
        val session = request.getSession(true)
        session.setAttribute("user", authUser)

        return ResponseEntity.ok(authUser)
    }

    @GetMapping("/me")
    fun getCurrentUser(session: HttpSession): ResponseEntity<*> {
        val authUser = session.getAttribute("user") as? AuthUserResponse
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Unauthorized access"))

        return ResponseEntity.ok(authUser)
    }

    @PostMapping("/logout")
    fun logout(request: HttpServletRequest): ResponseEntity<*> {
        val session = request.getSession(false)
        session?.invalidate()

        val response = mapOf("message" to "Logged out successfully")
        return ResponseEntity.ok(response)
    }

    @PostMapping("/change-password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = session.getAttribute("user") as? AuthUserResponse
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        return try {
            userService.changePassword(authUser.id, request)
            ResponseEntity.ok(mapOf("message" to "Password changed successfully"))
        } catch (ex: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message ?: "Failed to change password"))
        } catch (ex: Exception) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ErrorResponse("Failed to change password"))
        }
    }
}

package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.service.DashboardService
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/dashboard")
class DashboardController(private val dashboardService: DashboardService) {

    @GetMapping
    fun getDashboardData(session: HttpSession): ResponseEntity<*> {
        val authUser = session.getAttribute("user") as? AuthUserResponse
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Unauthorized access"))

        val metrics = dashboardService.getDashboardMetrics(authUser.email, authUser.role)
        return ResponseEntity.ok(metrics)
    }
}

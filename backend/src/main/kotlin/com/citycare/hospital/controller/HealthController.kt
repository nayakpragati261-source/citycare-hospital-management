package com.citycare.hospital.controller

import com.citycare.hospital.dto.HealthStatusResponse
import com.citycare.hospital.service.HealthService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/health")
class HealthController(private val healthService: HealthService) {

    @GetMapping
    fun getHealth(): ResponseEntity<HealthStatusResponse> {
        val health = healthService.checkApplicationHealth()
        return ResponseEntity.ok(health)
    }

    @GetMapping("/database")
    fun getDatabaseHealth(): ResponseEntity<HealthStatusResponse> {
        val dbHealth = healthService.checkDatabaseHealth()
        return if (dbHealth.status == "OK") {
            ResponseEntity.ok(dbHealth)
        } else {
            ResponseEntity.status(503).body(dbHealth)
        }
    }
}

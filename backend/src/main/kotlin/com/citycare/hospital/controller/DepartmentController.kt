package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.DepartmentRequest
import com.citycare.hospital.dto.DepartmentResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.service.DepartmentService
import jakarta.servlet.http.HttpSession
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/departments")
class DepartmentController(private val departmentService: DepartmentService) {

    @GetMapping
    fun getAllDepartments(): ResponseEntity<List<DepartmentResponse>> {
        val departments = departmentService.getAllDepartments()
        return ResponseEntity.ok(departments)
    }

    @GetMapping("/{id}")
    fun getDepartmentById(@PathVariable id: Long): ResponseEntity<*> {
        val department = departmentService.getDepartmentById(id)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Department not found with ID: $id"))

        return ResponseEntity.ok(department)
    }

    @PostMapping
    fun createDepartment(
        @Valid @RequestBody request: DepartmentRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val createdDepartment = departmentService.createDepartment(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdDepartment)
    }

    @PutMapping("/{id}")
    fun updateDepartment(
        @PathVariable id: Long,
        @Valid @RequestBody request: DepartmentRequest,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val updatedDepartment = departmentService.updateDepartment(id, request)
            ?: return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Department not found with ID: $id"))

        return ResponseEntity.ok(updatedDepartment)
    }

    @DeleteMapping("/{id}")
    fun deleteDepartment(
        @PathVariable id: Long,
        session: HttpSession
    ): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        if (authUser.role != "ADMIN") {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ErrorResponse("Access denied. Admin role required."))
        }

        val deleted = departmentService.deleteDepartment(id)
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse("Department not found with ID: $id"))
        }

        return ResponseEntity.ok(mapOf("message" to "Department deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

package com.citycare.hospital.service

import com.citycare.hospital.dto.DepartmentRequest
import com.citycare.hospital.dto.DepartmentResponse
import com.citycare.hospital.model.Department
import com.citycare.hospital.repository.DepartmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.PatientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
    private val doctorRepository: DoctorRepository,
    private val patientRepository: PatientRepository
) {

    fun getAllDepartments(): List<DepartmentResponse> {
        return departmentRepository.findAll()
            .map { toDepartmentResponse(it) }
    }

    fun getDepartmentById(id: Long): DepartmentResponse? {
        return departmentRepository.findById(id)
            .map { toDepartmentResponse(it) }
            .orElse(null)
    }

    fun getDepartmentByDepartmentId(departmentId: String): DepartmentResponse? {
        return departmentRepository.findByDepartmentId(departmentId)
            .map { toDepartmentResponse(it) }
            .orElse(null)
    }

    @Transactional
    fun createDepartment(request: DepartmentRequest): DepartmentResponse {
        val trimmedName = request.name.trim()

        if (departmentRepository.existsByNameIgnoreCase(trimmedName)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A department with this name already exists.")
        }

        val nextDepartmentId = generateUniqueDepartmentId()
        val now = LocalDateTime.now()

        val department = Department(
            departmentId = nextDepartmentId,
            name = trimmedName,
            description = request.description.trim(),
            createdAt = now,
            updatedAt = now
        )

        val savedDepartment = departmentRepository.save(department)
        return toDepartmentResponse(savedDepartment)
    }

    @Transactional
    fun updateDepartment(id: Long, request: DepartmentRequest): DepartmentResponse? {
        val existingDeptOptional = departmentRepository.findById(id)
        if (existingDeptOptional.isEmpty) {
            return null
        }

        val trimmedName = request.name.trim()

        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A department with this name already exists.")
        }

        val department = existingDeptOptional.get()
        department.name = trimmedName
        department.description = request.description.trim()
        department.updatedAt = LocalDateTime.now()

        val updatedDepartment = departmentRepository.save(department)
        return toDepartmentResponse(updatedDepartment)
    }

    @Transactional
    fun deleteDepartment(id: Long): Boolean {
        val departmentOptional = departmentRepository.findById(id)
        if (departmentOptional.isEmpty) {
            return false
        }

        val docCount = doctorRepository.countByDepartmentId(id)
        val patCount = patientRepository.countByDepartmentId(id)
        if (docCount > 0 || patCount > 0) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot delete department with active doctors or patients assigned.")
        }

        departmentRepository.deleteById(id)
        return true
    }

    fun getTotalDepartmentCount(): Long {
        return departmentRepository.count()
    }

    @Synchronized
    private fun generateUniqueDepartmentId(): String {
        val lastDept = departmentRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastDept.isPresent) {
            val lastIdStr = lastDept.get().departmentId
            if (lastIdStr.startsWith("DEPT-")) {
                val numPart = lastIdStr.removePrefix("DEPT-").toLongOrNull()
                if (numPart != null) {
                    nextIdNum = numPart + 1
                }
            } else {
                nextIdNum = (lastDept.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("DEPT-%04d", nextIdNum)
        while (departmentRepository.existsByDepartmentId(candidateId)) {
            nextIdNum++
            candidateId = String.format("DEPT-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toDepartmentResponse(department: Department): DepartmentResponse {
        val deptId = department.id ?: 0L
        val doctorCount = if (deptId > 0) doctorRepository.countByDepartmentId(deptId) else 0L
        val patientCount = if (deptId > 0) patientRepository.countByDepartmentId(deptId) else 0L

        return DepartmentResponse(
            id = deptId,
            departmentId = department.departmentId,
            name = department.name,
            description = department.description,
            createdAt = department.createdAt,
            updatedAt = department.updatedAt,
            doctorCount = doctorCount,
            patientCount = patientCount
        )
    }
}

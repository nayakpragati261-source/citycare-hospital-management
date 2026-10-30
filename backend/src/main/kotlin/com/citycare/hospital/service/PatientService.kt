package com.citycare.hospital.service

import com.citycare.hospital.dto.PatientRequest
import com.citycare.hospital.dto.PatientResponse
import com.citycare.hospital.model.Department
import com.citycare.hospital.model.NotificationType
import com.citycare.hospital.model.Patient
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.DepartmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.PatientRepository
import com.citycare.hospital.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class PatientService(
    private val patientRepository: PatientRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val doctorRepository: DoctorRepository,
    private val notificationService: NotificationService
) {

    fun getAllPatients(): List<PatientResponse> {
        return patientRepository.findAll()
            .map { toPatientResponse(it) }
    }

    fun getPatientsForUser(userEmail: String?, userRole: Role): List<PatientResponse> {
        if (userRole == Role.ADMIN) {
            return getAllPatients()
        }
        val doctor = doctorRepository.findByUserEmail(userEmail ?: "")
            .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }

        val deptId = doctor.department?.id
            ?: return emptyList()

        return patientRepository.findAllByDepartmentId(deptId)
            .map { toPatientResponse(it) }
    }

    fun getPatientById(id: Long): PatientResponse? {
        return patientRepository.findById(id)
            .map { toPatientResponse(it) }
            .orElse(null)
    }

    fun getPatientByIdForUser(id: Long, userEmail: String?, userRole: Role): PatientResponse {
        val patient = patientRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found with ID: $id") }

        if (userRole == Role.DOCTOR) {
            val doctor = doctorRepository.findByUserEmail(userEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }

            if (patient.department == null || patient.department?.id != doctor.department?.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. Patient belongs to a different department.")
            }
        }

        return toPatientResponse(patient)
    }

    @Transactional
    fun createPatient(request: PatientRequest): PatientResponse {
        val nextPatientId = generateUniquePatientId()
        val now = LocalDateTime.now()

        var dept: Department? = null
        if (request.departmentId != null) {
            dept = departmentRepository.findById(request.departmentId).orElse(null)
        }
        if (dept == null) {
            dept = departmentRepository.findByNameIgnoreCase("General Medicine").orElse(null)
        }

        val patient = Patient(
            patientId = nextPatientId,
            name = request.name.trim(),
            age = request.age ?: 0,
            gender = request.gender.trim(),
            disease = request.disease.trim(),
            phone = request.phone.trim(),
            address = request.address.trim(),
            department = dept,
            createdAt = now,
            updatedAt = now
        )

        val savedPatient = patientRepository.save(patient)

        try {
            val adminUsers = userRepository.findAll().filter { it.role == Role.ADMIN && it.active }
            for (admin in adminUsers) {
                notificationService.createNotification(
                    user = admin,
                    title = "New Patient Registered",
                    message = "Patient ${savedPatient.name} (${savedPatient.patientId}) has been registered.",
                    type = NotificationType.PATIENT_REGISTERED
                )
            }
        } catch (e: Exception) {
            // Ignore notification failure
        }

        return toPatientResponse(savedPatient)
    }

    @Transactional
    fun updatePatient(id: Long, request: PatientRequest): PatientResponse? {
        val existingPatientOptional = patientRepository.findById(id)
        if (existingPatientOptional.isEmpty) {
            return null
        }

        val existingPatient = existingPatientOptional.get()
        existingPatient.name = request.name.trim()
        existingPatient.age = request.age ?: 0
        existingPatient.gender = request.gender.trim()
        existingPatient.disease = request.disease.trim()
        existingPatient.phone = request.phone.trim()
        existingPatient.address = request.address.trim()
        if (request.departmentId != null) {
            val dept = departmentRepository.findById(request.departmentId).orElse(null)
            if (dept != null) {
                existingPatient.department = dept
            }
        }
        existingPatient.updatedAt = LocalDateTime.now()

        val updatedPatient = patientRepository.save(existingPatient)
        return toPatientResponse(updatedPatient)
    }

    @Transactional
    fun deletePatient(id: Long): Boolean {
        if (!patientRepository.existsById(id)) {
            return false
        }
        patientRepository.deleteById(id)
        return true
    }

    fun getTotalPatientCount(): Long {
        return patientRepository.count()
    }

    fun getPatientCountForDepartment(departmentId: Long): Long {
        return patientRepository.countByDepartmentId(departmentId)
    }

    @Synchronized
    private fun generateUniquePatientId(): String {
        val allPatients = patientRepository.findAll()
        var maxNum = 0L
        for (p in allPatients) {
            val pId = p.patientId
            if (pId.startsWith("PAT-")) {
                val numStr = pId.removePrefix("PAT-")
                val num = numStr.toLongOrNull()
                if (num != null && num > maxNum) {
                    maxNum = num
                }
            }
        }
        var nextIdNum = maxNum + 1
        var candidateId = String.format("PAT-%04d", nextIdNum)
        while (patientRepository.existsByPatientId(candidateId)) {
            nextIdNum++
            candidateId = String.format("PAT-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toPatientResponse(patient: Patient): PatientResponse {
        return PatientResponse(
            id = patient.id ?: 0L,
            patientId = patient.patientId,
            name = patient.name,
            age = patient.age,
            gender = patient.gender,
            disease = patient.disease,
            phone = patient.phone,
            address = patient.address,
            departmentId = patient.department?.id,
            departmentName = patient.department?.name,
            createdAt = patient.createdAt,
            updatedAt = patient.updatedAt
        )
    }
}

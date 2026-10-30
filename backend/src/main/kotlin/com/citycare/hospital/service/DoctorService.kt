package com.citycare.hospital.service

import com.citycare.hospital.dto.CreateDoctorAccountRequest
import com.citycare.hospital.dto.DoctorRequest
import com.citycare.hospital.dto.DoctorResponse
import com.citycare.hospital.dto.ResetPasswordRequest
import com.citycare.hospital.dto.UserSummaryResponse
import com.citycare.hospital.model.Department
import com.citycare.hospital.model.Doctor
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.User
import com.citycare.hospital.repository.DepartmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class DoctorService(
    private val doctorRepository: DoctorRepository,
    private val userRepository: UserRepository,
    private val departmentRepository: DepartmentRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun resetDoctorPassword(doctorId: Long, request: ResetPasswordRequest): Boolean {
        if (request.newPassword != request.confirmPassword) {
            throw IllegalArgumentException("New password and confirm password do not match.")
        }
        if (request.newPassword.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters long.")
        }

        val doctor = doctorRepository.findById(doctorId).orElseThrow {
            IllegalArgumentException("Doctor profile not found with ID: $doctorId")
        }

        val user = doctor.user ?: userRepository.findByEmail(doctor.email).orElseThrow {
            IllegalArgumentException("Associated user account not found for doctor.")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
        return true
    }

    fun getAllDoctors(): List<DoctorResponse> {
        return doctorRepository.findAll()
            .map { toDoctorResponse(it) }
    }

    fun getDoctorById(id: Long): DoctorResponse? {
        return doctorRepository.findById(id)
            .map { toDoctorResponse(it) }
            .orElse(null)
    }

    fun getDoctorByUserId(userId: Long): DoctorResponse? {
        return doctorRepository.findByUserId(userId)
            .map { toDoctorResponse(it) }
            .orElse(null)
    }

    fun getAvailableDoctorUsers(): List<UserSummaryResponse> {
        return userRepository.findAll()
            .filter { it.role == Role.DOCTOR && it.active && !doctorRepository.existsByUserId(it.id!!) }
            .map { UserSummaryResponse(id = it.id!!, name = it.name, email = it.email, role = it.role.name) }
    }

    @Transactional
    fun createDoctor(request: DoctorRequest): DoctorResponse {
        val userOptional = userRepository.findByEmail(request.userEmail.trim())
        if (userOptional.isEmpty) {
            throw IllegalArgumentException("No user found with email: ${request.userEmail}")
        }

        val user = userOptional.get()
        if (user.role != Role.DOCTOR) {
            throw IllegalArgumentException("Only users with DOCTOR role can be linked to a doctor profile.")
        }

        if (!user.active) {
            throw IllegalArgumentException("The specified user account is inactive.")
        }

        if (doctorRepository.existsByUserId(user.id!!)) {
            throw IllegalArgumentException("This user is already linked to a doctor profile.")
        }

        val nextDoctorId = generateUniqueDoctorId()
        val now = LocalDateTime.now()
        val dept = departmentRepository.findByNameIgnoreCase(request.specialization.trim()).orElse(null)

        val doctor = Doctor(
            doctorId = nextDoctorId,
            user = user,
            name = user.name,
            email = user.email,
            specialization = request.specialization.trim(),
            department = dept,
            experience = request.experience ?: 0,
            phone = request.phone.trim(),
            availability = request.availability.trim(),
            createdAt = now,
            updatedAt = now
        )

        val savedDoctor = doctorRepository.save(doctor)
        return toDoctorResponse(savedDoctor)
    }

    @Transactional
    fun createDoctorAccount(request: CreateDoctorAccountRequest): DoctorResponse {
        val emailClean = request.email.trim().lowercase()
        if (userRepository.existsByEmail(emailClean) || doctorRepository.existsByUserEmail(emailClean) || doctorRepository.existsByEmail(emailClean)) {
            throw IllegalArgumentException("A user account or doctor profile with email '$emailClean' already exists.")
        }

        val hashedPassword = passwordEncoder.encode(request.password)

        val user = User(
            name = request.name.trim(),
            email = emailClean,
            passwordHash = hashedPassword,
            role = Role.DOCTOR,
            active = true
        )
        val savedUser = userRepository.save(user)

        var dept: Department? = null
        if (request.departmentId != null) {
            dept = departmentRepository.findById(request.departmentId).orElse(null)
        }
        if (dept == null && !request.departmentName.isNullOrBlank()) {
            dept = departmentRepository.findByNameIgnoreCase(request.departmentName.trim()).orElse(null)
        }
        if (dept == null) {
            dept = departmentRepository.findByNameIgnoreCase(request.specialization.trim()).orElse(null)
        }

        val nextDoctorId = generateUniqueDoctorId()
        val now = LocalDateTime.now()

        val doctor = Doctor(
            doctorId = nextDoctorId,
            user = savedUser,
            name = savedUser.name,
            email = savedUser.email,
            specialization = request.specialization.trim(),
            department = dept,
            experience = request.experience ?: 0,
            phone = request.phone.trim(),
            availability = request.availability.trim(),
            createdAt = now,
            updatedAt = now
        )

        val savedDoctor = doctorRepository.save(doctor)
        return toDoctorResponse(savedDoctor)
    }

    @Transactional
    fun updateDoctor(id: Long, request: DoctorRequest): DoctorResponse? {
        val existingDoctorOptional = doctorRepository.findById(id)
        if (existingDoctorOptional.isEmpty) {
            return null
        }

        val doctor = existingDoctorOptional.get()
        doctor.specialization = request.specialization.trim()
        val dept = departmentRepository.findByNameIgnoreCase(request.specialization.trim()).orElse(null)
        if (dept != null) {
            doctor.department = dept
        }
        doctor.experience = request.experience ?: 0
        doctor.phone = request.phone.trim()
        doctor.availability = request.availability.trim()
        doctor.updatedAt = LocalDateTime.now()

        // Keep name and email in sync with linked user if present
        doctor.user?.let { u ->
            doctor.name = u.name
            doctor.email = u.email
        }

        val updatedDoctor = doctorRepository.save(doctor)
        return toDoctorResponse(updatedDoctor)
    }

    @Transactional
    fun deleteDoctor(id: Long): Boolean {
        if (!doctorRepository.existsById(id)) {
            return false
        }
        // Deletes the Doctor profile from doctors table without deleting the User record
        doctorRepository.deleteById(id)
        return true
    }

    fun getTotalDoctorCount(): Long {
        return doctorRepository.count()
    }

    @Synchronized
    private fun generateUniqueDoctorId(): String {
        val allDoctors = doctorRepository.findAll()
        var maxNum = 0L
        for (doc in allDoctors) {
            val docId = doc.doctorId
            if (docId.startsWith("DOC-")) {
                val numStr = docId.removePrefix("DOC-")
                val num = numStr.toLongOrNull()
                if (num != null && num > maxNum) {
                    maxNum = num
                }
            }
        }
        var nextIdNum = maxNum + 1
        var candidateId = String.format("DOC-%04d", nextIdNum)
        while (doctorRepository.existsByDoctorId(candidateId)) {
            nextIdNum++
            candidateId = String.format("DOC-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toDoctorResponse(doctor: Doctor): DoctorResponse {
        val dept = doctor.department ?: departmentRepository.findByNameIgnoreCase(doctor.specialization).orElse(null)
        return DoctorResponse(
            id = doctor.id ?: 0L,
            doctorId = doctor.doctorId,
            userId = doctor.user?.id ?: 0L,
            name = doctor.name.ifBlank { doctor.user?.name ?: "" },
            email = doctor.email.ifBlank { doctor.user?.email ?: "" },
            specialization = doctor.specialization,
            departmentId = dept?.id,
            departmentName = dept?.name ?: doctor.specialization,
            experience = doctor.experience,
            phone = doctor.phone,
            availability = doctor.availability,
            createdAt = doctor.createdAt,
            updatedAt = doctor.updatedAt
        )
    }
}

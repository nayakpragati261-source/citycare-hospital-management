package com.citycare.hospital.config

import com.citycare.hospital.dto.DepartmentRequest
import com.citycare.hospital.model.Doctor
import com.citycare.hospital.model.Patient
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.DepartmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.PatientRepository
import com.citycare.hospital.repository.UserRepository
import com.citycare.hospital.service.DepartmentService
import com.citycare.hospital.service.UserService
import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class DataInitializer(
    private val userService: UserService,
    private val doctorRepository: DoctorRepository,
    private val departmentService: DepartmentService,
    private val departmentRepository: DepartmentRepository,
    private val userRepository: UserRepository,
    private val patientRepository: PatientRepository,
    private val passwordEncoder: PasswordEncoder
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        // 0. Clean up legacy/test doctors (DOC-8888, DOC-7777, DOC-9999) & orphan test user accounts if present
        val testDoctorIds = setOf("DOC-8888", "DOC-7777", "DOC-9999")
        val testUserEmails = setOf("other.doc.mr@citycare.com", "other.doc.rx@citycare.com", "other.doctor@citycare.com")

        for (docId in testDoctorIds) {
            val docOpt = doctorRepository.findByDoctorId(docId)
            if (docOpt.isPresent) {
                val doc = docOpt.get()
                val linkedUser = doc.user
                doctorRepository.delete(doc)
                if (linkedUser != null && testUserEmails.contains(linkedUser.email)) {
                    if (doctorRepository.findByUserEmail(linkedUser.email).isEmpty) {
                        userRepository.delete(linkedUser)
                    }
                }
            }
        }

        for (email in testUserEmails) {
            val docOpt = doctorRepository.findByUserEmail(email)
            docOpt.ifPresent { doctorRepository.delete(it) }
            val userOpt = userRepository.findByEmail(email)
            userOpt.ifPresent { userRepository.delete(it) }
        }

        // 1. Seed / Verify Starter Departments Safely
        val starterDepartments = listOf(
            DepartmentRequest("General Medicine", "Primary care, diagnosis and treatment of common medical conditions."),
            DepartmentRequest("Cardiology", "Diagnosis and treatment of heart and cardiovascular conditions."),
            DepartmentRequest("Orthopedics", "Treatment of bones, joints, muscles and related conditions."),
            DepartmentRequest("Pediatrics", "Medical care and treatment for infants, children and adolescents."),
            DepartmentRequest("Dermatology", "Diagnosis and treatment of skin, hair and nail conditions.")
        )

        for (deptReq in starterDepartments) {
            if (!departmentRepository.existsByNameIgnoreCase(deptReq.name)) {
                try {
                    departmentService.createDepartment(deptReq)
                } catch (e: Exception) {
                    // Ignore if already seeded
                }
            }
        }

        // 2. Seed / Verify Demo Admin Account
        val targetEmail = "pragati@citycare.com"
        val targetName = "Pragati"
        val targetPassword = "pragati123"

        val existingPragati = userService.findByEmail(targetEmail)
        val existingOldAdmin = userService.findByEmail("admin@citycare.com")

        val adminUser = when {
            existingPragati.isPresent -> existingPragati.get()
            existingOldAdmin.isPresent -> existingOldAdmin.get()
            else -> null
        }

        if (adminUser == null) {
            userService.createUser(
                name = targetName,
                email = targetEmail,
                plainPassword = targetPassword,
                role = Role.ADMIN
            )
        } else {
            var updated = false
            if (adminUser.email != targetEmail) {
                adminUser.email = targetEmail
                updated = true
            }
            if (adminUser.name != targetName) {
                adminUser.name = targetName
                updated = true
            }
            if (adminUser.role != Role.ADMIN) {
                adminUser.role = Role.ADMIN
                updated = true
            }
            if (!adminUser.active) {
                adminUser.active = true
                updated = true
            }
            if (!passwordEncoder.matches(targetPassword, adminUser.passwordHash)) {
                adminUser.passwordHash = passwordEncoder.encode(targetPassword)
                updated = true
            }
            if (updated) {
                userRepository.save(adminUser)
            }
        }

        // 3. Seed / Verify 5 Demo Doctors (User + Doctor Profile + Department Link)
        data class DemoDoctorSeed(
            val doctorId: String,
            val name: String,
            val email: String,
            val plainPassword: String,
            val specialization: String,
            val experience: Int,
            val phone: String,
            val availability: String
        )

        val demoDoctors = listOf(
            DemoDoctorSeed(
                doctorId = "DOC-0001",
                name = "Dr. Rahul Sharma",
                email = "rahul.sharma@citycare.com",
                plainPassword = "rahul123",
                specialization = "General Medicine",
                experience = 8,
                phone = "9876543211",
                availability = "Monday - Friday, 9:00 AM - 2:00 PM"
            ),
            DemoDoctorSeed(
                doctorId = "DOC-0002",
                name = "Dr. Priya Mehta",
                email = "priya.mehta@citycare.com",
                plainPassword = "priya123",
                specialization = "Cardiology",
                experience = 10,
                phone = "9876543212",
                availability = "Monday - Friday, 10:00 AM - 3:00 PM"
            ),
            DemoDoctorSeed(
                doctorId = "DOC-0003",
                name = "Dr. Amit Patil",
                email = "amit.patil@citycare.com",
                plainPassword = "amit123",
                specialization = "Orthopedics",
                experience = 7,
                phone = "9876543213",
                availability = "Tuesday - Saturday, 9:00 AM - 1:00 PM"
            ),
            DemoDoctorSeed(
                doctorId = "DOC-0004",
                name = "Dr. Sneha Joshi",
                email = "sneha.joshi@citycare.com",
                plainPassword = "sneha123",
                specialization = "Pediatrics",
                experience = 6,
                phone = "9876543214",
                availability = "Monday - Friday, 11:00 AM - 4:00 PM"
            ),
            DemoDoctorSeed(
                doctorId = "DOC-0005",
                name = "Dr. Karan Shah",
                email = "karan.shah@citycare.com",
                plainPassword = "karan123",
                specialization = "Dermatology",
                experience = 9,
                phone = "9876543215",
                availability = "Monday - Saturday, 10:00 AM - 2:00 PM"
            )
        )

        for (dSeed in demoDoctors) {
            // A. Ensure User account exists & is valid
            val userOpt = userService.findByEmail(dSeed.email)
            val user = if (userOpt.isEmpty) {
                userService.createUser(
                    name = dSeed.name,
                    email = dSeed.email,
                    plainPassword = dSeed.plainPassword,
                    role = Role.DOCTOR
                )
            } else {
                val existingUser = userOpt.get()
                var updated = false
                if (existingUser.role != Role.DOCTOR) {
                    existingUser.role = Role.DOCTOR
                    updated = true
                }
                if (!existingUser.active) {
                    existingUser.active = true
                    updated = true
                }
                if (existingUser.name != dSeed.name) {
                    existingUser.name = dSeed.name
                    updated = true
                }
                if (existingUser.passwordHash.isBlank() || !passwordEncoder.matches(dSeed.plainPassword, existingUser.passwordHash)) {
                    existingUser.passwordHash = passwordEncoder.encode(dSeed.plainPassword)
                    updated = true
                }
                if (updated) userRepository.save(existingUser) else existingUser
            }

            // B. Ensure Doctor Profile exists & is linked to Department
            val doctorOpt = doctorRepository.findByUserEmail(dSeed.email)
            val dept = departmentRepository.findByNameIgnoreCase(dSeed.specialization).orElse(null)
            val now = LocalDateTime.now()

            var targetDocId = dSeed.doctorId
            if (doctorOpt.isEmpty && doctorRepository.existsByDoctorId(targetDocId)) {
                targetDocId = generateUniqueDoctorId()
            }

            if (doctorOpt.isEmpty) {
                val doctor = Doctor(
                    doctorId = targetDocId,
                    user = user,
                    name = dSeed.name,
                    email = dSeed.email,
                    specialization = dSeed.specialization,
                    department = dept,
                    experience = dSeed.experience,
                    phone = dSeed.phone,
                    availability = dSeed.availability,
                    createdAt = now,
                    updatedAt = now
                )
                doctorRepository.save(doctor)
            } else {
                val doctor = doctorOpt.get()
                var updated = false
                if (doctor.doctorId != dSeed.doctorId && !doctorRepository.existsByDoctorId(dSeed.doctorId)) {
                    doctor.doctorId = dSeed.doctorId
                    updated = true
                }
                if (doctor.name != dSeed.name) {
                    doctor.name = dSeed.name
                    updated = true
                }
                if (doctor.specialization != dSeed.specialization) {
                    doctor.specialization = dSeed.specialization
                    updated = true
                }
                if (dept != null && (doctor.department == null || doctor.department?.id != dept.id)) {
                    doctor.department = dept
                    updated = true
                }
                if (doctor.experience != dSeed.experience) {
                    doctor.experience = dSeed.experience
                    updated = true
                }
                if (doctor.phone != dSeed.phone) {
                    doctor.phone = dSeed.phone
                    updated = true
                }
                if (doctor.availability != dSeed.availability) {
                    doctor.availability = dSeed.availability
                    updated = true
                }
                if (updated) {
                    doctor.updatedAt = now
                    doctorRepository.save(doctor)
                }
            }
        }

        // 4. Seed / Verify 4 Demo Patients with Departments
        data class DemoPatientSeed(
            val patientId: String,
            val name: String,
            val age: Int,
            val gender: String,
            val disease: String,
            val phone: String,
            val address: String,
            val departmentName: String
        )

        val demoPatients = listOf(
            DemoPatientSeed("PAT-0001", "Aisha Khan", 28, "Female", "Migraine", "9876543210", "Andheri East, Mumbai", "General Medicine"),
            DemoPatientSeed("PAT-0002", "Rohan Mehta", 45, "Male", "Hypertension", "9988776655", "Powai, Mumbai", "Cardiology"),
            DemoPatientSeed("PAT-0003", "Neha Patil", 34, "Female", "Diabetes", "9765432180", "Ghatkopar, Mumbai", "Orthopedics"),
            DemoPatientSeed("PAT-0004", "Arjun Deshmukh", 52, "Male", "Arthritis", "9654321870", "Vikhroli, Mumbai", "General Medicine")
        )

        for (pSeed in demoPatients) {
            val deptOpt = departmentRepository.findByNameIgnoreCase(pSeed.departmentName)
            val dept = deptOpt.orElse(null)

            val pOptByCode = patientRepository.findByPatientId(pSeed.patientId)
            val pOptByName = patientRepository.findAll().find { it.name.equals(pSeed.name, ignoreCase = true) }

            if (pOptByCode.isPresent || pOptByName != null) {
                val p = pOptByCode.orElse(pOptByName)
                var updated = false
                if (p.name.equals("Test Patient", ignoreCase = true) ||
                    p.name.equals("Second Patient", ignoreCase = true) ||
                    p.name.contains("Test", ignoreCase = true) ||
                    p.name.contains("Sample", ignoreCase = true)) {
                    p.name = pSeed.name
                    p.age = pSeed.age
                    p.gender = pSeed.gender
                    p.disease = pSeed.disease
                    p.phone = pSeed.phone
                    p.address = pSeed.address
                    updated = true
                }
                if (p.department == null && dept != null) {
                    p.department = dept
                    updated = true
                }
                if (updated) {
                    p.updatedAt = LocalDateTime.now()
                    patientRepository.save(p)
                }
            } else {
                var targetPatientId = pSeed.patientId
                if (patientRepository.existsByPatientId(targetPatientId)) {
                    targetPatientId = generateUniquePatientId()
                }
                val now = LocalDateTime.now()
                val p = Patient(
                    patientId = targetPatientId,
                    name = pSeed.name,
                    age = pSeed.age,
                    gender = pSeed.gender,
                    disease = pSeed.disease,
                    phone = pSeed.phone,
                    address = pSeed.address,
                    department = dept,
                    createdAt = now,
                    updatedAt = now
                )
                patientRepository.save(p)
            }
        }
        
        // Also assign any existing patients without department to General Medicine if needed
        val unassignedPatients = patientRepository.findAll().filter { it.department == null }
        if (unassignedPatients.isNotEmpty()) {
            val defaultDept = departmentRepository.findByNameIgnoreCase("General Medicine").orElse(null)
            if (defaultDept != null) {
                for (p in unassignedPatients) {
                    p.department = defaultDept
                    p.updatedAt = LocalDateTime.now()
                    patientRepository.save(p)
                }
            }
        }
    }

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
}

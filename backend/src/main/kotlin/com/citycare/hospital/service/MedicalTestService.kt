package com.citycare.hospital.service

import com.citycare.hospital.dto.MedicalTestRequest
import com.citycare.hospital.dto.MedicalTestResponse
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.MedicalTest
import com.citycare.hospital.model.MedicalTestStatus
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.NotificationType
import com.citycare.hospital.repository.AppointmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.MedicalTestRepository
import com.citycare.hospital.repository.PatientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class MedicalTestService(
    private val medicalTestRepository: MedicalTestRepository,
    private val patientRepository: PatientRepository,
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
    private val notificationService: NotificationService
) {

    fun getAllMedicalTests(): List<MedicalTestResponse> {
        return medicalTestRepository.findAll()
            .sortedByDescending { it.testDate }
            .map { toMedicalTestResponse(it) }
    }

    fun getMedicalTestsForDoctor(userEmail: String): List<MedicalTestResponse> {
        val doctor = doctorRepository.findByUserEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found for user.") }
        val deptId = doctor.department?.id
        val tests = if (deptId != null) {
            medicalTestRepository.findByPatientDepartmentIdOrDoctorDepartmentId(deptId, deptId)
        } else {
            medicalTestRepository.findByDoctorId(doctor.id!!)
        }
        return tests
            .sortedByDescending { it.testDate }
            .map { toMedicalTestResponse(it) }
    }

    fun getMedicalTestById(id: Long, currentUserEmail: String?, currentUserRole: Role): MedicalTestResponse {
        val test = medicalTestRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical test not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val doctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            val doctorDeptId = doctor.department?.id
            val isAuthorized = (doctorDeptId != null && (test.patient?.department?.id == doctorDeptId || test.doctor?.department?.id == doctorDeptId)) ||
                    test.doctor?.id == doctor.id
            if (!isAuthorized) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this record.")
            }
        }

        return toMedicalTestResponse(test)
    }

    @Transactional
    fun createMedicalTest(
        request: MedicalTestRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): MedicalTestResponse {
        val patientId = request.patientId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is required.")
        val doctorId = request.doctorId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is required.")
        val testDate = request.testDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test date is required.")

        if (request.testName.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Test name is required.")

        val targetStatus = request.status ?: MedicalTestStatus.PENDING
        val resultStr = request.result?.trim()?.ifBlank { null }

        if (targetStatus == MedicalTestStatus.COMPLETED && resultStr.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A result is required when the medical test is completed.")
        }

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (loggedInDoctor.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create medical tests for yourself.")
            }
        }

        var appointment: Appointment? = null
        if (request.appointmentId != null) {
            appointment = appointmentRepository.findById(request.appointmentId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

            if (appointment.patient?.id != patient.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The selected appointment does not belong to the selected patient.")
            }
            if (appointment.doctor?.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The selected appointment does not belong to the selected doctor.")
            }
        }

        val testIdStr = generateUniqueTestId()
        val now = LocalDateTime.now()

        val test = MedicalTest(
            testId = testIdStr,
            patient = patient,
            doctor = doctor,
            appointment = appointment,
            testName = request.testName.trim(),
            testDate = testDate,
            result = resultStr,
            status = targetStatus,
            createdAt = now,
            updatedAt = now
        )

        val saved = medicalTestRepository.save(test)

        if (saved.status == MedicalTestStatus.COMPLETED) {
            saved.doctor?.user?.let { doctorUser ->
                try {
                    notificationService.createNotification(
                        user = doctorUser,
                        title = "Medical Test Completed",
                        message = "Medical test ${saved.testName} (${saved.testId}) for ${saved.patient?.name} is completed.",
                        type = NotificationType.MEDICAL_TEST_COMPLETED
                    )
                } catch (e: Exception) {
                    // Ignore notification failure
                }
            }
        }

        return toMedicalTestResponse(saved)
    }

    @Transactional
    fun updateMedicalTest(
        id: Long,
        request: MedicalTestRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): MedicalTestResponse {
        val test = medicalTestRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical test not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (test.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        val patientId = request.patientId ?: test.patient!!.id!!
        val doctorId = request.doctorId ?: test.doctor!!.id!!
        val testDate = request.testDate ?: test.testDate
        val targetStatus = request.status ?: test.status
        val resultStr = request.result?.trim()?.ifBlank { null } ?: test.result

        if (targetStatus == MedicalTestStatus.COMPLETED && resultStr.isNullOrBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A result is required when the medical test is completed.")
        }

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }
        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        var appointment: Appointment? = test.appointment
        if (request.appointmentId != null) {
            appointment = appointmentRepository.findById(request.appointmentId)
                .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

            if (appointment.patient?.id != patient.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The selected appointment does not belong to the selected patient.")
            }
            if (appointment.doctor?.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.CONFLICT, "The selected appointment does not belong to the selected doctor.")
            }
        }

        val wasPending = test.status == MedicalTestStatus.PENDING
        test.patient = patient
        test.doctor = doctor
        test.appointment = appointment
        if (request.testName.isNotBlank()) test.testName = request.testName.trim()
        test.testDate = testDate
        test.result = resultStr
        test.status = targetStatus
        test.updatedAt = LocalDateTime.now()

        val saved = medicalTestRepository.save(test)

        if (wasPending && saved.status == MedicalTestStatus.COMPLETED) {
            saved.doctor?.user?.let { doctorUser ->
                try {
                    notificationService.createNotification(
                        user = doctorUser,
                        title = "Medical Test Completed",
                        message = "Medical test ${saved.testName} (${saved.testId}) for ${saved.patient?.name} is completed.",
                        type = NotificationType.MEDICAL_TEST_COMPLETED
                    )
                } catch (e: Exception) {
                    // Ignore notification failure
                }
            }
        }

        return toMedicalTestResponse(saved)
    }

    @Transactional
    fun deleteMedicalTest(id: Long, currentUserEmail: String?, currentUserRole: Role): Boolean {
        val test = medicalTestRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical test not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (test.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        medicalTestRepository.delete(test)
        return true
    }

    fun getTotalMedicalTestCount(): Long {
        return medicalTestRepository.count()
    }

    fun getCountByStatus(status: MedicalTestStatus): Long {
        return medicalTestRepository.countByStatus(status)
    }

    @Synchronized
    private fun generateUniqueTestId(): String {
        val lastTest = medicalTestRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastTest.isPresent) {
            val lastIdStr = lastTest.get().testId
            if (lastIdStr.startsWith("TEST-")) {
                val numPart = lastIdStr.removePrefix("TEST-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastTest.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("TEST-%04d", nextIdNum)
        while (medicalTestRepository.existsByTestId(candidateId)) {
            nextIdNum++
            candidateId = String.format("TEST-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toMedicalTestResponse(test: MedicalTest): MedicalTestResponse {
        val patient = test.patient!!
        val doctor = test.doctor!!
        val appt = test.appointment

        return MedicalTestResponse(
            id = test.id ?: 0L,
            testId = test.testId,
            patientId = patient.id ?: 0L,
            patientName = patient.name,
            doctorId = doctor.id ?: 0L,
            doctorName = doctor.name,
            appointmentId = appt?.id,
            appointmentCode = appt?.appointmentId,
            testName = test.testName,
            testDate = test.testDate,
            result = test.result,
            status = test.status,
            createdAt = test.createdAt,
            updatedAt = test.updatedAt
        )
    }
}

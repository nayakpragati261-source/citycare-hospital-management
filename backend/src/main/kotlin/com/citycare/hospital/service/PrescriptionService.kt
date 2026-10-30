package com.citycare.hospital.service

import com.citycare.hospital.dto.PrescriptionRequest
import com.citycare.hospital.dto.PrescriptionResponse
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.Prescription
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.NotificationType
import com.citycare.hospital.repository.AppointmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.PatientRepository
import com.citycare.hospital.repository.PrescriptionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class PrescriptionService(
    private val prescriptionRepository: PrescriptionRepository,
    private val patientRepository: PatientRepository,
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository,
    private val notificationService: NotificationService
) {

    fun getAllPrescriptions(): List<PrescriptionResponse> {
        return prescriptionRepository.findAll()
            .sortedByDescending { it.prescriptionDate }
            .map { toPrescriptionResponse(it) }
    }

    fun getPrescriptionsForDoctor(userEmail: String): List<PrescriptionResponse> {
        val doctor = doctorRepository.findByUserEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found for user.") }
        val deptId = doctor.department?.id
        val prescriptions = if (deptId != null) {
            prescriptionRepository.findByPatientDepartmentIdOrDoctorDepartmentId(deptId, deptId)
        } else {
            prescriptionRepository.findByDoctorId(doctor.id!!)
        }
        return prescriptions
            .sortedByDescending { it.prescriptionDate }
            .map { toPrescriptionResponse(it) }
    }

    fun getPrescriptionById(id: Long, currentUserEmail: String?, currentUserRole: Role): PrescriptionResponse {
        val prescription = prescriptionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val doctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            val doctorDeptId = doctor.department?.id
            val isAuthorized = (doctorDeptId != null && (prescription.patient?.department?.id == doctorDeptId || prescription.doctor?.department?.id == doctorDeptId)) ||
                    prescription.doctor?.id == doctor.id
            if (!isAuthorized) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this record.")
            }
        }

        return toPrescriptionResponse(prescription)
    }

    @Transactional
    fun createPrescription(
        request: PrescriptionRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): PrescriptionResponse {
        val patientId = request.patientId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is required.")
        val doctorId = request.doctorId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is required.")
        val prescriptionDate = request.prescriptionDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Prescription date is required.")

        if (request.medicine.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Medicine is required.")
        if (request.dosage.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Dosage is required.")
        if (request.duration.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration is required.")

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (loggedInDoctor.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create prescriptions for yourself.")
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

        val rxId = generateUniquePrescriptionId()
        val now = LocalDateTime.now()

        val prescription = Prescription(
            prescriptionId = rxId,
            patient = patient,
            doctor = doctor,
            appointment = appointment,
            prescriptionDate = prescriptionDate,
            medicine = request.medicine.trim(),
            dosage = request.dosage.trim(),
            duration = request.duration.trim(),
            instructions = request.instructions?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )

        val saved = prescriptionRepository.save(prescription)

        doctor.user?.let { doctorUser ->
            try {
                notificationService.createNotification(
                    user = doctorUser,
                    title = "Prescription Added",
                    message = "New prescription ${saved.prescriptionId} (${request.medicine.trim()}) created for ${patient.name}.",
                    type = NotificationType.PRESCRIPTION_ADDED
                )
            } catch (e: Exception) {
                // Ignore notification failure if any
            }
        }

        return toPrescriptionResponse(saved)
    }

    @Transactional
    fun updatePrescription(
        id: Long,
        request: PrescriptionRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): PrescriptionResponse {
        val prescription = prescriptionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (prescription.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        val patientId = request.patientId ?: prescription.patient!!.id!!
        val doctorId = request.doctorId ?: prescription.doctor!!.id!!
        val prescriptionDate = request.prescriptionDate ?: prescription.prescriptionDate

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }
        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        var appointment: Appointment? = prescription.appointment
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

        prescription.patient = patient
        prescription.doctor = doctor
        prescription.appointment = appointment
        prescription.prescriptionDate = prescriptionDate
        if (request.medicine.isNotBlank()) prescription.medicine = request.medicine.trim()
        if (request.dosage.isNotBlank()) prescription.dosage = request.dosage.trim()
        if (request.duration.isNotBlank()) prescription.duration = request.duration.trim()
        prescription.instructions = request.instructions?.trim()?.ifBlank { null }
        prescription.updatedAt = LocalDateTime.now()

        val saved = prescriptionRepository.save(prescription)
        return toPrescriptionResponse(saved)
    }

    @Transactional
    fun deletePrescription(id: Long, currentUserEmail: String?, currentUserRole: Role): Boolean {
        val prescription = prescriptionRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (prescription.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        prescriptionRepository.delete(prescription)
        return true
    }

    fun getTotalPrescriptionCount(): Long {
        return prescriptionRepository.count()
    }

    @Synchronized
    private fun generateUniquePrescriptionId(): String {
        val lastRx = prescriptionRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastRx.isPresent) {
            val lastIdStr = lastRx.get().prescriptionId
            if (lastIdStr.startsWith("RX-")) {
                val numPart = lastIdStr.removePrefix("RX-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastRx.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("RX-%04d", nextIdNum)
        while (prescriptionRepository.existsByPrescriptionId(candidateId)) {
            nextIdNum++
            candidateId = String.format("RX-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toPrescriptionResponse(prescription: Prescription): PrescriptionResponse {
        val patient = prescription.patient!!
        val doctor = prescription.doctor!!
        val appt = prescription.appointment

        return PrescriptionResponse(
            id = prescription.id ?: 0L,
            prescriptionId = prescription.prescriptionId,
            patientId = patient.id ?: 0L,
            patientName = patient.name,
            doctorId = doctor.id ?: 0L,
            doctorName = doctor.name,
            appointmentId = appt?.id,
            appointmentCode = appt?.appointmentId,
            prescriptionDate = prescription.prescriptionDate,
            medicine = prescription.medicine,
            dosage = prescription.dosage,
            duration = prescription.duration,
            instructions = prescription.instructions,
            createdAt = prescription.createdAt,
            updatedAt = prescription.updatedAt
        )
    }
}

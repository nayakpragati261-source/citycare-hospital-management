package com.citycare.hospital.service

import com.citycare.hospital.dto.MedicalRecordRequest
import com.citycare.hospital.dto.MedicalRecordResponse
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.MedicalRecord
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.AppointmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.MedicalRecordRepository
import com.citycare.hospital.repository.PatientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class MedicalRecordService(
    private val medicalRecordRepository: MedicalRecordRepository,
    private val patientRepository: PatientRepository,
    private val doctorRepository: DoctorRepository,
    private val appointmentRepository: AppointmentRepository
) {

    fun getAllMedicalRecords(): List<MedicalRecordResponse> {
        return medicalRecordRepository.findAll()
            .sortedByDescending { it.recordDate }
            .map { toMedicalRecordResponse(it) }
    }

    fun getMedicalRecordsForDoctor(userEmail: String): List<MedicalRecordResponse> {
        val doctor = doctorRepository.findByUserEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found for user.") }
        val deptId = doctor.department?.id
        val records = if (deptId != null) {
            medicalRecordRepository.findByPatientDepartmentIdOrDoctorDepartmentId(deptId, deptId)
        } else {
            medicalRecordRepository.findByDoctorId(doctor.id!!)
        }
        return records
            .sortedByDescending { it.recordDate }
            .map { toMedicalRecordResponse(it) }
    }

    fun getMedicalRecordById(id: Long, currentUserEmail: String?, currentUserRole: Role): MedicalRecordResponse {
        val record = medicalRecordRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val doctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            val doctorDeptId = doctor.department?.id
            val isAuthorized = (doctorDeptId != null && (record.patient?.department?.id == doctorDeptId || record.doctor?.department?.id == doctorDeptId)) ||
                    record.doctor?.id == doctor.id
            if (!isAuthorized) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this record.")
            }
        }

        return toMedicalRecordResponse(record)
    }

    @Transactional
    fun createMedicalRecord(
        request: MedicalRecordRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): MedicalRecordResponse {
        val patientId = request.patientId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is required.")
        val doctorId = request.doctorId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is required.")
        val recordDate = request.recordDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Record date is required.")

        if (request.diagnosis.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Diagnosis is required.")
        if (request.treatment.isBlank()) throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Treatment details are required.")

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (loggedInDoctor.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only create medical records for yourself.")
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

        val recordIdStr = generateUniqueRecordId()
        val now = LocalDateTime.now()

        val record = MedicalRecord(
            recordId = recordIdStr,
            patient = patient,
            doctor = doctor,
            appointment = appointment,
            previousDisease = request.previousDisease?.trim()?.ifBlank { null },
            diagnosis = request.diagnosis.trim(),
            treatment = request.treatment.trim(),
            recordDate = recordDate,
            notes = request.notes?.trim()?.ifBlank { null },
            createdAt = now,
            updatedAt = now
        )

        val saved = medicalRecordRepository.save(record)
        return toMedicalRecordResponse(saved)
    }

    @Transactional
    fun updateMedicalRecord(
        id: Long,
        request: MedicalRecordRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): MedicalRecordResponse {
        val record = medicalRecordRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (record.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        val patientId = request.patientId ?: record.patient!!.id!!
        val doctorId = request.doctorId ?: record.doctor!!.id!!
        val recordDate = request.recordDate ?: record.recordDate

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }
        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        var appointment: Appointment? = record.appointment
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

        record.patient = patient
        record.doctor = doctor
        record.appointment = appointment
        record.previousDisease = request.previousDisease?.trim()?.ifBlank { null }
        if (request.diagnosis.isNotBlank()) record.diagnosis = request.diagnosis.trim()
        if (request.treatment.isNotBlank()) record.treatment = request.treatment.trim()
        record.recordDate = recordDate
        record.notes = request.notes?.trim()?.ifBlank { null }
        record.updatedAt = LocalDateTime.now()

        val saved = medicalRecordRepository.save(record)
        return toMedicalRecordResponse(saved)
    }

    @Transactional
    fun deleteMedicalRecord(id: Long, currentUserEmail: String?, currentUserRole: Role): Boolean {
        val record = medicalRecordRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Medical record not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (record.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this record.")
            }
        }

        medicalRecordRepository.delete(record)
        return true
    }

    fun getTotalMedicalRecordCount(): Long {
        return medicalRecordRepository.count()
    }

    @Synchronized
    private fun generateUniqueRecordId(): String {
        val lastRecord = medicalRecordRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastRecord.isPresent) {
            val lastIdStr = lastRecord.get().recordId
            if (lastIdStr.startsWith("MR-")) {
                val numPart = lastIdStr.removePrefix("MR-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastRecord.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("MR-%04d", nextIdNum)
        while (medicalRecordRepository.existsByRecordId(candidateId)) {
            nextIdNum++
            candidateId = String.format("MR-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toMedicalRecordResponse(record: MedicalRecord): MedicalRecordResponse {
        val patient = record.patient!!
        val doctor = record.doctor!!
        val appt = record.appointment

        return MedicalRecordResponse(
            id = record.id ?: 0L,
            recordId = record.recordId,
            patientId = patient.id ?: 0L,
            patientName = patient.name,
            doctorId = doctor.id ?: 0L,
            doctorName = doctor.name,
            appointmentId = appt?.id,
            appointmentCode = appt?.appointmentId,
            previousDisease = record.previousDisease,
            diagnosis = record.diagnosis,
            treatment = record.treatment,
            recordDate = record.recordDate,
            notes = record.notes,
            createdAt = record.createdAt,
            updatedAt = record.updatedAt
        )
    }
}

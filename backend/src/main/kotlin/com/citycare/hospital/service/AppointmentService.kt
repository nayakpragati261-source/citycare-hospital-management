package com.citycare.hospital.service

import com.citycare.hospital.dto.AppointmentRequest
import com.citycare.hospital.dto.AppointmentResponse
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.AppointmentStatus
import com.citycare.hospital.model.NotificationType
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.AppointmentRepository
import com.citycare.hospital.repository.DepartmentRepository
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.PatientRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class AppointmentService(
    private val appointmentRepository: AppointmentRepository,
    private val patientRepository: PatientRepository,
    private val doctorRepository: DoctorRepository,
    private val departmentRepository: DepartmentRepository,
    private val notificationService: NotificationService
) {

    fun getAllAppointments(): List<AppointmentResponse> {
        return appointmentRepository.findAll()
            .sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenByDescending { it.appointmentTime })
            .map { toAppointmentResponse(it) }
    }

    fun getAppointmentsForDoctor(userEmail: String): List<AppointmentResponse> {
        val doctor = doctorRepository.findByUserEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor profile not found for user.") }
        val deptId = doctor.department?.id
        val appointments = if (deptId != null) {
            appointmentRepository.findByDepartmentId(deptId)
        } else {
            appointmentRepository.findByDoctorId(doctor.id!!)
        }
        return appointments
            .sortedWith(compareByDescending<Appointment> { it.appointmentDate }.thenByDescending { it.appointmentTime })
            .map { toAppointmentResponse(it) }
    }

    fun getAppointmentById(id: Long, currentUserEmail: String?, currentUserRole: Role): AppointmentResponse {
        val appointment = appointmentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val doctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (appointment.department?.id != doctor.department?.id && appointment.doctor?.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to view this appointment.")
            }
        }

        return toAppointmentResponse(appointment)
    }

    @Transactional
    fun createAppointment(
        request: AppointmentRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): AppointmentResponse {
        val patientId = request.patientId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Patient is required.")
        val doctorId = request.doctorId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Doctor is required.")
        val departmentId = request.departmentId ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Department is required.")
        val appointmentDate = request.appointmentDate ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment date is required.")
        val appointmentTime = request.appointmentTime ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Appointment time is required.")

        if (request.reason.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Reason for appointment is required.")
        }

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        val department = departmentRepository.findById(departmentId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (loggedInDoctor.id != doctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only schedule appointments for yourself.")
            }
        }

        val doctorDeptMatch = (doctor.department?.id == department.id) ||
                doctor.specialization.equals(department.name, ignoreCase = true)
        if (!doctorDeptMatch) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The selected doctor does not belong to the selected department.")
        }

        val doctorConflict = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            doctor.id!!,
            appointmentDate,
            appointmentTime,
            AppointmentStatus.CANCELLED
        )
        if (doctorConflict) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The doctor already has an appointment at this date and time.")
        }

        val patientConflict = appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
            patient.id!!,
            appointmentDate,
            appointmentTime,
            AppointmentStatus.CANCELLED
        )
        if (patientConflict) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The patient already has an appointment at this date and time.")
        }

        val appointmentIdStr = generateUniqueAppointmentId()
        val now = LocalDateTime.now()

        val appointment = Appointment(
            appointmentId = appointmentIdStr,
            patient = patient,
            doctor = doctor,
            department = department,
            appointmentDate = appointmentDate,
            appointmentTime = appointmentTime,
            reason = request.reason.trim(),
            status = request.status ?: AppointmentStatus.SCHEDULED,
            createdAt = now,
            updatedAt = now
        )

        val saved = appointmentRepository.save(appointment)

        doctor.user?.let { doctorUser ->
            try {
                notificationService.createNotification(
                    user = doctorUser,
                    title = "New Appointment Booked",
                    message = "${patient.name} has an appointment with you on ${appointmentDate} at ${appointmentTime} in ${department.name}.",
                    type = NotificationType.APPOINTMENT_SCHEDULED
                )
            } catch (e: Exception) {
                // Ignore notification failure
            }
        }

        return toAppointmentResponse(saved)
    }

    @Transactional
    fun updateAppointment(
        id: Long,
        request: AppointmentRequest,
        currentUserEmail: String?,
        currentUserRole: Role
    ): AppointmentResponse {
        val appointment = appointmentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }

            if (appointment.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to modify this appointment.")
            }
        }

        val targetStatus = request.status ?: appointment.status
        if (appointment.status == AppointmentStatus.CANCELLED && targetStatus != AppointmentStatus.CANCELLED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "This appointment has already been cancelled.")
        }
        if (appointment.status == AppointmentStatus.COMPLETED && targetStatus != AppointmentStatus.COMPLETED && targetStatus != AppointmentStatus.CANCELLED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify a completed appointment.")
        }

        val patientId = request.patientId ?: appointment.patient!!.id!!
        val doctorId = request.doctorId ?: appointment.doctor!!.id!!
        val departmentId = request.departmentId ?: appointment.department!!.id!!
        val appointmentDate = request.appointmentDate ?: appointment.appointmentDate
        val appointmentTime = request.appointmentTime ?: appointment.appointmentTime

        val patient = patientRepository.findById(patientId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Patient not found.") }

        val doctor = doctorRepository.findById(doctorId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor not found.") }

        val department = departmentRepository.findById(departmentId)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Department not found.") }

        val doctorDeptMatch = (doctor.department?.id == department.id) ||
                doctor.specialization.equals(department.name, ignoreCase = true)
        if (!doctorDeptMatch) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The selected doctor does not belong to the selected department.")
        }

        val doctorConflict = appointmentRepository.existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            doctor.id!!,
            appointmentDate,
            appointmentTime,
            AppointmentStatus.CANCELLED,
            id
        )
        if (doctorConflict) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The doctor already has an appointment at this date and time.")
        }

        val patientConflict = appointmentRepository.existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
            patient.id!!,
            appointmentDate,
            appointmentTime,
            AppointmentStatus.CANCELLED,
            id
        )
        if (patientConflict) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "The patient already has an appointment at this date and time.")
        }

        val oldStatus = appointment.status
        appointment.patient = patient
        appointment.doctor = doctor
        appointment.department = department
        appointment.appointmentDate = appointmentDate
        appointment.appointmentTime = appointmentTime
        if (request.reason.isNotBlank()) {
            appointment.reason = request.reason.trim()
        }
        appointment.status = targetStatus
        appointment.updatedAt = LocalDateTime.now()

        val saved = appointmentRepository.save(appointment)

        if (oldStatus != AppointmentStatus.COMPLETED && saved.status == AppointmentStatus.COMPLETED) {
            doctor.user?.let { doctorUser ->
                try {
                    notificationService.createNotification(
                        user = doctorUser,
                        title = "Appointment Completed",
                        message = "Appointment for ${patient.name} has been marked as COMPLETED.",
                        type = NotificationType.APPOINTMENT_COMPLETED
                    )
                } catch (e: Exception) {
                    // Ignore notification failure
                }
            }
        }

        return toAppointmentResponse(saved)
    }

    @Transactional
    fun completeAppointment(id: Long, currentUserEmail: String?, currentUserRole: Role): AppointmentResponse {
        val appointment = appointmentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (appointment.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to complete this appointment.")
            }
        }

        if (appointment.status == AppointmentStatus.CANCELLED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot complete a cancelled appointment.")
        }

        val oldStatus = appointment.status
        appointment.status = AppointmentStatus.COMPLETED
        appointment.updatedAt = LocalDateTime.now()
        val saved = appointmentRepository.save(appointment)

        if (oldStatus != AppointmentStatus.COMPLETED) {
            saved.doctor?.user?.let { doctorUser ->
                try {
                    notificationService.createNotification(
                        user = doctorUser,
                        title = "Appointment Completed",
                        message = "Appointment for ${saved.patient?.name} has been marked as COMPLETED.",
                        type = NotificationType.APPOINTMENT_COMPLETED
                    )
                } catch (e: Exception) {
                    // Ignore notification failure
                }
            }
        }

        return toAppointmentResponse(saved)
    }

    @Transactional
    fun cancelAppointment(id: Long, currentUserEmail: String?, currentUserRole: Role): AppointmentResponse {
        val appointment = appointmentRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Appointment not found.") }

        if (currentUserRole == Role.DOCTOR) {
            val loggedInDoctor = doctorRepository.findByUserEmail(currentUserEmail ?: "")
                .orElseThrow { ResponseStatusException(HttpStatus.FORBIDDEN, "Doctor profile not found.") }
            if (appointment.doctor?.id != loggedInDoctor.id) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "You are not authorized to cancel this appointment.")
            }
        }

        if (appointment.status == AppointmentStatus.COMPLETED) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Cannot cancel a completed appointment.")
        }

        appointment.status = AppointmentStatus.CANCELLED
        appointment.updatedAt = LocalDateTime.now()
        val saved = appointmentRepository.save(appointment)
        return toAppointmentResponse(saved)
    }

    fun getAppointmentCountByStatus(status: AppointmentStatus): Long {
        return appointmentRepository.countByStatus(status)
    }

    fun getTotalAppointmentCount(): Long {
        return appointmentRepository.count()
    }

    @Synchronized
    private fun generateUniqueAppointmentId(): String {
        val lastAppt = appointmentRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastAppt.isPresent) {
            val lastIdStr = lastAppt.get().appointmentId
            if (lastIdStr.startsWith("APT-")) {
                val numPart = lastIdStr.removePrefix("APT-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastAppt.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("APT-%04d", nextIdNum)
        while (appointmentRepository.existsByAppointmentId(candidateId)) {
            nextIdNum++
            candidateId = String.format("APT-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toAppointmentResponse(appointment: Appointment): AppointmentResponse {
        val patient = appointment.patient!!
        val doctor = appointment.doctor!!
        val department = appointment.department!!

        return AppointmentResponse(
            id = appointment.id ?: 0L,
            appointmentId = appointment.appointmentId,
            patientId = patient.id ?: 0L,
            patientName = patient.name,
            patientCode = patient.patientId,
            doctorId = doctor.id ?: 0L,
            doctorName = doctor.name,
            doctorCode = doctor.doctorId,
            departmentId = department.id ?: 0L,
            departmentName = department.name,
            departmentCode = department.departmentId,
            appointmentDate = appointment.appointmentDate,
            appointmentTime = appointment.appointmentTime,
            reason = appointment.reason,
            status = appointment.status,
            createdAt = appointment.createdAt,
            updatedAt = appointment.updatedAt
        )
    }
}

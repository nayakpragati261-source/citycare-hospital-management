package com.citycare.hospital.repository

import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.AppointmentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime
import java.util.Optional

@Repository
interface AppointmentRepository : JpaRepository<Appointment, Long> {
    fun findByAppointmentId(appointmentId: String): Optional<Appointment>
    fun existsByAppointmentId(appointmentId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<Appointment>

    fun countByStatus(status: AppointmentStatus): Long

    fun findByPatientId(patientId: Long): List<Appointment>
    fun findByDoctorId(doctorId: Long): List<Appointment>
    fun findByDepartmentId(departmentId: Long): List<Appointment>
    fun countByDepartmentId(departmentId: Long): Long
    fun countByDepartmentIdAndStatus(departmentId: Long, status: AppointmentStatus): Long

    fun existsByDoctorId(doctorId: Long): Boolean
    fun existsByPatientId(patientId: Long): Boolean
    fun existsByDepartmentId(departmentId: Long): Boolean

    // Conflict check for Doctor slot
    fun existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
        doctorId: Long,
        appointmentDate: LocalDate,
        appointmentTime: LocalTime,
        status: AppointmentStatus
    ): Boolean

    fun existsByDoctorIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
        doctorId: Long,
        appointmentDate: LocalDate,
        appointmentTime: LocalTime,
        status: AppointmentStatus,
        id: Long
    ): Boolean

    // Conflict check for Patient slot
    fun existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNot(
        patientId: Long,
        appointmentDate: LocalDate,
        appointmentTime: LocalTime,
        status: AppointmentStatus
    ): Boolean

    fun existsByPatientIdAndAppointmentDateAndAppointmentTimeAndStatusNotAndIdNot(
        patientId: Long,
        appointmentDate: LocalDate,
        appointmentTime: LocalTime,
        status: AppointmentStatus,
        id: Long
    ): Boolean
}

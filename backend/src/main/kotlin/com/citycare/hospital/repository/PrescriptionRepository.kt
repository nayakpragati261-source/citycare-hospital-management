package com.citycare.hospital.repository

import com.citycare.hospital.model.Prescription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PrescriptionRepository : JpaRepository<Prescription, Long> {
    fun findByPrescriptionId(prescriptionId: String): Optional<Prescription>
    fun existsByPrescriptionId(prescriptionId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<Prescription>

    fun findByPatientId(patientId: Long): List<Prescription>
    fun findByDoctorId(doctorId: Long): List<Prescription>
    fun findByAppointmentId(appointmentId: Long): List<Prescription>
    fun findByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): List<Prescription>
    fun countByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): Long
}

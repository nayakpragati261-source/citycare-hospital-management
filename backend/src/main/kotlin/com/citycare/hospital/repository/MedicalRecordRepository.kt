package com.citycare.hospital.repository

import com.citycare.hospital.model.MedicalRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MedicalRecordRepository : JpaRepository<MedicalRecord, Long> {
    fun findByRecordId(recordId: String): Optional<MedicalRecord>
    fun existsByRecordId(recordId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<MedicalRecord>

    fun findByPatientId(patientId: Long): List<MedicalRecord>
    fun findByDoctorId(doctorId: Long): List<MedicalRecord>
    fun findByAppointmentId(appointmentId: Long): List<MedicalRecord>
    fun findByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): List<MedicalRecord>
    fun countByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): Long
}

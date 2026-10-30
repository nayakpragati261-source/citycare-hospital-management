package com.citycare.hospital.repository

import com.citycare.hospital.model.MedicalTest
import com.citycare.hospital.model.MedicalTestStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface MedicalTestRepository : JpaRepository<MedicalTest, Long> {
    fun findByTestId(testId: String): Optional<MedicalTest>
    fun existsByTestId(testId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<MedicalTest>

    fun countByStatus(status: MedicalTestStatus): Long

    fun findByPatientId(patientId: Long): List<MedicalTest>
    fun findByDoctorId(doctorId: Long): List<MedicalTest>
    fun findByAppointmentId(appointmentId: Long): List<MedicalTest>
    fun findByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): List<MedicalTest>
    fun countByPatientDepartmentIdOrDoctorDepartmentId(patientDeptId: Long, doctorDeptId: Long): Long
}

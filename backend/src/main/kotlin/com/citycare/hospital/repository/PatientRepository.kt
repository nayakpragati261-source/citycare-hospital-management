package com.citycare.hospital.repository

import com.citycare.hospital.model.Patient
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PatientRepository : JpaRepository<Patient, Long> {
    fun findByPatientId(patientId: String): Optional<Patient>
    fun findTopByOrderByIdDesc(): Optional<Patient>
    fun existsByPatientId(patientId: String): Boolean
    fun countByDepartmentId(departmentId: Long): Long
    fun findAllByDepartmentId(departmentId: Long): List<Patient>
    fun findByIdAndDepartmentId(id: Long, departmentId: Long): Optional<Patient>
}

package com.citycare.hospital.repository

import com.citycare.hospital.model.Doctor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DoctorRepository : JpaRepository<Doctor, Long> {
    fun findByDoctorId(doctorId: String): Optional<Doctor>
    fun findByUserId(userId: Long): Optional<Doctor>
    fun findByUserEmail(userEmail: String): Optional<Doctor>
    fun findTopByOrderByIdDesc(): Optional<Doctor>
    fun existsByDoctorId(doctorId: String): Boolean
    fun existsByUserId(userId: Long): Boolean
    fun existsByUserEmail(email: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun countByDepartmentId(departmentId: Long): Long
    fun findAllByDepartmentId(departmentId: Long): List<Doctor>
}

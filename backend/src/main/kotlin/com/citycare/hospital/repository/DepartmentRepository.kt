package com.citycare.hospital.repository

import com.citycare.hospital.model.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DepartmentRepository : JpaRepository<Department, Long> {
    fun findByDepartmentId(departmentId: String): Optional<Department>
    fun findByNameIgnoreCase(name: String): Optional<Department>
    fun existsByNameIgnoreCase(name: String): Boolean
    fun existsByNameIgnoreCaseAndIdNot(name: String, id: Long): Boolean
    fun findTopByOrderByIdDesc(): Optional<Department>
    fun existsByDepartmentId(departmentId: String): Boolean
}

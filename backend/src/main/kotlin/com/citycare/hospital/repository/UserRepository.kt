package com.citycare.hospital.repository

import com.citycare.hospital.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): Optional<User>
    fun findByEmailIgnoreCase(email: String): Optional<User>
    fun existsByEmail(email: String): Boolean
}

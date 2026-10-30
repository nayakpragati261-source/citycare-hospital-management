package com.citycare.hospital.service

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ChangePasswordRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.User
import com.citycare.hospital.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Optional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    @Transactional
    fun createUser(name: String, email: String, plainPassword: String, role: Role): User {
        val hashedPassword = passwordEncoder.encode(plainPassword)
        val user = User(
            name = name,
            email = email.trim().lowercase(),
            passwordHash = hashedPassword,
            role = role,
            active = true
        )
        return userRepository.save(user)
    }

    fun findByEmail(email: String): Optional<User> {
        val cleanEmail = email.trim()
        val opt = userRepository.findByEmail(cleanEmail)
        if (opt.isPresent) return opt
        return userRepository.findByEmailIgnoreCase(cleanEmail)
    }

    fun existsByEmail(email: String): Boolean {
        return userRepository.existsByEmail(email.trim()) || userRepository.findByEmailIgnoreCase(email.trim()).isPresent
    }

    fun verifyPassword(plainPassword: String, passwordHash: String): Boolean {
        return passwordEncoder.matches(plainPassword, passwordHash)
    }

    fun authenticateUser(email: String, plainPassword: String): AuthUserResponse? {
        val userOptional = findByEmail(email)
        if (userOptional.isEmpty) {
            return null
        }

        val user = userOptional.get()
        if (!user.active) {
            return null
        }

        if (!passwordEncoder.matches(plainPassword, user.passwordHash)) {
            return null
        }

        return toAuthUserResponse(user)
    }

    @Transactional
    fun changePassword(userId: Long, request: ChangePasswordRequest): Boolean {
        if (request.newPassword != request.confirmPassword) {
            throw IllegalArgumentException("New password and confirm password do not match.")
        }
        if (request.newPassword.length < 6) {
            throw IllegalArgumentException("Password must be at least 6 characters long.")
        }

        val user = userRepository.findById(userId).orElseThrow {
            IllegalArgumentException("User account not found.")
        }

        if (!passwordEncoder.matches(request.currentPassword, user.passwordHash)) {
            throw IllegalArgumentException("Incorrect current password.")
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)
        return true
    }

    fun toAuthUserResponse(user: User): AuthUserResponse {
        return AuthUserResponse(
            id = user.id ?: 0L,
            name = user.name,
            email = user.email,
            role = user.role.name
        )
    }
}

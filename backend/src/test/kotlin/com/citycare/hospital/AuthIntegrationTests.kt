package com.citycare.hospital

import com.citycare.hospital.dto.ChangePasswordRequest
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.dto.ResetPasswordRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.UserRepository
import com.citycare.hospital.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // Test 1 & 2: User creation with BCrypt hash and password not stored as plain text
    @Test
    fun testUserCreatedWithBCryptHashAndNotPlainText() {
        val testEmail = "test.bcrypt@citycare.com"
        val rawPassword = "mysecretpassword123"

        if (!userRepository.existsByEmail(testEmail)) {
            userService.createUser(
                name = "BCrypt Test User",
                email = testEmail,
                plainPassword = rawPassword,
                role = Role.DOCTOR
            )
        }

        val user = userRepository.findByEmail(testEmail).orElseThrow()
        assertNotNull(user.passwordHash)
        assertNotEquals(rawPassword, user.passwordHash)
        assertTrue(user.passwordHash.startsWith("$2a$") || user.passwordHash.startsWith("$2b$") || user.passwordHash.startsWith("$2y$"))
        assertTrue(passwordEncoder.matches(rawPassword, user.passwordHash))
    }

    // Test 3: Correct admin login succeeds
    @Test
    fun testAdminLoginSucceeds() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "pragati123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("pragati@citycare.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
            .andExpect(jsonPath("$.name").value("Pragati"))
    }

    // Test 4: Correct doctor login succeeds
    @Test
    fun testDoctorLoginSucceeds() {
        val loginRequest = LoginRequest(
            email = "rahul.sharma@citycare.com",
            password = "rahul123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("rahul.sharma@citycare.com"))
            .andExpect(jsonPath("$.role").value("DOCTOR"))
            .andExpect(jsonPath("$.name").value("Dr. Rahul Sharma"))
    }

    // Test 5: Wrong password returns 401
    @Test
    fun testWrongPasswordReturns401() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "wrongpassword"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
    }

    // Test 6: Unknown email returns 401
    @Test
    fun testUnknownEmailReturns401() {
        val loginRequest = LoginRequest(
            email = "nonexistent@citycare.com",
            password = "pragati123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Invalid email or password"))
    }

    // Test 7: /api/auth/me works after login
    @Test
    fun testMeWorksAfterLogin() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "pragati123"
        )

        val mvcResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val session = mvcResult.request.session as MockHttpSession

        mockMvc.perform(
            get("/api/auth/me")
                .session(session)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("pragati@citycare.com"))
            .andExpect(jsonPath("$.role").value("ADMIN"))
    }

    // Test 8: /api/auth/me returns 401 without login
    @Test
    fun testMeReturns401WithoutLogin() {
        mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized access"))
    }

    // Test 9 & 10: Logout invalidates session and /api/auth/me returns 401 after logout
    @Test
    fun testLogoutInvalidatesSessionAndMeReturns401() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "pragati123"
        )

        val mvcResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val session = mvcResult.request.session as MockHttpSession

        // Verify session works before logout
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk)

        // Perform logout
        mockMvc.perform(post("/api/auth/logout").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Logged out successfully"))

        // Verify session is invalidated and /api/auth/me returns 401
        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized)
    }

    // Test 11: Login response does not contain passwordHash or password
    @Test
    fun testLoginResponseDoesNotContainPasswordHash() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "pragati123"
        )

        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    // Test 12: Admin can reset doctor's password and doctor can login with new password
    @Test
    fun testAdminCanResetDoctorPasswordAndDoctorCanLogin() {
        val adminLogin = LoginRequest(email = "pragati@citycare.com", password = "pragati123")
        val adminRes = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adminLogin))
        ).andExpect(status().isOk).andReturn()
        val adminSession = adminRes.request.session as MockHttpSession

        val doctor = doctorRepository.findAll().first { it.email == "rahul.sharma@citycare.com" }
        val resetReq = ResetPasswordRequest(newPassword = "newpassword123", confirmPassword = "newpassword123")

        mockMvc.perform(
            post("/api/doctors/${doctor.id}/reset-password")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq))
        )
            .andExpect(status().isOk)

        // Login with new password succeeds
        val newLoginReq = LoginRequest(email = "rahul.sharma@citycare.com", password = "newpassword123")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginReq))
        ).andExpect(status().isOk)

        // Old password fails
        val oldLoginReq = LoginRequest(email = "rahul.sharma@citycare.com", password = "rahul123")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(oldLoginReq))
        ).andExpect(status().isUnauthorized)

        // Reset back to original password for consistency
        mockMvc.perform(
            post("/api/doctors/${doctor.id}/reset-password")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(ResetPasswordRequest("rahul123", "rahul123")))
        ).andExpect(status().isOk)
    }

    // Test 13: Doctor can change own password
    @Test
    fun testDoctorCanChangeOwnPassword() {
        val doctorLogin = LoginRequest(email = "rahul.sharma@citycare.com", password = "rahul123")
        val docRes = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doctorLogin))
        ).andExpect(status().isOk).andReturn()
        val doctorSession = docRes.request.session as MockHttpSession

        val changeReq = ChangePasswordRequest(currentPassword = "rahul123", newPassword = "brandnewpass123", confirmPassword = "brandnewpass123")

        mockMvc.perform(
            post("/api/auth/change-password")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(changeReq))
        )
            .andExpect(status().isOk)

        // Login with new password succeeds
        val newLoginReq = LoginRequest(email = "rahul.sharma@citycare.com", password = "brandnewpass123")
        val newLoginRes = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newLoginReq))
        ).andExpect(status().isOk).andReturn()
        val newDoctorSession = newLoginRes.request.session as MockHttpSession

        // Restore back to original
        val restoreReq = ChangePasswordRequest(currentPassword = "brandnewpass123", newPassword = "rahul123", confirmPassword = "rahul123")
        mockMvc.perform(
            post("/api/auth/change-password")
                .session(newDoctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restoreReq))
        ).andExpect(status().isOk)
    }

    // Test 14: Doctor change password rejects incorrect current password
    @Test
    fun testDoctorChangePasswordRejectsIncorrectCurrentPassword() {
        val doctorLogin = LoginRequest(email = "rahul.sharma@citycare.com", password = "rahul123")
        val docRes = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doctorLogin))
        ).andExpect(status().isOk).andReturn()
        val doctorSession = docRes.request.session as MockHttpSession

        val wrongReq = ChangePasswordRequest(currentPassword = "wrongcurrentpassword", newPassword = "newpass12345", confirmPassword = "newpass12345")

        mockMvc.perform(
            post("/api/auth/change-password")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongReq))
        )
            .andExpect(status().isBadRequest)
    }

    // Test 15: Non-Admin cannot reset doctor password
    @Test
    fun testNonAdminCannotResetDoctorPassword() {
        val doctorLogin = LoginRequest(email = "rahul.sharma@citycare.com", password = "rahul123")
        val docRes = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(doctorLogin))
        ).andExpect(status().isOk).andReturn()
        val doctorSession = docRes.request.session as MockHttpSession

        val doctor = doctorRepository.findAll().first { it.email == "priya.mehta@citycare.com" }
        val resetReq = ResetPasswordRequest(newPassword = "hackedpassword123", confirmPassword = "hackedpassword123")

        mockMvc.perform(
            post("/api/doctors/${doctor.id}/reset-password")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq))
        )
            .andExpect(status().isForbidden)
    }
}

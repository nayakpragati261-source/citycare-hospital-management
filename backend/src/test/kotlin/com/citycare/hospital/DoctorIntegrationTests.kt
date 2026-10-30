package com.citycare.hospital

import com.citycare.hospital.dto.DoctorRequest
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.model.Role
import com.citycare.hospital.repository.DoctorRepository
import com.citycare.hospital.repository.UserRepository
import com.citycare.hospital.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class DoctorIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    private val demoDoctorEmails = setOf(
        "rahul.sharma@citycare.com",
        "priya.mehta@citycare.com",
        "amit.patil@citycare.com",
        "sneha.joshi@citycare.com",
        "karan.shah@citycare.com"
    )

    @AfterEach
    fun cleanUpTestDoctors() {
        doctorRepository.findAll().forEach { doc ->
            if (!demoDoctorEmails.contains(doc.email)) {
                doctorRepository.delete(doc)
            }
        }
        userRepository.findAll().forEach { u ->
            if (u.email != "pragati@citycare.com" && !demoDoctorEmails.contains(u.email)) {
                if (doctorRepository.existsByUserEmail(u.email)) {
                    doctorRepository.findByUserEmail(u.email).ifPresent { doctorRepository.delete(it) }
                }
                userRepository.delete(u)
            }
        }
    }

    private fun getAdminSession(): MockHttpSession {
        val loginRequest = LoginRequest(email = "pragati@citycare.com", password = "pragati123")
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk).andReturn()

        return result.request.session as MockHttpSession
    }

    private fun getDoctorSession(): MockHttpSession {
        val loginRequest = LoginRequest(email = "rahul.sharma@citycare.com", password = "rahul123")
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk).andReturn()

        return result.request.session as MockHttpSession
    }

    // 1. Unauthenticated GET /api/doctors returns 401
    @Test
    fun testUnauthenticatedGetDoctorsReturns401() {
        mockMvc.perform(get("/api/doctors"))
            .andExpect(status().isUnauthorized)
    }

    // 2 & 3. Admin & Doctor can view doctors
    @Test
    fun testAdminAndDoctorCanViewDoctors() {
        val adminSession = getAdminSession()
        mockMvc.perform(get("/api/doctors").session(adminSession))
            .andExpect(status().isOk)

        val doctorSession = getDoctorSession()
        mockMvc.perform(get("/api/doctors").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 4 & 8 & 17. Admin can create Doctor linked to DOCTOR user; DOC-xxxx format; no passwordHash
    @Test
    fun testAdminCanCreateDoctor() {
        val testDoctorEmail = "test.doc.new@citycare.com"
        userService.createUser("Dr. Test User", testDoctorEmail, "doc12345", Role.DOCTOR)

        val adminSession = getAdminSession()
        val request = DoctorRequest(
            userEmail = testDoctorEmail,
            specialization = "Cardiology",
            experience = 10,
            phone = "9876543210",
            availability = "Mon-Wed, 10:00 AM - 4:00 PM"
        )

        mockMvc.perform(
            post("/api/doctors")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doctorId").exists())
            .andExpect(jsonPath("$.name").value("Dr. Test User"))
            .andExpect(jsonPath("$.email").value(testDoctorEmail))
            .andExpect(jsonPath("$.specialization").value("Cardiology"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    // 5. Doctor creation fails when user email does not exist
    @Test
    fun testCreateDoctorFailsWhenUserNotFound() {
        val adminSession = getAdminSession()
        val request = DoctorRequest(
            userEmail = "nonexistent.user@citycare.com",
            specialization = "Neurology",
            experience = 5,
            phone = "9876543210",
            availability = "Daily"
        )

        mockMvc.perform(
            post("/api/doctors")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // 6. Doctor creation fails when selected user is ADMIN
    @Test
    fun testCreateDoctorFailsWhenUserIsAdmin() {
        val adminSession = getAdminSession()
        val request = DoctorRequest(
            userEmail = "pragati@citycare.com", // ADMIN role!
            specialization = "Pediatrics",
            experience = 7,
            phone = "9876543210",
            availability = "Daily"
        )

        mockMvc.perform(
            post("/api/doctors")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Only users with DOCTOR role can be linked to a doctor profile."))
    }

    // 7. Doctor creation fails when selected user is already linked
    @Test
    fun testCreateDoctorFailsWhenAlreadyLinked() {
        val adminSession = getAdminSession()
        val request = DoctorRequest(
            userEmail = "rahul.sharma@citycare.com", // Already linked in seeder!
            specialization = "Dermatology",
            experience = 6,
            phone = "9876543210",
            availability = "Daily"
        )

        mockMvc.perform(
            post("/api/doctors")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("This user is already linked to a doctor profile."))
    }

    // 10 & 11. Admin can update Doctor; Doctor role CANNOT update (403 Forbidden)
    @Test
    fun testUpdateDoctorPermissions() {
        val rahulDoctor = doctorRepository.findByUserEmail("rahul.sharma@citycare.com").orElseThrow()
        val updateReq = DoctorRequest(
            userEmail = "rahul.sharma@citycare.com",
            specialization = "General Medicine & Internal Care",
            experience = 9,
            phone = "9000000000",
            availability = "Mon-Sat, 9:00 AM - 3:00 PM"
        )

        // Doctor role attempt -> 403 Forbidden
        val doctorSession = getDoctorSession()
        mockMvc.perform(
            put("/api/doctors/${rahulDoctor.id}")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isForbidden)

        // Admin role attempt -> 200 OK
        val adminSession = getAdminSession()
        mockMvc.perform(
            put("/api/doctors/${rahulDoctor.id}")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.specialization").value("General Medicine & Internal Care"))
            .andExpect(jsonPath("$.experience").value(9))
    }

    // 12, 13, 14, 15. Admin can delete Doctor; Doctor cannot (403); Deleting Doctor does NOT delete User account
    @Test
    fun testDeleteDoctorPermissionsAndUserRetention() {
        val testDoctorEmail = "test.delete.doc@citycare.com"
        val testUser = userService.createUser("Dr. Delete Test", testDoctorEmail, "pass1234", Role.DOCTOR)

        val adminSession = getAdminSession()
        val createReq = DoctorRequest(
            userEmail = testDoctorEmail,
            specialization = "Orthopedics",
            experience = 4,
            phone = "9876543210",
            availability = "Daily"
        )

        val createResStr = mockMvc.perform(
            post("/api/doctors")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val docId = objectMapper.readTree(createResStr).get("id").asLong()

        // Doctor role attempt -> 403 Forbidden
        val doctorSession = getDoctorSession()
        mockMvc.perform(delete("/api/doctors/$docId").session(doctorSession))
            .andExpect(status().isForbidden)

        // Admin role attempt -> 200 OK
        mockMvc.perform(delete("/api/doctors/$docId").session(adminSession))
            .andExpect(status().isOk)

        // Verify Doctor profile is removed from doctors table
        assertFalse(doctorRepository.existsById(docId))

        // Verify User account STAYS in users table!
        val userStillExists = userRepository.existsByEmail(testDoctorEmail)
        assertTrue(userStillExists, "Deleting a Doctor profile must NOT delete the linked User account!")
    }

    // 16. Dashboard totalDoctors reflects actual count
    @Test
    fun testDashboardTotalDoctorsReflectsActualCount() {
        val adminSession = getAdminSession()
        val count = doctorRepository.count()

        mockMvc.perform(get("/api/dashboard").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalDoctors").value(count))
    }

    // 17. Admin can create Doctor account (User + Profile) directly and new Doctor can log in
    @Test
    fun testAdminCanCreateDoctorAccountAndDoctorCanLogin() {
        val adminSession = getAdminSession()
        val request = com.citycare.hospital.dto.CreateDoctorAccountRequest(
            name = "Dr. Vikram Seth",
            email = "vikram.seth@citycare.com",
            password = "vikrampassword123",
            specialization = "Cardiology",
            experience = 12,
            phone = "9876543219",
            availability = "Mon-Fri, 9:00 AM - 2:00 PM"
        )

        // Admin creates doctor account
        mockMvc.perform(
            post("/api/doctors/create-account")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doctorId").exists())
            .andExpect(jsonPath("$.name").value("Dr. Vikram Seth"))
            .andExpect(jsonPath("$.email").value("vikram.seth@citycare.com"))
            .andExpect(jsonPath("$.specialization").value("Cardiology"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())

        // Verify newly created doctor can log in through doctor-login endpoint
        val loginRequest = LoginRequest(email = "vikram.seth@citycare.com", password = "vikrampassword123")
        mockMvc.perform(
            post("/api/auth/doctor-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("vikram.seth@citycare.com"))
            .andExpect(jsonPath("$.role").value("DOCTOR"))
    }
}

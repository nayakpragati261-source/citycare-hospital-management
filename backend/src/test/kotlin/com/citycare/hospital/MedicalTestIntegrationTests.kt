package com.citycare.hospital

import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.dto.MedicalTestRequest
import com.citycare.hospital.model.Doctor
import com.citycare.hospital.model.MedicalTestStatus
import com.citycare.hospital.model.Patient
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.User
import com.citycare.hospital.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.startsWith
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
import java.time.LocalDate
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
class MedicalTestIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var medicalTestRepository: MedicalTestRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        medicalTestRepository.deleteAll()
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

    private fun getOrCreatePatient(): Patient {
        val existing = patientRepository.findAll().firstOrNull()
        if (existing != null) return existing
        return patientRepository.save(
            Patient(patientId = "PAT-0001", name = "Test Patient Test", age = 45, gender = "Female", disease = "Diabetes", phone = "9876543210", address = "Main St")
        )
    }

    private fun getDoctor(): Doctor {
        val doc = doctorRepository.findByUserEmail("rahul.sharma@citycare.com").orElseGet { doctorRepository.findAll().first() }
        if (doc.department == null) {
            val dept = departmentRepository.findByNameIgnoreCase("General Medicine").orElseGet { departmentRepository.findAll().first() }
            doc.department = dept
            doctorRepository.save(doc)
        }
        return doc
    }

    // 1. Unauthenticated request -> 401
    @Test
    fun testUnauthenticatedGetTestsReturns401() {
        mockMvc.perform(get("/api/medical-tests"))
            .andExpect(status().isUnauthorized)
    }

    // 2, 9, 11. Admin can create PENDING test without result, format TEST-xxxx
    @Test
    fun testPendingTestCanHaveEmptyResult() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = MedicalTestRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            testName = "Complete Blood Count (CBC)",
            testDate = LocalDate.now(),
            result = null,
            status = MedicalTestStatus.PENDING
        )

        mockMvc.perform(
            post("/api/medical-tests")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.testId").value(startsWith("TEST-")))
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    // 10. Completed test requires result (400 if empty)
    @Test
    fun testCompletedTestRequiresResult() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = MedicalTestRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            testName = "Lipid Profile",
            testDate = LocalDate.now(),
            result = "   ",
            status = MedicalTestStatus.COMPLETED
        )

        mockMvc.perform(
            post("/api/medical-tests")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("A result is required when the medical test is completed."))
    }

    // 4 & 10. Completed test succeeds when result is provided
    @Test
    fun testCompletedTestSucceedsWithResult() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = MedicalTestRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            testName = "Fasting Blood Sugar",
            testDate = LocalDate.now(),
            result = "95 mg/dL (Normal)",
            status = MedicalTestStatus.COMPLETED
        )

        mockMvc.perform(
            post("/api/medical-tests")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
            .andExpect(jsonPath("$.result").value("95 mg/dL (Normal)"))
    }
}

package com.citycare.hospital

import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.dto.PatientRequest
import com.citycare.hospital.repository.*
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
class PatientIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var medicalTestRepository: MedicalTestRepository

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @AfterEach
    fun cleanUp() {
        billRepository.deleteAll()
        medicalTestRepository.deleteAll()
        prescriptionRepository.deleteAll()
        medicalRecordRepository.deleteAll()
        appointmentRepository.deleteAll()
        patientRepository.deleteAll()
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

    // 1. Unauthenticated GET /api/patients returns 401
    @Test
    fun testUnauthenticatedGetPatientsReturns401() {
        mockMvc.perform(get("/api/patients"))
            .andExpect(status().isUnauthorized)
    }

    // 2. Authenticated admin can get patients
    @Test
    fun testAdminCanGetPatients() {
        val session = getAdminSession()
        mockMvc.perform(get("/api/patients").session(session))
            .andExpect(status().isOk)
    }

    // 3. Authenticated doctor can get patients
    @Test
    fun testDoctorCanGetPatients() {
        val session = getDoctorSession()
        mockMvc.perform(get("/api/patients").session(session))
            .andExpect(status().isOk)
    }

    // 4. Admin can create a patient
    @Test
    fun testAdminCanCreatePatient() {
        val session = getAdminSession()
        val request = PatientRequest(
            name = "Aarav Sharma",
            age = 30,
            gender = "Male",
            disease = "Hypertension",
            phone = "9876543210",
            address = "Mumbai, Maharashtra"
        )

        val responseContent = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Aarav Sharma"))
            .andReturn().response.contentAsString

        assertTrue(responseContent.contains("PAT-"))
    }

    // 5. Doctor can create a patient
    @Test
    fun testDoctorCanCreatePatient() {
        val session = getDoctorSession()
        val request = PatientRequest(
            name = "Priya Verma",
            age = 28,
            gender = "Female",
            disease = "Asthma",
            phone = "9123456789",
            address = "Delhi, NCR"
        )

        mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Priya Verma"))
    }

    // 6. Invalid patient data returns 400
    @Test
    fun testInvalidPatientDataReturns400() {
        val session = getAdminSession()
        val invalidRequest = PatientRequest(
            name = "", // Blank name
            age = 150, // Invalid age > 120
            gender = "Unknown", // Invalid gender
            disease = "",
            phone = "123", // Invalid phone
            address = ""
        )

        mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest)
    }

    // 7. Patient can be retrieved by ID
    @Test
    fun testGetPatientById() {
        val session = getAdminSession()
        val request = PatientRequest(
            name = "Rohan Mehta",
            age = 45,
            gender = "Male",
            disease = "Diabetes",
            phone = "9988776655",
            address = "Pune"
        )

        val createRes = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString

        val createdId = objectMapper.readTree(createRes).get("id").asLong()

        mockMvc.perform(get("/api/patients/$createdId").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Rohan Mehta"))
            .andExpect(jsonPath("$.disease").value("Diabetes"))
    }

    // 8. Patient can be updated
    @Test
    fun testUpdatePatient() {
        val session = getAdminSession()
        val request = PatientRequest(
            name = "Initial Name",
            age = 25,
            gender = "Male",
            disease = "Viral Fever",
            phone = "9876543210",
            address = "Initial Address"
        )

        val createRes = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString

        val createdId = objectMapper.readTree(createRes).get("id").asLong()

        val updateRequest = PatientRequest(
            name = "Updated Name",
            age = 26,
            gender = "Male",
            disease = "Recovered",
            phone = "9876543210",
            address = "Updated Address"
        )

        mockMvc.perform(
            put("/api/patients/$createdId")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.disease").value("Recovered"))
    }

    // 9 & 10. Patient can be deleted and non-existent patient returns 404
    @Test
    fun testDeletePatientAndNotFound() {
        val session = getAdminSession()
        val request = PatientRequest(
            name = "To Delete",
            age = 50,
            gender = "Male",
            disease = "Flu",
            phone = "9876543210",
            address = "Address"
        )

        val createRes = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andReturn().response.contentAsString

        val createdId = objectMapper.readTree(createRes).get("id").asLong()

        mockMvc.perform(delete("/api/patients/$createdId").session(session))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/patients/$createdId").session(session))
            .andExpect(status().isNotFound)
    }

    // 11. Patient ID generation & uniqueness
    @Test
    fun testPatientIdGenerationAndUniqueness() {
        val session = getAdminSession()
        val req1 = PatientRequest("P1", 20, "Male", "D1", "9876543210", "A1")
        val req2 = PatientRequest("P2", 22, "Female", "D2", "9876543211", "A2")

        val res1Str = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1))
        ).andReturn().response.contentAsString

        val res2Str = mockMvc.perform(
            post("/api/patients")
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2))
        ).andReturn().response.contentAsString

        val pId1 = objectMapper.readTree(res1Str).get("patientId").asText()
        val pId2 = objectMapper.readTree(res2Str).get("patientId").asText()

        assertNotEquals(pId1, pId2)
        assertTrue(pId1.startsWith("PAT-"))
        assertTrue(pId2.startsWith("PAT-"))
    }

    // 12. Dashboard totalPatients reflects actual number of patients
    @Test
    fun testDashboardReflectsTotalPatients() {
        val session = getAdminSession()
        val initialCount = patientRepository.count().toInt()

        // Create 2 patients
        val req1 = PatientRequest("P1", 20, "Male", "D1", "9876543210", "A1")
        val req2 = PatientRequest("P2", 22, "Female", "D2", "9876543211", "A2")
        mockMvc.perform(post("/api/patients").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1)))
        mockMvc.perform(post("/api/patients").session(session).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2)))

        mockMvc.perform(get("/api/dashboard").session(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPatients").value(initialCount + 2))
    }
}

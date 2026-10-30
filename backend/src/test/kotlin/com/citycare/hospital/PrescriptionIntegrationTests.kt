package com.citycare.hospital

import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.dto.PrescriptionRequest
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.Doctor
import com.citycare.hospital.model.Patient
import com.citycare.hospital.model.Role
import com.citycare.hospital.model.User
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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
class PrescriptionIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        prescriptionRepository.deleteAll()
        appointmentRepository.deleteAll()
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
            Patient(patientId = "PAT-0001", name = "Test Patient", age = 30, gender = "Male", disease = "Fever", phone = "9876543210", address = "Main St")
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

    // 1. Unauthenticated GET -> 401
    @Test
    fun testUnauthenticatedGetPrescriptionsReturns401() {
        mockMvc.perform(get("/api/prescriptions"))
            .andExpect(status().isUnauthorized)
    }

    // 2 & 15. Admin can list & create prescription with RX-xxxx format
    @Test
    fun testAdminCanCreateAndListPrescription() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Amoxicillin 500mg",
            dosage = "1 tablet 3 times daily",
            duration = "5 days",
            instructions = "Take after meals"
        )

        mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.prescriptionId").value("RX-0001"))
            .andExpect(jsonPath("$.medicine").value("Amoxicillin 500mg"))

        mockMvc.perform(get("/api/prescriptions").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].prescriptionId").value("RX-0001"))
    }

    // 4. Admin can update prescription
    @Test
    fun testAdminCanUpdatePrescription() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val createReq = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Paracetamol",
            dosage = "500mg",
            duration = "3 days"
        )
        val resStr = mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(resStr).get("id").asLong()

        val updateReq = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Paracetamol 650mg",
            dosage = "1 tab twice daily",
            duration = "5 days"
        )

        mockMvc.perform(
            put("/api/prescriptions/$id")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.medicine").value("Paracetamol 650mg"))
    }

    // 5. Admin can delete prescription
    @Test
    fun testAdminCanDeletePrescription() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val createReq = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Temp Medicine",
            dosage = "10mg",
            duration = "1 day"
        )
        val resStr = mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(resStr).get("id").asLong()

        mockMvc.perform(delete("/api/prescriptions/$id").session(adminSession))
            .andExpect(status().isOk)
    }

    // 6 & 7. Doctor can view & create prescription for self
    @Test
    fun testDoctorCanCreatePrescriptionForSelf() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Cough Syrup",
            dosage = "10ml",
            duration = "7 days"
        )

        mockMvc.perform(
            post("/api/prescriptions")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doctorId").value(doctor.id))

        mockMvc.perform(get("/api/prescriptions").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 8. Doctor cannot create prescription for another doctor (403)
    @Test
    fun testDoctorCannotCreateForAnotherDoctor() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val dept = departmentRepository.findAll().first()

        val user2 = userRepository.findByEmail("other.doc.rx@citycare.com").orElseGet {
            userRepository.save(User(name = "Dr. Other Rx", email = "other.doc.rx@citycare.com", passwordHash = "hash", role = Role.DOCTOR))
        }
        val doc2 = doctorRepository.findByUserId(user2.id!!).orElseGet {
            doctorRepository.save(Doctor(doctorId = "DOC-7777", user = user2, name = "Dr. Other Rx", email = "other.doc.rx@citycare.com", specialization = "Cardiology", department = dept, phone = "9876543211", availability = "Mon-Fri"))
        }

        try {
            val request = PrescriptionRequest(
                patientId = patient.id,
                doctorId = doc2.id,
                prescriptionDate = LocalDate.now(),
                medicine = "Illegal Medicine",
                dosage = "10mg",
                duration = "1 day"
            )

            mockMvc.perform(
                post("/api/prescriptions")
                    .session(doctorSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
        } finally {
            doctorRepository.delete(doc2)
            userRepository.delete(user2)
        }
    }

    // 10. Invalid patient rejected (404)
    @Test
    fun testInvalidPatientRejected() {
        val adminSession = getAdminSession()
        val doctor = getDoctor()

        val request = PrescriptionRequest(
            patientId = 999999L,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Med",
            dosage = "Dosage",
            duration = "1 day"
        )

        mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // 13. Appointment/patient mismatch rejected (409)
    @Test
    fun testAppointmentPatientMismatchRejected() {
        val adminSession = getAdminSession()
        val patient1 = getOrCreatePatient()
        val patient2 = patientRepository.save(Patient(patientId = "PAT-0002", name = "Second Patient", age = 25, gender = "Female", disease = "Cold", phone = "9998887770", address = "Side St"))
        val doctor = getDoctor()
        val dept = doctor.department ?: departmentRepository.findAll().first()

        // Create appointment for patient1
        val appt = appointmentRepository.save(
            Appointment(appointmentId = "APT-5555", patient = patient1, doctor = doctor, department = dept, appointmentDate = LocalDate.now(), appointmentTime = LocalTime.of(10, 0), reason = "Appt")
        )

        // Attempt prescription linking appt to patient2
        val request = PrescriptionRequest(
            patientId = patient2.id,
            doctorId = doctor.id,
            appointmentId = appt.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Mismatch Med",
            dosage = "10mg",
            duration = "1 day"
        )

        mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("The selected appointment does not belong to the selected patient."))
    }
}

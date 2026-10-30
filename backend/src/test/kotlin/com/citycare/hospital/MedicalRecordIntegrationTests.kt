package com.citycare.hospital

import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.dto.MedicalRecordRequest
import com.citycare.hospital.model.Appointment
import com.citycare.hospital.model.Doctor
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
import java.time.LocalTime
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
class MedicalRecordIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

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

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var medicalTestRepository: MedicalTestRepository

    @AfterEach
    fun cleanUp() {
        prescriptionRepository.deleteAll()
        medicalRecordRepository.deleteAll()
        medicalTestRepository.deleteAll()
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
            Patient(patientId = "PAT-0001", name = "Test Patient MR", age = 35, gender = "Male", disease = "Hypertension", phone = "9876543210", address = "Main St")
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
    fun testUnauthenticatedGetRecordsReturns401() {
        mockMvc.perform(get("/api/medical-records"))
            .andExpect(status().isUnauthorized)
    }

    // 2 & 10. Admin CRUD & Record ID MR-xxxx generation
    @Test
    fun testAdminCanCreateAndListMedicalRecord() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = MedicalRecordRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            previousDisease = "Asthma",
            diagnosis = "Acute Bronchitis",
            treatment = "Inhaler and antibiotics",
            recordDate = LocalDate.now(),
            notes = "Patient responding well to treatment"
        )

        mockMvc.perform(
            post("/api/medical-records")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.recordId").value(startsWith("MR-")))
            .andExpect(jsonPath("$.diagnosis").value("Acute Bronchitis"))

        mockMvc.perform(get("/api/medical-records").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].recordId").value(startsWith("MR-")))
    }

    // 4. Doctor can create own record
    @Test
    fun testDoctorCanCreateRecordForSelf() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = MedicalRecordRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            diagnosis = "Migraine",
            treatment = "Pain relievers and rest",
            recordDate = LocalDate.now()
        )

        mockMvc.perform(
            post("/api/medical-records")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doctorId").value(doctor.id))

        mockMvc.perform(get("/api/medical-records").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 5. Doctor cannot create for another doctor (403)
    @Test
    fun testDoctorCannotCreateForAnotherDoctor() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val dept = departmentRepository.findAll().first()

        val user2 = userRepository.findByEmail("other.doc.mr@citycare.com").orElseGet {
            userRepository.save(User(name = "Dr. Other MR", email = "other.doc.mr@citycare.com", passwordHash = "hash", role = Role.DOCTOR))
        }
        val doc2 = doctorRepository.findByUserId(user2.id!!).orElseGet {
            doctorRepository.save(Doctor(doctorId = "DOC-8888", user = user2, name = "Dr. Other MR", email = "other.doc.mr@citycare.com", specialization = "General Medicine", department = dept, phone = "9876543211", availability = "Mon-Fri"))
        }

        try {
            val request = MedicalRecordRequest(
                patientId = patient.id,
                doctorId = doc2.id,
                diagnosis = "Illegal Diagnosis",
                treatment = "None",
                recordDate = LocalDate.now()
            )

            mockMvc.perform(
                post("/api/medical-records")
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

    // 9. Appointment mismatch rejected (409)
    @Test
    fun testAppointmentMismatchRejected() {
        val adminSession = getAdminSession()
        val patient1 = getOrCreatePatient()
        val uniquePatientId = "PAT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 6).uppercase()
        val patient2 = patientRepository.save(Patient(patientId = uniquePatientId, name = "Third Patient", age = 40, gender = "Male", disease = "Flu", phone = "9998887771", address = "North St"))
        val doctor = getDoctor()
        val dept = doctor.department ?: departmentRepository.findAll().first()

        val appt = appointmentRepository.save(
            Appointment(appointmentId = "APT-" + UUID.randomUUID().toString().substring(0, 4).uppercase(), patient = patient1, doctor = doctor, department = dept, appointmentDate = LocalDate.now(), appointmentTime = LocalTime.of(11, 0), reason = "Appt MR")
        )

        val request = MedicalRecordRequest(
            patientId = patient2.id,
            doctorId = doctor.id,
            appointmentId = appt.id,
            diagnosis = "Mismatch MR",
            treatment = "None",
            recordDate = LocalDate.now()
        )

        mockMvc.perform(
            post("/api/medical-records")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("The selected appointment does not belong to the selected patient."))
    }
}

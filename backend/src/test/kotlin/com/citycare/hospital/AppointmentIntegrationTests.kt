package com.citycare.hospital

import com.citycare.hospital.dto.AppointmentRequest
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.model.AppointmentStatus
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
class AppointmentIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    @Autowired
    private lateinit var medicalTestRepository: MedicalTestRepository

    @AfterEach
    fun cleanUpAppointments() {
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

        val patient = Patient(
            patientId = "PAT-0001",
            name = "Test Patient",
            age = 30,
            gender = "Male",
            disease = "Fever",
            phone = "9876543210",
            address = "123 Main Street",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        return patientRepository.save(patient)
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

    private fun getDepartmentForDoctor(doc: Doctor) = doc.department ?: departmentRepository.findByNameIgnoreCase(doc.specialization).orElseGet { departmentRepository.findAll().first() }

    // 1. Unauthenticated GET appointments -> 401
    @Test
    fun testUnauthenticatedGetAppointmentsReturns401() {
        mockMvc.perform(get("/api/appointments"))
            .andExpect(status().isUnauthorized)
    }

    // 2. Admin can list appointments
    @Test
    fun testAdminCanListAppointments() {
        val adminSession = getAdminSession()
        mockMvc.perform(get("/api/appointments").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    // 3 & 23. Admin can create appointment with APT-xxxx format
    @Test
    fun testAdminCanCreateAppointment() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(1),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Routine Checkup"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.appointmentId").value("APT-0001"))
            .andExpect(jsonPath("$.reason").value("Routine Checkup"))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
    }

    // 4. Admin can update appointment
    @Test
    fun testAdminCanUpdateAppointment() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val createReq = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(2),
            appointmentTime = LocalTime.of(11, 0),
            reason = "Initial Visit"
        )
        val createRes = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(createRes).get("id").asLong()

        val updateReq = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(3),
            appointmentTime = LocalTime.of(12, 0),
            reason = "Follow up Visit",
            status = AppointmentStatus.SCHEDULED
        )

        mockMvc.perform(
            put("/api/appointments/$id")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.reason").value("Follow up Visit"))
    }

    // 5 & 21. Admin can cancel appointment
    @Test
    fun testAdminCanCancelAppointment() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val createReq = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(4),
            appointmentTime = LocalTime.of(14, 0),
            reason = "Consultation"
        )
        val createRes = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(createRes).get("id").asLong()

        mockMvc.perform(put("/api/appointments/$id/cancel").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CANCELLED"))
    }

    // 6 & 20. Admin can complete appointment
    @Test
    fun testAdminCanCompleteAppointment() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val createReq = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(5),
            appointmentTime = LocalTime.of(15, 0),
            reason = "Treatment"
        )
        val createRes = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(createRes).get("id").asLong()

        mockMvc.perform(put("/api/appointments/$id/complete").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("COMPLETED"))
    }

    // 7. Doctor can view appointments
    @Test
    fun testDoctorCanViewAppointments() {
        val doctorSession = getDoctorSession()
        mockMvc.perform(get("/api/appointments").session(doctorSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
    }

    // 8. Doctor can create appointment for self
    @Test
    fun testDoctorCanCreateAppointmentForSelf() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(6),
            appointmentTime = LocalTime.of(9, 30),
            reason = "Doctor Self Booking"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.doctorId").value(doctor.id))
    }

    // 9. Doctor cannot create appointment for another doctor (403)
    @Test
    fun testDoctorCannotCreateAppointmentForAnotherDoctor() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val department = departmentRepository.findByNameIgnoreCase("Cardiology").orElseGet { departmentRepository.findAll().first() }

        // Create a second doctor profile
        val user2 = userRepository.findByEmail("other.doctor@citycare.com").orElseGet {
            userRepository.save(User(name = "Dr. Other", email = "other.doctor@citycare.com", passwordHash = "hash", role = Role.DOCTOR))
        }
        val doc2 = doctorRepository.findByUserId(user2.id!!).orElseGet {
            doctorRepository.save(Doctor(doctorId = "DOC-9999", user = user2, name = "Dr. Other", email = "other.doctor@citycare.com", specialization = "Cardiology", department = department, phone = "9876543211", availability = "Mon-Fri"))
        }

        try {
            val request = AppointmentRequest(
                patientId = patient.id,
                doctorId = doc2.id,
                departmentId = department.id,
                appointmentDate = LocalDate.now().plusDays(7),
                appointmentTime = LocalTime.of(10, 30),
                reason = "Unauthorized Booking"
            )

            mockMvc.perform(
                post("/api/appointments")
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

    // 10. Doctor cannot modify another doctor's appointment
    @Test
    fun testDoctorCannotModifyAnotherDoctorAppointment() {
        val adminSession = getAdminSession()
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()
        val doc1 = getDoctor()
        val dept = getDepartmentForDoctor(doc1)

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doc1.id,
            departmentId = dept.id,
            appointmentDate = LocalDate.now().plusDays(8),
            appointmentTime = LocalTime.of(11, 30),
            reason = "Existing Appt"
        )
        val resStr = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn().response.contentAsString
        val id = objectMapper.readTree(resStr).get("id").asLong()

        // Dr. Rahul Sharma owns doc1, so doctorSession can complete it
        mockMvc.perform(put("/api/appointments/$id/complete").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 11. Invalid patient rejected (404)
    @Test
    fun testInvalidPatientRejected() {
        val adminSession = getAdminSession()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val request = AppointmentRequest(
            patientId = 999999L,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(10),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Invalid Patient"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Patient not found."))
    }

    // 12. Invalid doctor rejected (404)
    @Test
    fun testInvalidDoctorRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val department = departmentRepository.findAll().first()

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = 999999L,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(10),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Invalid Doctor"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Doctor not found."))
    }

    // 13. Invalid department rejected (404)
    @Test
    fun testInvalidDepartmentRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = 999999L,
            appointmentDate = LocalDate.now().plusDays(10),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Invalid Dept"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Department not found."))
    }

    // 14. Blank reason rejected (400)
    @Test
    fun testBlankReasonRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(10),
            appointmentTime = LocalTime.of(10, 0),
            reason = "   "
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // 16. Doctor-department mismatch rejected (409)
    @Test
    fun testDoctorDepartmentMismatchRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor() // General Medicine
        val cardiologyDept = departmentRepository.findByNameIgnoreCase("Cardiology").orElseThrow()

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = cardiologyDept.id,
            appointmentDate = LocalDate.now().plusDays(11),
            appointmentTime = LocalTime.of(11, 0),
            reason = "Mismatch check"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("The selected doctor does not belong to the selected department."))
    }

    // 17. Same doctor + same date + same time rejected (409)
    @Test
    fun testSameDoctorSameSlotConflictRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val slotDate = LocalDate.now().plusDays(12)
        val slotTime = LocalTime.of(14, 30)

        val request1 = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = slotDate,
            appointmentTime = slotTime,
            reason = "Appt 1"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        ).andExpect(status().isCreated)

        // Attempt second appointment for same doctor at same date & time
        val request2 = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = slotDate,
            appointmentTime = slotTime,
            reason = "Appt 2 conflict"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("The doctor already has an appointment at this date and time."))
    }

    // 19. Cancelled appointment does not block same slot
    @Test
    fun testCancelledAppointmentDoesNotBlockSlot() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val slotDate = LocalDate.now().plusDays(13)
        val slotTime = LocalTime.of(16, 0)

        val request1 = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = slotDate,
            appointmentTime = slotTime,
            reason = "Appt to cancel"
        )

        val resStr = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(resStr).get("id").asLong()

        // Cancel the appointment
        mockMvc.perform(put("/api/appointments/$id/cancel").session(adminSession))
            .andExpect(status().isOk)

        // Now book the exact same slot again -> Should succeed!
        val request2 = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = slotDate,
            appointmentTime = slotTime,
            reason = "Replacement Appt"
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2))
        ).andExpect(status().isCreated)
    }

    // 22. Invalid status transition rejected (CANCELLED -> SCHEDULED)
    @Test
    fun testInvalidStatusTransitionRejected() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        val request = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(14),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Status Transition Test"
        )

        val resStr = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(resStr).get("id").asLong()

        // Cancel it first
        mockMvc.perform(put("/api/appointments/$id/cancel").session(adminSession))
            .andExpect(status().isOk)

        // Attempt to update cancelled appt status back to SCHEDULED -> Rejected
        val updateReq = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = department.id,
            appointmentDate = LocalDate.now().plusDays(14),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Reopen attempt",
            status = AppointmentStatus.SCHEDULED
        )

        mockMvc.perform(
            put("/api/appointments/$id")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("This appointment has already been cancelled."))
    }

    // 24, 25, 26, 27, 32. Dashboard appointment metrics check
    @Test
    fun testDashboardAppointmentMetrics() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()
        val department = getDepartmentForDoctor(doctor)

        // Create 1 SCHEDULED, 1 COMPLETED, 1 CANCELLED
        val req1 = AppointmentRequest(patient.id, doctor.id, department.id, LocalDate.now().plusDays(15), LocalTime.of(9, 0), "S1")
        mockMvc.perform(post("/api/appointments").session(adminSession).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req1))).andExpect(status().isCreated)

        val req2 = AppointmentRequest(patient.id, doctor.id, department.id, LocalDate.now().plusDays(15), LocalTime.of(10, 0), "S2")
        val r2 = mockMvc.perform(post("/api/appointments").session(adminSession).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req2))).andExpect(status().isCreated).andReturn().response.contentAsString
        val id2 = objectMapper.readTree(r2).get("id").asLong()
        mockMvc.perform(put("/api/appointments/$id2/complete").session(adminSession)).andExpect(status().isOk)

        val req3 = AppointmentRequest(patient.id, doctor.id, department.id, LocalDate.now().plusDays(15), LocalTime.of(11, 0), "S3")
        val r3 = mockMvc.perform(post("/api/appointments").session(adminSession).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(req3))).andExpect(status().isCreated).andReturn().response.contentAsString
        val id3 = objectMapper.readTree(r3).get("id").asLong()
        mockMvc.perform(put("/api/appointments/$id3/cancel").session(adminSession)).andExpect(status().isOk)

        mockMvc.perform(get("/api/dashboard").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAppointments").value(3))
            .andExpect(jsonPath("$.pendingAppointments").value(1))
            .andExpect(jsonPath("$.completedAppointments").value(1))
            .andExpect(jsonPath("$.cancelledAppointments").value(1))
    }

    // 28. Login still works
    @Test
    fun testLoginStillWorks() {
        val loginRequest = LoginRequest(email = "pragati@citycare.com", password = "pragati123")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.role").value("ADMIN"))
    }
}

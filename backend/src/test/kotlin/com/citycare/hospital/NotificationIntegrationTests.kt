package com.citycare.hospital

import com.citycare.hospital.dto.*
import com.citycare.hospital.model.*
import com.citycare.hospital.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
class NotificationIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        notificationRepository.deleteAll()
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

    private fun getAdminUser(): User {
        return userRepository.findByEmail("pragati@citycare.com").get()
    }

    private fun getDoctorUser(): User {
        return userRepository.findByEmail("rahul.sharma@citycare.com").get()
    }

    private fun getOrCreatePatient(): Patient {
        val existing = patientRepository.findAll().firstOrNull()
        if (existing != null) return existing
        return patientRepository.save(
            Patient(patientId = "PAT-0001", name = "Notification Patient", age = 30, gender = "Male", disease = "Flu", phone = "9876543210", address = "City Center")
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

    // 1. Unauthenticated GET /api/notifications -> 401
    @Test
    fun testUnauthenticatedGetNotificationsReturns401() {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isUnauthorized)
    }

    // 2. Admin can view notifications -> 200
    @Test
    fun testAdminCanViewNotifications() {
        val adminSession = getAdminSession()
        mockMvc.perform(get("/api/notifications").session(adminSession))
            .andExpect(status().isOk)
    }

    // 3. Doctor can view notifications -> 200
    @Test
    fun testDoctorCanViewNotifications() {
        val doctorSession = getDoctorSession()
        mockMvc.perform(get("/api/notifications").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 4. Unread count endpoint -> returns count
    @Test
    fun testGetUnreadCount() {
        val adminSession = getAdminSession()
        val adminUser = getAdminUser()

        notificationRepository.save(
            Notification(user = adminUser, title = "Test 1", message = "Msg 1", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )
        notificationRepository.save(
            Notification(user = adminUser, title = "Test 2", message = "Msg 2", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )

        mockMvc.perform(get("/api/notifications/unread-count").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unreadCount").value(2))
    }

    // 5. Mark notification as read -> 200 & isRead=true
    @Test
    fun testMarkAsRead() {
        val adminSession = getAdminSession()
        val adminUser = getAdminUser()

        val notification = notificationRepository.save(
            Notification(user = adminUser, title = "Test", message = "Msg", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )

        mockMvc.perform(
            put("/api/notifications/${notification.id}/read")
                .session(adminSession)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isRead").value(true))
    }

    // 6. Mark all as read -> 200 & unread count 0
    @Test
    fun testMarkAllAsRead() {
        val adminSession = getAdminSession()
        val adminUser = getAdminUser()

        notificationRepository.save(
            Notification(user = adminUser, title = "T1", message = "M1", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )
        notificationRepository.save(
            Notification(user = adminUser, title = "T2", message = "M2", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )

        mockMvc.perform(
            put("/api/notifications/read-all")
                .session(adminSession)
        )
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/notifications/unread-count").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unreadCount").value(0))
    }

    // 7. User isolation security: marking someone else's notification as read -> 403 Forbidden
    @Test
    fun testUserIsolationMarkAsRead() {
        val adminSession = getAdminSession()
        val doctorUser = getDoctorUser()

        val doctorNotification = notificationRepository.save(
            Notification(user = doctorUser, title = "Doc Notif", message = "Doc Msg", type = NotificationType.APPOINTMENT_SCHEDULED, isRead = false)
        )

        // Admin tries to mark doctor's notification as read
        mockMvc.perform(
            put("/api/notifications/${doctorNotification.id}/read")
                .session(adminSession)
        )
            .andExpect(status().isForbidden)
    }

    // 8. Appointment scheduling triggers notification
    @Test
    fun testAppointmentSchedulingTriggersNotification() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val appointmentRequest = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = doctor.department?.id,
            appointmentDate = LocalDate.now().plusDays(1),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Routine checkup",
            status = AppointmentStatus.SCHEDULED
        )

        mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest))
        ).andExpect(status().isCreated)

        val notifications = notificationRepository.findAll()
        assertTrue(notifications.any { it.type == NotificationType.APPOINTMENT_SCHEDULED })
    }

    // 9. Appointment completion triggers notification
    @Test
    fun testAppointmentCompletionTriggersNotification() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val appointmentRequest = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = doctor.department?.id,
            appointmentDate = LocalDate.now(),
            appointmentTime = LocalTime.of(11, 0),
            reason = "Consultation",
            status = AppointmentStatus.SCHEDULED
        )

        val createResult = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(appointmentRequest))
        ).andExpect(status().isCreated).andReturn()

        val responseJson = objectMapper.readTree(createResult.response.contentAsString)
        val appointmentId = responseJson.get("id").asLong()

        // Update to COMPLETED
        val updateRequest = AppointmentRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            departmentId = doctor.department?.id,
            appointmentDate = LocalDate.now(),
            appointmentTime = LocalTime.of(11, 0),
            reason = "Consultation",
            status = AppointmentStatus.COMPLETED
        )

        mockMvc.perform(
            put("/api/appointments/$appointmentId")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpect(status().isOk)

        val notifications = notificationRepository.findAll()
        assertTrue(notifications.any { it.type == NotificationType.APPOINTMENT_COMPLETED })
    }

    // 10. Medical test completion triggers notification
    @Test
    fun testMedicalTestCompletionTriggersNotification() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val testRequest = MedicalTestRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            testName = "X-Ray Chest",
            testDate = LocalDate.now(),
            result = "Normal lung fields",
            status = MedicalTestStatus.COMPLETED
        )

        mockMvc.perform(
            post("/api/medical-tests")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest))
        ).andExpect(status().isCreated)

        val notifications = notificationRepository.findAll()
        assertTrue(notifications.any { it.type == NotificationType.MEDICAL_TEST_COMPLETED })
    }

    // 11. Prescription creation triggers notification
    @Test
    fun testPrescriptionCreationTriggersNotification() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()
        val doctor = getDoctor()

        val prescriptionRequest = PrescriptionRequest(
            patientId = patient.id,
            doctorId = doctor.id,
            prescriptionDate = LocalDate.now(),
            medicine = "Paracetamol 500mg",
            dosage = "1 tablet twice daily",
            duration = "5 days",
            instructions = "Take after meals"
        )

        mockMvc.perform(
            post("/api/prescriptions")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(prescriptionRequest))
        ).andExpect(status().isCreated)

        val notifications = notificationRepository.findAll()
        assertTrue(notifications.any { it.type == NotificationType.PRESCRIPTION_ADDED })
    }

    // 12. Patient registration triggers PATIENT_REGISTERED notification
    @Test
    fun testPatientRegistrationTriggersNotification() {
        val adminSession = getAdminSession()

        val patientRequest = PatientRequest(
            name = "New Registered Patient",
            age = 28,
            gender = "Female",
            disease = "Asthma",
            phone = "9123456780",
            address = "North Street"
        )

        mockMvc.perform(
            post("/api/patients")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patientRequest))
        ).andExpect(status().isCreated)

        val notifications = notificationRepository.findAll()
        assertTrue(notifications.any { it.type == NotificationType.PATIENT_REGISTERED })
    }

    // 13. Delete notification / cleanup
    @Test
    fun testDeleteNotification() {
        val adminSession = getAdminSession()
        val adminUser = getAdminUser()

        val notification = notificationRepository.save(
            Notification(user = adminUser, title = "Delete Me", message = "Msg", type = NotificationType.APPOINTMENT_SCHEDULED)
        )

        val notifId = notification.id!!
        mockMvc.perform(
            delete("/api/notifications/$notifId")
                .session(adminSession)
        ).andExpect(status().isOk)

        assertFalse(notificationRepository.findById(notifId).isPresent)
    }
}

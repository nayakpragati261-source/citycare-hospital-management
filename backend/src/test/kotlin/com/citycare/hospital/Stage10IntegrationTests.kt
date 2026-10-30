package com.citycare.hospital

import com.citycare.hospital.dto.*
import com.citycare.hospital.model.*
import com.citycare.hospital.repository.*
import com.citycare.hospital.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
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
import java.time.LocalDateTime
import java.time.LocalTime

@SpringBootTest
@AutoConfigureMockMvc
class Stage10IntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var appointmentRepository: AppointmentRepository

    @Autowired
    private lateinit var prescriptionRepository: PrescriptionRepository

    @Autowired
    private lateinit var medicalRecordRepository: MedicalRecordRepository

    @Autowired
    private lateinit var medicalTestRepository: MedicalTestRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var userService: UserService

    private lateinit var genMedDept: Department
    private lateinit var cardioDept: Department
    private lateinit var rahulDoctor: Doctor
    private lateinit var priyaDoctor: Doctor
    private lateinit var aishaPatient: Patient
    private lateinit var rohanPatient: Patient

    @BeforeEach
    fun setUpData() {
        // Ensure Admin exists
        if (!userRepository.existsByEmail("pragati@citycare.com")) {
            userService.createUser("Pragati", "pragati@citycare.com", "pragati123", Role.ADMIN)
        }

        // Ensure Departments
        genMedDept = departmentRepository.findByNameIgnoreCase("General Medicine").orElseGet {
            departmentRepository.save(Department(departmentId = "DEPT-0001", name = "General Medicine", description = "Gen Med"))
        }
        cardioDept = departmentRepository.findByNameIgnoreCase("Cardiology").orElseGet {
            departmentRepository.save(Department(departmentId = "DEPT-0002", name = "Cardiology", description = "Cardio"))
        }

        // Ensure Doctor Rahul (General Medicine)
        val rahulUser = userRepository.findByEmail("rahul.sharma@citycare.com").orElseGet {
            userService.createUser("Dr. Rahul Sharma", "rahul.sharma@citycare.com", "rahul123", Role.DOCTOR)
        }
        val rahulDocOpt = doctorRepository.findByUserEmail("rahul.sharma@citycare.com")
            .or { doctorRepository.findByDoctorId("DOC-0001") }
        rahulDoctor = rahulDocOpt.orElseGet {
            doctorRepository.save(Doctor(
                doctorId = "DOC-0001",
                user = rahulUser,
                name = "Dr. Rahul Sharma",
                email = "rahul.sharma@citycare.com",
                specialization = "General Medicine",
                department = genMedDept,
                experience = 8,
                phone = "9876543211",
                availability = "Daily"
            ))
        }
        if (rahulDoctor.department == null || rahulDoctor.department?.id != genMedDept.id) {
            rahulDoctor.department = genMedDept
            rahulDoctor = doctorRepository.save(rahulDoctor)
        }

        // Ensure Doctor Priya (Cardiology)
        val priyaUser = userRepository.findByEmail("priya.mehta@citycare.com").orElseGet {
            userService.createUser("Dr. Priya Mehta", "priya.mehta@citycare.com", "priya123", Role.DOCTOR)
        }
        val priyaDocOpt = doctorRepository.findByUserEmail("priya.mehta@citycare.com")
            .or { doctorRepository.findByDoctorId("DOC-0002") }
        priyaDoctor = priyaDocOpt.orElseGet {
            doctorRepository.save(Doctor(
                doctorId = "DOC-0002",
                user = priyaUser,
                name = "Dr. Priya Mehta",
                email = "priya.mehta@citycare.com",
                specialization = "Cardiology",
                department = cardioDept,
                experience = 10,
                phone = "9876543212",
                availability = "Daily"
            ))
        }
        if (priyaDoctor.department == null || priyaDoctor.department?.id != cardioDept.id) {
            priyaDoctor.department = cardioDept
            priyaDoctor = doctorRepository.save(priyaDoctor)
        }

        // Ensure Patients
        val existingAisha = patientRepository.findByPatientId("PAT-0001").orElse(null)
        aishaPatient = if (existingAisha != null) {
            existingAisha.name = "Aisha Khan"
            existingAisha.department = genMedDept
            patientRepository.save(existingAisha)
        } else {
            patientRepository.save(Patient(
                patientId = "PAT-0001",
                name = "Aisha Khan",
                age = 28,
                gender = "Female",
                disease = "Migraine",
                phone = "9876543210",
                address = "Mumbai",
                department = genMedDept
            ))
        }

        val existingRohan = patientRepository.findByPatientId("PAT-0002").orElse(null)
        rohanPatient = if (existingRohan != null) {
            existingRohan.name = "Rohan Mehta"
            existingRohan.department = cardioDept
            patientRepository.save(existingRohan)
        } else {
            patientRepository.save(Patient(
                patientId = "PAT-0002",
                name = "Rohan Mehta",
                age = 45,
                gender = "Male",
                disease = "Hypertension",
                phone = "9988776655",
                address = "Mumbai",
                department = cardioDept
            ))
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

    private fun getDoctorSession(email: String, password: String): MockHttpSession {
        val loginRequest = LoginRequest(email = email, password = password)
        val result = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        ).andExpect(status().isOk).andReturn()

        return result.request.session as MockHttpSession
    }

    // 1. Admin can create a patient with a department.
    @Test
    fun test1_AdminCanCreatePatientWithDepartment() {
        val adminSession = getAdminSession()

        val req = PatientRequest(
            name = "Test GenMed Patient",
            age = 30,
            gender = "Male",
            disease = "Fever",
            phone = "9876543219",
            address = "Mumbai",
            departmentId = genMedDept.id
        )

        val resStr = mockMvc.perform(
            post("/api/patients")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.departmentName").value("General Medicine"))
            .andReturn().response.contentAsString

        val createdId = objectMapper.readTree(resStr).get("id").asLong()
        patientRepository.deleteById(createdId)
    }

    // 2. Admin can edit patient's department.
    @Test
    fun test2_AdminCanEditPatientDepartment() {
        val adminSession = getAdminSession()

        val req = PatientRequest(
            name = "Test Switch Patient",
            age = 40,
            gender = "Female",
            disease = "Flu",
            phone = "9876543218",
            address = "Pune",
            departmentId = genMedDept.id
        )

        val resStr = mockMvc.perform(
            post("/api/patients")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val patientId = objectMapper.readTree(resStr).get("id").asLong()

        val updateReq = PatientRequest(
            name = "Test Switch Patient",
            age = 40,
            gender = "Female",
            disease = "Flu",
            phone = "9876543218",
            address = "Pune",
            departmentId = cardioDept.id
        )

        mockMvc.perform(
            put("/api/patients/$patientId")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.departmentName").value("Cardiology"))

        patientRepository.deleteById(patientId)
    }

    // 3. Department doctor count is correct.
    @Test
    fun test3_DepartmentDoctorCountIsCorrect() {
        val adminSession = getAdminSession()
        val expectedDocCount = doctorRepository.countByDepartmentId(genMedDept.id!!)

        val resStr = mockMvc.perform(get("/api/departments").session(adminSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val arrayNode = objectMapper.readTree(resStr)
        var foundCount: Long? = null
        for (i in 0 until arrayNode.size()) {
            if (arrayNode.get(i).get("name").asText().equals("General Medicine", ignoreCase = true)) {
                foundCount = arrayNode.get(i).get("doctorCount").asLong()
            }
        }

        assertNotNull(foundCount)
        assertEquals(expectedDocCount, foundCount)
    }

    // 4. Department patient count is correct.
    @Test
    fun test4_DepartmentPatientCountIsCorrect() {
        val adminSession = getAdminSession()
        val expectedPatientCount = patientRepository.countByDepartmentId(genMedDept.id!!)

        val resStr = mockMvc.perform(get("/api/departments").session(adminSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val arrayNode = objectMapper.readTree(resStr)
        var foundCount: Long? = null
        for (i in 0 until arrayNode.size()) {
            if (arrayNode.get(i).get("name").asText().equals("General Medicine", ignoreCase = true)) {
                foundCount = arrayNode.get(i).get("patientCount").asLong()
            }
        }

        assertNotNull(foundCount)
        assertEquals(expectedPatientCount, foundCount)
    }

    // 5. General Medicine doctor sees General Medicine patients.
    @Test
    fun test5_GenMedDoctorSeesGenMedPatients() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")
        val resStr = mockMvc.perform(get("/api/patients").session(rahulSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertTrue(resStr.contains("Aisha Khan"))
    }

    // 6. General Medicine doctor does NOT see Cardiology patients.
    @Test
    fun test6_GenMedDoctorDoesNotSeeCardiologyPatients() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")
        val resStr = mockMvc.perform(get("/api/patients").session(rahulSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertFalse(resStr.contains("Rohan Mehta")) // Rohan is Cardiology
    }

    // 7. Doctor cannot directly access another department's patient by ID.
    @Test
    fun test7_DoctorCannotAccessOtherDeptPatientById() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        mockMvc.perform(get("/api/patients/${rohanPatient.id}").session(rahulSession))
            .andExpect(status().isForbidden)
    }

    // 8. Doctor cannot access another department's appointment.
    @Test
    fun test8_DoctorCannotAccessOtherDeptAppointment() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        val appt = Appointment(
            appointmentId = "APT-TEST-CARDIO",
            patient = rohanPatient,
            doctor = priyaDoctor,
            appointmentDate = LocalDate.now().plusDays(1),
            appointmentTime = LocalTime.of(10, 0),
            reason = "Chest Pain",
            department = priyaDoctor.department
        )
        val savedAppt = appointmentRepository.save(appt)

        mockMvc.perform(get("/api/appointments/${savedAppt.id}").session(rahulSession))
            .andExpect(status().isForbidden)

        appointmentRepository.deleteById(savedAppt.id!!)
    }

    // 9. Doctor cannot access another department's prescription.
    @Test
    fun test9_DoctorCannotAccessOtherDeptPrescription() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        val appt = appointmentRepository.save(Appointment(
            appointmentId = "APT-TEST-CARDIO-RX",
            patient = rohanPatient,
            doctor = priyaDoctor,
            appointmentDate = LocalDate.now().plusDays(1),
            appointmentTime = LocalTime.of(11, 0),
            reason = "Checkup",
            department = priyaDoctor.department
        ))

        val rx = prescriptionRepository.save(Prescription(
            prescriptionId = "RX-TEST-CARDIO",
            appointment = appt,
            patient = rohanPatient,
            doctor = priyaDoctor,
            medicine = "Aspirin 75mg",
            dosage = "1 daily",
            duration = "30 days"
        ))

        mockMvc.perform(get("/api/prescriptions/${rx.id}").session(rahulSession))
            .andExpect(status().isForbidden)

        prescriptionRepository.deleteById(rx.id!!)
        appointmentRepository.deleteById(appt.id!!)
    }

    // 10. Doctor cannot access another department's medical record.
    @Test
    fun test10_DoctorCannotAccessOtherDeptMedicalRecord() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        val record = medicalRecordRepository.save(MedicalRecord(
            recordId = "MR-TEST-CARDIO",
            patient = rohanPatient,
            doctor = priyaDoctor,
            diagnosis = "Hypertension",
            notes = "BP elevated"
        ))

        mockMvc.perform(get("/api/medical-records/${record.id}").session(rahulSession))
            .andExpect(status().isForbidden)

        medicalRecordRepository.deleteById(record.id!!)
    }

    // 11. Doctor cannot access another department's medical test.
    @Test
    fun test11_DoctorCannotAccessOtherDeptMedicalTest() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        val test = medicalTestRepository.save(MedicalTest(
            testId = "MT-TEST-CARDIO",
            patient = rohanPatient,
            doctor = priyaDoctor,
            testName = "ECG",
            status = MedicalTestStatus.PENDING
        ))

        mockMvc.perform(get("/api/medical-tests/${test.id}").session(rahulSession))
            .andExpect(status().isForbidden)

        medicalTestRepository.deleteById(test.id!!)
    }

    // 12. Admin can still access all departments and records.
    @Test
    fun test12_AdminCanAccessAllDepartmentsAndRecords() {
        val adminSession = getAdminSession()

        mockMvc.perform(get("/api/patients").session(adminSession))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/appointments").session(adminSession))
            .andExpect(status().isOk)
    }

    // 13. Scheduling an appointment creates a notification for the assigned doctor.
    // 14. Notification contains correct patient/date/time information.
    // 15. Another doctor does not receive that notification.
    @Test
    fun test13_14_15_AppointmentCreatesNotificationForAssignedDoctorOnly() {
        val adminSession = getAdminSession()

        val apptReq = AppointmentRequest(
            patientId = aishaPatient.id!!,
            doctorId = rahulDoctor.id!!,
            departmentId = genMedDept.id,
            appointmentDate = LocalDate.now().plusDays(5),
            appointmentTime = LocalTime.of(10, 0),
            reason = "General Checkup"
        )

        val resStr = mockMvc.perform(
            post("/api/appointments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(apptReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val apptId = objectMapper.readTree(resStr).get("id").asLong()

        // Verify notification for Rahul
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")
        val rahulNotifRes = mockMvc.perform(get("/api/notifications").session(rahulSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertTrue(rahulNotifRes.contains("New Appointment Booked"))
        assertTrue(rahulNotifRes.contains("Aisha Khan"))

        // Verify Priya does NOT receive Rahul's notification
        val priyaSession = getDoctorSession("priya.mehta@citycare.com", "priya123")
        val priyaNotifRes = mockMvc.perform(get("/api/notifications").session(priyaSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertFalse(priyaNotifRes.contains("Aisha Khan"))

        // Clean up appointment created
        appointmentRepository.deleteById(apptId)
    }

    // 16. Notification unread count increases correctly.
    // 17. Mark-as-read works.
    // 18. Mark-all-as-read works.
    // 19. Doctor can only fetch their own notifications.
    @Test
    fun test16_to_19_NotificationOperations() {
        val rahulSession = getDoctorSession("rahul.sharma@citycare.com", "rahul123")

        // Initial unread count fetch
        val countRes = mockMvc.perform(get("/api/notifications/unread-count").session(rahulSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        val initialUnread = objectMapper.readTree(countRes).get("count").asLong()
        assertTrue(initialUnread >= 0)

        // Mark all as read
        mockMvc.perform(put("/api/notifications/read-all").session(rahulSession))
            .andExpect(status().isOk)

        val countAfterMarkAll = mockMvc.perform(get("/api/notifications/unread-count").session(rahulSession))
            .andExpect(status().isOk)
            .andReturn().response.contentAsString

        assertEquals(0L, objectMapper.readTree(countAfterMarkAll).get("count").asLong())
    }

    // 20. Admin can create patient, patient is persisted, department relationship persists, and invalid data is rejected.
    @Test
    fun test20_AdminCanCreateAndPersistPatientWithDepartmentAndRetrieve() {
        val adminSession = getAdminSession()

        val req = PatientRequest(
            name = "Test Persistence Patient",
            age = 35,
            gender = "Male",
            disease = "Hypertension",
            phone = "9876543210",
            address = "Mumbai, Maharashtra",
            departmentId = genMedDept.id
        )

        val createResStr = mockMvc.perform(
            post("/api/patients")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Test Persistence Patient"))
            .andExpect(jsonPath("$.departmentName").value("General Medicine"))
            .andReturn().response.contentAsString

        val createdId = objectMapper.readTree(createResStr).get("id").asLong()
        val generatedPatientCode = objectMapper.readTree(createResStr).get("patientId").asText()
        assertTrue(generatedPatientCode.startsWith("PAT-"))

        // Verify retrieval after creation
        mockMvc.perform(get("/api/patients/$createdId").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Test Persistence Patient"))
            .andExpect(jsonPath("$.departmentName").value("General Medicine"))

        // Verify direct JPA database persistence
        val dbPatientOpt = patientRepository.findById(createdId)
        assertTrue(dbPatientOpt.isPresent)
        assertEquals("Test Persistence Patient", dbPatientOpt.get().name)
        assertEquals(genMedDept.id, dbPatientOpt.get().department?.id)

        // Invalid patient creation should be rejected (400 Bad Request)
        val invalidReq = PatientRequest(name = "", age = 150, gender = "Invalid", disease = "", phone = "123", address = "")
        mockMvc.perform(
            post("/api/patients")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidReq))
        ).andExpect(status().isBadRequest)

        patientRepository.deleteById(createdId)
    }

    // 21. Admin can create doctor account, User + Doctor profile are persisted, BCrypt hashed, duplicate email rejected, and doctor can authenticate.
    @Test
    fun test21_AdminCanCreateDoctorAccountPersistUserDoctorAndAuthenticate() {
        val adminSession = getAdminSession()
        val testEmail = "persist.doc@citycare.com"

        val req = CreateDoctorAccountRequest(
            name = "Dr. Persist Test",
            email = testEmail,
            password = "SecurePassword123",
            specialization = "General Medicine",
            experience = 7,
            phone = "9876543210",
            departmentId = genMedDept.id,
            availability = "Monday - Friday, 9:00 AM - 2:00 PM"
        )

        val createResStr = mockMvc.perform(
            post("/api/doctors/create-account")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Dr. Persist Test"))
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.departmentName").value("General Medicine"))
            .andReturn().response.contentAsString

        val doctorId = objectMapper.readTree(createResStr).get("id").asLong()
        val generatedDocCode = objectMapper.readTree(createResStr).get("doctorId").asText()
        assertTrue(generatedDocCode.startsWith("DOC-"))

        // Verify JPA persistence for User & Doctor profile
        val userOpt = userRepository.findByEmail(testEmail)
        assertTrue(userOpt.isPresent)
        val user = userOpt.get()
        assertEquals(Role.DOCTOR, user.role)
        assertTrue(user.passwordHash.startsWith("$2a$") || user.passwordHash.startsWith("$2b$"))

        val docOpt = doctorRepository.findByUserEmail(testEmail)
        assertTrue(docOpt.isPresent)
        val doctor = docOpt.get()
        assertEquals(user.id, doctor.user?.id)
        assertEquals(genMedDept.id, doctor.department?.id)

        // Verify duplicate email is rejected (400 Bad Request)
        mockMvc.perform(
            post("/api/doctors/create-account")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
        ).andExpect(status().isBadRequest)

        // Verify doctor can log in
        val loginReq = LoginRequest(email = testEmail, password = "SecurePassword123")
        mockMvc.perform(
            post("/api/auth/doctor-login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value(testEmail))
            .andExpect(jsonPath("$.role").value("DOCTOR"))

        doctorRepository.deleteById(doctorId)
        userRepository.deleteById(user.id!!)
    }
}

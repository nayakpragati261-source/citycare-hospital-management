package com.citycare.hospital

import com.citycare.hospital.dto.BillRequest
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.model.*
import com.citycare.hospital.repository.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.startsWith
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
import java.math.BigDecimal
import java.time.LocalDate

@SpringBootTest
@AutoConfigureMockMvc
class BillIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var patientRepository: PatientRepository

    @Autowired
    private lateinit var doctorRepository: DoctorRepository

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        billRepository.deleteAll()
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
            Patient(patientId = "PAT-0001", name = "Test Bill Patient", age = 40, gender = "Male", disease = "Fever", phone = "9876543210", address = "Main Street")
        )
    }

    // 1. Unauthenticated request -> 401
    @Test
    fun testUnauthenticatedGetBillsReturns401() {
        mockMvc.perform(get("/api/bills"))
            .andExpect(status().isUnauthorized)
    }

    // 2. Admin can view all bills -> 200
    @Test
    fun testAdminCanViewAllBills() {
        val adminSession = getAdminSession()
        mockMvc.perform(get("/api/bills").session(adminSession))
            .andExpect(status().isOk)
    }

    // 3. Doctor can view bills (view-only) -> 200
    @Test
    fun testDoctorCanViewBills() {
        val doctorSession = getDoctorSession()
        mockMvc.perform(get("/api/bills").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 4. Admin can create a bill -> 201
    @Test
    fun testAdminCanCreateBill() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val request = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("500.00"),
            testCharges = BigDecimal("300.00"),
            medicineCharges = BigDecimal("200.00"),
            paymentStatus = PaymentStatus.PENDING
        )

        mockMvc.perform(
            post("/api/bills")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.billId").value(startsWith("BILL-")))
            .andExpect(jsonPath("$.totalAmount").value(1000.00))
            .andExpect(jsonPath("$.paymentStatus").value("PENDING"))
    }

    // 5. Doctor trying to create a bill -> 403 Forbidden
    @Test
    fun testDoctorCannotCreateBill() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()

        val request = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("500.00"),
            testCharges = BigDecimal("0.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PENDING
        )

        mockMvc.perform(
            post("/api/bills")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)
    }

    // 6. Doctor trying to update a bill -> 403 Forbidden
    @Test
    fun testDoctorCannotUpdateBill() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()

        val bill = billRepository.save(
            Bill(
                billId = "BILL-0001",
                patient = patient,
                billDate = LocalDate.now(),
                consultationFee = BigDecimal("500.00"),
                testCharges = BigDecimal("0.00"),
                medicineCharges = BigDecimal("0.00"),
                totalAmount = BigDecimal("500.00"),
                paymentStatus = PaymentStatus.PENDING
            )
        )

        val updateRequest = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("500.00"),
            testCharges = BigDecimal("100.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PAID
        )

        mockMvc.perform(
            put("/api/bills/${bill.id}")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isForbidden)
    }

    // 7. Doctor trying to delete a bill -> 403 Forbidden
    @Test
    fun testDoctorCannotDeleteBill() {
        val doctorSession = getDoctorSession()
        val patient = getOrCreatePatient()

        val bill = billRepository.save(
            Bill(
                billId = "BILL-0001",
                patient = patient,
                billDate = LocalDate.now(),
                consultationFee = BigDecimal("500.00"),
                testCharges = BigDecimal("0.00"),
                medicineCharges = BigDecimal("0.00"),
                totalAmount = BigDecimal("500.00"),
                paymentStatus = PaymentStatus.PENDING
            )
        )

        mockMvc.perform(
            delete("/api/bills/${bill.id}")
                .session(doctorSession)
        )
            .andExpect(status().isForbidden)
    }

    // 8. Auto-generated BILL ID format BILL-xxxx
    @Test
    fun testBillIdGenerationFormat() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val request = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("100.00"),
            testCharges = BigDecimal("0.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PENDING
        )

        mockMvc.perform(
            post("/api/bills")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.billId").value(startsWith("BILL-")))
    }

    // 9. Total calculation authoritatively calculated on backend
    @Test
    fun testAuthoritativeBackendTotalCalculation() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val request = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("450.50"),
            testCharges = BigDecimal("250.25"),
            medicineCharges = BigDecimal("100.25"),
            paymentStatus = PaymentStatus.PENDING
        )

        // 450.50 + 250.25 + 100.25 = 801.00
        mockMvc.perform(
            post("/api/bills")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.totalAmount").value(801.00))
    }

    // 10. Negative charges validation -> 400 Bad Request
    @Test
    fun testNegativeChargesReturnsBadRequest() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val request = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("-100.00"),
            testCharges = BigDecimal("0.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PENDING
        )

        mockMvc.perform(
            post("/api/bills")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // 11. Non-existent patient -> 404 Not Found
    @Test
    fun testNonExistentPatientReturnsNotFound() {
        val adminSession = getAdminSession()

        val request = BillRequest(
            patientId = 999999L,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("100.00"),
            testCharges = BigDecimal("0.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PENDING
        )

        mockMvc.perform(
            post("/api/bills")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    // 12. Admin can update bill status -> 200
    @Test
    fun testAdminCanUpdateBillStatus() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val bill = billRepository.save(
            Bill(
                billId = "BILL-0001",
                patient = patient,
                billDate = LocalDate.now(),
                consultationFee = BigDecimal("500.00"),
                testCharges = BigDecimal("0.00"),
                medicineCharges = BigDecimal("0.00"),
                totalAmount = BigDecimal("500.00"),
                paymentStatus = PaymentStatus.PENDING
            )
        )

        val updateRequest = BillRequest(
            patientId = patient.id,
            billDate = LocalDate.now(),
            consultationFee = BigDecimal("500.00"),
            testCharges = BigDecimal("100.00"),
            medicineCharges = BigDecimal("0.00"),
            paymentStatus = PaymentStatus.PAID
        )

        mockMvc.perform(
            put("/api/bills/${bill.id}")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paymentStatus").value("PAID"))
            .andExpect(jsonPath("$.totalAmount").value(600.00))
    }

    // 13. Filter bills by payment status -> 200
    @Test
    fun testFilterBillsByStatus() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        billRepository.save(
            Bill(billId = "BILL-0001", patient = patient, billDate = LocalDate.now(), consultationFee = BigDecimal("100.00"), totalAmount = BigDecimal("100.00"), paymentStatus = PaymentStatus.PAID)
        )
        billRepository.save(
            Bill(billId = "BILL-0002", patient = patient, billDate = LocalDate.now(), consultationFee = BigDecimal("200.00"), totalAmount = BigDecimal("200.00"), paymentStatus = PaymentStatus.PENDING)
        )

        mockMvc.perform(
            get("/api/bills?status=PAID")
                .session(adminSession)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].paymentStatus").value("PAID"))
    }

    // 14. Filter bills by patientId -> 200
    @Test
    fun testFilterBillsByPatientId() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        billRepository.save(
            Bill(billId = "BILL-0001", patient = patient, billDate = LocalDate.now(), consultationFee = BigDecimal("100.00"), totalAmount = BigDecimal("100.00"), paymentStatus = PaymentStatus.PENDING)
        )

        mockMvc.perform(
            get("/api/bills?patientId=${patient.id}")
                .session(adminSession)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].patientId").value(patient.id))
    }

    // 15. Admin can delete a bill -> 200
    @Test
    fun testAdminCanDeleteBill() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val bill = billRepository.save(
            Bill(billId = "BILL-0001", patient = patient, billDate = LocalDate.now(), consultationFee = BigDecimal("100.00"), totalAmount = BigDecimal("100.00"), paymentStatus = PaymentStatus.PENDING)
        )

        mockMvc.perform(
            delete("/api/bills/${bill.id}")
                .session(adminSession)
        )
            .andExpect(status().isOk)

        val billId = bill.id!!
        assertFalse(billRepository.findById(billId).isPresent)
    }

    // 16. Verify bill get by ID
    @Test
    fun testGetBillById() {
        val adminSession = getAdminSession()
        val patient = getOrCreatePatient()

        val bill = billRepository.save(
            Bill(billId = "BILL-0001", patient = patient, billDate = LocalDate.now(), consultationFee = BigDecimal("150.00"), totalAmount = BigDecimal("150.00"), paymentStatus = PaymentStatus.PENDING)
        )

        mockMvc.perform(
            get("/api/bills/${bill.id}")
                .session(adminSession)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalAmount").value(150.00))
    }
}

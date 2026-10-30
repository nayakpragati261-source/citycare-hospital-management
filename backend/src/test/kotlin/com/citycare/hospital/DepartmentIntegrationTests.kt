package com.citycare.hospital

import com.citycare.hospital.dto.DepartmentRequest
import com.citycare.hospital.dto.LoginRequest
import com.citycare.hospital.repository.DepartmentRepository
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
class DepartmentIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var departmentRepository: DepartmentRepository

    private val starterDeptNames = setOf(
        "general medicine", "cardiology", "orthopedics", "pediatrics", "dermatology"
    )

    @AfterEach
    fun cleanUpCustomDepartments() {
        departmentRepository.findAll().forEach { dept ->
            if (!starterDeptNames.contains(dept.name.lowercase())) {
                departmentRepository.delete(dept)
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

    // 1. Unauthenticated GET /api/departments returns 401
    @Test
    fun testUnauthenticatedGetDepartmentsReturns401() {
        mockMvc.perform(get("/api/departments"))
            .andExpect(status().isUnauthorized)
    }

    // 2 & 6 & 13. Admin & Doctor can GET departments; Starter departments exist
    @Test
    fun testGetDepartmentsAndStarterDepartmentsExist() {
        val adminSession = getAdminSession()
        val resContent = mockMvc.perform(get("/api/departments").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].departmentId").exists())
            .andReturn().response.contentAsString

        assertTrue(resContent.contains("General Medicine"))
        assertTrue(resContent.contains("Cardiology"))
        assertTrue(resContent.contains("Orthopedics"))

        val doctorSession = getDoctorSession()
        mockMvc.perform(get("/api/departments").session(doctorSession))
            .andExpect(status().isOk)
    }

    // 3 & 14. Admin can create department with DEPT-xxxx format
    @Test
    fun testAdminCanCreateDepartment() {
        val adminSession = getAdminSession()
        val request = DepartmentRequest(
            name = "Neurology",
            description = "Diagnosis and treatment of nervous system disorders."
        )

        mockMvc.perform(
            post("/api/departments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.departmentId").exists())
            .andExpect(jsonPath("$.name").value("Neurology"))
    }

    // 4. Admin can update department
    @Test
    fun testAdminCanUpdateDepartment() {
        val adminSession = getAdminSession()
        val createReq = DepartmentRequest(name = "Psychiatry", description = "Mental health care.")
        val createResStr = mockMvc.perform(
            post("/api/departments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(createResStr).get("id").asLong()

        val updateReq = DepartmentRequest(name = "Psychiatry & Behavioral Health", description = "Updated description.")
        mockMvc.perform(
            put("/api/departments/$id")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Psychiatry & Behavioral Health"))
    }

    // 5 & 15. Admin can delete unused department
    @Test
    fun testAdminCanDeleteUnusedDepartment() {
        val adminSession = getAdminSession()
        val createReq = DepartmentRequest(name = "Temporary Dept", description = "To delete.")
        val createResStr = mockMvc.perform(
            post("/api/departments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createReq))
        ).andExpect(status().isCreated).andReturn().response.contentAsString

        val id = objectMapper.readTree(createResStr).get("id").asLong()

        mockMvc.perform(delete("/api/departments/$id").session(adminSession))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/departments/$id").session(adminSession))
            .andExpect(status().isNotFound)
    }

    // 7, 8, 9. Doctor cannot POST, PUT, DELETE (403 Forbidden)
    @Test
    fun testDoctorWriteEndpointsForbidden() {
        val doctorSession = getDoctorSession()
        val request = DepartmentRequest(name = "Forbidden Dept", description = "Fail")

        // POST -> 403
        mockMvc.perform(
            post("/api/departments")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)

        // PUT -> 403
        mockMvc.perform(
            put("/api/departments/1")
                .session(doctorSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isForbidden)

        // DELETE -> 403
        mockMvc.perform(delete("/api/departments/1").session(doctorSession))
            .andExpect(status().isForbidden)
    }

    // 10. Blank name rejected (400 Bad Request)
    @Test
    fun testBlankDepartmentNameRejected() {
        val adminSession = getAdminSession()
        val request = DepartmentRequest(name = "", description = "Invalid")

        mockMvc.perform(
            post("/api/departments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    // 11. Duplicate department name rejected case-insensitively (409 Conflict)
    @Test
    fun testDuplicateDepartmentNameRejectedCaseInsensitively() {
        val adminSession = getAdminSession()
        // Starter department "Cardiology" exists!
        val duplicateReq = DepartmentRequest(name = "cardiology", description = "Duplicate check")

        mockMvc.perform(
            post("/api/departments")
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateReq))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("A department with this name already exists."))
    }

    // 12. Non-existent department returns 404
    @Test
    fun testNonExistentDepartmentReturns404() {
        val adminSession = getAdminSession()
        mockMvc.perform(get("/api/departments/99999").session(adminSession))
            .andExpect(status().isNotFound)
    }

    // 20. Dashboard returns totalDepartments count
    @Test
    fun testDashboardReturnsTotalDepartmentsCount() {
        val adminSession = getAdminSession()
        val deptCount = departmentRepository.count()

        mockMvc.perform(get("/api/dashboard").session(adminSession))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalDepartments").value(deptCount))
    }
}

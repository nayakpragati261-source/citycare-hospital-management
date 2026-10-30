package com.citycare.hospital

import com.citycare.hospital.dto.LoginRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class DashboardIntegrationTests {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    // Test 1: Unauthenticated user cannot access /api/dashboard
    @Test
    fun testUnauthenticatedUserCannotAccessDashboard() {
        mockMvc.perform(get("/api/dashboard"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Unauthorized access"))
    }

    // Test 2 & 4 & 5: Authenticated ADMIN can access /api/dashboard, returns expected fields without sensitive info
    @Test
    fun testAuthenticatedAdminCanAccessDashboard() {
        val loginRequest = LoginRequest(
            email = "pragati@citycare.com",
            password = "pragati123"
        )

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val session = loginResult.request.session as MockHttpSession

        mockMvc.perform(
            get("/api/dashboard")
                .session(session)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPatients").exists())
            .andExpect(jsonPath("$.totalDoctors").exists())
            .andExpect(jsonPath("$.totalAppointments").exists())
            .andExpect(jsonPath("$.completedAppointments").exists())
            .andExpect(jsonPath("$.pendingAppointments").exists())
            .andExpect(jsonPath("$.cancelledAppointments").exists())
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist())
    }

    // Test 3: Authenticated DOCTOR can access /api/dashboard
    @Test
    fun testAuthenticatedDoctorCanAccessDashboard() {
        val loginRequest = LoginRequest(
            email = "rahul.sharma@citycare.com",
            password = "rahul123"
        )

        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andReturn()

        val session = loginResult.request.session as MockHttpSession

        mockMvc.perform(
            get("/api/dashboard")
                .session(session)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalPatients").exists())
            .andExpect(jsonPath("$.totalAppointments").exists())
    }
}

package com.citycare.hospital.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AuthInterceptor : HandlerInterceptor {

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        // OPTIONS requests for CORS preflight must pass through
        if ("OPTIONS".equals(request.method, ignoreCase = true)) {
            return true
        }

        val path = request.requestURI

        // Public endpoints bypass authentication check
        if (isPublicEndpoint(path)) {
            return true
        }

        // Check if server session contains authenticated user
        val session = request.getSession(false)
        val authUser = session?.getAttribute("user")

        if (authUser == null) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"message":"Unauthorized access"}""")
            return false
        }

        return true
    }

    private fun isPublicEndpoint(path: String): Boolean {
        return path == "/api/auth/login" ||
               path == "/api/auth/admin-login" ||
               path == "/api/auth/doctor-login" ||
               path == "/api/health" ||
               path == "/api/health/database"
    }
}

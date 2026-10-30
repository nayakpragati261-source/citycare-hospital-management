package com.citycare.hospital.service

import com.citycare.hospital.dto.HealthStatusResponse
import org.springframework.stereotype.Service
import javax.sql.DataSource
import java.sql.SQLException

@Service
class HealthService(private val dataSource: DataSource) {

    fun checkApplicationHealth(): HealthStatusResponse {
        return HealthStatusResponse(
            status = "OK",
            message = "CityCare Hospital API is running"
        )
    }

    fun checkDatabaseHealth(): HealthStatusResponse {
        return try {
            dataSource.connection.use { connection ->
                if (connection.isValid(2)) {
                    HealthStatusResponse(
                        status = "OK",
                        message = "Database connection successful"
                    )
                } else {
                    HealthStatusResponse(
                        status = "ERROR",
                        message = "Database connection invalid"
                    )
                }
            }
        } catch (ex: SQLException) {
            HealthStatusResponse(
                status = "ERROR",
                message = "Failed to connect to MySQL database"
            )
        }
    }
}

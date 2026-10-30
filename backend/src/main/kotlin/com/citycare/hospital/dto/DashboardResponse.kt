package com.citycare.hospital.dto

import java.math.BigDecimal

data class DashboardResponse(
    val totalPatients: Long = 0,
    val totalDoctors: Long = 0,
    val totalDepartments: Long = 0,
    val totalAppointments: Long = 0,
    val completedAppointments: Long = 0,
    val pendingAppointments: Long = 0,
    val cancelledAppointments: Long = 0,
    val totalPrescriptions: Long = 0,
    val totalMedicalRecords: Long = 0,
    val totalMedicalTests: Long = 0,
    val pendingMedicalTests: Long = 0,
    val totalBills: Long = 0,
    val paidBills: Long = 0,
    val pendingBills: Long = 0,
    val totalRevenue: BigDecimal? = null
)

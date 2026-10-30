package com.citycare.hospital.service

import com.citycare.hospital.dto.DashboardResponse
import com.citycare.hospital.model.AppointmentStatus
import com.citycare.hospital.model.MedicalTestStatus
import com.citycare.hospital.model.PaymentStatus
import com.citycare.hospital.repository.*
import org.springframework.stereotype.Service

@Service
class DashboardService(
    private val patientService: PatientService,
    private val doctorService: DoctorService,
    private val departmentService: DepartmentService,
    private val appointmentService: AppointmentService,
    private val prescriptionService: PrescriptionService,
    private val medicalRecordService: MedicalRecordService,
    private val medicalTestService: MedicalTestService,
    private val billService: BillService,
    private val doctorRepository: DoctorRepository,
    private val patientRepository: PatientRepository,
    private val appointmentRepository: AppointmentRepository,
    private val prescriptionRepository: PrescriptionRepository,
    private val medicalRecordRepository: MedicalRecordRepository,
    private val medicalTestRepository: MedicalTestRepository
) {

    fun getDashboardMetrics(userEmail: String, role: String): DashboardResponse {
        val isAdmin = role.equals("ADMIN", ignoreCase = true)
        val doctorOpt = if (!isAdmin) doctorRepository.findByUserEmail(userEmail) else null
        val doctorDeptId = doctorOpt?.orElse(null)?.department?.id

        val totalPatientsCount = if (isAdmin || doctorDeptId == null) {
            patientService.getTotalPatientCount()
        } else {
            patientRepository.countByDepartmentId(doctorDeptId)
        }

        val totalDoctorsCount = doctorService.getTotalDoctorCount()
        val totalDepartmentsCount = departmentService.getTotalDepartmentCount()

        val totalAppointmentsCount = if (isAdmin || doctorDeptId == null) {
            appointmentService.getTotalAppointmentCount()
        } else {
            appointmentRepository.countByDepartmentId(doctorDeptId)
        }

        val completedAppointmentsCount = if (isAdmin || doctorDeptId == null) {
            appointmentService.getAppointmentCountByStatus(AppointmentStatus.COMPLETED)
        } else {
            appointmentRepository.countByDepartmentIdAndStatus(doctorDeptId, AppointmentStatus.COMPLETED)
        }

        val pendingAppointmentsCount = if (isAdmin || doctorDeptId == null) {
            appointmentService.getAppointmentCountByStatus(AppointmentStatus.SCHEDULED)
        } else {
            appointmentRepository.countByDepartmentIdAndStatus(doctorDeptId, AppointmentStatus.SCHEDULED)
        }

        val cancelledAppointmentsCount = if (isAdmin || doctorDeptId == null) {
            appointmentService.getAppointmentCountByStatus(AppointmentStatus.CANCELLED)
        } else {
            appointmentRepository.countByDepartmentIdAndStatus(doctorDeptId, AppointmentStatus.CANCELLED)
        }

        val totalPrescriptionsCount = if (isAdmin || doctorDeptId == null) {
            prescriptionService.getTotalPrescriptionCount()
        } else {
            prescriptionRepository.countByPatientDepartmentIdOrDoctorDepartmentId(doctorDeptId, doctorDeptId)
        }

        val totalMedicalRecordsCount = if (isAdmin || doctorDeptId == null) {
            medicalRecordService.getTotalMedicalRecordCount()
        } else {
            medicalRecordRepository.countByPatientDepartmentIdOrDoctorDepartmentId(doctorDeptId, doctorDeptId)
        }

        val totalMedicalTestsCount = if (isAdmin || doctorDeptId == null) {
            medicalTestService.getTotalMedicalTestCount()
        } else {
            medicalTestRepository.countByPatientDepartmentIdOrDoctorDepartmentId(doctorDeptId, doctorDeptId)
        }

        val pendingMedicalTestsCount = if (isAdmin || doctorDeptId == null) {
            medicalTestService.getCountByStatus(MedicalTestStatus.PENDING)
        } else {
            medicalTestRepository.findByPatientDepartmentIdOrDoctorDepartmentId(doctorDeptId, doctorDeptId)
                .count { it.status == MedicalTestStatus.PENDING }.toLong()
        }

        val totalBillsCount = billService.getTotalBillCount()
        val paidBillsCount = billService.getCountByPaymentStatus(PaymentStatus.PAID)
        val pendingBillsCount = billService.getCountByPaymentStatus(PaymentStatus.PENDING)
        val revenue = if (isAdmin) billService.getTotalPaidRevenue() else null

        return DashboardResponse(
            totalPatients = totalPatientsCount,
            totalDoctors = totalDoctorsCount,
            totalDepartments = totalDepartmentsCount,
            totalAppointments = totalAppointmentsCount,
            completedAppointments = completedAppointmentsCount,
            pendingAppointments = pendingAppointmentsCount,
            cancelledAppointments = cancelledAppointmentsCount,
            totalPrescriptions = totalPrescriptionsCount,
            totalMedicalRecords = totalMedicalRecordsCount,
            totalMedicalTests = totalMedicalTestsCount,
            pendingMedicalTests = pendingMedicalTestsCount,
            totalBills = totalBillsCount,
            paidBills = paidBillsCount,
            pendingBills = pendingBillsCount,
            totalRevenue = revenue
        )
    }
}

# CityCare Hospital Management System

> **Development of a Secure Smart Hospital Management System for Efficient Patient, Doctor, Department, Appointment, Medical Record, Prescription, Test, Billing, and Notification Management**
> 
> *Complete 9-Stage Enterprise Application — CityCare Hospital Management System*

---

## 📋 Project Overview

CityCare Hospital Management System is built using a modern decoupled architecture:
- **Frontend**: React (JSX, Vite, Vanilla CSS, Fetch API, React Hooks)
- **Backend**: Kotlin + Spring Boot (Spring Web, Spring Data JPA, Hibernate, Bean Validation, Spring Security Crypto BCrypt, Server-side HTTP Sessions)
- **Database**: MySQL (`hospital_management`) on `localhost:3306`
- **Stages Completed (Stages 1–9)**:
  - **Stage 1**: Foundation & Database Setup
  - **Stage 2**: Authentication & Session Security (BCrypt)
  - **Stage 3**: Main Layout & Role-Based Navigation (ADMIN & DOCTOR)
  - **Stage 4**: Patient Management Module (`PAT-xxxx`)
  - **Stage 5**: Doctor Management & User-Doctor Linkage (`DOC-xxxx`)
  - **Stage 6**: Department Management Module (`DEPT-xxxx`)
  - **Stage 7**: Appointment Management Module (`APT-xxxx`)
  - **Stage 8**: Prescriptions (`RX-xxxx`) + Medical Records (`MR-xxxx`) + Medical Tests (`TEST-xxxx`)
  - **Stage 9**: Billing Module (`BILL-xxxx`), Notifications (`NOTIF-xxxx`), Final Security & Comprehensive UI Polish

---

## 🔑 Demo Login Credentials

| Role | Email | Password | Name | Dashboard View | Access Level |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **ADMIN** | `admin@citycare.com` | `admin123` | CityCare Administrator | System Administrator Portal | Full Management (CRUD all modules, Billing, Revenue) |
| **DOCTOR** | `rahul.sharma@citycare.com` | `rahul123` | Dr. Rahul Sharma | Doctor Medical Portal | Clinical View & Operations (My Patients, My Appointments, Prescriptions, Records, Tests, View-only Billing) |

---

## 📡 REST API Specifications (Stage 9 Final)

| Category | Endpoint | Method | Role Allowed | Description |
| :--- | :--- | :--- | :--- | :--- |
| **Auth** | `/api/auth/login` | `POST` | Public | Login with email & password |
| **Auth** | `/api/auth/me` | `GET` | Authenticated | Get current logged-in user profile |
| **Auth** | `/api/auth/logout` | `POST` | Authenticated | Destroy server-side session |
| **Dashboard** | `/api/dashboard` | `GET` | ADMIN / DOCTOR | Summary metrics (Revenue hidden from DOCTOR) |
| **Patients** | `/api/patients` | `GET`, `POST` | ADMIN / DOCTOR | List & register patients |
| **Patients** | `/api/patients/{id}` | `GET`, `PUT`, `DELETE` | ADMIN / DOCTOR | Patient details, update & delete |
| **Doctors** | `/api/doctors` | `GET` | ADMIN / DOCTOR | List all doctors |
| **Doctors** | `/api/doctors` | `POST` | ADMIN | Create doctor profile with user link |
| **Doctors** | `/api/doctors/{id}` | `PUT`, `DELETE` | ADMIN | Update & delete doctor |
| **Departments**| `/api/departments` | `GET` | ADMIN / DOCTOR | List departments |
| **Departments**| `/api/departments` | `POST`, `PUT`, `DELETE`| ADMIN | Department CRUD |
| **Appointments**| `/api/appointments` | `GET`, `POST` | ADMIN / DOCTOR | List & schedule appointments |
| **Appointments**| `/api/appointments/{id}`| `PUT`, `DELETE` | ADMIN / DOCTOR | Update & cancel/complete appointments |
| **Prescriptions**| `/api/prescriptions` | `GET`, `POST`, `PUT` | ADMIN / DOCTOR | Clinical prescriptions management |
| **Medical Records**| `/api/medical-records`| `GET`, `POST`, `PUT` | ADMIN / DOCTOR | Medical records & history tracking |
| **Medical Tests**| `/api/medical-tests` | `GET`, `POST`, `PUT` | ADMIN / DOCTOR | Diagnostic lab test orders & results |
| **Billing** | `/api/bills` | `GET` | ADMIN / DOCTOR | List bills (Doctor view-only) |
| **Billing** | `/api/bills` | `POST` | ADMIN | Create bill (Authoritative total calculated) |
| **Billing** | `/api/bills/{id}` | `PUT`, `DELETE` | ADMIN | Update status/amount & delete bill |
| **Notifications**| `/api/notifications` | `GET` | ADMIN / DOCTOR | List user notifications |
| **Notifications**| `/api/notifications/unread-count`| `GET` | ADMIN / DOCTOR | Unread notifications badge count |
| **Notifications**| `/api/notifications/{id}/read` | `PUT` | ADMIN / DOCTOR | Mark notification as read |
| **Notifications**| `/api/notifications/read-all` | `PUT` | ADMIN / DOCTOR | Mark all notifications as read |

---

## 🗄️ MySQL Database Schema

Database: `hospital_management` (MySQL on port 3306)

1. `users`: Authentication records (`id`, `email`, `password_hash`, `name`, `role`, `active`, `created_at`)
2. `patients`: Patient profiles (`id`, `patient_id` unique, `name`, `age`, `gender`, `disease`, `phone`, `address`, `created_at`, `updated_at`)
3. `doctors`: Doctor hospital profiles (`id`, `doctor_id` unique, `user_id` FK to `users.id`, `name`, `email`, `specialization`, `department_id` FK to `departments.id`, `experience`, `phone`, `availability`, `created_at`, `updated_at`)
4. `departments`: Hospital departments (`id`, `department_id` unique, `name`, `description`, `created_at`, `updated_at`)
5. `appointments`: Patient-Doctor appointments (`id`, `appointment_id` unique, `patient_id` FK, `doctor_id` FK, `department_id` FK, `appointment_date`, `appointment_time`, `reason`, `status`, `created_at`, `updated_at`)
6. `prescriptions`: Medication prescriptions (`id`, `prescription_id` unique, `patient_id` FK, `doctor_id` FK, `appointment_id` FK, `prescription_date`, `medicine`, `dosage`, `duration`, `instructions`, `created_at`, `updated_at`)
7. `medical_records`: Health history records (`id`, `record_id` unique, `patient_id` FK, `doctor_id` FK, `appointment_id` FK, `previous_disease`, `diagnosis`, `treatment`, `record_date`, `notes`, `created_at`, `updated_at`)
8. `medical_tests`: Diagnostic lab test orders (`id`, `test_id` unique, `patient_id` FK, `doctor_id` FK, `appointment_id` FK, `test_name`, `test_date`, `result`, `status`, `created_at`, `updated_at`)
9. `bills`: Billing invoices (`id`, `bill_id` unique, `patient_id` FK, `bill_date`, `consultation_fee` BigDecimal, `test_charges` BigDecimal, `medicine_charges` BigDecimal, `total_amount` BigDecimal, `payment_status` ENUM, `created_at`, `updated_at`)
10. `notifications`: User notification alerts (`id`, `notification_id` unique, `user_id` FK, `title`, `message`, `type` ENUM, `is_read`, `created_at`)

---

## 🚀 Execution & Verification Commands

### MySQL connection setup
The backend connects to the `hospital_management` database on `localhost:3306` as `root` by default. In the same PowerShell window where you start the backend, set the username and securely prompt for your local MySQL password:
```powershell
$env:SPRING_DATASOURCE_USERNAME = "root"
$securePassword = Read-Host "MySQL root password" -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $env:SPRING_DATASOURCE_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
}
Set-Location .\backend
.\mvnw.cmd spring-boot:run
```
If your MySQL `root` account has no password, set `$env:SPRING_DATASOURCE_PASSWORD = ""` instead of running the secure prompt; this overrides the configured fallback. These environment variables apply only to the current PowerShell window; set them again in a new window. Ensure MySQL is running and the `hospital_management` database exists.

### 1. Run Kotlin / Spring Boot Backend
```powershell
cd backend
./mvnw spring-boot:run
```

### 2. Run React / Vite Frontend
```powershell
cd frontend
npm run dev
```

### 3. Run Complete Integration Test Suite (111 Tests)
```powershell
cd backend
./mvnw clean test
```
*Output: `Tests run: 111, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)*

### 4. Production Build Verification
```powershell
cd frontend
npm run build
```
*Output: `vite v8.3.2 building client environment... built in 222ms`*

---

## 🏆 Project Status

**STAGES 1 THRU 9 FULLY COMPLETED & VERIFIED.**
All requirements met with zero errors.

package com.citycare.hospital.repository

import com.citycare.hospital.model.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByNotificationId(notificationId: String): Optional<Notification>
    fun existsByNotificationId(notificationId: String): Boolean
    fun findTopByOrderByIdDesc(): Optional<Notification>

    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>
    fun findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId: Long): List<Notification>
    fun countByUserIdAndIsReadFalse(userId: Long): Long
}

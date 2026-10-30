package com.citycare.hospital.dto

import com.citycare.hospital.model.NotificationType
import java.time.LocalDateTime

data class NotificationResponse(
    val id: Long,
    val notificationId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val isRead: Boolean,
    val createdAt: LocalDateTime
)

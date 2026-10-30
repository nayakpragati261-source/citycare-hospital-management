package com.citycare.hospital.service

import com.citycare.hospital.dto.NotificationResponse
import com.citycare.hospital.model.Notification
import com.citycare.hospital.model.NotificationType
import com.citycare.hospital.model.User
import com.citycare.hospital.repository.NotificationRepository
import com.citycare.hospital.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val userRepository: UserRepository
) {

    fun getNotificationsForUser(userEmail: String): List<NotificationResponse> {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.") }
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.id!!)
            .map { toNotificationResponse(it) }
    }

    fun getUnreadCountForUser(userEmail:String): Long {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.") }
        return notificationRepository.countByUserIdAndIsReadFalse(user.id!!)
    }

    @Transactional
    fun markAsRead(id: Long, userEmail: String): NotificationResponse {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.") }

        val notification = notificationRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found.") }

        if (notification.user?.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own notifications.")
        }

        notification.isRead = true
        val saved = notificationRepository.save(notification)
        return toNotificationResponse(saved)
    }

    @Transactional
    fun markAllAsRead(userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.") }

        val unreadList = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(user.id!!)
        unreadList.forEach { it.isRead = true }
        notificationRepository.saveAll(unreadList)
    }

    @Transactional
    fun deleteNotification(id: Long, userEmail: String) {
        val user = userRepository.findByEmail(userEmail)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "User not found.") }

        val notification = notificationRepository.findById(id)
            .orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found.") }

        if (notification.user?.id != user.id) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access your own notifications.")
        }

        notificationRepository.delete(notification)
    }

    @Transactional
    fun createNotification(user: User, title: String, message: String, type: NotificationType): NotificationResponse {
        val notifId = generateUniqueNotificationId()
        val notification = Notification(
            notificationId = notifId,
            user = user,
            title = title,
            message = message,
            type = type,
            isRead = false,
            createdAt = LocalDateTime.now()
        )
        val saved = notificationRepository.save(notification)
        return toNotificationResponse(saved)
    }

    @Synchronized
    private fun generateUniqueNotificationId(): String {
        val lastNotif = notificationRepository.findTopByOrderByIdDesc()
        var nextIdNum = 1L
        if (lastNotif.isPresent) {
            val lastIdStr = lastNotif.get().notificationId
            if (lastIdStr.startsWith("NOTIF-")) {
                val numPart = lastIdStr.removePrefix("NOTIF-").toLongOrNull()
                if (numPart != null) nextIdNum = numPart + 1
            } else {
                nextIdNum = (lastNotif.get().id ?: 0L) + 1
            }
        }

        var candidateId = String.format("NOTIF-%04d", nextIdNum)
        while (notificationRepository.existsByNotificationId(candidateId)) {
            nextIdNum++
            candidateId = String.format("NOTIF-%04d", nextIdNum)
        }
        return candidateId
    }

    fun toNotificationResponse(notification: Notification): NotificationResponse {
        return NotificationResponse(
            id = notification.id ?: 0L,
            notificationId = notification.notificationId,
            title = notification.title,
            message = notification.message,
            type = notification.type,
            isRead = notification.isRead,
            createdAt = notification.createdAt
        )
    }
}

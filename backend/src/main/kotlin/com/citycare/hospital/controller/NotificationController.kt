package com.citycare.hospital.controller

import com.citycare.hospital.dto.AuthUserResponse
import com.citycare.hospital.dto.ErrorResponse
import com.citycare.hospital.service.NotificationService
import jakarta.servlet.http.HttpSession
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/notifications")
class NotificationController(private val notificationService: NotificationService) {

    @GetMapping
    fun getNotifications(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val notifications = notificationService.getNotificationsForUser(authUser.email)
        return ResponseEntity.ok(notifications)
    }

    @GetMapping("/unread-count")
    fun getUnreadCount(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val unreadCount = notificationService.getUnreadCountForUser(authUser.email)
        return ResponseEntity.ok(mapOf("unreadCount" to unreadCount, "count" to unreadCount))
    }

    @PutMapping("/{id}/read")
    fun markAsRead(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        val updated = notificationService.markAsRead(id, authUser.email)
        return ResponseEntity.ok(updated)
    }

    @PutMapping("/read-all")
    fun markAllAsRead(session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        notificationService.markAllAsRead(authUser.email)
        return ResponseEntity.ok(mapOf("message" to "All notifications marked as read"))
    }

    @DeleteMapping("/{id}")
    fun deleteNotification(@PathVariable id: Long, session: HttpSession): ResponseEntity<*> {
        val authUser = getAuthenticatedUser(session)
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse("Unauthorized access"))

        notificationService.deleteNotification(id, authUser.email)
        return ResponseEntity.ok(mapOf("message" to "Notification deleted successfully"))
    }

    private fun getAuthenticatedUser(session: HttpSession): AuthUserResponse? {
        return session.getAttribute("user") as? AuthUserResponse
    }
}

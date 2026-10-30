package com.citycare.hospital.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "notification_id", nullable = false, unique = true)
    var notificationId: String = "",

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User? = null,

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false, length = 1000)
    var message: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: NotificationType = NotificationType.APPOINTMENT_SCHEDULED,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        if (notificationId.isBlank()) {
            notificationId = "NOTIF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).uppercase()
        }
    }
}

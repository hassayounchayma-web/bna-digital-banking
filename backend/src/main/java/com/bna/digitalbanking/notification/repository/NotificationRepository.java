package com.bna.digitalbanking.notification.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.bna.digitalbanking.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDescIdDesc(String email);

    Optional<Notification> findByIdAndRecipientEmail(Long id, String email);

    long countByRecipientEmailAndIsReadFalse(String email);

    @Modifying
    @Query("""
        update Notification notification
        set notification.isRead = true, notification.readAt = :readAt
        where notification.recipient.email = :email and notification.isRead = false
        """)
    int markAllUnreadAsRead(@Param("email") String email, @Param("readAt") Instant readAt);
}

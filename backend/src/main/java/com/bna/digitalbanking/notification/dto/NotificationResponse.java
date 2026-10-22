package com.bna.digitalbanking.notification.dto;

import java.time.Instant;
import com.bna.digitalbanking.notification.entity.Notification;
import com.bna.digitalbanking.notification.entity.NotificationType;

public record NotificationResponse(
    Long id,
    NotificationType type,
    String title,
    String message,
    boolean isRead,
    Instant createdAt,
    Instant readAt,
    Long applicationId,
    String applicationReference
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.isRead(),
            notification.getCreatedAt(),
            notification.getReadAt(),
            notification.getApplication().getId(),
            notification.getApplication().getReference());
    }
}

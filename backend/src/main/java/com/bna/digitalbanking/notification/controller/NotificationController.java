package com.bna.digitalbanking.notification.controller;

import java.util.List;
import com.bna.digitalbanking.notification.dto.MarkAllReadResponse;
import com.bna.digitalbanking.notification.dto.NotificationResponse;
import com.bna.digitalbanking.notification.dto.UnreadCountResponse;
import com.bna.digitalbanking.notification.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/notifications")
public class NotificationController {
    private final NotificationService service;

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping
    public List<NotificationResponse> list(Authentication authentication) {
        return service.listOwn(authentication.getName());
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse unreadCount(Authentication authentication) {
        return service.unreadCount(authentication.getName());
    }

    @PatchMapping("/{id}/read")
    public NotificationResponse markAsRead(@PathVariable Long id, Authentication authentication) {
        return service.markAsRead(authentication.getName(), id);
    }

    @PatchMapping("/read-all")
    public MarkAllReadResponse markAllAsRead(Authentication authentication) {
        return service.markAllAsRead(authentication.getName());
    }
}

package com.bna.digitalbanking.notification.entity;

import java.time.Instant;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.CreditApplication;
import com.bna.digitalbanking.identity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "client_notifications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_notification_application_event", columnNames = "application_event_id")
})
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private UserEntity recipient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private CreditApplication application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_event_id", nullable = false)
    private ApplicationEvent applicationEvent;

    protected Notification() {
    }

    public Notification(
            NotificationType type,
            String title,
            String message,
            UserEntity recipient,
            CreditApplication application,
            ApplicationEvent applicationEvent) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.recipient = recipient;
        this.application = application;
        this.applicationEvent = applicationEvent;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void markAsRead() {
        if (!isRead) {
            isRead = true;
            readAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public NotificationType getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public boolean isRead() { return isRead; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public UserEntity getRecipient() { return recipient; }
    public CreditApplication getApplication() { return application; }
}

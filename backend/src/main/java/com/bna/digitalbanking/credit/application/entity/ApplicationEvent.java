package com.bna.digitalbanking.credit.application.entity;

import java.time.Instant;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.identity.UserRole;
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

@Entity
@Table(name = "application_events")
public class ApplicationEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private CreditApplication application;

    @Column(name = "actor_email", nullable = false, length = 254)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 20)
    private UserRole actorRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private ApplicationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private ApplicationStatus toStatus;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected ApplicationEvent() {
    }

    public ApplicationEvent(
            CreditApplication application,
            String actorEmail,
            UserRole actorRole,
            ApplicationStatus fromStatus,
            ApplicationStatus toStatus,
            String note) {
        this.application = application;
        this.actorEmail = actorEmail;
        this.actorRole = actorRole;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.note = note;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public CreditApplication getApplication() { return application; }
    public String getActorEmail() { return actorEmail; }
    public UserRole getActorRole() { return actorRole; }
    public ApplicationStatus getFromStatus() { return fromStatus; }
    public ApplicationStatus getToStatus() { return toStatus; }
    public String getNote() { return note; }
    public Instant getCreatedAt() { return createdAt; }
}

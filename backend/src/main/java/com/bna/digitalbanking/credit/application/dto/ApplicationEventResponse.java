package com.bna.digitalbanking.credit.application.dto;

import java.time.Instant;
import com.bna.digitalbanking.credit.application.entity.ApplicationEvent;
import com.bna.digitalbanking.credit.application.entity.ApplicationStatus;
import com.bna.digitalbanking.identity.UserRole;

public record ApplicationEventResponse(
    String actorEmail,
    UserRole actorRole,
    ApplicationStatus fromStatus,
    ApplicationStatus toStatus,
    String note,
    Instant createdAt
) {
    public static ApplicationEventResponse from(ApplicationEvent event) {
        return new ApplicationEventResponse(
            event.getActorEmail(), event.getActorRole(), event.getFromStatus(),
            event.getToStatus(), event.getNote(), event.getCreatedAt());
    }
}

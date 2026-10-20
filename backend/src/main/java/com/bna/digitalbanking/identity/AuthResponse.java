package com.bna.digitalbanking.identity;

import java.time.Instant;

public record AuthResponse(
    String accessToken,
    String tokenType,
    Instant expiresAt,
    UserSummary user
) {
}

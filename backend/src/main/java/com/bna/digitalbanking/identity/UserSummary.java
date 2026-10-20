package com.bna.digitalbanking.identity;

public record UserSummary(Long id, String fullName, String email, UserRole role) {
    static UserSummary from(UserEntity user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }
}

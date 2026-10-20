package com.bna.digitalbanking.identity;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final Duration tokenLifetime;

    public IdentityService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtTokenService jwtTokenService,
            @Value("${app.security.jwt.expiration}") Duration tokenLifetime) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenService = jwtTokenService;
        this.tokenLifetime = tokenLifetime;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        validatePasswordLength(request.password());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException();
        }

        UserEntity user = new UserEntity(
            request.fullName().strip(),
            email,
            passwordEncoder.encode(request.password()),
            UserRole.CLIENT);
        try {
            user = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyExistsException();
        }
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        validatePasswordLength(request.password());
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(normalizeEmail(request.email()), request.password()));
        UserEntity user = userRepository.findByEmail(authentication.getName())
            .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
        return createAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser(String email) {
        return userRepository.findByEmail(email)
            .map(UserSummary::from)
            .orElseThrow(() -> new IllegalStateException("Authenticated user was not found"));
    }

    private AuthResponse createAuthResponse(UserEntity user) {
        Instant expiresAt = Instant.now().plus(tokenLifetime);
        String token = jwtTokenService.createToken(user, expiresAt);
        return new AuthResponse(token, "Bearer", expiresAt, UserSummary.from(user));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(java.util.Locale.ROOT);
    }

    private static void validatePasswordLength(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidPasswordException();
        }
    }
}

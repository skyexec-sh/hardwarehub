package com.hardwarehub.auth.service;

import com.hardwarehub.auth.domain.PasswordResetToken;
import com.hardwarehub.auth.domain.RefreshToken;
import com.hardwarehub.auth.dto.AuthResponse;
import com.hardwarehub.auth.dto.ChangePasswordRequest;
import com.hardwarehub.auth.dto.ForgotPasswordRequest;
import com.hardwarehub.auth.dto.LoginRequest;
import com.hardwarehub.auth.dto.RefreshTokenRequest;
import com.hardwarehub.auth.dto.ResetPasswordRequest;
import com.hardwarehub.auth.repository.PasswordResetTokenRepository;
import com.hardwarehub.auth.repository.RefreshTokenRepository;
import com.hardwarehub.common.audit.AuditService;
import com.hardwarehub.common.exception.BusinessException;
import com.hardwarehub.common.exception.UnauthorizedException;
import com.hardwarehub.common.security.JwtService;
import com.hardwarehub.common.security.UserPrincipal;
import com.hardwarehub.common.util.TokenHashUtil;
import com.hardwarehub.user.domain.User;
import com.hardwarehub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsernameAndDeletedAtIsNull(principal.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        AuthResponse response = issueTokens(user, principal);
        auditService.log("LOGIN", "USER", String.valueOf(user.getId()), "User logged in");
        return response;
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String hash = TokenHashUtil.sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository.findByTokenHashAndRevokedFalse(hash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            stored.setRevoked(true);
            throw new UnauthorizedException("Refresh token expired");
        }

        stored.setRevoked(true);
        User user = stored.getUser();
        if (!user.isActive() || user.isDeleted()) {
            throw new UnauthorizedException("Account is disabled");
        }

        UserPrincipal principal = new UserPrincipal(user);
        AuthResponse response = issueTokens(user, principal);
        auditService.log("REFRESH_TOKEN", "USER", String.valueOf(user.getId()), "Refresh token rotated");
        return response;
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        String hash = TokenHashUtil.sha256(request.refreshToken());
        refreshTokenRepository.findByTokenHashAndRevokedFalse(hash).ifPresent(token -> {
            token.setRevoked(true);
            auditService.log("LOGOUT", "USER", String.valueOf(token.getUser().getId()), "User logged out");
        });
        SecurityContextHolder.clearContext();
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailAndDeletedAtIsNull(request.email()).ifPresent(user -> {
            String rawToken = TokenHashUtil.generateRawToken();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setTokenHash(TokenHashUtil.sha256(rawToken));
            resetToken.setExpiresAt(Instant.now().plusSeconds(3600));
            passwordResetTokenRepository.save(resetToken);

            // Milestone 1: email delivery stubbed — token logged for local development only.
            log.info("Password reset token for {} (dev only): {}", user.getEmail(), rawToken);
            auditService.log("FORGOT_PASSWORD", "USER", String.valueOf(user.getId()), "Password reset requested");
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hash = TokenHashUtil.sha256(request.token());
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHashAndUsedFalse(hash)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid or expired reset token",
                        HttpStatus.BAD_REQUEST));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("INVALID_TOKEN", "Invalid or expired reset token", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log("RESET_PASSWORD", "USER", String.valueOf(user.getId()), "Password reset completed");
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findByUsernameAndDeletedAtIsNull(principal.getUsername())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("INVALID_PASSWORD", "Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditService.log("CHANGE_PASSWORD", "USER", String.valueOf(user.getId()), "Password changed");
    }

    private AuthResponse issueTokens(User user, UserPrincipal principal) {
        String accessToken = jwtService.generateAccessToken(principal);
        String rawRefresh = TokenHashUtil.generateRawToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setTokenHash(TokenHashUtil.sha256(rawRefresh));
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpirationMs()));
        refreshTokenRepository.save(refreshToken);

        List<String> roles = user.getRoles().stream()
                .map(role -> role.getName().name())
                .sorted()
                .toList();

        return new AuthResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtService.getAccessTokenExpirationMs() / 1000,
                new AuthResponse.UserSummary(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        roles
                )
        );
    }
}

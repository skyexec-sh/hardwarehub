package com.hardwarehub.common.security;

import com.hardwarehub.user.domain.Role;
import com.hardwarehub.user.domain.RoleName;
import com.hardwarehub.user.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("TestSecretKeyForHardwareHubJwtMustBeLongEnough123456");
        properties.getJwt().setAccessTokenExpirationMs(60_000);
        properties.getJwt().setRefreshTokenExpirationMs(3_600_000);
        jwtService = new JwtService(properties);
    }

    @Test
    void generatesAndParsesAccessToken() {
        UserPrincipal principal = stubPrincipal();
        String token = jwtService.generateAccessToken(principal);

        assertThat(jwtService.isValid(token)).isTrue();
        assertThat(jwtService.extractUsername(token)).isEqualTo("owner");
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(stubPrincipal());
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1] + ".invalidsignature";
        assertThat(jwtService.isValid(tampered)).isFalse();
        assertThat(jwtService.isValid("not-a-jwt")).isFalse();
    }

    private UserPrincipal stubPrincipal() {
        Role role = new Role();
        role.setName(RoleName.OWNER);

        User user = new User();
        user.setId(1L);
        user.setUsername("owner");
        user.setEmail("owner@test.local");
        user.setPasswordHash("hash");
        user.setFirstName("Store");
        user.setLastName("Owner");
        user.setActive(true);
        user.setRoles(Set.of(role));
        return new UserPrincipal(user);
    }
}

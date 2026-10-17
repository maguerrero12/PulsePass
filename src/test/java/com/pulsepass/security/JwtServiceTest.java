package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService service;

    private String secretKeyBase64 =
            "dGhpc0lzQVNlY3JldEtleUZvclB1bHNlUGFzc1Rlc3RpbmdPbmx5MTIzNDU2Nzg5MA==";
    private String issuer = "pulsepass-api";
    private Duration ttl = Duration.ofMinutes(15);

    @Mock
    private SecurityPrincipal securityPrincipal;

    @BeforeEach
    void setUp() {
        service = new JwtService(secretKeyBase64, issuer, ttl);
    }

    // Firma y claims correctos
    @Test
    void shouldGenerateTokenWithCorrectSignatureAndClaims() {

        Long userId = 1L;
        when(securityPrincipal.getId()).thenReturn(userId);

        String token = service.generateToken(securityPrincipal);
        Claims claims = service.validateAndGetClaims(token);

        assertThat(token).isNotBlank();
        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.getIssuer()).isEqualTo(issuer);
        assertThat(claims.getId()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());

        verify(securityPrincipal).getId();
    }

    // Expiración
    @Test
    void shouldThrowExceptionWhenTokenExpired() {

        Duration expiredTtl = Duration.ofSeconds(-10);
        JwtService expiredJwtService = new JwtService(secretKeyBase64, issuer, expiredTtl);

        Long userId = 1L;
        when(securityPrincipal.getId()).thenReturn(userId);

        String expiredToken = expiredJwtService.generateToken(securityPrincipal);

        assertThatThrownBy(() -> service.validateAndGetClaims(expiredToken))
                .isInstanceOf(RuntimeException.class);

        verify(securityPrincipal).getId();
    }

    // Issuer incorrecto
    @Test
    void shouldThrowExceptionWhenIncorrectIssuer() {

        JwtService wrongIssuerService = new JwtService(
                secretKeyBase64, "wrong-issuer", ttl);

        Long userId = 1L;
        when(securityPrincipal.getId()).thenReturn(userId);

        String tokenWithWrongIssuer = wrongIssuerService.generateToken(securityPrincipal);

        assertThatThrownBy(() -> service.validateAndGetClaims(tokenWithWrongIssuer))
                .isInstanceOf(RuntimeException.class);

        verify(securityPrincipal).getId();
    }

    // Manipulacion
    @Test
    void shouldThrowExceptionWhenManipulation() {

        Long userId = 1L;
        when(securityPrincipal.getId()).thenReturn(userId);

        String validToken = service.generateToken(securityPrincipal);
        String tamperedToken = validToken.substring(0, validToken.length() - 5) + "XXXXX";

        assertThatThrownBy(() -> service.validateAndGetClaims(tamperedToken))
                .isInstanceOf(RuntimeException.class);

        verify(securityPrincipal).getId();
    }

    // Token vacio
    @Test
    void shouldThrowExceptionWhenEmptyToken() {

        assertThatThrownBy(() -> service.validateAndGetClaims(""))
                .isInstanceOf(RuntimeException.class);

        assertThatThrownBy(() -> service.validateAndGetClaims(null))
                .isInstanceOf(RuntimeException.class);
    }

    // Clave debil
    @Test
    void shouldThrowExceptionWhenWeakSecret() {

        String weakSecretBase64 = "Y2xhdmVkZWJpbA=="; // menos de 256 bits

        assertThatThrownBy(() -> new JwtService(weakSecretBase64, issuer, ttl))
                .isInstanceOf(WeakKeyException.class);
    }
}
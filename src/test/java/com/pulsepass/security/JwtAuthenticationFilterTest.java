package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private Claims claims;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Sin Bearer
    @Test
    void shouldContinueChainWithoutAuthenticationWhenHeaderHasNoBearer() throws Exception {
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateAndGetClaims(anyString());
    }

    // Bearer valido
    @Test
    void shouldAuthenticateUserWhenValidBearer() throws Exception {
        String token = "valid.jwt.token";
        String userId = "100";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtService.validateAndGetClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId);
        when(userDetailsService.loadUserByUsername(userId)).thenReturn(userDetails);
        when(userDetails.getAuthorities()).thenReturn(Collections.emptyList());

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal()).isEqualTo(userDetails);

        verify(jwtService).validateAndGetClaims(token);
        verify(userDetailsService).loadUserByUsername(userId);
        verify(filterChain).doFilter(request, response);

    }

    // Bear invalido
    @Test
    void shouldClearContextAndContinueChainWhenTokenIsInvalid() throws Exception {
        String token = "invalid.jwt.token";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtService.validateAndGetClaims(token))
                .thenThrow(new RuntimeException("Invalid token"));

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(jwtService).validateAndGetClaims(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

    }

    // Dobele header sospechoso
    @Test
    void shouldIgnoreMalformedOrDoubleHeaderIfNotStartingWithBearer() throws Exception {
        String malformedHeader = "Basic dXNlcjpwYXNz";

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn(malformedHeader);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateAndGetClaims(anyString());
    }

    // No sobrescribir autenticación establecida de forma insegura
    @Test
    void shouldNotOverwriteExistingAuthenticationInSecurityContext() throws Exception {
        String token = "valid.jwt.token";
        String userId = "100";

        UsernamePasswordAuthenticationToken existingAuth =
                new UsernamePasswordAuthenticationToken(
                        "existingUser", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Bearer " + token);
        when(jwtService.validateAndGetClaims(token)).thenReturn(claims);
        when(claims.getSubject()).thenReturn(userId);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isEqualTo(existingAuth);

        verify(jwtService).validateAndGetClaims(token);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        verify(filterChain).doFilter(request, response);

    }
}
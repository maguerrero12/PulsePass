package com.pulsepass.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.domain.TicketType;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AuthorizationService authorizationService;

    // --- T-SEC-001: Anónimo GET /api/events/published
    @Test
    void testTSec001_AnonymousGetPublishedEvents() throws Exception {

        given(eventService.findPublishedEvents()).willReturn(List.of());

        mockMvc.perform(get("/api/events/published")).andExpect(status().isOk());

        verify(eventService).findPublishedEvents();
    }

    // --- T-SEC-002: Anónimo POST /api/events
    @Test
    void testTSec002_AnonymousPostEvent() throws Exception {

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Concierto Fest\",\"capacity\":5000}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(eventService, never()).create(any());
    }

    // --- T-SEC-003: USER POST /api/events
    @Test
    @WithMockUser(username = "user1@pulsepass.com", roles = {"USER"})
    void testTSec003_UserPostEventForbidden() throws Exception {

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Concierto Fest\",\"capacity\":5000}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(eventService, never()).create(any());
    }

    // --- T-SEC-004: ADMIN POST /api/events válido
    @Test
    void testTSec004_AdminPostEventSuccess() throws Exception {
        EventResponse responseDto = new EventResponse(
                "EVT-001", "Concierto Fest", "Descripción", LocalDateTime.now().plusDays(10),
                "PUBLISHED", 5000, 0
        );
        given(eventService.create(any())).willReturn(responseDto);

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Concierto Fest\",\"capacity\":5000}"))
                .andExpect(status().isCreated());
    }

    // --- T-SEC-005: Anónimo POST /api/auth/login correcto
    @Test
    void testTSec005_LoginSuccess() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@pulsepass.com", "Password123!");
        AuthResponse authResponse = new AuthResponse("mocked.jwt.token", "Bearer", 900L);

        given(authService.login(any())).willReturn(authResponse);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mocked.jwt.token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    // --- T-SEC-006: Login con contraseña incorrecta
    @Test
    void testTSec006_LoginBadCredentials() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@pulsepass.com", "WrongPassword");
        given(authService.login(any())).willThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    // --- T-SEC-007: Token expirado en ruta protegida -> 401 con ErrorResponse ---
    @Test
    void testTSec007_ExpiredToken() throws Exception {
        // Se envía un encabezado Bearer inválido/expirado simulado
        mockMvc.perform(get("/api/tickets/TCK-12345678")
                        .header("Authorization", "Bearer token_expirado_ejemplo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").exists());
    }

    // --- T-SEC-008: Token con firma alterada
    @Test
    void testTSec008_TamperedToken() throws Exception {
        mockMvc.perform(get("/api/tickets/TCK-12345678")
                        .header("Authorization", "Bearer token_firma_falsa"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        verify(ticketService, never()).findByCode(any());
    }

    // --- T-SEC-009: USER consulta ticket ajeno
    @Test
    @WithMockUser(username = "user1@pulsepass.com", roles = {"USER"})
    void testTSec009_UserAccessOtherUserTicketForbidden() throws Exception {
        given(ticketService.findByCode("TCK-99999999"))
                .willThrow(new AccessDeniedException("You do not have permission to access or modify this ticket"));

        mockMvc.perform(get("/api/tickets/TCK-99999999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    // --- T-SEC-010: USER compra con email ajeno
    @Test
    @WithMockUser(username = "user1@pulsepass.com", roles = {"USER"})
    void testTSec010_UserPurchaseWithOtherEmailForbidden() throws Exception {
        PurchaseTicketRequest request = new PurchaseTicketRequest("otro_usuario@pulsepass.com","EVT-001", TicketType.GENERAL,);

        given(ticketService.purchase(any()))
                .willThrow(new AccessDeniedException("You do not have permission to access or modify this user's data"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}

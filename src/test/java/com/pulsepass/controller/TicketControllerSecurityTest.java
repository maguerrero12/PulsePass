package com.pulsepass.controller;

import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
public class TicketControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean private TicketService ticketService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private com.pulsepass.security.CustomUserDetailsService customUserDetailsService;
    @MockitoBean private AuthorizationService authorizationService;

    @BeforeEach
    public void setUp() {
        // Conecta @WithMockUser con el filtro de Spring Security en Spring Boot 4
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    // T-SEC-009 (Anti-IDOR): Prueba que si Andrea intenta ver el ticket de Carlos, recibe un 403 Forbidden.
    @WithMockUser(roles = "USER", username = "andrea@mail.com")
    public void user_GetAnotherUserTicket_ReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("Denied")).when(authorizationService).verifyTicketOwnership(anyString());

        mockMvc.perform(get("/api/tickets/TKT-CARLOS-123"))
                .andExpect(status().isForbidden());
    }

    @Test
    // T-SEC-010 (Anti-IDOR): Prueba que si Andrea intenta comprar enviando el email de Carlos, recibe un 403.
    @WithMockUser(roles = "USER", username = "andrea@mail.com")
    public void user_BuyTicketForAnotherEmail_ReturnsForbidden() throws Exception {
        doThrow(new AccessDeniedException("Denied")).when(authorizationService).verifyUserAccessByEmail(anyString());

        String json = """
            {
              "userEmail": "carlos@mail.com",
              "eventCode": "CMF-2026",
              "type": "GENERAL"
            }
            """;
        mockMvc.perform(post("/api/tickets").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    // T-SEC-011: Prueba que un usuario SÍ pueda cancelar su propio ticket.
    @WithMockUser(roles = "USER")
    public void user_UseTicket_ReturnsForbidden() throws Exception {
        mockMvc.perform(patch("/api/tickets/TKT-123/use"))
                .andExpect(status().isForbidden());
    }

    // T-SEC-012: Prueba que un usuario normal NO pueda marcar un ticket como usado en la puerta.
    @Test
    @WithMockUser(roles = "USER", username = "andrea@mail.com")
    public void user_CancelOwnTicket_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/tickets/TKT-ANDREA-123/cancel"))
                .andExpect(status().isOk());
    }
    // T-SEC-013: Prueba que el personal (rol STAFF) SÍ pueda marcar un ticket como usado.
    @Test
    @WithMockUser(roles = "STAFF")
    public void staff_UseTicket_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/tickets/TKT-123/use"))
                .andExpect(status().isOk());
    }
}
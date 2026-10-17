package com.pulsepass.controller;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class, GlobalExceptionHandler.class})
public class EventControllerSecurityTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @MockitoBean private EventService eventService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    public void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // T-SEC-001: Anónimo GET /api/events/published -> 200
    @Test
    public void anonymous_GetPublishedEvents_ReturnsOk() throws Exception {
        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk());
    }

    // T-SEC-002: Anónimo POST /api/events -> 401
    @Test
    public void tSec002_anonymous_PostEvent_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // T-SEC-003: USER POST /api/events -> 403, sin invocar Service
    @Test
    @WithMockUser(roles = "USER")
    public void tSec003_user_PostEvent_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        verify(eventService, never()).create(any());
    }

    // T-SEC-004: ADMIN POST /api/events válido -> 201
    // T-SEC-004: ADMIN crea un evento válido -> 201 Created
    @Test
    @WithMockUser(roles = "ADMIN")
    public void tSec004_admin_PostEvent_ReturnsCreated() throws Exception {
        String validJson = """
            {
              "eventCode": "CMF-2026",
              "name": "Concierto CMF",
              "description": "Gran concierto de prueba",
              "category": "MUSIC",
              "eventDate": "2026-12-01T20:00:00",
              "minimumAge": 18,
              "venueCode": "VEN-SMR-01"
            }
            """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isCreated());
    }

    // T-SEC-014: ADMIN publica evento -> 200
    @Test
    @WithMockUser(roles = "ADMIN")
    public void tSec014_admin_PublishEvent_ReturnsOk() throws Exception {
        mockMvc.perform(patch("/api/events/CMF-2026/publish"))
                .andExpect(status().isOk());
    }

    // T-SEC-017: Anónimo consulta evento no publicado -> No expone datos del borrador (401)
    @Test
    public void tSec017_anonymous_GetDraftEvent_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/events/CMF-DRAFT"))
                .andExpect(status().isUnauthorized());
    }

    // T-SEC-018: Request inválido pero autorizado -> 400 previo de Bean Validation
    @Test
    @WithMockUser(roles = "ADMIN")
    public void tSec018_admin_PostInvalidEvent_ReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}
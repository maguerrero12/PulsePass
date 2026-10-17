package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.EventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService service;
    @MockitoBean
    private AuthorizationService authorizationService;
    @MockitoBean
    private com.pulsepass.security.JwtService jwtService;
    @MockitoBean
    private com.pulsepass.security.CustomUserDetailsService customUserDetailsService;

    // TEST-CTRL-EVT-001 crear válido → 201
    @Test
    void shouldCreateValidEvent() throws Exception{

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival de musica del Caribe",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of()
        );

        when(service.create(any(CreateEventRequest.class))).thenReturn(response);

        String request = """
                {
                    "eventCode": "CMF-2026",
                    "name": "Caribbean Music Fest 2026",
                    "description": "Festival de música del Caribe",
                    "category": "MUSIC",
                    "eventDate": "2026-12-15T18:00:00",
                    "minimumAge": 18,
                    "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(service).create(any(CreateEventRequest.class));
    }

    // TEST-CTRL-EVT-002 request inválido → 400
    @Test
    void shouldReturn400WhenRequestIsInvalid() throws Exception{

        String request = """
                {
                    "eventCode": "",
                    "name": "",
                    "description": "",
                    "category": "",
                    "eventDate": "",
                    "minimumAge": null,
                    "venueCode": ""
                }
                """;

        mockMvc.perform(post("/api/events")
                    .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest());

        verify(service, never()).create(any());
    }

    // TEST-CTRL-EVT-003 consultar existente → 200
    @Test
    void shouldReturnEventByCode() throws Exception{

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center", Set.of()
        );

        when(service.findByCode("CMF-2026")).thenReturn(response);

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(service).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-004 inexistente → 404
    @Test
    void shoulReturn404WhenEventNotFound() throws Exception{

        when(service.findByCode("CMF-2026"))
                .thenThrow(new ResourceNotFoundException("Event not found: CMF-2026"));

        mockMvc.perform(get("/api/events/{eventCode}", "CMF-2026"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Event not found: CMF-2026"));

        verify(service).findByCode("CMF-2026");
    }

    // TEST-CTRL-EVT-005 listar publicados → 200
    @Test
    void shouldReturnPublished() throws Exception{

        EventSummaryResponse response = new EventSummaryResponse(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 18, 0)
        );

        when(service.findPublishedEvents()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/events/published"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(service).findPublishedEvents();
    }

    // TEST-CTRL-EVT-006 publicar válido → 200
    @Test
    void shouldPublishEventCorrectly() throws Exception{

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center", Set.of()
        );

        when(service.publish("CMF-2026")).thenReturn(response);

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(service).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-007 publicar inválido → 409
    @Test
    void shouldReturn409WhenPublishInvalidEvent() throws Exception{

        when(service.publish("CMF-2026"))
                .thenThrow(new BusinessRuleException(
                        "Event cannot be published from its current status"));

        mockMvc.perform(patch("/api/events/{eventCode}/publish", "CMF-2026"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Event cannot be published from its current status"));

        verify(service).publish("CMF-2026");
    }

    // TEST-CTRL-EVT-008 asociar artista → 200
    @Test
    void shouldAssociateArtistCorrectly() throws Exception{

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of(new ArtistResponse(
                        1L,
                        "Solar Beat",
                        null,
                        null))
        );

        when(service.addArtist("CMF-2026",1L )).thenReturn(response);

        mockMvc.perform(post(
                "/api/events/{eventCode}/artists/{artistId}",
                        "CMF-2026", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists[0].id").value(1L));

        verify(service).addArtist("CMF-2026",1L);
    }

    // TEST-CTRL-EVT-009 buscar por artista → 200
    @Test
    void shouldReturnEventsByArtist() throws Exception{

        EventSummaryResponse response = new EventSummaryResponse(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 18, 0)
        );

        when(service.findByArtist("Solar Beat")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/events/by-artist")
                        .param("stageName", "Solar Beat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(service).findByArtist("Solar Beat");
    }

}
package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthorizationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService service;

    @MockitoBean
    private AuthorizationService authorizationService;
    @MockitoBean
    private com.pulsepass.security.JwtService jwtService;
    @MockitoBean
    private com.pulsepass.security.CustomUserDetailsService customUserDetailsService;

    //TEST-CTRL-ART-001 buscar por ID → 200
    @Test
    void shouldReturnArtistById() throws Exception{

        ArtistResponse response = new ArtistResponse(
                1L,
                "Solar Beat",
                "Electronic",
                "Colombia"
        );

        when(service.findById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/artists/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L));

        verify(service).findById(1L);
    }

    //TEST-CTRL-ART-002 ID inexistente → 404
    @Test
    void shouldReturn404WhenIdDoesNotExists() throws Exception{

        when(service.findById(99L))
                .thenThrow(new ResourceNotFoundException("Artist with id 99 not found"));

        mockMvc.perform(get("/api/artists/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Artist with id 99 not found"));

        verify(service).findById(99L);
    }

    //TEST-CTRL-ART-003 buscar por stageName → 200
    @Test
    void shouldReturnArtistByStageName() throws Exception{
        ArtistResponse response = new ArtistResponse(
                2L,
                "Neon Waves",
                "Pop",
                "Colombia"
        );

        when(service.findByStageName("Neon Waves")).thenReturn(response);

        mockMvc.perform(get("/api/artists/by-stage-name")
                    .param("stageName", "Neon Waves"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stageName").value("Neon Waves"));

        verify(service).findByStageName("Neon Waves");
    }

    //TEST-CTRL-ART-004 listar activos → 200
    @Test
    void shouldFindActiveArtists() throws Exception{

        List<ArtistResponse> responses = List.of(
                new ArtistResponse(
                        1L,
                        "Solar Beat",
                        "Electronic",
                        "Colombia"
                ),
                new ArtistResponse(
                        2L,
                        "Neon Waves",
                        "Pop",
                        "Colombia"
                )
        );

        when(service.findActiveArtists()).thenReturn(responses);

        // ACT + ASSERT
        mockMvc.perform(get("/api/artists/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stageName").
                        value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName")
                        .value("Neon Waves"));

        verify(service).findActiveArtists();
    }
}
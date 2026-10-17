package com.pulsepass.controller;

import com.pulsepass.dto.response.VenueResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.VenueService;
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

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService service;
    @MockitoBean
    private AuthorizationService authorizationService;
    @MockitoBean
    private com.pulsepass.security.JwtService jwtService;
    @MockitoBean
    private com.pulsepass.security.CustomUserDetailsService customUserDetailsService;

    //TEST-CTRL-VEN-001 venue existente → 200
    @Test
    void shouldReturnVenueByCode() throws Exception{

        VenueResponse response = new VenueResponse(
                1L,
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                3
        );

        when(service.findByCode("VEN-SMR-01")).thenReturn(response);

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("VEN-SMR-01"));

        verify(service).findByCode("VEN-SMR-01");
    }

    //TEST-CTRL-VEN-002 venue inexistente → 404
    @Test
    void shouldReturn404WhenVenueNotFound() throws Exception{

        when(service.findByCode("VEN-SMR-01"))
                .thenThrow(new ResourceNotFoundException("Venue not found: VEN-SMR-01"));

        mockMvc.perform(get("/api/venues/{code}", "VEN-SMR-01"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("Venue not found: VEN-SMR-01"));

        verify(service).findByCode("VEN-SMR-01");
    }

    //TEST-CTRL-VEN-003 venues activos → 200
    @Test
    void shouldReturnActiveVenues() throws Exception{

        VenueResponse response = new VenueResponse(
                1L,
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                3
        );

        when(service.findActiveVenues()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/venues/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("VEN-SMR-01"));

        verify(service).findActiveVenues();
    }
}
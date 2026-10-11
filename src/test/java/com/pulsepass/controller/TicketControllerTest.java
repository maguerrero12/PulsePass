package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService service;

    //TEST-CTRL-TKT-001 compra válida → 201
    @Test
    void shouldPurchaseTicketCorrectly() throws Exception{

        String request = """
                {
                    "userEmail": "andrea@example.com",
                    "eventCode": "CMF-2026",
                    "type": "GENERAL"
                }
                """;

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketCode").value("TCK-12345678"));

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    //TEST-CTRL-TKT-002 request inválido → 400
    @Test
    void shouldReturn404WhenRequestIsInvalid() throws Exception{

        String request = """
                {
                    "userEmail": "correo-invalido",
                    "eventCode": "",
                    "type": null
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message")
                        .value("Validation failed"));

        verify(service, never()).purchase(any(PurchaseTicketRequest.class));
    }

    //TEST-CTRL-TKT-003 usuario inexistente → 404
    @Test
    void shouldReturn404WhenUserDoesNotExists() throws Exception{

        String request = """
                {
                    "userEmail": "inexistente@example.com",
                    "eventCode": "CMF-2026",
                    "type": "GENERAL"
                }
                """;

        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new ResourceNotFoundException(
                        "User not found: inexistente@example.com"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message")
                        .value("User not found: inexistente@example.com"));

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    //TEST-CTRL-TKT-004 regla de negocio → 409
    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {

        String request = """
                {
                    "userEmail": "andrea@example.com",
                    "eventCode": "CMF-2026",
                    "type": "GENERAL"
                }
                """;

        when(service.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(new BusinessRuleException("Event is not published"));

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Event is not published"));

        verify(service).purchase(any(PurchaseTicketRequest.class));
    }

    //TEST-CTRL-TKT-005 consultar ticket → 200
    @Test
    void shouldFindTicketByCode() throws Exception {

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(service.findByCode("TCK-12345678")).thenReturn(response);

        mockMvc.perform(get("/api/tickets/{ticketCode}", "TCK-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-12345678"));

        verify(service).findByCode("TCK-12345678");
    }

    //TEST-CTRL-TKT-006 tickets por usuario → 200
    @Test
    void shouldFindTicketsByUserEmail() throws Exception {

        List<TicketResponse> responses = List.of(

                new TicketResponse(
                        1L,
                        "TCK-12345678",
                        TicketType.GENERAL,
                        new BigDecimal("150000.00"),
                        TicketStatus.PAID,
                        LocalDateTime.of(2026, 12, 1, 10, 0),
                        "andrea@example.com",
                        "CMF-2026",
                        "Caribbean Music Fest 2026"

                ),

                new TicketResponse(
                        2L,
                        "TCK-87654321",
                        TicketType.GENERAL,
                        new BigDecimal("150000.00"),
                        TicketStatus.PAID,
                        LocalDateTime.of(2026, 12, 1, 10, 30),
                        "andrea@example.com",
                        "CMF-2026",
                        "Caribbean Music Fest 2026"
                )
        );

        when(service.findByUserEmail("andrea@example.com")).thenReturn(responses);

        mockMvc.perform(get("/api/tickets/by-user")
                        .param("email", "andrea@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-12345678"))
                .andExpect(jsonPath("$[1].ticketCode").value("TCK-87654321"));

        verify(service).findByUserEmail("andrea@example.com");
    }

    //TEST-CTRL-TKT-007 tickets PAID por evento → 200
    @Test
    void shouldFindPaidTicketsByEvent() throws Exception {

        List<TicketResponse> response = List.of(
                new TicketResponse(
                        1L,
                        "TCK-12345678",
                        TicketType.GENERAL,
                        new BigDecimal("150000.00"),
                        TicketStatus.PAID,
                        LocalDateTime.of(2026, 12, 1, 10, 0),
                        "andrea@example.com",
                        "CMF-2026",
                        "Caribbean Music Fest 2026"

                )
        );

        when(service.findPaidTicketsByEvent("CMF-2026")).thenReturn(response);

        mockMvc.perform(get("/api/events/{eventCode}/tickets/paid",
                        "CMF-2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCode").value("TCK-12345678"))
                .andExpect(jsonPath("$[0].status").value("PAID"));

        verify(service).findPaidTicketsByEvent("CMF-2026");
    }

    //TEST-CTRL-TKT-008 cancelar válido → 200
    @Test
    void shouldCancelTicketCorrectly() throws Exception {

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.CANCELLED,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"

        );

        when(service.cancel("TCK-12345678")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel",
                        "TCK-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-12345678"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(service).cancel("TCK-12345678");
    }

    //TEST-CTRL-TKT-009 cancelar inválido → 409
    @Test
    void shouldReturn409WhenTicketCannotBeCancelled() throws Exception {

        when(service.cancel("TCK-12345678"))
                .thenThrow(new BusinessRuleException("Only PAID tickets can be cancelled"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/cancel",
                        "TCK-12345678"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only PAID tickets can be cancelled"));

        verify(service).cancel("TCK-12345678");
    }
    //TEST-CTRL-TKT-010 usar válido → 200
    @Test
    void shouldMarkTicketAsUsedCorrectly() throws Exception {

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.USED,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"

        );

        when(service.markAsUsed("TCK-12345678")).thenReturn(response);

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use",
                        "TCK-12345678"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticketCode").value("TCK-12345678"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(service).markAsUsed("TCK-12345678");
    }

    //TEST-CTRL-TKT-011 usar inválido → 409
    @Test
    void shouldReturn409WhenTicketCannotBeUsed() throws Exception {

        when(service.markAsUsed("TCK-12345678"))
                .thenThrow(new BusinessRuleException(
                        "Only PAID tickets can be marked as used"));

        mockMvc.perform(patch("/api/tickets/{ticketCode}/use",
                        "TCK-12345678"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message")
                        .value("Only PAID tickets can be marked as used"));

        verify(service).markAsUsed("TCK-12345678");
    }
}
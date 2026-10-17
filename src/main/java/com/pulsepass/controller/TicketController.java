package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class TicketController {

    private final TicketService ticketService;
    private final AuthorizationService authorizationService;

    public TicketController(TicketService ticketService, AuthorizationService authorizationService) {
        this.ticketService = ticketService;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/api/tickets")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody PurchaseTicketRequest request) {
        authorizationService.verifyUserAccessByEmail(request.userEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.purchase(request));
    }

    @GetMapping("/api/tickets/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        authorizationService.verifyTicketOwnership(ticketCode);
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    @GetMapping("/api/tickets/by-user")
    public ResponseEntity<List<TicketResponse>> findByUser(@RequestParam String email) {
        authorizationService.verifyUserAccessByEmail(email);
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }

    @GetMapping("/api/events/{eventCode}/tickets/paid")
    public ResponseEntity<List<TicketResponse>> findPaidTicketsByEvent(@PathVariable String eventCode) {
        return ResponseEntity.ok(ticketService.findPaidTicketsByEvent(eventCode));
    }

    @PatchMapping("/api/tickets/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        authorizationService.verifyTicketOwnership(ticketCode);
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    @PatchMapping("/api/tickets/{ticketCode}/use")
    public ResponseEntity<TicketResponse> useTicket(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
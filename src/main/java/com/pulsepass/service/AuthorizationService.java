package com.pulsepass.service;

import com.pulsepass.domain.Ticket;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    public AuthorizationService(TicketRepository ticketRepository,
                                UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    public SecurityPrincipal getCurrentPrincipal() {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !(auth.getPrincipal() instanceof SecurityPrincipal)) {
            throw new AccessDeniedException("No authenticated principal found");
        }

        return (SecurityPrincipal) auth.getPrincipal();
    }

    public void verifyUserAccessByEmail(String email) {

        SecurityPrincipal principal = getCurrentPrincipal();

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !principal.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException(
                    "You do not have permission to access or modify this user's data");
        }
    }

    public void verifyUserAccessByUsername(String username) {

        SecurityPrincipal principal = getCurrentPrincipal();

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !principal.getUser().getUsername().equalsIgnoreCase(username)) {
            throw new AccessDeniedException(
                    "You do not have permission to access or modify this user's data");
        }
    }

    /**
     * Valida que el ticket pertenezca al usuario autenticado o sea STAFF/ADMIN (FR-SEC-TKT-02).
     */
    public void verifyTicketOwnership(String ticketCode) {

        SecurityPrincipal principal = getCurrentPrincipal();

        boolean isStaffOrAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority()
                        .equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));

        if (isStaffOrAdmin) {
            return;
        }

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new AccessDeniedException("Ticket not found"));

        if (!ticket.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to access or modify this ticket");
        }
    }

    /**
     * Valida que el ticket pertenezca al usuario autenticado (USER) o sea ADMIN (FR-SEC-TKT-05).
     */
    public void verifyTicketCancelOwnership(String ticketCode) {

        SecurityPrincipal principal = getCurrentPrincipal();

        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            return;
        }

        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new AccessDeniedException("Ticket not found"));

        if (!ticket.getUser().getId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to cancel this ticket");
        }
    }
}

package com.pulsepass.service;

import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.SecurityPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // Dueño
    @Test
    void shouldAllowTicketOwnershipWhenUserIsOwner() {
        Long userId = 100L;
        String ticketCode = "TCK-12345678";

        User owner = new User();
        owner.setId(userId);
        owner.setEmail("owner@pulsepass.com");

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"));
        SecurityPrincipal principal = new SecurityPrincipal(
                owner, "hash", authorities);

        Ticket ticket = new Ticket();
        ticket.setTicketCode(ticketCode);
        ticket.setUser(owner);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(ticket));

        assertThatCode(() -> authorizationService.verifyTicketOwnership(ticketCode))
                .doesNotThrowAnyException();

        verify(ticketRepository).findByTicketCode(ticketCode);
    }

    // Tercero
    @Test
    void shouldDenyTicketOwnershipWhenUserIsThirdParty() {
        Long ownerId = 100L;
        Long thirdPartyId = 200L;
        String ticketCode = "TCK-12345678";

        User owner = new User();
        owner.setId(ownerId);

        User thirdParty = new User();
        thirdParty.setId(thirdPartyId);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"));
        SecurityPrincipal principal = new SecurityPrincipal(
                thirdParty, "hash", authorities);

        Ticket ticket = new Ticket();
        ticket.setTicketCode(ticketCode);
        ticket.setUser(owner);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(ticketRepository.findByTicketCode(ticketCode)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> authorizationService.verifyTicketOwnership(ticketCode))
                .isInstanceOf(AccessDeniedException.class);

        verify(ticketRepository).findByTicketCode(ticketCode);
    }

    // STAFF en uso
    @Test
    void shouldAllowTicketOwnershipWhenUserIsStaff() {
        String ticketCode = "TCK-99999999";
        User staffUser = new User();
        staffUser.setId(50L);

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_STAFF"));
        SecurityPrincipal principal = new SecurityPrincipal(
                staffUser, "hash", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        assertThatCode(() -> authorizationService.verifyTicketOwnership(ticketCode))
                .doesNotThrowAnyException();

        verify(ticketRepository, never()).findByTicketCode(anyString());
    }

    // ADMIN en publicacion
    @Test
    void shouldAllowUserAccessByEmailWhenUserIsAdmin() {
        String otherUserEmail = "other@pulsepass.com";
        User adminUser = new User();
        adminUser.setEmail("admin@pulsepass.com");

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityPrincipal principal = new SecurityPrincipal(
                adminUser, "hash", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        assertThatCode(() -> authorizationService.verifyUserAccessByEmail(otherUserEmail))
                .doesNotThrowAnyException();
    }

    // Correos ajenos
    @Test
    void shouldDenyUserAccessByEmailWhenAccessingOtherUserEmail() {
        String otherUserEmail = "other@pulsepass.com";
        User regularUser = new User();
        regularUser.setEmail("user@pulsepass.com");

        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER"));
        SecurityPrincipal principal = new SecurityPrincipal(
                regularUser, "hash", authorities);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);

        assertThatThrownBy(() -> authorizationService.verifyUserAccessByEmail(otherUserEmail))
                .isInstanceOf(AccessDeniedException.class);
    }
}
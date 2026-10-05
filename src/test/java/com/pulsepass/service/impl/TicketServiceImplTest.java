package com.pulsepass.service.impl;

import com.pulsepass.domain.*;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private TicketMapper ticketMapper;

    @InjectMocks
    private TicketServiceImpl service;

    @Test
    // TEST-TICKET-001
    void shouldPurchaseTicketCorrectly() {

        User user = mock(User.class);
        UserProfile profile = mock(UserProfile.class);
        Event event = mock(Event.class);
        Venue venue = mock(Venue.class);
        Ticket ticket = mock(Ticket.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 12, 1, 10, 0),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getEventDate()).thenReturn(LocalDateTime.now().plusDays(30));
        when(event.getMinimumAge()).thenReturn(18);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getBirthDate()).thenReturn(LocalDate.of(2000, 5, 10));
        when(event.getEventCode()).thenReturn("CMF-2026");
        when(event.getVenue()).thenReturn(venue);
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository.countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID))
                .thenReturn(0L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.purchase(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, times(2)).getEventDate();
        verify(event, times(2)).getMinimumAge();
        verify(user).getProfile();
        verify(profile).getBirthDate();
        verify(ticketRepository).countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID);
        verify(ticketRepository).save(any(Ticket.class));
        verify(ticketMapper).toResponse(ticket);
    }

    @Test
    // TEST-TICKET-002
    void shouldThrowExceptionWhenUserDoesNotExists(){

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(()-> service.purchase(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-003
    void shouldThrowExceptionWhenUserIsNotActive(){

        User user = mock(User.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(false);

        assertThatThrownBy(()-> service.purchase(request))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-004
    void shouldThrowExceptionWhenEventIsDraft(){

        User user = mock(User.class);
        Event event = mock(Event.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.DRAFT);

        assertThatThrownBy(() -> service.purchase(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, never()).getEventDate();
        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-005
    void shouldThrowExceptionWhenEventIsCancelled(){

        User user = mock(User.class);
        Event event = mock(Event.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.CANCELLED);

        assertThatThrownBy(() -> service.purchase(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, never()).getEventDate();
        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-006
    void shouldThrowExceptionWhenUserDoesNotMeetMinimumAge(){

        User user = mock(User.class);
        UserProfile profile = mock(UserProfile.class);
        Event event = mock(Event.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "laura@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("laura@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        LocalDateTime eventDate = LocalDateTime
                .of(2026, 12, 15, 18, 0);
        when(event.getEventDate()).thenReturn(eventDate);
        when(event.getMinimumAge()).thenReturn(18);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getBirthDate())
                .thenReturn(LocalDate.of(2009, 7, 20));

        assertThatThrownBy(() -> service.purchase(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmailIgnoreCase("laura@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, times(2)).getEventDate();
        verify(event, times(2)).getMinimumAge();
        verify(user) .getProfile();
        verify(profile).getBirthDate();
        verify(ticketRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-007
    void shouldThrowExceptionWhenEventIsSoldOut(){

        User user = mock(User.class);
        UserProfile profile = mock(UserProfile.class);
        Event event = mock(Event.class);
        Venue venue = mock(Venue.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getEventDate()).thenReturn(LocalDateTime.now().plusDays(30));
        when(event.getMinimumAge()).thenReturn(18);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getBirthDate())
                .thenReturn(LocalDate.of(2000, 5, 10));
        when(event.getEventCode()).thenReturn("CMF-2026");
        when(event.getVenue()).thenReturn(venue);
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository
                .countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID)).thenReturn(3L);

        assertThatThrownBy(() -> service.purchase(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, times(2)).getEventDate();
        verify(event, times(2)).getMinimumAge();
        verify(user).getProfile();
        verify(profile) .getBirthDate();
        verify(event).getEventCode();
        verify(ticketRepository)
                .countByEventEventCodeAndStatus("CMF-2026", TicketStatus.PAID);
        verify(event).getVenue();
        verify(venue).getCapacity();
        verify(ticketRepository, never()).save(any());
        verify(eventRepository, never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-008
    void shouldMarkEventAsSoldOutWhenBuyingLastAvailableTicket(){

        User user = mock(User.class);
        UserProfile profile = mock(UserProfile.class);
        Event event = mock(Event.class);
        Venue venue = mock(Venue.class);
        Ticket ticket = mock(Ticket.class);

        PurchaseTicketRequest request = new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );

        TicketResponse response = new TicketResponse(
                3L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.PAID,
                LocalDateTime.now(),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(userRepository.findByEmailIgnoreCase("andrea@email.com"))
                .thenReturn(Optional.of(user));
        when(user.getActive()).thenReturn(true);
        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getEventDate()).thenReturn(LocalDateTime.now().plusDays(30));
        when(event.getMinimumAge()).thenReturn(18);
        when(user.getProfile()).thenReturn(profile);
        when(profile.getBirthDate())
                .thenReturn(LocalDate.of(2000, 5, 10));
        when(event.getEventCode()).thenReturn("CMF-2026");
        when(event.getVenue()).thenReturn(venue);
        when(venue.getCapacity()).thenReturn(3);
        when(ticketRepository.countByEventEventCodeAndStatus
                ("CMF-2026", TicketStatus.PAID)).thenReturn(2L);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.purchase(request);

        assertThat(result).isEqualTo(response);
        verify(userRepository).findByEmailIgnoreCase("andrea@email.com");
        verify(user).getActive();
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, times(2)).getEventDate();
        verify(event, times(2)).getMinimumAge();
        verify(user).getProfile();
        verify(profile).getBirthDate();
        verify(event).getEventCode();
        verify(ticketRepository).countByEventEventCodeAndStatus(
                "CMF-2026", TicketStatus.PAID);
        verify(event).getVenue();
        verify(venue).getCapacity();
        verify(ticketRepository).save(any(Ticket.class));
        verify(event).setStatus(EventStatus.SOLD_OUT);
        verify(eventRepository).save(event);
        verify(ticketMapper).toResponse(ticket);
    }

    @Test
    // TEST-TICKET-009
    void shouldCancelTicketCorrectly(){

        Ticket ticket = mock(Ticket.class);
        Event event = mock(Event.class);
        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.CANCELLED,
                LocalDateTime.now(),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketRepository.findByTicketCode("TCK-12345678")).thenReturn(Optional.of(ticket));
        when(ticket.getStatus()).thenReturn(TicketStatus.PAID);
        when(ticket.getEvent()).thenReturn(event);
        when(event.getEventDate()).thenReturn(LocalDateTime.now().plusDays(10));
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.cancel("TCK-12345678");

        assertThat(result).isEqualTo(response);
        verify(ticketRepository).findByTicketCode("TCK-12345678");
        verify(ticket).getStatus();
        verify(ticket).getEvent();
        verify(event).getEventDate();
        verify(ticket).setStatus(TicketStatus.CANCELLED);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toResponse(ticket);
    }

    @Test
    // TEST-TICKET-010
    void shouldNotCancelUsedTicket(){

        Ticket ticket = mock(Ticket.class);

        when(ticketRepository.findByTicketCode("TCK-12345678")).thenReturn(Optional.of(ticket));
        when(ticket.getStatus()).thenReturn(TicketStatus.USED);

        assertThatThrownBy(() -> service.cancel("TCK-12345678") )
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository).findByTicketCode("TCK-12345678");
        verify(ticket).getStatus();
        verify(ticket, never()).setStatus(TicketStatus.CANCELLED);
        verify(ticketRepository,never()).save(any());
        verify(ticketMapper, never()).toResponse(any());
    }

    @Test
    // TEST-TICKET-011
    void shouldMarkPaidTicketAsUsedCorrectly(){

        Ticket ticket = mock(Ticket.class);

        TicketResponse response = new TicketResponse(
                1L,
                "TCK-12345678",
                TicketType.GENERAL,
                new BigDecimal("150000.00"),
                TicketStatus.USED,
                LocalDateTime.now(),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketRepository.findByTicketCode("TCK-12345678")).thenReturn(Optional.of(ticket));
        when(ticket.getStatus()).thenReturn(TicketStatus.PAID);
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = service.markAsUsed("TCK-12345678");

        assertThat(result).isEqualTo(response);
        verify(ticketRepository).findByTicketCode("TCK-12345678");
        verify(ticket).getStatus();
        verify(ticket).setStatus(TicketStatus.USED);
        verify(ticketRepository).save(ticket);
        verify(ticketMapper).toResponse(ticket);
    }

    @Test
    // TEST-TICKET-012
    void shouldThrowExceptionWhenUsingCancelledTicket(){

        Ticket ticket = mock(Ticket.class);

        when(ticketRepository.findByTicketCode("TCK-12345678")).thenReturn(Optional.of(ticket));
        when(ticket.getStatus()).thenReturn(TicketStatus.CANCELLED);

        assertThatThrownBy(() -> service.markAsUsed("TCK-12345678") )
                .isInstanceOf(BusinessRuleException.class);

        verify(ticketRepository).findByTicketCode("TCK-12345678");
        verify(ticket).getStatus();
        verify(ticket, never()).setStatus(TicketStatus.USED);
        verify(ticketRepository, never()) .save(any());
        verify(ticketMapper, never()).toResponse(any());
    }
}
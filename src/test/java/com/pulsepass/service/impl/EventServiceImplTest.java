package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private VenueRepository venueRepository;
    @Mock
    private ArtistRepository artistRepository;
    @Mock
    private EventMapper mapper;

    @InjectMocks EventServiceImpl service;

    @Test
    // TEST-EVENT-001
    void shouldReturnExistingEvent(){

        Event event = mock(Event.class);

        EventResponse response= new EventResponse(
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
                Set.of()
        );

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(mapper.toResponse(event)).thenReturn(response);

        EventResponse result = service.findByCode("CMF-2026");

        assertThat(result).isEqualTo(response);
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(mapper).toResponse(event);
    }

    @Test
    // TEST-TICKET-002
    void shouldThrowExceptionWhenEventDoesNotExists(){

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.empty());

        assertThatThrownBy(()-> service.findByCode("CMF-2026"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(eventRepository).findByEventCode("CMF-2026");
        verify(mapper, never()).toResponse(any());
    }

    @Test
    // TEST-EVENT-003
    void shouldCreateValidEvent(){

        Venue venue = mock(Venue.class);
        Event event = mock(Event.class);

        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01"
        );

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                request.eventDate(),
                18,
                "VEN-SMR-01",
                "Marina Convention Center",
                Set.of()
        );

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.getActive()).thenReturn(true);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(response);

        EventResponse result = service.create(request);

        assertThat(result).isEqualTo(response);
        verify(eventRepository).existsByEventCode("CMF-2026");
        verify(venueRepository).findByCode("VEN-SMR-01");
        verify(eventRepository).save(any());
        verify(mapper).toResponse(event);
    }

    @Test
    // TEST-EVENT-004
    void shouldThrowExceptionWhenVenueDoesNotExists(){

        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01"
        );

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.empty());

        assertThatThrownBy(()-> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository).existsByEventCode("CMF-2026");
        verify(venueRepository).findByCode("VEN-SMR-01");
        verify(eventRepository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    // TEST-EVENT-005
    void shouldThrowExceptionWhenVenueIsNotActive(){

        Venue venue = mock(Venue.class);

        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                LocalDateTime.of(2026, 12, 15, 18, 0),
                18,
                "VEN-SMR-01"
        );

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.getActive()).thenReturn(false);

        assertThatThrownBy(() -> service.create(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository).existsByEventCode("CMF-2026");
        verify(venueRepository).findByCode("VEN-SMR-01");
        verify(venue).getActive();
        verify(eventRepository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }

    @Test
    // TEST-EVENT-006
    void shouldReturnExceptionWhenEventDateIsInThePast(){

        Venue venue = mock(Venue.class);

        CreateEventRequest request = new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Caribbean Music Festival",
                EventCategory.MUSIC,
                LocalDateTime.now().minusDays(1),
                18,
                "VEN-SMR-01"
        );

        when(eventRepository.existsByEventCode("CMF-2026")).thenReturn(false);
        when(venueRepository.findByCode("VEN-SMR-01")).thenReturn(Optional.of(venue));
        when(venue.getActive()).thenReturn(true);

        assertThatThrownBy(() -> service.create(request) )
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository).existsByEventCode("CMF-2026");
        verify(venueRepository).findByCode("VEN-SMR-01");
        verify(venue).getActive();
        verify(eventRepository, never()).save(any(Event.class));
        verify(mapper, never()).toResponse(any());
    }

    @Test
    // TEST-EVENT-007
    void shouldPublishDraftEventCorrectly(){

        Event event = mock(Event.class);
        Venue venue = mock(Venue.class);

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

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.DRAFT);
        when(event.getEventDate()).thenReturn(LocalDateTime.now().plusDays(10));
        when(event.getVenue()).thenReturn(venue);
        when(venue.getActive()).thenReturn(true);
        when(eventRepository.save(event)).thenReturn(event);
        when(mapper.toResponse(event)).thenReturn(response);

        EventResponse result = service.publish("CMF-2026");

        assertThat(result).isEqualTo(response);

        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event).getEventDate();
        verify(event).getVenue();
        verify(venue).getActive();
        verify(event).setStatus(EventStatus.PUBLISHED);
        verify(eventRepository).save(event);
        verify(mapper).toResponse(event);
    }

    @Test
    // TEST-EVENT-008
    void shouldNotPublishCancelledEvent(){

        Event event = mock(Event.class);

        when(eventRepository.findByEventCode("CMF-2026")).thenReturn(Optional.of(event));
        when(event.getStatus()).thenReturn(EventStatus.CANCELLED);

        assertThatThrownBy(() -> service.publish("CMF-2026") )
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository).findByEventCode("CMF-2026");
        verify(event).getStatus();
        verify(event, never()).setStatus(EventStatus.PUBLISHED);
        verify(eventRepository, never()).save(any());
        verify(mapper, never()).toResponse(any());
    }
}
package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper mapper;

    public EventServiceImpl(EventRepository eventRepository, VenueRepository venueRepository, ArtistRepository artistRepository, EventMapper mapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public EventResponse create(CreateEventRequest request) {

        // Validar evento (BR-EVENT-001)
        if(eventRepository.existsByEventCode(request.eventCode())){
            throw new DuplicateResourceException(
                    "This event already exists: " + request.eventCode());
        }

        // Validar venue (BR-EVENT-002, BR-EVENT-003)
        Venue venue = venueRepository.findByCode(request.venueCode())
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Venue not found: " + request.venueCode())
                );

        if(!venue.getActive()){
            throw new BusinessRuleException(
                    "Cannot create event because the venue is not active"
            );
        }

        // Validar fecha (BR-EVENT-004)
        if(!request.eventDate().isAfter(LocalDateTime.now())){
            throw new BusinessRuleException(
                    "Event date must be in the future"
            );
        }

        // Validar edad minima (BR-EVENT-006)
        if (request.minimumAge() < 0) {
            throw new BusinessRuleException(
                    "Minimum age cannot be negative" );
        }

        // Estado inicial DRAFT (BR-EVENT-005)
        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge()
        );
        event.setVenue(venue);

        Event savedEvent = eventRepository.save(event);
        return mapper.toResponse(savedEvent);
    }

    @Override
    public EventResponse findByCode(String eventCode) {
        return eventRepository.findByEventCode(eventCode)
                .map(mapper::toResponse)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Event nor found: " + eventCode
                ));
    }

    @Override
    public List<EventSummaryResponse> findPublishedEvents() {
        return eventRepository
                .findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(String eventCode) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Event not found: " + eventCode
                ));

        // Validar estado DRAFT (BR-EVENT-007)
        if (event.getStatus() != EventStatus.DRAFT){
           throw new BusinessRuleException(
                   "Only DRAFT events can be published"
           );
        }

        // Validar fecha (BR-EVENT-008)
        if(!event.getEventDate().isAfter(LocalDateTime.now())){
            throw new BusinessRuleException(
                    "Event date must be in the future"
            );
        }

        // Validar venue (BR-EVENT-009)
        if(!event.getVenue().getActive()){
            throw new BusinessRuleException(
                    "Cannot publish event because the venue is not active"
            );
        }

        event.setStatus(EventStatus.PUBLISHED);

        Event savedEvent = eventRepository.save(event);
        return mapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse addArtist(String eventCode, Long artistId) {

        Event event = eventRepository.findByEventCode(eventCode)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Event not found: " + eventCode
                )
                );

        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(()-> new ResourceNotFoundException(
                        "Artist nor found: " + artistId
                ));

        // Validar estado del evento (BR-EVENT-011)
        if (event.getStatus() == EventStatus.CANCELLED
                || event.getStatus() == EventStatus.FINISHED){
            throw new BusinessRuleException(
                    "Cannot add artist to a CANCELLED or FINISHED event" );
        }

        // Validar artista no duplicado (BR-EVENT-010)
        boolean alreadyAssociated = event.getArtists()
                .stream()
                .anyMatch(
                        existingArtist -> existingArtist.getId().equals(artist.getId())
                );

        if (alreadyAssociated){
            throw new BusinessRuleException(
                    "This artist is already associated with this event" );
        }

        event.getArtists().add(artist);

        Event savedEvent = eventRepository.save(event);
        return mapper.toResponse(savedEvent);
    }

    @Override
    public List<EventSummaryResponse> findByArtist(String stageName) {
        return eventRepository
                .findByArtistStageName(stageName)
                .stream()
                .map(mapper::toSummary)
                .toList();
    }
}

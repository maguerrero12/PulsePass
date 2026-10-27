package com.pulsepass.controller;

import com.pulsepass.domain.EventStatus;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.service.EventService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(@Valid @RequestBody CreateEventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @GetMapping("/{eventCode}")
    public ResponseEntity<EventResponse> findByCode(@PathVariable String eventCode) {
        EventResponse event = eventService.findByCode(eventCode);
        if (event == null || event.status() != EventStatus.PUBLISHED) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null) {
                boolean isOps = auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
                if (!isOps) {
                    if (auth instanceof AnonymousAuthenticationToken || !auth.isAuthenticated()) {
                        throw new InsufficientAuthenticationException("Full authentication is required to access unpublished events");
                    }
                    throw new AccessDeniedException("You do not have permission to access unpublished events");
                }
            }
        }
        return ResponseEntity.ok(event);
    }

    @GetMapping("/published")
    public ResponseEntity<List<EventSummaryResponse>> findPublished() {
        return ResponseEntity.ok(eventService.findPublishedEvents());
    }

    @PatchMapping("/{eventCode}/publish")
    public ResponseEntity<EventResponse> publish(@PathVariable String eventCode) {
        return ResponseEntity.ok(eventService.publish(eventCode));
    }

    @PostMapping("/{eventCode}/artists/{artistId}")
    public ResponseEntity<EventResponse> addArtist(@PathVariable String eventCode,
                                                   @PathVariable Long artistId) {
        return ResponseEntity.ok(eventService.addArtist(eventCode, artistId));
    }

    @GetMapping("/by-artist")
    public ResponseEntity<List<EventSummaryResponse>> findByArtist(@RequestParam String stageName) {
        List<EventSummaryResponse> events = eventService.findByArtist(stageName);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            boolean isOps = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
            if (!isOps && events != null) {
                events = events.stream()
                        .filter(e -> e.status() == EventStatus.PUBLISHED)
                        .toList();
            }
        }
        return ResponseEntity.ok(events);
    }
}

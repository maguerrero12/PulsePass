package com.pulsepass.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event_code", nullable = false, unique = true)
    private String eventCode;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EventCategory category;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private EventStatus status;
    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;
    @Column(name = "minimum_age", nullable = false)
    private Integer minimumAge;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "venue_id", nullable = false)
    private Venue venue;

    @ManyToMany
    @JoinTable(name = "event_artists", joinColumns = @JoinColumn(name = "event_id"), inverseJoinColumns = @JoinColumn(name = "artist_id"))
    private Set<Artist> artists = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private List<Ticket> tickets = new ArrayList<>();

    public Event() {}

    public Event(String eventCode, String name, String description, EventCategory category, EventStatus status, LocalDateTime eventDate, Integer minimumAge) {
        this.eventCode = eventCode;
        this.name = name;
        this.description = description;
        this.category = category;
        this.status = status;
        this.eventDate = eventDate;
        this.minimumAge = minimumAge;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getEventCode() {return eventCode;}
    public void setEventCode(String eventCode) {this.eventCode = eventCode;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public EventCategory getCategory() {return category;}
    public void setCategory(EventCategory category) {this.category = category;}
    public EventStatus getStatus() {return status;}
    public void setStatus(EventStatus status) {this.status = status;}
    public LocalDateTime getEventDate() {return eventDate;}
    public void setEventDate(LocalDateTime eventDate) {this.eventDate = eventDate;}
    public Integer getMinimumAge() {return minimumAge;}
    public void setMinimumAge(Integer minimumAge) {this.minimumAge = minimumAge;}
    public Venue getVenue() {return venue;}
    public void setVenue(Venue venue) {this.venue = venue;}
    public Set<Artist> getArtists() {return artists;}
    public void setArtists(Set<Artist> artists) {this.artists = artists;}
    public List<Ticket> getTickets() {return tickets;}
    public void setTickets(List<Ticket> tickets) {this.tickets = tickets;}
}

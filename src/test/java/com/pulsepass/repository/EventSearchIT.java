package com.pulsepass.repository;

import com.pulsepass.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventSearchIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private EventRepository eventRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private VenueRepository venueRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void debeBuscarEventosPorArtistaSinDuplicarResultados() {
        Venue venue = venueRepository.saveAndFlush(crearVenue("Bogotá", "V-BOG"));

        Artist artistaBuscado = artistRepository.saveAndFlush(crearArtista("Banda Local"));
        Artist artistaRelleno = artistRepository.saveAndFlush(crearArtista("Banda Invitada"));

        Event e1 = crearEvento("EVT-1", venue);
        e1.getArtists().add(artistaBuscado);
        e1.getArtists().add(artistaRelleno); // Tiene ambos artistas

        Event e2 = crearEvento("EVT-2", venue);
        e2.getArtists().add(artistaBuscado); // Solo tiene el buscado

        eventRepository.saveAndFlush(e1);
        eventRepository.saveAndFlush(e2);


        // @Query("SELECT e FROM Event e JOIN e.artists a WHERE a.stageName = :stageName")
        List<Event> eventos = eventRepository.findByArtistStageName("Banda Local");

        assertThat(eventos).hasSize(2);
        assertThat(eventos).extracting(Event::getEventCode).containsExactlyInAnyOrder("EVT-1", "EVT-2");
    }

    private Venue crearVenue(String ciudad, String codigo) {
        Venue v = new Venue();
        v.setCode(codigo);
        v.setName("Teatro " + ciudad);
        v.setCity(ciudad);
        v.setAddress("Centro");
        v.setCapacity(1000);
        v.setActive(true);
        return v;
    }

    private Artist crearArtista(String stageName) {
        Artist a = new Artist();
        a.setStageName(stageName);
        a.setCountry("Colombia");
        a.setGenre("Rock");
        a.setActive(true);
        return a;
    }

    private Event crearEvento(String codigo, Venue venue) {
        Event e = new Event();
        e.setEventCode(codigo);
        e.setName("Concierto " + codigo);
        e.setDescription("Descripción requerida");
        e.setMinimumAge(14);
        e.setCategory(EventCategory.MUSIC);
        e.setStatus(EventStatus.PUBLISHED);
        e.setEventDate(LocalDateTime.now().plusDays(10));
        e.setVenue(venue);
        return e;
    }
}
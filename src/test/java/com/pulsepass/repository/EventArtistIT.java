package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
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
class EventArtistIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private ArtistRepository artistRepository;
    @Autowired
    private VenueRepository venueRepository;

    @BeforeEach
    void setUp() {

        eventRepository.deleteAll();
        artistRepository.deleteAll();
        venueRepository.deleteAll();
    }

    @Test
    void debeAsociarMultiplesArtistasAEventoSinDuplicados() {
        Venue venue = venueRepository.saveAndFlush(crearVenue());


        Artist artista1 = artistRepository.saveAndFlush(crearArtista("Banda Test 1"));
        Artist artista2 = artistRepository.saveAndFlush(crearArtista("Banda Test 2"));

        Event event = new Event();
        event.setEventCode("FEST-01");
        event.setName("Festival 01");
        event.setDescription("Festival de música de prueba");
        event.setMinimumAge(18);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCategory(EventCategory.MUSIC);
        event.setEventDate(LocalDateTime.now().plusMonths(1));
        event.setVenue(venue);


        event.getArtists().add(artista1);
        event.getArtists().add(artista2);
        event.getArtists().add(artista1); // Intento de duplicado en memoria

        Event guardado = eventRepository.saveAndFlush(event);


        assertThat(guardado.getArtists()).hasSize(2);
    }

    private Venue crearVenue() {
        Venue v = new Venue();
        v.setCode("V-1");
        v.setName("Teatro de Prueba");
        v.setCity("Santa Marta");
        v.setAddress("Calle 1 #2-3");
        v.setCapacity(100);
        v.setActive(true);
        return v;
    }

    private Artist crearArtista(String stageName) {
        Artist a = new Artist();
        a.setStageName(stageName);
        a.setCountry("CO");
        a.setGenre("Indie");
        a.setActive(true);
        return a;
    }
}
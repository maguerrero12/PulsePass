package com.pulsepass.repository;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventArtistIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private EventRepository eventRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private VenueRepository venueRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Venue venue;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        artistRepository.deleteAll();
        venueRepository.deleteAll();
        venue = venueRepository.saveAndFlush(crearVenue());
    }

    // AC-003 / FR-ART-003: tres artistas asociados, verificados contra la BD
    @Test
    void debeAsociarTresArtistasAUnEvento() {
        Artist a1 = artistRepository.saveAndFlush(crearArtista("Solar Beat Test"));
        Artist a2 = artistRepository.saveAndFlush(crearArtista("Neon Waves Test"));
        Artist a3 = artistRepository.saveAndFlush(crearArtista("Caribbean Sound Test"));

        Event event = crearEvento("CMF-2026");
        event.getArtists().add(a1);
        event.getArtists().add(a2);
        event.getArtists().add(a3);
        Long eventId = eventRepository.saveAndFlush(event).getId();

        entityManager.clear(); // fuerza a leer desde PostgreSQL, no desde memoria

        Event recuperado = eventRepository.findById(eventId).orElseThrow();
        assertThat(recuperado.getArtists()).hasSize(3);
        assertThat(recuperado.getArtists())
                .extracting(Artist::getStageName)
                .containsExactlyInAnyOrder("Solar Beat Test", "Neon Waves Test", "Caribbean Sound Test");
    }

    // AC-003: la BD impide repetir el par (event_id, artist_id)
    @Test
    void debeRechazarElMismoParEventoArtistaEnLaBaseDeDatos() {
        Artist artista = artistRepository.saveAndFlush(crearArtista("Banda Test 1"));
        Event event = crearEvento("FEST-01");
        event.getArtists().add(artista);
        Event guardado = eventRepository.saveAndFlush(event);

        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO event_artists (event_id, artist_id) VALUES (?, ?)",
                        guardado.getId(), artista.getId()));
    }

    // AC-007 / FR-ART-004: eventos de un artista, cada uno una sola vez
    @Test
    void debeBuscarEventosPorArtistaSinDuplicados() {
        Artist solar = artistRepository.saveAndFlush(crearArtista("Solar Beat Test"));
        Artist neon = artistRepository.saveAndFlush(crearArtista("Neon Waves Test"));

        Event e1 = crearEvento("EVT-1");
        e1.getArtists().add(solar);
        e1.getArtists().add(neon);
        Event e2 = crearEvento("EVT-2");
        e2.getArtists().add(solar);
        Event e3 = crearEvento("EVT-3");
        e3.getArtists().add(neon);
        eventRepository.saveAllAndFlush(List.of(e1, e2, e3));
        entityManager.clear();

        List<Event> eventos = eventRepository.findByArtistStageName("Solar Beat Test");

        assertThat(eventos).hasSize(2);
        assertThat(eventos).extracting(Event::getEventCode)
                .containsExactlyInAnyOrder("EVT-1", "EVT-2");
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

    private Event crearEvento(String code) {
        Event e = new Event();
        e.setEventCode(code);
        e.setName("Evento " + code);
        e.setDescription("Festival de música de prueba");
        e.setMinimumAge(18);
        e.setStatus(EventStatus.PUBLISHED);
        e.setCategory(EventCategory.MUSIC);
        e.setEventDate(LocalDateTime.now().plusMonths(1));
        e.setVenue(venue);
        return e;
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
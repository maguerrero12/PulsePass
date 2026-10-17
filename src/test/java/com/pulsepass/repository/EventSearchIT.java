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
        e1.getArtists().add(artistaRelleno);

        Event e2 = crearEvento("EVT-2", venue);
        e2.getArtists().add(artistaBuscado);

        eventRepository.saveAndFlush(e1);
        eventRepository.saveAndFlush(e2);

        List<Event> eventos = eventRepository.findByArtistStageName("Banda Local");

        assertThat(eventos).hasSize(2);
        assertThat(eventos).extracting(Event::getEventCode).containsExactlyInAnyOrder("EVT-1", "EVT-2");
    }

    @Test
    void debeFiltrarEventosPorCiudadYArtista() {
        // FR-SRC-002
        Venue venueSM = venueRepository.saveAndFlush(crearVenue("Santa Marta", "V-SMR"));
        Artist artista = artistRepository.saveAndFlush(crearArtista("Solar Beat"));

        Event e1 = crearEvento("EVT-SM", venueSM);
        e1.getArtists().add(artista);
        eventRepository.saveAndFlush(e1);

        List<Event> eventos = eventRepository.findByVenueCityAndArtistStageName("Santa Marta", "Solar Beat");

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getEventCode()).isEqualTo("EVT-SM");
    }

    @Test
    void debeBuscarEventosRecomendados() {
        // FR-SRC-003
        Venue venueCTG = venueRepository.saveAndFlush(crearVenue("Cartagena", "V-CTG"));
        Artist artista = artistRepository.saveAndFlush(crearArtista("Caribbean Sound"));

        Event e1 = crearEvento("EVT-CTG-1", venueCTG);
        e1.setEventDate(LocalDateTime.now().plusDays(5));
        e1.getArtists().add(artista);

        Event e2 = crearEvento("EVT-CTG-2", venueCTG);
        e2.setEventDate(LocalDateTime.now().plusDays(2)); // Más próximo, debe salir primero
        e2.getArtists().add(artista);

        eventRepository.saveAndFlush(e1);
        eventRepository.saveAndFlush(e2);

        // Llamada ajustada al orden exacto de tu EventRepository
        List<Event> recomendados = eventRepository.findRecommendedEvents(
                EventStatus.PUBLISHED,
                LocalDateTime.now(),
                "Cartagena",
                "caribbean"
        );

        assertThat(recomendados).hasSize(2);
        assertThat(recomendados.get(0).getEventCode()).isEqualTo("EVT-CTG-2"); // Orden cronológico
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
package com.pulsepass.repository;

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
class EventRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    private Venue venueBase;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        venueRepository.deleteAll();

        Venue venue = new Venue();
        venue.setCode("VEN-BOG-01");
        venue.setName("Movistar Arena");
        venue.setCity("Bogotá");
        venue.setAddress("Diagonal 61C");
        venue.setCapacity(14000);
        venue.setActive(true);

        this.venueBase = venueRepository.saveAndFlush(venue);
    }

    @Test
    void debeAsociarEventoAVenueExitosamente() {
        Event event = new Event();
        event.setEventCode("CMF-2026");
        event.setName("Caribbean Music Fest 2026");
        event.setDescription("Festival de música caribeña para fin de año");
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(EventStatus.PUBLISHED);
        event.setEventDate(LocalDateTime.now().plusDays(30));
        event.setMinimumAge(18);
        event.setVenue(venueBase);

        Event guardado = eventRepository.saveAndFlush(event);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getVenue().getCode()).isEqualTo("VEN-BOG-01");
    }

    @Test
    void debeRecuperarEventosPublicadosOrdenadosPorFecha() {
        Event eventoLejano = crearEvento("EVT-LEJOS", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(60));
        Event eventoCercano = crearEvento("EVT-CERCA", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10));
        Event eventoBorrador = crearEvento("EVT-DRAFT", EventStatus.DRAFT, LocalDateTime.now().plusDays(5));

        eventRepository.saveAllAndFlush(List.of(eventoLejano, eventoCercano, eventoBorrador));

        List<Event> publicados = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(publicados).hasSize(2);
        assertThat(publicados.get(0).getEventCode()).isEqualTo("EVT-CERCA");
        assertThat(publicados.get(1).getEventCode()).isEqualTo("EVT-LEJOS");
    }

    private Event crearEvento(String codigo, EventStatus estado, LocalDateTime fecha) {
        Event event = new Event();
        event.setEventCode(codigo);
        event.setName("Evento " + codigo);
        event.setDescription("Descripción automática para el evento " + codigo);
        event.setCategory(EventCategory.MUSIC);
        event.setStatus(estado);
        event.setEventDate(fecha);
        event.setMinimumAge(18);
        event.setVenue(venueBase);
        return event;
    }
}
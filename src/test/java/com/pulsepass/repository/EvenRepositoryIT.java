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
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        this.venueBase = venueRepository.saveAndFlush(
                crearVenue("VEN-BOG-01", "Movistar Arena", "Bogotá", 14000));
    }

    // FR-EVT-001: el evento queda asociado a un venue existente
    @Test
    void debeAsociarEventoAVenueExitosamente() {
        Event event = crearEvento("CMF-2026", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(30));
        event.setName("Caribbean Music Fest 2026");

        Event guardado = eventRepository.saveAndFlush(event);

        assertThat(guardado.getId()).isNotNull();
        assertThat(guardado.getVenue().getCode()).isEqualTo("VEN-BOG-01");
    }

    // AC-002: buscar por eventCode recupera el evento y su venue
    @Test
    void debeBuscarEventoPorEventCodeYRecuperarSuVenue() {
        eventRepository.saveAndFlush(
                crearEvento("ROCK-2026", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(20)));

        Optional<Event> encontrado = eventRepository.findByEventCode("ROCK-2026");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEventCode()).isEqualTo("ROCK-2026");
        assertThat(encontrado.get().getVenue().getCode()).isEqualTo("VEN-BOG-01");
    }

    // FR-EVT-005 / AC-006: solo publicados, ordenados por fecha ascendente
    @Test
    void debeRecuperarEventosPublicadosOrdenadosPorFecha() {
        Event lejano = crearEvento("EVT-LEJOS", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(60));
        Event cercano = crearEvento("EVT-CERCA", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10));
        Event borrador = crearEvento("EVT-DRAFT", EventStatus.DRAFT, LocalDateTime.now().plusDays(5));
        eventRepository.saveAllAndFlush(List.of(lejano, cercano, borrador));

        List<Event> publicados = eventRepository.findByStatusOrderByEventDateAsc(EventStatus.PUBLISHED);

        assertThat(publicados).hasSize(2);
        assertThat(publicados.get(0).getEventCode()).isEqualTo("EVT-CERCA");
        assertThat(publicados.get(1).getEventCode()).isEqualTo("EVT-LEJOS");
    }

    // FR-VEN-004: eventos de un venue por su código de negocio
    @Test
    void debeRecuperarSoloLosEventosDelVenueSolicitado() {
        Venue otroVenue = venueRepository.saveAndFlush(
                crearVenue("VEN-MED-01", "Plaza Mayor", "Medellín", 8000));

        Event enBogota = crearEvento("EVT-BOG", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10));
        Event enMedellin = crearEvento("EVT-MED", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(12));
        enMedellin.setVenue(otroVenue);
        eventRepository.saveAllAndFlush(List.of(enBogota, enMedellin));

        List<Event> eventos = eventRepository.findByVenueCode("VEN-BOG-01");

        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).getEventCode()).isEqualTo("EVT-BOG");
    }

    // FR-EVT-002: eventCode único
    @Test
    void debeRechazarConCodigoDuplicado() {
        eventRepository.saveAndFlush(
                crearEvento("CMF-2026", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(10)));

        Event duplicado = crearEvento("CMF-2026", EventStatus.PUBLISHED, LocalDateTime.now().plusDays(15));

        assertThrows(DataIntegrityViolationException.class,
                () -> eventRepository.saveAndFlush(duplicado));
    }

    private Venue crearVenue(String code, String name, String city, int capacity) {
        Venue venue = new Venue();
        venue.setCode(code);
        venue.setName(name);
        venue.setCity(city);
        venue.setAddress("Dirección de prueba");
        venue.setCapacity(capacity);
        venue.setActive(true);
        return venue;
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
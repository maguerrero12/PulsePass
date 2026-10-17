package com.pulsepass.repository;

import com.pulsepass.domain.*;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.VenueRepository;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TicketRepositoryIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private TicketRepository ticketRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EventRepository eventRepository;
    @Autowired private VenueRepository venueRepository;

    private User user;
    private Venue venue;
    private Event event;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();

        user = userRepository.saveAndFlush(crearUsuario("user_ticket", "tkt@example.com"));

        venue = new Venue();
        venue.setCode("V-TKT");
        venue.setName("Teatro Ticket");
        venue.setCity("Cartagena");
        venue.setAddress("Calle 123");
        venue.setCapacity(500);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        event = eventRepository.saveAndFlush(crearEvento("EVT-TKT-1"));
    }

    // FR-TKT-003: precio no negativo
    @Test
    void debeImpedirPrecioNegativoEnTicket() {
        Ticket ticket = crearTicket("TCK-001", TicketStatus.RESERVED, user, event);
        ticket.setPrice(new BigDecimal("-100.00"));

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(ticket));
    }

    // FR-TKT-002 / AC-005: el primer ticket es completo y válido
    @Test
    void debeRechazarTicketCodeDuplicado() {
        ticketRepository.saveAndFlush(crearTicket("TCK-0001", TicketStatus.PAID, user, event));

        Ticket duplicado = crearTicket("TCK-0001", TicketStatus.PAID, user, event);

        assertThrows(DataIntegrityViolationException.class,
                () -> ticketRepository.saveAndFlush(duplicado));
    }

    // FR-TKT-006: tickets de un usuario por email y estado
    @Test
    void debeConsultarTicketsPorEmailYEstado() {
        User otro = userRepository.saveAndFlush(crearUsuario("otro_user", "otro@example.com"));

        ticketRepository.saveAndFlush(crearTicket("TCK-A1", TicketStatus.PAID, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-A2", TicketStatus.RESERVED, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-B1", TicketStatus.PAID, otro, event));

        List<Ticket> tickets = ticketRepository.findByUserEmailAndStatus("tkt@example.com", TicketStatus.PAID);

        assertThat(tickets).hasSize(1);
        assertThat(tickets.get(0).getTicketCode()).isEqualTo("TCK-A1");
        assertThat(tickets.get(0).getUser().getEmail()).isEqualTo("tkt@example.com");
    }

    // FR-TKT-007: solo PAID y solo del evento solicitado
    @Test
    void debeRecuperarTicketsPagadosPorEventCode() {
        Event otroEvento = eventRepository.saveAndFlush(crearEvento("EVT-TKT-2"));

        ticketRepository.save(crearTicket("TCK-P1", TicketStatus.PAID, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-R1", TicketStatus.RESERVED, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-P2", TicketStatus.PAID, user, otroEvento));

        List<Ticket> pagados = ticketRepository.findByEventEventCodeAndStatus("EVT-TKT-1", TicketStatus.PAID);

        assertThat(pagados).hasSize(1);
        assertThat(pagados.get(0).getTicketCode()).isEqualTo("TCK-P1");
    }

    // FR-TKT-008 / AC-008: solo los PAID cuentan
    @Test
    void debeContarSoloTicketsPagadosPorEvento() {
        ticketRepository.saveAndFlush(crearTicket("TCK-1", TicketStatus.PAID, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-2", TicketStatus.PAID, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-3", TicketStatus.CANCELLED, user, event));
        ticketRepository.saveAndFlush(crearTicket("TCK-4", TicketStatus.RESERVED, user, event));

        long pagados = ticketRepository.countTicketsByEventCodeAndStatus("EVT-TKT-1", TicketStatus.PAID);

        assertThat(pagados).isEqualTo(2L);
    }

    private User crearUsuario(String username, String email) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(email);
        u.setActive(true);
        return u;
    }

    private Event crearEvento(String code) {
        Event e = new Event();
        e.setEventCode(code);
        e.setName("Concierto " + code);
        e.setDescription("Descripción requerida por DB");
        e.setMinimumAge(18);
        e.setStatus(EventStatus.PUBLISHED);
        e.setCategory(EventCategory.CULTURE);
        e.setEventDate(LocalDateTime.now().plusDays(10));
        e.setVenue(venue);
        return e;
    }

    private Ticket crearTicket(String code, TicketStatus status, User owner, Event evt) {
        Ticket t = new Ticket();
        t.setTicketCode(code);
        t.setType(TicketType.GENERAL);
        t.setPrice(new BigDecimal("150000"));
        t.setStatus(status);
        t.setPurchaseDate(LocalDateTime.now());
        t.setUser(owner);
        t.setEvent(evt);
        return t;
    }
}
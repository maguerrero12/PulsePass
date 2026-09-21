package com.pulsepass.repository;

import com.pulsepass.domain.*;
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
    private Event event;

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        userRepository.deleteAll();
        eventRepository.deleteAll();
        venueRepository.deleteAll();


        user = new User();
        user.setUsername("user_ticket");
        user.setEmail("tkt@example.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);


        Venue venue = new Venue();
        venue.setCode("V-TKT");
        venue.setName("Teatro Ticket");
        venue.setCity("Cartagena");
        venue.setAddress("Calle 123");
        venue.setCapacity(500);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);


        event = new Event();
        event.setEventCode("EVT-TKT-1");
        event.setName("Concierto Test");
        event.setDescription("Descripción requerida por DB");
        event.setMinimumAge(18);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCategory(EventCategory.CULTURE);
        event.setEventDate(LocalDateTime.now().plusDays(10));
        event.setVenue(venue);
        event = eventRepository.saveAndFlush(event);
    }

    @Test
    void debeImpedirPrecioNegativoEnTicket() {
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-001");
        ticket.setType(TicketType.GENERAL);
        ticket.setPrice(new BigDecimal("-100.00")); // Viola el CHECK constraint (precio >= 0)
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setUser(user);
        ticket.setEvent(event);

        assertThrows(DataIntegrityViolationException.class, () -> ticketRepository.saveAndFlush(ticket));
    }

    @Test
    void debeContarSoloTicketsPagadosPorEvento() {
        Ticket t1 = crearTicket("TCK-1", TicketStatus.PAID);
        Ticket t2 = crearTicket("TCK-2", TicketStatus.PAID);
        Ticket t3 = crearTicket("TCK-3", TicketStatus.CANCELLED);
        ticketRepository.saveAndFlush(t1);
        ticketRepository.saveAndFlush(t2);
        ticketRepository.saveAndFlush(t3);


        long paidCount = ticketRepository.countTicketsByEventCodeAndStatus("EVT-TKT-1", TicketStatus.PAID);

        assertThat(paidCount).isEqualTo(2L);
    }

    private Ticket crearTicket(String code, TicketStatus status) {
        Ticket t = new Ticket();
        t.setTicketCode(code);
        t.setType(TicketType.GENERAL);
        t.setPrice(new BigDecimal("150000"));
        t.setStatus(status);
        t.setPurchaseDate(LocalDateTime.now());
        t.setUser(user);
        t.setEvent(event);
        return t;
    }
}
package com.pulsepass.repository;

import com.pulsepass.domain.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class VenuePersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private VenueRepository venueRepository;

    private Venue crearVenue(String code, String name, int capacity) {
        Venue venue = new Venue();
        venue.setCode(code);
        venue.setName(name);
        venue.setCity("Santa Marta");
        venue.setAddress("Bahía");
        venue.setCapacity(capacity);
        venue.setActive(true);
        return venue;
    }

    // AC-001 / FR-VEN-001
    @Test
    void debePersistirYRecuperarVenuePorCodigo() {
        venueRepository.saveAndFlush(crearVenue("VEN-SMR-01", "Marina Convention Center", 5000));

        Optional<Venue> recuperado = venueRepository.findByCode("VEN-SMR-01");

        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getName()).isEqualTo("Marina Convention Center");
        assertThat(recuperado.get().getCapacity()).isEqualTo(5000);
    }

    // FR-VEN-002: código único
    @Test
    void debeRechazarConCodigoDuplicado() {
        venueRepository.saveAndFlush(crearVenue("VENUE-DUP", "Local A", 100));

        Venue duplicado = crearVenue("VENUE-DUP", "Local B", 200);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(duplicado));
    }

    // FR-VEN-003: capacidad mayor que cero (el resto de campos es válido)
    @Test
    void debeRechazarVenueConCapacidadInvalida() {
        Venue invalido = crearVenue("VEN-ERR", "Lugar Pequeño", 0);

        assertThrows(DataIntegrityViolationException.class,
                () -> venueRepository.saveAndFlush(invalido));
    }
}


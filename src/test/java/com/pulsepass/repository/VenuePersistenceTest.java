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

    @Test
    void debePersistirYRecuperarVenuePorCodigo() {
        Venue venue = new Venue();
        venue.setCode("VEN-SMR-01");
        venue.setName("Marina Convention Center");
        venue.setCity("Santa Marta");
        venue.setAddress("Bahía");
        venue.setCapacity(5000);
        venue.setActive(true);

        venueRepository.saveAndFlush(venue);

        Optional<Venue> recuperado = venueRepository.findByCode("VEN-SMR-01");
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getCapacity()).isEqualTo(5000);
    }

    @Test
    void debeRechazarConCodigoDuplicado() {

        Venue venue1 = new Venue();
        venue1.setName("Local A");
        venue1.setCode("VENUE-DUP");
        venue1.setAddress("Avenida 1");
        venue1.setActive(true);
        venue1.setCapacity(100);
        venue1.setCity("Santa Marta");
        venueRepository.saveAndFlush(venue1);


        assertThrows(DataIntegrityViolationException.class, () -> {
            Venue venue2 = new Venue();
            venue2.setName("Local B");
            venue2.setCode("VENUE-DUP");
            venue2.setAddress("Avenida 2");
            venue2.setActive(true);
            venue2.setCapacity(200);
            venue2.setCity("Bogotá");

            venueRepository.saveAndFlush(venue2);
        });
    }

    @Test
    void debeRechazarVenueConCapacidadInvalida() {
        Venue venue = new Venue();
        venue.setCode("VEN-ERR");
        venue.setName("Lugar Pequeño");
        venue.setCapacity(0); // Viola el CHECK (capacity > 0)

        assertThrows(DataIntegrityViolationException.class, () -> venueRepository.saveAndFlush(venue));
    }
}



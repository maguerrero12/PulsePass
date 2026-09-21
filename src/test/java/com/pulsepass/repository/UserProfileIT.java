package com.pulsepass.repository;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
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

import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserProfileIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired private UserRepository userRepository;
    @Autowired private UserProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void debeImpedirDosPerfilesParaElMismoUsuario() {
        User user = new User();
        user.setUsername("david_br");
        user.setEmail("david@ejemplo.com");
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        // Primer perfil: válido
        UserProfile perfil1 = new UserProfile();
        perfil1.setFirstName("David");
        perfil1.setLastName("Brunal");
        perfil1.setCity("Santa Marta");
        perfil1.setPhone("3000000000");
        perfil1.setBirthDate(LocalDate.of(2000, 1, 1));
        perfil1.setUser(user);
        profileRepository.saveAndFlush(perfil1);

        // Segundo perfil para el MISMO usuario: debe fallar por la FK UNIQUE
        UserProfile perfil2 = new UserProfile();
        perfil2.setFirstName("Intento Duplicado");
        perfil2.setLastName("Error");
        perfil2.setUser(user);

        assertThrows(DataIntegrityViolationException.class, () -> profileRepository.saveAndFlush(perfil2));
    }
}
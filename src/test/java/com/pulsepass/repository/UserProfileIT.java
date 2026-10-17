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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserProfileIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProfileRepository profileRepository;

    @BeforeEach
    void setUp() {
        profileRepository.deleteAll();
        userRepository.deleteAll();
    }

    // FR-USR-003 / FR-USR-004: un usuario con su perfil (1:1)
    @Test
    void debePersistirUsuarioConSuPerfil() {
        User user = userRepository.saveAndFlush(crearUsuario("david_br", "david@ejemplo.com"));
        UserProfile guardado = profileRepository.saveAndFlush(crearPerfil(user, "David"));

        UserProfile recuperado = profileRepository.findById(guardado.getId()).orElseThrow();

        assertThat(recuperado.getFirstName()).isEqualTo("David");
        assertThat(recuperado.getCity()).isEqualTo("Santa Marta");
        assertThat(recuperado.getUser().getEmail()).isEqualTo("david@ejemplo.com");
    }

    // AC-004: un segundo perfil para el mismo usuario viola la FK UNIQUE
    @Test
    void debeImpedirDosPerfilesParaElMismoUsuario() {
        User user = userRepository.saveAndFlush(crearUsuario("david_br", "david@ejemplo.com"));
        profileRepository.saveAndFlush(crearPerfil(user, "David"));

        UserProfile segundoPerfil = crearPerfil(user, "Intento Duplicado");

        assertThrows(DataIntegrityViolationException.class,
                () -> profileRepository.saveAndFlush(segundoPerfil));
    }

    // Query Method: buscar usuario por email ignorando mayúsculas
    @Test
    void debeBuscarUsuarioPorEmailIgnorandoMayusculas() {
        userRepository.saveAndFlush(crearUsuario("andrea", "Andrea@Ejemplo.com"));

        Optional<User> encontrado = userRepository.findByEmailIgnoreCase("andrea@ejemplo.com");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getUsername()).isEqualTo("andrea");
    }

    private User crearUsuario(String username, String email) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(true);
        return user;
    }

    private UserProfile crearPerfil(User user, String firstName) {
        UserProfile perfil = new UserProfile();
        perfil.setFirstName(firstName);
        perfil.setLastName("Brunal");
        perfil.setCity("Santa Marta");
        perfil.setPhone("3000000000");
        perfil.setBirthDate(LocalDate.of(2000, 1, 1));
        perfil.setUser(user);
        return perfil;
    }
}
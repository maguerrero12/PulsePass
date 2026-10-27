package com.pulsepass.security;

import com.pulsepass.domain.*;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.*;
import com.pulsepass.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class SecurityPostgresIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("pulsepass_test_db")
            .withUsername("test_user")
            .withPassword("test_secret_password_123");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");

        registry.add("pulsepass.security.jwt.secret-base64", () -> "dGhpc0lzQVNlY3JldEtleUZvclB1bHNlUGFzc1Rlc3RpbmdPbmx5MTIzNDU2Nzg5MA==");
        registry.add("pulsepass.security.jwt.issuer", () -> "pulsepass-api");
        registry.add("pulsepass.security.jwt.access-token-ttl", () -> "15m");
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository credentialRepository;

    @Autowired
    private UserRoleRepository roleRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private VenueRepository venueRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
        eventRepository.deleteAll();
        roleRepository.deleteAll();
        credentialRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("Prueba de registro atómico, persistencia de hash BCrypt y unicidad de roles")
    void testAtomicRegisterAndBCryptPersistence() {
        String rawPassword = "SecurePassword123!";
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "integration_user",
                "integration@pulsepass.com",
                "Test",
                "User",
                "3000000000",
                "Bogota",
                rawPassword,
                LocalDate.of(1995, 8, 20)
        );

        UserResponse registeredUser = authService.register(registerRequest);

        assertThat(registeredUser).isNotNull();
        assertThat(registeredUser.id()).isNotNull();

        // 1. Validar persistencia de usuario
        Optional<User> userOptional = userRepository.findById(registeredUser.id());
        assertThat(userOptional).isPresent();

        // 2. Validar persistencia de credencial y hash BCrypt
        Optional<UserCredential> credentialOptional = credentialRepository.findById(registeredUser.id());
        assertThat(credentialOptional).isPresent();
        assertThat(credentialOptional.get().getPasswordHash()).isNotBlank();
        assertThat(credentialOptional.get().getPasswordHash()).startsWith("$2");

        // 3. Validar asignación de rol
        List<UserRole> roles = roleRepository.findByUserId(registeredUser.id());
        assertThat(roles).hasSize(1);
        assertThat(roles.get(0).getRole()).isEqualTo("USER");

        // 4. Validar unicidad de roles directamente contra la BD (PostgreSQL / Flyway constraint)
        Long userId = registeredUser.id();
        assertThatThrownBy(() ->
                jdbcTemplate.update("INSERT INTO user_roles (user_id, role) VALUES (?, ?)", userId, "USER")
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Prueba de login contra usuario real almacenado en PostgreSQL")
    void testRealUserLoginAgainstPostgres() {
        String username = "login_user";
        String email = "login@pulsepass.com";
        String rawPassword = "TestPassword456!";

        // 1. Crear usuario directamente en la BD real
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setActive(true);
        user = userRepository.saveAndFlush(user);

        // 2. Crear y guardar perfil del usuario
        UserProfile profile = new UserProfile();
        profile.setUser(user);
        profile.setFirstName("Login");
        profile.setLastName("User");
        profile.setPhone("3001112233");
        profile.setCity("Medellin");
        profile.setBirthDate(LocalDate.of(1998, 3, 15));
        user.setProfile(profile);
        userRepository.saveAndFlush(user);

        // 3. Crear credenciales con el PasswordEncoder real del sistema
        UserCredential credential = new UserCredential();
        credential.setUser(user);
        credential.setPasswordHash(passwordEncoder.encode(rawPassword));
        credential.setPasswordChangedAt(LocalDateTime.now());
        credentialRepository.saveAndFlush(credential);

        // 4. Asignar rol al usuario
        UserRole role = new UserRole(user, "USER");
        roleRepository.saveAndFlush(role);

        // 5. Ejecutar login a través de AuthService usando el email o username
        LoginRequest loginRequest = new LoginRequest(email, rawPassword);
        AuthResponse authResponse = authService.login(loginRequest);

        assertThat(authResponse).isNotNull();
        assertThat(authResponse.accessToken()).isNotBlank();
        assertThat(authResponse.tokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("Prueba de consulta segura de propietario directamente en BD")
    void testSecureOwnerQueryInPostgres() {
        RegisterUserRequest registerRequest = new RegisterUserRequest(
                "owner_user",
                "owner@pulsepass.com",
                "Owner",
                "User",
                "3004445566",
                "Cali",
                "OwnerPassword789!",
                LocalDate.of(1992, 11, 5)
        );

        UserResponse registeredUser = authService.register(registerRequest);
        User user = userRepository.findById(registeredUser.id()).orElseThrow();

        // 1. Crear y guardar Venue obligatorio
        Venue venue = new Venue();
        venue.setCode("VEN-TEST-001");
        venue.setName("Movistar Arena Test");
        venue.setCity("Cali");
        venue.setAddress("Calle 10 # 5-20");
        venue.setCapacity(15000);
        venue.setActive(true);
        venue = venueRepository.saveAndFlush(venue);

        // 2. Crear y guardar Event asignando Venue y campos obligatorios
        Event event = new Event();
        event.setEventCode("EVT-TEST-001");
        event.setName("Concierto Test");
        event.setDescription("Descripción Test");
        event.setEventDate(LocalDateTime.now().plusDays(5));
        event.setMinimumAge(18);
        event.setStatus(EventStatus.PUBLISHED);
        event.setCategory(EventCategory.MUSIC);
        event.setVenue(venue);
        event = eventRepository.saveAndFlush(event);

        // 3. Crear y guardar Ticket asignando Usuario y Evento
        Ticket ticket = new Ticket();
        ticket.setTicketCode("TCK-POSTGRES-001");
        ticket.setUser(user);
        ticket.setEvent(event);
        ticket.setType(TicketType.GENERAL);
        ticket.setStatus(TicketStatus.RESERVED);
        ticket.setPrice(BigDecimal.valueOf(150000));
        ticket.setPurchaseDate(LocalDateTime.now());

        ticketRepository.saveAndFlush(ticket);

        // 4. Validar consulta segura de propietario
        Optional<Ticket> retrievedTicket = ticketRepository.findByTicketCode("TCK-POSTGRES-001");
        assertThat(retrievedTicket).isPresent();
        assertThat(retrievedTicket.get().getUser().getId()).isEqualTo(user.getId());
    }
}

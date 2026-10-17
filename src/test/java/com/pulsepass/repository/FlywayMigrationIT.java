package com.pulsepass.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class FlywayMigrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    // QT-001: Flyway aplica las migraciones desde una base vacía
    @Test
    void debeAplicarTodasLasMigracionesSinErrores() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Ninguna migración debe haber fallado
            try (ResultSet failed = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false")) {
                failed.next();
                assertThat(failed.getInt(1)).isZero();
            }

            // V1, V2 y V3 deben estar aplicadas con éxito
            try (ResultSet base = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history " +
                            "WHERE version IN ('1','2','3') AND success = true")) {
                base.next();
                assertThat(base.getInt(1)).isEqualTo(3);
            }
        }
    }
    // Fase 4: la migración de seguridad crea credenciales y roles
    @Test
    void debeAplicarLaMigracionDeSeguridad() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            try (ResultSet v4 = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE version = '4' AND success = true")) {
                v4.next();
                assertThat(v4.getInt(1)).isEqualTo(1);
            }

            List<String> tablas = new ArrayList<>();
            try (ResultSet rs = statement.executeQuery(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
                while (rs.next()) {
                    tablas.add(rs.getString("table_name"));
                }
            }
            assertThat(tablas).contains("user_credentials", "user_roles");
        }
    }
    // QT-002: Hibernate valida el esquema sin crearlo ni modificarlo
    @Test
    void hibernateDebeValidarElEsquemaYExistenLasTablasDelModelo() throws SQLException {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
                .isEqualTo("validate");

        List<String> tablas = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'")) {
            while (rs.next()) {
                tablas.add(rs.getString("table_name"));
            }
        }


        assertThat(tablas).contains(
                "venues", "events", "artists", "event_artists",
                "users", "user_profiles", "tickets");
    }
}
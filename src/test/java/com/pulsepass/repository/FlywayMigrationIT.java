package com.pulsepass.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

    @Test
    void debeHaberAplicadoTodasLasMigracionesDeFlyway() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            // Ninguna migración debe haber fallado
            try (ResultSet failed = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = false")) {
                failed.next();
                assertThat(failed.getInt(1)).isZero();
            }

            // Debe haberse aplicado al menos una migración exitosamente
            try (ResultSet applied = statement.executeQuery(
                    "SELECT COUNT(*) FROM flyway_schema_history WHERE success = true")) {
                applied.next();
                assertThat(applied.getInt(1)).isPositive();
            }
        }
    }
}
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
        try (Statement statement = dataSource.getConnection().createStatement()) {
            // Consultamos la tabla interna de Flyway para ver el estado de las migraciones
            ResultSet resultSet = statement.executeQuery(
                    "SELECT version, success FROM flyway_schema_history ORDER BY installed_rank DESC"
            );

            // Verificamos que haya resultados
            assertThat(resultSet.next()).isTrue();

            // Verificamos que la última migración aplicada sea la V3 (streaming_url) y haya sido exitosa
            assertThat(resultSet.getString("version")).isEqualTo("3");
            assertThat(resultSet.getBoolean("success")).isTrue();
        }
    }
}
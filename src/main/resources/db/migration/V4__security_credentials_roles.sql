-- Tabla para almacenar el hash de la contraseña (Relación 1:1 con User)
CREATE TABLE user_credentials (
                                  user_id BIGINT PRIMARY KEY,
                                  password_hash VARCHAR(255) NOT NULL,
                                  password_changed_at TIMESTAMP,
                                  CONSTRAINT fk_user_credentials_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Tabla para almacenar los roles del usuario (USER, STAFF, ADMIN)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role VARCHAR(50) NOT NULL,
                            PRIMARY KEY (user_id, role),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Migración de datos legados:
-- A los usuarios que ya existían (de la V1) les asignaremos el rol USER por defecto
INSERT INTO user_roles (user_id, role)
SELECT id, 'USER' FROM users;
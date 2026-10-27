-- tests/e2e/fixtures/seed-e2e.sql
-- Fixtures obligatorios para Pruebas E2E
-- La contraseña para todos estos usuarios de prueba es: password

-- 1. Insertar Usuarios (ADMIN, STAFF y Usuario Inactivo)
INSERT INTO users (id, username, email, active) VALUES
                                                    (901, 'admin_e2e', 'admin@example.test', true),
                                                    (902, 'staff_e2e', 'staff@example.test', true),
                                                    (903, 'inactivo_e2e', 'inactivo@example.test', false);

-- 2. Insertar Perfiles
INSERT INTO user_profiles (id, first_name, last_name, phone, city, birth_date, user_id) VALUES
                                                                                            (901, 'Admin', 'Global', '555-0001', 'Bogotá', '1980-01-01', 901),
                                                                                            (902, 'Staff', 'Operativo', '555-0002', 'Bogotá', '1990-01-01', 902),
                                                                                            (903, 'Usuario', 'Inactivo', '555-0003', 'Bogotá', '1995-01-01', 903);

-- 3. Insertar Credenciales
-- El hash corresponde a la palabra "password" en BCrypt
INSERT INTO user_credentials (user_id, password_hash) VALUES
                                                          (901, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCG3JZGZDAF.kZk0Q8hj6'),
                                                          (902, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCG3JZGZDAF.kZk0Q8hj6'),
                                                          (903, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HCG3JZGZDAF.kZk0Q8hj6');

-- 4. Insertar Roles
INSERT INTO user_roles (user_id, role) VALUES
                                           (901, 'ADMIN'),
                                           (902, 'STAFF'),
                                           (903, 'USER');
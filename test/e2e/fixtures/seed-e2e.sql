-- =============================================================================
-- PulsePass — Fixtures obligatorios para Pruebas Manuales E2E (Fase 5)
-- Archivo: test/e2e/fixtures/seed-e2e.sql
-- Contraseña para los usuarios de prueba (admin_e2e, staff_e2e, inactivo_e2e):
--   password
-- Hash BCrypt ($2a$10$) válido generado por BCryptPasswordEncoder para "password":
--   $2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG
-- =============================================================================

-- 0. Limpieza idempotente de ejecuciones manuales previas
DELETE FROM tickets WHERE event_id IN (
    SELECT id FROM events WHERE event_code IN ('CMF-2026', 'DRAFT-01', 'PUB-01', 'EVT-E2E-01', 'FAIL-01')
);
DELETE FROM event_artists WHERE event_id IN (
    SELECT id FROM events WHERE event_code IN ('CMF-2026', 'DRAFT-01', 'PUB-01', 'EVT-E2E-01', 'FAIL-01')
);
DELETE FROM events WHERE event_code IN ('EVT-E2E-01', 'FAIL-01');

-- 1. Asegurar Venue activo base (Escenario de aceptación: VEN-SMR-01)
INSERT INTO venues (code, name, city, address, capacity, active)
VALUES ('VEN-SMR-01', 'Marina Convention Center', 'Santa Marta', 'Carrera 1 # 22-10', 3, true)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    city = EXCLUDED.city,
    address = EXCLUDED.address,
    capacity = EXCLUDED.capacity,
    active = true;

-- 2. Asegurar Artistas activos base (Solar Beat, Neon Waves, Caribbean Sound)
INSERT INTO artists (stage_name, country, genre, active) VALUES
    ('Solar Beat', 'Colombia', 'Electronic', true),
    ('Neon Waves', 'Mexico', 'Synthwave', true),
    ('Caribbean Sound', 'Colombia', 'Reggae', true)
ON CONFLICT (stage_name) DO UPDATE SET
    active = true;

-- 3. Insertar o actualizar Usuarios de fixture (ADMIN, STAFF y Usuario Inactivo)
INSERT INTO users (id, username, email, active) VALUES
    (901, 'admin_e2e', 'admin@example.test', true),
    (902, 'staff_e2e', 'staff@example.test', true),
    (903, 'inactivo_e2e', 'inactivo@example.test', false)
ON CONFLICT (id) DO UPDATE SET
    username = EXCLUDED.username,
    email = EXCLUDED.email,
    active = EXCLUDED.active;

-- 4. Insertar o actualizar Perfiles de los usuarios de fixture
INSERT INTO user_profiles (id, first_name, last_name, phone, city, birth_date, user_id) VALUES
    (901, 'Admin', 'Global', '555-0001', 'Bogotá', '1980-01-01', 901),
    (902, 'Staff', 'Operativo', '555-0002', 'Bogotá', '1990-01-01', 902),
    (903, 'Usuario', 'Inactivo', '555-0003', 'Bogotá', '1995-01-01', 903)
ON CONFLICT (user_id) DO UPDATE SET
    first_name = EXCLUDED.first_name,
    last_name = EXCLUDED.last_name,
    phone = EXCLUDED.phone,
    city = EXCLUDED.city,
    birth_date = EXCLUDED.birth_date;

-- 5. Insertar o actualizar Credenciales BCrypt (contraseña: "password")
INSERT INTO user_credentials (user_id, password_hash) VALUES
    (901, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'),
    (902, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG'),
    (903, '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG')
ON CONFLICT (user_id) DO UPDATE SET
    password_hash = EXCLUDED.password_hash;

-- 6. Insertar Roles de los usuarios de fixture
INSERT INTO user_roles (user_id, role) VALUES
    (901, 'ADMIN'),
    (902, 'STAFF'),
    (903, 'USER')
ON CONFLICT (user_id, role) DO NOTHING;

-- 7. Insertar o restaurar Eventos de fixture (Sección 5.1 PRD Fase 5):
--    - CMF-2026 (publishedEventCode): PUBLISHED, fecha futura, edad mínima 18
--    - DRAFT-01 (draftEventCode): DRAFT, fecha futura, invisible al público
--    - PUB-01 (eventToPublishCode): DRAFT, listo para PATCH /api/events/PUB-01/publish
INSERT INTO events (event_code, name, description, category, status, event_date, minimum_age, venue_id)
VALUES
    ('CMF-2026', 'Caribbean Music Fest 2026', 'Festival de música del Caribe', 'MUSIC', 'PUBLISHED', '2027-12-15 18:00:00', 18, (SELECT id FROM venues WHERE code = 'VEN-SMR-01')),
    ('DRAFT-01', 'Festival E2E Borrador', 'Evento en borrador para verificar protección de visibilidad', 'MUSIC', 'DRAFT', '2027-12-20 18:00:00', 18, (SELECT id FROM venues WHERE code = 'VEN-SMR-01')),
    ('PUB-01', 'Festival E2E Por Publicar', 'Evento preparado en DRAFT para publicar con PATCH', 'MUSIC', 'DRAFT', '2027-12-22 18:00:00', 18, (SELECT id FROM venues WHERE code = 'VEN-SMR-01'))
ON CONFLICT (event_code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    category = EXCLUDED.category,
    status = EXCLUDED.status,
    event_date = EXCLUDED.event_date,
    minimum_age = EXCLUDED.minimum_age,
    venue_id = EXCLUDED.venue_id;

-- 8. Asociar el artista 'Solar Beat' a CMF-2026 (publicado) y a DRAFT-01 (borrador)
--    Esto permite comprobar en E2E-EVT-06 / TC-SEC-005 que /api/events/by-artist
--    solo devuelve CMF-2026 al público y nunca filtra DRAFT-01.
INSERT INTO event_artists (event_id, artist_id)
VALUES
    ((SELECT id FROM events WHERE event_code = 'CMF-2026'), (SELECT id FROM artists WHERE stage_name = 'Solar Beat')),
    ((SELECT id FROM events WHERE event_code = 'DRAFT-01'), (SELECT id FROM artists WHERE stage_name = 'Solar Beat'))
ON CONFLICT DO NOTHING;

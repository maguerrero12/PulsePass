# Checklist de Ejecución Manual E2E — PulsePass (Fase 5)

Orden obligatorio de rutas y escenarios conforme al Capítulo 9 del PRD (`PRD_PulsePass_Pruebas_E2E_Postman_Manual.md`).

## 0. Preflight y Preparación
- [ ] Base de datos PostgreSQL activa, migraciones Flyway aplicadas y `fixtures/seed-e2e.sql` ejecutado.
- [ ] Colección `PulsePass-Manual-E2E.postman_collection.json` y entorno `PulsePass-local.postman_environment.json` importados en Postman.
- [ ] Variables secretas configuradas en **Current Value** del environment (`userAPassword`, `userBPassword`, `adminIdentifier`, `staffIdentifier`, `adminPassword`, `staffPassword`).
- [ ] `PREFLIGHT-01` — `GET /api/venues/active` responde `200 OK`.

## 1. Flujo A — Descubrimiento Público (sin autenticación)
- [ ] `E2E-VEN-02` — `GET /api/venues/active` → `200 OK` (verificar `venueActiveCode = VEN-SMR-01`).
- [ ] `E2E-VEN-01` — `GET /api/venues/{code}` → `200 OK`.
- [ ] `E2E-ART-03` — `GET /api/artists/active` → `200 OK` (identificar `artistId = 1`).
- [ ] `E2E-ART-01` — `GET /api/artists/{id}` → `200 OK`.
- [ ] `E2E-ART-02` — `GET /api/artists/by-stage-name?stageName={{artistStageName}}` → `200 OK`.
- [ ] `E2E-EVT-03` — `GET /api/events/published` → `200 OK` (incluye `CMF-2026`, nunca `DRAFT-01`).
- [ ] `E2E-EVT-02` — `GET /api/events/{{publishedEventCode}}` → `200 OK`.
- [ ] `TC-SEC-004` — `GET /api/events/{{draftEventCode}}` (anónimo) → `401 Unauthorized` (no expone datos del borrador).
- [ ] `E2E-EVT-06` / `TC-SEC-005` — `GET /api/events/by-artist?stageName={{artistStageName}}` → `200 OK` (solo incluye `CMF-2026`, nunca `DRAFT-01`).

## 2. Flujo B — Registro y JWT
- [ ] `E2E-USR-01` / `TC-AUTH-001` — `POST /api/users` (Registrar `USER_A`) → `201 Created` (sin JWT implícito, sin exponer hash).
- [ ] `E2E-USR-01` / `TC-BIZ-006` — `POST /api/users` (Registrar `USER_B`) → `201 Created`.
- [ ] `E2E-AUTH-01` / `TC-AUTH-002` — `POST /api/auth/login` (`USER_A` con username) → `200 OK` → copiar `accessToken` a `userAToken`.
- [ ] `TC-AUTH-003` — `POST /api/auth/login` (`USER_A` con email) → `200 OK`.
- [ ] `E2E-AUTH-01` — `POST /api/auth/login` (`USER_B`) → `200 OK` → copiar `accessToken` a `userBToken`.
- [ ] `E2E-AUTH-01` — `POST /api/auth/login` (`ADMIN`) → `200 OK` → copiar `accessToken` a `adminToken`.
- [ ] `E2E-AUTH-01` — `POST /api/auth/login` (`STAFF`) → `200 OK` → copiar `accessToken` a `staffToken`.
- [ ] `E2E-AUTH-02` / `TC-AUTH-006` — `GET /api/auth/me` con `userAToken`, `adminToken` y `staffToken` → `200 OK` (identidad y roles efectivos sin credenciales).
- [ ] `TC-AUTH-004` — `POST /api/auth/login` con password erróneo → `401 Unauthorized`.
- [ ] `TC-AUTH-005` — `POST /api/auth/login` con identificador inexistente → `401 Unauthorized`.
- [ ] `TC-AUTH-007` — `GET /api/auth/me` sin header `Authorization` → `401 Unauthorized`.
- [ ] `TC-AUTH-008` / `TC-AUTH-009` — `GET /api/auth/me` con Bearer malformado/alterado → `401 Unauthorized`.
- [ ] `TC-AUTH-013` — `POST /api/auth/login` con usuario inactivo (`inactivo_e2e`) → `401 Unauthorized`.

## 3. Consultas de Usuario y Propiedad
- [ ] `E2E-USR-02` — `GET /api/users/by-email?email={{userAEmail}}` con `userAToken` → `200 OK`.
- [ ] `E2E-USR-03` — `GET /api/users/by-username?username={{userAUsername}}` con `userAToken` → `200 OK`.
- [ ] `E2E-USR-02 (ADMIN)` — `GET /api/users/by-email?email={{userAEmail}}` con `adminToken` → `200 OK`.
- [ ] `TC-SEC-006` — `GET /api/users/by-email?email={{userAEmail}}` con `userBToken` → `403 Forbidden`.
- [ ] `TC-SEC-007` — `GET /api/users/by-username?username={{userAUsername}}` con `userBToken` → `403 Forbidden`.

## 4. Flujo C — Gestión del Catálogo (ADMIN)
- [ ] `E2E-EVT-01` / `TC-BIZ-003` — `POST /api/events` con `adminToken` → `201 Created` (`status = DRAFT`).
- [ ] `TC-SEC-015` — `GET /api/events/{{draftEventCode}}` con `adminToken` → `200 OK` (`ADMIN` puede consultar borrador).
- [ ] `E2E-EVT-05` / `TC-BIZ-005` — `POST /api/events/{{eventToPublishCode}}/artists/{{artistId}}` con `adminToken` → `200 OK`.
- [ ] `E2E-EVT-04` / `TC-BIZ-004` — `PATCH /api/events/{{eventToPublishCode}}/publish` con `adminToken` → `200 OK` (`status = PUBLISHED`).
- [ ] `TC-SEC-001` — `POST /api/events` sin token (`401`) y con `userAToken` (`403`).
- [ ] `TC-SEC-002` — `PATCH /api/events/{{draftEventCode}}/publish` con `userAToken` (`403`) y con `staffToken` (`403`).
- [ ] `TC-SEC-003` — `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` con `userAToken` (`403`).

## 5. Flujo D — Compra, Propiedad, Cancelación y Uso
- [ ] `E2E-TKT-01` / `TC-BIZ-007` (Ticket 1) — `POST /api/tickets` con `userAToken` → `201 Created` → copiar `ticketCode` en `ticketCancelableCode`.
- [ ] `E2E-TKT-01` / `TC-BIZ-007` (Ticket 2) — `POST /api/tickets` con `userAToken` → `201 Created` → copiar `ticketCode` en `ticketUsableCode`.
- [ ] `E2E-TKT-02` / `TC-BIZ-008` — `GET /api/tickets/{{ticketCancelableCode}}` con `userAToken` → `200 OK`.
- [ ] `E2E-TKT-03` / `TC-BIZ-008` — `GET /api/tickets/by-user?email={{userAEmail}}` con `userAToken` → `200 OK`.
- [ ] `TC-SEC-008` — `POST /api/tickets` enviando `userEmail` de `USER_B` con `userAToken` → `403 Forbidden`.
- [ ] `TC-SEC-009` — `GET /api/tickets/{{ticketCancelableCode}}` con `userBToken` → `403 Forbidden`.
- [ ] `TC-SEC-010` — `GET /api/tickets/by-user?email={{userAEmail}}` con `userBToken` → `403 Forbidden`.
- [ ] `TC-SEC-011` — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` con `userBToken` → `403 Forbidden` (sin mutar estado).
- [ ] `E2E-TKT-05` / `TC-BIZ-010` — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` con `userAToken` → `200 OK` (`status = CANCELLED`).
- [ ] `E2E-TKT-04` / `TC-BIZ-009` — `GET /api/events/{{publishedEventCode}}/tickets/paid` con `staffToken` → `200 OK`.
- [ ] `TC-SEC-013` — `GET /api/events/{{publishedEventCode}}/tickets/paid` con `userAToken` → `403 Forbidden`.
- [ ] `TC-SEC-012` — `PATCH /api/tickets/{{ticketUsableCode}}/use` con `userAToken` → `403 Forbidden`.
- [ ] `E2E-TKT-06` / `TC-BIZ-012` — `PATCH /api/tickets/{{ticketUsableCode}}/use` con `staffToken` → `200 OK` (`status = USED`).

## 6. Flujo E — Regresión de Errores (400, 404, 409)
- [ ] `TC-HTTP-001` — `POST /api/users` con contraseña corta (< 12 caracteres) → `400 Bad Request`.
- [ ] `TC-HTTP-002` — `POST /api/users` con email mal formado → `400 Bad Request`.
- [ ] `TC-HTTP-003` — `POST /api/events` con campos obligatorios vacíos (`adminToken`) → `400 Bad Request`.
- [ ] `TC-HTTP-004` — `GET /api/venues/VENUE-INEXISTENTE-999` → `404 Not Found`.
- [ ] `TC-HTTP-005` — `GET /api/artists/999999` → `404 Not Found`.
- [ ] `TC-BIZ-006` — `POST /api/users` con usuario duplicado (`USER_A`) → `409 Conflict`.
- [ ] `TC-BIZ-011` — `PATCH /api/tickets/{{ticketCancelableCode}}/cancel` por segunda vez → `409 Conflict`.
- [ ] `TC-BIZ-013` — `PATCH /api/tickets/{{ticketUsableCode}}/use` por segunda vez → `409 Conflict`.
- [ ] `TC-BIZ-014` — `POST /api/tickets` para evento en estado `DRAFT` (`{{draftEventCode}}`) → `409 Conflict`.

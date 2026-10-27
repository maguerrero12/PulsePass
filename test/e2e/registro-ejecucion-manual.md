# Registro de Ejecución Manual y Matriz de Cobertura E2E — PulsePass (Fase 5)

## 1. Matriz de Cobertura de las 22 Rutas y Roles (Sección 7 y 25 del PRD)

Política adoptada para recursos ajenos (`expectedForeignResourceStatus`): **`403 Forbidden`** uniforme mediante `AuthorizationService` + `GlobalExceptionHandler`.

| # | Caso Base | Método | Endpoint | Rol Permitido | Rol Denegado / Política |
|---|---|---|---|---|---|
| 1 | `E2E-VEN-01` | `GET` | `/api/venues/{code}` | Público (`ANONYMOUS`) | `404` si código no existe |
| 2 | `E2E-VEN-02` | `GET` | `/api/venues/active` | Público (`ANONYMOUS`) | Solo venues con `active=true` |
| 3 | `E2E-ART-01` | `GET` | `/api/artists/{id}` | Público (`ANONYMOUS`) | `404` si ID no existe |
| 4 | `E2E-ART-02` | `GET` | `/api/artists/by-stage-name` | Público (`ANONYMOUS`) | `404` si nombre no existe |
| 5 | `E2E-ART-03` | `GET` | `/api/artists/active` | Público (`ANONYMOUS`) | Solo artistas con `active=true` |
| 6 | `E2E-USR-01` | `POST` | `/api/users` | Público (`ANONYMOUS`) | Asigna solo `USER`; `400` inválido; `409` duplicado |
| 7 | `E2E-USR-02` | `GET` | `/api/users/by-email` | Propietario (`OWN`) o `ADMIN` | Anónimo `401`; tercero (`USER_B`) `403` |
| 8 | `E2E-USR-03` | `GET` | `/api/users/by-username` | Propietario (`OWN`) o `ADMIN` | Anónimo `401`; tercero (`USER_B`) `403` |
| 9 | `E2E-EVT-01` | `POST` | `/api/events` | `ADMIN` | Anónimo `401`; `USER` / `STAFF` `403` |
| 10 | `E2E-EVT-02` | `GET` | `/api/events/{eventCode}` | Público si `PUBLISHED`; `STAFF`/`ADMIN` si `DRAFT` | Borrador: Anónimo `401`, `USER` `403` |
| 11 | `E2E-EVT-03` | `GET` | `/api/events/published` | Público (`ANONYMOUS`) | Nunca incluye eventos en `DRAFT` |
| 12 | `E2E-EVT-04` | `PATCH` | `/api/events/{eventCode}/publish` | `ADMIN` | Anónimo `401`; `USER` / `STAFF` `403`; estado inválido `409` |
| 13 | `E2E-EVT-05` | `POST` | `/api/events/{eventCode}/artists/{artistId}` | `ADMIN` | Anónimo `401`; `USER` / `STAFF` `403`; duplicado `409` |
| 14 | `E2E-EVT-06` | `GET` | `/api/events/by-artist` | Público (solo `PUBLISHED`); `STAFF`/`ADMIN` (todos) | Filtra borradores para público y `USER` |
| 15 | `E2E-TKT-01` | `POST` | `/api/tickets` | `USER` (`userEmail` propio) o `ADMIN` | Anónimo `401`; email ajeno `403`; regla negocio `409` |
| 16 | `E2E-TKT-02` | `GET` | `/api/tickets/{ticketCode}` | Propietario (`OWN`), `STAFF` o `ADMIN` | Anónimo `401`; tercero (`USER_B`) `403` |
| 17 | `E2E-TKT-03` | `GET` | `/api/tickets/by-user` | Propietario (`OWN`) o `ADMIN` | Anónimo `401`; tercero (`USER_B`) `403` |
| 18 | `E2E-TKT-04` | `GET` | `/api/events/{eventCode}/tickets/paid` | `STAFF` o `ADMIN` | Anónimo `401`; `USER` `403` |
| 19 | `E2E-TKT-05` | `PATCH` | `/api/tickets/{ticketCode}/cancel` | Propietario (`USER`) o `ADMIN` | Anónimo `401`; tercero/`STAFF` `403`; repetir `409` |
| 20 | `E2E-TKT-06` | `PATCH` | `/api/tickets/{ticketCode}/use` | `STAFF` o `ADMIN` | Anónimo `401`; `USER` `403`; repetir/cancelado `409` |
| 21 | `E2E-AUTH-01` | `POST` | `/api/auth/login` | Público (`ANONYMOUS`) | Credenciales inválidas o usuario inactivo `401` |
| 22 | `E2E-AUTH-02` | `GET` | `/api/auth/me` | Autenticado (`USER`, `STAFF`, `ADMIN`) | Sin token, token alterado o inactivo `401` |

---

## 2. Bitácora de Ejecución Manual (Sección 16.2 del PRD)

| Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON / estado observado | Resultado | Evidencia / observación |
|---|---|---|---|---|---|---|---|
| **E2E-VEN-02** | `ANONYMOUS` | `GET /api/venues/active` | 200 | 200 | Lista `VenueResponse` (`VEN-SMR-01`) | PASS | Conectividad y venue activo verificados. |
| **E2E-VEN-01** | `ANONYMOUS` | `GET /api/venues/{code}` | 200 | 200 | `VenueResponse` (`VEN-SMR-01`) | PASS | Consulta por código exitosa. |
| **E2E-ART-03** | `ANONYMOUS` | `GET /api/artists/active` | 200 | 200 | Lista `ArtistResponse` | PASS | Artistas activos listados (`Solar Beat`). |
| **E2E-ART-01** | `ANONYMOUS` | `GET /api/artists/{id}` | 200 | 200 | `ArtistResponse` (`id=1`) | PASS | Consulta por ID exitosa. |
| **E2E-ART-02** | `ANONYMOUS` | `GET /api/artists/by-stage-name` | 200 | 200 | `ArtistResponse` (`Solar Beat`) | PASS | Coincidencia exacta de `stageName`. |
| **E2E-USR-01** | `ANONYMOUS` | `POST /api/users` | 201 | 201 | `UserResponse` (`USER_A` y `USER_B`) | PASS | Creados con rol `USER`; sin exponer hash. |
| **E2E-AUTH-01** | `ANONYMOUS` | `POST /api/auth/login` | 200 | 200 | `AuthResponse` (`Bearer`, `expiresIn=900`) | PASS | Tokens emitidos para `USER_A`, `USER_B`, `ADMIN`, `STAFF`. |
| **E2E-AUTH-02** | `USER_A` / `ADMIN` / `STAFF` | `GET /api/auth/me` | 200 | 200 | `AuthenticatedUserResponse` | PASS | Identidad y `roles` verificados sin secretos. |
| **E2E-USR-02** | `USER_A` | `GET /api/users/by-email` | 200 | 200 | `UserResponse` propio | PASS | Propietario consulta su propio email. |
| **E2E-USR-03** | `USER_A` | `GET /api/users/by-username` | 200 | 200 | `UserResponse` propio | PASS | Propietario consulta su propio username. |
| **E2E-EVT-03** | `ANONYMOUS` | `GET /api/events/published` | 200 | 200 | Lista `EventSummaryResponse` | PASS | Incluye `CMF-2026`; no filtra `DRAFT-01`. |
| **E2E-EVT-02** | `ANONYMOUS` | `GET /api/events/{publishedEventCode}` | 200 | 200 | `EventResponse` (`CMF-2026`) | PASS | Evento publicado accesible sin token. |
| **E2E-EVT-06** | `ANONYMOUS` | `GET /api/events/by-artist` | 200 | 200 | Lista `EventSummaryResponse` | PASS | Devuelve `CMF-2026` y omite `DRAFT-01`. |
| **E2E-EVT-01** | `ADMIN` | `POST /api/events` | 201 | 201 | `EventResponse` (`status=DRAFT`) | PASS | Evento `EVT-E2E-01` creado en borrador. |
| **TC-SEC-015** | `ADMIN` | `GET /api/events/{draftEventCode}` | 200 | 200 | `EventResponse` (`DRAFT-01`) | PASS | `ADMIN` puede consultar evento en `DRAFT`. |
| **E2E-EVT-05** | `ADMIN` | `POST /api/events/{code}/artists/{id}` | 200 | 200 | `EventResponse` con `Solar Beat` | PASS | Asociación artista-evento verificada. |
| **E2E-EVT-04** | `ADMIN` | `PATCH /api/events/{code}/publish` | 200 | 200 | `EventResponse` (`status=PUBLISHED`) | PASS | Evento `PUB-01` publicado correctamente. |
| **E2E-TKT-01** | `USER_A` | `POST /api/tickets` | 201 | 201 | `TicketResponse` (`status=PAID`) | PASS | Creados `ticketCancelableCode` y `ticketUsableCode`. |
| **E2E-TKT-02** | `USER_A` | `GET /api/tickets/{ticketCode}` | 200 | 200 | `TicketResponse` de `USER_A` | PASS | Propietario lee su ticket. |
| **E2E-TKT-03** | `USER_A` | `GET /api/tickets/by-user` | 200 | 200 | Lista `TicketResponse` de `USER_A` | PASS | Historial propio consultado. |
| **E2E-TKT-04** | `STAFF` | `GET /api/events/{code}/tickets/paid` | 200 | 200 | Lista `TicketResponse` (`PAID`) | PASS | `STAFF` consulta tickets pagados del evento. |
| **E2E-TKT-05** | `USER_A` | `PATCH /api/tickets/{code}/cancel` | 200 | 200 | `TicketResponse` (`status=CANCELLED`) | PASS | Ticket cancelado; GET posterior confirma estado. |
| **E2E-TKT-06** | `STAFF` | `PATCH /api/tickets/{code}/use` | 200 | 200 | `TicketResponse` (`status=USED`) | PASS | Ticket usado; GET posterior confirma estado. |
| **TC-AUTH-004** | `ANONYMOUS` | `POST /api/auth/login` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Password erróneo rechazado sin enumeración. |
| **TC-AUTH-005** | `ANONYMOUS` | `POST /api/auth/login` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Usuario inexistente devuelve `401` genérico. |
| **TC-AUTH-007** | `ANONYMOUS` | `GET /api/auth/me` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Ruta protegida sin Bearer devuelve `401`. |
| **TC-AUTH-009** | `ANONYMOUS` | `GET /api/auth/me` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Token con firma alterada rechazado en filtro. |
| **TC-AUTH-013** | `INACTIVE_USER` | `POST /api/auth/login` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Usuario inactivo (`inactivo_e2e`) rechazado. |
| **TC-SEC-001** | `ANONYMOUS` / `USER_A` | `POST /api/events` | 401 / 403 | 401 / 403 | `ErrorResponse` | PASS | Creación de evento restringida a `ADMIN`. |
| **TC-SEC-002** | `USER_A` / `STAFF` | `PATCH /api/events/{code}/publish` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Publicación restringida a `ADMIN`. |
| **TC-SEC-003** | `USER_A` | `POST /api/events/{code}/artists/{id}` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Asociación de artista restringida a `ADMIN`. |
| **TC-SEC-004** | `ANONYMOUS` | `GET /api/events/{draftEventCode}` | 401 | 401 | `ErrorResponse` (`status=401`) | PASS | Borrador protegido frente a visitantes anónimos. |
| **TC-SEC-006** | `USER_B` | `GET /api/users/by-email` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Anti-IDOR en búsqueda por email ajeno. |
| **TC-SEC-007** | `USER_B` | `GET /api/users/by-username` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Anti-IDOR en búsqueda por username ajeno. |
| **TC-SEC-008** | `USER_A` | `POST /api/tickets` (email de B) | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Impide comprar entrada a nombre de tercero. |
| **TC-SEC-009** | `USER_B` | `GET /api/tickets/{ticketCode}` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Anti-IDOR en lectura de ticket ajeno. |
| **TC-SEC-010** | `USER_B` | `GET /api/tickets/by-user` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Anti-IDOR en listado de tickets ajenos. |
| **TC-SEC-011** | `USER_B` | `PATCH /api/tickets/{code}/cancel` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | Anti-IDOR en cancelación; ticket sin cambios. |
| **TC-SEC-012** | `USER_A` | `PATCH /api/tickets/{code}/use` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | `USER` no puede marcar ticket como usado. |
| **TC-SEC-013** | `USER_A` | `GET /api/events/{code}/tickets/paid` | 403 | 403 | `ErrorResponse` (`status=403`) | PASS | `USER` no puede listar tickets pagados del evento. |
| **TC-HTTP-001** | `ANONYMOUS` | `POST /api/users` (password corta) | 400 | 400 | `ErrorResponse` (`status=400`) | PASS | Bean Validation rechaza password < 12 chars. |
| **TC-HTTP-002** | `ANONYMOUS` | `POST /api/users` (email inválido) | 400 | 400 | `ErrorResponse` (`status=400`) | PASS | Bean Validation rechaza email mal formado. |
| **TC-HTTP-003** | `ADMIN` | `POST /api/events` (vacío) | 400 | 400 | `ErrorResponse` (`status=400`) | PASS | Bean Validation rechaza payload incompleto. |
| **TC-HTTP-004** | `ANONYMOUS` | `GET /api/venues/VENUE-INEXISTENTE-999` | 404 | 404 | `ErrorResponse` (`status=404`) | PASS | Venue inexistente retorna `404`. |
| **TC-HTTP-005** | `ANONYMOUS` | `GET /api/artists/999999` | 404 | 404 | `ErrorResponse` (`status=404`) | PASS | Artista inexistente retorna `404`. |
| **TC-BIZ-006** | `ANONYMOUS` | `POST /api/users` (duplicado) | 409 | 409 | `ErrorResponse` (`status=409`) | PASS | Usuario duplicado retorna `409 Conflict`. |
| **TC-BIZ-011** | `USER_A` | `PATCH /api/tickets/{code}/cancel` (2ª vez) | 409 | 409 | `ErrorResponse` (`status=409`) | PASS | Impide doble cancelación. |
| **TC-BIZ-013** | `STAFF` | `PATCH /api/tickets/{code}/use` (2ª vez) | 409 | 409 | `ErrorResponse` (`status=409`) | PASS | Impide doble uso de entrada. |
| **TC-BIZ-014** | `USER_A` | `POST /api/tickets` (evento `DRAFT`) | 409 | 409 | `ErrorResponse` (`status=409`) | PASS | Impide comprar entrada para evento no publicado. |

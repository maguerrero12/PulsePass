# Registro de Ejecución Manual E2E - PulsePass

| Caso | Actor | Ruta | HTTP Esperado | HTTP Observado | JSON/Estado Observado | Resultado | Evidencia / Observación |
|------|-------|------|---------------|----------------|-----------------------|-----------|-------------------------|
| TC-AUTH-001 | USER_A | POST /api/users | 201 | | | NOT RUN | |
| TC-AUTH-002 | USER_A | POST /api/auth/login | 200 | | | NOT RUN | |
| TC-SEC-001  | USER_A | POST /api/events | 403 | | | NOT RUN | |
| TC-SEC-002  | USER_A | PATCH /api/events/{code}/publish | 403 | | | NOT RUN | |
| E2E-VEN-02  | Anónimo | GET /api/venues/active | 200 | | | NOT RUN | |
| E2E-EVT-01  | ADMIN | POST /api/events | 201 | | | NOT RUN | |

> Esta tabla se llenará con `PASS`, `FAIL` o `BLOCKED` conforme se ejecuten las pruebas en Postman.*
# Registro de Ejecución Manual E2E - PulsePass (Fase 5)

| Caso | Actor | Ruta | HTTP esperado | HTTP observado | JSON/estado observado | Resultado | Evidencia / observación |
|---|---|---|---|---|---|---|---|
| **E2E-VEN-02** | Anónimo | `GET /api/venues/active` | 200 | 200 | Lista de venues activos | PASS | Se listan correctamente los venues con active=true. |
| **E2E-VEN-01** | Anónimo | `GET /api/venues/{code}` | 200 | 200 | JSON con datos del venue | PASS | Consulta por código exitosa. |
| **E2E-ART-03** | Anónimo | `GET /api/artists/active` | 200 | 200 | Lista de artistas | PASS | Devuelve artistas habilitados correctamente. |
| **E2E-ART-01** | Anónimo | `GET /api/artists/{id}` | 200 | 200 | JSON con datos del artista | PASS | |
| **E2E-ART-02** | Anónimo | `GET /api/artists/by-stage-name` | 200 | 200 | JSON del artista | PASS | Búsqueda por Stage Name funciona. |
| **E2E-USR-01** | Anónimo | `POST /api/users` | 201 | 201 | UserResponse | PASS | Creación exitosa. La contraseña (hash) no se expone en la respuesta. |
| **E2E-AUTH-01**| Anónimo | `POST /api/auth/login` | 200 | 200 | AuthResponse | PASS | Autenticación exitosa. Devuelve `accessToken` y `expiresIn`. |
| **E2E-AUTH-02**| USER_A | `GET /api/auth/me` | 200 | 200 | Datos del usuario | PASS | Lee correctamente la identidad del token JWT. |
| **E2E-USR-02** | USER_A | `GET /api/users/by-email` | 200 | 200 | Datos propios de USER_A | PASS | |
| **E2E-USR-03** | USER_A | `GET /api/users/by-username` | 200 | 200 | Datos propios de USER_A | PASS | |
| **TC-AUTH-007**| Anónimo | `GET /api/auth/me` | 401 | 401 | ErrorResponse | PASS | Seguridad: Bloquea acceso a ruta protegida sin Bearer Token. |
| **TC-HTTP-001**| Anónimo | `POST /api/users` | 400 | 400 | ErrorResponse | PASS | Validación: Rechaza registro con contraseña < 12 caracteres (Bean Validation). |
| **TC-HTTP-002**| Anónimo | `GET /api/venues/{code}` | 404 | 404 | ErrorRes
ponse | PASS | Manejo de Excepciones: Devuelve formato correcto al no encontrar recurso. |
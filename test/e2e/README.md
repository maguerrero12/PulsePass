# Pruebas Manuales E2E — PulsePass (Fase 5)

Este directorio contiene los entregables de la **Fase 5** para la ejecución manual reproducible de pruebas HTTP end-to-end mediante **Postman**, cumpliendo estrictamente el PRD (`PRD_PulsePass_Pruebas_E2E_Postman_Manual.md`):
- **Sin scripts** (`Pre-request` ni `Post-response/Tests`).
- **Sin captura automática de tokens**.
- **Sin contraseñas ni tokens hardcodeados** en la colección ni en los archivos versionados.

---

## 1. Preparación del Ambiente y Base de Datos
1. Asegúrese de que **PostgreSQL** esté corriendo y que la aplicación **PulsePass** haya aplicado las migraciones de Flyway (`V1` a `V5`).
2. Ejecute el script [`fixtures/seed-e2e.sql`](./fixtures/seed-e2e.sql) en la base de datos conectada a la aplicación.
3. Inicie o reinicie la aplicación Spring Boot en `http://localhost:8080`.

---

## 2. Importación y Configuración de Variables Locales en Postman
1. Abra **Postman** y haga clic en **Import**.
2. Importe la colección: [`postman/PulsePass-Manual-E2E.postman_collection.json`](./postman/PulsePass-Manual-E2E.postman_collection.json).
3. Importe el environment: [`postman/PulsePass-local.postman_environment.json`](./postman/PulsePass-local.postman_environment.json).
4. Seleccione el environment **PulsePass Local** en la esquina superior derecha de Postman.
5. Abra el panel de **Environments → PulsePass Local** y complete únicamente la columna **Current Value** (nunca *Initial Value*, para no exportar secretos) en las variables de tipo `secret`:
   - `userAPassword`: una contraseña de 12+ caracteres para `USER_A` (ej. `ClaveSeguraUsuarioA2026!`)
   - `userBPassword`: una contraseña de 12+ caracteres para `USER_B` (ej. `ClaveSeguraUsuarioB2026!`)
   - `adminIdentifier`: `admin_e2e`
   - `staffIdentifier`: `staff_e2e`
   - `adminPassword`: la contraseña del fixture definida en `seed-e2e.sql` (`password`)
   - `staffPassword`: la contraseña del fixture definida en `seed-e2e.sql` (`password`)

---

## 3. Orden de Ejecución Manual y Copia de Variables
1. **Carpeta `00` y Carpetas `01` y `02` (Flujo A)**:
   - Envíe con **Send** las consultas públicas de Venues y Artists.
2. **Carpeta `03 Auth / usuarios` (Flujo B)**:
   - Ejecute `POST Registrar USER_A` y `POST Registrar USER_B` (`201 Created`).
   - Ejecute los logins (`USER_A`, `USER_B`, `ADMIN`, `STAFF`) y copie manualmente el campo `accessToken` de cada respuesta JSON al **Current Value** de la variable correspondiente:
     - Login `USER_A` → pegar en `userAToken`
     - Login `USER_B` → pegar en `userBToken`
     - Login `ADMIN` → pegar en `adminToken`
     - Login `STAFF` → pegar en `staffToken`
   - Ejecute las peticiones `GET Auth Me` para verificar identidad y roles.
3. **Carpeta `04 Usuarios / consulta y propiedad`**:
   - Ejecute las consultas por email y username para `USER_A` y `ADMIN`.
4. **Carpeta `05 Events / administración y visibilidad` (Flujo A y Flujo C)**:
   - Ejecute las consultas públicas (`GET Eventos Publicados`, `GET Evento Publicado por Código`, `GET Eventos por Artista`) y verifique que `CMF-2026` es visible y `DRAFT-01` no se filtra.
   - Ejecute como `ADMIN`: `POST Crear Evento`, `GET Evento Borrador por Código`, `POST Asociar Artista a Evento` y `PATCH Publicar Evento`.
5. **Carpeta `06 Tickets` y Carpeta `07 Seguridad` (Flujo D)**:
   - Ejecute `POST Comprar Ticket 1` → copie el `ticketCode` devuelto en `ticketCancelableCode`.
   - Ejecute `POST Comprar Ticket 2` → copie el `ticketCode` devuelto en `ticketUsableCode`.
   - Ejecute las consultas de tickets en la carpeta `06` y las pruebas negativas Anti-IDOR de la carpeta `07` (`TC-SEC-009`, `TC-SEC-010`, `TC-SEC-011`).
   - Finalmente, ejecute `PATCH Cancelar Ticket` (`ticketCancelableCode` con `userAToken`) y `PATCH Marcar Ticket como Usado` (`ticketUsableCode` con `staffToken`).
6. **Carpeta `08 Validaciones y errores HTTP` (Flujo E)**:
   - Ejecute los casos de validación `400`, no encontrado `404` y conflicto de negocio `409`.

# Fixtures E2E — PulsePass (Fase 5)

Este directorio contiene el script [`seed-e2e.sql`](./seed-e2e.sql) para provisionar los datos mínimos controlados requeridos por la **Fase 5 (Sección 5.1 del PRD)**.

## Contenido de `seed-e2e.sql`
1. **Venue activo (`venueActiveCode = VEN-SMR-01`)**: `Marina Convention Center` (Santa Marta, capacidad `3`, `active = true`).
2. **Artistas activos (`artistId = 1`, `artistStageName = Solar Beat`)**: `Solar Beat`, `Neon Waves`, `Caribbean Sound`.
3. **Identidades privilegiadas e inactiva (fuera del registro público)**:
   - `admin_e2e` (`admin@example.test`, rol `ADMIN`, `active = true`) — contraseña: `password`
   - `staff_e2e` (`staff@example.test`, rol `STAFF`, `active = true`) — contraseña: `password`
   - `inactivo_e2e` (`inactivo@example.test`, rol `USER`, `active = false`) — contraseña: `password`
4. **Eventos iniciales controlados**:
   - `CMF-2026` (`publishedEventCode`): Estado `PUBLISHED`, fecha futura (`2027-12-15 18:00:00`), asociado a `Solar Beat`.
   - `DRAFT-01` (`draftEventCode`): Estado `DRAFT`, fecha futura, asociado a `Solar Beat` (permite demostrar que los endpoints públicos no filtran borradores).
   - `PUB-01` (`eventToPublishCode`): Estado `DRAFT`, fecha futura, listo para ser publicado con `PATCH /api/events/PUB-01/publish`.

## Cómo ejecutar o restaurar los fixtures
El script [`seed-e2e.sql`](./seed-e2e.sql) es **idempotente** (`ON CONFLICT ... DO UPDATE`). Puede ejecutarse desde la consola de base de datos de IntelliJ, pgAdmin o `psql` antes de cada sesión manual para limpiar ejecuciones previas y dejar el estado listo:

```sql
\i test/e2e/fixtures/seed-e2e.sql
```

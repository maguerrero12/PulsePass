# Checklist de Ejecución Manual E2E - PulsePass

## Flujo A - Descubrimiento Público
- [ x ] GET `/api/venues/active` (Guardar `venueActiveCode`)
- [ x ] GET `/api/venues/{code}` (200 OK)
- [ x ] GET `/api/artists/active` (Guardar `artistId`)
- [ x ] GET `/api/artists/{id}` y `/api/artists/by-stage-name` (200 OK)
- [ x ] GET `/api/events/published` (Asegurar que no salen borradores)
- [ ] GET `/api/events/{eventCode}` (200 OK)
- [ ] GET `/api/events/by-artist` (200 OK)

## Flujo B - Registro y JWT
- [ x ] POST `/api/users` (Registrar `USER_A` y `USER_B` con contraseñas fuertes)
- [ x ] POST `/api/auth/login` (Login de A, B, STAFF y ADMIN)
- [ ] Copiar los 4 `accessToken` a las variables de entorno locales de Postman.
- [ x ] GET `/api/auth/me` (Con cada token, verificar identidad correcta)
- [ x ] Validar errores: Contraseña incorrecta, token inválido y ausencia de token (401).

## Flujo C - Gestión del Catálogo (ADMIN)
- [ ] POST `/api/events` (Crear evento futuro con ADMIN)
- [ ] POST `/api/events/{code}/artists/{id}` (Asociar artista con ADMIN)
- [ ] PATCH `/api/events/{code}/publish` (Publicar evento con ADMIN)
- [ ] GET Verificar que el evento ahora es visible en listados públicos sin Bearer token.
- [ ] Verificar que USER y STAFF reciben 403 al intentar crear/publicar.

## Flujo D - Compra y Propiedad
*[ ] POST `/api/tickets` (USER_A compra ticket, guardar `ticketCode`)
- [ ] GET `/api/tickets/{code}` (USER_A puede verlo, USER_B recibe 403 o 404)
- [ ] POST `/api/tickets` (USER_A intenta comprar mandando el correo de USER_B -> Denegado)
- [ ] PATCH `/api/tickets/{code}/cancel` (USER_B intenta cancelar el ticket de A -> Denegado)
- [ ] PATCH `/api/tickets/{code}/cancel` (USER_A cancela su ticket -> 200 OK)
- [ ] PATCH `/api/tickets/{code}/use` (STAFF marca un ticket válido como usado -> 200 OK)
- [ ] PATCH `/api/tickets/{code}/use` (STAFF intenta usar un ticket cancelado -> Error de negocio)

## Flujo E - Regresión de Errores
- [ x ] Ejecutar validaciones 400 (Bad Request).
- [ x ] Ejecutar recursos ausentes 404 (Not Found).
- [ x ] Verificar formato unificado de `ErrorResponse`.
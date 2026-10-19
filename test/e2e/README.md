# Pruebas Manuales E2E - PulsePass (Fase 5)

Este directorio contiene los entregables de la Fase 5 para la ejecución de pruebas E2E manuales mediante Postman.

## Preparación del Entorno
1. Asegúrese de que la base de datos de pruebas (PostgreSQL) esté corriendo.
2. Aplique las migraciones de Flyway (V1 a V4).
3. Ejecute el script `tests/e2e/fixtures/seed-e2e.sql` en su base de datos para inyectar los usuarios privilegiados (`admin_e2e`, `staff_e2e` e `inactivo_e2e`).
4. Levante la aplicación Spring Boot de PulsePass en `http://localhost:8080`.

## Instrucciones para Postman
1. Abra Postman y seleccione **Import**.
2. Seleccione el archivo de la colección: `postman/PulsePass-Manual-E2E.postman_collection.json`.
3. Seleccione el archivo del entorno: `postman/PulsePass-local.postman_environment.json`.
4. En la parte superior derecha de Postman, seleccione el environment **PulsePass Local**.
5. Las contraseñas de los usuarios de prueba inyectados en SQL son `password`.
6. Al hacer login, copie manualmente el valor de `accessToken` devuelto por la API en la variable correspondiente del environment (`userAToken`, `adminToken`, etc.).


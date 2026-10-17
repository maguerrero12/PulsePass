# PulsePass — Capa de Persistencia

Este repositorio contiene la implementación de la capa de persistencia para **PulsePass**, una plataforma de gestión de eventos, artistas y entradas. El proyecto ha sido desarrollado como caso de estudio académico para aplicar conceptos avanzados de persistencia relacional, control de versiones de bases de datos y pruebas de integración aisladas.

## Tecnologías Utilizadas
* **Lenguaje:** Java 21
* **Framework:** Spring Boot 4.x
* **Persistencia:** Spring Data JPA / Hibernate
* **Base de Datos:** PostgreSQL
* **Control de Esquema:** Flyway
* **Testing:** JUnit 5 / Testcontainers

---

## 1. Modelo de Dominio y Esquema Relacional

El sistema está diseñado con un enfoque estricto en la integridad de los datos. La base de datos es la fuente de verdad y protege las reglas de negocio a través de llaves foráneas (FK), restricciones únicas (UNIQUE) y validaciones de rango (CHECK).

### Entidades Principales y Relaciones
* **Venue (1) -> (N) Event:** Un lugar puede albergar múltiples eventos. Protegido por `capacity > 0` y código único.
* **Event (N) <-> (M) Artist:** Un evento puede tener múltiples artistas y viceversa. Implementado mediante una tabla intermedia `event_artists` con PK compuesta para evitar duplicados.
* **User (1) -> (1) UserProfile:** Relación uno a uno estricta, garantizada mediante un índice `UNIQUE` sobre la FK en el perfil.
* **User (1) -> (N) Ticket** & **Event (1) -> (N) Ticket:** El ticket actúa como una entidad propia (no solo una relación N:M) que almacena precio (validado con `price >= 0`), tipo y estado.

---

## 2. Migraciones y Control de Esquema (Flyway)

La aplicación utiliza **Flyway** como único responsable de definir y evolucionar el esquema de la base de datos. Hibernate está configurado estrictamente en modo validación (`spring.jpa.hibernate.ddl-auto=validate`), impidiendo que el ORM altere la estructura.

Las migraciones se ejecutan secuencialmente al iniciar la aplicación:
1. `V1__create_schema.sql`: Creación de todas las tablas, índices, llaves foráneas y constraints (`UNIQUE`, `CHECK`).
2. `V2__insert_initial_artists.sql`: Poblado del catálogo base de artistas.
3. `V3__add_streaming_url_to_event.sql`: Alteración del esquema para soportar eventos híbridos, agregando la columna `streaming_url` a la tabla `events`.

---

## 3. Estrategia de Consultas

Para optimizar el rendimiento y la legibilidad, se ha dividido la estrategia de acceso a datos en dos enfoques según la complejidad del requisito:

* **Query Methods (Derivadas):** Utilizados para búsquedas simples e indexadas, así como para navegación lineal entre relaciones.
  * *Ejemplo:* `findByStatusOrderByEventDateAsc(EventStatus status)`
  * *Ejemplo:* `findByCode(String code)`
* **JPQL (Java Persistence Query Language):** Utilizado para cruces complejos (JOINs explícitos), filtrados sobre colecciones N:M y agregaciones de negocio.
  * *Ejemplo:* Conteo de ventas confirmadas mediante `@Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.eventCode = :eventCode AND t.status = :status")`
  * *Ejemplo:* Búsqueda por artista en relaciones múltiples (`JOIN e.artists a`).

---

## 4. Ejecución de Pruebas de Integración

La suite de pruebas valida la persistencia, integridad y consultas interactuando contra una instancia real de **PostgreSQL** mediante **Testcontainers**. No se utilizan bases de datos en memoria (como H2) para evitar falsos positivos derivados de diferencias entre dialectos SQL.

### Requisitos Previos
* Tener **Docker Desktop** (o el demonio de Docker) en ejecución en la máquina local.
* Java 21 y Maven instalados.

### Instrucciones de Ejecución
Para ejecutar todas las pruebas de integración y verificar la construcción del esquema:

```bash
mvn clean test

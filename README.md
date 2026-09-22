# backend-developer-as-final-73503-yagavolla

Final Project Assignment - This repository contains the complete final project code and documentation.

## Resource Booking System

Spring Boot REST API for booking rooms, vehicles, and other resources. It uses Java 21, Spring Security, JWT, Spring Data JPA, and PostgreSQL. An H2 database is provided as the convenient local default.

## Run locally

Requirements: Java 17+, Maven 3.9+, and PostgreSQL 14+ (or another JDBC database).

```bash
mvn spring-boot:run
```

The application starts on `http://localhost:8080`. Configure the database connection with these environment variables before starting:

```text
DB_URL=jdbc:postgresql://localhost:5432/booking_db
DB_USERNAME=postgres
DB_PASSWORD=your-password
JWT_SECRET=replace-with-a-random-secret-at-least-32-characters
JWT_EXPIRATION_MS=86400000
SPRING_PROFILES_ACTIVE=dev
SEED_ADMIN_PASSWORD=replace-with-a-strong-admin-password
SEED_USER_PASSWORD=replace-with-a-strong-user-password
```

Tests use an isolated in-memory H2 database and do not require PostgreSQL.

`spring.jpa.hibernate.ddl-auto=update` is intended for local development. Use migrations for production.

## Development seed accounts

When the `dev` profile is active, the application creates these accounts on an empty database using the `SEED_*_PASSWORD` environment variables:

| Role | Username | Password |
| --- | --- | --- |
| ADMIN | `admin` | `Admin@123` |
| USER | `user` | `User@123` |

Seeding is disabled outside the `dev` profile. Never enable that profile in production with the sample credentials.

## API

OpenAPI JSON is available at `/v3/api-docs` and the interactive Swagger UI at `/swagger-ui.html`.

1. `POST /auth/login` with `{"username":"user","password":"User@123"}`.
2. Send the returned token as `Authorization: Bearer <token>`.
3. `GET /api/resources` is available to both roles. Resource POST, PUT, and DELETE require ADMIN.
4. `POST /api/reservations` accepts `resourceId`, `startTime`, and `endTime`. The owner comes from the JWT; price starts from the resource price and status is `PENDING`.
5. `GET /api/reservations` supports `status`, `minPrice`, `maxPrice`, `page`, `size`, and `sort`, for example `?status=PENDING&minPrice=10&page=0&size=20&sort=price,asc`.
6. ADMIN sees all reservations and can use `PUT /api/reservations/{id}/admin` to change resource, time, price, and status. USER results are always limited to their own reservations.

Reservation times cannot overlap for the same active resource. A user may update or delete only their own reservation.

## Test

```bash
mvn test
```

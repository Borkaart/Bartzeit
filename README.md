# Bartzeit Backend

Spring Boot backend for a multi-tenant barbershop SaaS platform. This version is intentionally in-memory only and is designed to keep HTTP contracts stable while future SQL repositories can be introduced without changing controllers or core business rules.

## Prerequisites

- Java 21
- Maven 3.9+
- Docker is not required for the in-memory runtime

## Local execution

```bash
./mvnw clean install
./mvnw spring-boot:run
```

Application base URL:

```text
http://localhost:8080
```

## API base URL

```text
/api/v1
```

Public catalog endpoints also exist under:

```text
/api/public
```

## Security model for this stage

This stage intentionally avoids a real identity provider. The backend uses Spring Security with a restrictive default policy:

- Only the marketplace reads listed below are public
- Other reads and writes require the configured role; unmatched routes are denied
- No production users, default passwords, tokens or identity provider are configured
- Integration tests use mock authentication fixtures, not reusable application credentials
- HTTP Basic is enabled as the current authentication entry point, but there are no configured production accounts
- CSRF protection remains enabled; state-changing requests require a valid CSRF token as well as an authorized identity

## Public routes

- `GET /actuator/health` — health status only; detailed health data are disabled
- `GET /api/public/barbershops`
- `GET /api/public/barbershops/{tenantSlug}`
- `GET /api/public/barbershops/{tenantSlug}/services`
- `GET /api/v1/barbershops/{tenantSlug}/availability`

## Protected routes

- `GET /api/v1/barbershops` and `GET /api/v1/barbershops/{tenantSlug}` — tenant/platform administrator
- `GET /api/v1/barbershops/{tenantSlug}/services/**` — tenant/platform administrator or barber
- `GET /api/v1/barbershops/{tenantSlug}/barbers/**` — tenant/platform administrator or barber
- `GET /api/v1/barbershops/{tenantSlug}/customers/**` — tenant/platform administrator
- `GET /api/v1/barbershops/{tenantSlug}/appointments/**` — tenant/platform administrator or barber
- `GET /api/v1/barbershops/{tenantSlug}/barbers/{barberId}/appointments` — tenant/platform administrator or barber
- `POST /api/v1/barbershops` — tenant/platform administrator
- `POST /api/v1/barbershops/{tenantSlug}/services` — tenant/platform administrator
- `POST /api/v1/barbershops/{tenantSlug}/barbers` — tenant/platform administrator
- `POST /api/v1/barbershops/{tenantSlug}/customers` — tenant/platform administrator
- `POST /api/v1/barbershops/{tenantSlug}/appointments` — tenant/platform administrator
- `PATCH /api/v1/barbershops/{tenantSlug}/barbers/{barberId}/status` — tenant/platform administrator or barber
- `PATCH /api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status` — tenant/platform administrator or barber

While a real identity provider and tenant-bound authorization are not configured, protected functionality is not usable in production and must not be exposed as an operational feature. Do not enable test fixtures or add default credentials to make it accessible.

## Core features implemented

- Multi-tenant barbershops
- Unique tenant slug validation
- Service catalog with validation for duration and price
- Barber registry with active/inactive status
- Customer registry scoped to tenant
- Appointment creation, conflict detection, and availability checks
- Appointment lifecycle status transitions
- Cross-tenant path validation through the route slug
- Business validation at domain/service level, not only HTTP validation

## In-memory storage behavior

- Data is stored with `ConcurrentHashMap` in memory
- Data is lost on application restart
- No database, JPA, Hibernate, Flyway, PostgreSQL or H2 configuration is used in this stage
- Repositories are isolated behind interfaces and memory-backed implementations to enable future SQL migrations without rewriting controller or use-case layers

## Example requests

Protected write examples below require a real identity provider and a role-bound account. This project does not configure production credentials, so these requests are denied until authentication is configured.

Create a barbershop:

```bash
curl -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <credential-from-your-configured-provider>' \
  -X POST http://localhost:8080/api/v1/barbershops \
  -d '{
    "name": "Downtown Barber",
    "slug": "downtown",
    "city": "São Paulo",
    "address": "Main Street 123",
    "phone": "5511999999999"
  }'
```

Create a service:

```bash
curl -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <credential-from-your-configured-provider>' \
  -X POST http://localhost:8080/api/v1/barbershops/downtown/services \
  -d '{
    "name": "Classic Cut",
    "description": "Standard haircut",
    "durationMinutes": 45,
    "price": 65.00
  }'
```

Create a barber:

```bash
curl -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <credential-from-your-configured-provider>' \
  -X POST http://localhost:8080/api/v1/barbershops/downtown/barbers \
  -d '{
    "name": "Alex",
    "specialty": "Fade"
  }'
```

Create an appointment:

```bash
curl -H 'Content-Type: application/json' \
  -H 'Authorization: Basic <credential-from-your-configured-provider>' \
  -X POST http://localhost:8080/api/v1/barbershops/downtown/appointments \
  -d '{
    "serviceId": "<service-id>",
    "barberId": "<barber-id>",
    "customerName": "Maria",
    "startsAt": "2026-11-12T10:00:00",
    "notes": "First haircut"
  }'
```

## Availability and scheduling rules

- Appointments cannot be scheduled in the past
- The requested barber must belong to the same tenant
- Services and barbers cannot be reused across a different tenant in the same request
- Overlap checks are applied to the same barber and time window
- Availability check returns active barbers and candidate slots in a simple daily schedule
- Status transitions are explicit and restricted:
  - CONFIRMED -> IN_PROGRESS, COMPLETED, CANCELLED
  - IN_PROGRESS -> COMPLETED, CANCELLED
  - COMPLETED and CANCELLED are terminal states

## Error handling

The API returns consistent JSON errors with:

- timestamp
- status
- code
- message
- path

Examples of handled scenarios:

- invalid tenant slug
- resource not found
- invalid input payload
- appointment conflict
- access denied
- unauthorized access

## Future SQL replacement plan

The repository layer is already separated behind interfaces so a future SQL implementation can be plugged in without changing controllers, application services or tenant validation rules.

Recommended step:

1. Introduce a `BarbershopRepositorySql` implementation behind the same interface
2. Move in-memory implementations behind a separate profile or package
3. Keep HTTP contracts and validation rules unchanged
4. Keep tenant-scoped checks and business rules in the service layer

## Tests

```bash
./mvnw test
```

The project includes integration tests for:

- barbershop creation and lookup
- invalid input validation
- service and barber creation
- appointment scheduling and conflict prevention
- invalid status transitions
- tenant isolation
- security policy

## OpenAPI documentation

OpenAPI documentation is not enabled in this stage because the Boot 4-compatible SpringDoc artifact was not required by the runtime and would have introduced an unnecessary dependency risk while the in-memory backend was being stabilized. The project keeps the API contract explicit and controller-based, and the application remains ready for a future OpenAPI configuration once a compatible dependency version is confirmed.

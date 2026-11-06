# Bartzeit Backend

Spring Boot 4.1.2 / Java 21 API for a multi-tenant barbershop SaaS MVP. Tenant, user, service, barber, customer, and appointment data are stored in PostgreSQL through JDBC repositories. API responses use DTOs; persistence models are not returned directly.

## Local configuration

Requirements: Java 21 and the Maven wrapper. Copy `.env.example` to `.env` if needed and set the local PostgreSQL connection values. `.env` uses Java properties syntax, is ignored by Git, and must never be copied into logs, source control, or support requests.

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
```

Start from the repository root:

```powershell
.\mvnw.cmd spring-boot:run
```

The Maven run goal activates the `local` profile, which imports `./.env`. Business endpoints require the reviewed database schema described below. Flyway is **disabled by default** so starting the application cannot apply a schema change to the configured Supabase database unintentionally.

## Database schema and migration safety

Review the versioned schema in `src/main/resources/db/migration/V1__create_saas_schema.sql` and the PostgreSQL-only access-control migration in `src/main/resources/db/migration-postgresql/V2__restrict_saas_tables_to_backend.sql`. V1 creates tenants and users, catalog entities, appointments, tenant-aware foreign keys, checks, and indexes. V2 revokes `PUBLIC`, `anon`, and `authenticated` privileges only on Bartzeit's six business tables, enables RLS, and grants the Flyway migration role the CRUD privileges and an allow-all RLS policy required by this trusted backend. V2 does not grant Data API roles access and does not change privileges on unrelated public-schema objects. Neither migration has been applied to Supabase by this project workflow.

The backend must connect using the same dedicated PostgreSQL role that applies V2. That role must own the Bartzeit tables (as when it applies V1) or have the PostgreSQL authority needed to alter them and grant privileges. V2 grants CRUD access and creates the RLS policy for its executing role; if migrations instead run as a separate administrator, explicitly grant equivalent table privileges and create an equivalent policy for the backend role before deployment. Do not use the Supabase `anon` or `authenticated` role for the backend connection. Keep the Supabase service-role key and privileged database credentials out of browsers and client apps. The V2 policy is intentionally for the trusted backend role; application-level tenant authorization remains enforced by the API.

Before applying it to any remote database:

1. Review the complete SQL file and confirm the target Supabase project and schema.
2. Take a backup and test on a separate Supabase staging project first.
3. Obtain explicit approval to alter the target database.
4. From the project root, explicitly enable Flyway for that run:

```powershell
$env:SPRING_FLYWAY_ENABLED = 'true'
.\mvnw.cmd spring-boot:run
Remove-Item Env:SPRING_FLYWAY_ENABLED
```

With the project's local profile active, that command imports the local `.env`, applies pending Flyway migrations at startup, and records them in `flyway_schema_history`. Do not run it against production without the review, backup, staging check, and approval above. After applying, stop the process and start normally with `.\mvnw.cmd spring-boot:run`; Flyway remains disabled by default.

Tests use an isolated in-memory H2 database in PostgreSQL compatibility mode and explicitly run V1 only. V2 uses Supabase/PostgreSQL-specific grants and RLS syntax, so the test Flyway location intentionally excludes it; these tests do not validate V2 against PostgreSQL. They do not activate `local`, read `.env`, connect to Supabase, or modify any remote schema or data. Validate V2 on an isolated PostgreSQL/Supabase staging project before any production application.

## Authentication and tenant isolation

- Onboarding creates a tenant and its first `OWNER` in one transaction.
- Passwords are stored with Spring Security's BCrypt password encoder; raw passwords and hashes are never returned by API DTOs.
- Login creates a server-managed HTTP session; the API does not issue custom JWTs.
- CSRF protection applies to state-changing requests. Get a CSRF token from `GET /api/auth/csrf`, keep the returned session cookie, and send the token in the `X-CSRF-TOKEN` header.
- Session cookies are HttpOnly and SameSite=Lax. The `local` profile disables the Secure cookie flag for local HTTP; non-local deployments use Secure cookies and must be served over HTTPS.
- Roles are `OWNER` and `STAFF`. Only the owner can create staff accounts, services, and barber profiles. Staff can read tenant data, create customers and appointments, and change appointment/barber status.
- Tenant IDs are obtained from the authenticated account, never accepted from request bodies. A tenant slug in a URL must match the signed-in user's tenant; every business repository query also scopes by tenant ID.
- User email addresses are globally unique in this MVP. A barber profile is separate from a login account; linking a staff account to a barber is a future enhancement.
- Appointment start and end values are local wall-clock timestamps (`LocalDateTime` / SQL `TIMESTAMP`); tenant time-zone configuration is not implemented yet.

Only the health check and the CSRF, onboarding, and login endpoints are public. Other API routes require a session.

## API endpoints

| Method | Path | Access / purpose |
|---|---|---|
| `GET` | `/actuator/health` | Public health status; details remain hidden |
| `GET` | `/api/auth/csrf` | Public CSRF token and session bootstrap |
| `POST` | `/api/auth/onboarding` | Public tenant + owner registration |
| `POST` | `/api/auth/login` | Public credential check; creates session |
| `GET` | `/api/auth/me` | Current account and role |
| `POST` | `/api/auth/logout` | Ends the current session; CSRF required |
| `GET` | `/api/v1/barbershops` | Current tenant |
| `GET` | `/api/v1/barbershops/{tenantSlug}` | Current tenant, after slug match |
| `GET`, `POST` | `/api/v1/barbershops/{tenantSlug}/users` | Owner lists or creates staff |
| `GET`, `POST` | `/api/v1/barbershops/{tenantSlug}/services` | Owner manages; owner/staff read |
| `GET` | `/api/v1/barbershops/{tenantSlug}/services/{serviceId}` | Owner/staff |
| `GET`, `POST` | `/api/v1/barbershops/{tenantSlug}/barbers` | Owner manages; owner/staff read |
| `GET` | `/api/v1/barbershops/{tenantSlug}/barbers/{barberId}` | Owner/staff |
| `PATCH` | `/api/v1/barbershops/{tenantSlug}/barbers/{barberId}/status` | Owner/staff |
| `GET`, `POST` | `/api/v1/barbershops/{tenantSlug}/customers` | Owner/staff |
| `GET` | `/api/v1/barbershops/{tenantSlug}/customers/{customerId}` | Owner/staff |
| `GET`, `POST` | `/api/v1/barbershops/{tenantSlug}/appointments` | Owner/staff |
| `GET` | `/api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}` | Owner/staff |
| `GET` | `/api/v1/barbershops/{tenantSlug}/barbers/{barberId}/appointments` | Owner/staff |
| `GET` | `/api/v1/barbershops/{tenantSlug}/availability?date={date}&serviceId={id}` | Owner/staff |
| `PUT` | `/api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}` | Owner/staff; changes booking details |
| `PATCH` | `/api/v1/barbershops/{tenantSlug}/appointments/{appointmentId}/status` | Owner/staff; includes cancellation |

Unknown routes and methods are denied. Cross-tenant IDs return not found rather than exposing whether another tenant owns that resource.

## Example: onboard, login, and create records

Run this in PowerShell after the schema has been reviewed and applied. All values below are fictitious. The same `WebRequestSession` preserves the session cookie; refresh the CSRF token as needed.

```powershell
$base = 'http://localhost:8080'
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

$csrf = Invoke-RestMethod "$base/api/auth/csrf" -WebSession $session
$onboarding = @{
    name = 'Example Barbershop'
    slug = 'example-shop'
    city = 'Example City'
    address = '10 Example Street'
    phone = '555-0100'
    ownerName = 'Example Owner'
    email = 'owner@example.test'
    password = 'Example-Password-58!'
} | ConvertTo-Json
Invoke-RestMethod "$base/api/auth/onboarding" -Method Post -WebSession $session `
    -Headers @{ 'X-CSRF-TOKEN' = $csrf.token } -ContentType 'application/json' -Body $onboarding

$csrf = Invoke-RestMethod "$base/api/auth/csrf" -WebSession $session
$login = @{ email = 'owner@example.test'; password = 'Example-Password-58!' } | ConvertTo-Json
Invoke-RestMethod "$base/api/auth/login" -Method Post -WebSession $session `
    -Headers @{ 'X-CSRF-TOKEN' = $csrf.token } -ContentType 'application/json' -Body $login

Invoke-RestMethod "$base/api/auth/me" -WebSession $session
Invoke-RestMethod "$base/api/v1/barbershops/example-shop/services" -WebSession $session
```

Create a service, professional, customer, and appointment using the same session:

```powershell
$csrf = Invoke-RestMethod "$base/api/auth/csrf" -WebSession $session
$headers = @{ 'X-CSRF-TOKEN' = $csrf.token }
$service = Invoke-RestMethod "$base/api/v1/barbershops/example-shop/services" -Method Post `
    -WebSession $session -Headers $headers -ContentType 'application/json' `
    -Body '{"name":"Example Cut","description":"Standard haircut","durationMinutes":45,"price":65.00}'

$barber = Invoke-RestMethod "$base/api/v1/barbershops/example-shop/barbers" -Method Post `
    -WebSession $session -Headers $headers -ContentType 'application/json' `
    -Body '{"name":"Example Barber","specialty":"Classic cut"}'

$customer = Invoke-RestMethod "$base/api/v1/barbershops/example-shop/customers" -Method Post `
    -WebSession $session -Headers $headers -ContentType 'application/json' `
    -Body '{"name":"Example Customer","email":"customer@example.test","phone":"555-0101"}'

$appointmentBody = @{
    serviceId = $service.id
    barberId = $barber.id
    customerId = $customer.id
    startsAt = (Get-Date).AddDays(2).Date.AddHours(10).ToString('yyyy-MM-ddTHH:mm:ss')
} | ConvertTo-Json
$appointment = Invoke-RestMethod "$base/api/v1/barbershops/example-shop/appointments" -Method Post `
    -WebSession $session -Headers $headers -ContentType 'application/json' -Body $appointmentBody

$csrf = Invoke-RestMethod "$base/api/auth/csrf" -WebSession $session
Invoke-RestMethod "$base/api/v1/barbershops/example-shop/appointments/$($appointment.id)" `
    -Method Put -WebSession $session -Headers @{ 'X-CSRF-TOKEN' = $csrf.token } `
    -ContentType 'application/json' -Body (@{
        serviceId = $service.id
        barberId = $barber.id
        startsAt = (Get-Date).AddDays(2).Date.AddHours(11).ToString('yyyy-MM-ddTHH:mm:ss')
        notes = 'Updated example'
    } | ConvertTo-Json)

$csrf = Invoke-RestMethod "$base/api/auth/csrf" -WebSession $session
Invoke-RestMethod "$base/api/v1/barbershops/example-shop/appointments/$($appointment.id)/status" `
    -Method Patch -WebSession $session -Headers @{ 'X-CSRF-TOKEN' = $csrf.token } `
    -ContentType 'application/json' -Body '{"status":"CANCELLED"}'
```

An appointment can alternatively contain `customerName` instead of `customerId`; that creates a tenant-scoped customer record as part of the appointment transaction. Scheduling rejects past times, inactive or cross-tenant references, and overlapping appointments for the same professional. The application locks the selected professional row while checking and creating an appointment to serialize concurrent bookings.

Check database connectivity and actuator health:

```powershell
curl.exe --silent --show-error --include http://localhost:8080/actuator/health
```

The public health response is `200 {"status":"UP"}` when checks pass, or `503 {"status":"DOWN"}` otherwise. Component details and connection information are hidden.

## Build and tests

```powershell
.\mvnw.cmd test
```

The integration suite validates onboarding, session login, BCrypt hashes, owner/staff authorization, tenant boundaries for reads and cancellation, persistent CRUD flows, and appointment conflict handling. The test database is H2 in PostgreSQL compatibility mode; Flyway applies the same versioned schema to that isolated database only.

## Current MVP boundaries

- Repositories use JDBC and PostgreSQL; there is no JPA/Hibernate layer.
- Business data is persisted, but no payments, notifications, external identity provider, custom JWTs, customer self-service account, or barber-account linking is implemented.
- Customer and service records can be listed and read; update/delete endpoints are not part of this MVP.
- Appointment lifecycle: `CONFIRMED -> IN_PROGRESS | COMPLETED | CANCELLED`, `IN_PROGRESS -> COMPLETED | CANCELLED`; terminal states cannot transition again.
- The frontend still needs to adopt the authenticated session/CSRF flow and tenant-scoped endpoints; the former public marketplace routes are intentionally not exposed.

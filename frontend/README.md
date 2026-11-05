# Bartzeit Frontend

Frontend React + TypeScript + Vite for the Bartzeit Spring Boot API. Only API-backed, public read flows are enabled; the UI does not simulate authentication or claim that a booking has been placed.

## Requirements and setup

- Node.js 20.19+ or 22.12+
- npm
- The backend running at `http://localhost:8080`

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

The Vite server runs at `http://localhost:3000` to match the backend's existing CORS allowlist (`http://localhost:3000`). No backend CORS or security settings were changed.

Set `VITE_API_BASE_URL` in `.env` to the backend origin when it differs from `http://localhost:8080`. `VITE_*` values are public configuration and must not contain secrets.

## Available screens

- `/` — marketplace backed by `GET /api/public/barbershops?page={page}&size=20`. Lists up to 20 results per page and does not offer a location filter because the API only supports slug lookup and pagination.
- `/barbearias/{slug}` — barbershop identity and services backed by `GET /api/public/barbershops/{tenantSlug}` and `GET /api/public/barbershops/{tenantSlug}/services`.
- `/barbearias/{slug}/agendar` — service/date selection and real availability query backed by `GET /api/v1/barbershops/{tenantSlug}/availability?date={yyyy-MM-dd}&serviceId={uuid}`.
- `/cliente`, `/barbeiro`, `/administracao` — explanatory unavailable screens; authentication and tenant-aware authorization are not configured for real users.

The availability screen can show returned barber names and time slots. It does not submit an appointment: `POST /api/v1/barbershops/{tenantSlug}/appointments` is protected and there is no real identity provider. A selected time is not a reservation or confirmation.

## API integration details

All HTTP calls go through `src/api/client.ts`. It applies a ten-second timeout, reads the backend base URL from the environment, parses the backend's `{ timestamp, status, code, message, path }` error format, and handles network and non-JSON failures. No credentials or artificial authorization headers are sent.

## Backend-dependent screens not enabled

- Customer profile/history: requires authenticated customer identity and scoped read endpoints. The existing customer endpoints do not establish an authenticated user's identity.
- Barber agenda/status changes and administrative management: require real authentication and tenant-bound authorization. Although some controller routes exist, the current security configuration does not provide production identity or authorization mapping.
- Appointment creation: the endpoint exists but is protected. The UI does not reuse the development-only hardcoded credentials or pretend an appointment was created.

The backend stores records only in memory. Data may disappear after its process restarts; the frontend reports backend results as returned and does not treat an empty catalog as a client error.

## Run both applications

Terminal 1, workspace root:

```powershell
.\mvnw spring-boot:run
```

Terminal 2:

```powershell
cd frontend
npm run dev
```

## Quality checks

```powershell
npm run lint
npm test
npm run build
```

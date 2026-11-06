# Bartzeit Frontend

<p align="center">
  <a href="#portugues">Português</a>
  &nbsp;|&nbsp;
  <a href="#english">English</a>
</p>

---

<a id="portugues"></a>

## Português

Protótipo de frontend em React, TypeScript e Vite para a API Spring Boot do Bartzeit. As telas de marketplace existentes ainda usam as antigas rotas públicas de catálogo e não implementam o fluxo de login por sessão e CSRF da API SaaS protegida. Portanto, essas telas ainda não são compatíveis com a API atual.

### Requisitos e configuração

- Node.js 20.19+ ou 22.12+
- npm
- Backend Bartzeit em execução em `http://localhost:8080`

No PowerShell:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

O servidor Vite inicia em `http://localhost:3000`, origem incluída na lista CORS do backend. Defina `VITE_API_BASE_URL` no arquivo `.env` se o backend estiver em outro endereço, por exemplo:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

Valores `VITE_*` são incorporados ao frontend e ficam públicos. **Não coloque segredos, senhas ou tokens nessas variáveis.**

O backend atual usa sessão gerenciada pelo servidor e exige token CSRF nas operações de escrita. O frontend ainda não preserva a sessão nem envia o cabeçalho CSRF.

### Telas disponíveis

- `/` e `/barbearias/{slug}` — telas antigas de marketplace que chamam `/api/public/barbershops`. O backend atual não expõe mais o catálogo público; essas chamadas não funcionam com a API protegida.
- `/barbearias/{slug}/agendar` — visualização de disponibilidade criada para a API anterior. O endpoint atual exige uma sessão autenticada no tenant.
- `/cliente`, `/barbeiro` e `/administracao` — telas informativas de funcionalidade indisponível.

A interface ainda não permite cadastrar tenant, fazer login, enviar CSRF, gerenciar usuários ou criar agendamentos. Um horário selecionado **não é uma reserva nem uma confirmação**.

### Integração com a API

Todas as chamadas HTTP passam por `src/api/client.ts`. O cliente:

- usa timeout de dez segundos;
- lê a URL base da API de `VITE_API_BASE_URL`;
- interpreta erros no formato `{ timestamp, status, code, message, path }`;
- trata falhas de rede e respostas que não sejam JSON.

Ele ainda não mantém o cookie de sessão nem envia o cabeçalho CSRF exigido pela API protegida.

### Funcionalidades dependentes do backend ainda não habilitadas

- **Login, cadastro inicial e sessão do tenant:** os endpoints existem no backend, mas não há formulários ou tratamento de sessão/CSRF no frontend.
- **Catálogo e administração do tenant:** as rotas do backend são autenticadas e escopadas por tenant; o frontend ainda chama as rotas públicas descontinuadas.
- **Criação e alteração de agendamentos:** existem endpoints no backend, mas o frontend ainda não envia nem gerencia agendamentos.

O backend persiste os registros de negócio no PostgreSQL. As páginas do frontend precisam ser atualizadas antes de usar a API SaaS atual.

### Validações manuais recentes do backend

Em validação local, foram confirmados:

- aplicação iniciada com o perfil `validacao-local` e escutando somente em `127.0.0.1:8080`;
- conexões JDBC identificadas no banco local `bartzeit_validacao`, com a role `bartzeit_backend`;
- migrations Flyway V1 e V2 aplicadas com sucesso na preparação do banco;
- endpoint de saúde retornando `UP`;
- cadastro fictício de tenant e usuário proprietário retornando HTTP `201`;
- login com CSRF e sessão, seguido de consulta autenticada a `/api/auth/me`, retornando o papel `OWNER` e o tenant esperado.

Essas verificações são **testes manuais do backend em ambiente local**. Elas não significam que o frontend esteja integrado ao login, nem substituem os testes de isolamento entre tenants, privilégios/RLS do PostgreSQL, autorização dos endpoints ou concorrência de agendamentos. Os dados fictícios criados durante a validação permanecem no banco local até serem removidos explicitamente.

### Executar as aplicações

Terminal 1, na raiz do workspace:

```powershell
.\mvnw spring-boot:run
```

Terminal 2:

```powershell
cd frontend
npm run dev
```

Use apenas uma configuração de backend apropriada ao ambiente local. Não aponte a aplicação ou os testes para um banco de produção.

### Verificações de qualidade

Na pasta `frontend`:

```powershell
npm run lint
npm test
npm run build
```

---

<a id="english"></a>

## English

React, TypeScript, and Vite frontend prototype for the Bartzeit Spring Boot API. The existing marketplace screens still use the former public catalog routes and do not implement the secured SaaS API's session-based login and CSRF flow. These screens are therefore not yet compatible with the current API.

### Requirements and setup

- Node.js 20.19+ or 22.12+
- npm
- Bartzeit backend running at `http://localhost:8080`

In PowerShell:

```powershell
cd frontend
Copy-Item .env.example .env
npm install
npm run dev
```

The Vite server starts at `http://localhost:3000`, an origin included in the backend CORS allowlist. Set `VITE_API_BASE_URL` in `.env` if the backend uses a different address, for example:

```dotenv
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_*` values are bundled into the frontend and are public. **Do not put secrets, passwords, or tokens in these variables.**

The current backend uses a server-managed session and requires a CSRF token for write operations. The frontend does not yet preserve the session or send the CSRF header.

### Available screens

- `/` and `/barbearias/{slug}` — legacy marketplace screens that call `/api/public/barbershops`. The current backend no longer exposes the public catalog, so these requests do not work with the secured API.
- `/barbearias/{slug}/agendar` — availability view built for the earlier API. The current endpoint requires an authenticated tenant session.
- `/cliente`, `/barbeiro`, and `/administracao` — explanatory unavailable screens.

The UI does not yet onboard a tenant, log in, send CSRF tokens, manage users, or create appointments. A selected time **is not a reservation or confirmation**.

### API integration

All HTTP calls go through `src/api/client.ts`. The client:

- applies a ten-second timeout;
- reads the API base URL from `VITE_API_BASE_URL`;
- parses errors in the `{ timestamp, status, code, message, path }` format;
- handles network failures and non-JSON responses.

It does not yet preserve the session cookie or send the CSRF header required by the secured API.

### Backend-dependent features not enabled

- **Login, initial onboarding, and tenant sessions:** backend endpoints exist, but the frontend has no forms or session/CSRF handling.
- **Tenant catalog and administration:** backend routes are authenticated and tenant-scoped; the frontend still calls the retired public catalog routes.
- **Appointment creation and changes:** backend endpoints exist, but the frontend does not submit or manage appointments.

The backend persists business records in PostgreSQL. Frontend pages need updating before they can use the current SaaS API.

### Recent manual backend validation

The following were verified during local validation:

- the application started with the `validacao-local` profile and listened only on `127.0.0.1:8080`;
- JDBC connections were identified against the local `bartzeit_validacao` database using the `bartzeit_backend` role;
- Flyway migrations V1 and V2 were successfully applied while preparing the database;
- the health endpoint returned `UP`;
- fictitious tenant and owner-user onboarding returned HTTP `201`;
- CSRF-protected login and a subsequent authenticated `/api/auth/me` request returned the expected `OWNER` role and tenant.

These are **manual backend checks in a local environment**. They do not mean that frontend login is integrated, and they do not replace tests for tenant isolation, PostgreSQL privileges/RLS, endpoint authorization, or appointment concurrency. The fictitious records created during validation remain in the local database until explicitly removed.

### Run both applications

Terminal 1, from the workspace root:

```powershell
.\mvnw spring-boot:run
```

Terminal 2:

```powershell
cd frontend
npm run dev
```

Use a backend configuration appropriate for local development. Do not point the application or tests at a production database.

### Quality checks

From the `frontend` directory:

```powershell
npm run lint
npm test
npm run build
```

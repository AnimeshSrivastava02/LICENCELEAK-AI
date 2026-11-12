# LicenseLeak AI

Fictional creator licensing review demo for the PayPal AI Hackathon.

## Project

The project has a Spring Boot backend in `backend/` and a Next.js frontend in `frontend/`. The backend uses Java 21, Maven Wrapper, PostgreSQL, Spring Data JPA, and Flyway. PostgreSQL is run manually; Docker is not used.

### Local database setup

Use an existing PostgreSQL login and database, or create a separate application role and database. For your verified local setup, the values are database `licenseleak_db` and username `postgres`; keep your actual password only in your terminal or IDE run configuration. Example SQL for a separate setup:

```sql
CREATE ROLE licenseleak WITH LOGIN PASSWORD 'change-me';
CREATE DATABASE licenseleak_ai OWNER licenseleak;
```

Choose your own local password and use the same value in the environment variable below. Do not commit real credentials. Flyway creates demo tables and three fictional sample cases. The $10 USD per-ad daily rule counts calendar days after the contract's final valid date through the date the out-of-contract ad is received, inclusive. Each evidence item is charged separately. The daily rate is configurable.

### Run the backend in PowerShell

Open PowerShell in the repository root, set connection values to match your PostgreSQL setup, then start the app:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/licenseleak_db"
$env:DATABASE_USERNAME = "postgres"
$env:DATABASE_PASSWORD = "<your PostgreSQL password>"
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The first wrapper run downloads the Maven version configured by the project. No separate Maven installation is required. Do not append a trailing backslash to PowerShell assignment lines. In VS Code, open the repository root so it detects the Maven project under `backend/`.

### Case browsing API

- `GET http://localhost:8080/api/v1/cases`
- `GET http://localhost:8080/api/v1/cases/case-demo-001`
- `POST` and `GET http://localhost:8080/api/v1/cases/{caseId}/analysis`
- `POST` and `GET http://localhost:8080/api/v1/cases/{caseId}/decision`
- `POST` and `GET http://localhost:8080/api/v1/cases/{caseId}/invoice`
- `GET http://localhost:8080/api/v1/cases/{caseId}/audit`
- `POST http://localhost:8080/api/v1/webhooks/paypal`

All seeded data is fictional. The default analysis and invoice providers are clearly labeled mocks; no external calls are made until provider modes are switched to `gemini` or `paypal`. AI analysis is assistive and does not make legal findings. Guides: [PayPal Sandbox](docs/PAYPAL_SANDBOX_GUIDE.md) and [Render deployment](docs/RENDER_DEPLOY_GUIDE.md).

### Frontend

Use Node.js 20.9 through 24. From `frontend/`, copy `frontend/.env.example` to `.env.local` in that folder if needed, set `NEXT_PUBLIC_API_BASE_URL`, then run:

```powershell
npm install
npm run dev
```

Open `http://localhost:3000`. The frontend's API base defaults to `http://localhost:8080/api/v1`.

### Tests

Backend tests: from `backend/`, run `./mvnw test` (PowerShell: `./mvnw.cmd test`). Frontend checks from `frontend/`: `npm test`, `npm run typecheck`, `npm run lint`, and `npm run build` after `npm install`.

### Provider settings

Copy the provider variable names from `.env.example` into your terminal or local run configuration. Spring Boot does not automatically load a plain `.env` file. Keep actual keys in local environment settings, not in Git. Use `LICENSELEAK_ANALYSIS_PROVIDER=gemini` and `LICENSELEAK_INVOICE_PROVIDER=paypal` only after configuring the corresponding secrets; defaults are `mock`.

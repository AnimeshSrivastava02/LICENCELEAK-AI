# LicenseLeak AI

Fictional creator licensing review demo for the PayPal AI Hackathon.

## Backend starter

The Spring Boot backend is in `backend/`. It uses Java 21, Maven Wrapper, PostgreSQL, Spring Data JPA, and Flyway. PostgreSQL is run manually; Docker is not used.

### Local database setup

Create a local PostgreSQL role and database. For example, from `psql` as a PostgreSQL administrator:

```sql
CREATE ROLE licenseleak WITH LOGIN PASSWORD 'change-me';
CREATE DATABASE licenseleak_ai OWNER licenseleak;
```

Choose your own local password and use the same value in the environment variable below. Do not commit real credentials. Flyway creates the demo tables and inserts three fictional case summaries on application startup. The contract metadata is placeholder text with no fee rules or amounts.

### Run the backend in PowerShell

Open PowerShell in the repository root, set connection values to match your PostgreSQL setup, then start the app:

```powershell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/licenseleak_ai"
$env:DATABASE_USERNAME = "licenseleak"
$env:DATABASE_PASSWORD = "change-me"
Set-Location backend
.\mvnw.cmd spring-boot:run
```

The first wrapper run downloads the Maven version configured by the project. No separate Maven installation is required. In VS Code, open the repository root so it detects the Maven project under `backend/`.

### Case browsing API

- `GET http://localhost:8080/api/v1/cases`
- `GET http://localhost:8080/api/v1/cases/case-demo-001`

Case detail returns fictional contract metadata and evidence labels. Scenario categories are demo labels, not legal findings. Analysis, fees, approvals, invoices, and audit endpoints have not been implemented in this milestone.

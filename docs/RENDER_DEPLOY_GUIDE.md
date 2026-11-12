# Deploying the demo on Render

This guide sets up a Render PostgreSQL database, a Docker web service for the Spring Boot API, and a Node web service for the Next.js UI. Render also supports managing resources as code with Blueprints; this project uses dashboard setup so you can choose the team account's region and service plan directly.

## 1. Create the database

In Render, create a PostgreSQL database. Keep its internal hostname, port, database name, username, and password in the Render dashboard. The Spring app can build its JDBC URL from these fields. Keep the laptop's local database settings separate.

## 2. Deploy the backend

Create a **Web Service** connected to the repository and choose **Docker** as the runtime. Set the Dockerfile path to `backend/Dockerfile` and the build context to the repository root. The image runs the Spring Boot jar; Flyway applies migrations on startup.

Set these environment variables on the backend service:

| Key | Value |
|---|---|
| `DATABASE_HOST` | Render database internal hostname |
| `DATABASE_PORT` | Render database port |
| `DATABASE_NAME` | Render database name |
| `DATABASE_USERNAME` | Render database username |
| `DATABASE_PASSWORD` | Render database password (secret) |
| `LICENSELEAK_ANALYSIS_PROVIDER` | `mock` for the rehearsed demo; `gemini` when configured |
| `LICENSELEAK_INVOICE_PROVIDER` | `mock` until PayPal Sandbox is configured |
| `LICENSELEAK_FEE_DAILY_RATE` | `10.00` |
| `LICENSELEAK_FEE_CURRENCY` | `USD` |
| `FRONTEND_ORIGIN` | Frontend service's public HTTPS origin, set after step 3 |

Render supplies `PORT`; the app listens on it. Check the service logs for successful Flyway migrations, then open `https://<backend-host>/api/v1/cases`.

## 3. Deploy the frontend

Create another **Web Service** connected to the same repository, using the Node runtime and `frontend` as its root directory. Set:

- Build command: `npm ci && npm run build`
- Start command: `npm run start`
- `NEXT_PUBLIC_API_BASE_URL=https://<backend-host>/api/v1`

After Render gives the frontend its public HTTPS URL, set `FRONTEND_ORIGIN` on the backend to that exact origin (no trailing slash) and redeploy the backend. This enables the browser's API calls through the configured CORS policy.

## 4. Configure providers when ready

Start with both providers set to `mock`; this keeps the demo path deterministic and makes no external calls. To enable Gemini, set `LICENSELEAK_ANALYSIS_PROVIDER=gemini`, add `GEMINI_API_KEY`, and optionally set `GEMINI_MODEL`. To enable PayPal Sandbox, set `LICENSELEAK_INVOICE_PROVIDER=paypal` and add the Sandbox client ID, client secret, recipient email, and webhook ID from [docs/PAYPAL_SANDBOX_GUIDE.md](PAYPAL_SANDBOX_GUIDE.md). Enter secrets in Render's dashboard, never in Git.

The PayPal webhook URL must be the public HTTPS route `https://<backend-host>/api/v1/webhooks/paypal`. Configure this URL and the required invoice events in the PayPal developer dashboard.

## 5. Verify

Open the frontend URL, load a fictional case, run analysis, review the references and quote, record an explicit decision, then create a mock invoice. Only switch to PayPal Sandbox after that flow works. Render's database and web service commands follow its [Blueprint and deployment documentation](https://render.com/docs/blueprint-spec) and [web service setup](https://render.com/docs/web-services).

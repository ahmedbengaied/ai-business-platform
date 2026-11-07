# Deploying the AI Business Platform to Render

This guide deploys the complete application using Render. Render runs the Angular
frontend, the Spring Boot API, and a managed PostgreSQL database as separate
services. The database connection stays on Render's private network.

## What is being deployed

| Component | Technology | Render resource | Purpose |
| --- | --- | --- | --- |
| User interface | Angular 21, TypeScript, RxJS | Static site | Builds optimized browser files and serves them over HTTPS. |
| REST API | Java 21, Spring Boot 4, Spring Security, JPA | Docker web service | Handles authentication, business rules, and database access. |
| Database | PostgreSQL 16, Flyway | Managed PostgreSQL | Persists accounts, firms, clients, invoices, and workflow data; Flyway applies schema migrations. |
| Container build | Docker, Maven | API image | Packages the API and its Java runtime reproducibly. |
| Deployment definition | Render Blueprint (`render.yaml`) | Render | Creates and connects the services from one configuration file. |

Render supplies HTTPS for its public frontend and API addresses. The API accepts
browser requests only from the frontend origin configured in
`CORS_ALLOWED_ORIGINS`. The build writes the API host into the Angular
`runtime-config.ts` module; local Angular development continues to use the
`/api` proxy.

## Before you start

1. Put the complete repository, including `render.yaml`, `backend/`, and
   `frontend/`, in a GitHub repository that Render can access.
2. Create a Render account and connect that GitHub repository.
3. Expect recurring charges for the API service and managed PostgreSQL plan.
   The frontend static site may have a free tier; check Render's current prices
   and database backup/retention options before choosing a plan. Do not use an
   expiring free database for business records.

## Deploy

1. In Render, choose **New → Blueprint**, select the GitHub repository, and
   deploy the Blueprint from `render.yaml`.
2. Render builds the API Docker image, provisions PostgreSQL, runs Flyway
   migrations, and builds the Angular site. The API's JWT secret is generated
   by Render; do not replace it with a checked-in value.
3. Wait until both the API and frontend are deployed. Open the API service's
   **Environment** page and set `CORS_ALLOWED_ORIGINS` to the frontend's exact
   origin, for example `https://ai-business-platform-frontend.onrender.com`.
   Include the scheme, but no trailing slash, path, wildcard, or API suffix.
   Save the change and let Render redeploy the API.
4. Open the API health URL, `https://<api-host>/actuator/health`. It should
   return a healthy status. Then open the frontend URL and register the first
   Owner account.
5. Test logging in, creating a client, and creating an invoice. Confirm the
   records remain after a redeploy. Verify the database backup policy and test
   restoring a backup before storing important records.

The managed database credentials are injected into the API from the database
resource; they are not included in the repository. Do not publish `.env` files,
JWT secrets, database passwords, or API keys.

## Optional OpenAI task execution

The deployment intentionally uses the `mock` AI provider and does not need an
OpenAI key. To enable OpenAI execution, add `OPENAI_API_KEY` as a secret in the
API service's Render Environment settings, set `AI_PROVIDER` to `openai`, and
review `AI_MONTHLY_BUDGET_USD` and the token prices for the selected model.
Provider usage can incur charges independently of Render hosting.

## Important production limitations

This deployment makes the current application publicly reachable; it does not
by itself make the MVP suitable for regulated or sensitive accounting data.
Review the applicable privacy, tax, and accounting requirements before inviting
real clients. This release tracks invoices, payments, bills, and onboarding
statuses, but it does not process payments, produce statutory filings, or
upload/store client documents.

The current UI keeps JWTs in browser `sessionStorage`. Before broad public use,
review the threat model and add appropriate protections such as rate limiting,
account recovery and email verification, stronger account protection, and
operational monitoring. New registrations create a firm Owner account, so
review the desired public-signup policy before sharing the URL widely.

Git-workspace endpoints need actual Git repositories present under
`WORKSPACE_REPOSITORIES_ROOT`. Although the API container includes Git, its
filesystem is ephemeral in this Blueprint; repositories and worktrees will not
survive an instance replacement. Do not rely on those endpoints in production
until a persistent disk and a controlled repository provisioning process have
been designed and configured.

For ongoing operation, keep production secrets only in Render, use a custom
domain only after setting the exact CORS origin again, monitor service and
database health, and periodically rehearse database restoration.

# AI Business Platform

Spring Boot backend and Angular dashboard for an AI-assisted software delivery platform. The long-term goal is to coordinate Product, Architect, Project Manager, Developer, QA, Security, and DevOps agents through auditable workflows.

## Current Release

The repository currently includes:

- JWT-based registration, login, and protected user profile endpoint
- Spring Security with stateless authentication
- JPA persistence with PostgreSQL
- Request validation and Actuator health/info endpoints
- Docker Compose configuration for local PostgreSQL
- H2-backed integration tests
- Owner-scoped project, workflow, and agent-task APIs
- Firm-scoped accounting client onboarding with standard and client-specific document checklists
- Firm-owned reusable onboarding checklist templates that can be tailored per client
- Shared accounting-firm workspaces with Owner, Admin, Staff, and read-only Viewer access
- Secure, single-use team invitation links with seven-day expiry
- Firm-scoped sales customers, invoices, and payment tracking
- Firm-scoped supplier bill and expense tracking
- A mock executor for local/test use and an optional OpenAI task executor
- An Angular dashboard for accounts, client onboarding, sales invoices, workflows, task approvals, execution, run history, and AI budget reporting

The mock executor is selected by default. The OpenAI integration can be selected with configuration; it sends the task role, title, description, and completed prerequisite results to the configured OpenAI API. OpenAI results use a strict JSON shape with a `summary` and `next_steps` array; the application validates and stores that JSON in the task result. It does not execute generated code or perform external actions.

## Technology

- Java 21+
- Spring Boot 4.1.1
- Spring Security
- Spring Data JPA
- PostgreSQL 16
- Maven
- Docker Compose

## Run Locally

Start PostgreSQL:

```powershell
docker compose up -d postgres
```

Run the API:

```powershell
.\mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`.

Run the Angular dashboard in a second PowerShell window:

```powershell
cd frontend
npm install
npm start
```

Open `http://localhost:4200`. The Angular development server proxies `/api` requests to the backend at `http://localhost:8080`. The dashboard stores the JWT in browser `sessionStorage`; signing out removes it, and it is not persisted across browser sessions.

New registrations create an accounting firm and become its Owner. To join a firm, register from a valid invitation link; the invited email address must match the registration email. A user belongs to one firm in this release. Existing active accounts cannot join another firm's team, and invitation links should be shared privately.

Run tests without PostgreSQL:

```powershell
.\mvnw.cmd test
```

## API

### Accounting client onboarding

The authenticated client-onboarding API is scoped to the signed-in accounting firm account:

| Method | Endpoint | Purpose |
| --- | --- | --- |
| POST | `/api/v1/clients` | Create a client and its starter checklist |
| GET | `/api/v1/clients` | List the signed-in firm's clients and checklist status |
| GET | `/api/v1/clients/{clientId}` | Get a firm's client onboarding details |
| POST | `/api/v1/clients/{clientId}/documents` | Add a client-specific document requirement |
| PATCH | `/api/v1/clients/{clientId}/documents/{documentId}` | Update a requirement's review status |
| GET | `/api/v1/onboarding-templates` | List the signed-in firm's reusable templates |
| POST | `/api/v1/onboarding-templates` | Create a template |
| GET | `/api/v1/onboarding-templates/{templateId}` | Get a firm template |
| PUT | `/api/v1/onboarding-templates/{templateId}` | Replace a template's name and checklist |
| DELETE | `/api/v1/onboarding-templates/{templateId}` | Delete an owned template |

Client creation accepts `legalName`, `contactName`, optional `contactEmail` and `contactPhone`, and an optional firm `templateId`. Without a template, it adds starter requests for a business registration certificate, tax identification document, proof of business address, director identification, and previous financial statements.

Template creation and replacement accept a `name` and one to 50 unique `documentNames`. A client gets a snapshot of the selected template's requirements at creation time; later template edits or deletion do not alter existing client checklists. Firms can also add further requirements for individual clients.

Document statuses are `REQUESTED`, `RECEIVED`, `APPROVED`, and `REJECTED`. Onboarding is `NOT_STARTED` while every item is requested, `COMPLETE` when every item is approved, and otherwise `IN_PROGRESS`. This initial release tracks checklist statuses only; it does not upload or store client documents.

### Firm teams and roles

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/v1/firm/team` | List active firm members and pending invitations |
| POST | `/api/v1/firm/invitations` | Create an invitation link for an email and role |
| GET | `/api/v1/firm/invitations/preview?token={token}` | Preview a valid invitation before registration |
| DELETE | `/api/v1/firm/invitations/{invitationId}` | Revoke a pending invitation |
| PATCH | `/api/v1/firm/members/{memberId}` | Change a member's firm role |
| DELETE | `/api/v1/firm/members/{memberId}` | Remove a member and disable their account |

Owners and Admins can manage members. Only Owners can assign the Admin role; neither role can assign or transfer ownership. Staff can create and update shared client onboarding records, templates, customers, invoices, supplier bills, and payment records. Viewers can read shared firm records but cannot make changes, create projects, or execute workflows. Project and workflow records remain private to their creator; firm teams share accounting clients, onboarding templates, sales customers, invoices, and supplier bills.

Invitation tokens are generated with cryptographic randomness, stored only as SHA-256 hashes, and accepted once during registration. Each link expires after seven days. Removing a member disables sign-in and invalidates their existing JWTs; inviting that same email again re-enables it after the new link is accepted. Existing database users are migrated into separate firms so their current clients and templates remain private and intact.

### Sales customers and invoices

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/v1/sales/customers` | List the signed-in firm's sales customers |
| POST | `/api/v1/sales/customers` | Create a sales customer |
| GET | `/api/v1/sales/invoices` | List the signed-in firm's invoices and payment history |
| POST | `/api/v1/sales/invoices` | Issue an invoice for a firm customer |
| POST | `/api/v1/sales/invoices/{invoiceId}/payments` | Record a payment against an invoice |

Invoices include a unique firm-level invoice number, one description, issue and due dates, currency code, subtotal, and a configurable tax percentage. Tax is calculated and rounded to two decimal places. Payments are recorded with an amount, date, and optional note; a payment cannot exceed the remaining invoice balance. `ISSUED`, `PARTIALLY_PAID`, `PAID`, and `OVERDUE` statuses are calculated from payments and the due date. Owners, Admins, and Staff can create customers and invoices and record payments; Viewers can read them only.

This initial sales release tracks invoice and payment information only. It does not generate or send invoice documents, process payments, or post entries to a general ledger. Firms should confirm tax treatment and currency requirements for their jurisdiction before using invoice totals for statutory accounting.

### Supplier bills and expenses

| Method | Endpoint | Purpose |
| --- | --- | --- |
| GET | `/api/v1/purchases/bills` | List the signed-in firm's supplier bills and payment history |
| POST | `/api/v1/purchases/bills` | Record a categorized supplier bill |
| POST | `/api/v1/purchases/bills/{billId}/payments` | Record a payment against a supplier bill |

A bill includes the supplier name and bill number, category, description, issue and due dates, currency code, subtotal, and configurable tax percentage. Tax is calculated and rounded to two decimal places. Payments cannot exceed the remaining balance. `ISSUED`, `PARTIALLY_PAID`, `PAID`, and `OVERDUE` statuses are calculated from payments and the due date. Owners, Admins, and Staff can record bills and payments; Viewers have read-only access. This MVP stores no bill attachments and does not post expenses to a general ledger or calculate tax filings.

### Projects and workflows

| Method | Endpoint | Authentication |
| --- | --- | --- |
| POST | `/api/v1/projects` | Bearer token |
| GET | `/api/v1/projects` | Bearer token |
| GET | `/api/v1/projects/{projectId}` | Bearer token |
| POST | `/api/v1/projects/{projectId}/workflows` | Bearer token |
| GET | `/api/v1/projects/{projectId}/workflows` | Bearer token |
| PATCH | `/api/v1/projects/{projectId}/workflows/{workflowId}/status` | Bearer token |
| POST | `/api/v1/projects/{projectId}/workflows/{workflowId}/tasks` | Bearer token |
| GET | `/api/v1/projects/{projectId}/workflows/{workflowId}/tasks` | Bearer token |
| PATCH | `/api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/status` | Bearer token |

Projects are private to their owner; attempts to access another user's project return `404`. Task `agentRole` values are `PRODUCT`, `ARCHITECT`, `PROJECT_MANAGER`, `DEVELOPER`, `QA`, `SECURITY`, and `DEVOPS`. Task statuses are `TODO`, `IN_PROGRESS`, `DONE`, and `BLOCKED`; workflow statuses are `PLANNED`, `RUNNING`, `COMPLETED`, and `FAILED`.

Task execution is available at `POST /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/execute`. It is synchronous and uses the configured executor: mock mode is the default, while OpenAI can be enabled through configuration. A successful execution marks the task `DONE`.

Transient HTTP 5xx/429 responses and network errors are retried up to three total attempts by default (the initial attempt plus two retries). The limit can be changed with `TASK_MAX_EXECUTION_ATTEMPTS`. Application budget rejections and other non-retryable HTTP errors are not retried. When attempts are exhausted, the task becomes `BLOCKED`, and its response includes `executionAttempts` and `lastExecutionFailure` (the exception type, not its potentially sensitive message). Workflow execution then records a failed run and marks the workflow `FAILED`. Attempts are retained and are not automatically reset.

Task dependencies can be added while a workflow is `PLANNED` using `POST /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/dependencies` with `{"prerequisiteTaskId":"..."}`. Both tasks must belong to the same workflow, and dependencies cannot form cycles. Single-task execution rejects tasks whose prerequisites are not `DONE`; workflow execution orders tasks so prerequisites run first. Task responses include `dependsOnTaskIds`.

Completed task results are also exposed as a structured `artifact` containing a `summary` and `nextSteps`. OpenAI tasks use their validated structured output; mock and older plain-text results are represented as a summary with no next steps. Dependent OpenAI tasks receive the prerequisite artifact summary and suggested next steps as reference context, while the original `result` remains available for compatibility.

Every task requires human approval before it can execute. New tasks start with `approvalStatus: PENDING`; approve or reject them with `PATCH /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/approval` and a body such as `{"status":"APPROVED","note":"Reviewed"}`. Decisions are `APPROVED` or `REJECTED`; workflow execution requires all tasks to be approved. The response records the reviewer, review time, and optional note.

Workflow execution is available at `POST /api/v1/projects/{projectId}/workflows/{workflowId}/execute`. It runs `TODO` tasks in dependency order, using creation order when multiple tasks are ready, and marks the workflow `COMPLETED`; existing `DONE` tasks are left unchanged. A workflow must be `PLANNED`, and it cannot run if any task is `IN_PROGRESS` or `BLOCKED`.

Run history is available at `GET /api/v1/projects/{projectId}/workflows/{workflowId}/runs`. Each run records its initiator, status, start and completion timestamps, completed-task count, and (if execution fails) the failure type. Run records are kept even if task execution fails.

### Git workspaces

To enable a workspace for a project, set `repositoryPath` when creating it to a path relative to `WORKSPACE_REPOSITORIES_ROOT`, for example `{"name":"My project","repositoryPath":"my-repository"}` when the repository is at `./repositories/my-repository`. The path must point to the root of an existing local Git repository; absolute paths, paths outside the configured root, and symlinks that escape it are rejected.

For an approved `TODO` task in a `PLANNED` workflow, create its isolated Git worktree with `POST /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/workspace`. `GET` on the same endpoint returns its location and branch. Each task gets a deterministic `agent/task-{taskId}` branch and a separate directory under `WORKSPACE_WORKTREES_ROOT`. Repeated creation is idempotent. `DELETE` removes a clean worktree but leaves its branch intact; it refuses to remove worktrees with modified, untracked, or ignored files. This first version only manages Git worktrees—it does not run agent-generated commands or code.

Inspect a task worktree with `GET /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/workspace/qa`. This read-only report returns the branch, current commit, whether the worktree is clean, and staged/unstaged/untracked paths with Git status codes. It does not run builds, tests, scanners, or agent-generated commands.

Run a local heuristic secret scan with `GET /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/workspace/security`. It checks text files for common private-key headers, credential assignments, GitHub tokens, and AWS access-key IDs. Findings include only a rule ID, severity, file path, and line number—the matched value is never returned. Binary files and `.git` internals are skipped; files over 1 MiB are skipped and make `complete` false, and scans are capped at 5,000 files. This is a best-effort secret check, not a full vulnerability scanner, and a clean report does not prove the code is secure. The scan is local and read-only; it executes no commands and sends no code to external services.

Inspect basic DevOps readiness with `GET /api/v1/projects/{projectId}/workflows/{workflowId}/tasks/{taskId}/workspace/devops`. The report inventories common container files, GitHub Actions workflows, deployment manifests, and example production configuration files; it suggests categories that were not found. This is a file-presence checklist only: it does not parse or validate those files, build containers, run CI, or deploy anything.

### AI usage and budget

`GET /api/v1/ai/usage` returns the authenticated user's current UTC calendar-month AI usage: prompt and completion tokens, estimated USD spent, the configured monthly limit, and the remaining budget. Usage is owner-scoped.

OpenAI requests are checked against the user's remaining monthly budget before being sent. The default cap is `$5.00` per user per month. Actual provider-reported token counts are stored per task; cost is estimated from the configured per-million-token rates. A request that would exceed the remaining budget is rejected without calling the provider. The mock executor reports zero AI usage.

### Authentication and health

| Method | Endpoint | Authentication |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | Public |
| POST | `/api/v1/auth/login` | Public |
| GET | `/api/v1/auth/me` | Bearer JWT |
| GET | `/actuator/health` | Public |
| GET | `/actuator/info` | Public |

Example registration request:

```json
{
  "email": "user@example.com",
  "password": "secure-password",
  "firstName": "Ada",
  "lastName": "Lovelace"
}
```

## Configuration

The development profile reads these environment variables:

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/ai_business_platform` |
| `DB_USERNAME` | `ai_business` |
| `DB_PASSWORD` | `ai_business` |
| `JWT_SECRET` | Development-only fallback value |
| `JWT_EXPIRATION` | `3600` seconds |
| `AI_PROVIDER` | `mock` (`openai` to use OpenAI) |
| `OPENAI_API_KEY` | Required when `AI_PROVIDER=openai` |
| `OPENAI_MODEL` | `gpt-4o-mini` |
| `OPENAI_BASE_URL` | `https://api.openai.com/v1` |
| `AI_MONTHLY_BUDGET_USD` | `5.00` |
| `OPENAI_INPUT_USD_PER_MILLION_TOKENS` | `0.15` |
| `OPENAI_OUTPUT_USD_PER_MILLION_TOKENS` | `0.60` |
| `TASK_MAX_EXECUTION_ATTEMPTS` | `3` total attempts |
| `WORKSPACE_REPOSITORIES_ROOT` | `./repositories` |
| `WORKSPACE_WORKTREES_ROOT` | `./worktrees` |

Set a strong `JWT_SECRET` before deploying anywhere beyond local development.
To enable OpenAI, set `AI_PROVIDER=openai` and provide `OPENAI_API_KEY` through the environment; never commit the key. Task details are sent to OpenAI only when an approved task is executed. For OpenAI tasks, `result` is a JSON string such as `{"summary":"...","next_steps":["..."]}`.
The token price defaults are estimates for the configured model, not a live price lookup; verify and update them to match the provider's current prices. Changing them affects the application's cost estimates and budget enforcement.

## Roadmap

1. Add firm teams and role-based access so multiple staff members can work within one firm's tenant.
2. Design secure private document uploads, retention, and audit controls before storing client files.
3. Expand into commercial workflows such as sales follow-up, quote approval, and customer handoff.
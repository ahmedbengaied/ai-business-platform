# AI Business Platform

Spring Boot backend for an AI-assisted software delivery platform. The long-term goal is to coordinate Product, Architect, Project Manager, Developer, QA, Security, and DevOps agents through auditable workflows.

## Current Release

This repository currently contains the backend foundation:

- JWT-based registration, login, and protected user profile endpoint
- Spring Security with stateless authentication
- JPA persistence with PostgreSQL
- Request validation and Actuator health/info endpoints
- Docker Compose configuration for local PostgreSQL
- H2-backed integration tests

The agent orchestration layer and Angular dashboard are planned next and are intentionally not represented as completed features.

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

Run tests without PostgreSQL:

```powershell
.\mvnw.cmd test
```

## API

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

Set a strong `JWT_SECRET` before deploying anywhere beyond local development.

## Roadmap

1. Add project, workflow, task, and agent domain models.
2. Add an orchestrator that passes structured artifacts between agents.
3. Add human approval gates, audit logs, and retry limits.
4. Add sandboxed Git workspaces for backend and frontend agents.
5. Add QA, security scanning, and DevOps workflows.
6. Add an Angular dashboard for projects and workflow progress.
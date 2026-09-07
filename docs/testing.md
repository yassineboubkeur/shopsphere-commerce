# 18.9 - Testing Guide

This document describes how to run the automated tests of ShopSphere, what is
covered and the recommended manual test flow.

## Test types and tools

| Layer | Tool | Runs in CI | Coverage |
|-------|------|------------|----------|
| Backend unit tests | JUnit 5 + Mockito (Surefire) | yes | Controllers/`@ControllerAdvice` (mock service), validation errors, services (mock repos) |
| Backend integration | JUnit 5 + MockMvc + Testcontainers (Maven Failsafe, phase `verify`) | yes | Lifecycle init, full controller -> service -> repository flows against a real PostgreSQL container |
| Frontend unit | Vitest + jsdom (`ng test`) | yes | Angular components (initial render, forms, lifecycle) |

## Backend

Each microservice module already contains a `src/test/java` tree. Run tests for
one service:

```bash
cd backend/product-service/product-service

# unit tests only
./mvnw test

# unit + integration tests (Testcontainers need Docker)
./mvnw verify
```

Run every service's tests from the repo root via the CI script:

```powershell
# Windows PowerShell
Get-ChildItem backend -Directory | ForEach-Object {
    $svc = Join-Path $_.FullName (Split-Path -Leaf $_.FullName)
    if (Test-Path (Join-Path $svc 'mvnw.cmd')) {
        Push-Location $svc
        .\mvnw.cmd test
        Pop-Location
    }
}
```

> The CI pipeline runs `mvn test` and `mvn verify` (integration) for all nine
> services, then `npm test` for the frontend. Tests run with sources/resource
> filters so multi-module subpaths are handled correctly.

## Frontend

```bash
cd frontend
npm install
npm test            # single run (used by CI)
# or interactive watch mode:
npm run watch       # builds in watch mode
npx ng test
```

Vitest runs in a jsdom environment; component specs assert initial render,
reactive form validation and simple interaction. Example:

```bash
npm test -- --watch=false
```

## Integration tests with Testcontainers

Integration specs (suffixed `*IT`) start a real PostgreSQL container and boot
the Spring context:

```bash
cd backend/order-service/order-service
./mvnw verify -Dtest=none -Dfailsafe*runs=true
```

They verify end-to-end flows such as: user registration -> login -> JWT issuance;
catalog CRUD; order placement and status transitions; payment completion and
refund; notification persistence; analytics aggregation.

## Manual end-to-end test flow

1. **Start the stack**: `docker compose up --build -d` and wait for all healthy.
2. **Admin setup**: log in as `admin@shopsphere.com` / `Zephyr!91Kite`.
3. **Catalog**: create 2+ categories and 3+ products via the admin UI
   (set prices, stock, `active`).
4. **Guest browsing**: verify product list, category filter, search, price filter.
5. **Registration**: register a new user with a policy-compliant password
   (e.g. `StrongPass@1`) and confirm you are automatically logged in.
6. **Cart**: add/update/remove items; verify quantities and totals.
7. **Order**: run checkout, confirm order total, verify stock decremented.
8. **Payment**: complete payment -> order becomes CONFIRMED and a notification
   is created; test the forced-failure path -> order cancelled, notification.
9. **Notifications**: open the notifications view for the user.
10. **Analytics** (admin): total sales / orders / best-selling reflect the test data.
11. **Monitoring**: open Prometheus (`:9090`) target page - all 9 service
    targets `UP`; open Grafana (`:3000`) - `shopsphere-overview` panels populate.

## Where to see test results

- Locally: `backend/<service>/<service>/target/surefire-reports/` and
  `target/failsafe-reports/` (HTML + XML summaries).
- CI: the `test` job of `.github/workflows/ci.yml` (`mvn test` + `mvn verify`
  + `npm test`); results are attached as build logs on each run.
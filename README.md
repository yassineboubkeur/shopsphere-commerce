# ShopSphere - E-Commerce Platform

A complete microservices-based e-commerce platform built with Spring Boot (Java 26) on the backend and Angular on the frontend, with event-driven messaging (Kafka), caching (Redis), a full observability stack (Prometheus, Grafana, Loki), and an automated multi-architecture CI/CD pipeline that deploys to a cloud ARM Virtual Machine via Docker Compose.

## Features

- **User experience**: registration, login (JWT), product catalog, categories, search and price/stock filtering
- **Shopping**: persistent carts per user, stock validation, checkout with shipping details
- **Orders**: order placement, status lifecycle (PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED), payment completion
- **Payments**: process / forced success / forced failure endpoints, refunds, event-driven notifications
- **Administration**: admin role with user management, product/category management, order management, dashboards
- **Notifications**: user notifications for order confirmation, shipping, payment confirmation
- **Analytics**: total sales, total orders, best-selling products, sales over time, orders by status
- **Event-driven integration**: Apache Kafka topics linking order, inventory, notification and analytics domains
- **Observability**: Spring Boot Actuator + Micrometer Prometheus metrics, structured (Logstash) JSON logging, Prometheus, Grafana, Loki and Promtail
- **Reliability**: service discovery/registry, API gateway with JWT validation, per-container health checks, Docker ready

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 26, Spring Boot 4.0.7, Spring Web MVC / WebFlux, Spring Security, Spring Data JPA |
| Frontend | Angular 22, TypeScript, RxJS, SCSS |
| Database | PostgreSQL 16 (one database per service) |
| Cache | Redis 7 (product Redis caching) |
| Messaging | Apache Kafka 7.6 + Zookeeper (Confluent images) |
| Containerization | Docker, Docker Compose v2 |
| Service Discovery | Custom Eureka-like registry (`service-discovery`) |
| Observability | Micrometer, Prometheus, Grafana, Loki, Promtail |
| CI/CD | GitHub Actions (build, test, multi-arch images, auto-deploy) |
| Registry | GitHub Container Registry (GHCR), linux/amd64 + linux/arm64 |

## Project Structure

```
shopsphere-commerce/
├── backend/                     # 9 Spring Boot microservices
│   ├── service-discovery/       # Eureka-style registry  (8761)
│   ├── api-gateway/             # Reactive gateway + JWT  (8080)
│   ├── auth-service/            # Users, roles, JWT      (8081)
│   ├── product-service/         # Products, categories    (8082)
│   ├── order-service/           # Orders, carts           (8083)
│   ├── payment-service/         # Payments                (8084)
│   ├── inventory-service/       # Stock                   (8085)
│   ├── notification-service/    # Notifications           (8086)
│   └── analytics-service/       # Sales analytics         (8087)
├── frontend/                    # Angular 22 application  (4200)
├── docker/
│   └── postgres/init.sql        # Creates 7 databases
├── docker-compose.yml           # Full application stack
├── docker-compose.monitoring.yml# Prometheus/Grafana/Loki/Promtail
├── prometheus/grafana/loki/     # Observability configuration
├── .github/workflows/ci.yml     # CI/CD pipeline
└── docs/                        # Section 18 documentation
```

## Quick Start (Docker)

Prerequisites: Docker Engine + Docker Compose v2.

```bash
# 1. Start the whole stack (Postgres, Redis, Kafka, Zookeeper, registry, 9 services, frontend)
docker compose up --build -d

# 2. Open the application
open http://localhost:4200

# 3. Everything must be healthy
docker compose ps
```

The API gateway is at `http://localhost:8080`, and every service also exposes
`/actuator/health` with full details.

See [docs/installation.md](docs/installation.md) (a plain local/IDE setup),
[docs/docker.md](docs/docker.md) (Docker guide) and
[docs/docker.md](docs/deployment.md) (production) for more.

## Services & Ports

| Port | Service | Exposed |
|------|---------|---------|
| 8761 | service-discovery | Yes |
| 8080 | api-gateway (entry point for all `/api/*`) | Yes |
| 8081 | auth-service | Internal |
| 8082 | product-service | Internal |
| 8083 | order-service | Internal |
| 8084 | payment-service | Internal |
| 8085 | inventory-service | Internal |
| 8086 | notification-service | Internal |
| 8087 | analytics-service | Internal |
| 4200 | frontend (nginx) | Yes |
| 5432 | PostgreSQL | Yes (default `postgres` / `admin`) |
| 6379 | Redis | Yes |
| 9092 / 2181 | Kafka / Zookeeper | Yes |

All browser traffic goes to the frontend (4200); nginx proxies `/api/*` to the
gateway (8080), which authenticates JWTs and route-­forwards to the proper
microservice using the service registry.

## Default Accounts

| Role | Email | Password |
|------|-------|----------|
| Admin | `admin@shopsphere.com` | `Zephyr!91Kite` |
| User  | (register a new account) | (strong password required) |

The admin account is created automatically on first start
(`AdminInitializer`). Registration passwords must be 8-64 characters with
uppercase, lowercase, a digit and a special character.

## Documentation

| # | Topic | File |
|---|-------|------|
| 18.1 | Project README | this file |
| 18.2 | Architecture Diagram | [docs/architecture.md](docs/architecture.md) |
| 18.3 | Database Diagram | [docs/database.md](docs/database.md) |
| 18.4 | Use Case Diagram | [docs/use-cases.md](docs/use-cases.md) |
| 18.5 | Sequence Diagrams | [docs/sequence-diagrams.md](docs/sequence-diagrams.md) |
| 18.6 | API Documentation | [docs/api.md](docs/api.md) |
| 18.7 | Installation Guide | [docs/installation.md](docs/installation.md) |
| 18.8 | Docker Guide | [docs/docker.md](docs/docker.md) |
| 18.9 | Testing Guide | [docs/testing.md](docs/testing.md) |
| 18.10 | Deployment Guide | [docs/deployment.md](docs/deployment.md) |

## CI/CD

A single GitHub Actions workflow (`.github/workflows/ci.yml`) runs on every push
to `main`:

1. **Build** - Maven package for each of the 9 microservices (skip tests) and `npm run build` for the frontend.
2. **Test** - unit tests (Surefire/Karma->Vitest) and integration tests (Verify, Testcontainers) for every service.
3. **Images** - `docker/build-push-action` builds and pushes `linux/amd64` + `linux/arm64` images of all 10 components to GHCR, tagged `latest` and by commit SHA.
4. **Deploy** - when `DEPLOY_HOST`, `DEPLOY_USER` and `DEPLOY_SSH_KEY` secrets are configured, the pipeline uploads `docker-compose.yml` + SQL init to the server, pulls the SHA images and runs `docker compose up -d --no-build`. Otherwise it skips deployment.

See [docs/deployment.md](docs/deployment.md) for the full deployment guide.
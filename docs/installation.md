# 18.7 - Installation Guide

This guide covers installing and running ShopSphere locally: prerequisites, the
quickest path (Docker) and the manual path (native services + local PostgreSQL,
Redis, Kafka).

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| Java | 26 (Temurin) | Required for all microservices |
| Node.js | 22 | Required for the Angular frontend |
| Maven | 3.9+ | Or use the included `mvnw` wrapper |
| Docker | 24+ | With Docker Compose v2 (recommended path) |
| Git | any | To clone the repository |

## 1. Clone the repository

```bash
git clone https://github.com/yassineboubkeur/shopsphere-commerce.git
cd shopsphere-commerce
```

## 2. Option A - Run with Docker (recommended)

```bash
docker compose up --build -d
docker compose ps
```

- Frontend: `http://localhost:4200`
- API gateway: `http://localhost:8080`
- Discovery: `http://localhost:8761`
- Wait until every container reports `(healthy)` (the registry and services
  take up to ~90 s to boot on the first run).

## 3. Option B - Run natively

### 3.1 Start the infrastructure

Use the `infra` services from Docker (keeps the local toolchain light):

```bash
docker compose up -d postgres redis kafka zookeeper
```

Alternatively, provision your own:

- PostgreSQL 16 (databases below)
- Redis 7 on `localhost:6379`
- Kafka 7.6 + Zookeeper on `localhost:9092` / `localhost:2181`

Create the seven databases (the same as `docker/postgres/init.sql`):

```sql
CREATE DATABASE shopsphere_auth;
CREATE DATABASE shopsphere_product;
CREATE DATABASE shopsphere_order;
CREATE DATABASE shopsphere_payment;
CREATE DATABASE shopsphere_inventory;
CREATE DATABASE shopsphere_notification;
CREATE DATABASE shopsphere_analytics;
```

The default datasource credentials expected by the services are
`postgres` / `admin` (override with `POSTGRES_PASSWORD` if needed).

### 3.2 Build and run the microservices

Each service is a separate Maven module under `backend/<service>/<service>` with
its own wrapper. Run them in this order (each in its own terminal):

```bash
# In backend/service-discovery/service-discovery
./mvnw spring-boot:run

# In backend/api-gateway/api-gateway
./mvnw spring-boot:run

# Then the domain services (order does not matter among them)
# backend/auth-service/auth-service
# backend/product-service/product-service
# backend/order-service/order-service
# backend/payment-service/payment-service
# backend/inventory-service/inventory-service
# backend/notification-service/notification-service
# backend/analytics-service/analytics-service
```

On Windows, use `mvnw.cmd` instead of `./mvnw`.

> Note: the application configuration files point at `localhost` for Postgres,
> Redis, Kafka and Eureka, so native runs work out of the box.

### 3.3 Run the frontend

```bash
cd frontend
npm install
npm start        # http://localhost:4200 (dev server, proxies /api to :8080 via environment)
```

### 3.4 Verify

- Open `http://localhost:4200` - UI renders.
- `http://localhost:8080/actuator/health` - gateway `UP`.
- `http://localhost:8761/eureka/instances` - all 8 services registered.
- Register a user (`StrongPass@1` style password), log in, browse products.

## 4. Configuration

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | `admin` | Postgres password used by Compose |
| `JWT_SECRET` / `jwt.secret` | built-in secret | HS512 secret for tokens |
| `EUREKA_URL` | `http://localhost:8761/eureka` | Registry URL (Docker uses `http://service-discovery:8761/eureka`) |
| `EUREKA_HOST` | service name | Hostname advertised to the registry |
| `PRODUCT_SERVICE_URL` | `http://localhost:8082` | order-service target for stock calls (Docker: `http://product-service:8082`) |
| `ORDER_SERVICE_URL` | `http://localhost:8083` | payment-service target (Docker: `http://order-service:8083`) |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka broker address |
| `SPRING_DATASOURCE_URL` | per-service `jdbc:postgresql://localhost:5432/shopsphere_<x>` | JDBC URL (Docker uses `postgres`) |

## 5. Default data

- **Admin account** is created automatically on the first boot:
  `admin@shopsphere.com` / `Zephyr!91Kite`.
- Users are registered through the UI or the API; the password policy requires
  8-64 characters with uppercase, lowercase, a digit and a special character.
- The catalog starts empty in a fresh database. Create categories and products
  through the admin UI, or call the ADMIN product/category endpoints.

## 6. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Containers restarting / `(unhealthy)` | Check `docker compose logs <service>`; wait for dependencies to be healthy first |
| `Register/eureka` connection refused at startup | Start `service-discovery` first and ensure `EUREKA_URL` is correct |
| Browser "registration failed" but API works | Use a password that matches the policy; hard-refresh the UI (`Ctrl+F5`) |
| Port already in use | Change `server.port` in that service's properties or stop the conflicting process |
| `ng serve` cannot reach the API | Ensure the gateway is up on `:8080` and the frontend `environment.ts`/`apiUrl` matches |
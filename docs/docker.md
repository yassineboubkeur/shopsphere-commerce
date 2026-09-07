# 18.8 - Docker Guide

This document explains the container topology, images, the Compose file layout,
the monitoring stack and common operational commands.

## Images

Every component is built as its own image from the repo (locally) or from GHCR
(in CI):

| Service | Image (GHCR) | Build context |
|---------|--------------|---------------|
| service-discovery | `shopsphere-commerce/service-discovery` | `backend/service-discovery/service-discovery` |
| api-gateway | `shopsphere-commerce/api-gateway` | `backend/api-gateway/api-gateway` |
| auth-service | `shopsphere-commerce/auth-service` | `backend/auth-service/auth-service` |
| product-service | `shopsphere-commerce/product-service` | `backend/product-service/product-service` |
| order-service | `shopsphere-commerce/order-service` | `backend/order-service/order-service` |
| payment-service | `shopsphere-commerce/payment-service` | `backend/payment-service/payment-service` |
| inventory-service | `shopsphere-commerce/inventory-service` | `backend/inventory-service/inventory-service` |
| notification-service | `shopsphere-commerce/notification-service` | `backend/notification-service/notification-service` |
| analytics-service | `shopsphere-commerce/analytics-service` | `backend/analytics-service/analytics-service` |
| frontend | `shopsphere-commerce/frontend` | `frontend` (multi-stage nginx) |

Backend images are built with Eclipse Temurin JRE 26 + a Maven multi-stage
build; the frontend image is an nginx-alpine multi-stage build that serves the
compiled Angular bundle and proxies `/api/*` to the gateway.

## Compose File (`docker-compose.yml`)

Services: `zookeeper`, `kafka`, `postgres`, `redis`, `service-discovery`,
`api-gateway`, `auth-service`, `product-service`, `order-service`,
`payment-service`, `inventory-service`, `notification-service`,
`analytics-service`, `frontend`.

Key details:

```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.6.0
kafka:
  image: confluentinc/cp-kafka:7.6.0
  environment:
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1
postgres:
  image: postgres:16-alpine
  environment:
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-admin}   # overridable
  volumes:
    - ./docker/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql
redis:
  image: redis:7-alpine
service-discovery:
  build: ./backend/service-discovery/service-discovery
  healthcheck:
    test: bash -c 'exec 3<>/dev/tcp/127.0.0.1/8761 && ... grep -q "\"status\":\"UP\""'
api-gateway:
  healthcheck: (TCP + /actuator/health on 8080)
  restart: unless-stopped
frontend:
  build: ./frontend
  ports: ["4200:80"]
  depends_on:
    api-gateway: { condition: service_started }
```

- Docker networks: `backend-net` (infrastructure + microservices) and
  `frontend-net` (frontend only, bridged to the gateway's published port).
- Every microservice is pinned with `restart: unless-stopped` and has a
  TCP/`/actuator/health` healthcheck whose `start_period` accommodates slow cold
  starts (up to ~60-90 s).
- Infrastructure images expose their standard ports:
  `postgres 5432`, `redis 6379`, `kafka 9092`, `zookeeper 2181`.
- The only published UI ports are `4200:80` (frontend) and `8080:8080`
  (gateway) on the default Compose network.

## Service-to-service URLs in Docker

| Consumer | Configuration | Docker value |
|----------|---------------|--------------|
| all services -> registry | `EUREKA_URL` | `http://service-discovery:8761/eureka` |
| gateway -> services | route map | `http://<service-name>:<port>/api/...` |
| order-service -> product-service | `PRODUCT_SERVICE_URL` | `http://product-service:8082` |
| payment-service -> order-service | `ORDER_SERVICE_URL` | `http://order-service:8083` |

> In Docker these must be container hostnames, not `localhost`, otherwise
> cross-service calls fail (a common local-vs-Docker pitfall).

## Monitoring Stack (`docker-compose.monitoring.yml`)

```bash
docker compose -f docker-compose.monitoring.yml up -d
```

| Service | Image | Port | Notes |
|---------|-------|------|-------|
| prometheus | `prom/prometheus:latest` | 9090 | Scrapes all 9 services via `host.docker.internal:<port>/actuator/prometheus` |
| grafana | `grafana/grafana:latest` | 3000 | Provisioned Prometheus + Loki datasources and `shopsphere-overview` dashboard |
| loki | `grafana/loki:latest` | 3100 | Ingests JSON logs |
| promtail | `grafana/promtail:latest` | - | `docker_sd` discovery of `shopsphere*`/`kafka*`/`zookeeper*` containers + `/var/log/shopsphere/*.log` |

Default Grafana login: `admin` / `admin` (Grafana enforces a change on first
login).

## Common Docker commands

```bash
# Start / stop / status
docker compose up --build -d
docker compose down
docker compose ps

# Logs (regular + JSON / tail)
docker compose logs -f order-service
docker compose logs --tail=200 product-service

# Explicitly rebuild & recreate a single container
docker compose up -d --build order-service

# Run an ad-hoc command inside a container
docker compose exec postgres psql -U postgres -d shopsphere_order -c '\dt'

# Inspect registry contents
curl http://localhost:8761/eureka/instances

# Ports / prune
docker ps
docker system df
docker system prune -af      # WARNING: removes all unused data & images
```

## Recreating the Postgres data directory

When a schema or test goes wrong, the fastest clean reset is:

```bash
docker compose down -v      # remove the anonymous postgres volume
docker compose up -d        # init.sql re-applies the 7 databases
```

> `-v` also deletes Kafka/Redis data. Do **not** run `down -v` in production
> without a backup.

## Docker tips

- Use `docker compose config` to render the effective final compose file.
- Service healthchecks depend on Bash and `grep`; nginx-alpine and JRE images
  ship both, so healthchecks work unchanged.
- Images are multi-arch (`linux/amd64`, `linux/arm64`) when pulled from GHCR;
  local `docker compose build` builds for the host platform only.
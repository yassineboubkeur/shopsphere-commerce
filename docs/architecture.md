# 18.2 - Architecture Diagram

This document describes the physical and logical architecture of ShopSphere,
including the container topology, the request flow through the API gateway, the
event-driven data flows and the observability stack.

## System Context

```mermaid
flowchart LR
    subgraph User["Users"]
        C[Customer browser]
        A[Admin browser]
    end

    subgraph Edge["Frontend"]
        NG["nginx 4200"]
        UI["Angular SPA"]
    end

    subgraph Gateway["API Layer"]
        GW["api-gateway 8080 (JWT filter + proxy)"]
    end

    subgraph Platform["Microservices"]
        DISC["service-discovery 8761 (registry)"]
        AUTH["auth-service 8081"]
        PROD["product-service 8082"]
        ORD["order-service 8083"]
        PAY["payment-service 8084"]
        INV["inventory-service 8085"]
        NOTIF["notification-service 8086"]
        AN["analytics-service 8087"]
    end

    subgraph Infra["Infrastructure"]
        PG[("PostgreSQL 16")]
        RD[("Redis 7")]
        KF[("Kafka 7.6 + Zookeeper")]
    end

    C -->|HTTP :4200| UI
    A -->|HTTP :4200| UI
    UI -->|/api/*  same-origin| NG
    NG -->|proxy_pass| GW
    GW --> AUTH
    GW --> PROD
    GW --> ORD
    GW --> PAY
    GW --> INV
    GW --> NOTIF
    GW --> AN
    AUTH -.register / heartbeat.-> DISC
    PROD -.register / heartbeat.-> DISC
    ORD -.register / heartbeat.-> DISC
    PAY -.register / heartbeat.-> DISC
    INV -.register / heartbeat.-> DISC
    NOTIF -.register / heartbeat.-> DISC
    AN -.register / heartbeat.-> DISC
    GW -.resolves target URL.-> DISC

    AUTH --> PG
    PROD --> PG
    ORD --> PG
    PAY --> PG
    INV --> PG
    NOTIF --> PG
    AN --> PG

    PROD --> RD
    ORD --> KF
    PAY --> KF
    INV --> KF
    NOTIF --> KF
    AN --> KF
```

## Container Architecture (Docker Compose)

```mermaid
flowchart LR
    subgraph frontend-net["network: frontend-net"]
        FE["frontend\nnginx 4200:80"]
    end

    subgraph backend-net["network: backend-net"]
        ZK["zookeeper 2181"]
        KF["kafka 9092"]
        PG["postgres 5432\n7 databases"]
        RD["redis 6379"]
        DISC["service-discovery 8761"]
        GW["api-gateway 8080"]
        AUTH["auth-service 8081"]
        PROD["product-service 8082"]
        ORD["order-service 8083"]
        PAY["payment-service 8084"]
        INV["inventory-service 8085"]
        NOTIF["notification-service 8086"]
        AN["analytics-service 8087"]
    end

    FE ---|frontend-net| GW
    GW -.-> AUTH
    GW -.-> PROD
    GW -.-> ORD
    GW -.-> PAY
    GW -.-> INV
    GW -.-> NOTIF
    GW -.-> AN

    ZK --- KF
    PG --- AUTH
    PG --- PROD
    PG --- ORD
    PG --- PAY
    PG --- INV
    PG --- NOTIF
    PG --- AN
    RD --- PROD
    KF --- ORD
    KF --- PAY
    KF --- INV
    KF --- NOTIF
    KF --- AN
```

## Request Flow Through the Gateway

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant N as nginx (frontend)
    participant G as api-gateway
    participant D as service-discovery
    participant S as Target service

    B->>N: GET /api/products HTTP Origin=:4200
    N->>G: proxy_pass http://api-gateway:8080
    G->>G: JwtAuthenticationFilter (public path / validate token)
    G->>D: GET /instances/{target} (cached, refreshed every 30s)
    D-->>G: http://product-service:8082
    G->>S: forward request (headers + body + <br/>X-User-Email / X-User-Role)
    S-->>G: JSON response
    G-->>N: response
    N-->>B: response
```

- Every path under `/api/*` is proxied by `ProxyService` (WebFlux `RouterFunction`
  + `WebClient`), using the longest matching prefix of the route map.
- Public prefixes: `/api/auth/` and `/api/products/`.
- Protected prefixes (`/api/orders/`, `/api/payments/`, `/api/cart/`,
  `/api/inventory/`, `/api/notifications/`, `/api/analytics/`, `/api/user/`,
  `/api/admin/`) require a valid `Authorization: Bearer <JWT>`.
- On success the gateway injects `X-User-Email` and `X-User-Role`; downstream
  services use these headers for admin checks.
- OPTIONS preflight requests are answered directly with CORS headers.
- Safe (GET) requests are retried once (max-attempts 2, 1 s delay) on 5xx errors;
  failures produce 502, and unresolved routes produce 404.

## Event-Driven Integration (Kafka)

| Topic | Producer | Consumers |
|-------|----------|-----------|
| `order-created` | order-service | inventory, notification, analytics |
| `order-shipped` | order-service | notification |
| `order-delivered` | order-service | notification |
| `order-cancelled` | order-service | notification |
| `payment-successful` | payment-service | notification, analytics |
| `payment-failed` | payment-service | notification, analytics |
| `stock-updated` | inventory-service | (monitoring/observability) |
| `stock-insufficient` | inventory-service | (monitoring/observability) |

```mermaid
flowchart LR
    O[order-service] -->|order-created / shipped / delivered / cancelled| K[(Kafka)]
    P[payment-service] -->|payment-successful / payment-failed| K
    I[inventory-service] -->|stock-updated / stock-insufficient| K
    K --> INV[inventory-service]
    K --> NOTIF[notification-service]
    K --> AN[analytics-service]
```

## Observability Stack

```mermaid
flowchart LR
    subgraph Apps["Application Containers"]
        SVC["9 Spring Boot services"]
        KF["kafka / zookeeper"]
    end

    SVC --->|/actuator/prometheus scraped| P[("Prometheus 9090")]
    SVC --->|structured JSON logs| LT[("Loki 3100")]
    KF --->|logs via docker driver| LT

    SLTS["Promtail (docker_sd + /var/log/shopsphere/*.log)"] --> LT
    P --> G["Grafana 3000"]
    LT --> G

    G --> DASH["ShopSphere dashboard<br/>(10 panels)"]

    SVC --->|metrics endpoint (Micrometer)| P
```

The monitoring stack is defined in `docker-compose.monitoring.yml` and uses
official Prometheus, Grafana, Loki and Promtail images. Prometheus scrapes every
service via `host.docker.internal:<port>`; Promtail discovers containers named
`shopsphere*`, `kafka*` or `zookeeper*` and ships JSON logs to Loki. Grafana is
provisioned with a Prometheus datasource, a Loki datasource and the
`shopsphere-overview` dashboard.
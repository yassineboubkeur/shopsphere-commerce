# 18.3 - Database Diagram

All services share a single PostgreSQL 16 container, but each domain has its own
dedicated database (created by `docker/postgres/init.sql`). Schemas are created
and kept up to date by Hibernate (`ddl-auto=update`).

```
PostgreSQL (postgres:16-alpine)  port 5432  user: postgres  password: demo default: admin
├── shopsphere_auth        (auth-service)
├── shopsphere_product     (product-service)
├── shopsphere_order       (order-service)
├── shopsphere_payment     (payment-service)
├── shopsphere_inventory   (inventory-service)
├── shopsphere_notification (notification-service)
└── shopsphere_analytics   (analytics-service)
```

## Database Ownership

```mermaid
flowchart LR
    PG[(PostgreSQL 16)]
    PG --> D1[(shopsphere_auth)]
    PG --> D2[(shopsphere_product)]
    PG --> D3[(shopsphere_order)]
    PG --> D4[(shopsphere_payment)]
    PG --> D5[(shopsphere_inventory)]
    PG --> D6[(shopsphere_notification)]
    PG --> D7[(shopsphere_analytics)]
    D1 --> A[auth-service]
    D2 --> P[product-service]
    D3 --> O[order-service]
    D4 --> PA[payment-service]
    D5 --> I[inventory-service]
    D6 --> N[notification-service]
    D7 --> AN[analytics-service]
```

## auth-service - `shopsphere_auth`

```mermaid
erDiagram
    users {
        BIGINT id PK
        VARCHAR username UK
        VARCHAR email UK
        VARCHAR password
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    roles {
        BIGINT id PK
        VARCHAR name UK "USER | ADMIN"
    }
    users_roles {
        BIGINT user_id FK
        BIGINT role_id FK
    }
    users ||--o{ users_roles : "has"
    roles ||--o{ users_roles : "granted to"
```

## product-service - `shopsphere_product`

```mermaid
erDiagram
    categories {
        BIGINT id PK
        VARCHAR name UK
        TEXT description
        VARCHAR image_url
    }
    products {
        BIGINT id PK
        VARCHAR name
        TEXT description
        NUMERIC price
        INT stock_quantity
        VARCHAR image_url
        BOOLEAN active
        BIGINT category_id FK
    }
    categories ||--o{ products : "contains"
```

Note: the product-service uses Redis (`db 0`) to cache product lookups.

## order-service - `shopsphere_order`

```mermaid
erDiagram
    orders {
        BIGINT id PK
        BIGINT user_id
        VARCHAR order_number UK
        VARCHAR shipping_name
        VARCHAR shipping_address
        VARCHAR shipping_city
        VARCHAR shipping_zip
        VARCHAR shipping_phone
        NUMERIC total_amount
        VARCHAR status "PENDING..CANCELLED"
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    order_items {
        BIGINT id PK
        BIGINT order_id FK
        BIGINT product_id
        VARCHAR product_name
        NUMERIC price
        INT quantity
        NUMERIC subtotal
    }
    payments {
        BIGINT id PK
        BIGINT order_id FK
        NUMERIC amount
        VARCHAR status "PENDING|COMPLETED|FAILED|REFUNDED"
        VARCHAR payment_method
        VARCHAR transaction_id
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    carts {
        BIGINT id PK
        BIGINT user_id
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
    cart_items {
        BIGINT id PK
        BIGINT cart_id FK
        BIGINT product_id
        VARCHAR product_name
        NUMERIC price
        INT quantity
    }
    orders ||--o{ order_items : "contains"
    orders ||--o| payments : "has"
    carts ||--o{ cart_items : "contains"
```

## payment-service - `shopsphere_payment`

```mermaid
erDiagram
    payments {
        BIGINT id PK
        BIGINT order_id
        BIGINT user_id
        NUMERIC amount
        VARCHAR status "PENDING|PROCESSING|COMPLETED|FAILED|REFUNDED"
        VARCHAR payment_method
        VARCHAR transaction_id
        VARCHAR failure_reason
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```

The payment-service keeps its own copy of payment state and publishes
`payment-successful` / `payment-failed` events to Kafka.

## inventory-service - `shopsphere_inventory`

```mermaid
erDiagram
    inventory {
        BIGINT id PK
        BIGINT product_id UK
        VARCHAR product_name
        INT quantity
        INT reserved_quantity
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```

`availableQuantity` is derived: `quantity - reserved_quantity` (not persisted).

## notification-service - `shopsphere_notification`

```mermaid
erDiagram
    notifications {
        BIGINT id PK
        BIGINT user_id
        VARCHAR type "ORDER_CONFIRMATION | SHIPPING | PAYMENT_CONFIRMATION"
        VARCHAR subject
        TEXT message
        VARCHAR status "default PENDING"
        TIMESTAMP created_at
        TIMESTAMP updated_at
    }
```

## analytics-service - `shopsphere_analytics`

```mermaid
erDiagram
    analytics_events {
        BIGINT id PK
        VARCHAR event_type
        BIGINT order_id
        BIGINT user_id
        VARCHAR order_number
        BIGINT product_id
        VARCHAR product_name
        INT quantity
        NUMERIC amount
        VARCHAR payment_method
        VARCHAR status
        TIMESTAMP event_timestamp
        TIMESTAMP created_at
    }
```

Analytics events are produced by consuming `order-created`, `payment-successful`
and `payment-failed` Kafka events.

## Cross-Domain Relationships (logical, at the application level)

```mermaid
flowchart LR
    U[("users (auth)")]
    PR[("products/categories (product)")]
    ORD[("orders/order_items (order)")]
    CART[("carts/cart_items (order)")]
    PAYA[("payments (payment)")]
    INV[("inventory (inventory)")]
    NF[("notifications (notification)")]
    AN[("analytics_events (analytics)")]

    U -->|"user_id"| ORD
    U -->|"user_id"| CART
    U -->|"user_id"| PAYA
    U -->|"user_id"| NF
    U -->|"user_id"| AN
    PR -->|"product_id (denormalized name+price)"| ORD
    PR -->|"product_id"| CART
    PR -->|"product_id"| PAYA
    ORD -->|"order_id"| PAYA
    ORD -->|"order_id"| AN
    PR -->|"product_id"| INV
    PR -->|"product_id"| AN
```

There are no foreign keys referencing other databases; services join on
`id`/`user_id`/`order_id`/`product_id` at the application layer or via
denormalized columns.
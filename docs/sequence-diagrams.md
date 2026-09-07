# 18.5 - Sequence Diagrams

This document contains the core end-to-end flows of the platform: registration,
login, browsing the catalog, adding to cart, placing an order, processing a
payment, and the asynchronous notification/analytics flow.

## Registration

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant N as nginx (:4200)
    participant G as api-gateway
    participant A as auth-service
    participant P as PostgreSQL (shopsphere_auth)

    B->>N: POST /api/auth/register<br/>{username,email,password}
    N->>G: proxy /api/auth/register
    G->>G: public prefix - skip JWT
    G->>A: forward POST /api/auth/register
    A->>P: INSERT users + users_roles (USER)
    P-->>A: saved user
    A->>A: encode password, sign JWT
    A-->>G: 201 { token, user }
    G-->>N: forward response
    N-->>B: token stored (localStorage / interceptor)
```

## Login

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant G as api-gateway
    participant A as auth-service
    participant P as PostgreSQL

    B->>G: POST /api/auth/login {email,password}
    G->>A: forward
    A->>P: findByEmail
    P-->>A: user + roles
    A->>A: matches password? -> sign JWT (HS512)
    A-->>G: 200 { token, id, username, email, role }
    G-->>B: response
    Note over B: auth interceptor adds "Authorization: Bearer <token>"<br/>to every subsequent request
```

## Browse Products (public)

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant N as nginx
    participant G as api-gateway
    participant D as service-discovery
    participant PR as product-service
    participant RD as Redis

    B->>N: GET /api/products
    N->>G: proxy
    G->>D: cached instance map (refresh every 30 s)
    G->>PR: GET /api/products
    PR->>RD: cache lookup
    alt cache hit
        RD-->>PR: products
    else cache miss
        PR->>PR: PostgreSQL query
        PR->>RD: cache miss result
    end
    PR-->>G: 200 [ products ]
    G-->>N: forward
    N--->>B: JSON list
```

## Add Item to Cart

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant G as api-gateway
    participant O as order-service
    participant P as PostgreSQL (shopsphere_order)

    B->>G: POST /api/cart/{userId}/items {productId,quantity}
    G->>G: protected path - validate JWT
    G->>O: forward + X-User-Email/X-User-Role
    O->>O: find or create cart for user
    O->>P: upsert cart_items
    P-->>O: cart
    O-->>G: 200 cart
    G-->>B: response
```

## Place an Order

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant G as api-gateway
    participant O as order-service
    participant PS as product-service
    participant P as PostgreSQL (shopsphere_order)
    participant K as Kafka

    B->>G: POST /api/orders {userId, items[], shipping...}
    G->>G: protected - validate JWT
    G->>O: forward
    O->>O: map items -> subtotals -> total
    O->>P: INSERT order + order_items (status=PENDING)
    P-->>O: saved order
    O->>PS: POST /api/products/{id}/stock/decrement {quantity}
    PS-->>O: 200 (stock decremented)
    O->>K: publish order-created
    O-->>G: 201 order
    G-->>B: response -> redirect to /payment/{id}
    K-->>InventoryConsumer: order-created
    K-->>NotificationConsumer: order-created
    K-->>AnalyticsConsumer: order-created
```

## Cart Checkout

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant G as api-gateway
    participant O as order-service
    participant PS as product-service

    B->>G: POST /api/cart/{userId}/checkout
    G->>O: forward
    O->>O: build cart items into order request
    O->>PS: GET /api/products/{id} (validateStock)
    PS-->>O: product (or error)
    O->>O: if insufficient stock -> error
    O-->>G: 200 CheckoutResponse { orderId, total }
    G-->>B: proceed to payment
```

## Process a Payment

```mermaid
sequenceDiagram
    autonumber
    participant B as Browser
    participant G as api-gateway
    participant PY as payment-service
    participant O as order-service
    participant P as PostgreSQL (shopsphere_payment)
    participant K as Kafka

    B->>G: POST /api/payments/process {orderId,amount,method}
    G->>PY: forward
    PY->>PY: validate payment request
    PY->>P: create/update payment record
    alt payment successful
        PY->>O: POST /api/orders/{id}/payment/complete {transactionId}
        O-->>PY: 200 (order -> CONFIRMED)
        PY->>K: publish payment-successful
        PY-->>G: 200 PaymentResult OK
    else payment failed
        PY->>O: POST /api/orders/{id}/status {status:CANCELLED}
        PY->>K: publish payment-failed
        PY-->>G: 200 PaymentResult FAILED (reason)
    end
    G-->>B: result
    Note over K: Notification + Analytics consumers react
```

## Notifications & Analytics (async)

```mermaid
sequenceDiagram
    autonumber
    participant K as Kafka
    participant ND as notification-service consumer
    participant AC as analytics-service consumer
    participant NP as PostgreSQL (shopsphere_notification)
    participant AP as PostgreSQL (shopsphere_analytics)

    K-->>ND: order-created / order-shipped / order-delivered <br/> order-cancelled / payment-successful / payment-failed
    ND->>NP: INSERT notifications (typed, per user)
    ND-->>K: acknowledge
    K-->>AC: order-created / payment-successful / payment-failed
    AC->>AP: INSERT analytics_events
    AC-->>K: acknowledge

    B->>G: GET /api/notifications/user/{userId}
    G->>ND: forward
    ND->>NP: SELECT by user
    NP-->>ND: notifications
    ND-->>G: list
    G-->>B: list
```

## Failing Payment -> Order Cancellation (compensation)

```mermaid
sequenceDiagram
    autonumber
    participant PY as payment-service
    participant O as order-service
    participant K as Kafka
    participant NF as notification-service

    PY->>PY: payment fails
    PY->>O: POST /api/orders/{id}/status {CANCELLED}
    O->>O: restore stock (product-service) + save status + publish order-cancelled
    PY->>K: publish payment-failed
    PY->>O: POST /api/orders/{id}/status
    K-->>NF: order-cancelled + payment-failed
    NF-->>USER: notification "payment failed / order cancelled"
```
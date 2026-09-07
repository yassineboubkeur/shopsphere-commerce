# 18.6 - API Documentation

All endpoints are exposed through the **API gateway** at `http://localhost:8080`
(or the deployed host). The frontend calls them same-origin under `/api/*`;
nginx proxies `/api/*` to the gateway.

## Conventions

- **Base URL**: `http://localhost:8080` (development), or
  `http://51.170.143.240:8080` (example production host).
- **Content-Type**: `application/json` for request/response bodies.
- **Authentication**:
  - Public prefixes: `/api/auth/**`, `/api/products/**`.
  - Protected prefixes require: `Authorization: Bearer <JWT>`.
  - After validation the gateway adds `X-User-Email` and `X-User-Role`.
- **Admin-only** endpoints require the caller to have the `ADMIN` role; the
  check is performed downstream using the `X-User-Role` header (product, order).
- **Error format** (JSON):

```json
{
  "error": "Validation failed",
  "details": ["password: Password must be 8-64 characters..."],
  "timestamp": "2026-09-07T15:20:38",
  "status": 400
}
```

or

```json
{ "error": "Valid JWT token required", "status": 401 }
```

## Auth Service - `/api/auth`, `/api/user`, `/api/admin`

### POST /api/auth/register
Register a new user.
```json
{ "username": "jdoe", "email": "jdoe@example.com", "password": "StrongPass@1" }
```
**201** `{ "token": "<jwt>", "type": "Bearer", "id": 1, "username": "jdoe", "email": "...", "role": "USER" }`

### POST /api/auth/login
```json
{ "email": "jdoe@example.com", "password": "StrongPass@1" }
```
**200** same shape as register.

### GET /api/user/profile
Returns the authenticated user's profile (id, username, email, roles). **200**

### GET /api/admin/dashboard
Welcome message + admin email and role (ADMIN). **200**

### GET /api/admin/users
List all users with `id, username, email, role, createdAt`. **200**

### PUT /api/admin/users/{id}/role
Body: `{ "role": "ADMIN" | "USER" }` - updates a user's role. **200**

### DELETE /api/admin/users/{id}
Deletes a user. **204**

## Product Service - `/api/products`, `/api/categories`

### POST /api/products
(ADMIN) Create product.
```json
{ "name": "Wireless Headphones", "description": "...", "price": 149.99,
  "stockQuantity": 50, "imageUrl": "https://...", "active": true,
  "categoryId": 1 }
```
**201**

### GET /api/products
All products. **200** `[ {id, name, description, price, stockQuantity, imageUrl, active, category} ]`

### GET /api/products/paginated
`?page=0&size=10&sortBy=name&direction=asc` - paged result. **200**

### GET /api/products/search?name=
Search by name (case-insensitive contains). **200**

### GET /api/products/{id}
Product by id. **200**

### GET /api/products/filter/price?minPrice=&maxPrice=
Filter by price range. **200**

### GET /api/products/category/{categoryId}
Products in a category. **200**

### PUT /api/products/{id}
(ADMIN) Update a product. **200**

### PATCH /api/products/{id}/stock { "quantity": N }
(ADMIN) Overwrite product stock. **200**

### POST /api/products/{id}/stock/decrement { "quantity": N }
Decrement stock (used by order-service). **200**

### POST /api/products/{id}/stock/increase { "quantity": N }
Increase stock (used by order-service). **200**

### DELETE /api/products/{id}
(ADMIN) **204**

### POST /api/categories
(ADMIN) Create category `{ "name": "Electronics", "description": "...", "imageUrl": "..." }`. **201**

### GET /api/categories
All categories. **200**

### GET /api/categories/{id}
Category by id. **200**

### PUT /api/categories/{id}
(ADMIN) Update category. **200**

### DELETE /api/categories/{id}
(ADMIN) **204**

## Order Service - `/api/orders`, `/api/cart`

### POST /api/orders
Place an order.
```json
{
  "userId": 1,
  "items": [ { "productId": 11, "productName": "Car", "price": 40.0, "quantity": 1 } ],
  "shippingName": "Jane", "shippingAddress": "1 Main St", "shippingCity": "Casablanca",
  "shippingZip": "20000", "shippingPhone": "+212..."
}
```
**201** full order with `id`, `orderNumber` (ORD-XXXXXXXX), `status=PENDING`.

### GET /api/orders
(ADMIN) All orders. **200**

### GET /api/orders/{orderId}
Order by id. **200**

### GET /api/orders/number/{orderNumber}
Order by order number. **200**

### GET /api/orders/user/{userId}
Orders of a user. **200**

### PATCH /api/orders/{orderId}/status
(ADMIN) `{ "status": "CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED" }`
Triggers stock restore + events for CANCELLED, tracking events for SHIPPED/DELIVERED. **200**

### POST /api/orders/{orderId}/payment/complete
`{ "transactionId": "..." }` - marks payment completed, order -> CONFIRMED. **200**

### POST /api/orders/{orderId}/confirm
Confirm a PENDING order. **200**

### PUT /api/orders/{orderId}?userId=
Update shipping details of a PENDING order owned by the user. **200**

### DELETE /api/orders/{orderId}?userId=
Delete a PENDING order of the user (restores stock, publishes cancelled). **204**

### DELETE /api/orders/admin/{orderId}
(ADMIN) Delete any order. **204**

### GET /api/cart/{userId}
Get the user's cart. **200** `{ id, userId, items: [...] }`

### POST /api/cart/{userId}/items
`{ "productId": 11, "quantity": 1 }` - add to cart. **200**

### PUT /api/cart/{userId}/items/{productId}
`{ "quantity": 2 }` - update quantity. **200**

### DELETE /api/cart/{userId}/items/{productId}
Remove an item. **204**

### GET /api/cart/{userId}/validate
Validate the cart against current stock. **200**

### POST /api/cart/{userId}/checkout
Returns `CheckoutResponse { orderId, total, ... }` after stock validation. **200**

## Payment Service - `/api/payments`

### POST /api/payments/process
```json
{ "orderId": 1, "userId": 1, "amount": 40.0, "paymentMethod": "CARD" }
```
**200** `{ orderId, status: "COMPLETED"|"FAILED", amount, paymentMethod, message, processedAt }`

### POST /api/payments/process/success
Force a successful result (demo). **200**

### POST /api/payments/process/failed
Force a failed result (demo, triggers cancellation + notifications). **200**

### GET /api/payments/result/{paymentId}
Payment result by record id. **200**

### GET /api/payments/result/order/{orderId}
Payment result by order id. **200**

### POST /api/payments
Create a payment record. **201**

### POST /api/payments/{paymentId}/process
Process an existing payment. **200**

### GET /api/payments/{paymentId}
Payment by id. **200**

### GET /api/payments/order/{orderId}
Payment by order id. **200**

### POST /api/payments/{paymentId}/refund
Refund; publishes `payment-failed` and cancels order. **200**

### GET /api/payments/user/{userId}
All payments of a user. **200**

## Inventory Service - `/api/inventory`

### GET /api/inventory/product/{productId}
Stock record for a product. **200**

### GET /api/inventory
All stock records. **200**

### POST /api/inventory
Create record `{ productId, productName, quantity, reservedQuantity }`. **201**

### PUT /api/inventory/product/{productId}
Update quantity. **200**

### POST /api/inventory/product/{productId}/reserve
`{ "quantity": N }` - reserve stock. **200**

### POST /api/inventory/product/{productId}/release
`{ "quantity": N }` - release reserved stock. **200**

### GET /api/inventory/product/{productId}/available?quantity=N
Check availability. **200**

## Notification Service - `/api/notifications`

### GET /api/notifications
All notifications. **200**

### GET /api/notifications/user/{userId}
Notifications for a user. **200**

### GET /api/notifications/user/{userId}/type/{type}
Filtered by type. **200**

### GET /api/notifications/shipping/user/{userId}
Shipping notifications (SHIPPING_NOTIFICATION). **200**

### GET /api/notifications/order-confirmation/user/{userId}
Order confirmation notifications. **200**

### GET /api/notifications/payment-confirmation/user/{userId}
Payment confirmation notifications. **200**

### GET /api/notifications/{id}
By id, 404 when absent. **200**

### PATCH /api/notifications/{id}/status
`{ "status": "SEEN" }` - mark status. **200**

## Analytics Service - `/api/analytics`

### GET /api/analytics/total-sales
`{ totalSales: number }`. **200**

### GET /api/analytics/total-orders
`{ totalOrders: number }`. **200**

### GET /api/analytics/best-selling
`[ { productName, totalQuantity } ]`. **200**

### GET /api/analytics/sales-over-time
Sales aggregated over time. **200**

### GET /api/analytics/orders-by-status
Order counts grouped by status. **200**

## Service Discovery - `/eureka` (internal)

| Endpoint | Description |
|----------|-------------|
| `POST /eureka/register` | Register a service instance |
| `PUT /eureka/renew/{name}` | Renew a lease (204) |
| `DELETE /eureka/deregister/{name}` | Deregister |
| `GET /eureka/instances/{name}` | Instances of a service |
| `GET /eureka/instances` | All registered instances |

## Actuator (all services)

Every service (via the gateway or directly) exposes:

- `GET /actuator/health` - with `show-details: always`
- `GET /actuator/health/liveness`, `GET /actuator/health/readiness`
- `GET /actuator/info`, `GET /actuator/metrics`, `GET /actuator/env`,
  `GET /actuator/loggers`, `GET /actuator/beans`, `GET /actuator/conditions`
- `GET /actuator/prometheus` - Prometheus scrape endpoint
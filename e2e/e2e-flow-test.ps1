$ErrorActionPreference = "Stop"

$GW = "http://localhost:8080"
$PASS = "E2eTest!2026"
$TIMESTAMP = [DateTimeOffset]::Now.ToUnixTimeMilliseconds()
$BUYER_EMAIL = "e2ebuyer_${TIMESTAMP}@shopsphere.com"
$BUYER_USER = "e2ebuyer_${TIMESTAMP}"
$PRODUCT_NAME = "E2E Widget $TIMESTAMP"

$step = 0
function Step($msg) { $script:step++; Write-Host "`n[$($script:step)] $msg" -ForegroundColor Cyan }
function Ok($msg)  { Write-Host "  OK: $msg" -ForegroundColor Green }
function Fail($msg){ Write-Host "  FAIL: $msg" -ForegroundColor Red; throw $msg }

function Post($url, $body, $headers = @{}) {
    $json = $body | ConvertTo-Json -Depth 10
    $h = @{ "Content-Type" = "application/json" } + $headers
    try {
        $r = Invoke-WebRequest -Uri "$GW$url" -Method POST -Body $json -Headers $h -UseBasicParsing -ErrorAction Stop
        return @{ status = $r.StatusCode; body = ($r.Content | ConvertFrom-Json) }
    } catch {
        if ($_.Exception.Response) {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            return @{ status = [int]$_.Exception.Response.StatusCode; body = ($errBody | ConvertFrom-Json); raw = $errBody }
        }
        throw $_
    }
}

function Get-Api($url, $headers = @{}) {
    $h = @{ "Content-Type" = "application/json" } + $headers
    try {
        $r = Invoke-WebRequest -Uri "$GW$url" -Method GET -Headers $h -UseBasicParsing -ErrorAction Stop
        return @{ status = $r.StatusCode; body = ($r.Content | ConvertFrom-Json) }
    } catch {
        if ($_.Exception.Response) {
            $reader = [System.IO.StreamReader]::new($_.Exception.Response.GetResponseStream())
            $errBody = $reader.ReadToEnd()
            return @{ status = [int]$_.Exception.Response.StatusCode; body = ($errBody | ConvertFrom-Json); raw = $errBody }
        }
        throw $_
    }
}

function Auth($token) { @{ "Authorization" = "Bearer $token" } }

Write-Host "=== ShopSphere E2E Flow Test ===" -ForegroundColor Yellow
Write-Host "Gateway: $GW"
Write-Host "Buyer: $BUYER_EMAIL"
Write-Host "Product: $PRODUCT_NAME"

# ── 1. Login as Admin ──
Step "Login as Admin"
$r = Post "/api/auth/login" @{ email = "admin@shopsphere.com"; password = "Zephyr!91Kite" }
if ($r.status -ne 200) { Fail "Admin login failed: $($r.status) $($r.raw)" }
$adminToken = $r.body.token
$adminRole = $r.body.role
Ok "Admin token received, role=$adminRole"

# ── 2. Register Buyer ──
Step "Register Buyer"
$r = Post "/api/auth/register" @{ username = $BUYER_USER; email = $BUYER_EMAIL; password = $PASS }
if ($r.status -ne 201 -and $r.status -ne 200) { Fail "Register failed: $($r.status) $($r.raw)" }
$userToken = $r.body.token
$userId = $r.body.id
$userRole = $r.body.role
Ok "Buyer registered: id=$userId role=$userRole"

# ── 3. Create Category ──
Step "Create Category"
$r = Post "/api/categories" @{ name = "E2E Category $TIMESTAMP"; description = "Created by E2E test" } @{ "X-User-Role" = "ADMIN" }
if ($r.status -ne 201 -and $r.status -ne 200) { Fail "Category creation failed: $($r.status) $($r.raw)" }
$categoryId = $r.body.id
Ok "Category created: id=$categoryId"

# ── 4. Create Product (admin) ──
Step "Create Product (admin, stock=10)"
$r = Post "/api/products" @{ name = $PRODUCT_NAME; description = "E2E test product"; price = 29.99; stockQuantity = 10; categoryId = $categoryId } @{ "X-User-Role" = "ADMIN" }
if ($r.status -ne 201 -and $r.status -ne 200) { Fail "Product creation failed: $($r.status) $($r.raw)" }
$productId = $r.body.id
Ok "Product created: id=$productId"

# ── 5. Browse Products (public) ──
Step "Browse Products (public)"
$r = Get-Api "/api/products"
if ($r.status -ne 200) { Fail "Browse products failed: $($r.status)" }
$found = $r.body | Where-Object { $_.id -eq $productId }
if (-not $found) { Fail "Product $productId not found in catalog" }
Ok "Product browsable: stock=$($found.stockQuantity)"

# ── 6. Add to Cart ──
Step "Add to Cart"
$r = Post "/api/cart/$userId/items" @{ productId = $productId; productName = $PRODUCT_NAME; price = 29.99; quantity = 1 } (Auth $userToken)
if ($r.status -ne 200) { Fail "Add to cart failed: $($r.status) $($r.raw)" }
Ok "Item added to cart"

# ── 7. Checkout (creates order + triggers OrderCreated event) ──
Step "Checkout (creates order + publishes OrderCreated event)"
$r = Post "/api/cart/$userId/checkout" @{} (Auth $userToken)
if ($r.status -ne 200) { Fail "Checkout failed: $($r.status) $($r.raw)" }
$orderId = $r.body.orderId
$orderNumber = $r.body.orderNumber
$totalAmount = $r.body.totalAmount
Ok "Order created: id=$orderId number=$orderNumber total=$totalAmount"

# ── 8. Process Payment ──
Step "Process Payment"
$r = Post "/api/payments/process" @{
    orderId = $orderId; userId = $userId; amount = $totalAmount; paymentMethod = "STRIPE"
    cardNumber = "4242424242424242"; cardHolder = "E2E Buyer"; expiryDate = "12/28"; cvv = "123"
} (Auth $userToken)
if ($r.status -ne 200) { Fail "Payment request failed: $($r.status) $($r.raw)" }
if ($r.body.status -ne "SUCCESS") { Fail "Payment not SUCCESS: $($r.body.status) - $($r.body.message)" }
$txnId = $r.body.transactionId
Ok "Payment SUCCESS: txn=$txnId"

# ── 9. Verify Order Created and Status Confirmed ──
Step "Verify Order (poll for CONFIRMED status)"
$confirmed = $false
for ($i = 1; $i -le 10; $i++) {
    Start-Sleep -Seconds 2
    $r = Get-Api "/api/orders/$orderId" (Auth $userToken)
    if ($r.status -eq 200) {
        $s = $r.body.status
        Ok "Order status: $s (attempt $i/10)"
        if ($s -eq "CONFIRMED" -or $s -eq "COMPLETED") {
            $confirmed = $true
            break
        }
    }
}
if (-not $confirmed) { Fail "Order never confirmed after 20s" }

# ── 10. Verify Inventory Updated (stock decremented) ──
Step "Verify Inventory Updated (product stock decreased from 10)"
$stockOk = $false
for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 2
    $r = Get-Api "/api/products/$productId"
    if ($r.status -eq 200) {
        $stock = $r.body.stockQuantity
        Ok "Product stock=$stock (attempt $i/15)"
        if ($stock -eq 9) {
            $stockOk = $true
            break
        }
    }
}
if (-not $stockOk) { Fail "Stock never decreased to 9 after 30s" }

# ── 11. Verify Notifications ──
Step "Verify Notifications (ORDER_CONFIRMATION + PAYMENT_CONFIRMATION)"
$notifOk = $false
for ($i = 1; $i -le 15; $i++) {
    Start-Sleep -Seconds 2
    $r = Get-Api "/api/notifications/user/$userId" (Auth $userToken)
    if ($r.status -eq 200) {
        $types = @($r.body | ForEach-Object { $_.type })
        $hasOrder = $types -contains "ORDER_CONFIRMATION"
        $hasPay   = $types -contains "PAYMENT_CONFIRMATION"
        Ok "Notifications: $($types -join ', ') (attempt $i/15)"
        if ($hasOrder -and $hasPay) {
            $notifOk = $true
            break
        }
    }
}
if (-not $notifOk) { Fail "Notifications never arrived after 30s" }

# ── Summary ──
Write-Host "`n=== ALL $step STEPS PASSED ===" -ForegroundColor Green
Write-Host "  Register:     Buyer $BUYER_EMAIL (id=$userId)" -ForegroundColor Gray
Write-Host "  Login:        Admin + Buyer tokens obtained" -ForegroundColor Gray
Write-Host "  Browse:       Product $productId visible in catalog" -ForegroundColor Gray
Write-Host "  Add to Cart:  Item added for user $userId" -ForegroundColor Gray
Write-Host "  Checkout:     Order $orderNumber created (id=$orderId)" -ForegroundColor Gray
Write-Host "  Payment:      $txnId SUCCESS" -ForegroundColor Gray
Write-Host "  Order Status: CONFIRMED" -ForegroundColor Gray
Write-Host "  Inventory:    Stock decreased 10 -> 9" -ForegroundColor Gray
Write-Host "  Notification: ORDER_CONFIRMATION + PAYMENT_CONFIRMATION delivered" -ForegroundColor Gray

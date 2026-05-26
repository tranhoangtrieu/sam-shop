# Sam Shop - E2E API test runner
# Usage: powershell -ExecutionPolicy Bypass -File scripts/run-e2e-tests.ps1

$ErrorActionPreference = "Stop"
$Gateway = "http://localhost:8088"
$Keycloak = "http://localhost:8180"
$results = [System.Collections.Generic.List[object]]::new()
$script:Vars = @{}

function Write-Log($msg) { Write-Host "[$(Get-Date -Format 'HH:mm:ss')] $msg" }

function Add-Result {
    param([string]$Id, [string]$Module, [string]$Name, [string]$Status, [string]$Detail = "")
    $results.Add([pscustomobject]@{ Id = $Id; Module = $Module; Name = $Name; Status = $Status; Detail = $Detail })
    $color = if ($Status -eq "PASS") { "Green" } elseif ($Status -eq "SKIP") { "Yellow" } else { "Red" }
    Write-Host "[$Status] $Id - $Name" -ForegroundColor $color
    if ($Detail) { Write-Host "       $Detail" -ForegroundColor DarkGray }
}

function Get-Token {
    param([string]$Username, [string]$Password = "123456")
    $body = @{
        grant_type = "password"
        client_id  = "sam-shop-ui"
        username   = $Username
        password   = $Password
    }
    $r = Invoke-RestMethod -Uri "$Keycloak/realms/sam-shop/protocol/openid-connect/token" `
        -Method POST -ContentType "application/x-www-form-urlencoded" -Body $body
    return $r.access_token
}

function Invoke-Api {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Url,
        [string]$Token = $null,
        [object]$Body = $null,
        [switch]$NoAuth
    )
    $headers = @{ "Content-Type" = "application/json" }
    if (-not $NoAuth -and $Token) { $headers["Authorization"] = "Bearer $Token" }
    $params = @{ Method = $Method; Uri = $Url; Headers = $headers }
    if ($Body -ne $null) { $params["Body"] = ($Body | ConvertTo-Json -Depth 10 -Compress) }
    return Invoke-RestMethod @params
}

function Assert-True {
    param([bool]$Cond, [string]$Msg)
    if (-not $Cond) { throw $Msg }
}

function Run-Test {
    param([string]$Id, [string]$Module, [string]$Name, [scriptblock]$Block)
    try {
        & $Block
        Add-Result -Id $Id -Module $Module -Name $Name -Status "PASS"
        return $true
    }
    catch {
        Add-Result -Id $Id -Module $Module -Name $Name -Status "FAIL" -Detail $_.Exception.Message
        return $false
    }
}

Write-Log "Sam Shop E2E tests - Gateway $Gateway"
Write-Log "Waiting for services..."
$ready = $false
for ($i = 0; $i -lt 45; $i++) {
    try {
        $r = Invoke-RestMethod -Uri "$Gateway/api/products" -Method GET
        if ($r.success -eq $true) { $ready = $true; break }
    }
    catch {
        try {
            $null = Invoke-RestMethod -Uri "http://localhost:8081/api/products" -Method GET
            $ready = $true
            break
        }
        catch { Start-Sleep -Seconds 2 }
    }
}
if (-not $ready) { throw "Gateway/product-service not ready after 90s" }

# --- AUTH ---
Run-Test "AUTH-01" "Auth" "Login customer1 (USER)" {
    $script:Vars["tokenUser"] = Get-Token "customer1"
    Assert-True ($script:Vars["tokenUser"].Length -gt 100) "No access_token"
}
Run-Test "AUTH-02" "Auth" "Login staff1 (EMPLOYEE)" {
    $script:Vars["tokenStaff"] = Get-Token "staff1"
    Assert-True ($script:Vars["tokenStaff"].Length -gt 100) "No access_token"
}
Run-Test "AUTH-03" "Auth" "Login admin1 (ADMIN)" {
    $script:Vars["tokenAdmin"] = Get-Token "admin1"
    Assert-True ($script:Vars["tokenAdmin"].Length -gt 100) "No access_token"
}
Run-Test "AUTH-04" "Auth" "Reject invalid credentials" {
    try {
        $null = Get-Token "customer1" "wrong-password"
        throw "Expected login failure"
    }
    catch {
        Assert-True ($_.Exception.Message -match "400|401|invalid|Unauthorized|error") "Unexpected: $($_.Exception.Message)"
    }
}

# --- PRODUCT ---
Run-Test "PRD-01" "Product" "GET list products (public)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/products" -NoAuth
    Assert-True $r.success "success=false"
    Assert-True ($r.data.Count -ge 1) "No seeded products"
    $script:Vars["productId"] = $r.data[0].id
}
Run-Test "PRD-02" "Product" "GET product by id (public)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/products/$($script:Vars['productId'])" -NoAuth
    Assert-True $r.success "success=false"
    Assert-True ($r.data.id -eq $script:Vars["productId"]) "Wrong product id"
}
Run-Test "PRD-03" "Product" "GET products with search filter" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/products?search=Tai" -NoAuth
    Assert-True $r.success "success=false"
}
Run-Test "PRD-04" "Product" "POST create product (EMPLOYEE)" {
    $body = @{
        name = "Test SP E2E $(Get-Date -Format 'HHmmss')"
        price = 99000
        description = "Created by E2E"
        quantity = 10
        status = "ACTIVE"
        categoryId = 1
    }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/products" -Token $script:Vars["tokenStaff"] -Body $body
    Assert-True $r.success "success=false"
    $script:Vars["newProductId"] = $r.data.id
}
Run-Test "PRD-05" "Product" "PUT update product (EMPLOYEE)" {
    $body = @{ name = "Test SP E2E Updated"; price = 89000; quantity = 8; status = "ACTIVE" }
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/products/$($script:Vars['newProductId'])" -Token $script:Vars["tokenStaff"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.price -eq 89000) "Price not updated"
}
Run-Test "PRD-06" "Product" "GET categories (public)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/products/categories" -NoAuth
    Assert-True $r.success "success=false"
    Assert-True ($r.data.Count -ge 1) "No categories"
}
Run-Test "PRD-07" "Product" "POST create category (EMPLOYEE)" {
    $body = @{ name = "E2E Category $(Get-Date -Format 'HHmmss')"; description = "E2E" }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/products/categories" -Token $script:Vars["tokenStaff"] -Body $body
    Assert-True $r.success "success=false"
}
Run-Test "PRD-08" "Product" "PUT adjust stock (public)" {
    $body = @{ delta = 5 }
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/products/$($script:Vars['productId'])/stock" -NoAuth -Body $body
    Assert-True $r.success "success=false"
}
Run-Test "PRD-09" "Product" "DELETE product (ADMIN)" {
    $r = Invoke-Api -Method DELETE -Url "$Gateway/api/products/$($script:Vars['newProductId'])" -Token $script:Vars["tokenAdmin"]
    Assert-True $r.success "success=false"
}

# --- CART ---
Run-Test "CRT-01" "Cart" "POST add to cart (USER)" {
    $body = @{ productId = $script:Vars["productId"]; quantity = 2 }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/cart/add" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.items.Count -ge 1) "Cart empty"
    $script:Vars["cartItemId"] = $r.data.items[0].id
}
Run-Test "CRT-02" "Cart" "GET cart by userId (USER)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/cart/1" -Token $script:Vars["tokenUser"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.userId -eq 1) "userId mismatch"
}
Run-Test "CRT-03" "Cart" "PUT update cart item (USER)" {
    $body = @{ itemId = $script:Vars["cartItemId"]; quantity = 3 }
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/cart/update" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.items[0].quantity -eq 3) "quantity not updated"
}

# --- ORDER COD ---
Run-Test "ORD-01" "Order" "POST create order COD (USER)" {
    $body = @{ shippingAddress = "123 Test St, HCM"; paymentMethod = "COD" }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/orders" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.status -eq "PENDING") "status not PENDING"
    Assert-True ($r.data.paymentStatus -eq "UNPAID") "payment UNPAID expected for COD"
    $script:Vars["codOrderId"] = $r.data.id
}
Run-Test "ORD-02" "Order" "GET my orders (USER)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/orders" -Token $script:Vars["tokenUser"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.Count -ge 1) "No orders"
}
Run-Test "ORD-03" "Order" "GET order by id (USER)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/orders/$($script:Vars['codOrderId'])" -Token $script:Vars["tokenUser"]
    Assert-True $r.success "success=false"
}
Run-Test "PAY-01" "Payment" "GET payment by orderId" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/payments/order/$($script:Vars['codOrderId'])" -Token $script:Vars["tokenUser"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.paymentMethod -eq "COD") "Not COD payment"
    $script:Vars["codPaymentId"] = $r.data.id
}
Run-Test "ORD-04" "Order" "PUT confirm order (EMPLOYEE)" {
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/orders/$($script:Vars['codOrderId'])/confirm" -Token $script:Vars["tokenStaff"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.status -eq "CONFIRMED") "Not CONFIRMED"
}
Run-Test "ORD-05" "Order" "PUT update status SHIPPING (EMPLOYEE)" {
    $body = @{ status = "SHIPPING" }
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/orders/$($script:Vars['codOrderId'])/status" -Token $script:Vars["tokenStaff"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.status -eq "SHIPPING") "Not SHIPPING"
}
Run-Test "ORD-06" "Order" "PUT update status COMPLETED (EMPLOYEE)" {
    $body = @{ status = "COMPLETED" }
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/orders/$($script:Vars['codOrderId'])/status" -Token $script:Vars["tokenStaff"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.status -eq "COMPLETED") "Not COMPLETED"
}
Run-Test "PAY-02" "Payment" "PUT confirm COD (EMPLOYEE)" {
    $r = Invoke-Api -Method PUT -Url "$Gateway/api/payments/$($script:Vars['codPaymentId'])/confirm-cod" -Token $script:Vars["tokenStaff"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.paymentStatus -eq "PAID") "COD not PAID"
}
Run-Test "ORD-07" "Order" "GET all orders (EMPLOYEE)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/orders/all" -Token $script:Vars["tokenStaff"]
    Assert-True $r.success "success=false"
    Assert-True ($r.data.Count -ge 1) "No orders for staff"
}
Run-Test "ORD-08" "Order" "GET revenue (ADMIN)" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/orders/revenue" -Token $script:Vars["tokenAdmin"]
    Assert-True $r.success "success=false"
    Assert-True ($null -ne $r.data.totalRevenue) "No revenue data"
}

# --- ORDER ONLINE ---
Run-Test "CRT-04" "Cart" "POST add to cart for ONLINE flow" {
    $body = @{ productId = $script:Vars["productId"]; quantity = 1 }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/cart/add" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
}
Run-Test "ORD-09" "Order" "POST create order ONLINE (USER)" {
    $body = @{ shippingAddress = "456 Online Ave, HCM"; paymentMethod = "ONLINE" }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/orders" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    $script:Vars["onlineOrderId"] = $r.data.id
}
Run-Test "PAY-03" "Payment" "POST initiate online payment" {
    $pay = Invoke-Api -Method GET -Url "$Gateway/api/payments/order/$($script:Vars['onlineOrderId'])" -Token $script:Vars["tokenUser"]
    $script:Vars["onlinePaymentId"] = $pay.data.id
    $body = @{ paymentId = $script:Vars["onlinePaymentId"] }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/payments/online/initiate" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($null -ne $r.data.paymentUrl -and "$($r.data.paymentUrl)".Length -gt 0) "No paymentUrl"
}
Run-Test "PAY-04" "Payment" "POST online callback success" {
    $body = @{ paymentId = $script:Vars["onlinePaymentId"]; success = $true }
    $r = Invoke-Api -Method POST -Url "$Gateway/api/payments/online/callback" -Token $script:Vars["tokenUser"] -Body $body
    Assert-True $r.success "success=false"
    Assert-True ($r.data.paymentStatus -eq "PAID") "Online not PAID"
}
Run-Test "ORD-10" "Order" "Verify ONLINE order paymentStatus PAID" {
    $r = Invoke-Api -Method GET -Url "$Gateway/api/orders/$($script:Vars['onlineOrderId'])" -Token $script:Vars["tokenUser"]
    Assert-True ($r.data.paymentStatus -eq "PAID") "Order paymentStatus not PAID"
}

# --- CART cleanup ---
Run-Test "CRT-05" "Cart" "DELETE remove cart item" {
    $cart = Invoke-Api -Method GET -Url "$Gateway/api/cart/1" -Token $script:Vars["tokenUser"]
    if ($cart.data.items.Count -eq 0) {
        $body = @{ productId = $script:Vars["productId"]; quantity = 1 }
        $cart = Invoke-Api -Method POST -Url "$Gateway/api/cart/add" -Token $script:Vars["tokenUser"] -Body $body
    }
    $itemId = $cart.data.items[0].id
    $r = Invoke-Api -Method DELETE -Url "$Gateway/api/cart/remove/$itemId" -Token $script:Vars["tokenUser"]
    Assert-True $r.success "success=false"
}

# --- NEGATIVE / AUTHZ ---
Run-Test "SEC-01" "Security" "USER cannot access revenue (403)" {
    try {
        $null = Invoke-Api -Method GET -Url "$Gateway/api/orders/revenue" -Token $script:Vars["tokenUser"]
        throw "Expected 403"
    }
    catch {
        Assert-True ($_.Exception.Message -match "403|Forbidden|401|Unauthorized") "Expected forbidden"
    }
}
Run-Test "SEC-02" "Security" "Unauthenticated cart add (401)" {
    try {
        $body = @{ productId = 1; quantity = 1 }
        $null = Invoke-Api -Method POST -Url "$Gateway/api/cart/add" -Body $body
        throw "Expected 401"
    }
    catch {
        Assert-True ($_.Exception.Message -match "401|Unauthorized") "Expected unauthorized"
    }
}

# --- SUMMARY ---
Write-Host ""
Write-Host "========== TEST SUMMARY ==========" -ForegroundColor Cyan
$pass = ($results | Where-Object Status -eq "PASS").Count
$fail = ($results | Where-Object Status -eq "FAIL").Count
$total = $results.Count
Write-Host "Total: $total | PASS: $pass | FAIL: $fail" -ForegroundColor $(if ($fail -eq 0) { "Green" } else { "Yellow" })

$reportPath = Join-Path $PSScriptRoot "..\docs\TEST_RESULTS.md"
$reportDir = Split-Path $reportPath -Parent
if (-not (Test-Path $reportDir)) { New-Item -ItemType Directory -Path $reportDir | Out-Null }

$md = @"
# Sam Shop - Ket qua test E2E

**Thời gian:** $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  
**Gateway:** $Gateway  
**Tổng:** $total | **PASS:** $pass | **FAIL:** $fail

| ID | Module | Test case | Kết quả | Ghi chú |
|----|--------|-----------|---------|---------|
"@
foreach ($row in $results) {
    $md += "| $($row.Id) | $($row.Module) | $($row.Name) | **$($row.Status)** | $($row.Detail) |`n"
}
$md | Set-Content -Path $reportPath -Encoding UTF8
Write-Host "Report saved: $reportPath"
if ($fail -gt 0) { exit 1 }

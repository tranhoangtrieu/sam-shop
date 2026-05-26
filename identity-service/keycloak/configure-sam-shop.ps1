# Configure Keycloak realm sam-shop for Sam Shop microservices
# Usage: .\identity-service\keycloak\configure-sam-shop.ps1

$ErrorActionPreference = 'Stop'
$KeycloakUrl = if ($env:KEYCLOAK_URL) { $env:KEYCLOAK_URL } else { 'http://localhost:8180' }
$AdminUser = if ($env:KC_ADMIN_USERNAME) { $env:KC_ADMIN_USERNAME } else { 'admin' }
$AdminPassword = if ($env:KC_ADMIN_PASSWORD) { $env:KC_ADMIN_PASSWORD } else { 'admin' }
$Realm = 'sam-shop'
$FePublicUrl = if ($env:FE_PUBLIC_URL) { $env:FE_PUBLIC_URL.TrimEnd('/') } else { 'http://localhost:4200' }

Write-Host "Keycloak: $KeycloakUrl"
Write-Host "Frontend URL (redirect): $FePublicUrl"

$tokenBody = @{
    client_id  = 'admin-cli'
    username   = $AdminUser
    password   = $AdminPassword
    grant_type = 'password'
}
$masterTokenUri = "$KeycloakUrl/realms/master/protocol/openid-connect/token"
$token = (Invoke-RestMethod -Uri $masterTokenUri -Method Post -Body $tokenBody -ContentType 'application/x-www-form-urlencoded').access_token
$headers = @{ Authorization = "Bearer $token"; 'Content-Type' = 'application/json' }

function Invoke-Kc {
    param([string]$Method, [string]$Path, $Body = $null, [switch]$AsArray)
    $uri = "$KeycloakUrl/admin$Path"
    if ($null -ne $Body) {
        $json = if ($AsArray) {
            @(,$Body) | ConvertTo-Json -Depth 10 -Compress
        } else {
            $Body | ConvertTo-Json -Depth 10 -Compress
        }
        return Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers -Body $json
    }
    Invoke-RestMethod -Uri $uri -Method $Method -Headers $headers
}

# 1. Create realm (skip if exists)
try {
    Invoke-Kc Get "/realms/$Realm" | Out-Null
    Write-Host "Realm $Realm already exists - updating settings"
    Invoke-Kc Put "/realms/$Realm" @{
        enabled = $true
        registrationAllowed = $false
        loginWithEmailAllowed = $true
        resetPasswordAllowed = $true
    } | Out-Null
} catch {
    Write-Host "Creating realm $Realm"
    Invoke-Kc Post '/realms' @{
        realm = $Realm
        enabled = $true
        displayName = 'Sam Shop'
        registrationAllowed = $false
        loginWithEmailAllowed = $true
        duplicateEmailsAllowed = $false
        resetPasswordAllowed = $true
        editUsernameAllowed = $false
        sslRequired = 'external'
    } | Out-Null
}

# 2. Realm roles
foreach ($role in @('USER', 'EMPLOYEE', 'ADMIN')) {
    try {
        Invoke-Kc Get "/realms/$Realm/roles/$role" | Out-Null
        Write-Host "Role $role exists"
    } catch {
        Invoke-Kc Post "/realms/$Realm/roles" @{ name = $role } | Out-Null
        Write-Host "Created role $role"
    }
}

# 3. Client sam-shop-ui
$clients = Invoke-Kc Get "/realms/$Realm/clients?clientId=sam-shop-ui"
$client = $clients | Select-Object -First 1
if (-not $client) {
    Invoke-Kc Post "/realms/$Realm/clients" @{
        clientId = 'sam-shop-ui'
        name = 'Sam Shop UI'
        enabled = $true
        publicClient = $true
        directAccessGrantsEnabled = $true
        standardFlowEnabled = $true
        rootUrl = $FePublicUrl
        baseUrl = $FePublicUrl
        redirectUris = @("$FePublicUrl/*")
        webOrigins = @($FePublicUrl, '+')
        protocol = 'openid-connect'
    } | Out-Null
    $clients = Invoke-Kc Get "/realms/$Realm/clients?clientId=sam-shop-ui"
    $client = $clients | Select-Object -First 1
    Write-Host 'Created client sam-shop-ui'
} else {
    Write-Host 'Client sam-shop-ui exists'
    Invoke-Kc Put "/realms/$Realm/clients/$($client.id)" @{
        clientId = 'sam-shop-ui'
        enabled = $true
        publicClient = $true
        directAccessGrantsEnabled = $true
        standardFlowEnabled = $true
        rootUrl = $FePublicUrl
        redirectUris = @("$FePublicUrl/*")
        webOrigins = @($FePublicUrl, '+')
    } | Out-Null
}
$clientId = $client.id

# 4. Protocol mapper: userId claim
$mappers = Invoke-Kc Get "/realms/$Realm/clients/$clientId/protocol-mappers/models"
$userIdMapper = $mappers | Where-Object { $_.name -eq 'userId-mapper' }
if (-not $userIdMapper) {
    Invoke-Kc Post "/realms/$Realm/clients/$clientId/protocol-mappers/models" @{
        name = 'userId-mapper'
        protocol = 'openid-connect'
        protocolMapper = 'oidc-usermodel-attribute-mapper'
        config = @{
            'user.attribute' = 'userId'
            'claim.name' = 'userId'
            'jsonType.label' = 'long'
            'id.token.claim' = 'true'
            'access.token.claim' = 'true'
            'userinfo.token.claim' = 'true'
        }
    } | Out-Null
    Write-Host 'Created userId protocol mapper'
}

# 5. Users
$users = @(
    @{ username = 'customer1'; email = 'customer1@samshop.local'; userId = '1'; roles = @('USER'); password = '123456' }
    @{ username = 'staff1'; email = 'staff1@samshop.local'; userId = '2'; roles = @('EMPLOYEE'); password = '123456' }
    @{ username = 'admin1'; email = 'admin1@samshop.local'; userId = '3'; roles = @('ADMIN'); password = '123456' }
)

foreach ($u in $users) {
    $userQuery = "/realms/$Realm/users?username=$($u.username)" + '&exact=true'
    $existing = Invoke-Kc Get $userQuery
    $user = $existing | Select-Object -First 1
    if (-not $user) {
        Invoke-Kc Post "/realms/$Realm/users" @{
            username = $u.username
            email = $u.email
            enabled = $true
            emailVerified = $true
            attributes = @{ userId = @($u.userId) }
        } | Out-Null
        $existing = Invoke-Kc Get $userQuery
        $user = $existing | Select-Object -First 1
        Write-Host "Created user $($u.username)"
    } else {
        Invoke-Kc Put "/realms/$Realm/users/$($user.id)" @{
            email = $u.email
            enabled = $true
            attributes = @{ userId = @($u.userId) }
        } | Out-Null
        Write-Host "Updated user $($u.username)"
    }

    # Password
    Invoke-Kc Put "/realms/$Realm/users/$($user.id)/reset-password" @{
        type = 'password'
        value = $u.password
        temporary = $false
    } | Out-Null

    # Roles
    $roleReps = @()
    foreach ($r in $u.roles) {
        $roleReps += Invoke-Kc Get "/realms/$Realm/roles/$r"
    }
    Invoke-Kc Post "/realms/$Realm/users/$($user.id)/role-mappings/realm" -Body $roleReps -AsArray | Out-Null
    Write-Host "  Assigned roles: $($u.roles -join ', ')"
}

Write-Host ''
Write-Host '=== Keycloak sam-shop configured ==='
Write-Host "Admin console: $KeycloakUrl/admin/master/console/#/sam-shop"
Write-Host 'Test login (customer1 / 123456):'
Write-Host "  Token URL: $KeycloakUrl/realms/$Realm/protocol/openid-connect/token"
Write-Host '  client_id=sam-shop-ui, grant_type=password'

# Verify token
$testBody = @{
    client_id = 'sam-shop-ui'
    username = 'customer1'
    password = '123456'
    grant_type = 'password'
}
$tokenUri = "$KeycloakUrl/realms/$Realm/protocol/openid-connect/token"
$testToken = (Invoke-RestMethod -Uri $tokenUri -Method Post -Body $testBody -ContentType 'application/x-www-form-urlencoded').access_token
$payload = $testToken.Split('.')[1]
$pad = '=' * ((4 - $payload.Length % 4) % 4)
$json = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($payload.Replace('-', '+').Replace('_', '/') + $pad))
Write-Host 'Sample JWT payload:'
Write-Host $json

# API Testing Guide for Money Transfer System

This guide provides curl commands to test the authentication and API endpoints.

## 1. User Registration (Signup)

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "john_doe",
    "email": "john@example.com",
    "password": "SecurePassword123!"
  }'
```

**Expected Response:**
```json
{
  "name": "john_doe",
  "email": "john@example.com"
}
```

## 2. User Login (Get JWT Token)

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "name": "john_doe",
    "password": "SecurePassword123!",
    "rememberMe": false
  }'
```

**Expected Response:**
```json
{
  "id": 1,
  "email": "john@example.com",
  "name": "john_doe",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "rememberToken": null
}
```

**Important:** Save the `token` value for authenticated requests.

## 3. Access Protected Endpoint with JWT Token

```bash
# Replace YOUR_JWT_TOKEN with the token from login response
curl -X GET http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

## 4. Access Protected Endpoint with BasicAuth

```bash
# Replace USERNAME and PASSWORD with actual credentials
curl -u john_doe:SecurePassword123! \
  http://localhost:8080/api/v1/accounts
```

## 5. Create Account (Requires Authentication)

```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "holderName": "John Doe",
    "bank_name": "My Bank",
    "ifsc_code": "MYBNK0001",
    "branch_name": "Downtown Branch",
    "email": "john@example.com",
    "contact": "+1234567890"
  }'
```

## 6. Login with Remember Token

First, login with rememberMe = true:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "name": "john_doe",
    "password": "SecurePassword123!",
    "rememberMe": true
  }'
```

Then use the remember token:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login-with-token \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_REMEMBER_TOKEN"
  }'
```

## 7. Forgot Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe"
  }'
```

## 8. Reset Password

```bash
curl -X POST http://localhost:8080/api/v1/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "token": "RECOVERY_TOKEN",
    "newPassword": "NewSecurePassword123!"
  }'
```

## 9. Check JWT Token Validity

```bash
# Test if token is still valid
curl -X GET http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer EXPIRED_OR_INVALID_TOKEN" \
  -v
```

**Expected Response (if expired):**
```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token is expired"
}
```

## Testing with Postman

### Import as Environment Variables

Create a new Postman environment with:

```json
{
  "name": "Money Transfer System",
  "values": [
    {
      "key": "BASE_URL",
      "value": "http://localhost:8080",
      "type": "default"
    },
    {
      "key": "JWT_TOKEN",
      "value": "",
      "type": "string"
    },
    {
      "key": "USERNAME",
      "value": "john_doe",
      "type": "string"
    },
    {
      "key": "PASSWORD",
      "value": "SecurePassword123!",
      "type": "string"
    }
  ]
}
```

### Workflow in Postman

1. **Create a request:** POST `{{BASE_URL}}/api/v1/auth/login`
2. **Body (JSON):**
   ```json
   {
     "name": "{{USERNAME}}",
     "password": "{{PASSWORD}}",
     "rememberMe": false
   }
   ```
3. **Tests Tab (Auto-save token):**
   ```javascript
   var jsonData = pm.response.json();
   pm.environment.set("JWT_TOKEN", jsonData.token);
   ```
4. **For authenticated requests, use header:**
   ```
   Authorization: Bearer {{JWT_TOKEN}}
   ```

## Authentication Troubleshooting

### Invalid Credentials Error
- Check username/password are correct
- Username is case-insensitive but password is case-sensitive
- Ensure signup was completed first

### JWT Token Expired
- Get a new token by logging in again
- Default expiration is 24 hours

### Unauthorized - Missing Token
- Ensure Authorization header includes "Bearer " prefix
- Format should be: `Authorization: Bearer <token>`

### BasicAuth Not Working
- Ensure username/password exist in database
- Password must match the bcrypt hash in database
- Format should be: `curl -u username:password`

## Security Notes

- ⚠️ Never share JWT tokens publicly
- ⚠️ Always use HTTPS in production
- ⚠️ Tokens should be sent in Authorization header, not in URL
- ⚠️ Passwords should be strong (at least 12 characters)
- ⚠️ Use BasicAuth only for internal/secure connections or testing

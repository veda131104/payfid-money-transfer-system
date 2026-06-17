# Money Transfer System - Authentication & MySQL Migration Summary

## Overview

This document summarizes all changes made to migrate from H2 database to MySQL and implement comprehensive authentication with BasicAuth, Bcrypt, and JWT.

## Changes Summary

### 1. Dependencies (pom.xml)

**Removed:**
- H2 Database dependency (commented out)

**Updated:**
- JWT library updated from `jjwt:0.9.1` to modern `jjwt-api/jjwt-impl/jjwt-jackson:0.12.3` for Spring Boot 3.x compatibility

**Already Present:**
- Spring Security
- Spring Boot Data JPA
- MySQL Connector/J
- Lombok
- Spring Boot Mail

### 2. Configuration (application.yml)

**Changed:**
```yaml
# OLD (H2)
datasource:
  url: jdbc:h2:mem:testdb
  driver-class-name: org.h2.Driver
  username: sa
  password: ""

# NEW (MySQL)
datasource:
  url: jdbc:mysql://localhost:3306/money_transfer_system
  driver-class-name: com.mysql.cj.jdbc.Driver
  username: root
  password: root
```

**Removed:**
- H2 console configuration
- SecurityAutoConfiguration exclusion

**Added:**
- MySQL-specific JPA property

### 3. Database Schema (schema.sql)

**Changes:**
- Fixed comment syntax for MySQL compatibility
- Added missing fields to `auth_users` table:
  - `recovery_token` VARCHAR(255)
  - `recovery_token_expiry` TIMESTAMP
  - `remember_token` VARCHAR(255)
  - `remember_token_expiry` TIMESTAMP
- Added indexes for performance
- Added `UNIQUE` constraint to `name` field

### 4. Security Components (New Classes)

#### A. JwtTokenProvider
**Location:** `src/main/java/com/company/mts/security/JwtTokenProvider.java`

**Functionality:**
- Generates JWT tokens with configurable expiration
- Validates JWT signatures using HMAC-SHA512
- Extracts username, userId, and email claims
- Handles token expiration and validation errors

**Configuration:**
```yaml
app:
  jwt:
    secret: "your-secret-key" # Default: secure generated key
    expiration: 86400000       # Default: 24 hours in milliseconds
```

#### B. CustomUserDetailsService
**Location:** `src/main/java/com/company/mts/security/CustomUserDetailsService.java`

**Functionality:**
- Implements Spring Security's UserDetailsService
- Loads user from database by username or userId
- Returns UserDetails with Bcrypt-encoded password

#### C. JwtAuthenticationFilter
**Location:** `src/main/java/com/company/mts/security/JwtAuthenticationFilter.java`

**Functionality:**
- Intercepts all requests
- Extracts JWT from "Authorization: Bearer" header
- Validates token and sets authentication context
- Runs before UsernamePasswordAuthenticationFilter

#### D. JwtAuthenticationEntryPoint
**Location:** `src/main/java/com/company/mts/config/JwtAuthenticationEntryPoint.java`

**Functionality:**
- Handles unauthorized access attempts
- Returns structured JSON error response

### 5. Updated Security Configuration

**Location:** `src/main/java/com/company/mts/config/SecurityConfig.java`

**Key Changes:**
- ✅ Implemented `PasswordEncoder` bean with BCryptPasswordEncoder
- ✅ Added `AuthenticationManager` bean
- ✅ Configured stateless session management
- ✅ Defined public endpoints (no auth required)
- ✅ Integrated JWT filter into security chain
- ✅ Enabled BasicAuth support
- ✅ Configured exception handling with JwtAuthenticationEntryPoint

### 6. Repository Update

**Location:** `src/main/java/com/company/mts/repository/AuthUserRepository.java`

**Added Method:**
```java
boolean existsByEmailIgnoreCase(String email);
```

## Authentication Methods Supported

### Method 1: JWT (Recommended for Frontend/Mobile)

**Flow:**
1. Signup → Create account in database
2. Login → Receive JWT token (valid 24 hours)
3. Subsequent requests → Include token in Authorization header

**Example:**
```bash
curl -H "Authorization: Bearer eyJhbGciOi..." http://localhost:8080/api/v1/accounts
```

### Method 2: BasicAuth (For Testing/Internal Use)

**Flow:**
1. Include username:password in Authorization header
2. Server validates against database (Bcrypt)

**Example:**
```bash
curl -u username:password http://localhost:8080/api/v1/accounts
```

### Method 3: Remember Token (For "Remember Me" Feature)

**Flow:**
1. Login with `rememberMe: true`
2. Receive remember token (30-day expiration)
3. Use token to auto-login later

## Authentication Flow Diagram

```
┌─────────────────────────────────────────────────────────┐
│ User Signup/Login Request                               │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ AuthService.signup() or AuthService.login()             │
│ - Validates credentials                                  │
│ - Bcrypt encodes/validates password                     │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ JwtTokenProvider.generateToken()                        │
│ - Creates JWT with claims (userId, email)              │
│ - Signs with secret key (HMAC-SHA512)                  │
│ - Sets 24-hour expiration                              │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ Return JWT Token to Client                              │
└────────────────┬────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
        ▼                 ▼
    Store JWT        Use JWT in Headers
        │            "Authorization: Bearer <token>"
        │                 │
        └─────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ Incoming Authenticated Request                          │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ JwtAuthenticationFilter                                 │
│ - Extracts token from header                           │
│ - Validates signature & expiration                     │
└────────────────┬────────────────────────────────────────┘
                 │
        ┌────────┴────────┐
        │                 │
      Valid           Invalid
        │                 │
        ▼                 ▼
┌───────────────┐   ┌─────────────────┐
│ Load User     │   │ Return 401      │
│ Set Auth      │   │ Unauthorized    │
│ Context       │   │                 │
└────────┬──────┘   └─────────────────┘
         │
         ▼
   ✅ Request Processed
```

## Public Endpoints (No Authentication)

```
POST   /api/v1/auth/signup
POST   /api/v1/auth/login
POST   /api/v1/auth/login-with-token
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password/**
```

## Protected Endpoints (Authentication Required)

```
All other /api/v1/** endpoints
```

## Deployment Checklist

- [ ] MySQL server installed and running
- [ ] Database created: `money_transfer_system`
- [ ] `application.yml` updated with correct MySQL credentials
- [ ] `app.jwt.secret` configured with strong random string
- [ ] `mvn clean package -DskipTests` builds successfully
- [ ] Application starts without errors
- [ ] Signup endpoint works
- [ ] Login returns valid JWT token
- [ ] Protected endpoints require authentication
- [ ] BasicAuth works correctly

## Files Modified/Created

### Modified Files:
- `pom.xml`
- `src/main/resources/application.yml`
- `src/main/resources/schema.sql`
- `src/main/java/com/company/mts/config/SecurityConfig.java`
- `src/main/java/com/company/mts/repository/AuthUserRepository.java`

### New Files:
- `src/main/java/com/company/mts/security/JwtTokenProvider.java`
- `src/main/java/com/company/mts/security/JwtAuthenticationFilter.java`
- `src/main/java/com/company/mts/security/CustomUserDetailsService.java`
- `src/main/java/com/company/mts/config/JwtAuthenticationEntryPoint.java`

### Documentation Files:
- `MIGRATION_SETUP.md` - Detailed setup instructions
- `API_TESTING.md` - API testing guide with curl/Postman examples
- `QUICK_START.md` - This file

## Next Steps

1. **Setup MySQL Database**
   - Run: `setup-mysql.bat` (Windows) or `setup-mysql.sh` (Linux/Mac)
   - Or manually create database and user

2. **Configure Credentials**
   - Update `src/main/resources/application.yml` with MySQL credentials
   - Set strong `app.jwt.secret` value

3. **Build Project**
   - Run: `mvn clean package -DskipTests`

4. **Start Application**
   - Run: `java -jar target/mts-0.0.1-SNAPSHOT.jar`

5. **Test API**
   - See `API_TESTING.md` for examples
   - Use Postman to import API endpoints

## Security Notes

⚠️ **Important for Production:**

1. Use HTTPS only
2. Set strong random JWT secret (min 32 characters)
3. Implement CORS properly if needed
4. Use HTTPS for MySQL connections
5. Implement rate limiting
6. Add request logging/auditing
7. Regularly update dependencies
8. Use environment variables for sensitive config
9. Implement refresh token mechanism (optional)
10. Add role-based access control if needed

## Troubleshooting

**Check:** `MIGRATION_SETUP.md` Troubleshooting section

**Common Issues:**
- MySQL connection refused → Check MySQL is running
- Table doesn't exist → Verify schema.sql was executed
- JWT token expired → Login again to get new token
- Unauthorized error → Check Authorization header format

## Support

For questions or issues:
1. Check the documentation files
2. Review application logs
3. Verify database connection
4. Test with curl commands from `API_TESTING.md`

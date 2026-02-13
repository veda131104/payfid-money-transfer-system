# Migration to MySQL and Authentication Setup

This document outlines the changes made to migrate from H2 to MySQL and implement authentication with BasicAuth, Bcrypt, and JWT.

## Changes Made

### 1. Database Migration (H2 → MySQL)
- **Updated application.yml**: Changed datasource configuration from H2 to MySQL
  - URL: `jdbc:mysql://localhost:3306/money_transfer_system`
  - Driver: `com.mysql.cj.jdbc.Driver`
  - Default credentials: `root`/`root` (change these in production!)

- **Updated pom.xml**: Removed H2 dependency (commented out), ensured MySQL driver is included

- **Updated schema.sql**: 
  - Fixed comment syntax for MySQL compatibility
  - Added missing fields to `auth_users` table (remember_token, recovery_token, etc.)

### 2. Authentication Implementation

#### Dependencies Added (pom.xml)
- Spring Security (already present)
- JWT libraries: `jjwt-api`, `jjwt-impl`, `jjwt-jackson` (v0.12.3) - modern JWT support for Spring Boot 3.x
- BCrypt for password encoding (included in Spring Security)

#### New Security Components Created

1. **JwtTokenProvider** (`src/main/java/com/company/mts/security/JwtTokenProvider.java`)
   - Generates JWT tokens with user claims
   - Validates JWT tokens
   - Extracts username, userId, and email from tokens
   - Configurable via application properties:
     - `app.jwt.secret`: JWT signing secret (default: secure generated key)
     - `app.jwt.expiration`: Token expiration time in ms (default: 86400000 = 24 hours)

2. **CustomUserDetailsService** (`src/main/java/com/company/mts/security/CustomUserDetailsService.java`)
   - Implements Spring Security's UserDetailsService interface
   - Loads user details from database by username or userId
   - Returns UserDetails with Bcrypt-encoded password

3. **JwtAuthenticationFilter** (`src/main/java/com/company/mts/security/JwtAuthenticationFilter.java`)
   - Runs on every request
   - Extracts JWT token from Authorization header (Bearer scheme)
   - Validates token using JwtTokenProvider
   - Sets authentication in SecurityContext

4. **JwtAuthenticationEntryPoint** (`src/main/java/com/company/mts/config/JwtAuthenticationEntryPoint.java`)
   - Handles unauthorized requests
   - Returns JSON response with 401 status

5. **Updated SecurityConfig** (`src/main/java/com/company/mts/config/SecurityConfig.java`)
   - Configures PasswordEncoder (BCryptPasswordEncoder)
   - Sets up stateless session management
   - Defines public endpoints (signup, login, forgot-password, reset-password)
   - Requires authentication for all other endpoints
   - Enables BasicAuth support
   - Integrates JWT filter into security chain

### 3. Password Hashing
- **Bcrypt** is automatically used by Spring Security for password encoding
- AuthService already uses `PasswordEncoder.encode()` for signup
- Password validation uses `PasswordEncoder.matches()` for login

## Setup Instructions

### Prerequisites
- MySQL Server 5.7 or higher
- Java 17
- Maven 3.8+

### Step 1: Create MySQL Database

```sql
-- Connect to MySQL as root
mysql -u root -p

-- Create database
CREATE DATABASE money_transfer_system;

-- Create a user for the application (optional, more secure)
CREATE USER 'mts_user'@'localhost' IDENTIFIED BY 'mts_password';
GRANT ALL PRIVILEGES ON money_transfer_system.* TO 'mts_user'@'localhost';
FLUSH PRIVILEGES;
```

### Step 2: Update application.yml Credentials

Update `src/main/resources/application.yml` with your MySQL credentials:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/money_transfer_system
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root          # or 'mts_user' if you created a separate user
    password: root          # update this to match your MySQL password
```

### Step 3: Configure JWT Secret (Optional)

Add to `src/main/resources/application.yml` for custom JWT configuration:

```yaml
app:
  jwt:
    secret: "your-very-secure-secret-key-at-least-32-characters-long-for-sha256"
    expiration: 86400000  # 24 hours in milliseconds
```

### Step 4: Build and Run

```bash
# Clean build
mvn clean package -DskipTests

# Run the application
java -jar target/mts-0.0.1-SNAPSHOT.jar

# Or using Maven
mvn spring-boot:run
```

## API Authentication

### Authentication Flow

1. **Signup**
   ```
   POST /api/v1/auth/signup
   Content-Type: application/json
   
   {
     "name": "john_doe",
     "email": "john@example.com",
     "password": "secure_password"
   }
   ```

2. **Login**
   ```
   POST /api/v1/auth/login
   Content-Type: application/json
   
   {
     "name": "john_doe",
     "password": "secure_password",
     "rememberMe": false
   }
   ```
   Response includes JWT token

3. **Authenticated Requests**
   - Add JWT token to subsequent requests:
   ```
   Authorization: Bearer <your_jwt_token>
   ```

4. **BasicAuth Alternative**
   - Use Basic Authentication instead of JWT:
   ```
   curl -u username:password http://localhost:8080/api/v1/accounts
   ```

## Public Endpoints (No Authentication Required)

- POST `/api/v1/auth/signup`
- POST `/api/v1/auth/login`
- POST `/api/v1/auth/login-with-token`
- POST `/api/v1/auth/forgot-password`
- POST `/api/v1/auth/reset-password/**`

## Protected Endpoints (Authentication Required)

- All other `/api/v1/**` endpoints

### Authentication Methods
- **JWT Bearer Token** (Recommended for frontend/mobile)
- **Basic Auth** (For testing or specific use cases)

## Troubleshooting

### "Connection refused" error
- Ensure MySQL is running: `mysql -u root -p`
- Check database name and credentials in application.yml

### "User not found" error when logging in
- Ensure user exists in auth_users table
- Check username case sensitivity

### JWT token expired
- Generate a new token by logging in again
- Adjust `app.jwt.expiration` if needed (current: 24 hours)

### Tables not created
- Check MySQL connection is working
- Verify schema.sql was executed
- Check logs for SQL errors

## Security Recommendations (Production)

1. **Change Default Credentials**: Update MySQL username/password
2. **Use Strong JWT Secret**: Generate a cryptographically secure random string
3. **Enable HTTPS**: Ensure all authentication endpoints use HTTPS
4. **Set Short Token Expiration**: Consider shorter expiration times
5. **Refresh Token Implementation**: Consider adding refresh token mechanism
6. **Database Backups**: Regular backups of MySQL database
7. **Audit Logging**: Log all authentication attempts

## Testing with curl

```bash
# Signup
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"name":"testuser","email":"test@example.com","password":"password123"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"name":"testuser","password":"password123"}'

# Protected endpoint with JWT
curl -H "Authorization: Bearer <your_token>" \
  http://localhost:8080/api/v1/accounts

# Protected endpoint with BasicAuth
curl -u testuser:password123 \
  http://localhost:8080/api/v1/accounts
```

## Notes

- All passwords in the database are stored as bcrypt-hashed values
- JWT tokens are stateless and signed with HMAC-SHA512
- Sessions are stateless (no session persistence)
- Remember tokens are stored in the database for "remember me" functionality

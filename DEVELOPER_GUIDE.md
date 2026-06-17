# Developer Guide - Authentication & Database System

## Architecture Overview

```
┌─────────────────────────────────────┐
│        Angular Frontend              │
│    (Signup/Login/Dashboard)         │
└────────────────┬────────────────────┘
                 │
                 │ HTTP/REST APIs
                 │
┌────────────────▼────────────────────┐
│      Spring Boot API Server         │
├─────────────────────────────────────┤
│  Controllers                        │
│  ├─ AuthController                 │
│  ├─ AccountController              │
│  └─ TransferController             │
├─────────────────────────────────────┤
│  Security Layer                     │
│  ├─ SecurityConfig                 │
│  ├─ JwtAuthenticationFilter         │
│  ├─ JwtTokenProvider               │
│  └─ CustomUserDetailsService       │
├─────────────────────────────────────┤
│  Services                           │
│  ├─ AuthService                    │
│  ├─ AccountService                 │
│  └─ TransactionService             │
├─────────────────────────────────────┤
│  Repositories (JPA)                 │
│  ├─ AuthUserRepository             │
│  ├─ AccountRepository              │
│  └─ TransactionRepository          │
├─────────────────────────────────────┤
│  Entities (ORM)                     │
│  ├─ AuthUser                       │
│  ├─ Account                        │
│  └─ TransactionLog                 │
└────────────────┬────────────────────┘
                 │
                 │ JDBC/JPA
                 │
┌────────────────▼────────────────────┐
│      MySQL Database                 │
├─────────────────────────────────────┤
│  Tables:                            │
│  ├─ auth_users                      │
│  ├─ accounts                        │
│  ├─ bank_details                    │
│  └─ transaction_logs               │
└─────────────────────────────────────┘
```

## Component Details

### 1. Authentication Flow

#### Signup Flow
```
User Input → SignupRequest DTO
    ↓
AuthController.signup()
    ↓
AuthService.signup()
    ├─ Validate email/username uniqueness
    ├─ Bcrypt encode password
    └─ Save to DB (auth_users table)
    ↓
Return AuthResponse
```

#### Login Flow
```
User Credentials → LoginRequest DTO
    ↓
AuthController.login()
    ↓
AuthService.login()
    ├─ Find user by username
    ├─ Bcrypt verify password
    ├─ Create remember token (if requested)
    └─ Save to DB
    ↓
JwtTokenProvider.generateToken()
    ├─ Create JWT with claims
    ├─ Sign with secret (HMAC-SHA512)
    └─ Return token
    ↓
LoginResponse (includes JWT token)
```

#### Protected Request Flow
```
Incoming Request with "Authorization: Bearer <token>"
    ↓
JwtAuthenticationFilter.doFilterInternal()
    ├─ Extract token from header
    ├─ Validate signature & expiration
    └─ Extract username/userId/email
    ↓
CustomUserDetailsService.loadUserByUsername()
    ├─ Query database for user
    └─ Return UserDetails with authorities
    ↓
Set SecurityContext Authentication
    ↓
Request proceeds to controller
```

### 2. Class Responsibilities

#### Security Classes

| Class | Purpose | Key Methods |
|-------|---------|------------|
| **JwtTokenProvider** | Token creation & validation | generateToken(), validateToken(), getUsernameFromToken() |
| **JwtAuthenticationFilter** | Intercepts requests | doFilterInternal(), getJwtFromRequest() |
| **CustomUserDetailsService** | Loads user from DB | loadUserByUsername(), loadUserById() |
| **SecurityConfig** | Spring Security setup | securityFilterChain(), passwordEncoder() |
| **JwtAuthenticationEntryPoint** | Unauthorized handler | commence() |

#### Service Classes

| Class | Purpose | Key Methods |
|-------|---------|------------|
| **AuthService** | Auth business logic | signup(), login(), resetPassword() |
| **AuthUserRepository** | Database access | findByNameIgnoreCase(), existsByEmailIgnoreCase() |

### 3. Password Security

#### Bcrypt Implementation

```
User Password: "MyPassword123"
    ↓
BCryptPasswordEncoder.encode()
    ↓
Bcrypt Hash (with salt): "$2a$10$N9qo8uL..."
    ↓
Store in Database
```

#### During Login

```
User Input: "MyPassword123"
Database Hash: "$2a$10$N9qo8uL..."
    ↓
BCryptPasswordEncoder.matches()
    ├─ Compare password with hash
    └─ Return true/false
```

### 4. JWT Token Structure

```
Header.Payload.Signature

Header: 
{
  "alg": "HS512",
  "typ": "JWT"
}

Payload:
{
  "sub": "john_doe",           // username
  "userId": 1,                 // user id
  "email": "john@example.com", // user email
  "iat": 1234567890,          // issued at
  "exp": 1234654290           // expiration (24h later)
}

Signature:
HMACSHA512(base64(header) + "." + base64(payload), secret_key)
```

## Configuration Guide

### 1. JWT Configuration (application.yml)

```yaml
app:
  jwt:
    # Secret key for signing JWT tokens (must be <= 32 chars for HS256, longer for HS512)
    secret: "your-super-secret-key-at-least-32-characters-for-security"
    
    # Token expiration time in milliseconds
    # 86400000 = 24 hours
    # 3600000  = 1 hour
    # 604800000 = 7 days
    expiration: 86400000
```

### 2. MySQL Configuration (application.yml)

```yaml
spring:
  datasource:
    # Database URL format: jdbc:mysql://host:port/database_name
    url: jdbc:mysql://localhost:3306/money_transfer_system
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: root  # Change in production!
    
    # Connection pooling
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
  
  jpa:
    hibernate:
      # Options: validate, update, create, create-drop
      # validate: Production setting (no schema changes)
      # update: Development setting (auto-updates schema)
      ddl-auto: update
    
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true
        generate_statistics: false
```

### 3. Security Configuration (SecurityConfig.java)

```java
// Endpoints that don't require authentication
- POST   /api/v1/auth/signup
- POST   /api/v1/auth/login
- POST   /api/v1/auth/login-with-token
- POST   /api/v1/auth/forgot-password
- POST   /api/v1/auth/reset-password/**

// All other endpoints require:
// 1. JWT Bearer Token, OR
// 2. BasicAuth (username:password), OR
// 3. Remember Token
```

## Development Workflows

### Adding a New Protected Endpoint

```java
@RestController
@RequestMapping("/api/v1/resource")
public class ResourceController {
    
    @GetMapping("/{id}")
    public ResponseEntity<MyResource> getResource(@PathVariable Long id) {
        // SecurityContext automatically contains authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Endpoint is automatically protected by JwtAuthenticationFilter
        return ResponseEntity.ok(resourceService.getById(id));
    }
}
```

### Accessing Current User

```java
@Service
public class MyService {
    
    public void doSomething() {
        // Get current authenticated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Or inject into controller
        @GetMapping
        public void example(Principal principal) {
            String username = principal.getName();
        }
    }
}
```

### Adding Custom Authentication Logic

```java
// To add permission checking with roles:
@Configuration
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/api/v1/admin/**")
                .hasRole("ADMIN")  // Requires ADMIN role
            .requestMatchers("/api/v1/user/**")
                .hasAnyRole("USER", "ADMIN")
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

## Testing Guide

### Unit Test Example

```java
@SpringBootTest
public class AuthServiceTest {
    
    @MockBean
    private AuthUserRepository repository;
    
    @InjectMocks
    private AuthService authService;
    
    @Test
    public void testSignupSuccess() {
        SignupRequest request = new SignupRequest();
        request.setName("testuser");
        request.setEmail("test@example.com");
        request.setPassword("password123");
        
        AuthUser result = authService.signup(request);
        
        assertNotNull(result);
        assertEquals("testuser", result.getName());
        verify(repository, times(1)).save(any(AuthUser.class));
    }
}
```

### Integration Test Example

```java
@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void testLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"user\",\"password\":\"pass\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
    }
}
```

## Debugging Tips

### 1. Enable Debug Logging

```yaml
logging:
  level:
    com.company.mts: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
```

### 2. Common Error Messages

| Error | Cause | Solution |
|-------|-------|----------|
| "JWT token is expired" | Token older than 24h | Login again to get new token |
| "Invalid JWT token" | Token signature invalid | Check JWT secret matches |
| "User not found" | Username doesn't exist | Signup first or check username |
| "Invalid username or password" | Credentials mismatch | Verify password exact case |
| "Connection refused" | MySQL server down | Start MySQL: `mysql -u root -p` |

### 3. Inspecting JWT Token

```bash
# Install jwt-cli
npm install -g jwt-cli

# Decode token (no verification)
jwt decode "your_token_here"

# Online decoder: https://jwt.io
```

## Performance Optimization

### 1. Database Indexes

Already configured in schema.sql:
```sql
CREATE INDEX idx_auth_users_email ON auth_users(email);
CREATE INDEX idx_auth_users_name ON auth_users(name);
```

### 2. Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 20000
```

### 3. JWT Caching

```java
// Consider caching user details after loading
@Cacheable(value = "users", key = "#username")
public UserDetails loadUserByUsername(String username) {
    return userDetailsService.loadUserByUsername(username);
}
```

## Migration Rollback (If Needed)

```bash
# Keep H2 database for backup
# Restore from schema.sql if needed:
# 1. Drop MySQL database
# 2. Uncomment H2 in pom.xml
# 3. Revert application.yml to H2 config
# 4. Rebuild and run

mvn clean install
```

## Production Checklist

- [ ] JWT secret is set to strong random value
- [ ] Database credentials are environment variables
- [ ] HTTPS is enabled for all endpoints
- [ ] CORS is properly configured
- [ ] Rate limiting is implemented
- [ ] Request logging is enabled
- [ ] SQL injection protection verified
- [ ] XSS protection enabled
- [ ] CSRF protection configured
- [ ] Error messages don't leak sensitive info
- [ ] Passwords never logged
- [ ] Database backups are automated
- [ ] Monitoring/alerts are set up

## References

- [Spring Security Documentation](https://spring.io/projects/spring-security)
- [JWT.io](https://jwt.io/)
- [MySQL Connector/J](https://dev.mysql.com/doc/connector-j/)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [Bcrypt](https://en.wikipedia.org/wiki/Bcrypt)

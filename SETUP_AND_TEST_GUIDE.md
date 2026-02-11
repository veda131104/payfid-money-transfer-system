# Money Transfer System - Setup & Testing Guide

## Prerequisites

1. **MySQL Server** - Ensure MySQL is running on `localhost:3306`
2. **Java 17+** - For running the Spring Boot application
3. **Maven** - For building the project
4. **Postman or cURL** - For testing API endpoints

## Step 1: Setup MySQL Database

```sql
-- Open MySQL command line and execute:
CREATE DATABASE money_transfer_system;
USE money_transfer_system;

-- The schema.sql will be executed automatically by Spring Boot (ddl-auto: update)
```

## Step 2: Configure Database Credentials

Edit `src/main/resources/application.yml`:
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/money_transfer_system
  driver-class-name: com.mysql.cj.jdbc.Driver
  username: root
  password: your_mysql_password  # Change this to your MySQL password
```

## Step 3: Build and Run the Application

```bash
# Clean and build
mvn clean install

# Run the application
mvn spring-boot:run

# Or run the JAR directly
java -jar target/mts-0.0.1-SNAPSHOT.jar
```

The application should start on `http://localhost:8080`

---

## Testing the API Endpoints

### 1. **Signup (Create New User)**

**Request:**
```http
POST http://localhost:8080/api/v1/auth/signup
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

**Example with cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "name": "John Doe",
  "email": "john@example.com"
}
```

---

### 2. **Login (Get JWT Token)**

**Request:**
```http
POST http://localhost:8080/api/v1/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Example with cURL:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "password123"
  }'
```

**Expected Response:**
```json
{
  "userId": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "token": "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiZW1haWwiOiJqb2huQGV4YW1wbGUuY29tIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNzEzMDAwMDAwLCJleHAiOjE3MTMwODY0MDB9.xxxxx",
  "tokenType": "Bearer"
}
```

**Save the token** - You'll need it for authenticated requests.

---

### 3. **Using the Token for Authenticated Requests**

For any endpoint that requires authentication, include the token in the Authorization header:

```http
GET http://localhost:8080/api/v1/accounts
Authorization: Bearer <your_token_here>
```

**Example with cURL:**
```bash
curl -X GET http://localhost:8080/api/v1/accounts \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

---

## Database Verification

Check that tokens are being saved in the database:

```sql
-- View all users
SELECT * FROM auth_users;

-- View all tokens
SELECT * FROM auth_tokens;

-- View token details for a user
SELECT * FROM auth_tokens WHERE user_id = 1;
```

---

## Common Issues & Solutions

### Issue: "Connection refused" for MySQL
**Solution:** Ensure MySQL is running
```bash
# Windows
net start MySQL80

# Or verify MySQL is running on port 3306
netstat -an | findstr 3306
```

### Issue: "Access denied for user 'root'@'localhost'"
**Solution:** Update the password in `application.yml` to match your MySQL password

### Issue: "Table doesn't exist"
**Solution:** This will be created automatically by Spring Boot when the app starts. Wait a moment and refresh.

### Issue: Invalid token error
**Solution:** 
- Ensure the token hasn't expired (valid for 24 hours)
- Copy the entire token without any extra whitespace
- Use the correct Authorization header format: `Bearer <token>`

---

## Next Steps

Once authentication is working:
1. ✅ Test account setup endpoints
2. ✅ Test transfer endpoints
3. ✅ Verify all endpoints require valid JWT token
4. ✅ Check that transactions are logged in database


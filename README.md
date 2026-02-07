# Money Transfer System (MTS) - Complete Implementation

## 🏦 Overview

A comprehensive Spring Boot banking application that implements a secure money transfer system with transaction logging, idempotency protection, and audit trails.

## ✅ Capstone Requirements - Complete Checklist

### Core Features
- ✅ **Account Management** - Create, Read, Update, Delete accounts
- ✅ **Money Transfer** - Transfer between accounts with validation
- ✅ **Transaction Logging** - Complete audit trail with balance snapshots
- ✅ **Idempotency** - Prevent duplicate transactions using idempotency keys
- ✅ **Account Status Management** - ACTIVE, LOCKED, CLOSED states
- ✅ **Duplicate Prevention** - One account per person (case-insensitive)

### Technical Implementation
- ✅ **AOP Logging** - Method execution tracking with LoggingAspect
- ✅ **Global Exception Handling** - Error codes (ACC-404, TRX-400, etc.)
- ✅ **API Versioning** - `/api/v1/...` endpoints
- ✅ **Proper DTOs** - Separate request/response objects
- ✅ **Database Schema** - schema.sql and data.sql
- ✅ **Unit Tests** - Service layer tests with Mockito
- ✅ **Validation** - JSR-303 validation on DTOs
- ✅ **Transaction Management** - @Transactional for data consistency

## 🗂️ Project Structure

```
mts-complete/
├── src/
│   ├── main/
│   │   ├── java/com/company/mts/
│   │   │   ├── Main.java
│   │   │   ├── aspect/
│   │   │   │   └── LoggingAspect.java
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   │   ├── AccountControllerV1.java
│   │   │   │   └── TransferController.java
│   │   │   ├── dto/
│   │   │   │   ├── AccountBalanceDTO.java
│   │   │   │   ├── IdempotentTransferRequest.java
│   │   │   │   └── TransactionDTO.java
│   │   │   ├── entity/
│   │   │   │   ├── Account.java
│   │   │   │   ├── AccountStatus.java
│   │   │   │   ├── TransactionLog.java
│   │   │   │   ├── TransactionStatus.java
│   │   │   │   └── TransactionType.java
│   │   │   ├── exception/
│   │   │   │   ├── DuplicateAccountException.java
│   │   │   │   ├── DuplicateTransactionException.java
│   │   │   │   ├── GlobalExceptionHandlerV1.java
│   │   │   │   ├── InactiveAccountException.java
│   │   │   │   ├── InsufficientBalanceException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── repository/
│   │   │   │   ├── AccountRepository.java
│   │   │   │   └── TransactionLogRepository.java
│   │   │   ├── service/
│   │   │   │   ├── AccountService.java
│   │   │   │   └── TransactionService.java
│   │   │   └── utils/
│   │   │       └── AccountNumberGenerator.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── schema.sql
│   │       └── data.sql
│   └── test/
│       └── java/com/company/mts/
│           └── service/
│               ├── AccountServiceTest.java
│               └── TransactionServiceTest.java
└── pom.xml
```

## 🚀 API Endpoints

### Account Management (v1)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/accounts` | Create new account |
| GET | `/api/v1/accounts/{id}` | Get account by ID |
| GET | `/api/v1/accounts/{id}/balance` | Get balance only |
| GET | `/api/v1/accounts/number/{accountNumber}` | Get by account number |
| GET | `/api/v1/accounts/holder/{holderName}` | Get by holder name |
| POST | `/api/v1/accounts/{id}/credit` | Deposit money |
| POST | `/api/v1/accounts/{id}/debit` | Withdraw money |
| POST | `/api/v1/accounts/{id}/lock` | Lock account |
| POST | `/api/v1/accounts/{id}/unlock` | Unlock account |
| POST | `/api/v1/accounts/{id}/close` | Close account |

### Transfer Management (v1)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/transfers/idempotent` | Execute idempotent transfer |
| GET | `/api/v1/transfers` | Get all transactions |
| GET | `/api/v1/transfers/{id}` | Get transaction by ID |
| GET | `/api/v1/transfers/account/{accountId}` | Get account history |
| GET | `/api/v1/transfers/account/{accountId}/failed` | Get failed transactions |

## 📋 Key Features Explained

### 1. Transaction Logging System
Every financial operation is logged with:
- Balance snapshots (before/after)
- Transaction status (PENDING → SUCCESS/FAILED)
- Failure reasons for audit
- Timestamp and description

### 2. Idempotency Protection
```json
{
  "fromAccountId": 1,
  "toAccountId": 2,
  "amount": 500.00,
  "idempotencyKey": "UNIQUE-KEY-12345",
  "description": "Payment for services"
}
```
- Prevents duplicate transfers
- Returns existing transaction if key reused
- Error code: `TRX-409`

### 3. AOP Logging
Automatically logs:
- Service method entry/exit
- Controller execution time
- Repository queries
- Transfer operations with metrics

### 4. Error Codes
| Code | Description |
|------|-------------|
| ACC-404 | Account not found |
| ACC-409 | Duplicate account |
| ACC-403 | Account inactive |
| TRX-400-BALANCE | Insufficient balance |
| TRX-409 | Duplicate transaction |
| VAL-400 | Validation error |
| SYS-500 | System error |

### 5. Banking Validations
- Minimum initial balance: $100
- Maximum transaction: $1,000,000
- One account per person (case-insensitive)
- Cannot transfer to same account
- Both accounts must be ACTIVE
- Cannot close account with balance

## 🧪 Testing

### Run Unit Tests
```bash
mvn test
```

### Test Coverage
- ✅ AccountService (11 tests)
- ✅ TransactionService (5 tests)
- Tests cover: success cases, exceptions, edge cases

### Sample Test Cases
- Create account with duplicate holder name
- Transfer with insufficient balance
- Idempotent transfer with duplicate key
- Close account with non-zero balance
- Lock/unlock account operations

## 🔧 Configuration

### application.yml
```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:mem:testdb
  jpa:
    hibernate:
      ddl-auto: update
  h2:
    console:
      enabled: true
```

### Database
- **H2 In-Memory** for development
- **MySQL** configured for production
- Auto-creates schema from entities
- Seed data included

## 📊 Database Schema

### accounts
- id, account_number (unique), holder_name
- balance, status, version (optimistic locking)
- created_at, last_updated

### transaction_logs
- id, from_account_id, to_account_id
- amount, type, status
- balance snapshots (before/after)
- idempotency_key (unique)
- failure_reason, transaction_date

## 🔐 Security Notes

Security is currently disabled for development:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
```

**For Production:**
- Enable Spring Security
- Add JWT authentication
- Implement role-based access control
- Add rate limiting
- Enable HTTPS

## 🚦 How to Run

```bash
# Build the project
mvn clean install

# Run the application
mvn spring-boot:run

# Access H2 Console
http://localhost:8080/h2-console

# API Documentation
http://localhost:8080/swagger-ui.html (if Swagger configured)
```

## 💡 Usage Examples

### Create Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "holderName": "John Doe",
    "initialBalance": 1000.00
  }'
```

### Execute Idempotent Transfer
```bash
curl -X POST http://localhost:8080/api/v1/transfers/idempotent \
  -H "Content-Type: application/json" \
  -d '{
    "fromAccountId": 1,
    "toAccountId": 2,
    "amount": 500.00,
    "idempotencyKey": "TX-2024-001",
    "description": "Payment"
  }'
```

### Get Account Balance
```bash
curl http://localhost:8080/api/v1/accounts/1/balance
```

### Get Transaction History
```bash
curl http://localhost:8080/api/v1/transfers/account/1
```

## 🎯 What Makes This Production-Ready

1. **Idempotency** - Network retry safe
2. **Audit Trail** - Complete transaction history
3. **Error Handling** - Structured error codes
4. **Logging** - AOP-based method tracking
5. **Validation** - Input validation at all levels
6. **Testing** - Comprehensive unit tests
7. **Documentation** - Clear API structure
8. **Versioning** - API v1 for future compatibility
9. **DTOs** - Proper separation of concerns
10. **Transaction Management** - ACID compliance

## 📝 Next Steps for Enhancement

- [ ] Add pagination for transaction history
- [ ] Implement JWT authentication
- [ ] Add account statement generation
- [ ] Create scheduled reports
- [ ] Add transfer limits and daily caps
- [ ] Implement two-factor authentication
- [ ] Add email notifications
- [ ] Create admin dashboard
- [ ] Add transaction reversal capability
- [ ] Implement currency conversion

## 👨‍💻 Author

Spring Boot Banking System - Capstone Project

## 📄 License

Educational Project - Free to use and modify

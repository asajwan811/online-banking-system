# Online Banking System

A production-ready Spring Boot banking application with JWT authentication, Redis caching, and atomic transactions.

## Features

- **JWT Authentication** - Stateless authentication with role-based access control (USER/ADMIN)
- **Account Management** - Create and manage SAVINGS, CURRENT, and CHECKING accounts
- **Atomic Transactions** - Fund transfers with pessimistic locking to prevent race conditions and deadlocks
- **Idempotency** - Duplicate transaction detection via idempotency keys
- **Redis Caching** - Account data cached in Redis for improved read performance
- **Swagger UI** - Interactive API documentation at `/swagger-ui.html`
- **Input Validation** - Request validation with meaningful error messages
- **Global Exception Handling** - Consistent error response format

## Technology Stack

- Java 17
- Spring Boot 3.2.0
- Spring Security + JWT (jjwt 0.11.5)
- Spring Data JPA (Hibernate)
- MySQL 8.0
- Redis 7
- springdoc-openapi 2.3.0 (Swagger)
- Lombok
- Maven

## Prerequisites

- Java 17+
- Maven 3.9+ (or use included `./mvnw`)
- MySQL 8.0
- Redis 7 (or Docker)

## Quick Start with Docker

```bash
# Start MySQL and Redis
docker-compose up mysql redis -d

# Build and run the application
./mvnw spring-boot:run
```

Or run everything including the app:

```bash
docker-compose up --build
```

## Running Tests

```bash
./mvnw test
```

Tests use H2 in-memory database and simple caching (no Redis required).

## Manual Setup

1. Create MySQL database:
```sql
CREATE DATABASE banking_db;
```

2. Update `src/main/resources/application.properties` with your credentials.

3. Start Redis on `localhost:6379`.

4. Run the app:
```bash
./mvnw spring-boot:run
```

## API Endpoints

### Authentication (Public)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and get JWT token |

### Users (Authenticated)
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users/me` | Get current user profile |
| GET | `/api/users` | List all users (ADMIN only) |
| GET | `/api/users/{id}` | Get user by ID (ADMIN only) |

### Accounts (Authenticated)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/accounts` | Create a new account |
| GET | `/api/accounts/my-accounts` | Get current user's accounts |
| GET | `/api/accounts/{id}` | Get account by ID |
| GET | `/api/accounts/number/{accountNumber}` | Get account by number |
| GET | `/api/accounts/user/{userId}` | Get accounts by user ID (ADMIN only) |
| PUT | `/api/accounts/{id}/freeze` | Freeze account (ADMIN only) |
| PUT | `/api/accounts/{id}/activate` | Activate account (ADMIN only) |

### Transactions (Authenticated)
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/transactions/transfer` | Transfer funds between accounts |
| POST | `/api/transactions/deposit/{accountNumber}` | Deposit funds |
| POST | `/api/transactions/withdraw/{accountNumber}` | Withdraw funds |
| GET | `/api/transactions/history/{accountNumber}` | Get transaction history (paginated) |
| GET | `/api/transactions/{transactionRef}` | Get transaction by reference |

## Example Usage

### Register
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Secret123!","email":"john@example.com","fullName":"John Doe"}'
```

### Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"Secret123!"}'
```

### Create Account
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"accountType":"SAVINGS","currency":"USD"}'
```

### Transfer Funds
```bash
curl -X POST http://localhost:8080/api/transactions/transfer \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"fromAccountNumber":"1234567890123456","toAccountNumber":"9876543210987654","amount":100.00,"description":"Payment","idempotencyKey":"unique-key-123"}'
```

## Security Notes

- Change the `jwt.secret` in `application.properties` before deploying to production
- Use environment variables for sensitive configuration in production
- The `BCryptPasswordEncoder` uses strength 12 — adjust based on performance requirements

## Swagger UI

Navigate to `http://localhost:8080/swagger-ui.html` after starting the application for interactive API documentation.

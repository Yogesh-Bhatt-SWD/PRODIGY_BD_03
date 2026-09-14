# User CRUD API — JWT Authentication & Role-Based Access Control

A **Spring Boot REST API** for managing `User` records, secured with **JWT authentication** and **role-based access control (RBAC)**, backed by **MySQL** with **Flyway** migrations. This project is **Task 3** for the Prodigy InfoTech Backend Development Internship, building on BD_02 by adding registration, login, and per-role authorization on top of the existing CRUD API.

## Features

- User registration and login with **JWT** token issuance
- Passwords hashed with **BCrypt**
- Stateless authentication via a custom `JwtAuthenticationFilter`
- **Role-based access control** with three roles: `ADMIN`, `OWNER`, `USER`
- Fine-grained authorization: users can view/update their own profile; admins/owners have broader access
- Server-side request validation (name, email format, password length, age range)
- Enforces unique email addresses
- Centralized exception handling with clean, structured JSON error responses
- Persistent **MySQL** storage with schema managed via **Flyway** migrations
- Database and JWT secrets loaded from a `.env` file (kept out of version control)

## Tech Stack

- **Java 17**
- **Spring Boot 3.3.0**
  - Spring Web
  - Spring Data JPA
  - Spring Validation
  - Spring Security
- **JJWT (io.jsonwebtoken)** — JWT creation and parsing
- **MySQL** (via `mysql-connector-j`)
- **Flyway** — database schema migrations
- **spring-dotenv** — loads config from a `.env` file
- **Maven**

## Project Structure

```
src/main/java/com/example/usercrudapi
├── UserCrudApiApplication.java     # Application entry point
├── config/
│   └── SecurityConfig.java         # Spring Security filter chain & RBAC rules
├── controller/
│   ├── AuthController.java         # /api/auth — register & login
│   └── UserController.java         # /api/users, /api/profile — CRUD + profile
├── service/
│   ├── AuthService.java            # Registration & login logic
│   └── UserService.java            # User CRUD business logic
├── security/
│   ├── JwtService.java             # JWT generation & validation
│   └── JwtAuthenticationFilter.java# Extracts & validates JWT per request
├── repository/
│   └── UserRepository.java         # Spring Data JPA repository
├── entity/
│   ├── User.java                   # User entity (implements UserDetails)
│   └── Role.java                   # ADMIN, OWNER, USER
├── dto/
│   ├── RegisterRequest.java
│   ├── AuthRequest.java
│   └── AuthResponse.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java
    └── DuplicateResourceException.java

src/main/resources/
├── application.properties          # DataSource, JPA, Flyway, and JWT config
└── db/migration/
    └── V1__create_users_table.sql  # users table (adds password & role columns)
```

## Getting Started

### Prerequisites

- Java 17 or higher
- Maven (or use the included Maven wrapper, if present)
- A running MySQL server

### 1. Create the database

```sql
CREATE DATABASE user_crud_db;
```

### 2. Configure environment variables

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

```env
DB_URL=jdbc:mysql://localhost:3306/user_crud_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password
JWT_SECRET=your_base64_encoded_secret_key_at_least_256_bits
```

> `.env` is git-ignored — never commit real credentials or secrets. `JWT_SECRET` must be a Base64-encoded key of at least 256 bits (32 bytes).

### 3. Run the application

```bash
git clone https://github.com/Yogesh-Bhatt-SWD/PRODIGY_BD_03.git
cd PRODIGY_BD_03
mvn spring-boot:run
```

On startup, Flyway automatically applies `V1__create_users_table.sql` to create the `users` table. The API starts on `http://localhost:8080`.

## Authentication

### Register

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Jane Doe", "email": "jane.doe@example.com", "password": "secret123", "age": 28}'
```

New users are registered with the `USER` role.

### Login

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "jane.doe@example.com", "password": "secret123"}'
```

Both endpoints return a JWT:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "id": "b3f1...",
  "name": "Jane Doe",
  "email": "jane.doe@example.com",
  "role": "USER"
}
```

Include the token on subsequent requests:

```bash
curl http://localhost:8080/api/profile \
  -H "Authorization: Bearer <token>"
```

## API Endpoints

| Method | Endpoint            | Access                              | Description                       |
| ------ | -------------------- | ------------------------------------ | ---------------------------------- |
| POST   | `/api/auth/register`| Public                               | Register a new user (role `USER`) |
| POST   | `/api/auth/login`   | Public                               | Authenticate and receive a JWT    |
| GET    | `/api/profile`      | Authenticated                        | Get the current user's own profile|
| GET    | `/api/users`         | `ADMIN`, `OWNER`                     | Get all users                      |
| GET    | `/api/users/{id}`    | `ADMIN`, `OWNER`, or self            | Get a single user by ID            |
| POST   | `/api/users`          | `ADMIN`                              | Create a new user directly         |
| PUT    | `/api/users/{id}`    | Authenticated (self-scope in service)| Update an existing user            |
| DELETE | `/api/users/{id}`    | `ADMIN`                              | Delete a user                      |

### Validation rules

- `name`: required, 2–100 characters
- `email`: required, valid email address, must be unique
- `password`: required, minimum 6 characters (never returned in responses)
- `age`: required, between 0 and 150

### Error Responses

Errors are returned as structured JSON, for example:

```json
{
  "timestamp": "2026-09-14T10:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id: <id>"
}
```

Handled cases include:
- **401 Unauthorized** — missing/invalid credentials or JWT
- **403 Forbidden** — authenticated but lacking the required role
- **404 Not Found** — user does not exist
- **409 Conflict** — email already in use
- **400 Bad Request** — validation failures (with per-field error messages)

## Database Schema

Managed by Flyway (`V1__create_users_table.sql`):

```sql
CREATE TABLE IF NOT EXISTS users (
    id       BINARY(16)   NOT NULL,
    name     VARCHAR(100) NOT NULL,
    email    VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    age      INT          NOT NULL,
    role     VARCHAR(20)  NOT NULL DEFAULT 'USER',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    CONSTRAINT chk_users_age CHECK (age >= 0 AND age <= 150)
);
```

## About

This project is part of the [Prodigy InfoTech](https://prodigyinfotech.dev/) Backend Development internship program.

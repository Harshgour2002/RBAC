# RBAC In-Depth Flow Guide (Beginner Friendly)

This document explains how this branch works end-to-end: authentication, authorization, JWT, refresh-token rotation, roles/permissions, and protected endpoints.

---

## 1) Big Picture: What this project does

This is a Spring Boot authentication + authorization module with:

- **Authentication** via email/password (`/api/auth/login`, `/api/auth/signup`)
- **Access token (JWT)** for calling protected APIs
- **Refresh token** stored in DB and rotated on every refresh call
- **RBAC** (Role-Based Access Control) using roles + permissions
- **Method-level security** using `@PreAuthorize(...)`

---

## 2) Core architecture and responsibilities

### 2.1 Controllers

- `AuthController` handles signup/login/refresh endpoints.
- `SampleProtectedController` demonstrates role/permission protected endpoints.

### 2.2 Services

- `AuthService` orchestrates signup, login, refresh flow.
- `RefreshTokenService` creates/verifies/rotates refresh tokens.
- `JwtService` signs and validates access JWTs.

### 2.3 Security layer

- `SecurityConfig` sets stateless security and installs JWT filter.
- `JwtAuthenticationFilter` extracts `Authorization: Bearer <token>` and sets user auth in `SecurityContext`.
- `CustomUserDetailsService` loads user by email from DB.
- `CustomUserDetails` converts DB roles+permissions into Spring authorities.

### 2.4 Persistence layer

Entities and relations:

- `User` ↔ `Role` (**many-to-many**) via `user_roles`
- `Role` ↔ `Permission` (**many-to-many**) via `role_permissions`
- `RefreshToken` → `User` (**many-to-one**)

Repositories:

- `UserRepository`, `RoleRepository`, `PermissionRepository`, `RefreshTokenRepository`

### 2.5 Startup seeding

`DataSeeder` ensures default roles/permissions exist and maps permissions to USER/ADMIN roles.

---

## 3) Request lifecycle (how auth really works)

### 3.1 Public endpoints

Only `/api/auth/**` is public. Everything else needs authentication.

So these are open:
- `/api/auth/signup`
- `/api/auth/login`
- `/api/auth/refresh`

Everything under `/api/protected/**` requires valid auth context.

### 3.2 Protected request flow

When client calls a protected endpoint:

1. Client sends `Authorization: Bearer <accessToken>` header.
2. `JwtAuthenticationFilter` checks header.
3. Filter extracts email from token using `JwtService`.
4. Loads user via `CustomUserDetailsService`.
5. Validates token + expiry.
6. Builds `UsernamePasswordAuthenticationToken` with authorities.
7. Stores auth into `SecurityContextHolder`.
8. Now `@PreAuthorize` checks pass/fail based on role/permission.

If header missing/invalid, auth context is not set and protected routes fail with unauthorized/forbidden response.

---

## 4) Signup flow

Endpoint: `POST /api/auth/signup`

High-level:

1. Validate request.
2. Check email uniqueness.
3. Fetch default `USER` role from DB.
4. Save new user with encoded password.
5. Generate access token.
6. Create refresh token row in DB.
7. Return both tokens in `AuthResponse`.

Returned payload contains:

- `accessToken`
- `refreshToken`
- `tokenType` (`Bearer`)
- `expiresIn`
- resolved `roles`
- resolved `permissions`

---

## 5) Login flow

Endpoint: `POST /api/auth/login`

1. Authenticate with `AuthenticationManager` (email + password).
2. Load user.
3. Delete all existing refresh tokens for that user (single-session style refresh lifecycle).
4. Generate fresh access token.
5. Create a new refresh token.
6. Return `AuthResponse`.

Important behavior:
- After login, previously issued refresh token for same user is invalid.

---

## 6) Refresh-token flow (rotation)

Endpoint: `POST /api/auth/refresh`

Request body:

```json
{
  "refreshToken": "<token>"
}
```

Processing:

1. Find refresh token in DB.
2. Check expiry.
3. If expired: delete row and throw token-expired error.
4. If valid: **rotate** token:
   - delete old refresh token
   - create new refresh token
5. Create new access token.
6. Return both new access + new refresh tokens.

Important behavior:
- Old refresh token cannot be reused after a successful refresh.

---

## 7) RBAC model used here

### 7.1 Role names

Current role enum includes:
- `USER`
- `ADMIN`
- `STUDENT`
- `FACULTY`

### 7.2 Permission names

Current permission enum includes:
- `USER_VIEW`
- `ADMIN_DASHBOARD`

### 7.3 Seeded defaults

On startup:

- USER role gets `USER_VIEW`
- ADMIN role gets `USER_VIEW` + `ADMIN_DASHBOARD`

This means ADMIN inherits more capabilities via permissions mapping.

---

## 8) Endpoint authorization matrix

### Auth endpoints

- `POST /api/auth/signup` → public
- `POST /api/auth/login` → public
- `POST /api/auth/refresh` → public (uses refresh token from body, not access JWT)

### Protected demo endpoints

- `GET /api/protected/user-endpoint` → requires `ROLE_USER`
- `GET /api/protected/user-browser-endpoint` → requires `ROLE_USER` and **must NOT** have `ROLE_ADMIN`
- `GET /api/protected/admin-endpoint` → requires `ROLE_ADMIN`
- `GET /api/protected/admin-browser-endpoint` → requires `ROLE_ADMIN`
- `GET /api/protected/admin-dashboard` → requires authority `ADMIN_DASHBOARD`

---

## 9) Why token in URL does not work in current code

Current JWT filter reads token only from:

- HTTP Header: `Authorization: Bearer <token>`

It does **not** read query parameters like `?token=...`.

So for Flutter, Postman, browser-extension based testing — always send Authorization header.

---

## 10) Beginner testing walkthrough (safe sequence)

1. Signup with new email.
2. Save returned `accessToken`, `refreshToken`.
3. Call `user-endpoint` with user access token.
4. Call refresh with refresh token and replace tokens with returned ones.
5. Try refresh again using old refresh token (should fail).
6. Map admin role in DB for same user (or create admin user).
7. Login again to get admin token.
8. Call admin endpoints with admin token.
9. Call user-browser-endpoint with admin token (should fail due to strict expression).

---

## 11) Typical errors and what they mean

- **400 Validation failed**: missing/wrong request field names.
- **401 Invalid username/password**: bad login credentials.
- **401 token expired**: refresh or JWT expired.
- **404 refresh token not found**: old/rotated/invalid refresh token.
- **403 forbidden**: authenticated but lacks required role/permission.

---

## 12) How this maps to Flutter client

For protected APIs, call with headers:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

If access token expires:

1. Call `/api/auth/refresh` with refresh token in body.
2. Store new tokens.
3. Retry original request.

Do not append access token in URL for this backend implementation.

---

## 13) Environment/config overview

Key config values in `application.yaml`:

- MySQL datasource
- JWT secret (Base64)
- access token expiry
- refresh token expiry

This config controls token lifetime and DB persistence behavior.

---

## 14) Practical extension points

If you want to evolve this system:

1. Add feature-specific permissions (e.g., `COURSE_CREATE`, `GRADE_UPDATE`).
2. Build role management APIs (assign/revoke roles).
3. Add audit logs for auth and role changes.
4. Add refresh-token metadata (device, ip, user-agent).
5. Move from role names in DB to domain-specific policies.

---

## 15) Final mental model (for beginners)

- **Login/Signup** gives you two keys:
  - short key = access token (fast, expires sooner)
  - long key = refresh token (used only to get new short key)
- **RBAC** is the gate:
  - Roles and permissions are attached to user
  - `@PreAuthorize` checks gate rule on each protected endpoint
- **Refresh rotation** is safety:
  - each refresh invalidates old refresh token
  - stolen old refresh token cannot be reused

If you keep these 3 concepts clear, the whole flow becomes easy to reason about.
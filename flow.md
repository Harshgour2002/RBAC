# Project Flow (End-to-End)

This file explains the full working flow of this RBAC project from startup to protected API access.

## 1) Application startup flow

1. Spring Boot app starts (`RbacApplication`).
2. Security config loads and sets stateless JWT-based auth.
3. `DataSeeder` runs and ensures default permissions and roles are present.
4. Role-permission mapping is updated:
   - USER -> USER_VIEW
   - ADMIN -> USER_VIEW, ADMIN_DASHBOARD

## 2) Data model flow

Core entities:

- `User`
- `Role`
- `Permission`
- `RefreshToken`

Relationships:

- User <-> Role (many-to-many)
- Role <-> Permission (many-to-many)
- RefreshToken -> User (many-to-one)

Meaning:

- A user can have multiple roles.
- Each role can have multiple permissions.
- Refresh tokens are stored in DB and tied to a user.

## 3) Authentication flow

### 3.1 Signup (`POST /api/auth/signup`)

1. Validate incoming request.
2. Check if email already exists.
3. Load default role `USER`.
4. Encode password and save user.
5. Generate JWT access token.
6. Create refresh token in DB.
7. Return access token + refresh token + roles + permissions.

### 3.2 Login (`POST /api/auth/login`)

1. Authenticate email/password using `AuthenticationManager`.
2. Load user from DB.
3. Delete old refresh token(s) for user.
4. Generate new access token.
5. Generate new refresh token.
6. Return both tokens and authorities.

## 4) Token refresh flow (`POST /api/auth/refresh`)

1. Receive refresh token in request body.
2. Find token in DB.
3. Check expiry:
   - If expired, delete and return token error.
4. Rotate token:
   - Delete old refresh token.
   - Create a new refresh token.
5. Generate new access token.
6. Return new access + new refresh token.

Important:

- Old refresh token cannot be reused after successful refresh.

## 5) Request authorization flow for protected APIs

For every `/api/protected/**` call:

1. Client sends `Authorization: Bearer <accessToken>` header.
2. `JwtAuthenticationFilter` reads this header.
3. Extracts username/email from JWT.
4. Loads user details from DB.
5. Validates token.
6. Sets authentication in `SecurityContext`.
7. Controller method-level rule (`@PreAuthorize`) is evaluated.
8. If rule passes -> endpoint response returned.
9. If rule fails -> 403 Forbidden.

If header missing/invalid -> request is unauthenticated (401/403 depending on flow).

## 6) RBAC endpoint flow

### Auth endpoints (public)

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/refresh`

### Protected demo endpoints

- `GET /api/protected/user-endpoint`
  - Rule: `hasRole('USER')`
- `GET /api/protected/user-browser-endpoint`
  - Rule: `hasRole('USER') and !hasRole('ADMIN')`
  - Returns plain text `user point`
- `GET /api/protected/admin-endpoint`
  - Rule: `hasRole('ADMIN')`
- `GET /api/protected/admin-browser-endpoint`
  - Rule: `hasRole('ADMIN')`
  - Returns plain text `admin point`
- `GET /api/protected/admin-dashboard`
  - Rule: `hasAuthority('ADMIN_DASHBOARD')`

## 7) Error handling flow

Global handler maps exceptions to clean API responses:

- Validation error -> 400
- Bad credentials -> 401
- Token expired/JWT error -> 401
- Resource not found -> 404
- Illegal argument -> 400
- Unknown/unhandled -> 500

## 8) Client integration flow (Flutter/Web/Postman)

1. Signup/Login and store both tokens.
2. Use access token in `Authorization` header for protected APIs.
3. On access token expiry:
   - call `/api/auth/refresh` with refresh token body
   - replace old stored tokens with new ones
   - retry failed request
4. Never rely on token in URL query for this backend.

## 9) Practical sequence to verify complete flow

1. Signup user.
2. Call `user-endpoint` with user access token (expect success).
3. Call `admin-endpoint` with user token (expect 403).
4. Refresh token (expect new token pair).
5. Retry refresh with old token (expect failure).
6. Assign admin role in DB and login again.
7. Call admin endpoints (expect success).
8. Call strict user-browser endpoint with admin token (expect 403).

This confirms authentication + authorization + refresh rotation are all functioning together.
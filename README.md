# RBAC API - Testing Steps

## Endpoints added
- `GET /api/protected/user-endpoint` → only `ROLE_USER` can access
- `GET /api/protected/user-browser-endpoint` → only pure user (has USER and not ADMIN), returns plain text `user point`
- `GET /api/protected/admin-endpoint` → only `ROLE_ADMIN` can access
- `GET /api/protected/admin-browser-endpoint` → only `ROLE_ADMIN`, returns plain text `admin point`
- `POST /api/auth/refresh` → refresh token rotation

## 1) Run app
```bash
./mvnw spring-boot:run
```
Base URL: `http://localhost:8080`

## 2) Postman environment variables
Create these vars:
- `baseUrl = http://localhost:8080`
- `userAccessToken`
- `userRefreshToken`
- `adminAccessToken`

## 3) Signup (normal USER)
**POST** `{{baseUrl}}/api/auth/signup`
```json
{
  "email": "user1@example.com",
  "password": "Password@123"
}
```
Save:
- `data.accessToken` -> `userAccessToken`
- `data.refreshToken` -> `userRefreshToken`

## 4) Test refresh endpoint
**POST** `{{baseUrl}}/api/auth/refresh`
```json
{
  "refreshToken": "{{userRefreshToken}}"
}
```
Expected:
- new `accessToken`
- new `refreshToken`

Update vars again:
- `userAccessToken = data.accessToken`
- `userRefreshToken = data.refreshToken`

Negative check:
- Retry `/api/auth/refresh` with old refresh token -> should fail.

## 5) Test USER endpoint (Postman)
**GET** `{{baseUrl}}/api/protected/user-endpoint`
Header:
- `Authorization: Bearer {{userAccessToken}}`

Expected: `200` and `data = USER_ENDPOINT_OK`

## 6) Test USER browser endpoint (User only, not Admin)
This endpoint is strict: it allows USER role and blocks ADMIN role.

**URL**
- `http://localhost:8080/api/protected/user-browser-endpoint`

### Browser steps
1. Login/signup from Postman and copy user token in `userAccessToken`.
2. Add header in browser using extension (ModHeader / Header Editor):
   - `Authorization: Bearer <userAccessToken>`
3. Open URL above.

Expected text in browser:
- `user point`

If token belongs to admin (or user+admin), expected:
- `403`

## 7) Prepare ADMIN user (DB role mapping)
Signup creates USER role by default. To test admin endpoints, map admin role in DB.

```sql
SELECT id, email FROM users WHERE email='user1@example.com';
SELECT id, name FROM roles WHERE name IN ('USER','ADMIN');
DELETE FROM user_roles WHERE user_id=<user_id>; -- optional
INSERT INTO user_roles(user_id, role_id) VALUES (<user_id>, <admin_role_id>);
```

Now login again:

**POST** `{{baseUrl}}/api/auth/login`
```json
{
  "email": "user1@example.com",
  "password": "Password@123"
}
```
Save `data.accessToken` as `adminAccessToken`.

## 8) Test ADMIN endpoint (Postman)
**GET** `{{baseUrl}}/api/protected/admin-endpoint`
Header:
- `Authorization: Bearer {{adminAccessToken}}`

Expected: `200` and `data = ADMIN_ENDPOINT_OK`

## 9) Test ADMIN browser endpoint (Browser)
1. Use admin token in Authorization header.
2. Open:
   - `http://localhost:8080/api/protected/admin-browser-endpoint`

Expected text:
- `admin point`

## 10) Cross-check
- Call `/api/protected/admin-endpoint` with `userAccessToken` -> `403`
- Call `/api/protected/user-browser-endpoint` with `adminAccessToken` -> `403`
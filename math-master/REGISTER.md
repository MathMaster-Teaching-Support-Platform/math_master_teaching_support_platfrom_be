# Registration Flow — Frontend Implementation Guide

Full end-to-end flow the frontend must implement for account registration and email confirmation.

---

## Flow Overview

```
User fills form → POST /api/auth/register → Backend saves user (INACTIVE) + sends email
                                                              ↓
                            User receives email → clicks link → FE /confirm-email?token=...
                                                              ↓
                            FE calls GET /api/auth/confirm-email?token=... → Account = ACTIVE
                                                              ↓
                            FE redirects to login → User logs in
```

---

## Step 1 — Register Form

**Page:** `/register`

### Fields

| Field      | Type     | Validation                                                                                                    |
| ---------- | -------- | ------------------------------------------------------------------------------------------------------------- |
| `userName` | `string` | Required · 3–50 characters                                                                                    |
| `email`    | `string` | Required · valid email · max 50 characters                                                                    |
| `password` | `string` | Required · 8–128 characters · must contain uppercase, lowercase, digit, and special character (`!@#$%^&*...`) |

### API Call

```
POST /api/auth/register
Content-Type: application/json
```

**Request body:**

```json
{
  "userName": "john_doe",
  "email": "john@example.com",
  "password": "Abcd@1234"
}
```

**Success response — HTTP 200:**

```json
{
  "code": 1000,
  "result": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "john_doe",
    "email": "john@example.com",
    "status": "INACTIVE",
    "createdDate": "2026-03-25T10:00:00Z",
    "createdBy": "john_doe",
    "updatedDate": "2026-03-25T10:00:00Z",
    "updatedBy": "john_doe"
  }
}
```

Note: `status` will always be `"INACTIVE"` at this point — the account is not yet usable.

**Error responses:**

| `code`            | Meaning                          | HTTP |
| ----------------- | -------------------------------- | ---- |
| `1002`            | Username already taken           | 400  |
| `1013`            | Email already registered         | 400  |
| Validation errors | See field validation table above | 400  |

### After Success

Show the user a message:

> "Registration successful! Please check your inbox and click the confirmation link to activate your account."

Do **not** log the user in or redirect to a dashboard yet.

---

## Step 2 — Email Confirmation Page

**Page:** `/confirm-email`

The user receives an email containing a link like:

```
http://localhost:3000/confirm-email?token=<JWT>
```

When the user lands on this page, the FE must:

1. Read the `token` query parameter from the URL.
2. Call the backend to activate the account.
3. Handle the response and redirect.

### API Call

```
GET /api/auth/confirm-email?token=<TOKEN>
```

No auth header required — this endpoint is public.

**Success response — HTTP 200:**

```json
{
  "code": 1000,
  "result": null
}
```

**Error responses:**

| `code` | Meaning                                    | What to show                                                               |
| ------ | ------------------------------------------ | -------------------------------------------------------------------------- |
| `1006` | Token invalid or expired (UNAUTHENTICATED) | "This confirmation link is invalid or has expired. Please register again." |

### After Success

Show a brief success message:

> "Your account has been activated! You can now log in."

Then redirect to `/login` (optionally after a short delay, e.g. 3 seconds).

### Token expiry

Confirmation tokens expire after **24 hours**. If expired, the user must register again with the same or a different account.

---

## Step 3 — Login

**Page:** `/login`

Only `ACTIVE` accounts can log in. If a user tries to log in before confirming their email, the backend returns:

| `code` | Message                                                                      | HTTP |
| ------ | ---------------------------------------------------------------------------- | ---- |
| `1140` | "Account is not activated. Please check your email for a confirmation link." | 401  |

Show this message directly to the user so they know to check their email.

---

## Summary of API Endpoints Used

| Method | Path                             | Auth | Purpose                                |
| ------ | -------------------------------- | ---- | -------------------------------------- |
| `POST` | `/api/auth/register`             | None | Create account (INACTIVE) + send email |
| `GET`  | `/api/auth/confirm-email?token=` | None | Activate account                       |
| `POST` | `/api/auth/login`                | None | Login (only ACTIVE accounts)           |

---

## Environment

The confirmation email link uses `FRONTEND_URL` configured on the backend:

- Default: `http://localhost:3000`
- Production: set via the `FRONTEND_URL` environment variable on the server

So the FE confirm-email page must exist at `{FRONTEND_URL}/confirm-email`.

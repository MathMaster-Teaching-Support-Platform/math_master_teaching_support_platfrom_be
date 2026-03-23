# Frontend Implementation Guide — User & Teacher Profile APIs

> Base URL: `http://localhost:8080` (adjust per environment)
> All requests require `Authorization: Bearer <token>` unless stated otherwise.

---

## Table of Contents

- [Common Response Wrapper](#common-response-wrapper)
- [Common Error Codes](#common-error-codes)
- [Enum Reference](#enum-reference)
- [User APIs — `/users`](#user-apis----users)
  - [POST /users — Create User (ADMIN)](#1-post-users--create-user)
  - [PUT /users/{userId} — Update User (ADMIN)](#2-put-usersuserid--update-user)
  - [DELETE /users/{userId} — Delete User (ADMIN)](#3-delete-usersuserid--delete-user)
  - [GET /users/{userId} — Get User by ID](#4-get-usersuserid--get-user-by-id)
  - [GET /users — Get All Users (ADMIN)](#5-get-users--get-all-users)
  - [GET /users/page — Get Users Paginated (ADMIN)](#6-get-userspage--get-users-paginated)
  - [GET /users/my-info — Get My Info](#7-get-usersmy-info--get-my-info)
  - [PUT /users/my-info — Update My Info](#8-put-usersmy-info--update-my-info)
  - [POST /users/search — Search Users (ADMIN)](#9-post-userssearch--search-users)
  - [PUT /users/{userId}/ban — Ban User (ADMIN)](#10-put-usersuseridban--ban-user)
  - [PUT /users/{userId}/unban — Unban User (ADMIN)](#11-put-usersuseridunban--unban-user)
  - [PUT /users/{userId}/disable — Disable User (ADMIN)](#12-put-usersuseriddisable--disable-user)
  - [PUT /users/{userId}/enable — Enable User (ADMIN)](#13-put-usersuseridenable--enable-user)
  - [PUT /users/change-password — Change Password](#14-put-userschange-password--change-password)
- [Teacher Profile APIs — `/teacher-profiles`](#teacher-profile-apis----teacher-profiles)
  - [POST /teacher-profiles/submit — Submit Profile (STUDENT)](#1-post-teacher-profilessubmit--submit-profile)
  - [PUT /teacher-profiles/my-profile — Update My Profile (STUDENT)](#2-put-teacher-profilesmy-profile--update-my-profile)
  - [GET /teacher-profiles/my-profile — Get My Profile](#3-get-teacher-profilesmy-profile--get-my-profile)
  - [DELETE /teacher-profiles/my-profile — Delete My Profile (STUDENT)](#4-delete-teacher-profilesmy-profile--delete-my-profile)
  - [GET /teacher-profiles/{profileId} — Get Profile by ID (ADMIN)](#5-get-teacher-profilesprofileid--get-profile-by-id)
  - [GET /teacher-profiles/status/{status} — Get Profiles by Status (ADMIN)](#6-get-teacher-profilesstatusstatus--get-profiles-by-status)
  - [POST /teacher-profiles/{profileId}/review — Review Profile (ADMIN)](#7-post-teacher-profilesprofileidreview--review-profile)
  - [GET /teacher-profiles/pending/count — Count Pending (ADMIN)](#8-get-teacher-profilespendingcount--count-pending)
  - [GET /teacher-profiles/{profileId}/download — Get Download URL (ADMIN)](#9-get-teacher-profilesprofileiddownload--get-download-url)

---

## Common Response Wrapper

Every API response is wrapped in:

```json
{
  "code": 1000,
  "message": "...",
  "result": { ... }
}
```

| Field     | Type    | Notes                                                        |
| --------- | ------- | ------------------------------------------------------------ |
| `code`    | integer | `1000` = success. Any other value = error (see error table)  |
| `message` | string  | Present on errors or simple success messages (no `result`)   |
| `result`  | any     | The actual response payload. `null` / absent on void results |

**Success (no body result):**

```json
{ "code": 1000, "message": "User deleted successfully" }
```

**Error:**

```json
{ "code": 1005, "message": "User not existed" }
```

---

## Common Error Codes

| Code | Message                                        | HTTP |
| ---- | ---------------------------------------------- | ---- |
| 1002 | User existed                                   | 400  |
| 1005 | User not existed                               | 404  |
| 1006 | Unauthenticated                                | 401  |
| 1007 | You do not have permission                     | 403  |
| 1009 | Role not existed                               | 404  |
| 1013 | Email already exists                           | 400  |
| 1014 | Current password is incorrect                  | 400  |
| 1015 | New password and confirm password do not match | 400  |
| 1016 | User is already banned                         | 400  |
| 1017 | User is not banned                             | 400  |
| 1018 | User is already disabled                       | 400  |
| 1019 | User is already enabled                        | 400  |
| 1020 | Teacher profile already exists                 | 400  |
| 1021 | Teacher profile not found                      | 404  |
| 1022 | Profile is already approved                    | 400  |
| 1023 | Approved profile cannot be modified            | 400  |
| 1024 | Invalid profile status for this operation      | 400  |
| 9999 | Uncategorized error                            | 500  |

Validation errors (field-level) also return HTTP 400 with `code: 1000` (or a specific code) and a `message` describing which field failed.

---

## Enum Reference

### `Gender`

```
MALE | FEMALE | OTHER
```

### `Status` (User status)

```
ACTIVE | INACTIVE | DELETED | BANNED
```

### `ProfileStatus` (Teacher profile status)

```
PENDING | APPROVED | REJECTED
```

---

## User APIs — `/users`

### 1. POST /users — Create User

**Access:** `ADMIN` only

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "userName": "john_doe",
  "password": "Secret@123",
  "fullName": "John Doe",
  "email": "john@example.com",
  "phoneNumber": "0912345678",
  "gender": "MALE",
  "avatar": "https://example.com/avatar.jpg",
  "dob": "1995-06-15",
  "code": "STU-001",
  "roles": ["STUDENT"]
}
```

**Field Validations:**

| Field         | Required | Rules                                                                                        |
| ------------- | -------- | -------------------------------------------------------------------------------------------- |
| `userName`    | YES      | 3–50 characters                                                                              |
| `password`    | YES      | 8–128 chars; must have ≥1 uppercase, ≥1 lowercase, ≥1 digit, ≥1 special char (`!@#$%^&*...`) |
| `fullName`    | YES      | 2–50 characters                                                                              |
| `email`       | YES      | Valid email format; max 50 chars                                                             |
| `phoneNumber` | NO       | Vietnamese format: `0912345678` or `+84912345678`                                            |
| `gender`      | NO       | `MALE`, `FEMALE`, `OTHER`                                                                    |
| `avatar`      | NO       | URL string, max 2048 chars                                                                   |
| `dob`         | NO       | `YYYY-MM-DD`; must be in the past                                                            |
| `code`        | NO       | Max 100 chars; only `[a-zA-Z0-9_-]`                                                          |
| `roles`       | NO       | Array of role name strings; max 10 items (e.g., `["STUDENT"]`)                               |

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "0912345678",
    "gender": "MALE",
    "avatar": "https://example.com/avatar.jpg",
    "dob": "1995-06-15",
    "code": "STU-001",
    "status": "ACTIVE",
    "banReason": null,
    "banDate": null,
    "roles": ["STUDENT"],
    "createdDate": "2026-03-24T10:00:00Z",
    "createdBy": "admin",
    "updatedDate": "2026-03-24T10:00:00Z",
    "updatedBy": "admin"
  }
}
```

**Business rules:**

- Created user always gets `status: ACTIVE`.
- If `roles` is omitted, user has no roles assigned yet.
- `userName` and `email` must be unique in the system.

---

### 2. PUT /users/{userId} — Update User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:** (all fields optional; only send what needs to change)

```json
{
  "password": "NewSecret@456",
  "fullName": "John Updated",
  "email": "john.updated@example.com",
  "phoneNumber": "0987654321",
  "gender": "MALE",
  "avatar": "https://example.com/new-avatar.jpg",
  "dob": "1995-06-15",
  "code": "STU-002",
  "status": "INACTIVE",
  "roles": ["STUDENT", "TEACHER"]
}
```

**Field Validations:** same constraints as Create User (see above), except that:

- `email` is marked `@NotBlank` — send the current email if not changing it.
- `password` may be omitted; if included must satisfy complexity rules.
- `status` can be set directly: `ACTIVE | INACTIVE | DELETED | BANNED`.
- `roles` replaces the entire role set (send all desired roles).

**Success Response:** same `UserResponse` shape as Create User.

**Business rules:**

- Email uniqueness is checked against other users (can keep the same email).
- If `roles` is sent, **replaces** all existing roles — not additive.

---

### 3. DELETE /users/{userId} — Delete User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

> **Soft delete** — sets `status` to `DELETED`. Data is retained.

**Success Response `200`:**

```json
{
  "code": 1000,
  "message": "User deleted successfully"
}
```

**Error cases:** `1005` — user not found.

---

### 4. GET /users/{userId} — Get User by ID

**Access:** `ADMIN`, `TEACHER`, `STUDENT`

**Path param:** `userId` — UUID

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "john_doe",
    "fullName": "John Doe",
    "email": "john@example.com",
    "phoneNumber": "0912345678",
    "gender": "MALE",
    "avatar": "https://example.com/avatar.jpg",
    "dob": "1995-06-15",
    "code": "STU-001",
    "status": "ACTIVE",
    "banReason": null,
    "banDate": null,
    "roles": ["STUDENT"],
    "createdDate": "2026-03-24T10:00:00Z",
    "createdBy": "admin",
    "updatedDate": "2026-03-24T10:00:00Z",
    "updatedBy": "admin"
  }
}
```

---

### 5. GET /users — Get All Users

**Access:** `ADMIN` only

> Returns a flat list (no pagination). For large datasets prefer `/users/page`.

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": [
    {
      "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
      "userName": "john_doe",
      ...
    },
    ...
  ]
}
```

---

### 6. GET /users/page — Get Users Paginated

**Access:** `ADMIN` only

**Query Params:**

| Param           | Type    | Default | Description                                            |
| --------------- | ------- | ------- | ------------------------------------------------------ |
| `page`          | integer | `0`     | Zero-based page index                                  |
| `size`          | integer | `10`    | Number of items per page                               |
| `sortBy`        | string  | `id`    | Field name to sort by (e.g. `createdDate`, `fullName`) |
| `sortDirection` | string  | `ASC`   | `ASC` or `DESC`                                        |

**Example:**

```
GET /users/page?page=0&size=20&sortBy=createdDate&sortDirection=DESC
```

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        "userName": "john_doe",
        ...
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 20,
      "sort": { "sorted": true, "unsorted": false }
    },
    "totalElements": 150,
    "totalPages": 8,
    "last": false,
    "first": true,
    "numberOfElements": 20,
    "empty": false
  }
}
```

---

### 7. GET /users/my-info — Get My Info

**Access:** Any authenticated user

**Success Response:** same `UserResponse` shape.

---

### 8. PUT /users/my-info — Update My Info

**Access:** Any authenticated user

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:** (same shape as Update User, except `status` and `roles` are **ignored** — self-update cannot change own status or roles)

```json
{
  "fullName": "John Updated",
  "phoneNumber": "0987654321",
  "gender": "MALE",
  "avatar": "https://example.com/new-avatar.jpg",
  "dob": "1995-06-15",
  "password": "NewSecret@456"
}
```

> `email` is `@NotBlank` annotated — send the current email even if not changing it.

**Success Response:** `UserResponse` of the updated account.

---

### 9. POST /users/search — Search Users

**Access:** `ADMIN` only

**Query Params (pagination):**

| Param           | Type    | Default | Description           |
| --------------- | ------- | ------- | --------------------- |
| `page`          | integer | `0`     | Zero-based page index |
| `size`          | integer | `10`    | Items per page        |
| `sortBy`        | string  | `id`    | Sort field            |
| `sortDirection` | string  | `ASC`   | `ASC` or `DESC`       |

**Request Body:**

```json
{
  "keyword": "john",
  "userName": "john_doe",
  "email": "john@example.com",
  "fullName": "John",
  "gender": "MALE",
  "status": "ACTIVE",
  "code": "STU-001",
  "dobFrom": "1990-01-01",
  "dobTo": "2000-12-31",
  "roleName": "STUDENT"
}
```

All fields are optional — send only what you want to filter on.

| Field      | Type   | Notes                                                |
| ---------- | ------ | ---------------------------------------------------- |
| `keyword`  | string | Free-text search across multiple fields              |
| `userName` | string | Exact or partial username                            |
| `email`    | string | Exact or partial email                               |
| `fullName` | string | Exact or partial full name                           |
| `gender`   | string | `MALE \| FEMALE \| OTHER`                            |
| `status`   | string | `ACTIVE \| INACTIVE \| DELETED \| BANNED`            |
| `code`     | string | Student/teacher code                                 |
| `dobFrom`  | string | `YYYY-MM-DD`; must not be in the future              |
| `dobTo`    | string | `YYYY-MM-DD`; must not be in the future; ≥ `dobFrom` |
| `roleName` | string | Filter by role name (e.g. `STUDENT`, `TEACHER`)      |

**Validation:**

- `dobFrom` must not be after `dobTo`.

**Success Response:** Paginated `Page<UserResponse>` (same shape as [GET /users/page](#6-get-userspage--get-users-paginated)).

---

### 10. PUT /users/{userId}/ban — Ban User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

**Query Param:**

| Param    | Required | Default                     | Description    |
| -------- | -------- | --------------------------- | -------------- |
| `reason` | NO       | `Violated terms of service` | Reason for ban |

**Example:**

```
PUT /users/3fa85f64-5717-4562-b3fc-2c963f66afa6/ban?reason=Spam+content
```

**Business rules:**

- Cannot ban a user who is already `BANNED` → error `1016`.
- Sets `status = BANNED`, records `banReason` and `banDate`.

**Success Response:** Updated `UserResponse` with `status: "BANNED"`, `banReason`, `banDate` set.

---

### 11. PUT /users/{userId}/unban — Unban User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

**Business rules:**

- User must currently be `BANNED` → otherwise error `1017`.
- Sets `status = ACTIVE`, clears `banReason` and `banDate`.

**Success Response:** Updated `UserResponse` with `status: "ACTIVE"`.

---

### 12. PUT /users/{userId}/disable — Disable User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

**Business rules:**

- Cannot disable a user who is already `INACTIVE` → error `1018`.
- Sets `status = INACTIVE` (soft disable).

**Success Response:** Updated `UserResponse` with `status: "INACTIVE"`.

---

### 13. PUT /users/{userId}/enable — Enable User

**Access:** `ADMIN` only

**Path param:** `userId` — UUID

**Business rules:**

- Cannot enable a user who is already `ACTIVE` → error `1019`.
- Cannot enable a `DELETED` user → error `1005`.
- Sets `status = ACTIVE`.

**Success Response:** Updated `UserResponse` with `status: "ACTIVE"`.

---

### 14. PUT /users/change-password — Change Password

**Access:** Any authenticated user

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "currentPassword": "OldSecret@123",
  "newPassword": "NewSecret@456",
  "confirmPassword": "NewSecret@456"
}
```

**Field Validations:**

| Field             | Required | Rules                               |
| ----------------- | -------- | ----------------------------------- |
| `currentPassword` | YES      | Not blank                           |
| `newPassword`     | YES      | Not blank; min 8 characters         |
| `confirmPassword` | YES      | Not blank; must equal `newPassword` |

**Business rules:**

- `currentPassword` must match the stored (hashed) password → error `1014`.
- `newPassword` and `confirmPassword` must be identical → error `1015`.

**Success Response `200`:**

```json
{
  "code": 1000,
  "message": "Password changed successfully"
}
```

---

## Teacher Profile APIs — `/teacher-profiles`

---

### 1. POST /teacher-profiles/submit — Submit Profile

**Access:** `STUDENT` only

> Upload a teacher profile + supporting document files (ZIP will be created on the server).

**Headers:**

```
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**Multipart Parts:**

| Part      | Content-Type       | Description                                     |
| --------- | ------------------ | ----------------------------------------------- |
| `request` | `application/json` | JSON body with profile details (see below)      |
| `files`   | binary             | One or more document files (PDFs, images, etc.) |

**`request` part JSON:**

```json
{
  "schoolName": "FPT University",
  "schoolAddress": "Hoa Lac Hi-Tech Park, Hanoi",
  "schoolWebsite": "https://fpt.edu.vn",
  "position": "Mathematics Teacher",
  "description": "I have 5 years of experience teaching high school mathematics..."
}
```

**Field Validations:**

| Field           | Required | Rules                         |
| --------------- | -------- | ----------------------------- |
| `schoolName`    | YES      | Not blank                     |
| `schoolAddress` | NO       | —                             |
| `schoolWebsite` | NO       | —                             |
| `position`      | YES      | Not blank; max 100 characters |
| `description`   | NO       | Max 1000 characters           |
| `files`         | YES      | At least 1 file required      |

**JavaScript / Axios example:**

```javascript
const formData = new FormData();
formData.append(
  "request",
  new Blob(
    [
      JSON.stringify({
        schoolName: "FPT University",
        schoolAddress: "Hoa Lac Hi-Tech Park, Hanoi",
        position: "Mathematics Teacher",
        description: "5 years of experience...",
      }),
    ],
    { type: "application/json" },
  ),
);
files.forEach((file) => formData.append("files", file));

axios.post("/teacher-profiles/submit", formData, {
  headers: {
    Authorization: `Bearer ${token}`,
    // Do NOT set Content-Type manually — let the browser set boundary
  },
});
```

**Business rules:**

- A student can only have **one** profile — submitting again → error `1020`.
- Files are bundled into a ZIP and stored. The profile is created with `status: PENDING`.

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "id": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "userName": "john_doe",
    "fullName": "John Doe",
    "schoolName": "FPT University",
    "schoolAddress": "Hoa Lac Hi-Tech Park, Hanoi",
    "schoolWebsite": "https://fpt.edu.vn",
    "position": "Mathematics Teacher",
    "verificationDocumentKey": "verification/verification_docs_uuid.zip",
    "description": "5 years of experience...",
    "status": "PENDING",
    "adminComment": null,
    "reviewedBy": null,
    "reviewedByName": null,
    "reviewedAt": null,
    "createdAt": "2026-03-24T10:00:00Z",
    "updatedAt": "2026-03-24T10:00:00Z"
  }
}
```

---

### 2. PUT /teacher-profiles/my-profile — Update My Profile

**Access:** `STUDENT` only

> Can only update profiles with `status: PENDING` or `status: REJECTED`.  
> If updating a `REJECTED` profile → status is automatically reset to `PENDING`.

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "schoolName": "FPT University (Updated)",
  "schoolAddress": "Updated Address",
  "schoolWebsite": "https://fpt.edu.vn",
  "position": "Senior Mathematics Teacher",
  "description": "Updated description..."
}
```

Same validation as submit. All 5 fields are sent (they replace the existing values).

**Business rules:**

- `APPROVED` profile cannot be modified → error `1023`.
- No new document files can be uploaded via this endpoint (JSON body only).

**Success Response:** `TeacherProfileResponse` with updated data.

---

### 3. GET /teacher-profiles/my-profile — Get My Profile

**Access:** `STUDENT` or `TEACHER`

**Success Response:** `TeacherProfileResponse`

```json
{
  "code": 1000,
  "result": {
    "id": "a1b2c3d4-...",
    "userId": "3fa85f64-...",
    "userName": "john_doe",
    "fullName": "John Doe",
    "schoolName": "FPT University",
    "schoolAddress": "Hoa Lac Hi-Tech Park, Hanoi",
    "schoolWebsite": "https://fpt.edu.vn",
    "position": "Mathematics Teacher",
    "verificationDocumentKey": "verification/verification_docs_uuid.zip",
    "description": "5 years of experience...",
    "status": "PENDING",
    "adminComment": null,
    "reviewedBy": null,
    "reviewedByName": null,
    "reviewedAt": null,
    "createdAt": "2026-03-24T10:00:00Z",
    "updatedAt": "2026-03-24T10:00:00Z"
  }
}
```

**Error:** `1021` — profile not found (student hasn't submitted yet).

---

### 4. DELETE /teacher-profiles/my-profile — Delete My Profile

**Access:** `STUDENT` only

**Business rules:**

- Can only delete `PENDING` or `REJECTED` profiles.
- `APPROVED` profiles cannot be deleted → error `1023`.

**Success Response `200`:**

```json
{
  "code": 1000,
  "message": "Profile deleted successfully"
}
```

---

### 5. GET /teacher-profiles/{profileId} — Get Profile by ID

**Access:** `ADMIN` only

**Path param:** `profileId` — UUID

**Success Response:** `TeacherProfileResponse` (same as above).

**Error:** `1021` — profile not found.

---

### 6. GET /teacher-profiles/status/{status} — Get Profiles by Status

**Access:** `ADMIN` only

**Path param:** `status` — `PENDING | APPROVED | REJECTED`

**Query Params:**

| Param  | Type    | Default | Description           |
| ------ | ------- | ------- | --------------------- |
| `page` | integer | `0`     | Zero-based page index |
| `size` | integer | `10`    | Items per page        |

**Example:**

```
GET /teacher-profiles/status/PENDING?page=0&size=10
```

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "content": [
      {
        "id": "a1b2c3d4-...",
        "status": "PENDING",
        ...
      }
    ],
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true,
    "numberOfElements": 5,
    "empty": false
  }
}
```

Profiles are ordered by `createdAt` descending (newest first).

---

### 7. POST /teacher-profiles/{profileId}/review — Review Profile

**Access:** `ADMIN` only

**Path param:** `profileId` — UUID

**Headers:**

```
Authorization: Bearer <token>
Content-Type: application/json
```

**Request Body:**

```json
{
  "status": "APPROVED",
  "adminComment": "All documents verified. Welcome!"
}
```

or for rejection:

```json
{
  "status": "REJECTED",
  "adminComment": "Documents are incomplete. Please resubmit."
}
```

**Field Validations:**

| Field          | Required | Rules                                 |
| -------------- | -------- | ------------------------------------- |
| `status`       | YES      | Must be `APPROVED` or `REJECTED` only |
| `adminComment` | NO       | Max 1000 characters                   |

**Business rules:**

- Profile **must** be in `PENDING` status → otherwise error `1024`.
- `status` must be either `APPROVED` or `REJECTED` → `PENDING` is not a valid review outcome.
- On **APPROVED**: the user's role is automatically upgraded to include `TEACHER`. An email notification and an in-app stream notification are sent.
- On **REJECTED**: An email and in-app notification are sent with the rejection reason.

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": {
    "id": "a1b2c3d4-...",
    "userId": "3fa85f64-...",
    "userName": "john_doe",
    "fullName": "John Doe",
    "schoolName": "FPT University",
    "status": "APPROVED",
    "adminComment": "All documents verified. Welcome!",
    "reviewedBy": "admin-uuid-here",
    "reviewedByName": "Admin Name",
    "reviewedAt": "2026-03-24T12:00:00",
    "createdAt": "2026-03-24T10:00:00Z",
    "updatedAt": "2026-03-24T12:00:00Z"
  }
}
```

---

### 8. GET /teacher-profiles/pending/count — Count Pending

**Access:** `ADMIN` only

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": 7
}
```

Use this to show a badge or notification on the admin dashboard.

---

### 9. GET /teacher-profiles/{profileId}/download — Get Download URL

**Access:** `ADMIN` only

**Path param:** `profileId` — UUID

> Returns a **pre-signed** (time-limited) download URL for the verification ZIP file.

**Success Response `200`:**

```json
{
  "code": 1000,
  "result": "https://minio.example.com/bucketname/verification_docs_uuid.zip?X-Amz-Signature=..."
}
```

**FE usage:** Open or download directly using the URL returned in `result`. The URL expires after a short period — do not cache it.

**Error:** `1021` — profile not found.

---

## User Response — Full Field Reference

| Field         | Type          | Description                                           |
| ------------- | ------------- | ----------------------------------------------------- |
| `id`          | UUID string   | User's unique identifier                              |
| `userName`    | string        | Login username                                        |
| `fullName`    | string        | Display name                                          |
| `email`       | string        | Email address                                         |
| `phoneNumber` | string        | Vietnamese phone number                               |
| `gender`      | string        | `MALE \| FEMALE \| OTHER`                             |
| `avatar`      | string        | Avatar image URL                                      |
| `dob`         | string (date) | `YYYY-MM-DD`                                          |
| `code`        | string        | Student / teacher code                                |
| `status`      | string        | `ACTIVE \| INACTIVE \| DELETED \| BANNED`             |
| `banReason`   | string        | Present only when `status = BANNED`                   |
| `banDate`     | string (ISO)  | ISO-8601 instant; present only when `status = BANNED` |
| `roles`       | string[]      | Role name set, e.g. `["STUDENT", "TEACHER"]`          |
| `createdDate` | string (ISO)  | ISO-8601 instant                                      |
| `createdBy`   | string        | Username of creator                                   |
| `updatedDate` | string (ISO)  | ISO-8601 instant                                      |
| `updatedBy`   | string        | Username of last updater                              |

---

## TeacherProfileResponse — Full Field Reference

| Field                     | Type              | Description                                         |
| ------------------------- | ----------------- | --------------------------------------------------- |
| `id`                      | UUID string       | Profile's unique identifier                         |
| `userId`                  | UUID string       | Owner user's ID                                     |
| `userName`                | string            | Owner's username                                    |
| `fullName`                | string            | Owner's full name                                   |
| `schoolName`              | string            | School name                                         |
| `schoolAddress`           | string            | School address                                      |
| `schoolWebsite`           | string            | School website URL                                  |
| `position`                | string            | Job position/title                                  |
| `verificationDocumentKey` | string            | Internal storage key for the ZIP (not a direct URL) |
| `description`             | string            | Teacher self-description                            |
| `status`                  | string            | `PENDING \| APPROVED \| REJECTED`                   |
| `adminComment`            | string            | Admin feedback; present after review                |
| `reviewedBy`              | UUID string       | Admin's user ID who performed the review            |
| `reviewedByName`          | string            | Admin's full name                                   |
| `reviewedAt`              | string (datetime) | `YYYY-MM-DDTHH:mm:ss` (local datetime, no Z)        |
| `createdAt`               | string (ISO)      | ISO-8601 instant                                    |
| `updatedAt`               | string (ISO)      | ISO-8601 instant                                    |

---

## Notes for FE Team

1. **Authentication**: All endpoints require a valid JWT Bearer token except public auth endpoints (not in this document). Obtain the token via the `/auth/login` endpoint.

2. **Role resolution**: The `roles` field in `UserResponse` returns role **names** as strings (e.g., `"STUDENT"`, `"TEACHER"`, `"ADMIN"`).

3. **Profile status flow**:

   ```
   (none) → PENDING → APPROVED
                   ↘ REJECTED → PENDING (re-submit) → APPROVED / REJECTED
   ```

4. **User status flow**:

   ```
   ACTIVE ↔ INACTIVE (disable/enable)
   ACTIVE → BANNED (ban)
   BANNED → ACTIVE (unban)
   any → DELETED (delete — cannot be reversed via API)
   ```

5. **File upload**: Only the `POST /teacher-profiles/submit` endpoint accepts files. Use `multipart/form-data` and **do not manually set `Content-Type`** — let the browser/axios set it with the correct boundary.

6. **Download URL**: The pre-signed URL from `/teacher-profiles/{profileId}/download` is temporary. Request a fresh URL each time the admin wants to download.

7. **Pagination**: All paginated responses use Spring's `Page<T>` format. Key fields: `content` (the items), `totalElements`, `totalPages`, `first`, `last`.

8. **Date formats**:
   - `dob` → `YYYY-MM-DD` plain date string (e.g. `"1995-06-15"`)
   - `createdDate`, `updatedDate`, `banDate`, `createdAt`, `updatedAt` → ISO-8601 UTC instant (e.g. `"2026-03-24T10:00:00Z"`)
   - `reviewedAt` → local datetime without timezone (e.g. `"2026-03-24T12:00:00"`)

9. **`updateMyInfo`**: The `email` field has `@NotBlank` — always include it in the request body even if the user is not changing it.

10. **Notifications on review**: When admin approves or rejects a teacher profile, the system automatically sends both an email and an in-app real-time stream notification to the applicant. FE does not need to trigger this separately.

# BE Response: Student Explore Course Visibility

Date: 2026-04-25

## Scope

This response summarizes backend enforcement for public course visibility and status transitions used by FE Student Explore.

## 1) Public list (`GET /courses`)

Backend now enforces **published-only** visibility for public listing:

- `is_published = true`
- `status = 'PUBLISHED'`
- `deleted_at IS NULL`
- Backend validates `status` and `is_published` consistency on course state transitions.

Therefore, `DRAFT`, `PENDING_REVIEW`, `REJECTED`, or any unpublished record is excluded from public list results.

## 2) Public detail/preview (`GET /courses/{id}`, `GET /courses/{id}/preview`)

Backend now enforces visibility at service layer:

- If course is publicly visible (`isPublished=true` and `status=PUBLISHED`): allow.
- If course is not publicly visible:
  - allow only `ADMIN` or owner `TEACHER`.
  - deny all other users (including student/non-owner) with **403 Forbidden** (`COURSE_ACCESS_DENIED`).

## 3) Status lock after publish

To avoid post-sale lifecycle regressions, backend now blocks status transitions once a course is already `PUBLISHED`:

- Teacher cannot unpublish a published course via `PATCH /courses/{id}/publish` with `published=false`.
- Teacher cannot delete a published course via `DELETE /courses/{id}`.
- Admin approve/reject endpoints only accept courses in `PENDING_REVIEW`.

When transition is invalid, backend returns business error `INVALID_COURSE_STATUS`.

## FE integration note

Please keep FE guard in place, and treat backend as source of truth.

- FE must always check both fields together:
  - `status === 'PUBLISHED'`
  - `isPublished === true`
- If one of the two conditions is false, student must not see the course in Explore and must not be able to open detail/preview.
- If FE receives `403` for detail/preview, route to not-authorized or fallback page (do not display course content).

## Effective behavior for Student

Student/non-owner now:

- cannot see non-published courses in public listing.
- cannot access non-published course detail/preview endpoints.
- cannot bypass by direct URL if the course is unpublished/non-public.

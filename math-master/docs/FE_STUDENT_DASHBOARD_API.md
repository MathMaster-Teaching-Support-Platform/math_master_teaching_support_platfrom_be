# Student Dashboard API Contract (BE -> FE)

## Status

Implemented in backend.

Controller base paths:

- `/student/dashboard`
- `/api/student/dashboard` (alias for FE convenience)

All endpoints require authenticated user with role `STUDENT`.

## Response envelope

This backend uses a common envelope:

```json
{
  "code": 1000,
  "message": null,
  "result": {}
}
```

FE should read data from `result`.

## Endpoints

## 1) Summary

- `GET /api/student/dashboard/summary`
- (also available: `GET /student/dashboard/summary`)

Purpose: top greeting/profile + cards + motivation + today task count.

`result` shape:

```json
{
  "student": {
    "id": "string",
    "name": "string",
    "avatar": "string | null"
  },
  "notificationCount": 0,
  "stats": {
    "enrolledCourses": 0,
    "completedAssignments": 0,
    "averageScore": 0,
    "pendingAssignments": 0
  },
  "motivation": {
    "goalAssignments": 0,
    "completedAssignments": 0,
    "remainingAssignments": 0,
    "progressPercent": 0
  },
  "todayTaskCount": 0
}
```

## 2) Upcoming tasks

- `GET /api/student/dashboard/upcoming-tasks`

`result` shape:

```json
[
  {
    "id": "string",
    "title": "string",
    "subject": "string",
    "dueDate": "ISO-8601 | null",
    "type": "homework | quiz | exam",
    "progressPercent": 0
  }
]
```

Notes:

- Only pending tasks are returned (completed submissions are excluded).
- Sorted by `dueDate` ascending (null due dates are last).

## 3) Recent grades

- `GET /api/student/dashboard/recent-grades`

`result` shape:

```json
[
  {
    "id": "string",
    "title": "string",
    "subject": "string",
    "score": 0,
    "gradedAt": "ISO-8601"
  }
]
```

Notes:

- Returns latest graded items (up to 10).
- `score` is resolved from `finalScore`, fallback `score`, then `percentage`.

## 4) Learning progress

- `GET /api/student/dashboard/learning-progress`

`result` shape:

```json
[
  {
    "subject": "string",
    "doneLessons": 0,
    "totalLessons": 0,
    "percent": 0
  }
]
```

## 5) Weekly activity

- `GET /api/student/dashboard/weekly-activity`

`result` shape:

```json
{
  "range": {
    "from": "ISO-8601",
    "to": "ISO-8601"
  },
  "totalHours": 0,
  "deltaPercentVsPreviousWeek": 0,
  "days": [{ "dayLabel": "T2", "hours": 0 }]
}
```

Notes:

- Window is last 7 days in `Asia/Ho_Chi_Minh` timezone.
- Activity is aggregated from lesson watch progress and submission time spent.

## 6) Study streak

- `GET /api/student/dashboard/streak`

`result` shape:

```json
{
  "currentStreakDays": 0,
  "days": [{ "dayLabel": "T2", "active": true }],
  "message": "string"
}
```

Notes:

- Streak is computed from days with learning activity.
- Daily activity source: lesson watch updates + assignment submissions.

## 7) One-call merged payload (recommended to reduce round trips)

- `GET /api/student/dashboard`

`result` shape:

```json
{
  "summary": { "...same as summary endpoint...": true },
  "upcomingTasks": [],
  "recentGrades": [],
  "learningProgress": [],
  "weeklyActivity": {},
  "streak": {}
}
```

This endpoint is implemented for FE performance and simpler data loading.

## FE integration guidance

1. Prefer `GET /api/student/dashboard` for initial dashboard load.
2. Keep FE-owned text/copy in FE (Vietnamese labels, empty-state text, tone).
3. Format dates and numbers in FE (`vi-VN`) as planned.
4. Handle nullable fields safely (`avatar`, `dueDate`).
5. For any section needing independent refresh, FE can call section endpoint directly.

## Suggested FE mapping

- Header: `summary.student`, `summary.notificationCount`
- Cards: `summary.stats`
- Motivation card: `summary.motivation`
- Upcoming tasks list: `upcomingTasks`
- Recent grades list: `recentGrades`
- Learning progress bars: `learningProgress`
- Weekly chart: `weeklyActivity.days`
- Streak widget: `streak`

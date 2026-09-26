---
title: Exercise 14 — Implement the Current User Endpoint
description: Add a secured API endpoint that exposes a safe view of the validated caller identity.
---

# Implement the Current User Endpoint

**Estimated time:** 90–120 minutes  
**Work mode:** Individual implementation, pair review

## Goal

Implement `GET /api/me`, returning a safe JSON representation of the caller
derived from the validated JWT.

## Required Response

Return at least:

```json
{
  "username": "alice",
  "roles": ["user"],
  "admin": false
}
```

The exact Java type is your design choice, but the response must not contain the
raw token.

## Required Behavior

- The endpoint is under `/api/me`.
- A missing or invalid token is rejected.
- A caller needs the normal application role (`CurrentUser.USER`), the same role
  other resources require for everyday access.
- `username` comes from the same identity interpretation used for board ownership.
- Roles come from validated token claims.
- `admin` is consistent with existing administrator behavior.
- The response is JSON.

## 1. Create a Branch

```bash
git switch -c exercise/current-user-endpoint
```

## 2. Inspect Existing Security Abstractions

Open:

- `KanbanApplication`
- `CurrentUser`
- One resource with `@RolesAllowed`
- `microprofile-config.properties`

Decide which existing logic should be reused instead of duplicating claim
interpretation.

## 3. Design the Response Type

Prefer a small immutable response type, for example a Java record.

Questions:

1. Which package should contain it?
2. Should it be an entity? Why or why not?
3. Which fields are safe and useful to expose?
4. Should role ordering be deterministic?

## 4. Implement the Resource

Create a JAX-RS resource that:

- Uses the correct class-level path
- Produces JSON
- Uses request scope consistently with other resources
- Requires the appropriate role
- Injects and reuses current-user/JWT information
- Returns the response type

Do not manually parse the bearer-token string.

## 5. Verify the Endpoint

Test:

| Scenario | Expected result |
|---|---|
| No token | `401` |
| Alice token | Alice identity JSON |
| Bob token | Bob identity JSON |
| Admin token | Admin identity with `admin: true` |

Use `curl -i` so status and content type are visible.

Do not paste token values into source or the handout.

## 6. Inspect OpenAPI

Open `/openapi` or `/openapi/ui` and confirm that `/api/me` appears.

Compare the generated schema with your response type.

## Completion Check

Demonstrate:

- The resource source
- Reuse of existing identity logic
- `401` without a token
- Correct JSON for two users
- Correct administrator value
- The endpoint in OpenAPI

## Stretch Tasks

- Add a frontend account panel that calls `/api/me`.
- Compare the frontend's Keycloak-derived identity with the backend response.
- Add targeted backend tests if the project is extended with a test framework.


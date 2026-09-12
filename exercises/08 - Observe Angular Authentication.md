---
title: Exercise 8 — Observe Angular Authentication
description: Observe initialization, guarded navigation, Keycloak login, token attachment, and backend enforcement.
---

# Observe Angular Authentication

**Estimated time:** 45–60 minutes  
**Work mode:** Pairs

## Goal

Connect the authentication code to observable browser behavior without copying
or sharing access-token values.

## Safety Rule

Do not paste a token into this handout, chat, slides, source files, or issue
trackers. Record only header presence, claim names, and behavior.

## 1. Inspect Initialization

Find:

- `AuthService.init()`
- The application initializer in `app.config.ts`
- The Keycloak environment configuration

Record:

| Setting | Value |
|---|---|
| Keycloak URL | |
| Realm | |
| Client identifier | |
| `onLoad` mode | |
| PKCE method | |

Explain the difference between `check-sso` and forcing a login immediately.

## 2. Observe the Guard

1. Copy the URL of an existing board detail page.
2. Sign out.
3. Open that detail URL.
4. Observe the redirect to Keycloak.
5. Sign in.
6. Confirm the browser returns to the original detail URL.

Find the source line that preserves the requested URL.

## 3. Observe the Interceptor

1. Open the Network panel and filter to Fetch/XHR.
2. Trigger a boards request.
3. Confirm that an `Authorization` request header exists.
4. Trigger or inspect a request that does not begin with `/api`.
5. Explain why the interceptor should not attach the token to every URL.

Do not record the header value.

## 4. Distinguish UX from Security

Discuss:

1. Could a user modify Angular code to bypass the guard?
2. Would bypassing the guard grant access to another user's board?
3. Which backend mechanisms must still reject unauthorized access?
4. Why is hiding a button not authorization?

## Completion Check

Both partners can explain:

```text
app initializer
  -> AuthService
  -> authGuard
  -> Keycloak redirect and return
  -> authInterceptor
  -> backend validation
```

## Stretch Task

Inspect the token locally using browser tooling and list the names—not the
values—of the claims related to issuer, audience, username, and roles.

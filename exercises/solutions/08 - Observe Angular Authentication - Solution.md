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
| Keycloak URL | `http://localhost:8081` (`environment.keycloak.url`) |
| Realm | `kanban` |
| Client identifier | `kanban-app` |
| `onLoad` mode | `check-sso` |
| PKCE method | `S256` |

`provideAppInitializer(() => inject(AuthService).init())` in `app.config.ts`
runs before Angular renders anything, so by the time the router activates
any route, `AuthService.init()` has already resolved and `authenticated()`
holds a definitive synchronous answer.

**`check-sso` vs. forcing a login immediately:** `check-sso` silently asks
Keycloak "does a session already exist?" and comes straight back either
way — it never redirects to the login page by itself, even for an
unauthenticated visitor. Forcing a login (`onLoad: 'login-required'` would
be the alternative) would redirect *every* visitor to Keycloak immediately,
even before they asked for a protected page. Using `check-sso` instead lets
`authGuard` decide, per route, when a redirect is actually needed — so an
anonymous visit to a future public page (if one existed) would not force a
login.

## 2. Observe the Guard

1. Copy the URL of an existing board detail page.
2. Sign out.
3. Open that detail URL.
4. Observe the redirect to Keycloak.
5. Sign in.
6. Confirm the browser returns to the original detail URL.

Find the source line that preserves the requested URL.

```ts
// auth.guard.ts
auth.login(window.location.origin + state.url);
```

`state.url` is the URL the router was trying to activate (e.g.
`/boards/3`); `authGuard` passes it to `AuthService.login()`, which forwards
it to `keycloak.login({ redirectUri })`. Keycloak sends the browser back to
exactly that `redirectUri` after a successful sign-in, so the original deep
link survives the round trip instead of dropping the user back on `/`.

## 3. Observe the Interceptor

1. Open the Network panel and filter to Fetch/XHR.
2. Trigger a boards request.
3. Confirm that an `Authorization` request header exists.
4. Trigger or inspect a request that does not begin with `/api`.
5. Explain why the interceptor should not attach the token to every URL.

Do not record the header value.

- A request to `/api/boards` (or any `/api/...` call) carries an
  `Authorization` header (bearer scheme) — visible in the Network panel's
  Headers tab, without needing to read its value.
- A request that is not proxied through `/api` (e.g. the Keycloak redirect
  itself, or a static asset request) shows no such header, because
  `authInterceptor` explicitly checks `req.url.startsWith(environment.apiUrl)`
  and calls `next(req)` unchanged otherwise.
- **Why not attach it everywhere:** the token identifies the signed-in user.
  Sending it to any third-party host (analytics, CDN assets, or — worse —
  Keycloak's own endpoints, which expect different credentials) would leak
  the user's identity/session to a host that has no business receiving it.
  Scoping the header to `/api` keeps the token exactly where it is needed:
  requests to *this app's own backend*.

## 4. Distinguish UX from Security

Discuss:

1. **Could a user modify Angular code to bypass the guard?** Yes — all
   frontend code (including `authGuard`) runs in the user's own browser and
   can be edited, disabled, or bypassed via devtools, a modified build, or a
   direct HTTP client (`curl`), since Angular route guards are pure
   client-side UX gates.
2. **Would bypassing the guard grant access to another user's board?** No.
   Without a valid, signed token the backend's `@RolesAllowed` on
   `BoardResource` rejects the call with 401. Even with a valid token for a
   different user, `BoardAccess`/`accessibleBoard(id)` still checks board
   ownership (403 if the board isn't the caller's, unless they're `admin`).
3. **Which backend mechanisms must still reject unauthorized access?**
   MicroProfile JWT signature/issuer/audience/expiry verification (401 for
   no/invalid token), `@RolesAllowed({ USER, ADMIN })` (403 for a token
   missing the required role), and the per-board ownership check in
   `accessibleBoard()`/`BoardAccess` (403/404 for someone else's board).
4. **Why is hiding a button not authorization?** Hiding a "Delete" button or
   redirecting via a guard only changes what the UI *offers* — it does not
   stop a determined client from calling `DELETE /api/boards/3` directly.
   Authorization must be enforced where the request is actually acted upon
   (the backend), not merely where it's offered (the UI).

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

Using the Network panel's request headers (never paste the value) or a
local decode, the relevant claim names are:

- `iss` — issuer (matched against `mp.jwt.verify.issuer`)
- `aud` — audience (matched against `mp.jwt.verify.audiences=kanban-app`,
  populated by the realm's "kanban-app audience" mapper)
- `preferred_username` — read by the backend's `CurrentUser.name()` as the
  board owner
- `groups` — realm roles remapped into this claim (via the "realm roles as
  groups" mapper), read by `JsonWebToken.getGroups()` and checked in
  `CurrentUser.isAdmin()` / `@RolesAllowed`

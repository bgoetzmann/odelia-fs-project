---
title: Exercise 16 — Find the Security Misconfiguration
description: Spot deliberately broken configuration snippets, name the OWASP category, and propose a one-line fix.
---

# Find the Security Misconfiguration

**Estimated time:** 15–20 minutes
**Work mode:** Pairs

## Findings Table

| Scenario | OWASP category | Concrete risk in the Kanban app | One-line fix |
|---|---|---|---|
| A — CORS | Security misconfiguration | `allowedOrigins="*"` plus `allowCredentials="true"` lets **any** website's script read authenticated API responses from a signed-in user's browser — a malicious site could silently call `/api/boards` on a victim's behalf | Set `allowedOrigins` back to the real, specific frontend origin(s), e.g. `http://localhost:4200` in dev / the real deployed frontend URL in production |
| B — Token verification | Security misconfiguration (identification & authentication failure) | `mp.jwt.verify.issuer` points at the `master` realm instead of `kanban` — a token issued by *any* client in the `master` realm (not just `kanban-app`) would be accepted as a valid Kanban identity, and `preferred_username`/`groups` claims from an unrelated realm would be trusted as board ownership/roles | Set `mp.jwt.verify.issuer` to the `kanban` realm's actual issuer URL (matching `publickey.location`, which is already correct) |
| C — Error handling | Security misconfiguration (verbose error messages) | Returning `exception.getMessage()` and the full stack trace to the client can leak internal class names, SQL fragments, file paths, or (for some exception types) other users' data embedded in the message — this is reconnaissance information for an attacker | Log the exception server-side with full detail; return a generic message and a correlation id to the client, with no stack trace |
| D — Deployment secrets | Cryptographic failures / security misconfiguration | Hardcoded, guessable secrets (`kanban123`, `admin`) committed to `docker-compose.yml` are visible to anyone with repo access and are the actual production credentials if this file is used to deploy — an attacker with read access to the repo (or its git history) gets the database and Keycloak admin credentials for free | Move `DB_PASSWORD`/`KEYCLOAK_ADMIN_PASSWORD` out of the compose file into an untracked `.env` file or a secret manager, referenced via `${DB_PASSWORD}` |

## Discuss

1. **Which would a working end-to-end demo still pass with?** All four. A
   demo run from `localhost` with a real Keycloak token would look
   completely correct in every scenario — none of these four break the
   happy path a demo exercises. That is exactly why "it works when I click
   through it" is not a substitute for a security review: none of the
   ownership logic (`BoardAccess`) is exercised differently by any of these
   bugs.
2. **Authenticated-user-only vs. anyone-on-the-network:** Scenario A (CORS)
   is exploitable by any website a signed-in user's browser visits — no
   Kanban account needed by the attacker, only a victim who is already
   logged in. Scenario B (issuer) requires the attacker to already hold (or
   be able to obtain) a valid token from *some* client in the `master`
   realm — a lower bar than compromising `kanban-app` itself, but still
   requires *some* foothold. Scenarios C and D are closer to reconnaissance
   / credential theft than direct exploitation on their own.
3. **Why `BoardAccess` doesn't save any of these:** `BoardAccess` only runs
   *after* a request has already been authenticated and routed to the
   right resource method. Scenario A lets a browser make the authenticated
   request in the first place; Scenario B undermines what "authenticated"
   even means; Scenario C leaks information regardless of whether the
   ownership check correctly returned 403; Scenario D compromises the
   credentials the whole system trusts, upstream of any application logic.
   Object-level authorization is necessary but not sufficient — it assumes
   authentication, transport, and configuration are already sound.

## Completion Check

Both partners can:

- Point to the exact line changed in each of the four snippets:
  Scenario A's `allowedOrigins="*"`; Scenario B's
  `.../realms/master` (should be `.../realms/kanban`); Scenario C's
  `exception.getMessage()`/`stackTrace` entries in the response body;
  Scenario D's two hardcoded password values.
- Name the OWASP category for each, matching Day 4's OWASP slide (all four
  map to *security misconfiguration*; B and D also touch identification/
  authentication and cryptographic failures respectively).
- Explain Scenario B: a *typo'd* issuer (e.g. a misspelled hostname) would
  fail to resolve/match and reliably reject every token with a clear 401 —
  loud and easy to catch in any test run. Pointing at the *wrong but real*
  realm (`master` instead of `kanban`) still validates successfully against
  a real Keycloak signing key, so tokens are accepted silently; the bug
  only surfaces when someone notices *which* users can log in, which no
  automated test in this course currently checks.

## Stretch Task

Comparing against the real project:

- `backend/src/main/liberty/config/server.xml` sets `allowedOrigins` to the
  literal dev frontend origin (`http://localhost:4200`), not `*` — the
  scenario changed only that one attribute.
- The real `docker-compose.yml` (if/when the class builds one together)
  should source secrets from environment variables or an `.env` file
  rather than inlining literal values — comparing side-by-side is meant to
  surface that gap, since the course's docker-compose example in the Day 4
  slides uses `MP_JWT_VERIFY_ISSUER=...` as a placeholder rather than a
  real secret.

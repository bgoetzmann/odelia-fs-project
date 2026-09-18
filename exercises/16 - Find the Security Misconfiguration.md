---
title: Exercise 16 — Find the Security Misconfiguration
description: Spot deliberately broken configuration snippets, name the OWASP category, and propose a one-line fix.
---

# Find the Security Misconfiguration

**Estimated time:** 15–20 minutes
**Work mode:** Pairs

## Goal

Practice recognizing security misconfiguration as a category, not just a
vague "be careful" — by finding four deliberately broken snippets, each
based on real configuration from this project, and proposing the fix.

## Method

For **each** scenario below:

1. Name the OWASP category it falls under (see Day 4's OWASP slide).
2. State the concrete risk to the Kanban app if this shipped as-is.
3. Propose the one-line fix.

Do not just say "this is insecure" — name *what* becomes possible for an
attacker (or a mistaken script) because of this specific line.

## Scenario A — CORS

```xml
<!-- backend/src/main/liberty/config/server.xml -->
<cors domain="/"
      allowedOrigins="*"
      allowedMethods="GET, POST, PUT, PATCH, DELETE, OPTIONS"
      allowedHeaders="Content-Type, Accept, Authorization"
      allowCredentials="true"/>
```

## Scenario B — Token Verification

```properties
# backend/src/main/liberty/config/microprofile-config.properties
mp.jwt.verify.issuer=http://keycloak:8080/realms/master
mp.jwt.verify.audiences=kanban-app
mp.jwt.verify.publickey.location=https://keycloak:8443/realms/kanban/protocol/openid-connect/certs
```

## Scenario C — Error Handling

```java
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {
    @Override
    public Response toResponse(Exception exception) {
        return Response.serverError()
            .entity(Map.of(
                "message", exception.getMessage(),
                "stackTrace", Arrays.toString(exception.getStackTrace())
            ))
            .build();
    }
}
```

## Scenario D — Deployment Secrets

```yaml
# docker-compose.yml
services:
  backend:
    build: ./backend
    environment:
      - MP_JWT_VERIFY_ISSUER=http://keycloak:8080/realms/kanban
      - DB_PASSWORD=kanban123
      - KEYCLOAK_ADMIN_PASSWORD=admin
```

## Findings Table

| Scenario | OWASP category | Concrete risk in the Kanban app | One-line fix |
|---|---|---|---|
| A — CORS | | | |
| B — Token verification | | | |
| C — Error handling | | | |
| D — Deployment secrets | | | |

## Discuss

1. Which of these four would a working end-to-end demo *still pass* with,
   even though it is broken? (That is exactly why security review cannot
   rely on "it works.")
2. Which one is exploitable only by another authenticated Kanban user, and
   which is exploitable by anyone on the network?
3. `BoardAccess` ownership checks are unaffected by all four scenarios —
   why doesn't fixing them make any of these four safe to ship?

## Completion Check

Both partners can:

- Point to the exact line changed in each of the four snippets
- Name the OWASP category for each, matching Day 4's OWASP slide
- Explain scenario B without looking: why a *wrong realm* in the issuer is
  worse than a *typo'd* issuer (hint: which one might still validate
  successfully against a real, but wrong, Keycloak realm)

## Stretch Task

Compare Scenario A and Scenario D against `backend/src/main/liberty/config/server.xml`
and `docker-compose.yml` in the actual project. List every additional
difference you notice beyond the one this exercise introduced deliberately.

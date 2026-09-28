---
title: Exercise 16 — Find the Security Misconfiguration
description: Spot and fix four deliberately broken configuration snippets drawn from the Kanban app's real security setup.
---

# Find the Security Misconfiguration

**Estimated time:** 15–20 minutes
**Work mode:** Pairs

## Goal

Each snippet below is based on real configuration from the project, with one
deliberate change. For each snippet:

1. Name the OWASP category it falls under.
2. State the concrete risk in the Kanban app if this shipped as-is.
3. Propose the one-line fix.

## Snippet A — `server.xml`

```xml
<cors domain="/"
      allowedOrigins="*"
      allowedMethods="GET, POST, PUT, PATCH, DELETE, OPTIONS"
      allowedHeaders="Content-Type, Accept, Authorization"
      exposeHeaders="Location"
      allowCredentials="true"
      maxAge="3600"/>
```

**What changed:** `allowedOrigins` is `*` instead of the real
`http://localhost:4200` (the Angular dev server's origin).

- **OWASP category:** A05:2021 – Security Misconfiguration (specifically, a
  permissive CORS policy). It is worth noting this combination is doubly bad:
  the CORS spec itself forbids `allowedOrigins="*"` together with
  `allowCredentials="true"` — browsers refuse to honor it — but Liberty (and
  some other servers) will still happily start with this config, so it either
  fails open in a confusing way or, on servers/proxies that don't enforce the
  spec strictly, actually reflects any origin.
- **Concrete risk in the Kanban app:** with credentialed requests
  (`allowCredentials="true"`) allowed from *any* origin, a malicious page the
  victim merely has open in another tab could issue authenticated
  `fetch()`/`XHR` calls to `/api/boards` on the victim's behalf and read the
  JSON response — full cross-site read access to the victim's boards, cards,
  and columns, using whatever session/cookie or stored bearer token the
  browser attaches.
- **One-line fix:** `allowedOrigins="http://localhost:4200"` — restrict CORS
  to the actual trusted Angular origin instead of wildcarding it.

## Snippet B — `microprofile-config.properties`

```properties
mp.jwt.verify.publickey.location=http://keycloak:8080/realms/kanban/protocol/openid-connect/certs
mp.jwt.verify.issuer=http://keycloak:8080/realms/kanban
mp.jwt.verify.audiences=kanban-app
```

**What changed:** `mp.jwt.verify.issuer` is
`http://keycloak:8080/realms/kanban` instead of the real
`http://localhost:8081/realms/kanban` — it was quietly swapped to match the
*key-fetching* URL instead of the URL Keycloak actually stamps into the
`iss` claim of tokens it issues to the browser.

- **OWASP category:** A07:2021 – Identification and Authentication Failures
  (a broken/incorrectly configured token validation check). It is adjacent to
  A05 too, since it is fundamentally a misconfigured security control.
- **Concrete risk in the Kanban app:** this is the subtle one — the public
  keys are still fetched correctly, so *signature* validation still works.
  But every real token Keycloak issues (as seen by the browser at
  `localhost:8081`) carries `iss=http://localhost:8081/realms/kanban`, which
  no longer matches the configured issuer. MicroProfile JWT rejects every
  token as invalid, so **every request fails with 401** — a full outage of
  the API, not a silent bypass. (The dangerous direction of this class of bug
  is the reverse: an issuer check that's missing or too loose, which would
  let a token from an *unintended* or attacker-controlled issuer be accepted
  — see the Debrief question below for why that matters even though this
  particular snippet fails safe.)
- **One-line fix:**
  `mp.jwt.verify.issuer=http://localhost:8081/realms/kanban` — the issuer
  must match the URL the browser (and thus Keycloak, when it stamps `iss`)
  uses, which is independent of the internal Docker-network URL used to fetch
  the JWKS.

## Snippet C — An Error Handler

```java
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return Response.status(500)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(Map.of("error", sw.toString()))
                       .build();
    }
}
```

**What changed:** this mapper doesn't exist in the real project at all — it's
a hypothetical addition that returns the full stack trace as the error body
for every unhandled exception.

- **OWASP category:** A05:2021 – Security Misconfiguration, specifically
  "verbose error messages containing sensitive information" (also overlaps
  with A09:2021 – Security Logging and Monitoring Failures, since leaking
  internals to the client is the wrong place to put diagnostic detail).
- **Concrete risk in the Kanban app:** stack traces reveal package/class
  names (`com.odelia.kanban...`), the JPA/Jakarta Data provider in use, SQL
  fragments on constraint violations, and internal file/line detail — a
  reconnaissance gift to an attacker probing for a further exploit, and a
  path for internal exceptions (e.g. a database error containing connection
  details) to leak into a JSON response any client can see. It also risks
  colliding with `ForbiddenExceptionMapper`: see the Stretch Task below.
- **One-line fix:** return a generic message and log the stack trace
  server-side instead, e.g.
  `.entity(Map.of("error", "Internal server error"))` combined with
  `LOGGER.log(Level.SEVERE, "Unhandled exception", exception);` — never put
  `exception.printStackTrace()`/`sw.toString()` in the response body.

## Snippet D — `docker-compose.yml`

```yaml
  backend:
    build: ./backend
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: kanban
      DB_USER: kanban
      DB_PASSWORD: kanban
      MP_JWT_VERIFY_PUBLICKEY_LOCATION: http://keycloak:8080/realms/kanban/protocol/openid-connect/certs
      ADMIN_API_TOKEN: sk_live_9f2c7a1e4b6d4c2f9a0e7b5d3c1a8f60
```

**What changed:** an `ADMIN_API_TOKEN` line with a live-looking secret
(`sk_live_...`) was added — the real `docker-compose.yml` has no such
variable at all.

- **OWASP category:** A02:2021 – Cryptographic Failures / secrets management
  (hardcoded credentials committed to source control); also squarely
  A05:2021 – Security Misconfiguration for shipping a real-looking production
  secret in a dev compose file.
- **Concrete risk in the Kanban app:** `docker-compose.yml` is checked into
  git and world-readable in the repository's history forever, even if the
  line is later deleted. A token prefixed `sk_live_` reads as a *production*
  secret; if it were real, anyone with read access to the repo (or its
  history, or a fork) could use it to authenticate as an admin against a live
  system — the DB credentials on the surrounding lines are a lesser version
  of the same problem, acceptable only because `kanban`/`kanban` is a
  disposable local Postgres instance, not a shared or production one.
- **One-line fix:** remove the hardcoded value and inject it from the
  environment/a secrets store instead, e.g.
  `ADMIN_API_TOKEN: ${ADMIN_API_TOKEN}` sourced from an untracked `.env` file
  (and rotate the token, since it must be treated as already leaked once
  committed).

## Debrief

**Why does swapping `localhost:8081` for `keycloak:8080` as the issuer matter,
even though `keycloak:8080` is where the public keys are correctly fetched
from?**

The public-key location and the issuer check are two independent controls
that happen to point at the same Keycloak instance in this setup, but answer
different questions:

- `mp.jwt.verify.publickey.location` answers "whose signature do I trust?" —
  it's purely about fetching the JWKS used to verify the cryptographic
  signature.
- `mp.jwt.verify.issuer` answers "did this token come from the identity
  provider I actually mean to trust?" — it's a plain string comparison
  against the `iss` claim inside the (already-verified) token payload.

They must both agree with what's *actually true of the token*, not with each
other. `keycloak:8080` is correct as the JWKS location because that's the
Docker-internal address the backend uses to fetch keys — but Keycloak stamps
`iss` with the address the *browser* used to authenticate, `localhost:8081`,
because that's the realm's configured frontend URL. If a real multi-realm or
multi-tenant Keycloak deployment existed, the issuer check is precisely what
would stop a validly-signed token from a *different, unintended* realm (one
that happens to share the same signing infrastructure, or one an operator
mistakenly value into the same properties file) from being accepted here —
signature validity alone only proves "some realm on this Keycloak server
issued this," not "the `kanban` realm issued this for this application."
Getting the issuer "right" by accident (making it match the JWKS host
instead of the true issuer) breaks that guarantee even though it happens to
fail closed in this single-realm demo.

## Stretch Task

**Why would Snippet C defeat `ForbiddenExceptionMapper`'s purpose if
registered alongside it?**

JAX-RS resolves exception mappers by picking the **most specific**
`ExceptionMapper<T>` registered for the thrown exception's type — walking up
the exception's class hierarchy and choosing the mapper bound to the nearest
matching type. `ForbiddenExceptionMapper implements
ExceptionMapper<ForbiddenException>` is specific to `ForbiddenException`, so
today it correctly wins over the container's generic catch-all (from the
`mpMetrics` feature) whenever `BoardAccess` throws a `ForbiddenException`.

`GenericExceptionMapper implements ExceptionMapper<Exception>` is bound to
`Exception` itself — the *broadest* possible type — so ordinarily it would
still lose to `ForbiddenExceptionMapper` for a `ForbiddenException`, since
`ForbiddenException` is more specific than `Exception`. The real danger is
what it does to *every other* exception type in the app that has no
dedicated mapper: today those fall through to Liberty's own catch-all and
are logged as `CWPMI2006W "unhandled exception"` server-side with a plain
500 to the client. With `GenericExceptionMapper` registered, every one of
those genuine bugs instead returns a full stack trace straight to the
caller — silently converting internal errors that should stay
server-side-only into a client-visible information leak, which is exactly
the risk Snippet C was flagged for above. It doesn't break the *ownership*
check itself, but it does undermine the broader intent behind having a
narrow, purpose-built mapper for expected security denials versus letting
unexpected failures leak detail.

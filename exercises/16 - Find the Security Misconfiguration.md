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

Do not open the real files yet — reason from the snippet first, then compare
with the actual project files during debrief.

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

## Snippet B — `microprofile-config.properties`

```properties
mp.jwt.verify.publickey.location=http://keycloak:8080/realms/kanban/protocol/openid-connect/certs
mp.jwt.verify.issuer=http://keycloak:8080/realms/kanban
mp.jwt.verify.audiences=kanban-app
```

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

## Completion Check

For all four snippets, be able to state:

- The OWASP category
- The concrete risk in this app
- The one-line fix

## Debrief

Compare your answers with the real files:

- `backend/src/main/liberty/config/server.xml`
- `backend/src/main/resources/META-INF/microprofile-config.properties`
- `backend/src/main/java/com/odelia/kanban/security/ForbiddenExceptionMapper.java`
  (note there is no generic catch-all mapper in the real project — Snippet C
  is a hypothetical addition, not a real file)
- `docker-compose.yml`

Discuss: Snippet A and B are both about trusting the wrong origin — one for
CORS, one for token issuance. Why does swapping `localhost:8081` for
`keycloak:8080` as the *issuer* matter, even though `keycloak:8080` is where
the public keys are correctly fetched from?

## Stretch Task

Snippet C would also defeat `ForbiddenExceptionMapper`'s purpose from
Exercise 13/15 if registered alongside it. Explain why, based on how JAX-RS
picks between exception mappers.

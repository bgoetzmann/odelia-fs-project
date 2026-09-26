---
title: Exercise 10 — Explore the Jakarta EE Backend
description: Connect Open Liberty configuration, Jakarta EE application code, and operational endpoints.
---

# Explore the Jakarta EE Backend

**Estimated time:** 40–50 minutes  
**Work mode:** Pairs

## Goal

Identify how project dependencies, Open Liberty features, configuration, and Java
source combine into the running backend.

## 1. Inspect the Runtime Configuration

Open:

- `backend/pom.xml`
- `backend/src/main/liberty/config/server.xml`
- `backend/src/main/webapp/WEB-INF/beans.xml`
- `backend/src/main/resources/META-INF/microprofile-config.properties`

Complete:

| Question | Your answer |
|---|---|
| Java version | 21 (`maven.compiler.release` in `pom.xml`) |
| Jakarta EE profile/version | Jakarta EE 11 **Web Profile** (`webProfile-11.0` in `server.xml`, `jakarta.jakartaee-web-api` 11.0.0 in `pom.xml`) |
| MicroProfile version | 7.1 (`microProfile-7.1` in `server.xml`, `org.eclipse.microprofile` 7.1 in `pom.xml`) — plus the standalone `mpMetrics-5.1` feature, since Metrics was dropped from the MP 7.1 umbrella |
| CDI activation file | `beans.xml` (`bean-discovery-mode="all"`) |
| Datasource JNDI name | `jdbc/kanban` (`<dataSource id="kanbanDataSource" jndiName="jdbc/kanban">` in `server.xml`) |
| Database configuration source | `server.xml` variables (`db.host`, `db.port`, `db.name`, `db.user`, `db.password`), overridable by the `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` environment variables set in `docker-compose.yml` |
| JWT issuer configuration file | `microprofile-config.properties` (`mp.jwt.verify.issuer`, alongside `mp.jwt.verify.publickey.location` and `mp.jwt.verify.audiences`) |

## 2. Inspect the Application Entry Point

Open `KanbanApplication.java`.

1. What is the application base path?
   `/api` (`@ApplicationPath("/api")`).
2. Which authentication method is enabled?
   `MP-JWT`, for the `kanban` realm (`@LoginConfig(authMethod = "MP-JWT", realmName = "kanban")`).
   This is what switches the whole application to bearer-token authentication:
   Liberty reads the `Authorization: Bearer` header and validates the token
   against the `mp.jwt.*` settings from `microprofile-config.properties`.
   Without it, the `@RolesAllowed` annotations on the resources would have no
   identity to check against.
3. What does the class inherit from?
   `jakarta.ws.rs.core.Application` — the standard JAX-RS class that activates
   the JAX-RS runtime and roots it at the application path.
4. Which URLs are therefore inside the secured JAX-RS application?
   Everything under `/api/**` (`BoardResource`, `BoardListResource`,
   `CardResource`, …). `/health/ready`, `/openapi`, `/openapi/ui`, and
   `/metrics` are **not** JAX-RS resources of this application and stay open.

## 3. Observe the Running Backend

Open or call:

```text
http://localhost:9080/health/ready
http://localhost:9080/openapi
http://localhost:9080/openapi/ui
http://localhost:9080/metrics
```

| Endpoint | Status | Representation | Purpose |
|---|---:|---|---|
| `/health/ready` | `200` | JSON (`{"status":"UP","checks":[...]}`) | MicroProfile Health readiness probe — is the app ready to receive traffic (here: can it reach PostgreSQL, via `DatabaseReadinessCheck`) |
| `/openapi` | `200` | YAML | The machine-readable OpenAPI contract, generated from the JAX-RS resources and entities |
| `/openapi/ui` | `200` | HTML (Swagger UI) | Interactive, browsable view of the same contract; endpoints can be called from the page |
| `/metrics` | `200` | Prometheus text format | Runtime and application measurements (JVM, HTTP, custom), scraped by monitoring tools |

## 4. Separate Responsibilities

Classify each artifact:

| Artifact                         | Dependency, runtime config, application config, or Java code?                                                                                                                                                        |
| -------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `pom.xml`                        | Dependency management (and build/packaging) — declares the Jakarta EE 11 Web Profile, MicroProfile 7.1, and the PostgreSQL driver as Maven dependencies, plus the Liberty Maven plugin that runs/packages the server |
| `server.xml`                     | Runtime config — Liberty features, HTTP endpoint, the `kanbanDataSource`, CORS, and which ops endpoints require authentication                                                                                       |
| `beans.xml`                      | Application config — turns on CDI bean discovery for the whole module                                                                                                                                                |
| `microprofile-config.properties` | Application config — the `mp.jwt.*` properties consumed by MicroProfile JWT (portable across MicroProfile runtimes, unlike a Liberty-specific `server.xml` element)                                                  |
| `KanbanApplication.java`         | Java code — the JAX-RS `Application` subclass that activates and secures the API                                                                                                                                     |
| `DatabaseReadinessCheck.java`    | Java code — a CDI bean implementing `HealthCheck`, injected with the `jdbc/kanban` datasource via `@Resource(lookup = "jdbc/kanban")`                                                                                |

## Completion Check

Both partners can explain how a Java class becomes reachable at an HTTP URL
inside Open Liberty.

## Instructor References

- [Backend project](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend)
- [JAX-RS application](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/KanbanApplication.java)

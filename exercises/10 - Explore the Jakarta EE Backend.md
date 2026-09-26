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
| Java version | |
| Jakarta EE profile/version | |
| MicroProfile version | |
| CDI activation file | |
| Datasource JNDI name | |
| Database configuration source | |
| JWT issuer configuration file | |

## 2. Inspect the Application Entry Point

Open `KanbanApplication.java`.

1. What is the application base path?
2. Which authentication method is enabled?
3. What does the class inherit from?
4. Which URLs are therefore inside the secured JAX-RS application?

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
| `/health/ready` | | | |
| `/openapi` | | | |
| `/openapi/ui` | | | |
| `/metrics` | | | |

## 4. Separate Responsibilities

Classify each artifact:

| Artifact | Dependency, runtime config, application config, or Java code? |
|---|---|
| `pom.xml` | |
| `server.xml` | |
| `beans.xml` | |
| `microprofile-config.properties` | |
| `KanbanApplication.java` | |
| `DatabaseReadinessCheck.java` | |

## Completion Check

Both partners can explain how a Java class becomes reachable at an HTTP URL
inside Open Liberty.

## Instructor References

- [Backend project](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend)
- [JAX-RS application](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/KanbanApplication.java)

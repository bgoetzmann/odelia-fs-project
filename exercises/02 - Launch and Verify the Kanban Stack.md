---
title: Exercise 2 — Launch and Verify the Kanban Stack
description: Start the development environment and verify each service independently.
---

# Launch and Verify the Kanban Stack

**Estimated time:** 30–45 minutes  
**Work mode:** Individual setup, then pair verification

## Goal

Start the remaining part of the development environment and collect evidence
that the frontend, backend, database, and identity provider are working.

You already cloned the repository, opened the Dev Container, and started the
backend in [[Exercises/01 - Inspect an HTTP Exchange]]. Keep that backend
terminal running and continue from here.

## 1. Confirm Your Environment

1. Confirm your `mvn liberty:dev` terminal from Exercise 1 is still running
   and `http://localhost:9080/health/ready` responds.
2. If it is not running anymore, restart it:

   ```bash
   cd backend
   mvn liberty:dev
   ```

PostgreSQL and Keycloak started automatically with the Dev Container and do
not need to be started manually.

## 2. Start the Frontend

Open a second VS Code terminal:

```bash
cd frontend
npm start
```

Leave this process running, then open:

```text
http://localhost:4200
```

Sign in with:

```text
Username: alice
Password: alice
```

## 3. Verify Each Part

| Part            | Expected evidence                                            | Result |
| --------------- | ------------------------------------------------------------ | ------ |
| Angular         | The application loads on port 4200                           |        |
| Jakarta EE      | `/health/ready` returns an UP response                       |        |
| PostgreSQL      | The readiness check reports the database check as UP         |        |
| Keycloak        | Login works on port 8081                                     |        |
| End-to-end path | The board list loads after login (the first time it's empty) |        |

## 4. Identify Where Things Run

For each item, record whether it is a container, a process inside the Dev
Container, or a browser page:

| Item | Container, process, or page? |
|---|---|
| PostgreSQL | |
| Keycloak | |
| `mvn liberty:dev` | |
| `npm start` | |
| Angular UI | |
| Keycloak login screen | |

## Completion Check

You are done when you can:

- Reload the Angular application successfully
- Sign in as `alice`
- Open the backend readiness endpoint
- Explain which services started automatically
- Name the two commands you must run manually
- Show the backend and frontend terminal output

## If Setup Fails

Do not reinstall tools immediately. Record:

1. The step that failed
2. The exact error message
3. The URL or command involved
4. Whether the failure is on the host, in a container, or in the browser
5. One hypothesis based on that evidence

Then compare with a partner or ask the instructor.

---
title: Exercise 5 — Diagnose a Service Failure
description: Use symptoms, requests, health information, and logs to locate a controlled failure.
---

# Diagnose a Service Failure

**Estimated time:** 30–45 minutes  
**Work mode:** Pairs

## Goal

Locate and explain a failure using evidence. Do not restart the entire
environment unless the evidence shows that this is necessary.

## Diagnostic Method

For every scenario:

1. Reproduce the symptom.
2. Identify the first failing boundary.
3. Observe the relevant URL, process, health result, or log.
4. Form one hypothesis.
5. Make one corrective change.
6. Repeat the original verification.

## Choose One Scenario

Each partner should choose a **different** scenario, so you can compare
symptoms with each other afterward.

### Scenario A — Backend Process Stopped

1. Stop `mvn liberty:dev` with `Ctrl+C`.
2. Reload the Angular application.
3. Inspect the failed request in the Network panel.
4. Test `http://localhost:9080/health/ready`.
5. Restart the backend and verify recovery.

### Scenario B — Frontend Process Stopped

1. Stop `npm start` with `Ctrl+C`.
2. Reload `http://localhost:4200`.
3. Compare the browser symptom with your partner's Scenario A result (or
   what you'd expect from a stopped backend, if your partner hasn't run it
   yet).
4. Restart the frontend and verify recovery.

### Scenario C — PostgreSQL Unavailable

1. Record the healthy `/health/ready` response.
2. Stop only your PostgreSQL container.
3. Recheck readiness and inspect the backend output.
4. Start PostgreSQL again.
5. Wait for readiness to return to UP.

### Scenario D — Authentication Service Unavailable

1. Sign out of the application.
2. Stop only your Keycloak container.
3. Attempt to sign in.
4. Record the browser-visible symptom.
5. Start Keycloak again and verify login.

## Evidence Record

| Question | Your answer |
|---|---|
| Scenario | |
| User-visible symptom | |
| First failing URL or command | |
| HTTP status or connection error | |
| Relevant process/container | |
| Relevant log evidence | |
| Hypothesis | |
| Corrective action | |
| Evidence of recovery | |

## Compare Failures

Discuss:

1. Why does a stopped frontend look different from a stopped backend?
2. Why is the readiness endpoint useful?
3. When are browser tools more useful than server logs?
4. When are server logs more useful than browser tools?
5. Why can restarting everything hide the original cause?

## Completion Check

You are done when:

- You restored the environment
- Angular and the readiness endpoint both respond
- You can explain the cause using collected evidence
- Your partner can distinguish your failure from at least one other scenario

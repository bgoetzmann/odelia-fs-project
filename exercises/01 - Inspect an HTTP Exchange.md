---
title: Exercise 1 — Inspect an HTTP Exchange
description: Use browser developer tools to identify the parts of a real HTTP request and response.
---

# Inspect an HTTP Exchange

**Estimated time:** 45–60 minutes (longer on first Dev Container build)  
**Work mode:** Pairs  
**Prerequisite:** Your own backend is running in your Dev Container (see Setup
below).

## Goal

Use browser developer tools to identify the method, URL, headers, status, and
body of a real HTTP exchange.

## Setup

1. Follow the environment setup using these [instructions](https://drive.google.com/file/d/1wl7v7j4Yonk6bWIzngmSFz-GqMOzxcbf/view?usp=sharing).
2. Wait for the container build and `postCreateCommand` to finish.
   PostgreSQL and Keycloak start automatically with the Dev Container.
3. In a VS Code terminal, start the backend:

   ```bash
   cd backend
   mvn liberty:dev
   ```

   Leave this process running for the rest of the exercise. Wait until Liberty
   reports that the application is ready.

## Instructions

1. Open `http://localhost:9080/health/ready` in a browser.
2. Open the browser developer tools.
3. Select the **Network** panel.
4. Reload the page.
5. Select the request for `/health/ready`.
6. Record your observations in the table below.

| Observation | Your answer |
|---|---|
| HTTP method | |
| Complete URL | |
| Scheme | |
| Host | |
| Port | |
| Path | |
| Response status | |
| Response content type | |
| Response body | |
| Request duration | |

## Interpret the Exchange

Discuss and record:

1. Which side initiated the exchange?
2. Which process received the request?
3. What does the status code tell you?
4. What evidence shows that the body is JSON?
5. Does a successful HTTP response necessarily mean that every application
   feature works? Explain.

## Compare Browser and Command Line

Run this inside the Dev Container:

```bash
curl -i http://localhost:9080/health/ready
```

Compare the command-line result with the Network panel:

- Which information appears in both?
- Which information is presented differently?
- Which tool would you prefer in an automated diagnostic script?

## Completion Check

You are done when both partners can point to:

- The request method and URL
- At least two response headers
- The response status
- The JSON response body
- The client and server involved

## Stretch Task

Open `http://localhost:9080/openapi` and repeat the inspection. Explain how the
response differs from `/health/ready`.

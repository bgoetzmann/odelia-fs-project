# Exercise — A `/api/me` endpoint, and an account panel that shows it

**Track:** Full-stack (MicroProfile JWT + Angular)
**Starting point:** commit `a02c4d7` (token-based security: Keycloak, MicroProfile JWT, per-board ownership)
**Estimated time:** 60–90 min
**New backend code:** one small JAX-RS resource — everything it needs is already injectable

---

## Context

At this starting point the app is secured, but the identity behind it is invisible. The
header shows `alice`, and that string comes from the *frontend's* copy of the
token (`AuthService` reads `tokenParsed.preferred_username`). Nobody has checked
what the **backend** thinks it received — and when a token is rejected, "what
does the server actually see?" is the first question worth answering.

`CurrentUser` already wraps the token on the backend:

```java
@Inject
JsonWebToken jwt;

public String name() { ... }
public boolean isAdmin() { ... }
```

`JsonWebToken` gives you far more than that — `getIssuer()`, `getAudience()`,
`getGroups()`, `getExpirationTime()`, `getClaimNames()`. Your job is to expose a
useful slice of it and show it in the UI.

## Goal

A new endpoint:

```
GET /api/me     →  200 with the caller's identity, 401 without a token
```

returning something like:

```json
{
  "username": "carol",
  "subject": "1f0e...-...",
  "roles": ["user", "admin"],
  "issuer": "http://localhost:8081/realms/kanban",
  "expiresAt": "2026-09-07T18:42:11Z",
  "admin": true
}
```

and, in the Angular app, a **Account** panel in the header that fetches it and
displays it — so the browser's view of the token and the server's view sit side
by side.

## How to approach this

- **Minimal hints** — work from the **Goal** and **Acceptance criteria** alone.
- **Guided** — follow the numbered **Suggested steps** below.

## Getting started

```bash
git checkout a02c4d7
git switch -c exercise/jwt-me
docker compose up -d postgres keycloak
cd backend && mvn liberty:dev        # http://localhost:9080, leave it running
# in another terminal:
cd frontend && npm install
npm start                            # http://localhost:4200
npm test                             # keep this green
```

| File | What changes |
|---|---|
| `backend/.../resource/MeResource.java` | **new file** — the endpoint |
| `backend/.../resource/Identity.java` | **new file** — a `record` for the response |
| `backend/.../security/CurrentUser.java` | maybe: a `roles()` accessor |
| `frontend/src/app/auth/auth.service.ts` | maybe: nothing, if the panel has its own service |
| `frontend/src/app/auth/identity.service.ts` | **new file** — `GET /api/me` |
| `frontend/src/app/app.component.*` | the panel and its toggle |

## Suggested steps

1. **The response shape.** A Java `record` serialises to JSON with no extra
   work — `MoveCommand` in the same package is the in-repo example:

   ```java
   public record Identity(String username, String subject, Set<String> roles,
                          String issuer, Instant expiresAt, boolean admin) {
   }
   ```

2. **The resource.** Copy the shape of `CardResource`: `@Path("/me")`,
   `@RequestScoped`, `@Produces(MediaType.APPLICATION_JSON)`, and — this is the
   point of the exercise — `@RolesAllowed({ CurrentUser.USER, CurrentUser.ADMIN })`.
   Inject `CurrentUser` and `JsonWebToken` and map one onto the other.

   `getExpirationTime()` returns seconds since the epoch, so
   `Instant.ofEpochSecond(...)`.

3. **Check it with curl** before touching Angular — the `TOKEN=$(...)` snippet in
   the root `README.md` gets you a token:

   ```bash
   curl -H "Authorization: Bearer $TOKEN" http://localhost:9080/api/me | jq
   curl -i http://localhost:9080/api/me          # expect 401
   ```

4. **A service for it.** `IdentityService` is three lines and looks exactly like
   `BoardService` — inject `HttpClient`, `GET ${environment.apiUrl}/me`. You do
   **not** add the token yourself: `authInterceptor` already does.

5. **The panel.** Make the username in the header a button that toggles a small
   panel. Load the identity once, lazily, into a signal:

   ```ts
   readonly identity = signal<Identity | null>(null);

   toggleAccount(): void {
     this.accountOpen.update(open => !open);
     if (this.accountOpen() && !this.identity()) {
       this.identityService.me().subscribe(me => this.identity.set(me));
     }
   }
   ```

6. **Test the endpoint's contract on the frontend side.** A spec for
   `IdentityService` in the style of `auth.interceptor.spec.ts`: assert the URL,
   flush a fake identity, assert what comes out.

## Acceptance criteria

- [ ] `GET /api/me` returns 200 and the caller's identity with a valid token.
- [ ] `GET /api/me` returns 401 with no token, and with a token from another realm.
- [ ] `roles` contains `admin` for `carol` and not for `alice`.
- [ ] `username` matches what the header already shows, for all three accounts.
- [ ] The account panel opens from the header and shows the backend's answer.
- [ ] The panel's request carries `Authorization: Bearer …` **without**
      `IdentityService` doing anything about it.
- [ ] `npm test` and `npm run build` both pass; `mvn compile` passes.

## Stretch goals

- **Show the clock.** Add a countdown to `expiresAt`. The realm sets a 5-minute
  access token lifespan on purpose — keep the panel open, watch the token get
  refreshed by the interceptor, and re-fetch `/api/me` to see a new `expiresAt`.
- **Break it on purpose.** Change `mp.jwt.verify.audiences` to something else and
  restart. What status comes back? What does Liberty log? Put it back. Repeat
  with `mp.jwt.verify.issuer`. This is the fastest way to learn which knob
  produces which failure.
- **`@RolesAllowed("admin")`.** Add `GET /api/me/all` listing every board's owner
  and card count, restricted to admins. Sign in as `alice` (403) and `carol`
  (200).

## Reference solution

None committed. Background reading: the MicroProfile JWT `JsonWebToken` javadoc,
`backend/.../security/CurrentUser.java` for the injection idiom, and
`backend/JAKARTA_MP_INTRO.md`.

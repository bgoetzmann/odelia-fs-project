# Introduction to Jakarta EE + MicroProfile — via the Kanban backend

A tour of Jakarta EE 11 and MicroProfile 7.1 using the code that actually
lives in `backend/src/main/java/com/odelia/kanban/`. Every example below is a
real file from this project — read it alongside the source with your editor
open. Its Angular counterpart is `frontend/ANGULAR_INTRO.md`.

## 1. What you get, and from where

Two specifications stack together here, and it's worth knowing which is
which:

- **Jakarta EE 11 Web Profile** — the "container" specs: CDI (dependency
  injection), JAX-RS (REST endpoints), JPA (persistence), Bean Validation,
  JSON-B, and — new in EE 11 — **Jakarta Data**.
- **MicroProfile 7.1** — the "cloud-native" specs layered on top: Config,
  Health, OpenAPI, JWT, Rest Client, Telemetry. (Metrics used to be part of
  this umbrella; see §8.)

Both arrive as a single dependency each in `backend/pom.xml`:

```xml
<dependency>
    <groupId>jakarta.platform</groupId>
    <artifactId>jakarta.jakartaee-web-api</artifactId>
    <version>11.0.0</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.eclipse.microprofile</groupId>
    <artifactId>microprofile</artifactId>
    <version>7.1</version>
    <type>pom</type>
    <scope>provided</scope>
</dependency>
```

`scope=provided` because these are just APIs — **OpenLiberty**, the
application server, supplies the actual implementations at runtime. That's
also why nothing is `new`'d directly anywhere in this codebase: the container
creates and wires everything (see §3).

Which features are actually turned on is Liberty's own concern, declared in
`backend/src/main/liberty/config/server.xml`:

```xml
<featureManager>
    <feature>webProfile-11.0</feature>
    <feature>microProfile-7.1</feature>
    <feature>mpMetrics-5.1</feature>
</featureManager>
```

## 2. Entities: plain Java, mapped to tables

```java
// backend/src/main/java/com/odelia/kanban/entity/Board.java
@Entity
@Table(name = "BOARD")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    @Size(max = 120)
    @Column(name = "OWNER", length = 120)
    private String owner;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    // constructors, getters, setters...
}
```

This is **JPA** (persistence) and **Bean Validation** in the same class:

- `@Entity` + `@Table` + `@Id` + `@Column` map the class to a `BOARD` table —
  no SQL, no XML mapping file.
- `@NotBlank` and `@Size(max = 120)` are validation constraints. They're
  enforced automatically the moment a `Board` is annotated `@Valid` in a
  JAX-RS method (see `BoardResource.createBoard` in §4) — an invalid payload
  never reaches your code, JAX-RS rejects it with `400 Bad Request` first.
- `@JsonbTransient` on `isNew()` tells **JSON-B** (the JSON mapping spec)
  to skip that computed getter when serializing to JSON — only real fields
  go over the wire.

`BoardList` (a column of a board) is the same pattern, but references its
parent by a plain `boardId` field instead of a JPA `@ManyToOne`:

```java
// backend/src/main/java/com/odelia/kanban/entity/BoardList.java
@Column(name = "BOARD_ID", nullable = false)
private long boardId;
```

That's a deliberate simplification — it keeps the Jakarta Data repositories
below trivial and keeps the JSON flat (`{ "boardId": 3, "name": "Doing" }`
instead of a nested `board` object).

## 3. Jakarta Data: repositories with no implementation

This is the newest piece — **Jakarta Data 1.0**, introduced in Jakarta EE 11.
Compare it to old-school JPA: instead of writing a `DAO` class that calls
`EntityManager`, you declare an *interface*, and the container generates the
implementation for you at deploy time.

```java
// backend/src/main/java/com/odelia/kanban/repository/BoardRepository.java
@Repository(dataStore = "jdbc/kanban")
public interface BoardRepository extends CrudRepository<Board, Long> {

    @Find
    @OrderBy("name")
    List<Board> findAllSortedByName();
}
```

- `CrudRepository<Board, Long>` already provides `save`, `insert`, `update`,
  `findById`, `findAll`, `delete`, `deleteById` — none of that is
  hand-written anywhere in this project.
- `findAllSortedByName()` is the one extra query this repository needs;
  `@Find` + `@OrderBy` is enough to generate it.
- `dataStore = "jdbc/kanban"` is a JNDI name — it points at the
  `<dataSource>` declared in `server.xml` (§7). Liberty derives the
  persistence unit, and creates the tables at startup, from the entity types
  it sees used by the repository.

`BoardListRepository` shows the other Jakarta Data trick — **query
derivation from the method name**, no annotation at all:

```java
// backend/src/main/java/com/odelia/kanban/repository/BoardListRepository.java
public interface BoardListRepository extends CrudRepository<BoardList, Long> {

    List<BoardList> findByBoardIdOrderByPositionAsc(long boardId);

    long countByBoardId(long boardId);

    void deleteByBoardId(long boardId);
}
```

Read `findByBoardIdOrderByPositionAsc` as a sentence: find `BoardList`
entities `By BoardId`, `OrderBy Position Asc`. Jakarta Data parses the method
name and builds the query — this is a compile-time contract, not reflection
magic discovered at runtime; get the name wrong and deployment fails fast.

## 4. JAX-RS: REST endpoints as plain methods

`BoardResource` is the HTTP-facing layer — JAX-RS annotations turn plain
Java methods into REST endpoints:

```java
// backend/src/main/java/com/odelia/kanban/resource/BoardResource.java
@Path("/boards")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BoardResource {

    @Inject
    BoardRepository boards;

    @Inject
    BoardListRepository lists;

    @GET
    public List<Board> listBoards() {
        return boards.findAllSortedByName();
    }

    @GET
    @Path("/{id}")
    public Board getBoard(@PathParam("id") long id) {
        return boards.findById(id).orElseThrow(() -> notFound(id));
    }

    @POST
    @Transactional
    public Response createBoard(@Valid Board board) {
        // ...
        Board created = boards.insert(board);
        return Response.created(uriInfo.getAbsolutePathBuilder()
                                        .path(String.valueOf(created.getId())).build())
                       .entity(created)
                       .build();
    }
}
```

The annotations to recognize:

| Annotation                          | Meaning                                             |
|--------------------------------------|------------------------------------------------------|
| `@Path("/boards")`                   | this class handles `/api/boards` (the `/api` prefix comes from `KanbanApplication`, §5) |
| `@GET` / `@POST` / `@PUT` / `@DELETE`| HTTP method the method responds to                  |
| `@Path("/{id}")` on a method         | appends to the class path; `@PathParam("id")` reads it |
| `@Produces` / `@Consumes`            | content type in/out — `application/json`, handled by JSON-B automatically |
| `@Valid`                             | run Bean Validation on the parameter before the method body runs |
| `@Transactional`                     | wrap the method in a JTA transaction — commits on return, rolls back on exception |
| `@RequestScoped`                     | a fresh instance per HTTP request (see §6)          |

`createBoard` returns `Response.created(...)` — a `201 Created` with a
`Location` header pointing at the new resource, which is the conventional
REST response shape for a `POST`. Compare `updateBoard`, which just returns
the entity directly (JAX-RS wraps it in a `200 OK` automatically):

```java
@PUT
@Path("/{id}")
@Transactional
public Board updateBoard(@PathParam("id") long id, @Valid Board board) {
    Board existing = boards.findById(id).orElseThrow(() -> notFound(id));
    existing.setName(board.getName());
    return boards.save(existing);
}
```

Errors follow the same "just Java" philosophy — no special exception
framework, just a `WebApplicationException` carrying a status code:

```java
private WebApplicationException notFound(long id) {
    return new WebApplicationException("No board with id " + id, Response.Status.NOT_FOUND);
}
```

`BoardListResource` (`/api/lists/{id}`) is the same pattern for one column at
a time — get/update/delete, plus the nested `GET/POST /api/lists/{id}/cards` —
while `BoardResource` also owns `GET/POST /api/boards/{id}/lists`, since a
column always belongs to a board.

### 4a. `PATCH` and a record as the request body: the card move endpoint

Day 2's `CardResource` adds one endpoint that isn't plain CRUD —
`PATCH /api/cards/{id}/move`, called by the drag-and-drop UI every time a card
is dropped. `@PATCH` (`jakarta.ws.rs.PATCH`) has been a standard annotation
since Jakarta REST 3.1, alongside `@GET`/`@POST`/`@PUT`/`@DELETE`.

The request body is a **Java record**, not an entity:

```java
// backend/src/main/java/com/odelia/kanban/resource/MoveCommand.java
public record MoveCommand(long targetListId, @PositiveOrZero int position) {
}
```

JSON-B deserializes `{"targetListId": 2, "position": 0}` straight into the
record's canonical constructor, and `@Valid` on the parameter still runs the
Bean Validation constraints (`@PositiveOrZero`). A small immutable record is
the natural shape for a command that isn't a persisted resource:

```java
@PATCH
@Path("/{id}/move")
@Transactional
public Card move(@PathParam("id") long id, @Valid MoveCommand command) {
    // move the card to command.targetListId() at command.position(),
    // then renumber the affected column(s) so positions stay 0,1,2,...
}
```

Keeping `position` contiguous is the resource's job, not the database's —
there's no `ON DELETE CASCADE` or trigger; `deleteCard`, `deleteColumn` and
`deleteBoard` each clean up their children explicitly in Java, inside the same
`@Transactional` method.

## 5. Wiring JAX-RS into the app

```java
// backend/src/main/java/com/odelia/kanban/resource/KanbanApplication.java
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Kanban API", version = "1.0.0"))
public class KanbanApplication extends Application {
}
```

An empty class, but it does two things: `@ApplicationPath("/api")` is *the*
reason every resource in this app lives under `/api/...` (this is what the
frontend's `proxy.conf.json` forwards, per `ANGULAR_INTRO.md` §5), and
`@OpenAPIDefinition` is a MicroProfile OpenAPI annotation — Liberty uses it
(plus the JAX-RS annotations above) to generate a live OpenAPI document with
no extra code, browsable at `/openapi`.

## 6. CDI: how `@Inject` finds something to inject

Nowhere in this codebase does anything call `new BoardRepository()` or `new
BoardResource()`. That's **CDI** (Contexts and Dependency Injection) — the
backbone Jakarta EE is built on:

- `BoardRepository` and `BoardListRepository` are Jakarta Data
  `@Repository` interfaces — CDI-manage beans are generated for them
  automatically.
- `@Inject BoardRepository boards;` on a field asks the CDI container to
  supply an instance — no constructor call needed anywhere.
- `@RequestScoped` on `BoardResource` controls its **lifecycle**: a new
  instance per HTTP request. Compare `DatabaseReadinessCheck`, which is
  `@ApplicationScoped` — one instance for the whole app's lifetime, since a
  health check has no per-request state.

CDI is switched on for the whole WAR by one (empty, on purpose) file:

```xml
<!-- backend/src/main/webapp/WEB-INF/beans.xml -->
<beans ... bean-discovery-mode="all">
</beans>
```

`bean-discovery-mode="all"` means every class in the archive is a candidate
CDI bean — nothing needs an explicit `@Named` or XML declaration to be
discovered.

## 7. MicroProfile Config: externalizing a value

Anything that changes between a laptop, a Docker network and a real deployment
belongs outside the code. The clearest example in this project is the day 3 JWT
setup — nothing in Java mentions Keycloak at all:

```properties
# backend/src/main/resources/META-INF/microprofile-config.properties
mp.jwt.verify.publickey.location=http://keycloak:8080/realms/kanban/protocol/openid-connect/certs
mp.jwt.verify.issuer=http://localhost:8081/realms/kanban
mp.jwt.verify.audiences=kanban-app
```

Your own properties are read the same way, injected wherever they're needed:

```java
@Inject
@ConfigProperty(name = "kanban.some.knob", defaultValue = "a sensible default")
String knob;
```

MicroProfile Config reads from several sources in a defined priority order, and
this properties file is just the lowest-priority, always-present default. The
environment beats it, which is why running Liberty on the host instead of in
Docker needs no edit — only:

```bash
export MP_JWT_VERIFY_PUBLICKEY_LOCATION=http://localhost:8081/realms/kanban/protocol/openid-connect/certs
```

(dots become underscores, all upper case). This is the same mechanism
`server.xml` uses for its own knobs, just at the Liberty level instead of the
app level:

```xml
<!-- backend/src/main/liberty/config/server.xml -->
<variable name="db.host" defaultValue="localhost"/>
```

— overridable by the `DB_HOST` environment variable from `docker-compose.yml`,
without touching any file.

## 8. MicroProfile Health: are we ready?

```java
// backend/src/main/java/com/odelia/kanban/health/DatabaseReadinessCheck.java
@Readiness
@ApplicationScoped
public class DatabaseReadinessCheck implements HealthCheck {

    @Resource(lookup = "jdbc/kanban")
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (Connection connection = dataSource.getConnection()) {
            return HealthCheckResponse.builder()
                                      .name("kanban-database")
                                      .status(connection.isValid(2))
                                      .build();
        } catch (Exception e) {
            return HealthCheckResponse.builder()
                                      .name("kanban-database")
                                      .down()
                                      .withData("error", String.valueOf(e.getMessage()))
                                      .build();
        }
    }
}
```

Implement `HealthCheck`, annotate `@Readiness` (there's also `@Liveness` and
`@Startup` for other probes), and Liberty automatically exposes it at
`/health/ready` alongside every other readiness check in the app — this is
the kind of endpoint a container orchestrator polls before routing traffic to
an instance. `@Resource(lookup = "jdbc/kanban")` is plain Jakarta EE resource
injection, pulling in the same data source the Jakarta Data repositories use.

Metrics follow a similar philosophy but need no code at all here — Liberty's
`mpMetrics-5.1` feature (`server.xml`) auto-exposes JVM and JAX-RS metrics at
`/metrics` in Prometheus format. It's called out separately in `server.xml`
because MicroProfile 7.1 dropped Metrics from its umbrella spec in favor of
`mpTelemetry`, but Liberty still ships it standalone — worth knowing since
"MicroProfile Metrics" and "MicroProfile Telemetry" get easily confused.

## 9. server.xml: the one file that isn't Java

Everything above is portable Jakarta EE / MicroProfile code — it would run
unmodified on any compliant server. `server.xml` is OpenLiberty-specific
configuration: which features are on, and how the runtime is wired.

```xml
<!-- backend/src/main/liberty/config/server.xml -->
<featureManager>
    <feature>webProfile-11.0</feature>
    <feature>microProfile-7.1</feature>
    <feature>mpMetrics-5.1</feature>
</featureManager>

<dataSource id="kanbanDataSource" jndiName="jdbc/kanban">
    <jdbcDriver libraryRef="postgresql-library"/>
    <properties.postgresql serverName="${db.host}" portNumber="${db.port}"
                           databaseName="${db.name}" user="${db.user}" password="${db.password}"/>
</dataSource>

<cors domain="/"
      allowedOrigins="http://localhost:4200"
      allowedMethods="GET, POST, PUT, PATCH, DELETE, OPTIONS"
      allowedHeaders="Content-Type, Accept, Authorization"
      allowCredentials="true"/>
```

Three things worth noticing:

- The `jndiName="jdbc/kanban"` here is exactly the string used in
  `@Repository(dataStore = "jdbc/kanban")` and `@Resource(lookup =
  "jdbc/kanban")` above — this XML element is the one and only place the
  actual database connection is configured; everywhere else in the Java code
  just refers to it by name.
- `<cors>` exists purely because of the dev setup: `ng serve` runs the
  Angular app on `http://localhost:4200` while Liberty listens on `9080` —
  two origins — so the browser needs an explicit CORS allowance for local
  development. (In production, once both are served from behind the same
  reverse proxy, this becomes unnecessary — same story as the Angular-side
  proxy in `proxy.conf.json`.)
- `<mpMetrics authentication="false"/>` stays open even after day 3 secures the
  API: `/metrics` and `/health` carry no user data, and the readiness probe in
  `docker-compose.yml` has no token to present. Deciding *which* endpoints an
  authentication requirement should cover is part of the design, not an
  afterthought.
- Notice what is **not** here after day 3: no `<mpJwt>` element. The issuer, the
  JWKS location and the expected audience are MicroProfile Config properties
  (§7), so the same configuration would work on any MicroProfile runtime, not
  just Liberty.

## 10. Where this goes next

Day 2 (above) added the `Card` triplet and the `/move` endpoint. Day 3 brought
Keycloak into the loop: `@LoginConfig(authMethod = "MP-JWT")` on
`KanbanApplication` puts the whole API behind bearer tokens, `@RolesAllowed`
guards each resource, and the new `security/` package answers the two questions
a role alone cannot — `CurrentUser` (`@Inject JsonWebToken`, so a board can be
tagged with its creator) and `BoardAccess` (is this board, or the board behind
this column or card, actually yours?). Day 4 turns that owner-only rule into a
shareable `BoardMember` model. The shapes introduced here — entity + repository
+ resource, one triplet per concept — are the pattern the rest of the backend
builds on.

## Further reading

- [jakarta.ee/specifications](https://jakarta.ee/specifications/) — spec index (Jakarta EE 11)
- [Jakarta Data 1.0 spec](https://jakarta.ee/specifications/data/1.0/) — the repository pattern used in §3
- [MicroProfile Config](https://microprofile.io/specifications/) — spec index for Config, Health, OpenAPI, JWT, etc.
- [OpenLiberty docs](https://openliberty.io/docs/) — `server.xml` reference and feature list

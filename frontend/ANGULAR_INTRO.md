# Introduction to Angular — via the Kanban frontend

This is a tour of Angular's core concepts using the code that actually lives in
`frontend/src/app/`. Rather than a generic "hello world," every example below
is a real file from this project. Read it alongside the source with your
editor open.

## 1. What Angular gives you

Angular is a batteries-included framework for building single-page apps in
TypeScript. Out of the box you get:

- **Components** — reusable pieces of UI (a template + a class + styles)
- **A router** — URL ↔ component mapping, no page reloads
- **Dependency injection** — services get handed to whoever needs them
- **`HttpClient`** — typed HTTP calls, returning RxJS `Observable`s
- **Signals** — a reactive primitive for state that Angular can track efficiently

The Kanban frontend is small — two routes, a handful of feature components, one
service — but it touches every one of these.

## 2. Bootstrapping: `main.ts` + `app.config.ts`

Modern Angular (v14+) doesn't need `NgModule`s. The app starts from a single
function call:

```ts
// frontend/src/main.ts
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
```

`appConfig` is where you register the app-wide services — this is
**dependency injection** configured at the root:

```ts
// frontend/src/app/app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withFetch())
  ]
};
```

- `provideRouter(routes)` wires up the router (see §4).
- `provideHttpClient(withFetch())` makes `HttpClient` injectable anywhere in
  the app, using the browser's `fetch` API under the hood.

## 3. Components: template + class + styles

A component is the basic building block. `AppComponent` is the root shell:

```ts
// frontend/src/app/app.component.ts
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'kanban';
}
```

```html
<!-- frontend/src/app/app.component.html -->
<header class="app-header">
  <h1>Kanban</h1>
  <span class="tagline">Day 1 — boards and their columns, no security yet</span>
</header>

<main>
  <router-outlet />
</main>
```

Three files, one concept: `selector` is the HTML tag (`<app-root>`, dropped
into `index.html`), `templateUrl`/`styleUrl` point at the view and its scoped
CSS, and `imports` lists the other standalone building blocks this component's
template uses — here just `RouterOutlet`, the placeholder where routed
components get rendered.

There's no `NgModule` anywhere. This is a **standalone component** — each
component declares its own dependencies via `imports`, instead of everything
being declared once in a shared module. It's the default in current Angular.

## 4. Routing: URL → component

```ts
// frontend/src/app/app.routes.ts
export const routes: Routes = [
  { path: '', component: BoardListComponent },
  { path: 'boards/:id', component: BoardDetailComponent },
  { path: '**', redirectTo: '' }
];
```

Two real routes: `/` renders `BoardListComponent`, and `/boards/:id` renders
`BoardDetailComponent` — the `:id` segment is a route parameter the component
reads with `ActivatedRoute` (`this.route.snapshot.paramMap.get('id')`). Any
unmatched path (`**`) redirects back to the list. `<router-outlet />` in
`app.component.html` is where the matched component gets inserted into the page,
and `[routerLink]="['/boards', board.id]"` in the board list is what navigates
there without a full page reload.

## 5. Services and dependency injection

Components shouldn't talk to the network directly — that's a service's job.
`BoardService` is the single entry point to the REST API:

```ts
// frontend/src/app/services/board.service.ts
@Injectable({ providedIn: 'root' })
export class BoardService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/boards`;

  getBoards(): Observable<Board[]> {
    return this.http.get<Board[]>(this.baseUrl);
  }

  createBoard(name: string): Observable<Board> {
    return this.http.post<Board>(this.baseUrl, { name });
  }

  deleteBoard(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getLists(boardId: number): Observable<BoardList[]> {
    return this.http.get<BoardList[]>(`${this.baseUrl}/${boardId}/lists`);
  }
}
```

- `@Injectable({ providedIn: 'root' })` registers exactly one instance of this
  service for the whole app — a singleton, created lazily the first time
  something injects it.
- `inject(HttpClient)` is the modern way to grab a dependency (an alternative
  to the older constructor-parameter style). No `new HttpClient()` anywhere —
  Angular's injector hands it over.
- Every method returns an RxJS `Observable`. Nothing happens until something
  calls `.subscribe(...)` — see `BoardListComponent` below.

`environment.apiUrl` is `'/api'` in dev (`frontend/src/environments/environment.ts`),
and `frontend/proxy.conf.json` forwards `/api` to the OpenLiberty backend on
port 9080 while `ng serve` is running. The browser only ever talks to one
origin, so there's no CORS to worry about in development.

## 6. Signals: the state layer

`BoardListComponent` is the one non-trivial component in the app, and it's a
good example of Angular's **signals** — the newer reactive primitive that
replaced a lot of what used to require RxJS or `Zone.js` change detection:

```ts
// frontend/src/app/boards/board-list.component.ts
export class BoardListComponent implements OnInit {
  private readonly boardService = inject(BoardService);

  readonly boards = signal<Board[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly selectedBoardId = signal<number | null>(null);
  readonly columns = signal<BoardList[]>([]);

  newBoardName = '';

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading.set(true);
    this.error.set(null);
    this.boardService.getBoards().subscribe({
      next: boards => {
        this.boards.set(boards);
        this.loading.set(false);
      },
      error: err => this.fail('Could not load the boards', err)
    });
  }
  // ...
}
```

A `signal<T>()` is a container for a value that Angular can watch. Three
things you do with one:

- **Read it** by calling it as a function: `this.boards()` — this is also how
  the template reads it (see §7).
- **Set it**: `this.boards.set(boards)` replaces the value outright.
- **Update it** from its current value: `this.boards.update(boards => [...boards, board])`
  (used in `createBoard()`) — the functional-update form, safer than
  read-then-set when the new value depends on the old one.

`ngOnInit` is a **lifecycle hook** — Angular calls it once, right after the
component's inputs are set, before it's shown. It's the conventional place to
kick off an initial data load, which is exactly what happens here: call the
service, and when the `Observable` emits, push the result into a signal.

## 7. Templates: control flow and data binding

The template reads those signals and reacts to them. Angular's newer
"block" syntax (`@if`, `@for`, `@empty`) replaces the older `*ngIf`/`*ngFor`
directives:

```html
<!-- frontend/src/app/boards/board-list.component.html -->
<form class="new-board" (ngSubmit)="createBoard()">
  <input
    name="newBoardName"
    [(ngModel)]="newBoardName"
    placeholder="New board name"
    autocomplete="off"
    maxlength="120" />
  <button type="submit" [disabled]="!newBoardName.trim()">Create board</button>
</form>

@if (loading()) {
  <p class="muted">Loading boards…</p>
} @else if (boards().length === 0) {
  <p class="muted">No board yet. Create the first one above.</p>
} @else {
  <ul class="board-list">
    @for (board of boards(); track board.id) {
      <li class="board">
        <button class="board-name" type="button" (click)="toggleColumns(board)">
          {{ board.name }}
        </button>
        ...
      </li>
    }
  </ul>
}
```

The binding syntax to recognize:

| Syntax             | Direction              | Example here                          |
|--------------------|-------------------------|----------------------------------------|
| `{{ expr }}`        | class → template         | `{{ board.name }}`                     |
| `[prop]="expr"`     | class → template (property) | `[disabled]="!newBoardName.trim()"` |
| `(event)="handler"` | template → class (event) | `(click)="toggleColumns(board)"`      |
| `[(ngModel)]="prop"`| both ways (two-way)      | `[(ngModel)]="newBoardName"`           |

`[(ngModel)]` needs `FormsModule` in the component's `imports` array — it's
the classic "banana in a box" syntax combining `[ngModel]` (write the value
into the input) and `(ngModelChange)` (read it back out on every keystroke).

`@for (board of boards(); track board.id)` iterates a signal's value directly
(note the `()` call) and `track board.id` tells Angular how to match items
across re-renders efficiently, instead of tearing down and rebuilding the DOM
for the whole list every time.

## 8. Talking to the backend, end to end

Putting it together, here's what happens when a user creates a board:

1. They type into the input; `[(ngModel)]` keeps `newBoardName` in sync.
2. They submit the form; `(ngSubmit)="createBoard()"` calls the component method.
3. `createBoard()` calls `boardService.createBoard(name)`, which returns an
   `Observable<Board>` — no HTTP request has been sent yet.
4. `.subscribe({ next, error })` is what actually fires the `POST /api/boards`
   request and reacts to the result.
5. On success, `this.boards.update(...)` adds the new board to the signal.
6. Because the template reads `boards()`, Angular's change detection
   re-renders the `@for` block automatically — no manual DOM manipulation.

```ts
createBoard(): void {
  const name = this.newBoardName.trim();
  if (!name) return;
  this.error.set(null);
  this.boardService.createBoard(name).subscribe({
    next: board => {
      this.boards.update(boards => [...boards, board].sort((a, b) => a.name.localeCompare(b.name)));
      this.newBoardName = '';
    },
    error: err => this.fail(`Could not create the board "${name}"`, err)
  });
}
```

## 9. Types shared with the backend

```ts
// frontend/src/app/models/board.ts
/** Mirrors com.odelia.kanban.entity.Board on the backend. */
export interface Board {
  id?: number;
  name: string;
  owner?: string;
  createdAt?: string;
}
```

These interfaces aren't generated — they're hand-written to match the JSON
shape the JAX-RS `BoardResource` on the backend serializes. It's a common
pattern in small full-stack apps without a shared schema/codegen step: keep
one `interface` per entity, and update it by hand when the backend DTO
changes. `HttpClient`'s generics (`this.http.get<Board[]>(...)`) then give you
compile-time checked, autocompleted JSON responses.

## 10. Drag and drop with the Angular CDK

Day 2's board detail view uses the [Angular CDK](https://material.angular.io/cdk/drag-drop/overview)
(`@angular/cdk`) — a dependency-free toolbox of behaviours. `BoardDetailComponent`
imports three standalone directives:

```ts
// frontend/src/app/boards/board-detail.component.ts
import {
  CdkDrag, CdkDropList, CdkDropListGroup,
  moveItemInArray, transferArrayItem
} from '@angular/cdk/drag-drop';

@Component({
  imports: [FormsModule, RouterLink, CdkDropListGroup, CdkDropList, CdkDrag],
  ...
})
```

- `cdkDropListGroup` wraps the row of columns so every list accepts drags from
  the others.
- each column is a `cdkDropList` carrying `[cdkDropListData]="column"` and a
  `(cdkDropListDropped)="drop($event)"` handler.
- each card is a `cdkDrag` carrying `[cdkDragData]="card"`.

The `drop` handler mutates the local arrays for an instant response, then calls
the backend to persist the change:

```ts
drop(event: CdkDragDrop<BoardList>): void {
  const card = event.item.data as Card;
  const targetId = event.container.data.id!;
  if (event.previousContainer === event.container) {
    moveItemInArray(this.cardsFor(targetId), event.previousIndex, event.currentIndex);
  } else {
    transferArrayItem(
      this.cardsFor(event.previousContainer.data.id!), this.cardsFor(targetId),
      event.previousIndex, event.currentIndex);
    card.listId = targetId;
  }
  this.boardService.moveCard(card.id!, targetId, event.currentIndex).subscribe({
    error: () => this.reload()   // snap back to the server's truth on failure
  });
}
```

`moveItemInArray` / `transferArrayItem` are plain array helpers the CDK ships;
they don't know about Angular. Because they mutate the arrays in place, the
component re-emits its `cardsByList` signal (`this.cardsByList.set({ ...this.cardsByList() })`)
so the template re-renders. The CDK's visual feedback (drag preview, drop
placeholder, slide animation) is styled with the `.cdk-drag-*` classes in
`board-detail.component.css`.

## 11. Where this goes next

The concepts above — signals for state, a service per resource, `@if`/`@for`
in templates, one component per route — are the pattern the rest of the app
builds on. Day 3 introduces genuinely new pieces: route guards and an HTTP
interceptor that attaches the Keycloak JWT to every request.

## Further reading

- [angular.dev/essentials](https://angular.dev/essentials) — official concept-by-concept intro
- [angular.dev/guide/signals](https://angular.dev/guide/signals) — signals in depth
- [angular.dev/guide/templates](https://angular.dev/guide/templates) — the `@if`/`@for`/`@switch` control-flow syntax

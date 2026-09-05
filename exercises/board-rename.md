# Exercise — Rename a board inline

**Track:** Frontend (Angular)
**Starting point:** the `day1` tag
**Estimated time:** 45–90 min
**New backend code:** none — `PUT /api/boards/{id}` and `boardService.renameBoard(...)` already exist

---

## Context

After day 1 the board list works: you can create a board, delete one, and peek
at its columns. But a board's name is fixed once created — a typo means delete
and start over.

The backend has supported renaming since day 1 (`PUT /api/boards/{id}`), and
`frontend/src/app/services/board.service.ts` already exposes it:

```ts
renameBoard(id: number, name: string): Observable<Board> {
  return this.http.put<Board>(`${this.baseUrl}/${id}`, { name });
}
```

It is called from nowhere. Your job is to add the UI that uses it.

This is the day-1 warm-up for the day-2 card editor (`exercises/card-description.md`):
same shape — click, edit in place, Save/Cancel — but one field, one component,
and no drag-and-drop.

## Goal

Each board row gets a **Rename** button. Clicking it swaps the board name for a
text input pre-filled with the current name, plus **Save** and **Cancel**.

- **Save** calls `renameBoard(...)` and updates the row in place — no full
  reload. The list stays sorted by name.
- **Save** is disabled while the input is blank.
- **Cancel** closes the editor and keeps the old name.
- Only one board is editable at a time.
- Create, delete, and the column peek keep working.

## How to approach this

- **Minimal hints** — work from the **Goal** and **Acceptance criteria**. The
  day-2 card editor is the same pattern if you want a reference.
- **Guided** — follow the numbered **Suggested steps** below.

## Getting started

```bash
git checkout day1
git switch -c exercise/board-rename
docker compose up -d postgres        # the backend needs the DB
cd backend && mvn liberty:dev        # http://localhost:9080, leave it running
# in another terminal:
cd frontend && npm install
npm start                            # http://localhost:4200
npm test                             # keep this green
```

Everything you touch is in `frontend/src/app/boards/`:

| File | What changes |
|---|---|
| `board-list.component.ts` | editor state signal + `startRename` / `cancelRename` / `saveRename` |
| `board-list.component.html` | the Rename button, and the name ↔ input swap |
| `board-list.component.css` | style the rename form (optional) |
| `board-list.component.spec.ts` | **new file** — one test for the save path |

## Why there is no click-collision here

In the day-2 exercise the whole card is clickable, so opening the editor fights
with drag-and-drop and the delete button. Here you add a **dedicated Rename
button**, so a click on it is unambiguous — nothing else to guard against. Keep
it that way: do **not** make the board name itself the trigger (it already
toggles the column peek).

## Suggested steps

1. **Track which board is being edited.** In `BoardListComponent`, add:

   ```ts
   readonly editingBoardId = signal<number | null>(null);
   renameDraft = '';
   ```

   Do not bind the input straight to `board.name` — you want Cancel to be free.

2. **`startRename(board: Board)`** — set `editingBoardId` to `board.id` and copy
   `board.name` into `renameDraft`.

3. **`cancelRename()`** — `editingBoardId.set(null)`.

4. **`saveRename(board: Board)`** — trim `renameDraft`; bail if empty or
   `board.id` is undefined. Then:

   ```ts
   this.boardService.renameBoard(board.id, name).subscribe({
     next: updated => {
       this.boards.update(list =>
         list.map(b => (b.id === board.id ? updated : b))
             .sort((a, b) => a.name.localeCompare(b.name)));
       this.editingBoardId.set(null);
     },
     error: err => this.fail(`Could not rename "${board.name}"`, err)
   });
   ```

   (The existing `createBoard` already shows the `update` + `sort` pattern.)

5. **Template swap.** In `board-list.component.html`, inside `.board-row`, wrap
   the name button:

   ```html
   @if (editingBoardId() === board.id) {
     <form class="rename" (ngSubmit)="saveRename(board)">
       <input name="renameDraft" [(ngModel)]="renameDraft" autocomplete="off" maxlength="120" />
       <button type="submit" [disabled]="!renameDraft.trim()">Save</button>
       <button type="button" (click)="cancelRename()">Cancel</button>
     </form>
   } @else {
     <button class="board-name" type="button" (click)="toggleColumns(board)">
       {{ board.name }}
     </button>
     <button type="button" (click)="startRename(board)">Rename</button>
   }
   ```

   `FormsModule` is already imported by the component.

6. **Test it.** Create `board-list.component.spec.ts`. Copy the TestBed setup
   idiom from `app.component.spec.ts` (`provideHttpClient()`,
   `provideHttpClientTesting()`), then:

   ```ts
   it('PUTs the new name and re-sorts the list', () => {
     const fixture = TestBed.createComponent(BoardListComponent);
     const component = fixture.componentInstance;
     fixture.detectChanges();

     http.expectOne('/api/boards').flush([
       { id: 1, name: 'Zebra' },
       { id: 2, name: 'Apple' }
     ]);

     const board = component.boards().find(b => b.id === 1)!;
     component.startRename(board);
     component.renameDraft = 'Aardvark';
     component.saveRename(board);

     const put = http.expectOne('/api/boards/1');
     expect(put.request.method).toBe('PUT');
     expect(put.request.body).toEqual({ name: 'Aardvark' });
     put.flush({ id: 1, name: 'Aardvark' });

     expect(component.boards().map(b => b.name)).toEqual(['Aardvark', 'Apple']);
     expect(component.editingBoardId()).toBeNull();
   });
   ```

   Remember `afterEach(() => http.verify())`.

## Acceptance criteria

- [ ] Every board row has a Rename button.
- [ ] Clicking it shows an input pre-filled with the current name; the name text
      and the peek toggle are hidden while editing.
- [ ] Save issues exactly one `PUT /api/boards/{id}` and updates the row without
      a reload.
- [ ] After Save the list is still sorted by name.
- [ ] Save is disabled while the input is blank.
- [ ] Cancel closes the editor and keeps the old name.
- [ ] Opening the editor on one board closes any other open editor.
- [ ] Create, delete, and the column peek still work.
- [ ] `npm test` and `npm run build` (`ng build`) both pass.

## Stretch goals

- Close the editor on `Escape`, save on `Enter` (a plain `<form>` submit already
  gives you Enter).
- Focus the input automatically when the editor opens.
- **Add a column.** The backend has `POST /api/boards/{id}/lists`; add a
  `createList(boardId, name)` method to `BoardService` and a small "add a column"
  form under the column peek. This is the same difficulty as the rename and a
  good second rep.

## Reference solution

None committed — `renameBoard(...)` is in the service but wired to nothing on any
branch. The closest worked example is the day-2 card editor on `main` (commit
*"Add card description display and inline editor"*), which is this exact pattern
with a second field. Background: `frontend/ANGULAR_INTRO.md` §6 (signals) and §7
(templates: control flow and `[(ngModel)]`).

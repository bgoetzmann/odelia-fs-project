# Exercise — Show and edit a card's description

**Track:** Frontend (Angular)
**Starting point:** the `day2` tag
**Estimated time:** 2–3 h (≈1 h if you lean on the reference solution)
**New backend code:** none — the REST API already supports everything you need

---

## Context

After day 2 the board works: columns load, cards drag between them, and the move
is persisted with `PATCH /api/cards/{id}/move`. But a card only ever shows its
**title**. The `Card` entity already has a `description` (up to 2000 chars), the
backend returns it in every card payload, and `PUT /api/cards/{id}` already
accepts `{ title, description }` — nothing in the UI reads or writes it.

Your job is to close that gap.

## Goal

1. **Display** — a card shows its `description` under the title when it has one.
2. **Edit** — clicking a card turns it into an inline form with a title input and
   a description textarea, plus **Save** and **Cancel**. Saving calls
   `PUT /api/cards/{id}` and updates the card in place; no full reload.
3. **Create** — the "add a card" form gets a description textarea too, so a card
   can be created with a description in one step (`POST /api/lists/{id}/cards`).

Drag-and-drop, "add a card", and delete must keep working.

## How to approach this

Pick the lane that fits you:

- **Minimal hints** — work from the **Goal** and **Acceptance criteria** alone.
  Read the "Known gotcha" section below before you start (it will save you an
  hour), then design the rest yourself.
- **Guided** — follow the numbered **Suggested steps**. They spell out state,
  method names, and template structure; you are mostly wiring and testing.

Either way, the "Known gotcha" section is required reading and the acceptance
criteria are the same.

## Getting started

```bash
git checkout day2
git switch -c exercise/card-description
cd frontend && npm install
npm start            # http://localhost:4200, backend must be running too
npm test             # keep this green
```

Everything you touch is in `frontend/src/app/`:

| File | What changes |
|---|---|
| `services/board.service.ts` | `updateCard(...)` is ready to call; widen `createCard` to take a description |
| `models/board.ts` | `Card.description?: string` is already declared |
| `boards/board-detail.component.ts` | editor state + `startEdit` / `cancelEdit` / `saveEdit`; the `dragging` guard; a `{ title, description }` draft for the add form |
| `boards/board-detail.component.html` | description line, the inline form, and the description textarea in the add form |
| `boards/board-detail.component.css` | style the description and both forms |
| `boards/board-detail.component.spec.ts` | add tests for the save and create paths |

## Known gotcha — dragging vs. clicking a card

Making the card clickable puts it in conflict with two things that already work.
This is framework plumbing, not an Angular concept to discover — here is the
problem and the fix, use it as-is:

- **The delete `×` is inside the card.** A click on it bubbles to the card and
  would also open the editor. Its handler must call `$event.stopPropagation()`
  (pass `$event` from the template: `(click)="deleteCard(card, $event)"`).

- **`cdkDrag` fires a synthetic `click` when a drag ends.** So the drop that
  moves a card would immediately reopen it in the editor. The fix: a private
  `dragging` flag, set on `(cdkDragStarted)`, and cleared on `(cdkDragEnded)` —
  but cleared *on the next tick*, because the synthetic click lands right after
  `cdkDragEnded`:

  ```ts
  onDragStarted(): void { this.dragging = true; }
  onDragEnded(): void { setTimeout(() => (this.dragging = false)); }
  ```

  `startEdit(card)` then returns early while `this.dragging` is true.

If clicking a card "sometimes" opens the editor after a drag, or the delete
button also opens it, you missed one of these — not something in your editor
logic.

## Suggested steps

1. **Render the description.** In the `@for` over `cardsFor(...)`, add a
   `<p class="card-description">` under `.card-title`, guarded by
   `@if (card.description)`. Wrap title + description in a `.card-body` so the
   delete button stays on the right (`.card` is already a flex row).

2. **Track which card is being edited.** Add
   `readonly editingCardId = signal<number | null>(null)` and a plain
   `editDraft = { title: '', description: '' }` working copy. Do **not** bind the
   form straight to the card object — you want Cancel to be free.

3. **`startEdit(card)`** — return early if the `dragging` flag is set (see
   "Known gotcha"), otherwise set `editingCardId` to `card.id` and copy
   `title` / `description ?? ''` into `editDraft`.

4. **Swap card ↔ form in the template.**
   `@if (editingCardId() === card.id) { <form>…</form> } @else { <article>…</article> }`.
   Bind the input and textarea with `[(ngModel)]` (the component already imports
   `FormsModule`). Give the controls a `name`.

5. **`saveEdit(card)`** — trim the title; bail if it is empty (defensive — the
   button is also disabled, see below). Then call
   `boardService.updateCard(card.id, { title, description: description || undefined })`.
   On success, replace the card inside `cardsByList` (see the existing `mutate`
   helper) and clear `editingCardId`. In the template, disable Save while the
   trimmed title is blank: `[disabled]="!editDraft.title.trim()"`.

6. **`cancelEdit()`** — just `editingCardId.set(null)`.

7. **Wire up the two click guards** from the "Known gotcha" section: pass
   `$event` to `deleteCard` and call `$event.stopPropagation()` there, and add
   the `(cdkDragStarted)` / `(cdkDragEnded)` handlers with the `dragging` flag.

8. **Add form.** Turn each column's draft into a `{ title, description }` object
   (a `draftFor(columnId)` helper that lazily creates it keeps the template
   tidy), add a `<textarea>` bound to `draftFor(column.id!).description`, and pass
   `description || undefined` as a new third argument to `createCard`. Clear both
   fields on success.

9. **Test it.** `board-detail.component.spec.ts` already shows the pattern:
   `fixture.detectChanges()` triggers `ngOnInit`, then you `flush` the board,
   lists and cards requests in order (`afterEach(() => http.verify())` fails the
   test on any request you don't consume). Add two tests:

   - **Save path** — after the load sequence, grab `component.cardsFor(1)[0]`,
     call `component.startEdit(card)`, set `component.editDraft.title` /
     `.description`, call `component.saveEdit(card)`. Then:

     ```ts
     const put = http.expectOne('/api/cards/10');
     expect(put.request.method).toBe('PUT');
     expect(put.request.body).toEqual({ title: 'New title', description: 'Notes' });
     put.flush({ id: 10, listId: 1, title: 'New title', description: 'Notes', position: 0 });

     expect(component.cardsFor(1)[0].title).toBe('New title');
     expect(component.editingCardId()).toBeNull();
     ```

   - **Create path** — after the load sequence, set
     `component.draftFor(1).title` / `.description`, call
     `component.addCard(column)` (pass the column object, e.g.
     `{ id: 1, boardId: 7, name: 'To do', position: 0 }`), then
     `http.expectOne('/api/lists/1/cards')`, assert it is a `POST` whose body
     carries both `title` and `description`, and `flush` a card back.

## Acceptance criteria

- [ ] A card with a description renders it under the title.
- [ ] A card with no description renders no empty element.
- [ ] Clicking a card opens the inline editor pre-filled with its current values.
- [ ] Save issues exactly one `PUT /api/cards/{id}` and the card updates without a reload.
- [ ] The Save button is disabled while the title is blank (and `saveEdit` bails if called anyway).
- [ ] Cancel discards changes and closes the editor.
- [ ] Dragging a card does **not** open the editor; deleting a card does **not** open it.
- [ ] The "add a card" form has a description field; creating with it set stores the description, and both fields reset afterwards.
- [ ] `npm test` and `npm run build` (`ng build`) both pass.

## Stretch goals

- Close the editor on `Escape`, save on `Ctrl/Cmd+Enter`.
- Show a description on the drag preview.
- Render the description as Markdown (watch out for XSS — use a sanitizer).

## Reference solution

A worked implementation is on `main` (commits *"Add card description display and
inline editor"* and *"Add a description field to the add-a-card form"*) and is
described in `frontend/ANGULAR_INTRO.md` §11. Try the exercise before reading it.

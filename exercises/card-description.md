# Exercise — Show and edit a card's description

**Track:** Frontend (Angular)
**Starting point:** the `day2` tag
**Estimated time:** 45–90 min
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

Drag-and-drop, "add a card", and delete must keep working.

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
| `services/board.service.ts` | already has `updateCard(id, { title, description? })` — just call it |
| `models/board.ts` | `Card.description?: string` is already declared |
| `boards/board-detail.component.ts` | editor state + `startEdit` / `cancelEdit` / `saveEdit` |
| `boards/board-detail.component.html` | description line + the inline form |
| `boards/board-detail.component.css` | style the description and the form |
| `boards/board-detail.component.spec.ts` | add a test for the save path |

## Suggested steps

1. **Render the description.** In the `@for` over `cardsFor(...)`, add a
   `<p class="card-description">` under `.card-title`, guarded by
   `@if (card.description)`. Wrap title + description in a `.card-body` so the
   delete button stays on the right (`.card` is already a flex row).

2. **Track which card is being edited.** Add
   `readonly editingCardId = signal<number | null>(null)` and a plain
   `editDraft = { title: '', description: '' }` working copy. Do **not** bind the
   form straight to the card object — you want Cancel to be free.

3. **`startEdit(card)`** — set `editingCardId` to `card.id` and copy
   `title` / `description ?? ''` into `editDraft`.

4. **Swap card ↔ form in the template.**
   `@if (editingCardId() === card.id) { <form>…</form> } @else { <article>…</article> }`.
   Bind the input and textarea with `[(ngModel)]` (the component already imports
   `FormsModule`). Give the controls a `name`.

5. **`saveEdit(card)`** — trim the title, bail if empty, then
   `boardService.updateCard(card.id, { title, description: description || undefined })`.
   On success, replace the card inside `cardsByList` (see the existing `mutate`
   helper) and clear `editingCardId`.

6. **`cancelEdit()`** — just `editingCardId.set(null)`.

7. **Two collisions to handle:**
   - The delete `×` button is inside the now-clickable card. Its handler must
     call `$event.stopPropagation()` so it doesn't also open the editor.
   - `cdkDrag` fires a `click` on the card when a drag ends. Add
     `(cdkDragStarted)` / `(cdkDragEnded)` handlers that raise a `dragging` flag,
     have `startEdit` return early when it's set, and clear it on the next tick
     (`setTimeout(() => this.dragging = false)`).

8. **Test it.** In `board-detail.component.spec.ts`, drive
   `component.startEdit(card)` → set `editDraft` → `component.saveEdit(card)`,
   then assert `http.expectOne('/api/cards/10')` is a `PUT` with the right body,
   flush a response, and check the card was updated and `editingCardId()` is
   `null`.

## Acceptance criteria

- [ ] A card with a description renders it under the title.
- [ ] A card with no description renders no empty element.
- [ ] Clicking a card opens the inline editor pre-filled with its current values.
- [ ] Save issues exactly one `PUT /api/cards/{id}` and the card updates without a reload.
- [ ] Save is disabled / rejected when the title is blank.
- [ ] Cancel discards changes and closes the editor.
- [ ] Dragging a card does **not** open the editor; deleting a card does **not** open it.
- [ ] `npm test` and `npm run build` (`ng build`) both pass.

## Stretch goals

- Close the editor on `Escape`, save on `Ctrl/Cmd+Enter`.
- Show a description on the drag preview.
- Add a description textarea to the "add a card" form (the create endpoint,
  `POST /api/lists/{id}/cards`, already accepts it via `@Valid Card`).
- Render the description as Markdown (watch out for XSS — use a sanitizer).

## Reference solution

A worked implementation is on `main` (commit *"Add card description display and
inline editor"*) and is described in
`frontend/ANGULAR_INTRO.md` §11. Try the exercise before reading it.

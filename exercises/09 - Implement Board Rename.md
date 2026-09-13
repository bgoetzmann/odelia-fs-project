---
title: Exercise 9 — Implement Board Rename
description: Add and test an inline board-renaming interaction using the existing authenticated API.
---

# Implement Board Rename

**Estimated time:** 75–105 minutes  
**Work mode:** Individual implementation, pair review

## Goal

Add an inline rename interaction to the board list. The backend endpoint and
`BoardService.renameBoard(...)` already exist, so the work remains focused on
Angular state, templates, HTTP results, and tests.

## Required Behavior

- Every board row has a **Rename** button.
- Selecting it replaces that board's name with an input containing the current name.
- Only one board can be edited at a time.
- **Save** is disabled for a blank name.
- **Cancel** restores the unchanged display.
- **Save** sends one `PUT /api/boards/{id}` request.
- The returned board replaces the old value without reloading all boards.
- The board list remains sorted by name.
- Existing create, delete, navigation, loading, and error behavior still works.

## 1. Create a Branch

Create your feature branch:

```bash
git switch -c exercise/board-rename
```

## 2. Locate Existing Support

Find:

- The board-list component, template, and stylesheet
- The `Board` interface
- `BoardService.renameBoard(...)`
- The existing pattern used to update and sort the boards signal
- The component's existing error-reporting helper

## 3. Add Editor State

Add state that can represent:

- Which board is being edited
- The draft name
- Whether a rename operation is pending, if you choose to prevent duplicate saves

Do not bind the input directly to `board.name`; Cancel must leave the original
board unchanged.

## 4. Add Component Operations

Implement operations equivalent to:

- Start editing a board
- Cancel editing
- Trim and validate the draft
- Call `renameBoard`
- Replace the returned board in the signal
- Sort the resulting list
- Close the editor on success
- Surface an error using the component's existing pattern

<details>
<summary>Hint: the save-and-update pattern</summary>

The trickiest part is threading the HTTP response back into the signal
without a full reload. `createBoard` already shows the shape:

```ts
saveRename(board: Board): void {
  const name = this.renameDraft.trim();
  if (!name || board.id === undefined) {
    return;
  }
  this.boardService.renameBoard(board.id, name).subscribe({
    next: updated => {
      this.boards.update(list =>
        list.map(b => (b.id === board.id ? updated : b))
            .sort((a, b) => a.name.localeCompare(b.name)));
      this.editingBoardId.set(null);
    },
    error: err => this.fail(`Could not rename "${board.name}"`, err)
  });
}
```

</details>

## 5. Add the Template Interaction

Use Angular control flow to display either:

- The existing board name, navigation, Rename, and Delete controls
- Or the input, Save, and Cancel controls for the active board

Use a form submission so Enter can save naturally.

## 6. Add a Focused Test

Create or extend the board-list component test to prove:

1. Initial boards are loaded.
2. Saving sends one `PUT` request to the correct URL.
3. The request body contains the trimmed name.
4. The simulated response replaces the board.
5. The list remains sorted.
6. Editor state closes after success.
7. No unexpected HTTP requests remain.

Use the repository's TestBed pattern with:

```ts
provideRouter(routes)
provideHttpClient()
provideHttpClientTesting()
```

<details>
<summary>Hint: the HttpTestingController test skeleton</summary>

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

Copy the TestBed setup (`provideRouter(routes)`, `provideHttpClient()`,
`provideHttpClientTesting()`) from `app.component.spec.ts`, and remember
`afterEach(() => http.verify())`.

</details>

## Validation

```bash
cd frontend
npm test
npm run build
```

Then verify in the browser:

- Save
- Cancel
- Blank input
- Renaming changes sort order
- Reload preserves the new name
- Create, delete, and navigation still work

## Completion Check

Demonstrate:

- The editor state in the UI
- The outgoing PUT request in the Network panel
- The updated sorted list without a list reload
- The persisted name after browser reload
- The passing focused test

## Stretch Tasks

- Focus the input automatically.
- Cancel on Escape.
- Disable Save while the request is pending.
- Preserve the draft and show an inline message when the request fails.

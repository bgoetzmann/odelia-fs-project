---
title: Exercise 4 — Inspect HTML and CSS
description: Use browser developer tools to connect rendered markup, CSS rules, and Angular source.
---

# Inspect HTML and CSS

**Estimated time:** 30–45 minutes  
**Work mode:** Individual or pairs

## Goal

Connect what the browser renders to HTML elements, computed CSS, and the Angular
template and stylesheet that produced them.

## 1. Inspect the Rendered HTML

1. Open the Kanban application.
2. Open browser developer tools and select **Elements** or **Inspector**.
3. Use the element picker to select a board name or action button.
4. Record:

| Observation                        | Your answer  |
| ---------------------------------- | ------------ |
| Element name                       | button       |
| Text content                       | Refresh      |
| Important attributes               | type, class  |
| Parent element                     | form         |
| One accessibility-related property | Role: button |
See [[Web Accessibility]].

## 2. Inspect the Box Model

In the **Computed** or **Layout** panel, identify:

- Content dimensions
- Padding
- Border
- Margin

Change one value temporarily and describe the visible effect.

## 3. Experiment with CSS

In browser DevTools:

1. Change a background or text color.
2. Change spacing using `padding`, `margin`, or `gap`.
3. Disable one CSS declaration.
4. Reload the page.

Explain why your changes disappeared.

## 4. Find the Angular Source

Search under `frontend/src/app/` for the visible text or CSS class you inspected.

Record:

| Question                                 | Your answer               |
| ---------------------------------------- | ------------------------- |
| Angular template file                    | board-list.component.html |
| Component stylesheet                     | board-list.component.css  |
| Selector that applies the style          | app-board-list            |
| Is the style global or component-scoped? | component-scoped          |

## Completion Check

You are done when you can explain:

- The difference between HTML structure and CSS presentation
- How the browser's DOM relates to an Angular template
- Which CSS declaration controls one visible property
- Why a DevTools change is temporary

>1. HTML vs. CSS — HTML defines the structure/meaning of content (elements, attributes, hierarchy); CSS defines how that structure is presented visually (colors, spacing, layout). The same HTML can look completely different with different CSS.
>2. DOM ↔ Angular template — The DOM the browser renders is the compiled/expanded output of the Angular component's template ( `*.html `), with Angular directives ( `*ngFor` ,  `*ngIf` , bindings) resolved into plain elements, plus Angular's auto-generated attributes (e.g.  `_ngcontent-xxx`  for style scoping). Inspecting the DOM element leads back to a specific component's template file.
>3. CSS declaration → visible property — Using the Computed/Layout panel, they should be able to point to the exact CSS property (e.g.  `padding: 8px` ) responsible for one visible effect (e.g. spacing around a button), and name the source (which file/selector sets it, including specificity/overrides).
>4. Why DevTools changes are temporary — Edits made in DevTools only modify the in-memory rendered DOM/CSSOM in the browser tab; they never touch the actual source files ( `.html `/ `.css` / `.ts` ) on disk, so a page reload re-fetches/re-renders the original Angular-compiled output and discards the change.
>   
>   A good response is a short explanation covering all four, ideally referencing the specific element/file they inspected during the exercise (e.g., "I inspected the board name  `<h2>`  in  `board.component.html` ; its  `margin: 0`  comes from  `board.component.css` ; when I disabled it in DevTools the spacing came back on reload because DevTools only edits the live DOM, not the source file").

## Stretch Tasks

- Find a Flexbox container and identify its main and cross axes.
- Use the accessibility inspector to check the accessible name of a button.
- Make one small source-code style change and confirm Angular reloads it.

>Flexbox container: class `columns` in board-detail.component.css (`display: flex`); main axe is `row`, and cross exe `column`.
>"Sign out" button has "Sign out" accessibility name (see Accessibility pane for the button)

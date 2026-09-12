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

| Observation | Your answer |
|---|---|
| Element name | |
| Text content | |
| Important attributes | |
| Parent element | |
| One accessibility-related property | |

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

| Question | Your answer |
|---|---|
| Angular template file | |
| Component stylesheet | |
| Selector that applies the style | |
| Is the style global or component-scoped? | |

## Completion Check

You are done when you can explain:

- The difference between HTML structure and CSS presentation
- How the browser's DOM relates to an Angular template
- Which CSS declaration controls one visible property
- Why a DevTools change is temporary

## Stretch Tasks

- Find a Flexbox container and identify its main and cross axes.
- Use the accessibility inspector to check the accessible name of a button.
- Make one small source-code style change and confirm Angular reloads it.

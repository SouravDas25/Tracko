---
inclusion: fileMatch
fileMatchPattern: "frontend/**/*.dart"
---

# UI Design Principles — Tracko Frontend

## Core Philosophy
Compact, data-dense layouts. Maximize visible content per screen. Avoid decorative chrome that wastes vertical space.

## List Items & Rows
- Use flat rows with thin bottom dividers (`dividerColor.withOpacity(0.08)`, 0.5px) instead of cards.
- No card backgrounds, rounded corners, box shadows, or elevation on repeating list items.
- Row padding: `horizontal: 12, vertical: 8–10`.
- Use `Material` + `InkWell` for tap feedback (ripple), not `GestureDetector`.

## Avatars
- Size: 32×32 (not 48).
- Plain circle with `primaryColor` background, no box shadow.
- Initials font: 13px bold white.

## Typography
- List item title: 14–15px, `FontWeight.w600` or `w700`.
- Subtitle / secondary text: 10–11.5px, `hintColor`.
- For displaying currency amounts, use the `AmountText` widget (`lib/component/amount_text.dart`). It renders the currency symbol 3px larger than the numeric value, both in the specified color. Pass `amount`, `color`, and optionally `fontSize` (default 14), `currencyCode`, and `textAlign`.
- Small inline amounts in labels (e.g., "₹0 left") can stay as plain `Text` with `CommonUtil.toCurrency`.

## Summary / Overview Cards
- Keep the card style (gradient, rounded corners, shadow) only for top-level summary widgets (e.g., daily view balance header).
- Use compact padding: `horizontal: 16, vertical: 12`.
- Border radius: 16 max.
- Main number: 24px max (not 32).
- Labels: 11–13px. Sub-values: 13–14px.
- Collapse 2×2 grids into a single row of 4 when possible.

## Table Views (Monthly / Yearly Summaries)
- Use a proper table layout: header row with column labels, then flat data rows.
- Header row gets a `cardColor` background to visually separate it.
- Alternating row tints: even rows transparent, odd rows `cardColor.withOpacity(0.4)`.
- All columns use `Expanded(flex: 3)` for consistent alignment.
- Values right-aligned, labels/names left-aligned.
- Column labels appear once in the header — never repeat per row.

## Date Group Headers (Transaction Lists)
- Padding: `fromLTRB(12, 12, 12, 4)`.
- Font: 11.5px bold, `hintColor`, `letterSpacing: 1.0`, uppercase.

## Progress Bars (Budget Page)
- Stack budget and spent bars into a single 5–6px bar (budget as faint background, spent as foreground overlay).
- Show Budget and Spent amounts inline on a single row above the bar.

## Dark Mode Considerations
- Scaffold background: `#121212`, card color: `#1E1E1E` — these are very close.
- Never rely on box shadow alone for separation in dark mode.
- Use visible borders (`dividerColor.withOpacity(0.15)`, 0.5px) on any card that needs to stand out.
- For table rows, thin bottom dividers are sufficient.

## Spacing Guidelines
- Between list items: 0 (divider handles separation).
- Inside summary cards: 4–10px between sections (not 24).
- Icon sizes: 10–12px for inline icons, 20–22px for navigation icons.
- Gap between avatar and text: 10px.

## What to Avoid
- Cards for repeating list items (transactions, accounts, budget categories).
- Large outer padding/margins on list items (no 16px horizontal + 6px vertical wrappers).
- Oversized fonts (32px+ numbers, 18px+ titles in lists).
- Separate rows for information that fits on one line.
- Decorative pill/badge containers for simple text — use plain styled text instead.

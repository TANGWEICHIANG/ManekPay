---
name: ManekPay
description: A multi-currency neobank rendered as a counting frame — currencies as rods, balances as discrete beads.
colors:
  navy-frame: "rgb(11 15 26)"
  cyan-bead-light: "rgb(0 229 255)"
  primary: "rgb(8 145 178 / <alpha-value>)"
  background: "rgb(247 248 252 / <alpha-value>)"
  surface: "rgb(255 255 255 / <alpha-value>)"
  surface-hover: "rgb(238 240 247 / <alpha-value>)"
  foreground: "rgb(18 23 43 / <alpha-value>)"
  muted: "rgb(91 97 120 / <alpha-value>)"
  border: "rgb(219 223 235 / <alpha-value>)"
  success: "rgb(5 150 105 / <alpha-value>)"
  warning: "rgb(217 119 6 / <alpha-value>)"
  danger: "rgb(225 29 72 / <alpha-value>)"
  stamp-ink: "rgb(191 63 42 / <alpha-value>)"
  module-ledger: "rgb(37 99 235 / <alpha-value>)"
  module-fx: "rgb(5 150 105 / <alpha-value>)"
  module-vaults: "rgb(217 119 6 / <alpha-value>)"
  module-risk: "rgb(225 29 72 / <alpha-value>)"
  module-wealth: "rgb(124 58 237 / <alpha-value>)"
  brand-start: "rgb(11 15 26 / <alpha-value>)"
  brand-end: "rgb(19 26 44 / <alpha-value>)"
typography:
  body:
    fontFamily: "Rethink Sans, ui-sans-serif, system-ui, sans-serif"
    fontWeight: 400
  headline:
    fontFamily: "Rethink Sans, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.875rem"
    fontWeight: 700
  title:
    fontFamily: "Rethink Sans, ui-sans-serif, system-ui, sans-serif"
    fontSize: "1.5rem"
    fontWeight: 600
  numeral:
    fontFamily: "IBM Plex Mono, ui-monospace, SFMono-Regular, monospace"
    fontWeight: 500
    fontFeature: "tabular-nums"
rounded:
  sm: "2px"
  md: "3px"
  lg: "5px"
spacing:
  hairline-gap: "1px"
  sm: "0.5rem"
  md: "1rem"
  lg: "1.5rem"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.background}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
  button-secondary:
    backgroundColor: "transparent"
    textColor: "{colors.foreground}"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
  wallet-rod:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.foreground}"
    rounded: "{rounded.md}"
    padding: "1rem"
  badge-approved:
    backgroundColor: "{colors.stamp-ink}"
    textColor: "{colors.stamp-ink}"
    rounded: "{rounded.sm}"
    padding: "0.25rem 0.625rem"
---

# Design System: ManekPay

## Overview

**Creative North Star: "The Counting Frame"**

ManekPay's shipped visual system reads as a suanpan (counting frame): currencies are rods, balances are discrete beads, and the whole product refuses the soft gradient card-grid every neobank ships. The frame is a navy-and-cyan instrument that displays exact figures in tabular mono numerals rather than gesturing at wealth with big rounded panels — the interface wants to be trusted like a precise tool, not liked like a toy.

This is a flat, rectilinear, hairline-bordered system. There is no glassmorphism, no soft blur shadow, no card floating above a gradient backdrop. Depth is conveyed by a single 1px border ring (`shadow-card`), not by elevation. The one deliberately warm, non-navy/cyan mark is the stamp-ink vermillion used exclusively for the KYC-approved state — a customs-clearance stamp, not a routine status pill, and never repurposed for a money-negative state.

The system's discipline is legibility over ornament: every numeral renders in IBM Plex Mono for tabular alignment, every wallet balance sits beside a real bead-fill fraction (never a decorative one), and the five functional modules (Ledger, FX, Vaults, Risk, Wealth) each carry one fixed accent hue used consistently across sidebar nav, page icon, and page-local data marks — a way to keep five concurrent currency/module contexts legible without loud theming.

**Key Characteristics:**
- Rectilinear, tight-radius geometry (2–5px) — rods and frames have definite edges, not soft plastic corners.
- Hairline 1px border elevation only; no blurred/soft box-shadow ever used for depth.
- IBM Plex Mono strictly scoped to numerals, currency codes, and reference numbers — never prose, headers, or nav labels.
- Discrete bead-fill (`BeadRow`) as the system's one custom data mark, used only where a real fraction-of-N exists.
- Per-module fixed accent color (ledger blue, fx green, vaults amber, risk rose, wealth violet) carried consistently from sidebar to page icon to page-local marks.

## Colors

A navy-and-cyan instrument palette (extending the existing ManekPay wordmark identity) with a near-white/near-black operating surface, plus five fixed per-module accents and one reserved vermillion "stamp" color.

### Primary
- **Bead-Light Cyan** (`rgb(8 145 178)` light / `rgb(0 229 255)` dark — token `primary`): the frame's live accent — primary buttons, focus rings, active nav state, the "Pay" of the wordmark, wallet bead-fill default color. Rarer and more saturated in dark mode where it becomes the literal wordmark cyan (`#00E5FF`).

### Secondary
- **Navy Frame** (`rgb(11 15 26)` — tokens `brand-start`/`background` dark mode): the anchor surface. Used as the fixed (non-color-scheme-flipped) brand panel on the auth split-screen and as the dark-mode page background — always reads as "ManekPay navy" regardless of light/dark toggle when used as the brand panel.

### Tertiary
- **Stamp-Ink Vermillion** (`rgb(191 63 42)` light / `rgb(232 121 98)` dark — token `stamp`): reserved for the single mark of verification authority in the system — the KYC APPROVED badge's stamp icon and fill. Never used for a money-negative or error state; danger/warning own those independently.

### Neutral
- **Background** (`rgb(247 248 252)` light / `rgb(11 15 26)` dark): page canvas.
- **Surface** (`rgb(255 255 255)` light / `rgb(19 26 44)` dark): cards, wallet cells, sidebar, inputs.
- **Surface Hover** (`rgb(238 240 247)` light / `rgb(27 36 64)` dark): hover state for nav items and function-key rows.
- **Foreground** (`rgb(18 23 43)` light / `rgb(231 234 243)` dark): primary text.
- **Muted** (`rgb(91 97 120)` light / `rgb(148 156 181)` dark): secondary text, currency-code labels, timestamps.
- **Border** (`rgb(219 223 235)` light / `rgb(40 50 79)` dark): the system's one structural line — wallet-rod grid dividers, card hairlines, sidebar rule.

### Module Accents (fixed, not theme-flipped in role)
- **Ledger Blue** (`rgb(37 99 235)` / dark `rgb(96 165 250)`), **FX Green** (`rgb(5 150 105)` / dark `rgb(52 211 153)`), **Vaults Amber** (`rgb(217 119 6)` / dark `rgb(251 191 36)`), **Risk Rose** (`rgb(225 29 72)` / dark `rgb(251 113 133)`), **Wealth Violet** (`rgb(124 58 237)` / dark `rgb(167 139 250)`): each module owns one hue, applied identically to its sidebar active-state (background tint + text + 2px left border), its page icon chip, and any page-local data mark (e.g., ledger's wallet bead-fill uses `bg-ledger` instead of the default primary cyan).

### Named Rules
**The One Stamp Rule.** Stamp-ink vermillion marks exactly one state system-wide — KYC APPROVED. It is not a general "success" or "verified" color; success/positive states elsewhere use `success` green.

**The Module Accent Consistency Rule.** A module's accent color, once assigned (Sidebar `NAV_ITEMS`), must be reused for that module's `PageHeader` icon chip and any page-local data mark on that module's page. Accents are not decorative — they are the wayfinding system across five concurrent currency/module contexts.

## Typography

**Body/Display Font:** Rethink Sans (with `ui-sans-serif, system-ui, sans-serif` fallback)
**Numeral Font:** IBM Plex Mono (with `ui-monospace, SFMono-Regular, monospace` fallback)

**Character:** A single humanist grotesque (Rethink Sans, weights 400–800) carries all prose, labels, and headings — confident and plain, not a display face doing double duty as a "banking" costume. IBM Plex Mono is scoped narrowly and consistently to every numeral: wallet balances, vault/goal amounts, transfer amounts, currency codes (`MYR`), account/reference identifiers, and timestamps in mono-flanked contexts — never to prose, section headers, or nav labels.

### Hierarchy
- **Headline** (700, `text-3xl`/1.875rem): page-top welcome/greeting text (Dashboard's "Welcome back, {name}").
- **Title** (600–700, `text-2xl`/1.5rem): `PageHeader` titles ("Ledger", "Vaults", "FX").
- **Subtitle** (500, `text-lg`/1.125rem): section headers within a page ("Wallets", "Goals", "Balances").
- **Body** (400–500, `text-sm`/0.875rem–`text-base`): labels, descriptions, form copy.
- **Numeral** (mono 500, `text-xl`–`text-3xl`, `tabular-nums`): wallet/vault/goal balances and amounts — the only place mono appears at display size.
- **Data Label** (mono 400–500, `text-xs`, uppercase, `tracking-wider`): currency codes beside a balance (e.g. "MYR").

### Named Rules
**The Every-Numeral Rule.** IBM Plex Mono renders every numeral, currency code, and reference/account identifier in the product — and nothing else. A prose sentence, a section header, or a nav label never switches face mid-string; only the digits/codes within do.

## Layout

Wallet and asset data render in a rod-grid: a bordered container with `gap-px` and a `bg-border` backdrop, producing a hairline grid of cells (`grid-cols-2` mobile up to `grid-cols-5` desktop) — one cell per currency/asset, never a card-per-item layout. This pattern repeats identically on Dashboard, Ledger, and Wealth for wallet/asset balances.

Dashboard's module quick-links render as a single flat row of function keys (`flex flex-col` mobile / `flex-row` desktop, divided by hairline dividers, one shared bordered container) — deliberately not a card grid; each module is a full-bleed row/column segment with an icon, label, and trailing arrow, joined edge-to-edge like frame rods, not floating as separate tiles.

Sidebar is a collapsible left rail (`w-60` open / `w-20` collapsed, `duration-base ease-brand` transition) fixed to viewport height, bg-surface, right hairline border.

Forms and lists stack in a single-column `flex flex-col gap-4`–`gap-6` rhythm inside `Card` containers; two-field rows (currency pairs, sweep amount/frequency) use `grid-cols-2 gap-4`.

## Elevation & Depth

Flat by design. The only elevation token is a 1px solid ring (`shadow-card: 0 0 0 1px rgb(var(--color-border))`) — a hairline outline, not a blurred drop shadow. A counting frame's rods sit flush against the frame; they don't float above it. The single exception is `shadow-glow`, a ring-plus-soft-glow applied only on primary/danger button hover, signaling interactivity rather than resting elevation.

### Shadow Vocabulary
- **card** (`box-shadow: 0 0 0 1px rgb(var(--color-border))`): resting elevation for every `Card`, wallet cell, and bordered container. No blur, no offset.
- **glow** (`box-shadow: 0 0 0 1px rgb(var(--color-primary) / 0.5), 0 0 16px -6px rgb(var(--color-primary) / 0.6)`): hover-only state on primary/danger buttons.

### Named Rules
**The Flush Rod Rule.** Surfaces sit at the same visual plane as their frame at rest. Depth is a hairline border, never a blurred shadow; any glow appears only as a hover response, never as ambient decoration.

## Shapes

Tight rectilinear radii throughout: `sm` (2px) on badges and small chips, `md` (3px) on buttons, inputs, wallet cells, and page-icon chips, `lg` (5px) on `Card` containers. Nothing in the shipped system exceeds 5px — corners read as machined edges, not soft plastic. Borders are always 1px hairlines (`border-border`), and the wallet/asset rod-grid uses `gap-px` + `bg-border` to fake a shared hairline grid rather than individually bordering each cell.

## Components

### Buttons
- **Shape:** 3px radius (`rounded-md`), `px-4 py-2`.
- **Primary:** `bg-primary` / `text-background`, hover triggers `shadow-glow`; active state scales to 0.98.
- **Secondary:** transparent background, `border-border`, hover shifts border toward `primary/40` and fills `surface-hover`.
- **Danger:** same treatment as primary with `bg-danger`.

### Cards / Containers
- **Corner Style:** 5px radius (`rounded-lg`).
- **Background:** `surface`.
- **Shadow Strategy:** hairline `shadow-card` only (see Elevation & Depth).
- **Border:** 1px `border-border`.
- **Internal Padding:** `p-6`.

### Inputs / Fields
- **Style:** `surface` background, 1px `border-border`, 3px radius, label always above field.
- **Focus:** browser-default `:focus-visible` outline in `primary` (2px, 2px offset) — no custom glow ring on inputs.
- **Error:** border switches to `danger`, an inline `text-danger` message renders below, tied via `aria-describedby`.

### Navigation (Sidebar)
- Collapsible rail, `NavLink` items with a 2px left border (`border-l-2`) that is transparent at rest and fills with the active module's fixed accent color plus a 10%-opacity background tint and matching text color when active. This is a deliberate per-module color-coding convention (confirmed on review), not a generic AI "side-tab" pattern — the accent is meaningful wayfinding across five concurrent currency/module contexts, not a stylistic default.
- Hover (inactive items): `surface-hover` background, foreground text darkens from `muted`.
- Mobile/collapsed state hides labels, keeps icon + `title` tooltip attribute.

### Wallet Rod-Grid (Signature Component)
The system's primary data display: a hairline bordered grid (`gap-px`, `bg-border` backdrop) of per-currency cells, each showing a small accent dot, an uppercase mono currency code, a large tabular-mono balance, and a `BeadRow` beneath it representing that wallet's real fraction of the account's total balance. Appears on Dashboard, Ledger, and (as an assets variant) Wealth.

### BeadRow (Signature Component)
The system's one functional discrete-data mark: a horizontal row of small circular "beads" (10 by default), filled left-to-right by `Math.round(fraction * count)`, unfilled beads outlined in `border-border`. Used in exactly two places — wallet balance as a fraction of total balance (Dashboard, Ledger), and vault-goal progress as a fraction of target (Vaults). It is never decorative and never encodes a number that isn't a genuine fraction-of-N; a plain numeral is used everywhere a bead-fill would be fake precision.

### Badge / Stamp (Signature Component)
KYC status renders as an uppercase mono pill (`rounded-sm`, `border`, tinted 10%-opacity fill in the status's color). The one distinguished state is APPROVED: instead of the shared dot-marker other statuses use, it renders a rotated (-12°) stamp icon in stamp-ink vermillion — a single, reviewed exception that reads as an authority mark rather than a routine status chip, not a general icon system.

## Do's and Don'ts

### Do:
- **Do** render every numeral, currency code, and reference/account identifier in IBM Plex Mono with `tabular-nums`; keep prose, headers, and nav labels in Rethink Sans.
- **Do** reuse a module's fixed accent color (ledger/fx/vaults/risk/wealth) consistently across its sidebar nav state, `PageHeader` icon chip, and any page-local data mark.
- **Do** use `BeadRow` only where a genuine fraction-of-N exists (wallet share of total, goal progress toward target); render every other quantity as a plain mono numeral.
- **Do** keep elevation to the 1px `shadow-card` hairline at rest; reserve `shadow-glow` for hover-only interactive feedback.
- **Do** keep corner radii at or below 5px (`sm` 2px / `md` 3px / `lg` 5px) across all components.

### Don't:
- **Don't** apply IBM Plex Mono to prose, section headers, or nav labels — it is scoped strictly to numerals and short data-identifiers, corrected during finish review after an earlier draft over-applied it as decorative "technical costume."
- **Don't** use a blurred/soft `box-shadow` for resting elevation anywhere; this system conveys depth with hairline borders only, not floating cards.
- **Don't** introduce kicker/eyebrow labels above headings — none exist in the shipped system.
- **Don't** repurpose stamp-ink vermillion for any state other than KYC APPROVED; it is not a general "success" or "verified" color.
- **Don't** render Dashboard's module quick-links as a card grid — the shipped pattern is a single flat function-key row with hairline dividers, not separated tiles.

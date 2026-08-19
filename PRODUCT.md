# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Users

Primary users are individual retail banking customers of a multi-currency neobank: people who hold balances across five currencies (MYR, SGD, USD, EUR, GBP), send cross-border P2P transfers, convert currencies at live rates, save toward goals, monitor their own account's risk/restriction status, and — for a subset — invest in fractional shares.

This project is also built as an enterprise-grade portfolio piece reviewed by engineers/recruiters, but per explicit direction the interface itself is designed for the primary banking users as a credible production app would be, not visually optimized to impress reviewers.

## Product Purpose

ManekPay is a fintech super-app: multi-currency wallets, cross-border transfers, real-time FX conversion with rate locks, behavioral savings (goal vaults with recurring sweeps and spare-change round-ups), fraud/risk monitoring with account restrictions, and fractional stock investing (with Shariah-compliance filtering) — built on double-entry bookkeeping precision. Modeled on real neobanks like Revolut.

## Positioning

Combines Revolut-style multi-currency banking mechanics with a genuine visual identity rooted in Melaka's historical role as a Straits of Malacca trade hub — a neighboring generic fintech app could copy the feature set but not a considered regional-trade-history identity.

## Operating Context

Users manage five currency wallets side by side, send P2P transfers by account number/NRIC/mobile proxy, watch live FX rates and can lock a conversion rate for 15 seconds, set savings goals with recurring or round-up sweeps, see their own fraud/risk flags and any active account restriction, and trade fractional shares. Backed by six independent Spring Boot microservices behind a single gateway; the frontend is a single React SPA covering all of it (desktop and mobile web both need to work — no native apps).

## Capabilities and Constraints

- Five-currency wallets with real double-entry ledger precision (`BigDecimal`, never floats).
- P2P transfers with idempotency guarantees; auto top-up from home-currency wallet on shortfall.
- Live FX rates plus a 15-second rate lock for conversions.
- Goal vaults: recurring sweeps and spare-change round-ups.
- Risk engine: transaction velocity rules, location-anomaly (impossible-travel) detection, and a 24-hour account restriction on outgoing transfers when flagged.
- Fractional share trading with Shariah-compliance tags on the asset catalog.
- JWT-based auth with an e-KYC verification flow (ID + selfie checks).
- Web only — must work well on both desktop and mobile-web viewports.

## Brand Commitments

- Product name: **ManekPay**.
- An existing wordmark (`frontend/public/ManekPay.svg`) sets deep navy (`#0B132B`) as the anchor with white "Manek" + vibrant cyan "Pay" (`#00E5FF`) in Rethink Sans (800 weight) — the current design-token system's navy/cyan pairing already echoes this. Preserve this navy+cyan identity as the foundation; extend it rather than replace it.
- Internal module codenames evoke Melaka/Straits trade history — `selat-fx` (selat = strait), `kupang-vaults` (kupang = a historical regional currency), `beadguard-risk` (bead-trade guard). Per explicit direction, this redesign should let that trade-history theme genuinely inform the visual identity (palette extensions, motifs, imagery, type choices) instead of staying backend-only flavor — expressed with the restraint of a real banking product, not as loud theming.

## Evidence on Hand

No real customer testimonials, press mentions, case studies, or photography exist — this is a project, not a live business. The redesign must not fabricate customer quotes, logos, press coverage, or user counts.

## Product Principles

1. Money precision and trust read through the whole product — no cute gimmicks near balances, numbers, or transfer flows.
2. A genuine, distinctive identity (Melaka/Straits trade history, navy+cyan) over generic neobank-blue sameness — but expressed with the restraint a real bank would use, not as loud theming.
3. Multi-currency and cross-border complexity should feel calm and legible across five wallets and six functional modules, not overwhelming.
4. Ships as a credible production banking app: conventional, trustworthy interaction patterns win over novelty when they conflict.

## Accessibility & Inclusion

No product-specific accessibility requirement beyond standard web accessibility — the existing token system already carries WCAG-consistent contrast, focus-visible states, and `prefers-reduced-motion` support; maintain that baseline.

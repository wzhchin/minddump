## Context

MindDump is a Kotlin/Compose (single-activity) app, Hilt DI, Room index cache over an on-disk filesystem source of truth, Coil for images. The UI already carries partial M3 Expressive *infrastructure* — `ui/theme/Type.kt` (Google Sans Flex, `ROND=100`), `ui/theme/Shape.kt` (`ExpressiveShapes` + `LocalExpressiveShapes`), motion curves + reduced-motion in `Theme.kt` — but the render layer ignores it. The feed is `GroupedMessageBubble`-based (an iMessage metaphor: `surfaceContainerHigh`, flat, 20dp fully-rounded, zero elevation), ported from a chat app. That mismatch is why the UI "doesn't feel modern."

The reference project `/home/chin/repos/Momentum` is the target look. Its modern feel comes from three things working together: (1) `material3 1.5.0-alpha21` giving the full 2025 expressive component set; (2) `materialKolor`'s `DynamicMaterialTheme` (seed color + palette style + AMOLED) rather than the stock `dynamicColor`; (3) `surfaceContainer` Cards + `HorizontalMultiBrowseCarousel` instead of bubbles. MindDump pins `material3 1.3.1`, uses stock dynamic color, and renders bubbles — so it is one toolchain generation and one metaphor behind.

Two prior changes are relevant: the archived `expressive-ui-overhaul` (introduced the unused tokens) and the archived `text-fullscreen-edit` (touched `InputBar`). Neither moved the foundation.

## Goals / Non-Goals

**Goals:**
- Bring the toolchain (`material3`, `compileSdk`/`targetSdk`, Kotlin, KSP, Room3) to the Momentum baseline so the expressive component set is available.
- Adopt `materialKolor` `DynamicMaterialTheme` with persisted theme preferences (seed color, palette style, AMOLED, light/dark/system) and a theme settings UI.
- Replace the chat-bubble metaphor with an expressive Card system: single-entry cards, a group card with a media carousel, and in-card collapsed comments.
- Actually use the existing M3 Expressive infrastructure (typography, shapes, motion) at the render layer.
- Unify the green-XML / purple-Compose color inconsistency under the seed-derived scheme.
- Land Phase 1 (single-entry card) as a self-contained, low-risk improvement; Phase 2 (group carousel + comment collapse) follows.

**Non-Goals:**
- No Android home-screen widget (Glance/`AppWidgetProvider`) — neither project has one; "widget" here means in-app card components.
- No data-model or product-behavior changes. The entry/group/comment semantics are unchanged; only presentation changes.
- No re-architecture of Hilt/Room/repository wiring beyond the Room→Room3 package rename.
- No new entry types or capture flows.
- No on-disk schema preservation logic: the DB is an index cache rebuildable from disk (the `database-rebuild` capability already covers rebuild-from-disk).

## Decisions

### Decision 1 — Full toolchain upgrade to the Momentum baseline (not conservative)
**Choice:** `material3 1.3.1 → 1.5.0-alpha21`, `compileSdk`/`targetSdk 35 → 37`, Kotlin `2.2.0 → 2.3.x`, KSP matching, AGP `9.2.1` (already aligned), Compose BOM/compiler matching Kotlin, all other deps bumped as required.
**Why over the conservative path:** the entire point is the 2025 expressive component set (`MaterialExpressiveTheme`, `HorizontalMultiBrowseCarousel`, `SplitButton`, `LoadingIndicator`, expressive shape defaults). Those are gated behind `1.5.x`; `1.4.x` stable gives a subset but not the carousel/expressive-theme the reference uses. Stopping short recreates the exact failure of `expressive-ui-overhaul` (tokens without components).
**Alternatives considered:** `1.4.x` stable (less alpha risk, but lacks `HorizontalMultiBrowseCarousel`/`MaterialExpressiveTheme` in the form Momentum uses) — rejected because it defeats the goal.

### Decision 2 — Room → Room3
**Choice:** migrate `androidx.room` → `androidx.room3` (~`3.0.0-alpha06`, matching Momentum). Update imports across `EntryEntity`, `EntryDao`, `MindDumpDatabase`, the KSP compiler artifact, and the `room.schemaLocation` KSP arg.
**Why:** Momentum is already on Room3 and the new Kotlin/AGP baseline; staying on Room 2.8.4 risks compiler/KSP incompatibility with Kotlin 2.3. Room3 is the forward path Google is steering toward.
**Migration plan:** Room3 reads the same on-disk files; the SQLite DB is an index cache. After the package rename, run the existing **Rebuild Database from disk** action (the `database-rebuild` capability) to regenerate a clean DB rather than authoring a Room migration. This is explicitly permitted by the project's "filesystem is source of truth" rule.
**Risk:** Room3 is alpha — surface area we use is small (entity + DAO + Flow + FTS). Mitigation: smoke-test CRUD + FTS search + rebuild after migration.
**Alternatives considered:** keep Room 2.8.4 and only bump Kotlin if compatible — rejected as it fights the "no conservatism" decision and likely still needs a Room bump for Kotlin 2.3.

### Decision 3 — `materialKolor` theme engine with DataStore preferences
**Choice:** replace the stock `dynamicColor` branch in `Theme.kt` with `com.materialkolor` `DynamicMaterialTheme`, driven by a DataStore-backed `ThemePreferences` (seed color, palette style enum, AMOLED bool, light/dark/system enum). Defaults preserve the "follow system dynamic color" feel via the system accent seed when the user hasn't picked a custom seed.
**Why over `MaterialExpressiveTheme`-only:** Momentum's signature "flowing" cohesion comes from materialKolor's seed+palette derivation across the whole scheme, including a user-tunable palette style and AMOLED — none of which stock `MaterialExpressiveTheme` provides. This is the single biggest lever on perceived modernity.
**Defaults:** palette style defaults to a neutral TonalSpot/Expressive; AMOLED off; mode follows system; seed defaults to the system accent on Android 12+ (preserving current dynamic-color behavior) and a sensible brand seed below that.

### Decision 4 — Card system replaces the bubble metaphor
**Choice:** delete/rewrite `GroupedMessageBubble`/`BubblePosition`/`BubbleRole`. Introduce:
- **`EntryCard`** — single entry card: `surfaceContainer` container, `ExpressiveShapes.cardLarge` (28dp), light tonal elevation, `outlineVariant` border. Keeps the refined circular type-icon avatar + relative-timestamp header (heterogeneous content needs type color-coding for scanning — Momentum is photo-only so doesn't).
- **`GroupCard`** — `HorizontalMultiBrowseCarousel` of media-bearing members on top, group name + member count + type-stat row below. Faithful port of Momentum's `ProjectListItem` mapping group→project, media members→days.
- **In-card comments** — parent card shows `▸ N 条评论`; expanding reveals timestamped previews inline. Removes nested-bubble chat structure.

**Data grounding:** one `MindDumpEntry` == one file, so a single PHOTO entry has exactly one image — a carousel *inside* a single entry is meaningless. The carousel lives on the **group** (which holds multiple media members). This is the correct mapping.
**Why not keep bubbles:** the bubble metaphor is the thing reading as dated; keeping it while adding tokens (the `expressive-ui-overhaul` approach) demonstrably didn't work.

### Decision 5 — Two-phase delivery
**Phase 1:** `EntryCard` (all entry types) + remove bubbles + wire theme engine + theme settings UI + color unification. This alone is a major visual leap and is low-risk (no interaction-model change).
**Phase 2:** `GroupCard` carousel + in-card comment collapse. These change interaction models (carousel gestures, expand/collapse) and need separate verification. Stopping after Phase 1 is acceptable.

## Risks / Trade-offs

- **[Alpha dependency surface]** `material3 1.5.0-alpha21` and Room3 `3.0.0-alpha06` can change APIs. → Pin exact versions; keep the API surface small (carousel, expressive theme, entity/DAO/Flow); add a smoke test after each phase. Watch for `@ExperimentalMaterial3ExpressiveApi` opt-ins.
- **[Room3 alpha + Kotlin 2.3 compiler mismatch]** Room3 alpha may lag a Kotlin point release. → Resolve by matching the exact Momentum version triple (Kotlin/KSP/Room3) which is known to compile; if it drifts, pick the newest Kotlin 2.3.x that Room3's KSP supports.
- **[Toolchain bump breaks the build]** AGP 9.2.1 + compileSdk 37 + new lint/detekt rules may surface new violations. → Treat "project compiles + `./gradlew detekt ktlintCheck` green" as the phase gate; fix fallout as part of the task.
- **[User familiarity loss]** dropping the chat-bubble feed changes a familiar surface. → Phase ordering means the single-entry card lands first with unchanged tap/long-press behavior; the comment/group changes (the bigger interaction shifts) come later and are verified separately.
- **[Coil 3 / Kotlin 2.0.21 cap]** prior memory notes Coil 3.x is capped by the Kotlin toolchain. Bumping Kotlin to 2.3.x unblocks a newer Coil if needed — confirm the Coil bump lands with the Kotlin bump. (Cross-ref: `coil3-kotlin-version-cap` memory.)
- **[Color churn]** switching from green-XML/purple-Compose to seed-derived recolors everything at once. → Acceptable and intended; verify both locales' strings and that no hardcoded `Color(0x...)` literals survive where a scheme role should be used.

## Migration Plan

1. **Baseline (build-green gate):** bump versions in `libs.versions.toml`; Room→Room3 (imports + KSP); `app/build.gradle.kts` compileSdk/targetSdk 37. Get `./gradlew assembleDebug` green before any UI work.
2. **Theme engine:** add `materialKolor` + DataStore; rewrite `Theme.kt` on `DynamicMaterialTheme`; add `ThemePreferences` store + `ThemeSettings` UI; unify `colors.xml`. App themes from seed on cold start.
3. **Phase 1 — EntryCard:** introduce `EntryCard` + per-type bodies; re-point `EntryList`/`GroupedEntryItem`/`CommentBubble` (temporarily keep comments nested until Phase 2); delete bubble primitives once unused. Verify tap/long-press/multi-select intact. Run detekt/ktlint.
4. **Phase 2 — GroupCard + comments:** add carousel to `GroupCard`; convert comments to in-card collapse. Verify carousel gestures + open actions + collapse/expand. Run detekt/ktlint.
5. **Rollback:** each phase is a set of commits; Phase 1 and Phase 2 are independently revertible. The toolchain/theme commits are the shared base — reverting them reverts both phases, which is acceptable since they carry no data change.

## Open Questions

- Exact Kotlin 2.3.x patch and matching KSP/Room3 patch that compile together — to be resolved at task time by matching Momentum's working triple (Kotlin 2.3.21 / KSP 2.3.9 / Room3 3.0.0-alpha06 per Momentum's `libs.versions.toml`); adjust only if MindDump's AGP 9.2.1 disagrees.
- Whether tapping a carousel media item opens the group or opens that media directly — spec leaves it consistent with the card's open behavior; decide at implementation time to match the existing group-open affordance.

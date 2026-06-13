## Context

MindDump uses Jetpack Compose + Material 3. The current UI has:
- System default font, no custom typography
- Zero haptic feedback
- 0.97f press scale (barely visible)
- Hardcoded 16dp corner radius everywhere
- Flat card layout with type badge overlay and gradient accent bar
- No statistics/analytics screen

The reference app (LastChat) demonstrates M3 Expressive design with Google Sans Flex variable font, 12-pattern haptics, 0.85f scale+no-ripple feedback, systematic shape tokens, and chat-bubble-style asymmetric corners. We adapt these patterns for a note-taking context.

The current entry card layout in `EntryItem.kt` renders each entry type differently (text with gradient bar, photo with overlay, audio with waveform) but uses a uniform 16dp card. The new design unifies the layout pattern: type icon avatar + timestamp header row, then content body below.

The data layer (`EntryDao`) has `count(space)`, `countAll()`, `getAll(space)`, `getByDate(space, date)` queries. The `EntryEntity` has `dateFolder` (YYYY-MM-DD format), `type`, `lastModified` (unix timestamp), and `space` fields — sufficient for all statistics aggregations without schema changes.

## Goals / Non-Goals

**Goals:**
- Transform MindDump's visual identity from "functional" to "premium expressive"
- Unify entry card layout with clear type icon + timestamp header, content below
- Add statistics screen with 4 modules: trend chart, type distribution, key metrics, calendar heatmap
- Make every interaction feel tactile through haptics and scale feedback
- Maintain accessibility (reduce motion support, high contrast)

**Non-Goals:**
- Multiple theme presets (keeping single purple-orange-blue scheme)
- User-configurable haptic settings (always on, respects system vibration)
- Custom font settings UI (hardcoded Google Sans Flex)
- Database schema changes (all stats from existing `EntryEntity` fields)
- Animated transitions between theme variants

## Decisions

### D1: Google Sans Flex from LastChat (not fresh download)
**Choice**: Copy `google_sans_flex.ttf` from LastChat's `res/font/` directory.
**Rationale**: Same file, zero effort, already validated. ~200KB APK increase is acceptable.
**Alternative considered**: Download from Google Fonts — unnecessary extra step.

### D2: ROND=100 (fully rounded) for all text
**Choice**: Apply ROND=100 across all typography scales — display, headline, title, body, label.
**Rationale**: M3 Expressive philosophy. The rounded letterforms are the signature look. Mixing rounded and sharp would be inconsistent.
**Alternative considered**: ROND only for display/headline — feels disjointed.

### D3: 0.92f press scale (not 0.85f)
**Choice**: 0.92f default press scale, with spring(0.6, 400f).
**Rationale**: 0.85f is LastChat's dramatic squish — appropriate for a chat app with playful interactions. MindDump is a utility app; 0.92f is noticeable without being distracting.
**Alternative considered**: 0.85f — too dramatic for a note-taking tool.

### D4: Kill ripples, use scale + haptics
**Choice**: Remove all Material ripple indications. Use `animatePressScale()` + haptics for all touch feedback.
**Rationale**: Ripples fight with scale animations visually. Scale + haptics gives a cleaner, more premium feel. LastChat validates this approach.
**Alternative considered**: Keep ripples + scale — double feedback feels redundant.

### D5: Shape tokens via CompositionLocal
**Choice**: `LocalExpressiveShapes` CompositionLocal providing a data class with named shape tokens (cardLarge=28dp, cardMedium=24dp, buttonPill=50%, buttonRounded=20dp, inputField=20dp, chip=12dp).
**Rationale**: Centralized tokens allow global adjustments. CompositionLocal pattern matches existing `LocalGradientColors` / `LocalAnimationDuration`.
**Alternative considered**: Just use MaterialTheme.shapes override — too limited for asymmetric corners.

### D6: Asymmetric entry card corners
**Choice**: Entry cards use `RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)` — small corner on bottom-left for a "stack" effect.
**Rationale**: Creates visual rhythm in the vertical list. LastChat uses this for message bubbles; adapted for note entries.
**Alternative considered**: Uniform corners — misses the expressive opportunity.

### D7: Entry card layout — icon avatar + timestamp header
**Choice**: Top row: circular avatar with type icon (📝📷🎙️🎬📎) + relative timestamp ("今天 14:32"). Content area below fills card width. No gradient accent bar.
**Rationale**: Cleaner than current overlay/badge approach. Type is immediately identifiable. Matches LastChat's message bubble pattern.
**Alternative considered**: Horizontal layout (icon left, content right) — wastes width for photo entries.

### D8: Statistics — custom Canvas charts (no library)
**Choice**: Draw all charts (trend line, type pie, calendar heatmap) using Compose Canvas API.
**Rationale**: Avoids adding a charting library dependency. Stats charts are simple enough for custom drawing. Keeps APK small.
**Alternative considered**: Vico chart library — overkill for 4 simple charts, adds ~300KB.

### D9: Minimal top bar — no title, icon-only
**Choice**: Remove `CenterAlignedTopAppBar` title entirely. Top bar becomes a transparent row with only 3 icon buttons floating at top-right: Search, Statistics (bar chart icon), Settings. No app name, no space name text.
**Rationale**: Maximizes content area. The current space is indicated by the SpaceSwitchButton in the InputBar already — no need for a title. Matches LastChat's minimal chrome approach.
**Alternative considered**: Keep space title in top bar — redundant with SpaceSwitchButton state.

### D10: Stats navigation — top bar icon
**Choice**: Add a bar chart icon in the top bar (between search and settings). Navigates to `Screen.Statistics` route via `navController.navigate()`.
**Rationale**: Floating icon button, consistent with other top bar actions. No need for bottom navigation in a 2-screen app.
**Alternative considered**: Bottom navigation tab — overkill for 2 destinations.

### D10: Stats data flow — new DAO queries + ViewModel
**Choice**: Add new DAO queries (`getEntriesByDay()`, `getEntriesByType()`, `getStreak()`) as Room `@Query` methods returning `Flow<List<T>>`. New `StatisticsViewModel` with its own UI state.
**Rationale**: Separation of concerns. Main ViewModel doesn't need stats. DAO already has the right fields (`dateFolder`, `type`) for aggregation.
**Alternative considered**: Compute stats in ViewModel from existing `getAll()` — loads all entries into memory, poor performance at scale.

## Risks / Trade-offs

- **[Font licensing]** Google Sans Flex is free for personal/development use but has specific license terms. → Mitigation: This is a personal project; acceptable.
- **[Haptic fatigue]** 12 patterns may feel excessive if overused. → Mitigation: Only wire haptics to meaningful interactions (send, delete, toggle, navigate). Don't add haptics to passive scrolling.
- **[APK size]** Font file adds ~200KB. → Mitigation: Acceptable for the visual payoff. Can be excluded in future build variants if needed.
- **[Stats performance]** Complex DAO aggregation queries on large datasets. → Mitigation: Room returns `Flow`, computations happen on background thread. Add `@Transaction` for complex queries.
- **[Entry card reflow]** Existing EntryItem.kt handles 6 entry types with distinct layouts. Rewriting changes behavior for all types. → Mitigation: Test each type individually during implementation.

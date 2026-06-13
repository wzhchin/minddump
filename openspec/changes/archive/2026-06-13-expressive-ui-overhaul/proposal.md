## Why

MindDump's UI is functional but lacks the tactile, polished feel of modern Android apps. Compared to LastChat (a reference app with M3 Expressive design), MindDump has no custom typography, zero haptic feedback, barely-perceptible press animations, a flat shape system, and plain card layouts. Additionally, users have no way to visualize their capture habits over time. This change closes the expressiveness gap, redesigns entry cards for visual clarity, and adds a statistics page.

## What Changes

- **Typography overhaul**: Replace system default font with Google Sans Flex variable font (ROND=100 rounded letterforms) for M3 Expressive typography
- **Premium haptics system**: Add 12 distinct haptic patterns (Tick, Pop, Thud, Buildup, Success, Error, DragStart, DragEnd, Send, ScrollEdge, Selection, Cancel) wired to all interactive elements
- **Press feedback upgrade**: Increase press scale from 0.97f → 0.92f, remove Material ripples, use scale + haptics for all touch feedback
- **Shape token system**: Replace hardcoded 16dp shapes with systematic tokens (28dp cards, pill buttons, 20dp inputs, 12dp chips) with optical nesting
- **Entry card redesign**: LastChat-style cards — top row with type icon (avatar) + timestamp, content area below. Asymmetric corners (20dp outer, 6dp stack side)
- **Statistics page**: New screen with 4 modules — time trend chart, type distribution chart, key metrics (streak, peak hours, total entries), calendar heatmap. Accessible via top bar icon.

## Capabilities

### New Capabilities
- `expressive-typography`: Google Sans Flex variable font integration with M3 Expressive typography scale (ROND=100 rounded, wider display text, weight ladder)
- `premium-haptics`: 12-pattern haptic feedback system with Vibrator API integration, reduce-motion respect, and per-interaction pattern mapping
- `shape-tokens`: Systematic shape token hierarchy (card, button, input, chip sizes) with CompositionLocal provider, asymmetric corner support, and optical nesting rules
- `entry-card-redesign`: LastChat-style entry cards with type icon avatar, timestamp header, content body, and asymmetric corners per entry type (text, photo, audio, video, file)
- `statistics-page`: New statistics screen with time trend chart, type distribution, key metrics dashboard, and calendar heatmap, accessible via top bar navigation

### Modified Capabilities
<!-- No existing specs to modify -->

## Impact

- **New dependency**: Google Sans Flex font file (~200KB) bundled in `res/font/`
- **Theme layer**: `Theme.kt` typography replaced, new `Type.kt` and `Shape.kt` files added, `ThemeExtensions.kt` extended
- **Animation layer**: `FancyAnimations.kt` press scale default changed, new `noRippleClickable()` modifier
- **Top bar**: Remove app title, keep only 3 floating icon buttons (search, stats, settings) at top-right
- **Entry cards**: `EntryItem.kt` fully rewritten — new layout, asymmetric corners, type icons
- **Top bar**: `MainScreen.kt` removes CenterAlignedTopAppBar title, becomes minimal floating icon row (Search, Statistics, Settings only)
- **Navigation**: `MindDumpNavGraph.kt` adds statistics route
- **New screen**: `StatisticsScreen.kt` with chart components — may need a charting library (Vico/Microchart) or custom Canvas drawing
- **All UI consumers**: `InputBar.kt`, `SpaceSwitchButton.kt`, `SkeletonAndEmptyStates.kt`, and all dialog components updated for shape tokens, haptics, and ripple removal
- **Data layer**: New DAO queries for statistics aggregation (entries per day, per type, streak calculation)
- **No breaking changes**: All changes are additive or visual-only, no database schema changes (Room FTS and EntryEntity stay the same)

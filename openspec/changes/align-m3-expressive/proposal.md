## Why

The UI's M3 Expressive *infrastructure* (Google Sans Flex `ROND=100` typography, `ExpressiveShapes` tokens, motion curves) exists but is never used at the render layer. Entries render as iMessage-style chat bubbles (`GroupedMessageBubble` → `surfaceContainerHigh`, flat, fully-rounded, zero elevation), which reads as dated and inconsistent with the modern Material 3 Expressive direction. The root cause is `material3` pinned at `1.3.1` — the 2025 expressive components (`MaterialExpressiveTheme`, `HorizontalMultiBrowseCarousel`, `SplitButton`, expressive shape defaults) are unavailable. The prior `expressive-ui-overhaul` change only swapped typography/shape tokens on the old base. This change goes to the foundation: upgrade the toolchain to match the Momentum reference project, adopt its `materialKolor` theme engine, and replace the chat-bubble metaphor with an expressive Card system.

## What Changes

- **Toolchain baseline upgrade (BREAKING at the build level)** — `material3` `1.3.1 → 1.5.0-alpha21`, `compileSdk`/`targetSdk` `35 → 37`, Kotlin `2.2.0 → 2.3.x`, KSP matching, all other libraries bumped as the new baseline requires.
- **Room → Room3 migration** — adopt `androidx.room3` (~`3.0.0-alpha06`). Package rename (`androidx.room.*` → `androidx.room3.*`), annotation imports, compiler artifact. The database is an index cache rebuildable from disk, so no on-disk schema preservation is needed beyond confirming the rebuild-from-disk path.
- **Theme engine: `materialKolor` `DynamicMaterialTheme`** — replace the stock `dynamicColor` path with a seed-color + palette-style + AMOLED engine (the core of Momentum's "flowing modern" feel). Theme preferences (seed color, palette style, AMOLED, light/dark/system) persist via DataStore.
- **Theme settings UI** — a settings panel to pick seed color, palette style, and toggle AMOLED/dark mode.
- **Color unification** — `res/values/colors.xml` (green `#1B6B4A`) vs the Compose theme (purple `#8B418F`) are unified under the seed-derived scheme.
- **Entry rendering: chat bubble → expressive Card (BREAKING visual)** — single entries render as `surfaceContainer` Cards with `cardLarge` (28dp) corners, light tonal elevation, and an `outlineVariant` border. The circular type-icon avatar + relative-timestamp header is retained and refined (heterogeneous content needs the type color-coding for scanning).
- **Group card: media Carousel** — group cards gain a `HorizontalMultiBrowseCarousel` previewing the group's media-bearing members (photos/video thumbnails), mirroring Momentum's `ProjectListItem`. One entry == one file, so the Carousel lives on the group (multiple media members), not on a single entry.
- **Comments: nested bubbles → in-card collapsed list** — comments fold into the parent card as an expandable "▸ N 条评论" list, producing a note/card-app feel instead of a chat feel.
- **Removal** — `GroupedMessageBubble`, `BubblePosition`, `BubbleRole` chat-bubble primitives are removed/rewritten in favor of the Card system.

## Capabilities

### New Capabilities
- `expressive-theme`: the `materialKolor` seed-color / palette-style / AMOLED theme engine, the persisted theme preferences, the theme settings UI, and the unified color baseline.
- `entry-card`: how a single entry (text/photo/recording/video/file) renders as an expressive Card, including the type-icon header and per-type content.
- `group-card`: how a group renders as a card with a media Carousel preview, member count, and type-stat row.
- `comment-presentation`: how comments present inside their parent entry's card as a collapsed, expandable list.

### Modified Capabilities
- *(none — the rendering change is expressed via the new capabilities above; no existing spec's product behavior contract changes)*

## Impact

- **Build/dependencies**: `gradle/libs.versions.toml`, `app/build.gradle.kts` — full version bump; new `materialKolor` + `datastore-preferences-core` deps; Room3 artifacts.
- **Data layer**: `data/EntryEntity.kt`, `data/EntryDao.kt`, `data/MindDumpDatabase.kt`, `data/MindDumpRepository.kt` — Room→Room3 package migration; KSP compiler swap. Behavior unchanged; rebuild-from-disk remains the source of truth.
- **Theme**: `ui/Theme.kt` rewritten on `DynamicMaterialTheme`; new `ui/theme/` preference store (DataStore); `res/values/colors.xml` unified.
- **Render layer**: `ui/EntryItem.kt` (EntryList, GroupSummaryCard, GroupedEntryItem, CommentBubble, EntryItem, per-type content), `ui/components/GroupedMessageBubble.kt`, `ui/InputBar.kt` — Card system replaces bubbles.
- **Settings**: `ui/components/SettingsDialog.kt` gains the theme settings entry.
- **Strings**: `res/values/strings.xml` (zh-CN) and `res/values-en/strings.xml` (en) — new strings in both; code/comments stay English.
- **Verification**: `./gradlew detekt ktlintCheck` after implementation.

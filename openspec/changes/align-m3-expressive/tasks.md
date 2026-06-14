# Tasks — align-m3-expressive

> Phased delivery. **Phase 0–2** is the toolchain + theme foundation. **Phase 3** (EntryCard) is the low-risk visual workhorse and a shippable stopping point. **Phase 4** (group carousel + comment collapse) changes interaction models and is verified separately. Gate each phase on a green build + `./gradlew detekt ktlintCheck`.

## 1. Phase 0 — Toolchain baseline (build-green gate)

- [x] 1.1 In `gradle/libs.versions.toml`: bump `material3` `1.3.1 → 1.5.0-alpha21`, `kotlin 2.2.0 → 2.3.21`, `ksp → 2.3.9`, set `compileSdk`/`targetSdk `35 → 37`; bump Compose BOM + any deps the new baseline requires (incl. Coil if the Kotlin 2.3 toolchain unlocks it — recall the Coil/Kotlin cap).
- [x] 1.2 Add `materialKolor = "4.1.1"` and `datastore = "1.2.1"` versions + library aliases (`com.materialkolor:material-kolor`, `androidx.datastore:datastore-preferences-core`).
- [x] 1.3 Migrate Room → Room3: rename `room-runtime`/`room-ktx`/`room-compiler`/`room-testing` to `androidx.room3:room3-*` at `3.0.0-alpha06`; update the `room.schemaLocation` KSP arg path as needed.
- [x] 1.4 In `app/build.gradle.kts`: add the new dependencies, set `compileSdk`/`targetSdk = 37`; confirm AGP `9.2.1` is retained and the compose compiler plugin resolves.
- [x] 1.5 Room→Room3 source migration: update all imports `androidx.room.* → androidx.room3.*` across `data/EntryEntity.kt`, `data/EntryDao.kt`, `data/MindDumpDatabase.kt`, `data/EntryFts`/FTS classes, and any `@Database`/`@Entity`/`@Dao` annotations.
- [x] 1.6 Resolve Kotlin 2.3 / KSP / Room3 / Hilt compiler version agreement; fix any codegen fallout (e.g. KSP args, generated package moves).
- [x] 1.7 Confirm `./gradlew assembleDebug` is green, then run **Rebuild Database from disk** (existing `database-rebuild` capability) to regenerate a clean Room3 DB; verify CRUD + FTS search still work.

## 2. Phase 1 — Theme engine (materialKolor) + preferences

- [x] 2.1 Create a `ThemePreferences` model (seed color Int/Long, palette-style enum, AMOLED bool, light/dark/system enum) and a DataStore-backed `ThemePreferencesRepository` (`di/` Hilt-provided).
- [x] 2.2 Define the palette-style enum (TonalSpot, Vibrant, Expressive, etc.) and the theme-mode enum with zh-CN + en labels.
- [x] 2.3 Rewrite `ui/Theme.kt`: wrap content in `materialKolor` `DynamicMaterialTheme` driven by `ThemePreferences`; default seed to the system accent on Android 12+ when no custom seed is set; thread AMOLED through; preserve the existing motion/shape/typography `CompositionLocalProvider` (Google Sans Flex, `ExpressiveShapes`, motion curves, reduced-motion).
- [x] 2.4 Add a `ThemeSettings` panel (seed color picker, palette style chooser, AMOLED toggle, light/dark/system) reachable from `ui/components/SettingsDialog.kt`; persist changes through the repository.
- [x] 2.5 Unify color baseline: remove/replace the green declarative colors in `res/values/colors.xml` so all surfaces derive from the theme; replace any hardcoded `Color(0x...)` literals in the render layer with `MaterialTheme.colorScheme` roles.
- [x] 2.6 Add zh-CN strings to `res/values/strings.xml` and en strings to `res/values-en/strings.xml` for all theme-settings labels and palette/mode names.
- [x] 2.7 Verify: theme reflects seed/palette/AMOLED live, persists across restart, falls back safely on first launch and on unreadable prefs; both locales render; build + detekt + ktlint green.

## 3. Phase 2 — EntryCard replaces bubbles (single-entry, low risk)

- [ ] 3.1 Introduce `ui/components/EntryCard.kt`: a `Card`-based surface using `ExpressiveShapes.cardLarge` (28dp), `surfaceContainer` container color, light tonal elevation, `outlineVariant` border; `combinedClickable` for tap/long-press with `rememberPremiumHaptics` (Tick/Buildup).
- [ ] 3.2 Port the refined header into `EntryCard`: circular type-icon avatar (color-by-type) + relative timestamp + lock indicator for encrypted entries (outside multi-select).
- [ ] 3.3 Port per-type card bodies (text with expand affordance; photo with large rounded media region + `ZoomableAsyncImage`; recording/file chips via `DocumentChip`; video thumbnail + play overlay).
- [ ] 3.4 Preserve multi-select: selection checkbox/affordance inside the card, stable layout (no jump beyond the affordance).
- [ ] 3.5 Re-point `EntryList`/`GroupedEntryItem` in `ui/EntryItem.kt` to render via `EntryCard`; keep comments nested under the card for now (Phase 3 converts them).
- [ ] 3.6 Remove `ui/components/GroupedMessageBubble.kt` (and `BubblePosition`/`BubbleRole`) once no callers remain; clean up imports across the render layer.
- [ ] 3.7 Verify tap→open, long-press→action menu, multi-select, encrypted lock indicator, and per-type rendering all behave; build + detekt + ktlint green. *(Shippable stopping point.)*

## 4. Phase 3 — GroupCard carousel + in-card comment collapse

- [ ] 4.1 In the group card, add a `HorizontalMultiBrowseCarousel` (`@OptIn(ExperimentalMaterial3Api)`) of the group's media-bearing members (photo/video thumbnails via Coil), sized like Momentum's `ProjectListItem` (large item tiles, item spacing, content padding).
- [ ] 4.2 Omit the carousel region when the group has no media members (no empty media area); keep the existing group name + member count + type-stat row.
- [ ] 4.3 Make tapping the card open the group; make carousel gesture handling coexist with the card's tap/long-press (decide per the open question: group-open vs. open-tapped-media — implement to match the existing group-open affordance).
- [ ] 4.4 Convert comments from nested bubbles to an in-card collapsed list: an expandable `▸ N 条评论` affordance that reveals timestamped content previews inline; tapping a comment opens it.
- [ ] 4.5 Preserve the orphan-comment indicator ("原始文件已删除") inside the card presentation.
- [ ] 4.6 Verify carousel browsing + open actions + comment expand/collapse + orphan indication; build + detekt + ktlint green.

## 5. Phase 4 — Cleanup & verification

- [ ] 5.1 Final pass: ensure no leftover bubble imports/references; confirm `ExpressiveShapes` tokens are actually referenced by the render layer (not just defined).
- [ ] 5.2 Verify both locales (zh-CN default, en) for all new/changed strings; confirm code/comments remain English.
- [ ] 5.3 Run `./gradlew detekt ktlintCheck`; fix all findings.
- [ ] 5.4 Smoke-test full flow: capture text/photo/audio/video/file, multi-select, group open/long-press, comments expand, theme settings change, rebuild-from-disk, FTS search.

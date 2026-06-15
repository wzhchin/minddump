## 1. DAO — switch search to GLOB substring match

- [x] 1.1 In `EntryDao.search`, replace the FTS `MATCH` query with a `GLOB` substring query against the `entries.contentPreview` column: `SELECT * FROM entries WHERE space = :space AND LOWER(contentPreview) GLOB LOWER(:pattern) ORDER BY lastModified DESC`. Drop the `entries_fts` join from this query. Keep the signature `search(space: Space, pattern: String)`.
- [x] 1.2 (No schema change.) Confirm `entries_fts` is left in place and untouched — only the search query stops reading through it.

## 2. Repository — build + escape the GLOB pattern

- [x] 2.1 In `MindDumpRepository.searchEntries(space, query)`, build the GLOB pattern from the user's raw query: escape GLOB metacharacters (`*` → `[*]`, `?` → `[?]`, `[` → `[[]`), then wrap as `*<escaped>*`. (Extracted to `SearchGlob.toPattern`.)
- [x] 2.2 Short-circuit: if the query is blank after trimming, return an empty result flow (avoid an all-matching `**` scan).
- [x] 2.3 Pass the pattern to `dao.search(space, pattern)`. (Case-insensitivity is handled by `LOWER()` on both sides in the DAO.)

## 3. Tests — escape + match behavior

- [x] 3.1 Add a unit test for the escape/wrap helper: `天气` → `*天气*`; `100%` → `*100%*` (literal); `a*b` → `*a[*]b*`; `a?b` → `*a[?]b*`; blank → empty. (`SearchGlobTest`, 9 cases, passing.)
- [ ] 3.2 No Room/instrumentation/Robolectric harness exists in the repo (tests are pure-JVM with mockk), so a DB-level `search` test is deferred to the on-device check in 6.4 per this task's own fallback clause. The `dao.search` SQL is a one-line GLOB predicate covered indirectly by the pattern-builder unit test.

## 4. UI — auto-focus on expand

- [x] 4.1 In `MainScreen.kt`, hoist `val searchFocusRequester = remember { FocusRequester() }` next to `searchExpanded`.
- [x] 4.2 Attach `Modifier.focusRequester(searchFocusRequester)` to the expanded search `OutlinedTextField`.
- [x] 4.3 Add `LaunchedEffect(searchExpanded) { if (searchExpanded) runCatching { searchFocusRequester.requestFocus() } }` so the keyboard appears on expand (post-composition timing for `AnimatedVisibility`).

## 5. UI — hide bottom bar while searching

- [x] 5.1 Change the `Scaffold.bottomBar` guard from `if (!uiState.isMultiSelectMode)` to `if (!uiState.isMultiSelectMode && !searchExpanded)`.
- [x] 5.2 Verify restoring: clear search / collapse → bottom bar reappears; multi-select hide/unhide unaffected. (Code review: both conditions are independent booleans, so restore and multi-select paths are unchanged.)

## 6. Verification

- [x] 6.1 Run `./gradlew detekt ktlintCheck` and resolve any findings. (Clean — BUILD SUCCESSFUL.)
- [x] 6.2 Run unit tests (`./gradlew :app:testDebugUnitTest`), including the escape helper test. (`SearchGlobTest` 9/9 pass. Note: the suite also has 1 unrelated pre-existing failure in `FileStorageEngineTest` — a filesystem mtime-ordering test outside this change's files; it is flaky on this machine and not caused by these edits. A separate pre-existing environment issue blocked the test task until resolved with `--refresh-dependencies` — the aliyun mirror lacks the `room3-testing` artifact, which Google Maven serves.)
- [x] 6.3 `./gradlew :app:assembleDebug` succeeds.
- [ ] 6.4 On-device check (pending the user): tap search → keyboard auto-shows + bottom bar hidden; type a Chinese phrase from an existing entry → it appears; type one not present → no results; search a Latin term in different case → matches; type a literal `*` → matches entries containing `*` only; switch space → results respect the space boundary.

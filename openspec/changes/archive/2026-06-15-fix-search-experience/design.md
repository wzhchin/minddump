## Context

Search today has three defects:

1. **No focus on expand.** `MainScreen.kt` flips `searchExpanded = true` when the search icon is tapped, which animates the `OutlinedTextField` into the top app bar — but no `FocusRequester.requestFocus()` is ever called. The field appears without the keyboard, so the user taps twice. The repo already has the right pattern in `FullscreenEditScreen.kt` (`FocusRequester` + `LaunchedEffect` calling `requestFocus()`).
2. **Bottom bar lingers.** `Scaffold.bottomBar` renders the `InputBar` whenever `!uiState.isMultiSelectMode`. It ignores `searchExpanded`, so the bar competes for space while the user is searching.
3. **CJK search is broken.** The search path is `ViewModel.searchEntries` → `Dao.search` → `SELECT … WHERE entries_fts MATCH :query`. The FTS table is declared with `@Fts4(contentEntity = EntryEntity::class)` and no tokenizer option, so SQLite falls back to the **`simple`** tokenizer. `simple` only recognizes ASCII alphanumerics as word characters; every CJK code point is a separator, so a Chinese phrase like `天气` almost never matches.

## Goals / Non-Goals

**Goals:**
- Keyboard appears automatically when the search field expands.
- Bottom input bar is hidden during search and restored when search closes.
- Typing a Chinese phrase returns entries containing that phrase (substring match), with no schema change and no new dependencies.

**Non-Goals:**
- Tokenization/segmentation of any kind (GLOB makes this unnecessary — see Decision 3).
- Synonym, pinyin, or fuzzy search.
- Replacing the SQLite provider or adding an `icu`/`jieba` tokenizer.
- Searching inside encrypted entries' decrypted bodies (search operates on the `contentPreview` plaintext stored for text entries).
- Removing or restructuring the `entries_fts` table (it is simply no longer read by search; left untouched to keep the change minimal).

## Decisions

### 1. Auto-focus via FocusRequester + LaunchedEffect

**Decision**: Hoist a `FocusRequester` next to `searchExpanded` in `MainScreen`. When `searchExpanded` transitions to `true`, a `LaunchedEffect(searchExpanded)` calls `focusRequester.requestFocus()`. Assign the requester to the expanded `OutlinedTextField` via `Modifier.focusRequester(focusRequester)`.

**Rationale**: This is the established pattern in `FullscreenEditScreen.kt:52,129`. `requestFocus()` must run after the field is composed (hence the `LaunchedEffect`, which fires after composition), and gating on `searchExpanded` makes it fire exactly once per expand.

**Alternative considered**: `Modifier.focusRequester().focusable()` without the effect — rejected; the field is created lazily inside `AnimatedVisibility`, so the requester needs the effect's post-composition timing.

### 2. Hide the bottom bar while searching

**Decision**: Extend the `bottomBar` guard from `if (!uiState.isMultiSelectMode)` to `if (!uiState.isMultiSelectMode && !searchExpanded)`. When search closes (clear button or collapse), the condition re-evaluates and the bar re-renders.

**Rationale**: `searchExpanded` is already local `MainScreen` state and already drives the top-bar animation and container color. Reusing it for the bottom bar is one line and keeps the two affordances perfectly symmetric. Hiding the whole bar avoids the IME/content-overlap problem and matches how multi-select already hides it.

**Alternative considered**: Keep the bar but disable its inputs — rejected; it still occupies height and confuses the user.

### 3. Replace FTS MATCH with GLOB substring matching

**Decision**: Change `EntryDao.search` from an FTS `MATCH` to a `GLOB` substring predicate against the raw `entries.contentPreview` column. The query becomes, in effect:

```sql
SELECT * FROM entries
WHERE space = :space AND LOWER(contentPreview) GLOB :pattern
ORDER BY lastModified DESC
```

The repository builds `pattern` by escaping GLOB wildcards in the user's query (`*` → `[*]`, `?` → `[?]`, `[` → `[[]`) and wrapping it in `*…*`, so `天气` becomes the pattern `*天气*` — a substring test on the original, contiguous text. Chinese needs no tokenization here: the stored `contentPreview` already contains the real characters in order, and GLOB compares them as a literal run.

**Rationale**: This attacks the actual root cause. The bug was never that the text was segmented wrong — it was that FTS's `simple` tokenizer fragmented CJK *before* it could be matched. GLOB skips tokenization entirely and compares the raw bytes, so CJK works by construction. It also removes the entire tokenizer concept (no bigram preprocessing, no index reformatting, no read/write asymmetry to maintain).

**Alternative considered**:
- *Bigram/n-gram preprocessing of both index and query* (the previously-proposed approach): correct, but doubles the index size, requires a `SearchTokenizer` module applied at every write site + the read site, and forces a schema bump + rebuild. GLOB achieves the same correctness with one DAO query edit and zero write-path changes. Rejected in favor of GLOB.
- *FTS5 + `unicode61`*: still does not segment CJK; same problem.
- *Bundled ICU tokenizer (`requery/sqlite-android`)*: best-ranked-search quality but adds a native lib and forces swapping Room's open helper. Overkill for substring search. Rejected.
- *`LIKE '%...%'`*: functionally equivalent to `GLOB '*…*'` and case-insensitive by default for ASCII; also viable. GLOB is chosen because its wildcard escaping is simpler and more predictable than `LIKE`'s (`\`-based with `ESCAPE`), and because the user explicitly asked for GLOB.

### 4. Case-insensitivity and escaping, done at the repository boundary

**Decision**: The DAO query lowercases both sides (`LOWER(contentPreview) GLOB LOWER(:pattern)`) so Latin searches are case-insensitive. The repository escapes the user input and surrounds it with `*` before binding, so literal `*`/`?`/`[` in a query match themselves rather than acting as wildcards.

**Rationale**: Keeping escaping in the repository (not the DAO) keeps the DAO a thin SQL declaration and puts the one piece of real logic where it is unit-testable. `contentPreview` is never displayed (`EntryEntity.toEntry()` drops it), so there is no side effect from matching on its raw form.

## Risks / Trade-offs

- **[No index acceleration]** `GLOB`/`LIKE` with a leading wildcard cannot use the FTS index (or a B-tree); it is a full scan over `contentPreview`. → Acceptable: this is a brain-dump app; the `entries` table is at most thousands of rows of ≤500-char previews. A scan is sub-millisecond on device. If scale ever demands it, the FTS table can be reintroduced with a real tokenizer — but not today.
- **[Single-char queries are broad]** A one-character query matches many entries. → Expected behavior for substring search; the user can narrow by typing more.
- **[Escaping correctness]** If GLOB metacharacters in user input aren't escaped, a stray `*` matches everything. → Covered by a unit test for the escape function; the repository is the single call site.
- **[`entries_fts` becomes vestigial]** Search no longer reads it, and `rebuildFtsIndex()` remains but is harmless. → Acceptable for this change; removing the FTS table is a separate cleanup (out of scope, would be a schema bump). Leaving it avoids a migration.

## Migration Plan

None required. `GLOB` reads the `entries.contentPreview` column as it already exists. No schema version bump, no rebuild, no destructive migration. Existing installs and new installs behave identically on first launch after the update.

## Open Questions

None — query mechanism (GLOB), bottom-bar behavior (hide entirely), and focus pattern are all confirmed.

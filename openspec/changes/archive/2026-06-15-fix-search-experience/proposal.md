## Why

The search feature has two problems that make it hard to use. First, tapping the search icon expands the field but never focuses it — the user must tap again to bring up the keyboard. Second, the bottom input bar stays on screen while searching, wasting space and inviting accidental input. Third, and most damaging, Chinese (CJK) search is effectively broken: the FTS index uses SQLite's default `simple` tokenizer, which treats every CJK code point as a token boundary, so multi-character Chinese words almost never match.

## What Changes

- Auto-focus the search field the moment it expands so the keyboard appears immediately (mirroring the existing `FullscreenEditScreen` focus pattern).
- Hide the bottom input bar entirely while search is expanded, and restore it when search is closed (symmetric with the existing multi-select hide logic).
- Fix Chinese search by switching the query mechanism from FTS `MATCH` to SQLite `GLOB` substring matching against the raw `contentPreview` column. GLOB matches the original text directly, so CJK runs are already contiguous characters — no segmentation is needed at all. This removes the tokenizer entirely (no bigram preprocessing, no index reformatting, no schema change) and replaces one DAO query.

## Capabilities

### New Capabilities
- `entry-search`: The end-to-end entry search experience — expanding the search field from the root top bar, typing a query, and getting matches. Covers auto-focus on expand, hiding the bottom bar while searching, and substring matching via `GLOB` so Chinese (and any language) substring search returns correct results.

### Modified Capabilities
<!-- No existing specs describe search behavior today, so this is a fresh capability rather than a delta. -->

## Impact

- `data/EntryDao.kt` — `search()` switches from FTS `MATCH` to a `GLOB` substring query against the `entries.contentPreview` column (the FTS join is dropped from this query).
- `data/EntryFts.kt` — **unchanged** (the FTS table is left in place; search simply no longer reads through it).
- `data/MindDumpRepository.kt` — `searchEntries` passes the raw query to the DAO; GLOB wildcards (`*`, `?`) in user input are escaped so literal content still matches literally. Lowercasing on both sides gives case-insensitive matching for Latin text.
- `ui/MainScreen.kt` — add a `FocusRequester` to the search field and request focus on expand; gate `bottomBar` visibility on `!searchExpanded` in addition to `!isMultiSelectMode`.
- No Room schema version bump, no migration, no rebuild required — the underlying column data is unchanged.
- No new third-party dependencies.

# entry-search Specification

## Purpose
TBD - created by archiving change fix-search-experience. Update Purpose after archive.
## Requirements
### Requirement: Search field auto-focuses when expanded

When the user taps the search action in the root top app bar, the search field SHALL expand and immediately receive input focus so the soft keyboard appears without a second tap.

#### Scenario: Tapping search shows the keyboard
- **WHEN** the user taps the search action on the root feed
- **THEN** the search field SHALL expand into the top app bar
- **AND** the field SHALL receive input focus
- **AND** the soft keyboard SHALL appear automatically without any further tap

#### Scenario: Keyboard does not steal focus from already-focused input
- **WHEN** the search field is already expanded and focused
- **THEN** re-entering the root feed SHALL NOT cause a duplicate focus request that disrupts typing

### Requirement: Bottom input bar is hidden while searching

While the search field is expanded, the bottom input bar SHALL be hidden so it does not occupy screen space or capture input. The bottom input bar SHALL reappear when the search is closed.

#### Scenario: Expanding search hides the bottom input bar
- **WHEN** the user expands the search field
- **THEN** the bottom input bar SHALL be removed from the screen

#### Scenario: Closing search restores the bottom input bar
- **WHEN** the user clears the query and closes the search field
- **THEN** the bottom input bar SHALL become visible again

#### Scenario: Multi-select hide behavior is unaffected
- **WHEN** the user enters multi-select mode (search not expanded)
- **THEN** the bottom input bar SHALL remain hidden exactly as before
- **AND** the bottom input bar SHALL return only after multi-select is exited

### Requirement: Search matches entries by substring

Search SHALL match entries whose stored content contains the query as a contiguous substring, regardless of the characters' language. The match SHALL operate on the raw entry content text rather than on a tokenized index, so multi-character words in any language — including Chinese (CJK) — match correctly without segmentation.

#### Scenario: Multi-character Chinese phrase matches
- **WHEN** an entry contains the text "今天天气真好" in its content preview
- **AND** the user searches "天气"
- **THEN** the entry SHALL appear in the results

#### Scenario: Longer Chinese phrase matches
- **WHEN** an entry contains "我想去北京旅行"
- **AND** the user searches "北京旅行"
- **THEN** the entry SHALL appear in the results

#### Scenario: Phrase not present returns no match
- **WHEN** no entry contains the searched phrase as a substring
- **THEN** no entries SHALL be returned for that query

#### Scenario: Latin and numeric tokens match case-insensitively
- **WHEN** an entry contains "Meeting notes 2024"
- **AND** the user searches "meeting" or "2024" or "NOTES"
- **THEN** the entry SHALL appear in the results for any of those queries

#### Scenario: Mixed CJK and Latin content
- **WHEN** an entry contains "用 Kotlin 写代码"
- **AND** the user searches "写代码" or "kotlin"
- **THEN** the entry SHALL appear in the results for either query

#### Scenario: Query with GLOB wildcard characters matches literally
- **WHEN** an entry contains "100% done"
- **AND** the user searches "100%" (where `%` is a literal character, not a wildcard)
- **THEN** the entry SHALL appear in the results
- **AND** characters such as `*`, `?`, and `[` typed by the user SHALL be treated as literals, not as match wildcards

### Requirement: Search remains scoped to the current space

Search results SHALL respect the current space (Public or Private) boundary; an entry in one space SHALL NOT appear in search results for the other space.

#### Scenario: Private entry not shown in public search
- **WHEN** the user is in the Public space
- **AND** searches a phrase that exists only in a Private-space entry
- **THEN** no results SHALL be returned

### Requirement: Tag is a filter dimension

The entry list SHALL support filtering by tag as a composable dimension alongside existing search. Tag filtering SHALL operate on the index populated from sidecars (see database-rebuild). Tag filtering SHALL respect the current space boundary exactly as search does, and SHALL NOT surface Private entries whose sidecars are still encrypted.

#### Scenario: Filter list by a tag
- **WHEN** the user applies a tag filter for "idea" in the current space
- **THEN** only entries in the current space carrying the "idea" tag SHALL be shown

#### Scenario: Locked Private entries are not surfaced by tag
- **WHEN** the user filters by a tag while in the Private space but the space is locked
- **THEN** no Private entries SHALL be surfaced, because their sidecars are not yet decrypted

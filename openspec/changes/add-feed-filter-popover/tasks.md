# Implementation Tasks — add-feed-filter-popover

## 1. Filter model & ViewModel wiring

- [x] 1.1 Add `FeedFilter` + `TimeFilter` (sealed: None / Bucket(kind) / Range(start,end)) to a small `FeedFilter.kt` (or in the ViewModel). `BucketKind` enum: TODAY, NEXT_3_DAYS, NEXT_7_DAYS, PAST_7_DAYS.
- [x] 1.2 Add `MindDumpEntry.date(): LocalDate` (from `tid`, system timezone, day granularity).
- [x] 1.3 Implement `FeedFilter.matches(entry)`: time (entry.date() in bucket/range, skipped when None), todo (entry.todoState in set when non-empty), tag (**intersection** — `tags.all { it in entry.tags }` when non-empty). AND across active dimensions.
- [x] 1.4 Add bucket→inclusive `[start,end]` resolver (`LocalDate` math relative to today).
- [x] 1.5 Replace `tagFilter: String?` / `tagFilterFlow` with `filterFlow: MutableStateFlow<FeedFilter>`; expose `uiState.feedFilter` + `uiState.filterActive` (per-dimension active flags for the indicators).
- [x] 1.6 `entriesFlow` combines space + search + filter; narrow by `matches(filter)`. Keep search composition.
- [x] 1.7 Compute `uiState.facetTags`: distinct tags among entries matching time+todo dimensions only, restricted to tags that co-occur with the currently-selected tag set (a tag listed only if some matching entry carries it AND all selected tags). Always include already-selected tags so they stay visible.
- [x] 1.8 Mutators: `setTimeFilter(TimeFilter)`, `toggleTodo(TodoState)`, `toggleTag(String)`, `clearTime()`, `clearTodo()`, `clearTags()`, `clearAllFilter()`. Clear-all on space switch.

## 2. UI — three top-bar buttons + indicators

- [x] 2.1 Add eventTime, todo, tag `IconButton`s to the root top app bar `actions` (icons: e.g. `Icons.Filled.Schedule`/`Event`, `CheckCircle`/`TaskAlt`, `Label`/`Sell`). Place alongside Search/Statistics/Settings; decide visibility while `searchExpanded`.
- [x] 2.2 Per-button active indicator (dot/badge) when its dimension is non-default.

## 3. UI — eventTime & todo dropdowns

- [x] 3.1 eventTime `DropdownMenu`: 今天 / 三天 / 一周 / 过去一周 / 自定义. Highlight the active bucket; re-tapping it deactivates → `None`.
- [x] 3.2 自定义 opens a Material3 `DatePicker` (start, then end; both required) → `TimeFilter.Range(start, end)` inclusive.
- [x] 3.3 todo `DropdownMenu`: multi-select `TODO/DOING/WAIT/DONE/CANCEL` (checkable items); tap toggles; clear affordance.

## 4. UI — tag faceted inline list

- [x] 4.1 A `FlowRow` above the entry list showing `uiState.facetTags` as chips. Selected tags highlighted; unselected-available tags tappable; hidden tags not rendered.
- [x] 4.2 Toggle the list's visibility via the tag action (and keep it shown while any tag is selected). Auto-hide when no tag selected and not toggled (open question default).
- [x] 4.3 Tap a selected tag → deselect (`toggleTag`); tap an available tag → select (intersection narrows).
- [x] 4.4 Remove the old standalone single-tag `FilterChip` row (the **BREAKING** removal). Verify nothing else referenced `uiState.tagFilter`.

## 5. UI — empty state + clear-all

- [x] 5.1 Distinct empty state when a filter yields zero entries ("当前筛选无匹配" / "No matches for the current filter") vs. a genuinely empty scope.
- [x] 5.2 Global clear-all affordance (e.g. in the overflow or on each popover footer).

## 6. Strings (zh-CN + en)

- [x] 6.1 `res/values/strings.xml`: three action descriptions; time-bucket labels (今天/三天/一周/过去一周/自定义); todo labels (reuse existing status strings if present); custom range start/end; clear / clear-all; empty-filtered state.
- [x] 6.2 `res/values-en/strings.xml`: English counterparts.

## 7. Verification

- [x] 7.1 `./gradlew detekt ktlintCheck` clean.
- [x] 7.2 Unit tests: `FeedFilter.matches()` (each dimension single/active, empty-dimension-skip, AND across); bucket resolver math (today/forward/past inclusivity); facet computation (A/B/C example → select tag1 keeps tag2,tag3 available, hides tag4; select tag2 → only A).
- [ ] 7.3 Manual: each dimension alone; all-three AND; search+filter; group-scope filtering; tag intersection narrowing + facet list updates; clear-all; per-button indicator on/off; space-switch clears filter; locked-Private not surfaced.

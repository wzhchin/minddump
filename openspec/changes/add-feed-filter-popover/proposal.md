## Why

The feed can be narrowed by a single tag today, and content can be searched, but there is no way to slice by todo status or by a time window — nor to combine criteria. Users with growing libraries cannot answer "what's still TODO this week" or "everything shared by these tags" without scrolling. Three independent top-bar filter affordances — time, todo, and tag — that combine under AND close that gap, with tag narrowing expressed as a live, faceted inline list (a folder-like intersection) rather than a checkbox sheet.

## What Changes

- Add **three independent filter actions** to the root top app bar (alongside Search/Statistics/Settings): **eventTime**, **todo**, and **tag**. Each opens its own `DropdownMenu` anchored below it.
- **eventTime** dropdown — preset time buckets plus custom:
  - 今天 (today)
  - 三天 (next 3 days — today forward)
  - 一周 (next 7 days — today forward)
  - 过去一周 (past 7 days — today backward)
  - 自定义 (custom inclusive `[start, end]` date range via Material3 `DatePicker`)
- **todo** dropdown — multi-select across `TODO`, `DOING`, `WAIT`, `DONE`, `CANCEL`.
- **tag** — opens an **inline wrap list** of tags above the entry list (not a checkbox dropdown). Selecting tags is **intersection** semantics (an entry must carry **all** selected tags). The list is **faceted/live**: it shows the tags still present among the currently-visible entries, with unselected-but-available tags highlighted as tappable and tags that would yield no entries hidden. Selecting narrows; deselecting widens.
- The three dimensions combine with **AND**: an entry shows only when it satisfies time AND todo AND tag. An empty dimension imposes no constraint.
- **BREAKING** — Replace the existing single-tag filter chip and its single-select (any-match) semantics. The standalone `FilterChip` row below the app bar is removed; tag filtering is now the inline faceted list, and eventTime/todo are the two new dropdowns.
- Filtering composes with search (entry must match the query AND every active filter dimension) and stays scoped to the current space and current group scope, exactly as the existing tag filter does.

## Capabilities

### New Capabilities
<!-- None: filtering extends the existing entry-search capability rather than introducing a parallel one. -->

### Modified Capabilities
- `entry-search`: replace the "Tag is a filter dimension" requirement with a "Feed filtering by time, todo status, and tags" requirement set — three independent top-bar affordances, AND-combined; time as preset buckets (past + future) plus custom range; todo multi-select; tag as an intersection-narrowing faceted inline list. Keep the cross-space-isolation and locked-Private behavior already specified for tag filtering. Remove the single-tag chip.

## Impact

- **UI** (`ui/MainScreen.kt`): add the three filter `IconButton`s to the top app bar `actions`; render each `DropdownMenu`; render the inline tag list (a `FlowRow` above the entry list) when the tag filter is open or active; remove the standalone tag `FilterChip` row; show an active indicator on each button.
- **ViewModel** (`ui/MindDumpViewModel.kt`): replace `tagFilter: String?` with a `FeedFilter` state holding a time bucket/custom-range, a todo set, and a tag set (intersection); combine into `entriesFlow`; expose the faceted tag list (tags present among currently-visible entries) reactively.
- **Filtering** runs in memory on the already-scope-filtered entries (entries carry tags + todoState + tid); no DAO/SQL change.
- **Strings** (`res/values/strings.xml` zh-CN, `res/values-en/strings.xml` en): three action descriptions, time-bucket labels, todo labels (reuse if present), custom range placeholders, empty-state copy.
- **Dependencies**: custom date range uses Material3 `DatePicker` — no new library.

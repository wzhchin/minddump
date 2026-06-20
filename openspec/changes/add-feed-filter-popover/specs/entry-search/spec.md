## REMOVED Requirements

### Requirement: Tag is a filter dimension
**Reason**: Replaced by the broader "Feed filtering by time, todo status, and tags" requirement set. Tag filtering is no longer a single-select any-match chip; it is now an intersection-narrowing faceted inline list, composed under AND with two new dimensions (time, todo).
**Migration**: The standalone tag `FilterChip` below the app bar is removed. Tag selection moves to the inline faceted list opened by the new tag action. Semantics change from any-match (OR) to intersection (AND across selected tags).

## ADDED Requirements

### Requirement: Three independent filter actions in the top app bar

The root top app bar SHALL expose three independent filter actions — **eventTime**, **todo**, and **tag** — each opening its own popover (dropdown) anchored to the action. The three dimensions SHALL combine with **AND**: an entry is shown only when it satisfies every active dimension. A dimension left inactive imposes no constraint; with all three inactive the feed is unfiltered. The filter SHALL compose with an active search query (entry must match the query AND every active filter dimension). The filter SHALL apply to the current scope — the current space and, when a group page is open, that group's direct members — exactly as the former tag filter did.

#### Scenario: Each action opens its own popover
- **WHEN** the user taps the eventTime, todo, or tag action
- **THEN** a popover SHALL appear anchored to that action, scoped to its dimension

#### Scenario: AND-combines across dimensions
- **WHEN** the user selects eventTime "今天", todo "TODO", and tag "projectA"
- **THEN** only entries that are today AND have todo status "TODO" AND carry "projectA" SHALL be shown

#### Scenario: Filter composes with search
- **WHEN** the user has an active search query "meeting" and selects tag "idea"
- **THEN** only entries matching "meeting" AND carrying "idea" SHALL be shown

#### Scenario: Filter applies to the open group scope
- **WHEN** the user is inside a group page and applies a todo filter
- **THEN** only direct members of that group matching the filter SHALL be shown

#### Scenario: All-inactive filter shows the unfiltered feed
- **WHEN** no dimension is active
- **THEN** the feed SHALL show every entry in the current scope, unfiltered

### Requirement: eventTime filters by preset buckets or a custom range

The eventTime popover SHALL offer preset time buckets and a custom range:
- 今天 (today)
- 三天 (the next 3 days — today inclusive, forward)
- 一周 (the next 7 days — today inclusive, forward)
- 过去一周 (the past 7 days — today inclusive, backward)
- 自定义 (a custom inclusive `[start, end]` date range chosen via a date picker)

A bucket SHALL match an entry whose date — derived from its `tid` (epoch-millis, in the device timezone, day granularity) — falls within the bucket's inclusive day range. Selecting a bucket or applying a custom range activates the dimension; selecting the active bucket again (or a clear affordance) deactivates it. Only one time selection SHALL be active at a time.

#### Scenario: Today bucket matches same-day entries
- **WHEN** the user selects "今天"
- **THEN** only entries whose date is today SHALL be shown

#### Scenario: Forward bucket matches upcoming entries
- **WHEN** the user selects "一周"
- **THEN** only entries whose date is within today through today+6 days (inclusive) SHALL be shown

#### Scenario: Past bucket matches earlier entries
- **WHEN** the user selects "过去一周"
- **THEN** only entries whose date is within today-6 days through today (inclusive) SHALL be shown

#### Scenario: Custom range is inclusive
- **WHEN** the user picks custom start 2026-06-10 and end 2026-06-20
- **THEN** entries whose date is within 2026-06-10 through 2026-06-20 (inclusive) SHALL be shown

#### Scenario: Selecting the active bucket deactivates the dimension
- **WHEN** "今天" is active and the user selects "今天" again
- **THEN** the time dimension SHALL become inactive and the feed SHALL no longer be constrained by time

### Requirement: todo filters by multiple statuses

The todo popover SHALL offer the todo statuses `TODO`, `DOING`, `WAIT`, `DONE`, and `CANCEL` as a multi-select dimension. An entry matches the todo dimension when its todo status is one of the selected statuses. Entries with no todo status (plain notes) SHALL NOT match when any status is selected. Selecting an already-selected status deselects it.

#### Scenario: Filter by a single status
- **WHEN** the user selects "TODO"
- **THEN** only entries whose todo status is "TODO" SHALL be shown

#### Scenario: Filter by multiple statuses
- **WHEN** the user selects "TODO" and "DOING"
- **THEN** entries whose todo status is "TODO" or "DOING" SHALL be shown

#### Scenario: Plain notes excluded when a status is selected
- **WHEN** the user selects any todo status
- **THEN** entries with no todo status (plain notes) SHALL NOT be shown

### Requirement: tag filters by intersection via a faceted inline list

The tag action SHALL open an inline wrap list of tags above the entry list (not a checkbox popover). Tag selection is **intersection** semantics: when multiple tags are selected, an entry is shown only when it carries **all** of the selected tags. The list is **faceted/live**: it SHALL present the tags that still occur among the entries currently visible after applying the other dimensions and the already-selected tags; a tag that no visible entry carries SHALL NOT be shown; an unselected-but-available tag SHALL be shown as tappable. Selecting a tag narrows the list; deselecting (tapping an already-selected tag) widens it. When no tag is selected the list shows every tag in the current scope.

#### Scenario: Selecting one tag narrows to entries carrying it
- **WHEN** entries A (tag1, tag2), B (tag1, tag3), C (tag2, tag4) are visible and the user selects tag1
- **THEN** only entries A and B SHALL be shown (C lacks tag1)

#### Scenario: The list reflects remaining tags after a selection
- **WHEN** tag1 is selected and A (tag1, tag2), B (tag1, tag3) remain visible
- **THEN** the tag list SHALL offer tag1 (selected), tag2, and tag3, and SHALL NOT show tag4 (no remaining entry has it)

#### Scenario: Selecting a second tag intersects
- **WHEN** tag1 is selected (A, B visible) and the user selects tag2
- **THEN** only entry A (carrying both tag1 and tag2) SHALL be shown

#### Scenario: Deselecting a tag widens
- **WHEN** tag1 and tag2 are selected (A visible) and the user deselects tag2
- **THEN** entries A and B (those carrying tag1) SHALL be shown again

### Requirement: Filter actions indicate active dimensions and allow clearing

Each filter action SHALL indicate when its dimension is active (e.g. a dot or badge on the icon). Each popover SHALL provide a way to clear its own dimension (e.g. tapping the active preset again, or a clear affordance). A global clear SHALL empty all three dimensions.

#### Scenario: A button indicates its dimension is active
- **WHEN** a dimension is active
- **THEN** that dimension's action SHALL show a visual indicator

#### Scenario: Clear all dimensions
- **WHEN** the user invokes clear-all
- **THEN** every dimension SHALL become inactive and the feed SHALL return to unfiltered

### Requirement: Filtering is scoped to the current space and cleared on space switch

Tag, todo, and time filtering SHALL respect the current space boundary exactly as search does, and SHALL NOT surface Private entries whose sidecars are still encrypted. Switching spaces SHALL clear all active filter dimensions (so a stale filter from one space does not leak into the other).

#### Scenario: Locked Private entries are not surfaced
- **WHEN** the user applies any filter while in the Private space but the space is locked
- **THEN** no Private entries SHALL be surfaced, because their sidecars are not yet decrypted

#### Scenario: Switching spaces clears filters
- **WHEN** filters are active in Public and the user switches to Private
- **THEN** all filter dimensions SHALL be cleared

### Requirement: Localization of filter strings

All filter-related user-facing strings SHALL be provided in both the default zh-CN locale (`values/strings.xml`) and the en locale (`values-en/strings.xml`).

#### Scenario: English locale for filter strings
- **WHEN** the device locale is English
- **THEN** the three action descriptions, time-bucket labels, todo labels, custom-range labels, and clear-all action SHALL render in English

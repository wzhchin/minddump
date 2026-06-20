## Context

The feed supports a single-tag filter (`MindDumpViewModel.tagFilterFlow`, any-match) and a content search (`searchEntries`). Entries served to the UI already carry `tags`, `todoState`, and `tid` (the rebuild-stable epoch-millis identity, v6 schema). There is no todo or time filtering, and no way to combine criteria. This change adds three independent top-bar filter affordances — time, todo, and tag — that combine under AND, with tag expressed as an intersection-narrowing **faceted inline list** (a folder-like intersection) rather than a checkbox sheet. It is a UI + ViewModel change; no DAO/SQL work.

See `proposal.md` for motivation; `specs/entry-search/spec.md` for the behavior contract.

## Goals / Non-Goals

**Goals:**
- Three independent top-bar buttons (eventTime / todo / tag), each its own `DropdownMenu` (tag's "menu" is the inline list).
- eventTime: preset buckets (今天 / 三天 / 一周 forward / 过去一周 backward) + 自定义 inclusive `[start, end]`; single-active.
- todo: multi-select statuses.
- tag: inline `FlowRow` above the list, **intersection** semantics, **faceted** (list reflects tags still present among visible entries).
- AND across all three; composes with search; scoped to current space + current group scope; cleared on space switch.

**Non-Goals:**
- OR-combination across dimensions (AND only, per user decision).
- Persisting filters across restarts or per-space/per-group. Session state.
- Saved/named filter presets.
- New DAO queries for todo/time. In-memory filtering over already-fetched entries.

## Decisions

### Decision 1: `FeedFilter` value combining time + todo + tag, AND in `entriesFlow`

**Choice.** Replace `tagFilter: String?` with a `FeedFilter` held in a `MutableStateFlow<FeedFilter>`:
```kotlin
sealed interface TimeFilter { object None; data class Bucket(kind: BucketKind); data class Range(start: LocalDate, end: LocalDate) }
data class FeedFilter(
    val time: TimeFilter = TimeFilter.None,
    val todoStates: Set<TodoState> = emptySet(),
    val tags: Set<String> = emptySet(),   // intersection: entry must carry ALL
)
```
`entriesFlow` `combine`s `currentSpaceFlow`, `searchQueryFlow`, and `filterFlow`, narrowing by each active dimension. Tag dimension uses **intersection**: `filter.tags.all { it in entry.tags }` when `tags` non-empty.

**Why intersection, not any-match.** The user modeled tags as folders — selecting several means "entries in all of these folders", i.e. intersection. (The pre-existing single-tag filter was trivially both; multi-tag forces the choice, and the user chose intersection.)

**Why in-memory.** Entries already arrive with tags + todoState + tid. SQL would add queries/indexes for no gain at this scale and would make the three dimensions inconsistent (tag via join, todo/time via SQL). One in-memory pass keeps them uniform and makes the faceted tag list trivial to compute (it's just the tags of the currently-visible entries).

### Decision 2: Three independent buttons, each a `DropdownMenu`; tag list is inline

**Choice.** eventTime and todo open `DropdownMenu`s anchored to their buttons. The **tag** action toggles the inline faceted `FlowRow` above the entry list (and the list stays visible while any tag is selected, even if the user dismisses the toggle — selection persists; the toggle only shows/hides the affordance). The faceted list = distinct tags among entries visible after applying time + todo + already-selected tags.

**Why tag differs (inline vs dropdown).** A dropdown checkbox sheet can't show the faceted narrowing ("which tags still have results") naturally — the user explicitly wants the list to reflect remaining options as they select. An inline list that recomputes per selection is the standard faceted-filter UX.

**Why eventTime/todo are dropdowns.** They're bounded enums (5 buckets / 5 statuses); a dropdown is the lightest affordance and anchors cleanly to each button.

### Decision 3: Time buckets are day-granular, device timezone; one active selection

**Choice.** `MindDumpEntry.date(): LocalDate = Instant.ofEpochMilli(tid).atZone(systemDefault()).toLocalDate()`. Buckets (today = now):
- 今天: `[today, today]`
- 三天: `[today, today+2]` (3 calendar days inclusive)
- 一周: `[today, today+6]` (7 calendar days inclusive, forward)
- 过去一周: `[today-6, today]` (7 calendar days inclusive, backward)
- 自定义: `[start, end]` inclusive.

One time selection active at a time; re-tapping the active bucket deactivates (→ `None`).

**Why "一周" is forward.** The user clarified 一周 = future week (entries scheduled ahead via events), 过去一周 = past week. Most content entries are past, so forward buckets mainly surface entries carrying future scheduled events — acceptable and intentional per the user.

**Why day granularity.** `tid` is the only timestamp; a date-range filter is naturally day-granular. The same-second collision offset in `tid` doesn't change the day.

### Decision 4: Faceted tag list derives from the visible set, reactively

**Choice.** Expose `uiState.facetTags: List<String>` = distinct tags among `entriesFlow`'s current emission **after** applying time + todo dimensions (but the list itself drives tag selection, so it must not include the tag filter's own effect — compute facets from entries matching time+todo only, then the tag selection narrows within that). A tag not present in that set is hidden; selected tags always remain listed even if the intersection is about to go empty (so the user can deselect). 

**Why facets exclude the tag filter's own narrowing.** Otherwise selecting tag1 would immediately hide tag2/tag3 (the still-available options the user wants to see), defeating the faceted UX. Facets reflect the *other* dimensions + already-selected tags' co-occurring tags.

**Concrete recompute:** `facetTags = visibleAfterTimeAndTodo.flatMap { it.tags }.distinct()`, restricted to tags that co-occur with the current tag selection (a tag is listed only if some visible-after-time-andTodo entry both carries it AND carries all currently-selected tags).

### Decision 5: Active indicators per button; clear-all; clear-on-space-switch

**Choice.** Each button shows a dot/badge when its dimension is non-default. Each popover has a clear-for-its-dimension affordance. A global clear-all empties `FeedFilter`. Space switch resets `FeedFilter` (replaces today's `tagFilterFlow.value = null`).

## Risks / Trade-offs

- **[Facet recompute cost] → O(n·t) per emission.** Recomputing facets on every selection scans visible entries. **Mitigation:** personal-scale libraries; predicates are cheap. Revisit if measured slow.
- **[Forward time buckets mostly empty] → "no results" confusion.** Forward buckets surface future-event-bearing entries; most content is past. **Mitigation:** empty state distinguishes "no matches for this filter" from a genuinely empty scope (open question); users who pick a forward bucket expect scheduled items.
- **[Intersection tag selection can collapse to zero fast] → user stuck.** Selecting many tags narrows hard. **Mitigation:** facets hide tags that would yield nothing; deselect is one tap; clear-all available.
- **[Three buttons crowd the app bar] → layout pressure.** Search/Statistics/Settings + eventTime/todo/tag = 6 actions. **Mitigation:** actions are icon-only; if tight, hide some behind search-expanded rules already in place. Decide exact visibility at implementation time.

## Open Questions

- Should the inline tag list auto-hide when no tag is selected, or stay as a "browse tags" strip? **Leaning auto-hide** (only show when the user has toggled it on or a tag is active), to reduce clutter.
- Empty-state message when a filter yields nothing: distinct copy vs. the normal empty feed? **Leaning distinct** ("当前筛选无匹配" / "No matches for the current filter").
- Forward buckets (三天/一周) with no scheduled-event entries: show the bucket as empty, or note it surfaces scheduled items? **Leaning just empty + the distinct empty message**; document that forward buckets are for scheduled-event entries.

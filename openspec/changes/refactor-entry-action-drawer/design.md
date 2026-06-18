## Context

`EntryActionDrawer` (`ui/components/EntryActionDrawer.kt`) is a `ModalBottomSheet`
rendering every entry action as a full-width `ActionItem` row. It has grown to 12
actions (pin, todo status, add comment, tags, scheduled reminder, share, rename,
multi-select, move to group, move out of group, move space, delete). The long
uniform list buries the actions that actually carry state (status, tags,
reminder), and forces label-free quick actions into verbose full rows. The
reminder row in particular shows only "待提醒/已提醒" — not the useful value (the
due time).

The existing `entry-actions-drawer` spec predates the tags and scheduled-reminder
actions (added via the metadata-sidecar work) and says nothing about layout
tiers or summaries. This change reorganizes presentation and fills those spec
gaps.

## Goals / Non-Goals

**Goals:**

- Two-tier layout: icon-only quick-action bar (wraps to a second row) + detail
  rows for status / tags / reminder / delete.
- Show the reminder's **due time** (not just state) in its trailing summary.
- Show the **existing tags** in the tags row trailing summary (already done in
  code; spec it).
- Make pinned state visible on the pin icon (filled tint).
- Stop hardcoding zh-CN action labels; move all of them to `strings.xml` with
  English values; icons use them as `contentDescription`.

**Non-Goals:**

- No change to callbacks, conditional visibility, sub-dialogs, or any action's
  behavior.
- No data / Room / file-format / migration changes.
- Not adding the tag/reminder actions to a *new* capability — they belong to the
  drawer surface and are specified here.
- Not reordering actions semantically beyond the natural icon-bar vs detail-row
  split.

## Decisions

### D1: Quick-action bar = `FlowRow` of `IconButton`s, no labels.

A `FlowRow` of standard `IconButton`s keeps the existing icon set, wraps cleanly
to a second row (so a comment entry's smaller set still lays out correctly), and
needs no new component. Visible labels are dropped in favor of
`contentDescription`. The pin icon uses a filled icon + tint cue for the pinned
state.

- *Alternative considered:* a row of `AssistChip`s with icon+label — rejected:
  taller, and the user explicitly chose icon-only.

### D2: Pin visual cue = filled icon variant + `primary` tint when pinned.

`Icons.Filled.PushPin` is already filled, so the distinguishing cue is the
**tint**: default `onSurface` when unpinned, `primary` (and optionally a filled
background) when pinned. This matches how the entry card already communicates
pinned state via color rather than a label.

- *Alternative considered:* a `Selectable` `FilterChip` toggling selection —
  rejected: inconsistent with the other icon buttons in the bar.

### D3: Reminder time formatting reuses the existing relative formatter.

`EntryItem.kt` already has a today/yesterday/M月d日/yyyy年M月d日 + `HH:mm`
formatter. Extract it into a small shared helper (e.g. a top-level
`formatFriendlyDateTime(LocalDateTime): String` in `ui/`) and have the drawer's
reminder row call it. An entry can carry **multiple** events (`entry.events`);
the summary shows the soonest pending event's due, falling back to the most
recent fired event if none are pending, mirroring the "is there something to
surface" intent of the current state word.

- *Why not show all events:* the row is a summary; the picker remains the place
  to manage them.
- *Alternative considered:* ISO date string — rejected: not user-facing-friendly.

### D4: Move hardcoded labels to `strings.xml`, add English counterparts.

New keys: `action_rename`, `action_multi_select`, `action_move_to_group`,
`action_move_out_of_group`, `action_move_to_private`, `action_move_to_public`
(pin/comment/share/status/tags/reminder already have keys). Icons use these as
`contentDescription`; detail-row labels use them directly. Comments stay English.

### D5: Keep `ActionItem` for the four detail rows unchanged.

Status / tags / reminder / delete keep using the existing full-width
`ActionItem` (with `trailing`), preserving the trailing-summary capability. No
new row component is introduced.

## Risks / Trade-offs

- **[Icon discoverability]** Icon-only buttons are less discoverable than labeled
  rows for first-time users. → Mitigation: every icon has a localized
  `contentDescription`; the icons are standard, well-known glyphs (pin, share,
  folder, lock). Power users (the app's audience) gain speed.
- **[Pin cue color reliance]** The pinned cue is color/tint based, which is
  subtle and less robust under accessibility color settings. → Mitigation: pair
  tint with the filled icon variant; the label-less design is an accepted
  trade-off per the user's choice.
- **[Multiple events summary ambiguity]** Showing only the soonest pending event
  could hide other events from the summary. → Mitigation: this is a summary, not
  the management surface; the picker shows all. Document the choice in the
  helper.
- **[Formatter extraction scope creep]** Reusing `EntryItem`'s formatter means
  moving shared code. → Mitigation: small, pure, top-level function; no
  behavior change to existing call sites.

## Migration Plan

None required — purely presentational, no persistence or schema involvement.
Build, run, long-press a few entry types (file, comment, pinned, tagged, with a
reminder), confirm layout and summaries, then verify with `./gradlew detekt
ktlintCheck`. Rollback is reverting the one source file and the added string
keys.

## 1. String resources

- [x] 1.1 Add zh-CN keys to `app/src/main/res/values/strings.xml`: `action_rename` (重命名), `action_multi_select` (多选), `action_move_to_group` (移动到组), `action_move_out_of_group` (移出该组), `action_move_to_private` (移动到私有), `action_move_to_public` (移动到公共).
- [x] 1.2 Add matching en keys to `app/src/main/res/values-en/strings.xml`: Rename / Multi-select / Move to group / Move out of group / Move to Private / Move to Public.

## 2. Shared date-time helper

- [x] 2.1 Extract the today/yesterday/M月d日/yyyy年M月d日 + `HH:mm` relative formatter from `EntryItem.kt` (around lines 968–994) into a small top-level pure function, e.g. `formatFriendlyDateTime(LocalDateTime): String` in `ui/` (new file or `ui/util/`).
- [ ] 2.2 _(Deviation: NOT rerouted.)_ `EntryItem.formatRelativeTimestamp` has feed-specific `刚刚`/`N分钟前` duration branches that are wrong for a (often future) reminder due time; rerouting would change feed timestamps. Created a clean calendar-date-anchored `formatFriendlyDateTime(LocalDateTime)` in `ui/FriendlyDateTime.kt` instead. Feed output unchanged.

## 3. Drawer layout — quick-action icon bar

- [x] 3.1 Replace the pin/comment/share/rename/multi-select/move-to-group/move-out-of-group/move-space `ActionItem` rows with a `FlowRow` of `IconButton`s at the top of the drawer `Column`, in original order: pin → comment → share → rename → multi-select → move-to-group → move-out-of-group → move-space.
- [x] 3.2 Preserve conditional visibility exactly: hide pin + comment for comment entries; hide move-out-of-group unless `entry.groupPath != null`; compute move-space target/icon/label from `currentSpace` as today.
- [x] 3.3 Set each icon's `contentDescription` to the localized label via `stringResource` (pin/share/status/tags/reminder reuse existing keys; others use the new keys from §1).
- [x] 3.4 Pin icon: render `Icons.Filled.PushPin` with a `primary` tint when `entry.isPinned`, default `onSurface` otherwise, to give the pinned visual cue.
- [x] 3.5 Keep each icon button's `onClick` identical to the current row (haptic tick, then the existing callback / sub-dialog trigger / `onDismiss`).

## 4. Drawer layout — detail rows

- [x] 4.1 Keep the todo-status `ActionItem` row (with current-status trailing text) below the icon bar.
- [x] 4.2 Keep the tags `ActionItem` row; confirm it shows existing tags (`#tag1 #tag2 …`) as the trailing summary (no behavior change).
- [x] 4.3 Change the reminder `ActionItem` trailing summary to show the formatted due time (soonest pending, else most-recent fired event) via the §2 helper, instead of the bare 待触发/已触发 word.
- [x] 4.4 Keep the delete `ActionItem` row (`isDestructive`) as the last row.

## 5. Verification

- [ ] 5.1 Long-press a plain file entry: icon bar (8 icons, two rows), then status/tags/reminder/delete detail rows.
- [ ] 5.2 Long-press a comment entry: reduced icon set (no pin/comment/status); share/rename/multi-select/move/delete present.
- [ ] 5.3 Long-press a pinned entry: pin icon shows the pinned visual cue; an unpinned sibling does not.
- [ ] 5.4 Long-press an entry in a group: move-out-of-group icon appears; an entry not in a group omits it.
- [ ] 5.5 Long-press an entry with a scheduled reminder: reminder row shows the formatted due time; an entry with none shows no trailing summary.
- [ ] 5.6 Switch device locale to English: all icon `contentDescription`s and row labels render from en strings; no leftover hardcoded zh-CN.
- [x] 5.7 Run `./gradlew detekt ktlintCheck` and resolve any findings. _(Passed clean. `:app:compileDebugKotlin` also succeeds; the only warning — `Icons.Filled.Label` deprecation — is pre-existing, not introduced here.)_

> **Manual on-device verification (5.1–5.6) is not done.** These require running the app and long-pressing various entry types / switching locales. Code-verified equivalents: conditional visibility (5.2/5.4) and the pinned cue (5.3) are preserved by construction from the original rows; 5.1/5.5 compile and follow directly from the layout; 5.6 — all labels now route through `stringResource`, no hardcoded zh-CN remains in the drawer body. Run the app to confirm visually.

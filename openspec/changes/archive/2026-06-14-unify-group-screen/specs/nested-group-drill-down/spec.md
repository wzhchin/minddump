## MODIFIED Requirements

### Requirement: Group detail is a drillable sub-homepage
The app SHALL present the root feed (the current month directory) and every group as the **same** screen — `MainScreen` — parametrized by a `currentDir: File?`. `null` is the root (the current month directory, treated as the root group); a non-null `File` is an open group directory. Every level renders the same way: that scope's direct member entries, nested sub-group cards, and a bottom input bar. The currently open group SHALL be carried by the navigation back stack (`group_detail?groupPath={groupPath}`, Uri-encoded absolute path), which acts solely as the persistent carrier of `currentDir` — NOT as a separate screen. The in-scope group directory (`currentGroupDir`) SHALL be set exactly once per route entry, from a single `LaunchedEffect(currentDir)` in `MainScreen`, and SHALL NOT be cleared by any screen leaving composition (returning to root is done by navigating back to the root route, whose `currentDir` is null).

#### Scenario: Enter a group from the main feed
- **WHEN** the user taps a group card on the main feed
- **THEN** the app navigates to `group_detail/<encoded path>` and `MainScreen` re-renders for that group with `currentDir` set to that group directory
- **AND** the previous root feed remains on the back stack

#### Scenario: Drill into a sub-group
- **WHEN** the user taps a sub-group card inside a group page
- **THEN** the app navigates to `group_detail/<encoded sub-group path>` and `MainScreen` re-renders for that sub-group
- **AND** the previous group page remains on the back stack

#### Scenario: Back returns one level at a time
- **WHEN** the user presses system back from a sub-group page reached via `A → B`
- **THEN** the app returns to group A's page, not the root feed

#### Scenario: Capture route does not clear group scope
- **WHEN** the user is inside a group and navigates to the camera route to capture a photo or video
- **THEN** the group page SHALL remain on the back stack and `currentGroupDir` SHALL remain set to that group
- **AND** the captured media file SHALL be created inside that group directory (not the month-top level)

#### Scenario: Process death restores the open group
- **WHEN** the app is killed and relaunched while a group page is on top of the back stack
- **THEN** the app SHALL restore to that same group page (the route's encoded `groupPath` is the source of truth)

### Requirement: Group page shows only direct members
`MainScreen` at a given `currentDir` SHALL list only entries whose `groupPath` equals that directory's absolute path. At the root (`currentDir == null`) it SHALL list entries whose `groupPath` is null. Members of nested sub-groups SHALL NOT appear in a parent scope; they appear in their own sub-group page.

#### Scenario: Direct members listed
- **WHEN** a group page is open
- **THEN** only entries placed directly in that group directory are shown

#### Scenario: Root shows only ungrouped entries
- **WHEN** the root feed is shown
- **THEN** only entries whose `groupPath` is null are shown as standalone bubbles

#### Scenario: Sub-group members excluded
- **WHEN** a group contains a sub-group with its own members
- **THEN** those sub-group members are not listed in the parent group page

### Requirement: Nested sub-group cards in a group page
A group page SHALL display a card for each direct sub-group (sub-directory with group role) found inside the current group directory, using the same card presentation as group cards on the root feed. Tapping a sub-group card drills into it.

#### Scenario: Sub-groups shown as cards
- **WHEN** a group contains one or more sub-group directories
- **THEN** each sub-group is rendered as a card with its name and member summary

#### Scenario: Group with no sub-groups
- **WHEN** a group contains no sub-group directories
- **THEN** no sub-group cards are rendered (only its direct members and input bar)

## ADDED Requirements

### Requirement: Space toggle available only at the root scope
The InputBar's Public/Private space toggle (`SpaceSwitchButton`) SHALL be shown only when `currentDir` is null (the root feed). Inside any open group, the toggle SHALL be hidden; all other capture/import controls remain available. Space switching is therefore reachable only from the root feed.

#### Scenario: Root feed shows the space toggle
- **WHEN** the user is on the root feed (`currentDir == null`)
- **THEN** the InputBar SHALL display the Public/Private space toggle

#### Scenario: Open group hides the space toggle
- **WHEN** the user is inside a group (`currentDir != null`)
- **THEN** the InputBar SHALL hide the space toggle
- **AND** the text, audio, camera, import, and fullscreen-edit controls SHALL remain available

#### Scenario: Encryption behavior unchanged inside a group
- **WHEN** the user creates an entry inside a Private-space group
- **THEN** the entry SHALL be encrypted at rest, identical to a root Private-space entry
- **AND** no encryption setting is exposed inside the group (it follows the space)

## MODIFIED Requirements

### Requirement: Group detail is a drillable sub-homepage
The app SHALL present the root feed (the current month directory) and every group as the **same** screen — `MainScreen` — parametrized by a `currentDir: File?`. `null` is the root (the current month directory, treated as the root group); a non-null `File` is an open group directory. Every group renders the same way: that scope's direct member files and a bottom input bar. Because groups are limited to a single level, an open group contains only member files and never any sub-group cards. The currently open group SHALL be carried by the navigation back stack (`group_detail?groupPath={groupPath}`, Uri-encoded path relative to `workDir`), which acts solely as the persistent carrier of `currentDir` — NOT as a separate screen. The in-scope group directory (`currentGroupDir`) SHALL be set exactly once per route entry, from a single `LaunchedEffect(currentDir)` in `MainScreen`, and SHALL NOT be cleared by any screen leaving composition.

#### Scenario: Enter a group from the main feed
- **WHEN** the user taps a group card on the main feed
- **THEN** the app navigates to `group_detail/<encoded path>` and `MainScreen` re-renders for that group with `currentDir` set to that group directory
- **AND** the previous root feed remains on the back stack

#### Scenario: Back returns to the root feed
- **WHEN** the user presses system back from a group page
- **THEN** the app returns to the root feed (groups are single-level, so there is no intermediate group level)

#### Scenario: Capture route does not clear group scope
- **WHEN** the user is inside a group and navigates to the camera route to capture a photo or video
- **THEN** the group page SHALL remain on the back stack and `currentGroupDir` SHALL remain set to that group
- **AND** the captured media file SHALL be created inside that group directory (not the month-top level)

#### Scenario: Process death restores the open group
- **WHEN** the app is killed and relaunched while a group page is on top of the back stack
- **THEN** the app SHALL restore to that same group page (the route's encoded `groupPath` is the source of truth)

### Requirement: Group page shows only direct members
`MainScreen` at a given `currentDir` SHALL list only files whose `groupPath` equals that directory's path. At the root (`currentDir == null`) it SHALL list files whose `groupPath` is null. Because groups are single-level, a group directory contains only files (no sub-groups), so a group page shows exactly the files placed directly inside it.

#### Scenario: Direct members listed
- **WHEN** a group page is open
- **THEN** only files placed directly in that group directory are shown

#### Scenario: Root shows only ungrouped entries
- **WHEN** the root feed is shown
- **THEN** only files whose `groupPath` is null are shown as standalone bubbles, plus top-level group cards

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

### Requirement: In-group capture writes into the open group
When the user creates an entry from a group page's input bar (text, photo, audio, or imported file), the entry SHALL be written directly into the currently open group directory and indexed with that group's path, rather than being created at the month-top level and moved afterward.

#### Scenario: Text entry created inside a group
- **WHEN** the user types text in a group page's input bar and submits
- **THEN** the text entry file is created inside that group directory and appears as a direct member

#### Scenario: Photo captured inside a group
- **WHEN** the user captures a photo from a group page
- **THEN** the photo file is created inside that group directory and appears as a direct member

#### Scenario: Encrypted in-group entry
- **WHEN** the open group is in Private space and the user creates a text entry from its input bar
- **THEN** the entry is encrypted at rest, same as a month-top Private-space entry

### Requirement: Group creation only at the month-top level
The user SHALL create groups only at the month-top level (from the root feed). "新建组" SHALL NOT be available inside an open group page, because groups are single-level and cannot contain other groups. A group created from the root feed SHALL be created directly under the month bucket.

#### Scenario: Month-top creation
- **WHEN** the user creates a group from the main feed (no group open)
- **THEN** the group is created at the month-top level

#### Scenario: In-group group creation is unavailable
- **WHEN** the user is inside a group page
- **THEN** the "新建组" action SHALL NOT be offered, since a group cannot be nested inside another group

### Requirement: Parent-aware group dissolve
Dissolving a group SHALL move its members to the month-top level (the group's parent is always the month directory, because groups are single-level). The dissolved group directory is then deleted. There is no "dissolve a sub-group into a parent group" case, because no group has a group parent.

#### Scenario: Dissolve a month-top group
- **WHEN** the user dissolves a group
- **THEN** its members move to the month-top level (feed), and the group directory is deleted

## REMOVED Requirements

### Requirement: Nested sub-group cards in a group page
**Reason**: Groups are single-level; a group directory contains only files and never contains sub-group directories, so there are no sub-group cards to render.
**Migration**: A group page shows only its direct member files (and the input bar). Sub-group card rendering is deleted.

### Requirement: Sub-group creation under an open group
**Reason**: Groups are single-level; a sub-group cannot be created inside a group. (Superseded by the MODIFIED "Group creation only at the month-top level" requirement above.)
**Migration**: The "新建组" action is removed from inside group pages.

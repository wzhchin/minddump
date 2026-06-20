## MODIFIED Requirements

### Requirement: Group page shows only direct members
`MainScreen` at a given `currentDir` SHALL list only entries whose `parentId` equals the `tid` of the group that `currentDir` resolves to. At the root (`currentDir == null`) it SHALL list entries whose `parentId` is null. Members of nested sub-groups SHALL NOT appear in a parent scope; they appear in their own sub-group page. (Membership is now expressed by the integer `parentId` foreign key to `entries.tid`, replacing the former absolute-path `groupPath` string comparison; the navigation route still carries the directory path and resolves it to the group's `tid` on entry.)

#### Scenario: Direct members listed
- **WHEN** a group page is open
- **THEN** only entries whose `parentId` is that group's `tid` are shown

#### Scenario: Root shows only ungrouped entries
- **WHEN** the root feed is shown
- **THEN** only entries whose `parentId` is null are shown as standalone bubbles

#### Scenario: Sub-group members excluded
- **WHEN** a group contains a sub-group with its own members
- **THEN** those sub-group members are not listed in the parent group page

## ADDED Requirements

### Requirement: Multi-select merge accepts groups and re-clusters them
The multi-select "合并为分组" (merge-to-group) action SHALL accept a mixed selection of loose entries and whole groups. When the selection contains one or more groups, the merge SHALL move every member of every selected group — in addition to every selected loose entry — into the target group (an existing group or a newly created one). Source groups that become empty as a result SHALL be dissolved (removed from the filesystem) so no empty folder remains. Source groups that still retain members (because the target group coincided with a source, or because a member could not be moved) SHALL be left intact.

#### Scenario: Merge a mix of entries and groups into an existing group
- **WHEN** the user has 2 loose entries and 1 group selected in multi-select
- **AND** the user taps "合并为分组" and picks an existing target group
- **THEN** the 2 loose entries are moved into the target group
- **AND** every member of the selected group is moved into the target group
- **AND** the selected source group is dissolved (it is now empty and is removed)
- **AND** multi-select mode exits

#### Scenario: Merge several groups into a brand-new group (re-cluster)
- **WHEN** the user has 3 groups selected in multi-select and no loose entries
- **AND** the user taps "合并为分组" and creates a new group named "trip"
- **THEN** all members of the 3 source groups are moved into the new "trip" group
- **AND** the 3 source groups are dissolved (removed)
- **AND** the new "trip" group appears on the feed with those members

#### Scenario: Selected group's checkbox and the merge target interact safely
- **WHEN** the target group of a merge is itself one of the selected groups
- **THEN** the members of the other selected groups and loose entries are moved into that target group
- **AND** the target group is not dissolved (it still holds its original members plus the merged ones)
- **AND** only the other selected source groups are dissolved if they became empty

#### Scenario: Merge with only loose entries is unchanged
- **WHEN** the user merges a selection that contains only loose entries (no groups)
- **THEN** the behavior is identical to the previous loose-entries-only merge: entries move into the target group and no group is dissolved

### Requirement: Multi-select selection count reflects groups and entries
The multi-select top action bar SHALL report a count that reflects both selected loose entries and selected groups, so the user knows the selection contains whole groups.

#### Scenario: Count with groups present
- **WHEN** the user has 2 loose entries and 2 groups selected
- **THEN** the top bar reports both counts (e.g. "已选 4 项 · 含 2 分组")

#### Scenario: Count with only entries
- **WHEN** the user has 3 loose entries and no groups selected
- **THEN** the top bar reports the entry count only (e.g. "已选 3 项")

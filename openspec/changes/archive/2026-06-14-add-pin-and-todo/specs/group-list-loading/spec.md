## ADDED Requirements

### Requirement: Group list ordering is pinned-first by filename timestamp
The system SHALL render the group list ordered by filename (descending), so that pinned groups (carrying the `9999-` prefix) appear before unpinned groups, and within each block newer groups appear before older ones.

#### Scenario: Pinned group appears first
- **WHEN** the group list contains `9999-2506-12-080000-g-travel` and `2506-14-091500-g-work`
- **THEN** the pinned travel group is rendered above the newer work group

#### Scenario: Normal groups sort newest-first
- **WHEN** the group list contains `2506-13-120000-g-a` and `2506-12-080000-g-b` (both unpinned)
- **THEN** group `a` (June 13) is rendered above group `b` (June 12)

#### Scenario: Refresh re-reads current pin/status from disk
- **WHEN** a group is pinned and the list refreshes
- **THEN** the group moves to the pinned block at its timestamp position

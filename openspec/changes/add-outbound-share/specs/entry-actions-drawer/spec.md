## ADDED Requirements

### Requirement: Share action
The entry actions drawer SHALL include a "分享" (Share) action available for every entry type, including comments. Selecting it SHALL export the entry to other Android apps via the system Share sheet, as defined by the `outbound-share` capability.

#### Scenario: Share action is present
- **WHEN** the user opens the entry actions drawer for any entry (file or comment)
- **THEN** a "分享" action SHALL be listed among the drawer actions

#### Scenario: Share action on a text note
- **WHEN** the user taps "分享" on a text note
- **THEN** the drawer SHALL dismiss and the system Share sheet SHALL open with the note's text content

#### Scenario: Share action on a comment
- **WHEN** the user opens the drawer for a comment (`-n-`) entry
- **THEN** the "分享" action SHALL still be available (unlike pin/status/comment, share applies to comments)

#### Scenario: Share action on an encrypted Private entry while locked
- **WHEN** the user taps "分享" on an encrypted Private entry AND the session is locked
- **THEN** the Share sheet SHALL NOT open and a locked-session message SHALL be shown

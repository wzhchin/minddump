## ADDED Requirements

### Requirement: Kotlin version set to 2.2.0
The project SHALL use Kotlin 2.2.0 as defined in `gradle/libs.versions.toml` under the `kotlin` version key.

#### Scenario: Kotlin version is 2.2.0
- **WHEN** the version catalog is read
- **THEN** the `kotlin` version entry SHALL be `"2.2.0"`

### Requirement: KSP version aligned to Kotlin 2.2.0
The KSP version SHALL be updated to the release matching Kotlin 2.2.0 (pattern `2.2.0-<ksp-version>`).

#### Scenario: KSP version matches Kotlin
- **WHEN** the version catalog is read
- **THEN** the `ksp` version entry SHALL start with `2.2.0-`

### Requirement: Kotlin Compose plugin version matches Kotlin
The `kotlin-compose` plugin in the version catalog SHALL reference the same Kotlin version (`kotlin` version ref).

#### Scenario: Compose plugin aligned
- **WHEN** the plugins section of the version catalog is read
- **THEN** `kotlin-compose` SHALL use `version.ref = "kotlin"`

### Requirement: Project builds successfully with Kotlin 2.2.0
After the version bump, the full project SHALL compile without errors.

#### Scenario: Clean build passes
- **WHEN** `./gradlew assembleDebug` is run
- **THEN** the build SHALL complete with `BUILD SUCCESSFUL`

#### Scenario: No new compilation errors
- **WHEN** the build output is inspected
- **THEN** there SHALL be zero Kotlin compilation errors

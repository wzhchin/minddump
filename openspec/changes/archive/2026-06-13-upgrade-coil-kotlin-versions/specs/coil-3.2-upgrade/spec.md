## ADDED Requirements

### Requirement: Coil version set to 3.2.0
The project SHALL use Coil 3.2.0 as defined in `gradle/libs.versions.toml` under the `coil` version key.

#### Scenario: Coil version is 3.2.0
- **WHEN** the version catalog is read
- **THEN** the `coil` version entry SHALL be `"3.2.0"`

### Requirement: All Coil dependencies use the same version
Both `coil-compose` and `coil-network-okhttp` SHALL reference the `coil` version key in the version catalog.

#### Scenario: Coil libraries aligned
- **WHEN** the libraries section of the version catalog is read
- **THEN** both `coil-compose` and `coil-network-okhttp` SHALL use `version.ref = "coil"`

### Requirement: Existing image loading behaviour preserved
All existing AsyncImage composables and ImageRequest usage SHALL continue to function identically after the Coil upgrade. No visual or behavioural regressions in thumbnail loading.

#### Scenario: Thumbnails render correctly
- **WHEN** the app displays entry cards with photo thumbnails
- **THEN** images SHALL load and display as before the upgrade

#### Scenario: Image viewer still works
- **WHEN** a user taps a photo thumbnail to open the full image viewer
- **THEN** the high-resolution image SHALL load successfully

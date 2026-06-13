## Context

MindDump uses Kotlin 2.0.21 with Coil 3.0.4 for image loading. The Kotlin version pins KSP at `2.0.21-1.0.28`, which in turn limits which versions of Room, Hilt, and Coil's KSP-based components can be used. Coil 3.0.4 is several minor versions behind — newer releases include performance improvements, bug fixes, and API refinements.

All version management is centralised in `gradle/libs.versions.toml`. KSP versioning follows the pattern `<kotlin-version>-<ksp-version>`, so bumping Kotlin requires finding the matching KSP release.

## Goals / Non-Goals

**Goals:**
- Upgrade Kotlin to 2.2.0
- Upgrade Coil to 3.2.0
- Upgrade KSP to the version matching Kotlin 2.2.0
- Verify all downstream dependencies (Room, Hilt, Compose BOM, detekt, ktlint) remain compatible
- Resolve any source-level deprecation warnings or breakages from the Kotlin/Coil bumps

**Non-Goals:**
- Refactoring existing Coil usage patterns (AsyncImage calls, ImageRequest builders)
- Changing the image loading architecture
- Adding new Coil features (e.g., custom Fetchers, new DiskCache config)
- Upgrading AGP or other unrelated dependencies unless they block the Kotlin bump

## Decisions

### 1. Version bump ordering: Kotlin + KSP first, then Coil

KSP must match the exact Kotlin version. We bump Kotlin and KSP together, verify the build compiles, then bump Coil. This isolates failure domains — if something breaks after the Kotlin bump, it's not a Coil issue.

**Alternative considered**: Bump everything at once — rejected because a failure would be harder to attribute.

### 2. Minimal changes to dependency graph

Only change what the proposal requires: `kotlin`, `ksp`, and `coil` version entries. Other dependencies (Compose BOM, Room, Hilt, AGP) stay at current versions unless they are provably incompatible with Kotlin 2.2.0. We verify compatibility by building, not by reading changelogs.

### 3. Source-level fixes kept local

Any deprecation warnings or compile errors from Kotlin 2.2.0 stdlib changes will be fixed in-place. No architectural changes.

## Risks / Trade-offs

- **KSP version availability** → KSP `2.2.0-x.x.x` must exist on Maven Central. If not yet published, we'll use the latest KSP release for the `2.2` line.
- **Compose compiler plugin** → The Kotlin Compose plugin (`kotlin-compose`) is version-locked to Kotlin. Version 2.2.0 includes it, but the Compose BOM must support the generated compiler output. If incompatible, we may need a Compose BOM bump as well.
- **Room KSP processor** → Room's KSP processor must work with the new KSP version. Room 2.6.1 should be fine, but if not, a Room bump may be needed.
- **Detekt compatibility** → Detekt `1.23.7` may not support Kotlin 2.2.0. If it fails, we'll bump detekt to the latest compatible version.
- **Build cache invalidation** → Kotlin version change invalidates all Kotlin compilation caches. First build after upgrade will be slow.

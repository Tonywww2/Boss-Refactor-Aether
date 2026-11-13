# Changelog

All notable changes to Boss Refactor Aether are documented in this file.

## [1.4.4] - 2026-11-12

### Changed

- SenDimS is an optional integration with no version floor. An incompatible SenDimS (missing or changed Leader API, events or client indicator API) now logs a warning and turns the integration off, and the bosses keep fighting with this mod's own rules instead of failing to load or crashing.

## [1.4.3] - 2026-11-12

### Changed

- Slider Continuous Glide keeps one SenDimS parry window across the whole chain, including the pauses between dashes. When the first dash is unblockable, the pause before the first parryable dash lasts at least 20 ticks.
- SenDimS parried durations are clamped to 30-100 ticks, and every `parry_recovery_ticks` setting now accepts 30-100.
- Parryable windups (`continuous_glide_windup_ticks`, `skill_one_windup_ticks`, `skill_two_windup_ticks`, `rising_flame_windup_ticks`, `titan_fist_windup_ticks`) now have a minimum of 20 ticks.
- `slider.display.status_label_height_offset` now defaults to `1.75` so the status labels no longer overlap the SenDimS warning and stun characters.

### Fixed

- The SenDimS warning provider now shows a fixed warning while a parry window is open without a visible telegraph.
- Valkyrie Queen and Sun Spirit no longer close the SenDimS parry window from inside `LeaderParryAttemptEvent`.
- Slider, Valkyrie Queen and Sun Spirit resynchronize their local parry-window flag with SenDimS every tick, so a window restored from a save no longer stays open.

## [1.4.2] - 2026-10-14

### Added

- Added `boss_recovery.heal_when_no_target`, defaulting to `true`, to control whether bosses heal while they have no valid target. A dormant Slider still heals regardless of this setting.

### Changed

- Slider bypasses block collision on every tick again instead of restoring its previous collision state after chase.

## [1.4.1] - 2026-09-01

### Fixed

- Restored the Sun Spirit's original pre-combat invulnerability so it remains immune until its dialogue finishes and the boss fight begins.
- Slider now switches from patrol to chase when its target is outside its vertical attack range, allowing it to align with players at different elevations.
- Slider now retains block-collision bypass throughout chase movement and restores its previous collision state after leaving chase.
- Continuous Glide can now be prepared and released after vertical alignment at non-default arena elevations.

### Tests

- Added regression coverage for height-forced Slider chase, missing-target patrol recovery, and attack-height checks above the arena's default elevation.
- Verified on a dedicated server that Slider crosses a solid stone wall during chase and restores block collision after returning to patrol.
- Verified all mechanics tests and the full build, then launched the client directly into the existing `ggg` world.

## [1.4.0] - 2026-08-17

### Added

- Added `slider.range.continuous_glide_alignment_tolerance`, defaulting to `0.1` blocks, to control the maximum lateral center-line error before Continuous Glide locks its route.
- Added overlap recovery state for Slider chase movement.

### Changed

- Reworked Continuous Glide aiming to use a closed-form, axis-specific intercept time based on windup, estimated dash travel time, and the configured extra prediction ticks.
- Continuous Glide now keeps its X/Z single-axis movement while requiring the dash center line to align with the predicted player position before showing and locking the telegraph.
- Slider patrol and chase movement now reevaluate dash opportunities before the telegraph appears, while the route remains locked after the telegraph is shown.
- Patrol alignment points now validate directional arena space and ignore zero-progress intersections before stopping Slider movement.
- Continuous Glide telegraph width now matches the effective collision lane for the current target.

### Fixed

- Fixed Continuous Glide consistently missing stationary players because a laterally offset single-axis route could be accepted and locked.
- Fixed stationary targets failing to trigger follow-up behavior when Slider reached an already-aligned chase position.
- Fixed Slider and player overlap stopping ordinary chase logic indefinitely; Slider now escapes two blocks along the horizontal direction with the most available arena space before resuming chase.
- Fixed overlap between dash segments terminating Continuous Glide; follow-up dashes now choose the direction with the most available arena space and retain the normal full dash distance.
- Fixed zero-progress patrol alignment candidates repeatedly stopping Slider in place.
- Fixed directional arena limits being ignored when validating a patrol position for a future dash.
- Fixed vertical chase movement contributing to horizontal perimeter progress.
- Added a safe patrol fallback when an extreme arena configuration provides no usable overlap-escape distance.

### Tests

- Added regression coverage for stationary and moving intercept prediction, center-line tolerance, zero-progress alignment, directional arena reach, overlap detection, escape direction, escape distance, and transient-state reset.
- Verified the full stationary-target flow on a dedicated server: patrol alignment, telegraph lock, dash execution, and collision sweep intersection.
- Verified overlap recovery on a dedicated server for both ordinary chase and multi-segment Continuous Glide.

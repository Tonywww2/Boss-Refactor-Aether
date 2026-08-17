# Changelog

All notable changes to Boss Refactor Aether are documented in this file.

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

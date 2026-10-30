# Bubble and buoyancy refinement

This follow-up remains `1.0.0-rc.1`, protocol 14. Update both client and server.

## Exhaust

An exhale starts with a mouth-directed puff of five to seven bubbles, followed by a smaller burst of two or three and single trailing bubbles every four to seven ticks. Timing varies by breath; the existing breathing phases and gas use remain authoritative.

Each particle has its own size, upward acceleration, rise-speed limit, sideways drift and 8–15-second lifetime. Smaller bubbles generally rise more slowly. Particles fade during their last 2–4 seconds. Within a few blocks of a water surface, they also fade and dissolve at varied distances below the waterline. This prevents a solid group arriving at one height and popping together. Dry air and obstacles still remove exhaust.

The surface lookup checks at most a few nearby loaded client water cells, every four ticks. It does not search the ocean or load chunks. The single-bubble trail remains sparse after the stronger opening puff.

## Idle fins and depth hold

Idle bob is now ±0.08 blocks over ten seconds, down from ±0.30 over eight seconds. The vertical correction limit is 0.025 blocks/tick (0.5 blocks/second), half the former limit. Configured slower ascent limits continue to apply.

Passive fins capture physical height rather than continually moving their target with a sampled water-column depth. They recenter if their old target is more than half a block away. The client also ignores distant stale targets and can predict neutral rest while a refreshed hover state is delayed. Steering, rising and descending immediately release passive correction. A server-declined swimming pose still releases it.

Passive fins stop assisting rather than automatically forcing a multi-block dive to a deeper decompression stop. Existing decompression warnings and injury calculations are retained. The explicit V depth hold still tracks its chosen depth, with the gentler speed cap.

A reported three-block dip is larger than the old bob itself. These changes address two plausible code paths—an old rest target and a changed depth sample—but do not establish that either caused every reported dip or rule out server lag.

## Validation

The standalone suite checks varied sparse exhale schedules, bob/velocity bounds, slow ascent limits, convergence and rejection of a target three blocks away. Native client checks use actual particle cohorts to measure varied ascent, gradual opacity loss, staggered surface dissolution and bounded expiry. They also exercise a local player with an artificially stale snapshot three blocks below them, delayed hover data, manual movement and a new rest height.

The dedicated-server suite exercises real fin control for changed depth samples, stale targets, gentle correction, manual release, external impulses and a deeper decompression ceiling. These are controlled development-world cases, not a reproduction of live server latency.

Final results: [native particles and stale-target cases](qa/bubble-buoyancy-native.txt), [dedicated-server regressions](qa/bubble-buoyancy-server.txt), and [build, simulation and packaging](qa/bubble-buoyancy-build.txt). All passed. Earlier UI screenshots/reports retain their original build scope and checksums.

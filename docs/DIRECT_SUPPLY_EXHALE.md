# Direct cylinder supply and mouth exhaust

This refinement remains `1.0.0-rc.1`, protocol 14. Update the client and server together.

## Simpler equipment

A Create diving helmet draws gas directly from a worn Deep Diving cylinder. An enabled, filled inventory or offhand backup takes over when necessary, followed by available Create backtank air. Only the selected supply drains. A helmet is still required; empty and disabled backup cylinders do not supply gas. The existing Create-air configuration continues to control whether Deep Diving manages Create backtank breathing.

The separate regulator is removed from the creative equipment tab, obtainable recipes, Ponder index and preparation requirements. Advanced and high-capacity cylinder recipes now use a Create precision mechanism in its place; the emergency cylinder uses a fluid pipe. Preparation and backup advancements and the first-dive guides use the simpler setup.

The old item ID, model and texture remain registered so previous saves load their stacks. Craft an old regulator by itself to reclaim one precision mechanism. New cylinder recipes do not consume regulators. The former recipe and recipe-unlock IDs are explicitly disabled with NeoForge conditions, preventing obsolete definitions in incremental development resources from remaining active.

## Opening puff

Each exhale begins with five to seven bubbles clustered at the mouth, briefly moving outward with the diver's facing direction. A smaller burst of two or three follows two or three ticks later. The existing single-bubble trail follows at uneven four-to-seven-tick intervals. A normal exhale produces eight to fifteen bubbles, with most concentrated in its opening.

The individual rise speeds, sizes, drift, lifetimes, gradual surface fade and staggered dissolution from the preceding refinement are retained. Gas use and breathing phase timing are unchanged. The ±0.08-block idle bob, reduced correction speed and stale-rest-target protections are also retained.

## Validation

The numerical suite checks positive and negative breath variations, a denser opening than trail, bounded counts and sparse spacing, alongside the prior buoyancy and physiology regressions.

The dedicated-server checks exercise all three cylinder tiers without a regulator, the helmet requirement, inventory/offhand backup priority, single-supply consumption, empty/disabled supply rejection, Create fallback and its configuration, legacy stack serialization, recycling and real assembly of all three replacement recipes.

The connected integrated-server player earns all four preparation/dive/backup advancements without a regulator. This check resets and restores its advancement state because NeoForge intentionally blocks FakePlayer awards. Native client checks exercise the actual exhale handler: a mouth-local opening puff, outward movement and sparse trailing emission while using a cylinder directly. Existing particle-cohort, surface-fading, stale-rest-target and bundle-marker checks remain part of the same run. Ponder playback checks cover all seven tutorials and verify that the retired regulator no longer has a tutorial. These controlled checks do not reproduce live multiplayer latency.

Final results: [native mouth burst, particles and stale-target cases](qa/direct-supply-native.txt), [dedicated-server regressions](qa/direct-supply-server.txt), [Ponder playback](qa/direct-supply-ponder.txt), and [build, simulation and packaging](qa/direct-supply-build.txt). All passed. Earlier UI screenshots/reports retain their original build scope and checksums.

# 1.0.0-beta.3 checks and limits

Prepared October 29, 2026, from the complete rc.1 source. The build is now beta.3 and is not being presented as a release candidate. No mod-host release has been published.

See the current [changelog](../CHANGELOG.md), [bundle controls](MAP_BUNDLE.md) and [sign guide](DIVE_SIGNS.md). Existing physiology, gas, equipment and sign IDs are preserved. Client and server must both use beta.3 (protocol 13).

## Ascent balance retained

The default ascent warning threshold is 2.5 blocks/second. A fast ascent must last at least three continuous seconds and gain four blocks before warning. Slowing, stopping, descending or leaving the valid diving state resets that history. Actual decompression-ceiling violations remain independent.

Existing worlds preserve their configured maximum. To adopt the default, set `maximumAscentBlocksPerSecond = 2.5` in the world's `serverconfig/deepdiving-server.toml`. New grace/rise settings fill in automatically. See [the settings example](../config-examples/ascent-settings.toml).

## Validation

The original beta.3 results are recorded in [qa/beta3-validation.txt](qa/beta3-validation.txt). The later, broader audit and its bug fixes are documented in [RELEASE_QA.md](RELEASE_QA.md). Older audit screenshots and reports are historical and do not establish beta.3 results.

## Remaining live playtesting

Extended multiplayer dives, moving Deep Seas/Sable compartments and third-party shader combinations require testing in the actual server pack. These checks are not replaced by fixtures. No new animation/movement overhaul or Cold Sweat thermal equipment is included; swim trunks remain cosmetic.

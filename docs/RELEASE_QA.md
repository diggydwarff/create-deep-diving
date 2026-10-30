# Release QA follow-up — 1.0.0-beta.3

This audit keeps the build at **1.0.0-beta.3**. It does not publish or certify an official release. These results supersede the original beta.3 validation. The later [Ponder/interface follow-up](UI_POLISH.md) records the subsequent presentation changes and their current build checks; the checksums below identify the earlier audit build.

## Confirmed bugs fixed

| Issue | Before | Corrected behavior and regression check |
| --- | --- | --- |
| Cylinder equipment swap | Using a cylinder could replace chest armour carrying Curse of Binding. | Uses Minecraft's equipment swap implementation. A real survival-player fixture rejects the bound swap and conserves both items on an ordinary swap. |
| Partial blender fill | A blender stopped when its input buffer could not supply a complete production batch, even when a useful smaller fill was possible. | Finds the largest feasible partial batch. Seven available mB of oxygen now fill a cylinder with a nominal 32 mB production batch. |
| Small-batch gas ratios | Independently rounding each low-RPM fill could repeatedly charge zero oxygen for a low-oxygen mixture. | A conserved 100-unit flow cycle carries the blending position across fills and saves. Twenty-five 32 mB fills at 1% oxygen consume exactly 8 mB oxygen and 792 mB nitrogen, including a machine save/load after the seventh fill. |
| Protected seagrass harvesting | Dive Knife right-click harvesting did not consult the ordinary block-break cancellation event. | Checks interaction permission and the NeoForge break event before harvesting. A canceled event leaves the plant intact; an allowed harvest removes it and charges durability once. |
| Client state after respawn | A new local player in the same dimension could inherit the previous computer snapshot and depth-hold prediction. | Clears the old snapshot when either the level or player instance changes. Tested by replacing the actual client LocalPlayer within the same ClientLevel. |

The gas-flow change adds one saved integer, `blendPhase`, clamped to 0–99. Older saves start at phase zero. Wrench cargo also retains the phase. Existing registry IDs, item/map components and network packet shapes remain unchanged; protocol stays **13**. Use this updated build on both ends so the fixes match.

## Executed checks

Runtime versions: Java 21, Minecraft 1.21.1, NeoForge 21.1.238, Create 6.0.10-280, Ponder 1.0.82, Flywheel 1.0.6 and the bundled Illumination API 1.0. These results apply to these versions and fixtures.

| Check | Result and coverage |
| --- | --- |
| Numerical suite | Pass: original 50,500 mixture transactions, all 5,050 valid gas mixtures across repeated small batches, phase-aware partial availability against exhaustive search, ascent grace, tissues, logging bounds, chart traversal and lamp bounds. |
| Dedicated server, optional mods absent | Pass: startup, gas/fluid accounting, recipes, all 16 gear colors, map persistence, signs, logs, actual submerged ascent behavior and all new release-audit scenarios. See [base server results](qa/release-server-base.txt). |
| Adversarial map-bundle inventory | Pass: 3,000 deterministic randomized menu operations covering pickup, shift-click, hotbar swap, pickup-all and drag distribution. Per-map counts, persisted contents and the open bundle's hand lock remain correct. |
| Real Mekanism integration | Pass with Mekanism 10.7.19.85: actual chemical capability, oxygen round trip, simulated transactions without mutation, wrong-gas/side rejection, large quantities and chemical-tag reload invalidation. All new audit scenarios also pass in this environment. See [Mekanism results](qa/release-server-mekanism.txt). |
| Player lifecycle and effects | Pass: death resets exposure; non-death cloning retains exposure while clearing active movement state; diving-effect cleanup preserves independent Night Vision and Slowness effects. |
| Native base client | Pass: same-dimension respawn reset, standard/slim worn trunks, core sign gallery, supplied item models, actual bundle opening/map packets, paging the same open menu and the small empty-bundle screen. See [client results](qa/release-client-vanilla.txt). |
| Iris without a shader pack | Pass with Iris 1.8.8 and Sodium 0.6.13: worn gear, all four live lamp sources and their expected ranges, held depth map and six native screenshots. See [Iris results](qa/release-client-iris-base.txt). |
| Complementary shader pack | Pass with unmodified Complementary Unbound r5.9.3, POTATO preset, Iris 1.8.8 and Sodium 0.6.13. Seven native captures include a nearby receiver and an inactive-lamp control. Each active profile publishes one material light, visibly brightens the receiver and has a distinct beam shape. See [probe results](qa/release-client-complementary.txt) and [on/off comparison](qa/release-complementary-comparison.txt). |
| Assets and packaging | Resource checker validates 630 JSON files, model/texture references and 48 wearable variants. Sign checker validates ten 16×16 symbols plus blank, hard transparency, selective glow, all retained mount models and stonecutter recipes. Final production packaging excludes QA classes and third-party test dependencies. |

Deep Seas oxygen interoperability is covered by tag fixtures; it is not a live Deep Seas installation. The graphical checks use Mesa software rendering. They do not measure normal GPU performance. No audible hardware playtest was possible. External-authentication and upstream shader warnings in the development environment are not treated as passing those services.

## Before the official release

The main remaining acceptance check is a real two-player expedition in the intended server pack. Include cylinder/backup changes, a full descent and ascent with stops, bundle editing/paging and reconnects, death/respawn, and entry/exit through moving Deep Seas/Sable air compartments. Confirm that another player sees equipment and lamps correctly. Fixtures and single-player captures do not replace this check.

Run that expedition with the pack's actual GPU/shader combination and listen to breathing, alarms and water/air transitions. Check the intended minimum NeoForge version separately if the release page promises a version older than the tested 21.1.238.

## Useful final additions

1. **JEI/EMI machine pages.** Crafting recipes exist, but there is no dedicated gas-machine recipe-viewer integration. Showing air separation, blender ingredients, ports and partial fills would reduce the need to consult external documentation.
2. **A short map-bundle/site-marker Ponder scene.** Added in the subsequent [Ponder/interface follow-up](UI_POLISH.md), together with a dive-log tutorial.
3. **Resolve the scope of Cold Sweat thermal equipment.** This remains absent; swim trunks are cosmetic. If thermal wetsuits are a promised 1.0 feature, implement and test them before release. Otherwise state the limitation on the release page and schedule them separately.

The wiki already contains first-dive guidance, backup instructions and computer help. The larger swimming-animation overhaul is documented in [NEXT_UPDATE.md](NEXT_UPDATE.md) and should remain a separate feature update.

## Reproducing the checks

```sh
python3 tools/verify_resources.py
python3 tools/verify_dive_signs.py
bash gradlew build
bash gradlew -Pqa verifyServerProbe
bash gradlew -Pqa -PmekanismQa verifyServerProbe
bash gradlew -Pqa -Pbeta3Qa verifyClientProbe
bash gradlew -Pqa -PworldQa -PgraphicsQa verifyClientProbe
```

Use a disposable QA world. Optional-mod probes require the corresponding unmodified JARs under ignored `qa-libs` directories; they are not distributed with this source or the production mod. Native client probes need a working graphical display. Select and configure the shader pack before running its client probe.

Final production build, checksums and packaging checks: [release-build-and-package.txt](qa/release-build-and-package.txt). Individual new server/client regression outcomes: [release-regression-details.txt](qa/release-regression-details.txt).

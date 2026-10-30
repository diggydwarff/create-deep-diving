# Source guide

The server owns diving state, item contents and machine transfers. Client code renders that state; it does not calculate gas usage or replace the physiology simulation.

| Package | Responsibility |
|---|---|
| `physiology`, `diving` | Pressure, tissues, breathing equipment selection and the authoritative diver update |
| `gas`, `machine`, `menu` | Mixture accounting, gas machinery, storage and machine menus |
| `compat` | Optional dry-compartment queries and tagged fluid/chemical adapters |
| `equipment`, `guideline` | Wearables, tools, lamp profiles and cave guidelines |
| `logbook` | Book data, session lifecycle, statistics and book packets |
| `sign` | Marine pictogram panels, component-preserving stonecutter recipes and validated caption editing |
| `chart` | Loaded-chunk bathymetry, bounded survey traversal, contours and label palette |
| `network` | Client/server payloads, including the shared protocol version |
| `client`, `presentation` | Screens, worn models, visual effects and local lamp rendering |
| `config`, `sediment`, `mixin` | Configuration, sediment simulation and narrow game integration hooks |

`DeepDiving` registers items, equipment variants, machine types, capabilities and event listeners. `src/main/resources` contains production models, textures, tags, recipes and translations. `src/qa` contains opt-in probes and test-only registry/tag fixtures; it is excluded from the release JAR.

## Exploration data flow

### Gas adapters

`GasCompatibility` recognizes common breathing-gas fluid tags and normalizes accepted input to Deep Diving's virtual gas fluids. Machinery retains its existing capacities and side directions. Simulation must not mutate contents. Executed transfers must preserve quantity and reject a different stored gas.

`MekanismCompatibility` loads the optional chemical API through reflection. Its proxy translates the same fluid ports at one chemical unit per mB, including the API's default insertion/extraction methods. The adapter caches chemical matches and misses until a tag reload. Avoid linking Mekanism classes into production signatures: startup without Mekanism is a supported path.

### Dive books

`DiveLogService` owns the book's `CUSTOM_DATA.deepdiving_log` compound. Each book has an identity, lifetime dive counter, optional current session and at most 32 completed records. A session belongs to one player and dimension.

`DiveLogStatistics` integrates the existing `DiverState` at one-second samples and final closure. It bounds elapsed time, records the final partial second once and keeps wet time separate from total time. Real gas debits call `gasUsed`; the book does not infer consumption from changing tank contents.

`DiveLogPackets` accepts only held-book identity and input intent. The server validates the book and computes the response. A response explicitly distinguishes opening a book from refreshing an existing view. `DiveLogScreen` accepts refreshes only for its book and hand.

### Depth charts

`DepthMapItem` uses vanilla map IDs, saved data, decorations and update packets. `depth` reads maintained surface/floor height maps from already loaded chunks. A return value of `-1` means unavailable; `0` means land/no surface water. It does not scan blocks down a column or load distant chunks.

`DiveSiteSignBlockEntity` retains vanilla sign text/editing. Site signs are represented as native banner markers, so their names use normal map save/load, packet and item-frame paths. `DiveChartData` marks depth-chart saved data; its mixin retains that identity and markers through zooming/locking. Narrow menu hooks admit initialized depth maps and route shift-click correctly. See [site-map notes](MAP_SITES.md).

`ChartMath` defines the 64-sample per-player tick budget, complete pixel traversal, contour intervals and numerical glyphs. Two distinct held maps split the budget. `ChartPalette` chooses vanilla map colors and contrasting label ink. Keeping palette and numerical helpers separate allows the QA fixture to use the real rendering rules.

### Wearables and lamps

The five 16-color equipment families reuse the established equipment behavior. `SwimsuitLayer` follows the player's leg model; fins and masks retain their existing worn rendering paths. Regenerate resources with `tools/generate_gear.py` and `tools/generate_exploration_assets.py`, then run the resource checker.

`LampProfile` contains the beam differences. Server equipment state selects the profile; `DivingLampLights` turns it into local light frames and smoothly changes held/stowed position. `LampRenderer` uses the existing Illumination/shadow pipeline, including sediment attenuation and nearby-light limits. Add profiles through this shared path rather than a second render pass.

Pictogram artwork and generated models are described in [DIVE_SIGNS.md](DIVE_SIGNS.md). Panels share one unticked block entity and renderer; named site panels reuse the existing banner-marker path.

See [AUDIT_NOTES.md](AUDIT_NOTES.md) for commands, checked cases and remaining integration limits.

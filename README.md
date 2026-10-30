# Create: Deep Diving — 1.0.0-rc.1

Release candidate 1 for NeoForge **Minecraft 1.21.1**. Install matching builds on **both client and server** with **Create 6.0.10** and its normal required dependencies. Built against NeoForge 21.1.238; the declared minimum is 21.1.219. Java 21 is required. This is a release candidate. See [RC1 changes and validation](docs/RELEASE_CANDIDATE_1.md), [release notes](CHANGELOG.md), [beta checks](docs/BETA_3.md) and [the next update plan](docs/NEXT_UPDATE.md).

The complete [player and server wiki](WIKI.md) covers first dives, equipment, gas handling, recipes, maps, log books, configuration and troubleshooting.

This is the Phase 1 implementation. Diving bells, hoses, rebreathers and atmospheric diving suits belong to later updates.

## Exploration update

- Optional Create Deep Seas oxygen-fluid and Mekanism chemical connections for the existing gas workshop.
- All 16 dye colors of fins, combined snorkeling masks and above-knee swim trunks.
- A Dive Log Book with manual start/end, computer telemetry, saved history and automatic session closure.
- A Depth Map with water-depth bands, 10-block contours, numerical soundings and four zoom steps.
- Dive Site Signs with named markers on terrain/depth maps, plus cartography-table zooming, copying and locking.
- Ten 16×16 marine pictogram panels plus a blank panel, optional captions, arrows, glow ink and stonecutter design selection.
- A Dive Map Bundle holding 16 depth maps with previous/next pages and real map storage.
- Wide-beam, long-throw and close inspection lamps using the existing local-light renderer.

See [EXPLORATION_UPDATE.md](EXPLORATION_UPDATE.md) for controls, recipes, gas connections and validation, [dive sign panels](docs/DIVE_SIGNS.md) and [site-map notes](docs/MAP_SITES.md) for the new tools, and [review notes](docs/AUDIT_NOTES.md) for the preceding audit. Existing equipment IDs and diving physiology are preserved. This revision uses network protocol 14 and requires matching builds on client and server.

## Included

- Local water depth, configurable reference-ocean pressure curves and per-dimension overrides. Linear mode still defaults to one additional atmosphere per 10 blocks.
- Pressure-scaled consumption for Create backtanks and new cylinders, with one system owning each breath.
- Air, Nitrox, Trimix, Heliox and custom mixtures in 1% increments.
- Four nitrogen and helium tissue compartments, residual loading, decompression ceiling and no-decompression time.
- Progressive hypoxia with curved eyelid blinks, tunnel vision, sluggish movement, camera drift, muffled environmental sounds and gradual blackout recovery.
- Combined world shaders for hypoxia, narcosis, oxygen toxicity, decompression sickness and pressure injury. Smooth onset/recovery, clear OLED/menu text, reduced motion and an overlay fallback.
- Separate pressure injury, red mask flashes, blood/bubble clouds and optional fatal sudden exposure; nitrogen narcosis, oxygen toxicity and decompression sickness.
- Advanced, high-capacity and emergency cylinders that can be worn or placed; a diving computer and lamp with local illumination.
- Rotational compressor, separator and blender; gas storage; Create pipe/tank and hopper automation.
- Engineer's Goggles information, a Display Link source, recipes and seven Ponder tutorials.
- Server-owned simulation, equipment data components, saved physiology and machine contents.
- Spacious machine screens with high-contrast inset readouts, Create borders/buttons/inventory and a compact fallback for small GUI sizes.
- A black OLED-style diving computer and HUD, large white depth/pressure readings and paged warnings.
- Server-calculated gas time, an ascent-limit bar, forgiving sustained-ascent warnings, quiet configurable computer alarms, and a separate recovery guide.
- A next-stop countdown, shorter configurable decompression stops, and remappable depth hold (V by default).
- Colored cave guidelines with anchor nodes, individual glow-ink coating and cutting.
- Diving fins with gentle stopped-swim buoyancy and an aquatic dive knife.
- Equipment onboarding and four preparation/diving advancements.
- Named Hypoxia, Narcosis, Oxygen Toxicity, Decompression Sickness, Pressure Injury and Blackout effects, with vanilla symptoms derived in the background.
- Extended, twin and compact cylinder models assembled directly from Create's installed backtank models, including worn player rendering.
- Arcade, Standard and Hardcore presets and server/client configuration.

## Start playing

1. Make a **Gas Compressor**. Connect rotational power to its **rear shaft**, at least 16 RPM. Keep the block above its intake free of water. A redstone signal pauses powered machines.
2. Insert an empty cylinder into its **input slot**. It fills with air; the finished cylinder moves to the **output slot**. A hopper underneath extracts finished cylinders.
3. For other mixtures, pump compressed air into the **top of a Gas Separator**. **Oxygen exits the blue-marked port** and **nitrogen exits the green-marked port**. Both outputs need free space.
4. Pipe oxygen into the **blue-marked port of a Gas Blending Station**, nitrogen into the **green-marked port**, and helium into the **top**. Helium cartridges can also go directly into the station's cartridge slot.
5. Obtain helium cartridges through the heated Create mixing recipe: gold ingot, amethyst shard, blaze powder and ender pearl. One cartridge produces 1000 mB helium. Store helium in a separate gas vessel or Create tank.
6. Select a preset or adjust oxygen and helium. Nitrogen fills the remainder. Hover the mixture/depth row to see constituent reserves and ports. A cylinder with another mixture must be **purged first**, which vents its existing gas.
7. **Use a filled cylinder in the air to wear it in the chest slot, then wear a Create diving helmet.** The helmet draws gas directly from the cylinder. Carry a Diving Computer in the normal inventory or offhand for readouts. No Curios dependency is required.
8. **Sneak-use a spare cylinder in the air** to enable its inventory backup. Backups are disabled initially. The worn cylinder has priority; enabled inventory spares are selected in inventory order.
9. Carry a **Diving Computer** for the HUD and use it to open the full readout. Watch depth, gas, ascent rate, gas suitability and decompression ceiling.
10. If a ceiling appears, stay at least that deep, keep a suitable breathing gas available and wait for the ceiling to fall. Surface air also gradually clears tissue loading, but violating the ceiling accumulates sickness.

Cylinders carry 12000, 24000 and 2000 mB of surface-equivalent gas. Their default pressure ratings are 12, 21 and 12 atmospheres respectively; all ratings can now be changed in the server config. Default surface use is 8 mB/s, multiplied by pressure. Copper Create backtanks retain their native air units, consumed at approximately one unit/s multiplied by pressure. Existing copper helmets, backtanks and boots remain usable; native helmet behavior in lava is left to Create. Create's enhanced underwater helmet visibility is disabled by default; set `createHelmetNightVision = true` in `deepdiving-server.toml` to restore it. In Create 6 this is a longer underwater fog distance, not a Night Vision potion. Ordinary potions are unaffected.

Use a cylinder on a block to place it. All three types also work underwater. Empty-hand right-click picks it up and wears it if your chest slot is empty; sneak-wrenching collects it into your inventory. Mining, pickup and world save/load retain its gas mix, fractional quantity, backup preference and custom name. Placed cylinders are equipment storage, not automatic refill machines; fill them using the compressor or blender.

A lamp carried in inventory produces downward illumination; holding it in either hand aims the beam with the player. Right-click toggles it. The bundled Illumination API provides local light and shadows without replacing world blocks. Wide-beam, long-throw and inspection lamps share the same renderer, nearby-light limits and sediment attenuation. See the exploration notes for profiles and recipes.

## Diving status effects

The inventory and HUD show named diving conditions, rather than separate Slowness/Mining Fatigue/Nausea/Weakness/Blindness entries for the same condition. They persist while the underlying condition persists and clear or downgrade as you recover. Mining, nausea and blindness use derived vanilla effect queries; movement/attack penalties use a single owned modifier, accounting for independently applied vanilla potions. Milk can clear the icons temporarily, but does not reset physiology; an ongoing condition returns on the next simulation update. `/deepdiving reset`, disabling simulation, Spectator and Creative without a diving helmet clear the addon's own conditions without removing unrelated potions.

The computer is a 320×260 OLED-style readout, with a 320×236 compact layout for small GUI heights. LIVE shows depth, pressure, gas time at the current depth, gas quantity/composition, no-decompression time or stop depth, and ascent guidance. The ascent marker uses the server's configured limit. Arrow buttons page through every warning. HELP shows each condition's cause, current reading and recovery advice; compact help pages show one condition at a time. SND/MUTE toggles separate computer alarms. The HUD shares the black/white/amber style. Machine screens retain their spacious and compact layouts. Text is not shrunk to fit.

## Cave diving tools

Place **Guide Line Nodes** on solid floors, walls or ceilings, including underwater. Each node is a tiny metal pin with a three-pixel head and matching line attachment. Craft a white guideline spool from string, then dye it any of the 16 colors. Use the spool on a first node, then a second node to connect them. One spool makes one segment up to **64 blocks** long; each node accepts up to eight connections. Sneak-use a spool in air to clear a mistaken first selection.

Aim at a line segment and right-click with a **Glow Ink Sac** to give that individual line a faint glow in darkness. Aim with a **Dive Knife** or shears to cut a segment and recover its spool. Dye and glow are retained on that recovered spool. Removing an anchor detaches its connections; guide data persists per dimension without requiring both endpoints' chunks to remain loaded. Guidelines are navigation aids and do not block movement. They anchor to fixed world terrain; moving submarine/sublevel attachments are not provided.

Wear **Diving Fins** in the feet slot. They add an **8% swim-speed boost** and keep you in the swimming pose when you stop after swimming. Stopping gives gentle neutral buoyancy; **Space/jump** releases that pose and lets you rise, while **sneak** permits descent. Resume sprint-swimming to engage the pose again. Walking on land or the bottom is 25% slower. Fins provide no armor protection and replace Create diving boots.

Fins and V depth hold have a slow, visible bob and soft rest-position correction. Directional input allows normal horizontal travel immediately. The bob target moves up to 0.08 blocks each way over a ten-second cycle, with upward movement limited by the current decompression ceiling. Neither system advances you to a shallower stop automatically. Rest correction is capped at 0.5 blocks/second. Passive fins recenter near your physical height instead of chasing a rest target more than half a block away; they release rather than force a multi-block dive to a deeper stop. See [bubble and buoyancy refinements](docs/BUBBLE_BUOYANCY.md). While stopped, gentle alternating foot kicks and free-hand sweeps ease in to suggest treading against the current. Movement releases that idle animation; held items, attacks and item-use poses remain authoritative. Nearby players also receive the buoyancy animation state. Worn fins have ribbed blades, rubber pockets, heel straps and small brass buckles, with dedicated 16×16 rubber textures, teal highlights, dark recesses and strap shading.

The **Dive Knife** is a modest iron-tier weapon with **+3 damage against aquatic mobs**, including guardians and drowned. It cuts kelp, vines and cobwebs quickly and shears seagrass underwater. Use it on underwater seagrass, or mine the grass, to collect it while retaining water.

## Machinery and automation

| Machine | Shaft | Fluid ports | Inventory |
|---|---|---|---|
| Compressor | Rear | Compressed air out on other faces | Cylinder in, filled cylinder out |
| Separator | Rear | Air in top; O2 out blue port; N2 out green port | No cylinder slots |
| Blender | Rear | O2 in blue port; N2 in green port; He in top | Cylinder in, filled cylinder out, helium cartridges |
| Gas vessel | None | One gas in/out on every face | Helium cartridges |

Ports rotate with the machine. Engineer's Goggles show their exact world directions. The rear drive has a visible shaft socket; placing a machine beside an existing motor/shaft automatically aligns the rear unless you are sneaking. All gases use normal fluid capabilities and can move through Create pumps, pipes and fluid tanks. They are virtual fluids and do not spill into the world. Mixtures live in cylinders, so there is no separate fluid for each recipe.

Production machines can exchange gas directly with compatible adjacent containers. For a separator, put a vessel containing compressed air on its top inlet and empty vessels against its blue/green outputs. For longer routes, use **powered Create pumps** with their arrows pointing from source to destination. Empty storage vessels do not generate compressed air: supply it from a compressor. The separator now reports missing air and full oxygen/nitrogen buffers separately.

Vessels hold 64000 mB; machinery buffers hold 16000 mB. Gas-vessel pressure is a simple displayed fill gauge, rated to 200 atm. Ordinary breaking drops inventory and vents gas. **Sneak-wrench pickup preserves gas and inventory**, which are restored on placement. Rotating a machine invalidates its cached capability ports.

Wear Engineer's Goggles for tank/mixture/status information. Attach a Create Display Link and select **Diving gas status**. Hold the normal Ponder key over a machine, cylinder, computer, depth map, map bundle, log book or site-sign item for its tutorial. The **Deep Diving** Ponder index entry groups them together. Standard crafting, mechanical crafting and Create mixing recipes use their native serializers; a custom JEI/EMI machine-category plugin is not included.

## Configuration

The dive-computer HUD is compact by default. Under **Mods → Create: Deep Diving → Config → Client settings**, choose **HUD size** (75%, 100%, 125% or 150%) or turn **Compact HUD** off for the expanded panel. The HUD offsets still control its position; its size setting only affects this HUD.

Open **Mods > Create: Deep Diving > Config** for the nautical settings screen. Client settings cover the HUD, lamps, sound and effects; world settings cover diving rules and integrations. See [config screen notes](docs/CONFIG_UI.md).

Server settings: `config/deepdiving-server.toml` on NeoForge 21.1.238, with optional world overrides in `<world>/serverconfig/deepdiving-server.toml`. Earlier NeoForge builds may store server settings directly in that world folder. Client settings: `config/deepdiving-client.toml`.

Existing worlds retain `pressureMode = "LINEAR"` and `blocksPerAtmosphere = 10.0`. To make progression fit 200-block oceans, set these top-level server entries:

```toml
pressureMode = "REFERENCE"
referenceOceanDepth = 200.0
referencePressure = 21.0
pressureCurveExponent = 1.35
```

`referenceOceanDepth` is **blocks**, not a percentage or a detected seabed. Players at the same fraction of that depth encounter the same pressure, whether the world has 40-, 200- or 600-block oceans. `referencePressure` is the pressure at 100% of that reference depth. An exponent of 1 gives a straight curve; 1.35 makes the shallows gentler. Pressure continues above the reference value in deeper trenches, with a defensive simulation ceiling of 1000 atm. Actual depth in the HUD remains blocks below the surface.

For different dimensions, use overrides instead of changing the global defaults:

```toml
dimensionPressureProfiles = [
  "minecraft:overworld|REFERENCE|200|21|1.35|AUTO",
  "yourmod:deep_ocean|REFERENCE|600|21|1.35|256",
  "yourmod:shallow_sea|REFERENCE|40|21|1.35|AUTO"
]
```

Replace the example mod/dimension IDs with your real IDs. Format: `dimension|mode|scale|reference atm|exponent|surface`. In REFERENCE mode, scale means reference depth; in LINEAR mode it means blocks per atmosphere. `AUTO` measures the local water surface. A numeric surface is a fixed Y coordinate for roofed oceans or flooded dimensions. Global `surfaceMode = "FIXED"` and `fixedSurfaceY` offer the same option. The last duplicate dimension entry wins. Invalid/nonfinite profiles are rejected by config validation. The seabed under the player never changes pressure.

All cylinder/Create-backtank pressure ratings, the unprotected game threshold, oxygen partial-pressure limits, narcosis threshold, hypoxia depletion and recovery rates are configurable. Gas depth tooltips, blender ranges and decompression ceilings all invert the same pressure curve, including dimension overrides. Defaults retain existing gear/gas balance. The oxygen reserve meter is a game resource, **not a real blood-oxygen measurement**.

Sudden compression uses `suddenPressureInjury`, `pressureShockDelta`, `catastrophicPressureExposure`, `catastrophicPressureDelta` and `catastrophicPressureExcess`. Defaults allow an extreme unprotected jump from dry air to deep water to inflict fatal damage. This is cinematic pressure injury, not literal whole-body implosion or a ragdoll system. Worn rated equipment protects independently of remaining gas; enabled inventory backups provide breathing gas but do not protect an unequipped body. Joining, dimension changes and pressure-profile edits establish a fresh transition baseline, preventing fabricated sudden shocks; sustained over-rating exposure still causes injury. Pressure injury persists and recovers gradually in safe pressure.

Client controls include `visualAndSoundEffects`, `effectIntensity`, `eyelidBlinks`, `pressureBloodOverlay`, `cameraSway` and `distressSounds`. Server `pressureBloodParticles` controls shared blood clouds. Turning cosmetic effects off preserves server gameplay, including derived blindness/slowness at blackout. Blackout begins below 20 reserve and ends after recovery to 35, avoiding threshold flicker. Inventory stays accessible for cylinder changes.

Alpha.4 adds client `[shaders]` controls for quality, intensity, calmer central vision and each condition, plus `reducedMotion`. The GPU pass affects the completed world before any HUD/menu is drawn. Active external shader packs use the overlay fallback by default; no Iris/Oculus dependency is required. Custom optical symptoms replace only this addon's derived nausea when available; independent vanilla nausea remains active. The preview commands below exercise these effects without changing physiology.

Try `/deepdivingvisual preview narcosis 3`, `/deepdivingvisual preview combined 3` or `/deepdivingvisual preview blackout 3`. These are **client-only visual demos** lasting 60 seconds; `/deepdivingvisual stop` ends them. They do not change gas, health, potions or server physiology.

`decompressionRate = 6.0` speeds off-gassing independently of tissue uptake. Set 1.0 to restore the previous pace. The default simulated five-minute air dive at 40 blocks requires 32 seconds of stops instead of 189, excluding ascent time. `allowDepthHold = true` enables the remappable V control with a computer, diving equipment and available breathing supply. Jump, sneak or press V again to release; holding never advances to the next stop automatically.

Other settings cover master simulation, physiology, Create backtank ownership, Water Breathing, difficulty, gas consumption, tissue speed, damage, ascent speed, bounded surface scans and HUD placement. The pressure-profile examples above use those same server settings.

Ascent warnings now default to a sustained speed above **2.5 blocks/second**, for **three consecutive seconds**, with at least **four blocks gained**. Brief depth adjustments do not immediately warn. Slowing, stopping or descending resets the warning history. Decompression ceilings remain in force. Existing worlds keep their saved ascent limit: update `maximumAscentBlocksPerSecond` to `2.5` to adopt the new balance. See [ascent settings](config-examples/ascent-settings.toml).

With Water Breathing or Conduit Power, the default bypass preserves cylinder gas and avoids loading compressed gas. It does not erase prior decompression obligations. Death resets physiology; ordinary logout and End return retain it. Spectator and Creative without a diving helmet bypass the simulation.

Crush Depth is detected through installed-mod metadata and suppresses this addon's equipment-pressure damage by default. Other pressure-owning mods can be added to `externalPressureOwners` or managed through the independent damage/backtank switches. A guarded optional Create Deep Seas bridge queries its public dry-compartment API when available. A dry sealed compartment uses surface breathing pressure; leaving it for exposed water starts normal depth/pressure simulation and can cause compression injury. The bridge does not equate hull pressure with breathing pressure, change submarine oxygen, or require Deep Seas/Sable classes at startup. Missing/changed APIs fall back safely to ordinary water detection. Live moving-submarine tests remain necessary. Aeronautics/Sable, CC: Tweaked and Fluidworks are not required.

## Operator testing commands

- `/deepdiving status` — current server-calculated diver state.
- `/deepdiving reset` — reset your physiology for repeatable testing.
- `/deepdiving fill 21 0` — fill the cylinder in your main hand with air.
- `/deepdiving fill 32 0` — fill with Nitrox 32.
- `/deepdiving fill 18 45` — fill with Trimix 18/45.
- `/deepdiving fill 21 79` — fill with Heliox 21/79.

The two numbers are oxygen and helium percentages. Their sum must not exceed 100. All commands require operator permission level 2.

## Build and checks

```sh
./gradlew build
./gradlew simulationTest
./gradlew -Pqa verifyServerProbe
```

The server probe needs the ordinary development server EULA/configuration setup. It uses an isolated **test world** and exits after writing `run/server/qa-results.txt`; do not point it at an important world. QA sources are excluded from the distributed JAR. `-Pqa verifyClientProbe` runs the automated item/journal probe without a world. The separate integrated-world probe uses a disposable save named `exploration-audit`; setup is in the review notes.

`-Pqa -PshaderQa runClient` runs the separate native shader probe. It checks real GPU passes, unchanged HUD/menu pixels, condition controls, recovery, resize, reload, vanilla-effect coexistence and a deliberately missing shader resource. It uses a temporary resource pack for that failure test, then restores the prior selected packs.

See [review notes](docs/AUDIT_NOTES.md) for validation and [source guide](docs/ARCHITECTURE.md) for implementation responsibilities.

## Current limits

- This is a beta for gameplay testing and balancing. Tissue kinetics are deliberately simplified game simulation, not medically validated dive planning.
- Open-water depth is tested. Roofed/connected caves, artificial air pockets and moving sublevels need broader compatibility testing; scanning is bounded and uses the local water column.
- Cylinders render in hand, inventory and on normal/slim player backs using Create's actual backtank model. Extended and twin models transform that model at runtime; no upstream textures or geometry are bundled.
- Lamps use local rendered light and shadows. Optional shader packs and high-latency movement still need broader player testing. Audio reuses Minecraft drowning, heartbeat and glass sounds rather than new recordings. Environmental muffling covers eligible one-shot sounds; existing loops/music/records are left alone.
- The gas storage pressure gauge is informational; a separate gas-pressure pipe network is not simulated.
- No direct Sable physics or CC peripheral integration, custom JEI/EMI machine categories, hose system, rebreather, diving bell or atmospheric suit is included in this phase. Deep Seas support includes tagged oxygen-fluid exchange and the optional dry-compartment query; a running Deep Seas/Sable installation still needs a live modpack test.
- Long multiplayer expeditions and optional-mod combinations still need player testing.

All UI textures, widgets and referenced Create block textures are loaded from the installed Create mod, not bundled copies. Code is MIT licensed; upstream Create resources retain their own license.

The gas bridge and remaining Deep Seas testing limits are described in [exploration notes](EXPLORATION_UPDATE.md#gas-connections).

## Swimming cinematics

While in water, the perspective key (**F5** by default) cycles first person → rear view → front view → **orbit view** → first person. If another mod adds camera views, those views remain in their existing order and orbit comes just before the cycle returns to first person. Mouse movement in orbit rotates only the camera; your diver keeps the same heading and pitch, and movement/depth hold keep working. Scroll zooms between 1.5 and 12 player-scale blocks without changing the selected hotbar slot. The normal camera collision rays keep the view out of walls. Press the perspective key again to return to first person; normal aiming resumes from the diver's unchanged heading. Leaving water, entering a confirmed dry compartment, dying, changing camera entities or replacing the level clears orbit. Menus do not consume orbit mouse input. F1 hides the HUD for shots as usual.

Set `swimmingOrbitCamera = false` in the client config to keep your existing perspective cycle, including any views added by other mods. Orbit does not move the diver, move a detached entity, or send camera rotation to the server. Held lamps and interactions keep following the diver's actual look direction. Existing camera roll/sway contributions remain independent; camera/shader mod combinations still need playtesting.

Supplied divers now release mouth-area **breathing bubbles** in short exhalation bursts, roughly every 3.6 seconds. A stronger opening puff leaves the mouth, followed by a sparse trail. Individual bubbles rise at varied speeds and fade gradually near the surface, visible to nearby players. They do not charge extra gas. Empty supplies, removed helmets, dry eye/mouth positions, dry compartments and magical breathing bypass stop the exhaust. Enabled emergency supplies can continue it. Set `breathingBubbles = false` in the server config to disable the effect.


## Creative diving

Wear a **Create diving helmet** to enable diving simulation in Creative. With a filled cylinder or Create backtank, gas drains normally and the computer tracks pressure, tissues, decompression and warnings. Condition effects and computer alarms work too. Fins and V depth hold work while swimming with the helmet; V still requires a computer and available breathing supply.

Removing the helmet returns Creative to its normal behavior. Creative flight releases fin buoyancy and depth hold without changing flight controls, and Creative damage immunity remains intact. Water Breathing/Conduit bypass and all existing server settings still apply. Survival and Adventure continue to simulate diving without this Creative-only opt-in.


Source layout and commenting conventions are described in [code style](docs/CODE_STYLE.md).

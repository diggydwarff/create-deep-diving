# 1.0.0-beta.10 — Clearer implementation

- Broke the long diver update into straightforward environment, gas consumption, physiology, decompression-readout and warning helpers, preserving update order and the actual inhaled mixture during backup handoff.
- Replaced nested condition/alarm thresholds and pressure-damage expressions with named decisions and ordinary branches. Split visual layers and computer panels into focused drawing functions.
- Replaced unclear local names in core state, breathing, configuration, depth and geometry code. Documented the guideline intersection math, visual draw order and ownership rules.
- Expanded chained reset assignments, reflowed awkward comments and retained the generous declaration spacing and four-space layout throughout the source.
- This remains a readability update: tuning, recipes, assets, save keys and protocol 6 are unchanged.

# 1.0.0-beta.9 — Source readability

- Applied a consistent four-space layout across all 93 production and QA Java files, with extra blank lines between declarations and logical steps.
- Split combined variable declarations, expanded compressed methods/statements and added braces around control-flow bodies.
- Added comments explaining important state, breathing-supply, buoyancy, guideline and machine synchronization decisions. Simplified qualified type names in core diving code and spaced out the Gradle build configuration.
- This is a source readability update. Gameplay, tuning, assets, saved data and network protocol remain unchanged.

# 1.0.0-beta.8 — Creative diving

- Wearing a Create diving helmet now opts Creative players into normal gas consumption, physiology, pressure/decompression tracking, condition effects and computer alarms. Removing the helmet disables the simulation for Creative again.
- Enabled fins and V depth hold for equipped Creative swimmers. Creative flight immediately releases both buoyancy modes without changing flight abilities or motion.
- Managed helmet breathing now distinguishes Creative invulnerability from actual magical water breathing, allowing empty supplies to deplete the breath reserve and cause hypoxia. Configured magical bypass remains intact.
- Preserved Creative damage immunity, normal ungeared Creative behavior, Spectator exemption and Survival/Adventure mechanics. No new config or network format is required; protocol remains 6. Update client and server together for Creative support.

# 1.0.0-beta.7 — Camera cycle crash fix

- Fixed the `MatchException` in `OrbitCamera.nextPerspective` when pressing F5 with extra camera types, as seen in the supplied Sable/Aeronautics modpack log. The camera bridge no longer switches exhaustively over the three vanilla enum values.
- Preserved additional camera views in their existing order. Underwater orbit is inserted when the host cycle wraps back to first person; the next press exits orbit to first person. Disabled/ineligible orbit passes the existing cycle through unchanged.
- Added regression coverage for extra incoming/current camera views, complete extended cycles, orbit exit and disabled behavior.
- Protocol remains 6. This fixes client camera behavior; beta.6 servers remain compatible. Textures, diving balance, bubbles and buoyancy are unchanged.

# 1.0.0-beta.6 — Swimming cinematics and bubbles

- Increased V/fin rest bobbing to a 0.30-block amplitude over eight seconds, making it look like slow wave motion while respecting decompression ceilings and ascent limits.
- Added an optional fourth underwater perspective after the normal front view: orbit around the diver with the mouse while swimming direction stays unchanged. The remappable perspective key enters/exits; scroll zooms without switching hotbar slots.
- Reused vanilla camera interpolation, entity scaling and wall collision. Orbit clears on leaving water/dry-compartment entry, death, camera changes, relog or level replacement. Mouse sensitivity, invert-Y and existing camera roll/sway remain available.
- Added client setting `swimmingOrbitCamera`, default true. First-person/rear/front behavior remains ordinary outside water or when disabled.
- Added server-authoritative regulator-area bubble bursts for supplied divers, staggered roughly every 3.6 seconds and visible to nearby players. Vanilla bubbles rise and linger as the diver moves past them. Exhaust reads the supply without consuming extra gas and stops for empty supplies, missing gear, dry positions/compartments and magical breathing bypass.
- Added server setting `breathingBubbles`, default true. Emergency backups can continue exhaust.
- Protocol remains 6 and all existing item/effect textures are preserved. Install beta.6 on client and server for both additions.

# 1.0.0-beta.5 — Fin textures and helmet visibility

- Added dedicated 16×16 pixel textures for worn fin blades, foot pockets, raised ribs and straps, with clearer teal highlights, rubber recesses and shading. Supplied inventory icons remain unchanged.
- Added server setting `createHelmetNightVision`, default false. It gates Create 6's enhanced underwater helmet fog distance, preserving Night Vision potions, other fog handlers, lamps and netherite helmet lava vision. The master switch restores native Create behavior when the addon is disabled.
- Inspected the provided Deep Seas 3.3.0 JAR: its exact sealed-compartment query matches the existing guarded adapter and rejects actual plot water. Documented a minimal cabin/flooding/airlock integration and its remaining live validation. No new Deep Seas dependency or unverified chamber-pressure behavior was added.
- Protocol remains 6; install beta.5 on both client and server for the new client visual policy.

# 1.0.0-beta.4 — Diving presentation polish

- Grounded placed cylinders at their scaled base, fixing the emergency cylinder floating above the floor.
- Replaced the large guide-node bracket with a tiny metal pin at native Minecraft pixel scale. Updated hitboxes, line attachment points and shallow sag so floor lines stay above the ground.
- Increased the slow buoyancy bob to a 0.16-block amplitude over twelve seconds for both fins and V depth hold, still limited by the decompression ceiling.
- Added smooth stationary foot flutter and free-hand treading strokes. Travel, item use and attacks keep their own poses. Nearby players receive the active buoyancy pose on changes and when they start tracking a diver.
- Detailed worn fins with textured rubber pockets, raised blade ribs, heel straps and small brass buckles; retained the normal and slim player models.
- Installed the supplied 16×16 fins, guideline bundle and dive-knife item textures unchanged.
- Bumped the network protocol to 6 for observer animation state. Install beta.4 on clients and server; existing equipment and physiology saves remain readable.

# 1.0.0-beta.3 — Cave diving equipment

- Added waterloggable guide-line nodes for floors, walls and ceilings, plus all 16 dyed guideline spools. Use a spool on two nodes to connect a segment up to 64 blocks long; nodes support up to eight links.
- Added per-segment glow-ink coating and knife/shears cutting. Refunded spools retain their color and glow; a dimension-local saved graph prevents duplicate refunds from unloaded endpoint copies.
- Added wearable diving fins with an 8% swim-speed boost, gentle neutral buoyancy when stopped, retained swimming pose and a 25% walking penalty. Space releases the pose and buoyancy; sneaking permits descent.
- Added a tiny, slow bob and bounded rest-position sway to fin buoyancy and V depth hold. Horizontal input resumes normal travel; bobbing never crosses a decompression ceiling.
- Added a dive knife with +3 damage against aquatic mobs, quick plant/cobweb cutting, underwater seagrass shearing and guideline cutting.
- Extended the held lamp into a longer forward beam that stops at solid walls. A lamp carried in inventory retains its small nearby light pool. Shared-point cleanup preserves water and player placements.
- Reserved proper heading space for Create's goggles icon in machine popups.
- Added crafting recipes, item/foot models and new pixel sprites. Bumped network protocol to 5; install beta.3 on both clients and server.

# 1.0.0-beta.2 — Placeable cylinders, connections and local lamp light

- Made all three diving cylinders placeable and waterloggable. Empty-hand pickup can equip them; mining and sneak-wrench pickup preserve their mixture, fractional gas, backup preference and custom name.
- Fixed missing machine shafts under Flywheel, changed to visible rear half-shafts, and added rear socket housings and motor-aware placement. Storage vessels retain their non-kinetic behavior.
- Added bounded automatic transfer between production machines and directly adjacent gas containers. Create pipe routes still use powered mechanical pumps.
- Replaced the separator's ambiguous gas/output-space message with specific missing-air, oxygen-full and nitrogen-full messages. Goggles show port directions; machine help refers to the colored ports.
- Changed the lamp from Night Vision to a small pool of real level-10 light. Light follows the diver and expires when no longer needed; water is retained and overlapping lamps share their light point safely. No dynamic-lighting dependency is required.
- Added a minimum-depth warning for hypoxic cylinder mixtures.
- Kept protocol 4 and existing equipment/physiology saves. Install this build on both client and server for the new placed-block registry entries.

# 1.0.0-beta.1 — Shorter stops and depth hold

- Added a server-calculated NEXT countdown to the computer and HUD for the next shallower decompression stop at the current depth and gas.
- Added independent `decompressionRate` configuration, default 6.0, accelerating off-gassing without accelerating uptake. A simulated five-minute air dive at 40 blocks now needs 32 seconds of stops instead of 189, excluding ascent time.
- Added remappable depth hold (V by default), requiring diving equipment, an available breathing supply and a carried computer. Horizontal swimming remains available; jump, sneak or the hotkey releases it.
- Hold targets are server-selected, stay within the configured ascent limit, and release on lost equipment/supply, water exit, blackout, teleport or dimension/profile changes. Hold never automatically advances to a shallower stop.
- Improved no-decompression forecasts for gas switches that unload one inert gas while loading another, and for custom world pressure curves.
- Bumped network protocol to 4. Install matching beta.1 builds on clients and server; existing cylinder and physiology saves remain compatible.
- Includes the supplied item/effect icons and Create material/UV cleanup from the texture updates below.

# 1.0.0-alpha.5-textures.2 — Final texture cleanup

- Matched the gas storage vessel to the other machines with Create copper plating, industrial iron bands, an andesite base and a correctly mapped dial.
- Removed coplanar clamp surfaces to prevent texture flicker.
- Normalized the breathing regulator to a crisp 16x16 transparent sprite while keeping its brass/dark-metal design.
- Matched the helium cartridge to Create's industrial iron and brass materials, with a muted helium band and corrected UVs.
- Normalized the Unix Gradle wrapper's line endings and made it respect JAVA_HOME.
- Preserved all other supplied equipment/effect PNGs and all gameplay behavior.

# 1.0.0-alpha.5-textures.1 — Texture refresh

- Replaced the regulator, diving computer and diving lamp items with the supplied pixel textures.
- Added all six supplied condition icons, using Minecraft's effect atlas in both the inventory and HUD.
- Reworked compressor, separator and blending station materials/UVs around Create's copper backtank, industrial iron, casings, gauge and pipe textures.
- Replaced large concrete color bands with metalwork and small muted gas labels; corrected overlapping band surfaces.
- Visual update only: gameplay, saved data and network protocol remain compatible with alpha.5.

# 1.0.0-alpha.5 — Computer tools and equipment onboarding

- Added server-calculated gas time for the active supply, using actual depth/configuration and gas consumption.
- Added an ascent-limit bar and plain stop/ascent guidance to the computer and HUD.
- Refined the OLED layout with LIVE/HELP pages, reading tooltips and a compact layout without smaller text.
- Added quiet edge-triggered computer alarms, cooldown/volume configuration and a saved mute control.
- Added condition causes, current readings and recovery guidance in HELP and inventory effect tooltips.
- Added an equipment Ponder tutorial and four preparation/diving advancements.
- Made the computer immediately follow backup mixture/capacity after cylinder depletion.
- Bumped the network protocol to 3; install matching alpha.5 on clients and server. Existing saved equipment and physiology remain compatible.

# 1.0.0-alpha.4 — Condition shaders

- Added a combined world-only shader with progressive desaturation/peripheral blur for hypoxia, slow optical drift/double vision for narcosis, unstable focus for oxygen toxicity, intermittent focus loss for decompression sickness, and distortion/colour separation for pressure injury.
- Preserved curved eyelids, complete blackout, blood overlays and sharp OLED/menu text.
- Added smooth severity transitions, low/high quality, individual condition toggles, shader intensity, central clarity and reduced motion.
- Kept other post effects independent; missing shader resources and active external shader packs use condition overlays by default.
- Avoided duplicate derived vanilla nausea while the custom optical effect is active; independent vanilla nausea remains untouched.
- Added client-only `/deepdivingvisual preview <condition> <1-3>`, `stop` and `status` commands.
- Added native shader/resource lifecycle checks and bounded transition/recovery checks. No server physiology, gas balance or packet-format change.

# 1.0.0-alpha.3 — Readable screens and diving equipment

- Added solid high-contrast machine readouts, darker button labels, wider control spacing and a responsive compact layout.
- Reworked the diving computer and HUD as black OLED-style displays with white numeric readings and paged warnings.
- Added named diving status effects; vanilla symptom behavior is derived without adding separate vanilla debuff entries.
- Preserved independent potion effects and prevented duplicate movement penalties from combined diving conditions.
- Replaced cylinder cuboids with direct rendering of Create's backtank model: extended single, twin with manifold, and compact emergency variants.
- Added worn-cylinder rendering for normal and slim player skins.
- Expanded server and native visual checks for named effects, potion coexistence, layout routing and equipment rendering.

# 1.0.0-alpha.2 — World profiles and diving effects

- Added optional reference-depth pressure curves, individual dimension profiles, fixed surfaces for roofed/flooded worlds, and wider linear scaling ranges.
- Added configurable cylinder/backtank protection ratings, unprotected threshold, oxygen/narcosis limits and hypoxia/recovery rates.
- Gas tooltips, blender ranges and decompression ceilings now use the same configured world pressure curve.
- Added curved eyelid blinks that lengthen with hypoxia, tunnel vision, smooth sensory transitions, slower controls and camera drift.
- Added heartbeat/breath cues and gradual recovery with blackout hysteresis; renamed the game meter to oxygen reserve.
- Added persistent pressure injury, red mask flashes, shared blood/bubble clouds, configurable catastrophic rapid compression and a distinct pressure death message.
- Added a guarded optional Create Deep Seas dry-compartment bridge without mandatory submarine/physics dependencies.
- Avoided fabricated pressure shocks on login, dimension changes and pressure-profile changes; rated worn equipment protects even with an empty cylinder.
- Added independent cosmetic controls and pressure damage ownership safeguards.
- Expanded simulation, server and native visual probes; bumped network version, requiring matching alpha.2 on both sides.

# 1.0.0-alpha.1 — First playable diving systems

- Added depth-based pressure and pressure-scaled breathing gas use.
- Added four tissue compartments with nitrogen/helium loading, decompression ceilings and residual dive state.
- Added hypoxia, blackout and recovery, oxygen toxicity, narcosis and decompression sickness.
- Added custom Air, Nitrox, Trimix and Heliox cylinder mixtures.
- Added three cylinder sizes, regulator, diving computer and underwater lamp.
- Added Create-powered compressor, separator and gas blending station, plus gas storage vessels.
- Added Create pipe/tank capabilities, automated cylinder input/output and helium cartridge processing.
- Added Engineer's Goggles data, Display Link output and four Ponder tutorials.
- Used Create's actual UI assets for screens, controls, inventory and HUD.
- Preserved diving state on relog/End return and machine contents on save/reload and sneak-wrench pickup.
- Added difficulty presets, configuration, optional pressure-damage ownership detection and operator testing commands.
- Added simulation, dedicated-server and client visual checks.

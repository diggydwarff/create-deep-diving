# Create: Deep Diving Wiki

**Version: 1.0.0-rc.1 · Minecraft 1.21.1 · NeoForge · Create 6**

Deep Diving adds refillable breathing cylinders, gas production and blending, diving physiology, exploration gear, underwater charts and a persistent dive journal. This guide describes the current RC1 build. Numerical examples use a new world's **Standard** defaults; server settings and saved world configurations can change them. Depth means **blocks below the water surface**, measured from the diver's eyes, rather than world Y or real-world metres.

## Contents

- [Installation](#installation)
- [First dive](#first-dive)
- [Controls](#controls)
- [Equipment and breathing](#equipment-and-breathing)
- [Dive computer and HUD](#dive-computer-and-hud)
- [Pressure and gas mixtures](#pressure-and-gas-mixtures)
- [Physiology and recovery](#physiology-and-recovery)
- [Ascent and decompression](#ascent-and-decompression)
- [Gas workshop](#gas-workshop)
- [Lighting and dive tools](#lighting-and-dive-tools)
- [Maps and bundles](#maps-and-bundles)
- [Site markers and marine signs](#site-markers-and-marine-signs)
- [Dive Log Book](#dive-log-book)
- [Water effects and performance](#water-effects-and-performance)
- [Compatibility](#compatibility)
- [Configuration](#configuration)
- [Recipe reference](#recipe-reference)
- [Commands](#commands)
- [Troubleshooting](#troubleshooting)
- [Upgrading and current limits](#upgrading-and-current-limits)

## Installation

Install matching Deep Diving builds on the client and dedicated server. Singleplayer uses the same JAR.

| Requirement | Supported target |
| --- | --- |
| Minecraft | 1.21.1 |
| Loader | NeoForge 21.1.219 or newer; this build was validated with 21.1.238 |
| Java | 21 |
| Create | 6.0.10 or a compatible later 6.0.x build; the declared range stops before 6.1 |
| Create dependencies | Install the dependencies required by your Create distribution, including its Ponder/Flywheel components |
| Illumination API | Version 1.0 is bundled with Deep Diving |

The creative tab uses blue diving fins as its icon. Hover supported items and hold the normal Ponder key for illustrated tutorials. The **Deep Diving** Ponder chapter contains seven scenes: compressor, separator, blender, gas storage, diving equipment, charts/bundles/site markers, and the dive log.

## First dive

1. Craft a **Gas Compressor** and a cylinder. Advanced and High-Capacity cylinders use Create mechanical crafting; the Emergency Air Cylinder uses a normal crafting table.
2. Supply at least **16 RPM** to the compressor's rear shaft. Keep the block immediately above its intake out of water and leave its redstone pause input unpowered.
3. Put the cylinder in the compressor input. The compressor makes compressed air and fills the cylinder; a full cylinder moves to the output slot. A partially filled input cylinder can also be taken out for use.
4. Wear a **Create diving helmet** in the head slot and the filled cylinder in the chest slot. Use the cylinder while looking into open space to equip it or swap chest equipment.
5. Carry a **Diving Computer** in your inventory or offhand. It supplies the HUD, full readout and alarms. The helmet draws gas directly from the cylinder: **a separate regulator is no longer required**.
6. For reserve gas, fill another cylinder and sneak-use it in open space until its tooltip says backup is enabled. Keep that spare in your inventory or offhand.
7. Equip fins if desired. Choose a lamp, knife and guide lines for the site. Start a Dive Log Book session if you want a record.
8. Begin in shallow water. Check remaining gas time, mixture depth limits and the worn equipment's pressure rating before going deeper.
9. Watch your ascent rate and no-decompression time. If a required stop appears, follow the computer's stop depth and next-stop guidance.
10. Finish the log session after the dive and refill used cylinders. Residual decompression loading can remain after surfacing.

A **combined snorkeling mask** is an alternative for surface swimming. It occupies the helmet slot and does not connect a cylinder to your breathing system; use a Create diving helmet for cylinder dives.

### Preparation advancements

| Advancement | What it teaches |
| --- | --- |
| Create: Deep Diving | Obtain a diving cylinder |
| Ready to Dive | Wear a Create diving helmet and filled cylinder, and carry a computer |
| First Breath Below | Breathe underwater with that prepared kit, adequate oxygen reserve, suitable oxygen exposure and pressure within the worn equipment rating |
| A Breath in Reserve | Carry a filled spare with inventory backup enabled |

## Controls

Bindings below are defaults. Rebind the depth-hold action in Minecraft's Controls menu.

| Action | Control |
| --- | --- |
| Wear or swap a cylinder | Use it while looking into open space |
| Toggle a cylinder's inventory-backup setting | Sneak-use it in open space |
| Place a cylinder | Use it on a block |
| Collect a placed cylinder | Use it with an empty hand; sneak to collect into inventory instead of auto-equipping |
| Equip fins, mask or swim trunks | Use the item, or put it in its equipment slot |
| Swim with fins | Sprint while moving in water; jump to return upright |
| Rest with fins | Release movement and vertical input for gentle passive floating |
| Toggle explicit depth hold | **V**, with a computer, diving helmet and available breathing supply |
| Release depth hold | V again, jump or sneak |
| Open the computer | Use the Diving Computer |
| Toggle the selected lamp | Use that lamp |
| Open the log book | Use the Dive Log Book |
| Log start/end shortcut | Sneak-use the log book; its journal also opens |
| Initialize a Depth Map | Use a new map once |
| Zoom a Depth Map out | Sneak-use an initialized, unlocked map; up to four zoom steps |
| Open a Dive Map Bundle | Use the bundle |
| Change bundle page | Previous/next buttons or Left/Right arrow keys |
| Add/remove a dive-site marker | Use a map, or the selected page of a bundle, on a named site sign/panel |
| Connect a guide line | Use the line on one Guide Node, then the next |
| Clear an unfinished line selection | Sneak-use the line in open space |
| Coat or cut a placed line | Aim at it and use glow ink, or a knife/shears |
| Open a pictogram panel's editor | Empty-hand use |
| Rotate a pictogram panel | Sneak-use with an empty hand |
| Cycle the underwater orbit camera | Perspective key, normally **F5**; mouse orbits, wheel changes distance |

The orbit view looks around the diver without changing their swimming direction or lamp aim. It is an additional underwater perspective, not a detached spectator camera.

## Equipment and breathing

### Cylinder types

| Cylinder | Surface-equivalent capacity | Default equipment rating | Rating depth with the default linear curve |
| --- | ---: | ---: | ---: |
| Advanced Diving Cylinder | 12,000 mB | 12 atm | 110 blocks |
| High-Capacity Diving Cylinder | 24,000 mB | 21 atm | 200 blocks |
| Emergency Air Cylinder | 2,000 mB | 12 atm | 110 blocks |

These are equipment limits, not promises that the contained gas is suitable at that depth. Oxygen exposure, nitrogen narcosis, remaining gas and decompression must also be considered.

Create Copper Backtanks default to **6 atm**, and Netherite Backtanks to **12 atm**, when Deep Diving owns their breathing simulation. Their air capacity and upgrades remain Create's system. Deep Diving cylinders and backtanks share the chest slot.

A cylinder tooltip shows its mixture, remaining amount, equipment rating, oxygen-derived maximum depth, any minimum depth, and backup setting. Gas and settings stay with the same stack when worn, carried or placed.

### Supply priority and reserves

With a Create diving helmet equipped, breathing selects one available supply:

1. A filled cylinder worn in the chest slot.
2. Filled inventory cylinders with backup enabled, in inventory order.
3. An enabled, filled offhand backup.
4. Available Create backtank air.

Only the selected supply is drained. Empty cylinders and disabled inventory spares are skipped. Enabling backup is necessary for a carried spare, but not for the cylinder you are wearing. Emergency cylinders also identify their supply as emergency gas when worn.

Inventory backups provide breathing gas; pressure protection comes from the worn equipment. A high-rated spare does not upgrade the protection of the chest equipment you are actually wearing.

Gas use increases with ambient pressure. A cylinder's default Standard drain is **8 × pressure mB per second** while it supplies underwater breathing. For example, 40 blocks of depth gives 5 atm, so a full Advanced cylinder lasts about **five minutes at that constant depth**. The computer estimates time from the current supply and current pressure, rather than planning the rest of your trip.

### Masks, fins and swimwear

| Item | Slot | Purpose |
| --- | --- | --- |
| Create diving helmet | Head | Connects a worn cylinder or enabled backup to breathing; supports Create backtank use |
| Combined snorkeling mask | Head | Surface-air breathing through an unobstructed snorkel |
| Diving Fins | Feet | Fins swimming and gentle passive floating |
| Swim Trunks | Legs | Cosmetic, above-knee swimwear |

Fins, combined masks and trunks each support all **16 dye colors**. Combine one piece with a dye to recolor it.

The snorkel needs reachable surface air **less than two blocks above the eyes**. A solid roof, glass, a closed trapdoor or another collision obstruction can block it. It is not a compressed-gas source for deep dives.

Idle fins use a small bob and gentle height correction. Moving, rising or descending releases passive assistance. Explicit V depth hold is a separate action and does not require fins, but it does require supplied diving gear and a computer. It cannot be enabled while flying, riding, fall-flying or blacked out, or while already above a required stop. You choose when to move to the next stop; holding depth does not advance through stops automatically. Depth hold needs breathing managed by Deep Diving: with `manageCreateBacktanks` disabled, use a supplied Deep Diving cylinder rather than relying only on unmanaged Create air.

## Dive computer and HUD

Carry the computer for its HUD and alarms; use it for the detailed display and **HELP** recovery pages. The full screen includes paged warnings and an **SND** alarm toggle. The HUD appears during a dive and can remain visible on the surface while a ceiling or warning needs attention.

| Reading | Meaning |
| --- | --- |
| Depth / blocks | Distance below the sampled water surface, measured from the eyes |
| ATM | Ambient pressure from the server's pressure profile |
| GAS | Estimated time on the selected supply at current pressure; `--` means an estimate is unavailable |
| Mixture | Current oxygen/nitrogen/helium mix; the compact panel uses this space for next-stop information during decompression |
| NDL | Estimated time before a no-decompression dive becomes a decompression dive; `60m+` is the display's upper estimate, not unlimited time |
| STOP | Required decompression depth; a ceiling is the shallowest permitted depth while that obligation remains |
| NEXT | Estimate for clearing the next three-block stop step; `NEXT --` means the current position cannot provide that estimate |
| Ascent rate and bar | Positive values are ascending, negative values descending; the bar's centre mark is the configured sustained-ascent limit |
| Guidance | Actions such as slow ascent, descend to a stop, hold a stop, or an active depth/fin hold |
| Warning footer | Highest-priority warning; the compact HUD adds `+N` when further warnings are present |

`NO GAS` replaces stop-time information when breathing supply is unavailable. Open the full computer to inspect every warning and its recovery guidance.

### HUD size and layout

Go to **Mods → Create: Deep Diving → Config → Client settings**:

- **Compact HUD:** enabled by default. Turn it off for the expanded layout.
- **HUD size:** Small **75%**, Normal **100%**, Large **125%**, or Extra large **150%**.
- **HUD horizontal/vertical offset:** position the panel in GUI pixels.

The default compact panel uses about **37% less screen area** than the previous expanded HUD while keeping normal-size labels. Its size setting affects this HUD independently of Minecraft's global GUI scale. Placement includes the full Create frame and stays within the screen; unusually small viewports reduce the requested size to fit.

## Pressure and gas mixtures

### Depth and pressure

The default linear profile is:

```text
Pressure in atm = 1 + depth in blocks / 10
```

| Depth below the surface | Default pressure |
| ---: | ---: |
| 0 blocks | 1 atm |
| 10 blocks | 2 atm |
| 30 blocks | 4 atm |
| 40 blocks | 5 atm |
| 100 blocks | 11 atm |
| 200 blocks | 21 atm |

World Y alone does not tell you depth: the water surface and your eye height matter. Local-water sampling is the default. Server owners can choose a fixed surface, a reference-ocean curve, or per-dimension profiles. A reference curve can give deep custom oceans a different progression; the computer, gas tooltips and decompression calculations use the same curve.

### Presets and custom mixtures

The blender provides four presets. Oxygen and helium can also be adjusted in **one-percent steps**; nitrogen is the remainder.

| Preset | Oxygen | Nitrogen | Helium | Oxygen-only maximum depth under default settings, rounded down |
| --- | ---: | ---: | ---: | ---: |
| Air | 21% | 79% | 0% | 56 blocks |
| Nitrox | 32% | 68% | 0% | 33 blocks |
| Trimix | 18% | 37% | 45% | 67 blocks |
| Heliox | 21% | 0% | 79% | 56 blocks |

These maxima describe only the upper oxygen limit. They do not include narcosis, worn equipment rating, gas duration or decompression. Check the current cylinder tooltip when the server uses custom settings.

The default oxygen partial-pressure interval is **0.16–1.4 atm**. Partial pressure is ambient pressure multiplied by the gas fraction. More oxygen can improve a shallower mixture but reaches the upper oxygen limit sooner. Low-oxygen custom mixes can have a **minimum usable depth** as well as a maximum. The tooltip rounds the minimum upward and maximum downward.

Helium replaces nitrogen and reduces nitrogen narcosis; it does not remove pressure limits, oxygen toxicity or decompression. The default nitrogen-narcosis threshold is **3.2 atm nitrogen pressure**. With air and the default linear curve, it begins around **31 blocks** of depth, before air reaches its oxygen-only maximum.

Purge a partly filled cylinder before changing to a different mixture. Purging vents its remaining gas. Topping up with the same mixture preserves what is already inside.

## Physiology and recovery

Physiology is a simplified game model with four nitrogen/helium tissue compartments and persistent residual loading. The oxygen reserve shown by the computer is a game resource, not a blood-oxygen measurement.

| Warning or condition | What causes it in the mod | Response in the game |
| --- | --- | --- |
| Low gas reserve | Selected supply falls below 15% capacity | Check time remaining and prepare a filled, enabled reserve or end the dive |
| Backup cylinder active | An inventory/offhand backup or emergency supply is selected | Check which supply is being used and its remaining time |
| No breathing supply | Diving helmet is underwater without available supply or a breathing bypass | Restore a filled compatible supply; check helmet, slot and spare enablement |
| Low oxygen / hypoxia | No breathable supply after reserve air runs low, or insufficient oxygen at current pressure | Restore breathing with a mixture suitable for current pressure |
| Oxygen toxicity | Excess oxygen partial pressure; exposure accumulates | Reduce the excessive exposure and allow toxicity to recover |
| Narcosis | Nitrogen partial pressure exceeds the configured threshold | Reduce nitrogen exposure, using the computer to check mixture and depth |
| Ascending too fast | Sustained ascent crosses the server's speed, time and rise thresholds | Slow the ascent; required decompression stops still apply |
| Decompression required | Residual tissue loading imposes a ceiling | Follow the stop guidance and wait for the next step to clear |
| Decompression recovery | Decompression stress remains after the ceiling clears | Allow recovery; a clear ceiling does not instantly remove existing illness |
| Equipment pressure limit / pressure injury | Ambient pressure exceeds the worn equipment's protection | Return to pressure within the equipment's rating and allow injury to recover |
| Sudden pressure exposure | An abrupt, insufficiently protected compression transition | Equip rated gear before entering deep water; check the world's pressure profile |
| Blackout | Severe oxygen deficit or accumulated oxygen toxicity | Restore suitable breathing and allow reserve/toxicity to recover; inventory access remains available |

Symptoms can include altered vision, eyelid blinks, camera sway, muffled sound, impaired movement and damage. Recovery is gradual. Blackout uses separate onset/recovery thresholds so it does not rapidly flicker on and off.

By default, Water Breathing and Conduit Power bypass compressed-gas requirements and preserve tank gas. They do not erase earlier tissue loading or existing decompression obligations. Breathable air also does not instantly clear prior loading.

Physiology persists through ordinary logout. Death resets the diver's physiology. Spectator mode and Creative without a diving helmet bypass simulation; wearing a Create diving helmet opts Creative into gas use, warnings and physiology for testing.

## Ascent and decompression

Ordinary short up/down adjustments are allowed a grace period. Standard defaults require all of the following before a rapid-ascent warning:

- More than **2.5 blocks per second** upward.
- **Three consecutive seconds** of fast ascent.
- At least **four blocks gained** during that continuous ascent.

Slowing, stopping or descending resets the fast-ascent history. This grace period does not permit crossing a decompression ceiling.

The computer rounds required stops to **three-block increments**. A stop depth of 12 means stay at least 12 blocks below the surface while that ceiling remains. `DESCEND TO …` takes priority if you are too shallow. Approach the indicated stop, maintain breathing supply, and follow the countdown for the next step. Being much deeper is not a shortcut: off-gassing and the estimate depend on the mixture and pressure where you actually are. A missing countdown can mean you need to move to the stop or restore supply.

When the next step clears, move to the newly indicated depth. V depth hold helps you stay in place, but you release and retoggle it when choosing a new depth. Passive fins do not automatically force a multi-block descent to a deeper stop.

The default `decompressionRate = 6.0` accelerates off-gassing for shorter game stops without accelerating gas uptake. `tissueRate` scales tissue time more generally. World owners can change these separately. Existing worlds retain saved settings, so an older ascent limit may need updating manually.

## Gas workshop

### Machinery and ports

Production machines take rotation through the rear shaft and need at least **16 RPM**. Faster rotation increases processing throughput. A redstone signal pauses production. Gas Storage Vessels are passive and need no shaft power.

Follow the colored ports when routing pipes: **blue is oxygen**, **green is nitrogen**. The rear is the shaft side.

| Machine | Inputs | Outputs and use |
| --- | --- | --- |
| Gas Compressor | Dry top intake; rear shaft; cylinder input slot | Creates compressed air; gas output on every face except the rear; directly fills air cylinders |
| Gas Separator | Compressed air through the top; rear shaft | Oxygen from the blue port and nitrogen from the green port; both outputs need room |
| Gas Blending Station | Oxygen through blue, nitrogen through green, helium through the top or cartridge slot; rear shaft; cylinder input | Fills cylinders with the selected mixture; finished cylinders move to the output slot |
| Gas Storage Vessel | Matching gas through any face; helium cartridges can supply helium | Stores one gas type; any face permits gas input/output; no rotational power required |

Production buffers hold **16,000 mB** per gas tank; a storage vessel holds **64,000 mB** of its one gas. Separate compressed air, oxygen, nitrogen and helium into appropriate vessels. Custom breathing mixtures are filled into cylinders by the blender rather than exported as a mixed pipe fluid.

### A working filling station

1. Start with a powered compressor and use it to fill air cylinders directly.
2. Route compressed air from the compressor to a separator's top inlet.
3. Route the separator's blue oxygen outlet and green nitrogen outlet to separate vessels or the corresponding blender inlets. Keep both separator outputs clear.
4. Add helium to the blender's top inlet or cartridge slot when the mixture requires it.
5. Select Air, Nitrox, Trimix, Heliox or custom percentages. Insert a cylinder and purge any incompatible residual mixture before filling.
6. Collect the filled output manually or with a hopper below the machine.

Create pumps move gas through longer pipe networks. Production machines can also transfer directly through matching adjacent container capabilities. Different fluids do not mix inside a vessel. A storage vessel holds gas; use the compressor or blender's cylinder slot for refilling equipment.

### Helium and automation

A **Helium Cartridge** comes from heated Create mixing: one gold ingot, amethyst shard, blaze powder and ender pearl. A cartridge supplies **1,000 mB helium** in a blender or gas vessel. It is consumed as the machine accepts the gas.

Automation inserts cylinders/cartridges through valid inventory sides; the bottom extracts completed cylinders. The blender conserves constituent quantities, including partial fills and low-speed batches. It stops when a needed gas is unavailable or the output is blocked; it does not silently substitute another mixture.

Wear Engineer's Goggles to inspect gas levels, mixture and status. A Create Display Link can use **Diving gas status** for machine status, stored gas amounts and the selected mixture. The vessel's displayed pressure gauge represents its fill level rather than a separate pipe-pressure simulation.

Use a Create wrench for rotation. **Sneak-wrench pickup** preserves machine contents. Ordinary breaking drops inventory and vents stored gas, so use the wrench when relocating a filled station.

## Lighting and dive tools

### Lamps

Each lamp is reusable and has its own on/off setting. Hold an enabled lamp to aim it; stowed enabled lamps provide downward illumination. Active held lamps take priority over stowed spares. Nearby divers' lamps can also be visible when enabled in client settings.

| Lamp | Beam | Reach multiplier |
| --- | --- | ---: |
| Diving Lamp | Balanced general-purpose beam | 1.00× |
| Wide-beam Diving Lamp | Broad flood light | 0.70× |
| Long-throw Diving Lamp | Narrow, longer-reaching spot | 1.80× |
| Inspection Diving Lamp | Very wide close-work light | 0.45× |

Multipliers apply to the configured lamp range, subject to the renderer's budgets. Lamps need no separate battery. Sediment can attenuate their light. Client options cover warm/cool/custom color, brightness, range, volumetric appearance, shadows and nearby-light limits.

### Dive Knife

The knife cuts guide lines, quickly clears underwater plants, and provides shears-style harvesting for seagrass. It also works on kelp, vines and cobwebs and adds **3 damage** against aquatic targets. Normal durability and block-protection rules apply.

### Guide lines

Place Guide Nodes on supported floors, walls or ceilings. Use a colored guide line on the first node, then a second loaded node within **64 blocks**. One successful connection consumes one line outside Creative. Add intermediate nodes for longer routes.

Sneak-use a line in open space to clear an unfinished anchor selection. Aim at a placed span and use glow ink to coat it, or a Dive Knife/shears to cut it. Cutting refunds the line, including its glow coating. Guide lines are navigation aids; they are not a breathing hose or a restraint tether.

## Maps and bundles

### Depth Maps

Use a new Depth Map once to initialize it, then hold it in either hand to survey nearby loaded terrain. Light blue shows shallow water, darker blue deeper water, and gray land. Contours follow **10-block depth intervals**; sparse contrasting soundings give water depth in blocks.

The chart measures surface-water bathymetry. It does not reveal flooded caves beneath solid land or load distant chunks for surveying. New chart pixels appear progressively while you travel and hold the map. Survey work is shared across held maps/bundles, capped at **64 samples per player per tick**; holding duplicates does not multiply the budget.

Sneak-use an initialized, unlocked chart to zoom out, up to map scale 4. Zooming gives the wider chart a new map ID and it must survey that wider area. Its data and known site markers are preserved using the chart system's scaling rules.

### Cartography and display

| Operation | Cartography-table partner |
| --- | --- |
| Enlarge | Paper |
| Copy | Empty vanilla map |
| Lock | Glass pane |

Copies share the saved map identity and updates. Locking creates a frozen chart snapshot. Depth maps can be placed and rotated in normal or glow item frames.

### Dive Map Bundles

A bundle contains up to **16 actual Depth Map stacks**, one per chart slot.

1. Initialize each new Depth Map before inserting it.
2. Open the bundle. Place charts normally or shift-click them into its chart slots; remove them with ordinary inventory controls.
3. Browse populated pages with the buttons or Left/Right keys. Empty slots are skipped and the selected page is saved.
4. Hold the closed bundle to survey its selected page. Other pages keep their prior survey.
5. Use the bundle on a named dive-site sign or panel to toggle that site's marker on the selected page.

The bundle preserves chart names, map IDs, zoom, lock state and markers. Inserting/removing maps does not copy them. It accepts initialized Depth Maps, including their retained chart data; vanilla terrain maps, uninitialized maps, other items and nested bundles are not accepted.

Remove an individual chart before using a cartography table or item frame. The held bundle is secured while its inventory is open.

## Site markers and marine signs

### Two kinds of site sign

| Item | Appearance and editing | How it marks maps |
| --- | --- | --- |
| Dive Site Sign | Writable riveted metal sign; ordinary sign text, dye, glow ink, wax, wall placement and waterlogging | Write the site name on the **front first line**, then use an initialized Depth Map, filled terrain map or bundle on it |
| Dive Site Panel | Thin metal pictogram panel with a custom caption and optional arrow | Choose the Dive Site design and give it a nonempty custom caption, then use a map or bundle on it |

Using the same map on the site again removes its marker. The current build uses a compact custom dive-site icon, visible in standalone charts and bundle pages.

The map must belong to the current dimension, be unlocked, and cover the site. A bundle acts on its selected page. Unlocked viewed maps refresh site names when the sign is loaded; locked charts keep their snapshot. Marker identity survives supported chart scaling, copying, locking, storage and reload.

### Marine pictogram panels

Craft blank metal panels, then select a design in a stonecutter. The available set contains a blank plus ten 16×16 pictograms:

| Group | Designs |
| --- | --- |
| Sites | Dive Site, Diver Down, Cave Entrance, Wreck, Dive Training Pool |
| Warnings | Danger / Turn Back, Strong Current, Do Not Enter |
| Navigation and facilities | Exit / Surface, Gas Filling |

Panels mount on solid walls, floors, ceilings and fences, support waterlogging, and have no movement collision. Empty-hand use opens the caption editor. Sneak-use with an empty hand rotates the mounted design by a quarter turn.

The editor supports captions up to **32 characters**, 16 caption colors, and no arrow/left/right/up/down. Dye recolors a caption directly. Glow ink makes the pictogram, caption and arrow readable in darkness; ordinary ink removes that glow. Glowing panels do not cast light into the room.

Captions, arrows and ink survive normal pickup, placement and re-cutting. Older retired designs remain loadable and can be re-cut, but the creative menu and output recipes use the smaller core set. The Training Pool design is a facility sign, not a progression or training-buff system.

## Dive Log Book

Craft a book, ink sac, compass and copper ingot together. Open it to view the field-journal interface.

Carry the book and a Diving Computer. Open the journal and choose **Start dive**, then **End dive** when finished. Sneak-use provides the same start/end shortcut. Recording is manual; simply entering water does not start a session.

Each book retains the latest **32 completed dives**, with a lifetime dive number that continues as older entries roll out of history. The recorded data includes:

- Duration and time actually underwater.
- Maximum/average depth and pressure readings.
- Breathing mixtures and actual gas consumption.
- Ascent/decompression information and warning history.
- Starting coordinates and dimension.

Cylinder mB and Create backtank air remain separate consumption units. Swapping tanks does not count a refill as negative gas use. Completed records travel with the book.

The active session closes on logout, death, dimension transfer, computer removal or dropping the book. A book transferred to someone else cannot continue its former owner's session. A stored book stops being sampled; returning after an interruption seals the stale session instead of inventing missing dive time. Starting another book closes your other active recording. Use the journal's page controls to inspect completed history.

## Water effects and performance

Supplied divers have equipment breathing sounds and an exhalation puff near the mouth followed by a smaller trail. Bubbles rise at varied speeds, stay behind as you swim past, and fade with age and near the surface. Their effect does not charge extra breathing gas. Empty supply, missing helmet, dry air and breathing bypasses stop ordinary cylinder exhaust.

Underwater acoustics blend air/water filtering, equipment breathing and nearby diver sounds. Dry air pockets restore ordinary acoustics. A subtle depth-dependent ambience and sparse marine snow provide environmental detail; rare luminous specks have their own switch.

Mining loose seabed material and moving close to the bottom can raise sediment clouds. Staying above the seabed or moving carefully reduces disturbance, and fins reduce newly stirred sediment. Cloud density also affects lamp visibility.

For lower GPU cost, start with **Diving lamps → Reduced effects**, fewer nearby lights and a smaller shadow budget. Reduce marine-snow/sediment particle limits if needed. Disable `acoustics.soundFiltering` when another sound mod owns muffling; breathing sounds and exhaust have independent switches. **Reduced motion** disables camera sway and animated distortion while retaining readable condition cues.

Client cosmetic settings do not disable the server's diving conditions or damage. The detailed computer and recovery guide remain the place to identify the cause of impaired vision or movement.

## Compatibility

| Mod or system | Current integration |
| --- | --- |
| Create | Required; helmets/backtanks, rotation, pipes/tanks, mechanical crafting, mixing, Ponder, Goggles and Display Link support |
| Create Deep Seas | Optional oxygen-fluid/tag exchange and guarded dry-compartment detection; the bridge does not change submarine oxygen storage or equate hull pressure with breathing pressure |
| Mekanism | Optional chemical ports on the corresponding gas-machine sides; oxygen is directly supported, with one chemical unit to one mB conversion |
| Other gas providers | Matching common gas tags can supply nitrogen/helium and compatible fluids; unrelated gases and untagged cryogenic fluids are rejected |
| Crush Depth | Optional detection defers Deep Diving equipment-pressure damage by default to avoid duplicate pressure systems; gas and other physiology remain separately configurable |
| Other pressure mods | Add their actual installed mod IDs to `externalPressureOwners`, or configure the relevant simulation/damage ownership switches |
| Iris / Sodium / shader packs | Lighting bridges and condition-overlay fallback are provided; exact appearance and performance depend on the pack and GPU |
| Fading Depths | Separate depth-darkness mod; install it separately if desired |
| Cold Sweat | No bundled thermal wetsuit or dedicated insulation stats; swim trunks are cosmetic |
| Aeronautics / Sable / CC: Tweaked / Fluidworks | Not required dependencies |

Deep Diving oxygen joins `c:oxygen` and `create_submarine:oxygen`. Matching fluid aliases are normalized 1:1 into the internal gas representation. Mekanism connections retain the same input/output directions as fluid ports. Core Mekanism supplies oxygen; nitrogen/helium chemicals need a provider with the matching tags.

Deep Seas compartment calls are optional and guarded. Missing or changed APIs fall back to ordinary water detection. A detected dry compartment is treated as surface-pressure breathing air in this model; residual decompression loading still needs recovery. Moving submarine assemblies, extended multiplayer sessions and the exact server pack still warrant in-pack testing; support for an integration is not a blanket certification of every assembly or shader combination.

## Configuration

Open **Mods → Create: Deep Diving → Config** for the themed editor. **Client settings** control local display/audio; **World settings** control gameplay. World editing follows ordinary singleplayer, LAN and server permissions. The editor supports validation, Undo, Reset, saving, and applicable restart/reload notices.

| File | Purpose |
| --- | --- |
| `config/deepdiving-client.toml` | Per-client HUD, effects, sound, particles and lamps |
| `<world>/serverconfig/deepdiving-server.toml` | Dedicated-server world gameplay settings |
| `saves/<world>/serverconfig/deepdiving-server.toml` | Singleplayer world gameplay settings |

Existing worlds retain their saved values. Changing a default in a newer JAR does not overwrite an older configured value. For dedicated servers, edit the correct world's file and apply it through the normal server restart. Merge excerpts into their existing sections; root keys belong before any TOML section header.

### Useful client settings

| Setting | Purpose |
| --- | --- |
| `showHud`, `compactHud`, `hudSize`, `hudX`, `hudY` | Visibility, compact/expanded layout, four size presets and position |
| `reducedMotion`, `effectIntensity` | Motion accessibility and overall cosmetic intensity |
| `[shaders]` | Condition-specific world effects, quality and overlay fallback |
| `[computer]` | Alarm enablement, volume and cooldown |
| `[acoustics]` | Breathing, exhaust, nearby divers and sound filtering |
| `[ambience]` | Environmental sounds, depth transition and marine snow |
| `[sediment]`, `[underwaterParticles]` | Client particles and budgets |
| `[lamp]` | Color, brightness, range, beam, nearby lights and shadows |

HUD defaults at the root of the client file:

```toml
showHud = true
compactHud = true
hudSize = "NORMAL"
hudX = 8
hudY = 8
```

`hudSize` accepts `SMALL`, `NORMAL`, `LARGE`, or `EXTRA_LARGE`. The menu displays their percentages.

### Useful world settings

| Setting | Default or role |
| --- | --- |
| `difficulty` | `STANDARD`; Arcade is gentler and reduces gas demand, Hardcore increases condition severity |
| `enabled`, `physiology`, `equipmentPressureDamage` | Master simulation, physiology and equipment-pressure damage switches |
| `manageCreateBacktanks` | `true`; chooses ownership of Create air drain, independently of Deep Diving cylinders |
| `createHelmetNightVision` | `false`; Create helmet enhanced underwater visibility, independently of lamps or Night Vision potions |
| `gasPerSecondAtSurface` | `8.0` for cylinders in Standard |
| `waterBreathingBypass` | `true`; magical breathing avoids compressed-gas requirements |
| `blocksPerAtmosphere` | `10.0` in the default linear profile |
| `minimumOxygenPartialPressure`, `maximumOxygenPartialPressure` | `0.16`, `1.4` |
| `narcosisNitrogenPressure` | `3.2` |
| `tissueRate`, `decompressionRate` | `1.0`, `6.0`; uptake/general tissue time and off-gassing can be tuned separately |
| `maximumAscentBlocksPerSecond` | `2.5` |
| `ascentWarningGraceSeconds`, `ascentWarningMinimumRise` | `3.0` seconds and `4.0` blocks |
| `allowDepthHold` | `true` |
| Equipment rating keys | Separate ratings for Create backtanks, all cylinder tiers and unprotected divers; the unprotected threshold defaults to 6 atm |
| `suddenPressureInjury`, `pressureShockDelta` | Abrupt-compression injury; enabled with a 4 atm transition threshold by default |
| `catastrophicPressureExposure`, `catastrophicPressureDelta`, `catastrophicPressureExcess` | Extreme compression can be fatal by default; transition/excess thresholds are 12/8 atm |
| `breathingBubbles`, `pressureBloodParticles` | Server switches for shared breathing exhaust and pressure-injury particles |
| `deepSeasCompartmentSupport`, `autoAvoidDuplicatePressureDamage` | Optional compatibility ownership/detection |
| `[sediment]` | Server cloud generation, mining/movement disturbance and density limits |

To adopt RC1's more forgiving ascent settings in an older world, review these existing root values:

```toml
maximumAscentBlocksPerSecond = 2.5
ascentWarningGraceSeconds = 3.0
ascentWarningMinimumRise = 4.0
decompressionRate = 6.0
```

For custom oceans, `pressureMode = "REFERENCE"` maps `referenceOceanDepth` to `referencePressure`; `pressureCurveExponent` controls the curve. `surfaceMode = "LOCAL_WATER"` samples water, while `FIXED` uses `fixedSurfaceY`.

Per-dimension entries have six fields:

```text
dimension|mode|blocks-per-atm-or-reference-depth|reference-atm|curve-exponent|AUTO-or-surface-Y
```

For example, **if your intended surface is Y=60**, this root entry gives the Overworld a curved profile reaching 21 atm at 200 blocks below that surface:

```toml
dimensionPressureProfiles = [
  "minecraft:overworld|REFERENCE|200|21|1.35|60"
]
```

`AUTO` chooses local surface detection for that override. The last entry for a repeated dimension wins. Matching tooltips, pressure ratings and stop calculations all use the selected profile; adjusting the curve changes the meaning of earlier depth examples.

## Recipe reference

This table lists the shipped base recipes and exact ingredient counts. Use the recipe book or a recipe viewer for shaped arrangements. **Mechanical crafting** means Create mechanical crafters, rather than a normal crafting table. Dye variants use the shared recoloring recipes.

| Result | Station | Ingredients |
| --- | --- | --- |
| Gas Compressor | Crafting table | 1× Copper Casing; 2× Mechanical Pump; 1× Shaft; 1× Brass Ingot |
| Gas Separator | Crafting table | 2× Iron Ingot; 1× Copper Casing; 2× Fluid Pipe; 1× Precision Mechanism; 1× Shaft |
| Gas Blending Station | Crafting table | 2× Fluid Pipe; 1× Precision Mechanism; 2× Brass Casing; 1× Fluid Tank; 1× Shaft |
| Gas Storage Vessel | Crafting table | 5× Iron Ingot; 1× Fluid Pipe; 2× Copper Ingot; 1× Fluid Tank |
| Advanced Diving Cylinder | Mechanical crafting | 1× Precision Mechanism; 2× Brass Ingot; 1× Fluid Tank; 3× Iron Ingot |
| High-Capacity Diving Cylinder | Mechanical crafting | 1× Precision Mechanism; 2× Advanced Diving Cylinder; 1× Fluid Tank; 1× Brass Casing |
| Emergency Air Cylinder | Crafting table | 1× Fluid Pipe; 3× Iron Nugget; 1× Copper Ingot |
| Diving Computer | Crafting table | 1× Glass Pane; 2× Brass Ingot; 1× Precision Mechanism; 1× Redstone Dust |
| Diving Lamp | Crafting table | 1× Glass Pane; 2× Copper Ingot; 1× Glowstone; 1× Iron Ingot |
| Wide-beam Diving Lamp | Shapeless crafting | 1× Diving Lamp; 1× Brass Sheet; 2× Glass Pane |
| Long-throw Diving Lamp | Shapeless crafting | 1× Diving Lamp; 2× Iron Ingot; 1× Glass Pane |
| Inspection Diving Lamp | Shapeless crafting | 1× Diving Lamp; 2× Copper Ingot; 1× Redstone Dust |
| Diving Fins | Crafting table | 2× Leather; 2× Dried Kelp; 2× Brass Nugget |
| Mask and Snorkel | Crafting table | 2× Leather; 2× Glass Pane; 2× Dried Kelp; 1× Copper Ingot |
| Blue Swim Trunks | Crafting table | 5× Blue Wool; 2× String |
| Dive Knife | Crafting table | 1× Iron Ingot; 1× Brass Nugget; 1× Stick |
| Guide Line Node ×4 | Crafting table | 4× Iron Nugget; 1× Brass Ingot |
| White Guide Line ×4 | Crafting table | 4× String |
| Dive Log Book | Shapeless crafting | 1× Book; 1× Ink Sac; 1× Compass; 1× Copper Ingot |
| Depth Map | Crafting table | 7× Paper; 1× Compass; 1× Blue Dye |
| Dive Map Bundle | Crafting table | 2× String; 5× Leather; 1× Paper |
| Dive Site Sign | Shapeless crafting | 1× Oak Sign; 1× Compass; 1× Blue Dye |
| Blank Dive Sign ×4 | Crafting table | 3× Iron Nugget; 2× Copper Ingot; 1× Blue Dye |
| Helium Cartridge | Heated Create mixing | 1× Gold Ingot; 1× Amethyst Shard; 1× Blaze Powder; 1× Ender Pearl |

### Mechanical cylinder layouts

The tables show ingredient positions; **— means empty**. Use Create's normal mechanical-crafting arrangement. Newly crafted cylinders start empty. Use empty Advanced cylinders as crafting components for a High-Capacity cylinder; the ordinary recipe does not transfer their stored gas.

**Advanced Diving Cylinder**

| Left | Centre | Right |
| --- | --- | --- |
| — | Precision Mechanism | — |
| Brass Ingot | Fluid Tank | Brass Ingot |
| Iron Ingot | Iron Ingot | Iron Ingot |

**High-Capacity Diving Cylinder**

| Left | Centre | Right |
| --- | --- | --- |
| — | Precision Mechanism | — |
| Advanced Diving Cylinder | Fluid Tank | Advanced Diving Cylinder |
| — | Brass Casing | — |

One guide line plus one dye makes one line in that color. Fins, combined masks and trunks also accept one dye per piece. Stonecutting a panel selects a core pictogram and consumes one panel for one output. A single older Diving Regulator can be crafted alone into one Create Precision Mechanism; its former production recipe is retired.

## Commands

### Operator helpers

Run these as a player with permission level 2. They act on the executing player; filling requires a Deep Diving cylinder in the main hand.

| Command | Result |
| --- | --- |
| `/deepdiving status` | Prints server depth, pressure, reserve, ceiling, stress and mixture |
| `/deepdiving reset` | Resets your physiology for repeatable testing |
| `/deepdiving fill 21 0` | Fills the held cylinder with Air |
| `/deepdiving fill 32 0` | Fills with Nitrox 32 |
| `/deepdiving fill 18 45` | Fills with Trimix 18/45 |
| `/deepdiving fill 21 79` | Fills with Heliox 21/79 |

Fill arguments are oxygen percent, then helium percent. Their sum must not exceed 100; nitrogen fills the remainder. This command fills to the held cylinder's capacity and preserves its backup setting.

Creative test divers need a Create diving helmet to opt into the simulation. Turn off creative flight when testing depth hold or normal swimming behavior.

### Client visual previews

```text
/deepdivingvisual preview hypoxia 3
/deepdivingvisual preview narcosis 3
/deepdivingvisual preview toxicity 3
/deepdivingvisual preview decompression 3
/deepdivingvisual preview pressure 3
/deepdivingvisual preview blackout 3
/deepdivingvisual preview combined 3
/deepdivingvisual status
/deepdivingvisual stop
```

Severity is 1–3. Previews last 60 seconds unless stopped and demonstrate client visuals only; they do not change server gas, health or physiology.

## Troubleshooting

| Problem | Check |
| --- | --- |
| Cylinder is full but I cannot breathe | Wear a Create diving helmet, not the snorkeling mask. Wear the cylinder in the chest slot, or enable it as an inventory/offhand backup. Check the mix's minimum depth too. |
| A spare never takes over | Sneak-use that spare to enable backup, confirm it contains gas, and check the tooltip. The currently worn filled cylinder has priority. |
| My high-capacity cylinder causes narcosis/toxicity | Capacity and equipment rating are separate from gas suitability. Check nitrogen pressure, oxygen limits and the actual mixture. |
| Narcosis appears around 31 blocks | Default air reaches the configured 3.2 atm nitrogen threshold around that depth. A different gas or server pressure curve changes this. |
| Short ascents still warn immediately | Check the existing world's saved ascent speed/grace/minimum-rise values. Old configured values are retained on upgrade. Distinguish a rapid-ascent alert from a required decompression ceiling. |
| A stop timer is missing | Follow the depth guidance, restore breathing supply, and check whether the current mix/pressure permits off-gassing. `NEXT --` is not an elapsed countdown. |
| V depth hold will not enable | Carry a computer, wear a Create diving helmet and have available gas. Check the world's `allowDepthHold` setting and managed supply. Be submerged with flight off, not riding/fall-flying/blacked out, and at or below any required stop. |
| The compressor stopped | Check 16+ RPM, its rear shaft, redstone pause, water above the intake, gas capacity and its cylinder/output slots. |
| The separator stopped with air available | Both oxygen and nitrogen outputs need space. Clear the blocked output and keep the top air inlet supplied. |
| The blender refuses a partly filled cylinder | Purge a different mixture first. Check required constituent gases, the selected percentages and the finished-cylinder slot. |
| Oxygen will not enter a machine | Use the blue oxygen inlet on the blender, or a gas vessel. Check matching tags, gas identity, direction and capacity. The compressor does not accept external gas input. |
| A vessel will not accept another gas | It stores one gas identity. Empty it or use a separate vessel for the other gas. |
| A map stays blank | Initialize it, hold it or the selected bundle page, travel through loaded terrain, allow progressive surveying, and check dimension and lock state. |
| A bundle rejects a map | Insert an initialized Depth Map; vanilla terrain maps, blank charts and nested bundles are not accepted. |
| A site marker is missing from a bundle | Check the selected page, map coverage/dimension/lock state and the site's actual name or custom caption. Use the bundle on the site to mark that page. |
| A log session will not start or ends early | Carry a computer and the book. Check whether the book was dropped, stored, transferred, or carried across a dimension/logout. Start/end sessions manually. |
| A lamp still gives light after one is turned off | Check other enabled carried lamps and nearby divers' lamps. Each lamp has its own state. |
| Lighting looks wrong with a shader pack | Review `lamp.shaderLighting`, reduced effects and shadow settings, and the condition-shader fallback. Test the actual pack/GPU combination. |
| Water muffling conflicts with another sound mod | Disable `acoustics.soundFiltering` while retaining the breathing/exhaust settings you want. |
| The HUD covers too much of the screen | Enable Compact HUD, choose Small/Normal, or adjust its offsets in Client settings. |
| Impaired vision returns after removing a potion effect | The server can reapply symptoms while the underlying diving condition remains. Check the computer's cause/recovery guide. |

## Upgrading and current limits

- RC1 uses network protocol **14**. Use matching Deep Diving builds on client and server, and remove duplicate older JARs.
- Existing Diving Regulators remain loadable but are no longer required, crafted normally, or shown in the creative tab. Recycle old ones into Precision Mechanisms.
- Older colored plain-goggle and separate-snorkel IDs resolve to the corresponding combined snorkeling mask.
- Retired niche pictogram panels remain loadable and can be re-cut into the core set.
- Existing world configuration values remain in force. Review old ascent and pressure settings when adopting RC1's defaults.
- The advanced gas machines use native recipe serializers. A custom JEI/EMI gas-machine category is not included; the in-game Ponder scenes and this workshop guide explain their operation.
- Thermal wetsuits/Cold Sweat insulation, diving bells, breathing hoses, rebreathers, atmospheric suits and a larger swimming-animation overhaul are not part of this RC1 feature set.
- Moving Deep Seas/Sable assemblies, older declared-minimum loader combinations, hardware performance and extended expeditions should be validated in the actual modpack before treating them as certified release combinations.

This wiki covers implemented RC1 behavior. It is a player and server-configuration guide to the game's mechanics, not a real-world diving planner.

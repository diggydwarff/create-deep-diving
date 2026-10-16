# Create Deep Diving Wiki

Deep Diving adds refillable diving equipment, breathing gases and gas-handling machines to Create. Longer dives need some planning: pressure increases gas use, your mixture matters, and a quick trip to the surface can leave you with decompression trouble.

## Your first dive

Start with a Gas Compressor, a cylinder, a Diving Regulator and one of Create's diving helmets. Power the compressor through its rear shaft at 16 RPM or more, keep its top intake above water, and put the cylinder in its input slot. Collect it from the output when filled.

Wear the helmet and cylinder, then carry the regulator and a Diving Computer. Right-click a cylinder while looking into open space to wear it or swap your chest equipment. Sneak-right-click it to toggle its use as an inventory backup.

The worn cylinder is used first, followed by enabled backups, then available Create backtank air. Cylinders need a regulator; Create backtanks retain their own breathing setup. Only one supply is drained at a time. Gas use increases with pressure, so watch the computer's remaining-time estimate.

| Cylinder | Capacity | Default pressure rating |
|---|---:|---:|
| Advanced Diving Cylinder | 12,000 mB | 12 atm |
| High-Capacity Diving Cylinder | 24,000 mB | 21 atm |
| Emergency Air Cylinder | 2,000 mB | 12 atm |

Capacity is surface-equivalent gas. Ratings are configurable and do not replace the mixture's oxygen limits. Cylinders can also be placed as blocks. Collect one with an empty hand; sneak to put it in your inventory instead of equipping it.

## Controls and useful gear

| Gear | Use |
|---|---|
| Diving Computer | Right-click for readouts and built-in help. Carry it for the diving HUD and alarms. |
| Depth hold | Press **V** with supplied diving gear and a computer to hold depth. Press again, jump or sneak to release. The key can be rebound. |
| Diving Fins | Wear in your feet slot. Sprint while moving to swim; jump to return upright. Rest without movement or vertical input to float at depth. |
| Diving Lamp | Right-click to toggle. Hold it to aim; carrying it gives a softer local light. No separate battery is needed. |

The Dive Knife clears underwater plants, cuts guide lines and deals extra damage to aquatic mobs. Cycling perspectives underwater also gives you an orbit view: move the mouse to look around your diver.

## Gases and ascent

The blending station has four presets and lets you adjust oxygen and helium in one-percent steps. Purge a cylinder before changing its mixture.

| Preset | Oxygen | Nitrogen | Helium |
|---|---:|---:|---:|
| Air | 21% | 79% | 0% |
| Nitrox | 32% | 68% | 0% |
| Trimix | 18% | 37% | 45% |
| Heliox | 21% | 0% | 79% |

More oxygen shortens the usable depth range. Helium reduces nitrogen exposure, but pressure and decompression still matter. Check the cylinder tooltip for the mixture's depth limits.

Watch the computer's ascent rate, no-decompression time and stop depth. When it shows a stop, remain at that depth or deeper until it clears to the next step. Stops use three-block increments. Reaching air restores breathing, but previous tissue loading still needs time to clear.

## Building a gas station

Production machines need at least 16 RPM through their rear shaft. Redstone pauses production. Directions below are relative to the front; colored ports and Engineer's Goggles help identify connections.

| Machine | Connections and purpose |
|---|---|
| Gas Compressor | Keep the top intake dry. Extract compressed air from any face except the rear shaft. Can fill air cylinders directly. |
| Gas Separator | Compressed air enters the top. Oxygen leaves the blue left port; nitrogen leaves the green right port. |
| Gas Blending Station | Oxygen enters left, nitrogen right, helium through the top. Helium cartridges also work. Select a mixture and insert a cylinder. |
| Gas Storage Vessel | Holds 64,000 mB of one gas. All faces accept pipes; no rotational power is needed. |

Use Create pipes and pumps for longer connections. Production machines also transfer through matching ports on adjacent containers. A bottom hopper extracts completed cylinders. Sneak-wrench pickup preserves machine gas and inventory.

## Guidelines

Place two Guide Nodes, then use a colored guide line on each to connect them. A span can reach 64 blocks. Sneak-use in open space clears an unfinished selection. Glow ink coats a line for visibility; a knife or shears can cut it.

## Water effects and settings

Equipment breathing follows the active supply, with bubbles on exhalation. Sounds crossing the surface are muffled while nearby underwater interactions stay audible. Air pockets restore ordinary acoustics and fade the underwater ambience.

Deeper water brings a quiet ambient wash and sparse marine snow. A lamp reveals ordinary particles; rare luminous specks have their own switch. Disturbing the seabed raises temporary sediment clouds. Moving carefully or staying above the bottom reduces them.

Client settings are in `config/deepdiving-client.toml`: breathing, filtering, ambience, particles, lamp appearance, shadow limits, HUD and reduced motion. Set `acoustics.soundFiltering` to false if another sound mod handles muffling; equipment sounds remain available. Gameplay settings are in `serverconfig/deepdiving-server.toml` inside the world folder, including difficulty, pressure curves, equipment ratings and gas use.

Illumination API is bundled. Fading Depths remains a separate darkness mod. Complementary Unbound, Reimagined and Euphoria Patches are shader integration targets; appearance still needs in-game validation for this beta. See `LIGHTING_PATCH.md` for the compatibility details.

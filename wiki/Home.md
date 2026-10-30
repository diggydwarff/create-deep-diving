# Create: Deep Diving Wiki

**1.0.0-rc.1 · Minecraft 1.21.1 · NeoForge · Create 6**

Deep Diving adds breathing equipment, Create-powered gas processing and underwater exploration tools. This wiki has **eight pages**, including this getting-started page. Examples use new-world Standard defaults; existing saves keep their configured values.

## Browse the wiki

| Page | Covers |
| --- | --- |
| [Equipment and controls](Equipment.md#cylinders-masks-fins-and-swimwear) | Cylinders, masks, fins, computer, lamps, knife, guide lines and shortcuts |
| [Diving mechanics](Diving.md) | Pressure, gas mixtures, conditions, ascent, stops and recovery |
| [Gas workshop and recipes](Gas-Workshop.md#machines-and-filling) | Machines, ports, automation, filling and all base recipes |
| [Maps, signs and dive logs](Exploration.md) | Depth charts, 16-map bundles, site markers, signs and recorded dives |
| [Configuration guide](Configuration.md#where-files-live) | File locations, difficulty/pressure modes and practical examples |
| [Complete settings reference](Config-Reference.md) | Every setting's default, allowed values, effect and interactions |
| [Help and compatibility](Help.md) | Troubleshooting, integrations, performance, commands, upgrades and glossary |

## Installation

Install matching RC1 builds on the client and server, with Java 21 and Create 6.0.x.

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

## Your first dive

Start with compressed air, a Create diving helmet, a filled worn cylinder and a computer.

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

For keybinds, see [Controls](Equipment.md#cylinders-masks-fins-and-swimwear). For confusing warnings, start with [Troubleshooting](Help.md#troubleshooting).

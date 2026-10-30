# Help, compatibility and reference

[Wiki home](Home.md) · RC1

Find a symptom, check an integration, review commands or look up a term.

## Contents

- [Troubleshooting](#troubleshooting)
- [Compatibility](#compatibility)
- [Effects and performance](#effects-and-performance)
- [Commands](#commands)
- [Upgrading](#upgrading)
- [Glossary](#glossary)

## Troubleshooting

Match the symptom to a check before changing a whole difficulty or pressure profile.

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

## Compatibility

Create is required. Deep Seas, gas providers, pressure mods and shader packs have distinct integration rules.

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

## Effects and performance

Breathing, acoustics, ambience, sediment and lamps have separate controls.

Supplied divers have equipment breathing sounds and an exhalation puff near the mouth followed by a smaller trail. Bubbles rise at varied speeds, stay behind as you swim past, and fade with age and near the surface. Their effect does not charge extra breathing gas. Empty supply, missing helmet, dry air and breathing bypasses stop ordinary cylinder exhaust.

Underwater acoustics blend air/water filtering, equipment breathing and nearby diver sounds. Dry air pockets restore ordinary acoustics. A subtle depth-dependent ambience and sparse marine snow provide environmental detail; rare luminous specks have their own switch.

Mining loose seabed material and moving close to the bottom can raise sediment clouds. Staying above the seabed or moving carefully reduces disturbance, and fins reduce newly stirred sediment. Cloud density also affects lamp visibility.

For lower GPU cost, start with **Diving lamps → Reduced effects**, fewer nearby lights and a smaller shadow budget. Reduce marine-snow/sediment particle limits if needed. Disable `acoustics.soundFiltering` when another sound mod owns muffling; breathing sounds and exhaust have independent switches. **Reduced motion** disables camera sway and animated distortion while retaining readable condition cues.

Client cosmetic settings do not disable the server's diving conditions or damage. The detailed computer and recovery guide remain the place to identify the cause of impaired vision or movement.

## Commands

Operator helpers change the executing player. Client previews demonstrate visuals without changing gameplay.

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

## Upgrading

RC1 uses protocol 14. Existing world settings survive an update and can differ from new-world defaults.

- RC1 uses network protocol **14**. Use matching Deep Diving builds on client and server, and remove duplicate older JARs.
- Existing Diving Regulators remain loadable but are no longer required, crafted normally, or shown in the creative tab. Recycle old ones into Precision Mechanisms.
- Older colored plain-goggle and separate-snorkel IDs resolve to the corresponding combined snorkeling mask.
- Retired niche pictogram panels remain loadable and can be re-cut into the core set.
- Existing world configuration values remain in force. Review old ascent and pressure settings when adopting RC1's defaults.
- The advanced gas machines use native recipe serializers. A custom JEI/EMI gas-machine category is not included; the in-game Ponder scenes and this workshop guide explain their operation.
- Thermal wetsuits/Cold Sweat insulation, diving bells, breathing hoses, rebreathers, atmospheric suits and a larger swimming-animation overhaul are not part of this RC1 feature set.
- Moving Deep Seas/Sable assemblies, older declared-minimum loader combinations, hardware performance and extended expeditions should be validated in the actual modpack before treating them as certified release combinations.

This wiki covers implemented RC1 behavior. It is a player and server-configuration guide to the game's mechanics, not a real-world diving planner.

## Glossary

A quick reference for the labels in the computer, item tooltips and configuration.

| Term | Meaning in Deep Diving |
| --- | --- |
| Depth | Blocks below the water surface, measured from the eyes; not the same as world Y |
| atm | Absolute modeled pressure; surface air is 1 atm |
| Partial pressure | Ambient pressure multiplied by a gas's fraction; used for oxygen exposure and nitrogen narcosis |
| mB | Cylinder/machine gas quantity; Create backtank air uses its own units |
| Capacity | Amount a cylinder can hold; separate from its pressure rating |
| Pressure rating | Protection of the worn helmet/chest equipment against ambient pressure |
| Backup enabled | A carried cylinder is allowed to supply breathing gas when selected by the priority rules |
| NDL | No-decompression-time forecast at the current supply/pressure, not a guarantee for an entire route |
| Ceiling / required stop | Shallowest currently permitted decompression depth; a larger depth number is deeper |
| NEXT | Forecast for clearing the next three-block stop step at the actual breathing conditions |
| Tissue loading | The simplified model's retained nitrogen/helium exposure; persists beyond a single dive |
| Off-gassing | Release of retained inert gas when the current pressure/mix permits it |
| Hypoxia | Inadequate oxygen in the game's reserve/exposure model |
| Oxygen toxicity | Accumulated excessive oxygen exposure |
| Narcosis | Condition driven by nitrogen partial pressure |
| Barotrauma / pressure injury | The mod's equipment-overload/abrupt-compression injury model |
| LINEAR | Pressure mode using a fixed number of blocks per added atmosphere |
| REFERENCE | Pressure mode reaching a chosen pressure at a chosen depth, with a configurable curve |
| LOCAL_WATER | Surface mode that detects the local loaded water column |
| FIXED | Surface mode measuring submerged depth below a configured world-Y reference |
| HUD | The small on-screen computer readout; separate from the full computer screen |
| Marine snow | Cosmetic suspended water particles; separate from seabed sediment |
| Sediment | Temporary plumes created by eligible mining/bottom movement |
| Base recipe | A shipped core recipe before color variants, panel re-cutting and legacy recycling |

All numbers and thresholds describe the game. This wiki is not a real-world dive planner.

[Back to wiki home](Home.md)

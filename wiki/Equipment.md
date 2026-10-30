# Equipment and controls

[Wiki home](Home.md) · RC1

Choose breathing gear, read the computer and use the exploration tools. Capacity, protection and mixture limits are separate.

## Contents

- [Cylinders, masks, fins and swimwear](#cylinders-masks-fins-and-swimwear)
- [Dive computer](#dive-computer)
- [Diving lamps](#diving-lamps)
- [Knife and guide lines](#knife-and-guide-lines)
- [Controls](#controls)

## Cylinders, masks, fins and swimwear

Capacity, pressure protection and breathing-gas suitability are three separate limits.

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

## Dive computer

Use the HUD for immediate guidance and open the computer for every warning and recovery page.

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

## Diving lamps

Choose a beam profile, toggle each lamp independently and tune its rendering on your client.

Each lamp is reusable and has its own on/off setting. Hold an enabled lamp to aim it; stowed enabled lamps provide downward illumination. Active held lamps take priority over stowed spares. Nearby divers' lamps can also be visible when enabled in client settings.

| Lamp | Beam | Reach multiplier |
| --- | --- | ---: |
| Diving Lamp | Balanced general-purpose beam | 1.00× |
| Wide-beam Diving Lamp | Broad flood light | 0.70× |
| Long-throw Diving Lamp | Narrow, longer-reaching spot | 1.80× |
| Inspection Diving Lamp | Very wide close-work light | 0.45× |

Multipliers apply to the configured lamp range, subject to the renderer's budgets. Lamps need no separate battery. Sediment can attenuate their light. Client options cover warm/cool/custom color, brightness, range, volumetric appearance, shadows and nearby-light limits.

## Knife and guide lines

Mark a route with connected Guide Nodes and cut or illuminate the spans when needed.

### Dive Knife

The knife cuts guide lines, quickly clears underwater plants, and provides shears-style harvesting for seagrass. It also works on kelp, vines and cobwebs and adds **3 damage** against aquatic targets. Normal durability and block-protection rules apply.

### Guide lines

Place Guide Nodes on supported floors, walls or ceilings. Use a colored guide line on the first node, then a second loaded node within **64 blocks**. One successful connection consumes one line outside Creative. Add intermediate nodes for longer routes.

Sneak-use a line in open space to clear an unfinished anchor selection. Aim at a placed span and use glow ink to coat it, or a Dive Knife/shears to cut it. Cutting refunds the line, including its glow coating. Guide lines are navigation aids; they are not a breathing hose or a restraint tether.

## Controls

Look up equipment actions, the depth-hold key, map paging and camera controls.

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

[Back to wiki home](Home.md)

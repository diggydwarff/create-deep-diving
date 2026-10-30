# Gas workshop and recipes

[Wiki home](Home.md) · RC1

Build a filling station, automate individual gases and look up all 24 base recipes.

## Contents

- [Machines and filling](#machines-and-filling)
- [Recipe reference](#recipe-reference)

## Machines and filling

Compress air, separate oxygen/nitrogen, store individual gases and fill cylinders in the blender.

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

## Recipe reference

All 24 shipped base recipes are listed below; cylinder mechanical-crafting layouts are included.

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

[Back to wiki home](Home.md)

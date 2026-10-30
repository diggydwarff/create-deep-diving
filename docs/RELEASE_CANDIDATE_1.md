# Create: Deep Diving 1.0.0-rc.1

Minecraft 1.21.1 / NeoForge. This is release candidate 1; no official release has been published. Install matching RC1 builds on both sides. Protocol 14 accounts for the new map-decoration and exhaust-particle registrations.

## Visual refinements

- The supplied `art/items/masksnorkel.png` is the exact default item sprite. All 16 dye variants retain its silhouette, glass, copper and dark rubber; only the colored trim changes. Worn frames and snorkel tubes use matching trim, with copper tips. Existing separate-gear aliases still resolve to combined masks.
- The creative-tab icon is the blue fins.
- The map bundle is a single stitched chart case with flush teal slots. Its top and bottom use one cover rather than a stock UI texture wrapped around a second book frame. The 296×238 layout fits the minimum 320×240 GUI.
- Editable dive-site signs use a dedicated metal material and item icon, while retaining standing/wall placement, waterlogging, editable text, dye, ink and wax. Existing blocks retain their IDs and text.
- Dive sites use a compact high-contrast custom marker. Ordinary orange banners keep their own icon. Sites retain their names, save/load identity, frame display, cartography scaling and locking. Older site banners migrate when their loaded sign is observed; unloaded chunks are not forced to load.
- The dive log has a distinct waterproof teal cover, copper corner guards, stitched edges, brass binding and lightly ruled pages. Its 16×16 item sprite matches the new journal treatment. The original supplied log icon remains archived in `art/items`.

The marker display bug came from the map renderer's depth direction: decorations were behind the chart image in the bundle. The GUI now reverses that depth direction, putting icons and labels above the map. A newly selected page also establishes map tracking before sending its updates.

## Exhaust bubbles

Breathing still uses the existing server-controlled inhale/exhale phases and gas consumption. The latest follow-up uses a small opening burst and sparse trail, with varied per-particle ascent, drift, size and 8–15-second lifetimes. Bubbles fade during their final 2–4 seconds or as they approach a nearby water surface, dissolving at different heights below it. They remain behind while the diver swims away. The short surface search uses loaded client water cells only.

Passive fins also reject stale resting targets and have gentler vertical motion. See [bubble and buoyancy refinements](BUBBLE_BUOYANCY.md) for the current behavior and validation. The initial RC1 reports below retain their original scope and artifact checksum.

## Validation

The native RC1 probe uses a real integrated server, actual item interactions, container-button packets and map updates. It opens a two-map bundle, checks both pages at normal and minimum GUI sizes, and checks the rendered marker pixels above the chart image. It also checks the blue-fins creative icon, renders the metal signs and item models, and exercises real client particles for sustained ascent, expiry and surface/dry-air removal.

The screen/Ponder gallery covers populated, empty and warning-filled journals, map bundles and their hover cards at both sizes. The dedicated-server suite covers saved map data, named markers, legacy migration, copying/scaling/locking, inventories and the preceding diving regressions. Static asset checks cover the supplied mask's exact pixels, every dye silhouette, fixed-material pixels, worn models and sprite dimensions.

Opt-in native probes use the disposable development save `exploration-audit`; do not run them against an important world. The probes and test mods stay outside the production JAR. Native captures use software rendering and do not establish GPU performance or additional moving-ship compatibility.

Final results: [native bundle/particles/items](qa/rc1-refinement-results.txt), [52 screens and seven Ponders](qa/rc1-polish-results.txt), [dedicated server](qa/rc1-refinement-server.txt), and [production build/asset/package checks](qa/rc1-refinement-build.txt). All passed. The six integrated-test captures and 52 gallery captures are included under `docs/qa/rc1-*`.

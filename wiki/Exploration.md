# Maps, signs and dive logs

[Wiki home](Home.md) · RC1

Survey dive sites, carry several charts, mark routes and keep a dive history.

## Contents

- [Depth maps](#depth-maps)
- [Map bundles](#map-bundles)
- [Site signs and markers](#site-signs-and-markers)
- [Dive Log Book](#dive-log-book)

## Depth maps

Hold an initialized chart to survey loaded surface-water terrain; use a bundle to carry several.

### Surveying

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

## Map bundles

Store up to 16 initialized depth maps and flip between them. Only the selected page surveys.

A bundle contains up to **16 actual Depth Map stacks**, one per chart slot.

1. Initialize each new Depth Map before inserting it.
2. Open the bundle. Place charts normally or shift-click them into its chart slots; remove them with ordinary inventory controls.
3. Browse populated pages with the buttons or Left/Right keys. Empty slots are skipped and the selected page is saved.
4. Hold the closed bundle to survey its selected page. Other pages keep their prior survey.
5. Use the bundle on a named dive-site sign or panel to toggle that site's marker on the selected page.

The bundle preserves chart names, map IDs, zoom, lock state and markers. Inserting/removing maps does not copy them. It accepts initialized Depth Maps, including their retained chart data; vanilla terrain maps, uninitialized maps, other items and nested bundles are not accepted.

Remove an individual chart before using a cartography table or item frame. The held bundle is secured while its inventory is open.

## Site signs and markers

Name a site before marking it. A bundle adds the marker to its selected map page.

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

Start and finish a session manually while carrying the book and a Diving Computer.

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

[Back to wiki home](Home.md)

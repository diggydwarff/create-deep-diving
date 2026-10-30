# Dive sites and depth maps — 1.0.0-rc.1

## Site signs

Craft a **Dive Site Sign** from an oak sign, compass and blue dye. Place it on a floor or wall, including underwater. It uses normal oak-sign rendering, editing, dyes, glow ink and waxing.

Write the site name on the **front top line**. The other lines and back remain available for notes. Use an initialized Depth Map or a normal filled terrain map on the sign to add its orange banner-style marker. Use the same map on the same sign again to remove it. An offhand map works with an empty main hand. Each saved map tracks its own sites; placing a sign does not reveal it automatically on every map.

The marker uses only the filtered plain text of the front first line. The map must cover the sign's position and belong to the same dimension. Blank names, locked maps and the vanilla tracked-marker limit are handled without changing the sign or consuming the map.

While an unlocked map is carried or displayed in a tracked item frame, sign-name changes refresh about once a second. Breaking a loaded site sign removes its marker. Unloaded sites remain recorded and their chunks are never loaded just to check them. Locked depth maps retain their site-marker snapshot.

## Cartography table

Open a blank Depth Map once before putting it in the table's upper slot.

| Lower slot | Result |
|---|---|
| Paper | A new depth map one zoom level wider, up to scale 4 |
| Empty vanilla map | Two depth maps sharing the original map ID, colors and sites |
| Glass pane | A new locked snapshot with the current colors and site names |

Zooming retains a depth chart's known markers within its new bounds and starts a fresh survey at the wider scale. It leaves the input map's saved data unchanged. Copies intentionally share saved data, as normal map copies do. Depth maps stack up to 64, allowing the two-map copy result to behave normally. Both insertion and result collection support shift-click. Opening or sneak-zooming a stacked map processes one item and leaves the other copies unchanged. Locked maps can be copied but cannot be enlarged or locked again.

Sneak-use zooming remains available and retains depth-chart markers too. Named ordinary banners also remain usable on depth maps through the existing map interaction.

## Display

Depth maps use Minecraft's native held-map and item-frame rendering, including glow item frames, rotation, player/frame markers and map update packets. Site icons and names use the native banner marker path so they appear on framed maps as well as held maps and cartography previews. Frames display the saved survey; holding an unlocked chart surveys terrain.

## Equipment cleanup

The equipment catalog now contains **48 colored wearables**: 16 fins, 16 combined snorkel masks and 16 swimsuit trousers. Standalone snorkels and plain goggles have been removed along with their recipes, models and textures. Their old item IDs are migration aliases to the same-color combined mask, preserving old saved stacks without keeping duplicate catalog entries.

## Checks

`./gradlew -Pqa verifyServerProbe` exercises real sign/map serialization, label packets, name refresh/removal, unloaded sites, cartography insertion/copy/zoom/lock/shift-click, frame insertion/rotation, and all 32 legacy equipment aliases. The existing gas, log and survey checks still run.

`./gradlew -Pqa verifyClientProbe` checks the 48 remaining wearable models and journal. `./gradlew -Pqa -PsiteQa verifyClientProbe` opens the disposable `exploration-audit` client save, builds a review room, and captures held maps, normal/glow/rotated frames, signs and the three cartography previews. Do not run visual probes against an important save. Reports and captures are in `docs/qa/`.

Live Deep Seas/Sable vessel and extended multiplayer tests remain separate integration work.

## Multiple chart pages

The [Dive Map Bundle](MAP_BUNDLE.md) stores up to 16 initialized depth maps. Its selected page retains normal sign-marker behavior. Take a page out for cartography-table work or item-frame display.

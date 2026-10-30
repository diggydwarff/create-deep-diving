# Dive sign panels — 1.0.0-beta.3

The available set is **ten pictograms and one blank panel**, all drawn on an exact **16×16 pixel grid** like normal Minecraft blocks and Trailbound signs. The thin marine metal plates retain wall, floor, ceiling and fence mounts.

| Use | Available designs |
|---|---|
| Sites | Dive Site, Diver Down, Cave Entrance, Wreck, Dive Training Pool |
| Warnings | Danger / Turn Back (simple hooded skull), Strong Current, Do Not Enter |
| Navigation and facilities | Exit / Surface, Gas Filling |

The niche training, equipment and duplicate warning panels are no longer shown in the creative tab or produced by stonecutting. Their stable IDs, models, drops and saved settings remain registered so older worlds retain placed panels. Their low-resolution artwork uses the closest core pictogram. Existing retired panels are valid stonecutter inputs.

## Crafting and use

Craft four blank panels with three iron nuggets across the top row, then copper ingot / blue dye / copper ingot below. Put a panel into a stonecutter to choose a pictogram. Re-cutting another design preserves its custom caption, arrow, ink and anvil name, and consumes one input for one output.

Mount panels on solid walls, floors, ceilings or fences. Horizontal fence mounts sit flush against the post, including fences supplied by mods using the normal fence class or tag. Panels are waterloggable and have no movement collision. Removing their backing drops the configured panel and leaves source water in place when waterlogged.

- **Empty-hand use:** open the caption editor. Leaving the caption empty displays only the pictogram.
- **Sneak-use with an empty hand:** rotate the panel by a quarter turn. Graphics, caption and direction arrow rotate together.
- **Caption editor:** choose a caption of up to 32 characters, one of the 16 caption colors, and no arrow / left / right / up / down. Its preview uses the actual sign texture. Cancel leaves the sign unchanged.
- **Dye:** recolor an existing caption directly.
- **Glow ink sac:** make the pictogram, caption and arrow readable in darkness. This does not emit block light.
- **Ink sac:** remove glow ink.

Captions and ink survive world save/load, breaking, re-placement, creative pick and stonecutter design changes. Panels have no block-entity ticker.

## Map markers

The original writable **Dive Site Sign** keeps its existing behavior. The new graphical **Dive Site Panel** can also mark maps: give it a custom caption, then use an initialized depth map or a filled terrain map on it. Use again to remove the marker. Its custom caption becomes the marker name; default titles are not used as site names.

Other pictogram designs are local information panels. Existing map zoom/copy/lock, marker refresh and item-frame behavior are shared with the earlier site-sign system. See [MAP_SITES.md](MAP_SITES.md).

## Artwork and contributors

`art/dive_signs/catalog.json` holds stable identifiers, display names, default captions and color families. `symbols.svg` holds the editable pixel-grid pictograms. `tools/generate_dive_signs.py` exports the exact game textures, glow overlays, block/item models, recipes, translations and review sheet. Do not hand-edit its generated outputs; edit the SVG/catalog or generator, regenerate, and review the results.

```sh
pip install -r tools/requirements-signs.txt
python tools/generate_dive_signs.py
python tools/verify_resources.py
python tools/verify_dive_signs.py
./gradlew build
./gradlew -Pqa verifyServerProbe
```

The resource checker verifies all ten distinct available pictograms, original SVG IDs, 16px image sizes and hard alpha edges, selective glow, every model orientation, recipes and at least 6:1 contrast between the main symbol and its enamel field. Runtime tests cover actual block methods, fence attachment, waterlogging, serialization, drops, stonecutter transactions, same-item input swaps, edit tokens and map labels.

For native visual checks, use a disposable client save named `exploration-audit` and run `./gradlew -Pqa -PpanelQa verifyClientProbe`. The probe builds review rooms, captures the gallery, lit/dark underwater signs, fence rotations, floor/ceiling signs, the editor at normal/small window sizes, a real saved caption and the stonecutter screen. It modifies that development save and exits automatically. Reports and screenshots are in `docs/qa/`.

Trailbound's MIT mounting implementation was adapted for the panels; its copyright/license is retained in `src/main/resources/META-INF/licenses/trailbound-signs-MIT.txt`. The pictograms are original pixel-grid vector artwork. Protocol **13** requires matching client and server builds. Live Deep Seas/Sable vessels, extended multiplayer and third-party shader combinations remain separate playtesting work.

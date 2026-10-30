# Exploration review — 1.0.0-beta.2-exploration.1

Reviewed on October 28, 2026. This revision fixes issues found in the exploration update and makes its code and checks easier to maintain. Install this revision on both client and server: the network protocol is now **11**. Existing item IDs and saved books/maps remain compatible.

## Corrections

- A delayed journal refresh could reopen a book after Escape. Opening and refreshing now have separate packet semantics, and refreshes must match the open book and hand.
- The last partial second of a dive was omitted from underwater time and depth averages. A shared statistics helper now integrates it once and distinguishes an unsampled average from zero depth.
- Returning an interrupted book could add an invented final second. Interrupted/transferred sessions now retain their last confirmed statistics. Dropping a recording book seals the actual dropped stack.
- Dive numbers could repeat once the 32-entry history filled. Each book now retains a lifetime counter while limiting stored entries to 32.
- Two held charts could compete for the entire survey budget. Distinct maps now share 64 samples per player per tick; duplicate map IDs receive one update.
- Water at the world's minimum build height could be treated as land. Soundings now include that boundary.
- White depth labels were difficult to read in shallow water. Labels now use dark or light ink according to the underlying depth band.
- Mekanism chemical matches and missing-tag results could survive a data reload. Reloads now clear the cache.
- A newly observed stowed lamp could briefly start with a forward-facing beam. Its transition now starts at the actual carry position.

New Java code uses consistent formatting, normal imports and comments explaining ownership and limits. Asset generators have descriptive shared names and no longer write a knowingly invalid advancement before replacing it. Regeneration preserved the existing PNG assets byte for byte.

## Validation

Runtime: Java 21, Minecraft 1.21.1, NeoForge 21.1.238 and Create 6.0.10-280. Optional chemical tests use Mekanism 10.7.19.85. Native rendering used Mesa software OpenGL through a virtual display.

| Check | Coverage |
|---|---|
| Production build | Compilation, packaging, simulation tests and pinned Illumination API checksum |
| Numerical tests | 50,500 mixture transactions; tissue recovery; final partial-second logging; clock reset and session bounds; complete single/dual chart traversal; glyph and lamp-profile bounds |
| Resources | 485 production JSON files, internal texture/model references, translations and all 80 wearable variants |
| Server without optional mods | 80 real dye recipes; equipment slots; tagged oxygen; fluid transaction conservation; bounded chart sampling and persistence; log recording, numbering, dropped books and history |
| Server with Mekanism | Real chemical capabilities; oxygen simulation/execution; hydrogen rejection; large quantities; directional ports; a real resource reload and cache refresh |
| Native client | All 80 item models; readable journal; matching-book refresh and delayed-reply regression checks |
| Integrated world | Worn fins/mask/trousers; four active lamp profiles and their range; vanilla held-map rendering with depth bands and labels |

Reports and native screenshots are in [qa/](qa/). The held-map visual fixture uses synthetic bathymetry so the test room's roof cannot overwrite it; actual world soundings are tested on the server.

These checks do not establish that the mod is bug free. Moving Deep Seas/Sable vessels, long multiplayer sessions and third-party shader packs still need live modpack testing. Deep Seas oxygen compatibility is covered by common-tag fixtures, not a running Deep Seas installation. Sessions stored in a container stop being sampled; an interrupted session is sealed when the book returns.

## Repeating the checks

```sh
./gradlew build
python tools/verify_resources.py
./gradlew -Pqa verifyServerProbe
./gradlew -Pqa verifyClientProbe
```

The Python resource checker requires Pillow. The server probe needs the normal development EULA/configuration setup. It modifies `run/server/world` and stops automatically. Use a disposable development world.

For chemical tests, place Mekanism 10.7.19.85 in `qa-libs/` and run `./gradlew -Pqa -PmekanismQa verifyServerProbe`. Optional QA JARs are not included in the release.

For the integrated visual probe, first create a disposable client save named `exploration-audit`, then run `./gradlew -Pqa -PworldQa verifyClientProbe`. The probe builds an underwater review room in that save, equips the test player, captures six screenshots and exits. The ordinary asset/journal probe needs no save. Reports are removed before each run so an earlier success cannot hide a failed startup. All QA classes and fixtures remain outside the production JAR.

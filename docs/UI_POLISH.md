# Ponder and interface polish — beta.3

This follow-up keeps version **1.0.0-beta.3** and protocol **13**. It follows the [release QA audit](RELEASE_QA.md); the earlier reports and checksums identify that earlier build.

## Ponder tutorials

The Deep Diving entry in Ponder's index groups seven tutorials. They are also available from the corresponding inventory items using the normal Ponder key.

| Tutorial | What changed |
| --- | --- |
| Compressor | Reveals the drive and storage in stages; distinguishes the dry intake from gas extraction and cylinder collection. |
| Separator | Highlights the actual blue oxygen and green nitrogen connections independently; explains 21/79 separation and blocked outputs. |
| Blending station | Demonstrates each input separately, then mixture selection, emptying a cylinder and useful partial fills. |
| Storage | Shows helium storage, cartridge use, goggles/Display Links and wrench pickup. |
| Equipment | Uses an actual filled Deep Diving cylinder in the demonstration instead of a Create backtank; covers regulator, enabled inventory backups and computer use. |
| Map bundles and site markers | New: initialize a map, insert charts, change pages, survey the selected chart and mark a named site. |
| Dive log | New: start/finish a session, keep the book and computer with you, and review the latest 32 dives. |

Each instruction has a timeline keyframe. Longer captions receive more reading time. Scene captions and English translations are synchronized. Small original structures are reproducible with `python3 tools/generate_ponder_scenes.py`.

Port descriptions use their visible blue/green markings so the instructions do not depend on which side of a rotated machine the player views.

## Screens

- **Machines:** mark the selected preset, color every blocked/paused state consistently and disable Purge when the input has no gas. The tooltip explains that purging discards gas. Port/buffer help stays on the status strip; the depth-range tooltip stays on the depth row, leaving the mixture controls clear.
- **Dive log:** use Create button chrome, display the history page count, support Left/Right paging and show recorded warning types. This counts distinct warning kinds encountered, not repeated incidents. Hover the warning row for their names; hover shortened statistics for the complete text.
- **Map bundle:** display an ellipsis on long names and show the complete name, map ID, scale and lock state in a compact hover card. Empty chart slots explain that a new map must be initialized before insertion.
- **Sign editor:** use matching Create controls, add a caption length counter and give color swatches readable control names.
- **Computer:** show one recovery condition per page with distinct Cause and Recovery sections, retain live readings and support Left/Right navigation. The guide and alarm buttons show their active state.

## Validation

The opt-in native probe opens a disposable `exploration-audit` world, compiles and runs every instruction in all seven Ponders, and checks their keyframes. It verifies the equipment stand/placed cylinder and the named site-marker instruction. It renders the machines, journal states, map-bundle states, sign editor, live computer, all six recovery pages and all seven Ponders at both normal and minimum GUI sizes, checking visible widget and inventory-slot bounds.

The screens in this visual probe use local fixtures. They exercise native rendering and interaction paths; they do not claim new multiplayer or server-packet coverage. The earlier bundle/server regression tests remain documented in the release audit. This follow-up also runs the production build, its numerical checks and the dedicated-server probe.

Results and selected screenshots are under `docs/qa` with the `polish-` prefix. Reproduce the client check with:

```sh
bash gradlew -Pqa -PpolishQa verifyClientProbe
```

Use a working graphical display and a disposable world. The production JAR excludes the QA source set. These changes add presentation and guidance; they do not implement thermal wetsuits, JEI/EMI machine pages or the planned swimming-animation overhaul.

Final results: [52 native screen checks and seven complete tutorials](qa/polish-native-results.txt), [actual sign-editor save/reopen and placement checks](qa/polish-panel-results.txt), [dedicated server](qa/polish-server-results.txt), and [production build/checksum/packaging](qa/polish-build-results.txt). All passed. Screenshots use GUI sizes 640×450 and 320×240; physical windows are 1280×900 and 640×480. These use software rendering and do not establish GPU performance or new live modpack compatibility.

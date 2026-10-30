# Dive-computer HUD sizing

This refinement remains `1.0.0-rc.1`, protocol 14. The new settings are client preferences.

Open **Mods → Create: Deep Diving → Config → Client settings**. **Compact HUD** is on by default and **HUD size** defaults to **Normal (100%)**. Disable Compact HUD for the preceding expanded readout. Existing horizontal and vertical offsets still position the panel; the scaled outer frame is clamped to the viewport, including Create's projecting header. Very small viewports reduce the requested size to fit.

| HUD size | Compact panel | Expanded panel |
| --- | --- | --- |
| Small (75%) | 138×84 | 156×117 |
| Normal (100%) | 184×112 | 208×156 |
| Large (125%) | 230×140 | 260×195 |
| Extra large (150%) | 276×168 | 312×234 |

Dimensions are GUI pixels including the full frame. Minecraft's overall GUI scale still determines physical pixel size. The default covers about 37% less area than the old HUD, with normal-size labels. It pairs depth and pressure, gas time and no-decompression/stop depth, then mixture/next-stop time and ascent rate. Ascent guidance and the highest-priority warning remain visible. A `+N` footer count indicates additional warnings; open the computer for their full details and recovery guidance.

Compact stop guidance retains the requested depth, prioritizes a violated ceiling and respects the server's ascent-warning grace. An unreached stop shows `NEXT --`; an unavailable supply shows `NO GAS`. Long numeric readings fit their column rather than losing digits. The full computer screen keeps its detailed layout and help pages.

## Validation

The native HUD probe exercises all four translated size choices, both layouts, undo and saving through the registered configuration screen. It checks the actual saved TOML, restores its prior preferences, measures all 4,096 warning combinations with the game font, and checks stop/ascent guidance. Geometry cases cover both layouts, every preset, extreme offsets and small viewports. Native captures verify every rendered HUD pixel stays inside its declared scaled frame at normal and minimum GUI sizes, including large warning readings and corner placement.

The normal build and dedicated-server suite cover common config loading and the existing simulation, breathing supply, recipe, chart and buoyancy regressions. Earlier release reports retain their original scope and build hashes.

Final results: [native HUD and configuration](qa/hud-native.txt), [dedicated server](qa/hud-server.txt), and [build and packaging](qa/hud-build.txt). All passed.

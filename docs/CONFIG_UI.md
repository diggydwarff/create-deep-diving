# Nautical configuration screen

Open **Mods > Create: Deep Diving > Config**. The screen uses Create's installed casing and button textures, a deep-teal background, brass trim, equipment icons and small depth-scale marks. The layout stays still and uses the normal Minecraft font size.

The two entry cards lead to client preferences and world rules. Nested sections and list editors share the same frame. A small status line detects whether Create: Deep Seas (`create_submarine`) is installed; this is an installation indicator, not a certification of moving-ship support.

## Editing

The screens extend NeoForge's configuration editor, preserving its validators, undo/reset history, save behavior, tooltips and restart handling. Readable labels replace raw camel-case keys. Existing TOML keys, defaults, paths, list schemas and gameplay behavior are unchanged. The current build is `1.0.0-rc.1`, protocol 14.

World settings are available while a private local world is loaded. The hub explains when they are unavailable, and follows the native editor's restrictions for remote servers and worlds opened to LAN. The availability check runs again when opening an editor. Server operators can continue using the existing TOML files.

The footer adapts to small GUI widths without changing the native undo, reset or Done actions. Long labels use Minecraft's normal scrolling/truncation behavior and full-label tooltips.

## Validation

The opt-in native probe (`-Pqa -PconfigQa verifyClientProbe`) opens the registered config factory and operates the actual widgets. It checks boolean and enum changes, numeric and list validation, undo/reset, real TOML saving, nested navigation, world availability and layout at GUI sizes 640×450 and 320×240. It uses a disposable development save named `exploration-audit`; do not run it against an important save. The probe restores the values it edits and is excluded from the distributed JAR.

Results and native screenshots are stored under `docs/qa/config-*`. The production build and dedicated-server checks cover the resulting artifact. Previous release and Ponder QA reports retain their original scope and checksums.

Final results: [native config workflow](qa/config-native-results.txt), [dedicated server](qa/config-server-results.txt), and [production build and packaging](qa/config-build-results.txt). All passed. Fifteen screenshots show normal and minimum GUI layouts, invalid edits and nested screens. The run uses software rendering; it does not establish GPU performance or additional live modpack compatibility.

# Contributor conventions

Use Java 21 and four-space indentation. Format changed Java code consistently; the exploration review used google-java-format 1.28.0 with `--aosp`. Python generators use Black formatting. Keep generated resources in source control so normal builds need no Python tooling.

Prefer names that describe state and units: `wetTicks`, `depthSum`, `original` and `accepted` make transaction and time accounting easier to review. Use ordinary imports rather than repeating long qualified names. Comments should explain ownership, constraints or API behavior that the code alone does not show.

Keep game state on the server. Accept player intent through packets and validate the current item identity before changing data. Keep optional-mod classes out of required signatures. Register compatibility behind installed-mod checks and retain a working absent-mod path.

When changing gas transfers, check simulated and executed operations, remainders, invalid gases and side directions. When changing persisted items, check save/load, copying or dropping, bounded history and legacy data. When changing rendering, inspect a native capture as well as resource references.

Use the focused probes in [AUDIT_NOTES.md](AUDIT_NOTES.md). Extend tests for a real failure or invariant; avoid tests that only repeat an implementation detail. QA-only registrations belong in `src/qa` and must not appear in the release JAR.

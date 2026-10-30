# 1.0.0-rc.1 candidate checks

Prepared October 29, 2026, from the complete exploration.3 source and matching JAR recovered for this task. This is the final planned 1.0.0 candidate, pending final player testing. It has not been published as the official release.

The candidate updates versioning, documentation and ascent-warning balance. It preserves registries, recipes, saved physiology, gas quantities, books, maps and network protocol 12. Back up the world before replacing an existing mod. Use the same candidate on both sides and remove the previous Deep Diving JAR.

## More forgiving ascent

The old warning triggered on a single sample above 0.75 blocks/second. The default is now 2.5 blocks/second. A continuous fast ascent must last at least three seconds and gain at least four blocks before raising an ascent warning. Slowing to the limit, stopping or descending resets the history. Entering/leaving water, dry compartments, invalid motion history and disabling the simulation also reset it; warning history is not saved across logins.

The numeric rate remains a live reading. The computer's warning text, bar color and alarm follow the server's sustained-ascent decision. Crossing a required decompression ceiling still shows descend-to-stop guidance; the grace period does not erase tissue loading or suspend decompression consequences.

For existing worlds, change `maximumAscentBlocksPerSecond` to `2.5` in the world's `serverconfig/deepdiving-server.toml` to adopt the new threshold. Existing custom limits are preserved. New settings `ascentWarningGraceSeconds = 3.0` and `ascentWarningMinimumRise = 4.0` fill in automatically. Use [the example](../config-examples/ascent-settings.toml) as values to merge, not a replacement for unrelated server settings. Setting both new tolerances to zero restores immediate warning behavior at the chosen limit.

## Verification evidence

Fresh candidate verification is recorded in [qa/rc1-validation.txt](qa/rc1-validation.txt). Other reports and screenshots in `qa/` were supplied with exploration.3 and describe that earlier build; they are retained as historical evidence. [AUDIT_NOTES.md](AUDIT_NOTES.md) also describes the earlier exploration audit, including the subsequently removed standalone gear.

## Final player testing

- Load a copy of an existing world and verify stored cylinders, machine inventories, physiology, log books, depth maps and placed signs.
- On a dedicated server, test a complete fill, equip, dive, decompression stop and return cycle. Check that one supply drains and another player's bubbles/pose remain visible.
- Swim up/down briefly at depth, then make a sustained fast ascent: only the sustained case should raise the ascent warning. Check that slowing clears it and that actual decompression-ceiling violations still take precedence.
- Test death, logout/rejoin, dimension travel, inventory backup cylinders and the combined snorkel mask at the surface.
- With the actual server pack, enter and leave a moving Deep Seas/Sable dry compartment; verify gas drain, pressure transitions and decompression obligations.
- Test the four lamps and symptom effects using the players' shader packs. Check camera transitions, orbit exit and depth hold under normal server latency.
- If using Mekanism, test oxygen transfer in both directions through the actual tube/pipe setup.

These are live acceptance checks, not claims that this candidate has passed those scenarios. Automated startup and fixture checks cannot establish full compatibility with moving ships, external shaders or a long multiplayer expedition.

## Scope and limits

Cold Sweat wetsuits and dedicated thermal protection are absent from the recovered source and are not included or claimed in this candidate. Swimsuit trousers are cosmetic. Keep the requested Cold Sweat equipment work on the backlog; test existing Cold Sweat behavior separately before extended dives on that server.

No diving bells, hoses, rebreathers, atmospheric suits, direct Sable physics control, CC peripherals or custom JEI/EMI machine categories are included. Existing swimming, treading, fin buoyancy and depth-hold behavior is retained. The larger movement and animation changes belong to the next feature update.

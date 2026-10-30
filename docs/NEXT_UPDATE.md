# Next feature update — diving animation and movement

Planned focus after 1.0.0: make divers move convincingly while keeping the controls responsive and familiar. These are design targets, not features included in 1.0.0-beta.3.

## Core work

- A stable horizontal scuba posture with a relaxed head and arms, bent knees and equipment aligned with the body.
- Distinct flutter and frog kicks, with a short glide after each stroke. Blend stroke strength with actual swim speed.
- Smooth transitions between surface swimming, descending, horizontal travel, hovering, ascending and standing on the bottom.
- Subtle breathing motion, slow sculling and restrained bobbing while hovering. Depth hold should remain controllable and respect decompression stops.
- Natural banking and body alignment during turns without forcing the first-person camera to roll.
- Better surface entry, equipment handling and recovery transitions, including clean exits when gear is removed or the player leaves water.

## Implementation boundaries

The server continues to own displacement, breathing, gas and depth-hold targets. Clients interpolate the visible pose between state updates. Separate cosmetic motion from actual drift so animation cannot change gas use, push a diver through a ceiling or fight player input.

Keep fins, masks, cylinders, held lamps and the outer skin layers aligned on normal and slim player models. Preserve orbit-camera aiming rules and interactions. Provide reduced-motion controls and a fallback for conflicting player-animation mods. Avoid a large new set of keybinds.

## Acceptance targets

Check first person, third person and another player's view at low and high frame rates. Test packet delay, start/stop/turn transitions, cave walls, shallow water, ladders, boats, flight, death, dimension travel and moving ship air pockets. Verify that no state leaves a player stuck in a swimming pose or carrying a stale movement modifier.

Build one complete movement loop first: swim, stop, hover, turn, resume, surface and stand. Refine that loop in-game before adding more animation variants.

## Separate follow-up

The previously requested Cold Sweat wetsuit/thermal equipment work remains outstanding in the recovered source. It should receive its own explicit implementation and compatibility checks; the animation work should not silently claim thermal protection.

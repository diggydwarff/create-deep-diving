# Dive Map Bundle

Craft with string / empty / string, leather / paper / leather, then three leather. The result is one reusable bundle with room for 16 depth maps, one per slot.

1. Use each new Depth Map once to initialize its survey before putting it in the bundle.
2. Use the bundle to open its chart pages and inventory. Shift-click a surveyed depth map into the chart slots, or place it in a slot normally. Remove maps with ordinary clicks or shift-click.
3. Use the previous/next buttons or Left/Right arrow keys to browse populated pages. Empty slots are skipped. The selected slot is outlined and the selection is saved on the bundle.
4. Hold the closed bundle to survey its selected map. Other pages retain their previous survey. Main-hand and offhand maps/bundles share the existing 64-sample-per-player tick budget, and duplicate map IDs are surveyed only once per tick.
5. Use the bundle on a named Dive Site Sign or graphical Dive Site Panel to add/remove a site marker on the selected page. Locked and wrong-dimension maps keep the normal restrictions.

These are actual stored map stacks, with the same vanilla map IDs and saved map data as the original items. Names, zoom, lock state and markers survive storage, save/load and removal. The bundle does not copy maps when inserting or removing them. Vanilla terrain maps, uninitialized depth maps, other items and nested bundles are not accepted.

Take a map out to zoom/copy/lock it in a cartography table or display it in an item frame. The bundle itself is a book-style viewer, not a cartography-table map.

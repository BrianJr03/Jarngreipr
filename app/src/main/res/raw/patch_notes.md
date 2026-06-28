**New**
- New page type: **Unified Canvas** — a single continuous grid that holds apps, folders, ROMs, widgets, and an optional RSS launcher tile together
- Scroll orientation toggles between horizontal and vertical per page, with configurable columns and rows
- Add apps, folders, ROMs, widgets, and an RSS launcher tile from one picker; long-press to drag tiles to a new spot or release in place to remove
- Real widget rendering via the system AppWidgetHost; widgets keep their multi-cell spans
- Canvas layouts survive backup/restore via Settings → Import/Export
- **Canvas grid rewrite**: tiles now live at absolute (col, row) coordinates instead of flowing in list order. Drag and resize push overlapped neighbors into the nearest free cells along the scroll axis, cascading deterministically — same gesture, same result, every time. Backing off a drag or resize mid-gesture returns displaced neighbors to where they started.
- **Resize handle on every tile** in Edit Mode (bottom-right corner). Drag the handle to resize; tap (or focus + select on D-pad/gamepad) to open the stepper dialog. Widgets honor their declared minimum size and can't be shrunk below it.
- **Tidy** action in the canvas edit dialog: closes the gaps left by moves, deletes, and resizes. Gaps no longer auto-fill — Tidy is the only path that compacts the layout.

**Fixes**
- **Unified Canvas — items added in one scroll orientation now appear in the other.** Vertical and horizontal grids each keep their own independent layout over a shared set of items, so adding, removing, moving, or resizing in one orientation no longer hides items from the other. Existing layouts are migrated on first launch — your current positions are preserved, and items previously missing from one orientation come back.
- Multi-disc games using ES-DE's "directories interpreted as files" convention (`.m3u`, `.cue`, `.gdi`, `.ps3`, etc.) now display their scraped background, logo, and video instead of a black screen
- Fix applies to both the in-app ROM browser and the live wallpaper that reacts to ES-DE's selection events

2.5.1
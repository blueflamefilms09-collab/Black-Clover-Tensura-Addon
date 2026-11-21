# Grimoire Slot rework

Owner request: "fix the grimoire slot to be something different and rework its UI because it's really laggy and buggy."

Setting (SERVER config, the mod's main server config, section `black_clover`): `grimoireSlotInventory`, default **true**.
`true` = the slot is a slot in the player's own inventory screen (E). `false` = exactly the old separate window (the menu, its screen,
key, status-screen button and command are all still there). Clients learn the mode at login (`ModePayload`), so a server that runs the
old mode shows nothing in the inventory screen; a config change reaches clients at their next login (a click against the wrong mode is
refused and answered with the right mode).

## What it does

### Lag (both modes)
Before: every tick, for every player, `ItemStack.matches(last, now)` (a deep compare of the big grimoire data) and two `copy()`s on change;
while the slot was empty also a `List.of(...)` and a scan of 37 inventory slots; a static `HashMap<UUID, ItemStack>` kept a copy per player.

Now (`blackclover/GrimoireSlot.java`, pure rules in `blackclover/SlotClickRules.java`):
- **Change signal**: a version counter bumped by `GrimoireSlot.set(...)` and by `SlotContainer.setChanged()` (`touch`). The tick sends the hip book
  (`HipPayload`) only while the version differs from the one last sent. One copy is made at send time and is both the packet content and the
  snapshot (never mutated).
- **Scan every 20 ticks**, staggered by `UUID.hashCode() mod 20`, and once on the first tick after login / respawn / dimension change.
  Moves the first owned grimoire (main inventory, then offhand) into the empty slot: the same behaviour as before, the stack itself moves (no copy).
- **Net for in-place edits**: on the scan tick only, one `ItemStack.matches(snapshot, now)`; a stack edited in place (no `set`) is still caught within a second.
- State is an attachment on the player (`grimoire_slot_state`, not serialized): it is per player, gone with the entity (a respawn starts fresh and
  resends), reset on logout / respawn / dimension change. No static map, nothing to leak for stand-in players.
- The perf probe `s.book.grimoire_slot` is kept.
- Old window (`GrimoireSlotScreen`, setting off): the Great-Sage readout was rebuilt every frame (3 to 5 `Component.translatable(...).getString()` formats,
  the status read, `SageBlock.set` string compares). It is now built only when the slot content or the status tag (by identity) changes; per frame
  it draws the panel, the magic circle (7 textured quads, fixed cost, scissor kept) and the cached text.

### Something different (inventory mode)
`client/grimoire/GrimoireSlotInventoryClient.java`, no mixin (events only):
- `ContainerScreenEvent.Render.Background` on `InventoryScreen`: a 20x20 gold-framed box at (77, 40) relative to the screen's top-left, above the
  offhand slot and right of the player model (it follows the recipe book shift because it uses `getGuiLeft/Top`). Shows the book from the client copy
  (`GrimoireFloatClient.hip`, read once per client tick) or a dimmed book icon when empty; white hover highlight.
- `ScreenEvent.Render.Post`: tooltip on hover (the item's own tooltip plus a hint, or "no grimoire is bound"); none while the cursor carries an item.
- `ScreenEvent.MouseButtonPressed.Pre` / `Released.Pre`: a click on the box is cancelled (no vanilla click there) and sent as
  `SlotClickPayload(button, shift)` (new C2S payload `grimoire_slot_click`).
- Server (`GrimoireSlot.click`): validates (inventory mode on, sender is a `ServerPlayer`, alive, not spectator, `containerMenu == inventoryMenu`,
  button 0/1, rate limit 8 clicks per 20 ticks per player), then applies `SlotClickRules.decide(cursorOwned, cursorEmpty, slotEmpty, shift, button)`:
  | cursor | slot | action |
  |---|---|---|
  | empty | full | TAKE: book goes to the cursor |
  | own book | empty | PLACE: exactly one of the cursor stack |
  | own book (count 1) | full | SWAP through the cursor |
  | foreign item / foreign grimoire | any | nothing |
  | empty, shift | full | TO_INVENTORY (only if there is room) |
  | empty, shift | empty | FROM_INVENTORY: pulls an owned grimoire from the inventory into the slot |
  Then `menu.broadcastChanges()` (carried item and inventory slots) and an immediate `HipPayload` (`syncNow`).
- After the owner takes the book out on purpose (TAKE / TO_INVENTORY) the scan leaves the inventory alone for 10 s (200 ticks), otherwise the
  scan would pull the book straight back in. This exists only in inventory mode (the old window keeps the old immediate re-absorb).
- Key / status-screen button / `/multiverse slot`: `GrimoireSlot.open(p)`. Inventory mode: server sends `ModePayload(true, open=true)` and the client
  opens its own `InventoryScreen` (the server cannot open the player's inventory screen itself; E is client side). Off: the old window.
  `OpenPayload` is now rate limited too (8 per second).
- Creative mode: the creative inventory is a different screen and does not show the box; the survival `InventoryScreen` opened by the key does.

## New / replaced / kept
- NEW: inventory-screen slot, `SlotClickPayload`, `ModePayload`, `SlotClickRules`, `GrimoireSlotSelfTests`, server setting `grimoireSlotInventory`, 3 lang keys
  (`tools/lang/grimoire_slot.json`), `docs/grimoire_slot_rework.md`.
- REPLACED (both modes, perf only): the per-tick compare / scan / static map of `GrimoireSlot`, the per-frame readout of `GrimoireSlotScreen`.
- KEPT: the `grimoire_slot` attachment (same id, copy on death), the old `Menu`, `SlotContainer`, `GrimoireSlotScreen`, the key, the status-screen button, the
  command, `HipPayload`, `OpenPayload`, every public method other code uses (`get`, `set`, `ready`, `anyReady`, `holdsOwn`, `replaceOwned`, `open`).

## Bug audit
"Fixed" = changed in code; "read only" = found or cleared by reading the code, not reproduced in a game (none could be run here).

| # | Finding | Result |
|---|---|---|
| 1 | Per-tick deep `ItemStack.matches` of the grimoire plus two `copy()` on change | fixed (change signal + one compare per second) |
| 2 | Per-tick scan of 37 slots and a `List.of` allocation while the slot is empty | fixed (every 20 ticks, staggered, no allocation) |
| 3 | Static `SENT` map kept a stack copy per player (leaks for players who never log out, e.g. stand-ins) | fixed (attachment state) |
| 4 | `SlotContainer.setChanged()` did nothing; vanilla edits the stored stack in place (`shrink`, `split`) and calls `setChanged` | fixed (`touch`); before, only the per-tick compare caught it, so no visible bug, read only |
| 5 | No immediate hip sync after a menu change (it waited for the next tick compare) | click path now syncs at once; the old window still syncs on the next tick (50 ms), read only |
| 6 | `SlotContainer.removeItem` made two copies and stored a count-0 stack as the remainder | fixed (stored stack leaves itself, `set` normalizes empty to `EMPTY`) |
| 7 | `SlotContainer.setItem` copied the stack | not a dupe (every vanilla path hands the stack over); the copy is gone so the stored stack is the stack the menu moved, read only |
| 8 | Double-click collect (`canTakeItemForPickAll`) could pull from slot 0 | defensive fix; unreachable with real items because `GrimoireItem` is `stacksTo(1)`, read only |
| 9 | Stack of more than one placed | window: vanilla places one because `Slot.getMaxStackSize()` is 1, read only. Inventory mode: places exactly one, swap refused when the cursor stack is larger (pure rule + self-test `slot.slot`) |
| 10 | Someone else's grimoire placed | window: `Slot.mayPlace` rejects it, read only. `SlotContainer.setItem` (API) has no check, left as is because other code calls `set` with canon books. Inventory mode: the server rule rejects it (`decide`, self-test `slot.slot`) |
| 11 | Shift-click both ways in the window | read: `quickMoveStack` is correct (owned only, empty slot only, respects `mayPlace`), no change |
| 12 | Cursor item when the menu closes | read: vanilla `removed()` returns or drops it; the menu does not override it, no bug |
| 13 | Client `SimpleContainer(1)` desync | read: normal vanilla pattern, contents come from the slot packets, `mayPlace` is the same rule on both sides; server-side changes (scan, `replaceOwned`) are broadcast because the container is a live view, no desync found |
| 14 | Death / respawn losing the book | read: attachment is `copyOnDeath`; respawn/dimension now also force a resend and a scan; not reproduced |
| 15 | `GrimoireKeeper.ensureGrimoire` only looked at the inventory for an orphan book: a slot book of a magic whose skill is gone blocked the new book (it sat in the inventory, unusable, because casting reads the slot) | fixed, inventory mode only (the slot book is replaced) |
| 16 | `OpenPayload` had no rate limit (menu-open spam) | fixed |
| 17 | Taking the book out is undone within a tick by the scan (old and new design) | old window unchanged; inventory mode pauses the scan 10 s after a deliberate take (decision for the owner: length, or no pause) |

## Worst-case cost
- Server, per player per tick: one attachment lookup, one integer compare. Per player per second: one scan of 37 slots (`instanceof` per slot) and one
  deep `ItemStack.matches`. No stack copy per tick, no allocation per tick. All players never scan in the same tick (20 phases by UUID hash).
- Packets: `HipPayload` only when the slot changed (plus once after respawn/dimension/tracking start), to the owner and nearby trackers as before.
  `SlotClickPayload` is 2 small fields, at most 8 accepted per second per player (the rest dropped before any work); `OpenPayload` at most 8 per second.
  `ModePayload`: one at login, one per open request.
- Client, per frame: one 20x20 box, one item draw, one tooltip while hovered; the stack is read from the map once per tick; nothing is built per frame.
  The old window: fixed-cost panel + magic circle + cached text.

## Tests
- `blackclover/SlotClickRulesTest` (pure: the whole click table, scan every 20 ticks and spread over all 20 phases, change signal, rate limit).
- `perf/PerfSourceScanTest.theGrimoireSlotTickIsAChangeSignalPlusAStaggeredScan` (no copy in the tick, the compare sits behind the scan gate, probe kept,
  `SlotClickRules` pure, the window's readout cache).
- Self-test keys (server smoke): `slot.rules` and `slot.slot` (prefix `slot.`; add it to the smoke list). `slot.slot` uses a stand-in player:
  set / get / replace round trip, scan moves an owned book from the inventory into the empty slot, foreign book refused, a stack of two places one,
  take and swap through the cursor.
- `ServerSafetyTest`: no change needed (`GrimoireSlot.java` was already on the allow list for its client payload handlers; the new client class is under `client/`).

## Not verified
No game was run: the screen position of the box at other GUI scales, with the recipe book open, and next to other mods' inventory buttons or slots is
unchecked, as is the exact event order of `ContainerScreenEvent.Render.Background` (coordinates are absolute there). Packet delivery to a client
without the mod (channels are `optional`) is as before.

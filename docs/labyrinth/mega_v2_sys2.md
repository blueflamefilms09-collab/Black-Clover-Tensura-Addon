# The System for everyone, its buff only in the dungeon, and a window you can move (builder SYS2, 1.58.0)

Part of the second edition of the Mega-Dungeon (`docs/labyrinth/mega_v2.md` section 3). It changes the System of 1.55.0 (`docs/labyrinth/mega_system.md`, which still
describes everything it does not say otherwise). The owner's words: "the solo leveling system should be for all players in the dungeon but the buff for the system only
work in the dungeons; also add a way to move where it is placed on your screen (with keybinds)".

Nothing here has been run in the game. Section 9 lists what is unverified.

## 1. New, replacement, kept (rule 5)

| Part | Status |
|---|---|
| The System covers every player in the dungeon on both roads (`SystemQuests.covers`, the sync, the extraction, the overlay) | REPLACEMENT of "Holy road only unless `abyssalToo`" |
| The Hub (`nusmp:sanctuary`) shows the status window (stat points, no floor, no quest, no threat) | NEW |
| The stat bonus works only inside the dungeon dimensions, as transient modifiers (`BuffGate`, `SystemStats`) | REPLACEMENT of "permanent modifiers everywhere" (config `system.buffOnlyInDungeon`, default on; off restores the old always-on behaviour) |
| Safety net, health across a relog, purge of the old permanent modifiers (`SystemStats.reconcile`, `stashHealth`, `restoreHealth`, `SystemQuests.refreshFresh`) | NEW |
| Boss floor rule in floor numbers (`QuestFloors`) | REPLACEMENT of `FloorMap.isDecennial(floor, sector.perRegion)` inside the System (that test breaks when a sector is one floor) |
| Party seam `SystemParty.members(ServerPlayer)` and shared kill credit (`PartyRules`, config `system.shareProgress`, `system.shareRadius`) | NEW |
| One-time hint on entry (`system.hudHintOnEntry`, flag `hint` in `SystemState`) | NEW |
| The movable window: client config `anchor`/`offsetX`/`offsetY`, eight key mappings in "Multiverse: System", edit mode (`SystemEditMode`, `KeyRepeat`, `OverlayMath` placement, `OverlayLayout.edit`) | NEW (the anchor key of the config is KEPT and now means the corner the offsets count from) |
| Self-tests `system.cover` and `system.buff` | NEW (the five of 1.55.0 are KEPT and pass unchanged in meaning) |
| `abyssalToo` | KEPT in the config file, documented as ignored |
| `SystemText.MSG_NOTHING_TO_EXTRACT` and `TIP_USE` (they said "Holy floors") | KEPT unused; the code uses the new keys `msg.nothing_to_extract_any` and `tip.use_any` (the lang merge never overwrites an existing key, so a new key is the only way a changed text reaches the game without a hand edit) |
| The toggle key `key.nusmp.system_toggle` (Y) | KEPT, moved into the new Controls category; its id and default key are unchanged, so saved bindings load |
| Shadows, the Shadow Core, quests, rewards, the wire format of `Sync` and `Toast`, every public method other packages call (`SystemRegistry.*`, `SystemQuests.LISTENER`, `covers`, `enter`, `state`, `lastFloor`, `currentFloor`) | KEPT, source compatible |

## 2. The System for every player in the dungeon

* `SystemQuests.covers(Polarity)` is `pol != null`: every road. `covers(int floor)` follows. `ABYSSAL_TOO` stays in `SystemConfig` and in old config files and is ignored
  (its comment says so).
* **Where the window is on.** The Mega-Dungeon (both roads) with the floor, road, threat and quest as before; the **Hub** with a status window only. The server builds the
  Hub picture in `SystemSync.hub`: `active`, floor 0, no road, threat 0, no quest, no clock, the stat points. The wire format did not change ("floor 0, quest none"), so an
  old client reads it as it always read a floor-less window. The client layout drops the THREAT word when the threat is 0 (`OverlayLayout.panel`), and the overlay writes
  "SANCTUARY: NO QUESTS HERE" in the quest line of floor 0. Floor quests are never rolled in the Hub: `SystemQuests.enter` is only called by the floor tracker, which
  watches the Mega-Dungeon.
* **Quests are per floor and per player.** Every player still rolls their own quest on a depth they have never reached (seeded by player and floor, as before). What
  changed is who is CREDITED. `SystemQuests.killed` credits the killer and, when `system.shareProgress` is on, every member of `SystemParty.members(killer)` who is alive, in
  the same level and within `system.shareRadius` blocks (default 64; 0 = anywhere on the floor) of the fallen creature, for their own "defeat N monsters" quest (each quest still
  checks that the kill was on ITS floor). Why only kills: "no damage", "speed" and "rooms" judge a player's own conduct (one member's mistake must not fail the others and a
  member must not be paid for a room they never stood in); the ambush quest already credited every player within 48 blocks. The pure rule is `PartyRules.credit`.
* **The party seam.** `SystemParty.members(ServerPlayer)` answers the player and everyone who plays with them (the player first, each once, never empty). The default is the
  players standing in the same SECTOR (`PartyRules.sameGroup` over `MegaDungeon.sectorAt`; a sector is one floor since 1.58.0). The PARTY builder replaces it with
  `SystemParty.setProvider(player -> ...)` (the FTB Teams bridge); a provider that throws or returns null falls back to the default for that call, so a broken party layer can
  never break a quest. Nothing else in the System needs to change.
* **Boss floors.** `QuestFloors.isBossFloor(floor)` is `|floor| % 10 == 0` (the contract's "every tenth floor"). The old test, `FloorMap.isDecennial(floor, sector.perRegion)`,
  is "a multiple of the sector's floors": with one floor per sector (`perRegion` 1) it would call EVERY floor a boss floor and no quest would ever be given. If GEN2 adds a
  config or helper for the boss interval, `QuestFloors.BOSS_EVERY` is the one place to change.
* **Shadows and the Core.** Extraction accepts a kill on any road (`ShadowExtraction.facts`: `covers(polarity)`); the texts no longer say "Holy". Shadows are NOT touched:
  they live in the Mega-Dungeon and never leave it on their own (`SystemEvents.onTravel`). Outside the dungeon they are dormant: when the owner is in the Hub or the Overworld
  the sweep treats them as "away" (no following, no fighting) and they end `minionMinutes` (30) after the owner left, exactly as before. Why not follow the owner: a shadow is a
  fresh creature of a dungeon monster type; the Hub is a peaceful plaza that suppresses hostile spawns, and a pack of hostile mobs trailing a player through the Overworld would
  grief and farm. Coming back within the time finds them where they stood.

## 3. The buff only inside the dungeons

* `BuffGate` (pure): `active(dimensionId, buffOnlyInDungeon)` is true for `nusmp:mega_dungeon` and `nusmp:sanctuary`, or always when the switch is off. `needsChange(applied,
  wanted)` says when the modifiers must be touched. A self-test compares the two id strings with the real `Labyrinth` dimension keys.
* `SystemStats.apply(p[, state])` puts the five fixed-id modifiers (`nusmp:system/<stat>`) on when the gate is open and takes them off when it is not;
  `sync(p)` also takes them off when `system.enabled` is false; `applyActive` is the unconditional form (self-tests); `remove` takes them off by id.
* **Transient, not saved.** `AttributeInstance.addOrUpdateTransientModifier`. A transient modifier is not written with the player, so nothing can be left behind by a crash,
  a killed server, a disconnect outside the dungeon or a missed event: the worst case is a bonus that the next look puts back. The stat POINTS (the attachment) are the truth and
  are never touched. Events that set the state: login and respawn (`SystemQuests.refreshFresh`), dimension change (`refresh`), a stat reward or admin change (`apply`).
* **The safety net.** `SystemStats.reconcile(p)` runs from `SystemEvents.onPlayerTick` every 40 ticks (offset 11): it compares what was last done to the player (`APPLIED`,
  an in-memory map cleared on logout and server stop) with what the gate says now and corrects any difference. It exists because many mechanisms of this mod move players
  (chain travel, the Hub, stairs, commands) and one that skips `PlayerChangedDimensionEvent` would otherwise leave the bonus on in the Overworld.
* **Old worlds.** 1.55.0 to 1.57.0 saved the modifiers as PERMANENT ones. `refreshFresh` removes them by id on every login and respawn before applying, so after the first login
  they are transient. A modifier of an attribute that is no longer in `stats.json` would stay; the shipped table never changed its attributes.
* **Health across a relog.** The game loads a player with their health already cut to the plain maximum; a VIT bonus is not saved any more, so a player with 40 of 40 would log in
  with 20 of 40. The logout event (verified: NeoForge fires `PlayerLoggedOutEvent` as the FIRST statement of `PlayerList.remove`, before the save) stores the health in the
  player's persistent data (`nusmp_system_hp`) while the bonus is on, and the login gives it back after the bonus is applied (`restoreHealth`: only while the bonus is on, never
  above the maximum or below the current health; the note is removed either way; a respawn throws it away). This is not a heal: it returns exactly what the player had.
* Shadows are not affected by `buffOnlyInDungeon` (see section 2).

## 4. The window you can move

### Keys (Controls, category "Multiverse: System", lang key `key.categories.nusmp.system`)

| Mapping id | Name (English) | Default | Notes |
|---|---|---|---|
| `key.nusmp.system_toggle` | Toggle the System window | Y | KEPT; hides or shows the window for the session (the client config `enabled` is the lasting switch) |
| `key.nusmp.system_edit` | Toggle edit mode (move the window) | U | NEW |
| `key.nusmp.system_left` / `_right` / `_up` / `_down` | Edit mode: nudge left / right / up / down | Left / Right / Up / Down arrows | NEW; Shift = 10 pixels, a held key repeats |
| `key.nusmp.system_reset` | Edit mode: reset position | Home | NEW; top-left corner, no offset |
| `key.nusmp.system_corner` | Edit mode: cycle the corner | End | NEW; clockwise top-left, top-right, bottom-right, bottom-left; the offset resets so the window visibly jumps to the corner |

**Conflict check** (from the sources of this repo and vanilla 1.21.1): the mod binds V (sing, use worn relic), R (use relic), J, K, H (djinn), G (grimoire summon) and Y (this toggle);
vanilla binds W A S D E Q F T / Tab Esc Shift Ctrl Space 1-9 L P C X B F1-F12 in the game. U, Home, End and the arrow keys are free (the arrows only matter in screens). **R was
rejected for Reset** although the brief suggests it: NeoForge's `KeyMapping.click` gives a click to EVERY mapping on the key, so pressing R in edit mode would also fire the
relic (`key.nusmp.use_relic`). **C was rejected for the corner**: vanilla's save-hotbar key. The mappings are rebindable and the hint box always shows the CURRENT keys. A player who
moves with the arrow keys sees the conflict in Controls and rebinds; edit mode never opens a screen, so the keys stay free the rest of the time.

### Edit mode (client: `SystemEditMode`, `KeyRepeat`, `SystemOverlay`)

* U toggles edit mode (action-bar line "System window: edit mode on"). The window keeps drawing on the HUD with a pulsing amber frame, corner handles and a hint box
  (`OverlayLayout.edit`): the corner and the position, the keys, and "Open the chat or the inventory to drag the window with the mouse". It is drawn in edit mode even when the
  window is switched off, hidden with Y or the player is outside the dungeon (a labelled sample window then, so the place can be set before going in).
* While edit mode is on and no screen is open: the nudge keys move the window 1 pixel a step (Shift: 10), a held key waits 10 ticks and then steps every tick (`KeyRepeat`, pure);
  Home resets; End cycles the corner; every step goes through `OverlayMath`, which clamps the window to the screen. Leaving edit mode (U again) writes the file.
* **The mouse.** In the game the cursor is captured, so dragging works while a screen that frees it is open (chat, inventory, pause menu) and edit mode is on: a left press
  inside the window picks it up (`ScreenEvent.MouseButtonPressed.Pre`, cancelled so the screen below never sees that click), moving the mouse drags it
  (`MouseDragged.Pre`), the release drops it and writes the file (`MouseButtonReleased.Pre`). Nothing here grabs or releases the mouse. While a screen is open the window is drawn over it
  (`ScreenEvent.Render.Post`, translated 350 up in z so item stacks do not cover it and tooltips, drawn at 400, stay on top). A drag that loses its screen (Escape) is dropped on the
  next tick. Why no screen of our own: the brief asked for the overlay layer plus tick and input handlers, and a custom screen would also have solved the R problem; the cost
  of this design is that a drag needs the player to open the chat or the inventory first, which the hint box says. If the owner prefers a dedicated drag screen it is a small
  addition on top of the same pure math.
* **Why the player's place is saved in the CLIENT config** (`nusmp-labyrinth-mega-system-client.toml`, section `[system]`): it is a preference of one screen, not of a world or a server.
  Keys: `anchor` (0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right; the 1.55.0 key, kept), `offsetX`, `offsetY` (-4096..4096). The offsets are in the window's OWN pixels (the unit
  of its 164-pixel width, before the GUI scale and the window's `scale` option), measured from the anchor's corner inwards: right-anchored panels count from the right edge, bottom-anchored
  from the bottom edge. `OverlayMath.placeX/placeY` add the offset to the standard 6-pixel margin and clamp, so a place saved on a big screen can never leave a small one (also after
  a GUI scale change or a resize). The edit keys read the place back from where the window REALLY is (after the clamp), so a stale offset never swallows key presses.
  The in-memory value changes at once; the file is written when edit mode ends, a drag ends, or the player leaves the world (never 20 times a second).
* **Toasts.** The pop-up lines slide in from the left edge. `OverlayMath.toastTop` puts them under the window when it sits in their column in the upper half of the screen (as
  before for the default corner) and at the top margin otherwise; in edit mode `toastClear` moves them under the hint box when it would cover them.

## 5. Net, sync, throttles

No packet changed: `SystemNet.Sync` and `Toast` have the same fields, order and limits; the Hub is "floor 0, no quest". `SyncThrottle` (10 ticks) and `SystemSync.requestNow` on every
dimension change are unchanged. No new packet was needed: edit mode is purely client-side.

## 6. Configuration

Server `nusmp-labyrinth-mega-system-server.toml`, `[system]` (new keys; the lead registers the spec as before, nothing new to register):

| Key | Default | Meaning |
|---|---|---|
| `buffOnlyInDungeon` | true | the stat bonus works only in the Mega-Dungeon and the Hub; false = everywhere (the 1.55.0 behaviour) |
| `hudHintOnEntry` | true | tell a player once, the first time they are in a dungeon dimension, how to move the window (the keys are `Component.keybind`, resolved by each client to its own binding) |
| `shareProgress` | true | a kill counts for the "defeat N monsters" quest of every party member near it |
| `shareRadius` | 64 | blocks; 0 = anywhere on the floor |
| `abyssalToo` | false | IGNORED since 1.58.0, kept so old files load |

Client `nusmp-labyrinth-mega-system-client.toml`, `[system]`: new `offsetX` 0, `offsetY` 0; `anchor` is the old key (0). Admin: `/multiverse mega system status [player]` also prints the
stat bonus state (on / dormant, the dimension, the switch) and the number of players in the credit party.

## 7. Files

Server (`labyrinth/mega/system`): `BuffGate` (pure gate), `QuestFloors` (pure boss-floor rule), `PartyRules` (pure credit rule), `SystemParty` (the seam), `SystemStats` (transient modifiers, safety
net, health note), `SystemQuests` (covers, shared kill credit, refresh, hint), `SystemSync` (Hub window), `SystemState`/`SystemAttachments` (the `hint` flag, optional field, so every older save
loads), `SystemEvents` (the safety net, login/respawn/logout), `SystemConfig`, `SystemText`, `SystemCommand`, `ShadowExtraction`/`ShadowCoreItem`/`ExtractRules` (texts only), `SystemSelfTests`.
Client (`client/mega/system`): `OverlayMath` (placement math), `KeyRepeat` (pure), `OverlayLayout` (View offsets, no threat at 0, `edit`), `OverlayStyle`, `SystemEditMode`, `SystemClient` (keys, events),
`SystemOverlay` (sample window, Hub, edit marks), `SystemClientConfig` (offsets, save). Data: `stats.json` (the `why` text only). Tools: `tools/system/OverlayDump.java` and
`preview_overlay.py` (edit mode and the Hub scenes).

## 8. Tests

Pure (shim-run locally, real JUnit in CI): `BuffGateTest`, `QuestFloorsTest`, `PartyRulesTest`, `OverlayPlacementTest` (every corner, nudge direction, clamp, stale offsets, GUI scale change, drag,
reset, cycle, toasts), `KeyRepeatTest`, `OverlayEditLayoutTest` (offsets, Hub, hint box on screen and clear of the window), three new cases in `SystemStateTest`, the refusal text in `ExtractTest`.
All 211 pure tests of the two packages pass. **Mutation check:** twelve deliberate breakages (anchor sides, nudge base, clamp, corner order, the gate, repeat delay, credit radius, boss
rule, toast placement, the threat row, the hint-box clamp, the saved hint flag) were applied one at a time to a copy: ten failed a test, two survived and are equivalent mutants
(`Math.min(Math.max(lo, hi), v)` versus `Math.min(hi, v)` give the same result for an empty range because the outer `Math.max(lo, ...)` wins; `Math.abs(floor) % 10 == 0` versus `floor % 10 == 0`
are the same test for zero). In-game: `system.cover` (both roads covered, a quest on floor -1 and -2, none on the boss floor -10, the party of a lone player) and `system.buff` (the id
strings match the dimension keys; on in the Mega-Dungeon, off in the Overworld, a transient modifier that is absent from the saved attribute, off after leaving and on after
entering, a relog keeps the bonus hearts, the safety net, the Hub has the bonus and a status window). A fake player cannot change dimension, so the test creates one in each level.

## 9. Looked at, and not verified (honest list)

Looked at: `python tools/system/preview_overlay.py` renders the REAL layout class (the same one the game replays) on a mock dungeon with the game's own font: the edit-mode window with the
hint box and the toasts under it, the four corners, the window moved off its corner on a small and a large screen, the Hub's status window and the sample window. The renderer is not the
game's (no real blending, z-order or font hinting).

Read from the NeoForge 1.21.1 sources (raw.githubusercontent.com, `patches/net/minecraft/server/players/PlayerList.java.patch` and `.../client/KeyMapping.java.patch`): the logout event fires
before the save; `KeyMapping.click` clicks every mapping on a key; `isDown` includes the conflict context; unknown categories are sorted after the vanilla ones.

NOT verified, because nothing was run in the game: every key and mouse path (the events fire as documented, a cancelled `MouseButtonPressed.Pre` keeps the click from the screen below,
the drag coordinates are GUI-scaled doubles as `ScreenEvent` documents); the z of the window over screens; that the arrow-key mappings do not clash with a player's own binding (they show as
a conflict in Controls); the transient modifier through death, respawn, the End portal and dimension changes (the safety net covers a missed event, but a path that replaces the player's
attributes after the event could take a tick or two to be repaired); the logout health note (the order was read from the patch, not observed); the config file write
(`ModConfigSpec.save()`); `Component.keybind` in a chat line; the lead's lang merge. The type check had only the known stub artefacts (an abstract `KeyMapping` and `FriendlyByteBuf` in the
stubs): no real error.

## 10. Sources

NeoForge 1.21.1 patches, read for the points above: https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/patches/net/minecraft/server/players/PlayerList.java.patch and
https://raw.githubusercontent.com/neoforged/NeoForge/1.21.1/patches/net/minecraft/client/KeyMapping.java.patch. The vanilla font atlas used by the preview:
https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/font/ascii.png (fetched into a scratch folder, never committed).

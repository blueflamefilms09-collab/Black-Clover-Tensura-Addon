# Compatibility with map mods (Xaero's, FTB Chunks, JourneyMap, VoxelMap)

Owner request: "also make my mod compatible with map mods like from FTB and Xaero's".

Honest status first. No Xaero's, FTB or JourneyMap jar exists in the build sandbox and the Gradle build cannot download one, so EVERYTHING here is soft:
detection by mod id, no compile-time dependency, no reflection into a map mod at all (not one stable public class of those mods is known to me with
certainty), and "data/format level" compatibility everywhere else. Nothing was run in a game. What is only type-checked against API stubs, and what is
only a unit test of pure code, is said under "Certainty" at the end. Where I am not sure of a map mod's behaviour, the table says so instead of
guessing.

This is NEW (nothing like it existed in the mod), so by CLAUDE.md rule 5 there is no replacement; every edit to an existing file is an addition.

## 1. Audit: what can go wrong with map mods in this mod, and the fix

| # | Risk | Evidence in this repo | Fix | Certainty about the map mod |
|---|------|----------------------|-----|------------------------------|
| 1 | A custom block with no `mapColor` is `MapColor.NONE`. Vanilla maps (and every mod that borrows the vanilla rule) look THROUGH such a block to what is below it, so the block is a hole or the void colour. | 26 custom blocks are registered (list in section 2). 24 had a `mapColor`; `nusmp:arena_return_sigil` (`arena/ArenaSystem.java`) and `nusmp:relic_forge` (`relics/forge/ForgeRegistry.java`) had none. `nusmp:grimoire_altar` and `nusmp:underworld_matter` set it inside their own constructor (fine). | Added `mapColor` to the two blocks; a data table of all blocks (`tools/compat/map_block_colours.json`) and a JUnit guard (`MapBlockColourGuardTest`) that fails when any block registered anywhere in `src/main/java` has no `mapColor` (explicit whitelist in the same JSON, empty today). | The vanilla rule is certain (`MapItem` skips `MapColor.NONE`). That Xaero's World Map and FTB Chunks use `MapColor` at all is NOT certain: I believe both mainly average the block TEXTURE (Xaero's World Map has an "accurate vs vanilla" colour option, from memory), so the texture path matters too; every custom block here has a blockstate and a model with a block texture (checked), except the invisible `nusmp:dimensional_rift`, whose model has only a particle texture (amethyst), so that path needs nothing. |
| 2 | Invisible or see-through blocks (`nusmp:dimensional_rift`: render shape INVISIBLE, no collision) might be drawn as a coloured dot on a texture-based map. | `labyrinth/mega/rift/RiftRegistry.java`. | It has a mapColor (purple) for the vanilla path; it has no collision, so vanilla's motion-blocking height map ignores it. Nothing else is done: how Xaero's treats an INVISIBLE render shape is not known to me. If a purple dot shows on a world map, that is this block. | Unknown. |
| 3 | Dimension types: a value a map mod reads could be out of range (`min_y`/`height` not multiples of 16, `logical_height` above `height`, `min_y + height` over 2032) or make a map blank. | `data/nusmp/dimension_type/*.json` (8 files), `data/nusmp/dimension/*.json` (8 files). | Audited, see section 3. No value is wrong, so NO dimension JSON was changed (changing `has_skylight`/`has_ceiling` would change light, spawning and the whole feel of the dungeon). Added `DimensionAudit` (pure) + test so a later edit cannot break the rules, and a profile per dimension in `/multiverse compat maps`. | Vanilla's own limits are certain. How each map mod reacts to `has_ceiling` is from memory: Xaero's switches to a cave-style layer under the player in a roofed dimension (like the Nether), which is what the Mega-Dungeon (`has_ceiling: true`, no skylight) wants. |
| 4 | Dimension and biome NAMES: a minimap shows the biome name (visible in the owner's screenshots) and a world map lists dimensions. A key with no translation is shown raw (`biome.nusmp.mega_depths`). | `en_us.json` has `biome.nusmp.labyrinth_sky` only; `dungeon_depths`, `mega_depths`, `sanctuary_plains` and `dimension.nusmp.*` (except `zogratis_arena`) are missing. | Lang fragment `tools/compat/maps_lang.json` (merge into `en_us.json`; not edited here by instruction). | Biome names via the vanilla translation key: fairly sure for Xaero's. Dimension names: unsure which key a map mod uses; `dimension.<ns>.<path>` is the vanilla-style key, harmless if unused. |
| 5 | Mega-Dungeon geometry. Floors are laid out HORIZONTALLY in one dimension (`nusmp:mega_dungeon`), parties are instanced into different sectors of the same dimension, rifts' pocket realms sit at x = 4,000,000 + 192 per slot, z = 4,000,000 (`labyrinth/mega/rift/RiftRules.java`). | Docs `rifts.md`, `mega_arch.md`. | Nothing to fix server-side. Map tiles of pocket cells and regenerated floors go stale on a player's map (the pocket is cleared when it collapses); that is client data we cannot clear. All coordinates are far inside the 30,000,000 limit, so no map mod or waypoint should overflow. Documented only. | Fairly sure no limit is hit. |
| 6 | Waypoints: the player has to type coordinates of the Gate, a Return Waypoint, a rift. | `rift/WaypointService.java`, `hub/HubTravel.java`. | Shareable waypoint chat lines (`compat/maps/WaypointShare`, `MapShare`, `/multiverse waypoint share ...`), opt-in, rate limited. See section 4. | Format from memory, medium certainty, see section 4. |
| 7 | HUD overlap: Xaero's Minimap and FTB Chunks' minimap, and JourneyMap/VoxelMap, default to the top-right corner. | Every `RegisterGuiLayersEvent` layer of the mod was read (section 5). | `client/compat/MapHudAvoid` + client config `compat.maps.minimapSize`. No layer of the mod DEFAULTS to the top-right corner, so no layer was changed (the rule was: only top-right defaults). | Default corner of those minimaps: fairly sure for Xaero's/JourneyMap/VoxelMap, less for FTB Chunks. |
| 8 | FTB Chunks claims inside the mod's dimensions: a claimed chunk blocks other players from using blocks and entities in it. In the Hub (`nusmp:sanctuary`, survival, non-instanced) a player could claim the plaza and lock the altars and NPCs for the others. The dungeon itself is held by the Dungeon Guard (blocks are unbreakable), which does not stop a claim from existing. | `dungeon/DungeonGuard.java` never mentions claims. | See section 6: the switch is FTB Chunks' own `claim_dimension_blacklist`. The mod reads that file softly, logs at start, and tells ops. It does NOT write FTB's config. | The config key name and file location are from memory (section 6). |
| 9 | "Fair play": a minimap/world map shows the layout of a dungeon the owner wants explored. | The Mega-Dungeon is built to be explored; `hidden_caches.md`. | NOTHING shipped. Xaero's has fair-play variants and server-side switches, but I am not CERTAIN of the exact mechanism (a special string in a name or message is what I half remember), so no dimension list for "do not show on world maps" is shipped, because it would be invented. The owner can use the Fair Play builds of Xaero's, or FTB Chunks' own settings. | Not certain, so nothing done. |
| 10 | JourneyMap / VoxelMap / other minimaps. | n/a | Same data paths as above (mapColor, lang, waypoint chat line in the JourneyMap `[name:"x", x:.., y:.., z:.., dim:..]` form, off by default). | Medium. |

## 2. Map colours: the table

Data: `tools/compat/map_block_colours.json` (id -> `MapColor` constant), checked against the source by `MapBlockColourGuardTest`.

| Block | MapColor | Source |
|---|---|---|
| nusmp:arena_return_sigil | COLOR_LIGHT_BLUE | **added** in `arena/ArenaSystem.java` |
| nusmp:relic_forge | METAL | **added** in `relics/forge/ForgeRegistry.java` |
| nusmp:soul_forge | COLOR_PURPLE | existing |
| nusmp:grimoire_altar | COLOR_GREEN | existing (constructor) |
| nusmp:underworld_matter | COLOR_PURPLE | existing (constructor) |
| nusmp:labyrinth_gate | COLOR_BLUE | existing |
| nusmp:labyrinth_exit | GOLD | existing |
| nusmp:relic_pedestal | COLOR_BLACK | existing |
| nusmp:labyrinth_bricks, labyrinth_tiles, labyrinth_pillar, labyrinth_glyph, labyrinth_lantern, magicule_vein | DEEPSLATE | existing (`masonry()`) |
| nusmp:gatekeeper_door | COLOR_BLACK | existing |
| nusmp:peak_altar | COLOR_PURPLE | existing |
| nusmp:guardian_altar | COLOR_CYAN | existing |
| nusmp:dimensional_rift | COLOR_PURPLE | existing |
| nusmp:sealed_archway | COLOR_BLACK | existing |
| nusmp:magic_barrier | DIAMOND | existing |
| nusmp:ward_rune | DEEPSLATE | existing |
| nusmp:spawn_glyph | COLOR_PURPLE | existing |
| nusmp:pitfall_tile | DEEPSLATE | existing |
| nusmp:snuffer_glyph | COLOR_BLACK | existing |
| nusmp:sigil_plate | COLOR_PURPLE | existing |
| nusmp:riddle_stone | COLOR_BLACK | existing |

**Merge note (parallel builders).** Other builders add blocks under `elite/` (forge stations, crystals, crates, plushies, hub NPCs) in other worktrees; this
worktree did not touch them. After the merge, RUN `MapBlockColourGuardTest`: it scans every block registration in `src/main/java`, so it will fail on any
new block that has no `mapColor`; add the colour to the registration (and a row to `tools/compat/map_block_colours.json`) or, for a see-through block that
must be left out of maps on purpose, add its id to the `whitelist` object of that JSON (id -> reason).

## 3. Dimension audit

Read from `data/nusmp/dimension_type/*.json` and `data/nusmp/dimension/*.json`. Vanilla limits checked: `min_y` and `height` multiples of 16, `height` at
most 4064, `min_y + height` at most 2032, `logical_height` at most `height`, `coordinate_scale` positive.

| Dimension id | min_y / height / logical | skylight / ceiling | effects | Map profile | Verdict |
|---|---|---|---|---|---|
| nusmp:mega_dungeon (Mega-Dungeon floors) | 0 / 128 / 128 | no / yes | nusmp:living_dungeon | roofed, Nether-like (cave layer) | valid, unchanged |
| nusmp:sanctuary (Floor 0 Hub) | -64 / 384 / 384 | yes / no | minecraft:overworld | surface | valid, unchanged |
| nusmp:labyrinth (the Spire's sky, old worlds) | 0 / 320 / 320 | yes / no | nusmp:labyrinth | surface | valid, unchanged |
| nusmp:living_dungeon (removed dungeon, old worlds) | -64 / 192 / 192 | no / yes | nusmp:living_dungeon | roofed | valid, unchanged |
| nusmp:grimoire_library | 0 / 256 / 256 | yes / no | minecraft:overworld | surface (void flat) | valid, unchanged |
| nusmp:arc_arena | 0 / 256 / 256 | yes / no | minecraft:overworld | surface (void flat) | valid, unchanged |
| nusmp:zogratis_arena | 0 / 256 / 256 | yes / no | minecraft:the_end | surface (void flat) | valid, unchanged |
| nusmp:dream | 0 / 256 / 256 | yes / no | nusmp:dream | surface (void flat) | valid, unchanged |

All `coordinate_scale` are 1.0, so no map mod will show converted "Nether-style" coordinates for them. Xaero's keys its per-dimension data by the dimension
id (directory name `dim%nusmp$mega_dungeon`, from memory), so the ids above are what a player's map data hangs on: do not rename them (existing worlds and
players' maps would restart).

Dimension "do not show on world maps" list: **not shipped.** I am not certain of any fair-play or disable mechanism that a mod can legitimately declare for
Xaero's or FTB Chunks, so I did not invent one.

## 4. Waypoint sharing

Chat line understood by Xaero's Minimap / World Map (current form, as the owner's spec gave it and as I remember the source `WaypointSharing`):

    xaero-waypoint:<name>:<initials>:<x>:<y>:<z>:<colour 0-15>:<disabled true|false>:<type int>:Internal-<dimension key>-waypoints

* name: any colon in a name is written `^col^` (from memory; I am sure a colon cannot stay a colon because the line is colon-separated, only moderately sure
  of the exact escape). `WaypointShare` escapes `:` that way and removes control characters.
* initials: one to three upper-case letters; colour index into Xaero's 16 colours (0 black ... 15 white, Minecraft chat-colour order); type 0 = normal.
* dimension key in the last field: `overworld`, `the-nether`, `the-end` for vanilla; for any other dimension `dim%<namespace>$<path>` (for example
  `Internal-dim%nusmp$mega_dungeon-waypoints`). Certainty: medium (about 75 percent). If a client does not recognise the set it should still offer the
  waypoint (I believe it falls back to the current set, not certain).
* the older `xaero_waypoint:` form (underscore) is parsed by `WaypointShare.parse` too, but only the hyphen form is ever written.
* y may be written `~` by Xaero's for an unknown height; `parse` accepts it, we always write a number.
* Newer Xaero's versions may append more fields (a rotation flag and a yaw) before the set name; `parse` ignores unknown extra fields and finds the set
  by its `Internal-` prefix. We never write them.
* JourneyMap form (optional, off by default): `[name:"Gate", x:12, y:64, z:-30, dim:minecraft:overworld]`. Medium certainty.
* FTB Chunks: as far as I know it has no waypoint chat format and no waypoints, so nothing is written for it.
* The server cannot tell which map mod a client has, so one line is sent to everybody who has sharing on. It is a normal chat line: a friendly,
  clickable text (hover shows the coordinates, click copies the raw Xaero's string) plus, by default, the raw string in dark grey (a client with Xaero's turns
  it into an "add waypoint" button, I believe also for system messages, not verified). `compat.maps.rawXaeroString = false` removes the raw string.

Moments (server side, all through `MapShare`, all failure-proof and rate limited):

| Moment | Hook (one added line) |
|---|---|
| The Overworld Gateway used for the first time (and each new Gateway) | `HubTravel.enterFromOverworld` |
| The Hub gates (home, holy, abyssal) the first time a player reaches Floor 0 | `HubTravel.enterFromOverworld` |
| A Return Waypoint saved by the Return Crystal | `WaypointService.crystal` |
| A shortcut rift opened near a player | `RiftService.roll` |
| A Dimensional Rift pocket entrance opened | `RiftService.roll` |
| A safe room found (first rest at its anchor) | `RestEvents.start` |

The periodic 2-second waypoint sampling is NOT shared (it would be spam); only the explicit Crystal save is.

Opt-in and limits: server config `compat.maps.shareWaypoints` (default true) in `nusmp-compat-maps-server.toml`; per-player `/multiverse waypoint share on|off`
(stored in the player's persistent data, key `nusmp_maps_share`, survives death); at most 4 automatic lines per player per minute, and the same place is
never shared twice (first discovery is remembered per player, capped list). Commands: `/multiverse waypoint share here|gate|return` print the line for
the present position / the known Gateway and Hub gates / the saved Return Waypoint (these always answer, whatever the toggle says, because the player asked).

## 5. HUD co-existence

Every layer registered by the mod was read.

| Layer id | Where it draws | Default anchor top-right? |
|---|---|---|
| nusmp:system_overlay | the System window; corner set by the player (`system.anchor`, default 0 = top-left) | no; saved anchor wins |
| nusmp:page_unlock | top centre (300 wide) | no |
| nusmp:route_unlocked | centre banner and full-screen flash | no |
| nusmp:song_meter | bottom centre | no |
| nusmp:line_arts_bar | bottom centre | no |
| nusmp:hero_hud, hero_curse_overlay | bottom left; curse overlay is full screen | no |
| nusmp:relic_hud | bottom right | no |
| nusmp:djinn_hud | full-screen effects | no |
| boss bar (`ModBossBarOverlay`, replaces the vanilla boss layer) | top centre | no |

Result: **no layer was changed.** `MapHudAvoid` exists for the layers that will default to top-right in the future and for a player-chosen opt-in; the
only near miss is the top-centre page-unlock panel at very large GUI scales, where a 140 px minimap can overlap its right edge by about 50 px (not changed,
it is not top-right).

## 6. FTB Chunks

* `DungeonGuard`/`DungeonGuardRules` never look at claims; the Dungeon is protected by making blocks unbreakable, which claims do not influence. The Hub
  (`nusmp:sanctuary`) has its own protection (`HubProtect`), but a claim there could still lock other players out of using things.
* The switch is FTB Chunks' own server config key `claim_dimension_blacklist` (I believe a list of dimension ids in the `claims` group of
  `ftbchunks-world.snbt`; it may also accept wildcards such as `nusmp:*`, not certain). I know no documented data-pack or API mechanism by which a mod can
  add itself to it, so the mod does NOT declare it. Instead `FtbChunksCheck` reads, as plain text, `<world>/serverconfig/ftbchunks-world.snbt`,
  `<game dir>/config/ftbchunks/ftbchunks-world.snbt`, `<game dir>/defaultconfigs/ftbchunks-world.snbt` (first that exists), and looks for the key; every
  failure is a quiet "unknown". When FTB Chunks is loaded and the file does not blacklist `nusmp:*` / the mod's dimensions (or its whitelist excludes
  them), the mod logs one line at server start and `/multiverse compat maps` prints an op-visible warning with the exact lines to add.
* There is no `/multiverse status` command in this mod; the op-visible place is `/multiverse compat maps` (permission 2), and ops get the same warning
  at each login while it applies (config `compat.maps.warnOps`, default true).

## 7. Commands, configs, files

* `/multiverse compat maps` (op): detected map mods and what the mod did for each, the dimension profiles, the FTB Chunks claim check.
* `/multiverse waypoint share on|off|here|gate|return`.
* Self-test `compat.maps` (`/multiverse mega selftest compat.maps`): format round trip for every dimension and kind, the limiter, the dimension audit of the real JSONs (skipped quietly if the class loader cannot see the mod's own JSONs), the FTB reader, the minimap geometry, the chat line, and a registry sweep that every `nusmp:` block has a map colour (so a block merged in from another branch is caught in the game too).
* `nusmp-compat-maps-server.toml`: `compat.maps.shareWaypoints`, `rawXaeroString`, `shareJourneyMapForm`, `maxLinesPerMinute`, `warnOps`.
* `nusmp-compat-maps-client.toml`: `compat.maps.minimapSize` (default 140), `compat.maps.avoidMinimap` (default true).
* `tools/compat/maps_lang.json`: lang fragment to merge into `en_us.json`.

## 8. Edits to existing files (all additions)

* `arena/ArenaSystem.java`: `.mapColor(MapColor.COLOR_LIGHT_BLUE)` on `arena_return_sigil`.
* `relics/forge/ForgeRegistry.java`: `.mapColor(MapColor.METAL)` on `relic_forge`.
* `NUSMP.java`: `MapsCompat.init(modContainer)` and the client config registration.
* `multiverse/MultiverseCommands.java`: `root.then(MapsCommand.waypointNode())`. `compat/CompatCommand.java`: `.then(MapsCommand.compatMapsNode())`.
* Hook lines: `labyrinth/mega/hub/HubTravel.java` (enterFromOverworld), `labyrinth/mega/rift/WaypointService.java` (crystal), `labyrinth/mega/rift/RiftService.java` (roll),
  `labyrinth/mega/safe/RestEvents.java` (start). `MapHooksTest` fails if one of them is removed.
* No dimension JSON, no HUD layer, no `elite/` file, no version/CHANGELOG/README/LIBRARIES/`en_us.json` edit.

## 9. When merging with the parallel branches

1. Run `MapBlockColourGuardTest` (and `/multiverse mega selftest compat.maps` on a server): blocks added under `elite/` need `mapColor` and a row in `tools/compat/map_block_colours.json`.
2. Run `MapsLangTest` after merging `tools/compat/maps_lang.json` into `en_us.json` (identical text is accepted).
3. A new HUD layer that defaults to the top-right corner should pass its box through `MapHudAvoid.avoidX/avoidY`; a position the player chose must not.
4. A new dimension: add it to `DimensionAudit.NAMES` (a test compares the list with the folders) and to the table in section 3.

## 10. Certainty summary

Verified in this repo by reading the code: the block list, the missing colours, the dimension JSON values, the HUD layers, the hook points.
Verified by pure JUnit (run here with the stub runner, not Gradle): the waypoint strings, the limiter, the table/source guard, the dimension audit, the
FTB config parser, the minimap geometry.
Only type-checked against API stubs (never run): everything that touches Minecraft/NeoForge types (`MapShare`, `MapsCommand`, `MapsCompat`, the client
helper, the config classes, the hook lines).
Not known / from memory: every Xaero's, FTB Chunks, JourneyMap behaviour and format named above (each marked in the text).

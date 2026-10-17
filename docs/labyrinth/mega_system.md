# The System: the holographic window, floor quests, stat rewards and the Shadow Core (builder SYSTEM, 1.55.0)

Part of the bidirectional Mega-Dungeon (`docs/labyrinth/mega_floors.md`). The owner's third brief, system 3 ("Holy Ascensions: the Solo Leveling System"), asks for a
holographic status window, quests per floor, and a reward for a quest done before the next staircase: a permanent stat boost OR a Shadow Core that raises the souls of
slain Holy enemies as minions. This builder owns packages `labyrinth/mega/system` and `client/mega/system`, the data in `data/nusmp/mega/system/`, the item
`nusmp:shadow_core`, the tools in `tools/system/` and this document.

**State of this document.** Written without a game. CI compiles the Java and runs the pure JUnit tests; five in-game self-tests (`system.*`) run in the CI smoke test.
The look of the window was rendered by running the REAL layout class through `tools/system/preview_overlay.py` (section 8): nobody has seen it in the game, and section 11
lists exactly what is unverified. Every class, data file and strategy below says WHY.

**1.58.0 update.** Several statements below are replaced by `docs/labyrinth/mega_v2_sys2.md`: the System now covers every player in the dungeon on both roads (`abyssalToo` is ignored),
the Hub has a status window, the stat bonus is a transient modifier that works only inside the dungeon dimensions (it was permanent), the window can be moved with keys (client config `anchor`,
`offsetX`, `offsetY`; category "Multiverse: System"), and a kill is credited to the party. Read that document first where the two disagree.

---------------------------------------------------------------------------------------------------------------------------------------------

## 1. New, replacement, kept (rule 5)

Everything here is **NEW**: before building, the mod was searched for an existing System / quest / minion feature and none is a replacement. Existing code that was
REUSED and not changed: `MegaDungeon` (floors, polarity, rooms, floor listeners), `FloorMap`, `HpBuffer` (one constant), `Paladins`/`Corpses`/`MonarchBoss`/`BossIdentity`/
`BalanceLaw` (to recognise servants and bosses), `RukhFx` (the dark burst), `NusmpEvents.fire` (quest hooks), the `SelfTests` registry. Kept: the vanilla particle purge and
the Rukh particles (not touched). The overlay does not replace `ModBossBarOverlay`, `SongClient` or `PageUnlockOverlay`; it is one more layer.

| Part | Status |
|---|---|
| Attachment `nusmp:system_state` (quest, stat points, counters), payloads `nusmp:system_sync` and `nusmp:system_toast` | NEW |
| Five quest kinds as data (`quests.json`), pure roller and state machine, rewards, stat table (`stats.json`) | NEW |
| Item `nusmp:shadow_core` + data component `nusmp:shadow_core_charges`, extraction of slain Holy creatures as shadows | NEW |
| Client layer `nusmp:system_overlay`, toggle key, client config `nusmp-labyrinth-mega-system-client.toml` | NEW |
| Server config `nusmp-labyrinth-mega-system-server.toml`, command `/multiverse mega system ...`, quest keys `mega.system_quest`, `mega.shadow` | NEW |
| Tools: `tools/system/` (overlay preview, item model generator and preview, lang generator), `docs/labyrinth/mega_system.md` | NEW |

---------------------------------------------------------------------------------------------------------------------------------------------

## 2. How it fits together

```
 floor tracker (builder FLOORS)  --fireFloorEntered(p, floor, previous)-->  SystemQuests (a FloorListener)
        finish the old quest (counting + ACTIVE = FAILED; "reach the next floor" + ACTIVE = DONE)  ->  pay (SystemRewards)
        roll the new quest (QuestRoller, seeded by player and floor)                              ->  toast + sync
 PlayerTickEvent    every 5 ticks: the clock of a timed quest, the room the player stands in (RoomDwell: 40 ticks, nothing hostile alive)
 LivingDeathEvent   kill quests, ambush quests (entity tag nusmp_dm), the extraction memory (ExtractLedger)
 LivingDamageEvent.Post  health really lost -> "no damage" and "no magic damage" quests
 SystemSync         one S2C packet per player per 10 ticks at most: floor, road, threat, quest, clock, stats   --->  ClientSystem  --->  SystemOverlay
 Shadow Core        right click within 100 ticks and 12 blocks of a kill  ->  ShadowExtraction -> ShadowMinions.raise (a shadow, nusmp_servant)
```

A floor is a band of about ten rooms of a sector (`FloorMap`). "Finding the next staircase" is stepping into a deeper floor of the same road; the quest must be done
BEFORE that, which is exactly one rule of the state machine (section 3, "before the next floor").

---------------------------------------------------------------------------------------------------------------------------------------------

## 3. Decisions and their WHY

**A layer, not an `IGuiOverlay`.** `IGuiOverlay` was removed in NeoForge 1.21; the form is a `LayeredDraw.Layer` registered with `RegisterGuiLayersEvent`
(`event.registerAboveAll(id, layer)`), exactly as `SongClient` and `PageUnlockOverlay` already do. Read from the NeoForge 1.21.1 source (`GuiLayerManager`, fetched from
raw.githubusercontent.com): mod layers sit in ONE flat list beside the vanilla ones and are drawn even when the GUI is hidden or a screen is open (only the vanilla layers are
wrapped in `!hideGui`). So the brief's "hides in chat and inventory" is NOT automatic: `SystemOverlay.render` checks `options.hideGui` and `mc.screen` itself.

**An attachment, not a world table.** The stat points and the quest are per player and must follow the player through death, dimensions and restarts. `AttachmentType`
with a codec, `copyOnDeath()` and a "serialise only if non-empty" predicate does that with no code of ours; the codec uses `optionalFieldOf` everywhere and `SystemState.of`
clamps, so an old or hand-edited save still loads. It is not auto-synced: the client needs things that are not in it (the floor, the road).

**Quests are templates in JSON.** The brief gives one example quest; the kinds, sizes and pay are balance numbers that will change after the first play. A datapack can
retune a row, add one or switch one off (weight 0). The parsers (`QuestParser`, `StatParser`) are pure and library-free, report each problem with the row it belongs to and
skip only the broken row; the shipped files are parsed by the unit tests, so a typo fails the build, not the game.

**The roller is pure and seeded** (`QuestRoller`, `RewardRoller`). Same player on the same floor always gets the same quest, so dying, relogging or re-entering cannot
re-roll an easy one; the kind never repeats twice in a row (unless nothing else is left); a quest the floor cannot satisfy (more rooms than the floor has, an ambush quest
with ambushes switched off) is never offered. The reward is seeded too, so a save-and-reload cannot reroll a bad result.

**"Success before the next floor" is a state machine** (`QuestProgress`: ACTIVE -> DONE or FAILED, exactly once, every later call a no-op). The counting kinds succeed the
moment the count is reached; entering the NEXT floor with such a quest still ACTIVE fails it. The two "reach the next floor" kinds (no damage, speed) are judged by the
arrival itself: still ACTIVE there means the condition held. Idempotence matters: a kill and a room counted in the same tick can never pay twice.

**Damage is judged on `LivingDamageEvent.Post`.** The brief names the incoming-damage event, but that fires before armour, absorption and the shield; a blocked arrow would
fail an "unscathed" quest. `Post.getNewDamage()` is documented as "the amount of health this entity lost", which is the quest's meaning (source: NeoForge 1.21.1
`LivingDamageEvent.java`). Friendly fire (a shadow and its owner) is still cancelled early on the incoming event.

**Rewards are permanent attribute modifiers with fixed ids** (`nusmp:system/<stat>`): applying twice changes nothing, a changed table value replaces the old one instead of
stacking, they can be removed by id, no other system mistakes them for its own, and they are re-applied on login, respawn and dimension change because the points in the
attachment are the truth. Every attribute id is resolved at run time and skipped if absent (Tensura's magicule pool only exists with Tensura).

**"Hostile" is `Enemy` or a Dungeon Master monster.** Tensura's monsters are tamable animals, not `Enemy`; the economy, the HP buffer and the faction drops already ask
`MegaDungeon.isDungeonMonster` as well. So do the kill quests, the cleared-room test and the extraction (`ShadowMinions.isHostile`).

**The shadows reuse the repo's summon pattern** (`soul/PaladinGoals`, `PaladinNet`): the mob's own target goals come out and owner-hurt-by / owner-hurt / follow goals go in
(reflection on `Mob.goalSelector/targetSelector` by official names, no access transformer), and a ten-tick drive does the same decisions for brain-driven mobs
(Tensura's). No new AI framework. A shadow is a fresh creature of the fallen one's type at `minionStrength` of its health and attack, tagged `nusmp_servant` (the turf-war code
already leaves that tag alone), drops nothing, gives no XP, never targets or hurts its owner or a fellow shadow, never leaves the Mega-Dungeon alone, and ends on death,
release (the cap), or `minionMinutes` after its owner was last seen. **Not buffered twice:** the Labyrinth's HP buffer multiplies every hostile that joins; a shadow's
health is already a share of a buffered creature's, so the buffer's own marker (its modifier id with amount 0) goes on before the shadow joins.

**The window is cheap and fail-safe.** The layout is a pure function from "what to say" to a list of marks (`Op.Fill`, `Op.Gradient`, `Op.Text`); the layer only replays the
list: fills, one gradient kind, the default font, no textures, no shaders, 38 to 82 marks for the panel (measured over the preview scenes) and about 6 for each toast. The pose stack is popped in a `finally`; any exception switches the window
off until the server sends news (`ClientSystem.broken`) with one log line instead of one per frame. Because the layout is pure, the SAME class that draws in the game was run
for the preview pictures (section 8) and is unit tested to the pixel.

**A Sync packet is cut to the wire limits.** `writeUtf` throws on an over-long string and the packet is encoded later on the network thread, where an exception disconnects the
player; a datapack quest title can be any length, so every text is clipped first.

---------------------------------------------------------------------------------------------------------------------------------------------

## 4. Files and what each is for

### Server (`labyrinth/mega/system`)
| Class | Why it exists |
|---|---|
| `SystemRegistry` | the one registration entry `MegaSystem` calls: item and component, attachment, payloads, the floor listener, the self-tests, the reload listener, every game-bus handler, the command |
| `SystemConfig` | server config, plain `define`/`defineInRange`, readable before the spec is loaded |
| `SystemState`, `SystemAttachments` | the per-player memory and its codec (the attachment) |
| `Stat`, `QuestKind`, `QuestTemplate`, `QuestCatalog`, `QuestParser`, `StatTable`, `StatParser` | the data model of the two JSON files, pure |
| `SystemReload`, `SystemData` | the reload listener (`ResourceManagerReloadListener`) and the two volatile references it swaps in one assignment |
| `QuestRoller`, `QuestProgress`, `RewardRoller`, `RoomDwell`, `MagicDamage` | the pure rules (see section 3) |
| `SystemQuests` | the lifecycle: the floor listener, the player tick feed, kills, ambushes, damage, admin entry points |
| `SystemRewards`, `SystemStats` | paying a reward; fixed-id attribute modifiers |
| `SystemNet`, `SystemSync`, `SyncThrottle`, `ToastKind`, `SystemText`, `SystemMsg` | the payloads, the throttled sending, the English text map every consumer shares |
| `ShadowItems`, `ShadowCoreItem`, `CoreCharges` | the item, its charges component, the pure charge arithmetic |
| `ExtractLedger`, `ExtractRules`, `ShadowExtraction` | what may be extracted, the bounded memory of fresh kills, using the core |
| `ShadowMinions`, `MinionGoals`, `MinionRoster`, `MinionLife` | raising, driving, capping and ending a shadow |
| `SystemEvents` | the game-bus handlers, as a table of contents; every one wrapped so it cannot break the game's own event |
| `SystemCommand`, `SystemSelfTests` | `/multiverse mega system ...` and the five in-game tests |

### Client (`client/mega/system`)
| Class | Why it exists |
|---|---|
| `SystemClient` | the one client entry: layer, toggle key, client config, logout cleanup, the two payload receivers |
| `SystemOverlay` | the layer: decides whether to draw, translates, hands the facts to the layout, replays the marks |
| `OverlayLayout`, `Op`, `OverlayStyle`, `TextWrap`, `OverlayMath`, `ToastTimeline` | the pure layout, its marks, every number and colour, the title wrap, the arithmetic of bars and animations |
| `ClientSystem`, `SystemClientConfig` | the last picture the server sent; the screen settings |

### Data, assets, tools
`data/nusmp/mega/system/quests.json`, `stats.json`; `data/nusmp/tags/damage_type/mega_system_magic.json` (datapack widening of "magic damage"; default minecraft:magic and
indirect_magic); `data/nusmp/tags/entity_type/shadow_immune.json` (the four vanilla bosses; a datapack adds more). `assets/nusmp/models/item/shadow_core{,_3d,_icon}.json`,
`textures/item/shadow_core{,_model}.png`. Tools: `tools/system/preview_overlay.py` + `OverlayDump.java`, `gen_shadow_core_assets.py` + `preview_item_model.py`,
`gen_system_lang.py` + `LangDump.java`.

---------------------------------------------------------------------------------------------------------------------------------------------

## 5. Data

### quests.json (`{"quests": [ ... ]}`; later rows of the same id replace earlier ones, so a datapack overrides by id)
Row: `id` (a-z 0-9 _), `kind` (CLEAR_ROOMS KILL_N NO_DAMAGE_STRETCH SPEED SURVIVE_AMBUSH), `title` (English; `{n}` and `{t}` stand for the target; the lang key
`message.nusmp.mega.system.quest.<id>` takes over where it exists), `base`, `perFloor`, `min`, `max` (target = `clamp(round(base + perFloor * threat), min, max)`), `weight`
(0 = off), optional `noMagic` (counting kinds only), `rewards {stats [..], points, shadowCore}`, `why`. Shipped: `clear_rooms_no_magic` (the brief's own example, pays INT),
`clear_rooms` (INT or STR), `slay_hostiles` and `slay_no_magic` (STR or AGI), `unscathed` (2 VIT, hardest), `swift_ascent` (seconds on the floor, AGI), `survive_ambush` (SENSE).
A SPEED quest's target is its length in seconds. A boss floor (`FloorMap.isDecennial`) has no quest.

### stats.json
Per stat: `name`, `effects [{attribute, operation add_value|add_multiplied_base|add_multiplied_total, perPoint, cap}]`. Shipped: STR +0.2 attack damage a point (cap 6), AGI +1 % movement
speed (cap 25 %), VIT +1 max health (cap 20), INT +2 % `tensura:max_magicule` (cap 60 %), SENSE +0.25 luck (cap 5) and +1 `tensura:presence_sense_radius` (cap 24). At the
per-stat limit of 30 points the best build is a strong player, not a god. The attribute ids with a namespace other than `minecraft` are skipped when absent.

---------------------------------------------------------------------------------------------------------------------------------------------

## 6. Configuration, commands, keys, lang

**Server** `nusmp-labyrinth-mega-system-server.toml`, section `[system]`: `enabled` true, `abyssalToo` false (the brief puts the System on the Holy road), `questChance` 1.0,
`ambushQuests` true, `statWeight` 70, `shadowCoreWeight` 30, `rechargeFirst` true, `maxStatPoints` 30, `extractWindowTicks` 100, `extractRadius` 12, `maxMinions` 3,
`minionStrength` 0.7, `minionMinutes` 30.
**Client** `nusmp-labyrinth-mega-system-client.toml`, `[system]`: `enabled` true, `scale` 1.0 (0.5 to 2), `anchor` 0 (0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right), `alpha` 0.67
(0.1 to 1), `showStats` true, `toasts` true.
**Key** `key.nusmp.system_toggle`, default Y, category "Multiverse of Anime": hides or shows the window for the session (the config's `enabled` is the lasting switch).
**Command** (permission 2, attached by the lead under `/multiverse mega`): `system status [player]`, `system quest roll|complete|fail|clear [player]`,
`system quest start <id> [player]`, `system stat give <player> <stat> <n>`, `system core give [player]`, `system minions clear [player]`.
**Quest keys** (`NusmpEvents.fire`): `mega.system_quest` (arg: quest id, on success), `mega.shadow` (arg: entity type id, when a shadow is raised).
**Lang**: 69 keys, all in `message.nusmp.mega.system.*` plus `item.nusmp.shadow_core` and `key.nusmp.system_toggle`; written from `SystemText` by
`tools/system/gen_system_lang.py`, so file and code cannot drift. Every server line is a `translatableWithFallback` with the English text, so it reads properly before the fragment is merged.

---------------------------------------------------------------------------------------------------------------------------------------------

## 7. The Shadow Core

`nusmp:shadow_core`: one to a stack, epic, fire-resistant, three charges (data component `nusmp:shadow_core_charges`, absent = full). Right click within `extractWindowTicks`
of a hostile non-boss creature you killed on a covered road, and within `extractRadius` of where it fell: a charge is spent and a shadow rises. A held core announces
"EXTRACTION READY" once per kill. Never extractable: a boss (any of the mod's definitions, wandering bosses), anything already someone's servant (a shadow, Paladin, Soul
Corpse, shadow soldier, relic summon, tamed pet), a named or persistent creature, a type in `#nusmp:shadow_immune`, a creature that cannot fight, a kill by anyone but a player.
The last charge turns the core into nothing (a message, no leftover item); a quest reward recharges a held core first (`rechargeFirst`). Creative players spend no charge.

**The held model.** A trinket, so no engravings. Following the Genesis Core of 1.51.0 (not the weapon mesh pipeline): a vanilla elements model with
`neoforge:separate_transforms`, the 16x16 sprite in the GUI and the 3D model everywhere else, built by `tools/system/gen_shadow_core_assets.py`: a stepped dark crystal turned a
quarter, a full-bright violet heart band, four orbiting shards, a dark disc with a violet rim. WHY not `WeaponRenderer`: it exists for blades and relics with per-frame
glow and needs a registration line in a shared Java file and an entry in the 3,200-line weapon generator; a trinket gains nothing from it, and this way the item needs no shared
file edit. (If the owner wants the pulsing glow later, the upgrade path is a `weapon_meshes/shadow_core.txt` plus one line in `WeaponRenderer.BASE_WEAPONS`.)

---------------------------------------------------------------------------------------------------------------------------------------------

## 8. The window, seen

`python tools/system/preview_overlay.py` compiles `OverlayDump.java` against the mod's real `OverlayLayout`, measures text with the game's own font atlas (fetched once from a
public mirror of the vanilla assets into `build/`, never committed) and paints every mark on a mock dungeon screenshot: `system_overlay.png` (panel, quest, stats, three toasts),
`system_scenes.png` (14 states: counting, typing, long title, timed, nearly out of time, unharmed, complete, failed, back on another floor, boss floor, no quest, Abyssal at -120,
no stats row, a glitch frame) and `system_anchors.png` (the four corners). `python tools/system/preview_item_model.py` renders the Shadow Core model from four sides and from
above on a dark and a light background next to its icon. The renderer is NOT the game's (no real lighting); shape, colour and layout are what it shows.

The look in numbers (all in `OverlayStyle`): panel width 164 pixels (it widens when a row needs more), fill 0xAA06222E-ish as a gradient from 0x0B3344 to 0x051B26 at the
player's opacity (default 0.67), a one-pixel border 0x3DE0FF, four corner brackets 0x9BF3FF (7 long, 2 thick), scanlines every 5 pixels creeping one pixel per three ticks and a
soft sweep band crossing every 170 ticks, the header "[ SYSTEM ]" with a half-second blinking cursor and a colour-split glitch for two short flickers every 7.5 seconds, the
floor / road / threat row, the quest title (typed out once when the quest is new, at most three lines), a segmented bar (at most twelve segments, adding up to the bar's width
exactly) with "3 / 5" or COMPLETE / FAILED, a clock row with a draining bar for timed quests (amber under a minute, red and blinking under twenty seconds), a state line for the
"unharmed" quest, a "QUEST OF FLOOR +3" line when the player is back on an earlier floor, and the five-cell stat row. Toasts (cyan information, green success, red failure, gold
reward, violet shadow) slide in from the left in seven ticks, type at about 34 characters a second, hold, and fade over the last 16 of 110 ticks; at most three show at once.

---------------------------------------------------------------------------------------------------------------------------------------------

## 9. Tests

Pure JUnit (152): `QuestRollerTest` (determinism, scaling, no repeat, impossible kinds avoided, weights, chance), `QuestProgressTest` (every transition, idempotence, fail on the
next floor), `RewardRollerTest`, `MagicDamageTest`, `QuestParserTest` and `StatTableTest` (they parse the SHIPPED files), `RoomDwellTest`, `ExtractTest`,
`MinionBookkeepingTest`, `SystemStateTest` (codec round trips, damaged saves, the text map), `OverlayMathTest`, `TextWrapTest`, `OverlayLayoutTest` (nothing leaves the panel or the
screen at every anchor and scale, no two texts overlap, the bar adds up, the toasts, the colours). **Mutation check:** 24 deliberate breakages of the pure classes were applied one
at a time to a copy of the tree; 23 failed a test, and the one that survived is an equivalent mutant (a redundant `Math.min` in `OverlayMath.litSegments`).
In-game self-tests: `system.data`, `system.sync` (both payloads round-trip through the real codecs, an over-long title is cut), `system.quest` (floor 1 ACTIVE, complete and pay, floor 2 a
new kind, unfinished at floor 3 FAILED, a revisit starts nothing, the boss floor has no quest), `system.stats` (fixed-id modifier, idempotent, cap, remove and re-apply),
`system.extract` (a zombie becomes a shadow with the tag, owner and scaled health, refusals, the cap, the last charge).

---------------------------------------------------------------------------------------------------------------------------------------------

## 10. Lead integration (the exact lines)

1. `NUSMP.java`, server configs: `modContainer.registerConfig(ModConfig.Type.SERVER, com.newuniverse.nusmp.labyrinth.mega.system.SystemConfig.SPEC, "nusmp-labyrinth-mega-system-server.toml");`
2. `NUSMP.java`, inside `if (FMLEnvironment.dist == Dist.CLIENT) { ... }`: `com.newuniverse.nusmp.client.mega.system.SystemClient.init(modEventBus, modContainer);` (it registers the layer, the key and
   the client config itself).
3. `MultiverseCommands` (the `/multiverse mega` node): `.then(com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.command())`.
4. `LabyrinthCreative.IDS`: append `"shadow_core"` (or `output.accept(com.newuniverse.nusmp.labyrinth.mega.system.SystemRegistry.SHADOW_CORE.get())`).
5. Lang: merge `msy.lang.json` (69 keys) into `en_us.json`.
6. For SAFE (not mine): `RestEvents.isHostile` should return false for a shadow (`m.getTags().contains("nusmp_servant")`), or a resting player's own shadow zombie stops the rest.
No `WeaponRenderer` line, no key-registration line, nothing for the item model.

---------------------------------------------------------------------------------------------------------------------------------------------

## 11. Verified online, and not verified (honest list)

Read from the NeoForge 1.21.1 sources on raw.githubusercontent.com (not guessed): that mod GUI layers are drawn even when the GUI is hidden or a screen is open
(`GuiLayerManager`); what `LivingDamageEvent.Post#getNewDamage` is (health actually lost); that cancelling `EntityTravelToDimensionEvent` makes `Entity.changeDimension` return null (the
`Entity` patch). The glyph advances of the default font (last opaque column + 1, plus 1; space 4) come from the vanilla `ascii.png` of the 1.21.1 assets.

Nothing was run in the game. Unverified: the look (the preview uses the real layout and the game's font but not the game's renderer: blending of many translucent fills, the
sweep band, and the scale at your GUI size are judged by eye only); that `DeltaTracker.getGameTimeDeltaPartialTick(false)` is the right partial tick for a layer; the Shadow Core
model in hand (display transforms copied from the Genesis Core, not seen) and the `neoforge_data` full-bright on the heart and shards; the reflection on `Mob.goalSelector/targetSelector` by official names at runtime
(precedent: `PaladinGoals`); Tensura brains under the ten-tick drive (shadows of brain-driven mobs rely on `ATTACK_TARGET` memory and `doHurtTarget`); the attribute
`tensura:presence_sense_radius` (the field `PRESENCE_SENSE_RADIUS` exists in the API dump, the registry path is assumed; it is skipped if absent); the floor tracker that fires
`fireFloorEntered` (another builder's: the self-test fires it by hand); balance of every number in the two JSON files.

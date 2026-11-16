# Dungeon bosses and dungeon NPCs: the audit

Owner's request: "fix all the bosses for the dungeons as well to actually be better looking, actually have good abilities, effects, sounds, and everything", and later "show me what the bosses
will look like and everything, each of the NPCs and whatnot".

**How this was rated.** By reading the code and the docs and by looking at the repo's own previews (a software rasteriser of the real `GeoDraw`, not the game). Nothing was seen in a running game.
Ratings are 1 (bare) to 5 (as good as the Time Magic / Genesis standard). "Before" is the state at the start of this work; "after" only exists for what this change touches.
Source of the facts: this repository's code and docs (no wiki research was needed: the bosses are the mod's own).

The Living Dungeon was removed in 1.58.0; the active dungeon is the Mega-Dungeon (`labyrinth/mega`, 1000 floors, a Holy road and an Abyssal road).

## 1. Bosses

| # | Boss | Id / class | Where it spawns | Body (model) | Abilities before | VFX | Sounds | Already reworked | Look | Abil. | FX | Snd |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| 1 | **Floor Warden** | `anime_multiverse:floor_warden`, `FloorWardenEntity` | the Seal floors (E2 trial), sector halls, and as the wandering "Warden" (`mega/wanderers.json`, weight 5) | art kit `arc_floor_warden` + the player-model kit (`playermodel/floor_warden`, the in-game default), a plain dark suit with a small hammer | Ground Slam when close, Shadow Step when far, two Living Armors once at half health; no telegraph, no phases | smoke ring and a witch burst | family voices (`armor`): attack, cast, aggro | no (it is the base of every Guardian) | **2** | **2** | **2** | **3** |
| 2 | **Labyrinth Guardian** (sector boss, Decennial and pocket boss) | `labyrinth_guardian`, `LabyrinthGuardianEntity extends FloorWardenEntity` | one per sector, the 200 Decennial halls, rift pockets | the Warden suit, or one of 22 abyssal looks x 3 phases (`AbyssalRenderer`) | the Warden's two moves scaled by sector threat, traits (storm, gravity ...), phase two, DnD profile moves, damage budget | trait effects, phase captions | family voices | yes: abyssal looks (1.94.0), wave 3 decennial moves, lair actions, boss bars | 4 | 3 | 3 | 3 |
| 3 | **Goliath** | `GoliathEntity` (`dungeon/floor/goliath`) | a dungeon floor boss | Blender mesh (`entity_meshes/goliath.txt`, wave 2) | wave 1 moves (appended), slam, stomp, roar | shaped slam effects | boss sounds | waves 1, 2 (mesh), 3 | 4 | 4 | 3 | 3 |
| 4 | **Charybdis Spawn** | `CharybdisEntities.CHARYBDIS_SPAWN` | water floors | art kit `arc_charybdis_spawn` (+ form 2) | wave 1 moves appended | wave 1 | boss sounds | wave 1 | 3 | 3 | 3 | 3 |
| 5 | **Minotaur, Lucifero, God of Order** | Zogratis / order bosses | dungeon and arc floors | Blender meshes (wave 2) | their own move books, wave 1 appended | shaped effects | boss sounds | waves 1, 2 | 4 | 4 | 4 | 3 |
| 6 | **Wandering Warden** | the Warden above, drawn from `wanderers.json` (also a Ravager, Tensura beasts ...) | sector halls | the Warden's | the Warden's, plus the Dungeon Master's patrol / hide logic | none of its own | none of its own | no | 2 | 2 | 2 | 3 |

## 2. Dungeon creatures (the food web, `dungeon/`)

| # | Creature | Id / class | Where | Body | Abilities before | Look | Abil. | FX | Snd |
|---|---|---|---|---|---|---|---|---|---|
| 7 | **Direwolf** | `dungeon_direwolf`, `DirewolfEntity` | dens of the dungeon floors, sleeps when full | art kit `arc_dungeon_direwolf` (a grey wolfman) | a bite; hunts armours and slimes | 3 | **1** | 1 | 3 (family `beast`) |
| 8 | **Basilisk** | `basilisk`, `BasiliskEntity` | dens | art kit `arc_basilisk` (a coiled green lizard) | a bite with poison | 3 | **2** | 1 | 3 (family `drake`) |
| 9 | **Living Armor** | `living_armor`, `LivingArmorEntity` | everywhere, also the Warden's two guards | art kit `arc_living_armor` (found armour, a sword) | a bite | 3 | **1** | 1 | 3 (family `armor`) |
| 10 | **Dungeon Slime** | `dungeon_slime`, `DungeonSlimeEntity` | tier 1 food | art kit `arc_dungeon_slime` | grazes, grows; harmless | 3 | 1 (by design: food) | 1 | 3 |

## 3. Dungeon NPCs and the 400-mob Foundry

| # | NPC group | Class | Where | Body | Behaviour | Look | Behaviour | FX | Snd |
|---|---|---|---|---|---|---|---|---|---|
| 11 | **Foundry mobs** (400: 200 Holy, 200 Abyssal) | `FoundryMobEntity` | every floor by tier | one art-kit character each, on the player rig; no two alike (guard-tested) | archetype brain, 1-3 abilities, `TelegraphedAttackGoal` telegraphs | 4 | 3 | 3 | 3 |
| 12 | **Hub NPCs** (merchant, quest giver, guides) | `elite/hub/*`, `HubNpcBase` | the hub plaza | art-kit or player-model characters | dialogue and shops; invulnerable, peaceful by design | 3 | 3 (not a combat NPC) | 2 | 2 |
| 13 | **Arc NPCs** (Black Bulls, Danmachi ...) | `ArcNpcEntity`, `ArcHubNpcs` | arc hubs | art-kit characters (`ArcNpcRenderer`) | idle, talk, a few poses | 4 | 3 | 2 | 2 |
| 14 | **Wandering bosses** | see 6 | | | | | | | |

## 4. Ranking: the weakest first

1. **Living Armor** (abilities 1, FX 1): it is the commonest thing in the dungeon and it only bites.
2. **Direwolf** (abilities 1, FX 1): a pack hunter with no pack behaviour.
3. **Floor Warden** (look 2, abilities 2, FX 2): the first boss most players meet, a plain suit and two untelegraphed moves; every Guardian inherits it.
4. **Wandering Warden**: the same body, so it improves with the Warden.
5. **Basilisk** (abilities 2, FX 1): a poison bite and nothing else, for a "gaze" monster.
6. Charybdis Spawn (all 3): fine but unremarkable, wave 1 already worked on it.
7. Hub and arc NPCs: fine for their job; sounds and effects are the weakest, but they are not fighters.
8. Slime: harmless by design.

## 5. What this change did

Reworked, in this order: **Floor Warden** (new look, five moves, three phases, an ultimate, an enrage timer), then **Direwolf**, **Basilisk**, **Living Armor** (one to three signature moves each).
Not reworked and why:

* **Labyrinth Guardians and the 200 Decennial bosses**: they extend the Warden but already have their own traits, phases, abyssal looks and wave 3 moves; the new move set is switched off for them
  (`getClass() == FloorWardenEntity.class`), so nothing of theirs changes.
* **Goliath, Minotaur, Lucifero, God of Order, Charybdis Spawn**: already reworked in waves 1 to 3 (models, moves, effects); another pass without a game to watch would be guessing.
* **Slime**: food by design.
* **Foundry mobs**: 400 characters with telegraphed abilities; a per-mob pass is a different, large job. Left as is.
* **Hub and arc NPCs**: not fighters; no change.

The new previews are in `docs/dungeon_bosses_previews/` (see `docs/dungeon_bosses_rework.md`).

# The Mega-Dungeon, second edition (1.58.0 to 1.60.0): floors as levels, parties, no old dungeon, new themes, the characters of the arcs

The owner's request (their words, shortened): remove the old dungeon completely but keep the blocks the new one uses; make the new dungeon multiplayer friendly
and work with FTB Teams; the Solo Leveling System is for every player in the dungeon, its buff works only in the dungeons, and its window can be moved on the
screen with keybinds; fix the floors, which are "together": each floor must be a level of its own and the player must find a descend/ascend (like D&D) to
reach the next; more themes (the Cursed Archive on the evil route, the Holy Church on the good route, and many more from D&D, fantasy and horror); the actual
bosses and NPCs of the arcs and anime. This file is the BINDING contract of the builders. Read it with `docs/labyrinth/mega.md` and `docs/labyrinth/mega_floors.md`.

## 1. One sector is one floor (the floor model)

Until 1.57.0 a sector was ten floors laid out flat and "banded" by distance (`FloorMap`, `floors.banding`): the owner's picture is that the floors are
together, so nothing ever feels like going down or up. From 1.58.0:

* **A sector is exactly one floor.** `FloorMap.floorsPerSector()` is 1 (config `floors.perSector`, default 1; the value 10 keeps the old banding code alive for
  anyone who wants it, rule 2 of the owner). Holy road: floors +1, +2 ... ; Abyssal road: floors -1, -2 ...; sector `chainIndex` k of a road is floor k+1.
* **You start at the foot of the stairs and must find the way on.** Every floor has an **arrival stairwell** (where you appear; it leads back to the previous
  floor, or to Floor 0 on the first floor) and a **forward stairwell** (up on the Holy road, down on the Abyssal road) placed at the far end of the floor
  (the room farthest from the Landing by walking distance). Between them is the explorable dungeon: halls, corridors, caverns, traps, puzzles, a Safe Zone.
  Floors are SMALLER than the old sectors (about 18 to 40 rooms; config `mega.floorRooms`), because there are now ten times as many of them.
* **A stairwell is a real room** (template folder `structure/mega/stairwell/`, kind `RoomKind.STAIRWELL`, appended): a shaft with a staircase that visibly
  climbs or drops, lit, with a landing at each end. Walking onto its last step (a marker `nusmp:stair_exit`, one sector link) takes the player to the
  linked floor's arrival stairwell, with a short fade, the way `ChainTravel` already moves players between Landings. The forward stairwell is usable only when
  the next floor is ACTIVE; until then it says so ("the stairs are still forming") and queues it.
* **The next floor is generated when a party ENTERS a floor** (queue floor n+1 as soon as floor n is reached), so the stairs are normally ready by the time they are found.
* **Boss floors**: every tenth floor (|floor| % 10 == 0) is a boss floor: its forward stairwell lies BEHIND the Guardian's arena, the Gatekeeper door, the
  antechamber and the vault (Crucible). The forward stairwell opens when the Crucible is resolved (`mega.expandAfterVault`). Floors 1..9, 11..19 ... have no
  guardian: a boss or a mini-boss may still stand in a hall of them (the arc characters, section 6), but nothing blocks the stairs.
* **Themes**: the theme belongs to a DECADE of floors (floors 1..10 share one, 11..20 the next ...), drawn by `RegionTheme.pick` for the decade and replaced by the
  relic the player took at the Crucible for the next decade (`ThemeOverride`, unchanged in meaning). So "the next ten floors' theme" of the original brief holds.
* **Why**: one level at a time is what a D&D dungeon is: the party reads the map, finds the stair, decides whether to go on, and a short floor is cheap to
  generate, to load and to finish in a session. A flat ten-floor sector made the "floors" a label on a big plan.

## 2. Parties and FTB Teams (wave 2, builder PARTY)

* A **dungeon party** is the FTB Team of a player (through the reflection bridge that already exists for squads, `multiverse/squad`, never a hard dependency);
  a player with no team is a party of one. Teammates share one chain of floors per road; different parties never meet in the same floors.
* **Per-party lanes**: `MegaGrid.laneZ(polarity)` becomes `laneZ(polarity, partyIndex)`; the party index is allocated on first entry and saved. Config
  `party.mode`: `"party"` (default, instanced chains) or `"shared"` (everyone in party 0: the 1.57.0 behaviour).
* Progress that belongs to the party: the floors reached, the Crucible relic of a decade, floor quests (the System). Progress that belongs to the player: the
  Directional Lock, stats, gear.
* Why instanced: the dungeon is persistent and consumable (a Crucible gives one relic per ten floors, a boss dies once). In a shared dungeon the first group
  would take everything. Why FTB Teams: the owner's servers already group players there.

## 3. The System for everyone, and where its buff works (builder SYS2)

* The System overlay (Solo Leveling window, floor quests, rewards) works for EVERY player inside the dungeon dimensions (`nusmp:mega_dungeon` and the Hub),
  on both roads, not only the Holy road, and for every player of the party at once (a floor quest completes for all present).
* The permanent stat rewards of the System are applied as attribute modifiers that are **active only while the player is inside the dungeon dimensions**; they
  are removed on leaving (dimension change, death, logout) and restored on entering. They are never lost, only dormant.
* The window can be moved: client keybinds (category "Multiverse: System"): toggle edit mode, nudge left/right/up/down (arrow keys, with Shift for ten
  pixels), reset, and drag with the mouse in edit mode. The position is saved in the CLIENT config. Why client config: it is a preference of one screen.

## 4. Removing the old dungeon (builder PRUNE)

"Remove the old dungeon completely but keep the blocks if the new one uses them." The old dungeon is: the Spire layout of 1.35.0 (dimension
`nusmp:labyrinth`, its structure set, pools `nusmp:labyrinth/*`, `mega.layout = "spire"`), the Living Dungeon (`dungeon/**`: tiers, city, atmosphere, nexus zones, the
Maw, dens, floor arenas), the legacy Dungeon Gates and Maws, and the config switches that kept them (`labyrinth.engine`, `legacyGates`, `mega.layout`).
What stays: everything the Mega-Dungeon, the Hub, the Gateway, the Guard, the D&D layer, the economy, quests, relics, bosses and any other system still
reads. The builder first MAPS the dependencies (who uses what), then removes in steps, then proves the whole tree still type-checks (`tc_full.sh`).
Blocks and items that only the old dungeon used may go; entities and items that other systems (relics, charter grimoires, quests, the arcs) still reference
stay registered. Old worlds: unknown blocks become air, unknown entities vanish: that is accepted by the owner for the removed dungeon; nothing of the
Mega-Dungeon, the Hub or the player data may break.

## 5. Themes (builders THEME-A to THEME-D)

Sixteen new themes, each BOUND TO A ROAD (`RegionTheme.Route`, `exclusive()`): a good-route theme is never drawn on the Abyssal road and the other way round.

| Builder | Theme (enum id) | Road | Idea (D&D / fantasy / horror source) |
|---|---|---|---|
| A | `HOLY_CHURCH` | Holy | a great cathedral: nave, pews, stained glass, crypt below, bell tower, reliquary |
| A | `CELESTIAL_COURT` | Holy | marble terraces and golden halls of a heavenly court (Mount Celestia) |
| A | `SKY_CITADEL` | Holy | a cloud giant's castle in the sky: white stone, wind, long drops |
| A | `MOUNTAIN_MONASTERY` | Holy | a monks' retreat: timber, paper lanterns, training halls, a bell |
| B | `FEYWILD_GROVE` | Holy | the Feywild: giant glowing flowers and mushrooms, cherry and azalea, will-o'-wisps |
| B | `DWARVEN_FORGEHOLD` | Holy | a dwarven hold: stonework, copper, anvils, lava channels, vaults, mine carts |
| B | `CRYSTAL_GEODE` | Holy | amethyst caverns, budding crystals, glowing geodes |
| B | `SUNKEN_TEMPLE` | Holy | a flooded temple of a forgotten good god: prismarine, glass, air pockets |
| C | `CURSED_ARCHIVE` | Abyssal | a massive book archive of forbidden knowledge: endless shelves, reading halls, chained tomes, a scriptorium, a lectern of black fire |
| C | `NECROPOLIS` | Abyssal | a city of the dead: catacombs, mausoleums, ossuaries, bone bridges |
| C | `VAMPIRE_CASTLE` | Abyssal | Ravenloft: gothic halls, crimson drapes, a dining hall, coffins, a chapel gone wrong |
| C | `HAUNTED_MANOR` | Abyssal | a haunted house: parlours, a nursery, a basement, mirrors, cobwebs |
| D | `FUNGAL_UNDERDARK` | Abyssal | the Underdark: huge mushroom forests, glowing spores, drow-like architecture |
| D | `INFERNAL_FOUNDRY` | Abyssal | the Nine Hells as a factory: nether bricks, chains, lava, cages, contract halls |
| D | `DROWNED_CRYPT` | Abyssal | a sunken crypt: dripping vaults, kelp, drowned dead, tide gates |
| D | `SPIDER_WARRENS` | Abyssal | a spider queen's webbed warren: cobweb tunnels, egg sacs, silk bridges |

The builders research the themes on the internet (D&D 5e sources, the Forgotten Realms and Ravenloft settings, fantasy and horror dungeon design) and
record the URLs and what each decided in their doc. Every theme is a COMPLETE data set: enum entry, `data/nusmp/mega/themes/<id>.json` (palette swaps and
keywords), an encounter table `data/nusmp/mega/encounters/<id>.json`, at least five signature rooms (templates) restricted to the theme (section 5.1),
atmosphere (the existing theme data), the relic themes that lead to it (`RelicThemes` data) and lang entries.

### 5.1 Rooms that belong to a theme (`TemplateThemes`, pure, in `labyrinth/mega/gen`)

A template whose id starts with a prefix listed in a theme file may only be drawn in sectors of that theme; every other template stays free for all themes.
A theme file `data/nusmp/mega/template_themes/<theme>.json`:

```json
{ "theme": "holy_church",
  "templates": ["nusmp:mega/hall/holy_church_", "nusmp:mega/set/holy_church_"],
  "relative":  { "mega/hall/holy_church_": 8, "mega/set/holy_church_": 6 } }
```

`templates` are id prefixes (the rooms live in the usual kind folders, named `<theme>_<room>.nbt`); `relative` are the weights `tools/labyrinth/gen_mega_pools.py`
puts on them inside their folder (the same meaning as `mega_pools.json`, written here so builders never edit that shared file). The generator's data loader
reads the folder and the assembler asks `TemplateThemes.allows(templateId, sectorTheme)` whenever it draws from a pool (builder GEN2 wires this; it also adds the
`theme` of each template to `src/test/resources/mega/catalog.json`).

## 6. The characters of the arcs (builders ARC-*, see `$S/arc/*.md`)

`com.newuniverse.nusmp.arc` (contract in this commit): `ArcCharacter` (one pure record per character), `ArcRoster`, `ArcCharacters.bootstrap()` (the list), `ArcBoss` (a
thin layer over the existing `ZogratisBoss` framework, so every boss has moves with telegraphs, power steps, an arena that resets, a damage budget, the second
health bar and the Charter Grimoire drop for free), `ArcEntities.boss(...)` (one call registers type, attributes and look), `ArcLook`, and ONE shared NPC entity
`nusmp:arc_npc` whose identity is a synced string. Bosses stand as floor bosses (the Mega-Dungeon picks one by road, floor and theme), in story arenas, and unlock
each other in a story chain (`ArcCharacter.prerequisites`); NPCs live in the Sanctuary Hub and give the arc quests.

## 7. Release plan

1.58.0 the dungeon rework (sections 1 to 4), 1.59.0 the sixteen themes, 1.60.0 the characters. Each is verified by the CI build and the `[smoke] [strict]` server
test before it is handed over. Nothing of this runs in the game until the owner plays it.

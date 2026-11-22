# Advancements: the tab "Multiverse of Anime" (1.49.0)

Why: the owner asked for advancements next to a full FTB Quests line. The quest book is built on top of these advancements (an FTB advancement task completes when a team member has the advancement), so the tree is also the progress model of the book. Everything here is **new**: nothing like it existed. The 25 hidden files under `data/anime_multiverse/advancement/convergence` (from the removed Convergence system) are **kept untouched**.

Nothing was run in Minecraft. See "Unverified in the game".

## Where it comes from

- `tools/quests/spec_adv.py` is the single source of truth (ids, titles, descriptions, parents, icons, criteria). `python tools/quests/gen_advancements.py` writes `src/main/resources/data/anime_multiverse/advancement/**` and the lang fragment `tools/quests/lang/qst.lang.json`; `--check` fails if anything is out of date.
- The lang fragment must be merged into `assets/anime_multiverse/lang/en_us.json` (`python tools/quests/merge_lang.py`, idempotent). `AdvancementDataTest` fails while a key is missing.
- `src/test/java/com/newuniverse/nusmp/quests/AdvancementDataTest.java` checks every file: parses, parent exists, the roots are `root` and the `tab/*` heads (each with the background), no cycle, translations exist, triggers are known, event keys are in the contract, mod items / entities / effects / dimensions exist, every criterion is used by a requirement.

## The tabs (4.101.0)

Why: the game lays an advancement out only from the SHAPE of its tree (no x/y exists in 1.21): siblings stack in rows, depth is columns. One root meant one tab of about 333 rows that nobody could move around in. Now the same tree is cut into 15 tabs of 14 to 30 rows each, with the same shared background (`multiverse_weave.png`, unchanged).

- Each tab head is a display advancement under `advancement/tab/` that completes on the first tick (`minecraft:tick`, no toast, no chat, no reward), so it is always open and every branch below it shows. Ids, criteria, rewards, quest-book links and saved progress of the existing advancements are untouched; only the `parent` line of the branch heads changed.
- The tabs: Multiverse of Anime (`root`: start, world, spirits, squads, arcs and the rest), Magic Awakening, Soul Elements (Flame, Water, Wind, Light), Soul Earth, Soul Space/Time/Empty, Soul Battle/Darkness, Soul Fantasy, Spells, Character Pages, Bosses and Challenges, Relics and Weapons, Dungeon, Story, More Sagas, Faces 1 and 2 (the `arc/met_*` characters, 16 per tab).
- Why not chains of rows: the game shows an unfinished advancement only when its parent or grandparent is done, so chaining children into rows would hide later members. Why not make the soul hubs roots: their real criteria would hide the tab until earned.
- `tools/quests/adv_layout.py` holds the tab list and is applied at the end of `spec_adv.py`. `ADV_LAYOUT=classic python tools/quests/gen_advancements.py` regenerates the old single tree (the setting that restores the old behaviour). `devil/contract_crafted` was a stray root without a background; it now hangs under `spirits/devil_contract`.
- `AdvancementLayoutTest` fails when a tab is taller than 32 rows or wider than 16 columns, when there are more than 20 tabs, when a tab lacks the shared background, or when an advancement ends outside every tab.
- Cost: data only (one tick criterion per tab head); no tick work, entity, packet or client draw beyond the extra tab icons. The Earth Soul (25 rows) and Battle/Darkness (30 rows) tabs are still tall because their hubs are flat lists; the game gives no way to place them differently.

## The tree (468 advancements, 15 of them tab heads)

| Branch (folder) | Advancements | What is in it |
| --- | --- | --- |
| `root` | 1 | the first tab root; granted on the first tick |
| `tab` | 15 | the tab heads of 4.101.0 (see above) |
| `start` | 15 | grimoire, ceremony, wizard, first spell, pages, mastery |
| `magic` | 162 | awakening, one hub per soul type (11), one advancement per magic family (105), a signature and an all-magics challenge per soul type, per quest branch (9) and the distinct-magics ladder |
| `spells` | 42 | signature casts, merging, engraving, the twenty canon pages, the special arts (butoh, song, corpse, paladin, rouge, thread, shadow, time, unite) |
| `bosses` | 21 | every boss the mod has, kill counts, the Triad, Lucius stages, the all-bosses challenge |
| `relics` | 25 | obtain, equip, the seven grades, forge, sockets, tiers, Djinn, pairs, sets, Vecna, the Infinity Stones |
| `weapons` | 16 | the named weapons, the anti-magic blades, the Genesis edge, engravings |
| `dungeon` | 30 | Mega-Dungeon, floor bosses, cooking, Labyrinth, wardens, keys, seals, vault |
| `spirits` | 11 | spirit charm and contract, elemental lords, devil contract, union, the nine cores |
| `squads` | 6 | found, join, captain, vice, charters |
| `world` | 15 | kingdoms, library, dream, the arena of the Final Hour, the codex |
| `challenges` | 11 | long deeds: no-relic arena win, necromancer, every magic, Genesis in hand |
| `arcs` | 13 | one milestone per anime arc (13), chained in story order |
| `future` | 11 | hidden placeholders for the bosses that are not made yet (granted by their death once they exist) |
| `arc` | 73 | the characters of the arcs, one advancement per character of tools/quests/arc_roster.txt (defeated a boss, met a friend), one per saga cleared, and one for the arenas (spec_arc.py, refreshed by tools/quests/gen_arc_adv.py) |

Frames: 248 task, 144 goal, 60 challenge. Goals and challenges announce in chat, tasks do not. Every advancement grants experience (5 to 2000 points, more for challenges).

The tab background is `textures/gui/advancements/multiverse_weave.png` (3.44.0, a seamless 32x32 tile made by `tools/quests/gen_tab_background.py`; the advancement screen tiles it in 16x16 cells). Two other candidates, `sigil_tiles.png` and `grimoire_pages.png`, are in the same folder; the old altar tile (`textures/block/grimoire_altar_top.png`) is kept for the altar block. To switch, change the `background` string in `data/anime_multiverse/advancement/root.json` and `ROOT_BG` in `tools/quests/spec_adv.py`.

## Triggers

Vanilla triggers are used wherever the game can see the thing itself: `minecraft:tick` (the root), `inventory_changed` (carry a mod item), `changed_dimension` (the five mod dimensions), `player_killed_entity` (bosses, together with the event), `player_interacted_with_entity` (the Tower Wizard, a Spirit Lord), `consume_item` (cooked monster meat), `effects_changed` (mod effects), `minecraft:impossible` (the future-boss placeholders).

Everything else is the mod's own trigger `anime_multiverse:event` (implemented in `quests/NusmpEventTrigger`, fired by `NusmpEvents.fire`, see `docs/quests_java.md`):

```json
{"trigger": "anime_multiverse:event", "conditions": {"key": "spell.cast", "count": 10, "arg": "thread"}}
```

`key` is required; `count` (default 1, written only when above 1) is compared with the per-player counter of the key (or of `key|arg` when `arg` is given) with `>=`; `arg` is an exact string match. Boss kills use an OR of the event and the vanilla kill, so they work whichever one the Java side delivers. The keys:

| Key | Argument | Used by (advancements) | Counts used | Hook point suggested to the Java side |
| --- | --- | --- | --- | --- |
| `arc.arena` | boss id lower case | 1 | 1 | a player is carried into a story arena by the Chronicle Sigil: arc/arena/ArcArenaTravel.enter |
| `arc.defeated` | character id lower case (dante, lucius, goliath ...) | 31 | 1 | a boss of the arcs falls: arc/story/ArcStory.defeated (the arcs' own bosses), arc/world/ArcWorldCredit (the seven older ones); credits the fighters and, with arc.shareStoryCredit, their nearby FTB teammates |
| `arc.met` | character id lower case | 37 | 1 | first conversation with a friendly character of the arcs (arc/story/ArcStory.met) |
| `arc.saga_clear` | saga id lower case (elf_reincarnation, spade_raid, wizard_king, seven_deadly_sins, danmachi) | 5 | 1 | a player's story closes a saga (every boss of it defeated), once per saga: arc/world/ArcWorldCredit.settle |
| `arena.entered` | - | 2 | 1 | arena travel |
| `arena.hourglass_used` | - | 1 | 1 | Hourglass of the Final Hour used |
| `arena.victory` | lucius (what the Java side sends); norelic is an extra arg requested by the data side (also fired when the winner carried no relic) | 2 | 1 | arena won |
| `boss.defeated` | entity type path (dante_zogratis, lucius_zogratis, zagred, goliath, charybdis_spawn, floor_warden, god_of_order, ...) | 34 | 1, 3, 10, 25 | boss death; credit the squad (QuestTeams) |
| `boss.phase` | <boss>:<stage>, e.g. lucius:2, lucius:3, zagred:2 (extra format) | 5 | 1 | stage change of a boss |
| `butoh.dance` | - | 2 | 1 | butoh/ButohDance start |
| `ceremony.completed` | - | 2 | 1 | multiverse/Ceremony after the grimoire is handed over |
| `corpse.raised` | - | 3 | 1, 10 | corpse/Corpses |
| `devil.union` | - | 2 | 1 | devil union |
| `dungeon.entered` | - | 2 | 1 | enter the dungeon by any door (no longer fired since the Living Dungeon was removed in 1.58.0; the criterion stays in the advancement so saved progress counts) |
| `dungeon.floor_boss` | goliath or charybdis | 4 | 1 | floor boss dies |
| `grimoire.kingdom` | clover, spade, heart or diamond (extra key requested by the data side) | 8 | 1 | same moment as grimoire.received, one event per grant (extra key) |
| `grimoire.opened` | - | 1 | 1 | grimoire GUI open (GrimoireItem) |
| `grimoire.received` | magic id lower case (what the Java side sends) | 3 | 1, 5 | ceremony / altar grant (multiverse/Ceremony) |
| `labyrinth.entered` | - | 4 | 1 | labyrinth gate travel |
| `labyrinth.seal_opened` | - | 1 | 1 | gatekeeper door opened with the key |
| `labyrinth.vault_opened` | - | 1 | 1 | vault pedestal opened |
| `labyrinth.warden_defeated` | - | 1 | 1 | Floor Warden dies |
| `magic.merged` | - | 3 | 1, 5 | core/magic/merge |
| `mastery.max` | - | 1 | 1 | mastery bar reaches 100% |
| `mega.aligned` | ? | 2 | 1 | ? |
| `mega.crucible_choice` | ? | 1 | 1 | ? |
| `mega.entered` | ? | 3 | 1 | ? |
| `mega.floor` | ? | 2 | 1 | ? |
| `mega.gear` | ? | 2 | 1 | ? |
| `mega.hub` | ? | 1 | 1 | ? |
| `mega.rested` | ? | 1 | 1 | ? |
| `mega.sector_cleared` | ? | 2 | 1, 3 | ? |
| `mega.shadow` | ? | 1 | 1 | ? |
| `mega.system_quest` | ? | 1 | 1 | ? |
| `mega.wanderer_survived` | ? | 1 | 1 | ? |
| `page.unlocked` | page id (e.g. thread_trap, paladin_conversion) | 64 | 1, 5, 10 | multiverse/MasteryPages when a page opens |
| `paladin.converted` | - | 3 | 1, 5 | soul/Paladins |
| `relic.djinn_bound` | - | 1 | 1 | Djinn bound to a vessel |
| `relic.equipped` | - | 2 | 1 | a relic is worn or wielded |
| `relic.forged` | - | 1 | 1 | relic forge used |
| `relic.grade` | grade lower case: normal special rare unique legend god genesis | 8 | 1 | when a relic of that grade is obtained |
| `relic.obtained` | relic id | 3 | 1, 10, 30 | first time a relic item is in the player's inventory |
| `relic.socketed` | - | 1 | 1 | socket core applied |
| `relic.tier` | - | 1 | 1 | tier ritual completed |
| `relic.unique_ledger` | - | 1 | 1 | player entered in the Unique Ledger |
| `rouge.saved` | - | 3 | 1 | thread/RougeFateEvents |
| `shadow.travel` | - | 2 | 1 | shadow/ShadowTravel |
| `song.sung` | - | 2 | 1 | song/SongTicker first pulse |
| `spell.cast` | magic id = MagicType name lower case (flame, black_oil, world_tree ...) | 426 | 1, 3, 25, 50, 500 | GrimoireBook.castPage success path |
| `spell.cast.signature` | magic id | 214 | 1, 10, 50 | same, when the page is a signature page |
| `spell.distinct` | - | 6 | 5, 10, 25, 50, 105 | fired once the first time a player casts a magic id never cast before; the counter is the number of distinct magics (extra key) |
| `spirit.contract` | - | 2 | 1 | spirit contract signed |
| `squad.captain` | - | 1 | 1 | multiverse/Squads |
| `squad.created` | - | 1 | 1 | multiverse/Squads |
| `squad.joined` | - | 1 | 1 | multiverse/Squads |
| `squad.vice` | - | 1 | 1 | multiverse/Squads |
| `thread.trap` | - | 2 | 1 | thread trap sprung |
| `time.stop` | - | 2 | 1 | blackclover/TimeStop |
| `unite.mode` | canis, equus, felis or gallus | 12 | 1 | shadow/UniteWatcher |
| `weapon.engraved` | - | 2 | 1, 10 | item/WeaponEngravings applied |
| `weapon.genesis` | - | 1 | 1 | Genesis power of the Demon-Slayer |
| `wizard.questionnaire` | - | 2 | 1 | wizard/Questionnaire finished |

Keys of the original brief that no advancement uses: , plus `convergence.stage` and `convergence.anomaly` (the Convergence system was removed in 1.14.0, so there is nothing to hook).

## Add an advancement in three lines

```python
# tools/quests/spec_adv.py
add("dungeon/new_thing", "dungeon/mega", "Title", "What to do.", "anime_multiverse:labyrinth_gate", ev("mega.entered", 3), xp=20)
```
then `python tools/quests/gen_advancements.py` and `python tools/quests/merge_lang.py`.

## The future-boss placeholders

`anime_multiverse:future/<arc>_<boss>` (hidden, criterion `impossible`). They are awarded by `NusmpEvents.futureBoss(player, arc, boss)`. The table is in `docs/quests.md`.

## Unverified in the game

- No advancement was loaded by Minecraft. The JSON follows the 1.21.1 format (`display.icon.id`, `background` as a texture path, `ContextAwarePredicate` lists for entity conditions) but a typo in a vanilla condition would only show as a log error at load time.
- The custom trigger codec belongs to the Java side; the conditions above (`key`, `count`, `arg`, no `player`) are the contract.
- Icons are checked against the mod's lang and item models, not against the registry.

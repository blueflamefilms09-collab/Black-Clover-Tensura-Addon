# Advancements: the tab "Multiverse of Anime" (1.49.0)

Why: the owner asked for advancements next to a full FTB Quests line. The quest book is built on top of these advancements (an FTB advancement task completes when a team member has the advancement), so the tree is also the progress model of the book. Everything here is **new**: nothing like it existed. The 25 hidden files under `data/nusmp/advancement/convergence` (from the removed Convergence system) are **kept untouched**.

Nothing was run in Minecraft. See "Unverified in the game".

## Where it comes from

- `tools/quests/spec_adv.py` is the single source of truth (ids, titles, descriptions, parents, icons, criteria). `python tools/quests/gen_advancements.py` writes `src/main/resources/data/nusmp/advancement/**` and the lang fragment `tools/quests/lang/qst.lang.json`; `--check` fails if anything is out of date.
- The lang fragment must be merged into `assets/nusmp/lang/en_us.json` (`python tools/quests/merge_lang.py`, idempotent). `AdvancementDataTest` fails while a key is missing.
- `src/test/java/com/newuniverse/nusmp/quests/AdvancementDataTest.java` checks every file: parses, parent exists, one root, no cycle, translations exist, triggers are known, event keys are in the contract, mod items / entities / effects / dimensions exist, every criterion is used by a requirement.

## The tree (369 advancements)

| Branch (folder) | Advancements | What is in it |
| --- | --- | --- |
| `root` | 1 | the tab root; granted on the first tick |
| `start` | 15 | grimoire, ceremony, wizard, first spell, pages, mastery |
| `magic` | 162 | awakening, one hub per soul type (11), one advancement per magic family (105), a signature and an all-magics challenge per soul type, per quest branch (9) and the distinct-magics ladder |
| `spells` | 42 | signature casts, merging, engraving, the twenty canon pages, the special arts (butoh, song, corpse, paladin, rouge, thread, shadow, time, unite) |
| `bosses` | 21 | every boss the mod has, kill counts, the Triad, Lucius stages, the all-bosses challenge |
| `relics` | 25 | obtain, equip, the seven grades, forge, sockets, tiers, Djinn, pairs, sets, Vecna, the Infinity Stones |
| `weapons` | 16 | the named weapons, the anti-magic blades, the Genesis edge, engravings |
| `dungeon` | 20 | Living Dungeon, Maw, floor bosses, cooking, Labyrinth, wardens, keys, seals, vault |
| `spirits` | 11 | spirit charm and contract, elemental lords, devil contract, union, the nine cores |
| `squads` | 6 | found, join, captain, vice, charters |
| `world` | 15 | kingdoms, library, dream, the arena of the Final Hour, the codex |
| `challenges` | 11 | long deeds: no-relic arena win, necromancer, every magic, Genesis in hand |
| `arcs` | 13 | one milestone per anime arc (13), chained in story order |
| `future` | 11 | hidden placeholders for the bosses that are not made yet (granted by their death once they exist) |

Frames: 207 task, 109 goal, 53 challenge. Goals and challenges announce in chat, tasks do not. Every advancement grants experience (5 to 2000 points, more for challenges).

The tab background is `textures/block/grimoire_altar_top.png` (an existing 16x16 texture of the mod; the advancement screen tiles it).

## Triggers

Vanilla triggers are used wherever the game can see the thing itself: `minecraft:tick` (the root), `inventory_changed` (carry a mod item), `changed_dimension` (the five mod dimensions), `player_killed_entity` (bosses, together with the event), `player_interacted_with_entity` (the Tower Wizard, a Spirit Lord), `consume_item` (cooked monster meat), `effects_changed` (mod effects), `minecraft:impossible` (the future-boss placeholders).

Everything else is the mod's own trigger `nusmp:event` (implemented in `quests/NusmpEventTrigger`, fired by `NusmpEvents.fire`, see `docs/quests_java.md`):

```json
{"trigger": "nusmp:event", "conditions": {"key": "spell.cast", "count": 10, "arg": "thread"}}
```

`key` is required; `count` (default 1, written only when above 1) is compared with the per-player counter of the key (or of `key|arg` when `arg` is given) with `>=`; `arg` is an exact string match. Boss kills use an OR of the event and the vanilla kill, so they work whichever one the Java side delivers. The keys:

| Key | Argument | Used by (advancements) | Counts used | Hook point suggested to the Java side |
| --- | --- | --- | --- | --- |
| `arena.entered` | - | 2 | 1 | arena travel |
| `arena.hourglass_used` | - | 1 | 1 | Hourglass of the Final Hour used |
| `arena.victory` | lucius (what the Java side sends); norelic is an extra arg requested by the data side (also fired when the winner carried no relic) | 2 | 1 | arena won |
| `boss.defeated` | entity type path (dante_zogratis, lucius_zogratis, zagred, goliath, charybdis_spawn, floor_warden, god_of_order, ...) | 34 | 1, 3, 10, 25 | boss death; credit the squad (QuestTeams) |
| `boss.phase` | <boss>:<stage>, e.g. lucius:2, lucius:3, zagred:2 (extra format) | 5 | 1 | stage change of a boss |
| `butoh.dance` | - | 2 | 1 | butoh/ButohDance start |
| `ceremony.completed` | - | 2 | 1 | multiverse/Ceremony after the grimoire is handed over |
| `corpse.raised` | - | 3 | 1, 10 | corpse/Corpses |
| `devil.union` | - | 2 | 1 | devil union |
| `dungeon.entered` | - | 2 | 1 | enter the living dungeon or the labyrinth by any door |
| `dungeon.floor_boss` | goliath or charybdis | 4 | 1 | floor boss dies |
| `dungeon.maw` | - | 1 | 1 | step through a Dungeon Maw |
| `grimoire.kingdom` | clover, spade, heart or diamond (extra key requested by the data side) | 8 | 1 | same moment as grimoire.received, one event per grant (extra key) |
| `grimoire.opened` | - | 1 | 1 | grimoire GUI open (GrimoireItem) |
| `grimoire.received` | magic id lower case (what the Java side sends) | 3 | 1, 5 | ceremony / altar grant (multiverse/Ceremony) |
| `labyrinth.entered` | - | 4 | 1 | labyrinth gate travel |
| `labyrinth.seal_opened` | - | 1 | 1 | gatekeeper door opened with the key |
| `labyrinth.vault_opened` | - | 1 | 1 | vault pedestal opened |
| `labyrinth.warden_defeated` | - | 1 | 1 | Floor Warden dies |
| `magic.merged` | - | 3 | 1, 5 | core/magic/merge |
| `mastery.max` | - | 1 | 1 | mastery bar reaches 100% |
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
add("dungeon/new_thing", "dungeon/living_dungeon", "Title", "What to do.", "nusmp:dungeon_maw", ev("dungeon.maw", 3), xp=20)
```
then `python tools/quests/gen_advancements.py` and `python tools/quests/merge_lang.py`.

## The future-boss placeholders

`nusmp:future/<arc>_<boss>` (hidden, criterion `impossible`). They are awarded by `NusmpEvents.futureBoss(player, arc, boss)`. The table is in `docs/quests.md`.

## Unverified in the game

- No advancement was loaded by Minecraft. The JSON follows the 1.21.1 format (`display.icon.id`, `background` as a texture path, `ContextAwarePredicate` lists for entity conditions) but a typo in a vanilla condition would only show as a log error at load time.
- The custom trigger codec belongs to the Java side; the conditions above (`key`, `count`, `arg`, no `player`) are the contract.
- Icons are checked against the mod's lang and item models, not against the registry.

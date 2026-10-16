# The questline: FTB Quests book "Multiverse of Anime" (1.49.0)

Why: the owner asked for "a fully fleshed out questline with FTB Quests and Teams, with all the main anime arcs as quests when the bosses for them are made". This is the data half (the pack and its generators); the Java half (the advancement trigger, the fire points, the installer command, Teams credit) is in `docs/quests_java.md`.

**New**, not a replacement: the mod had no quest pack before (the Convergence chapter of 0.39 was written by code and removed in 1.14.0). Nothing was run in Minecraft. See "Unverified in the game".

## What is in the book

22 chapters in 3 groups, 339 quests (60 boss, 25 final, 23 intro, 67 mile, 50 side, 11 soon, 103 step), 10 reward tables. Nearly every task is an `advancement` task on the advancement tree (`docs/advancements.md`), plus `item` (obtain, squad treasuries), `kill` (hunters), `dimension` and `checkmark` tasks.

| Group | Chapter (file) | Title | Quests | Optional | Boss-kind quests |
| --- | --- | --- | --- | --- | --- |
| The Journey | `prologue` | Prologue: First Steps | 20 | 3 | 0 |
| The Journey | `magic_mastery` | Magic Mastery | 49 | 3 | 12 |
| The Journey | `weapons_relics` | Weapons and Relics | 43 | 6 | 5 |
| The Journey | `dungeon_labyrinth` | Dungeon and Labyrinth | 21 | 7 | 3 |
| The Journey | `spirits_devils` | Spirits and Devils | 13 | 2 | 3 |
| The Journey | `squads_kingdoms` | Squads and Kingdoms | 27 | 12 | 5 |
| The Anime Arcs | `arc01_hage` | Arc 1: Hage Village | 8 | 2 | 0 |
| The Anime Arcs | `arc02_exam` | Arc 2: Magic Knights Entrance Exam | 8 | 2 | 0 |
| The Anime Arcs | `arc03_dungeon` | Arc 3: The Dungeon Arc | 8 | 1 | 2 |
| The Anime Arcs | `arc04_triad` | Arc 4: Dark Triad: The Royal Capital | 8 | 1 | 1 |
| The Anime Arcs | `arc05_eye` | Arc 5: Eye of the Midnight Sun | 8 | 1 | 1 |
| The Anime Arcs | `arc06_witches` | Arc 6: The Witches' Forest | 8 | 2 | 1 |
| The Anime Arcs | `arc07_temple` | Arc 7: The Underwater Temple | 7 | 1 | 1 |
| The Anime Arcs | `arc08_selection` | Arc 8: Royal Knights Selection Exam | 9 | 1 | 2 |
| The Anime Arcs | `arc09_spade_raid` | Arc 9: Spade Kingdom Raid | 8 | 1 | 3 |
| The Anime Arcs | `arc10_heart` | Arc 10: Heart Kingdom: Elf Reincarnation | 9 | 2 | 2 |
| The Anime Arcs | `arc11_diamond` | Arc 11: The Diamond Kingdom | 8 | 2 | 1 |
| The Anime Arcs | `arc12_final` | Arc 12: Spade Kingdom Invasion: The Final Arc | 9 | 1 | 1 |
| The Anime Arcs | `arc13_devil` | Arc 13: The Devil Arc | 11 | 1 | 5 |
| Bosses and Challenges | `gauntlet` | The Boss Gauntlet | 25 | 6 | 12 |
| Bosses and Challenges | `lucius_god` | Lucius: The God Tier | 18 | 8 | 4 |
| Bosses and Challenges | `hard_mode` | Hard Mode | 14 | 13 | 7 |

Quest kinds (shape and default reward): `intro` (rounded square, the welcome node), `step` (circle), `mile` (diamond, journey cache), `boss` (octagon, boss trophy), `final` (hexagon, a pick-one reward), `side` (square, optional), `soon` (octagon with a barrier icon: a locked placeholder). Arcs are visible and ordered but not hidden: chapter 2, 3 and 13 wait for the previous arc's final quest, the others wait only for the previous arc's (free) welcome node. `hide_until_deps_complete` is off everywhere.

## Arc to boss table

Existing bosses award their quest through `boss.defeated` (arg = entity path); a placeholder quest sits on a hidden advancement `nusmp:future/<arc>_<boss>` that nothing awards today. **To make an arc's boss real**: add the boss, then call `NusmpEvents.futureBoss(serverPlayer, "<arc>", "<boss>")` for the killer (and, with Teams credit on, the squad) in the boss' death hook (`BossHooks` style); the quest completes and the hidden advancement appears. Nothing in the pack changes.

| Arc chapter | Live boss quests (advancement) | Placeholder (advancement id) | Placeholder arguments for `futureBoss` | What to hook when the boss is made |
| --- | --- | --- | --- | --- |
| `arc01_hage` 1 Hage Village | none (no boss in this arc) | none | - | no boss in this arc |
| `arc02_exam` 2 Magic Knights Entrance Exam | none (no boss in this arc) | none | - | no boss in this arc |
| `arc03_dungeon` 3 The Dungeon Arc | Goliath (`nusmp:bosses/goliath`) | `nusmp:future/dungeon_arc_vetto` | `"dungeon_arc", "vetto"` | death hook of Vetto |
| `arc04_triad` 4 Dark Triad: Royal Capital | none (no boss in this arc) | `nusmp:future/dark_triad_boss` | `"dark_triad", "boss"` | death hook of the Dark Triad arc's boss |
| `arc05_eye` 5 Eye of the Midnight Sun | none (no boss in this arc) | `nusmp:future/eye_midnight_boss` | `"eye_midnight", "boss"` | death hook of the Eye of the Midnight Sun arc's boss |
| `arc06_witches` 6 The Witches' Forest | none (no boss in this arc) | `nusmp:future/witches_forest_boss` | `"witches_forest", "boss"` | death hook of the Witch Queen |
| `arc07_temple` 7 The Underwater Temple | Spawn of Charybdis (`nusmp:bosses/charybdis_spawn`) | `nusmp:future/underwater_temple_boss` | `"underwater_temple", "boss"` | death hook of the temple's guardian |
| `arc08_selection` 8 Royal Knights Selection Exam | Floor Warden (`nusmp:bosses/floor_warden`) | `nusmp:future/royal_knights_boss` | `"royal_knights", "boss"` | death hook of the examiner |
| `arc09_spade_raid` 9 Spade Kingdom Raid | Dante (`nusmp:bosses/dante_zogratis`); Vanica (`nusmp:bosses/vanica_zogratis`); Zenon (`nusmp:bosses/zenon_zogratis`) | none | - | nothing: the arc has all the bosses it needs |
| `arc10_heart` 10 Heart Kingdom: Elf Reincarnation | Wind Spirit Lord (optional) (`nusmp:bosses/wind_spirit_lord`) | `nusmp:future/heart_elf_boss` | `"heart_elf", "boss"` | death hook of the reincarnated elf |
| `arc11_diamond` 11 The Diamond Kingdom | none (no boss in this arc) | `nusmp:future/diamond_kingdom_boss` | `"diamond_kingdom", "boss"` | death hook of the Diamond Kingdom's champion |
| `arc12_final` 12 Spade Kingdom Invasion and the Final Arc | Lucius (`nusmp:bosses/lucius_zogratis`) | none | - | nothing: the arc has all the bosses it needs |
| `arc13_devil` 13 The Devil Arc | Zagred (`nusmp:bosses/zagred`); Devil Avatar (`nusmp:bosses/devil_avatar`) | `nusmp:future/devil_arc_megicula`; `nusmp:future/devil_arc_lucifero`; `nusmp:future/devil_arc_astaroth` | `"devil_arc", "megicula"`; `"devil_arc", "lucifero"`; `"devil_arc", "astaroth"` | death hook of Megicula; death hook of Lucifero; death hook of Astaroth |

The same placeholders as a flat list: `nusmp:future/dungeon_arc_vetto`, `nusmp:future/dark_triad_boss`, `nusmp:future/eye_midnight_boss`, `nusmp:future/witches_forest_boss`, `nusmp:future/underwater_temple_boss`, `nusmp:future/royal_knights_boss`, `nusmp:future/heart_elf_boss`, `nusmp:future/diamond_kingdom_boss`, `nusmp:future/devil_arc_megicula`, `nusmp:future/devil_arc_lucifero`, `nusmp:future/devil_arc_astaroth`.

## Teams: how progress is shared, and how it relates to Squads

- FTB Quests keeps **all progress per FTB team**, never per player. A player always belongs to a team (their own one-player party team until they join another). A task an individual completes (an advancement task is checked on each online member's tick) completes for the whole team. Item tasks with `consume_items` (the squad treasury, armoury and library) are filled by any member and belong to the team. Rewards are claimed per member by default; the quests marked as team rewards (`team_reward: true`) are claimed once for the whole team.
- A **Magic Knight squad is one FTB Teams party team** (`multiverse/Squads`). So a squad's quest progress *is* the squad's: founding a squad or joining one changes which team's progress you see, and FTB Quests handles it (read from `ServerQuestFile.teamCreated` / `playerChangedTeam`): the founder's own progress is **copied** into the new party team; a player who joins a party has their progress **merged** into the party's; a player who leaves gets their old one-player progress back, and rewards already claimed stay claimed.
- The "Squads and Kingdoms" chapter teaches this: found / join / captain / vice (advancements `squad.*` fired by the Squads class), a team treasury and armoury and library (consumed items), three **squad hunt** quests whose tasks are bosses that different members can kill, and honour-system standings quests (a checkmark: the book cannot read the standings).
- With `quests.shareSquadCredit` (Java side) a boss kill or an arena victory is also fired for every online team member within 128 blocks, so the squad's advancements follow the squad, not only the killer.

## Event keys: the contract with the Java side

The advancement trigger `nusmp:event` takes `key`, optional `count` (>= 1, compared with the per-player counter of the key, or of `key|arg` when `arg` is set) and optional `arg`. The keys the advancement tree uses, with the meaning of `arg` and the place to fire them from (the full agreement, including what the Java side really sends, is `docs/quests_java.md` and the shared note QST_TRG.md):

| Key | Argument | Fired from |
| --- | --- | --- |
| `grimoire.received` | magic id lower case (what the Java side sends) | ceremony / altar grant (multiverse/Ceremony) |
| `grimoire.kingdom` | clover, spade, heart or diamond (extra key requested by the data side) | same moment as grimoire.received, one event per grant (extra key) |
| `grimoire.opened` | - | grimoire GUI open (GrimoireItem) |
| `ceremony.completed` | - | multiverse/Ceremony after the grimoire is handed over |
| `spell.cast` | magic id = MagicType name lower case (flame, black_oil, world_tree ...) | GrimoireBook.castPage success path |
| `spell.cast.signature` | magic id | same, when the page is a signature page |
| `spell.distinct` | - | fired once the first time a player casts a magic id never cast before; the counter is the number of distinct magics (extra key) |
| `page.unlocked` | page id (e.g. thread_trap, paladin_conversion) | multiverse/MasteryPages when a page opens |
| `mastery.max` | - | mastery bar reaches 100% |
| `wizard.questionnaire` | - | wizard/Questionnaire finished |
| `relic.obtained` | relic id | first time a relic item is in the player's inventory |
| `relic.equipped` | - | a relic is worn or wielded |
| `relic.grade` | grade lower case: normal special rare unique legend god genesis | when a relic of that grade is obtained |
| `relic.forged` | - | relic forge used |
| `relic.socketed` | - | socket core applied |
| `relic.tier` | - | tier ritual completed |
| `relic.djinn_bound` | - | Djinn bound to a vessel |
| `relic.unique_ledger` | - | player entered in the Unique Ledger |
| `labyrinth.entered` | - | labyrinth gate travel |
| `labyrinth.warden_defeated` | - | Floor Warden dies |
| `labyrinth.vault_opened` | - | vault pedestal opened |
| `labyrinth.seal_opened` | - | gatekeeper door opened with the key |
| `dungeon.entered` | - | enter the living dungeon or the labyrinth by any door |
| `dungeon.maw` | - | step through a Dungeon Maw |
| `dungeon.floor_boss` | goliath or charybdis | floor boss dies |
| `arena.entered` | - | arena travel |
| `arena.hourglass_used` | - | Hourglass of the Final Hour used |
| `arena.victory` | lucius (what the Java side sends); norelic is an extra arg requested by the data side (also fired when the winner carried no relic) | arena won |
| `boss.defeated` | entity type path (dante_zogratis, lucius_zogratis, zagred, goliath, charybdis_spawn, floor_warden, god_of_order, ...) | boss death; credit the squad (QuestTeams) |
| `boss.phase` | <boss>:<stage>, e.g. lucius:2, lucius:3, zagred:2 (extra format) | stage change of a boss |
| `spirit.contract` | - | spirit contract signed |
| `devil.union` | - | devil union |
| `squad.created` | - | multiverse/Squads |
| `squad.joined` | - | multiverse/Squads |
| `squad.captain` | - | multiverse/Squads |
| `squad.vice` | - | multiverse/Squads |
| `magic.merged` | - | core/magic/merge |
| `weapon.engraved` | - | item/WeaponEngravings applied |
| `weapon.genesis` | - | Genesis power of the Demon-Slayer |
| `butoh.dance` | - | butoh/ButohDance start |
| `song.sung` | - | song/SongTicker first pulse |
| `corpse.raised` | - | corpse/Corpses |
| `paladin.converted` | - | soul/Paladins |
| `rouge.saved` | - | thread/RougeFateEvents |
| `thread.trap` | - | thread trap sprung |
| `shadow.travel` | - | shadow/ShadowTravel |
| `unite.mode` | canis, equus, felis or gallus | shadow/UniteWatcher |
| `time.stop` | - | blackclover/TimeStop |

Keys marked *extra* are not in the original list: `spell.distinct` (counter of distinct magics cast), `grimoire.kingdom`, `arena.victory` with arg `norelic`, and the `boss.phase` argument form `<boss>:<stage>`. Until the Java side fires them the advancements that need them (the `magic/distinct_*` ladder and `challenges/all_magic`, the four kingdom advancements, `challenges/norelic`) cannot be earned; every other advancement and quest works without them.

## Add a quest in three lines

```python
# tools/quests/spec_q_journey.py (or the arc / boss module)
c.q("my_key", "Title", (col, row), "nusmp:magic_stone", "step", "Subtitle", "One paragraph.\nAnother.",
    deps=["earlier_key", "other_chapter/quest_key"], tasks=[T.adv("start/first_spell")], rewards=[R.xp(20)])
```
then `python tools/quests/gen_quests.py && python tools/quests/validate_snbt.py`. An id is the hash of `quest/<chapter>/<key>`: never rename a key you have shipped (add a new quest instead).

## How to regenerate and check

```
python tools/quests/gen_advancements.py      # advancements + lang fragment
python tools/quests/merge_lang.py            # fragment -> en_us.json
python tools/quests/gen_quests.py             # the pack
python tools/quests/validate_snbt.py          # parse + check every key against the FTB source
python tools/quests/gen_docs.py               # these two documents
python tools/quests/gen_quests.py --check    # CI: nothing out of date
```

`validate_snbt.py --src <FTB Quests checkout>` re-derives `ftbq_keys.json` (the keys each object's `readData` reads) from the FTB Quests source; the committed copy lets the check run without the checkout.

## How the installer works (Java side, summary)

The pack is bundled in the jar under `/quest_pack/` (FTB's files directly at its top level; `README.txt` is documentation and is not copied). `/nusmp quests install|status|update|uninstall` (operators) copies the files into `config/ftbquests/quests/`, writes a marker `.nusmp_pack` (pack version, hash, file list), and tells you to `/ftbquests reload`. Details and safety rules: `docs/quests_java.md`.

## The format, derived from the FTB Quests source

- `data.snbt` (`version: 13`), `chapter_groups.snbt`, `chapters/<name>.snbt` (the file name must equal the `filename` key and be lower case), `reward_tables/<name>.snbt`, `lang/en_us.snbt`.
- In 1.21 titles, subtitles and descriptions live **only in the lang file**, under the keys `<object type>.<16 hex id>.<title | quest_subtitle | quest_desc | chapter_subtitle>` (`quest`, `chapter`, `chapter_group`, `reward_table`, `task`, `reward`, and `file.0000000000000001.title` for the book). The loader still accepts the old inline keys but converts them and marks the book dirty, so the pack does not use them.
- Ids are 16 upper-case hex digits in a string; the top bit is kept clear because FTB parses them with `Long.parseLong(hex, 16)`. A reward table is referenced from a `loot` / `choice` reward as `table_id: <decimal long>L` (a *long* tag: it is read with `getLong`).
- `x`, `y` and `size` must be double tags (`1.5d`), `count` / `value` any number. Quest `icon` is `{id: "item:id"}` (a compound; a bare string is ignored for icons). Descriptions are lists of lines, `""` is a blank line; `&` + a colour or style code works, `{` starts a text tag and is avoided; the generator rejects non-ASCII text (the SNBT reader only knows a few escapes).
- Shapes used: `circle rsquare diamond octagon hexagon square` (all exist in FTB Quests' `textures/shapes`).

## Validation result

```
quest pack src/main/resources/quest_pack: 22 chapters, 3 groups, 339 quests, 470 tasks (416 advancement tasks), 536 rewards, 10 reward tables, 357 dependency edges, 1109 lang entries; 0 errors, 0 warnings
```

`validate_snbt.py` also ran with deliberately broken packs (unknown key, a missing dependency, an int `x`, a missing advancement) and reported each of them; `QuestPackTest` fails on the same mutations.

`QuestPackTest` (CI) asserts: unique 16-hex ids that fit a signed long, stable golden ids, chapter file names, dependencies exist and are acyclic, reward tables and groups exist, `table_id` is a long tag, every advancement task has its JSON file, every future-boss advancement has a quest, lang keys point at the right kind of object, ASCII-safe text.

## Unverified in the game

- **FTB Quests never loaded this pack.** The format was derived by reading the FTB Quests 2101.1.x and FTB Library sources and checked with a re-implementation of their SNBT parser; a mistake the source reading missed would show as a log line at load or a missing field.
- Layouts (x/y) were laid out by hand on a grid and checked for overlaps only; whether the lines read well on screen is untested.
- Advancement tasks are detected on a 5-tick auto-submit per online member; a quest whose advancement was earned before the quest unlocked completes the moment it unlocks.
- Team behaviour (shared progress, `team_reward`) is as the FTB Teams / FTB Quests source describes it; not tried with two players.
- The reward items are vanilla and a few mod consumables; no relic is ever a reward (relics are unique and exclusive to the vaults).

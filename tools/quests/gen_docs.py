#!/usr/bin/env python3
"""Write docs/quests.md and docs/advancements.md from the specs (tables) and the prose in this file.

Why: the chapter map, the arc-to-boss table and the event-key table are facts that live in the spec; generating them keeps the docs from drifting
away from the data they describe. The prose (how it works, why, what is unverified) is written here once.
"""
import collections
import io
import contextlib
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spec_lib as L  # noqa: E402
import spec_adv  # noqa: E402
import spec_quests  # noqa: E402
import validate_snbt  # noqa: E402

DOCS = os.path.join(L.ROOT, "docs")


def validator_summary():
    keys = json.load(open(validate_snbt.KEYS_JSON, encoding="utf-8"))
    errors, warns, stats = validate_snbt.validate(validate_snbt.DEFAULT_PACK, keys)
    return ("quest pack src/main/resources/quest_pack: %d chapters, %d groups, %d quests, %d tasks (%d advancement tasks), %d rewards, "
            "%d reward tables, %d dependency edges, %d lang entries; %d errors, %d warnings"
            % (stats["chapters"], stats["groups"], stats["quests"], stats["tasks"], stats["adv_tasks"], stats["rewards"], stats["tables"],
               stats["deps"], stats["lang"], len(errors), len(warns)))


EVENTS = [
    # key, arg meaning, where it is fired (hook point suggested to the Java side)
    ("grimoire.received", "magic id lower case (what the Java side sends)", "ceremony / altar grant (multiverse/Ceremony)"),
    ("grimoire.kingdom", "clover, spade, heart or diamond (extra key requested by the data side)", "same moment as grimoire.received, one event per grant (extra key)"),
    ("grimoire.opened", "-", "grimoire GUI open (GrimoireItem)"),
    ("ceremony.completed", "-", "multiverse/Ceremony after the grimoire is handed over"),
    ("spell.cast", "magic id = MagicType name lower case (flame, black_oil, world_tree ...)", "GrimoireBook.castPage success path"),
    ("spell.cast.signature", "magic id", "same, when the page is a signature page"),
    ("spell.distinct", "-", "fired once the first time a player casts a magic id never cast before; the counter is the number of distinct magics (extra key)"),
    ("page.unlocked", "page id (e.g. thread_trap, paladin_conversion)", "multiverse/MasteryPages when a page opens"),
    ("mastery.max", "-", "mastery bar reaches 100%"),
    ("wizard.questionnaire", "-", "wizard/Questionnaire finished"),
    ("relic.obtained", "relic id", "first time a relic item is in the player's inventory"),
    ("relic.equipped", "-", "a relic is worn or wielded"),
    ("relic.grade", "grade lower case: normal special rare unique legend god genesis", "when a relic of that grade is obtained"),
    ("relic.forged", "-", "relic forge used"),
    ("relic.socketed", "-", "socket core applied"),
    ("relic.tier", "-", "tier ritual completed"),
    ("relic.djinn_bound", "-", "Djinn bound to a vessel"),
    ("relic.unique_ledger", "-", "player entered in the Unique Ledger"),
    ("labyrinth.entered", "-", "labyrinth gate travel"),
    ("labyrinth.warden_defeated", "-", "Floor Warden dies"),
    ("labyrinth.vault_opened", "-", "vault pedestal opened"),
    ("labyrinth.seal_opened", "-", "gatekeeper door opened with the key"),
    ("dungeon.entered", "-", "enter the living dungeon or the labyrinth by any door"),
    ("dungeon.maw", "-", "step through a Dungeon Maw"),
    ("dungeon.floor_boss", "goliath or charybdis", "floor boss dies"),
    ("arena.entered", "-", "arena travel"),
    ("arena.hourglass_used", "-", "Hourglass of the Final Hour used"),
    ("arena.victory", "lucius (what the Java side sends); norelic is an extra arg requested by the data side (also fired when the winner carried no relic)", "arena won"),
    ("boss.defeated", "entity type path (dante_zogratis, lucius_zogratis, zagred, goliath, charybdis_spawn, floor_warden, god_of_order, ...)",
     "boss death; credit the squad (QuestTeams)"),
    ("boss.phase", "<boss>:<stage>, e.g. lucius:2, lucius:3, zagred:2 (extra format)", "stage change of a boss"),
    ("spirit.contract", "-", "spirit contract signed"),
    ("devil.union", "-", "devil union"),
    ("squad.created", "-", "multiverse/Squads"),
    ("squad.joined", "-", "multiverse/Squads"),
    ("squad.captain", "-", "multiverse/Squads"),
    ("squad.vice", "-", "multiverse/Squads"),
    ("magic.merged", "-", "core/magic/merge"),
    ("weapon.engraved", "-", "item/WeaponEngravings applied"),
    ("weapon.genesis", "-", "Genesis power of the Demon-Slayer"),
    ("butoh.dance", "-", "butoh/ButohDance start"),
    ("song.sung", "-", "song/SongTicker first pulse"),
    ("corpse.raised", "-", "corpse/Corpses"),
    ("paladin.converted", "-", "soul/Paladins"),
    ("rouge.saved", "-", "thread/RougeFateEvents"),
    ("thread.trap", "-", "thread trap sprung"),
    ("shadow.travel", "-", "shadow/ShadowTravel"),
    ("unite.mode", "canis, equus, felis or gallus", "shadow/UniteWatcher"),
    ("time.stop", "-", "blackclover/TimeStop"),
]


def usage():
    use = collections.defaultdict(lambda: [set(), set(), 0])
    for a in spec_adv.S.advs:
        for n, t, c in a.crit["list"]:
            if t == "nusmp:event":
                u = use[c["key"]]
                if "arg" in c:
                    u[0].add(c["arg"])
                u[1].add(c.get("count", 1))
                u[2] += 1
    return use


def adv_doc():
    S = spec_adv.S
    by = collections.OrderedDict()
    for a in S.advs:
        by.setdefault(a.path.split("/")[0], []).append(a)
    desc = {
        "root": "the tab root; granted on the first tick",
        "start": "grimoire, ceremony, wizard, first spell, pages, mastery",
        "magic": "awakening, one hub per soul type (11), one advancement per magic family (105), a signature and an all-magics challenge per soul "
                 "type, per quest branch (9) and the distinct-magics ladder",
        "spells": "signature casts, merging, engraving, the twenty canon pages, the special arts (butoh, song, corpse, paladin, rouge, thread, shadow, time, unite)",
        "arcs": "one milestone per anime arc (13), chained in story order",
        "future": "hidden placeholders for the bosses that are not made yet (granted by their death once they exist)",
        "bosses": "every boss the mod has, kill counts, the Triad, Lucius stages, the all-bosses challenge",
        "relics": "obtain, equip, the seven grades, forge, sockets, tiers, Djinn, pairs, sets, Vecna, the Infinity Stones",
        "weapons": "the named weapons, the anti-magic blades, the Genesis edge, engravings",
        "dungeon": "Living Dungeon, Maw, floor bosses, cooking, Labyrinth, wardens, keys, seals, vault",
        "spirits": "spirit charm and contract, elemental lords, devil contract, union, the nine cores",
        "squads": "found, join, captain, vice, charters",
        "world": "kingdoms, library, dream, the arena of the Final Hour, the codex",
        "challenges": "long deeds: no-relic arena win, necromancer, every magic, Genesis in hand",
    }
    lines = ["# Advancements: the tab \"Multiverse of Anime\" (1.49.0)", "",
             "Why: the owner asked for advancements next to a full FTB Quests line. The quest book is built on top of these advancements (an FTB "
             "advancement task completes when a team member has the advancement), so the tree is also the progress model of the book. "
             "Everything here is **new**: nothing like it existed. The 25 hidden files under `data/nusmp/advancement/convergence` (from the removed "
             "Convergence system) are **kept untouched**.", "",
             "Nothing was run in Minecraft. See \"Unverified in the game\".", "",
             "## Where it comes from", "",
             "- `tools/quests/spec_adv.py` is the single source of truth (ids, titles, descriptions, parents, icons, criteria). "
             "`python tools/quests/gen_advancements.py` writes `src/main/resources/data/nusmp/advancement/**` and the lang fragment "
             "`tools/quests/lang/qst.lang.json`; `--check` fails if anything is out of date.",
             "- The lang fragment must be merged into `assets/nusmp/lang/en_us.json` (`python tools/quests/merge_lang.py`, idempotent). "
             "`AdvancementDataTest` fails while a key is missing.",
             "- `src/test/java/com/newuniverse/nusmp/quests/AdvancementDataTest.java` checks every file: parses, parent exists, one root, no cycle, "
             "translations exist, triggers are known, event keys are in the contract, mod items / entities / effects / dimensions exist, every "
             "criterion is used by a requirement.", "",
             "## The tree (%d advancements)" % len(S.advs), "", "| Branch (folder) | Advancements | What is in it |", "| --- | --- | --- |"]
    for k, v in by.items():
        lines.append("| `%s` | %d | %s |" % (k, len(v), desc.get(k, "")))
    frames = collections.Counter(a.frame for a in S.advs)
    lines += ["", "Frames: %d task, %d goal, %d challenge. Goals and challenges announce in chat, tasks do not. Every advancement grants experience "
              "(5 to 2000 points, more for challenges)." % (frames["task"], frames["goal"], frames["challenge"]), "",
              "The tab background is `textures/block/grimoire_altar_top.png` (an existing 16x16 texture of the mod; the advancement screen tiles it).", "",
              "## Triggers", "",
              "Vanilla triggers are used wherever the game can see the thing itself: `minecraft:tick` (the root), `inventory_changed` (carry a mod item), "
              "`changed_dimension` (the five mod dimensions), `player_killed_entity` (bosses, together with the event), `player_interacted_with_entity` "
              "(the Tower Wizard, a Spirit Lord), `consume_item` (cooked monster meat), `effects_changed` (mod effects), `minecraft:impossible` (the "
              "future-boss placeholders).", "",
              "Everything else is the mod's own trigger `nusmp:event` (implemented in `quests/NusmpEventTrigger`, fired by `NusmpEvents.fire`, see "
              "`docs/quests_java.md`):", "",
              "```json", '{"trigger": "nusmp:event", "conditions": {"key": "spell.cast", "count": 10, "arg": "thread"}}', "```", "",
              "`key` is required; `count` (default 1, written only when above 1) is compared with the per-player counter of the key (or of `key|arg` "
              "when `arg` is given) with `>=`; `arg` is an exact string match. Boss kills use an OR of the event and the vanilla kill, so they work "
              "whichever one the Java side delivers. The keys:", "",
              "| Key | Argument | Used by (advancements) | Counts used | Hook point suggested to the Java side |", "| --- | --- | --- | --- | --- |"]
    use = usage()
    ev = {e[0]: e for e in EVENTS}
    for key in sorted(use):
        args, counts, n = use[key]
        lines.append("| `%s` | %s | %d | %s | %s |" % (key, ev[key][1] if key in ev else "?", n, ", ".join(str(c) for c in sorted(counts)),
                                                       ev[key][2] if key in ev else "?"))
    lines += ["", "Keys of the original brief that no advancement uses: " + ", ".join("`%s`" % e[0] for e in EVENTS if e[0] not in use)
              + ", plus `convergence.stage` and `convergence.anomaly` (the Convergence system was removed in 1.14.0, so there is nothing to hook).",
              "", "## Add an advancement in three lines", "",
              "```python", "# tools/quests/spec_adv.py", 'add("dungeon/new_thing", "dungeon/living_dungeon", "Title", "What to do.", "nusmp:dungeon_maw", ev("dungeon.maw", 3), xp=20)',
              "```", "then `python tools/quests/gen_advancements.py` and `python tools/quests/merge_lang.py`.", "",
              "## The future-boss placeholders", "",
              "`nusmp:future/<arc>_<boss>` (hidden, criterion `impossible`). They are awarded by `NusmpEvents.futureBoss(player, arc, boss)`. "
              "The table is in `docs/quests.md`.", "",
              "## Unverified in the game", "",
              "- No advancement was loaded by Minecraft. The JSON follows the 1.21.1 format (`display.icon.id`, `background` as a texture path, "
              "`ContextAwarePredicate` lists for entity conditions) but a typo in a vanilla condition would only show as a log error at load time.",
              "- The custom trigger codec belongs to the Java side; the conditions above (`key`, `count`, `arg`, no `player`) are the contract.",
              "- Icons are checked against the mod's lang and item models, not against the registry.", ""]
    path = os.path.join(DOCS, "advancements.md")
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(lines))


ARC_ROWS = [
    # chapter, arc title, live bosses (advancement), placeholders (advancement)
    ("arc01_hage", "1 Hage Village", [], []),
    ("arc02_exam", "2 Magic Knights Entrance Exam", [], []),
    ("arc03_dungeon", "3 The Dungeon Arc", [("Goliath", "bosses/goliath")], [("dungeon_arc", "vetto", "Vetto")]),
    ("arc04_triad", "4 Dark Triad: Royal Capital", [], [("dark_triad", "boss", "the Dark Triad arc's boss")]),
    ("arc05_eye", "5 Eye of the Midnight Sun", [], [("eye_midnight", "boss", "the Eye of the Midnight Sun arc's boss")]),
    ("arc06_witches", "6 The Witches' Forest", [], [("witches_forest", "boss", "the Witch Queen")]),
    ("arc07_temple", "7 The Underwater Temple", [("Spawn of Charybdis", "bosses/charybdis_spawn")],
     [("underwater_temple", "boss", "the temple's guardian")]),
    ("arc08_selection", "8 Royal Knights Selection Exam", [("Floor Warden", "bosses/floor_warden")],
     [("royal_knights", "boss", "the examiner")]),
    ("arc09_spade_raid", "9 Spade Kingdom Raid", [("Dante", "bosses/dante_zogratis"), ("Vanica", "bosses/vanica_zogratis"),
                                                 ("Zenon", "bosses/zenon_zogratis")], []),
    ("arc10_heart", "10 Heart Kingdom: Elf Reincarnation", [("Wind Spirit Lord (optional)", "bosses/wind_spirit_lord")],
     [("heart_elf", "boss", "the reincarnated elf")]),
    ("arc11_diamond", "11 The Diamond Kingdom", [], [("diamond_kingdom", "boss", "the Diamond Kingdom's champion")]),
    ("arc12_final", "12 Spade Kingdom Invasion and the Final Arc", [("Lucius", "bosses/lucius_zogratis")], []),
    ("arc13_devil", "13 The Devil Arc", [("Zagred", "bosses/zagred"), ("Devil Avatar", "bosses/devil_avatar")],
     [("devil_arc", "megicula", "Megicula"), ("devil_arc", "lucifero", "Lucifero"), ("devil_arc", "astaroth", "Astaroth")]),
]


def quests_doc():
    reg = spec_quests.build_registry()
    by_group = collections.OrderedDict()
    for c in reg.chapters:
        by_group.setdefault(c.group, []).append(c)
    gtitle = {g.key: g.title for g in reg.groups}
    n_q = sum(len(c.quests) for c in reg.chapters)
    kinds = collections.Counter(q.kind for c in reg.chapters for q in c.quests)
    L_ = ["# The questline: FTB Quests book \"Multiverse of Anime\" (1.49.0)", "",
          "Why: the owner asked for \"a fully fleshed out questline with FTB Quests and Teams, with all the main anime arcs as quests when the "
          "bosses for them are made\". This is the data half (the pack and its generators); the Java half (the advancement trigger, the fire "
          "points, the installer command, Teams credit) is in `docs/quests_java.md`.", "",
          "**New**, not a replacement: the mod had no quest pack before (the Convergence chapter of 0.39 was written by code and removed in 1.14.0). "
          "Nothing was run in Minecraft. See \"Unverified in the game\".", "",
          "## What is in the book", "",
          "%d chapters in %d groups, %d quests (%s), %d reward tables. Nearly every task is an `advancement` task on the advancement tree "
          "(`docs/advancements.md`), plus `item` (obtain, squad treasuries), `kill` (hunters), `dimension` and `checkmark` tasks."
          % (len(reg.chapters), len(reg.groups), n_q, ", ".join("%d %s" % (v, k) for k, v in sorted(kinds.items())), len(reg.tables)), "",
          "| Group | Chapter (file) | Title | Quests | Optional | Boss-kind quests |", "| --- | --- | --- | --- | --- | --- |"]
    for g, cs in by_group.items():
        for c in cs:
            L_.append("| %s | `%s` | %s | %d | %d | %d |" % (gtitle[g], c.key, c.title, len(c.quests), sum(1 for q in c.quests if q.optional),
                                                          sum(1 for q in c.quests if q.kind in ("boss", "soon"))))
    L_ += ["", "Quest kinds (shape and default reward): `intro` (rounded square, the welcome node), `step` (circle), `mile` (diamond, journey cache), `boss` "
           "(octagon, boss trophy), `final` (hexagon, a pick-one reward), `side` (square, optional), `soon` (octagon with a barrier icon: a locked "
           "placeholder). Arcs are visible and ordered but not hidden: chapter 2, 3 and 13 wait for the previous arc's final quest, the others wait "
           "only for the previous arc's (free) welcome node. `hide_until_deps_complete` is off everywhere.", "",
           "## Arc to boss table", "",
           "Existing bosses award their quest through `boss.defeated` (arg = entity path); a placeholder quest sits on a hidden advancement "
           "`nusmp:future/<arc>_<boss>` that nothing awards today. **To make an arc's boss real**: add the boss, then call "
           "`NusmpEvents.futureBoss(serverPlayer, \"<arc>\", \"<boss>\")` for the killer (and, with Teams credit on, the squad) in the boss' death hook "
           "(`BossHooks` style); the quest completes and the hidden advancement appears. Nothing in the pack changes.", "",
           "| Arc chapter | Live boss quests (advancement) | Placeholder (advancement id) | Placeholder arguments for `futureBoss` | What to hook when the boss is made |",
           "| --- | --- | --- | --- | --- |"]
    for ch, title, live, soon in ARC_ROWS:
        liv = "; ".join("%s (`nusmp:%s`)" % (n, a) for n, a in live) or "none (no boss in this arc)"
        if soon:
            sid = "; ".join("`nusmp:future/%s_%s`" % (a, b) for a, b, _ in soon)
            args = "; ".join("`\"%s\", \"%s\"`" % (a, b) for a, b, _ in soon)
            hook = "; ".join("death hook of %s" % w for _, _, w in soon)
        else:
            sid, args, hook = "none", "-", ("nothing: the arc has all the bosses it needs" if live else "no boss in this arc")
        L_.append("| `%s` %s | %s | %s | %s | %s |" % (ch, title, liv, sid, args, hook))
    L_ += ["", "The same placeholders as a flat list: " + ", ".join("`nusmp:future/%s_%s`" % (a, b) for a, b, *_ in
                                                                   [(x, y) for x, y, _, _ in spec_adv.FUTURE]) + ".", "",
           "## Teams: how progress is shared, and how it relates to Squads", "",
           "- FTB Quests keeps **all progress per FTB team**, never per player. A player always belongs to a team (their own one-player party "
           "team until they join another). A task an individual completes (an advancement task is checked on each online member's tick) "
           "completes for the whole team. Item tasks with `consume_items` (the squad treasury, armoury and library) are filled by any member and "
           "belong to the team. Rewards are claimed per member by default; the quests marked as team rewards (`team_reward: true`) are claimed once "
           "for the whole team.",
           "- A **Magic Knight squad is one FTB Teams party team** (`multiverse/Squads`). So a squad's quest progress *is* the squad's: founding a "
           "squad or joining one changes which team's progress you see, and FTB Quests handles it (read from `ServerQuestFile.teamCreated` / "
           "`playerChangedTeam`): the founder's own progress is **copied** into the new party team; a player who joins a party has their progress "
           "**merged** into the party's; a player who leaves gets their old one-player progress back, and rewards already claimed stay claimed.",
           "- The \"Squads and Kingdoms\" chapter teaches this: found / join / captain / vice (advancements `squad.*` fired by the Squads class), a "
           "team treasury and armoury and library (consumed items), three **squad hunt** quests whose tasks are bosses that different members "
           "can kill, and honour-system standings quests (a checkmark: the book cannot read the standings).",
           "- With `quests.shareSquadCredit` (Java side) a boss kill or an arena victory is also fired for every online team member within 128 "
           "blocks, so the squad's advancements follow the squad, not only the killer.", "",
           "## Event keys: the contract with the Java side", "",
           "The advancement trigger `nusmp:event` takes `key`, optional `count` (>= 1, compared with the per-player counter of the key, or of `key|arg` "
           "when `arg` is set) and optional `arg`. The keys the advancement tree uses, with the meaning of `arg` and the place to fire them from "
           "(the full agreement, including what the Java side really sends, is `docs/quests_java.md` and the shared note QST_TRG.md):", "",
           "| Key | Argument | Fired from |", "| --- | --- | --- |"] + ["| `%s` | %s | %s |" % e for e in EVENTS] + [
           "", "Keys marked *extra* are not in the original list: `spell.distinct` (counter of distinct magics cast), `grimoire.kingdom`, "
           "`arena.victory` with arg `norelic`, and the `boss.phase` argument form `<boss>:<stage>`. Until the Java side fires them the advancements that need "
           "them (the `magic/distinct_*` ladder and `challenges/all_magic`, the four kingdom advancements, `challenges/norelic`) cannot be earned; "
           "every other advancement and quest works without them.", "",
           "## Add a quest in three lines", "",
           "```python", "# tools/quests/spec_q_journey.py (or the arc / boss module)",
           'c.q("my_key", "Title", (col, row), "nusmp:magic_stone", "step", "Subtitle", "One paragraph.\\nAnother.",',
           '    deps=["earlier_key", "other_chapter/quest_key"], tasks=[T.adv("start/first_spell")], rewards=[R.xp(20)])',
           "```", "then `python tools/quests/gen_quests.py && python tools/quests/validate_snbt.py`. An id is the hash of `quest/<chapter>/<key>`: "
           "never rename a key you have shipped (add a new quest instead).", "",
           "## How to regenerate and check", "",
           "```", "python tools/quests/gen_advancements.py      # advancements + lang fragment", "python tools/quests/merge_lang.py            # fragment -> en_us.json",
           "python tools/quests/gen_quests.py             # the pack", "python tools/quests/validate_snbt.py          # parse + check every key against the FTB source",
           "python tools/quests/gen_docs.py               # these two documents",
           "python tools/quests/gen_quests.py --check    # CI: nothing out of date", "```", "",
           "`validate_snbt.py --src <FTB Quests checkout>` re-derives `ftbq_keys.json` (the keys each object's `readData` reads) from the FTB "
           "Quests source; the committed copy lets the check run without the checkout.", "",
           "## How the installer works (Java side, summary)", "",
           "The pack is bundled in the jar under `/quest_pack/` (FTB's files directly at its top level; `README.txt` is documentation and is not copied). "
           "`/nusmp quests install|status|update|uninstall` (operators) copies the files into `config/ftbquests/quests/`, writes a marker "
           "`.nusmp_pack` (pack version, hash, file list), and tells you to `/ftbquests reload`. Details and safety rules: `docs/quests_java.md`.", "",
           "## The format, derived from the FTB Quests source",
           "",
           "- `data.snbt` (`version: 13`), `chapter_groups.snbt`, `chapters/<name>.snbt` (the file name must equal the `filename` key and be lower case), "
           "`reward_tables/<name>.snbt`, `lang/en_us.snbt`.",
           "- In 1.21 titles, subtitles and descriptions live **only in the lang file**, under the keys `<object type>.<16 hex id>.<title | quest_subtitle | "
           "quest_desc | chapter_subtitle>` (`quest`, `chapter`, `chapter_group`, `reward_table`, `task`, `reward`, and `file.0000000000000001.title` "
           "for the book). The loader still accepts the old inline keys but converts them and marks the book dirty, so the pack does not use them.",
           "- Ids are 16 upper-case hex digits in a string; the top bit is kept clear because FTB parses them with `Long.parseLong(hex, 16)`. A reward "
           "table is referenced from a `loot` / `choice` reward as `table_id: <decimal long>L` (a *long* tag: it is read with `getLong`).",
           "- `x`, `y` and `size` must be double tags (`1.5d`), `count` / `value` any number. Quest `icon` is `{id: \"item:id\"}` (a compound; a bare "
           "string is ignored for icons). Descriptions are lists of lines, `\"\"` is a blank line; `&` + a colour or style code works, `{` starts a "
           "text tag and is avoided; the generator rejects non-ASCII text (the SNBT reader only knows a few escapes).",
           "- Shapes used: `circle rsquare diamond octagon hexagon square` (all exist in FTB Quests' `textures/shapes`).", "",
           "## Validation result", "", "```", validator_summary(), "```", "",
           "`validate_snbt.py` also ran with deliberately broken packs (unknown key, a missing dependency, an int `x`, a missing advancement) and "
           "reported each of them; `QuestPackTest` fails on the same mutations.", "",
           "`QuestPackTest` (CI) asserts: unique 16-hex ids that fit a signed long, stable golden ids, chapter file names, "
           "dependencies exist and are acyclic, reward tables and groups exist, `table_id` is a long tag, every advancement task has its JSON file, "
           "every future-boss advancement has a quest, lang keys point at the right kind of object, ASCII-safe text.", "",
           "## Unverified in the game", "",
           "- **FTB Quests never loaded this pack.** The format was derived by reading the FTB Quests 2101.1.x and FTB Library sources and checked with a "
           "re-implementation of their SNBT parser; a mistake the source reading missed would show as a log line at load or a missing field.",
           "- Layouts (x/y) were laid out by hand on a grid and checked for overlaps only; whether the lines read well on screen is untested.",
           "- Advancement tasks are detected on a 5-tick auto-submit per online member; a quest whose advancement was earned before the quest unlocked "
           "completes the moment it unlocks.",
           "- Team behaviour (shared progress, `team_reward`) is as the FTB Teams / FTB Quests source describes it; not tried with two players.",
           "- The reward items are vanilla and a few mod consumables; no relic is ever a reward (relics are unique and exclusive to the vaults).", ""]
    path = os.path.join(DOCS, "quests.md")
    open(path, "w", encoding="utf-8", newline="\n").write("\n".join(L_))


if __name__ == "__main__":
    adv_doc()
    quests_doc()
    print("wrote docs/advancements.md and docs/quests.md")

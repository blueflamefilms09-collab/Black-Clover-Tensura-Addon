#!/usr/bin/env python3
"""Generate the FTB Quests pack (src/main/resources/quest_pack/) from tools/quests/spec_quests.py.

Why: the pack is generated so that (a) every id is a hash of a stable key and therefore never changes between versions (quest progress
survives updates), (b) advancement tasks can be checked against the advancement spec before they ever reach a player, and (c) a quest is
one line of spec instead of a hand edited SNBT file.

Format (derived from the FTB Quests 2101.1.x source, see docs/quests.md): data.snbt, chapter_groups.snbt, chapters/*.snbt, reward_tables/*.snbt,
lang/en_us.snbt (titles, subtitles and descriptions live ONLY in the lang file in 1.21), plus README.txt (documentation, not copied by the installer).

Usage:  python tools/quests/gen_quests.py [--check]
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spec_lib as L  # noqa: E402
from spec_lib import D, L as LongT, hid, hid_long, snbt  # noqa: E402,F401
import spec_adv  # noqa: E402
import spec_quests  # noqa: E402

PACK = os.path.join(L.RES, "quest_pack")
PACK_VERSION = "1.49.0"
FILE_ID = "0000000000000001"      # BaseQuestFile id (QuestFile is object 1): lang keys of the book itself use it
ALLOWED_VANILLA_ADV = {"minecraft:story/root"}


# ----------------------------------------------------------------------------------------------------- text checks
_BAD_AMP = re.compile(r"&(?![0-9a-fk-or])")


def check_text(where, s, errors):
    if any(ord(c) > 126 or (ord(c) < 32) for c in s):
        errors.append("%s: non ASCII or control character in %r" % (where, s[:40]))
    if _BAD_AMP.search(s):
        errors.append("%s: '&' not followed by a colour code in %r" % (where, s[:60]))
    if "{" in s.replace("{@pagebreak}", ""):
        errors.append("%s: '{' starts a FTB text tag in %r" % (where, s[:60]))


def tfmt(task, qkey, errors, adv_paths, known):
    kw = task.kw
    if task.type == "advancement":
        adv = kw["advancement"]
        if adv.startswith("nusmp:"):
            if adv[6:] not in adv_paths:
                errors.append("%s: advancement %s is not in the advancement spec" % (qkey, adv))
        elif adv not in ALLOWED_VANILLA_ADV:
            errors.append("%s: advancement %s unknown" % (qkey, adv))
    if task.type == "item" and not item_ok(kw["item"], known):
        errors.append("%s: item %s unknown" % (qkey, kw["item"]))
    if task.type == "kill":
        ns, p = kw["entity"].split(":")
        if ns == "nusmp" and p not in KNOWN_ENTITIES:
            errors.append("%s: entity %s unknown" % (qkey, kw["entity"]))
    if task.type == "dimension" and kw["dimension"] not in KNOWN_DIMS:
        errors.append("%s: dimension %s unknown" % (qkey, kw["dimension"]))


KNOWN_ENTITIES = {"dante_zogratis", "vanica_zogratis", "zenon_zogratis", "lucius_zogratis", "dark_disciple", "blood_beast",
                  "zogratis_clone", "devil_avatar", "zagred", "grimoire_daemon", "goliath", "charybdis_spawn", "magicule_scale",
                  "floor_warden", "god_of_order", "wind_spirit_lord", "elemental_spirit_lord", "spirit_lord", "dungeon_slime",
                  "living_armor", "dungeon_direwolf", "basilisk", "tower_wizard", "mirror_double", "painted_construct"}
KNOWN_DIMS = {"nusmp:labyrinth", "nusmp:living_dungeon", "nusmp:mega_dungeon", "nusmp:sanctuary", "nusmp:zogratis_arena", "nusmp:dream", "nusmp:grimoire_library",
              "minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"}


def item_ok(item, known):
    return L.check_icon(item, known)


# ----------------------------------------------------------------------------------------------------- builders
def icon_tag(item):
    return {"id": item}


def build(reg):
    errors = []
    known = L.known_items()
    adv_paths = set(spec_adv.S.by_path)
    lang = {}
    files = {}

    # ---- ids
    ids = {}

    def reg_id(kind, key):
        h = hid("%s/%s" % (kind, key))
        if h in ids:
            errors.append("id collision: %s %s vs %s" % (kind, key, ids[h]))
        ids[h] = "%s %s" % (kind, key)
        return h

    chap_by_key = {c.key: c for c in reg.chapters}
    quests = {}
    for c in reg.chapters:
        for q in c.quests:
            quests[q.gkey] = q
    qid = {k: reg_id("quest", k) for k in quests}
    tables = {t.key: t for t in reg.tables}
    tid = {k: reg_id("table", k) for k in tables}
    gid = {g.key: reg_id("group", g.key) for g in reg.groups}
    cid = {c.key: reg_id("chapter", c.key) for c in reg.chapters}

    def resolve(chapter, dep):
        key = dep if "/" in dep else "%s/%s" % (chapter.key, dep)
        if key not in quests:
            errors.append("%s: dependency %s does not exist" % (chapter.key, dep))
            return None
        return key

    # ---- file title and groups
    lang["file.%s.title" % FILE_ID] = reg.book_title
    group_list = []
    for g in reg.groups:
        group_list.append({"id": gid[g.key], "icon": icon_tag(g.icon)} if g.icon else {"id": gid[g.key]})
        lang["chapter_group.%s.title" % gid[g.key]] = g.title

    # ---- reward tables
    for t in reg.tables:
        entries = []
        for i, (item, count, weight) in enumerate(t.entries):
            if not item_ok(item, known):
                errors.append("table %s: item %s unknown" % (t.key, item))
            e = {"id": reg_id("tablereward", "%s/%d" % (t.key, i)), "item": icon_tag(item)}
            if count > 1:
                e["count"] = count
            if weight != 1:
                e["weight"] = float(weight)
            entries.append(e)
        d = {"id": tid[t.key], "order_index": reg.tables.index(t)}
        if t.empty_weight:
            d["empty_weight"] = float(t.empty_weight)
        d["loot_size"] = t.loot_size
        d["rewards"] = entries
        files["reward_tables/%s.snbt" % t.key] = snbt(d)
        lang["reward_table.%s.title" % tid[t.key]] = t.title

    # ---- chapters
    seen_deps = {}
    for ci, c in enumerate(reg.chapters):
        if c.group not in gid:
            errors.append("chapter %s: unknown group %s" % (c.key, c.group))
        qlist = []
        for q in c.quests:
            where = q.gkey
            d = {"id": qid[where], "x": D(q.at[0] * 1.5), "y": D(q.at[1] * 1.5)}
            if q.shape and q.shape != "circle":
                d["shape"] = q.shape
            if q.size and q.size != 1.0:
                d["size"] = D(q.size)
            if not L.check_icon(q.icon, known):
                errors.append("%s: icon %s unknown" % (where, q.icon))
            d["icon"] = icon_tag(q.icon)
            deps = []
            for dep in q.deps:
                k = resolve(c, dep)
                if k:
                    if k == where:
                        errors.append("%s depends on itself" % where)
                    deps.append(qid[k])
            seen_deps[where] = [resolve(c, dep) for dep in q.deps]
            if deps:
                d["dependencies"] = deps
                if q.dep_any:
                    d["dependency_requirement"] = "one_completed"
            if q.optional:
                d["optional"] = True
            if q.extra.get("hide_until_deps_complete"):
                d["hide_until_deps_complete"] = True
            tasks = []
            for ti, t in enumerate(q.tasks):
                tfmt(t, where, errors, adv_paths, known)
                td = {"id": reg_id("task", "%s/%d" % (where, ti)), "type": t.type}
                kw = t.kw
                if t.type == "advancement":
                    td["advancement"] = kw["advancement"]
                    td["criterion"] = kw.get("criterion", "")
                elif t.type == "item":
                    td["item"] = icon_tag(kw["item"])
                    if kw["count"] > 1:
                        td["count"] = LongT(kw["count"])
                    td["consume_items"] = bool(kw["consume"])
                elif t.type == "kill":
                    td["entity"] = kw["entity"]
                    td["value"] = LongT(kw["value"])
                elif t.type == "dimension":
                    td["dimension"] = kw["dimension"]
                elif t.type == "xp":
                    td["value"] = LongT(kw["value"])
                    td["points"] = bool(kw["points"])
                if t.type == "checkmark" and kw.get("title"):
                    lang["task.%s.title" % td["id"]] = kw["title"]
                tasks.append(td)
            if not tasks:
                errors.append("%s has no task" % where)
            d["tasks"] = tasks
            rewards = []
            for ri, r in enumerate(q.rewards):
                rd = {"id": reg_id("reward", "%s/%d" % (where, ri)), "type": r.type}
                kw = r.kw
                if r.type == "xp":
                    rd["xp"] = kw["xp"]
                    if kw.get("auto"):
                        rd["auto"] = "enabled"
                elif r.type == "xp_levels":
                    rd["xp_levels"] = kw["xp_levels"]
                elif r.type == "item":
                    if not item_ok(kw["item"], known):
                        errors.append("%s: reward item %s unknown" % (where, kw["item"]))
                    rd["item"] = icon_tag(kw["item"])
                    if kw["count"] > 1:
                        rd["count"] = kw["count"]
                    if kw.get("team"):
                        rd["team_reward"] = True
                elif r.type in ("loot", "choice"):
                    if kw["table"] not in tables:
                        errors.append("%s: reward table %s unknown" % (where, kw["table"]))
                    else:
                        rd["table_id"] = LongT(hid_long("table/" + kw["table"]))
                        if r.type == "choice" or kw.get("team"):
                            pass
                elif r.type == "toast":
                    rd["description"] = kw["description"]
                    check_text(where + " toast", kw["description"], errors)
                rewards.append(rd)
            if rewards:
                d["rewards"] = rewards
            qlist.append(d)
            # lang
            check_text(where + " title", q.title, errors)
            lang["quest.%s.title" % d["id"]] = q.title
            if q.sub:
                check_text(where + " subtitle", q.sub, errors)
                lang["quest.%s.quest_subtitle" % d["id"]] = q.sub
            if q.desc:
                for line in q.desc:
                    check_text(where + " desc", line, errors)
                lang["quest.%s.quest_desc" % d["id"]] = list(q.desc)
        cd = {"id": cid[c.key], "group": gid.get(c.group, ""), "order_index": ci, "filename": c.key}
        if not L.check_icon(c.icon, known):
            errors.append("chapter %s: icon %s unknown" % (c.key, c.icon))
        cd["icon"] = icon_tag(c.icon)
        cd["default_hide_dependency_lines"] = False
        cd["quests"] = qlist
        cd["quest_links"] = []
        cd["images"] = []
        files["chapters/%s.snbt" % c.key] = snbt(cd)
        check_text("chapter " + c.key, c.title, errors)
        lang["chapter.%s.title" % cid[c.key]] = c.title
        if c.sub:
            for line in c.sub:
                check_text("chapter " + c.key + " sub", line, errors)
            lang["chapter.%s.chapter_subtitle" % cid[c.key]] = list(c.sub)

    # layout: two quests of a chapter must not sit on top of each other (less than one cell apart in both directions)
    for c in reg.chapters:
        for i, a in enumerate(c.quests):
            for b in c.quests[i + 1:]:
                if abs(a.at[0] - b.at[0]) < 1 and abs(a.at[1] - b.at[1]) < 1:
                    errors.append("layout: %s/%s and %s/%s overlap at %s / %s" % (c.key, a.key, c.key, b.key, a.at, b.at))

    # cycles
    color = {}

    def visit(k, stack):
        if color.get(k) == 2:
            return
        if color.get(k) == 1:
            errors.append("dependency cycle through " + " > ".join(stack + [k]))
            return
        color[k] = 1
        for dk in seen_deps.get(k, []):
            if dk:
                visit(dk, stack + [k])
        color[k] = 2

    for k in quests:
        visit(k, [])

    # ---- data.snbt and groups
    files["chapter_groups.snbt"] = snbt({"chapter_groups": group_list})
    data = {
        "version": 13,
        "default_reward_team": False,
        "default_consume_items": False,
        "default_autoclaim_rewards": "disabled",
        "default_quest_shape": "circle",
        "default_quest_disable_jei": False,
        "emergency_items_cooldown": 300,
        "drop_loot_crates": False,
        "grid_scale": D(0.5),
        "pause_game": False,
        "lock_message": "",
        "progression_mode": "flexible",
        "detection_delay": 20,
        "show_lock_icons": True,
        "drop_book_on_death": False,
        "hide_excluded_quests": False,
        "fallback_locale": "en_us",
        "verify_on_load": False,
    }
    files["data.snbt"] = snbt(data)

    # ---- lang (sorted for stable output)
    lang_sorted = {k: lang[k] for k in sorted(lang)}
    files["lang/en_us.snbt"] = snbt(lang_sorted)

    # ---- readme + manifest
    n_quests = len(quests)
    files["README.txt"] = readme(reg, n_quests)
    return files, errors, {"chapters": len(reg.chapters), "quests": n_quests, "groups": len(reg.groups), "tables": len(reg.tables),
                           "ids": len(ids)}


def readme(reg, n_quests):
    return (
        "Multiverse of Anime quest pack %s\n"
        "================================\n\n"
        "This folder is generated by tools/quests/gen_quests.py from tools/quests/spec_quests.py. Do not edit it by hand: edit the spec and\n"
        "regenerate. Every id is a hash of a stable key, so quest progress survives updates.\n\n"
        "What it is: the FTB Quests book for the mod (%d chapters, %d quests, %d chapter groups, %d reward tables).\n"
        "Install: run  /nusmp quests install  as an operator (it copies this pack into config/ftbquests/quests) and then  /ftbquests reload.\n"
        "Update:  /nusmp quests update   Remove: /nusmp quests uninstall   Status: /nusmp quests status.\n"
        "The files: data.snbt, chapter_groups.snbt, chapters/*.snbt, reward_tables/*.snbt, lang/en_us.snbt. This README is documentation and is\n"
        "not copied into the quest folder.\n"
        % (PACK_VERSION, len(reg.chapters), n_quests, len(reg.groups), len(reg.tables))
    )


def main(argv):
    reg = spec_quests.build_registry()
    files, errors, stats = build(reg)
    if errors:
        print("\n".join(errors[:80]))
        print("%d errors" % len(errors))
        return 1
    check = "--check" in argv
    diff = []
    for rel, text in files.items():
        path = os.path.join(PACK, *rel.split("/"))
        cur = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if cur != text:
            diff.append(rel)
    stale = []
    for root, dirs, fs in os.walk(PACK):
        for f in fs:
            rel = os.path.relpath(os.path.join(root, f), PACK).replace(os.sep, "/")
            if rel not in files:
                stale.append(rel)
    if check:
        for r in diff:
            print("out of date:", r)
        for r in stale:
            print("stale:", r)
        print("quest pack: %s; out of date %d, stale %d" % (stats, len(diff), len(stale)))
        return 1 if diff or stale else 0
    for rel, text in files.items():
        path = os.path.join(PACK, *rel.split("/"))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    for rel in stale:                       # only files a previous run of this generator wrote
        os.remove(os.path.join(PACK, *rel.split("/")))
    print("wrote quest pack %s: %s" % (PACK_VERSION, stats))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

#!/usr/bin/env python3
"""Generate the advancement JSON files and the lang fragment from tools/quests/spec_adv.py.

Why: one spec is the single source of truth for advancement ids, titles, parents, icons and criteria; the quest pack points its advancement
tasks at the same ids, so a rename in the spec is caught by gen_quests.py instead of silently breaking a quest.

Usage:
    python tools/quests/gen_advancements.py            write the files
    python tools/quests/gen_advancements.py --check    exit 1 if a file on disk differs from the spec (CI / pre-commit)
    python tools/quests/gen_advancements.py --lang-copy PATH   also copy the lang fragment to PATH
The 25 files under data/nusmp/advancement/convergence are never touched.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spec_lib as L  # noqa: E402
import spec_adv  # noqa: E402

OUT = os.path.join(L.RES, "data", "nusmp", "advancement")
LANG_OUT = os.path.join(L.HERE, "lang", "qst.lang.json")
OWNED_DIRS = {"start", "magic", "spells", "arcs", "bosses", "relics", "weapons", "dungeon", "spirits", "squads", "world", "challenges",
              "future", "arc"}
FRAMES = {"task", "goal", "challenge"}


def build_json(a):
    crit = a.crit
    criteria = {}
    for name, trig, cond in crit["list"]:
        criteria[name] = {"trigger": trig, "conditions": cond} if cond else {"trigger": trig}
    if crit["mode"] == "any":
        req = [[n for n, _, _ in crit["list"]]]
    elif crit["mode"] == "req":
        req = crit["req"]
    else:
        req = [[n] for n, _, _ in crit["list"]]
    disp = {
        "icon": {"id": a.icon},
        "title": {"translate": a.lang_base + ".title"},
        "description": {"translate": a.lang_base + ".desc"},
        "frame": a.frame,
        "show_toast": bool(a.toast),
        "announce_to_chat": bool(a.chat),
        "hidden": bool(a.hidden),
    }
    if a.background:
        disp["background"] = a.background
    d = {}
    if a.parent:
        d["parent"] = "%s:%s" % (L.MOD, a.parent)
    d["display"] = disp
    d["criteria"] = criteria
    d["requirements"] = req
    if a.xp:
        d["rewards"] = {"experience": int(a.xp)}
    return d


def validate(spec):
    errors = []
    known = L.known_items()
    paths = set(spec.by_path)
    roots = [a for a in spec.advs if a.parent is None]
    if len(roots) != 1 or roots[0].path != "root":
        errors.append("exactly one root named 'root' expected")
    for a in spec.advs:
        if a.parent is not None and a.parent not in paths:
            errors.append("%s: unknown parent %s" % (a.path, a.parent))
        if a.frame not in FRAMES:
            errors.append("%s: bad frame" % a.path)
        if not L.check_icon(a.icon, known):
            errors.append("%s: icon %s is not a known item" % (a.path, a.icon))
        if not a.crit["list"]:
            errors.append("%s: no criteria" % a.path)
        names = [n for n, _, _ in a.crit["list"]]
        if len(set(names)) != len(names):
            errors.append("%s: duplicate criterion names" % a.path)
        for n, t, cond in a.crit["list"]:
            for it in _items_in(cond):
                if not L.check_icon(it, known):
                    errors.append("%s: predicate item %s unknown" % (a.path, it))
    for a in spec.advs:                       # cycle check
        seen, cur = set(), a
        while cur.parent:
            if cur.path in seen:
                errors.append("cycle at " + a.path)
                break
            seen.add(cur.path)
            cur = spec.by_path.get(cur.parent)
            if cur is None:
                break
    return errors


def _items_in(cond):
    out = []
    if isinstance(cond, dict):
        for k, v in cond.items():
            if k == "items" and isinstance(v, str):
                out.append(v)
            else:
                out.extend(_items_in(v))
    elif isinstance(cond, list):
        for v in cond:
            out.extend(_items_in(v))
    return out


def render(a):
    return json.dumps(build_json(a), indent=2, ensure_ascii=True) + "\n"


def lang_fragment(spec):
    frag = {}
    for a in spec.advs:
        frag[a.lang_base + ".title"] = a.title
        frag[a.lang_base + ".desc"] = a.desc
    return frag


def main(argv):
    check = "--check" in argv
    lang_copy = argv[argv.index("--lang-copy") + 1] if "--lang-copy" in argv else None
    spec = spec_adv.S
    errors = validate(spec)
    if errors:
        print("\n".join(errors))
        return 1
    wanted = {}
    for a in spec.advs:
        wanted[os.path.join(OUT, *a.path.split("/")) + ".json"] = render(a)
    frag_text = json.dumps(lang_fragment(spec), indent=2, ensure_ascii=True) + "\n"
    stale, diff = [], []
    for root, dirs, files in os.walk(OUT):
        rel = os.path.relpath(root, OUT)
        top = rel.split(os.sep)[0]
        for f in files:
            full = os.path.join(root, f)
            owned = (rel == "." and f == "root.json") or top in OWNED_DIRS
            if owned and full not in wanted:
                stale.append(full)
    for path, text in wanted.items():
        cur = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if cur != text:
            diff.append(path)
    cur_lang = open(LANG_OUT, encoding="utf-8").read() if os.path.exists(LANG_OUT) else None
    if cur_lang != frag_text:
        diff.append(LANG_OUT)
    if check:
        for p in diff + stale:
            print("out of date:" if p in diff else "stale:", os.path.relpath(p, L.ROOT))
        print("advancements: %d, out of date: %d, stale: %d" % (len(wanted), len(diff), len(stale)))
        return 1 if (diff or stale) else 0
    for path, text in wanted.items():
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(text)
    os.makedirs(os.path.dirname(LANG_OUT), exist_ok=True)
    with open(LANG_OUT, "w", encoding="utf-8", newline="\n") as fh:
        fh.write(frag_text)
    for p in stale:                               # only files this generator owns and the spec no longer lists
        os.remove(p)
    if lang_copy:
        with open(lang_copy, "w", encoding="utf-8", newline="\n") as fh:
            fh.write(frag_text)
    by = {}
    for a in spec.advs:
        by[a.path.split("/")[0]] = by.get(a.path.split("/")[0], 0) + 1
    print("wrote %d advancements (%s)" % (len(wanted), ", ".join("%s %d" % kv for kv in by.items())))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

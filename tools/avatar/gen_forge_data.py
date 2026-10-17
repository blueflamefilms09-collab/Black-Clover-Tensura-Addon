#!/usr/bin/env python3
"""Why: writes the Soul Forge's shipped datapack files from the tables in forge_tables.py, after checking every id against its real source, so a typo
can never ship as a "skill that does not exist". Output (deterministic, rerun-safe):

    src/main/resources/data/nusmp/avatar_aptitudes/<id>.json   one per Magic Aptitude (format: AptitudeParser)
    src/main/resources/data/nusmp/avatar_skills/<file>.json    one per Unique / Ultimate skill (format: ForgeSkillParser)

    python3 -B tools/avatar/gen_forge_data.py            # validate and write
    python3 -B tools/avatar/gen_forge_data.py --check    # validate and compare with what is in the repo (exit 1 on any difference)

Checks: aptitude ids are snake_case and unique; rarity is a Rarity name; tax rises with rarity; `magic` is empty or a MagicType name (read from
MagicType.java); every `tensura:` id is in the owner's id reference; every `nusmp:` skill is registered in NUSkills.java; Anti-Magic and Time are
EPIC or LEGENDARY; at least 24 aptitudes; every skill file name is unique.
"""
import json
import os
import re
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import forge_tables as T  # noqa: E402

REPO = os.path.abspath(os.path.join(HERE, "..", ".."))
OUT_APT = os.path.join(REPO, "src", "main", "resources", "data", "nusmp", "avatar_aptitudes")
OUT_SKL = os.path.join(REPO, "src", "main", "resources", "data", "nusmp", "avatar_skills")
IDS = os.path.join(REPO, "src", "test", "resources", "com", "newuniverse", "nusmp", "charter", "tensura_skill_ids.txt")
MAGIC = os.path.join(REPO, "src", "main", "java", "com", "newuniverse", "nusmp", "blackclover", "MagicType.java")
NUSKILLS = os.path.join(REPO, "src", "main", "java", "com", "newuniverse", "nusmp", "skill", "NUSkills.java")
RARITY = ["COMMON", "UNCOMMON", "RARE", "EPIC", "LEGENDARY"]


def tensura_ids():
    with open(IDS, encoding="utf-8") as f:
        return {l.strip() for l in f if l.strip() and not l.startswith("#")}


def magic_names():
    src = open(MAGIC, encoding="utf-8").read()
    return {m.lower() for m in re.findall(r"^\s{4}([A-Z][A-Z_]*)\(\"", src, re.M)}


def nusmp_skills():
    src = open(NUSKILLS, encoding="utf-8").read()
    return set(re.findall(r'SKILLS\.register\("([a-z_]+)"', src))


def hexcol(c):
    return "#%06X" % c


def validate():
    bad = []
    tid, mag, nus = tensura_ids(), magic_names(), nusmp_skills()
    seen = set()
    last_tax = {r: [] for r in RARITY}
    for (i, name, rarity, tax, magic, skills, tint, glyph, desc) in T.APTITUDES:
        if not re.fullmatch(r"[a-z][a-z0-9_]*", i): bad.append("bad aptitude id " + i)
        if i in seen: bad.append("duplicate aptitude " + i)
        seen.add(i)
        if rarity not in RARITY: bad.append(i + ": rarity " + rarity)
        else: last_tax[rarity].append(tax)
        if magic and magic not in mag: bad.append(i + ": magic '" + magic + "' is not a MagicType name")
        for s in skills:
            ns, _, p = s.partition(":")
            if ns != "tensura" or p not in tid: bad.append(i + ": skill " + s + " is not in the Tensura id reference")
        if not (0 <= tax <= 90): bad.append(i + ": tax")
        if len(desc) > 200: bad.append(i + ": description too long")
        if not skills: bad.append(i + ": no skills")
    if len(T.APTITUDES) < 24: bad.append("fewer than 24 aptitudes")
    # the tax ladder: every rarity's lowest tax is not below the previous rarity's highest
    prev_max = -1
    for r in RARITY:
        if not last_tax[r]: bad.append("no aptitude of rarity " + r); continue
        if min(last_tax[r]) < prev_max: bad.append("tax ladder broken at " + r)
        prev_max = max(last_tax[r])
    by = {a[0]: a for a in T.APTITUDES}
    for need in ("anti_magic", "time"):
        if by[need][2] not in ("EPIC", "LEGENDARY"): bad.append(need + " must be EPIC or LEGENDARY")
    files = set()
    ids = set()
    for (f, sid, kind, name, tax, min_ep, tint, desc) in T.SKILLS:
        if f in files: bad.append("duplicate skill file " + f)
        files.add(f)
        if sid in ids: bad.append("duplicate skill id " + sid)
        ids.add(sid)
        ns, _, p = sid.partition(":")
        if ns == "tensura" and p not in tid: bad.append(sid + " is not in the Tensura id reference")
        elif ns == "nusmp" and p not in nus: bad.append(sid + " is not registered in NUSkills")
        elif ns not in ("tensura", "nusmp"): bad.append(sid + ": unknown namespace")
        if kind not in ("unique", "ultimate"): bad.append(sid + ": kind")
        if ns == "tensura" and p in ("usurper", "anti_skill"): bad.append(sid + " steals or erases skills and is never offered")
    return bad


def apt_json(a):
    (i, name, rarity, tax, magic, skills, tint, glyph, desc) = a
    return {"name": name, "rarity": rarity, "tax_percent": tax, "magic": magic, "skills": skills, "tint": hexcol(tint), "description": desc}


def skl_json(s):
    (f, sid, kind, name, tax, min_ep, tint, desc) = s
    return {"id": sid, "kind": kind, "name": name, "ep_tax": tax, "min_ep": min_ep, "tint": hexcol(tint), "description": desc}


def text(obj):
    return json.dumps(obj, indent=2, ensure_ascii=False) + "\n"


def main():
    check = "--check" in sys.argv
    bad = validate()
    for b in bad: print("PROBLEM:", b)
    if bad:
        return 1
    want = {}
    for a in T.APTITUDES: want[os.path.join(OUT_APT, a[0] + ".json")] = text(apt_json(a))
    for s in T.SKILLS: want[os.path.join(OUT_SKL, s[0] + ".json")] = text(skl_json(s))
    diffs = 0
    for d in (OUT_APT, OUT_SKL):
        os.makedirs(d, exist_ok=True)
    if check:
        for path, body in want.items():
            if not os.path.isfile(path) or open(path, encoding="utf-8").read() != body:
                print("DIFFERS:", os.path.relpath(path, REPO)); diffs += 1
        for d in (OUT_APT, OUT_SKL):
            for fn in os.listdir(d):
                if os.path.join(d, fn) not in want:
                    print("EXTRA FILE:", os.path.relpath(os.path.join(d, fn), REPO)); diffs += 1
    else:
        for path, body in want.items():
            with open(path, "w", encoding="utf-8", newline="\n") as f: f.write(body)
    print("%d aptitudes, %d skills, %s" % (len(T.APTITUDES), len(T.SKILLS), "%d differences" % diffs if check else "written"))
    return 1 if diffs else 0


if __name__ == "__main__":
    sys.exit(main())

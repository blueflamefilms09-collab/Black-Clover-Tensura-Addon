#!/usr/bin/env python3
"""Writes src/main/resources/data/nusmp/relics/franchise_map.json: which franchise (anime / world) each item of the mod comes from.

The map drives the franchise engravings (franchise/FranchiseEngravings): an item with a franchise carries that franchise's engravings. Deterministic (sorted keys,
no clock, no random) and re-runnable after any wave is added or changed. An item whose origin is not clear gets NO franchise: it is simply not in the map.

Where the franchise of an item comes from, strongest first:
  1. tools/relics/franchise_overrides.json  ("exact": id -> franchise or null to force none; "prefixes": id prefix -> franchise; "dnd_waves": the wave numbers whose
     cards are Dungeons and Dragons magic items). The owner-checked manual layer.
  2. a roster that names the source of each weapon (tools/relics/new197_roster.py: src="Black Clover"), matched against the franchise aliases.
  3. the wave: the class comment of a generated wave says "inspired by <work>" (an alias match; several works in one wave are told apart by 4.); the cards of the
     waves listed in "dnd_waves" are D&D items unless 4. finds another franchise in their own text.
  4. (items that are no relic) the item's own text: its name, the .desc("...") in the wave source, and the lang keys item.nusmp.<id>[.desc|.card|.lore]; the franchise whose words (data/nusmp/
     franchise/engravings.json "words") occur most wins, a tie gives none.
  5. nothing: no franchise.
Consumables (relic slot CONSUMABLE) are never mapped (an engraving on something you drink means nothing). A pair's left half (<id>_left) follows its right half.
Missing text is tolerated everywhere: a def without a description, a wave without a header, an unreadable file only means fewer hits.

usage: python3 -B tools/relics/franchise_map.py [--check] [--report]
   --check    only compare with the file on disk (exit 1 when it would change)
   --report   print the per-franchise counts and the unmapped relic ids
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WAVES = ROOT / "src/main/java/com/newuniverse/nusmp/relics/wave"
LANG = ROOT / "src/main/resources/assets/nusmp/lang/en_us.json"
LANG_FRAGMENTS = ROOT / "tools/lang"
ENGRAVINGS = ROOT / "src/main/resources/data/nusmp/franchise/engravings.json"
OVERRIDES = Path(__file__).resolve().parent / "franchise_overrides.json"
ROSTERS = [ROOT / "tools/relics/new197_roster.py"]
WEAPON_ENGRAVINGS = ROOT / "src/main/java/com/newuniverse/nusmp/item/WeaponEngravings.java"
OUT = ROOT / "src/main/resources/data/nusmp/relics/franchise_map.json"

DEF = re.compile(r'RelicDef\s*\.\s*of\s*\(\s*([^,]+?)\s*,\s*"((?:[^"\\]|\\.)*)"\s*,\s*Grade\s*\.\s*(\w+)\s*,\s*RelicSlot\s*\.\s*(\w+)\s*\)', re.S)
CONST_DECL = re.compile(r'static\s+final\s+String\s+([^;]+);')
CONST_PAIR = re.compile(r'(\w+)\s*=\s*"([a-z0-9_]+)"')
DESC = re.compile(r'\.desc\(\s*((?:"(?:[^"\\]|\\.)*"\s*\+?\s*)+)\)', re.S)
STR = re.compile(r'"((?:[^"\\]|\\.)*)"')
WAVE_NO = re.compile(r'^Wave(\d+)')
HEADER = re.compile(r'/\*\*(.*?)\*/\s*public\s+final\s+class', re.S)


def read(p):
    try:
        return p.read_text(encoding="utf-8", errors="replace")
    except OSError:
        return ""


def load_json(p, default):
    try:
        return json.loads(read(p))
    except ValueError:
        return default


def franchises():
    data = load_json(ENGRAVINGS, {})
    return data.get("franchises", [])


def lang_table():
    table = dict(load_json(LANG, {}))
    if LANG_FRAGMENTS.is_dir():
        for f in sorted(LANG_FRAGMENTS.glob("*.json")):
            d = load_json(f, {})
            if isinstance(d, dict):
                table.update({k: v for k, v in d.items() if isinstance(v, str)})
    return table


def constants(sources):
    table = {}
    for cls, text in sources.items():
        for decl in CONST_DECL.findall(text):
            for name, value in CONST_PAIR.findall(decl):
                table[f"{cls}.{name}"] = value
                table.setdefault(name, value)
    return table


def parse_defs(sources):
    """-> {id: dict(slot, name, wave, desc)}; ids that cannot be resolved are skipped (reported)."""
    consts = constants(sources)
    found, unresolved = {}, []
    for cls, text in sources.items():
        matches = list(DEF.finditer(text))
        for i, m in enumerate(matches):
            arg = m.group(1).strip()
            if arg.startswith('"') and arg.endswith('"'):
                rid = arg[1:-1]
            else:
                rid = (consts.get(f"{cls}.{arg}") if "." not in arg else None) or consts.get(arg) or consts.get(arg.split(".")[-1])
            if rid is None or not re.fullmatch(r"[a-z0-9_]+", rid):
                unresolved.append(f"{cls}: {arg}")
                continue
            end = matches[i + 1].start() if i + 1 < len(matches) else len(text)
            chain = text[m.end(): min(end, m.end() + 6000)]
            desc = ""
            dm = DESC.search(chain)
            if dm:
                desc = " ".join(s.encode().decode("unicode_escape", "ignore") if "\\" in s else s for s in STR.findall(dm.group(1)))
            found[rid] = dict(slot=m.group(4), name=m.group(2), wave=cls, desc=desc)
    return found, unresolved


def wave_header(text):
    """The work(s) a generated wave says it is inspired by ("... relics inspired by Overlord. GENERATED by ..."), or ""."""
    m = HEADER.search(text)
    if not m:
        return ""
    h = re.sub(r"\s*\*\s*", " ", m.group(1))
    i = re.search(r"\binspired by (.+?)(?:\.\s|\s+GENERATED|$)", h)
    return i.group(1) if i else ""


def alias_hits(text, fr):
    """franchise ids whose alias occurs in text as a whole word (case-insensitive)."""
    t = text.lower()
    out = []
    for f in fr:
        if any(re.search(r"(?<![a-z0-9])" + re.escape(a) + r"(?![a-z0-9])", t) for a in f.get("aliases", [])):
            out.append(f["id"])
    return out


def word_hits(text, fr):
    """{franchise id: number of distinct words / aliases found}."""
    t = " " + re.sub(r"[^a-z0-9:&'\- ]+", " ", text.lower()) + " "
    scores = {}
    for f in fr:
        n = 0
        for w in set(f.get("words", [])):
            if re.search(r"(?<![a-z0-9])" + re.escape(w) + r"(?![a-z0-9])", t):
                n += 1
        if n:
            scores[f["id"]] = n
    return scores


def best(scores, among=None):
    items = [(n, k) for k, n in scores.items() if among is None or k in among]
    if not items:
        return None
    items.sort(reverse=True)
    if len(items) > 1 and items[0][0] == items[1][0]:
        return None                                                        # a tie tells nothing
    return items[0][1]


def roster_sources(fr):
    out = {}
    for p in ROSTERS:
        text = read(p)
        for m in re.finditer(r'dict\(id="([a-z0-9_]+)"', text):
            seg = text[m.end(): m.end() + 900]
            s = re.search(r'\bsrc="([^"]*)"', seg)
            if s:
                hits = alias_hits(s.group(1), fr)
                out[m.group(1)] = hits[0] if len(hits) == 1 else None
    return out


def main():
    fr = franchises()
    if not fr:
        print("warning: data/nusmp/franchise/engravings.json is missing; run tools/franchise/gen_engravings.py first")
    fids = {f["id"] for f in fr}
    ov = load_json(OVERRIDES, {})
    exact, prefixes = ov.get("exact", {}), ov.get("prefixes", {})
    lo, hi = (ov.get("dnd_waves") or [0, 0])[:2]
    sources = {}
    if WAVES.is_dir():
        for f in sorted(WAVES.glob("*.java")):
            sources[f.stem] = read(f)
    defs, unresolved = parse_defs(sources)
    lang = lang_table()
    roster = roster_sources(fr)

    wave_fr = {}                                                           # wave class -> (franchise ids named in its header)
    for cls, text in sources.items():
        if WAVE_NO.match(cls) and "RelicDef" in text:
            wave_fr[cls] = alias_hits(wave_header(text), fr)

    universe = set(defs)
    for k in lang:
        m = re.fullmatch(r"item\.nusmp\.([a-z0-9_]+)", k)
        if m:
            universe.add(m.group(1))
    for m in re.finditer(r'Map\.entry\("([a-z0-9_]+)"', read(WEAPON_ENGRAVINGS)):
        universe.add(m.group(1))
    universe.update(exact)

    items, why = {}, {}
    for rid in sorted(universe):
        d = defs.get(rid)
        if d and d["slot"] == "CONSUMABLE":
            continue
        text = " ".join([rid.replace("_", " "), d["name"] if d else "", d["desc"] if d else ""] +
                        [lang.get(f"item.nusmp.{rid}{s}", "") for s in ("", ".desc", ".card", ".lore")])
        choice, how = None, ""
        if rid in exact:
            choice, how = exact[rid], "override"
        elif rid in roster:
            choice, how = roster[rid], "roster"
        elif d:
            cls = d["wave"]
            named = wave_fr.get(cls, [])
            n = int(WAVE_NO.match(cls).group(1)) if WAVE_NO.match(cls) else -1
            if len(set(named)) == 1:
                choice, how = named[0], "wave header"
            elif len(set(named)) > 1:
                choice, how = best(word_hits(text, fr), set(named)), "wave header + words"
            elif lo <= n <= hi:
                choice, how = "dnd", "dnd wave"
        if choice is None and not how:
            for pre in sorted(prefixes, key=len, reverse=True):
                if rid.startswith(pre):
                    choice, how = prefixes[pre], "prefix"
                    break
        if choice is None and not how and d is None:                      # only an item that is no relic: a relic's origin is its wave
            pick = best(word_hits(text, fr))
            if pick:
                choice, how = pick, "words"
        if choice and choice in fids:
            items[rid] = choice
            why[rid] = how
    for rid in sorted(universe):                                           # a pair's left half follows its right half
        if rid.endswith("_left") and rid not in items and rid[:-5] in items:
            items[rid] = items[rid[:-5]]
    counts = {}
    for v in items.values():
        counts[v] = counts.get(v, 0) + 1
    out = {
        "version": 1,
        "comment": "Written by tools/relics/franchise_map.py (re-run it after a wave changes; manual choices are in tools/relics/franchise_overrides.json). An id that is not here has no franchise.",
        "franchises": dict(sorted(counts.items())),
        "prefixes": dict(sorted(prefixes.items())),
        "items": dict(sorted(items.items())),
    }
    text = json.dumps(out, indent=1, ensure_ascii=False) + "\n"
    if "--check" in sys.argv:
        same = read(OUT) == text
        print("franchise_map.json is", "up to date" if same else "OUT OF DATE")
        sys.exit(0 if same else 1)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(text, encoding="utf-8", newline="\n")
    print(f"{len(items)} items in {len(counts)} franchises -> {OUT.relative_to(ROOT)}")
    for u in unresolved:
        print("warning: could not resolve the id of a def:", u)
    if "--report" in sys.argv:
        for k, n in sorted(counts.items(), key=lambda x: -x[1]):
            print(f"  {n:4d}  {k}")
        none = sorted(r for r in defs if r not in items and defs[r]["slot"] != "CONSUMABLE")
        print(f"relics without a franchise ({len(none)}):", " ".join(none))
        if "--why" in sys.argv:
            for r in sorted(items):
                print(f"  {r:42s} {items[r]:22s} {why.get(r, 'pair')}")
        how = {}
        for r, h in why.items():
            how[h] = how.get(h, 0) + 1
        print("how:", how)


if __name__ == "__main__":
    main()

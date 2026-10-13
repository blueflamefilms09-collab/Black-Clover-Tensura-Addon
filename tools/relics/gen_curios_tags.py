#!/usr/bin/env python3
"""Writes the Curios item tags of the Relics of Many Worlds.

Every relic whose slot is NECK, BACK, BELT, HANDS or CHARM is worn in a Curios slot, and Curios decides what fits a slot by an item tag:
data/curios/tags/item/<slot>.json (necklace, back, belt, hands, charm). This script finds the curio-type defs and merges their ids into those files
(keeping every entry that is there, adding "required": false so a missing item can never break the tag). Run it again after any wave changes.

Where the defs come from, in this order:
  1. the wave sources: every  RelicDef.of(<id>, "<name>", Grade.X, RelicSlot.Y)  in src/main/java/com/newuniverse/nusmp/relics/wave/*.java, where <id> is
     a string literal or a constant ("Wave6Tune.ID_SKY") whose  String NAME = "value";  is found in the same folder;
  2. tools/relics/relic_slots.json (id -> slot), the list the cards were planned with, for anything the sources do not show.
A def with RelicDef.adopt(...) is an old item of the mod and is skipped (its tags exist since 1.15.0).

usage: python3 tools/relics/gen_curios_tags.py [--check]     (--check only reports what would change and exits 1 if something is missing)
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WAVES = ROOT / "src/main/java/com/newuniverse/nusmp/relics/wave"
TAGS = ROOT / "src/main/resources/data/curios/tags/item"
SLOT_TAG = {"NECK": "necklace", "BACK": "back", "BELT": "belt", "HANDS": "hands", "CHARM": "charm"}
MODID = "nusmp"

DEF = re.compile(r'RelicDef\s*\.\s*of\s*\(\s*([^,]+?)\s*,\s*"(?:[^"\\]|\\.)*"\s*,\s*Grade\s*\.\s*(\w+)\s*,\s*RelicSlot\s*\.\s*(\w+)\s*\)', re.S)
CONST_DECL = re.compile(r'static\s+final\s+String\s+([^;]+);')
CONST_PAIR = re.compile(r'(\w+)\s*=\s*"([a-z0-9_]+)"')


def read_waves():
    sources = {}
    if WAVES.is_dir():
        for f in sorted(WAVES.glob("*.java")):
            sources[f.stem] = f.read_text(encoding="utf-8")
    return sources


def constants(sources):
    table = {}
    for cls, text in sources.items():
        for decl in CONST_DECL.findall(text):                                      # one declaration may list several: String A = "a", B = "b";
            for name, value in CONST_PAIR.findall(decl):
                table[f"{cls}.{name}"] = value
                table.setdefault(name, value)
    return table


def parse_defs(sources):
    consts = constants(sources)
    found, unresolved = {}, []
    for cls, text in sources.items():
        for m in DEF.finditer(text):
            arg, _grade, slot = m.group(1).strip(), m.group(2), m.group(3)
            if arg.startswith('"') and arg.endswith('"'):
                rid = arg[1:-1]
            else:
                rid = consts.get(arg) or consts.get(arg.split(".")[-1])
            if rid is None:
                unresolved.append(f"{cls}: {arg}")
                continue
            tail = text[m.end(): m.end() + 1500]
            chain = tail.split(");", 1)[0]
            if ".adopt(" in chain:
                continue
            found[rid] = slot
    return found, unresolved


def load_tag(path):
    if path.exists():
        return json.loads(path.read_text(encoding="utf-8"))
    return {"replace": False, "values": []}


def entry_id(v):
    return v["id"] if isinstance(v, dict) else v


def main():
    check = "--check" in sys.argv
    sources = read_waves()
    defs, unresolved = parse_defs(sources)
    planned = json.loads((Path(__file__).parent / "relic_slots.json").read_text(encoding="utf-8"))
    for rid, slot in planned.items():
        defs.setdefault(rid, slot)
    for u in unresolved:
        print("warning: could not resolve the id of a def:", u)
    by_tag = {}
    for rid, slot in sorted(defs.items()):
        tag = SLOT_TAG.get(slot)
        if tag:
            by_tag.setdefault(tag, []).append(f"{MODID}:{rid}")
    missing = 0
    for tag, ids in sorted(by_tag.items()):
        path = TAGS / f"{tag}.json"
        data = load_tag(path)
        have = {entry_id(v) for v in data["values"]}
        add = [i for i in ids if i not in have]
        missing += len(add)
        print(f"{tag}: {len(ids)} relics, {len(add)} to add")
        if add and not check:
            data["values"] = data["values"] + [{"id": i, "required": False} for i in add]
            TAGS.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
    if check and missing:
        sys.exit(1)


if __name__ == "__main__":
    main()

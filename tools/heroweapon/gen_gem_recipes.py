#!/usr/bin/env python3
"""Hero weapons (1.62.0), acquisition: writes the nineteen crafting recipes of the Calling Gems.

    python3 -B tools/heroweapon/gen_gem_recipes.py            write the recipes (idempotent)
    python3 -B tools/heroweapon/gen_gem_recipes.py --check    fail if a shipped recipe differs from the table (nothing is written)
    python3 -B tools/heroweapon/gen_gem_recipes.py --doc      print the Markdown table of the Seal recipes (for docs/heroweapon/gem.md)

The single source of truth is the table in
  src/main/java/com/newuniverse/nusmp/heroweapon/gem/GemRecipes.java
which this script READS (it never repeats the table). It writes, under src/main/resources/data/nusmp/recipe/ (1.21.1 format):
  calling_gem.json        shaped 3 x 3: the Holy Calling Gem (the four jewel colours round a nether star, bound by netherite)
  gem_fragment.json       shapeless: 4 amethyst shards, a gold ingot, an ender pearl -> one Gem Fragment
  vassal_seal_<kind>.json shapeless: a Gem Fragment and the weapon's signature material -> its Vassal Seal (seventeen)
GemRecipesTest reads the same JSON back and fails when it differs from the Java table, so the two cannot drift.
"""
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "heroweapon", "gem", "GemRecipes.java")
OUT = os.path.join(ROOT, "src", "main", "resources", "data", "nusmp", "recipe")
CATALOG = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "heroweapon", "HeroKind.java")

FRAGMENT_ITEM = "nusmp:gem_fragment"
HOLY_ID = "calling_gem"
FRAGMENT_ID = "gem_fragment"
SEAL_PREFIX = "vassal_seal_"


def read_catalogue():
    """The weapons in enum order with their class, from HeroKind.java (the only list of weapons)."""
    text = open(CATALOG, encoding="utf-8").read()
    kinds = []
    for m in re.finditer(r'^\s*([A-Z_]+)\("[a-z_]+",\s*"([^"]+)",\s*HeroClass\.([A-Z_]+),', text, re.M):
        kinds.append((m.group(1), m.group(2), m.group(3)))
    if len(kinds) < 21:
        sys.exit("could not read the weapon catalogue from HeroKind.java (found %d)" % len(kinds))
    return kinds


def read_table():
    text = open(JAVA, encoding="utf-8").read()
    pat = re.search(r'HOLY_PATTERN\s*=\s*List\.of\(([^)]*)\)', text)
    pattern = re.findall(r'"([A-Z]{3})"', pat.group(1)) if pat else []
    keys_block = re.search(r'HOLY_KEYS\s*=\s*Map\.of\((.*?)\);', text, re.S)
    keys = dict(re.findall(r"'([A-Z])',\s*\"([a-z_]+:[a-z0-9_]+)\"", keys_block.group(1))) if keys_block else {}
    frag_block = re.search(r'FRAGMENT_PARTS\s*=\s*List\.of\((.*?)\);', text, re.S)
    fragment = [(i, int(n)) for i, n in re.findall(r'new Part\("([a-z_]+:[a-z0-9_]+)",\s*(\d+)\)', frag_block.group(1))] if frag_block else []
    seals = {}
    for m in re.finditer(r'^\s*seal\(HeroKind\.([A-Z_]+),\s*"((?:[^"\\]|\\.)*)",\s*(.*)\);\s*$', text, re.M):
        parts = [(i, int(n)) for i, n in re.findall(r'p\("([a-z_]+:[a-z0-9_]+)",\s*(\d+)\)', m.group(3))]
        if not parts:
            sys.exit("the Seal of %s has no ingredients" % m.group(1))
        seals[m.group(1)] = (m.group(2).replace('\\"', '"'), parts)
    if len(pattern) != 3 or not keys or not fragment:
        sys.exit("could not read the Holy pattern, its key or the fragment recipe from GemRecipes.java")
    for row in pattern:
        for c in row:
            if c not in keys:
                sys.exit("the Holy pattern uses the key %r which HOLY_KEYS does not define" % c)
    return pattern, keys, fragment, seals


def recipes():
    """{file name: json object} for all nineteen recipes."""
    pattern, keys, fragment, seals = read_table()
    cat = read_catalogue()
    out = {}
    used = sorted({c for row in pattern for c in row})
    out[HOLY_ID + ".json"] = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": {c: {"item": keys[c]} for c in used},
        "pattern": pattern,
        "result": {"id": "nusmp:" + HOLY_ID, "count": 1},
    }
    out[FRAGMENT_ID + ".json"] = {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": [{"item": i} for i, n in fragment for _ in range(n)],
        "result": {"id": "nusmp:" + FRAGMENT_ID, "count": 1},
    }
    for name, display, klass in cat:
        if klass == "HOLY":
            continue
        if name not in seals:
            sys.exit("GemRecipes.java has no Seal recipe for %s" % name)
        why, parts = seals[name]
        item_id = SEAL_PREFIX + name.lower()
        ingredients = [{"item": FRAGMENT_ITEM}] + [{"item": i} for i, n in parts for _ in range(n)]
        if len(ingredients) > 9:
            sys.exit("the Seal of %s needs %d slots, the grid has 9" % (name, len(ingredients)))
        out[item_id + ".json"] = {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "group": "vassal_seal",
            "ingredients": ingredients,
            "result": {"id": "nusmp:" + item_id, "count": 1},
        }
    extra = set(seals) - {n for n, _, k in cat if k != "HOLY"}
    if extra:
        sys.exit("GemRecipes.java has Seal recipes for weapons that have no Seal: %s" % sorted(extra))
    return out


def render(obj):
    return json.dumps(obj, indent=2, ensure_ascii=True) + "\n"


def doc_table():
    pattern, keys, fragment, seals = read_table()
    cat = read_catalogue()
    lines = ["| Seal | Class | Gem Fragment plus | Why |", "|---|---|---|---|"]
    for name, display, klass in cat:
        if klass == "HOLY":
            continue
        why, parts = seals[name]
        short = display[len("Vassal "):] if display.startswith("Vassal ") else display
        mats = ", ".join("%d x %s" % (n, i.split(":")[1]) if n > 1 else i.split(":")[1] for i, n in parts)
        lines.append("| Vassal Seal: %s | %s | %s | %s |" % (short, "Seven Star" if klass == "SEVEN_STAR" else "Vassal", mats, why))
    return "\n".join(lines)


def main(argv):
    if "--doc" in argv:
        print(doc_table())
        return 0
    want = recipes()
    check = "--check" in argv
    os.makedirs(OUT, exist_ok=True)
    bad = []
    for name, obj in sorted(want.items()):
        path = os.path.join(OUT, name)
        text = render(obj)
        have = open(path, encoding="utf-8").read() if os.path.exists(path) else None
        if have == text:
            continue
        if check:
            bad.append(name)
        else:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(text)
            print("wrote", name)
    if check:
        if bad:
            print("out of date or missing:", ", ".join(bad))
            return 1
        print("all %d recipes match the table" % len(want))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

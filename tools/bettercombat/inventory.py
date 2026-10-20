#!/usr/bin/env python3
"""Why: Better Combat only knows a weapon if a data file exists for its registry id, and a missing file fails silently (the item just keeps
the vanilla one-click swing). So the list of "every melee weapon of the mod" has to come from the repo sources, not from a hand-kept list that
rots the day someone adds a relic. This script builds that list from the places weapons are declared:

  1. the `weapons` table of src/main/resources/assets/nusmp/weapon_classes.json (the size-class table every model goes through; the copy in
     tools/relics/weapon_classes.json must agree),
  2. NUItems: every `weapon("id", Kind...)` / `ITEMS.register("id", ...)` that is a sword-like class (MagicWeaponItem, MagicToolSword,
     DemonSlayerSwordItem, OtherworldTridentItem),
  3. relics/**: every `RelicDef.of("id", ...)` block that calls `.melee(...)`, and the generated left half `<id>_left` of every `.pair(...)`,
  4. the Djinn weapons: `djinn_weapon_<djinn>` and `_dark` for every Djinn of the roster, class from DjinnWeaponRules' `row(...)` kind.

and writes tools/bettercombat/manifest.json: id -> {class, profile, source}, plus the excluded ids with the reason.

Usage:  python tools/bettercombat/inventory.py [--check] [--list]
  --check  exit 1 when the manifest on disk differs from the sources (or an id has no class)
  --list   print the melee ids, one per line, with class
"""
import glob
import json
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import profiles as PR  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
JAVA = os.path.join(ROOT, "src", "main", "java")
CLASS_JSON = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "weapon_classes.json")
TOOLS_CLASS_JSON = os.path.join(ROOT, "tools", "relics", "weapon_classes.json")
MANIFEST = os.path.join(HERE, "manifest.json")

# Decisions per item that the class table cannot make (documented in docs/better_combat.md).
EXCLUDED_REASON = {
    "handitem": "hand item / charm / book / key: not a weapon",
    "crossbow": "ranged (crossbow): Better Combat only handles melee",
    "bow": "ranged (bow): Better Combat only handles melee",
    "shield": "shield: no swing",
    "item": "not a weapon",
}
# Table entries that are not registered items (only a model file), kept as aliases of the real weapon so a stack that carries the id still works.
ALIAS_OF = {"elsdocia_legacy": "elsdocia"}
PAIR_CLASS_PROFILE = {"sabre": "twin_sabre", "shortsword": "twin_shortsword"}


def read(p):
    with open(p, encoding="utf-8") as f:
        return f.read()


def java_files():
    return sorted(glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True))


def class_table():
    a = json.loads(read(CLASS_JSON))["weapons"]
    b = json.loads(read(TOOLS_CLASS_JSON))["weapons"]       # the tools copy also carries _doc / excluded notes; only the table must agree
    b = {k: (v["class"] if isinstance(v, dict) else v) for k, v in b.items()}     # the tools copy stores {"class": ...}, the game copy the bare name
    return a, b


def nuitems_ids():
    """Ids of the sword-like items NUItems registers, with the registering expression's class."""
    t = read(os.path.join(JAVA, "com", "newuniverse", "nusmp", "item", "NUItems.java"))
    out = {}
    for m in re.finditer(r'weapon\("([a-z0-9_]+)",\s*MagicWeaponItem\.Kind', t):
        out[m.group(1)] = "NUItems.weapon"
    for m in re.finditer(r'ITEMS\.register\("([a-z0-9_]+)",\s*(?:\(\)\s*->\s*)?(?:new\s+)?(MagicToolSword|DemonSlayerSwordItem|OtherworldTridentItem)', t):
        out[m.group(1)] = "NUItems." + m.group(2)
    return out


def relic_melee():
    """(melee ids, pair base ids) from every RelicDef block; a block runs from its `RelicDef.of(` to the next one."""
    melee, pairs = {}, set()
    for f in java_files():
        t = read(f)
        if "RelicDef.of(" not in t:
            continue
        starts = [m for m in re.finditer(r'RelicDef\.of\("([a-z0-9_]+)"', t)]
        for i, m in enumerate(starts):
            end = starts[i + 1].start() if i + 1 < len(starts) else len(t)
            blk = t[m.end():end]
            if ".melee(" in blk:
                melee[m.group(1)] = os.path.relpath(f, JAVA).replace(os.sep, "/")
            if ".pair(" in blk:
                pairs.add(m.group(1))
    return melee, pairs


def djinn_weapons():
    """{id: (kind, reach)} for the Djinn weapons, light and dark."""
    base = os.path.join(JAVA, "com", "newuniverse", "nusmp", "relics", "djinn")
    rules = read(os.path.join(base, "weapon", "DjinnWeaponRules.java"))
    prefix = re.search(r'PREFIX\s*=\s*"([a-z_]+)"', rules).group(1)
    dark = re.search(r'DARK_SUFFIX\s*=\s*"([a-z_]+)"', rules).group(1)
    out = {}
    for m in re.finditer(r'row\(Djinn\.([A-Z_]+),\s*"([a-z]+)",\s*([\d.]+),\s*([\d.]+),\s*([\d.]+),', rules):
        did, kind, reach = m.group(1).lower(), m.group(2), float(m.group(5))
        out[prefix + did] = (kind, reach)
        out[prefix + did + dark] = (kind, reach)
    return out


def profile_name(wid, cls, source):
    if wid == "demon_slayer_sword":
        return "genesis"
    if wid in ("elsdocia", "elsdocia_legacy"):
        return "elsdocia"
    if source.startswith("pair"):
        return PAIR_CLASS_PROFILE[cls]
    if source.startswith("djinn"):
        return "djinn_" + cls
    return cls


def build():
    """Returns (weapons, excluded, problems): weapons {id: {class, profile, source}}, excluded {id: reason}."""
    table, table2 = class_table()
    problems = []
    if table != table2:
        problems.append("tools/relics/weapon_classes.json and assets/nusmp/weapon_classes.json disagree")
    nu = nuitems_ids()
    melee, pairs = relic_melee()
    djinn = djinn_weapons()
    weapons, excluded = {}, {}

    def add(wid, cls, source):
        weapons[wid] = {"class": cls, "profile": profile_name(wid, cls, source), "source": source}

    for wid, cls in table.items():
        if cls in PR.NON_MELEE_CLASSES:
            excluded[wid] = EXCLUDED_REASON[cls]
            continue
        src = "relic" if wid in melee else ("item" if wid in nu else "model-only alias" if wid in ALIAS_OF else "table")
        add(wid, cls, src)
    # every relic melee def and every NUItems sword-like item must have a class
    for wid in melee:
        if wid not in table:
            problems.append("relic with .melee() has no class in weapon_classes.json: " + wid)
    for wid, how in nu.items():
        if wid not in table:
            if wid == "demon_slayer_sword":
                add(wid, "greatsword", "item")           # Genesis: absent from the table (it has its own model pipeline), a greatsword
            elif wid == "magic_tool_bow":
                excluded[wid] = EXCLUDED_REASON["bow"]
            else:
                problems.append("NUItems item has no class in weapon_classes.json: " + wid)
    # pair halves: the generated left half copies the class of the right half
    for base in sorted(pairs):
        if base not in weapons:
            problems.append("pair base has no class: " + base)
            continue
        cls = weapons[base]["class"]
        if cls not in PAIR_CLASS_PROFILE:
            problems.append("pair class has no twin profile: %s (%s)" % (base, cls))
            continue
        weapons[base]["profile"] = PAIR_CLASS_PROFILE[cls]
        weapons[base]["source"] = "pair right half"
        add(base + "_left", cls, "pair left half")
    # Djinn weapons
    for wid, (kind, reach) in sorted(djinn.items()):
        if kind == "bow":
            excluded[wid] = "ranged (Sitri's bow): Better Combat only handles melee"
            continue
        if kind not in PR.DJINN_KIND_CLASS:
            problems.append("unknown djinn weapon kind: %s (%s)" % (wid, kind))
            continue
        add(wid, PR.DJINN_KIND_CLASS[kind], "djinn %s reach %.2f" % (kind, reach))
    for wid, cls in weapons.items():
        if cls["class"] not in PR.CLASS_PROFILES:
            problems.append("class has no profile: %s (%s)" % (wid, cls["class"]))
    return dict(sorted(weapons.items())), dict(sorted(excluded.items())), problems


def render(weapons, excluded):
    return json.dumps({"_why": "Generated by tools/bettercombat/inventory.py from the repo sources; do not edit by hand. "
                               "id -> weapon class -> chosen profile (profiles.py). Excluded ids are not melee weapons.",
                       "weapons": weapons, "excluded": excluded}, indent=2) + "\n"


def main(argv):
    weapons, excluded, problems = build()
    for p in problems:
        print("PROBLEM: " + p)
    if "--list" in argv:
        for wid, w in weapons.items():
            print("%-40s %s" % (wid, w["class"]))
        return 1 if problems else 0
    text = render(weapons, excluded)
    cur = read(MANIFEST) if os.path.exists(MANIFEST) else None
    if "--check" in argv:
        if cur != text:
            print("manifest.json is out of date: run python tools/bettercombat/inventory.py")
            return 1
        return 1 if problems else 0
    if cur != text:
        with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)
        print("wrote tools/bettercombat/manifest.json (%d melee weapons, %d excluded)" % (len(weapons), len(excluded)))
    else:
        print("manifest.json unchanged (%d melee weapons, %d excluded)" % (len(weapons), len(excluded)))
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

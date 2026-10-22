#!/usr/bin/env python3
"""
Prints the markdown tables of docs/labyrinth/rooms_rebuilt.md from the recipes and the manifest (so the doc cannot drift from the generator):

    python3 tools/labyrinth/rr_doc_tables.py families    # which shipped templates are in which family
    python3 tools/labyrinth/rr_doc_tables.py recipes     # the four variants of every family in words
    python3 tools/labyrinth/rr_doc_tables.py themes      # the themed halls by theme prefix
    python3 tools/labyrinth/rr_doc_tables.py stands      # the rooms that carry the armour stand slot
"""
import collections
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import rr_halls as H  # noqa: E402

MANIFEST = os.path.join(L.RES, "data", "nusmp", "mega", "rooms_rebuilt.json")

ROOF = {"groin": "groin (cross) vault, one bay per module", "barrel": "barrel vault along the long axis", "flat": "the old roof height, coffered", "gable": "A-frame (gable) roof",
        "dome": "dome over the middle", "hip": "hip (pyramid) roof"}
FLOOR = {"grid": "module grid with medallions", "runner": "wool runner from every door", "checker": "chequerboard in a border", "rosette": "concentric rings", "border": "bordered field",
         "diamond": "diagonal lattice", "inlay": "cross and medallion inlay", "ruin": "broken tiles"}


def manifest():
    return json.load(open(MANIFEST))


def families():
    m = manifest()
    by = collections.defaultdict(list)
    for name in m["variants"]:
        if name.split("/")[1] in ("hall", "junction", "treasure") or name.split("/")[-1].startswith("gallery"):
            by[H.family_of(name)].append(name.replace("mega/", ""))
    print("| family | rooms |")
    print("|---|---|")
    for fam in sorted(by):
        print("| %s | %s |" % (fam, ", ".join("`%s`" % n for n in sorted(by[fam]))))


def recipes():
    print("| family | v1 | v2 | v3 | v4 |")
    print("|---|---|---|---|---|")
    for fam in ["arms", "library", "sacred", "crypt", "cells", "hearth", "water", "lab", "other"]:
        rc = H.recipes_for(fam)
        cells = []
        for v in (1, 2, 3, 4):
            r = rc[v]
            kind, rise = r["roof"]
            bits = ["%s (rise up to %d)" % (ROOF[kind], rise) if rise else ROOF[kind]]
            bits.append("floor: %s" % FLOOR[r["floor"]])
            bits.append("%s pilasters" % ("standing" if r["pilaster"] == "standing" else "flush"))
            if r["pillars"]:
                bits.append("pillars at the rib crossings")
            if r["balcony"] is not None:
                bits.append("galleries" + (" with shelves" if r["balcony"].get("shelves") else ""))
            if r["coffers"]:
                bits.append("coffers with %s lights" % r["coffers"].split(":")[1].replace("_", " "))
            bits.append("%s" % {"chandelier": "chandeliers", "hanging": "hanging lanterns", "rib": "rib lamps"}[r["light"]])
            if r["posts"]:
                bits.append("lamp posts")
            w = r["wear"]
            bits.append("wear %d%% cracks%s%s" % (round(100 * w["crack"]), ", moss" if w["moss"] else "", ", broken ribs" if w["broken"] else ""))
            cells.append("; ".join(bits))
        print("| %s | %s |" % (fam, " | ".join(cells)))


def themes():
    m = manifest()
    by = collections.defaultdict(list)
    for name in m["variants"]:
        stem = name.split("/")[-1]
        for th in ("celestial_court", "crystal_geode", "cursed_archive", "drowned_crypt", "dwarven_forgehold", "feywild_grove", "fungal_underdark", "haunted_manor", "holy_church",
                   "infernal_foundry", "mountain_monastery", "necropolis", "sky_citadel", "spider_warrens", "sunken_temple", "vampire_castle"):
            if stem.startswith(th + "_"):
                by[th].append(name.replace("mega/", ""))
    print("| theme | themed rooms rebuilt (family) |")
    print("|---|---|")
    for th in sorted(by):
        print("| %s | %s |" % (th, ", ".join("`%s` (%s)" % (n, H.family_of(n)) for n in sorted(by[th]))))


def stands():
    m = manifest()
    print(", ".join("`%s`" % n.replace("mega/", "") for n in m["stand_rooms"]))


if __name__ == "__main__":
    {"families": families, "recipes": recipes, "themes": themes, "stands": stands}[sys.argv[1]]()

#!/usr/bin/env python3
"""Cube / quad / bone statistics of the mob geo models per group (read-only audit helper, phase 0 of the mob remake).

A cube is counted as 6 quads (the loader draws every face; cards are cubes of thickness 0 and still count 6, so this is an upper bound).
Usage: python3 -B tools/mobs_audit/geo_stats.py
"""
import json, os, re, statistics, collections

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
G = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "geo", "entity")

GROUPS = [
    ("foundry_<id> (400 art-kit bodies)", r"^foundry_[a-z0-9_]+$"),
    ("pmx_foundry_<id> (extras only)", r"^pmx_foundry_"),
    ("pmx_dg_* (archetype extras)", r"^pmx_dg_"),
    ("pmx_abyssal_* (humanoid boss extras)", r"^pmx_abyssal_"),
    ("pmx_* (arc extras)", r"^pmx_"),
    ("arc_<id> (94 characters + form2)", r"^arc_"),
    ("abyssal_<look>_p<n> (22 looks x 3)", r"^abyssal_"),
    ("zogratis_*", r"^zogratis_"),
    ("rades_* / corpse_* (puppets)", r"^(rades_|corpse_)"),
    ("celestial_* (zodiac + king)", r"^celestial_"),
    ("tinydjinn_*", r"^tinydjinn_"),
    ("sylph_* / spirit_* (spirit lords)", r"^(sylph_|spirit_)"),
    ("dungeon_* goliath charybdis tower magicule", r"^(dungeon_|goliath|charybdis|tower|magicule)"),
    ("avatar_* (hair / horns / wings)", r"^avatar_"),
]

def stats(path):
    try:
        d = json.load(open(path))
    except Exception:
        return None
    geos = d.get("minecraft:geometry") or []
    cubes = bones = 0
    for g in geos:
        for b in g.get("bones", []):
            bones += 1
            cubes += len(b.get("cubes", []))
    return cubes, bones

def main():
    acc = collections.OrderedDict((n, []) for n, _ in GROUPS)
    for f in sorted(os.listdir(G)):
        if not f.endswith(".geo.json") or f.startswith("foundry_anim"):
            continue
        stem = f[:-len(".geo.json")]
        for n, rx in GROUPS:
            if re.search(rx, stem):
                s = stats(os.path.join(G, f))
                if s: acc[n].append(s)
                break
    print(f"{'group':46s} {'files':>5s} {'avg cubes':>9s} {'max':>5s} {'avg quads':>9s} {'avg bones':>9s}")
    for n, v in acc.items():
        if not v: continue
        c = [x[0] for x in v]; b = [x[1] for x in v]
        print(f"{n:46s} {len(v):5d} {statistics.mean(c):9.0f} {max(c):5d} {6*statistics.mean(c):9.0f} {statistics.mean(b):9.0f}")

if __name__ == "__main__":
    main()

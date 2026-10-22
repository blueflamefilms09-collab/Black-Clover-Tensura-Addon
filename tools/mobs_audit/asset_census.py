#!/usr/bin/env python3
"""Census of the mob / NPC / character assets (read-only audit helper, phase 0 of the mob remake).

Counts files and bytes of geo models, textures and animations per group, plus what the jar would compress them to
(deflate of every file, the way a jar stores them), so the plan can estimate size impacts.

Usage: python3 -B tools/mobs_audit/asset_census.py [--csv docs/mobs/inventory.csv]
Writes nothing unless --csv is given. Reads only src/main/resources/assets/nusmp.
"""
import os, sys, re, zlib, csv, collections

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

# group of a geo / texture / animation stem. Order matters: first match wins.
GROUPS = [
    ("foundry bodies (400 mobs, art-kit)", r"^foundry_"),
    ("pmx-foundry (extras on player rig)", r"^pmx_foundry_"),
    ("pmx-dg (archetype extras+head parts)", r"^pmx_dg_"),
    ("pmx-abyssal",    r"^pmx_abyssal_"),
    ("pmx-other",      r"^pmx_"),
    ("abyssal",        r"^abyssal_"),
    ("erza armour sets (requip, NOT mobs)",   r"^erza_"),
    ("arc",            r"^arc_"),
    ("fairy-tail armour (ft, NOT mobs)", r"^ft_"),
    ("spiritdive armour (NOT mobs)",     r"^spiritdive_"),
    ("avatar",         r"^avatar_"),
    ("tinydjinn",      r"^tinydjinn_"),
    ("zogratis",       r"^zogratis_"),
    ("celestial",      r"^celestial_"),
    ("corpse",         r"^(corpse_|rades_)"),
    ("sylph/spirit",   r"^(sylph_|spirit_|great_spirit)"),
    ("dungeon",        r"^(dungeon_|goliath|charybdis|beast_|tower_)"),
]
def group_of(stem):
    for name, rx in GROUPS:
        if re.search(rx, stem):
            return name
    return "VFX/aura/summon/prop models (not mobs)"

def census(sub, exts):
    out = collections.OrderedDict()
    base = os.path.join(A, sub)
    for dp, dn, fn in os.walk(base):
        for f in fn:
            if not f.endswith(exts):
                continue
            p = os.path.join(dp, f)
            stem = f.split(".")[0]
            rel = os.path.relpath(dp, base)
            top = rel.split(os.sep)[0]
            if rel != "." and top != "entity":
                g = "subdir:" + top
            else:
                g = group_of(stem)
            raw = os.path.getsize(p)
            with open(p, "rb") as fh:
                z = len(zlib.compress(fh.read(), 6))
            d = out.setdefault(g, [0, 0, 0])
            d[0] += 1; d[1] += raw; d[2] += z
    return out

def main():
    rows = []
    for label, sub, exts in (("geo", "geo", (".json",)), ("texture", "textures/entity", (".png",)), ("animation", "animations", (".json",))):
        c = census(sub, exts)
        tot = [0, 0, 0]
        print(f"== {label} ({sub})")
        for g, (n, raw, z) in sorted(c.items(), key=lambda kv: -kv[1][1]):
            print(f"  {g:34s} {n:5d} files {raw/1e6:8.2f} MB raw {z/1e6:8.2f} MB deflated")
            rows.append([label, g, n, round(raw/1e6, 3), round(z/1e6, 3)])
            for i, v in enumerate((n, raw, z)): tot[i] += v
        print(f"  {'TOTAL':34s} {tot[0]:5d} files {tot[1]/1e6:8.2f} MB raw {tot[2]/1e6:8.2f} MB deflated")
        rows.append([label, "TOTAL", tot[0], round(tot[1]/1e6, 3), round(tot[2]/1e6, 3)])
    if "--csv" in sys.argv:
        path = sys.argv[sys.argv.index("--csv") + 1]
        with open(path, "w", newline="") as fh:
            w = csv.writer(fh); w.writerow(["kind", "group", "files", "raw_mb", "deflated_mb"]); w.writerows(rows)

if __name__ == "__main__":
    main()

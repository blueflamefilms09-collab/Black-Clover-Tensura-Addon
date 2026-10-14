"""The 16x16 inventory icons of the three boss relics (Dante's Gravity Crown, Zenon's Spatial Ring, the Artifact Mimic).

Writes textures/item/<id>.png and models/item/<id>.json (parent item/generated) under src/main/resources/assets/nusmp. Idempotent. Reuses the Icon
helper of wave7_icons.py (a char grid, derived light and dark edges, a one pixel outline, grade-coloured rim pixels).

Run from the repository root:  python3 -B tools/relics/waveboss_icons.py
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wave7_icons as w7   # noqa: E402

ASSETS = w7.ASSETS


def crown():
    mask = [
        "................",
        ".......mm.......",
        "..m....mm....m..",
        "..mm.m.mm.m.mm..",
        "..mmmmmmmmmmmm..",
        "..mmmmmmmmmmmm..",
        "..mmmmmmmmmmmm..",
        "..mmmmmmmmmmmm..",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    ic = w7.Icon(mask, 0xD8B04A, 0x5B2A8C, 0xB58CFF, accent=0x7B4BCF, grade="LEGEND")
    for x in range(2, 14):
        ic.put(x, 6, "t")                                  # the violet inlay band
    ic.put(7, 5, "g"); ic.put(8, 5, "g"); ic.put(7, 4, "G"); ic.put(8, 4, "G")   # the brow gem
    for x, y in ((3, 9), (8, 11), (12, 9)):                # the floating shards under the crown (glow pixels)
        ic.force(x, y, "g"); ic.force(x, y + 1, "G")
    for x, y in ((1, 1), (14, 1)):
        ic.force(x, y, "G")
    return ic


def ring():
    mask = [list("." * 16) for _ in range(16)]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 9.0)
            if 3.0 <= d <= 5.4:
                mask[y][x] = "m"
    mask = ["".join(r) for r in mask]
    ic = w7.Icon(mask, 0xC9D3EE, 0x1B2257, 0x8FB0FF, accent=0x3A52D6, grade="LEGEND")
    ic.put(7, 4, "t"); ic.put(8, 4, "t")                    # the setting
    ic.force(7, 3, "g"); ic.force(8, 3, "g"); ic.force(7, 2, "G"); ic.force(8, 2, "G")   # the rift slit standing on the band
    for x, y in ((2, 3), (13, 4), (1, 12), (14, 13)):       # rift fragments
        ic.force(x, y, "g")
    return ic


def mimic():
    mask = [
        "................",
        "................",
        "................",
        ".....mmmmmm.....",
        ".....mmmmmm.....",
        ".....mmmmmm.....",
        ".....mmmmmm.....",
        "mmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmm",
        "mmmmmmmmmmmmmmmm",
        ".....mmmmmm.....",
        ".....mmmmmm.....",
        "................",
        "................",
        "................",
        "................",
    ]
    ic = w7.Icon(mask, 0x6B4423, 0xD8B04A, 0xE0A030, accent=0xA03048, grade="UNIQUE")
    for x in (1, 3, 12, 14):
        ic.put(x, 8, "t")                                   # studs
    for x in (6, 9):
        ic.put(x, 5, "g")                                   # amber eyes
    for x in range(6, 10):
        ic.put(x, 7, "k")                                   # the mouth line
    for x in (6, 8):
        ic.put(x, 8, "w")                                   # teeth under the line
    ic.put(7, 6, "w"); ic.put(9, 6, "w")                    # and over it
    ic.put(7, 10, "t"); ic.put(8, 10, "t"); ic.put(7, 11, "t"); ic.put(8, 11, "u")   # the hasp
    ic.force(7, 12, "x"); ic.force(8, 12, "x"); ic.force(8, 13, "x")                 # the tongue
    return ic


def main():
    tex = os.path.join(ASSETS, "textures", "item")
    mod = os.path.join(ASSETS, "models", "item")
    os.makedirs(tex, exist_ok=True)
    os.makedirs(mod, exist_ok=True)
    for rid, make in (("dante_gravity_crown", crown), ("zenon_spatial_ring", ring), ("artifact_mimic", mimic)):
        make().image().save(os.path.join(tex, rid + ".png"))
        with open(os.path.join(mod, rid + ".json"), "w") as f:
            json.dump({"parent": "minecraft:item/generated", "textures": {"layer0": "nusmp:item/" + rid}}, f, indent=2)
        print("wrote", rid)


if __name__ == "__main__":
    main()

"""Writes tools/render_preview/scenes.d/spirit_dive_reel.json: the scenes of the REEL Spirit Dive (every element, both tiers, half / full, the entry frames, the exit).

    python3 -B tools/spirit_dive_reel_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdr_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_reel_previews.py          # the sheets of docs/previews/spirit_dive_reel/

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0, WATER 1, FIRE 2, EARTH 3; HALF 0, FULL 1, UPGRADE 2; STANDARD 0, YUNO 1, FUEGOLEON 2, NOELLE 3.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_reel.json")
EL = {"wind": 0, "water": 1, "fire": 2, "earth": 3}
STAGE = {"half": 0, "full": 1, "upgrade": 2}
GRADE = {"std": 0, "yuno": 1, "fuego": 2, "noelle": 3}
CHAR = {"wind": "yuno", "fire": "fuego", "water": "noelle"}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
VIEWS3 = [{"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 4.6}, {"name": "back 3/4", "yaw": 150, "pitch": 12, "target": [0, Y, 0.1], "dist": 4.6},
          {"name": "side", "yaw": 90, "pitch": 6, "target": [0, Y, 0.1], "dist": 4.6}]
VIEWS_FRONT = [{"name": "front", "yaw": 0, "pitch": 6, "target": [0, Y, 0.1], "dist": 4.6}]
VIEWS_34 = [{"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 4.6}]
VIEWS_CLOSE = [{"name": "head and chest", "yaw": 24, "pitch": 6, "target": [0, 1.45, 0.0], "dist": 2.1}, {"name": "head, back", "yaw": 160, "pitch": 8, "target": [0, 1.4, 0.1], "dist": 2.6}]
VIEWS_LOW = [{"name": "low front", "yaw": 12, "pitch": -4, "target": [0, 1.0, 0.1], "dist": 4.2}]


def scene(el, grade, stage, ages=(50,), views=VIEWS3, tile=(380, 560)):
    d = {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": EL[el] + 4 * STAGE[stage] + 16 * GRADE[grade], "total": 120, "ages": list(ages), "tile": list(tile),
         "views": views, "examples": ["SdrReel"]}
    if el == "fire" and grade == "fuego":
        d["examples"] = ["SdrReel", "SdvFuegoleonArm"]
        d["alsoAuras"] = ["FUEGOLEON_ARM:2"]
    return d


def main():
    out = {"_comment": "Spirit Dive, REEL look: written by tools/spirit_dive_reel_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for el in EL:
        tiers = ["std"] + ([CHAR[el]] if el in CHAR else [])
        for g in tiers:
            out["sdr_%s_%s_full" % (el, g)] = scene(el, g, "full", views=VIEWS3, tile=(380, 560))
            out["sdr_%s_%s_half" % (el, g)] = scene(el, g, "half", views=VIEWS_34, tile=(330, 480))
            out["sdr_%s_%s_front" % (el, g)] = scene(el, g, "full", views=VIEWS_FRONT, tile=(380, 560))
            out["sdr_%s_%s_close" % (el, g)] = scene(el, g, "full", views=VIEWS_CLOSE, tile=(440, 440))
            out["sdr_%s_%s_entry" % (el, g)] = scene(el, g, "full", ages=(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24), views=VIEWS_34, tile=(260, 440))
            out["sdr_%s_%s_exit" % (el, g)] = scene(el, g, "full", ages=(100, 104, 108, 112, 116, 119), views=VIEWS_34, tile=(260, 440))
    out["sdr_wind_yuno_upgrade"] = scene("wind", "yuno", "upgrade", ages=(2, 6, 10, 16, 40), views=VIEWS_34, tile=(260, 440))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

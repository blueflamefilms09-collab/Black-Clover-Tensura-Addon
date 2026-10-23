"""Writes tools/render_preview/scenes.d/spirit_dive_merge.json: the scenes of the Spirit Dive MERGE (the diver merges with the spirit): every element, both tiers, the entry frames, the steady state, the exit.

    python3 -B tools/gen_sdm_models.py                                    # the meshes first
    python3 -B tools/spirit_dive_merge_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdm_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_merge_sheets.py

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0, WATER 1, FIRE 2, EARTH 3; FULL stage 1; STANDARD 0, YUNO 1, FUEGOLEON 2, NOELLE 3.
The look is REEL (the default); the Wind is drawn as design A (A_ANIME, the new default Wind) with the example SdwV3A; the others as REEL; `_off` scenes (SdmOff) draw the same dive without the merge.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_merge.json")
EL = {"wind": 0, "water": 1, "fire": 2, "earth": 3}
GRADE = {"std": 0, "yuno": 1, "fuego": 2, "noelle": 3}
CHAR = {"wind": "yuno", "fire": "fuego", "water": "noelle"}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java",
        "book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
V34 = [{"name": "front 3/4", "yaw": 20, "pitch": 6, "target": [0, Y, 0.0], "dist": 6.4}]
V_BACK = [{"name": "back 3/4", "yaw": 150, "pitch": 8, "target": [0, Y, 0.0], "dist": 6.4}]
V_STEADY = [{"name": "front", "yaw": 0, "pitch": 6, "target": [0, Y, 0.0], "dist": 4.6}, {"name": "back 3/4", "yaw": 150, "pitch": 10, "target": [0, Y, 0.0], "dist": 4.6}]
ENTRY = (0, 4, 8, 12, 14, 16, 20, 24, 60)
EXIT = (100, 104, 108, 112, 116, 119)


def scene(el, g, ages, views, tile, off=False):
    ex = ["SdwV3A"] if el == "wind" else ["SdrReel"]
    ex = ex + ["SdmOff" if off else "SdmOn"]
    d = {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": EL[el] + 4 * 1 + 16 * GRADE[g], "total": 120, "ages": list(ages), "tile": list(tile), "views": views, "examples": ex}
    if el == "fire" and g == "fuego":
        d["examples"] = ex + ["SdvFuegoleonArm"]
        d["alsoAuras"] = ["FUEGOLEON_ARM:2"]
    return d


VS = [{"name": "solo", "yaw": 18, "pitch": 6, "target": [0.45, Y, 0.0], "dist": 6.8}]
SOLO_A = (2, 4, 6, 8, 10, 12, 13)
SOLO_B = (14, 15, 16, 17, 18, 20, 24)
SOLO_X = (100, 104, 108, 112, 116, 119)


def solo(el, g, ages):
    d = scene(el, g, ages, VS, (340, 420))
    d["examples"] = ["SdmSolo"] + (["SdvFuegoleonArm"] if (el == "fire" and g == "fuego") else [])
    return d


def main():
    out = {"_comment": "Spirit Dive MERGE: written by tools/spirit_dive_merge_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for el in EL:
        for g in ["std"] + ([CHAR[el]] if el in CHAR else []):
            out["sdm_%s_%s_entry" % (el, g)] = scene(el, g, ENTRY, V34, (400, 440))
            out["sdm_%s_%s_exit" % (el, g)] = scene(el, g, EXIT, V34, (400, 440))
            out["sdm_%s_%s_back" % (el, g)] = scene(el, g, (12, 14, 16, 20, 60), V_BACK, (400, 440))
            out["sdm_%s_%s_steady" % (el, g)] = scene(el, g, (60,), V_STEADY, (380, 560))
            out["sdm_%s_%s_soloA" % (el, g)] = solo(el, g, SOLO_A)
            out["sdm_%s_%s_soloB" % (el, g)] = solo(el, g, SOLO_B)
            out["sdm_%s_%s_soloX" % (el, g)] = solo(el, g, SOLO_X)
            out["sdm_%s_%s_off" % (el, g)] = scene(el, g, (60,), V_STEADY[:1], (380, 560), off=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

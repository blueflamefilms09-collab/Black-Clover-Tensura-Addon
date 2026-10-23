"""Writes tools/render_preview/scenes.d/spirit_dive_merge2.json: the preview scenes of merge 2 (slim spirits, calm entry, first person merge, small crowns).

    python3 -B tools/gen_sdm2_models.py && python3 -B tools/gen_sdm2_crowns.py          # the meshes first
    python3 -B tools/spirit_dive_merge2_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdm2_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_merge2_sheets.py

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0, WATER 1, FIRE 2, EARTH 3; FULL stage 1; STANDARD 0, YUNO 1, FUEGOLEON 2, NOELLE 3.
Wind is drawn as design A (the default) with SdwV4A; the others in the REEL look (SdrReel). The merge 2 helpers (Sdm2New, Sdm2Old, Sdm2Boxy, Sdm2NoCalm, Sdm2CrownOld, Sdm2Fp) set the client options.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_merge2.json")
EL = {"wind": 0, "water": 1, "fire": 2, "earth": 3}
GRADE = {"std": 0, "yuno": 1, "fuego": 2, "noelle": 3}
CHAR = {"wind": "yuno", "fire": "fuego", "water": "noelle"}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java", "book/SpiritDiveWind4Rules.java",
        "book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java", "book/SpiritDiveMerge2Rules.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
V34 = [{"name": "front 3/4", "yaw": 20, "pitch": 6, "target": [0, Y, 0.0], "dist": 6.4}]
VS = [{"name": "solo", "yaw": 18, "pitch": 6, "target": [0.45, Y, 0.0], "dist": 6.8}]
SOLO = (2, 5, 8, 10, 12, 13, 14, 16, 18)
ENTRY = (0, 4, 8, 10, 12, 14, 16, 18, 20, 24)
CT = [0, 2.12, 0.0]
VIEWS_CROWN = [{"name": "front", "yaw": 0, "pitch": 10, "target": CT, "dist": 2.0}, {"name": "3/4", "yaw": 38, "pitch": 12, "target": CT, "dist": 2.0},
               {"name": "side", "yaw": 90, "pitch": 6, "target": CT, "dist": 2.0}]
EYE = [0.0, 1.62, 0.30]
VIEW_FP = [{"name": "first person", "eye": EYE, "target": [0.0, 1.62, 1.3], "fov": 70}]
FP_AGES = (2, 5, 8, 10, 12, 13, 14, 15, 16, 18)


def look(el):
    return "SdwV4A" if el == "wind" else "SdrReel"


def scene(el, g, helper, ages, views, tile, extra=None, solo=False, bg=None, fp=False):
    ex = [helper] if fp else ([look(el)] if not solo else []) + [helper] + (["SdmSolo"] if solo else [])
    d = {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": EL[el] + 4 * 1 + 16 * GRADE[g], "total": 120, "ages": list(ages), "tile": list(tile), "views": views, "examples": ex}
    if bg:
        d["bg"] = bg
        d["floor"] = False
    if el == "fire" and g == "fuego":
        d["examples"] = ex + ["SdvFuegoleonArm"]
        d["alsoAuras"] = ["FUEGOLEON_ARM:2"]
    return d


def main():
    out = {"_comment": "Spirit Dive merge 2: written by tools/spirit_dive_merge2_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for el in EL:
        for g in ["std"] + ([CHAR[el]] if el in CHAR else []):
            for tag, helper in (("slim", "Sdm2New"), ("boxy", "Sdm2Boxy")):
                out["sdm2_%s_%s_solo_%s" % (el, g, tag)] = scene(el, g, helper, SOLO, VS, (300, 400), solo=True)
            for tag, helper in (("new", "Sdm2New"), ("old", "Sdm2Old")):
                out["sdm2_%s_%s_entry_%s" % (el, g, tag)] = scene(el, g, helper, ENTRY, V34, (260, 420))
            for tag, helper in (("new", "Sdm2New"), ("old", "Sdm2CrownOld")):
                for bg in ("dark", "bright"):
                    out["sdm2_%s_%s_crown_%s_%s" % (el, g, tag, bg)] = scene(el, g, helper, (50,), VIEWS_CROWN, (300, 300), bg=bg)
            out["sdm2_%s_%s_fp" % (el, g)] = scene(el, g, "Sdm2Fp", FP_AGES, VIEW_FP, (320, 200), fp=True)
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

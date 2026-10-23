"""Writes tools/render_preview/scenes.d/spirit_dive_wind3.json: the scenes of the Wind remake (three looks A, B, C; standard tier and Yuno; full, half, upgrade, the entry, the exit, the cast).

    python3 -B tools/gen_sdw3_models.py                                   # the meshes first
    python3 -B tools/spirit_dive_wind3_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdw3_*"
    SDV_CAST=1 RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py sdw3_a_yuno_cast       # the bow / the big arrow are drawn while a page is cast

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0; HALF 0, FULL 1, UPGRADE 2; STANDARD 0, YUNO 1.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind3.json")
STAGE = {"half": 0, "full": 1, "upgrade": 2}
GRADE = {"std": 0, "yuno": 1}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
VIEWS3 = [{"name": "front", "yaw": 0, "pitch": 6, "target": [0, Y, 0.1], "dist": 5.2}, {"name": "front 3/4", "yaw": 32, "pitch": 8, "target": [0, Y, 0.1], "dist": 5.2},
          {"name": "back 3/4", "yaw": 150, "pitch": 12, "target": [0, Y, 0.1], "dist": 5.2}]
VIEWS4 = VIEWS3 + [{"name": "side", "yaw": 90, "pitch": 6, "target": [0, Y, 0.1], "dist": 5.2}]
VIEWS_34 = [{"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 5.0}]
VIEWS_CLOSE = [{"name": "head and chest", "yaw": 24, "pitch": 6, "target": [0, 1.45, 0.0], "dist": 2.1}, {"name": "head, back", "yaw": 160, "pitch": 8, "target": [0, 1.4, 0.1], "dist": 2.6}]
VIEWS_HAND = [{"name": "weapon", "yaw": 50, "pitch": 4, "target": [-0.45, 0.95, -0.2], "dist": 2.6}, {"name": "weapon, front", "yaw": 0, "pitch": 4, "target": [-0.45, 0.95, -0.2], "dist": 2.6}]
EX = {"a": "SdwV3A", "b": "SdwV3B", "c": "SdwV3C"}


def scene(grade, stage, ex, ages=(50,), views=VIEWS4, tile=(380, 560)):
    return {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": 0 + 4 * STAGE[stage] + 16 * GRADE[grade], "total": 120, "ages": list(ages), "tile": list(tile),
            "views": views, "examples": [EX[ex]]}


def main():
    out = {"_comment": "Spirit Dive, Wind remake (looks A, B, C): written by tools/spirit_dive_wind3_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for v in "abc":
        for g in ("std", "yuno"):
            out["sdw3_%s_%s_full" % (v, g)] = scene(g, "full", v, views=VIEWS4, tile=(400, 580))
            out["sdw3_%s_%s_half" % (v, g)] = scene(g, "half", v, views=VIEWS_34, tile=(330, 480))
            out["sdw3_%s_%s_close" % (v, g)] = scene(g, "full", v, views=VIEWS_CLOSE, tile=(440, 440))
            out["sdw3_%s_%s_weapon" % (v, g)] = scene(g, "full", v, views=VIEWS_HAND, tile=(440, 440))
            out["sdw3_%s_%s_entry" % (v, g)] = scene(g, "full", v, ages=(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24), views=VIEWS_34, tile=(260, 440))
            out["sdw3_%s_%s_exit" % (v, g)] = scene(g, "full", v, ages=(100, 104, 108, 112, 116, 119), views=VIEWS_34, tile=(260, 440))
        out["sdw3_%s_yuno_upgrade" % v] = scene("yuno", "upgrade", v, ages=(2, 6, 10, 16, 40), views=VIEWS_34, tile=(260, 440))
        out["sdw3_%s_yuno_cast" % v] = scene("yuno", "full", v, views=VIEWS3, tile=(400, 580))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

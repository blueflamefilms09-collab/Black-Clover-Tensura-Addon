"""Writes tools/render_preview/scenes.d/spirit_dive_wind_final.json: the scenes of the final Wind A (3D crown, calm steady state; standard tier and Yuno).

    python3 -B tools/gen_sdw4_models.py                                     # the crown meshes first
    python3 -B tools/spirit_dive_wind_final_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdw4_*"
    SDV_CAST=1 RENDER_PREVIEW_BUILD=<dir2> python3 -B tools/render_preview/preview.py sdw4_yuno_cast      # a cast is going: the loud effects are on

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0; HALF 0, FULL 1, UPGRADE 2; STANDARD 0, YUNO 1.
The dive lasts 120 ticks in the harness ("total"): ages 0..22 are the entry, 22..34 the settling, 50 the quiet steady state, 100..119 the exit.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind_final.json")
STAGE = {"half": 0, "full": 1, "upgrade": 2}
GRADE = {"std": 0, "yuno": 1}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java", "book/SpiritDiveWind4Rules.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
VIEWS4 = [{"name": "front", "yaw": 0, "pitch": 6, "target": [0, Y, 0.1], "dist": 5.2}, {"name": "front 3/4", "yaw": 32, "pitch": 8, "target": [0, Y, 0.1], "dist": 5.2},
          {"name": "back 3/4", "yaw": 150, "pitch": 12, "target": [0, Y, 0.1], "dist": 5.2}, {"name": "side", "yaw": 90, "pitch": 6, "target": [0, Y, 0.1], "dist": 5.2}]
VIEWS_34 = [{"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 5.0}]
# the crown from four sides (the head of the player is at about 1.4 .. 1.9 blocks in the harness, the crown just above)
CT = [0, 2.25, 0.0]
VIEWS_CROWN = [{"name": "crown, front", "yaw": 0, "pitch": 14, "target": CT, "dist": 2.7}, {"name": "crown, 3/4 right", "yaw": 55, "pitch": 14, "target": CT, "dist": 2.7},
               {"name": "crown, side", "yaw": 90, "pitch": 6, "target": CT, "dist": 2.7}, {"name": "crown, back", "yaw": 180, "pitch": 14, "target": CT, "dist": 2.7}]
VIEWS_HEAD = [{"name": "head and chest", "yaw": 24, "pitch": 6, "target": [0, 1.45, 0.0], "dist": 2.1}, {"name": "head, back", "yaw": 160, "pitch": 8, "target": [0, 1.4, 0.1], "dist": 2.6}]
AGES_ENTRY = (4, 8, 12, 16, 20, 22, 26, 30, 36, 50)


def scene(grade, stage, ex, ages=(50,), views=VIEWS4, tile=(400, 580)):
    return {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": 0 + 4 * STAGE[stage] + 16 * GRADE[grade], "total": 120, "ages": list(ages), "tile": list(tile),
            "views": views, "examples": [ex]}


def main():
    out = {"_comment": "Spirit Dive, final Wind A: written by tools/spirit_dive_wind_final_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for g in ("std", "yuno"):
        out["sdw4_%s_full" % g] = scene(g, "full", "SdwV4A")
        out["sdw4_%s_crown" % g] = scene(g, "full", "SdwV4A", views=VIEWS_CROWN, tile=(380, 380))
        out["sdw4_%s_head" % g] = scene(g, "full", "SdwV4A", views=VIEWS_HEAD, tile=(440, 440))
        out["sdw4_%s_entry" % g] = scene(g, "full", "SdwV4A", ages=AGES_ENTRY, views=VIEWS_34, tile=(260, 440))
        out["sdw4_%s_loud" % g] = scene(g, "full", "SdwV4Loud", views=VIEWS_34, tile=(400, 580))
        out["sdw4_%s_exit" % g] = scene(g, "full", "SdwV4A", ages=(100, 106, 112, 118), views=VIEWS_34, tile=(260, 440))
        out["sdw4_%s_half" % g] = scene(g, "half", "SdwV4A", views=VIEWS_34, tile=(330, 480))
    out["sdw4_yuno_upgrade"] = scene("yuno", "upgrade", "SdwV4A", ages=(2, 6, 10, 16, 40), views=VIEWS_34, tile=(260, 440))
    out["sdw4_yuno_cast"] = scene("yuno", "full", "SdwV4A", views=VIEWS_34, tile=(400, 580))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

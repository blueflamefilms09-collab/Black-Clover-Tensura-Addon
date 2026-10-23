"""Writes tools/render_preview/scenes.d/spirit_dive_wind.json: the scenes of the Wind of the PACK look (standard tier and Yuno; full, half, upgrade, the entry, the exit, the bow) and, for the before / after
sheets, the Wind of the REEL look of 3.3.

    python3 -B tools/spirit_dive_wind_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "sdw_*"
    SDV_CAST=1 RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py sdw_yuno_bow          # the bow is drawn while a page is cast
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/spirit_dive_wind_previews.py                              # the sheets of docs/previews/spirit_dive_wind/

style = element + 4 * stage + 16 * grade (book.SpiritDivePieces.style): WIND 0; HALF 0, FULL 1, UPGRADE 2; STANDARD 0, YUNO 1.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "spirit_dive_wind.json")
STAGE = {"half": 0, "full": 1, "upgrade": 2}
GRADE = {"std": 0, "yuno": 1}
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java"]
Y = 1.0
VIEWS4 = [{"name": "front", "yaw": 0, "pitch": 6, "target": [0, Y, 0.1], "dist": 4.6}, {"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 4.6},
          {"name": "back 3/4", "yaw": 150, "pitch": 12, "target": [0, Y, 0.1], "dist": 4.6}, {"name": "side", "yaw": 90, "pitch": 6, "target": [0, Y, 0.1], "dist": 4.6}]
VIEWS4_W = [dict(v, dist=5.6) for v in VIEWS4]
VIEWS_34 = [{"name": "front 3/4", "yaw": 30, "pitch": 8, "target": [0, Y, 0.1], "dist": 4.6}]
VIEWS_CLOSE = [{"name": "head and chest", "yaw": 24, "pitch": 6, "target": [0, 1.45, 0.0], "dist": 2.1}, {"name": "head, back", "yaw": 160, "pitch": 8, "target": [0, 1.4, 0.1], "dist": 2.6}]
VIEWS_HAND = [{"name": "hand", "yaw": 60, "pitch": 4, "target": [0.35, 1.0, -0.1], "dist": 2.4}]


def scene(grade, stage, ages=(50,), views=VIEWS4, tile=(380, 560), example="SdwPack"):
    return {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "style": 0 + 4 * STAGE[stage] + 16 * GRADE[grade], "total": 120, "ages": list(ages), "tile": list(tile),
            "views": views, "examples": [example]}


def main():
    out = {"_comment": "Spirit Dive, Wind of the PACK look: written by tools/spirit_dive_wind_scenes.py (do not edit by hand). style = element + 4 * stage + 16 * grade."}
    for g in ("std", "yuno"):
        out["sdw_%s_full" % g] = scene(g, "full", views=VIEWS4_W if g == "yuno" else VIEWS4, tile=(480, 560) if g == "yuno" else (380, 560))
        out["sdw_%s_half" % g] = scene(g, "half", views=VIEWS_34, tile=(330, 480))
        out["sdw_%s_close" % g] = scene(g, "full", views=VIEWS_CLOSE, tile=(440, 440))
        out["sdw_%s_hand" % g] = scene(g, "full", views=VIEWS_HAND, tile=(440, 440))
        out["sdw_%s_entry" % g] = scene(g, "full", ages=(0, 2, 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24), views=VIEWS_34, tile=(260, 440))
        out["sdw_%s_exit" % g] = scene(g, "full", ages=(100, 104, 108, 112, 116, 119), views=VIEWS_34, tile=(260, 440))
        out["sdw_reel_%s_full" % g] = scene(g, "full", views=VIEWS_34, tile=(380, 560), example="SdwReelWind")
    out["sdw_yuno_upgrade"] = scene("yuno", "upgrade", ages=(2, 6, 10, 16, 40), views=VIEWS_34, tile=(260, 440))
    out["sdw_yuno_bow"] = scene("yuno", "full", views=VIEWS4_W[:2] + VIEWS4_W[3:], tile=(480, 560))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

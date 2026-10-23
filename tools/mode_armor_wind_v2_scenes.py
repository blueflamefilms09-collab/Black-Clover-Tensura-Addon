"""Writes tools/render_preview/scenes.d/mode_armor_wind_v2.json: the scenes of the Wind Spirit Dive overlay v2 (Yuno), BEFORE (classic cards) and AFTER (the layered coat, crown and leaf wings).

    python3 -B tools/mode_armor_wind_v2_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "mwv2_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/mode_armor_wind_v2_sheets.py

The scenes run the REAL ModeArmorLayer (examples/MwV2Common.java installs it as a painter of the carrier aura MUCUS) in the harness, so the overlay is drawn with the same pose and buffers as the
Spirit Dive aura painters of the combined scenes. style = element + 4 * stage + 16 * grade: WIND 0, FULL 1 -> 4, YUNO 1 -> 16: 20.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "mode_armor_wind_v2.json")
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java", "book/SpiritDiveWind4Rules.java", "book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java",
        "book/ModeArmorWindV2Rules.java", "book/ModeArmorWindV2Mesh.java", "client/mode/ModeArmorLayer.java", "client/mode/ModeArmorWindV2.java"]
Y = 1.0


def views(dist=5.2, target=(0, Y, 0.1), names=None):
    base = [("front", 0, 6), ("front 3/4", 32, 8), ("back 3/4", 150, 12), ("side", 90, 6), ("back", 180, 8)]
    return [{"name": n, "yaw": yaw, "pitch": p, "target": list(target), "dist": dist} for (n, yaw, p) in base if names is None or n in names]


def scene(example, ages=(50,), vs=None, tile=(420, 620), combined=False, limb=None, **extra):
    s = {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "exampleFiles": ["MwV2Common.java"], "examples": [example], "total": 120, "ages": list(ages),
         "tile": list(tile), "views": vs or views()}
    if combined:
        s["style"] = 20
        s["alsoAuras"] = ["MUCUS"]
    else:
        s["aura"] = "MUCUS"
    if limb:
        s["limbSwing"] = limb[0]
        s["limbSwingAmount"] = limb[1]
    s.update(extra)
    return s


def main():
    out = {"_comment": "Wind Spirit Dive overlay v2 (Yuno): written by tools/mode_armor_wind_v2_scenes.py (do not edit by hand)."}
    for tag, ex in (("after", "MwV2New"), ("before", "MwV2Classic")):
        out["mwv2_%s_full" % tag] = scene(ex, vs=views(3.6, (0, 1.05, 0.15)), tile=(480, 700))
        out["mwv2_%s_collar" % tag] = scene(ex, vs=views(1.7, (0, 1.5, 0.1), ("front", "front 3/4", "side", "back")), tile=(420, 420))
        out["mwv2_%s_crown" % tag] = scene(ex, vs=views(1.8, (0, 2.3, 0.0), ("front", "front 3/4", "side", "back")), tile=(420, 420))
        out["mwv2_%s_wings" % tag] = scene(ex, vs=views(3.2, (0, 1.75, 0.5), ("back", "back 3/4", "front 3/4", "side")), tile=(480, 520))
        out["mwv2_%s_run" % tag] = scene(ex, vs=views(3.6, (0, 1.05, 0.15), ("front 3/4", "back 3/4", "side")), tile=(480, 700), limb=(3.0, 0.9))
        out["mwv2_%s_entry" % tag] = scene(ex, ages=(1, 4, 8, 11, 14, 18, 22, 40), vs=views(4.4, (0, 1.05, 0.15), ("front 3/4",)), tile=(300, 520))
        out["mwv2_%s_dive" % tag] = scene(ex, combined=True, vs=views(5.2, (0, Y, 0.1), ("front", "front 3/4", "back 3/4", "side")), tile=(420, 620))
        out["mwv2_%s_dive_entry" % tag] = scene(ex, combined=True, ages=(2, 6, 10, 14, 18, 22, 30, 50), vs=views(4.6, (0, Y, 0.1), ("front 3/4",)), tile=(300, 520))
        out["mwv2_%s_dive_big" % tag] = scene(ex, combined=True, vs=views(3.6, (0, 1.05, 0.15), ("front 3/4", "back 3/4")), tile=(560, 760))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

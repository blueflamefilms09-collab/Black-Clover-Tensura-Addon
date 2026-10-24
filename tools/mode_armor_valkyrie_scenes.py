"""Writes tools/render_preview/scenes.d/mode_armor_valkyrie.json: the scenes of the Valkyrie armour v2 (Noelle's Valkyrie Dress), BEFORE (the old overlay, the setting CLASSIC) and AFTER (the player-fitted armour).

    python3 -B tools/mode_armor_valkyrie_scenes.py
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/render_preview/preview.py "vg2_*"
    RENDER_PREVIEW_BUILD=<dir> python3 -B tools/mode_armor_valkyrie_sheets.py            # writes docs/previews/vfx_remake/valkyrie/ingame_*.png

The scenes run the REAL ModeArmorLayer and the REAL ModeArmorValkyrieV2 (examples/Vg2Common.java installs the layer as a painter of the carrier aura MUCUS) in the harness: the same meshes, the frame plan under the
own pool, the wing flutter, the lance's attach and spin, the baked colours and passes as the game; only the world (other players, hands, camera) is a stub.
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "mode_armor_valkyrie.json")
REAL = ["book/SpiritDiveDjinnPieces.java", "book/SpiritDivePieces.java", "book/SpiritDiveTier.java", "book/SpiritDiveAnimeRules.java", "book/SpiritDiveAnimeGeometry.java",
        "book/SpiritDiveReelRules.java", "book/SpiritDiveReelGeometry.java", "book/SpiritDiveWindRules.java", "book/SpiritDiveWindGeometry.java",
        "book/SpiritDiveWind3Rules.java", "book/SpiritDiveWind3Geometry.java", "book/SpiritDiveWind3Mesh.java", "book/SpiritDiveWind4Rules.java", "book/SpiritDiveMergeRules.java", "book/SpiritDiveMergeMesh.java", "book/SpiritDiveMerge2Rules.java",
        "book/FuegoleonArmRules.java", "book/FuegoleonArmDjinnRules.java", "perf/PerfBudget.java", "client/aura/FuegoleonArmAura.java", "client/aura/FuegoleonArmFx.java",
        "client/aura/FuegoleonArmDjinn.java", "client/aura/FuegoleonArmDjinnFx.java", "client/aura/FuegoleonArmLook.java",
        "book/ModeArmorWindV2Rules.java", "book/ModeArmorWindV2Mesh.java", "client/mode/ModeArmorLayer.java", "client/mode/ModeArmorWindV2.java",
        "book/ModeArmorValkyrieRules.java", "book/ModeArmorValkyrieMesh.java", "book/ModeArmorValkyrieBudget.java", "book/ModeArmorValkyrieSpin.java", "client/mode/ModeArmorValkyrieV2.java"]
Y = 1.0


def views(dist=5.2, target=(0, Y, 0.1), names=None):
    base = [("front", 0, 6), ("front 3/4", 32, 8), ("back 3/4", 150, 12), ("side", 90, 6), ("back", 180, 8)]
    return [{"name": n, "yaw": yaw, "pitch": p, "target": list(target), "dist": dist} for (n, yaw, p) in base if names is None or n in names]


def scene(example, ages=(50,), vs=None, tile=(420, 620), limb=None, **extra):
    s = {"kind": "aura", "attr": "SpiritDive", "realFiles": list(REAL), "exampleFiles": ["Vg2Common.java"], "examples": [example], "total": 120, "ages": list(ages),
         "tile": list(tile), "views": vs or views(), "aura": "MUCUS"}
    if limb:
        s["limbSwing"] = limb[0]
        s["limbSwingAmount"] = limb[1]
    s.update(extra)
    return s


def main():
    out = {"_comment": "Valkyrie armour v2: written by tools/mode_armor_valkyrie_scenes.py (do not edit by hand)."}
    for tag, ex in (("before", "Vg2Old"), ("after", "Vg2New"), ("float", "Vg2Float"), ("nolance", "Vg2NoLance"), ("saint", "Vg2Saint"), ("mermaid", "Vg2Mermaid")):
        out["vg2_%s_full" % tag] = scene(ex, vs=views(4.4, (0, 0.75, 0.2)), tile=(480, 700))
        out["vg2_%s_wings" % tag] = scene(ex, vs=views(3.2, (0, 1.55, 0.5), ("back", "back 3/4", "front 3/4", "side")), tile=(480, 520))
        out["vg2_%s_upper" % tag] = scene(ex, vs=views(2.2, (0, 1.4, 0.1), ("front", "front 3/4", "side", "back 3/4")), tile=(420, 520))
        out["vg2_%s_run" % tag] = scene(ex, vs=views(4.4, (0, 0.75, 0.2), ("front 3/4", "back 3/4", "side")), tile=(480, 700), limb=(3.0, 0.9))
        out["vg2_%s_ages" % tag] = scene(ex, ages=(2, 6, 12, 20, 33, 50), vs=views(4.4, (0, 0.75, 0.2), ("front 3/4",)), tile=(300, 520))
    with open(OUT, "w", encoding="utf-8") as fh:
        json.dump(out, fh, indent=1)
        fh.write("\n")
    print(OUT, len(out) - 1, "scenes")


if __name__ == "__main__":
    main()

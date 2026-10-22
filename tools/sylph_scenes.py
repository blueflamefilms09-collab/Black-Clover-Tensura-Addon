"""Writes tools/render_preview/scenes.d/sylph.json (the preview scenes of the Sylph, docs/magic/sylph.md) so they can be redone:

    python3 -B tools/sylph_scenes.py

The scenes are drawn by the real client.prop.SylphDraw through the example SylphProp: param = placed * 1000 + form * 100 + lod * 10 + clip (form 0 small, 1 powered; clip 0 idle, 1 laugh, 2 cast,
3 hover, 4 powered_cast, 5 shout; placed 1 puts the Sylph where the game puts her next to a standing player).
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
REAL = ["client/prop/SylphDraw.java", "client/prop/FlatLit.java", "sylph/SylphLod.java"]


def sc(param, views, ages=(0,), tile=(520, 560), figure=False, floor=False):
    return {"kind": "prop", "example": "SylphProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "floor": floor, "ages": list(ages),
            "tile": list(tile), "views": views, "light": "0xF000F0"}


def v(name, yaw, pitch, tx, ty, tz, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": [tx, ty, tz], "dist": dist}


def main():
    s = {"_comment": "Sylph (docs/magic/sylph.md): the small fairy and the powered form drawn by the real client.prop.SylphDraw (example SylphProp; param = placed*1000 + form*100 + lod*10 + clip: "
                     "form 0 small, 1 powered; clip 0 idle, 1 laugh, 2 cast, 3 hover, 4 powered_cast, 5 shout). Regenerate the models with tools/gen_sylph_geo.py and the scenes with tools/sylph_scenes.py."}
    s["sylph_small"] = sc(0, [v("front", 0, 6, 0, 0, 0, 1.5), v("3/4", 35, 8, 0, 0, 0, 1.5), v("side", 90, 6, 0, 0, 0, 1.5), v("back", 160, 10, 0, 0, 0, 1.5)], tile=(420, 460))
    s["sylph_small_rest"] = sc(6, [v("rest front", 0, 4, 0, 0, 0, 1.6), v("rest side", 90, 4, 0, 0, 0, 1.6)], tile=(380, 460))
    s["sylph_small_face"] = sc(0, [v("face front", 0, 2, 0, 0.18, 0, 0.55), v("face 3/4", 30, 4, 0, 0.18, 0, 0.55)], tile=(520, 520))
    s["sylph_small_laugh"] = sc(1, [v("laugh 3/4", 25, 6, 0, 0.04, 0, 1.1)], ages=(0, 6, 12), tile=(380, 420))
    s["sylph_small_cast"] = sc(2, [v("cast 3/4", -30, 6, 0, 0.0, 0, 1.4), v("cast side", -90, 6, 0, 0, 0, 1.4)], ages=(4, 12), tile=(420, 460))
    s["sylph_small_lods"] = sc(0, [v("full", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_small_lod1"] = sc(10, [v("lod1", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_small_lod2"] = sc(20, [v("lod2", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_scale"] = sc(1000, [v("next to a player", 25, 6, -0.25, 1.4, 0, 3.4)], figure=True, tile=(560, 560))
    s["sylph_small_scale_close"] = sc(1000, [v("head and shoulder", 20, 4, -0.3, 1.8, 0, 1.6)], figure=True, tile=(520, 520))
    s["sylph_powered"] = sc(103, [v("front", 0, 6, 0, 0, 0, 5.2), v("3/4", 35, 8, 0, 0, 0, 5.2), v("side", 90, 6, 0, 0, 0, 5.2), v("back", 160, 10, 0, 0, 0, 5.2)], tile=(420, 520))
    s["sylph_powered_face"] = sc(103, [v("face front", 0, 2, 0, 0.42, 0, 1.6), v("face 3/4", 30, 4, 0, 0.42, 0, 1.6)], tile=(520, 520))
    s["sylph_powered_cast"] = sc(104, [v("cast 3/4", -30, 6, 0, 0, 0, 5.2)], ages=(4, 12), tile=(420, 520))
    s["sylph_powered_lod1"] = sc(113, [v("lod1", 30, 8, 0, 0, 0, 5.2)], tile=(360, 460))
    s["sylph_powered_lod2"] = sc(123, [v("lod2", 30, 8, 0, 0, 0, 5.2)], tile=(360, 460))
    s["sylph_powered_scale"] = sc(1103, [v("next to a player", 20, 8, 0, 2.2, -1.0, 8.0)], figure=True, tile=(560, 560))
    with open(os.path.join(HERE, "render_preview", "scenes.d", "sylph.json"), "w", newline="\n") as f:
        json.dump(s, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main()

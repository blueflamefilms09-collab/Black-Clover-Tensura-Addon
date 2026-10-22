"""Writes the render preview scenes of the Gnome (tools/render_preview/scenes.d/gnome.json): python3 -B tools/gnome_scenes.py

Two kinds: "geo" scenes draw one model file (the body only, for checking a pose), "prop" scenes draw the REAL helper the game uses (client/prop/GnomeDraw through the example GnomeProp:
body + glow-only model, the place the game puts each form relative to a standing player at the origin). The prop scenes' param = form * 1000 + lod * 100 + clip * 10 + glow
(form 0 companion, 1 full; clip index into GnomeProp.CLIPS; glow 0/1 draws the glow-only model).
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "gnome.json")


def V(n, y, p, t, d):
    return {"name": n, "yaw": y, "pitch": p, "target": t, "dist": d}


def geo(model, anim, ages, views, tile=(520, 560), figure=True):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "00000000", "ages": ages, "tile": list(tile), "floor": True,
            "figure": figure, "expect": [], "anim": anim, "views": views}


REAL = ["client/prop/GnomeDraw.java", "gnome/GnomeLod.java", "gnome/GnomeRules.java"]


def prop(param, ages, views, tile=(520, 560), figure=True):
    return {"kind": "prop", "example": "GnomeProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "ages": ages, "tile": list(tile), "views": views}


def main():
    s = {"_comment": "The Gnome (docs/magic/gnome.md): the full form and the companion form drawn by the real client/prop/GnomeDraw (example GnomeProp). Regenerate with python3 -B tools/gnome_scenes.py."}
    F = [0, 3.3, 4.8]                                  # the giant's middle, 4.8 blocks ahead of the player at the origin
    C = [-1.15, 0.75, -0.35]                           # the companion's middle
    s["gnome_dev_full"] = geo("gnome_full", "idle", [0], [V("front", 0, 6, [0, 3.4, 0], 17), V("3/4", 38, 10, [0, 3.4, 0], 17), V("side", 90, 5, [0, 3.4, 0], 17), V("back", 155, 12, [0, 3.4, 0], 17)])
    s["gnome_dev_comp"] = geo("gnome_companion", "idle", [0], [V("front", 0, 6, [0, 0.8, 0], 3.6), V("3/4", 38, 10, [0, 0.8, 0], 3.6), V("side", 90, 5, [0, 0.8, 0], 3.6), V("back", 155, 12, [0, 0.8, 0], 3.6)], tile=(420, 440))
    s["gnome_full"] = prop(1000, [30], [V("front", 0, 6, F, 17), V("3/4", 38, 10, F, 17), V("side", 90, 5, F, 17), V("back", 160, 12, F, 17)], tile=(540, 620), figure=True)
    s["gnome_full_close"] = prop(1000, [30], [V("face", 0, 4, [0, 5.3, 4.2], 3.6), V("chest circle", 25, 6, [0, 3.9, 4.2], 4.6), V("fist", 15, 14, [-1.8, 0.8, 4.4], 4.2)], tile=(520, 520), figure=False)
    s["gnome_companion"] = prop(0, [30], [V("front", 0, 6, C, 3.2), V("3/4", 35, 10, C, 3.2), V("side", 90, 5, C, 3.2), V("back", 160, 12, C, 3.2)], tile=(440, 480), figure=True)
    s["gnome_companion_close"] = prop(0, [30], [V("face", 0, 4, [-1.15, 1.15, -0.35], 1.3), V("3/4 close", 35, 8, [-1.15, 0.8, -0.35], 1.9)], tile=(520, 520), figure=False)
    s["gnome_scale"] = prop(2000, [30], [V("beside a player", 35, 7, [-0.6, 2.7, 2.4], 21)], tile=(640, 660), figure=True)
    s["gnome_full_rise"] = prop(1020, [0, 6, 11, 14, 17, 22], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_full_cast"] = prop(1030, [0, 5, 9, 12, 15, 20], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_full_sink"] = prop(1040, [0, 5, 10, 15, 19], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_companion_walk"] = prop(10, [0, 8, 16, 24], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=True)
    s["gnome_companion_cast"] = prop(30, [0, 5, 9, 12, 15, 20], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=True)
    for lod in (1, 2):
        s["gnome_full_lod%d" % lod] = prop(1000 + lod * 100, [30], [V("3/4", 38, 10, F, 17)], tile=(520, 560), figure=True)
        s["gnome_companion_lod%d" % lod] = prop(lod * 100, [30], [V("3/4", 35, 10, C, 3.2)], tile=(440, 480), figure=True)
    for clip, ages in (("rise", [0, 6, 11, 14, 17]), ("cast", [0, 5, 9, 12, 15]), ("punch", [0, 6, 10, 14]), ("walk", [0, 8, 16, 24]), ("sink", [0, 6, 12, 18]), ("stomp", [0, 5, 10, 14])):
        s["gnome_dev_full_" + clip] = geo("gnome_full", clip, ages, [V("3/4", 38, 10, [0, 3.4, 0], 17), V("side", 90, 6, [0, 3.4, 0], 17)], tile=(300, 420))
    json.dump(s, open(OUT, "w", newline="\n"), indent=1)
    print("wrote", OUT, len(s) - 1, "scenes")


if __name__ == "__main__":
    main()

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
    full_t = [0, 3.4, 0]
    comp_t = [0, 0.8, 0]
    s = {"_comment": "The Gnome (docs/magic/gnome.md): the full form and the companion form drawn by the real client/prop/GnomeDraw (example GnomeProp). Regenerate with python3 -B tools/gnome_scenes.py."}
    s["gnome_dev_full"] = geo("gnome_full", "idle", [0], [V("front", 0, 6, full_t, 17), V("3/4", 38, 10, full_t, 17), V("side", 90, 5, full_t, 17), V("back", 155, 12, full_t, 17)])
    s["gnome_dev_comp"] = geo("gnome_companion", "idle", [0], [V("front", 0, 6, comp_t, 3.6), V("3/4", 38, 10, comp_t, 3.6), V("side", 90, 5, comp_t, 3.6), V("back", 155, 12, comp_t, 3.6)], tile=(420, 440))
    for clip, ages in (("rise", [0, 6, 11, 14, 17]), ("cast", [0, 5, 9, 12, 15]), ("punch", [0, 6, 10, 14]), ("walk", [0, 8, 16, 24]), ("sink", [0, 6, 12, 18])):
        s["gnome_dev_full_" + clip] = geo("gnome_full", clip, ages, [V("3/4", 38, 10, full_t, 17), V("side", 90, 6, full_t, 17)], tile=(300, 420))
    json.dump(s, open(OUT, "w", newline="\n"), indent=1)
    print("wrote", OUT, len(s) - 1, "scenes")


if __name__ == "__main__":
    main()

"""Writes the render preview scenes of the Gnome (tools/render_preview/scenes.d/gnome.json): python3 -B tools/gnome_scenes.py

Two kinds: "geo" scenes draw one model file (the body only, for checking a pose), "prop" scenes draw the REAL helper the game uses (client/prop/GnomeDraw through the example GnomeProp:
body + glow-only model + the face plate, the place the game puts each form relative to a standing player at the origin). The prop scenes' param = form * 100000 + lod * 10000 + clip * 100 + face
(form 0 companion, 1 full, 2 both; clip index into GnomeDraw.CLIPS; face index into GnomeRules.FACE_*), see pp().
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


def pp(form, lod=0, clip=0, face=0):
    return form * 100000 + lod * 10000 + clip * 100 + face


CLIP = {"idle": 0, "walk": 1, "rise": 2, "cast": 3, "sink": 4, "punch": 5, "stomp": 6, "slam": 7, "shrug": 8, "guard": 9, "pound": 10, "doze": 11, "kneel": 12}
FACE = {"calm": 0, "amused": 1, "angry": 2, "sleepy": 3, "surprised": 4, "stern": 5}
REAL = ["client/prop/GnomeDraw.java", "gnome/GnomeLod.java", "gnome/GnomeRules.java"]


def prop(param, ages, views, tile=(520, 560), figure=True):
    return {"kind": "prop", "example": "GnomeProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "ages": ages, "tile": list(tile), "views": views}


def main():
    s = {"_comment": "The Gnome (docs/magic/gnome.md): the full form and the companion form drawn by the real client/prop/GnomeDraw (example GnomeProp). Regenerate with python3 -B tools/gnome_scenes.py."}
    F = [0, 3.3, 4.8]                                  # the giant's middle, 4.8 blocks ahead of the player at the origin
    C = [-1.15, 0.75, -0.35]                           # the companion's middle
    s["gnome_dev_full"] = geo("gnome_full", "idle", [0], [V("front", 0, 6, [0, 3.4, 0], 17), V("3/4", 38, 10, [0, 3.4, 0], 17), V("side", 90, 5, [0, 3.4, 0], 17), V("back", 155, 12, [0, 3.4, 0], 17)])
    s["gnome_dev_comp"] = geo("gnome_companion", "idle", [0], [V("front", 0, 6, [0, 0.8, 0], 3.6), V("3/4", 38, 10, [0, 0.8, 0], 3.6), V("side", 90, 5, [0, 0.8, 0], 3.6), V("back", 155, 12, [0, 0.8, 0], 3.6)], tile=(420, 440))
    s["gnome_full"] = prop(pp(1), [30], [V("front", 0, 6, F, 17), V("3/4", 38, 10, F, 17), V("side", 90, 5, F, 17), V("back", 160, 12, F, 17)], tile=(540, 620), figure=True)
    s["gnome_full_close"] = prop(pp(1), [30], [V("face", 0, 4, [0, 5.3, 4.2], 3.6), V("chest circle", 25, 6, [0, 3.9, 4.2], 4.6), V("fist", 15, 14, [-1.8, 0.8, 4.4], 4.2)], tile=(520, 520), figure=False)
    s["gnome_companion"] = prop(pp(0), [30], [V("front", 0, 6, C, 3.2), V("3/4", 35, 10, C, 3.2), V("side", 90, 5, C, 3.2), V("back", 160, 12, C, 3.2)], tile=(440, 480), figure=True)
    s["gnome_companion_solo"] = prop(pp(0), [30], [V("front", 0, 6, C, 3.2), V("3/4", 35, 10, C, 3.2), V("side", 90, 5, C, 3.2), V("back", 160, 12, C, 3.2)], tile=(440, 480), figure=False)
    s["gnome_companion_close"] = prop(pp(0), [30], [V("face", 0, 4, [-1.15, 1.15, -0.35], 1.3), V("3/4 close", 35, 8, [-1.15, 0.8, -0.35], 1.9)], tile=(520, 520), figure=False)
    s["gnome_scale"] = prop(pp(2), [30], [V("side: player, companion, giant", 90, 5, [-0.3, 2.9, 2.4], 20), V("3/4 from the front", 28, 8, [-0.3, 2.9, 2.4], 20)], tile=(640, 520), figure=True)
    s["gnome_full_rise"] = prop(pp(1, 0, CLIP["rise"]), [0, 6, 11, 14, 17, 22], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_full_cast"] = prop(pp(1, 0, CLIP["cast"], FACE["angry"]), [0, 5, 9, 12, 15, 20], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_full_sink"] = prop(pp(1, 0, CLIP["sink"]), [0, 5, 10, 15, 19], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_companion_walk"] = prop(pp(0, 0, CLIP["walk"]), [0, 8, 16, 24], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=True)
    s["gnome_companion_cast"] = prop(pp(0, 0, CLIP["cast"], FACE["angry"]), [0, 5, 9, 12, 15, 20], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=True)
    s["gnome_full_punch"] = prop(pp(1, 0, CLIP["punch"], FACE["stern"]), [0, 5, 10, 13, 17], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_full_stomp"] = prop(pp(1, 0, CLIP["stomp"]), [0, 4, 8, 10, 14], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=True)
    s["gnome_companion_punch"] = prop(pp(0, 0, CLIP["punch"], FACE["stern"]), [0, 5, 10, 13, 17], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=True)
    for clip, ages in (("guard", [0, 20, 40]), ("shrug", [0, 2, 6, 11, 16]), ("pound", [0, 5, 8, 11, 14, 17, 20]), ("doze", [0, 40, 80]), ("kneel", [0, 12, 22, 30, 40]), ("slam", [0, 6, 10, 13, 17])):
        s["gnome_dev_comp_" + clip] = geo("gnome_companion", clip, ages, [V("3/4", 35, 10, [0, 0.8, 0], 3.6), V("side", 90, 5, [0, 0.8, 0], 3.6)], tile=(300, 340), figure=False)
    # the poses and the faces (docs/magic/gnome.md section 7): the companion in each pose at its telling tick, with the face its situation gives it; then the giant; then the six faces close up
    for name, tick, face in (("kneel", 14, "calm"), ("slam", 12, "stern"), ("guard", 30, "stern"), ("stomp", 10, "surprised"), ("shrug", 7, "stern"), ("pound", 12, "amused"), ("doze", 40, "sleepy"),
                             ("cast", 13, "angry"), ("idle", 30, "calm")):
        s["gnome_pose_comp_" + name] = prop(pp(0, 0, CLIP[name], FACE[face]), [tick], [V("3/4", 35, 10, C, 3.2)], tile=(330, 400), figure=False)
    for name, tick, face in (("slam", 12, "stern"), ("guard", 30, "stern"), ("shrug", 7, "stern"), ("pound", 12, "amused")):
        s["gnome_pose_full_" + name] = prop(pp(1, 0, CLIP[name], FACE[face]), [tick], [V("3/4", 38, 8, F, 15)], tile=(300, 460), figure=False)
    for name in FACE:
        s["gnome_face_comp_" + name] = prop(pp(0, 0, 0, FACE[name]), [30], [V("face", 0, 4, [-1.15, 1.15, -0.35], 1.3)], tile=(300, 300), figure=False)
        s["gnome_face_full_" + name] = prop(pp(1, 0, 0, FACE[name]), [30], [V("face", 0, 4, [0, 5.3, 4.2], 3.6)], tile=(300, 300), figure=False)
    for lod in (1, 2):
        s["gnome_full_lod%d" % lod] = prop(pp(1, lod), [30], [V("3/4", 38, 10, F, 17)], tile=(520, 560), figure=True)
        s["gnome_companion_lod%d" % lod] = prop(pp(0, lod), [30], [V("3/4", 35, 10, C, 3.2)], tile=(440, 480), figure=True)
    for clip, ages in (("rise", [0, 6, 11, 14, 17]), ("cast", [0, 5, 9, 12, 15]), ("punch", [0, 6, 10, 14]), ("walk", [0, 8, 16, 24]), ("sink", [0, 6, 12, 18]), ("stomp", [0, 5, 10, 14])):
        s["gnome_dev_full_" + clip] = geo("gnome_full", clip, ages, [V("3/4", 38, 10, [0, 3.4, 0], 17), V("side", 90, 6, [0, 3.4, 0], 17)], tile=(300, 420))
    json.dump(s, open(OUT, "w", newline="\n"), indent=1)
    print("wrote", OUT, len(s) - 1, "scenes")


if __name__ == "__main__":
    main()

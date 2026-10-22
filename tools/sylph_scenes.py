"""Writes tools/render_preview/scenes.d/sylph.json (the preview scenes of the Sylph, docs/magic/sylph.md) so they can be redone:

    python3 -B tools/sylph_scenes.py

The scenes are drawn by the real client.prop.SylphDraw through the example SylphProp (tools/render_preview/examples/SylphProp.java): param = placed * 10000 + form * 1000 + lod * 100 + clip, where the
clip is the index into CLIPS below (the same list the example has; "rest" is no clip: the rest pose); form 0 small, 1 powered; placed 1 puts the Sylph where the game puts her next to a standing player.
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
REAL = ["client/prop/SylphDraw.java", "client/prop/FlatLit.java", "client/prop/SheenUv.java", "sylph/SylphLod.java"]
CLIPS = ["idle", "laugh", "cast", "hover", "powered_cast", "shout", "rest", "point", "fly", "swing", "wave", "crossed", "pout", "victory", "sleepy", "startled", "hurt", "worry",
         "cry", "cheer", "battle", "fury", "triumph"]
SMALL_POSES = ["idle", "laugh", "point", "cast", "fly", "swing", "wave", "crossed", "pout", "victory", "sleepy", "startled", "hurt", "worry", "cry", "cheer"]
POWERED_POSES = ["hover", "powered_cast", "shout", "battle", "fury", "triumph"]
# the face each clip shows, for the faces sheet (face -> (clip, form))
FACES = [("laugh", "laugh", 0), ("smile", "point", 0), ("smug", "crossed", 0), ("angry", "hurt", 0), ("sad", "cry", 0), ("surprised", "startled", 0), ("sleepy", "sleepy", 0), ("pout", "pout", 0),
         ("stern", "hover", 1), ("shout", "shout", 1)]


def P(form, lod, clip, placed=0):
    return placed * 10000 + form * 1000 + lod * 100 + CLIPS.index(clip)


def sc(param, views, ages=(0,), tile=(520, 560), figure=False, floor=False):
    return {"kind": "prop", "example": "SylphProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "floor": floor, "ages": list(ages),
            "tile": list(tile), "views": views, "light": "0xF000F0"}


def geo(model, views, anim="rest", layer="TRANSLUCENT", tile=(520, 520)):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": layer, "argb": "FFFFFFFF", "glow": "00000000", "ages": [0], "tile": list(tile), "floor": False, "figure": False, "expect": [],
            "anim": anim, "views": views}


def v(name, yaw, pitch, tx, ty, tz, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": [tx, ty, tz], "dist": dist}


def main():
    s = {"_comment": "Sylph (docs/magic/sylph.md): the small fairy and the powered form drawn by the real client.prop.SylphDraw (example SylphProp; param = placed*10000 + form*1000 + lod*100 + clip index, "
                     "see tools/sylph_scenes.py). Regenerate the models with tools/gen_sylph_geo.py and the scenes with tools/sylph_scenes.py."}
    s["sylph_small"] = sc(P(0, 0, "idle"), [v("front", 0, 6, 0, 0, 0, 1.5), v("3/4", 35, 8, 0, 0, 0, 1.5), v("side", 90, 6, 0, 0, 0, 1.5), v("back", 160, 10, 0, 0, 0, 1.5)], tile=(420, 460))
    s["sylph_small_rest"] = sc(P(0, 0, "rest"), [v("rest front", 0, 4, 0, 0, 0, 1.6), v("rest side", 90, 4, 0, 0, 0, 1.6)], tile=(380, 460))
    s["sylph_small_point"] = sc(P(0, 0, "point"), [v("point 3/4", 20, 6, 0, 0, 0, 1.5), v("point front", -10, 4, 0, 0, 0, 1.5)], ages=(0, 8), tile=(420, 460))
    s["sylph_small_fly"] = sc(P(0, 0, "fly"), [v("fly side", 90, 6, 0, 0, 0, 1.5)], tile=(420, 420))
    s["sylph_wings_small"] = sc(P(0, 0, "idle"), [v("front", 0, 4, 0, 0.12, 0, 1.45), v("back", 180, 4, 0, 0.12, 0, 1.45), v("3/4 back", 150, 10, 0, 0.12, 0, 1.45), v("side", 90, 4, 0, 0.12, 0, 1.45)],
                                ages=(0, 60, 120), tile=(380, 440))
    s["sylph_wings_small_cast"] = sc(P(0, 0, "cast"), [v("cast back", 160, 6, 0, 0.12, 0, 1.45), v("cast front", -20, 4, 0, 0.12, 0, 1.45)], ages=(3, 9), tile=(380, 440))
    s["sylph_wings_powered"] = sc(P(1, 0, "hover"), [v("front", 0, 4, 0, 0.35, 0, 4.6), v("back", 180, 4, 0, 0.35, 0, 4.6), v("3/4 back", 150, 10, 0, 0.35, 0, 4.6), v("side", 90, 4, 0, 0.35, 0, 4.6)],
                                  ages=(0, 60, 120), tile=(380, 440))
    s["cmp_wings_119"] = sc(P(0, 0, "idle"), [v("small wings, seated (the first picture)", 28, 5, 0, 0.0, 0, 1.25)], ages=(6,), tile=(560, 560))
    s["cmp_wings_small"] = sc(P(0, 0, "point"), [v("small wings, from the front (the second picture)", -12, 3, 0, 0.16, 0, 1.0)], ages=(8,), tile=(560, 640))
    s["cmp_wings_powered"] = sc(P(1, 0, "hover"), [v("powered wings, from the front (the third picture)", 0, 3, 0, 0.42, 0, 3.6)], ages=(6,), tile=(560, 640))
    s["wings_close_small"] = sc(P(0, 0, "point"), [v("back, close", 180, 3, 0, 0.2, 0, 1.0), v("front, close", 0, 3, 0, 0.2, 0, 1.0)], ages=(0, 70), tile=(560, 640))
    s["wings_close_powered"] = sc(P(1, 0, "hover"), [v("back, close", 180, 3, 0, 0.55, 0, 3.4), v("front, close", 0, 3, 0, 0.55, 0, 3.4)], ages=(0, 70), tile=(560, 640))
    s["sylph_wings_lods"] = sc(P(0, 0, "idle"), [v("full", 160, 6, 0, 0.12, 0, 1.45)], tile=(300, 380))
    s["sylph_wings_lod1"] = sc(P(0, 1, "idle"), [v("lod1", 160, 6, 0, 0.12, 0, 1.45)], tile=(300, 380))
    s["sylph_wings_lod2"] = sc(P(0, 2, "idle"), [v("lod2", 160, 6, 0, 0.12, 0, 1.45)], tile=(300, 380))
    s["sylph_small_wings"] = geo("sylph_small_wings", [v("wings back", 180, 0, 0, 1.2, 0, 3.4), v("wings front", 0, 0, 0, 1.2, 0, 3.4)], anim="idle")
    s["cmp_small_laugh"] = sc(P(0, 0, "idle"), [v("small, idle (the first picture)", 28, 5, 0, 0.0, 0, 1.25)], ages=(6,), tile=(560, 560))
    s["cmp_small_point"] = sc(P(0, 0, "point"), [v("small, pointing (the second picture)", -12, 4, 0, 0.0, 0, 1.25)], ages=(8,), tile=(560, 560))
    s["cmp_powered"] = sc(P(1, 0, "hover"), [v("powered (the third picture)", 0, 4, 0, 0.35, 0, 4.2)], ages=(6,), tile=(560, 660))
    s["sylph_small_face"] = sc(P(0, 0, "idle"), [v("face front", 0, 2, 0, 0.18, 0, 0.55), v("face 3/4", 30, 4, 0, 0.18, 0, 0.55)], tile=(520, 520))
    s["sylph_small_laugh"] = sc(P(0, 0, "laugh"), [v("laugh 3/4", 25, 6, 0, 0.04, 0, 1.1)], ages=(0, 6, 12), tile=(380, 420))
    s["sylph_small_cast"] = sc(P(0, 0, "cast"), [v("cast 3/4", -30, 6, 0, 0.0, 0, 1.4), v("cast side", -90, 6, 0, 0, 0, 1.4)], ages=(4, 12), tile=(420, 460))
    s["sylph_small_lods"] = sc(P(0, 0, "idle"), [v("full", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_small_lod1"] = sc(P(0, 1, "idle"), [v("lod1", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_small_lod2"] = sc(P(0, 2, "idle"), [v("lod2", 30, 8, 0, 0, 0, 1.5)], tile=(360, 420))
    s["sylph_scale"] = sc(P(0, 0, "idle", 1), [v("next to a player", 25, 6, -0.25, 1.4, 0, 3.4)], figure=True, tile=(560, 560))
    s["sylph_small_scale_close"] = sc(P(0, 0, "idle", 1), [v("head and shoulder", 20, 4, -0.3, 1.8, 0, 1.6)], figure=True, tile=(520, 520))
    s["sylph_powered"] = sc(P(1, 0, "hover"), [v("front", 0, 6, 0, 0, 0, 5.2), v("3/4", 35, 8, 0, 0, 0, 5.2), v("side", 90, 6, 0, 0, 0, 5.2), v("back", 160, 10, 0, 0, 0, 5.2)], tile=(420, 520))
    s["sylph_powered_face"] = sc(P(1, 0, "hover"), [v("face front", 0, 2, 0, 0.42, 0, 1.6), v("face 3/4", 30, 4, 0, 0.42, 0, 1.6)], tile=(520, 520))
    s["sylph_powered_cast"] = sc(P(1, 0, "powered_cast"), [v("cast 3/4", -30, 6, 0, 0, 0, 5.2)], ages=(4, 12), tile=(420, 520))
    s["sylph_powered_lod1"] = sc(P(1, 1, "hover"), [v("lod1", 30, 8, 0, 0, 0, 5.2)], tile=(360, 460))
    s["sylph_powered_lod2"] = sc(P(1, 2, "hover"), [v("lod2", 30, 8, 0, 0, 0, 5.2)], tile=(360, 460))
    s["sylph_powered_scale"] = sc(P(1, 0, "hover", 1), [v("next to a player", 20, 8, 0, 2.2, -1.0, 8.0)], figure=True, tile=(560, 560))
    # the pose sheet: one scene per clip (a front 3/4 view at a moment inside the loop), and the face close-ups
    for clip in SMALL_POSES:
        s["pose_small_" + clip] = sc(P(0, 0, clip), [v(clip, 24, 5, 0, 0.0, 0, 1.25)], ages=(5,), tile=(360, 400))
    for clip in POWERED_POSES:
        s["pose_powered_" + clip] = sc(P(1, 0, clip), [v(clip, 24, 5, 0, 0.3, 0, 4.0)], ages=(5,), tile=(360, 400))
    for face, clip, form in FACES:
        s["face_" + face] = sc(P(form, 0, clip), [v(face, 0, 2, 0, 0.2 if form == 0 else 0.42, 0, 0.62 if form == 0 else 1.7)], ages=(5,), tile=(260, 260))
    with open(os.path.join(HERE, "render_preview", "scenes.d", "sylph.json"), "w", newline="\n") as f:
        json.dump(s, f, indent=1)
        f.write("\n")


if __name__ == "__main__":
    main()

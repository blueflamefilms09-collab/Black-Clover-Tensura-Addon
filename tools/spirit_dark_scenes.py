"""Writes tools/render_preview/scenes.d/spirit_dark.json (the preview scenes of the Spirit of Darkness; run it after editing the views below):

    python3 -B tools/spirit_dark_scenes.py
    python3 -B tools/render_preview/preview.py sd_powered_game sd_companion_game ...
    python3 -B tools/spirit_dark_sheets.py        (after rendering the sdp_* and sdf_* scenes: the pose sheet and the face sheet)

geo scenes draw one model of the Spirit alone (colour pass); the prop scenes draw the REAL helper the game uses (client/prop/SpiritDarkDraw) in the place the game puts the form, next to a standing
figure. Prop scene param = form * 10000 + lod * 1000 + clip * 10 + face (see tools/render_preview/examples/SpiritDarkProp.java).
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))

CLIPS = ["idle", "flap", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "vanish", "appear", "farewell"]
MOODS = ["smug", "grin", "angry", "bored", "surprised", "sinister", "amused"]
POWERED_POSES = ["idle", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "appear", "farewell"]
COMPANION_POSES = ["idle", "bow", "smug", "cast", "feast", "recoil", "cackle", "battle", "appear", "farewell"]
POSE_AGE = {"idle": 40, "bow": 14, "smug": 30, "cast": 12, "feast": 12, "recoil": 4, "cackle": 20, "battle": 20, "appear": 4, "farewell": 18, "vanish": 4, "flap": 4}


def geo(model, anim, views, ages=(0,), tile=(560, 440), floor=True, figure=False, layer="CUTOUT"):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": layer, "argb": "FFFFFFFF", "glow": "00000000", "ages": list(ages), "tile": list(tile), "floor": floor,
            "figure": figure, "expect": [], "anim": anim, "views": views}


def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": target, "dist": dist}


REAL = ["client/prop/SpiritDarkDraw.java", "spiritdark/SpiritDarkLod.java", "spiritdark/SpiritDarkRules.java", "spiritdark/DarkMoodRules.java"]


def prop(param, views, ages=(20,), tile=(520, 520), figure=True):
    return {"kind": "prop", "example": "SpiritDarkProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "ages": list(ages), "tile": list(tile), "views": views}


def code(form, lod, clip, face):
    return form * 10000 + lod * 1000 + CLIPS.index(clip) * 10 + MOODS.index(face)


PT = [-0.7, 1.5, -1.9]                      # the powered form's middle when the owner stands at the origin facing +z
CT = [0.62, 1.95, -0.18]                    # the companion's middle


def main():
    sc = {}
    sc["sd_powered_body"] = geo("spirit_dark_powered", "idle", [view("front 3/4", 30, 10, [0, 1.3, 0], 6.0), view("side", 90, 6, [0, 1.3, 0], 6.0), view("back 3/4", 150, 12, [0, 1.3, 0], 6.0)])
    sc["sd_companion_body"] = geo("spirit_dark_companion", "idle", [view("front 3/4", 30, 10, [0, 0.6, 0], 2.6), view("side", 90, 6, [0, 0.6, 0], 2.6), view("back 3/4", 150, 12, [0, 0.6, 0], 2.8)])
    sc["sd_powered_game"] = prop(code(1, 0, "idle", "smug"), [view("front 3/4, from beside the owner", 25, 14, PT, 8.0), view("behind the owner", 160, 12, PT, 8.0), view("side", 90, 6, [0, 1.4, -1.0], 8.0)], ages=(30,), tile=(560, 520))
    sc["sd_powered_battle"] = prop(code(1, 0, "battle", "angry"), [view("battle stance", 25, 12, PT, 7.5)], ages=(20,), tile=(520, 500))
    sc["sd_powered_face"] = prop(code(1, 0, "idle", "smug"), [view("face and chest", 12, 6, [-0.7, 2.1, -1.9], 3.4), view("wing and shoulder", 70, 14, [-0.7, 2.2, -1.9], 4.6)], ages=(30,), tile=(560, 560), figure=False)
    sc["sd_powered_lod"] = prop(code(1, 1, "battle", "angry"), [view("lod 1", 25, 12, PT, 8.0)], ages=(30,), tile=(420, 460))
    sc["sd_powered_lod2"] = prop(code(1, 2, "battle", "angry"), [view("lod 2", 25, 12, PT, 8.0)], ages=(30,), tile=(420, 460))
    sc["sd_companion_game"] = prop(code(0, 0, "idle", "smug"), [view("shoulder 3/4", 30, 8, [0, 1.7, 0.0], 3.2), view("beside", 90, 6, [0, 1.6, 0.0], 3.6), view("behind", 160, 10, [0, 1.7, 0.0], 3.4)], ages=(30,), tile=(520, 480))
    sc["sd_companion_lod"] = prop(code(0, 1, "idle", "smug"), [view("lod 1", 30, 8, [0, 1.7, 0.0], 3.0)], ages=(30,), tile=(400, 400))
    sc["sd_companion_lod2"] = prop(code(0, 2, "idle", "smug"), [view("lod 2", 30, 8, [0, 1.7, 0.0], 3.0)], ages=(30,), tile=(400, 400))
    # the sheets: one scene per pose and per mood, one view each (tools/spirit_dark_sheets.py joins them)
    for c in POWERED_POSES:
        sc["sdp_powered_" + c] = prop(code(1, 0, c, "sinister" if c in ("idle", "battle") else "smug"), [view(c, 18, 8, [-0.7, 1.7, -1.9], 4.6)], ages=(POSE_AGE[c],), tile=(330, 400), figure=False)
    for c in COMPANION_POSES:
        sc["sdp_companion_" + c] = prop(code(0, 0, c, "smug"), [view(c, 25, 8, CT, 2.4)], ages=(POSE_AGE[c],), tile=(300, 300), figure=False)
    for m in MOODS:
        sc["sdf_powered_" + m] = prop(code(1, 0, "idle", m), [view(m, 8, 4, [-0.7, 2.3, -1.9], 1.9)], ages=(10,), tile=(300, 280), figure=False)
        sc["sdf_companion_" + m] = prop(code(0, 0, "idle", m), [view(m, 8, 4, [0.62, 1.97, -0.18], 1.05)], ages=(10,), tile=(300, 280), figure=False)
    json.dump(sc, open(os.path.join(HERE, "render_preview", "scenes.d", "spirit_dark.json"), "w"), indent=1)


if __name__ == "__main__":
    main()

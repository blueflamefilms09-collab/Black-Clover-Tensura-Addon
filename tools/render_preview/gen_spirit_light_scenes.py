#!/usr/bin/env python3
"""Writes the preview scenes of the Spirit of Light (docs/magic/spirit_light.md) into tools/render_preview/scenes.d/spirit_light_*.json:

    spirit_light_companion / spirit_light_powered      the two models at rest (idle), four views each
    spirit_light_pose_<form>_<pose>                    one scene per pose (a few moments of its clip), the sheet docs/previews/spirit_light/spirit_light_poses.png
    spirit_light_face_<form>_<face>                    one scene per face (the head close), the sheet docs/previews/spirit_light/spirit_light_faces.png
    spirit_light_lod_<form>_<lod>                      the three levels of detail side by side in one scene each

The model scenes use the colour models only (kind geo); the scenes of the real drawing helper (colour model plus the additive glow model, the beam and the pulse ring) are
spirit_light_props.json (example SpiritLightProp), written by the same script.
    python3 -B tools/render_preview/gen_spirit_light_scenes.py
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "scenes.d")
sys.path.insert(0, os.path.join(HERE, ".."))
import gen_spirit_light_anim as AN  # noqa: E402


def geo(model, anim, ages, views, tile=(420, 420), floor=True):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "00000000", "ages": ages, "tile": list(tile), "floor": floor, "figure": False, "expect": [],
            "anim": anim, "views": views}


def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


CUB_TARGET, SER_TARGET = (0, 1.5, -0.2), (0, 2.9, 0)


def main():
    scenes = {}
    comp = {"spirit_light_companion": geo("spiritlight_cub", "idle", [0], [
        view("front 3/4", 30, 12, CUB_TARGET, 6.2), view("head close", 15, 8, (0, 2.1, -0.9), 3.6), view("side", 90, 6, CUB_TARGET, 6.5), view("back 3/4", 150, 14, (0, 1.5, 0.3), 6.2)], (520, 520))}
    powered = {"spirit_light_powered": geo("spiritlight_seraph", "idle", [0], [
        view("front", 0, 6, SER_TARGET, 13), view("front 3/4", 35, 10, SER_TARGET, 13), view("side", 90, 6, SER_TARGET, 13), view("back", 180, 8, SER_TARGET, 13)], (520, 620))}
    json.dump(comp, open(os.path.join(OUT, "spirit_light_companion.json"), "w"), indent=1)
    json.dump(powered, open(os.path.join(OUT, "spirit_light_powered.json"), "w"), indent=1)

    poses = {}
    for pose, (length, loop, face) in AN.CUB_POSES.items():
        ages = sorted({int(round(length * 20 * f)) for f in (0.2, 0.45, 0.7)}) if not loop else [0, 24, 48]
        poses["spirit_light_pose_cub_" + pose] = geo("spiritlight_cub", pose, ages, [view(pose + " (cub)", 35, 10, CUB_TARGET, 6.6)], (330, 330))
    for pose, (length, loop, face) in AN.SERAPH_POSES.items():
        ages = sorted({int(round(length * 20 * f)) for f in (0.2, 0.45, 0.7)}) if not loop else [0, 40, 80]
        poses["spirit_light_pose_seraph_" + pose] = geo("spiritlight_seraph", pose, ages, [view(pose + " (seraph)", 20, 8, SER_TARGET, 11.5)], (330, 400))
    json.dump(poses, open(os.path.join(OUT, "spirit_light_poses.json"), "w"), indent=1)

    faces = {}
    # a moment of each pose that shows its face fully (the cub's face by the clip that wears it)
    cub_face_clip = {"happy": ("idle", 0), "sleepy": ("face_sleepy", 0), "fierce": ("cast", 9), "surprised": ("hurt", 2)}
    cub_face_target = {"happy": (0, 1.6, -1.1), "sleepy": (0, 1.6, -1.1), "fierce": (0, 2.0, -1.2), "surprised": (0, 1.9, -1.1)}
    for face, (clip, age) in cub_face_clip.items():
        faces["spirit_light_face_cub_" + face] = geo("spiritlight_cub", clip, [age], [view(face + " (cub)", 0, 8, cub_face_target[face], 5.8)], (300, 300))
    for face in AN.SERAPH_FACES:
        faces["spirit_light_face_seraph_" + face] = geo("spiritlight_seraph", "face_" + face, [4], [view(face + " (seraph)", 0, 2, (0, 3.2, -0.4), 2.9)], (300, 300))
    json.dump(faces, open(os.path.join(OUT, "spirit_light_faces.json"), "w"), indent=1)

    lods = {}
    for form, target, dist, tile in (("cub", CUB_TARGET, 6.4, (360, 360)), ("seraph", SER_TARGET, 13.0, (360, 440))):
        for lod, suffix in ((0, ""), (1, "_lod1"), (2, "_lod2")):
            lods["spirit_light_lod_%s_%d" % (form, lod)] = geo("spiritlight_" + form + suffix, "idle", [0], [view("lod %d" % lod, 30, 10, target, dist)], tile)
    json.dump(lods, open(os.path.join(OUT, "spirit_light_lods.json"), "w"), indent=1)

    # the real drawing helper (SpiritLightDraw): param = variant * 1000 + lod * 100 + clip index (see examples/SpiritLightProp.java)
    props = {}
    realfiles = ["client/prop/SpiritLightDraw.java", "spiritlight/SpiritLightLod.java", "spiritlight/SpiritLightRules.java", "spiritlight/LightMoodRules.java"]

    def prop(param, ages, views, figure=True, tile=(460, 520)):
        return {"kind": "prop", "example": "SpiritLightProp", "realFiles": realfiles, "propKind": "BRONZE_1", "param": param, "figure": figure, "ages": ages, "tile": list(tile), "views": views}

    props["spirit_light_in_game_companion"] = prop(0, [24], [view("beside the shoulder, front 3/4", 40, 8, (-0.3, 1.5, 0.1), 3.6), view("from the back", 160, 12, (-0.3, 1.5, 0.1), 3.8)])
    props["spirit_light_in_game_powered"] = prop(1000, [30], [view("behind the owner, front 3/4", 25, 8, (0, 1.6, -0.9), 7.0), view("from the front", -5, 6, (0, 1.4, -0.6), 7.4)], tile=(460, 560))
    props["spirit_light_in_game_cast_companion"] = prop(1, [4, 8, 12], [view("the beam cast (cub)", 40, 8, (-0.3, 1.5, 0.6), 3.8)], tile=(380, 440))
    props["spirit_light_in_game_cast_powered"] = prop(1001, [6, 12, 20], [view("the beam cast (seraph)", 25, 8, (0, 1.6, -0.5), 7.2)], tile=(380, 520))
    props["spirit_light_beam"] = prop(2000, [1, 2, 4], [view("the piercing beam", 60, 6, (0, 1.6, 4.0), 9.0)], figure=True, tile=(380, 380))
    props["spirit_light_beam_powered"] = prop(2001, [1, 2, 4], [view("the seraph's beam", 60, 6, (0, 1.6, 4.0), 9.0)], figure=True, tile=(380, 380))
    props["spirit_light_pulse"] = prop(3000, [3, 6, 10], [view("the Glowing pulse", 30, 22, (0, 1.0, 0), 11.0)], figure=True, tile=(380, 380))
    props["spirit_light_lod_game_companion"] = prop(100, [24], [view("half level", 40, 8, (-0.3, 1.5, 0.1), 3.6)], tile=(360, 360))
    props["spirit_light_lod_game_companion2"] = prop(200, [24], [view("low level", 40, 8, (-0.3, 1.5, 0.1), 3.6)], tile=(360, 360))
    props["spirit_light_lod_game_powered"] = prop(1100, [30], [view("half level", 25, 8, (0, 1.6, -0.9), 7.0)], tile=(360, 440))
    props["spirit_light_lod_game_powered2"] = prop(1200, [30], [view("low level", 25, 8, (0, 1.6, -0.9), 7.0)], tile=(360, 440))
    json.dump(props, open(os.path.join(OUT, "spirit_light_props.json"), "w"), indent=1)
    print("wrote", len(comp) + len(powered) + len(poses) + len(faces) + len(lods) + len(props), "scenes")


if __name__ == "__main__":
    main()

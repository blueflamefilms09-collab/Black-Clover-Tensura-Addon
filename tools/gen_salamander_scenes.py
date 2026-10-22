"""Writes the preview scenes of the Salamander's moods (docs/magic/fire_casting.md, 4.2) to tools/render_preview/scenes.d/firecast_salamander_moods.json:

    firecast_pose_small_<clip> / firecast_pose_big_<clip>   every new clip, side and 3/4 views, at a spread of ticks (geo scenes of the full model)
    firecast_face_small / firecast_face_big                 every face on the head, close up (prop scenes of the real FireCastDraw, so the plates, the glow and the clip are the game's own)
    firecast_pose_in_context_*                              the poses on the player's shoulder, as the game puts them (prop scenes)

Run the previews with  python3 -B tools/render_preview/preview.py firecast_pose_small_yawn firecast_face_small ...  (pictures land in build/render_preview or $RENDER_PREVIEW_BUILD).
"""
import json
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "render_preview", "scenes.d", "firecast_salamander_moods.json")
sys.path.insert(0, HERE)
import gen_salamander_poses as P  # noqa: E402

REAL = ["client/prop/FireCastDraw.java", "firecast/FireCastLod.java", "firecast/FireCastRules.java", "firecast/SalamanderMoodRules.java"]
FACES = ["calm", "alert", "angry", "sleepy", "smug", "roaring"]


def ticks(length, n=6):
    t = int(round(length * 20))
    return sorted(set(int(round(t * i / (n - 1))) for i in range(n)))


def geo_scene(model, clip, length, big):
    tgt = [0, 3.6, 0.3] if big else [0, 0.95, 0.3]
    dist = 17.0 if big else 5.4
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "00000000", "ages": ticks(length), "tile": [360, 380] if not big else [340, 360],
            "floor": False, "figure": False, "expect": [], "anim": clip,
            "views": [{"name": "side (right)", "yaw": -90, "pitch": 5, "target": tgt, "dist": dist}, {"name": "front 3/4", "yaw": 35, "pitch": 12, "target": tgt, "dist": dist}]}


def prop_scene(param, ages, views, tile, figure=True):
    return {"kind": "prop", "example": "FireCastProp", "realFiles": REAL, "propKind": "BRONZE_1", "param": param, "figure": figure, "ages": ages, "tile": tile, "views": views, "target": [0, 0, 0]}


def main():
    scenes = {"_comment": "The Salamander's mood poses and faces (docs/magic/fire_casting.md, 4.2). Written by tools/gen_salamander_scenes.py; param = 100000 + face * 10000 + variant * 1000 + lod * 100 + pose id."}
    for form, big in (("small", False), ("big", True)):
        for name, c in P.pose_clips("dragon" if big else "salamander").items():
            scenes["firecast_pose_%s_%s" % (form, name)] = geo_scene("firecast_dragon" if big else "firecast_salamander", name, c["length"] if not c["loop"] else 2.0, big)
    # faces, close on the head: the face id is the digit after 1
    head_views_small = [{"name": "front 3/4", "yaw": 40, "pitch": 8, "target": [-0.4, 1.62, 0.2], "dist": 0.55}, {"name": "side (right)", "yaw": -90, "pitch": 4, "target": [-0.4, 1.62, 0.2], "dist": 0.55}]
    head_views_big = [{"name": "front 3/4", "yaw": 38, "pitch": 8, "target": [0.5, 4.2, 1.5], "dist": 3.4}, {"name": "side (right)", "yaw": -90, "pitch": 4, "target": [0.0, 4.2, 0.9], "dist": 3.2}]
    for f in range(len(FACES)):
        scenes["firecast_face_small_%s" % FACES[f]] = prop_scene(100000 + f * 10000 + 2000 + 0, [24], head_views_small, [420, 420], figure=False)
        scenes["firecast_face_big_%s" % FACES[f]] = prop_scene(100000 + f * 10000 + 3000 + 0, [24], head_views_big, [420, 420], figure=False)
    # a face together with its pose, as the game puts them (the face id, the variant, the pose id of SalamanderMoodRules)
    scenes["firecast_combo_small_breathe_roaring"] = prop_scene(100000 + 5 * 10000 + 2000 + 1, [8, 14, 20], head_views_small[:1], [380, 380], figure=False)
    scenes["firecast_combo_small_yawn_sleepy"] = prop_scene(100000 + 3 * 10000 + 2000 + 6, [10, 20, 30], head_views_small[:1], [380, 380], figure=False)
    scenes["firecast_combo_big_roar_roaring"] = prop_scene(100000 + 5 * 10000 + 3000 + 2, [8, 14, 20], head_views_big[:1], [380, 380], figure=False)
    scenes["firecast_combo_big_swipe_angry"] = prop_scene(100000 + 2 * 10000 + 3000 + 4, [6, 9], head_views_big[:1], [380, 380], figure=False)
    json.dump(scenes, open(OUT, "w", newline="\n"), indent=1)
    open(OUT, "a").write("\n")
    print("wrote", OUT, len(scenes) - 1, "scenes")


if __name__ == "__main__":
    main()

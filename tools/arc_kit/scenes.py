"""Why: the preview harness (tools/render_preview) draws a model from scene files, and every character needs the same dozen scenes (four sides, the face, one strip
of frames per clip). This module writes them from the facts of the built model (its real bounds, so a tall boss and a small child are both framed whole), and
collects the manifest facts (bounds, bones, clips, glow bones, recommended hitbox) that the character builders and the renderers read.
"""
import json
import os

import numpy as np

from .rig import rot_file

CLIPS = ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death")
OPTIONAL = ("chest_gem_reveal", "spawn")


def bounds(R):
    """The UNPOSED box of every cube (rotated cubes included), pixels: (lo, hi)."""
    lo, hi = [1e9] * 3, [-1e9] * 3
    for b in R.m.bones.values():
        for c in b["cubes"]:
            infl = c.get("inflate", 0.0)
            for k in range(8):
                x = c["origin"][0] - infl + ((c["size"][0] + 2 * infl) if k & 1 else 0)
                y = c["origin"][1] - infl + ((c["size"][1] + 2 * infl) if k & 2 else 0)
                z = c["origin"][2] - infl + ((c["size"][2] + 2 * infl) if k & 4 else 0)
                if c.get("rotation"):
                    px_, py_, pz_ = c["pivot"]
                    dx, dy, dz = rot_file((x - px_, y - py_, z - pz_), *c["rotation"])
                    x, y, z = px_ + dx, py_ + dy, pz_ + dz
                lo = [min(lo[0], x), min(lo[1], y), min(lo[2], z)]
                hi = [max(hi[0], x), max(hi[1], y), max(hi[2], z)]
    return lo, hi


def view(name, yaw, pitch, target, dist, fov=None):
    v = {"name": name, "yaw": yaw, "pitch": pitch, "target": [round(t, 3) for t in target], "dist": round(dist, 3)}
    if fov:
        v["fov"] = fov
    return v


def scene(model, anim, ages, views, tile=(340, 480), glow=True, floor=True, expect=()):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF" if glow else "0", "ages": [round(a, 1) for a in ages],
            "tile": list(tile), "floor": floor, "figure": False, "expect": list(expect), "anim": anim, "views": views}


def tick(sec):
    return round(sec * 20.0, 1)


def scenes_for(R, clips):
    """The scenes of one model: <id>_front, _back, _face, and one per clip. `clips` = {name: (length, loop)}."""
    d = R.d
    lo, hi = bounds(R)
    h = max(hi[1], d.top) / 16.0
    wide = max(abs(lo[0]), abs(hi[0])) * 2.0 / 16.0
    deep = max(abs(lo[2]), abs(hi[2])) * 2.0 / 16.0
    span = max(h, wide * 0.9, deep * 0.9)
    ty = (max(hi[1], d.top) + max(lo[1], 0.0)) / 32.0
    dist = max(span * 2.05, h * 2.0)
    full = "arc_" + R.name
    sc = {}
    pre = "arc_" + R.name + "_"
    sc[pre + "front"] = scene(full, "idle", [0], [view("front", 0, 4, (0, ty, 0), dist), view("front 3/4", 35, 8, (0, ty, 0), dist)], (400, 560))
    sc[pre + "back"] = scene(full, "idle", [0], [view("back", 180, 5, (0, ty, 0), dist), view("side", 90, 4, (0, ty, 0), dist)], (400, 560))
    fy = (d.head_y + 4.0) / 16.0
    sc[pre + "face"] = scene(full, "idle", [0], [view("face", 0, 2, (0, fy, 0), 2.1), view("face 3/4", 32, 6, (0, fy, 0), 2.1)], (420, 420))
    for clip, (length, loop) in clips.items():
        n = 4 if loop else 5
        ages = [tick(length * k / n) for k in range(n)]
        wideview = 1.3 if clip in ("cast_big", "transform", "chest_gem_reveal", "spawn", "attack") else 1.05
        views = [view("3/4", 30, 6, (0, ty + (0.1 if wideview > 1.2 else 0.0), 0), dist * wideview)]
        if clip in ("attack", "cast_big", "transform"):
            views.append(view("side", 90, 4, (0, ty, 0), dist * wideview))
        sc[pre + clip] = scene(full, clip, ages, views, (340, 480))
    return sc


def write_scenes(R, clips, path):
    sc = scenes_for(R, clips)
    with open(path, "w") as f:
        json.dump(sc, f, indent=1)
        f.write("\n")
    return sc

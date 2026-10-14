"""Draws the Artifact Mimic's geo model on the player's body boxes, from the side and from the front, in the rest, jaw-open and bite poses.

    python3 -B tools/relics/waveboss_preview.py [out.png]      # default: tools/relics/waveboss_preview.png

A software check for the file frame (y up, neck at y = 24, hips at y = 12, front on -z): it applies the animation's rotation (about the bone's pivot, +x raises
the front edge of the lid), position and scale to each bone, projects every cube orthographically and paints it from the texture's average colour, back to
front. It is not the game's renderer (that is client/geo/GeoDraw) but it shows silhouettes, placement on the torso and what the jaw does.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "nusmp")
S = 14   # pixels per model pixel


def load(name):
    geo = json.load(open(os.path.join(ASSETS, "geo", "entity", name + ".geo.json")))["minecraft:geometry"][0]
    anim = json.load(open(os.path.join(ASSETS, "animations", "entity", name + ".animation.json")))["animations"]
    tex = Image.open(os.path.join(ASSETS, "textures", "entity", name + ".png")).convert("RGBA")
    return geo, anim, tex


def sample(track, t):
    keys = sorted((float(k), v) for k, v in track.items())
    if t <= keys[0][0]:
        return np.array(keys[0][1], float)
    if t >= keys[-1][0]:
        return np.array(keys[-1][1], float)
    for (t0, v0), (t1, v1) in zip(keys, keys[1:]):
        if t0 <= t <= t1:
            k = (t - t0) / (t1 - t0)
            return np.array(v0, float) + (np.array(v1, float) - np.array(v0, float)) * k
    return np.array(keys[-1][1], float)


def rot(rx, ry, rz):
    cx, sx, cy, sy, cz, sz = [f(math.radians(a)) for a in (rx, ry, rz) for f in (math.cos, math.sin)][0:6]
    cx, sx = math.cos(math.radians(rx)), math.sin(math.radians(rx))
    cy, sy = math.cos(math.radians(ry)), math.sin(math.radians(ry))
    cz, sz = math.cos(math.radians(rz)), math.sin(math.radians(rz))
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X


def average(tex, u, v, sx, sy, sz):
    px = []
    for (x, y, w, h) in ((u + sz, v + sz, sx, sy), (u + sz, v, sx, sz)):
        for j in range(int(y), int(y + max(1, h))):
            for i in range(int(x), int(x + max(1, w))):
                if i < tex.width and j < tex.height:
                    p = tex.getpixel((i, j))
                    if p[3]:
                        px.append(p[:3])
    return tuple(int(sum(c[k] for c in px) / len(px)) for k in range(3)) if px else (128, 128, 128)


def world(geo, anim, clip, t):
    bones = {b["name"]: b for b in geo["bones"]}
    memo = {}

    def mat(name):
        if name in memo:
            return memo[name]
        b = bones[name]
        piv = np.array(b["pivot"], float)
        r = np.zeros(3)
        p = np.zeros(3)
        sc = np.ones(3)
        if clip:
            a = anim[clip]["bones"].get(name, {})
            if "rotation" in a:
                r = sample(a["rotation"], t)
            if "position" in a:
                p = sample(a["position"], t)
            if "scale" in a:
                sc = sample(a["scale"], t)
        R = rot(*r) @ np.diag(sc)
        local = lambda x: R @ (x - piv) + piv + p
        parent = memo_f(b.get("parent")) if b.get("parent") else (lambda x: x)
        f = lambda x: parent(local(x))
        memo[name] = f
        return f

    def memo_f(n):
        return mat(n)

    return bones, mat


def view(geo, anim, tex, clip, t, axis):
    bones, mat = world(geo, anim, clip, t)
    shapes = []
    for name, b in bones.items():
        f = mat(name)
        for c in b["cubes"]:
            o = np.array(c["origin"], float) - c.get("inflate", 0)
            sz = np.array(c["size"], float) + 2 * c.get("inflate", 0)
            pts = np.array([[o[0] + dx * sz[0], o[1] + dy * sz[1], o[2] + dz * sz[2]] for dx in (0, 1) for dy in (0, 1) for dz in (0, 1)])
            pts = np.array([f(p) for p in pts])
            u, v = c["uv"]
            col = average(tex, u, v, *c["size"])
            shapes.append((pts, col))
    return shapes


def project(p, axis):
    # side view looks from the player's left (+x): screen x = -z (front on the left... we put the front on the right: x = -z flipped), screen y = -y
    if axis == "side":
        return (-p[2], -p[1])
    return (p[0], -p[1])      # front view: the player's left (+x) is on the viewer's right? the front faces the viewer, so +x (left) is on the viewer's right


def hull(pts):
    pts = sorted(set(map(tuple, np.round(pts, 4))))
    if len(pts) <= 2:
        return pts

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])
    lo = []
    for p in pts:
        while len(lo) >= 2 and cross(lo[-2], lo[-1], p) <= 0:
            lo.pop()
        lo.append(p)
    up = []
    for p in reversed(pts):
        while len(up) >= 2 and cross(up[-2], up[-1], p) <= 0:
            up.pop()
        up.append(p)
    return lo[:-1] + up[:-1]


BODY = [((-4, 12, -2), (4, 24, 2), (60, 90, 150)), ((4, 12, -2), (8, 24, 2), (200, 160, 130)), ((-8, 12, -2), (-4, 24, 2), (200, 160, 130)),
        ((-4, 0, -2), (0, 12, 2), (50, 60, 110)), ((0, 0, -2), (4, 12, 2), (50, 60, 110)), ((-4, 24, -4), (4, 32, 4), (220, 180, 150))]


def render(geo, anim, tex, poses):
    W, H = 56, 40
    out = Image.new("RGB", (W * S * len(poses), H * S * 2), (28, 28, 34))
    d = ImageDraw.Draw(out)
    for row, axis in enumerate(("side", "front")):
        for col, (clip, t, label) in enumerate(poses):
            ox, oy = col * W * S + W * S // 2, row * H * S + int(H * S * 0.92)
            tx = lambda p: (ox + project(p, axis)[0] * S, oy + project(p, axis)[1] * S)
            for lo, hi, colr in BODY:
                pts = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])], float)
                d.polygon([tx(np.array(q)) for q in hull(np.array([project(p, axis) for p in pts]))] if False else [(ox + h[0] * S, oy + h[1] * S) for h in hull(np.array([project(p, axis) for p in pts]))], fill=colr, outline=(20, 20, 20))
            shapes = view(geo, anim, tex, clip, t, axis)
            # depth: along the viewing axis (side: +x towards viewer; front: -z towards viewer)
            shapes.sort(key=lambda s: (s[0][:, 0].mean() if axis == "side" else -s[0][:, 2].mean()))
            for pts, colr in shapes:
                poly = [(ox + h[0] * S, oy + h[1] * S) for h in hull(np.array([project(p, axis) for p in pts]))]
                d.polygon(poly, fill=colr, outline=(10, 10, 10))
            d.text((ox - W * S // 2 + 8, row * H * S + 8), "%s %s %s" % (axis, label, clip or "rest"), fill=(230, 230, 230))
    return out


def main():
    geo, anim, tex = load("relic_mimic_belt")
    poses = [(None, 0, "rest"), ("yawn", 0.6, "jaw open"), ("bite", 0.5, "bite: wide"), ("bite", 0.63, "bite: snap")]
    out = render(geo, anim, tex, poses)
    path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "waveboss_preview.png")
    out.save(path)
    print("wrote", path, out.size)


if __name__ == "__main__":
    main()

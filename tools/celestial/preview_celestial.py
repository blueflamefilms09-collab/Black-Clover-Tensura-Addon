"""A software preview of the Celestial models (not the game's renderer): every cube face is drawn as a polygon in the average colour of its texture patch (brighter where the
glow map shines), back to front, from the front and from a three-quarter view, in a clip at a time.

    python3 -B tools/celestial/preview_celestial.py out.png [id[:clip[:time]] ...]       e.g. leo king_p3:judge:1.5 gate:open_hold:1

It applies the animation exactly as GeoDraw documents it (rotation about the bone's pivot, Z then Y then X; position; scale; parents first).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "nusmp")
S = 5


def load(name):
    key = "celestial_" + name
    geo = json.load(open(os.path.join(ASSETS, "geo", "entity", key + ".geo.json")))["minecraft:geometry"][0]
    ap = os.path.join(ASSETS, "animations", "entity", key + ".animation.json")
    anim = json.load(open(ap))["animations"] if os.path.exists(ap) else {}
    tex = np.asarray(Image.open(os.path.join(ASSETS, "textures", "entity", key + ".png")).convert("RGBA"), np.float32)
    gp = os.path.join(ASSETS, "textures", "entity", key + "_glow.png")
    glow = np.asarray(Image.open(gp).convert("RGBA"), np.float32) if os.path.exists(gp) else np.zeros_like(tex)
    return geo, anim, tex, glow


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
    cx, sx = math.cos(math.radians(rx)), math.sin(math.radians(rx))
    cy, sy = math.cos(math.radians(ry)), math.sin(math.radians(ry))
    cz, sz = math.cos(math.radians(rz)), math.sin(math.radians(rz))
    X = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Y = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Z = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Z @ Y @ X


def transforms(geo, anim, clip, t):
    bones = {b["name"]: b for b in geo["bones"]}
    memo = {}

    def f(name):
        if name in memo:
            return memo[name]
        b = bones[name]
        piv = np.array(b["pivot"], float)
        r, p, sc = np.zeros(3), np.zeros(3), np.ones(3)
        if clip and clip in anim:
            a = anim[clip]["bones"].get(name, {})
            if "rotation" in a: r = sample(a["rotation"], t)
            if "position" in a: p = sample(a["position"], t)
            if "scale" in a: sc = sample(a["scale"], t)
        R = rot(*r) @ np.diag(sc)
        parent = f(b["parent"]) if b.get("parent") else (lambda x: x)
        memo[name] = lambda x, R=R, piv=piv, p=p, parent=parent: parent(R @ (x - piv) + piv + p)
        return memo[name]
    return bones, f


def faces_of(c, tex, glow):
    o = np.array(c["origin"], float) - c.get("inflate", 0)
    sz = np.array(c["size"], float) + 2 * c.get("inflate", 0)
    u, v = c["uv"]
    sx, sy, sa = [int(round(x)) for x in c["size"]]
    rects = {"top": (u + sa, v, sx, sa), "bottom": (u + sa + sx, v, sx, sa), "right": (u, v + sa, sa, sy), "front": (u + sa, v + sa, sx, sy), "left": (u + sa + sx, v + sa, sa, sy), "back": (u + 2 * sa + sx, v + sa, sx, sy)}
    X0, Y0, Z0 = o
    X1, Y1, Z1 = o + sz
    quads = {"front": [(X0, Y0, Z0), (X1, Y0, Z0), (X1, Y1, Z0), (X0, Y1, Z0)], "back": [(X0, Y0, Z1), (X1, Y0, Z1), (X1, Y1, Z1), (X0, Y1, Z1)],
             "right": [(X0, Y0, Z0), (X0, Y0, Z1), (X0, Y1, Z1), (X0, Y1, Z0)], "left": [(X1, Y0, Z0), (X1, Y0, Z1), (X1, Y1, Z1), (X1, Y1, Z0)],
             "top": [(X0, Y1, Z0), (X1, Y1, Z0), (X1, Y1, Z1), (X0, Y1, Z1)], "bottom": [(X0, Y0, Z0), (X1, Y0, Z0), (X1, Y0, Z1), (X0, Y0, Z1)]}
    out = []
    for name, q in quads.items():
        x, y, w, h = rects[name]
        patch = tex[y:y + max(1, h), x:x + max(1, w)]
        gl = glow[y:y + max(1, h), x:x + max(1, w)]
        a = patch[..., 3:4]
        col = (patch[..., :3] * a).sum((0, 1)) / max(1.0, a.sum())
        g = gl[..., :3] * (gl[..., 3:4] / 255.0)
        gm = g.mean((0, 1)) if g.size else np.zeros(3)
        col = np.clip(col * 0.85 + gm * 0.7, 0, 255)
        out.append((np.array(q, float), tuple(int(v) for v in col)))
    return out


def render(name, clip, t, yaw):
    geo, anim, tex, glow = load(name)
    bones, f = transforms(geo, anim, clip, t)
    polys = []
    ca, sa = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    for bn, b in bones.items():
        fn = f(bn)
        for c in b["cubes"]:
            if "rotation" in c:
                pv = np.array(c.get("pivot", b["pivot"]), float)
                R = rot(*c["rotation"])
                cf = lambda x, R=R, pv=pv: R @ (x - pv) + pv
            else:
                cf = lambda x: x
            for q, col in faces_of(c, tex, glow):
                pts = [fn(cf(p)) for p in q]
                pts = [np.array([p[0] * ca + p[2] * sa, p[1], -p[0] * sa + p[2] * ca]) for p in pts]
                depth = np.mean([p[2] for p in pts])
                n = np.cross(pts[1] - pts[0], pts[3] - pts[0])
                polys.append((depth, [(p[0], p[1]) for p in pts], col))
    polys.sort(key=lambda x: -x[0])
    return polys


def draw(names, out):
    W = 330
    sheets = []
    for spec in names:
        parts = spec.split(":")
        name, clip, t = parts[0], parts[1] if len(parts) > 1 else "idle", float(parts[2]) if len(parts) > 2 else 0.5
        for yaw in (0, 40):
            polys = render(name, clip, t, yaw)
            allp = [p for _d, ps, _c in polys for p in ps]
            xs, ys = [p[0] for p in allp], [p[1] for p in allp]
            sc = min(W / max(1, (max(xs) - min(xs)) + 6), 340 / max(1, (max(ys) - min(ys)) + 6))
            sc = min(sc, 9)
            im = Image.new("RGB", (W, 360), (14, 12, 34))
            d = ImageDraw.Draw(im)
            ox, ymin = W / 2 - (min(xs) + max(xs)) / 2 * sc, min(ys)
            for _depth, ps, col in polys:
                d.polygon([(ox + x * sc, 350 - (y - ymin) * sc) for x, y in ps], fill=col, outline=tuple(max(0, v - 40) for v in col))
            d.text((6, 4), f"{name} {clip} {t} yaw {yaw}", fill=(255, 236, 160))
            sheets.append(im)
    cols = 4
    rows = (len(sheets) + cols - 1) // cols
    sheet = Image.new("RGB", (W * cols, 360 * rows), (14, 12, 34))
    for i, im in enumerate(sheets):
        sheet.paste(im, ((i % cols) * W, (i // cols) * 360))
    sheet.save(out)


if __name__ == "__main__":
    draw(sys.argv[2:], sys.argv[1])

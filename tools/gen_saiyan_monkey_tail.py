#!/usr/bin/env python3
"""The Saiyan MONKEY tail (docs/body_parts_pass.md), written without Blender (no Blender in the build session; see the caveat in the doc).

The owner: "It should be a monkey tail, not some lizard looking tail." The old tail (tools/blender/saiyan_hair.py, build_tail) stays: it is a red 7-sided tube with a hard
curl that rises like a scorpion's. This one is a furry brown tail: a gentle S-curve that leaves the hips, hangs behind and curls softly up at the tip, only a slight taper, a coat of
fur strands laid along it and a tuft at the end. A second mesh holds the same tail WRAPPED AROUND THE WAIST (the classic Saiyan pose).

Files written (the mesh format of EntityMeshData: parts tail / wing_l / wing_r are the root, middle and tip segments, the same names the old tail uses so the same rig swings them):
  entity_meshes/saiyan_tail_monkey.txt, _lod.txt        the hanging tail (full / far copy)
  entity_meshes/saiyan_tail_wrap.txt,   _lod.txt        the tail around the waist
  textures/entity/boss_mesh/saiyan_tail_monkey.png (+ _glow.png, transparent)   128x128 fur skin, shared by both meshes (the wrap mesh reads the same files under its own id)
  textures/entity/boss_mesh/saiyan_tail_wrap.png    (+ _glow.png)               copy of the same skin (the drawer loads skins by mesh id)

Every number is in model pixels (1/16 block), x sideways, y up (feet 0), the player's back at z -2, front +z. Deterministic (fixed seeds): running it twice writes the same bytes.
  python3 tools/gen_saiyan_monkey_tail.py
"""
import math
import os
import random

import numpy as np
from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
MESH = os.path.join(RES, "entity_meshes")
SKIN = os.path.join(RES, "textures", "entity", "boss_mesh")
TEX = 128

# the hanging tail: leaves the hips, drops behind the legs, then a soft curl up at the tip (y up, z back = negative)
HANG = [(0.0, 13.5, -1.4), (0.0, 12.6, -4.6), (0.3, 10.8, -8.6), (0.6, 8.6, -12.8), (0.9, 7.0, -17.0), (1.3, 6.6, -21.0), (1.7, 7.9, -24.4), (1.9, 10.4, -26.0), (1.8, 12.8, -25.6)]
ROOT_PIVOT = (0.0, 13.6, -1.4)


def wrap_path():
    """From the hips around the waist one and a third times, the tip tucked in at the front (a = half width, b = half depth of the path about the body at the hips)."""
    pts = [(0.0, 13.5, -1.4), (0.0, 13.4, -2.6)]
    a, b = 5.0, 3.2
    n = 18
    for i in range(n + 1):
        t = i / n
        ang = math.pi + t * 2.0 * math.pi * 1.33                      # from the back round, the front, the other side, the back again
        pts.append((a * math.sin(ang), 13.3 + 1.5 * t, b * math.cos(ang)))
    return pts


def smooth(points, sub=4):
    """Catmull-Rom through the control points, `sub` samples per span"""
    P = [np.array(p, float) for p in points]
    P = [P[0] * 2 - P[1]] + P + [P[-1] * 2 - P[-2]]
    out = []
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for k in range(sub):
            t = k / sub
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(P[-2])
    return out


def unit(v):
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


class Mesh:
    def __init__(self, eid):
        self.eid = eid
        self.parts = {"tail": [], "wing_l": [], "wing_r": []}
        self.pivots = {}

    def quad(self, part, verts, normals, uvs):
        """verts: 4 xyz, normals: 4 xyz (or one), uvs: 4 (u, v) in 128 units. The winding is fixed so that (v1-v0)x(v3-v0) looks along the normal, as in the other meshes."""
        v = [np.array(x, float) for x in verts]
        n = [unit(np.array(x, float)) for x in (normals if len(normals) == 4 else normals * 4)]
        if np.dot(np.cross(v[1] - v[0], v[3] - v[0]), sum(n)) < 0:
            v = [v[0], v[3], v[2], v[1]]
            n = [n[0], n[3], n[2], n[1]]
            uvs = [uvs[0], uvs[3], uvs[2], uvs[1]]
        self.parts[part].append((v, n, uvs))

    def count(self):
        return sum(len(q) for q in self.parts.values())

    def write(self, path, comment):
        L = ["# " + comment, "entity %s 0.062500" % self.eid, "glow FFFFFF 0.1 0.5 1.0"]
        prev = "-"
        for name in ("tail", "wing_l", "wing_r"):
            L.append("pivot %s %s %.3f %.3f %.3f" % ((name, prev) + tuple(self.pivots[name])))
            prev = name
        for name in ("tail", "wing_l", "wing_r"):
            L.append("part " + name)
            for v, n, uv in self.parts[name]:
                f = []
                for i in range(4):
                    f += ["%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % (v[i][0], v[i][1], v[i][2], uv[i][0], uv[i][1], n[i][0], n[i][1], n[i][2])]
                L.append("qn 0 " + " ".join(f))
        with open(path, "w", newline="\n") as fh:
            fh.write("\n".join(L) + "\n")


def build(eid, ctrl, sides, strands_step, lod_strands, wrap):
    """The tail along the control points: a ring tube (taper, soft fur ruff), strands laid along it and a tuft at the end."""
    m = Mesh(eid)
    path = smooth(ctrl, 4 if not wrap else 2)
    if sides < 8:                                                                       # the far copy: every other station
        path = path[::2] + ([path[-1]] if (len(path) - 1) % 2 else [])
    n = len(path)
    cuts = [0, int(n * 0.36), int(n * 0.68), n - 1]
    names = ["tail", "wing_l", "wing_r"]
    m.pivots = {"tail": ROOT_PIVOT, "wing_l": tuple(path[cuts[1]]), "wing_r": tuple(path[cuts[2]])}
    side0 = np.array([1.0, 0.0, 0.0]) if not wrap else np.array([0.0, 1.0, 0.0])
    frames = []
    for i in range(n):
        tan = unit(path[min(i + 1, n - 1)] - path[max(i - 1, 0)])
        sd = unit(side0 - tan * np.dot(side0, tan))
        frames.append((tan, sd, unit(np.cross(tan, sd))))

    def radius(i):
        t = i / (n - 1)
        r = 1.45 - 0.55 * t ** 1.3                                                      # a slight taper: 1.45 at the hips, 0.9 near the end
        if t > 0.9:
            r *= 1.0 + 1.2 * (t - 0.9)                                                  # the tip swells into the tuft
        return r * (0.84 if wrap else 1.0)

    # arc length for the texture (v runs along the tail)
    arc = [0.0]
    for i in range(1, n):
        arc.append(arc[-1] + float(np.linalg.norm(path[i] - path[i - 1])))
    total = arc[-1]

    def ring(i):
        tan, sd, up = frames[i]
        r = radius(i)
        pts, nor = [], []
        for k in range(sides):
            a = 2 * math.pi * k / sides
            d = sd * math.cos(a) + up * math.sin(a)
            pts.append(path[i] + d * r)
            nor.append(d)
        return pts, nor

    seg_of = lambda i: 0 if i < cuts[1] else 1 if i < cuts[2] else 2
    rings = [ring(i) for i in range(n)]
    uw = 56.0 / sides
    for i in range(n - 1):
        part = names[seg_of(i)]
        v0, v1 = 4.0 + 120.0 * arc[i] / total, 4.0 + 120.0 * arc[i + 1] / total
        for k in range(sides):
            k2 = (k + 1) % sides
            m.quad(part, [rings[i][0][k], rings[i + 1][0][k], rings[i + 1][0][k2], rings[i][0][k2]],
                   [rings[i][1][k], rings[i + 1][1][k], rings[i + 1][1][k2], rings[i][1][k2]],
                   [(uw * k, v0), (uw * k, v1), (uw * (k + 1), v1), (uw * (k + 1), v0)])
    # the end cap
    last = rings[-1]
    for k in range(0, sides, 2):
        m.quad("wing_r", [last[0][k], last[0][(k + 1) % sides], last[0][(k + 2) % sides], path[-1] + frames[-1][0] * 0.3],
               [frames[-1][0]], [(58, 100), (60, 100), (62, 100), (60, 102)])

    # fur strands: thin tapering blades laid along the tail, pointing out and tipward, in a staggered spiral
    rnd = random.Random(1966)
    step = strands_step
    for i in range(1, n - 1, step):
        part = names[seg_of(i)]
        tan, sd, up = frames[i]
        t = i / (n - 1)
        for k in range(5 if not lod_strands else 3):
            a = 2 * math.pi * (k + 0.5 * (i // step % 2)) / (5 if not lod_strands else 3) + rnd.uniform(-0.3, 0.3)
            rad = sd * math.cos(a) + up * math.sin(a)
            d = unit(rad * 0.30 + tan * 0.95)
            base = path[i] + rad * radius(i) * 0.82
            ln = (3.0 - 0.8 * t) * (1 + rnd.uniform(-0.2, 0.2))
            w = 2.1 * (1 - 0.2 * t)
            strand(m, part, base, d, tan, ln, w, rnd.randrange(4), droop=(0.0, -0.45, 0.0))
    # the tuft: a fan of longer strands at the very end
    tan, sd, up = frames[-1]
    for k in range(9 if not lod_strands else 5):
        a = 2 * math.pi * k / (9 if not lod_strands else 5) + rnd.uniform(-0.2, 0.2)
        rad = sd * math.cos(a) + up * math.sin(a)
        d = unit(rad * 0.45 + tan * 0.9)
        strand(m, "wing_r", path[-1] - tan * 0.5 + rad * 0.5, d, tan, 4.6 * (1 + rnd.uniform(-0.2, 0.2)), 2.3, rnd.randrange(4), droop=(0.0, -0.45, 0.0))
    return m


def strand(m, part, base, d, tan, length, width, variant, droop=(0.0, 0.0, 0.0)):
    """One fur blade (two quads, front and back, because the entity render type culls back faces): `width` at the root, a tenth of it at the tip, leaning a little by `droop`."""
    side = unit(np.cross(tan, d))
    if np.linalg.norm(side) < 1e-6:
        side = np.array([1.0, 0.0, 0.0])
    tip = base + d * length + np.array(droop) * length * 0.7
    hw, tw = width / 2, width * 0.1
    nrm = unit(np.cross(side, tip - base))
    u0 = 64 + 16 * variant
    for flip in (1, -1):
        m.quad(part, [base - side * hw, base + side * hw, tip + side * tw, tip - side * tw], [nrm * flip], [(u0, 62), (u0 + 16, 62), (u0 + 16, 2), (u0, 2)])


# ================================================================================================ skin
def fur_skin():
    """128x128: left 56 columns the tube (brown fur streaked along v), right the strand variants (dark root to a light tip), the cap texel at (58..62, 100)."""
    rnd = np.random.default_rng(7)
    img = np.zeros((TEX, TEX, 4), np.uint8)
    base = np.array([122, 78, 40], float)
    dark = np.array([74, 44, 22], float)
    light = np.array([176, 118, 64], float)
    # tube: per column a streak colour, per row a slow light wave, tipward a little lighter
    colk = rnd.uniform(0, 1, 64)
    for x in range(0, 56):
        for y in range(0, TEX):
            t = y / (TEX - 1)
            s = colk[x] * 0.7 + rnd.uniform(0, 0.3)
            c = dark + (base - dark) * (0.35 + 0.65 * s)
            c = c + (light - c) * (0.25 * t + 0.12 * math.sin(y * 0.55 + x * 0.9))
            if (x % 7) == 0:
                c = c * 0.78                                                     # the seams between sides read as the darker valley between fur ridges
            img[y, x] = [*np.clip(c, 0, 255), 255]
    # the cap texel block and filler
    img[96:106, 56:64] = [*dark.astype(int), 255]
    # strands: four 16-wide variants, v=62 is the root (dark) and v=2 the tip (light, warm)
    for j in range(4):
        for x in range(16):
            u = 64 + 16 * j + x
            streak = rnd.uniform(0.0, 1.0)
            for y in range(0, 64):
                t = 1 - y / 63.0                                                 # 0 at the tip, 1 at the root... flipped below
                root = y / 63.0
                c = light * (1 - root) + dark * root
                c = c * (0.82 + 0.3 * streak) + np.array([10, 4, 0]) * (1 - root)
                edge = 1.0 - 0.18 * (abs(x - 7.5) / 7.5)
                img[y, u] = [*np.clip(c * edge, 0, 255).astype(int), 255]
    # the lower right quarter: the same strands in a deeper tone, so a repeat does not read flat
    img[64:128, 64:128] = img[0:64, 64:128]
    img[64:128, 64:128, :3] = (img[64:128, 64:128, :3] * 0.8).astype(np.uint8)
    return img


def write_skin(name, img):
    Image.fromarray(img, "RGBA").save(os.path.join(SKIN, name + ".png"), optimize=True)
    Image.fromarray(np.zeros((TEX, TEX, 4), np.uint8), "RGBA").save(os.path.join(SKIN, name + "_glow.png"), optimize=True)


def main():
    os.makedirs(SKIN, exist_ok=True)
    img = fur_skin()
    for eid, ctrl, wrap in (("saiyan_tail_monkey", HANG, False), ("saiyan_tail_wrap", wrap_path(), True)):
        full = build(eid, ctrl, 8, 3, False, wrap)
        lod = build(eid, ctrl, 6, 2, True, wrap)
        full.write(os.path.join(MESH, eid + ".txt"), "monkey tail by tools/gen_saiyan_monkey_tail.py (no Blender), %d quads; 3 parts" % full.count())
        lod.write(os.path.join(MESH, eid + "_lod.txt"), "far copy of %s, %d quads" % (eid, lod.count()))
        write_skin(eid, img)
        print(eid, full.count(), "quads,", lod.count(), "far")


if __name__ == "__main__":
    main()

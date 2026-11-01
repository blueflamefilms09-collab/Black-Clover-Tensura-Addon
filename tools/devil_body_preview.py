#!/usr/bin/env python3
"""Flat preview of the devil body parts (horns and wings) without the game (3.48.0). PIL only.

    python3 tools/devil_body_preview.py

Mirrors the box trees of client/devil/DevilModels.java (Horns.create, Wings.create, Wings.pose) and the vanilla player boxes, projects them
orthographically (back view, three-quarter back view, side view) and draws every face flat-shaded, far to near. It is a check of the SHAPES (do the wings
leave the shoulders, do the horns sit on the head, is the fold tidy), not of the textures: those are previewed by tools/gen_devil_textures.py.
If you change the numbers in DevilModels.java, change them here too. Writes docs/previews/devil/body_preview.png.
"""
import math
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "docs/previews/devil/body_preview.png"


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def ident():
    return [[1 if i == j else 0 for j in range(4)] for i in range(4)]


def translate(x, y, z):
    m = ident(); m[0][3], m[1][3], m[2][3] = x, y, z
    return m


def scale(x, y, z):
    m = ident(); m[0][0], m[1][1], m[2][2] = x, y, z
    return m


def rot_zyx(xr, yr, zr):
    cx, sx, cy, sy, cz, sz = math.cos(xr), math.sin(xr), math.cos(yr), math.sin(yr), math.cos(zr), math.sin(zr)
    rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]

    def mm(a, b): return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    r = mm(rz, mm(ry, rx))
    m = ident()
    for i in range(3):
        for j in range(3): m[i][j] = r[i][j]
    return m


class Part:
    def __init__(self, off=(0, 0, 0), rot=(0, 0, 0), sc=(1, 1, 1), boxes=(), color=(150, 150, 150), children=None):
        self.off, self.rot, self.sc, self.boxes, self.color, self.children = off, rot, sc, list(boxes), color, children or {}


def faces_of(part, parent_m, out):
    m = mat_mul(mat_mul(mat_mul(parent_m, translate(*part.off)), rot_zyx(*part.rot)), scale(*part.sc))
    for (x, y, z, dx, dy, dz, grow) in part.boxes:
        x0, y0, z0, x1, y1, z1 = x - grow, y - grow, z - grow, x + dx + grow, y + dy + grow, z + dz + grow
        c = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
        w = [tuple(sum(m[i][k] * (p[k] if k < 3 else 1) for k in range(4)) for i in range(3)) for p in (tuple(q) + (1,) for q in c)]
        for idx in ((0, 1, 2, 3), (4, 5, 6, 7), (0, 1, 5, 4), (3, 2, 6, 7), (0, 3, 7, 4), (1, 2, 6, 5)):
            out.append(([w[i] for i in idx], part.color))
    for ch in part.children.values():
        faces_of(ch, m, out)


# ---- the player (vanilla boxes, y down, the face at -z, the back at +z)
def player():
    skin, shirt, pants = (190, 150, 120), (92, 96, 112), (52, 56, 76)
    head = Part(boxes=[(-4, -8, -4, 8, 8, 8, 0)], color=skin)
    body = Part(boxes=[(-4, 0, -2, 8, 12, 4, 0)], color=shirt)
    ra = Part(off=(-5, 2, 0), boxes=[(-3, -2, -2, 4, 12, 4, 0)], color=skin)
    la = Part(off=(5, 2, 0), boxes=[(-1, -2, -2, 4, 12, 4, 0)], color=skin)
    rl = Part(off=(-1.9, 12, 0), boxes=[(-2, 0, -2, 4, 12, 4, 0)], color=pants)
    ll = Part(off=(1.9, 12, 0), boxes=[(-2, 0, -2, 4, 12, 4, 0)], color=pants)
    return head, body, [ra, la, rl, ll]


# ---- horns: mirror of DevilModels.Horns.create
def horns(grow=1.0):
    col = (60, 52, 70)
    parts = []
    for s in (-1, 1):
        t = Part(off=(0, 0, 0), boxes=[(-0.5, -3, -0.5, 1, 3, 1, 0)], color=col)
        b = Part(off=(0, -2.85, 0), rot=(-0.34, 0, s * 0.12), boxes=[(-0.5, -3, -0.5, 1, 3, 1, 0)], color=col)
        a2 = Part(off=(0, -3, 0), rot=(-0.32, 0, s * 0.26), boxes=[(-1, -3, -1, 2, 3, 2, -0.15)], color=col, children={"t": b})
        a = Part(off=(s * 2.6, -7.7, -1.2), rot=(-0.30, 0, s * 0.42), boxes=[(-1, -3, -1, 2, 3, 2, 0)], color=col, children={"b": a2})
        parts.append(a)
    # scale about the head top (y = -8): translate(0,-0.5,0) scale translate(0,0.5,0), in pixels: -8 .. 
    return parts


# ---- wings: mirror of DevilModels.Wings (create + pose)
HUM, FORE = 9.0, 12.0
FINGER = (13.0, 14.0, 12.0)
NOM_L, NOM_W, BODY_X, BODY_Y = 22.0, 15.2, 0.5, 14.0
STEP_X, STEP_W = (1.0, 8.0, 15.0), (3.4, 8.8, 15.2)


def lerp(a, b, t): return a + (b - a) * t


def wings(spread, flap, size):
    bone, web = (74, 58, 52), (150, 40, 60)
    sp = max(0.0, min(1.0, spread))
    yaw = -(0.2 + 0.3 * sp)
    hum = lerp(-1.25, -0.9, sp) + flap * 0.42
    elbow = lerp(2.3, 0.65, sp) - flap * 0.12
    fore_abs = hum + elbow
    fa = [lerp(1.3, 0.15, sp) + flap * 0.3, lerp(1.4, 0.7, sp) + flap * 0.4, lerp(1.5, 1.25, sp) + flap * 0.5]
    wx = HUM * math.cos(hum) + FORE * math.cos(fore_abs)
    wy = HUM * math.sin(hum) + FORE * math.sin(fore_abs)
    ax, ay = [wx], [wy]
    for k in range(3):
        ax.append(wx + FINGER[k] * math.cos(fa[k]))
        ay.append(wy + FINGER[k] * math.sin(fa[k]))
    ax.append(BODY_X)
    ay.append(BODY_Y)
    out = []
    for s in (-1, 1):
        fingers = {}
        for k in range(3):
            fingers["f%d" % k] = Part(off=(s * FORE, 0, 0), rot=(0, 0, s * (fa[k] - fore_abs)),
                                      boxes=[((0 if s > 0 else -FINGER[k]), -0.5, -0.5, FINGER[k], 1, 1, 0)], color=bone)
        fore = Part(off=(s * HUM, 0, 0), rot=(0, 0, s * elbow), boxes=[((0 if s > 0 else -FORE), -1, -1, FORE, 2, 2, 0)], color=bone, children=fingers)
        h = Part(rot=(0, 0, s * hum), boxes=[((0 if s > 0 else -HUM), -1, -1, HUM, 2, 2, 0)], color=bone, children={"fore": fore})
        kids = {"hum": h}
        for k in range(4):
            x0, y0, x1, y1 = ax[k], ay[k], ax[k + 1], ay[k + 1]
            aim = (math.atan2(y0, x0) + math.atan2(y1, x1)) * 0.5
            ln = (math.hypot(x0, y0) + math.hypot(x1, y1)) * 0.5
            chord = math.hypot(x1 - x0, y1 - y0)
            boxes = [((STEP_X[st] if s > 0 else -(STEP_X[st] + 7.0)), -STEP_W[st] / 2, -0.25, 7.0, STEP_W[st], 0.5, 0) for st in range(3)]
            kids["m%d" % k] = Part(off=(0, 0, 0.08 * k), rot=(0, 0, s * aim), sc=(max(0.1, ln / NOM_L), max(0.06, chord / NOM_W), 1), boxes=boxes,
                                   color=(150 + 8 * k, 40, 60 + 6 * k))
        out.append(Part(off=(s * 2.2, 2.2, 2.6), rot=(0, s * yaw, 0), sc=(size, size, size), children=kids))
    return out


def shade(pts, base):
    (ax, ay, az), (bx, by, bz), (cx, cy, cz) = pts[0], pts[1], pts[2]
    ux, uy, uz, vx, vy, vz = bx - ax, by - ay, bz - az, cx - ax, cy - ay, cz - az
    nx, ny, nz = uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx
    n = math.sqrt(nx * nx + ny * ny + nz * nz) or 1
    d = abs((nx * 0.3 + ny * -0.6 + nz * 0.74) / n)
    k = 0.45 + 0.55 * d
    return tuple(min(255, int(c * k)) for c in base)


def render(scene_faces, yaw, px, w, h, cx, cy):
    """Orthographic: rotate about y by yaw (0 = seen from behind), screen x = -x (the player's right is on the viewer's right), depth = -z."""
    ca, sa = math.cos(yaw), math.sin(yaw)
    img = Image.new("RGB", (w, h), (40, 44, 56))
    d = ImageDraw.Draw(img)
    proj = []
    for pts, col in scene_faces:
        q = []
        for (x, y, z) in pts:
            xr, zr = x * ca + z * sa, -x * sa + z * ca
            q.append((-xr, y, zr))
        depth = sum(p[2] for p in q) / len(q)
        proj.append((depth, q, col))
    proj.sort(key=lambda t: t[0])
    for depth, q, col in proj:
        poly = [(cx + p[0] * px, cy + p[1] * px) for p in q]
        d.polygon(poly, fill=shade(q, col), outline=(20, 20, 26))
    return img


def scene(spread, flap, size, with_horns=True, with_wings=True, horn_grow=1.0):
    faces = []
    head, body, others = player()
    root = ident()
    faces_of(head, root, faces)
    faces_of(body, root, faces)
    for p in others: faces_of(p, root, faces)
    if with_horns:
        m = mat_mul(mat_mul(translate(0, -8, 0), scale(horn_grow, horn_grow, horn_grow)), translate(0, 8, 0))
        for h in horns(horn_grow): faces_of(h, m, faces)
    if with_wings:
        for w in wings(spread, flap, size): faces_of(w, root, faces)
    return faces


def main():
    cases = [("union: spread 1, flap 0", 1.0, 0.0, 0.9), ("flying 50%: down stroke", 1.0, 0.85, 0.55), ("ground idle: spread 0.4", 0.4, 0.0, 0.65), ("folded: spread 0", 0.0, 0.0, 0.55)]
    views = [("back", 0.0), ("3/4", 0.6), ("side", math.pi / 2)]
    px, cw, ch = 7, 300, 300
    sheet = Image.new("RGB", (cw * len(views), ch * len(cases)), (30, 32, 40))
    d = ImageDraw.Draw(sheet)
    for r, (name, sp, fl, sz) in enumerate(cases):
        faces = scene(sp, fl, sz)
        for c, (vn, yaw) in enumerate(views):
            tile = render(faces, yaw, px, cw, ch, cw // 2, 120)
            sheet.paste(tile, (c * cw, r * ch))
            d.text((c * cw + 6, r * ch + 4), name + " | " + vn, fill=(230, 230, 240))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(OUT)
    print("ok ->", OUT)


if __name__ == "__main__":
    main()

"""art_hart2_kit: the shared tools of the seven Seven Star weapon builders (tools/heroweapon/art_hart2.py).

Why: the Hammer, Claw, Axe, Whip, Staff, Gauntlet and Projectile are one family in the series (the Seven Star Weapons of Raphtalia's world, every one
vassal to one of the four Holy Weapons) and have to read as siblings while each keeps its own silhouette. This file holds what they share, so each weapon
function in art_hart2.py is only its own proportions: the owner's model method (tools/relics/mesh_lib.py: lofts, sweeps, lathes, named parts, smoothed
normals, painted materials, a second additive glow skin), the family marks (the heptagonal gem bezel and the cabochon gem, the Big Dipper engraving), the 16x16
sprite canvas, the item model writer (display transforms and the first-person preset of tools/relics/display_presets.py), the previews (the mesh in four
views, the third-person held view next to Steve, the sprite) and the size / orientation audit against a class fragment, so the lead can merge the fragment
without surprises. Nothing here edits a file outside this builder's own outputs.

Everything is deterministic (seeded noise, no clocks) so a re-run rewrites identical bytes.
"""
import hashlib
import json
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
RELICS = os.path.join(ROOT, "tools", "relics")
if RELICS not in sys.path:
    sys.path.insert(0, RELICS)

import mesh_lib as ml                                              # noqa: E402  (the owner's model method)
import wave1_models as w                                           # noqa: E402  (add, paint_part, set_glow, ellipsoid, noise2 ...)
from wave1_icons import Canvas, shade, mix                         # noqa: E402  (the 16x16 pixel canvas of the relic icons)
import display_presets as dp                                       # noqa: E402  (the first-person scale rule of long weapons)
import weapon_sheet as ws                                          # noqa: E402  (the held view next to Steve)
import weapon_orientation as wo                                    # noqa: E402
import weapon_classes as wc                                        # noqa: E402
import audit_weapon_scale as aws                                   # noqa: E402

V = np.array
ASSETS = ml.ASSETS
MESH_DIR = os.path.join(ASSETS, "weapon_meshes")
SKIN_DIR = os.path.join(ASSETS, "textures", "entity", "weapon")
ICON_DIR = os.path.join(ASSETS, "textures", "item")
MODEL_DIR = os.path.join(ASSETS, "models", "item")
CLASS_FRAGMENT = os.path.join(HERE, "classes_hart2.json")
SCRATCH = os.environ.get("HART2_SCRATCH", os.path.join(ROOT, "build", "hart2"))
PREVIEW_DIR = os.environ.get("HART2_PREVIEW", os.path.join(SCRATCH, "previews"))


# ================================================================ colours
# Why: the logic builders write the look they want in docs/heroweapon/weapon_<pkg>.md (palette in hex, gem colour, glow colour: their effects reuse the colour, so the gem
# must match it). hx() turns their hex into the (r, g, b) every painter here takes.
def hx(h):
    """'4A4F5C' or '#4A4F5C' -> (74, 79, 92)."""
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16))


def register_materials():
    """Adds the family's own painted materials to mesh_lib's table at run time (a colour AND its highlight, so the highlight band of a dark leather is dark, not tan;
    mesh_lib.py itself is not edited). Only the materials the seven weapons use."""
    M = ml.MATERIALS

    def add(name, template, base, hi, **kw):
        d = dict(M[template])
        d.update(base=base, hi=hi)
        d.update(kw)
        M[name] = d

    add("hero_steel", "steel", (118, 128, 146), (236, 242, 252), streak=.12, blotch=.10, edge=.22)
    add("hero_blade_steel", "steel", (154, 163, 178), (240, 246, 255), streak=.17, blotch=.035, grain=.05, band=.55, band_pos=-.15, band_w=.34, edge=.26, scratches=26)
    add("hero_dark_steel", "iron", (58, 64, 80), (176, 188, 214), streak=.10, blotch=.12, band=.38)
    add("hero_white_steel", "silver", (196, 204, 218), (255, 255, 255), streak=.07, blotch=.05)
    add("hero_old_gold", "gold", (168, 128, 52), (236, 204, 118), blotch=.16, band=.5)
    add("hero_brass", "bronze", (184, 137, 58), (236, 200, 124), blotch=.18, band=.52)
    add("hero_ebony", "wood", (54, 38, 32), (116, 84, 62))
    add("hero_walnut", "wood", (96, 62, 40), (168, 120, 78))
    add("hero_dark_leather", "leather", (52, 36, 30), (110, 80, 62))
    add("hero_cord", "cloth", (176, 40, 52), (240, 120, 120))
    add("hero_gem", "crystal", (80, 200, 140), (230, 255, 240))


# ================================================================ the family marks
# The Big Dipper: the seven stars of the "Seven Star" name (the Japanese word for the Big Dipper is the Seven Stars; from memory, stated in the doc). Positions are
# the real ones, projected flat: x = (right ascension in degrees - 185) * cos(56 degrees), y = declination - 55 (Dubhe 11h04m +61.8, Merak 11h02m +56.4, Phecda
# 11h54m +53.7, Megrez 12h15m +57.0, Alioth 12h54m +56.0, Mizar 13h24m +54.9, Alkaid 13h48m +49.3).
DIPPER = {"Dubhe": (-10.7, 6.75), "Merak": (-11.0, 1.38), "Phecda": (-3.6, -1.31), "Megrez": (-0.6, 2.03),
          "Alioth": (4.8, 0.96), "Mizar": (8.9, -0.07), "Alkaid": (12.2, -5.69)}
DIPPER_LINES = [("Dubhe", "Merak"), ("Merak", "Phecda"), ("Phecda", "Megrez"), ("Megrez", "Dubhe"), ("Megrez", "Alioth"), ("Alioth", "Mizar"), ("Mizar", "Alkaid")]


def dipper(scale=1.0, rotate_deg=0.0, centre="Megrez", mirror=False):
    """The seven stars as {name: (u, v)} and the segments as [((u, v), (u, v))], centred on one star (the gem sits on it), scaled and turned."""
    c = DIPPER[centre]
    a = math.radians(rotate_deg)
    ca, sa = math.cos(a), math.sin(a)
    pts = {}
    for k, (x, y) in DIPPER.items():
        x, y = (x - c[0]) * scale, (y - c[1]) * scale
        if mirror:
            x = -x
        pts[k] = (x * ca - y * sa, x * sa + y * ca)
    return pts, [(pts[a_], pts[b_]) for a_, b_ in DIPPER_LINES]


def ring_points(n, r, start_deg=90.0):
    """n points on a circle of radius r as a viewer sees a face (right, up), counter-clockwise from `start_deg` (90 = straight up)."""
    return [(r * math.cos(math.radians(start_deg + 360.0 * i / n)), r * math.sin(math.radians(start_deg + 360.0 * i / n))) for i in range(n)]


def heptagon(r, start_deg=90.0):
    """The seven corners of a regular heptagon (the star bezel), counter-clockwise from `start_deg`."""
    return [(r * math.cos(math.radians(start_deg + 360.0 * i / 7)), r * math.sin(math.radians(start_deg + 360.0 * i / 7))) for i in range(7)]


# ================================================================ geometry helpers
def unit(v):
    v = V(v, float)
    return v / max(float(np.linalg.norm(v)), 1e-9)


def add(m, name, material, **kw):
    return w.add(m, name, material, **kw)


def cabochon(part, centre, r, height, axis="z", sides=16, squash=1.0):
    """A cabochon: a smooth low dome standing out of a surface. `axis` ('z', '-z', 'x', '-x', 'y' ...) is the way it faces; its base is buried a little."""
    prof = [(-height * 0.25, r * 0.90), (0.0, r), (height * 0.22, r * 0.96), (height * 0.50, r * 0.80), (height * 0.76, r * 0.52), (height * 0.93, r * 0.26), (height, 0.0)]
    return ml.lathe(part, axis, centre, prof, sides=sides, squash=squash)


def star_bezel(part, centre, r, height, axis="z", lip=0.18):
    """The heptagonal setting of a gem (seven sides, the family mark): a seven-sided collar with a stepped, bevelled rim, base buried in the surface."""
    prof = [(-height * 0.4, r * 1.04), (0.0, r * 1.04), (height * 0.35, r), (height * 0.7, r * (1.0 - lip * 0.5)), (height, r * (1.0 - lip))]
    return ml.lathe(part, axis, centre, prof, sides=7, start=math.pi / 2)


def rivet(part, pos, r, normal=(0, 0, 1), sides=6):
    """A low rivet head: a 6-sided dome standing out of a surface along `normal` (12 to 18 quads; the family's studs use it so seven of them stay cheap)."""
    n = unit(normal)
    return ml.lathe(part, tuple(n), tuple(V(pos, float) - n * r * 0.25), [(0.0, r), (r * 0.25, r), (r * 0.62, r * 0.78), (r * 0.95, r * 0.34)], sides=sides, caps=(False, True))


def rivet_ring(part, centre, ring_r, r, n=7, axis="z", start_deg=90.0):
    """n rivets on a circle round a point of a face looking along `axis` (z, -z, x, -x, y, -y), the first at the top of the circle as the viewer sees the face."""
    frames = {"z": ((1, 0, 0), (0, 1, 0), (0, 0, 1)), "-z": ((-1, 0, 0), (0, 1, 0), (0, 0, -1)), "x": ((0, 0, -1), (0, 1, 0), (1, 0, 0)), "-x": ((0, 0, 1), (0, 1, 0), (-1, 0, 0)),
              "y": ((1, 0, 0), (0, 0, -1), (0, 1, 0)), "-y": ((1, 0, 0), (0, 0, 1), (0, -1, 0))}
    e1, e2, nrm = (V(a, float) for a in frames[axis])
    c = V(centre, float)
    for i in range(n):
        t = math.radians(start_deg + 360.0 * i / n)
        rivet(part, tuple(c + e1 * ring_r * math.cos(t) + e2 * ring_r * math.sin(t)), r, tuple(nrm))


def torus(part, centre, R, r, plane="xy", n=28, sides=8, start=0.0, section=None, scales=None, up=None):
    """A ring: a section swept round a circle of radius R in the plane 'xy', 'yz' or 'xz' about `centre` (a closed loop, no caps)."""
    c = V(centre, float)
    pts = []
    for i in range(n + 1):
        a = start + 2 * math.pi * i / n
        ca, sa = R * math.cos(a), R * math.sin(a)
        d = {"xy": (ca, sa, 0.0), "yz": (0.0, ca, sa), "xz": (ca, 0.0, sa)}[plane]
        pts.append(tuple(c + V(d)))
    nrm = {"xy": (0, 0, 1), "yz": (1, 0, 0), "xz": (0, 1, 0)}[plane]
    sec = section if section is not None else ml.circle(r, sides)
    return ml.sweep(part, pts, sec, scales=scales, caps=(False, False), up=up or nrm)


def tube(m, name, material, pts, r0, r1=None, sides=8, base=None, glow=False, smooth=60, end_point=False, wear=0.5, thick=1.0, caps=(True, True)):
    """A round tube along a polyline, radius r0 at the start and r1 at the end (end_point closes the last few percent to a point)."""
    p = add(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft", wear=wear)
    r1 = r0 if r1 is None else r1

    def sc(t):
        r = r0 + (r1 - r0) * t
        if end_point and t > 0.97:
            return (0.0, 0.0)
        return (r, r * thick)
    ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc, caps=caps, up=(0, 0, 1))
    return p


def spike(part, base, direction, length, r0, r1=0.0, sides=6, caps=(True, False)):
    """A cone / pyramid from `base` along `direction` (a thorn, a finial, a claw tooth): radius r0 at the base, r1 at the tip."""
    return ml.loft(part, tuple(unit(direction)), tuple(base), [(0.0, r0, r0), (length, r1, r1)], ml.circle(1.0, sides), caps=caps)


def bezier(p0, p1, p2, p3, n):
    """n + 1 points of a cubic Bezier curve (a lash, a claw, a horn): smooth curves from four control points."""
    P = [V(p, float) for p in (p0, p1, p2, p3)]
    out = []
    for i in range(n + 1):
        t = i / n
        a = (1 - t) ** 3
        b = 3 * (1 - t) ** 2 * t
        c = 3 * (1 - t) * t ** 2
        d = t ** 3
        out.append(tuple(a * P[0] + b * P[1] + c * P[2] + d * P[3]))
    return out


# ================================================================ painting helpers
def glow_all(atlas, part, rgb, strength=0.5, core=0.3):
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))


def enamel_rect(atlas, part, axis, sign, box, enamel=(24, 46, 116), border=(226, 234, 248), border_w=1.1, inset_w=0.0, glow_border=None, round_r=2.0, bevel=0.0):
    """Paints an enamel inlay on a flat face of a part: the rectangle `box` = (a0, b0, a1, b1) in the coordinates a viewer in front of the +-`axis` face sees (right, up),
    a deep enamel field with a soft mottling inside a bright line `border_w` wide, rounded at the corners by `round_r`. The group's texels carry those coordinates in
    c.A (right) and c.B (up). With `bevel` > 0 (texels) the field reads as a sunk plate rather than a flat colour: the inner lip towards the upper left sits in shadow, the
    lip towards the lower right catches the light, and the field is a little lighter at the bottom."""
    ec = np.array(enamel, np.float32)
    bc = np.array(border, np.float32)
    a0, b0, a1, b1 = box

    def fn(c):
        g = c.g
        if g.kind != "dom" or g.key[0] != "xyz".index(axis) or g.key[1] != sign:
            return
        A, B = c.A, c.B
        ca, cb = (a0 + a1) / 2, (b0 + b1) / 2
        ha_, hb_ = (a1 - a0) / 2 - round_r, (b1 - b0) / 2 - round_r
        qa, qb = np.abs(A - ca) - ha_, np.abs(B - cb) - hb_
        dist = np.sqrt(np.maximum(qa, 0) ** 2 + np.maximum(qb, 0) ** 2) + np.minimum(np.maximum(qa, qb), 0) - round_r       # signed distance to the rounded box
        n = w.noise2(c.img.shape[:2], 6, 41 + int(g.rect[0]) % 19)
        fine = w.noise2(c.img.shape[:2], 1.5, 9)
        field = ec[None, None, :] * (0.78 + 0.46 * n + 0.1 * fine)[..., None]
        inside = dist < -border_w
        line = (dist >= -border_w) & (dist < 0)
        if bevel > 0:
            lip = np.clip(1.0 - (-dist - border_w) / bevel, 0.0, 1.0) * inside                    # 1 at the line, 0 one `bevel` further in
            ul = np.clip(0.5 * (ca - A) / (ha_ + round_r) + 0.5 * (B - cb) / (hb_ + round_r), -1.0, 1.0)      # +1 towards the upper left, -1 towards the lower right
            shade = 1.0 - 0.42 * lip * np.maximum(ul, 0.0) + 0.38 * lip * np.maximum(-ul, 0.0) - 0.10 * (B - cb) / (hb_ + round_r)
            field = np.clip(field * shade[..., None], 0, 255)
        c.img[..., :3] = np.where(inside[..., None], field, c.img[..., :3])
        c.img[..., :3] = np.where(line[..., None], bc[None, None, :] * (0.85 + 0.15 * fine)[..., None], c.img[..., :3])
        if glow_border is not None:
            gl = np.zeros(c.img.shape, np.float32)
            gl[..., :3] = np.array(glow_border, np.float32)
            gl[..., 3] = line.astype(np.float32) * 130
            c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, part, fn)


def paint_cut_gem(atlas, part, rgb, glow=0.6, facets=7):
    """Paints a cut gem built with mesh_lib.gem (a lathe, uv_mode 'loft': the strip runs from the lower point over the girdle to the upper point): `facets` bright and dark
    panels round it, a bright girdle, a deep core, the same colour hot on the glow skin."""
    col = np.array(rgb, np.float32)

    def fn(c):
        T = np.clip(c.T, 0, 1)
        S = c.S
        panel = np.floor(S * facets).astype(np.int64)
        tone = 0.45 + 0.30 * (((panel * 37) % 11) / 10.0)
        girdle = np.exp(-(((T - 0.5) / 0.05) ** 2))
        n = w.noise2(c.img.shape[:2], 4, 77 + int(c.g.rect[0]) % 13)
        body = col[None, None, :] * (tone * (0.55 + 0.45 * (1.0 - np.abs(2 * T - 1)) ** 0.7 + 0.1 * n))[..., None]
        body = body + (255 - body) * (girdle * 0.55)[..., None]
        edge = (np.abs(((S * facets) % 1.0) - 0.5) > 0.47).astype(np.float32)
        body = body + (255 - body) * (edge * 0.35)[..., None]
        c.img[..., :3] = np.clip(body, 0, 255)
        c.img[..., 3] = 255
        hot = col[None, None, :] + (255 - col)[None, None, :] * (0.05 + 0.25 * girdle)[..., None]
        c.glow[..., :3] = hot * (0.72 + 0.28 * tone)[..., None]
        c.glow[..., 3] = 255 * glow
    w.paint_part(atlas, part, fn)


def paint_gem(atlas, part, rgb, glow=0.6):
    """Paints a cabochon built as a lathe (uv_mode 'loft'): a bright heart, a dark rim and a soft inner swirl on the body skin, the same colour hot on the glow skin."""
    col = np.array(rgb, np.float32)

    def fn(c):
        T = np.clip(c.T, 0, 1)                                    # 0 at the base, 1 at the apex
        S = c.S
        n = w.noise2(c.img.shape[:2], 5, 31 + c.g.rect[0] % 17)
        swirl = 0.5 + 0.5 * np.sin(S * 6.2832 * 3 + T * 5.0 + n * 3.0)
        heart = np.clip(T, 0, 1) ** 0.8
        shade_ = 0.20 + 0.50 * heart
        body = col[None, None, :] * (shade_ * (0.82 + 0.18 * swirl))[..., None] + 255 * 0.0
        spot = np.exp(-(((S - 0.18) / 0.07) ** 2 + ((T - 0.74) / 0.12) ** 2))
        body = body + (255 - body) * (spot * 0.7)[..., None]
        c.img[..., :3] = np.clip(body, 0, 255)
        c.img[..., 3] = 255
        hot = col[None, None, :] + (255 - col)[None, None, :] * (0.04 + 0.22 * heart)[..., None]
        c.glow[..., :3] = hot * (0.75 + 0.25 * swirl)[..., None]
        c.glow[..., 3] = 255 * glow
    w.paint_part(atlas, part, fn)


def star_polygon(cx, cy, r_out, r_in, rot_deg=-90.0, points=5):
    """The corners of a star (the Seven Star mark on a shaft): `points` tips on r_out, the notches on r_in, the first tip straight up for rot_deg = -90 on a y-down canvas."""
    out = []
    for i in range(points * 2):
        a = math.radians(rot_deg + 180.0 * i / points)
        r = r_out if i % 2 == 0 else r_in
        out.append((cx + r * math.cos(a), cy + r * math.sin(a)))
    return out


def stars_on_loft(atlas, part, ts, ss_, size, rgb, light=(200, 210, 255), glow_rgb=None, glow_strength=0.5, rot_deg=-90.0):
    """Paints a five-point star at each (T, S) position of a loft / sweep / lathe part (uv_mode 'loft': T = 0..1 along the form, S = 0..1 round it): the star lighter than the
    material with a dark rim, and (with glow_rgb) soft on the glow skin. `size` is the star's outer radius in model units."""
    def fn(c):
        h, wd = c.img.shape[:2]
        sc_ = 4
        big = Image.new("L", (wd * sc_, h * sc_), 0)
        d = ImageDraw.Draw(big)
        ppu = c.g.ppu
        for t, s_ in zip(ts, ss_):
            cx = s_ * wd * sc_
            cy = (1.0 - t) * h * sc_
            d.polygon(star_polygon(cx, cy, size * ppu * sc_, size * ppu * sc_ * 0.42, rot_deg), fill=255)
        m = np.asarray(big.resize((wd, h), Image.LANCZOS), np.float32)[..., None] / 255.0
        ring = np.asarray(big.filter(ImageFilter.MaxFilter(5)).resize((wd, h), Image.LANCZOS), np.float32)[..., None] / 255.0
        c.img[..., :3] *= 1 - 0.55 * np.clip(ring - m, 0, 1)
        c.img[..., :3] = c.img[..., :3] * (1 - m) + np.array(light, np.float32) * m
        if glow_rgb is not None:
            gl = ml.glow_from_mask(m[..., 0], glow_rgb, glow_strength, halo=1.2)
            c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, part, fn)


def engrave(atlas, part, axis, sign, segments=(), dots=(), width=0.5, dot_r=0.9, dark=0.55, rim=0.16, glow_rgb=None, glow_strength=0.9, ss=4, z=None):
    """Engraves lines (and round dots) onto the +- `axis` face group of a part, in MODEL space: segments = [((x, y), (x, y))] and dots = [(x, y)] given as the
    two coordinates a viewer in front of that face sees (right, up); `z` is only used to build the 3D point. The groove darkens the skin and has a pale rim; with
    `glow_rgb` the line and the dots also light up on the glow skin. Draws at `ss` times the resolution and filters down, so thin lines stay smooth."""
    g = atlas.group_for(part, axis, sign)
    x0, y0, gw, gh = g.rect

    def fn(img, glow, grp):
        big = Image.new("L", (gw * ss, gh * ss), 0)
        d = ImageDraw.Draw(big)

        def px(p):
            # a viewer in front of the face sees (a, b) = (right, up); local_px takes a 3D point, so rebuild one with the dominant coordinates
            q = _point_for(axis, sign, p[0], p[1], 0.0 if z is None else z)
            a, b = grp.local_px(q)
            return (a * ss, b * ss)
        for (p0, p1) in segments:
            d.line([px(p0), px(p1)], fill=255, width=max(1, int(round(width * grp.ppu * ss))))
        for p in dots:
            cx, cy = px(p)
            r = dot_r * grp.ppu * ss
            d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
        mk = big.resize((gw, gh), Image.LANCZOS)
        m = np.asarray(mk, np.float32)[..., None] / 255.0
        if dark:
            img[..., :3] *= 1 - dark * m
            m2 = np.asarray(mk.filter(ImageFilter.MaxFilter(3)).filter(ImageFilter.GaussianBlur(0.6)), np.float32)[..., None] / 255.0
            img[..., :3] += 255 * rim * np.clip(m2 - m, 0, 1) * 0.6
        if glow_rgb is not None:
            gl = ml.glow_from_mask(m[..., 0], glow_rgb, glow_strength, halo=max(1.0, width * grp.ppu * 0.8))
            glow[...] = np.maximum(glow, gl)
    atlas.paint_in(g, fn)


def _point_for(axis, sign, a, b, depth):
    """A 3D model point whose dominant-plane coordinates are (a, b) as a viewer in front of the face of `axis` / `sign` sees them (right, up)."""
    if axis == "z":
        return (a if sign >= 0 else -a, b, depth)
    if axis == "x":
        return (depth, b, -a if sign >= 0 else a)
    return (a, depth, b if sign >= 0 else -b)


def dipper_engraving(atlas, part, axis, sign, centre, scale, rotate_deg=0.0, glow_rgb=None, width=0.35, dot_r=0.7, strong=0.9, mirror=False, centre_star="Megrez", skip=None):
    """The Big Dipper on a face: seven glowing stars joined by thin engraved lines, centred on `centre` (right, up) of the face (usually round the gem)."""
    pts, segs = dipper(scale, rotate_deg, centre_star, mirror)
    cx, cy = centre
    segments = [((cx + p[0], cy + p[1]), (cx + q[0], cy + q[1])) for (p, q) in segs]
    dots = [(cx + p[0], cy + p[1]) for k, p in pts.items() if k != skip]
    engrave(atlas, part, axis, sign, segments=segments, dots=dots, width=width, dot_r=dot_r, dark=0.5, rim=0.14, glow_rgb=glow_rgb, glow_strength=strong)
    return pts


def etched_bit(atlas, part, top, bot, yc, d0, d_tip, steel=(154, 163, 178), bevel=(213, 220, 232), inlay=(63, 143, 102), inlay_glow=None, inlay_w=1.5, line=0.80):
    """Paints a crescent bit lofted along x (mesh -x forward) as polished steel with an etched channel: the ground bevels and edge facets bright (`bevel`), the broad face the
    brushed `steel` a little darker inside the channel, and the channel itself (an inlay `inlay_w` wide that follows the outline at `line` of its height, plus a faint inner
    one) filled with the inlay colour, glowing softly when `inlay_glow` is given. `top(d)` and `bot(d)` are the outline functions of the distance from the haft axis; the face is
    a +-z dominant group, where the group's (a, b) are the viewer's (right, up)."""
    ic = np.array(inlay, np.float32)
    bv = np.array(bevel, np.float32)

    def fn(c):
        g = c.g
        if g.kind != "dom" or g.key[0] != 2:                              # the bevel facets, the edge, the caps: polished bright steel
            c.img[..., :3] = c.img[..., :3] * 0.45 + bv * 0.55
            return
        x = c.A * (1.0 if g.key[1] > 0 else -1.0)                          # model x of every texel
        d = np.abs(x)
        yrel = c.B - yc
        h = np.where(yrel >= 0, top(d), bot(d))
        u = np.abs(yrel) / np.maximum(h, 1e-3)                            # 0 on the centre line, 1 at the edge
        inside = (d > d0 + 4.0) & (d < d_tip - 3.0)
        wu = inlay_w / np.maximum(h, 1.0)                                  # the channel's width as a share of the local height
        ch1 = inside & (np.abs(u - line) < wu * 0.5)
        ch2 = inside & (np.abs(u - (line - 0.10)) < wu * 0.22)
        field = inside & (u < line - wu * 0.5)
        n = w.noise2(c.img.shape[:2], 5, 23 + int(g.rect[0]) % 17)
        c.img[..., :3] = np.where(field[..., None], c.img[..., :3] * (0.70 + 0.12 * n[..., None]), c.img[..., :3])
        c.img[..., :3] = np.where(ch1[..., None], ic[None, None, :] * (0.8 + 0.4 * n[..., None]), c.img[..., :3])
        c.img[..., :3] = np.where(ch2[..., None], c.img[..., :3] * 0.6, c.img[..., :3])
        outer = inside & (u >= line + wu * 0.5)
        c.img[..., :3] = np.where(outer[..., None], c.img[..., :3] * 0.7 + bv * 0.3, c.img[..., :3])
        if inlay_glow is not None:
            gl = np.zeros(c.img.shape, np.float32)
            gl[..., :3] = np.array(inlay_glow, np.float32)
            gl[..., 3] = ch1.astype(np.float32) * 105
            c.glow[...] = np.maximum(c.glow, gl)
    w.paint_part(atlas, part, fn)


def braid(atlas, part, dark=0.5, period_a=2.2, period_b=3.2, slant=1.0, light=0.18, base=None):
    """A braided look on a loft / sweep (uv_mode 'loft'): slanted strands of alternating light and dark, left and right-handed halves crossing (a leather braid)."""
    def fn(c):
        A, B = c.A, c.B
        s1 = ((A / period_a + slant * B / period_b) % 1.0) < 0.5
        s2 = ((A / period_a - slant * B / period_b) % 1.0) < 0.5
        weave = np.where(s1 ^ s2, 1.0, 0.0).astype(np.float32)
        edge = np.abs(((A / period_a + slant * B / period_b) % 1.0) - 0.5) * 2
        k = (1 - dark) + dark * (0.35 + 0.65 * weave) * (0.7 + 0.3 * edge)
        c.img[..., :3] = c.img[..., :3] * k[..., None] + light * 255 * (weave * (1 - edge))[..., None] * 0.4
    w.paint_part(atlas, part, fn)


# ================================================================ the 16x16 sprite
class Sprite(Canvas):
    """The relic icons' Canvas with a few helpers for a weapon drawn on the sprite's diagonal (butt low left, tip up right)."""

    def thick_line(self, x0, y0, x1, y1, c, c2=None, width=1, edge=None):
        """A line `width` px thick (extra pixels go up-left, the lit side) with an optional darker lower-right edge pixel colour."""
        n = int(max(abs(x1 - x0), abs(y1 - y0)))
        for i in range(n + 1):
            t = i / max(1, n)
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            col = c if c2 is None else mix(c, c2, t)
            self.put(x, y, col)
            for k in range(1, width):
                self.put(x - k * 0.5, y - k * 0.5, mix(col, (255, 255, 255), 0.18))
            if edge is not None:
                self.put(x + 1, y, edge)

    def block(self, x0, y0, x1, y1, c, hi=None, lo=None):
        """A filled rectangle with a lit top / left edge (hi) and a shaded bottom / right edge (lo)."""
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                col = c
                if hi is not None and (y == int(y0) or x == int(x0)):
                    col = hi
                if lo is not None and (y == int(y1) or x == int(x1)):
                    col = lo
                self.put(x, y, col)

    def rotrect(self, cx, cy, hu, hv, colour_fn):
        """A rectangle turned 45 degrees to sit on the diagonal: centre (cx, cy) in pixels, half length `hu` along mesh +x (down-right on the sprite: across the weapon) and
        half length `hv` along mesh +y (up-right: along the weapon). colour_fn(u, v) -> colour or None for each covered pixel."""
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                u = (dx + dy) * 0.70710678
                v = (dx - dy) * 0.70710678
                if abs(u) <= hu and abs(v) <= hv:
                    c = colour_fn(u, v)
                    if c is not None:
                        self.put(x, y, c)

    def band(self, x0, y0, n, width, colour_fn):
        """A staircase band running down-right (across the weapon on the sprite's diagonal): row y0 + k covers x0 + k .. x0 + k + width - 1 for k < n.
        colour_fn(k, dx) -> colour or None. The classic pixel-art head of a hammer or an axe across a diagonal haft."""
        for k in range(n):
            for dx in range(width):
                c = colour_fn(k, dx)
                if c is not None:
                    self.put(x0 + k + dx, y0 + k, c)

    def shape(self, cx, cy, colour_fn, reach=9):
        """Rasterises any shape given in the weapon's own frame: for every pixel within `reach` of (cx, cy) the call colour_fn(u, v) is made, u = distance along mesh +x
        (down-right on the sprite: across the weapon, forward is negative) and v along mesh +y (up-right: along the weapon), both in pixels; it returns a colour or None."""
        for y in range(16):
            for x in range(16):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                u = (dx + dy) * 0.70710678
                v = (dx - dy) * 0.70710678
                if abs(u) <= reach and abs(v) <= reach:
                    c = colour_fn(u, v)
                    if c is not None:
                        self.put(x, y, c)

    def gem(self, x, y, rgb, size=2):
        """A glowing gem of 1 to 3 px: dark rim colour, bright body, a white highlight pixel."""
        base = tuple(int(v) for v in rgb)
        dark = shade(base, 0.55)
        light = mix(base, (255, 255, 255), 0.65)
        if size >= 3:
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if abs(dx) + abs(dy) < 2 or (dx == 0 and dy == 0):
                        self.put(x + dx, y + dy, base)
            self.put(x, y + 1, dark)
            self.put(x + 1, y, dark)
            self.put(x - 1, y - 1, light)
            self.put(x, y, mix(base, light, 0.5))
        elif size == 2:
            self.put(x, y, base)
            self.put(x + 1, y, dark)
            self.put(x, y + 1, dark)
            self.put(x + 1, y + 1, shade(base, 0.7))
            self.put(x, y, light)
        else:
            self.put(x, y, light)


def save_sprite(item_id, cv, outline):
    """Outlines the canvas by one dark pixel and writes textures/item/<id>.png (16 x 16, transparent ground)."""
    cv.outline(outline)
    os.makedirs(ICON_DIR, exist_ok=True)
    p = os.path.join(ICON_DIR, item_id + ".png")
    cv.image().save(p, optimize=True)
    return p


# ================================================================ item models: the in-hand model and the inventory icon model
BASE_DISPLAY = {          # the base display of every baked weapon (the same numbers as models/item/brooch_pin.json, vanilla's handheld transforms)
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "head": {"rotation": [0, 180, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


def write_item_models(item_id):
    """models/item/<id>.json (builtin/entity, drawn by WeaponRenderer from the mesh; the long-weapon first-person preset of display_presets.py is applied so a long
    weapon never goes through the camera) and models/item/<id>_icon.json (the flat handheld model of the sprite, the inventory icon). Returns the mesh length in blocks."""
    os.makedirs(MODEL_DIR, exist_ok=True)
    model = {"parent": "minecraft:builtin/entity", "gui_light": "front", "textures": {"particle": "nusmp:item/" + item_id}, "display": json.loads(json.dumps(BASE_DISPLAY))}
    mesh = dp.read_mesh(os.path.join(MESH_DIR, item_id + ".txt"))
    plan = dp.plan_for(item_id, model, mesh)
    model = dp.apply_plan(model, plan)
    with open(os.path.join(MODEL_DIR, item_id + ".json"), "w", newline="\n") as f:
        json.dump(model, f, indent=2)
    icon = {"parent": "minecraft:item/handheld", "textures": {"layer0": "nusmp:item/" + item_id}}
    with open(os.path.join(MODEL_DIR, item_id + "_icon.json"), "w", newline="\n") as f:
        json.dump(icon, f, indent=2)
    return plan.get("length", mesh.length), plan


# ================================================================ placing, writing, finishing
def place_diagonal(m, butt_px, tip_px, apparent, grip, s3=0.85):
    """Places the mesh on the sprite's diagonal like Mesh.place_on_sprite, but from two chosen sprite points instead of the principal axis of the pixels (so a
    wide head cannot skew the angle). butt_px / tip_px are sprite pixel coordinates (x right, y DOWN, 0..16); `apparent` is the weapon's length in blocks as a viewer
    sees it in the hand (mesh length times the third-person scale s3); the weapon grows about the share `grip` of its length from the butt (the hand stays put)."""
    bx, by = butt_px[0] / 16.0, 1.0 - butt_px[1] / 16.0
    tx, ty = tip_px[0] / 16.0, 1.0 - tip_px[1] / 16.0
    length0 = math.hypot(tx - bx, ty - by)
    angle = math.degrees(math.atan2(ty - by, tx - bx))
    ymin, ymax = m.y_range()
    scale0 = length0 / max(ymax - ymin, 1e-6)
    kk = apparent / (s3 * length0)
    d = np.array([math.cos(math.radians(angle)), math.sin(math.radians(angle))])
    origin = np.array([bx, by]) + d * (grip * length0) * (1 - kk)
    m.place(float(origin[0]), float(origin[1]), angle, scale0 * kk, ymin)
    return m.place_cfg


def finish(item_id, m, atlas, butt_px, tip_px, apparent, grip, grip_mid_y, glow=("FFFFFF", 0.14, 0.45, 1.0)):
    """Places the mesh, writes the mesh + both skins + the two item models. `grip_mid_y` is the height (mesh units) of the middle of the grip: the mesh states it as its own
    `grip <fraction>` directive (the share of the length, from the low end, where the hand holds), replacing the one mesh_lib guesses from the part names, so the game never
    has to decide which end is the grip (a value above 0.5 would make the runtime turn the weapon over). Returns a stats dict."""
    place_diagonal(m, butt_px, tip_px, apparent, grip)
    m.set_glow(*glow)
    os.makedirs(MESH_DIR, exist_ok=True)
    path = os.path.join(MESH_DIR, item_id + ".txt")
    n = m.write_txt(path, atlas, normals="vertex", comment="generated by tools/heroweapon/art_hart2.py")
    ymin, ymax = m.y_range()
    frac = (grip_mid_y - ymin) / max(ymax - ymin, 1e-6)
    set_grip_directive(path, frac)
    atlas.save(item_id)
    length_blocks, _plan = write_item_models(item_id)
    st = m.stats(atlas)
    st.update(quads_written=n, atlas_ppu=round(atlas.ppu, 2), length_blocks=round(length_blocks, 3), grip_fraction=round(frac, 3))
    return st


def set_grip_directive(path, fraction):
    """Writes `grip <fraction>` into a mesh file (after its `place` line), replacing a `grip` line that mesh_lib guessed from the part names."""
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")
    lines = [ln for ln in lines if not ln.startswith("grip ")]
    at = next(i for i, ln in enumerate(lines) if ln.startswith("place "))
    lines.insert(at + 1, "grip %.3f" % fraction)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


def lay(m, size=512, ppu_max=24.0):
    """Lays out and paints the skin of a mesh (the owner's Atlas): returns the atlas, every part painted with its material."""
    atlas = ml.Atlas(size)
    atlas.layout(m, ppu_max=ppu_max)
    atlas.paint(m)
    return atlas


def sha(paths):
    h = hashlib.sha256()
    for p in sorted(paths):
        with open(p, "rb") as f:
            h.update(os.path.basename(p).encode())
            h.update(f.read())
    return h.hexdigest()[:16]


def outputs_of(item_id):
    return [os.path.join(MESH_DIR, item_id + ".txt"), os.path.join(SKIN_DIR, item_id + ".png"), os.path.join(SKIN_DIR, item_id + "_glow.png"),
            os.path.join(ICON_DIR, item_id + ".png"), os.path.join(MODEL_DIR, item_id + ".json"), os.path.join(MODEL_DIR, item_id + "_icon.json")]


# ================================================================ the size / orientation audit against the class fragment
def class_data():
    """tools/relics/weapon_classes.json with this builder's fragment merged in memory (the shared file is never edited here)."""
    data = wc.load()
    if os.path.exists(CLASS_FRAGMENT):
        frag = json.load(open(CLASS_FRAGMENT, encoding="utf-8"))
        for k, v in frag.get("classes", {}).items():
            data["classes"][k] = v
        for k, v in frag.get("weapons", {}).items():
            data["weapons"][k] = v
    return data


def audit(item_id):
    """The numbers of tools/relics/audit_weapon_scale.py for one mesh (class, apparent length against the class range, head, flags, status)."""
    return aws.audit_one(item_id, class_data())


# ================================================================ previews
def preview(item_id, out_dir=None, views=("profile", "three_quarter", "edge", "top"), focus=None, name=None, sprite=True, held=True):
    """Renders the written mesh with its skins: the mesh views, then (to the right) the third-person held view next to Steve and the 16 x 16 sprite enlarged.
    Returns the PNG path."""
    out_dir = out_dir or PREVIEW_DIR
    os.makedirs(out_dir, exist_ok=True)
    tmp = os.path.join(out_dir, "_mesh_%s.png" % item_id)
    ml.preview(item_id, None, tmp, views=views, normals="vertex", focus=focus)
    a = Image.open(tmp).convert("RGB")
    os.remove(tmp)
    tiles = [a]
    if held:
        md = wo.read_mesh_file(os.path.join(MESH_DIR, item_id + ".txt"))
        img, _ln = ws.held_tile(md, item_id, w=330, h=min(a.height, 540))
        tiles.append(img.resize((int(img.width * a.height / img.height), a.height)) if img.height != a.height else img)
    if sprite:
        s = Image.open(os.path.join(ICON_DIR, item_id + ".png")).convert("RGBA").resize((16 * 12, 16 * 12), Image.NEAREST)
        bg = Image.new("RGBA", s.size, (84, 89, 107, 255))
        tiles.append(Image.alpha_composite(bg, s).convert("RGB").crop((0, 0, 192, 192)))
    W = sum(t.width for t in tiles)
    sheet = Image.new("RGB", (W, a.height), (40, 44, 56))
    x = 0
    for t in tiles:
        sheet.paste(t, (x, 0))
        x += t.width
    p = os.path.join(out_dir, (name or item_id) + ".png")
    sheet.save(p)
    return p


# ================================================================ held views with the real textures (a bigger, textured version of weapon_sheet's held tile, and a first-person view)
import preview_mesh as pm                                          # noqa: E402


def _model_chain(item_id, hand="right"):
    """The 4 x 4 matrix mesh space -> player model space of the third-person right hand (arm pose, ItemInHandLayer, the model json's display transform, WeaponRenderer's place)."""
    md = wo.read_mesh_file(os.path.join(MESH_DIR, item_id + ".txt"))
    rot, tr, sc = ws.display_of(item_id)
    return ws.hand_matrix(rot, tr, sc) @ ws.place_matrix(md["place"]), md


def held_render(item_id, out_png, px_per_block=300, size=(760, 640), view="side"):
    """The third-person held view with the real skin and glow: Steve (head, body, legs, the right arm in its pose) seen from the player's right, facing right, and the weapon
    in his hand. view = 'side' (from the right: what onlookers see) or 'front' (from in front of the player). Returns the PNG path."""
    M, md = _model_chain(item_id)
    parsed = pm.parse_game_txt(os.path.join(MESH_DIR, item_id + ".txt"))
    td = SKIN_DIR
    skin = os.path.join(td, item_id + ".png")
    glow_tex = os.path.join(td, item_id + "_glow.png")
    W, H = size
    ss = 2
    k = px_per_block * ss
    img = np.zeros((H * ss, W * ss, 3), np.float32) + np.array([0.30, 0.32, 0.38], np.float32)
    img *= (0.8 + 0.4 * np.linspace(1, 0, H * ss, dtype=np.float32))[:, None, None]
    d = ImageDraw.Draw(Image.new("RGB", (1, 1)))
    # the player: drawn first as flat polygons in the same screen mapping
    ground = H * ss - 40 * ss
    cx = W * ss * (0.42 if view == "side" else 0.5)

    def scr(y, z, x=0.0):
        if view == "side":
            return (cx - z * ws.ENTITY_SCALE * k, ground - (1.5 - y) * ws.ENTITY_SCALE * k)
        return (cx + (-x if False else -x) * ws.ENTITY_SCALE * k, ground - (1.5 - y) * ws.ENTITY_SCALE * k)
    pil = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8))
    dr = ImageDraw.Draw(pil)
    dr.line([(8, ground), (W * ss - 8, ground)], fill=(40, 44, 54), width=3 * ss)
    if view == "side":
        for name, pts in ws.steve_polys().items():
            dr.polygon([scr(y, z) for z, y in pts], fill=(56, 64, 84) if name != "head" else (74, 90, 116))
        dr.polygon([scr(y, z) for z, y in ws.arm_poly()], fill=(88, 100, 128))
    img = np.asarray(pil, np.float32) / 255.0
    zb = np.full(img.shape[:2], -1e9, np.float32)
    sk, gt = pm._tex(skin), (pm._tex(glow_tex) if os.path.exists(glow_tex) else None)
    glow = parsed["glow"]
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else np.ones(3, np.float32)
    L3 = M[:3, :3]
    for layer in (0, 2):
        for (ly, P, UV, N) in parsed["quads"]:
            if ly != layer:
                continue
            Q = (M @ np.hstack([P, np.ones((4, 1))]).T).T[:, :3]                   # player model space: x to the player's left, y down, forward -z
            Nn = (L3 @ N.T).T
            if view == "side":
                S = np.stack([cx - Q[:, 2] * ws.ENTITY_SCALE * k, ground - (1.5 - Q[:, 1]) * ws.ENTITY_SCALE * k, -Q[:, 0]], axis=1)
                Nv = np.stack([-Nn[:, 2], -Nn[:, 1], -Nn[:, 0]], axis=1)
            else:
                S = np.stack([cx + Q[:, 0] * ws.ENTITY_SCALE * k * -1.0, ground - (1.5 - Q[:, 1]) * ws.ENTITY_SCALE * k, -Q[:, 2]], axis=1)
                Nv = np.stack([-Nn[:, 0], -Nn[:, 1], -Nn[:, 2]], axis=1)
            nl = np.linalg.norm(Nv, axis=1, keepdims=True)
            Nv = Nv / np.maximum(nl, 1e-9)
            for tri in ((0, 1, 2), (0, 2, 3)):
                a = (S[tri[1], 0] - S[tri[0], 0]) * (S[tri[2], 1] - S[tri[0], 1]) - (S[tri[2], 0] - S[tri[0], 0]) * (S[tri[1], 1] - S[tri[0], 1])
                if abs(a) < 1e-9 or a > 0:
                    continue
                uv = UV[list(tri)] * ((sk.shape[1] / 128.0) if layer == 0 else (gt.shape[1] / 128.0 if gt is not None else 1.0))
                pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], sk if layer == 0 else gt, np.array((0.7, 0.7, 0.7), np.float32), "body" if layer == 0 else "glow", None) if (layer == 0 or gt is not None) else None
    img = np.clip(img, 0, 1).reshape(H, ss, W, ss, 3).mean(axis=(1, 3))
    Image.fromarray((img * 255).astype(np.uint8)).save(out_png)
    return out_png


def first_person_render(item_id, out_png, size=(1280, 720), crop=None, hand="right"):
    """The first-person view of the item as the game frames it: the camera at the origin looking down -z with a 70 degree vertical field of view, the hand at vanilla's
    resting position, the item under the model json's firstperson display transform (display_presets' chain: c = H + T + R * (s * (p - P)) with p the item-cube point of a
    mesh vertex). A software rasteriser: textures are interpolated affinely in screen space (a little swimming on very long quads), depth is 1/z. Returns the PNG path."""
    md = wo.read_mesh_file(os.path.join(MESH_DIR, item_id + ".txt"))
    parsed = pm.parse_game_txt(os.path.join(MESH_DIR, item_id + ".txt"))
    model = json.load(open(os.path.join(MODEL_DIR, item_id + ".json"), encoding="utf-8"))
    d = model["display"]["firstperson_righthand" if hand == "right" else "firstperson_lefthand"]
    R = np.array(dp.rot_matrix(*d["rotation"]))
    s = d["scale"][0]
    T = np.array(d["translation"], np.float64) / 16.0
    Hh = np.array(dp.HAND, np.float64)
    P0 = np.array([0.5, 0.5, 0.5])
    ox, oy, ang, sc, ymin = md["place"]
    a = math.radians(ang - 90.0)
    Rz = np.array([[math.cos(a), -math.sin(a), 0], [math.sin(a), math.cos(a), 0], [0, 0, 1.0]])
    W, H = size
    ss = 2
    f = (H * ss / 2) / math.tan(math.radians(35.0))
    img = np.zeros((H * ss, W * ss, 3), np.float32) + np.array([0.46, 0.60, 0.80], np.float32)
    img *= (0.85 + 0.3 * np.linspace(1, 0, H * ss, dtype=np.float32))[:, None, None]
    zb = np.full(img.shape[:2], -1e9, np.float32)
    skin, glow_tex = os.path.join(SKIN_DIR, item_id + ".png"), os.path.join(SKIN_DIR, item_id + "_glow.png")
    sk, gt = pm._tex(skin), (pm._tex(glow_tex) if os.path.exists(glow_tex) else None)
    glow = parsed["glow"]
    tint = np.array([(glow[0] >> 16) & 255, (glow[0] >> 8) & 255, glow[0] & 255], np.float32) / 255 * glow[3] if glow else np.ones(3, np.float32)
    worst = 0.0
    for layer in (0, 2):
        for (ly, Pq, UV, N) in parsed["quads"]:
            if ly != layer:
                continue
            m0 = Pq * np.array([1, 1, 1.0]) - np.array([0.0, ymin, 0.0])
            item = (Rz @ (m0 * sc).T).T + np.array([ox, oy, 0.5])                     # item cube
            cam = Hh + T + (R @ (s * (item - P0)).T).T
            nrm = (R @ (Rz @ N.T)).T
            if np.any(cam[:, 2] > -0.05):
                worst = max(worst, float(cam[:, 2].max()))
                continue
            S = np.stack([W * ss / 2 + f * cam[:, 0] / -cam[:, 2], H * ss / 2 - f * cam[:, 1] / -cam[:, 2], 1.0 / -cam[:, 2]], axis=1)
            Nv = nrm / np.maximum(np.linalg.norm(nrm, axis=1, keepdims=True), 1e-9)
            for tri in ((0, 1, 2), (0, 2, 3)):
                ar = (S[tri[1], 0] - S[tri[0], 0]) * (S[tri[2], 1] - S[tri[0], 1]) - (S[tri[2], 0] - S[tri[0], 0]) * (S[tri[1], 1] - S[tri[0], 1])
                if abs(ar) < 1e-9 or ar > 0:
                    continue
                uv = UV[list(tri)] * ((sk.shape[1] / 128.0) if layer == 0 else (gt.shape[1] / 128.0 if gt is not None else 1.0))
                if layer == 0 or gt is not None:
                    pm._raster(img, zb, S[list(tri)], uv, Nv[list(tri)], sk if layer == 0 else gt, np.array((0.7, 0.7, 0.7), np.float32), "body" if layer == 0 else "glow", None)
    img = np.clip(img, 0, 1).reshape(H, ss, W, ss, 3).mean(axis=(1, 3))
    im = Image.fromarray((img * 255).astype(np.uint8))
    if crop:
        im = im.crop(crop)
    im.save(out_png)
    return out_png, worst

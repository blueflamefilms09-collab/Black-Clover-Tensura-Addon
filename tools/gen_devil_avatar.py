"""The Devil Avatar, the floor boss of the Underworld Gate (entity/avatar/DevilAvatarBoss), in the repo's GeoLite pipeline (tools/geo_builder.py ->
client.geo.GeoDraw, drawn by client/avatar/AvatarRenderer): a high devil of the Spade Kingdom's Underworld, tall (3.2 blocks to the tips of its horns),
gaunt, horned, with long clawed arms, a tattered void-black cloak, a thin tail ending in a spade, a halo of gravity sigils that turns behind the
shoulders, glowing eyes and glowing cracks. Original pixel art, nothing is traced.

    python3 -B tools/gen_devil_avatar.py                    # everything (idempotent)
    python3 -B tools/gen_devil_avatar.py base               # only the named models (base, gates)

Models (GeoSpec.of("devil_avatar", "<name>")):
    base     the Avatar before the Gates of the Tree of Qliphoth open
    gates    the same body at 100% power: a second, larger counter-turning halo with floating sigil tablets, longer horns with a second pair, spines on the
             shoulders and back, wider cloak tatters, more cracks (the renderer scales it 1.15 with the hitbox and swaps at the middle of the transformation)

Two skins of each (the glow layer follows the tint): the Avatar of Lucifero, violet (devil_avatar_<name>), and the Avatar of Megicula, crimson
(devil_avatar_<name>_megicula).

Writes (through tools/geo_builder.py):
    assets/nusmp/geo/entity/devil_avatar_<name>.geo.json
    assets/nusmp/animations/entity/devil_avatar_<name>.animation.json
    assets/nusmp/textures/entity/devil_avatar_<name>[_megicula].png          512 wide at 2 texels per model pixel (the face, the halo and the tablets at 4)
    assets/nusmp/textures/entity/devil_avatar_<name>[_megicula]_glow.png     eyes, cracks, sigils, ember edges (additive, full bright)
and tools/render_preview/scenes.d/devil_avatar.json (python3 -B tools/render_preview/preview.py devil_base_front devil_gates_death ...; the scenes are
named devil_<model>[_m]_<front|back|face|idle|walk|run|attack|cast|cast_big|hurt|transform|death>, _m for the Megicula skin).

Clips (the same names on both models): idle walk run attack cast cast_big hurt transform death. Rotation signs of this frame (see tools/geo_builder.py):
positive x swings a hanging limb FORWARD, negative x folds a knee / leans the body forward; positive z swings a hanging limb towards +x (the model's
left); positive y turns the head towards the model's right. Positions are pixels (y up), scales multiply.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim, ROOT  # noqa: E402
from zogratis_lucius_kit import Pack, Canvas, pal, mix, px, A3, clamp, smooth, lerp, r5, SIN, COS, TAU, SIDES, FACES, FACE_T, edge_cut, strand_id, keys, cyc  # noqa: E402
from spirit_geo import nz, fz, hsh  # noqa: E402

KEY = "devil_avatar"
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "devil_avatar.json")
MODELS = ("base", "gates")


# ------------------------------------------------------------------------------------------------------------------ the two tints
class Tint:
    """Everything that differs between the Avatar of Lucifero and the Avatar of Megicula: palettes (dark -> light) and the glow colours."""

    def __init__(self, name, skin, skin_deep, bone, void, lining, steel, glow, glow_hot, rune):
        self.name, self.skin, self.skin_deep, self.bone, self.void, self.lining, self.steel = name, skin, skin_deep, bone, void, lining, steel
        self.glow, self.glow_hot, self.rune = glow, glow_hot, rune


LUCIFERO = Tint(
    "lucifero",
    skin=((30, 26, 46), (46, 40, 68), (68, 58, 98), (96, 82, 132), (128, 110, 168)),
    skin_deep=((18, 15, 30), (28, 23, 46), (40, 34, 64)),
    bone=((26, 22, 36), (58, 50, 70), (104, 94, 116), (160, 150, 170), (214, 206, 224)),
    void=((6, 5, 12), (12, 10, 24), (20, 17, 40), (34, 28, 64), (52, 44, 92)),
    lining=((24, 12, 52), (40, 20, 84), (62, 30, 124), (90, 46, 170)),
    steel=((18, 16, 30), (36, 32, 58), (62, 56, 96), (98, 90, 138)),
    glow=(168, 96, 255), glow_hot=(236, 206, 255), rune=(190, 130, 255))

MEGICULA = Tint(
    "megicula",
    skin=((40, 18, 24), (62, 26, 34), (90, 38, 46), (124, 54, 62), (160, 76, 82)),
    skin_deep=((24, 10, 14), (38, 15, 20), (54, 22, 28)),
    bone=((30, 20, 22), (64, 44, 46), (110, 86, 84), (166, 140, 132), (222, 202, 190)),
    void=((10, 4, 8), (20, 8, 14), (34, 12, 22), (54, 18, 32), (80, 28, 44)),
    lining=((52, 8, 16), (86, 12, 24), (128, 18, 34), (176, 28, 46)),
    steel=((28, 14, 18), (52, 26, 32), (86, 42, 50), (126, 66, 74)),
    glow=(255, 66, 80), glow_hot=(255, 218, 200), rune=(255, 110, 110))

TINT = {"cur": LUCIFERO}


def T():
    return TINT["cur"]


# ------------------------------------------------------------------------------------------------------------------ painters
def glow_px(c, a):
    return None if a <= 0 else (c[0], c[1], c[2], int(clamp(a, 0, 255)))


def crack_field(i, j, salt, scale=0.16, width=0.045):
    """0..1: how close a texel is to a crack (1 on the line): thin wandering lines from a smooth noise field's zero crossings."""
    v = nz(i * scale + 7, j * scale * 1.15, salt) + 0.55 * nz(i * scale * 2.3, j * scale * 2.3 + 11, salt + 3)
    return clamp(1.0 - abs(v) / width, 0.0, 1.0)


def skin_paint(salt=1, cracks=0.0, deep=False, ribs=False):
    """Gaunt devil skin: a few tone bands, mottling, faint scars, and (cracks > 0) glowing cracks in the tint."""
    def p(face, i, j, fw, fh):
        t = T()
        cols = t.skin_deep if deep else t.skin
        v = j / max(1, fh - 1)
        k = 0.60 + 0.17 * nz(i * 0.3, j * 0.3, salt) + 0.07 * fz(i, j, salt) - 0.20 * v + FACE_T[face] * 0.6
        c = pal(cols, k, i, j, 0.05)
        g = None
        if cracks > 0:
            cr = crack_field(i, j, salt, 0.17, 0.05 * cracks)
            if cr > 0.55:
                hot = cr > 0.85
                c = mix(c, t.glow_hot if hot else t.glow, 0.85 if hot else 0.6)
                g = glow_px(t.glow, 255 if hot else 170)
            elif cr > 0.2:
                c = mix(c, t.glow, 0.25)
                g = glow_px(t.glow, 70)
        return px(c), (px(g[:3], g[3]) if g else None)
    return p


def bone_paint(salt=2, tip_glow=0.0, flow="y"):
    """Horn, claw and spine: ridged bone, dark at the root and pale at the tip; tip_glow lights the last stretch in the tint."""
    def p(face, i, j, fw, fh):
        t = T()
        along = j / max(1, fh - 1) if flow == "y" else i / max(1, fw - 1)
        across = i if flow == "y" else j
        ridge = 0.5 + 0.5 * SIN(across * 1.7 + salt + 0.35 * nz(i * 0.4, j * 0.4, salt))
        k = 0.30 + 0.45 * along + 0.18 * ridge + FACE_T[face] * 0.4 + 0.06 * fz(i, j, salt)
        c = pal(t.bone, k, i, j, 0.05)
        g = None
        if tip_glow > 0 and along > 1.0 - tip_glow:
            a = ((along - (1.0 - tip_glow)) / tip_glow) ** 1.3
            c = mix(c, t.glow, 0.55 * a)
            g = px(t.glow, 200 * a)
        return px(c), g
    return p


def cloak_paint(salt=3, edges="", depth=6, lining_front=True, ember=True, runes=0.0, width=2.0):
    """The void-black cloak: pleats of near black, a lining in the tint on the face that looks at the body, ragged spiky cut edges (edges = 'b', 't',
    'l', 'r'), and a faint ember glow along every cut edge."""
    def p(face, i, j, fw, fh):
        t = T()
        if edges and face in SIDES + ("top",) and edge_cut(face, i, j, fw, fh, edges, depth, salt, width, 1.6):
            return None, None
        v = j / max(1, fh - 1)
        f = SIN((i + 2.4 * SIN(j * 0.11 + salt)) * TAU / 8.0 + salt * 1.3)
        k = 0.34 + 0.20 * f * (0.7 + 0.3 * SIN(j * 0.07 + salt * 2)) - 0.12 * v + 0.12 * nz(i * 0.3, j * 0.3, salt) + FACE_T[face] * 0.4
        cols = t.lining if (lining_front and face == "front") else t.void
        if cols is t.lining:
            k = 0.25 + 0.4 * (0.5 + 0.5 * f) * (1.0 - 0.5 * v) + 0.08 * nz(i * 0.3, j * 0.3, salt + 4)
        c = pal(cols, k, i, j, 0.05)
        g = None
        if ember and edges and face in SIDES:
            # a texel next to a cut texel glows faintly: the tatters smoulder
            near = False
            for (di, dj) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ii, jj = i + di, j + dj
                if 0 <= ii < fw and 0 <= jj < fh and edge_cut(face, ii, jj, fw, fh, edges, depth, salt, width, 1.6):
                    near = True
                    break
            if near:
                c = mix(c, t.glow, 0.5)
                g = px(t.glow, 150)
        if runes > 0 and face == "back" and hsh(i // 3, j // 5, salt + 9) > 1.0 - runes and nz(i * 0.2, j * 0.2, salt) > 0.05:
            c = mix(c, t.rune, 0.7)
            g = px(t.rune, 190)
        return px(c), g
    return p


def steel_paint(salt=5, glow_line=True):
    """Dark worked metal with a lit edge: belts, bracers, collar studs."""
    def p(face, i, j, fw, fh):
        t = T()
        v = j / max(1, fh - 1)
        k = 0.62 - 0.4 * v + FACE_T[face] * 0.5 + 0.08 * fz(i, j, salt)
        if (i == 0 or j == 0) and fw > 3 and fh > 3:
            k += 0.2
        if (i == fw - 1 or j == fh - 1) and fw > 3 and fh > 3:
            k -= 0.2
        c = pal(t.steel, k, i, j, 0.05)
        g = None
        if glow_line and fh >= 4 and j == fh // 2 and 2 <= i < fw - 2 and face in SIDES:
            c = mix(c, t.glow, 0.7)
            g = px(t.glow, 200)
        return px(c), g
    return p


def blade_paint(salt=8, tip=0.55):
    """A blade of the halo's crown: dark metal from a lit root to a tip that burns in the tint."""
    def p(face, i, j, fw, fh):
        t = T()
        along = j / max(1, fh - 1)
        k = 0.55 + 0.25 * (1.0 - along) + FACE_T[face] * 0.4 + 0.06 * fz(i, j, salt)
        c = pal(t.steel, k, i, j, 0.05)
        g = None
        if along > 1.0 - tip:
            a = ((along - (1.0 - tip)) / tip) ** 1.1
            c = mix(c, t.glow, 0.7 * a)
            g = px(t.glow, 230 * a)
        return px(c), g
    return p


def solid_glow(rgb_fn, a=255):
    """A plain glowing block (an eye, a core): the colour layer and the glow layer both in the tint."""
    def p(face, i, j, fw, fh):
        c = rgb_fn()
        return px(c), px(c, a)
    return p


# ------------------------------------------------------------------------------------------------------------------ the build helper
def rot_file(v, rx, ry, rz):
    """Where the file-frame vector v goes under a cube / bone rotation (rx, ry, rz) degrees, exactly as client.geo.GeoDraw applies it."""
    x, y, z = v[0], -v[1], v[2]
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(-rz)
    c, s_ = math.cos(ax), math.sin(ax)
    y, z = y * c - z * s_, y * s_ + z * c
    c, s_ = math.cos(ay), math.sin(ay)
    x, z = x * c + z * s_, -x * s_ + z * c
    c, s_ = math.cos(az), math.sin(az)
    x, y = x * c - y * s_, x * s_ + y * c
    return (x, -y, z)


class Build:
    """A model under construction: bones, boxes (with mirrored pairs), animation."""

    def __init__(self, name, tw=512, th=1024, dens=2):
        self.m = Model(KEY, name, tw, th)
        self.pk = Pack(tw, th, dens)
        self.A = Anim()
        self.name = name

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def box(self, bone, origin, size, paint, **kw):
        return self.pk.box(self.m, bone, origin, size, paint, **kw)

    def pair(self, bone_l, bone_r, origin, size, paint, **kw):
        """origin is the +x cube; its mirrored twin (x -> -x) goes on bone_r (the same bone twice for a single bone)."""
        c = self.pk.box(self.m, bone_l, origin, size, paint, **kw)
        self.pk.twin(self.m, bone_r, c)
        return c

    def spike(self, bone, base, w, ln, rx, rz, paint, ry=0.0, key=None, fracs=(0.55, 0.45), taper=0.55, bend=(0.0, 0.0), faces=None, twin=None):
        """A tapering spike of stacked boxes standing at `base` (x, y, z), tilted by (rx, ry, rz); each segment is `taper` times as wide as the one below and
        bent by `bend` more degrees: horns, spines, claws. The next segment starts exactly where the last one ends (rot_file). twin = a bone: the mirrored
        copy goes there."""
        pos = list(base)
        angx, angz = rx, rz
        wd = w
        out = []
        for k, fr in enumerate(fracs):
            sl = max(0.5, r5(ln * fr))
            sw = max(0.5, r5(wd))
            c = self.box(bone, (pos[0] - sw / 2, pos[1], pos[2] - sw / 2), (sw, sl, sw), paint, rotation=(angx, ry, angz), pivot=tuple(pos), faces=faces,
                         key=None if key is None else "%s_%d" % (key, k))
            out.append(c)
            if twin:
                self.pk.twin(self.m, twin, c)
            d = rot_file((0, sl, 0), angx, ry, angz)
            pos = [pos[0] + d[0], pos[1] + d[1], pos[2] + d[2]]
            angx += bend[0]
            angz += bend[1]
            wd *= taper
        return out

    def finish(self):
        self.pk.finish()
        return self.pk.images()

    def repaint(self):
        self.pk.paint_all()
        return self.pk.images()

    def save_geo(self):
        used = self.pk.height_used()
        hh = 64
        while hh < used:
            hh += 64
        self.m.tex_h = hh
        self.m.save(self.A)
        return hh

    def save_textures(self, img, glow, hh, suffix=""):
        img, glow = img.crop((0, 0, self.pk.W, hh)), glow.crop((0, 0, self.pk.W, hh))
        base = self.m.id + suffix
        from PIL import Image  # noqa: F401
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        img.save(os.path.join(ROOT, "textures", "entity", base + ".png"))
        glow.save(os.path.join(ROOT, "textures", "entity", base + "_glow.png"))


# ------------------------------------------------------------------------------------------------------------------ the face, the chest and the halo
def spade_pts(cx, cy, w, h):
    """A spade (the Spade Kingdom's mark) as a polygon, point up: a heart turned over with a stem."""
    pts = []
    for k in range(0, 21):
        a = math.pi * k / 20.0
        pts.append((cx - w / 2.0 * (0.5 + 0.5 * math.cos(math.pi - a)) * 1.0, cy + h * 0.1 - h * 0.45 * math.sin(a)))
    return pts


def face_canvas(gates):
    """The head's front (7 x 7 px at 4 texels = 28 x 28): a gaunt skull of a face, sunk eye sockets with glowing slit eyes, a heavy brow, nose slits, cheek
    cracks that run like tears, a row of teeth along the bottom edge, and (at the Gates) a sigil on the brow."""
    t = T()
    cv = Canvas(28, 28)
    for y in range(28):
        for x in range(28):
            k = 0.52 + 0.10 * nz(x * 0.35, y * 0.35, 4) - 0.20 * (y / 27.0) + (0.0 if 3 <= x <= 24 else -0.16)
            if 7 <= y <= 17 and (x < 9 or x > 18):
                k -= 0.0
            cv.put(x, y, pal(t.skin, k, x, y, 0.06))
    dark = t.skin_deep[0]
    # the brow ridge: a dark bar, lowered towards the middle (a permanent frown)
    cv.poly([(2, 7.0), (13.5, 10.0), (13.5, 12.0), (2, 9.2)], t.skin_deep[0])
    cv.poly([(25.5, 7.0), (14.5, 10.0), (14.5, 12.0), (25.5, 9.2)], t.skin_deep[0])
    cv.line(2, 6.5, 13, 9.5, t.skin[3])
    cv.line(25.5, 6.5, 14.5, 9.5, t.skin[3])
    # the sockets and the eyes: an almond slit, a hot core, a thin white-hot line through the middle
    for (x0, x1, left) in ((3, 12, True), (15, 24, False)):
        cv.poly([(x0, 13), (x0 + 3, 11.5), (x1 + 0.5, 13.5), (x1 - 1.5, 16.0), (x0 + 2, 16.2)] if left else
                [(x1 + 1, 13), (x1 - 2, 11.5), (x0 - 0.5, 13.5), (x0 + 1.5, 16.0), (x1 - 1, 16.2)], t.skin_deep[0])
        ex = list(range(x0 + 1, x1)) if left else list(range(x0 + 1, x1))
        for x in ex:
            u = (x - (x0 + x1) / 2.0) / ((x1 - x0) / 2.0)
            hgt = 1 if abs(u) > 0.55 else 2
            y0 = 13 + (1 if (left and u > 0.3) or (not left and u < -0.3) else 0)
            for y in range(y0, y0 + hgt + 1):
                core = abs(u) < 0.45 and y == y0 + (hgt // 2)
                cv.put(x, y, t.glow_hot if core else t.glow, 255, t.glow_hot if core else t.glow, 255)
    # the nose: two slits
    for x in (13, 14):
        cv.rect(x, 17, x, 20, dark)
    cv.put(12, 20, dark)
    cv.put(15, 20, dark)
    # cheek cracks that run like tears from the eyes, and a crack across the forehead
    for (x, y, dx) in ((5, 17, -1), (22, 17, 1)):
        for k in range(7):
            cv.put(x + (dx if k % 3 == 2 else 0) * (k // 3), y + k, t.glow, 230, t.glow, 220)
    for k in range(5):
        cv.put(13 + (k % 2), 1 + k, t.glow, 200, t.glow, 170)
    if gates:                                                                  # the brow sigil of the open Gates: a spade
        for (x, y) in ((14, 3), (13, 4), (14, 4), (15, 4), (12, 5), (13, 5), (15, 5), (16, 5), (14, 6)):
            cv.put(x, y, t.glow_hot, 255, t.glow_hot, 255)
    # the teeth: a row of small fangs along the bottom edge
    for x in range(4, 24):
        if x % 2 == 0:
            cv.put(x, 24, t.bone[4])
            cv.put(x, 25, t.bone[3])
            cv.put(x, 26, t.bone[2] if 7 < x < 20 else t.bone[1])
    cv.rect(4, 23, 23, 23, dark)
    return cv


def jaw_canvas():
    """The jaw's front (5.2 x 3.3 px at 4 texels = 21 x 13): a dark mouth line, a row of teeth pointing up, and the cracks of the chin."""
    t = T()
    cv = Canvas(21, 13)
    for y in range(13):
        for x in range(21):
            cv.put(x, y, pal(t.skin, 0.38 + 0.1 * nz(x * 0.4, y * 0.4, 6) - 0.12 * y / 12.0, x, y, 0.06))
    cv.rect(1, 0, 19, 1, t.skin_deep[0])
    for x in range(2, 19):
        if x % 2 == 1:
            cv.put(x, 2, t.bone[4])
            cv.put(x, 3, t.bone[3])
    for k in range(6):
        cv.put(10 + (1 if k % 3 == 1 else 0), 6 + k, t.glow, 220, t.glow, 200)
    return cv


def chest_canvas(gates):
    """The ribcage's front (9.5 x 8 px at 4 texels = 38 x 32): sternum, five pairs of ribs in pale bone over a dark hollow, collar bones, and a core of
    light along the sternum that grows to a diamond at the middle."""
    t = T()
    w, h = 40, 32
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            cv.put(x, y, pal(t.skin_deep, 0.55 + 0.2 * nz(x * 0.3, y * 0.3, 8) - 0.3 * (y / h), x, y, 0.06))
    mid = w // 2
    for y in range(h):
        for x in (mid - 1, mid):
            cv.put(x, y, t.bone[2] if (y % 3) else t.bone[1])
    for r in range(5):
        y0 = 7 + r * 5
        for side in (-1, 1):
            for k in range(0, 15):
                x = mid + side * (1 + k)
                y = y0 + (3 * (1 - (1 - k / 14.0) ** 2)) * 1.0 + (0.28 * k)
                cv.put(x, round(y), t.bone[3] if k < 11 else t.bone[2])
                cv.put(x, round(y) + 1, t.bone[1])
    for side in (-1, 1):                                                      # the collar bones
        cv.line(mid + side * 2, 3, mid + side * 17, 1, t.bone[3], width=2)
    for y in range(2, h - 2):                                                 # the core: a glowing seam down the sternum
        a = 150 + 90 * (1.0 - abs(y - h / 2.0) / (h / 2.0))
        cv.put(mid - 1, y, t.glow, 255, t.glow, a)
        cv.put(mid, y, t.glow, 255, t.glow, a)
    for (dx, dy) in ((0, 0), (-1, 0), (1, 0), (0, -1), (0, 1), (-2, 0), (2, 0), (0, -2), (0, 2)):
        cv.put(mid - 1 + dx, 16 + dy, t.glow_hot, 255, t.glow_hot, 255)
    if gates:
        for side in (-1, 1):                                                  # the cracks of the open Gates spread over the ribs
            for k in range(10):
                cv.put(mid + side * (4 + k), 24 - k + (k % 2), t.glow, 255, t.glow, 210)
    return cv


def runes_cell(cv, cx, cy, kind, r, col, hot):
    """One gravity glyph about (cx, cy) in a cell of radius r: 0 an arrow up with a bar, 1 an arrow down, 2 a ring with a dot, 3 a cross in a ring, 4 a spade, 5 an
    hourglass; drawn in the glow colour on the colour layer and the glow layer."""
    def dot(x, y, c=col):
        cv.put(round(x), round(y), c, 255, c, 255)

    def ln(x0, y0, x1, y1, c=col):
        cv.line(x0, y0, x1, y1, c, 255, c, 255)
    if kind == 0:
        ln(cx, cy - r, cx, cy + r * 0.7)
        ln(cx, cy - r, cx - r * 0.6, cy - r * 0.2)
        ln(cx, cy - r, cx + r * 0.6, cy - r * 0.2)
        ln(cx - r * 0.7, cy + r, cx + r * 0.7, cy + r)
    elif kind == 1:
        ln(cx, cy + r, cx, cy - r * 0.7)
        ln(cx, cy + r, cx - r * 0.6, cy + r * 0.2)
        ln(cx, cy + r, cx + r * 0.6, cy + r * 0.2)
        ln(cx - r * 0.7, cy - r, cx + r * 0.7, cy - r)
    elif kind == 2:
        for a in range(0, 360, 20):
            dot(cx + math.cos(math.radians(a)) * r * 0.85, cy + math.sin(math.radians(a)) * r * 0.85)
        dot(cx, cy, hot)
        dot(cx + 1, cy, hot)
    elif kind == 3:
        for a in range(0, 360, 24):
            dot(cx + math.cos(math.radians(a)) * r * 0.9, cy + math.sin(math.radians(a)) * r * 0.9)
        ln(cx - r * 0.55, cy, cx + r * 0.55, cy)
        ln(cx, cy - r * 0.55, cx, cy + r * 0.55)
    elif kind == 4:
        cv.poly(spade_pts(cx, cy - 0.5, r * 1.5, r * 1.8), col, 255, col, 255)
        ln(cx, cy + r * 0.3, cx, cy + r * 0.95)
    else:
        ln(cx - r * 0.8, cy - r * 0.9, cx + r * 0.8, cy - r * 0.9)
        ln(cx - r * 0.8, cy + r * 0.9, cx + r * 0.8, cy + r * 0.9)
        ln(cx - r * 0.8, cy - r * 0.9, cx + r * 0.8, cy + r * 0.9)
        ln(cx + r * 0.8, cy - r * 0.9, cx - r * 0.8, cy + r * 0.9)


def halo_canvas(size, gates, outer=False):
    """A ring of gravity sigils (size x size texels): a thin outer ring and an inner ring of dark metal, between them a band of twelve glyphs (arrows up and
    down, rings, crosses, spades, hourglasses) in the tint that burn through the glow layer, tick marks round the inner edge, and spokes that carry it to a
    hollow middle (transparent: the head shows through). outer=True: a bigger, plainer ring of the Gates (fewer, larger glyphs)."""
    t = T()
    cv = Canvas(size, size)
    c = (size - 1) / 2.0
    ro, ri, rin = size * 0.495, size * 0.425, size * 0.325
    rk = size * 0.285
    steel, hot = t.steel, t.glow_hot
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            col = None
            if ri <= d <= ro:                                                    # the outer band: dark metal, a lit rim, the glyph ground
                k = 0.45 + 0.15 * nz(x * 0.4, y * 0.4, 12)
                if d > ro - 1.6 or d < ri + 1.0:
                    k = 0.9
                col = pal(steel, k, x, y, 0.05)
            elif rin <= d < ri:                                                  # the inner ring and its ticks
                k = 0.5 + 0.12 * nz(x * 0.4, y * 0.4, 14)
                col = pal(steel, k, x, y, 0.05)
            elif rk <= d < rin - 0.5:
                ang = math.degrees(math.atan2(y - c, x - c)) % 360.0
                if abs((ang % (360.0 / (24 if not outer else 12))) - 0.0) < 1.6 or d > rin - 2.5:
                    col = pal(steel, 0.6, x, y, 0.0)
            if col is not None:
                cv.put(x, y, col)
    # the glow of the rims
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c)
            if abs(d - (ro - 0.6)) < 0.6 or abs(d - (ri + 0.5)) < 0.55 or abs(d - rin) < 0.55:
                cv.put(x, y, t.glow, 255, t.glow, 200)
    n = 12 if not outer else 8
    pattern = (4, 0, 1) if not outer else (3, 5)                                # the ring repeats every quarter turn: it can turn a quarter in a loop and not show it
    for k in range(n):
        a = math.radians(360.0 * k / n - 90.0)
        gx, gy = c + math.cos(a) * (ro + ri) / 2.0, c + math.sin(a) * (ro + ri) / 2.0
        runes_cell(cv, gx, gy, pattern[k % len(pattern)], (ro - ri) * 0.36, t.rune, hot)
    ticks = 60 if not outer else 36
    for k in range(ticks):                                                       # the ticks: short glowing lines on the inner ring's face
        a = math.radians(360.0 * k / ticks)
        r0, r1 = rin + 1.0, rin + (3.0 if k % 5 == 0 else 1.6)
        cv.line(c + math.cos(a) * r0, c + math.sin(a) * r0, c + math.cos(a) * r1, c + math.sin(a) * r1, t.glow, 255, t.glow, 255)
    return cv


def tablet_canvas(kind):
    """A floating sigil tablet (4 x 4 px at 4 texels = 16): dark metal with one glyph that burns."""
    t = T()
    cv = Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            cv.put(x, y, pal(t.steel, 0.9 if edge else 0.45 + 0.1 * nz(x * 0.5, y * 0.5, 15), x, y, 0.05))
    runes_cell(cv, 7.5, 7.5, kind, 4.6, t.rune, t.glow_hot)
    return cv


def spade_tip_canvas():
    """The spade at the end of the tail (5 x 6 px at 4 texels = 20 x 24): black stone with a lit rim and a glowing core."""
    t = T()
    cv = Canvas(20, 24)
    cv.poly(spade_pts(9.5, 10.0, 17, 17), t.steel[1])
    cv.poly(spade_pts(9.5, 10.0, 12, 12), t.steel[2])
    cv.poly(spade_pts(9.5, 10.0, 6, 6), t.glow_hot, 255, t.glow, 255)
    cv.rect(9, 17, 10, 22, t.steel[1])
    cv.put(9, 17, t.glow, 255, t.glow, 255)
    cv.put(10, 17, t.glow, 255, t.glow, 255)
    return cv


def canvas_on(canvas_fn, face_name, fallback):
    """A painter that reads a canvas (made fresh for the current tint) on one face and falls back on the others; texels the canvas leaves clear fall back too."""
    cache = {}

    def p(face, i, j, fw, fh):
        key = T().name
        if key not in cache:
            cache[key] = canvas_fn()
        if face == face_name:
            c, g = cache[key].at(i, j)
            if c is not None or g is not None:
                if c is None:
                    base = fallback(face, i, j, fw, fh)
                    return base[0], g
                return c, g
        return fallback(face, i, j, fw, fh)
    return p


def plate_of(canvas_fn):
    """A painter for a thin plate: the canvas on the front face and, mirrored, on the back face; every other face stays clear."""
    cache = {}

    def p(face, i, j, fw, fh):
        key = T().name
        if key not in cache:
            cache[key] = canvas_fn()
        cv = cache[key]
        if face == "front":
            return cv.at(i, j)
        if face == "back":
            return cv.at(fw - 1 - i, j)
        return None, None
    return p


# ------------------------------------------------------------------------------------------------------------------ the body
def build_body(b, gates):
    """Bones and boxes of the Avatar. Heights in pixels, feet at 0, the front at -z, +x the model's left; 16 pixels are a block."""
    sk, skd = skin_paint(1, cracks=0.0), skin_paint(2, cracks=0.0, deep=True)
    skc = skin_paint(3, cracks=1.0 if gates else 0.7)
    bonep = bone_paint(4)
    horn = bone_paint(5, tip_glow=0.25)
    steel = steel_paint(6)
    bones = [("root", None, (0, 0, 0)), ("hips", "root", (0, 24, 0)), ("body", "hips", (0, 26, 0)), ("head", "body", (0, 39.5, 0)),
             ("jaw", "head", (0, 41.0, 1.8)), ("halo", "body", (0, 42, 7.6)),
             ("arm_l", "body", (7.4, 37, 0)), ("forearm_l", "arm_l", (7.4, 26.5, 0)), ("hand_l", "forearm_l", (7.4, 14.5, 0)),
             ("arm_r", "body", (-7.4, 37, 0)), ("forearm_r", "arm_r", (-7.4, 26.5, 0)), ("hand_r", "forearm_r", (-7.4, 14.5, 0)),
             ("leg_l", "root", (3.2, 24, 0)), ("shin_l", "leg_l", (3.2, 13.5, 0)), ("foot_l", "shin_l", (3.2, 3, 0)),
             ("leg_r", "root", (-3.2, 24, 0)), ("shin_r", "leg_r", (-3.2, 13.5, 0)), ("foot_r", "shin_r", (-3.2, 3, 0)),
             ("cloak_top", "body", (0, 39.5, 2.6)), ("cloak_a", "cloak_top", (0, 33, 3.1)), ("tabard", "hips", (0, 26, -2.7)),
             ("tail1", "hips", (0, 24.5, 2.4)), ("tail2", "tail1", (0, 19.0, 2.4)), ("tail3", "tail2", (0, 13.5, 2.4)), ("tail4", "tail3", (0, 8.5, 2.4))]
    strip_x = [-7.8, -5.2, -2.6, 0.0, 2.6, 5.2, 7.8] + ([-10.4, 10.4] if gates else [])
    for k, x in enumerate(strip_x):
        bones.append(("cloak_s%d" % (k + 1), "cloak_a", (x, 21.0, 3.4)))
    if gates:
        bones += [("halo_b", "body", (0, 42, 9.2)), ("horn2_l", "head", (1.2, 47.4, -2.6)), ("horn2_r", "head", (-1.2, 47.4, -2.6)),
                  ("pa3_l", "arm_l", (8.0, 37.6, -2.0)), ("pa3_r", "arm_r", (-8.0, 37.6, -2.0))]
    rest = {"tail1": (-62, 0, 0), "tail2": (14, 0, 0), "tail3": (18, 0, 0), "tail4": (12, 0, 0)}
    for n, par, pv in bones:
        b.bone(n, par, pv, rest.get(n))
    # ---- legs: thin, long, the knee a little forward of the hip's line
    b.pair("leg_l", "leg_r", (1.7, 13.5, -1.8), (3.0, 11.0, 3.6), skd)
    b.pair("shin_l", "shin_r", (1.9, 3.0, -1.4), (2.6, 10.5, 2.8), skd)
    b.pair("shin_l", "shin_r", (1.5, 12.0, -2.2), (3.4, 2.0, 1.2), bonep, key="knee")
    b.pair("foot_l", "foot_r", (1.6, 0.0, -5.5), (3.2, 2.2, 7.0), skd)
    for k in range(3):
        b.pair("foot_l", "foot_r", (1.6 + 1.1 * k, 0.0, -8.0), (0.9, 1.3, 2.6), bonep, key="toe")
    b.pair("foot_l", "foot_r", (2.0, 0.0, 1.0), (2.4, 2.8, 1.2), bonep, key="heel")
    # ---- hips and belt
    b.box("hips", (-4.2, 22.0, -2.4), (8.4, 4.5, 4.8), skd)
    b.box("hips", (-4.6, 25.2, -2.8), (9.2, 1.6, 5.6), steel)
    b.box("hips", (-1.2, 24.8, -3.3), (2.4, 2.4, 0.7), steel, key="buckle")
    # ---- torso
    b.box("body", (-3.2, 26.0, -2.0), (6.4, 5.0, 4.0), skd)
    b.box("body", (-5.0, 30.5, -2.7), (10.0, 8.0, 5.4), canvas_on(lambda: chest_canvas(gates), "front", skc), dens={"front": 4})
    b.box("body", (-1.9, 38.2, -1.6), (3.8, 3.2, 3.2), sk)
    b.box("body", (-6.9, 37.0, -2.0), (13.8, 1.4, 4.0), skd)
    for k, y in enumerate((28.5, 31.3, 34.1, 36.9)):                       # the spine's ridge: bone spikes along the back, leaning back
        b.spike("body", (0, y, 2.2), 1.3, 2.8 + (0.6 if gates else 0), 28, 0, bonep, key="spine%d" % k, fracs=(0.55, 0.45), taper=0.55)
    # ---- the head: skull, brow, jaw, fangs, horns, ears
    b.box("head", (-3.5, 40.5, -3.5), (7.0, 7.0, 7.0), canvas_on(lambda: face_canvas(gates), "front", sk), dens={"front": 4})
    b.box("head", (-3.9, 44.6, -4.0), (7.8, 1.3, 1.1), skd, key="brow")
    b.box("jaw", (-2.6, 38.0, -3.4), (5.2, 3.6, 5.8), canvas_on(jaw_canvas, "front", sk), dens={"front": 4})
    for sx in (-1, 1):
        b.spike("head", (sx * 1.7, 40.7, -3.0), 0.9, 1.9, 0, 0, bonep, key="fang", fracs=(0.6, 0.4), taper=0.5, faces=FACES)   # upper fangs (pointing up: flipped below)
    b.spike("jaw", (0, 38.0, -3.1), 1.2, 3.2, -35, 0, bonep, key="chin", fracs=(0.55, 0.45), taper=0.5)
    for sx in (-1, 1):
        b.box("head", (sx * 3.5 + (0.0 if sx > 0 else -1.8), 43.0, -0.6), (1.8, 1.0, 1.6), sk, rotation=(0, 0, -sx * 18), pivot=(sx * 3.5, 43.5, 0), key="ear")
    # horns: from the temples out, back and up; the Gates grow them longer and add a second pair
    hl = 12.0 if gates else 9.5
    b.spike("head", (2.5, 46.8, -0.2), 2.1, hl, 22, -26, horn, key="horn", fracs=(0.40, 0.34, 0.26), taper=0.62, bend=(16, -10), twin="head")
    if gates:
        b.spike("horn2_l", (1.2, 47.4, -2.6), 1.4, 7.0, -14, -18, horn, key="horn2", fracs=(0.55, 0.45), taper=0.55, bend=(-10, -8), twin="horn2_r")
    # ---- arms: pauldron, spikes, upper arm, bracer, forearm, hand and claws
    b.pair("arm_l", "arm_r", (5.6, 36.0, -2.7), (4.6, 2.4, 5.4), steel, key="pauldron")
    b.spike("arm_l", (7.6, 38.2, -0.8), 1.5, 4.5 + (1.5 if gates else 0), 6, -30, horn, key="pa1", fracs=(0.6, 0.4), taper=0.5, twin="arm_r")
    b.spike("arm_l", (7.6, 38.0, 1.4), 1.2, 3.4, 30, -22, horn, key="pa2", fracs=(0.6, 0.4), taper=0.5, twin="arm_r")
    if gates:
        b.spike("pa3_l", (8.0, 37.6, -2.0), 1.1, 3.0, -28, -35, horn, key="pa3", fracs=(0.6, 0.4), taper=0.5, twin="pa3_r")
    b.pair("arm_l", "arm_r", (6.0, 26.5, -1.6), (2.8, 10.0, 3.2), skc)
    b.spike("arm_l", (8.8, 28.0, 0.0), 1.2, 3.4, 24, -66, bonep, key="elbow", fracs=(0.6, 0.4), taper=0.5, twin="arm_r")
    b.pair("forearm_l", "forearm_r", (6.1, 14.5, -1.5), (2.6, 12.0, 3.0), skc)
    b.pair("forearm_l", "forearm_r", (5.8, 20.5, -1.8), (3.2, 3.6, 3.6), steel, key="bracer")
    b.spike("forearm_l", (7.4, 24.8, 1.4), 1.0, 3.2, 70, 0, bonep, key="spur1", fracs=(0.6, 0.4), taper=0.5, twin="forearm_r")
    b.spike("forearm_l", (7.4, 18.0, 1.4), 1.0, 2.6, 70, 0, bonep, key="spur2", fracs=(0.6, 0.4), taper=0.5, twin="forearm_r")
    b.pair("hand_l", "hand_r", (6.0, 10.5, -1.8), (2.8, 4.2, 3.6), skd)
    for k in range(3):                                                       # three long fingers and their claws
        fx = 6.1 + 0.95 * k
        b.pair("hand_l", "hand_r", (fx, 4.6, -0.6 - 0.5 * (k == 1)), (0.8, 6.0, 0.8), bonep, key="finger")
        b.pair("hand_l", "hand_r", (fx + 0.1, 1.4, -0.5 - 0.5 * (k == 1)), (0.6, 3.4, 0.6), horn, key="claw")
    b.pair("hand_l", "hand_r", (6.2, 6.0, -2.9), (0.8, 4.4, 0.8), bonep, rotation=(32, 0, 0), pivot=(6.6, 10.4, -2.4), key="thumb")
    # ---- the cloak: the mantle and the tall collar, the back, the ragged strips, the front hanging
    cl = cloak_paint(3, edges="b", depth=5, runes=0.05)
    b.box("cloak_top", (-7.4, 33.0, 2.4), (14.8, 7.0, 1.0), cloak_paint(5, edges="b", depth=4), faces=("front", "back"))
    for sx in (-1, 1):
        b.box("cloak_top", (1.0 if sx > 0 else -6.0, 39.4, 2.5), (5.0, 8.0, 0.9), cloak_paint(7, edges="t", depth=5, lining_front=False), faces=("front", "back"),
              rotation=(0, -sx * 14, -sx * 8), pivot=(sx * 3.5, 39.5, 2.9), key="collar%d" % sx)
    b.box("cloak_a", (-7.0, 21.0, 2.8), (14.0, 12.2, 0.9), cloak_paint(9, edges="b", depth=4, runes=0.05), faces=("front", "back"))
    lens = [13.5, 15.0, 16.5, 17.5, 16.0, 14.5, 13.0, 11.5, 11.5]
    for k, x in enumerate(strip_x):
        ln = lens[k] if k < len(lens) else 12.0
        lean = x * 1.6                                                          # the hem flares: the outer strips lean out
        b.box("cloak_s%d" % (k + 1), (x - 1.5, 21.0 - ln, 3.0), (3.0, ln, 0.8), cloak_paint(11 + k, edges="b", depth=6, runes=0.0, width=1.5), faces=("front", "back"),
              rotation=(0, 0, -lean * 0.5), pivot=(x, 21.0, 3.4))
    for sx, x in ((-1, -4.2), (1, 1.0)):                                       # the front hanging: three tatters from the belt
        b.box("tabard", (x, 12.0, -3.2), (3.2, 14.0, 0.6), cloak_paint(30 + sx, edges="b", depth=5, lining_front=False), faces=("front", "back"))
    b.box("tabard", (-1.4, 8.5, -3.5), (2.8, 17.5, 0.6), cloak_paint(33, edges="b", depth=6, lining_front=False), faces=("front", "back"))
    # ---- the tail: four segments of a thin tail and a spade
    for k, (ln, w) in enumerate(((5.5, 1.8), (5.5, 1.6), (5.0, 1.4), (4.0, 1.2))):
        y0 = (24.5, 19.0, 13.5, 8.5)[k]
        b.box("tail%d" % (k + 1), (-w / 2, y0 - ln, 2.4 - w / 2), (w, ln, w), skd if k < 2 else bonep)
    b.box("tail4", (-1.5, 3.9, 2.1), (3.0, 3.6, 0.6), plate_of(spade_tip_canvas), faces=("front", "back"), dens={"front": 4, "back": 4})
    b.box("tail4", (-0.3, 3.9, 0.6), (0.6, 3.6, 3.0), plate_of(spade_tip_canvas), faces=("left", "right"), dens=4)
    # ---- the halo: the plate of sigils and a crown of blades round it
    b.box("halo", (-14.0, 28.0, 7.1), (28.0, 28.0, 1.0), plate_of(lambda: halo_canvas(112, gates)), dens={"front": 4, "back": 4}, faces=("front", "back"))
    for k in range(12):
        th = 360.0 * k / 12.0
        bx, by = 13.4 * math.sin(math.radians(th)), 42.0 + 13.4 * math.cos(math.radians(th))
        b.box("halo", (bx - 0.6, by, 7.2), (1.2, 3.0 + (1.6 if k % 3 == 0 else 0), 0.8), blade_paint(40 + k % 3), rotation=(0, 0, -th),
              pivot=(bx, by, 7.6), key="blade%d" % (k % 3))
    if gates:
        b.box("halo_b", (-19.0, 23.0, 8.8), (38.0, 38.0, 0.8), plate_of(lambda: halo_canvas(152, gates, outer=True)), dens={"front": 4, "back": 4}, faces=("front", "back"))
        for k in range(6):
            th = 360.0 * k / 6.0 + 30.0
            bx, by = 24.0 * math.sin(math.radians(th)), 42.0 + 24.0 * math.cos(math.radians(th))
            b.box("halo_b", (bx - 2.0, by - 2.0, 8.6), (4.0, 4.0, 0.8), plate_of(lambda k=k: tablet_canvas(0)), dens={"front": 4, "back": 4}, faces=("front", "back"),
                  rotation=(0, 0, -th), pivot=(bx, by, 9.0), key="tablet%d" % k)
    return b


# ------------------------------------------------------------------------------------------------------------------ animation
DEG = math.pi / 180.0


def pose_clip(A, name, L, loop, poses):
    """A clip from poses: [(time, {"bone:r": (x, y, z), "bone:p": (x, y, z), "bone:s": (sx, sy, sz)})]. A bone that has a track in any pose is at its rest
    value in the poses that do not name it. The keys are smoothed with a Catmull-Rom spline (the loader interpolates linearly)."""
    A.clip(name, length=L, loop=loop)
    used = set()
    for _, d in poses:
        used |= set(d)
    for key in sorted(used):
        bone, kind = key.split(":")
        rest = (1.0, 1.0, 1.0) if kind == "s" else (0.0, 0.0, 0.0)
        keys(A, name, bone, {"r": "rot", "p": "pos", "s": "scale"}[kind], [(t, d.get(key, rest)) for t, d in poses])


def loop_clip(A, name, L, n, steps, tracks):
    """A looping clip of n repeats of one cycle: tracks = {"bone:r": fn(theta) -> (x, y, z)}, sampled `steps` times over the clip."""
    A.clip(name, length=L, loop=True)
    for key, fn in tracks.items():
        bone, kind = key.split(":")
        cyc(A, name, bone, {"r": "rot", "p": "pos", "s": "scale"}[kind], L, fn, n=n, steps=steps)


def spin_track(A, clip, bone, L, degrees, extra=None):
    """A bone turning about z: degrees over the whole clip, linearly (a halo): two keys."""
    A.rot(clip, bone, {0.0: (0, 0, 0), L: (0, 0, degrees)})


def strips_of(b):
    return sorted([n for n in b.m.bones if n.startswith("cloak_s")], key=lambda n: int(n[len("cloak_s"):]))


def add_clips(b, gates):
    """Every clip of the Avatar on this model. The clip names are the ones client/zogratis/ZogratisPose plays."""
    A = b.A
    S = strips_of(b)
    nS = len(S)
    LEG, SHIN, FOOT, ARM, FORE, HAND = "leg_%s", "shin_%s", "foot_%s", "arm_%s", "forearm_%s", "hand_%s"

    def sides(fn):
        """The same track on both sides: fn(side) with side +1 for the left (+x), -1 for the right."""
        return fn(1, "l"), fn(-1, "r")

    # ------------------------------------------------------------------ idle: 8 s, four breaths, the halo a quarter turn, the cloak and tail drifting
    L = 8.0
    t = {}
    t["hips:p"] = lambda th: (0.35 * SIN(th * 0.5), 0.0, 0.0)
    t["body:p"] = lambda th: (0.0, 0.5 * SIN(th), 0.0)
    t["body:r"] = lambda th: (1.2 * SIN(th + 0.6), 2.5 * SIN(th * 0.25), 1.0 * SIN(th * 0.5))
    t["head:r"] = lambda th: (-1.5 * SIN(th) + 2.0, 9.0 * SIN(th * 0.25 + 0.4), 1.5 * SIN(th * 0.5))
    t["jaw:r"] = lambda th: (-3.0 - 2.0 * SIN(th), 0.0, 0.0)
    for sd, nm in ((1, "l"), (-1, "r")):
        t["arm_%s:r" % nm] = (lambda sd: lambda th: (4.0 * SIN(th + 0.3 * sd) + 3.0, 0.0, sd * (6.0 + 2.0 * SIN(th)) )) (sd)
        t["forearm_%s:r" % nm] = (lambda sd: lambda th: (10.0 + 4.0 * SIN(th + 0.8), 0.0, sd * 2.0)) (sd)
        t["hand_%s:r" % nm] = (lambda sd: lambda th: (-10.0 - 5.0 * SIN(th + 1.2), 0.0, sd * (3.0 * SIN(th))) ) (sd)
    t["cloak_top:r"] = lambda th: (2.0 * SIN(th + 0.2), 0.0, 0.0)
    t["cloak_a:r"] = lambda th: (4.0 * SIN(th + 0.5) + 3.0, 0.0, 0.0)
    for k, n_ in enumerate(S):
        t[n_ + ":r"] = (lambda k: lambda th: (6.0 * SIN(th + 0.9 + 0.35 * k) + 6.0, 0.0, 3.5 * SIN(th * 0.5 + 0.8 * k)))(k)
    t["tail1:r"] = lambda th: (2.0 * SIN(th), 11.0 * SIN(th * 0.25), 0.0)
    t["tail2:r"] = lambda th: (3.0 * SIN(th - 0.5), 14.0 * SIN(th * 0.25 - 0.7), 0.0)
    t["tail3:r"] = lambda th: (3.0 * SIN(th - 1.0), 16.0 * SIN(th * 0.25 - 1.4), 0.0)
    t["tail4:r"] = lambda th: (3.0 * SIN(th - 1.5), 18.0 * SIN(th * 0.25 - 2.1), 0.0)
    t["halo:p"] = lambda th: (0.0, 0.6 * SIN(th + 1.0), 0.0)
    if gates:
        t["halo_b:p"] = lambda th: (0.0, -0.6 * SIN(th + 0.4), 0.0)
    loop_clip(A, "idle", L, 4, 64, {k: v for k, v in t.items() if k.split(":")[0] in b.m.bones})
    spin_track(A, "idle", "halo", L, 90.0)
    if gates:
        spin_track(A, "idle", "halo_b", L, -180.0)

    # ------------------------------------------------------------------ walk: 1 s, two long strides, the arms dragging, the cloak streaming
    def stride(L, leg_amp, shin_fold, arm_amp, lean, bob, twist, stream, tail_amp, halo_deg, n_steps):
        t = {}
        for sd, nm in ((1, "l"), (-1, "r")):
            ph = 0.0 if sd > 0 else math.pi
            t["leg_%s:r" % nm] = (lambda ph: lambda th: (leg_amp * SIN(th + ph) - 3.0, 0.0, 0.0))(ph)
            t["shin_%s:r" % nm] = (lambda ph: lambda th: (-(8.0 + shin_fold * max(0.0, COS(th + ph))), 0.0, 0.0))(ph)
            t["foot_%s:r" % nm] = (lambda ph: lambda th: (0.4 * (leg_amp * SIN(th + ph) + 8.0 + shin_fold * max(0.0, COS(th + ph))) - 2.0, 0.0, 0.0))(ph)
            t["arm_%s:r" % nm] = (lambda ph, sd: lambda th: (-arm_amp * SIN(th + ph) + 6.0, 0.0, sd * 7.0))(ph, sd)
            t["forearm_%s:r" % nm] = (lambda ph: lambda th: (14.0 + 0.5 * arm_amp * (1.0 + SIN(th + ph + 1.2)), 0.0, 0.0))(ph)
            t["hand_%s:r" % nm] = (lambda ph: lambda th: (-12.0 + 5.0 * SIN(th + ph), 0.0, 0.0))(ph)
        t["hips:p"] = lambda th: (0.0, -bob * COS(2 * th), 0.0)
        t["hips:r"] = lambda th: (0.0, -twist * SIN(th), 0.0)
        t["body:r"] = lambda th: (-lean, twist * 1.4 * SIN(th), 1.2 * SIN(th))
        t["head:r"] = lambda th: (lean * 0.8 + 1.0, -twist * 0.8 * SIN(th), 0.0)
        t["jaw:r"] = lambda th: (-4.0, 0.0, 0.0)
        t["cloak_top:r"] = lambda th: (stream * 0.6 + 2.0 * SIN(th - 0.4), 0.0, 0.0)
        t["cloak_a:r"] = lambda th: (stream + 4.0 * SIN(th - 0.8), 0.0, 0.0)
        for k, n_ in enumerate(S):
            t[n_ + ":r"] = (lambda k: lambda th: (stream * 1.3 + 8.0 * SIN(th - 1.0 - 0.4 * k), 0.0, 4.0 * SIN(th - 0.6 * k)))(k)
        t["tail1:r"] = lambda th: (-leg_amp * 0.1 * SIN(2 * th), tail_amp * SIN(th), 0.0)
        t["tail2:r"] = lambda th: (3.0 * SIN(2 * th - 0.5), tail_amp * 1.2 * SIN(th - 0.6), 0.0)
        t["tail3:r"] = lambda th: (3.0 * SIN(2 * th - 1.0), tail_amp * 1.4 * SIN(th - 1.2), 0.0)
        t["tail4:r"] = lambda th: (3.0 * SIN(2 * th - 1.5), tail_amp * 1.6 * SIN(th - 1.8), 0.0)
        return t
    wt = stride(1.0, 27.0, 34.0, 13.0, 3.0, 0.8, 4.0, 8.0, 9.0, 90.0, 20)
    loop_clip(A, "walk", 1.0, 1, 20, {k: v for k, v in wt.items() if k.split(":")[0] in b.m.bones})
    spin_track(A, "walk", "halo", 1.0, 90.0)
    if gates:
        spin_track(A, "walk", "halo_b", 1.0, -180.0)
    rt = stride(0.6, 40.0, 52.0, 22.0, 13.0, 1.4, 5.0, 26.0, 14.0, 90.0, 20)
    loop_clip(A, "run", 0.6, 1, 20, {k: v for k, v in rt.items() if k.split(":")[0] in b.m.bones})
    spin_track(A, "run", "halo", 0.6, 90.0)
    if gates:
        spin_track(A, "run", "halo_b", 0.6, -180.0)

    # ------------------------------------------------------------------ attack: the Void Claws, a sweep of the right arm across the body (the clip starts 0.2 s in)
    pose_clip(A, "attack", 0.6, False, [
        (0.00, {"body:r": (-2, 20, 0), "arm_r:r": (-70, 0, -35), "forearm_r:r": (-30, 0, 0), "hand_r:r": (-30, 0, -20), "arm_l:r": (10, 0, 10),
                "head:r": (0, -12, 0), "cloak_a:r": (8, 0, 0), "hips:r": (0, -8, 0)}),
        (0.12, {"body:r": (-8, -8, 0), "arm_r:r": (-30, 0, -50), "forearm_r:r": (-20, 0, 0), "hand_r:r": (-40, 0, -25), "arm_l:r": (14, 0, 12),
                "head:r": (0, -2, 0), "cloak_a:r": (-6, 0, 0), "hips:r": (0, 4, 0)}),
        (0.26, {"body:r": (-16, -32, 0), "arm_r:r": (75, 0, 25), "forearm_r:r": (15, 0, 0), "hand_r:r": (20, 0, 22), "arm_l:r": (18, 0, 14),
                "head:r": (-4, 10, 0), "cloak_a:r": (-18, 0, 0), "hips:r": (0, 14, 0)}),
        (0.40, {"body:r": (-10, -22, 0), "arm_r:r": (60, 0, 20), "forearm_r:r": (20, 0, 0), "hand_r:r": (10, 0, 12), "arm_l:r": (14, 0, 12),
                "head:r": (-2, 6, 0), "cloak_a:r": (6, 0, 0), "hips:r": (0, 10, 0)}),
        (0.60, {"body:r": (-2, -4, 0), "arm_r:r": (14, 0, 8), "forearm_r:r": (12, 0, 0), "hand_r:r": (-10, 0, 0), "arm_l:r": (6, 0, 8),
                "head:r": (0, 0, 0), "cloak_a:r": (3, 0, 0), "hips:r": (0, 2, 0)})])

    # ------------------------------------------------------------------ cast: the wind-up of every move: the arms rise and open, the head lifts, the jaw opens, the halo spins up, the cloak billows
    cast = [
        (0.0, {}),
        (0.45, {"body:r": (6, 0, 0), "head:r": (10, 0, 0), "jaw:r": (-14, 0, 0), "arm_l:r": (55, 0, 28), "arm_r:r": (55, 0, -28), "forearm_l:r": (22, 0, 0), "forearm_r:r": (22, 0, 0),
                "hand_l:r": (22, 0, 18), "hand_r:r": (22, 0, -18), "cloak_top:r": (-8, 0, 0), "cloak_a:r": (-14, 0, 0), "hips:p": (0, -0.8, 0)}),
        (1.0, {"body:r": (8, 0, 0), "head:r": (14, 0, 0), "jaw:r": (-18, 0, 0), "arm_l:r": (70, 0, 38), "arm_r:r": (70, 0, -38), "forearm_l:r": (26, 0, 0), "forearm_r:r": (26, 0, 0),
                "hand_l:r": (26, 0, 24), "hand_r:r": (26, 0, -24), "cloak_top:r": (-12, 0, 0), "cloak_a:r": (-22, 0, 0), "hips:p": (0, -1.0, 0)}),
        (1.6, {"body:r": (9, 0, 0), "head:r": (16, 0, 0), "jaw:r": (-20, 0, 0), "arm_l:r": (78, 0, 44), "arm_r:r": (78, 0, -44), "forearm_l:r": (28, 0, 0), "forearm_r:r": (28, 0, 0),
                "hand_l:r": (28, 0, 28), "hand_r:r": (28, 0, -28), "cloak_top:r": (-14, 0, 0), "cloak_a:r": (-26, 0, 0), "hips:p": (0, -1.2, 0)})]
    for k, n_ in enumerate(S):
        for pose in cast[1:]:
            pose[1][n_ + ":r"] = (-18.0 - 6.0 * pose[0] - 1.5 * (k % 3), 0.0, (k - (nS - 1) / 2.0) * 1.6)
    for key in ("leg_l", "leg_r"):
        for pose in cast[1:]:
            pose[1][key + ":r"] = (0.0, 0.0, 0.0)
    pose_clip(A, "cast", 1.6, False, cast)
    A.rot("cast", "halo", {0.0: (0, 0, 0), 1.6: (0, 0, 180)})
    if gates:
        A.rot("cast", "halo_b", {0.0: (0, 0, 0), 1.6: (0, 0, -270)})

    # ------------------------------------------------------------------ cast_big: Abyssal Collapse: a crouch, the arms swept above the head, the halo spinning up for 2 s; at 0.8 of the clip the slam
    big = [
        (0.0, {}),
        (0.5, {"body:r": (-8, 0, 0), "hips:p": (0, -3.0, 0), "leg_l:r": (-16, 0, 0), "leg_r:r": (-16, 0, 0), "shin_l:r": (-28, 0, 0), "shin_r:r": (-28, 0, 0),
               "arm_l:r": (30, 0, 18), "arm_r:r": (30, 0, -18), "forearm_l:r": (20, 0, 0), "forearm_r:r": (20, 0, 0), "head:r": (-8, 0, 0), "cloak_a:r": (-10, 0, 0)}),
        (1.3, {"body:r": (4, 0, 0), "hips:p": (0, -1.0, 0), "leg_l:r": (-6, 0, 0), "leg_r:r": (-6, 0, 0), "shin_l:r": (-12, 0, 0), "shin_r:r": (-12, 0, 0),
               "arm_l:r": (150, 0, 30), "arm_r:r": (150, 0, -30), "forearm_l:r": (12, 0, 0), "forearm_r:r": (12, 0, 0), "head:r": (18, 0, 0), "jaw:r": (-24, 0, 0),
               "cloak_a:r": (-26, 0, 0), "cloak_top:r": (-14, 0, 0), "hand_l:r": (30, 0, 25), "hand_r:r": (30, 0, -25)}),
        (2.08, {"body:r": (12, 0, 0), "hips:p": (0, 0.8, 0), "leg_l:r": (4, 0, 0), "leg_r:r": (4, 0, 0), "shin_l:r": (-4, 0, 0), "shin_r:r": (-4, 0, 0),
                "arm_l:r": (172, 0, 34), "arm_r:r": (172, 0, -34), "forearm_l:r": (6, 0, 0), "forearm_r:r": (6, 0, 0), "head:r": (22, 0, 0), "jaw:r": (-30, 0, 0),
                "cloak_a:r": (-34, 0, 0), "cloak_top:r": (-18, 0, 0), "hand_l:r": (34, 0, 28), "hand_r:r": (34, 0, -28)}),
        (2.22, {"body:r": (-24, 0, 0), "hips:p": (0, -2.6, 0), "leg_l:r": (-24, 0, 0), "leg_r:r": (12, 0, 0), "shin_l:r": (-34, 0, 0), "shin_r:r": (-8, 0, 0),
                "arm_l:r": (30, 0, 8), "arm_r:r": (30, 0, -8), "forearm_l:r": (30, 0, 0), "forearm_r:r": (30, 0, 0), "head:r": (-14, 0, 0), "jaw:r": (-34, 0, 0),
                "cloak_a:r": (24, 0, 0), "cloak_top:r": (14, 0, 0), "hand_l:r": (20, 0, 10), "hand_r:r": (20, 0, -10)}),
        (2.6, {"body:r": (-4, 0, 0), "hips:p": (0, -0.4, 0), "arm_l:r": (8, 0, 8), "arm_r:r": (8, 0, -8), "forearm_l:r": (14, 0, 0), "forearm_r:r": (14, 0, 0),
               "head:r": (0, 0, 0), "cloak_a:r": (6, 0, 0), "leg_l:r": (-6, 0, 0), "shin_l:r": (-10, 0, 0)})]
    for k, n_ in enumerate(S):
        for tt, pose in big[1:]:
            lift = -34.0 if tt < 2.1 else 26.0
            pose[n_ + ":r"] = (lift - (tt < 2.1) * 8.0 * (tt / 2.1) - 2.0 * (k % 3), 0.0, (k - (nS - 1) / 2.0) * 2.0)
    pose_clip(A, "cast_big", 2.6, False, big)
    A.rot("cast_big", "halo", {0.0: (0, 0, 0), 2.08: (0, 0, 1080), 2.6: (0, 0, 1170)})
    A.scale("cast_big", "halo", {0.0: (1, 1, 1), 1.9: (1.0, 1.0, 1.0), 2.08: (1.18, 1.18, 1.0), 2.3: (1.3, 1.3, 1.0), 2.6: (1, 1, 1)})
    if gates:
        A.rot("cast_big", "halo_b", {0.0: (0, 0, 0), 2.08: (0, 0, -720), 2.6: (0, 0, -810)})
        A.scale("cast_big", "halo_b", {0.0: (1, 1, 1), 2.08: (1.18, 1.18, 1.0), 2.3: (1.3, 1.3, 1.0), 2.6: (1, 1, 1)})

    # ------------------------------------------------------------------ hurt: a flinch
    hurt = [(0.0, {}),
            (0.07, {"body:r": (8, 8, 3), "head:r": (12, -10, 0), "jaw:r": (-14, 0, 0), "arm_l:r": (10, 0, 14), "arm_r:r": (10, 0, -14), "cloak_a:r": (-14, 0, 0), "hips:p": (0, -0.6, 0)}),
            (0.18, {"body:r": (3, 3, 1), "head:r": (4, -3, 0), "jaw:r": (-6, 0, 0), "arm_l:r": (4, 0, 8), "arm_r:r": (4, 0, -8), "cloak_a:r": (4, 0, 0)}),
            (0.30, {})]
    for k, n_ in enumerate(S):
        hurt[1][1][n_ + ":r"] = (-26.0, 0.0, (k - (nS - 1) / 2.0) * 3.0)
        hurt[2][1][n_ + ":r"] = (6.0, 0.0, 0.0)
    pose_clip(A, "hurt", 0.3, False, hurt)
    A.rot("hurt", "halo", {0.0: (0, 0, 0), 0.07: (0, 0, 14), 0.18: (0, 0, -6), 0.3: (0, 0, 0)})

    # ------------------------------------------------------------------ transform: each model plays its half of the 3 s of the Gates (the clip runs 1.5 s)
    spread = {"body:r": (12, 0, 0), "hips:p": (0, 1.0, 0), "head:r": (24, 0, 0), "jaw:r": (-32, 0, 0), "arm_l:r": (20, 0, 62), "arm_r:r": (20, 0, -62),
              "forearm_l:r": (18, 0, 0), "forearm_r:r": (18, 0, 0), "hand_l:r": (24, 0, 30), "hand_r:r": (24, 0, -30), "cloak_a:r": (-30, 0, 0), "cloak_top:r": (-18, 0, 0),
              "leg_l:r": (-4, 0, 0), "leg_r:r": (-4, 0, 0)}
    if not gates:
        tr = [(0.0, {}),
              (0.45, {"body:r": (-14, 0, 0), "hips:p": (0, -3.0, 0), "leg_l:r": (-18, 0, 0), "leg_r:r": (-18, 0, 0), "shin_l:r": (-32, 0, 0), "shin_r:r": (-32, 0, 0),
                      "head:r": (-10, 0, 0), "arm_l:r": (18, 0, 12), "arm_r:r": (18, 0, -12), "forearm_l:r": (20, 0, 0), "forearm_r:r": (20, 0, 0), "cloak_a:r": (-10, 0, 0)}),
              (1.0, dict(spread, **{"body:r": (4, 0, 0), "arm_l:r": (10, 0, 30), "arm_r:r": (10, 0, -30), "hips:p": (0, -0.6, 0)})),
              (1.5, spread)]
        for k, n_ in enumerate(S):
            for tt, pose in tr[1:]:
                pose[n_ + ":r"] = (-34.0 * min(1.0, tt / 1.0) - 2.0 * (k % 3), 0.0, (k - (nS - 1) / 2.0) * 2.4)
        pose_clip(A, "transform", 1.5, False, tr)
        A.rot("transform", "halo", {0.0: (0, 0, 0), 1.5: (0, 0, 720)})
    else:
        tr = [(0.0, spread),
              (0.45, dict(spread, **{"jaw:r": (-36, 0, 0), "head:r": (26, 0, 0), "body:r": (14, 0, 0)})),
              (1.0, {"body:r": (4, 0, 0), "head:r": (6, 0, 0), "jaw:r": (-12, 0, 0), "arm_l:r": (24, 0, 24), "arm_r:r": (24, 0, -24), "forearm_l:r": (14, 0, 0), "forearm_r:r": (14, 0, 0),
                     "cloak_a:r": (-10, 0, 0), "cloak_top:r": (-6, 0, 0)}),
              (1.5, {"body:r": (-2, 0, 0), "arm_l:r": (6, 0, 8), "arm_r:r": (6, 0, -8), "forearm_l:r": (12, 0, 0), "forearm_r:r": (12, 0, 0), "cloak_a:r": (3, 0, 0)})]
        for k, n_ in enumerate(S):
            for tt, pose in tr:
                pose[n_ + ":r"] = (-30.0 * (1.0 - tt / 1.5) + 4.0, 0.0, (k - (nS - 1) / 2.0) * 2.0 * (1.0 - tt / 1.5))
        pose_clip(A, "transform", 1.5, False, tr)
        A.rot("transform", "halo", {0.0: (0, 0, 0), 1.5: (0, 0, 360)})
        A.rot("transform", "halo_b", {0.0: (0, 0, 0), 1.5: (0, 0, -540)})
        grow = {0.0: (0.01, 0.01, 0.01), 0.25: (0.01, 0.01, 0.01), 0.9: (1.0, 1.0, 1.0), 1.5: (1.0, 1.0, 1.0)}
        for n_ in ("halo_b", "horn2_l", "horn2_r", "pa3_l", "pa3_r"):
            A.scale("transform", n_, grow)

    # ------------------------------------------------------------------ death: the 20 ticks of the dying body: a stagger, the knees give, it topples forward, the cloak settles, the halo shrinks
    dth = [(0.0, {}),
           (0.25, {"body:r": (14, 0, 0), "head:r": (22, 0, 0), "jaw:r": (-36, 0, 0), "arm_l:r": (24, 0, 36), "arm_r:r": (24, 0, -36), "forearm_l:r": (16, 0, 0), "forearm_r:r": (16, 0, 0),
                   "hips:p": (0, -1.0, 0), "cloak_a:r": (-22, 0, 0)}),
           (0.55, {"body:r": (-6, 0, 0), "head:r": (-14, 0, 0), "jaw:r": (-28, 0, 0), "hips:p": (0, -9.0, 4.0), "leg_l:r": (-60, 0, 0), "leg_r:r": (-52, 0, 0),
                   "shin_l:r": (72, 0, 0), "shin_r:r": (66, 0, 0), "arm_l:r": (28, 0, 20), "arm_r:r": (34, 0, -20), "forearm_l:r": (20, 0, 0), "forearm_r:r": (24, 0, 0), "cloak_a:r": (10, 0, 0)}),
           (0.85, {"body:r": (-46, 0, 0), "head:r": (-30, 0, 0), "jaw:r": (-24, 0, 0), "hips:p": (0, -11.0, -3.0), "leg_l:r": (-72, 0, 0), "leg_r:r": (-66, 0, 0),
                   "shin_l:r": (88, 0, 0), "shin_r:r": (84, 0, 0), "arm_l:r": (70, 0, 12), "arm_r:r": (76, 0, -12), "forearm_l:r": (8, 0, 0), "forearm_r:r": (8, 0, 0), "cloak_a:r": (24, 0, 0)}),
           (1.2, {"body:r": (-58, 0, 0), "head:r": (-24, 0, 0), "jaw:r": (-20, 0, 0), "hips:p": (0, -12.0, -4.0), "leg_l:r": (-78, 0, 0), "leg_r:r": (-72, 0, 0),
                  "shin_l:r": (94, 0, 0), "shin_r:r": (90, 0, 0), "arm_l:r": (84, 0, 10), "arm_r:r": (88, 0, -10), "forearm_l:r": (4, 0, 0), "forearm_r:r": (4, 0, 0), "cloak_a:r": (34, 0, 0)})]
    for k, n_ in enumerate(S):
        for tt, pose in dth:
            f = min(1.0, tt / 1.2)
            pose[n_ + ":r"] = (-30.0 * math.sin(min(1.0, tt / 0.3) * math.pi / 2) * (1.0 - f) + 36.0 * f, 0.0, (k - (nS - 1) / 2.0) * 2.0)
    pose_clip(A, "death", 1.2, False, dth)
    A.rot("death", "halo", {0.0: (0, 0, 0), 1.2: (0, 0, 270)})
    A.scale("death", "halo", {0.0: (1, 1, 1), 0.6: (1.0, 1.0, 1.0), 1.2: (0.01, 0.01, 0.01)})
    if gates:
        A.rot("death", "halo_b", {0.0: (0, 0, 0), 1.2: (0, 0, -360)})
        A.scale("death", "halo_b", {0.0: (1, 1, 1), 0.5: (1.0, 1.0, 1.0), 1.0: (0.01, 0.01, 0.01), 1.2: (0.01, 0.01, 0.01)})


def build_all(only=None):
    out = {}
    for name in MODELS:
        if only and name not in only:
            continue
        gates = name == "gates"
        b = Build(name)
        build_body(b, gates)
        add_clips(b, gates)
        TINT["cur"] = LUCIFERO
        img, glow = b.finish()
        hh = b.save_geo()
        b.save_textures(img, glow, hh)
        TINT["cur"] = MEGICULA
        img, glow = b.repaint()
        b.save_textures(img, glow, hh, "_megicula")
        TINT["cur"] = LUCIFERO
        print(b.m.id, "cubes", b.m.cube_count(), "texture", b.pk.W, "x", hh, "(rows used", b.pk.height_used(), ")")
        out[name] = b
    return out


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(420, 640), tex=None):
    sc = {"kind": "geo", "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile), "floor": True,
          "figure": False, "expect": [], "anim": anim, "views": views}
    if tex is None:
        sc["model"] = "%s_%s" % (KEY, model)
    else:
        sc["model"] = "nusmp:geo/entity/%s_%s.geo.json" % (KEY, model)
        sc["texture"] = "nusmp:textures/entity/%s_%s%s.png" % (KEY, model, tex)
    return sc


def scenes_for(models=MODELS):
    out = {}
    for name in models:
        for tex, tag in (("", ""), ("_megicula", "m")):
            pre = "devil_%s%s_" % (name, ("_" + tag) if tag else "")
            tx = tex or None
            h = 1.65
            out[pre + "front"] = scene(name, "idle", [0], [view("front", 0, 4, (0, h, 0), 7.2), view("front 3/4", 35, 8, (0, h, 0), 7.2)], tex=tx)
            out[pre + "back"] = scene(name, "idle", [0], [view("back", 180, 5, (0, h, 0), 7.2), view("side", 90, 4, (0, h, 0), 7.2)], tex=tx)
            out[pre + "face"] = scene(name, "idle", [0], [view("face", 0, 2, (0, 2.95, 0), 2.6), view("face 3/4", 32, 6, (0, 2.95, 0), 2.6)], tile=(420, 420), tex=tx)
            for clip, ages in (("walk", [0, 5, 10, 15]), ("run", [0, 3, 6, 9]), ("attack", [0, 4, 8, 12]), ("cast", [0, 6, 12, 18, 24]), ("cast_big", [0, 14, 28, 42, 52, 60]),
                               ("hurt", [0, 2, 4, 6]), ("transform", [0, 12, 24, 36, 48, 60]), ("death", [0, 5, 10, 15, 20])):
                out[pre + clip] = scene(name, clip, ages, [view("3/4", 30, 6, (0, h, 0), 7.6)], tile=(340, 520), tex=tx)
            out[pre + "idle"] = scene(name, "idle", [0, 20, 40, 60, 80], [view("3/4", 30, 6, (0, h, 0), 7.6)], tile=(340, 520), tex=tx)
    return out


def write_scenes(models=MODELS, path=SCENES_OUT):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(scenes_for(models), f, indent=1)


def main(argv):
    only = [a for a in argv if not a.startswith("-")]
    build_all(only or None)
    write_scenes()


if __name__ == "__main__":
    main(sys.argv[1:])

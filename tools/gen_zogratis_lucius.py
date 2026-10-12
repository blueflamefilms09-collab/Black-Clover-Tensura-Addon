"""Lucius Zogratis, the Wizard King's other soul and the main antagonist of the Zogratis bosses, in the repo's GeoLite pipeline (tools/geo_builder.py ->
client.geo.GeoDraw): both of his forms, the Soul Paladins he makes of the mobs he takes, and his floating grimoire.

    python3 -B tools/gen_zogratis_lucius.py                    # everything (idempotent)
    python3 -B tools/gen_zogratis_lucius.py lucius_base        # only the named models

Models (GeoSpec.of("zogratis", "<name>")), original pixel-art interpretations of the manga designs (nothing is traced from the reference images):
    lucius_base     the human form, 2.1 blocks to the crown of the head (34 px), 2.3 with the hair: pale face, short spiky black hair, red eyes, a small sigil on the left
                    forehead, a faint smile, the huge white fur mantle over a tall dark collar and choker, a long black cloak with a dark fur lining, a
                    brown-maroon long robe with crossed dark straps over the chest; one hand raised with a spark (bone "spark")
    lucius_divine   the true form, 2.4 blocks from the toes to the crown of the head, floating and barefoot: a pale-blue and white layered robe, the white fur mantle
                    and a crossed chain, THREE PAIRS of feathered wings (wing1_l/r upper, wing2_l/r middle, wing3_l/r lower, each in two segments with
                    feather plates), a golden gear and lattice halo behind the head, golden stag antlers, gold eyes; the grimoire floats beside it as its own model
    soul_paladin    an armoured white-gold knight with a faint soul glow (the mobs Lucius turns into immortal Paladins): great helm with a visor slit,
                    crest, pauldrons, tabard, cape, sword and shield (bones "sword" and "shield")
    grimoire_prop   Lucius's floating golden open grimoire: gold covers, pages, a radiant starburst, a turning page, orbiting runes

Writes for each <name> (through tools/geo_builder.py):
    assets/nusmp/geo/entity/zogratis_<name>.geo.json
    assets/nusmp/animations/entity/zogratis_<name>.animation.json
    assets/nusmp/textures/entity/zogratis_<name>.png          512 wide at 2 texels per model pixel (faces, halo, feathers, plates at 3 to 6), as tall as needed
    assets/nusmp/textures/entity/zogratis_<name>_glow.png     eyes, spark, halo, antlers, wing tips, sigils, grimoire (additive, full bright)
and tools/render_preview/scenes.d/zogratis_lucius.json (python3 -B tools/render_preview/preview.py lucius_base_idle ...), and, with the scratchpad path of the session (ZOG_SCRATCH), the manifest
and, with --preview, the preview sheets (see MANIFEST_OUT / PREVIEW_OUT).

Clips (the same names on every model): idle walk run attack cast cast_big hurt transform death. Rotation signs of this frame (checked in the preview):
positive x swings a hanging limb FORWARD (arms raise forward), negative x folds a knee / leans the body forward; positive z swings a hanging limb
towards +x (the model's left); positive y turns the head towards the model's right. Positions are pixels (y up), scales multiply.
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
from zogratis_lucius_kit import *  # noqa: E402,F401,F403
from zogratis_lucius_kit import SIN, COS, TAU, A3, Canvas, Pack  # noqa: E402

KEY = "zogratis"
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "zogratis_lucius.json")
SCRATCH = os.environ.get("ZOG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
MANIFEST_OUT = os.path.join(SCRATCH, "zog_art_manifest_lucius.json")
PREVIEW_OUT = os.path.join(SCRATCH, "previews_lucius")

# ------------------------------------------------------------------------------------------------------------------ palettes (dark -> light)
SKIN = ((186, 146, 152), (226, 190, 186), (247, 220, 212), (255, 238, 230))
SKIN_DIV = ((198, 168, 182), (234, 206, 208), (251, 231, 228), (255, 246, 242))
HAIR = ((8, 8, 12), (18, 18, 28), (36, 38, 58), (66, 72, 106))
FUR = ((116, 108, 124), (164, 156, 172), (204, 198, 206), (230, 226, 228), (246, 243, 240), (255, 253, 250))
FUR_DIV = ((150, 160, 196), (192, 204, 230), (224, 232, 248), (242, 247, 255), (251, 253, 255), (255, 255, 255))
ROBE = ((44, 26, 26), (66, 38, 34), (92, 56, 48), (118, 74, 62), (146, 96, 80))
ROBE_DARK = ((30, 18, 20), (46, 26, 28), (62, 36, 36), (82, 50, 46))
COLLAR = ((30, 20, 22), (48, 32, 32), (70, 48, 44), (92, 64, 58))
STRAP = ((14, 10, 12), (26, 20, 22), (44, 34, 36), (74, 60, 60))
CLOAK = ((10, 9, 15), (18, 17, 27), (30, 30, 46), (50, 52, 76))
CLOAK_FUR = ((8, 8, 12), (16, 16, 24), (30, 32, 46), (54, 58, 82), (84, 90, 122))
LEATHER = ((24, 16, 14), (42, 28, 24), (66, 46, 38), (96, 70, 56))
GOLD = ((88, 56, 22), (140, 92, 30), (200, 148, 40), (240, 196, 72), (255, 238, 150))
SILVER = ((70, 76, 92), (120, 128, 148), (176, 184, 200), (224, 230, 240))
ROBE_BLUE = ((120, 156, 200), (156, 190, 222), (190, 214, 238), (222, 236, 250), (246, 251, 255))
ROBE_WHITE = ((170, 184, 216), (206, 218, 238), (234, 242, 252), (252, 254, 255))
CLOAK_BLUE = ((112, 152, 196), (146, 184, 218), (184, 214, 236), (222, 238, 250), (248, 252, 255))
BAND_BLUE = ((40, 80, 122), (62, 108, 154), (96, 142, 186), (146, 184, 216))
FEATHER = ((150, 168, 204), (190, 204, 230), (222, 232, 248), (244, 250, 255), (255, 255, 255))
FEATHER_GOLD = (255, 224, 140, 255)
GOLD_GLOW = (255, 214, 96)
SPARK_GLOW = (255, 190, 90)
RED_GLOW = (255, 56, 56)
PLATE_W = ((150, 158, 182), (196, 204, 226), (228, 234, 248), (248, 251, 255), (255, 255, 255))
PLATE_G = ((132, 90, 30), (186, 136, 44), (226, 178, 62), (250, 214, 100), (255, 244, 176))
SOUL = (150, 235, 255)
SOUL_GLOW = (120, 220, 255)
TABARD = ((150, 156, 176), (196, 202, 220), (230, 234, 246), (252, 253, 255))


# ------------------------------------------------------------------------------------------------------------------ faces (4 texels per pixel: 32 x 32)
def face_art(kind):
    """The head's front face (8 x 8 px) at 32 x 32 texels, drawn by hand with primitives. kind 'base': red narrowed eyes, a faint smile showing teeth;
    'divine': gold eyes under calm lids, a serene closed smile, a golden sigil."""
    cv = Canvas(32, 32)
    div = kind == "divine"
    sk = SKIN_DIV if div else SKIN
    # skin: a few flat tone bands (light from the upper left): the cheek on the far side and the jaw in shade
    for y in range(32):
        for x in range(32):
            t = 0.70
            if y >= 27:
                t = 0.5
            if y >= 30:
                t = 0.34
            if x >= 27 or x <= 1:
                t -= 0.2
            elif x >= 24 and y > 19:
                t -= 0.12
            cv.put(x, y, pal(sk, t, x, y, 0.0))
    H = HAIR
    # hair: the cap, two long temple locks and four fringe locks ending in points; the strand lines and the sheen are drawn on top
    cv.rect(0, 0, 31, 4, H[1])
    cv.poly([(0, 4), (5, 4), (4, 12), (2, 16), (0, 14)], H[1])
    cv.poly([(26, 4), (31.5, 4), (31.5, 15), (29, 13), (27, 9)], H[1])
    locks = (((5, 4), (10.5, 4), (7, 11)), ((10, 4), (16, 4), (13.5, 9.5)), ((15.5, 4), (21, 4), (18, 9)), ((20.5, 4), (26.5, 4), (24, 11)))
    for a_, b_, tip in locks:
        cv.poly([a_, b_, tip], H[1])
    for (x0, y0, x1, y1, c) in ((7, 5, 7, 9, 0), (13, 5, 13, 8, 0), (18, 5, 18, 7, 0), (24, 5, 24, 9, 0), (2, 5, 2, 12, 0), (29, 5, 29, 11, 0),
                                (9, 5, 8, 8, 2), (15, 5, 14, 8, 2), (20, 5, 19, 7, 2), (26, 5, 25, 8, 2), (3, 5, 3, 9, 2), (28, 5, 28, 8, 2)):
        cv.line(x0, y0, x1, y1, H[c])
    for x in range(3, 30):                                                  # the sheen, a broken curve over the crown
        if x % 3 != 1:
            cv.put(x, 1 + (1 if x < 8 or x > 24 else 0), H[3])
    for x in range(0, 32, 5):
        cv.put(x, 3, H[0])
    # forehead sigil (viewer's left): a small ring with a dot
    sig, sig2 = ((150, 40, 40), (232, 100, 60)) if not div else ((214, 170, 60), (255, 240, 160))
    for (x, y) in ((9, 8), (10, 8), (11, 9), (11, 10), (10, 11), (9, 11), (8, 10), (8, 9)):
        cv.put(x, y, sig, 255, GOLD_GLOW if div else None, 200)
    cv.put(9, 9, sig2, 255, GOLD_GLOW if div else (255, 90, 60), 230)
    cv.put(10, 10, sig2, 255, GOLD_GLOW if div else (255, 90, 60), 230)
    # brows
    bro = (14, 12, 18)
    if div:
        cv.poly([(6, 14.5), (14.5, 13.5), (14.5, 14.8), (6.5, 15.8)], (40, 30, 52))
        cv.poly([(25.5, 14.5), (17, 13.5), (17, 14.8), (25, 15.8)], (40, 30, 52))
    else:
        cv.poly([(6, 14), (14.5, 14.5), (14.5, 16), (6.5, 15.5)], bro)
        cv.poly([(25.5, 14), (17, 14.5), (17, 16), (25, 15.5)], bro)
    # eyes: 7 x 3 texels, mirrored; the divine eyes are half closed under heavy lids, a glowing gold crescent below the lash line
    for cx0 in (7, 18):
        ex = list(range(cx0, cx0 + 7))
        left = cx0 == 7
        ir = (cx0 + 2, cx0 + 3, cx0 + 4)
        if div:
            for x in ex:
                cv.put(x, 17, mix(sk[1], sk[0], 0.6))                           # the lid fold
                cv.put(x, 18, (40, 30, 64))                                     # the lash line
                cv.put(x, 19, (222, 224, 242))                                  # sclera
                cv.put(x, 20, (150, 132, 176) if x in (ex[0], ex[-1]) else sk[1])
            cv.put(cx0 - 1 if left else cx0 + 7, 17, (40, 30, 64))
            for x in ir:
                cv.put(x, 19, (244, 196, 72), 255, GOLD_GLOW, 255)
            cv.put(ir[1], 19, (255, 244, 170), 255, GOLD_GLOW, 255)
            for x in ir[:2] if left else ir[1:]:
                cv.put(x, 20, (210, 150, 40), 255, GOLD_GLOW, 220)
            continue
        for x in ex:
            cv.put(x, 17, (22, 14, 20))                                         # the upper lid
        cv.put(cx0 - 1 if left else cx0 + 7, 16, (22, 14, 20))                  # a flick at the outer corner
        for x in ex:
            cv.put(x, 18, (238, 232, 238))                                      # sclera
            cv.put(x, 19, (226, 218, 228))
        ic = (200, 26, 40)
        il = (255, 120, 110)
        for x in ir:
            for y in (18, 19):
                cv.put(x, y, ic if (x != ir[1] or y == 18) else (30, 0, 8), 255, RED_GLOW, 235)
        cv.put(ir[1], 18, (40, 0, 10), 255, RED_GLOW, 255)
        cv.put(ir[0] if left else ir[2], 18, il, 255, (255, 235, 210), 255)
        for x in ex[1:-1]:                                                      # the soft shadow under the lower lid
            cv.put(x, 20, mix(sk[1], sk[0], 0.5))
    # nose
    for y in range(18, 22):
        cv.put(17, y, sk[1])
    for x in (14, 15, 16, 17):
        cv.put(x, 22, sk[1])
    cv.put(15, 22, sk[0])
    cv.put(16, 22, sk[0])
    cv.put(15, 20, sk[3])
    cv.put(15, 19, sk[3])
    # mouth
    lip = (196, 128, 134) if not div else (206, 160, 180)
    dk = (86, 40, 48) if not div else (112, 84, 116)
    if div:
        for x, y in ((11, 25), (12, 26), (13, 27), (14, 27), (15, 27), (16, 27), (17, 27), (18, 27), (19, 26), (20, 25)):
            cv.put(x, y, lip)
        for x in (11, 20):
            cv.put(x, 24, sk[1])
        for x in range(14, 18):
            cv.put(x, 28, sk[1])
    else:
        for x, y in ((10, 24), (11, 25), (12, 26), (13, 27), (14, 27), (15, 27), (16, 27), (17, 27), (18, 27), (19, 26), (20, 25), (21, 24)):
            cv.put(x, y, dk)
        for x in range(12, 20):
            cv.put(x, 26, (248, 242, 240))
            if 13 <= x <= 18:
                cv.put(x, 27, (226, 218, 224) if x != 15 else (248, 242, 240))
        cv.put(15, 26, (214, 204, 212))
        cv.put(13, 26, (214, 204, 212))
        cv.put(17, 26, (214, 204, 212))
        for x in range(13, 19):
            cv.put(x, 28, mix(lip, dk, 0.25))
        cv.put(9, 24, sk[0])
        cv.put(22, 24, sk[0])
    return cv


def face_paint(kind, base_skin, hair_p):
    """The head box painter: the front face from the art, the temples and the top as hair, the sides as skin with an ear, the back as hair."""
    art = face_art(kind)
    sk = SKIN_DIV if kind == "divine" else SKIN

    def p(face, i, j, fw, fh):
        if face == "front":
            c, g = art.at(i, j)
            return c, g
        if face in ("left", "right"):
            f = i if face == "left" else fw - 1 - i                      # distance from the front edge, 0..fw-1 (8 px = 16 texels)
            if j < 8 or (f > 9 and j < 18):
                return hair_p(face, i, j, fw, fh)
            if 7 <= f <= 11 and 9 <= j <= 13:                              # the ear
                return px(sk[1] if (f in (7, 11) or j in (9, 13)) else sk[2]), None
            if f < 3 and j < 12:
                return hair_p(face, i, j, fw, fh)
            return base_skin(face, i, j, fw, fh)
        if face == "back":
            return hair_p(face, i, j, fw, fh) if j < 13 else base_skin(face, i, j, fw, fh)
        if face == "top":
            return hair_p(face, i, j, fw, fh)
        return base_skin(face, i, j, fw, fh)
    return p


# ------------------------------------------------------------------------------------------------------------------ the rig helpers
def rot_file(v, rx, ry, rz):
    """Where the file-frame vector v goes under a cube / bone rotation (rx, ry, rz) degrees, exactly as client.geo.GeoDraw applies it: the file's y is
    negated into vanilla space, the quaternion rotationZYX(-rz, ry, -rx) turns it (X first, then Y, then Z), and y is negated back."""
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

    def __init__(self, name, tw=512, th=512, dens=2):
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

    def spike(self, bone, base, w, ln, rx, rz, paint, ry=0.0, key=None, fracs=(0.55, 0.45), taper=0.55, bend=(0.0, 0.0), faces=None):
        """A tapering spike of stacked boxes standing at `base` (x, y, z), tilted by (rx, ry, rz); each segment is `taper` times as wide as the one below and
        bent by `bend` more degrees: hair spikes, fur tufts, bone tines. The next segment starts exactly where the last one ends (rot_file)."""
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
            d = rot_file((0, sl, 0), angx, ry, angz)
            pos = [pos[0] + d[0], pos[1] + d[1], pos[2] + d[2]]
            angx += bend[0]
            angz += bend[1]
            wd *= taper
        return out

    def clump(self, bone, pivot, theta, alpha, w, ln, paint, standing=False, key=None, dens=4, thick=0.5, spin=0.0):
        """A flat clump plate (fur, hair, feathers) hanging from `pivot` (or standing on it when `standing`), its face turned to azimuth theta (0 = the front,
        90 = +x, 180 = the back) and its tip swung `alpha` degrees out of the vertical towards that side."""
        px_, py_, pz_ = pivot
        if standing:
            org, rot = (px_ - w / 2, py_, pz_ - thick / 2), (-alpha, -theta, spin)
        else:
            org, rot = (px_ - w / 2, py_ - ln, pz_ - thick / 2), (alpha, -theta, spin)
        return self.box(bone, org, (w, ln, thick), paint, rotation=rot, pivot=tuple(pivot), faces=("front", "back"), dens=dens, key=key)

    def raise_upper(self, dy, keep=("root",), keep_prefix=("leg_", "skirt_", "shin_", "foot_", "robe_")):
        """The upper body is designed with the hips at one height; this lifts every bone and cube except `keep` / `keep_prefix` by dy (pivots, origins and cube
        pivots), so the lower body can be built in real coordinates and made longer without touching an upper-body number."""
        for n, bn in self.m.bones.items():
            if n in keep or n.startswith(keep_prefix):
                continue
            bn["pivot"][1] += dy
            for c in bn["cubes"]:
                c["origin"][1] += dy
                if "pivot" in c:
                    c["pivot"][1] += dy

    def finish(self):
        self.pk.finish()
        img, glow = self.pk.images()
        return img, glow

    def save(self, img, glow):
        used = self.pk.height_used()
        hh = 64
        while hh < used:
            hh += 64
        img, glow = img.crop((0, 0, self.pk.W, hh)), glow.crop((0, 0, self.pk.W, hh))
        self.m.tex_h = hh
        self.m.save(self.A)
        self.m.save_texture(img, glow)
        n = self.m.cube_count()
        quads = sum(len(c["uv"]) for b in self.m.bones.values() for c in b["cubes"])
        print(self.m.id, "cubes", n, "quads/pass", quads, "texture", self.pk.W, "x", hh, "(rows used", used, ")")
        return {"cubes": n, "quads": quads, "tex": [self.pk.W, hh]}


# ------------------------------------------------------------------------------------------------------------------ painters of the base form
def torso_front():
    """The robe's chest (8 x 12 px = 16 x 24 texels): the two crossed dark straps (an X from each shoulder to the opposite hip) with a knot at the crossing."""
    cv = Canvas(16, 24)
    for (x0, y0, x1, y1) in ((2.5, 0, 12.5, 23), (12.5, 0, 2.5, 23)):
        for w in range(2):
            cv.line(x0 + w, y0, x1 + w, y1, STRAP[1])
        cv.line(x0 - 0.5, y0, x1 - 0.5, y1, STRAP[2], width=1)
    cv.rect(6, 10, 9, 13, STRAP[1])
    cv.rect(7, 11, 8, 12, STRAP[3])
    cv.put(6, 10, STRAP[2])
    return cv


def robe_front_paint(base):
    """canvas_paint, but the strap texels get a one-texel shadow on the robe beside them so they sit on the cloth."""
    cv = torso_front()
    shade = Canvas(16, 24)
    for y in range(24):
        for x in range(16):
            c, _ = cv.at(x, y)
            if c is not None:
                for (dx, dy) in ((1, 1), (2, 1)):
                    if cv.at(x + dx, y + dy)[0] is None:
                        shade.put(x + dx, y + dy, ROBE[0])
    for y in range(24):
        for x in range(16):
            c, _ = shade.at(x, y)
            if c is not None and cv.at(x, y)[0] is None:
                cv.put(x, y, c)
    return canvas_paint(cv, "front", base)


def sleeve_paint(salt, hem=0):
    return fabric(ROBE, salt=salt, period=6.0, amp=0.2, patch=ROBE_DARK, patch_thr=0.3, hem=hem, hem_cols=ROBE_DARK)


def cloak_paint(salt=4, fur_hem=0, fur_edge=""):
    base = fabric(CLOAK, salt=salt, period=10.0, amp=0.24, lit=0.16, dither=0.03)
    furp = fur(CLOAK_FUR, salt=salt + 3, flow="y", root=0.2, cut="b", cut_depth=7, taper=2.0, dither=0.04, lift=-0.1)

    def p(face, i, j, fw, fh):
        if fur_hem and face in SIDES and j >= fh - fur_hem:
            return furp(face, i, j, fw, fh)
        if fur_edge and face in SIDES:
            if ("l" in fur_edge and i < 3) or ("r" in fur_edge and i >= fw - 3):
                return fur(CLOAK_FUR, salt=salt + 9, flow="y", root=0.3, cut=None, dither=0.04, lift=-0.05)(face, i, j, fw, fh)
        return base(face, i, j, fw, fh)
    return p


def fur_slab(cols, salt, cut="", depth=6, root=0.5, lift=0.0):
    """A fur slab: strands run down the side faces and out along the top and bottom, ragged spiky tips on the edges named in `cut`."""
    down = fur(cols, salt, flow="y", root=root, cut=cut, cut_depth=depth, lift=lift)
    along = fur(cols, salt + 1, flow="x", root=root, cut=cut, cut_depth=depth, lift=lift)

    def p(face, i, j, fw, fh):
        return (along if face in ("top", "bottom") else down)(face, i, j, fw, fh)
    return p


def mantle(b, cols=FUR, salt=100, tall=1.0, glow=None):
    """The huge white fur mantle on bone "fur": a solid fur core (a standing ring round the neck that is open at the front, a slab over each shoulder, lobes
    on the chest), then a bell of overlapping flame-shaped clumps in four tiers hanging from the neck out over the shoulders (each tier wider and lower, the
    tips pointing out and down), and a ring of clumps standing up round the neck."""
    core = lambda sd, cut="", depth=5: fur_slab(cols, salt + sd, cut, depth, 0.5, -0.05)
    b.box("fur", (-5, 22.5, 1.8), (10, 5.5 * tall, 2.5), core(1, "t", 6), rotation=(14, 0, 0), pivot=(0, 22.5, 2.5))
    b.pair("fur", "fur", (3.2, 22.4, -3.6), (2.5, 4.0 * tall, 7.5), core(2, "t", 6), rotation=(0, 0, -12), pivot=(3.5, 22.4, 0))
    b.pair("fur", "fur", (1.2, 22.2, -5.0), (2.5, 3.0 * tall, 2), core(3, "t", 5), rotation=(-14, 0, -10), pivot=(2.5, 22.2, -4))
    b.pair("fur", "fur", (1.5, 20.4, -5.0), (10.5, 2.5, 10.0), core(4, "r", 6), rotation=(0, 0, -14), pivot=(2, 23.0, 0))
    b.pair("fur", "fur", (1.0, 16.6, -5.2), (4.5, 6.0, 2.5), core(6, "b", 5), rotation=(-12, 0, -12), pivot=(3, 23, -4))
    # the bell: four tiers of hanging clumps
    plates = {}

    def plate(kind, variant, wide=1.0):
        k = (kind, variant)
        if k not in plates:
            plates[k] = clump(cols, salt=salt + 7 * variant + 13 * kind, strands=3.0, root=0.5, curl=((-1) ** variant) * 0.14, split=0.5 if variant == 3 else 0.0)
        return plates[k]
    tiers = ((4.4, 4.2, 24.6, 22, 6.0, 6.5), (7.2, 5.4, 23.6, 32, 6.5, 7.0), (10.0, 6.2, 22.2, 42, 7.0, 7.5), (12.6, 6.6, 20.4, 52, 7.0, 7.5))
    n = 0
    for k, (rx_, rz_, y0, alpha, w, ln) in enumerate(tiers):
        cnt = (10, 14, 17, 20)[k]
        for q in range(cnt):
            th = 56 + q * (248.0 / (cnt - 1))                                        # from the front-right round the back to the front-left; the front stays open
            ang = math.radians(th)
            hv = hsh(q, k, salt)
            v = int(hv * 4) % 4
            pv = (rx_ * math.sin(ang), y0 + 0.5 * (hv - 0.5) - 0.35 * abs(math.cos(ang)) * k, -rz_ * math.cos(ang))
            b.clump("fur", pv, th, alpha + 10 * (hv - 0.5), w, ln * tall, plate(k, v), key="bell%d_%d" % (k, v))
            n += 1
    # a ridge of clumps standing along the top of each shoulder, leaning further out the further they are from the neck
    for sg in (1, -1):
        for q in range(6):
            hv = hsh(q, sg + 20, salt)
            v = int(hv * 4) % 4
            x0 = 3.4 + q * 1.6
            for zi, z0 in enumerate((-3.0, 0.2, 3.2)):
                b.clump("fur", (sg * x0 + (hv - 0.5), 24.0 - 0.55 * q, z0 + 0.6 * (hv - 0.5)), 90 * sg + (zi - 1) * 38 * (1 if sg > 0 else -1), 22 + 10 * q + 8 * hv,
                        6.0, 5.5 * tall, plate(6, v), standing=True, key="ridge%d" % v)
                n += 1
    # the ring standing round the neck, open at the front
    for ring, (rx_, rz_, y0, alpha, w, ln) in enumerate(((4.8, 5.0, 23.4, 14, 5.5, 6.5), (6.8, 6.4, 22.6, 26, 6.0, 7.0))):
        for q in range(11 if ring == 0 else 14):
            th = 52 + q * (256.0 / (10 if ring == 0 else 13))
            ang = math.radians(th)
            hv = hsh(q, ring + 5, salt)
            v = int(hv * 4) % 4
            front = clamp((abs(th - 180.0) - 60.0) / 70.0)                              # the clumps near the front stay low so the face is clear
            b.clump("fur", (rx_ * math.sin(ang), y0, -rz_ * math.cos(ang)), th, alpha + 8 * hv, w, r5(ln * tall * (1.0 - 0.5 * front)), plate(7 + ring, v), standing=True,
                    key="ring%d_%d" % (ring, v))
            n += 1
    # the edge of the V on the chest
    for sg in (1, -1):
        for k, y0 in enumerate((22.0, 19.6)):
            b.clump("fur", (sg * (3.2 + 0.9 * k), y0, -5.4), 0 + sg * 18, 18 + 8 * k, 4.5, 6.0, plate(8, k), key="vee%d" % k)
            n += 1
    return n


def chain_front():
    """The divine robe's chest (16 x 24 texels): a crossed chain of silver links with gold beads, from each shoulder to the opposite hip."""
    cv = Canvas(16, 24)
    for (x0, y0, x1, y1) in ((2.5, 0, 12.5, 23), (12.5, 0, 2.5, 23)):
        n = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
        for k in range(0, n + 1):
            t = k / n
            x, y = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
            col = (214, 224, 240) if k % 3 != 2 else (122, 138, 170)
            cv.put(round(x), round(y), col)
            cv.put(round(x) + 1, round(y), (176, 190, 214) if k % 3 != 2 else (96, 112, 146))
            if k % 6 == 3:
                cv.put(round(x), round(y) + 1, (255, 232, 150), 255, GOLD_GLOW, 150)
    cv.rect(6, 10, 9, 13, (214, 168, 70), 255, GOLD_GLOW, 160)
    cv.rect(7, 11, 8, 12, (255, 244, 180), 255, GOLD_GLOW, 220)
    return cv


def build_human(kind):
    """lucius_base and lucius_divine share the upper body (head, hair, face, collar, torso, arms, the fur mantle, the spark, the cloak bones); the lower body
    and the extras differ. The upper body is designed with the hips at y = 11 and then lifted (raise_upper)."""
    div = kind == "divine"
    b = Build("lucius_divine" if div else "lucius_base", 512, 1024 if div else 512)
    A = b.A
    sk = SKIN_DIV if div else SKIN
    # ----- bones
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 11, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("hair", "head", (0, 31, 0))
    b.bone("fur", "body", (0, 22, 0))
    b.bone("cloak", "body", (0, 23, 2.5))
    b.bone("cloak_top", "cloak", (0, 23, 2.5))
    b.bone("cloak_bottom", "cloak_top", (0, 12, 2.5))
    if not div:
        b.bone("cloak_l", "cloak_top", (7.5, 22.5, 0))
        b.bone("cloak_r", "cloak_top", (-7.5, 22.5, 0))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 5.5, 21.5, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 5.5, 14.5, 0))
    b.bone("spark", "forearm_l", (5.5, 3.0, -0.5))

    # ----- torso, sash, collar, choker
    if div:
        torso = canvas_paint(chain_front(), "front", fabric(ROBE_BLUE, salt=1, period=7.0, amp=0.16, patch=ROBE_WHITE, patch_thr=0.3, dither=0.04))
        collar = fabric(ROBE_BLUE, salt=3, period=3.0, amp=0.22, seam=False)
    else:
        torso = robe_front_paint(fabric(ROBE, salt=1, period=7.0, amp=0.18, patch=ROBE_DARK, patch_thr=0.2))
        collar = fabric(COLLAR, salt=3, period=3.0, amp=0.25, seam=False)
    b.box("body", (-4, 11, -2.5), (8, 12, 5), torso)
    b.box("body", (-4, 10.5, -2.5), (8, 2.5, 5), fabric(ROBE_WHITE if div else ROBE_DARK, salt=2, period=9.0, amp=0.15, seam=False), inflate=0.3, faces=SIDES)
    b.box("body", (-3.5, 22.5, -3), (7, 3.5, 6), collar, inflate=0.25)
    if not div:
        b.box("body", (-3.5, 22.8, -3), (7, 1, 6), solid(STRAP[0], noise=3), inflate=0.45, faces=SIDES)
        b.box("body", (-0.75, 21.6, -3.8), (1.5, 1.2, 0.5), metal(SILVER, salt=2, emboss=False), faces=("front", "left", "right", "top"))
    # ----- head: the face at 4 texels per pixel
    skin_p = skin(sk, salt=1)
    b.box("head", (-4, 24, -4), (8, 8, 8), face_paint("divine" if div else "base", skin_p, hair(salt=2)), dens={"front": 4}, faces=("front", "left", "right", "back", "top"))
    b.pair("head", "head", (3.9, 25.5, -0.6), (0.6, 2, 1.6), skin(sk, salt=4), faces=("left", "front", "back", "top"))
    # hair: a thin cap, a crown of clumps standing and leaning out, the locks over the forehead and the temples
    b.box("hair", (-4.3, 31.6, -4.3), (8.6, 1.5, 8.6), hair(salt=5), faces=("top", "back", "left", "right"))
    b.box("hair", (-4.4, 25.5, 3.8), (8.8, 6.5, 1.0), hair(salt=6), faces=("back", "left", "right"))
    hpl = [clump(HAIR, salt=70 + k, strands=2.4, root=0.1, curl=((-1) ** k) * 0.2, dither=0.04, split=0.0) for k in range(4)]
    crown = []
    for q in range(8):                                                      # an outer ring of clumps leaning away from the crown
        ang = q * 45.0 + 10
        crown.append((3.1 * math.sin(math.radians(ang)), 31.9, -3.1 * math.cos(math.radians(ang)), ang, 38 + 18 * hsh(q, 1, 7), 2.4 + 0.9 * hsh(q, 2, 7)))
    for q in range(4):                                                      # an inner ring standing up with a swept lean to the model's left
        ang = q * 90.0 + 40
        crown.append((1.3 * math.sin(math.radians(ang)), 32.2, -1.3 * math.cos(math.radians(ang)), 60 + ang * 0.2, 18 + 20 * hsh(q, 3, 7), 2.8 + 0.8 * hsh(q, 4, 7)))
    for n, (x, y, z, th, al, ln) in enumerate(crown):
        v = n % 4
        b.clump("hair", (x, y, z), th, al, 3.5, ln, hpl[v], standing=True, key="hc%d" % v)
    for n, x in enumerate((-3.0, -1.0, 1.0, 3.0)):                          # the fringe locks hanging over the forehead
        b.clump("hair", (x, 32.3, -4.5), (x * 6), 28 + 4 * (n % 2), 3.0, 3.0, hpl[n], key="hf%d" % n)
    for sg in (1, -1):                                                      # the temple locks
        for n, z in enumerate((-2.2, 0.6, 3.0)):
            b.clump("hair", (sg * 4.5, 31.2, z), 90 * sg, 14 + 6 * n, 3.0, 3.5 - 0.4 * n, hpl[(n + 1) % 4], key="ht%d" % ((n + 1) % 4 + 10 * (sg > 0)))
    # ----- arms
    if div:
        sl_u = fabric(ROBE_BLUE, salt=11, period=6.0, amp=0.2, patch=ROBE_WHITE, patch_thr=0.35, dither=0.04)
        sl_f = fabric(ROBE_BLUE, salt=12, period=6.0, amp=0.2, hem=3, hem_cols=BAND_BLUE, dither=0.04)
    else:
        sl_u, sl_f = sleeve_paint(11), sleeve_paint(12, hem=3)
    b.pair("arm_l", "arm_r", (4, 14.5, -1.5), (3, 7.5, 3), sl_u, inflate=0.3)
    b.pair("forearm_l", "forearm_r", (4, 9.5, -1.5), (3, 5.0, 3), sl_f, inflate=0.3)
    b.pair("forearm_l", "forearm_r", (4.25, 7.0, -1.25), (2.5, 3, 2.5), skin(sk, salt=7), faces=SIDES + ("top",))
    b.pair("forearm_l", "forearm_r", (4.25, 5.5, -1.25), (2.5, 1.5, 1.5), skin(sk, salt=8), faces=SIDES + ("bottom",))
    b.pair("forearm_l", "forearm_r", (3.75, 6.5, -1.0), (0.5, 2, 1.0), skin(sk, salt=9), faces=("left", "right", "front", "back"))
    # ----- the cloak (the base form's is black with dark fur and side drapes; the divine form's a pale cape)
    if div:
        b.box("cloak_top", (-6.5, 12, 2.7), (13, 11.5, 1.5), fabric(CLOAK_BLUE, salt=4, period=8.0, amp=0.26, dither=0.04))
        b.box("cloak_bottom", (-8, -3.0, 2.9), (16, 15.0, 1.5), fabric(CLOAK_BLUE, salt=5, period=7.0, amp=0.3, hem=3, hem_cols=ROBE_WHITE, dither=0.04))
    else:
        b.box("cloak_top", (-6.5, 12, 2.7), (13, 11.5, 1.5), cloak_paint(4, fur_edge="lr"))
        b.box("cloak_bottom", (-8, -1.5, 2.9), (16, 13.5, 1.5), cloak_paint(5, fur_hem=7, fur_edge="lr"))
        b.pair("cloak_l", "cloak_r", (6.6, -1.0, -2.6), (2.5, 23.5, 7.0), cloak_paint(6, fur_hem=0, fur_edge="l"), rotation=(0, 0, 9), pivot=(7.5, 22.5, 0))
    mantle(b, cols=FUR_DIV if div else FUR, tall=1.12 if div else 1.0)
    if div:
        spark_parts(b, size=1.0, core=(255, 244, 190), mid=(255, 224, 120), edge=(255, 180, 60), glow=GOLD_GLOW)
        b.raise_upper(7.0)
        divine_lower(b)
        divine_extras(b)
        divine_clips(A)
    else:
        base_lower(b)
        spark_parts(b)
        base_clips(A)
        b.raise_upper(2.0)
    return b


def base_lower(b):
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("leg" + suf, "root", (sg * 2, 13, 0))
        b.bone("skirt" + suf, "root", (sg * 2, 13, 0))
    for (y0, y1, w, d, salt) in ((8.0, 13.5, 5.0, 6.0, 21), (4.0, 8.0, 5.6, 6.8, 22), (0.5, 4.0, 6.2, 7.6, 23)):
        b.pair("skirt_l", "skirt_r", (0, y0, -d / 2), (w, y1 - y0, d), fabric(ROBE, salt=salt, period=9.0, amp=0.3, patch=ROBE_DARK, patch_thr=0.12,
                                                                               hem=3 if y0 < 1 else 0, hem_cols=ROBE_DARK, seam=False), faces=SIDES + ("bottom",))
    b.bone("robe_front", "body", (0, 13.5, -3.6))
    b.box("robe_front", (-4.6, 0.6, -4.5), (9.2, 12.9, 0.9), fabric(ROBE, salt=24, period=8.0, amp=0.3, patch=ROBE_DARK, patch_thr=0.1, hem=3, hem_cols=ROBE_DARK, seam=False),
          faces=SIDES + ("bottom",))
    b.pair("leg_l", "leg_r", (0.5, 2.5, -1.5), (3, 11, 3), fabric(LEATHER, salt=30, period=5.0, amp=0.15, seam=False), faces=SIDES)
    b.pair("leg_l", "leg_r", (0.2, 0, -4.2), (3.6, 2.5, 5.7), fabric(LEATHER, salt=31, period=6.0, amp=0.15, seam=False))


# ------------------------------------------------------------------------------------------------------------------ the divine form's lower body and extras
HALO_R = 17.0
HALO_C = (0.0, 37.0, 8.8)


def halo_canvas(inner):
    """The halo's lattice at 4 texels per pixel (120 x 120 for the outer ring plate, the inner plate the same size but only the middle): rings, 24 radial
    spokes (the lattice has 24-fold symmetry, so a turn of 15 degrees repeats the picture and a clip can spin it seamlessly), small square cells between
    them, and a hot centre. Everything is gold in the colour layer and glowing in the glow layer."""
    n = int(HALO_R * 8)
    cv = Canvas(n, n)
    c0 = n / 2.0
    R = HALO_R * 4

    def gold(t):
        return pal(GOLD, t)
    for y in range(n):
        for x in range(n):
            dx, dy = x + 0.5 - c0, y + 0.5 - c0
            r = math.hypot(dx, dy)
            ang = (math.degrees(math.atan2(dy, dx)) + 360.0) % 360.0
            seg = (ang % 15.0)
            dseg = min(seg, 15.0 - seg)                                             # degrees from the nearest spoke
            arc = r * math.radians(dseg)                                            # texels from the nearest spoke
            rr = r / R
            on = False
            tone = 0.62
            if not inner:
                if 0.86 <= rr <= 0.99:                                              # the thick outer ring
                    on = True
                    tone = 0.85 if rr < 0.9 else (0.5 if rr > 0.95 else 0.7)
                elif 0.62 <= rr < 0.66:                                             # the ring inside it
                    on = True
                    tone = 0.75
                elif 0.62 <= rr < 0.86 and arc < 0.9:                               # spokes between the two
                    on = True
                    tone = 0.6
                elif 0.66 <= rr < 0.86:                                             # lattice cells: short bars across, between every pair of spokes
                    k = int((rr - 0.66) / 0.2 * 4)
                    if k % 2 == 0 and abs(arc - r * math.radians(7.5)) > 0.0 and (rr * R) % 6.0 < 1.2:
                        on = True
                        tone = 0.55
            else:
                if 0.52 <= rr < 0.56 or 0.30 <= rr < 0.33:
                    on = True
                    tone = 0.8
                elif 0.14 <= rr < 0.52 and arc < 0.8:
                    on = True
                    tone = 0.62
                elif rr < 0.1:
                    on = True
                    tone = 1.0
                elif 0.33 <= rr < 0.52 and (rr * R) % 5.0 < 1.0 and dseg > 1:
                    on = True
                    tone = 0.5
            if on:
                col = gold(tone + 0.04 * (1 if (x + y) % 2 else 0))
                cv.put(x, y, col, 255, GOLD_GLOW, 200 if tone < 0.9 else 255)
    return cv


def halo(b):
    cx, cy, cz = HALO_C
    b.bone("halo", "body", (cx, cy, cz))
    b.bone("halo_in", "body", (cx, cy, cz + 0.5))
    h = HALO_R
    b.box("halo", (cx - h, cy - h, cz - 0.25), (2 * h, 2 * h, 0.5), canvas_plate(halo_canvas(False)), faces=("front", "back"), dens=4)
    b.box("halo_in", (cx - h, cy - h, cz + 0.25), (2 * h, 2 * h, 0.5), canvas_plate(halo_canvas(True)), faces=("front", "back"), dens=4)
    gp = metal(GOLD, salt=3, glow=(*GOLD_GLOW, 210), emboss=False)
    for k in range(24):                                                      # the rim: 24 chunky segments, and a tooth between each pair (the gear)
        phi = k * 15.0
        ang = math.radians(phi)
        x, y = cx + (h - 0.4) * math.sin(ang), cy + (h - 0.4) * math.cos(ang)
        b.box("halo", (x - 1.9, y - 0.5, cz - 0.7), (3.8, 1.0, 1.4), gp, rotation=(0, 0, -phi), pivot=(x, y, cz), key="halo_rim")
        phi2 = phi + 7.5
        ang2 = math.radians(phi2)
        x2, y2 = cx + (h + 0.6) * math.sin(ang2), cy + (h + 0.6) * math.cos(ang2)
        b.box("halo", (x2 - 0.75, y2 - 1.0, cz - 0.6), (1.5, 2.0, 1.2), gp, rotation=(0, 0, -phi2), pivot=(x2, y2, cz), key="halo_tooth")


def antlers(b):
    b.bone("antlers", "head", (0, 39.5, 0))
    gp = metal(GOLD, salt=5, glow=(*GOLD_GLOW, 190), emboss=False)
    lens, wds = (5.6, 5.0, 4.8, 4.2, 3.8), (2.0, 1.9, 1.7, 1.5, 1.2)
    rzs, rxs = (-24, -34, -26, -10, 8), (-6, -8, -6, -4, -2)
    pos = [2.4, 39.2, -0.4]
    joints = []
    for k in range(5):
        sw = r5(wds[k])
        b.pair("antlers", "antlers", (pos[0] - sw / 2, pos[1], pos[2] - sw / 2), (sw, lens[k], sw), gp, rotation=(rxs[k], 0, rzs[k]), pivot=tuple(pos))
        d = rot_file((0, lens[k], 0), rxs[k], 0, rzs[k])
        pos = [pos[0] + d[0], pos[1] + d[1], pos[2] + d[2]]
        joints.append(tuple(pos))
    tips = ((0, -56, -36, 7.0), (1, -54, -28, 8.5), (2, -50, -22, 8.0), (3, -44, -16, 6.5), (4, -26, -8, 5.0))
    for (jk, rz, rx, ln) in tips:                                           # tines fanning out and forward from the joints
        base = list(joints[jk - 1]) if jk > 0 else [2.6, 39.4, -1.0]
        for seg in range(2):
            sw = 1.0 if seg == 0 else 0.5
            sl = ln * (0.6 if seg == 0 else 0.5)
            b.pair("antlers", "antlers", (base[0] - sw / 2, base[1], base[2] - sw / 2), (sw, r5(sl), sw), gp, rotation=(rx, 0, rz - seg * 14), pivot=tuple(base))
            d = rot_file((0, r5(sl), 0), rx, 0, rz - seg * 14)
            base = [base[0] + d[0], base[1] + d[1], base[2] + d[2]]


def wing(b, n, P, arc, lmax, count=7, z0=0.0):
    """One pair of wings as fans of feather plates (in the xy plane) radiating from the attach point P: a back row of long flight feathers over the arc
    (gamma degrees from +x, counter-clockwise) and a row of shorter, wider coverts in front; the first half of the arc is on wing<n>_l, the outer half on
    wing<n>_tip_l (a child bone with the same pivot, so the fan can fold and ruffle); wing<n>_r is the mirror."""
    for suf in ("_l", "_r"):
        sg = 1 if suf == "_l" else -1
        b.bone("wing%d%s" % (n, suf), "body", (sg * P[0], P[1], P[2]))
        b.bone("wing%d_tip%s" % (n, suf), "wing%d%s" % (n, suf), (sg * P[0], P[1], P[2]))
    cols = [FEATHER, FEATHER_W2][n % 2]
    for row in range(2):
        cnt = count if row == 0 else count - 1
        for k in range(cnt):
            t = k / (cnt - 1)
            gam = arc[0] + (arc[1] - arc[0]) * t
            ln = lmax * (0.74 + 0.26 * SIN(math.pi * (0.12 + 0.88 * t)))
            if row == 1:
                ln *= 0.5
            ln = r5(ln)
            w = 4.5 if row == 0 else 5.0
            beta = gam + 90.0
            ox = 0.0 if row == 0 else 0.2 * lmax * math.cos(math.radians(gam))
            oy = 0.0 if row == 0 else 0.2 * lmax * math.sin(math.radians(gam))
            piv = (P[0] + ox, P[1] + oy, P[2] + z0 + 0.32 * k + (-0.6 if row else 0.0))
            bone = "wing%d_l" % n if t < 0.5 else "wing%d_tip_l" % n
            boner = bone[:-2] + "_r"
            paint = feather(cols, tip_glow=(*GOLD_GLOW, 120), salt=n * 10 + k, dither=0.05, barbs=True, tip_frac=0.16)
            b.pair(bone, boner, (piv[0] - w / 2, piv[1] - ln, piv[2] - 0.25), (w, ln, 0.5), paint, rotation=(0, 0, beta), pivot=piv, faces=("front", "back"), dens=4,
                   key="wf%d_%s_%s" % (n % 2, w, ln))


FEATHER_W2 = ((160, 176, 212), (200, 214, 238), (230, 238, 252), (248, 252, 255), (255, 255, 255))


def divine_lower(b):
    """Bare legs and pointed feet, and the bell of the robe: four panels (front, back, left, right) in two tiers each, with pointed tails along the hem."""
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("leg" + suf, "root", (sg * 2, 18, 0))
        b.bone("foot" + suf, "leg" + suf, (sg * 2, 4.6, 0.4))
    b.pair("leg_l", "leg_r", (0.5, 4.2, -1.5), (3, 14, 3), skin(SKIN_DIV, salt=31), faces=SIDES)
    b.pair("foot_l", "foot_r", (0.4, 3.2, -4.0), (2.6, 1.5, 5.5), skin(SKIN_DIV, salt=32), rotation=(-50, 0, 0), pivot=(2.0, 4.6, 0.4))
    b.pair("foot_l", "foot_r", (0.5, 2.6, -2.2), (2.4, 1.0, 1.4), skin(SKIN_DIV, salt=33), rotation=(-50, 0, 0), pivot=(2.0, 4.6, 0.4))
    tiers = ((12.0, 18.6, 4.2, 4.6, 8.0), (5.5, 12.2, 5.4, 6.0, 15.0))
    names = ("f", "b", "l", "r")
    for nm in names:
        b.bone("skirt_" + nm, "body", (0, 18.6, 0))
        b.bone("skirt_%s2" % nm, "skirt_" + nm, (0, 12.1, 0))
    for ti, (y0, y1, rx_, rz_, fl) in enumerate(tiers):
        h = y1 - y0
        sfx = "" if ti == 0 else "2"
        fab = lambda sd: fabric(ROBE_BLUE, salt=40 + sd + 7 * ti, period=6.0, amp=0.3, patch=ROBE_WHITE, patch_thr=0.28, hem=3 if ti else 0, hem_cols=ROBE_WHITE, dither=0.04, seam=False)
        piv_y = 18.6 if ti == 0 else 12.1
        b.box("skirt_f" + sfx, (-rx_ - 0.5, y0, -rz_ - 1.0), (2 * rx_ + 1.0, h, 1.5), fab(1), rotation=(fl, 0, 0), pivot=(0, piv_y, -rz_), faces=SIDES + ("bottom",))
        b.box("skirt_b" + sfx, (-rx_ - 0.5, y0, rz_ - 0.5), (2 * rx_ + 1.0, h, 1.5), fab(2), rotation=(-fl, 0, 0), pivot=(0, piv_y, rz_), faces=SIDES + ("bottom",))
        b.box("skirt_l" + sfx, (rx_ - 0.5, y0, -rz_ + 0.2), (1.5, h, 2 * rz_ - 0.4), fab(3), rotation=(0, 0, fl), pivot=(rx_, piv_y, 0), faces=SIDES + ("bottom",))
        b.box("skirt_r" + sfx, (-rx_ - 1.0, y0, -rz_ + 0.2), (1.5, h, 2 * rz_ - 0.4), fab(4), rotation=(0, 0, -fl), pivot=(-rx_, piv_y, 0), faces=SIDES + ("bottom",))
    # pointed tails along the hem
    tail = [clump(ROBE_WHITE, salt=90 + k, strands=2.0, root=0.0, curl=((-1) ** k) * 0.1, dither=0.04) for k in range(3)]
    for q in range(6):
        x = -6.0 + q * 2.4
        b.clump("skirt_f2", (x, 6.0, -rz_ - 2.6), 0, 14, 3.6, 4.5, tail[q % 3], key="tf%d" % (q % 3))
        b.clump("skirt_b2", (x, 6.0, rz_ + 1.2), 180, 14, 3.6, 4.5, tail[(q + 1) % 3], key="tb%d" % ((q + 1) % 3))
    for q in range(4):
        z = -4.2 + q * 2.8
        b.clump("skirt_l2", (rx_ + 2.2, 6.0, z), 90, 14, 3.6, 4.5, tail[q % 3], key="tl%d" % (q % 3))
        b.clump("skirt_r2", (-rx_ - 2.2, 6.0, z), -90, 14, 3.6, 4.5, tail[(q + 2) % 3], key="tr%d" % ((q + 2) % 3))


def divine_extras(b):
    halo(b)
    antlers(b)
    wing(b, 1, (3.0, 32.0, 5.0), (36, 90), 27.0, 7, 0.0)
    wing(b, 2, (3.0, 27.5, 5.8), (-8, 32), 35.0, 7, 0.4)
    wing(b, 3, (3.0, 23.0, 6.6), (-66, -30), 25.0, 6, 0.8)


def build_base():
    return build_human("base")


def build_divine():
    return build_human("divine")


def star_plate(core=(255, 250, 225), mid=(255, 206, 112), edge=(255, 140, 56), glow=SPARK_GLOW, diag=0.35, ray=1.0):
    """A radiant starburst on a plate face: a hot core, four long rays (and shorter diagonal ones) and a soft halo; the colour layer keeps the bright
    part (alpha-tested), the glow layer the whole soft shape."""
    def p(face, i, j, fw, fh):
        u, v = (i + 0.5) / fw * 2 - 1, (j + 0.5) / fh * 2 - 1
        r = math.hypot(u, v)
        c = max(0.0, 1 - r * 2.4)
        rays = max(0.0, 1 - min(abs(u), abs(v)) * 11) * max(0.0, 1 - max(abs(u), abs(v))) * ray
        d = (u + v) * 0.7071, (u - v) * 0.7071
        dr = max(0.0, 1 - min(abs(d[0]), abs(d[1])) * 12) * max(0.0, 1 - r * 1.2) * diag
        halo = max(0.0, 1 - r) ** 2 * 0.45
        I = clamp(c + rays * 0.85 + dr + halo)
        if I < 0.04:
            return None, None
        col = pal([edge, mid, core], I, i, j, 0.05)
        return (px(col) if I > 0.35 else None), px(glow, 255 * clamp(I * 1.15))
    return p


def spark_parts(b, bone="spark", pivot=(5.5, 3.0, -0.5), size=1.35, core=(255, 250, 225), mid=(255, 206, 112), edge=(255, 140, 56), glow=SPARK_GLOW):
    x, y, z = pivot
    h = 0.75 * size
    b.box(bone, (x - h, y - h, z - h), (1.5 * size, 1.5 * size, 1.5 * size), solid(core, g=(*glow, 255)), rotation=(35, 45, 0), pivot=pivot)
    q = 2.5 * size
    for ry in (0, 90):
        b.box(bone, (x - q, y - q, z - 0.25), (5 * size, 5 * size, 0.5), star_plate(core, mid, edge, glow), faces=("front", "back"), dens=4,
              rotation=(0, ry, 0) if ry else None, pivot=pivot if ry else None)


# ------------------------------------------------------------------------------------------------------------------ the grimoire
def page_canvas(right):
    """One page of the open grimoire (10.4 x 15 px at 4 texels per pixel = 42 x 60): gold leaf with an ornate frame, rows of golden script and a large
    emblem; the frame, the emblem and the script glow."""
    w, h = 42, 60
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            t = 0.55 + 0.1 * nz(x * 0.3, y * 0.3, 11) + (0.1 if (x + y) % 2 == 0 else 0.0) * 0.5
            cv.put(x, y, pal(PLATE_G, t, x, y, 0.05))
    for (x0, y0, x1, y1, col, gl) in ((0, 0, w - 1, h - 1, PLATE_G[0], 0), (2, 2, w - 3, h - 3, PLATE_G[4], 200), (4, 4, w - 5, h - 5, PLATE_G[1], 0)):
        for x in range(x0, x1 + 1):
            cv.put(x, y0, col, 255, GOLD_GLOW if gl else None, gl)
            cv.put(x, y1, col, 255, GOLD_GLOW if gl else None, gl)
        for y in range(y0, y1 + 1):
            cv.put(x0, y, col, 255, GOLD_GLOW if gl else None, gl)
            cv.put(x1, y, col, 255, GOLD_GLOW if gl else None, gl)
    for (cx, cy) in ((5, 5), (w - 6, 5), (5, h - 6), (w - 6, h - 6)):                # corner studs
        cv.rect(cx - 1, cy - 1, cx + 1, cy + 1, PLATE_G[4], 255, GOLD_GLOW, 230)
    cx, cy = w / 2.0, 22
    for y in range(h):                                                           # the emblem: rings and a four-leaf clover
        for x in range(w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if abs(r - 12) < 0.9 or abs(r - 9) < 0.6:
                cv.put(x, y, PLATE_G[4], 255, GOLD_GLOW, 230)
            lobes = [(abs(dx) - 3.2) ** 2 + (abs(dy) - 3.2) ** 2 < 8.5]
            if r < 8 and lobes[0] and (abs(dx) > 0.7 or abs(dy) > 0.7):
                cv.put(x, y, PLATE_G[3] if (x + y) % 3 else PLATE_G[4], 255, GOLD_GLOW, 240)
            if r < 2.2:
                cv.put(x, y, (255, 250, 210), 255, GOLD_GLOW, 255)
    for k in range(7):                                                           # lines of script
        y = 38 + k * 3
        x = 6
        while x < w - 6:
            ln = 2 + int(hsh(x, k, 5 if right else 9) * 4)
            for xx in range(x, min(x + ln, w - 6)):
                cv.put(xx, y, PLATE_G[0], 255, GOLD_GLOW, 120)
            x += ln + 2
    return cv


def cover_canvas():
    """The outside of a cover (11.4 x 16.4 px): gold-tooled leather in a dark gold, a raised border and a big clover in the middle, studs at the corners."""
    w, h = 46, 66
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            t = 0.38 + 0.12 * nz(x * 0.25, y * 0.25, 4)
            cv.put(x, y, pal(PLATE_G, t, x, y, 0.06))
    for inset, col in ((1, PLATE_G[4]), (3, PLATE_G[2]), (6, PLATE_G[3])):
        for x in range(inset, w - inset):
            cv.put(x, inset, col, 255, GOLD_GLOW, 150)
            cv.put(x, h - 1 - inset, col, 255, GOLD_GLOW, 150)
        for y in range(inset, h - inset):
            cv.put(inset, y, col, 255, GOLD_GLOW, 150)
            cv.put(w - 1 - inset, y, col, 255, GOLD_GLOW, 150)
    cx, cy = w / 2.0, h / 2.0
    for y in range(h):
        for x in range(w):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            r = math.hypot(dx, dy)
            if abs(r - 14) < 0.8:
                cv.put(x, y, PLATE_G[4], 255, GOLD_GLOW, 220)
            if r < 11 and (abs(dx) - 4.5) ** 2 + (abs(dy) - 4.5) ** 2 < 14 and (abs(dx) > 1 or abs(dy) > 1):
                cv.put(x, y, PLATE_G[4] if (x + y) % 3 else PLATE_G[3], 255, GOLD_GLOW, 235)
            if r < 2.4:
                cv.put(x, y, (255, 250, 210), 255, GOLD_GLOW, 255)
    return cv


def rune_plate(salt):
    """A small floating glyph (3 x 3 px at 4 texels): a gold sigil with a glow."""
    cv = Canvas(12, 12)
    g = GLYPHS_L[salt % len(GLYPHS_L)]
    for y in range(12):
        for x in range(12):
            if g[y * 6 // 12][x * 6 // 12] == "#":
                cv.put(x, y, (232, 190, 80), 255, GOLD_GLOW, 220)
    return cv


GLYPHS_L = (("..##..", ".#..#.", "#.##.#", "#.##.#", ".#..#.", "..##.."), ("#....#", ".#..#.", "..##..", "..##..", ".#..#.", "#....#"),
            ("..#...", "..#...", "######", "..#...", "..#...", "..#..."), ("######", "#....#", "#.##.#", "#.##.#", "#....#", "######"))


def rays_plate(n_arms=4, long=1.0):
    """A starburst on a 24 x 24 px plate (96 x 96 texels): long thin rays along the axes with a soft glow, additive; the colour layer holds only the hot cores."""
    def p(face, i, j, fw, fh):
        u, v = (i + 0.5) / fw * 2 - 1, (j + 0.5) / fh * 2 - 1
        r = math.hypot(u, v)
        ray = max(0.0, 1 - min(abs(u), abs(v)) * 14) * max(0.0, 1 - max(abs(u), abs(v)) * long)
        halo = max(0.0, 1 - r) ** 2.2 * 0.5
        I = clamp(ray * 0.9 + halo)
        if I < 0.05:
            return None, None
        return (px(pal([PLATE_G[1], PLATE_G[2], PLATE_G[3]], I)) if I > 0.55 else None), px(GOLD_GLOW, 235 * clamp(I * 1.05))
    return p


def build_grimoire():
    b = Build("grimoire_prop", 512, 256)
    A = b.A
    cy = 10.0
    b.bone("root", None, (0, 0, 0))
    b.bone("book", "root", (0, cy, 0))
    b.bone("page_l", "book", (0, cy, -0.4))
    b.bone("page_r", "book", (0, cy, -0.4))
    b.bone("leaf", "book", (0, cy, -1.4))
    b.bone("rays", "book", (0, cy, 3.4))
    b.bone("rays2", "book", (0, cy, 3.9))
    b.bone("runes", "book", (0, cy, 0))
    side = metal(PLATE_G, salt=3, glow=(*GOLD_GLOW, 120), emboss=False)
    cover_p = canvas_paint(cover_canvas(), "back", lambda *a: (px(PLATE_G[1]), None))
    page_l = canvas_paint(page_canvas(False), "front", lambda *a: (px(PLATE_G[2]), None))
    page_r = canvas_paint(page_canvas(True), "front", lambda *a: (px(PLATE_G[2]), None))
    # each half: a cover behind, a page block in front (the open V is the bones' rest rotation in the clips), gilded page edges
    b.box("page_l", (0.3, cy - 8.2, 0.9), (11.4, 16.4, 0.8), cover_p, dens={"back": 4}, faces=("back", "left", "right", "top", "bottom"), key="cover")
    b.box("page_r", (-11.7, cy - 8.2, 0.9), (11.4, 16.4, 0.8), cover_p, dens={"back": 4}, faces=("back", "left", "right", "top", "bottom"), key="cover")
    b.box("page_l", (0.4, cy - 7.5, -1.4), (10.4, 15.0, 2.3), page_l, dens={"front": 4}, faces=("front", "left", "top", "bottom"))
    b.box("page_r", (-10.8, cy - 7.5, -1.4), (10.4, 15.0, 2.3), page_r, dens={"front": 4}, faces=("front", "right", "top", "bottom"))
    b.box("book", (-0.9, cy - 8.2, 0.2), (1.8, 16.4, 1.8), side, faces=("back", "left", "right", "top", "bottom"))                    # the spine
    b.box("leaf", (0.4, cy - 7.2, -1.55), (10.0, 14.4, 0.3), page_r, dens={"front": 4, "back": 4}, faces=("front", "back"))
    b.box("rays", (-20.0, cy - 20.0, 3.15), (40, 40, 0.5), rays_plate(), faces=("front", "back"), dens=3, key="rays")
    b.box("rays2", (-20.0, cy - 20.0, 3.65), (40, 40, 0.5), rays_plate(), faces=("front", "back"), dens=3, rotation=(0, 0, 45), pivot=(0, cy, 3.9), key="rays")
    for k in range(4):                                                           # four runes orbiting the book
        ang = k * 90.0
        x, z = 15.0 * math.sin(math.radians(ang)), -15.0 * math.cos(math.radians(ang))
        y0 = cy - 1.5 + (3.0 if k % 2 else -3.0)
        b.box("runes", (x - 1.0, y0 + 0.5, z - 0.25), (2, 2, 0.5), canvas_plate(rune_plate(k)), faces=("front", "back"), dens=6,
              rotation=(0, -ang, 0), pivot=(x, cy, z))
    grimoire_clips(A)
    b.m.tex_h = 256
    return b


def grimoire_clips(a):
    S = SIN
    # the open V: each half is turned a little towards the viewer (page_l +y, page_r -y), the spine the farthest part

    def halves(clip, L, ang, rep=1, amp=2.0):
        cyc(a, clip, "page_l", "rot", L, lambda t: (0, ang + amp * S(t), 0), rep)
        cyc(a, clip, "page_r", "rot", L, lambda t: (0, -ang - amp * S(t + 0.6), 0), rep)

    def flips(clip, L, n=1):
        """The leaf turns over (towards the viewer, +y) and back n times in the clip, each in its own slice of the clip, so the loop is seamless."""
        c = L / n
        pts = []
        for k in range(n):
            for rel, ang in ((0.0, 0.0), (0.15, 0.0), (0.28, 90.0), (0.4, 176.0), (0.55, 176.0), (0.68, 90.0), (0.8, 0.0)):
                pts.append((k * c + rel * c, (0, ang, 0)))
        pts.append((L, (0, 0, 0)))
        keys(a, clip, "leaf", "rot", pts)
    L = 4.0
    a.clip("idle", L, True)
    cyc(a, "idle", "book", "pos", L, lambda t: (0, 0.8 * S(t), 0))
    cyc(a, "idle", "book", "rot", L, lambda t: (1.5 * S(t + 0.5), 4 * S(t), 1.0 * S(t + 1)))
    halves("idle", L, 12.0)
    flips("idle", L, 1)
    cyc(a, "idle", "rays", "scale", L, lambda t: (0.92 + 0.08 * S(2 * t),) * 3, 1)
    cyc(a, "idle", "rays2", "scale", L, lambda t: (0.92 + 0.08 * S(2 * t + 1.0),) * 3, 1)
    a.rot("idle", "rays", {0.0: (0, 0, 0), L: (0, 0, 90)})
    a.rot("idle", "rays2", {0.0: (0, 0, 0), L: (0, 0, -90)})
    spin(a, "idle", "runes", 1, 1)
    for clip, L, ang, rs, rn, runes in (("walk", 1.2, 12.0, 1.0, 1, 1), ("run", 0.7, 14.0, 1.0, 1, 2)):
        a.clip(clip, L, True)
        cyc(a, clip, "book", "pos", L, lambda t: (0, 0.9 * S(2 * t), 0))
        cyc(a, clip, "book", "rot", L, lambda t: (-8 + 1.5 * S(2 * t + 0.5), 4 * S(t), 1.0 * S(t + 1)))
        halves(clip, L, ang, 2)
        a.rot(clip, "rays", {0.0: (0, 0, 0), L: (0, 0, 90)})
        a.rot(clip, "rays2", {0.0: (0, 0, 0), L: (0, 0, -90)})
        spin(a, clip, "runes", runes, 1)
    L = 1.0
    a.clip("attack", L, False)
    T = (0.0, 0.25, 0.42, 0.6, 1.0)
    seq(a, "attack", "rot", T, book=[(0, 0, 0), (-12, 0, 0), (14, 0, 0), (4, 0, 0), (0, 0, 0)], page_l=[(0, 12, 0), (0, 30, 0), (0, 8, 0), (0, 12, 0), (0, 12, 0)],
        page_r=[(0, -12, 0), (0, -30, 0), (0, -8, 0), (0, -12, 0), (0, -12, 0)])
    seq(a, "attack", "pos", T, book=[(0, 0, 0), (0, 0.5, 1.5), (0, 0, -4.0), (0, 0, -1.0), (0, 0, 0)])
    seq(a, "attack", "scale", T, rays=[(1,) * 3, (1.3,) * 3, (1.7,) * 3, (1.1,) * 3, (1,) * 3], rays2=[(1,) * 3, (1.3,) * 3, (1.7,) * 3, (1.1,) * 3, (1,) * 3])
    L = 1.0
    a.clip("cast", L, True)
    cyc(a, "cast", "book", "pos", L, lambda t: (0, 1.0 + 0.7 * S(t), 0))
    cyc(a, "cast", "book", "rot", L, lambda t: (-6 + 2 * S(t + 0.5), 0, 0))
    halves("cast", L, 26.0, 1, 3.0)
    flips("cast", L, 2)
    cyc(a, "cast", "rays", "scale", L, lambda t: (1.2 + 0.15 * S(t),) * 3)
    cyc(a, "cast", "rays2", "scale", L, lambda t: (1.2 + 0.15 * S(t + 1.0),) * 3)
    a.rot("cast", "rays", {0.0: (0, 0, 0), L: (0, 0, 90)})
    a.rot("cast", "rays2", {0.0: (0, 0, 0), L: (0, 0, -90)})
    spin(a, "cast", "runes", 1, 1)
    L = 2.0
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "book", "pos", L, lambda t: (0, 2.0 + 1.0 * S(t), 0))
    cyc(a, "cast_big", "book", "rot", L, lambda t: (-10 + 2 * S(t), 0, 0))
    cyc(a, "cast_big", "book", "scale", L, lambda t: (1.25 + 0.05 * S(2 * t),) * 3)
    halves("cast_big", L, 44.0, 1, 3.0)
    flips("cast_big", L, 3)
    cyc(a, "cast_big", "rays", "scale", L, lambda t: (1.5 + 0.2 * S(2 * t),) * 3)
    cyc(a, "cast_big", "rays2", "scale", L, lambda t: (1.5 + 0.2 * S(2 * t + 1.0),) * 3)
    a.rot("cast_big", "rays", {0.0: (0, 0, 0), L: (0, 0, 180)})
    a.rot("cast_big", "rays2", {0.0: (0, 0, 0), L: (0, 0, -180)})
    cyc(a, "cast_big", "runes", "scale", L, lambda t: (1.6 + 0.2 * S(t),) * 3)
    spin(a, "cast_big", "runes", 3, 1)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.2, 0.45)
    seq(a, "hurt", "rot", T, book=[(0, 0, 0), (12, 0, 8), (-6, 0, -5), (0, 0, 0)])
    seq(a, "hurt", "pos", T, book=[(0, 0, 0), (0, 0, 2.0), (0, 0, 0.5), (0, 0, 0)])
    L = 3.0
    a.clip("transform", L, False)
    T = (0.0, 0.6, 1.4, 2.0, 3.0)
    seq(a, "transform", "scale", T, book=[(TINY,) * 3, (0.3,) * 3, (1.5,) * 3, (1.2,) * 3, (1,) * 3],
        rays=[(TINY,) * 3, (0.5,) * 3, (3.0,) * 3, (1.8,) * 3, (1,) * 3], rays2=[(TINY,) * 3, (0.5,) * 3, (3.0,) * 3, (1.8,) * 3, (1,) * 3], runes=[(TINY,) * 3, (0.4,) * 3, (2.0,) * 3, (1.4,) * 3, (1,) * 3])
    seq(a, "transform", "rot", T, book=[(0, 0, 0), (0, 180, 0), (0, 360, 0), (0, 360, 0), (0, 360, 0)], page_l=[(0, 70, 0), (0, 50, 0), (0, 20, 0), (0, 14, 0), (0, 12, 0)],
        page_r=[(0, -70, 0), (0, -50, 0), (0, -20, 0), (0, -14, 0), (0, -12, 0)])
    a.rot("transform", "runes", {0.0: (0, 0, 0), L: (0, 720, 0)})
    L = 2.4
    a.clip("death", L, False)
    T = (0.0, 0.5, 1.2, 1.8, 2.4)
    seq(a, "death", "rot", T, page_l=[(0, 12, 0), (0, 30, 0), (0, 70, 0), (0, 84, 0), (0, 84, 0)], page_r=[(0, -12, 0), (0, -30, 0), (0, -70, 0), (0, -84, 0), (0, -84, 0)],
        book=[(0, 0, 0), (-8, 0, 0), (8, 0, 0), (20, 0, 0), (40, 0, 0)])
    seq(a, "death", "pos", T, book=[(0, 0, 0), (0, 1.0, 0), (0, 0, 0), (0, -3.0, 0), (0, -9.0, 0)])
    seq(a, "death", "scale", T, rays=[(1,) * 3, (1.4,) * 3, (0.8,) * 3, (0.2,) * 3, (TINY,) * 3], rays2=[(1,) * 3, (1.4,) * 3, (0.8,) * 3, (0.2,) * 3, (TINY,) * 3],
        runes=[(1,) * 3, (1.4,) * 3, (0.8,) * 3, (0.2,) * 3, (TINY,) * 3], book=[(1,) * 3, (1,) * 3, (1,) * 3, (0.8,) * 3, (0.3,) * 3])


# ------------------------------------------------------------------------------------------------------------------ the Soul Paladin
def helm_canvas():
    """The great helm's front (9 x 9 px at 4 texels = 36 x 36): white enamel, a gold nose-guard ridge and brim, a T-shaped visor slit that glows with soul
    light, breath holes, rivets."""
    n = 36
    cv = Canvas(n, n)
    for y in range(n):
        for x in range(n):
            t = 0.72 - 0.25 * (abs(x - 17.5) / 17.5) ** 2 - 0.12 * (y / n) + 0.04 * nz(x * 0.3, y * 0.3, 8)
            cv.put(x, y, pal(PLATE_W, t, x, y, 0.05))
    for x in range(n):                                                           # the gold brim above and the gold band below
        for y in (3, 4):
            cv.put(x, y, PLATE_G[3] if y == 3 else PLATE_G[1])
        for y in (31, 32):
            cv.put(x, y, PLATE_G[3] if y == 31 else PLATE_G[1])
    for y in range(5, 31):                                                       # the ridge
        for x in (16, 17, 18, 19):
            cv.put(x, y, PLATE_G[2] if x in (17, 18) else PLATE_G[1])
    for x in range(5, 31):                                                       # the visor slit
        for y in (13, 14, 15):
            if not (16 <= x <= 19):
                cv.put(x, y, (14, 22, 34), 255, SOUL_GLOW, 255 if y == 14 else 200)
                if y == 14:
                    cv.put(x, y, (200, 250, 255), 255, SOUL_GLOW, 255)
    for y in range(16, 27):
        for x in (16, 17, 18, 19):
            if x in (17, 18):
                cv.put(x, y, (14, 22, 34), 255, SOUL_GLOW, 220)
    for (cx, cy) in ((8, 22), (11, 23), (8, 25), (11, 26), (27, 22), (24, 23), (27, 25), (24, 26)):    # breath holes
        cv.put(cx, cy, (24, 30, 44))
    for (cx, cy) in ((3, 8), (32, 8), (3, 28), (32, 28)):                         # rivets
        cv.rect(cx - 1, cy - 1, cx, cy, PLATE_G[4], 255, SOUL_GLOW, 120)
    return cv


def chest_canvas():
    """The breastplate (9 x 11 px at 2 texels = 18 x 22): gold edging, a belt line and a soul sigil: rings, eight rays and a bright core."""
    cv = Canvas(18, 22)
    for y in range(22):
        for x in range(18):
            t = 0.7 - 0.2 * (abs(x - 8.5) / 8.5) ** 2 + 0.03 * nz(x * 0.4, y * 0.4, 9)
            cv.put(x, y, pal(PLATE_W, t, x, y, 0.05))
    for x in range(18):
        cv.put(x, 0, PLATE_G[3])
        cv.put(x, 21, PLATE_G[1])
        cv.put(x, 19, PLATE_G[2])
    for y in range(22):
        cv.put(0, y, PLATE_G[2])
        cv.put(17, y, PLATE_G[1])
    cx, cy = 8.5, 9.0
    for y in range(22):
        for x in range(18):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx)) % 45.0
            ray = min(ang, 45 - ang) * math.radians(1) * r < 0.55 and 2.6 < r < 7.0
            if abs(r - 6.0) < 0.55 or abs(r - 3.4) < 0.5 or ray:
                cv.put(x, y, PLATE_G[3], 255, SOUL_GLOW, 180)
            if r < 1.6:
                cv.put(x, y, (220, 252, 255), 255, SOUL_GLOW, 255)
    return cv


def tabard_paint(salt, sigil=True):
    base = fabric(TABARD, salt=salt, period=7.0, amp=0.16, hem=2, hem_cols=PLATE_G, dither=0.04, seam=False)

    def p(face, i, j, fw, fh):
        if face in ("front", "back"):
            if i < 2 or i >= fw - 2:
                return px(PLATE_G[3] if i % 2 == 0 else PLATE_G[1]), None
            if sigil and abs(i - fw / 2.0 + 0.5) < 1.0 and 3 <= j <= fh - 5:
                return px(PLATE_G[3]), px(SOUL_GLOW, 130)
        return base(face, i, j, fw, fh)
    return p


def blade_paint():
    def p(face, i, j, fw, fh):
        v = j / max(1, fh - 1)
        t = 0.62 + 0.2 * (1 - v) - 0.2 * (abs(i - (fw - 1) / 2.0) / max(1.0, fw / 2.0))
        edge = i == 0 or i == fw - 1
        c = pal(PLATE_W, t, i, j, 0.03)
        if face in ("front", "back") and abs(i - (fw - 1) / 2.0) < 0.6:
            c = PLATE_G[2]
        g = px(SOUL_GLOW, 235 if edge else 90) if face in ("front", "back", "left", "right") else None
        return px(c), g
    return p


def build_paladin():
    b = Build("soul_paladin", 512, 512)
    A = b.A
    wh = metal(PLATE_W, salt=1, glow=(*SOUL_GLOW, 26), emboss=False, lift=0.3)
    gd = metal(PLATE_G, salt=2, glow=(*SOUL_GLOW, 44), emboss=False, lift=0.16)
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 13, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("crest", "head", (0, 33, 0))
    b.bone("cape", "body", (0, 23, 3.0))
    b.bone("cape_bottom", "cape", (0, 12, 3.2))
    b.bone("tabard_f", "body", (0, 13, -2.9))
    b.bone("tabard_b", "body", (0, 13, 2.9))
    b.bone("shield", "body", (8.5, 17, -4.4))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 6.5, 22.5, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 6.5, 15.5, 0))
        b.bone("leg" + suf, "root", (sg * 2.4, 13, 0))
        b.bone("shin" + suf, "leg" + suf, (sg * 2.4, 6.5, 0))
    b.bone("sword", "forearm_r", (-6.5, 6.0, 0))
    # torso, belt, gorget
    b.box("body", (-4.5, 13, -2.75), (9, 11, 5.5), canvas_paint(chest_canvas(), "front", wh))
    b.box("body", (-4.8, 12.2, -3.0), (9.6, 2, 6), gd, faces=SIDES + ("top",))
    b.box("body", (-1.0, 12.4, -3.3), (2, 1.6, 0.6), gd, faces=SIDES + ("top",))
    b.box("body", (-3.5, 23.4, -3.0), (7, 2.4, 6), wh)
    # head: the great helm, 4 texels on its front
    helm = canvas_paint(helm_canvas(), "front", wh)
    b.box("head", (-4.5, 24, -4.5), (9, 9, 9), helm, dens={"front": 4}, faces=("front", "left", "right", "back", "top"))
    b.box("head", (-4.8, 24.0, -4.8), (9.6, 1.4, 9.6), gd, faces=SIDES + ("top",))
    # crest: a gold fin and a swept white plume
    b.box("crest", (-0.75, 32.8, -4.2), (1.5, 4.0, 9.0), gd, faces=SIDES + ("top",))
    plume = [clump(PLATE_W, salt=200 + k, strands=2.4, root=0.1, curl=((-1) ** k) * 0.15, tip_cols=((160, 236, 255), (210, 248, 255)), glow=(*SOUL_GLOW, 120), dither=0.04) for k in range(3)]
    for k in range(7):
        z = -3.0 + k * 1.2
        b.clump("crest", (0.0, 35.6, z), 180, 40 + 6 * k, 3.0, 6.0 - 0.3 * abs(k - 3), plume[k % 3], standing=True, key="pl%d" % (k % 3))
    # pauldrons (three overlapping lames over a round cap), arms, gauntlets
    b.pair("arm_l", "arm_r", (4.0, 21.6, -3.2), (4.6, 3.4, 6.4), wh, rotation=(0, 0, -8), pivot=(5.0, 24.0, 0))
    b.pair("arm_l", "arm_r", (5.2, 20.4, -3.7), (5.0, 2.0, 7.4), gd, rotation=(0, 0, -20), pivot=(5.0, 24.0, 0))
    b.pair("arm_l", "arm_r", (6.4, 18.8, -4.0), (4.6, 2.0, 8.0), wh, rotation=(0, 0, -34), pivot=(5.0, 24.0, 0))
    b.pair("arm_l", "arm_r", (7.4, 17.0, -4.2), (3.6, 1.8, 8.4), gd, rotation=(0, 0, -48), pivot=(5.0, 24.0, 0))
    b.spike("arm_l", (6.4, 24.6, 0.0), 1.5, 3.2, 0, -26, gd, fracs=(0.6, 0.4), taper=0.5)
    b.spike("arm_r", (-6.4, 24.6, 0.0), 1.5, 3.2, 0, 26, gd, fracs=(0.6, 0.4), taper=0.5)
    b.pair("arm_l", "arm_r", (4.6, 15.5, -2.0), (3.8, 6.0, 4.0), wh)
    b.pair("forearm_l", "forearm_r", (4.4, 14.7, -2.5), (4.2, 2.0, 5.0), gd)
    b.pair("forearm_l", "forearm_r", (4.7, 9.0, -2.0), (3.6, 6.0, 4.0), wh)
    b.pair("forearm_l", "forearm_r", (4.5, 8.0, -2.3), (4.0, 1.4, 4.6), gd)
    b.pair("forearm_l", "forearm_r", (4.6, 5.0, -2.2), (3.8, 3.2, 4.4), wh)
    # a winged helm: two small feathered wings on each side
    wing_f = feather(PLATE_W, tip_glow=(*SOUL_GLOW, 150), salt=3, dither=0.04, tip_frac=0.2)
    wing_p = lambda face, i, j, fw, fh: wing_f(face, i, fh - 1 - j, fw, fh)                  # the tip is the top row of a standing wing
    for k, (dy, ang, ln) in enumerate(((0.0, 36, 6.0), (-1.6, 58, 5.0))):
        b.pair("head", "head", (4.9, 29.0 + dy, -1.5), (1.0, ln, 3.0), wing_p, rotation=(0, 0, -ang), pivot=(4.9, 29.0 + dy, 0), faces=("left", "right"), dens=4,
               key="hw%d" % k)
    # legs
    b.pair("leg_l", "leg_r", (0.4, 6.8, -2.4), (4.0, 6.4, 4.8), wh)
    b.pair("shin_l", "shin_r", (0.5, 5.8, -3.4), (3.6, 2.0, 1.2), gd, faces=SIDES + ("top",))
    b.pair("shin_l", "shin_r", (0.5, 2.0, -2.2), (3.9, 4.6, 4.4), wh)
    b.pair("shin_l", "shin_r", (0.3, 0.0, -4.6), (4.3, 2.4, 7.4), gd)
    # tabard and cape
    b.box("tabard_f", (-3.5, 3.2, -3.5), (7, 9.8, 0.8), tabard_paint(1))
    b.box("tabard_b", (-3.5, 3.2, 2.7), (7, 9.8, 0.8), tabard_paint(2, False))
    capep = fabric(TABARD, salt=6, period=8.0, amp=0.18, hem=3, hem_cols=PLATE_G, dither=0.04, seam=False)
    b.box("cape", (-5.5, 11.5, 3.2), (11, 12.0, 0.9), capep)
    b.box("cape_bottom", (-6.0, 1.0, 3.4), (12, 11.2, 0.9), capep)
    # sword (along the forearm's axis, the hilt in the fist) and shield
    b.box("sword", (-0.0 - 7.25, 6.4, -0.4), (1.5, 1.0, 0.8), gd)
    b.box("sword", (-9.0, 5.6, -0.7), (5.0, 0.9, 1.4), gd)
    b.box("sword", (-7.25, -10.4, -0.4), (1.5, 16.0, 0.8), blade_paint(), faces=("front", "back", "left", "right", "bottom"), dens=4)
    b.box("sword", (-7.0, 8.0, -0.5), (1.0, 3.0, 1.0), gd)
    shield = canvas_paint(shield_canvas(), "front", wh)
    b.box("shield", (4.5, 6.0, -5.4), (8.0, 14.0, 1.2), shield, dens={"front": 4}, faces=("front", "back", "left", "right", "top", "bottom"))
    knight_clips(A)
    b.m.tex_h = 512
    return b


def shield_canvas():
    """A heater shield (8 x 14 px at 4 texels = 32 x 56): white enamel, a gold rim, a soul sigil."""
    w, h = 32, 56
    cv = Canvas(w, h)
    for y in range(h):
        for x in range(w):
            half = (w / 2.0) * (1.0 if y < 30 else max(0.0, 1.0 - ((y - 30) / 26.0) ** 1.5))
            if abs(x + 0.5 - w / 2.0) > half:
                continue
            edge = half - abs(x + 0.5 - w / 2.0) < 2.2 or y < 2
            t = 0.7 - 0.2 * (abs(x - 15.5) / 16.0) ** 2 + 0.04 * nz(x * 0.3, y * 0.3, 12)
            cv.put(x, y, PLATE_G[3 if (x + y) % 4 else 2] if edge else pal(PLATE_W, t, x, y, 0.05), 255, SOUL_GLOW if edge else None, 60)
    cx, cy = 15.5, 22.0
    for y in range(h):
        for x in range(w):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.degrees(math.atan2(dy, dx)) % 90.0
            arm = min(ang, 90 - ang) * math.radians(1) * r < 1.1 and r < 12
            if abs(r - 9) < 0.9 or arm:
                cv.put(x, y, PLATE_G[3], 255, SOUL_GLOW, 190)
            if r < 2.4:
                cv.put(x, y, (220, 252, 255), 255, SOUL_GLOW, 255)
    return cv


def knight_clips(a):
    S, C = SIN, COS
    L = 4.0
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.2 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.5, 1.0, 0.3), 0.6))
    cyc(a, "idle", "head", "rot", L, swing((0, 0, 0), (1.0, 3.0, 0.8), 0.7))
    cyc(a, "idle", "crest", "rot", L, swing((-6, 0, 0), (2.0, 0, 1.0), 1.0))
    cyc(a, "idle", "cape", "rot", L, swing((-3, 0, 0), (1.2, 0, 0.6), 0.6))
    cyc(a, "idle", "cape_bottom", "rot", L, swing((-3, 0, 0), (2.2, 0, 1.2), 1.2))
    cyc(a, "idle", "tabard_f", "rot", L, swing((0, 0, 0), (1.2, 0, 0.4), 0.3))
    cyc(a, "idle", "tabard_b", "rot", L, swing((0, 0, 0), (-1.2, 0, 0.4), 0.9))
    cyc(a, "idle", "arm_r", "rot", L, swing((12, 0, -18), (1.5, 0, 1.0), 0.4))
    cyc(a, "idle", "forearm_r", "rot", L, swing((150, 0, 0), (2.0, 0, 0), 1.0))
    cyc(a, "idle", "arm_l", "rot", L, swing((24, 0, 12), (1.5, 0, 1.0), 1.4))
    cyc(a, "idle", "forearm_l", "rot", L, swing((60, 0, 0), (2.0, 0, 0), 2.0))
    cyc(a, "idle", "shield", "rot", L, swing((0, -14, 0), (1.0, 1.0, 0.5), 1.6))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "idle", "leg" + sd, "rot", L, lambda t, sg=sg: (0, 0, 3 * sg))
    L = 1.0
    a.clip("walk", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "walk", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (28 * S(t + ph), 0, 2 * sg))
        cyc(a, "walk", "shin" + sd, "rot", L, lambda t, ph=ph: (-34 * max(0.0, C(t + ph)) ** 1.2 - 4, 0, 0))
        cyc(a, "walk", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-26 * S(t + ph) + 10, 0, 9 * sg))
        cyc(a, "walk", "forearm" + sd, "rot", L, lambda t, ph=ph: (20 + 14 * max(0.0, -S(t + ph)), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.5 * C(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (-1.5, 3.0 * S(t), 1.0 * S(t)))
    cyc(a, "walk", "head", "rot", L, lambda t: (1.0 * S(2 * t + 1.6), -2.5 * S(t), 0))
    cyc(a, "walk", "crest", "rot", L, lambda t: (-12 + 3 * S(2 * t + 0.4), 0, 1.2 * S(t)))
    cyc(a, "walk", "cape", "rot", L, lambda t: (-8 + 3 * S(2 * t + 0.5), 0, 1.5 * S(t)))
    cyc(a, "walk", "cape_bottom", "rot", L, lambda t: (-12 + 5 * S(2 * t + 1.1), 0, 2.5 * S(t + 0.4)))
    cyc(a, "walk", "tabard_f", "rot", L, lambda t: (10 * S(t) * 0.5 + 2, 0, 0))
    cyc(a, "walk", "tabard_b", "rot", L, lambda t: (-10 * S(t) * 0.5 - 2, 0, 0))
    L = 0.6
    a.clip("run", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "run", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (52 * S(t + ph), 0, 2 * sg))
        cyc(a, "run", "shin" + sd, "rot", L, lambda t, ph=ph: (-78 * max(0.0, C(t + ph + 0.3)) ** 1.1 - 8, 0, 0))
        cyc(a, "run", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-46 * S(t + ph) + 14, 0, 6 * sg))
        cyc(a, "run", "forearm" + sd, "rot", L, lambda t, ph=ph: (60 + 24 * max(0.0, -S(t + ph)), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 1.1 * C(2 * t) + 0.2, 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-11 + 1.5 * S(2 * t), 5.0 * S(t), 1.5 * S(t)))
    cyc(a, "run", "head", "rot", L, lambda t: (6 + 1.5 * S(2 * t + 1.6), -4.0 * S(t), 0))
    cyc(a, "run", "crest", "rot", L, lambda t: (-26 + 4 * S(2 * t + 0.4), 0, 2.0 * S(t)))
    cyc(a, "run", "cape", "rot", L, lambda t: (-26 + 5 * S(2 * t + 0.5), 0, 2.5 * S(t)))
    cyc(a, "run", "cape_bottom", "rot", L, lambda t: (-34 + 8 * S(2 * t + 1.1), 0, 4 * S(t + 0.4)))
    cyc(a, "run", "tabard_f", "rot", L, lambda t: (22 + 8 * S(2 * t), 0, 0))
    cyc(a, "run", "tabard_b", "rot", L, lambda t: (-22 - 8 * S(2 * t), 0, 0))
    L = 0.9
    a.clip("attack", L, False)
    T = (0.0, 0.26, 0.4, 0.52, 0.9)
    seq(a, "attack", "rot", T,
        arm_r=[(12, 0, -18), (-50, -10, -26), (96, 6, -6), (70, 4, -8), (12, 0, -18)],
        forearm_r=[(150, 0, 0), (110, 0, 0), (12, 0, 0), (30, 0, 0), (150, 0, 0)],
        arm_l=[(24, 0, 12), (40, 0, 14), (30, 0, 14), (26, 0, 12), (24, 0, 12)],
        body=[(0, 0, 0), (6, 16, 0), (-12, -18, 0), (-7, -9, 0), (0, 0, 0)],
        head=[(0, 0, 0), (3, 10, 0), (-6, -9, 0), (-3, -4, 0), (0, 0, 0)],
        leg_l=[(0, 0, 0), (-8, 0, 0), (30, 0, 0), (16, 0, 0), (0, 0, 0)], leg_r=[(0, 0, 0), (10, 0, 0), (-20, 0, 0), (-10, 0, 0), (0, 0, 0)],
        shin_r=[(0, 0, 0), (-26, 0, 0), (-30, 0, 0), (-12, 0, 0), (0, 0, 0)],
        cape=[(-3, 0, 0), (-14, 0, 0), (10, 0, 0), (-4, 0, 0), (-3, 0, 0)], cape_bottom=[(-3, 0, 0), (-22, 0, 0), (14, 0, 0), (-6, 0, 0), (-3, 0, 0)],
        crest=[(-6, 0, 0), (-14, 0, 0), (6, 0, 0), (-4, 0, 0), (-6, 0, 0)], shield=[(0, -14, 0), (0, -26, 0), (0, -6, 0), (0, -12, 0), (0, -14, 0)])
    seq(a, "attack", "pos", T, body=[(0, 0, 0), (0, 0.2, 1.2), (0, -0.8, -2.6), (0, -0.3, -1.2), (0, 0, 0)], shield=[(0, 0, 0), (0, 0, 1.0), (0, 0, -1.6), (0, 0, -0.6), (0, 0, 0)])
    L = 1.2
    a.clip("cast", L, True)
    cyc(a, "cast", "arm_r", "rot", L, lambda t: (176 + 3 * S(t), 0, -10))
    cyc(a, "cast", "forearm_r", "rot", L, lambda t: (4 + 2 * S(t + 0.6), 0, 0))
    cyc(a, "cast", "arm_l", "rot", L, lambda t: (100 + 4 * S(t + 1.4), 6, 14))
    cyc(a, "cast", "forearm_l", "rot", L, lambda t: (30 + 6 * S(t + 2.0), 0, 0))
    cyc(a, "cast", "body", "pos", L, lambda t: (0, 0.3 + 0.3 * S(t), 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (-4 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (-8 + 1.5 * S(t + 0.4), 2.0 * S(t), 0))
    cyc(a, "cast", "crest", "rot", L, lambda t: (-14 + 3 * S(t + 0.6), 0, 2.0 * S(t)))
    cyc(a, "cast", "cape", "rot", L, lambda t: (-13 + 4 * S(t + 1.0), 0, 2.0 * S(t)))
    cyc(a, "cast", "cape_bottom", "rot", L, lambda t: (-18 + 6 * S(t + 1.6), 0, 3.0 * S(t + 0.5)))
    cyc(a, "cast", "shield", "rot", L, lambda t: (-20, -20 + 3 * S(t), 0))
    L = 2.0
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "arm_r", "rot", L, lambda t: (170 + 3 * S(t), 0, -22))
    cyc(a, "cast_big", "arm_l", "rot", L, lambda t: (-8 + 3 * S(t + 1), 0, 84))
    cyc(a, "cast_big", "forearm_r", "rot", L, lambda t: (6 + 3 * S(t + 0.6), 0, 0))
    cyc(a, "cast_big", "forearm_l", "rot", L, lambda t: (14 + 4 * S(t + 1.6), 0, 0))
    cyc(a, "cast_big", "root", "pos", L, lambda t: (0, 3.0 + 1.0 * S(t), 0))
    cyc(a, "cast_big", "body", "rot", L, lambda t: (-8 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast_big", "head", "rot", L, lambda t: (-16 + 2 * S(t + 0.4), 0, 0))
    cyc(a, "cast_big", "crest", "rot", L, lambda t: (-24 + 5 * S(t + 0.6), 0, 3 * S(t)))
    cyc(a, "cast_big", "cape", "rot", L, lambda t: (-30 + 4 * S(t + 1), 0, 2 * S(t)))
    cyc(a, "cast_big", "cape_bottom", "rot", L, lambda t: (-42 + 6 * S(t + 1.6), 0, 4 * S(t + 0.5)))
    cyc(a, "cast_big", "shield", "rot", L, lambda t: (-10, -50 + 4 * S(t), 0))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "cast_big", "leg" + sd, "rot", L, lambda t, sg=sg: (-8, 0, 5 * sg))
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    seq(a, "hurt", "rot", T, body=[(0, 0, 0), (10, 0, 3), (0, 0, 0)], head=[(0, 0, 0), (14, 6, 0), (0, 0, 0)],
        arm_l=[(24, 0, 12), (-16, 0, 26), (24, 0, 12)], arm_r=[(12, 0, -18), (-24, 0, -30), (12, 0, -18)],
        cape=[(-3, 0, 0), (-14, 0, 0), (-3, 0, 0)], cape_bottom=[(-3, 0, 0), (-22, 0, 0), (-3, 0, 0)], crest=[(-6, 0, 0), (-18, 0, 0), (-6, 0, 0)])
    seq(a, "hurt", "pos", T, body=[(0, 0, 0), (0, 0, 1.6), (0, 0, 0)])
    L = 2.8
    a.clip("transform", L, False)
    T = (0.0, 0.8, 1.6, 2.2, 2.8)
    seq(a, "transform", "rot", T, body=[(0, 0, 0), (-14, 0, 0), (-4, 0, 0), (2, 0, 0), (0, 0, 0)], head=[(30, 0, 0), (20, 0, 0), (-6, 0, 0), (-3, 0, 0), (0, 0, 0)],
        leg_l=[(70, 0, 4), (40, 0, 4), (6, 0, 2), (0, 0, 0), (0, 0, 0)], leg_r=[(-10, 0, -4), (-8, 0, -4), (-2, 0, -2), (0, 0, 0), (0, 0, 0)],
        shin_l=[(-100, 0, 0), (-60, 0, 0), (-8, 0, 0), (0, 0, 0), (0, 0, 0)], shin_r=[(-110, 0, 0), (-70, 0, 0), (-8, 0, 0), (0, 0, 0), (0, 0, 0)],
        arm_r=[(30, 0, -10), (40, 0, -14), (150, 0, -20), (30, 0, -18), (12, 0, -18)], forearm_r=[(90, 0, 0), (100, 0, 0), (40, 0, 0), (150, 0, 0), (150, 0, 0)],
        arm_l=[(30, 0, 10), (20, 0, 14), (24, 0, 14), (24, 0, 12), (24, 0, 12)], forearm_l=[(70, 0, 0), (66, 0, 0), (60, 0, 0), (60, 0, 0), (60, 0, 0)])
    seq(a, "transform", "pos", T, root=[(0, -7.5, 0), (0, -6.5, 0), (0, -1.5, 0), (0, 0, 0), (0, 0, 0)])
    L = 2.6
    a.clip("death", L, False)
    T = (0.0, 0.4, 1.1, 1.8, 2.6)
    seq(a, "death", "rot", T, body=[(0, 0, 0), (12, 0, 4), (-8, 0, 0), (-24, 0, 0), (-8, 0, 0)], head=[(0, 0, 0), (18, 0, 0), (-8, 0, 0), (-30, 0, 6), (-10, 0, 10)],
        arm_l=[(24, 0, 12), (-24, 0, 24), (24, 0, 18), (30, 0, 26), (-6, 0, 32)], arm_r=[(12, 0, -18), (-24, 0, -24), (24, 0, -18), (30, 0, -26), (-6, 0, -32)],
        forearm_r=[(150, 0, 0), (60, 0, 0), (20, 0, 0), (10, 0, 0), (6, 0, 0)],
        leg_l=[(0, 0, 0), (-10, 0, 0), (62, 0, 4), (80, 0, 5), (8, 0, 0)], leg_r=[(0, 0, 0), (10, 0, 0), (-60, 0, -4), (-70, 0, -5), (8, 0, 0)],
        shin_l=[(0, 0, 0), (-20, 0, 0), (-118, 0, 0), (-124, 0, 0), (-10, 0, 0)], shin_r=[(0, 0, 0), (-20, 0, 0), (-118, 0, 0), (-124, 0, 0), (-10, 0, 0)],
        cape=[(-3, 0, 0), (-14, 0, 0), (-4, 0, 0), (-8, 0, 0), (18, 0, 0)], cape_bottom=[(-3, 0, 0), (-26, 0, 0), (6, 0, 0), (-4, 0, 0), (26, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 84)])
    seq(a, "death", "pos", T, body=[(0, 0, 0), (0, 0, 2.0), (0, -6.0, 0), (0, -7.4, 0), (0, -9.5, 0)], root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-4, 3, 0)])


# ------------------------------------------------------------------------------------------------------------------ animation of the base form
TINY = 0.01


def seq(a, clip, kind, T, **tracks):
    """keys() for several bones at once: seq(a, "attack", "rot", T, arm_r=[v0, v1, ...], body=[...]) (one value per time in T)."""
    for bone, vals in tracks.items():
        keys(a, clip, bone, kind, list(zip(T, vals)))


def hold(a, clip, bone, kind, v):
    L = a.clips[clip]["animation_length"]
    (a.rot if kind == "rot" else a.pos if kind == "pos" else a.scale)(clip, bone, {0.0: tuple(v), L: tuple(v)})


def base_clips(a):
    S, C = SIN, COS
    # ------------------------------------------------------------------------------------------------------- idle (loop 4 s): the pose of the reference
    L = 4.0
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.2 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.6, 1.4, 0.4), 0.6))
    cyc(a, "idle", "body", "scale", L, lambda t: (1, 1 + 0.007 * S(t), 1 + 0.012 * S(t)))
    cyc(a, "idle", "head", "rot", L, swing((1.5, 0, 0), (1.5, 4.0, 1.2), 0.7))
    cyc(a, "idle", "hair", "rot", L, swing((0, 0, 0), (2.0, 0, 1.6), 1.2))
    cyc(a, "idle", "fur", "rot", L, swing((0, 0, 0), (0.7, 0, 0.7), 0.4))
    cyc(a, "idle", "fur", "scale", L, lambda t: (1, 1 + 0.01 * (S(t + 0.3) - S(0.3)), 1))
    cyc(a, "idle", "cloak_top", "rot", L, swing((-2, 0, 0), (1.4, 0, 0.8), 0.6))
    cyc(a, "idle", "cloak_bottom", "rot", L, swing((-2, 0, 0), (2.4, 0, 1.4), 1.2))
    cyc(a, "idle", "cloak_l", "rot", L, swing((0, 0, 2), (0.6, 0, 1.4), 0.9))
    cyc(a, "idle", "cloak_r", "rot", L, swing((0, 0, -2), (0.6, 0, 1.4), 2.1))
    cyc(a, "idle", "arm_l", "rot", L, swing((20, 0, 9), (2.0, 0, 1.4), 0.4))
    cyc(a, "idle", "forearm_l", "rot", L, swing((134, 0, 0), (4.0, 0, 0), 1.0))
    cyc(a, "idle", "spark", "scale", L, lambda t: (0.62 + 0.14 * S(2 * t), 0.62 + 0.14 * S(2 * t), 0.62 + 0.14 * S(2 * t)), 1)
    spin(a, "idle", "spark", 1, 1)
    cyc(a, "idle", "arm_r", "rot", L, swing((8, 0, -9), (2.0, 0, -1.4), 1.8))
    cyc(a, "idle", "forearm_r", "rot", L, swing((30, 0, 0), (4.0, 0, 0), 2.6))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "idle", "skirt" + sd, "rot", L, lambda t, sg=sg: (0.7 * S(t + 0.5 * sg), 0, 0.5 * sg * S(t)))

    # ------------------------------------------------------------------------------------------------------- walk (loop 1.1 s): the long robe keeps the stride short
    L = 1.1
    a.clip("walk", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "walk", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (20 * S(t + ph), 0, 1.5 * sg))
        cyc(a, "walk", "skirt" + sd, "rot", L, lambda t, ph=ph, sg=sg: (11 * S(t + ph), 0, 1.0 * sg))
        cyc(a, "walk", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-22 * S(t + ph), 0, 7 * sg))
        cyc(a, "walk", "forearm" + sd, "rot", L, lambda t, ph=ph: (14 + 12 * max(0.0, S(t + ph)), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.45 * COS(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (-1.5, 3.0 * S(t), 1.0 * S(t)))
    cyc(a, "walk", "head", "rot", L, lambda t: (1.5 + 1.0 * S(2 * t + 1.6), -2.5 * S(t), -0.8 * S(t)))
    cyc(a, "walk", "hair", "rot", L, lambda t: (3.0 * S(2 * t + 0.4), 0, 1.2 * S(t)))
    cyc(a, "walk", "fur", "rot", L, lambda t: (1.0 * S(2 * t + 0.8), 0, 0.8 * S(t)))
    cyc(a, "walk", "cloak_top", "rot", L, lambda t: (-7 + 3.0 * S(2 * t + 0.5), 0, 1.5 * S(t)))
    cyc(a, "walk", "cloak_bottom", "rot", L, lambda t: (-10 + 5.0 * S(2 * t + 1.1), 0, 2.5 * S(t + 0.4)))
    cyc(a, "walk", "cloak_l", "rot", L, lambda t: (-3 + 3 * S(2 * t), 0, 3 + 2 * S(t)))
    cyc(a, "walk", "cloak_r", "rot", L, lambda t: (-3 + 3 * S(2 * t + 1), 0, -3 - 2 * S(t)))
    hold(a, "walk", "spark", "scale", (TINY,) * 3)
    cyc(a, "walk", "robe_front", "rot", L, lambda t: (3 + 5 * S(2 * t + 0.3), 0, 0))

    # ------------------------------------------------------------------------------------------------------- run (loop 0.62 s)
    L = 0.62
    a.clip("run", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "run", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (40 * S(t + ph), 0, 2 * sg))
        cyc(a, "run", "skirt" + sd, "rot", L, lambda t, ph=ph, sg=sg: (26 * S(t + ph), 0, 2.0 * sg))
        cyc(a, "run", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-46 * S(t + ph), 0, 10 * sg))
        cyc(a, "run", "forearm" + sd, "rot", L, lambda t, ph=ph: (60 + 26 * max(0.0, S(t + ph)), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 1.0 * COS(2 * t) + 0.2, 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-12 + 1.5 * S(2 * t), 5.0 * S(t), 1.5 * S(t)))
    cyc(a, "run", "head", "rot", L, lambda t: (8 + 1.5 * S(2 * t + 1.6), -4.0 * S(t), 0))
    cyc(a, "run", "hair", "rot", L, lambda t: (-14 + 4.0 * S(2 * t + 0.4), 0, 2.0 * S(t)))
    cyc(a, "run", "fur", "rot", L, lambda t: (-3 + 2.0 * S(2 * t + 0.8), 0, 1.2 * S(t)))
    cyc(a, "run", "cloak_top", "rot", L, lambda t: (-26 + 5.0 * S(2 * t + 0.5), 0, 2.5 * S(t)))
    cyc(a, "run", "cloak_bottom", "rot", L, lambda t: (-34 + 8.0 * S(2 * t + 1.1), 0, 4 * S(t + 0.4)))
    cyc(a, "run", "cloak_l", "rot", L, lambda t: (-22 + 5 * S(2 * t), 0, 8 + 3 * S(t)))
    cyc(a, "run", "cloak_r", "rot", L, lambda t: (-22 + 5 * S(2 * t + 1), 0, -8 - 3 * S(t)))
    hold(a, "run", "spark", "scale", (TINY,) * 3)
    cyc(a, "run", "robe_front", "rot", L, lambda t: (14 + 6 * S(2 * t + 0.3), 0, 0))

    # ------------------------------------------------------------------------------------------------------- attack (one-shot 0.9 s): a back-handed strike with the right hand
    L = 0.9
    a.clip("attack", L, False)
    T = (0.0, 0.26, 0.4, 0.52, 0.9)
    seq(a, "attack", "rot", T,
        arm_r=[(8, 0, -9), (-62, -10, -26), (96, 8, -4), (70, 4, -6), (8, 0, -9)],
        forearm_r=[(30, 0, 0), (80, 0, 0), (8, 0, 0), (20, 0, 0), (30, 0, 0)],
        arm_l=[(20, 0, 9), (-30, 0, 14), (-16, 0, 12), (-6, 0, 10), (20, 0, 9)],
        forearm_l=[(134, 0, 0), (30, 0, 0), (20, 0, 0), (24, 0, 0), (134, 0, 0)],
        body=[(0, 0, 0), (6, 14, 0), (-14, -16, 0), (-8, -8, 0), (0, 0, 0)],
        head=[(1.5, 0, 0), (3, 10, 0), (-6, -9, 0), (-3, -4, 0), (1.5, 0, 0)],
        leg_l=[(0, 0, 0), (-8, 0, 0), (26, 0, 0), (14, 0, 0), (0, 0, 0)],
        leg_r=[(0, 0, 0), (10, 0, 0), (-18, 0, 0), (-8, 0, 0), (0, 0, 0)],
        skirt_l=[(0, 0, 0), (-6, 0, 0), (16, 0, 0), (8, 0, 0), (0, 0, 0)],
        skirt_r=[(0, 0, 0), (6, 0, 0), (-12, 0, 0), (-5, 0, 0), (0, 0, 0)],
        cloak_top=[(-2, 0, 0), (-14, 0, 0), (10, 0, 0), (-4, 0, 0), (-2, 0, 0)],
        cloak_bottom=[(-2, 0, 0), (-22, 0, 0), (14, 0, 0), (-6, 0, 0), (-2, 0, 0)],
        hair=[(0, 0, 0), (-6, 0, 0), (10, 0, 0), (2, 0, 0), (0, 0, 0)],
        fur=[(0, 0, 0), (-4, 0, 0), (5, 0, 0), (0, 0, 0), (0, 0, 0)])
    seq(a, "attack", "pos", T, body=[(0, 0, 0), (0, 0.2, 1.2), (0, -0.8, -2.6), (0, -0.3, -1.2), (0, 0, 0)])
    seq(a, "attack", "scale", T, spark=[(0.62,) * 3, (0.9,) * 3, (0.4,) * 3, (0.55,) * 3, (0.62,) * 3])

    # ------------------------------------------------------------------------------------------------------- cast (loop 1.2 s): both hands raised, the spark swelling
    L = 1.2
    a.clip("cast", L, True)
    cyc(a, "cast", "arm_l", "rot", L, lambda t: (76 + 4 * S(t), 0, 52 + 3 * S(t + 1)))
    cyc(a, "cast", "forearm_l", "rot", L, lambda t: (34 + 8 * S(t + 0.6), 0, 0))
    cyc(a, "cast", "arm_r", "rot", L, lambda t: (66 + 4 * S(t + 1.4), 0, -48 - 3 * S(t)))
    cyc(a, "cast", "forearm_r", "rot", L, lambda t: (30 + 8 * S(t + 2.0), 0, 0))
    cyc(a, "cast", "spark", "scale", L, lambda t: (1.35 + 0.3 * S(t), 1.35 + 0.3 * S(t), 1.35 + 0.3 * S(t)))
    spin(a, "cast", "spark", 2, 1)
    cyc(a, "cast", "body", "pos", L, lambda t: (0, 0.5 + 0.4 * S(t), 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (3 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (-5 + 1.5 * S(t + 0.4), 2.0 * S(t), 0))
    cyc(a, "cast", "hair", "rot", L, lambda t: (-9 + 3.0 * S(t + 0.6), 0, 2.0 * S(t)))
    cyc(a, "cast", "fur", "rot", L, lambda t: (-2 + 1.2 * S(t + 0.8), 0, 1.0 * S(t)))
    cyc(a, "cast", "cloak_top", "rot", L, lambda t: (-13 + 4.0 * S(t + 1.0), 0, 2.0 * S(t)))
    cyc(a, "cast", "cloak_bottom", "rot", L, lambda t: (-18 + 6.0 * S(t + 1.6), 0, 3.0 * S(t + 0.5)))
    cyc(a, "cast", "cloak_l", "rot", L, lambda t: (-10 + 3 * S(t), 0, 6 + 2 * S(t + 1)))
    cyc(a, "cast", "cloak_r", "rot", L, lambda t: (-10 + 3 * S(t + 1), 0, -6 - 2 * S(t)))

    # ------------------------------------------------------------------------------------------------------- cast_big (loop 2.0 s): arms thrown wide, rising, the spark a sun
    L = 2.0
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "robe_front", "rot", L, lambda t: (10 + 3 * S(t), 0, 0))
    cyc(a, "cast_big", "arm_l", "rot", L, lambda t: (-8 + 3 * S(t), 0, 74 + 4 * S(t + 0.4)))
    cyc(a, "cast_big", "arm_r", "rot", L, lambda t: (-8 + 3 * S(t + 1), 0, -74 - 4 * S(t + 1.4)))
    cyc(a, "cast_big", "forearm_l", "rot", L, lambda t: (14 + 5 * S(t + 0.6), 0, 0))
    cyc(a, "cast_big", "forearm_r", "rot", L, lambda t: (14 + 5 * S(t + 1.6), 0, 0))
    cyc(a, "cast_big", "spark", "scale", L, lambda t: (3.2 + 0.6 * S(2 * t), 3.2 + 0.6 * S(2 * t), 3.2 + 0.6 * S(2 * t)))
    spin(a, "cast_big", "spark", 3, 1)
    cyc(a, "cast_big", "root", "pos", L, lambda t: (0, 4.0 + 1.0 * S(t), 0))
    cyc(a, "cast_big", "body", "rot", L, lambda t: (-6 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast_big", "head", "rot", L, lambda t: (-16 + 2.0 * S(t + 0.4), 0, 0))
    cyc(a, "cast_big", "hair", "rot", L, lambda t: (-24 + 5.0 * S(t + 0.6), 0, 3.0 * S(t)))
    cyc(a, "cast_big", "fur", "rot", L, lambda t: (-8 + 2 * S(t), 0, 1.4 * S(t + 1)))
    cyc(a, "cast_big", "fur", "scale", L, lambda t: (1.04, 1.1 + 0.02 * S(t), 1.05))
    cyc(a, "cast_big", "cloak_top", "rot", L, lambda t: (-30 + 4 * S(t + 1), 0, 2 * S(t)))
    cyc(a, "cast_big", "cloak_bottom", "rot", L, lambda t: (-42 + 6 * S(t + 1.6), 0, 4 * S(t + 0.5)))
    cyc(a, "cast_big", "cloak_l", "rot", L, lambda t: (-26 + 4 * S(t), 0, 18 + 4 * S(t + 1)))
    cyc(a, "cast_big", "cloak_r", "rot", L, lambda t: (-26 + 4 * S(t + 1), 0, -18 - 4 * S(t)))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "cast_big", "leg" + sd, "rot", L, lambda t, sg=sg: (-8, 0, 1.5 * sg))
        cyc(a, "cast_big", "skirt" + sd, "rot", L, lambda t, sg=sg: (-12 + 3 * S(t), 0, 2.0 * sg + 1 * S(t)))

    # ------------------------------------------------------------------------------------------------------- hurt (one-shot 0.45 s)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    seq(a, "hurt", "rot", T, body=[(0, 0, 0), (10, 0, 3), (0, 0, 0)], head=[(1.5, 0, 0), (14, 6, 0), (1.5, 0, 0)],
        arm_l=[(20, 0, 9), (-18, 0, 20), (20, 0, 9)], forearm_l=[(134, 0, 0), (60, 0, 0), (134, 0, 0)],
        arm_r=[(8, 0, -9), (-22, 0, -22), (8, 0, -9)], forearm_r=[(30, 0, 0), (50, 0, 0), (30, 0, 0)],
        cloak_top=[(-2, 0, 0), (-14, 0, 0), (-2, 0, 0)], cloak_bottom=[(-2, 0, 0), (-22, 0, 0), (-2, 0, 0)],
        hair=[(0, 0, 0), (-14, 0, 0), (0, 0, 0)], fur=[(0, 0, 0), (-6, 0, 0), (0, 0, 0)])
    seq(a, "hurt", "pos", T, body=[(0, 0, 0), (0, 0, 1.6), (0, 0, 0)])
    seq(a, "hurt", "scale", T, spark=[(0.62,) * 3, (0.2,) * 3, (0.62,) * 3])

    # ------------------------------------------------------------------------------------------------------- transform (one-shot 3.2 s): rises, arms wide, the spark swells into a sun; ends on the divine form's idle pose
    L = 3.2
    a.clip("transform", L, False)
    T = (0.0, 0.5, 1.0, 1.5, 2.2, 2.8, 3.2)
    seq(a, "transform", "rot", T,
        arm_l=[(20, 0, 9), (36, 0, 14), (14, 0, 48), (6, 0, 72), (6, 0, 78), (10, 0, 56), (14, 0, 44)],
        arm_r=[(8, 0, -9), (30, 0, -12), (14, 0, -48), (6, 0, -72), (6, 0, -78), (10, 0, -56), (14, 0, -44)],
        forearm_l=[(134, 0, 0), (96, 0, 0), (30, 0, 0), (12, 0, 0), (12, 0, 0), (14, 0, 0), (16, 0, 0)],
        forearm_r=[(30, 0, 0), (80, 0, 0), (30, 0, 0), (12, 0, 0), (12, 0, 0), (14, 0, 0), (16, 0, 0)],
        body=[(0, 0, 0), (-8, 0, 0), (4, 0, 0), (8, 0, 0), (8, 0, 0), (4, 0, 0), (0, 0, 0)],
        head=[(1.5, 0, 0), (-16, 0, 0), (-8, 0, 0), (-18, 0, 0), (-18, 0, 0), (-8, 0, 0), (-2, 0, 0)],
        hair=[(0, 0, 0), (10, 0, 0), (-22, 0, 4), (-30, 0, -4), (-30, 0, 4), (-10, 0, 0), (0, 0, 0)],
        fur=[(0, 0, 0), (6, 0, 0), (-8, 0, 0), (-12, 0, 0), (-12, 0, 0), (-4, 0, 0), (0, 0, 0)],
        cloak_top=[(-2, 0, 0), (4, 0, 0), (-24, 0, 5), (-34, 0, -4), (-34, 0, 5), (-12, 0, 0), (-4, 0, 0)],
        cloak_bottom=[(-2, 0, 0), (8, 0, 0), (-34, 0, -6), (-46, 0, 7), (-46, 0, -6), (-16, 0, 0), (-6, 0, 0)],
        cloak_l=[(0, 0, 2), (0, 0, 6), (-18, 0, 20), (-30, 0, 26), (-30, 0, 24), (-8, 0, 12), (0, 0, 6)],
        cloak_r=[(0, 0, -2), (0, 0, -6), (-18, 0, -20), (-30, 0, -26), (-30, 0, -24), (-8, 0, -12), (0, 0, -6)],
        leg_l=[(0, 0, 0), (30, 0, 0), (-4, 0, 6), (-8, 0, 8), (-8, 0, 8), (-3, 0, 2), (-2, 0, 0)],
        leg_r=[(0, 0, 0), (30, 0, 0), (-4, 0, -6), (-8, 0, -8), (-8, 0, -8), (-3, 0, -2), (-2, 0, 0)],
        skirt_l=[(0, 0, 0), (14, 0, 0), (-10, 0, 2), (-14, 0, 3), (-14, 0, 3), (-6, 0, 1), (-2, 0, 0)],
        skirt_r=[(0, 0, 0), (14, 0, 0), (-10, 0, -2), (-14, 0, -3), (-14, 0, -3), (-6, 0, -1), (-2, 0, 0)])
    seq(a, "transform", "pos", T, body=[(0, 0, 0), (0, -2.6, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 3, 0), (0, 8, 0), (0, 10, 0), (0, 9, 0), (0, 8, 0)])
    seq(a, "transform", "scale", T, spark=[(0.62,) * 3, (0.9,) * 3, (1.8,) * 3, (4.0,) * 3, (5.5,) * 3, (3.0,) * 3, (0.8,) * 3],
        fur=[(1, 1, 1), (1, 0.96, 1), (1.04, 1.1, 1.05), (1.08, 1.18, 1.1), (1.08, 1.18, 1.1), (1.03, 1.06, 1.03), (1, 1, 1)])
    spin(a, "transform", "spark", 4, 1)

    # ------------------------------------------------------------------------------------------------------- death (one-shot 2.8 s, holds): stagger, kneel, topple onto the side
    L = 2.8
    a.clip("death", L, False)
    T = (0.0, 0.35, 0.95, 1.5, 2.2, 2.8)
    seq(a, "death", "rot", T,
        arm_l=[(20, 0, 9), (-30, 0, 16), (30, 0, 12), (38, 0, 10), (-8, 0, 30), (-8, 0, 30)],
        arm_r=[(8, 0, -9), (-30, 0, -16), (30, 0, -12), (38, 0, -10), (-8, 0, -30), (-8, 0, -30)],
        forearm_l=[(134, 0, 0), (50, 0, 0), (20, 0, 0), (10, 0, 0), (6, 0, 0), (6, 0, 0)],
        forearm_r=[(30, 0, 0), (50, 0, 0), (20, 0, 0), (10, 0, 0), (6, 0, 0), (6, 0, 0)],
        body=[(0, 0, 0), (14, 0, 4), (-8, 0, 0), (-22, 0, 0), (-4, 0, 0), (-4, 0, 0)],
        head=[(1.5, 0, 0), (20, 0, 0), (-8, 0, 0), (-34, 0, 4), (-6, 0, 10), (-6, 0, 10)],
        leg_l=[(0, 0, 0), (-10, 0, 0), (62, 0, 5), (80, 0, 6), (6, 0, 0), (6, 0, 0)],
        leg_r=[(0, 0, 0), (10, 0, 0), (-70, 0, -5), (-74, 0, -6), (6, 0, 0), (6, 0, 0)],
        skirt_l=[(0, 0, 0), (-8, 0, 0), (40, 0, 8), (50, 0, 10), (6, 0, 4), (6, 0, 4)],
        skirt_r=[(0, 0, 0), (8, 0, 0), (-40, 0, -8), (-50, 0, -10), (6, 0, -4), (6, 0, -4)],
        cloak_top=[(-2, 0, 0), (-14, 0, 0), (-4, 0, 0), (-10, 0, 0), (18, 0, 0), (18, 0, 0)],
        cloak_bottom=[(-2, 0, 0), (-26, 0, 0), (6, 0, 0), (-4, 0, 0), (26, 0, 0), (26, 0, 0)],
        fur=[(0, 0, 0), (-8, 0, 0), (2, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)],
        hair=[(0, 0, 0), (-16, 0, 0), (6, 0, 0), (22, 0, 0), (8, 0, 0), (8, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 84), (0, 0, 84)])
    seq(a, "death", "pos", T, body=[(0, 0, 0), (0, 0, 2.0), (0, -6.2, 0), (0, -7.4, 0), (0, -9.5, 0), (0, -9.5, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-3, 1, 0), (-3, 3, 0)])
    seq(a, "death", "scale", T, spark=[(0.62,) * 3, (0.3,) * 3, (TINY,) * 3, (TINY,) * 3, (TINY,) * 3, (TINY,) * 3])


# ------------------------------------------------------------------------------------------------------------------ animation of the divine form
WING_REST = {1: (0.0, 0.0), 2: (0.0, 0.0), 3: (0.0, 0.0)}            # the model is built with the wings fully spread; clips fold them (negative z folds the left wing)
WING_IDLE = {1: (-7.0, -14.0), 2: (-9.0, -16.0), 3: (-6.0, -13.0)}
WING_CAST = {1: (2.0, -2.0), 2: (-3.0, -6.0), 3: (-2.0, -6.0)}
WING_BIG = {1: (5.0, 2.0), 2: (3.0, 2.0), 3: (4.0, 1.0)}
WING_FOLD = {1: (-92.0, -26.0), 2: (-96.0, -22.0), 3: (-88.0, -18.0)}
WING_RUN = {1: (-34.0, -12.0), 2: (-40.0, -12.0), 3: (-30.0, -10.0)}
SKIRTS = ("f", "b", "l", "r")


def wings_cyc(a, clip, L, pose, amp=4.0, rep=1, lag=0.55):
    """The six wings in a looping clip: the pose (inner, outer) angles of the left wings (the right ones are mirrored) plus a flap of amp degrees."""
    for n in (1, 2, 3):
        for sd, sg in (("_l", 1), ("_r", -1)):
            inner, tip = pose[n]
            ph = n * 0.5
            cyc(a, clip, "wing%d%s" % (n, sd), "rot", L, lambda t, sg=sg, ph=ph, inner=inner: (0, 0, sg * (inner + amp * (SIN(t + ph) - SIN(ph)))), rep)
            cyc(a, clip, "wing%d_tip%s" % (n, sd), "rot", L, lambda t, sg=sg, ph=ph, tip=tip: (0, 0, sg * (tip + amp * 0.7 * (SIN(t + ph - lag) - SIN(ph - lag)))), rep)


def wings_keys(a, clip, T, poses):
    """The six wings in a one-shot clip: poses[k] = {n: (inner, outer)} at time T[k]."""
    for n in (1, 2, 3):
        for sd, sg in (("_l", 1), ("_r", -1)):
            keys(a, clip, "wing%d%s" % (n, sd), "rot", [(t, (0, 0, sg * poses[k][n][0])) for k, t in enumerate(T)])
            keys(a, clip, "wing%d_tip%s" % (n, sd), "rot", [(t, (0, 0, sg * poses[k][n][1])) for k, t in enumerate(T)])


def halo_spin(a, clip, mult=1):
    """The halo and its inner ring turn against each other: whole 15 degree steps (the lattice repeats every 15 degrees), so every clip loops seamlessly."""
    L = a.clips[clip]["animation_length"]
    ang = 15.0 * max(1, round(L / 3.0)) * mult
    a.rot(clip, "halo", {0.0: (0, 0, 0), L: (0, 0, ang)})
    a.rot(clip, "halo_in", {0.0: (0, 0, 0), L: (0, 0, -ang * 1.0)})


def skirts_cyc(a, clip, L, amp=1.0, base=0.0, rep=1):
    for k, nm in enumerate(SKIRTS):
        sg = {"f": (1, 0), "b": (-1, 0), "l": (0, 1), "r": (0, -1)}[nm]
        for lvl, sfx in ((1.0, ""), (1.8, "2")):
            cyc(a, clip, "skirt_%s%s" % (nm, sfx), "rot", L,
                lambda t, sg=sg, lvl=lvl, k=k: (sg[0] * (base * lvl + amp * lvl * 3.0 * SIN(t + 0.7 * k + 0.5 * lvl)), 0, sg[1] * (base * lvl + amp * lvl * 3.0 * SIN(t + 0.7 * k + 0.5 * lvl))), rep)


def divine_clips(a):
    S, C = SIN, COS
    # ------------------------------------------------------------------------------------------------------- idle (loop 6 s): hovering, the robe drifting, the wings slowly breathing
    L = 6.0
    a.clip("idle", L, True)
    cyc(a, "idle", "root", "pos", L, lambda t: (0, 1.5 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((-1, 0, 0), (1.2, 1.6, 0.8), 0.5))
    cyc(a, "idle", "head", "rot", L, swing((2, 0, 0), (2.5, 5.0, 1.5), 0.9))
    cyc(a, "idle", "hair", "rot", L, swing((-5, 0, 0), (3.0, 0, 2.0), 1.3))
    cyc(a, "idle", "fur", "rot", L, swing((0, 0, 0), (1.0, 0, 1.0), 0.4))
    cyc(a, "idle", "cloak_top", "rot", L, swing((-9, 0, 0), (2.0, 0, 1.0), 0.6))
    cyc(a, "idle", "cloak_bottom", "rot", L, swing((-14, 0, 0), (4.0, 0, 2.0), 1.2))
    cyc(a, "idle", "arm_r", "rot", L, swing((58, -14, -28), (3.0, 2.0, 2.0), 0.4))
    cyc(a, "idle", "forearm_r", "rot", L, swing((22, 0, 0), (6.0, 0, 0), 1.0))
    cyc(a, "idle", "arm_l", "rot", L, swing((38, 0, 14), (3.0, 0, 2.0), 1.8))
    cyc(a, "idle", "forearm_l", "rot", L, swing((88, 0, 0), (5.0, 0, 0), 2.6))
    cyc(a, "idle", "spark", "scale", L, lambda t: (0.5 + 0.1 * S(2 * t),) * 3, 1)
    cyc(a, "idle", "leg_l", "rot", L, swing((6, 0, 2), (2.5, 0, 1.0), 0.3))
    cyc(a, "idle", "leg_r", "rot", L, swing((9, 0, -2), (2.5, 0, 1.0), 1.5))
    cyc(a, "idle", "foot_l", "rot", L, swing((0, 0, 0), (4.0, 0, 0), 0.9))
    cyc(a, "idle", "foot_r", "rot", L, swing((0, 0, 0), (4.0, 0, 0), 2.2))
    skirts_cyc(a, "idle", L, 1.0, 0.0)
    wings_cyc(a, "idle", L, WING_IDLE, 4.0)
    halo_spin(a, "idle")
    cyc(a, "idle", "antlers", "scale", L, lambda t: (1, 1 + 0.012 * S(2 * t), 1))

    # ------------------------------------------------------------------------------------------------------- walk (loop 1.4 s): a glide, the legs trailing
    L = 1.4
    a.clip("walk", L, True)
    cyc(a, "walk", "root", "pos", L, lambda t: (0, 1.2 + 0.8 * S(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (-5 + 1.0 * S(2 * t + 0.4), 2.0 * S(t), 0.8 * S(t)))
    cyc(a, "walk", "head", "rot", L, lambda t: (4 + 1.0 * S(2 * t + 1.0), -2.0 * S(t), 0))
    cyc(a, "walk", "hair", "rot", L, lambda t: (-12 + 3.0 * S(2 * t + 0.4), 0, 1.4 * S(t)))
    cyc(a, "walk", "fur", "rot", L, lambda t: (-3 + 1.0 * S(2 * t + 0.8), 0, 0.8 * S(t)))
    cyc(a, "walk", "cloak_top", "rot", L, lambda t: (-22 + 3.0 * S(2 * t + 0.5), 0, 1.5 * S(t)))
    cyc(a, "walk", "cloak_bottom", "rot", L, lambda t: (-32 + 5.0 * S(2 * t + 1.1), 0, 2.5 * S(t + 0.4)))
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "walk", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (12 + 9 * S(t + ph), 0, 2 * sg))
        cyc(a, "walk", "foot" + sd, "rot", L, lambda t, ph=ph: (3 * S(t + ph + 0.8), 0, 0))
        cyc(a, "walk", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (14 - 16 * S(t + ph), 0, 12 * sg))
        cyc(a, "walk", "forearm" + sd, "rot", L, lambda t, ph=ph: (28 + 8 * S(t + ph + 1.0), 0, 0))
    hold(a, "walk", "spark", "scale", (TINY,) * 3)
    skirts_cyc(a, "walk", L, 1.0, 5.0)
    wings_cyc(a, "walk", L, {1: (-14.0, -14.0), 2: (-20.0, -16.0), 3: (-14.0, -12.0)}, 4.0)
    halo_spin(a, "walk")

    # ------------------------------------------------------------------------------------------------------- run (loop 0.8 s): flying low, leaning, the wings swept back
    L = 0.8
    a.clip("run", L, True)
    cyc(a, "run", "root", "pos", L, lambda t: (0, 2.0 + 1.0 * S(2 * t), 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-18 + 1.5 * S(2 * t + 0.4), 3.0 * S(t), 1.0 * S(t)))
    cyc(a, "run", "head", "rot", L, lambda t: (14 + 1.5 * S(2 * t + 1.0), -3.0 * S(t), 0))
    cyc(a, "run", "hair", "rot", L, lambda t: (-22 + 4.0 * S(2 * t + 0.4), 0, 2.0 * S(t)))
    cyc(a, "run", "fur", "rot", L, lambda t: (-6 + 2.0 * S(2 * t + 0.8), 0, 1.0 * S(t)))
    cyc(a, "run", "cloak_top", "rot", L, lambda t: (-42 + 4.0 * S(2 * t + 0.5), 0, 2.0 * S(t)))
    cyc(a, "run", "cloak_bottom", "rot", L, lambda t: (-56 + 7.0 * S(2 * t + 1.1), 0, 3.0 * S(t + 0.4)))
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "run", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (26 + 12 * S(t + ph), 0, 3 * sg))
        cyc(a, "run", "foot" + sd, "rot", L, lambda t, ph=ph: (4 * S(t + ph + 0.8), 0, 0))
        cyc(a, "run", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-36 - 14 * S(t + ph), 0, 10 * sg))
        cyc(a, "run", "forearm" + sd, "rot", L, lambda t, ph=ph: (28 + 10 * S(t + ph + 1.0), 0, 0))
    hold(a, "run", "spark", "scale", (TINY,) * 3)
    skirts_cyc(a, "run", L, 1.0, 14.0)
    wings_cyc(a, "run", L, WING_RUN, 9.0, 1, 0.8)
    halo_spin(a, "run", 2)

    # ------------------------------------------------------------------------------------------------------- attack (one-shot 0.9 s): the wings snap forward, a palm strike
    L = 0.9
    a.clip("attack", L, False)
    T = (0.0, 0.26, 0.4, 0.52, 0.9)
    seq(a, "attack", "rot", T,
        arm_r=[(58, -14, -28), (-40, -14, -34), (104, 4, -8), (84, 2, -10), (58, -14, -28)],
        forearm_r=[(22, 0, 0), (70, 0, 0), (6, 0, 0), (14, 0, 0), (22, 0, 0)],
        arm_l=[(38, 0, 14), (-26, 0, 20), (22, 0, 18), (30, 0, 16), (38, 0, 14)],
        forearm_l=[(88, 0, 0), (50, 0, 0), (36, 0, 0), (60, 0, 0), (88, 0, 0)],
        body=[(-1, 0, 0), (8, 14, 0), (-18, -14, 0), (-10, -6, 0), (-1, 0, 0)],
        head=[(2, 0, 0), (4, 10, 0), (-6, -8, 0), (-3, -3, 0), (2, 0, 0)],
        hair=[(-5, 0, 0), (-10, 0, 0), (12, 0, 0), (2, 0, 0), (-5, 0, 0)],
        cloak_top=[(-9, 0, 0), (-20, 0, 0), (8, 0, 0), (-8, 0, 0), (-9, 0, 0)],
        cloak_bottom=[(-14, 0, 0), (-30, 0, 0), (12, 0, 0), (-10, 0, 0), (-14, 0, 0)])
    seq(a, "attack", "pos", T, root=[(0, 0, 0), (0, 1.0, 1.4), (0, 0.6, -3.0), (0, 0.2, -1.4), (0, 0, 0)])
    wings_keys(a, "attack", T, [WING_IDLE, {1: (-24.0, -28.0), 2: (-30.0, -30.0), 3: (-24.0, -26.0)}, {1: (16.0, 12.0), 2: (12.0, 10.0), 3: (12.0, 8.0)},
                                {1: (4.0, 0.0), 2: (0.0, -4.0), 3: (0.0, -4.0)}, WING_IDLE])
    halo_spin(a, "attack", 2)

    # ------------------------------------------------------------------------------------------------------- cast (loop 1.4 s): both arms raised, the golden spark swelling
    L = 1.4
    a.clip("cast", L, True)
    cyc(a, "cast", "arm_l", "rot", L, lambda t: (110 + 4 * S(t), -6, 12 + 2 * S(t + 1)))
    cyc(a, "cast", "forearm_l", "rot", L, lambda t: (20 + 8 * S(t + 0.6), 0, 0))
    cyc(a, "cast", "arm_r", "rot", L, lambda t: (100 + 4 * S(t + 1.4), 6, -16 - 2 * S(t)))
    cyc(a, "cast", "forearm_r", "rot", L, lambda t: (24 + 8 * S(t + 2.0), 0, 0))
    cyc(a, "cast", "spark", "scale", L, lambda t: (1.6 + 0.3 * S(t),) * 3)
    cyc(a, "cast", "root", "pos", L, lambda t: (0, 2.4 + 1.2 * S(t), 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (-3 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (-8 + 1.5 * S(t + 0.4), 2.0 * S(t), 0))
    cyc(a, "cast", "hair", "rot", L, lambda t: (-14 + 3.0 * S(t + 0.6), 0, 2.0 * S(t)))
    cyc(a, "cast", "fur", "rot", L, lambda t: (-3 + 1.2 * S(t + 0.8), 0, 1.0 * S(t)))
    cyc(a, "cast", "cloak_top", "rot", L, lambda t: (-15 + 3.0 * S(t + 1.0), 0, 1.5 * S(t)))
    cyc(a, "cast", "cloak_bottom", "rot", L, lambda t: (-22 + 5.0 * S(t + 1.6), 0, 2.5 * S(t + 0.5)))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "cast", "leg" + sd, "rot", L, lambda t, sg=sg: (7 + 2 * S(t), 0, 3 * sg))
    skirts_cyc(a, "cast", L, 1.2, 4.0)
    wings_cyc(a, "cast", L, WING_CAST, 5.0)
    halo_spin(a, "cast", 2)

    # ------------------------------------------------------------------------------------------------------- cast_big (loop 2.4 s): arms thrown wide, the wings fully spread, the halo racing
    L = 2.4
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "arm_l", "rot", L, lambda t: (-10 + 3 * S(t), 0, 80 + 4 * S(t + 0.4)))
    cyc(a, "cast_big", "arm_r", "rot", L, lambda t: (-10 + 3 * S(t + 1), 0, -80 - 4 * S(t + 1.4)))
    cyc(a, "cast_big", "forearm_l", "rot", L, lambda t: (14 + 5 * S(t + 0.6), 0, 0))
    cyc(a, "cast_big", "forearm_r", "rot", L, lambda t: (14 + 5 * S(t + 1.6), 0, 0))
    cyc(a, "cast_big", "spark", "scale", L, lambda t: (3.4 + 0.6 * S(2 * t),) * 3)
    cyc(a, "cast_big", "root", "pos", L, lambda t: (0, 5.0 + 1.2 * S(t), 0))
    cyc(a, "cast_big", "body", "rot", L, lambda t: (-6 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast_big", "head", "rot", L, lambda t: (-18 + 2.0 * S(t + 0.4), 0, 0))
    cyc(a, "cast_big", "hair", "rot", L, lambda t: (-26 + 5.0 * S(t + 0.6), 0, 3.0 * S(t)))
    cyc(a, "cast_big", "fur", "rot", L, lambda t: (-8 + 2 * S(t), 0, 1.4 * S(t + 1)))
    cyc(a, "cast_big", "fur", "scale", L, lambda t: (1.04, 1.1 + 0.02 * S(t), 1.05))
    cyc(a, "cast_big", "cloak_top", "rot", L, lambda t: (-28 + 4 * S(t + 1), 0, 2 * S(t)))
    cyc(a, "cast_big", "cloak_bottom", "rot", L, lambda t: (-40 + 6 * S(t + 1.6), 0, 4 * S(t + 0.5)))
    for sd, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "cast_big", "leg" + sd, "rot", L, lambda t, sg=sg: (-6, 0, 5 * sg))
        cyc(a, "cast_big", "foot" + sd, "rot", L, lambda t: (-6 + 3 * S(t), 0, 0))
    skirts_cyc(a, "cast_big", L, 1.4, 12.0)
    wings_cyc(a, "cast_big", L, WING_BIG, 6.0)
    halo_spin(a, "cast_big", 3)
    cyc(a, "cast_big", "halo", "scale", L, lambda t: (1.08 + 0.04 * S(2 * t),) * 3, 1)
    cyc(a, "cast_big", "halo_in", "scale", L, lambda t: (1.08 + 0.04 * S(2 * t),) * 3, 1)
    cyc(a, "cast_big", "antlers", "scale", L, lambda t: (1.05, 1.1 + 0.03 * S(2 * t), 1.05))

    # ------------------------------------------------------------------------------------------------------- hurt (one-shot 0.45 s)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    seq(a, "hurt", "rot", T, body=[(-1, 0, 0), (10, 0, 3), (-1, 0, 0)], head=[(2, 0, 0), (14, 6, 0), (2, 0, 0)],
        arm_l=[(38, 0, 14), (0, 0, 28), (38, 0, 14)], arm_r=[(58, -14, -28), (10, -6, -34), (58, -14, -28)],
        forearm_l=[(88, 0, 0), (40, 0, 0), (88, 0, 0)], forearm_r=[(22, 0, 0), (40, 0, 0), (22, 0, 0)],
        cloak_top=[(-9, 0, 0), (-22, 0, 0), (-9, 0, 0)], cloak_bottom=[(-14, 0, 0), (-32, 0, 0), (-14, 0, 0)],
        hair=[(-5, 0, 0), (-18, 0, 0), (-5, 0, 0)])
    seq(a, "hurt", "pos", T, root=[(0, 0, 0), (0, 0.4, 2.0), (0, 0, 0)])
    wings_keys(a, "hurt", T, [WING_IDLE, {1: (6.0, 4.0), 2: (4.0, 2.0), 3: (4.0, 2.0)}, WING_IDLE])
    seq(a, "hurt", "scale", T, spark=[(0.5,) * 3, (0.15,) * 3, (0.5,) * 3])

    # ------------------------------------------------------------------------------------------------------- transform (one-shot 3.6 s): arrives with the arms wide and the wings folded; the halo, the antlers and the wings unfold
    L = 3.6
    a.clip("transform", L, False)
    T = (0.0, 0.5, 1.1, 1.7, 2.3, 2.9, 3.6)
    seq(a, "transform", "rot", T,
        arm_l=[(14, 0, 44), (12, 0, 70), (10, 0, 78), (-6, 0, 84), (-8, 0, 80), (20, 0, 40), (38, 0, 14)],
        arm_r=[(14, 0, -44), (12, 0, -70), (10, 0, -78), (-6, 0, -84), (-8, 0, -80), (20, 0, -40), (58, -14, -28)],
        forearm_l=[(16, 0, 0), (14, 0, 0), (14, 0, 0), (14, 0, 0), (14, 0, 0), (40, 0, 0), (88, 0, 0)],
        forearm_r=[(16, 0, 0), (14, 0, 0), (14, 0, 0), (14, 0, 0), (14, 0, 0), (30, 0, 0), (22, 0, 0)],
        body=[(0, 0, 0), (-4, 0, 0), (-8, 0, 0), (-10, 0, 0), (-6, 0, 0), (-3, 0, 0), (-1, 0, 0)],
        head=[(-2, 0, 0), (-12, 0, 0), (-20, 0, 0), (-22, 0, 0), (-12, 0, 0), (0, 0, 0), (2, 0, 0)],
        hair=[(0, 0, 0), (-14, 0, 0), (-28, 0, 4), (-32, 0, -4), (-24, 0, 3), (-10, 0, 0), (-5, 0, 0)],
        fur=[(0, 0, 0), (-4, 0, 0), (-9, 0, 0), (-12, 0, 0), (-8, 0, 0), (-2, 0, 0), (0, 0, 0)],
        cloak_top=[(-4, 0, 0), (-14, 0, 0), (-28, 0, 0), (-34, 0, 0), (-22, 0, 0), (-12, 0, 0), (-9, 0, 0)],
        cloak_bottom=[(-6, 0, 0), (-20, 0, 0), (-40, 0, 0), (-46, 0, 0), (-30, 0, 0), (-18, 0, 0), (-14, 0, 0)],
        leg_l=[(-2, 0, 0), (-4, 0, 3), (-6, 0, 5), (-6, 0, 5), (2, 0, 3), (6, 0, 2), (6, 0, 2)],
        leg_r=[(-2, 0, 0), (-4, 0, -3), (-6, 0, -5), (-6, 0, -5), (2, 0, -3), (8, 0, -2), (9, 0, -2)])
    seq(a, "transform", "pos", T, root=[(0, 8, 0), (0, 8, 0), (0, 7, 0), (0, 6, 0), (0, 4, 0), (0, 2, 0), (0, 0, 0)])
    seq(a, "transform", "scale", T,
        halo=[(TINY,) * 3, (TINY,) * 3, (0.3,) * 3, (1.25,) * 3, (1.1,) * 3, (1.0,) * 3, (1.0,) * 3],
        halo_in=[(TINY,) * 3, (TINY,) * 3, (0.2,) * 3, (1.2,) * 3, (1.1,) * 3, (1.0,) * 3, (1.0,) * 3],
        antlers=[(TINY,) * 3, (TINY,) * 3, (0.15, 0.15, 0.15), (1.0, 1.25, 1.0), (1.0, 1.1, 1.0), (1, 1, 1), (1, 1, 1)],
        spark=[(1.0,) * 3, (2.2,) * 3, (4.0,) * 3, (3.0,) * 3, (1.2,) * 3, (0.7,) * 3, (0.5,) * 3],
        fur=[(1, 1, 1), (1, 0.98, 1), (1.05, 1.1, 1.06), (1.08, 1.16, 1.08), (1.04, 1.08, 1.04), (1.0, 1.02, 1.0), (1, 1, 1)])
    wings_keys(a, "transform", T, [WING_FOLD, WING_FOLD, {1: (-70.0, -14.0), 2: (-72.0, -12.0), 3: (-66.0, -10.0)},
                                   {1: (10.0, 6.0), 2: (8.0, 6.0), 3: (8.0, 4.0)}, {1: (-2.0, -4.0), 2: (0.0, -4.0), 3: (-2.0, -4.0)},
                                   {1: (-6.0, -10.0), 2: (-8.0, -14.0), 3: (-6.0, -12.0)}, WING_IDLE])
    a.rot("transform", "halo", {0.0: (0, 0, 0), L: (0, 0, 120)})
    a.rot("transform", "halo_in", {0.0: (0, 0, 0), L: (0, 0, -120)})

    # ------------------------------------------------------------------------------------------------------- death (one-shot 3.2 s, holds): the wings fold, the halo and antlers fade, he sinks and tips back
    L = 3.2
    a.clip("death", L, False)
    T = (0.0, 0.4, 1.2, 2.0, 2.7, 3.2)
    seq(a, "death", "rot", T,
        arm_l=[(38, 0, 14), (-20, 0, 40), (10, 0, 50), (6, 0, 36), (4, 0, 30), (4, 0, 30)],
        arm_r=[(58, -14, -28), (-20, 0, -40), (10, 0, -50), (6, 0, -36), (4, 0, -30), (4, 0, -30)],
        forearm_l=[(88, 0, 0), (40, 0, 0), (20, 0, 0), (8, 0, 0), (6, 0, 0), (6, 0, 0)],
        forearm_r=[(22, 0, 0), (40, 0, 0), (20, 0, 0), (8, 0, 0), (6, 0, 0), (6, 0, 0)],
        body=[(-1, 0, 0), (14, 0, 3), (-14, 0, 0), (-30, 0, 0), (-40, 0, 0), (-40, 0, 0)],
        head=[(2, 0, 0), (20, 0, 0), (-14, 0, 0), (-30, 0, 4), (-26, 0, 8), (-26, 0, 8)],
        hair=[(-5, 0, 0), (-18, 0, 0), (6, 0, 0), (20, 0, 0), (16, 0, 0), (16, 0, 0)],
        fur=[(0, 0, 0), (-8, 0, 0), (2, 0, 0), (6, 0, 0), (0, 0, 0), (0, 0, 0)],
        cloak_top=[(-9, 0, 0), (-24, 0, 0), (-6, 0, 0), (-2, 0, 0), (6, 0, 0), (6, 0, 0)],
        cloak_bottom=[(-14, 0, 0), (-30, 0, 0), (-8, 0, 0), (-2, 0, 0), (10, 0, 0), (10, 0, 0)],
        leg_l=[(6, 0, 2), (-8, 0, 3), (14, 0, 3), (24, 0, 4), (30, 0, 4), (30, 0, 4)],
        leg_r=[(9, 0, -2), (-8, 0, -3), (14, 0, -3), (20, 0, -4), (26, 0, -4), (26, 0, -4)])
    seq(a, "death", "pos", T, root=[(0, 0, 0), (0, 2.0, 1.5), (0, 4.0, 0), (0, 2.0, 0), (0, -0.5, 4), (0, -0.5, 4)])
    seq(a, "death", "scale", T,
        halo=[(1, 1, 1), (1.1, 1.1, 1.1), (0.8, 0.8, 0.8), (0.4,) * 3, (TINY,) * 3, (TINY,) * 3],
        halo_in=[(1, 1, 1), (1.1, 1.1, 1.1), (0.8, 0.8, 0.8), (0.4,) * 3, (TINY,) * 3, (TINY,) * 3],
        antlers=[(1, 1, 1), (1, 1.1, 1), (1, 0.8, 1), (0.6, 0.5, 0.6), (TINY,) * 3, (TINY,) * 3],
        spark=[(0.5,) * 3, (0.2,) * 3, (TINY,) * 3, (TINY,) * 3, (TINY,) * 3, (TINY,) * 3])
    wings_keys(a, "death", T, [WING_IDLE, {1: (10.0, 6.0), 2: (6.0, 4.0), 3: (6.0, 4.0)}, {1: (-40.0, -20.0), 2: (-50.0, -20.0), 3: (-44.0, -16.0)},
                               WING_FOLD, WING_FOLD, WING_FOLD])
    a.rot("death", "halo", {0.0: (0, 0, 0), L: (0, 0, 30)})
    a.rot("death", "halo_in", {0.0: (0, 0, 0), L: (0, 0, -30)})


def run_build(b, tex_h=None):
    img, glow = b.finish()
    return b.save(img, glow)


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist, fov=None):
    v = {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}
    if fov:
        v["fov"] = fov
    return v


def scene(model, anim, ages, views, tile=(440, 620), glow=True, floor=True, expect=()):
    if model.endswith("lucius_divine"):                                         # 9.5k vertices a frame (two passes): the harness's note for aura painters, fine for a boss
        expect = tuple(expect) + ("BUDGET",)
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF" if glow else "0",
            "ages": list(ages), "tile": list(tile), "floor": floor, "figure": False, "expect": list(expect), "anim": anim, "views": views}


def std_views(h, dist=None, yaws=(0, 35, 180, 90), pitch=6, ty=None):
    ty = h * 0.5 if ty is None else ty
    d = dist or h * 2.15
    names = {0: "front", 35: "front 3/4", 180: "back", 90: "side", -35: "front 3/4 left", 145: "back 3/4"}
    return [view(names.get(y, "yaw %d" % y), y, pitch, (0, ty, 0), d) for y in yaws]


MODELS = ("lucius_base", "lucius_divine", "soul_paladin", "grimoire_prop")
CLIPS = ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death")
TOWER = {"lucius_base": 2.15, "lucius_divine": 4.0, "soul_paladin": 2.3, "grimoire_prop": 1.4}          # block height that frames the whole model
LOOK = {"lucius_base": 1.05, "lucius_divine": 1.7, "soul_paladin": 1.1, "grimoire_prop": 0.65}


def tick(sec):
    return round(sec * 20.0, 1)


def scenes_for(models=MODELS):
    """The preview scenes (tools/render_preview): every model from four sides in its idle, and a strip of frames of the clips that matter."""
    sc = {}
    for m in models:
        h, ty = TOWER[m], LOOK[m]
        d = h * (2.0 if m != "lucius_divine" else 1.9)
        tile = (400, 560) if m != "grimoire_prop" else (420, 420)
        full = "zogratis_" + m
        sc[m + "_front"] = scene(full, "idle", [0], [view("front", 0, 4, (0, ty, 0), d), view("front 3/4", 35, 8, (0, ty, 0), d)], tile)
        sc[m + "_back"] = scene(full, "idle", [0], [view("back", 180, 5, (0, ty, 0), d), view("side", 90, 4, (0, ty, 0), d)], tile)
        if m in ("lucius_base", "lucius_divine"):
            fy = {"lucius_base": 1.75, "lucius_divine": 2.1}[m]
            sc[m + "_face"] = scene(full, "idle", [0], [view("face", 0, 2, (0, fy, 0), 2.1), view("face 3/4", 32, 6, (0, fy, 0), 2.1)], (420, 420))
        for clip, secs, n in (("cast", 1.2, 4), ("cast_big", 2.0, 4), ("attack", 0.9, 5), ("transform", 3.2 if m != "lucius_divine" else 3.6, 6), ("death", 2.8, 5), ("walk", 1.1, 4)):
            ages = [tick(secs * k / n) for k in range(n)]
            wide = 1.35 if clip in ("cast_big", "transform") else 1.05
            exp = ("ZERO_AREA",) if (m, clip) == ("grimoire_prop", "transform") else ()               # its first frame is the book at a hundredth of its size
            vs = [view("3/4", 30, 6, (0, ty + (0.25 if wide > 1.2 else 0.0), 0), d * wide)]
            if clip == "cast_big" and m in ("lucius_base", "lucius_divine"):
                vs.append(view("front", 0, 4, (0, ty + 0.25, 0), d * wide))
            sc["%s_%s" % (m, clip)] = scene(full, clip, ages, vs, (340, 480) if m != "grimoire_prop" else (340, 340), expect=exp)
    return sc


def write_scenes(path=SCENES_OUT):
    sc = scenes_for()
    with open(path, "w") as f:
        json.dump(sc, f, indent=1)
    return sc


# ------------------------------------------------------------------------------------------------------------------ checks and the manifest
def load_json(path):
    with open(path) as f:
        return json.load(f)


def asset_paths(name):
    i = "zogratis_" + name
    return {"geo": "src/main/resources/assets/nusmp/geo/entity/%s.geo.json" % i, "animations": "src/main/resources/assets/nusmp/animations/entity/%s.animation.json" % i,
            "texture": "src/main/resources/assets/nusmp/textures/entity/%s.png" % i, "glow": "src/main/resources/assets/nusmp/textures/entity/%s_glow.png" % i}


def inspect(name):
    """Reads the files of a model back and checks them: every clip's bones exist, every clip name is there, every cube has uv inside the texture,
    parents exist. Returns the facts for the manifest (bones, clips, bounds, bones with glow pixels)."""
    from PIL import Image
    paths = {k: os.path.join(REPO, v) for k, v in asset_paths(name).items()}
    geo = load_json(paths["geo"])["minecraft:geometry"][0]
    anim = load_json(paths["animations"])["animations"]
    img = Image.open(paths["texture"])
    glow = Image.open(paths["glow"])
    tw, th = geo["description"]["texture_width"], geo["description"]["texture_height"]
    assert img.size == (tw, th) == glow.size, (name, img.size, glow.size, tw, th)
    bones = {b["name"]: b for b in geo["bones"]}
    problems = []
    for b in geo["bones"]:
        if b.get("parent") and b["parent"] not in bones:
            problems.append("bone %s: parent %s missing" % (b["name"], b["parent"]))
    for cn in CLIPS:
        if cn not in anim:
            problems.append("clip %s missing" % cn)
    for cn, c in anim.items():
        for bn in c["bones"]:
            if bn not in bones:
                problems.append("clip %s: bone %s not in the model" % (cn, bn))
        if c["animation_length"] <= 0:
            problems.append("clip %s: no length" % cn)
    ga = np.asarray(glow.convert("RGBA"))[..., 3]
    ca = np.asarray(img.convert("RGBA"))[..., 3]
    glow_bones, lo, hi, n_cubes, n_faces, head_top = [], [1e9] * 3, [-1e9] * 3, 0, 0, {}
    for b in geo["bones"]:
        has = False
        for c in b["cubes"]:
            n_cubes += 1
            for fk, f in c["uv"].items():
                n_faces += 1
                u, v = f["uv"]
                w, h = f["uv_size"]
                if u < 0 or v < 0 or u + w > tw or v + h > th:
                    problems.append("%s: a %s face of a cube of %s is outside the texture" % (name, fk, b["name"]))
                    continue
                if ga[int(v):int(v + h), int(u):int(u + w)].any():
                    has = True
                if not ca[int(v):int(v + h), int(u):int(u + w)].any():
                    problems.append("%s: a %s face of a cube of %s is fully transparent" % (name, fk, b["name"]))
            infl = c.get("inflate", 0.0)
            for k in range(8):
                x = c["origin"][0] - infl + ((c["size"][0] + 2 * infl) if k & 1 else 0)
                y = c["origin"][1] - infl + ((c["size"][1] + 2 * infl) if k & 2 else 0)
                z = c["origin"][2] - infl + ((c["size"][2] + 2 * infl) if k & 4 else 0)
                if c.get("rotation"):                                           # the cube's own rotation about its pivot (bone poses are not applied)
                    px_, py_, pz_ = c["pivot"]
                    dx, dy, dz = rot_file((x - px_, y - py_, z - pz_), *c["rotation"])
                    x, y, z = px_ + dx, py_ + dy, pz_ + dz
                lo = [min(lo[0], x), min(lo[1], y), min(lo[2], z)]
                hi = [max(hi[0], x), max(hi[1], y), max(hi[2], z)]
                if b["name"] in ("head", "hair"):
                    head_top[b["name"]] = max(head_top.get(b["name"], -1e9), y)
        if has:
            glow_bones.append(b["name"])
    return {"bones": [b["name"] for b in geo["bones"]], "clips": {k: {"length": c["animation_length"], "loop": c["loop"]} for k, c in anim.items()},
            "texture": [tw, th], "cubes": n_cubes, "faces": n_faces, "glow_bones": glow_bones, "bounds_px": [lo, hi], "head_top_px": head_top.get("head"),
            "hair_top_px": head_top.get("hair"), "problems": problems}


HITBOX = {"lucius_base": (0.7, 2.1), "lucius_divine": (0.8, 2.4), "soul_paladin": (0.7, 2.1), "grimoire_prop": (0.5, 1.0)}
NOTES = {
    "lucius_base": "Human form. Height 2.12 blocks to the crown of the head (34 px), about 2.3 with the hair; the visible width is about 2.4 blocks (the fur mantle and "
                   "the cloak) and the cloak reaches the ground. Pose of idle = the reference (left hand raised with the spark; the spark bone is scaled to nothing in walk / run / "
                   "death). Clip `transform` = the first half of the change to the divine form: he rises 8 px, the arms go wide, the spark swells to a sun; "
                   "swap to lucius_divine at the end (its own `transform` starts from the same pose, wings folded, halo and antlers scaled to nothing).",
    "lucius_divine": "True form. Toes at y = 1 px, the crown of the head at 2.44 blocks (39 px), the antlers and the upper wings to about 3.8 blocks, the halo diameter 2.1 "
                     "blocks, the wing span about 4.7 blocks. The model hovers only through its clips (idle bobs the root 1.5 px); the renderer may lift it further. Wings: wing<n>_l/_r "
                     "(inner half of the fan) + wing<n>_tip_l/_r (outer half), n = 1 upper, 2 middle, 3 lower; the model is built with the wings fully spread (that is "
                     "the rest pose, cast_big), clips fold them (a negative z rotation folds a left wing). halo spins in 15 degree steps (24-fold lattice), halo_in against it.",
    "soul_paladin": "Armoured white-gold knight, 2.1 blocks to the helm (crest and plume to 2.5). Hold the `sword` bone's blade along the forearm axis (the idle pose is the sword "
                    "raised at attention); `shield` is a child of body that the clips move with the left arm. Faint soul glow: the visor, the sigils, the blade, the gold trim.",
    "grimoire_prop": "Open golden grimoire, 23 x 16 px, origin at the foot of its bounding column, the book centre at y = 10 px (0.62 blocks): put it where the renderer "
                     "wants it (in front of the chest, the pages facing the viewer). Bones: book (bob), page_l / page_r (the open V, hinge at the spine: rotate y), leaf (a "
                     "turning page), rays / rays2 (the starburst behind it, additive), runes (four orbiting glyphs). It carries all nine clip names so it can share the "
                     "boss's animation state.",
}


def write_manifest(info, path=MANIFEST_OUT):
    files = []
    for n in MODELS:
        files += list(asset_paths(n).values())
    files += [os.path.relpath(SCENES_OUT, REPO), "tools/gen_zogratis_lucius.py", "tools/zogratis_lucius_kit.py"]
    models = {}
    for n in MODELS:
        i = info[n]
        lo, hi = i["bounds_px"]
        paths = asset_paths(n)
        models[n] = {"spec": 'GeoSpec.of("zogratis", "%s")' % n, "files": list(paths.values()), "texture": i["texture"], "cubes": i["cubes"], "faces_per_pass": i["faces"],
                     "bones": i["bones"], "clips": i["clips"], "glow_bones": i["glow_bones"],
                     "bounds_blocks": {"min": [round(v / 16.0, 2) for v in lo], "max": [round(v / 16.0, 2) for v in hi]},
                     "head_top_blocks": None if i["head_top_px"] is None else round(i["head_top_px"] / 16.0, 2),
                     "hair_top_blocks": None if i["hair_top_px"] is None else round(i["hair_top_px"] / 16.0, 2),
                     "recommended_hitbox_blocks": {"width": HITBOX[n][0], "height": HITBOX[n][1]}, "notes": NOTES[n]}
    man = {"generator": "python3 -B tools/gen_zogratis_lucius.py", "preview_scenes": os.path.relpath(SCENES_OUT, REPO), "key": KEY, "files": files, "models": models,
           "renderer_hints": {
               "culling_aabb_blocks": "lucius_divine: at least 5 x 4 x 5 (wings span 4.7, antlers and upper wings reach 3.8 blocks); lucius_base 2.6 x 2.5 x 2.6; bounds_blocks of each model is the UNPOSED box",
               "grimoire_offset_blocks": {"lucius_divine": [0.0, 1.05, -0.85], "lucius_base": [0.0, 0.75, -0.8],
                                          "meaning": "translate the grimoire_prop model by this (x, y, z) in blocks from the boss's feet (the book centre is 0.62 blocks up in its own model), "
                                                     "front = -z of the boss; it only needs the clip of the boss's state (idle / cast / cast_big ...)"},
               "glow": "draw the *_glow texture additive with GeoDraw.paint(..., glowArgb): pulse the alpha, e.g. 0.75 + 0.25 sin(age * 0.2); the glow is already shaped (eyes, spark, halo, "
                       "antlers, feather tips, sigils), nothing else needs a tint",
               "spark": "bone `spark` is scaled to nothing outside idle / cast / cast_big / transform; put a particle emitter on the left hand when it is shown",
               "divine_swap": "play lucius_base `transform` (3.2 s), swap to lucius_divine at its end (same pose) and play lucius_divine `transform` (3.6 s), then idle",
               "wings": "wing<n>_l/_r and wing<n>_tip_l/_r (n = 1 upper, 2 middle, 3 lower); the model is built fully spread, the clips fold them; the six wings carry the glow on their tips",
               "halo": "halo + halo_in are 24-fold symmetric: any rotation about z by a multiple of 15 degrees repeats, so a renderer may spin them freely in 15 degree steps"},
           "clip_names": list(CLIPS), "frame": "pixels (1/16 block), y up, the model's front on -z, +x the model's left, origin at the feet; GeoDraw.Space.PROP (front +z in the world)",
           "problems": {n: info[n]["problems"] for n in MODELS if info[n]["problems"]}}
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as f:
        json.dump(man, f, indent=1)
    return man


def render_previews(names=None):
    """Runs tools/render_preview over the scenes of scenes.d/zogratis_lucius.json (build folder RENDER_PREVIEW_BUILD or <scratch>/rp_lucius) and copies the sheets
    to PREVIEW_OUT; prints the warnings of the harness (ZERO_AREA / NORMAL_MISMATCH / MISSING_TEXTURE are real, BUDGET is only a note for aura painters)."""
    import shutil
    import subprocess
    sc = scenes_for()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp_lucius"))
    os.makedirs(PREVIEW_OUT, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    out = [l for l in (r.stdout + r.stderr).splitlines() if "Picked up" not in l]
    for l in out:
        if "WARNING" in l or "ERROR" in l or "scenes:" in l:
            if "BUDGET" not in l:
                print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(PREVIEW_OUT, n + ".png"))
    return r.returncode


BUILDERS = {"lucius_base": build_base, "lucius_divine": build_divine, "soul_paladin": build_paladin, "grimoire_prop": build_grimoire}


def main(argv):
    which = [a for a in argv if not a.startswith("-")]
    out = {}
    for name, fn in BUILDERS.items():
        if which and name not in which:
            continue
        out[name] = run_build(fn())
    if not which:
        write_scenes()
        info = {n: inspect(n) for n in MODELS}
        bad = {n: i["problems"] for n, i in info.items() if i["problems"]}
        for n, pr in bad.items():
            for q in pr[:12]:
                print("PROBLEM", n, q)
        write_manifest(info)
        print("manifest ->", MANIFEST_OUT)
    return out


if __name__ == "__main__":
    if "--face" in sys.argv:
        os.makedirs(SCRATCH, exist_ok=True)
        for k in ("base", "divine"):
            face_art(k).image(10).save(os.path.join(SCRATCH, "face_%s.png" % k))
        sys.exit(0)
    if "--check" in sys.argv:
        bad = 0
        for n in MODELS:
            i = inspect(n)
            print("%-14s texture %s, %d cubes, %d faces/pass, %d bones, clips %s, glow bones %d" % (n, i["texture"], i["cubes"], i["faces"], len(i["bones"]), len(i["clips"]), len(i["glow_bones"])))
            for q in i["problems"]:
                print("  PROBLEM", q)
                bad += 1
        load_json(SCENES_OUT)
        sys.exit(1 if bad else 0)
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    main(sys.argv[1:])

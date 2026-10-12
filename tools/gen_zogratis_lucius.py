"""Lucius Zogratis, the Wizard King's other soul and the main antagonist of the Zogratis bosses, in the repo's GeoLite pipeline (tools/geo_builder.py ->
client.geo.GeoDraw): both of his forms, the Soul Paladins he makes of the mobs he takes, and his floating grimoire.

    python3 -B tools/gen_zogratis_lucius.py                    # everything (idempotent)
    python3 -B tools/gen_zogratis_lucius.py lucius_base        # only the named models

Models (GeoSpec.of("zogratis", "<name>")), original pixel-art interpretations of the manga designs (nothing is traced from the reference images):
    lucius_base     the human form, 2.0 blocks to the crown (2.15 with the hair): pale face, short spiky black hair, red eyes, a small sigil on the left
                    forehead, a faint smile, the huge white fur mantle over a tall dark collar and choker, a long black cloak with a dark fur lining, a
                    brown-maroon long robe with crossed dark straps over the chest; one hand raised with a spark (bone "spark")
    lucius_divine   the true form, 2.4 blocks from the toes to the crown, floating and barefoot: a pale-blue and white layered robe, the white fur mantle
                    and a crossed chain, THREE PAIRS of feathered wings (wing1_l/r upper, wing2_l/r middle, wing3_l/r lower, each in two segments with
                    feather plates), a golden gear and lattice halo behind the head, golden stag antlers, gold eyes; the grimoire floats beside it as its own model
    soul_paladin    an armoured white-gold knight with a faint soul glow (the mobs Lucius turns into immortal Paladins): great helm with a visor slit,
                    crest, pauldrons, tabard, cape, sword and shield (bones "sword" and "shield")
    grimoire_prop   Lucius's floating golden open grimoire: gold covers, pages, a radiant starburst, a turning page, orbiting runes

Writes for each <name> (through tools/geo_builder.py):
    assets/nusmp/geo/entity/zogratis_<name>.geo.json
    assets/nusmp/animations/entity/zogratis_<name>.animation.json
    assets/nusmp/textures/entity/zogratis_<name>.png          humanoids 512 wide at 2 texels per model pixel (the faces at 4), plates / props 256 wide
    assets/nusmp/textures/entity/zogratis_<name>_glow.png     eyes, spark, halo, antlers, wing tips, sigils, grimoire (additive, full bright)
and tools/render_preview/scenes.d/zogratis_lucius.json (python3 -B tools/render_preview/preview.py lucius_base_idle ...), and, when the scratchpad path
of the owner's session exists, the manifest and the preview PNGs (see MANIFEST_OUT / PREVIEW_OUT).

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
SKIN = ((176, 136, 142), (214, 176, 172), (238, 208, 198), (250, 230, 222))
SKIN_DIV = ((170, 160, 190), (206, 200, 222), (232, 228, 240), (250, 248, 255))
HAIR = ((8, 8, 12), (18, 18, 28), (36, 38, 58), (66, 72, 106))
FUR = ((116, 108, 124), (164, 156, 172), (204, 198, 206), (230, 226, 228), (246, 243, 240), (255, 253, 250))
FUR_DIV = ((120, 128, 160), (164, 176, 206), (204, 214, 232), (232, 238, 248), (250, 252, 255), (255, 255, 255))
ROBE = ((44, 26, 26), (66, 38, 34), (92, 56, 48), (118, 74, 62), (146, 96, 80))
ROBE_DARK = ((30, 18, 20), (46, 26, 28), (62, 36, 36), (82, 50, 46))
COLLAR = ((30, 20, 22), (48, 32, 32), (70, 48, 44), (92, 64, 58))
STRAP = ((14, 10, 12), (26, 20, 22), (44, 34, 36), (74, 60, 60))
CLOAK = ((10, 9, 15), (18, 17, 27), (30, 30, 46), (50, 52, 76))
CLOAK_FUR = ((8, 8, 12), (16, 16, 24), (30, 32, 46), (54, 58, 82), (84, 90, 122))
LEATHER = ((24, 16, 14), (42, 28, 24), (66, 46, 38), (96, 70, 56))
GOLD = ((88, 56, 22), (140, 92, 30), (200, 148, 40), (240, 196, 72), (255, 238, 150))
SILVER = ((70, 76, 92), (120, 128, 148), (176, 184, 200), (224, 230, 240))
ROBE_BLUE = ((96, 136, 182), (128, 170, 208), (168, 204, 230), (206, 228, 244), (240, 248, 255))
ROBE_WHITE = ((146, 160, 196), (188, 200, 226), (222, 232, 246), (246, 250, 255))
CLOAK_BLUE = ((112, 152, 196), (146, 184, 218), (184, 214, 236), (222, 238, 250), (248, 252, 255))
BAND_BLUE = ((40, 80, 122), (62, 108, 154), (96, 142, 186), (146, 184, 216))
FEATHER = ((150, 168, 204), (190, 204, 230), (222, 232, 248), (244, 250, 255), (255, 255, 255))
FEATHER_GOLD = (255, 224, 140, 255)
GOLD_GLOW = (255, 214, 96)
SPARK_GLOW = (255, 190, 90)
RED_GLOW = (255, 56, 56)
PLATE_W = ((120, 128, 150), (176, 184, 204), (216, 224, 238), (244, 248, 255), (255, 255, 255))
PLATE_G = ((96, 62, 24), (150, 100, 34), (206, 156, 50), (244, 206, 90), (255, 242, 170))
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
        cv.poly([(6, 14), (14.5, 13.5), (14.5, 15), (6, 15.5)], bro)
        cv.poly([(25.5, 14), (17, 13.5), (17, 15), (25.5, 15.5)], bro)
    else:
        cv.poly([(6, 14), (14.5, 14.5), (14.5, 16), (6.5, 15.5)], bro)
        cv.poly([(25.5, 14), (17, 14.5), (17, 16), (25, 15.5)], bro)
    # eyes: 7 x 3 texels, mirrored
    for cx0 in (7, 18):
        ex = list(range(cx0, cx0 + 7))
        left = cx0 == 7
        ir = (cx0 + 2, cx0 + 3, cx0 + 4)
        for x in ex:
            cv.put(x, 17, (22, 14, 20))                                         # the upper lid
        cv.put(cx0 - 1 if left else cx0 + 7, 16 if not div else 17, (22, 14, 20))     # a flick at the outer corner
        for x in ex:
            cv.put(x, 18, (238, 232, 238) if not div else (224, 226, 240))       # sclera
            cv.put(x, 19, (226, 218, 228) if not div else (214, 214, 232))
        ic = (200, 26, 40) if not div else (240, 196, 70)
        il = (255, 120, 110) if not div else (255, 248, 190)
        for x in ir:
            for y in (18, 19):
                cv.put(x, y, ic if (x != ir[1] or y == 18) else ((30, 0, 8) if not div else (150, 100, 24)), 255, RED_GLOW if not div else GOLD_GLOW, 235)
        cv.put(ir[1], 18, (40, 0, 10) if not div else (255, 226, 120), 255, RED_GLOW if not div else GOLD_GLOW, 255)
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

    def raise_upper(self, dy, keep=("root",), keep_prefix=("leg_", "skirt_", "shin_", "foot_")):
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
    core = lambda sd, cut="", depth=5: fur_slab(cols[:5], salt + sd, cut, depth, 0.5, -0.12)
    b.box("fur", (-5, 22.5, 1.8), (10, 5.5 * tall, 2.5), core(1), rotation=(14, 0, 0), pivot=(0, 22.5, 2.5))
    b.pair("fur", "fur", (3.2, 22.4, -3.6), (2.5, 5.5 * tall, 7.5), core(2), rotation=(0, 0, -12), pivot=(3.5, 22.4, 0))
    b.pair("fur", "fur", (1.2, 22.2, -5.0), (2.5, 4.5 * tall, 2), core(3), rotation=(-14, 0, -10), pivot=(2.5, 22.2, -4))
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
            b.clump("fur", (rx_ * math.sin(ang), y0, -rz_ * math.cos(ang)), th, alpha + 8 * hv, w, ln * tall, plate(7 + ring, v), standing=True, key="ring%d_%d" % (ring, v))
            n += 1
    # the edge of the V on the chest
    for sg in (1, -1):
        for k, y0 in enumerate((22.0, 19.6)):
            b.clump("fur", (sg * (3.2 + 0.9 * k), y0, -5.4), 0 + sg * 18, 18 + 8 * k, 4.5, 6.0, plate(8, k), key="vee%d" % k)
            n += 1
    return n


def build_base():
    b = Build("lucius_base", 512, 512)
    A = b.A
    # ----- bones
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 11, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("hair", "head", (0, 31, 0))
    b.bone("fur", "body", (0, 22, 0))
    b.bone("cloak", "body", (0, 23, 2.5))
    b.bone("cloak_top", "cloak", (0, 23, 2.5))
    b.bone("cloak_bottom", "cloak_top", (0, 12, 2.5))
    b.bone("cloak_l", "cloak_top", (7.5, 22.5, 0))
    b.bone("cloak_r", "cloak_top", (-7.5, 22.5, 0))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 5.5, 21.5, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 5.5, 14.5, 0))
        b.bone("leg" + suf, "root", (sg * 2, 13, 0))
        b.bone("skirt" + suf, "root", (sg * 2, 13, 0))
    b.bone("spark", "forearm_l", (5.5, 3.0, -0.5))

    # ----- torso, sash, collar, choker
    b.box("body", (-4, 11, -2.5), (8, 12, 5), robe_front_paint(fabric(ROBE, salt=1, period=7.0, amp=0.18, patch=ROBE_DARK, patch_thr=0.2)))
    b.box("body", (-4, 10.5, -2.5), (8, 2.5, 5), fabric(ROBE_DARK, salt=2, period=9.0, amp=0.15, seam=False), inflate=0.3, faces=SIDES)
    b.box("body", (-3.5, 22.5, -3), (7, 3.5, 6), fabric(COLLAR, salt=3, period=3.0, amp=0.25, seam=False), inflate=0.25)
    b.box("body", (-3.5, 22.8, -3), (7, 1, 6), solid(STRAP[0], noise=3), inflate=0.45, faces=SIDES)
    b.box("body", (-0.75, 21.4, -3.8), (1.5, 1.5, 0.5), metal(SILVER, salt=2, emboss=False), faces=("front", "left", "right", "top"))
    # ----- head: the face at 4 texels per pixel
    skin_p = skin(SKIN, salt=1)
    b.box("head", (-4, 24, -4), (8, 8, 8), face_paint("base", skin_p, hair(salt=2)), dens={"front": 4}, faces=("front", "left", "right", "back", "top"))
    b.pair("head", "head", (3.9, 25.5, -0.6), (0.6, 2, 1.6), skin(SKIN, salt=4), faces=("left", "front", "back", "top"))
    # hair: a thin cap, a crown of clumps standing and leaning out, the locks over the forehead and the temples
    b.box("hair", (-4.3, 31.6, -4.3), (8.6, 1.5, 8.6), hair(salt=5), faces=("top", "front", "back", "left", "right"))
    b.box("hair", (-4.4, 25.5, 3.8), (8.8, 6.5, 1.0), hair(salt=6), faces=("back", "left", "right"))
    hpl = [clump(HAIR, salt=70 + k, strands=2.4, root=0.1, curl=((-1) ** k) * 0.2, dither=0.04, split=0.0) for k in range(4)]
    crown = []
    for q in range(8):                                                      # an outer ring of clumps leaning away from the crown
        ang = q * 45.0 + 10
        crown.append((3.1 * math.sin(math.radians(ang)), 31.9, -3.1 * math.cos(math.radians(ang)), ang, 34 + 16 * hsh(q, 1, 7), 3.6 + 1.4 * hsh(q, 2, 7)))
    for q in range(4):                                                      # an inner ring standing up with a swept lean to the model's left
        ang = q * 90.0 + 40
        crown.append((1.3 * math.sin(math.radians(ang)), 32.2, -1.3 * math.cos(math.radians(ang)), 60 + ang * 0.2, 14 + 18 * hsh(q, 3, 7), 4.4 + 1.2 * hsh(q, 4, 7)))
    for n, (x, y, z, th, al, ln) in enumerate(crown):
        v = n % 4
        b.clump("hair", (x, y, z), th, al, 3.5, ln, hpl[v], standing=True, key="hc%d" % v)
    for n, x in enumerate((-3.0, -1.0, 1.0, 3.0)):                          # the fringe locks hanging over the forehead
        b.clump("hair", (x, 32.3, -4.5), (x * 6), 28 + 4 * (n % 2), 3.0, 3.0, hpl[n], key="hf%d" % n)
    for sg in (1, -1):                                                      # the temple locks
        for n, z in enumerate((-2.2, 0.6, 3.0)):
            b.clump("hair", (sg * 4.5, 31.2, z), 90 * sg, 14 + 6 * n, 3.0, 3.5 - 0.4 * n, hpl[(n + 1) % 4], key="ht%d" % ((n + 1) % 4 + 10 * (sg > 0)))
    # ----- arms
    b.pair("arm_l", "arm_r", (4, 14.5, -1.5), (3, 7.5, 3), sleeve_paint(11), inflate=0.3)
    b.pair("forearm_l", "forearm_r", (4, 9.5, -1.5), (3, 5.0, 3), sleeve_paint(12, hem=3), inflate=0.3)
    b.pair("forearm_l", "forearm_r", (4.25, 7.0, -1.25), (2.5, 3, 2.5), skin(SKIN, salt=7), faces=SIDES + ("top",))
    b.pair("forearm_l", "forearm_r", (4.25, 5.5, -1.25), (2.5, 1.5, 1.5), skin(SKIN, salt=8), faces=SIDES + ("bottom",))
    b.pair("forearm_l", "forearm_r", (3.75, 6.5, -1.0), (0.5, 2, 1.0), skin(SKIN, salt=9), faces=("left", "right", "front", "back"))
    # ----- the robe's skirt in three tiers, the legs and the boots
    for (y0, y1, w, d, salt) in ((8.0, 13.5, 5.0, 6.0, 21), (4.0, 8.0, 5.6, 6.8, 22), (0.5, 4.0, 6.2, 7.6, 23)):
        b.pair("skirt_l", "skirt_r", (0, y0, -d / 2), (w, y1 - y0, d), fabric(ROBE, salt=salt, period=9.0, amp=0.3, patch=ROBE_DARK, patch_thr=0.12,
                                                                               hem=3 if y0 < 1 else 0, hem_cols=ROBE_DARK, seam=False), faces=SIDES + ("bottom",))
    b.pair("leg_l", "leg_r", (0.5, 2.5, -1.5), (3, 11, 3), fabric(LEATHER, salt=30, period=5.0, amp=0.15, seam=False), faces=SIDES)
    b.pair("leg_l", "leg_r", (0.2, 0, -4.2), (3.6, 2.5, 5.7), fabric(LEATHER, salt=31, period=6.0, amp=0.15, seam=False))
    # ----- the cloak
    b.box("cloak_top", (-6.5, 12, 2.7), (13, 11.5, 1.5), cloak_paint(4, fur_edge="lr"))
    b.box("cloak_bottom", (-8, 0.5, 2.9), (16, 11.5, 1.5), cloak_paint(5, fur_hem=7, fur_edge="lr"))
    b.pair("cloak_l", "cloak_r", (6.6, 1.0, -2.6), (2.5, 21.5, 7.0), cloak_paint(6, fur_hem=0, fur_edge="l"), rotation=(0, 0, 9), pivot=(7.5, 22.5, 0))
    mantle(b)
    spark_parts(b)
    base_clips(A)
    b.raise_upper(2.0)
    b.m.tex_h = 512
    return b


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


def spark_parts(b, bone="spark", pivot=(5.5, 3.0, -0.5), size=1.35):
    x, y, z = pivot
    h = 0.75 * size
    b.box(bone, (x - h, y - h, z - h), (1.5 * size, 1.5 * size, 1.5 * size), solid((255, 246, 214), g=(*SPARK_GLOW, 255)), rotation=(35, 45, 0), pivot=pivot)
    q = 2.5 * size
    b.box(bone, (x - q, y - q, z - 0.25), (5 * size, 5 * size, 0.5), star_plate(), faces=("front", "back"), dens=4, pivot=pivot)
    b.box(bone, (x - q, y - q, z - 0.25), (5 * size, 5 * size, 0.5), star_plate(), faces=("front", "back"), dens=4, rotation=(0, 90, 0), pivot=pivot)


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
    cyc(a, "cast", "arm_l", "rot", L, lambda t: (104 + 4 * S(t), -8, 9 + 2 * S(t + 1)))
    cyc(a, "cast", "forearm_l", "rot", L, lambda t: (22 + 8 * S(t + 0.6), 0, 0))
    cyc(a, "cast", "arm_r", "rot", L, lambda t: (82 + 4 * S(t + 1.4), 12, -12 - 2 * S(t)))
    cyc(a, "cast", "forearm_r", "rot", L, lambda t: (36 + 8 * S(t + 2.0), 0, 0))
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
        cyc(a, "cast_big", "leg" + sd, "rot", L, lambda t, sg=sg: (-8, 0, 4 * sg))
        cyc(a, "cast_big", "skirt" + sd, "rot", L, lambda t, sg=sg: (-16 + 3 * S(t), 0, 8 * sg + 2 * S(t)))

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
        skirt_l=[(0, 0, 0), (14, 0, 0), (-10, 0, 6), (-16, 0, 10), (-16, 0, 10), (-6, 0, 4), (-2, 0, 0)],
        skirt_r=[(0, 0, 0), (14, 0, 0), (-10, 0, -6), (-16, 0, -10), (-16, 0, -10), (-6, 0, -4), (-2, 0, 0)])
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
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF" if glow else "0",
            "ages": list(ages), "tile": list(tile), "floor": floor, "figure": False, "expect": list(expect), "anim": anim, "views": views}


def std_views(h, dist=None, yaws=(0, 35, 180, 90), pitch=6, ty=None):
    ty = h * 0.5 if ty is None else ty
    d = dist or h * 2.15
    names = {0: "front", 35: "front 3/4", 180: "back", 90: "side", -35: "front 3/4 left", 145: "back 3/4"}
    return [view(names.get(y, "yaw %d" % y), y, pitch, (0, ty, 0), d) for y in yaws]


def scenes_for(models):
    sc = {}
    return sc


BUILDERS = {"lucius_base": build_base}


def main(argv):
    which = [a for a in argv if not a.startswith("-")]
    out = {}
    for name, fn in BUILDERS.items():
        if which and name not in which:
            continue
        out[name] = run_build(fn())
    return out


if __name__ == "__main__":
    if "--face" in sys.argv:
        os.makedirs(SCRATCH, exist_ok=True)
        for k in ("base", "divine"):
            face_art(k).image(10).save(os.path.join(SCRATCH, "face_%s.png" % k))
        sys.exit(0)
    main(sys.argv[1:])

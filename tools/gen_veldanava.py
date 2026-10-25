"""Veldanava, the Creator God, in the repo's GeoLite pipeline (tools/geo_builder.py -> client.geo.GeoDraw): his three forms.

    python3 -B tools/gen_veldanava.py                       # everything (idempotent)
    python3 -B tools/gen_veldanava.py humanoid              # only the named models
    python3 -B tools/gen_veldanava.py --preview [scene..]   # renders tools/render_preview scenes into <ZOG_SCRATCH>/previews_veldanava

Models (GeoSpec.of("veldanava", "<name>")), original pixel-art interpretations of the three reference images (nothing is traced):
    humanoid      the first reference: long blue-violet hair with bangs over the eyes, two gold rings at the temples, a white robe with violet borders and gold meander
                  trim, wide sleeves, a black high-collared inner outfit with a gold diamond on the chest, a tan sash, long black trousers, white pointed boots. 1.81 blocks.
    dark_avatar   the second reference (added in the next step)
    true_dragon   the third reference (added in the step after)

Clips (the same names on every model, like tools/gen_zogratis_lucius.py): idle walk run attack cast cast_big hurt transform death. Rotation signs: positive x swings a hanging
limb FORWARD, negative x folds a knee / trails cloth BACK, positive z swings a hanging limb to +x (the model's left), positive y turns the head to the model's right.
"""
import json
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from geo_builder import Model, Anim  # noqa: E402
from gen_zogratis_lucius import Build, rot_file, seq, hold, TINY, clump as _unused_clump  # noqa: E402,F401
from zogratis_lucius_kit import (SIN, COS, TAU, Canvas, canvas_paint, fabric, hair as hair_paint, skin, solid, metal, clump as clump_paint, edge_cut,  # noqa: E402
                                 cyc, swing, keys, spin, px, pal, hsh, nz, SIDES, FACE_T, strand_id)

KEY = "veldanava"
import gen_zogratis_lucius as _lucius  # noqa: E402
_lucius.KEY = KEY            # Build.__init__ names its Model after the Lucius module's KEY
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "veldanava.json")
SCRATCH = os.environ.get("ZOG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
PREVIEW_OUT = os.path.join(SCRATCH, "previews_veldanava")

# ------------------------------------------------------------------------------------------------------------------ palettes (dark -> light)
HAIR_V = ((44, 48, 128), (76, 82, 178), (122, 128, 222), (172, 178, 246))
HAIR_MID = ((60, 74, 150), (96, 116, 204), (140, 162, 236), (186, 206, 252))
HAIR_TIP = ((100, 134, 214), (140, 176, 240), (182, 212, 255), (222, 238, 255))
SKIN_P = ((196, 168, 180), (230, 206, 208), (250, 232, 226), (255, 245, 240))
WHITE = ((168, 176, 204), (208, 216, 236), (238, 242, 252), (255, 255, 255))
BLUE_B = ((34, 34, 112), (58, 58, 162), (92, 94, 206), (134, 140, 236))
GOLD = ((88, 56, 22), (140, 92, 30), (200, 148, 40), (240, 196, 72), (255, 238, 150))
BLACK = ((8, 8, 14), (18, 18, 28), (32, 32, 48), (54, 54, 78))
TAN = ((92, 58, 32), (132, 88, 48), (170, 122, 70), (206, 158, 100))
GOLD_GLOW = (255, 214, 96)
SPARKLE = (170, 200, 255)


# ------------------------------------------------------------------------------------------------------------------ painters
MEANDER = ((1, 1), (1, 0), (1, 0), (1, 1), (0, 1), (0, 1))          # a two-texel-wide bracket pattern, repeats every 6 rows


def blue_gold_band(vertical, salt=0, pitch=6):
    """The violet-blue border with a gold meander line down its middle (vertical) or along it (horizontal): returns (colour, glow) for texel position `along`/`across`."""
    def at(across, along, width):
        t = 0.45 + 0.2 * nz(across * 0.4, along * 0.4, salt)
        base = pal(BLUE_B, t, across, along, 0.05)
        row = MEANDER[along % pitch]
        mid = width // 2
        if across in (mid - 1, mid) and row[across - (mid - 1)]:
            return GOLD[2 + ((along // pitch + across) & 1)], 1
        if across == 0:
            return BLUE_B[0], 0
        if across == width - 1:
            return GOLD[1], 0
        return base, 0
    return at


def hair_sparkle(salt, cols=HAIR_V, strand=2.0, cut=None, cut_depth=5):
    """The blue-violet hair; a few pale glints (also on the glow layer) like the stars in the reference."""
    base = hair_paint(salt=salt, cols=cols, strand=strand, sheen=0.35)

    def p(face, i, j, fw, fh):
        if cut and face in SIDES and edge_cut(face, i, j, fw, fh, cut, cut_depth, salt, strand, 1.6):
            return None, None
        c, g = base(face, i, j, fw, fh)
        if hsh(i, j, salt + 41) > 0.985:
            return px((196, 214, 255)), px(SPARKLE, 230)
        return c, g
    return p


def white_robe(salt, hem=0, cut=None):
    f = fabric(WHITE, salt=salt, period=7.0, amp=0.17, lit=0.08, dither=0.04, hem=0, seam=False)

    def p(face, i, j, fw, fh):
        if cut and face in SIDES and edge_cut(face, i, j, fw, fh, cut, 3, salt, 2.0, 1.5):
            return None, None
        return f(face, i, j, fw, fh)
    return p


def panel_paint(salt, inner_left=True, bw=4, hem=6):
    """A front robe panel: the violet border with the gold meander on its inner edge, a blue hem band, the white robe elsewhere."""
    robe = white_robe(salt)
    vband = blue_gold_band(True, salt)
    hband = blue_gold_band(False, salt + 1)

    def p(face, i, j, fw, fh):
        if face not in ("front", "back"):
            return robe(face, i, j, fw, fh)
        ii = i if inner_left else fw - 1 - i
        if face == "back":
            ii = fw - 1 - ii
        if j >= fh - hem:
            c, g = hband(j - (fh - hem), i, hem)
            return px(c), (px(GOLD_GLOW, 120) if g else None)
        if ii < bw:
            c, g = vband(ii, j, bw)
            return px(c), (px(GOLD_GLOW, 120) if g else None)
        return robe(face, i, j, fw, fh)
    return p


def sleeve_paint(salt, cuff=8, funnel=True):
    robe = white_robe(salt)
    hband = blue_gold_band(False, salt + 2)

    def p(face, i, j, fw, fh):
        if face == "top" or face == "bottom":
            return (px(BLUE_B[1]), None) if face == "bottom" else robe(face, i, j, fw, fh)
        if j >= fh - cuff:
            c, g = hband(j - (fh - cuff), i, cuff)
            return px(c), (px(GOLD_GLOW, 120) if g else None)
        if funnel and (i == 0 or i == fw - 1) and face in ("front", "back"):
            return px(BLUE_B[1 + (j & 1)]), None
        return robe(face, i, j, fw, fh)
    return p


def collar_paint(salt):
    f = fabric(BLACK, salt=salt, period=5.0, amp=0.2, seam=False)

    def p(face, i, j, fw, fh):
        if (j < 2 and face in SIDES) or face == "top":
            return px(GOLD[2 + ((i // 2) & 1)]), px(GOLD_GLOW, 90)
        return f(face, i, j, fw, fh)
    return p


def gold_plain(salt=0, glow=True):
    return metal(GOLD, salt=salt, glow=(255, 214, 96, 150) if glow else None, emboss=False)


# ------------------------------------------------------------------------------------------------------------------ the face (4 texels per pixel: 32 x 32)
def face_art_humanoid():
    """Front of the head (8 px x 4 texels = 32 x 32): the bangs hang over the eyes (as in the reference), a small nose, a calm mouth, a soft chin; the lower face stays bare."""
    cv = Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            shade = 0.55 + 0.1 * (1 - abs(x - 15.5) / 16) - 0.2 * (y / 31.0)
            cv.put(x, y, pal(SKIN_P, shade, x, y, 0.05))
    hp = hair_sparkle(7)
    for y in range(32):
        for x in range(32):
            edge = 14 + int(3 * hsh(x // 3, 3, 9)) + (3 if x // 3 % 3 == 1 else 0)       # a ragged lower edge of the bangs
            if y < edge:
                c, g = hp("front", x, y, 32, 32)
                cv.put(x, y, c[:3] if c else HAIR_V[1], glow=g[:3] if g else None, ga=g[3] if g else 255)
    cv.rect(15, 22, 16, 23, SKIN_P[0])                                                     # nose
    cv.put(15, 24, SKIN_P[0])
    cv.rect(13, 27, 18, 27, (206, 132, 148))                                                # mouth
    cv.rect(14, 28, 17, 28, (226, 168, 176))
    cv.rect(11, 31, 20, 31, SKIN_P[0])                                                      # chin shade
    return cv


def head_paint(art, hair_p, skin_p):
    def p(face, i, j, fw, fh):
        if face == "front":
            return art.at(i, j)
        if face in ("left", "right", "back", "top"):
            if face in ("left", "right") and j >= 22 and 8 <= (i if face == "left" else fw - 1 - i) <= 24:
                return skin_p(face, i, j, fw, fh)                                          # the cheek below the hair on the sides
            return hair_p(face, i, j, fw, fh)
        return skin_p(face, i, j, fw, fh)
    return p


# ------------------------------------------------------------------------------------------------------------------ the humanoid rig
def build_humanoid():
    """The Minecraft-skin layout the owner pointed at (8 px head, 12 px torso, 12 px arms and legs; 2.0 blocks with the hair, the renderer scales it to about 1.8),
    with the skin's second layer built as real 3D overlays: the robe, the wide sleeves, the flared trousers, the hair (cap, side layers, bangs, back chain), the gold pieces."""
    b = Build("humanoid", 512, 1536)
    A = b.A
    skin_p = skin(SKIN_P, salt=2)
    # ----- bones
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 12, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("hair", "head", (0, 32, 0))
    b.bone("hair_b1", "body", (0, 30.0, 4.6))
    b.bone("hair_b2", "hair_b1", (0, 20.0, 4.6))
    b.bone("hair_b3", "hair_b2", (0, 10.0, 4.6))
    b.bone("lock_l", "head", (4.4, 30.0, -3.0))
    b.bone("lock_r", "head", (-4.4, 30.0, -3.0))
    b.bone("robe_back", "body", (0, 23.0, 2.4))
    b.bone("robe_l", "body", (3.0, 23.0, -3.0))
    b.bone("robe_r", "body", (-3.0, 23.0, -3.0))
    b.bone("sash_tail", "body", (3.6, 15.0, -2.9))
    b.bone("front_cloth", "body", (0, 12.5, -2.6))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 6.0, 22.0, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 6.0, 17.0, 0))
        b.bone("leg" + suf, "root", (sg * 2.0, 12, 0))
        b.bone("skirt" + suf, "root", (sg * 2.0, 12, 0))
        b.bone("ring" + suf, "head", (sg * 6.2, 29.4, 0))

    # ----- legs (base layer 4x12x4): black trousers, white boots with a gold cuff and a pointed toe; second layer: the trousers flare over the boots
    b.pair("leg_l", "leg_r", (0.0, 5.0, -2.0), (4.0, 7.0, 4.0), fabric(BLACK, salt=30, period=5.0, amp=0.16, seam=False), faces=SIDES)
    b.pair("leg_l", "leg_r", (0.0, 0.0, -2.0), (4.0, 5.2, 4.0), fabric(WHITE, salt=31, period=6.0, amp=0.12, seam=False))
    b.pair("leg_l", "leg_r", (0.0, 0.0, -5.0), (4.0, 2.2, 3.0), fabric(WHITE, salt=32, period=6.0, amp=0.1, seam=False))
    b.pair("leg_l", "leg_r", (0.2, 1.4, -6.5), (3.6, 1.0, 1.6), fabric(WHITE, salt=33, period=6.0, amp=0.1, seam=False), rotation=(-18, 0, 0), pivot=(2.0, 2.0, -5.0))   # the pointed, lifted toe
    b.pair("leg_l", "leg_r", (-0.2, 3.4, -2.4), (4.4, 0.8, 4.8), metal(GOLD, salt=34, glow=None, emboss=False), faces=SIDES + ("top",))       # gold boot cuff
    for k, (y0, y1, w, d, salt) in enumerate(((8.0, 12.4, 4.6, 4.8, 21), (4.0, 8.0, 5.0, 5.4, 22))):
        b.pair("skirt_l", "skirt_r", (-0.3, y0, -d / 2), (w, y1 - y0, d), fabric(BLACK, salt=salt, period=8.0, amp=0.22, seam=False, hem=2 if y0 < 5 else 0, hem_cols=BLACK),
               faces=SIDES + ("bottom",))
    b.box("front_cloth", (-2.2, 4.4, -3.1), (4.4, 8.4, 0.8), lambda f, i, j, w, h: fabric(BLACK, salt=24, period=7.0, amp=0.24, seam=False)(f, i, j, w, h)
          if not (f in SIDES and edge_cut(f, i, j, w, h, "b", 5, 24, 2.0, 1.5)) else (None, None), faces=SIDES + ("bottom",))

    # ----- torso (base layer 8x12x4): the black inner outfit with the high collar, the tan sash, the gold diamond
    b.box("body", (-4.0, 12.0, -2.0), (8.0, 12.0, 4.0), fabric(BLACK, salt=1, period=6.0, amp=0.2, seam=False))
    b.box("body", (-2.5, 22.4, -2.5), (5.0, 2.0, 5.0), collar_paint(3))
    b.box("body", (-4.4, 12.4, -2.4), (8.8, 3.2, 4.8), fabric(TAN, salt=2, period=6.0, amp=0.2, seam=False))
    b.box("body", (-1.2, 18.8, -2.7), (2.4, 2.4, 0.8), gold_plain(5), rotation=(0, 0, 45), pivot=(0, 20.0, -2.3), faces=("front", "left", "right", "top"))
    b.box("body", (-0.55, 19.4, -2.95), (1.1, 1.2, 0.3), solid((255, 238, 160), g=(255, 220, 120, 220)), rotation=(0, 0, 45), pivot=(0, 20.0, -2.5), faces=("front",))
    b.box("sash_tail", (2.6, 5.4, -3.3), (2.2, 9.6, 0.9), fabric(TAN, salt=6, period=5.0, amp=0.2, seam=False, hem=2, hem_cols=TAN), faces=SIDES + ("bottom",))
    b.box("sash_tail", (2.3, 13.2, -3.4), (2.8, 2.4, 1.2), fabric(TAN, salt=7, period=4.0, amp=0.2, seam=False))                      # the knot
    # ----- the white robe, second layer: shoulder yokes, two front panels with the violet border and the gold meander, the back panel
    b.pair("body", "body", (2.6, 20.6, -2.7), (2.6, 3.6, 5.4), white_robe(8), faces=SIDES + ("top", "bottom"))
    b.pair("robe_l", "robe_r", (1.7, 2.2, -3.2), (2.7, 21.0, 0.9), panel_paint(9, inner_left=True, bw=4, hem=6), faces=SIDES + ("bottom",))
    b.box("robe_back", (-4.6, 2.2, 2.2), (9.2, 21.2, 0.9), white_robe(10, cut="b"), faces=SIDES + ("bottom",))
    # ----- head: the face (base layer), then the hair as a second layer: cap, side layers, back plate, bangs, temple locks
    head = b.box("head", (-4, 24, -4), (8, 8, 8), head_paint(face_art_humanoid(), hair_sparkle(11), skin_p), dens={"front": 4}, faces=("front", "left", "right", "back", "top", "bottom"))
    b.box("hair", (-4.5, 31.4, -4.5), (9.0, 1.7, 9.0), hair_sparkle(12), faces=("top", "back", "left", "right", "front"))
    b.pair("hair", "hair", (4.0, 25.2, -4.4), (0.7, 6.4, 8.8), hair_sparkle(22), faces=("front", "back", "top", "left", "right"))                # the side hair layers
    b.box("hair", (-4.5, 24.4, 3.7), (9.0, 7.2, 1.0), hair_sparkle(13), faces=("back", "left", "right"))
    hp = [clump_paint(HAIR_V, salt=70 + k, strands=2.6, root=0.1, curl=((-1) ** k) * 0.18, dither=0.04) for k in range(4)]
    for n, x in enumerate((-3.6, -1.8, 0.0, 1.8, 3.6)):                                                                                     # the bangs over the eyes
        b.clump("head", (x, 32.3, -4.6), x * 4, 6 + 3 * (n % 2), 3.2, 5.6 - 0.6 * (n % 2), hp[n % 4], key="bg%d" % (n % 4))
    for sg in (1, -1):                                                                                                                      # the temple locks
        for n, z in enumerate((-2.6, 0.0, 2.6)):
            b.clump("head", (sg * 4.6, 31.4, z), 90 * sg, 6 + 4 * n, 2.8, 8.0 - 1.0 * n, hp[(n + 1) % 4], key="tl%d%d" % ((n + 1) % 4, sg > 0))
    # long hair down the back: three segments, lavender at the root fading to pale blue at the tips, ending in tapering strands
    b.box("hair_b1", (-4.8, 20.0, 4.0), (9.6, 10.0, 2.0), hair_sparkle(14), faces=SIDES)
    b.box("hair_b2", (-5.0, 10.0, 4.0), (10.0, 10.0, 2.0), hair_sparkle(15, cols=HAIR_MID), faces=SIDES)
    b.box("hair_b3", (-4.8, 3.0, 4.0), (9.6, 7.0, 2.0), hair_sparkle(16, cols=HAIR_TIP, cut="b", cut_depth=6), faces=SIDES)
    hpt = [clump_paint(HAIR_TIP, salt=90 + k, strands=2.6, root=0.1, curl=((-1) ** k) * 0.18, dither=0.04) for k in range(4)]
    for n, x in enumerate((-3.6, -1.2, 1.2, 3.6)):
        b.clump("hair_b3", (x, 3.6, 5.0), 180, 4, 2.8, 5.0 - (n % 2) * 1.2, hpt[n % 4], key="hb%d" % (n % 4))
    for suf, sg in (("_l", 1), ("_r", -1)):                                                                                                # the front locks over the shoulders
        b.box("lock" + suf, (sg * 4.4 - 0.9, 17.0, -4.2), (1.8, 13.0, 1.3), hair_sparkle(17 + (sg > 0), cut="b", cut_depth=5), faces=SIDES)
    # gold rings at the temples: an ellipse of short bars in the x-y plane, a stud joining each to the head
    for suf, sg in (("_l", 1), ("_r", -1)):
        n_seg, rx, ry, cx, cy = 12, 2.0, 3.4, sg * 6.2, 29.4
        for k in range(n_seg):
            th = TAU * (k + 0.5) / n_seg
            x, y = cx + rx * COS(th), cy + ry * SIN(th)
            dx, dy = -rx * SIN(th), ry * COS(th)
            ln = math.hypot(dx, dy) * TAU / n_seg * 1.12
            ang = math.degrees(math.atan2(dy, dx))
            b.box("head", (x - ln / 2, y - 0.45, -0.8), (ln, 0.9, 1.6), gold_plain(40 + k), rotation=(0, 0, ang), pivot=(x, y, 0), faces=("front", "back", "top", "bottom", "left", "right"),
                  key="ring%d" % (k % 3))
        b.box("head", (min(sg * 4.0, sg * 4.2 + 0.0), 29.0, -0.6), (0.9 + 0.0, 0.9, 1.2), gold_plain(60), faces=("front", "back", "top", "bottom"))
    # ----- arms (base layer 4x12x4 = the sleeve above the elbow and the bare forearm), second layer: the shoulder cap and the wide funnel sleeve with the blue and gold cuff
    b.pair("arm_l", "arm_r", (4.0, 17.0, -2.0), (4.0, 7.0, 4.0), sleeve_paint(20, cuff=0, funnel=False))
    b.pair("arm_l", "arm_r", (3.8, 20.2, -2.4), (4.8, 4.0, 4.8), white_robe(23), faces=SIDES + ("top", "bottom"))                              # the shoulder cap
    b.pair("forearm_l", "forearm_r", (4.0, 11.0, -2.0), (4.0, 6.0, 4.0), skin(SKIN_P, salt=9), faces=SIDES + ("bottom",))                   # the bare forearm and hand
    b.pair("forearm_l", "forearm_r", (4.2, 10.6, -1.7), (3.6, 1.2, 3.4), skin(SKIN_P, salt=10), faces=SIDES + ("bottom",))                  # the fingers
    b.pair("forearm_l", "forearm_r", (3.6, 13.4, -3.2), (6.0, 4.6, 6.4), sleeve_paint(21, cuff=5), faces=SIDES + ("top", "bottom"))           # the wide funnel sleeve

    clips_humanoid(A)
    return b


# ------------------------------------------------------------------------------------------------------------------ the Dark Avatar of God
HAIR_K = ((6, 6, 12), (16, 16, 28), (32, 34, 54), (62, 66, 98))
WHITE_L = ((196, 204, 226), (226, 232, 246), (246, 248, 255), (255, 255, 255))
SKIN_D = ((176, 168, 182), (208, 202, 214), (230, 226, 236), (244, 242, 248))
CHAR = ((10, 10, 16), (22, 22, 32), (38, 38, 52), (60, 60, 80))
SILVER = ((108, 112, 132), (166, 172, 192), (214, 220, 238), (248, 250, 255))
HORN = ((58, 58, 68), (92, 92, 104), (132, 132, 146), (176, 176, 190))
AMBER = (255, 150, 22)
SILVER_GLOW = (200, 212, 255)


def lined(salt, cols=CHAR, edge=1, hem=True, centre=False, glow=70):
    """Fitted charcoal cloth with the silver-white lines: one line a texel in from each vertical edge, one near the hem, optionally one down the middle."""
    base = fabric(cols, salt=salt, period=6.0, amp=0.18, seam=False)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face in ("front", "back", "left", "right"):
            on = i == edge or i == fw - 1 - edge or (hem and j == fh - 2) or (centre and i == fw // 2 and face == "front")
            if on and fw > 3 and fh > 3:
                return px(SILVER[2 + (hsh(i, j, salt) > 0.5)]), px(SILVER_GLOW, glow)
        return c, g
    return p


def white_panel(salt, border=1):
    """A white cloth panel with a thin charcoal edge line and a silver hem."""
    base = fabric(WHITE_L, salt=salt, period=7.0, amp=0.12, lit=0.12, dither=0.04, hem=0, seam=False)

    def p(face, i, j, fw, fh):
        c, g = base(face, i, j, fw, fh)
        if face in ("front", "back") and fw > 3:
            if i < border or i >= fw - border:
                return px(CHAR[1]), None
            if j >= fh - 2:
                return px(SILVER[1]), px(SILVER_GLOW, 60)
        return c, g
    return p


def cloth_flap(salt, panel=True):
    """A hanging black flap: black cloth, a white panel down the middle third, silver edge lines."""
    cloth = lined(salt, cols=BLACK, edge=0, hem=True)
    white = white_panel(salt + 1)

    def p(face, i, j, fw, fh):
        if panel and face in ("front", "back") and fw // 3 <= i < fw - fw // 3 and fh // 6 < j < fh - 3:
            return white(face, i, j, fw, fh)
        return cloth(face, i, j, fw, fh)
    return p


def horn_paint(salt):
    def p(face, i, j, fw, fh):
        t = 0.75 - 0.35 * (i / max(1, fw - 1)) + FACE_T[face] * 0.5 + 0.08 * (hsh(i, j, salt) - 0.5)
        if j % 5 == 0:
            t -= 0.3                                                      # growth rings
        return px(pal(HORN, t, i, j, 0.04)), None
    return p


def hair_black(salt, cut=None, cut_depth=5):
    base = hair_paint(salt=salt, cols=HAIR_K, strand=2.0, sheen=0.4)

    def p(face, i, j, fw, fh):
        if cut and face in SIDES and edge_cut(face, i, j, fw, fh, cut, cut_depth, salt, 2.0, 1.6):
            return None, None
        return base(face, i, j, fw, fh)
    return p


def face_art_avatar():
    """Front of the head (32 x 32): pale face, a centre-parted black fringe with the side curtains, sharp amber eyes (on the glow layer), a slim nose, a thin mouth."""
    cv = Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            shade = 0.6 + 0.08 * (1 - abs(x - 15.5) / 16) - 0.16 * (y / 31.0)
            cv.put(x, y, pal(SKIN_D, shade, x, y, 0.04))
    hp = hair_black(5)
    for y in range(32):
        for x in range(32):
            part = abs(x - 15.5)
            if y < 6 or x < 4 or x > 27 or (y < 13 and part > 3.2 + (12 - y) * 0.9 and y < 6 + (part - 3.0) * 1.1):
                c, g = hp("front", x, y, 32, 32)
                cv.put(x, y, c[:3] if c else HAIR_K[1])
    for x0 in (6, 19):                                                                       # the eyes: dark upper lid, amber iris, a pupil
        for y in range(15, 19):
            for x in range(x0, x0 + 7):
                lid = y == 15
                rim = (x in (x0, x0 + 6)) and y >= 18
                if lid or rim:
                    cv.put(x, y, HAIR_K[0])
                else:
                    cv.put(x, y, AMBER if y == 16 else (214, 112, 14), glow=AMBER, ga=235)
        cv.put(x0 + 3, 17, (70, 20, 6)); cv.put(x0 + 3, 16, (70, 20, 6))
        for x in range(x0 - 1, x0 + 8):                                                      # the brow
            cv.put(x, 13 + (1 if (x - x0) % 7 in (0, 6) else 0), HAIR_K[0])
    cv.rect(15, 21, 16, 23, SKIN_D[0])                                                       # nose
    cv.rect(13, 26, 18, 26, (120, 76, 92))                                                   # mouth
    cv.rect(11, 31, 20, 31, SKIN_D[0])                                                       # chin shade
    return cv


def build_avatar():
    """The second reference: the Dark Avatar of God. The same skin-sized body as the humanoid, an unrelated look: a fitted charcoal outfit with silver lines, layered black
    cloth with white panels, a short mantle, dark pointed shoes, hair down to the shoes, two big curved horns, amber eyes."""
    b = Build("dark_avatar", 512, 1536)
    A = b.A
    skin_p = skin(SKIN_D, salt=4)
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 12, 0))
    b.bone("head", "body", (0, 24, 0))
    b.bone("hair", "head", (0, 32, 0))
    b.bone("hair_b1", "body", (0, 30.0, 4.6))
    b.bone("hair_b2", "hair_b1", (0, 20.0, 4.6))
    b.bone("hair_b3", "hair_b2", (0, 10.0, 4.6))
    b.bone("lock_l", "head", (4.4, 30.0, -3.0))
    b.bone("lock_r", "head", (-4.4, 30.0, -3.0))
    b.bone("robe_back", "body", (0, 23.5, 2.4))
    b.bone("robe_l", "body", (2.2, 15.8, -2.8))
    b.bone("robe_r", "body", (-2.2, 15.8, -2.8))
    b.bone("sash_tail", "body", (3.6, 14.8, -2.9))
    b.bone("front_cloth", "body", (0, 14.6, -2.6))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 6.0, 22.0, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 6.0, 17.0, 0))
        b.bone("leg" + suf, "root", (sg * 2.0, 12, 0))
        b.bone("skirt" + suf, "root", (sg * 2.0, 12, 0))
        b.bone("horn" + suf, "head", (sg * 3.2, 31.0, 0.0))

    # ----- legs: fitted trousers with a silver line, a black thigh wrap, dark pointed shoes with a silver rim
    b.pair("leg_l", "leg_r", (0.0, 4.8, -2.0), (4.0, 7.2, 4.0), lined(30, hem=False, edge=0))
    b.pair("leg_l", "leg_r", (0.0, 0.0, -2.0), (4.0, 5.0, 4.0), fabric(BLACK, salt=31, period=6.0, amp=0.12, seam=False))
    b.pair("leg_l", "leg_r", (0.0, 0.0, -5.2), (4.0, 1.8, 3.2), fabric(BLACK, salt=32, period=6.0, amp=0.1, seam=False))
    b.pair("leg_l", "leg_r", (0.3, 1.2, -7.0), (3.4, 0.9, 1.9), fabric(BLACK, salt=33, period=6.0, amp=0.1, seam=False), rotation=(-22, 0, 0), pivot=(2.0, 1.8, -5.2))
    b.pair("leg_l", "leg_r", (-0.15, 4.4, -2.15), (4.3, 0.5, 4.3), metal(SILVER, salt=34, glow=None, emboss=False), faces=SIDES + ("top",))
    b.pair("skirt_l", "skirt_r", (-0.3, 6.6, -2.4), (4.6, 5.8, 4.8), lined(21, cols=BLACK, edge=0, hem=True), faces=SIDES + ("bottom",))
    # ----- torso: fitted charcoal, silver centre seam and collar, a belt with a silver buckle, a white chest panel
    b.box("body", (-4.0, 12.0, -2.0), (8.0, 12.0, 4.0), lined(1, centre=True, edge=1, hem=False))
    b.box("body", (-2.5, 22.4, -2.5), (5.0, 2.0, 5.0), lined(3, edge=0, hem=False))
    b.box("body", (-2.5, 24.1, -2.5), (5.0, 0.4, 5.0), metal(SILVER, salt=35, glow=None, emboss=False), faces=SIDES + ("top",))
    b.box("body", (-4.3, 12.2, -2.3), (8.6, 2.6, 4.6), fabric(BLACK, salt=2, period=6.0, amp=0.2, seam=False))
    b.box("body", (-1.0, 12.6, -2.75), (2.0, 1.8, 0.6), metal(SILVER, salt=36, glow=(220, 230, 255, 120), emboss=False), faces=("front", "top", "left", "right"))
    b.box("body", (-2.4, 14.8, -2.6), (4.8, 8.4, 0.6), white_panel(37), faces=("front", "left", "right", "top"))
    # ----- the layered black cloth: two long front flaps with white panels, a short white-panelled apron, a hip strip, the mantle
    b.pair("robe_l", "robe_r", (0.5, 2.4, -3.2), (3.4, 13.4, 0.9), cloth_flap(40), faces=SIDES + ("bottom",))
    b.box("front_cloth", (-1.3, 5.6, -3.7), (2.6, 9.0, 0.8), cloth_flap(42), faces=SIDES + ("bottom",))
    b.box("sash_tail", (3.0, 3.6, -3.1), (1.8, 11.0, 0.8), lined(44, cols=BLACK, edge=0, hem=True), faces=SIDES + ("bottom",))
    b.box("robe_back", (-4.8, 12.0, 2.2), (9.6, 12.0, 0.9), cloth_flap(46, panel=False), faces=SIDES + ("bottom",))
    b.box("robe_back", (-4.4, 5.0, 2.7), (8.8, 8.0, 0.8), white_panel(47), faces=SIDES + ("bottom",))
    # ----- head: the face, the hair as layers, the horns
    b.box("head", (-4, 24, -4), (8, 8, 8), head_paint(face_art_avatar(), hair_black(11), skin_p), dens={"front": 4}, faces=("front", "left", "right", "back", "top", "bottom"))
    b.box("hair", (-4.5, 31.4, -4.5), (9.0, 1.7, 9.0), hair_black(12), faces=("top", "back", "left", "right", "front"))
    b.pair("hair", "hair", (4.0, 25.2, -4.4), (0.7, 6.4, 8.8), hair_black(22), faces=("front", "back", "top", "left", "right"))
    b.box("hair", (-4.5, 24.4, 3.7), (9.0, 7.2, 1.0), hair_black(13), faces=("back", "left", "right"))
    hk = [clump_paint(HAIR_K, salt=170 + k, strands=2.6, root=0.1, curl=((-1) ** k) * 0.18, dither=0.04) for k in range(4)]
    for n, x in enumerate((-3.9, 0.0, 3.9)):                                                                                              # the fringe: a short centre and the side curtains
        b.clump("head", (x, 32.3, -4.6), x * 3, 6, 3.0 if x else 2.4, 7.6 if x else 4.0, hk[n % 4], key="bg%d" % n)
    for sg in (1, -1):
        for n, z in enumerate((-2.6, 0.0, 2.6)):
            b.clump("head", (sg * 4.6, 31.4, z), 90 * sg, 6 + 4 * n, 2.8, 8.0 - 1.0 * n, hk[(n + 1) % 4], key="tl%d%d" % ((n + 1) % 4, sg > 0))
    b.box("hair_b1", (-4.8, 20.0, 4.0), (9.6, 10.0, 2.0), hair_black(14), faces=SIDES)
    b.box("hair_b2", (-5.2, 10.0, 4.0), (10.4, 10.0, 2.0), hair_black(15), faces=SIDES)
    b.box("hair_b3", (-5.0, 2.0, 4.0), (10.0, 8.0, 2.0), hair_black(16, cut="b", cut_depth=7), faces=SIDES)
    for n, x in enumerate((-3.8, -1.3, 1.3, 3.8)):
        b.clump("hair_b3", (x, 2.8, 5.0), 180, 4, 3.0, 6.0 - (n % 2) * 1.6, hk[n % 4], key="hb%d" % (n % 4))
    for suf, sg in (("_l", 1), ("_r", -1)):                                                                                               # the long front locks
        b.box("lock" + suf, (sg * 4.4 - 0.9, 12.0, -4.2), (1.8, 18.0, 1.3), hair_black(17 + (sg > 0), cut="b", cut_depth=6), faces=SIDES)
    # the horns: four tapering segments sweeping up and outward, then curving back and in
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.spike("horn" + suf, (sg * 3.2, 31.0, 0.5), 3.4, 17.0, 6, -sg * 24, horn_paint(60), key="horn", fracs=(0.3, 0.28, 0.24, 0.18), taper=0.8, bend=(15, sg * 12))
    # ----- arms: fitted sleeves with silver cuffs and bare hands, an armoured left shoulder and a cloth right one
    b.pair("arm_l", "arm_r", (4.0, 17.0, -2.0), (4.0, 7.0, 4.0), lined(20, edge=0, hem=False))
    b.box("arm_l", (3.7, 20.2, -2.6), (5.0, 4.4, 5.2), lined(23, cols=CHAR, edge=0, hem=True), faces=SIDES + ("top", "bottom"))
    b.box("arm_l", (3.5, 23.8, -2.3), (4.6, 0.6, 4.6), metal(SILVER, salt=38, glow=None, emboss=False), faces=SIDES + ("top",))
    b.box("arm_r", (-8.7, 20.6, -2.4), (4.7, 3.6, 4.8), fabric(BLACK, salt=24, period=6.0, amp=0.2, seam=False), faces=SIDES + ("top", "bottom"))
    b.pair("forearm_l", "forearm_r", (4.0, 12.4, -2.0), (4.0, 4.6, 4.0), lined(25, edge=0, hem=False))
    b.pair("forearm_l", "forearm_r", (3.85, 12.2, -2.15), (4.3, 0.6, 4.3), metal(SILVER, salt=39, glow=None, emboss=False), faces=SIDES + ("top", "bottom"))
    b.pair("forearm_l", "forearm_r", (4.2, 10.4, -1.8), (3.6, 1.9, 3.6), skin(SKIN_D, salt=9), faces=SIDES + ("bottom",))

    clips_humanoid(A)
    return b


# ------------------------------------------------------------------------------------------------------------------ the True Dragon
TEAL = ((8, 44, 56), (18, 86, 96), (34, 138, 138), (78, 190, 178), (146, 230, 212))
CREAM = ((140, 116, 84), (196, 174, 130), (228, 210, 168), (248, 238, 200))
CLAWC = ((96, 84, 64), (150, 136, 104), (204, 192, 156), (240, 232, 200))
MEMB = ((18, 84, 92), (40, 130, 134), (86, 182, 174), (150, 228, 208))
EYE_G = (255, 204, 72)
D_DENS = 1


def scales(cols, salt, cw=4, ch=3, lift=0.0):
    """Overlapping rows of scales: a bright centre, a dark rim on the lower and one side edge, every other row shifted by half a scale."""
    def p(face, i, j, fw, fh):
        row = j // ch
        ci = (i + (cw // 2 if row & 1 else 0)) % cw
        cj = j % ch
        t = 0.52 + lift + 0.14 * (hsh(i // cw + row * 7, row, salt) - 0.5) - 0.10 * (j / max(1, fh - 1)) + FACE_T[face] * 0.5
        if cj == ch - 1 or ci == 0:
            t -= 0.26
        elif cj == 0 and 1 <= ci <= cw - 2:
            t += 0.12
        return px(pal(cols, t, i, j, 0.04)), None
    return p


def plates(cols, salt, band=3):
    """Belly plates: broad horizontal scutes with a dark seam between them."""
    def p(face, i, j, fw, fh):
        t = 0.62 + 0.1 * (hsh(j // band, salt, 5) - 0.5) + FACE_T[face] * 0.4
        if j % band == band - 1:
            t -= 0.3
        if (i % 9) == 0 and band > 2:
            t -= 0.08
        return px(pal(cols, t, i, j, 0.03)), None
    return p


def body_paint(salt, belly=0.62, cw=4):
    """Teal scales on top and the flanks, the cream plates on the underside and the lower flank (a ragged boundary)."""
    sc = scales(TEAL, salt, cw)
    dark = scales(TEAL, salt + 1, cw, lift=-0.08)
    pl = plates(CREAM, salt + 2)

    def p(face, i, j, fw, fh):
        if face == "bottom":
            return pl(face, i, j, fw, fh)
        if face == "top":
            return dark(face, i, j, fw, fh)
        if face in ("front", "back", "left", "right") and j / max(1, fh - 1) > belly + 0.12 * (hsh(i // 2, salt, 9) - 0.5):
            return pl(face, i, j, fw, fh)
        return sc(face, i, j, fw, fh)
    return p


def limb_paint(salt, cw=4, lift=0.0):
    return scales(TEAL, salt, cw, lift=lift)


def claw_paint(salt):
    def p(face, i, j, fw, fh):
        t = 0.74 - 0.5 * (j / max(1, fh - 1)) + FACE_T[face] * 0.4 + 0.06 * (hsh(i, j, salt) - 0.5)
        if j % 4 == 0:
            t -= 0.12
        return px(pal(CLAWC, t, i, j, 0.03)), None
    return p


def membrane_paint(salt):
    """A wing membrane: translucent-looking teal with dark veins running along the long side and a pale rim."""
    def p(face, i, j, fw, fh):
        t = 0.55 + 0.16 * nz(i * 0.25, j * 0.25, salt) + FACE_T[face] * 0.3
        long_i = fw >= fh
        k = i if not long_i else j
        if k % 5 == 0 and 2 < k < (fw if not long_i else fh) - 2:
            t -= 0.32
        if (j if not long_i else i) in (0, (fh if not long_i else fw) - 1):
            t += 0.2
        return px(pal(MEMB, t, i, j, 0.04)), None
    return p


def eye_paint():
    """A glowing amber eye with a dark vertical slit pupil."""
    def p(face, i, j, fw, fh):
        cx = (fw - 1) / 2.0
        if abs(i - cx) < 0.6 and 0 < j < fh - 1:
            return px((24, 10, 4)), px((60, 20, 4), 255)
        t = 0.6 + 0.3 * (1 - abs(j - (fh - 1) / 2.0) / max(1.0, fh / 2.0))
        return px((255 * t, 196 * t, 60 * t)), px((255, 196, 70), 255)
    return p


def build_dragon():
    """The True Dragon: a teal serpentine dragon with a cream underside, four limbs, a long S-curved neck, a detailed horned head, bone-and-membrane wings and a long tail.
    About 11.7 blocks head to tail, a wingspan of about 14 blocks, 5 blocks to the raised head. Everything is built straight along -z and bent by the bones' rest rotations."""
    b = Build("true_dragon", 1024, 2048, dens=D_DENS)
    A = b.A
    CL = claw_paint(50)
    bonep = claw_paint(51)

    def seg(bone, x0, y0, z0, w, h, d, paint, **kw):
        return b.box(bone, (x0, y0, z0), (w, h, d), paint, **kw)

    # ----- bones
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 44, 0))
    NECK_Z = [-20.0, -32.0, -44.0, -56.0]
    NECK_REL = [(62, 0, 0), (-8, 0, 0), (-26, 0, 0), (-26, 0, 0)]
    prev = "body"
    for k in range(4):
        b.bone("neck%d" % (k + 1), prev, (0, 50, NECK_Z[k]), NECK_REL[k])
        prev = "neck%d" % (k + 1)
    b.bone("head", "neck4", (0, 50, -68))
    b.bone("jaw", "head", (0, 47, -71))
    TAIL_Z = [22.0 + 16 * k for k in range(5)]
    TAIL_REL = [(6, 6, 0), (5, -12, 0), (5, 12, 0), (5, -12, 0), (5, 10, 0)]
    prev = "body"
    for k in range(5):
        b.bone("tail%d" % (k + 1), prev, (0, 46, TAIL_Z[k]), TAIL_REL[k])
        prev = "tail%d" % (k + 1)
    FING = ((-6, 44.0), (16, 50.0), (40, 44.0), (66, 34.0))                 # (angle back from +x, length)
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("wing" + suf, "body", (sg * 11, 54, -8), (0, -sg * 8, sg * 18))
        b.bone("wing2" + suf, "wing" + suf, (sg * 35, 54.5, -8), (0, -sg * 6, sg * 8))
        for n, (phi, ln) in enumerate(FING):
            b.bone("wf%d%s" % (n + 1, suf), "wing2" + suf, (sg * 69, 54.5, -8), (0, -sg * phi, 0))
        b.bone("fleg" + suf, "body", (sg * 9, 38, -12), (-22, 0, 0))
        b.bone("fleg2" + suf, "fleg" + suf, (sg * 9, 18, -12), (50, 0, 0))
        b.bone("ffoot" + suf, "fleg2" + suf, (sg * 9, 1, -12), (-28, 0, 0))
        b.bone("hleg" + suf, "body", (sg * 10, 38, 14), (35, 0, 0))
        b.bone("hleg2" + suf, "hleg" + suf, (sg * 10, 18, 14), (-70, 0, 0))
        b.bone("hfoot" + suf, "hleg2" + suf, (sg * 10, -2, 14), (35, 0, 0))

    # ----- torso: a barrel chest, narrower hips, the dorsal row of spikes
    seg("body", -11, 36, -22, 22, 20, 24, body_paint(1))
    seg("body", -10, 37, 2, 20, 17, 20, body_paint(3))
    for z in (-15, -7, 1, 9, 17):
        b.spike("body", (0, 56.0 if z < 2 else 54.0, z), 3.0, 8.0, 16, 0, bonep, key="ds", fracs=(0.55, 0.45), taper=0.6)
    # ----- the neck: four tapering segments, a cream throat, a row of back spikes
    for k, (w, h) in enumerate(((16, 16), (14, 14), (12, 12), (10, 10))):
        seg("neck%d" % (k + 1), -w / 2, 50 - h / 2, NECK_Z[k] - 12, w, h, 12, body_paint(10 + k, belly=0.66, cw=3))
        b.spike("neck%d" % (k + 1), (0, 50 + h / 2 - 0.4, NECK_Z[k] - 6), 2.6, 6.0, 22, 0, bonep, key="nsp", fracs=(0.55, 0.45), taper=0.6)
    # ----- the head: cranium, brow, snout with a raised nasal ridge and an upper lip, lower jaw on its own bone, fangs and teeth, horns, cheek spines, barbels, slit-pupil eyes
    hp = body_paint(20, belly=0.8, cw=3)
    seg("head", -7.0, 43.6, -85, 14, 13.4, 17, hp, dens=2)                                                    # the cranium
    seg("head", -4.6, 45.4, -98, 9.2, 7.6, 13.5, hp, dens=2)                                                  # the snout
    seg("head", -1.3, 52.8, -97, 2.6, 1.6, 12, claw_paint(57), dens=2)                                        # the nasal ridge
    seg("head", -5.0, 44.6, -97.5, 10.0, 1.6, 12.5, scales(TEAL, 58, 3, lift=-0.1), dens=2)                   # the upper lip
    seg("head", -5.4, 42.2, -80, 10.8, 2.6, 8.0, plates(CREAM, 59), dens=2)                                   # the throat under the cranium
    seg("jaw", -4.9, 42.4, -96.5, 9.8, 4.4, 25.5, body_paint(21, belly=0.0, cw=3), dens=2)                   # the lower jaw
    seg("jaw", -5.6, 42.2, -76, 11.2, 5.0, 7.0, body_paint(21, belly=0.0, cw=3), dens=2)                      # its hinge
    tooth = solid((238, 234, 214))
    for n in range(5):
        z = -95.5 + n * 2.7
        for sx in (-1, 1):
            seg("head", sx * 3.9 - 0.6, 43.6, z, 1.2, 2.6, 1.5, tooth)
            seg("jaw", sx * 4.2 - 0.6, 46.6, z + 1.2, 1.2, 2.4, 1.5, tooth)
    for sx in (-1, 1):
        seg("head", sx * 3.2 - 0.9, 41.0, -97.6, 1.8, 4.6, 1.8, tooth)                                        # the upper fangs
        seg("jaw", sx * 3.0 - 0.8, 46.4, -96.0, 1.6, 4.0, 1.6, tooth)                                         # the lower fangs
    for sx in (1, -1):
        seg("head", sx * 5.6 - 2.0, 57.0, -84, 4.0, 2.2, 9.0, claw_paint(52))                                  # the brow ridge
        b.spike("head", (sx * 5.0, 57.0, -74), 4.2, 28.0, 52, -sx * 14, claw_paint(53), key="horn", fracs=(0.4, 0.34, 0.26), taper=0.72, bend=(8, sx * 4))
        b.spike("head", (sx * 6.8, 52.0, -72), 3.2, 14.0, 34, -sx * 62, claw_paint(54), key="cheek", fracs=(0.55, 0.45), taper=0.6, bend=(4, -sx * 6))
        b.spike("head", (sx * 6.6, 47.0, -76), 2.6, 9.0, 20, -sx * 80, claw_paint(54), key="cheek2", fracs=(0.55, 0.45), taper=0.6)
        seg("head", sx * 7.0 - (0.0 if sx > 0 else 0.6), 51.4, -82.0, 0.6, 3.2, 4.0, eye_paint())                # the eye
        seg("head", sx * 7.1 - (0.0 if sx > 0 else 0.4), 55.2, -82.6, 0.4, 1.0, 5.2, solid((10, 28, 30)))         # the lid line
        seg("head", sx * 2.0 - 0.8, 50.0, -98.4, 1.6, 1.6, 0.6, solid((10, 20, 24)))                              # the nostril
        b.spike("jaw", (sx * 5.2, 44.4, -90.0), 1.2, 22.0, 80, -sx * 22, claw_paint(60), key="barb", fracs=(0.34, 0.33, 0.33), taper=0.8, bend=(-8, sx * 6))   # the barbels
    for k in range(3):
        b.spike("head", (0, 57.4, -82 + k * 4), 2.4, 6.0 - k, 30, 0, claw_paint(55), key="crest", fracs=(0.55, 0.45), taper=0.6)
    # ----- the tail: five tapering segments, a cream underside, spikes and a blade at the tip
    for k, (w, h) in enumerate(((14, 15), (11, 12), (8.5, 9), (6, 6.5), (4, 4.5))):
        seg("tail%d" % (k + 1), -w / 2, 46 - h / 2, TAIL_Z[k], w, h, 16, body_paint(30 + k, belly=0.5, cw=3 if k < 3 else 2))
        if k < 4:
            b.spike("tail%d" % (k + 1), (0, 46 + h / 2 - 0.3, TAIL_Z[k] + 8), 2.8 - 0.3 * k, 7.0 - k, 18, 0, bonep, key="ts%d" % k, fracs=(0.55, 0.45), taper=0.6)
    b.spike("tail5", (0, 46, TAIL_Z[4] + 15), 4.0, 15.0, 90, 0, bonep, key="tip", fracs=(0.5, 0.3, 0.2), taper=0.7)
    seg("tail5", -0.8, 40.5, TAIL_Z[4] + 6, 1.6, 11, 9, claw_paint(56))                                            # the blade
    # ----- the wings: arm bones, thumb claw, four fingers, and the membrane panels between them
    for sg_name, suf in (("l", "_l"), ("r", "_r")):
        pass
    b.pair("wing_l", "wing_r", (11, 51.0, -11), (24, 6, 6), bonep)
    b.pair("wing2_l", "wing2_r", (35, 52.0, -10.5), (18, 5, 5), bonep)
    b.pair("wing2_l", "wing2_r", (53, 52.7, -9.8), (17, 3.6, 3.6), bonep)
    b.spike("wing2_l", (69, 55.0, -8), 3.2, 8.0, -40, 0, CL, key="thumb", fracs=(0.6, 0.4), taper=0.55)
    b.spike("wing2_r", (-69, 55.0, -8), 3.2, 8.0, -40, 0, CL, key="thumb", fracs=(0.6, 0.4), taper=0.55)
    mem = [membrane_paint(60 + k) for k in range(8)]
    for n, (phi, ln) in enumerate(FING):
        nxt = FING[n + 1][0] if n + 1 < len(FING) else phi + 55
        gap = math.radians(nxt - phi)
        b.pair("wf%d_l" % (n + 1), "wf%d_r" % (n + 1), (69, 53.1, -9.4), (ln, 2.8, 2.8), bonep)
        b.pair("wf%d_l" % (n + 1), "wf%d_r" % (n + 1), (69 + ln - 2.0, 53.1 + 0.4, -9.4 - 0.4), (5.0, 2.0, 3.6), CL)
        for s in range(3):
            r0 = ln * s / 3.0
            wd = max(2.0, (ln * (s + 0.5) / 3.0) * gap * 0.97)
            b.pair("wf%d_l" % (n + 1), "wf%d_r" % (n + 1), (69 + r0, 54.2 + 0.06 * n, -8 + 1.3), (ln / 3.0 + 0.8, 0.6, wd), mem[(n + s) % 8])
    b.pair("wing_l", "wing_r", (12, 54.3, -5.5), (23, 0.6, 27), mem[5])
    b.pair("wing2_l", "wing2_r", (35, 54.45, -5.5), (34, 0.6, 29), mem[6])
    # ----- legs: shoulder / hip, shin, a paw with three clawed toes
    for suf, sg in (("_l", 1), ("_r", -1)):
        pass
    b.pair("fleg_l", "fleg_r", (4.5, 18, -16.5), (9, 20, 9), limb_paint(70))
    b.pair("fleg2_l", "fleg2_r", (5.5, 1, -15.5), (7, 17, 7), limb_paint(71, lift=-0.04))
    b.pair("ffoot_l", "ffoot_r", (5.0, -4, -26), (8, 5, 17), limb_paint(72, lift=-0.06))
    b.pair("hleg_l", "hleg_r", (4.0, 18, 8.0), (12, 20, 12), limb_paint(73))
    b.pair("hleg2_l", "hleg2_r", (5.5, -2, 10.5), (9, 20, 7), limb_paint(74, lift=-0.04))
    b.pair("hfoot_l", "hfoot_r", (5.5, -7, -2.0), (9, 5, 20), limb_paint(75, lift=-0.06))
    for pre, px0, zt, bonepre in (("f", 9, -26, "ffoot"), ("h", 10, -2.0, "hfoot")):
        for n, dx in enumerate((-2.6, 0.0, 2.6)):
            for suf, sg in (("_l", 1), ("_r", -1)):
                b.spike(bonepre + suf, (sg * px0 + dx, -4.0 if pre == "f" else -7.0 + 1.0, zt), 2.4, 6.5, -96, 0, CL, key=pre + "cl%d" % n, fracs=(0.6, 0.4), taper=0.55)

    clips_dragon(A)
    return b


def clips_dragon(a):
    S, C = SIN, COS
    NECK = ("neck1", "neck2", "neck3", "neck4")
    TAIL = ("tail1", "tail2", "tail3", "tail4", "tail5")

    def wings(clip, L, amp, ph=0.0, base=(0, 0, 0), lag=(0.9, 1.6)):
        for suf, sg in (("_l", 1), ("_r", -1)):
            cyc(a, clip, "wing" + suf, "rot", L, lambda t, sg=sg: (base[0], base[1], sg * (base[2] + amp * S(t + ph))))
            cyc(a, clip, "wing2" + suf, "rot", L, lambda t, sg=sg: (0, 0, sg * (-0.55 * amp * S(t + ph - lag[0]) + base[2] * -0.3)))
            for n in range(4):
                cyc(a, clip, "wf%d%s" % (n + 1, suf), "rot", L, lambda t, sg=sg, n=n: (0, 0, sg * (-0.3 * amp * S(t + ph - lag[1] - 0.15 * n))))

    # idle (loop 4 s): breathing, a slow wing lift, the neck swaying, the tail trailing
    L = 4.0
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.7 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.6, 0.8, 0.4), 0.5))
    for k, bn in enumerate(NECK):
        cyc(a, "idle", bn, "rot", L, swing((0, 0, 0), (1.6 - 0.2 * k, 2.4, 0.4), 0.6 + 0.5 * k))
    cyc(a, "idle", "head", "rot", L, swing((0, 0, 0), (2.0, 2.4, 0.5), 1.8))
    cyc(a, "idle", "jaw", "rot", L, lambda t: (-3.5 - 2.5 * S(2 * t), 0, 0))
    for k, bn in enumerate(TAIL):
        cyc(a, "idle", bn, "rot", L, swing((0, 0, 0), (0.6, 4.0 + k, 0), 0.4 + 0.7 * k))
    wings("idle", L, 6.0)
    for suf, sg in (("_l", 1), ("_r", -1)):
        cyc(a, "idle", "fleg" + suf, "rot", L, swing((0, 0, 0), (0.8, 0, 0), 0.3))

    # walk (loop 1.6 s)
    L = 1.6
    a.clip("walk", L, True)
    for suf, sg in (("_l", 1), ("_r", -1)):
        ph_f = 0.0 if sg > 0 else math.pi
        ph_h = math.pi if sg > 0 else 0.0
        cyc(a, "walk", "fleg" + suf, "rot", L, lambda t, ph=ph_f: (20 * S(t + ph), 0, 0))
        cyc(a, "walk", "fleg2" + suf, "rot", L, lambda t, ph=ph_f: (16 * max(0.0, S(t + ph + 1.2)), 0, 0))
        cyc(a, "walk", "ffoot" + suf, "rot", L, lambda t, ph=ph_f: (-10 * max(0.0, S(t + ph + 1.2)), 0, 0))
        cyc(a, "walk", "hleg" + suf, "rot", L, lambda t, ph=ph_h: (18 * S(t + ph), 0, 0))
        cyc(a, "walk", "hleg2" + suf, "rot", L, lambda t, ph=ph_h: (-18 * max(0.0, S(t + ph + 1.2)), 0, 0))
        cyc(a, "walk", "hfoot" + suf, "rot", L, lambda t, ph=ph_h: (12 * max(0.0, S(t + ph + 1.2)), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 1.0 * C(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (0.8 * S(2 * t), 3.0 * S(t), 2.0 * S(t)))
    for k, bn in enumerate(NECK):
        cyc(a, "walk", bn, "rot", L, lambda t, k=k: (1.4 * S(2 * t + 0.4 * k), -3.5 * S(t + 0.5 * k), 0))
    cyc(a, "walk", "head", "rot", L, lambda t: (1.5 * S(2 * t + 1.2), -3 * S(t + 2.0), 0))
    cyc(a, "walk", "jaw", "rot", L, lambda t: (-3.0, 0, 0))
    for k, bn in enumerate(TAIL):
        cyc(a, "walk", bn, "rot", L, lambda t, k=k: (0.8 * S(2 * t + 0.6 * k), 8.0 * S(t - 0.55 * k), 0))
    wings("walk", L, 5.0, base=(0, 0, 2.0))

    # run (loop 0.9 s): a flapping, bounding gallop
    L = 0.9
    a.clip("run", L, True)
    for suf, sg in (("_l", 1), ("_r", -1)):
        ph_f = 0.0 if sg > 0 else 0.5
        cyc(a, "run", "fleg" + suf, "rot", L, lambda t, ph=ph_f: (32 * S(t + ph), 0, 0))
        cyc(a, "run", "fleg2" + suf, "rot", L, lambda t, ph=ph_f: (26 * max(0.0, S(t + ph + 1.2)), 0, 0))
        cyc(a, "run", "hleg" + suf, "rot", L, lambda t, ph=ph_f: (-30 * S(t + ph), 0, 0))
        cyc(a, "run", "hleg2" + suf, "rot", L, lambda t, ph=ph_f: (-22 * max(0.0, S(t + ph + 1.2)), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 3.0 * C(t), 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-4 + 3 * S(t), 2 * S(t), 0))
    for k, bn in enumerate(NECK):
        cyc(a, "run", bn, "rot", L, lambda t, k=k: (-3.0 + 2.5 * S(t + 0.4 * k), 0, 0))
    cyc(a, "run", "head", "rot", L, lambda t: (-6 + 3 * S(t + 1.0), 0, 0))
    cyc(a, "run", "jaw", "rot", L, lambda t: (-8 - 4 * S(t), 0, 0))
    for k, bn in enumerate(TAIL):
        cyc(a, "run", bn, "rot", L, lambda t, k=k: (-3.0 + 2 * S(t - 0.5 * k), 8.0 * S(t - 0.6 * k), 0))
    wings("run", L, 24.0, base=(0, 0, 8.0))

    # attack (one-shot 1.1 s): the neck coils back, then the head lunges and the jaws snap shut
    L = 1.1
    a.clip("attack", L, False)
    T = (0.0, 0.34, 0.5, 0.7, 1.1)
    seq(a, "attack", "rot", T,
        neck1=[(0, 0, 0), (16, 0, 0), (-26, 0, 0), (-8, 0, 0), (0, 0, 0)],
        neck2=[(0, 0, 0), (12, 0, 0), (-20, 0, 0), (-6, 0, 0), (0, 0, 0)],
        neck3=[(0, 0, 0), (8, 0, 0), (-14, 0, 0), (-4, 0, 0), (0, 0, 0)],
        neck4=[(0, 0, 0), (6, 0, 0), (-10, 0, 0), (-3, 0, 0), (0, 0, 0)],
        head=[(0, 0, 0), (14, 0, 0), (-16, 0, 0), (-4, 0, 0), (0, 0, 0)],
        jaw=[(0, 0, 0), (-42, 0, 0), (-50, 0, 0), (2, 0, 0), (0, 0, 0)],
        body=[(0, 0, 0), (-5, 0, 0), (6, 0, 0), (2, 0, 0), (0, 0, 0)],
        wing_l=[(0, 0, 0), (0, 0, 14), (0, 0, -6), (0, 0, 4), (0, 0, 0)],
        wing_r=[(0, 0, 0), (0, 0, -14), (0, 0, 6), (0, 0, -4), (0, 0, 0)],
        tail1=[(0, 0, 0), (0, 8, 0), (0, -10, 0), (0, 4, 0), (0, 0, 0)],
        tail3=[(0, 0, 0), (0, 14, 0), (0, -18, 0), (0, 6, 0), (0, 0, 0)])

    # cast (loop 1.8 s): the head raised, the jaws open on a held roar, the wings lifted
    L = 1.8
    a.clip("cast", L, True)
    for k, bn in enumerate(NECK):
        cyc(a, "cast", bn, "rot", L, lambda t, k=k: (8 - 2 * k + 1.5 * S(t + 0.4 * k), 0, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (16 + 2 * S(t + 0.8), 0, 0))
    cyc(a, "cast", "jaw", "rot", L, lambda t: (-26 - 5 * S(2 * t), 0, 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (4 + 0.8 * S(t), 0, 0))
    cyc(a, "cast", "body", "pos", L, lambda t: (0, 1.2 + 0.5 * S(t), 0))
    for k, bn in enumerate(TAIL):
        cyc(a, "cast", bn, "rot", L, lambda t, k=k: (-2.0, 6.0 * S(t - 0.5 * k), 0))
    wings("cast", L, 5.0, base=(0, 0, 16.0))

    # cast_big (loop 2.4 s): the dragon rears, the wings flung wide and beating, a full roar
    L = 2.4
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "body", "rot", L, lambda t: (14 + 1.5 * S(t), 0, 0))
    cyc(a, "cast_big", "body", "pos", L, lambda t: (0, 8.0 + 1.5 * S(t), 0))
    for k, bn in enumerate(NECK):
        cyc(a, "cast_big", bn, "rot", L, lambda t, k=k: (6 + 2 * S(t + 0.5 * k), 0, 0))
    cyc(a, "cast_big", "head", "rot", L, lambda t: (20 + 3 * S(t + 0.6), 0, 0))
    cyc(a, "cast_big", "jaw", "rot", L, lambda t: (-48 - 4 * S(2 * t), 0, 0))
    for suf in ("_l", "_r"):
        cyc(a, "cast_big", "fleg" + suf, "rot", L, lambda t: (28 + 4 * S(t), 0, 0))
        cyc(a, "cast_big", "fleg2" + suf, "rot", L, lambda t: (-20, 0, 0))
        cyc(a, "cast_big", "hleg" + suf, "rot", L, lambda t: (-6, 0, 0))
    for k, bn in enumerate(TAIL):
        cyc(a, "cast_big", bn, "rot", L, lambda t, k=k: (-4.0, 5.0 * S(t - 0.5 * k), 0))
    wings("cast_big", L, 26.0, base=(0, 0, 12.0))

    # hurt (one-shot 0.5 s): the neck recoils, the jaws gape
    L = 0.5
    a.clip("hurt", L, False)
    T = (0.0, 0.1, 0.5)
    seq(a, "hurt", "rot", T, neck1=[(0, 0, 0), (-12, 0, 6), (0, 0, 0)], neck2=[(0, 0, 0), (-10, 0, 4), (0, 0, 0)], neck3=[(0, 0, 0), (-8, 0, 3), (0, 0, 0)],
        head=[(0, 0, 0), (14, 0, -6), (0, 0, 0)], jaw=[(0, 0, 0), (-36, 0, 0), (0, 0, 0)], body=[(0, 0, 0), (4, 0, 2), (0, 0, 0)],
        wing_l=[(0, 0, 0), (0, 0, 16), (0, 0, 0)], wing_r=[(0, 0, 0), (0, 0, -16), (0, 0, 0)],
        tail2=[(0, 0, 0), (0, 14, 0), (0, 0, 0)])

    # transform (one-shot 3.4 s): a shudder, the head thrown up in a roar, the wings flared, then everything slumps (the entity swaps to the Avatar at the end)
    L = 3.4
    a.clip("transform", L, False)
    T = (0.0, 0.6, 1.2, 1.8, 2.4, 3.0, 3.4)
    seq(a, "transform", "rot", T,
        neck1=[(0, 0, 0), (-6, 0, 0), (12, 2, 0), (18, -2, 0), (14, 2, 0), (4, 0, 0), (0, 0, 0)],
        neck2=[(0, 0, 0), (-4, 0, 0), (10, -2, 0), (14, 2, 0), (10, -2, 0), (3, 0, 0), (0, 0, 0)],
        neck3=[(0, 0, 0), (-3, 0, 0), (8, 2, 0), (12, -2, 0), (8, 2, 0), (2, 0, 0), (0, 0, 0)],
        head=[(0, 0, 0), (-8, 0, 0), (26, 0, 0), (32, 0, 0), (26, 0, 0), (8, 0, 0), (0, 0, 0)],
        jaw=[(0, 0, 0), (-6, 0, 0), (-52, 0, 0), (-56, 0, 0), (-50, 0, 0), (-12, 0, 0), (0, 0, 0)],
        body=[(0, 0, 0), (-6, 0, 0), (12, 0, 0), (16, 0, 0), (12, 0, 0), (4, 0, 0), (0, 0, 0)],
        wing_l=[(0, 0, 0), (0, 0, 8), (0, 0, 34), (0, 0, 44), (0, 0, 30), (0, 0, 6), (0, 0, 0)],
        wing_r=[(0, 0, 0), (0, 0, -8), (0, 0, -34), (0, 0, -44), (0, 0, -30), (0, 0, -6), (0, 0, 0)],
        tail1=[(0, 0, 0), (0, 10, 0), (0, -14, 0), (0, 14, 0), (0, -10, 0), (0, 4, 0), (0, 0, 0)],
        tail3=[(0, 0, 0), (0, -16, 0), (0, 20, 0), (0, -20, 0), (0, 14, 0), (0, -6, 0), (0, 0, 0)])
    seq(a, "transform", "pos", T, body=[(0, 0, 0), (0, -1, 0), (0, 4, 0), (0, 6, 0), (0, 4, 0), (0, 1, 0), (0, 0, 0)])

    # death (one-shot 3.0 s, holds): the legs buckle, the neck falls, the wings droop
    L = 3.0
    a.clip("death", L, False)
    T = (0.0, 0.5, 1.2, 2.0, 3.0)
    seq(a, "death", "rot", T,
        neck1=[(0, 0, 0), (-10, 0, 0), (-30, 0, 4), (-44, 0, 6), (-48, 0, 6)],
        neck2=[(0, 0, 0), (-6, 0, 0), (-14, 6, 0), (-18, 10, 0), (-20, 10, 0)],
        neck3=[(0, 0, 0), (-4, 0, 0), (-10, 8, 0), (-14, 12, 0), (-14, 12, 0)],
        head=[(0, 0, 0), (10, 0, 0), (-8, 0, 0), (-22, 0, 0), (-26, 0, 0)],
        jaw=[(0, 0, 0), (-30, 0, 0), (-18, 0, 0), (-10, 0, 0), (-8, 0, 0)],
        body=[(0, 0, 0), (4, 0, 0), (-4, 0, 4), (-6, 0, 8), (-6, 0, 10)],
        fleg_l=[(0, 0, 0), (10, 0, 0), (34, 0, 0), (44, 0, 0), (44, 0, 0)], fleg_r=[(0, 0, 0), (10, 0, 0), (34, 0, 0), (44, 0, 0), (44, 0, 0)],
        hleg_l=[(0, 0, 0), (-8, 0, 0), (-26, 0, 0), (-34, 0, 0), (-34, 0, 0)], hleg_r=[(0, 0, 0), (-8, 0, 0), (-26, 0, 0), (-34, 0, 0), (-34, 0, 0)],
        wing_l=[(0, 0, 0), (0, 0, 14), (0, 0, -22), (0, 0, -44), (0, 0, -52)], wing_r=[(0, 0, 0), (0, 0, -14), (0, 0, 22), (0, 0, 44), (0, 0, 52)],
        tail1=[(0, 0, 0), (0, 6, 0), (0, 14, 0), (0, 20, 0), (0, 22, 0)], tail3=[(0, 0, 0), (0, 10, 0), (0, 24, 0), (0, 32, 0), (0, 34, 0)])
    seq(a, "death", "pos", T, body=[(0, 0, 0), (0, 1, 0), (0, -9, 0), (0, -17, 0), (0, -19, 0)])


# ------------------------------------------------------------------------------------------------------------------ animation (shared by the two humanoid forms)
CLIPS = ("idle", "walk", "run", "attack", "cast", "cast_big", "hurt", "transform", "death")
HAIR_CHAIN = ("hair_b1", "hair_b2", "hair_b3")
CLOTH = ("robe_l", "robe_r", "robe_back", "front_cloth", "sash_tail")


def clips_humanoid(a):
    S = SIN
    # idle (loop 4 s): the robe breathes, the hair and sleeves sway, the arms rest with the open palms a little forward
    L = 4.0
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.18 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.5, 1.2, 0.4), 0.6))
    cyc(a, "idle", "head", "rot", L, swing((2, 0, 0), (1.4, 3.5, 1.0), 0.7))
    cyc(a, "idle", "hair", "rot", L, swing((0, 0, 0), (1.2, 0, 1.0), 1.2))
    for n, bn in enumerate(HAIR_CHAIN):
        cyc(a, "idle", bn, "rot", L, swing((0, 0, 0), (1.6 + n * 0.8, 0, 0.7 + n * 0.5), 0.5 + n * 0.5))
    for sg, suf in ((1, "_l"), (-1, "_r")):
        cyc(a, "idle", "lock" + suf, "rot", L, swing((0, 0, 0), (1.2, 0, 1.0 * sg), 0.9))
        cyc(a, "idle", "arm" + suf, "rot", L, swing((6, 0, 8 * sg), (1.6, 0, 1.2 * sg), 0.4 + sg))
        cyc(a, "idle", "forearm" + suf, "rot", L, swing((14, 0, 0), (3.0, 0, 0), 1.0 + sg))
        cyc(a, "idle", "robe" + suf, "rot", L, swing((0, 0, 0), (1.0, 0, 0.6 * sg), 0.8 + sg))
        cyc(a, "idle", "skirt" + suf, "rot", L, swing((0, 0, 0), (0.6, 0, 0.4 * sg), 0.3))
    cyc(a, "idle", "robe_back", "rot", L, swing((0, 0, 0), (1.4, 0, 0), 1.2))
    cyc(a, "idle", "front_cloth", "rot", L, swing((0, 0, 0), (1.2, 0, 0.5), 2.0))
    cyc(a, "idle", "sash_tail", "rot", L, swing((0, 0, 0), (2.0, 0, 1.2), 1.5))
    for suf in ("_l", "_r"):
        cyc(a, "idle", "ring" + suf, "rot", L, swing((0, 0, 0), (0, 0, 0), 0))

    # walk (loop 1.1 s)
    L = 1.1
    a.clip("walk", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "walk", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (24 * S(t + ph), 0, 1.0 * sg))
        cyc(a, "walk", "skirt" + sd, "rot", L, lambda t, ph=ph, sg=sg: (13 * S(t + ph), 0, 0.8 * sg))
        cyc(a, "walk", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-18 * S(t + ph) + 6, 0, 8 * sg))
        cyc(a, "walk", "forearm" + sd, "rot", L, lambda t, ph=ph: (14 + 8 * max(0.0, S(t + ph)), 0, 0))
        cyc(a, "walk", "robe" + sd, "rot", L, lambda t, ph=ph: (-5 + 6 * S(t + ph + 0.8), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.4 * COS(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (-1.5, 2.5 * S(t), 0.8 * S(t)))
    cyc(a, "walk", "head", "rot", L, lambda t: (2 + 0.8 * S(2 * t + 1.6), -2 * S(t), 0))
    cyc(a, "walk", "hair", "rot", L, lambda t: (3 * S(2 * t + 0.4), 0, 1.0 * S(t)))
    for n, bn in enumerate(HAIR_CHAIN):
        cyc(a, "walk", bn, "rot", L, lambda t, n=n: (-6 - 4 * n + 3 * S(2 * t + 0.4 + n * 0.7), 0, 1.5 * S(t + n * 0.6)))
    cyc(a, "walk", "robe_back", "rot", L, lambda t: (-6 + 3 * S(2 * t + 0.5), 0, 1.2 * S(t)))
    cyc(a, "walk", "front_cloth", "rot", L, lambda t: (5 + 5 * S(2 * t + 0.3), 0, 0))
    cyc(a, "walk", "sash_tail", "rot", L, lambda t: (-8 + 5 * S(2 * t + 1.0), 0, 2 * S(t)))

    # run (loop 0.62 s)
    L = 0.62
    a.clip("run", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "run", "leg" + sd, "rot", L, lambda t, ph=ph, sg=sg: (42 * S(t + ph), 0, 1.5 * sg))
        cyc(a, "run", "skirt" + sd, "rot", L, lambda t, ph=ph, sg=sg: (28 * S(t + ph), 0, 1.5 * sg))
        cyc(a, "run", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-40 * S(t + ph) + 10, 0, 12 * sg))
        cyc(a, "run", "forearm" + sd, "rot", L, lambda t, ph=ph: (44 + 24 * max(0.0, S(t + ph)), 0, 0))
        cyc(a, "run", "robe" + sd, "rot", L, lambda t, ph=ph: (-16 + 8 * S(t + ph + 0.8), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 0.9 * COS(2 * t) + 0.2, 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-12 + 1.5 * S(2 * t), 4.5 * S(t), 1.2 * S(t)))
    cyc(a, "run", "head", "rot", L, lambda t: (9 + 1.5 * S(2 * t + 1.6), -4 * S(t), 0))
    cyc(a, "run", "hair", "rot", L, lambda t: (-14 + 4 * S(2 * t + 0.4), 0, 2 * S(t)))
    for n, bn in enumerate(HAIR_CHAIN):
        cyc(a, "run", bn, "rot", L, lambda t, n=n: (-18 - 10 * n + 5 * S(2 * t + 0.4 + n * 0.8), 0, 3 * S(t + n * 0.6)))
    cyc(a, "run", "robe_back", "rot", L, lambda t: (-24 + 6 * S(2 * t + 0.5), 0, 2 * S(t)))
    cyc(a, "run", "front_cloth", "rot", L, lambda t: (14 + 6 * S(2 * t + 0.3), 0, 0))
    cyc(a, "run", "sash_tail", "rot", L, lambda t: (-26 + 8 * S(2 * t + 1.0), 0, 4 * S(t)))
    cyc(a, "run", "lock_l", "rot", L, lambda t: (-10 + 3 * S(2 * t), 0, 2))
    cyc(a, "run", "lock_r", "rot", L, lambda t: (-10 + 3 * S(2 * t + 1), 0, -2))

    # attack (one-shot 0.8 s): a sweep of the right sleeve across the body
    L = 0.8
    a.clip("attack", L, False)
    T = (0.0, 0.22, 0.36, 0.5, 0.8)
    seq(a, "attack", "rot", T,
        arm_r=[(6, 0, -8), (-70, -14, -30), (92, 10, -8), (60, 4, -8), (6, 0, -8)],
        forearm_r=[(14, 0, 0), (60, 0, 0), (6, 0, 0), (18, 0, 0), (14, 0, 0)],
        arm_l=[(6, 0, 8), (-24, 0, 16), (-14, 0, 14), (-4, 0, 10), (6, 0, 8)],
        body=[(0, 0, 0), (6, 14, 0), (-14, -16, 0), (-8, -8, 0), (0, 0, 0)],
        head=[(2, 0, 0), (3, 10, 0), (-6, -9, 0), (-3, -4, 0), (2, 0, 0)],
        leg_l=[(0, 0, 0), (-8, 0, 0), (24, 0, 0), (12, 0, 0), (0, 0, 0)],
        leg_r=[(0, 0, 0), (10, 0, 0), (-18, 0, 0), (-8, 0, 0), (0, 0, 0)],
        hair_b1=[(0, 0, 0), (12, 0, 0), (-22, 0, 4), (-8, 0, 0), (0, 0, 0)],
        hair_b2=[(0, 0, 0), (16, 0, 0), (-30, 0, 6), (-12, 0, 0), (0, 0, 0)],
        hair_b3=[(0, 0, 0), (20, 0, 0), (-38, 0, 8), (-14, 0, 0), (0, 0, 0)],
        robe_back=[(0, 0, 0), (8, 0, 0), (-20, 0, 0), (-6, 0, 0), (0, 0, 0)])

    # cast (loop 1.6 s): the right hand raised palm-up, the left held low, the hair lifted
    L = 1.6
    a.clip("cast", L, True)
    cyc(a, "cast", "arm_r", "rot", L, lambda t: (-70 + 3 * S(t), 0, -22 - 2 * S(t + 0.5)))
    cyc(a, "cast", "forearm_r", "rot", L, lambda t: (60 + 6 * S(t + 0.3), 0, 0))
    cyc(a, "cast", "arm_l", "rot", L, lambda t: (10 + 2 * S(t + 1), 0, 12))
    cyc(a, "cast", "forearm_l", "rot", L, lambda t: (22 + 3 * S(t + 1.4), 0, 0))
    cyc(a, "cast", "body", "rot", L, lambda t: (-2 + 0.8 * S(t), -6, 0))
    cyc(a, "cast", "head", "rot", L, lambda t: (-8 + 1.5 * S(t + 0.4), 4, 0))
    for n, bn in enumerate(HAIR_CHAIN):
        cyc(a, "cast", bn, "rot", L, lambda t, n=n: (-8 - 4 * n + 3 * S(t + 0.6 * n), 0, 2 * S(t + n)))
    cyc(a, "cast", "robe_back", "rot", L, lambda t: (-10 + 3 * S(t + 1.0), 0, 1.5 * S(t)))
    cyc(a, "cast", "root", "pos", L, lambda t: (0, 0.8 + 0.5 * S(t), 0))

    # cast_big (loop 2.0 s): both arms thrown wide, rising, hair and robe streaming up
    L = 2.0
    a.clip("cast_big", L, True)
    cyc(a, "cast_big", "arm_l", "rot", L, lambda t: (-6 + 3 * S(t), 0, 72 + 4 * S(t + 0.4)))
    cyc(a, "cast_big", "arm_r", "rot", L, lambda t: (-6 + 3 * S(t + 1), 0, -72 - 4 * S(t + 1.4)))
    cyc(a, "cast_big", "forearm_l", "rot", L, lambda t: (12 + 4 * S(t + 0.6), 0, 0))
    cyc(a, "cast_big", "forearm_r", "rot", L, lambda t: (12 + 4 * S(t + 1.6), 0, 0))
    cyc(a, "cast_big", "root", "pos", L, lambda t: (0, 5.0 + 1.0 * S(t), 0))
    cyc(a, "cast_big", "body", "rot", L, lambda t: (-5 + 1.0 * S(t + 0.5), 0, 0))
    cyc(a, "cast_big", "head", "rot", L, lambda t: (-14 + 2 * S(t + 0.4), 0, 0))
    cyc(a, "cast_big", "hair", "rot", L, lambda t: (-20 + 4 * S(t + 0.6), 0, 2 * S(t)))
    for n, bn in enumerate(HAIR_CHAIN):
        cyc(a, "cast_big", bn, "rot", L, lambda t, n=n: (-24 - 10 * n + 5 * S(t + 0.7 * n), 0, 4 * S(t + n)))
    cyc(a, "cast_big", "robe_back", "rot", L, lambda t: (-30 + 5 * S(t + 1.0), 0, 2 * S(t)))
    for suf in ("_l", "_r"):
        cyc(a, "cast_big", "robe" + suf, "rot", L, lambda t: (-18 + 4 * S(t), 0, 0))
        cyc(a, "cast_big", "leg" + suf, "rot", L, lambda t: (-8, 0, 0))
        cyc(a, "cast_big", "skirt" + suf, "rot", L, lambda t: (-12 + 3 * S(t), 0, 0))
    cyc(a, "cast_big", "front_cloth", "rot", L, lambda t: (-18 + 4 * S(t + 0.5), 0, 0))
    cyc(a, "cast_big", "sash_tail", "rot", L, lambda t: (-36 + 6 * S(t + 0.5), 0, 3 * S(t)))

    # hurt (one-shot 0.45 s)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    seq(a, "hurt", "rot", T, body=[(0, 0, 0), (10, 0, 3), (0, 0, 0)], head=[(2, 0, 0), (14, 6, 0), (2, 0, 0)],
        arm_l=[(6, 0, 8), (-18, 0, 22), (6, 0, 8)], arm_r=[(6, 0, -8), (-22, 0, -22), (6, 0, -8)],
        hair_b1=[(0, 0, 0), (14, 0, 0), (0, 0, 0)], hair_b2=[(0, 0, 0), (20, 0, 0), (0, 0, 0)], hair_b3=[(0, 0, 0), (26, 0, 0), (0, 0, 0)])
    seq(a, "hurt", "pos", T, body=[(0, 0, 0), (0, 0, 1.6), (0, 0, 0)])

    # transform (one-shot 3.2 s): rises, arms wide, a deep breath; ends on the pose of the next form's idle
    L = 3.2
    a.clip("transform", L, False)
    T = (0.0, 0.5, 1.0, 1.5, 2.2, 2.8, 3.2)
    seq(a, "transform", "rot", T,
        arm_l=[(6, 0, 8), (24, 0, 14), (10, 0, 48), (6, 0, 72), (6, 0, 78), (10, 0, 56), (6, 0, 8)],
        arm_r=[(6, 0, -8), (24, 0, -14), (10, 0, -48), (6, 0, -72), (6, 0, -78), (10, 0, -56), (6, 0, -8)],
        body=[(0, 0, 0), (-8, 0, 0), (4, 0, 0), (8, 0, 0), (8, 0, 0), (4, 0, 0), (0, 0, 0)],
        head=[(2, 0, 0), (-14, 0, 0), (-8, 0, 0), (-18, 0, 0), (-18, 0, 0), (-8, 0, 0), (2, 0, 0)],
        hair_b1=[(0, 0, 0), (8, 0, 0), (-24, 0, 4), (-32, 0, -4), (-32, 0, 4), (-10, 0, 0), (0, 0, 0)],
        hair_b2=[(0, 0, 0), (10, 0, 0), (-32, 0, -6), (-42, 0, 7), (-42, 0, -6), (-14, 0, 0), (0, 0, 0)],
        hair_b3=[(0, 0, 0), (12, 0, 0), (-40, 0, 6), (-52, 0, -8), (-52, 0, 8), (-18, 0, 0), (0, 0, 0)],
        robe_back=[(0, 0, 0), (6, 0, 0), (-26, 0, 0), (-38, 0, 0), (-38, 0, 0), (-12, 0, 0), (0, 0, 0)])
    seq(a, "transform", "pos", T, root=[(0, 0, 0), (0, 0, 0), (0, 3, 0), (0, 8, 0), (0, 10, 0), (0, 9, 0), (0, 0, 0)])

    # death (one-shot 2.8 s, holds): stagger, kneel, topple onto the side
    L = 2.8
    a.clip("death", L, False)
    T = (0.0, 0.35, 0.95, 1.5, 2.2, 2.8)
    seq(a, "death", "rot", T,
        arm_l=[(6, 0, 8), (-30, 0, 16), (30, 0, 12), (38, 0, 10), (-8, 0, 30), (-8, 0, 30)],
        arm_r=[(6, 0, -8), (-30, 0, -16), (30, 0, -12), (38, 0, -10), (-8, 0, -30), (-8, 0, -30)],
        body=[(0, 0, 0), (14, 0, 4), (-8, 0, 0), (-22, 0, 0), (-4, 0, 0), (-4, 0, 0)],
        head=[(2, 0, 0), (20, 0, 0), (-8, 0, 0), (-34, 0, 4), (-6, 0, 10), (-6, 0, 10)],
        leg_l=[(0, 0, 0), (-10, 0, 0), (62, 0, 5), (80, 0, 6), (6, 0, 0), (6, 0, 0)],
        leg_r=[(0, 0, 0), (10, 0, 0), (-70, 0, -5), (-74, 0, -6), (6, 0, 0), (6, 0, 0)],
        hair_b1=[(0, 0, 0), (-12, 0, 0), (4, 0, 0), (14, 0, 0), (10, 0, 0), (10, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 84), (0, 0, 84)])
    seq(a, "death", "pos", T, body=[(0, 0, 0), (0, 0, 2.0), (0, -7.4, 0), (0, -8.9, 0), (0, -11.4, 0), (0, -11.4, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-3, 1, 0), (-3, 3, 0)])


def prune_clips(b):
    """A clip may only name bones the model has (the loader and the check both insist)."""
    have = set(b.m.bones)
    for c in b.A.clips.values():
        for bn in [x for x in c["bones"] if x not in have]:
            del c["bones"][bn]


def run_build(b):
    prune_clips(b)
    img, glow = b.finish()
    return b.save(img, glow)


MODELS = ("humanoid", "dark_avatar", "true_dragon")
BUILDERS = {"humanoid": build_humanoid, "dark_avatar": build_avatar, "true_dragon": build_dragon}
TOWER = {"humanoid": 2.0, "dark_avatar": 2.0, "true_dragon": 5.0}
DIST = {"true_dragon": 4.4}
LOOK = {"humanoid": 1.0, "dark_avatar": 1.1, "true_dragon": 2.4}


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(400, 560)):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile),
            "floor": True, "figure": False, "expect": [], "anim": anim, "views": views}


def scenes_for(models=MODELS):
    sc = {}
    for m in models:
        h, ty = TOWER[m], LOOK[m]
        d = h * DIST.get(m, 2.0)
        full = "veldanava_" + m
        sc[m + "_front"] = scene(full, "idle", [0], [view("front", 0, 4, (0, ty, 0), d), view("front 3/4", 35, 8, (0, ty, 0), d)])
        sc[m + "_back"] = scene(full, "idle", [0], [view("back", 180, 5, (0, ty, 0), d), view("side", 90, 4, (0, ty, 0), d)])
        sc[m + "_face"] = scene(full, "idle", [0], [view("face", 0, 2, (0, h * 0.88, 0), 2.2), view("face 3/4", 32, 6, (0, h * 0.88, 0), 2.2)], (420, 420))
        if m == "true_dragon":
            sc[m + "_head"] = scene(full, "idle", [0], [view("head 3/4", 35, 8, (0, 4.7, 4.9), 3.6), view("head side", 90, 4, (0, 4.7, 4.9), 3.6)], (420, 420))
        for clip, secs, n in (("cast", 1.2, 4), ("cast_big", 2.0, 4), ("attack", 0.8, 5), ("transform", 3.2, 6), ("death", 2.8, 5), ("walk", 1.1, 4), ("run", 0.62, 4)):
            ages = [round(secs * 20.0 * k / n, 1) for k in range(n)]
            wide = 1.35 if clip in ("cast_big", "transform") else 1.05
            sc["%s_%s" % (m, clip)] = scene(full, clip, ages, [view("3/4", 30, 6, (0, ty + (0.25 if wide > 1.2 else 0.0), 0), d * wide)], (340, 480))
    return sc


def write_scenes(path=SCENES_OUT):
    with open(path, "w") as f:
        json.dump(scenes_for(), f, indent=1)


def render_previews(names=None):
    import shutil
    import subprocess
    sc = scenes_for()
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp_veldanava"))
    os.makedirs(PREVIEW_OUT, exist_ok=True)
    r = subprocess.run([sys.executable, "-B", os.path.join(HERE, "render_preview", "preview.py")] + names, cwd=REPO, env=env, capture_output=True, text=True)
    for l in (r.stdout + r.stderr).splitlines():
        if ("WARNING" in l or "ERROR" in l or "error" in l or "scenes:" in l) and "Picked up" not in l:
            print(l)
    for n in names:
        src = os.path.join(env["RENDER_PREVIEW_BUILD"], n + ".png")
        if os.path.isfile(src):
            shutil.copy(src, os.path.join(PREVIEW_OUT, n + ".png"))
    return r.returncode


def main(argv):
    which = [a for a in argv if not a.startswith("-")]
    for name, fn in BUILDERS.items():
        if which and name not in which:
            continue
        run_build(fn())
    if not which:
        write_scenes()


if __name__ == "__main__":
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    main(sys.argv[1:])

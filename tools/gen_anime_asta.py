"""Phase E pilot 1 of the mob remake (docs/mobs/remake_plan.md): Asta, a hero NPC, in the "anime cel" style. A NEW model, next to the old arc_asta (which stays).

    python3 -B tools/gen_anime_asta.py              # writes geo / animation / texture of anime_asta and the preview scenes
    python3 -B tools/gen_anime_asta.py --preview    # renders tools/render_preview scenes into <ZOG_SCRATCH>/previews_anime_asta

Style (plan section 4): flat colour, one shadow band, a one-texel darker outline, few hues; hair is spike cards, the cape is two swinging pieces, the face is a plain
head plus five thin face plates (calm / grin / shout / hurt / blink) that the clips switch on and off by scale. About 4 heads tall (a 8 px head on a 33 px body).
Clips: idle walk run attack talk wave hurt death. Rotation signs as in gen_veldanava.py: positive x swings a hanging limb FORWARD, negative x folds a knee back.
Look source: web summaries of the Black Clover wiki (the fandom host itself was not reachable from this session), see docs/mobs/phaseE_asta.md.
"""
import json
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from gen_zogratis_lucius import Build, seq, TINY  # noqa: E402
from zogratis_lucius_kit import SIN, COS, TAU, Canvas, cyc, swing, px, hsh, SIDES, edge_cut  # noqa: E402
import gen_zogratis_lucius as _lucius  # noqa: E402

_lucius.KEY = "anime"
REPO = os.path.abspath(os.path.join(HERE, ".."))
SCENES_OUT = os.path.join(HERE, "render_preview", "scenes.d", "anime_asta.json")
SCRATCH = os.environ.get("ZOG_SCRATCH", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad")
PREVIEW_OUT = os.path.join(SCRATCH, "previews_anime_asta")
MODEL = "anime_asta"
ONE = (1.0, 1.0, 1.0)
GONE = (TINY, TINY, TINY)

# ------------------------------------------------------------------------------------------------------------------ palette: (shadow, base, light) + outline
SKIN = ((214, 140, 114), (250, 202, 166), (255, 226, 196))
SKIN_L = (140, 80, 66)
HAIR = ((204, 180, 116), (240, 222, 158), (255, 248, 206))
HAIR_L = (104, 86, 48)
WHITE = ((176, 186, 214), (238, 242, 252), (255, 255, 255))
WHITE_L = (92, 100, 130)
NAVY = ((28, 38, 84), (46, 66, 136), (82, 108, 186))
NAVY_L = (14, 18, 46)
BLUE = ((34, 78, 132), (56, 120, 196), (110, 176, 238))
BLUE_L = (16, 36, 70)
BLACK = ((12, 12, 20), (28, 28, 42), (56, 56, 78))
BLACK_L = (4, 4, 8)
GOLD = ((156, 104, 30), (230, 180, 58), (255, 232, 128))
GOLD_L = (92, 56, 14)
BOOT = ((44, 30, 26), (86, 58, 44), (130, 94, 68))
BOOT_L = (22, 14, 12)
STEEL = ((70, 74, 96), (140, 146, 172), (214, 220, 240))
EYE_G = ((28, 110, 56), (46, 170, 76), (150, 232, 138))
BROW = (120, 98, 54)
MOUTH = (132, 40, 48)


# ------------------------------------------------------------------------------------------------------------------ painters
def cel(tones, line, band=0.0, hi=0, cut=None, salt=0, mark=None, right_dark=True, back_dark=False, outline_top=False):
    """Flat colour with one shadow band: the face decides the tone (top light, bottom dark, the -x side and optionally the back dark), the lowest `band` of a
    side face is the shadow band, the first `hi` texel rows are a light rim, and a one-texel outline runs round the face. `mark(face, i, j, fw, fh)` may return a
    (colour or None) to paint over the flat tone (trim, emblems); `cut` is an edge_cut edge string for ragged cloth."""
    sh, base, lt = tones

    def p(face, i, j, fw, fh):
        if cut and face in SIDES and edge_cut(face, i, j, fw, fh, cut, 3, salt, 2.0, 1.5):
            return None, None
        if face in ("top", "bottom"):
            edge = i == 0 or j == 0 or i == fw - 1 or j == fh - 1
        else:
            edge = i == 0 or i == fw - 1 or j == fh - 1 or (outline_top and j == 0)
        if edge:
            return px(line), None
        if face == "top":
            c = lt
        elif face == "bottom":
            c = sh
        else:
            c = base
            if (face == "right" and right_dark) or (face == "back" and back_dark):
                c = sh
            if band and j >= fh * (1.0 - band):
                c = sh
            if hi and j < hi:
                c = lt
        if mark:
            m = mark(face, i, j, fw, fh)
            if m is not None:
                c = m
        return px(c), None
    return p


def hair_cel(cut=None, salt=0, streak=True, tips=True):
    """Hair: flat ash-blond with a few light streaks near the root and dark tips; the outline runs along the sides and the bottom only."""
    sh, base, lt = HAIR

    def mark(face, i, j, fw, fh):
        if face in ("top", "bottom"):
            return lt if (i + j * 2 + salt) % 7 == 0 else None
        if streak and (i * 5 + salt * 3) % 7 == 0 and j < fh * 0.62:
            return lt
        if tips and j > fh * 0.72 and (i + salt) % 3 == 0:
            return sh
        return None
    return cel(HAIR, HAIR_L, cut=cut, salt=salt, mark=mark, right_dark=False)


def hair_tri(salt=0, down=False, lean=0):
    """A flat hair spike: a triangle card (the alpha cut IS the shape), outlined, a light streak on one side and the shadow tone on the other. `down` = tip at the
    bottom (bangs), else tip at the top. The faces other than front / back are not drawn."""
    sh, base, lt = HAIR

    def p(face, i, j, fw, fh):
        if face not in ("front", "back"):
            return px(base), None
        t = (j + 0.5) / fh
        k = (1.0 - t) if down else t                          # 0 at the tip .. 1 at the root
        half = (fw / 2.0) * (0.12 + 0.88 * k)
        d = abs(i + 0.5 - fw / 2.0 - lean * (1.0 - k))
        if d > half or (not down and j == 0 and d > 0.9) or (down and j == fh - 1 and d > 0.9):
            return None, None
        edge = half - d < 1.0 or (j == 0 if not down else j == fh - 1)
        if edge:
            return px(HAIR_L), None
        x = (i + 0.5 - fw / 2.0 - lean * (1.0 - k)) / max(0.5, half)       # -1 .. 1 across the spike
        if face == "back":
            x = -x
        if x > 0.28:
            return px(sh), None
        if x < -0.45 and k > 0.25:
            return px(lt), None
        return px(base), None
    return p


def skin_cel(band=0.0, salt=0):
    return cel(SKIN, SKIN_L, band=band, salt=salt)


def gold_cel(salt=0):
    return cel(GOLD, GOLD_L, salt=salt)


# ------------------------------------------------------------------------------------------------------------------ the face
def head_front_art():
    """The bare front of the head (8 px x 4 texels = 32 x 32 over the 8 px): skin, a soft cheek tone, a tiny nose. The eyes / brows / mouth are the face plates."""
    cv = Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            c = SKIN[1]
            if y < 8:
                c = SKIN[0]                                              # in the shadow of the headband and the bangs
            elif y >= 28:
                c = SKIN[0]                                              # under the chin line
            cv.put(x, y, c)
    for x in range(32):
        cv.put(x, 31, SKIN_L)
    for y in range(8, 32):
        cv.put(0, y, SKIN_L)
        cv.put(31, y, SKIN_L)
    cv.rect(15, 20, 16, 21, SKIN[0])                                    # nose
    cv.put(15, 22, SKIN_L)
    for (x0, x1) in ((4, 8), (23, 27)):                                  # a little blush
        cv.rect(x0, 22, x1, 23, (255, 168, 150))
    return cv


def head_paint():
    art = head_front_art()
    sk = skin_cel()
    hr = hair_cel(salt=3, streak=False)

    def p(face, i, j, fw, fh):
        if face == "front":
            return art.at(i, j)
        if face == "top":
            return hr(face, i, j, fw, fh)
        if face in ("left", "right", "back"):
            wy = 33.0 - j / 3.0
            if wy > 31.2 or (face == "back" and wy > 26.6):
                return hr(face, i, j, fw, fh)
        return sk(face, i, j, fw, fh)
    return p


def eye(cv, x0, y0, mood, inner_right):
    """One eye, 8 texels wide, in the plate canvas. `inner_right` is True for the eye on the viewer's left (its inner corner is on its right)."""
    k = 1 if inner_right else -1
    xs = range(x0, x0 + 8)
    mid = x0 + 3.5
    if mood == "blink":
        for n, x in enumerate(xs):
            dy = 1 if 1 <= n <= 6 else 0
            cv.put(x, y0 + 5 + dy, BLACK_L)
            cv.put(x, y0 + 6 + dy, BLACK[1]) if 2 <= n <= 5 else None
        return
    if mood == "hurt":
        for n in range(4):
            yy = y0 + 3 + n
            xx = x0 + (n if inner_right else 7 - n)
            cv.put(xx, yy, BLACK_L)
            cv.put(xx + k, yy, BLACK_L)
            yy2 = y0 + 8 - n
            cv.put(xx, yy2, BLACK_L)
            cv.put(xx + k, yy2, BLACK_L)
        return
    top = y0 + (1 if mood in ("calm", "grin") else 2)
    bot = y0 + (9 if mood != "grin" else 8)
    cv.ellipse(mid + 0.5, (top + bot) / 2.0 + 0.5, 4.0, (bot - top) / 2.0 + 0.8, (252, 252, 255))
    ih = 3.7 if mood != "shout" else 3.2
    cv.ellipse(mid + 0.5, (top + bot) / 2.0 + 1.0, 2.7, ih, EYE_G[1])
    cv.rect(int(mid) - 1, int(top + 1), int(mid) + 2, int(top + 2), EYE_G[0])
    cv.ellipse(mid + 0.5, (top + bot) / 2.0 + 1.2, 1.4, 1.9, (12, 42, 26))
    cv.rect(int(mid) - 2, int(top) + 2, int(mid) - 1, int(top) + 3, (255, 255, 255))
    cv.put(int(mid) + 2, int(top) + 5, (255, 255, 255))
    cv.rect(int(mid) - 1, bot - 1, int(mid) + 2, bot - 1, EYE_G[2])
    # the upper lid: a thick dark line, with the corner flick on the outer side
    for n, x in enumerate(xs):
        slope = 0
        if mood == "shout":
            slope = (n * 2 // 7) if inner_right else ((7 - n) * 2 // 7)
        cv.put(x, top - 1 + slope, BLACK_L)
        cv.put(x, top + slope, BLACK_L)
    outer = x0 - 1 if inner_right else x0 + 8
    cv.put(outer, top + (0 if mood != "shout" else 0), BLACK_L)
    if mood == "grin":
        for x in xs:
            cv.put(x, bot + 1, (214, 140, 114))


def brows(cv, mood):
    spans = ((2, 10), (17, 25))
    for idx, (x0, x1) in enumerate(spans):
        inner_right = idx == 0
        for n, x in enumerate(range(x0, x1)):
            t = n / 7.0 if inner_right else (7 - n) / 7.0           # 0 outer .. 1 inner
            if mood == "shout":
                y = 1 + int(round(t * 3))
            elif mood == "hurt":
                y = 3 - int(round(t * 2))
            elif mood == "grin":
                y = 2 - (1 if 2 <= n <= 5 else 0)
            else:
                y = 2 - (1 if t > 0.5 else 0) + (0 if mood != "blink" else -0)
            cv.put(x, y, BROW)
            cv.put(x, y + 1, BROW)


def mouth(cv, mood):
    if mood in ("calm", "blink"):
        for x in range(10, 18):
            cv.put(x, 16 + (1 if 11 <= x <= 16 else 0), MOUTH)
        cv.put(9, 15, MOUTH)
        cv.put(18, 15, MOUTH)
    elif mood == "grin":
        cv.rect(7, 14, 20, 15, (255, 255, 255))
        cv.rect(8, 16, 19, 16, MOUTH)
        cv.rect(9, 17, 18, 18, MOUTH)
        cv.rect(11, 18, 16, 18, (236, 110, 120))
        cv.put(6, 13, MOUTH)
        cv.put(21, 13, MOUTH)
        for x in range(7, 21, 3):
            cv.put(x, 15, (200, 206, 224))
        for x in range(7, 21):
            cv.put(x, 14, (255, 255, 255)) if (x % 3) else cv.put(x, 14, (200, 206, 224))
        cv.put(7, 16, MOUTH)
        cv.put(20, 16, MOUTH)
    elif mood == "shout":
        cv.rect(8, 14, 19, 14, (255, 255, 255))
        cv.rect(8, 15, 19, 18, MOUTH)
        cv.rect(9, 19, 18, 19, MOUTH)
        cv.rect(10, 18, 17, 18, (236, 110, 120))
        for x in (8, 19):
            cv.put(x, 14, MOUTH)
    else:                                                                # hurt: a wavy grimace
        for n, x in enumerate(range(9, 19)):
            cv.put(x, 17 + (n % 2), MOUTH)
        cv.rect(11, 16, 16, 16, (255, 255, 255))


def plate_art(mood):
    cv = Canvas(28, 20)
    brows(cv, mood)
    eye(cv, 2, 3, mood, True)
    eye(cv, 18, 3, mood, False)
    mouth(cv, mood)
    return cv


def plate_paint(mood):
    art = plate_art(mood)

    def p(face, i, j, fw, fh):
        c, g = art.at(i, j)
        return c, g
    return p


# ------------------------------------------------------------------------------------------------------------------ cloth with marks
def tunic_mark(face, i, j, fw, fh):
    """The white tunic: a V-neck of skin on the front, a centre seam, the belt line is a separate box."""
    if face == "front":
        cx = (fw - 1) / 2.0
        if j < 7 and abs(i - cx) <= (6 - j) * 0.9:
            return SKIN[1]
        if j < 8 and abs(abs(i - cx) - (6 - j) * 0.9 - 1) < 0.6:
            return None
        if 8 <= j and abs(i - cx) < 0.6:
            return WHITE[0]
    return None


def cape_mark(trim_sides=True, trim_bottom=False, emblem=None):
    def m(face, i, j, fw, fh):
        if face in ("front", "back"):
            if trim_sides and (i == 1 or i == fw - 2):
                return GOLD[1]
            if trim_bottom and j == fh - 2:
                return GOLD[1]
            if emblem is not None and face == "back":
                c, _ = emblem.at(i, j)
                if c is not None:
                    return c[:3]
        return None
    return m


def bull_emblem(w, h):
    """The Black Bulls crest: a bull's head with two up-curved horns, gold with a light rim, on the black cape."""
    cv = Canvas(w, h)
    c = w / 2.0
    g1, g2, dk = GOLD[1], GOLD[2], BLACK[0]
    for sgn in (-1, 1):                                                  # horns: out from the brow, then curling up
        pts = [(c + sgn * 3.5, 12.0), (c + sgn * 6.5, 12.5), (c + sgn * 9.0, 10.5), (c + sgn * 10.0, 6.5), (c + sgn * 9.2, 4.5)]
        for k in range(len(pts) - 1):
            cv.line(pts[k][0], pts[k][1], pts[k + 1][0], pts[k + 1][1], g1, width=2)
        cv.put(c + sgn * 9.2 - (1 if sgn > 0 else 0), 4, g2)
        cv.put(c + sgn * 9.2 - (1 if sgn > 0 else 0), 5, g2)
        ear = [(c + sgn * 4.0, 14.0), (c + sgn * 7.0, 15.0), (c + sgn * 4.0, 16.5)]
        cv.poly(ear, g1)
    cv.poly([(c - 4.2, 11.0), (c + 4.2, 11.0), (c + 3.4, 18.0), (c + 2.4, 23.0), (c - 2.4, 23.0), (c - 3.4, 18.0)], g1)   # the head
    cv.rect(int(c) - 2, 20, int(c) + 1, 23, g2)                                                                         # the muzzle
    for sgn in (-1, 1):
        cv.put(int(c) + (2 if sgn > 0 else -3), 15, dk)                                                                 # eyes
        cv.put(int(c) + (1 if sgn > 0 else -2), 15, dk)
        cv.put(int(c) + (1 if sgn > 0 else -2), 22, dk)                                                                 # nostrils
    return cv


def vest_mark(face, i, j, fw, fh):
    if face in ("front", "back") and (i == 1 or i == fw - 2) and 2 < j < fh - 2:
        return BLUE[2]
    return None


def pants_mark(face, i, j, fw, fh):
    if face == "front" and i == fw // 2 and j > 1:
        return NAVY[0]
    return None


def wrap_mark(face, i, j, fw, fh):
    if face in SIDES and (j + (i // 2)) % 3 == 0:
        return WHITE[0]
    return None


def blade_mark(face, i, j, fw, fh):
    """The Demon-Slayer: a black blade with a thin steel edge on the long (lower) rim and a few pale cracks."""
    if face in ("left", "right"):
        if j >= fh - 2:
            return STEEL[1]
        if hsh(i // 2, j // 3, 9) > 0.93:
            return STEEL[0]
    return None


# ------------------------------------------------------------------------------------------------------------------ the build
def build():
    b = Build("asta", 512, 512, dens=3)
    A = b.A
    # ----- bones (pixels, y up, feet at 0)
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 14, 0))
    b.bone("head", "body", (0, 25, 0))
    b.bone("hair", "head", (0, 33.4, 0))
    for mood in ("calm", "grin", "shout", "hurt", "blink"):
        b.bone("face_" + mood, "head", (0, 28.5, -4.2))
    b.bone("cape", "body", (0, 25, 2.3))
    b.bone("cape2", "cape", (0, 16.2, 2.5))
    b.bone("sword_back", "body", (0, 22, 3.6))
    for suf, sg in (("_l", 1), ("_r", -1)):
        b.bone("arm" + suf, "body", (sg * 5.3, 24, 0))
        b.bone("forearm" + suf, "arm" + suf, (sg * 5.3, 17.5, 0))
        b.bone("leg" + suf, "root", (sg * 2.0, 14, 0))
        b.bone("shin" + suf, "leg" + suf, (sg * 2.0, 7, 0))
    b.bone("sword_hand", "forearm_r", (-5.3, 11.0, 0))

    # ----- legs: navy trousers (thigh, lower leg), brown boots with a cuff and a toe cap
    b.pair("leg_l", "leg_r", (0.1, 7.0, -2.0), (3.8, 7.0, 4.0), cel(NAVY, NAVY_L, band=0.2, mark=pants_mark))
    b.pair("shin_l", "shin_r", (0.2, 3.7, -1.9), (3.6, 3.3, 3.8), cel(NAVY, NAVY_L, band=0.2, mark=pants_mark))
    b.pair("shin_l", "shin_r", (0.0, 0.0, -2.1), (3.9, 4.3, 4.2), cel(BOOT, BOOT_L, band=0.22))
    b.pair("shin_l", "shin_r", (-0.1, 3.7, -2.3), (4.1, 1.0, 4.6), cel(GOLD, GOLD_L), faces=SIDES + ("top",))          # the boot cuff
    b.pair("shin_l", "shin_r", (0.15, 0.0, -4.4), (3.6, 2.3, 2.4), cel(BOOT, BOOT_L, band=0.3))                         # the toe cap
    # ----- torso: the white tunic with the V-neck, the belt and its buckle, the blue open vest
    b.box("body", (-3.5, 14.0, -2.0), (7.0, 11.0, 4.0), cel(WHITE, WHITE_L, band=0.14, mark=tunic_mark, right_dark=False))
    b.box("body", (-3.8, 14.0, -2.3), (7.6, 1.7, 4.6), cel(BOOT, BOOT_L, hi=2), faces=SIDES + ("top", "bottom"))
    b.box("body", (-1.0, 14.1, -2.7), (2.0, 1.4, 0.5), gold_cel(2), faces=("front", "top", "bottom", "left", "right"))
    b.pair("body", "body", (2.0, 15.2, -2.45), (1.7, 9.6, 0.6), cel(BLUE, BLUE_L, band=0.1, mark=vest_mark, right_dark=False), faces=SIDES + ("top", "bottom"))   # vest front flaps
    b.pair("body", "body", (3.45, 15.4, -2.3), (0.55, 8.4, 4.6), cel(BLUE, BLUE_L, band=0.1, right_dark=False), faces=SIDES + ("top", "bottom"))                  # vest sides
    b.box("body", (-1.2, 25.0, -1.2), (2.4, 0.9, 2.4), skin_cel(), faces=SIDES)                                                                                 # neck
    # ----- arms: bare upper arm in a short blue sleeve, a white wrist wrap, a fist
    b.pair("arm_l", "arm_r", (3.8, 18.0, -1.5), (3.0, 6.0, 3.0), skin_cel(band=0.2))
    b.pair("arm_l", "arm_r", (3.5, 21.0, -1.85), (3.6, 3.4, 3.7), cel(BLUE, BLUE_L, band=0.18, hi=1), faces=SIDES + ("top", "bottom"))
    b.pair("forearm_l", "forearm_r", (3.8, 12.2, -1.5), (3.0, 5.3, 3.0), skin_cel(band=0.2))
    b.pair("forearm_l", "forearm_r", (3.65, 12.4, -1.65), (3.3, 2.7, 3.3), cel(WHITE, WHITE_L, mark=wrap_mark, right_dark=False), faces=SIDES + ("bottom",))   # the wrap
    b.pair("forearm_l", "forearm_r", (3.65, 9.9, -1.7), (3.3, 2.4, 3.4), skin_cel(band=0.25), faces=SIDES + ("bottom",))                                         # the fist
    # ----- head: skin, ears, the black headband with a gold crest, the hair
    b.box("head", (-4.0, 25.0, -4.0), (8.0, 8.0, 8.0), head_paint(), dens={"front": 4}, faces=("front", "left", "right", "back", "top", "bottom"))
    b.pair("head", "head", (4.0, 27.4, -0.9), (0.8, 2.4, 1.8), skin_cel(), faces=SIDES + ("top", "bottom"))
    b.box("head", (-4.3, 31.2, -4.3), (8.6, 1.3, 8.6), cel(BLACK, BLACK_L, hi=1), faces=SIDES + ("top",))
    b.box("head", (-1.4, 31.15, -4.6), (2.8, 1.4, 0.4), gold_cel(7), faces=("front", "top", "bottom", "left", "right"))
    b.box("head", (-4.35, 32.4, -4.35), (8.7, 1.4, 8.7), hair_cel(salt=1), faces=("front", "left", "right", "back", "top"))
    b.box("head", (-4.35, 27.0, 3.55), (8.7, 4.4, 0.8), hair_cel(salt=4, cut="b"), faces=("back", "left", "right", "bottom"))
    b.pair("head", "head", (3.95, 28.0, -3.0), (0.55, 3.2, 2.2), hair_cel(salt=5, cut="b"), faces=SIDES + ("top",))
    for mood in ("calm", "grin", "shout", "hurt", "blink"):
        b.box("face_" + mood, (-3.5, 26.0, -4.34), (7.0, 5.0, 0.34), plate_paint(mood), dens={"front": 4}, faces=("front",))
    # the bangs and the spikes (flat cards and tapering stacks, all ash-blond)
    # bangs: pointed locks hanging over the forehead; the spikes: crossed pairs of triangle cards fanned out over the crown
    for n, x in enumerate((-3.6, -2.2, -0.7, 0.8, 2.3, 3.7)):
        ln = (3.0, 2.3, 2.9, 2.1, 3.0, 2.4)[n]
        b.clump("head", (x, 33.2, -4.75), x * 3.0, 4 + 4 * (n % 2), 2.6, ln, hair_tri(10 + n, down=True, lean=(-1.0 if n % 2 else 1.0)), key="bg%d" % n, dens=3)
    crown = ((0, 0.0, 4, 7.4, 5.0), (-38, 2.0, 24, 7.2, 4.6), (38, 2.0, 24, 7.2, 4.6), (-82, 2.8, 36, 6.6, 4.4), (82, 2.8, 36, 6.6, 4.4),
             (-132, 2.6, 38, 7.0, 4.6), (132, 2.6, 38, 7.0, 4.6), (180, 2.4, 40, 7.4, 4.8), (-14, -2.4, 16, 6.4, 4.2), (16, -2.4, 16, 6.4, 4.2))
    for n, (phi, r, a, ln, w) in enumerate(crown):
        px_, pz_ = r * math.sin(math.radians(phi)), -r * math.cos(math.radians(phi))
        base = (px_, 33.2, pz_)
        b.clump("hair", base, phi, a, w, ln, hair_tri(30 + n), standing=True, key="cs%d" % n, dens=3, thick=0.4)
        b.clump("hair", base, phi + 90, 0, w * 0.9, ln * 0.96, hair_tri(50 + n), standing=True, key="cc%d" % n, dens=3, thick=0.4, spin=-a * 0.8)
    # ----- the cape: a black cape with gold edges and the crest, in two swinging pieces
    em = bull_emblem(24, 27)
    b.box("cape", (-4.0, 16.2, 2.2), (8.0, 8.8, 0.9), cel(BLACK, BLACK_L, mark=cape_mark(emblem=em), back_dark=False, right_dark=False), faces=SIDES + ("top",))
    b.box("cape2", (-4.0, 7.4, 2.2), (8.0, 8.8, 0.9), cel(BLACK, BLACK_L, mark=cape_mark(trim_bottom=True), cut="b", salt=3, right_dark=False), faces=SIDES + ("bottom",))
    b.box("cape", (-4.4, 23.6, 1.0), (8.8, 2.0, 2.6), cel(BLACK, BLACK_L, hi=1, right_dark=False), faces=SIDES + ("top",))                                      # the collar
    # ----- the Demon-Slayer: slung on the back, and drawn in the right hand
    b.box("sword_back", (-0.5, 19.0, 3.4), (1.0, 5.2, 1.0), cel(BOOT, BOOT_L), rotation=(0, 0, -24), pivot=(0, 22, 3.8))
    b.box("sword_back", (-1.5, -1.0, 3.3), (3.0, 20.6, 0.9), cel(BLACK, BLACK_L, mark=blade_mark, right_dark=False, back_dark=False), rotation=(0, 0, -24), pivot=(0, 22, 3.8))
    b.box("sword_hand", (-5.8, 10.4, -2.0), (1.0, 1.0, 4.4), cel(BOOT, BOOT_L))
    b.box("sword_hand", (-5.75, 9.6, -23.0), (0.9, 2.8, 21.0), cel(BLACK, BLACK_L, mark=blade_mark, right_dark=False))
    b.box("sword_hand", (-5.5, 10.0, -24.2), (0.4, 2.0, 1.4), cel(STEEL, BLACK_L), faces=SIDES + ("top", "bottom"))

    clips(A)
    return b


# ------------------------------------------------------------------------------------------------------------------ clips
def plate_track(a, clip, L, shows, loop):
    """The face plates by scale: `shows` = {mood: [(t0, t1), ...]}; instant switches (0.02 s apart)."""
    for mood in ("calm", "grin", "shout", "hurt", "blink"):
        spans = shows.get(mood, [])
        frames = {}
        state0 = ONE if any(t0 <= 0.0 < t1 for t0, t1 in spans) else GONE
        frames[0.0] = state0
        for t0, t1 in spans:
            if t0 > 0.0:
                frames[round(t0 - 0.02, 2)] = GONE
                frames[round(t0, 2)] = ONE
            if t1 < L:
                frames[round(t1, 2)] = ONE
                frames[round(t1 + 0.02, 2)] = GONE
        end = ONE if any(t0 < L <= t1 for t0, t1 in spans) else GONE
        frames[round(L, 2)] = end if not loop else state0
        a.scale(clip, "face_" + mood, frames)


def clips(a):
    S, C = SIN, COS
    # idle (loop 3.2 s): breathing, the cape and hair sway, a blink
    L = 3.2
    a.clip("idle", L, True)
    cyc(a, "idle", "body", "pos", L, lambda t: (0, 0.16 * S(t), 0))
    cyc(a, "idle", "body", "rot", L, swing((0, 0, 0), (0.5, 1.0, 0.4), 0.6))
    cyc(a, "idle", "head", "rot", L, swing((1, 0, 0), (1.2, 3.0, 0.8), 0.7))
    cyc(a, "idle", "hair", "rot", L, swing((0, 0, 0), (1.8, 0, 1.6), 1.2))
    cyc(a, "idle", "cape", "rot", L, swing((3, 0, 0), (1.6, 0, 0.8), 0.9))
    cyc(a, "idle", "cape2", "rot", L, swing((2, 0, 0), (2.6, 0, 1.4), 1.6))
    for sg, suf in ((1, "_l"), (-1, "_r")):
        cyc(a, "idle", "arm" + suf, "rot", L, swing((4, 0, 7 * sg), (1.4, 0, 1.0 * sg), 0.4 + sg))
        cyc(a, "idle", "forearm" + suf, "rot", L, swing((12, 0, 0), (2.6, 0, 0), 1.0 + sg))
    plate_track(a, "idle", L, {"calm": [(0.0, 2.5), (2.64, L)], "blink": [(2.5, 2.64)]}, True)

    # walk (loop 0.9 s)
    L = 0.9
    a.clip("walk", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "walk", "leg" + sd, "rot", L, lambda t, ph=ph: (28 * S(t + ph), 0, 0))
        cyc(a, "walk", "shin" + sd, "rot", L, lambda t, ph=ph: (-6 - 38 * max(0.0, C(t + ph)), 0, 0))
        cyc(a, "walk", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-26 * S(t + ph) + 4, 0, 6 * sg))
        cyc(a, "walk", "forearm" + sd, "rot", L, lambda t, ph=ph: (14 + 12 * max(0.0, S(t + ph)), 0, 0))
    cyc(a, "walk", "body", "pos", L, lambda t: (0, 0.5 * C(2 * t), 0))
    cyc(a, "walk", "body", "rot", L, lambda t: (-2, 3.0 * S(t), 0.8 * S(t)))
    cyc(a, "walk", "head", "rot", L, lambda t: (3 + 0.8 * S(2 * t + 1.6), -2.4 * S(t), 0))
    cyc(a, "walk", "hair", "rot", L, lambda t: (3 * S(2 * t + 0.4), 0, 1.0 * S(t)))
    cyc(a, "walk", "cape", "rot", L, lambda t: (-8 + 3 * S(2 * t + 0.5), 0, 1.2 * S(t)))
    cyc(a, "walk", "cape2", "rot", L, lambda t: (-8 + 5 * S(2 * t + 1.0), 0, 2 * S(t)))
    plate_track(a, "walk", L, {"calm": [(0.0, L)]}, True)

    # run (loop 0.55 s): leaning in, the grin
    L = 0.55
    a.clip("run", L, True)
    for sd, ph, sg in (("_l", 0.0, 1), ("_r", math.pi, -1)):
        cyc(a, "run", "leg" + sd, "rot", L, lambda t, ph=ph: (48 * S(t + ph), 0, 0))
        cyc(a, "run", "shin" + sd, "rot", L, lambda t, ph=ph: (-12 - 70 * max(0.0, C(t + ph)), 0, 0))
        cyc(a, "run", "arm" + sd, "rot", L, lambda t, ph=ph, sg=sg: (-52 * S(t + ph) + 8, 0, 10 * sg))
        cyc(a, "run", "forearm" + sd, "rot", L, lambda t, ph=ph: (50 + 26 * max(0.0, S(t + ph)), 0, 0))
    cyc(a, "run", "body", "pos", L, lambda t: (0, 0.9 * C(2 * t) + 0.2, 0))
    cyc(a, "run", "body", "rot", L, lambda t: (-14 + 1.5 * S(2 * t), 5 * S(t), 1.2 * S(t)))
    cyc(a, "run", "head", "rot", L, lambda t: (10 + 1.5 * S(2 * t + 1.6), -5 * S(t), 0))
    cyc(a, "run", "hair", "rot", L, lambda t: (-12 + 4 * S(2 * t + 0.4), 0, 2 * S(t)))
    cyc(a, "run", "cape", "rot", L, lambda t: (-28 + 6 * S(2 * t + 0.5), 0, 2 * S(t)))
    cyc(a, "run", "cape2", "rot", L, lambda t: (-34 + 8 * S(2 * t + 1.0), 0, 4 * S(t)))
    plate_track(a, "run", L, {"grin": [(0.0, L)]}, True)

    # attack (one-shot 0.8 s): reach back, draw (the sword swaps hands at 0.12 s), a diagonal slash, follow through
    L = 0.8
    a.clip("attack", L, False)
    T = (0.0, 0.12, 0.28, 0.42, 0.56, 0.8)
    seq(a, "attack", "rot", T,
        arm_r=[(4, 0, -7), (-150, 0, -30), (-96, -10, -24), (52, 8, -12), (66, 10, -10), (10, 0, -8)],
        forearm_r=[(12, 0, 0), (40, 0, 0), (84, 0, 0), (10, 0, 0), (24, 0, 0), (14, 0, 0)],
        arm_l=[(4, 0, 7), (-20, 0, 24), (-30, 0, 34), (24, 0, 30), (16, 0, 22), (4, 0, 7)],
        body=[(0, 0, 0), (-4, -12, 0), (-6, -16, 0), (10, 22, 0), (8, 14, 0), (0, 0, 0)],
        head=[(1, 0, 0), (2, -8, 0), (0, -10, 0), (4, 14, 0), (3, 10, 0), (1, 0, 0)],
        leg_l=[(0, 0, 0), (-10, 0, 0), (-14, 0, 0), (26, 0, 0), (22, 0, 0), (0, 0, 0)],
        leg_r=[(0, 0, 0), (12, 0, 0), (14, 0, 0), (-30, 0, 0), (-24, 0, 0), (0, 0, 0)],
        shin_r=[(0, 0, 0), (-8, 0, 0), (-12, 0, 0), (-40, 0, 0), (-28, 0, 0), (0, 0, 0)],
        cape=[(3, 0, 0), (10, 0, 0), (14, 0, 0), (-26, 0, 0), (-16, 0, 0), (3, 0, 0)],
        cape2=[(2, 0, 0), (14, 0, 0), (22, 0, 0), (-40, 0, 0), (-24, 0, 0), (2, 0, 0)],
        hair=[(0, 0, 0), (8, 0, 0), (12, 0, 0), (-16, 0, 0), (-8, 0, 0), (0, 0, 0)])
    a.scale("attack", "sword_back", {0.0: ONE, 0.1: ONE, 0.12: GONE, 0.8: GONE})
    a.scale("attack", "sword_hand", {0.0: GONE, 0.1: GONE, 0.12: ONE, 0.8: ONE})
    plate_track(a, "attack", L, {"calm": [(0.0, 0.12)], "shout": [(0.12, 0.62)], "grin": [(0.62, L)]}, False)

    # talk (loop 2.4 s): the right hand gestures, the head nods, the mouth flaps between calm and grin
    L = 2.4
    a.clip("talk", L, True)
    cyc(a, "talk", "body", "pos", L, lambda t: (0, 0.14 * S(2 * t), 0))
    cyc(a, "talk", "body", "rot", L, swing((0, 0, 0), (0.6, 1.4, 0.5), 0.6))
    cyc(a, "talk", "head", "rot", L, lambda t: (2 + 3 * S(2 * t + 0.4), 3 * S(t + 0.6), 0.8 * S(t)))
    cyc(a, "talk", "arm_r", "rot", L, lambda t: (-48 + 14 * S(2 * t), 0, -22 + 6 * S(t)))
    cyc(a, "talk", "forearm_r", "rot", L, lambda t: (62 + 12 * S(2 * t + 1.0), 0, 0))
    cyc(a, "talk", "arm_l", "rot", L, swing((4, 0, 8), (1.4, 0, 1.0), 1.4))
    cyc(a, "talk", "forearm_l", "rot", L, swing((12, 0, 0), (2.4, 0, 0), 1.0))
    cyc(a, "talk", "hair", "rot", L, swing((0, 0, 0), (1.6, 0, 1.4), 1.2))
    cyc(a, "talk", "cape", "rot", L, swing((3, 0, 0), (1.4, 0, 0.8), 0.9))
    cyc(a, "talk", "cape2", "rot", L, swing((2, 0, 0), (2.4, 0, 1.2), 1.6))
    flap = [(round(0.3 * k, 2), round(0.3 * k + 0.15, 2)) for k in range(8)]
    rest = [(round(0.3 * k + 0.15, 2), round(0.3 * k + 0.3, 2)) for k in range(8)]
    plate_track(a, "talk", L, {"grin": flap, "calm": rest}, True)

    # wave (loop 1.6 s): the left hand up and waving, a grin
    L = 1.6
    a.clip("wave", L, True)
    cyc(a, "wave", "body", "pos", L, lambda t: (0, 0.14 * S(t), 0))
    cyc(a, "wave", "arm_l", "rot", L, lambda t: (-8, 0, 138 + 8 * S(2 * t)))
    cyc(a, "wave", "forearm_l", "rot", L, lambda t: (4, 0, 22 * S(2 * t)))
    cyc(a, "wave", "arm_r", "rot", L, swing((4, 0, -7), (1.4, 0, -1.0), 0.4))
    cyc(a, "wave", "head", "rot", L, swing((0, 0, 0), (1.0, 3.0, 4.0), 0.7))
    cyc(a, "wave", "hair", "rot", L, swing((0, 0, 0), (1.8, 0, 1.6), 1.2))
    cyc(a, "wave", "cape", "rot", L, swing((3, 0, 0), (1.6, 0, 0.8), 0.9))
    cyc(a, "wave", "cape2", "rot", L, swing((2, 0, 0), (2.6, 0, 1.4), 1.6))
    plate_track(a, "wave", L, {"grin": [(0.0, L)]}, True)

    # hurt (one-shot 0.45 s)
    L = 0.45
    a.clip("hurt", L, False)
    T = (0.0, 0.08, 0.45)
    seq(a, "hurt", "rot", T, body=[(0, 0, 0), (-12, 0, 4), (0, 0, 0)], head=[(1, 0, 0), (-16, 8, 0), (1, 0, 0)],
        arm_l=[(4, 0, 7), (-24, 0, 30), (4, 0, 7)], arm_r=[(4, 0, -7), (-30, 0, -30), (4, 0, -7)],
        cape=[(3, 0, 0), (-22, 0, 0), (3, 0, 0)], cape2=[(2, 0, 0), (-30, 0, 0), (2, 0, 0)], hair=[(0, 0, 0), (-14, 0, 0), (0, 0, 0)])
    seq(a, "hurt", "pos", T, body=[(0, 0, 0), (0, 0, 1.6), (0, 0, 0)])
    plate_track(a, "hurt", L, {"hurt": [(0.0, L)]}, False)

    # death (one-shot 2.2 s, holds): staggers, drops to the knees, falls back
    L = 2.2
    a.clip("death", L, False)
    T = (0.0, 0.3, 0.8, 1.3, 1.8, 2.2)
    seq(a, "death", "rot", T,
        arm_l=[(4, 0, 7), (-24, 0, 30), (26, 0, 14), (30, 0, 12), (-6, 0, 40), (-6, 0, 40)],
        arm_r=[(4, 0, -7), (-28, 0, -30), (26, 0, -14), (30, 0, -12), (-6, 0, -40), (-6, 0, -40)],
        body=[(0, 0, 0), (-14, 0, 3), (6, 0, 0), (-20, 0, 0), (0, 0, 0), (0, 0, 0)],
        head=[(1, 0, 0), (-18, 0, 0), (14, 0, 0), (-30, 0, 4), (-8, 0, 8), (-8, 0, 8)],
        leg_l=[(0, 0, 0), (-8, 0, 0), (58, 0, 4), (60, 0, 4), (4, 0, 2), (4, 0, 2)],
        leg_r=[(0, 0, 0), (8, 0, 0), (-60, 0, -4), (-62, 0, -4), (4, 0, -2), (4, 0, -2)],
        shin_l=[(0, 0, 0), (-6, 0, 0), (-90, 0, 0), (-92, 0, 0), (-4, 0, 0), (-4, 0, 0)],
        shin_r=[(0, 0, 0), (-6, 0, 0), (-4, 0, 0), (-6, 0, 0), (-4, 0, 0), (-4, 0, 0)],
        cape=[(3, 0, 0), (-18, 0, 0), (14, 0, 0), (16, 0, 0), (60, 0, 0), (60, 0, 0)],
        cape2=[(2, 0, 0), (-24, 0, 0), (18, 0, 0), (22, 0, 0), (50, 0, 0), (50, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (-84, 0, 0), (-84, 0, 0)])
    seq(a, "death", "pos", T, body=[(0, 0, 0), (0, 0, 1.2), (0, -6.6, 0), (0, -7.4, 0), (0, 0, 0), (0, 0, 0)],
        root=[(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 3.0, 4.0), (0, 3.0, 4.0)])
    plate_track(a, "death", L, {"hurt": [(0.0, 1.3)], "blink": [(1.3, L)]}, False)


def weapon_defaults(a):
    """Every clip but the draw shows the sword on the back and hides the one in the hand."""
    for name, c in a.clips.items():
        if name == "attack":
            continue
        L = c["animation_length"]
        a.scale(name, "sword_back", {0.0: ONE, L: ONE})
        a.scale(name, "sword_hand", {0.0: GONE, L: GONE})


def prune_clips(b):
    weapon_defaults(b.A)
    have = set(b.m.bones)
    for c in b.A.clips.values():
        for bn in [x for x in c["bones"] if x not in have]:
            del c["bones"][bn]


# ------------------------------------------------------------------------------------------------------------------ preview scenes
def view(name, yaw, pitch, target, dist):
    return {"name": name, "yaw": yaw, "pitch": pitch, "target": list(target), "dist": dist}


def scene(model, anim, ages, views, tile=(400, 560)):
    return {"kind": "geo", "model": model, "space": "PROP", "layer": "CUTOUT", "argb": "FFFFFFFF", "glow": "FFFFFFFF", "ages": list(ages), "tile": list(tile),
            "floor": True, "figure": False, "expect": [], "anim": anim, "views": views}


H = 2.35     # blocks tall with the spikes
LOOKY = 1.12


def scenes_for():
    sc = {}
    d = H * 2.0
    sc["front"] = scene(MODEL, "idle", [0], [view("front", 0, 4, (0, LOOKY, 0), d), view("front 3/4", 35, 8, (0, LOOKY, 0), d)])
    sc["back"] = scene(MODEL, "idle", [0], [view("back", 180, 5, (0, LOOKY, 0), d), view("side", 90, 4, (0, LOOKY, 0), d)])
    sc["face"] = scene(MODEL, "idle", [0], [view("face", 0, 2, (0, 1.9, 0), 2.6), view("face 3/4", 32, 6, (0, 1.9, 0), 2.6)], (420, 420))
    sc["top"] = scene(MODEL, "idle", [0], [view("from above", 0, 62, (0, 2.0, 0), 3.4), view("from the back, high", 150, 30, (0, 2.0, 0), 3.2)], (420, 420))
    for clip, secs, n in (("walk", 0.9, 4), ("run", 0.55, 4), ("attack", 0.8, 5), ("talk", 2.4, 4), ("wave", 1.6, 4), ("hurt", 0.45, 3), ("death", 2.2, 5)):
        ages = [round(secs * 20.0 * k / n, 1) for k in range(n)]
        sc[clip] = scene(MODEL, clip, ages, [view("3/4", 30, 6, (0, LOOKY, 0), d * 1.05)], (340, 480))
    return sc


def write_scenes(path=SCENES_OUT):
    sc = {MODEL + "_" + k: v for k, v in scenes_for().items()}
    with open(path, "w") as f:
        json.dump(sc, f, indent=1)


def render_previews(names=None):
    import shutil
    import subprocess
    sc = {MODEL + "_" + k: v for k, v in scenes_for().items()}
    names = list(names or sc)
    env = dict(os.environ)
    env.setdefault("RENDER_PREVIEW_BUILD", os.path.join(SCRATCH, "rp_anime_asta"))
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


def main():
    b = build()
    prune_clips(b)
    img, glow = b.finish()
    b.save(img, glow)
    write_scenes()


if __name__ == "__main__":
    if "--preview" in sys.argv:
        sys.exit(render_previews([a for a in sys.argv[1:] if not a.startswith("-")] or None))
    main()

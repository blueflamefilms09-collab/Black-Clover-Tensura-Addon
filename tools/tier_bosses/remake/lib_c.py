"""Group c of the 3.37 Tier List remake (big_mom, code, foxy, ao); a private copy of the group b helpers so nothing here depends on another agent's file: shared helpers for hand-painted skins (ASCII pixel maps per face, mirrored arms, shaded tone ramps, eyes).

A skin is drawn face by face: `m(cv, part, face, rows, pal, overlay)` stamps a list of strings (one char per texel, '.' = leave alone) on one face of one part, exactly as the face
appears in the texture (viewed from outside). Arms are drawn once (the character's right arm, x = 0 is the outer edge on the front face) and mirrored for the left arm.
Nothing here touches shared files; it only imports skin_lib / anime_skin.
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TIER = os.path.abspath(os.path.join(HERE, ".."))
TOOLS = os.path.abspath(os.path.join(TIER, ".."))
for p in (TIER, os.path.join(TOOLS, "playermodel"), TOOLS):
    if p not in sys.path:
        sys.path.insert(0, p)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import skin_lib as K  # noqa: E402
import anime_skin as A  # noqa: E402


def hx(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def ramp(c, ks=(0.5, 0.74, 1.0, 1.2, 1.45)):
    """Five tones of a colour, dark to light, with the anime hue shift (cool shadow, warm light)."""
    return [A.lit(c, k) for k in ks]


def mix(a, b, t):
    return A.mixc(a, b, t)


def flip(rows):
    return [r[::-1] for r in rows]


def m(cv, part, face, rows, pal, overlay=False):
    """Stamp an ASCII map on one face. '.' and ' ' leave the texel alone."""
    w, h, d = cv.size(part)
    u, v = cv.uv(part, overlay)
    x0, y0, rw, rh = K.rects(u, v, w, h, d)[face]
    assert len(rows) == rh and all(len(r) == rw for r in rows), (part, face, overlay, rh, rw, len(rows), [len(r) for r in rows])
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in ". ":
                continue
            c = pal[ch]
            cv.put(x0 + x, y0 + y, tuple(c) + (255,) if len(c) == 3 else tuple(c))


def fill(cv, part, face, color, overlay=False, fn=None):
    """Fill a whole face with a colour, or with fn(x, y, w, h) -> colour/None."""
    w, h, d = cv.size(part)
    u, v = cv.uv(part, overlay)
    x0, y0, rw, rh = K.rects(u, v, w, h, d)[face]
    for y in range(rh):
        for x in range(rw):
            c = fn(x, y, rw, rh) if fn else color
            if c is not None:
                cv.put(x0 + x, y0 + y, tuple(c) + (255,))


def get(cv, part, face, x, y, overlay=False):
    w, h, d = cv.size(part)
    u, v = cv.uv(part, overlay)
    x0, y0, rw, rh = K.rects(u, v, w, h, d)[face]
    return tuple(int(t) for t in cv.px[y0 + y, x0 + x])


def setp(cv, part, face, x, y, c, overlay=False):
    w, h, d = cv.size(part)
    u, v = cv.uv(part, overlay)
    x0, y0, rw, rh = K.rects(u, v, w, h, d)[face]
    cv.put(x0 + x, y0 + y, tuple(c) + (255,))


def arm_pair(cv, faces, pal, overlay=True):
    """faces: dict face -> rows for the character's RIGHT arm (front: x = 0 is the outer edge; west is the outer side). The left arm gets the mirror image."""
    for face, rows in faces.items():
        m(cv, "arm_r", face, rows, pal, overlay)
        mirror = {"front": "front", "back": "back", "west": "east", "east": "west", "top": "top", "bottom": "bottom"}[face]
        m(cv, "arm_l", mirror, flip(rows), pal, overlay)


def leg_pair(cv, faces, pal, overlay=True):
    """faces for the RIGHT leg (front: x = 3 is the inner edge); the left leg gets the mirror image."""
    arm_like = {"front": "front", "back": "back", "west": "east", "east": "west", "top": "top", "bottom": "bottom"}
    for face, rows in faces.items():
        m(cv, "leg_r", face, rows, pal, overlay)
        m(cv, "leg_l", arm_like[face], flip(rows), pal, overlay)


def side_pair(cv, part, west_rows, pal, overlay=True):
    m(cv, part, "west", west_rows, pal, overlay)
    m(cv, part, "east", flip(west_rows), pal, overlay)


def glow_image(points):
    g = np.zeros((64, 64, 4), np.uint8)
    for (x, y, c) in points:
        g[y, x] = (c[0], c[1], c[2], 255)
    return Image.fromarray(g, "RGBA") if points else None


def head_front_origin(cv, overlay=False):
    fx, fy, _, _ = cv.front_rect("head", overlay)
    return fx, fy


def check_hat_top_empty(cv):
    """The hat layer's top face (TierBossesTest: u 40..47, v 0..7) must stay free."""
    assert not cv.px[0:8, 40:48, 3].any(), "hat layer top is not empty"


# ---------------------------------------------------------------------------------------------------- group c extras
def pal_from(**groups):
    """pal_from(skin=("abcef", rampcolors), ...) is not needed: use put_ramp."""
    raise NotImplementedError


def put_ramp(pal, chars, color, ks):
    for ch, c in zip(chars, ramp(color, ks)):
        pal[ch] = c


def skin_base(cv, pal, tone="c", shade="b"):
    """Every face of every part painted with skin first (front/top lighter)."""
    for part in K.PARTS:
        for face in ("front", "back", "west", "east", "top", "bottom"):
            fill(cv, part, face, pal[tone] if face in ("front", "top") else pal[shade], overlay=False)


def rows_fn(rowchars, shade_right=None):
    """fn(x, y, w, h) -> palette char key, from a list of per-row chars; kept as a helper for gradient fills: returns the char for row y."""
    return lambda x, y, w, h: rowchars[min(y, len(rowchars) - 1)]


def fillc(cv, part, face, pal, fn, overlay=False):
    """Like fill, but fn returns a palette KEY (or None)."""
    fill(cv, part, face, None, overlay=overlay, fn=lambda x, y, w, h: (lambda k: pal[k] if k else None)(fn(x, y, w, h)))


def stripes(rows, left, right):
    """Pad/extend rows so the ASCII maps stay readable: returns rows unchanged (placeholder for symmetry helpers)."""
    return rows


def mirror_rows(rows):
    """Left half -> full row: rows given as left halves (4 chars for an 8-wide face), mirrored to the right."""
    return [r + r[::-1] for r in rows]


def head_shell(cv, pal, h_hi="4", h_mid="3", h_lo="2", h_dk="1", side_hair_rows=4, ear="b", jaw="b"):
    """Head sides / top / back / bottom: hair tones on the scalp, a skin cheek on the lower front half of each side (west: front is x = 7, east: front is x = 0)."""
    def side(face):
        def f(x, y, w, h):
            front = x >= 4 if face == "west" else x <= 3
            if y < side_hair_rows:
                if y == side_hair_rows - 1 and front:
                    return pal[jaw]
                return pal[h_mid] if y < 2 else pal[h_lo]
            if front:
                return pal[jaw] if x not in (3, 4) else pal[ear]
            return pal[h_lo] if y < 7 else pal[h_dk]
        return f
    fill(cv, "head", "west", None, fn=side("west"))
    fill(cv, "head", "east", None, fn=side("east"))
    fill(cv, "head", "top", None, fn=lambda x, y, w, h: pal[h_hi] if (x + y) % 3 else pal[h_mid])
    fill(cv, "head", "back", None, fn=lambda x, y, w, h: pal[h_mid] if y < 3 else (pal[h_lo] if y < 6 else pal[h_dk]))
    fill(cv, "head", "bottom", pal[jaw])


def skin_pal(pal, color, ks=(0.76, 0.88, 1.0, 1.07, 1.14)):
    put_ramp(pal, "abcef", color, ks)


def hair_pal(pal, color, ks=(0.5, 0.74, 1.0, 1.2, 1.42)):
    put_ramp(pal, "12345", color, ks)


def glow_of(cv, colors):
    """Glow map: every texel of the finished skin whose colour is one of `colors` (RGB tuples) glows with exactly that colour."""
    want = set(tuple(c[:3]) for c in colors)
    pts = []
    for y in range(64):
        for x in range(64):
            px = cv.px[y, x]
            if px[3] and tuple(int(t) for t in px[:3]) in want:
                pts.append((x, y, tuple(int(t) for t in px[:3])))
    return glow_image(pts)

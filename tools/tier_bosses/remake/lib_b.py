"""Group b of the 3.37 Tier List remake: shared helpers for hand-painted skins (ASCII pixel maps per face, mirrored arms, shaded tone ramps, eyes).

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

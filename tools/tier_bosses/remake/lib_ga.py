"""Helpers for the hand-painted Tier List skins of group a (charlos, zetsu, yammy_llargo, sogou_nobuyuki, kurozumi_orochi).

Everything is painted in TEXTURE space of each face (what you see when you look at that face from outside), so an ASCII panel reads like the picture it makes:
    front: x = 0 is the viewer's left (the entity's right side); west: the entity's right side, its front at the RIGHT end of the panel; east: the entity's left side, front at the LEFT end.
Parts: head body arm_r arm_l leg_r leg_l; overlay=True is the second layer (hat, jacket, sleeve, pants). Original pixel art, no traced skins.
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TB = os.path.abspath(os.path.join(HERE, ".."))
TOOLS = os.path.abspath(os.path.join(TB, ".."))
for p in (TB, os.path.join(TOOLS, "playermodel"), TOOLS):
    if p not in sys.path:
        sys.path.insert(0, p)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import skin_lib as K  # noqa: E402
import anime_skin as A  # noqa: E402


def hx(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16))


def ramp(base, n=6, soft=1.0):
    """dark -> light tones of a colour (hue-shifted: cool shadows, warm lights). soft < 1 squeezes the range (whites and pale colours must not go grey)."""
    base = hx(base) if isinstance(base, str) else base
    ks = (0.46, 0.64, 0.82, 1.0, 1.17, 1.36) if n == 6 else (0.55, 0.78, 1.0, 1.2, 1.42)
    return [A.lit(base, 1.0 + (k - 1.0) * soft) for k in ks]


def mix(a, b, t):
    return A.mixc(a, b, t)


class Sk:
    def __init__(self, slim=False):
        self.slim = slim
        self.cv = K.Canvas(slim=slim)
        self.glow = np.zeros((64, 64, 4), np.uint8)
        self.any_glow = False

    # ---------------------------------------------------------------- geometry
    def rect(self, part, face, overlay=False):
        w, h, d = self.cv.size(part)
        u, v = self.cv.uv(part, overlay)
        return K.rects(u, v, w, h, d)[face]

    def put(self, part, face, overlay, x, y, c, glow=False):
        x0, y0, rw, rh = self.rect(part, face, overlay)
        if 0 <= x < rw and 0 <= y < rh and c is not None:
            self.cv.put(x0 + x, y0 + y, tuple(c)[:3] + (255,))
            if glow:
                self.glow[y0 + y, x0 + x] = tuple(c)[:3] + (255,)
                self.any_glow = True

    def get(self, part, face, overlay, x, y):
        x0, y0, rw, rh = self.rect(part, face, overlay)
        return tuple(int(v) for v in self.cv.px[y0 + y, x0 + x])

    def size(self, part, face, overlay=False):
        r = self.rect(part, face, overlay)
        return r[2], r[3]

    # ---------------------------------------------------------------- painting
    def art(self, part, face, overlay, rows, leg, ox=0, oy=0, glow_chars=""):
        """ASCII panel. '.' leaves the texel alone; a char in leg paints it (None = erase to transparent)."""
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch == ".":
                    continue
                if ch not in leg:
                    raise KeyError("legend has no '%s'" % ch)
                c = leg[ch]
                if c is None:
                    x0, y0, rw, rh = self.rect(part, face, overlay)
                    if 0 <= x + ox < rw and 0 <= y + oy < rh:
                        self.cv.put(x0 + x + ox, y0 + y + oy, (0, 0, 0, 0))
                else:
                    self.put(part, face, overlay, x + ox, y + oy, c, glow=ch in glow_chars)

    def fn(self, part, face, overlay, f):
        """f(x, y, w, h) -> colour or None, in texture space of the face."""
        x0, y0, rw, rh = self.rect(part, face, overlay)
        for y in range(rh):
            for x in range(rw):
                c = f(x, y, rw, rh)
                if c is not None:
                    self.cv.put(x0 + x, y0 + y, tuple(c)[:3] + (255,))

    def cloth(self, part, overlay, rp, faces=("front", "back", "west", "east", "top", "bottom"), y0=0, y1=None, light=0, folds=()):
        """A banded, shaded garment surface: light on top, shade toward the hem, the sides one step darker, a shade rim on the outer columns."""
        for face in faces:
            def f(x, y, w, h, face=face):
                hh = (y1 if y1 is not None else h) - y0
                if y < y0 or y >= y0 + hh:
                    return None
                t = (y - y0) / max(1.0, hh - 1.0)
                i = 4 if t < 0.12 else (3 if t < 0.5 else (2 if t < 0.82 else 1))
                if face in ("west", "east"):
                    i -= 1
                elif face == "back":
                    i -= 1
                elif face == "top":
                    i = 4
                elif face == "bottom":
                    i = 0
                if face in ("front", "back") and (x == 0 or x == w - 1):
                    i -= 1
                if face in ("front", "back") and x in folds and y >= y0 + 2:
                    i -= 1
                return rp[max(0, min(5, i + light))]
            self.fn(part, face, overlay, f)

    def finish(self):
        g = Image.fromarray(self.glow, "RGBA") if self.any_glow else None
        return self.cv.image(), g


def face_art(sk, rows, leg, glow_chars=""):
    """The 8x8 front of the head on the base layer."""
    sk.art("head", "front", False, rows, leg, glow_chars=glow_chars)


def scalp(sk, tones, front_rows=2, side_rows=4, back_rows=7):
    """Hair roots on the base head (the volumetric hair stands over them); the hat layer stays empty."""
    T = tones
    for x in range(8):
        for y in range(front_rows):
            sk.put("head", "front", False, x, y, T[3] if y == 0 else T[2])
    for face in ("west", "east"):
        for x in range(8):
            for y in range(side_rows):
                sk.put("head", face, False, x, y, T[2] if y < 2 else T[1])
    for x in range(8):
        for y in range(back_rows):
            sk.put("head", "back", False, x, y, T[2] if y < 2 else T[1])
    for x in range(8):
        for y in range(8):
            sk.put("head", "top", False, x, y, T[3] if y > 3 else T[2])


def eyes(sk, y, iris, lash, shape="round", hl=(255, 255, 255), glow=False, flick=True):
    """Big anime eyes, mirrored. iris = (dark, mid, light). round = 3 rows (lash, iris+highlight, light iris), sharp = 2 rows."""
    for outer, inner, fl in ((1, 2, 0), (6, 5, 7)):
        sk.put("head", "front", False, outer, y, lash)
        sk.put("head", "front", False, inner, y, lash)
        if flick and shape in ("round", "tall"):
            sk.put("head", "front", False, fl, y, lash)
        if shape == "sharp":
            sk.put("head", "front", False, outer, y + 1, iris[0], glow)
            sk.put("head", "front", False, inner, y + 1, iris[2] if not glow else iris[2], glow)
        else:
            sk.put("head", "front", False, outer, y + 1, iris[0], glow)
            sk.put("head", "front", False, inner, y + 1, hl, glow)
            sk.put("head", "front", False, outer, y + 2, iris[1], glow)
            sk.put("head", "front", False, inner, y + 2, iris[2], glow)


def brows(sk, y, col, style="flat"):
    for outer, inner in ((1, 2), (6, 5)):
        yo, yi = y, y
        if style == "angry":
            yi = y + 1
        elif style == "sad":
            yo = y + 1
        elif style == "arched":
            yo = y + 1
        sk.put("head", "front", False, outer, yo, col)
        sk.put("head", "front", False, inner, yi, col)


def skin_ramp(base):
    return ramp(base, 5)


def paint_base_skin(sk, skin5):
    """The bare body in the five tones (dark..light), light from above."""
    for part in K.PARTS:
        w, h, d = sk.cv.size(part)
        u, v = sk.cv.uv(part, False)
        for face, (x0, y0, rw, rh) in K.rects(u, v, w, h, d).items():
            for y in range(rh):
                for x in range(rw):
                    i = 3 if y < rh * 0.4 else 2
                    if face in ("west", "east", "back"):
                        i -= 1
                    if face == "bottom":
                        i = 0
                    if face == "top":
                        i = 4
                    sk.cv.put(x0 + x, y0 + y, skin5[max(0, i)] + (255,))

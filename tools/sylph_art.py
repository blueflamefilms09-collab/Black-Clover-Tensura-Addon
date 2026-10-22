"""Painter of the Sylph's sheets (docs/magic/sylph.md): an anime cel look (flat colours, a hard shadow band, dark line work painted INTO the texture) drawn with PIL at 4x and reduced, so the
lines are clean at the small sizes she is seen at. Every picture is made from nothing and deterministic (seeded), so the sheets can be redone.

References (the owner's three pictures): a laughing fairy with closed eyes, blush, an open mouth and a bright green ruffled dress; the same fairy with pale green eyes pointing at her head, a
white-mint shell / petal dress with a scalloped strapless top, a dark choker and long iridescent dragonfly wings with cell veins; and the powered form: stern face, long hair streaming
up, big green translucent veined wings, a layered petal-wing ruffle skirt, an intense green glow.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True

SS = 4                                                       # supersampling of every painter

INK = (74, 58, 40, 255)                                      # the dark brown line of the anime
INK_HAIR = (96, 84, 38, 255)
INK_GREEN = (52, 100, 70, 255)
SKIN = (255, 244, 230, 255)
SKIN_SH = (238, 208, 186, 255)
SKIN_HI = (255, 252, 244, 255)
BLUSH = (255, 150, 150, 150)
HAIR = {"small": ((240, 232, 128, 255), (206, 194, 84, 255), (252, 248, 176, 255)),
        "powered": ((240, 234, 142, 255), (208, 198, 100, 255), (252, 250, 188, 255))}
EYE_IRIS = (150, 214, 168, 255)
EYE_IRIS_DK = (74, 150, 112, 255)
DRESS = {"small": ((222, 246, 204, 255), (168, 214, 156, 255), (246, 255, 236, 255)),         # lime mint: the green glow of the first picture on a white-mint dress
         "powered": ((232, 246, 226, 255), (176, 218, 184, 255), (250, 255, 246, 255))}
CHOKER = (92, 74, 66, 255)


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(4))


class Canvas:
    """A picture of w x h model units at ppu pixels per unit, drawn at SS times that size. Coordinates are in units, (0, 0) the top left."""

    def __init__(self, wu, hu, ppu, bg=(0, 0, 0, 0)):
        self.wu, self.hu, self.ppu = wu, hu, ppu
        self.W, self.H = int(round(wu * ppu)), int(round(hu * ppu))
        self.k = ppu * SS
        self.img = Image.new("RGBA", (self.W * SS, self.H * SS), bg)
        self.d = ImageDraw.Draw(self.img)

    def p(self, x, y):
        return (x * self.k, y * self.k)

    def pts(self, ps):
        return [self.p(x, y) for x, y in ps]

    def poly(self, ps, fill, outline=None, width=0.0):
        self.d.polygon(self.pts(ps), fill=fill)
        if outline is not None:
            self.line(list(ps) + [ps[0]], outline, width)

    def ellipse(self, cx, cy, rx, ry, fill=None, outline=None, width=0.0):
        box = [(cx - rx) * self.k, (cy - ry) * self.k, (cx + rx) * self.k, (cy + ry) * self.k]
        self.d.ellipse(box, fill=fill, outline=outline, width=max(1, int(width * self.k)) if outline else 0)

    def line(self, ps, color, width, round_ends=True):
        w = max(1, int(round(width * self.k)))
        pp = self.pts(ps)
        self.d.line(pp, fill=color, width=w, joint="curve")
        if round_ends:
            r = w / 2.0
            for (x, y) in (pp[0], pp[-1]):
                self.d.ellipse([x - r, y - r, x + r, y + r], fill=color)

    def curve(self, ps, color, width, n=24):
        """A smooth line through the points (Catmull-Rom)."""
        self.line(catmull(ps, n), color, width)

    def layer(self):
        return Layer(self)

    def blur(self, r):
        self.img = self.img.filter(ImageFilter.GaussianBlur(r * self.k))
        self.d = ImageDraw.Draw(self.img)

    def done(self):
        return self.img.resize((self.W, self.H), Image.LANCZOS)


class Layer:
    """A transparent layer on a canvas: draw on layer.c (same units) and then layer.apply(alpha) to composite it."""

    def __init__(self, canvas):
        self.cv = canvas
        self.c = Canvas(canvas.wu, canvas.hu, canvas.ppu)

    def apply(self, alpha=1.0, mask=None):
        im = self.c.img
        if alpha < 1.0:
            a = np.asarray(im).copy()
            a[..., 3] = (a[..., 3] * alpha).astype(np.uint8)
            im = Image.fromarray(a, "RGBA")
        if mask is not None:
            a = np.asarray(im).copy()
            a[..., 3] = (a[..., 3].astype(np.float32) * mask).astype(np.uint8)
            im = Image.fromarray(a, "RGBA")
        self.cv.img = Image.alpha_composite(self.cv.img, im)
        self.cv.d = ImageDraw.Draw(self.cv.img)


def catmull(ps, n=24):
    if len(ps) < 3:
        return list(ps)
    out = []
    P = [ps[0]] + list(ps) + [ps[-1]]
    for i in range(1, len(P) - 2):
        p0, p1, p2, p3 = P[i - 1], P[i], P[i + 1], P[i + 2]
        for s in range(n):
            t = s / float(n)
            t2, t3 = t * t, t * t * t
            out.append(tuple(0.5 * ((2 * p1[j]) + (-p0[j] + p2[j]) * t + (2 * p0[j] - 5 * p1[j] + 4 * p2[j] - p3[j]) * t2 + (-p0[j] + 3 * p1[j] - 3 * p2[j] + p3[j]) * t3) for j in (0, 1)))
    out.append(tuple(ps[-1]))
    return out


def lerp(a, b, t):
    return a + (b - a) * t


# ====================================================================================================================== the face
def face(kind, wu=9.0, hu=8.5, ppu=16):
    """The front of the head, 9 x 8.5 units (the forehead, the eyes, the nose, the mouth, the cheeks and the chin). kind: laugh (closed eyes, open mouth, blush: the first picture),
    smile (pale green open eyes, a bright open smile: the second), stern (green narrow eyes, down-slanted brows, a small flat mouth: the powered form), shout (open eyes, open mouth: a cast);
    1.99.0 (poses and faces): smug (half-lidded, a smirk), angry (glaring, bared teeth, a red anger mark), sad (wet eyes, tears, a wide wobbly mouth: the first picture's cry), surprised (huge eyes, a small o),
    sleepy (drooping closed lids, a small yawn), pout (side-looking half-closed eyes, a sulky mouth, puffed cheeks)."""
    c = Canvas(wu, hu, ppu, SKIN)
    # the cheek shading: a soft cel shadow at the chin and the temples (hard band, no gradient)
    c.poly([(0, hu * 0.93), (wu, hu * 0.93), (wu, hu), (0, hu)], SKIN_SH)
    cx = wu / 2.0
    ey = hu * 0.52
    ex = 2.35                                                   # the eyes sit this far from the middle line
    # blush: two pink ovals under the eyes
    lay = c.layer()
    for s in (-1, 1):
        lay.c.ellipse(cx + s * 2.75, ey + 1.55, 0.95, 0.52, BLUSH)
    lay.c.blur(0.12)
    lay.apply()
    for s in (-1, 1):
        x = cx + s * ex
        if kind == "laugh":
            # a closed happy eye: a downward arc (a ^), thick at the middle, with a small lash flick at the outer end
            arc = [(x - s * 1.25, ey + 0.55), (x - s * 0.62, ey - 0.15), (x, ey - 0.45), (x + s * 0.62, ey - 0.15), (x + s * 1.2, ey + 0.55)]
            c.curve(arc, INK, 0.30)
            c.line([(x + s * 1.15, ey + 0.45), (x + s * 1.55, ey + 0.15)], INK, 0.20)
            c.curve([(x - s * 0.9, ey + 0.1), (x, ey - 0.55), (x + s * 0.9, ey + 0.1)], SKIN_SH, 0.12)
            # the brow: a thin raised arc
            c.curve([(x - s * 1.2, ey - 1.30), (x, ey - 1.75), (x + s * 1.2, ey - 1.40)], (150, 130, 80, 255), 0.17)
        elif kind in ("smile", "shout"):
            # an open eye: white, a pale green iris with a dark rim, a pupil, two catch-lights, a thick upper lid and a thin lower lid
            c.ellipse(x, ey, 1.12, 0.98, (255, 255, 255, 255))
            c.ellipse(x + s * 0.02, ey + 0.05, 0.80, 0.90, EYE_IRIS_DK)
            c.ellipse(x + s * 0.02, ey + 0.12, 0.68, 0.76, EYE_IRIS)
            c.ellipse(x + s * 0.02, ey + 0.55, 0.60, 0.30, (206, 246, 214, 255))
            c.ellipse(x + s * 0.02, ey + 0.05, 0.28, 0.42, (40, 78, 66, 255))
            c.ellipse(x - 0.28, ey - 0.38, 0.22, 0.22, (255, 255, 255, 255))
            c.ellipse(x + 0.30, ey + 0.34, 0.11, 0.11, (255, 255, 255, 255))
            c.curve([(x - s * 1.15, ey - 0.20), (x - s * 0.6, ey - 0.78), (x, ey - 0.98), (x + s * 0.7, ey - 0.78), (x + s * 1.25, ey - 0.25)], INK, 0.26)
            c.line([(x + s * 1.2, ey - 0.28), (x + s * 1.72, ey - 0.46)], INK, 0.20)
            c.curve([(x - s * 0.9, ey + 0.72), (x, ey + 0.98), (x + s * 0.9, ey + 0.72)], (150, 120, 100, 255), 0.10)
            c.curve([(x - s * 1.3, ey - 1.45), (x - s * 0.2, ey - 1.80), (x + s * 1.1, ey - 1.55)], (150, 130, 80, 255), 0.17)
        elif kind == "smug":
            # half-lidded eyes: a heavy straight upper lid over the top of the eye, the iris slid to the side, one brow cocked up and one pressed down
            c.ellipse(x, ey + 0.2, 1.10, 0.62, (255, 255, 255, 255))
            c.ellipse(x + 0.35, ey + 0.28, 0.66, 0.60, EYE_IRIS_DK)
            c.ellipse(x + 0.35, ey + 0.32, 0.54, 0.48, EYE_IRIS)
            c.ellipse(x + 0.35, ey + 0.30, 0.22, 0.32, (40, 78, 66, 255))
            c.ellipse(x + 0.05, ey + 0.08, 0.16, 0.16, (255, 255, 255, 255))
            c.poly([(x - 1.35, ey - 0.50), (x + 1.35, ey - 0.50), (x + 1.30, ey + 0.02), (x - 1.30, ey + 0.02)], SKIN)
            c.line([(x - s * 1.25, ey + 0.02), (x + s * 1.3, ey + 0.0)], INK, 0.30)
            c.line([(x + s * 1.2, ey - 0.02), (x + s * 1.66, ey - 0.16)], INK, 0.2)
            if s < 0:
                c.curve([(x - 1.2, ey - 1.05), (x, ey - 1.55), (x + 1.2, ey - 1.3)], (150, 130, 80, 255), 0.19)
            else:
                c.line([(x - 1.2, ey - 1.0), (x + 1.2, ey - 0.78)], (150, 130, 80, 255), 0.20)
        elif kind == "angry":
            # narrow glaring eyes, steep brows pressed down to the nose, a red anger mark on the forehead
            c.ellipse(x, ey + 0.14, 1.06, 0.62, (255, 255, 255, 255))
            c.ellipse(x, ey + 0.2, 0.72, 0.58, EYE_IRIS_DK)
            c.ellipse(x, ey + 0.22, 0.58, 0.46, EYE_IRIS)
            c.ellipse(x, ey + 0.22, 0.22, 0.30, (40, 78, 66, 255))
            c.curve([(x - s * 1.2, ey - 0.46), (x - s * 0.4, ey - 0.62), (x + s * 0.7, ey - 0.40), (x + s * 1.28, ey - 0.02)], INK, 0.30)
            c.line([(x - s * 1.2, ey - 1.55), (x + s * 1.25, ey - 0.62)], (110, 84, 44, 255), 0.30)
        elif kind == "sad":
            # big wet eyes: a wide iris full of catch-lights, brows pulled up in the middle, tears welling and running down the cheeks
            c.ellipse(x, ey + 0.05, 1.18, 1.08, (255, 255, 255, 255))
            c.ellipse(x, ey + 0.12, 0.92, 0.98, EYE_IRIS_DK)
            c.ellipse(x, ey + 0.16, 0.80, 0.86, EYE_IRIS)
            c.ellipse(x, ey + 0.62, 0.70, 0.34, (206, 246, 214, 255))
            c.ellipse(x, ey + 0.12, 0.30, 0.46, (40, 78, 66, 255))
            c.ellipse(x - 0.34, ey - 0.34, 0.30, 0.30, (255, 255, 255, 255))
            c.ellipse(x + 0.36, ey + 0.36, 0.14, 0.14, (255, 255, 255, 255))
            c.curve([(x - s * 1.2, ey - 0.25), (x - s * 0.5, ey - 0.82), (x + s * 0.5, ey - 1.04), (x + s * 1.3, ey - 0.4)], INK, 0.26)
            c.curve([(x - s * 1.2, ey - 1.0), (x + s * 0.1, ey - 1.48), (x + s * 1.3, ey - 1.9)], (150, 130, 80, 255), 0.17)
            tear = c.layer()
            tear.c.ellipse(x, ey + 1.05, 0.95, 0.30, (150, 205, 255, 180))
            tear.c.line([(x + s * 0.55, ey + 0.95), (x + s * 0.85, ey + 2.0), (x + s * 0.75, ey + 2.55)], (150, 205, 255, 200), 0.26)
            tear.c.ellipse(x + s * 0.75, ey + 2.62, 0.20, 0.28, (170, 215, 255, 220))
            tear.apply()
        elif kind == "surprised":
            # huge round eyes with tiny pupils, brows flung high
            c.ellipse(x, ey, 1.30, 1.22, (255, 255, 255, 255))
            c.ellipse(x, ey + 0.02, 0.62, 0.66, EYE_IRIS_DK)
            c.ellipse(x, ey + 0.04, 0.52, 0.56, EYE_IRIS)
            c.ellipse(x, ey + 0.04, 0.20, 0.26, (40, 78, 66, 255))
            c.ellipse(x - 0.20, ey - 0.20, 0.16, 0.16, (255, 255, 255, 255))
            c.curve([(x - 1.3, ey - 0.30), (x - 0.7, ey - 1.0), (x, ey - 1.22), (x + 0.7, ey - 1.0), (x + 1.3, ey - 0.30)], INK, 0.22)
            c.curve([(x - s * 1.1, ey - 1.85), (x, ey - 2.20), (x + s * 1.1, ey - 1.95)], (150, 130, 80, 255), 0.17)
        elif kind == "sleepy":
            # drooping closed lids: a soft downward arc (a u) with a few lashes, the brows slack
            arc = [(x - s * 1.2, ey - 0.05), (x - s * 0.6, ey + 0.38), (x, ey + 0.5), (x + s * 0.6, ey + 0.38), (x + s * 1.2, ey - 0.05)]
            c.curve(arc, INK, 0.26)
            c.line([(x + s * 1.1, ey + 0.05), (x + s * 1.5, ey + 0.3)], INK, 0.18)
            c.curve([(x - s * 1.3, ey - 1.15), (x, ey - 1.45), (x + s * 1.2, ey - 1.1)], (150, 130, 80, 255), 0.15)
        elif kind == "pout":
            # sulky eyes: half closed, the iris looking off to one side, brows low
            c.ellipse(x, ey + 0.2, 1.08, 0.66, (255, 255, 255, 255))
            c.ellipse(x - 0.42, ey + 0.26, 0.66, 0.62, EYE_IRIS_DK)
            c.ellipse(x - 0.42, ey + 0.30, 0.54, 0.50, EYE_IRIS)
            c.ellipse(x - 0.42, ey + 0.28, 0.22, 0.34, (40, 78, 66, 255))
            c.ellipse(x - 0.62, ey + 0.08, 0.14, 0.14, (255, 255, 255, 255))
            c.poly([(x - 1.35, ey - 0.50), (x + 1.35, ey - 0.50), (x + 1.30, ey - 0.06), (x - 1.30, ey - 0.02)], SKIN)
            c.line([(x - s * 1.25, ey - 0.04), (x + s * 1.3, ey - 0.10)], INK, 0.28)
            c.line([(x - s * 1.2, ey - 1.35), (x + s * 1.25, ey - 0.95)], (150, 130, 80, 255), 0.20)
        else:                                                   # stern
            c.ellipse(x, ey + 0.1, 1.08, 0.74, (255, 255, 255, 255))
            c.ellipse(x, ey + 0.16, 0.76, 0.70, EYE_IRIS_DK)
            c.ellipse(x, ey + 0.2, 0.62, 0.58, EYE_IRIS)
            c.ellipse(x, ey + 0.2, 0.26, 0.38, (40, 78, 66, 255))
            c.ellipse(x - 0.25, ey - 0.12, 0.18, 0.18, (255, 255, 255, 255))
            c.curve([(x - s * 1.18, ey - 0.12), (x - s * 0.4, ey - 0.50), (x + s * 0.6, ey - 0.62), (x + s * 1.25, ey - 0.40)], INK, 0.28)
            c.line([(x + s * 1.2, ey - 0.12), (x + s * 1.66, ey - 0.20)], INK, 0.2)
            c.line([(x - s * 1.0, ey - 0.95), (x + s * 1.35, ey - 1.55)], (120, 100, 60, 255), 0.22)        # the brow slants down towards the nose (the inner end is the low one)
    # the nose: a tiny dot with a short stroke
    c.curve([(cx - 0.12, ey + 1.55), (cx, ey + 1.7), (cx + 0.12, ey + 1.55)], (200, 150, 130, 255), 0.11)
    my = ey + 2.75
    if kind in ("laugh", "shout"):
        # an open mouth: a wide D shape, dark, with a pink tongue and a row of upper teeth showing as a pale band
        ww = 1.95 if kind == "laugh" else 1.15
        hh = 1.75 if kind == "laugh" else 1.5
        mouth = [(cx - ww, my - 0.35), (cx - ww * 0.8, my + hh * 0.7), (cx, my + hh), (cx + ww * 0.8, my + hh * 0.7), (cx + ww, my - 0.35)]
        c.poly(catmull(mouth, 12), (120, 48, 52, 255))
        c.line(catmull(mouth, 12) + [mouth[0]], INK, 0.22)
        c.ellipse(cx, my + hh * 0.72, ww * 0.55, hh * 0.32, (238, 120, 128, 255))
        c.poly([(cx - ww * 0.92, my - 0.30), (cx + ww * 0.92, my - 0.30), (cx + ww * 0.78, my + 0.08), (cx - ww * 0.78, my + 0.08)], (255, 255, 255, 255))
    elif kind == "smile":
        mouth = [(cx - 1.25, my - 0.25), (cx - 0.9, my + 0.5), (cx, my + 0.9), (cx + 0.9, my + 0.5), (cx + 1.25, my - 0.25)]
        c.poly(catmull(mouth, 12), (120, 48, 52, 255))
        c.line(catmull(mouth, 12) + [mouth[0]], INK, 0.2)
        c.ellipse(cx, my + 0.62, 0.62, 0.26, (238, 120, 128, 255))
        c.poly([(cx - 1.05, my - 0.22), (cx + 1.05, my - 0.22), (cx + 0.85, my + 0.1), (cx - 0.85, my + 0.1)], (255, 255, 255, 255))
    elif kind == "smug":
        # a sly smirk: a long curve that climbs on one side, a small dimple
        c.curve([(cx - 1.1, my + 0.2), (cx - 0.2, my + 0.35), (cx + 0.8, my + 0.2), (cx + 1.35, my - 0.35)], INK, 0.22)
        c.line([(cx + 1.4, my - 0.38), (cx + 1.6, my - 0.62)], INK, 0.16)
    elif kind == "angry":
        # bared teeth: a wide flat rectangle of white with a dark outline and tooth lines, the corners pulled down
        c.poly([(cx - 1.35, my - 0.15), (cx + 1.35, my - 0.15), (cx + 1.2, my + 0.55), (cx - 1.2, my + 0.55)], (255, 255, 255, 255), INK, 0.2)
        for k in range(-3, 4):
            c.line([(cx + k * 0.38, my - 0.12), (cx + k * 0.36, my + 0.52)], (150, 130, 120, 255), 0.07)
        c.line([(cx - 1.35, my - 0.15), (cx - 1.6, my + 0.15)], INK, 0.18)
        c.line([(cx + 1.35, my - 0.15), (cx + 1.6, my + 0.15)], INK, 0.18)
        c.line([(cx + 2.9, hu * 0.14), (cx + 3.4, hu * 0.14)], (230, 70, 70, 255), 0.20)            # the anger mark at the temple: a red cross
        c.line([(cx + 3.15, hu * 0.14 - 0.25), (cx + 3.15, hu * 0.14 + 0.25)], (230, 70, 70, 255), 0.20)
    elif kind == "sad":
        # a wide wobbly open mouth (the first picture's cry): dark, the corners down, the tongue pink
        mouth = [(cx - 1.9, my + 0.55), (cx - 1.3, my - 0.25), (cx, my - 0.45), (cx + 1.3, my - 0.25), (cx + 1.9, my + 0.55), (cx + 1.0, my + 1.35), (cx, my + 1.6), (cx - 1.0, my + 1.35)]
        c.poly(catmull(mouth + [mouth[0]], 10), (120, 48, 52, 255))
        c.line(catmull(mouth + [mouth[0]], 10), INK, 0.22)
        c.ellipse(cx, my + 1.12, 1.0, 0.34, (238, 120, 128, 255))
    elif kind == "surprised":
        c.ellipse(cx, my + 0.55, 0.62, 0.78, (120, 48, 52, 255), INK, 0.2)
        c.ellipse(cx, my + 0.95, 0.36, 0.30, (238, 120, 128, 255))
    elif kind == "sleepy":
        c.ellipse(cx + 0.2, my + 0.5, 0.55, 0.62, (120, 48, 52, 255), INK, 0.18)
        c.ellipse(cx + 0.2, my + 0.78, 0.30, 0.24, (238, 120, 128, 255))
    elif kind == "pout":
        # a small sulky mouth pushed to one side, the cheeks puffed (a wider blush)
        c.curve([(cx - 0.9, my + 0.35), (cx - 0.45, my + 0.05), (cx, my + 0.3), (cx + 0.45, my + 0.0), (cx + 0.9, my + 0.3)], INK, 0.20)
        lay = c.layer()
        for sd in (-1, 1):
            lay.c.ellipse(cx + sd * 3.0, ey + 1.7, 1.0, 0.8, (255, 140, 140, 90))
        lay.c.blur(0.15)
        lay.apply()
    else:
        c.curve([(cx - 0.8, my + 0.2), (cx - 0.3, my + 0.05), (cx + 0.3, my + 0.05), (cx + 0.8, my + 0.22)], INK, 0.2)
    return c.done()




def ear_card(wu=4.0, hu=2.2, ppu=16):
    """A pointed ear as a thin card: a long leaf with a pink inner shell, pointing to the left of the picture (the card is mirrored for the other side)."""
    c = Canvas(wu, hu, ppu)
    shape = [(wu * 0.02, hu * 0.30), (wu * 0.34, hu * 0.04), (wu * 0.86, hu * 0.18), (wu, hu * 0.62), (wu * 0.80, hu * 0.96), (wu * 0.30, hu * 0.86)]
    c.poly(catmull(shape, 10), SKIN)
    c.poly(catmull([(wu * 0.22, hu * 0.34), (wu * 0.44, hu * 0.24), (wu * 0.72, hu * 0.36), (wu * 0.76, hu * 0.62), (wu * 0.5, hu * 0.76), (wu * 0.3, hu * 0.62)], 8), (250, 206, 190, 255))
    c.line(catmull(shape, 10) + [shape[0]], INK, 0.12)
    return c.done()


# ====================================================================================================================== hair
def _rng(seed):
    return np.random.default_rng(seed)


def hair_block(wu, hu, ppu, seed, tone="small"):
    """A solid hair surface (the cap and the back of the head): flat colour with a hard shadow band at the bottom, a highlight arc and long strand lines, dark line work at the rim."""
    base, dark, light = HAIR[tone]
    c = Canvas(wu, hu, ppu, base)
    rng = _rng(seed)
    c.poly([(0, hu * 0.72), (wu, hu * 0.72), (wu, hu), (0, hu)], dark)
    c.curve([(wu * 0.08, hu * 0.30), (wu * 0.3, hu * 0.14), (wu * 0.62, hu * 0.14), (wu * 0.9, hu * 0.30)], light, 0.55)
    for i in range(int(wu * 1.3)):
        x = rng.uniform(0.1, wu - 0.1)
        y0 = rng.uniform(0, hu * 0.3)
        c.curve([(x, y0), (x + rng.uniform(-0.4, 0.4), y0 + hu * 0.4), (x + rng.uniform(-0.6, 0.6), hu * rng.uniform(0.85, 1.0))], INK_HAIR if i % 3 == 0 else dark, 0.09)
    return c.done()


def fringe(wu, hu, ppu, seed, tone="small", teeth=5, depth=0.45):
    """The side of the hair cap: hair colour down to a ragged edge of pointed locks (the bottom of the picture is cut out by alpha), a light arc near the top, a dark band under it, thin strand
    lines running down each lock and a dark outline along the ragged edge."""
    base, dark, light = HAIR[tone]
    c = Canvas(wu, hu, ppu)
    rng = _rng(seed)
    edge = []
    for i in range(teeth):
        x0, x1 = wu * i / teeth, wu * (i + 1) / teeth
        notch = hu * (1.0 - depth * rng.uniform(0.7, 1.0))
        tip = hu * (1.0 - rng.uniform(0.0, 0.12))
        edge += [(x0, notch), (lerp(x0, x1, 0.45 + rng.uniform(-0.15, 0.15)), tip)]
    edge.append((wu, hu * (1.0 - depth * 0.8)))
    shape = [(0, 0), (wu, 0)] + edge[::-1]
    c.poly(shape, base)
    c.poly([(0, hu * 0.55), (wu, hu * 0.55)] + [(p[0], max(p[1], hu * 0.55)) for p in edge[::-1]], dark)
    c.curve([(wu * 0.04, hu * 0.30), (wu * 0.3, hu * 0.14), (wu * 0.7, hu * 0.14), (wu * 0.96, hu * 0.30)], light, 0.42)
    for i in range(teeth):
        x = lerp(wu * i / teeth, wu * (i + 1) / teeth, 0.5)
        c.curve([(x, hu * 0.30), (x + rng.uniform(-0.2, 0.2), hu * 0.6), (x + rng.uniform(-0.2, 0.2), hu * 0.9)], INK_HAIR, 0.08)
    c.line(edge, INK, 0.12)
    c.line([(0, 0), (0, edge[0][1])], INK, 0.10, False)
    c.line([(wu, 0), (wu, edge[-1][1])], INK, 0.10, False)
    return c.done()


def strand(wu, hu, ppu, seed, tone="small", curl=0.0, ragged=1.0, tip_up=False):
    """One lock of hair as a card: a leaf that tapers to a ragged point (the notches of the anime's ragged ends), flat colour, a light centre band, a dark rim shadow, thin strand lines,
    dark outline. The root is at the top of the picture (the tip at the bottom) unless tip_up."""
    base, dark, light = HAIR[tone]
    c = Canvas(wu, hu, ppu)
    rng = _rng(seed)
    n = 14
    left, right = [], []
    for i in range(n + 1):
        t = i / float(n)
        w = (0.62 + 0.38 * math.sin(min(1.0, t * 4.0) * math.pi * 0.5)) * (1.0 - t ** 1.5) * wu * 0.5
        mid = wu * 0.5 + math.sin(t * math.pi) * curl * wu * 0.3
        left.append((mid - w, t * hu))
        right.append((mid + w, t * hu))
    # ragged end: the last third has a few jagged teeth
    teeth = []
    for i in range(3):
        x = wu * (0.28 + 0.22 * i) + rng.uniform(-0.1, 0.1) * wu * ragged
        teeth.append((x, hu * (0.97 - 0.05 * rng.random() * ragged)))
        teeth.append((x + wu * 0.1, hu * (0.82 - 0.04 * rng.random() * ragged)))
    outline = left + [(wu * 0.5 - wu * 0.04, hu * 0.99)] + [(p[0] + 0, p[1]) for p in reversed(right)]
    c.poly(outline, base)
    # shadow rim on the right edge and a light band down the middle
    inner = [(p[0] * 0.55 + wu * 0.5 * 0.45 + wu * 0.06, p[1]) for p in right] + [(p[0], p[1]) for p in reversed(right)]
    c.poly(inner, dark)
    mid_band = [(wu * 0.36 + (left[i][0] - wu * 0.36) * 0.1, left[i][1]) for i in range(2, n - 2)]
    c.curve([(wu * 0.42, hu * 0.12), (wu * 0.4 + curl * 0.3, hu * 0.45), (wu * 0.47, hu * 0.78)], light, wu * 0.12)
    for k in range(3):
        x = wu * (0.3 + 0.18 * k)
        c.curve([(x, hu * 0.05), (x + rng.uniform(-0.3, 0.3), hu * 0.45), (x + rng.uniform(-0.5, 0.5), hu * 0.9)], INK_HAIR if k == 1 else dark, 0.08)
    c.line(left + [(wu * 0.5, hu * 0.995)], INK, 0.11)
    c.line(right + [(wu * 0.5, hu * 0.995)], INK, 0.11)
    img = c.done()
    if tip_up:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    return img




# ====================================================================================================================== dress and body
def skin_plain(wu, hu, ppu, edges=True, toes=False, shade_bottom=0.08):
    """Flat cel skin for a limb face: cream, a hard shadow band along the bottom, a dark line down both edges (the outline of the limb)."""
    c = Canvas(wu, hu, ppu, SKIN)
    if shade_bottom > 0:
        c.poly([(0, hu * (1 - shade_bottom)), (wu, hu * (1 - shade_bottom)), (wu, hu), (0, hu)], SKIN_SH)
    if edges:
        c.line([(0.03, 0), (0.03, hu)], INK, 0.10, False)
        c.line([(wu - 0.03, 0), (wu - 0.03, hu)], INK, 0.10, False)
    if toes:
        for i in range(5):
            x = wu * (0.1 + 0.2 * i)
            c.line([(x, hu * 0.70), (x, hu)], INK, 0.07, False)
    return c.done()


def skin_flat(wu, hu, ppu):
    c = Canvas(wu, hu, ppu, SKIN)
    return c.done()


def ruffle(wu, hu, ppu, seed, tone="small", scallop=True, waves=3, fold_lines=True, edge_glow=False, translucent=0):
    """A ruffled dress panel (a tier of the skirt, a petal): the colour of the dress with a cel shadow band, long fold lines, a darker underside and a SCALLOPED / wavy hem cut by alpha
    (the hem is a row of rounded arcs, each with a dark line). translucent 0..1: the panel's alpha (the powered form's petal wings are see-through)."""
    base, dark, light = DRESS[tone]
    c = Canvas(wu, hu, ppu)
    rng = _rng(seed)
    n = max(2, int(waves))
    hem = hu * 0.88
    arcs = []
    for i in range(n):
        x0, x1 = wu * i / n, wu * (i + 1) / n
        for s in range(9):
            t = s / 8.0
            x = x0 + (x1 - x0) * t
            y = hem + math.sin(t * math.pi) * (hu - hem) * (0.9 if scallop else 0.3) * (1.0 + rng.uniform(-0.1, 0.1))
            arcs.append((x, y))
    outline = [(0, 0), (wu, 0)] + [(wu, hem * 0.6)] + arcs[::-1] + [(0, hem * 0.6)]
    c.poly(outline, base)
    # the cel shading: every wave has a shadow on its right half, and the band under the waist
    for i in range(n):
        x0, x1 = wu * i / n, wu * (i + 1) / n
        c.poly([(x0 + (x1 - x0) * 0.55, hu * 0.12), (x1, hu * 0.12), (x1, hem + (hu - hem) * 0.6), (x0 + (x1 - x0) * 0.6, hem + (hu - hem) * 0.9)], dark)
        c.curve([(x0 + (x1 - x0) * 0.3, hu * 0.06), (x0 + (x1 - x0) * 0.25, hu * 0.5), (x0 + (x1 - x0) * 0.33, hem)], light, max(0.18, (x1 - x0) * 0.12))
    c.poly([(0, 0), (wu, 0), (wu, hu * 0.14), (0, hu * 0.14)], dark)
    if fold_lines:
        for i in range(n):
            x0, x1 = wu * i / n, wu * (i + 1) / n
            for f in (0.2, 0.62):
                c.curve([(x0 + (x1 - x0) * f, hu * 0.14), (x0 + (x1 - x0) * (f + rng.uniform(-0.05, 0.05)), hu * 0.5), (x0 + (x1 - x0) * (f + 0.04), hem)], INK_GREEN, 0.07)
    c.line(arcs, INK_GREEN, 0.12)
    img = c.done()
    if translucent:
        a = np.asarray(img).copy()
        a[..., 3] = (a[..., 3] * (1.0 - translucent)).astype(np.uint8)
        img = Image.fromarray(a, "RGBA")
    return img


def bodice(wu, hu, ppu, tone="small", back=False):
    """The chest front: bare skin above a scalloped strapless neckline, two shell cups with a fold line each, a dark seam; the back is plain dress with a bow-less closure line."""
    base, dark, light = DRESS[tone]
    c = Canvas(wu, hu, ppu, SKIN)
    top = hu * (0.34 if not back else 0.22)
    # dress area below the neckline (scallops: one per half)
    neck = []
    for i in range(2):
        x0, x1 = wu * i / 2, wu * (i + 1) / 2
        for s in range(9):
            t = s / 8.0
            neck.append((lerp(x0, x1, t), top - math.sin(t * math.pi) * hu * 0.10))
    c.poly(neck + [(wu, hu), (0, hu)], base)
    c.poly([(0, hu * 0.68), (wu, hu * 0.68), (wu, hu), (0, hu)], dark)
    c.poly(neck + [(wu, top + hu * 0.18), (0, top + hu * 0.18)], light)
    c.line(neck, INK_GREEN, 0.12)
    if not back:
        c.curve([(wu * 0.25, top + hu * 0.04), (wu * 0.24, hu * 0.55), (wu * 0.3, hu * 0.7)], INK_GREEN, 0.07)
        c.curve([(wu * 0.75, top + hu * 0.04), (wu * 0.76, hu * 0.55), (wu * 0.7, hu * 0.7)], INK_GREEN, 0.07)
        c.line([(wu * 0.5, top), (wu * 0.5, hu)], INK_GREEN, 0.08, False)
    else:
        c.line([(wu * 0.5, top), (wu * 0.5, hu)], INK_GREEN, 0.07, False)
    return c.done()


def dress_flat(wu, hu, ppu, tone="small", band=0.25):
    base, dark, light = DRESS[tone]
    c = Canvas(wu, hu, ppu, base)
    c.poly([(0, hu * (1 - band)), (wu, hu * (1 - band)), (wu, hu), (0, hu)], dark)
    c.line([(0.03, 0), (0.03, hu)], INK_GREEN, 0.08, False)
    c.line([(wu - 0.03, 0), (wu - 0.03, hu)], INK_GREEN, 0.08, False)
    return c.done()


def choker(wu, hu, ppu):
    c = Canvas(wu, hu, ppu, CHOKER)
    for i in range(int(wu * 1.6)):
        x = (i + 0.5) / 1.6
        c.line([(x, 0), (x + 0.1, hu)], (62, 48, 44, 255), 0.07, False)
    return c.done()




# ====================================================================================================================== wings
def wing(wu, hu, ppu, seed, kind="fore", tone="small", glow=False, powered=False):
    """A dragonfly wing seen from above: root at the left of the picture, tip at the right, the leading edge on the top. A long leaf with a rounded tip. The membrane is nearly clear with
    a soft iridescent sheen (green, blue, pink, gold bands), a network of cell veins (long veins out of the root, cross veins between them), a darker leading edge and a thin outline.
    glow=True returns the glow map instead (the veins and the leading edge, additive)."""
    rng = _rng(seed)
    c = Canvas(wu, hu, ppu)
    # the outline: leading edge nearly straight, the trailing edge curved, a rounded tip
    top = [(0.0, hu * 0.30), (wu * 0.25, hu * 0.14), (wu * 0.55, hu * 0.10), (wu * 0.82, hu * 0.20), (wu * 0.97, hu * 0.42)]
    bot = [(wu, hu * 0.52), (wu * 0.92, hu * 0.72), (wu * 0.72, hu * 0.88), (wu * 0.42, hu * 0.94), (wu * 0.14, hu * 0.80), (0.0, hu * 0.58)]
    shape = catmull(top + bot, 14)
    mask = Canvas(wu, hu, ppu)
    mask.poly(shape, (255, 255, 255, 255))
    m = np.asarray(mask.done())[..., 3].astype(np.float32) / 255.0
    H, W = m.shape
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u, v = xx / max(1, W - 1), yy / max(1, H - 1)
    # the membrane: a sheen that wanders in bands (thin-film colours), strongest towards the tip
    band = 0.5 + 0.5 * np.sin(u * 7.0 + v * 4.0 + rng.uniform(0, 6))
    band2 = 0.5 + 0.5 * np.sin(u * 3.2 - v * 5.5 + rng.uniform(0, 6))
    if tone == "small":
        g = np.array([196, 240, 190], np.float32)
        b = np.array([186, 226, 246], np.float32)
        p = np.array([246, 206, 240], np.float32)
        y_ = np.array([250, 244, 176], np.float32)
    else:
        g = np.array([120, 226, 110], np.float32)
        b = np.array([130, 214, 190], np.float32)
        p = np.array([186, 240, 120], np.float32)
        y_ = np.array([214, 246, 110], np.float32)
    col = g[None, None] * (1 - band[..., None]) + b[None, None] * band[..., None]
    col = col * (1 - 0.55 * band2[..., None]) + p[None, None] * (0.55 * band2[..., None])
    col = col * (1 - 0.35 * (u[..., None] ** 2)) + y_[None, None] * (0.35 * (u[..., None] ** 2))
    alpha = np.full((H, W), 0.42 if tone == "small" else 0.58, np.float32)
    alpha += 0.10 * band + 0.10 * (1 - v) * 0.0
    vein_img = Canvas(wu, hu, ppu)
    ink = (98, 134, 108, 255) if tone == "small" else (60, 130, 80, 255)
    # long veins from the root, fanning out and curving towards the tip
    nl = 4 if tone == "powered" else 5                                        # a few long veins: the cells of the pictures are big
    for i in range(nl):
        t = (i + 0.5) / nl
        y0 = lerp(hu * 0.34, hu * 0.56, t)
        y1 = lerp(hu * 0.16, hu * 0.80, t) + (t - 0.5) * hu * 0.08
        vein_img.curve([(0.0, y0), (wu * 0.3, lerp(y0, y1, 0.35)), (wu * 0.68, lerp(y0, y1, 0.78)), (wu * (0.93 - 0.05 * abs(t - 0.5)), y1)], ink, (0.10 if tone == "powered" else 0.08) if i not in (0, nl - 1) else 0.14)
    # cross veins: short links between neighbouring long veins make the cells
    ncross = 5 if tone == "powered" else 7
    for k in range(ncross):
        x = wu * (0.14 + (0.78 / ncross) * k)
        for i in range(nl - 1):
            ya = lerp(lerp(hu * 0.34, hu * 0.56, (i + 0.5) / nl), lerp(hu * 0.16, hu * 0.80, (i + 0.5) / nl), min(1.0, x / wu * 0.9))
            yb = lerp(lerp(hu * 0.34, hu * 0.56, (i + 1.5) / nl), lerp(hu * 0.16, hu * 0.80, (i + 1.5) / nl), min(1.0, x / wu * 0.9))
            j = rng.uniform(-0.02, 0.02) * wu
            vein_img.line([(x + j, ya), (x + j + wu * 0.02, yb)], ink, 0.09 if tone == "powered" else 0.06, False)
    vimg = np.asarray(vein_img.done()).astype(np.float32)
    va = vimg[..., 3] / 255.0
    edge = Canvas(wu, hu, ppu)
    edge.line(catmull(top, 14), ink, 0.16)
    edge.line(catmull(top[-2:] + bot, 14), ink, 0.10)
    ea = np.asarray(edge.done()).astype(np.float32)[..., 3] / 255.0
    if glow:
        g = np.zeros((H, W, 4), np.float32)
        gc = np.array([170, 255, 160], np.float32) if tone == "small" else np.array([120, 255, 120], np.float32)
        a = np.clip((va * 0.9 + ea * 0.7) * m, 0, 1)
        g[..., :3] = gc
        g[..., 3] = a * 255
        g[..., :3] *= (a > 0.01)[..., None]
        return Image.fromarray(np.clip(g, 0, 255).astype(np.uint8), "RGBA")
    out = np.zeros((H, W, 4), np.float32)
    out[..., :3] = col
    out[..., 3] = alpha * 255 * m
    # the veins and the rim are drawn over the membrane, more opaque
    ink_rgb = np.array(ink[:3], np.float32)
    a_v = np.clip(va * 0.80 + ea * 0.95, 0, 1) * m
    out[..., :3] = out[..., :3] * (1 - a_v[..., None]) + ink_rgb[None, None] * a_v[..., None]
    out[..., 3] = np.maximum(out[..., 3], a_v * 255 * (0.85 if tone == "small" else 0.9))
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


# ====================================================================================================================== glow pictures (additive, safe: nothing under a transparent texel)
def face_glow(kind, wu=9.0, hu=8.5, ppu=16):
    """The glow of the face: the irises of an open eye (a soft green) and the cheeks; nothing for a closed eye (a faint warm line)."""
    c = Canvas(wu, hu, ppu)
    ey = hu * 0.52
    cx = wu / 2.0
    for s in (-1, 1):
        x = cx + s * 2.35
        if kind in ("smile", "shout", "stern", "smug", "angry", "sad", "surprised", "pout"):
            c.ellipse(x, ey + 0.12, 0.62, 0.66, (150, 255, 170, 255))
        else:
            c.curve([(x - s * 1.1, ey + 0.5), (x, ey - 0.4), (x + s * 1.1, ey + 0.5)], (255, 236, 200, 200), 0.22)
    c.blur(0.10)
    img = c.done()
    a = np.asarray(img).copy()
    a[a[..., 3] < 6] = 0
    return Image.fromarray(a, "RGBA")


def hem_glow(wu, hu, ppu, seed, tone="small"):
    """The glow of a skirt panel: the scalloped hem and the fold lines, a bright pale green."""
    base, dark, light = DRESS[tone]
    c = Canvas(wu, hu, ppu)
    n = max(2, int(round(wu / 2.0)))
    hem = hu * 0.88
    arcs = []
    for i in range(n):
        x0, x1 = wu * i / n, wu * (i + 1) / n
        for s in range(9):
            t = s / 8.0
            arcs.append((lerp(x0, x1, t), hem + math.sin(t * math.pi) * (hu - hem) * 0.9))
    c.line(arcs, (200, 255, 190, 255), 0.30)
    c.line(arcs, (255, 255, 240, 255), 0.12)
    c.poly([(0, hu * 0.80), (wu, hu * 0.80), (wu, hem), (0, hem)], (170, 250, 160, 120))
    c.blur(0.08)
    img = c.done()
    a = np.asarray(img).copy()
    a[a[..., 3] < 6] = 0
    return Image.fromarray(a, "RGBA")




def halo(size=128):
    """The soft round halo behind the Sylph (a smooth radial falloff, pale green to white at the heart), additive; nothing under a transparent texel."""
    ys, xs = np.mgrid[0:size, 0:size].astype(np.float32)
    r = np.sqrt(((xs + 0.5) / size - 0.5) ** 2 + ((ys + 0.5) / size - 0.5) ** 2) * 2.0
    a = np.clip(1.0 - r, 0.0, 1.0)
    a = a * a * (3.0 - 2.0 * a)
    a = a ** 1.6
    out = np.zeros((size, size, 4), np.float32)
    k = np.clip(1.0 - r * 1.4, 0.0, 1.0)
    out[..., 0] = (150 + 105 * k) * a                                              # the colour fades to black with the alpha: a plain additive blend is as soft as an alpha-weighted one
    out[..., 1] = 255 * a
    out[..., 2] = (150 + 95 * k) * a
    out[..., 3] = a * 255
    out[out[..., 3] < 3] = 0
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


# ====================================================================================================================== the powered form: streaming hair and petal wings
def ribbon(wu, hu, ppu, seed, tone="powered", phase=0.0, sway=1.0, tips=1):
    """A long lock of streaming hair as a standing card: the ROOT at the bottom of the picture, the tip at the top. The silhouette follows an S-curve (it sways to one side and back), swells in
    the middle and ends in a few ragged points; flat colour, a dark shadow along the right side, a light band, long strand lines that follow the curve, a dark outline."""
    base, dark, light = HAIR[tone]
    c = Canvas(wu, hu, ppu)
    rng = _rng(seed)
    n = 40

    def centre(t):                                           # t = 0 at the root (bottom) .. 1 at the tip (top)
        return wu * 0.5 + math.sin(t * math.pi * 2.2 + phase) * wu * 0.22 * sway * min(1.0, t * 2.5)

    def half(t):
        return wu * 0.5 * (0.62 + 0.38 * math.sin(min(1.0, t * 2.0) * math.pi * 0.5)) * (1.0 - t ** 2.4) * 0.96

    left, right = [], []
    for i in range(n + 1):
        t = i / float(n)
        y = hu * (1.0 - t)
        left.append((centre(t) - half(t), y))
        right.append((centre(t) + half(t), y))
    pts = left + right[::-1]
    tipx = centre(1.0)
    c.poly(pts, base)
    # the ragged end: a few pointed locks, each a thin triangle
    for k in range(tips):
        t0 = 0.80 + 0.04 * k
        tx = centre(t0) + (k - (tips - 1) / 2.0) * wu * 0.18
        ty = hu * (1.0 - t0 - 0.16 - 0.04 * rng.random())
        c.poly([(centre(t0) - wu * 0.10, hu * (1.0 - t0)), (tx + wu * 0.02, max(0.0, ty)), (centre(t0) + wu * 0.10, hu * (1.0 - t0))], base)
    # the shadow side (the right third) and the light band
    c.poly([(centre(i / float(n)) + half(i / float(n)) * 0.25, hu * (1.0 - i / float(n))) for i in range(n - 3)] + [(centre((n - 3) / float(n)) + half((n - 3) / float(n)), hu * (1.0 - (n - 3) / float(n)))] +
           [(centre(i / float(n)) + half(i / float(n)), hu * (1.0 - i / float(n))) for i in range(n - 3, -1, -1)], dark)
    c.curve([(centre(t) - half(t) * 0.35, hu * (1.0 - t)) for t in (0.04, 0.2, 0.4, 0.6, 0.75)], light, wu * 0.07)
    for k in range(4):
        off = (k - 1.5) * 0.28
        c.curve([(centre(t) + half(t) * off, hu * (1.0 - t)) for t in (0.02, 0.2, 0.4, 0.6, 0.8 - 0.05 * k)], INK_HAIR if k % 2 == 0 else dark, 0.09)
    c.line(left[:-3], INK, 0.11)
    c.line(right[:-3], INK, 0.11)
    return c.done()


def petal(wu, hu, ppu, seed, tone="powered", glow=False, alpha=0.62):
    """A petal-wing of the skirt (the layered petal ruffle that stands out behind her in the powered form): a fan, narrow at the root (the left of the picture), ending in a wide scalloped
    edge; pale mint, translucent, radiating fold lines, a darker underside near the root, a bright rim along the scallops. glow=True gives the glow map (the rim and the folds)."""
    base, dark, light = DRESS[tone]
    rng = _rng(seed)
    c = Canvas(wu, hu, ppu)
    n = 4
    outer = []
    for i in range(n):
        a0, a1 = -0.5 + i / float(n), -0.5 + (i + 1) / float(n)
        for s in range(10):
            t = s / 9.0
            a = lerp(a0, a1, t)
            r = 0.80 + 0.20 * math.sin(t * math.pi) * (0.9 + 0.2 * rng.random())
            outer.append((wu * (0.10 + 0.86 * r * math.cos(a * 1.9)), hu * (0.5 + 0.56 * r * math.sin(a * 1.9))))
    shape = [(0.0, hu * 0.46), (wu * 0.10, hu * 0.42)] + outer + [(wu * 0.10, hu * 0.58), (0.0, hu * 0.54)]
    if glow:
        c.line(outer, (190, 255, 190, 255), 0.30)
        c.line(outer, (255, 255, 245, 255), 0.12)
        for k in range(1, n):
            a = -0.5 + k / float(n)
            c.line([(wu * 0.12, hu * 0.5), (wu * (0.10 + 0.70 * math.cos(a * 1.9)), hu * (0.5 + 0.50 * math.sin(a * 1.9)))], (170, 250, 170, 200), 0.12)
        c.blur(0.07)
        img = c.done()
        a = np.asarray(img).copy()
        a[a[..., 3] < 6] = 0
        return Image.fromarray(a, "RGBA")
    c.poly(shape, base)
    c.poly([(wu * 0.05, hu * 0.44), (wu * 0.42, hu * 0.20), (wu * 0.42, hu * 0.80), (wu * 0.05, hu * 0.56)], mix(base, dark, 0.5))
    for k in range(1, n):
        a = -0.5 + k / float(n)
        c.line([(wu * 0.12, hu * 0.5), (wu * (0.10 + 0.82 * math.cos(a * 1.9)), hu * (0.5 + 0.55 * math.sin(a * 1.9)))], INK_GREEN, 0.07)
    c.line(outer, INK_GREEN, 0.13)
    img = c.done()
    a = np.asarray(img).copy()
    a[..., 3] = (a[..., 3] * alpha).astype(np.uint8)
    return Image.fromarray(a, "RGBA")

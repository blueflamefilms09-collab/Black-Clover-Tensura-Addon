"""The skin of Kakashi Hatake on the Black Clover player-model kit (docs/bosses/kakashi.md): a 256x256 skin painted from scratch (the kit's 64x64 layout at 4x4 texels per pixel), the same way the
devil skins are (dvskin.py), with the anime face of bcskin.paint_face and Kakashi's own details on top of it.

What is painted here (the cube hair, the hitai-ate, the mask cloth, the pockets and the guards are cubes of kkparts.py, drawn over it):
  * head: pale skin, the silver hair roots on top / back, the lower half of the face and the sides under the mask (navy cloth with folds and a nose ridge), the lazy right eye, the left eye as
    Sharingan (3 tomoe) or Mangekyo (the Kamui pinwheel) with the vertical scar through it, glowing on the glow map;
  * body: dark blue shirt, the green flak jacket with its pockets, zip and high collar, the leather belt;
  * arms: dark blue sleeves, black fingerless gloves with a steel back plate; legs: dark blue trousers, sandals.
Every painted pixel is a model pixel of the kit; coordinates of a face are the hd_core ones (see tools/tier_bosses/hd/hd_core.py).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import math  # noqa: E402

import numpy as np  # noqa: E402

import bclib as L  # noqa: E402,F401  (path setup)
import bcskin as S  # noqa: E402

import hd_core as C  # noqa: E402
import hd_detail as H  # noqa: E402

SKIN = ("#d2ab94", "#efcdb6", "#ffe6d2")           # shadow, base, light
HAIR = "#cdd3e2"
SHIRT = "#222c4c"                                  # the shinobi blue-black of the shirt, trousers and mask
SHIRT_D = "#161d36"
VEST = "#5b6a4c"                                   # the Konoha flak jacket
VEST_D = "#3f4a35"
VEST_L = "#79896a"
LEATHER = "#3a2c24"
STEEL = "#b4becd"
GLOVE = "#15151f"
SAND = "#26304e"


def _skin(f, sk=SKIN):
    f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, sk[2]), (0.5, sk[1]), (1, sk[0])]))


# ================================================================================================ the eyes
def _sharingan(f, cx, cy, w=2.75, h=2.3, tilt=0.06, mangekyo=False):
    """The left eye: an almond of the eye pair's shape with a red iris. Sharingan: black pupil, a thin black ring, three tomoe. Mangekyo (Kamui): a black three-bladed pinwheel on the red."""
    red = ("#5a0610", "#d81828", "#ff7a7a")
    H.eye(f, cx, cy, w, h, red, lash="#1c1226", sclera="#f4eeea", tilt=tilt, pupil="#0c0814", iris_r=1.05, glow="#ff2a30", hi=0.55, lid_shadow=0.34, lash_w=0.38, flick=0.3)
    ix, iy = cx, cy + h * 0.04
    r = w * 0.34 * 1.05
    # redraw the iris centre over the eye's own pupil: the pattern sits on a clean red disc
    disc = f.m_ellipse(ix, iy, r * 0.93, h * 0.5 * 1.05 * 0.9)
    f.fill(disc, H.lin((ix, iy - r), (ix, iy + r), [(0, "#9a0c1a"), (0.5, "#e01c2c"), (1, "#ff5a4a")]), 1.0)
    f.glow_fill(disc, "#ff2a30", 0.9)
    f.fill(f.m_ring(ix, iy, r * 1.6, 0.16) * disc, "#2a0408", 0.8)                  # the limbal ring
    if not mangekyo:
        f.ellipse(ix, iy, 0.2, 0.22, "#0a0408")                                      # the pupil
        for k in range(3):                                                           # three tomoe: a dot with a short tail, turning
            a = -math.pi / 2 + k * 2 * math.pi / 3
            px, py = ix + math.cos(a) * r * 0.62, iy + math.sin(a) * r * 0.62 * 0.9
            f.ellipse(px, py, 0.15, 0.15, "#0a0408")
            ta = a + math.pi * 0.62
            f.line((px, py), (px + math.cos(ta) * 0.26, py + math.sin(ta) * 0.24), "#0a0408", 0.16, 0.02)
        f.fill(f.m_ring(ix, iy, r * 1.15, 0.07) * disc, "#0a0408", 0.55)
    else:                                                                            # Kamui: three curved blades round a black core, drawn as swept triangles
        for k in range(3):
            a = -math.pi / 2 + k * 2 * math.pi / 3
            tip = (ix + math.cos(a + 0.55) * r * 1.0, iy + math.sin(a + 0.55) * r * 0.92)
            base1 = (ix + math.cos(a - 0.35) * r * 0.22, iy + math.sin(a - 0.35) * r * 0.2)
            base2 = (ix + math.cos(a + 1.5) * r * 0.30, iy + math.sin(a + 1.5) * r * 0.28)
            mid = (ix + math.cos(a + 0.05) * r * 0.82, iy + math.sin(a + 0.05) * r * 0.76)
            f.poly([base1, mid, tip, base2], "#0a0408")
        f.ellipse(ix, iy, 0.2, 0.2, "#0a0408")
        f.glow_fill(f.m_ellipse(ix, iy, r * 0.5, r * 0.5), "#ff8a50", 0.5)
    f.ellipse(ix - r * 0.42, iy - h * 0.2, 0.12, 0.1, "#ffffff", a=0.75)              # a catch light
    return disc


def _lazy_eye(f, cx, cy):
    """The right eye: a half-lidded charcoal eye (Kakashi's bored look)."""
    H.eye(f, cx, cy, 2.45, 1.62, ("#16141c", "#34323e", "#7c7a8a"), lash="#1c1226", sclera="#f4f0ee", tilt=-0.03, pupil="#08060c", iris_r=1.0, hi=0.6, lid_shadow=0.5, lash_w=0.4, flick=0.26)
    f.polyline([(cx - 1.3, cy - 0.78), (cx, cy - 0.64), (cx + 1.3, cy - 0.76)], "#1c1226", 0.2, a=0.55)       # the heavy upper lid


def _scar(f, cx=5.62):
    """The vertical scar through the left eye."""
    pts = [(cx - 0.12, 3.35), (cx + 0.02, 4.6), (cx - 0.05, 5.5), (cx + 0.1, 6.45)]
    f.polyline(pts, "#a8505a", 0.2, a=0.95)
    f.polyline([(x - 0.06, y) for x, y in pts], "#e8a8a8", 0.07, a=0.55)
    f.polyline([(x + 0.13, y) for x, y in pts], "#6a2a34", 0.06, a=0.5)


# ================================================================================================ the mask
def _mask(f):
    """The mask of the lower face on the front of the head: dark blue cloth from the nose bridge down, folds along the nose and a lit ridge."""
    top = [(-0.1, 5.62), (1.0, 5.5), (2.4, 5.62), (3.3, 5.3), (3.7, 4.6), (4.3, 4.6), (4.7, 5.3), (5.6, 5.62), (7.0, 5.5), (8.1, 5.62), (8.1, 8.1), (-0.1, 8.1)]
    m = f.m_poly(top, soft=1.2)
    H.cloth(f, m, SHIRT, top=1.18, bottom=0.78, fold_xs=[1.3, 6.7], fold_depth=0.09, fold_y0=5.0)
    f.polyline([(3.7, 4.65), (3.55, 5.2), (2.5, 5.62), (1.0, 5.52), (-0.1, 5.62)], H.tone(SHIRT, 0.55), 0.13, a=0.75)       # the upper edge, in shade
    f.polyline([(4.3, 4.65), (4.45, 5.2), (5.5, 5.62), (7.0, 5.52), (8.1, 5.62)], H.tone(SHIRT, 0.55), 0.13, a=0.75)
    f.fill(f.m_poly([(3.78, 4.7), (4.22, 4.7), (4.3, 5.5), (3.7, 5.5)], soft=1.5) * m, H.tone(SHIRT, 1.55), 0.55)           # the lit nose ridge
    f.fill(f.m_ellipse(4.0, 6.9, 2.3, 1.0, soft=3.0) * m, H.tone(SHIRT, 0.6), 0.28)                                          # the mouth sinks the cloth a little
    f.polyline([(2.4, 6.1), (3.4, 5.9)], H.tone(SHIRT, 0.55), 0.09, a=0.5)
    f.polyline([(5.6, 6.1), (4.6, 5.9)], H.tone(SHIRT, 0.55), 0.09, a=0.5)
    f.shade(f.m_rect(0, 7.2, 8, 8.1) * m, 0.82)                                                                               # under the chin


# ================================================================================================ the head
def _head(cv, eye):
    mangekyo = eye == "mangekyo"
    for nm in ("top", "back", "west", "east", "bottom"):
        f = cv.face("head", nm, False)
        _skin(f)
        if nm == "bottom":
            H.cloth(f, f.m_all(), SHIRT, top=1.0, bottom=0.8, weave_k=0.0)
            continue
        hm = f.m_all()
        if nm in ("west", "east"):
            x0, x1 = (4.0, 8.0) if nm == "west" else (0.0, 4.0)                   # the face-side half of a side face is not hair: the cheek and the mask
            hm = np.clip(hm - f.m_poly([(x0, 1.9), (x1, 1.9), (x1, 8.2), (x0, 8.2)], soft=1.5), 0, 1)
            hm = hm * (1.0 - f.m_rect(0.0, 6.0, 8.0, 8.0, soft=2.0) * 0.85)
        elif nm == "back":
            hm = hm * (1.0 - f.m_rect(0.0, 6.2, 8.0, 8.0, soft=2.0) * 0.9)
        if nm != "bottom":
            f.fill(hm, H.lin((0, 0), (0, f.h), [(0, H.tone(HAIR, 1.12)), (0.6, HAIR), (1, H.tone(HAIR, 0.7))]))
        if nm in ("west", "east"):                                                # the mask runs round the jaw to the ear
            xa, xb = (3.2, 8.2) if nm == "west" else (-0.2, 4.8)
            mm = f.m_poly([(xa, 4.6), (xb, 4.6), (xb, 8.2), (xa, 8.2)], soft=1.3)
            H.cloth(f, mm, SHIRT, top=1.15, bottom=0.8, fold_xs=[5.0], weave_k=0.0)
            edge = 3.4 if nm == "west" else 4.6
            f.polyline([(edge, 4.7), (edge, 8.0)], H.tone(SHIRT, 0.5), 0.12, a=0.7)
    f = cv.face("head", "front")
    f.clear()
    H.skin_face(f, SKIN, hair=HAIR, hairline=1.5, side=0.10, chin=0.06, shadow=0.20, wave=0.3)
    f.shade(f.m_ellipse(0.6, 6.9, 1.0, 1.4, soft=3.0), 0.9, 0.7)
    f.shade(f.m_ellipse(7.4, 6.9, 1.0, 1.4, soft=3.0), 0.9, 0.7)
    for cx, sgn in ((2.35, -1), (5.65, 1)):                                       # the brows: silver, low, relaxed
        H.brow(f, (cx + (1.45 if sgn < 0 else -1.45), 3.55), (cx + (-1.45 if sgn < 0 else 1.45) * 0.9, 3.4), "#9aa2b6", 0.24, 0.11, arch=0.1)
    _lazy_eye(f, 2.35, 4.95)
    _sharingan(f, 5.65, 4.95, mangekyo=mangekyo)
    _scar(f)
    _mask(f)


# ================================================================================================ the body
def _torso(cv):
    for nm in ("front", "back", "west", "east", "top", "bottom"):
        f = cv.face("body", nm, False)
        _skin(f)
        H.cloth(f, f.m_all(), SHIRT, top=1.12, bottom=0.8, fold_xs=[2.0, 6.0] if nm in ("front", "back") else [2.0], weave_k=0.025)
        if nm in ("top", "bottom"):
            continue
        if nm in ("front", "back"):
            vest = f.m_rect(0.0, 0.0, f.w, 9.2)
            H.cloth(f, vest, VEST, top=1.2, bottom=0.82, fold_xs=[2.2, 5.8], fold_depth=0.08, weave_k=0.03)
            f.polyline([(0.0, 9.1), (8.0, 9.1)], H.tone(VEST, 0.5), 0.14, a=0.7)
        else:
            vest = f.m_rect(0.0, 0.0, f.w, 9.2)
            H.cloth(f, vest, VEST, top=1.15, bottom=0.8, weave_k=0.03)
        if nm == "front":
            f.fill(f.m_poly([(2.9, -0.1), (5.1, -0.1), (4.0, 1.9)], soft=1.2), SHIRT)                       # the V of the collar, the mask cloth shows
            f.polyline([(4.0, 1.7), (4.0, 9.2)], H.tone(VEST, 0.45), 0.12, a=0.9)                              # the zip
            f.polyline([(4.12, 1.9), (4.12, 9.0)], H.tone(VEST, 1.45), 0.05, a=0.55)
            for (x0, y0, x1, y1) in ((0.7, 2.6, 3.4, 4.9), (4.6, 2.6, 7.3, 4.9), (0.7, 5.5, 3.4, 8.4), (4.6, 5.5, 7.3, 8.4)):    # the four pockets: a flap, a seam and a stud
                pk = f.m_rect(x0, y0, x1, y1, r=0.12)
                f.fill(f.m_rect(x0 - 0.05, y0 - 0.05, x1 + 0.05, y1 + 0.08, r=0.12), H.tone(VEST_D, 0.7), 0.85)
                H.cloth(f, pk, VEST_L if y0 < 4 else VEST, top=1.14, bottom=0.86, weave_k=0.02)
                f.polyline([(x0 + 0.1, y0 + 0.9), (x1 - 0.1, y0 + 0.9)], H.tone(VEST_D, 0.8), 0.1, a=0.8)
                H.stud(f, (x0 + x1) / 2, y0 + 0.9 + 0.35, 0.16, STEEL)
            f.fill(f.m_rect(-0.1, 8.7, 8.1, 9.4) * f.m_rect(0, 0, 8, 12), H.tone(VEST_D, 0.8), 0.3)
        if nm == "back":
            f.polyline([(4.0, 0.0), (4.0, 9.2)], H.tone(VEST, 0.5), 0.12, a=0.8)
            f.rect(1.6, 4.0, 6.4, 5.0, H.tone(VEST_D, 0.9), a=0.55)
        H.trim_band(f, 9.4, 10.9, LEATHER, spec=0.25)
        if nm == "front":
            H.trim_band(f, 9.55, 10.75, STEEL, x0=3.3, x1=4.7, spec=0.5)
        # the belt and the lower shirt below it
        f.shade(f.m_rect(0, 10.9, 8, 12) * (1 - f.m_rect(0, 0, 0, 0)), 0.9, 0.5)


def _arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in ("front", "back", "west", "east", "top", "bottom"):
            f = cv.face(part, nm, False)
            _skin(f)
            if nm == "bottom":
                H.cloth(f, f.m_all(), GLOVE, weave_k=0.0)
                continue
            H.cloth(f, f.m_all(), SHIRT, top=1.12, bottom=0.8, fold_xs=[1.3, 2.7] if nm in ("front", "back") else [], weave_k=0.025)
            if nm == "top":
                continue
            g = f.m_rect(-0.2, 9.4, f.w + 0.2, 12.2, soft=1.0)                          # the fingerless glove
            H.cloth(f, g, GLOVE, top=1.25, bottom=0.8, weave_k=0.0)
            f.fill(f.m_rect(-0.2, 11.4, f.w + 0.2, 12.2, soft=1.0), SKIN[1], 0.9)       # the bare fingertips
            H.trim_band(f, 9.1, 9.7, H.tone(GLOVE, 1.6), spec=0.2)
            if nm in ("back", "front"):
                H.trim_band(f, 9.9, 11.1, STEEL, x0=0.6, x1=f.w - 0.6, spec=0.6)        # the steel plate on the back of the hand
            f.shade(f.m_rect(-0.2, 11.4, f.w + 0.2, 12.2), 0.86, 0.5)


def _legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in ("front", "back", "west", "east", "top", "bottom"):
            f = cv.face(part, nm, False)
            _skin(f)
            if nm == "top":
                H.cloth(f, f.m_all(), SHIRT, weave_k=0.0)
                continue
            if nm == "bottom":
                H.cloth(f, f.m_all(), SAND, weave_k=0.0)
                continue
            H.cloth(f, f.m_all(), SHIRT, top=1.12, bottom=0.82, fold_xs=[1.4, 2.6] if nm in ("front", "back") else [], weave_k=0.025)
            b = f.m_rect(-0.2, 8.6, f.w + 0.2, 12.2, soft=1.0)                           # the sandals: dark blue to the ankle, the toe cap lighter
            H.cloth(f, b, SAND, top=1.2, bottom=0.78, weave_k=0.0)
            H.trim_band(f, 8.5, 9.1, H.tone(SAND, 1.9), spec=0.15)
            if nm == "front":
                f.fill(f.m_rect(0.2, 10.9, f.w - 0.2, 12.2, r=0.4), H.tone(SAND, 1.35), 0.8)
            f.shade(f.m_rect(0, 11.2, f.w, 12.2), 0.72, 0.7)


def build_skin(eye, out_png, out_glow):
    """The skin of one Kakashi look. eye: 'sharingan' or 'mangekyo'. -> (image, glow image or None)."""
    cv = C.Canvas(256, False)
    _head(cv, eye)
    _torso(cv)
    _arms(cv)
    _legs(cv)
    img, glow = cv.finish()
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    img.save(out_png, optimize=True)
    if glow is not None and out_glow:
        glow.save(out_glow, optimize=True)
    return img, glow

"""Shared drawing helpers of the hand-painted Black Clover characters of group bc3 (Vanessa Enoteca, Klaus Lunettes, Secre Swallowtail, Zora Ideale, Gauche Adlai, Marx Francois, Solid Silva), docs/arc/hd_arc_bc3.md.

Not a character module (the leading underscore keeps it out of the generator and of the module test). It starts from the bc1 kit (`_bc1_kit.py`, copied, never imported, so that a change there can
never change these skins) and adds what the seven Black Bulls, Golden Dawn, Silver Eagle and court characters needed: the face (eyes with lids and shadow, glasses, a monocle, brow studs), button
columns and trims, plates and cuffs, gloves, wraps, boots with cuffs, ruffles. It is built on arc_hdlib (the bridge and the HD library of the Tier List bosses). Everything works in MODEL PIXELS of a face
(4 texels at 256), deterministic, numpy and Pillow only.
"""
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import arc_hdlib as A  # noqa: E402

H = A.H
SIDES = ("front", "back", "west", "east")


# ================================================================================================ the contract helpers (second layer stays inside the 64x64 blueprint)
def cover_of(cv, part, name):
    """The second-layer coverage of the blueprint (what the 64x64 sheet draws on the second layer)."""
    return (cv.face(part, name, True).m_alpha() > 0.5).astype(np.float32)


def all_covers(cv, parts=("head", "body", "arm_r", "arm_l", "leg_r", "leg_l")):
    return {(p, n): cover_of(cv, p, n) for p in parts for n in SIDES}


def hold_cover(cv, covers):
    """Clips every second-layer island to its blueprint coverage after the drawing (the overlay may only be where the sheet puts cloth)."""
    for (part, name), m in covers.items():
        f = cv.face(part, name, True)
        f.fill(1.0 - m, "#000000", mode="erase")
        f.glow[..., 3] *= m


def clear_glow(f):
    f.glow[...] = 0.0


# ================================================================================================ colour
def lit(c, k):
    """The colour c lit by k > 1 without washing out toward white (H.tone mixes 45 % toward a warm white at k = 1.3, which turns dark cloth grey): a plain scale with a touch of warmth."""
    a = H.col(c)
    return np.clip(a * k + (k - 1.0) * np.array([0.03, 0.02, 0.0], np.float32), 0, 1).astype(np.float32)


def deep(c, k=0.6):
    """The colour c in shadow, cooled and kept saturated (H.tone with k < 1)."""
    return H.tone(c, k)


def hair_palette(base):
    """dict(d, m, l, h) for a hair colour: shadow, mid, light, highlight (the light and the highlight keep the hue: no washing out towards white)."""
    b = H.col(base)
    light = np.clip(b * 1.16 + 0.035, 0, 1).astype(np.float32)
    return {"d": H.tone(base, 0.6), "m": b, "l": light, "h": np.clip(light + (1.0 - light) * 0.32, 0, 1).astype(np.float32)}


def dark_hair_palette(base, lift=0.07):
    """The same for hair that is nearly black: the shadow cannot go much darker, so the light and the highlight carry the shape (a blue-grey sheen like the anime's)."""
    b = H.col(base)
    sheen = np.array([0.30, 0.34, 0.50], np.float32)
    light = np.clip(b + (sheen - b) * 0.32 + lift * 0.4, 0, 1).astype(np.float32)
    hi = np.clip(b + (sheen - b) * 0.62 + lift, 0, 1).astype(np.float32)
    return {"d": np.clip(b * 0.55, 0, 1).astype(np.float32), "m": b, "l": light, "h": hi}


# ================================================================================================ cloth and lines
def cloth(face, mask, color, top=1.1, bottom=0.84, round_=0.10, fold_xs=None, fold_depth=0.07, fold_y0=0.0, fold_y1=None, weave_k=0.0, a=1.0):
    """H.cloth with a lit top that keeps the hue (H.tone whitens: a dark jacket turned grey at top = 1.25)."""
    mask = np.asarray(mask, np.float32)
    ys = np.where(mask > 0.5)[0]
    y0 = face.Y[ys.min(), 0] - 0.5 / face.f if len(ys) else 0.0
    y1 = face.Y[ys.max(), 0] + 0.5 / face.f if len(ys) else face.h
    t = lit(color, top) if top > 1.0 else H.tone(color, top)
    face.fill(mask, H.lin((0, y0), (0, y1), [(0, t), (0.55, color), (1, H.tone(color, bottom))]), a)
    away = face.X if face.light[0] > 0 else face.w - face.X
    face.shade(mask, 1.0 - round_ * H.smoothstep(0.45 * face.w, face.w, away), a)
    if fold_xs:
        H.folds(face, mask, fold_xs, depth=fold_depth, width=0.25, y0=fold_y0 if fold_y0 else y0, y1=fold_y1 if fold_y1 else y1)
    if weave_k:
        H.weave(face, mask, strength=weave_k)
    return face


def seam(f, pts, color="#c9a867", width=0.07, a=0.8, dash=(0.34, 0.2), shadow=0.28, mask=None):
    """A stitched line; at 256 texels a line thinner than one texel (0.25 px) breaks into dots, so width and dashes are kept at texel scale."""
    d = None if dash is None else (max(dash[0], 0.5), max(dash[1], 0.36))
    H.stitch(f, pts, color, width=max(width, 0.2), dash=d, shadow=shadow, a=a, mask=mask)


def edge_light(f, mask, x0, x1, color, k=1.6, a=0.3):
    """A thin lit edge on the light side of a panel (dark smooth cloth reads as flat black without it)."""
    f.fill(mask * f.m_rect(x0, f.Y.min(), x1, f.Y.max() + 1.0), lit(color, k), a)


def crease(f, pts, color, width=0.1, a=0.55, light=1.7, mask=None):
    """A cloth crease: a dark line with a thin light line just above it."""
    m = 1.0 if mask is None else mask
    f.fill(f.m_polyline(pts, width) * m, H.tone(color, 0.5), a)
    f.fill(f.m_polyline([(x, y - width * 1.3) for x, y in pts], width * 0.6) * m, lit(color, light), a * 0.7)


def hem_shadow(f, y, width=0.5, strength=0.3, x0=None, x1=None, mask=None):
    H.shadow_under(f, y, width, strength, mask=mask, x0=x0, x1=x1)


# ================================================================================================ hair on the face (the fringe) and locks
def lock(f, pal, cx, w, top, length, lean, shade=1.0, edge=0.07):
    """One tapered lock of hair hanging from y=top: a teardrop that leans, a dark edge on its shadow side, a light streak down its middle. pal = dict(d, m, l, h) dark / mid / light / highlight."""
    n = 9
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.6)
        x = cx + lean * t * t
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["m"], pal["d"], 0.25 * (1 - shade))), (0.55, H.mix(pal["l"], pal["m"], 0.2 * (1 - shade))), (1, H.mix(pal["m"], pal["d"], 0.3 + 0.3 * (1 - shade)))]))
    f.fill(f.m_polyline(left[2:], edge) * m, pal["d"], 0.8)
    f.fill(f.m_polyline(right[2:], edge) * m, H.mix(pal["d"], "#000000", 0.25), 0.85)
    mid = [(cx + lean * (i / n) ** 2 * 0.9, top + length * 0.12 + length * 0.7 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.12) * m, pal["h"], 0.55 * shade)
    return m


def spike(f, pal, cx, w, top, length, lean, shade=1.0, edge=0.07):
    """A thick pointed clump of hair (a spike): wide at the root, a pointed tip that leans, a dark shadow edge, a light streak."""
    n = 8
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.25)
        x = cx + lean * t ** 1.5
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["l"], pal["m"], 0.3 + 0.4 * (1 - shade))), (0.5, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.55))]))
    f.fill(f.m_polyline(left[1:], edge) * m, pal["d"], 0.75)
    f.fill(f.m_polyline(right[1:], edge) * m, H.mix(pal["d"], "#000000", 0.2), 0.9)
    mid = [(cx - w * 0.12 + lean * (i / n) ** 1.5 * 0.9, top + length * 0.1 + length * 0.62 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.11) * m, pal["h"], 0.6 * shade)
    return m


def hair_mass(f, pal, poly, y0=0.0, y1=2.2, shadow_k=0.86, shadow_depth=2.2, streaks=(), streak_len=1.8):
    """The hair mass of the forehead (a polygon of the face, model pixels), the soft shadow it throws on the skin under it, and light streaks. Returns its mask."""
    mass = f.m_poly(poly)
    ymax = max(p[1] for p in poly)
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - (ymax - 0.25)) / shadow_depth, 0, 1) * (f.Y > ymax - 0.7), shadow_k)
    f.fill(mass, H.lin((0, y0), (0, y1), [(0, H.mix(pal["d"], pal["m"], 0.45)), (0.5, pal["m"]), (1, H.tone(pal["m"], 0.9))]))
    for x in streaks:
        f.fill(f.m_line((x, y0), (x + 0.12, y0 + streak_len), 0.10, 0.05) * mass, pal["h"], 0.35)
    return mass


# ================================================================================================ the face
def face_base(f, skin, side=0.09, chin=0.07, jaw=None):
    """The skin of the front of the head: a soft gradient, darker at both sides and under the chin; `jaw` (0..1) shades the cheeks in a lean face."""
    H.skin_face(f, skin, hair=None, side=side, chin=chin, shadow=0.0)
    if jaw:
        f.shade(f.m_poly([(0.0, 5.2), (0.9, 5.6), (1.6, 7.6), (2.6, 8.0), (0.0, 8.0)]), 1.0 - jaw, 0.7)
        f.shade(f.m_poly([(8.0, 5.2), (7.1, 5.6), (6.4, 7.6), (5.4, 8.0), (8.0, 8.0)]), 1.0 - jaw, 0.7)


def ear_face(f, skin, name, y=4.6, h=1.7):
    """The ear on a side of the head (west = the entity's right): a rounded shape with an inner shadow. Drawn on the part of the side that the hair leaves free."""
    cx = 2.0 if name == "west" else f.w - 2.0
    f.ellipse(cx, y + h / 2.0, 0.75, h / 2.0, H.tone(skin[1], 1.02))
    f.ellipse(cx + (0.1 if name == "west" else -0.1), y + h / 2.0, 0.4, h / 2.0 - 0.35, H.tone(skin[0], 0.78), a=0.55)


# ================================================================================================ skin
def limb_skin(f, skin, y0, y1, tendon=True, vein=False, muscle=0.0):
    """Bare skin of an arm or a leg from y0 to y1: a soft gradient, darker at both sides (the roundness), a sheen, the shadow of whatever hangs above, a tendon line; `muscle` (0..1) adds the
    shaded swell of the upper arm (deltoid, biceps) and the groove of the forearm."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(skin[1], 0.86)), (0.2, skin[1]), (0.7, H.tone(skin[1], 0.98)), (1, H.tone(skin[0], 0.92))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    H.sheen(f, m, [f.w * 0.42], width=0.4, strength=0.12, tilt=0.0, y0=y0 + 0.6, y1=y1 - 0.4, color="#fff0e0")
    H.shadow_under(f, y0 + 0.05, 0.9, 0.25, mask=m)
    if muscle > 0 and y1 - y0 > 6:
        cx = f.w * 0.46
        bi = f.m_ellipse(cx, y0 + 2.0, f.w * 0.36, 1.7) * m                                  # the biceps swell
        f.fill(bi, H.rad((cx - 0.3, y0 + 1.5), 2.4, [(0, H.tone(skin[2], 1.0)), (0.7, skin[1]), (1, H.tone(skin[0], 0.95))]), 0.55 * muscle)
        f.fill(f.m_polyline([(f.w * 0.2, y0 + 3.7), (f.w * 0.5, y0 + 3.95), (f.w * 0.85, y0 + 3.7)], 0.14) * m, H.tone(skin[0], 0.7), 0.45 * muscle)   # the lower edge of the biceps
        f.fill(f.m_line((f.w * 0.55, y0 + 4.4), (f.w * 0.5, y0 + 7.2), 0.2, 0.12) * m, H.tone(skin[0], 0.78), 0.4 * muscle)                          # the forearm groove
    if tendon:
        f.fill(f.m_line((f.w * 0.64, y0 + 1.6), (f.w * 0.58, y0 + 3.4), 0.16, 0.08) * m, H.tone(skin[0], 0.8), 0.4)
    if vein:
        f.fill(f.m_polyline([(f.w * 0.3, y1 - 4.2), (f.w * 0.42, y1 - 3.3), (f.w * 0.36, y1 - 2.5), (f.w * 0.5, y1 - 1.6)], 0.09) * m, H.tone(skin[0], 0.72), 0.35)
    f.fill(f.m_band(y1 - 0.7, y1), H.tone(skin[0], 0.9), 0.25)
    return m


def fingers(f, skin, y0=10.9, y1=11.9, n=3):
    """The bare hand: finger gaps on the front and the back."""
    if f.name in ("front", "back"):
        for i in range(1, n + 1):
            x = f.w * i / (n + 1)
            f.fill(f.m_line((x, y0), (x, y1), 0.17, 0.12), H.tone(skin[0], 0.7), 0.6)
        f.fill(f.m_band(y0 - 0.1, y0 + 0.05), H.tone(skin[0], 0.72), 0.5)


# ================================================================================================ boots and trousers
def trousers(f, color, y0, y1, name, knee=True, fold_xs=(1.2, 2.8), seam_color="#8a7e9a", top=1.3, bottom=0.8, knee_y=(2.6, 3.7)):
    cloth(f, f.m_band(y0, y1), color, top=top, bottom=bottom, round_=0.12, fold_xs=list(fold_xs), fold_depth=0.10, fold_y0=y0 + 0.4, fold_y1=y1 - 0.4)
    if knee:
        for y, k in ((knee_y[0], 0.6), (knee_y[1], 0.45)):
            f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.1) * f.m_band(y0, y1), H.tone(color, 0.5), k)
            f.fill(f.m_polyline([(0.2, y - 0.15), (f.w * 0.5, y + 0.13), (f.w - 0.2, y - 0.10)], 0.06) * f.m_band(y0, y1), lit(color, 1.8), k * 0.5)
    if name in ("west", "east"):
        seam(f, [(f.w * 0.5, y0 + 0.2), (f.w * 0.5, y1 - 0.4)], seam_color, 0.06, 0.55)


def boot(f, color, y0, y1, name, strap=None, gold="#e0b43e", toe=True, sole="#0d0b0a", sole_top="#1c1512", welt_k=1.7):
    """A boot from y0 to the foot line at 11.25 with welt and sole to 12: a gradient, a sheen, ankle wrinkles, an optional strap with a buckle across the shin and a polished toe cap."""
    boot_m = f.m_band(y0, 11.25)
    f.fill(boot_m, H.lin((0, y0), (0, 11.25), [(0, lit(color, 1.35)), (0.5, color), (1, H.tone(color, 0.72))]))
    H.shadow_under(f, y0 + 0.05, 0.7, 0.34)
    H.sheen(f, boot_m, [f.w * 0.3], width=0.22, strength=0.42, tilt=0.04, y0=y0 + 0.35, y1=10.6, color="#c8b0b0")
    H.sheen(f, boot_m, [f.w * 0.72], width=0.14, strength=0.2, tilt=0.04, y0=y0 + 0.6, y1=10.4, color="#a8a8c0")
    if name in ("west", "east"):                                                                 # the stitched side seam of the boot, a texel wide
        seam(f, [(f.w * 0.5, y0 + 0.5), (f.w * 0.5, 10.8)], lit(color, 2.2), 0.05, 0.5, (0.3, 0.2), 0.2)
    f.fill(f.m_polyline([(0.0, 9.45), (f.w * 0.5, 9.7), (f.w, 9.5)], 0.11), H.tone(color, 0.45), 0.7)
    f.fill(f.m_polyline([(0.0, 9.78), (f.w * 0.5, 10.0), (f.w, 9.8)], 0.07), lit(color, 1.8), 0.32)
    if strap and name == "front":
        f.rect(0.0, 8.1, f.w, 8.9, H.tone(strap, 1.05), r=0.0)
        f.fill(f.m_rect(0.0, 8.1, f.w, 8.28), lit(strap, 1.5), 0.8)
        f.fill(f.m_rect(0.0, 8.82, f.w, 8.92), "#0e0a08", 0.7)
        H.stud(f, f.w * 0.5, 8.5, 0.34, gold)
    if toe and name == "front":
        f.ellipse(f.w * 0.5, 10.25, 1.2, 0.65, lit(color, 1.9), a=0.38, soft=2.0)
    f.fill(f.m_band(11.0, 11.35), lit(color, welt_k), 0.9)
    f.fill(f.m_band(11.3, 12.0), H.lin((0, 11.3), (0, 12.0), [(0, sole_top), (1, sole)]))
    f.fill(f.m_band(11.0, 11.12), lit(color, 2.2), 0.6)


def belt_band(f, y0, y1, color, stitch="#d4ac68", stitch_a=0.7):
    """A leather belt band: a vertical gradient, a lit top edge, a dark lower edge and a stitch line."""
    f.fill(f.m_rect(0.0, y0, f.w, y1), H.lin((0, y0), (0, y1), [(0, lit(color, 1.45)), (0.4, color), (1, H.tone(color, 0.7))]))
    f.fill(f.m_rect(0.0, y0 + 0.02, f.w, y0 + 0.18), lit(color, 2.0), 0.45)
    f.fill(f.m_rect(0.0, y1 - 0.12, f.w, y1), "#0e0a08", 0.6)
    seam(f, [(0.0, (y0 + y1) / 2.0), (f.w, (y0 + y1) / 2.0)], stitch, 0.055, stitch_a, (0.3, 0.18), 0.2)


def buckle(f, cx, cy, w, h, metal="#e0b43e", opening="#1d130c", r=0.22):
    """A rectangular belt buckle: a dark rim, a metal frame with a bevel, a dark opening, a prong and a glint."""
    x0, y0, x1, y1 = cx - w / 2.0, cy - h / 2.0, cx + w / 2.0, cy + h / 2.0
    f.rect(x0 - 0.07, y0 - 0.07, x1 + 0.07, y1 + 0.07, H.tone(metal, 0.3), r=r + 0.04)
    f.rect(x0, y0, x1, y1, H.metal_paint(y0, y1, metal, 1.15), r=r)
    H.bevel(f, f.m_rect(x0, y0, x1, y1, r), depth=0.24, gain=1.3, spec=0.7)
    f.rect(x0 + w * 0.2, y0 + h * 0.22, x1 - w * 0.2, y1 - h * 0.22, opening, r=0.1)
    f.line((x0 + w * 0.28, cy), (cx + w * 0.1, cy), H.metal_paint(cy - 0.2, cy + 0.2, metal, 1.1), 0.2, 0.14)
    f.ellipse(x0 + 0.25, y0 + 0.2, 0.24, 0.12, "#fff6c8", a=0.8)


# ================================================================================================ fur and feathers
def fur_collar(f, y0, y1, base, tip, dark=None, n=16, seed=3, scallop=True, mask=None, a=1.0):
    """A fur or feather collar band from y0 to y1: a gradient, rows of tufts (scallops) and light tips."""
    dark = dark if dark is not None else H.tone(base, 0.45)
    m = f.m_rect(-0.1, y0, f.w + 0.1, y1)
    if mask is not None:
        m = m * mask
    f.fill(m, H.lin((0, y0), (0, y1), [(0, lit(base, 1.35)), (0.5, base), (1, H.tone(base, 0.65))]), a)
    if scallop:
        H.scallops(f, m, y0 + 0.05, rows=2, cell=0.9, depth=max(0.8, (y1 - y0) * 0.75), base=None, tip=None, edge=dark, edge_a=0.5, shadow=0.24)
    for i in range(n):
        x = f.w * H.hsh(i, seed, 4)
        y = y0 + 0.15 + (y1 - y0 - 0.3) * H.hsh(i, seed + 1, 4)
        f.fill(f.m_line((x, y), (x + 0.1, y + 0.4), 0.09, 0.03) * m, tip, 0.7 * a)
    H.shadow_under(f, y1, 0.5, 0.3)
    return m


# ================================================================================================ marks and light
def glow_line(f, pts, color, width=0.22, a=0.95, glow=True, core="#ffffff", core_a=0.5):
    """A line of light on the skin: a coloured stroke with a bright core and a glow-map twin."""
    m = f.m_polyline(pts, width)
    f.fill(m, color, a, glow=glow)
    f.fill(f.m_polyline(pts, width * 0.38) * m, core, core_a)
    return m


def blob(f, cx, cy, rx, ry, power=1.6, rot=0.0):
    """A soft elliptical weight map (1 in the centre, 0 at the edge) for light and shade: shade(mask, 1 + k * blob)."""
    import math
    x, y = f.X - cx, f.Y - cy
    c, s = math.cos(math.radians(rot)), math.sin(math.radians(rot))
    u, v = (x * c + y * s) / rx, (-x * s + y * c) / ry
    return np.clip(1.0 - np.sqrt(u * u + v * v), 0.0, 1.0) ** power


def bare_arm(f, skin, name, y0=0.0, y1=12.0, power=1.0, fist=True):
    """A bare muscled arm on one face of an arm part (front, back, west = the outer side of the right arm, east): a skin gradient, a lit deltoid cap and its lower edge, the biceps (front) or the triceps
    (back), the elbow, the forearm with a tendon, veins and the knuckles. `power` 0..1 scales the muscle shading. Everything is soft (blobs), the few lines are a texel wide."""
    s0, s1, s2 = skin
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(s1, 0.9)), (0.15, s1), (0.6, H.tone(s1, 0.98)), (1, H.tone(s0, 0.92))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)                       # the roundness: both edges fall into shadow
    w = f.w
    k = power
    f.shade(m, 1.0 + 0.20 * k * blob(f, w * 0.42, 1.2, w * 0.62, 1.7), 0.9)                  # the deltoid cap catches the light
    f.shade(m, 1.0 - 0.26 * k * blob(f, w * 0.5, 2.7, w * 0.7, 0.55, 1.0), 0.9)              # its lower edge falls into shadow
    if name == "back":
        f.shade(m, 1.0 + 0.12 * k * blob(f, w * 0.5, 4.0, w * 0.3, 1.5), 0.9)                 # the triceps
        f.fill(f.m_polyline([(w * 0.5, 2.9), (w * 0.5, 5.6)], 0.2, 2.0), H.tone(s0, 0.72), 0.3 * k)
    else:
        f.shade(m, 1.0 + 0.18 * k * blob(f, w * 0.5, 3.7, w * 0.36, 1.3), 0.9)                # the biceps peak
        f.shade(m, 1.0 - 0.14 * k * blob(f, w * 0.5, 5.35, w * 0.6, 0.45, 1.0), 0.9)          # the crease inside the elbow
    f.shade(m, 1.0 + 0.10 * k * blob(f, w * 0.44, 7.6, w * 0.30, 2.0), 0.9)                  # the forearm
    f.shade(m, 1.0 - 0.12 * k * blob(f, w * 0.9, 7.6, w * 0.3, 2.4), 0.9)
    H.shadow_under(f, y0 + 0.05, 0.9, 0.2, mask=m)
    f.fill(f.m_line((w * 0.62, 6.5), (w * 0.58, 8.6), 0.17, 0.1) * m, H.tone(s0, 0.72), 0.32 * k)   # the tendon
    f.fill(f.m_band(y1 - 0.7, y1), H.tone(s0, 0.9), 0.25)
    if fist and name in ("front", "back"):
        for i in (1, 2, 3):
            x = w * i / 4.0
            f.fill(f.m_line((x, 10.95), (x, 11.9), 0.17, 0.12), H.tone(s0, 0.7), 0.6)
        f.fill(f.m_band(10.85, 11.0), H.tone(s0, 0.72), 0.5)
        if name == "back":
            for i in (0.5, 1.5, 2.5, 3.5):
                f.ellipse(w * i / 4.0, 10.75, 0.36, 0.3, H.tone(s1, 1.06), a=0.5)


def feather_pad(f, x0, x1, y0, y1, base, light, dark=None, seed=1, cell=0.85, mask=None, a=1.0):
    """A pad of soft feathers or fur (a mantle's shoulder collar): a rounded, scalloped lower edge, a gradient from light to dark, short curved feather strokes and a shadow under it. Returns the mask."""
    dark = dark if dark is not None else H.tone(base, 0.55)
    pts = [(x0 - 0.2, y0 - 0.3), (x1 + 0.2, y0 - 0.3)]
    n = max(2, int(round((x1 - x0) / cell)))
    for i in range(n * 4 + 1):
        t = i / (n * 4)
        x = x1 - (x1 - x0) * t
        k = (t * n) % 1.0
        dip = 0.34 * (1.0 - (2.0 * k - 1.0) ** 2) ** 0.7                   # each tab is a rounded bump below the line
        pts.append((x, y1 - 0.34 + dip + 0.12 * (H.hsh(int(t * n), seed, 2) - 0.5)))
    m = f.m_poly(pts)
    if mask is not None:
        m = m * mask
    f.fill(m, H.lin((0, y0), (0, y1), [(0, light), (0.45, base), (1, dark)]), a)
    for i in range(int((x1 - x0) * 3.2)):                                    # feather strokes: short arcs that lean outward
        x = x0 + (x1 - x0) * H.hsh(i, seed, 5)
        y = y0 + (y1 - y0 - 0.3) * H.hsh(i, seed, 6)
        lean = (H.hsh(i, seed, 7) - 0.5) * 0.5
        f.fill(f.m_line((x, y), (x + lean, y + 0.55), 0.1, 0.04) * m, light, 0.55 * a)
        f.fill(f.m_line((x + 0.12, y + 0.05), (x + 0.12 + lean, y + 0.6), 0.08, 0.03) * m, dark, 0.35 * a)
    f.fill(f.m_polyline(pts[2:], 0.09) * m, dark, 0.55 * a)
    H.shadow_under(f, y1 - 0.1, 0.55, 0.3 * a, mask=None if mask is None else mask * 0 + 1.0)
    return m


# ================================================================================================ the rear of the head
def head_rear(cv, pal, back_y=6.0, nape_y=7.2, side_top=3.0, ear_x=4.1, side_low=6.3, parting=False, streaks=(0.8, 1.9, 3.0, 4.2, 5.3, 6.4, 7.3)):
    """Hair on the base layer of the back and the sides of the head (the 3D cap sits over it; this is what shows between its locks and under it). The back is a mass down to a pointed hairline at the
    nape (`back_y` at the sides, `nape_y` in the middle); each side face is hair above the temple (`side_top`) and behind the ear (`ear_x` from the back edge, down to `side_low`); ear and cheek stay skin."""
    f = cv.face("head", "back")
    poly = [(-0.3, -0.3), (8.3, -0.3), (8.3, back_y - 0.8), (7.3, back_y + 0.1), (6.2, back_y), (5.2, nape_y - 0.5), (4.0, nape_y), (2.8, nape_y - 0.5), (1.8, back_y), (0.7, back_y + 0.1), (-0.3, back_y - 0.8)]
    mass = hair_mass(f, pal, poly, 0.0, back_y + 0.5, shadow_k=0.9, shadow_depth=1.2, streaks=streaks, streak_len=3.2)
    if parting:
        f.fill(f.m_line((4.0, 0.2), (4.0, nape_y - 0.2), 0.1, 0.08) * mass, pal["d"], 0.5)
    for nm in ("west", "east"):
        f = cv.face("head", nm)
        flip = (nm == "east")
        px = (lambda x: f.w - x) if flip else (lambda x: x)                                    # model x counted from the BACK edge of the side face
        outline = [(px(-0.3), -0.3), (px(8.3), -0.3), (px(8.3), side_top - 0.1), (px(ear_x + 1.5), side_top + 0.1), (px(ear_x + 0.3), side_top + 0.5), (px(ear_x), side_top + 2.0),
                   (px(ear_x + 0.3), side_low - 0.4), (px(ear_x - 0.9), side_low), (px(ear_x - 2.5), side_low + 0.5), (px(-0.3), side_low + 0.2)]
        hair_mass(f, pal, outline, 0.0, side_low + 0.2, shadow_k=0.88, shadow_depth=1.0, streaks=tuple(px(x) for x in (0.7, 1.8, 2.9, 4.1, 5.2, 6.4, 7.5)), streak_len=2.6)


# ================================================================================================ bc3 additions: the face, glasses, trims, plates, gloves, ruffles
def grow_cover(cv, covers, part, y0, y1, names=SIDES):
    """Lets the second layer of `part` cover the rows y0..y1 too, where the 64x64 sheet has none (a deliberate exception for a bulky piece such as a boot cuff; documented per character)."""
    for n in names:
        covers[(part, n)] = np.maximum(covers[(part, n)], cv.face(part, n, True).m_band(y0, y1))


def clear_head_overlay(cv):
    """Empties every second-layer island of the head (the accessories of the sheet: glasses, monocle, ear studs are redrawn by hand, both layers)."""
    for n in ("front", "back", "west", "east", "top", "bottom"):
        cv.face("head", n, True).clear()


def eyeshadow(f, cx, cy, w, color, a=0.5, h=0.62, lift=0.55):
    """A soft band of eyeshadow over the upper lid of an eye at (cx, cy) of width w (draw it before the eye)."""
    m = f.m_ellipse(cx, cy - lift, w * 0.62, h, soft=3.0)
    f.fill(m, color, a)


def eye_pair(f, iris, cx=(2.4, 5.6), cy=5.0, w=2.5, h=1.8, **kw):
    """Both anime eyes of the front of the head with the shared keywords of H.eye (tilt, lash, lash_w, flick, lid_shadow, iris_r, hi, pupil, glow)."""
    out = []
    for x in cx:
        out.append(H.eye(f, x, cy, w, h, iris, **kw))
    return out


def eye_bags(f, cx, cy, w, color="#9a7a90", a=0.5, depth=0.5):
    """The dark crescent under an eye (tired characters)."""
    m = f.m_polyline([(cx - w * 0.5, cy + 0.15), (cx, cy + depth), (cx + w * 0.5, cy + 0.15)], 0.22, soft=2.0)
    f.fill(m, color, a)


def ring_mask(f, x0, y0, x1, y1, th, r=0.3):
    """(ring, inside) masks of a rounded rectangle frame of thickness th (a lens frame, a plate rim)."""
    outer = f.m_rect(x0, y0, x1, y1, r)
    inner = f.m_rect(x0 + th, y0 + th, x1 - th, y1 - th, max(0.05, r - th * 0.6))
    return H.minus(outer, inner), inner


def vband(f, x0, x1, y0, y1, color, bevel=0.2, spec=0.4, outline=0.55):
    """A vertical bevelled metal or piping band (a placket, a lapel trim): dark outline, a gradient across (light on the left), a bevel, a specular streak."""
    m = f.m_rect(x0, y0, x1, y1)
    if outline > 0:
        f.fill(f.m_rect(x0 - 0.1, y0 - 0.02, x1 + 0.1, y1 + 0.02), H.tone(color, 0.38), outline)
    f.fill(m, H.lin((x0, 0), (x1, 0), [(0, H.tone(color, 1.35)), (0.45, color), (1, H.tone(color, 0.66))]))
    H.bevel(f, m, depth=bevel, gain=1.1, spec=spec, spec_pow=12)
    return m


def button_column(f, x, ys, color, r=0.26, thread=None):
    for y in ys:
        H.stud(f, x, y, r, color, spec=0.85)
        if thread:
            f.fill(f.m_ring(x, y, r * 1.55, 0.1), thread, 0.55)


def plate(f, mask, color, light=1.1, spec=0.55, bevel=0.3, y0=None, y1=None):
    """A steel or gold plate over a mask: the vertical metal gradient, a bevel with a specular streak. Returns the mask."""
    ys = np.where(np.asarray(mask) > 0.5)[0]
    if len(ys) and y0 is None:
        y0 = float(f.Y[ys.min(), 0])
        y1 = float(f.Y[ys.max(), 0]) + 0.25
    f.fill(mask, H.metal_paint(y0, y1, color, light))
    H.bevel(f, mask, depth=bevel, gain=1.15, spec=spec, spec_pow=14)
    return mask


def edge_line(f, mask, color, a=0.8, grow=1.0):
    """A dark line round a mask (its border, `grow` texels wide): the dilated mask minus the mask."""
    m = np.asarray(mask, np.float32)
    k = max(1, int(round(grow)))
    d = m.copy()
    for _ in range(k):
        e = d.copy()
        e[1:] = np.maximum(e[1:], d[:-1])
        e[:-1] = np.maximum(e[:-1], d[1:])
        e[:, 1:] = np.maximum(e[:, 1:], d[:, :-1])
        e[:, :-1] = np.maximum(e[:, :-1], d[:, 1:])
        d = e
    f.fill(np.clip(d - m, 0, 1), color, a)


def glove(f, color, y0=10.4, y1=12.0, name="front", shade=0.7, cuff=None):
    """A glove from y0 to the end of the hand: a gradient, a sheen, finger gaps on the front and the back and a knuckle line."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, lit(color, 1.12)), (0.6, color), (1, H.tone(color, 0.82))]))
    H.sheen(f, m, [f.w * 0.35], width=0.35, strength=0.18, tilt=0.0, y0=y0 + 0.2, y1=y1 - 0.2, color="#ffffff")
    if name in ("front", "back"):
        for i in range(1, 4 if f.w > 3.5 else 3):
            x = f.w * i / (4.0 if f.w > 3.5 else 3.0)
            f.fill(f.m_line((x, y0 + 0.9), (x, y1 - 0.05), 0.17, 0.12) * m, H.tone(color, shade), 0.7)
        f.fill(f.m_line((0.15, y0 + 0.85), (f.w - 0.15, y0 + 0.85), 0.14, 0.14) * m, H.tone(color, shade), 0.55)
    return m


def ruffle(f, y0, y1, base, light, dark, cell=1.3, rows=2, mask=None, a=1.0, seed=2):
    """A frilled hem or collar from y0 to y1: scallops (1.3 px cells, never a 1 px lattice), a light lip, a dark gap under each tab and a soft shadow."""
    m = f.m_rect(-0.1, y0, f.w + 0.1, y1)
    if mask is not None:
        m = m * mask
    f.fill(m, H.lin((0, y0), (0, y1), [(0, lit(base, 1.25)), (0.5, base), (1, H.tone(base, 0.62))]), a)
    H.scallops(f, m, y0 + 0.1, rows=rows, cell=cell, depth=max(0.7, (y1 - y0) * 0.8), base=None, tip=None, edge=dark, edge_a=0.45, shadow=0.22)
    n = max(2, int(f.w / cell))
    for i in range(n * 2):
        x = f.w * H.hsh(i, seed, 8)
        y = y0 + (y1 - y0) * (0.2 + 0.6 * H.hsh(i, seed, 9))
        f.fill(f.m_line((x, y), (x + 0.1, y + 0.45), 0.12, 0.05) * m, light, 0.6 * a)
    return m


def sleeve(f, color, y0=0.0, y1=10.4, sheen="#9aa6d0", elbow=4.2):
    """A long cloth sleeve from y0 to y1: gradient, the lit shoulder, a sheen, creases at the elbow. Returns its mask."""
    sl = f.m_band(y0, y1)
    cloth(f, sl, color, top=1.4, bottom=0.8, round_=0.14, fold_xs=[f.w * 0.5], fold_depth=0.08, fold_y0=y0 + 1.5, fold_y1=y1 - 0.4)
    f.shade(sl, 1.0 + 0.12 * blob(f, f.w * 0.4, y0 + 1.2, f.w * 0.7, 1.4), 0.9)
    H.sheen(f, sl, [f.w * 0.3], width=0.26, strength=0.2, tilt=0.0, y0=y0 + 1.5, y1=y1 - 0.4, color=sheen)
    for dy, k in ((0.0, 0.5), (0.6, 0.35)):
        crease(f, [(0.0, elbow + dy), (f.w * 0.4, elbow + dy + 0.3), (f.w, elbow + dy + 0.05)], color, 0.1, k, mask=sl)
    return sl


def cuff(f, y0, y1, color, trim=None, stitch=None):
    """A cuff band (y0..y1): a lit gradient, a bevel, an optional bright trim line at the top and bottom and a stitch between."""
    band = f.m_rect(0.0, y0, f.w, y1)
    f.fill(band, H.lin((0, y0), (0, y1), [(0, lit(color, 1.35)), (0.5, color), (1, H.tone(color, 0.7))]))
    H.bevel(f, band, depth=0.18, gain=1.0, spec=0.3)
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.12), "#06060a", 0.55)
    if trim:
        f.fill(f.m_rect(0.0, y0 + 0.12, f.w, y0 + 0.3), trim, 0.95)
        f.fill(f.m_rect(0.0, y1 - 0.3, f.w, y1 - 0.12), trim, 0.95)
    if stitch:
        seam(f, [(0.12, (y0 + y1) / 2.0), (f.w - 0.12, (y0 + y1) / 2.0)], stitch, 0.055, 0.7, (0.3, 0.18), 0.2)
    return band


def leather_band(f, mask, color="#1e1b1f", light=1.9, a=1.0):
    """A strap of dark leather over a mask: a gradient, a lit upper edge, a darker lower edge."""
    ys = np.where(np.asarray(mask) > 0.5)[0]
    y0 = float(f.Y[ys.min(), 0]) if len(ys) else 0.0
    y1 = float(f.Y[ys.max(), 0]) + 0.25 if len(ys) else 1.0
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, lit(color, light)), (0.4, color), (1, H.tone(color, 0.55))]), a)
    return mask


def epaulette(f, x0, x1, y0, y1, base, light, trim):
    """A shoulder board: a rounded plate with a lit top, a trim line round it and two studs."""
    m = f.m_rect(x0, y0, x1, y1, 0.2)
    f.fill(f.m_rect(x0 - 0.08, y0 - 0.08, x1 + 0.08, y1 + 0.1, 0.24), H.tone(base, 0.3), 0.85)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, light), (0.5, base), (1, H.tone(base, 0.65))]))
    H.bevel(f, m, depth=0.22, gain=1.1, spec=0.5)
    f.fill(f.m_rect(x0 + 0.12, y0 + 0.12, x1 - 0.12, y0 + 0.28, 0.1), trim, 0.9)
    f.fill(f.m_rect(x0 + 0.12, y1 - 0.28, x1 - 0.12, y1 - 0.12, 0.1), trim, 0.9)
    return m


def bare_leg(f, skin, name, y0=0.0, y1=10.4, knee_y=6.4):
    """A bare leg from y0 to y1 (a slim thigh and calf): a skin gradient, both edges falling into shadow (the roundness), a soft kneecap on the front or the crease behind the knee on the back, the
    swell of the calf and a lit shin line. Everything is soft; the few lines are a texel wide."""
    s0, s1, s2 = skin
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(s1, 0.94)), (0.3, s1), (0.7, H.tone(s1, 0.97)), (1, H.tone(s0, 0.9))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 1.0, side)), 0.9)
    f.shade(m, 1.0 + 0.10 * blob(f, f.w * 0.45, (y0 + knee_y) / 2.0 - 0.5, f.w * 0.55, 2.2), 0.9)          # the thigh catches the light
    f.shade(m, 1.0 + 0.12 * blob(f, f.w * 0.42, knee_y + 2.0, f.w * 0.42, 1.6), 0.9)                     # the calf swells
    if name == "front":
        f.ellipse(f.w * 0.5, knee_y, f.w * 0.3, 0.55, H.tone(s2, 1.0), a=0.30, soft=2.0)                 # the kneecap
        f.fill(f.m_polyline([(f.w * 0.22, knee_y + 0.62), (f.w * 0.5, knee_y + 0.78), (f.w * 0.78, knee_y + 0.62)], 0.09) * m, H.tone(s0, 0.72), 0.4)
        f.fill(f.m_line((f.w * 0.46, knee_y + 1.2), (f.w * 0.46, y1 - 0.8), 0.1, 0.07) * m, H.tone(s2, 1.0), 0.28)   # the shin
    elif name == "back":
        f.fill(f.m_polyline([(f.w * 0.15, knee_y + 0.1), (f.w * 0.5, knee_y + 0.3), (f.w * 0.85, knee_y + 0.1)], 0.12) * m, H.tone(s0, 0.7), 0.45)
    H.shadow_under(f, y0 + 0.05, 0.7, 0.2, mask=m)
    return m

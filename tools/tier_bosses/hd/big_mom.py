"""\"Big Mom\" Charlotte Linlin (One Piece, Yonko of the Big Mom Pirates), HD 256x256 (3.38.0, group c of docs/bosses/hd_outfits.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/big_mom.py) through the HD bridge; then everything is drawn again at 4x detail: the made-up face
(violet eyeshadow, big lashed violet eyes, angry brows, blush, a huge red-lipped grin with teeth), the white scalloped lace collar with a gold heart brooch, the cream brocade stomacher with the
big red heart and gold lacing, the hot-pink gown with gathered puff sleeves, gold bands and scalloped lace cuffs, the plum damask sash with a gold buckle, the flared plum skirt with a gold scalloped
hem over a lace petticoat, the striped stockings and the wine boots with gold cuffs, and the gold tiara with its glowing jewel. Hair: `anime_curls` (big rolled curls hugging the head), magenta.
Original art from memory of the canon look (the wikis could not be fetched; see docs/bosses/hd_outfits_c.md). Glow: the tiara jewel, the heart brooch, the sash buckle.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

PINK, PINK_L, PINK_D = "#f4508e", "#ffa2c4", "#a8205a"
PLUM, PLUM_L, PLUM_D = "#b01a74", "#e255a8", "#580838"
GOLD, GOLD_D, GOLD_L = "#ffd24a", "#a8741a", "#fff0a0"
LACE, LACE_D = "#fff4f8", "#e6c0d0"
CREAM = "#fff0e0"
WINE, WINE_D = "#82204e", "#4a0e2c"
HEART = "#e0183c"
SKIN = ("#e8b6a8", "#f7d2c4", "#fde8de")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def damask(f, mask, color, cell=1.6, k=0.10, y0=0.0):
    """Tone-on-tone diamond brocade over a mask: a lattice of small lozenges slightly lighter and darker than the cloth."""
    u = (f.X + (f.Y - y0)) * (2 * math.pi / cell)
    v = (f.X - (f.Y - y0)) * (2 * math.pi / cell)
    p = np.sin(u) * np.sin(v)
    f.shade(mask, 1.0 + k * p)
    return f


def hearts(f, mask, y0, y1, color=GOLD, a=0.55, dx=2.0, size=0.62):
    """Small gold hearts scattered in a staggered grid over a mask (the skirt motif)."""
    n = 0
    y = y0
    while y <= y1:
        x = dx * (0.5 if n % 2 == 0 else 1.0) - dx * 0.5 + (0.0 if n % 2 == 0 else dx * 0.0)
        x = (dx * 0.5) if n % 2 == 0 else dx
        while x < f.w + dx * 0.5:
            hp = f.m_poly(H.heart_pts(x, y, size)) * mask
            f.fill(hp, H.lin((0, y - size * 0.5), (0, y + size * 0.5), [(0, GOLD_L), (1, GOLD)]), a)
            x += dx
        y += 1.15
        n += 1


def gores(f, mask, xs, k=0.16, y0=0.0, y1=None):
    """Skirt gores: panels between the x positions, each lit in the middle and darker at its seams, fading in toward the waist."""
    y1 = f.h if y1 is None else y1
    edges = [0.0] + list(xs) + [f.w]
    kk = np.ones_like(f.X)
    for a, b in zip(edges[:-1], edges[1:]):
        u = np.clip((f.X - a) / max(1e-3, b - a), 0, 1)
        panel = np.sin(u * math.pi)                                        # 0 at the seams, 1 in the middle
        kk = np.where((f.X >= a) & (f.X < b), 1.0 + k * (panel - 0.55), kk)
    fade = H.smoothstep(y0, y0 + 1.0, f.Y)
    f.shade(mask, 1.0 + (kk - 1.0) * fade)
    for x in xs:
        f.fill(mask * np.exp(-((f.X - x) / 0.07) ** 2) * (f.Y > y0), H.tone(PLUM_D, 0.8), 0.5)


def pearls(f, y, n, r=0.2, sag=0.25, color="#fff4ee"):
    """A string of pearls along a gently sagging line across the face."""
    for i in range(n):
        t = (i + 0.5) / n
        x = f.w * t
        yy = y + sag * (1.0 - (2 * t - 1) ** 2)
        f.ellipse(x + 0.04, yy + 0.07, r * 1.1, r * 1.1, "#8a5a60", a=0.45, soft=2.0)
        f.ellipse(x, yy, r, r, H.rad((x - r * 0.35, yy - r * 0.4), r * 1.4, [(0, "#ffffff"), (0.6, color), (1, "#d8b8b8")]))


def lacing(f, x0, x1, y0, y1, n, color=GOLD, width=0.16):
    """Criss-cross gold lacing between two vertical edges: n crossings from y0 to y1."""
    for i in range(n):
        y = y0 + (y1 - y0) * i / max(1, n - 1)
        h = (y1 - y0) / max(1, n - 1) * 0.55
        H.trim_line(f, [(x0, y), (x1, y + h)], color, width=width, outline=0.3, spec=0.2)
        H.trim_line(f, [(x1, y), (x0, y + h)], color, width=width, outline=0.3, spec=0.2)


def head(cv, hair):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=2.0, wave=0.5, shadow=0.2)
    # violet eyeshadow: wide feathered arcs over the eyes, up to the brow
    for cx, rot in ((2.3, -10), (5.7, 10)):
        m = f.m_ellipse(cx, 4.15, 1.85, 1.15, rot=rot, soft=7.0)
        f.fill(m, H.lin((0, 3.1), (0, 5.3), [(0, "#5a1f9a"), (0.5, "#9a60d6"), (1, "#e2b8f4")]), 0.8)
    # angry brows: the inner ends low, the outer ends high
    H.brow(f, (3.45, 3.45), (0.75, 2.45), "#5a1840", 0.34, 0.14, arch=0.12)
    H.brow(f, (4.55, 3.45), (7.25, 2.45), "#5a1840", 0.34, 0.14, arch=0.12)
    iris = ("#3a1470", "#8a50d4", "#e8c8ff")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.95, 2.75, 2.5, iris, lash="#1c0a2a", tilt=0.14, flick=0.5, lash_w=0.44, lid_shadow=0.26)
    for cx, sg in ((2.3, -1), (5.7, 1)):                                       # a second lash flick at the outer corner
        x0 = cx + sg * 1.4
        f.line((x0, 4.6), (x0 + sg * 0.7, 4.0), "#1c0a2a", 0.16, 0.05)
    H.blush(f, 0.9, 6.3, 1.05, 0.7, "#ff6a92", 0.6)
    H.blush(f, 7.1, 6.3, 1.05, 0.7, "#ff6a92", 0.6)
    H.nose(f, 4.0, 6.05, "#d89a90", 0.2)
    H.mouth_smile(f, 4.0, 6.7, 5.4, color="#a0102c", lip="#ff3c5c", depth=0.5, thick=0.36, teeth="#ffffff", open_h=1.05)
    f.ellipse(2.4, 6.78, 0.6, 0.12, "#ffffff", a=0.35, soft=3.0)
    # the gold tiara on the second layer and a jewel; also drawn on the base so it survives the LOD cut
    for nm in SIDES:
        for g in lay(cv, "head", nm):
            if g.overlay:
                g.clear()
            H.trim_band(g, 1.6, 2.6, GOLD, outline=0.5)
            for x in ((1.0, 2.2, 5.8, 7.0) if nm == "front" else (1.0, 2.5, 4.0, 5.5, 7.0)):
                if nm == "front" and 3.0 < x < 5.0:
                    continue
                H.stud(g, x, 2.1, 0.17, GOLD_L)
    for g in lay(cv, "head", "front"):
        g.poly([(3.0, 1.7), (4.0, 0.05), (5.0, 1.7)], H.metal_paint(0.0, 1.8, GOLD))
        H.bevel(g, g.m_poly([(3.0, 1.7), (4.0, 0.05), (5.0, 1.7)]), depth=0.25, gain=1.0, spec=0.4)
        H.gem(g, 4.0, 2.1, 0.5, "#ff2a68", setting=GOLD, glow=True)


def collar(f, notch=False):
    m = f.m_band(0.0, 2.9)
    if notch:
        m = H.minus(m, f.m_poly([(3.2, 0.9), (4.8, 0.9), (4.0, 3.0)], soft=1.4))
    H.scallops(f, m, 0.0, rows=2, cell=1.3, depth=1.75, base=LACE, tip="#ffffff", edge=LACE_D, edge_a=0.45, shadow=0.20)
    if not notch:
        pearls(f, 2.5, 8 if f.w > 5 else 4, r=0.27, sag=0.35)


def bodice_base(f, back=False):
    m = f.m_band(0.0, 7.2)
    f.fill(m, H.lin((0, 0), (0, 7.2), [(0, H.tone(PINK, 1.12)), (0.55, PINK), (1, H.tone(PINK, 0.78))]))
    away = f.X if f.light[0] > 0 else f.w - f.X
    f.shade(m, 1.0 - 0.10 * H.smoothstep(0.4 * f.w, f.w, away))
    H.sheen(f, m, [1.0, f.w - 1.0], width=0.26, strength=0.20, tilt=0.1, y0=0.6, y1=6.8, color="#ffd2e2")
    damask(f, m, PINK, cell=1.5, k=0.05)


def sash(f, back=False):
    s = f.m_band(7.1, 8.9)
    f.fill(s, H.lin((0, 7.1), (0, 8.9), [(0, H.tone(PLUM, 1.25)), (0.5, PLUM), (1, PLUM_D)]))
    H.folds(f, s, [1.2, 2.9, 5.1, 6.8] if f.w > 5 else [1.0, 3.0], depth=0.14, width=0.2, y0=7.1, y1=8.9)
    H.trim_band(f, 7.0, 7.45, GOLD, outline=0.5)
    H.trim_band(f, 8.55, 9.0, GOLD, outline=0.5)
    H.shadow_under(f, 9.0, 0.55, 0.28)


def skirt(f, front=True):
    m = f.m_band(8.9, 12.0)
    f.fill(m, H.lin((0, 8.9), (0, 12.0), [(0, H.tone(PLUM, 0.85)), (0.5, PLUM), (1, H.tone(PLUM_L, 0.95))]))
    xs = [0.9, 2.2, 3.6, 4.9, 6.3, 7.4] if f.w > 5 else [1.0, 3.0]
    gores(f, m, [2.0, 4.0, 6.0] if f.w > 5 else [2.0], k=0.22, y0=8.9, y1=12.0)
    hearts(f, f.m_band(9.0, 11.05), 9.85, 10.7, dx=2.0, size=1.15, a=0.8)
    H.sheen(f, m, [2.0, 5.6] if f.w > 5 else [1.4], width=0.24, strength=0.22, tilt=0.0, y0=9.2, y1=11.5, color="#ff9acc")
    # the gold hem with little scallops hanging from it
    H.trim_band(f, 11.1, 11.75, GOLD, outline=0.5)
    H.scallops(f, f.m_band(11.6, 12.0), 11.6, rows=1, cell=0.8, depth=0.5, base=GOLD, tip=GOLD_L, edge=GOLD_D, edge_a=0.6, shadow=0.1)


def body_front(f):
    bodice_base(f)
    # the cream brocade stomacher with the big heart
    st = f.m_poly([(2.0, 2.4), (6.0, 2.4), (5.65, 7.2), (2.35, 7.2)], soft=1.0)
    f.fill(st, H.lin((0, 2.4), (0, 7.2), [(0, "#fffaf0"), (1, "#f2dcc8")]))
    damask(f, st, CREAM, cell=1.3, k=0.06, y0=2.4)
    H.trim_line(f, [(2.0, 2.4), (2.35, 7.2)], GOLD, width=0.26)
    H.trim_line(f, [(6.0, 2.4), (5.65, 7.2)], GOLD, width=0.26)
    lacing(f, 1.25, 1.85, 2.9, 6.6, 5)
    lacing(f, 6.15, 6.75, 2.9, 6.6, 5)
    hp = f.m_poly(H.heart_pts(4.0, 4.7, 3.3))
    f.fill(f.m_poly(H.heart_pts(4.0, 4.74, 3.6)), H.tone(HEART, 0.4), 0.9)
    f.fill(hp, H.rad((3.2, 4.0), 2.6, [(0, "#ff6a80"), (0.55, HEART), (1, H.tone(HEART, 0.6))]))
    H.bevel(f, hp, depth=0.34, gain=0.9, spec=0.55, spec_pow=10)
    f.ellipse(3.1, 4.05, 0.5, 0.17, "#ffffff", a=0.75, rot=-35, soft=1.8)
    H.sparkle(f, 5.0, 4.9, 0.45, "#ffffff", 0.85)
    sash(f)
    # buckle: a gold oval with a jewel
    f.ellipse(4.0, 8.0, 1.05, 0.95, H.tone(GOLD_D, 0.6))
    f.ellipse(4.0, 8.0, 0.92, 0.82, H.rad((3.6, 7.6), 1.5, [(0, GOLD_L), (0.5, GOLD), (1, GOLD_D)]), glow=GOLD)
    H.gem(f, 4.0, 8.0, 0.4, HEART, setting=None, glow=True)
    skirt(f)
    collar(f)
    H.stud(f, 4.0, 2.55, 0.34, GOLD, glow=True)


def body_back(f):
    bodice_base(f, True)
    lacing(f, 3.4, 4.6, 1.9, 6.9, 7)
    H.trim_line(f, [(3.4, 1.8), (3.4, 7.0)], GOLD, width=0.2, outline=0.3)
    H.trim_line(f, [(4.6, 1.8), (4.6, 7.0)], GOLD, width=0.2, outline=0.3)
    sash(f, True)
    skirt(f, False)
    collar(f, notch=False)


def body_side(f):
    bodice_base(f)
    sash(f)
    skirt(f, False)
    collar(f)


def bow(f, cx, cy):
    """The big plum satin bow at the back of the sash: two loops, two tails, a gold knot."""
    sat = lambda a, b: H.lin((0, cy - 1.3), (0, cy + 3.0), [(0, a), (1, b)])
    for sg in (-1, 1):
        tail = f.m_poly([(cx + sg * 0.3, cy + 0.3), (cx + sg * 1.9, cy + 3.0), (cx + sg * 0.9, cy + 3.15), (cx + sg * 0.1, cy + 1.0)])
        f.fill(f.m_poly([(cx + sg * 0.3, cy + 0.3), (cx + sg * 2.02, cy + 3.12), (cx + sg * 0.84, cy + 3.3), (cx, cy + 1.05)]), H.tone(PLUM_D, 0.7), 0.9)
        f.fill(tail, sat(PLUM_L, PLUM_D))
        H.trim_line(f, [(cx + sg * 1.9, cy + 3.0), (cx + sg * 0.9, cy + 3.15)], GOLD, width=0.2, outline=0.3)
    for sg in (-1, 1):
        loop = f.m_ellipse(cx + sg * 1.55, cy - 0.1, 1.55, 1.0, rot=sg * 20)
        f.fill(f.m_ellipse(cx + sg * 1.55, cy, 1.68, 1.1, rot=sg * 20), H.tone(PLUM_D, 0.7), 0.9)
        f.fill(loop, sat("#e060ae", PLUM_D))
        f.fill(f.m_ellipse(cx + sg * 1.6, cy - 0.05, 0.95, 0.5, rot=sg * 20), H.tone(PLUM_D, 0.7), 0.85)
        H.bevel(f, loop, depth=0.3, gain=0.8, spec=0.5, spec_pow=8)
        f.ellipse(cx + sg * 1.25, cy - 0.5, 0.7, 0.12, "#ffffff", rot=sg * 20, a=0.5, soft=2.5)
    knot = f.m_ellipse(cx, cy + 0.05, 0.55, 0.7)
    f.fill(f.m_ellipse(cx, cy + 0.1, 0.65, 0.8), H.tone(GOLD_D, 0.6), 0.95)
    f.fill(knot, H.rad((cx - 0.2, cy - 0.25), 0.9, [(0, GOLD_L), (0.5, GOLD), (1, GOLD_D)]))
    H.bevel(f, knot, depth=0.25, gain=0.8, spec=0.6, spec_pow=8)


def body(cv):
    for nm in SIDES:
        for f in lay(cv, "body", nm):
            if nm == "front":
                body_front(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f)
    # the overlay keeps only the lace collar and the sash and skirt (the flare); the bodice is the base layer
    for nm in SIDES:
        f = cv.face("body", nm, True)
        f.clear(f.m_band(2.9, 7.0))
    f = cv.face("body", "back", False)
    bow(f, 4.0, 9.0)                                                       # base layer, over the skirt top
    f = cv.face("body", "back", True)
    bow(f, 4.0, 9.0)
    for part_f in (cv.face("body", "top", False), cv.face("body", "top", True)):
        part_f.fill(part_f.m_all(), H.rad((part_f.w * 0.4, part_f.h * 0.4), part_f.w, [(0, "#ffffff"), (1, LACE_D)]))


def sleeve(f, nm):
    w = f.w
    # the puff: pink satin gathered into a band at y 4.6, a white scalloped lace cuff at 6.4, the forearm bare underneath
    puff = f.m_band(0.0, 4.9)
    f.fill(puff, H.rad((w * 0.4, 1.4), 4.2, [(0, H.tone(PINK, 1.3)), (0.55, PINK), (1, H.tone(PINK, 0.72))]))
    H.folds(f, puff, [0.8, 1.9, 3.1], depth=0.18, width=0.2, y0=0.4, y1=4.9, sway=0.15)
    H.sheen(f, puff, [1.1, 2.9], width=0.22, strength=0.28, tilt=0.0, y0=0.5, y1=4.2, color="#ffd0e2")
    H.trim_band(f, 4.5, 5.3, GOLD, outline=0.5)
    H.stud(f, w * 0.5, 4.9, 0.14, GOLD_L)
    lower = f.m_band(5.3, 6.9)
    f.fill(lower, H.lin((0, 5.3), (0, 6.9), [(0, H.tone(PINK, 0.85)), (1, H.tone(PINK, 0.65))]))
    H.scallops(f, f.m_band(6.2, 9.0), 6.2, rows=2, cell=1.0, depth=1.5, base=LACE, tip="#ffffff", edge=LACE_D, edge_a=0.45, shadow=0.2)


def arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            base, ov = lay(cv, part, nm)
            # bare forearm and hand on the base layer
            base.fill(base.m_all(), H.lin((0, 5), (0, 12), [(0, SKIN[1]), (1, SKIN[0])]))
            base.shade(base.m_band(7.0, 12.0), 1.0 - 0.07 * H.smoothstep(0.45 * base.w, base.w, base.X if base.light[0] > 0 else base.w - base.X))
            # a gold bangle at the wrist, red nails on the hand
            for g in (base, ov):
                if g.overlay:
                    g.clear()
                sleeve(g, nm)
                H.trim_band(g, 9.4, 10.2, GOLD, outline=0.5)
        for f in lay(cv, part, "top"):
            f.fill(f.m_all(), H.rad((f.w * 0.4, f.h * 0.4), f.w, [(0, H.tone(PINK, 1.3)), (1, H.tone(PINK, 0.75))]))
        for f in lay(cv, part, "bottom"):
            f.fill(f.m_all(), SKIN[0])


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            if f.overlay:
                f.clear()
        w = base.w
        # base: the skirt down to the knee, striped stockings, wine boots
        base.fill(base.m_band(0.0, 6.8), H.lin((0, 0), (0, 6.8), [(0, H.tone(PLUM, 0.85)), (1, PLUM)]))
        gores(base, base.m_band(0.0, 6.8), [2.0], k=0.2, y0=0.0, y1=6.8)
        hearts(base, base.m_band(0.2, 6.4), 1.2, 5.0, dx=2.0, size=1.15, a=0.75)
        # stockings: pink and white stripes
        st = base.m_band(6.6, 8.8)
        stripe = ((base.Y - 6.6) // 0.6).astype(int) % 2 == 0
        base.fill(st, np.where(stripe[..., None], H.col("#fff2f6"), H.col("#f0608f")).astype(np.float32))
        H.sheen(base, st, [1.1], width=0.2, strength=0.25, y0=6.8, y1=8.6)
        H.shadow_under(base, 6.6, 0.5, 0.24)
        # wine boot: gold cuff, lace ruffle, lacing, dark sole
        boot = base.m_band(8.6, 12.0)
        base.fill(boot, H.lin((0, 8.6), (0, 12.0), [(0, H.tone(WINE, 1.25)), (0.5, WINE), (1, WINE_D)]))
        H.sheen(base, boot, [1.0], width=0.2, strength=0.28, tilt=0.05, y0=9.6, y1=11.5, color="#ff9ac0")
        H.trim_band(base, 8.6, 9.2, GOLD, outline=0.5)
        if nm == "front":
            lacing(base, 1.0, 3.0, 9.8, 11.2, 4, width=0.13)
            for y in (9.8, 10.55, 11.3):
                H.stud(base, 1.0, y, 0.11, GOLD_L)
                H.stud(base, 3.0, y, 0.11, GOLD_L)
        elif nm in ("west", "east"):
            H.trim_line(base, [(0.0, 11.0), (w, 11.0)], GOLD, width=0.16, outline=0.3)
        base.fill(base.m_band(11.62, 12.0), H.tone(WINE_D, 0.6), 0.9)
        # overlay: the skirt flare (plum, gold hem, lace petticoat) and the lace boot ruffle
        ov.fill(ov.m_band(0.0, 5.2), H.lin((0, 0), (0, 5.2), [(0, H.tone(PLUM, 0.9)), (1, PLUM)]))
        gores(ov, ov.m_band(0.0, 5.2), [2.0], k=0.2, y0=0.0, y1=5.2)
        hearts(ov, ov.m_band(0.2, 4.6), 1.2, 4.0, dx=2.0, size=1.15, a=0.8)
        H.trim_band(ov, 4.7, 5.35, GOLD, outline=0.5)
        H.scallops(ov, ov.m_band(5.3, 7.0), 5.3, rows=2, cell=1.0, depth=1.2, base=LACE, tip="#ffffff", edge=LACE_D, edge_a=0.45, shadow=0.24)
        H.trim_band(ov, 8.6, 9.2, GOLD, outline=0.5)
        H.scallops(ov, ov.m_band(8.9, 9.9), 8.95, rows=1, cell=1.0, depth=0.85, base=LACE, tip="#ffffff", edge=LACE_D, edge_a=0.45, shadow=0.2)
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), WINE_D)
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), PLUM)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    hair = "anime_curls"
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.near("#ffd24a", 0.2)], "skin": [H.near("#f7d2c4", 0.12)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arms(cv)
    for part in ("leg_r", "leg_l"):
        leg(cv, part)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

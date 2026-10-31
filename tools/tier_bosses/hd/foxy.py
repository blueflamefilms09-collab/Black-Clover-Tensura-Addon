"""Foxy, the Silver Fox (One Piece, captain of the Foxy Pirates), HD 256x256 (3.38.0, group c of docs/bosses/hd_outfits.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/foxy.py) through the HD bridge; then drawn again at 4x detail: the sly face (sharp brown eyes with heavy
lids, arched silver brows, a smirk under a silver moustache and a pointed goatee), the huge fluffy silver fur collar, the pale blue-grey captain's coat with slate lining, orange piping and topstitching,
the cream shirt, the orange sash with a cream-gold buckle, the dark slate pauldrons with an orange rim and fur trim, the orange cuffs, the coat tails with an orange hem, the dark trousers and the dark
boots with big orange cuffs, and the orange fox-head crest on the back. Hair: `anime_afro_wide` (a wide full afro attached to the head), silver. Original art from memory of the canon look. Glow: none.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

COAT, COAT_L, COAT_D = "#8ea6b6", "#b4cad8", "#566e80"
LINING, LINING_D = "#4a6270", "#2c3e4a"
ORANGE, ORANGE_L, ORANGE_D = "#ff8a3c", "#ffb878", "#b4501c"
CREAM, CREAM_D = "#f1e8d6", "#cfc2a8"
TROUSER, TROUSER_D = "#33444f", "#1e2a32"
BOOT, BOOT_D = "#202c35", "#10171c"
FUR, FUR_L, FUR_D = "#d4d8e2", "#f6f8fc", "#8a90a2"
GOLD = "#e8cf86"
SKIN = ("#d9a98c", "#e9c3a8", "#f5dac6")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def fluff(f, mask, y0, y1, n=60, seed=0):
    """Short slanted strokes in lighter and darker silver over a fur area: reads as fluffy hair instead of a smooth frill."""
    m_all = np.asarray(mask, np.float32)
    for i in range(n):
        x = f.w * H.hsh(i, seed, 1)
        y = y0 + (y1 - y0) * H.hsh(i, seed, 2)
        ln = 0.45 + 0.5 * H.hsh(i, seed, 3)
        ang = math.radians(70 + 40 * H.hsh(i, seed, 4))
        x1, y1_ = x + math.cos(ang) * ln * 0.4, y + math.sin(ang) * ln
        light = H.hsh(i, seed, 5) > 0.45
        f.fill(f.m_line((x, y), (x1, y1_), 0.1, 0.04) * m_all, FUR_L if light else FUR_D, 0.65 if light else 0.5)


def fur(f, y0, rows=3, cell=1.25, depth=1.7, y1=None, notch=False, seed=0):
    yy1 = y1 if y1 is not None else y0 + depth + 0.62 * (rows - 1)
    m = f.m_band(y0 - 0.1, yy1)
    if notch and f.w > 5:
        m = H.minus(m, f.m_poly([(3.0, -0.2), (5.0, -0.2), (4.0, 2.5)], soft=1.4))
    H.scallops(f, m, y0, rows=rows, cell=cell, depth=depth, base=FUR, tip=FUR_L, edge=FUR_D, edge_a=0.6, shadow=0.28)
    fluff(f, f.m_alpha() * m, y0, yy1, n=int(26 * f.w * (yy1 - y0) / 8.0) + 10, seed=seed)


def coat_cloth(f, mask, y0, y1, top=1.1, bottom=0.82):
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, H.tone(COAT_L, top * 0.96)), (0.45, COAT), (1, H.tone(COAT_D, bottom))]))
    away = f.X if f.light[0] > 0 else f.w - f.X
    f.shade(mask, 1.0 - 0.14 * H.smoothstep(0.35 * f.w, f.w, away))
    H.sheen(f, mask, [f.w * 0.3, f.w * 0.75], width=0.3, strength=0.14, tilt=0.05, y0=y0 + 0.4, y1=y1 - 0.4, color="#e8f4ff")


def scroll(f, x0, x1, y, amp=0.28, n=3, color=ORANGE, a=0.9):
    """An embroidered orange wave with curls along a coat hem: a sine ribbon with a small spiral at each crest."""
    pts = [(x0 + (x1 - x0) * i / 24.0, y + amp * math.sin(i / 24.0 * n * math.pi * 2)) for i in range(25)]
    f.fill(f.m_polyline(pts, 0.11), color, a)
    for k in range(n):
        cx = x0 + (x1 - x0) * (k + 0.25) / n
        f.fill(f.m_ring(cx, y - amp * 1.2, 0.2, 0.08), color, a)


def head(cv, hair):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=2.0, wave=0.5, shadow=0.2)
    # sharp, narrow brown eyes with heavy lids, silver arched brows
    H.brow(f, (3.5, 3.5), (0.8, 2.75), "#b8bfcc", 0.3, 0.12, arch=0.2)
    H.brow(f, (4.5, 3.5), (7.2, 2.75), "#b8bfcc", 0.3, 0.12, arch=0.2)
    iris = ("#2a1408", "#7a4620", "#d49c5c")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 4.85, 2.55, 1.85, iris, lash="#2a1a14", tilt=0.2, flick=0.4, lash_w=0.4, lid_shadow=0.34, sclera="#f6f0e8")
    H.nose(f, 4.0, 5.75, "#c08a74", 0.24)
    # a wide sly grin with teeth, a silver moustache above it curling up at the ends, a pointed goatee below
    H.mouth_smile(f, 4.0, 6.85, 3.7, color="#6a2428", lip="#b06a60", depth=0.32, thick=0.17, teeth="#ffffff", open_h=0.42)
    mo = [(1.7, 6.55), (2.5, 6.1), (3.4, 6.2), (4.0, 6.35), (4.6, 6.2), (5.5, 6.1), (6.3, 6.55)]
    f.fill(f.m_polyline([(x, y + 0.14) for x, y in mo], 0.34), "#4a4252", 0.35)
    f.fill(f.m_polyline(mo, 0.3), H.lin((0, 5.9), (0, 6.7), [(0, "#eef0f6"), (1, "#a4acbc")]))
    for k in range(7):
        f.line((mo[k][0], mo[k][1] - 0.12), (mo[k + 1 if k < 6 else k][0], mo[k + 1 if k < 6 else k][1] + 0.1), "#ffffff", 0.05, 0.03, a=0.7)
    gt = f.m_poly([(3.0, 7.55), (5.0, 7.55), (4.7, 8.0), (4.0, 8.25), (3.3, 8.0)])
    f.fill(gt, H.lin((0, 7.5), (0, 8.25), [(0, "#e4e8f0"), (1, "#8e96a8")]))
    for x in (3.3, 3.65, 4.0, 4.35, 4.7):
        f.line((x, 7.6), (4.0 + (x - 4.0) * 0.5, 8.1), "#7a8294", 0.06, 0.03, a=0.55)
    # smile creases
    f.line((1.2, 5.7), (1.6, 6.6), "#c89a84", 0.1, 0.05, a=0.5)
    f.line((6.8, 5.7), (6.4, 6.6), "#c89a84", 0.1, 0.05, a=0.5)


def collar(f, back=False):
    """The huge fluffy silver fur collar: three rows of long tufts, open at the front where the cream shirt shows."""
    fur(f, 0.0, rows=3, cell=1.3, depth=2.1, notch=not back, seed=3 if back else 1)


def body_front(f):
    # cream shirt and trousers underneath
    sh = f.m_band(0.0, 7.4)
    f.fill(sh, H.lin((0, 0), (0, 7.4), [(0, H.tone(CREAM, 1.05)), (1, H.tone(CREAM, 0.88))]))
    H.folds(f, sh, [3.3, 4.7], depth=0.1, width=0.2, y0=0.5, y1=7.4)
    tr = f.m_band(8.7, 12.0)
    f.fill(tr, H.lin((0, 8.7), (0, 12.0), [(0, TROUSER), (1, TROUSER_D)]))
    H.folds(f, tr, [3.7, 4.3], depth=0.14, width=0.2, y0=8.7, y1=12.0)
    # the coat panels, with the slate lining along the open edge
    for sg in (-1, 1):
        x = (lambda v: v) if sg < 0 else (lambda v: 8.0 - v)
        panel = f.m_poly([(x(0.0), 0.0), (x(3.1), 0.0), (x(2.75), 7.5), (x(2.55), 12.0), (x(0.0), 12.0)])
        coat_cloth(f, panel, 0.0, 12.0)
        H.folds(f, panel, [x(0.9), x(1.9)] if sg < 0 else [x(0.9), x(1.9)], depth=0.1, width=0.22, y0=2.0, y1=12.0)
        lin_ = f.m_poly([(x(2.35), 0.0), (x(3.1), 0.0), (x(2.75), 7.5), (x(2.55), 12.0), (x(1.95), 12.0), (x(2.1), 7.5)])
        f.fill(lin_ * panel, H.lin((0, 0), (0, 12.0), [(0, LINING), (1, LINING_D)]))
        H.trim_line(f, [(x(3.1), 0.0), (x(2.75), 7.5), (x(2.55), 12.0)], ORANGE, width=0.2, outline=0.45)
        H.stitch(f, [(x(2.2), 0.5), (x(1.95), 7.5), (x(1.8), 11.5)], "#dce6ee", width=0.06, dash=(0.3, 0.2), shadow=0.2)
        for y in (3.1, 4.6, 6.1):
            H.stud(f, x(0.9), y, 0.17, GOLD)
        scroll(f, x(0.4), x(2.2), 10.2, n=1, amp=0.2)
    # orange sash with a cream-gold buckle
    sash = f.m_band(7.1, 8.8)
    f.fill(sash, H.lin((0, 7.1), (0, 8.8), [(0, H.tone(ORANGE, 1.15)), (0.5, ORANGE), (1, ORANGE_D)]))
    H.folds(f, sash, [1.0, 2.5, 5.5, 7.0], depth=0.12, width=0.18, y0=7.1, y1=8.8)
    H.sheen(f, sash, [1.6, 6.3], width=0.2, strength=0.25, y0=7.2, y1=8.7, color="#ffe0b8")
    H.shadow_under(f, 8.8, 0.5, 0.3)
    bk = f.m_rect(3.0, 7.2, 5.0, 8.7, r=0.25)
    f.fill(f.m_rect(2.92, 7.12, 5.08, 8.78, r=0.3), H.tone(ORANGE_D, 0.5), 0.9)
    f.fill(bk, H.metal_paint(7.2, 8.7, GOLD))
    H.bevel(f, bk, depth=0.3, gain=1.2, spec=0.6, spec_pow=10)
    f.rect(3.5, 7.55, 4.5, 8.35, H.tone(CREAM_D, 0.7), r=0.15)
    f.rect(3.62, 7.67, 4.38, 8.23, H.lin((0, 7.6), (0, 8.2), [(0, "#fff4d8"), (1, CREAM_D)]), r=0.1)
    # shirt collar and the fur
    f.poly([(2.9, 0.0), (4.0, 2.6), (3.4, 0.0)], H.tone(CREAM, 0.9))
    f.poly([(5.1, 0.0), (4.0, 2.6), (4.6, 0.0)], H.tone(CREAM, 0.9))
    collar(f)


def fox_crest(f, cx, cy, s):
    """The Foxy Pirates' fox head in orange with a dark outline, an inner shade, slit eyes and a dark nose."""
    ear = lambda sg: [(cx + sg * 1.45 * s, cy - 0.6 * s), (cx + sg * 1.25 * s, cy - 2.0 * s), (cx + sg * 0.25 * s, cy - 0.95 * s)]
    head_pts = [(cx - 1.5 * s, cy - 0.7 * s), (cx + 1.5 * s, cy - 0.7 * s), (cx + 1.0 * s, cy + 0.35 * s), (cx + 0.18 * s, cy + 1.45 * s), (cx - 0.18 * s, cy + 1.45 * s), (cx - 1.0 * s, cy + 0.35 * s)]
    for pts in (ear(-1), ear(1), head_pts):
        f.fill(f.m_poly([(cx + (x - cx) * 1.14, cy + (y - cy) * 1.14) for x, y in pts]), H.tone(ORANGE_D, 0.45), 0.9)
    m = f.m_poly(head_pts)
    for pts in (ear(-1), ear(1)):
        m = H.union(m, f.m_poly(pts))
    f.fill(m, H.lin((0, cy - 2.0 * s), (0, cy + 1.5 * s), [(0, ORANGE_L), (0.5, ORANGE), (1, ORANGE_D)]))
    for sg in (-1, 1):
        f.poly([(cx + sg * 1.18 * s, cy - 0.7 * s), (cx + sg * 1.0 * s, cy - 1.5 * s), (cx + sg * 0.5 * s, cy - 0.8 * s)], H.tone(ORANGE_D, 0.75), 0.9)
        f.poly([(cx + sg * 0.35 * s, cy - 0.15 * s), (cx + sg * 1.05 * s, cy - 0.35 * s), (cx + sg * 0.8 * s, cy + 0.05 * s)], "#2a1608", 0.95)
    f.poly([(cx - 0.55 * s, cy + 0.55 * s), (cx + 0.55 * s, cy + 0.55 * s), (cx, cy + 1.3 * s)], CREAM, 0.9)
    f.ellipse(cx, cy + 1.38 * s, 0.22 * s, 0.17 * s, "#1a0e08")
    H.bevel(f, m, depth=0.3, gain=0.9, spec=0.4, spec_pow=10)


def body_back(f):
    m = f.m_band(0.0, 12.0)
    coat_cloth(f, m, 0.0, 12.0)
    H.folds(f, m, [0.9, 2.0, 6.0, 7.1], depth=0.12, width=0.22, y0=2.0, y1=12.0)
    H.trim_line(f, [(4.0, 3.5), (4.0, 12.0)], COAT_D, width=0.14, outline=0.0, spec=0.0, a=0.8)          # centre seam
    H.stitch(f, [(3.75, 3.5), (3.75, 11.5)], "#dce6ee", width=0.05, dash=(0.3, 0.2), shadow=0.2)
    H.stitch(f, [(4.25, 3.5), (4.25, 11.5)], "#dce6ee", width=0.05, dash=(0.3, 0.2), shadow=0.2)
    fox_crest(f, 4.0, 5.1, 1.25)
    sash = f.m_band(7.4, 9.0)
    f.fill(sash, H.lin((0, 7.4), (0, 9.0), [(0, H.tone(ORANGE, 1.15)), (0.5, ORANGE), (1, ORANGE_D)]))
    H.folds(f, sash, [1.0, 2.5, 5.5, 7.0], depth=0.12, width=0.18, y0=7.4, y1=9.0)
    H.shadow_under(f, 9.0, 0.5, 0.3)
    # the sash's knot and two tails
    for sg in (-1, 1):
        f.poly([(4.0 + sg * 0.2, 8.5), (4.0 + sg * 1.5, 10.8), (4.0 + sg * 0.7, 11.0), (4.0, 9.2)], H.lin((0, 8.5), (0, 11), [(0, ORANGE), (1, ORANGE_D)]))
    f.ellipse(4.0, 8.4, 0.7, 0.62, H.rad((3.8, 8.2), 1.0, [(0, ORANGE_L), (1, ORANGE_D)]))
    H.bevel(f, f.m_ellipse(4.0, 8.4, 0.7, 0.62), depth=0.25, gain=0.9, spec=0.5)
    H.trim_band(f, 11.2, 11.7, ORANGE, outline=0.45)
    scroll(f, 0.4, f.w - 0.4, 10.2, n=4)
    collar(f, back=True)


def body_side(f):
    m = f.m_band(0.0, 12.0)
    coat_cloth(f, m, 0.0, 12.0)
    H.folds(f, m, [1.0, 2.8], depth=0.12, width=0.22, y0=2.0, y1=12.0)
    sash = f.m_band(7.1, 8.8)
    f.fill(sash, H.lin((0, 7.1), (0, 8.8), [(0, H.tone(ORANGE, 1.15)), (0.5, ORANGE), (1, ORANGE_D)]))
    H.shadow_under(f, 8.8, 0.5, 0.3)
    H.trim_band(f, 11.2, 11.7, ORANGE, outline=0.45)
    scroll(f, 0.3, f.w - 0.3, 10.2, n=2)
    collar(f)


def body(cv):
    for nm in SIDES:
        for f in lay(cv, "body", nm):
            if nm == "front":
                body_front(f)
            elif nm == "back":
                body_back(f)
            else:
                body_side(f)
    for f in lay(cv, "body", "top"):
        f.fill(f.m_all(), H.rad((f.w * 0.5, f.h * 0.5), f.w * 0.8, [(0, FUR_L), (1, FUR)]))
    # the overlay keeps the fur collar, the coat panels, the sash and the coat tails; the shirt in the open middle is the base layer
    f = cv.face("body", "front", True)
    opening = f.m_poly([(3.1, 0.0), (4.9, 0.0), (5.25, 7.5), (5.45, 12.0), (2.55, 12.0), (2.75, 7.5)], soft=1.0)
    f.clear(opening * (f.m_band(3.6, 7.05) + f.m_band(8.85, 12.0)))


def sleeve(f, nm):
    w = f.w
    m = f.m_band(3.2, 8.2)
    coat_cloth(f, m, 3.2, 8.2, 1.05, 0.8)
    H.folds(f, m, [1.0, 2.9], depth=0.14, width=0.22, y0=3.4, y1=8.2)
    # orange cuff: two bands with a fold between
    H.trim_band(f, 7.0, 7.55, ORANGE, outline=0.5)
    cf = f.m_band(7.55, 9.4)
    f.fill(cf, H.lin((0, 7.55), (0, 9.4), [(0, ORANGE), (1, ORANGE_D)]))
    H.folds(f, cf, [1.0, 2.9], depth=0.15, width=0.2, y0=7.55, y1=9.4)
    H.trim_band(f, 9.0, 9.55, ORANGE_L, outline=0.5)
    H.stitch(f, [(0.0, 8.3), (w, 8.3)], "#ffd8b0", width=0.05, dash=(0.25, 0.2), shadow=0.1)
    H.shadow_under(f, 9.55, 0.5, 0.25)
    # the slate pauldron with an orange rim and rivets, fur trim over it
    pm = f.m_rect(-0.3, 0.6, w + 0.3, 4.1, r=0.7)
    f.fill(f.m_rect(-0.3, 0.62, w + 0.3, 4.2, r=0.7), H.tone(LINING_D, 0.6), 0.8)
    f.fill(pm, H.lin((0, 0.6), (0, 4.1), [(0, H.tone("#5a7686", 1.2)), (0.5, "#4a6270"), (1, "#2e404c")]))
    H.bevel(f, pm, depth=0.45, gain=1.2, spec=0.2, spec_pow=24)
    H.trim_band(f, 3.5, 4.1, ORANGE, outline=0.5)
    H.trim_line(f, [(0.0, 2.3), (w, 2.3)], "#7a98aa", width=0.1, outline=0.0, spec=0.0, a=0.6)
    for x in (0.7, w - 0.7):
        H.stud(f, x, 3.0, 0.16, GOLD)
    fur(f, 0.0, rows=2, cell=1.0, depth=1.5, seed=7)


def arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            base, ov = lay(cv, part, nm)
            base.fill(base.m_all(), H.lin((0, 9), (0, 12), [(0, SKIN[1]), (1, SKIN[0])]))
            for f in (base, ov):
                if f.overlay:
                    f.clear()
                sleeve(f, nm)
            # a few knuckle lines on the bare hand
            for x in (1.0, 2.0, 3.0):
                base.line((x, 11.0), (x, 12.0), SKIN[0], 0.05, 0.05, a=0.6)
        for f in lay(cv, part, "top"):
            f.fill(f.m_all(), H.rad((f.w * 0.45, f.h * 0.45), f.w, [(0, FUR_L), (1, FUR)]))
        for f in lay(cv, part, "bottom"):
            f.fill(f.m_all(), SKIN[0])


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        w = base.w
        for f in (base, ov):
            if f.overlay:
                f.clear()
        # base: trousers, boots with big orange cuffs
        tr = base.m_band(0.0, 8.4)
        base.fill(tr, H.lin((0, 0), (0, 8.4), [(0, TROUSER), (1, TROUSER_D)]))
        H.folds(base, tr, [1.0, 2.9], depth=0.12, width=0.22, y0=0.0, y1=8.4)
        cuff = base.m_band(8.3, 9.9)
        base.fill(cuff, H.lin((0, 8.3), (0, 9.9), [(0, ORANGE_L), (0.4, ORANGE), (1, ORANGE_D)]))
        H.folds(base, cuff, [1.0, 2.9], depth=0.14, width=0.2, y0=8.3, y1=9.9)
        H.trim_band(base, 8.3, 8.7, ORANGE_L, outline=0.5)
        H.shadow_under(base, 9.9, 0.5, 0.3)
        boot = base.m_band(9.9, 12.0)
        base.fill(boot, H.lin((0, 9.9), (0, 12.0), [(0, "#2e3c46"), (0.5, BOOT), (1, BOOT_D)]))
        H.sheen(base, boot, [1.1], width=0.2, strength=0.3, y0=10.2, y1=11.6)
        H.trim_line(base, [(0.0, 10.6), (w, 10.6)], "#46586a", width=0.1, outline=0.0, spec=0.0, a=0.8)
        base.fill(base.m_band(11.65, 12.0), "#080c10", 0.9)
        # overlay: the coat tails (pale coat, orange hem) down to the knee, the orange cuff and the boot top
        tails = ov.m_band(0.0, 6.4)
        coat_cloth(ov, tails, 0.0, 6.4, 1.05, 0.8)
        H.folds(ov, tails, [1.0, 2.9], depth=0.14, width=0.22, y0=0.0, y1=6.4)
        H.trim_band(ov, 5.9, 6.6, ORANGE, outline=0.5)
        scroll(ov, 0.2, w - 0.2, 4.6, n=2, amp=0.22)
        if nm == "front":
            H.stitch(ov, [(0.35, 0.2), (0.35, 5.6)], "#dce6ee", width=0.05, dash=(0.3, 0.2), shadow=0.2)
        ov.fill(ov.m_band(8.3, 9.9), H.lin((0, 8.3), (0, 9.9), [(0, ORANGE_L), (0.4, ORANGE), (1, ORANGE_D)]))
        H.folds(ov, ov.m_band(8.3, 9.9), [1.0, 2.9], depth=0.14, width=0.2, y0=8.3, y1=9.9)
        H.trim_band(ov, 8.3, 8.7, ORANGE_L, outline=0.5)
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), BOOT_D)
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), COAT_D)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    hair = "anime_afro_wide"
    hrgb = (232, 236, 246)                                                         # a brighter silver: the kit tint renders the remake's #d4d8e2 as a flat grey
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.near("#ffb878", 0.2)], "skin": [H.near("#e9c3a8", 0.12)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arms(cv)
    for part in ("leg_r", "leg_l"):
        leg(cv, part)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

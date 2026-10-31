"""Ao (Boruto / Naruto, the Hidden Mist shinobi with the stolen Byakugan), HD 256x256 (3.38.0, group c of docs/bosses/hd_outfits.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/ao.py) through the HD bridge; then drawn again at 4x detail: the calm face (ONE glowing pale-lavender
Byakugan eye with a pupil-less iris and the temple veins, a black leather eyepatch with straps and stitching over the other, a flat brow, a thin mouth), the navy shinobi headband with its steel plate
and wave mark, the sky-blue open knee-length coat with white piping and a white stand collar, the navy flak vest with a steel zipper, flapped chest pockets and a pouch, the navy obi sash with a steel
buckle, the pale steel pauldrons over navy straps, the steel bracers, the white-wrapped shins, the dark boots with white cuffs and the white ring crest on the back. Hair: `anime_bangs_long` (long
parted bangs, forehead and eyes visible), blue-black. Original art from memory of the canon look. Glow: the Byakugan eye and the crest ring.
"""
import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

SKY, SKY_L, SKY_D = "#4fb4ea", "#8ed6fa", "#2a82b8"
WHITE, WHITE_D = "#f4f8ff", "#c4d4e6"
NAVY, NAVY_L, NAVY_D = "#1b3a74", "#3b64aa", "#0e2250"
STEEL, STEEL_L, STEEL_D = "#8db0c6", "#c8e0ee", "#56788e"
BOOT, BOOT_D = "#1a2c4c", "#0c1628"
LAV, LAV_L = "#b4a8f0", "#eae4ff"
SKIN = ("#dbb8a8", "#f0d3c3", "#fae6da")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def sky_cloth(f, mask, y0, y1, top=1.1, bottom=0.82):
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, H.tone(SKY, top)), (0.5, SKY), (1, H.tone(SKY_D, bottom))]))
    away = f.X if f.light[0] > 0 else f.w - f.X
    f.shade(mask, 1.0 - 0.12 * H.smoothstep(0.35 * f.w, f.w, away))
    H.weave(f, mask, 0.025)


def navy_cloth(f, mask, y0, y1):
    f.fill(mask, H.lin((0, y0), (0, y1), [(0, H.tone(NAVY, 1.2)), (0.5, NAVY), (1, H.tone(NAVY_D, 1.0))]))
    away = f.X if f.light[0] > 0 else f.w - f.X
    f.shade(mask, 1.0 - 0.10 * H.smoothstep(0.35 * f.w, f.w, away))
    H.weave(f, mask, 0.04)


def wave_mark(f, cx, cy, s=1.0, color="#dfeaf4"):
    """Three small waves: the Hidden Mist plate's mark in this design."""
    for k, dy in enumerate((-0.45, 0.0, 0.45)):
        pts = [(cx - 0.8 * s + i * 0.4 * s, cy + dy * s + 0.14 * s * math.sin(i * 1.6 + k)) for i in range(5)]
        f.fill(f.m_polyline(pts, 0.1 * s), color, 0.9)


def headband(cv, nm, hair):
    """The navy headband round the head at row 2 with a steel plate in the middle of the front (second layer and base)."""
    for f in lay(cv, "head", nm):
        if f.overlay:
            f.clear()
        band = f.m_band(2.0, 3.0)
        f.fill(f.m_rect(-0.01, 1.92, f.w + 0.01, 3.08), H.tone(NAVY_D, 0.5), 0.8)
        f.fill(band, H.lin((0, 2.0), (0, 3.0), [(0, NAVY_L), (0.5, NAVY), (1, NAVY_D)]))
        H.folds(f, band, [1.2, 3.0, 5.0, 6.8][: (4 if f.w > 5 else 2)], depth=0.12, width=0.2, y0=2.0, y1=3.0)
        H.stitch(f, [(0.0, 2.15), (f.w, 2.15)], "#6a90d0", width=0.04, dash=(0.25, 0.2), shadow=0.0, a=0.7)
        H.stitch(f, [(0.0, 2.85), (f.w, 2.85)], "#6a90d0", width=0.04, dash=(0.25, 0.2), shadow=0.0, a=0.7)
        if nm == "front":
            pl = f.m_rect(2.3, 1.85, 5.7, 3.15, r=0.25)
            f.fill(f.m_rect(2.22, 1.77, 5.78, 3.23, r=0.3), H.tone(STEEL_D, 0.5), 0.9)
            f.fill(pl, H.metal_paint(1.85, 3.15, STEEL))
            H.bevel(f, pl, depth=0.3, gain=1.2, spec=0.5, spec_pow=14)
            wave_mark(f, 4.0, 2.5, 1.3, "#2a3e58")
            for x in (2.6, 5.4):
                H.stud(f, x, 2.5, 0.1, STEEL_L)
        if nm == "back":
            kn = f.m_ellipse(f.w * 0.5, 2.5, 0.8, 0.6)
            f.fill(kn, H.rad((3.8, 2.3), 1.0, [(0, NAVY_L), (1, NAVY_D)]))
            H.bevel(f, kn, depth=0.25, gain=0.9, spec=0.3)


def wraps(f, y0, y1):
    """White bandage wraps: slanted strips with a light face, a darker lower edge and a soft shadow under each, from y0 to y1."""
    w = f.w
    f.fill(f.m_band(y0, y1), H.lin((0, y0), (0, y1), [(0, WHITE), (1, WHITE_D)]))
    k = 0
    y = y0 - 0.3
    while y < y1:
        strip = f.m_poly([(0.0, y), (w, y - 0.4), (w, y + 0.5), (0.0, y + 0.9)]) * f.m_band(y0, y1)
        f.fill(strip, H.lin((0, y), (0, y + 0.9), [(0, "#ffffff"), (1, H.tone(WHITE_D, 0.92))]), 0.9)
        f.fill(f.m_polyline([(0.0, y + 0.9), (w, y + 0.5)], 0.08) * f.m_band(y0, y1), H.tone(WHITE_D, 0.7), 0.7)
        y += 0.78
        k += 1
    H.shadow_under(f, y0, 0.45, 0.2)


def head(cv, hair):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(hair, 0.9), hairline=1.7, wave=0.4, shadow=0.18)
    # the Byakugan (viewer's left): a big pale-lavender eye with no pupil, a soft ring, the temple veins
    H.eye(f, 2.2, 4.85, 2.6, 2.2, ("#7d6ed8", "#b0a4f2", "#e4dcff"), lash="#202436", sclera="#cfc8ee", tilt=0.04, flick=0.3, lash_w=0.3, lid_shadow=0.3, glow="#7a6ad8", pupil="#a79af0", hi=0.55, iris_r=1.1)
    f.fill(f.m_ring(2.2, 4.9, 0.62, 0.06), "#8878d0", 0.6)
    for sg, (x0, y0, x1, y1) in ((1, (0.9, 4.3, 0.1, 3.5)), (1, (0.85, 4.6, 0.0, 4.5)), (1, (0.95, 4.0, 0.3, 3.0)), (1, (1.0, 5.6, 0.2, 6.3))):
        f.line((x0, y0), (x1, y1), "#8a82cc", 0.07, 0.03, a=0.75)
    f.line((1.0, 4.35), (1.6, 3.9), "#8a82cc", 0.06, 0.03, a=0.6)
    # the black leather eyepatch (viewer's right): a rounded patch, a strap up to the headband and a strap round the head, stitching
    f.line((4.7, 4.0), (7.6, 3.0), "#12141e", 0.26, 0.26)
    f.line((4.3, 5.2), (3.4, 3.1), "#12141e", 0.22, 0.22)
    pm = f.m_rect(4.2, 3.75, 6.95, 6.25, r=0.95)
    f.fill(f.m_rect(4.12, 3.67, 7.03, 6.33, r=1.0), "#0a0c14", 0.75)
    f.fill(pm, H.lin((4.2, 3.75), (6.95, 6.25), [(0, "#41465c"), (0.45, "#1d202e"), (1, "#0c0e18")]))
    H.bevel(f, pm, depth=0.35, gain=1.1, spec=0.35, spec_pow=14)
    H.stitch(f, [(4.55, 4.1), (6.6, 4.1), (6.6, 5.9), (4.55, 5.9), (4.55, 4.1)], "#58607c", width=0.05, dash=(0.2, 0.15), shadow=0.0, a=0.8)
    f.ellipse(5.1, 4.3, 0.55, 0.14, "#8a92b0", a=0.4, rot=-20, soft=2.5)
    H.stud(f, 4.45, 4.0, 0.1, STEEL_L)
    H.brow(f, (3.3, 3.55), (0.8, 3.3), "#1c2036", 0.24, 0.1, arch=0.04)
    H.nose(f, 4.0, 6.2, "#c8998a", 0.2)
    H.mouth_smile(f, 4.0, 7.15, 1.7, color="#a66a68", lip="#cf9890", depth=0.0, thick=0.14)
    for nm in SIDES:
        headband(cv, nm, hair)


def collar(f, back=False):
    m = f.m_band(0.0, 2.5)
    f.fill(m, H.lin((0, 0), (0, 2.5), [(0, WHITE), (1, H.tone(WHITE_D, 0.95))]))
    H.folds(f, m, [1.3, 2.9, 5.1, 6.7] if f.w > 5 else [1.3, 2.7], depth=0.08, width=0.2, y0=0.2, y1=2.5)
    if not back and f.w > 5:
        v = f.m_poly([(3.05, -0.1), (4.95, -0.1), (4.0, 1.6)])
        f.fill(v, H.lin((0, 0), (0, 1.6), [(0, SKIN[0]), (1, H.tone(SKIN[0], 0.7))]))
    H.trim_band(f, 2.15, 2.6, SKY, outline=0.5)
    H.shadow_under(f, 2.65, 0.5, 0.3)


def obi(f, back=False):
    m = f.m_band(7.0, 8.9)
    navy_cloth(f, m, 7.0, 8.9)
    H.folds(f, m, [1.0, 2.6, 5.4, 7.0] if f.w > 5 else [1.0, 3.0], depth=0.14, width=0.2, y0=7.0, y1=8.9)
    H.trim_band(f, 7.0, 7.25, NAVY_L, outline=0.0)
    H.shadow_under(f, 8.9, 0.55, 0.3)
    if not back and f.w > 5:
        bk = f.m_rect(3.2, 7.2, 4.8, 8.7, r=0.25)
        f.fill(f.m_rect(3.12, 7.12, 4.88, 8.78, r=0.3), H.tone(STEEL_D, 0.5), 0.9)
        f.fill(bk, H.metal_paint(7.2, 8.7, STEEL))
        H.bevel(f, bk, depth=0.28, gain=1.2, spec=0.6, spec_pow=12)
        f.rect(3.6, 7.6, 4.4, 8.3, H.tone(NAVY_D, 0.8), r=0.12)
        wave_mark(f, 4.0, 7.95, 0.75, STEEL_L)
    if back and f.w > 5:
        for sg in (-1, 1):                                                       # the knot's two tails
            f.poly([(4.0 + sg * 0.2, 8.3), (4.0 + sg * 1.4, 10.6), (4.0 + sg * 0.6, 10.8), (4.0, 9.0)], H.lin((0, 8.3), (0, 10.8), [(0, NAVY_L), (1, NAVY_D)]))
        kn = f.m_ellipse(4.0, 8.0, 0.8, 0.7)
        f.fill(kn, H.rad((3.8, 7.8), 1.1, [(0, NAVY_L), (1, NAVY_D)]))
        H.bevel(f, kn, depth=0.25, gain=0.9, spec=0.3)


def body_front(f):
    # navy flak vest in the middle (base), the sky-blue open coat on both sides
    v = f.m_band(0.0, 7.2)
    navy_cloth(f, v, 0.0, 7.2)
    # steel zipper with teeth
    f.fill(f.m_rect(3.8, 0.0, 4.2, 7.2), H.lin((0, 0), (0, 7.2), [(0, STEEL_L), (1, STEEL_D)]))
    for y in np.arange(0.4, 7.0, 0.35):
        f.line((3.82, float(y)), (4.18, float(y)), STEEL_D, 0.08, 0.08, a=0.9)
    H.stud(f, 4.0, 2.7, 0.2, STEEL_L)
    for sg in (-1, 1):
        cx = 4.0 + sg * 1.55
        pk = f.m_rect(cx - 0.75, 3.6, cx + 0.75, 5.4, r=0.12)
        f.fill(f.m_rect(cx - 0.8, 3.65, cx + 0.8, 5.5, r=0.14), H.tone(NAVY_D, 0.5), 0.8)
        f.fill(pk, H.lin((0, 3.6), (0, 5.4), [(0, NAVY_L), (1, NAVY)]))
        H.bevel(f, pk, depth=0.18, gain=0.8, spec=0.0)
        flap = f.m_poly([(cx - 0.75, 3.6), (cx + 0.75, 3.6), (cx + 0.75, 4.3), (cx, 4.55), (cx - 0.75, 4.3)])
        f.fill(flap, H.lin((0, 3.6), (0, 4.5), [(0, NAVY_L), (1, NAVY)]))
        H.bevel(f, flap, depth=0.16, gain=0.9, spec=0.0)
        H.stud(f, cx, 4.2, 0.12, STEEL_L)
        H.stitch(f, [(cx - 0.6, 3.75), (cx + 0.6, 3.75)], "#6a90d0", width=0.04, dash=(0.2, 0.15), shadow=0.0, a=0.7)
    for sg in (-1, 1):                                                           # shoulder straps of the vest
        H.trim_line(f, [(4.0 + sg * 1.2, 0.0), (4.0 + sg * 1.9, 2.6)], NAVY_L, width=0.28, outline=0.4, spec=0.0)
    obi(f)
    # trousers below the obi (base) between the coat skirts
    tr = f.m_band(8.9, 12.0)
    navy_cloth(f, tr, 8.9, 12.0)
    H.folds(f, tr, [3.7, 4.3], depth=0.14, width=0.2, y0=8.9, y1=12.0)
    # the sky-blue coat on both sides: open at the front, white piping on the inner edge
    for sg in (-1, 1):
        x = (lambda v: v) if sg < 0 else (lambda v: 8.0 - v)
        panel = f.m_poly([(x(0.0), 0.0), (x(2.9), 0.0), (x(2.6), 7.0), (x(2.45), 12.0), (x(0.0), 12.0)])
        panel = panel * (1.0 - f.m_band(7.0, 8.95))
        sky_cloth(f, panel, 0.0, 12.0)
        H.folds(f, panel, [x(0.8), x(1.7)], depth=0.1, width=0.22, y0=2.0, y1=12.0)
        H.trim_line(f, [(x(2.9), 0.0), (x(2.6), 7.0), (x(2.45), 12.0)], WHITE, width=0.26, outline=0.35, spec=0.2)
        H.stitch(f, [(x(2.2), 0.5), (x(1.98), 7.0), (x(1.85), 11.5)], "#d8f0ff", width=0.05, dash=(0.28, 0.18), shadow=0.15)
    for sg in (-1, 1):                                                           # slanted hip pockets with a flap and a button
        x = (lambda v: v) if sg < 0 else (lambda v: 8.0 - v)
        H.trim_line(f, [(x(0.55), 9.5), (x(1.95), 10.0)], SKY_D, width=0.16, outline=0.0, spec=0.0, a=0.9)
        fl = f.m_poly([(x(0.5), 9.2), (x(2.0), 9.7), (x(2.0), 10.4), (x(0.5), 9.95)])
        f.fill(fl, H.lin((0, 9.2), (0, 10.4), [(0, SKY_L), (1, SKY)]))
        H.bevel(f, fl, depth=0.16, gain=0.9)
        H.stud(f, x(1.25), 10.0, 0.1, WHITE)
    obi(f)                                                                       # the obi over the coat edges
    H.trim_band(f, 11.2, 12.0, WHITE, outline=0.45)
    collar(f)


def ring_crest(f, cx, cy):
    f.fill(f.m_ring(cx, cy, 1.95, 0.55), H.tone(SKY_D, 0.55), 0.7)
    f.fill(f.m_ring(cx, cy, 1.95, 0.4), WHITE, glow="#9ad8ff")
    f.fill(f.m_ring(cx, cy, 1.95, 0.12), "#ffffff", 0.85)
    f.fill(f.m_ring(cx, cy, 1.2, 0.18), WHITE, 0.95, glow="#9ad8ff")
    wave_mark(f, cx, cy, 1.1, "#ffffff")


def body_back(f):
    m = f.m_band(0.0, 12.0)
    sky_cloth(f, m, 0.0, 12.0)
    H.folds(f, m, [0.9, 2.0, 6.0, 7.1], depth=0.12, width=0.22, y0=2.0, y1=12.0)
    H.trim_line(f, [(4.0, 3.0), (4.0, 12.0)], SKY_D, width=0.12, outline=0.0, spec=0.0, a=0.8)
    ring_crest(f, 4.0, 4.7)
    obi(f, back=True)
    H.trim_band(f, 11.2, 12.0, WHITE, outline=0.45)
    collar(f, back=True)


def body_side(f):
    m = f.m_band(0.0, 12.0)
    sky_cloth(f, m, 0.0, 12.0)
    H.folds(f, m, [1.0, 2.8], depth=0.12, width=0.22, y0=2.0, y1=12.0)
    H.stitch(f, [(f.w * 0.5, 3.0), (f.w * 0.5, 6.8)], "#d8f0ff", width=0.05, dash=(0.28, 0.18), shadow=0.15)
    obi(f)
    H.trim_band(f, 11.2, 12.0, WHITE, outline=0.45)
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
        f.fill(f.m_all(), H.rad((f.w * 0.5, f.h * 0.5), f.w * 0.8, [(0, WHITE), (1, WHITE_D)]))
    # the overlay keeps the collar, the coat panels, the obi and the coat tails; the vest and the trousers in the open middle are the base layer
    f = cv.face("body", "front", True)
    opening = f.m_poly([(2.9, 0.0), (5.1, 0.0), (5.4, 7.0), (5.55, 12.0), (2.45, 12.0), (2.6, 7.0)], soft=1.0)
    f.clear(opening * (f.m_band(2.6, 6.95) + f.m_band(8.95, 12.0)))


def sleeve(f, nm):
    w = f.w
    m = f.m_band(3.4, 7.3)
    sky_cloth(f, m, 3.4, 7.3, 1.05, 0.85)
    H.folds(f, m, [1.0, 2.9], depth=0.14, width=0.22, y0=3.6, y1=7.3)
    f.fill(f.m_band(7.3, 8.3), H.lin((0, 7.3), (0, 8.3), [(0, NAVY), (1, NAVY_D)]))
    H.trim_band(f, 6.9, 7.5, WHITE, outline=0.5)
    # navy strap under the pauldron
    H.trim_band(f, 3.1, 3.9, NAVY, outline=0.5, spec=0.2)
    H.stud(f, w * 0.5, 3.5, 0.12, STEEL_L)
    # the pale steel pauldron: two plates
    pm2 = f.m_rect(-0.3, 1.3, w + 0.3, 3.2, r=0.55)
    f.fill(f.m_rect(-0.3, 1.34, w + 0.3, 3.32, r=0.55), H.tone(STEEL_D, 0.5), 0.7)
    f.fill(pm2, H.metal_paint(1.3, 3.2, STEEL_L, 0.8))
    H.bevel(f, pm2, depth=0.35, gain=1.1, spec=0.3, spec_pow=22)
    pm1 = f.m_rect(-0.3, 0.0, w + 0.3, 2.2, r=0.5)
    f.fill(f.m_rect(-0.3, 0.0, w + 0.3, 2.34, r=0.5), H.tone(STEEL_D, 0.5), 0.7)
    f.fill(pm1, H.metal_paint(0.0, 2.2, STEEL_L, 0.8))
    H.bevel(f, pm1, depth=0.35, gain=1.1, spec=0.3, spec_pow=22)
    for x in (0.6, w - 0.6):
        H.stud(f, x, 1.0, 0.12, STEEL_D)
        H.stud(f, x, 2.5, 0.11, STEEL_D)
    # steel bracer with navy straps
    br = f.m_band(8.0, 11.2)
    f.fill(br, H.metal_paint(8.0, 11.2, STEEL, 0.8))
    H.bevel(f, br, depth=0.28, gain=1.1, spec=0.3, spec_pow=22)
    for y in (8.7, 10.5):
        H.trim_band(f, y - 0.2, y + 0.2, NAVY, outline=0.3, spec=0.0)
    H.trim_line(f, [(0.0, 9.6), (w, 9.6)], STEEL_D, width=0.08, outline=0.0, spec=0.0, a=0.8)
    # white wrapping above the hand
    H.trim_band(f, 11.2, 11.6, WHITE, outline=0.4)


def arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            base, ov = lay(cv, part, nm)
            base.fill(base.m_all(), H.lin((0, 8), (0, 12), [(0, SKIN[1]), (1, SKIN[0])]))
            for f in (base, ov):
                if f.overlay:
                    f.clear()
                sleeve(f, nm)
            for x in (1.0, 2.0, 3.0):
                base.line((x, 11.65), (x, 12.0), SKIN[0], 0.05, 0.05, a=0.6)
        for f in lay(cv, part, "top"):
            f.fill(f.m_all(), H.rad((f.w * 0.45, f.h * 0.45), f.w, [(0, STEEL_L), (1, STEEL)]))
            H.stud(f, f.w / 2.0, f.h / 2.0, 0.3, STEEL_D)
        for f in lay(cv, part, "bottom"):
            f.fill(f.m_all(), SKIN[0])


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        w = base.w
        for f in (base, ov):
            if f.overlay:
                f.clear()
        # base: navy trousers, white shin wraps, the steel ankle guard, dark boots with a white cuff
        tr = base.m_band(0.0, 8.1)
        navy_cloth(base, tr, 0.0, 8.1)
        H.folds(base, tr, [1.0, 2.9], depth=0.14, width=0.2, y0=0.0, y1=8.1)
        wraps(base, 7.9, 10.3)
        H.trim_band(base, 10.2, 10.7, STEEL, outline=0.5)
        boot = base.m_band(10.6, 12.0)
        base.fill(boot, H.lin((0, 10.6), (0, 12.0), [(0, BOOT), (1, BOOT_D)]))
        H.sheen(base, boot, [1.1], width=0.2, strength=0.25, y0=10.8, y1=11.6)
        base.fill(base.m_band(11.65, 12.0), "#05090f", 0.9)
        # overlay: the knee-length coat skirt with a white hem
        sk = ov.m_band(0.0, 6.6)
        sky_cloth(ov, sk, 0.0, 6.6, 1.05, 0.8)
        H.folds(ov, sk, [1.0, 2.9], depth=0.14, width=0.22, y0=0.0, y1=6.6)
        H.trim_band(ov, 6.0, 6.7, WHITE, outline=0.45)
        H.shadow_under(ov, 6.7, 0.5, 0.2)
        wraps(ov, 7.9, 10.3)
        H.trim_band(ov, 10.2, 10.7, STEEL, outline=0.5)
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), "#05090f")
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), SKY_D)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    hair = "anime_bangs_long"
    hrgb = (30, 36, 76)                                                            # blue-black: the kit tint renders the remake's #2a2f58 as a bright navy
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.family(STEEL, 14, 0.4)], "skin": [H.near("#f0d3c3", 0.12)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arms(cv)
    for part in ("leg_r", "leg_l"):
        leg(cv, part)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

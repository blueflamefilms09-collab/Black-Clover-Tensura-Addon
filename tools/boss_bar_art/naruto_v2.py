"""The second Naruto bar art ("naruto_leaf", docs/bosses/kakashi.md): a clean ninja-scroll frame with a leaf-village cap, a tight palette (ink navy, slate steel, ice highlight, paper cream for
the scroll rolls, Sharingan red only for the tomoe) and one light direction (top left: bright top and left edges, shadow at the bottom and right). The old Naruto bar ("naruto") is kept as it
was; this is an appended style the client picks while the setting bossbar.narutoLeafBar is on.

It replaces only the BASE of the generator (the frame shapes); the layout numbers (window, socket, plate) are the shared ones of lib.py, so the Java painter is unchanged. The three tomoe that
mark the 75 / 50 / 25 percent phases are NOT drawn here: the painter blits the 'tomoe' emblem on the mounting plates this file leaves at the bottom of the bezel."""
import math

from lib import Canvas, W, H, WIN, SOCK, PLATE, mix, shade, gem

BX0, BX1, BY0, BY1 = 34, 248, 22, 38           # same bezel box as motifs.py
MARKERS = (25, 50, 75)                          # Kakashi's phase notches, percent of health

INK = (7, 11, 28)
NAVY_D = (8, 14, 36)
NAVY = (20, 32, 70)
NAVY_L = (34, 52, 108)
STEEL_D = (38, 56, 108)
STEEL_M = (74, 100, 164)
STEEL_L = (136, 164, 218)
ICE = (216, 234, 255)
RED = (226, 38, 44)
PAPER_L = (238, 229, 200)
PAPER_D = (160, 146, 112)
WHITE = (255, 255, 255)


def marker_x(percent):
    """Same rounding as BarLayoutMath.markerX plus the 1 px the notch sits right of the window position."""
    return WIN[0] + int(round(percent * WIN[2] / 100.0 + 1e-6)) + 1


def _roll(f, cx, y0, y1):
    """One rolled end of the scroll: a paper cylinder lit from the left, a red cord, spiral ends."""
    hw = 2.8
    n = 5
    for i in range(n):
        t = i / (n - 1)
        col = mix(PAPER_L, PAPER_D, t ** 1.15)
        f.rect(cx - hw + i * (2 * hw / n), y0, cx - hw + (i + 1) * (2 * hw / n) + 0.05, y1, col)
    f.line([(cx - hw, y0), (cx + hw, y0), (cx + hw, y1), (cx - hw, y1), (cx - hw, y0)], INK, 0.6)
    for ey, up in ((y0, 1), (y1, -1)):
        f.ell(cx, ey, hw, 1.1, mix(PAPER_L, PAPER_D, 0.35), INK, 0.5)
        f.ell(cx, ey, hw * 0.55, 0.55, mix(PAPER_L, PAPER_D, 0.7), None)
    mid = (y0 + y1) / 2
    f.rect(cx - hw - 0.2, mid - 0.9, cx + hw + 0.2, mid + 0.9, RED)
    f.line([(cx - hw - 0.2, mid - 0.9), (cx + hw + 0.2, mid - 0.9)], mix(RED, WHITE, 0.45), 0.25)
    f.line([(cx - hw - 0.2, mid + 0.9), (cx + hw + 0.2, mid + 0.9)], shade(RED, 0.45), 0.3)


def scroll_base(x):
    b, f, g = x.back, x.front, x.glow
    wx, wy, ww, wh = WIN
    cx, cy, r = SOCK
    px, py, pw, ph = PLATE

    # ---- BACK: dark track, scroll banner, socket disc
    b.grad_rect(wx - 1, wy - 1, wx + ww + 1, wy + wh + 1, (5, 9, 24), (18, 30, 68))
    b.rect(wx, wy, wx + ww, wy + 1.2, (2, 4, 14))
    ban = [(px - 3, py + 1.5), (px - 1.5, py), (px + pw + 1.5, py), (px + pw + 3, py + 1.5), (px + pw + 3, py + ph - 1.5), (px + pw + 1.5, py + ph), (px - 1.5, py + ph), (px - 3, py + ph - 1.5)]
    b.grad_poly(ban, NAVY_L, NAVY_D)
    b.line([(px + 2, py + 1.6), (px + pw - 2, py + 1.6)], (140, 176, 240, 110), 0.3)
    b.line([(px + 2, py + ph - 1.5), (px + pw - 2, py + ph - 1.5)], (4, 8, 22, 200), 0.4)
    b.grad_ell(cx, cy, r + 1, r + 1, (16, 28, 64), (4, 8, 22))
    b.ell(cx, cy, r - 3, r - 3, (4, 8, 20))

    # ---- FRONT: bezel (steel, bright top, dark bottom), the window cut out
    bez = [(BX0, BY0 + 1), (BX0 + 2, BY0), (BX1 - 12, BY0), (BX1, 30), (BX1 - 12, BY1), (BX0 + 2, BY1), (BX0, BY1 - 1)]
    f.grad_poly(bez, STEEL_L, STEEL_D)
    f.line(bez + [bez[0]], INK, 0.7)
    f.line([(BX0 + 3, BY0 + 1.0), (BX1 - 14, BY0 + 1.0)], (ICE[0], ICE[1], ICE[2], 230), 0.4)
    f.line([(BX0 + 3, BY1 - 1.0), (BX1 - 14, BY1 - 1.0)], (10, 18, 46, 220), 0.4)
    f.erase_rect(wx - 1.2, wy - 1.2, wx + ww + 1.2, wy + wh + 1.2)
    f.line([(wx - 1.2, wy - 1.2), (wx + ww + 1.2, wy - 1.2), (wx + ww + 1.2, wy + wh + 1.2), (wx - 1.2, wy + wh + 1.2), (wx - 1.2, wy - 1.2)], INK, 0.7)
    f.line([(wx - 0.6, wy + wh + 0.7), (wx + ww + 0.6, wy + wh + 0.7)], mix(STEEL_L, ICE, 0.5), 0.3)
    f.line([(wx - 0.6, wy - 0.7), (wx + ww + 0.6, wy - 0.7)], (4, 8, 22), 0.5)
    for p in MARKERS:                                                        # seams of the bezel at the phase notches
        mx = marker_x(p)
        f.line([(mx, BY0 + 0.5), (mx, wy - 1.3)], (10, 18, 46, 200), 0.35)
        f.line([(mx, wy + wh + 1.3), (mx, BY1 - 0.4)], (10, 18, 46, 200), 0.35)

    # ---- mounting plates for the three tomoe the painter blits (centre x of the notch, y 39)
    for p in MARKERS:
        mx = marker_x(p)
        f.grad_ell(mx, 39.0, 3.3, 3.3, STEEL_L, STEEL_D)
        f.d.ellipse([(mx - 3.3) * 4, (39.0 - 3.3) * 4, (mx + 3.3) * 4, (39.0 + 3.3) * 4], outline=INK + (255,), width=3)
        f.ell(mx, 39.0, 2.7, 2.7, (6, 10, 26))

    # ---- scroll: rails, the dark neck that ties the plate to the bezel, the two rolled ends
    for y0, y1 in ((py - 1.4, py + 0.7), (py + ph - 0.7, py + ph + 1.4)):
        f.grad_poly([(px - 2, y0), (px + pw + 2, y0), (px + pw + 2, y1), (px - 2, y1)], STEEL_L, STEEL_M)
        f.line([(px - 2, y0), (px + pw + 2, y0), (px + pw + 2, y1), (px - 2, y1), (px - 2, y0)], INK, 0.5)
        f.line([(px - 1.5, y0 + 0.45), (px + pw + 1.5, y0 + 0.45)], (ICE[0], ICE[1], ICE[2], 190), 0.25)
    f.poly([(px + 3, py + ph + 1.4), (px + pw - 3, py + ph + 1.4), (px + pw - 3, BY0 + 0.4), (px + 3, BY0 + 0.4)], NAVY_D, INK, 0.4)
    _roll(f, px - 1.0, 0.4, 20.6)
    _roll(f, px + pw + 1.0, 0.4, 20.6)

    # ---- socket ring and the flange to the bezel, lit from the top left; three red studs for the three tomoe
    f.poly([(cx + r - 2, cy - 7), (BX0 + 2, BY0 + 1), (BX0 + 2, BY1 - 1), (cx + r - 2, cy + 7)], STEEL_M, INK, 0.5)
    f.grad_ell(cx, cy, r + 2.6, r + 2.6, STEEL_L, STEEL_D)
    f.erase_ell(cx, cy, r, r)
    f.d.ellipse([(cx - r - 2.6) * 4, (cy - r - 2.6) * 4, (cx + r + 2.6) * 4, (cy + r + 2.6) * 4], outline=INK + (255,), width=3)
    f.d.ellipse([(cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4], outline=INK + (255,), width=3)
    f.d.arc([(cx - r - 1.2) * 4, (cy - r - 1.2) * 4, (cx + r + 1.2) * 4, (cy + r + 1.2) * 4], 170, 280, fill=ICE + (255,), width=3)
    for ang in (-90, 30, 150):
        a = math.radians(ang)
        gem(f, cx + math.cos(a) * (r + 1.3), cy + math.sin(a) * (r + 1.3), 1.9, RED, mix(RED, WHITE, 0.6), INK)

    # ---- the leaf-village swirl at the pointed cap
    sx, sy = BX1 - 10.0, 30.0
    f.grad_ell(sx, sy, 3.8, 3.8, ICE, STEEL_M)
    f.d.ellipse([(sx - 3.8) * 4, (sy - 3.8) * 4, (sx + 3.8) * 4, (sy + 3.8) * 4], outline=INK + (255,), width=3)
    pts = []
    for k in range(22):
        t = k / 21.0
        a = -2.2 + t * 2 * math.pi * 1.1
        rr = 0.5 + t * 2.4
        pts.append((sx + math.cos(a) * rr, sy + math.sin(a) * rr))
    f.line(pts, INK, 0.5)
    f.poly([(sx + 2.4, sy + 1.9), (sx + 3.4, sy + 3.4), (sx + 1.0, sy + 2.9)], INK)

    # ---- GLOW (white + alpha, the game tints it with the style's glow colour)
    g.line([(wx, wy + 0.4), (wx + ww, wy + 0.4)], (255, 255, 255, 150), 0.5)
    g.line([(wx, wy + wh - 0.4), (wx + ww, wy + wh - 0.4)], (255, 255, 255, 90), 0.5)
    g.glow(lambda t: t.d.ellipse([(cx - r) * 4, (cy - r) * 4, (cx + r) * 4, (cy + r) * 4], outline=(255, 255, 255, 255), width=3), WHITE, 1.2, 0.5)
    g.glow(lambda t: t.ell(sx, sy, 2.6, 2.6, (255, 255, 255)), WHITE, 1.6, 0.8)


BASES = {"naruto_scroll": scroll_base}

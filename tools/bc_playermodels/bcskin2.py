"""Wave 2 face marks (docs/bosses_bc_wave2.md): scars, a burn, stones, a tattoo, closed eyes, a mask. Same face coordinates as bcskin.py (x 0..8 left to right as the viewer sees it, the entity's RIGHT
eye at x ~ 2.35, the eyes at y ~ 4.95, the mouth ~ 6.9)."""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bcskin as S  # noqa: E402,F401  (path setup for hd_detail)

import hd_detail as H  # noqa: E402


def mark_scar(f, pts=((2.1, 0.9), (3.5, 2.8), (4.9, 4.5), (6.0, 6.5)), color="#a85a60", a=0.95, w=0.15):
    """A long scar (Heath: from the right of the forehead across the face to the left cheek): a pale ridge with a darker line in it and two stitch ticks."""
    f.polyline(list(pts), H.tone(color, 1.25), w * 2.2, a=a * 0.55)
    f.polyline(list(pts), color, w, a=a)
    for (x, y) in (pts[1], pts[2]):
        f.line((x - 0.35, y + 0.18), (x + 0.35, y - 0.18), color, 0.09, 0.09, a=a * 0.8)


def mark_burn(f, cx=6.3, cy=4.4, color="#8a3a3a", a=0.8):
    """A burn mark over the entity's left side of the face (Revchi): overlapping blotches, a darker rim, a pale scar tissue centre."""
    for (dx, dy, rx, ry) in ((0.0, 0.0, 1.5, 2.2), (0.6, -0.9, 1.0, 1.1), (-0.4, 1.3, 1.0, 1.0), (0.9, 1.1, 0.8, 1.2)):
        f.fill(f.m_ellipse(cx + dx, cy + dy, rx, ry, soft=2.5), color, a * 0.55)
    f.fill(f.m_ellipse(cx + 0.1, cy + 0.1, 0.9, 1.4, soft=2.5), H.tone(color, 1.5), a * 0.5)
    f.polyline([(cx - 1.0, cy - 1.6), (cx - 0.4, cy - 0.2), (cx - 1.0, cy + 1.4)], H.tone(color, 0.6), 0.1, a=0.6)


def mark_stones(f, cx=4.0, ys=(0.9, 1.7, 2.5), color="#40e0d0", glow="#80fff0", sizes=(0.28, 0.4, 0.28)):
    """Three stones in a vertical line on the forehead (Mars): diamonds lit from the top."""
    for y, r in zip(ys, sizes):
        f.poly([(cx, y - r * 1.3), (cx + r, y), (cx, y + r * 1.3), (cx - r, y)], color, a=1.0)
        f.poly([(cx, y - r * 1.3), (cx + r * 0.5, y - r * 0.3), (cx, y), (cx - r * 0.5, y - r * 0.3)], H.tone(color, 1.6), a=0.9)
        if glow:
            f.glow_fill(f.m_ellipse(cx, y, r * 0.8, r * 1.1), glow, 0.9)


def mark_tattoo(f, color="#c8283a", a=0.95, glow=None):
    """A red tattoo across the bridge of the nose and under both eyes (Vetto)."""
    f.fill(f.m_rect(0.4, 5.95, 7.6, 6.45, soft=1.2), color, a)
    f.fill(f.m_rect(3.3, 3.9, 4.7, 6.2, soft=1.2), color, a * 0.9)
    f.polyline([(0.4, 6.2), (1.6, 6.5), (2.8, 6.1)], H.tone(color, 0.6), 0.08, a=0.7)
    f.polyline([(5.2, 6.1), (6.4, 6.5), (7.6, 6.2)], H.tone(color, 0.6), 0.08, a=0.7)
    if glow:
        f.glow_fill(f.m_rect(0.6, 6.05, 7.4, 6.35, soft=1.5), glow, 0.5)


def mark_closed_eyes(f, color="#8a6a20", glow="#ffd870", y=4.95):
    """Closed, serene eyes: two downward arcs with lashes at the ends, the lids glowing gold (the God of Order)."""
    for cx in (2.35, 5.65):
        pts = [(cx - 1.25, y - 0.15), (cx - 0.6, y + 0.22), (cx, y + 0.3), (cx + 0.6, y + 0.22), (cx + 1.25, y - 0.15)]
        f.polyline(pts, color, 0.2, a=1.0)
        f.polyline([(x, yy + 0.16) for x, yy in pts[1:4]], H.tone(color, 1.5), 0.1, a=0.5)
        if glow:
            f.glow_fill(f.m_polyline(pts, 0.22), glow, 0.9)


def mark_mask_lines(f, color="#14101a", glow="#c8a0ff", a=0.92):
    """A pale mask with dark cheek lines and a nose slit lit in the devil's colour (the Dark Disciples): runes down the cheeks from the eyes."""
    for s, x in ((-1, 1.4), (1, 6.6)):
        f.polyline([(x, 5.7), (x + s * 0.1, 6.5), (x - s * 0.1, 7.5)], color, 0.15, a=a)
        f.polyline([(x + s * 0.55, 5.8), (x + s * 0.6, 6.9)], color, 0.1, a=a * 0.8)
        if glow:
            f.glow_fill(f.m_polyline([(x, 5.7), (x + s * 0.1, 6.5), (x - s * 0.1, 7.5)], 0.16), glow, 0.9)
    f.line((4.0, 5.6), (4.0, 6.5), color, 0.12, 0.08, a=a * 0.7)
    f.line((2.6, 1.9), (5.4, 1.9), color, 0.1, 0.1, a=a * 0.6)


def mark_forehead_diamond(f, cx=4.0, cy=1.5, color="#e8b830", glow="#ffe080"):
    """A small gold diamond on the brow."""
    r = 0.42
    f.poly([(cx, cy - r * 1.4), (cx + r, cy), (cx, cy + r * 1.4), (cx - r, cy)], color, a=1.0)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, r * 0.7, r * 1.0), glow, 0.9)


def mark_eye_patches(f, color="#1a1018", a=0.8):
    """Dark patches round the eyes (Megicula): soft dark ellipses under and round both eyes."""
    for cx in (2.35, 5.65):
        f.fill(f.m_ellipse(cx, 5.05, 1.75, 1.45, soft=3.0), color, a)


def mark_forehead_eye(f, cx=4.0, cy=1.6, iris="#e03040", glow="#ff6070"):
    """A third eye with a slit pupil (Megicula, the Avatar)."""
    f.ellipse(cx, cy, 0.78, 0.46, "#1a1018", a=1.0)
    f.ellipse(cx, cy, 0.56, 0.34, "#f0e8f0", a=1.0)
    f.ellipse(cx, cy, 0.3, 0.3, iris, a=1.0)
    f.ellipse(cx, cy, 0.07, 0.3, "#10060a", a=1.0)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, 0.5, 0.3), glow, 0.8)

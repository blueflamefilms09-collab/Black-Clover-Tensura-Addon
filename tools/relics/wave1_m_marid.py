"""Marid's Valor (card: "Marid's Valor", rare, requires attunement): a scimitar whose blade is a marid's magical water. Two states: solid (a curved
ice-blue scimitar, drawn as the right figure) and liquid (a flowing stream, the left figure). The 3D models, built with the owner's method:
  marids_valor          the solid blade (how the sword is found and what the item model draws)
  marids_valor_liquid   the liquid blade, on the same hilt (ready for a state-variant hook; the item model does not use it yet)

Reference: the card, 736 x 952; the solid figure on the right: pommel hook top at card y 248, guard at y 342, blade from y 365 to the point at (450, 840).
Axis: y up from the pommel to the point; the blade curves toward +x; faces +z.
"""
import math

import numpy as np

import mesh_lib as ml
import wave1_models as w

# ---------------------------------------------------------------- 1 measure
TOTAL_M = 0.95                                      # a scimitar, pommel hook to point
P_TOP, P_TIP = 248.0, 840.0                         # card y of the hook's top and of the point (solid figure)
K = w.px_to_cm(TOTAL_M * 100.0, P_TIP - P_TOP + 40)  # cm per card px (the curve adds about 40 px of length to the vertical run)
AXIS_X = 589.0                                      # card x of the grip's axis (solid figure)
BLADE_K = 0.7                                       # the card draws the water blade broad: widths scaled to this share


def Y(py):
    return (py - P_TOP) * K


def X(px):
    return -(px - AXIS_X) * K                       # turned half a turn: the point is up, the curve falls to +x


GRIP_Y = (270.0, 335.0)                             # card y of the grip's ends
R_GRIP_TOP, R_GRIP_BOT = 7.0 * K, 9.0 * K           # grip radius (card width 14..18 px)
GUARD_Y, GUARD_SPAN, GUARD_T = 342.0, 100.0, 8.0    # card y of the crossguard, its span (537..637) and bar thickness in px
BOSS_R, BOSS_H = 15.0 * K, 22.0 * K                 # the ornate central plate, half width and height
ROOT_Y = (336.0, 372.0)                             # the block the blade rises from
# the solid blade: centre (card x) and full width (card px) at card y. Read off the right figure.
CENTRE_S = w.hermite([(365, 589), (475, 588), (560, 580), (625, 568), (707, 545), (785, 500), (840, 450)])
WIDTH_S = w.lerp_table([(365, 37), (475, 52), (560, 62), (625, 70), (707, 56), (785, 34), (822, 14), (840, 0)])
THICK_S = w.lerp_table([(365, 9), (560, 11), (707, 9), (800, 5), (840, 0)])


def hilt(m):
    """The silver hilt shared by both states: hook pommel, grip with rings, crossguard with terminals, the central plate and the root block."""
    g_lo, g_hi = Y(GRIP_Y[0]), Y(GRIP_Y[1])                        # the grip, from the pommel end to the guard (the card's hook is at the top, here at the bottom)
    grip = w.add(m, "grip", "silver", base=(176, 190, 180), smooth=40, uv_mode="loft")
    ml.loft(grip, "y", (0, 0, 0), [(g_lo, R_GRIP_TOP / R_GRIP_BOT * 0.9, 0.78), (g_lo + (g_hi - g_lo) * 0.5, 1.0, 0.82), (g_hi, 1.0, 0.82)], ml.circle(R_GRIP_BOT, 12))
    rings = w.add(m, "grip_rings", "gold", base=(206, 190, 130))
    for f in (0.0, 0.5, 1.0):
        ml.collar(rings, g_lo + (g_hi - g_lo - 0.6) * f, R_GRIP_BOT * 1.12, 0.6, lip=0.14, sides=12, oval=0.82)
    # the hook pommel: a spiral scroll curling off the grip's end
    hook = w.add(m, "pommel_hook", "silver", base=(186, 196, 206), smooth=45)
    cx, cy = 3.0, g_lo - 3.4
    pts = []
    for i in range(0, 25):
        th = math.radians(i * 12.0)
        r = 3.0 - 1.4 * i / 24
        pts.append((cx - r * math.cos(th), cy - r * math.sin(th), 0.0))
    pts = [(0.0, g_lo + 0.2, 0.0), (0.0, g_lo - 1.0, 0.0)] + pts
    ml.sweep(hook, pts, ml.circle(0.75, 8), scales=lambda t: (1.0 - 0.45 * t, 0.85 * (1.0 - 0.45 * t)), caps=(True, True))
    ball = w.add(m, "hook_tip", "silver", base=(210, 218, 228))
    ex, ey = pts[-1][0], pts[-1][1]
    ml.lathe(ball, "y", (ex, ey, 0), [(-0.5, 0.0), (-0.2, 0.55), (0.3, 0.6), (0.9, 0.0)], sides=8)
    # the crossguard: a bar with flared terminals
    gy = Y(GUARD_Y)
    bar = w.add(m, "guard_bar", "silver", base=(180, 190, 206), smooth=35)
    half = GUARD_SPAN / 2 * K
    ml.loft(bar, "x", (0, gy, 0), [(-half, 0.5, 0.5), (-half + 1.4, 0.9, 0.85), (-3.0, 0.8, 0.8), (0.0, 0.95, 0.9), (3.0, 0.8, 0.8), (half - 1.4, 0.9, 0.85), (half, 0.5, 0.5)],
            ml.octagon(1.5, GUARD_T * K * 1.1, 0.3))
    for sgn, nm in ((-1, "guard_tip_l"), (1, "guard_tip_r")):
        t = w.add(m, nm, "silver", base=(200, 208, 220))
        ml.lathe(t, "x", (sgn * half, gy, 0), [(0.0, 0.7), (sgn * 0.5, 0.95), (sgn * 1.3, 0.5), (sgn * 1.9, 0.0)], sides=8)
    # the ornate central plate: a shield-shaped boss with a gem and rivets
    boss = w.add(m, "guard_plate", "silver", base=(172, 184, 202), smooth=45)
    ml.loft(boss, "y", (0, gy - BOSS_H / 2, 0), [(0, 0.55, 1.0), (BOSS_H * 0.35, 1.0, 1.0), (BOSS_H * 0.7, 0.9, 1.0), (BOSS_H, 0.35, 1.0)], ml.rounded_rect(BOSS_R * 2, 1.6, 0.5, 2))
    gm = w.add(m, "plate_gem", "crystal", base=(120, 210, 255), glow=True, smooth=0)
    ml.gem(gm, (0, gy, 0.95), 0.55, long=1.2, sides=6, axis="z")
    for i, dy in enumerate((-0.45, 0.45)):
        rv = w.add(m, f"plate_rivet_{i}", "gold", base=(214, 196, 130))
        ml.rivet(rv, (0.0, gy + dy * BOSS_H * 0.9, 0.82), 0.2, (0, 0, 1), 8)
    # the root block: where the water rises from the metal
    root = w.add(m, "blade_root", "silver", base=(168, 184, 204), smooth=40)
    ml.loft(root, "y", (0, 0, 0), [Y(ROOT_Y[0]), Y(ROOT_Y[1])], ml.rounded_rect(2.6, 1.2, 0.4, 2))


def solid():
    m = ml.Mesh("marids_valor")
    hilt(m)
    # ---- the solid blade: a swept curved scimitar, closing to a point
    bl = w.add(m, "blade", "crystal", base=(120, 200, 240), smooth=45, uv_mode="loft")
    ys = np.arange(365.0, P_TIP + 0.01, 10.0)
    path = [(X(CENTRE_S(y)), Y(y), 0.0) for y in ys]
    sc = [(WIDTH_S(y) * BLADE_K * K, THICK_S(y) * K) for y in ys]
    sc[-1] = (0.0, 0.0)
    ml.sweep(bl, path, ml.bevelled(1.0, 1.0, 0.55), scales=sc)
    # ---- the spine ridge and two frost flow lines on each face
    ridge = w.add(m, "spine_ridge", "crystal", base=(200, 236, 252), wear=0.2, smooth=60)
    ml.sweep(ridge, [(X(CENTRE_S(y)) + 0.0, Y(y), THICK_S(y) * K * 0.5 + 0.02) for y in ys[1:-6]], ml.circle(0.2, 6), scales=lambda t: (1.0, 1.0) if 0.05 < t < 0.95 else (0.4, 0.4))
    for face in (1, -1):
        for k, off in enumerate((-0.28, 0.3)):
            fl = w.add(m, f"flow_{'f' if face == 1 else 'b'}{k}", "crystal", base=(190, 232, 250), wear=0.2, smooth=60)
            pts = [(X(CENTRE_S(y)) + off * WIDTH_S(y) * BLADE_K * K, Y(y), face * THICK_S(y) * K * (0.5 - 0.25 * abs(off) * 2) + face * 0.02) for y in ys[3:-8]]
            ml.sweep(fl, pts, ml.circle(0.16, 6), scales=lambda t: (1.0, 1.0) if 0.06 < t < 0.94 else (0.4, 0.4))
    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    for nm in ("blade", "spine_ridge", "flow_f0", "flow_f1", "flow_b0", "flow_b1"):
        w.paint_part(atlas, m.part(nm), _ice)
    w.paint_part(atlas, m.part("plate_gem"), lambda c: w.set_glow(c, (110, 220, 255), 1.0, core=0.5))
    ref = w.reference("881be34435f7868e9abfe4b59306f8ab.jpg", (500, 220, 700, 870), "marids_valor")
    w.finish("marids_valor", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.16, 0.5, 1.0), ref=ref)


def _ice(c):
    n = w.noise2(c.img.shape[:2], 16, 77)
    k = (0.2 + 0.6 * n)[..., None]
    c.img[..., :3] = c.img[..., :3] * (1 - 0.4 * k) + np.array([225, 248, 255], np.float32) * (0.4 * k)
    edge = np.clip(1.0 - np.abs(c.S - 0.5) * 2.0, 0, 1) ** 0.5
    w.set_glow(c, (110, 205, 255), 0.18 + 0.22 * edge.mean(), core=0.35)


# the liquid state: the left figure, a stream of water that wanders from the guard (card x 497) toward the point
CENTRE_L = w.hermite([(270, 495), (330, 488), (398, 478), (473, 457), (515, 432), (573, 474), (615, 508), (644, 538), (700, 548), (760, 520), (840, 470)])
WIDTH_L = w.lerp_table([(270, 14), (330, 24), (398, 30), (473, 34), (515, 30), (573, 28), (644, 34), (700, 36), (760, 28), (810, 14), (840, 0)])


def liquid():
    m = ml.Mesh("marids_valor_liquid")
    hilt(m)
    ys = np.arange(366.0, P_TIP + 0.01, 8.0)
    # the guard's real position is y 342; the stream starts below the root block at 366 and follows the left figure's wander (shifted to the grip axis)
    shift = AXIS_X - 497.0
    yl = lambda y: 270.0 + (y - 366.0) * (570.0 / 475.0)                     # the left figure's y for a model y (it is stretched to the same length)
    st = w.add(m, "stream", "crystal", base=(118, 204, 240), smooth=50, uv_mode="loft")
    path = [(X(AXIS_X + 0.55 * (CENTRE_L(yl(y)) - 497.0)), Y(y), 1.2 * math.sin((y - 366) / 38.0)) for y in ys]
    sc = [(WIDTH_L(yl(y)) * K * 0.9, 0.9 + 0.5 * math.sin((y - 366) / 26.0) ** 2) for y in ys]
    sc[-1] = (0.0, 0.0)
    sc[0] = (sc[0][0] * 0.6, sc[0][1] * 0.6)
    ml.sweep(st, path, ml.ellipse(0.5, 0.5, 10), scales=sc)
    for nm, sgn in (("stream_bead_l", -1), ("stream_bead_r", 1)):
        b = w.add(m, nm, "crystal", base=(190, 236, 252), smooth=60)
        pts = [(path[i][0] + sgn * sc[i][0] * 0.45, path[i][1], path[i][2] + 0.25) for i in range(2, len(path) - 4)]
        ml.sweep(b, pts, ml.circle(0.24, 6), scales=lambda t: (1.0, 1.0) if 0.06 < t < 0.94 else (0.4, 0.4))
    rng = np.random.default_rng(5)
    for i in range(14):                                                      # the spray: droplets thrown off the stream
        j = int(rng.integers(3, len(path) - 3))
        side = 1 if rng.random() < 0.5 else -1
        r = 0.25 + 0.35 * rng.random()
        d = w.add(m, f"spray_{i}", "crystal", base=(170, 226, 250), smooth=80)
        ml.lathe(d, "y", (path[j][0] + side * (sc[j][0] * 0.5 + 0.8 + 2.5 * rng.random()), path[j][1] + 1.5 * rng.random(), path[j][2]),
                 [(-r, 0.0), (-0.8 * r, 0.58 * r), (-0.2 * r, 0.98 * r), (0.5 * r, 0.8 * r), (1.5 * r, 0.34 * r), (2.5 * r, 0.0)], sides=8)
    atlas = ml.Atlas(1024)
    atlas.layout(m)
    atlas.paint(m)
    for nm in ["stream", "stream_bead_l", "stream_bead_r"] + [f"spray_{i}" for i in range(14)]:
        w.paint_part(atlas, m.part(nm), _ice)
    w.paint_part(atlas, m.part("plate_gem"), lambda c: w.set_glow(c, (110, 220, 255), 1.0, core=0.5))
    ref = w.reference("881be34435f7868e9abfe4b59306f8ab.jpg", (400, 130, 600, 900), "marids_valor_liquid")
    w.finish("marids_valor_liquid", m, atlas, real_m=TOTAL_M, glow=("FFFFFF", 0.16, 0.5, 1.0), ref=ref, icon_id="marids_valor")


def build():
    solid()
    liquid()

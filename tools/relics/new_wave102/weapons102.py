"""The 9 weapons of Wave102 (release 1.111.0) as 3D models: 5 God grade (Moonfang Zangetsu, Ruyi Power Pole, Ackerman Twin Blades, Arash's Stella Bow, Hylian Hero's Shield) and
4 Special grade (Rem's Morning Star, Explosion Wizard's Staff, Anneal Blade, Thorn Princess Stiletto). One function per weapon id, named build_<id>; built on the owner's model
method (tools/relics/mesh_lib.py, the 1.97.0 kit new197_*.py): named parts, lofts and sweeps, a painted skin and a second additive glow skin, the icon rendered from the mesh.

    python3 -B tools/relics/new_wave102/build102.py weapons [id ...]
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, ".."))
from new197_kit2 import *            # noqa: F401,F403,E402
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, catmull, sweep_blade,  # noqa: E402
                         cuboid, hammer_head, pommel, guard_cross, guard_horns, guard_ring, straight_sword, blade_curve)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish  # noqa: E402
from new197_legend import glow_parts, fuller_pair  # noqa: E402


# ================================================================ GOD
def build_moonfang_zangetsu():
    """Moonfang Zangetsu: a huge black single-edged cleaver with a moon-blue edge line, a crescent horn guard, a long bandage-wrapped hilt and a trailing chain and cloth."""
    m = new_mesh("moonfang_zangetsu")
    L0 = 34.0
    st = [(L0, -5.4, 5.4, 0.9), (L0 + 6, -5.8, 5.6, 0.95), (90, -6.2, 5.6, 0.92), (126, -6.0, 5.4, 0.86), (142, -5.0, 4.4, 0.72), (154, -2.6, 2.4, 0.5), (160, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=8.5, rise=7.0, t=1.3), grip_len=30.0, pommel_style="ring", pommel_kw=dict(r=3.2),
                         blade_material="obsidian", blade_base=(34, 34, 46), guard_material="silver", guard_base=(222, 224, 236), grip_base=(232, 228, 216), wrap_base=(196, 190, 176),
                         bevel=0.5)
    fuller_pair(m, "fuller", L0 + 12, 130.0, 2.4, base=(120, 190, 255), material="silver", w_=0.36)
    chain_pts = catmull([(0.0, 2.0), (1.5, -6.0), (-1.0, -13.0), (2.0, -21.0), (0.0, -28.0)], 5)
    tube(m, "chain", "iron", [(x, y, 0.8 * math.sin(y * 0.4)) for x, y in chain_pts], 0.45, 0.45, sides=6, base=(150, 154, 168))
    cl = P(m, "cloth", "cloth", base=(230, 232, 244), smooth=40, wear=0.3)
    ml.sweep(cl, [(0.0, 2.0, 1.2), (1.0, -8.0, 1.8), (-0.5, -18.0, 2.4), (1.5, -30.0, 2.0)], ml.rounded_rect(1.0, 1.0, 0.2, 2), scales=lambda t: (1.6 * (1 - 0.6 * t), 0.18), caps=(True, True))
    moon = P(m, "moon_gem", "crystal", base=(150, 220, 255), glow=True, smooth=40)
    ml.gem(moon, (0.0, L0 + 3.2, 1.2), 1.4, 1.2, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (140, 210, 255), 0.7, 0.9, 0.5, side="edge")
    paint_center_glow(atlas, blade, out, (120, 190, 255), 0.5, L0 + 12, 130.0, 0.8, 0.5)
    paint_gradient(atlas, blade, (24, 24, 34), (70, 80, 110), L0, 156.0, 0.35)
    paint_runes(atlas, blade, out, (150, 220, 255), L0 + 18, 118.0, 6, seed=11, strength=0.7, side=1.5, size=0.8)
    glow_parts(atlas, m, ["fuller_f", "fuller_b", "moon_gem"], (150, 215, 255), 0.95, 0.55)
    finish("moonfang_zangetsu", m, atlas, "greatsword", glow=("FFFFFF", 0.16, 0.5, 1.0))


def build_ruyi_power_pole():
    """Ruyi Power Pole: a long red lacquered pole with a gold cap at each end, rings of gold bands, rune marks that glow and a flare at each cap."""
    m = new_mesh("ruyi_power_pole")
    Y1 = 220.0
    rod(m, "pole", "wood", 0.0, Y1, 2.3, 2.3, sides=12, base=(176, 28, 36), smooth=55)
    for k, y in enumerate((30.0, 70.0, 110.0, 150.0, 190.0)):
        collar_ring(m, y, 2.9, 3.2 if k % 2 == 0 else 2.0, "gold", (232, 188, 76), name="band_%d" % k, sides=12)
    lathe_y(m, "cap_top", "gold", Y1 - 1.0, [(0.0, 2.3), (2.0, 3.3), (5.0, 3.2), (8.0, 2.3), (10.0, 0.0)], sides=12, base=(236, 192, 80), caps=(False, False))
    lathe_y(m, "cap_bottom", "gold", -10.0, [(0.0, 0.0), (2.0, 2.3), (5.0, 3.2), (8.0, 3.3), (11.0, 2.3)], sides=12, base=(236, 192, 80), caps=(False, False))
    for sgn, nm in ((1, "orb_top"), (-1, "orb_bottom")):
        orb(m, nm, "crystal", (0.0, (Y1 + 11.0) if sgn > 0 else -11.5, 0.0), 1.9, base=(255, 214, 120), glow=True)
    runes = P(m, "runes", "gold", base=(255, 226, 130), glow=True, smooth=30)
    for i in range(14):
        y = 14.0 + i * 14.0
        if any(abs(y - b) < 5.0 for b in (30.0, 70.0, 110.0, 150.0, 190.0)):
            continue
        ml.bar(runes, (2.4, y, 0.6 * (-1) ** i), (2.6, y + 4.5, 0.6 * (-1) ** i), 0.55, 0.22, up=(1, 0, 0), taper=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["runes", "orb_top", "orb_bottom"], (255, 220, 120), 0.95, 0.55)
    glow_parts(atlas, m, ["cap_top", "cap_bottom"], (255, 200, 90), 0.4, 0.4)
    finish("ruyi_power_pole", m, atlas, "staff", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_ackerman_twin_blades():
    """Ackerman Twin Blades: two slim ultrahard steel blades splayed from one trigger grip, each with a blue cord, a notch near the guard and a flared crest ring."""
    m = new_mesh("ackerman_twin_blades")
    for sx, sf, lean in ((-3.2, "_l", -4.5), (3.2, "_r", 4.5)):
        N = 14
        pts = [(sx + lean * (i / N) ** 1.4 - 2.2 * (i / N) ** 2.6 * (1 if lean > 0 else -1), 12.0 + 44.0 * i / N) for i in range(N + 1)]
        blade_curve(m, "blade" + sf, "silver", pts, lambda t: 2.0 * (1.0 - 0.8 * t ** 1.3) + 0.15, lambda t: 0.42 * (1.0 - 0.5 * t), bevel=0.5, base=(198, 208, 224), smooth=45)
        edge = P(m, "edge_line" + sf, "silver", base=(180, 230, 255), glow=True, smooth=40)
        for i in range(1, N - 1):
            x0, y0 = pts[i]
            x1, y1 = pts[i + 1]
            ml.bar(edge, (x0 - 1.7 * (1.0 - 0.8 * (i / N) ** 1.3), y0, 0.5), (x1 - 1.7 * (1.0 - 0.8 * ((i + 1) / N) ** 1.3), y1, 0.5), 0.34, 0.12, up=(0, 0, 1))
    gd = P(m, "guard", "steel", base=(70, 84, 112), smooth=40)
    ml.loft(gd, "x", (0, 11.5, 0), [(-8.0, 1.0, 1.6), (-5.0, 1.5, 2.2), (5.0, 1.5, 2.2), (8.0, 1.0, 1.6)], ml.octagon(2.0, 2.0, 0.5))
    gp = P(m, "grip", "leather", base=(54, 66, 100), smooth=55, uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(0.5, 1.7, 1.7), (4.0, 2.0, 2.0), (8.0, 1.8, 1.8), (11.5, 1.6, 1.6)], ml.circle(1.0, 10))
    cd = P(m, "cord", "cloth", base=(110, 190, 255), wear=0.3, smooth=60, glow=True)
    for i in range(5):
        ml.collar(cd, 1.4 + i * 2.0, 2.1, 0.7, lip=0.18, sides=10, caps=(False, False))
    for sx, sf in ((-5.6, "_l"), (5.6, "_r")):
        tube(m, "crest_ring" + sf, "gold", [(sx + 2.0 * math.cos(a), 11.0, 2.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 15)], 0.45, 0.45, sides=6, base=(236, 196, 90))
    tube(m, "trigger", "steel", [(0.0, 10.0, 2.0), (0.0, 7.0, 4.5), (0.0, 3.0, 4.4), (0.0, 1.0, 2.2)], 0.4, 0.4, sides=6, base=(90, 100, 128))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["edge_line_l", "edge_line_r", "cord"], (140, 220, 255), 0.9, 0.5)
    for sf in ("_l", "_r"):
        paint_glow(atlas, m.part("blade" + sf), (180, 225, 255), 0.1, 0.3)
    finish("ackerman_twin_blades", m, atlas, "dagger", glow=("FFFFFF", 0.17, 0.4, 1.0))


def build_arashs_stella_bow():
    """Arash's Stella Bow: a tall bow of pale wood and white-gold with sky-blue star inlays on the limbs, a red-gold wrapped grip, a star-white string and a flame-tipped arrow."""
    import wave23_models as wm
    from new197_ranged import finish_bow
    HALF, BELLY = 64.0, 22.0
    m = new_mesh("arashs_stella_bow")
    up = wm.limb_points(1, HALF, BELLY, n=20, y0=3.0, power=1.8, tip_curl=8.0)
    lo = wm.limb_points(-1, HALF, BELLY, n=20, y0=3.0, power=1.8, tip_curl=8.0)
    wm.stave(m, "limb_upper", "wood", up, 2.0, 0.6, 1.1, 0.5, base=(238, 224, 190))
    wm.stave(m, "limb_lower", "wood", lo, 2.0, 0.6, 1.1, 0.5, base=(238, 224, 190))
    gild = P(m, "limb_gilt", "gold", base=(236, 196, 90), glow=True, smooth=35)
    stars = P(m, "limb_stars", "crystal", base=(150, 210, 255), glow=True, smooth=25)
    V = np.array
    for pts in (up, lo):
        n = len(pts) - 1
        for z in (-0.6, 0.6):
            ml.bar(gild, V(pts[2]) + V((0, 0, z * 1.1)), V(pts[n - 2]) + V((0, 0, z * 0.9)), 0.5, 0.12, up=(0, 0, 1), taper=3.0)
        for k in range(5):
            i = 3 + 3 * k
            ml.gem(stars, V(pts[i]) + V((0.0, 0.0, 0.95)), 0.75 + 0.1 * (k % 2), 1.3, 4)
    gp = P(m, "grip", "leather", base=(170, 40, 40), smooth=50, uv_mode="loft")
    ml.loft(gp, "y", (BELLY, 0, 0), [(-6.0, 1.0, 1.0), (-4.0, 1.9, 1.9), (4.0, 1.9, 1.9), (6.0, 1.0, 1.0)], ml.circle(1.0, 10))
    orb(m, "grip_star", "crystal", (BELLY + 1.0, 0.0, 1.9), 1.5, base=(255, 214, 130), glow=True)
    wm.string_part(m, HALF, 0.2, base=(236, 242, 255))
    wm.arrow_parts(m, length=66.0, z=2.3, head="crystal", head_base=(255, 150, 70), fletch=(250, 236, 214), glow_head=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["limb_stars", "arrow_head"], (160, 215, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["limb_gilt", "grip_star"], (255, 215, 130), 0.85, 0.5)
    glow_parts(atlas, m, ["string"], (220, 232, 255), 0.7, 0.5)
    finish_bow("arashs_stella_bow", m, atlas, HALF, BELLY, flex=9.0, max_draw=24.0)


def build_hylian_hero_shield():
    """Hylian Hero's Shield: a blue kite shield with a gold border, a red upper field, a gold three-triangle crest under a bird, a central sapphire, rim studs and a strap."""
    m = new_mesh("hylian_hero_shield")
    left = [(-24.0, 38.0), (-25.0, 24.0), (-23.0, 6.0), (-17.0, -12.0), (-8.0, -30.0)]
    outline = left + [(0.0, -46.0)] + [(-x, y) for (x, y) in reversed(left)] + [(0.0, 41.0)]
    scaled = lambda k: [(x * k, y * k) for (x, y) in outline]
    extrude(m, "rim", "gold", scaled(1.07), 1.1, edge=0.97, base=(232, 190, 84), z0=-0.4)
    extrude(m, "face", "steel", outline, 1.7, edge=0.9, base=(38, 62, 160), smooth=35, z0=0.6)
    upper = [(-22.0, 34.0), (-23.0, 22.0), (-21.0, 8.0), (-12.0, 2.0), (0.0, -1.0), (12.0, 2.0), (21.0, 8.0), (23.0, 22.0), (22.0, 34.0), (0.0, 37.0)]
    extrude(m, "field", "steel", upper, 0.5, edge=0.95, base=(178, 32, 40), smooth=35, z0=2.3)
    tri = P(m, "triforce", "gold", base=(240, 200, 90), glow=True, smooth=40)
    for (cx, cy) in ((0.0, -9.0), (-7.0, -21.0), (7.0, -21.0)):
        pts = [(cx - 6.5, cy - 5.6), (cx + 6.5, cy - 5.6), (cx, cy + 5.6)]
        ex = [(x - cx, y - cy) for x, y in pts]
        ml.loft(tri, "z", (0, 0, 2.2), [(-0.45, 0.82, 0.82, cx, cy), (0.0, 1.0, 1.0, cx, cy), (0.45, 0.82, 0.82, cx, cy)], ex)
    bird = P(m, "bird", "gold", base=(240, 204, 100), glow=True, smooth=40)
    for sgn in (-1, 1):
        pts = [(sgn * (1.5 + 15.0 * (i / 8.0)), 24.0 + 7.0 * math.sin(math.pi * (i / 8.0) * 0.55) - 5.0 * (i / 8.0) ** 2, 2.9) for i in range(9)]
        ml.sweep(bird, pts, ml.lozenge(2.0, 2.0), scales=lambda t: (2.2 * (1 - 0.8 * t), 0.4), up=(0, 0, 1))
    ml.gem(bird, (0.0, 24.0, 3.0), 2.4, 1.0, 6, axis="z")
    boss = P(m, "boss", "gold", base=(236, 196, 90), smooth=45, uv_mode="loft")
    ml.lathe(boss, "z", (0.0, 11.0, 2.0), [(0.0, 5.4), (1.4, 4.8), (2.8, 2.8), (3.4, 0.0)], sides=16, caps=(True, False))
    sap = P(m, "sapphire", "crystal", base=(80, 170, 255), glow=True, smooth=35)
    ml.gem(sap, (0.0, 11.0, 5.0), 2.4, 1.0, 8, axis="z")
    stu = P(m, "studs", "gold", base=(236, 196, 90), smooth=40)
    for (x, y) in outline[:-1]:
        ml.stud(stu, (x * 0.93, y * 0.93, 2.3), 0.8, 0.8, (0, 0, 1), 6, 0.5)
    strap = P(m, "strap", "leather", base=(80, 56, 38), smooth=50, uv_mode="loft")
    ml.sweep(strap, [(-11.0, 6.0, -2.0), (-11.0, 6.0, -6.0), (11.0, 6.0, -6.0), (11.0, 6.0, -2.0)], ml.rounded_rect(1.0, 1.0, 0.3, 2), scales=lambda t: (2.4, 0.7), caps=(True, True))
    ml.sweep(strap, [(-11.0, -10.0, -2.0), (-11.0, -10.0, -6.0), (11.0, -10.0, -6.0), (11.0, -10.0, -2.0)], ml.rounded_rect(1.0, 1.0, 0.3, 2), scales=lambda t: (2.4, 0.7), caps=(True, True))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["sapphire"], (110, 190, 255), 0.95, 0.55)
    glow_parts(atlas, m, ["triforce", "bird"], (255, 215, 120), 0.85, 0.5)
    finish("hylian_hero_shield", m, atlas, "shield", glow=("FFFFFF", 0.15, 0.45, 1.0), shield=True)


# ================================================================ SPECIAL
def build_rems_morning_star():
    """Rem's Morning Star: a short iron haft with a leather grip, a heavy chain of iron links and a spiked iron ball with a pale blue gleam in its studs."""
    m = new_mesh("rems_morning_star")
    rod(m, "haft", "iron", 0.0, 62.0, 2.0, 2.0, sides=10, base=(84, 88, 104), smooth=55)
    grip(m, 3.0, 40.0, r=2.2, base=(44, 50, 78), wrap_base=(120, 150, 210), n_wraps=9, material="leather", sides=10)
    lathe_y(m, "butt", "iron", -4.0, [(0.0, 0.0), (1.5, 2.4), (4.0, 3.0)], sides=10, base=(110, 116, 136), caps=(False, True))
    collar_ring(m, 62.0, 3.2, 3.0, "iron", (120, 126, 148), name="swivel", sides=10)
    pts = catmull([(0.0, 62.0), (4.0, 70.0), (9.0, 80.0), (10.0, 90.0)], 4)
    tube(m, "chain", "iron", [(x, y, 0.0) for x, y in pts], 0.75, 0.75, sides=6, base=(120, 126, 146))
    links = P(m, "links", "iron", base=(150, 156, 176), smooth=45)
    for i, (x, y) in enumerate(pts):
        ml.collar(links, y, 1.7, 1.3, lip=0.25, sides=8, origin=(x, 0, 0))
    cx, cy = 10.0, 99.0
    orb(m, "ball", "iron", (cx, cy, 0.0), 8.0, base=(92, 96, 116), glow=False, sides=14, stacks=9, smooth=40)
    sp = P(m, "spikes", "iron", base=(168, 176, 200), glow=True, smooth=30)
    rng = np.random.default_rng(7)
    dirs = []
    for k in range(20):
        v = rng.normal(size=3)
        dirs.append(v / np.linalg.norm(v))
    dirs += [np.array(d, float) for d in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, 1), (0, 0, -1))]
    for d in dirs:
        if d[1] < -0.75:
            continue
        spike(sp, (cx + d[0] * 7.4, cy + d[1] * 7.4, d[2] * 7.4), (cx + d[0] * 12.4, cy + d[1] * 12.4, d[2] * 12.4), 1.3, 5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["spikes"], (130, 190, 255), 0.5, 0.4)
    finish("rems_morning_star", m, atlas, "club", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_explosion_wizards_staff():
    """Explosion Wizard's Staff: a crooked dark-wood staff with gold bands, a claw head that holds a large glowing crimson stone, and a dark leather grip."""
    m = new_mesh("explosion_wizards_staff")
    ctrl = [(0.0, 0.0), (2.0, 40.0), (-2.5, 90.0), (1.5, 140.0), (0.0, 180.0)]
    pts = catmull(ctrl, 6)
    tube(m, "shaft", "wood", [(x, y, 0.0) for x, y in pts], 2.0, 1.7, sides=10, base=(84, 52, 38), smooth=55)
    for k, y in enumerate((20.0, 60.0, 110.0, 160.0)):
        xs = float(np.interp(y, [p[1] for p in pts], [p[0] for p in pts]))
        p = P(m, "band_%d" % k, "gold", base=(232, 180, 72), smooth=40, uv_mode="loft")
        ml.collar(p, y, 2.9, 2.4, lip=0.3, sides=10, origin=(xs, 0, 0))
    cradle = P(m, "claws", "gold", base=(226, 172, 70), smooth=40)
    top = pts[-1]
    for k in range(5):
        a = 2 * math.pi * k / 5
        cp = [(top[0] + 2.4 * math.cos(a) * (1 + 1.5 * (i / 8.0)), top[1] + 1.0 + 18.0 * (i / 8.0) ** 1.2, 2.4 * math.sin(a) * (1 + 1.5 * (i / 8.0))) for i in range(9)]
        ml.sweep(cradle, cp, ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (1.1 * (1 - 0.7 * t) + 0.15, 1.1 * (1 - 0.7 * t) + 0.15))
    stone = P(m, "crimson_stone", "crystal", base=(255, 70, 50), glow=True, smooth=25)
    ml.gem(stone, (top[0], top[1] + 14.0, 0.0), 5.5, 1.5, 6)
    ember = P(m, "embers", "crystal", base=(255, 160, 70), glow=True, smooth=25)
    for k in range(4):
        a = 2 * math.pi * k / 4 + 0.4
        ml.gem(ember, (top[0] + 9.0 * math.cos(a), top[1] + 15.0 + 2.0 * math.sin(a * 2), 9.0 * math.sin(a)), 0.9, 1.4, 4)
    grp = P(m, "grip", "leather", base=(40, 30, 28), smooth=55, uv_mode="loft")
    ml.loft(grp, "y", (0, 0, 0), [(8.0, 2.2, 2.2), (26.0, 2.3, 2.3), (44.0, 2.2, 2.2)], ml.circle(1.0, 10))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["crimson_stone"], (255, 90, 50), 1.0, 0.6)
    glow_parts(atlas, m, ["embers"], (255, 170, 80), 0.9, 0.5)
    finish("explosion_wizards_staff", m, atlas, "staff", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_anneal_blade():
    """Anneal Blade: a straight blue-grey single-edged short sword with a plain cross guard, a brown wrapped grip and a round pommel; a faint blue line runs along the edge."""
    m = new_mesh("anneal_blade")
    L0 = 18.0
    st = [(L0, -2.9, 2.9, 0.5), (L0 + 5, -3.0, 2.8, 0.52), (50, -2.9, 2.6, 0.48), (70, -2.4, 2.2, 0.42), (80, -1.5, 1.5, 0.34), (86, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=5.6, t=1.0, h=1.0, bulb=0.9, curve=0.6), grip_len=15.0, pommel_style="orb", pommel_kw=dict(r=1.9),
                         blade_material="steel", blade_base=(150, 168, 196), guard_material="steel", guard_base=(90, 78, 62), grip_base=(96, 64, 40), wrap_base=(150, 108, 70), bevel=0.5)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (150, 210, 255), 0.45, 0.6, 0.45, side="edge")
    paint_gradient(atlas, blade, (120, 140, 175), (190, 215, 245), L0, 84.0, 0.3)
    finish("anneal_blade", m, atlas, "shortsword", glow=("FFFFFF", 0.12, 0.4, 1.0))


def build_thorn_princess_stiletto():
    """Thorn Princess Stiletto: a long, very thin black blade with a ruby edge line, a rose-bud guard, a thorn-wrapped grip and a small ruby pommel."""
    m = new_mesh("thorn_princess_stiletto")
    L0 = 14.0
    st = [(L0, -1.5, 1.5, 0.55), (L0 + 3, -1.6, 1.5, 0.6), (40, -1.4, 1.3, 0.5), (52, -0.9, 0.9, 0.38), (60, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=3.8, t=0.9, h=0.9, bulb=0.7, curve=0.8), grip_len=13.0, pommel_style="orb", pommel_kw=dict(r=1.4),
                         blade_material="obsidian", blade_base=(40, 36, 46), guard_material="silver", guard_base=(150, 40, 56), grip_base=(40, 24, 30), wrap_base=(120, 40, 56), bevel=0.55)
    rose = P(m, "rose_bud", "crystal", base=(210, 40, 70), glow=True, smooth=35)
    ml.gem(rose, (0.0, L0 + 2.6, 1.1), 1.4, 1.3, 6)
    th = P(m, "thorns", "obsidian", base=(80, 30, 40), smooth=30)
    for i in range(6):
        y = 2.5 + i * 1.8
        a = i * 2.1
        spike(th, (1.5 * math.cos(a), y, 1.5 * math.sin(a)), (2.9 * math.cos(a), y + 0.7, 2.9 * math.sin(a)), 0.4, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_edge_glow(atlas, blade, out, (255, 80, 110), 0.4, 0.8, 0.5, side="edge")
    glow_parts(atlas, m, ["rose_bud", "pommel"], (255, 90, 120), 0.9, 0.5)
    finish("thorn_princess_stiletto", m, atlas, "dagger", glow=("FFFFFF", 0.16, 0.4, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

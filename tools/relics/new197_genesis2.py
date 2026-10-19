"""Genesis grade weapons of 1.97.0, second half: the Starforged Aeon Bow, the Fairy Glitter Gauntlets, the Cardinal Halo Chakram, the Magic Emperor's Black Halberd and the Starsplitter Hammer."""
import math

import numpy as np

from new197_kit2 import *            # noqa: F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,
                         catmull, sweep_blade, cuboid, gauntlet, hammer_head, guard_leaf, spear_head, void_poly, void_strip)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish
from new197_legend import glow_parts, fuller_pair
from new197_god import helix_pts
from new197_genesis import void_ribbon


def build_starforged_aeon_bow():
    """Starforged Aeon Bow: a tall white-gold bow with star-metal crescent limbs set with glowing stars, swept fins, a halo at the grip, a comet-tail string and a star-arrow."""
    import wave23_models as wm
    from new197_ranged import finish_bow
    HALF, BELLY = 66.0, 24.0
    m = new_mesh("starforged_aeon_bow")
    up = wm.limb_points(1, HALF, BELLY, n=20, y0=3.0, power=1.9, tip_curl=7.0)
    lo = wm.limb_points(-1, HALF, BELLY, n=20, y0=3.0, power=1.9, tip_curl=7.0)
    wm.stave(m, "limb_upper", "gold", up, 2.0, 0.6, 1.1, 0.5, base=(244, 236, 214))
    wm.stave(m, "limb_lower", "gold", lo, 2.0, 0.6, 1.1, 0.5, base=(244, 236, 214))
    gild = P(m, "limb_gilt", "gold", base=(236, 192, 80), glow=True, smooth=35)
    stars = P(m, "limb_stars", "crystal", base=(190, 230, 255), glow=True, smooth=25)
    fins = P(m, "limb_fins", "silver", base=(226, 232, 250), smooth=25, wear=0.3)
    V = np.array
    for pts, sgn in ((up, 1), (lo, -1)):
        n = len(pts) - 1
        for z in (-0.6, 0.6):
            ml.bar(gild, V(pts[2]) + V((0, 0, z * 1.1)), V(pts[n - 2]) + V((0, 0, z * 0.9)), 0.55, 0.12, up=(0, 0, 1), taper=3.0)
        for k in range(6):
            i = 3 + 3 * k
            ml.gem(stars, V(pts[i]) + V((0.0, 0.0, 0.9)), 0.8 + 0.12 * (k % 2), 1.3, 4)
        for k in range(5):                                            # swept fins on the belly side
            i = 4 + 3 * k
            root = V(pts[i])
            d = V((0.5, sgn * 0.86, 0.0))
            ml.bar(fins, root, root + d * (8.0 + 2.0 * math.sin(k)), 2.2, 0.14, up=(0, 0, 1), taper=4.0, end_scale=0.1)
    gp = P(m, "grip", "gold", base=(120, 100, 150), smooth=50, uv_mode="loft")
    ml.loft(gp, "y", (BELLY, 0, 0), [(-6.5, 1.0, 1.0), (-4.5, 1.9, 1.9), (4.5, 1.9, 1.9), (6.5, 1.0, 1.0)], ml.circle(1.0, 10))
    tube(m, "grip_halo", "gold", [(BELLY + 1.0 + 6.0 * math.cos(a), 6.0 * math.sin(a), 2.4 * math.sin(2 * a) * 0.0) for a in np.linspace(0, 2 * math.pi, 25)], 0.45, 0.45, sides=6, base=(255, 230, 150), glow=True)
    orb(m, "grip_star", "crystal", (BELLY + 1.0, 0.0, 1.9), 1.7, base=(200, 235, 255), glow=True)
    wm.string_part(m, HALF, 0.2, base=(230, 240, 255))
    wm.arrow_parts(m, length=70.0, z=2.3, head="crystal", head_base=(200, 235, 255), fletch=(220, 230, 255), glow_head=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["limb_stars", "grip_star", "arrow_head"], (190, 230, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["limb_gilt", "grip_halo"], (255, 225, 140), 0.85, 0.5)
    glow_parts(atlas, m, ["string"], (200, 225, 255), 0.7, 0.5)
    glow_parts(atlas, m, ["limb_upper", "limb_lower"], (230, 235, 255), 0.18, 0.4)
    finish_bow("starforged_aeon_bow", m, atlas, HALF, BELLY, flex=9.0, max_draw=26.0)


def build_fairy_glitter_gauntlets():
    """Fairy Glitter Gauntlets: white-and-gold gauntlets with wide wing flares, star gems on the backs of the hands and a cloud of fairy glitter hanging round them."""
    m = new_mesh("fairy_glitter_gauntlets")
    for sx, sf in ((-12.0, "_l"), (12.0, "_r")):
        gauntlet(m, sx, sf, (240, 236, 250), (255, 214, 120), glow_rgb=(255, 232, 160), fist_h=15.0, wings=True, spikes=0, flare=1.12, mat="silver")
        sg = P(m, "star_gem" + sf, "crystal", base=(255, 236, 170), glow=True, smooth=25)
        ml.gem(sg, (sx, 35.0, 5.3), 2.0, 1.1, 8, axis="z")
    gl = P(m, "glitter", "crystal", base=(255, 240, 190), glow=True, smooth=20)
    rng = np.random.default_rng(97)
    for k in range(18):
        x = float(rng.uniform(-24.0, 24.0))
        y = float(rng.uniform(8.0, 58.0))
        z = float(rng.uniform(-9.0, 9.0))
        ml.gem(gl, (x, y, z), 0.55 + 0.3 * float(rng.random()), 1.5, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["star_gem_l", "star_gem_r", "glitter"], (255, 236, 170), 1.0, 0.6)
    glow_parts(atlas, m, ["cuff_l", "cuff_r", "knuckles_l", "knuckles_r", "wing_flares_l", "wing_flares_r"], (255, 220, 140), 0.45, 0.4)
    finish("fairy_glitter_gauntlets", m, atlas, "dagger", glow=("FFFFFF", 0.18, 0.5, 1.0))


def build_cardinal_halo_chakram():
    """Cardinal Halo Chakram: a golden ring blade set with sea-glass studs, a bright cyan inner ring, four spokes to a gem hub and a crossbar grip; a second halo hangs round it."""
    m = new_mesh("cardinal_halo_chakram")
    R, CY = 33.0, 52.0
    ring_blade(m, "ring", "gold", 0.0, CY, R, 3.6, 0.8, n=36, bevel=0.55, base=(238, 200, 100))
    ring_blade(m, "inner_ring", "crystal", 0.0, CY, R - 6.0, 0.9, 0.9, n=36, bevel=0.3, base=(120, 230, 255), glow=True)
    ring_blade(m, "outer_halo", "gold", 0.0, CY, R + 7.0, 0.7, 0.5, n=36, bevel=0.3, base=(255, 232, 150), glow=True)
    gl = P(m, "sea_glass", "crystal", base=(80, 210, 240), glow=True, smooth=30)
    for k in range(12):
        a = 2 * math.pi * k / 12
        ml.gem(gl, ((R + 0.0) * math.cos(a), CY + (R + 0.0) * math.sin(a), 0.9), 1.4, 1.3, 6, axis="z")
    spokes = P(m, "spokes", "gold", base=(232, 190, 90), smooth=45)
    for k in range(4):
        a = math.pi / 2 * k + math.pi / 4
        ml.bar(spokes, (3.0 * math.cos(a), CY + 3.0 * math.sin(a), 0.0), ((R - 6.0) * math.cos(a), CY + (R - 6.0) * math.sin(a), 0.0), 1.6, 0.6, up=(0, 0, 1), taper=1.0, section="rect")
    hub = P(m, "hub", "crystal", base=(120, 230, 255), glow=True, smooth=30)
    ml.gem(hub, (0.0, CY, 0.6), 3.4, 1.0, 8, axis="z")
    cross = P(m, "crossbar", "gold", base=(214, 174, 84), smooth=50, uv_mode="loft")
    ml.loft(cross, "y", (0, 0, 0), [(-2.0, 1.4, 1.4), (6.0, 1.6, 1.6), (CY - R + 2.0, 1.5, 1.5)], ml.circle(1.0, 9))
    wrap(m, "grip_wraps", 0.0, CY - R - 4.0, 1.6, 6, "leather", (240, 240, 250))
    pom = P(m, "pommel", "gold", base=(240, 204, 100), smooth=50, uv_mode="loft")
    ml.lathe(pom, "y", (0, 0, 0), [(-3.0, 0.0), (-1.5, 1.9), (0.5, 2.0), (1.2, 1.4)], sides=10)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_glow(atlas, m.part("ring"), (255, 226, 140), 0.25, 0.4)
    glow_parts(atlas, m, ["inner_ring", "sea_glass", "hub"], (120, 235, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["outer_halo"], (255, 232, 150), 0.9, 0.5)
    finish("cardinal_halo_chakram", m, atlas, "handaxe", glow=("FFFFFF", 0.17, 0.5, 1.0))


def build_magic_emperors_black_halberd():
    """Magic Emperor's Black Halberd: a black pole with magenta bands, an axe blade with a window of the void, a spear tip, a back spike and a four-leaf guard with a magenta gem."""
    m = new_mesh("magic_emperors_black_halberd")
    Y_S = 190.0
    YC = Y_S - 14.0
    shaft(m, 0.0, Y_S, r=1.9, material="obsidian", base=(22, 18, 32), name="shaft", sides=10)
    for y in (10.0, 60.0, 120.0, 156.0):
        collar_ring(m, y, 2.5, 2.0, "iron", (150, 30, 90), name="band_%d" % int(y))
    lathe_y(m, "butt", "iron", -6.0, [(0.0, 0.0), (2.0, 2.2), (6.0, 2.6)], sides=10, base=(40, 30, 54), caps=(False, True))
    guard_leaf(m, Y_S - 24.0, r=4.2, material="iron", base=(40, 30, 54), name="clover")
    lathe_y(m, "socket", "iron", Y_S - 4.0, [(0.0, 2.2), (3.0, 3.6), (8.0, 3.2), (12.0, 2.4)], sides=12, base=(44, 34, 60))
    out = spear_head(m, Y_S + 6.0, 46.0, 4.4, t=0.9, base=(30, 22, 40), material="obsidian", leaf=0.38, name="spear_tip", bevel=0.45)
    pts = [(-4.0, YC - 10.0)] + [(-(5.0 + 27.0 * math.cos(math.radians(a))), YC + 31.0 * math.sin(math.radians(a))) for a in np.linspace(-68, 68, 17)] + [(-4.0, YC + 10.0)]
    pts = pts[::-1]
    extrude(m, "axe_blade", "obsidian", pts, 1.7, edge=0.8, base=(26, 20, 36), smooth=38)
    inner = [(-(14.0 + (-x - 14.0) * 0.0) if False else x * 0.55 - 6.0, (y - YC) * 0.55 + YC) for (x, y) in pts]
    void_poly(m, "void_window", inner, 1.72)
    back = P(m, "back_spike", "obsidian", base=(40, 28, 58), smooth=35)
    spike(back, (2.0, YC, 0.0), (22.0, YC + 6.0, 0.0), 2.0, 6)
    gem = P(m, "clover_gem", "crystal", base=(255, 60, 140), glow=True, smooth=30)
    ml.gem(gem, (0.0, Y_S - 24.0, 4.4), 1.5, 1.2, 6, axis="z")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_edge_glow(atlas, m.part("spear_tip"), out, (255, 60, 140), 0.6, 0.9, 0.5, side="both")

    def edge(pc):
        if not pc.face:
            return
        r = np.hypot(pc.X + 5.0, (pc.Y - YC) / 1.15)
        m_ = np.clip((r - 24.0) / 3.0, 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.4 * m_[..., None]) + np.array([255, 50, 130], np.float32) * 0.4 * m_[..., None]
        put_glow(pc, m_ * 0.95, (255, 60, 140), 0.95, 0.5)
    paint(atlas, m.part("axe_blade"), edge)
    for nm in ("band_10", "band_60", "band_120", "band_156"):
        paint_glow(atlas, m.part(nm), (255, 60, 140), 0.6, 0.4)
    glow_parts(atlas, m, ["clover_gem"], (255, 70, 150), 1.0, 0.6)
    finish("magic_emperors_black_halberd", m, atlas, "glaive", glow=("FFFFFF", 0.16, 0.4, 1.0))


def build_starsplitter_hammer():
    """Starsplitter Hammer: a colossal bronze-gold head cracked across both faces by a ribbon of the void, violet bands, star gems and spiked ends on an iron haft."""
    m = new_mesh("starsplitter_hammer")
    YH = 150.0
    rod(m, "haft", "iron", 0.0, YH + 6.0, 2.5, 2.5, sides=10, base=(60, 50, 76), smooth=55)
    wrap(m, "haft_wraps", 6.0, 70.0, 2.5, 12, "leather", (110, 80, 120))
    lathe_y(m, "butt", "gold", -6.0, [(0.0, 0.0), (2.0, 2.4), (6.0, 3.4)], sides=10, base=(214, 170, 84), caps=(False, True))
    hammer_head(m, YH + 12.0, w_=64.0, h=34.0, d=34.0, material="bronze", base=(214, 172, 88), name="head")
    bands = P(m, "bands", "iron", base=(74, 54, 108), smooth=38)
    for sx in (-1, 1):
        for xo in (11.0, 25.0):
            ml.loft(bands, "x", (0, YH + 12.0, 0), [(sx * xo, 1.04 * 17.0, 1.04 * 17.0), (sx * xo + sx * 3.0, 1.04 * 17.0, 1.04 * 17.0)], ml.octagon(2.0, 2.0, 0.45))
    sock = P(m, "socket", "gold", base=(214, 170, 84), smooth=40, uv_mode="loft")
    ml.collar(sock, YH - 2.0, 4.6, 8.0, lip=1.0, sides=12)
    spikes_p = P(m, "end_spikes", "iron", base=(80, 60, 110), smooth=35)
    for sx in (-1, 1):
        spike(spikes_p, (sx * 32.0, YH + 12.0, 0.0), (sx * 44.0, YH + 12.0, 0.0), 5.0, 8)
    gems = P(m, "star_gems", "crystal", base=(255, 226, 120), glow=True, smooth=25)
    for sx in (-1, 1):
        for xo in (18.0, 6.0):
            ml.gem(gems, (sx * xo, YH + 12.0, 17.6), 1.8, 1.0, 8, axis="z")
            ml.gem(gems, (sx * xo, YH + 12.0, -17.6), 1.8, 1.0, 8, axis="-z")
    zig = [(-26.0, YH + 20.0), (-14.0, YH + 14.0), (-8.0, YH + 17.0), (2.0, YH + 8.0), (8.0, YH + 11.0), (18.0, YH + 3.0), (27.0, YH + 5.0)]
    void_ribbon(m, "void_crack", [(x, y) for (x, y) in zig], 1.6, 17.12)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)

    def starcrack(pc):
        n = w.noise2(pc.A.shape, 6, 5)
        pc.img[..., :3] = pc.img[..., :3] * (0.85 + 0.3 * n[..., None]) + np.array([30, 10, 50], np.float32) * 0.2
        put_glow(pc, np.clip(n - 0.7, 0, 1) * 0.5, (255, 220, 120), 0.6, 0.5)
    paint(atlas, m.part("head"), starcrack)
    glow_parts(atlas, m, ["star_gems"], (255, 230, 130), 1.0, 0.6)
    glow_parts(atlas, m, ["bands"], (170, 120, 255), 0.4, 0.4)
    finish("starsplitter_hammer", m, atlas, "hammer", glow=("FFFFFF", 0.16, 0.4, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

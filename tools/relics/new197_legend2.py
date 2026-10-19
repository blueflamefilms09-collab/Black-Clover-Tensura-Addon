"""Legend grade weapons of 1.97.0, second half: the bow, the whip, the gauntlets, the chakram, the axe and the scythe (see new197_legend.py for the swords and the lance)."""
import math

import numpy as np

from new197_kit2 import *            # noqa: F401,F403
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,
                         catmull, sweep_blade, cuboid, gauntlet)
from new197_lib import paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_gradient, new_mesh, new_atlas, lay_and_paint, finish
from new197_legend import glow_parts


def build_silver_eagles_galewing_bow():
    """Silver Eagle's Galewing Bow: a slender silver recurve whose belly side carries a crest of long silver feathers, tail streamers of green wind-glass, a glass gem grip."""
    import wave23_models as wm
    from new197_ranged import finish_bow
    HALF, BELLY = 62.0, 20.0
    m = new_mesh("silver_eagles_galewing_bow")
    up = wm.limb_points(1, HALF, BELLY, n=18, y0=3.0, power=2.0, tip_curl=5.0)
    lo = wm.limb_points(-1, HALF, BELLY, n=18, y0=3.0, power=2.0, tip_curl=5.0)
    wm.stave(m, "limb_upper", "silver", up, 1.5, 0.45, 0.9, 0.4, base=(208, 216, 232))
    wm.stave(m, "limb_lower", "silver", lo, 1.5, 0.45, 0.9, 0.4, base=(208, 216, 232))
    glass = P(m, "limb_glass", "crystal", base=(120, 220, 190), glow=True, smooth=40)
    fea = P(m, "limb_feathers", "silver", base=(226, 232, 244), smooth=25, wear=0.3)
    stm = P(m, "limb_streamers", "crystal", base=(130, 230, 200), glow=True, smooth=25)
    V = np.array
    for pts, sgn in ((up, 1), (lo, -1)):
        n = len(pts) - 1
        for k in range(8):                                             # the crest: long feathers fanned out on the belly side
            t = 0.12 + 0.1 * k
            i = min(n - 1, int(t * n))
            root = V(pts[i])
            ln = 14.0 + 17.0 * math.sin(math.pi * min(1.0, t * 1.25))
            d = V((0.62, sgn * 0.78, 0.0))
            d = d / np.linalg.norm(d)
            tip = root + d * ln + V((0, 0, 0.15 * ((k % 3) - 1)))
            ml.bar(fea, root, tip, 3.0 - 1.2 * t, 0.15, up=(0, 0, 1), taper=ln * 0.55, end_scale=0.1)
        for z in (-0.5, 0.5):                                         # glass inlay down the limb
            ml.bar(glass, V(pts[2]) + V((0, 0, z * 0.9)), V(pts[n - 2]) + V((0, 0, z * 0.7)), 0.5, 0.12, up=(0, 0, 1), taper=3.0)
        tipp = V(pts[n])
        for j in range(3):                                            # the streamers beyond the tip
            ml.bar(stm, tipp + V((0.4 * j, 0.0, 0.0)), tipp + V((-3.0 - 2.5 * j, sgn * (9.0 + 3.0 * j), 0.0)), 1.2, 0.1, up=(0, 0, 1), taper=4.0, end_scale=0.1)
    gp = P(m, "grip", "silver", base=(70, 120, 110), smooth=50, uv_mode="loft")
    ml.loft(gp, "y", (BELLY, 0, 0), [(-6.0, 1.0, 1.0), (-4.0, 1.8, 1.8), (4.0, 1.8, 1.8), (6.0, 1.0, 1.0)], ml.circle(1.0, 10))
    orb(m, "grip_gem", "crystal", (BELLY + 1.0, 0.0, 1.8), 1.7, base=(120, 230, 200), glow=True)
    wm.string_part(m, HALF, 0.18)
    wm.arrow_parts(m, length=66.0, z=2.2, head="silver", head_base=(210, 220, 235), fletch=(120, 210, 180))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["limb_glass", "limb_streamers", "grip_gem"], (130, 255, 210), 0.8, 0.5)
    glow_parts(atlas, m, ["limb_feathers"], (210, 245, 255), 0.18, 0.4)
    finish_bow("silver_eagles_galewing_bow", m, atlas, HALF, BELLY, flex=8.0, max_draw=24.0)


def build_starlight_whip():
    """Starlight Whip: a gold key-handle and a long lash of braided violet-silver light that waves in an S, set with star beads and ending in a star."""
    m = new_mesh("starlight_whip")
    rod(m, "handle", "gold", 0.0, 22.0, 1.7, 1.5, sides=10, base=(232, 188, 76), swell=[(0.5, 1.1)])
    wrap(m, "handle_wraps", 3.0, 19.0, 1.7, 6, "leather", (70, 50, 130))
    ring = [(5.0 * math.cos(a), -4.0 + 0.0, 5.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)]
    tube(m, "key_bow", "gold", [(0.0 + 3.6 * math.cos(a), -4.5 + 3.6 * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 17)], 0.8, 0.8, sides=6, base=(236, 192, 80))
    lathe_y(m, "guard", "gold", 21.0, [(0.0, 2.0), (1.4, 3.4), (2.2, 1.8)], sides=10, base=(236, 192, 80))
    N = 40
    lash = [(7.0 * math.sin((22.0 + 150.0 * i / N - 22.0) * 0.085) * min(1.0, i / 6.0), 24.0 + 150.0 * i / N, 0.0) for i in range(N + 1)]
    tube(m, "lash", "cloth", lash, 1.1, 0.35, sides=8, base=(150, 130, 230), glow=True, smooth=70)
    tube(m, "lash_core", "crystal", [(x, y, 0.0 + 0.0) for (x, y, _z) in lash], 0.55, 0.15, sides=6, base=(230, 220, 255), glow=True, smooth=70)
    stars = P(m, "star_beads", "crystal", base=(236, 226, 255), glow=True, smooth=30)
    for k in range(6):
        x, y, _ = lash[int((k + 1) * N / 7.5)]
        ml.gem(stars, (x, y, 0.0), 1.5, 1.4, 4)
    tip = P(m, "star_tip", "crystal", base=(255, 240, 200), glow=True, smooth=20)
    x, y, _ = lash[-1]
    ml.gem(tip, (x, y + 1.0, 0.0), 2.4, 1.6, 8)
    ml.gem(tip, (x, y + 1.0, 0.0), 2.4, 1.6, 8, axis="x")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["lash", "lash_core", "star_beads", "star_tip"], (190, 170, 255), 0.9, 0.5)
    glow_parts(atlas, m, ["star_tip"], (255, 236, 170), 1.0, 0.6)
    finish("starlight_whip", m, atlas, "whip", glow=("FFFFFF", 0.18, 0.5, 1.0))


def build_fairy_heart_gauntlets():
    """Fairy Heart Gauntlets: two broad red-and-gold gauntlets with winged cuffs, knuckle studs, finger plates and a heart gem on the back of each."""
    m = new_mesh("fairy_heart_gauntlets")
    for sx, sf in ((-11.0, "_l"), (11.0, "_r")):
        gauntlet(m, sx, sf, (196, 56, 88), (238, 206, 120), glow_rgb=(255, 150, 170), fist_h=15.0, wings=True, spikes=0)
        hg = P(m, "heart" + sf, "crystal", base=(255, 140, 170), glow=True, smooth=30)
        for dx in (-0.9, 0.9):
            w.ellipsoid(hg, (sx + dx, 38.0, 4.9), 1.6, 1.8, 0.8, sides=8, stacks=5)
        ml.gem(hg, (sx, 36.0, 5.0), 1.5, 1.2, 4, axis="-y") if False else None
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["heart_l", "heart_r"], (255, 120, 160), 1.0, 0.5)
    glow_parts(atlas, m, ["cuff_l", "cuff_r", "knuckles_l", "knuckles_r", "wing_flares_l", "wing_flares_r"], (255, 190, 120), 0.35, 0.4)
    finish("fairy_heart_gauntlets", m, atlas, "dagger", glow=("FFFFFF", 0.16, 0.45, 1.0))


def build_tidebreaker_chakram():
    """Tidebreaker Chakram: a large ring of blue steel with a rolling wave-crest edge of curled teeth, a bright inner rim, a crossbar grip and a leather-wrapped handle."""
    m = new_mesh("tidebreaker_chakram")
    R, CY = 32.0, 52.0
    ring_blade(m, "ring", "steel", 0.0, CY, R, 4.6, 0.8, n=36, bevel=0.55, base=(54, 134, 214))
    ring_blade(m, "inner_rim", "silver", 0.0, CY, R - 4.2, 0.9, 1.0, n=36, bevel=0.3, base=(226, 244, 255), glow=True)
    teeth = P(m, "wave_teeth", "steel", base=(120, 190, 240), smooth=28)
    for k in range(18):
        a = 2 * math.pi * k / 18
        bx, by = (R + 4.0) * math.cos(a), CY + (R + 4.0) * math.sin(a)
        tx, ty = -math.sin(a), math.cos(a)                              # tangent: the teeth curl like a breaking wave
        ox, oy = math.cos(a), math.sin(a)
        spike(teeth, (bx - ox * 1.2, by - oy * 1.2, 0.0), (bx + ox * 3.2 + tx * 4.4, by + oy * 3.2 + ty * 4.4, 0.0), 1.1, 4, up=(0, 0, 1))
    cross = P(m, "crossbar", "silver", base=(170, 190, 214), smooth=50, uv_mode="loft")
    ml.loft(cross, "x", (0, CY, 0), [(-R + 3.0, 1.4, 1.4), (-6.0, 1.7, 1.7), (6.0, 1.7, 1.7), (R - 3.0, 1.4, 1.4)], ml.circle(1.0, 8))
    gp = P(m, "grip", "leather", base=(40, 70, 110), smooth=55, uv_mode="loft")
    ml.loft(gp, "y", (0, 0, 0), [(-2.0, 1.4, 1.4), (6.0, 1.6, 1.6), (CY - 3.0, 1.5, 1.5)], ml.circle(1.0, 9))
    wrap(m, "grip_wraps", 0.0, CY - 8.0, 1.6, 8, "leather", (230, 245, 255))
    pom = P(m, "pommel", "silver", base=(210, 232, 250), smooth=50, uv_mode="loft")
    ml.lathe(pom, "y", (0, 0, 0), [(-3.0, 0.0), (-1.5, 1.9), (0.5, 2.0), (1.2, 1.4)], sides=10)
    gem = P(m, "wave_gem", "crystal", base=(120, 230, 255), glow=True, smooth=35)
    ml.gem(gem, (0.0, CY, 1.4), 2.2, 1.3, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    ring = m.part("ring")

    def waves(pc):
        v = pc.B / 4.5 if not pc.face else np.arctan2(pc.Y - CY, pc.X) * 6.0
        band = 0.5 + 0.5 * np.sin(v * 1.0)
        pc.img[..., :3] = pc.img[..., :3] * (0.8 + 0.4 * band[..., None]) + np.array([20, 40, 60], np.float32) * band[..., None] * 0.3
        put_glow(pc, np.clip(band - 0.55, 0, 1) * 0.9, (110, 210, 255), 0.7, 0.5)
    paint(atlas, ring, waves)
    glow_parts(atlas, m, ["inner_rim", "wave_gem"], (130, 230, 255), 0.95, 0.55)
    finish("tidebreaker_chakram", m, atlas, "handaxe", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_noon_cleaver():
    """Noon Cleaver: a great two-bladed golden axe on a dark haft: a sun disc at the heart, bright edges, gold bands and a spear tip above."""
    m = new_mesh("noon_cleaver")
    YC = 138.0
    shaft(m, 0.0, 150.0, r=1.9, material="wood", base=(70, 44, 30), name="shaft", sides=10)
    for y in (8.0, 40.0, 96.0, 124.0):
        collar_ring(m, y, 2.4, 1.8, "gold", (232, 176, 60), name="band_%d" % int(y))
    lathe_y(m, "butt", "gold", -5.0, [(0.0, 0.0), (1.8, 1.8), (5.0, 2.4)], sides=10, base=(232, 176, 60), caps=(False, True))
    for sgn, nm in ((1, "blade_r"), (-1, "blade_l")):
        out_pts = [(sgn * 4.0, YC - 7.0)] + [(sgn * (5.0 + 31.0 * math.cos(math.radians(a))), YC + 27.0 * math.sin(math.radians(a))) for a in np.linspace(-62, 62, 14)] + [(sgn * 4.0, YC + 7.0)]
        if sgn < 0:
            out_pts = out_pts[::-1]
        extrude(m, nm, "gold", out_pts, 1.5, edge=0.78, base=(238, 178, 62), glow=False, smooth=38)
    sun = P(m, "sun_disc", "gold", base=(255, 214, 96), glow=True, smooth=40, uv_mode="loft")
    ml.lathe(sun, "z", (0, YC, 0), [(-2.4, 0.0), (-2.0, 3.8), (2.0, 3.8), (2.4, 0.0)], sides=14, caps=(False, False))
    for k in range(12):
        a = 2 * math.pi * k / 12
        spike(sun, (4.0 * math.cos(a), YC + 4.0 * math.sin(a), 1.9), (6.6 * math.cos(a), YC + 6.6 * math.sin(a), 1.9), 0.55, 4)
    tipp = P(m, "spear_tip", "steel", base=(240, 220, 150), glow=True, smooth=40, uv_mode="loft")
    ml.lathe(tipp, "y", (0, 150.0, 0), [(0.0, 2.0), (3.0, 2.4), (10.0, 1.2), (16.0, 0.0)], sides=8, caps=(True, False))
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    for nm, sg in (("blade_r", 1), ("blade_l", -1)):
        part = m.part(nm)

        def edge(pc, sg=sg):
            if not pc.face:
                return
            r = np.hypot(pc.X - sg * 5.0, (pc.Y - YC) / 0.87)
            m_ = np.clip((r - 28.0) / 3.0, 0, 1)
            rad = np.clip(1.0 - np.abs(np.arctan2(pc.Y - YC, sg * (pc.X - sg * 5.0))) * 1.2, 0, 1)
            pc.img[..., :3] = pc.img[..., :3] * (1 - 0.35 * m_[..., None]) + np.array([255, 236, 150], np.float32) * 0.35 * m_[..., None]
            put_glow(pc, m_ * 0.9, (255, 214, 100), 0.9, 0.55)
            ray = np.clip(np.sin(np.arctan2(pc.Y - YC, sg * (pc.X - sg * 5.0)) * 10.0) - 0.8, 0, 1)
            darken(pc, ray, 0.25)
        paint(atlas, part, edge)
    glow_parts(atlas, m, ["sun_disc", "spear_tip"], (255, 214, 100), 0.9, 0.55)
    finish("noon_cleaver", m, atlas, "greataxe", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_calamity_scythe():
    """Calamity Scythe: a tall black pole with bone skull ferrules, a huge violet crescent blade that curls back, a soul crystal at the root and a spike above."""
    m = new_mesh("calamity_scythe")
    Y0 = 186.0
    shaft(m, 0.0, Y0, r=1.7, material="obsidian", base=(40, 28, 56), name="shaft", sides=10)
    for y in (10.0, 60.0, 120.0):
        collar_ring(m, y, 2.2, 1.8, "iron", (96, 70, 120), name="band_%d" % int(y))
    sk = P(m, "skull_ferrules", "bone", base=(230, 222, 200), smooth=45)
    for y in (4.0, Y0 - 6.0):
        w.ellipsoid(sk, (0.0, y, 0.0), 3.2, 3.0, 3.0, sides=10, stacks=7)
    ctrl = [(0.0, Y0 + 2.0), (-9.0, Y0 + 12.0), (-24.0, Y0 + 17.0), (-42.0, Y0 + 11.0), (-55.0, Y0 - 3.0), (-60.0, Y0 - 20.0)]
    blade, pts = sweep_blade(m, "blade", "obsidian", ctrl, lambda t: 8.5 * (1.0 - t) ** 0.75 + 0.6, lambda t: 1.0 * (1.0 - 0.5 * t), n=7, bevel=0.5, base=(72, 36, 112), smooth=45)
    spike_p = P(m, "top_spike", "obsidian", base=(60, 30, 96), smooth=40, uv_mode="loft")
    ml.lathe(spike_p, "y", (0, Y0, 0), [(0.0, 2.0), (4.0, 2.2), (14.0, 1.0), (22.0, 0.0)], sides=8, caps=(True, False))
    cr = P(m, "soul_crystal", "crystal", base=(190, 90, 255), glow=True, smooth=30)
    ml.gem(cr, (-2.0, Y0 + 2.0, 2.2), 2.0, 1.7, 6)
    ribs = P(m, "blade_ribs", "obsidian", base=(150, 70, 210), glow=True, smooth=40)
    for i in range(3, 19, 3):
        x, y = pts[i]
        ml.bar(ribs, (x - 1.0, y + 1.0, 0.8), (x + 1.2, y + 4.0, 0.8), 0.5, 0.14, up=(0, 0, 1), taper=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    bl = m.part("blade")

    def violet(pc):
        if not pc.face:
            return
        t = np.clip(np.hypot(pc.X, pc.Y - Y0) / 70.0, 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * 0.8 + np.array([110, 50, 170], np.float32) * (0.3 + 0.5 * t[..., None])
        put_glow(pc, np.clip((t - 0.35) * 1.4, 0, 1) * 0.6, (200, 100, 255), 0.8, 0.5)
    paint(atlas, bl, violet)
    glow_parts(atlas, m, ["soul_crystal", "blade_ribs"], (210, 110, 255), 1.0, 0.55)
    finish("calamity_scythe", m, atlas, "scythe", glow=("FFFFFF", 0.14, 0.4, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

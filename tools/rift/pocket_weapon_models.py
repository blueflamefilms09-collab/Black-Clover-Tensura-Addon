"""The 13 boss weapons of the exclusive pocket tombs (1.99.0, wave 78) as 3D models: one function per weapon id, named build_<id>, in the style and with the kit of the 35 weapons of 1.97.0
(tools/relics/new197_*.py): a mesh of named parts (centimetres, y along the weapon, the edge toward -x), a painted skin and a second additive glow skin, a 16 x 16 sprite rendered from the
finished mesh, the item models. Like the Genesis Demon-Slayer the Genesis weapon carries void parts (the dimensional void showing through the metal) and a living glow. Built by
build_pocket_weapons.py; the table is pocket_weapon_roster.py."""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "relics"))
from new197_kit2 import *            # noqa: F401,F403,E402
from new197_kit2 import (P, plate, rod, lathe_y, orb, crystal, bar, tube, wrap, studs, spike, ml, w, Outline, grip, collar_ring, shaft, extrude, arc_pts, ring_blade,   # noqa: E402
                         catmull, sweep_blade, cuboid, hammer_head, guard_cross, guard_wings, guard_disc, guard_leaf, guard_horns, guard_ring, pommel, straight_sword, spear_head,
                         void_poly, void_strip)
from new197_lib import (paint, put_glow, sm, step, darken, paint_glow, paint_edge_glow, paint_center_glow, paint_runes, paint_gradient, new_mesh, new_atlas, lay_and_paint,   # noqa: E402
                        finish)
from new197_legend import glow_parts, fuller_pair                    # noqa: E402
from new197_god import helix_pts                                     # noqa: E402


def build_thornmothers_briar_bow():
    """Thornmother's Briar Bow: a recurve of twisted dark briar wood whose limbs carry rows of thorns and a spiral of green vine, glass-green leaf feathers on the belly, a pale blossom gem for a grip."""
    import wave23_models as wm
    from new197_ranged import finish_bow
    HALF, BELLY = 60.0, 18.0
    m = new_mesh("thornmothers_briar_bow")
    up = wm.limb_points(1, HALF, BELLY, n=18, y0=3.0, power=1.9, tip_curl=4.0)
    lo = wm.limb_points(-1, HALF, BELLY, n=18, y0=3.0, power=1.9, tip_curl=4.0)
    wm.stave(m, "limb_upper", "wood", up, 1.6, 0.5, 1.0, 0.45, base=(78, 70, 44))
    wm.stave(m, "limb_lower", "wood", lo, 1.6, 0.5, 1.0, 0.45, base=(78, 70, 44))
    vine = P(m, "limb_vine", "cloth", base=(88, 170, 70), glow=True, smooth=40)
    thorns = P(m, "limb_thorns", "bone", base=(232, 226, 190), smooth=28)
    leaves = P(m, "limb_leaves", "crystal", base=(120, 220, 120), glow=True, smooth=30)
    V = np.array
    for pts, sgn in ((up, 1), (lo, -1)):
        n = len(pts) - 1
        for k in range(3, n - 1):
            r = V(pts[k])
            ph = k * 1.3
            spike(thorns, r + V((-0.6 * math.cos(ph), 0.0, 0.6 * math.sin(ph))), r + V((-3.2 * math.cos(ph), sgn * 0.8, 3.2 * math.sin(ph))), 0.55, 4)
        helix = []
        for i in range(2, n):
            c = V(pts[i])
            a = i * 0.9
            helix.append((c[0] + 1.15 * math.cos(a), c[1], c[2] + 1.15 * math.sin(a)))
        ml.sweep(vine, helix, ml.circle(0.35, 5), scales=lambda t: (1.0, 1.0))
        for k in (4, 8, 12):                                           # leaf feathers on the belly side
            root = V(pts[k])
            d = V((0.55, sgn * 0.83, 0.0))
            ml.bar(leaves, root, root + d * (11.0 - 0.3 * k), 2.4, 0.15, up=(0, 0, 1), taper=5.0, end_scale=0.1)
    gp = P(m, "grip", "wood", base=(52, 90, 48), smooth=50, uv_mode="loft")
    ml.loft(gp, "y", (BELLY, 0, 0), [(-6.0, 1.0, 1.0), (-4.0, 1.8, 1.8), (4.0, 1.8, 1.8), (6.0, 1.0, 1.0)], ml.circle(1.0, 10))
    blossom = P(m, "grip_blossom", "crystal", base=(250, 222, 236), glow=True, smooth=35)
    for k in range(5):
        a = 2 * math.pi * k / 5
        ml.gem(blossom, (BELLY + 1.0 + 1.5 * math.cos(a), 1.5 * math.sin(a) * 0.0 + 0.0, 1.9 + 1.5 * math.sin(a)), 0.9, 1.2, 4)
    orb(m, "grip_bud", "crystal", (BELLY + 1.0, 0.0, 1.9), 1.0, base=(255, 240, 120), glow=True)
    wm.string_part(m, HALF, 0.18, base=(190, 200, 150))
    wm.arrow_parts(m, length=64.0, z=2.2, head="bone", head_base=(190, 220, 140), fletch=(100, 190, 90), barbed=True)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["limb_vine", "limb_leaves"], (150, 255, 110), 0.8, 0.5)
    glow_parts(atlas, m, ["grip_blossom", "grip_bud"], (255, 230, 180), 0.9, 0.55)
    finish_bow("thornmothers_briar_bow", m, atlas, HALF, BELLY, flex=8.0, max_draw=24.0)


def build_broodqueens_stinger():
    """Brood-Queen's Stinger: a curved black chitin blade with a glowing green venom channel, a hooked and spiked chitin guard, a bulbous venom sac for a pommel."""
    m = new_mesh("broodqueens_stinger")
    L0 = 14.0
    N = 14
    pts = [(-6.5 * (i / N) ** 1.8, L0 + 38.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "obsidian", pts, lambda t: 2.6 * (1.0 - 0.75 * t ** 1.2) + 0.15, lambda t: 0.55 * (1.0 - 0.45 * t), bevel=0.55, base=(40, 52, 30), smooth=48)
    chan = P(m, "venom_channel", "crystal", base=(150, 240, 70), glow=True, smooth=40)
    for sgn in (1, -1):
        for i in range(1, N - 3):
            x0, y0 = pts[i]
            x1, y1 = pts[i + 1]
            ml.bar(chan, (x0 - 0.2, y0, sgn * 0.55), (x1 - 0.2, y1, sgn * 0.5), 0.5, 0.1, up=(0, 0, sgn), taper=0.5)
    gd = P(m, "guard", "bone", base=(54, 66, 36), smooth=38)
    for sx in (-1, 1):
        ml.bar(gd, (sx * 1.2, L0 + 0.5, 0.0), (sx * 6.5, L0 + 3.0, 0.0), 2.0, 1.4, up=(0, 0, 1), taper=3.0, end_scale=0.25)
        spike(gd, (sx * 6.0, L0 + 2.6, 0.0), (sx * 7.4, L0 + 7.5, 0.0), 0.7, 4)
        spike(gd, (sx * 3.6, L0 + 1.4, 0.0), (sx * 4.6, L0 - 3.5, 0.0), 0.6, 4)
    collar_ring(m, L0 - 1.0, 1.9, 2.0, "bone", (60, 74, 40), name="collar", sides=10)
    grip(m, 2.0, L0 - 1.6, r=1.35, base=(30, 38, 22), wrap_base=(120, 180, 70), n_wraps=6, material="leather", sides=9)
    orb(m, "venom_sac", "crystal", (0.0, 1.0, 0.0), 2.6, base=(130, 220, 60), glow=True, ry=3.0)
    for sx in (-1, 1):
        spike(gd, (sx * 1.6, 0.4, 0.0), (sx * 3.6, -2.0, 0.0), 0.5, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["venom_channel", "venom_sac"], (160, 255, 80), 1.0, 0.6)
    paint_glow(atlas, m.part("blade"), (120, 255, 60), 0.2, 0.3)
    finish("broodqueens_stinger", m, atlas, "dagger", glow=("FFFFFF", 0.16, 0.45, 1.0))


def build_anheteps_sunscourge():
    """Anhetep's Sunscourge: a heavy gold handle with a scarab pommel, and a long lash that is a chain of golden sun-discs round a glowing core, ending in a sun hook."""
    m = new_mesh("anheteps_sunscourge")
    rod(m, "handle", "gold", 0.0, 24.0, 1.9, 1.6, sides=10, base=(232, 186, 70), swell=[(0.5, 1.1)])
    wrap(m, "handle_wraps", 4.0, 21.0, 1.9, 6, "leather", (96, 50, 30))
    lathe_y(m, "guard", "gold", 23.0, [(0.0, 2.2), (1.4, 3.8), (2.4, 2.0)], sides=12, base=(240, 196, 80))
    scarab = P(m, "scarab", "crystal", base=(60, 170, 190), glow=True, smooth=40)
    w.ellipsoid(scarab, (0.0, -2.2, 0.0), 2.6, 3.2, 1.9, sides=10, stacks=7)
    for sx in (-1, 1):
        ml.bar(scarab, (sx * 2.2, -1.0, 0.0), (sx * 5.0, 1.8, 0.0), 0.9, 0.3, up=(0, 0, 1), taper=1.2)
    N = 44
    lash = [(6.5 * math.sin(i * 0.31) * min(1.0, i / 8.0) * (1 - 0.3 * i / N), 26.0 + 130.0 * i / N, 0.0) for i in range(N + 1)]
    tube(m, "chain_core", "crystal", [(x, y, 0.0) for (x, y, _z) in lash], 0.55, 0.3, sides=6, base=(255, 224, 120), glow=True, smooth=70)
    discs = P(m, "sun_discs", "gold", base=(238, 188, 70), smooth=40, uv_mode="loft")
    for i in range(2, N, 3):
        x, y, _ = lash[i]
        r = 1.9 * (1.0 - 0.35 * i / N)
        ml.lathe(discs, "y", (x, y - 0.55, 0.0), [(0.0, r * 0.55), (0.3, r), (0.8, r), (1.1, r * 0.55)], sides=12)
    rays = P(m, "sun_rays", "gold", base=(255, 214, 96), glow=True, smooth=28)
    for i in range(5, N - 2, 8):
        x, y, _ = lash[i]
        for k in range(6):
            a = 2 * math.pi * k / 6
            spike(rays, (x + 1.8 * math.cos(a), y, 1.8 * math.sin(a)), (x + 3.2 * math.cos(a), y + 0.4, 3.2 * math.sin(a)), 0.35, 4)
    x, y, _ = lash[-1]
    hook = P(m, "sun_hook", "gold", base=(255, 220, 110), glow=True, smooth=35)
    ml.sweep(hook, [(x + 4.0 * math.sin(a) * 0.0 + 3.2 * (1 - math.cos(a)), y + 2.0 + 4.0 * math.sin(a), 0.0) for a in np.linspace(0, math.pi * 1.2, 10)], ml.octagon(2.0, 2.0, 0.5),
             scales=lambda t: (0.85 * (1 - 0.7 * t) + 0.12, 0.85 * (1 - 0.7 * t) + 0.12))
    sun = P(m, "tip_sun", "crystal", base=(255, 236, 150), glow=True, smooth=22)
    ml.gem(sun, (x, y + 1.0, 0.0), 2.2, 1.5, 8)
    ml.gem(sun, (x, y + 1.0, 0.0), 2.2, 1.5, 8, axis="x")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["chain_core", "sun_rays", "sun_hook", "tip_sun"], (255, 214, 100), 0.9, 0.55)
    glow_parts(atlas, m, ["scarab"], (110, 240, 255), 0.8, 0.5)
    finish("anheteps_sunscourge", m, atlas, "whip", glow=("FFFFFF", 0.18, 0.5, 1.0))


def build_ignareths_forge_hammer():
    """Ignareth's Forge Hammer: a huge black-iron head cracked with glowing ember veins and bound with orange-lit bands, on a leather-wrapped iron haft with an anvil-shaped butt."""
    m = new_mesh("ignareths_forge_hammer")
    YH = 108.0
    rod(m, "haft", "iron", 0.0, YH + 4.0, 2.2, 2.0, sides=10, base=(60, 54, 56), smooth=55)
    wrap(m, "haft_wraps", 8.0, 56.0, 2.2, 10, "leather", (140, 70, 40))
    anv = P(m, "anvil_butt", "iron", base=(52, 46, 48), smooth=34)
    ml.box(anv, (-4.5, -7.0, -2.5), (4.5, -3.0, 2.5), 0.5)
    ml.box(anv, (-2.4, -3.0, -1.7), (2.4, 0.5, 1.7), 0.4)
    tube(m, "lanyard_ring", "gold", [(4.6 * math.cos(a), 7.0, 4.6 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)], 0.6, 0.6, sides=6, base=(210, 120, 40))
    hammer_head(m, YH + 11.0, w_=44.0, h=26.0, d=24.0, material="iron", base=(46, 40, 44), name="head")
    bands = P(m, "bands", "gold", base=(214, 110, 40), smooth=38, glow=True)
    for sx in (-1, 1):
        for off in (15.0, 6.5):
            ml.loft(bands, "x", (0, YH + 11.0, 0), [(sx * off, 1.05 * 13.0, 1.05 * 12.0), (sx * off + sx * 2.4, 1.05 * 13.0, 1.05 * 12.0)], ml.octagon(2.0, 2.0, 0.45))
    sock = P(m, "socket", "iron", base=(70, 60, 62), smooth=40, uv_mode="loft")
    ml.collar(sock, YH - 2.0, 4.0, 8.0, lip=1.0, sides=12)
    veins = P(m, "ember_veins", "crystal", base=(255, 150, 50), glow=True, smooth=40)
    zig = [(-5.0, 15.0), (-1.0, 11.0), (-3.0, 8.0), (2.0, 3.0), (0.0, 0.0), (5.0, -5.0), (3.0, -8.0)]
    for z0 in (12.4, -12.4):
        sg = 1 if z0 > 0 else -1
        for (a, b) in zip(zig[:-1], zig[1:]):
            ml.bar(veins, (a[0] * 2.4, YH + 11.0 + a[1], z0), (b[0] * 2.4, YH + 11.0 + b[1], z0), 1.1, 0.2, up=(0, 0, sg), taper=0.0)
    core = P(m, "ember_core", "crystal", base=(255, 180, 70), glow=True, smooth=30)
    for sx in (-1, 1):
        ml.gem(core, (sx * 22.4, YH + 11.0, 0.0), 2.6, 1.0, 6, axis="x")
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")

    def scorch(pc):
        n = w.noise2(pc.A.shape, 6, 5)
        pc.img[..., :3] = pc.img[..., :3] * (0.8 + 0.4 * n[..., None]) + np.array([40, 14, 6], np.float32) * 0.3
        put_glow(pc, np.clip(n - 0.68, 0, 1) * 0.7, (255, 120, 40), 0.7, 0.5)
    paint(atlas, head, scorch)
    glow_parts(atlas, m, ["ember_veins", "ember_core"], (255, 150, 60), 1.0, 0.6)
    glow_parts(atlas, m, ["bands"], (255, 110, 40), 0.5, 0.4)
    finish("ignareths_forge_hammer", m, atlas, "hammer", glow=("FFFFFF", 0.17, 0.45, 1.0))


def build_rimeheart_greatblade():
    """Rimeheart Greatblade: a broad jagged slab of pale ice crystal with a deep blue core, a heart-shaped crystal at the guard, rime spikes and a frost-wrapped grip."""
    m = new_mesh("rimeheart_greatblade")
    L0 = 38.0
    st = [(L0, -4.6, 4.6, 1.0), (L0 + 8, -5.2, 5.0, 1.05)]
    y = L0 + 16
    k = 0
    while y < 128.0:
        dent = 1.6 if k % 2 == 0 else 0.0
        st += [(y, -5.2 + dent * 0.0, 5.0, 1.0), (y + 0.6, -5.2 + dent, 5.0, 1.0)]
        y += 9.0
        k += 1
    st += [(134, -4.8, 4.6, 0.85), (144, -3.4, 3.4, 0.6), (152, -1.4, 1.4, 0.32), (156, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="horns", guard_kw=dict(span=10.0, rise=8.0, t=1.2), grip_len=32.0, pommel_style="spike", pommel_kw=dict(r=2.6),
                         blade_material="crystal", blade_base=(176, 224, 244), guard_material="silver", guard_base=(120, 180, 214), grip_base=(34, 66, 104), wrap_base=(210, 236, 255), bevel=0.45, ricasso=4.0)
    heart = P(m, "heart_crystal", "crystal", base=(80, 150, 240), glow=True, smooth=25)
    ml.gem(heart, (-1.3, L0 + 3.2, 1.6), 1.7, 1.3, 6)
    ml.gem(heart, (1.3, L0 + 3.2, 1.6), 1.7, 1.3, 6)
    ml.gem(heart, (0.0, L0 + 1.4, 1.6), 1.6, 1.6, 6)
    rime = P(m, "rime_spikes", "crystal", base=(226, 246, 255), glow=True, smooth=28)
    for i in range(7):
        yy = L0 + 16.0 + i * 15.0
        spike(rime, (5.0, yy, 0.0), (8.0 + (i % 3), yy + 6.0, 0.0), 0.9, 5)
        spike(rime, (-5.0, yy + 4.0, 0.0), (-7.4 - (i % 2), yy + 9.0, 0.0), 0.75, 5)
    core = P(m, "ice_core", "crystal", base=(60, 130, 230), glow=True, smooth=40)
    for sgn in (1, -1):
        ml.bar(core, (0.0, L0 + 10, sgn * 1.0), (0.0, 140.0, sgn * 0.6), 2.0, 0.2, up=(0, 0, sgn), taper=18.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")
    paint_gradient(atlas, blade, (90, 150, 220), (232, 248, 255), L0, 150.0, 0.55)
    paint_edge_glow(atlas, blade, out, (200, 240, 255), 0.7, 0.8, 0.5, side="both")
    paint_runes(atlas, blade, out, (190, 235, 255), L0 + 14, 130.0, 8, seed=41, size=1.1, side=2.4, strength=0.7)
    glow_parts(atlas, m, ["heart_crystal", "ice_core"], (130, 200, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["rime_spikes"], (225, 248, 255), 0.5, 0.4)
    finish("rimeheart_greatblade", m, atlas, "greatsword", glow=("FFFFFF", 0.15, 0.4, 1.0))


def build_deepwatch_harpoon():
    """Deepwatch Harpoon: a long teal shaft with silver bands and a coiled brine line, a barbed leaf head, a weighted anchor butt."""
    m = new_mesh("deepwatch_harpoon")
    Y_S = 172.0
    shaft(m, 0.0, Y_S, r=1.7, material="iron", base=(34, 100, 112), name="shaft", sides=10)
    for y in (14.0, 70.0, 124.0, Y_S - 10.0):
        collar_ring(m, y, 2.2, 1.8, "silver", (206, 232, 240), name="band_%d" % int(y))
    wrap(m, "grip_wraps", 30.0, 78.0, 1.7, 11, "leather", (40, 60, 70))
    tube(m, "brine_line", "cloth", helix_pts(2.4, 84.0, Y_S - 14.0, 9.0, 120), 0.4, 0.4, sides=5, base=(196, 232, 240), glow=True, smooth=70)
    anc = P(m, "anchor_butt", "iron", base=(60, 96, 110), smooth=40, uv_mode="loft")
    ml.lathe(anc, "y", (0, 0.0, 0), [(0.0, 0.0), (-2.0, 2.0), (-5.0, 2.6), (-7.5, 1.4), (-9.0, 0.0)], sides=10)
    for sx in (-1, 1):
        spike(anc, (sx * 2.0, -3.5, 0.0), (sx * 5.5, -9.0, 0.0), 0.8, 4)
    lathe_y(m, "socket", "silver", Y_S - 6.0, [(0.0, 2.2), (3.0, 3.2), (7.0, 3.4), (14.0, 2.0)], sides=12, base=(200, 230, 240))
    st_head = spear_head(m, Y_S + 6.0, 46.0, 4.4, t=0.9, base=(130, 220, 230), material="steel", leaf=0.3, name="head", bevel=0.5)
    barbs = P(m, "barbs", "steel", base=(170, 236, 244), smooth=30)
    for sx in (-1, 1):
        for k in range(3):
            yy = Y_S + 10.0 + k * 8.0
            spike(barbs, (sx * 3.0, yy + 6.0, 0.0), (sx * 8.4, yy - 2.0, 0.0), 0.9, 4)
    pearl = P(m, "pearl", "crystal", base=(235, 252, 255), glow=True, smooth=40)
    w.ellipsoid(pearl, (0.0, Y_S + 4.0, 3.4), 1.9, 1.9, 1.9, sides=10, stacks=7)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    head = m.part("head")
    paint_center_glow(atlas, head, st_head, (110, 240, 250), 0.45, Y_S + 10.0, Y_S + 48.0, 0.9, 0.5)
    paint_edge_glow(atlas, head, st_head, (200, 250, 255), 0.5, 0.5, 0.5, side="both")

    def wave(pc):
        v = pc.B / 5.0 if not pc.face else pc.Y / 5.0
        band = 0.5 + 0.5 * np.sin(v)
        pc.img[..., :3] = pc.img[..., :3] * (0.85 + 0.3 * band[..., None])
        put_glow(pc, np.clip(band - 0.75, 0, 1), (100, 240, 240), 0.5, 0.5)
    paint(atlas, m.part("shaft"), wave)
    glow_parts(atlas, m, ["pearl", "brine_line"], (170, 250, 255), 0.9, 0.55)
    finish("deepwatch_harpoon", m, atlas, "spear", glow=("FFFFFF", 0.14, 0.4, 1.0))


def build_sanguine_wardens_cleaver():
    """Sanguine Warden's Cleaver: a door-sized slab cleaver with a hole near the spine, a notched crimson edge, glowing blood runnels and a bone-and-iron grip."""
    m = new_mesh("sanguine_wardens_cleaver")
    L0 = 22.0
    outline = [(-9.0, L0 + 1.0), (-14.5, L0 + 8.0), (-15.5, L0 + 30.0), (-15.0, L0 + 52.0), (-12.5, L0 + 60.0), (-3.5, L0 + 62.0), (6.5, L0 + 60.0), (9.5, L0 + 52.0), (9.5, L0 + 22.0), (7.5, L0 + 8.0), (5.0, L0 + 1.0)]
    outline = outline[::-1]                                        # counter-clockwise
    extrude(m, "blade", "iron", outline, 1.2, edge=0.7, base=(86, 76, 82), smooth=38)
    notches = P(m, "notches", "iron", base=(120, 20, 24), smooth=30)
    for i in range(7):
        yy = L0 + 8.0 + i * 7.5
        spike(notches, (-14.8, yy, 0.0), (-17.2, yy + 3.6, 0.0), 0.9, 4)
    ring = P(m, "spine_hole_rim", "iron", base=(150, 30, 34), smooth=40, uv_mode="loft")
    ml.sweep(ring, [(5.2 + 3.2 * math.cos(a), L0 + 42.0 + 3.2 * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 17)], ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (0.9, 1.5))
    runs = P(m, "blood_runnels", "crystal", base=(255, 40, 50), glow=True, smooth=40)
    for sgn in (1, -1):
        for x0 in (-10.0, -3.0, 3.0):
            ml.bar(runs, (x0, L0 + 14.0, sgn * 1.25), (x0 + 0.8, L0 + 38.0 - abs(x0), sgn * 1.25), 0.9, 0.12, up=(0, 0, sgn), taper=6.0)
    collar_ring(m, L0 - 1.0, 2.1, 2.4, "iron", (70, 60, 66), name="collar", sides=10)
    grip(m, 2.0, L0 - 1.6, r=1.9, base=(214, 206, 184), wrap_base=(150, 24, 28), n_wraps=7, material="bone", sides=10, swell=1.12)
    orb(m, "pommel_skull", "bone", (0.0, 0.6, 0.0), 2.6, base=(226, 218, 196), glow=False, ry=3.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def edge(pc):
        if not pc.face:
            return
        d = np.clip((-pc.X - 10.0) / 6.0, 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * (1 - 0.5 * d[..., None]) + np.array([170, 20, 28], np.float32) * 0.5 * d[..., None]
        put_glow(pc, d * 0.4, (255, 60, 60), 0.6, 0.5)
        n = w.noise2(pc.X.shape, 5, 7)
        darken(pc, np.clip(n - 0.6, 0, 1), 0.3)
    paint(atlas, blade, edge)
    glow_parts(atlas, m, ["blood_runnels"], (255, 60, 70), 1.0, 0.6)
    finish("sanguine_wardens_cleaver", m, atlas, "cleaver", real_m=1.05, glow=("FFFFFF", 0.15, 0.4, 1.0))


def build_valkarins_stormlance():
    """Valkarin's Stormlance: a long pale lance with a conical fluted head, three swept fins, a spiral of blue lightning coil down the shaft and a storm gem at the vamplate."""
    m = new_mesh("valkarins_stormlance")
    Y_S = 178.0
    shaft(m, 0.0, Y_S, r=1.8, material="wood", base=(214, 220, 238), name="shaft", sides=10)
    tube(m, "coil", "crystal", helix_pts(2.5, 24.0, Y_S - 10.0, 16.0, 170), 0.4, 0.4, sides=5, base=(130, 190, 255), glow=True, smooth=70)
    wrap(m, "grip_wraps", 46.0, 96.0, 1.8, 11, "leather", (46, 60, 110))
    lathe_y(m, "butt", "silver", -5.0, [(0.0, 0.0), (1.6, 1.8), (5.0, 2.4)], sides=10, base=(200, 210, 232), caps=(False, True))
    vam = P(m, "vamplate", "silver", base=(210, 220, 240), smooth=42, uv_mode="loft")
    ml.lathe(vam, "y", (0, Y_S - 34.0, 0), [(0.0, 2.0), (6.0, 6.8), (12.0, 7.6), (24.0, 3.4), (34.0, 2.2)], sides=14)
    head = P(m, "head", "steel", base=(224, 232, 248), smooth=42, uv_mode="loft")
    ml.lathe(head, "y", (0, Y_S, 0), [(0.0, 2.6), (6.0, 4.0), (26.0, 3.0), (46.0, 1.4), (58.0, 0.0)], sides=12, caps=(True, False))
    fins = P(m, "fins", "silver", base=(190, 206, 244), smooth=38)
    for k in range(3):
        a = 2 * math.pi * k / 3
        ca, sa = math.cos(a), math.sin(a)
        ml.sweep(fins, [(ca * (3.0 + 5.0 * t), Y_S - 20.0 + 18.0 * t, sa * (3.0 + 5.0 * t)) for t in np.linspace(0, 1, 7)], ml.bevelled(2.0, 2.0, 0.5),
                 scales=lambda t: (0.9 * (1 - 0.8 * t) + 0.15, 1.9 * (1 - 0.7 * t) + 0.2))
    gem = P(m, "storm_gem", "crystal", base=(120, 180, 255), glow=True, smooth=30)
    ml.gem(gem, (0.0, Y_S - 24.0, 7.2), 2.0, 1.4, 6)
    flute = P(m, "flutes", "crystal", base=(150, 200, 255), glow=True, smooth=40)
    for k in range(3):
        a = 2 * math.pi * k / 3 + math.pi / 3
        ml.bar(flute, (3.4 * math.cos(a), Y_S + 6.0, 3.4 * math.sin(a)), (0.9 * math.cos(a), Y_S + 50.0, 0.9 * math.sin(a)), 0.7, 0.1, up=(math.cos(a), 0, math.sin(a)), taper=10.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["coil", "storm_gem", "flutes"], (140, 200, 255), 1.0, 0.6)
    paint_gradient(atlas, m.part("head"), (200, 214, 244), (250, 252, 255), Y_S, Y_S + 58.0, 0.5)
    finish("valkarins_stormlance", m, atlas, "spear", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_orials_starfall_staff():
    """Oriel's Starfall Staff: a dark violet shaft with silver bands and glowing star-script, a crescent cradle holding a falling-star crystal, a ring of small watching eyes."""
    m = new_mesh("orials_starfall_staff")
    Y1 = 200.0
    rod(m, "shaft", "obsidian", 0.0, Y1, 1.7, 1.5, sides=10, base=(44, 32, 86), smooth=55)
    for y in (12.0, 74.0, 138.0, Y1 - 2.0):
        collar_ring(m, y, 2.9, 2.2, "silver", (206, 200, 240), name="band_%d" % int(y))
    lathe_y(m, "butt", "silver", -6.0, [(0.0, 0.0), (1.8, 2.0), (6.0, 2.6)], sides=10, base=(200, 196, 232), caps=(False, True))
    cradle = P(m, "cradle", "silver", base=(214, 208, 244), smooth=40)
    ml.sweep(cradle, [(14.0 * math.sin(a), Y1 + 10.0 + 14.0 * (1 - math.cos(a)) * 0.0 + 12.0 * (1 - math.cos(a)) + 4.0, 0.0) for a in np.linspace(-1.9, 1.9, 17)], ml.octagon(2.0, 2.0, 0.5),
             scales=lambda t: (1.5 * (1 - abs(2 * t - 1) ** 2) + 0.35, 1.4 * (1 - abs(2 * t - 1) ** 2) + 0.35))
    for sx in (-1, 1):
        ml.bar(cradle, (sx * 1.4, Y1, 0.0), (sx * 8.0, Y1 + 12.0, 0.0), 1.3, 1.1, up=(0, 0, 1), taper=1.0)
    star = P(m, "falling_star", "crystal", base=(180, 160, 255), glow=True, smooth=22)
    ml.gem(star, (0.0, Y1 + 20.0, 0.0), 5.0, 2.0, 5)
    ml.gem(star, (0.0, Y1 + 20.0, 0.0), 5.0, 2.0, 5, axis="x")
    tail = P(m, "star_tail", "crystal", base=(210, 196, 255), glow=True, smooth=25)
    for k in range(5):
        spike(tail, (0.6 * (k - 2), Y1 + 24.0, 0.0), (2.8 * (k - 2), Y1 + 40.0 + (k % 2) * 3.0, 0.0), 0.7, 4)
    eyes = P(m, "eye_gems", "crystal", base=(255, 224, 140), glow=True, smooth=28)
    for k in range(7):
        a = math.pi * (0.1 + 0.8 * k / 6.0)
        ml.gem(eyes, (11.0 * math.cos(a) * 0.0 + (k - 3) * 3.4, Y1 + 13.0 + 7.0 * math.sin(a) * 0.0 + 6.0 * math.sin(a), 1.6), 0.9, 1.2, 4)
    script = P(m, "star_script", "silver", base=(210, 200, 255), glow=True, smooth=30)
    for i in range(11):
        y = 26.0 + i * 15.0
        ml.bar(script, (1.7, y, 0.8), (2.3, y + 4.0, -0.4), 0.5, 0.2, up=(1, 0, 0), taper=1.0)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    glow_parts(atlas, m, ["falling_star", "star_tail"], (190, 170, 255), 1.0, 0.6)
    glow_parts(atlas, m, ["eye_gems", "star_script"], (255, 226, 150), 0.9, 0.5)
    finish("orials_starfall_staff", m, atlas, "staff", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_chronarchs_pendulum_sabre():
    """Chronarch's Pendulum Sabre: a curved brass sabre with a clock-face disc guard (a gear ring, twelve glowing hour marks and two hands), a gear spine and a pendulum-weight pommel."""
    m = new_mesh("chronarchs_pendulum_sabre")
    L0 = 22.0
    N = 18
    pts = [(7.5 * (i / N) ** 2.0, L0 + 74.0 * i / N) for i in range(N + 1)]
    blade_curve(m, "blade", "gold", pts, lambda t: 3.1 * (1.0 - 0.6 * t ** 1.3), lambda t: 0.5 * (1.0 - 0.4 * t), bevel=0.5, base=(214, 180, 110), smooth=48)
    face = P(m, "clock_face", "gold", base=(236, 220, 176), smooth=40, uv_mode="loft")
    ml.lathe(face, "y", (0, L0 - 0.6, 0), [(-0.5, 2.0), (-0.3, 5.4), (0.3, 5.4), (0.5, 2.0)], sides=24)
    ringp = P(m, "gear_ring", "gold", base=(190, 142, 64), smooth=38)
    ml.sweep(ringp, [(5.8 * math.cos(a), L0 - 0.6, 5.8 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 25)], ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (0.7, 0.8))
    for k in range(16):
        a = 2 * math.pi * k / 16
        spike(ringp, (5.8 * math.cos(a), L0 - 0.6, 5.8 * math.sin(a)), (7.2 * math.cos(a), L0 - 0.6, 7.2 * math.sin(a)), 0.55, 4)
    marks = P(m, "hour_marks", "crystal", base=(255, 214, 110), glow=True, smooth=32)
    for k in range(12):
        a = 2 * math.pi * k / 12
        ml.gem(marks, (4.6 * math.cos(a), L0 + 0.1, 4.6 * math.sin(a)), 0.45, 1.2, 4)
    hands = P(m, "clock_hands", "gold", base=(255, 224, 130), glow=True, smooth=30)
    ml.bar(hands, (0.0, L0 + 0.3, 0.0), (0.0, L0 + 0.3, 3.8), 0.5, 0.25, up=(0, 1, 0), taper=1.5)
    ml.bar(hands, (0.0, L0 + 0.3, 0.0), (2.9, L0 + 0.3, -1.6), 0.4, 0.25, up=(0, 1, 0), taper=1.5)
    spine = P(m, "gear_spine", "gold", base=(176, 134, 60), smooth=34)
    for i in range(3, N - 2, 3):
        x, y = pts[i]
        for k in range(6):
            a = 2 * math.pi * k / 6
            spike(spine, (x + 3.0 * (1 - 0.6 * i / N) + 0.8 * math.cos(a), y + 0.8 * math.sin(a), 0.6 * math.sin(a)), (x + 3.0 * (1 - 0.6 * i / N) + 2.2 * math.cos(a), y + 2.2 * math.sin(a), 0.0), 0.4, 4)
    grip(m, 3.0, L0 - 1.2, r=1.5, base=(48, 36, 26), wrap_base=(196, 160, 90), n_wraps=7, material="leather", sides=9)
    rod(m, "pendulum_rod", "gold", -9.0, 3.0, 0.35, 0.35, sides=6, base=(200, 160, 80))
    orb(m, "pendulum_weight", "gold", (0.0, -11.0, 0.0), 2.8, base=(232, 190, 90), glow=True, ry=2.4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def ticks(pc):
        if not pc.face:
            return
        t = np.clip((pc.Y - L0) / 74.0, 0, 1)
        m_ = (np.abs(((pc.Y - L0) % 6.0) - 3.0) < 0.35) * np.clip(pc.X * 0 + 1, 0, 1) * (t > 0.08)
        darken(pc, m_, 0.3)
        put_glow(pc, m_ * 0.5, (255, 214, 110), 0.5, 0.4)
    paint(atlas, blade, ticks)
    glow_parts(atlas, m, ["hour_marks", "clock_hands"], (255, 220, 120), 1.0, 0.6)
    glow_parts(atlas, m, ["pendulum_weight"], (255, 200, 90), 0.6, 0.4)
    finish("chronarchs_pendulum_sabre", m, atlas, "sabre", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_reflected_kings_mirrorblade():
    """Reflected King's Mirrorblade: a long blade of silvered mirror-glass in segments, a rectangular mirror-frame guard with a glowing pane, a silver-grey grip and a faceted crystal pommel."""
    m = new_mesh("reflected_kings_mirrorblade")
    L0 = 26.0
    st = [(L0, -3.0, 3.0, 0.5), (L0 + 6, -3.4, 3.4, 0.55), (68, -3.3, 3.3, 0.5), (92, -2.9, 2.9, 0.45), (108, -2.1, 2.1, 0.38), (116, -1.1, 1.1, 0.28), (119, 0.0, 0.0, 0.0)]
    out = straight_sword(m, st, L0, guard="cross", guard_kw=dict(half=8.0, t=1.0, h=1.0, bulb=1.0, curve=0.0), grip_len=22.0, pommel_style="orb", pommel_kw=dict(r=2.4),
                         blade_material="silver", blade_base=(226, 234, 246), guard_material="silver", guard_base=(100, 116, 140), grip_base=(70, 78, 96), wrap_base=(190, 200, 222), bevel=0.4)
    frame = P(m, "frame", "silver", base=(112, 128, 156), smooth=36)
    for sx in (-1, 1):
        ml.bar(frame, (sx * 5.0, L0 + 1.0, 0.0), (sx * 5.0, L0 + 13.0, 0.0), 1.2, 1.3, up=(0, 0, 1), taper=0.0)
    ml.bar(frame, (-5.0, L0 + 13.0, 0.0), (5.0, L0 + 13.0, 0.0), 1.2, 1.3, up=(0, 0, 1), taper=0.0)
    pane = P(m, "mirror_pane", "crystal", base=(210, 236, 255), glow=True, smooth=30)
    for sgn in (1, -1):
        ml.bar(pane, (0.0, L0 + 2.0, sgn * 0.5), (0.0, L0 + 12.0, sgn * 0.5), 8.4, 0.12, up=(0, 0, sgn), taper=0.0)
    seams = P(m, "segment_seams", "silver", base=(120, 136, 164), smooth=30)
    for yy in (52.0, 76.0, 98.0):
        for sgn in (1, -1):
            ml.bar(seams, (-3.0, yy, sgn * 0.58), (3.0, yy, sgn * 0.58), 0.6, 0.1, up=(0, 0, sgn), taper=0.0)
    gem = P(m, "pommel_crystal", "crystal", base=(210, 236, 255), glow=True, smooth=26)
    ml.gem(gem, (0.0, 1.5, 0.0), 2.4, 1.3, 6)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    blade = m.part("blade")

    def glass(pc):
        if not pc.face:
            return
        n = w.noise2(pc.X.shape, 3, 9)
        sheen = np.clip(np.sin((pc.Y * 0.18) + pc.X * 0.9), 0, 1)
        pc.img[..., :3] = pc.img[..., :3] * (0.9 + 0.1 * n[..., None]) + np.array([30, 40, 60], np.float32) * sheen[..., None] * 0.4
        put_glow(pc, sheen * 0.25, (220, 240, 255), 0.5, 0.5)
    paint(atlas, blade, glass)
    paint_edge_glow(atlas, blade, out, (220, 240, 255), 0.5, 0.7, 0.5, side="both")
    glow_parts(atlas, m, ["mirror_pane", "pommel_crystal"], (225, 244, 255), 0.9, 0.55)
    finish("reflected_kings_mirrorblade", m, atlas, "longsword", glow=("FFFFFF", 0.14, 0.45, 1.0))


def build_aurliels_seraph_halberd():
    """Auriel's Seraph Halberd: a white-gold halberd: an axe blade with a pair of swept feather wings, a spear point, a halo ring under the head and a sun gem at the socket."""
    m = new_mesh("aurliels_seraph_halberd")
    Y_S = 170.0
    shaft(m, 0.0, Y_S, r=1.8, material="wood", base=(236, 226, 196), name="shaft", sides=10)
    for y in (10.0, 60.0, 112.0):
        collar_ring(m, y, 2.3, 1.8, "gold", (236, 190, 80), name="band_%d" % int(y))
    wrap(m, "grip_wraps", 24.0, 84.0, 1.8, 12, "cloth", (255, 246, 214))
    lathe_y(m, "butt", "gold", -5.0, [(0.0, 0.0), (1.6, 1.8), (5.0, 2.4)], sides=10, base=(236, 190, 80), caps=(False, True))
    lathe_y(m, "socket", "gold", Y_S - 6.0, [(0.0, 2.2), (3.0, 3.6), (8.0, 3.8), (14.0, 2.2)], sides=12, base=(240, 198, 90))
    out = spear_head(m, Y_S + 8.0, 40.0, 3.6, t=0.9, base=(250, 244, 220), material="gold", leaf=0.3, name="point", bevel=0.5)
    blade_pts = [(-3.0, Y_S - 2.0)] + [(-(3.0 + 21.0 * math.sin(math.radians(a))), Y_S + 6.0 + 20.0 * (1 - math.cos(math.radians(a))) * 0.9 - 8.0 * math.sin(math.radians(a))) for a in np.linspace(5, 85, 10)] + [(-3.0, Y_S + 24.0)]
    extrude(m, "axe_blade", "gold", blade_pts, 1.1, edge=0.75, base=(252, 236, 190), smooth=38)          # the edge toward -x, like every weapon of the mod
    wings = P(m, "wings", "gold", base=(255, 250, 232), smooth=36)
    for sx in (1, -1):
        for k in range(5):
            f = k / 4.0
            ln = 26.0 - 8.0 * f
            ml.sweep(wings, [(-sx * (3.0 + ln * t), Y_S - 4.0 + 12.0 * (1 - f) * t ** 1.5 + 6.0 * f * t, -0.6 - 0.5 * k * 0.0) for t in np.linspace(0, 1, 7)], ml.bevelled(2.0, 2.0, 0.5),
                     scales=lambda t, f=f: ((1.6 - 0.9 * f) * (1 - 0.85 * t) + 0.15, 0.5 * (1 - 0.6 * t) + 0.08))
    halo = P(m, "halo_ring", "gold", base=(255, 220, 120), glow=True, smooth=40)
    ml.sweep(halo, [(9.0 * math.cos(a), Y_S - 18.0, 9.0 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 29)], ml.octagon(2.0, 2.0, 0.5), scales=lambda t: (0.55, 0.55))
    gem = P(m, "sun_gem", "crystal", base=(255, 224, 120), glow=True, smooth=28)
    ml.gem(gem, (0.0, Y_S - 2.0, 3.8), 2.0, 1.4, 8)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    paint_center_glow(atlas, m.part("point"), out, (255, 230, 140), 0.4, Y_S + 10.0, Y_S + 46.0, 0.9, 0.5)
    glow_parts(atlas, m, ["halo_ring", "sun_gem"], (255, 226, 130), 1.0, 0.6)
    glow_parts(atlas, m, ["wings", "axe_blade"], (255, 240, 190), 0.3, 0.3)
    finish("aurliels_seraph_halberd", m, atlas, "glaive", glow=("FFFFFF", 0.15, 0.45, 1.0))


def build_nekrathuls_pact_scythe():
    """Nekrathul's Pact Scythe (Genesis): a tall black pole with bone skull ferrules, a huge violet crescent blade with the dimensional void showing through three rifts, a soul crystal at the root and a spike above; pact seals glow down the shaft."""
    m = new_mesh("nekrathuls_pact_scythe")
    Y0 = 190.0
    shaft(m, 0.0, Y0, r=1.8, material="obsidian", base=(26, 16, 38), name="shaft", sides=10)
    for y in (10.0, 64.0, 124.0):
        collar_ring(m, y, 2.4, 1.8, "iron", (96, 60, 130), name="band_%d" % int(y))
    sk = P(m, "skull_ferrules", "bone", base=(230, 222, 204), smooth=45)
    for y in (4.0, Y0 - 6.0):
        w.ellipsoid(sk, (0.0, y, 0.0), 3.4, 3.1, 3.1, sides=10, stacks=7)
    ctrl = [(0.0, Y0 + 2.0), (-10.0, Y0 + 13.0), (-26.0, Y0 + 19.0), (-45.0, Y0 + 12.0), (-58.0, Y0 - 3.0), (-63.0, Y0 - 22.0)]
    blade, pts = sweep_blade(m, "blade", "obsidian", ctrl, lambda t: 9.0 * (1.0 - t) ** 0.75 + 0.6, lambda t: 1.0 * (1.0 - 0.5 * t), n=7, bevel=0.5, base=(40, 20, 70), smooth=45)
    spk = P(m, "top_spike", "obsidian", base=(36, 18, 60), smooth=40, uv_mode="loft")
    ml.lathe(spk, "y", (0, Y0, 0), [(0.0, 2.2), (4.0, 2.4), (14.0, 1.1), (24.0, 0.0)], sides=8, caps=(True, False))
    cr = P(m, "soul_crystal", "crystal", base=(190, 90, 255), glow=True, smooth=28)
    ml.gem(cr, (-2.0, Y0 + 2.0, 2.4), 2.2, 1.7, 6)
    ribs = P(m, "blade_ribs", "obsidian", base=(160, 80, 230), glow=True, smooth=40)
    for i in range(3, 19, 3):
        x, y = pts[i]
        ml.bar(ribs, (x - 1.0, y + 1.0, 0.9), (x + 1.2, y + 4.0, 0.9), 0.55, 0.14, up=(0, 0, 1), taper=1.0)
    seals = P(m, "pact_seals", "silver", base=(200, 130, 255), glow=True, smooth=30)
    for i in range(9):
        y = 22.0 + i * 16.0
        ml.bar(seals, (1.9, y, 0.9), (2.5, y + 5.0, -0.5), 0.55, 0.2, up=(1, 0, 0), taper=1.0)
        ml.bar(seals, (-1.9, y + 7.0, 0.9), (-2.5, y + 11.0, -0.5), 0.55, 0.2, up=(-1, 0, 0), taper=1.0)
    for k in (4, 9, 14):                                                # the three rifts of the void through the crescent
        x, y = pts[k]
        t = k / float(len(pts) - 1)
        half = max(1.4, 7.0 * (1.0 - t) ** 0.75 - 1.0)
        void_poly(m, "void_%d" % k, [(x - 2.2, y - half * 0.7), (x + 0.4, y - half * 0.4), (x + 2.0, y + half * 0.2), (x + 0.6, y + half * 0.8), (x - 1.6, y + half * 0.5), (x - 2.8, y)], 1.06)
    wisps = P(m, "soul_wisps", "crystal", base=(180, 120, 255), glow=True, smooth=25)
    for k, (x, y, z) in enumerate(((7.0, 60.0, 3.0), (-7.0, 96.0, -3.0), (7.0, 132.0, 2.5), (-6.5, 160.0, 3.0))):
        ml.gem(wisps, (x, y, z), 0.9 + 0.2 * (k % 3), 1.5, 4)
    atlas = new_atlas(512)
    lay_and_paint(m, atlas)
    bl = m.part("blade")

    def violet(pc):
        if not pc.face:
            return
        t = np.clip(np.hypot(pc.X, pc.Y - Y0) / 74.0, 0, 1)
        n = w.noise2(pc.X.shape, 4, 17)
        pc.img[..., :3] = pc.img[..., :3] * (0.8 + 0.4 * n[..., None]) + np.array([90, 40, 150], np.float32) * (0.25 + 0.5 * t[..., None])
        put_glow(pc, np.clip((t - 0.3) * 1.3, 0, 1) * 0.7, (190, 100, 255), 0.8, 0.5)
    paint(atlas, bl, violet)
    paint_glow(atlas, m.part("pact_seals"), (200, 130, 255), 0.8, 0.5)
    glow_parts(atlas, m, ["soul_crystal", "blade_ribs", "soul_wisps"], (210, 110, 255), 1.0, 0.6)
    finish("nekrathuls_pact_scythe", m, atlas, "scythe", glow=("FFFFFF", 0.17, 0.4, 1.0))


BUILDERS = {n[len("build_"):]: f for n, f in globals().items() if n.startswith("build_") and callable(f)}

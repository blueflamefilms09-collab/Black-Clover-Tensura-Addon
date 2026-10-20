"""Upgrades of Chronarch's Pendulum Sabre, the Reflected King's Mirrorblade, Auriel's Seraph Halberd and Paradox (see items_a.py for the layout of an item)."""
import math

import numpy as np

import lib
import kit
from lib import P, ml, nl, V
from kit import spike_row, cog, coil, flame_ribbons
from items_a import core_glow, vein_glow, vines_on, layers, AMBIENT, PASSES, ANIMATIONS


# ================================================================ Chronarch's Pendulum Sabre
def geo_chronarch(m):
    L0 = 22.0
    kit.numerals(m, "hour_numerals", "gold", (255, 226, 150), L0 + 0.4, 2.4, 12, 1.0, 0.34)
    cg = P(m, "blade_cogs", "gold", base=(208, 164, 78), smooth=35)
    for (t, r, teeth) in ((0.27, 1.55, 7), (0.49, 1.25, 6), (0.68, 0.95, 6)):
        y = L0 + 74.0 * t
        x = 7.5 * t ** 2.0
        for sgn in (1,):
            cog(cg, (x, y, sgn * 0.62), r, teeth, 0.14, tooth=0.5, axis="z", hub=0.4)
    coil(m, "pendulum_spring", "gold", (222, 180, 90), -8.2, 2.4, 0.95, 5.0, 0.16, n=28, sides=4)
    spike_row(m, "weight_fins", "gold", (236, 196, 100), [((1.9 * math.cos(a), -11.0, 1.9 * math.sin(a)), (3.7 * math.cos(a), -11.0 - 1.4, 3.7 * math.sin(a))) for a in np.linspace(0, 2 * math.pi, 9)[:-1]], rad=0.45, sides=4)
    tk = P(m, "spine_ticks", "gold", base=(255, 222, 130), smooth=30)
    for k in range(10):
        t = (8.0 + 6.6 * k) / 74.0
        y = L0 + 74.0 * t
        x = 7.5 * t ** 2.0 + 3.1 * (1.0 - 0.6 * t ** 1.3) - 0.9
        ml.bar(tk, (x - 0.35, y, 0.56), (x + 0.35, y, 0.56), 0.22, 0.08, up=(0, 0, 1), taper=0.0)
        ml.bar(tk, (x - 0.35, y, -0.56), (x + 0.35, y, -0.56), 0.22, 0.08, up=(0, 0, -1), taper=0.0)
    arms = P(m, "escapement_arms", "gold", base=(190, 146, 66), smooth=36)
    for sx in (-1, 1):
        ml.sweep(arms, [(sx * (6.4 + 2.4 * math.sin(a)), L0 - 0.6 + 3.2 * (1 - math.cos(a)), 0.0) for a in np.linspace(0.0, 1.5, 6)], ml.bevelled(2.0, 2.0, 0.5),
                 scales=lambda t: (0.7 * (1 - 0.7 * t) + 0.15, 0.5 * (1 - 0.6 * t) + 0.12), up=(0, 0, 1))
    o = P(m, "orbit_cog", "gold", base=(236, 192, 96), smooth=35)
    cog(o, (0.0, 0.0, 0.0), 1.3, 6, 0.22, tooth=0.55, axis="z", hub=0.0)


def paint_chronarch(m, atlas, g2):
    blade = m.part("blade")

    def ticks(pc):                                                      # a second, finer scale of hour ticks and a pendulum arc engraved along the blade
        if not pc.face:
            return
        mk = (np.abs(((pc.Y - 22.0) % 3.0) - 1.5) < 0.18) * (nl.step(pc.Y, 30.0, 33.0)) * (1 - nl.step(pc.Y, 90.0, 94.0)) * (nl.sm(pc.X - (pc.X * 0 + 1.2), 4.0))
        lib.engrave(pc, mk, 0.35)
    nl.paint(atlas, blade, ticks)
    vines_on(atlas, g2, blade, (255, 214, 120), [(1.5, 1.0, 18.0, 0.0, 0.22, 32.0, 92.0)], etch=0.3, core_rgb=(255, 224, 140), core_strength=0.3)
    core_glow(atlas, m, ["hour_numerals", "spine_ticks", "pendulum_spring", "orbit_cog", "blade_cogs"], (255, 214, 110), 0.8, 0.5)
    vein_glow(atlas, g2, m, ["hour_numerals", "spine_ticks", "pendulum_spring", "blade_cogs", "hour_marks", "clock_hands", "weight_fins", "gear_ring", "escapement_arms"], (255, 210, 100), 0.95, 0.5)

    def sweepline(pc):
        if not pc.face:
            return
        nl.put_glow(pc, nl.sm(pc.X - (7.5 * ((pc.Y - 22.0) / 74.0) ** 2.0) - 0.2, 0.35) * (pc.Y > 28) * (pc.Y < 96), (255, 200, 90), 0.6, 0.5)
    lib.paint_g2(atlas, g2, blade, sweepline)


SPEC_CHRONARCH = dict(
    palette=dict(core="FFD070", accent="FFF0C0", vein="FFC850", halo="C8B07A"),
    veins=dict(speed=0.50, wavelength=24.0, base=0.12, peak=1.0), halo=dict(scale=1.07, alpha=54, breath=0.035, speed=0.12),
    orbiters=[dict(count=4, radius=9.0, y_frac=0.20, speed=0.060, spin=-0.14, bob=0.8, bob_speed=0.08, tilt=0.0, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="FFD070", ghosts=4, lag_deg=7.0, alpha=84),
    particles=layers(motes=dict(particle="minecraft:end_rod", rgb="FFE8A8", rate=0.9, speed=0.008), halo_ring=dict(rgb="FFD070", rate=1.5), edge_line=dict(particle="dust", rgb="FFC850", rate=1.0, size=0.8),
                     ground_cast=dict(rgb="C8A040"), wrist_flare=dict(particle="minecraft:glow", rgb="FFD070"), light_shaft=dict(rgb="FFE090"), accent_flare=dict(particle="minecraft:firework", rate=0.12)),
    ground_radius=1.0, shaft_height=1.4,
)


# ================================================================ Reflected King's Mirrorblade
def geo_mirror(m):
    L0 = 26.0
    hw = lambda y: float(np.interp(y, [L0, L0 + 6, 68, 92, 108, 116, 119], [3.0, 3.4, 3.3, 2.9, 2.1, 1.1, 0.0]))
    pn = P(m, "mirror_panels", "silver", base=(232, 240, 252), smooth=30)
    for sgn in (1, -1):
        for (ya, yb) in ((40.0, 50.0), (54.0, 74.0), (78.0, 96.0), (100.0, 109.0)):
            hh = min(hw(ya), hw(yb)) * 0.62
            ml.box(pn, (-hh, ya, sgn * 0.5 if sgn > 0 else -0.64), (hh, yb, 0.64 if sgn > 0 else -0.5), 0.1)
    vine = P(m, "blade_filigree", "silver", base=(206, 220, 246), smooth=40)
    for sgn in (1, -1):
        ml.sweep(vine, lib.vine_pts(0.0, 0.0, 41.0, 108.0, 1.1, 5.0, z=sgn * 0.66, n=26), ml.bevelled(2.0, 2.0, 0.45), scales=lambda t: (0.16 * (1.0 - 0.6 * t) + 0.06, 0.07), up=(0, 0, sgn))
    for sgn in (1, -1):
        cv = P(m, "frame_curls_%s" % ("f" if sgn > 0 else "b"), "silver", base=(176, 190, 220), smooth=40)
        ml.sweep(cv, lib.vine_pts(-5.0, 5.0, L0 + 14.8, L0 + 14.8, 1.0, 2.0, z=sgn * 0.5, n=14), ml.bevelled(2.0, 2.0, 0.45), scales=lambda t: (0.3, 0.24), up=(0, 1, 0))
    fg = P(m, "frame_gems", "crystal", base=(200, 232, 255), smooth=28)
    for (x, y) in ((-5.0, L0 + 13.9), (5.0, L0 + 13.9), (-5.0, L0 + 1.5), (5.0, L0 + 1.5)):
        ml.gem(fg, (x, y, 0.0), 0.8, 1.2, 4, "y")
    spike_row(m, "guard_crown", "silver", (214, 224, 244), [((x, L0 + 1.2, 0.0), (x, L0 + 3.6 + 2.6 * (1.0 - abs(x) / 7.0), 0.0)) for x in np.linspace(-6.5, 6.5, 7)], rad=0.5, sides=4)
    lib.tube_ring(m, "echo_ring_low", "silver", 0.0, 62.0, 0.0, 4.5, 0.2, n=20, plane="xz", base=(214, 228, 250), sides=5)
    lib.tube_ring(m, "echo_ring_high", "silver", 0.0, 90.0, 0.0, 4.0, 0.18, n=20, plane="xz", base=(214, 228, 250), sides=5)
    for y in (11.0, 17.0):
        lib.gem_ring(m, "grip_gems_%d" % int(y), "crystal", 0.0, y, 0.0, 1.78, 6, 0.42, 1.3, base=(190, 220, 255), glow=False, sides=4)
    o = P(m, "orbit_pane", "silver", base=(228, 238, 252), smooth=30)
    ml.box(o, (-0.9, -1.5, -0.1), (0.9, 1.5, 0.1), 0.08)
    fr = P(m, "orbit_pane_frame", "silver", base=(120, 136, 168), smooth=30)
    ml.gem(fr, (0.0, 1.9, 0.0), 0.3, 1.4, 4, "y")


def paint_mirror(m, atlas, g2):
    blade = m.part("blade")
    vines_on(atlas, g2, blade, (200, 232, 255), [(0.0, 0.9, 20.0, 1.0, 0.28, 40.0, 112.0)], etch=0.0, core_rgb=(225, 242, 255), core_strength=0.35)

    def sheen(pc):                                                      # a travelling reflection streak across every panel
        if not pc.face:
            return
        mk = nl.sm(((pc.Y * 0.55 + pc.X * 0.9) % 9.0) - 4.5, 1.1) * (pc.Y > 38) * (pc.Y < 112)
        nl.put_glow(pc, mk, (205, 232, 255), 0.5, 0.5)
    lib.paint_g2(atlas, g2, blade, sheen)
    core_glow(atlas, m, ["blade_filigree", "echo_ring_low", "echo_ring_high", "frame_gems", "grip_gems_11", "grip_gems_17", "orbit_pane", "orbit_pane_frame", "mirror_panels"], (215, 238, 255), 0.55, 0.5)
    vein_glow(atlas, g2, m, ["blade_filigree", "echo_ring_low", "echo_ring_high", "frame_gems", "mirror_pane", "frame_curls_f", "frame_curls_b", "guard_crown", "pommel_crystal", "grip_gems_11", "grip_gems_17", "orbit_pane_frame"], (195, 228, 255), 0.95, 0.5)


SPEC_MIRROR = dict(
    palette=dict(core="CFE8FF", accent="FFFFFF", vein="B4DCFF", halo="E8EEF8"),
    veins=dict(speed=0.62, wavelength=34.0, base=0.10, peak=1.0), halo=dict(scale=1.06, alpha=60, breath=0.03, speed=0.09),
    orbiters=[dict(count=6, radius=8.0, y_frac=0.55, speed=0.052, spin=0.12, bob=2.0, bob_speed=0.06, tilt=0.10, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="CFE8FF", ghosts=3, lag_deg=9.0, alpha=88),
    particles=layers(motes=dict(particle="minecraft:end_rod", rgb="FFFFFF", rate=1.0, speed=0.010), halo_ring=dict(rgb="CFE8FF"), edge_line=dict(particle="minecraft:electric_spark", rgb="E8F4FF", rate=0.8),
                     ground_cast=dict(rgb="8AA8D8"), wrist_flare=dict(particle="minecraft:glow", rgb="E8F4FF"), light_shaft=dict(rgb="FFFFFF"), accent_flare=dict(particle="minecraft:firework", rate=0.14)),
    ground_radius=1.1, shaft_height=1.8,
)


# ================================================================ Auriel's Seraph Halberd
def geo_aurliel(m):
    Y_S = 170.0
    barbs = []
    for sx in (1, -1):
        for k in range(5):
            f = k / 4.0
            ln = 26.0 - 8.0 * f
            for t in (0.45, 0.65, 0.85):
                x = -sx * (3.0 + ln * t)
                y = Y_S - 4.0 + 12.0 * (1 - f) * t ** 1.5 + 6.0 * f * t
                barbs.append(((x, y, -0.6), (x - sx * 0.7, y + 2.6, -0.6)))
    spike_row(m, "feather_barbs", "gold", (255, 244, 214), barbs, rad=0.42, sides=3)
    lib.gem_ring(m, "halo_beads", "crystal", 0.0, Y_S - 18.0, 0.0, 9.0, 12, 0.42, 1.4, base=(255, 232, 150), glow=False, sides=4)
    spike_row(m, "sun_rays", "gold", (255, 226, 130), [((2.2 * math.cos(a), Y_S - 2.0 + 2.2 * math.sin(a), 3.6), (4.6 * math.cos(a), Y_S - 2.0 + 4.6 * math.sin(a), 3.6)) for a in np.linspace(0, 2 * math.pi, 11)[:-1]], rad=0.42, sides=4)
    flame_ribbons(m, "cloth_streamers", "cloth", (255, 244, 220), (0.0, Y_S - 7.0, 2.2), [(0.12, -1.0, 0.1), (-0.2, -1.0, -0.05), (0.0, -1.0, 0.25)], 30.0, 1.4, n=8, sway=2.6, up=(0, 0, 1))
    for y in (36.0, 84.0, 132.0):
        pr = P(m, "filigree_band_%d" % int(y), "gold", base=(246, 206, 96), smooth=40, uv_mode="loft")
        ml.lathe(pr, "y", (0, y - 2.0, 0), [(0.0, 1.85), (0.6, 2.4), (1.4, 2.0), (2.6, 2.4), (3.2, 1.85), (4.0, 1.85)], sides=10)
    lib.tube_ring(m, "crown_ring", "gold", 0.0, Y_S + 14.0, 0.0, 5.8, 0.24, n=20, plane="xz", base=(255, 224, 120), sides=5)
    o = P(m, "orbit_feather", "cloth", base=(255, 250, 232), smooth=35)
    ml.sweep(o, [(0.0, -3.2 + 6.4 * t, 0.6 * math.sin(3.0 * t)) for t in np.linspace(0, 1, 6)], ml.bevelled(2.0, 2.0, 0.5), scales=lambda t: (0.85 * (1 - 0.5 * abs(2 * t - 1.0) ** 2) + 0.1, 0.08), up=(1, 0, 0))


def paint_aurliel(m, atlas, g2):
    point = m.part("point")
    vines_on(atlas, g2, point, (255, 226, 130), [(0.0, 0.9, 12.0, 0.0, 0.22, 182.0, 214.0), (0.0, 0.9, 12.0, math.pi, 0.22, 182.0, 214.0)], core_rgb=(255, 240, 180))
    core_glow(atlas, m, ["halo_beads", "sun_rays", "crown_ring", "orbit_feather", "feather_barbs", "cloth_streamers"], (255, 232, 150), 0.7, 0.55)
    vein_glow(atlas, g2, m, ["halo_ring", "halo_beads", "sun_gem", "sun_rays", "crown_ring", "wings", "axe_blade", "feather_barbs", "filigree_band_36", "filigree_band_84", "filigree_band_132"], (255, 220, 120), 0.9, 0.5)

    def hymn(pc):                                                       # golden lines winding up the shaft
        v = pc.B if not pc.face else pc.Y
        mk = np.clip(1.0 - np.abs(((v * 0.45 + pc.A * 2.4) % 6.28) - 3.14) / 0.3, 0, 1)
        nl.put_glow(pc, mk, (255, 224, 130), 0.6, 0.5)
    lib.paint_g2(atlas, g2, m.part("shaft"), hymn)


SPEC_AURLIEL = dict(
    palette=dict(core="FFE890", accent="FFFBE8", vein="FFD870", halo="FFF0C0"),
    veins=dict(speed=0.34, wavelength=48.0, base=0.20, peak=1.0), halo=dict(scale=1.08, alpha=70, breath=0.05, speed=0.05),
    orbiters=[dict(count=5, radius=13.0, y_frac=0.74, speed=0.030, spin=0.06, bob=2.4, bob_speed=0.045, tilt=0.15, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="FFE890", ghosts=4, lag_deg=8.0, alpha=96),
    particles=layers(motes=dict(particle="minecraft:end_rod", rgb="FFF4C0", rate=1.3, speed=0.014), halo_ring=dict(rgb="FFE890", rate=1.6), edge_line=dict(particle="minecraft:glow", rgb="FFF0B0", rate=0.9),
                     ground_cast=dict(rgb="E8B84A"), wrist_flare=dict(particle="minecraft:end_rod", rgb="FFF4C0"), light_shaft=dict(rgb="FFE890", rate=1.5), accent_flare=dict(particle="minecraft:totem_of_undying", rate=0.05)),
    ground_radius=1.3, shaft_height=2.4,
)


# ================================================================ Paradox
def geo_paradox(m):
    K = 0.1434
    kit_ring = lib.tube_ring(m, "time_ring", "crystal", 0.0, 155.0, 0.0, 11.0, 0.22, n=24, plane="xz", base=(150, 112, 236), sides=5)
    lib.gem_ring(m, "time_ticks", "crystal", 0.0, 155.0, 0.0, 11.0, 8, 0.4, 1.5, base=(200, 230, 170), glow=False, sides=4)
    spike_row(m, "sand_shards", "crystal", (200, 160, 255), [((2.6 * math.cos(a), 149.0, 2.6 * math.sin(a)), (4.8 * math.cos(a), 153.0 + (i % 2) * 2.0, 4.8 * math.sin(a))) for i, a in enumerate(np.linspace(0, 2 * math.pi, 9)[:-1])], rad=0.5, sides=4)
    coil(m, "thorn_vine", "cloth", (96, 190, 120), 33.0, 90.0, 4.5, 5.0, 0.2, n=30, sides=4, phase=0.5)
    o = P(m, "orbit_sand", "crystal", base=(200, 170, 255), smooth=35)
    ml.lathe(o, "y", (0, -1.6, 0), [(0.0, 0.0), (0.4, 0.85), (1.6, 0.18), (2.8, 0.85), (3.2, 0.0)], sides=6)


def paint_paradox(m, atlas, g2):
    core_glow(atlas, m, ["time_ring", "time_ticks", "sand_shards", "thorn_vine", "orbit_sand"], (190, 150, 255), 0.8, 0.55)
    core_glow(atlas, m, ["thorn_vine"], (140, 230, 160), 0.6, 0.5)
    vein_glow(atlas, g2, m, ["time_ring", "time_ticks", "sand_shards", "thorn_vine", "crystal_core", "crystal_shards", "crystal_orb", "cage_ring", "cage_spikes", "halo"], (200, 160, 255), 0.95, 0.5)
    vein_glow(atlas, g2, m, ["green_binding"], (130, 230, 160), 0.25, 0.3)

    def sap(pc):                                                        # violet light running up the thorned shaft
        v = pc.B if not pc.face else pc.Y
        mk = np.clip(np.sin(v * 0.55 + pc.A * 3.1) - 0.8, 0, 1) * 5.0
        nl.put_glow(pc, mk, (190, 140, 255), 0.7, 0.5)
    for nm in ("lower_shaft", "upper_shaft"):
        lib.paint_g2(atlas, g2, m.part(nm), sap)


SPEC_PARADOX = dict(
    palette=dict(core="C8A0FF", accent="8CD8A0", vein="B488FF", halo="5A2A8A"),
    veins=dict(speed=0.30, wavelength=60.0, base=0.15, peak=1.0), halo=dict(scale=1.05, alpha=62, breath=0.05, speed=0.04),
    orbiters=[dict(count=6, radius=13.0, y_frac=0.83, speed=-0.028, spin=0.10, bob=2.0, bob_speed=0.05, tilt=0.35, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="C8A0FF", ghosts=3, lag_deg=8.0, alpha=84),
    particles=layers(motes=dict(particle="minecraft:enchant", rgb="C8A0FF", rate=1.4, speed=0.008), halo_ring=dict(rgb="C8A0FF", rate=1.5), edge_line=dict(particle="minecraft:reverse_portal", rgb="B488FF", rate=0.6, speed=0.004),
                     ground_cast=dict(rgb="6A3AAA"), wrist_flare=dict(particle="minecraft:glow", rgb="8CD8A0"), light_shaft=dict(rgb="B488FF"), accent_flare=dict(particle="minecraft:witch", rate=0.14)),
    ground_radius=1.2, shaft_height=2.4,
)

ITEMS_B = [
    dict(id="chronarchs_pendulum_sabre", geometry=geo_chronarch, paint=paint_chronarch, spec=SPEC_CHRONARCH),
    dict(id="reflected_kings_mirrorblade", geometry=geo_mirror, paint=paint_mirror, spec=SPEC_MIRROR),
    dict(id="aurliels_seraph_halberd", geometry=geo_aurliel, paint=paint_aurliel, spec=SPEC_AURLIEL),
    dict(id="paradox", geometry=geo_paradox, paint=paint_paradox, spec=SPEC_PARADOX),
]

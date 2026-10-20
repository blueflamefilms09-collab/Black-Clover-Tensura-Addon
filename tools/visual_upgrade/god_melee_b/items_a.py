"""Upgrades of Rimeheart Greatblade, Deepwatch Harpoon, Sanguine Warden's Cleaver, Valkarin's Stormlance and Oriel's Starfall Staff.

Each item is a dict: id, geometry(m) (new parts, called just before the UV layout of the ORIGINAL builder), paint(m, atlas, g2) (called after the builder's own painting:
engravings on the skin, the core glow map `atlas.glow`, the energy-vein map `g2`), spec (the data table of the effects, see lib.py / docs/visual_upgrade/god_melee_b.md).
Orbiting pieces are parts named `orbit_*` in LOCAL coordinates (around the origin); build.py writes them as the separate `<id>_orbit` mesh.
"""
import math

import numpy as np

import lib
import kit
from lib import P, ml, nl, V
from kit import chevrons, spike_row, cog, chain, coil, flame_ribbons, nl_helix


# ---------------------------------------------------------------- painting shorthands
def core_glow(atlas, m, names, rgb, s=0.9, c=0.5):
    for n in names:
        nl.paint_glow(atlas, m.part(n), rgb, s, c)


def vein_glow(atlas, g2, m, names, rgb, s=0.9, c=0.5):
    for n in names:
        lib.glow_part2(atlas, g2, m.part(n), rgb, s, c)


def vines_on(atlas, g2, part, rgb, specs, strength=0.85, etch=0.45, core_rgb=None, core_strength=0.45):
    """Winding lines on the flat faces of a blade part: etched into the skin, lit on the vein map (and faintly on the core map)."""
    def fn(pc):
        if not pc.face:
            return
        mk = np.zeros_like(pc.X)
        for (cx, amp, per, ph, wd, y0, y1) in specs:
            mk = np.maximum(mk, lib.vine_mask(pc.X, pc.Y, cx, amp, per, ph, wd, y0, y1))
        lib.engrave(pc, mk, etch)
        if core_rgb is not None:
            nl.put_glow(pc, mk, core_rgb, core_strength, 0.4)
    nl.paint(atlas, part, fn)

    def fn2(pc):
        if not pc.face:
            return
        mk = np.zeros_like(pc.X)
        for (cx, amp, per, ph, wd, y0, y1) in specs:
            mk = np.maximum(mk, lib.vine_mask(pc.X, pc.Y, cx, amp, per, ph, wd, y0, y1))
        nl.put_glow(pc, mk, rgb, strength, 0.5)
    lib.paint_g2(atlas, g2, part, fn2)


def layers(**over):
    """The seven particle layers (kinds are fixed, the particle, colour and rate are the item's own)."""
    base = {
        "motes": dict(particle="minecraft:end_rod", rgb="FFFFFF", rate=0.9, speed=0.012, size=1.0),
        "halo_ring": dict(particle="dust", rgb="FFFFFF", rate=1.4, speed=0.0, size=0.9),
        "edge_line": dict(particle="minecraft:end_rod", rgb="FFFFFF", rate=0.8, speed=0.004, size=1.0),
        "ground_cast": dict(particle="dust", rgb="FFFFFF", rate=1.0, speed=0.0, size=1.1),
        "wrist_flare": dict(particle="minecraft:glow", rgb="FFFFFF", rate=0.5, speed=0.01, size=1.0),
        "light_shaft": dict(particle="dust", rgb="FFFFFF", rate=1.2, speed=0.03, size=0.8),
        "accent_flare": dict(particle="minecraft:firework", rgb="FFFFFF", rate=0.07, speed=0.02, size=1.0),
    }
    for k, v in over.items():
        base[k].update(v)
    return [dict(kind=k, **v) for k, v in base.items()]


AMBIENT = ["grade_shimmer", "motes", "halo_ring", "edge_line", "ground_cast", "wrist_flare", "light_shaft", "accent_flare", "orbit_pieces"]
PASSES = ["body", "core_glow", "vein_wave", "halo_shell", "orbit_pieces"]
ANIMATIONS = ["glow_pulse", "core_flicker", "vein_wave_flow", "halo_shell_breath", "orbit_revolve", "orbit_piece_spin", "orbit_bob"]


# ================================================================ Rimeheart Greatblade
def geo_rimeheart(m):
    L0 = 38.0
    chevrons(m, "frost_ribs", "crystal", (180, 228, 250), 56.0, 128.0, 9, 2.5, 1.2, 2.8, 1.0, 0.30, 0.17)
    rng = np.random.default_rng(11)
    sh = []
    for i in range(18):
        y = 52.0 + i * 4.4 + rng.uniform(-0.6, 0.6)
        side = 1 if i % 2 == 0 else -1
        ln = rng.uniform(2.2, 4.4)
        x0 = 5.0 if side > 0 else -5.1
        sh.append(((x0, y, 0.0), (x0 + side * ln * 0.8, y + ln * 1.2, rng.uniform(-0.5, 0.5))))
    spike_row(m, "frost_shards", "crystal", (214, 240, 255), sh, rad=0.55, sides=5, glow=False)
    lib.tube_ring(m, "heart_halo", "crystal", 0.0, L0 + 3.0, 1.8, 4.6, 0.28, n=20, plane="xy", base=(150, 205, 250), sides=5)
    gp = P(m, "guard_gems", "crystal", base=(120, 190, 250), smooth=28)
    for sx in (-1, 1):
        for j in range(6):
            ax = 2.6 + j * 1.35
            y = L0 + 8.0 * ((ax - 1.2) / 10.0) ** 2.75 + 0.7
            ml.gem(gp, (sx * ax, y, (0.8 if j % 2 == 0 else -0.8)), 0.42, 1.5, 4)
    for y in (9.0, 17.0, 25.0):
        lib.nk.collar_ring(m, y, 1.85, 1.3, "silver", (150, 208, 240), name="frost_band_%d" % int(y), sides=8)
    lib.gem_ring(m, "pommel_gems", "crystal", 0.0, 7.0, 0.0, 2.3, 6, 0.45, 1.4, base=(130, 200, 255), glow=False, sides=4)
    # the orbiting piece: an ice shard with a small companion
    o = P(m, "orbit_shard", "crystal", base=(190, 236, 255), smooth=25)
    ml.gem(o, (0, 0, 0), 0.9, 3.2, 6, "y")
    ml.gem(o, (0.0, -3.6, 0.0), 0.45, 2.2, 4, "y")


def paint_rimeheart(m, atlas, g2):
    blade = m.part("blade")
    vines_on(atlas, g2, blade, (130, 215, 255), [(0.0, 1.6, 26.0, 0.0, 0.32, 52.0, 130.0), (0.0, 1.6, 26.0, math.pi, 0.32, 52.0, 130.0), (-3.2, 0.5, 14.0, 0.7, 0.22, 46.0, 120.0), (3.2, 0.5, 14.0, 2.4, 0.22, 46.0, 120.0)],
             core_rgb=(190, 235, 255))
    core_glow(atlas, m, ["frost_ribs", "frost_shards", "heart_halo", "guard_gems", "pommel_gems", "orbit_shard"], (170, 225, 255), 0.7, 0.55)
    vein_glow(atlas, g2, m, ["frost_ribs", "heart_halo", "ice_core", "heart_crystal", "guard_gems", "frost_shards"], (120, 205, 255), 0.95, 0.5)
    lib.paint_g2(atlas, g2, blade, lambda pc: pc.face and nl.put_glow(pc, nl.sm(pc.X - 0.0, 0.42) * (pc.Y > 50) * (pc.Y < 150), (110, 190, 255), 0.7, 0.5))


SPEC_RIMEHEART = dict(
    palette=dict(core="9FE3FF", accent="E8F8FF", vein="78CDFF", halo="BFE6F5"),
    veins=dict(speed=0.30, wavelength=44.0, base=0.18, peak=1.0), halo=dict(scale=1.075, alpha=64, breath=0.03, speed=0.05),
    orbiters=[dict(count=6, radius=9.5, y_frac=0.80, speed=0.040, spin=0.11, bob=2.2, bob_speed=0.05, tilt=0.30, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="BFE6F5", ghosts=3, lag_deg=8.0, alpha=84),
    particles=layers(motes=dict(particle="minecraft:snowflake", rgb="E8F8FF", rate=1.1, speed=0.010), halo_ring=dict(rgb="9FE3FF"), edge_line=dict(particle="minecraft:snowflake", rgb="CFF0FF", rate=0.9),
                     ground_cast=dict(rgb="9FE3FF"), wrist_flare=dict(particle="minecraft:end_rod", rgb="E8F8FF"), light_shaft=dict(rgb="BFE6F5"), accent_flare=dict(particle="minecraft:end_rod", rate=0.12)),
    ground_radius=1.15, shaft_height=1.9,
)


# ================================================================ Deepwatch Harpoon
def geo_deepwatch(m):
    Y_S = 172.0
    ays = [0.0, 0.08, 0.2, 0.3, 0.55, 0.75, 0.92, 1.0]
    aws = [0.18, 0.5, 0.88, 1.0, 0.84, 0.55, 0.2, 0.0]
    teeth = []
    for k in range(7):
        a = 0.32 + 0.08 * k
        hw = 4.4 * float(np.interp(a, ays, aws))
        y = Y_S + 6.0 + 46.0 * a
        for s_ in (-1, 1):
            teeth.append(((s_ * hw * 0.93, y, 0.0), (s_ * (hw + 1.5), y + 3.2, 0.0)))
    spike_row(m, "head_teeth", "steel", (190, 240, 246), teeth, rad=0.5, sides=3)
    spike_row(m, "coral_crown", "bone", (120, 214, 220), [((math.cos(a) * 3.2, Y_S - 4.0, math.sin(a) * 3.2), (math.cos(a) * 5.4, Y_S + 6.5 + (i % 2) * 3.0, math.sin(a) * 5.4)) for i, a in enumerate(np.linspace(0, 2 * math.pi, 9)[:-1])], rad=0.85, sides=4)
    flame_ribbons(m, "kelp_ribbons", "cloth", (46, 130, 120), (0.0, Y_S - 8.0, 2.0), [(0.15, -1.0, 0.1), (-0.25, -1.0, -0.05)], 26.0, 1.5, n=8, sway=3.0, up=(0, 0, 1))
    for y in (40.0, 98.0, 146.0):
        pr = P(m, "scale_band_%d" % int(y), "silver", base=(176, 226, 236), smooth=40, uv_mode="loft")
        ml.lathe(pr, "y", (0, y - 2.2, 0), [(0.0, 1.85), (0.7, 2.35), (1.5, 2.55), (2.3, 2.35), (3.0, 1.85)], sides=10)
    bub = P(m, "bubble_pearls", "crystal", base=(224, 250, 255), smooth=50)
    for k, y in enumerate((96.0, 110.0, 126.0, 142.0)):
        a = k * 1.7
        lib.w.ellipsoid(bub, (2.9 * math.cos(a), y, 2.9 * math.sin(a)), 0.7 + 0.1 * (k % 2), 0.7 + 0.1 * (k % 2), 0.7, sides=8, stacks=5)
    fl = P(m, "anchor_flukes", "iron", base=(70, 112, 124), smooth=36)
    for sx in (-1, 1):
        ml.sweep(fl, [(sx * (1.6 + 5.4 * math.sin(a)), -5.0 - 4.0 * (1 - math.cos(a)) * 0.0 - 5.0 * math.sin(a * 0.9) * 0 - 3.5 * (1 - math.cos(a)) + 2.0 * math.sin(a), 0.0) for a in np.linspace(0.0, 1.6, 7)], ml.bevelled(2.0, 2.0, 0.5),
                 scales=lambda t: (0.9 * (1 - 0.7 * t) + 0.2, 0.6 * (1 - 0.6 * t) + 0.15), up=(0, 0, 1))
    o = P(m, "orbit_bubble", "crystal", base=(190, 244, 252), smooth=45)
    lib.w.ellipsoid(o, (0, 0, 0), 0.8, 1.0, 0.8, sides=6, stacks=4)
    ml.gem(o, (0.0, 1.3, 0.0), 0.28, 1.4, 4)


def paint_deepwatch(m, atlas, g2):
    head = m.part("head")
    vines_on(atlas, g2, head, (110, 240, 250), [(0.0, 1.3, 14.0, 0.0, 0.26, 182.0, 222.0), (0.0, 1.3, 14.0, math.pi, 0.26, 182.0, 222.0)], core_rgb=(190, 250, 255))
    core_glow(atlas, m, ["bubble_pearls", "coral_crown", "orbit_bubble", "head_teeth"], (150, 245, 250), 0.75, 0.55)
    vein_glow(atlas, g2, m, ["brine_line", "bubble_pearls", "pearl", "coral_crown", "head_teeth", "scale_band_40", "scale_band_98", "scale_band_146"], (90, 235, 245), 0.95, 0.5)

    def tide(pc):                                                       # ring bands of light on the shaft
        v = pc.B if not pc.face else pc.Y
        nl.put_glow(pc, np.clip(np.sin(v * 0.42) - 0.72, 0, 1) * 2.4, (80, 220, 240), 0.55, 0.4)
    lib.paint_g2(atlas, g2, m.part("shaft"), tide)


SPEC_DEEPWATCH = dict(
    palette=dict(core="6AF0FF", accent="D8F4FF", vein="52E0F0", halo="2A7A8A"),
    veins=dict(speed=0.42, wavelength=36.0, base=0.15, peak=1.0), halo=dict(scale=1.09, alpha=58, breath=0.04, speed=0.045),
    orbiters=[dict(count=6, radius=7.0, y_frac=0.86, speed=0.050, spin=0.06, bob=3.0, bob_speed=0.07, tilt=0.12, phase=0.4, scale=1.0)],
    swing_trail=dict(rgb="6AF0FF", ghosts=3, lag_deg=7.0, alpha=80),
    particles=layers(motes=dict(particle="minecraft:bubble", rgb="D8F4FF", rate=1.2, speed=0.03), halo_ring=dict(rgb="6AF0FF"), edge_line=dict(particle="minecraft:bubble_pop", rgb="D8F4FF", rate=0.7),
                     ground_cast=dict(rgb="2A9AAA"), wrist_flare=dict(particle="minecraft:nautilus", rgb="9AF4FF", rate=0.5), light_shaft=dict(rgb="6AF0FF"), accent_flare=dict(particle="minecraft:dolphin", rate=0.10)),
    ground_radius=1.2, shaft_height=2.2,
)


# ================================================================ Sanguine Warden's Cleaver
def geo_sanguine(m):
    L0 = 22.0
    chain(m, "pommel_chain", "iron", (96, 84, 92), (0.0, -2.4, 0.0), (3.0, -22.0, 0.0), 5, 1.1, 0.26, sag=1.6)
    st = P(m, "spine_rivets", "iron", base=(150, 134, 120), smooth=40)
    for k in range(7):
        y = L0 + 10.0 + k * 4.0
        for sgn in (1, -1):
            ml.stud(st, (5.0, y, sgn * 1.2), 0.65, 0.5, (0, 0, sgn), 6, 0.55)
    tal = P(m, "tally_marks", "bone", base=(196, 182, 160), smooth=30)
    for g in range(2):
        for k in range(5):
            x = -8.6 + g * 5.2 + k * 0.75
            y = L0 + 45.5
            if k == 4:
                ml.bar(tal, (x - 3.8, y - 1.0, 1.27), (x + 0.2, y + 4.6, 1.27), 0.2, 0.09, up=(0, 0, 1), taper=0.0)
            else:
                ml.bar(tal, (x, y, 1.27), (x, y + 5.0, 1.27), 0.2, 0.09, up=(0, 0, 1), taper=0.0)
    rune = P(m, "blood_vessels", "crystal", base=(210, 30, 40), smooth=35)
    for (x0, y0, x1, y1, ph) in ((-3.0, L0 + 14.0, 3.0, L0 + 36.0, 0.0), (0.0, L0 + 14.0, 4.0, L0 + 30.0, 1.2), (-8.0, L0 + 16.0, -5.0, L0 + 36.0, 2.0)):
        for sgn in (1, -1):
            ml.sweep(rune, lib.vine_pts(x0, x1, y0, y1, 1.2, 1.5, z=sgn * 1.27, n=7, phase=ph), ml.bevelled(2.0, 2.0, 0.45), scales=lambda t: (0.22 * (1.0 - 0.5 * t) + 0.05, 0.09), up=(0, 0, sgn))
    ey = [L0 + 1.0, L0 + 8.0, L0 + 30.0, L0 + 52.0, L0 + 60.0]
    ex = [-9.0, -14.5, -15.5, -15.0, -12.5]
    sers = []
    for k in range(15):
        y = L0 + 10.0 + 3.1 * k
        x = float(np.interp(y, ey, ex))
        sers.append(((x + 0.25, y, 0.0), (x - 1.5, y + 1.0, 0.0)))
    spike_row(m, "edge_serration", "iron", (150, 28, 34), sers, rad=0.55, sides=3)
    spike_row(m, "grip_spurs", "bone", (226, 218, 196), [((math.cos(a) * 1.9, 4.0 + 4.2 * i, math.sin(a) * 1.9), (math.cos(a) * 3.6, 5.2 + 4.2 * i, math.sin(a) * 3.6)) for i, a in enumerate(np.linspace(0, 6.3 * 2, 5))], rad=0.4, sides=4)
    o = P(m, "orbit_droplet", "crystal", base=(210, 20, 34), smooth=45)
    ml.lathe(o, "y", (0, 0, 0), [(-1.2, 0.0), (-0.2, 0.7), (0.7, 0.55), (1.8, 0.0)], sides=6)


def paint_sanguine(m, atlas, g2):
    blade = m.part("blade")

    def lines(pc):
        if not pc.face:
            return
        mk = lib.ring_mask(pc.X, pc.Y, 5.2, 22.0 + 42.0, 3.2, 0.35) * 0.7
        lib.engrave(pc, mk, 0.5)
        nl.put_glow(pc, mk, (255, 50, 56), 0.5, 0.4)
    nl.paint(atlas, blade, lines)

    def fn2(pc):
        if not pc.face:
            return
        mk = np.maximum(lib.ring_mask(pc.X, pc.Y, 5.2, 64.0, 3.2, 0.4), nl.sm(pc.X + 15.0, 0.9) * (pc.Y > 28) * (pc.Y < 82))       # the hole's rim and the cutting edge
        nl.put_glow(pc, mk, (255, 50, 60), 0.9, 0.45)
    lib.paint_g2(atlas, g2, blade, fn2)
    core_glow(atlas, m, ["blood_vessels", "orbit_droplet", "edge_serration"], (255, 70, 70), 0.9, 0.5)
    core_glow(atlas, m, ["tally_marks"], (255, 150, 110), 0.35, 0.3)
    vein_glow(atlas, g2, m, ["blood_vessels", "blood_runnels", "edge_serration", "notches", "tally_marks", "spine_hole_rim"], (255, 60, 70), 0.95, 0.5)


SPEC_SANGUINE = dict(
    palette=dict(core="FF3A3A", accent="FFB0A0", vein="FF2A3A", halo="C01818"),
    veins=dict(speed=0.26, wavelength=30.0, base=0.12, peak=1.0), halo=dict(scale=1.06, alpha=60, breath=0.05, speed=0.07),
    orbiters=[dict(count=7, radius=11.0, y_frac=0.50, speed=0.034, spin=0.07, bob=2.0, bob_speed=0.06, tilt=0.5, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="FF2A3A", ghosts=3, lag_deg=9.0, alpha=96),
    particles=layers(motes=dict(particle="dust", rgb="C01818", rate=1.2, speed=0.010, size=1.0), halo_ring=dict(rgb="FF3A3A"), edge_line=dict(particle="dust", rgb="FF5A5A", rate=1.0, size=0.8),
                     ground_cast=dict(rgb="8A1010"), wrist_flare=dict(particle="minecraft:crimson_spore", rgb="FF3A3A", rate=0.6), light_shaft=dict(rgb="C01818"), accent_flare=dict(particle="minecraft:witch", rate=0.12)),
    ground_radius=1.1, shaft_height=1.6,
)


# ================================================================ Valkarin's Stormlance
def geo_valkarin(m):
    Y_S = 178.0
    coil(m, "coil_counter", "crystal", (170, 214, 255), 30.0, Y_S - 14.0, 2.6, -5.0, 0.28, n=40, sides=4, phase=1.0)
    flame_ribbons(m, "wind_pennants", "cloth", (96, 130, 210), (1.5, Y_S - 38.0, 1.5), [(1.0, -0.55, 0.1), (0.9, -0.85, -0.25)], 34.0, 2.2, n=9, sway=4.0, up=(0, 0, 1))
    for k in range(3):
        a = 2 * math.pi * k / 3 + math.pi / 3
        pf = P(m, "tail_fin_%d" % k, "silver", base=(200, 214, 244), smooth=38)
        ml.sweep(pf, [(math.cos(a) * (2.2 + 4.0 * t), 6.0 + 20.0 * t ** 1.2, math.sin(a) * (2.2 + 4.0 * t)) for t in np.linspace(0, 1, 6)], ml.bevelled(2.0, 2.0, 0.5),
                 scales=lambda t: (0.6 * (1 - 0.8 * t) + 0.12, 1.4 * (1 - 0.7 * t) + 0.15), up=(0, 1, 0))
    for y in (Y_S + 14.0, Y_S + 26.0, Y_S + 38.0):
        pr = P(m, "head_ring_%d" % int(y - Y_S), "silver", base=(230, 238, 252), smooth=40, uv_mode="loft")
        r = 3.9 - 0.05 * (y - Y_S - 14.0)
        ml.lathe(pr, "y", (0, y - 0.9, 0), [(0.0, r), (0.4, r + 0.5), (1.4, r + 0.5), (1.8, r)], sides=10)
    lib.tube_ring(m, "gem_halo", "crystal", 0.0, Y_S - 24.0, 7.0, 3.3, 0.24, n=16, plane="xy", base=(150, 200, 255), sides=5)
    rn = P(m, "storm_runes", "silver", base=(190, 210, 255), smooth=30)
    for k in range(10):
        a = 2 * math.pi * k / 10
        ml.bar(rn, (7.0 * math.cos(a), Y_S - 14.0 + (k % 2) * 1.6, 7.0 * math.sin(a)), (7.4 * math.cos(a), Y_S - 11.0 + (k % 2) * 1.6, 7.4 * math.sin(a)), 0.4, 0.18, up=(math.cos(a), 0, math.sin(a)), taper=0.0)
    o = P(m, "orbit_bolt", "crystal", base=(160, 210, 255), smooth=30)
    pts = [(0.0, -3.0, 0.0), (1.0, -1.0, 0.0), (-0.9, 0.2, 0.0), (0.8, 1.6, 0.0), (0.0, 3.2, 0.0)]
    ml.sweep(o, pts, ml.circle(0.32, 4), scales=lambda t: (1.0 - 0.6 * abs(2 * t - 1) ** 2 + 0.2, 1.0 - 0.6 * abs(2 * t - 1) ** 2 + 0.2))


def paint_valkarin(m, atlas, g2):
    core_glow(atlas, m, ["coil_counter", "gem_halo", "storm_runes", "orbit_bolt"], (150, 200, 255), 0.9, 0.55)
    vein_glow(atlas, g2, m, ["coil", "coil_counter", "gem_halo", "storm_gem", "flutes", "storm_runes"], (120, 190, 255), 0.95, 0.5)
    vein_glow(atlas, g2, m, ["head", "vamplate"], (150, 200, 255), 0.2, 0.3)

    def bolts(pc):                                                      # forked lightning scratched into the head
        v = pc.B if not pc.face else pc.Y
        mk = np.clip(np.sin(v * 0.9 + pc.A * 1.7) - 0.86, 0, 1) * 6.0
        nl.put_glow(pc, mk, (150, 205, 255), 0.7, 0.5)
    lib.paint_g2(atlas, g2, m.part("head"), bolts)


SPEC_VALKARIN = dict(
    palette=dict(core="9ACBFF", accent="F0F6FF", vein="6FB4FF", halo="34406A"),
    veins=dict(speed=0.55, wavelength=28.0, base=0.10, peak=1.0), halo=dict(scale=1.06, alpha=56, breath=0.06, speed=0.11),
    orbiters=[dict(count=5, radius=8.5, y_frac=0.80, speed=0.070, spin=0.16, bob=1.6, bob_speed=0.09, tilt=0.20, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="9ACBFF", ghosts=3, lag_deg=10.0, alpha=92),
    particles=layers(motes=dict(particle="minecraft:electric_spark", rgb="BFE0FF", rate=0.9, speed=0.02), halo_ring=dict(rgb="9ACBFF", rate=1.6), edge_line=dict(particle="minecraft:electric_spark", rate=1.0),
                     ground_cast=dict(rgb="5A78C0"), wrist_flare=dict(particle="minecraft:glow", rgb="BFE0FF"), light_shaft=dict(rgb="9ACBFF"), accent_flare=dict(particle="minecraft:firework", rate=0.12)),
    ground_radius=1.2, shaft_height=2.3,
)


# ================================================================ Oriel's Starfall Staff
def geo_orial(m):
    Y1 = 200.0
    st = P(m, "star_studs", "crystal", base=(255, 232, 160), smooth=35)
    pts = []
    for k in range(9):
        a = k * 1.15
        y = 30.0 + k * 19.0
        pt = (2.3 * math.cos(a), y, 2.3 * math.sin(a))
        pts.append(pt)
        ml.gem(st, pt, 0.55, 1.3, 4, "y")
    lib.tube_ring(m, "orrery_ring_a", "silver", 0.0, Y1 + 20.0, 0.0, 15.5, 0.26, n=22, plane="xy", base=(220, 214, 250), sides=5)
    lib.tube_ring(m, "orrery_ring_b", "silver", 0.0, Y1 + 20.0, 0.0, 12.5, 0.24, n=20, plane="yz", base=(220, 214, 250), sides=5)
    spike_row(m, "cradle_thorns", "silver", (222, 216, 250), [((14.8 * math.sin(a), Y1 + 14.0 + 12.0 * (1 - math.cos(a)), 0.0), (17.4 * math.sin(a), Y1 + 17.0 + 12.0 * (1 - math.cos(a)), 0.0)) for a in np.linspace(-1.8, 1.8, 9)], rad=0.6, sides=4)
    for y in (36.0, 92.0, 150.0, 186.0):
        pr = P(m, "inlay_band_%d" % int(y), "silver", base=(206, 200, 244), smooth=40, uv_mode="loft")
        ml.lathe(pr, "y", (0, y - 1.5, 0), [(0.0, 1.75), (0.5, 2.15), (2.5, 2.15), (3.0, 1.75)], sides=10)
    flame_ribbons(m, "comet_tail", "cloth", (150, 128, 255), (0.0, Y1 + 18.0, -1.0), [(0.25, 1.0, 0.0), (-0.3, 1.0, 0.0), (0.0, 1.0, 0.3)], 32.0, 1.3, n=8, sway=2.4, up=(0, 0, 1))
    spike_row(m, "butt_star", "silver", (214, 208, 244), [((0.0, -3.0, 0.0), (3.4 * math.cos(a), -9.0, 3.4 * math.sin(a))) for a in np.linspace(0, 2 * math.pi, 6)[:-1]], rad=0.7, sides=4)
    o = P(m, "orbit_star", "crystal", base=(255, 236, 170), smooth=25)
    kit.star4(o, (0.0, 0.0, 0.0), 2.6, 0.55, 0.3)
    ml.gem(o, (0.0, 0.0, 0.0), 0.55, 1.2, 4, "z")


def paint_orial(m, atlas, g2):
    core_glow(atlas, m, ["star_studs", "orrery_ring_a", "orrery_ring_b", "comet_tail", "orbit_star", "cradle_thorns"], (230, 214, 255), 0.8, 0.55)
    vein_glow(atlas, g2, m, ["star_studs", "orrery_ring_a", "orrery_ring_b", "comet_tail", "star_script", "eye_gems", "falling_star", "star_tail", "cradle_thorns", "inlay_band_36", "inlay_band_92", "inlay_band_150", "inlay_band_186"],
              (190, 170, 255), 0.95, 0.5)

    def script(pc):                                                     # constellation lines wound round the shaft
        v = pc.B if not pc.face else pc.Y
        mk = np.clip(1.0 - np.abs(((v * 0.31 + pc.A * 2.2) % 6.28) - 3.14) / 0.35, 0, 1) * (np.sin(v * 0.17) > 0.0)
        nl.put_glow(pc, mk, (200, 180, 255), 0.7, 0.5)
    lib.paint_g2(atlas, g2, m.part("shaft"), script)


SPEC_ORIAL = dict(
    palette=dict(core="B9A3FF", accent="FFE8A0", vein="A98BFF", halo="2A1E5A"),
    veins=dict(speed=0.36, wavelength=52.0, base=0.15, peak=1.0), halo=dict(scale=1.07, alpha=60, breath=0.04, speed=0.05),
    orbiters=[dict(count=6, radius=17.0, y_frac=0.965, speed=0.032, spin=0.09, bob=2.5, bob_speed=0.05, tilt=0.45, phase=0.0, scale=1.0)],
    swing_trail=dict(rgb="B9A3FF", ghosts=3, lag_deg=8.0, alpha=84),
    particles=layers(motes=dict(particle="minecraft:end_rod", rgb="FFE8A0", rate=1.2, speed=0.010), halo_ring=dict(rgb="B9A3FF"), edge_line=dict(particle="minecraft:enchant", rgb="D8CCFF", rate=1.4),
                     ground_cast=dict(rgb="6A4ACF"), wrist_flare=dict(particle="minecraft:glow", rgb="D8CCFF"), light_shaft=dict(rgb="FFE8A0"), accent_flare=dict(particle="minecraft:firework", rate=0.13)),
    ground_radius=1.25, shaft_height=2.6,
)

ITEMS_A = [
    dict(id="rimeheart_greatblade", geometry=geo_rimeheart, paint=paint_rimeheart, spec=SPEC_RIMEHEART),
    dict(id="deepwatch_harpoon", geometry=geo_deepwatch, paint=paint_deepwatch, spec=SPEC_DEEPWATCH),
    dict(id="sanguine_wardens_cleaver", geometry=geo_sanguine, paint=paint_sanguine, spec=SPEC_SANGUINE),
    dict(id="valkarins_stormlance", geometry=geo_valkarin, paint=paint_valkarin, spec=SPEC_VALKARIN),
    dict(id="orials_starfall_staff", geometry=geo_orial, paint=paint_orial, spec=SPEC_ORIAL),
]

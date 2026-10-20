"""The Mind Stone (a lotus of gold claws round a tall thinking crystal), the Power Stone (a burst of crystal in an iron war cage), the Time Stone (the Eye of Agamotto)."""
import math

import numpy as np

import geo
import kitlib as K
import paint as PT
from spec import Item, geo_layer as GL, part_layer as PL, step as ST
from vu import MAIN, TRIM, GLOW, DARK, ACCENT

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


# ====================================================================== MIND
def build_mind(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_gem = vm.grp("gem", "bob", (0, 0, 0), 0.3, 0.06, 0.0)
    g_arc_a = vm.grp("thought_arcs", "tumble", (0, 2, 0), 0.0, 0.0, 1.6)
    g_arc_b = vm.grp("thought_arcs_b", "tumble", (0, 2, 0), 1.1, 0.0, -0.8)
    g_beads = vm.grp("memory_beads", "spin", (0, 2, 0), 3.4)
    g_core = vm.grp("gem_core", "pulse", (0, 2, 0), 0.05, 0.12, 0.0)
    # the gem: a tall crystal of thought, drawn as the nebula, in a gold lotus of claws
    gem = vm.star("gem", g_core)
    geo.merge(gem, geo.prim_crystal(2.7, 14.5, sides=6, tip=0.42), None, (0, 2, 0))
    ridges = vm.P("gem_ridges", "gold", group=g_gem, base=(255, 224, 120), slot=TRIM, lod=False)
    for i in range(6):
        a = math.pi / 6 + math.pi / 3 * i
        c, s_ = math.cos(a), math.sin(a)
        geo.tube_path(ridges, [(0.5 * c, -4.8, 0.5 * s_), (2.95 * c, -2.5, 2.95 * s_), (2.95 * c, 4.2, 2.95 * s_), (0.35 * c, 8.9, 0.35 * s_)], 0.22, sides=3, caps=(False, False))
    for yy in (-1.8, 5.2):
        geo.merge(ridges, geo.prim_torus(2.95, 0.26, n=vm.S(12, 6), m=3), None, (0, yy, 0))
    K.claws(vm, "gem_claws", g_gem, 6, 3.2, -4.6, 7.4, 0.5, "gold", (240, 205, 90), curl=0.18, segs=3, sides=3, phase=0.0, lod=False)
    collar = vm.P("collar", "gold", group=g_gem, base=(235, 200, 80), slot=TRIM, lod=True)
    geo.lathe(collar, "y", (0, 0, 0), [(-6.6, 1.4), (-5.6, 2.9), (-4.6, 3.5), (-4.0, 2.2), (-3.4, 1.2)], sides=vm.S(12, 6), caps=(True, False))
    stem = vm.P("sceptre", "bronze", group=g_gem, base=(200, 160, 70), lod=True)
    geo.lathe(stem, "y", (0, 0, 0), [(-17.0, 0.9), (-16.0, 1.5), (-14.5, 0.9), (-9.0, 1.1), (-8.0, 1.8), (-7.0, 1.0), (-6.4, 1.2)], sides=vm.S(8, 4), caps=(True, False))
    wire = vm.P("sceptre_wire", "gold", group=g_gem, base=(255, 230, 130), slot=TRIM)
    geo.spiral_tube(wire, (0, -12, 0), 1.25, 8.0, 3.0, 0.24, n=vm.S(18, 6), sides=3)
    # thought arcs with memory beads
    K.gyro(vm, "arc_a", g_arc_a, 7.2, 0.28, 30, "gold", (255, 224, 120), tilt_axis="z", n=18, m=3, center=(0, 2, 0))
    K.gyro(vm, "arc_b", g_arc_b, 8.4, 0.24, -50, "gold", (255, 236, 150), tilt_axis="x", n=14, m=3, center=(0, 2, 0))
    beads = K.orbs(vm, "beads", g_beads, 6, 7.0, 2.0, 0.65, "crystal", (255, 224, 90), slot=GLOW, phase=0.3)
    K.halo_set(vm, pal, y=2.0, R=8.0, shaft_len=20.0)
    vm.deco(PT.deco_glyphs, collar, glow, text="MIND", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_filigree, ridges, trim, seed=4)
    vm.deco(PT.deco_fill_glow, beads, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_glyphs, stem, glow, rows=1, seed=6, glow=False)
    vm.deco(PT.deco_grain, stem, amount=0.1)


add(Item("the_mind_stone", build_mind, dict(style="swirl", dark=(24, 18, 0), mid=(140, 105, 6), hot=(255, 235, 120)), hand_size=0.8, hand_style="tool", seed=12,
         layers=[GL("thought crystal", "nebula_core", "gem_core"), GL("thought arc a", "halo_ring", "thought_arcs"), GL("thought arc b", "halo_ring", "thought_arcs_b"),
                 GL("memory beads", "orbit_shards", "memory_beads"), GL("mind rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("thought motes", "motes", "end_rod", None, count=1, every=4, radius=0.5, rise=0.014),
                 PL("whisper glints", "glints", "glow", None, count=1, every=7, radius=0.55, rise=0.006)],
         acts={"sense_minds": [ST("DIVINE_HALO", 0xFFD93A, 2.4, 50, "self"), ST("MAGIC_CIRCLE", 0xFFF0A0, 3.2, 40, "ground")],
               "read_mind": [ST("MANA_CHARGE", 0xFFD93A, 1.6, 30, "front"), ST("LIGHT_FLARE", 0xFFF0A0, 1.2, 16, "front")]}))


# ====================================================================== POWER
def build_power(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_core = vm.grp("power_core", "pulse", (0, 0, 0), 0.10, 0.22, 0.0)
    g_burst = vm.grp("shatter_crown", "pulse", (0, 0, 0), 0.04, 0.16, 1.0)
    g_chunks = vm.grp("rock_chunks", "tumble", (0, 0, 0), 0.6, 0.9, 1.4)
    g_chunks2 = vm.grp("rock_chunks_far", "spin", (0, 0, 0), -2.2)
    g_arcs = vm.grp("lightning", "pulse", (0, 0, 0), 0.25, 0.5, 2.0)
    g_cage = vm.grp("war_cage", "sway", (0, 0, 0), 2.0, 0.05, 0.0)
    # a jagged heart of raw power: a faceted ovoid in the nebula pass, a crown of crystal spikes, a war-cage of iron claws
    core = vm.star("core", g_core)
    geo.merge(core, geo.prim_octa(4.6, 6.0), None, (0, 0, 0))
    geo.merge(core, geo.prim_octa(3.2, 4.0), geo.rot_matrix("y", 45) @ geo.rot_matrix("z", 30), (0, 0, 0))
    spikes = vm.P("crystal_spikes", "crystal", group=g_burst, base=(170, 90, 255), slot=GLOW, lod=False)
    sp = geo.prim_crystal(1.1, 6.5, sides=4, tip=0.5)
    rng = np.random.default_rng(5)
    for i in range(10):
        th = 2 * math.pi * i / 10 + rng.uniform(-0.2, 0.2)
        el = math.radians(rng.uniform(-50, 55))
        d = np.array([math.cos(th) * math.cos(el), math.sin(el), math.sin(th) * math.cos(el)])
        geo.place(spikes, sp, d * 6.2, up=d, scale=rng.uniform(0.7, 1.25))
    cage = vm.P("war_cage", "iron", group=g_cage, base=(86, 60, 120), lod=False)
    for i in range(4):
        th = math.pi / 4 + math.pi / 2 * i
        c, s_ = math.cos(th), math.sin(th)
        pts = [(2.2 * c, -9.0, 2.2 * s_), (4.4 * c, -6.0, 4.4 * s_), (7.8 * c, -1.0, 7.8 * s_), (7.6 * c, 4.6, 7.6 * s_), (4.4 * c, 8.2, 4.4 * s_), (1.0 * c, 9.6, 1.0 * s_)]
        ns = vm.S(5, 3)
        geo.sweep(cage, geo.bez3(pts[0], pts[2], pts[3], pts[5], ns), geo.circle(0.62, 4), scales=[(1 - 0.7 * k / ns,) * 2 for k in range(ns + 1)], caps=(True, True))
    base = vm.P("war_base", "iron", group=g_cage, base=(70, 50, 100), lod=True)
    geo.lathe(base, "y", (0, 0, 0), [(-12.0, 2.0), (-11.0, 3.8), (-9.0, 3.0), (-7.5, 1.6), (-6.4, 1.4)], sides=vm.S(8, 4), caps=(True, False))
    rim = vm.P("war_rim", "gold", group=g_cage, base=(190, 150, 230), slot=TRIM)
    geo.merge(rim, geo.prim_torus(3.1, 0.32, n=vm.S(14, 6), m=3), None, (0, -9.4, 0))
    chunks = vm.P("rock_chunks", "stone", group=g_chunks, base=(96, 70, 130), lod=False)
    ch = geo.prim_octa(1.1, 1.0)
    for i in range(5):
        th = 2 * math.pi * i / 5
        geo.merge(chunks, ch, geo.rot_matrix("y", 40 * i) @ geo.rot_matrix("x", 25 * i), (9.0 * math.cos(th), 3.0 * math.sin(i * 2.1), 9.0 * math.sin(th)))
    chunks2 = vm.P("rock_chunks_far", "obsidian", group=g_chunks2, base=(58, 30, 90))
    ch2 = geo.prim_octa(0.7, 0.8)
    for i in range(6):
        th = 2 * math.pi * i / 6 + 0.4
        geo.merge(chunks2, ch2, geo.rot_matrix("y", 50 * i), (12.0 * math.cos(th), -4 + 2 * math.sin(i * 1.7), 12.0 * math.sin(th)))
    # lightning arcs (fx bolts), a shock ripple and a flare
    for i in range(4):
        a = math.radians(45 + 90 * i)
        vm.fx_disc(g_arcs, ACCENT, (0, 0, 0), (math.cos(a), 0, math.sin(a)), 17.0, "bolt", rot=20 * i)
    g_rip = vm.grp("shock_ripple", "pulse", (0, -8, 0), 0.3, 0.12, 0.0)
    vm.fx_disc(g_rip, DARK, (0, -10.0, 0), (0, 1, 0), 22.0, "ripple")
    K.halo_set(vm, pal, y=0.0, R=9.5, rune=False, shaft_len=18.0)
    vm.deco(PT.deco_fill_glow, spikes, glow, strength=0.95, seed=3)
    vm.deco(PT.deco_glyphs, base, glow, text="POWER", rows=1, glow=False, seed=7)
    vm.deco(PT.deco_filigree, rim, trim, seed=8)
    vm.deco(PT.deco_grain, chunks, amount=0.2)
    vm.deco(PT.deco_veins, chunks, glow, n=2, strength=0.8, seed=9, min_w=6, min_h=6)


add(Item("the_power_stone", build_power, dict(style="lava", dark=(18, 0, 30), mid=(110, 30, 190), hot=(225, 160, 255)), hand_size=0.72, hand_style="palm", seed=13,
         layers=[GL("raw power core", "nebula_core", "power_core"), GL("crystal burst", "aura_shell", "shatter_crown"), GL("rock chunks", "orbit_shards", "rock_chunks"),
                 GL("far debris", "orbit_shards", "rock_chunks_far"), GL("lightning arcs", "energy_arcs", "lightning"), GL("shock ripple", "portal_ripples", "shock_ripple"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("power embers", "embers", "dust", 0xA04AF0, count=2, every=3, radius=0.5, rise=0.02),
                 PL("arc sparks", "arcs", "electric_spark", None, count=1, every=6, radius=0.6, rise=0.0)],
         acts={"power_burst": [ST("MAGIC_CIRCLE_EXPLOSION", 0xA04AF0, 4.0, 40, "ground"), ST("LIGHTNING_FIEND", 0xD0A0FF, 3.0, 36, "self"), ST("LIGHT_FLARE", 0xFFFFFF, 2.0, 12, "self")]}))


# ====================================================================== TIME (the Eye of Agamotto)
def _face_xy(part):
    """Turns a part built round the y axis so that its axis points along z (it lies in the xy plane, facing the viewer)."""
    part.V = [geo.rot_matrix("x", 90) @ v for v in part.V]
    part.UV = part.N = None


def build_time(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_eye = vm.grp("eye", "pulse", (0, 0, 0), 0.04, 0.10, 0.0)
    g_dial_a = vm.grp("dial_outer_turn", "sway", (0, 0, 0), 1.5, 0.04, 0.0)
    g_dial_b = vm.grp("dial_mid", "sway", (0, 0, 0), -2.2, 0.05, 1.0)
    g_dial_c = vm.grp("dial_inner", "sway", (0, 0, 0), 3.0, 0.07, 2.0)
    g_lid_u = vm.grp("eyelid_upper", "bob", (0, 0, 0), 0.5, 0.06, 0.0)
    g_lid_l = vm.grp("eyelid_lower", "bob", (0, 0, 0), -0.5, 0.06, 1.0)
    g_rays = vm.grp("sun_rays", "pulse", (0, 0, 0), 0.05, 0.1, 0.0)
    g_swing = vm.grp("pendant", "sway", (0, 15, 0), 2.0, 0.05, 0.0)
    # the amulet is authored in the xy plane facing -z, hanging from the top: the eye
    iris = vm.star("iris", g_eye)
    ir = geo.prim_lathe([(-0.7, 3.4), (0.0, 3.6), (0.9, 0.2)], sides=vm.S(10, 6), caps=(True, True))
    geo.merge(iris, ir, geo.rot_matrix("x", -90), (0, 0, -0.6))
    # three dial rings
    outer = vm.P("dial_outer", "bronze", group=g_dial_a, base=(176, 142, 74), lod=True)
    geo.lathe(outer, "y", (0, 0, 0), [(-0.7, 10.6), (0.9, 10.9), (0.9, 9.0), (-0.7, 8.8)], sides=vm.S(20, 8), caps=(False, False))
    _face_xy(outer)
    mid = vm.P("dial_mid", "gold", group=g_dial_b, base=(220, 190, 100), slot=TRIM)
    geo.lathe(mid, "y", (0, 0, 0), [(-0.5, 8.0), (0.6, 8.2), (0.6, 6.4), (-0.5, 6.2)], sides=vm.S(18, 8), caps=(False, False))
    _face_xy(mid)
    inner = vm.P("dial_inner", "bronze", group=g_dial_c, base=(160, 128, 66))
    geo.lathe(inner, "y", (0, 0, 0), [(-0.4, 5.6), (0.5, 5.8), (0.5, 4.4), (-0.4, 4.2)], sides=vm.S(14, 8), caps=(False, False))
    _face_xy(inner)
    teeth = vm.P("dial_teeth", "gold", group=g_dial_a, base=(196, 160, 86), slot=TRIM)
    tooth = geo.prim_octa(0.5, 0.7)
    nt = vm.S(12, 6)
    for i in range(nt):
        th = 2 * math.pi * i / nt
        geo.merge(teeth, tooth, geo.align_y((math.cos(th), math.sin(th), 0)), (11.4 * math.cos(th), 11.4 * math.sin(th), 0.1))
    # eyelids: two curved plates above and below the iris
    lid_u = vm.P("eyelid_upper", "gold", group=g_lid_u, base=(230, 196, 96), slot=TRIM, lod=False)
    lid_l = vm.P("eyelid_lower", "gold", group=g_lid_l, base=(230, 196, 96), slot=TRIM, lod=False)
    for p, sg in ((lid_u, 1), (lid_l, -1)):
        pts = [(-6.4, sg * 0.3, -0.9), (-3.0, sg * 3.1, -1.1), (0.0, sg * 3.9, -1.2), (3.0, sg * 3.1, -1.1), (6.4, sg * 0.3, -0.9)]
        geo.sweep(p, pts, geo.circle(0.55, 4), scales=[(0.5, 0.5), (1, 1), (1.1, 1.1), (1, 1), (0.5, 0.5)], caps=(True, True))
    # sun rays around the rim
    rays = vm.P("sun_rays", "gold", group=g_rays, base=(240, 205, 100), slot=TRIM, lod=False)
    ray = geo.prim_octa(0.55, 2.2)
    nr = vm.S(10, 6)
    for i in range(nr):
        th = 2 * math.pi * i / nr
        geo.merge(rays, ray, geo.align_y((math.cos(th), math.sin(th), 0)), (13.8 * math.cos(th), 13.8 * math.sin(th), 0))
    # the bail and chain
    bail = vm.P("bail", "gold", group=g_swing, base=(230, 196, 96), slot=TRIM, lod=True)
    geo.merge(bail, geo.prim_torus(1.2, 0.34, n=vm.S(10, 6), m=3), geo.rot_matrix("x", 90), (0, 16.2, 0))
    ch = vm.P("chain", "bronze", group=g_swing, base=(180, 146, 70), lod=False)
    geo.chain(ch, [(0, 17.4, 0), (0, 21, 0), (2.5, 24, 0)], link_r=0.8, wire=0.2, n=6, m=3, every=2.2)
    K.halo_set(vm, pal, y=0.0, R=10.0, rune=False, shaft_len=16.0, flare=True)
    g_ring = vm.grp("time_rings", "spin", (0, 0, 0), -0.9)
    vm.fx_disc(g_ring, GLOW, (0, 0, -1.4), (0, 0, 1), 26.0, "rune")
    vm.fx_disc(g_ring, DARK, (0, 0, -1.2), (0, 0, 1), 20.0, "ticks")
    vm.deco(PT.deco_glyphs, mid, glow, text="IXVIIIXII", rows=1, glow=True, strength=0.9, seed=3)
    vm.deco(PT.deco_glyphs, outer, glow, text="AGAMOTTO", rows=1, glow=False, seed=4)
    vm.deco(PT.deco_glyphs, inner, glow, rows=1, glow=False, seed=5)
    vm.deco(PT.deco_filigree, lid_u, trim, seed=6)
    vm.deco(PT.deco_filigree, lid_l, trim, seed=7)
    vm.deco(PT.deco_grain, outer, amount=0.12)


add(Item("the_time_stone_or_the_eye_of_agamotto", build_time, dict(style="swirl", dark=(0, 18, 6), mid=(20, 130, 70), hot=(150, 255, 190)), hand_size=0.74, hand_style="hang", seed=14,
         layers=[GL("eye of agamotto", "nebula_core", "eye"), GL("outer dial", "rune_ring", "dial_outer_turn"), GL("middle dial", "rune_ring", "dial_mid"), GL("inner dial", "rune_ring", "dial_inner"),
                 GL("eyelids", "aura_shell", "eyelid_upper"), GL("sun rays", "orbit_shards", "sun_rays"), GL("time rings", "rune_ring", "time_rings"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("future motes", "motes", "end_rod", None, count=1, every=5, radius=0.5, rise=0.008),
                 PL("green glints", "glints", "dust", 0x3AE07A, count=1, every=7, radius=0.55, rise=0.01)],
         acts={"foresight": [ST("DIVINE_HALO", 0x3AE07A, 3.0, 80, "self"), ST("STAR_MAGIC", 0xA0FFC0, 2.2, 70, "self"), ST("MAGIC_CIRCLE", 0x3AE07A, 3.4, 60, "ground")]}))

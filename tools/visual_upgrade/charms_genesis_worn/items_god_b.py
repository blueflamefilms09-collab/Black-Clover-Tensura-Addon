"""God grade charms, second half: Kamish's Fang, the Moon-Tower Black Sigil, the Orrery of Drowned Hours, the Pocket Watch of Return, Serie's Ancient Grimoire, the Vaultmaker's Pocketwatch."""
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


# ====================================================================== KAMISH'S FANG
def build_fang(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_fang = vm.grp("fang", "sway", (0, -12, 0), 1.6, 0.05, 0.0)
    g_heat = vm.grp("heart_fire", "pulse", (0, -2, 0), 0.10, 0.18, 0.0)
    g_scales = vm.grp("scale_shards", "spin", (0, -2, 0), 2.0)
    g_embers = vm.grp("rising_embers", "spin", (0, -2, 0), -1.6)
    g_ring = vm.grp("fire_ring", "pulse", (0, -12, 0), 0.2, 0.1, 0.0)
    g_cord = vm.grp("cord", "sway", (0, 12, 0), 4.0, 0.06, 0.0)
    # a great curved fang: ridged, ash-black at the root, white-hot at the point
    fang = vm.P("fang", "bone", group=g_fang, base=(46, 22, 20), lod=True)
    pts = geo.bez3((0, -13.0, 0), (1.0, -4.0, -1.2), (4.0, 5.0, -2.2), (4.8, 13.0, -0.4), vm.S(9, 4))
    n = len(pts)
    geo.sweep(fang, pts, geo.ellipse(3.0, 2.5, vm.S(8, 4)), scales=[((1.0 - 0.96 * (k / (n - 1)) ** 1.5),) * 2 for k in range(n)], caps=(True, True))
    ridges = vm.P("fang_ridges", "iron", group=g_fang, base=(90, 38, 30), slot=TRIM)
    for k in range(1, 4):
        t = 0.12 + 0.24 * k
        i = int(t * (n - 1))
        c = np.array(pts[i])
        d = np.array(pts[min(i + 1, n - 1)]) - np.array(pts[max(i - 1, 0)])
        rad = 3.0 * (1.0 - 0.96 * (i / (n - 1)) ** 1.5) * 1.06
        geo.merge(ridges, geo.prim_torus(rad, 0.3, n=vm.S(8, 6), m=3), geo.align_y(d), tuple(c))
    veins = vm.P("lava_scales", "obsidian", group=g_fang, base=(30, 12, 12), lod=False)
    for k in range(5):
        a = 2 * math.pi * k / 5
        geo.merge(veins, geo.prim_octa(0.9, 1.3), geo.align_y((math.cos(a) * 0.4, 1, math.sin(a) * 0.4)), (3.1 * math.cos(a), -10.0 + (k % 2) * 1.6, 2.6 * math.sin(a) - 0.3))
    cap = vm.P("root_cap", "iron", group=g_fang, base=(60, 30, 26), lod=False)
    geo.lathe(cap, "y", (0, 0, 0), [(-14.5, 2.6), (-13.5, 4.0), (-11.5, 4.1), (-10.0, 3.2)], sides=vm.S(8, 4), caps=(True, False))
    rim = vm.P("root_bands", "gold", group=g_fang, base=(230, 120, 60), slot=TRIM)
    for yy in (-13.6, -11.0):
        geo.lathe(rim, "y", (0, 0, 0), [(yy - 0.3, 4.15), (yy + 0.3, 4.15)], sides=vm.S(8, 4), caps=(False, False))
    # a cord with the bail, and fire burning at the root
    bail = vm.P("bail", "gold", group=g_cord, base=(240, 140, 70), slot=TRIM, lod=False)
    geo.merge(bail, geo.prim_torus(1.2, 0.34, n=vm.S(8, 6), m=3), geo.rot_matrix("x", 90), (0, 14.4, -0.4))
    cord = vm.P("cord", "leather", group=g_cord, base=(70, 36, 24), lod=False)
    geo.chain(cord, [(0, 15.6, -0.4), (0, 19, -0.4), (2.0, 22, -0.4)], link_r=0.7, wire=0.2, n=6, m=3, every=2.2)
    heart = vm.star("heart_fire", g_heat)
    geo.merge(heart, geo.prim_octa(2.4, 4.4), None, (0, -9.0, 0))
    # scale shards and embers circling
    sh = vm.P("scale_shards", "iron", group=g_scales, base=(120, 30, 28), lod=False)
    lf = geo.prim_leaf(3.0, 1.5, 0.3, sides=2, bend=0.03)
    for i in range(4):
        a = 2 * math.pi * i / 4
        geo.merge(sh, lf, geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 90), (8.0 * math.cos(a), -3.0 + 1.6 * math.sin(i * 2.0), 8.0 * math.sin(a)))
    em = K.orbs(vm, "rising_embers", g_embers, 6, 10.0, -1.0, 0.55, "crystal", (255, 150, 60), slot=GLOW, phase=0.4, wobble=4.0, lod=False)
    vm.fx_disc(g_ring, GLOW, (0, -12.0, 0), (0, 1, 0), 24.0, "ripple")
    vm.fx_cross(g_ring, DARK, (0, -3.0, 0), (0, 1, 0), 18.0, "smoke", n=2)
    K.halo_set(vm, pal, y=-3.0, R=9.0, rune=False, shaft_len=26.0)
    vm.deco(PT.deco_veins, fang, glow, n=5, strength=0.9, seed=3, min_w=8, min_h=8)
    vm.deco(PT.deco_fill_glow, em, glow, strength=0.9, seed=4)
    vm.deco(PT.deco_fill_glow, veins, glow, strength=0.8, seed=5)
    vm.deco(PT.deco_grain, fang, amount=0.2)
    vm.deco(PT.deco_glyphs, cap, glow, text="KAMISH", rows=1, glow=False, seed=6)
    vm.deco(PT.deco_filigree, ridges, trim, seed=7)


add(Item("kamishs_fang_charm", build_fang, dict(style="lava", dark=(18, 2, 0), mid=(170, 50, 12), hot=(255, 200, 100)), hand_size=0.86, hand_style="tool", seed=23,
         layers=[GL("dragon heart fire", "nebula_core", "heart_fire"), GL("scale shards", "orbit_shards", "scale_shards"), GL("rising embers", "orbit_shards", "rising_embers"),
                 GL("fire ring", "portal_ripples", "fire_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("embers", "embers", "flame", None, count=1, every=4, radius=0.4, rise=0.03),
                 PL("ash", "drips", "dust", 0xFF5A2A, count=1, every=6, radius=0.5, rise=0.02),
                 PL("heat sparks", "sparks", "lava", None, count=1, every=12, radius=0.4, rise=0.01)],
         acts={"dragons_fury": [ST("FIRE_PILLAR", 0xFF5A2A, 2.6, 50, "ground"), ST("FIRE_WILD", 0xFFB060, 2.4, 44, "self"), ST("LIGHT_FLARE", 0xFFB060, 1.6, 14, "self")],
               "dragons_roar": [ST("FIRE_BURST", 0xFF5A2A, 3.2, 40, "front"), ST("WIND_RING", 0xFFB060, 3.0, 40, "self")]}))


# ====================================================================== MOON-TOWER BLACK SIGIL
def build_sigil(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_disc = vm.grp("sigil", "bob", (0, 0, 0), 0.3, 0.06, 0.0)
    g_void = vm.grp("void_disc", "pulse", (0, 0, -0.8), 0.03, 0.09, 0.0)
    g_phases = vm.grp("moon_phases", "spin", (0, 0, 0), 1.5)
    g_flames = vm.grp("shadow_flames", "pulse", (0, 0, 0), 0.08, 0.17, 0.4)
    g_tower = vm.grp("tower", "sway", (0, -3, -1), 0.0, 0.0, 0.0)
    g_seal = vm.grp("sigil_seal", "spin", (0, 0, 2.2), -0.9)
    g_seal2 = vm.grp("sigil_seal_b", "spin", (0, 0, 2.6), 1.6)
    # a black disc: a raised rim of obsidian round a void inlay; the sigil is a crescent moon holding a tower
    disc = vm.P("disc", "obsidian", group=g_disc, base=(14, 12, 28), lod=False)
    geo.lathe(disc, "y", (0, 0, 0), [(-0.6, 9.6), (0.2, 10.0), (1.1, 9.4), (1.1, 7.0), (0.3, 6.8)], sides=vm.S(16, 8), caps=(True, False))
    disc.V = [geo.rot_matrix("x", -90) @ v for v in disc.V]
    rim = vm.P("rim_ring", "gold", group=g_disc, base=(120, 90, 200), slot=TRIM)
    geo.merge(rim, geo.prim_torus(8.2, 0.34, n=vm.S(20, 8), m=3), geo.rot_matrix("x", -90), (0, 0, -1.0))
    void = vm.star("void", g_void)
    geo.merge(void, geo.prim_lathe([(-0.15, 6.8), (0.15, 6.8)], sides=vm.S(10, 6), caps=(True, True)), geo.rot_matrix("x", -90), (0, 0, -0.75))
    moon = vm.P("crescent", "silver", group=g_disc, base=(176, 150, 255), slot=TRIM, lod=True)
    pts = geo.arc_pts((0, -0.5, -1.5), 4.6, 120, 330, vm.S(10, 5), plane="xy")
    n = len(pts)
    geo.sweep(moon, pts, geo.circle(0.9, 4), scales=[(0.3 + 0.9 * math.sin(math.pi * k / (n - 1)),) * 2 for k in range(n)], caps=(True, True))
    tower = vm.P("tower", "obsidian", group=g_tower, base=(40, 28, 78), slot=TRIM, lod=False)
    for i, (w, h, yy) in enumerate(((2.4, 2.0, -2.6), (2.0, 1.8, -0.7), (1.7, 1.6, 1.0), (1.3, 1.4, 2.6))):
        geo.merge(tower, geo.prim_box(w, h, w, caps=False), None, (0, yy, -1.7))
    geo.merge(tower, geo.prim_spike(0.8, 2.4, sides=4), None, (0, 4.5, -1.7))
    win = vm.P("tower_windows", "crystal", group=g_tower, base=(210, 190, 255), slot=GLOW)
    for yy in (-2.6, -0.7, 1.0):
        geo.merge(win, geo.prim_box(0.5, 0.7, 0.2, caps=False), None, (0, yy, -2.6))
    ph = vm.P("moon_phases", "crystal", group=g_phases, base=(200, 180, 255), slot=GLOW, lod=False)
    for i in range(8):
        a = 2 * math.pi * i / 8
        geo.merge(ph, geo.prim_octa(0.8 if i % 2 else 0.6, 0.8 if i % 2 else 0.6), None, (12.0 * math.cos(a), 12.0 * math.sin(a), -0.5))
    fl = vm.P("shadow_flames", "obsidian", group=g_flames, base=(54, 30, 110), slot=ACCENT, lod=False)
    lf = geo.prim_spike(0.9, 4.6, sides=3)
    for i in range(10):
        a = 2 * math.pi * i / 10
        geo.merge(fl, lf, geo.align_y((math.cos(a), math.sin(a), -0.15)), (9.6 * math.cos(a), 9.6 * math.sin(a), -0.4))
    vm.fx_disc(g_seal, GLOW, (0, 0, 2.2), (0, 0, 1), 30.0, "seal")
    vm.fx_disc(g_seal2, DARK, (0, 0, 2.6), (0, 0, 1), 24.0, "rune")
    K.halo_set(vm, pal, y=0.0, R=9.5, rune=False, shaft_len=22.0)
    vm.deco(PT.deco_glyphs, disc, glow, text="UNSEEN ECLIPSE", rows=2, glow=True, strength=0.85, seed=3)
    vm.deco(PT.deco_fill_glow, win, glow, strength=0.9, seed=4, facets=False)
    vm.deco(PT.deco_fill_glow, ph, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_filigree, rim, trim, seed=6)
    vm.deco(PT.deco_stars, moon, glow, n=30, seed=7, strength=0.7)


add(Item("moon_tower_black_sigil", build_sigil, dict(style="void", dark=(1, 0, 8), mid=(54, 34, 120), hot=(190, 170, 255)), hand_size=0.74, hand_style="palm", seed=24,
         layers=[GL("void disc", "nebula_core", "void_disc"), GL("moon phases", "orbit_shards", "moon_phases"), GL("shadow flames", "aura_shell", "shadow_flames"),
                 GL("sigil seal", "rune_ring", "sigil_seal"), GL("counter seal", "rune_ring", "sigil_seal_b"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("shadow motes", "wisps", "dust", 0x8A6AE8, count=1, every=4, radius=0.55, rise=0.012),
                 PL("unseen glints", "glints", "witch", None, count=1, every=9, radius=0.5, rise=0.0)],
         acts={"eminence": [ST("SHADOW_UNITE", 0x8A6AE8, 3.0, 70, "self"), ST("SEAL_TRINITY", 0xC0A8FF, 2.6, 60, "ground"), ST("DIVINE_HALO", 0x8A6AE8, 2.4, 50, "sky")],
               "quiet_finale": [ST("DEMON_METEOR", 0x8A6AE8, 3.0, 56, "sky"), ST("SHADOW_POOL", 0x0A0A1A, 3.0, 60, "ground")]}))


# ====================================================================== ORRERY OF DROWNED HOURS
def build_orrery(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_base = vm.grp("tide_base", "bob", (0, 0, 0), 0.3, 0.05, 0.0)
    g_sun = vm.grp("drowned_sun", "pulse", (0, 3, 0), 0.07, 0.12, 0.0)
    g_ecl = vm.grp("ecliptic", "tumble", (0, 3, 0), 0.0, 0.0, 0.5)
    g_arm = [vm.grp("planet_%d" % (i + 1), "spin", (0, 3, 0), s) for i, s in enumerate((3.2, -2.4, 1.9, -1.5, 1.1, -0.8, 0.55))]
    g_bub = vm.grp("bubbles", "bob", (0, 3, 0), 2.0, 0.08, 0.0)
    g_tide = vm.grp("tide_ripple", "pulse", (0, -9, 0), 0.25, 0.08, 0.0)
    # a verdigris brass pedestal, barnacles and a coral branch on it
    ped = vm.P("pedestal", "bronze", group=g_base, base=(150, 130, 80), lod=True)
    geo.lathe(ped, "y", (0, 0, 0), [(-13.0, 5.4), (-12.0, 6.4), (-10.5, 4.0), (-9.0, 1.8), (-5.0, 1.2), (-2.0, 1.6)], sides=vm.S(10, 5), caps=(True, False))
    band = vm.P("pedestal_band", "gold", group=g_base, base=(206, 170, 90), slot=TRIM)
    for yy in (-12.4, -9.6):
        geo.lathe(band, "y", (0, 0, 0), [(yy - 0.3, 6.5 if yy < -12 else 4.2), (yy + 0.3, 6.5 if yy < -12 else 4.2)], sides=vm.S(10, 5), caps=(False, False))
    barn = vm.P("barnacles", "stone", group=g_base, base=(150, 190, 180), lod=False)
    rng = np.random.default_rng(3)
    for i in range(6):
        a = rng.uniform(0, 6.28)
        geo.merge(barn, geo.prim_octa(0.6, 0.8), None, (6.0 * math.cos(a), -12.5 + rng.uniform(0, 2.5), 6.0 * math.sin(a)))
    coral = vm.P("coral", "bone", group=g_base, base=(230, 130, 120), lod=False)
    for i in range(2):
        a = 2 * math.pi * i / 2 + 0.6
        pts = geo.bez3((5.6 * math.cos(a), -12.2, 5.6 * math.sin(a)), (7.4 * math.cos(a), -9.0, 7.4 * math.sin(a)), (6.4 * math.cos(a), -6.0, 6.4 * math.sin(a)), (7.2 * math.cos(a), -3.0, 7.2 * math.sin(a)), vm.S(5, 3))
        geo.tube_path(coral, pts, 0.5, sides=3, taper=(1.0, 0.3))
    # a drowned sun, the ecliptic rings, and seven planets on arms
    sun = vm.star("sun", g_sun)
    geo.merge(sun, geo.prim_sphere(2.8, 4, 8), None, (0, 3, 0))
    ecl = vm.P("ecliptic", "gold", group=g_ecl, base=(214, 180, 96), slot=TRIM, lod=False)
    geo.merge(ecl, geo.prim_torus(9.0, 0.34, n=vm.S(14, 8), m=3), geo.align_y((0.4, 1, 0)), (0, 3, 0))
    geo.merge(ecl, geo.prim_torus(11.6, 0.28, n=vm.S(16, 8), m=3), geo.align_y((-0.3, 1, 0.5)), (0, 3, 0))
    for i, g in enumerate(g_arm):
        rr = 3.6 + 1.25 * i
        arm = vm.P("arm_%d" % (i + 1), "bronze", group=g, base=(176, 146, 80), lod=False)
        geo.tube_path(arm, [(0, 3, 0), (rr * 0.5, 3.0 + 0.4 * i, 0), (rr, 3.0 + 0.4 * i, 0)], 0.16, sides=3, caps=(False, True))
        pl = vm.P("planet_%d" % (i + 1), "crystal", group=g, base=[(90, 220, 220), (240, 160, 120), (150, 190, 255), (120, 230, 170), (255, 220, 120), (200, 150, 255), (255, 140, 160)][i], slot=GLOW, lod=False)
        geo.merge(pl, geo.prim_octa(1.0 + 0.18 * (i % 3), 1.25 + 0.18 * (i % 3)), None, (rr, 3.0 + 0.4 * i, 0))
        vm.deco(PT.deco_fill_glow, pl, glow, strength=0.8, seed=i)
    bubbles = K.orbs(vm, "bubbles", g_bub, 4, 6.0, 8.0, 0.45, "crystal", (190, 250, 255), slot=ACCENT, phase=0.2, wobble=3.0, lod=False)
    vm.fx_disc(g_tide, GLOW, (0, -11.5, 0), (0, 1, 0), 26.0, "ripple")
    vm.fx_cross(g_tide, DARK, (0, 3, 0), (0, 1, 0), 14.0, "web", n=2)
    K.halo_set(vm, pal, y=3.0, R=9.0, rune=True, shaft_len=24.0)
    vm.deco(PT.deco_tint, ped, (60, 170, 150), amount=0.45)
    vm.deco(PT.deco_glyphs, ped, glow, text="DROWNED HOURS", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_glyphs, ecl, glow, text="VII", rows=1, glow=True, strength=0.7, seed=4)
    vm.deco(PT.deco_filigree, band, trim, seed=5)
    vm.deco(PT.deco_grain, ped, amount=0.18)
    vm.deco(PT.deco_tint, barn, (80, 140, 120), amount=0.3)


add(Item("orrery_of_drowned_hours", build_orrery, dict(style="ocean", dark=(0, 10, 16), mid=(14, 110, 130), hot=(160, 245, 255)), hand_size=0.8, hand_style="palm", seed=25,
         layers=[GL("drowned sun", "nebula_core", "drowned_sun"), GL("ecliptic rings", "halo_ring", "ecliptic")] + [GL("planet %d" % (i + 1), "orbit_shards", "planet_%d" % (i + 1)) for i in range(7)] +
                [GL("bubbles", "orbit_shards", "bubbles"), GL("tide ripple", "portal_ripples", "tide_ripple"), GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("rising bubbles", "motes", "bubble", None, count=1, every=4, radius=0.45, rise=0.03),
                 PL("tide drips", "drips", "dripping_water" if False else "dust", 0x3AC8D8, count=1, every=7, radius=0.5, rise=-0.015)],
         acts={"tidal_hour": [ST("WATER_NEST", 0x3AC8D8, 3.2, 70, "ground"), ST("WATER_RING", 0x8AF0FF, 3.0, 50, "ground"), ST("DIVINE_HALO", 0x8AF0FF, 2.0, 50, "self")],
               "slow_the_tide": [ST("WATER_CRADLE", 0x3AC8D8, 2.6, 60, "self"), ST("MAGIC_CIRCLE", 0x8AF0FF, 2.6, 40, "ground")]}))


# ====================================================================== POCKET WATCH OF RETURN
def build_watch(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_w = vm.grp("watch", "bob", (0, 0, 0), 0.3, 0.06, 0.0)
    g_sec = vm.grp("second_hand_back", "spin", (0, 0, -1.0), -7.0)
    g_min = vm.grp("minute_hand_back", "spin", (0, 0, -1.0), -1.1)
    g_hour = vm.grp("hour_hand_back", "spin", (0, 0, -1.0), -0.09)
    g_lid = vm.grp("open_lid", "sway", (9.5, 0, 0), 4.0, 0.05, 0.0)
    g_gear1 = vm.grp("gear_a", "tumble", (0, 0, 2.6), 0.0, 0.0, 0.0)
    g_gear1 = vm.grp("gear_a_turn", "spin", (-8, 6, 2), 3.0)
    g_gear2 = vm.grp("gear_b_turn", "spin", (8, -6, 2), -3.6)
    g_chain = vm.grp("chain", "sway", (0, 11, 0), 3.0, 0.05, 0.0)
    g_rew = vm.grp("rewind_rings", "pulse", (0, 0, -1.5), -0.22, 0.12, 0.0)
    g_rew2 = vm.grp("rewind_rings_b", "spin", (0, 0, -1.8), -1.5)
    # the case: a silver disc with a bezel, a crown, a bow; the dial with twelve markers and three hands that run backwards
    body = vm.P("case", "silver", group=g_w, base=(204, 206, 218), lod=True)
    geo.lathe(body, "y", (0, 0, 0), [(-1.8, 8.4), (-1.0, 9.4), (0.8, 9.6), (1.4, 8.8), (1.4, 7.6)], sides=vm.S(14, 8), caps=(True, False))
    body.V = [geo.rot_matrix("x", -90) @ v for v in body.V]
    dial = vm.P("dial", "bone", group=g_w, base=(246, 242, 230), lod=True)
    geo.lathe(dial, "y", (0, 0, 0), [(0.0, 7.6), (0.15, 7.5)], sides=vm.S(14, 8), caps=(True, True))
    dial.V = [geo.rot_matrix("x", -90) @ v + np.array([0, 0, 0.0]) for v in dial.V]
    dial.V = [v + np.array([0, 0, -1.55]) for v in dial.V]
    marks = vm.P("hour_marks", "gold", group=g_w, base=(236, 196, 100), slot=TRIM, lod=False)
    mk = geo.prim_box(0.6, 1.5, 0.3, caps=False)
    for h in range(12):
        a = math.radians(30 * h)
        geo.merge(marks, mk, geo.rot_matrix("z", -30 * h), (6.4 * math.sin(a), 6.4 * math.cos(a), -1.75))
    bez = vm.P("bezel", "gold", group=g_w, base=(240, 214, 140), slot=TRIM)
    geo.merge(bez, geo.prim_torus(8.2, 0.55, n=vm.S(20, 8), m=3), geo.rot_matrix("x", -90), (0, 0, -1.3))
    crown = vm.P("crown", "gold", group=g_w, base=(236, 204, 120), slot=TRIM, lod=False)
    geo.lathe(crown, "y", (0, 0, 0), [(8.8, 1.1), (9.6, 1.5), (11.3, 1.0)], sides=vm.S(6, 4), caps=(True, True))
    geo.merge(crown, geo.prim_torus(2.0, 0.42, n=vm.S(12, 6), m=3), geo.rot_matrix("x", 90) @ geo.rot_matrix("y", 0), (0, 13.2, 0))
    chain = vm.P("chain", "silver", group=g_chain, base=(190, 192, 206), lod=False)
    geo.chain(chain, [(0, 15.0, 0), (1.5, 18.5, 0), (4.5, 21.5, 0)], link_r=0.8, wire=0.2, n=6, m=3, every=3.0)
    for g, nm, L, W, base in ((g_hour, "hand_hour", 4.2, 0.9, (40, 40, 60)), (g_min, "hand_min", 6.2, 0.7, (50, 50, 70)), (g_sec, "hand_sec", 6.8, 0.3, (200, 60, 50))):
        h = vm.P(nm, "iron", group=g, base=base, lod=False)
        blade = geo.prim_blade(L, W, 0.3, tip=0.3)
        geo.merge(h, blade, geo.rot_matrix("x", 90) @ geo.rot_matrix("z", 0), (0, 0, -2.0 - 0.15 * (0 if nm == "hand_hour" else 1 if nm == "hand_min" else 2)))
    lid = vm.P("open_lid", "silver", group=g_lid, base=(196, 198, 212), lod=False)
    geo.lathe(lid, "y", (0, 0, 0), [(-0.4, 8.4), (0.6, 8.2), (0.6, 7.4)], sides=vm.S(12, 8), caps=(True, False))
    lid.V = [geo.rot_matrix("y", 105) @ (geo.rot_matrix("x", -90) @ v) + np.array([9.2, 0.0, 0.0]) for v in lid.V]
    # two gears in the case back peeking out, and rings that rewind
    for g, nm, c, R_ in ((g_gear1, "gear_a", (-8.0, 6.0, 2.0), 3.2), (g_gear2, "gear_b", (8.0, -6.0, 2.0), 2.6)):
        gp = vm.P(nm, "gold", group=g, base=(220, 190, 120), slot=TRIM, lod=False)
        gg = geo.prim_gear(R_, 6, 0.7, 0.8)
        geo.merge(gp, gg, geo.rot_matrix("x", 90), c)
    vm.fx_disc(g_rew, TRIM, (0, 0, -2.3), (0, 0, 1), 22.0, "ripple")
    vm.fx_disc(g_rew2, DARK, (0, 0, -2.6), (0, 0, 1), 19.0, "ticks")
    K.halo_set(vm, pal, y=0.0, R=9.0, rune=True, shaft_len=22.0)
    vm.deco(PT.deco_glyphs, body, glow, text="RETURN", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_filigree, bez, trim, seed=4)
    vm.deco(PT.deco_glyphs, dial, glow, rows=1, glow=False, seed=5, min_w=20)
    vm.deco(PT.deco_fill_glow, marks, glow, strength=0.8, seed=6, facets=False)
    vm.deco(PT.deco_grain, body, amount=0.08)


add(Item("pocket_watch_of_return", build_watch, dict(style="swirl", dark=(10, 10, 14), mid=(120, 120, 150), hot=(255, 255, 255)), hand_size=0.74, hand_style="hang", seed=26,
         layers=[GL("second hand backwards", "halo_ring", "second_hand_back"), GL("minute hand backwards", "halo_ring", "minute_hand_back"), GL("hour hand backwards", "halo_ring", "hour_hand_back"),
                 GL("gear a", "orbit_shards", "gear_a_turn"), GL("gear b", "orbit_shards", "gear_b_turn"), GL("rewind ripple", "portal_ripples", "rewind_rings"), GL("rewind ticks", "rune_ring", "rewind_rings_b"),
                 GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("returning motes", "motes", "end_rod", None, count=1, every=5, radius=0.5, rise=-0.012),
                 PL("silver glints", "glints", "dust", 0xF0D890, count=1, every=7, radius=0.5, rise=0.008)],
         acts={"return": [ST("MANA_CHARGE", 0xF0D890, 2.4, 36, "self"), ST("DIVINE_HALO", 0xFFFFFF, 2.4, 40, "self"), ST("LIGHT_FLARE", 0xFFFFFF, 1.6, 14, "self")],
               "remember": [ST("MAGIC_CIRCLE", 0xF0D890, 2.2, 36, "ground"), ST("LIGHT_FLARE", 0xFFFFFF, 1.0, 12, "self")]}))

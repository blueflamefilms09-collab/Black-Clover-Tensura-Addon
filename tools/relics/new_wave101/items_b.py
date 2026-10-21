"""The Millennium Items, part 2 (Yu-Gi-Oh!): the Eye (a brow piece on a lapis circlet) and the Ring (a golden ring hung with five needles, on the wrist)."""
import math

import numpy as np

from common import *  # noqa: F401,F403
from common import (geo, K, PT, V, Item, GL, PL, ST, MAIN, TRIM, GLOW, DARK, ACCENT, GOLD, GOLD_HI, GOLD_DK, LAPIS, LAPIS_DK, LAPIS_HI, TURQ, deco_lapis, deco_hiero, deco_wdjat, wdjat,
                    paint_wdjat, ankh, orbit_motes)

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


# ====================================================================== MILLENNIUM EYE (head)
def build_eye(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_c = vm.grp("circlet", "bob", (0, 8, 0), 0.22, 0.05, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, 7.6, -5.3), 0.12, 0.14, 0.0)
    g_cobra = vm.grp("uraeus", "sway", (0, 9.0, -4.4), 2.5, 0.06, 0.0)
    g_wings = vm.grp("falcon_wings", "sway", (0, 7.4, -3.5), 1.6, 0.05, 0.0)
    g_lap = vm.grp("lappets", "sway", (0, 6.0, 1.0), 4.0, 0.06, 0.0)
    g_tab = vm.grp("thought_tablets", "spin", (0, 11.5, 0), 1.6)
    g_halo = vm.grp("truth_halo", "spin", (0, 15.0, 0), 1.0)
    g_halo2 = vm.grp("truth_halo_b", "spin", (0, 16.5, 0), -1.5)
    g_void = vm.grp("mind_void", "pulse", (0, 9.0, 6.5), 0.05, 0.08, 0.0)
    g_seal = vm.grp("mind_seal", "spin", (0, 9.0, 7.0), 0.5)
    g_ray = vm.grp("sight_ray", "pulse", (0, 7.6, -8.0), 0.12, 0.16, 0.0)
    g_sig = vm.grp("read_sigils", "tumble", (0, 12.0, 0), 0.5, 0.35, 1.0)
    # the band: lapis, edged in gold, with a row of hieroglyphs
    band = vm.P("band", "stone", group=g_c, base=LAPIS, slot=MAIN, lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(6.6, 4.55), (7.1, 4.85), (8.1, 4.85), (8.6, 4.55)], sides=vm.S(20, 8), caps=(False, False))
    rims = vm.P("band_gold", "gold", group=g_c, base=GOLD, slot=TRIM, lod=False)
    for yy in (6.55, 8.65):
        geo.lathe(rims, "y", (0, 0, 0), [(yy - 0.22, 4.65), (yy + 0.22, 4.9)], sides=vm.S(20, 8), caps=(False, False))
    beads = vm.P("band_beads", "gold", group=g_c, base=GOLD_HI, slot=TRIM, lod=False)
    for i in range(vm.S(10, 4)):
        a = 2 * math.pi * i / 10 + 0.2
        if abs(math.sin(a / 2 + math.pi / 2)) < 0.25:
            continue
        geo.merge(beads, geo.prim_octa(0.28, 0.34), None, (4.95 * math.sin(a), 7.6, -4.95 * math.cos(a)))
    # the brow plaque and the great eye
    plaque = vm.P("brow_plaque", "gold", group=g_c, base=GOLD, slot=TRIM, lod=True)
    geo.merge(plaque, geo.prim_box(5.6, 2.9, 0.7, chamfer=0.3), None, (0, 7.6, -5.0))
    ins = vm.P("plaque_lapis", "stone", group=g_c, base=LAPIS_DK, slot=MAIN, lod=False)
    geo.merge(ins, geo.prim_box(5.0, 2.3, 0.2, chamfer=0.1), None, (0, 7.6, -5.4))
    eye = wdjat(vm, g_eye, (0, 7.55, -5.75), 2.0, lod=True)
    paint_wdjat(vm, eye, glow, trim)
    # the uraeus: a rearing cobra over the brow, hood spread, a lapis eye
    cob = vm.P("uraeus", "gold", group=g_cobra, base=GOLD_HI, slot=TRIM, lod=True)
    geo.tube_path(cob, geo.bez3((0, 9.0, -4.2), (0, 9.2, -5.4), (0, 10.4, -5.9), (0, 11.8, -5.0), vm.S(7, 3)), 0.5, sides=vm.S(6, 4), taper=(1.0, 0.5), caps=(True, True))
    hood = vm.P("uraeus_hood", "gold", group=g_cobra, base=GOLD, slot=TRIM, lod=False)
    geo.merge(hood, geo.prim_lathe([(-0.18, 0.7), (0.0, 1.25), (0.18, 0.7)], sides=vm.S(12, 6), caps=(True, True)), geo.rot_matrix("x", 90), (0, 10.9, -5.55))
    cey = vm.P("uraeus_eyes", "crystal", group=g_cobra, base=(120, 240, 240), slot=GLOW, lod=False)
    for sx in (-0.4, 0.4):
        geo.merge(cey, geo.prim_octa(0.2, 0.25), None, (sx, 12.0, -5.5))
    # falcon wings: gold feathers sweeping back along the band on both sides
    wing = vm.P("falcon_wings", "gold", group=g_wings, base=GOLD_HI, slot=TRIM, lod=False)
    fe = geo.prim_leaf(5.2, 1.5, 0.3, sides=2, bend=0.0)
    for sx in (-1, 1):
        for k in range(3):
            d = (sx * (0.30 + 0.05 * k), -0.10 - 0.16 * k, 1.0)
            geo.place(wing, fe, (sx * 4.95, 8.3 - 0.5 * k, -3.4 + 2.2 * k), up=d, spin=0.0)
    # lappets: striped cloth to the shoulders behind the ears
    lap = vm.P("lappets", "cloth", group=g_lap, base=LAPIS, slot=MAIN, lod=False)
    for sx in (-1, 1):
        geo.ribbon(lap, [(sx * 4.9, 7.0, 0.0), (sx * 5.5, 3.5, 0.4), (sx * 5.3, 0.0, 0.9), (sx * 5.6, -3.0, 1.2)], 2.6, thick=0.2, up=(sx, 0, 0), taper=0.3)
    lg = vm.P("lappet_gold", "gold", group=g_lap, base=GOLD, slot=TRIM, lod=False)
    for sx in (-1, 1):
        for yy in (5.4, 2.4, -0.6):
            geo.merge(lg, geo.prim_box(0.3, 0.45, 2.7), None, (sx * 5.45 + sx * 0.12, yy, 0.6))
    # tablets of thought circling above: lapis plates with gold signs
    tab = vm.P("thought_tablets", "stone", group=g_tab, base=LAPIS, slot=MAIN, lod=False)
    tabg = vm.P("tablet_frames", "gold", group=g_tab, base=GOLD, slot=TRIM, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        M = geo.rot_matrix("y", -math.degrees(a) + 90)
        geo.merge(tab, geo.prim_card(2.2, 3.0, 0.55), M, (8.4 * math.cos(a), 11.5 + 0.9 * math.sin(i * 2.1), 8.4 * math.sin(a)))
        geo.merge(tabg, geo.prim_card(2.8, 3.6, 0.2), M, (8.4 * math.cos(a), 11.5 + 0.9 * math.sin(i * 2.1), 8.4 * math.sin(a)))
    sig = orbit_motes(vm, "read_sigils", g_sig, 6, 11.0, 12.0, 0.7, (130, 245, 240), slot=GLOW, wobble=2.0)
    # two halos of the Eye
    h1 = vm.P("truth_halo", "gold", group=g_halo, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(h1, geo.prim_torus(7.2, 0.45, n=vm.S(26, 10), m=4), geo.rot_matrix("x", 12), (0, 15.0, 0))
    h2 = vm.P("truth_halo_b", "stone", group=g_halo2, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(h2, geo.prim_torus(5.4, 0.3, n=vm.S(22, 8), m=3), geo.rot_matrix("x", -16) @ geo.rot_matrix("z", 8), (0, 16.5, 0))
    # the void behind the head and the beams of sight
    void = vm.star("mind_void", g_void)
    geo.merge(void, geo.prim_lathe([(-0.2, 9.5), (0.2, 9.5)], sides=vm.S(14, 6), caps=(True, True)), geo.rot_matrix("x", 90), (0, 9.0, 6.6))
    vm.fx_disc(g_seal, GLOW, (0, 9.0, 7.2), (0, 0, 1), 19.0, "seal")
    vm.fx_beam(g_ray, ACCENT, (0, 7.6, -6.0), (0, 7.6, -24.0), 2.6, "beam", lod=True)
    vm.fx_disc(g_ray, GLOW, (0, 7.6, -6.2), (0, 0, 1), 5.0, "flare", lod=True)
    K.halo_set(vm, pal, y=11.0, R=6.5, rune=False, shafts=False)
    vm.deco(deco_hiero, band, glow, rows=1, seed=2, glow=False, min_w=12, min_h=3)
    vm.deco(deco_lapis, band, seed=3)
    vm.deco(deco_lapis, ins, seed=4)
    vm.deco(deco_lapis, tab, seed=5, flecks=0.4)
    vm.deco(deco_hiero, tab, glow, rows=2, seed=6, glow=True, strength=0.8, min_w=4, min_h=4)
    vm.deco(PT.deco_fill_glow, sig, glow, strength=0.9, seed=7)
    vm.deco(PT.deco_fill_glow, cey, glow, strength=1.0, seed=8, facets=False)
    vm.deco(PT.deco_filigree, plaque, trim, seed=9, min_w=10, min_h=8)
    vm.deco(PT.deco_grain, wing, amount=0.14)
    vm.deco(PT.deco_grain, cob, amount=0.1)
    vm.deco(deco_hiero, h1, glow, rows=1, seed=10, glow=True, strength=0.7)


add(Item("millennium_eye", build_eye, dict(style="void", dark=(0, 6, 22), mid=(20, 110, 160), hot=(190, 255, 250)), hand_size=0.9, hand_style="brim", seed=102, worn="pieces",
         pieces=[("", "head", False)], anchor="head",
         layers=[GL("mind void", "nebula_core", "mind_void"), GL("wdjat glow", "aura_shell", "wdjat_glow"), GL("uraeus", "aura_shell", "uraeus"), GL("falcon wings", "petals", "falcon_wings"),
                 GL("lappets", "ribbons", "lappets"), GL("thought tablets", "orbit_shards", "thought_tablets"), GL("read sigils", "orbit_shards", "read_sigils"), GL("truth halo", "halo_ring", "truth_halo"),
                 GL("truth halo b", "halo_ring", "truth_halo_b"), GL("mind seal", "rune_ring", "mind_seal"), GL("sight ray", "light_shafts", "sight_ray"), GL("lens flare", "lens_flare", "lens_flare"),
                 PL("watching eyes", "motes", "nusmp:rune_eye", None, count=1, every=9, radius=0.6, rise=0.012),
                 PL("truth glints", "glints", "nusmp:holy_ray", 0x58F0E8, count=1, every=5, radius=0.5, rise=0.012),
                 PL("brow sparks", "sparks", "nusmp:star4", 0xE8B040, count=1, every=7, radius=0.4, rise=0.01)],
         acts={"mind_read": [ST("MIRROR_ARRAY", 0x58F0E8, 3.6, 70, "self"), ST("DIVINE_SIGIL", 0xE8B040, 3.0, 60, "ground"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")],
               "truth_gaze": [ST("LIGHT_BLADE", 0x58F0E8, 3.4, 40, "front"), ST("DIVINE_RAY", 0xE8B040, 3.0, 50, "front")]}))


# ====================================================================== MILLENNIUM RING (hands: built for the right arm, worn on the left through the mirror)
def build_ring(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    AX = -1.0
    g_a = vm.grp("ring_arm", "swing", (0, 0, 0), 0.0, 0.0, 0.0)
    g_need = vm.grp("needles", "sway", (AX, -6.4, -3.6), 5.0, 0.12, 0.0)
    g_dow = vm.grp("dowsing_needle", "sway", (AX, -6.4, -3.9), 9.0, 0.2, 0.7)
    g_eye = vm.grp("ring_glow", "pulse", (AX, -6.4, -3.9), 0.12, 0.14, 0.0)
    g_gyro = vm.grp("wrist_halo", "spin", (AX, -7.0, 0), 1.5)
    g_gyro2 = vm.grp("wrist_halo_b", "spin", (AX, -6.0, 0), -2.1)
    g_orb = vm.grp("seeking_needles", "spin", (AX, -7.0, 0), 2.4)
    g_dust = vm.grp("power_motes", "tumble", (AX, -8.0, 0), 0.6, 0.4, 1.2)
    g_ripple = vm.grp("dowsing_ripple", "pulse", (AX, -7.0, 0), 0.22, 0.1, 0.0)
    g_cuff = vm.grp("cuff_glints", "pulse", (AX, -8.0, 0), 0.12, 0.2, 1.1)
    # the wrist cuff: lapis, gold edges and a row of signs
    cuff = vm.P("cuff", "stone", group=g_a, base=LAPIS, slot=MAIN, lod=True)
    geo.lathe(cuff, "y", (AX, 0, 0), [(-5.6, 3.15), (-6.0, 3.4), (-7.8, 3.4), (-8.2, 3.15)], sides=vm.S(16, 8), caps=(False, False))
    cg = vm.P("cuff_gold", "gold", group=g_a, base=GOLD, slot=TRIM, lod=False)
    for yy in (-5.55, -8.25):
        geo.lathe(cg, "y", (AX, 0, 0), [(yy - 0.2, 3.2), (yy + 0.2, 3.5)], sides=vm.S(16, 8), caps=(False, False))
    # the great ring on the outer side of the wrist, round a pyramid-cut lapis, five needles hanging from it
    CZ = -3.9
    ring = vm.P("ring", "gold", group=g_a, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(ring, geo.prim_torus(2.3, 0.42, n=vm.S(22, 9), m=5, plane="xy"), None, (AX, -6.9, CZ))
    inner = vm.P("ring_inner", "gold", group=g_a, base=GOLD, slot=TRIM, lod=False)
    geo.merge(inner, geo.prim_torus(1.45, 0.2, n=vm.S(18, 7), m=4, plane="xy"), None, (AX, -6.9, CZ - 0.15))
    disc = vm.P("ring_lapis", "stone", group=g_a, base=LAPIS_DK, slot=MAIN, lod=False)
    geo.merge(disc, geo.prim_lathe([(-0.2, 1.25), (0.15, 1.2), (0.4, 0.6), (0.5, 0.0)], sides=vm.S(12, 6), caps=(True, False)), geo.rot_matrix("x", -90), (AX, -6.9, CZ - 0.2))
    eye = wdjat(vm, g_eye, (AX, -6.9, CZ - 0.78), 1.0, lod=False)
    paint_wdjat(vm, eye, glow, trim)
    core = vm.star("ring_core", g_eye)
    geo.merge(core, geo.prim_octa(1.4, 1.9), None, (AX, -6.9, CZ + 0.9))
    nd = vm.P("needles", "gold", group=g_need, base=GOLD_HI, slot=TRIM, lod=False)
    tip = vm.P("needle_tips", "crystal", group=g_need, base=(255, 232, 150), slot=GLOW, lod=False)
    for k in range(5):
        a = math.radians(180 + (k - 2) * 28)                       # fanned along the lower arc of the ring
        base = np.array([AX + 2.3 * math.sin(math.radians((k - 2) * 28)) * -1, -6.9 - 2.3 * math.cos(math.radians((k - 2) * 28)), CZ])
        d = np.array([-0.4 * (k - 2) * 0.3, -1.0, -0.12])
        geo.merge(nd, geo.prim_spike(0.3, 3.6, sides=4), geo.align_y(d), base)
        geo.merge(tip, geo.prim_octa(0.28, 0.5), geo.align_y(d), base + d / np.linalg.norm(d) * 3.7)
    dow = vm.P("dowsing_needle", "gold", group=g_dow, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(dow, geo.prim_spike(0.26, 2.4, sides=4), geo.rot_matrix("x", -90), (AX, -6.9, CZ - 1.1))
    # the ring on the finger, with a lapis eye
    fr = vm.P("finger_ring", "gold", group=g_a, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(fr, geo.prim_torus(0.78, 0.17, n=vm.S(12, 6), m=4, plane="xz"), None, (AX + 0.2, -9.9, -1.5))
    fg = vm.P("finger_stone", "stone", group=g_a, base=LAPIS_HI, slot=MAIN, lod=False)
    geo.merge(fg, geo.prim_octa(0.42, 0.5), None, (AX + 0.2, -9.9, -2.3))
    # halos of the wrist, needles that seek, motes of drawn power
    h1 = vm.P("wrist_halo", "gold", group=g_gyro, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(h1, geo.prim_torus(4.6, 0.22, n=vm.S(20, 8), m=3), None, (AX, -7.0, 0))
    h2 = vm.P("wrist_halo_b", "stone", group=g_gyro2, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(h2, geo.prim_torus(5.2, 0.16, n=vm.S(20, 8), m=3), geo.rot_matrix("x", 20), (AX, -6.0, 0))
    seek = vm.P("seeking_needles", "gold", group=g_orb, base=GOLD_HI, slot=TRIM, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5
        geo.merge(seek, geo.prim_spike(0.26, 2.4, sides=4), geo.align_y((math.cos(a), 0.0, math.sin(a))), (AX + 5.6 * math.cos(a), -7.0, 5.6 * math.sin(a)))
    mot = orbit_motes(vm, "power_motes", g_dust, 7, 7.2, -8.0, 0.4, (255, 226, 130), slot=GLOW, wobble=2.2, phase=0.3)
    vm.fx_disc(g_ripple, GLOW, (AX, -7.0, 0), (0, 1, 0), 22.0, "ripple")
    vm.fx_disc(g_ripple, DARK, (AX, -6.9, 0), (0, 1, 0), 15.0, "ticks")
    vm.fx_cross(g_cuff, ACCENT, (AX, -9.6, -2.0), (0, 1, 0), 4.4, "glint", n=2)
    K.halo_set(vm, pal, y=-7.0, R=9.0, rune=False, shaft=(-22.0, 2.0))
    vm.deco(deco_hiero, cuff, glow, rows=1, seed=2, glow=False, min_w=10, min_h=3)
    vm.deco(deco_lapis, cuff, seed=3)
    vm.deco(deco_lapis, disc, seed=4)
    vm.deco(deco_lapis, fg, seed=5)
    vm.deco(PT.deco_fill_glow, tip, glow, strength=1.0, seed=6)
    vm.deco(PT.deco_fill_glow, mot, glow, strength=0.9, seed=7, facets=False)
    vm.deco(PT.deco_grain, ring, amount=0.12)
    vm.deco(PT.deco_glyphs, h1, glow, rows=1, glow=True, strength=0.7, seed=8)


add(Item("millennium_ring", build_ring, dict(style="swirl", dark=(8, 6, 0), mid=(150, 112, 24), hot=(255, 236, 170)), hand_size=0.95, hand_style="cuff", seed=103, worn="pieces",
         pieces=[("", "larm", True)], anchor="rarm",
         layers=[GL("ring core", "nebula_core", "ring_glow"), GL("ring glow", "aura_shell", "ring_glow"), GL("needles", "aura_shell", "needles"), GL("dowsing needle", "aura_shell", "dowsing_needle"), GL("wrist halo", "halo_ring", "wrist_halo"),
                 GL("wrist halo b", "halo_ring", "wrist_halo_b"), GL("seeking needles", "orbit_shards", "seeking_needles"), GL("power motes", "orbit_shards", "power_motes"),
                 GL("dowsing ripple", "portal_ripples", "dowsing_ripple"), GL("cuff glints", "lens_flare", "cuff_glints"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("drawn power", "motes", "nusmp:mana_mote", 0xFFD870, count=1, every=5, radius=0.5, rise=0.014),
                 PL("needle glints", "glints", "nusmp:gem_sparkle", 0xF2C860, count=1, every=7, radius=0.4, rise=0.01)],
         acts={"dowsing_needles": [ST("COMPASS_ARRAY", 0xFFD870, 3.4, 70, "ground"), ST("SPIRIT_AURA", 0xF2C860, 2.4, 40, "self")],
               "draw_power": [ST("MANA_CHARGE", 0xFFD870, 3.4, 60, "self"), ST("GRAVITY_WELL", 0xF2C860, 3.2, 70, "ground")]}))

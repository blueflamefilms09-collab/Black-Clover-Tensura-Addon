"""Eight more world-level artifacts, part 1: the Truth-Seeking Orbs of the Sage (Naruto, back), the Hogyoku (Bleach, charm), the Seven Dragon Balls of Shenron (Dragon Ball, belt)."""
import math

import numpy as np

from common import *  # noqa: F401,F403
from common import (geo, K, PT, V, Item, GL, PL, ST, MAIN, TRIM, GLOW, DARK, ACCENT, GOLD, GOLD_HI, GOLD_DK, deco_lapis, deco_hiero, orbit_motes)

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


def tomoe(vm, part, centre, scale=1.0, M=None, sides=4):
    """One tomoe (a comma): a round head and a curled tail, built from a tapering tube, facing -Z, merged into `part`."""
    M = np.eye(3) if M is None else M
    c = np.asarray(centre, float)
    path = [(0.9 * scale * math.cos(a) * (1 - a / 7.5), 0.9 * scale * math.sin(a) * (1 - a / 7.5) + 0.0, 0.0) for a in np.linspace(0.3, 4.4, vm.S(6, 3))]
    geo.tube_path(part, [tuple(M @ np.array(p) + c) for p in path], 0.55 * scale, sides=sides, taper=(1.0, 0.12), caps=(True, True))


# ====================================================================== TRUTH-SEEKING ORBS OF THE SAGE (back)
def build_orbs(vm, info):
    pal = info[4]
    glow = K.rgb(pal[2])
    BZ, BY = 6.4, -3.6                                                  # the centre of the circle of orbs, behind the back
    g_ring = vm.grp("orb_ring", "tumble", (0, BY, BZ), 0.0, 0.7, 0.0)
    g_tom = vm.grp("tomoe_ring", "tumble", (0, BY, BZ - 0.6), 0.0, -0.45, 0.0)
    g_core = vm.grp("black_core", "pulse", (0, BY, BZ + 0.6), 0.06, 0.12, 0.0)
    g_seal = vm.grp("rinnegan_seal", "spin", (0, BY, BZ + 1.4), 0.5)
    g_seal2 = vm.grp("rinnegan_seal_b", "spin", (0, BY, BZ + 1.6), -0.9)
    g_staff = vm.grp("sage_staff", "sway", (4.4, 1.0, 3.2), 2.0, 0.05, 0.0)
    g_rings = vm.grp("staff_rings", "swing", (4.4, 0.5, 3.2), 6.0, 0.15, 0.0)
    g_mot = vm.grp("seeking_motes", "spin", (0, BY, BZ), 1.4)
    g_pet = vm.grp("black_feathers", "spin", (0, BY, 1.0), 0.8)
    g_neck = vm.grp("tomoe_collar", "bob", (0, 0.0, 0.0), 0.2, 0.05, 0.0)
    g_fl = vm.grp("orb_glints", "pulse", (0, BY, BZ - 1.0), 0.12, 0.2, 1.0)
    # nine black orbs on a circle behind the shoulders
    R = 8.6
    orbs = vm.P("truth_orbs", "obsidian", group=g_ring, base=(14, 14, 22), slot=MAIN, lod=True)
    rimr = vm.P("orb_rims", "silver", group=g_ring, base=(214, 214, 230), slot=TRIM, lod=False)
    cores = vm.P("orb_cores", "crystal", group=g_ring, base=(230, 230, 255), slot=GLOW, lod=False)
    for i in range(9):
        a = 2 * math.pi * i / 9 + math.pi / 2
        c = (R * math.cos(a), BY + R * math.sin(a), BZ)
        geo.merge(orbs, geo.prim_sphere(1.55, vm.S(4, 2), vm.S(8, 4)), None, c)
        geo.merge(rimr, geo.prim_torus(1.95, 0.1, n=vm.S(12, 6), m=3, plane="xy"), None, c) if i % 3 == 0 else None
        geo.merge(cores, geo.prim_octa(0.28, 0.4), None, (c[0], c[1], c[2] - 1.55))
    link = vm.P("orb_chain", "silver", group=g_ring, base=(196, 196, 214), slot=TRIM, lod=False)
    geo.merge(link, geo.prim_torus(R, 0.1, n=vm.S(24, 10), m=3, plane="xy"), None, (0, BY, BZ))
    # the sage's ring of tomoe, turning the other way inside it
    tr = vm.P("tomoe", "silver", group=g_tom, base=(236, 236, 248), slot=TRIM, lod=False)
    geo.merge(tr, geo.prim_torus(5.6, 0.12, n=vm.S(26, 10), m=3, plane="xy"), None, (0, BY, BZ - 0.6))
    for i in range(9):
        a = 2 * math.pi * i / 9
        tomoe(vm, tr, (5.6 * math.cos(a), BY + 5.6 * math.sin(a), BZ - 0.9), 0.95, geo.rot_matrix("z", math.degrees(a)))
    # the black core of the world behind, and the rinnegan seal under it
    core = vm.star("black_core", g_core)
    geo.merge(core, geo.prim_octa(2.8, 3.4), None, (0, BY, BZ + 0.4))
    for k, (rad, cell, g) in enumerate(((19.0, "seal", g_seal), (15.0, "ring", g_seal2), (11.0, "ticks", g_seal), (7.0, "ring", g_seal2))):
        vm.fx_disc(g, GLOW if k % 2 == 0 else DARK, (0, BY, BZ + 1.8 + 0.1 * k), (0, 0, 1), rad, cell)
    # the sage's staff (khakkhara) carried over the shoulder: a black shaft, a silver head with six rings
    sh = vm.P("sage_staff", "iron", group=g_staff, base=(34, 32, 44), slot=MAIN, lod=False)
    geo.tube_path(sh, [(4.4, -13.0, 3.2), (4.4, 6.2, 3.2)], 0.4, sides=6, caps=(True, True))
    hd = vm.P("staff_head", "silver", group=g_staff, base=(214, 214, 232), slot=TRIM, lod=False)
    geo.merge(hd, geo.prim_torus(1.7, 0.28, n=vm.S(14, 6), m=4, plane="xy"), None, (4.4, 8.2, 3.2))
    geo.merge(hd, geo.prim_octa(0.55, 0.9), None, (4.4, 6.5, 3.2))
    sr = vm.P("staff_rings", "silver", group=g_rings, base=(236, 236, 250), slot=TRIM, lod=False)
    for k in range(4):
        a = 2 * math.pi * k / 4
        geo.merge(sr, geo.prim_torus(0.5, 0.1, n=vm.S(7, 5), m=3, plane="xy"), None, (4.4 + 1.7 * math.cos(a), 8.2 + 1.7 * math.sin(a), 3.2))
    # a collar of nine tomoe at the neck (the sage's necklace) and motes of seeking
    col = vm.P("tomoe_collar", "obsidian", group=g_neck, base=(18, 18, 26), slot=MAIN, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        x, z = 3.6 * math.sin(a), -3.2 * math.cos(a) * 0.62 - 0.1
        tomoe(vm, col, (x, 0.3, z), 0.7, geo.rot_matrix("x", -90) @ geo.rot_matrix("z", math.degrees(a)), sides=4)
    mot = orbit_motes(vm, "seeking_motes", g_mot, 7, 12.5, BY, 0.5, (236, 236, 255), slot=GLOW, wobble=2.0)
    pet = vm.P("black_feathers", "obsidian", group=g_pet, base=(20, 20, 28), slot=MAIN, lod=False)
    for i in range(4):
        a = 2 * math.pi * i / 4
        geo.merge(pet, geo.prim_leaf(2.6, 0.9, 0.1, sides=2, bend=0.05), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 95), (10.0 * math.cos(a), BY + 3.0 - 1.2 * i, 8.0 + 10.0 * math.sin(a) * 0.5))
    vm.fx_cross(g_fl, ACCENT, (0, BY, BZ - 2.2), (0, 0, 1), 6.0, "glint", n=2)
    K.halo_set(vm, pal, y=BY, R=10.0, rune=False, shaft=(-20.0, 12.0))
    vm.deco(PT.deco_fill_glow, cores, glow, strength=0.9, seed=2, facets=False)
    vm.deco(PT.deco_fill_glow, mot, glow, strength=0.9, seed=3, facets=False)
    vm.deco(PT.deco_grain, orbs, amount=0.2)
    vm.deco(PT.deco_glyphs, tr, glow, rows=1, glow=False, seed=5)
    vm.deco(PT.deco_glyphs, link, glow, rows=1, glow=False, seed=6, min_w=6)
    vm.deco(PT.deco_grain, sh, amount=0.14)


add(Item("truth_seeking_orbs_of_the_sage", build_orbs, dict(style="void", dark=(2, 2, 8), mid=(60, 60, 96), hot=(236, 236, 255)), hand_size=1.0, hand_style="cloak", seed=107, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("black core", "nebula_core", "black_core"), GL("orb ring", "orbit_shards", "orb_ring"), GL("tomoe ring", "orbit_shards", "tomoe_ring"), GL("rinnegan seal", "rune_ring", "rinnegan_seal"),
                 GL("rinnegan seal b", "rune_ring", "rinnegan_seal_b"), GL("sage staff", "aura_shell", "sage_staff"), GL("staff rings", "aura_shell", "staff_rings"), GL("seeking motes", "orbit_shards", "seeking_motes"),
                 GL("black feathers", "petals", "black_feathers"), GL("tomoe collar", "aura_shell", "tomoe_collar"), GL("orb glints", "lens_flare", "orb_glints"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("black feathers falling", "drips", "nusmp:feather_black", None, count=1, every=8, radius=0.6, rise=-0.012),
                 PL("seeking wisps", "wisps", "nusmp:void_speck", 0xE0E0FF, count=1, every=5, radius=0.55, rise=0.012),
                 PL("truth glints", "glints", "nusmp:glint", 0xFFFFFF, count=1, every=6, radius=0.45, rise=0.01)],
         acts={"truth_seeking_barrage": [ST("LIGHT_BLADE", 0xE0E0FF, 2.6, 30, "front"), ST("SPACE_CRACK", 0x8A8AA8, 3.0, 36, "front")],
               "shinra_tensei": [ST("GRAVITY_WELL", 0xE0E0FF, 4.4, 50, "ground"), ST("WIND_RING", 0xFFFFFF, 4.0, 40, "ground")],
               "chibaku_tensei": [ST("GRAVITY_WELL", 0xE0E0FF, 4.6, 90, "ground"), ST("SPACE_TEAR", 0x8A8AA8, 3.8, 80, "sky"), ST("SHADOW_POOL", 0x14141C, 3.6, 80, "ground")]}))


# ====================================================================== HOGYOKU, THE CRYSTAL OF DISSOLUTION (charm)
def build_hogyoku(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_c = vm.grp("crystal", "bob", (0, 0, 0), 0.45, 0.05, 0.0)
    g_core = vm.grp("soul_swirl", "tumble", (0, 2, 0), 0.5, 0.3, 0.9)
    g_r1 = vm.grp("dissolve_ring", "tumble", (0, 2, 0), 0.0, 0.0, 2.0)
    g_r2 = vm.grp("dissolve_ring_b", "tumble", (0, 2, 0), 1.4, 0.0, -1.3)
    g_r3 = vm.grp("dissolve_ring_c", "tumble", (0, 2, 0), 0.0, 1.8, 0.0)
    g_wing = vm.grp("reishi_wings", "sway", (0, 2, -1.5), 3.5, 0.05, 0.0)
    g_shards = vm.grp("crystal_shards", "spin", (0, 2, 0), 2.0)
    g_shards2 = vm.grp("crystal_shards_far", "spin", (0, 4, 0), -1.1)
    g_bound = vm.grp("boundary_ripple", "pulse", (0, 2, 0), 0.2, 0.1, 0.0)
    g_hal = vm.grp("lilac_halo", "spin", (0, 14, 0), 1.0)
    g_cage = vm.grp("cradle", "bob", (0, 0, 0), 0.45, 0.05, 0.0)
    # the crystal: a faceted lilac egg with a bright heart of swirling souls
    egg = vm.P("crystal_shell", "crystal", group=g_c, base=(190, 150, 240), slot=MAIN, lod=True)
    geo.merge(egg, geo.prim_lathe([(-5.2, 0.0), (-4.4, 2.2), (-2.0, 4.1), (0.8, 4.6), (3.4, 3.6), (5.2, 1.6), (6.0, 0.0)], sides=vm.S(10, 6), caps=(False, False)), None, (0, 2.0, 0))
    inner = vm.star("soul_swirl", g_core)
    geo.merge(inner, geo.prim_octa(2.6, 3.4), None, (0, 2.0, 0))
    heart = vm.P("heart_gem", "crystal", group=g_c, base=(255, 190, 255), slot=GLOW, lod=False)
    geo.merge(heart, geo.prim_octa(1.2, 1.6), None, (0, 2.0, 0))
    # the cradle: four slender silver-white prongs under it and a ring of pale petals
    cage = vm.P("cradle", "silver", group=g_cage, base=(236, 228, 250), slot=TRIM, lod=False)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        c_, s_ = math.cos(a), math.sin(a)
        geo.tube_path(cage, geo.bez3((c_ * 1.0, -4.6, s_ * 1.0), (c_ * 3.6, -3.4, s_ * 3.6), (c_ * 4.8, -0.4, s_ * 4.8), (c_ * 3.2, 4.4, s_ * 3.2), vm.S(7, 3)), 0.3, sides=4, taper=(1.0, 0.6), caps=(True, True))
    foot = vm.P("cradle_foot", "silver", group=g_cage, base=(214, 206, 236), slot=TRIM, lod=False)
    geo.merge(foot, geo.prim_lathe([(-6.2, 0.5), (-5.8, 1.6), (-4.8, 0.9), (-4.4, 0.0)], sides=vm.S(10, 6), caps=(True, False)), None, (0, 0, 0))
    # dissolution: three thin rings that turn through each other, the wings of reishi behind
    for g, rad, tilt, nm, base_ in ((g_r1, 6.6, 0, "dissolve_ring", (240, 232, 255)), (g_r2, 7.6, 60, "dissolve_ring_b", (214, 180, 255)), (g_r3, 8.6, -60, "dissolve_ring_c", (255, 214, 250))):
        p = vm.P(nm, "silver", group=g, base=base_, slot=TRIM if nm != "dissolve_ring_b" else ACCENT, lod=False)
        geo.merge(p, geo.prim_torus(rad, 0.2, n=vm.S(24, 8), m=3), geo.rot_matrix("x", tilt), (0, 2.0, 0))
        vm.deco(PT.deco_glyphs, p, glow, rows=1, glow=True, strength=0.7, seed=3 + tilt % 7)
    wing = vm.P("reishi_wings", "crystal", group=g_wing, base=(206, 160, 250), slot=GLOW, lod=False)
    fe = geo.prim_leaf(11.0, 4.0, 0.2, sides=2, bend=0.0)
    for sx in (-1, 1):
        for k, (ang, ln) in enumerate(((54, 1.0), (34, 0.85), (14, 0.7))):
            geo.place(wing, fe, (sx * 1.6, 2.4 + 0.2 * k, -2.0), up=(sx * math.sin(math.radians(ang + 20)), math.cos(math.radians(ang + 20)) * 0.6, -0.45), spin=0.0, scale=ln)
    sh = vm.P("crystal_shards", "crystal", group=g_shards, base=(220, 180, 255), slot=GLOW, lod=False)
    for i in range(7):
        a = 2 * math.pi * i / 7
        geo.merge(sh, geo.prim_crystal(0.6, 3.4, sides=4, tip=0.5), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 16), (9.6 * math.cos(a), 2.0 + 1.6 * math.sin(i * 2.0), 9.6 * math.sin(a)))
    sh2 = orbit_motes(vm, "crystal_shards_far", g_shards2, 6, 12.8, 4.0, 0.7, (255, 214, 250), slot=ACCENT, wobble=2.0, phase=0.4)
    hal = vm.P("lilac_halo", "silver", group=g_hal, base=(236, 224, 255), slot=TRIM, lod=False)
    geo.merge(hal, geo.prim_torus(6.2, 0.34, n=vm.S(24, 8), m=3), geo.rot_matrix("x", 12), (0, 14.5, 0))
    vm.fx_disc(g_bound, GLOW, (0, 2.0, 0), (0, 1, 0), 30.0, "ripple")
    vm.fx_disc(g_bound, DARK, (0, 2.1, 0), (0, 1, 0), 21.0, "ticks")
    K.halo_set(vm, pal, y=2.0, R=10.0, rune=False, shaft=(-12.0, 22.0))
    vm.deco(PT.deco_veins, egg, glow, n=4, strength=0.8, seed=2, min_w=8, min_h=8)
    vm.deco(PT.deco_fill_glow, heart, glow, strength=1.0, seed=3)
    vm.deco(PT.deco_fill_glow, sh, glow, strength=0.9, seed=4)
    vm.deco(PT.deco_fill_glow, sh2, glow, strength=0.9, seed=5, facets=False)
    vm.deco(PT.deco_fill_glow, wing, glow, strength=0.7, seed=6, facets=False)
    vm.deco(PT.deco_glyphs, hal, glow, rows=1, glow=True, strength=0.6, seed=7)
    vm.deco(PT.deco_grain, cage, amount=0.08)


add(Item("hogyoku_of_the_crystal_dissolution", build_hogyoku, dict(style="swirl", dark=(10, 0, 22), mid=(120, 60, 180), hot=(255, 220, 255)), hand_size=0.95, hand_style="palm", seed=108,
         layers=[GL("soul swirl", "nebula_core", "soul_swirl"), GL("crystal shell", "aura_shell", "crystal"), GL("dissolve ring", "halo_ring", "dissolve_ring"), GL("dissolve ring b", "halo_ring", "dissolve_ring_b"),
                 GL("dissolve ring c", "halo_ring", "dissolve_ring_c"), GL("reishi wings", "petals", "reishi_wings"), GL("crystal shards", "orbit_shards", "crystal_shards"),
                 GL("far shards", "orbit_shards", "crystal_shards_far"), GL("boundary ripple", "portal_ripples", "boundary_ripple"), GL("lilac halo", "halo_ring", "lilac_halo"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("reishi motes", "motes", "nusmp:mana_mote", 0xD8A0FF, count=2, every=4, radius=0.5, rise=0.014),
                 PL("soul wisps", "wisps", "nusmp:soul_wisp", 0xE8D8FF, count=1, every=6, radius=0.5, rise=0.012),
                 PL("crystal glints", "glints", "nusmp:gem_sparkle", 0xFFFFFF, count=1, every=5, radius=0.45, rise=0.01)],
         acts={"fusion": [ST("SHADOW_UNITE", 0xD8A0FF, 3.4, 70, "self"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")],
               "boundary_dissolution": [ST("MAGIC_CIRCLE_EXPLOSION", 0xD8A0FF, 4.4, 60, "ground"), ST("SPACE_RINGGATE", 0xE8D8FF, 3.6, 60, "ground")],
               "transcendence": [ST("DIVINE_PILLAR", 0xD8A0FF, 4.0, 90, "ground"), ST("DIVINE_HALO", 0xE8D8FF, 3.6, 80, "sky"), ST("SOUL_VORTEX", 0xD8A0FF, 3.2, 70, "self")]},
         worn_scale=0.36, worn_offset=(11.6, -9.0, 0.0)))


# ====================================================================== SEVEN DRAGON BALLS OF SHENRON (belt)
def build_balls(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    BY, SQ = -10.8, 0.56
    g_b = vm.grp("belt", "bob", (0, BY, 0), 0.18, 0.05, 0.0)
    g_ball = vm.grp("dragon_balls", "spin", (0, BY, 0), 1.3)
    g_dragon = vm.grp("shenron", "spin", (0, BY, 0), -0.6)
    g_pouch = vm.grp("pouch", "sway", (5.6, BY - 1.0, -0.6), 3.0, 0.07, 0.0)
    g_stars = vm.grp("star_glow", "pulse", (0, BY, 0), 0.1, 0.2, 0.0)
    g_rad = vm.grp("radar", "spin", (0, BY, 0), 2.6)
    g_ring = vm.grp("wish_ring", "spin", (0, BY, 0), 0.9)
    g_cloud = vm.grp("storm_cloud", "pulse", (0, BY + 5.0, 0), 0.05, 0.08, 0.0)
    g_sparks = vm.grp("lightning_sparks", "tumble", (0, BY, 0), 0.4, 0.3, 1.1)
    g_gl = vm.grp("ball_glints", "pulse", (0, BY, -3.4), 0.14, 0.2, 1.0)
    # the belt: orange leather with a brass buckle, a pouch at the right hip
    band = vm.P("belt_band", "leather", group=g_b, base=(206, 108, 28), slot=MAIN, lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(BY - 1.0, 4.55), (BY - 0.6, 4.8), (BY + 0.6, 4.8), (BY + 1.0, 4.55)], sides=vm.S(24, 10), caps=(False, False), squash=SQ)
    edge = vm.P("belt_edges", "gold", group=g_b, base=GOLD, slot=TRIM, lod=False)
    for yy in (BY - 1.05, BY + 1.05):
        geo.lathe(edge, "y", (0, 0, 0), [(yy - 0.18, 4.62), (yy + 0.18, 4.88)], sides=vm.S(24, 10), caps=(False, False), squash=SQ)
    buckle = vm.P("buckle", "gold", group=g_b, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(buckle, geo.prim_lathe([(-0.2, 1.7), (0.0, 1.9), (0.4, 1.5), (0.5, 0.0)], sides=vm.S(12, 6), caps=(True, False)), geo.rot_matrix("x", -90), (0, BY, -2.95))
    star1 = vm.P("buckle_star", "crystal", group=g_stars, base=(255, 70, 40), slot=GLOW, lod=False)
    geo.merge(star1, geo.prim_octa(0.55, 0.7), None, (0, BY, -3.85))
    pouch = vm.P("pouch", "leather", group=g_pouch, base=(176, 90, 24), slot=MAIN, lod=False)
    geo.merge(pouch, geo.prim_box(3.4, 3.0, 2.4, chamfer=0.4), None, (5.6, BY - 2.6, -0.6))
    flap = vm.P("pouch_flap", "leather", group=g_pouch, base=(150, 74, 20), slot=ACCENT, lod=False)
    geo.merge(flap, geo.prim_box(3.6, 1.4, 2.6, chamfer=0.3), None, (5.6, BY - 1.5, -0.6))
    tie = vm.P("pouch_tie", "gold", group=g_pouch, base=GOLD, slot=TRIM, lod=False)
    geo.merge(tie, geo.prim_sphere(0.4, 3, 6), None, (5.6, BY - 2.2, -1.9))
    # the seven balls on a ring round the waist: amber spheres, red stars on the outer face (1 to 7)
    layout = {1: [(0, 0)], 2: [(-0.5, 0.45), (0.5, -0.45)], 3: [(0, 0.7), (-0.7, -0.5), (0.7, -0.5)], 4: [(-0.6, 0.6), (0.6, 0.6), (-0.6, -0.6), (0.6, -0.6)],
              5: [(0, 0), (-0.75, 0.75), (0.75, 0.75), (-0.75, -0.75), (0.75, -0.75)], 6: [(-0.5, 0.85), (0.5, 0.85), (-0.5, 0.0), (0.5, 0.0), (-0.5, -0.85), (0.5, -0.85)],
              7: [(0, 0), (-0.8, 0.0), (0.8, 0.0), (-0.45, 0.85), (0.45, 0.85), (-0.45, -0.85), (0.45, -0.85)]}
    balls = vm.P("dragon_balls", "crystal", group=g_ball, base=(246, 150, 40), slot=MAIN, lod=True)
    sts = vm.P("ball_stars", "crystal", group=g_ball, base=(220, 40, 30), slot=TRIM, lod=False)
    R = 7.4
    for i in range(7):
        a = 2 * math.pi * i / 7
        c = np.array([R * math.cos(a), BY, R * math.sin(a)])
        geo.merge(balls, geo.prim_sphere(1.55, vm.S(4, 2), vm.S(8, 4)), None, c)
        out = np.array([math.cos(a), 0.0, math.sin(a)])
        up = np.array([0.0, 1.0, 0.0])
        side = np.cross(up, out)
        for (u, v) in layout[i + 1]:
            pos = c + out * 1.5 + side * u * 0.75 + up * v * 0.75
            geo.merge(sts, geo.prim_octa(0.26, 0.12), geo.align_y(out), pos)
    # Shenron, a green dragon coiled once round the waist, head at the front with horns and whiskers
    dr = vm.P("shenron", "crystal", group=g_dragon, base=(70, 190, 90), slot=ACCENT, lod=False)
    path = [(5.7 * math.cos(t), BY + 1.2 + 0.7 * math.sin(2 * t), 5.7 * SQ * 1.15 * math.sin(t)) for t in np.linspace(0.3, 2 * math.pi + 1.4, vm.S(24, 10))]
    geo.tube_path(dr, path, 0.9, sides=vm.S(6, 4), taper=(0.25, 1.0), caps=(True, True))
    hd = path[-1]
    geo.merge(dr, geo.prim_sphere(1.3, 3, vm.S(7, 5), squash=0.9), None, hd)
    horns = vm.P("shenron_horns", "gold", group=g_dragon, base=GOLD_HI, slot=TRIM, lod=False)
    for sg in (-1, 1):
        geo.merge(horns, geo.prim_spike(0.25, 2.2, sides=4), geo.rot_matrix("x", 40) @ geo.rot_matrix("z", sg * 20), (hd[0] + sg * 0.5, hd[1] + 0.9, hd[2] - 0.2))
        geo.tube_path(horns, [(hd[0] + sg * 0.3, hd[1], hd[2] + 0.3), (hd[0] + sg * 2.0, hd[1] - 0.5, hd[2] + 0.9), (hd[0] + sg * 3.2, hd[1] + 0.3, hd[2] + 1.6)], 0.08, sides=3, caps=(True, True))
    eyes = vm.P("shenron_eyes", "crystal", group=g_dragon, base=(255, 60, 40), slot=GLOW, lod=False)
    for sg in (-1, 1):
        geo.merge(eyes, geo.prim_octa(0.2, 0.26), None, (hd[0] + sg * 0.55, hd[1] + 0.35, hd[2] - 0.7))
    # the radar of the balls, the wish ring, a storm cloud and its sparks
    rd = vm.P("radar", "gold", group=g_rad, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(rd, geo.prim_torus(9.8, 0.2, n=vm.S(30, 10), m=3), None, (0, BY, 0))
    for i in range(7):
        a = 2 * math.pi * i / 7
        geo.merge(rd, geo.prim_octa(0.3, 0.4), None, (9.8 * math.cos(a), BY, 9.8 * math.sin(a)))
    cloud = vm.star("storm_cloud", g_cloud)
    geo.merge(cloud, geo.prim_octa(3.0, 2.2), None, (0, BY + 5.0, 0))
    sp = orbit_motes(vm, "lightning_sparks", g_sparks, 6, 11.6, BY, 0.5, (255, 226, 90), slot=GLOW, wobble=2.0)
    vm.fx_disc(g_ring, GLOW, (0, BY - 0.4, 0), (0, 1, 0), 24.0, "seal")
    vm.fx_disc(g_ring, DARK, (0, BY - 0.2, 0), (0, 1, 0), 17.0, "rune")
    vm.fx_cross(g_gl, ACCENT, (0, BY, -R - 1.0), (0, 1, 0), 4.4, "glint", n=2)
    K.halo_set(vm, pal, y=BY, R=8.0, rune=False, shaft=(-24.0, 6.0))
    vm.deco(PT.deco_fill_glow, sts, (255, 70, 40), strength=0.9, seed=3, facets=False)
    vm.deco(PT.deco_fill_glow, star1, (255, 70, 40), strength=0.9, seed=4, facets=False)
    vm.deco(PT.deco_veins, dr, (190, 255, 160), n=2, strength=0.6, seed=5, min_w=6, min_h=6)
    vm.deco(PT.deco_fill_glow, eyes, (255, 80, 50), strength=1.0, seed=6, facets=False)
    vm.deco(PT.deco_fill_glow, sp, glow, strength=0.9, seed=7, facets=False)
    vm.deco(PT.deco_grain, band, amount=0.16)
    vm.deco(PT.deco_filigree, buckle, trim, seed=8, min_w=8, min_h=8)


add(Item("seven_dragon_balls_of_shenron", build_balls, dict(style="lava", dark=(24, 6, 0), mid=(200, 100, 10), hot=(255, 230, 140)), hand_size=1.0, hand_style="coil", seed=109, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("storm cloud", "nebula_core", "storm_cloud"), GL("dragon balls", "orbit_shards", "dragon_balls"), GL("shenron", "ribbons", "shenron"), GL("star glow", "aura_shell", "star_glow"),
                 GL("pouch", "aura_shell", "pouch"), GL("radar", "halo_ring", "radar"), GL("wish ring", "rune_ring", "wish_ring"), GL("lightning sparks", "orbit_shards", "lightning_sparks"),
                 GL("ball glints", "lens_flare", "ball_glints"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("ball embers", "embers", "nusmp:ember", 0xFFC040, count=1, every=4, radius=0.55, rise=0.018),
                 PL("dragon sparks", "sparks", "nusmp:spark", 0xFFE070, count=1, every=7, radius=0.6, rise=0.0),
                 PL("star glints", "glints", "nusmp:star4", 0xFF6A40, count=1, every=6, radius=0.45, rise=0.012)],
         acts={"dragon_radar": [ST("COMPASS_ARRAY", 0xFFC040, 3.6, 70, "ground"), ST("STAR_MAGIC", 0xFF6A40, 2.6, 50, "self")],
               "eternal_dragons_roar": [ST("LIGHTNING_FIEND", 0xFFE070, 3.4, 40, "self"), ST("FIRE_BURST", 0xFFC040, 3.4, 40, "front"), ST("FIRE_WILD", 0xFF6A40, 3.0, 40, "ground")],
               "shenrons_wish": [ST("DIVINE_PILLAR", 0xFFC040, 4.4, 100, "ground"), ST("LIGHTNING_GOD", 0xFFE070, 3.6, 70, "sky"), ST("DIVINE_HALO", 0xFF6A40, 3.6, 80, "sky")]}))

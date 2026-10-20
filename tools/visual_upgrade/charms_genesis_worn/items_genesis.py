"""Genesis grade charms: the Holy Grail Fragment, the Philosopher's Stone Shard and the Prison Realm Cube."""
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


# ====================================================================== HOLY GRAIL FRAGMENT
def build_grail(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_cup = vm.grp("cup", "bob", (0, 0, 0), 0.4, 0.05, 0.0)
    g_liquid = vm.grp("wish_light", "pulse", (0, 6, 0), 0.05, 0.1, 0.0)
    g_col = vm.grp("light_pillar", "pulse", (0, 14, 0), 0.07, 0.07, 0.0)
    g_wings = vm.grp("angel_wings", "sway", (0, 4, 0), 3.0, 0.05, 0.0)
    g_shards = vm.grp("cup_shards", "tumble", (0, 6, 0), 0.4, 0.3, 1.0)
    g_shards2 = vm.grp("cup_shards_far", "spin", (0, 8, 0), -1.4)
    g_wish = vm.grp("wish_orbs", "spin", (0, 6, 0), 2.4)
    g_halo = vm.grp("angel_halo", "spin", (0, 22, 0), 1.0)
    g_vines = vm.grp("gold_vines", "sway", (0, -6, 0), 1.5, 0.06, 0.0)
    g_feath = vm.grp("drifting_feathers", "spin", (0, 4, 0), 0.9)
    # the cup: a bowl with the broken crown of its rim, a stem with a knop, a stepped foot - all hammered gold
    bowl = vm.P("bowl", "gold", group=g_cup, base=(236, 196, 84), lod=True)
    geo.lathe(bowl, "y", (0, 0, 0), [(0.2, 1.4), (1.2, 3.2), (3.0, 6.0), (5.4, 7.6), (7.8, 7.4), (9.4, 6.6), (9.9, 6.0)], sides=vm.S(20, 8), caps=(True, False))
    rim = vm.P("broken_rim", "gold", group=g_cup, base=(250, 214, 110), slot=TRIM, lod=False)
    rng = np.random.default_rng(8)
    for i in range(10):
        a = 2 * math.pi * i / 10
        h = rng.uniform(1.0, 5.6) * (1.0 if 0.4 < (i / 10) < 0.9 else 0.35)
        geo.merge(rim, geo.prim_spike(0.7, h, sides=4), geo.rot_matrix("z", rng.uniform(-14, 14)), (6.1 * math.cos(a), 9.8, 6.1 * math.sin(a)))
    stem = vm.P("stem", "gold", group=g_cup, base=(224, 180, 74), lod=True)
    geo.lathe(stem, "y", (0, 0, 0), [(-10.0, 4.6), (-9.0, 5.6), (-8.0, 3.6), (-6.4, 1.3), (-4.6, 1.1), (-3.6, 2.4), (-2.6, 1.0), (-1.4, 1.4), (0.2, 1.4)], sides=vm.S(12, 6), caps=(True, False))
    knop_gems = vm.P("knop_gems", "crystal", group=g_cup, base=(200, 30, 50), slot=GLOW, lod=False)
    cab = geo.prim_lathe([(0.0, 0.9), (0.9, 0.0)], sides=vm.S(6, 4), caps=(True, False))
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(knop_gems, cab, geo.align_y((math.cos(a), 0, math.sin(a))), (2.45 * math.cos(a), -3.6, 2.45 * math.sin(a)))
    bowl_gems = vm.P("bowl_gems", "crystal", group=g_cup, base=(220, 40, 60), slot=GLOW, lod=False)
    for i in range(8):
        a = 2 * math.pi * i / 8 + 0.2
        geo.merge(bowl_gems, geo.prim_octa(0.7, 1.0), geo.align_y((math.cos(a), 0.3, math.sin(a))), (7.45 * math.cos(a), 6.2, 7.45 * math.sin(a)))
    feet = vm.P("foot_rings", "gold", group=g_cup, base=(255, 224, 120), slot=TRIM, lod=False)
    for yy, rr in ((-9.6, 5.4), (-8.3, 4.0)):
        geo.merge(feet, geo.prim_torus(rr, 0.36, n=vm.S(14, 8), m=3), None, (0, yy, 0))
    # vines of gold wire climbing the stem and bowl, in the same hand as a cathedral's ironwork
    vines = vm.P("gold_vines", "gold", group=g_vines, base=(255, 222, 130), slot=TRIM, lod=False)
    for k in range(2):
        geo.spiral_tube(vines, (0, -1.0, 0), 5.0 + 1.0 * k, 12.0, 1.4, 0.28, n=vm.S(14, 6), sides=3, phase=k * math.pi, taper=0.2)
    # the wish: light pooled in the cup, a pillar rising, the broken pieces of the rim circling
    pool = vm.star("wish_light", g_liquid)
    geo.merge(pool, geo.prim_lathe([(-0.3, 6.6), (0.3, 6.6)], sides=vm.S(14, 6), caps=(True, True)), None, (0, 8.6, 0))
    pillar = vm.star("pillar", g_col)
    geo.merge(pillar, geo.prim_lathe([(-6.0, 5.2), (0.0, 4.0), (8.0, 2.4), (20.0, 0.8)], sides=vm.S(10, 5), caps=(False, False)), None, (0, 16.0, 0))
    sh = vm.P("cup_shards", "gold", group=g_shards, base=(246, 206, 94), slot=TRIM, lod=False)
    lf = geo.prim_leaf(4.4, 2.6, 0.45, sides=2, bend=0.05)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(sh, lf, geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 100 - 12 * i), (11.0 * math.cos(a), 6.0 + 3.0 * math.sin(i * 1.9), 11.0 * math.sin(a)))
    sh2 = vm.P("cup_shards_far", "gold", group=g_shards2, base=(255, 232, 150), slot=TRIM, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5 + 0.5
        geo.merge(sh2, geo.prim_leaf(3.0, 1.8, 0.35, sides=2, bend=0.04), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 70 + 15 * i), (15.0 * math.cos(a), 9.0 - 3.0 * math.sin(i * 1.3), 15.0 * math.sin(a)))
    orbs = K.orbs(vm, "wish_orbs", g_wish, 8, 8.4, 12.0, 0.7, "crystal", (255, 236, 150), slot=GLOW, phase=0.0, wobble=1.5, lod=False)
    # the angel's halo over it all, wings of feathers behind it
    halo = vm.P("angel_halo", "gold", group=g_halo, base=(255, 232, 140), slot=TRIM, lod=False)
    geo.merge(halo, geo.prim_torus(7.0, 0.5, n=vm.S(24, 8), m=4), geo.rot_matrix("x", 14), (0, 23.0, 0))
    wings = vm.P("angel_wings", "bone", group=g_wings, base=(255, 250, 232), lod=False)
    fe = geo.prim_leaf(8.0, 2.4, 0.3, sides=2, bend=0.02)
    for sg in (-1, 1):
        for k in range(3):
            geo.merge(wings, fe, geo.rot_matrix("z", sg * (-62 + 20 * k)) @ geo.rot_matrix("y", -sg * 18), (sg * 3.2, 5.0 + 2.2 * k, 5.8 + 0.3 * k))
    fea = vm.P("drifting_feathers", "bone", group=g_feath, base=(255, 252, 238), lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5
        geo.merge(fea, geo.prim_leaf(2.6, 1.0, 0.1, sides=2, bend=0.05), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 95), (13.0 * math.cos(a), 18.0 - 3.4 * i, 13.0 * math.sin(a)))
    # additive layers: a ground circle, the rays of the grail, flare, shafts
    g_gc = vm.grp("ground_circle", "spin", (0, -10, 0), 0.7)
    vm.fx_disc(g_gc, GLOW, (0, -10.4, 0), (0, 1, 0), 34.0, "seal")
    vm.fx_disc(g_gc, DARK, (0, -10.2, 0), (0, 1, 0), 26.0, "rune")
    g_rays = vm.grp("grail_rays", "pulse", (0, 9, 0), 0.08, 0.06, 0.0)
    vm.fx_cross(g_rays, ACCENT, (0, 9, 0), (0, 1, 0), 40.0, "rays", n=2)
    g_rip = vm.grp("wish_ripple", "pulse", (0, 10, 0), 0.25, 0.1, 0.0)
    vm.fx_disc(g_rip, GLOW, (0, 10.0, 0), (0, 1, 0), 22.0, "ripple")
    K.halo_set(vm, pal, y=9.0, R=11.0, rune=False, shaft_len=36.0)
    vm.deco(PT.deco_glyphs, bowl, glow, text="HOLY GRAIL WISH", rows=2, glow=False, seed=3)
    vm.deco(PT.deco_veins, bowl, glow, n=4, strength=0.9, seed=4, min_w=12, min_h=12)
    vm.deco(PT.deco_filigree, stem, trim, seed=5)
    vm.deco(PT.deco_fill_glow, knop_gems, (255, 60, 80), strength=0.9, seed=6)
    vm.deco(PT.deco_fill_glow, bowl_gems, (255, 60, 80), strength=0.9, seed=7)
    vm.deco(PT.deco_fill_glow, orbs, glow, strength=0.9, seed=8)
    vm.deco(PT.deco_enamel, rim, (255, 240, 170), (255, 205, 90), seed=9)
    vm.deco(PT.deco_grain, bowl, amount=0.2)
    vm.deco(PT.deco_grain, stem, amount=0.16)
    vm.deco(PT.deco_glyphs, halo, glow, rows=1, glow=False, seed=10)


add(Item("holy_grail_fragment", build_grail, dict(style="swirl", dark=(18, 10, 0), mid=(170, 124, 20), hot=(255, 240, 170)), hand_size=1.0, hand_style="palm", seed=29,
         layers=[GL("wish light pooled in the cup", "nebula_core", "wish_light"), GL("pillar of light", "light_shafts", "light_pillar"), GL("angel wings", "petals", "angel_wings"),
                 GL("cup shards", "orbit_shards", "cup_shards"), GL("far cup shards", "orbit_shards", "cup_shards_far"), GL("wish orbs", "orbit_shards", "wish_orbs"),
                 GL("angel halo", "halo_ring", "angel_halo"), GL("gold vines", "ribbons", "gold_vines"), GL("drifting feathers", "petals", "drifting_feathers"),
                 GL("ground seal circle", "rune_ring", "ground_circle"), GL("grail rays", "lens_flare", "grail_rays"), GL("wish ripple", "portal_ripples", "wish_ripple"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("rising wish motes", "motes", "end_rod", None, count=2, every=3, radius=0.5, rise=0.03),
                 PL("holy glints", "glints", "dust", 0xFFF0A0, count=1, every=4, radius=0.6, rise=0.015),
                 PL("falling gold", "drips", "dust", 0xE8D070, count=1, every=6, radius=0.6, rise=-0.012)],
         acts={"wish": [ST("DIVINE_PILLAR", 0xFFF0A0, 3.4, 80, "ground"), ST("DIVINE_HALO", 0xE8D070, 3.2, 70, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 2.0, 16, "self")],
               "the_cup_overflows": [ST("DIVINE_PILLAR", 0xE8D070, 4.0, 90, "ground"), ST("MAGIC_CIRCLE_EXPLOSION", 0xFFF0A0, 4.4, 60, "ground"), ST("DIVINE_SIGIL", 0xFFF0A0, 3.0, 70, "sky")]},
         worn_scale=0.36, worn_offset=(11.8, -9.0, 0.0)))


# ====================================================================== PHILOSOPHER'S STONE SHARD
def build_philosopher(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_stone = vm.grp("stone", "bob", (0, 0, 0), 0.4, 0.06, 0.0)
    g_souls_core = vm.grp("soul_core", "pulse", (0, 5, 0), 0.07, 0.14, 0.0)
    g_souls_in = vm.grp("soul_swirl", "tumble", (0, 5, 0), 0.0, 0.0, 1.8)
    g_circle = vm.grp("transmutation_circle", "spin", (0, -9, 0), 0.8)
    g_circle2 = vm.grp("transmutation_inner", "spin", (0, -9, 0), -1.4)
    g_plates = vm.grp("element_plates", "spin", (0, 6, 0), 1.6)
    g_wire = vm.grp("gold_wire", "sway", (0, 0, 0), 1.2, 0.05, 0.0)
    g_souls = vm.grp("rising_souls", "spin", (0, 6, 0), 2.2)
    g_souls2 = vm.grp("rising_souls_far", "spin", (0, 8, 0), -1.2)
    g_arcs = vm.grp("chain_lightning", "pulse", (0, 5, 0), 0.3, 0.45, 0.0)
    g_burst = vm.grp("shard_burst", "pulse", (0, 5, 0), 0.04, 0.2, 0.0)
    # a cluster of red crystal, raised from a ritual base: three great columns in the nebula pass (the people inside it), smaller ones lit from within
    cl = vm.star("great_columns", g_souls_core)
    for (h, r, tilt, az, off) in ((17.0, 3.4, 8, 0, (0, 5.0, 0)), (13.0, 2.7, 22, 130, (-1.0, 2.5, 1.0)), (11.0, 2.4, -20, 250, (1.4, 1.5, -1.2))):
        geo.merge(cl, geo.prim_crystal(r, h, sides=6, tip=0.4), geo.rot_matrix("y", az) @ geo.rot_matrix("z", tilt), off)
    sm = vm.P("small_crystals", "crystal", group=g_burst, base=(220, 50, 40), slot=GLOW, lod=False)
    rng = np.random.default_rng(31)
    for i in range(11):
        a = rng.uniform(0, 6.28)
        el = math.radians(rng.uniform(5, 75))
        d = np.array([math.cos(a) * math.cos(el), math.sin(el), math.sin(a) * math.cos(el)])
        geo.place(sm, geo.prim_crystal(0.9, rng.uniform(4.0, 7.0), sides=5, tip=0.5), d * 4.4 + (0, -1.0, 0), up=d, scale=1.0)
    base = vm.P("ritual_bowl", "iron", group=g_stone, base=(78, 24, 22), lod=True)
    geo.lathe(base, "y", (0, 0, 0), [(-13.0, 3.0), (-12.0, 6.6), (-10.0, 8.0), (-7.4, 7.2), (-5.4, 4.4), (-4.0, 3.6)], sides=vm.S(16, 6), caps=(True, False))
    claws = K.claws(vm, "foot_claws", g_stone, 5, 7.0, -10.0, -2.0, 0.6, "gold", (236, 206, 140), curl=0.5, segs=4, sides=3, phase=0.0, lod=False)
    # the circle: a transmutation circle cut in a ring plate, with its inner hexagram, drawn on the ground
    ring = vm.P("circle_ring", "gold", group=g_circle, base=(214, 180, 110), slot=TRIM, lod=False)
    geo.merge(ring, geo.prim_torus(13.0, 0.45, n=vm.S(28, 8), m=3), None, (0, -13.4, 0))
    geo.merge(ring, geo.prim_torus(11.0, 0.32, n=vm.S(24, 8), m=3), None, (0, -13.4, 0))
    star6 = vm.P("circle_hexagram", "gold", group=g_circle2, base=(240, 214, 150), slot=TRIM, lod=False)
    pts = [(10.0 * math.cos(math.radians(90 + 60 * i)), -13.4, 10.0 * math.sin(math.radians(90 + 60 * i))) for i in range(6)]
    for i in range(6):
        geo.tube_path(star6, [pts[i], pts[(i + 2) % 6]], 0.28, sides=3, caps=(False, False))
    plates = vm.P("element_plates", "gold", group=g_plates, base=(236, 206, 140), slot=TRIM, lod=False)
    for i in range(4):
        a = 2 * math.pi * i / 4
        geo.merge(plates, geo.prim_card(4.2, 4.2, 0.4), geo.rot_matrix("y", -math.degrees(a) + 90), (11.0 * math.cos(a), 5.0 + 2.0 * math.sin(i * 2.0), 11.0 * math.sin(a)))
    wire = vm.P("gold_wire", "gold", group=g_wire, base=(255, 224, 150), slot=TRIM, lod=False)
    for k in range(2):
        geo.spiral_tube(wire, (0, 5.0, 0), 5.2 + k * 0.8, 15.0, 2.0, 0.22, n=vm.S(20, 6), sides=3, phase=k * math.pi, taper=0.5)
    souls = K.orbs(vm, "rising_souls", g_souls, 8, 9.0, 6.0, 0.55, "crystal", (255, 130, 110), slot=GLOW, phase=0.2, wobble=4.0, lod=False)
    souls2 = K.orbs(vm, "rising_souls_far", g_souls2, 6, 13.0, 9.0, 0.45, "crystal", (255, 170, 150), slot=ACCENT, phase=0.7, wobble=-5.0, lod=False)
    for i in range(6):
        a = math.radians(60 * i + 15)
        vm.fx_disc(g_arcs, ACCENT, (0, 5, 0), (math.cos(a), 0, math.sin(a)), 22.0, "bolt", rot=30 * i)
    vm.fx_disc(g_circle, GLOW, (0, -13.2, 0), (0, 1, 0), 36.0, "seal")
    vm.fx_disc(g_circle2, DARK, (0, -13.1, 0), (0, 1, 0), 28.0, "rune")
    K.halo_set(vm, pal, y=5.0, R=10.0, rune=False, shaft_len=36.0)
    vm.deco(PT.deco_fill_glow, sm, glow, strength=0.95, seed=3)
    vm.deco(PT.deco_fill_glow, souls, glow, strength=0.95, seed=4)
    vm.deco(PT.deco_glyphs, base, glow, text="EQUIVALENT EXCHANGE", rows=2, glow=True, strength=0.8, seed=5)
    vm.deco(PT.deco_glyphs, ring, glow, rows=1, glow=True, strength=0.8, seed=6)
    vm.deco(PT.deco_glyphs, plates, glow, text="FIRE WATER EARTH AIR", rows=2, glow=True, strength=0.9, seed=7)
    vm.deco(PT.deco_filigree, wire, trim, seed=8)
    vm.deco(PT.deco_grain, base, amount=0.2)


add(Item("philosophers_stone_shard", build_philosopher, dict(style="swirl", dark=(24, 0, 0), mid=(170, 28, 22), hot=(255, 150, 120)), hand_size=1.0, hand_style="palm", seed=30,
         layers=[GL("people inside the stone", "nebula_core", "soul_core"), GL("crystal burst", "aura_shell", "shard_burst"), GL("transmutation circle", "rune_ring", "transmutation_circle"),
                 GL("inner hexagram", "rune_ring", "transmutation_inner"), GL("element plates", "orbit_shards", "element_plates"), GL("gold wire cage", "ribbons", "gold_wire"),
                 GL("rising souls", "orbit_shards", "rising_souls"), GL("far souls", "orbit_shards", "rising_souls_far"), GL("chain lightning", "energy_arcs", "chain_lightning"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("soul embers", "embers", "dust", 0xFF6A5A, count=2, every=3, radius=0.55, rise=0.02),
                 PL("alchemy sparks", "arcs", "electric_spark", None, count=1, every=5, radius=0.6, rise=0.0),
                 PL("red glints", "glints", "dust", 0xF0E0A0, count=1, every=6, radius=0.6, rise=0.01)],
         acts={"transmute": [ST("ALCHEMY_CIRCLE", 0xFF6A5A, 3.6, 70, "ground"), ST("LIGHTNING_FIEND", 0xFF3A2A, 2.4, 40, "self"), ST("LIGHT_FLARE", 0xFFB0A0, 1.6, 14, "self")],
               "equivalent_exchange": [ST("ALCHEMY_CIRCLE", 0xF0E0A0, 4.4, 80, "ground"), ST("SOUL_VORTEX", 0xC8302A, 3.0, 70, "self"), ST("MAGIC_CIRCLE_EXPLOSION", 0xFF6A5A, 3.6, 50, "ground")]},
         worn_scale=0.34, worn_offset=(11.8, -9.0, 0.0)))


# ====================================================================== PRISON REALM CUBE
def build_prison(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_cube = vm.grp("prison_cube", "bob", (0, 0, 0), 0.35, 0.05, 0.0)
    g_eye = vm.grp("sealed_eye", "pulse", (0, 0, -7.4), 0.08, 0.1, 0.0)
    g_core = vm.grp("inner_realm", "tumble", (0, 0, 0), 0.5, 0.0, 0.8)
    g_tags = vm.grp("sutra_tags", "sway", (0, 7, 0), 5.0, 0.07, 0.0)
    g_tags2 = vm.grp("sutra_tags_b", "sway", (0, 7, 0), -5.0, 0.06, 1.8)
    g_chain = vm.grp("binding_chains", "sway", (0, 7, 0), 2.0, 0.05, 0.4)
    g_domains = vm.grp("tiny_cubes", "tumble", (0, 0, 0), 0.9, 0.4, 1.3)
    g_seal = vm.grp("seal_circle", "spin", (0, 0, 0), 1.1)
    g_seal2 = vm.grp("seal_circle_b", "spin", (0, 0, 0), -1.5)
    # an off-white cube: six framed faces, black cord on every edge, knots at the corners, an eye on the front
    H = 6.4
    frame = vm.P("face_frames", "bone", group=g_cube, base=(232, 226, 208), lod=False)
    fb = geo.prim_box(11.8, 1.5, 1.4, caps=False)
    sb = geo.prim_box(1.4, 1.5, 9.4, caps=False)
    faces = [((0, 0, -1), geo.rot_matrix("x", -90)), ((0, 0, 1), geo.rot_matrix("x", 90)), ((1, 0, 0), geo.rot_matrix("z", -90)), ((-1, 0, 0), geo.rot_matrix("z", 90)), ((0, 1, 0), np.eye(3)), ((0, -1, 0), geo.rot_matrix("x", 180))]
    for (nrm, M) in faces:
        c = np.array(nrm) * H
        for off in ((0, 0, 5.0), (0, 0, -5.0)):
            geo.merge(frame, fb, M, tuple(c + M @ np.array(off)))
        for off in ((5.0, 0, 0), (-5.0, 0, 0)):
            geo.merge(frame, sb, M, tuple(c + M @ np.array(off)))
    edges = vm.P("black_cords", "leather", group=g_cube, base=(24, 20, 18), lod=True)
    eb = geo.prim_box(0.9, 2 * H + 1.2, 0.9, caps=False)
    for dx in (-1, 1):
        for dz in (-1, 1):
            geo.merge(edges, eb, None, (dx * H, 0, dz * H))
    for dy in (-1, 1):
        for dz in (-1, 1):
            geo.merge(edges, eb, geo.rot_matrix("z", 90), (0, dy * H, dz * H))
    for dy in (-1, 1):
        for dx in (-1, 1):
            geo.merge(edges, eb, geo.rot_matrix("x", 90), (dx * H, dy * H, 0))
    knots = vm.P("corner_knots", "bone", group=g_cube, base=(60, 46, 36), slot=TRIM, lod=False)
    for dx in (-1, 1):
        for dy in (-1, 1):
            for dz in (-1, 1):
                geo.merge(knots, geo.prim_octa(1.2, 1.3), None, (dx * H, dy * H, dz * H))
    # the sealed realm in each face: a pale disc of the nebula; and the eye on the front
    for k, (nrm, M) in enumerate(faces):
        disc = vm.star("face_%d" % k, g_core if k else g_eye, lod=(k < 1))
        geo.merge(disc, geo.prim_lathe([(-0.15, 4.2), (0.15, 4.2)], sides=vm.S(10, 6), caps=(True, True)), M, tuple(np.array(nrm) * (H + 0.05)))
    eye = vm.P("eye_lids", "gold", group=g_eye, base=(34, 26, 20), slot=TRIM, lod=True)
    geo.tube_path(eye, [(-4.2, 0.0, -H - 0.5), (-2.0, 1.9, -H - 0.6), (0.0, 2.4, -H - 0.65), (2.0, 1.9, -H - 0.6), (4.2, 0.0, -H - 0.5)], 0.38, sides=3)
    geo.tube_path(eye, [(-4.2, 0.0, -H - 0.5), (-2.0, -1.9, -H - 0.6), (0.0, -2.4, -H - 0.65), (2.0, -1.9, -H - 0.6), (4.2, 0.0, -H - 0.5)], 0.38, sides=3)
    pupil = vm.P("eye_pupil", "crystal", group=g_eye, base=(250, 250, 250), slot=GLOW, lod=False)
    geo.merge(pupil, geo.prim_octa(0.55, 2.0), None, (0, 0, -H - 0.7))
    # sutra tags tied on every edge, black chains binding it, tiny cubes of sealed things circling
    tags = vm.P("sutra_tags", "bone", group=g_tags, base=(238, 232, 214), lod=False)
    tags2 = vm.P("sutra_tags_b", "bone", group=g_tags2, base=(232, 224, 206), lod=False)
    for j, part in enumerate((tags, tags2)):
        for k in range(5):
            x = -4.8 + 2.4 * k + 1.0 * j
            z = -H - 0.7 if (k + j) % 2 else H + 0.7
            geo.ribbon(part, [(x, H, z), (x + 0.2, H - 3.0, z + 0.2), (x - 0.2, H - 6.2, z), (x + 0.1, H - 9.2, z - 0.1)], 1.5, thick=0.1, up=(0, 0, 1), taper=0.2)
    ch = vm.P("binding_chains", "iron", group=g_chain, base=(26, 24, 30), lod=False)
    geo.chain(ch, [(-H - 1.0, H, 0), (-H - 2.2, 0.0, 0.5), (-H - 1.0, -H, 0)], link_r=1.0, wire=0.26, n=6, m=3, every=2.4)
    geo.chain(ch, [(H + 1.0, H, 0), (H + 2.2, 0.0, -0.5), (H + 1.0, -H, 0)], link_r=1.0, wire=0.26, n=6, m=3, every=2.4)
    tiny = vm.P("tiny_cubes", "bone", group=g_domains, base=(220, 214, 196), lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(tiny, geo.prim_box(1.5, 1.5, 1.5, caps=True), geo.rot_matrix("y", 25 * i) @ geo.rot_matrix("x", 18 * i), (11.0 * math.cos(a), 3.0 * math.sin(i * 1.6), 11.0 * math.sin(a)))
    vm.fx_disc(g_seal, GLOW, (0, 0, 0), (0, 1, 0), 34.0, "seal")
    vm.fx_disc(g_seal2, DARK, (0, 0, 0), (1, 0, 0), 30.0, "seal")
    K.halo_set(vm, pal, y=0.0, R=10.0, rune=True, shaft_len=30.0)
    vm.deco(PT.deco_glyphs, frame, glow, text="PRISON REALM SEALED", rows=1, glow=False, seed=3, min_w=10)
    vm.deco(PT.deco_glyphs, tags, (255, 60, 40), text="SEAL SEAL", rows=3, glow=True, strength=0.9, seed=4, vertical=False)
    vm.deco(PT.deco_glyphs, tags2, (255, 60, 40), text="SEAL", rows=3, glow=True, strength=0.9, seed=5)
    vm.deco(PT.deco_fill_glow, pupil, glow, strength=1.0, seed=6)
    vm.deco(PT.deco_grain, frame, amount=0.1)
    vm.deco(PT.deco_filigree, eye, trim, seed=7)


add(Item("prison_realm_cube", build_prison, dict(style="void", dark=(12, 12, 14), mid=(120, 118, 110), hot=(255, 255, 255)), hand_size=0.9, hand_style="palm", seed=31,
         layers=[GL("sealed eye", "aura_shell", "sealed_eye"), GL("inner realm faces", "nebula_core", "inner_realm"), GL("sutra tags", "ribbons", "sutra_tags"), GL("sutra tags b", "ribbons", "sutra_tags_b"),
                 GL("binding chains", "chains", "binding_chains"), GL("tiny sealed cubes", "orbit_shards", "tiny_cubes"), GL("seal circle", "rune_ring", "seal_circle"), GL("seal circle b", "rune_ring", "seal_circle_b"),
                 GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("cursed ash", "drips", "dust", 0xDDDDDD, count=1, every=4, radius=0.5, rise=0.006),
                 PL("seal sparks", "sparks", "end_rod", None, count=1, every=7, radius=0.6, rise=0.0),
                 PL("red ink", "wisps", "dust", 0xC82A1A, count=1, every=9, radius=0.5, rise=0.01)],
         acts={"seal": [ST("SEAL_CHAINS", 0xFFFFFF, 2.6, 70, "front"), ST("SEAL_TRINITY", 0xE8E4D4, 2.6, 70, "ground"), ST("SPACE_CUBE", 0xFFFFFF, 2.0, 70, "front")],
               "lock_the_room": [ST("SPACE_CUBE", 0xFFFFFF, 4.0, 90, "self"), ST("SEAL_TRINITY", 0xFFFFFF, 3.4, 80, "ground"), ST("SEAL_CHAINS", 0xE8E4D4, 3.4, 80, "self")]},
         worn_scale=0.4, worn_offset=(11.4, -9.0, 0.0)))

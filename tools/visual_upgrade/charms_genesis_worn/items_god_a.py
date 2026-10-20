"""God grade charms, first half: the Deck of Many Things, Warren's Key, the Rose Petal, the Administrator's Authority Key, the Archivist's Starbound Codex, the Behelit Egg."""
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


# ====================================================================== DECK OF MANY THINGS
def build_deck(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_case = vm.grp("case", "bob", (0, 0, 0), 0.35, 0.06, 0.0)
    g_fan = vm.grp("card_fan", "sway", (0, -2, 0), 2.5, 0.05, 0.0)
    g_orbit = vm.grp("fate_cards", "spin", (0, 4, 0), 2.1)
    g_orbit2 = vm.grp("fate_cards_far", "spin", (0, 6, 0), -1.3)
    g_sky = vm.grp("fate_sky", "pulse", (0, 3, 1.5), 0.04, 0.1, 0.0)
    g_seal = vm.grp("fate_seal", "pulse", (0, 4, 3), 0.1, 0.08, 0.0)
    # the case: a black enamel box with a gold edge, a hinge and a jewelled clasp
    case = vm.P("case", "obsidian", group=g_case, base=(28, 28, 40), lod=True)
    geo.merge(case, geo.prim_box(9.4, 8.0, 3.4, chamfer=0.5), None, (0, -6.0, 0))
    edge = vm.P("case_edge", "gold", group=g_case, base=(232, 190, 80), slot=TRIM, lod=False)
    for dy in (-10.1, -1.9):
        geo.merge(edge, geo.prim_box(10.0, 0.5, 3.9, chamfer=0.1), None, (0, dy, 0))
    for dx in (-4.85, 4.85):
        geo.merge(edge, geo.prim_box(0.5, 8.4, 3.9, chamfer=0.1), None, (dx, -6.0, 0))
    corners = vm.P("case_corners", "gold", group=g_case, base=(255, 220, 120), slot=TRIM)
    for dx in (-4.85, 4.85):
        for dy in (-10.1, -1.9):
            geo.merge(corners, geo.prim_octa(0.9, 1.0), None, (dx, dy, -1.95))
    clasp = vm.P("clasp", "crystal", group=g_case, base=(255, 200, 90), slot=GLOW)
    geo.merge(clasp, geo.prim_lathe([(0.0, 1.5), (0.6, 1.2), (1.3, 0.0)], sides=vm.S(8, 4), caps=(True, False)), geo.rot_matrix("x", -90), (0, -3.2, -1.7))
    # the fan of cards rising from the case (22 in the deck, nine shown)
    fan = vm.P("cards", "bone", group=g_fan, base=(36, 30, 52), lod=False)
    card = geo.prim_card(5.6, 9.0, 0.28)
    nfan = 9
    for i in range(nfan):
        a = -48 + 96 * i / (nfan - 1)
        geo.merge(fan, card, geo.rot_matrix("z", -a) @ geo.rot_matrix("y", 6 * (i % 2)), (math.sin(math.radians(a)) * 5.0, -2.0 + math.cos(math.radians(a)) * 4.6, 0.18 * i))
    fates = vm.P("fate_cards", "bone", group=g_orbit, base=(46, 36, 66))
    c2 = geo.prim_card(3.6, 5.6, 0.2)
    for i in range(5):
        th = 2 * math.pi * i / 5
        geo.merge(fates, c2, geo.rot_matrix("y", -math.degrees(th) + 90) @ geo.rot_matrix("x", 10), (10.0 * math.cos(th), 4.0 + 2.0 * math.sin(i * 1.9), 10.0 * math.sin(th)))
    fates2 = vm.P("fate_cards_far", "bone", group=g_orbit2, base=(62, 44, 84), lod=False)
    for i in range(4):
        th = 2 * math.pi * i / 4 + 0.7
        geo.merge(fates2, geo.prim_card(2.6, 4.0, 0.15), geo.rot_matrix("y", -math.degrees(th) + 90) @ geo.rot_matrix("z", 14 * i), (14.0 * math.cos(th), 7.0 - 3.0 * math.sin(i * 1.3), 14.0 * math.sin(th)))
    # a starry sky behind the fan (the deck's wonders), and a seal circle above it
    sky = vm.star("sky", g_sky)
    geo.merge(sky, geo.prim_lathe([(-0.2, 9.0), (0.2, 9.0)], sides=vm.S(12, 6), caps=(True, True)), geo.rot_matrix("x", 90), (0, 3.0, 1.6))
    vm.fx_disc(g_seal, GLOW, (0, 3.0, 2.4), (0, 0, 1), 26.0, "seal")
    K.halo_set(vm, pal, y=3.0, R=9.0, rune=False, shaft_len=18.0)
    vm.deco(PT.deco_glyphs, case, glow, text="XXII FATE", rows=2, glow=False, seed=3)
    vm.deco(PT.deco_filigree, fan, trim, seed=4, lit=glow)
    vm.deco(PT.deco_filigree, fates, trim, seed=5)
    vm.deco(PT.deco_fill_glow, clasp, glow, strength=0.95, seed=6)
    vm.deco(PT.deco_grain, edge, amount=0.1)


add(Item("deck_of_many_things", build_deck, dict(style="swirl", dark=(4, 2, 12), mid=(130, 96, 34), hot=(255, 214, 120)), hand_size=0.62, hand_style="palm", seed=17,
         layers=[GL("card fan", "ribbons", "card_fan"), GL("fate cards", "orbit_shards", "fate_cards"), GL("far fate cards", "orbit_shards", "fate_cards_far"), GL("starry sky", "starfield", "fate_sky"),
                 GL("fate seal", "rune_ring", "fate_seal"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("gold dust", "motes", "dust", 0xFFD27A, count=1, every=4, radius=0.45, rise=-0.01),
                 PL("fate glints", "glints", "end_rod", None, count=1, every=8, radius=0.5, rise=0.01)],
         acts={"draw_one": [ST("SPELL_CARD", 0xFFD27A, 1.6, 40, "front"), ST("LIGHT_FLARE", 0xFFD27A, 1.2, 14, "front")],
               "draw_two": [ST("SPELL_CARD", 0xFFD27A, 1.6, 40, "front"), ST("MAGIC_CIRCLE", 0xFFD27A, 2.4, 40, "ground")],
               "draw_three": [ST("SPELL_CARD", 0xFFD27A, 1.8, 40, "front"), ST("MAGIC_CIRCLE", 0xFFD27A, 3.0, 44, "ground"), ST("DIVINE_HALO", 0xFFE6A0, 2.2, 44, "self")],
               "look": [ST("SPELL_CARD", 0xE6B94A, 1.2, 28, "front")]}))


# ====================================================================== WARREN'S KEY
def build_key(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_key = vm.grp("key", "bob", (0, 0, 0), 0.35, 0.06, 0.0)
    g_door = vm.grp("door_frame", "bob", (9, 0, 0), 0.5, 0.05, 1.0)
    g_rip = vm.grp("door_ripples", "pulse", (9, 0, 0), 0.2, 0.1, 0.0)
    g_keys = vm.grp("lost_keys", "spin", (0, 2, 0), 2.4)
    g_halo = vm.grp("maze_glow", "spin", (0, 14, 0), 1.2)
    # the bow is a labyrinth: concentric bronze rings joined by gapped spokes
    bow = vm.P("maze_bow", "bronze", group=g_key, base=(176, 112, 52), lod=True)
    for R in ((2.0, 4.0, 6.0) if not vm.lod else (5.0,)):
        geo.merge(bow, geo.prim_torus(R, 0.5, n=vm.S(14, 8), m=4), geo.rot_matrix("x", 90), (0, 14, 0))
    spokes = vm.P("maze_spokes", "gold", group=g_key, base=(224, 186, 100), slot=TRIM)
    sp = geo.prim_box(0.6, 2.2, 0.7)
    for i in range(6):
        th = math.pi / 3 * i + 0.3
        rr = 3.0 if i % 2 == 0 else 5.0
        geo.merge(spokes, sp, geo.rot_matrix("z", math.degrees(th) - 90), (rr * math.cos(th), 14 + rr * math.sin(th), 0))
    eye = vm.P("maze_gem", "crystal", group=g_key, base=(240, 220, 140), slot=GLOW)
    geo.merge(eye, geo.prim_octa(1.1, 1.5), None, (0, 14, 0))
    # the shaft with collars, and the bit: a stack of notched blocks like a maze wall
    shaft = vm.P("shaft", "bronze", group=g_key, base=(168, 108, 50), lod=True)
    geo.lathe(shaft, "y", (0, 0, 0), [(-14.0, 0.7), (-13.0, 1.0), (7.0, 0.95), (7.5, 1.6), (8.4, 1.6), (8.9, 0.95)], sides=vm.S(8, 4), caps=(True, False))
    col = vm.P("shaft_collars", "gold", group=g_key, base=(236, 200, 110), slot=TRIM)
    for yy in (-4.0, 0.0, 4.0):
        geo.lathe(col, "y", (0, 0, 0), [(yy - 0.35, 1.25), (yy + 0.35, 1.25)], sides=vm.S(8, 4), caps=(False, False))
    bit = vm.P("maze_bit", "bronze", group=g_key, base=(188, 124, 58), lod=False)
    for k, (w, off) in enumerate(((3.6, 1.8), (2.4, 1.2), (3.2, 1.6), (2.0, 1.0), (3.4, 1.7), (2.2, 1.1))):
        geo.merge(bit, geo.prim_box(w, 1.0, 0.8), None, (off, -13.4 + 1.5 * k, 0))
    # a door standing open beside it: a gothic arch frame with a ripple of light in it
    frame = vm.P("door_frame", "gold", group=g_door, base=(222, 184, 96), slot=TRIM, lod=False)
    geo.tube_path(frame, [(6.0, -6.0, 0), (6.0, 2.0, 0)], 0.55, sides=4)
    geo.tube_path(frame, [(12.0, -6.0, 0), (12.0, 2.0, 0)], 0.55, sides=4)
    geo.tube_path(frame, geo.arc_pts((9.0, 2.0, 0), 3.0, 0, 180, vm.S(10, 4), plane="xy"), 0.55, sides=4)
    sill = vm.P("door_sill", "bronze", group=g_door, base=(150, 100, 46), lod=False)
    geo.merge(sill, geo.prim_box(7.6, 0.8, 1.6), None, (9.0, -6.4, 0))
    vm.fx_disc(g_rip, GLOW, (9.0, -1.0, -0.2), (0, 0, 1), 8.6, "ripple")
    vm.fx_disc(g_rip, DARK, (9.0, -1.0, -0.1), (0, 0, 1), 6.0, "web")
    # three lost keys circling
    lost = vm.P("lost_keys", "bronze", group=g_keys, base=(150, 100, 48), lod=False)
    small = geo.new_part("smallkey")
    geo.lathe(small, "y", (0, 0, 0), [(-2.4, 0.22), (1.6, 0.22)], sides=3, caps=(True, True))
    geo.merge(small, geo.prim_torus(0.8, 0.2, n=6, m=3), geo.rot_matrix("x", 90), (0, 2.4, 0))
    geo.merge(small, geo.prim_box(0.9, 0.4, 0.3), None, (0.4, -2.0, 0))
    for i in range(2):
        th = 2 * math.pi * i / 2
        geo.merge(lost, small, geo.rot_matrix("y", -math.degrees(th)) @ geo.rot_matrix("z", 60), (6.0 * math.cos(th), 2 + 2 * i, 6.0 * math.sin(th)))
    vm.fx_disc(g_halo, GLOW, (0, 14, 0.9), (0, 0, 1), 18.0, "seal")
    K.halo_set(vm, pal, y=2.0, R=9.0, rune=True, shaft_len=30.0)
    vm.deco(PT.deco_fill_glow, eye, glow, strength=0.95, seed=3)
    vm.deco(PT.deco_glyphs, shaft, glow, text="LABYRINTH", rows=1, glow=False, vertical=True, seed=4)
    vm.deco(PT.deco_filigree, frame, trim, seed=5)
    vm.deco(PT.deco_grain, bow, amount=0.16)
    vm.deco(PT.deco_grain, shaft, amount=0.12)


add(Item("warrens_key", build_key, dict(style="grid", dark=(8, 4, 0), mid=(120, 90, 36), hot=(232, 216, 160)), hand_size=0.9, hand_style="tool", seed=18,
         layers=[GL("labyrinth glow", "rune_ring", "maze_glow"), GL("open door", "portal_ripples", "door_ripples"), GL("door frame", "aura_shell", "door_frame"),
                 GL("lost keys", "orbit_shards", "lost_keys"), GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("door dust", "motes", "dust", 0xE8D8A0, count=1, every=5, radius=0.4, rise=0.008),
                 PL("lock sparks", "sparks", "crit", None, count=1, every=10, radius=0.4, rise=0.0)],
         acts={"door_to_door": [ST("SPACE_PORTAL", 0xE8D8A0, 2.0, 50, "front"), ST("SPACE_RINGGATE", 0xD8B060, 1.6, 40, "ground")],
               "choose_door": [ST("SPACE_CRACK", 0xE8D8A0, 1.2, 24, "front")],
               "knock": [ST("LIGHT_FLARE", 0xD8B060, 1.0, 12, "front"), ST("MAGIC_CIRCLE", 0xD8B060, 1.4, 20, "front")],
               "arcane_lock": [ST("SEAL_TRINITY", 0xE8D8A0, 1.6, 40, "front"), ST("LIGHT_FLARE", 0xD8B060, 1.0, 12, "front")]}))


# ====================================================================== ROSE PETAL
def build_rose(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_bloom = vm.grp("bloom", "sway", (0, -4, 0), 2.0, 0.05, 0.0)
    g_w1 = vm.grp("whorl_outer", "spin", (0, 4, 0), 0.5)
    g_w2 = vm.grp("whorl_mid", "spin", (0, 4, 0), -0.8)
    g_w3 = vm.grp("whorl_heart", "spin", (0, 4, 0), 1.3)
    g_vine = vm.grp("thorn_vine", "spin", (0, -3, 0), 0.7)
    g_fall = vm.grp("falling_petals", "spin", (0, 2, 0), 1.6)
    g_ghost = vm.grp("ghost_halberd", "pulse", (0, 2, 3), 0.05, 0.08, 0.0)
    # the bloom: three whorls of curved petals round a glowing heart
    outer = vm.P("petals_outer", "cloth", group=g_w1, base=(222, 238, 244), lod=False)
    mid = vm.P("petals_mid", "cloth", group=g_w2, base=(236, 246, 250))
    heart = vm.P("petals_heart", "cloth", group=g_w3, base=(246, 252, 255))
    for part, n, L, W, tilt, y, rr, ph in ((outer, 5, 8.0, 4.2, 70.0, 2.0, 1.4, 0.0), (mid, 5, 6.0, 3.4, 48.0, 4.0, 0.9, 0.62), (heart, 4, 4.2, 2.6, 24.0, 5.4, 0.5, 0.2)):
        petal = geo.prim_leaf(L, W, 0.35, sides=2, bend=-0.03)
        geo.scatter_ring(part, petal, n, rr, y=y, up_out=True, tilt=tilt, phase=ph)
    core = vm.P("heart_gem", "crystal", group=g_w3, base=(120, 255, 190), slot=GLOW, lod=True)
    geo.merge(core, geo.prim_octa(1.1, 1.5), None, (0, 5.2, 0))
    # the stem with thorns, two leaves, and a thorned vine wound round it
    stem = vm.P("stem", "wood", group=g_bloom, base=(60, 120, 100), lod=True)
    pts = geo.bez3((0, 2.0, 0), (1.5, -3.0, 0), (-1.6, -9.0, 0), (0, -17.0, 0), vm.S(7, 3))
    geo.tube_path(stem, pts, 0.7, sides=vm.S(5, 4), taper=(1.0, 0.8))
    thorns = vm.P("thorns", "bone", group=g_bloom, base=(230, 244, 240), slot=TRIM)
    th = geo.prim_spike(0.45, 1.8, sides=3)
    for i in range(7):
        yy = -2.5 - 2.0 * i
        a = i * 2.4
        geo.merge(thorns, th, geo.align_y((math.cos(a), 0.25, math.sin(a))), (0.8 * math.cos(a), yy, 0.8 * math.sin(a)))
    leaves = vm.P("leaves", "cloth", group=g_bloom, base=(76, 170, 130))
    for sgn, yy in ((1, -6.0), (-1, -10.5)):
        geo.merge(leaves, geo.prim_leaf(5.5, 2.4, 0.3, sides=2, bend=0.02), geo.align_y((sgn * 1.0, 0.5, 0.2)), (0, yy, 0))
    vine = vm.P("thorn_vine", "wood", group=g_vine, base=(48, 100, 88), lod=False)
    geo.spiral_tube(vine, (0, -9.0, 0), 2.4, 14.0, 2.2, 0.35, n=vm.S(20, 6), sides=3)
    # the ghost halberd the rose becomes: a pale blade of the nebula behind it
    ghost = vm.star("halberd", g_ghost, lod=False)
    geo.merge(ghost, geo.prim_blade(13.0, 3.6, 0.5, tip=0.35), None, (0, 6.0, 2.6))
    geo.merge(ghost, geo.prim_blade(5.0, 2.4, 0.4, tip=0.5), geo.rot_matrix("z", 90), (1.8, 8.5, 2.6))
    # petals fall round it
    fall = vm.P("falling_petals", "cloth", group=g_fall, base=(236, 248, 242), lod=False)
    small = geo.prim_leaf(2.4, 1.3, 0.2, sides=2, bend=-0.04)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(fall, small, geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 100), (7.0 * math.cos(a), 6.0 - 2.4 * i, 7.0 * math.sin(a)))
    K.halo_set(vm, pal, y=3.0, R=9.0, rune=True, shaft_len=22.0)
    vm.deco(PT.deco_fill_glow, core, glow, strength=0.95, seed=3)
    vm.deco(PT.deco_veins, outer, trim, n=2, strength=0.6, seed=4, min_w=6, min_h=6, dark=0.2)
    vm.deco(PT.deco_veins, mid, trim, n=2, strength=0.6, seed=5, min_w=6, min_h=6, dark=0.2)
    vm.deco(PT.deco_tint, stem, (60, 150, 120), amount=0.3)


add(Item("rose_petal", build_rose, dict(style="ocean", dark=(2, 14, 12), mid=(40, 150, 110), hot=(200, 255, 235)), hand_size=0.78, hand_style="tool", seed=19,
         layers=[GL("bloom whorl outer", "petals", "whorl_outer"), GL("bloom whorl mid", "petals", "whorl_mid"), GL("bloom whorl heart", "petals", "whorl_heart"),
                 GL("thorn vine", "ribbons", "thorn_vine"), GL("falling petals", "petals", "falling_petals"), GL("ghost halberd", "nebula_core", "ghost_halberd"),
                 GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("drifting petals", "motes", "cherry_leaves", None, count=1, every=7, radius=0.5, rise=-0.012),
                 PL("spirit glints", "glints", "dust", 0x2FBF8A, count=1, every=6, radius=0.5, rise=0.01)],
         acts={"bloom": [ST("TREE_ROOTS", 0x2FBF8A, 2.4, 50, "ground"), ST("LIGHT_FLARE", 0xE8F6F0, 1.4, 16, "self")],
               "ensnaring_strike": [ST("TREE_ROOTS", 0x2FBF8A, 1.8, 40, "front")],
               "grasping_vine": [ST("TREE_ROOTS", 0x2FBF8A, 3.0, 60, "ground"), ST("TREE_CANOPY", 0x2FBF8A, 1.2, 40, "ground")],
               "guardian_of_nature": [ST("TREE_CANOPY", 0x2FBF8A, 2.2, 80, "ground"), ST("DIVINE_HALO", 0xE8F6F0, 2.0, 50, "self")]}))


# ====================================================================== ADMINISTRATOR'S AUTHORITY KEY
def build_admin(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_key = vm.grp("key", "bob", (0, 0, 0), 0.3, 0.07, 0.0)
    g_hex = vm.grp("authority_ring", "spin", (0, 13, 0), 0.9)
    g_core = vm.grp("root_core", "pulse", (0, 13, 0), 0.08, 0.15, 0.0)
    g_panels = vm.grp("system_panels", "spin", (0, 4, 0), 1.7)
    g_wire = vm.grp("wireframe", "tumble", (0, 4, 0), 0.8, 0.0, 1.5)
    g_blocks = vm.grp("permission_blocks", "pulse", (0, -12, 0), 0.06, 0.2, 0.0)
    g_seal = vm.grp("command_seal", "pulse", (0, 13, 2.5), 0.08, 0.1, 0.0)
    # the bow: a double hexagonal ring round a root core; the shaft is stacked console segments; the bit is a comb of permission blocks
    hexa = vm.P("bow_hex", "silver", group=g_hex, base=(236, 238, 250), lod=True)
    geo.merge(hexa, geo.prim_torus(5.2, 0.6, n=6, m=4), geo.rot_matrix("x", 90), (0, 13, 0))
    geo.merge(hexa, geo.prim_torus(3.6, 0.4, n=6, m=4), geo.rot_matrix("x", 90) @ geo.rot_matrix("y", 30), (0, 13, 0))
    nodes = vm.P("bow_nodes", "crystal", group=g_hex, base=(230, 240, 255), slot=GLOW)
    for i in range(6):
        a = math.pi / 3 * i
        geo.merge(nodes, geo.prim_octa(0.7, 0.9), None, (5.2 * math.cos(a), 13 + 5.2 * math.sin(a), 0))
    core = vm.star("root", g_core)
    geo.merge(core, geo.prim_octa(2.4, 3.2), None, (0, 13, 0))
    segs = vm.P("shaft_segments", "silver", group=g_key, base=(222, 226, 244), lod=False)
    for k in range(5):
        geo.merge(segs, geo.prim_box(1.9, 3.0, 1.9, chamfer=0.35), None, (0, 6.0 - 3.6 * k, 0))
    bands = vm.P("shaft_lines", "crystal", group=g_key, base=(240, 246, 255), slot=GLOW)
    for k in range(6):
        geo.merge(bands, geo.prim_box(2.15, 0.28, 2.15), None, (0, 7.6 - 3.6 * k, 0))
    comb = vm.P("permission_comb", "silver", group=g_blocks, base=(206, 212, 236), lod=False)
    for k, (w, off) in enumerate(((3.6, 2.2), (2.4, 1.6), (3.2, 2.0), (2.0, 1.4), (3.4, 2.1))):
        geo.merge(comb, geo.prim_box(w, 1.5, 1.2), None, (off, -9.6 - 1.7 * k, 0))
    # system panels with glowing command text, orbiting; a wireframe octahedron turning round the shaft
    panels = vm.P("system_panels", "silver", group=g_panels, base=(214, 222, 246), lod=False)
    pc = geo.prim_card(6.6, 4.2, 0.22)
    for i in range(4):
        a = 2 * math.pi * i / 4
        geo.merge(panels, pc, geo.rot_matrix("y", -math.degrees(a) + 90), (9.5 * math.cos(a), 4.0 + 2.5 * math.sin(i * 1.6), 9.5 * math.sin(a)))
    wire = vm.P("wireframe", "silver", group=g_wire, base=(246, 248, 255), slot=TRIM, lod=False)
    apex = [(0, 7.0, 0), (0, -7.0, 0), (7.0, 0, 0), (-7.0, 0, 0), (0, 0, 7.0), (0, 0, -7.0)]
    for a, b in ((0, 2), (0, 3), (0, 4), (0, 5), (1, 2), (1, 3), (1, 4), (1, 5), (2, 4), (4, 3), (3, 5), (5, 2)):
        geo.tube_path(wire, [tuple(np.array(apex[a]) * 0.8 + (0, 4, 0)), tuple(np.array(apex[b]) * 0.8 + (0, 4, 0))], 0.16, sides=3, caps=(False, False))
    vm.fx_disc(g_seal, GLOW, (0, 13, 2.4), (0, 0, 1), 22.0, "seal")
    K.halo_set(vm, pal, y=4.0, R=10.0, rune=True, shaft_len=32.0)
    vm.deco(PT.deco_glyphs, panels, glow, text="ADMIN COMMAND SYSTEM CALL", rows=2, glow=True, strength=0.9, seed=3, density=0.9)
    vm.deco(PT.deco_fill_glow, nodes, glow, strength=0.9, seed=4)
    vm.deco(PT.deco_fill_glow, bands, glow, strength=0.8, seed=5, facets=False)
    vm.deco(PT.deco_glyphs, segs, glow, text="ROOT", rows=1, glow=False, seed=6)
    vm.deco(PT.deco_grain, hexa, amount=0.06)


add(Item("administrators_authority_key", build_admin, dict(style="grid", dark=(6, 8, 18), mid=(100, 110, 170), hot=(240, 244, 255)), hand_size=0.92, hand_style="tool", seed=20,
         layers=[GL("authority ring", "halo_ring", "authority_ring"), GL("root core", "nebula_core", "root_core"), GL("system panels", "orbit_shards", "system_panels"),
                 GL("wireframe", "chains", "wireframe"), GL("permission blocks", "aura_shell", "permission_blocks"), GL("command seal", "rune_ring", "command_seal"),
                 GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("data motes", "motes", "end_rod", None, count=1, every=4, radius=0.45, rise=0.012),
                 PL("command glints", "glints", "electric_spark", None, count=1, every=9, radius=0.5, rise=0.0)],
         acts={"change_the_rules": [ST("SEAL_TRINITY", 0xFFFFFF, 2.6, 50, "ground"), ST("MIRROR_ARRAY", 0xF0F0F8, 3.0, 50, "self"), ST("LIGHT_FLARE", 0xFFFFFF, 1.6, 14, "self")],
               "system_call": [ST("SPELL_CARD", 0xFFFFFF, 1.8, 36, "front"), ST("LIGHT_FLARE", 0xF0F0F8, 1.2, 12, "front")]}))


# ====================================================================== ARCHIVIST'S STARBOUND CODEX
def build_codex(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_book = vm.grp("codex", "bob", (0, 0, 0), 0.35, 0.06, 0.0)
    g_sky = vm.grp("star_cover", "pulse", (0, 0, -2.0), 0.03, 0.1, 0.0)
    g_pages = vm.grp("flying_pages", "spin", (0, 0, 0), 2.0)
    g_pages2 = vm.grp("flying_pages_far", "spin", (0, 2, 0), -1.2)
    g_eyes = vm.grp("many_eyes", "pulse", (0, 0, -2.2), 0.15, 0.12, 0.0)
    g_rib = vm.grp("bookmarks", "sway", (0, -7, 1.0), 6.0, 0.06, 0.0)
    g_zodiac = vm.grp("zodiac_ring", "spin", (0, 0, 0), 0.7)
    # the book: two boards, a page block, a spine with bands, gold corners, and a sky for a cover
    boards = vm.P("boards", "leather", group=g_book, base=(40, 28, 86), lod=False)
    geo.merge(boards, geo.prim_box(9.4, 13.0, 0.9, chamfer=0.25), None, (0, 0, -1.7))
    geo.merge(boards, geo.prim_box(9.4, 13.0, 0.9, chamfer=0.25), None, (0, 0, 1.7))
    geo.merge(boards, geo.prim_box(1.0, 13.0, 4.3, chamfer=0.25), None, (-4.85, 0, 0))
    pages = vm.P("page_block", "bone", group=g_book, base=(236, 228, 210), lod=True)
    geo.merge(pages, geo.prim_box(8.2, 12.2, 2.5), None, (0.4, 0, 0))
    cor = vm.P("corners", "gold", group=g_book, base=(226, 200, 150), slot=TRIM)
    for dx in (-4.7, 4.7):
        for dy in (-6.5, 6.5):
            geo.merge(cor, geo.prim_octa(0.9, 1.0), None, (dx, dy, -2.2))
            geo.merge(cor, geo.prim_box(1.6, 1.6, 0.3), None, (dx * 0.92, dy * 0.95, -2.12))
    bands = vm.P("spine_bands", "gold", group=g_book, base=(214, 190, 140), slot=TRIM)
    for yy in (-4.2, 0.0, 4.2):
        geo.merge(bands, geo.prim_box(1.3, 0.7, 4.7), None, (-4.85, yy, 0))
    sky = vm.star("cover_sky", g_sky)
    geo.merge(sky, geo.prim_box(7.6, 11.2, 0.12), None, (0.0, 0, -2.25))
    eyes = vm.P("eyes", "crystal", group=g_eyes, base=(232, 216, 255), slot=GLOW)
    ep = [(0, 2.8), (-2.4, 1.2), (2.2, 0.6), (-1.4, -1.6), (1.8, -2.4), (-2.8, -3.6), (0.2, -4.2)]
    for (ex, ey) in ep:
        geo.merge(eyes, geo.prim_octa(0.7, 0.85), geo.rot_matrix("x", 90), (ex, ey, -2.45))
    # pages tearing free and orbiting
    pgs = vm.P("flying_pages", "bone", group=g_pages, base=(238, 232, 222), lod=False)
    pc = geo.prim_card(3.8, 5.0, 0.1)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(pgs, pc, geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("x", 12 * (i % 3)), (10.0 * math.cos(a), 1.0 + 2.0 * math.sin(i * 1.7), 10.0 * math.sin(a)))
    pgs2 = vm.P("flying_pages_far", "bone", group=g_pages2, base=(222, 214, 236), lod=False)
    for i in range(4):
        a = 2 * math.pi * i / 4 + 0.5
        geo.merge(pgs2, geo.prim_card(2.8, 3.8, 0.08), geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("z", 20), (14.0 * math.cos(a), -3.0 + 3.0 * math.sin(i * 1.3), 14.0 * math.sin(a)))
    rib = vm.P("bookmarks", "cloth", group=g_rib, base=(140, 70, 190), lod=False)
    for dx, col_ in ((-1.2, 0.0), (1.0, 0.6)):
        pts = [(dx - 2.0, -6.4, 1.4), (dx - 2.2 + col_, -9.0, 1.7), (dx - 1.4, -11.4, 1.2), (dx - 1.8, -14.0, 1.6)]
        geo.ribbon(rib, pts, 1.0, thick=0.12, up=(0, 0, 1), taper=0.4)
    vm.fx_disc(g_zodiac, GLOW, (0, 0, 0), (0, 1, 0), 28.0, "rune")
    vm.fx_disc(g_zodiac, DARK, (0, 0, 0), (0, 1, 0), 22.0, "ticks", rot=11)
    K.halo_set(vm, pal, y=0.0, R=9.0, rune=False, shaft_len=22.0)
    vm.deco(PT.deco_fill_glow, eyes, glow, strength=0.95, seed=3)
    vm.deco(PT.deco_glyphs, boards, glow, text="STARBOUND CODEX", rows=2, glow=False, seed=4)
    vm.deco(PT.deco_glyphs, pgs, glow, rows=3, glow=True, strength=0.7, seed=5)
    vm.deco(PT.deco_filigree, cor, trim, seed=6)
    vm.deco(PT.deco_stars, boards, glow, n=26, seed=7, strength=0.6)


add(Item("archivists_starbound_codex", build_codex, dict(style="void", dark=(2, 0, 14), mid=(60, 40, 150), hot=(210, 190, 255)), hand_size=0.72, hand_style="palm", seed=21,
         layers=[GL("star cover", "starfield", "star_cover"), GL("many eyes", "aura_shell", "many_eyes"), GL("flying pages", "orbit_shards", "flying_pages"), GL("far pages", "orbit_shards", "flying_pages_far"),
                 GL("bookmark ribbons", "ribbons", "bookmarks"), GL("zodiac ring", "rune_ring", "zodiac_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("star dust", "stars", "end_rod", None, count=1, every=4, radius=0.5, rise=0.01),
                 PL("ink glints", "glints", "dust", 0xB9A3FF, count=1, every=6, radius=0.5, rise=0.008)],
         acts={"star_chart": [ST("STAR_MAGIC", 0xB9A3FF, 2.8, 70, "self"), ST("MAGIC_CIRCLE", 0xE8D8FF, 3.0, 50, "ground")],
               "quote_the_sky": [ST("STAR_MAGIC", 0xE8D8FF, 2.0, 50, "front"), ST("LIGHT_FLARE", 0xB9A3FF, 1.2, 14, "front")]}))


# ====================================================================== BEHELIT EGG
def build_behelit(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_egg = vm.grp("egg", "bob", (0, 0, 0), 0.3, 0.05, 0.0)
    g_eye = vm.grp("closed_eye", "pulse", (0, 3, -5), 0.1, 0.06, 0.0)
    g_chain = vm.grp("chain", "sway", (0, 10, 0), 3.0, 0.05, 0.0)
    g_eclipse = vm.grp("eclipse", "pulse", (0, 0, 3.4), 0.03, 0.05, 0.0)
    g_corona = vm.grp("corona", "spin", (0, 0, 3.0), 0.8)
    g_feathers = vm.grp("black_feathers", "spin", (0, 0, 0), 1.4)
    g_tears = vm.grp("blood_tears", "bob", (0, 0, 0), 1.2, 0.09, 0.0)
    # the egg: gold, with a face carved on its front that sits very quietly
    egg = vm.P("egg", "gold", group=g_egg, base=(212, 170, 66), lod=True)
    geo.merge(egg, geo.prim_egg(7.0, 17.0, sides=vm.S(12, 6), rings=6), None, (0, 0, 0))
    brow = vm.P("brow_lids", "gold", group=g_egg, base=(236, 196, 96), slot=TRIM)
    geo.tube_path(brow, geo.arc_pts((-2.6, 3.2, -6.0), 2.2, 20, 160, vm.S(6, 3), plane="xy"), 0.34, sides=3)
    geo.tube_path(brow, geo.arc_pts((2.6, 3.2, -6.0), 2.2, 20, 160, vm.S(6, 3), plane="xy"), 0.34, sides=3)
    geo.tube_path(brow, [(0, 4.2, -6.0), (0, -0.8, -6.4)], 0.45, sides=3)                    # the nose ridge
    lips = vm.P("lips", "gold", group=g_egg, base=(180, 120, 50), slot=TRIM)
    geo.tube_path(lips, geo.arc_pts((0, -3.0, -6.2), 3.0, 200, 340, vm.S(7, 3), plane="xy"), 0.42, sides=3)
    geo.tube_path(lips, geo.arc_pts((0, -4.2, -6.0), 2.2, 210, 330, vm.S(6, 3), plane="xy"), 0.32, sides=3)
    eyes = vm.P("closed_eyes", "crystal", group=g_eye, base=(255, 210, 110), slot=GLOW)
    for ex in (-2.6, 2.6):
        geo.merge(eyes, geo.prim_octa(1.0, 0.35), geo.rot_matrix("x", 90), (ex, 2.6, -6.5))
    tears = vm.P("blood_tears", "crystal", group=g_tears, base=(150, 10, 40), slot=ACCENT)
    for ex in (-2.8, 2.8):
        geo.merge(tears, geo.prim_octa(0.35, 0.8), None, (ex, -0.2, -6.7))
    ring = vm.P("ring", "gold", group=g_egg, base=(228, 186, 90), slot=TRIM, lod=False)
    geo.merge(ring, geo.prim_torus(1.3, 0.38, n=vm.S(10, 6), m=3), geo.rot_matrix("x", 90), (0, 9.4, 0))
    chain = vm.P("chain", "bronze", group=g_chain, base=(160, 110, 50), lod=False)
    geo.chain(chain, [(0, 10.7, 0), (0, 14, 0), (2.0, 18, 0)], link_r=0.8, wire=0.2, n=6, m=3, every=2.2)
    # the eclipse behind: a black disc with a corona, and black feathers falling
    ecl = vm.star("eclipse", g_eclipse)
    geo.merge(ecl, geo.prim_lathe([(-0.2, 8.4), (0.2, 8.4)], sides=vm.S(14, 6), caps=(True, True)), geo.rot_matrix("x", 90), (0, 0, 3.6))
    vm.fx_disc(g_corona, ACCENT, (0, 0, 3.3), (0, 0, 1), 30.0, "rays")
    vm.fx_disc(g_corona, GLOW, (0, 0, 3.2), (0, 0, 1), 22.0, "ring")
    fth = vm.P("black_feathers", "obsidian", group=g_feathers, base=(20, 6, 26), lod=False)
    lf = geo.prim_leaf(4.4, 1.5, 0.2, sides=2, bend=0.05)
    for i in range(5):
        a = 2 * math.pi * i / 5
        geo.merge(fth, lf, geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 100 + 12 * i), (10.0 * math.cos(a), 4.0 - 2.4 * i, 10.0 * math.sin(a)))
    K.halo_set(vm, pal, y=0.0, R=9.5, rune=True, shaft_len=22.0)
    vm.deco(PT.deco_fill_glow, eyes, glow, strength=0.9, seed=3)
    vm.deco(PT.deco_fill_glow, tears, (210, 20, 60), strength=0.9, seed=4)
    vm.deco(PT.deco_filigree, egg, trim, seed=5, min_w=14, min_h=14)
    vm.deco(PT.deco_grain, egg, amount=0.14)
    vm.deco(PT.deco_glyphs, egg, glow, text="BEHELIT", rows=1, glow=False, seed=6)


add(Item("behelit_egg", build_behelit, dict(style="void", dark=(8, 0, 10), mid=(90, 20, 70), hot=(255, 200, 90)), hand_size=0.66, hand_style="hang", seed=22,
         layers=[GL("closed eyes", "aura_shell", "closed_eye"), GL("eclipse", "nebula_core", "eclipse"), GL("eclipse corona", "light_shafts", "corona"), GL("black feathers", "orbit_shards", "black_feathers"),
                 GL("blood tears", "orbit_shards", "blood_tears"), GL("hanging chain", "chains", "chain"), GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("eclipse embers", "embers", "dust", 0xFFD870, count=1, every=5, radius=0.45, rise=0.012),
                 PL("crimson drips", "drips", "dust", 0xB0102A, count=1, every=12, radius=0.3, rise=-0.02)],
         acts={"offer_a_sacrifice": [ST("DEVIL_CIRCLE", 0xB0102A, 3.0, 50, "ground"), ST("SHADOW_POOL", 0x2A0A2A, 2.6, 60, "ground")],
               "eclipse": [ST("DEMON_METEOR", 0x2A0A2A, 3.0, 60, "sky"), ST("DIVINE_HALO", 0xFFD870, 3.2, 70, "sky"), ST("SHADOW_UNITE", 0x2A0A2A, 3.0, 60, "self")]}))

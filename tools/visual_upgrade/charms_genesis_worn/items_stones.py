"""The six Infinity Stones, in the style the lore gives each: Space (a cage of folded space round a tumbling cube of stars), Mind, Power, Time, Reality, Soul."""
import math

import numpy as np

import geo
import kitlib as K
import paint as PT
import vu as V
from spec import Item, geo_layer as GL, part_layer as PL, step as ST
from vu import MAIN, TRIM, GLOW, DARK, ACCENT

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


# ====================================================================== SPACE
def build_space(vm, info):
    pal = info[4]
    main, trim, glow = K.rgb(pal[0]), K.rgb(pal[1]), K.rgb(pal[2])
    # ---- animation groups (the idle set)
    g_core = vm.grp("core", "tumble", (0, 0, 0), 0.7, 0.35, 0.5)
    g_inner = vm.grp("inner_cube", "tumble", (0, 0, 0), -1.3, 0.8, -0.4)
    g_gyro_a = vm.grp("gyro_a", "tumble", (0, 0, 0), 0.0, 0.0, 2.4)
    g_gyro_b = vm.grp("gyro_b", "tumble", (0, 0, 0), 0.0, 0.0, -1.7)
    g_shards = vm.grp("orbit_shards", "spin", (0, 0, 0), 2.6)
    g_shards2 = vm.grp("orbit_shards_far", "spin", (0, 0, 0), -1.5)
    g_bob = vm.grp("float", "bob", (0, 0, 0), 0.35, 0.07, 0.0)
    # ---- the starfield core: a cube of stars and a smaller cube turned inside it (nebula pass)
    core = vm.star("core", g_core)
    geo.merge(core, geo.prim_box(6.4, 6.4, 6.4), None, (0, 0, 0))
    inner = vm.star("inner", g_inner, lod=False)
    geo.merge(inner, geo.prim_box(3.6, 3.6, 3.6), geo.rot_matrix("y", 45) @ geo.rot_matrix("x", 35), (0, 0, 0))
    # ---- the cage of folded space: bars, corner crystals, six face jewels
    H = 5.2
    edges = K.cube_edges(vm, "cage_edges", g_bob, H, 1.0, "silver", (150, 176, 232))
    corners = K.cube_corners(vm, "cage_corners", g_bob, H, 1.4, "crystal", (120, 190, 255), lod=False)
    jew = vm.P("face_jewels", "crystal", group=g_bob, base=(150, 210, 255), slot=GLOW)
    cab = geo.prim_lathe([(0.0, 1.9), (1.0, 0.0)], sides=vm.S(6, 4), caps=(True, False))
    for ax, rot in (((1, 0, 0), geo.rot_matrix("z", -90)), ((-1, 0, 0), geo.rot_matrix("z", 90)), ((0, 1, 0), np.eye(3)), ((0, -1, 0), geo.rot_matrix("x", 180)),
                    ((0, 0, 1), geo.rot_matrix("x", 90)), ((0, 0, -1), geo.rot_matrix("x", -90))):
        geo.merge(jew, cab, rot, tuple(np.array(ax) * (H + 0.45)))
    # ---- the base: a turned foot with prongs that cradle the cage
    body, trimp = K.plinth(vm, "foot", -9.4, 3.4, 3.4, "silver", (120, 142, 200), "gold", (210, 226, 255), sides=8, petals=0, group=g_bob, lod=False)
    # ---- gyroscope rings, and two shard orbits
    a = K.gyro(vm, "gyro_a", g_gyro_a, 8.6, 0.38, 25, "silver", (170, 200, 255), tilt_axis="z", n=20, m=3)
    b = K.gyro(vm, "gyro_b", g_gyro_b, 9.8, 0.30, -40, "gold", (150, 190, 255), tilt_axis="x", n=14, m=3)
    sh1 = K.shard_set(vm, "shards_near", g_shards, 3, 7.2, 1.0, 0.55, 2.6, "crystal", (110, 190, 255), sides=4)
    sh2 = K.shard_set(vm, "shards_far", g_shards2, 4, 11.0, -2.0, 0.55, 2.4, "crystal", (190, 220, 255), phase=0.6, octa=True)
    # ---- additive layers: rune ring, lens flare, shafts, a ripple around the cube
    K.halo_set(vm, pal, y=0.0, R=9.5, tilt=0.0)
    g_rip = vm.grp("ripple", "pulse", (0, 0, 0), 0.22, 0.09, 0.0)
    vm.fx_cross(g_rip, DARK, (0, 0, 0), (0, 1, 0), 12.0, "ripple", n=2)
    # ---- the skin details
    vm.deco(PT.deco_glyphs, edges, glow, text="TESSERACT", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_fill_glow, corners, glow, strength=0.95, seed=4)
    vm.deco(PT.deco_fill_glow, jew, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_glyphs, body, glow, text="SPACE", rows=2, seed=5, min_w=10, min_h=6, glow=False)
    vm.deco(PT.deco_filigree, trimp, trim, seed=6)
    vm.deco(PT.deco_glyphs, a, glow, rows=1, seed=7, glow=False)
    vm.deco(PT.deco_grain, edges, amount=0.10)


add(Item("the_space_stone_or_the_tesseract", build_space, dict(style="grid", dark=(2, 6, 24), mid=(20, 56, 160), hot=(120, 190, 255)), hand_size=0.62, hand_style="palm", seed=11,
         layers=[GL("tesseract core", "nebula_core", "core"), GL("folded cube", "nebula_core", "inner_cube"), GL("gyroscope rune ring", "rune_ring", "halo_ring"), GL("gyro ring a", "halo_ring", "gyro_a"),
                 GL("gyro ring b", "halo_ring", "gyro_b"), GL("near shards", "orbit_shards", "orbit_shards"), GL("far shards", "orbit_shards", "orbit_shards_far"),
                 GL("space ripple", "portal_ripples", "ripple"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("folded motes", "motes", "end_rod", None, count=1, every=5, radius=0.45, rise=0.012),
                 PL("blink sparks", "sparks", "electric_spark", None, count=1, every=9, radius=0.5, rise=0.0)],
         acts={"tesseract": [ST("SPACE_PORTAL", 0x3A8BFF, 2.2, 44, "front"), ST("SPACE_RINGGATE", 0x8FD0FF, 1.8, 36, "ground"), ST("LIGHT_FLARE", 0xFFFFFF, 1.2, 14, "self")],
               "mark": [ST("SPACE_CRACK", 0x8FD0FF, 1.4, 30, "ground"), ST("LIGHT_FLARE", 0x3A8BFF, 1.0, 16, "self")]}))

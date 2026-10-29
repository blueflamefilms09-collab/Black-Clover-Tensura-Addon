"""The Millennium Items, part 1 (Yu-Gi-Oh!): the Puzzle, the Eye, the Ring. Gold and lapis, the Eye of Wdjat everywhere, built on the visual upgrade kit (see common.py)."""
import math

import numpy as np

from common import *  # noqa: F401,F403
from canon_a import build_puzzle_canon
from common import (geo, K, PT, V, Item, GL, PL, ST, MAIN, TRIM, GLOW, DARK, ACCENT, GOLD, GOLD_HI, GOLD_DK, LAPIS, LAPIS_DK, LAPIS_HI, TURQ, deco_lapis, deco_hiero, deco_wdjat, wdjat,
                    paint_wdjat, ankh, orbit_motes)

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


def chain_over_neck(vm, group, y_ring, z_ring, bead_every=3, lod=True):
    """The chain of a neck piece: from the ring at the chest over each shoulder and behind the neck, gold links with lapis beads."""
    ch = vm.P("chain", "gold", group=group, base=GOLD_HI, slot=TRIM, lod=False)
    beads = vm.P("chain_lapis", "stone", group=group, base=LAPIS, slot=MAIN, lod=False)
    for sx in (-1, 1):
        path = geo.bez3((0.0, y_ring, z_ring), (sx * 1.6, y_ring + 1.5, z_ring - 0.3), (sx * 4.4, 0.8, -2.4), (sx * 3.7, 0.5, 0.0), vm.S(7, 3))
        path += geo.bez3((sx * 3.7, 0.5, 0.0), (sx * 3.4, 0.4, 1.6), (sx * 2.6, 0.5, 2.7), (0.0, 0.5, 2.9), vm.S(6, 3))[1:]
        geo.tube_path(ch, path, 0.2, sides=3, caps=(True, True))
        for i, p in enumerate(path):
            if i % bead_every == 1:
                geo.merge(beads, geo.prim_sphere(0.46, 3, 6), None, p)
    return ch, beads


# ====================================================================== MILLENNIUM PUZZLE (neck)
def build_puzzle(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_p = vm.grp("puzzle", "bob", (0, -3.0, -3.4), 0.3, 0.05, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, -4.4, -4.3), 0.12, 0.12, 0.0)
    g_soul = vm.grp("second_soul", "pulse", (0, -5.6, -2.0), 0.08, 0.1, 0.0)
    g_motes = vm.grp("seven_motes", "spin", (0, -5.0, -3.4), 1.8)
    g_wheel = vm.grp("fate_wheel", "spin", (0, -5.0, -2.8), 1.1)
    g_wheel2 = vm.grp("fate_wheel_b", "spin", (0, -5.0, -2.8), -1.7)
    g_chain = vm.grp("chain", "sway", (0, 0.5, 0.0), 1.2, 0.05, 0.0)
    g_rip = vm.grp("shadow_ripple", "pulse", (0, -5.0, -2.0), 0.2, 0.1, 0.0)
    g_tears = vm.grp("tear_glints", "pulse", (0, -7.0, -4.0), 0.16, 0.2, 1.2)
    g_sand = vm.grp("falling_sand", "spin", (0, -5.0, -3.4), -0.9)
    CZ = -3.5
    # the pyramid: inverted, a square plate narrowing to a point, bevelled, with the lapis bands of the puzzle's seams
    body = vm.P("pyramid", "gold", group=g_p, base=GOLD, slot=TRIM, lod=True)
    sec = geo.octagon(4.8, 2.6, 0.55)
    geo.loft(body, "y", (0, -2.7, CZ), [(-5.2, 0.05, 0.05), (-4.6, 0.2, 0.2), (-1.0, 0.82, 0.82), (-0.3, 0.97, 0.97), (0.0, 1.0, 1.0)], sec, caps=(True, True))
    bands = vm.P("lapis_bands", "stone", group=g_p, base=LAPIS, slot=MAIN, lod=False)
    for s0 in (-1.6, -3.0, -4.2):
        def sc(s):
            return max(0.06, 1.0 + s / 5.4) * 1.05
        geo.loft(bands, "y", (0, -2.7, CZ), [(s0 - 0.28, sc(s0 - 0.28), sc(s0 - 0.28)), (s0 + 0.28, sc(s0 + 0.28), sc(s0 + 0.28))], sec, caps=(False, False))
    edges = vm.P("seams", "gold", group=g_p, base=GOLD_HI, slot=TRIM, lod=False)
    for sx in (-1, 1):
        for sz in (-1, 1):
            geo.tube_path(edges, [(sx * 2.3, -2.6, CZ + sz * 1.2), (sx * 0.15, -7.8, CZ + sz * 0.1)], 0.2, sides=3, caps=(True, True))
    top = vm.P("top_plate", "gold", group=g_p, base=GOLD_DK, slot=TRIM, lod=False)
    geo.merge(top, geo.prim_box(5.2, 0.5, 3.0, chamfer=0.25), None, (0, -2.45, CZ))
    studs = vm.P("top_studs", "stone", group=g_p, base=LAPIS_HI, slot=MAIN, lod=False)
    for sx in (-2.3, -0.8, 0.8, 2.3):
        geo.merge(studs, geo.prim_octa(0.34, 0.42), None, (sx, -2.15, CZ - 1.3))
    # the ring it hangs from
    ring = vm.P("bail", "gold", group=g_p, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(ring, geo.prim_torus(0.95, 0.24, n=vm.S(14, 6), m=4, plane="xy"), None, (0, -1.7, CZ))
    chain_over_neck(vm, g_chain, -1.5, CZ)
    # the Eye of Wdjat on the face, slanted with it
    parts = wdjat(vm, g_eye, (0, -4.55, CZ - 1.17), 1.35, M=geo.rot_matrix("x", -12.5), lod=True)
    paint_wdjat(vm, parts, glow, trim)
    # a second soul: a twin of the pyramid behind it, made of the shadow realm, and the seven items circling the two
    soul = vm.star("second_soul", g_soul)
    geo.merge(soul, geo.prim_octa(2.8, 3.8), None, (0, -5.4, CZ + 1.6))
    mot = orbit_motes(vm, "seven_motes", g_motes, 7, 5.2, -5.0, 0.55, GOLD_HI, slot=TRIM, wobble=0.9, material="gold")
    wheel = vm.P("fate_wheel", "gold", group=g_wheel, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(wheel, geo.prim_torus(3.2, 0.18, n=vm.S(24, 10), m=3, plane="yz"), None, (0, -5.0, CZ - 0.2))
    wheel2 = vm.P("fate_wheel_b", "stone", group=g_wheel2, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(wheel2, geo.prim_torus(2.7, 0.15, n=vm.S(20, 8), m=3, plane="xy"), None, (0, -5.0, CZ - 0.2))
    sand = orbit_motes(vm, "falling_sand", g_sand, 6, 3.2, -6.6, 0.3, (255, 226, 150), slot=ACCENT, phase=0.4, wobble=1.6, material="crystal")
    # additive layers
    vm.fx_disc(g_rip, GLOW, (0, -5.0, CZ + 1.8), (0, 0, 1), 12.0, "ripple")
    vm.fx_disc(g_rip, DARK, (0, -5.0, CZ + 2.0), (0, 0, 1), 8.0, "rune")
    vm.fx_cross(g_tears, ACCENT, (0, -7.2, CZ - 0.6), (0, 1, 0), 3.4, "glint", n=2)
    K.halo_set(vm, pal, y=-5.0, R=6.6, rune=True, shaft=(-13.0, 3.0))
    vm.deco(deco_hiero, top, glow, rows=1, seed=2, glow=False, min_w=10, min_h=3)
    vm.deco(PT.deco_filigree, body, trim, seed=3, min_w=14, min_h=10)
    vm.deco(deco_lapis, bands, seed=4)
    vm.deco(deco_lapis, studs, seed=5)
    vm.deco(PT.deco_grain, body, amount=0.14)
    vm.deco(PT.deco_fill_glow, mot, trim, strength=0.8, seed=6, facets=False)
    vm.deco(deco_lapis, wheel2, seed=7)
    vm.deco(PT.deco_glyphs, wheel, glow, rows=1, glow=True, strength=0.7, seed=8)


add(Item("millennium_puzzle", build_puzzle_canon, dict(style="void", dark=(2, 4, 24), mid=(40, 70, 170), hot=(255, 232, 150)), hand_size=0.95, hand_style="hang", seed=101, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("wdjat glow", "aura_shell", "wdjat_glow"),
                 PL("eye glint", "glints", "nusmp:gem_sparkle", 0xFFE890, count=1, every=12, radius=0.4, rise=0.01)],
         acts={"other_me": [ST("SHADOW_UNITE", 0x2A4FC0, 3.2, 70, "self"), ST("DIVINE_HALO", 0xFFE890, 3.0, 60, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")],
               "shadow_game": [ST("SHADOW_POOL", 0x2A4FC0, 5.0, 90, "ground"), ST("MAGIC_CIRCLE_EXPLOSION", 0xF0C050, 4.6, 60, "ground"), ST("DIVINE_SIGIL", 0xFFE890, 3.0, 50, "sky")],
               "summon_duel_monster": [ST("MAGIC_CIRCLE", 0xF0C050, 4.4, 60, "front"), ST("SHADOW_UNITE", 0x2A4FC0, 3.0, 50, "front"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 14, "front")]}))

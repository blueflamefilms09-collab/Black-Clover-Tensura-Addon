"""The Millennium Items, part 3 (Yu-Gi-Oh!): the Key (a lapis belt with the ankh key and the door to the soul room), the Scale (a golden balance that weighs a heart against a feather) and the
Necklace (the broad collar over the chest, with the Eye of Wdjat and the visions it shows)."""
import math

import numpy as np

from common import *  # noqa: F401,F403
from common import (geo, K, PT, V, Item, GL, PL, ST, MAIN, TRIM, GLOW, DARK, ACCENT, GOLD, GOLD_HI, GOLD_DK, LAPIS, LAPIS_DK, LAPIS_HI, TURQ, deco_lapis, deco_hiero, deco_wdjat, wdjat,
                    paint_wdjat, ankh, orbit_motes)

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


# ====================================================================== MILLENNIUM KEY (belt)
def build_key(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    BY, SQ = -10.8, 0.56                                              # the belt line and the oval of the waist
    g_b = vm.grp("belt", "bob", (0, BY, 0), 0.18, 0.05, 0.0)
    g_key = vm.grp("ankh_key", "swing", (0, -12.2, -3.2), 5.0, 0.07, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, -14.9, -3.5), 0.12, 0.14, 0.0)
    g_door = vm.grp("soul_door", "bob", (6.8, -9.0, -0.5), 0.5, 0.06, 0.0)
    g_room = vm.grp("soul_room", "pulse", (6.8, -9.0, -0.5), 0.06, 0.1, 0.0)
    g_motes = vm.grp("soul_motes", "spin", (0, BY, 0), 1.6)
    g_ring = vm.grp("lock_ring", "spin", (0, BY, 0), -0.9)
    g_tails = vm.grp("belt_tails", "sway", (0, BY, -3.0), 4.0, 0.07, 0.0)
    g_rip = vm.grp("door_ripple", "pulse", (7.4, -9.0, -0.5), 0.2, 0.1, 0.0)
    g_gl = vm.grp("key_glints", "pulse", (0, -15.0, -3.5), 0.14, 0.2, 1.1)
    # the belt: a lapis band round the waist with a hieroglyph row, gold edges
    band = vm.P("belt_band", "stone", group=g_b, base=LAPIS, slot=MAIN, lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(BY - 0.95, 4.55), (BY - 0.6, 4.8), (BY + 0.6, 4.8), (BY + 0.95, 4.55)], sides=vm.S(24, 10), caps=(False, False), squash=SQ)
    edge = vm.P("belt_edges", "gold", group=g_b, base=GOLD, slot=TRIM, lod=False)
    for yy in (BY - 1.0, BY + 1.0):
        geo.lathe(edge, "y", (0, 0, 0), [(yy - 0.2, 4.62), (yy + 0.2, 4.88)], sides=vm.S(24, 10), caps=(False, False), squash=SQ)
    buckle = vm.P("buckle", "gold", group=g_b, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(buckle, geo.prim_box(3.8, 2.4, 0.7, chamfer=0.3), None, (0, BY, -2.95))
    ins = vm.P("buckle_lapis", "stone", group=g_b, base=LAPIS_DK, slot=MAIN, lod=False)
    geo.merge(ins, geo.prim_box(3.1, 1.7, 0.2, chamfer=0.1), None, (0, BY, -3.35))
    # the ankh key hanging from the buckle on a ring, the Eye of Wdjat in its loop
    ak = ankh(vm, g_key, (0, -14.9, -3.5), 1.5, thick=1.0, lod=True)
    ring = vm.P("key_ring", "gold", group=g_key, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(ring, geo.prim_torus(0.55, 0.16, n=vm.S(10, 6), m=4, plane="yz"), None, (0, -12.2, -3.4))
    eye = wdjat(vm, g_eye, (0, -14.75, -4.25), 0.82, lod=False)
    paint_wdjat(vm, eye, glow, trim)
    tailb = vm.P("key_bit", "gold", group=g_key, base=GOLD_DK, slot=TRIM, lod=False)
    for k, yy in enumerate((-17.0, -17.9)):
        geo.merge(tailb, geo.prim_box(1.4 - 0.3 * k, 0.5, 0.7), None, (0.9 + 0.1 * k, yy, -3.5))
    # tails of the belt, striped cloth with gold ends
    tails = vm.P("belt_tails", "cloth", group=g_tails, base=LAPIS, slot=MAIN, lod=False)
    tg = vm.P("tail_tips", "gold", group=g_tails, base=GOLD, slot=TRIM, lod=False)
    for sx in (-1, 1):
        geo.ribbon(tails, [(sx * 2.7, BY - 0.9, -3.1), (sx * 2.9, BY - 3.3, -3.35), (sx * 2.7, BY - 5.6, -3.2)], 1.5, thick=0.18, up=(0, 0, -1), taper=0.0)
        geo.merge(tg, geo.prim_box(1.55, 0.35, 0.3), None, (sx * 2.7, BY - 5.8, -3.2))
        geo.merge(tg, geo.prim_octa(0.34, 0.45), None, (sx * 2.7, BY - 6.3, -3.2))
    # the door to the soul room at the left hip: a gold arch with a lapis frame, a pool of stars within
    door = vm.P("soul_door", "gold", group=g_door, base=GOLD, slot=TRIM, lod=False)
    for sz in (-1, 1):
        geo.tube_path(door, [(6.8, -12.6, -0.5 + sz * 2.0), (6.8, -7.3, -0.5 + sz * 2.0)], 0.38, sides=4, caps=(True, True))
    geo.tube_path(door, [(6.8, -7.2 + 1.9 * math.sin(a), -0.5 + 2.0 * math.cos(a)) for a in np.linspace(0.0, math.pi, vm.S(10, 4))], 0.38, sides=4, caps=(True, True))
    sill = vm.P("door_sill", "stone", group=g_door, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(sill, geo.prim_box(0.9, 0.5, 4.8, chamfer=0.1), None, (6.8, -12.8, -0.5))
    room = vm.star("soul_room", g_room)
    geo.merge(room, geo.prim_box(0.2, 5.2, 3.6), None, (6.9, -9.9, -0.5))
    mot = orbit_motes(vm, "soul_motes", g_motes, 7, 7.6, BY, 0.5, (255, 224, 140), slot=GLOW, wobble=1.2, phase=0.5)
    lk = vm.P("lock_ring", "gold", group=g_ring, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(lk, geo.prim_torus(6.6, 0.22, n=vm.S(26, 10), m=3), None, (0, BY - 0.6, 0))
    vm.fx_disc(g_rip, GLOW, (7.5, -9.9, -0.5), (1, 0, 0), 9.0, "ripple")
    vm.fx_cross(g_gl, ACCENT, (0, -16.8, -3.6), (0, 1, 0), 3.6, "glint", n=2)
    K.halo_set(vm, pal, y=BY, R=8.2, rune=True, shaft=(-26.0, -2.0))
    vm.deco(deco_hiero, band, glow, rows=1, seed=2, glow=False, min_w=12, min_h=3)
    vm.deco(deco_lapis, band, seed=3)
    vm.deco(deco_lapis, ins, seed=4)
    vm.deco(deco_lapis, tails, seed=5)
    vm.deco(PT.deco_fill_glow, mot, glow, strength=0.9, seed=6, facets=False)
    vm.deco(PT.deco_grain, ak, amount=0.12)
    vm.deco(deco_hiero, lk, glow, rows=1, seed=7, glow=True, strength=0.7)
    vm.deco(PT.deco_filigree, buckle, trim, seed=8, min_w=8, min_h=8)


add(Item("millennium_key", build_key, dict(style="swirl", dark=(10, 6, 0), mid=(150, 104, 20), hot=(255, 232, 160)), hand_size=1.0, hand_style="coil", seed=104, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("soul room", "nebula_core", "soul_room"), GL("wdjat glow", "aura_shell", "wdjat_glow"), GL("ankh key", "aura_shell", "ankh_key"), GL("belt tails", "ribbons", "belt_tails"),
                 GL("soul motes", "orbit_shards", "soul_motes"), GL("lock ring", "halo_ring", "lock_ring"), GL("door ripple", "portal_ripples", "door_ripple"), GL("key glints", "lens_flare", "key_glints"),
                 GL("halo ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("unlocked minds", "motes", "nusmp:rune_key", None, count=1, every=8, radius=0.5, rise=0.012),
                 PL("soul glints", "glints", "nusmp:gem_sparkle", 0xFFD060, count=1, every=5, radius=0.45, rise=0.012)],
         acts={"open_the_soul_room": [ST("SPACE_PORTAL", 0xFFD060, 3.0, 70, "front"), ST("DIVINE_SIGIL", 0xE4AE3C, 3.2, 60, "ground"), ST("SPIRIT_AURA", 0x2C46A8, 2.6, 50, "self")],
               "mind_garden": [ST("DIVINE_HALO", 0xFFD060, 3.4, 80, "sky"), ST("MAGIC_CIRCLE", 0x2C46A8, 3.4, 60, "ground"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")]}))


# ====================================================================== MILLENNIUM SCALE (charm)
def build_scale(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_base = vm.grp("scale_base", "bob", (0, 0, 0), 0.4, 0.05, 0.0)
    g_beam = vm.grp("balance_beam", "sway", (0, 12, 0), 3.0, 0.07, 0.0)
    g_heart = vm.grp("heart_glow", "pulse", (-8.2, 2.5, 0), 0.1, 0.2, 0.0)
    g_feath = vm.grp("feather_light", "pulse", (8.2, 3.0, 0), 0.08, 0.14, 0.0)
    g_star = vm.grp("truth_star", "pulse", (0, 17, 0), 0.07, 0.12, 0.0)
    g_ma = vm.grp("drifting_feathers", "spin", (0, 9, 0), 0.9)
    g_hal = vm.grp("judgement_halo", "spin", (0, 19, 0), 1.0)
    g_hal2 = vm.grp("judgement_halo_b", "spin", (0, 21, 0), -1.5)
    g_ams = vm.grp("verdict_motes", "tumble", (0, 8, 0), 0.5, 0.4, 1.3)
    g_gc = vm.grp("ground_circle", "spin", (0, -9, 0), 0.7)
    # base: a lapis plinth with a gold foot, the Eye of Wdjat on its face
    plinth = vm.P("plinth", "stone", group=g_base, base=LAPIS, slot=MAIN, lod=True)
    geo.merge(plinth, geo.prim_box(9.0, 3.0, 5.0, chamfer=0.7), None, (0, -6.8, 0))
    foot = vm.P("foot", "gold", group=g_base, base=GOLD, slot=TRIM, lod=False)
    geo.merge(foot, geo.prim_box(10.4, 0.9, 6.2, chamfer=0.35), None, (0, -8.7, 0))
    geo.merge(foot, geo.prim_box(8.2, 0.55, 4.2, chamfer=0.2), None, (0, -5.1, 0))
    eye = wdjat(vm, g_base, (0, -6.8, -2.95), 1.7, lod=False)
    paint_wdjat(vm, eye, glow, trim)
    pillar = vm.P("pillar", "gold", group=g_base, base=GOLD_HI, slot=TRIM, lod=True)
    geo.lathe(pillar, "y", (0, 0, 0), [(-5.0, 1.5), (-4.0, 1.25), (-1.0, 1.0), (8.5, 0.95), (11.0, 1.2), (12.0, 0.9)], sides=vm.S(10, 6), caps=(False, False))
    bands = vm.P("pillar_bands", "stone", group=g_base, base=LAPIS_DK, slot=MAIN, lod=False)
    for yy in (-2.5, 1.0, 4.5, 8.0):
        geo.lathe(bands, "y", (0, 0, 0), [(yy - 0.45, 1.12), (yy + 0.45, 1.12)], sides=vm.S(10, 6), caps=(False, False))
    cap = vm.P("cap_ball", "stone", group=g_base, base=LAPIS_HI, slot=MAIN, lod=True)
    geo.merge(cap, geo.prim_sphere(1.4, 4, vm.S(8, 5)), None, (0, 13.2, 0))
    # the beam, swinging on the post, with lapis ends; pans on gold chains
    beam = vm.P("beam", "gold", group=g_beam, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(beam, geo.prim_box(17.0, 0.9, 0.9, chamfer=0.25), None, (0, 12.0, 0))
    ends = vm.P("beam_ends", "stone", group=g_beam, base=LAPIS, slot=MAIN, lod=False)
    for sx in (-1, 1):
        geo.merge(ends, geo.prim_octa(0.8, 1.0), None, (sx * 8.9, 12.0, 0))
    for sx, gname in ((-1, g_heart), (1, g_feath)):
        ch = vm.P("chains_%d" % sx, "gold", group=g_beam, base=GOLD, slot=TRIM, lod=False)
        for a in (0.0, 2.094, 4.189):
            geo.tube_path(ch, [(sx * 8.2, 11.6, 0.0), (sx * 8.2 + 2.2 * math.cos(a), 4.3, 2.2 * math.sin(a))], 0.12, sides=3, caps=(True, True))
        pan = vm.P("pan_%d" % sx, "gold", group=g_beam, base=GOLD, slot=TRIM, lod=False)
        geo.lathe(pan, "y", (sx * 8.2, 0, 0), [(3.4, 2.3), (3.8, 2.8), (4.4, 2.8), (4.6, 2.3)], sides=vm.S(12, 6), caps=(True, False))
        lap = vm.P("pan_inlay_%d" % sx, "stone", group=g_beam, base=LAPIS, slot=MAIN, lod=False)
        geo.lathe(lap, "y", (sx * 8.2, 0, 0), [(3.25, 2.2), (3.4, 2.3)], sides=vm.S(12, 6), caps=(True, True))
    # the heart (a ruby on the left pan) and the feather of Ma'at (on the right)
    heart = vm.P("heart", "crystal", group=g_heart, base=(214, 40, 60), slot=GLOW, lod=True)
    geo.merge(heart, geo.prim_octa(1.35, 1.7), None, (-8.2, 5.2, 0))
    feath = vm.P("feather", "bone", group=g_feath, base=(250, 248, 235), slot=ACCENT, lod=False)
    geo.merge(feath, geo.prim_leaf(5.0, 1.5, 0.3, sides=2, bend=0.0), geo.rot_matrix("z", 82), (7.0, 4.9, 0))
    # truth: a star over the beam, motes of verdict, feathers drifting, two halos
    star = vm.star("truth_star", g_star)
    geo.merge(star, geo.prim_octa(1.9, 2.6), None, (0, 17.0, 0))
    h1 = vm.P("judgement_halo", "gold", group=g_hal, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(h1, geo.prim_torus(8.2, 0.42, n=vm.S(26, 10), m=4), geo.rot_matrix("x", 10), (0, 19.5, 0))
    h2 = vm.P("judgement_halo_b", "stone", group=g_hal2, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(h2, geo.prim_torus(6.2, 0.28, n=vm.S(22, 8), m=3), geo.rot_matrix("x", -14) @ geo.rot_matrix("z", 6), (0, 21.0, 0))
    fl = vm.P("drifting_feathers", "bone", group=g_ma, base=(250, 248, 235), slot=ACCENT, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5
        geo.merge(fl, geo.prim_leaf(2.8, 1.0, 0.1, sides=2, bend=0.05), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 95), (13.0 * math.cos(a), 14.0 - 2.6 * i, 13.0 * math.sin(a)))
    mot = orbit_motes(vm, "verdict_motes", g_ams, 7, 11.0, 9.0, 0.6, (140, 245, 225), slot=GLOW, wobble=2.0)
    vm.fx_disc(g_gc, GLOW, (0, -9.4, 0), (0, 1, 0), 34.0, "seal")
    vm.fx_disc(g_gc, DARK, (0, -9.2, 0), (0, 1, 0), 25.0, "rune")
    K.halo_set(vm, pal, y=8.0, R=11.0, rune=False, shaft=(-6.0, 30.0))
    vm.deco(deco_lapis, plinth, seed=2)
    vm.deco(deco_hiero, plinth, glow, rows=2, seed=3, glow=True, strength=0.7, min_w=10, min_h=6)
    vm.deco(deco_lapis, bands, seed=4)
    vm.deco(deco_lapis, h2, seed=5)
    vm.deco(PT.deco_fill_glow, heart, (255, 60, 80), strength=1.0, seed=6)
    vm.deco(PT.deco_fill_glow, mot, glow, strength=0.9, seed=7, facets=False)
    vm.deco(PT.deco_veins, feath, glow, n=2, strength=0.6, seed=8, min_w=4, min_h=8)
    vm.deco(PT.deco_filigree, foot, trim, seed=9, min_w=10, min_h=4)
    vm.deco(PT.deco_grain, pillar, amount=0.12)
    vm.deco(deco_hiero, h1, glow, rows=1, seed=10, glow=True, strength=0.7)


add(Item("millennium_scale", build_scale, dict(style="swirl", dark=(0, 14, 16), mid=(24, 130, 120), hot=(190, 255, 240)), hand_size=1.0, hand_style="palm", seed=105,
         layers=[GL("heart glow", "aura_shell", "heart_glow"), GL("feather light", "aura_shell", "feather_light"), GL("balance beam", "aura_shell", "balance_beam"), GL("truth star", "nebula_core", "truth_star"),
                 GL("drifting feathers", "petals", "drifting_feathers"), GL("judgement halo", "halo_ring", "judgement_halo"), GL("judgement halo b", "halo_ring", "judgement_halo_b"),
                 GL("verdict motes", "orbit_shards", "verdict_motes"), GL("ground seal", "rune_ring", "ground_circle"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("falling feathers", "drips", "nusmp:feather_white", None, count=1, every=7, radius=0.6, rise=-0.012),
                 PL("verdict glints", "glints", "nusmp:gem_sparkle", 0x78F0D8, count=1, every=5, radius=0.5, rise=0.012),
                 PL("judging eyes", "motes", "nusmp:rune_eye", None, count=1, every=12, radius=0.5, rise=0.012)],
         acts={"weigh_the_heart": [ST("MIRROR_FRAME", 0x78F0D8, 3.0, 40, "front"), ST("DIVINE_SIGIL", 0xF4C85A, 3.0, 60, "ground")],
               "ammits_verdict": [ST("SEAL_CHAINS", 0xF4C85A, 4.0, 80, "ground"), ST("DIVINE_PILLAR", 0x78F0D8, 3.6, 80, "ground"), ST("SOUL_VORTEX", 0xF4C85A, 3.0, 60, "self")]},
         worn_scale=0.38, worn_offset=(11.6, -9.0, 0.0)))


# ====================================================================== MILLENNIUM NECKLACE (chest: the broad collar)
def build_necklace(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    CZ = -3.3                                                           # in front of the chest plate
    g_c = vm.grp("collar", "bob", (0, 0, CZ), 0.2, 0.05, 0.0)
    g_eye = vm.grp("wdjat_glow", "pulse", (0, -5.2, CZ - 0.9), 0.12, 0.14, 0.0)
    g_drops = vm.grp("collar_drops", "sway", (0, -5.0, CZ), 3.0, 0.08, 0.0)
    g_vis = vm.grp("visions", "spin", (0, -5.0, 0), 1.3)
    g_vis2 = vm.grp("visions_far", "spin", (0, -3.0, 0), -0.8)
    g_menat = vm.grp("menat", "sway", (0, 0.5, 2.8), 2.0, 0.06, 0.0)
    g_zod = vm.grp("time_ring", "spin", (0, -5.0, 0), 1.0)
    g_zod2 = vm.grp("time_ring_b", "tumble", (0, -5.0, 0), 0.0, 0.0, -1.6)
    g_pane = vm.grp("future_panes", "pulse", (0, -5.0, -8.0), 0.08, 0.1, 0.0)
    g_wing = vm.grp("vulture_wings", "sway", (0, -0.5, 2.6), 2.0, 0.05, 0.0)
    g_lens = vm.grp("collar_glints", "pulse", (0, -3.0, CZ), 0.14, 0.2, 0.9)
    # five rows of the collar: arcs over the chest, lapis, gold and turquoise bands, the outermost with drops
    rows = [(3.4, "stone", LAPIS), (4.3, "gold", GOLD_HI), (5.2, "stone", LAPIS_HI), (6.1, "gold", GOLD), (7.0, "stone", LAPIS_DK)]
    for ri, (r, mat, base) in enumerate(rows):
        p = vm.P("collar_row_%d" % ri, mat, group=g_c, base=base, slot=MAIN if mat == "stone" else TRIM, lod=(ri in (1, 4)))
        pts = [(r * math.cos(t), 0.5 + 0.86 * r * math.sin(t), CZ - 0.06 * ri) for t in np.linspace(math.radians(195), math.radians(345), vm.S(18, 7))]
        geo.ribbon(p, pts, 0.95, thick=0.2, up=(0, 0, -1), taper=0.0)
        if mat == "stone":
            vm.deco(deco_lapis, p, seed=ri, flecks=0.8)
    beads = vm.P("collar_beads", "crystal", group=g_c, base=(120, 236, 236), slot=GLOW, lod=False)
    for ri in (1, 3):
        r = rows[ri][0]
        for t in np.linspace(math.radians(215), math.radians(325), vm.S(5, 3)):
            geo.merge(beads, geo.prim_octa(0.3, 0.38), None, (r * math.cos(t), 0.5 + 0.86 * r * math.sin(t), CZ - 0.06 * ri - 0.24))
    drops = vm.P("collar_drops", "gold", group=g_drops, base=GOLD_HI, slot=TRIM, lod=False)
    drop_l = vm.P("collar_drops_lapis", "stone", group=g_drops, base=LAPIS, slot=MAIN, lod=False)
    r = 7.0
    for i, t in enumerate(np.linspace(math.radians(200), math.radians(340), vm.S(11, 3))):
        x, y = r * math.cos(t), 0.5 + 0.86 * r * math.sin(t)
        geo.merge(drops if i % 2 == 0 else drop_l, geo.prim_octa(0.42, 1.0), None, (x, y - 1.0, CZ - 0.25))
    # the great eye at the middle, on a gold plate
    plate = vm.P("eye_plate", "gold", group=g_c, base=GOLD, slot=TRIM, lod=True)
    geo.merge(plate, geo.prim_lathe([(-0.15, 2.5), (0.0, 2.7), (0.3, 2.3), (0.45, 0.0)], sides=vm.S(16, 7), caps=(True, False)), geo.rot_matrix("x", -90), (0, -5.0, CZ - 0.1))
    core = vm.star("time_core", g_eye)
    geo.merge(core, geo.prim_octa(1.9, 2.5), None, (0, -5.0, CZ + 0.9))
    eye = wdjat(vm, g_eye, (0, -5.0, CZ - 0.75), 1.5, lod=True)
    paint_wdjat(vm, eye, glow, trim)
    # a counterweight behind the neck (the menat), vulture wings over the shoulders at the back
    men = vm.P("menat", "gold", group=g_menat, base=GOLD, slot=TRIM, lod=False)
    geo.merge(men, geo.prim_box(3.6, 3.0, 0.6, chamfer=0.25), None, (0, -1.5, 2.9))
    menl = vm.P("menat_lapis", "stone", group=g_menat, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(menl, geo.prim_box(2.8, 2.2, 0.2, chamfer=0.1), None, (0, -1.5, 3.25))
    wing = vm.P("vulture_wings", "gold", group=g_wing, base=GOLD_HI, slot=TRIM, lod=False)
    fe = geo.prim_leaf(4.4, 1.2, 0.25, sides=2, bend=0.0)
    for sx in (-1, 1):
        for k in range(3):
            geo.place(wing, fe, (sx * 3.2, -0.4 - 0.9 * k, 2.6), up=(sx * (1.0 - 0.15 * k), -0.35 - 0.2 * k, 0.15), spin=0.0)
    # visions: panes of the future circling the wearer, a zodiac of time, future mirrors
    pane = vm.P("visions", "stone", group=g_vis, base=LAPIS_HI, slot=MAIN, lod=False)
    paneg = vm.P("vision_frames", "gold", group=g_vis, base=GOLD, slot=TRIM, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5
        M = geo.rot_matrix("y", -math.degrees(a) + 90)
        pos = np.array([9.4 * math.cos(a), -5.0 + 1.2 * math.sin(i * 2.4), 9.4 * math.sin(a)])
        geo.merge(pane, geo.prim_card(2.4, 3.2, 0.5), M, pos)
        geo.merge(paneg, geo.prim_card(3.0, 3.8, 0.2), M, pos)
    far = orbit_motes(vm, "visions_far", g_vis2, 6, 12.5, -3.0, 0.7, (130, 245, 240), slot=GLOW, wobble=2.4)
    z1 = vm.P("time_ring", "gold", group=g_zod, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(z1, geo.prim_torus(7.8, 0.3, n=vm.S(26, 10), m=3), None, (0, -5.0, 0))
    z2 = vm.P("time_ring_b", "stone", group=g_zod2, base=LAPIS, slot=MAIN, lod=False)
    geo.merge(z2, geo.prim_torus(6.2, 0.22, n=vm.S(22, 8), m=3), geo.rot_matrix("x", 24), (0, -5.0, 0))
    vm.fx_disc(g_pane, GLOW, (0, -5.0, -9.0), (0, 0, 1), 10.0, "ripple")
    vm.fx_cross(g_lens, ACCENT, (0, -5.0, CZ - 1.0), (0, 1, 0), 4.0, "glint", n=2)
    K.halo_set(vm, pal, y=-5.0, R=9.0, rune=True, shaft=(-17.0, 7.0))
    vm.deco(deco_hiero, z1, glow, rows=1, seed=2, glow=True, strength=0.7)
    vm.deco(deco_lapis, pane, seed=3, flecks=0.4)
    vm.deco(deco_hiero, pane, glow, rows=2, seed=4, glow=True, strength=0.8, min_w=4, min_h=4)
    vm.deco(deco_lapis, z2, seed=5)
    vm.deco(deco_lapis, drop_l, seed=6)
    vm.deco(deco_lapis, menl, seed=7)
    vm.deco(PT.deco_fill_glow, beads, glow, strength=0.9, seed=8)
    vm.deco(PT.deco_fill_glow, far, glow, strength=0.9, seed=9, facets=False)
    vm.deco(PT.deco_filigree, plate, trim, seed=10, min_w=8, min_h=8)
    vm.deco(PT.deco_grain, drops, amount=0.12)


add(Item("millennium_necklace", build_necklace, dict(style="void", dark=(0, 8, 20), mid=(20, 100, 150), hot=(190, 255, 250)), hand_size=1.0, hand_style="garment", seed=106, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("time core", "nebula_core", "wdjat_glow"), GL("wdjat glow", "aura_shell", "wdjat_glow"), GL("collar drops", "aura_shell", "collar_drops"), GL("menat", "aura_shell", "menat"), GL("vulture wings", "petals", "vulture_wings"),
                 GL("visions", "orbit_shards", "visions"), GL("far visions", "orbit_shards", "visions_far"), GL("time ring", "halo_ring", "time_ring"), GL("time ring b", "halo_ring", "time_ring_b"),
                 GL("future panes", "portal_ripples", "future_panes"), GL("collar glints", "lens_flare", "collar_glints"), GL("halo ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("foreseen eyes", "motes", "nusmp:rune_eye", None, count=1, every=9, radius=0.55, rise=0.012),
                 PL("vision glints", "glints", "nusmp:star4", 0x62E6F0, count=1, every=5, radius=0.5, rise=0.012),
                 PL("collar gold", "glints", "nusmp:glint", 0xEBBB52, count=1, every=6, radius=0.45, rise=0.01)],
         acts={"foresight": [ST("MAGIC_CIRCLE", 0x62E6F0, 3.0, 70, "ground"), ST("SPIRIT_AURA", 0xEBBB52, 2.8, 50, "self"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")],
               "prophecy": [ST("DIVINE_SIGIL", 0x62E6F0, 3.0, 90, "ground"), ST("DIVINE_HALO", 0xEBBB52, 3.4, 80, "sky"), ST("SPACE_RINGGATE", 0x62E6F0, 3.0, 60, "ground")]}))

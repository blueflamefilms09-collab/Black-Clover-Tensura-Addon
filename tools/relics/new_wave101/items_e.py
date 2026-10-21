"""Eight more world-level artifacts, part 2: the Straw Hat of Joy Boy (One Piece, head), the Law of Cycles Gown (Madoka Magica, chest), the Greed Island Original Binder (Hunter x Hunter, charm)."""
import math

import numpy as np

from common import *  # noqa: F401,F403
from common import (geo, K, PT, V, Item, GL, PL, ST, MAIN, TRIM, GLOW, DARK, ACCENT, GOLD, GOLD_HI, GOLD_DK, deco_lapis, deco_hiero, deco_weave, deco_slots, orbit_motes)

ITEMS = []


def add(item):
    ITEMS.append(item)
    return item


# ====================================================================== STRAW HAT OF JOY BOY (head)
def build_hat(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_h = vm.grp("hat", "bob", (0, 8, 0), 0.3, 0.05, 0.0)
    g_brim = vm.grp("brim", "sway", (0, 7.2, 0), 1.2, 0.06, 0.0)
    g_cord = vm.grp("chin_cord", "sway", (0, 7.0, 0), 3.0, 0.07, 0.0)
    g_sun = vm.grp("sun_disc", "pulse", (0, 8.4, -5.3), 0.1, 0.14, 0.0)
    g_rays = vm.grp("sun_rays", "tumble", (0, 9.5, 6.4), 0.0, 0.5, 0.0)
    g_rays2 = vm.grp("sun_rays_b", "tumble", (0, 9.5, 6.8), 0.0, -0.8, 0.0)
    g_cloud = vm.grp("laughing_clouds", "spin", (0, 14.5, 0), 1.0)
    g_cloud2 = vm.grp("laughing_clouds_far", "spin", (0, 11.0, 0), -0.7)
    g_core = vm.grp("freedom_cloud", "pulse", (0, 14.0, 0), 0.07, 0.12, 0.0)
    g_notes = vm.grp("drum_notes", "tumble", (0, 12.0, 0), 0.5, 0.4, 1.2)
    g_hal = vm.grp("nika_halo", "spin", (0, 17.0, 0), 1.0)
    g_gl = vm.grp("sun_glints", "pulse", (0, 8.4, -5.8), 0.14, 0.2, 0.9)
    # the hat: a straw dome and a wide brim, woven
    crown = vm.P("crown", "cloth", group=g_h, base=(232, 196, 112), slot=MAIN, lod=True)
    geo.lathe(crown, "y", (0, 0, 0), [(7.2, 4.95), (8.2, 5.05), (10.0, 4.6), (11.4, 3.5), (12.2, 1.6), (12.5, 0.0)], sides=vm.S(20, 8), caps=(False, False))
    brim = vm.P("brim", "cloth", group=g_brim, base=(236, 202, 120), slot=MAIN, lod=True)
    geo.lathe(brim, "y", (0, 0, 0), [(7.15, 5.0), (7.05, 8.2), (6.7, 10.2), (6.55, 11.1), (6.9, 11.1), (7.3, 10.0), (7.45, 8.0), (7.4, 5.0)], sides=vm.S(24, 10), caps=(False, False))
    band = vm.P("red_band", "cloth", group=g_h, base=(202, 40, 36), slot=TRIM, lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(7.5, 5.12), (9.1, 5.14)], sides=vm.S(20, 8), caps=(False, False))
    bgold = vm.P("band_gold", "gold", group=g_h, base=GOLD_HI, slot=TRIM, lod=False)
    for yy in (7.45, 9.15):
        geo.lathe(bgold, "y", (0, 0, 0), [(yy - 0.12, 5.1), (yy + 0.12, 5.2)], sides=vm.S(20, 8), caps=(False, False))
    # the sun of Nika on the front of the band: a disc and eight rays
    sun = vm.P("sun_disc", "gold", group=g_sun, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(sun, geo.prim_lathe([(-0.15, 1.25), (0.0, 1.4), (0.3, 1.1), (0.4, 0.0)], sides=vm.S(14, 6), caps=(True, False)), geo.rot_matrix("x", -90), (0, 8.3, -5.2))
    sr = vm.P("sun_spikes", "gold", group=g_sun, base=GOLD, slot=TRIM, lod=False)
    for i in range(8):
        a = 2 * math.pi * i / 8
        geo.merge(sr, geo.prim_spike(0.26, 1.0, sides=4), geo.align_y((math.cos(a), math.sin(a), -0.15)), (1.4 * math.cos(a), 8.3 + 1.4 * math.sin(a), -5.25))
    smile = vm.P("sun_core", "crystal", group=g_sun, base=(255, 236, 150), slot=GLOW, lod=False)
    geo.merge(smile, geo.prim_octa(0.5, 0.6), None, (0, 8.3, -5.75))
    # the chin cords, each with a gold bead
    cord = vm.P("chin_cord", "leather", group=g_cord, base=(90, 56, 34), slot=ACCENT, lod=False)
    cb = vm.P("cord_beads", "gold", group=g_cord, base=GOLD, slot=TRIM, lod=False)
    for sx in (-1, 1):
        path = geo.bez3((sx * 4.9, 7.4, -1.0), (sx * 5.4, 2.4, -1.8), (sx * 3.2, 0.1, -4.3), (sx * 0.4, 0.1, -4.5), vm.S(8, 4))
        geo.tube_path(cord, path, 0.18, sides=3, caps=(True, True))
        geo.merge(cb, geo.prim_sphere(0.34, 3, 5), None, path[len(path) // 2])
    # sun rays behind the head: a great gold sun with long rays, turning, and a second ring of rays the other way
    big = vm.P("sun_rays", "gold", group=g_rays, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(big, geo.prim_torus(7.2, 0.3, n=vm.S(26, 10), m=3, plane="xy"), None, (0, 9.5, 6.4))
    for i in range(16):
        a = 2 * math.pi * i / 16
        geo.merge(big, geo.prim_spike(0.45, 3.4 if i % 2 == 0 else 2.2, sides=4), geo.align_y((math.cos(a), math.sin(a), 0.0)), (7.4 * math.cos(a), 9.5 + 7.4 * math.sin(a), 6.4))
    big2 = vm.P("sun_rays_b", "gold", group=g_rays2, base=(255, 240, 170), slot=ACCENT, lod=False)
    geo.merge(big2, geo.prim_torus(10.6, 0.18, n=vm.S(30, 10), m=3, plane="xy"), None, (0, 9.5, 6.8))
    for i in range(12):
        a = 2 * math.pi * i / 12 + 0.26
        geo.merge(big2, geo.prim_octa(0.35, 0.6), None, (10.6 * math.cos(a), 9.5 + 10.6 * math.sin(a), 6.8))
    # laughing clouds in two orbits and a swirl of white in the middle
    cl = vm.P("laughing_clouds", "bone", group=g_cloud, base=(250, 250, 252), slot=ACCENT, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5
        c = np.array([9.4 * math.cos(a), 14.5 + 0.8 * math.sin(i * 2.0), 9.4 * math.sin(a)])
        for off in ((0, 0, 0), (1.5, 0.3, 0.6), (-1.3, 0.2, -0.5)):
            geo.merge(cl, geo.prim_sphere(1.2, 2, 5), None, c + np.array(off))
    cl2 = vm.P("laughing_clouds_far", "bone", group=g_cloud2, base=(246, 246, 250), slot=ACCENT, lod=False)
    for i in range(4):
        a = 2 * math.pi * i / 4 + 0.7
        c = np.array([13.0 * math.cos(a), 11.0 + 1.0 * math.sin(i * 1.7), 13.0 * math.sin(a)])
        for off in ((0, 0, 0), (1.3, 0.2, 0.5)):
            geo.merge(cl2, geo.prim_sphere(1.0, 2, 5), None, c + np.array(off))
    core = vm.star("freedom_cloud", g_core)
    geo.merge(core, geo.prim_octa(3.0, 2.4), None, (0, 14.0, 0))
    nt = orbit_motes(vm, "drum_notes", g_notes, 6, 8.0, 12.0, 0.6, (255, 240, 170), slot=GLOW, wobble=2.0)
    hal = vm.P("nika_halo", "gold", group=g_hal, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(hal, geo.prim_torus(6.4, 0.34, n=vm.S(24, 8), m=3), geo.rot_matrix("x", 12), (0, 17.0, 0))
    vm.fx_disc(g_gl, GLOW, (0, 8.3, -5.9), (0, 0, 1), 5.0, "flare", lod=True)
    vm.fx_disc(g_rays, GLOW, (0, 9.5, 7.2), (0, 0, 1), 26.0, "rays")
    K.halo_set(vm, pal, y=10.0, R=8.0, rune=False, shafts=False)
    vm.deco(deco_weave, crown, seed=1)
    vm.deco(deco_weave, brim, seed=2, cell=3.4)
    vm.deco(PT.deco_grain, band, amount=0.16)
    vm.deco(PT.deco_filigree, sun, trim, seed=3, min_w=8, min_h=8)
    vm.deco(PT.deco_fill_glow, smile, glow, strength=1.0, seed=4, facets=False)
    vm.deco(PT.deco_fill_glow, nt, glow, strength=0.9, seed=5, facets=False)
    vm.deco(PT.deco_glyphs, hal, glow, rows=1, glow=True, strength=0.6, seed=6)
    vm.deco(PT.deco_grain, big, amount=0.1)


add(Item("straw_hat_of_joy_boy", build_hat, dict(style="swirl", dark=(24, 16, 4), mid=(210, 150, 40), hot=(255, 250, 220)), hand_size=1.0, hand_style="brim", seed=110, worn="pieces",
         pieces=[("", "head", False)], anchor="head",
         layers=[GL("freedom cloud", "nebula_core", "freedom_cloud"), GL("sun disc", "aura_shell", "sun_disc"), GL("sun rays", "halo_ring", "sun_rays"), GL("sun rays b", "halo_ring", "sun_rays_b"),
                 GL("laughing clouds", "petals", "laughing_clouds"), GL("far clouds", "petals", "laughing_clouds_far"), GL("drum notes", "orbit_shards", "drum_notes"), GL("nika halo", "halo_ring", "nika_halo"),
                 GL("chin cord", "ribbons", "chin_cord"), GL("brim", "aura_shell", "brim"), GL("sun glints", "lens_flare", "sun_glints"), GL("lens flare", "lens_flare", "lens_flare"),
                 PL("laughter notes", "motes", "nusmp:music_note", 0xFFF0B0, count=1, every=7, radius=0.55, rise=0.014),
                 PL("sun glints", "glints", "nusmp:star4", 0xFFF0B0, count=1, every=5, radius=0.5, rise=0.012),
                 PL("white puffs", "wisps", "nusmp:mote_soft", 0xFFFFFF, count=1, every=6, radius=0.6, rise=0.01)],
         acts={"gear_fifth": [ST("WIND_ZEPHYR", 0xFFFFFF, 3.4, 80, "self"), ST("DIVINE_HALO", 0xFFF0B0, 3.4, 70, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 2.0, 16, "self")],
               "rubber_bajrang": [ST("FIRE_BURST", 0xFFC040, 3.6, 40, "front"), ST("SLASH_WAVE", 0xFFF0B0, 3.4, 30, "front")],
               "drums_of_liberation": [ST("SONG_RING", 0xFFF0B0, 4.0, 70, "ground"), ST("SONG_NOTES", 0xFFC040, 3.4, 60, "self"), ST("DIVINE_HALO", 0xFFF0B0, 3.6, 80, "sky")]}))


# ====================================================================== LAW OF CYCLES GOWN (chest)
def build_gown(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    CZ = -3.3
    g_g = vm.grp("gown", "bob", (0, 0, 0), 0.25, 0.05, 0.0)
    g_bow = vm.grp("collar_bow", "sway", (0, -0.6, CZ), 2.0, 0.07, 0.0)
    g_gem = vm.grp("soul_gem", "pulse", (0, -4.4, CZ - 0.6), 0.12, 0.16, 0.0)
    g_skirt = vm.grp("skirt", "sway", (0, -10.4, 0), 2.0, 0.05, 0.0)
    g_train = vm.grp("train", "sway", (0, -10.0, 2.6), 5.0, 0.06, 0.0)
    g_rib = vm.grp("hair_ribbons", "sway", (0, 0.4, 2.8), 6.0, 0.08, 0.0)
    g_ring = vm.grp("cycle_ring", "tumble", (0, -4.0, 6.2), 0.0, 0.6, 0.0)
    g_ring2 = vm.grp("cycle_ring_b", "tumble", (0, -4.0, 6.6), 0.0, -0.9, 0.0)
    g_core = vm.grp("cosmic_core", "pulse", (0, -4.0, 6.0), 0.06, 0.1, 0.0)
    g_gems = vm.grp("soul_gems", "spin", (0, -4.0, 0), 1.2)
    g_bow = vm.grp("hope_bow", "sway", (10.4, -6.0, 0), 2.0, 0.05, 0.0)
    g_pet = vm.grp("falling_petals", "spin", (0, -2.0, 0), 0.8)
    g_gl = vm.grp("gown_glints", "pulse", (0, -4.4, CZ - 1.4), 0.14, 0.2, 1.0)
    PINK, PINK_DK, WHITE = (244, 140, 186), (218, 96, 150), (255, 236, 246)
    # the bodice: a jewelled brooch at the heart, in a gold ring, with four petals
    gem = vm.P("soul_gem", "crystal", group=g_gem, base=(255, 120, 190), slot=GLOW, lod=True)
    geo.merge(gem, geo.prim_octa(1.2, 1.7), None, (0, -4.4, CZ - 0.9))
    setting = vm.P("gem_setting", "gold", group=g_g, base=GOLD_HI, slot=TRIM, lod=True)
    geo.merge(setting, geo.prim_torus(1.7, 0.26, n=vm.S(14, 6), m=4, plane="xy"), None, (0, -4.4, CZ - 0.4))
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        geo.place(setting, geo.prim_leaf(2.2, 0.9, 0.2, sides=2), (1.7 * math.cos(a), -4.4 + 1.7 * math.sin(a), CZ - 0.5), up=(math.cos(a), math.sin(a), -0.15))
    # the collar: a frill of white petals round the neck and a big pink bow
    frill = vm.P("collar_frill", "cloth", group=g_g, base=WHITE, slot=ACCENT, lod=False)
    for i in range(10):
        a = 2 * math.pi * i / 10
        geo.place(frill, geo.prim_leaf(2.4, 1.3, 0.2, sides=2), (3.5 * math.sin(a), 0.5, -3.0 * math.cos(a) * 0.62), up=(math.sin(a), 0.5, -math.cos(a) * 0.6))
    bow = vm.P("bow", "cloth", group=g_bow, base=PINK, slot=MAIN, lod=True)
    for sx in (-1, 1):
        geo.merge(bow, geo.prim_sphere(1.5, 3, vm.S(8, 5), squash=0.55), geo.rot_matrix("z", sx * 20), (sx * 1.9, -1.0, CZ - 0.2))
    knot = vm.P("bow_knot", "gold", group=g_bow, base=GOLD, slot=TRIM, lod=False)
    geo.merge(knot, geo.prim_sphere(0.55, 3, 6), None, (0, -1.0, CZ - 0.3))
    tails = vm.P("bow_tails", "cloth", group=g_bow, base=PINK_DK, slot=MAIN, lod=False)
    for sx in (-1, 1):
        geo.ribbon(tails, [(sx * 0.6, -1.4, CZ - 0.3), (sx * 1.2, -3.2, CZ - 0.5), (sx * 1.0, -5.0, CZ - 0.4)], 1.0, thick=0.15, up=(0, 0, -1), taper=0.3)
    # shoulder frills: layered pink and white domes
    sh = vm.P("shoulder_frills", "cloth", group=g_g, base=WHITE, slot=ACCENT, lod=False)
    shp = vm.P("shoulder_pink", "cloth", group=g_g, base=PINK, slot=MAIN, lod=False)
    for sx in (-1, 1):
        for k, (r, y) in enumerate(((3.6, 0.9), (3.0, -0.6), (2.4, -2.0))):
            geo.merge(sh if k != 1 else shp, geo.prim_sphere(r, 3, vm.S(8, 5), squash=0.42), None, (sx * 5.4, y, 0.0))
    # the skirt: two flared layers (the pink under the white), a scalloped hem, and a long train behind
    sk1 = vm.P("skirt_under", "cloth", group=g_skirt, base=PINK, slot=MAIN, lod=True)
    geo.lathe(sk1, "y", (0, 0, 0), [(-10.4, 4.9), (-12.6, 5.7), (-15.0, 6.6), (-17.0, 7.4)], sides=vm.S(20, 8), caps=(False, False), squash=0.7)
    sk2 = vm.P("skirt_over", "cloth", group=g_skirt, base=WHITE, slot=ACCENT, lod=False)
    geo.lathe(sk2, "y", (0, 0, 0), [(-10.4, 5.15), (-12.4, 5.95), (-14.8, 6.9), (-16.0, 7.5)], sides=vm.S(20, 8), caps=(False, False), squash=0.7)
    hem = vm.P("skirt_hem", "gold", group=g_skirt, base=GOLD_HI, slot=TRIM, lod=False)
    geo.lathe(hem, "y", (0, 0, 0), [(-16.9, 7.35), (-17.2, 7.5)], sides=vm.S(20, 8), caps=(False, False), squash=0.7)
    for i in range(vm.S(10, 3)):
        a = 2 * math.pi * i / 10
        geo.merge(hem, geo.prim_octa(0.3, 0.45), None, (7.4 * math.cos(a), -17.1, 7.4 * 0.7 * math.sin(a)))
    train = vm.P("train", "cloth", group=g_train, base=(250, 232, 244), slot=ACCENT, lod=False)
    for sx in (-1, 1):
        geo.ribbon(train, [(sx * 1.8, -10.0, 2.7), (sx * 2.6, -15.0, 3.6), (sx * 2.2, -20.0, 4.2), (sx * 3.0, -26.0, 4.6)], 3.0, thick=0.2, up=(sx, 0, 0), taper=0.4)
    rib = vm.P("hair_ribbons", "cloth", group=g_rib, base=(216, 40, 80), slot=TRIM, lod=False)
    for sx in (-1, 1):
        geo.ribbon(rib, [(sx * 1.2, 0.5, 2.8), (sx * 2.4, -3.5, 3.6), (sx * 1.6, -8.0, 4.0), (sx * 2.6, -12.5, 4.4)], 1.4, thick=0.15, up=(sx, 0, 0), taper=0.4)
    # the Law: a cosmic core and two rings of petals and gems turning behind, soul gems circling, a bow of hope at the side
    core = vm.star("cosmic_core", g_core)
    geo.merge(core, geo.prim_octa(3.2, 3.8), None, (0, -4.0, 6.0))
    r1 = vm.P("cycle_ring", "gold", group=g_ring, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(r1, geo.prim_torus(8.8, 0.3, n=vm.S(30, 10), m=3, plane="xy"), None, (0, -4.0, 6.2))
    r1p = vm.P("cycle_petals", "cloth", group=g_ring, base=PINK, slot=MAIN, lod=False)
    for i in range(12):
        a = 2 * math.pi * i / 12
        geo.place(r1p, geo.prim_leaf(2.6, 1.0, 0.15, sides=2), (8.8 * math.cos(a), -4.0 + 8.8 * math.sin(a), 6.2), up=(math.cos(a), math.sin(a), 0.0))
    r2 = vm.P("cycle_ring_b", "silver", group=g_ring2, base=(255, 232, 246), slot=ACCENT, lod=False)
    geo.merge(r2, geo.prim_torus(6.0, 0.2, n=vm.S(24, 8), m=3, plane="xy"), None, (0, -4.0, 6.6))
    sg = orbit_motes(vm, "soul_gems", g_gems, 8, 10.4, -4.0, 0.6, (255, 150, 210), slot=GLOW, wobble=2.0)
    hb = vm.P("hope_bow", "gold", group=g_bow, base=GOLD_HI, slot=TRIM, lod=False)
    geo.tube_path(hb, [(10.4, -6.0 + 11.0 * t, -3.5 * (1 - t * t)) for t in np.linspace(-1, 1, vm.S(12, 5))], 0.3, sides=4, taper=(0.6, 0.6), caps=(True, True))
    geo.tube_path(hb, [(10.4, -17.0, 0.0), (10.4, 5.0, 0.0)], 0.07, sides=3, caps=(True, True))
    geo.merge(hb, geo.prim_spike(0.2, 5.0, sides=4), geo.rot_matrix("x", -90), (10.4, -6.0, 0.4))
    pet = vm.P("falling_petals", "cloth", group=g_pet, base=PINK, slot=MAIN, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(pet, geo.prim_leaf(2.0, 0.9, 0.1, sides=2, bend=0.04), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 95), (12.0 * math.cos(a), 6.0 - 3.0 * i, 12.0 * math.sin(a)))
    vm.fx_disc(g_ring, GLOW, (0, -4.0, 7.0), (0, 0, 1), 24.0, "seal")
    vm.fx_cross(g_gl, ACCENT, (0, -4.4, CZ - 1.8), (0, 1, 0), 4.6, "glint", n=2)
    K.halo_set(vm, pal, y=-6.0, R=10.0, rune=False, shaft=(-22.0, 4.0))
    vm.deco(PT.deco_fill_glow, gem, (255, 110, 180), strength=1.0, seed=2)
    vm.deco(PT.deco_fill_glow, sg, glow, strength=0.9, seed=3, facets=False)
    vm.deco(PT.deco_grain, hem, amount=0.1)
    vm.deco(PT.deco_glyphs, r1, glow, rows=1, glow=True, strength=0.6, seed=5)
    vm.deco(PT.deco_grain, setting, amount=0.1)
    vm.deco(PT.deco_veins, sk1, (255, 200, 230), n=3, strength=0.5, seed=6, min_w=10, min_h=10, dark=0.1)


add(Item("law_of_cycles_gown", build_gown, dict(style="swirl", dark=(24, 4, 18), mid=(210, 80, 150), hot=(255, 236, 250)), hand_size=1.0, hand_style="garment", seed=111, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("cosmic core", "nebula_core", "cosmic_core"), GL("soul gem", "aura_shell", "soul_gem"), GL("collar bow", "ribbons", "collar_bow"), GL("skirt", "ribbons", "skirt"), GL("train", "ribbons", "train"),
                 GL("hair ribbons", "ribbons", "hair_ribbons"), GL("cycle ring", "halo_ring", "cycle_ring"), GL("cycle ring b", "halo_ring", "cycle_ring_b"), GL("soul gems", "orbit_shards", "soul_gems"),
                 GL("hope bow", "aura_shell", "hope_bow"), GL("falling petals", "petals", "falling_petals"), GL("gown glints", "lens_flare", "gown_glints"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("petals of hope", "drips", "nusmp:petal_cherry", None, count=1, every=6, radius=0.6, rise=-0.012),
                 PL("law glints", "glints", "nusmp:gem_sparkle", 0xFFC0E0, count=1, every=5, radius=0.5, rise=0.012),
                 PL("holy motes", "motes", "nusmp:holy_ray", 0xFFE0F0, count=1, every=6, radius=0.5, rise=0.012)],
         acts={"arrow_of_hope": [ST("LIGHT_BLADE", 0xFFC0E0, 3.2, 36, "front"), ST("DIVINE_RAY", 0xF080B8, 3.0, 50, "front")],
               "purify_despair": [ST("DIVINE_HALO", 0xFFC0E0, 3.6, 80, "sky"), ST("ELF_CIRCLE", 0xF080B8, 3.6, 60, "ground")],
               "law_of_cycles": [ST("DIVINE_PILLAR", 0xFFC0E0, 4.6, 100, "ground"), ST("MAGIC_CIRCLE_EXPLOSION", 0xF080B8, 4.8, 70, "ground"), ST("DIVINE_SIGIL", 0xFFE0F0, 3.8, 80, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 2.4, 20, "self")]}))


# ====================================================================== GREED ISLAND ORIGINAL BINDER (charm)
def build_binder(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_b = vm.grp("binder", "bob", (0, 0, 0), 0.4, 0.05, 0.0)
    g_fan = vm.grp("card_fan", "sway", (0, 6, 0), 3.0, 0.06, 0.0)
    g_isl = vm.grp("floating_island", "bob", (0, 19, 0), 0.9, 0.04, 0.0)
    g_tree = vm.grp("palm", "sway", (0, 20, 0), 3.0, 0.07, 0.0)
    g_cards = vm.grp("spell_cards", "spin", (0, 4, 0), 1.5)
    g_cards2 = vm.grp("spell_cards_far", "spin", (0, 8, 0), -0.9)
    g_clasp = vm.grp("clasp_glow", "pulse", (0, -3, -2.2), 0.12, 0.16, 0.0)
    g_core = vm.grp("game_core", "pulse", (0, 19, 0), 0.07, 0.1, 0.0)
    g_hal = vm.grp("game_halo", "spin", (0, 22, 0), 1.0)
    g_wat = vm.grp("waterfall", "pulse", (0, 14, 0), 0.1, 0.2, 0.0)
    g_gc = vm.grp("ground_circle", "spin", (0, -9, 0), 0.7)
    # the book: green cover, cream pages, gold corners and spine bands
    cover = vm.P("cover", "leather", group=g_b, base=(48, 126, 78), slot=MAIN, lod=True)
    geo.merge(cover, geo.prim_box(11.0, 15.0, 3.4, chamfer=0.5), None, (0, 0, 0))
    pages = vm.P("pages", "bone", group=g_b, base=(246, 238, 208), slot=ACCENT, lod=False)
    geo.merge(pages, geo.prim_box(10.2, 14.2, 2.4), None, (0.5, 0, 0.0))
    cg = vm.P("corners", "gold", group=g_b, base=GOLD_HI, slot=TRIM, lod=False)
    for sx in (-1, 1):
        for sy in (-1, 1):
            geo.merge(cg, geo.prim_box(2.2, 2.2, 3.7, chamfer=0.3), None, (sx * 4.9, sy * 6.9, 0.0))
    for yy in (-5.0, -2.0, 1.0, 4.0):
        geo.merge(cg, geo.prim_box(0.8, 0.7, 3.8, chamfer=0.1), None, (-5.7, yy, 0.0))
    plate = vm.P("slot_plate", "leather", group=g_b, base=(34, 96, 60), slot=MAIN, lod=False)
    geo.merge(plate, geo.prim_box(8.6, 10.8, 0.25), None, (0.0, 0.4, -1.78))
    emblem = vm.P("emblem", "gold", group=g_b, base=GOLD, slot=TRIM, lod=False)
    geo.merge(emblem, geo.prim_lathe([(-0.1, 1.6), (0.0, 1.8), (0.25, 1.4), (0.35, 0.0)], sides=vm.S(14, 6), caps=(True, False)), geo.rot_matrix("x", -90), (0, 5.2, -1.95))
    clasp = vm.P("clasp", "gold", group=g_b, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(clasp, geo.prim_box(1.4, 2.0, 0.5, chamfer=0.15), None, (5.6, 0.0, -1.5))
    cj = vm.P("clasp_gem", "crystal", group=g_clasp, base=(130, 255, 170), slot=GLOW, lod=False)
    geo.merge(cj, geo.prim_octa(0.5, 0.65), None, (5.6, 0.0, -1.95))
    # a fan of spell cards rising from the pages
    fan = vm.P("card_fan", "bone", group=g_fan, base=(250, 240, 214), slot=ACCENT, lod=False)
    ff = vm.P("card_frames", "gold", group=g_fan, base=GOLD, slot=TRIM, lod=False)
    for k in range(5):
        ang = -34 + 17 * k
        M = geo.rot_matrix("z", ang)
        geo.merge(fan, geo.prim_card(3.4, 4.8, 0.2), M, M @ np.array([0.0, 2.6, 0.0]) + np.array([0.5, 7.0, 0.2 - 0.02 * k]))
        geo.merge(ff, geo.prim_card(3.8, 5.2, 0.12), M, M @ np.array([0.0, 2.6, 0.0]) + np.array([0.5, 7.0, 0.2 - 0.02 * k]))
    # the island of the game, floating above: a cone of rock, a green top, a palm and a waterfall
    rock = vm.P("island_rock", "stone", group=g_isl, base=(110, 96, 82), slot=DARK, lod=True)
    geo.lathe(rock, "y", (0, 0, 0), [(10.5, 0.0), (11.6, 1.6), (14.0, 3.6), (16.0, 4.6), (16.6, 4.9)], sides=vm.S(10, 6), caps=(False, False))
    grass = vm.P("island_grass", "cloth", group=g_isl, base=(70, 170, 80), slot=MAIN, lod=True)
    geo.lathe(grass, "y", (0, 0, 0), [(16.5, 4.95), (16.9, 5.1), (17.2, 3.5), (17.4, 0.0)], sides=vm.S(10, 6), caps=(False, True))
    wat = vm.P("waterfall", "crystal", group=g_wat, base=(130, 220, 255), slot=ACCENT, lod=False)
    geo.merge(wat, geo.prim_box(0.8, 4.4, 0.5), None, (4.5, 14.2, -1.2))
    trunk = vm.P("palm_trunk", "wood", group=g_tree, base=(130, 92, 52), slot=DARK, lod=False)
    geo.tube_path(trunk, geo.bez3((-0.5, 17.2, 0.0), (-1.0, 19.0, 0.2), (-0.2, 21.0, 0.2), (0.4, 22.5, 0.0), vm.S(6, 3)), 0.45, sides=4, taper=(1.0, 0.6), caps=(True, True))
    leaves = vm.P("palm_leaves", "cloth", group=g_tree, base=(60, 160, 70), slot=MAIN, lod=False)
    for k in range(5):
        a = 2 * math.pi * k / 5
        geo.place(leaves, geo.prim_leaf(4.2, 1.5, 0.15, sides=2, bend=-0.03), (0.4, 22.5, 0.0), up=(math.cos(a) * 0.9, 0.4, math.sin(a) * 0.9))
    core = vm.star("game_core", g_core)
    geo.merge(core, geo.prim_octa(2.4, 2.0), None, (0, 12.5, 0))
    # cards and a ring of light round the whole
    cc = vm.P("spell_cards", "bone", group=g_cards, base=(250, 240, 214), slot=ACCENT, lod=False)
    ccf = vm.P("spell_card_frames", "gold", group=g_cards, base=GOLD, slot=TRIM, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        M = geo.rot_matrix("y", -math.degrees(a) + 90)
        pos = (9.6 * math.cos(a), 4.0 + 1.0 * math.sin(i * 2.0), 9.6 * math.sin(a))
        geo.merge(cc, geo.prim_card(2.6, 3.6, 0.2), M, pos)
        geo.merge(ccf, geo.prim_card(3.0, 4.0, 0.12), M, pos)
    cf = orbit_motes(vm, "spell_cards_far", g_cards2, 7, 13.0, 8.0, 0.6, (160, 255, 190), slot=GLOW, wobble=2.2)
    hal = vm.P("game_halo", "gold", group=g_hal, base=GOLD_HI, slot=TRIM, lod=False)
    geo.merge(hal, geo.prim_torus(9.6, 0.34, n=vm.S(26, 10), m=3), geo.rot_matrix("x", 10), (0, 22.5, 0))
    vm.fx_disc(g_gc, GLOW, (0, -9.4, 0), (0, 1, 0), 34.0, "seal")
    vm.fx_disc(g_gc, DARK, (0, -9.2, 0), (0, 1, 0), 25.0, "rune")
    K.halo_set(vm, pal, y=6.0, R=11.0, rune=False, shaft=(-8.0, 30.0))
    vm.deco(PT.deco_grain, cover, amount=0.18)
    vm.deco(deco_slots, plate, glow, cols=8, rows=5, seed=1)
    vm.deco(PT.deco_filigree, emblem, trim, seed=2, min_w=10, min_h=10)
    vm.deco(PT.deco_glyphs, fan, glow, rows=2, glow=False, seed=3, min_w=6, min_h=6)
    vm.deco(PT.deco_glyphs, cc, glow, rows=2, glow=False, seed=4, min_w=6, min_h=6)
    vm.deco(PT.deco_fill_glow, cj, glow, strength=1.0, seed=5, facets=False)
    vm.deco(PT.deco_fill_glow, cf, glow, strength=0.9, seed=6, facets=False)
    vm.deco(PT.deco_filigree, cg, trim, seed=7, min_w=10, min_h=10)
    vm.deco(PT.deco_glyphs, hal, glow, rows=1, glow=True, strength=0.6, seed=8)


add(Item("greed_island_original_binder", build_binder, dict(style="swirl", dark=(0, 16, 6), mid=(30, 130, 70), hot=(190, 255, 210)), hand_size=1.0, hand_style="palm", seed=112,
         layers=[GL("game core", "nebula_core", "game_core"), GL("clasp glow", "aura_shell", "clasp_glow"), GL("card fan", "aura_shell", "card_fan"), GL("floating island", "aura_shell", "floating_island"),
                 GL("palm", "aura_shell", "palm"), GL("waterfall", "aura_shell", "waterfall"), GL("spell cards", "orbit_shards", "spell_cards"), GL("far cards", "orbit_shards", "spell_cards_far"),
                 GL("game halo", "halo_ring", "game_halo"), GL("ground circle", "rune_ring", "ground_circle"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("falling cards", "drips", "nusmp:card", None, count=1, every=7, radius=0.6, rise=-0.012),
                 PL("game glints", "glints", "nusmp:gem_sparkle", 0x90FFB0, count=1, every=5, radius=0.5, rise=0.012),
                 PL("leaf motes", "motes", "nusmp:leaf_green", None, count=1, every=8, radius=0.5, rise=0.01)],
         acts={"spell_card_leave": [ST("SPACE_PORTAL", 0x90FFB0, 3.0, 40, "front"), ST("SPELL_CARD", 0xE0C060, 2.4, 40, "self")],
               "spell_card_reflection": [ST("MIRROR_FRAME", 0x90FFB0, 3.2, 70, "front"), ST("BARRIER_FX1", 0xE0C060, 3.0, 60, "self")],
               "spell_card_magnetic_force": [ST("GRAVITY_WELL", 0x90FFB0, 3.6, 70, "ground"), ST("SPELL_CARD", 0xE0C060, 2.6, 40, "self")]},
         worn_scale=0.36, worn_offset=(11.6, -9.0, 0.0)))

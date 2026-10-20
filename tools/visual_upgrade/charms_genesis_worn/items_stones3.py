"""The Reality Stone (the Aether: a liquid red heart held by writhing tendrils) and the Soul Stone (an orange soul-gem cupped in a hand of bone)."""
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


# ====================================================================== REALITY (the Aether)
def build_reality(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_heart = vm.grp("aether_heart", "pulse", (0, 2, 0), 0.07, 0.13, 0.0)
    g_inner = vm.grp("aether_core", "tumble", (0, 2, 0), 0.8, 0.5, -1.1)
    g_t1 = vm.grp("tendril_a", "sway", (0, -7, 0), 4.0, 0.06, 0.0)
    g_t2 = vm.grp("tendril_b", "sway", (0, -7, 0), -4.5, 0.05, 1.7)
    g_t3 = vm.grp("tendril_c", "sway", (0, -7, 0), 3.5, 0.07, 3.1)
    g_rib = vm.grp("liquid_ribbons", "spin", (0, 2, 0), 2.0)
    g_rib2 = vm.grp("liquid_ribbons_b", "spin", (0, 2, 0), -3.0)
    g_drops = vm.grp("droplets", "tumble", (0, 2, 0), 0.0, 0.0, 2.8)
    g_base = vm.grp("float", "bob", (0, 0, 0), 0.4, 0.07, 0.0)
    # the Aether: two spheres of liquid red, one turning inside the other, in the nebula pass
    heart = vm.star("heart", g_heart)
    geo.merge(heart, geo.prim_sphere(4.6, 5, 9, squash=1.12), None, (0, 2, 0))
    core = vm.star("core", g_inner, lod=False)
    geo.merge(core, geo.prim_sphere(2.6, 4, 7), None, (0, 2, 0))
    # writhing tendrils that cup it
    for gi, (g, name, ph) in enumerate(((g_t1, "tendril_a", 0.0), (g_t2, "tendril_b", 2.1), (g_t3, "tendril_c", 4.2))):
        t = vm.P(name, "obsidian", group=g, base=(120, 22, 26), lod=False)
        c, s_ = math.cos(ph), math.sin(ph)
        pts = geo.bez3((3.0 * c, -7.0, 3.0 * s_), (9.0 * c, -3.0, 9.0 * s_), (-3.0 * s_ + 7 * c, 9.0, 3.0 * c + 7 * s_), (0.8 * c, 10.5, 0.8 * s_), vm.S(8, 4))
        n = len(pts)
        geo.sweep(t, pts, geo.circle(0.7, vm.S(4, 3)), scales=[(1.0 - 0.8 * (k / (n - 1)) ** 1.4,) * 2 for k in range(n)], caps=(True, True))
        vm.deco(PT.deco_veins, t, glow, n=2, strength=0.8, seed=gi, min_w=6, min_h=6)
    # the cup of the stone: a dark crimson bowl with a rim of petals
    bowl = vm.P("bowl", "iron", group=g_base, base=(92, 20, 26), lod=False)
    geo.lathe(bowl, "y", (0, 0, 0), [(-11.0, 1.6), (-10.0, 3.0), (-8.8, 3.6), (-7.4, 2.4), (-6.6, 1.8)], sides=vm.S(10, 5), caps=(True, False))
    petals = vm.P("bowl_petals", "gold", group=g_base, base=(230, 120, 120), slot=TRIM)
    geo.scatter_ring(petals, geo.prim_leaf(4.2, 1.5, 0.35, sides=2, bend=-0.015), vm.S(4, 3), 2.0, y=-6.6, up_out=True, tilt=60.0)
    # liquid ribbons spiralling up round the heart, and floating droplets
    for gi, (g, R0, ph) in enumerate(((g_rib, 7.0, 0.0), (g_rib2, 8.8, 2.6))):
        r = vm.P("ribbon_%d" % gi, "crystal", group=g, base=(230, 70, 70), slot=GLOW, lod=False)
        pts = geo.helix_pts((0, 2, 0), R0, 14.0, 1.25, vm.S(16, 6), phase=ph)
        geo.ribbon(r, pts, 1.4, thick=0.2, up=(0, 1, 0), taper=0.5)
        vm.deco(PT.deco_fill_glow, r, glow, strength=0.8, seed=gi + 7)
    drops = K.orbs(vm, "droplets", g_drops, 7, 10.5, 2.0, 0.6, "crystal", (240, 90, 90), slot=GLOW, phase=0.2, wobble=4.0, lod=False)
    vm.deco(PT.deco_fill_glow, drops, glow, strength=0.9, seed=9)
    g_dist = vm.grp("distortion", "pulse", (0, 2, 0), 0.12, 0.08, 0.0)
    vm.fx_cross(g_dist, DARK, (0, 2, 0), (0, 1, 0), 20.0, "web", n=3)
    K.halo_set(vm, pal, y=2.0, R=8.0, rune=True, shaft_len=20.0)
    vm.deco(PT.deco_glyphs, bowl, glow, text="REALITY", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_filigree, petals, trim, seed=4)


add(Item("the_reality_stone", build_reality, dict(style="swirl", dark=(30, 0, 4), mid=(150, 20, 24), hot=(255, 140, 120)), hand_size=0.74, hand_style="palm", seed=15,
         layers=[GL("aether heart", "nebula_core", "aether_heart"), GL("aether core", "nebula_core", "aether_core"), GL("tendril a", "ribbons", "tendril_a"), GL("tendril b", "ribbons", "tendril_b"),
                 GL("tendril c", "ribbons", "tendril_c"), GL("liquid ribbons", "ribbons", "liquid_ribbons"), GL("far ribbons", "ribbons", "liquid_ribbons_b"),
                 GL("droplets", "orbit_shards", "droplets"), GL("reality distortion", "distortion", "distortion"), GL("rune ring", "rune_ring", "halo_ring"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("aether motes", "motes", "dust", 0xE03A3A, count=1, every=4, radius=0.5, rise=0.012),
                 PL("rewrite glints", "glints", "end_rod", None, count=1, every=8, radius=0.55, rise=0.0)],
         acts={"polymorph": [ST("SHADOW_UNITE", 0xE03A3A, 1.8, 40, "self"), ST("DREAM_MANIFEST", 0xFF9A9A, 1.6, 30, "front")],
               "seeming": [ST("DREAM_MANIFEST", 0xE03A3A, 2.0, 36, "self")],
               "mirage": [ST("MIRROR_ARRAY", 0xFF9A9A, 3.0, 50, "self"), ST("DIVINE_HALO", 0xE03A3A, 2.0, 40, "self")]}))


# ====================================================================== SOUL
def build_soul(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_gem = vm.grp("soul_gem", "pulse", (0, 3, 0), 0.06, 0.1, 0.0)
    g_hand = vm.grp("bone_hand", "bob", (0, 0, 0), 0.3, 0.05, 0.0)
    g_souls = vm.grp("souls", "spin", (0, 3, 0), 2.3)
    g_souls2 = vm.grp("souls_far", "spin", (0, 5, 0), -1.4)
    g_wisp = vm.grp("soul_wisps", "spin", (0, 3, 0), 1.4)
    g_wisp2 = vm.grp("soul_wisps_b", "spin", (0, 3, 0), -2.1)
    g_smoke = vm.grp("soul_smoke", "pulse", (0, 6, 0), 0.2, 0.07, 0.0)
    # the soul gem: an oval cabochon of orange souls in the nebula pass
    gem = vm.star("gem", g_gem)
    geo.merge(gem, geo.prim_egg(3.2, 9.5, sides=vm.S(10, 5), rings=5), None, (0, 3, 0))
    # a hand of bone cupping it: four fingers and a thumb round a wrist cuff
    bone = vm.P("bone_fingers", "bone", group=g_hand, base=(226, 210, 176), lod=False)
    fin = geo.prim_finger(11.0, 0.95, segs=2, curl=1.05, sides=3)
    for i in range(5):
        th = 2 * math.pi * i / 5 + 0.3
        M = geo.rot_matrix("y", -math.degrees(th) + 90) @ geo.rot_matrix("z", -14.0 if i < 4 else -24.0)
        # fingers start on the cuff and lean out, then curl in over the gem
        geo.merge(bone, fin, geo.rot_matrix("y", math.degrees(th)) @ geo.rot_matrix("z", -16.0), (3.2 * math.cos(th), -6.0, 3.2 * math.sin(th)))
    cuff = vm.P("wrist_cuff", "gold", group=g_hand, base=(235, 190, 120), slot=TRIM, lod=True)
    geo.lathe(cuff, "y", (0, 0, 0), [(-14.5, 2.2), (-12.0, 3.1), (-9.6, 3.9), (-7.5, 3.4), (-6.5, 2.4)], sides=vm.S(10, 5), caps=(True, False))
    wrist = vm.P("wrist_bone", "bone", group=g_hand, base=(214, 196, 160))
    geo.lathe(wrist, "y", (0, 0, 0), [(-20.0, 1.4), (-18.0, 1.9), (-16.0, 1.5), (-14.5, 2.0)], sides=vm.S(6, 4), caps=(True, False))
    rings = vm.P("cuff_rings", "gold", group=g_hand, base=(255, 214, 150), slot=TRIM)
    for yy in (-11.2, -8.4):
        geo.merge(rings, geo.prim_torus(3.6 if yy > -10 else 3.2, 0.3, n=vm.S(8, 6), m=3), None, (0, yy, 0))
    # souls: pale flames orbiting, wisps of smoke spiralling up
    souls = K.orbs(vm, "souls", g_souls, 6, 6.6, 3.0, 0.6, "crystal", (255, 190, 120), slot=GLOW, phase=0.0, wobble=2.5, lod=False)
    souls2 = K.orbs(vm, "souls_far", g_souls2, 5, 9.4, 5.0, 0.45, "crystal", (255, 160, 80), slot=GLOW, phase=0.9, wobble=-3.0, lod=False)
    for gi, (g, R0, ph) in enumerate(((g_wisp, 5.2, 0.0), (g_wisp2, 6.4, 3.1))):
        w = vm.P("wisp_%d" % gi, "crystal", group=g, base=(255, 150, 70), slot=GLOW, lod=False)
        pts = geo.helix_pts((0, 8.5, 0), R0, 14.0, 1.5, vm.S(14, 6), phase=ph, taper=0.7)
        geo.ribbon(w, pts, 1.3, thick=0.2, up=(0, 1, 0), taper=0.85)
        vm.deco(PT.deco_fill_glow, w, glow, strength=0.75, seed=gi + 3)
    vm.deco(PT.deco_fill_glow, souls, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_fill_glow, souls2, glow, strength=0.9, seed=6)
    vm.fx_cross(g_smoke, DARK, (0, 6, 0), (0, 1, 0), 16.0, "smoke", n=3)
    K.halo_set(vm, pal, y=3.0, R=8.0, rune=True, shaft_len=20.0)
    vm.deco(PT.deco_glyphs, cuff, glow, text="SOUL", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_filigree, rings, trim, seed=4)
    vm.deco(PT.deco_grain, bone, amount=0.14)
    vm.deco(PT.deco_tint, wrist, (170, 130, 80), amount=0.25)


add(Item("the_soul_stone", build_soul, dict(style="void", dark=(24, 8, 0), mid=(170, 80, 12), hot=(255, 200, 110)), hand_size=0.85, hand_style="tool", seed=16,
         layers=[GL("soul gem", "nebula_core", "soul_gem"), GL("bone hand", "aura_shell", "bone_hand"), GL("orbiting souls", "orbit_shards", "souls"), GL("far souls", "orbit_shards", "souls_far"),
                 GL("soul wisps", "ribbons", "soul_wisps"), GL("soul wisps b", "ribbons", "soul_wisps_b"), GL("soul smoke", "distortion", "soul_smoke"), GL("rune ring", "rune_ring", "halo_ring"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("soul fire", "embers", "soul_fire_flame", None, count=1, every=5, radius=0.4, rise=0.02),
                 PL("ghost glints", "wisps", "dust", 0xFF8A2A, count=1, every=6, radius=0.5, rise=0.014)],
         acts={"souls": [ST("SOUL_VORTEX", 0xFF8A2A, 2.4, 60, "self"), ST("DIVINE_HALO", 0xFFC080, 2.0, 40, "self")]}))

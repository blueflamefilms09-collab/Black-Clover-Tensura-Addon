"""Genesis grade, worn on the head and the back: the Absolute Being's Crown Shard, Zahard's Crown Fragment and the Gate of Babylon Crest."""
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


# ====================================================================== ABSOLUTE BEING'S CROWN SHARD (head)
def build_absolute(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_crown = vm.grp("crown", "bob", (0, 8, 0), 0.25, 0.05, 0.0)
    g_halo = vm.grp("sovereign_halo", "spin", (0, 19, 0), 1.0)
    g_halo2 = vm.grp("sovereign_halo_b", "spin", (0, 21, 0), -1.7)
    g_orbit = vm.grp("orbiting_shards", "spin", (0, 12, 0), 2.2)
    g_orbit2 = vm.grp("orbiting_shards_far", "spin", (0, 15, 0), -1.3)
    g_chains = vm.grp("throne_chains", "spin", (0, 14, 0), 0.8)
    g_runes = vm.grp("floating_runes", "tumble", (0, 13, 0), 0.6, 0.4, 1.5)
    g_void = vm.grp("throne_void", "pulse", (0, 12, 8), 0.04, 0.07, 0.0)
    g_rib = vm.grp("royal_ribbons", "sway", (0, 9, 4.5), 7.0, 0.07, 0.0)
    g_seal = vm.grp("command_seal", "spin", (0, 12, 8.6), 0.5)
    g_core = vm.grp("shard_core", "pulse", (0, 11, 0), 0.06, 0.16, 0.0)
    # the band: a tapered ring of white metal over the brow, runes engraved round it, violet gems set in it
    band = vm.P("band", "silver", group=g_crown, base=(240, 236, 252), lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(6.6, 4.6), (7.4, 5.0), (8.8, 5.1), (9.8, 4.7), (10.4, 4.3)], sides=vm.S(18, 8), caps=(False, False))
    gems = vm.P("band_gems", "crystal", group=g_crown, base=(150, 100, 235), slot=GLOW, lod=False)
    for i in range(9):
        a = 2 * math.pi * i / 9
        geo.merge(gems, geo.prim_octa(0.7, 1.0), geo.align_y((math.cos(a), 0, math.sin(a))), (5.2 * math.cos(a), 8.6, 5.2 * math.sin(a)))
    # the shards of the crown: seven blades of broken light rising at the front and sides, the tallest over the brow
    shards = vm.P("crown_shards", "silver", group=g_crown, base=(232, 226, 252), slot=TRIM, lod=False)
    rng = np.random.default_rng(14)
    heights = (7.0, 11.5, 15.0, 12.5, 9.5, 13.5, 8.0)
    for i, h in enumerate(heights):
        a = math.radians(-100 + 200 * i / 6)                    # the front arc, from the right ear over the brow to the left
        c, s_ = math.cos(a - math.pi / 2), math.sin(a - math.pi / 2)
        tilt = 8 + 4 * abs(i - 3)
        d = np.array([c * math.sin(math.radians(tilt)), math.cos(math.radians(tilt)), s_ * math.sin(math.radians(tilt))])
        geo.place(shards, geo.prim_blade(h, 2.1, 0.55, tip=0.45), (5.0 * c, 9.2, 5.0 * s_), up=d, spin=math.degrees(a), scale=1.0)
    small = vm.P("small_shards", "crystal", group=g_crown, base=(206, 180, 255), slot=GLOW, lod=False)
    for i in range(8):
        a = math.radians(-125 + 250 * (i + 0.5) / 8)
        c, s_ = math.cos(a - math.pi / 2), math.sin(a - math.pi / 2)
        geo.place(small, geo.prim_crystal(0.7, rng.uniform(3.5, 6.0), sides=4, tip=0.5), (5.2 * c, 9.6, 5.2 * s_), up=(0.2 * c, 1, 0.2 * s_), scale=1.0)
    fil = vm.P("shard_wire", "silver", group=g_crown, base=(255, 255, 255), slot=TRIM, lod=False)
    for h, i in zip(heights, range(7)):
        a = math.radians(-100 + 200 * i / 6)
        c, s_ = math.cos(a - math.pi / 2), math.sin(a - math.pi / 2)
        geo.tube_path(fil, [(5.4 * c, 9.2, 5.4 * s_), (5.9 * c, 9.2 + h * 0.5, 5.9 * s_), (5.5 * c, 9.2 + h * 0.85, 5.5 * s_)], 0.14, sides=3, caps=(False, False))
    # a heart of the crown floating over the brow; two halos; shards, chains and runes in orbit
    core = vm.star("shard_core", g_core)
    geo.merge(core, geo.prim_octa(1.8, 3.6), None, (0, 12.0, -3.4))
    halo = vm.P("sovereign_halo", "silver", group=g_halo, base=(236, 228, 255), slot=TRIM, lod=True)
    geo.merge(halo, geo.prim_torus(7.4, 0.5, n=vm.S(26, 10), m=4), geo.rot_matrix("x", 10), (0, 19.0, 0))
    halo2 = vm.P("sovereign_halo_b", "silver", group=g_halo2, base=(150, 110, 230), slot=GLOW, lod=False)
    geo.merge(halo2, geo.prim_torus(5.6, 0.3, n=vm.S(20, 8), m=3), geo.rot_matrix("x", -16) @ geo.rot_matrix("z", 8), (0, 21.0, 0))
    orb1 = vm.P("orbiting_shards", "crystal", group=g_orbit, base=(190, 150, 255), slot=GLOW, lod=False)
    for i in range(8):
        a = 2 * math.pi * i / 8
        geo.merge(orb1, geo.prim_crystal(0.7, 3.6, sides=4, tip=0.5), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 20), (8.6 * math.cos(a), 12.0 + 1.6 * math.sin(i * 2.0), 8.6 * math.sin(a)))
    orb2 = vm.P("orbiting_shards_far", "silver", group=g_orbit2, base=(220, 214, 248), slot=TRIM, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6 + 0.4
        geo.merge(orb2, geo.prim_octa(0.8, 1.9), geo.rot_matrix("y", -math.degrees(a)) @ geo.rot_matrix("z", 30), (12.0 * math.cos(a), 15.0 + 2.0 * math.sin(i * 1.6), 12.0 * math.sin(a)))
    ch = vm.P("throne_chains", "iron", group=g_chains, base=(120, 108, 170), lod=False)
    link = geo.prim_link(1.0, 0.24, n=6, m=3)
    nl = vm.S(12, 4)
    for i in range(nl):
        a = 2 * math.pi * i / nl
        M = geo.align_y((-math.sin(a), 0, math.cos(a)))
        if i % 2:
            M = M @ geo.rot_matrix("y", 90)
        geo.merge(ch, link, M, (9.6 * math.cos(a), 14.0, 9.6 * math.sin(a)))
    rn = vm.P("floating_runes", "crystal", group=g_runes, base=(220, 200, 255), slot=GLOW, lod=False)
    for i in range(7):
        a = 2 * math.pi * i / 7
        geo.merge(rn, geo.prim_octa(0.9, 1.3), geo.rot_matrix("y", 30 * i) @ geo.rot_matrix("x", 24 * i), (10.6 * math.cos(a), 10.0 + 3.0 * math.sin(i * 1.4), 10.6 * math.sin(a)))
    rib = vm.P("royal_ribbons", "cloth", group=g_rib, base=(236, 230, 252), lod=False)
    for sg in (-1, 1):
        geo.ribbon(rib, [(sg * 3.0, 9.0, 4.6), (sg * 3.6, 5.0, 5.4), (sg * 3.0, 1.0, 5.2), (sg * 3.8, -3.0, 5.6)], 2.6, thick=0.15, up=(0, 0, 1), taper=0.35)
    # the void of the throne behind the head, with its seal
    void = vm.star("throne_void", g_void)
    geo.merge(void, geo.prim_lathe([(-0.2, 11.0), (0.2, 11.0)], sides=vm.S(14, 6), caps=(True, True)), geo.rot_matrix("x", 90), (0, 12.0, 8.0))
    vm.fx_disc(g_seal, GLOW, (0, 12.0, 8.6), (0, 0, 1), 26.0, "seal")
    vm.fx_disc(g_seal, DARK, (0, 12.0, 8.8), (0, 0, 1), 20.0, "rune")
    g_rays = vm.grp("sovereign_rays", "pulse", (0, 12, -4), 0.1, 0.07, 0.0)
    vm.fx_disc(g_rays, ACCENT, (0, 12.0, -4.5), (0, 0, 1), 30.0, "rays")
    K.halo_set(vm, pal, y=14.0, R=9.0, rune=False, shaft=(13.0, 38.0))
    vm.deco(PT.deco_glyphs, band, glow, text="ABSOLUTE SOVEREIGN", rows=2, glow=False, seed=3)
    vm.deco(PT.deco_fill_glow, gems, glow, strength=0.95, seed=4)
    vm.deco(PT.deco_fill_glow, small, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_fill_glow, orb1, glow, strength=0.9, seed=6)
    vm.deco(PT.deco_veins, shards, glow, n=2, strength=0.7, seed=7, min_w=6, min_h=8)
    vm.deco(PT.deco_glyphs, halo, glow, rows=1, glow=False, seed=8)
    vm.deco(PT.deco_fill_glow, rn, glow, strength=0.8, seed=9, facets=False)
    vm.deco(PT.deco_filigree, band, trim, seed=10, min_w=14, min_h=10)
    vm.deco(PT.deco_grain, shards, amount=0.1)


add(Item("absolute_beings_crown_shard", build_absolute, dict(style="void", dark=(4, 0, 12), mid=(86, 54, 160), hot=(226, 210, 255)), hand_size=0.9, hand_style="brim", seed=35, worn="pieces",
         pieces=[("", "head", False)], anchor="head",
         layers=[GL("shard heart", "nebula_core", "shard_core"), GL("sovereign halo", "halo_ring", "sovereign_halo"), GL("second halo", "halo_ring", "sovereign_halo_b"),
                 GL("orbiting shards", "orbit_shards", "orbiting_shards"), GL("far shards", "orbit_shards", "orbiting_shards_far"), GL("throne chains", "chains", "throne_chains"),
                 GL("floating runes", "orbit_shards", "floating_runes"), GL("royal ribbons", "ribbons", "royal_ribbons"), GL("throne void", "nebula_core", "throne_void"),
                 GL("command seal", "rune_ring", "command_seal"), GL("sovereign rays", "lens_flare", "sovereign_rays"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("sovereign motes", "motes", "end_rod", None, count=2, every=3, radius=0.6, rise=0.014),
                 PL("void wisps", "wisps", "dust", 0x6A4AA8, count=1, every=5, radius=0.55, rise=0.012),
                 PL("crown glints", "glints", "dust", 0xD8C0FF, count=1, every=6, radius=0.5, rise=0.01)],
         acts={"sovereigns_will": [ST("SHADOW_UNITE", 0x6A4AA8, 3.6, 80, "self"), ST("DIVINE_HALO", 0xD8C0FF, 3.4, 70, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 1.8, 16, "self")],
               "absolute_command": [ST("SEAL_CHAINS", 0xD8C0FF, 4.0, 90, "front"), ST("MAGIC_CIRCLE_EXPLOSION", 0x6A4AA8, 4.4, 60, "ground"), ST("DEMON_METEOR", 0x6A4AA8, 3.0, 56, "sky")]}))


# ====================================================================== ZAHARD'S CROWN FRAGMENT (head)
def build_zahard(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_crown = vm.grp("king_crown", "bob", (0, 8, 0), 0.25, 0.05, 0.0)
    g_t1 = vm.grp("tower_floor_1", "spin", (0, 18, 0), 0.9)
    g_t2 = vm.grp("tower_floor_2", "spin", (0, 21, 0), -1.4)
    g_t3 = vm.grp("tower_floor_3", "spin", (0, 24, 0), 2.0)
    g_t4 = vm.grp("tower_floor_4", "spin", (0, 27, 0), -2.8)
    g_kids = vm.grp("children", "spin", (0, 14, 0), 1.9)
    g_kids2 = vm.grp("children_far", "spin", (0, 17, 0), -1.1)
    g_pend = vm.grp("jewel_pendants", "sway", (0, 8, 0), 6.0, 0.08, 0.0)
    g_cloth = vm.grp("ivory_mantle", "sway", (0, 9, 5), 6.0, 0.06, 0.0)
    g_plan = vm.grp("grand_plan", "pulse", (0, 14, 0), 0.07, 0.1, 0.0)
    g_seal = vm.grp("king_seal", "spin", (0, 9, 0), -0.7)
    # a fragment of a king's crown: a gold band broken open at the back, five tall pointed arches, a gem at every apex
    band = vm.P("band", "gold", group=g_crown, base=(246, 222, 150), lod=True)
    geo.lathe(band, "y", (0, 0, 0), [(6.6, 4.7), (7.6, 5.2), (9.0, 5.2), (9.8, 4.8)], sides=vm.S(16, 8), caps=(False, False))
    arches = vm.P("arches", "gold", group=g_crown, base=(255, 232, 160), slot=TRIM, lod=False)
    apex = vm.P("apex_gems", "crystal", group=g_crown, base=(170, 120, 230), slot=GLOW, lod=False)
    for i in range(5):
        a = math.radians(-120 + 240 * i / 4)
        c, s_ = math.cos(a - math.pi / 2), math.sin(a - math.pi / 2)
        side = np.array([-s_, 0, c])
        base_c = np.array([5.1 * c, 9.4, 5.1 * s_])
        h = 12.0 if i == 2 else 9.5 if i in (1, 3) else 7.0
        for sg in (-1, 1):
            p0 = base_c + side * sg * 2.4
            p1 = base_c + side * sg * 2.4 + np.array([0, h * 0.55, 0])
            p2 = base_c + side * sg * 1.0 + np.array([0, h * 0.85, 0])
            p3 = base_c + np.array([0, h, 0])
            geo.tube_path(arches, geo.bez3(tuple(p0), tuple(p1), tuple(p2), tuple(p3), vm.S(5, 3)), 0.42, sides=3, taper=(1.0, 0.7))
        geo.merge(apex, geo.prim_octa(0.9, 1.3), None, tuple(base_c + np.array([0, h + 0.8, 0])))
    pend = vm.P("jewel_pendants", "crystal", group=g_pend, base=(255, 224, 140), slot=GLOW, lod=False)
    wire = vm.P("pendant_wire", "gold", group=g_pend, base=(240, 206, 120), slot=TRIM, lod=False)
    for i in range(7):
        a = math.radians(80 + 200 * i / 6)
        c, s_ = math.cos(a - math.pi / 2), math.sin(a - math.pi / 2)
        geo.tube_path(wire, [(5.3 * c, 7.0, 5.3 * s_), (5.6 * c, 4.4, 5.6 * s_)], 0.14, sides=3, caps=(False, False))
        geo.merge(pend, geo.prim_octa(0.55, 1.0), None, (5.6 * c, 3.8, 5.6 * s_))
    # the Tower over the crown: floors as rings, each turning its own way, a spire on top
    for gi, (g, R, y, base) in enumerate(((g_t1, 6.0, 18.0, (246, 228, 170)), (g_t2, 4.8, 21.0, (232, 214, 250)), (g_t3, 3.7, 24.0, (246, 228, 170)), (g_t4, 2.7, 27.0, (232, 214, 250)))):
        p = vm.P("tower_floor_%d" % (gi + 1), "gold", group=g, base=base, slot=TRIM if gi % 2 == 0 else GLOW, lod=(gi == 0))
        geo.merge(p, geo.prim_torus(R, 0.48 - 0.05 * gi, n=vm.S(14, 7), m=3), None, (0, y, 0))
        for k in range(4):
            a = math.pi / 2 * k + gi
            geo.merge(p, geo.prim_octa(0.5, 0.8), None, (R * math.cos(a), y, R * math.sin(a)))
    spire = vm.P("tower_spire", "gold", group=g_t4, base=(255, 238, 180), slot=TRIM, lod=False)
    geo.merge(spire, geo.prim_spike(1.0, 5.0, sides=4), None, (0, 27.0, 0))
    plan = vm.star("grand_plan", g_plan)
    geo.merge(plan, geo.prim_octa(2.2, 4.4), None, (0, 14.0, 0))
    kids = K.orbs(vm, "children", g_kids, 6, 9.0, 14.0, 0.7, "crystal", (255, 236, 160), slot=GLOW, phase=0.0, wobble=2.0, lod=False)
    kids2 = K.orbs(vm, "children_far", g_kids2, 8, 12.4, 17.0, 0.5, "crystal", (226, 200, 255), slot=ACCENT, phase=0.4, wobble=-3.0, lod=False)
    cloth = vm.P("ivory_mantle", "cloth", group=g_cloth, base=(246, 240, 226), lod=False)
    for sg in (-1, 1):
        geo.ribbon(cloth, [(sg * 3.4, 9.0, 5.2), (sg * 4.4, 5.0, 5.8), (sg * 3.6, 1.0, 5.6), (sg * 4.6, -4.0, 6.0)], 3.0, thick=0.15, up=(0, 0, 1), taper=0.3)
    vm.fx_disc(g_seal, GLOW, (0, 9.4, 0), (0, 1, 0), 26.0, "seal")
    vm.fx_disc(g_seal, DARK, (0, 9.2, 0), (0, 1, 0), 20.0, "rune")
    K.halo_set(vm, pal, y=15.0, R=9.0, rune=False, shaft=(14.0, 40.0))
    vm.deco(PT.deco_glyphs, band, glow, text="KING OF THE TOWER", rows=2, glow=True, strength=0.85, seed=3)
    vm.deco(PT.deco_fill_glow, apex, glow, strength=0.95, seed=4)
    vm.deco(PT.deco_fill_glow, pend, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_fill_glow, kids, glow, strength=0.9, seed=6)
    vm.deco(PT.deco_filigree, band, trim, seed=7, min_w=12, min_h=8)
    vm.deco(PT.deco_enamel, arches, (236, 214, 255), (250, 226, 160), seed=8, min_w=4, min_h=4)
    vm.deco(PT.deco_grain, band, amount=0.14)


add(Item("zahards_crown_fragment", build_zahard, dict(style="swirl", dark=(10, 8, 0), mid=(130, 100, 40), hot=(255, 236, 170)), hand_size=0.9, hand_style="brim", seed=36, worn="pieces",
         pieces=[("", "head", False)], anchor="head",
         layers=[GL("grand plan", "nebula_core", "grand_plan"), GL("tower floor 1", "halo_ring", "tower_floor_1"), GL("tower floor 2", "halo_ring", "tower_floor_2"), GL("tower floor 3", "halo_ring", "tower_floor_3"),
                 GL("tower floor 4", "halo_ring", "tower_floor_4"), GL("children", "orbit_shards", "children"), GL("far children", "orbit_shards", "children_far"), GL("jewel pendants", "aura_shell", "jewel_pendants"),
                 GL("ivory mantle", "ribbons", "ivory_mantle"), GL("king seal", "rune_ring", "king_seal"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("royal motes", "motes", "end_rod", None, count=2, every=3, radius=0.6, rise=0.014),
                 PL("gold dust", "drips", "dust", 0xFFE8A0, count=1, every=5, radius=0.6, rise=-0.01),
                 PL("tower glints", "glints", "dust", 0xD8C8F8, count=1, every=6, radius=0.5, rise=0.012)],
         acts={"kings_edict": [ST("DIVINE_SIGIL", 0xFFE8A0, 3.2, 70, "sky"), ST("MAGIC_CIRCLE", 0x8A6AC8, 3.4, 60, "ground"), ST("LIGHT_FLARE", 0xFFE8A0, 1.6, 14, "self")],
               "ascend": [ST("DIVINE_PILLAR", 0xFFE8A0, 3.6, 90, "ground"), ST("DIVINE_HALO", 0x8A6AC8, 3.2, 80, "sky"), ST("STAR_MAGIC", 0xFFE8A0, 2.6, 70, "self")]}))


# ====================================================================== GATE OF BABYLON CREST (back)
WEAPONS = ["sword", "spear", "axe", "sword", "spear", "sword", "axe", "spear", "sword"]


def build_gate(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    BZ = 4.6                                             # the plane of the gates behind the back
    g_gate = vm.grp("great_gate", "bob", (0, -6, BZ), 0.25, 0.05, 0.0)
    g_ring = vm.grp("gate_ring", "spin", (0, -6, BZ), 0.5)
    g_cloth = vm.grp("golden_mantle", "wave", (0, -1, 2.4), 1.4, 0.13, 0.55)
    g_mini = [vm.grp("mini_gate_%d" % (i + 1), "pulse", (0, -6, BZ), 0.02, 0.08 + 0.01 * i, 0.7 * i) for i in range(3)]
    g_rip = [vm.grp("gate_ripple_%d" % (i + 1), "pulse", (0, -6, BZ + 0.2), 0.16, 0.09 + 0.012 * i, 0.9 * i) for i in range(3)]
    g_blades = vm.grp("emerging_weapons", "sway", (0, -6, BZ), 1.6, 0.05, 0.0)
    g_chain = vm.grp("light_chains", "sway", (0, -1, 2.4), 3.0, 0.06, 0.0)
    g_seal = vm.grp("treasury_seal", "spin", (0, -6, BZ + 0.6), 0.6)
    g_motes = vm.grp("golden_ripples", "pulse", (0, -6, BZ + 1.2), 0.2, 0.07, 0.0)
    # shoulder pauldrons, a gorget, and a gold mantle falling from them down the back
    paul = vm.P("pauldrons", "gold", group=g_gate, base=(246, 208, 96), lod=True)
    for sg in (-1, 1):
        geo.merge(paul, geo.prim_sphere(3.4, vm.S(5, 3), vm.S(8, 5), squash=0.62), None, (sg * 5.6, -0.4, 0.3))
    ridges = vm.P("pauldron_ridges", "gold", group=g_gate, base=(255, 232, 140), slot=TRIM, lod=False)
    for sg in (-1, 1):
        for k in range(2):
            geo.merge(ridges, geo.prim_torus(2.0 + 0.9 * k, 0.26, n=vm.S(12, 6), m=3), geo.rot_matrix("z", sg * 12), (sg * 5.6, 0.2 + 0.1 * k, 0.3))
        for k in range(3):
            a = math.radians(-50 + 50 * k)
            geo.merge(ridges, geo.prim_spike(0.55, 2.2, sides=4), geo.align_y((sg * math.cos(a), 0.4, math.sin(a))), (sg * 7.8, -0.9, 1.4 * (k - 1)))
    gorget = vm.P("gorget", "gold", group=g_gate, base=(240, 198, 90), slot=TRIM, lod=False)
    geo.merge(gorget, geo.prim_torus(4.4, 0.55, n=vm.S(16, 8), m=3), None, (0, 0.4, 0.2))
    gg = vm.P("gorget_gems", "crystal", group=g_gate, base=(255, 90, 80), slot=GLOW, lod=False)
    for i in range(5):
        a = math.radians(-70 + 140 * i / 4) + math.pi / 2
        geo.merge(gg, geo.prim_octa(0.55, 0.75), None, (4.6 * math.cos(a), 0.9, 4.6 * math.sin(a) - 0.2))
    mantle = vm.P("golden_mantle", "cloth", group=g_cloth, base=(240, 200, 84), lod=False)
    def surf(u, v):
        x = -5.4 + 10.8 * u
        y = -1.0 - 15.0 * v
        z = 2.5 + 0.9 * math.sin(u * math.pi * 3.0) * 0.25 + 0.5 * v
        return (x, y, z)
    GL_sheet = geo.GL.sheet
    GL_sheet(mantle, surf, vm.S(8, 3), vm.S(7, 3), thick=0.3)
    # the great gate: a ring of gold, a counter-ring with gems, ornaments round it
    ring = vm.P("great_ring", "gold", group=g_ring, base=(250, 212, 100), slot=TRIM, lod=True)
    geo.merge(ring, geo.prim_torus(8.0, 0.65, n=vm.S(26, 10), m=4), geo.rot_matrix("x", 90), (0, -6.0, BZ))
    geo.merge(ring, geo.prim_torus(6.4, 0.4, n=vm.S(22, 8), m=3), geo.rot_matrix("x", 90), (0, -6.0, BZ + 0.1))
    orn = vm.P("ring_ornaments", "gold", group=g_gate, base=(255, 230, 140), slot=TRIM, lod=False)
    og = vm.P("ring_gems", "crystal", group=g_gate, base=(255, 120, 90), slot=GLOW, lod=False)
    for i in range(10):
        a = 2 * math.pi * i / 10
        geo.merge(orn, geo.prim_spike(0.6, 2.6, sides=4), geo.align_y((math.cos(a), math.sin(a), 0)), (8.9 * math.cos(a), -6.0 + 8.9 * math.sin(a), BZ))
        if i % 2 == 0:
            geo.merge(og, geo.prim_octa(0.6, 0.9), geo.align_y((math.cos(a), math.sin(a), 0)), (7.2 * math.cos(a), -6.0 + 7.2 * math.sin(a), BZ))
    # nine lesser gates in a fan, a treasure half out of each
    fan = [(-7.4, -2.2), (-5.4, -6.2), (-3.0, -9.8), (0.0, -11.2), (3.0, -9.8), (5.4, -6.2), (7.4, -2.2), (-2.6, -2.4), (2.6, -2.4)]
    weapons = vm.P("emerging_weapons", "gold", group=g_blades, base=(255, 226, 130), slot=TRIM, lod=False)
    gates = vm.P("mini_gates", "gold", group=g_gate, base=(236, 192, 84), slot=TRIM, lod=False)
    for i, ((gx, gy), kind) in enumerate(zip(fan, WEAPONS)):
        d = np.array([gx * 0.12, (gy + 6.0) * 0.12 + 0.3, 1.0])
        d = d / np.linalg.norm(d)
        pos = np.array([gx, gy, BZ + 0.2])
        geo.merge(gates, geo.prim_torus(1.7, 0.3, n=vm.S(10, 6), m=3), geo.rot_matrix("x", 90), tuple(pos))
        if kind == "sword":
            geo.place(weapons, geo.prim_blade(11.0, 1.6, 0.4, tip=0.3), tuple(pos + d * 2.4), up=d)
            geo.place(weapons, geo.prim_box(3.0, 0.5, 0.5, caps=False), tuple(pos + d * 2.0), up=d)
        elif kind == "spear":
            geo.place(weapons, geo.prim_blade(5.0, 1.5, 0.35, tip=0.55), tuple(pos + d * 8.0), up=d)
            geo.tube_path(weapons, [tuple(pos + d * 0.5), tuple(pos + d * 8.2)], 0.28, sides=3, caps=(False, True))
        else:
            geo.tube_path(weapons, [tuple(pos + d * 0.5), tuple(pos + d * 9.5)], 0.28, sides=3, caps=(False, True))
            geo.place(weapons, geo.prim_blade(4.2, 3.2, 0.4, tip=0.5), tuple(pos + d * 8.0), up=geo.v3(d) + geo.v3((0.6, 0, 0)))
    # chains of light from the pauldrons to the gate, and the golden ripples of every gate
    ch = vm.P("light_chains", "gold", group=g_chain, base=(255, 228, 140), slot=TRIM, lod=False)
    for sg in (-1, 1):
        geo.chain(ch, [(sg * 6.4, -1.4, 1.0), (sg * 8.8, -4.2, 2.6), (sg * 9.4, -7.4, BZ - 0.6)], link_r=0.9, wire=0.22, n=6, m=3, every=3.2)
    for gi, g in enumerate(g_rip):
        vm.fx_disc(g, GLOW, (0, -6.0, BZ + 0.3 + 0.1 * gi), (0, 0, 1), 15.0 - 3.0 * gi, "ripple")
    for gi, g in enumerate(g_mini):
        for k in range(3):
            gx, gy = fan[gi * 3 + k]
            vm.fx_disc(g, ACCENT, (gx, gy, BZ + 0.35), (0, 0, 1), 4.6, "ripple")
    vm.fx_disc(g_seal, DARK, (0, -6.0, BZ + 0.6), (0, 0, 1), 24.0, "seal")
    vm.fx_disc(g_motes, GLOW, (0, -6.0, BZ + 1.2), (0, 0, 1), 36.0, "rays")
    K.halo_set(vm, pal, y=-6.0, R=11.0, rune=False, shaft_len=30.0)
    vm.deco(PT.deco_glyphs, ring, glow, text="BABYLON TREASURY", rows=1, glow=False, seed=3)
    vm.deco(PT.deco_filigree, paul, trim, seed=4, min_w=10, min_h=10)
    vm.deco(PT.deco_fill_glow, gg, glow, strength=0.9, seed=5)
    vm.deco(PT.deco_fill_glow, og, glow, strength=0.9, seed=6)
    vm.deco(PT.deco_veins, mantle, (255, 214, 100), n=3, strength=0.6, seed=7, min_w=10, min_h=10, dark=0.15)
    vm.deco(PT.deco_enamel, ridges, (255, 240, 170), (250, 200, 90), seed=8, min_w=4, min_h=4)
    vm.deco(PT.deco_grain, weapons, amount=0.12)
    vm.deco(PT.deco_grain, paul, amount=0.14)
    vm.deco(PT.deco_glyphs, weapons, glow, rows=1, glow=False, seed=9, min_w=4, min_h=3)


add(Item("gate_of_babylon_crest", build_gate, dict(style="swirl", dark=(12, 6, 0), mid=(160, 112, 20), hot=(255, 240, 170)), hand_size=0.95, hand_style="palm", seed=37, worn="pieces",
         pieces=[("", "body", False)], anchor="body",
         layers=[GL("great gate ring", "rune_ring", "gate_ring"), GL("great gate ripple", "portal_ripples", "gate_ripple_1"), GL("second ripple", "portal_ripples", "gate_ripple_2"), GL("third ripple", "portal_ripples", "gate_ripple_3"),
                 GL("lesser gates a", "portal_ripples", "mini_gate_1"), GL("lesser gates b", "portal_ripples", "mini_gate_2"), GL("lesser gates c", "portal_ripples", "mini_gate_3"),
                 GL("emerging weapons", "orbit_shards", "emerging_weapons"), GL("golden mantle", "ribbons", "golden_mantle"), GL("chains of light", "chains", "light_chains"),
                 GL("treasury seal", "rune_ring", "treasury_seal"), GL("golden rays", "lens_flare", "golden_ripples"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("gold motes", "motes", "end_rod", None, count=2, every=3, radius=0.6, rise=0.012),
                 PL("treasury dust", "drips", "dust", 0xFFF0A0, count=2, every=4, radius=0.7, rise=0.0),
                 PL("gate sparks", "sparks", "crit", None, count=1, every=6, radius=0.7, rise=0.0)],
         acts={"shower_of_blades": [ST("WEAPON_CONSTRUCTS", 0xFFF0A0, 3.0, 70, "self"), ST("SPACE_PORTAL", 0xE8C850, 3.0, 50, "sky"), ST("LIGHT_FLARE", 0xFFF0A0, 1.6, 14, "self")],
               "gate_fully_open": [ST("SPACE_PORTAL", 0xE8C850, 4.6, 90, "front"), ST("WEAPON_CONSTRUCTS", 0xFFF0A0, 4.0, 90, "self"), ST("DIVINE_PILLAR", 0xFFF0A0, 3.6, 80, "ground")]}))

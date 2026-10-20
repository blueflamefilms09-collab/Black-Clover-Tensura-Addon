"""Genesis grade: the Seal of the Sealed God, Sukuna's Twenty Fingers and the Infinity Gauntlet."""
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


# ====================================================================== SEAL OF THE SEALED GOD
def build_seal(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_seal = vm.grp("seal_stone", "bob", (0, 0, 0), 0.35, 0.05, 0.0)
    g_r1 = vm.grp("seal_ring_1", "spin", (0, 0, -1.5), 0.45)
    g_r2 = vm.grp("seal_ring_2", "spin", (0, 0, -1.5), -0.7)
    g_r3 = vm.grp("seal_ring_3", "spin", (0, 0, -1.5), 1.0)
    g_r4 = vm.grp("seal_ring_broken", "spin", (0, 0, -1.5), -1.5)
    g_eye = vm.grp("sealed_god", "pulse", (0, 0, 0), 0.09, 0.11, 0.0)
    g_chain = vm.grp("anchor_chains", "sway", (0, 0, 0), 2.5, 0.05, 0.0)
    g_shackles = vm.grp("shackle_rings", "tumble", (0, 0, 0), 0.7, 0.4, 1.1)
    g_smoke = vm.grp("tomb_smoke", "pulse", (0, 0, 3), 0.15, 0.06, 0.0)
    g_seal_fx = vm.grp("seal_sigil", "spin", (0, 0, 3.4), 0.6)
    g_seal_fx2 = vm.grp("seal_sigil_b", "spin", (0, 0, 3.8), -1.1)
    # an octagonal slab of pale stone with a deep step round a black-violet socket where the sealed thing looks out
    slab = vm.P("slab", "stone", group=g_seal, base=(222, 214, 232), lod=True)
    geo.lathe(slab, "y", (0, 0, 0), [(-1.6, 10.6), (-0.9, 11.4), (0.6, 11.2), (1.3, 9.6), (1.3, 6.4), (0.2, 6.0)], sides=8, caps=(True, False))
    slab.V = [geo.rot_matrix("x", -90) @ v for v in slab.V]
    bevel = vm.P("slab_studs", "gold", group=g_seal, base=(150, 100, 200), slot=TRIM, lod=False)
    for i in range(8):
        a = math.pi / 4 * i + math.pi / 8
        geo.merge(bevel, geo.prim_octa(0.9, 0.8), geo.rot_matrix("x", 90), (10.3 * math.cos(a), 10.3 * math.sin(a), -1.7))
    # four concentric seal rings, each a set of arcs; the outermost-but-one is broken
    def arcs(part, R, wire, gaps, z=-1.9):
        a0 = 0.0
        for (s, e) in gaps:
            geo.tube_path(part, geo.arc_pts((0, 0, z), R, s, e, vm.S(10, 4), plane="xy"), wire, sides=4)
    rings = []
    for gi, (g, R, w, gaps, base) in enumerate(((g_r1, 8.4, 0.5, ((0, 150), (170, 330)), (176, 150, 214)), (g_r2, 6.9, 0.45, ((10, 120), (140, 250), (270, 350)), (200, 180, 236)),
                                                  (g_r3, 5.4, 0.42, ((0, 110), (125, 235), (250, 355)), (176, 150, 214)), (g_r4, 3.9, 0.38, ((0, 70), (100, 200), (230, 300)), (255, 120, 220)))):
        p = vm.P("ring_%d" % (gi + 1), "silver", group=g, base=base, slot=TRIM if gi < 3 else GLOW, lod=(gi == 0))
        arcs(p, R, w, gaps)
        rings.append(p)
    broken = vm.P("broken_seal_shards", "crystal", group=g_r4, base=(255, 130, 230), slot=GLOW, lod=False)
    for i in range(5):
        a = math.radians(40 + 22 * i)
        geo.merge(broken, geo.prim_octa(0.55, 1.1), geo.align_y((math.cos(a), math.sin(a), 0.2)), (4.5 * math.cos(a), 4.5 * math.sin(a), -2.6 - 0.3 * i))
    # the sealed god: a void socket with a huge slit eye, lit from behind
    void = vm.star("void", g_eye)
    geo.merge(void, geo.prim_lathe([(-0.2, 3.6), (0.2, 3.6)], sides=vm.S(12, 6), caps=(True, True)), geo.rot_matrix("x", -90), (0, 0, -0.9))
    iris = vm.star("iris", g_eye, lod=False)
    geo.merge(iris, geo.prim_octa(0.9, 3.0), geo.rot_matrix("x", 90), (0, 0, -1.2))
    lids = vm.P("socket_lids", "obsidian", group=g_seal, base=(34, 18, 56), lod=False)
    geo.tube_path(lids, geo.arc_pts((0, -1.2, -1.4), 3.8, 25, 155, vm.S(8, 4), plane="xy"), 0.55, sides=4)
    geo.tube_path(lids, geo.arc_pts((0, 1.2, -1.4), 3.8, 205, 335, vm.S(8, 4), plane="xy"), 0.55, sides=4)
    # six chains from the slab to anchor spikes, and spikes
    spikes = vm.P("anchor_spikes", "iron", group=g_chain, base=(70, 54, 100), lod=False)
    chains = vm.P("anchor_chains", "iron", group=g_chain, base=(96, 76, 130), lod=False)
    for i in range(6):
        a = math.radians(30 + 60 * i)
        c, s_ = math.cos(a), math.sin(a)
        geo.merge(spikes, geo.prim_spike(1.2, 6.0, sides=4), geo.align_y((c, s_, 0.1)), (16.5 * c, 16.5 * s_, 0.0))
        geo.chain(chains, [(11.0 * c, 11.0 * s_, 0.0), (13.4 * c, 13.4 * s_, 0.6), (15.8 * c, 15.8 * s_, 0.0)], link_r=1.0, wire=0.26, n=6, m=3, every=2.4)
    sh = vm.P("shackle_rings", "iron", group=g_shackles, base=(120, 90, 160), lod=False)
    for i in range(4):
        a = 2 * math.pi * i / 4
        geo.merge(sh, geo.prim_torus(1.4, 0.32, n=vm.S(10, 6), m=3), geo.rot_matrix("x", 30 * i) @ geo.rot_matrix("z", 40 * i), (13.0 * math.cos(a), 13.0 * math.sin(a), -2.0))
    vm.fx_cross(g_smoke, DARK, (0, 0, 2.0), (0, 0, 1), 26.0, "smoke", n=3)
    vm.fx_disc(g_seal_fx, GLOW, (0, 0, 3.4), (0, 0, 1), 36.0, "seal")
    vm.fx_disc(g_seal_fx2, DARK, (0, 0, 3.8), (0, 0, 1), 30.0, "rune")
    K.halo_set(vm, pal, y=0.0, R=11.0, rune=False, shaft_len=30.0)
    vm.deco(PT.deco_glyphs, slab, glow, text="SEALED", rows=2, glow=True, strength=0.85, seed=3)
    for r_ in rings:
        vm.deco(PT.deco_glyphs, r_, glow, rows=1, glow=True, strength=0.9, seed=4, min_w=6, min_h=4)
    vm.deco(PT.deco_fill_glow, broken, (255, 120, 240), strength=0.95, seed=5)
    vm.deco(PT.deco_veins, lids, glow, n=2, strength=0.7, seed=6, min_w=6, min_h=6)
    vm.deco(PT.deco_grain, slab, amount=0.2)
    vm.deco(PT.deco_filigree, bevel, trim, seed=7)


add(Item("seal_of_the_sealed_god", build_seal, dict(style="void", dark=(6, 0, 14), mid=(110, 36, 170), hot=(236, 170, 255)), hand_size=0.85, hand_style="palm", seed=32,
         layers=[GL("sealed god void", "nebula_core", "sealed_god"), GL("outer seal ring", "rune_ring", "seal_ring_1"), GL("second seal ring", "rune_ring", "seal_ring_2"), GL("third seal ring", "rune_ring", "seal_ring_3"),
                 GL("broken seal ring", "rune_ring", "seal_ring_broken"), GL("anchor chains", "chains", "anchor_chains"), GL("shackle rings", "orbit_shards", "shackle_rings"),
                 GL("tomb smoke", "distortion", "tomb_smoke"), GL("seal sigil", "rune_ring", "seal_sigil"), GL("counter sigil", "rune_ring", "seal_sigil_b"),
                 GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("tomb ash", "wisps", "dust", 0x6A3A9A, count=2, every=3, radius=0.55, rise=0.012),
                 PL("seal sparks", "sparks", "dust", 0xD8A8FF, count=1, every=5, radius=0.6, rise=0.0),
                 PL("god glints", "glints", "end_rod", None, count=1, every=8, radius=0.5, rise=0.01)],
         acts={"break_one_seal": [ST("SEAL_TRINITY", 0xD8A8FF, 3.0, 70, "ground"), ST("SPACE_CRACK", 0xD8A8FF, 2.2, 36, "front"), ST("LIGHT_FLARE", 0xFFFFFF, 1.6, 14, "front")],
               "the_tomb_opens": [ST("SEAL_CHAINS", 0x6A3A9A, 4.0, 90, "self"), ST("SPACE_TEAR", 0xD8A8FF, 3.4, 70, "front"), ST("SHADOW_POOL", 0x2A1040, 3.4, 80, "ground")]},
         worn_scale=0.4, worn_offset=(11.4, -9.0, 0.0)))


# ====================================================================== SUKUNA'S TWENTY FINGERS
def build_fingers(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_hand = vm.grp("finger_bouquet", "sway", (0, -6, 0), 1.5, 0.05, 0.0)
    g_inner = vm.grp("inner_fingers", "pulse", (0, -4, 0), 0.025, 0.2, 0.0)
    g_malice = vm.grp("malevolence", "pulse", (0, 2, 0), 0.08, 0.17, 0.0)
    g_blades = vm.grp("cleave_blades", "spin", (0, 2, 0), 3.0)
    g_blades2 = vm.grp("cleave_blades_far", "spin", (0, 4, 0), -2.0)
    g_eyes = vm.grp("four_eyes", "pulse", (0, -8, -3), 0.2, 0.13, 0.0)
    g_cloth = vm.grp("curse_cloth", "sway", (0, -10, 0), 5.0, 0.07, 0.0)
    g_smoke = vm.grp("curse_smoke", "pulse", (0, 4, 0), 0.12, 0.08, 0.0)
    g_shrine = vm.grp("shrine_seal", "spin", (0, -12, 0), 0.7)
    # a bone-and-leather cuff holds the bouquet: a mouth of teeth round it, four eyes on its front
    cuff = vm.P("cuff", "bone", group=g_hand, base=(206, 184, 150), lod=True)
    geo.lathe(cuff, "y", (0, 0, 0), [(-14.0, 3.4), (-12.5, 4.8), (-10.0, 5.2), (-8.0, 4.6), (-6.4, 3.4)], sides=vm.S(14, 6), caps=(True, False))
    teeth = vm.P("mouth_teeth", "bone", group=g_hand, base=(246, 238, 220), slot=TRIM, lod=False)
    tp = geo.prim_spike(0.55, 1.5, sides=3)
    for i in range(vm.S(14, 6)):
        a = 2 * math.pi * i / vm.S(14, 6)
        geo.merge(teeth, tp, geo.align_y((math.cos(a) * 0.3, -1, math.sin(a) * 0.3)), (5.0 * math.cos(a), -6.6, 5.0 * math.sin(a)))
    eyes = vm.P("four_eyes", "crystal", group=g_eyes, base=(255, 60, 40), slot=GLOW, lod=False)
    for (ex, ey) in ((-2.6, -8.6), (2.6, -8.6), (-1.6, -10.8), (1.6, -10.8)):
        geo.merge(eyes, geo.prim_octa(0.9, 0.5), geo.rot_matrix("x", 90), (ex, ey, -5.3))
    # twenty fingers: ten tall ones fanned out, ten short ones standing in front of them
    fin = geo.prim_finger(15.0, 0.9, segs=3, curl=0.5, sides=3)
    fin_s = geo.prim_finger(10.0, 0.8, segs=2, curl=0.4, sides=3)
    out = vm.P("fingers_outer", "leather", group=g_hand, base=(120, 84, 70), lod=False)
    inn = vm.P("fingers_inner", "leather", group=g_inner, base=(142, 100, 82), lod=False)
    for i in range(10):
        a = 2 * math.pi * i / 10
        geo.merge(out, fin, geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("z", -30.0 + 5 * (i % 2)), (3.4 * math.cos(a), -6.0, 3.4 * math.sin(a)))
    for i in range(10):
        a = 2 * math.pi * i / 10 + math.pi / 10
        geo.merge(inn, fin_s, geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("z", -10.0), (2.0 * math.cos(a), -6.0, 2.0 * math.sin(a)))
    nails = vm.P("cursed_nails", "obsidian", group=g_hand, base=(36, 14, 14), lod=False)
    # the malevolence between them: a flame of red nebula
    mal = vm.star("malevolence", g_malice)
    geo.merge(mal, geo.prim_crystal(3.0, 14.0, sides=6, tip=0.55), None, (0, 2.0, 0))
    geo.merge(mal, geo.prim_octa(2.0, 8.0), geo.rot_matrix("y", 30), (0, 1.0, 0))
    # cleave: blades of cursed energy in two orbits
    bl = vm.P("cleave_blades", "crystal", group=g_blades, base=(255, 60, 50), slot=GLOW, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(bl, geo.prim_blade(7.0, 1.3, 0.25, tip=0.4), geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("z", 90 + 8 * i), (10.0 * math.cos(a), 3.0 * math.sin(i * 1.9), 10.0 * math.sin(a)))
    bl2 = vm.P("cleave_blades_far", "obsidian", group=g_blades2, base=(60, 16, 18), slot=ACCENT, lod=False)
    for i in range(5):
        a = 2 * math.pi * i / 5 + 0.6
        geo.merge(bl2, geo.prim_blade(5.0, 1.0, 0.2, tip=0.4), geo.rot_matrix("y", -math.degrees(a) + 90) @ geo.rot_matrix("z", 100 - 10 * i), (14.0 * math.cos(a), 5.0 - 2.6 * math.sin(i * 1.4), 14.0 * math.sin(a)))
    cloth = vm.P("curse_cloth", "cloth", group=g_cloth, base=(130, 20, 24), lod=False)
    for k in range(3):
        a = 2 * math.pi * k / 3 + 0.4
        pts = [(5.0 * math.cos(a), -9.6, 5.0 * math.sin(a)), (6.0 * math.cos(a), -12.0, 6.0 * math.sin(a)), (5.4 * math.cos(a), -15.0, 5.4 * math.sin(a)), (6.4 * math.cos(a), -18.0, 6.4 * math.sin(a))]
        geo.ribbon(cloth, pts, 2.0, thick=0.15, up=(math.sin(a), 0, -math.cos(a)), taper=0.4)
    vm.fx_cross(g_smoke, DARK, (0, 4, 0), (0, 1, 0), 22.0, "smoke", n=3)
    vm.fx_disc(g_shrine, GLOW, (0, -14.0, 0), (0, 1, 0), 34.0, "seal")
    vm.fx_disc(g_shrine, DARK, (0, -13.8, 0), (0, 1, 0), 26.0, "web")
    K.halo_set(vm, pal, y=2.0, R=11.0, rune=False, shaft_len=30.0)
    vm.deco(PT.deco_fill_glow, eyes, glow, strength=1.0, seed=3, facets=False)
    vm.deco(PT.deco_fill_glow, bl, glow, strength=0.9, seed=4)
    vm.deco(PT.deco_veins, out, (255, 60, 50), n=3, strength=0.7, seed=5, min_w=6, min_h=8)
    vm.deco(PT.deco_veins, inn, (255, 60, 50), n=3, strength=0.7, seed=6, min_w=6, min_h=8)
    vm.deco(PT.deco_grain, out, amount=0.2)
    vm.deco(PT.deco_grain, inn, amount=0.2)
    vm.deco(PT.deco_glyphs, cuff, glow, text="KING OF CURSES", rows=2, glow=True, strength=0.8, seed=7)
    vm.deco(PT.deco_tint, cloth, (180, 30, 30), amount=0.2)


add(Item("sukunas_twenty_fingers", build_fingers, dict(style="lava", dark=(20, 0, 0), mid=(150, 14, 18), hot=(255, 130, 100)), hand_size=1.0, hand_style="palm", seed=33,
         layers=[GL("malevolence", "nebula_core", "malevolence"), GL("finger bouquet", "aura_shell", "finger_bouquet"), GL("inner fingers", "aura_shell", "inner_fingers"), GL("cleave blades", "orbit_shards", "cleave_blades"),
                 GL("far blades", "orbit_shards", "cleave_blades_far"), GL("four eyes", "aura_shell", "four_eyes"), GL("curse cloth", "ribbons", "curse_cloth"), GL("curse smoke", "distortion", "curse_smoke"),
                 GL("shrine seal", "rune_ring", "shrine_seal"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("cursed embers", "embers", "dust", 0xFF3A3A, count=2, every=3, radius=0.6, rise=0.022),
                 PL("malice smoke", "wisps", "smoke", None, count=1, every=4, radius=0.5, rise=0.02),
                 PL("slash sparks", "sparks", "crit", None, count=1, every=7, radius=0.7, rise=0.0)],
         acts={"cleave": [ST("SLASH_SCYTHE", 0xFF3A3A, 3.0, 40, "front"), ST("LIGHT_BLADE", 0xFF6A5A, 2.4, 24, "front")],
               "malevolent_shrine": [ST("SPACE_CUBE", 0xFF3A3A, 4.4, 90, "self"), ST("SLASH_SCYTHE", 0xFF3A3A, 4.0, 60, "self"), ST("BLOOD_SIGIL", 0xFF3A3A, 3.6, 80, "ground")]},
         worn_scale=0.36, worn_offset=(11.6, -9.0, 0.0)))


# ====================================================================== THE INFINITY GAUNTLET (authored for the right arm; worn on the left through the mirror)
STONES = [("space", (58, 139, 255)), ("mind", (255, 217, 58)), ("reality", (224, 58, 58)), ("power", (160, 74, 240)), ("soul", (255, 138, 42)), ("time", (58, 224, 122))]


def build_gauntlet(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    AX = -1.0                                                          # the arm's axis (x), the arm is x -3..1
    g_g = vm.grp("gauntlet", "swing", (0, 0, 0), 0.0, 0.0, 0.0)
    g_ring = vm.grp("wrist_ring", "spin", (AX, -7.0, 0), 1.6)
    g_ring2 = vm.grp("wrist_ring_b", "spin", (AX, -5.0, 0), -2.2)
    g_stones = vm.grp("stone_glow", "pulse", (AX, -12.0, 0), 0.10, 0.15, 0.0)
    g_orbit = vm.grp("stone_motes", "spin", (AX, -13.0, 0), 2.6)
    g_arcs = vm.grp("snap_arcs", "pulse", (AX, -13.0, 0), 0.3, 0.5, 1.0)
    g_palm = vm.grp("palm_vortex", "tumble", (AX + 2.4, -13.0, 0), 0.0, 0.0, 1.8)
    g_fingers = vm.grp("finger_flex", "sway", (AX, -12.0, 0), 1.2, 0.04, 0.0)
    # forearm: a black vambrace flaring to a gold cuff, bands and plates
    arm = vm.P("vambrace", "iron", group=g_g, base=(66, 62, 84), lod=True)
    geo.lathe(arm, "y", (AX, 0, 0), [(-2.6, 3.0), (-3.4, 3.4), (-6.0, 3.1), (-8.0, 2.8), (-9.4, 3.3), (-10.2, 3.8)], sides=vm.S(10, 6), caps=(False, False))
    bands = vm.P("vambrace_bands", "gold", group=g_g, base=(236, 190, 84), slot=TRIM, lod=False)
    for yy, rr in ((-2.7, 3.05), (-5.2, 3.3), (-7.6, 3.0), (-10.0, 3.7)):
        geo.lathe(bands, "y", (AX, 0, 0), [(yy - 0.32, rr), (yy + 0.32, rr)], sides=vm.S(10, 6), caps=(False, False))
    plates = vm.P("vambrace_plates", "gold", group=g_g, base=(226, 180, 80), slot=TRIM, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6 + 0.3
        geo.merge(plates, geo.prim_box(1.4, 2.6, 0.45, caps=False), geo.rot_matrix("y", -math.degrees(a) + 90), (AX + 3.35 * math.cos(a), -6.4, 3.35 * math.sin(a)))
    # the hand: back plate, palm plate with the ribcage, fingers, thumb
    back = vm.P("back_plate", "iron", group=g_g, base=(60, 58, 76), lod=True)
    geo.merge(back, geo.prim_box(1.7, 4.4, 5.6, chamfer=0.35), None, (AX - 1.9, -12.6, 0.0))
    palm = vm.P("palm_plate", "iron", group=g_g, base=(72, 68, 88), lod=False)
    geo.merge(palm, geo.prim_box(1.4, 4.4, 5.2, chamfer=0.3), None, (AX + 0.6, -12.6, 0.0))
    ribs = vm.P("ribcage_palm", "bone", group=g_g, base=(220, 212, 190), lod=False)
    for k in range(5):
        yy = -10.8 - 0.75 * k
        geo.tube_path(ribs, geo.arc_pts((AX + 1.3, yy, 0.0), 2.3 - 0.2 * abs(k - 2), -80, 80, vm.S(6, 3), plane="xz"), 0.2, sides=3, caps=(True, True))
    ring_a = vm.P("wrist_ring", "gold", group=g_ring, base=(250, 206, 100), slot=TRIM, lod=False)
    geo.merge(ring_a, geo.prim_torus(4.0, 0.3, n=vm.S(16, 8), m=3), None, (AX, -7.0, 0))
    ring_b = vm.P("wrist_ring_b", "gold", group=g_ring2, base=(255, 224, 130), slot=TRIM, lod=False)
    geo.merge(ring_b, geo.prim_torus(4.5, 0.22, n=vm.S(16, 8), m=3), geo.rot_matrix("x", 18), (AX, -5.0, 0))
    fing = vm.P("fingers", "iron", group=g_fingers, base=(60, 58, 76), lod=False)
    tips = vm.P("finger_tips", "gold", group=g_fingers, base=(236, 190, 84), slot=TRIM, lod=False)
    seg = geo.prim_box(1.15, 1.5, 1.15, caps=True)
    for k, zz in enumerate((-2.0, -0.67, 0.67, 2.0)):
        for s in range(3):
            geo.merge(fing, seg, geo.rot_matrix("z", -7 * s), (AX - 0.9 + 0.35 * s, -15.6 - 1.45 * s, zz))
        geo.merge(tips, geo.prim_spike(0.5, 1.3, sides=4), geo.rot_matrix("x", 180) @ geo.rot_matrix("z", 8), (AX - 0.5, -20.3, zz))
    thumb = vm.P("thumb", "iron", group=g_fingers, base=(60, 58, 76), lod=False)
    for s in range(2):
        geo.merge(thumb, seg, geo.rot_matrix("x", -38) @ geo.rot_matrix("z", 8), (AX - 0.7, -13.4 - 1.35 * s, -3.4 - 1.1 * s))
    geo.merge(tips, geo.prim_spike(0.5, 1.3, sides=4), geo.rot_matrix("x", 142), (AX - 0.7, -16.6, -5.6))
    # the six stones: a socket ring and a cut gem each: the back of the hand, the four knuckles, the thumb
    spots = [(AX - 3.0, -12.2, 0.0), (AX - 3.0, -14.9, -2.0), (AX - 3.0, -14.9, -0.67), (AX - 3.0, -14.9, 0.67), (AX - 3.0, -14.9, 2.0), (AX - 2.3, -13.2, -4.6)]
    order = [1, 3, 0, 2, 4, 5]                     # mind on the back of the hand, then the knuckles, time on the thumb
    gem_parts = []
    for (name, rgb_), (px, py, pz) in zip([STONES[o] for o in order], spots):
        big = name == "mind"
        sock = vm.P("socket_" + name, "gold", group=g_g, base=(250, 206, 104), slot=TRIM, lod=False)
        geo.merge(sock, geo.prim_torus(1.25 if big else 0.8, 0.26, n=vm.S(10, 6), m=3), geo.rot_matrix("z", 90), (px, py, pz))
        gm = vm.P("stone_" + name, "crystal", group=g_stones, base=rgb_, slot=GLOW, lod=big)
        geo.merge(gm, geo.prim_octa(1.15 if big else 0.82, 1.35 if big else 0.95), geo.rot_matrix("z", 90), (px - 0.25, py, pz))
        gem_parts.append((gm, rgb_))
    rivets = vm.P("vambrace_rivets", "gold", group=g_g, base=(255, 224, 130), slot=TRIM, lod=False)
    for yy in (-3.1, -6.5, -9.0):
        for i in range(8):
            a_ = 2 * math.pi * i / 8 + 0.2
            rr = 3.25 if yy > -4 else 3.2 if yy > -7 else 3.1
            geo.merge(rivets, geo.prim_octa(0.28, 0.33), geo.align_y((math.cos(a_), 0, math.sin(a_))), (AX + rr * math.cos(a_), yy, rr * math.sin(a_)))
    wires = vm.P("stone_wires", "gold", group=g_g, base=(250, 206, 100), slot=TRIM, lod=False)
    for (p0, p1) in ((spots[0], spots[1]), (spots[0], spots[2]), (spots[0], spots[3]), (spots[0], spots[4]), (spots[1], spots[2]), (spots[3], spots[4]), (spots[5], spots[1])):
        geo.tube_path(wires, [(p0[0] - 0.4, p0[1], p0[2]), ((p0[0] + p1[0]) / 2 - 0.9, (p0[1] + p1[1]) / 2, (p0[2] + p1[2]) / 2), (p1[0] - 0.4, p1[1], p1[2])], 0.14, sides=3, caps=(False, False))
    spikes = vm.P("knuckle_spikes", "gold", group=g_fingers, base=(236, 190, 84), slot=TRIM, lod=False)
    for zz in (-2.0, -0.67, 0.67, 2.0):
        geo.merge(spikes, geo.prim_spike(0.45, 1.5, sides=4), geo.rot_matrix("z", 100), (AX - 1.5, -15.3, zz))
    fins = vm.P("cuff_fins", "gold", group=g_g, base=(226, 180, 80), slot=TRIM, lod=False)
    for i in range(5):
        a_ = math.radians(-60 + 30 * i) + math.pi
        geo.merge(fins, geo.prim_blade(2.6, 1.0, 0.3, tip=0.5), geo.align_y((math.cos(a_) * 0.7, -0.5, math.sin(a_) * 0.7)), (AX + 3.6 * math.cos(a_), -10.2, 3.6 * math.sin(a_)))
    vortex = vm.star("palm_vortex", g_palm)
    geo.merge(vortex, geo.prim_octa(1.8, 2.8), None, (AX + 1.9, -13.0, 0.0))
    orbs = vm.P("stone_motes", "crystal", group=g_orbit, base=(255, 240, 200), slot=GLOW, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(orbs, geo.prim_octa(0.45, 0.6), None, (AX + 6.0 * math.cos(a), -13.0 + 2.2 * math.sin(i * 2.3), 6.0 * math.sin(a)))
    for i in range(5):
        a = math.radians(36 + 72 * i)
        vm.fx_disc(g_arcs, ACCENT, (AX, -13.0, 0), (math.cos(a), 0.3, math.sin(a)), 12.0, "bolt", rot=24 * i)
    vm.fx_disc(g_ring, GLOW, (AX, -7.0, 0), (0, 1, 0), 13.0, "rune")
    vm.fx_disc(g_ring2, DARK, (AX, -5.0, 0), (0, 1, 0), 15.0, "ticks")
    g_fl = vm.grp("lens_flare", "pulse", (AX, -13.0, 0), 0.16, 0.10, 0.0)
    vm.fx_cross(g_fl, ACCENT, (AX, -13.0, 0), (0, 1, 0), 12.0, "flare", n=3, lod=True)
    g_sh = vm.grp("light_shafts", "pulse", (AX, -10.0, 0), 0.10, 0.07, 1.3)
    vm.fx_beam(g_sh, ACCENT, (AX, -22.0, 0), (AX, 2.0, 0), 4.5, "beam", lod=True)
    for gm, rgb_ in gem_parts:
        vm.deco(PT.deco_fill_glow, gm, rgb_, strength=1.0, seed=3, core=0.5)
    vm.deco(PT.deco_glyphs, arm, glow, text="INFINITY", rows=2, glow=False, seed=4)
    vm.deco(PT.deco_glyphs, ring_a, glow, rows=1, glow=True, strength=0.9, seed=5)
    vm.deco(PT.deco_filigree, plates, trim, seed=6)
    vm.deco(PT.deco_filigree, back, trim, seed=7, min_w=8, min_h=8)
    vm.deco(PT.deco_rim_glow, bands, glow, strength=0.5)
    vm.deco(PT.deco_grain, arm, amount=0.1)
    vm.deco(PT.deco_grain, ribs, amount=0.12)


add(Item("the_infinity_gauntlet", build_gauntlet, dict(style="swirl", dark=(10, 8, 0), mid=(130, 96, 20), hot=(255, 235, 170)), hand_size=1.0, hand_style="tool", seed=34, worn="pieces",
         pieces=[("", "larm", True)], anchor="rarm",
         layers=[GL("stone glow", "aura_shell", "stone_glow"), GL("stone motes", "orbit_shards", "stone_motes"), GL("snap arcs", "energy_arcs", "snap_arcs"), GL("palm vortex", "nebula_core", "palm_vortex"),
                 GL("wrist ring", "rune_ring", "wrist_ring"), GL("wrist ring b", "rune_ring", "wrist_ring_b"), GL("finger flex", "aura_shell", "finger_flex"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("stone embers", "embers", "dust", 0xFFD27A, count=2, every=3, radius=0.4, rise=0.015, views=["hand"]),
                 PL("stone sparks", "sparks", "electric_spark", None, count=1, every=6, radius=0.5, rise=0.0),
                 PL("infinity glints", "glints", "end_rod", None, count=1, every=5, radius=0.45, rise=0.01)],
         acts={"greater_wish": [ST("DIVINE_PILLAR", 0xFFD27A, 4.0, 90, "ground"), ST("DIVINE_HALO", 0xFFD27A, 3.6, 80, "sky"), ST("LIGHT_FLARE", 0xFFFFFF, 2.4, 18, "self")],
               "choose_wish": [ST("MAGIC_CIRCLE", 0xFFD27A, 2.4, 40, "ground")],
               "stone_space": [ST("SPACE_PORTAL", 0x3A8BFF, 2.4, 50, "front")],
               "stone_mind": [ST("DIVINE_HALO", 0xFFD93A, 2.4, 50, "self")],
               "stone_power": [ST("MAGIC_CIRCLE_EXPLOSION", 0xA04AF0, 3.6, 40, "ground"), ST("LIGHTNING_FIEND", 0xD0A0FF, 2.6, 36, "self")],
               "stone_time": [ST("DIVINE_HALO", 0x3AE07A, 3.0, 70, "self"), ST("MAGIC_CIRCLE", 0x3AE07A, 3.0, 50, "ground")],
               "stone_reality": [ST("DREAM_MANIFEST", 0xE03A3A, 2.2, 40, "front")],
               "stone_soul": [ST("SOUL_VORTEX", 0xFF8A2A, 2.4, 60, "self")],
               "snap": [ST("MAGIC_CIRCLE_EXPLOSION", 0xFFD27A, 5.0, 60, "ground"), ST("DIVINE_PILLAR", 0xFFFFFF, 4.6, 90, "ground"), ST("LIGHT_FLARE", 0xFFFFFF, 3.0, 20, "self")]}))

"""God grade charms, the last two: Serie's Ancient Grimoire (an elf's open book on a column of mana) and the Vaultmaker's Pocketwatch (a brass skeleton watch with a vault door's bolts)."""
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


# ====================================================================== SERIE'S ANCIENT GRIMOIRE
def build_grimoire(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_book = vm.grp("grimoire", "bob", (0, 0, 0), 0.35, 0.06, 0.0)
    g_flutter_l = vm.grp("page_flutter_l", "sway", (0, 0, 0), 5.0, 0.09, 0.0)
    g_flutter_r = vm.grp("page_flutter_r", "sway", (0, 0, 0), -5.0, 0.09, 1.5)
    g_col = vm.grp("mana_column", "pulse", (0, 9, 0), 0.06, 0.13, 0.0)
    g_runes = vm.grp("floating_runes", "tumble", (0, 7, 0), 0.8, 0.3, 1.9)
    g_seal_h = vm.grp("spell_circle", "spin", (0, 15, 0), 1.2)
    g_seal_v = vm.grp("spell_circle_b", "spin", (0, 8, 3), -0.8)
    g_vines = vm.grp("silver_vines", "sway", (0, 0, 0), 2.0, 0.04, 0.0)
    # the open book: two ivory boards in a V, page stacks, a spine with a gem, silver vines on the cover edges
    boards = vm.P("covers", "bone", group=g_book, base=(232, 224, 238), lod=True)
    for sg in (-1, 1):
        geo.merge(boards, geo.prim_box(7.4, 11.4, 0.8, caps=True), geo.rot_matrix("z", sg * 24), (sg * 4.0, 0.0, 0.0))
    pages = vm.P("page_stacks", "bone", group=g_book, base=(250, 246, 238), lod=False)
    for sg in (-1, 1):
        geo.merge(pages, geo.prim_box(6.6, 10.6, 1.0, caps=True), geo.rot_matrix("z", sg * 20), (sg * 3.7, 0.9, 0.0))
    spine = vm.P("spine", "gold", group=g_book, base=(214, 196, 240), slot=TRIM, lod=False)
    geo.lathe(spine, "y", (0, 0, 0), [(-5.9, 0.7), (-5.2, 1.3), (5.2, 1.3), (5.9, 0.7)], sides=vm.S(6, 4), caps=(True, True))
    gem = vm.P("spine_gem", "crystal", group=g_book, base=(190, 130, 255), slot=GLOW, lod=False)
    geo.merge(gem, geo.prim_octa(0.9, 1.4), geo.rot_matrix("x", 90), (0, 0, -1.4))
    vines = vm.P("silver_vines", "silver", group=g_vines, base=(214, 218, 236), slot=TRIM, lod=False)
    for sg in (-1, 1):
        pts = [(sg * 7.0, -5.0, -0.9), (sg * 7.6, -2.0, -1.2), (sg * 6.6, 1.0, -1.1), (sg * 7.4, 4.0, -1.3), (sg * 6.2, 6.0, -1.0)]
        geo.tube_path(vines, geo.bez3(pts[0], pts[1], pts[3], pts[4], vm.S(6, 3)), 0.22, sides=3)
        for k in range(3):
            geo.merge(vines, geo.prim_leaf(1.8, 0.9, 0.12, sides=2), geo.align_y((sg * 1.0, 0.4, -0.6)), (sg * 7.0, -3.0 + 3.0 * k, -1.1))
    # pages fluttering up out of the book
    fl = vm.P("fluttering_pages_l", "bone", group=g_flutter_l, base=(246, 242, 252), lod=False)
    fr = vm.P("fluttering_pages_r", "bone", group=g_flutter_r, base=(240, 236, 250), lod=False)
    pc = geo.prim_card(4.4, 6.0, 0.08)
    for part, sg in ((fl, -1), (fr, 1)):
        for k in range(3):
            geo.merge(part, pc, geo.rot_matrix("z", sg * (40 + 12 * k)) @ geo.rot_matrix("y", 12 * k), (sg * (4.8 + 0.8 * k), 6.0 + 2.2 * k, 0.4 * k))
    # a column of mana rising from the open pages, floating rune cubes, spell circles
    col = vm.star("column", g_col)
    geo.merge(col, geo.prim_lathe([(-6.0, 0.4), (-2.0, 2.2), (4.0, 1.6), (12.0, 0.3)], sides=vm.S(8, 4), caps=(False, False)), None, (0, 8.0, 0))
    runes = vm.P("floating_runes", "crystal", group=g_runes, base=(216, 184, 255), slot=GLOW, lod=False)
    for i in range(6):
        a = 2 * math.pi * i / 6
        geo.merge(runes, geo.prim_box(1.3, 1.3, 1.3, caps=True), geo.rot_matrix("y", 30 * i) @ geo.rot_matrix("x", 20 * i), (8.0 * math.cos(a), 7.0 + 2.5 * math.sin(i * 1.7), 8.0 * math.sin(a)))
    vm.fx_disc(g_seal_h, GLOW, (0, 15.0, 0), (0, 1, 0), 22.0, "seal")
    vm.fx_disc(g_seal_v, DARK, (0, 8.0, 3.0), (0, 0, 1), 20.0, "rune")
    K.halo_set(vm, pal, y=6.0, R=9.0, rune=False, shaft_len=26.0)
    vm.deco(PT.deco_glyphs, pages, glow, text="ANCIENT SPELL", rows=4, glow=True, strength=0.7, seed=3, density=0.9)
    vm.deco(PT.deco_filigree, boards, trim, seed=4, min_w=14, min_h=14)
    vm.deco(PT.deco_fill_glow, gem, glow, strength=0.95, seed=5)
    vm.deco(PT.deco_fill_glow, runes, glow, strength=0.8, seed=6)
    vm.deco(PT.deco_glyphs, spine, glow, rows=1, glow=False, seed=7, vertical=True)


add(Item("series_ancient_grimoire", build_grimoire, dict(style="swirl", dark=(10, 2, 20), mid=(120, 70, 190), hot=(236, 214, 255)), hand_size=0.78, hand_style="palm", seed=27,
         layers=[GL("mana column", "nebula_core", "mana_column"), GL("fluttering pages left", "ribbons", "page_flutter_l"), GL("fluttering pages right", "ribbons", "page_flutter_r"),
                 GL("floating runes", "orbit_shards", "floating_runes"), GL("spell circle", "rune_ring", "spell_circle"), GL("spell circle b", "rune_ring", "spell_circle_b"),
                 GL("silver vines", "ribbons", "silver_vines"), GL("lens flare", "lens_flare", "lens_flare"), GL("light shafts", "light_shafts", "light_shafts"),
                 PL("mana motes", "motes", "end_rod", None, count=1, every=4, radius=0.5, rise=0.016),
                 PL("elven glints", "glints", "dust", 0xD8B8FF, count=1, every=6, radius=0.55, rise=0.008)],
         acts={"ancient_spell": [ST("MAGIC_CIRCLE", 0xD8B8FF, 3.0, 50, "ground"), ST("ELF_CIRCLE", 0x6A4A9A, 2.4, 44, "front"), ST("LIGHT_FLARE", 0xE8E0F0, 1.2, 14, "front")],
               "thousand_year_magic": [ST("MAGIC_CIRCLE_EXPLOSION", 0xD8B8FF, 4.0, 60, "ground"), ST("DIVINE_HALO", 0xE8E0F0, 3.0, 60, "sky"), ST("STAR_MAGIC", 0xD8B8FF, 2.4, 60, "self")]}))


# ====================================================================== VAULTMAKER'S POCKETWATCH
def build_vaultwatch(vm, info):
    pal = info[4]
    glow, trim = K.rgb(pal[2]), K.rgb(pal[1])
    g_w = vm.grp("watch", "bob", (0, 0, 0), 0.3, 0.06, 0.0)
    g_bolts = vm.grp("vault_bolts", "pulse", (0, 0, 0), 0.05, 0.16, 0.0)
    g_g1 = vm.grp("gear_great", "spin", (0, 0, -1.0), 1.0)
    g_g2 = vm.grp("gear_mid", "spin", (-4.5, 3.5, -1.2), -1.9)
    g_g3 = vm.grp("gear_small", "spin", (4.2, -3.2, -1.2), 2.8)
    g_hand = vm.grp("day_hand", "spin", (0, 0, -2.4), -0.35)
    g_sec = vm.grp("behind_hand", "spin", (0, 0, -2.2), 4.5)
    g_chain = vm.grp("chain", "sway", (0, 12, 0), 3.0, 0.05, 0.0)
    g_door = vm.grp("vault_door", "spin", (0, 0, 2.4), 0.5)
    # a hexagonal brass case: a thick bezel with six bolts like a vault door, a dark plate, and the works showing through
    hexa = vm.P("bezel", "bronze", group=g_w, base=(214, 168, 80), lod=True)
    geo.merge(hexa, geo.prim_torus(9.6, 0.9, n=6, m=4), geo.rot_matrix("x", -90), (0, 0, -1.4))
    geo.merge(hexa, geo.prim_torus(7.6, 0.55, n=6, m=4), geo.rot_matrix("x", -90) @ geo.rot_matrix("y", 30), (0, 0, -1.9))
    plate = vm.P("back_plate", "obsidian", group=g_w, base=(34, 28, 18), lod=False)
    geo.lathe(plate, "y", (0, 0, 0), [(0.0, 9.4), (0.8, 9.4)], sides=6, caps=(True, True))
    plate.V = [geo.rot_matrix("x", -90) @ v + np.array([0, 0, 0.8]) for v in plate.V]
    bolts = vm.P("vault_bolts", "iron", group=g_bolts, base=(110, 96, 70), lod=False)
    bolt = geo.prim_box(1.1, 3.0, 1.1, caps=False)
    head = geo.prim_octa(0.8, 0.9)
    for i in range(6):
        a = math.pi / 3 * i + math.pi / 6
        geo.merge(bolts, bolt, geo.rot_matrix("z", math.degrees(a) - 90), (11.0 * math.cos(a), 11.0 * math.sin(a), -1.2))
        geo.merge(bolts, head, geo.align_y((math.cos(a), math.sin(a), 0)), (12.8 * math.cos(a), 12.8 * math.sin(a), -1.2))
    # the works: three gears of different sizes meshing, a keyhole, a day hand and a hand behind the day
    for g, nm, c, R_, teeth, base in ((g_g1, "gear_great", (0, 0, 0), 6.0, 6, (230, 176, 70)), (g_g2, "gear_mid", (-4.5, 3.5, 0), 3.2, 6, (206, 150, 60)), (g_g3, "gear_small", (4.2, -3.2, 0), 2.4, 5, (240, 196, 100))):
        gp = vm.P(nm, "gold", group=g, base=base, slot=TRIM, lod=False)
        geo.merge(gp, geo.prim_gear(R_, teeth, 0.9, 0.9), geo.rot_matrix("x", 90) @ np.eye(3), (c[0], c[1], -1.0 - (0.2 if nm != "gear_great" else 0)))
    key = vm.P("keyhole", "iron", group=g_w, base=(20, 16, 10), lod=False)
    geo.merge(key, geo.prim_lathe([(0.0, 1.2), (0.3, 1.2)], sides=vm.S(8, 5), caps=(True, True)), geo.rot_matrix("x", -90), (0, 0, -2.0))
    geo.merge(key, geo.prim_box(0.7, 2.4, 0.3, caps=False), None, (0, -1.8, -2.1))
    h1 = vm.P("day_hand", "gold", group=g_hand, base=(255, 224, 140), slot=TRIM, lod=False)
    geo.merge(h1, geo.prim_blade(5.4, 0.9, 0.3, tip=0.3), geo.rot_matrix("x", 90), (0, 0, -2.5))
    h2 = vm.P("behind_hand", "crystal", group=g_sec, base=(255, 140, 70), slot=GLOW, lod=False)
    geo.merge(h2, geo.prim_blade(7.4, 0.35, 0.2, tip=0.3), geo.rot_matrix("x", 90), (0, 0, -2.3))
    crown = vm.P("crown", "gold", group=g_w, base=(236, 196, 100), slot=TRIM, lod=False)
    geo.lathe(crown, "y", (0, 0, 0), [(9.4, 1.0), (10.2, 1.4), (11.8, 1.0)], sides=vm.S(6, 4), caps=(True, True))
    geo.merge(crown, geo.prim_torus(1.9, 0.4, n=vm.S(8, 6), m=3), geo.rot_matrix("x", 90), (0, 13.6, 0))
    chain = vm.P("chain", "bronze", group=g_chain, base=(190, 150, 70), lod=False)
    geo.chain(chain, [(0, 15.4, 0), (1.5, 19.0, 0), (4.0, 22.0, 0)], link_r=0.8, wire=0.2, n=6, m=3, every=3.0)
    vm.fx_disc(g_door, GLOW, (0, 0, 2.4), (0, 0, 1), 28.0, "seal")
    vm.fx_disc(g_door, DARK, (0, 0, 2.0), (0, 0, 1), 22.0, "ticks", rot=9)
    K.halo_set(vm, pal, y=0.0, R=9.0, rune=True, shaft_len=22.0)
    vm.deco(PT.deco_fill_glow, h2, (255, 120, 60), strength=0.95, seed=3)
    vm.deco(PT.deco_glyphs, hexa, glow, text="VAULT OF HOURS", rows=1, glow=False, seed=4, min_w=10)
    vm.deco(PT.deco_veins, plate, glow, n=3, strength=0.5, seed=5, min_w=8, min_h=8)
    for gname in ("gear_great", "gear_mid", "gear_small"):
        vm.deco(PT.deco_rim_glow, [p for p in vm.mesh.parts if p.name == gname][0], glow, strength=0.7)
    vm.deco(PT.deco_grain, hexa, amount=0.12)


add(Item("vaultmakers_pocketwatch", build_vaultwatch, dict(style="grid", dark=(8, 6, 0), mid=(150, 100, 20), hot=(255, 224, 150)), hand_size=0.74, hand_style="hang", seed=28,
         layers=[GL("great gear", "orbit_shards", "gear_great"), GL("mid gear", "orbit_shards", "gear_mid"), GL("small gear", "orbit_shards", "gear_small"),
                 GL("day hand", "halo_ring", "day_hand"), GL("hand behind the day", "halo_ring", "behind_hand"), GL("vault bolts", "aura_shell", "vault_bolts"),
                 GL("vault door seal", "rune_ring", "vault_door"), GL("rune ring", "rune_ring", "halo_ring"), GL("lens flare", "lens_flare", "lens_flare"),
                 GL("light shafts", "light_shafts", "light_shafts"),
                 PL("brass sparks", "sparks", "crit", None, count=1, every=7, radius=0.45, rise=0.0),
                 PL("gold dust", "motes", "dust", 0xE0A040, count=1, every=5, radius=0.5, rise=0.01)],
         acts={"stop_the_watch": [ST("MAGIC_CIRCLE", 0xE0A040, 3.2, 60, "ground"), ST("DIVINE_HALO", 0xFFE0A0, 2.4, 60, "self"), ST("LIGHT_FLARE", 0xFFE0A0, 1.4, 14, "self")],
               "wind_back": [ST("MAGIC_CIRCLE", 0xE0A040, 2.4, 40, "ground"), ST("MANA_CHARGE", 0xFFE0A0, 1.6, 30, "self")]}))

#!/usr/bin/env python3
"""
HOLY_CHURCH (1.59.0, docs/labyrinth/mega_v2_tha.md): the signature rooms of the Holy road's cathedral. Rooms (all `holy_church_*`, drawn ONLY in sectors of the
theme through `data/nusmp/mega/template_themes/holy_church.json`):

    hall/holy_church_nave         the great nave: pews, two arcades, a gallery above each aisle, a choir loft with an organ over the door, the chancel and altar
    hall/holy_church_cloister     the cloister garden: a covered walk round an open garden under a glass roof, a well in the middle
    set/holy_church_bell_tower    the bell tower: a climb up four storeys to a real belfry with a bronze bell
    puzzle/holy_church_confessional  six confessional booths, four of them with a sigil plate; a Riddle Stone names the order in which to press them and a puzzle door opens the sacristy
    hall/holy_church_crypt        the crypt under the church: low vaults, sarcophagi, an ossuary pit
    treasure/holy_church_reliquary  the reliquary vault: gilded cases of relics behind glass, a chest of offerings
    safe/holy_church_baptistery   the baptistery: a font under a stone canopy, a diamond of gold in the floor, nine seats, a votive fire and three pallets (a Safe Zone)

    python3 tools/labyrinth/gen_mega_theme_holy_church.py              # (re)write every room, check each one (a room with a defect is NOT written)
    python3 tools/labyrinth/gen_mega_theme_holy_church.py nave crypt   # only these
    python3 tools/labyrinth/gen_mega_theme_holy_church.py --check      # write nothing; compare with the files on disk (CI style)

Why a generator: see gen_mega_rooms1.py; the same kit (`mega_decor_r1`) and the same gates run here, plus `mega_tha_kit` for windows, arcades and decks.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_tha_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

ROOMS = {}
THEME = "holy_church"


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


# jewel palettes: every wall gets its own set, so no two windows of a church are alike
WEST = (("red", "yellow", "red", "orange"), ("blue", "light_blue", "blue", "cyan"), ("purple", "magenta", "purple", "pink"), ("red", "orange", "yellow", "orange"))
EAST = (("green", "lime", "green", "yellow"), ("cyan", "light_blue", "white", "blue"), ("orange", "yellow", "orange", "red"), ("magenta", "pink", "purple", "blue"))
APSE = (("red", "orange", "yellow", "white", "yellow", "orange"), ("blue", "light_blue", "white", "light_blue"), ("red", "magenta", "purple", "blue"))


def vault_slice(h, z, ring, x0=8, x1=18, y_spring=22, y_top=None):
    """One slice (a row of cells across x at z) of the nave's pointed barrel vault; `ring` is 'polished' for a rib and 'brick' between ribs."""
    import mega_arch_kit as AK
    y_top = h.top if y_top is None else y_top

    def put(u, y, block, props):
        h.set(x0 + u, y, z, block, props, over=True)
    AK.pointed_arch(put, x1 - x0 + 1, y_spring, y_top, ring, "brick", "west", "east")


def gable_window(h, side, colors, y_spring=22, x0=8, x1=18):
    """Fills the wall behind the vault's end slice with coloured glass in the shape of the vault's section: a gable window, with polished mullions."""
    w = h.wall(side)
    n = x1 - x0 + 1
    for r in range(0, n // 2 + 1):
        y = y_spring + r
        lo, hi = x0 + r + 1, x1 - r - 1
        for x in range(lo, hi + 1):
            mullion = (x - x0) in (3, n - 4) or (y == y_spring + 2 and r == 2)
            if mullion:
                w.carve(x, y, -1)
                w.set(x, y, -1, L.POLISHED)
                continue
            col = colors[((x - x0) // 3 + r) % len(colors)] if len(colors) > 1 else colors[0]
            K.window_cell(w, x, y, K.glass(col))


def organ(h, z, y, wall_z=42):
    """A pipe organ against the south wall: a case of planks across the nave, pipes of iron bars standing alone (a gap between every two so each reads as a pipe)
    and rising to the middle under the vault, copper-tipped, a second rank of short pipes in front, and a bench and a console of note blocks before it. `y` is the
    standing row of the loft; the pipes stop under the vault, which is 5 rows lower at the sides of the nave than in the middle."""
    for x in range(9, 18):
        h.set(x, y, wall_z, L.PLANKS)
        h.set(x, y, wall_z - 1, L.PLANKS_SLAB, {"type": "bottom"})
    for x in (9, 11, 13, 15, 17):
        top = GROUND + 17 - abs(x - 13)
        for yy in range(y + 1, top):
            h.set(x, yy, wall_z, L.IRON_BARS)
        h.set(x, top, wall_z, "minecraft:lightning_rod", {"facing": "up", "powered": False, "waterlogged": False})
    for x in (10, 12, 14, 16):
        for yy in range(y + 1, y + 4 + (2 if x in (12, 14) else 0)):
            h.set(x, yy, wall_z - 1, L.IRON_BARS)
    for x in (12, 13, 14):
        h.set(x, y, wall_z - 2, "minecraft:note_block")
    h.set(13, y, wall_z - 3, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})


@room("nave")
def nave():
    h = Hall("mega/hall/holy_church_nave", 3, 5, 22, wear=0.06)
    for side, off in (("s", 13), ("n", 4), ("n", 22), ("w", 22), ("e", 22)):
        h.door(side, off)
    wn, ww, we = h.wall("n"), h.wall("w"), h.wall("e")
    h.route(12, 10, 14, 41, label="the aisle of the nave")
    h.route(2, 20, 24, 24, label="the crossing")
    ARCH = [5, 12, 19, 26, 33]                               # arch k spans z = a .. a + 6: its opening is z = a + 1 .. a + 5, centred on 8, 15, 22, 29, 36
    BAYS = [a + 3 for a in ARCH]
    # ---- the two arcades and the pointed barrel vault over the nave
    for x in (8, 18):
        K.arcade(h, "z", x, ARCH, 7, GROUND, GROUND + 3, GROUND + 12)
    for z in range(2, 43):
        vault_slice(h, z, "polished" if z in (2, 42) or z in [a + o for a in ARCH for o in (0, 6)] else "brick")
    # ---- the roofs over the aisles (the floor of nothing: a sealed lid) and the gallery decks
    for x0, x1 in ((2, 7), (19, 24)):
        K.solid(h, x0, 5, x1, 42, GROUND + 12, GROUND + 12, L.TILE)
    for sx in (2, 22):
        K.stairs_up(h, sx, 42, "north", 7, width=3, base=GROUND)
    stair_hole = {(x, z) for x in (2, 3, 4, 22, 23, 24) for z in range(37, 43)}
    for z in range(5, 43):
        for x in list(range(2, 8)) + list(range(19, 25)):
            if (x, z) not in stair_hole:
                h.try_set(x, GROUND + 6, z, L.PLANKS)
    for z in range(38, 43):
        for x in range(8, 19):
            h.try_set(x, GROUND + 6, z, L.PLANKS)
    for x in (5, 21):
        K.rail(h, x, 37, x, 42, GROUND + 7)
    K.rail(h, 9, 38, 17, 38, GROUND + 7)                     # the loft's front rail
    for x0, x1 in ((2, 7), (19, 24)):
        K.rail(h, x0, 5, x1, 5, GROUND + 7)
    # the triforium: openings in the arcade wall above each bay, a parapet below them
    for x in (8, 18):
        for c in BAYS:
            for z in range(c - 1, c + 2):
                for y in range(GROUND + 8, GROUND + 11):
                    h.carve(x, y, z)
    # ---- the floor: a runner down the nave, stone-brick field, sandstone borders, a medallion at the crossing
    for z in range(2, 43):
        for x in (11, 15):
            h.floor_set(x, z, L.POLISHED)
    for z in range(3, 42):
        for x in range(2, 25):
            if (x < 8 or x > 18) and (x + z) % 2 == 0:
                h.floor_set(x, z, L.POLISHED)
    D.floor_medallion(h, 13, 22, r=6, ring=L.CHISELED, core=L.CHISELED, field=L.POLISHED)
    for z in range(10, 42):
        if not 19 <= z <= 25:
            for x in (12, 13, 14):
                h.try_set(x, GROUND, z, "minecraft:red_carpet")
    # ---- the chancel: one step up, a platform, the altar with its cloth, candlesticks, the reredos and the cross
    for x in range(9, 18):
        h.set(x, GROUND, 9, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
        for z in range(3, 9):
            h.set(x, GROUND, z, L.POLISHED if (x + z) % 2 else L.TILE)
    for x in (12, 13, 14):
        h.set(x, GROUND + 1, 5, L.CHISELED if x == 13 else L.POLISHED)
        h.set(x, GROUND + 2, 5, "minecraft:white_carpet")
        for z in (6, 7):
            h.try_set(x, GROUND + 1, z, "minecraft:red_carpet")
    for x in (11, 15):
        D.candelabra(h, x, 5, y=GROUND + 1, n=4, color="white")
    for x in (10, 16):
        D.candelabra(h, x, 7, y=GROUND + 1, n=3, color="white")
    for x in range(10, 17):                                  # the reredos: a screen with three niches, a gilded shelf, candles
        for y in range(GROUND + 1, GROUND + 6):
            if x in (11, 13, 15) and y in (GROUND + 2, GROUND + 3, GROUND + 4):
                continue
            h.set(x, y, 3, L.POLISHED if y < GROUND + 5 else L.CHISELED)
    for x in (11, 13, 15):
        h.set(x, GROUND + 2, 3, "minecraft:gold_block")
        D.candles_at(h, x, GROUND + 3, 3, 3, "white")
    for y in range(GROUND + 7, GROUND + 13):
        h.set(13, y, 2, "minecraft:gold_block")
    for x in (12, 14):
        h.set(x, GROUND + 11, 2, "minecraft:gold_block")
    # ---- windows: a different set on every wall; the vault's end walls are gable windows
    gable_window(h, "n", APSE[0])
    gable_window(h, "s", APSE[2])
    K.band_of_windows(ww, [8, 15, 29, 36], GROUND + 1, GROUND + 5, WEST)
    K.band_of_windows(we, [8, 15, 29, 36], GROUND + 1, GROUND + 5, EAST)
    K.band_of_windows(ww, BAYS, GROUND + 8, GROUND + 12, WEST[::-1])
    K.band_of_windows(we, BAYS, GROUND + 8, GROUND + 12, EAST[::-1])
    K.lancet(wn, 4, GROUND + 6, GROUND + 17, APSE[1], width=3)
    K.lancet(wn, 22, GROUND + 6, GROUND + 17, APSE[1][::-1], width=3)
    K.lancet(wn, 13, GROUND + 7, GROUND + 12, APSE[0][::-1], width=3)
    # ---- pews: three seats wide on both sides of the runner, candle stands in the gaps
    pews = [11, 13, 15, 17, 27, 29, 31, 33]
    for z in pews:
        for x0 in (9, 15):
            for k in range(3):
                h.set(x0 + k, GROUND, z, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
    for z in (12, 16, 28, 32):
        D.candelabra(h, 11, z, n=3, color="white")
        D.candelabra(h, 15, z, n=3, color="white")
    h.marker(16, GROUND + 1, 27, "seat", facing="north", **{"as": "chair"})
    # ---- light: wagon chandeliers under the vault, a lantern in every arch of the arcade, lanterns under the gallery decks
    for z in (14, 22, 30):
        K.wagon_chandelier(h, 13, z, GROUND + 17, 6, r=2)
    for x in (8, 18):
        for c in BAYS:
            h.set(x, GROUND + 5, c, L.CHAIN_BLOCK, {"axis": "y"})
            h.set(x, GROUND + 4, c, L.LANTERN, {"hanging": "true"})
    for z in BAYS:
        for x in (6, 20):
            D.hang_lantern(h, x, z, 2, y_top=GROUND + 5)
    # ---- banners on the nave faces of the piers, holy water at the door
    for x, face, nx in ((8, "east", 9), (18, "west", 17)):
        for a in ARCH:
            for z in (a, a + 6):
                h.try_set(nx, GROUND + 4, z, "minecraft:red_wall_banner", {"facing": face})
    h.set(10, GROUND, 38, "minecraft:water_cauldron", {"level": 3})
    h.set(16, GROUND, 38, "minecraft:water_cauldron", {"level": 3})
    D.lectern(h.frame(16, GROUND + 1, 8, "south"))
    D.statue(h, 9, 4, "south", kit="gold", y=GROUND + 1)
    D.statue(h, 17, 4, "south", kit="gold", y=GROUND + 1)
    # ---- the choir loft: the organ against the south wall, its console and the stalls
    organ(h, 42, GROUND + 7)
    for x0, x1 in ((6, 10), (16, 20)):
        for x in range(x0, x1 + 1):
            h.try_set(x, GROUND + 7, 40, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})
    h.faction("cultist", 13, 14)
    K.auto_light(h)
    D.auto_markers(h)
    return h


FLOWERS = ("minecraft:poppy", "minecraft:dandelion", "minecraft:cornflower", "minecraft:azure_bluet", "minecraft:lily_of_the_valley", "minecraft:oxeye_daisy",
           "minecraft:allium", "minecraft:red_tulip", "minecraft:white_tulip")


@room("cloister")
def cloister():
    h = Hall("mega/hall/holy_church_cloister", 3, 3, 14, wear=0.08)
    for side in ("n", "s", "w", "e"):
        h.door(side, 13)
    top = h.top                                              # 22
    rng = h.rng
    ARCS = [6, 11, 16]                                       # three arches of five cells on every side: openings 7..9, 12..14, 17..19
    for z in (6, 20):
        K.arcade(h, "x", z, ARCS, 5, GROUND, GROUND + 2, GROUND + 5)
    for x in (6, 20):
        K.arcade(h, "z", x, ARCS, 5, GROUND, GROUND + 2, GROUND + 5)
    # ---- the roofs of the four walks, the garden's skylight (jewel glass over a glowing lid) and its floor
    for z in range(2, 25):
        for x in range(2, 25):
            if 7 <= x <= 19 and 7 <= z <= 19:
                continue
            h.try_set(x, GROUND + 5, z, L.TILE)
    SKY = ("light_blue", "white", "light_blue", "cyan")
    for z in range(7, 20):
        for x in range(7, 20):
            rib = (x - 7) % 4 == 0 or (z - 7) % 4 == 0
            h.shell_set(x, top + 1, z, L.POLISHED if rib else K.glass(SKY[((x - 7) // 4 + (z - 7) // 4) % len(SKY)]))
            h.shell_set(x, top + 2, z, "minecraft:sea_lantern" if not rib else L.POLISHED)
            h.floor_set(x, z, "minecraft:grass_block", {"snowy": "false"})
    for z in range(2, 25):
        for x in range(2, 25):
            if not (7 <= x <= 19 and 7 <= z <= 19) and (x + z) % 2 == 0 and not (6 <= x <= 20 and 6 <= z <= 20):
                h.floor_set(x, z, L.POLISHED)
    for k in range(7, 20):                                   # the gravel cross of paths to the well
        for d in (12, 13, 14):
            h.floor_set(d, k, "minecraft:dirt_path")
            h.floor_set(k, d, "minecraft:dirt_path")
    h.route(12, 7, 14, 10, label="path n")
    h.route(12, 16, 14, 19, label="path s")
    h.route(7, 12, 10, 14, label="path w")
    h.route(16, 12, 19, 14, label="path e")
    K.well(h, 13, 13)
    # ---- the garden: four trees, statues and lamp posts first, then grass, ferns and a colour of flower to every quarter
    for (tx, tz) in ((9, 9), (17, 9), (9, 17), (17, 17)):
        K.tree(h, tx, tz, trunk=5, radius=2)
    for (lx, lz) in ((11, 8), (15, 8), (11, 18), (15, 18), (8, 11), (8, 15), (18, 11), (18, 15)):
        K.lamp_post(h, lx, lz, height=2)
    for (sx, sz) in ((11, 11), (15, 11), (11, 15), (15, 15)):
        D.statue(h, sx, sz, "south", kit="gold", y=GROUND)
    for z in range(7, 20):
        for x in range(7, 20):
            c = h.get(x, FLOOR, z)
            if c is None or c.block != "minecraft:grass_block" or not h.free(x, GROUND, z):
                continue
            quarter = (0 if x < 13 else 1) + (0 if z < 13 else 2)
            r = rng.random()
            if r < 0.30:
                h.set(x, GROUND, z, "minecraft:short_grass")
            elif r < 0.50:
                h.set(x, GROUND, z, FLOWERS[(quarter * 2 + (x + z) % 2) % len(FLOWERS)])
            elif r < 0.56:
                h.set(x, GROUND, z, "minecraft:fern")
    # ---- the walks: benches against the outer walls, banners between the windows, windows high in the garden walls
    for x in (7, 8, 9, 17, 18, 19):
        K.bench(h, x, 2, "south")
        K.bench(h, x, 24, "north")
    for z in (7, 8, 9, 17, 18, 19):
        K.bench(h, 2, z, "east")
        K.bench(h, 24, z, "west")
    h.marker(8, GROUND + 1, 2, "seat", facing="south", **{"as": "chair"})
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    for w, pal in ((wn, WEST), (ws, EAST), (ww, WEST[::-1]), (we, EAST[::-1])):
        K.band_of_windows(w, [5, 9, 13, 17, 21], GROUND + 7, GROUND + 12, pal, width=3)
        K.band_of_windows(w, [8, 18], GROUND + 1, GROUND + 4, pal, width=1)
    h.faction("construct", 13, 5)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("bell_tower")
def bell_tower():
    h = Hall("mega/set/holy_church_bell_tower", 2, 2, 36, wear=0.10)
    h.door("s", 4)
    h.door("w", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the climb: three flights of eight steps round the walls (north to east, east to south, south to west), a landing at every corner, a rail on the open side
    K.stairs_up(h, 5, 2, "east", 8, width=3, base=GROUND, fill_down=1, rail_col=2)           # rows 9..16: from the north-west landing to the north-east one
    K.stairs_up(h, 13, 5, "south", 8, width=3, base=GROUND + 8, fill_down=1, rail_col=0)     # rows 17..24: to the south-east landing
    K.stairs_up(h, 12, 13, "west", 8, width=3, base=GROUND + 16, fill_down=1, rail_col=0)    # rows 25..32: to the south-west landing, the belfry floor
    for (x0, z0, row) in ((13, 2, GROUND + 7), (13, 13, GROUND + 15)):
        for z in range(z0, z0 + 3):
            for x in range(x0, x0 + 3):
                h.set(x, row, z, L.POLISHED if (x + z) % 2 else L.TILE)
                h.set(x, row - 1, z, L.COBBLED)
    # the belfry deck: a ring round a square hole over the shaft (railed), a chest on the south-east landing, candles on the landings
    for z in range(2, 16):
        for x in range(2, 16):
            if 7 <= x <= 10 and 7 <= z <= 10:
                continue
            h.try_set(x, GROUND + 23, z, L.PLANKS if (x + z) % 3 else L.TILE)
    for z in range(6, 12):
        for x in range(6, 12):
            if (x in (6, 11) or z in (6, 11)) and h.free(x, GROUND + 24, z):
                h.set(x, GROUND + 24, z, L.FENCE)
    # ---- the belfry: louvres of bars in front of a glow, the beam, the bell, the gargoyles
    for w in (wn, ws, ww, we):
        for lat in range(5, 13):
            for y in range(GROUND + 25, GROUND + 31):
                if lat in (8, 9) or y in (GROUND + 25, GROUND + 30) or lat in (5, 12):
                    continue
                K.window_cell(w, lat, y, L.IRON_BARS, "minecraft:sea_lantern")
    for x in range(2, 16):
        h.set(x, GROUND + 31, 8, L.LOG, {"axis": "x"})
    for z in range(2, 16):
        h.try_set(8, GROUND + 31, z, L.LOG, {"axis": "z"})
    h.set(8, GROUND + 30, 8, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    for (gx, gz, face) in ((3, 3, "south"), (14, 3, "west"), (3, 14, "east"), (14, 14, "north")):
        D.statue(h, gx, gz, face, kit="skull", y=GROUND + 24)
    h.loot_chest(14, GROUND + 16, 14, "north")
    D.candles_at(h, 13, GROUND + 16, 14, 3, "white")
    # ---- the ground floor: a saint on a plinth in the middle of the foot of the shaft, candles round it, a holy-water font by each door
    for dx, dz in ((0, 0), (1, 0), (0, 1), (1, 1)):
        h.set(8 + dx, GROUND, 8 + dz, L.POLISHED)
        h.set(8 + dx, GROUND + 1, 8 + dz, L.CHISELED if dx == dz else L.POLISHED_SLAB, {"type": "bottom"} if dx != dz else None)
    for (x, z) in ((6, 6), (11, 6), (6, 11), (11, 11)):
        D.candelabra(h, x, z, n=4, color="white")
    # windows: narrow lancets up the walls, one per flight
    K.lancet(wn, 8, GROUND + 3, GROUND + 14, WEST[0], width=3)
    K.lancet(we, 8, GROUND + 11, GROUND + 22, EAST[1], width=3)
    K.lancet(ws, 8, GROUND + 19, GROUND + 22, APSE[1], width=3)
    K.lancet(ww, 8, GROUND + 3, GROUND + 14, WEST[1], width=3)
    h.faction("construct", 8, 12)
    K.auto_light(h, max_lamps=200)
    D.auto_markers(h)
    return h


SIGIL_ORDER = (1, 0, 3, 2)                                   # blue, red, green, gold: the Riddle Stone of the confessional asks for this order


@room("confessional")
def confessional():
    h = Hall("mega/puzzle/holy_church_confessional", 3, 2, 12, wear=0.08)
    h.door("w", 13)
    h.door("s", 13)
    top = h.top                                              # 20
    rng = h.rng
    wn, ws, ww = h.wall("n"), h.wall("s"), h.wall("w")
    h.route(3, 5, 17, 7, label="the walk before the booths")
    h.route(9, 8, 11, 11, label="the aisle")
    centres = [5, 7, 9, 11, 13, 15]
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    plate_booths = [0, 2, 3, 5]
    banner_of = {0: "red", 1: "blue", 2: "yellow", 3: "green"}
    # ---- the booths: timber partitions, a roof, a prie-dieu, a grille lit from behind, a banner in the colour of the plate
    for x in range(4, 17, 2):
        for z in range(2, 5):
            for y in range(GROUND, GROUND + 3):
                h.set(x, y, z, L.STRIPPED_LOG if z == 4 else L.PLANKS, {"axis": "y"} if z == 4 else None)
    for x in range(4, 17):
        for z in range(2, 5):
            h.set(x, GROUND + 3, z, L.PLANKS)
    for i, c in enumerate(centres):
        h.set(c, GROUND, 3, L.PLANKS_SLAB, {"type": "bottom"})
        for y in (GROUND + 1, GROUND + 2):
            K.window_cell(wn, c, y, L.IRON_BARS, "minecraft:sea_lantern")
        if i in plate_booths:
            k = plate_booths.index(i)
            K.sigil_plate(h, c, GROUND, 2, colours[k])
            h.set(c, GROUND + 3, 5, "minecraft:%s_wall_banner" % banner_of[colours[k]], {"facing": "south"})
        else:
            D.skull_at(h, c, GROUND, 2, rot=8)
            h.set(c, GROUND + 3, 5, "minecraft:%s_wall_banner" % ("white" if i == 1 else "purple"), {"facing": "south"})
    for x in range(3, 18):
        h.try_set(x, GROUND, 5, "minecraft:red_carpet")
    for z in range(6, 12):
        for x in (9, 10, 11):
            h.try_set(x, GROUND, z, "minecraft:red_carpet")
    # ---- the nave of the hall: pews facing the booths, candles, the Riddle Stone on its plinth by the door, the holy-water font
    for z in (8, 10):
        for x0 in (3, 12):
            for k in range(6):
                h.set(x0 + k, GROUND, z, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
    h.marker(14, GROUND + 1, 10, "seat", facing="north", **{"as": "chair"})
    K.riddle_stone(h, 10, GROUND, 13, SIGIL_ORDER)
    K.lamp_post(h, 9, 13, height=1)
    K.lamp_post(h, 11, 13, height=1)
    for (x, z) in ((3, 11), (3, 15), (17, 11), (17, 15)):
        D.candelabra(h, x, z, n=3, color="white")
    # ---- the sacristy behind the puzzle door
    K.puzzle_closet(h, h.frame(18, GROUND, 8, "east"), top)
    # ---- windows, light
    K.band_of_windows(ws, [5, 8, 17], GROUND + 1, GROUND + 9, EAST, width=3)
    K.band_of_windows(ww, [5, 9], GROUND + 1, GROUND + 9, WEST, width=3)
    K.band_of_windows(wn, [6, 10, 14], GROUND + 5, GROUND + 10, APSE, width=3)
    for x in (7, 14):
        K.wagon_chandelier(h, x, 12, top, 5, r=1, color="white")
    h.faction("cultist", 13, 7)
    K.auto_light(h)
    D.auto_markers(h, spawns=3, patrols=2)
    return h


@room("crypt")
def crypt():
    h = Hall("mega/hall/holy_church_crypt", 3, 3, 11, wear=0.22)
    for side, off in (("n", 4), ("n", 22), ("s", 4), ("s", 22), ("w", 13), ("e", 13)):
        h.door(side, off)
    top = h.top                                              # 19
    ARCH = [2, 9, 16]                                        # three arches of seven cells between pier pairs; the last two cells of the aisle wall stay solid
    for x in (8, 18):
        K.arcade(h, "z", x, ARCH, 7, GROUND, GROUND + 1, GROUND + 4)
        for z in (23, 24):
            for y in range(GROUND, GROUND + 5):
                h.set(x, y, z, L.POLISHED)
    for z in range(2, 25):
        vault_slice(h, z, "polished" if z in [a + o for a in ARCH for o in (0, 6)] else "brick", y_spring=GROUND + 5, y_top=top)
    for x0, x1 in ((2, 7), (19, 24)):
        K.solid(h, x0, 2, x1, 24, GROUND + 5, GROUND + 5, L.TILE)
    # ---- the floor: stone-brick field with a polished border and a cracked belt, the saint's tomb on a dais at the north end
    for z in range(2, 25):
        for x in (11, 15):
            h.floor_set(x, z, L.POLISHED)
    D.dais(h, 10, 2, 16, 5, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    f = h.frame(14, GROUND + 1, 3, "south")
    D.sarcophagus(f, length=4, width=3, banner=None)
    for x in (10, 16):
        D.candelabra(h, x, 6, n=4, color="white")
    for x in (11, 15):
        h.try_set(x, GROUND + 1, 2, "minecraft:gold_block")
    # ---- the aisles: sarcophagi against the outer walls, skulls in the wall niches, candles, webs, bones
    for z0 in (8, 15):
        D.sarcophagus(h.frame(3, GROUND, z0, "south"), length=4, width=2, open_lid=(z0 == 15))
        D.sarcophagus(h.frame(24, GROUND, z0, "south"), length=4, width=2, open_lid=(z0 == 8))
    for side in ("w", "e"):
        D.loculi(h, side, [z for z in range(3, 24, 2) if not 10 <= z <= 16], (GROUND + 1, GROUND + 3))
    # ---- windows: one slit of coloured glass in each end of the aisles, and the gable of the nave
    gable_window(h, "s", APSE[1], y_spring=GROUND + 5, x0=8, x1=18)
    # ---- the descent: a stairwell in the floor, a sunk chamber of bones and tombs under the north half of the nave
    K.sunken_lining(h, 9, 2, 17, 11, 4, skip={(12, 12), (13, 12), (14, 12)})
    for z in range(12, 15):                                  # the stairwell: a hole in the floor, walled on both sides, four steps down to the chamber
        for x in (11, 15):
            for y in range(4, FLOOR):
                if z > 12:
                    h.set(x, y, z, D.brick_mix(h, 0.2, 0.3, L.COBBLED))
        for x in (12, 13, 14):
            h.carve(x, FLOOR, z)
    for x in (12, 13, 14):
        h.carve(x, FLOOR, 15)
        for k in range(4):
            row = FLOOR - k
            h.set(x, row, 15 - k, L.POLISHED_STAIRS, {"facing": "south", "half": "bottom"})
            for yy in range(4, row):
                h.set(x, yy, 15 - k, L.COBBLED)
    for x in (9, 17):
        D.sarcophagus(h.frame(x, FLOOR - 3, 3, "south"), length=4, width=1, open_lid=(x == 17))
    for z in (7, 9):
        D.bones(h, 13, z, rot=z, big=True)
    D.candelabra(h, 12, 3, y=FLOOR - 3, n=4, color="white")
    D.candelabra(h, 14, 3, y=FLOOR - 3, n=4, color="white")
    h.loot_chest(13, FLOOR - 3, 2, "south")
    D.cobwebs(h, 0.6)
    h.faction("undead", 13, 10)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("reliquary")
def reliquary():
    h = Hall("mega/treasure/holy_church_reliquary", 2, 2, 13, wear=0.05)
    h.door("n", 4)
    h.door("s", 13)
    top = h.top                                              # 21
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    K.hip_roof(h, 2, 2, 15, 15, GROUND + 4, top, cap=K.glass("yellow"), glow="minecraft:ochre_froglight")
    # ---- the floor: a gold-and-white medallion under the shrine, red carpets to it from both doors
    D.floor_medallion(h, 8, 8, r=6, ring=L.CHISELED, core="minecraft:gold_block", field=L.POLISHED)
    for z in range(2, 16):
        for x in (4, 5) if z < 7 else (12, 13):
            h.try_set(x, GROUND, z, "minecraft:red_carpet")
    # ---- the shrine in the middle: three steps up to a gilded case of glass round a skull on a gold block, candles at the four corners
    D.dais(h, 6, 6, 11, 11, rows=1, top="minecraft:gold_block", step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    for z in (8, 9):
        for x in (8, 9):
            h.set(x, GROUND + 1, z, "minecraft:gold_block")
    for z in range(7, 11):
        for x in range(7, 11):
            if x in (7, 10) or z in (7, 10):
                for y in (GROUND + 1, GROUND + 2):
                    if (x in (7, 10)) and (z in (7, 10)):
                        h.set(x, y, z, L.CHISELED if y == GROUND + 1 else "minecraft:gold_block")
                    else:
                        h.set(x, y, z, K.pane("white"))
    for z in (8, 9):
        for x in (8, 9):
            h.set(x, GROUND + 3, z, K.glass("white"))
    h.set(8, GROUND + 2, 8, "minecraft:skeleton_skull", {"rotation": "8"})
    for (x, z) in ((6, 6), (11, 6), (6, 11), (11, 11)):
        D.candelabra(h, x, z, y=GROUND + 1, n=4, color="yellow")
    # ---- eight niches (two to a wall) with their own small case: a block of the relic's metal behind a pane, a candle; two hold the offerings
    cases = ("gold_block", "diamond_block", "emerald_block", "lapis_block", "amethyst_block", "raw_gold_block", "copper_block", "iron_block")
    i = 0
    chest_niches = {1, 6}
    for side in ("n", "e", "s", "w"):
        w = h.wall(side)
        for lat in (5, 12):
            if (side, lat) in (("n", 5), ("s", 12)):
                continue                                     # the doorways
            D.niche(w, lat, GROUND, GROUND + 3, width=3)
            if i in chest_niches:
                x, y, z = w.cell(lat, GROUND, -1)
                h.loot_chest(x, y, z, w.into)
                D.candles_at(h, *w.cell(lat - 1, GROUND, -1), 3, "yellow")
                D.candles_at(h, *w.cell(lat + 1, GROUND, -1), 3, "yellow")
            else:
                w.set(lat, GROUND, -1, "minecraft:" + cases[i % len(cases)])
                for dl in (-1, 1):
                    D.candles_at(h, *w.cell(lat + dl, GROUND, -1), 2, "yellow")
            i += 1
    # ---- windows (glowing glass in the walls above the niches), light
    for w, pal in ((wn, EAST), (ws, WEST), (ww, APSE), (we, WEST[::-1])):
        K.band_of_windows(w, [8, 9], GROUND + 6, GROUND + 10, pal, width=1)
    for (x, z) in ((15, 3), (3, 14), (15, 14)):
        K.lamp_post(h, x, z, height=2)
    h.faction("construct", 8, 4)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("baptistery")
def baptistery():
    h = Hall("mega/safe/holy_church_baptistery", 2, 2, 14, wear=0.02)
    h.door("n", 13)
    h.door("s", 13)
    h.safe = True
    K.keep_doors_clear(h)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the floor: a diamond of gold and white round the font
    D.floor_medallion(h, 8, 8, r=6, ring="minecraft:gold_block", core=L.CHISELED, field=L.POLISHED)
    # ---- the font: a rim of polished stone one high round a ring of still water (shut in by the rim and the floor) with a pedestal and candles in the middle
    for z in range(6, 11):
        for x in range(6, 11):
            if x in (6, 10) or z in (6, 10):
                h.set(x, GROUND, z, L.CHISELED if x in (6, 10) and z in (6, 10) else L.POLISHED)
            elif (x, z) == (8, 8):
                h.set(x, GROUND, z, L.POLISHED)
            else:
                h.set(x, GROUND, z, "minecraft:water", {"level": "0"})
    D.candles_at(h, 8, GROUND + 1, 8, 4, "white")
    # ---- the canopy over it: four pillars at the corners of a seven by seven square and a hipped roof closing to a lantern of light
    for (x, z) in ((5, 5), (11, 5), (5, 11), (11, 11)):
        for y in range(GROUND, GROUND + 8):
            h.set(x, y, z, L.CHISELED if y == GROUND + 7 else L.POLISHED)
    K.hip_roof(h, 5, 5, 11, 11, GROUND + 8, GROUND + 11, cap=K.glass("white"), glow="minecraft:sea_lantern")
    # ---- nine seats round it on three sides, each facing the font and each a rest anchor
    seats = [(2, z, "east") for z in (6, 8, 10)] + [(x, 2, "south") for x in (6, 8, 10)] + [(x, 15, "north") for x in (6, 8, 10)]
    for (x, z, sit_toward) in seats:
        h.set(x, GROUND, z, L.POLISHED_STAIRS, {"facing": D.OPP[sit_toward], "half": "bottom"})
        h.marker(x, GROUND + 1, z, "seat", facing=sit_toward, **{"as": "chair"})
    # ---- the hearth: a votive fire in a niche of polished stone in the south-west corner, a bench in front of it
    for z in range(13, 16):
        for x in range(2, 5):
            if (x, z) != (3, 14):
                h.set(x, GROUND, z, L.POLISHED)
    h.set(3, GROUND, 14, "minecraft:campfire", {"facing": "east", "lit": "true", "signal_fire": "false"})
    h.marker(3, GROUND + 1, 14, "hearth", **{"as": "stove"})
    h.set(5, GROUND, 14, L.POLISHED_STAIRS, {"facing": "east", "half": "bottom"})
    h.marker(5, GROUND + 1, 14, "seat", facing="west", **{"as": "chair"})
    # ---- three pallets of white wool in the north-west corner, a banner over each
    for x in (2, 3, 4):
        D.bed(h.frame(x, GROUND, 2, "south"), color="white", marker=True)
        h.try_set(x, GROUND + 3, 2, "minecraft:white_wall_banner", {"facing": "south"})
    for w, lats, pal in ((ww, [6, 10], WEST), (we, [6, 10], EAST), (wn, [6, 9], APSE), (ws, [6, 9], APSE[::-1])):
        K.band_of_windows(w, lats, GROUND + 3, GROUND + 10, pal, width=3)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    K.auto_light(h, threshold=8)
    return h


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, sys.argv[1:], __doc__.split("\n\n")[0]))

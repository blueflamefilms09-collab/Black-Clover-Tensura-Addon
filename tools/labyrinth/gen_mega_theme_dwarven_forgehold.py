#!/usr/bin/env python3
"""
Why: THEME-B (1.59.0, docs/labyrinth/mega_v2_thb.md section 2) gives the Holy road a dwarven hold: dressed stone, waxed copper and iron, lava that is kept behind glass, anvils
and forges that never went cold, mine carts on rails and a vault for dead kings. This script writes its rooms (`structure/mega/<folder>/dwarven_forgehold_*.nbt`, the prefix
`data/nusmp/mega/template_themes/dwarven_forgehold.json` claims for the theme), so only a sector of the Dwarven Forgehold theme ever draws them.

    python3 tools/labyrinth/gen_mega_theme_dwarven_forgehold.py                      # builds, checks and writes every room (a room with a defect is not written)
    python3 tools/labyrinth/gen_mega_theme_dwarven_forgehold.py hall_of_pillars --preview   # one room, then render it in its own theme into $THB_PREVIEWS
    python3 tools/labyrinth/gen_mega_theme_dwarven_forgehold.py --check              # CI style: builds in memory, checks, compares with the files on disk

Architecture uses the palette constants of mega_lib (the Region Theme turns them into polished andesite, stone bricks, waxed cut copper with chiselled copper keystones, plain
andesite and oak); the character of the hold is the free vanilla blocks on top: lava behind glass (every lava cell is shut in by the wall, so it can never flow), iron blocks,
copper bulbs that glow like runes, anvils, grindstones, blast furnaces, rails and ore. The wood of the palette stays at least four cells from any lava (lava sets oak alight).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_decor_r2 as D2  # noqa: E402
import gen_mega_rooms2 as R2  # noqa: E402
import mega_theme_kit as K  # noqa: E402
from mega_lib import MT, Frame  # noqa: E402
from mega_decor_r1 import Hall, GROUND, FLOOR  # noqa: E402

T = K.Theme("dwarven_forgehold")
FR = L.FLOOR_ROW

GLASS = "minecraft:glass"
LAVA = "minecraft:lava"
BULB = "minecraft:waxed_copper_bulb"
IRON = "minecraft:iron_block"
COPPER_GRATE = "minecraft:waxed_copper_grate"


# ===================================================================================================================
# the props of the hold
# ===================================================================================================================
def lava_window(w, lat, width=3, y0=GROUND + 1, y1=GROUND + 4, glass=GLASS):
    """A window onto lava: the cells of the inner wall layer (rows y0..y1, `width` laterals from `lat`) become lava and the interior cells in front of them become glass. The
    lava is shut in by the outer wall layer behind, the wall above, below and at both sides, and the glass in front, so it cannot flow (`Analysis.fluid_problems` proves it).
    Returns False (and places nothing) when a cell is taken or is not plain wall. Why: the brief's 'lava channels behind glass'; a lava sheet one block thick in the wall
    lights the hall with the colour of the forge and costs the room no floor."""
    h = w.hall
    cells = [(lt, y) for lt in range(lat, lat + width) for y in range(y0, y1 + 1)]
    for (lt, y) in cells:
        pos = w.cell(lt, y, -1)
        c = h.get(*pos)
        if c is None or pos in h.deco or pos in h.mt.reserved or c.block not in (L.BRICK, L.CRACKED_BRICK, L.TILE, L.COBBLED, L.ROCK, L.POLISHED, L.CHISELED):
            return False
        if not w.can(lt, y, 0, glass):
            return False
    for (lt, y) in cells:
        w.shell_set(lt, y, -1, LAVA, {"level": "0"})
        w.set(lt, y, 0, glass)
    for lt in range(lat - 1, lat + width + 1):                  # a copper sill and lintel round the window, in the wall's own layer
        for y in (y0 - 1, y1 + 1):
            pos = w.cell(lt, y, -1)
            c = h.get(*pos)
            if c is not None and pos not in h.deco and pos not in h.mt.reserved and c.block in (L.BRICK, L.CRACKED_BRICK, L.TILE, L.COBBLED):
                w.shell_set(lt, y, -1, L.CHISELED if lt in (lat - 1, lat + width) else L.POLISHED)
    return True


def rune_pillar(h, x, z, y1=None, bulbs=(4, 10), belts=(2, 7, 12), shaft=L.POLISHED, cap=L.CHISELED):
    """A dwarven rune pillar: a 2 x 2 shaft with a stair-ring foot and capital (`mega_decor_r1.column`), chiselled belts at the rows in `belts` (counted from the floor) and at
    each row in `bulbs` a ring of lit copper bulbs set into the shaft: the runes. A copper bulb keeps the `lit` it is placed with (it flips only on a redstone pulse), so the
    runes glow without any wiring. Returns the number of bulbs placed."""
    y1 = h.top if y1 is None else y1
    D.column(h, x, z, w=2, y1=y1, shaft=shaft, cap=cap)
    cells = [(x + dx, z + dz) for dx in (0, 1) for dz in (0, 1)]
    for r in belts:
        if GROUND + r < y1:
            for (cx, cz) in cells:
                h.set(cx, GROUND + r, cz, cap, over=True)
    n = 0
    for r in bulbs:
        if GROUND + r < y1:
            for (cx, cz) in cells:
                h.set(cx, GROUND + r, cz, BULB, {"lit": True, "powered": False}, over=True)
                n += 1
    return n


def lamp(h, x, z, drop, soul=False):
    """A lantern on a chain from whatever is over (x, z): the ceiling (row top + 1) or a beam in the top row. `drop` is the length of the chain in cells. Returns False when the
    column is taken or nothing holds it."""
    for y_top in (h.top, h.top - 1):
        above = h.get(x, y_top + 1, z)
        if above is None or not D.center_support(above.block, above.props, "down"):
            continue
        if all(h.can_set(x, y_top - k, z, L.CHAIN_BLOCK) for k in range(drop + 1)):
            return K.hang_chain_light(h, x, z, y_top, drop, soul=soul)
    return False


def great_anvil(h, cx, cz, along="z"):
    """The Great Anvil: a 3 x 5 foot of iron block, a one-wide waist two rows high and a 3 x 5 plate, standing on a platform of the room's own stone. `along` is the long axis.
    Returns False when a cell is taken. Why: the centrepiece of a hold's hall is the anvil the founders struck the first ingot on; built from iron blocks it needs no custom block
    and it reads as an anvil from the far end of the hall."""
    long_ax = (0, 1) if along == "z" else (1, 0)
    wide_ax = (1, 0) if along == "z" else (0, 1)
    cells = []
    for l in range(-2, 3):
        for wd in (-1, 0, 1):
            x, z = cx + long_ax[0] * l + wide_ax[0] * wd, cz + long_ax[1] * l + wide_ax[1] * wd
            cells.append((x, GROUND + 1, z, IRON))
            cells.append((x, GROUND + 5, z, IRON))
    for l in (-1, 0, 1):
        x, z = cx + long_ax[0] * l, cz + long_ax[1] * l
        cells.append((x, GROUND + 2, z, IRON))
        cells.append((x, GROUND + 3, z, IRON))
        cells.append((x, GROUND + 4, z, IRON))
    if not all(h.can_set(x, y, z, b) for (x, y, z, b) in cells):
        return False
    for (x, y, z, b) in cells:
        h.set(x, y, z, b)
    return True


def lit_furnace(h, x, y, z, kind, facing, over=False, burn=32767):
    """`Hall.furnace` with the `over` switch (a furnace set into a wall replaces a stone block): a furnace, blast furnace or smoker that starts lit, with the burn time written
    into its block entity (the lit state alone is reset on the first tick)."""
    import nbt as _nbt
    data = _nbt.Compound({"id": "minecraft:%s" % kind, "BurnTime": _nbt.Short(burn), "CookTime": _nbt.Short(0), "CookTimeTotal": _nbt.Short(100)})
    return h.set(x, y, z, "minecraft:%s" % kind, {"facing": facing, "lit": True}, nbt=data, over=over)


def forge_station(h, x, y, z, kind, facing):
    """One workstation of the smithy: an anvil, a grindstone (standing on the floor), a smithing table, a lit blast furnace, a water cauldron or a chest. All stand on a solid
    block (an anvil over air would fall); returns False when the cell is taken."""
    if not h.can_set(x, y, z):
        return False
    if kind == "anvil":
        h.set(x, y, z, "minecraft:anvil", {"facing": facing})
    elif kind == "grindstone":
        h.set(x, y, z, "minecraft:grindstone", {"face": "floor", "facing": facing})
    elif kind == "smithing":
        h.set(x, y, z, "minecraft:smithing_table")
    elif kind == "furnace":
        lit_furnace(h, x, y, z, "blast_furnace", facing)
    elif kind == "water":
        h.set(x, y, z, "minecraft:water_cauldron", {"level": "3"})
    elif kind == "chest":
        h.loot_chest(x, y, z, facing)
    else:
        raise ValueError(kind)
    return True


def table_dishes(h, x0, x1, z0, z1):
    """What is on a dwarf's table: candles in a clay-brown row, cakes, potted mushrooms, a standing lantern every sixth cell. Every item sits on a top slab (a sturdy face) and the
    table stays three cells from any lava (the generator proves it)."""
    items = ["minecraft:cake", "minecraft:potted_brown_mushroom", "minecraft:cake", "minecraft:potted_red_mushroom"]
    k = 0
    for z in range(z0 + 1, z1, 2):
        for x in range(x0, x1 + 1):
            if (x + z) % 3 == 0:
                continue
            if h.rng.random() < 0.5:
                h.try_set(x, GROUND + 1, z, items[k % len(items)])
                k += 1
        for x in (x0, x1):
            if h.rng.random() < 0.5:
                D.candles_at(h, x, GROUND + 1, z + 1, h.rng.randint(2, 4), h.rng.choice(("orange", "brown", "white", "yellow")))
    for z in range(z0 + 3, z1 - 1, 5):
        h.try_set((x0 + x1) // 2, GROUND + 1, z, L.LANTERN, {"hanging": "false"})


# ===================================================================================================================
# HALL: the hall of pillars (3 x 5 modules)
# ===================================================================================================================
@T.room("hall", "hall_of_pillars", faction="construct",
        cuts=[{"name": "west_half", "x": (0, 14), "y": (8, 26), "z": None}],
        views=[("gallery", (4.5, 10.62, 3.5), (3.0, 11.2, 40.0)), ("nave", (13.5, 10.62, 41.0), (13.5, 12.0, 4.0)), ("anvil", (8.5, 10.62, 19.5), (13.5, 11.5, 22.5)),
               ("window", (5.5, 10.62, 12.0), (2.5, 11.5, 9.5))])
def hall_of_pillars(path):
    """Why: the great hall is what a dwarven hold is remembered for, and it is built to be fought in: 27 x 45 blocks and sixteen high, two rows of rune pillars (2 x 2 shafts
    with a ring of lit copper bulbs at two heights: the runes of the founding) holding a grid of copper beams, a nave seven wide between them with two long tables laid out for a
    feast that never came, and a gallery four wide down each side with eight windows onto lava (a sheet of lava one block thick in the wall behind glass: it lights the hold the
    colour of its forge and nobody can reach it). At the crossing in the middle stands the Great Anvil on its platform, ringed with blast furnaces, grindstones and smithing
    tables; four doors (north, south, west, east) meet at it. The galleries and the cross aisles are the three-wide routes; the tables and the platform are cover, the pillars are
    corners to fight round, and the lava is the thing nobody wants to be knocked towards. No wood stands within three cells of lava (the generator proves it)."""
    h = Hall(path, 3, 5, 16, wear=0.08)
    top = h.top
    h.door("n", 13)
    h.door("s", 13)
    h.door("w", 22)
    h.door("e", 22)
    # the routes that must stay open: both galleries, the links between them at the two ends, the sides of the crossing
    h.route(3, 2, 5, 42, label="west gallery")
    h.route(21, 2, 23, 42, label="east gallery")
    h.route(3, 3, 23, 6, label="north link")
    h.route(3, 38, 23, 41, label="south link")
    h.route(6, 18, 9, 26, label="west crossing")
    h.route(17, 18, 20, 26, label="east crossing")
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    # 1. the floor: laid stone with a copper border along the nave and a medallion at the crossing
    for z in range(2, 43):
        for x in (10, 16):
            if h.get(x, FLOOR, z) is not None and (z % 4 != 0):
                h.floor_set(x, z, L.POLISHED)
    for z in range(7, 38):
        for x in (6, 9, 17, 20):
            if (z // 3) % 2 == 0 and h.get(x, FLOOR, z) is not None:
                h.floor_set(x, z, L.CRACKED_TILE)
    D.floor_medallion(h, 13, 22, r=7, ring=L.POLISHED, core=L.CHISELED, field=L.TILE)
    # 2. the pillars and the beams they hold
    for x in (7, 18):
        for z in (8, 15, 28, 35):
            rune_pillar(h, x, z)
    D.beams(h, "x", [8, 9, 15, 16, 28, 29, 35, 36], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.beams(h, "z", [7, 8, 18, 19], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    # 3. the lava windows: four in each long wall (z 8..10, 13..15, 29..31, 34..36), a relief pilaster between them
    for wl in (ww, we):
        for lat in (8, 13, 29, 34):
            lava_window(wl, lat)
        for lat in (11, 16, 27, 32):
            for lt in (lat, lat + 1):
                for y in range(GROUND, top):
                    wl.try_set(lt, y, 0, L.POLISHED if y not in (GROUND + 2, GROUND + 6) else L.CHISELED)
                wl.try_set(lt, top, 0, L.POLISHED_STAIRS, {"facing": wl.out, "half": "top"})
    # 4. the Great Anvil on its platform at the crossing, the smithy round it
    D.dais(h, 10, 19, 16, 25, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    great_anvil(h, 13, 22, "z")
    for (x, z, kind, facing) in ((11, 20, "anvil", "east"), (15, 20, "anvil", "west"), (11, 24, "grindstone", "east"), (15, 24, "grindstone", "west"),
                                 (11, 22, "furnace", "west"), (15, 22, "furnace", "east"), (12, 20, "smithing", "south"), (14, 24, "smithing", "north")):
        forge_station(h, x, GROUND + 1, z, kind, facing)
    # 5. the long tables in the two halves of the nave, the benches and the feast
    for (z0, z1) in ((8, 17), (27, 36)):
        D.table(h.frame(14, GROUND, z0, "south"), z1 - z0 + 1, 3)
        D.bench(h.frame(11, GROUND, z0 + 1, "south"), z1 - z0 - 1)
        D.bench(h.frame(15, GROUND, z0 + 1, "south"), z1 - z0 - 1)
        table_dishes(h, 12, 14, z0, z1)
    h.marker(13, GROUND + 1, 12, "furniture", **{"as": "table"})
    h.marker(13, GROUND + 1, 31, "furniture", **{"as": "table"})
    # 6. the light: lanterns on chains over the nave and the galleries
    for z in (11, 15, 31, 35):
        for x in (11, 15):
            lamp(h, x, z, 9)
    for z in (4, 14, 30, 40):
        for x in (4, 22):
            lamp(h, x, z, 8)
    # 7. statues of the founders at the two ends of the nave, the doors, the cornice
    for (x, z, f) in ((10, 2, "south"), (16, 2, "south"), (10, 42, "north"), (16, 42, "north")):
        D.statue(h, x, z, f, kit="iron")
    for door in (("n", 13), ("s", 13), ("w", 22), ("e", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    D.wall_band(h, GROUND + 7, block=L.POLISHED, accent=L.CHISELED, every=4)
    h.faction("construct", 13, 15)
    D.auto_light(h)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ===================================================================================================================
# HALL: the chasm forge (4 x 4 modules, three levels)
# ===================================================================================================================
#: the chasm's x and z range (the floor row is removed there), the arms and the Forgeheart's, and the row of the chasm's floor block (the room's floor is row 8)
CH0, CH1 = 10, 25
ARM0, ARM1 = 15, 20
PIT_ROW = 2
TRAPDOOR_DECK = {"facing": "north", "half": "top", "open": False, "powered": False, "waterlogged": False}


def rot35(x, z):
    """The point opposite (x, z) in a 36 x 36 room: the chasm forge turns like a pinwheel, so what is built at one corner is built again at the other end."""
    return 35 - x, 35 - z


def forgeheart(h, x0, z0, size=6, y0=3, y1=30):
    """The Forgeheart: a tower of lava (the inner `size - 2` square, from row y0 to y1) in a skin of glass with four chiselled corner posts and a copper band every sixth row,
    a polished cap over the lava. Every lava cell has glass, chiselled copper or the chasm's floor on all four sides and below, so it cannot flow (the generator proves it).
    Returns the number of lava cells."""
    x1, z1 = x0 + size - 1, z0 + size - 1
    n = 0
    for y in range(y0, y1 + 1):
        band = (y - y0) % 6 == 5
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                edge = x in (x0, x1) or z in (z0, z1)
                corner = x in (x0, x1) and z in (z0, z1)
                if edge:
                    h.set(x, y, z, L.CHISELED if (corner or band) else GLASS)
                else:
                    h.set(x, y, z, LAVA, {"level": "0"})
                    n += 1
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            edge = x in (x0, x1) or z in (z0, z1)
            h.set(x, y1 + 1, z, L.CHISELED if edge else L.POLISHED)
    return n


def pit_wall_dressing(h, rng):
    """The four walls of the chasm (the stone under the ring floor, rows 3..7) become a forge's wall: a band of copper at row 5, iron, copper, gold and coal ore in the stone, and
    blast furnaces (lit) set into it facing the pit. Why: the pit is where the hold smelted, and its walls should say so."""
    ores = ["minecraft:iron_ore", "minecraft:copper_ore", "minecraft:gold_ore", "minecraft:coal_ore", "minecraft:raw_iron_block"]
    for t in range(CH0, CH1 + 1):
        for (x, z, facing) in ((CH0 - 1, t, "east"), (CH1 + 1, t, "west"), (t, CH0 - 1, "south"), (t, CH1 + 1, "north")):
            for y in range(3, FLOOR):
                c = h.get(x, y, z)
                if c is None or c.block != L.ROCK:
                    continue
                if y == 5:
                    h.set(x, y, z, L.POLISHED if (x + z) % 4 else L.CHISELED, over=True)
                elif rng.random() < 0.12:
                    h.set(x, y, z, rng.choice(ores), over=True)
    for (x, z, facing) in ((CH0 - 1, 12, "east"), (CH0 - 1, 23, "east"), (CH1 + 1, 12, "west"), (CH1 + 1, 23, "west"),
                           (12, CH0 - 1, "south"), (23, CH0 - 1, "south"), (12, CH1 + 1, "north"), (23, CH1 + 1, "north")):
        c = h.get(x, 4, z)
        if c is not None and c.block in (L.ROCK, "minecraft:iron_ore", "minecraft:copper_ore", "minecraft:gold_ore", "minecraft:coal_ore", "minecraft:raw_iron_block"):
            lit_furnace(h, x, 4, z, "blast_furnace", facing, over=True)


@T.room("hall", "chasm_forge", faction="elemental",
        cuts=[{"name": "west_half", "x": (0, 18), "y": (0, 34), "z": None}, {"name": "south_half", "x": None, "y": (0, 34), "z": (18, 35), "rot": 0}],
        views=[("door0", (4.5, 10.62, 13.5), (18.0, 11.0, 18.0)), ("door3", (31.5, 10.62, 22.5), (17.0, 11.0, 17.0)),
               ("pit", (6.5, 10.62, 12.5), (22.0, 4.0, 12.0)), ("gallery", (30.5, 19.62, 3.5), (12.0, 17.0, 22.0)), ("arm", (17.5, 10.62, 9.5), (17.5, 14.0, 17.0)),
               ("below", (13.5, 4.62, 21.5), (17.5, 6.0, 12.5))])
def chasm_forge(path):
    """Why: the hold's deepest forge is a chasm: the floor of the hall is cut away in a square sixteen blocks across and six deep, the Forgeheart (a tower of lava in glass and
    copper, twenty-eight rows high) stands at its heart, and four bridges of iron grate reach out to it from the ring floor round the edge. The pit is the lower forge (blast
    furnaces in its walls, anvils, a smelter's cauldrons, ore piles, a chest) and the way down is a stair at two of its corners; two galleries (a pinwheel: one along the north
    wall in the east, one along the south wall in the west) climb nine blocks above the ring on a flight each, with a track of mine-cart rails, a rail and an arcade under them, so
    the room has three levels to fight on: the pit, the ring and the gallery. The doors are the four on the west and east walls, the north and south walls belong to the galleries.
    The danger is the edge: the rim has a wall all round it and a gap where the bridges and the stairs begin, and whoever is knocked over lands six blocks down among the forges, not
    in the lava (the only lava is the Forgeheart, behind glass)."""
    h = Hall(path, 4, 4, 26, wear=0.08)
    top = h.top
    rng = h.rng
    for (side, off) in (("w", 13), ("w", 22), ("e", 13), ("e", 22)):
        h.door(side, off)
    h.route(2, 6, 33, 8, label="north lane")
    h.route(2, 27, 33, 29, label="south lane")
    h.route(6, 6, 8, 29, label="west lane")
    h.route(27, 6, 29, 29, label="east lane")
    # 1. the stone under the floor, the chasm cut out of it, the chasm's floor
    for z in range(h.D):
        for x in range(h.W):
            if CH0 <= x <= CH1 and CH0 <= z <= CH1:
                h.set(x, PIT_ROW, z, L.CRACKED_TILE if rng.random() < 0.15 else L.TILE)
                h.carve(x, FLOOR, z)
            else:
                for y in range(PIT_ROW, FLOOR):
                    h.set(x, y, z, L.ROCK)
    # 2. the four bridges of iron grate (flush with the old floor) and the Forgeheart where they meet
    arms = set()
    for t in range(ARM0, ARM1 + 1):
        for u in list(range(CH0, ARM0)) + list(range(ARM1 + 1, CH1 + 1)):
            arms.add((t, u))
            arms.add((u, t))
    for (x, z) in sorted(arms):
        h.set(x, FLOOR, z, "minecraft:iron_trapdoor", TRAPDOOR_DECK)
    forgeheart(h, ARM0, ARM0)
    # 3. the rim: a wall all round the chasm with openings for the bridges and the two stairs down, posts with a lantern at the corners and at every bridge head
    gaps = {("w", z) for z in range(ARM0, ARM1 + 1)} | {("e", z) for z in range(ARM0, ARM1 + 1)} | {("n", x) for x in range(ARM0, ARM1 + 1)} | {("s", x) for x in range(ARM0, ARM1 + 1)}
    gaps |= {("w", z) for z in (11, 12, 13)} | {("e", z) for z in (22, 23, 24)}
    for t in range(CH0, CH1 + 1):
        for (side, x, z) in (("w", CH0 - 1, t), ("e", CH1 + 1, t), ("n", t, CH0 - 1), ("s", t, CH1 + 1)):
            if (side, t) not in gaps:
                h.set(x, GROUND, z, L.POLISHED_WALL)
    for (x, z) in ((CH0 - 1, CH0 - 1), (CH1 + 1, CH0 - 1), (CH0 - 1, CH1 + 1), (CH1 + 1, CH1 + 1)):
        for y in range(GROUND, GROUND + 3):
            h.set(x, y, z, L.POLISHED, over=True)
        h.set(x, GROUND + 3, z, L.CHISELED, over=True)
        h.set(x, GROUND + 4, z, L.LANTERN, {"hanging": False})
    for side, (xa, za), (xb, zb) in (("w", (CH0 - 1, ARM0 - 1), (CH0 - 1, ARM1 + 1)), ("e", (CH1 + 1, ARM0 - 1), (CH1 + 1, ARM1 + 1)),
                                   ("n", (ARM0 - 1, CH0 - 1), (ARM1 + 1, CH0 - 1)), ("s", (ARM0 - 1, CH1 + 1), (ARM1 + 1, CH1 + 1))):
        for (x, z) in ((xa, za), (xb, zb)):
            for y in range(GROUND, GROUND + 2):
                h.set(x, y, z, L.POLISHED, over=True)
            h.set(x, GROUND + 2, z, L.CHISELED, over=True)
            h.set(x, GROUND + 3, z, L.LANTERN, {"hanging": False})
    # the bridges' own rails (their outer lanes), so the walk is four lanes wide
    for t in range(CH0, CH1 + 1):
        if ARM0 <= t <= ARM1:
            continue
        for e in (ARM0, ARM1):
            h.set(e, GROUND, t, L.POLISHED_WALL)
            h.set(t, GROUND, e, L.POLISHED_WALL)
    # 4. the stairs down into the pit (a pinwheel: north-west from the west rim, south-east from the east rim)
    D.stair_flight(h, 15, 13, "west", 6, width=3, base=3, stairs=L.POLISHED_STAIRS)
    D.stair_flight(h, 20, 22, "east", 6, width=3, base=3, stairs=L.POLISHED_STAIRS)
    # 5. the galleries: a flight of nine steps, a deck of stone at row 17, a rail, and an arcade of columns under the front edge; the south gallery is the north one turned
    for (turn) in (False, True):
        def at(x, z):
            return rot35(x, z) if turn else (x, z)
        sx, sz = at(10, 2)
        D.stair_flight(h, sx, sz, "west" if turn else "east", 9, width=3, base=GROUND, stairs=L.POLISHED_STAIRS, rail_r=2, rail=L.POLISHED_WALL)
        for z in range(2, 6):
            for x in range(19, 34):
                px, pz = at(x, z)
                h.set(px, 17, pz, L.TILE if rng.random() > 0.12 else L.CRACKED_TILE)
        for x in range(19, 34):
            px, pz = at(x, 5)
            h.set(px, 18, pz, L.POLISHED_WALL)
            h.set(px, 16, pz, L.POLISHED)
        for x in (22, 27, 32):
            px, pz = at(x, 5)
            for y in range(GROUND, 16):
                h.set(px, y, pz, L.POLISHED if y not in (GROUND, 15) else L.CHISELED)
    # the gallery's own light: a lantern on every fourth post of the rail and hanging under the deck in the arcade, and one on the flight's rail
    for (turn) in (False, True):
        def at(x, z):
            return rot35(x, z) if turn else (x, z)
        for x in range(20, 34, 4):
            px, pz = at(x, 5)
            h.set(px, 19, pz, L.LANTERN, {"hanging": False})
        for x in range(21, 34, 4):
            px, pz = at(x, 3)
            h.set(px, 16, pz, L.LANTERN, {"hanging": True})
        for k in (1, 5):
            px, pz = at(10 + k, 4)
            h.set(px, GROUND + k + 2, pz, L.LANTERN, {"hanging": False})
    # 6. the track of the mine carts along each gallery, a chest at its end
    for (turn) in (False, True):
        def at(x, z):
            return rot35(x, z) if turn else (x, z)
        for x in range(21, 33):
            px, pz = at(x, 3)
            if x in (24, 29):
                h.set(px, 18, pz, "minecraft:powered_rail", {"shape": "east_west", "powered": False, "waterlogged": False})
            else:
                h.set(px, 18, pz, "minecraft:rail", {"shape": "east_west", "waterlogged": False})
        px, pz = at(33, 3)
        h.loot_chest(px, 18, pz, "west" if not turn else "east")
        for x in (20, 25, 31):
            px, pz = at(x, 2)
            h.set(px, 18, pz, "minecraft:barrel", {"facing": "up", "open": False})
    # 7. the forge in the pit: a smithy in the north-east and, turned, in the south-west corner (anvils, grindstones, a smithing table, two cauldrons of molten metal, a chest),
    #    stacks of ore under the bridges
    flip = {"north": "south", "south": "north", "east": "west", "west": "east"}
    for turn in (False, True):
        def at(x, z):
            return rot35(x, z) if turn else (x, z)
        for (x, z, kind, facing) in ((22, 10, "anvil", "south"), (23, 10, "grindstone", "south"), (24, 10, "smithing", "south"), (22, 14, "anvil", "north"),
                                     (23, 14, "grindstone", "north")):
            px, pz = at(x, z)
            forge_station(h, px, 3, pz, kind, flip[facing] if turn else facing)
        for (x, z) in ((25, 10), (25, 14)):
            px, pz = at(x, z)
            h.set(px, 3, pz, "minecraft:lava_cauldron")
        px, pz = at(25, 12)
        h.loot_chest(px, 3, pz, "east" if turn else "west")
        for (x, z, blk) in ((16, 11, "minecraft:raw_iron_block"), (19, 11, "minecraft:coal_block"), (11, 16, "minecraft:raw_copper_block"), (11, 19, "minecraft:raw_iron_block")):
            px, pz = at(x, z)
            h.set(px, 3, pz, blk)
            h.set(px, 4, pz, "minecraft:raw_gold_block" if (x + z) % 3 == 0 else blk)
    pit_wall_dressing(h, rng)
    # 8. the ceiling's cross of copper beams, the lanterns on chains, glowstone in the walls, the doors, the cornice
    D.beams(h, "x", [17, 18], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.beams(h, "z", [17, 18], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    for (x, z) in ((6, 6), (6, 29), (29, 6), (29, 29), (12, 6), (23, 29), (6, 17), (29, 18)):
        lamp(h, x, z, 12)
    K.wall_lights(h, ["minecraft:glowstone"], spacing=6, y=GROUND + 4, sides="we", door_gap=3)
    K.wall_lights(h, ["minecraft:glowstone"], spacing=6, y=GROUND + 12, sides="we", door_gap=3)
    for door in (("w", 13), ("w", 22), ("e", 13), ("e", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    D.wall_band(h, GROUND + 7, block=L.POLISHED, accent=L.CHISELED, every=4)
    h.faction("elemental", 5, 17)
    D.auto_light(h, max_lamps=24)
    D.auto_markers(h, spawns=9, patrols=3)
    return h


# ===================================================================================================================
# HALL: the mine works (3 x 3 modules, rock with tunnels cut in it)
# ===================================================================================================================
ORES = ["minecraft:iron_ore"] * 4 + ["minecraft:coal_ore"] * 4 + ["minecraft:copper_ore"] * 3 + ["minecraft:gold_ore"] * 2 + ["minecraft:redstone_ore"]
RAW = ["minecraft:raw_iron_block", "minecraft:raw_copper_block", "minecraft:raw_gold_block", "minecraft:coal_block"]


def rail(h, x, z, shape, powered=None, y=GROUND):
    """A rail (or a powered rail that is not powered: a brake) on the floor cell, if the cell is free and the floor under it is a full block."""
    if not h.free(x, y, z):
        return False
    below = h.get(x, y - 1, z)
    if below is None or not L.is_full(below.block):
        return False
    if powered is None:
        h.set(x, y, z, "minecraft:rail", {"shape": shape, "waterlogged": False})
    else:
        h.set(x, y, z, "minecraft:powered_rail", {"shape": shape, "powered": powered, "waterlogged": False})
    return True


def timber_frame(h, axis, pos, lo, hi, other0, other1):
    """A mine timber across a tunnel: the two wall cells beside it (rock) become logs, the ceiling cell row over it planks, and a lantern hangs from the beam. `axis` 'z' is a
    frame across a tunnel that runs along z, at z = pos, between the x of the wall posts `lo` and `hi`; the beam is at row GROUND + 4."""
    for e in (lo, hi):
        for y in range(GROUND, GROUND + 4):
            x, z = (e, pos) if axis == "z" else (pos, e)
            c = h.get(x, y, z)
            if c is None or not L.is_full(c.block):
                return False
    for e in (lo, hi):
        for y in range(GROUND, GROUND + 4):
            x, z = (e, pos) if axis == "z" else (pos, e)
            h.set(x, y, z, L.LOG, {"axis": "y"}, over=True)
    for e in range(lo, hi + 1):
        x, z = (e, pos) if axis == "z" else (pos, e)
        c = h.get(x, GROUND + 4, z)
        if c is not None:
            h.set(x, GROUND + 4, z, L.PLANKS, over=True)
    mid = (lo + hi) // 2
    x, z = (mid, pos) if axis == "z" else (pos, mid)
    if h.free(x, GROUND + 3, z):
        h.set(x, GROUND + 3, z, L.LANTERN, {"hanging": True})
    return True


@T.room("hall", "mine_works", faction="goblin", view_h=12, plan_h=3,
        cuts=[{"name": "south_half", "x": None, "y": (8, 21), "z": (13, 26), "rot": 0}],
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 10.5, 24.0)), ("cavern", (13.5, 10.62, 7.5), (13.5, 13.0, 15.0)), ("west", (3.5, 10.62, 13.5), (20.0, 10.5, 13.5)),
               ("spring", (10.5, 10.62, 20.0), (4.5, 10.0, 20.5))])
def mine_works(path):
    """Why: a dwarven hold lives on what it digs, and the hall of its mines is the place the hold's own people never cleaned out. The room is solid rock with tunnels cut in it: a
    cross of three-wide tunnels from the four doors (mine timbers set into the rock every few blocks, a lantern on every beam, rails with a brake every so often), a round cavern at the
    crossing (a column of ore rising into the dome, a loop of rails round it), and four pockets off side tunnels in the corners: a stockpile of raw ore with a hopper and a chest, a
    collapse where the roof came down, a spring that fills a pool under dripstone, and the foreman's tool store with an anvil and a chest. It is the room of ambush and of narrow
    fights, the opposite of the great hall: the goblin raiders from the lower deeps have been digging here, so the native creatures are goblins."""
    h = Hall(path, 3, 3, 12, wear=0.0)
    top = h.top
    rng = h.rng
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    # 1. the open space as a height per column: rows GROUND .. GROUND + H - 1 stay air, the rest of the column is rock
    H = {}

    def opn(x, z, hh):
        if h.inside(x, z) and 2 <= x <= 24 and 2 <= z <= 24:
            H[(x, z)] = max(H.get((x, z), 0), hh)
    for t in range(2, 25):
        for u in (12, 13, 14):
            opn(u, t, 4)
            opn(t, u, 4)
    for (x0, x1, z0, z1) in ((9, 11, 5, 7), (15, 17, 5, 7), (9, 11, 19, 21), (15, 17, 19, 21)):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                opn(x, z, 4)
    pockets = ((3, 8, 3, 8), (18, 23, 3, 8), (3, 8, 18, 23), (18, 23, 18, 23))
    for (x0, x1, z0, z1) in pockets:
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                opn(x, z, 4 + (1 if (x + z) % 5 == 0 else 0))
    for z in range(2, 25):
        for x in range(2, 25):
            d = math.hypot(x - 13, z - 13)
            if d <= 6.3:
                opn(x, z, 9 - int(d * 0.75))
    # roughness: a tenth of the rock cells that touch the open space on a side give way (not near the doors, not at the outer ring, not at the pockets' own walls)
    base = dict(H)
    for (x, z), hh in sorted(base.items()):
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in base or not (3 <= n[0] <= 23 and 3 <= n[1] <= 23):
                continue
            if any(abs(n[0] - 13) <= 1 and (n[1] <= 7 or n[1] >= 19) or abs(n[1] - 13) <= 1 and (n[0] <= 7 or n[0] >= 19) for _ in (0,)):
                continue
            if rng.random() < 0.10:
                H[n] = max(H.get(n, 0), 3 + (1 if rng.random() < 0.5 else 0))
    # 2. the rock: a column of stone over every cell, deepslate-coloured with a tenth of the plain cobbled and ore where the cell touches the open space
    def exposed(x, y, z):
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
            n = (x + dx, z + dz)
            hh = H.get(n)
            if hh is not None and GROUND + hh > y + dy and (y + dy) >= GROUND:
                return True
        return False
    for z in range(2, 25):
        for x in range(2, 25):
            for y in range(GROUND + H.get((x, z), 0), top + 1):
                r = rng.random()
                if exposed(x, y, z) and r < 0.085:
                    h.set(x, y, z, rng.choice(ORES))
                elif r < 0.20:
                    h.set(x, y, z, L.COBBLED)
                elif r < 0.30:
                    h.set(x, y, z, "minecraft:tuff")
                else:
                    h.set(x, y, z, L.ROCK)
    # 3. the floor of the works: coarse dirt, gravel and plain stone laid by the miners in patches
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in H and h.get(x, FLOOR, z) is not None:
                r = rng.random()
                if r < 0.18:
                    h.floor_set(x, z, "minecraft:dirt")
                elif r < 0.30:
                    h.floor_set(x, z, "minecraft:packed_mud")
                elif r < 0.40:
                    h.floor_set(x, z, L.COBBLED)
    # 4. the Lode: a column of ore in the middle of the cavern, a loop of rails round it
    for (x, z) in K.disc(13, 13, 1.5):
        for y in range(GROUND, GROUND + 9 - int(math.hypot(x - 13, z - 13) * 0.75)):
            h.set(x, y, z, rng.choice(["minecraft:raw_gold_block", "minecraft:gold_ore", "minecraft:raw_iron_block", "minecraft:raw_copper_block", "minecraft:gold_ore"]), over=True) \
                if h.get(x, y, z) is None else None
    for x in range(10, 17):
        rail(h, x, 9, "east_west")
        rail(h, x, 17, "east_west")
    for z in range(10, 17):
        rail(h, 9, z, "north_south")
        rail(h, 17, z, "north_south")
    rail(h, 9, 9, "south_east")
    rail(h, 17, 9, "south_west")
    rail(h, 9, 17, "north_east")
    rail(h, 17, 17, "north_west")
    # the stubs of the four tunnels run to the loop, a brake (an unpowered powered rail) every fifth cell
    for z in range(3, 9):
        rail(h, 13, z, "north_south", powered=False if z in (4,) else None)
    for z in range(18, 24):
        rail(h, 13, z, "north_south", powered=False if z in (22,) else None)
    for x in range(3, 9):
        rail(h, x, 13, "east_west", powered=False if x in (4,) else None)
    for x in range(18, 24):
        rail(h, x, 13, "east_west", powered=False if x in (22,) else None)
    for (x, z) in ((13, 6), (13, 20)):                      # two carts standing on the rails
        h.mt.entity(x, GROUND, z, {"id": "minecraft:minecart", "Invulnerable": 1, "Silent": 1, "NoGravity": 0})
    # 5. mine timbers across the tunnels, every fourth cell outside the cavern
    for zz in (4, 6, 20, 22):
        timber_frame(h, "z", zz, 11, 15, 0, 0)
    for xx in (4, 6, 20, 22):
        timber_frame(h, "x", xx, 11, 15, 0, 0)
    # 6. the four pockets
    # north-east: the stockpile
    for z in range(3, 9):
        for k, blk in enumerate(("minecraft:raw_iron_block", "minecraft:raw_copper_block", "minecraft:raw_gold_block")):
            if (z + k) % 2 == 0 or k == 0:
                h.try_set(23, GROUND, z, rng.choice(RAW))
                if rng.random() < 0.6:
                    h.try_set(23, GROUND + 1, z, rng.choice(RAW))
    h.try_set(21, GROUND, 4, "minecraft:chest", {"facing": "south", "type": "single", "waterlogged": False})
    h.try_set(21, GROUND + 1, 4, "minecraft:hopper", {"facing": "down", "enabled": True})
    for (x, z) in ((19, 3), (20, 3), (19, 8), (22, 8)):
        h.try_set(x, GROUND, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.loot_chest(22, GROUND, 6, "west")
    # north-west: the collapse (the chest first, so the rubble piles round it)
    h.loot_chest(3, GROUND, 8, "east")
    for (x, z) in K.disc(5, 5, 2.4):
        for y in range(GROUND, GROUND + 1 + (1 if math.hypot(x - 5, z - 5) < 1.6 else 0) + (1 if rng.random() < 0.3 else 0)):
            if (x, z) != (7, 5):
                h.try_set(x, y, z, rng.choice([L.COBBLED, "minecraft:tuff", L.COBBLED, L.ROCK]))
    # south-west: the spring
    K.sealed_pool(h, [(x, z) for z in (19, 20, 21) for x in (4, 5, 6)], depth=1)
    for (x, z) in ((4, 19), (6, 21), (5, 20)):
        for k in range(2):
            h.try_set(x, GROUND + 3 - k, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": "tip" if k == 1 else "base", "waterlogged": False}) \
                if h.get(x, GROUND + 4, z) is not None else None
    # south-east: the foreman's tool store
    for (x, z, kind, facing) in ((22, 19, "anvil", "west"), (22, 20, "grindstone", "west"), (22, 21, "smithing", "west"), (22, 22, "furnace", "west"), (19, 19, "furnace", "east")):
        forge_station(h, x, GROUND, z, kind, facing)
    h.loot_chest(20, GROUND, 22, "north")
    h.faction("goblin", 13, 9)
    # 7. light (timbers carry lanterns already; the cavern gets a ring of them standing on the ore) and the markers
    for (x, z) in ((10, 13), (16, 13), (13, 10), (13, 16)):
        if h.free(x, GROUND + 3, z) and h.get(x, GROUND + 4, z) is not None:
            h.set(x, GROUND + 3, z, L.LANTERN, {"hanging": True})
    for t in list(range(3, 24, 3)):                       # a lantern on the ceiling of every tunnel every third cell (not inside the cavern), whatever the timbers did
        for (x, z) in ((13, t), (t, 13)):
            if math.hypot(x - 13, z - 13) > 7.0 and h.free(x, GROUND + 3, z) and h.get(x, GROUND + 4, z) is not None and L.is_full(h.get(x, GROUND + 4, z).block):
                h.set(x, GROUND + 3, z, L.LANTERN, {"hanging": True})
    for (x, z) in ((5, 20), (21, 5), (5, 5), (21, 21), (3, 3), (23, 23), (3, 23), (23, 3), (10, 6), (16, 6), (10, 20), (16, 20)):
        if h.free(x, GROUND + 3, z) and h.get(x, GROUND + 4, z) is not None and L.is_full(h.get(x, GROUND + 4, z).block):
            h.set(x, GROUND + 3, z, L.LANTERN, {"hanging": True})
    D.auto_light(h, max_lamps=24)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ===================================================================================================================
# SET PIECE: the vault of kings (3 x 5 modules)
# ===================================================================================================================
GOLD = "minecraft:gold_block"
RAW_GOLD = "minecraft:raw_gold_block"


def sarcophagus(h, x, z, along="z", length=3, head_gold=True):
    """A king's stone coffin: a 2-wide base of polished stone one block high (`length` cells along `along`), a lid of bottom slabs with a chiselled slab block at the heart and a block
    of gold at the head. Returns False when a cell is taken."""
    cells = []
    for l in range(length):
        for w in (0, 1):
            cx, cz = (x + w, z + l) if along == "z" else (x + l, z + w)
            cells.append((cx, cz, l, w))
    if not all(h.can_set(cx, GROUND, cz) and h.can_set(cx, GROUND + 1, cz) for (cx, cz, _l, _w) in cells):
        return False
    for (cx, cz, l, w) in cells:
        h.set(cx, GROUND, cz, L.POLISHED)
        if l == length // 2 and w == 0:
            h.set(cx, GROUND + 1, cz, L.CHISELED)
        elif head_gold and l == 0 and w == 0:
            h.set(cx, GROUND + 1, cz, GOLD)
        else:
            h.set(cx, GROUND + 1, cz, L.POLISHED_SLAB, {"type": "bottom"})
    return True


@T.room("set", "vault_of_kings", faction="construct",
        cuts=[{"name": "west_half", "x": (0, 14), "y": (0, 24), "z": None}],
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 10.5, 40.0)), ("nave", (13.5, 10.62, 14.5), (13.5, 11.5, 40.0)), ("tomb", (13.5, 10.62, 31.5), (13.5, 12.0, 41.0)),
               ("coffins", (6.5, 10.62, 14.5), (4.0, 10.5, 30.0))])
def vault_of_kings(path):
    """Why: the last room a party of a dwarven hold is told about is the one under the mountain where its kings lie, and the set piece of the theme is that vault. From the north door
    an antechamber of armoured statues in lit niches leads to the Gate (a great portal in a partition, its portcullis drawn up above it); behind the Gate is the nave, forty feet of it:
    the middle five blocks are a grate of iron laid over a channel of lava, so the way to the kings is walked over fire, with a row of pillars either side, and in the aisles beyond
    them the coffins of eight kings in niches (a block of gold at each head); at the far end three steps up to the tomb of the first king, a sarcophagus of copper and gold with
    braziers at the corners, chests either side of it and a heap of raw gold. Doors north, west and east (the side doors open on the nave's cross aisle). No wood stands within three
    blocks of the lava (the generator proves it); the danger is the kings' guard (living armour and golems that wake when the grate is crossed) and the lava under the feet."""
    h = Hall(path, 3, 5, 14, wear=0.06)
    top = h.top
    rng = h.rng
    h.door("n", 13)
    h.door("w", 22)
    h.door("e", 22)
    h.route(11, 7, 15, 13, label="the way through the gate")
    h.route(2, 21, 10, 23, label="west cross aisle")
    h.route(16, 21, 24, 23, label="east cross aisle")
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    # 1. the stone under the floor with the lava channel cut into it; the grate of iron over the channel
    for z in range(h.D):
        for x in range(h.W):
            in_channel = 11 <= x <= 15 and 14 <= z <= 32
            for y in range(3, FLOOR):
                if in_channel and y >= 5:
                    h.set(x, y, z, LAVA, {"level": "0"})
                else:
                    h.set(x, y, z, L.ROCK)
    for z in range(13, 34):
        for x in range(11, 16):
            h.carve(x, FLOOR, z)
            h.set(x, FLOOR, z, "minecraft:iron_trapdoor", TRAPDOOR_DECK)
    # 2. the floor: gold in the stone along the way to the tomb, copper borders, the cross aisle's inlay
    for z in range(2, 13):
        for x in (12, 14):
            if h.get(x, FLOOR, z) is not None and z % 2 == 0:
                h.floor_set(x, z, GOLD if z % 4 == 0 else L.CHISELED)
    for z in range(13, 43):
        for x in (10, 16):
            if h.get(x, FLOOR, z) is not None:
                h.floor_set(x, z, L.POLISHED)
    for z in (21, 23):
        for x in list(range(2, 10)) + list(range(17, 25)):
            if (x + z) % 2 == 0:
                h.floor_set(x, z, L.CRACKED_TILE)
    # 3. the Gate: a partition across the hall at z 10..11 with a five-wide portal (x 11..15), a pointed arch of copper, chiselled keystone, the portcullis raised above it
    for z in (10, 11):
        for x in range(2, 25):
            if 11 <= x <= 15 and GROUND <= GROUND + 6:
                continue
            for y in range(GROUND, top + 1):
                h.set(x, y, z, L.BRICK if (x + y) % 7 else L.CRACKED_BRICK)
    for z in (10, 11):
        for x in range(11, 16):
            for y in range(GROUND + 7, top + 1):
                h.set(x, y, z, L.POLISHED if y < GROUND + 8 else L.BRICK)
    for z in (10, 11):
        for y in range(GROUND, GROUND + 7):
            for x in (10, 16):
                h.set(x, y, z, L.POLISHED if y < GROUND + 6 else L.CHISELED, over=True)
        for x in range(11, 16):
            h.set(x, GROUND + 6, z, L.POLISHED_STAIRS if x in (11, 15) else L.POLISHED, {"facing": "east" if x == 11 else "west", "half": "top"} if x in (11, 15) else None, over=True) \
                if h.get(x, GROUND + 6, z) is not None else h.set(x, GROUND + 6, z, L.POLISHED_STAIRS if x in (11, 15) else L.POLISHED, {"facing": "east" if x == 11 else "west", "half": "top"} if x in (11, 15) else None)
    h.set(13, GROUND + 6, 10, L.CHISELED, over=True)
    h.set(13, GROUND + 6, 11, L.CHISELED, over=True)
    for x in range(12, 15):                                  # the portcullis, drawn up into the arch: bars on chains
        for y in (GROUND + 7, GROUND + 8):
            if h.get(x, y, 10) is not None:
                h.set(x, y, 10, L.IRON_BARS, over=True)
    # 4. the antechamber: four armour niches in the long walls, two statues at the gate, braziers, a copper wall band
    for (wl, lats) in ((ww, (4, 8)), (we, (4, 8))):
        for lat in lats:
            D.armor_alcove(wl, lat, kit="iron", width=3, rows=4, lantern=True)
    for (x, z, f) in ((9, 8, "south"), (17, 8, "south")):
        D.statue(h, x, z, f, kit="iron")
    for (x, z) in ((5, 3), (21, 3)):
        D.brazier(h, x, z)
    # 5. the nave: pillars either side of the grate, the coffins of eight kings in niches in the side walls, lanterns on the pillar heads
    for z in (16, 19, 25, 28):
        for x in (10, 16):
            for y in range(GROUND, top - 1):
                h.set(x, y, z, L.POLISHED if y not in (GROUND, GROUND + 5, top - 2) else L.CHISELED)
            h.set(x, top - 1, z, L.POLISHED_STAIRS, {"facing": "east" if x == 10 else "west", "half": "top"})
            h.set(x, GROUND + 6, z + 1, L.LANTERN, {"hanging": False}) if False else None
    for (wl, x0) in ((ww, 3), (we, 22)):
        for z0 in (14, 17, 25, 28):
            ok = sarcophagus(h, x0, z0, "z", 3)
            if ok:
                D.niche(wl, z0 + 1, GROUND + 2, GROUND + 5, width=3, arch=True)
                x_n, y_n, z_n = wl.cell(z0 + 1, GROUND + 5, -1)
                if h.free(x_n, y_n, z_n):
                    h.set(x_n, y_n, z_n, L.LANTERN, {"hanging": True}) if h.get(x_n, y_n + 1, z_n) is not None else None
    # 6. the tomb of the first king: three steps up, a sarcophagus of copper and gold, braziers, chests, raw gold, banners on the far wall
    D.dais(h, 8, 35, 18, 41, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    D.dais(h, 10, 36, 16, 40, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND + 1)
    for l in range(5):
        for w in range(3):
            x, z = 12 + w, 37 + (l if l < 3 else 0)
    for z in range(37, 40):
        for x in range(12, 15):
            h.set(x, GROUND + 2, z, L.POLISHED)
            h.set(x, GROUND + 3, z, L.CHISELED if (x, z) == (13, 38) else L.POLISHED_SLAB, None if (x, z) == (13, 38) else {"type": "top"})
    h.set(13, GROUND + 4, 38, GOLD)
    for x in (12, 14):
        h.try_set(x, GROUND + 4, 37, "minecraft:orange_candle", {"candles": "3", "lit": True})
        h.try_set(x, GROUND + 4, 39, "minecraft:orange_candle", {"candles": "3", "lit": True})
    for (x, z) in ((9, 36), (17, 36), (9, 40), (17, 40)):
        D.brazier(h, x, z, y=GROUND + 1) if h.can_set(x, GROUND + 1, z) else None
    h.loot_chest(5, GROUND, 38, "east")
    h.loot_chest(21, GROUND, 38, "west")
    for (x, z) in ((4, 37), (4, 39), (22, 37), (22, 39), (5, 36), (21, 36)):
        h.try_set(x, GROUND, z, RAW_GOLD if (x + z) % 2 else GOLD)
    for lat in (10, 13, 16):
        for y in (GROUND + 3, GROUND + 4, GROUND + 5):
            ws.try_set(lat, y, 0, "minecraft:red_wall_banner" if lat != 13 else "minecraft:orange_wall_banner", {"facing": ws.into})
    # 7. the dressing: door trims, cornice, a copper band, flush glowstone, the marker of the native faction
    for door in (("n", 13), ("w", 22), ("e", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    D.wall_band(h, GROUND + 8, block=L.POLISHED, accent=L.CHISELED, every=4)
    K.wall_lights(h, ["minecraft:glowstone"], spacing=6, y=GROUND + 9, sides="we", door_gap=3, lo=14, hi=36)
    h.faction("construct", 6, 20)
    D.auto_light(h, max_lamps=24)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ===================================================================================================================
# TRIAL: the anvil trial (3 x 3 modules, a Sealed Archway trial)
# ===================================================================================================================
def lava_cage(h, cx, cz, y0=GROUND, y1=GROUND + 9):
    """A forge fire in a cage: a 3 x 3 post of glass round one column of lava, chiselled copper on the four corners and the cap, the floor under it. Every lava cell has glass on
    its four sides and the floor under it (the generator proves it). Returns False when a cell is taken."""
    cells = [(cx + dx, y, cz + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in range(y0, y1 + 2)]
    if not all(h.can_set(x, y, z) for (x, y, z) in cells):
        return False
    for (x, y, z) in cells:
        dx, dz = x - cx, z - cz
        if dx == 0 and dz == 0:
            h.set(x, y, z, LAVA, {"level": "0"} ) if y <= y1 else h.set(x, y, z, L.CHISELED)
        elif y == y1 + 1:
            h.set(x, y, z, L.CHISELED if abs(dx) + abs(dz) == 2 else L.POLISHED)
        else:
            h.set(x, y, z, L.CHISELED if abs(dx) + abs(dz) == 2 else GLASS)
    return True


@T.room("trial", "anvil_trial", faction="construct", no_spawn=True, kind="trial", view_h=14,
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 11.0, 14.0)), ("anvil", (13.5, 10.62, 20.5), (13.5, 12.0, 12.0)), ("cage", (13.5, 10.62, 12.5), (4.0, 12.0, 4.0))])
def anvil_trial(path):
    """Why: the Spire's trial chamber (docs/labyrinth/e1_trial.md) is the one fight in the dungeon that is fair by construction: the keystone seals both doors when a party walks in and the
    glyphs spawn its wave, scaled by the party. The hold stages it as the forging of the guard: a square arena of nested copper rings in the floor, the Great Anvil on a platform in the
    middle with the keystone hanging over it on two chains, a forge fire in a glass cage in each corner (the arena's light and its heat: the cages cannot be broken into), four pillars with
    lanterns for cover, eight glyphs where the wave appears, and the reward in an alcove in the west wall. The wave is living armour (the anvil wakes what it made), two rebels with axes and
    two slag-cubes from the fires. The doors are north and south; the fight is round the anvil."""
    h = Hall(path, 3, 3, 14, wear=0.06)
    rng = h.rng
    h.door("n", 13)
    h.door("s", 13)
    c = 13
    # 1. the floor: nested copper squares, a cross of chiselled copper through the middle, the platform of the anvil
    for z in range(2, 25):
        for x in range(2, 25):
            ch = max(abs(x - c), abs(z - c))
            cell = h.get(x, FLOOR, z)
            if cell is None:
                continue
            if ch in (4, 8):
                h.floor_set(x, z, L.CHISELED if (x == c or z == c) else L.POLISHED)
            elif (x == c or z == c) and ch > 8:
                h.floor_set(x, z, L.POLISHED)
    D.dais(h, 10, 10, 16, 16, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    great_anvil(h, 13, 13, "x")
    # 2. the four forge fires in their cages, the four pillars
    for (x, z) in ((4, 4), (22, 4), (4, 22), (22, 22)):
        lava_cage(h, x, z)
    for (x, z) in ((8, 8), (18, 8), (8, 18), (18, 18)):
        D.column(h, x, z, w=1, shaft=L.POLISHED, cap=L.CHISELED)
        for (dx, dz, f) in ((-1, 0, "east"), (1, 0, "west")):
            pass
    for (x, z) in ((8, 8), (18, 8), (8, 18), (18, 18)):
        sx = 1 if x < c else -1
        sz = 1 if z < c else -1
        h.try_set(x + sx * 2, GROUND, z + sz * 2, L.POLISHED)
        h.try_set(x + sx * 2, GROUND + 1, z + sz * 2, L.LANTERN, {"hanging": False})
    # 3. beams across the roof, lanterns on chains
    D.beams(h, "x", [8, 18], y=h.top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    for (x, z) in ((9, 13), (17, 13), (13, 9), (13, 17)):
        lamp(h, x, z, 7)
    # 4. the reward: a niche in the west wall with the chest and candles, a statue in the east wall
    ww, we = h.wall("w"), h.wall("e")
    D.niche(ww, 13, GROUND, GROUND + 4, width=5, arch=True)
    h.loot_chest(1, GROUND, 13, "east")
    for z in (11, 15):
        D.candles_at(h, 1, GROUND, z, 4, "orange")
    h.set(1, GROUND + 4, 13, L.LANTERN, {"hanging": True})
    D.armor_alcove(we, 13, kit="diamond", width=3, rows=4, lantern=True)
    for door in (("n", 13), ("s", 13)):
        D.door_trim(h, *door)
    D.cornice(h)
    D.wall_band(h, GROUND + 7, block=L.POLISHED, accent=L.CHISELED, every=4)
    for lat in (6, 20):
        for wl in (h.wall("n"), h.wall("s")):
            wl.try_set(lat, GROUND + 4, 0, "minecraft:orange_wall_banner", {"facing": wl.into})
    glyphs = [(8, 5), (18, 5), (8, 21), (18, 21), (5, 10), (21, 10), (5, 16), (21, 16)]
    h.faction("construct", 20, 13)
    for (x, z) in ((13, 6), (13, 20), (6, 13)):
        h.marker(x, GROUND, z, "patrol")
    D.auto_light(h, max_lamps=8)
    h.explicit_defaults()
    R2.trial_core(h.mt, [("nusmp:living_armor", 2), ("minecraft:vindicator", 2), ("minecraft:magma_cube", 2)], rng, [("n", 13), ("s", 13)], FR + 12, glyphs)
    return h.mt


# ===================================================================================================================
# TREASURE: the hoard nook (1 x 2 modules, one door)
# ===================================================================================================================
@T.room("treasure", "hoard_nook", faction="construct", view_h=8, few_spawns=True,
        views=[("door0", (4.5, 10.62, 3.0), (4.5, 10.5, 15.0)), ("hoard", (5.5, 10.62, 7.5), (2.5, 10.0, 12.5))])
def hoard_nook(path):
    """Why: the calm room and the reward of the hold: the strongroom of a clan, a dead end behind one door with a window onto the forge's lava (it keeps the room warm and the gold
    bright), two chests in heaps of gold, raw ore and copper, a counting table with a ledger on its lectern, and a hearth with an anvil where the clan's smith sorted what came up. The
    constructs of the hold guard it (the faction marker says construct) but nothing waits in it: a lantern, a ledger and a great deal of gold. No wood stands within three blocks of the
    lava: the lectern is on the far side of the room."""
    h = Hall(path, 1, 2, 8, wear=0.06)
    rng = h.rng
    top = h.top
    h.door("n", 4)
    h.route(3, 2, 5, 8, label="entrance run")
    we = h.wall("e")
    ww = h.wall("w")
    h.marker(4, GROUND, 7, "spawn", weight=2)
    h.marker(5, GROUND, 9, "room_center")
    h.faction("construct", 5, 11)
    lava_window(we, 9, width=3, y0=GROUND + 1, y1=GROUND + 3)
    for z in range(2, 16):
        for x in (2, 6):
            if h.get(x, FLOOR, z) is not None and z % 3 == 0:
                h.floor_set(x, z, L.POLISHED)
    # the chests first, then the heaps round them
    h.loot_chest(2, GROUND, 9, "east")
    h.loot_chest(2, GROUND, 14, "east")
    mix = [GOLD, GOLD, RAW_GOLD, RAW_GOLD, "minecraft:iron_block", "minecraft:copper_block", "minecraft:raw_copper_block"]
    for (cz, hh) in ((9, 2), (14, 2), (11, 1), (12, 1)):
        for z in range(cz - 1, cz + 2):
            for x in (2, 3, 4):
                if (x, z) in ((2, 9), (2, 14)):
                    continue
                d = abs(z - cz) + (x - 2)
                if d <= hh and rng.random() < 0.9:
                    h.try_set(x, GROUND, z, rng.choice(mix))
                    if d == 0 and h.get(x, GROUND, z) is not None:
                        h.try_set(x, GROUND + 1, z, rng.choice(mix))
    # the counting table with its ledger, candles, a hearth with an anvil, a lantern on a chain
    h.set(3, GROUND, 12, "minecraft:lectern", {"facing": "east", "has_book": True, "powered": False}) if h.can_set(3, GROUND, 12) else None
    D.candles_at(h, 4, GROUND, 12, 3, "orange") if h.can_set(4, GROUND, 12) else None
    h.set(5, GROUND, 13, L.POLISHED) if h.can_set(5, GROUND, 13) else None
    h.set(5, GROUND + 1, 13, L.POLISHED_SLAB, {"type": "bottom"}) if h.can_set(5, GROUND + 1, 13) else None
    forge_station(h, 6, GROUND, 14, "furnace", "west")
    forge_station(h, 6, GROUND, 15, "anvil", "west")
    forge_station(h, 6, GROUND, 13, "grindstone", "west")
    for (x, z) in ((3, 8), (5, 14), (3, 15)):
        lamp(h, x, z, 3)
    D.door_trim(h, "n", 4)
    D.cornice(h)
    D.wall_band(h, GROUND + 5, block=L.POLISHED, accent=L.CHISELED, every=3)
    D.auto_light(h, max_lamps=6)
    return h


if __name__ == "__main__":
    sys.exit(K.run(T, sys.argv[1:]))

#!/usr/bin/env python3
"""
Generates the STAIRWELL templates of the Mega-Dungeon (1.58.0, docs/labyrinth/mega_v2_gen2.md): the rooms that join two floors.

    structure/mega/stairwell/up_a, up_b, up_c                 9 x 9 (one module): a shaft whose staircase CLIMBS 32 blocks round a newel (two full turns)
    structure/mega/stairwell/down_a, down_b, down_c           9 x 9: a shaft whose staircase DROPS 8 blocks round a pit (half a turn)
    structure/mega/stairwell/grand_up_a, grand_up_b           18 x 18: the grand stair of a boss floor, four flights round a well that climb 28 blocks
    structure/mega/stairwell/grand_down_a, grand_down_b       18 x 18: two flights that drop 8 blocks into a sunken court

    python3 tools/labyrinth/gen_mega_stairwell.py                      # build, verify and write every stairwell
    python3 tools/labyrinth/gen_mega_stairwell.py up_a grand_down_b    # only the named ones
    python3 tools/labyrinth/gen_mega_stairwell.py --check              # build and verify, write nothing
    python3 tools/labyrinth/gen_mega_stairwell.py --render DIR         # also draw each one (cutaways, a plan, the light map) into DIR (needs Pillow)

After a change run `python3 tools/labyrinth/gen_mega_pools.py`, `python3 tools/labyrinth/mega_check.py`, `python3 tools/labyrinth/mega_arch_check.py stairwell` and
`python3 tools/labyrinth/mega_catalog.py`.

Why a generator and not hand-built NBT: the same reason as every other Mega template (deterministic, reviewable in a diff, regenerated with one command when the
palette changes), plus one of its own: a staircase is arithmetic. A stair block that faces the wrong way, a step that rises two blocks, a landing a block too low,
a rail where a stair passes at head height: each is a template that LOOKS fine in a flat picture and is a wall in the game. So every stairwell is VERIFIED before it
is written (`verify`): the door is walked to the arrival cell and on to the stair exit with the walking model of the light checker (one block up or down per step,
two free cells of headroom), the exit stands at the level the geometry promises, the light never drops below 6 on a walked cell, and the survival rules of
`mega_arch_check` hold.

How a stairwell works in the game (docs/labyrinth/mega_v2.md section 1): it is a room of kind STAIRWELL with ONE door (a `nusmp:mega_door` socket on its east face,
drawing its first room from `nusmp:mega/stair_door`), an ARRIVAL marker on the platform inside the door (where a party stands when it comes to the floor) and a
STAIR_EXIT marker above the middle of the landing pad at the far end of the staircase (walking onto the pad takes the party to the linked floor). A stairwell does not
know whether it is the arrival or the forward one of its floor: the generator decides that from the sector. Stairs that climb are `up_*`, stairs that drop are
`down_*`: on the Holy road the forward stairs climb and the stairs that lead back drop, on the Abyssal road the other way round.

Geometry (template frame, x east, z south, y up; the floor row is 8 as everywhere). A small stairwell has an interior of 5 x 5 (x and z 2..6). Its staircase is laid
along the ring of the 16 cells at the edge of the interior, one stair block per cell, one block of rise per cell, so a full turn climbs 16 blocks; the 3 x 3 core is
the newel (up) or the pit (down). The platform is the east column of the ring at floor level; the door opens onto it. A stair's high side is its FACING side, so a
stair that climbs faces the way the walker goes and a stair that drops faces the way he came from. A grand stairwell has an interior of 14 x 14 with bands of 3 cells
along the walls (x or z in 3..5 and 13..15) and flights of 7 cells between the corner landings (6..12). Everything architectural uses the palette of mega_lib, so the
Region Theme restyles it; details are plain vanilla blocks.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from mega_lib import MT  # noqa: E402
import mega_arch_kit as K  # noqa: E402
import mega_arch_check as AC  # noqa: E402
import mega_shape_preview as SP  # noqa: E402

POOL_DOOR = "nusmp:mega/stair_door"
FLOOR = L.FLOOR_ROW        # 8
ROW0 = L.DOOR_ROW          # 9: the first standing row above the floor
DIR = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}
MIN_LIGHT = 6              # the least block light at head height on any cell a player can walk to (the gloom clock counts <= 4)


# =====================================================================================================================
# the ring, the styles, the small helpers
# =====================================================================================================================
def ring(lo, hi, ccw=False):
    """The 16 cells at the edge of the square [lo..hi]^2 in walking order, starting at the middle of the east column. Clockwise seen from above (x east, z south):
    down the east column, west along the south row, north up the west column, east along the north row and back. `ccw` mirrors it (z -> lo + hi - z), so the same
    code builds the staircase that turns the other way."""
    mid = (lo + hi) // 2
    cw = ([(hi, z) for z in range(mid, hi + 1)] + [(x, hi) for x in range(hi - 1, lo - 1, -1)] + [(lo, z) for z in range(hi - 1, lo - 1, -1)]
          + [(x, lo) for x in range(lo + 1, hi + 1)] + [(hi, z) for z in range(lo + 1, mid)])
    return [(x, lo + hi - z) for (x, z) in cw] if ccw else cw


def toward(a, b):
    """The compass name of the step from cell a to the adjacent cell b."""
    return DIR[(b[0] - a[0], b[1] - a[1])]


class Style:
    """The materials of one variant. Architecture is always from the palette (the Region Theme swaps those families), details are plain vanilla."""

    def __init__(self, stairs, tread, newel, trim, band, soul=False, wood=False):
        self.stairs, self.tread, self.newel, self.trim, self.band = stairs, tread, newel, trim, band
        self.soul, self.wood = soul, wood

    def lamp(self, mt, x, y, z, hanging=False, glow=None):
        """A lantern. A soul lantern burns at light 10, so in the soul variants a hidden light block (level 12) joins it in the free cell beside its flame (above a
        standing lantern, below a hanging one) unless `glow` is False: the shaft stays readable and the lamp still looks like a soul lantern."""
        mt.set(x, y, z, L.SOUL_LANTERN if self.soul else L.LANTERN, {"hanging": "true" if hanging else "false"})
        if (self.soul if glow is None else glow):
            gy = y - 1 if hanging else y + 1
            if mt.get(x, gy, z) is None:
                mt.set(x, gy, z, "minecraft:light", {"level": "12"})

    def torch(self, mt, x, y, z, facing):
        mt.set(x, y, z, "minecraft:soul_wall_torch" if self.soul else "minecraft:wall_torch", {"facing": facing})

    def banner(self, mt, x, y, z, facing, color=None):
        mt.set(x, y, z, "minecraft:%s_wall_banner" % (color or ("purple" if self.soul else "red")), {"facing": facing})


STONE = Style(L.POLISHED_STAIRS, L.POLISHED, L.POLISHED, L.COBBLED, L.CHISELED)
IRON = Style(L.TILE_STAIRS, L.TILE, L.BRICK, L.POLISHED, L.CHISELED, soul=True)
TIMBER = Style(L.PLANKS_STAIRS, L.PLANKS, L.LOG, L.STRIPPED_LOG, L.PLANKS, wood=True)


def shell(name, mx, mz, h, wear=0.10):
    """The room: floor row, two-thick walls up to `h` interior rows, a two-thick ceiling. The door is added by the caller (it needs the pool)."""
    mt = MT(name, mx, mz)
    mt.room(h, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=wear)
    return mt


def fill_mass(mt, y0, y1):
    """Solid masonry from row y0 to y1 under the whole footprint (a pit is dug INTO this)."""
    for z in range(mt.D):
        for x in range(mt.W):
            for y in range(y0, y1 + 1):
                mt.set(x, y, z, L.COBBLED if (x + y + z) % 3 else L.BRICK)


def stair(mt, st, x, y, z, facing, half="bottom"):
    mt.set(x, y, z, st.stairs, {"facing": facing, "half": half})


def bars(mt, x, y, z):
    mt.set(x, y, z, L.IRON_BARS)


def rail(mt, st, x, y, z):
    """A rail cell: fence in a timber variant, iron bars otherwise (one block, the autoconnect pass joins neighbours)."""
    if st.wood:
        mt.set(x, y, z, L.FENCE)
    else:
        bars(mt, x, y, z)


def clear_column(mt, x, z, y0, y1):
    for y in range(y0, y1 + 1):
        mt.remove(x, y, z)


# =====================================================================================================================
# the small climbing stairwell (9 x 9): up_a, up_b, up_c
# =====================================================================================================================
UP_H = 36                  # interior rows 9..44: the landing is at standing row 41, the ceiling at 45
UP_STEPS = 32              # two full turns of the 16-cell ring
UP_FIRST = 2               # ring index of the first stair (indices 14, 15, 0, 1 are the platform)


def small_up(name, style, ccw, well):
    mt = shell(name, 1, 1, UP_H)
    mt.socket("e", 4, pool=POOL_DOOR)
    r = ring(2, 6, ccw)
    top = ROW0 + UP_STEPS - 1                                   # block row of the last stair and of the landing pad (40)
    # the newel: a solid 3 x 3 core, or an open well of bars round a centre column of invisible light
    for x in range(3, 6):
        for z in range(3, 6):
            for y in range(ROW0, top):
                centre = (x, z) == (4, 4)
                if well:
                    if centre:
                        continue
                    bars(mt, x, y, z)
                else:
                    mt.set(x, y, z, style.band if (y - ROW0) % 5 == 4 else style.newel)
            mt.set(x, top, z, style.tread)                      # the landing pad
    mt.set(4, top, 4, style.band)
    if well:
        for y in range(ROW0 + 2, top - 1, 6):
            mt.set(4, y, 4, "minecraft:light", {"level": "13"})
    for k in (14, 15, 0, 1):                                    # the platform inside the door
        mt.set(r[k][0], FLOOR, r[k][1], style.tread)
    for j in range(UP_STEPS):                                   # one stair per ring cell, one block higher each
        k = UP_FIRST + j
        a, b = r[k % 16], r[(k + 1) % 16]
        stair(mt, style, a[0], ROW0 + j, a[1], toward(a, b))
    for k in range(3, 14):                                      # a rail round the landing, on the cells the last turn does not pass at that height
        for y in (top + 1, top + 2):
            rail(mt, style, r[k][0], y, r[k][1])
    mt.marker(r[15][0], ROW0, r[15][1], "arrival")
    mt.marker(4, top + 1, 4, "stair_exit", dir="up")
    # light: lamps in niches of the newel (a), or wall flames along the outer wall (b, c); a lamp over the landing
    if not well:
        faces = [(3, 4), (4, 5), (5, 4), (4, 3)]
        n = 0
        for y in range(ROW0 + 4, top - 2, 5):
            x, z = faces[n % 4]
            n += 1
            mt.remove(x, y, z)
            mt.remove(x, y + 1, z)
            style.lamp(mt, x, y, z)
            mt.set(x, y + 2, z, style.trim)
    wall_flames(mt, style, r, UP_FIRST, UP_STEPS)
    style.lamp(mt, 4, ROW0 + UP_H - 1, 4, hanging=True)
    if style.wood:                                               # a crate at the end of the platform, a lamp on it
        mt.set(r[14][0], ROW0, r[14][1], "minecraft:barrel", {"facing": "up", "open": "false"})
        style.lamp(mt, r[14][0], ROW0 + 1, r[14][1])
    else:
        style.lamp(mt, r[14][0], ROW0, r[14][1])
        for (x, z, f) in ((4, 2, "south"), (4, 6, "north")):
            style.banner(mt, x, ROW0 + 6, z, f)
    return mt


def wall_flames(mt, style, r, first, steps):
    """A flame on the outer wall above every fourth stair: the torch stands in the ring cell next to the wall, three rows above the stair (above a head), on the
    wall block behind it. Only ring cells with a solid wall behind (not the corners, not the doorway) get one."""
    behind = {(6, 4): "west", (2, 4): "east", (4, 6): "north", (4, 2): "south"}
    for j in range(steps):
        k = first + j
        cell = r[k % 16]
        if cell in behind and (k % 16) in (0, 4, 8, 12):
            y = ROW0 + j + 3
            if cell == (6, 4) and y < ROW0 + 5:
                continue                                          # the doorway is open in the east wall up to row 12
            style.torch(mt, cell[0], y, cell[1], behind[cell])


# =====================================================================================================================
# the small dropping stairwell (9 x 9): down_a, down_b, down_c
# =====================================================================================================================
DOWN_H = 14
DOWN_STEPS = 8
DOWN_FIRST = 2
FLOOR_LEVEL_RING = (10, 11, 12, 13, 14, 15, 0, 1)             # ring indices that stay at floor level (the walkway round the pit and the platform)


def small_down(name, style, ccw, flavour):
    mt = shell(name, 1, 1, DOWN_H)
    mt.socket("e", 4, pool=POOL_DOOR)
    fill_mass(mt, 0, FLOOR - 1)
    r = ring(2, 6, ccw)
    for x in range(3, 6):                                       # the pit
        for z in range(3, 6):
            clear_column(mt, x, z, 1, FLOOR)
            mt.set(x, 0, z, style.tread)
    for k in (14, 15, 0, 1):                                    # the platform inside the door
        mt.set(r[k][0], FLOOR, r[k][1], style.tread)
    for j in range(DOWN_STEPS):                                 # a stair that drops faces the way the walker came from
        k = DOWN_FIRST + j
        a, prev = r[k % 16], r[(k - 1) % 16]
        y = FLOOR - j
        clear_column(mt, a[0], a[1], y + 1, FLOOR)
        stair(mt, style, a[0], y, a[1], toward(a, prev))
    entry = None                                                # the pad cell the last stair opens onto
    last = r[(DOWN_FIRST + DOWN_STEPS - 1) % 16]
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        if (last[0] + dx, last[1] + dz) in {(x, z) for x in range(3, 6) for z in range(3, 6)}:
            entry = (last[0] + dx, last[1] + dz)
    # the rim: a rail on the pit cells next to a walkway at floor level
    for x in range(3, 6):
        for z in range(3, 6):
            if any(r.index(n) in FLOOR_LEVEL_RING for n in ((x + 1, z), (x - 1, z), (x, z + 1), (x, z - 1)) if n in r):
                for y in (ROW0, ROW0 + 1):
                    rail(mt, style, x, y, z)
    mt.marker(r[15][0], ROW0, r[15][1], "arrival")
    mt.marker(4, 1, 4, "stair_exit", dir="down")
    # light: standing lamps on the pad corners that are not the way in, flames on the pit walls that face a walkway, a lamp over the platform
    for (x, z) in ((3, 3), (5, 3), (3, 5), (5, 5)):
        if (x, z) != entry:
            style.lamp(mt, x, 1, z)
    pit_wall = {(4, 3): ((4, 2), "south"), (4, 5): ((4, 6), "north"), (5, 4): ((6, 4), "west"), (3, 4): ((2, 4), "east")}
    for (px, pz), (ring_cell, f) in pit_wall.items():
        if r.index(ring_cell) in FLOOR_LEVEL_RING:
            style.torch(mt, px, 5, pz, f)
    style.lamp(mt, r[15][0], ROW0 + DOWN_H - 1, r[15][1], hanging=True)
    if flavour != "crates":
        style.lamp(mt, r[14][0], ROW0, r[14][1])                 # the end of the platform
    behind = {(6, 4): "west", (2, 4): "east", (4, 6): "north", (4, 2): "south"}       # a flame on the wall above the stairs, three rows over each step
    for j in range(DOWN_STEPS):
        cell = r[(DOWN_FIRST + j) % 16]
        if cell in behind and r.index(cell) % 4 == 0:
            style.torch(mt, cell[0], FLOOR - j + 3, cell[1], behind[cell])
    if flavour == "chain":                                      # a lamp hung over the middle of the pit on a long chain
        for y in range(ROW0 + DOWN_H - 1, ROW0 + 5, -1):
            K.chain(mt, 4, y, 4)
        style.lamp(mt, 4, ROW0 + 5, 4, hanging=True)
    elif flavour == "crates":
        mt.set(r[14][0], ROW0, r[14][1], "minecraft:barrel", {"facing": "up", "open": "false"})
        style.lamp(mt, r[14][0], ROW0 + 1, r[14][1])
    else:
        for (x, z, f) in ((4, 2, "south"), (4, 6, "north")):
            style.banner(mt, x, ROW0 + 5, z, f)
    return mt


# =====================================================================================================================
# the grand stairwell of a boss floor (18 x 18)
# =====================================================================================================================
G_LO, G_HI = (3, 4, 5), (13, 14, 15)                              # the cells of the two bands along the walls (3 wide)
G_FLIGHT = range(6, 13)                                           # the seven cells of a flight between two landings
GRAND_UP_H = 37                                                   # interior rows 9..45: the pad is at standing row 37, the ceiling at 46


def grand_up(name, style, extras):
    mt = shell(name, 2, 2, GRAND_UP_H)
    mt.socket("e", 4, pool=POOL_DOOR)
    # landings: piers of solid masonry from the floor, a tread on top
    landings = {"SE": (G_HI, G_HI, ROW0 + 6), "SW": (G_LO, G_HI, ROW0 + 13), "NW": (G_LO, G_LO, ROW0 + 20), "NE": (G_HI, G_LO, ROW0 + 27)}
    for key, (xs, zs, row) in landings.items():
        for x in xs:
            for z in zs:
                if key != "NE":
                    for y in range(ROW0, row):
                        mt.set(x, y, z, style.band if (y - ROW0) % 5 == 4 else style.newel)
                mt.set(x, row, z, style.tread)
        mt.set(xs[1], row, zs[1], style.band)
    # the four flights: seven stairs of seven blocks of rise each, three wide, each facing the way it climbs
    flights = [("E", "south", lambda i, w: (G_HI[w], 6 + i), ROW0 + 0), ("S", "west", lambda i, w: (12 - i, G_HI[w]), ROW0 + 7),
               ("W", "north", lambda i, w: (G_LO[w], 12 - i), ROW0 + 14), ("N", "east", lambda i, w: (6 + i, G_LO[w]), ROW0 + 21)]
    for key, facing, cell, base in flights:
        for i in range(7):
            for w in range(3):
                x, z = cell(i, w)
                stair(mt, style, x, base + i, z, facing)
    # rails along the well side of every flight, rising with the stairs
    for i in range(7):
        rail(mt, style, 12, ROW0 + 1 + i, 6 + i); rail(mt, style, 12, ROW0 + 2 + i, 6 + i)                    # E: x = 12
        rail(mt, style, 12 - i, ROW0 + 8 + i, 12); rail(mt, style, 12 - i, ROW0 + 9 + i, 12)                  # S: z = 12
        rail(mt, style, 6, ROW0 + 15 + i, 12 - i); rail(mt, style, 6, ROW0 + 16 + i, 12 - i)                  # W: x = 6
        rail(mt, style, 6 + i, ROW0 + 22 + i, 6); rail(mt, style, 6 + i, ROW0 + 23 + i, 6)                    # N: z = 6
    for x in G_HI:                                                                                           # the pad's open side
        for y in (ROW0 + 29, ROW0 + 30):
            rail(mt, style, x, y, 6)
    mt.marker(14, ROW0, 4, "arrival")
    mt.marker(14, ROW0 + 28, 4, "stair_exit", dir="up")
    # light: a chandelier in the well, a lamp on every landing, flames on the walls beside the flights
    K.chandelier(mt, 9, FLOOR + GRAND_UP_H + 1, 9, drop=12, radius=2, soul=style.soul, arm_lift=2)
    for key, (xs, zs, row) in landings.items():
        style.lamp(mt, xs[0] if key in ("SE", "NE") else xs[2], row + 1, zs[0] if key in ("NE", "NW") else zs[2])
    for i in (1, 4):                                              # the walls beside the flights (the west and north flights have a one-cell margin before the wall)
        style.torch(mt, 15, ROW0 + i + 3, 6 + i, "west")
        style.torch(mt, 12 - i, ROW0 + 7 + i + 3, 15, "north")
        style.torch(mt, 2, ROW0 + 14 + i + 3, 12 - i, "east")
        style.torch(mt, 6 + i, ROW0 + 21 + i + 3, 2, "south")
    for (x, z) in ((6, 2), (9, 2), (12, 2), (2, 6), (2, 9), (2, 12), (7, 15), (10, 15), (15, 8), (15, 11), (2, 2), (2, 15), (9, 9)):     # the ground under the flights
        style.lamp(mt, x, ROW0, z)
    for i in (3,):                                                # a lamp hung under the middle stair of each flight, on the outer column
        style.lamp(mt, 15, ROW0 + i - 1, 6 + i, hanging=True)
        style.lamp(mt, 12 - i, ROW0 + 7 + i - 1, 15, hanging=True)
        style.lamp(mt, 3, ROW0 + 14 + i - 1, 12 - i, hanging=True)
        style.lamp(mt, 6 + i, ROW0 + 21 + i - 1, 3, hanging=True)
    style.lamp(mt, 14, FLOOR + GRAND_UP_H, 4, hanging=True)
    if extras == "banners":
        for z in (8, 12):
            style.banner(mt, 15, ROW0 + 12, z, "west", "black")
    elif extras == "braziers":
        for (x, z) in ((6, 15), (12, 3)):
            mt.set(x, ROW0, z, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false"})
    return mt


GRAND_DOWN_H = 20


def grand_down(name, style, extras):
    mt = shell(name, 2, 2, GRAND_DOWN_H)
    mt.socket("e", 4, pool=POOL_DOOR)
    fill_mass(mt, 0, FLOOR - 1)
    ew = list(range(13, 16))                                    # the east band, 3 wide
    sw = list(range(13, 16))                                    # the south band
    # the court: everything south of the north gallery (z >= 6) is dug down to row 0, except the flights and the landing between them
    stairs_e = {(x, z) for x in ew for z in range(6, 10)}                       # E flight, z = 6..9, rows 8..5
    flat_e = {(x, z) for x in ew for z in range(10, 16)}                         # the flat after it and the SE landing, row 4
    stairs_s = {(x, z) for x in range(9, 13) for z in sw}                       # S flight, x = 12..9, rows 4..1
    for z in range(6, 16):
        for x in range(2, 16):
            if (x, z) in stairs_e or (x, z) in flat_e or (x, z) in stairs_s:
                continue
            clear_column(mt, x, z, 1, FLOOR)
            mt.set(x, 0, z, style.tread if (x in range(3, 6) and z in range(13, 16)) or (z in sw and x in range(6, 9)) else (L.TILE if (x + z) % 2 else L.POLISHED))
    for (x, z) in sorted(stairs_e):
        j = z - 6
        clear_column(mt, x, z, FLOOR - j + 1, FLOOR)
        stair(mt, style, x, FLOOR - j, z, "north")
    for (x, z) in sorted(flat_e):
        clear_column(mt, x, z, 5, FLOOR)
        mt.set(x, 4, z, style.tread)
    for (x, z) in sorted(stairs_s):
        j = 12 - x
        clear_column(mt, x, z, 4 - j + 1, FLOOR)
        stair(mt, style, x, 4 - j, z, "east")
    mt.set(14, 4, 14, style.band)
    mt.set(4, 0, 14, style.band)
    # rails: the gallery's rim and the well side of both flights
    for x in range(2, 13):
        for y in (ROW0, ROW0 + 1):
            rail(mt, style, x, y, 6)
    for j in range(4):
        rail(mt, style, 12, FLOOR - j + 1, 6 + j); rail(mt, style, 12, FLOOR - j + 2, 6 + j)
        rail(mt, style, 12 - j, 4 - j + 1, 12); rail(mt, style, 12 - j, 4 - j + 2, 12)
    for z in range(10, 13):
        rail(mt, style, 12, 5, z); rail(mt, style, 12, 6, z)
    mt.marker(14, ROW0, 4, "arrival")
    mt.marker(4, 1, 14, "stair_exit", dir="down")
    # light
    K.chandelier(mt, 9, FLOOR + GRAND_DOWN_H + 1, 9, drop=8, radius=2, soul=style.soul, arm_lift=2)
    for (x, z) in ((3, 13), (5, 15), (3, 15), (3, 6), (6, 6), (9, 6), (12, 6), (3, 9), (3, 12), (8, 15), (6, 15), (8, 9), (6, 9)):
        style.lamp(mt, x, 1, z)
    style.lamp(mt, 15, 5, 15)
    style.lamp(mt, 13, 5, 11)
    for x in (4, 8, 12):
        style.torch(mt, x, ROW0 + 3, 2, "south")
    for z in (8, 11, 14):
        style.torch(mt, 2, 5, z, "east")
    for j in (0, 2):                                              # flames over the stairs along the east and the south wall
        style.torch(mt, 15, FLOOR - j + 3, 6 + j, "west")
        style.torch(mt, 12 - j, 4 - j + 3, 15, "north")
    style.torch(mt, 15, 8, 11, "west")
    style.lamp(mt, 14, FLOOR + GRAND_DOWN_H, 4, hanging=True)
    if extras == "banners":
        for x in (5, 9):
            style.banner(mt, x, ROW0 + 5, 2, "south", "black")
    elif extras == "braziers":
        for (x, z) in ((3, 3), (9, 3)):
            mt.set(x, ROW0, z, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false"})
    return mt


# =====================================================================================================================
# the catalogue
# =====================================================================================================================
# name -> (builder, expected standing row of the exit, direction)
BUILDERS = {
    "up_a": (lambda: small_up("mega/stairwell/up_a", STONE, False, False), ROW0 + UP_STEPS, "up"),
    "up_b": (lambda: small_up("mega/stairwell/up_b", IRON, True, True), ROW0 + UP_STEPS, "up"),
    "up_c": (lambda: small_up("mega/stairwell/up_c", TIMBER, False, False), ROW0 + UP_STEPS, "up"),
    "down_a": (lambda: small_down("mega/stairwell/down_a", STONE, False, "banner"), 1, "down"),
    "down_b": (lambda: small_down("mega/stairwell/down_b", IRON, True, "chain"), 1, "down"),
    "down_c": (lambda: small_down("mega/stairwell/down_c", TIMBER, False, "crates"), 1, "down"),
    "grand_up_a": (lambda: grand_up("mega/stairwell/grand_up_a", STONE, "banners"), ROW0 + 28, "up"),
    "grand_up_b": (lambda: grand_up("mega/stairwell/grand_up_b", IRON, "braziers"), ROW0 + 28, "up"),
    "grand_down_a": (lambda: grand_down("mega/stairwell/grand_down_a", STONE, "banners"), 1, "down"),
    "grand_down_b": (lambda: grand_down("mega/stairwell/grand_down_b", IRON, "braziers"), 1, "down"),
}


# =====================================================================================================================
# verification
# =====================================================================================================================
class Report:
    def __init__(self, name):
        self.name = name
        self.problems = []
        self.facts = {}

    @property
    def ok(self):
        return not self.problems

    def text(self):
        head = "%-14s %s" % (self.name, "ok  " if self.ok else "FAIL")
        facts = "  ".join("%s=%s" % kv for kv in self.facts.items())
        return head + "  " + facts + "".join("\n      - " + p for p in self.problems)


def marker_cells(t, kind):
    out = []
    for p, c in t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            meta = str(c.nbt.get("metadata", ""))
            if meta.split(";")[0] == "nusmp:" + kind:
                out.append((p, meta))
    return out


def verify(name, mt, exit_row, direction):
    """The checks of the module docstring. Returns a Report; the template is only written when it is ok."""
    rep = Report(name)
    mt.finish()
    rep.problems += mt.validate()
    t = mt.t
    arrivals, exits = marker_cells(t, "arrival"), marker_cells(t, "stair_exit")
    if len(arrivals) != 1:
        rep.problems.append("needs exactly one arrival marker, has %d" % len(arrivals))
    if len(exits) != 1:
        rep.problems.append("needs exactly one stair_exit marker, has %d" % len(exits))
    jigs = t.jigsaws()
    if len(jigs) != 1:
        rep.problems.append("a stairwell has exactly one door, found %d jigsaws" % len(jigs))
    else:
        (jx, jy, jz), orient, data = jigs[0]
        if orient.split("_")[0] != "east" or jx != t.size[0] - 1 or jy != ROW0:
            rep.problems.append("the door must be on the east face in the first row above the floor: %s at %s" % (orient, (jx, jy, jz)))
        if str(data["pool"]) != POOL_DOOR:
            rep.problems.append("the door must draw from %s" % POOL_DOOR)
    cells = SP.standable_cells(t)
    if arrivals:
        a = arrivals[0][0]
        if a not in cells:
            rep.problems.append("the arrival cell %s cannot be walked to from the door" % (a,))
        if a[1] != ROW0:
            rep.problems.append("the arrival cell must be at the standing row %d, not %d" % (ROW0, a[1]))
    if exits:
        e, meta = exits[0]
        if e not in cells:
            rep.problems.append("the stair exit %s cannot be walked to from the door (a stair too steep, a rail across the way, no headroom)" % (e,))
        if e[1] != exit_row:
            rep.problems.append("the stair exit stands at row %d but the geometry promises row %d" % (e[1], exit_row))
        if "dir=" + direction not in meta:
            rep.problems.append("the stair exit says %s but this stairwell goes %s" % (meta, direction))
        # the pad: the exit cell and its eight neighbours must be standable, so a player on the pad is not on a post
        pad = [(e[0] + dx, e[1], e[2] + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1)]
        missing = [c for c in pad if c not in cells and not blocked_by_rail(t, c)]
        if missing:
            rep.problems.append("the landing pad is not a 3 x 3 floor: %s cannot be stood on" % (missing[:3],))
    lf = SP.light_field(t)
    vals = [lf.get((x, y + 1, z), 0) for (x, y, z) in cells]
    low = min(vals) if vals else 0
    if low < MIN_LIGHT:
        worst = [c for c in cells if lf.get((c[0], c[1] + 1, c[2]), 0) == low][:3]
        rep.problems.append("block light drops to %d at head height (cells %s); the least allowed is %d" % (low, worst, MIN_LIGHT))
    rep.problems += AC.problems(t, name)
    rep.facts.update(size="%dx%dx%d" % t.size, walkable=len(cells), light_min=low, blocks=len(t.cells))
    return rep


def blocked_by_rail(t, c):
    """A pad cell may hold a rail or a lamp (it is then not stood on, but the pad is still a pad)."""
    cell = t.cells.get(c)
    return cell is not None


def build_all(names=None):
    out = []
    for name, (fn, exit_row, direction) in BUILDERS.items():
        if names and name not in names:
            continue
        mt = fn()
        out.append((name, mt, verify("mega/stairwell/" + name, mt, exit_row, direction)))
    return out


def render(name, mt, directory):
    """Four cutaways (from each corner), a plan of the floor level with the markers and a block-light map."""
    os.makedirs(directory, exist_ok=True)
    t = mt.t
    sx, sy, sz = t.size
    for k in range(4):
        w, d = (sx, sz) if k % 2 == 0 else (sz, sx)
        SP.render(t, os.path.join(directory, "%s_iso%d.png" % (name, k)), xr=(0, w - 3), yr=(0, sy - 3), zr=(0, d - 3), scale=7, rot=k)
    SP.render(t, os.path.join(directory, "%s_plan.png" % name), xr=(0, sx - 1), yr=(0, sy - 1), zr=(0, sz - 1), scale=16, top=True)
    SP.render(t, os.path.join(directory, "%s_light.png" % name), xr=(0, sx - 1), yr=(0, sy - 1), zr=(0, sz - 1), scale=16, top=True, light=True)


def main(argv):
    check_only = "--check" in argv
    render_dir = None
    names = []
    i = 0
    while i < len(argv):
        a = argv[i]
        if a == "--render":
            if i + 1 >= len(argv):
                print("--render needs a directory")
                return 2
            render_dir = argv[i + 1]
            i += 1
        elif not a.startswith("--"):
            names.append(a)
        i += 1
    unknown = [n for n in names if n not in BUILDERS]
    if unknown:
        print("unknown stairwell(s): %s (known: %s)" % (", ".join(unknown), ", ".join(BUILDERS)))
        return 2
    bad = 0
    results = build_all(names or None)
    for name, mt, rep in results:
        print(rep.text())
        if not rep.ok:
            bad += 1
        if render_dir:
            render(name, mt, render_dir)
        if rep.ok and not check_only:
            p = mt.save()
            print("      wrote %s (%d bytes)" % (os.path.relpath(p, L.ROOT), os.path.getsize(p)))
    print("%d stairwell(s), %d failed verification" % (len(results), bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

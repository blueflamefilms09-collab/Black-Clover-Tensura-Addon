#!/usr/bin/env python3
"""
Helpers of the guard builder's templates (the boss chain of a Mega-Dungeon sector: the Guardian's hall, the antechamber, the Way Onward).

Why a module of its own: `mega_lib.py` is shared by every builder of the Mega-Dungeon and nobody may edit it, so everything the three boss-chain
rooms have in common (columns with flared capitals, lit lancet windows, hanging chains of lanterns, rune rings in a floor, the Gatekeeper slab, the
rubble seal of the Way Onward, the barrel vault of a corridor) is written once here and imported by `gen_mega_guard.py`. Every helper takes the
template `mt` (an `mega_lib.MT`) and writes with `mt.set`, so the block-state type-check of `mega_lib` and the "a doorway hole must stay clear"
guard apply to everything in here too.

Conventions: template-local cells (x east, y up, z south); `side` is one of 'n', 'e', 's', 'w' (the face of the template a wall stands on);
`lat` is the position ALONG that face (x for n/s, z for e/w); `depth` is how far in from the outer face (0, 1 = the two wall layers, 2 = the first
interior layer). A stair `facing` is the direction of its high back (the way you climb), exactly as in the game; a stair with `half=top` hangs
from above, its high back toward `facing`.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

FLOOR = L.FLOOR_ROW
DIRS = {"n": "north", "e": "east", "s": "south", "w": "west"}
INWARD = {"n": "south", "s": "north", "w": "east", "e": "west"}      # from the wall into the room
TOWARD_WALL = DIRS                                                      # from the room to the wall

GATE_STATE = "nusmp:gatekeeper_door[kind=sector,open=false,half=lower]"


# ------------------------------------------------------------------ small writers
def put(mt, x, y, z, block, **props):
    """mt.set with the properties as keyword arguments (booleans become true/false)."""
    mt.set(x, y, z, block, {k: ("true" if v is True else "false" if v is False else str(v)) for k, v in props.items()} or None)


def stair(mt, x, y, z, family, facing, half="bottom", shape=None):
    """A stair of `family` (e.g. L.POLISHED_STAIRS) whose high back is toward `facing`."""
    props = {"facing": facing, "half": half}
    if shape:
        props["shape"] = shape
    mt.set(x, y, z, family, props)


def slab(mt, x, y, z, block, typ="bottom"):
    mt.set(x, y, z, block, {"type": typ})


def cell(mt, side, lat, depth, y):
    """The template cell at lateral `lat`, `depth` layers in from the outer face of `side`, at row y."""
    return mt._lateral(side, 0, lat, depth, y)


def put_wall(mt, side, lat, depth, y, block, props=None):
    x, yy, z = cell(mt, side, lat, depth, y)
    mt.set(x, yy, z, block, props)


# ------------------------------------------------------------------ plan shapes
def disc(cx, cz, r):
    """Cells of a filled disc of radius r round the cell (cx, cz) (the +0.5 makes small radii round)."""
    return [(x, z) for z in range(int(cz - r) - 1, int(cz + r) + 2) for x in range(int(cx - r) - 1, int(cx + r) + 2)
            if (x - cx) ** 2 + (z - cz) ** 2 <= (r + 0.5) ** 2]


def dist(x, z, cx, cz):
    return math.hypot(x - cx, z - cz)


def oct_norm(dx, dz, half=20.0, sum_limit=28.0):
    """The octagonal distance of an offset from the centre of the chamfered hall: 0 at the centre, `half` on an axis wall and on a diagonal wall
    (`sum_limit` is |dx| + |dz| on the diagonal walls). Rings of equal value are regular octagons."""
    return max(abs(dx), abs(dz), (abs(dx) + abs(dz)) * half / sum_limit)


def in_octagon(x, z, cx=22, cz=22, half=20, cut=12):
    """Is the cell inside the interior of the octagon: the square of half-width `half` with each corner cut off by `cut` cells."""
    dx, dz = abs(x - cx), abs(z - cz)
    return dx <= half and dz <= half and dx + dz <= 2 * half - cut


# ------------------------------------------------------------------ columns, pilasters, arches
def column(mt, cx, cz, y0, y1, shaft=L.POLISHED, band=L.CHISELED, bands=(0.33, 0.66), base=True):
    """A free-standing 3x3 column from row y0 to y1: a flared base of stairs, a shaft with chiselled bands, a flared capital of upside-down stairs.
    (cx, cz) is the centre cell. Rows y0+1 .. y1-1 are the shaft; the capital is the top row y1 (5x5 at the very top, a ring of stairs round the shaft)."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in range(y0, y1 + 1):
                mt.set(cx + dx, y, cz + dz, shaft)
    for k in bands:
        y = y0 + int((y1 - y0) * k)
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if (dx, dz) != (0, 0):
                    mt.set(cx + dx, y, cz + dz, band)
    ring = [(dx, dz) for dx in range(-2, 3) for dz in range(-2, 3) if max(abs(dx), abs(dz)) == 2]
    for dx, dz in ring:
        corner = abs(dx) == 2 and abs(dz) == 2
        if corner:
            continue
        toward = ("east" if dx < 0 else "west") if abs(dx) == 2 else ("south" if dz < 0 else "north")
        stair(mt, cx + dx, y1, cz + dz, L.POLISHED_STAIRS, toward, half="top", shape="straight")
        if base:
            stair(mt, cx + dx, y0, cz + dz, L.POLISHED_STAIRS, toward, half="bottom", shape="straight")
    return mt


def lateral_step(side, sign):
    """The compass direction of one step of +1 / -1 along the wall of `side`."""
    along_x = side in ("n", "s")
    return ("east" if sign > 0 else "west") if along_x else ("south" if sign > 0 else "north")


def pilaster(mt, side, lat, y0, y1, w=3, shaft=L.POLISHED, band=L.CHISELED):
    """A half column against the inner face of the wall of `side`: `w` wide centred on lateral `lat`, one cell deep, rows y0 .. y1 (a chiselled band
    half way up), with a flared capital of upside-down stairs on its front and its two flanks."""
    half = w // 2
    first = lat - half
    for l in range(first, first + w):
        for y in range(y0, y1 + 1):
            put_wall(mt, side, l, 2, y, shaft)
        put_wall(mt, side, l, 2, y0 + (y1 - y0) // 2, band)
        x, y, z = cell(mt, side, l, 3, y1)
        stair(mt, x, y, z, L.POLISHED_STAIRS, TOWARD_WALL[side], half="top", shape="straight")
    for l, sign in ((first - 1, 1), (first + w, -1)):
        x, y, z = cell(mt, side, l, 2, y1)
        stair(mt, x, y, z, L.POLISHED_STAIRS, lateral_step(side, sign), half="top", shape="straight")


def lancet(mt, side, lat0, w, y0, h, glass, light="minecraft:sea_lantern", frame=L.POLISHED):
    """A lit lancet window in the two-layer wall of `side`: `w` wide starting at lateral cell `lat0`, `h` tall from row y0; coloured glass in the
    inner layer, a sea lantern behind it in the outer layer (so it glows into the room), the top row narrowed by one cell each side, a polished
    sill and a polished lintel. The glass blocks are full blocks (no panes): they never need connecting and they let the light through."""
    for k in range(h):
        narrow = k == h - 1 and w >= 3
        for l in range(lat0, lat0 + w):
            if narrow and (l == lat0 or l == lat0 + w - 1):
                continue
            put_wall(mt, side, l, 1, y0 + k, glass)
            put_wall(mt, side, l, 0, y0 + k, light)
    for l in range(lat0 - 1, lat0 + w + 1):
        put_wall(mt, side, l, 1, y0 - 1, frame)
        put_wall(mt, side, l, 1, y0 + h, frame)
    if w >= 4:                                                           # a mullion splits a wide window
        mid = lat0 + w // 2
        for k in range(h - 1):
            put_wall(mt, side, mid, 1, y0 + k, frame)


def wall_banners(mt, side, lat0, count, y, colors):
    """`count` wall banners side by side from lateral cell `lat0`, hanging from row y (the cloth reaches the row below), colours cycling through `colors`."""
    for k in range(count):
        x, yy, z = cell(mt, side, lat0 + k, 2, y)
        mt.set(x, yy, z, "minecraft:%s_wall_banner" % colors[k % len(colors)], {"facing": INWARD[side]})


def hang_lantern(mt, x, y_top, z, length, soul=False):
    """A lantern hanging on `length` chain blocks from the cell below `y_top` (the lowest solid ceiling row is y_top + 1)."""
    for k in range(length):
        mt.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    mt.set(x, y_top - length, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})


def chandelier_ring(mt, cx, cz, y_top, radius, count, length, soul=True, phase=0.0):
    """`count` lantern chains in a ring of `radius` round (cx, cz), hanging from y_top."""
    seen = set()
    for i in range(count):
        a = phase + i * 2 * math.pi / count
        x, z = int(round(cx + math.cos(a) * radius)), int(round(cz + math.sin(a) * radius))
        if (x, z) in seen:
            continue
        seen.add((x, z))
        hang_lantern(mt, x, y_top, z, length, soul=soul)


def brazier(mt, x, y, z, soul=False):
    """A stone post with a fire on top (a campfire: the game counts it as a light source, it smokes and it is solid ground for nothing else)."""
    mt.set(x, y, z, L.POLISHED)
    mt.set(x, y + 1, z, L.POLISHED_WALL)
    mt.set(x, y + 2, z, "minecraft:soul_campfire" if soul else "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})


def candle_cluster(mt, x, y, z, color="white", n=3):
    mt.set(x, y, z, "minecraft:%s_candle" % color, {"candles": str(n), "lit": "true"})


def beam(mt, x1, z1, x2, z2, y, axis, block=L.LOG):
    """A dark-oak beam (a log lying along `axis`) from (x1, z1) to (x2, z2) at row y."""
    for z in range(min(z1, z2), max(z1, z2) + 1):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            mt.set(x, y, z, block, {"axis": axis})


# ------------------------------------------------------------------ the chain: door, seal
def fill_gatekeeper(mt, side, offset):
    """Fills the 3 x 4 x 2 hole of the socket on `side` at `offset` with the sector Gatekeeper (rows 9-10 lower half, rows 11-12 upper half, both wall
    layers). Call AFTER `mt.finish()`; the jigsaw cell itself (bottom centre of the outer layer) is left alone: its final state is the door block."""
    jig = mt.socket_cell(side, offset)
    n = 0
    for depth in range(L.WALL):
        for lat in range(offset - 1, offset + 2):
            for row in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                x, y, z = cell(mt, side, lat, depth, row)
                if (x, y, z) == jig:
                    continue
                # written on the underlying Template: mega_lib's schema table reads `gatekeeper_door` as a wooden door (its `.*_door` rule) and would refuse `kind`
                mt.t.set(x, y, z, "nusmp:gatekeeper_door", {"kind": "sector", "open": "false", "half": "lower" if row < L.DOOR_ROW + 2 else "upper"})
                n += 1
    return n


RUBBLE = ((38, L.COBBLED), (18, L.CRACKED_BRICK), (14, "minecraft:tuff"), (12, L.ROCK), (10, L.COBBLED_WALL), (8, L.CRACKED_TILE))


def rubble_seal(mt, side, offset):
    """The sealed end of a Way Onward: the 3 x 4 x 2 hole of the socket filled with a mix of rubble blocks (the jigsaw cell becomes cobbled deepslate
    by its final state). The cell above the jigsaw cell in the outer layer stays free for the EXPANSION_SOCKET marker (a structure block is never
    placed, so that one cell is a dimple in the rubble until the generator clears the seal). Call AFTER `mt.finish()`."""
    jig = mt.socket_cell(side, offset)
    marker_cell = cell(mt, side, offset, 0, L.DOOR_ROW + 1)
    for depth in range(L.WALL):
        for lat in range(offset - 1, offset + 2):
            for row in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                c = cell(mt, side, lat, depth, row)
                if c == jig or c == marker_cell:
                    continue
                block = mt.pick(RUBBLE)
                if block.endswith("_wall"):
                    mt.set(c[0], c[1], c[2], block, {"up": "true"})
                else:
                    mt.set(c[0], c[1], c[2], block)
    return marker_cell


# ------------------------------------------------------------------ the corridor shell
class Leg:
    """One straight leg of a corridor: it runs along `axis` ('z' = north-south, 'x' = east-west) at lateral centre `c` from `a0` to `a1` (inclusive,
    interior cells only), 5 wide. Used by `corridor_shell` to build a straight corridor or an L (two legs)."""

    def __init__(self, axis, c, a0, a1):
        self.axis, self.c, self.a0, self.a1 = axis, c, min(a0, a1), max(a0, a1)

    def cells(self):
        out = []
        for a in range(self.a0, self.a1 + 1):
            for u in range(-2, 3):
                out.append((self.c + u, a) if self.axis == "z" else (a, self.c + u))
        return out

    def contains(self, x, z):
        a, l = (z, x) if self.axis == "z" else (x, z)
        return self.a0 <= a <= self.a1 and abs(l - self.c) <= 2

    def u(self, x, z):
        return abs((x if self.axis == "z" else z) - self.c)


#: free height above the floor (interior rows above row 8) by distance from the leg's centre line: 0 -> 9, 1 -> 8, 2 -> 6 (a rounded barrel vault)
VAULT = {0: 9, 1: 8, 2: 6}


def corridor_shell(mt, legs, wall=L.BRICK, floor=L.TILE, ceiling=L.TILE, wear=0.14, vault=VAULT):
    """Builds the shell of a corridor made of `legs` on the template: a floor, two-thick walls round every interior cell (so a straight leg fills its
    9-block module exactly), and a barrel-vaulted roof (a groin vault where two legs cross). Returns {(x, z): top} with the highest free row of every
    interior cell. Doorway holes are NOT carved here: declare the sockets afterwards and `finish()` carves them."""
    interior = {}
    for leg in legs:
        for (x, z) in leg.cells():
            top = FLOOR + vault[leg.u(x, z)]
            interior[(x, z)] = max(interior.get((x, z), 0), top)
    solid = {}
    for (x, z), top in interior.items():
        for dx in range(-2, 3):
            for dz in range(-2, 3):
                p = (x + dx, z + dz)
                if p in interior:
                    continue
                near = max(abs(dx), abs(dz))
                if p not in solid or near < solid[p][0]:
                    solid[p] = (near, top)
                elif near == solid[p][0]:
                    solid[p] = (near, min(solid[p][1], top))
    for (x, z), top in interior.items():
        mt.set(x, FLOOR, z, L.CRACKED_TILE if mt.rng.random() < wear else floor)
        for k in (1, 2):
            mt.set(x, top + k, z, ceiling)
    for (x, z), (near, top) in solid.items():
        if not (0 <= x < mt.W and 0 <= z < mt.D):
            continue
        mt.set(x, FLOOR, z, wall)
        for y in range(FLOOR + 1, top + 3):
            mt.set(x, y, z, L.CRACKED_BRICK if mt.rng.random() < wear * 0.6 else wall)
    return interior


# ------------------------------------------------------------------ the checks a game would do for us
#: blocks that do not stop air, light or a mob from passing for the purposes of the seal check (a hanging lantern does not close a hole)
_NOT_A_SEAL = ("air", "cave_air", "void_air", "structure_block", "light")
_THIN_EXACT = ("chain", "lantern", "soul_lantern", "campfire", "soul_campfire", "end_rod", "cobweb", "ladder", "lever", "lightning_rod", "pointed_dripstone", "armor_stand")
_THIN_PARTS = ("carpet", "banner", "candle", "torch", "skull", "_head", "button", "pressure_plate", "flower_pot")


def _is_seal(c):
    """Does this template cell keep a room closed (is it a wall for air and for a mob)?"""
    if c is None:
        return False
    path = c.block.split(":", 1)[1]
    if path in _NOT_A_SEAL:
        return False
    if path == "jigsaw":
        return not str(c.nbt.get("final_state", "minecraft:air")).startswith("minecraft:air")
    return not (path in _THIN_EXACT or any(t in path for t in _THIN_PARTS))


def leak_check(mt, start, open_sockets=None):
    """Flood fill from the free cell `start` through every cell that is not a seal; returns the list of exits to the outside of the template that are NOT
    one of the declared open doorways. An empty list means the room is closed everywhere the design says it is (no gap in a wall or a roof, no void
    corner touching the hall). Open doorways are the sockets whose final state is air and whose hole was carved."""
    W, D, H = mt.W, mt.D, L.HEIGHT
    allowed = set()
    for side, offset, kw in mt.sockets:
        if kw["hole"] and kw["final_state"] == L.AIR:
            for depth in range(-1, L.WALL):
                for lat in range(offset - 1, offset + 2):
                    for row in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                        allowed.add(mt._lateral(side, offset, lat, depth, row))
    seen = {start}
    stack = [start]
    leaks = []
    while stack:
        x, y, z = stack.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen:
                continue
            if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                if (x, y, z) not in allowed and n not in allowed:
                    leaks.append(((x, y, z), n))
                continue
            if _is_seal(mt.t.get(*n)):
                continue
            seen.add(n)
            stack.append(n)
    return leaks, seen


def lane_check(mt, length=10, headroom=8):
    """The in-game self-test `guard.traits` needs a straight, clear strip of floor 3 wide and `length + 1` long with `headroom` free cells above it,
    searched exactly the way GuardSelfTests.findLane searches (every second cell from 3 in from the footprint's corner). Returns the first one or None."""
    y = L.FLOOR_ROW + 1

    def standable(x, z):
        floor = mt.t.get(x, y - 1, z)
        if floor is None or floor.block.endswith(":air"):
            return False
        for k in range(headroom):
            c = mt.t.get(x, y + k, z)
            if c is not None and not (c.block.endswith(":air") or c.block.endswith("structure_block") or c.block.endswith("_wall_banner")):
                return False
        return True

    for x in range(3, mt.W - 3, 2):
        for z in range(3, mt.D - 3, 2):
            for d in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ok = True
                for k in range(length + 1):
                    for lat in (-1, 0, 1):
                        xx, zz = x + d[0] * k + d[1] * lat, z + d[1] * k + d[0] * lat
                        if not (0 <= xx < mt.W and 0 <= zz < mt.D) or not standable(xx, zz):
                            ok = False
                            break
                    if not ok:
                        break
                if ok:
                    return (x, z, d)
    return None

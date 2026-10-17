#!/usr/bin/env python3
"""
Why: the grand and mechanical rooms of the Mega-Dungeon (1.54.0, ROOMS-2: the Cathedral Rotunda, round and colonnade halls, cisterns, lava halls, the
mirror hall, caverns, trap, puzzle and trial rooms) all need the same three things that `mega_lib.py` (which must not be edited) does not give them:
round geometry, architectural details (columns with bases and capitals, arches, cornices, braziers, candle fields) and a way to PROVE a room is
playable without a game. This module is that toolkit, so `gen_mega_rooms2.py` reads like a floor plan and not like a pile of loops.

The proof is the second half of the file: `Analysis` re-reads the finished template and answers the questions a player would ask. Can I walk from every
door to every other door, and is there a 3 wide way (a corridor of furniture is not a room)? Is every spawn marker on a floor I can reach? Where is it
darker than the gloom limit (block light 4 or less, `DndConfig.gloomLight`)? Can I look through a wall into the void? Does water or lava touch a
doorway, or have a side that is not sealed (a source block flows into any open neighbour)? Is anything flammable next to lava? It is a model, not the
game (a block is solid, ground, obstacle, fluid, light source or air), but it catches exactly the mistakes a generator makes: a pillar in a doorway, a
marker in a wall, a pool with a leak, a dark corner. The numbers it prints are in the docs.

Vanilla Block Primacy Rule: nothing here uses a custom block; the rooms that need the 1.36.0 trap blocks name them in `gen_mega_rooms2.py`.
"""
import math
import os
import random
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from mega_lib import Frame  # noqa: E402

FR = L.FLOOR_ROW

# mega_lib's block-state schema tries `.*_door` (the vanilla door properties) BEFORE its `nusmp:.*` rule, and it matches the path of `nusmp:gatekeeper_door`
# first, so the puzzle door's `kind` property would be rejected as "not a door property". The rule order is the lead's file (mega_lib.py must not be
# edited by a room builder), so this module puts the `nusmp:` rule first at import time; the one-line fix for mega_lib is in docs/labyrinth/mega_rooms2.md.
if not L._SCHEMA or L._SCHEMA[0][0] != r"nusmp:.*":
    L._SCHEMA.insert(0, (r"nusmp:.*", None))
    L._check_cached.cache_clear()


# =====================================================================================================================
# geometry
# =====================================================================================================================

def polar(cx, cz, r, deg):
    """The integer cell at radius r and angle deg (0 = +x, 90 = +z) round the cell (cx, cz)."""
    a = math.radians(deg)
    return int(round(cx + r * math.cos(a))), int(round(cz + r * math.sin(a)))


def dist(x, z, cx, cz):
    """Distance between two cells on the floor plan."""
    return math.hypot(x - cx, z - cz)


def in_disc(x, z, cx, cz, r):
    """Same rule as `mega_lib.disk_cells`: a cell belongs to the disc when its centre is within r + 0.5."""
    return (x - cx) ** 2 + (z - cz) ** 2 <= (r + 0.5) ** 2


def angle_of(x, z, cx, cz):
    """The angle in degrees (0 = +x, 90 = +z) at which cell (x, z) lies seen from (cx, cz), in [0, 360)."""
    return math.degrees(math.atan2(z - cz, x - cx)) % 360.0


def cells_between(cx, cz, r0, r1):
    """Cells with r0 < distance <= r1 (a ring of any thickness), inside the template caller's range: the caller clips."""
    out = []
    n = int(r1) + 2
    for z in range(cz - n, cz + n + 1):
        for x in range(cx - n, cx + n + 1):
            d = math.hypot(x - cx, z - cz)
            if r0 < d <= r1:
                out.append((x, z))
    return out


def around(cx, cz, r, count, start=0.0):
    """`count` evenly spaced cells on the circle of radius r, starting at angle `start` (degrees)."""
    return [polar(cx, cz, r, start + 360.0 * k / count) for k in range(count)]


def jitter(rng, p):
    """True with probability p: a coin the caller flips with the template's own seeded rng, so the result is reproducible."""
    return rng.random() < p


# =====================================================================================================================
# the template wrapper: a safe `put` that clips to the box and never raises for out-of-box decoration
# =====================================================================================================================

def inside(mt, x, y, z):
    """True when (x, y, z) lies inside the template box."""
    return 0 <= x < mt.W and 0 <= y < L.HEIGHT and 0 <= z < mt.D


def put(mt, x, y, z, block, props=None, nbt_=None, clip=True, skip_reserved=False):
    """`mt.set` that silently skips cells outside the box when clip is true (round rooms draw circles that may graze the edge). With `skip_reserved`
    a cell inside a doorway hole is skipped instead of raising: for fills of architecture (walls, floors) that pass through a doorway on purpose;
    decoration keeps the default so a pillar put in a doorway still fails loudly."""
    if clip and not inside(mt, x, y, z):
        return False
    if skip_reserved and (x, y, z) in mt.reserved:
        return False
    mt.set(x, y, z, block, props, nbt_)
    return True


def put_free(mt, x, y, z, block, props=None):
    """Only when the cell is still unset (decoration that must never overwrite architecture)."""
    if not inside(mt, x, y, z) or mt.get(x, y, z) is not None or (x, y, z) in mt.reserved:
        return False
    mt.set(x, y, z, block, props)
    return True


def is_air(mt, x, y, z):
    """True when the cell is unset or an air block: nothing there that decoration could overwrite."""
    c = mt.get(x, y, z)
    return c is None or c.block in (L.AIR, "minecraft:cave_air", "minecraft:void_air")


def stairs(facing, half="bottom"):
    """The block-state dict of a stair: `facing` is the direction of its TALL side (the way up the flight), `half` bottom or top."""
    return {"facing": facing, "half": half}


def slab(kind="bottom"):
    """The block-state dict of a slab of the given type (bottom, top or double)."""
    return {"type": kind}


# =====================================================================================================================
# architecture: columns, capitals, cornices, arches, braziers, candles
# =====================================================================================================================

def column(mt, x, z, y0, y1, shaft=L.POLISHED, base=True, cap=True, wall_shaft=False, cap_width=3, base_block=L.POLISHED, cap_block=L.CHISELED):
    """A column with a base and a capital from the palette. Rows y0..y1 inclusive; the shaft is a full block (stout) or a polished wall (slender).
    Base: a plinth block with a stair skirt; capital: the top row is a chiselled block flanked by four upside-down stairs flaring outward (a
    3x3 capital), so a column reads as a column and not as a stack of cubes. Skirt and flare only go into cells that are still unset."""
    for y in range(y0, y1 + 1):
        if wall_shaft and y0 < y < y1:
            put(mt, x, y, z, L.POLISHED_WALL, {"up": "true"})
        else:
            put(mt, x, y, z, shaft)
    if base:
        put(mt, x, y0, z, base_block)
        for dx, dz, f in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
            put_free(mt, x + dx, y0, z + dz, L.POLISHED_STAIRS, stairs(f))
    if cap:
        put(mt, x, y1, z, cap_block)
        if cap_width >= 3:
            # upside-down stairs whose full-height part is the side NEXT TO the shaft: the capital overhangs like a corbel and never leaves a notch at the shaft
            for dx, dz, f in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
                put_free(mt, x + dx, y1, z + dz, L.POLISHED_STAIRS, stairs(f, "top"))


def pilaster(mt, x, z, y0, y1, face, cap=True):
    """A flat column set against a wall: `face` is the direction it faces (away from the wall)."""
    for y in range(y0, y1 + 1):
        put(mt, x, y, z, L.POLISHED)
    put(mt, x, y0, z, L.CHISELED)
    if cap:
        put(mt, x, y1, z, L.CHISELED)
    dx, dz = L.STEP[face]
    toward = L.OPPOSITE[face]                    # the tall side of both stairs is the side against the pilaster
    if cap:
        put_free(mt, x + dx, y1, z + dz, L.POLISHED_STAIRS, stairs(toward, "top"))
    put_free(mt, x + dx, y0, z + dz, L.POLISHED_STAIRS, stairs(toward))


def hang_lantern(mt, x, y_top, z, drop=2, lantern=L.LANTERN):
    """A lantern on `drop` chain links under the cell (x, y_top) which must be solid."""
    for k in range(1, drop + 1):
        put(mt, x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    put(mt, x, y_top - drop - 1, z, lantern, {"hanging": "true"})


def hang_lantern_from_above(mt, x, y, z, lantern=L.LANTERN, reach=12):
    """Why: a hanging lantern must hang from something (vanilla's `canSupportCenter`), and a niche or an alcove has no ceiling at the height a builder
    wants its lantern at, so this one looks up: it puts the lantern at (x, y, z) and a chain in every free cell above it until it meets a block that can carry
    a hanging lantern (a full block, bars, a wall post, a chain) within `reach` cells. When nothing above can carry it, nothing is placed. Returns True when
    the lantern hangs."""
    if not inside(mt, x, y, z) or mt.get(x, y, z) is not None or (x, y, z) in mt.reserved:
        return False
    chain = []
    for k in range(1, reach + 1):
        if not inside(mt, x, y + k, z):
            return False
        c = mt.get(x, y + k, z)
        if c is None and (x, y + k, z) not in mt.reserved:
            chain.append(y + k)
            continue
        if c is not None and center_support(c, "down"):
            for yy in chain:
                mt.set(x, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
            mt.set(x, y, z, lantern, {"hanging": "true"})
            return True
        return False
    return False


def lamp_post(mt, x, y, z, height=3, lantern=L.LANTERN):
    """A standing lamp: a polished wall post `height` cells high with a lantern on its top (a wall's top face supports a lantern's centre). The lamp is the
    fixture that lights a floor from head height, which a lantern hung from a 14-row ceiling cannot (light loses one level per cell)."""
    for k in range(height):
        put(mt, x, y + k, z, L.POLISHED_WALL, {"up": "true"})
    put(mt, x, y + height, z, lantern, {"hanging": "false"})


def pillar_lantern(mt, px, pz, toward, y, lantern=L.LANTERN):
    """A lantern on a top-half slab corbel against the pillar at (px, pz), on its `toward` side, standing at row y + 1 (the slab at row y). Skipped when the
    cells are taken."""
    dx, dz = L.STEP[toward]
    x, z = px + dx, pz + dz
    if inside_cell_free(mt, x, y, z) and inside_cell_free(mt, x, y + 1, z):
        put(mt, x, y, z, L.POLISHED_SLAB, {"type": "top"})
        put(mt, x, y + 1, z, lantern, {"hanging": "false"})
        return True
    return False


def inside_cell_free(mt, x, y, z):
    """True when the cell is in the box, still unset and not part of a doorway hole."""
    return inside(mt, x, y, z) and mt.get(x, y, z) is None and (x, y, z) not in mt.reserved


def brazier(mt, x, y, z, fire="campfire", height=2, cage=True):
    """A standing brazier: a polished foot, wall posts, a slab bowl and a lit campfire in it, optionally in an iron-bar cage. (x, y, z) is the foot cell.
    Campfires are the vanilla flame that lights 15 and smokes, and the gloom clock already counts them as a light source."""
    put(mt, x, y, z, L.POLISHED)
    for k in range(1, height):
        put(mt, x, y + k, z, L.POLISHED_WALL, {"up": "true"})
    top = y + height
    put(mt, x, top, z, L.POLISHED_SLAB, {"type": "top"})
    put(mt, x, top + 1, z, "minecraft:%s" % fire, {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    if cage:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            put_free(mt, x + dx, top + 1, z + dz, L.IRON_BARS)


def candle_cluster(mt, x, y, z, rng, colors=("red", "orange", "yellow", "white", "pink", "purple", "light_blue"), count=None):
    """A lit candle block (1-4 candles) of a seeded colour on the full block under (x, y, z). Returns True when placed."""
    n = count or rng.choice((1, 1, 2, 2, 3, 4))
    return put_free(mt, x, y, z, "minecraft:%s_candle" % rng.choice(colors), {"candles": str(n), "lit": "true", "waterlogged": "false"})


def wall_candles(mt, x, y, z, rng, colors=("red", "orange", "white")):
    """Candles on a ledge or niche floor: the same call as `candle_cluster`, named for what the call site means."""
    return candle_cluster(mt, x, y, z, rng, colors)


def banner_on(mt, x, y, z, facing, color="red"):
    """A wall banner on the wall behind (x, y, z); facing = away from the wall. Skipped when the cell is taken."""
    return put_free(mt, x, y, z, "minecraft:%s_wall_banner" % color, {"facing": facing})


def cobweb(mt, x, y, z):
    """A cobweb in an unset cell (never over anything else)."""
    return put_free(mt, x, y, z, "minecraft:cobweb")


def light(mt, x, y, z, level=12):
    """An invisible light block in an unset cell (never over an existing block)."""
    return put_free(mt, x, y, z, "minecraft:light", {"level": str(level), "waterlogged": "false"})


def lit_floor_stone(rng, wear=0.12):
    """A floor tile of the palette: plain, or cracked with probability `wear` (seeded by the caller's rng)."""
    return L.CRACKED_TILE if rng.random() < wear else L.TILE


def arch(mt, f, span, jamb, depth=1, stair=L.POLISHED_STAIRS, keystone=L.CHISELED, carve=True):
    """A pointed stair arch in the vertical plane of Frame `f` (r = right, u = up, origin = the centre of the opening at its foot), `depth` cells deep.
    The opening is `span` (odd) wide and `jamb` rows high at the jambs; from row `jamb` up it narrows by one cell on each side per row (a 45 degree
    diagonal of upside-down stairs whose full side faces outward) until one cell is left, which a keystone closes: the arch rises (span-1)/2 + 1 rows
    over the jambs. With `carve` the free cells are removed first (the arch is cut into a wall that is already built); without it only the stairs and
    the keystone are written. Why one function: every colonnade, door, window and alcove of the straight halls uses this same arch, drawn once."""
    half = span // 2
    for fo in range(depth):
        if carve:
            for r in range(-half, half + 1):
                for u in range(0, jamb):
                    mt.remove(*f.at(r, u, fo))
            for k in range(half):
                w = half - 1 - k
                for r in range(-w, w + 1):
                    mt.remove(*f.at(r, jamb + k, fo))
        for k in range(half):
            r = half - k
            for sign, side in ((1, "right"), (-1, "left")):
                f.put(sign * r, jamb + k, fo, stair, {"facing": f.dir(side), "half": "top"})
        f.put(0, jamb + half - 1 + 1 if half > 0 else jamb, fo, keystone)
    return mt


# =====================================================================================================================
# dripstone, solid boxes, snake routes
# =====================================================================================================================

def dripstone(mt, x, y_root, z, length, direction="down"):
    """A natural-looking pointed dripstone column of `length` cells whose root cell is (x, y_root, z) and which grows `direction` ('down': a stalactite
    hanging from the ceiling cell above the root; 'up': a stalagmite standing on the floor cell below it). Thickness follows the vanilla rule: the cell at
    the root is `base`, the free end `tip`, the cell before the tip `frustum`, the rest `middle`. Cells that are already taken are left alone."""
    step = -1 if direction == "down" else 1
    for i in range(length):
        y = y_root + step * i
        j = length - 1 - i                    # distance from the tip
        thick = "tip" if j == 0 else ("base" if i == 0 and length > 1 else ("frustum" if j == 1 else "middle"))
        if length > 1 and i == 0:
            thick = "base"
        if not put_free(mt, x, y, z, "minecraft:pointed_dripstone", {"vertical_direction": "down" if direction == "down" else "up", "thickness": thick,
                                                                  "waterlogged": "false"}):
            return False
    return True


def solid_box(mt, x0, x1, y0, y1, z0, z1, block=L.BRICK, rng=None, wear=0.0):
    """Fills the inclusive box with `block` (cracked variants at `wear`), skipping cells inside a doorway hole."""
    rng = rng or mt.rng
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                if (x, y, z) in mt.reserved:
                    continue
                b = block
                if wear and block == L.BRICK and rng.random() < wear:
                    b = L.CRACKED_BRICK
                mt.set(x, y, z, b)


def carve(mt, x0, x1, y0, y1, z0, z1):
    """Removes (leaves unset = air) every cell of the inclusive box."""
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                mt.remove(x, y, z)


def snake_path(rng, start, goals, cells, min_len=14, max_tries=4000):
    """A thin one-wide route over `cells` (a set of (x, z)): a self-avoiding walk from `start` to any cell of `goals` that never touches itself sideways
    (no cell of the route is next to an earlier non-consecutive one), at least `min_len` long. Why: a pitfall field whose safe route is a thin zig-zag
    is a puzzle, a wide band would be luck. Deterministic for the rng; returns the list of cells (start first, goal last) or None."""
    cells = set(cells)
    goals = set(goals)
    sys.setrecursionlimit(max(sys.getrecursionlimit(), 5000))
    tries = [0]

    def walk(path, seen):
        """One step of the depth-first search: extends `path` until a goal cell is reached after `min_len` cells, backtracks when stuck and gives up after
        `max_tries` calls."""
        if tries[0] > max_tries:
            return None
        tries[0] += 1
        x, z = path[-1]
        if (x, z) in goals and len(path) >= min_len:
            return path
        order = [(1, 0), (-1, 0), (0, 1), (0, -1)]
        rng.shuffle(order)
        # prefer turning: sort so that continuing straight comes last when the walk has been straight for a while
        for dx, dz in order:
            n = (x + dx, z + dz)
            if n not in cells or n in seen:
                continue
            if n in goals and len(path) + 1 < min_len:
                continue
            touching = False
            for ex, ez in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                m = (n[0] + ex, n[1] + ez)
                if m in seen and m != (x, z):
                    touching = True
                    break
            if touching:
                continue
            seen.add(n)
            path.append(n)
            r = walk(path, seen)
            if r:
                return r
            path.pop()
            seen.discard(n)
        return None

    return walk([start], {start})


# =====================================================================================================================
# wall dressing in a Frame: windows, alcoves, sconces, banners (written once, correct for all four walls)
# =====================================================================================================================

def inner_frame(mt, side, lat, y=FR + 1):
    """A Frame on the first interior cell in front of the `side` wall ('n','e','s','w') at absolute lateral position `lat` (x for n/s, z for e/w), at row y,
    facing into the room: f = -1 is the inner wall layer, f = -2 the outer layer (the walls of every Mega template are two thick)."""
    x, yy, z = mt._lateral(side, 0, lat, L.WALL, y)
    facing = {"n": "south", "s": "north", "w": "east", "e": "west"}[side]
    return Frame(mt, x, yy, z, facing)


def wall_free(mt, side, lat, half=1):
    """True when the wall stretch lat-half .. lat+half on `side` clears every doorway of that side (a doorway with its two frame posts spans offset +- 2)."""
    for sd, off, _kw in mt.sockets:
        if sd == side and not (lat + half < off - 2 or lat - half > off + 2):
            return False
    return True


def lit_window(mt, f, width=1, y0=3, height=4, glass="minecraft:iron_bars", backing="minecraft:sea_lantern"):
    """A window that glows, in a two-thick wall: `glass` (bars or stained glass) in the inner layer and a solid light block behind it in the outer layer.
    Why a solid backing and not an invisible `light` block: the outer layer of a two-thick wall is the template's boundary, and a transparent cell
    there would let the room look out into the void (or into a neighbour's niche) - the leak test of `Analysis` would flag it."""
    half = width // 2
    lo, hi = (-half, half) if width % 2 else (-half, half - 1)      # an even width straddles the frame's centre cell: r = -half .. half - 1
    for r in range(lo, hi + 1):
        for u in range(y0, y0 + height):
            f.put(r, u, -1, glass)
            f.put(r, u, -2, backing)


def arched_alcove(mt, f, span=3, jamb=4):
    """A shallow arched niche carved into the inner wall layer in front of frame `f` (the outer layer stays as its back wall)."""
    arch(mt, f.sub(0, 0, -1), span, jamb, depth=1)


def alcove_statue(mt, f, gear=("iron", "iron_sword"), facing=None):
    """A statue in the arched niche in front of frame f: plinth, slab, armoured stand, lantern overhead (the niche is `jamb` 4 tall)."""
    x, y, z = f.at(0, 0, -1)
    head, chest, legs, feet = ("minecraft:%s_%s" % (gear[0], p_) for p_ in ("helmet", "chestplate", "leggings", "boots"))
    L.statue(mt, x, y, z, facing=facing or f.facing, head=head, chest=chest, legs=legs, feet=feet, weapon="minecraft:" + gear[1])
    hang_lantern_from_above(mt, *f.at(0, 4, -1))                 # over the statue's head, on a chain from the keystone of whatever arch the niche has


def alcove_sarcophagus(mt, f, skull="skeleton"):
    """A stone sarcophagus in the niche (3 wide): blocks, a slab lid, a skull at the head end and candles; the lid keeps the 4 rows of the niche open."""
    for r in (-1, 0, 1):
        f.put(r, 0, -1, L.POLISHED)
        f.put(r, 1, -1, L.POLISHED_SLAB if r != 1 else L.POLISHED, {"type": "bottom"} if r != 1 else None)    # the foot end is a whole block: a candle cannot stand on a slab
    f.put(-1, 2, -1, "minecraft:%s_skull" % skull, {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[f.facing])})
    f.put(1, 2, -1, "minecraft:white_candle", {"candles": "2", "lit": "true", "waterlogged": "false"})


def alcove_candles(mt, f, rng, colors=("red", "orange", "white")):
    """A niche with a low altar of three slabs and candle clusters of seeded colour on it."""
    for r in (-1, 0, 1):
        f.put(r, 0, -1, L.POLISHED)
        candle_cluster(mt, *f.at(r, 1, -1), rng, colors=colors, count=rng.choice((2, 3, 4)))


def sconce(mt, f, r, u, style="sconce", skull="wither_skeleton"):
    """A skull-lamp sconce on the wall in front of frame f at lateral r and height u (the skull cell is the interior cell in front of the wall)."""
    x, y, z = f.at(r, u, 0)
    L.skull_lamp(mt, x, y, z, facing=f.facing, style=style, skull=skull)


# =====================================================================================================================
# set dressing along the walls of rectangular rooms: the small things that make a room look used
# =====================================================================================================================

def wall_band(mt, depth=1, door_gap=5):
    """Candidate cells for props: [(x, z, facing)] on the interior ring of a rectangular room, `depth` cells in from the inner wall face, with `facing` the
    direction away from the nearest wall. Cells that are taken at floor + 1 or floor + 2, or that lie within `door_gap` (Manhattan) of a doorway's inner
    cell, are left out, so a prop never narrows the way through a door."""
    out = []
    anchors = []
    for side, off, _kw in mt.sockets:
        x, y, z = mt._lateral(side, off, off, L.WALL, FR + 1)
        anchors.append((x, z))
    x0, x1, z0, z1 = L.WALL, mt.W - 1 - L.WALL, L.WALL, mt.D - 1 - L.WALL
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            near = min(x - x0, x1 - x, z - z0, z1 - z)
            if near >= depth:
                continue
            if any(abs(x - ax) + abs(z - az) <= door_gap for (ax, az) in anchors):
                continue
            if mt.get(x, FR + 1, z) is not None or mt.get(x, FR + 2, z) is not None or mt.get(x, FR, z) is None:
                continue
            if mt.get(x, FR, z).block.startswith("minecraft:water") or (x, FR + 1, z) in mt.reserved:
                continue
            if x - x0 == near:
                f = "east"
            elif x1 - x == near:
                f = "west"
            elif z - z0 == near:
                f = "south"
            else:
                f = "north"
            out.append((x, z, f))
    return out


def prop_barrels(mt, x, z, facing, rng):
    """A stack of one to three barrels against the wall (the commonest cellar furniture, readable at a glance)."""
    n = rng.choice((1, 2, 2, 3))
    for k in range(n):
        put_free(mt, x, FR + 1 + k, z, "minecraft:barrel", {"facing": "up" if k == n - 1 else rng.choice(("up", "north", "south")), "open": "false"})
    return True


def prop_bones(mt, x, z, facing, rng):
    """A bone block with a skull on it and a second skull on the floor beside it: what is left of an earlier party."""
    put_free(mt, x, FR + 1, z, "minecraft:bone_block")
    put_free(mt, x, FR + 2, z, "minecraft:skeleton_skull", {"rotation": str(rng.randint(0, 15))})
    dx, dz = L.STEP[facing]
    put_free(mt, x + dx, FR + 1, z + dz, "minecraft:skeleton_skull", {"rotation": str(rng.randint(0, 15))})
    return True


def prop_rubble(mt, x, z, facing, rng):
    """A heap of cobbled deepslate: a block, a stair and one or two slabs fallen from the wall."""
    put_free(mt, x, FR + 1, z, L.COBBLED)
    dx, dz = L.STEP[facing]
    put_free(mt, x + dx, FR + 1, z + dz, L.COBBLED_STAIRS, {"facing": facing, "half": "bottom"})
    put_free(mt, x - dz, FR + 1, z + dx, L.COBBLED_SLAB, {"type": "bottom"})
    if rng.random() < 0.5:
        put_free(mt, x, FR + 2, z, L.COBBLED_SLAB, {"type": "bottom"})
    return True


def prop_candles(mt, x, z, facing, rng):
    """A polished plinth with a lit candle cluster of four on it (an altar of light)."""
    put_free(mt, x, FR + 1, z, L.POLISHED)
    candle_cluster(mt, x, FR + 2, z, rng, colors=("white", "yellow", "orange"), count=4)
    return True


def prop_rack(mt, x, z, facing, rng):
    """A weapon rack against the wall: a stand holding a weapon, on a plinth, a banner above it."""
    L.statue(mt, x, FR + 1, z, facing=facing, head="minecraft:iron_helmet", chest="minecraft:chainmail_chestplate", legs="minecraft:chainmail_leggings",
             feet="minecraft:iron_boots", weapon=rng.choice(("minecraft:iron_sword", "minecraft:iron_axe", "minecraft:stone_sword")))
    return True


def prop_cauldron(mt, x, z, facing, rng):
    """An empty cauldron on the floor."""
    put_free(mt, x, FR + 1, z, "minecraft:cauldron")
    return True


def dress_walls(mt, rng, props, count, depth=1, door_gap=5, spacing=4):
    """Places up to `count` props from `props` (a list of (weight, function)) on seeded cells of `wall_band`, no two closer than `spacing`. Returns how many."""
    cells = wall_band(mt, depth, door_gap)
    rng.shuffle(cells)
    placed = []
    total = sum(w for w, _f in props)
    for (x, z, f) in cells:
        if len(placed) >= count:
            break
        if any(abs(x - px) + abs(z - pz) < spacing for (px, pz) in placed):
            continue
        r = rng.random() * total
        for w, fn in props:
            r -= w
            if r < 0:
                break
        if fn(mt, x, z, f, rng):
            placed.append((x, z))
    return len(placed)


def corner_webs(mt, rng, prob=0.9, height=3):
    """Cobwebs in the four upper corners of a rectangular room (a diagonal tuft of cells just under the ceiling)."""
    top = None
    for y in range(L.HEIGHT - 1, FR, -1):
        c = mt.get(L.WALL, y, L.WALL)
        if c is not None:
            top = y
            break
    if top is None:
        return 0
    n = 0
    for (cx_, cz_, dx, dz) in ((L.WALL, L.WALL, 1, 1), (mt.W - 1 - L.WALL, L.WALL, -1, 1), (L.WALL, mt.D - 1 - L.WALL, 1, -1), (mt.W - 1 - L.WALL, mt.D - 1 - L.WALL, -1, -1)):
        if rng.random() > prob:
            continue
        for k in range(height):
            for (ox, oz) in ((0, 0), (dx, 0), (0, dz)):
                if put_free(mt, cx_ + ox * (1 if k else 0), top - 1 - k, cz_ + oz * (1 if k else 0), "minecraft:cobweb"):
                    n += 1
    return n


# =====================================================================================================================
# stairs along an arc (curved staircases) and spiral helpers
# =====================================================================================================================

def stair_facing(dx, dz):
    """The stair `facing` (the direction of the tall side, i.e. the way UP the flight) for a step that climbs toward (dx, dz)."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


# =====================================================================================================================
# caverns: a smooth 3-D noise cave carved in a solid box (stdlib only)
# =====================================================================================================================

def crc(text):
    """A stable 31-bit hash of a text (zlib.crc32): Python's own hash() changes from run to run, which would break reproducible templates."""
    import zlib
    return zlib.crc32(text.encode("utf-8")) & 0x7FFFFFFF


def _h3(ix, iy, iz, seed):
    """The random value in [0, 1) of one integer lattice point (an integer hash), the building block of `noise3`."""
    n = (ix * 374761393 + iy * 668265263 + iz * 1442695041 + seed * 2246822519) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    n ^= n >> 16
    return n / 4294967296.0


def _fade(t):
    """Smoothstep, so the noise has no visible creases along the lattice lines."""
    return t * t * (3.0 - 2.0 * t)


def noise3(x, y, z, seed):
    """Smooth value noise in [0, 1): trilinear interpolation of an integer-hash lattice with a smoothstep fade. Deterministic, no tables."""
    ix, iy, iz = math.floor(x), math.floor(y), math.floor(z)
    fx, fy, fz = _fade(x - ix), _fade(y - iy), _fade(z - iz)
    out = 0.0
    for dx in (0, 1):
        for dy in (0, 1):
            for dz in (0, 1):
                w = (fx if dx else 1 - fx) * (fy if dy else 1 - fy) * (fz if dz else 1 - fz)
                out += w * _h3(ix + dx, iy + dy, iz + dz, seed)
    return out


def fbm3(x, y, z, seed, octaves=3, scale=8.0):
    """Fractal sum of `noise3` (octave k at scale / 2^k, weight 2^-k), normalised to [0, 1)."""
    total, norm, amp, sc = 0.0, 0.0, 1.0, scale
    for k in range(octaves):
        total += amp * noise3(x / sc, y / sc, z / sc, seed + 101 * k)
        norm += amp
        amp *= 0.5
        sc *= 0.5
    return total / norm


class Cavern:
    """Why: an organic cave is a SHAPE problem the room kit does not solve (the kit builds boxes and circles). This class makes one from three simple
    ingredients, so that it is smooth, reproducible and provably open: (1) a footprint (union of blobs plus a corridor to every door, smoothed by a majority
    filter) whose inward distance `d2` says how deep inside the cave a column is; (2) a floor height field that is flat (row 8) at the doors and rolls gently
    elsewhere, relaxed so that neighbours differ by at most one block (every floor can be walked); (3) a dome rule: a cell above the floor is air while
    `d2 + noise >= shrink * (height - flat)`, so the cave narrows as it rises, its ceiling is high in the middle and low at the rim, and the walls lean in like
    the green cavern of the owner's map. Air that is not connected to the doors is filled in. Nothing here touches a template: `air` and `floor` are plain
    dicts the room builder turns into blocks."""

    def __init__(self, name, W, D, doors, blobs, shrink=0.42, flat=3.0, noise_amp=1.6, margin=4, floor_amp=2.2, max_row=44, seed=None, pools=()):
        """Builds the whole cave at once (outline, distance field, floor, air). `blobs` are the chambers [(cx, cz, rx, rz)], `doors` [(side, offset)], `pools`
        [(x, z, radius)] or ('auto', radius) ponds; the rest are the shape knobs explained in the class doc."""
        self.name, self.W, self.D = name, W, D
        self.pool_specs = list(pools)      # [(x, z, radius)]: shallow ponds, one block deep, with a flat bank round them
        self.pool = set()
        self.seed = crc(name) if seed is None else seed
        self.rng = random.Random(self.seed)
        self.margin, self.shrink, self.flat, self.noise_amp, self.floor_amp, self.max_row = margin, shrink, flat, noise_amp, floor_amp, max_row
        self.doors = doors                 # [(side, offset)]
        self.lanes = {}                    # (x, z) -> depth: the straight approach cells inside each door (kept at row 8, 5 wide at the mouth)
        self._lanes()
        self.mask = self._footprint(blobs)
        self.d2 = self._distance()
        self.floor = self._floor_field()
        self.air = self._carve()

    # ---- the door approaches
    def _cell(self, side, offset, lat, depth):
        """The (x, z) cell at lateral position `lat` along the `side` wall and `depth` cells in from its outer face."""
        if side == "n":
            return (lat, depth)
        if side == "s":
            return (lat, self.D - 1 - depth)
        if side == "w":
            return (depth, lat)
        return (self.W - 1 - depth, lat)

    def _lanes(self):
        """Marks the straight approach inside every door: depth 2 to 7, 3 wide at depth 2 and 5 wide from depth 3, kept at the door's floor row and open up to
        floor + 6, so a doorway is never buried in rock."""
        for side, off in self.doors:
            for depth in range(2, 8):
                half = 1 if depth < 3 else 2
                for lat in range(off - half, off + half + 1):
                    self.lanes[self._cell(side, off, lat, depth)] = depth

    def lane_anchor(self, side, off):
        """The standing cell just inside a door on its centre line (depth 2): where the routes of the analysis start."""
        return self._cell(side, off, off, 2)

    # ---- the footprint
    def _footprint(self, blobs):
        """The cave outline: the union of the wobbled ellipses `blobs`, a corridor from every door lane to the nearest chamber, the lanes themselves, rounded
        by a 3 x 3 majority filter applied twice and kept `margin` cells off the box edge."""
        m = self.margin
        mask = [[False] * self.D for _ in range(self.W)]
        for (bx, bz, rx, rz) in blobs:
            for z in range(self.D):
                for x in range(self.W):
                    if m <= x < self.W - m and m <= z < self.D - m:
                        dx, dz = (x - bx) / rx, (z - bz) / rz
                        wob = 1.0 + 0.28 * (noise3(x / 5.0, 0.5, z / 5.0, self.seed) - 0.5) * 2
                        if dx * dx + dz * dz <= wob * wob:
                            mask[x][z] = True
        # a corridor from each door lane to the nearest blob centre (so every door opens into the cave)
        centres = [(b[0], b[1]) for b in blobs]
        for side, off in self.doors:
            ax, az = self.lane_anchor(side, off)
            cx, cz = min(centres, key=lambda c: (c[0] - ax) ** 2 + (c[1] - az) ** 2)
            x, z = float(ax), float(az)
            steps = int(max(abs(cx - ax), abs(cz - az))) + 1
            for k in range(steps + 1):
                t = k / max(1, steps)
                px, pz = ax + (cx - ax) * t, az + (cz - az) * t
                wob = 1.2 * math.sin(t * 5.0 + self.seed % 7)
                if side in ("n", "s"):
                    px += wob
                else:
                    pz += wob
                for dz in range(-2, 3):
                    for dx in range(-2, 3):
                        if dx * dx + dz * dz <= 5:
                            xx, zz = int(round(px)) + dx, int(round(pz)) + dz
                            if 2 <= xx < self.W - 2 and 2 <= zz < self.D - 2:
                                mask[xx][zz] = True
        for (x, z) in self.lanes:
            mask[x][z] = True
        # majority filter (twice) to round the outline, then re-assert the lanes
        for _ in range(2):
            new = [[False] * self.D for _ in range(self.W)]
            for z in range(1, self.D - 1):
                for x in range(1, self.W - 1):
                    n = sum(1 for dz in (-1, 0, 1) for dx in (-1, 0, 1) if mask[x + dx][z + dz])
                    new[x][z] = n >= 5 if (m <= x < self.W - m and m <= z < self.D - m) else False
            mask = new
            for (x, z) in self.lanes:
                mask[x][z] = True
        return mask

    def _distance(self):
        """Chamfer distance of every footprint cell to the nearest non-footprint cell (inward depth)."""
        INF = 9999.0
        d = [[0.0 if not self.mask[x][z] else INF for z in range(self.D)] for x in range(self.W)]
        for z in range(self.D):
            for x in range(self.W):
                if d[x][z] > 0:
                    for dx, dz, w in ((-1, 0, 1.0), (0, -1, 1.0), (-1, -1, 1.4), (1, -1, 1.4)):
                        xx, zz = x + dx, z + dz
                        if 0 <= xx < self.W and 0 <= zz < self.D:
                            d[x][z] = min(d[x][z], d[xx][zz] + w)
        for z in range(self.D - 1, -1, -1):
            for x in range(self.W - 1, -1, -1):
                if d[x][z] > 0:
                    for dx, dz, w in ((1, 0, 1.0), (0, 1, 1.0), (1, 1, 1.4), (-1, 1, 1.4)):
                        xx, zz = x + dx, z + dz
                        if 0 <= xx < self.W and 0 <= zz < self.D:
                            d[x][z] = min(d[x][z], d[xx][zz] + w)
        return d

    # ---- the floor
    def _floor_field(self):
        """The floor height of every footprint cell: row 8 on the lanes, a gentle noise field elsewhere (flat near the doors), ponds one block lower with a
        flat bank, relaxed so neighbours differ by at most one block."""
        base = FR
        f = {}
        lane_cells = list(self.lanes)
        for z in range(self.D):
            for x in range(self.W):
                if not self.mask[x][z]:
                    continue
                if (x, z) in self.lanes:
                    f[(x, z)] = base
                    continue
                dl = min((abs(x - lx) + abs(z - lz) for (lx, lz) in lane_cells), default=99)
                w = min(1.0, max(0.0, (dl - 3) / 7.0))
                off = (fbm3(x, 0.0, z, self.seed + 7, 3, 9.0) - 0.5) * 2 * self.floor_amp * (1.0 if self.d2[x][z] > 2 else 0.5)
                f[(x, z)] = base + int(round(off * w))
        # ponds: a flat bank (row 8) round a pool one block lower, only where the footprint is deep enough to hold the whole thing; both are locked
        locked = set(self.lanes)
        specs = []
        taken = []
        for spec in self.pool_specs:
            if spec[0] == "auto":
                # the deepest footprint cell that is clear of the door lanes and of the ponds already placed
                best = None
                for (x, z) in sorted(f):
                    if self.d2[x][z] < spec[1] + 3 or any(abs(x - lx) + abs(z - lz) < spec[1] + 9 for (lx, lz) in lane_cells):
                        continue
                    if any(math.hypot(x - tx, z - tz) < spec[1] + tr + 6 for (tx, tz, tr) in taken):
                        continue
                    score = self.d2[x][z] - 0.01 * abs(x - self.W / 2) - 0.01 * abs(z - self.D / 2)
                    if best is None or score > best[0]:
                        best = (score, x, z)
                if best is not None:
                    specs.append((best[1], best[2], spec[1]))
                    taken.append((best[1], best[2], spec[1]))
            else:
                specs.append(spec)
                taken.append(spec)
        for (px, pz, pr) in specs:
            cells = [(x, z) for z in range(self.D) for x in range(self.W) if math.hypot(x - px, z - pz) <= pr + 1.8]
            if not cells or not all((x, z) in f and self.d2[x][z] >= 1.5 for (x, z) in cells):
                continue
            for (x, z) in cells:
                if (x, z) in self.lanes:
                    break
            else:
                for (x, z) in cells:
                    inside_pool = math.hypot(x - px, z - pz) <= pr
                    f[(x, z)] = base - 1 if inside_pool else base
                    locked.add((x, z))
                    if inside_pool:
                        self.pool.add((x, z))
        # relax: neighbours differ by at most one block; the lanes and the ponds stay as they are
        for _ in range(30):
            changed = False
            for (x, z), h in sorted(f.items()):
                if (x, z) in locked:
                    continue
                for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    n = f.get((x + dx, z + dz))
                    if n is not None and abs(n - h) > 1:
                        f[(x, z)] = h = n + (1 if h > n else -1)
                        changed = True
            if not changed:
                break
        return f

    # ---- the air
    def _carve(self):
        """The air of each column from its floor up: a cell is open while the inward distance plus noise is at least the dome rule, the first closed cell is
        the ceiling, columns with fewer than three free rows are dropped and the rest is filtered for connection to a door."""
        air = {}
        for (x, z), fy in sorted(self.floor.items()):
            d2 = self.d2[x][z]
            col = []
            for y in range(fy + 1, self.max_row + 1):
                h = y - fy
                n = fbm3(x, y, z, self.seed + 31, 3, 6.0) - 0.5
                need = self.shrink * max(0.0, h - self.flat)
                if (x, z) in self.lanes and h <= 6:
                    col.append(y)
                    continue
                if d2 + 2.0 * self.noise_amp * n >= need and d2 > 0.9:
                    col.append(y)
                else:
                    break                      # the first closed cell is the ceiling: nothing above it is air (no floating pockets)
            if len(col) >= 3 or (x, z) in self.lanes:
                air[(x, z)] = col
        return self._keep_connected(air)

    def _keep_connected(self, air):
        """Drops columns that are not connected to a door lane through cells of at least 3 free rows (a pocket you cannot reach is just a hole)."""
        starts = []
        for side, off in self.doors:
            a = self.lane_anchor(side, off)
            if a in air:
                starts.append(a)
        seen = set(starts)
        stack = list(starts)
        while stack:
            x, z = stack.pop()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in air and n not in seen:
                    # connected when the two columns share at least 3 free rows
                    a, b = set(air[(x, z)]), set(air[n])
                    if len(a & b) >= 3:
                        seen.add(n)
                        stack.append(n)
        return {k: v for k, v in air.items() if k in seen}

    # ---- queries
    def free(self, x, y, z):
        """True when (x, y, z) is an air cell of the cave."""
        col = self.air.get((x, z))
        return col is not None and col[0] <= y <= col[-1]

    def floor_row(self, x, z):
        """The row of the first air cell of the column (the floor is the row below), or None outside the cave."""
        return self.floor.get((x, z)) if (x, z) in self.air else None

    def ceiling_row(self, x, z):
        """The first solid row over the column, or None outside the cave."""
        col = self.air.get((x, z))
        return None if col is None else col[-1] + 1

    def columns(self):
        """Every air column (x, z) of the cave, sorted, so builders iterate deterministically."""
        return sorted(self.air)


# =====================================================================================================================
# the analysis: a model of the game rules good enough to find the mistakes a generator makes
# =====================================================================================================================

PASSABLE_RE = ("air", "cave_air", "void_air", "structure_block", "jigsaw", "light", "chain", "lantern", "soul_lantern", "cobweb", "vine", "glow_lichen", "sculk_vein",
               "moss_carpet", "wall_torch", "soul_wall_torch", "torch", "soul_torch", "end_rod", "ladder", "cave_vines", "cave_vines_plant", "weeping_vines",
               "weeping_vines_plant", "twisting_vines", "twisting_vines_plant", "hanging_roots", "spore_blossom", "short_grass", "tall_grass", "fern", "large_fern",
               "dead_bush", "brown_mushroom", "red_mushroom", "crimson_fungus", "warped_fungus", "crimson_roots", "warped_roots", "nether_sprouts", "small_dripleaf",
               "lily_pad", "tripwire", "tripwire_hook", "redstone_wire", "string", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "bamboo")
PASSABLE_SUFFIX = ("_carpet", "_pressure_plate", "_wall_banner", "_banner", "_wall_skull", "_wall_head", "_skull", "_head", "_candle", "_button", "_sign",
                   "_wall_sign", "_hanging_sign", "_amethyst_bud", "amethyst_cluster", "_sapling", "_tulip", "_orchid", "_bush", "_roots")
GROUND_PARTIAL_SUFFIX = ("_slab", "_stairs", "_trapdoor", "_bed")
OBSTACLE_SUFFIX = ("_fence", "_fence_gate", "_wall", "_cauldron", "anvil")
OBSTACLE_NAMES = ("iron_bars", "cauldron", "water_cauldron", "lava_cauldron", "grindstone", "brewing_stand", "enchanting_table", "lectern", "chest", "trapped_chest",
                  "ender_chest", "pointed_dripstone", "azalea", "flowering_azalea", "big_dripleaf", "bell", "cake", "composter", "flower_pot", "conduit", "heavy_core",
                  "dragon_egg", "campfire", "soul_campfire", "fire", "soul_fire", "magma_block", "lava", "cactus", "sweet_berry_bush", "scaffolding", "armor_stand",
                  "lightning_rod")
FLUIDS = ("water", "lava")


def _path(b):
    """The block id without its namespace."""
    return b.split(":", 1)[1] if ":" in b else b


def classify(cell, open_gates=False):
    """'air' (walk through), 'solid' (full block), 'ground' (partial block you can stand on), 'obstacle' (blocks walking, not a floor), 'fluid', 'hazard'.
    `open_gates` treats every Gatekeeper door as open (a solved puzzle)."""
    if cell is None:
        return "air"
    b = cell.block
    n = _path(b)
    if b.startswith("nusmp:"):
        if n == "sigil_plate":
            return "ground"                                     # a 2-pixel plate on the floor
        if n == "gatekeeper_door":
            return "solid" if (cell.props.get("open", "false") == "false" and not open_gates) else "air"
        if n == "relic_pedestal":
            return "obstacle"
        return "solid"                                          # pitfall tile, glyphs, rune, keystone, riddle stone, barrier: full cubes
    if n in FLUIDS:
        return "fluid"
    if n in ("fire", "soul_fire", "magma_block", "campfire", "soul_campfire", "cactus", "sweet_berry_bush", "lava_cauldron"):
        return "hazard"
    if n in PASSABLE_RE or n.endswith(PASSABLE_SUFFIX):
        if n.endswith("_trapdoor"):
            return "ground"
        return "air"
    if n.endswith("_trapdoor") and cell.props.get("open") == "true":
        return "obstacle"
    if n.endswith("_door") and "trapdoor" not in n:
        return "air" if cell.props.get("open") == "true" else "obstacle"
    if n.endswith(GROUND_PARTIAL_SUFFIX):
        return "ground"
    if n.endswith(OBSTACLE_SUFFIX) or n in OBSTACLE_NAMES:
        return "obstacle"
    if n.endswith("glass_pane") or n == "iron_bars":
        return "obstacle"
    return "solid"


def is_transparent(cell):
    """Does light and sight pass through the cell?"""
    if cell is None:
        return True
    n = _path(cell.block)
    if cell.block.startswith("nusmp:"):
        return n in ("sigil_plate", "magic_barrier", "relic_pedestal", "pitfall_tile") or (n == "gatekeeper_door" and cell.props.get("open") == "true")
    if n in ("glass", "tinted_glass", "water", "ice", "light", "chain") or n.endswith("glass") or n.endswith("glass_pane") or n.endswith("_leaves") or n in ("iron_bars", "cobweb"):
        return n != "tinted_glass"
    return classify(cell) not in ("solid",)


def light_emission(cell):
    """Why: the gloom limit is about block light, so the analysis needs what every block gives off; this is the vanilla table (lantern 15, torch 14, soul
    lantern 10, candles 3 per candle, glow lichen 7, amethyst cluster 5, light block = its level ...) plus the mod's own lamps and glyphs in one place."""
    if cell is None:
        return 0
    n = _path(cell.block)
    p = cell.props
    if cell.block.startswith("nusmp:"):
        if n == "sigil_plate":
            return 9 if p.get("lit") == "true" else 2
        return {"labyrinth_lantern": 15, "labyrinth_glyph": 8, "magicule_vein": 6, "ward_rune": 3, "spawn_glyph": 3, "magic_barrier": 7, "sealed_archway": 4,
                "guardian_altar": 5}.get(n, 0)
    if n == "light":
        return int(p.get("level", "15"))
    if n in ("lantern", "sea_lantern", "glowstone", "shroomlight", "jack_o_lantern", "lava", "fire", "beacon", "conduit") or n.endswith("froglight"):
        return 15
    if n in ("torch", "wall_torch", "end_rod"):
        return 14
    if n in ("soul_lantern", "soul_torch", "soul_wall_torch", "soul_fire", "crying_obsidian"):
        return 10
    if n in ("campfire",):
        return 15 if p.get("lit", "true") == "true" else 0
    if n in ("soul_campfire",):
        return 10 if p.get("lit", "true") == "true" else 0
    if n.endswith("candle") and p.get("lit") == "true":
        return 3 * int(p.get("candles", "1"))
    if n in ("glow_lichen",):
        return 7
    if n in ("magma_block",):
        return 3
    if n == "amethyst_cluster":
        return 5
    if n == "large_amethyst_bud":
        return 4
    if n == "medium_amethyst_bud":
        return 2
    if n == "small_amethyst_bud":
        return 1
    if n in ("furnace", "blast_furnace", "smoker") and p.get("lit") == "true":
        return 13
    if n in ("cave_vines", "cave_vines_plant") and p.get("berries") == "true":
        return 14
    if n == "enchanting_table":
        return 7
    if n == "lava_cauldron":
        return 15
    if n == "sculk_catalyst":
        return 6
    return 0


# =====================================================================================================================
# attachment rules: the blocks that fall off at the first neighbour update when nothing holds them
# =====================================================================================================================
# Why: a template is placed with "known shape" (no neighbour updates), so a lantern hung from nothing or a ladder with no wall behind it looks fine until a
# player breaks a block next to it and it drops as an item. The rules below are the `canSurvive` rules of the vanilla blocks this generator uses, so the
# check finds every such block before the game does. A rule that is only a vanilla preference (a small mushroom wants darkness) is reported as a warning.

DIRS = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}
FLIP = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "down", "down": "up"}
#: the vanilla `minecraft:dirt` block tag (what plants, azaleas and roots may stand on)
DIRT_TAG = ("grass_block", "dirt", "coarse_dirt", "podzol", "mycelium", "rooted_dirt", "moss_block", "mud", "muddy_mangrove_roots")


def face_sturdy(cell, face):
    """`BlockState.isFaceSturdy(.., face)`: the side of `cell` that looks toward `face` is a whole square (a full block, the flat side of a slab, the tall back
    or the flat top or bottom of a stair). Air, plants, walls, panes, bars and every other thin block are not."""
    if cell is None:
        return False
    if classify(cell) == "solid":
        return True
    n = _path(cell.block)
    p = cell.props
    if cell.block.startswith("nusmp:"):
        return False
    if n == "magma_block":
        return True                                       # a full cube that `classify` files under hazards (it burns the feet, it still holds a torch)
    if n.endswith("_slab"):
        t = p.get("type", "bottom")
        return t == "double" or (t == "top" and face == "up") or (t == "bottom" and face == "down")
    if n.endswith("_stairs"):
        shape = p.get("shape", "straight")
        if face == p.get("facing") and not shape.startswith("outer"):
            return True                                   # the tall back of a stair is a full face
        return (p.get("half", "bottom") == "bottom" and face == "down") or (p.get("half", "bottom") == "top" and face == "up")
    return False


def center_support(cell, face):
    """`Block.canSupportCenter(.., face)`: the middle of the `face` side of `cell` can carry a lantern, a candle or a torch: a whole face does, and so do the
    thin posts of a wall, a fence, iron bars and a chain (a lantern hangs from a chain and stands on a wall post). A fence gate does not hold a hanging block."""
    if cell is None:
        return False
    if face_sturdy(cell, face):
        return True
    n = _path(cell.block)
    if n.endswith("_fence_gate"):
        return False
    return n.endswith(("_wall", "_fence")) or n in ("iron_bars", "chain")


def solid_for_banner(cell):
    """`BlockState.isSolid()` as `WallBannerBlock.canSurvive` uses it: a block whose collision box is nearly a whole cube (a full block or a stair)."""
    if cell is None:
        return False
    return classify(cell) == "solid" or _path(cell.block).endswith("_stairs")


def attachment_problem(cells, pos, cell):
    """None when the block at `pos` is held the way vanilla needs, else (severity, text): severity 'error' when the block would pop off at the first update,
    'warn' for a rule vanilla only prefers. `cells` is the template's cell dict."""
    n = _path(cell.block)
    p = cell.props
    if cell.block.startswith("nusmp:"):
        return None

    def at(direction):
        dx, dy, dz = DIRS[direction]
        return cells.get((pos[0] + dx, pos[1] + dy, pos[2] + dz))

    def err(text):
        return ("error", text)

    if n == "ladder":
        back = at(FLIP[p["facing"]])
        if not face_sturdy(back, p["facing"]):
            return err("ladder with nothing solid behind it (%s)" % (back.block if back else "air"))
    elif n.endswith("_wall_banner"):
        back = at(FLIP[p["facing"]])
        if not solid_for_banner(back):
            return err("wall banner with nothing solid behind it (%s)" % (back.block if back else "air"))
    elif n in ("wall_torch", "soul_wall_torch", "redstone_wall_torch"):
        back = at(FLIP[p["facing"]])
        if not face_sturdy(back, p["facing"]):
            return err("wall torch with nothing sturdy behind it (%s)" % (back.block if back else "air"))
    elif n in ("torch", "soul_torch", "redstone_torch"):
        if not center_support(at("down"), "up"):
            return err("torch standing on nothing sturdy")
    elif n in ("lantern", "soul_lantern"):
        if p.get("hanging", "false") == "true":
            if not center_support(at("up"), "down"):
                return err("hanging lantern with nothing to hang from (%s)" % (at("up").block if at("up") else "air"))
        elif not center_support(at("down"), "up"):
            below = at("down")
            return err("standing lantern on nothing sturdy (%s %s)" % (below.block if below else "air", dict(below.props) if below else ""))
    elif n == "candle" or n.endswith("_candle"):
        below = at("down")
        if not center_support(below, "up"):
            return err("candle on nothing sturdy (%s %s)" % (below.block if below else "air", dict(below.props) if below else ""))
    elif n.endswith("_carpet") or n == "moss_carpet":
        below = at("down")
        if below is None or _path(below.block) in ("air", "cave_air", "void_air"):
            return err("carpet over nothing")
    elif n == "pointed_dripstone":
        d = p.get("vertical_direction", "up")
        root = at("up" if d == "down" else "down")
        ok = root is not None and (face_sturdy(root, d) or (_path(root.block) == "pointed_dripstone" and root.props.get("vertical_direction") == d))
        if not ok:
            return err("dripstone (%s) with no rock to grow from (%s)" % (d, root.block if root else "air"))
    elif n in ("amethyst_cluster", "large_amethyst_bud", "medium_amethyst_bud", "small_amethyst_bud"):
        back = at(FLIP[p["facing"]])
        if not face_sturdy(back, p["facing"]):
            return err("amethyst growth with nothing solid behind it (%s)" % (back.block if back else "air"))
    elif n == "vine":
        bad = []
        above = at("up")
        for d in ("north", "south", "east", "west"):
            if p.get(d) == "true":
                held = face_sturdy(at(d), FLIP[d]) or (above is not None and _path(above.block) == "vine" and above.props.get(d) == "true")
                if not held:
                    bad.append(d)
        if p.get("up") == "true" and not face_sturdy(above, "down"):
            bad.append("up")
        if bad:
            return err("vine face(s) %s held by nothing" % ",".join(bad))
        if not any(p.get(d) == "true" for d in ("north", "south", "east", "west", "up")):
            return err("vine with no face")
    elif n in ("glow_lichen", "sculk_vein"):
        faces = [d for d in DIRS if p.get(d) == "true"]
        bad = [d for d in faces if not face_sturdy(at(d), FLIP[d])]
        if bad or not faces:
            return err("%s face(s) %s held by nothing" % (n, ",".join(bad) or "(none set)"))
    elif n == "hanging_roots":
        above = at("up")
        if not face_sturdy(above, "down"):
            return err("hanging roots under nothing solid (%s)" % (above.block if above else "air"))
    elif n in ("cave_vines", "cave_vines_plant"):
        above = at("up")
        if not (above is not None and (_path(above.block) in ("cave_vines", "cave_vines_plant") or face_sturdy(above, "down"))):
            return err("cave vines hanging from nothing (%s)" % (above.block if above else "air"))
    elif n == "spore_blossom":
        if not center_support(at("up"), "down"):
            return err("spore blossom hanging from nothing sturdy")
    elif n in ("short_grass", "tall_grass", "fern", "large_fern", "azalea", "flowering_azalea", "warped_roots", "crimson_roots", "nether_sprouts", "warped_fungus",
               "crimson_fungus"):
        below = at("down")
        ok = below is not None and _path(below.block) in DIRT_TAG + ("farmland",)
        if n in ("azalea", "flowering_azalea"):
            ok = ok or (below is not None and _path(below.block) == "clay")
        if n in ("warped_roots", "crimson_roots", "nether_sprouts", "warped_fungus", "crimson_fungus"):
            ok = ok or (below is not None and _path(below.block) in ("warped_nylium", "crimson_nylium", "soul_soil"))
        if not ok:
            return err("%s on %s" % (n, below.block if below else "air"))
    elif n in ("brown_mushroom", "red_mushroom"):
        below = at("down")
        if below is None or _path(below.block) not in ("mycelium", "podzol", "crimson_nylium", "warped_nylium"):
            return ("warn", "small mushroom on %s (vanilla keeps it only in light under 13 there)" % (below.block if below else "air"))
    elif n == "lily_pad":
        below = at("down")
        if below is None or _path(below.block) != "water":
            return err("lily pad not on water (%s)" % (below.block if below else "air"))
    elif n.endswith("_pressure_plate"):
        if not face_sturdy(at("down"), "up"):
            return err("pressure plate on nothing sturdy")
    return None


class Analysis:
    """Why: the generator cannot run the game, but it still has to prove that the rooms it writes can be played, so this class reads a finished template back
    as a small model of the game rules (what can be stood on, walked through, seen through, what burns, what the light does) and answers the questions the
    checks ask: can every doorway reach every other, is there a three-wide route, can the shell be seen through from inside, does every water and lava cell
    have a wall, how dark is the floor. It is a model: the game's own light, fluid and collision code is the authority, and docs/labyrinth/mega_rooms2.md
    lists that as unverified. Build it once per template, ask questions."""

    def __init__(self, mt, open_gates=False):
        """Reads the template's cells and finds the anchor, hole cells and jigsaw cell of every doorway."""
        self.mt = mt
        self.open_gates = open_gates
        self.cells = mt.t.cells
        self.W, self.D = mt.W, mt.D
        self.cls_cache = {}
        self.doors = []                                   # per socket: dict(side, offset, anchor=(x, y, z) first standing cell inside, cells=hole cells)
        for side, offset, kw in mt.sockets:
            self.doors.append(self._door(side, offset))

    # ---- cells
    def cls(self, x, y, z):
        """The class of a cell ('air', 'solid', 'ground', 'obstacle', 'fluid', 'hazard'): cells outside the box count as solid, unset cells inside it as air.
        Cached per block state."""
        c = self.cells.get((x, y, z))
        if c is None:
            return "air" if (0 <= x < self.W and 0 <= y < L.HEIGHT and 0 <= z < self.D) else "solid"
        k = (c.block, tuple(sorted(c.props.items())))
        v = self.cls_cache.get(k)
        if v is None:
            v = classify(c, self.open_gates)
            self.cls_cache[k] = v
        return v

    def free(self, x, y, z):
        """Passable: you can be in the cell (air-like, and not a hazard or a fluid)."""
        return self.cls(x, y, z) == "air"

    def ground(self, x, y, z):
        """True when the cell can be stood on: a full block, or a partial one such as a slab, stair or trapdoor."""
        return self.cls(x, y, z) in ("solid", "ground")

    def standable(self, x, y, z):
        """A standing cell: the cell and the one above are free and the cell below is ground."""
        return self.free(x, y, z) and self.free(x, y + 1, z) and self.ground(x, y - 1, z)

    def _door(self, side, offset):
        """The description of one doorway: side, offset, the standing cell just inside it (anchor), its hole cells and its jigsaw cell."""
        mt = self.mt
        # the hole cells (depth 0..1); the anchor is the floor+1 cell at depth 2 (inside the room) on the socket's centre line
        ax, ay, az = mt._lateral(side, offset, offset, 2, L.DOOR_ROW)
        return {"side": side, "offset": offset, "anchor": (ax, ay, az), "cells": mt._hole_cells(side, offset), "jig": mt.socket_cell(side, offset)}

    # ---- walking
    def neighbours(self, node):
        """The standing cells reachable in one step from `node`, four directions: same level, one up (a jump, with head room) or one to three down (a drop
        onto a floor)."""
        x, y, z = node
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            # same level
            if self.standable(nx, y, nz):
                yield (nx, y, nz)
                continue
            # one up (a jump: head room above the start)
            if self.free(x, y + 2, z) and self.standable(nx, y + 1, nz) and self.free(nx, y + 2, nz):
                yield (nx, y + 1, nz)
                continue
            # down 1..3
            for dy in (1, 2, 3):
                if self.free(nx, y, nz) and self.free(nx, y + 1, nz):
                    if self.standable(nx, y - dy, nz) and all(self.free(nx, yy, nz) for yy in range(y - dy, y)):
                        yield (nx, y - dy, nz)
                        break
                else:
                    break

    def reach(self, start, allow=None, limit=400000):
        """BFS over standing nodes from `start`; `allow(node)` can veto nodes. Returns {node: previous}."""
        if not self.standable(*start) or (allow is not None and not allow(start)):
            return {}
        prev = {start: None}
        q = deque([start])
        while q and len(prev) < limit:
            n = q.popleft()
            for m in self.neighbours(n):
                if m not in prev and (allow is None or allow(m)):
                    prev[m] = n
                    q.append(m)
        return prev

    def anchors(self):
        """One standing cell per doorway (the nominal one, or one block above or below it, or two above), None where none exists: the start of every route
        test."""
        out = []
        for d in self.doors:
            a = d["anchor"]
            # the anchor may stand one block off the nominal floor (a stepped approach): search a short column
            for dy in (0, 1, -1, 2):
                if self.standable(a[0], a[1] + dy, a[2]):
                    out.append((a[0], a[1] + dy, a[2]))
                    break
            else:
                out.append(None)
        return out

    def wide(self, node, radius=1):
        """A node is 'wide' when every cell within `radius` (Chebyshev) has a standing node at the same height +-1."""
        x, y, z = node
        for dx in range(-radius, radius + 1):
            for dz in range(-radius, radius + 1):
                ok = False
                for dy in (0, 1, -1):
                    if self.standable(x + dx, y + dy, z + dz):
                        ok = True
                        break
                if not ok:
                    return False
        return True

    # ---- light
    def light_map(self):
        """Block light of every cell of the box (a dict for the non-zero ones), by a BFS from the emitters: -1 per cell, opaque cells stop it."""
        lv = {}
        q = deque()
        for pos, c in self.cells.items():
            e = light_emission(c)
            if e > 0:
                lv[pos] = max(lv.get(pos, 0), e)
        for pos, e in sorted(lv.items()):
            q.append(pos)
        while q:
            pos = q.popleft()
            e = lv[pos]
            if e <= 1:
                continue
            x, y, z = pos
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                np_ = (x + dx, y + dy, z + dz)
                if not (0 <= np_[0] < self.W and 0 <= np_[1] < L.HEIGHT and 0 <= np_[2] < self.D):
                    continue
                c = self.cells.get(np_)
                if c is not None and not is_transparent(c):
                    continue
                ne = e - 1 - (1 if (c is not None and _path(c.block) == "water") else 0)
                if ne > lv.get(np_, 0):
                    lv[np_] = ne
                    q.append(np_)
        return lv

    # ---- leaks
    def leaks(self, starts):
        """Cells of the template boundary that can be SEEN from the inside through see-through cells (air, glass, bars ...), other than the doorway holes.
        Why: a window slit or a missing wall block would show the void (or let a niche look out of the room); the hole cells of a socket are expected."""
        hole = set()
        for d in self.doors:
            hole.update(d["cells"])
            hole.add(d["jig"])
        seen = set(starts)
        q = deque(starts)
        bad = []
        while q:
            x, y, z = q.popleft()
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + dx, y + dy, z + dz)
                if n in seen:
                    continue
                if not (0 <= n[0] < self.W and 0 <= n[1] < L.HEIGHT and 0 <= n[2] < self.D):
                    bad.append(((x, y, z), (dx, dy, dz)))
                    continue
                if n in hole and (n[0] in (0, self.W - 1) or n[2] in (0, self.D - 1)):
                    seen.add(n)
                    continue
                c = self.cells.get(n)
                if c is None or is_transparent(c):
                    seen.add(n)
                    q.append(n)
        # a seen cell on the boundary that is not a hole is a leak
        out = []
        for (p, dv) in bad:
            if p in hole:
                continue
            out.append(p)
        return out, seen

    # ---- fluids
    @staticmethod
    def fluid_of(cell):
        """'water' / 'lava' for a fluid block or a waterlogged block (a waterlogged stair is a water source that can spread), else None."""
        n = _path(cell.block)
        if n in FLUIDS:
            return n
        if cell.props.get("waterlogged") == "true":
            return "water"
        return None

    def fluid_problems(self):
        """Fluid cells whose side or bottom neighbour is neither the same fluid nor a full block (a source block flows into any open neighbour), and fluid
        cells touching a doorway hole. Waterlogged blocks count as water."""
        probs = []
        hole = set()
        for d in self.doors:
            hole.update(d["cells"])
            hole.add(d["jig"])
        for pos, c in sorted(self.cells.items()):
            fl = self.fluid_of(c)
            if fl is None:
                continue
            x, y, z = pos
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                q = (x + dx, y + dy, z + dz)
                if not (0 <= q[0] < self.W and 0 <= q[1] < L.HEIGHT and 0 <= q[2] < self.D):
                    probs.append("%s %s at %s touches the template boundary" % (fl, c.block, pos))
                    continue
                o = self.cells.get(q)
                if o is None:
                    probs.append("%s at %s has an open side at %s" % (fl, pos, q))
                elif self.fluid_of(o) == fl:
                    continue
                elif classify(o) != "solid" or o.block.startswith("nusmp:"):
                    probs.append("%s at %s: neighbour %s at %s is not a full block" % (fl, pos, o.block, q))
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
                if (x + dx, y + dy, z + dz) in hole or pos in hole:
                    probs.append("%s at %s touches a doorway hole" % (fl, pos))
                    break
        return probs

    def flammable_near_lava(self, radius=3):
        """{cell: block} of every wood, wool, carpet, fence, leaf, vine, barrel or bookshelf block within `radius` cells of lava: lava would set them alight,
        so a template must have none."""
        lava = [p for p, c in self.cells.items() if _path(c.block) in ("lava", "lava_cauldron")]
        out = {}
        for (x, y, z) in lava:
            for dx in range(-radius, radius + 1):
                for dy in range(-radius, radius + 1):
                    for dz in range(-radius, radius + 1):
                        c = self.cells.get((x + dx, y + dy, z + dz))
                        if c is None:
                            continue
                        n = _path(c.block)
                        if c.block.startswith("minecraft:dark_oak") or c.block.startswith("minecraft:oak") or n.endswith(("_planks", "_log", "_wool", "_carpet", "hay_block", "bookshelf")) \
                                or (n.endswith(("_fence", "_fence_gate", "vine", "_leaves")) and "nether" not in n) or n in ("vine", "scaffolding", "barrel", "lectern", "moss_block", "moss_carpet", "azalea", "flowering_azalea"):
                            if n.endswith("_trapdoor") or "iron" in n:
                                continue
                            out[(x + dx, y + dy, z + dz)] = c.block
        return out

    def attachment_problems(self):
        """[(severity, position, block, text)] for every block that vanilla would drop at the first neighbour update (see `attachment_problem`), sorted."""
        out = []
        for pos, c in sorted(self.cells.items(), key=lambda kv: (kv[0][1], kv[0][2], kv[0][0])):
            r = attachment_problem(self.cells, pos, c)
            if r is not None:
                out.append((r[0], pos, c.block, r[1]))
        return out

#!/usr/bin/env python3
"""
The shared motifs of the circulation templates (1.54.0): the vocabulary the corridors, junctions, plazas, galleries, the crossroads hall, the door
caps and the Gateway are all drawn with. Imported by `gen_mega_arch.py` and `gen_mega_gateway.py`; builds on `mega_lib.MT` (the grid, the palette,
the block-state checker), it does not replace it.

Why a kit: the owner wants every room to read as hand-crafted, and hand-crafted means a small number of good details used with rhythm and
variety, not forty one-off pieces of code. A pilaster, a coved rib, a niche with an arch over it, a hanging lantern on its chain, a skirting of
stairs along a wall: each is written once, with the block states checked by `MT.set`, and every piece reuses them in a different dressing
(`Style`). That keeps forty templates visually one family (the same trim, the same door frames, the same palette the Region Theme can swap) while
no two corridors are the same.

Conventions: coordinates are template-local (x east, y up, z south); `side` is "w" or "e" for a wall running along z, and facings are compass
names. Row 8 is the floor, row 9 the first standing row; `ROW0` is 9. Everything stays inside the palette of `mega_lib` for architecture (the
Region Theme swaps those families) and uses plain vanilla blocks for the details.
"""
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import nbt  # noqa: E402

FLOOR = L.FLOOR_ROW          # 8
ROW0 = L.DOOR_ROW            # 9: first row above the floor (the standing row, the door's bottom row)
N, E, S, W = "north", "east", "south", "west"
OPP = L.OPPOSITE
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}
SKULL_ROT = {"south": 0, "west": 4, "north": 8, "east": 12}


# =====================================================================================================================
# block helpers: every call goes through MT.set, so a bad block state raises here instead of showing up in game
# =====================================================================================================================
def stair(mt, x, y, z, block, facing, half="bottom"):
    mt.set(x, y, z, block, {"facing": facing, "half": half})


def slab(mt, x, y, z, block, kind="bottom"):
    mt.set(x, y, z, block, {"type": kind})


def lantern(mt, x, y, z, hanging=False, soul=False):
    mt.set(x, y, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true" if hanging else "false"})


def chain(mt, x, y, z):
    mt.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})


def hang_lantern(mt, x, y_attach, z, drop=2, soul=False):
    """A lantern hung on `drop` chain blocks below the block at row `y_attach` (the chain starts in the row under it)."""
    for k in range(drop):
        chain(mt, x, y_attach - 1 - k, z)
    lantern(mt, x, y_attach - 1 - drop, z, hanging=True, soul=soul)


def chandelier(mt, x, y_ceiling, z, drop=2, radius=1, soul=False, arm_lift=1):
    """A chandelier hung from the solid cells at row `y_ceiling` (a rib band or the ceiling): a centre chain of `drop` links with a lantern at its foot and,
    `radius` cells away in the four compass directions, a chain `arm_lift` links shorter with a lantern of its own, so the arms hang higher than the
    middle. Every chain runs up to the ceiling. (`mega_lib.chandelier` hangs its four side lanterns from chain stubs that end in the air one cell from
    the centre chain: they look fine in a flat preview and float in the game, which `mega_arch_check` reports.)"""
    hang_lantern(mt, x, y_ceiling, z, drop=drop, soul=soul)
    arm = max(1, drop - arm_lift)
    for dx, dz in ((radius, 0), (-radius, 0), (0, radius), (0, -radius)):
        hang_lantern(mt, x + dx, y_ceiling, z + dz, drop=arm, soul=soul)


def lectern(mt, x, y, z, facing):
    """A lectern that really holds a book. The block state `has_book` only DRAWS a book; with an empty block entity the lectern opens an empty screen and its
    "take book" button hands out nothing, so the block entity carries a writable book (the one book item that needs no components: players can take it
    and write in it). `mt` is anything with `set(x, y, z, block, props, nbt_)`: a template builder, a cap builder or a `Quad`."""
    book = nbt.Compound({"id": "minecraft:writable_book", "count": nbt.Int(1)})
    mt.set(x, y, z, "minecraft:lectern", {"facing": facing, "has_book": "true", "powered": "false"},
           nbt.Compound({"id": "minecraft:lectern", "Book": book, "Page": nbt.Int(0)}))


def candle(mt, x, y, z, n=3, color=None, lit=True):
    block = "minecraft:candle" if color is None else "minecraft:%s_candle" % color
    mt.set(x, y, z, block, {"candles": str(n), "lit": "true" if lit else "false"})


def wall_banner(mt, x, y, z, facing, color="red"):
    mt.set(x, y, z, "minecraft:%s_wall_banner" % color, {"facing": facing})


def cobweb(mt, x, y, z):
    mt.set(x, y, z, "minecraft:cobweb")


def skull(mt, x, y, z, facing="south", kind="skeleton"):
    mt.set(x, y, z, "minecraft:%s_skull" % kind, {"rotation": str(SKULL_ROT[facing])})


def wall_skull_torch(mt, x, y, z, facing, kind="wither_skeleton", soul=True):
    """A skull on the wall with a torch above it (a sconce): the light is a real torch, which the gloom clock counts."""
    mt.set(x, y, z, "minecraft:%s_wall_skull" % kind, {"facing": facing})
    mt.set(x, y + 1, z, "minecraft:soul_wall_torch" if soul else "minecraft:wall_torch", {"facing": facing})


def wall_torch(mt, x, y, z, facing, soul=False):
    mt.set(x, y, z, "minecraft:soul_wall_torch" if soul else "minecraft:wall_torch", {"facing": facing})


def bars(mt, x, y, z):
    mt.set(x, y, z, L.IRON_BARS)


def pillar(mt, x, z, y0, y1, block=None, axis="y"):
    for y in range(y0, y1 + 1):
        mt.set(x, y, z, block or L.POLISHED, {"axis": axis} if (block or L.POLISHED) in (L.LOG, L.STRIPPED_LOG) else None)


def put_if_free(mt, x, y, z, block, props=None):
    """Sets the cell only when nothing is there yet (so a decoration never overwrites architecture)."""
    if mt.get(x, y, z) is None:
        mt.set(x, y, z, block, props)
        return True
    return False


def seeded(mt, tag=""):
    return random.Random("nusmp:mega:arch:" + mt.name + ":" + tag)


def clear(mt, x, y, z):
    """Removes a cell (the world keeps air there): carving a niche out of a wall layer."""
    mt.remove(x, y, z)


def marker_standing(mt, x, z, kind, y=ROW0, **args):
    """A data marker in the standing cell (floor row + 1 by default). Refuses a cell that already holds a block: a marker would replace it, and as the game
    never places the marker the cell would end up as a hole in whatever stood there (a carpet, a skirting stair, a candle)."""
    cur = mt.get(x, y, z)
    if cur is not None:
        raise ValueError("%s: marker %s at (%d,%d,%d) would replace %s" % (mt.name, kind, x, y, z, cur.block))
    mt.marker(x, y, z, kind, **args)


def outward(side):
    return W if side == "w" else E


def inward(side):
    return E if side == "w" else W


def side_x(side, k):
    """x of the column k cells in from the wall face: k=0 the inner wall layer, k=1 the interior edge column (west: 1, 2 / east: 7, 6); k=-1 the outer layer."""
    return (1 + k) if side == "w" else (7 - k)


# =====================================================================================================================
# the dressing of a variant
# =====================================================================================================================
class Style:
    """The dressing of one variant of a piece. Plain data: the builders read it, nothing else does."""

    def __init__(self, name, wear=0.10, rib="stone", floor="runner", lights="lantern", niches=None, cobwebs=0.0, rubble=0.0, carpet=None,
                 light_every=1, skirt="stone", ends="banner", clutter=None, carpet_w=1, broken_ribs=0.0):
        self.name = name
        self.wear = wear
        self.rib = rib              # "stone" (polished ribs) or "timber" (dark oak beams)
        self.floor = floor          # "runner", "checker" or "plain"
        self.lights = lights        # "lantern", "soul" or "mixed"
        self.niches = niches or [(3, "lantern"), (2, "banner"), (2, "skull"), (1, "slit"), (2, "plain")]
        self.cobwebs = cobwebs      # chance per upper corner of a cobweb
        self.rubble = rubble        # chance per bay of a rubble heap against a wall
        self.carpet = carpet        # the colour of a runner (wool blocks in the floor row), or None
        self.light_every = light_every
        self.skirt = skirt          # "stone", "wood" or None: the plinth along the walls
        self.ends = ends            # what flanks the end doorways: "banner", "sconce", "lantern" or "none"
        self.clutter = clutter or []  # [(weight, kind)] props along the wall foot: barrels, crates, bones, benches
        self.carpet_w = carpet_w    # 1 or 3: the width of the runner
        self.broken_ribs = broken_ribs  # chance per rib that its middle has fallen (the corbels and pilasters stay)


# =====================================================================================================================
# the 5-wide corridor: bays, flush pilasters, coved ribs, skirting, niches
# =====================================================================================================================
def bay_layout(z0, z1, doors, gap=4):
    """Z positions of the pilasters of a corridor whose interior spans z0..z1 (inclusive). Pilasters stand at both ends and flank every side door (door
    centre +-2, so a pilaster continues each door frame), and the stretches between them are split so that no gap is wider than `gap` free cells."""
    req = {z0, z1}
    for c in doors:
        req.add(c - 2)
        req.add(c + 2)
    req = sorted(r for r in req if z0 <= r <= z1)
    out = list(req)
    for a, b in zip(req, req[1:]):
        free = b - a - 1
        if free > gap:
            n = -(-free // gap)                   # bays needed
            for k in range(1, n):
                out.append(a + int(round(k * (b - a) / float(n))))
    return sorted(set(out))


def door_holes(doors, z, margin=2):
    """Is z inside the 3-wide hole (plus `margin` cells each side) of a door centred at one of `doors`?"""
    return any(abs(z - c) <= margin for c in doors)


def runner_block(color):
    """The block a runner is laid with. Why wool blocks in the floor row and not carpet on top of it: a data marker (a patrol node, a spawn cell)
    occupies the standing cell above the floor, and a marker standing in a carpet cell would leave a hole in the carpet (the game never places a
    marker block). A wool block in the floor row looks the same from above and leaves the standing cell free for markers."""
    return "minecraft:%s_wool" % color


def floor_pattern(mt, style, z0, z1, rng, x0=2, x1=6):
    """The floor of the interior: a runner of polished stone down the middle (or a chequer), tile at the edges, a few cracks, an optional coloured runner."""
    mid = (x0 + x1) // 2
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            inner = x0 + 1 <= x <= x1 - 1
            if style.floor == "runner":
                if inner:
                    mt.set(x, FLOOR, z, L.POLISHED if (x == mid or (z % 4) != 1) else L.TILE)
                elif rng.random() < 0.14:
                    mt.set(x, FLOOR, z, L.CRACKED_TILE)
            elif style.floor == "checker":
                if inner:
                    mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
                elif rng.random() < 0.14:
                    mt.set(x, FLOOR, z, L.CRACKED_TILE)
            if style.carpet and abs(x - mid) <= style.carpet_w // 2:
                mt.set(x, FLOOR, z, runner_block(style.carpet))


def pilaster(mt, side, z, style, y1=ROW0 + 5):
    """A flush pier in the inner wall layer: polished stone (a stripped dark oak log in a timber corridor) with a chiselled plinth and band. Flush, so
    the corridor stays 5 wide everywhere, but it reads as a column because its material differs from the wall bricks around it."""
    x = side_x(side, 0)
    for y in range(ROW0, y1 + 1):
        if style.rib == "timber":
            mt.set(x, y, z, L.STRIPPED_LOG, {"axis": "y"})
        else:
            mt.set(x, y, z, L.POLISHED)
    mt.set(x, ROW0, z, L.CHISELED)
    mt.set(x, ROW0 + 3, z, L.CHISELED)


def rib(mt, z, style, x0=2, x1=6, top=ROW0 + 5, broken=False):
    """A coved rib across the ceiling at z: a band one block under the ceiling with upside-down stairs flaring into the walls. `broken`: the middle of
    the band has fallen (only the two end cells and the corbels remain), the way a ruin loses its ribs."""
    timber = style.rib == "timber"
    band = L.LOG if timber else L.POLISHED
    for x in range(x0, x1 + 1):
        if broken and x0 + 1 < x < x1 - 1:
            continue
        mt.set(x, top, z, band, {"axis": "x"} if timber else None)
    sb = L.PLANKS_STAIRS if timber else L.POLISHED_STAIRS
    stair(mt, x0, top - 1, z, sb, W, "top")
    stair(mt, x1, top - 1, z, sb, E, "top")


def cove(mt, z0, z1, ribs, x0=2, x1=6, top=ROW0 + 5, skip=()):
    """Upside-down stairs along both walls under the ceiling between the ribs: the ceiling meets the wall in a curve instead of a corner."""
    for z in range(z0, z1 + 1):
        if z in ribs or z in skip:
            continue
        stair(mt, x0, top, z, L.TILE_STAIRS, W, "top")
        stair(mt, x1, top, z, L.TILE_STAIRS, E, "top")


def skirting(mt, side, z0, z1, skip, kind="stone"):
    """A one-step plinth along a wall: bottom stairs whose high side is against the wall (walkable: a stair is a half block)."""
    x = side_x(side, 1)
    block = L.PLANKS_STAIRS if kind == "wood" else L.POLISHED_STAIRS
    for z in range(z0, z1 + 1):
        if z in skip:
            continue
        stair(mt, x, ROW0, z, block, outward(side), "bottom")


class Wall:
    """One of the four walls of a piece seen from inside: `cell(lat, depth, y)` is the template cell `lat` along the wall and `depth` layers in from
    the INNER wall layer (depth 0 = the inner layer, -1 = the outer layer, +1 = the first interior cell), so a feature is written once and works on
    all four walls of any size. `into` is the facing that points into the room, `a_dir` / `b_dir` the compass names toward lower / higher `lat`."""

    def __init__(self, mt, face):
        if face not in ("n", "e", "s", "w"):
            raise ValueError("wall face must be n/e/s/w")
        self.mt, self.face = mt, face
        self.into = {"n": S, "s": N, "w": E, "e": W}[face]
        self.a_dir, self.b_dir = ((W, E) if face in ("n", "s") else (N, S))

    def cell(self, lat, depth, y):
        f, mt = self.face, self.mt
        if f == "n":
            return (lat, y, 1 + depth)
        if f == "s":
            return (lat, y, mt.D - 2 - depth)
        if f == "w":
            return (1 + depth, y, lat)
        return (mt.W - 2 - depth, y, lat)

    def set(self, lat, depth, y, block, props=None):
        x, yy, z = self.cell(lat, depth, y)
        self.mt.set(x, yy, z, block, props)

    def get(self, lat, depth, y):
        return self.mt.get(*self.cell(lat, depth, y))

    def clear(self, lat, depth, y):
        x, yy, z = self.cell(lat, depth, y)
        self.mt.remove(x, yy, z)

    def stair(self, lat, depth, y, block, facing, half="bottom"):
        self.set(lat, depth, y, block, {"facing": facing, "half": half})


def carve_niche(w, cells, rows, arch=True):
    """Removes the inner wall layer over `cells` (laterals) for the given rows, leaving the outer layer as the back, and closes the top corners with
    upside-down stairs when the niche is at least three wide: a rounded-off recess instead of a square hole."""
    cells = list(cells)
    top = rows[-1]
    for c in cells:
        for y in rows:
            w.clear(c, 0, y)
        if arch and len(cells) >= 3 and c in (cells[0], cells[-1]):
            w.stair(c, 0, top, L.BRICK_STAIRS, w.a_dir if c == cells[0] else w.b_dir, "top")


def niche(w, cells, kind, rng, style):
    """One wall feature across the lateral `cells` of a bay. The inner wall layer is carved 3 rows tall (rows 10..12) with a stair arch on top, the outer
    layer stays as the back, so nothing opens to the void; the contents stand on the shelf formed by the row-9 block under the carve."""
    cells = list(cells)
    if kind == "plain" or not cells:
        return
    zc = cells[len(cells) // 2]
    face = w.into
    soul = style.lights == "soul" or (style.lights == "mixed" and rng.random() < 0.5)
    y1 = ROW0 + 1
    if kind in ("lantern", "banner", "skull", "candles", "shelf"):
        carve_niche(w, cells, (ROW0 + 1, ROW0 + 2, ROW0 + 3))
    if kind == "lantern":
        w.set(zc, 0, y1, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "false"})
        if len(cells) % 2 == 0:
            w.set(zc - 1, 0, y1, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "false"})
    elif kind == "banner":
        w.set(zc, 0, ROW0 + 3, "minecraft:%s_wall_banner" % rng.choice(("red", "black", "purple", "gray")), {"facing": face})
        if len(cells) % 2 == 0:
            w.set(zc - 1, 0, ROW0 + 3, "minecraft:%s_wall_banner" % rng.choice(("red", "black", "purple", "gray")), {"facing": face})
    elif kind == "skull":
        w.set(zc, 0, y1, L.POLISHED_SLAB, {"type": "top"})          # a TOP slab: the skull above it then rests on it instead of floating half a block up
        w.set(zc, 0, y1 + 1, "minecraft:%s_skull" % rng.choice(("skeleton", "wither_skeleton")), {"rotation": str(SKULL_ROT[face])})
        w.set(cells[0], 0, y1, "minecraft:gray_candle", {"candles": "2", "lit": "true"})
    elif kind == "candles":
        w.set(zc, 0, y1, L.POLISHED_SLAB, {"type": "top"})
        col = rng.choice((None, "red", "gray", "purple"))
        w.set(zc, 0, y1 + 1, "minecraft:candle" if col is None else "minecraft:%s_candle" % col, {"candles": str(rng.choice((2, 3, 4))), "lit": "true"})
    elif kind == "shelf":
        for y in (y1, y1 + 1):
            w.set(zc, 0, y, "minecraft:bookshelf")
        if len(cells) >= 3:
            w.set(cells[0], 0, y1, "minecraft:candle", {"candles": "1", "lit": "true"})
    elif kind == "slit":
        # a barred window slit: bars in the inner layer and a warm backlight in the outer layer behind them
        for y in (y1, y1 + 1):
            w.set(zc, 0, y, L.IRON_BARS)
            w.set(zc, -1, y, "minecraft:ochre_froglight", {"axis": "y"})
    elif kind == "sconce":
        x, y, z = w.cell(zc, 1, ROW0 + 2)
        wall_skull_torch(w.mt, x, y, z, face, kind=rng.choice(("skeleton", "wither_skeleton")), soul=True)
    else:
        raise ValueError("unknown niche kind %r" % kind)


def lights_for(mt, style, zs, y_ceiling=ROW0 + 6, x=4, drop=2):
    """Hanging lanterns on the line x at the given z's (under the ceiling row)."""
    for i, z in enumerate(zs):
        soul = style.lights == "soul" or (style.lights == "mixed" and i % 2 == 1)
        hang_lantern(mt, x, y_ceiling, z, drop=drop, soul=soul)


def end_dressing(mt, style, z_inner, face, rng, xs=(2, 6)):
    """What flanks a doorway at the end of a corridor: banners on the frame columns, skull sconces or small torches. `z_inner` is the z of the
    interior cell next to the end wall, `face` the direction pointing into the corridor."""
    for x in xs:
        if style.ends == "banner":
            wall_banner(mt, x, ROW0 + 3, z_inner, face, rng.choice(("red", "black", "purple")))
        elif style.ends == "sconce":
            wall_skull_torch(mt, x, ROW0 + 2, z_inner, face, kind="wither_skeleton", soul=True)
        elif style.ends == "lantern":
            put_if_free(mt, x, ROW0 + 2, z_inner, "minecraft:wall_torch", {"facing": face})


def rubble_heap(mt, x, z, side_dir, rng, size=2):
    """A heap of fallen masonry against a wall at the floor: cobbled slabs, stairs and a wall post. `side_dir` is the direction pointing away from
    the wall (the way the stairs descend)."""
    mt.set(x, ROW0, z, L.COBBLED_SLAB, {"type": "bottom"})
    if size >= 2:
        put_if_free(mt, x, ROW0, z + 1, L.COBBLED_STAIRS, {"facing": OPP[side_dir], "half": "bottom"})
        put_if_free(mt, x, ROW0 + 1, z, L.COBBLED_WALL, {"up": "true"})
    if size >= 3:
        put_if_free(mt, x, ROW0, z - 1, L.COBBLED_STAIRS, {"facing": OPP[side_dir], "half": "bottom"})
        put_if_free(mt, x, ROW0 + 1, z + 1, L.COBBLED_SLAB, {"type": "bottom"})


def bone_pile(mt, x, z, rng):
    mt.set(x, ROW0, z, "minecraft:bone_block", {"axis": rng.choice("xz")})
    skull(mt, x, ROW0 + 1, z, rng.choice((N, E, S, W)), rng.choice(("skeleton", "wither_skeleton")))


def barrel(mt, x, y, z, facing="up"):
    mt.set(x, y, z, "minecraft:barrel", {"facing": facing, "open": "false"})


def string_course(mt, side, z0, z1, skip=(), row=ROW0 + 4):
    """A band along the inner wall layer at the springing row (13): chiselled and polished blocks alternating, so the door lintels (which use the same two
    blocks) and the wall read as one design. `skip` are laterals left alone (pilasters and niche arches already own their cell)."""
    x = side_x(side, 0)
    for z in range(z0, z1 + 1):
        if z in skip or mt.get(x, row, z) is None:
            continue
        cur = mt.get(x, row, z)
        if cur.block not in (L.BRICK, L.CRACKED_BRICK):
            continue
        mt.set(x, row, z, L.CHISELED if z % 2 == 0 else L.POLISHED)


def medallion(mt, cx, cz, r=1, centre=None, ring=None, y=FLOOR):
    """A small inlay in the floor: a plus of `ring` blocks around a `centre` block (defaults: chiselled in a polished floor)."""
    centre = centre or L.CHISELED
    ring = ring or L.TILE
    mt.set(cx, y, cz, centre)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        mt.set(cx + dx * r, y, cz + dz * r, ring)


# =====================================================================================================================
# arches and arcades (the gallery and the crossroads hall)
# =====================================================================================================================
def pointed_arch(put, n, y_spring, y_top, ring="polished", fill="brick", left_dir=W, right_dir=E):
    """A 45-degree pointed arch over an opening n cells wide, in the vertical plane described by `put(u, y, block, props)` (u = 0..n-1 along the span,
    left to right). Row r above `y_spring`: upside-down stairs at u = r and u = n-1-r face outward, the cells beyond them are solid, the cells between
    them are open, so the opening narrows by one cell per side per row; at the crown the stairs meet in a keystone and everything above, up to
    `y_top`, is wall. `ring` is the family of the visible ring (polished stone), `fill` that of the wall behind it. Returns the highest open row."""
    stair_block = L.POLISHED_STAIRS if ring == "polished" else L.BRICK_STAIRS
    ring_block = L.POLISHED if ring == "polished" else L.BRICK
    fill_block = L.BRICK if fill == "brick" else L.TILE
    last_open = y_spring - 1
    for r in range(0, n):
        y = y_spring + r
        if y > y_top:
            break
        lo, hi = r, n - 1 - r
        if lo > hi:
            for u in range(n):
                put(u, y, fill_block, None)
            continue
        for u in range(n):
            if u < lo or u > hi:
                put(u, y, ring_block if (u == lo - 1 or u == hi + 1) else fill_block, None)
        if lo == hi:
            put(lo, y, ring_block, None)            # the keystone
        else:
            put(lo, y, stair_block, {"facing": left_dir, "half": "top"})
            put(hi, y, stair_block, {"facing": right_dir, "half": "top"})
            last_open = y
    for y in range(y_spring + n, y_top + 1):
        for u in range(n):
            put(u, y, fill_block, None)
    return last_open


# =====================================================================================================================
# plaza furniture: water, benches, lamp posts, braziers, statues
# =====================================================================================================================
def water(mt, x, y, z):
    """A water SOURCE block. Only ever place it with full blocks on its four sides and below (a source next to air spreads when the game ticks it)."""
    mt.set(x, y, z, "minecraft:water", {"level": "0"})


def lamp_post(mt, x, z, y0=ROW0, height=2, soul=True, base=None):
    """A lamp post: `height` polished wall posts topped by a standing lantern (the lantern needs a block under it, a wall post is one)."""
    if base:
        mt.set(x, y0, z, base)
        y0 += 1
    for y in range(y0, y0 + height):
        mt.set(x, y, z, L.POLISHED_WALL, {"up": "true"})
    mt.set(x, y0 + height, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "false"})


def bench_row(mt, wall, cells, depth=1, wood=True, y=ROW0):
    """A bench along a wall: stairs with their backs to the wall (the high side of a stair is the way it faces)."""
    block = L.PLANKS_STAIRS if wood else L.POLISHED_STAIRS
    back = {N: S, S: N, E: W, W: E}[wall.into]
    for c in cells:
        wall.set(c, depth, y, block, {"facing": back, "half": "bottom"})


def statue(mt, x, y, z, facing, kit="iron", plinth=True, weapon=True):
    """An armour-stand statue on a plinth (a polished block with a slab): `kit` is iron, golden, diamond, netherite or chainmail. The stand is the one
    kind of entity a template may hold; DisabledSlots (set by mega_lib.armor_stand) keeps players from stripping it and it is invulnerable."""
    g = {"iron": "iron", "golden": "golden", "diamond": "diamond", "netherite": "netherite", "chainmail": "chainmail"}[kit]
    sword = "minecraft:%s_sword" % ("golden" if g == "golden" else g if g != "chainmail" else "iron")
    if plinth:
        mt.set(x, y, z, L.POLISHED)
        mt.set(x, y + 1, z, L.POLISHED_SLAB, {"type": "bottom"})
        yy = y + 1
    else:
        yy = y
    mt.armor_stand(x, yy, z, yaw=YAW[facing], head="minecraft:%s_helmet" % g, chest="minecraft:%s_chestplate" % g, legs="minecraft:%s_leggings" % g,
                   feet="minecraft:%s_boots" % g, mainhand=sword if weapon else None, dy=0.5 if plinth else 0.0)


def tall_niche(w, cells, kind, rng, rows=None, colors=("red", "black", "purple")):
    """A tall recess in a wall (rows 10..13 by default, any width >= 3) with a feature in it: a statue between banners, a shrine, a brazier behind bars,
    a bookcase and lectern, a glowing stained-glass window, a ruined heap or a shelf of skulls. The outer wall layer is the back of every niche, so
    nothing opens to the void; windows are lit by a froglight set into that back layer."""
    mt = w.mt
    cells = list(cells)
    rows = tuple(rows or range(ROW0 + 1, ROW0 + 5))
    carve_niche(w, cells, rows)
    face = w.into
    c0 = cells[len(cells) // 2 - (0 if len(cells) % 2 else 1)]       # the centre (the left of the two centre cells when the width is even)
    c1 = cells[len(cells) // 2]
    mids = sorted({c0, c1})
    y1 = ROW0 + 1
    top = rows[-1]
    if kind == "statue":
        for c in mids:
            statue(mt, *w.cell(c, 0, y1), facing=face, kit=rng.choice(("iron", "iron", "golden", "diamond")))
        for c in (cells[0], cells[-1]):
            w.set(c, 0, top, "minecraft:%s_wall_banner" % rng.choice(colors), {"facing": face})
    elif kind == "shrine":
        for c in cells:
            w.set(c, 0, y1, L.POLISHED)
        for c in mids:
            w.set(c, 0, y1 + 1, L.CHISELED)
            w.set(c, 0, y1 + 2, "minecraft:candle", {"candles": "4", "lit": "true"})
        for c in (cells[0], cells[-1]):
            w.set(c, 0, y1 + 1, L.POLISHED_SLAB, {"type": "top"})        # top slab: the standing lantern rests on it with no gap
            w.set(c, 0, y1 + 2, "minecraft:soul_lantern", {"hanging": "false"})
        for c in mids:
            w.set(c, 0, top, "minecraft:purple_wall_banner", {"facing": face})
    elif kind == "brazier":
        for c in mids:
            w.set(c, 0, y1, L.POLISHED)
            w.set(c, 0, y1 + 1, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
            for y in (y1 + 2, y1 + 3):
                w.set(c, 0, y, L.IRON_BARS)
        for c in cells:
            if c not in mids:
                for y in (y1, y1 + 1):
                    w.set(c, 0, y, L.IRON_BARS)
    elif kind == "library":
        for c in cells:
            for y in (y1, y1 + 1):
                w.set(c, 0, y, "minecraft:bookshelf")
        x, y, z = w.cell(c1, 1, ROW0)
        lectern(mt, x, y, z, face)
        for c in (cells[0], cells[-1]):
            w.set(c, 0, y1 + 2, "minecraft:candle", {"candles": "2", "lit": "true"})
    elif kind == "window":
        col = rng.choice(("purple", "cyan", "red", "blue"))
        for c in cells:
            for y in range(y1 + 1, top):
                w.set(c, 0, y, "minecraft:%s_stained_glass" % col)
                w.set(c, -1, y, "minecraft:ochre_froglight", {"axis": "y"})
        for c in cells:
            w.set(c, 0, y1, L.POLISHED)       # a full block under the glass: a bottom slab would leave a half-block slot between it and the pane
    elif kind == "ruin":
        for c in cells:
            for y in (y1, y1 + 1):
                w.set(c, 0, y, rng.choice((L.COBBLED, L.CRACKED_BRICK, L.COBBLED)))
        for c in mids:
            w.set(c, 0, y1 + 2, L.COBBLED_STAIRS, {"facing": face, "half": "bottom"})
        x, y, z = w.cell(c1, 1, ROW0)
        rubble_heap(mt, x, z, face, rng, size=3)
        x, y, z = w.cell(cells[0], 1, top)
        mt.set(x, y, z, "minecraft:cobweb")
    elif kind == "skulls":
        for c in cells:
            w.set(c, 0, y1, L.POLISHED_SLAB, {"type": "top"})
            w.set(c, 0, y1 + 1, "minecraft:%s_skull" % rng.choice(("skeleton", "wither_skeleton")), {"rotation": str(SKULL_ROT[face])})
        w.set(c1, 0, y1 + 2, "minecraft:soul_wall_torch", {"facing": face})       # on the back wall of the recess, above the shelf: it lights the niche and the wall
    elif kind == "banners":
        for c in cells:
            w.set(c, 0, top, "minecraft:%s_wall_banner" % colors[(cells.index(c)) % len(colors)], {"facing": face})
        for c in mids:
            w.set(c, 0, y1, L.POLISHED_SLAB, {"type": "top"})
            w.set(c, 0, y1 + 1, "minecraft:candle", {"candles": "3", "lit": "true"})
    elif kind == "casks":
        for c in cells:
            w.set(c, 0, y1, "minecraft:barrel", {"facing": face if (cells.index(c) % 2) else "up", "open": "false"})
            if cells.index(c) % 2 == 0:
                w.set(c, 0, y1 + 1, "minecraft:barrel", {"facing": "up", "open": "false"})
        if w.get(c1, 0, y1 + 1) is None:
            w.set(c1, 0, y1 + 1, "minecraft:lantern", {"hanging": "false"})
    else:
        raise ValueError("unknown tall niche kind %r" % kind)


class Quad:
    """Writes a quarter of a symmetrical piece once and mirrors it into the four quadrants: coordinates are given for the north-west quarter and mapped
    for the others, and every `facing` property (and the direction arguments of the helpers) is mirrored with them. `size` is the template edge
    (27 for the crossroads hall): x -> size-1-x for the east quarters, z -> size-1-z for the south ones."""

    def __init__(self, mt, mx, mz, size):
        self.mt, self.mx, self.mz, self.size = mt, mx, mz, size

    def xz(self, x, z):
        return (self.size - 1 - x if self.mx else x, self.size - 1 - z if self.mz else z)

    def f(self, d):
        if self.mx and d in (E, W):
            d = OPP[d]
        if self.mz and d in (N, S):
            d = OPP[d]
        return d

    def set(self, x, y, z, block, props=None, nbt_=None):
        props = dict(props or {})
        if "facing" in props and props["facing"] in (N, E, S, W):
            props["facing"] = self.f(props["facing"])
        xx, zz = self.xz(x, z)
        self.mt.set(xx, y, zz, block, props or None, nbt_)

    def get(self, x, y, z):
        xx, zz = self.xz(x, z)
        return self.mt.get(xx, y, zz)

    def cell(self, x, y, z):
        xx, zz = self.xz(x, z)
        return (xx, y, zz)


def arch_span(mt, axis, fixed, a, b, y_spring, top, left_dir=None, right_dir=None):
    """A pointed arch spanning cells a..b (inclusive) of a one-thick wall. axis "z": the wall lies at x = `fixed` and runs along z; axis "x": it lies at
    z = `fixed` and runs along x. `left_dir` / `right_dir` are the compass facings toward `a` and toward `b` (default north/south for "z", west/east
    for "x"); pass them mirrored when the wall is a mirror image."""
    if axis == "z":
        left_dir, right_dir = left_dir or N, right_dir or S

        def put(u, y, block, props):
            mt.set(fixed, y, a + u, block, props)
    else:
        left_dir, right_dir = left_dir or W, right_dir or E

        def put(u, y, block, props):
            mt.set(a + u, y, fixed, block, props)
    pointed_arch(put, b - a + 1, y_spring, top, "polished", "brick", left_dir, right_dir)


def small_brazier(mt, x, z, soul=False, y0=ROW0):
    """A 1x1 fire basket: a polished foot, a campfire, bars on the four sides and over the top. 3x3 footprint including the bars; `y0` is the row of the foot."""
    mt.set(x, y0, z, L.POLISHED)
    mt.set(x, y0 + 1, z, "minecraft:soul_campfire" if soul else "minecraft:campfire",
           {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        mt.set(x + dx, y0, z + dz, L.POLISHED_SLAB, {"type": "bottom"})
        mt.set(x + dx, y0 + 1, z + dz, L.IRON_BARS)
        mt.set(x + dx, y0 + 2, z + dz, L.IRON_BARS)
    mt.set(x, y0 + 2, z, L.IRON_BARS)

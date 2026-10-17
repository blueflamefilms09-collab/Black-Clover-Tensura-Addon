#!/usr/bin/env python3
"""
The shared kit of the THEME-A rooms (1.59.0): the Holy Church, the Celestial Court, the Sky Citadel and the Mountain Monastery.

Why a kit of its own: the four themes are built on the furnishing kit of ROOMS-1 (`mega_decor_r1.Hall`, which refuses furniture in a doorway or on a declared
route and checks the finished template without a game) and on the shell of `mega_lib.MT`, but they all need the same few things those do not have: windows of
several panes in several colours that glow (a stained-glass lancet, a rose window), a pointed arcade between pillars, a deck on a second level with a stair up to
it and a railing, a ring of pillars, a fountain with water that cannot flow, a plain `run()` that builds, checks, saves and compares rooms the way
`gen_mega_rooms1.py` does. Written once here, checked by the same gates, so the four generators stay short and the four themes stay one family.

Everything architectural uses the palette constants of `mega_lib` (the Region Theme swaps those families, which is what makes one room a Church in one sector's
data and a Court in another's); furniture, glass, light and colour are free vanilla blocks.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR  # noqa: E402
import mega_arch_kit as AK  # noqa: E402
import mega_arch_check as AC  # noqa: E402

# ===================================================================================================================
# the checker of ROOMS-1 knows only the blocks of crypts and libraries; these four themes add gardens, bells, organs and water
# ===================================================================================================================
import re  # noqa: E402

#: blocks a person can stand on (or hang a ladder on) and that stop light and sight, which `mega_lib` and `mega_decor_r1` have no rule for (they were written for stone rooms).
#: Without this a grass floor is "an obstacle" to the walk graph and "see-through" to the leak check, and every garden would fail for the wrong reason.
_SOLID_GROUND = "ward_rune|spawn_glyph|pitfall_tile|grass_block|dirt_path|coarse_dirt|podzol|rooted_dirt|farmland|mycelium|moss_block|snow_block|packed_snow|bamboo_mosaic|stripped_bamboo_block|bamboo_block|(?:waxed_)?(?:\\w+_)?copper_bulb"
L._FULL = re.compile("(?:%s)|%s" % (L._FULL.pattern, _SOLID_GROUND))
_orig_opaque = D.opaque


def _opaque(block):
    return _orig_opaque(block) or bool(re.fullmatch(_SOLID_GROUND, block.split(":", 1)[-1]))


D.opaque = _opaque
_orig_strict = D.strict_state_problems


def _strict_state_problems(t):
    """`mega_decor_r1.strict_state_problems` with the properties vanilla really gives the extra blocks of these themes (the game's own table, read through
    `mega_lib.real_states`): a property that exists in the game is no longer reported as unknown. Values are checked at the moment of `set` already."""
    real = L.real_states()
    out = []
    for msg in _orig_strict(t):
        m = re.match(r"(\S+) has no property (\S+) ", msg)
        if m and (m.group(1).startswith("nusmp:") or m.group(2) in real.get(m.group(1)[len("minecraft:"):], {})):
            continue                                          # the mod's own blocks carry the states the mod gives them; the game's table covers the vanilla ones
        out.append(msg)
    return out


D.strict_state_problems = _strict_state_problems


ROT = {"south": 0, "west": 4, "north": 8, "east": 12}
OPP = L.OPPOSITE

#: stained glass colours that read well lit from behind (a glow block in the outer wall layer)
JEWEL = ("red", "blue", "yellow", "green", "purple", "orange", "cyan", "magenta", "light_blue", "lime", "pink", "white")


def glass(color):
    return "minecraft:%s_stained_glass" % color


def pane(color):
    return "minecraft:%s_stained_glass_pane" % color


# ===================================================================================================================
# windows: glass in the inner wall layer, a glowing block in the outer layer behind it (never the void)
# ===================================================================================================================
def window_cell(w, lat, y, block, glow="minecraft:sea_lantern"):
    """One cell of a window: the inner wall layer becomes `block` (glass) and the outer layer a glow block, so the pane is lit from behind and nothing
    ever shows the void. Returns False (and changes nothing) when the cell is a doorway hole or not plain wall."""
    h = w.hall
    pos_in, pos_out = w.cell(lat, y, -1), w.cell(lat, y, -2)
    if pos_in in h.mt.reserved or pos_out in h.mt.reserved:
        return False
    if h.get(*pos_in) is None or h.get(*pos_out) is None:
        return False
    if not w.free(lat, y, 0):
        return False
    w.carve(lat, y, -1)
    w.set(lat, y, -1, block)
    w.shell_set(lat, y, -2, glow)
    return True


def window_ok(w, lats, y0, y1):
    """True when every wall cell of the window box is plain wall (so a window never eats a doorway frame, a niche or another window)."""
    h = w.hall
    for lat in lats:
        for y in range(y0, y1 + 1):
            for d in (-1, -2):
                pos = w.cell(lat, y, d)
                c = h.get(*pos)
                if c is None or pos in h.mt.reserved or c.block not in D.WALL_BLOCKS:
                    return False
            if not w.free(lat, y, 0):
                return False
    return True


def lancet(w, lat, y0, y1, colors, width=3, glow="minecraft:sea_lantern", pointed=True):
    """A tall pointed window: `width` cells wide (1, 3 or 5) from row y0 to y1, the top two rows narrowed to the middle cell with a bevel of upside-down stairs
    on each side (a pointed arch), the glass in horizontal bands of `colors` and the middle column one colour step off the sides, so a window reads as leaded
    glass and not as a block of one tint. Skipped (returns False) when it would eat anything that is not plain wall."""
    half = width // 2
    lats = range(lat - half, lat + half + 1)
    if not window_ok(w, lats, y0, y1):
        return False
    n = len(colors)
    for y in range(y0, y1 + 1):
        band = (y - y0) // 2
        for dx in range(-half, half + 1):
            if pointed and dx != 0 and y >= y1 - 1:
                if y == y1 - 1:
                    w.carve(lat + dx, y, -1)
                    w.set(lat + dx, y, -1, L.POLISHED_STAIRS, {"facing": w.dec if dx < 0 else w.inc, "half": "top"})
                continue
            col = colors[(band + (0 if dx == 0 else 1)) % n]
            window_cell(w, lat + dx, y, glass(col), glow)
    return True


def rose(w, lat, yc, r, colors, glow="minecraft:sea_lantern", spokes=8, ring_block=L.POLISHED, hub_block=L.CHISELED):
    """A round window of radius r centred on (lat, yc): a polished ring, polished spokes (every 45 degrees) and glass petals between them coloured by angle, a
    chiselled hub in the middle. Returns the number of glass cells."""
    import math
    n = 0
    cells = []
    for dy in range(-r, r + 1):
        for dx in range(-r, r + 1):
            d = math.hypot(dx, dy)
            if d <= r + 0.35:
                cells.append((dx, dy, d))
    if not window_ok(w, [lat + dx for dx, _dy, _d in cells], yc - r, yc + r):
        return 0
    for dx, dy, d in cells:
        lt, y = lat + dx, yc + dy
        ring = d > r - 0.7
        spoke = (dx == 0 or dy == 0 or abs(dx) == abs(dy)) and d > 1.2
        if d < 1.2 and r >= 3:
            w.carve(lt, y, -1)
            w.set(lt, y, -1, hub_block)
            continue
        if ring or spoke:
            w.carve(lt, y, -1)
            w.set(lt, y, -1, ring_block)
            continue
        ang = (math.atan2(dy, dx) + math.pi) / (2 * math.pi)
        col = colors[int(ang * spokes * 1.0) % len(colors)] if d > r * 0.55 else colors[(int(ang * spokes) + 1) % len(colors)]
        if window_cell(w, lt, y, glass(col), glow):
            n += 1
    return n


def band_of_windows(w, lats, y0, y1, palettes, width=3, glow="minecraft:sea_lantern"):
    """A row of lancets at the laterals `lats`, each with the next palette of `palettes` (so no two neighbours match): the quick way to give a wall its windows."""
    done = 0
    for i, lat in enumerate(lats):
        if lancet(w, lat, y0, y1, palettes[i % len(palettes)], width=width, glow=glow):
            done += 1
    return done


# ===================================================================================================================
# arcades, decks, rails
# ===================================================================================================================
def arcade(h, axis, fixed, starts, n, y0, y_spring, y_top, pier=L.POLISHED, stairs=L.POLISHED_STAIRS):
    """An arcade of pointed arches in a one-thick wall. `axis` 'z': the wall lies at x = `fixed` and runs along z; 'x': it lies at z = `fixed` and runs along x.
    Every arch spans `n` cells (the two end cells are its piers, so neighbours do not share a cell: a pair of piers stands between two arches) starting at the
    coordinates in `starts`; it springs at row `y_spring` (straight jambs of `pier` below it) and is walled above up to `y_top`. The opening of an arch is
    n - 2 wide at the spring and narrows by one cell per side per row to a keystone. Why pairs of piers: two arches that share one cell would write two
    different upside-down stairs into it; with a pair each arch owns its own ring."""
    left, right = ("north", "south") if axis == "z" else ("west", "east")
    for a in starts:
        def put(u, y, block, props, a=a):
            pos = (fixed, y, a + u) if axis == "z" else (a + u, y, fixed)
            if block == L.POLISHED_STAIRS:
                h.set(*pos, stairs, dict(props or {}), over=True)
            elif block == L.POLISHED:
                h.set(*pos, pier, over=True)
            else:
                h.set(*pos, block, props, over=True)
        AK.pointed_arch(put, n, y_spring, y_top, "polished", "brick", left, right)
        for end in (a, a + n - 1):
            for y in range(y0, y_spring):
                pos = (fixed, y, end) if axis == "z" else (end, y, fixed)
                h.set(*pos, pier, over=True)


def rail(h, x1, z1, x2, z2, y, block=L.FENCE, skip=()):
    """A railing of `block` along a straight line of cells at row y (fences join up by themselves)."""
    for z in range(min(z1, z2), max(z1, z2) + 1):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            if (x, z) not in skip:
                h.try_set(x, y, z, block)


def solid(h, x1, z1, x2, z2, y1, y2, block=L.COBBLED):
    """Fills a box with `block` where it is still free (a fill for the cavity above a gallery roof or the body of a platform)."""
    for y in range(y1, y2 + 1):
        for z in range(z1, z2 + 1):
            for x in range(x1, x2 + 1):
                h.try_set(x, y, z, block)


def stairs_up(h, x, z, facing, steps, width=3, base=GROUND, stairs=L.POLISHED_STAIRS, under=L.COBBLED, fill_down=None, rail_col=None, rail=L.FENCE):
    """A flight of `steps` stairs climbing toward `facing` from (x, z), its first step at row `base`, `width` wide to the right of the climb. The surface at its
    top is base + steps (a deck whose floor block is at row base + steps - 1 is flush with it). `fill_down` is how many cells under each step are filled with
    `under` (None: all the way down to `base`, a solid flight; 1: a flight that hangs from the wall on a skin of stone); `rail_col` puts a `rail` on the
    column r = rail_col of the flight, one row above its steps."""
    fx, fz = D.DV[facing]
    rx, rz = D.DV[D.RIGHT[facing]]
    for k in range(steps):
        for r in range(width):
            cx, cz = x + k * fx + r * rx, z + k * fz + r * rz
            h.set(cx, base + k, cz, stairs, {"facing": facing, "half": "bottom"})
            lo = base if fill_down is None else max(base, base + k - fill_down)
            for yy in range(lo, base + k):
                h.set(cx, yy, cz, under)
            if rail_col == r and k < steps - 1:
                h.try_set(cx, base + k + 1, cz, rail)


def wagon_chandelier(h, cx, cz, y_top, drop, r=2, color="white", candles=True, rods=False):
    """A wagon-wheel chandelier hung from the block above (cx, y_top + 1, cz): a chain of `drop` cells, a square ring of iron bars of radius r with spokes
    to the chain, a lantern under each corner and each middle of a side (they hang from bars, which hold a hanging lantern), lit candles on the corners.
    Why not `mega_decor_r1.chandelier_wheel`: that one hangs every stem to the same row, which is wrong under a pointed vault, where the middle is higher than the sides."""
    y_ring = y_top - drop
    for y in range(y_ring + 1, y_top + 1):
        h.set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    cells = set()
    for d in range(-r, r + 1):
        cells.update({(cx + d, cz - r), (cx + d, cz + r), (cx - r, cz + d), (cx + r, cz + d)})
    for k in range(-r + 1, r):
        cells.update({(cx + k, cz), (cx, cz + k)})
    cells.add((cx, cz))
    for (x, z) in sorted(cells):
        h.set(x, y_ring, z, L.IRON_BARS)
    lamps = [(cx + sx * r, cz + sz * r) for sx in (-1, 1) for sz in (-1, 1)] + [(cx + r, cz), (cx - r, cz), (cx, cz + r), (cx, cz - r)]
    for (x, z) in lamps:
        h.set(x, y_ring - 1, z, L.LANTERN, {"hanging": "true"})
    if candles:
        for sx in (-1, 1):
            for sz in (-1, 1):
                D.candles_at(h, cx + sx * r, y_ring + 1, cz + sz * r, 2, color)
    if rods:
        for (x, z) in lamps:
            h.set(x, y_ring + 1, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})


# ===================================================================================================================
# gardens and water: plants only on grass, water only sealed
# ===================================================================================================================
def tree(h, x, z, trunk=4, radius=2, log="minecraft:oak_log", leaves="minecraft:oak_leaves", y0=GROUND):
    """A small round tree: a trunk of `trunk` logs and a ball of persistent leaves (they never decay in a template with no logs near them). The trunk stands on
    whatever block is at row y0 - 1 (the garden's grass)."""
    for k in range(trunk):
        h.set(x, y0 + k, z, log, {"axis": "y"})
    yc = y0 + trunk - 1
    for dy in range(-1, radius + 1):
        r = radius if dy <= 0 else max(1, radius - dy)
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dz * dz > r * r + 1 or (dx == 0 and dz == 0 and dy <= 0):
                    continue
                h.try_set(x + dx, yc + dy + 1, z + dz, leaves, {"distance": "1", "persistent": "true", "waterlogged": "false"})


def lamp_post(h, x, z, height=3, y0=GROUND, lantern=L.LANTERN):
    """A lamp post: a fence column `height` high with a standing lantern on its top (a lantern stands on the top of a fence)."""
    for k in range(height):
        h.set(x, y0 + k, z, L.FENCE)
    h.set(x, y0 + height, z, lantern, {"hanging": "false"})


def well(h, cx, cz, y0=GROUND, roof=True, rim=L.COBBLED, rim_wall=L.COBBLED_WALL):
    """A covered well centred on (cx, cz): a ring of `rim` blocks round one cell of still water (shut in on four sides and below by the rim and the floor), four
    fence posts carrying a slab roof, a lantern hung under it. The floor under the water must be solid (a plain floor row)."""
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dx == 0 and dz == 0:
                h.set(cx, y0, cz, "minecraft:water", {"level": "0"})
            else:
                h.set(cx + dx, y0, cz + dz, rim)
    if roof:
        for dx in (-1, 1):
            for dz in (-1, 1):
                for k in (1, 2):
                    h.set(cx + dx, y0 + k, cz + dz, L.FENCE)
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                h.set(cx + dx, y0 + 3, cz + dz, L.PLANKS_SLAB, {"type": "bottom"})
        h.set(cx, y0 + 2, cz, L.LANTERN, {"hanging": "true"})


def bench(h, x, z, facing, length=1, y0=GROUND, stairs=L.PLANKS_STAIRS):
    """Stair seats in a row of `length` along the axis perpendicular to `facing` (the direction the sitters look)."""
    fx, fz = D.DV[facing]
    rx, rz = D.DV[D.RIGHT[facing]]
    for k in range(length):
        h.set(x + k * rx, y0, z + k * rz, stairs, {"facing": OPP[facing], "half": "bottom"})


# ===================================================================================================================
# the mod's puzzle blocks (1.36.0 `nusmp:sigil_plate`, `nusmp:riddle_stone`, `nusmp:gatekeeper_door[kind=puzzle]`), written the way the Spire's templates write them
# ===================================================================================================================
PLATE = "nusmp:sigil_plate"
RIDDLE = "nusmp:riddle_stone"
GATE = "nusmp:gatekeeper_door"
LOOT_TRIAL = "nusmp:chests/labyrinth_trial"


def sigil_plate(h, x, y, z, color):
    """One sigil plate (colour 0 red, 1 blue, 2 gold, 3 green), unlit. A plate is a right click, never a pressure plate: nobody solves it by walking."""
    h.set(x, y, z, PLATE, {"color": str(color), "lit": "false"})


def riddle_stone(h, x, y, z, sequence):
    """The Riddle Stone with the order of colours its plates must be pressed in (the block entity tag `sequence`, a list of ints)."""
    import nbt as _nbt
    seq = _nbt.List([_nbt.Int(v) for v in sequence], tag_type=_nbt.TAG_INT)
    h.set(x, y, z, RIDDLE, None, _nbt.Compound({"id": RIDDLE, "sequence": seq}))


def puzzle_closet(h, f, top, loot=LOOT_TRIAL):
    """The partition, the teal puzzle door and the treasure closet behind it. Frame `f` stands on the cell of the room in front of the door's first column
    and looks into the closet: forward 1 is the partition, 2 to 5 the closet (four deep), the door is two wide (r = 0, 1) and three tall, the closet six wide
    (r = -2 .. 3). Everything behind the partition (nine cells to each side, six deep) is solid brick up to row `top` except the closet itself, so the closet
    cannot be entered except through the door (clipped to the room's interior). Two chests of the trial loot table stand against the back wall, candles and
    lanterns in the corners; the stone of the room must be within twelve cells of the plates and the door."""
    door = {(r, u, 1) for r in (0, 1) for u in (0, 1, 2)}
    jambs = {(r, u, 1) for r in (-1, 2) for u in range(0, 4)} | {(r, 3, 1) for r in (0, 1)}
    hollow = {(r, u, ff) for r in range(-2, 4) for u in range(0, 5) for ff in range(2, 6)}
    for r in range(-9, 10):
        for u in range(0, top - GROUND + 1):
            for ff in range(1, 7):
                cell = (r, u, ff)
                x, y, z = f.at(*cell)
                if cell in door or cell in jambs or cell in hollow or not h.inside(x, z) or not h.free(x, y, z):
                    continue
                h.set(x, y, z, L.BRICK)
    for (r, u, ff) in sorted(door):
        h.set(*f.at(r, u, ff), GATE, {"kind": "puzzle", "open": "false", "half": "lower" if u == 0 else "upper"})
    for (r, u, ff) in sorted(jambs):
        h.set(*f.at(r, u, ff), L.CHISELED if (u == 0 and r in (-1, 2)) or (u == 3 and r in (0, 1)) else L.POLISHED)
    for r in range(-2, 4):
        for ff in range(2, 6):
            x, _y, z = f.at(r, 0, ff)
            h.floor_set(x, z, L.POLISHED if (r + ff) % 2 == 0 else L.TILE)
    for r in (1, 0):
        x, y, z = f.at(r, 0, 5)
        h.loot_chest(x, y, z, f.dir("back"), loot)
    for r in (-2, 3):
        D.candles_at(h, *f.at(r, 0, 5), 4, "yellow")
        h.set(*f.at(r, 0, 3), L.LANTERN, {"hanging": "false"})


def sunken_lining(h, x1, z1, x2, z2, floor_row, block=None, skip=(), floor_fn=None):
    """The lining of a room sunk into the eight rows under the floor: its floor (row `floor_row`) and a ring one cell wide round it, from the floor row up to the row
    under the hall's floor. The cavity itself (x1..x2 by z1..z2, rows floor_row + 1 to FLOOR - 1) stays unset (air); the hall's own floor row is its ceiling. Why: the
    templates are placed with void under row 8, so a cavity without a lining would be a hole in the world. `skip` lists ring cells (x, z) left out (a stair comes through)."""
    block = block or L.COBBLED
    for z in range(z1 - 1, z2 + 2):
        for x in range(x1 - 1, x2 + 2):
            inside = x1 <= x <= x2 and z1 <= z <= z2
            if inside:
                h.set(x, floor_row, z, floor_fn(x, z) if floor_fn else (L.TILE if (x + z) % 2 else L.POLISHED))
            elif (x, z) not in skip:
                for y in range(floor_row, FLOOR):
                    h.set(x, y, z, D.brick_mix(h, 0.18, 0.3, block))


def sunken_lining_cells(h, cells, floor_row, block=None, skip=(), floor_fn=None):
    """`sunken_lining` for a cavity of any outline (a round chasm, a pool): `cells` is the set of (x, z) that is open from `floor_row` up to the hall's floor row,
    each given the floor `floor_fn(x, z)` (default: a chequer of tile and polished), and every cell that touches one of them, side or corner, and is not itself in
    `cells` or in `skip`, becomes a wall of brick from `floor_row` up to the row under the hall's floor. Why corners too: a wall that only closes the four sides
    leaves a diagonal crack that a player could see the void through. `skip` is for cells the caller builds itself (an island standing in the cavity)."""
    block = block or L.COBBLED
    ring = set()
    for (x, z) in cells:
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                n = (x + dx, z + dz)
                if n not in cells and n not in skip:
                    ring.add(n)
    for (x, z) in sorted(cells):
        h.set(x, floor_row, z, floor_fn(x, z) if floor_fn else (L.TILE if (x + z) % 2 else L.POLISHED))
    for (x, z) in sorted(ring):
        for y in range(floor_row, FLOOR):
            h.set(x, y, z, D.brick_mix(h, 0.18, 0.3, block))
    return ring


def hip_roof(h, x1, z1, x2, z2, y_spring, y_top, stairs=L.POLISHED_STAIRS, fill=L.BRICK, cap=None, glow=None):
    """A pyramidal vault (a hip roof seen from inside) over the rectangle [x1..x2] x [z1..z2]: from row `y_spring` the open area shrinks by one cell on every
    side per row, each step an upside-down stair facing outward (the corners turn into corner stairs by themselves, `mega_lib.autoconnect`), and everything
    outside the slope up to row `y_top` is solid `fill`. With `cap` (a block) the last ring is closed by that block (stained glass) and with `glow` the row above it
    is a glowing block, so the apex is a lantern of coloured light. Why: a square hall under a flat ceiling reads as a box; one slope on four sides reads as a
    canopy for the price of a few hundred blocks, and the stairs are palette blocks the theme restyles."""
    k = 0
    while x1 + k <= x2 - k and z1 + k <= z2 - k:
        y = y_spring + k
        if y > y_top:
            break
        last = (x1 + k + 1 > x2 - k - 1) or (z1 + k + 1 > z2 - k - 1)
        for z in range(z1 + k, z2 - k + 1):
            for x in range(x1 + k, x2 - k + 1):
                edge = x in (x1 + k, x2 - k) or z in (z1 + k, z2 - k)
                if not edge and not last:
                    continue
                if last and cap is not None:
                    h.set(x, y, z, cap, over=True)
                    if glow is not None:
                        if y + 1 <= y_top:
                            h.set(x, y + 1, z, glow, over=True)
                        else:
                            h.shell_set(x, y + 1, z, glow)
                    continue
                if x == x1 + k:
                    face = "west"
                elif x == x2 - k:
                    face = "east"
                elif z == z1 + k:
                    face = "north"
                else:
                    face = "south"
                h.set(x, y, z, stairs, {"facing": face, "half": "top"}, over=True)
        k += 1
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            inset = min(x - x1, x2 - x, z - z1, z2 - z)
            start = y_spring + inset + 1
            for y in range(start, y_top + 1):
                if h.free(x, y, z):
                    h.set(x, y, z, fill)


def round_wall(h, cx, cz, r, y1, block=None):
    """Makes the interior of a hall round: every free cell farther than r from (cx, cz) is filled solid from the floor up to row y1, so the room is a drum inside its
    square box. Returns the wall-mass cells (x, z) in a ring one to two cells deep behind the inner face (the places windows can be cut)."""
    import math
    block = block or L.BRICK
    ring = []
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - cx, z - cz)
            if d > r:
                for y in range(GROUND, y1 + 1):
                    if h.free(x, y, z):
                        h.set(x, y, z, D.brick_mix(h, 0.06, 0.04, block))
                if d <= r + 1.6:
                    ring.append((x, z))
    return ring


def dome(h, cx, cz, r0, y_spring, y_top, r_top=3.0, stairs=L.POLISHED_STAIRS, fill=L.BRICK):
    """A round vault over a drum: from row `y_spring` the open radius closes from r0 to r_top at row `y_top` along a quarter circle (steep at the foot, flat at the
    crown), every cell outside it up to row `y_top` is solid `fill`, and the cells whose underside shows (nothing under them) are upside-down stairs facing outward,
    so each ring is bevelled instead of a flat step. The drum under it must already be filled up to `y_spring - 1` (see `round_wall`). Why: a flat ceiling over a
    round hall reads as a lid; a dome costs a few hundred blocks and is the shape every tower of the Holy road ends in."""
    import math
    n = max(1, y_top - y_spring)

    def radius(y):
        t = min(1.0, (y - y_spring) / n)
        return r_top + (r0 - r_top) * math.sqrt(max(0.0, 1.0 - t * t))

    for y in range(y_spring, y_top + 1):
        rr = radius(y)
        for z in range(h.z0, h.z1 + 1):
            for x in range(h.x0, h.x1 + 1):
                d = math.hypot(x - cx, z - cz)
                if d <= rr or not h.free(x, y, z):
                    continue
                under_open = y > y_spring and h.free(x, y - 1, z) and math.hypot(x - cx, z - cz) <= radius(y - 1) + 1.0 and y < y_top
                if under_open and d <= rr + 1.0:
                    dx, dz = x - cx, z - cz
                    face = ("east" if dx > 0 else "west") if abs(dx) >= abs(dz) else ("south" if dz > 0 else "north")
                    h.set(x, y, z, stairs, {"facing": face, "half": "top"})
                else:
                    h.set(x, y, z, fill)


# ===================================================================================================================
# the Sealed Archway trial (1.36.0): written the way the Spire's and ROOMS-2's trial halls write it
# ===================================================================================================================
ARCHWAY = "nusmp:sealed_archway"
WARD_RUNE = "nusmp:ward_rune"
SPAWN_GLYPH = "nusmp:spawn_glyph"


def trial_core(h, mobs, glyphs, radius, reset=60, eye_row=None):
    """What every trial hall has: a ward rune in the floor of each doorway (the inner wall layer), a spawn glyph on the floor wherever the wave should appear, and the
    keystone (`nusmp:sealed_archway`, its block entity names the watched box, the roster and the reset time) hanging from the ceiling in the middle of the hall on a
    chain. `radius` is the half edge of the watched box (13 for a 27 x 27 hall); `mobs` is [(entity id, count)]. The hall must have a doorway on every side the party
    can come in by: the keystone seals every ward rune inside its box."""
    import nbt as _nbt
    eye_row = (h.top - 2) if eye_row is None else eye_row
    for (side, off) in h.doors:
        for lat in range(off - 1, off + 2):
            x, y, z = h.mt._lateral(side, off, lat, 1, FLOOR)
            h.shell_set(x, y, z, WARD_RUNE)
    for (x, z) in glyphs:
        h.shell_set(x, FLOOR, z, SPAWN_GLYPH)
    cx, cz = h.cx, h.cz
    for y in range(eye_row + 1, h.top + 1):
        h.set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    data = _nbt.Compound({"id": ARCHWAY, "radius": _nbt.Int(radius), "height": _nbt.Int(eye_row - (FLOOR - 1)),
                          "mobs": _nbt.List([_nbt.Compound({"id": i, "count": _nbt.Int(c)}) for i, c in mobs], tag_type=_nbt.TAG_COMPOUND),
                          "reset": _nbt.Int(reset)})
    h.set(cx, eye_row, cz, ARCHWAY, None, data)


def bar_window(w, lat, y0, y1, width=5, glow="minecraft:sea_lantern", block=L.IRON_BARS):
    """A pointed arch of iron bars in front of a bright glow: rows y0..y1, `width` cells wide at the bottom and one cell narrower on each side per row over the last
    width // 2 rows. Why: a window of the Sky Citadel is an opening into the weather, not a pane of glass; the bars keep it a wall and the glow behind is the sky."""
    half = width // 2
    n = 0
    for y in range(y0, y1 + 1):
        hw = min(half, y1 - y)
        for dx in range(-hw, hw + 1):
            if window_cell(w, lat + dx, y, block, glow):
                n += 1
    return n


def spiral(h, cx, cz, r, start, steps, base, stairs=L.POLISHED_STAIRS, under=L.COBBLED):
    """A spiral staircase on the square ring of radius r round (cx, cz): `steps` stairs clockwise (seen from above), the first at the ring cell `start` on row
    `base`, each next one a ring cell on and a row up, every stair facing the way it climbs, one block of `under` under every step so each touches the one before.
    Returns the ring cell after the last step (where the landing goes). Why not `mega_decor_r1.spiral_stairs`: it fills the whole column under every step, which
    collides with the step of the next turn; a spiral of two turns needs the cell free from row to row."""
    ring = D.ring_cells(cx, cz, r)
    i0 = ring.index(start)
    for k in range(steps):
        x, z = ring[(i0 + k) % len(ring)]
        nx, nz = ring[(i0 + k + 1) % len(ring)]
        d = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(nx - x, nz - z)]
        h.set(x, base + k, z, stairs, {"facing": d, "half": "bottom"})
        if k:
            h.set(x, base + k - 1, z, under)
    return ring[(i0 + steps) % len(ring)]


def candles(h, x, y, z, n=3, color="white"):
    D.candles_at(h, x, y, z, n, color)


def lit_candle_row(h, cells, y, color="white", n=2):
    for (x, z) in cells:
        if h.free(x, y, z):
            D.candles_at(h, x, y, z, n, color)


# ===================================================================================================================
# light that finds its own ceiling (decks, galleries, bridges: places `mega_decor_r1.auto_light` cannot reach)
# ===================================================================================================================
def _dark_spots(h, threshold):
    """The dark cells of the room under BOTH light models the gates use: `mega_decor_r1` (eye at 1.62 over the feet, walk graph of the doorways) and
    `mega_shape_preview` (eye one row over the feet, every cell a person can stand in and reach). [(light, x, z, feet row)], darkest first."""
    import mega_shape_preview as SP
    t = h.mt.t
    spots = {}
    holes = D.door_holes(t)
    nodes = D.walk_nodes(t)
    seen = D.reach(t, nodes, D._door_starts(h))
    lv = D.light_levels(t)
    for (x, z, s) in seen:
        ye = int(s / 2.0 + 1.62)
        v = lv.get((x, ye, z), 0)
        feet = s // 2 if s % 2 == 0 else (s - 1) // 2 + 1
        if v < threshold and (x, feet, z) not in holes and h.inside(x, z):
            spots[(x, z, feet)] = v
    lf = SP.light_field(t)
    for (x, y, z) in SP.standable_cells(t):
        v = lf.get((x, y + 1, z), 0)
        if v < threshold and (x, y, z) not in holes and h.inside(x, z):
            spots[(x, z, y)] = min(v, spots.get((x, z, y), 99))
    return sorted(((v, x, z, y) for (x, z, y), v in spots.items()))


def auto_light(h, soul=False, threshold=6, max_lamps=160, radius=2, min_clear=3, max_chain=9):
    """Hangs lanterns over the darkest walkable cells until every cell has block light >= `threshold` at eye level under both of the gates' light models. Unlike
    `mega_decor_r1.auto_light`, which hangs everything from the room's top row, each lamp here hangs from whatever solid block lies above its column (a gallery
    deck's roof, a loft, an arch): a chain of at most `max_chain` cells ends in a lantern at least `min_clear` rows over the floor, and only over floor on the
    SAME level as the dark cell. Why: the nave has three floors of different heights and the old pass left the galleries dark. Deterministic (the darkest cell
    first, ties by position). Returns the number of lamps hung."""
    n = 0
    bulb = L.SOUL_LANTERN if soul else L.LANTERN
    blocked = set()
    for _ in range(max_lamps):
        spots = [s for s in _dark_spots(h, threshold) if (s[1], s[2], s[3]) not in blocked]
        if not spots:
            break
        v, x, z, feet = spots[0]
        done = False
        for rad in (radius, radius + 2):
            cands = sorted(((dx * dx + dz * dz, x + dx, z + dz) for dx in range(-rad, rad + 1) for dz in range(-rad, rad + 1)))
            for (_, cx, cz) in cands:
                if not h.inside(cx, cz):
                    continue
                ground = h.get(cx, feet - 1, cz)
                if ground is None or D.classify(ground.block, ground.props)[0] != "floor":
                    continue                                  # over floor on the same level as the dark spot only
                for yl in (feet + min_clear, feet + min_clear + 1, feet + min_clear + 2, feet + min_clear - 1):
                    if not all(h.free(cx, yy, cz) for yy in range(feet, yl)):
                        continue
                    yc = yl + 1
                    while yc <= yl + max_chain + 1 and h.can_set(cx, yc, cz, "minecraft:chain") and h.free(cx, yc, cz):
                        yc += 1
                    above = h.get(cx, yc, cz)
                    if yc == yl + 1 or above is None or D.classify(above.block, above.props)[0] == "pass" or yc > yl + max_chain + 1:
                        continue
                    if not (h.can_set(cx, yl, cz, bulb) and h.free(cx, yl, cz)) or not AC.touches_bottom(above):
                        continue
                    for yy in range(yl + 1, yc):
                        h.set(cx, yy, cz, L.CHAIN_BLOCK, {"axis": "y"})
                    h.set(cx, yl, cz, bulb, {"hanging": "true"})
                    done = True
                    break
                if done:
                    break
            if done:
                break
        if not done:
            done = _wall_torch_near(h, x, z, feet)
        if done:
            n += 1
        else:
            blocked.add((x, z, feet))
    return n


def _wall_torch_near(h, x, z, feet, radius=3):
    """The fallback of `auto_light` where no chain can hang (a stair shaft thirty rows tall, a deck with nothing over it): a wall torch two rows over the floor on the
    nearest full wall block within `radius` cells on the same level, never in a doorway apron. Returns True when one was placed."""
    for rad in range(1, radius + 1):
        for (dx, dz) in sorted(((a, b) for a in range(-rad, rad + 1) for b in range(-rad, rad + 1)), key=lambda p: (abs(p[0]) + abs(p[1]), p)):
            cx, cz = x + dx, z + dz
            ground = h.get(cx, feet - 1, cz)
            if not h.inside(cx, cz) or ground is None or D.classify(ground.block, ground.props)[0] != "floor":
                continue
            for d, (ddx, ddz) in D.DV.items():
                back = h.get(cx + ddx, feet + 1, cz + ddz)
                if back is None or not L.is_full(back.block) or (cx + ddx, feet + 1, cz + ddz) in h.mt.reserved:
                    continue
                if h.can_set(cx, feet + 1, cz, "minecraft:wall_torch") and h.free(cx, feet + 1, cz):
                    h.set(cx, feet + 1, cz, "minecraft:wall_torch", {"facing": D.OPP[d]})
                    return True
    return False



# ===================================================================================================================
# the runner shared by the four generators
# ===================================================================================================================
def keep_doors_clear(h, depth=3, height=4):
    """Declares the volume in front of every doorway (3 wide, `depth` deep, `height` tall: the doorway's own four rows) as a route, so nothing solid, no chain and no
    lantern is ever put there. `Hall.door` keeps three rows clear; the Safe Zone gate (`mega_safe_verify`) asks for four, and a hanging lantern in the fourth is
    what it found first."""
    for (side, off) in h.doors:
        w = h.wall(side)
        a = w.cell(off - 1, GROUND, 0)
        b = w.cell(off + 1, GROUND + height - 1, depth - 1)
        h.volumes.append((min(a[0], b[0]), GROUND, min(a[2], b[2]), max(a[0], b[0]), GROUND + height - 1, max(a[2], b[2]), "doorway %s@%d" % (side, off)))


def safe_problems(h):
    """The gates of a Safe Zone (docs/labyrinth/mega_safe.md) on top of the hall's: `mega_safe_verify.verify` (shell, doors, every anchor reachable, light of at least
    6 on the whole floor, markers where the rest code reads them, counts), plus: no spawn and no faction marker in a safe room."""
    import mega_safe_verify as SV
    out = []
    for kind in ("spawn", "faction"):
        n = sum(1 for k, *_ in D.markers_of(h.mt.t) if k == kind)
        if n:
            out.append("a safe room carries no %s marker (%d found)" % (kind, n))
    h.mt.top = h.top
    rep = SV.verify(h.mt, min_light=6)
    for e in rep.errors:
        out.append("safe check: " + str(e))
    return out


def _same(h, path):
    import nbt as _nbt
    mine = _nbt.dumps(h.mt.t.to_nbt(), compress=True)
    with open(path, "rb") as f:
        return f.read() == mine


def run(rooms, argv, description):
    """Builds, checks and writes the rooms of one generator. `rooms` maps a short name to a function returning a finished-but-for-`finish()` Hall."""
    ap = argparse.ArgumentParser(description=description)
    ap.add_argument("names", nargs="*", help="rooms to build (default: all)")
    ap.add_argument("--check", action="store_true", help="write nothing; compare with the files on disk and run every check (exit 1 on any difference)")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--force", action="store_true", help="write a room even when a check fails (to look at it); the exit status stays 1")
    ap.add_argument("--quiet", action="store_true")
    a = ap.parse_args(argv)
    if a.list:
        for n in rooms:
            print(n)
        return 0
    names = a.names or list(rooms)
    bad = 0
    for n in names:
        if n not in rooms:
            print("unknown room", n)
            return 2
        h = rooms[n]()
        h.finish()
        problems, stats = h.check()
        if getattr(h, "no_spawn", False):
            problems = [p for p in problems if p != "no spawn marker"]       # a trial or a puzzle is not an ambush: the keystone sends the wave
        if getattr(h, "safe", False):
            problems = [p for p in problems if p != "no spawn marker"] + safe_problems(h)
        if problems:
            bad += 1
        if a.check:
            p = h.mt.t.path(L.RES)
            if not (os.path.exists(p) and _same(h, p)):
                print("STALE %s (the file on disk differs from the generator)" % n)
                bad += 1
        elif not problems or a.force:
            h.save()
        else:
            print("NOT WRITTEN %s: a room with a defect never reaches the data folder (--force writes it anyway)" % n)
        if not a.quiet or problems:
            print("%s %-34s %6d blocks %2d doors %2d stands  light min %s median %s dark %s%%  markers %s" % (
                "ok  " if not problems else "FAIL", n, stats.get("blocks", 0), len(h.doors), stats.get("armor_stands", 0), stats.get("eye_light_min"),
                stats.get("eye_light_median"), round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
            for p in problems:
                print("     - " + p)
    return 1 if bad else 0

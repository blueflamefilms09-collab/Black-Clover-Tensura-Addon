#!/usr/bin/env python3
"""
The Labyrinth Spire: every Jigsaw template of the spire (docs/labyrinth/a2_spire.md) and every template pool that strings them
together, generated from one script so the jigsaw positions, sizes and block ids of thirty-odd pieces cannot drift apart.

    python3 tools/labyrinth/gen_spire.py            # (re)writes the .nbt templates, the pool JSONs, the processor list, the example pack
    python3 tools/labyrinth/gen_spire.py --check    # regenerates in memory and verifies the files on disk + every jigsaw rule
    python3 tools/labyrinth/gen_spire.py --ascii labyrinth/base   # the layers of one piece

Why generated and not hand-built: Jigsaw only connects two pieces when the source jigsaw's `target` equals the other jigsaw's `name`,
the two face each other and the boxes do not overlap. A vertical spine needs the `nusmp:up` jigsaw of every level piece on its top row
at the exact horizontal centre and the `nusmp:down` of the next piece on its bottom row at the same centre, for every piece of every
pool in the chain. One helper (`stairwell`) writes those cells for all of them, so the rule holds by construction and `--check` proves it.

Deterministic: every piece gets its own seeded random (name-based) for the accents, so the same script writes the same bytes.
Idempotent: running it twice changes nothing. The heights are a budget (`HEIGHTS`): the spire is a jigsaw structure with
`max_distance_from_center` 128, a CUBE around the start piece, so base + levels + halls + crown must stay below 128 blocks.
"""
import argparse
import io
import json
import os
import random
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbt  # noqa: E402
from template import Template, facing, load  # noqa: E402

ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources")
POOL_DIR = os.path.join(RES, "data", "nusmp", "worldgen", "template_pool", "labyrinth")
PROC_LIST = os.path.join(RES, "data", "nusmp", "worldgen", "processor_list", "labyrinth_interior.json")
EXAMPLE_DIR = os.path.join(ROOT, "docs", "labyrinth", "wildcard_example")
EXAMPLE_ZIP = os.path.join(ROOT, "docs", "labyrinth", "labyrinth_wildcard_example.zip")

# ---- the palette (CONTRACT "Palette convention": B's processor swaps the tagged ones for the biome's look) ----
BRICKS = "nusmp:labyrinth_bricks"      # walls, never swapped: the veins grow in it
TILES = "nusmp:labyrinth_tiles"        # floors (swap_floor)
PILLAR = "nusmp:labyrinth_pillar"      # columns (swap_pillar), axis=y
GLYPH = "nusmp:labyrinth_glyph"        # emissive accent (swap_accent)
LANTERN = "nusmp:labyrinth_lantern"    # light 15
GOLD = "minecraft:gold_block"
AMETHYST = "minecraft:amethyst_block"  # swap_accent
CLUSTER = "minecraft:amethyst_cluster"
SEA = "minecraft:sea_lantern"
PLANKS = "minecraft:dark_oak_planks"   # swap_wood
FENCE = "minecraft:dark_oak_fence"
AIR = "minecraft:air"
GLASS = "minecraft:glass"
EXIT = "nusmp:labyrinth_exit"          # C
ARCHWAY = "nusmp:sealed_archway"       # E1 (block entity)
RUNE = "nusmp:ward_rune"               # E1
SPAWN_GLYPH = "nusmp:spawn_glyph"      # E1
DOOR = "nusmp:gatekeeper_door"         # E2
ALTAR = "nusmp:peak_altar"             # E2 (block entity)
PEDESTAL = "nusmp:relic_pedestal"      # D (block entity)
PITFALL = "nusmp:pitfall_tile"         # F1 (1.36.0): props safe=true|false, open=false
SNUFFER = "nusmp:snuffer_glyph"        # F1: floor glyph that snuffs light
PLATE = "nusmp:sigil_plate"            # F1: props color=0..3, lit=false
RIDDLE = "nusmp:riddle_stone"          # F1 (block entity, nbt {sequence:[...]})
LADDER = "minecraft:ladder"
SPIKE = "minecraft:pointed_dripstone"
ROCK = ("minecraft:deepslate", "minecraft:deepslate", "minecraft:tuff", "minecraft:cobbled_deepslate", "minecraft:deepslate_bricks")

LOOT_TRIAL = "nusmp:chests/labyrinth_trial"
LOOT_VAULT = "nusmp:chests/dungeon_relic_vault"
LOOT_PEDESTAL = "nusmp:chests/labyrinth_pedestal"

# ---- pools and jigsaw names (CONTRACT "Template pools" / "Jigsaw names") ----
POOL = "nusmp:labyrinth/%s"
PROCESSORS = "nusmp:labyrinth_interior"
CHAIN = ["base", "ascent_1", "ascent_2", "warden", "ascent_3", "ascent_4", "peak", "vault", "crown"]
UP, DOWN = "nusmp:up", "nusmp:down"
SIDE_OUT, SIDE_IN = "nusmp:side_out", "nusmp:side_in"
WILD_OUT, WILD_IN = "nusmp:wild_out", "nusmp:wild_in"
SPINE_PRIORITY = 10  # the vertical jigsaws are selected and their pieces placed before any side room

#: Heights: base 16, four ascent levels of 12, warden 16, peak 18, vault 14, crown 16 = 128. The structure's free box is
#: start_y +- max_distance_from_center (128), so the whole stack must fit in 128 blocks above the base's bottom row.
HEIGHTS = {"base": 16, "level": 12, "warden": 16, "peak": 18, "vault": 14, "crown": 16}
MAX_DISTANCE_FROM_CENTER = 128
LEVEL_SIZE = 25
WARDEN_VESTIBULE_RUNES = True  # ward runes at the Warden hall's arrival vestibule door (needs E1's armed-by-doorway rule; see the doc)

SEALED_ARCH_SIDE = "caps/sealed_arch"


# =====================================================================================================================
# building helpers
# =====================================================================================================================

class Piece:
    """A template plus a conflict guard: shell cells (floor/walls/roof) may be overwritten by features, a feature cell may not be
    overwritten by anything. A silent overwrite is how a chest ends up inside a stair, so it is an error here, and --check re-runs it."""

    def __init__(self, name, sx, sy, sz, seed=None):
        self.t = Template(name, sx, sy, sz)
        self.size = (sx, sy, sz)
        self.cx, self.cz = sx // 2, sz // 2
        self.feature = set()
        self.rng = random.Random("nusmp:" + name if seed is None else seed)

    def shell(self, x, y, z, block, props=None):
        if (x, y, z) in self.feature:
            raise ValueError("%s: shell block %s at (%d,%d,%d) would overwrite a feature" % (self.t.name, block, x, y, z))
        self.t.set(x, y, z, block, props)

    def put(self, x, y, z, block, props=None, nbt_=None):
        if (x, y, z) in self.feature:
            cur = self.t.get(x, y, z)
            if nbt_ is None and cur.nbt is None and cur.block == block and cur.props == {k: str(v) for k, v in (props or {}).items()}:
                return  # the very same block twice (two walls meeting): not a conflict
            raise ValueError("%s: feature %s at (%d,%d,%d) overwrites another feature (%s)" % (self.t.name, block, x, y, z, cur))
        self.t.set(x, y, z, block, props, nbt_)
        self.feature.add((x, y, z))

    def box(self, a, b, block, props=None, shell=True):
        (x1, y1, z1), (x2, y2, z2) = (min(a[0], b[0]), min(a[1], b[1]), min(a[2], b[2])), (max(a[0], b[0]), max(a[1], b[1]), max(a[2], b[2]))
        for y in range(y1, y2 + 1):
            for z in range(z1, z2 + 1):
                for x in range(x1, x2 + 1):
                    (self.shell if shell else self.put)(x, y, z, block, props)

    def column(self, x, z, y1, y2, block=PILLAR, props=None, shell=False):
        self.box((x, y1, z), (x, y2, z), block, props if props is not None else ({"axis": "y"} if block == PILLAR else None), shell)

    def jigsaw(self, x, y, z, orientation, name, target, pool, final_state=AIR, joint="aligned", priority=0):
        self.t.jigsaw(x, y, z, orientation, name, target, pool, final_state, joint, priority, priority)
        self.feature.add((x, y, z))

    def chest(self, x, y, z, table, facing_="north"):
        self.t.chest(x, y, z, table, facing_)
        self.feature.add((x, y, z))

    def be(self, x, y, z, block, data, props=None):
        self.t.be(x, y, z, block, nbt.Compound(data), props)
        self.feature.add((x, y, z))


def shell_room(p, pilaster_every=6, pilasters=True):
    """Floor of tiles, roof and walls of bricks, pillar pilasters in the walls (corners always, then every N cells, never at a wall
    centre where a doorway may go)."""
    sx, sy, sz = p.size
    p.box((0, 0, 0), (sx - 1, 0, sz - 1), TILES)
    p.box((0, sy - 1, 0), (sx - 1, sy - 1, sz - 1), BRICKS)
    for y in range(1, sy - 1):
        for x in range(sx):
            p.shell(x, y, 0, BRICKS)
            p.shell(x, y, sz - 1, BRICKS)
        for z in range(sz):
            p.shell(0, y, z, BRICKS)
            p.shell(sx - 1, y, z, BRICKS)
    if not pilasters:
        return
    xs = [x for x in range(0, sx, pilaster_every) if abs(x - p.cx) > 1] + [sx - 1]
    zs = [z for z in range(0, sz, pilaster_every) if abs(z - p.cz) > 1] + [sz - 1]
    for x in sorted(set(xs)):
        p.column(x, 0, 1, sy - 2, shell=True)
        p.column(x, sz - 1, 1, sy - 2, shell=True)
    for z in sorted(set(zs)):
        p.column(0, z, 1, sy - 2, shell=True)
        p.column(sx - 1, z, 1, sy - 2, shell=True)


def wall_accents(p, n_glyph, n_lantern, y_lo=4, y_hi=None):
    """Seeded glyph and lantern blocks set INTO the four walls (never in a doorway column or a corner)."""
    sx, sy, sz = p.size
    y_hi = y_hi or sy - 3
    spots = []
    for x in range(2, sx - 2):
        if abs(x - p.cx) > 2:
            spots += [(x, 0), (x, sz - 1)]
    for z in range(2, sz - 2):
        if abs(z - p.cz) > 2:
            spots += [(0, z), (sx - 1, z)]
    p.rng.shuffle(spots)
    for i, (x, z) in enumerate(spots[:n_glyph + n_lantern]):
        y = p.rng.randint(y_lo, y_hi)
        p.shell(x, y, z, GLYPH if i < n_glyph else LANTERN)


def doorway(p, side, width=3, height=4, name=SIDE_OUT, target=SIDE_IN, pool=POOL % "side", runes=False, jig=True, y0=1):
    """A `width` x `height` opening in an outer wall at floor level with the jigsaw in its bottom centre cell, facing out.
    side: 'east' (+x wall), 'west', 'south' (+z), 'north'. `y0` (1.36.0, default 1 = every older piece) is the row of the jigsaw and of the
    opening's bottom: the pit room's doorway sits at 4 because its floor is sunk three blocks to make room for the pit."""
    sx, sy, sz = p.size
    h = width // 2
    if side in ("east", "west"):
        x = sx - 1 if side == "east" else 0
        cells = [(x, y, p.cz + d) for d in range(-h, h + 1) for y in range(y0, y0 + height)]
        jx, jz, orient = x, p.cz, side + "_up"
        floor = [(x, y0 - 1, p.cz + d) for d in range(-h, h + 1)]
    else:
        z = sz - 1 if side == "south" else 0
        cells = [(p.cx + d, y, z) for d in range(-h, h + 1) for y in range(y0, y0 + height)]
        jx, jz, orient = p.cx, z, side + "_up"
        floor = [(p.cx + d, y0 - 1, z) for d in range(-h, h + 1)]
    for c in cells:
        p.put(*c, AIR)
    if jig:
        p.feature.discard((jx, y0, jz))
        p.jigsaw(jx, y0, jz, orient, name, target, pool, AIR, "aligned")
    if runes:
        for c in floor:
            p.put(*c, RUNE)
    return (jx, y0, jz)


def support(p, x, y1, y2, z):
    """Solid brick under a step: cells that already hold a solid feature (a vestibule wall, a roof) are kept, an air feature is a bug."""
    for y in range(y1, y2 + 1):
        if (x, y, z) in p.feature:
            if p.t.get(x, y, z).block == AIR:
                raise ValueError("%s: support at (%d,%d,%d) would fill an air cell" % (p.t.name, x, y, z))
            continue
        p.put(x, y, z, BRICKS)


def stairwell(p, gap=3):
    """The spine: a 3x3 hole in the floor with the `nusmp:down` jigsaw in its centre (facing down), a switchback stair that climbs to
    a landing in the roof row; the `nusmp:up` jigsaw (facing up, final state air) is the roof-row cell above the axis. Flight A climbs +x in rows
    cz+gap..cz+gap+2 (reached from the west over the floor), a turn landing, flight B comes back in rows cz-1..cz+1 and ends one cell
    east of the axis, two ramp steps inside the hole zone and the landing on the west side of the axis. A full-block stair is climbed by
    jumping, which needs three free cells above the tread you jump from: the roof over flight B's top steps is cut, and the last two
    rises happen inside the 3x3 floor hole of the piece ABOVE (every piece has one, whatever its rotation, because it is centred).
    Heights: block rows 1..H-2 for the steps (H = piece height), the landing strip in row H-1. The emergence zone (Chebyshev distance
    <= 1 around the hole, rows 0..3) stays air for players coming up from the piece below."""
    sx, sy, sz = p.size
    cx, cz = p.cx, p.cz
    n = sy - 2
    if n % 2:
        raise ValueError("%s: a switchback stair needs an even number of steps, height %d gives %d" % (p.t.name, sy, n))
    a = n // 2
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.put(cx + dx, 0, cz + dz, AIR)
            for y in (1, 2, 3):
                p.put(cx + dx, y, cz + dz, AIR)
    p.feature.discard((cx, 0, cz))
    p.jigsaw(cx, 0, cz, "down_north", DOWN, UP, POOL % "caps", AIR, "rollable", SPINE_PRIORITY)
    za, zb = range(cz + gap, cz + gap + 3), range(cz - 1, cz + 2)
    for k in range(1, a + 1):                                   # flight A (out, +x), solid under
        for z in za:
            p.put(cx + k, k, z, TILES)
            support(p, cx + k, 1, k - 1, z)
    for z in range(cz - 1, cz + gap + 3):                       # the turn landing, level with the top of flight A
        p.put(cx + a + 1, a, z, TILES)
        support(p, cx + a + 1, 1, a - 1, z)
    for k in range(2, a + 1):                                   # a wall between the two flights
        for y in range(1, 5):
            if (cx + k, y, cz + gap - 1) not in p.feature:  # a vestibule wall may already stand there
                p.put(cx + k, y, cz + gap - 1, BRICKS)
    for k in range(a, 0, -1):                                   # flight B (back, -x); the steps nearest the axis fly
        h = 2 * a + 1 - k if k >= 2 else 2 * a - 1              # the last step before the hole is level with the one before it
        for z in zb:
            p.put(cx + k, h, z, TILES)
            if k >= 3:
                support(p, cx + k, 1, h - 1, z)
    for z in zb:
        p.put(cx, 2 * a, z, TILES)                              # the hole ramp: the last two rises happen INSIDE the 3x3 hole zone of
        p.put(cx - 1, sy - 1, z, TILES)                         # the piece above (open head room); the landing is the roof row west
        for dx in range(0, 4):                                  # of the axis. The roof is cut over x = cx..cx+3 so a jump from any step
            p.put(cx + dx, sy - 1, z, AIR)                      # of flight B has three free cells above the tread.
    p.feature.discard((cx, sy - 1, cz))
    return a



def up_jigsaw(p, pool):
    p.jigsaw(p.cx, p.size[1] - 1, p.cz, "up_north", UP, DOWN, pool, TILES if p.t.name == "labyrinth/base" else AIR, "rollable", SPINE_PRIORITY)


def vestibule(p, radius=3, height=4):
    """A walled square around the arrival hole (walls at Chebyshev distance `radius`, a roof one above the walls): the Warden hall and the
    Boss Peak turn the stair mouth into a small room whose one doorway a trial or a Gatekeeper door can seal. A lantern hangs in each
    inner corner."""
    cx, cz = p.cx, p.cz
    for dx in range(-radius, radius + 1):
        for dz in range(-radius, radius + 1):
            if max(abs(dx), abs(dz)) == radius:
                corner = abs(dx) == radius and abs(dz) == radius
                for y in range(1, height + 1):
                    p.put(cx + dx, y, cz + dz, PILLAR if corner else BRICKS, {"axis": "y"} if corner else None)
            p.put(cx + dx, height + 1, cz + dz, BRICKS)
    for sx_, sz_ in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
        p.put(cx + sx_ * (radius - 1), height, cz + sz_ * (radius - 1), LANTERN)



def gatekeeper_door(p, cells_lower, kind):
    """A 2-wide column pair of Gatekeeper doors: the bottom row half=lower, every row above half=upper, closed."""
    for (x, y, z, rows) in cells_lower:
        p.put(x, y, z, DOOR, {"kind": kind, "open": "false", "half": "lower"})
        for dy in range(1, rows):
            p.put(x, y + dy, z, DOOR, {"kind": kind, "open": "false", "half": "upper"})


def archway_nbt(radius, height, mobs, reset):
    return {"id": ARCHWAY, "radius": nbt.Int(radius), "height": nbt.Int(height),
            "mobs": nbt.List([nbt.Compound({"id": i, "count": nbt.Int(c)}) for i, c in mobs], tag_type=nbt.TAG_COMPOUND),
            "reset": nbt.Int(reset)}


def altar_nbt():
    bosses = [("nusmp:dante_zogratis", 3), ("nusmp:vanica_zogratis", 3), ("nusmp:zenon_zogratis", 3), ("nusmp:floor_warden", 2)]
    return {"id": ALTAR, "radius": nbt.Int(16),
            "bosses": nbt.List([nbt.Compound({"id": i, "weight": nbt.Int(w)}) for i, w in bosses], tag_type=nbt.TAG_COMPOUND)}


def pedestal_nbt():
    return {"id": PEDESTAL, "LootTable": LOOT_PEDESTAL, "LootTableSeed": nbt.Long(0)}


def floor_ring(p, cx, cz, r, block, every=2, y=0):
    """Accent blocks on a square ring of Chebyshev radius r (every other cell), skipping the stair footprint."""
    for dx in range(-r, r + 1):
        for dz in range(-r, r + 1):
            if max(abs(dx), abs(dz)) == r and (dx + dz) % every == 0:
                x, z = cx + dx, cz + dz
                if (x, y, z) not in p.feature and 0 < x < p.size[0] - 1 and 0 < z < p.size[2] - 1:
                    p.shell(x, y, z, block)


# =====================================================================================================================
# the pieces
# =====================================================================================================================

def build_base():
    """labyrinth/base, 41x16x41: the floating island. Rock underside (y 0..3) tapering down, the arrival pad at y=4 (the contract's
    arrival point is the pad centre, y=5), the exit gate at (centre+3, 5, centre), a ring colonnade, four flying stairways that meet
    at the landing in the top row, which is the `nusmp:up` jigsaw of the whole spire."""
    s = 41
    p = Piece("labyrinth/base", s, HEIGHTS["base"], s)
    cx, cz = p.cx, p.cz
    for y, r in enumerate((7.5, 12.5, 16.5, 19.5)):
        for x in range(s):
            for z in range(s):
                if (x - cx) ** 2 + (z - cz) ** 2 <= r * r:
                    p.shell(x, y, z, p.rng.choice(ROCK))
    for x in range(s):  # the pad: a disc of tiles with a glyph ring and gold cross inlays
        for z in range(s):
            dx, dz = x - cx, z - cz
            r2 = dx * dx + dz * dz
            if r2 <= 20.5 ** 2:
                p.shell(x, 4, z, TILES)
                if 8.5 ** 2 <= r2 < 9.5 ** 2 and (dx + dz) % 2 == 0:
                    p.shell(x, 4, z, GLYPH)
                if (dx == 0 or dz == 0) and 6 <= abs(dx + dz) <= 18 and (dx + dz) % 2 == 0:
                    p.shell(x, 4, z, GOLD)
                if 19.5 ** 2 <= r2:
                    p.shell(x, 5, z, BRICKS)  # parapet
    import math
    for i in range(16):  # colonnade at radius 16 with an entablature ring at y=11
        ang = i * math.pi / 8
        x, z = cx + int(round(16 * math.cos(ang))), cz + int(round(16 * math.sin(ang)))
        p.column(x, z, 5, 10)
        p.put(x, 11, z, LANTERN if i % 2 == 0 else GOLD)
    for x in range(s):
        for z in range(s):
            r2 = (x - cx) ** 2 + (z - cz) ** 2
            if 15.5 ** 2 <= r2 <= 16.5 ** 2 and (x, 11, z) not in p.feature:
                p.shell(x, 11, z, BRICKS)
    # the exit gate and its frame (clear of the +x stair, which is at y 12 above it)
    p.put(cx + 3, 5, cz, EXIT)
    for dz in (-1, 1):
        p.column(cx + 3, cz + dz, 5, 7)
    for dz in (-1, 0, 1):
        p.put(cx + 3, 8, cz + dz, GLYPH if dz == 0 else BRICKS)
    # four stairways: the tread at distance d is at row 15-d (three wide), level at row 12 from d=3 inwards, arcaded (brick-filled) on the
    # outer rows from d=5. A jump needs three free cells above the take-off tread and the next piece's floor starts at y=16, so the last
    # rises are taken INSIDE that piece's 3x3 hole zone: a ring road at row 12 (Chebyshev distance 2) joins the four stairways, one
    # ramp climbs from its east side through the hole zone (rows 13, 14, 15) to the landing beside the axis, and the up jigsaw cell
    # (row 15, final state tiles) is the centre of the landing.
    for ddx, ddz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for d in range(2, 11):
            h = min(15 - d, 12)
            for w in (-1, 0, 1):
                x, z = cx + ddx * d + ddz * w, cz + ddz * d + ddx * w
                if (x, h, z) in p.feature:  # the arms share their innermost cells
                    continue
                p.put(x, h, z, GLYPH if (d == 10 and w == 0) else TILES)
                if d >= 5 and w != 0:
                    for y in range(5, h):
                        p.put(x, y, z, BRICKS)
    for dx in range(-2, 3):
        for dz in range(-2, 3):
            if max(abs(dx), abs(dz)) == 2 and (cx + dx, 12, cz + dz) not in p.feature:
                p.put(cx + dx, 12, cz + dz, TILES)
    p.put(cx + 1, 13, cz, TILES)
    p.put(cx + 1, 14, cz + 1, TILES)
    p.put(cx, 15, cz + 1, TILES)
    # the first-tier footprint rests on eight pillars
    for x, z in ((cx - 12, cz - 12), (cx + 12, cz - 12), (cx - 12, cz + 12), (cx + 12, cz + 12), (cx - 12, cz), (cx + 12, cz), (cx, cz - 12), (cx, cz + 12)):
        p.column(x, z, 5, 15)
    up_jigsaw(p, POOL % "ascent_1")
    return p


def level_piece(kind, n, next_pool):
    """hall_n / gallery_n / shrine_n: 25x12x25, centred on the axis, the spine stair, side doorways on the outer walls."""
    s, h = LEVEL_SIZE, HEIGHTS["level"]
    p = Piece("labyrinth/ascent_%d/%s_%d" % (n, kind, n), s, h, s)
    cx, cz = p.cx, p.cz
    shell_room(p)
    wall_accents(p, 6 + n, 4)
    if kind == "hall":
        for x, z in ((6, 6), (6, 18), (18, 6), (18, 18)):
            p.column(x, z, 1, h - 2)
            p.feature.discard((x, 8, z))
            p.put(x, 8, z, LANTERN)
            p.feature.discard((x, h - 2, z))
            p.put(x, h - 2, z, GOLD)
        floor_ring(p, cx, cz, 4, GLYPH)
        floor_ring(p, cx, cz, 8, GOLD, every=4)
        doorway(p, "east")
        doorway(p, "west")
    elif kind == "gallery":
        for x in range(1, s - 1):  # mezzanine ring at y=6 (planks), 4 wide, railed on its inner edge, on pillars at the inner corners
            for z in range(1, s - 1):
                m = min(x, s - 1 - x, z, s - 1 - z)
                if 1 <= m <= 4:
                    p.put(x, 6, z, PLANKS)
                    if m == 4:
                        p.put(x, 7, z, FENCE)
        for x, z in ((5, 5), (5, 19), (19, 5), (19, 19)):
            p.column(x, z, 1, 5)
        for x, z in ((8, 8), (8, 16), (16, 8), (16, 16)):  # the light well: sea lanterns in the roof
            p.shell(x, h - 1, z, SEA)
        floor_ring(p, cx, cz, 6, GLYPH, every=3)
        doorway(p, "east")
        doorway(p, "west")
        doorway(p, "south")
    elif kind == "shrine":
        sx_, sz_ = 5, cz  # the statue: nine pillars of three heights near the -x wall, a gold crown, amethyst at the feet
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                top = 8 if (dx, dz) == (0, 0) else (6 if dx == 0 or dz == 0 else 4)
                p.column(sx_ + dx, sz_ + dz, 1, top)
                if (dx, dz) != (0, 0):
                    p.put(sx_ + dx, top + 1, sz_ + dz, GLYPH if dx == 0 or dz == 0 else AMETHYST)
        p.put(sx_, 9, sz_, GOLD)
        p.put(sx_, 10, sz_, SEA)
        floor_ring(p, sx_, sz_, 2, GLYPH, every=1)
        floor_ring(p, sx_, sz_, 3, GOLD, every=2)
        p.chest(sx_, 1, sz_ + 5, LOOT_TRIAL, "north")
        p.chest(sx_, 1, sz_ - 5, LOOT_TRIAL, "south") if n % 2 == 0 else None
        doorway(p, "south")
    else:
        raise ValueError(kind)
    stairwell(p)
    up_jigsaw(p, next_pool)
    return p


def build_warden():
    """labyrinth/warden/hall, 31x16x31: the Floor Warden's hall. Arrival vestibule with a ward-rune doorway and the Sealed Archway
    keystone over it; a walled stair foot with its own runes; three spawn glyphs; the up jigsaw continues to ascent_3."""
    s, h = 31, HEIGHTS["warden"]
    p = Piece("labyrinth/warden/hall", s, h, s)
    cx, cz = p.cx, p.cz
    shell_room(p, pilaster_every=5)
    wall_accents(p, 10, 6, 5, h - 4)
    for x, z in ((6, 6), (6, 24), (24, 6), (24, 24)):
        p.column(x, z, 1, h - 2)
        p.feature.discard((x, 10, z))
        p.put(x, 10, z, LANTERN)
    floor_ring(p, cx, cz, 7, GLYPH, every=2)
    floor_ring(p, cx, cz, 11, GOLD, every=3)
    for x, z in ((cx - 9, cz - 9), (cx + 9, cz - 9), (cx - 9, cz + 9)):
        p.put(x, 0, z, SPAWN_GLYPH)
    vestibule(p, 3, 4)
    # the vestibule doorway (-x side, 3x4) with runes and the keystone
    for dz in (-1, 0, 1):
        for y in range(1, 5):
            p.feature.discard((cx - 3, y, cz + dz))
            p.put(cx - 3, y, cz + dz, AIR)
        if WARDEN_VESTIBULE_RUNES:
            p.put(cx - 3, 0, cz + dz, RUNE)
    p.feature.discard((cx - 3, 5, cz))
    p.be(cx - 3, 5, cz, ARCHWAY, archway_nbt(14, 12, [("nusmp:floor_warden", 1), ("nusmp:living_armor", 2)], 90))
    a = stairwell(p, gap=4)
    # the stair foot enclosure: a wall on the outer row of flight A, the entry face at x=cx is an open 3x4 doorway on runes
    for k in range(1, a + 2):
        for y in range(1, 5):
            p.put(cx + k, y, cz + 7, BRICKS)
    for dz in (4, 5, 6):
        p.put(cx, 0, cz + dz, RUNE)
    for y in range(1, 6):
        p.put(cx, y, cz + 7, BRICKS)
        p.put(cx, y, cz + 3, BRICKS)
    for dz in (4, 5, 6):
        p.put(cx, 5, cz + dz, GLYPH if dz == 5 else BRICKS)
    up_jigsaw(p, POOL % "ascent_3")
    return p


def build_peak():
    """labyrinth/peak/boss_peak, 31x18x31: arrival vestibule sealed by a 2x3 Gatekeeper door (kind=warden), the Peak Altar on the
    vestibule's roof at the exact horizontal centre (a dais with a stair), the walled stair foot behind it closed by a 2x3 door
    (kind=boss) that opens when the altar's boss dies; the up jigsaw continues to the vault."""
    s, h = 31, HEIGHTS["peak"]
    p = Piece("labyrinth/peak/boss_peak", s, h, s)
    cx, cz = p.cx, p.cz
    shell_room(p, pilaster_every=5)
    wall_accents(p, 12, 8, 5, h - 4)
    for x, z in ((5, 5), (5, 25), (25, 5), (25, 25)):
        p.column(x, z, 1, h - 2)
        p.feature.discard((x, 12, z))
        p.put(x, 12, z, LANTERN)
        p.feature.discard((x, h - 2, z))
        p.put(x, h - 2, z, AMETHYST)
    floor_ring(p, cx, cz, 8, GLYPH, every=2)
    floor_ring(p, cx, cz, 12, GOLD, every=4)
    vestibule(p, 3, 4)
    # warden door: 2 wide x 3 tall in the -x wall of the vestibule
    for dz in (-1, 0, 1):
        for y in range(1, 5):
            p.feature.discard((cx - 3, y, cz + dz))
    gatekeeper_door(p, [(cx - 3, 1, cz - 1, 3), (cx - 3, 1, cz, 3)], "warden")
    for y in (1, 2, 3):
        p.put(cx - 3, y, cz + 1, BRICKS)
    for dz in (-1, 0, 1):
        p.put(cx - 3, 4, cz + dz, GLYPH if dz == 0 else BRICKS)
    # the altar on the dais (the vestibule roof, y=5) with a 3-wide stair up its -z face
    p.feature.discard((cx, 5, cz))
    p.put(cx, 5, cz, GOLD)
    p.be(cx, 6, cz, ALTAR, altar_nbt())
    for dx in (-1, 1):
        p.put(cx + dx, 6, cz + dx, GLYPH)
        p.put(cx + dx, 6, cz - dx, GLYPH)
    for k in range(1, 5):
        for dx in (-1, 0, 1):
            p.put(cx + dx, k, cz - 8 + k, TILES)
            for y in range(1, k):
                p.put(cx + dx, y, cz - 8 + k, BRICKS)
    a = stairwell(p, gap=4)
    for k in range(1, a + 2):
        for y in range(1, 5):
            p.put(cx + k, y, cz + 7, BRICKS)
    for y in range(1, 6):
        p.put(cx, y, cz + 7, BRICKS)
        p.put(cx, y, cz + 3, BRICKS)
    gatekeeper_door(p, [(cx, 1, cz + 4, 3), (cx, 1, cz + 5, 3)], "boss")
    for y in (1, 2, 3):
        p.put(cx, y, cz + 6, BRICKS)
    for dz in (4, 5, 6):
        p.put(cx, 4, cz + dz, BRICKS)
        p.put(cx, 5, cz + dz, GLYPH if dz == 5 else BRICKS)
    up_jigsaw(p, POOL % "vault")
    return p


def build_vault():
    """labyrinth/vault/treasury, 31x14x31: the grand vault. Raised dais with six Relic Pedestals round a glyph spire crowned by
    amethyst, four relic-vault chests in the corners, two trial chests by the stair, gold inlays; the up jigsaw goes to the crown."""
    s, h = 31, HEIGHTS["vault"]
    p = Piece("labyrinth/vault/treasury", s, h, s)
    cx, cz = p.cx, p.cz
    shell_room(p, pilaster_every=5)
    wall_accents(p, 12, 8, 4, h - 4)
    for x in range(1, s - 1):
        for z in range(1, s - 1):
            if (x + z) % 7 == 0 and abs(x - cx) > 2 and abs(z - cz) > 2:
                p.shell(x, 0, z, GOLD)
    # the dais (x 3..11, z 11..19), one high, gold rim
    for x in range(3, 12):
        for z in range(11, 20):
            p.put(x, 1, z, GOLD if (x in (3, 11) or z in (11, 19)) else BRICKS)
    for dx in (-1, 0, 1):  # the spire: amethyst ring, glyph column, sea lantern tip
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                p.put(7 + dx, 2, cz + dz, AMETHYST)
                p.put(7 + dx, 3, cz + dz, CLUSTER, {"facing": "up", "waterlogged": "false"})
    p.column(7, cz, 2, h - 3, GLYPH)
    p.put(7, h - 2, cz, SEA)
    for x, z in ((4, cz - 2), (4, cz + 2), (7, cz - 3), (7, cz + 3), (10, cz - 2), (10, cz + 2)):
        p.be(x, 2, z, PEDESTAL, pedestal_nbt())
    for x, z, f in ((2, 2, "south"), (s - 3, 2, "south"), (2, s - 3, "north"), (s - 3, s - 3, "north")):
        p.chest(x, 1, z, LOOT_VAULT, f)
        p.column(x - 1 if x < cx else x + 1, z, 1, 3, AMETHYST, None)
        p.column(x, z - 1 if z < cz else z + 1, 1, 3, AMETHYST, None)
    p.chest(cx + 9, 1, cz - 7, LOOT_TRIAL, "west")
    p.chest(cx + 9, 1, cz + 9, LOOT_TRIAL, "west")
    for x, z in ((cx - 8, 5), (cx + 8, 5), (cx - 8, s - 6), (cx + 8, s - 6)):
        p.column(x, z, 1, h - 2)
        p.feature.discard((x, 9, z))
        p.put(x, 9, z, LANTERN)
    stairwell(p)
    up_jigsaw(p, POOL % "crown")
    return p


def build_crown():
    """labyrinth/crown/spire_top, 15x16x15: the open-air pinnacle. Only a `nusmp:down` jigsaw: nothing goes above it."""
    s, h = 15, HEIGHTS["crown"]
    p = Piece("labyrinth/crown/spire_top", s, h, s)
    cx, cz = p.cx, p.cz
    p.box((0, 0, 0), (s - 1, 0, s - 1), TILES)
    for x in range(s):
        for z in range(s):
            if x in (0, s - 1) or z in (0, s - 1):
                p.shell(x, 1, z, FENCE)
            elif (x + z) % 4 == 0 and max(abs(x - cx), abs(z - cz)) > 1:
                p.shell(x, 0, z, GLYPH)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.put(cx + dx, 0, cz + dz, AIR)
    p.feature.discard((cx, 0, cz))
    p.jigsaw(cx, 0, cz, "down_north", DOWN, UP, POOL % "caps", AIR, "rollable", SPINE_PRIORITY)
    for x, z in ((3, 3), (11, 3), (3, 11), (11, 11)):
        p.column(x, z, 1, 7)
        p.put(x, 8, z, AMETHYST)
    for i in range(3, 12):  # the ring and the cross beams at y=8 carry the spire over the stair mouth
        for a_, b_ in ((i, 3), (i, 11), (3, i), (11, i), (i, cz), (cx, i)):
            if (a_, 8, b_) not in p.feature:
                p.put(a_, 8, b_, BRICKS)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                p.put(cx + dx, 9, cz + dz, GOLD)
    p.column(cx, cz, 9, h - 2, GLYPH)
    p.put(cx, h - 1, cz, SEA)
    for x, z in ((3, 3), (11, 3), (3, 11), (11, 11)):
        p.put(x, 9, z, LANTERN)
    return p


def side_shell(p, n_glyph=4, n_lantern=3):
    shell_room(p, pilaster_every=4)
    wall_accents(p, n_glyph, n_lantern, 3)


def build_trial(variant):
    """side/trial_a and side/trial_b, 17x12x17: a trial chamber. The Sealed Archway keystone sits in the CEILING CENTRE with radius 8 /
    height 11, so the box it watches (x/z +-8, y 0..13) is exactly this template's footprint: the approach corridor outside the doorway
    is never 'inside', the doorway runes on the box edge are. Four spawn glyphs, one trial chest."""
    s, h = 17, 12
    p = Piece("labyrinth/side/trial_%s" % variant, s, h, s)
    cx, cz = p.cx, p.cz
    side_shell(p, 5, 3)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps", runes=True)
    mobs = [("minecraft:zombie", 3), ("minecraft:skeleton", 2)] if variant == "a" else [("nusmp:living_armor", 2), ("nusmp:dungeon_direwolf", 1)]
    p.be(cx, h - 1, cz, ARCHWAY, archway_nbt(8, h - 1, mobs, 60))
    for x, z in ((4, 4), (12, 4), (4, 12), (12, 12)):
        p.put(x, 0, z, SPAWN_GLYPH)
    for x, z in ((2, 2), (14, 2), (2, 14), (14, 14)):
        p.column(x, z, 1, h - 2)
        p.feature.discard((x, 7, z))
        p.put(x, 7, z, LANTERN)
    if variant == "a":
        floor_ring(p, cx, cz, 3, GLYPH, every=2)
        p.chest(cx, 1, s - 2, LOOT_TRIAL, "north")
    else:
        p.column(cx, cz, 1, h - 2)
        p.feature.discard((cx, 5, cz))
        p.put(cx, 5, cz, GLYPH)
        floor_ring(p, cx, cz, 2, GOLD, every=2)
        p.chest(s - 2, 1, cz, LOOT_TRIAL, "west")
    return p


def build_treasury_small():
    s, h = 11, 8
    p = Piece("labyrinth/side/treasury_small", s, h, s)
    cx = p.cx
    side_shell(p, 3, 2)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps")
    p.chest(cx - 2, 1, s - 2, LOOT_TRIAL, "north")
    p.chest(cx + 2, 1, s - 2, LOOT_TRIAL, "north")
    for x in range(2, s - 2):
        p.shell(x, 0, p.cz, GOLD if x % 2 == 0 else GLYPH)
    p.put(cx, h - 1, p.cz, SEA)
    return p


def build_corridor(name, length, far_name, far_target, far_pool):
    """A 7x7 corridor: the entrance is a side_in doorway, the far wall carries the next jigsaw (side_out -> side_end, or wild_out ->
    wildcard)."""
    p = Piece("labyrinth/side/" + name, 7, 7, length)
    side_shell(p, 2, 2)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps")
    doorway(p, "south", name=far_name, target=far_target, pool=far_pool)
    for z in range(3, length - 3, 3):
        p.shell(p.cx, 6, z, GLYPH)
    return p


def build_balcony():
    """side/balcony 9x7x7: an open-air balcony: no walls, no roof, a rail, two lantern posts."""
    s, h, d = 9, 7, 7
    p = Piece("labyrinth/side/balcony", s, h, d)
    p.box((0, 0, 0), (s - 1, 0, d - 1), TILES)
    for x in range(s):
        for z in range(d):
            if x in (0, s - 1) or z == d - 1:
                p.shell(x, 1, z, FENCE)
    for x in (0, s - 1):
        p.column(x, d - 1, 1, 2)
        p.feature.discard((x, 1, d - 1))
        p.put(x, 3, d - 1, LANTERN)
    p.shell(p.cx, 0, p.cz, GLYPH)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps")
    return p


def build_cap(name, jig_name, jig_target, kind):
    """3 wide x 4 tall x 1 deep plugs that sit just outside a doorway (the jigsaw block itself becomes bricks)."""
    p = Piece("labyrinth/" + name, 3, 4, 1)
    p.box((0, 0, 0), (2, 3, 0), BRICKS)
    if kind == "glyph":
        p.put(1, 2, 0, GLYPH)
        p.put(1, 3, 0, GOLD)
    elif kind == "amethyst":
        p.put(1, 2, 0, AMETHYST)
        p.put(1, 3, 0, GLYPH)
    elif kind == "window":
        p.put(1, 1, 0, GLASS)
        p.put(1, 2, 0, GLASS)
    p.jigsaw(1, 0, 0, "north_up", jig_name, jig_target, POOL % "caps", BRICKS, "aligned")
    return p


def build_roof_plug():
    """caps/roof_plug 5x4x5: the vertical cap. If a chain pool cannot place its level (no room left in the structure box), the up
    jigsaw's fallback puts this little sealed shrine over the landing instead of leaving a stair into the sky."""
    p = Piece("labyrinth/caps/roof_plug", 5, 4, 5)
    p.box((0, 0, 0), (4, 0, 4), TILES)
    p.box((0, 1, 0), (4, 2, 4), BRICKS)
    p.box((1, 1, 1), (3, 2, 3), AIR, shell=False)
    p.box((0, 3, 0), (4, 3, 4), BRICKS)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            p.feature.discard((2 + dx, 1, 2 + dz))
            p.put(2 + dx, 0, 2 + dz, AIR)
    p.feature.discard((2, 0, 2))
    p.jigsaw(2, 0, 2, "down_north", DOWN, UP, POOL % "caps", AIR, "rollable")
    p.feature.discard((2, 3, 2))
    p.put(2, 3, 2, SEA)
    p.feature.discard((2, 2, 0))
    p.put(2, 2, 0, GLYPH)
    return p


def build_example_chapel():
    """labyrinth_example:wild/chapel 9x8x9: the example datapack's room; its entrance jigsaw is `nusmp:wild_in`."""
    p = Piece("wild/chapel", 9, 8, 9, seed="example-chapel")
    side_shell(p, 3, 2)
    doorway(p, "north", name=WILD_IN, target=WILD_OUT, pool=POOL % "caps")
    p.column(2, 6, 1, 3, GOLD, None)
    p.column(6, 6, 1, 3, GOLD, None)
    p.put(p.cx, 1, 6, AMETHYST)
    p.put(p.cx, 2, 6, CLUSTER, {"facing": "up", "waterlogged": "false"})
    p.put(p.cx, 7, p.cz, SEA)
    return p


# =====================================================================================================================
# 1.36.0: the D&D trap rooms (docs/labyrinth/f1_traps.md): three side rooms that drain the party's resources
# =====================================================================================================================

#: Row of the pit room's doorway jigsaw and floor stand: the floor of every older side room is row 0 and its jigsaw row 1; the pit room's
#: floor is sunk three rows so the pit has somewhere to be, which puts its jigsaw (the same height as the parent's) at row 4.
ENTRY_Y = {"labyrinth/side/pit_room": 4}

PIT_SIZE = (13, 12, 22)
PIT_FLOOR_Y = 3                      # the pitfall layer; feet stand at 4
PIT_FIELD_Z = (6, 15)                # rows of the pit field; rows 4-5 are the landing, 16-20 the treasure alcove
PIT_FIELD_X = (1, 11)
PIT_LADDER = (1, 5)                  # (x, z) of the escape ladder, in the west wall of the landing


def pit_route(rng, lo_x=1, hi_x=11, z0=6, z1=15):
    """The safe route over the pit field: a list of (x, z) cells, one wide, 4-connected, from row z0 to row z1. Deterministic for the rng.
    In every row the route runs sideways and then drops one row; a sideways run continues the direction of the run above (or goes straight
    down), never back over it, so two rows of the route are never beside each other: the route is a thin snake with pits all around, never
    a wide band a player could cross by luck."""
    x = rng.randint(lo_x + 2, hi_x - 2)
    cells = []
    direction = 0
    for z in range(z0, z1 + 1):
        if direction == 0:
            want = rng.choice((-1, 1))
        else:
            want = direction
        room = (hi_x - x) if want > 0 else (x - lo_x)
        if z == z1 or room == 0 or rng.random() < 0.18:
            run, d = 0, 0                      # straight down (always on the last row: it is just the cell)
        else:
            run, d = rng.randint(1, min(4, room)), want
        for k in range(run + 1):
            cells.append((x + d * k, z))
        x += d * run
        direction = d
    return cells


def build_pit_room():
    """side/pit_room 13x12x22 (entrance row 4): a corridor, a landing, a pit field and a treasure alcove behind it. The whole floor (row 3) is
    `nusmp:pitfall_tile`, `safe=true` on the landing, the alcove and ONE fixed one-wide route over the field, `safe=false` everywhere else
    on it: both look the same. Below the field a dark chamber (rows 1-2) with pointed dripstone on a lattice of its floor, and a ladder
    in the landing's west wall to climb back out. Sinking the floor three rows is what gives the pit room."""
    sx, sy, sz = PIT_SIZE
    p = Piece("labyrinth/side/pit_room", sx, sy, sz)
    cx = p.cx
    fy = PIT_FLOOR_Y
    # shell: a solid block of brick up to the floor row, walls and roof
    p.box((0, 0, 0), (sx - 1, fy - 1, sz - 1), BRICKS)
    for y in range(fy, sy - 1):
        for x in range(sx):
            p.shell(x, y, 0, BRICKS)
            p.shell(x, y, sz - 1, BRICKS)
        for z in range(sz):
            p.shell(0, y, z, BRICKS)
            p.shell(sx - 1, y, z, BRICKS)
    p.box((0, sy - 1, 0), (sx - 1, sy - 1, sz - 1), BRICKS)
    # the corridor: rows 1-3 are 3 wide, the rest of those rows is wall
    for z in range(1, 4):
        for x in range(1, sx - 1):
            if abs(x - cx) > 1:
                for y in range(fy, sy - 1):
                    p.shell(x, y, z, BRICKS)
    route = set(pit_route(p.rng))
    x0, x1 = PIT_FIELD_X
    z0, z1 = PIT_FIELD_Z
    # the floor row: every cell the player may stand on is a pitfall tile (safe or not)
    for z in range(1, sz - 1):
        for x in range(1, sx - 1):
            if z <= 3 and abs(x - cx) > 1:
                continue
            on_field = x0 <= x <= x1 and z0 <= z <= z1
            safe = (not on_field) or (x, z) in route
            if (x, z) == PIT_LADDER:
                continue
            p.shell(x, fy, z, PITFALL, {"safe": "true" if safe else "false", "open": "false"})
    # the chamber under the field (rows 1-2), carved out of the brick block, plus the ladder shaft in the landing
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            for y in (1, 2):
                p.shell(x, y, z, AIR)
    lx, lz = PIT_LADDER
    for y in (1, 2):
        p.shell(lx, y, lz, AIR)
    for y in (1, 2, 3):
        p.put(lx, y, lz, LADDER, {"facing": "east", "waterlogged": "false"})
    # spikes on a lattice (even x, even z) of the chamber floor, never near the foot of the ladder: a lattice leaves whole rows and columns free,
    # so every free cell stays connected to the ladder (a checkerboard would wall in the cells between its spikes)
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            if x % 2 == 0 and z % 2 == 0 and not (x <= lx + 3 and z <= lz + 3) and p.rng.random() < 0.85:
                p.put(x, 1, z, SPIKE, {"thickness": "tip", "vertical_direction": "up", "waterlogged": "false"})
    # the doorway at the sunk row, light in the walls above the floor, the treasure
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps", y0=ENTRY_Y["labyrinth/side/pit_room"])
    wall_accents(p, 3, 4, y_lo=5)
    p.chest(cx, fy + 1, sz - 2, LOOT_TRIAL, "north")
    return p


def build_dark_room():
    """side/dark_room 15x10x15: a square room with a snuffer glyph in the middle of the floor and no light of its own, and a chest of
    rations five blocks from the glyph (inside its darkness). The glyph is ringed by a circle of polished blackstone."""
    s, h = 15, 10
    p = Piece("labyrinth/side/dark_room", s, h, s)
    cx, cz = p.cx, p.cz
    shell_room(p, pilaster_every=4)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if (dx, dz) != (0, 0):
                p.shell(cx + dx, 0, cz + dz, "minecraft:polished_blackstone")
    p.put(cx, 0, cz, SNUFFER, {"active": "false"})
    for x, z in ((3, 3), (11, 3), (3, 11), (11, 11)):
        p.column(x, z, 1, h - 2)
    p.chest(cx, 1, cz + 5, LOOT_TRIAL, "north")
    return p


def build_puzzle_room():
    """side/puzzle_room 19x11x20: four sigil plates in a row, a riddle stone beside the entrance, and behind a wall a treasure alcove whose
    only way in is a `kind=puzzle` Gatekeeper door (2 wide, 3 tall). The alcove holds a trial chest and ONE relic pedestal. The riddle's
    sequence and the plates' colours are shuffled with the piece's seeded rng, so the room is reproducible."""
    sx, sy, sz = 19, 11, 20
    p = Piece("labyrinth/side/puzzle_room", sx, sy, sz)
    cx = p.cx
    shell_room(p, pilaster_every=4)
    doorway(p, "north", name=SIDE_IN, target=SIDE_OUT, pool=POOL % "caps")
    colours = [0, 1, 2, 3]
    p.rng.shuffle(colours)
    for x, c in zip((cx - 3, cx - 1, cx + 1, cx + 3), colours):
        p.put(x, 1, 6, PLATE, {"color": c, "lit": "false"})
    seq = [0, 1, 2, 3]
    p.rng.shuffle(seq)
    p.be(3, 1, 2, RIDDLE, {"id": RIDDLE, "sequence": nbt.List([nbt.Int(c) for c in seq], tag_type=nbt.TAG_INT)})
    p.put(3, 2, 2, LANTERN)
    # the partition and its door
    door_cells = [(cx - 1, 1, 13, 3), (cx, 1, 13, 3)]
    gatekeeper_door(p, door_cells, "puzzle")
    for x in range(1, sx - 1):
        for y in range(1, sy - 1):
            if (x, y, 13) not in p.feature:
                p.shell(x, y, 13, BRICKS)
    for x in (cx - 2, cx + 1):
        for y in range(1, 4):
            p.shell(x, y, 13, PILLAR, {"axis": "y"})
    for x in (cx - 1, cx):
        p.shell(x, 4, 13, GOLD)
    # the alcove behind it
    p.chest(cx - 3, 1, 17, LOOT_TRIAL, "north")
    p.be(cx, 1, 17, PEDESTAL, pedestal_nbt())
    p.put(cx + 3, 3, 17, LANTERN)
    for x in range(cx - 2, cx + 3):
        p.shell(x, 0, 15, GOLD if x % 2 == 0 else GLYPH)
    return p


def pit_problems(p):
    """--check of the pit room (1.36.0): the route over the field exists and is the ONLY way to the treasure, the field is mostly pits,
    the chamber's free cells all reach the ladder, the ladder reaches the landing."""
    t = p.t
    sx, sy, sz = t.size
    fy = PIT_FLOOR_Y
    name = t.name
    probs = []
    x0, x1 = PIT_FIELD_X
    z0, z1 = PIT_FIELD_Z

    def tile(x, z):
        c = t.get(x, fy, z)
        return c if c is not None and c.block == PITFALL else None

    # 1. a safe path from the doorway to a cell beside the chest, over safe tiles only
    chest = [pos for pos, c in t.cells.items() if c.block == "minecraft:chest"]
    if len(chest) != 1:
        return probs + ["%s: expected exactly one chest" % name]
    cxx, _, czz = chest[0]
    start = (p.cx, 1)
    seen = {start}
    todo = [start]
    while todo:
        x, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            c = tile(*n)
            if c is not None and c.props.get("safe") == "true" and n not in seen:
                seen.add(n)
                todo.append(n)
    if (cxx, czz) not in seen:
        probs.append("%s: no route of safe tiles from the doorway to the chest" % name)
    # 2. the field is mostly traps and the route is thin
    field = [(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)]
    unsafe = [c for c in field if tile(*c) is not None and tile(*c).props.get("safe") == "false"]
    if len(unsafe) < 0.55 * len(field):
        probs.append("%s: only %d of %d field tiles are pits" % (name, len(unsafe), len(field)))
    for c in field:
        if tile(*c) is None:
            probs.append("%s: field cell %s is not a pitfall tile" % (name, c))
    # 3. the route is the only crossing: with every pit removed, no other way over (the walls span the field's full width)
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            for xx in (x0 - 1, x1 + 1):
                c = t.get(xx, fy + 1, z)
                if c is None or c.block == AIR:
                    probs.append("%s: the field row %d is open at x=%d: a way round the pits" % (name, z, xx))
    # 4. below: the chamber is dark, carved, spiked, and its free cells reach the ladder
    lx, lz = PIT_LADDER
    free = set()
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            for y in (1, 2):
                c = t.get(x, y, z)
                if c is None or c.block not in (AIR, SPIKE):
                    probs.append("%s: chamber cell (%d,%d,%d) is %s" % (name, x, y, z, c.block if c else None))
            c = t.get(x, 1, z)
            if c is not None and c.block == AIR:
                free.add((x, z))
    spikes = sum(1 for z in range(z0, z1 + 1) for x in range(x0, x1 + 1) if t.get(x, 1, z) is not None and t.get(x, 1, z).block == SPIKE)
    if spikes < 10:
        probs.append("%s: only %d spikes" % (name, spikes))
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            if t.get(x, 1, z) is not None and t.get(x, 1, z).block == SPIKE:
                below = t.get(x, 0, z)
                if below is None or below.block == AIR:
                    probs.append("%s: spike at (%d,%d) has no floor" % (name, x, z))
    start = (lx, lz + 1)
    if start not in free:
        probs.append("%s: the foot of the ladder is not free" % name)
    else:
        seen = {start}
        todo = [start]
        while todo:
            x, z = todo.pop()
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = (x + dx, z + dz)
                if n in free and n not in seen:
                    seen.add(n)
                    todo.append(n)
        lost = free - seen
        if lost:
            probs.append("%s: %d free chamber cells cannot reach the ladder, e.g. %s" % (name, len(lost), sorted(lost)[0]))
    for y in (1, 2, 3):
        c = t.get(lx, y, lz)
        if c is None or c.block != LADDER or c.props.get("facing") != "east":
            probs.append("%s: ladder missing at (%d,%d,%d)" % (name, lx, y, lz))
    wall = t.get(lx - 1, 2, lz)
    if wall is None or wall.block != BRICKS:
        probs.append("%s: the ladder has no wall behind it" % name)
    top = tile(lx + 1, lz)
    if top is None or top.props.get("safe") != "true":
        probs.append("%s: the ladder's top must open onto a safe tile" % name)
    # 5. the sunk floor: nothing but the pitfall layer, the ladder and the airspace above it
    for z in range(1, sz - 1):
        for x in range(1, sx - 1):
            c = t.get(x, fy, z)
            if c is not None and c.block == BRICKS and not (z <= 3 and abs(x - p.cx) > 1):
                probs.append("%s: brick at floor row (%d,%d)" % (name, x, z))
    return probs


def puzzle_problems(p):
    """--check of the puzzle room: four plates of four different colours, one riddle stone whose sequence is valid, the puzzle door is the
    only way to the alcove (with the door treated as a wall the chest and the pedestal cannot be reached), and with it open they can."""
    t = p.t
    name = t.name
    probs = []
    plates = [(pos, c) for pos, c in t.cells.items() if c.block == PLATE]
    if sorted(c.props.get("color") for _, c in plates) != ["0", "1", "2", "3"]:
        probs.append("%s: the plates must be one of each colour (found %s)" % (name, [c.props.get("color") for _, c in plates]))
    stones = [(pos, c) for pos, c in t.cells.items() if c.block == RIDDLE]
    if len(stones) != 1:
        probs.append("%s: expected one riddle stone" % name)
    else:
        seq = [int(v) for v in stones[0][1].nbt["sequence"]]
        if not (1 <= len(seq) <= 8 and all(0 <= v <= 3 for v in seq)):
            probs.append("%s: bad riddle sequence %s" % (name, seq))
    doors = [pos for pos, c in t.cells.items() if c.block == DOOR]
    if not doors or any(t.get(*pos).props.get("kind") != "puzzle" for pos in doors):
        probs.append("%s: the doors must all be kind=puzzle" % name)
    peds = [pos for pos, c in t.cells.items() if c.block == PEDESTAL]
    if len(peds) != 1:
        probs.append("%s: exactly one relic pedestal expected, found %d" % (name, len(peds)))
    sx, sy, sz = t.size
    passable = (AIR, "minecraft:structure_void", PLATE)

    def solid(x, y, z):
        if y < 0:
            return True
        c = t.get(x, y, z)
        return not (c is None or c.block in passable)

    entry = [pos for pos, o, d in t.jigsaws() if str(d["name"]) == SIDE_IN][0]
    seen = _flood(solid, (entry[0], 1, entry[2]), (-1, 0, -1), (sx, sy + 1, sz))
    for pos, c in t.cells.items():
        if c.block in ("minecraft:chest", PEDESTAL):
            beside = any((pos[0] + dx, pos[1] + dy, pos[2] + dz) in seen for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)))
            if beside:
                probs.append("%s: the %s at %s is reachable with the puzzle door shut" % (name, c.block, pos))
    for pos, c in plates + stones:
        beside = any((pos[0] + dx, pos[1] + dy, pos[2] + dz) in seen for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0))) or pos in seen
        if not beside:
            probs.append("%s: %s at %s cannot be reached from the entrance" % (name, c.block, pos))
    return probs


def dark_problems(p):
    """--check of the dark room: one snuffer glyph in the centre column, no light-giving block anywhere, the ration chest inside the glyph's
    darkness radius (6)."""
    t = p.t
    name = t.name
    probs = []
    glyphs = [pos for pos, c in t.cells.items() if c.block == SNUFFER]
    if glyphs != [(p.cx, 0, p.cz)]:
        probs.append("%s: the snuffer glyph must be the one block at the centre of the floor (found %s)" % (name, glyphs))
    for pos, c in t.cells.items():
        if c.block in (LANTERN, GLYPH, SEA, "minecraft:glowstone", "minecraft:shroomlight", "minecraft:torch"):
            probs.append("%s: a light source (%s) at %s defeats the darkness" % (name, c.block, pos))
    for pos, c in t.cells.items():
        if c.block == "minecraft:chest":
            d = ((pos[0] - p.cx) ** 2 + (pos[1] - 0) ** 2 + (pos[2] - p.cz) ** 2) ** 0.5
            if d > 6.0:
                probs.append("%s: the chest at %s is %.1f from the glyph, outside its darkness" % (name, pos, d))
    return probs


TRAP_CHECKS = {"labyrinth/side/pit_room": pit_problems, "labyrinth/side/puzzle_room": puzzle_problems, "labyrinth/side/dark_room": dark_problems}


# =====================================================================================================================
# the set
# =====================================================================================================================

LEVEL_KINDS = (("hall", 4), ("gallery", 3), ("shrine", 2))
ASCENT = {"ascent_1": "ascent_2", "ascent_2": "warden", "ascent_3": "ascent_4", "ascent_4": "peak"}
SIDE_ELEMENTS = [("side/trial_a", 4), ("side/trial_b", 3), ("side/treasury_small", 3), ("side/corridor", 3), ("side/wild_adapter", 2), ("side/balcony", 2),
                 ("side/pit_room", 2), ("side/dark_room", 2), ("side/puzzle_room", 2)]   # 1.36.0, appended: the trap rooms


def build_all():
    """name -> Piece for every spire template (the example chapel is separate)."""
    pieces = [build_base()]
    for n in range(1, 5):
        for kind, _ in LEVEL_KINDS:
            pieces.append(level_piece(kind, n, POOL % ASCENT["ascent_%d" % n]))
    pieces += [build_warden(), build_peak(), build_vault(), build_crown(),
               build_trial("a"), build_trial("b"), build_treasury_small(),
               build_corridor("corridor", 13, SIDE_OUT, SIDE_IN, POOL % "side_end"),
               build_corridor("wild_adapter", 9, WILD_OUT, WILD_IN, POOL % "wildcard"),
               build_balcony(),
               build_pit_room(), build_dark_room(), build_puzzle_room(),
               build_cap(SEALED_ARCH_SIDE, SIDE_IN, SIDE_OUT, "glyph"),
               build_cap("caps/window", SIDE_IN, SIDE_OUT, "window"),
               build_cap("caps/wild_sealed_arch", WILD_IN, WILD_OUT, "amethyst"),
               build_roof_plug()]
    return {p.t.name: p for p in pieces}


def element(location, weight, processors=PROCESSORS):
    return {"weight": weight, "element": {"element_type": "minecraft:single_pool_element", "location": "nusmp:" + location,
                                          "processors": processors, "projection": "rigid"}}


def pools():
    """pool name -> JSON object."""
    out = {"base": {"fallback": "minecraft:empty", "elements": [element("labyrinth/base", 1)]}}
    for n in range(1, 5):
        out["ascent_%d" % n] = {"fallback": POOL % "caps",
                                "elements": [element("labyrinth/ascent_%d/%s_%d" % (n, k, n), w) for k, w in LEVEL_KINDS]}
    out["warden"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/warden/hall", 1)]}
    out["peak"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/peak/boss_peak", 1)]}
    out["vault"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/vault/treasury", 1)]}
    out["crown"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/crown/spire_top", 1)]}
    out["side"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/" + n, w) for n, w in SIDE_ELEMENTS]}
    out["side_end"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/" + n, w) for n, w in SIDE_ELEMENTS if n != "side/corridor"]}
    out["wildcard"] = {"fallback": POOL % "caps", "elements": [element("labyrinth/caps/wild_sealed_arch", 1)]}
    out["caps"] = {"fallback": "minecraft:empty", "elements": [element("labyrinth/caps/sealed_arch", 3), element("labyrinth/caps/window", 2),
                                                               element("labyrinth/caps/wild_sealed_arch", 1), element("labyrinth/caps/roof_plug", 1)]}
    return out


PROCESSOR_LIST = {"processors": [{"processor_type": "nusmp:biome_palette"}, {"processor_type": "nusmp:magicule_veins"}]}

EXAMPLE_POOL = {"fallback": POOL % "caps",
                "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "labyrinth_example:wild/chapel",
                                                        "processors": "minecraft:empty", "projection": "rigid"}}]}
EXAMPLE_MCMETA = {"pack": {"pack_format": 48, "description": "Labyrinth wildcard example: one chapel snapped into the spire's wildcard pool"}}


def json_bytes(obj):
    return (json.dumps(obj, indent=2) + "\n").encode("utf-8")


def example_files():
    """relative path -> bytes of the example datapack."""
    chapel = build_example_chapel()
    return {
        "pack.mcmeta": json_bytes(EXAMPLE_MCMETA),
        "data/nusmp/worldgen/template_pool/labyrinth/wildcard.json": json_bytes(EXAMPLE_POOL),
        "data/labyrinth_example/structure/wild/chapel.nbt": nbt.dumps(chapel.t.to_nbt(), compress=True),
    }


def write_bytes(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    if os.path.exists(path):
        with open(path, "rb") as f:
            if f.read() == data:
                return False
    with open(path, "wb") as f:
        f.write(data)
    return True


def example_zip_bytes(files):
    buf = io.BytesIO()
    with zipfile.ZipFile(buf, "w", zipfile.ZIP_DEFLATED) as z:
        for rel in sorted(files):
            info = zipfile.ZipInfo(rel, date_time=(2026, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            z.writestr(info, files[rel])
    return buf.getvalue()


def generate():
    pieces = build_all()
    changed = 0
    for p in pieces.values():
        changed += write_bytes(p.t.path(RES), nbt.dumps(p.t.to_nbt(), compress=True))
    for name, obj in pools().items():
        changed += write_bytes(os.path.join(POOL_DIR, name + ".json"), json_bytes(obj))
    changed += write_bytes(PROC_LIST, json_bytes(PROCESSOR_LIST))
    files = example_files()
    for rel, data in files.items():
        changed += write_bytes(os.path.join(EXAMPLE_DIR, *rel.split("/")), data)
    changed += write_bytes(EXAMPLE_ZIP, example_zip_bytes(files))
    print("%d template(s), %d pool(s), %d file(s) written" % (len(pieces), len(pools()), changed))


# =====================================================================================================================
# --check
# =====================================================================================================================

# =====================================================================================================================
# --check: can a player actually walk it?
# =====================================================================================================================

#: blocks a player walks through: nothing, the structure void, plants, and the Gatekeeper doors (the walk test treats them as OPENED,
#: because the point is that the stair behind them is climbable once the boss is dead).
_PASSABLE = (AIR, "minecraft:structure_void", DOOR, PLATE)


def walkable_report(p):
    """Flood-fill the piece as a player moves (1x2 body, step up 1, drop up to 3) from where he enters, and return the problems:
    the stair must reach the landing, every side doorway and every chest/pedestal/altar must be reachable. Doors count as open."""
    t = p.t
    sx, sy, sz = t.size
    cx, cz = p.cx, p.cz
    name = t.name

    def solid(x, y, z):
        if y < 0:
            return True
        c = t.get(x, y, z)
        if c is None or c.block in _PASSABLE:
            return False
        if c.block == "minecraft:jigsaw":
            return str(c.nbt["final_state"]) != AIR
        return True

    def free(x, y, z):  # a cell a body can occupy: inside the template footprint (walls are cells) or open sky above/around
        return not solid(x, y, z)

    jig = {pos: (o, d) for pos, o, d in t.jigsaws()}
    if name == "labyrinth/base":
        start = (cx, 5, cz)
    elif name.startswith("labyrinth/side/"):
        entry = [pos for pos, (o, d) in jig.items() if str(d["name"]) == SIDE_IN]
        start = (entry[0][0], ENTRY_Y.get(name, 1), entry[0][2])
    elif name.startswith("labyrinth/caps/"):
        return []
    else:
        start = (cx, 0, cz)
    seen = {start}
    todo = [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (-1 <= nx <= sx and -1 <= nz <= sz):
                continue
            for dy in (1, 0, -1, -2, -3):
                ny = y + dy
                if ny < 0 or ny > sy + 1:
                    continue
                if not (free(nx, ny, nz) and free(nx, ny + 1, nz) and solid(nx, ny - 1, nz)):
                    continue
                if dy == 1 and not free(x, y + 2, z):
                    continue          # no room to jump
                if dy < 0 and not (free(nx, y, nz) and free(nx, y + 1, nz)):
                    continue          # the walk-off column must be open
                if (nx, ny, nz) not in seen:
                    seen.add((nx, ny, nz))
                    todo.append((nx, ny, nz))
    problems = []

    def near(x, y, z):
        return any((x + dx, y + dy, z + dz) in seen for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (0, 0)) for dy in (0, -1, 1))

    if name == "labyrinth/base":
        if (cx, sy, cz) not in seen:
            problems.append("%s: the stairways do not reach the landing under the up jigsaw" % name)
    elif not name.startswith("labyrinth/side/") and name != "labyrinth/crown/spire_top":
        if (cx - 1, sy, cz) not in seen:
            problems.append("%s: the stair does not reach the landing under the up jigsaw" % name)
    for (x, y, z), (o, d) in jig.items():
        if str(d["name"]) in (SIDE_OUT, WILD_OUT) and not near(x, 1, z):
            problems.append("%s: the doorway at (%d,%d,%d) cannot be reached from the stair" % (name, x, y, z))
    for (x, y, z), c in t.cells.items():
        if c.block in ("minecraft:chest", PEDESTAL, ALTAR, PLATE, RIDDLE) and not near(x, y, z):
            problems.append("%s: %s at (%d,%d,%d) cannot be reached" % (name, c.block, x, y, z))
    return problems


def _flood(solid, start, lo, hi):
    """Cells (feet positions) a player can reach from `start` in a world where solid(x,y,z) says whether a cell blocks. A body is
    1x2, steps up 1 (a jump: three free cells above the take-off cell), drops up to 3."""
    free = lambda x, y, z: not solid(x, y, z)  # noqa: E731
    seen = {start}
    todo = [start]
    while todo:
        x, y, z = todo.pop()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (lo[0] <= nx <= hi[0] and lo[2] <= nz <= hi[2]):
                continue
            for dy in (1, 0, -1, -2, -3):
                ny = y + dy
                if ny < lo[1] or ny > hi[1]:
                    continue
                if not (free(nx, ny, nz) and free(nx, ny + 1, nz) and solid(nx, ny - 1, nz)):
                    continue
                if dy == 1 and not free(x, y + 2, z):
                    continue
                if dy < 0 and not (free(nx, y, nz) and free(nx, y + 1, nz)):
                    continue
                if (nx, ny, nz) not in seen:
                    seen.add((nx, ny, nz))
                    todo.append((nx, ny, nz))
    return seen


def _rotate(dx, dz, quarter):
    for _ in range(quarter % 4):
        dx, dz = -dz, dx
    return dx, dz


def stack_walk(pieces, names, rotations):
    """Stack the named pieces on the axis the way Jigsaw does (the next piece's bottom row directly above the previous piece's top row,
    centres aligned, each turned by its own quarter turn) and flood-fill from the base pad: the player must be able to walk from the
    arrival pad to the top of the crown. Returns problems."""
    world = {}
    y0 = 0
    tops = []
    for name, rot in zip(names, rotations):
        p = pieces[name]
        t = p.t
        for (x, y, z), c in t.cells.items():
            dx, dz = _rotate(x - p.cx, z - p.cz, rot)
            block = c.block
            if block == "minecraft:jigsaw":
                fs = str(c.nbt["final_state"]).split("[")[0]
                block = fs
            if block in _PASSABLE:
                world.pop((dx, y0 + y, dz), None)
                continue
            world[(dx, y0 + y, dz)] = block
        tops.append(y0 + t.size[1])
        y0 += t.size[1]
    solid = lambda x, y, z: (x, y, z) in world  # noqa: E731
    cxb = 0
    start = (0, 5, 0)
    lo, hi = (-24, 0, -24), (24, y0 + 3, 24)
    seen = _flood(solid, start, lo, hi)
    problems = []
    crown_base = tops[-2]
    if not any(y == crown_base + 1 for (x, y, z) in seen):
        problems.append("stack %s rot %s: the arrival pad cannot walk to the crown floor (highest reachable feet y=%d, crown floor at %d)" % (
            "/".join(n.split("/")[-1] for n in names), rotations, max(y for (x, y, z) in seen), crown_base + 1))
    return problems


def check():
    problems = []
    pieces = build_all()
    pool_objs = pools()
    pool_names = {POOL % n for n in pool_objs} | {"minecraft:empty"}
    # 1. files on disk are what the script writes (stale templates are the classic silent failure)
    for p in pieces.values():
        path = p.t.path(RES)
        if not os.path.exists(path):
            problems.append("missing template file %s (run gen_spire.py)" % path)
            continue
        with open(path, "rb") as f:
            if f.read() != nbt.dumps(p.t.to_nbt(), compress=True):
                problems.append("stale template %s (run gen_spire.py)" % path)
    for name, obj in pool_objs.items():
        path = os.path.join(POOL_DIR, name + ".json")
        if not os.path.exists(path) or open(path, "rb").read() != json_bytes(obj):
            problems.append("missing or stale pool %s" % path)
    if not os.path.exists(PROC_LIST) or open(PROC_LIST, "rb").read() != json_bytes(PROCESSOR_LIST):
        problems.append("missing or stale %s" % PROC_LIST)
    files = example_files()
    for rel, data in files.items():
        path = os.path.join(EXAMPLE_DIR, *rel.split("/"))
        if not os.path.exists(path) or open(path, "rb").read() != data:
            problems.append("missing or stale example file %s" % path)
    if not os.path.exists(EXAMPLE_ZIP) or open(EXAMPLE_ZIP, "rb").read() != example_zip_bytes(files):
        problems.append("missing or stale %s" % EXAMPLE_ZIP)
    # 2. pools: every element has a template, every fallback is a pool, the chain is complete
    for name, obj in pool_objs.items():
        if obj["fallback"] not in pool_names:
            problems.append("pool %s: fallback %s is not a pool" % (name, obj["fallback"]))
        for e in obj["elements"]:
            loc = e["element"]["location"]
            if not loc.startswith("nusmp:") or loc[6:] not in pieces:
                problems.append("pool %s: element %s has no template" % (name, loc))
            if e["element"]["processors"] not in (PROCESSORS, "minecraft:empty"):
                problems.append("pool %s: element %s uses an unknown processor list" % (name, loc))
    for name in CHAIN:
        if name not in pool_objs or not pool_objs[name]["elements"]:
            problems.append("chain pool %s is missing or empty" % name)
    # 3. jigsaw geometry per piece
    budget = 0
    for name, p in pieces.items():
        t = p.t
        sx, sy, sz = t.size
        jigs = t.jigsaws()
        for (x, y, z), o, data in jigs:
            jn, pool, fin = str(data["name"]), str(data["pool"]), str(data["final_state"])
            if pool not in pool_names:
                problems.append("%s: jigsaw %s at (%d,%d,%d) names unknown pool %s" % (name, jn, x, y, z, pool))
            if jn == UP:
                if (x, y, z) != (p.cx, sy - 1, p.cz) or o != "up_north" or str(data["target"]) != DOWN or str(data["joint"]) != "rollable":
                    problems.append("%s: up jigsaw not at the top-row centre facing up (%s %s)" % (name, (x, y, z), o))
                want = TILES if name == "labyrinth/base" else AIR
                if fin != want:
                    problems.append("%s: up jigsaw final_state must be %s" % (name, want))
            elif jn == DOWN:
                if (x, y, z) != (p.cx, 0, p.cz) or o != "down_north" or str(data["target"]) != UP or str(data["joint"]) != "rollable":
                    problems.append("%s: down jigsaw not at the bottom-row centre facing down (%s %s)" % (name, (x, y, z), o))
            else:
                fx, fy, fz = facing(o)
                on_wall = (fx == 1 and x == sx - 1) or (fx == -1 and x == 0) or (fz == 1 and z == sz - 1) or (fz == -1 and z == 0)
                is_cap = name.startswith("labyrinth/caps/")
                if is_cap:  # a plug is one cell deep: its jigsaw is the bottom-centre cell of the plane, facing back toward the room
                    on_wall = (x, y, z) == (1, 0, 0) and (sx, sy, sz) == (3, 4, 1)
                ey = ENTRY_Y.get(name, 1)          # 1.36.0: the pit room's doorway is sunk (see ENTRY_Y)
                if (not on_wall or fy != 0 or y != ey) and not is_cap:
                    problems.append("%s: side jigsaw %s at (%d,%d,%d) is not on an outer wall at floor level facing out" % (name, jn, x, y, z))
                if is_cap and not on_wall:
                    problems.append("%s: cap jigsaw must be the bottom centre of its 3x4x1 plane" % name)
                pairs = {SIDE_OUT: SIDE_IN, SIDE_IN: SIDE_OUT, WILD_OUT: WILD_IN, WILD_IN: WILD_OUT}
                if pairs.get(jn) != str(data["target"]):
                    problems.append("%s: jigsaw %s targets %s" % (name, jn, data["target"]))
                if name.startswith("labyrinth/caps/"):
                    if fin != BRICKS:
                        problems.append("%s: a cap's jigsaw must become bricks" % name)
                else:  # the doorway must be open: 3 wide x 4 tall of air around the jigsaw
                    px, pz = (0, 1) if fx else (1, 0)
                    for d in (-1, 0, 1):
                        for yy in range(ey, ey + 4):
                            c = t.get(x + px * d, yy, z + pz * d)
                            if (d, yy) != (0, ey) and (c is None or c.block != AIR):
                                problems.append("%s: doorway of %s at (%d,%d,%d) is not open" % (name, jn, x + px * d, yy, z + pz * d))
        names = [str(d["name"]) for _, _, d in jigs]
        level_like = name.startswith("labyrinth/ascent_") or name.split("/")[1] in ("warden", "peak", "vault")
        if level_like or name == "labyrinth/base":
            if names.count(UP) != 1:
                problems.append("%s: needs exactly one up jigsaw" % name)
            if name != "labyrinth/base" and names.count(DOWN) != 1:
                problems.append("%s: needs exactly one down jigsaw" % name)
            if sx % 2 == 0 or sz % 2 == 0:
                problems.append("%s: an odd size keeps the centre on the axis" % name)
            if name.startswith("labyrinth/ascent_") and (sx > LEVEL_SIZE or sz > LEVEL_SIZE or sy > HEIGHTS["level"]):
                problems.append("%s: larger than %dx%dx%d" % (name, LEVEL_SIZE, HEIGHTS["level"], LEVEL_SIZE))
            # the 3x3 floor hole and the emergence headroom (the piece above may be in any rotation)
            if name != "labyrinth/base":
                for dx in (-1, 0, 1):
                    for dz in (-1, 0, 1):
                        for yy in range(0, 4):
                            c = t.get(p.cx + dx, yy, p.cz + dz)
                            if not (c is None or c.block in (AIR, "minecraft:jigsaw")):
                                problems.append("%s: (%d,%d,%d) must be air for players coming up the stair" % (name, p.cx + dx, yy, p.cz + dz))
        problems += walkable_report(p)
        if name in TRAP_CHECKS:
            problems += TRAP_CHECKS[name](p)
        if name == "labyrinth/base":
            c = t.get(p.cx, 4, p.cz)
            if c is None or c.block != TILES:
                problems.append("base: the arrival pad centre (%d,4,%d) must be tiles" % (p.cx, p.cz))
            for yy in range(5, 13):
                c = t.get(p.cx, yy, p.cz)
                if c is not None and c.block != AIR:
                    problems.append("base: the arrival column must be air up to y=12 (found %s at y=%d)" % (c.block, yy))
            c = t.get(p.cx + 3, 5, p.cz)
            if c is None or c.block != EXIT:
                problems.append("base: labyrinth_exit must stand at (centre+3, 5, centre)")
    chain = ["labyrinth/base", "labyrinth/ascent_1/hall_1", "labyrinth/ascent_2/gallery_2", "labyrinth/warden/hall", "labyrinth/ascent_3/shrine_3",
             "labyrinth/ascent_4/hall_4", "labyrinth/peak/boss_peak", "labyrinth/vault/treasury", "labyrinth/crown/spire_top"]
    for rots in ((0,) * 9, (0, 1, 2, 3, 0, 1, 2, 3, 0), (0, 3, 2, 1, 3, 2, 1, 0, 2), (0, 2, 2, 2, 2, 2, 2, 2, 2)):
        problems += stack_walk(pieces, chain, rots)
    for key in ("base", "warden", "peak", "vault", "crown"):
        budget += HEIGHTS[key]
    budget += 4 * HEIGHTS["level"]
    if budget > MAX_DISTANCE_FROM_CENTER:
        problems.append("height budget %d exceeds max_distance_from_center %d" % (budget, MAX_DISTANCE_FROM_CENTER))
    # 4. no stray templates: everything under structure/labyrinth is generated here
    import glob
    on_disk = {os.path.relpath(f, os.path.join(RES, "data", "nusmp", "structure")).replace("\\", "/")[:-4]
               for f in glob.glob(os.path.join(RES, "data", "nusmp", "structure", "labyrinth", "**", "*.nbt"), recursive=True)}
    for stray in sorted(on_disk - set(pieces)):
        problems.append("template %s on disk is not generated by this script" % stray)
    # 5. round-trip through the reader
    for p in pieces.values():
        again = load(p.t.path(RES)) if os.path.exists(p.t.path(RES)) else None
        if again is not None and again != p.t:
            problems.append("%s: the file does not round-trip" % p.t.name)
    for m in problems:
        print("FAIL " + m)
    print("%d template(s), %d pool(s), height budget %d/%d, %d problem(s)" % (len(pieces), len(pool_objs), budget, MAX_DISTANCE_FROM_CENTER, len(problems)))
    return not problems


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true", help="regenerate in memory and verify the files on disk and every jigsaw rule")
    ap.add_argument("--ascii", metavar="NAME", help="print the layers of one piece (e.g. labyrinth/base)")
    a = ap.parse_args(argv)
    if a.ascii:
        print(build_all()[a.ascii].t.ascii_layers())
        return 0
    if a.check:
        return 0 if check() else 1
    generate()
    return 0


if __name__ == "__main__":
    sys.exit(main())

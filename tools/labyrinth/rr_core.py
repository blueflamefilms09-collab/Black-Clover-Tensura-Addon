#!/usr/bin/env python3
"""
Rooms rebuilt, part 1: the engine (docs/labyrinth/rooms_rebuilt.md).

What this is. The Mega-Dungeon's rooms were drawn by many builders as static templates on the 9-block module grid (walls two layers thick, floor row 8, height
48). The owner asked to keep the dungeon's size and rebuild the INSIDES of the rooms, halls and hallways. This engine loads a shipped template, keeps everything
that other systems read (the sockets, the data markers, the chests and mechanisms, the size, the walking graph) and rebuilds the architecture around it: floor
pattern, wall bays with pilasters and niches, door surrounds, a new vaulted ceiling (barrel, groin, dome, gable, coffered) inside the same 48-block envelope,
lighting design, wear and room-type dressing. Every choice comes from a random generator seeded with (template, variant), so the same script writes the same
bytes, and a room has several variants that differ in all of those.

Vocabulary used below.
  * footprint:  the whole template, x in 0..W-1, z in 0..D-1.   * interior: x0..x1, z0..z1 = 2..W-3, 2..D-3 (inside the two wall layers).
  * depth:      0 = outer wall layer, 1 = inner wall layer, 2 = the first interior cell.   * FR = 8, the floor row; G = 9, the row a player stands in.
  * ceil:       the first row of the old roof (the shipped rooms have two roof layers); the new roof never starts below it.
Nothing here needs the game; the checks at the end of gen_rooms_rebuilt.py are the only eyes this has (nothing was seen in a game).
"""
import collections
import math
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D1  # noqa: E402
import nbt  # noqa: E402
from template import Template, load, Cell  # noqa: E402

FR = L.FLOOR_ROW           # 8
G = FR + 1                 # 9
DIRS = ("north", "east", "south", "west")
DV = L.STEP
OPP = L.OPPOSITE
RIGHT = L.RIGHT_OF
LEFT = L.LEFT_OF
ROOF_MAX = 45              # the highest row a roof may START at; two layers keep the top under row 47 (the template is 48 high)
STRUCT = os.path.join(L.RES, "data", "nusmp", "structure")

PAL_WALL = {L.BRICK, L.CRACKED_BRICK, L.COBBLED, L.POLISHED, L.CHISELED, L.TILE, L.CRACKED_TILE, L.ROCK}
PAL_FLOOR = {L.TILE, L.CRACKED_TILE, L.POLISHED, L.CHISELED, L.BRICK, L.CRACKED_BRICK, L.COBBLED}
STAIRISH = ("_stairs", "_slab", "_wall")


#: block entities are saved with their chunk (and a few are synced): the rebuild adds at most this many per room
BE_CAP = 10
_BE_SUFFIX = ("_banner", "_wall_banner", "_skull", "_wall_skull", "_head", "_wall_head", "_sign", "_wall_sign", "_hanging_sign", "_wall_hanging_sign", "_bed", "_shulker_box")
_BE_EXACT = {"minecraft:chiseled_bookshelf", "minecraft:decorated_pot", "minecraft:lectern", "minecraft:barrel", "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest",
             "minecraft:furnace", "minecraft:blast_furnace", "minecraft:smoker", "minecraft:brewing_stand", "minecraft:bell", "minecraft:campfire", "minecraft:soul_campfire",
             "minecraft:jukebox", "minecraft:beacon", "minecraft:enchanting_table", "minecraft:hopper", "minecraft:dropper", "minecraft:dispenser", "minecraft:spawner", "minecraft:conduit",
             "minecraft:daylight_detector", "minecraft:crafter", "minecraft:trial_spawner", "minecraft:vault", "minecraft:beehive", "minecraft:bee_nest", "minecraft:sculk_sensor",
             "minecraft:sculk_catalyst", "minecraft:sculk_shrieker", "minecraft:comparator", "minecraft:structure_block", "minecraft:jigsaw"}


def is_block_entity(block):
    """Does the block have a block entity in the game (the ones a rebuilt room may use are counted against its budget)?"""
    return block in _BE_EXACT or block.endswith(_BE_SUFFIX)


def path_of(block):
    return block.split(":", 1)[1] if ":" in block else block


def is_palette(block):
    return block in L.PALETTE


def stairs_of(block):
    return {L.BRICK: L.BRICK_STAIRS, L.TILE: L.TILE_STAIRS, L.POLISHED: L.POLISHED_STAIRS, L.COBBLED: L.COBBLED_STAIRS, L.PLANKS: L.PLANKS_STAIRS,
            L.CRACKED_BRICK: L.BRICK_STAIRS, L.CRACKED_TILE: L.TILE_STAIRS, L.CHISELED: L.POLISHED_STAIRS}.get(block, L.POLISHED_STAIRS)


def slab_of(block):
    return {L.BRICK: L.BRICK_SLAB, L.TILE: L.TILE_SLAB, L.POLISHED: L.POLISHED_SLAB, L.COBBLED: L.COBBLED_SLAB, L.PLANKS: L.PLANKS_SLAB,
            L.CRACKED_BRICK: L.BRICK_SLAB, L.CRACKED_TILE: L.TILE_SLAB, L.CHISELED: L.POLISHED_SLAB}.get(block, L.POLISHED_SLAB)


def wall_of(block):
    return {L.BRICK: L.BRICK_WALL, L.TILE: L.TILE_WALL, L.POLISHED: L.POLISHED_WALL, L.COBBLED: L.COBBLED_WALL}.get(block, L.POLISHED_WALL)


def template_path(name):
    return os.path.join(STRUCT, *name.split("/")) + ".nbt"


class Room:
    """A shipped template opened for rebuilding. `cells` is the live block map (the same dict the Template holds)."""

    def __init__(self, name, seed, template=None, interior=None, ceil=None, base=None):
        """Opens the shipped template `name` (default), or wraps a template built from scratch (`template`, with the `interior` bounds (x0, x1, z0, z1), the first roof
        row `ceil` and the shipped `base` template it replaces, for the comparisons of the checks)."""
        self.name = name
        self.t = template if template is not None else load(template_path(name), name)
        self.base_t = base if base is not None else load(template_path(name), name)        # a second, untouched copy: the checks compare against it
        self.base = {p: c for p, c in self.base_t.cells.items()}          # what the shipped template had (never modified)
        self.base_entities = list(self.base_t.entities)
        self.cells = self.t.cells
        self.W, self.H, self.D = self.t.size
        self.rng = random.Random("nusmp:rooms_rebuilt:%s:%s" % (name, seed))
        self.x0, self.x1, self.z0, self.z1 = interior if interior else (2, self.W - 3, 2, self.D - 3)
        self.cx, self.cz = self.W // 2, self.D // 2
        self.added = 0
        self.be_added = 0            # block-entity blocks (banners, skulls, pots, barrels ...) the rebuild added
        self.be_cap = BE_CAP
        self.scratch = template is not None
        if self.scratch:
            self.ceil, self.layers = ceil, 2
        else:
            self.ceil, self.layers = self.find_roof()
        self.regular = self.ceil is not None and self.layers <= 3
        self.doors = self.find_doors()
        self.markers = D1.markers_of(self.t)
        self.reserved = set()
        self.locked = set()          # cells no pass may touch (markers, door aprons, stand cells ...)
        self.stands = self.find_stands()
        if self.regular and not self.scratch:
            self.reserve()

    # ------------------------------------------------------------------ analysis
    def find_roof(self):
        cnt = collections.Counter(y for (x, y, z) in self.cells)
        full = self.W * self.D * 0.9
        ceil = None
        for y in range(G, self.H):
            if cnt[y] >= full:
                ceil = y
                break
        if ceil is None:
            return None, 0
        layers = 0
        while ceil + layers < self.H and cnt[ceil + layers] >= full:
            layers += 1
        return ceil, layers

    def find_doors(self):
        out = []
        for (x, y, z), orient, data in self.t.jigsaws():
            face = orient.split("_")[0]
            side = {"north": "n", "south": "s", "west": "w", "east": "e"}[face]
            lat = x if side in ("n", "s") else z
            out.append((side, lat, (x, y, z)))
        return out

    def find_stands(self):
        out = []
        for e in self.t.entities:
            n = e.get("nbt", {})
            if str(n.get("id", "")) == "minecraft:armor_stand":
                bp = e.get("blockPos")
                if bp is not None:
                    out.append((int(bp[0]), int(bp[1]), int(bp[2])))
        return out

    def wcell(self, side, lat, d, y):
        """World-local cell of a wall address: depth d from the outer face (0 outer layer, 1 inner layer, 2.. interior), lateral position lat (x for n/s, z for w/e)."""
        if side == "n":
            return (lat, y, d)
        if side == "s":
            return (lat, y, self.D - 1 - d)
        if side == "w":
            return (d, y, lat)
        return (self.W - 1 - d, y, lat)

    def into(self, side):
        return {"n": "south", "s": "north", "w": "east", "e": "west"}[side]

    def lat_len(self, side):
        return self.W if side in ("n", "s") else self.D

    def reserve(self):
        """Cells that additive passes must leave alone. Door aprons are the cut boxes of Clearance7 (7 wide, rows 9..15, wall layers and three cells into the room);
        a marker keeps its neighbours; a stand keeps its cell and the ring round it."""
        for side, lat, (jx, jy, jz) in self.doors:
            for dl in range(-3, 4):
                for d in range(0, 5):
                    for y in range(G, 16):
                        c = self.wcell(side, lat + dl, d, y)
                        self.reserved.add(c)
                        self.locked.add(c)
        for kind, x, y, z, args in self.markers:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in (-1, 0, 1, 2):
                        self.locked.add((x + dx, y + dy, z + dz))
        for (x, y, z) in self.stands:
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    for dy in (-1, 0, 1, 2):
                        self.locked.add((x + dx, y + dy, z + dz))
        # containers, spawners and the mod's own mechanism blocks keep a ring of air round them (nothing is built against a chest that a player must open)
        for (x, y, z), c in self.base.items():
            if c.block in ("minecraft:structure_block", "minecraft:jigsaw"):
                continue
            r = 2 if c.block.startswith("nusmp:") else (1 if c.nbt is not None else 0)
            if r:
                for dx in range(-r, r + 1):
                    for dz in range(-r, r + 1):
                        for dy in range(-1, r + 1):
                            self.locked.add((x + dx, y + dy, z + dz))

    # ------------------------------------------------------------------ cell access
    def get(self, x, y, z):
        return self.cells.get((x, y, z))

    def inside(self, x, z):
        return self.x0 <= x <= self.x1 and self.z0 <= z <= self.z1

    def in_box(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def free(self, x, y, z):
        """Air in the template, inside the footprint and not a locked cell."""
        return self.in_box(x, y, z) and (x, y, z) not in self.cells and (x, y, z) not in self.locked

    def free_col(self, x, z, y0, y1):
        return all(self.free(x, y, z) for y in range(y0, y1 + 1))

    def put(self, x, y, z, block, props=None, nbt_=None, over=False):
        """Writes a block. `over` allows replacing what is there. Returns True when written."""
        if not self.in_box(x, y, z):
            return False
        if ":" not in block:
            block = "minecraft:" + block
        if not over and (x, y, z) in self.cells:
            return False
        if (x, y, z) in self.locked and block != L.AIR:
            return False
        props = {k: ("true" if v is True else "false" if v is False else str(v)) for k, v in (props or {}).items()}
        err = L.check_state(block, props)
        if err:
            raise ValueError("%s: %s at %s" % (self.name, err, (x, y, z)))
        if block.startswith("nusmp:") and block not in L.CUSTOM:
            raise ValueError("%s: %s is not allowed in a template" % (self.name, block))
        be = is_block_entity(block)
        if be:
            old = self.cells.get((x, y, z))
            if self.be_added >= self.be_cap and not (old is not None and old.block == block):
                return False                    # block entities are saved with the chunk: a rebuilt room adds a handful at most
        self.t.set(x, y, z, block, props, nbt_)
        self.added += 1
        if be:
            self.be_added += 1
        return True

    def force(self, x, y, z, block, props=None, nbt_=None):
        """Writes a block even into a locked cell (shell work the engine itself does in reserved zones)."""
        return self.put_unlocked(x, y, z, block, props, nbt_)

    def put_unlocked(self, x, y, z, block, props=None, nbt_=None):
        saved = self.locked
        self.locked = set()
        try:
            return self.put(x, y, z, block, props, nbt_, over=True)
        finally:
            self.locked = saved

    def remove(self, x, y, z):
        self.cells.pop((x, y, z), None)

    def stair(self, x, y, z, block, facing, half="bottom", over=False):
        return self.put(x, y, z, block, {"facing": facing, "half": half, "waterlogged": "false"}, over=over)        # no `shape`: mega_lib.autoconnect turns neighbouring stairs into corners as the game does

    def slab(self, x, y, z, block, top=False, over=False):
        return self.put(x, y, z, block, {"type": "top" if top else "bottom", "waterlogged": "false"}, over=over)

    # ------------------------------------------------------------------ roof and walls
    def strip_roof(self):
        """Removes the old roof (every roof layer, over the whole footprint) and the old ribs, corbels and cornices that hang under it: the palette blocks of the top
        interior rows that have air under them. Palette blocks that stand on something (pillars, wall faces) stay. Returns the number of cells removed."""
        n = 0
        c = self.ceil
        for y in range(c, c + self.layers):
            for z in range(self.D):
                for x in range(self.W):
                    if self.cells.pop((x, y, z), None) is not None:
                        n += 1
        # ribs: palette cells of rows c-1 and c-2 in the interior with air (or a non-solid cell) directly below them and not part of a vertical stack
        for y in (c - 1, c - 2):
            for z in range(self.z0, self.z1 + 1):
                for x in range(self.x0, self.x1 + 1):
                    cell = self.cells.get((x, y, z))
                    if cell is None or not is_palette(cell.block):
                        continue
                    below = self.cells.get((x, y - 1, z))
                    if below is not None:
                        continue            # a pillar, a wall face or the rib of something that hangs from it (a chain, a crystal, leaves): stays and is extended later
                    self.cells.pop((x, y, z))
                    n += 1
        return n

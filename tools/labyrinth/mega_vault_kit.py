#!/usr/bin/env python3
"""
The building kit of the Crucible Vault templates (1.54.0, docs/labyrinth/mega_theme.md): everything `gen_mega_vault.py` needs that `mega_lib.py` does not
offer, and the checks that stand in for "walk through it in the game". It imports the shared toolkit (the grid, the palette, the template builder) and
adds three things:

  * `VaultMT`, a `mega_lib.MT` that refuses to put anything solid in a declared clear zone (the processional route from the door to the middle of the
    room): a vault is a trial of nerve, and the catastrophes of the Crucible only stay survivable if a person can always run out of it.
  * geometry and dressing primitives that are written once and used by every layout: polar placement, stairs from a direction of ascent, columns with
    plinth, collar, capital and lightning-rod spire, a raised dais with a pedestal, a hanging cage on chains, a spoked chandelier wheel, a statue shrine, wall
    furniture that works out its own facing from the wall it hangs on, cracks and rubble.
  * `analyze`, a static check of a finished template that answers the questions a game would: is the room sealed (flood fill of the air from inside never
    reaches the outside except through the door), can a person walk from the door to every pedestal and is the route three wide, is every pedestal on solid
    ground with free air above it and at least four blocks from the next, does every pedestal have its marker directly above it, is there exactly one
    vault centre and no spawn marker, does nothing float, how bright is the floor. Nothing here needs Minecraft; it reads the same cells the game will read.

Why a kit of its own and not `mega_lib` alone: the library is shared by every builder and must not be edited, and the vault is the one room of the
dungeon whose layout is a rule of the game (pedestals, a centre, a clear way out) and not only a look.
"""
import math
import os
import re
import sys
from collections import deque

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

GROUND = L.FLOOR_ROW + 1        # the first interior row: the cell a person stands in
FLOOR = L.FLOOR_ROW
PEDESTAL = "nusmp:relic_pedestal"
GOLD = "minecraft:gold_block"
RED_CARPET = "minecraft:red_carpet"
DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}
ROT = {"south": 0, "west": 4, "north": 8, "east": 12}     # skull / banner rotation of a block that faces that way
YAW = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}


# ===================================================================================================================
# the template: a MT with a clear zone
# ===================================================================================================================
class VaultMT(L.MT):
    """A Mega template that knows which cells must stay walkable. `keep_clear` declares them; `set` then refuses anything that blocks a person there."""

    def __init__(self, name, mx, mz):
        super().__init__(name, mx, mz)
        self.clear = set()

    def keep_clear(self, x1, z1, x2, z2, y1=GROUND, y2=GROUND + 2):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for y in range(y1, y2 + 1):
                    self.clear.add((x, y, z))
        return self

    def set(self, x, y, z, block, props=None, nbt_=None):
        if (x, y, z) in self.clear and not passable(block):
            raise ValueError("%s: %s at (%d,%d,%d) would block the clear route of the vault" % (self.name, block, x, y, z))
        return super().set(x, y, z, block, props, nbt_)

    def free(self, x, y, z):
        return self.get(x, y, z) is None

    def try_set(self, x, y, z, block, props=None):
        """Sets only into an empty cell (decor never overwrites architecture); returns whether it was placed."""
        if not (0 <= x < self.W and 0 <= y < L.HEIGHT and 0 <= z < self.D) or self.get(x, y, z) is not None:
            return False
        self.set(x, y, z, block, props)
        return True

    def force(self, x, y, z, block, props=None):
        """Sets a cell whatever is there (architecture carved or replaced on purpose)."""
        self.remove(x, y, z)
        return self.set(x, y, z, block, props)

    def carve(self, x, y, z):
        """Makes a cell air: removes whatever is there (and declares nothing: the interior is unset = air)."""
        self.remove(x, y, z)
        return self


# ===================================================================================================================
# geometry
# ===================================================================================================================
def polar(cx, cz, theta_deg, rho):
    """The (x, z) at angle `theta_deg` and radius `rho` from (cx, cz): 0 deg is east, 90 deg north (smaller z), 270 deg south (the door)."""
    a = math.radians(theta_deg)
    return cx + rho * math.cos(a), cz - rho * math.sin(a)


def rpolar(cx, cz, theta_deg, rho):
    x, z = polar(cx, cz, theta_deg, rho)
    return int(round(x)), int(round(z))


def dist2(x, z, cx, cz):
    return (x - cx) ** 2 + (z - cz) ** 2


def angle_of(x, z, cx, cz):
    """The angle of the cell as `polar` measures it (0..360)."""
    return math.degrees(math.atan2(-(z - cz), x - cx)) % 360.0


def toward(x, z, cx, cz):
    """The compass direction from (x, z) towards (cx, cz), by the larger component."""
    dx, dz = cx - x, cz - z
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def away(x, z, cx, cz):
    return L.OPPOSITE[toward(x, z, cx, cz)]


def mirror_x(x, w):
    return w - 1 - x


# ===================================================================================================================
# blocks
# ===================================================================================================================
def stairs(mt, x, y, z, ascends, half="bottom", block=L.POLISHED_STAIRS, force=False):
    """A stair block whose high side is towards `ascends` (the direction a person walks to climb it)."""
    (mt.force if force else mt.set)(x, y, z, block, {"facing": ascends, "half": half})


def slab(mt, x, y, z, kind="bottom", block=L.POLISHED_SLAB, force=False):
    (mt.force if force else mt.set)(x, y, z, block, {"type": kind})


def hang_chain(mt, x, z, y_top, y_bottom, force=False):
    """Chain blocks from y_top (the highest) down to y_bottom (inclusive)."""
    for y in range(y_bottom, y_top + 1):
        (mt.force if force else mt.set)(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})


def lantern(mt, x, y, z, hanging=True, soul=True):
    mt.set(x, y, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true" if hanging else "false"})


def candle_cluster(mt, x, y, z, n=3, color="white", if_free=False):
    """Lit candles (1 to 4 in one cell, light 3 each). `if_free` leaves an occupied cell alone (a field of candles must never replace a dais or a stair)."""
    if if_free and (mt.get(x, y, z) is not None or mt.get(x, y - 1, z) is None or (x, y, z) in mt.clear):
        return False
    mt.set(x, y, z, "minecraft:%s_candle" % color, {"candles": str(max(1, min(4, n))), "lit": "true"})
    return True


def skull(mt, x, y, z, rot=0, kind="skeleton"):
    mt.set(x, y, z, "minecraft:%s_skull" % kind, {"rotation": str(rot % 16)})


def solid_neighbour(mt, x, y, z):
    """The direction of a blocking wall next to the air cell (x, y, z), or None; the first of north, east, south, west."""
    for d in ("north", "east", "south", "west"):
        dx, dz = DIRS[d]
        c = mt.get(x + dx, y, z + dz)
        if c is not None and blocks_movement(c.block) and is_opaque(c.block, c.props):
            return d
    return None


def wall_item(mt, x, y, z, kind, **kw):
    """Hangs a wall item on whichever wall touches the air cell (x, y, z): its facing is worked out from the wall, so a curved wall needs no hand-made
    orientation. Returns False (and places nothing) when no wall touches the cell. kind: soul_torch, torch, banner, skull, lantern_bracket."""
    wall = solid_neighbour(mt, x, y, z)
    if wall is None or mt.get(x, y, z) is not None:
        return False
    face = L.OPPOSITE[wall]
    if kind == "soul_torch":
        mt.set(x, y, z, "minecraft:soul_wall_torch", {"facing": face})
    elif kind == "torch":
        mt.set(x, y, z, "minecraft:wall_torch", {"facing": face})
    elif kind == "banner":
        mt.set(x, y, z, "minecraft:%s_wall_banner" % kw.get("color", "red"), {"facing": face})
    elif kind == "skull":
        mt.set(x, y, z, "minecraft:%s_wall_skull" % kw.get("skull", "skeleton"), {"facing": face})
    else:
        raise ValueError("unknown wall item " + kind)
    return True


# ===================================================================================================================
# furniture of the vault
# ===================================================================================================================
def column(mt, cx, cz, y0=GROUND, y1=GROUND + 12, spire=3, collar=True, lanterns=True):
    """A pillar on a 3x3 footprint: a plinth, a plus-shaped shaft (a polished block ringed by polished walls), a gold collar, an inverted-stair capital and a
    spire of lightning rods on top. `y0` is the first row of the plinth, `y1` the last of the capital; the spire stands above it. Two soul lanterns hang
    from the capital. Symmetrical by construction, so a mirrored pair is an exact mirror."""
    arms = (("north", 0, -1), ("south", 0, 1), ("east", 1, 0), ("west", -1, 0))
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            mt.set(cx + dx, y0, cz + dz, L.POLISHED)
    for dx, dz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):
        slab(mt, cx + dx, y0 + 1, cz + dz, "bottom")
    mt.set(cx, y0 + 1, cz, L.POLISHED)
    if lanterns:
        for dx, dz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):                  # a lit candle cluster on each corner of the plinth
            candle_cluster(mt, cx + dx, y0 + 2, cz + dz, 3 + ((cx + cz + dx) & 1), "white")
    for d, dx, dz in arms:
        mt.set(cx + dx, y0 + 1, cz + dz, L.POLISHED_STAIRS, {"facing": L.OPPOSITE[d], "half": "bottom"})
    top = y1 - 1
    for y in range(y0 + 2, top + 1):
        mt.set(cx, y, cz, L.POLISHED)
        for d, dx, dz in arms:
            if collar and y == (y0 + top) // 2:
                mt.set(cx + dx, y, cz + dz, GOLD)
            else:
                mt.set(cx + dx, y, cz + dz, L.POLISHED_WALL)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx == 0 and dz == 0:
                mt.set(cx, y1, cz, L.CHISELED)
            elif dx == 0 or dz == 0:
                d = {(0, -1): "north", (0, 1): "south", (1, 0): "east", (-1, 0): "west"}[(dx, dz)]
                mt.set(cx + dx, y1, cz + dz, L.POLISHED_STAIRS, {"facing": L.OPPOSITE[d], "half": "top"})
            else:
                mt.set(cx + dx, y1, cz + dz, L.POLISHED_SLAB, {"type": "top"})
    stop = ceiling_at(mt, cx, cz, y1 + 1)                      # the spire ends under the vault, never inside it
    for k in range(spire):
        if stop is not None and y1 + 1 + k >= stop:
            break
        mt.set(cx, y1 + 1 + k, cz, "minecraft:lightning_rod", {"facing": "up"})
    if lanterns:
        for dx, dz in ((-1, -1), (-1, 1), (1, -1), (1, 1)):                  # under the four corner slabs of the capital
            mt.try_set(cx + dx, y1 - 1, cz + dz, L.SOUL_LANTERN, {"hanging": "true"})
    return mt


def dais(mt, cx, cz, front, pedestal=True, w=5, base=L.POLISHED, trim=GOLD, slot=None, height=1, y0=GROUND):
    """A raised dais (`w` x `w`, `height` blocks) centred on (cx, cz) with a flight of stairs on the side called `front` (the side the people come from),
    four corner posts with standing soul lanterns and, at the middle, a Relic Pedestal with its marker directly above. The platform is polished stone with a
    band of the trim block round its top edge. Returns the pedestal block's cell (x, y, z)."""
    half = w // 2
    top_y = y0 + height - 1
    fx, fz = DIRS[front]
    for dx in range(-half, half + 1):
        for dz in range(-half, half + 1):
            edge = max(abs(dx), abs(dz)) == half
            for k in range(height):
                mt.force(cx + dx, y0 + k, cz + dz, trim if (edge and k == height - 1 and (dx + dz) % 2 == 0) else base)
    # the flight of stairs: one row of stairs in front of the platform, ascending towards it
    for k in range(-half, half + 1):
        sx, sz = cx + fx * (half + 1) + (k if fz else 0), cz + fz * (half + 1) + (k if fx else 0)
        stairs(mt, sx, y0, sz, L.OPPOSITE[front], "bottom", L.POLISHED_STAIRS, force=True)
    # corner posts
    for dx in (-half, half):
        for dz in (-half, half):
            mt.force(cx + dx, top_y + 1, cz + dz, L.POLISHED_WALL)
            mt.set(cx + dx, top_y + 2, cz + dz, L.SOUL_LANTERN, {"hanging": "false"})
    if pedestal:
        for ox, oz in ((1, 0), (-1, 0), (0, 1), (0, -1)):             # four lit candle clusters beside the pedestal: the relic must be well lit
            candle_cluster(mt, cx + ox, top_y + 1, cz + oz, 4, "white")
        mt.force(cx, top_y + 1, cz, PEDESTAL)
        if slot is not None:
            mt.marker(cx, top_y + 2, cz, "pedestal", slot=slot)
    return cx, top_y + 1, cz


def cage_on_chains(mt, x, z, y_bottom, w=3, d=3, h=4, skull_inside=True):
    """A bar cage hung from the ceiling above it by four chains: (x, z) is its middle, `y_bottom` the lowest bar row. The chains run up to the first solid
    cell above; the cage is the shared kit's bars with a skull on a block of the floor of the cage."""
    ceil = ceiling_at(mt, x, z, y_bottom + h)
    if ceil is None:
        return False
    x0, z0 = x - w // 2, z - d // 2
    for dx in range(w):
        for dz in range(d):
            for dy in range(h):
                edge = dx in (0, w - 1) or dz in (0, d - 1)
                if (edge and dy < h - 0) or dy == h - 1 or dy == 0:
                    mt.try_set(x0 + dx, y_bottom + dy, z0 + dz, L.IRON_BARS)
    if skull_inside:
        mt.try_set(x, y_bottom + 1, z, "minecraft:skeleton_skull", {"rotation": str((x * 3 + z) % 16)})
    for cx, cz in ((x0, z0), (x0 + w - 1, z0), (x0, z0 + d - 1), (x0 + w - 1, z0 + d - 1)):
        for y in range(y_bottom + h, ceil):
            mt.try_set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    return True


def ceiling_at(mt, x, z, y_from):
    """The first non-air cell above y_from at (x, z), or None."""
    for y in range(y_from, L.HEIGHT):
        c = mt.get(x, y, z)
        if c is not None and blocks_movement(c.block) and c.block not in (L.CHAIN_BLOCK, L.IRON_BARS):
            return y
    return None


def chandelier_wheel(mt, cx, cz, y, r=3, hub_top=None):
    """A spoked wheel of chains at row `y` hung on a central chain, with a soul lantern under each of eight points of the rim and one under the hub. The rim
    is a ring of horizontal chains, the spokes run along both axes."""
    top = hub_top if hub_top is not None else ceiling_at(mt, cx, cz, y + 1)
    if top is None:
        return False
    for yy in range(y + 1, top):
        mt.try_set(cx, yy, cz, L.CHAIN_BLOCK, {"axis": "y"})
    mt.try_set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    for k in range(1, r):
        for sx, sz, ax in ((k, 0, "x"), (-k, 0, "x"), (0, k, "z"), (0, -k, "z")):
            mt.try_set(cx + sx, y, cz + sz, L.CHAIN_BLOCK, {"axis": ax})
    rim = []
    for a in range(0, 360, 45):
        px, pz = rpolar(cx, cz, a, r)
        rim.append((px, pz))
    for i, (px, pz) in enumerate(rim):
        mt.try_set(px, y, pz, L.POLISHED_WALL if i % 2 == 0 else L.CHAIN_BLOCK, None if i % 2 == 0 else {"axis": "x" if (px - cx) else "z"})
        mt.try_set(px, y - 1, pz, L.SOUL_LANTERN, {"hanging": "true"})
    mt.try_set(cx, y - 1, cz, L.SOUL_LANTERN, {"hanging": "true"})
    return True


def statue_shrine(mt, x, y, z, yaw, head="minecraft:iron_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
                  feet="minecraft:iron_boots", weapon="minecraft:iron_sword"):
    """An armoured statue on a polished plinth (an armor stand: an entity of the template, no custom block). `yaw` is the stand's rotation in degrees."""
    mt.force(x, y, z, L.POLISHED)
    mt.force(x, y + 1, z, L.POLISHED_SLAB, {"type": "bottom"})
    mt.armor_stand(x, y + 1, z, yaw=yaw, head=head, chest=chest, legs=legs, feet=feet, mainhand=weapon, dy=0.5)


def brazier(mt, x, z, y=GROUND, height=1, fire="minecraft:soul_campfire"):
    """A brazier: a polished post `height` blocks high with a lit campfire on top (soul fire by default). Light 10 at head height, smoke rising; nothing
    stands IN the fire (the post is under it), so it never burns a person who walks past."""
    for k in range(height):
        mt.force(x, y + k, z, L.POLISHED_WALL if k else L.POLISHED)
    mt.force(x, y + height, z, fire, {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})


def window(mt, x, y, z, behind, glass="minecraft:purple_stained_glass_pane", light=11):
    """A coloured window in a solid wall: a stained glass pane at (x, y, z) with an invisible light block in the wall cell `behind` it (a (dx, dz) step), so
    the pane glows. The light block is vanilla (`minecraft:light`) and invisible in play; the pane keeps the cell sealed to mobs."""
    mt.force(x, y, z, glass)
    mt.force(x + behind[0], y, z + behind[1], "minecraft:light", {"level": str(light)})


def crack(mt, x, y, z, block=None):
    """Replaces a wall or floor block with its cracked sibling (a deepslate brick or tile), if it is one."""
    c = mt.get(x, y, z)
    if c is None:
        return
    if c.block == L.BRICK:
        mt.force(x, y, z, L.CRACKED_BRICK)
    elif c.block == L.TILE:
        mt.force(x, y, z, L.CRACKED_TILE)


def rubble_pile(mt, x, z, spread=1):
    """A little heap of fallen stone: a cobbled block with cobbled slabs and stairs round it (never on a cell that is not floor)."""
    mt.try_set(x, GROUND, z, L.COBBLED)
    for dx, dz, d in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        if mt.rng.random() < 0.6 and spread:
            if mt.rng.random() < 0.5:
                mt.try_set(x + dx, GROUND, z + dz, L.COBBLED_SLAB, {"type": "bottom"})
            else:
                mt.try_set(x + dx, GROUND, z + dz, L.COBBLED_STAIRS, {"facing": d, "half": "bottom"})
    if mt.rng.random() < 0.5:
        mt.try_set(x, GROUND + 1, z, L.COBBLED_SLAB, {"type": "bottom"})


# ===================================================================================================================
# block classes for the checks
# ===================================================================================================================
_PASSABLE = re.compile(r"air|cave_air|void_air|.*_carpet|moss_carpet|.*banner|.*wall_torch|torch|soul_torch|light|structure_block|jigsaw|"
                       r"cobweb|.*_pressure_plate|.*_button|lever|string|vine|glow_lichen|sculk_vein|.*_sign|.*_wall_sign|fire|soul_fire")


def _path(block):
    return block.split(":", 1)[1] if ":" in block else block


def passable(block, props=None):
    return bool(_PASSABLE.fullmatch(_path(block)))


def blocks_movement(block, props=None):
    return not passable(block, props)


_TRANSPARENT = re.compile(r".*_stairs|.*_slab|.*_wall|.*_fence|.*_fence_gate|iron_bars|.*glass_pane|chain|.*lantern|.*candle|.*_carpet|lightning_rod|"
                          r".*skull|.*_head|.*banner|pointed_dripstone|end_rod|.*_trapdoor|.*_door|.*campfire|cauldron|.*_bed|anvil|grindstone|"
                          r"glass|.*_stained_glass|tinted_glass|ladder|cobweb|brewing_stand|flower_pot|bell|.*_sign|armor_stand|lectern")


def is_opaque(block, props=None):
    """Does the block stop block light (a full opaque cube)? Passable blocks and the shapes in _TRANSPARENT do not."""
    p = _path(block)
    if passable(block, props) or _TRANSPARENT.fullmatch(p):
        return False
    if p in ("barrier", "air"):
        return False
    return True


_EMISSION = {"lantern": 15, "soul_lantern": 10, "torch": 14, "wall_torch": 14, "soul_torch": 10, "soul_wall_torch": 10, "campfire": 15, "soul_campfire": 10,
             "shroomlight": 15, "glowstone": 15, "sea_lantern": 15, "magma_block": 3, "fire": 15, "soul_fire": 10, "lava": 15, "end_rod": 14,
             "beacon": 15, "conduit": 15, "ochre_froglight": 15, "verdant_froglight": 15, "pearlescent_froglight": 15, "redstone_lamp": 15,
             "jack_o_lantern": 15, "lava_cauldron": 15, "respawn_anchor": 15, "crying_obsidian": 10}


def emission(block, props):
    p = _path(block)
    if p == "light":
        return int(props.get("level", "15"))
    if p.endswith("candle") and props.get("lit") == "true":
        return 3 * int(props.get("candles", "1"))
    if p in ("campfire", "soul_campfire") and props.get("lit", "true") == "false":
        return 0
    return _EMISSION.get(p, 0)


# ===================================================================================================================
# the analysis
# ===================================================================================================================
def cells_where(t, pred):
    return [(pos, c) for pos, c in t.cells.items() if pred(c)]


def _blocking_set(t):
    return {pos for pos, c in t.cells.items() if blocks_movement(c.block, c.props)}


def seal_leaks(t, start):
    """Flood fill of the air from `start`; the cells on the template boundary it reaches (a leak lets monsters and light in or out). The door hole's own
    outer cells are not counted: they are the way in."""
    sx, sy, sz = t.size
    block = _blocking_set(t)
    seen = {start}
    q = deque([start])
    leaks = []
    while q:
        x, y, z = q.popleft()
        if x in (0, sx - 1) or z in (0, sz - 1) or y in (0, sy - 1):
            leaks.append((x, y, z))
            continue
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen or n in block:
                continue
            seen.add(n)
            q.append(n)
    return leaks, seen


def standing_cells(t, region=None):
    """Cells a person can stand in: feet and head cell passable, a blocking cell below."""
    block = _blocking_set(t)
    sx, sy, sz = t.size
    out = set()
    for (x, y, z) in block:
        if y + 2 >= sy:
            continue
        a, b = (x, y + 1, z), (x, y + 2, z)
        if a not in block and b not in block:
            c = t.cells[(x, y, z)]
            if _path(c.block) in ("chain", "iron_bars", "lightning_rod", "pointed_dripstone") or _path(c.block).endswith("candle") or _path(c.block).endswith("skull") \
                    or _path(c.block).endswith("lantern"):
                continue
            out.add(a)
    return out, block


def walk(t, start):
    """The standing cells reachable from `start` on foot: a step up of one block (a jump), a drop of up to three."""
    stand, block = standing_cells(t)
    if start not in stand:
        return set(), stand
    seen = {start}
    q = deque([start])
    while q:
        x, y, z = q.popleft()
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            for dy in (0, 1, -1, -2, -3):
                n = (x + dx, y + dy, z + dz)
                if n in seen or n not in stand:
                    continue
                if dy == 1 and (x, y + 2, z) in block:        # no headroom to jump
                    continue
                base = y + max(dy, 0)                          # the column beside must be open at the height the person crosses it
                if any((x + dx, base + k, z + dz) in block for k in (0, 1)):
                    continue
                seen.add(n)
                q.append(n)
                break
    return seen, stand


def light_map(t):
    """Block light on every cell (the vanilla rule: 15 at a full source, one less per step through anything that is not an opaque cube)."""
    sx, sy, sz = t.size
    level = {}
    q = deque()
    for pos, c in t.cells.items():
        e = emission(c.block, c.props)
        if e > 0:
            level[pos] = e
            q.append(pos)
    while q:
        pos = q.popleft()
        v = level[pos]
        if v <= 1:
            continue
        x, y, z = pos
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if not (0 <= n[0] < sx and 0 <= n[1] < sy and 0 <= n[2] < sz):
                continue
            c = t.cells.get(n)
            if c is not None and is_opaque(c.block, c.props):
                continue
            if level.get(n, 0) < v - 1:
                level[n] = v - 1
                q.append(n)
    return level


def floating_decor(t):
    """Cells of decor (not architecture) that touch no other block at all, and decor that needs a wall or floor and has none."""
    bad = []
    attach = re.compile(r".*banner|.*wall_torch|.*wall_skull|.*wall_head")
    for (x, y, z), c in t.cells.items():
        p = _path(c.block)
        nb = [t.cells.get((x + dx, y + dy, z + dz)) for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))]
        if all(n is None for n in nb) and p not in ("structure_block", "jigsaw"):
            bad.append(((x, y, z), c.block, "touches nothing"))
            continue
        below = t.cells.get((x, y - 1, z))
        above = t.cells.get((x, y + 1, z))
        if p in ("torch", "soul_torch") or p.endswith("candle") or p.endswith("_carpet") or (p == "lightning_rod" and c.props.get("facing") == "up") \
                or (p in ("lantern", "soul_lantern") and c.props.get("hanging") == "false") or (p.endswith("skull") and not p.endswith("wall_skull")):
            if below is None or (not blocks_movement(below.block) and _path(below.block) not in ("lightning_rod",)):
                bad.append(((x, y, z), c.block, "has nothing under it"))
        elif p in ("lantern", "soul_lantern") and c.props.get("hanging") == "true":
            if above is None or (not blocks_movement(above.block) and _path(above.block) != "chain"):
                bad.append(((x, y, z), c.block, "hangs from nothing"))
        elif attach.fullmatch(p):
            f = c.props.get("facing")
            if f in DIRS:
                dx, dz = DIRS[L.OPPOSITE[f]]
                back = t.cells.get((x + dx, y, z + dz))
                if back is None or not blocks_movement(back.block):
                    bad.append(((x, y, z), c.block, "has no wall behind it"))
        elif p == "chain" and c.props.get("axis", "y") == "y":
            if (above is None or not (blocks_movement(above.block) or _path(above.block) == "chain")) and (below is None or _path(below.block) not in ("chain", "lantern", "soul_lantern") and not blocks_movement(below.block)):
                bad.append(((x, y, z), c.block, "a chain that holds nothing and hangs from nothing"))
    return bad


def analyze(t, entry, n_pedestals, clear=None):
    """Runs every check on a finished template. `entry` is the standing cell just inside the door, `clear` the declared clear zone (cells). Returns (problems, stats)."""
    problems = []
    stats = {}
    sx, sy, sz = t.size
    # markers and sockets
    markers = []
    for pos, c in t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            markers.append((pos, str(c.nbt.get("metadata", ""))))
    kinds = [meta.split(";")[0] for _, meta in markers]
    if kinds.count("nusmp:vault_center") != 1:
        problems.append("exactly one vault_center marker expected, found %d" % kinds.count("nusmp:vault_center"))
    for bad in ("nusmp:spawn", "nusmp:patrol", "nusmp:faction", "nusmp:trap", "nusmp:arrival"):
        if bad in kinds:
            problems.append("a vault carries no %s marker" % bad)
    peds = {pos: c for pos, c in t.cells.items() if c.block == PEDESTAL}
    pm = {}
    for pos, meta in markers:
        if meta.startswith("nusmp:pedestal"):
            args = dict(a.split("=", 1) for a in meta.split(";")[1:] if "=" in a)
            pm[pos] = args.get("slot")
    if len(peds) != n_pedestals:
        problems.append("%d pedestal blocks, %d expected" % (len(peds), n_pedestals))
    if sorted(pm.values()) != [str(i) for i in range(n_pedestals)]:
        problems.append("pedestal markers carry slots %s, expected 0..%d" % (sorted(pm.values()), n_pedestals - 1))
    block = _blocking_set(t)
    for (x, y, z) in peds:
        if (x, y + 1, z) not in pm:
            problems.append("pedestal at (%d,%d,%d) has no marker directly above" % (x, y, z))
        below = t.cells.get((x, y - 1, z))
        if below is None or not is_opaque(below.block, below.props):
            problems.append("pedestal at (%d,%d,%d) does not stand on a solid block (%s)" % (x, y, z, below.block if below else "nothing"))
        for k in (2, 3, 4):
            c = t.cells.get((x, y + k, z))
            if c is not None and blocks_movement(c.block, c.props):
                problems.append("pedestal at (%d,%d,%d): cell %d above is %s (the relic floats there)" % (x, y, z, k, c.block))
        for k in (1, 2, 3):                 # the relic hangs about 1.3 blocks up and is 0.5 wide: keep the air round it too
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                c = t.cells.get((x + dx, y + k, z + dz))
                if c is not None and blocks_movement(c.block, c.props) and k >= 2:
                    problems.append("pedestal at (%d,%d,%d): %s beside the relic at +%d" % (x, y, z, c.block, k))
    pl = list(peds)
    for i in range(len(pl)):
        for j in range(i + 1, len(pl)):
            d = math.hypot(pl[i][0] - pl[j][0], pl[i][2] - pl[j][2])
            if d < 4.0:
                problems.append("pedestals %s and %s are %.1f blocks apart (4 minimum)" % (pl[i], pl[j], d))
    jig = t.jigsaws()
    if len(jig) != 1:
        problems.append("%d jigsaws, exactly one (the door) expected" % len(jig))
    # the door and the seal
    leaks, air = seal_leaks(t, entry)
    outside = [p for p in leaks if not (p[2] == sz - 1 and 12 <= p[0] <= 14 and L.DOOR_ROW <= p[1] < L.DOOR_ROW + L.DOOR_H)]
    if outside:
        problems.append("the room leaks to the outside at %d cell(s), e.g. %s" % (len(outside), outside[:3]))
    if not leaks:
        problems.append("the door does not open to the outside (nothing reaches the south face)")
    stats["air_cells"] = len(air)
    # walkability
    reach, stand = walk(t, entry)
    if entry not in stand:
        problems.append("the entry cell %s is not a standing cell" % (entry,))
    else:
        for (x, y, z) in peds:
            near = [(x + dx, y + dy, z + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for dy in (0, 1)]
            if not any(n in reach for n in near):
                problems.append("a person cannot walk to the pedestal at (%d,%d,%d)" % (x, y, z))
        for (pos, meta) in markers:
            if meta.startswith("nusmp:vault_center"):
                below = (pos[0], pos[1] - 1, pos[2])
                if pos not in reach and not any((pos[0], pos[1] + dy, pos[2]) in reach for dy in (-1, 0, 1)):
                    problems.append("the vault centre %s cannot be reached on foot" % (pos,))
        cols = {(x, z) for (x, y, z) in clear} if clear else set()
        for (x, z) in sorted(cols):
            if not any((x, y, z) in stand and (x, y, z) in reach for y in range(GROUND - 1, GROUND + 3)):
                problems.append("the declared clear route has no walkable ground at (%d, %d)" % (x, z))
                break
    stats["walkable_cells"] = len(reach)
    # light
    lv = light_map(t)
    floor = [c for c in reach if (c[1] == GROUND or c[1] == GROUND + 1) and c[2] <= sz - 3]       # the door hole itself is lit from the other side
    low = [c for c in floor if lv.get(c, 0) < 5]
    stats["floor_cells"] = len(floor)
    stats["dark_floor_cells"] = len(low)
    if floor and len(low) > 0.05 * len(floor):
        problems.append("%d of %d floor cells are darker than light 5 (%.0f %%), e.g. %s" % (len(low), len(floor), 100.0 * len(low) / len(floor), sorted(low)[:6]))
    if floor:
        vals = sorted(lv.get(c, 0) for c in floor)
        stats["light_min"], stats["light_median"] = vals[0], vals[len(vals) // 2]
    for (x, y, z) in peds:
        if lv.get((x, y + 1, z), 0) < 8:
            problems.append("the pedestal at (%d,%d,%d) is lit only at %d" % (x, y, z, lv.get((x, y + 1, z), 0)))
    # floating decor
    fl = floating_decor(t)
    for pos, b, why in fl[:12]:
        problems.append("%s at %s %s" % (b, pos, why))
    if len(fl) > 12:
        problems.append("... and %d more floating blocks" % (len(fl) - 12))
    # sizes
    stats["blocks"] = len(t.cells)
    stats["size"] = t.size
    return problems, stats

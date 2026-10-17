#!/usr/bin/env python3
"""
Helpers of the Landing and Safe Zone templates of the Mega-Dungeon (1.54.0, docs/labyrinth/mega_safe.md). It sits on top of `mega_lib.py` (the shared
toolkit: never edited here) and adds what only the places a player RESTS in need:

  * `SR`: a room builder that knows the shell, the doors, the exit gate and the rest anchors. `seat()`, `bed()`, `hearth()` place the block AND its
    data marker in one call, so a block and the marker above it can never drift apart (the Java reader finds a rest anchor by the marker);
  * `Wall`: wall-relative coordinates (along the wall, up, out from the wall), so a bench, a shelf or a workbench is written once and works on all
    four walls;
  * architecture written once (cornice, plinth, pilasters, window slits with shutters, niches, arches), so every room has real structure;
  * lights (chandeliers, wall lamps, braziers) and furniture (tables, benches, bunks, shelves, counters ...);
  * `finish_and_check()`: carves the doorways, runs `mega_safe_verify.verify()` (shell closed, doors reachable, anchors reachable, floor lit, nothing
    hangs from nothing, markers where Java expects them) and refuses to save a template that fails.

Why a library and not eight scripts of raw `MT` calls: these rooms are the first thing a player sees and the places they rest in, so they need a lot of
hand-placed detail, and detail written eight times drifts. All architecture uses the palette of `mega_lib` (so the Region Theme restyles it); furniture
and decoration use anything vanilla.

Vocabulary of the compat hints (the `as=` argument; the mod-compat processor swaps the block BELOW the marker when the mod is loaded):
    seat;as=chair|sofa     a stair seat (a bench row is `sofa`)       hearth;as=stove          a campfire / furnace
    bed;as=bed             the FOOT of a bed                          furniture;as=table       the middle slabs of a rustic table
    furniture;as=storage   a barrel                                   furniture;as=cutting_board   a pressure plate on a counter
    furniture;as=cooking_pot   a cauldron next to a fire              furniture;as=skillet     an iron trapdoor on a counter
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_arch_check as CK  # noqa: E402
import mega_lib as L  # noqa: E402
import mega_safe_verify as V  # noqa: E402
import nbt  # noqa: E402
from mega_lib import (MT, Frame, FLOOR_ROW, WALL, MODULE, DOOR_ROW, DOOR_H, AIR, IRON_BARS, CHAIN_BLOCK, LANTERN, SOUL_LANTERN,  # noqa: E402,F401
                      BRICK, BRICK_STAIRS, BRICK_SLAB, BRICK_WALL, TILE, TILE_STAIRS, TILE_SLAB, POLISHED, POLISHED_STAIRS, POLISHED_SLAB,
                      POLISHED_WALL, CHISELED, COBBLED, COBBLED_STAIRS, COBBLED_SLAB, COBBLED_WALL, CRACKED_BRICK, CRACKED_TILE, ROCK, PLANKS,
                      PLANKS_STAIRS, PLANKS_SLAB, FENCE, FENCE_GATE, TRAPDOOR, WOOD_DOOR, LOG, STRIPPED_LOG, BUTTON, PLATE, OPPOSITE, STEP,
                      RIGHT_OF, LEFT_OF, FACING, SIDES)

EXIT = "nusmp:labyrinth_exit"
INWARD = {"n": "south", "s": "north", "w": "east", "e": "west"}      # from a wall into the room
TOWARD = {"n": "north", "s": "south", "w": "west", "e": "east"}      # from the room toward that wall
SIDE_OF = {"north": "n", "south": "s", "west": "w", "east": "e"}


def pth(block):
    return block.split(":", 1)[1] if ":" in block else block


def skull_rot(facing):
    """The 0..15 rotation of a standing skull or banner that looks toward `facing`."""
    return str({"south": 0, "west": 4, "north": 8, "east": 12}[facing])


def wood_of(block, wood):
    """`minecraft:dark_oak_stairs` with wood='spruce' -> `minecraft:spruce_stairs` (palette woods become accent woods)."""
    n = pth(block)
    for k in ("dark_oak", "oak", "spruce", "birch", "jungle", "acacia", "crimson", "warped", "cherry", "mangrove", "bamboo"):
        if n.startswith(k + "_"):
            return "minecraft:" + wood + n[len(k):]
    return block


# =============================================================================================================================== the room
class SR(MT):
    """A Landing or Safe Zone template: an `MT` with its shell built, `h` interior rows and the rest anchors recorded for the verifier."""

    def __init__(self, name, mx, mz, h, floor=TILE, wall=BRICK, ceiling=TILE, wear=0.05, ceiling_layers=2):
        self.overwrites = []                     # (cell, old block, new block) for every functional or decorative block a later call replaced: the verifier lists them
        super().__init__(name, mx, mz)
        self.h = h
        self.top = FLOOR_ROW + h                 # the last interior row; the ceiling is top+1 .. top+ceiling_layers
        self.stand = FLOOR_ROW + 1               # the row players stand in
        self.room(h, floor=floor, wall=wall, ceiling=ceiling, wear=wear, ceiling_layers=ceiling_layers)
        self.expect = {}
        self.report = None
        self.intended = []                       # every marker the builder asked for: the verifier checks that none was overwritten later

    # ---- placing
    def set(self, x, y, z, block, props=None, nbt_=None):
        """`MT.set`, remembering when it replaces a block that mattered (a bed, a chest, a fire, a work station, a banner, a lantern ...): in a template written
        as code, "a lamp landed on the bed table" is the commonest mistake and nothing else would notice it."""
        old = self.t.get(x, y, z)
        full = block if ":" in block else "minecraft:" + block
        if old is not None and old.block != full and V.IMPORTANT.fullmatch(pth(old.block)):
            self.overwrites.append(((x, y, z), old.block, full))
        return super().set(x, y, z, block, props, nbt_)

    def marker(self, x, y, z, kind, **args):
        self.intended.append((x, y, z, kind))
        return super().marker(x, y, z, kind, **args)

    def set_(self, x, y, z, block, **props):
        """`set` with keyword properties; a cell inside a doorway hole is skipped silently (decoration must never fight a door)."""
        if (x, y, z) in self.reserved:
            return self
        return self.set(x, y, z, block, props or None)

    def clear(self, x, y, z):
        """An explicit air cell (carves what the shell put there)."""
        return self.set(x, y, z, AIR)

    def box_(self, x1, y1, z1, x2, y2, z2, block, **props):
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for x in range(min(x1, x2), max(x1, x2) + 1):
                    self.set_(x, y, z, block, **props)
        return self

    def is_free(self, x, y, z):
        return self.t.get(x, y, z) is None

    def door(self, side, offset, **kw):
        """The contract's ordinary door (pool `nusmp:mega/side`, name `nusmp:mega_door`) at `offset` along `side`."""
        return self.socket(side, offset, **kw)

    def lateral_edge(self, side):
        return self.W if side in "ns" else self.D

    # ---- the rest anchors: block + marker in one call
    def seat(self, x, y, z, facing, kind="chair", block=PLANKS_STAIRS):
        """A seat: the stair at (x,y,z) with its high side behind the sitter, and a `seat` marker in the cell above. `facing` is where the sitter looks."""
        self.set(x, y, z, block, {"facing": OPPOSITE[facing], "half": "bottom"})
        self.marker(x, y + 1, z, "seat", **{"as": kind, "facing": facing})
        return self

    def hearth(self, x, y, z, block="minecraft:campfire", hint="stove", facing="north", **props):
        """A cooking fire: the block at (x,y,z) (a lit campfire by default) and a `hearth` marker above it."""
        if pth(block).endswith("campfire"):
            p = {"facing": facing, "lit": "true", "signal_fire": "false", "waterlogged": "false"}
        elif pth(block) in ("furnace", "blast_furnace", "smoker"):
            p = {"facing": facing, "lit": "true"}          # lit, so the forge and the kitchens glow; the verifier does not count them as light (see mega_safe_verify._EMIT)
        else:
            p = {}
        p.update(props)
        self.set(x, y, z, block, p or None)
        self.marker(x, y + 1, z, "hearth", **{"as": hint})
        return self

    def bed(self, x, y, z, facing, color="red", hint="bed"):
        """A bed whose FOOT is at (x,y,z) and whose head is one cell toward `facing`; the `bed` marker is above the foot."""
        f = Frame(self, x, y, z, facing)
        f.put(0, 0, 0, "minecraft:%s_bed" % color, {"facing": f.dir("forward"), "part": "foot"})
        f.put(0, 0, 1, "minecraft:%s_bed" % color, {"facing": f.dir("forward"), "part": "head"})
        self.marker(x, y + 1, z, "bed", **{"as": hint})
        return self

    def chain_bed(self, x, y, z, facing, color="red", height=4, hint="bed"):
        """A bed hung from the ceiling above it by four chains (`mega_lib.chain_bed`), with the `bed` marker above the foot. `height` chain blocks hang
        above each end of the bed, so pick it so the top chain touches the ceiling or the floor above (the verifier refuses a chain that hangs from nothing)."""
        L.chain_bed(self, x, y, z, facing, color=color, height=height)
        self.marker(x, y + 1, z, "bed", **{"as": hint})
        return self

    def hint(self, x, y, z, as_):
        """A `furniture` marker above the block at (x,y,z): the compat processor may swap that block for another mod's (see the vocabulary above)."""
        self.marker(x, y + 1, z, "furniture", **{"as": as_})
        return self

    def patrol(self, x, z):
        self.marker(x, self.stand, z, "patrol")
        return self

    def arrival(self, x, z):
        """The ARRIVAL marker of a Landing: the standing cell where a player appears (at least 3 blocks from the exit gate)."""
        self.marker(x, self.stand, z, "arrival")
        return self

    # ---- finishing
    def finish_and_check(self, portcullis=True, **kw):
        """Puts a raised portcullis over every doorway (`portcullis()`), carves the doorways, verifies, and returns the report (the caller decides
        whether to save). The report also carries, as errors, every defect `mega_arch_check.problems` finds: that module applies the rules the game's own
        `canSurvive` methods apply (a standing lantern or candle needs a top face under it that covers the middle of the cell, a chain must end against a
        block, an armour stand must stand on something ...), which a walk-through model does not know, so a template that fails them is never written."""
        if portcullis and not self.finished:
            raise_portcullis(self)
        self.finish()
        self.report = V.verify(self, expect=self.expect, **kw)
        for line in CK.problems(self.t, self.name):
            self.report.err("game rule: " + line)
        return self.report


class Wall:
    """Wall-relative coordinates for one wall: `lat` is the absolute position along the wall, `out` counts interior cells away from it (0 = the cell
    touching the wall, -1 = the inner wall layer, -2 = the outer layer), `y` is absolute. Furniture written against a Wall works on all four walls."""

    def __init__(self, sr, side):
        self.sr, self.side = sr, side
        self.toward = TOWARD[side]       # compass direction pointing at the wall (the high side of a bench stair)
        self.away = INWARD[side]         # compass direction pointing into the room (where a sitter looks)

    def xz(self, lat, out=0):
        sr, s = self.sr, self.side
        d = WALL + out
        if s == "n":
            return lat, d
        if s == "s":
            return lat, sr.D - 1 - d
        if s == "w":
            return d, lat
        return sr.W - 1 - d, lat

    def at(self, lat, y, out):
        x, z = self.xz(lat, out)
        return x, y, z

    def put(self, lat, y, out, block, **props):
        """Places a block (skipping doorway holes) and returns its (x, y, z)."""
        x, z = self.xz(lat, out)
        self.sr.set_(x, y, z, block, **props)
        return x, y, z

    def clear(self, lat, y, out):
        x, z = self.xz(lat, out)
        self.sr.set_(x, y, z, AIR)
        return x, y, z

    def get(self, lat, y, out):
        x, z = self.xz(lat, out)
        return self.sr.t.get(x, y, z)

    def stairs(self, lat, y, out, block, high="wall", half="bottom", **extra):
        """A stair whose HIGH side is toward the wall (`high='wall'`) or into the room (`high='room'`)."""
        f = self.toward if high == "wall" else self.away
        p = {"facing": f, "half": half}
        p.update(extra)
        return self.put(lat, y, out, block, **p)

    def trapdoor(self, lat, y, out, block=TRAPDOOR, open_="true", half="bottom", face=None):
        """A trapdoor; the default `face` (toward the room) puts an open panel flat against the wall."""
        return self.put(lat, y, out, block, facing=face or self.away, half=half, open=open_)

    def attached(self, lat, y, out, block, **props):
        """A wall-mounted block (wall torch, wall banner, wall skull, ladder, wall sign ...) facing into the room."""
        return self.put(lat, y, out, block, facing=self.away, **props)

    def hint(self, lat, y, out, as_):
        """A compat `furniture` marker above the block at (lat, y, out)."""
        x, z = self.xz(lat, out)
        self.sr.hint(x, y, z, as_)

    def seat(self, lat, y, out, kind="chair", toward=False):
        """A seat on the cell (lat, y, out): the sitter looks into the room, or (`toward=True`) at the wall, as on a window seat."""
        x, z = self.xz(lat, out)
        self.sr.seat(x, y, z, self.toward if toward else self.away, kind)

    def barrel(self, lat, y, out, up=True, hint=False):
        x, yy, z = self.put(lat, y, out, "minecraft:barrel", facing="up" if up else self.away, open="false")
        if hint:
            self.sr.hint(x, y, z, "storage")

    def chest(self, lat, y, out, kind="single"):
        return self.put(lat, y, out, "minecraft:chest", facing=self.away, type=kind, waterlogged="false")


# =============================================================================================================================== architecture
def cornice(sr, block=BRICK_STAIRS):
    """A crown moulding under the ceiling: upside-down stairs along every wall at the top interior row, the high side toward the wall."""
    y = sr.top
    for side in "nsew":
        w = Wall(sr, side)
        lo, hi = WALL, (sr.W if side in "ns" else sr.D) - WALL
        for lat in range(lo, hi):
            w.stairs(lat, y, 0, block, high="wall", half="top")
    return sr


def plinth(sr, block=POLISHED_SLAB):
    """A low plinth (bottom slabs) along the foot of every wall, skipping the doorway approaches."""
    for side in "nsew":
        w = Wall(sr, side)
        lo, hi = WALL, (sr.W if side in "ns" else sr.D) - WALL
        for lat in range(lo, hi):
            if not _door_near(sr, side, lat):
                w.put(lat, sr.stand, 0, block, type="bottom")
    return sr


def _door_near(sr, side, lat):
    for s, off, _kw in sr.sockets:
        if s == side and off - 1 <= lat <= off + 1:
            return True
    return False


def pilaster(sr, side, lat, shaft=POLISHED, cap=CHISELED, y1=None):
    """A 1x1 pilaster against the inner face of `side`'s wall at `lat`: a shaft from the floor to the cornice with a base and a capital of the `cap`
    block. Reads as structure carrying the roof instead of a flat wall."""
    w = Wall(sr, side)
    y1 = sr.top - 1 if y1 is None else y1
    for y in range(sr.stand, y1 + 1):
        w.put(lat, y, 0, shaft)
    w.put(lat, sr.stand, 0, cap)
    w.put(lat, y1, 0, cap)
    return sr


def corner_pillars(sr, shaft=POLISHED, cap=CHISELED):
    """A pillar in each interior corner (1x1, floor to cornice)."""
    for x, z in ((WALL, WALL), (sr.W - WALL - 1, WALL), (WALL, sr.D - WALL - 1), (sr.W - WALL - 1, sr.D - WALL - 1)):
        for y in range(sr.stand, sr.top):
            sr.set_(x, y, z, shaft)
        sr.set_(x, sr.top - 1, z, cap)
    return sr


def window_slit(sr, side, lat, y0, height=3, glass="minecraft:orange_stained_glass", shutters=True, shutter=TRAPDOOR, sill=True, lintel=True):
    """A narrow recessed window in `side`'s wall: the inner wall layer is carved for `height` rows, the OUTER layer holds coloured glass (the shell
    stays closed and the slit glows), and open trapdoor shutters flank it. Why glass in the outer layer and not air: a Landing builds against its
    neighbours and the void, and the checker (and the owner) must be able to say 'no hole in the shell' without exceptions."""
    w = Wall(sr, side)
    for k in range(height):
        w.clear(lat, y0 + k, -1)
        x, z = w.xz(lat, -2)
        sr.set(x, y0 + k, z, glass)
    if sill:
        x, z = w.xz(lat, -1)
        sr.set(x, y0 - 1, z, POLISHED_SLAB, {"type": "top"})
    if lintel:
        x, z = w.xz(lat, -1)
        sr.set(x, y0 + height, z, POLISHED_SLAB, {"type": "bottom"})
    if shutters:
        for dl in (-1, 1):
            for k in range(height):
                w.trapdoor(lat + dl, y0 + k, 0, shutter)
    return sr


def niche(sr, side, lat, y0, width=3, height=3):
    """A recess in the inner wall layer (1 deep) with the outer layer as its back wall; returns the (x, z) cells of the recess floor."""
    w = Wall(sr, side)
    half = width // 2
    cells = []
    for dl in range(-half, half + 1):
        for k in range(height):
            w.clear(lat + dl, y0 + k, -1)
        cells.append(w.xz(lat + dl, -1))
    return cells


# =============================================================================================================================== light
def hang_lamp(sr, x, z, chain=3, lantern=LANTERN, top=None):
    """A lantern on a chain from the ceiling at (x, z)."""
    y = sr.top if top is None else top
    for k in range(chain):
        sr.set_(x, y - k, z, CHAIN_BLOCK, axis="y")
    sr.set_(x, y - chain, z, lantern, hanging="true")
    return sr


def chandelier(sr, x, z, drop=4, arm=1, lantern=LANTERN, top=None):
    """A chandelier: a chain from the ceiling (or from a beam: pass `top`, the highest chain row) to an iron-bar cross with a lantern hanging from the
    middle and from each arm end. Why not the `mega_lib` one: that one hangs its ring lanterns from chains that float; this one carries them on a bar
    cross, so it reads as an object."""
    top = sr.top if top is None else top
    yb = top - drop
    for y in range(yb + 1, top + 1):
        sr.set_(x, y, z, CHAIN_BLOCK, axis="y")
    sr.set_(x, yb, z, IRON_BARS)
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in range(1, arm + 1):
            sr.set_(x + dx * k, yb, z + dz * k, IRON_BARS)
        sr.set_(x + dx * arm, yb - 1, z + dz * arm, lantern, hanging="true")
    sr.set_(x, yb - 1, z, lantern, hanging="true")
    return sr


def hang_chandelier(sr, x, z, bar_y=12, arm=1, lantern=LANTERN):
    """A chandelier at (x, z) whose iron-bar cross is at row `bar_y` (its lanterns one row lower, so `bar_y` 12 keeps every lantern above head height of
    a person standing on the floor row 9). It hangs from a ceiling beam when one runs over that cell (the chain starts just under the beam) and straight
    from the ceiling otherwise. Why: a chain must always end against a block, and the builders place beams and chandeliers independently."""
    beam = sr.t.get(x, sr.top - 1, z)
    top = sr.top - 2 if beam is not None and beam.block.endswith("_log") else sr.top
    return chandelier(sr, x, z, drop=top - bar_y, arm=arm, lantern=lantern, top=top)


def raise_portcullis(sr):
    """A raised portcullis over each doorway: iron bars in the INNER wall layer in the two rows above the door's lintel (the outer layer stays solid
    behind them, so the shell is closed), a chiselled keystone above. Why: "a door you could imagine barring": a safe room should show that its doors can
    be shut; the bars sit above the 4-high doorway and its lintel row, never in it, so no route changes."""
    y = DOOR_ROW + DOOR_H + 1
    for side, off, _kw in sr.sockets:
        w = Wall(sr, side)
        for lat in range(off - 1, off + 2):
            for dy in (0, 1):
                x, z = w.xz(lat, -1)
                sr.set(x, y + dy, z, IRON_BARS)
        x, z = w.xz(off, -1)
        sr.set(x, y + 2, z, CHISELED)
    return sr


def column(sr, x, z, y1=None, shaft=POLISHED, cap=CHISELED):
    """A free-standing column from the floor to `y1` (default the ceiling row): a polished shaft with a chiselled base and a chiselled capital. Reads as
    structure carrying the roof; one block thin, so it never blocks a route."""
    y1 = sr.top if y1 is None else y1
    for y in range(sr.stand, y1 + 1):
        sr.set_(x, y, z, shaft)
    sr.set_(x, sr.stand, z, cap)
    sr.set_(x, y1 - 1, z, cap)
    return sr


def banner_on(sr, x, y, z, toward, color="red"):
    """A wall banner hanging on the face of the block at (x, y, z) that looks toward `toward` (a compass name); the banner takes the cell in front."""
    dx, dz = STEP[toward]
    sr.set_(x + dx, y, z + dz, "minecraft:%s_wall_banner" % color, facing=toward)
    return sr


def herbs(sr, cells):
    """Bundles of herbs drying from the ceiling beams: hanging roots one row under the beam, at each (x, z) where a beam runs over a free cell."""
    for (x, z) in cells:
        if sr.is_free(x, sr.top - 2, z) and sr.t.get(x, sr.top - 1, z) is not None:
            sr.set_(x, sr.top - 2, z, "minecraft:hanging_roots", waterlogged="false")
    return sr


def wall_lamp(sr, side, lat, y, lantern=LANTERN):
    """A lantern on a bracket: a fence post stuck to the wall and the lantern hanging below it."""
    w = Wall(sr, side)
    w.put(lat, y + 1, 0, FENCE, **{w.toward: "true"})
    w.put(lat, y, 0, lantern, hanging="true")
    return sr


def lamp_post(sr, x, y, z, height=2, lantern=LANTERN):
    """A floor lamp: a fence post `height` high with a lantern on top."""
    for k in range(height):
        sr.set_(x, y + k, z, FENCE)
    sr.set_(x, y + height, z, lantern, hanging="false")
    return sr


def brazier(sr, x, y, z, hint=True, facing="north"):
    """A standing brazier: a polished pedestal with a lit campfire on it (a `hearth` anchor when `hint`). Why a campfire on a pedestal: it is a real
    cooking fire the Farmer's Delight stove can replace, it lights 15, and the pedestal keeps it out of the walkway. No cage of iron bars round it: bars
    at fire height would hang in the air over empty floor, and a cage would also stand between the player and the block they have to click to rest."""
    sr.set_(x, y, z, POLISHED)
    if hint:
        sr.hearth(x, y + 1, z, facing=facing)
    else:
        sr.set_(x, y + 1, z, "minecraft:campfire", facing=facing, lit="true", signal_fire="false", waterlogged="false")
    return sr


# =============================================================================================================================== furniture
def table(sr, x, y, z, facing, length, width=1, wood=None, hint=True):
    """A rustic trestle table: a top of slabs (top half, so the top is exactly one block high) with chunky plank legs at both ends. (x,y,z) is the
    first cell, `length` cells along `facing`, `width` cells to the right of it. The middle slab carries a `furniture;as=table` marker when `hint`."""
    slab = wood_of(PLANKS_SLAB, wood) if wood else PLANKS_SLAB
    plank = wood_of(PLANKS, wood) if wood else PLANKS
    f = Frame(sr, x, y, z, facing)
    for k in range(length):
        for r in range(width):
            if k in (0, length - 1) and length > 2:
                sr.set_(*f.at(r, 0, k), plank)
            else:
                sr.set_(*f.at(r, 0, k), slab, type="top")
    if hint:
        mid = (length // 2) if length > 2 else 0
        cx, cy, cz = f.at(0, 0, mid)
        if sr.t.get(cx, cy, cz).block.endswith("_slab"):
            sr.hint(cx, cy, cz, "table")
    return sr


def counter(sr, x, y, z, facing, length, wood=None):
    """A waist-high counter: a row of plank blocks, one block high, so things (brewing stands, plates, lanterns) stand directly on top of it."""
    plank = wood_of(PLANKS, wood) if wood else PLANKS
    f = Frame(sr, x, y, z, facing)
    for k in range(length):
        sr.set_(*f.at(0, 0, k), plank)
    return sr


def stool_row(sr, x, y, z, facing, length, kind="sofa", wood=None):
    """A bench of `length` seats along the direction at right angles to `facing` (sitters look toward `facing`); every seat carries a marker."""
    f = Frame(sr, x, y, z, facing)
    stairs = wood_of(PLANKS_STAIRS, wood) if wood else PLANKS_STAIRS
    for k in range(length):
        cx, cy, cz = f.at(k, 0, 0)
        sr.seat(cx, cy, cz, facing, kind=kind, block=stairs)
    return sr


def barrels(sr, cells, facing="up", marker=False):
    """Barrels at the (x, y, z) cells; `marker` puts a `furniture;as=storage` hint above each (the compat mod's crates / drawers)."""
    for (x, y, z) in cells:
        sr.set_(x, y, z, "minecraft:barrel", facing=facing, open="false")
        if marker:
            sr.hint(x, y, z, "storage")
    return sr


def chest(sr, x, y, z, facing, loot=None, kind="single"):
    """A chest (empty storage for the party, or with a loot table); `facing` is the way its lid faces."""
    if loot:
        sr.t.chest(x, y, z, loot, facing=facing)
    else:
        sr.set(x, y, z, "minecraft:chest", {"facing": facing, "type": kind, "waterlogged": "false"})
    return sr


def double_chest(sr, x, y, z, along, facing):
    """A double chest: two single halves side by side along `along` ('x' or 'z'), both facing `facing`."""
    if along == "x":
        sr.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "left" if facing in ("north", "west") else "right", "waterlogged": "false"})
        sr.set(x + 1, y, z, "minecraft:chest", {"facing": facing, "type": "right" if facing in ("north", "west") else "left", "waterlogged": "false"})
    else:
        sr.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "left" if facing in ("east", "south") else "right", "waterlogged": "false"})
        sr.set(x, y, z + 1, "minecraft:chest", {"facing": facing, "type": "right" if facing in ("east", "south") else "left", "waterlogged": "false"})
    return sr


def rug(sr, x1, z1, x2, z2, colors, y=None):
    """A rug of concentric carpet rings: `colors` lists the ring colours from the outside in (the last fills the middle)."""
    y = sr.stand if y is None else y
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            ring = min(x - x1, x2 - x, z - z1, z2 - z)
            c = colors[min(ring, len(colors) - 1)]
            sr.set_(x, y, z, "minecraft:%s_carpet" % c)
    return sr


def banner(sr, side, lat, y, color="red"):
    """A wall banner hanging from (lat, y): it hangs about 1.7 blocks down, so keep the cell below it free."""
    Wall(sr, side).attached(lat, y, 0, "minecraft:%s_wall_banner" % color)
    return sr


def cobweb(sr, x, y, z):
    sr.set_(x, y, z, "minecraft:cobweb")
    return sr


def armor_stand(sr, x, y, z, yaw, head=None, chest=None, legs=None, feet=None, mainhand=None):
    sr.armor_stand(x, y, z, yaw=yaw, head=head, chest=chest, legs=legs, feet=feet, mainhand=mainhand)
    return sr


def sign(sr, x, y, z, facing, lines, block="minecraft:oak_wall_sign"):
    """A wall sign with the given (up to four) lines of plain text. Block entity data follows the 1.21.1 format (`front_text.messages` as JSON strings)."""
    def msg(t):
        return nbt.String('{"text":"%s"}' % t.replace('"', "'"))
    empty = nbt.String('{"text":""}')
    lines = list(lines) + [""] * (4 - len(lines))
    text = nbt.Compound({"messages": nbt.List([msg(t) if t else empty for t in lines[:4]], tag_type=nbt.TAG_STRING), "color": "black",
                         "has_glowing_text": nbt.Byte(0)})
    back = nbt.Compound({"messages": nbt.List([empty] * 4, tag_type=nbt.TAG_STRING), "color": "black", "has_glowing_text": nbt.Byte(0)})
    data = nbt.Compound({"id": "minecraft:sign", "is_waxed": nbt.Byte(1), "front_text": text, "back_text": back})
    sr.set(x, y, z, block, {"facing": facing}, data)
    return sr

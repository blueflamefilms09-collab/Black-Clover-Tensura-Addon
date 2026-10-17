#!/usr/bin/env python3
"""
The door caps of the Mega-Dungeon (1.54.0, docs/labyrinth/mega.md section 4, docs/labyrinth/mega_arch.md): the alcoves that end every doorway nothing
was attached to. Imported by `gen_mega_arch.py`, which writes them to `structure/mega/cap/*.nbt`.

Why caps exist: a doorway is a 3x4 hole through BOTH layers of a wall, and a room has a doorway at every module centre of every side. The assembler
attaches a room, a corridor or another piece to as many of them as it can; the rest would be holes into the void. A cap is a small plug that
continues the doorway outward and closes it: a bricked-up arch, a lantern niche, a statue, a cell. So a player sees "a door to a closet" where
there is no room behind the wall, and the tight dungeon never shows its emptiness.

Why every cap is a one-block-wide slot between two walls: a cap is exactly as wide as the doorway (3 cells, the checker enforces it). The only way to
leave no side of it open to the void is to wall both flanks inside those three cells, which leaves a slot one cell wide. From the room it reads as
a deep recess behind a framed doorway: the three-wide hole of the wall, then a narrow niche with its prop at the end.

Cap geometry, in cap-local cells (x across 0..2, y 0..5, z 0..depth-1): y = 0 is the floor row (it lines up with the room's floor row 8), y = 1..4 the
four open rows of the doorway (rows 9..12), y = 5 the roof row. z = 0 is the layer that touches the room's outer wall; the jigsaw is at (1,1,0) facing
north_up, so after the assembler turns the cap its first layer lies directly outside the wall and the cap grows away from the room. The jigsaw becomes its
`final_state` when the cap is placed: air for an open alcove, a plain full block for a bricked-up one. The state is NOT rotated with the piece (the
vanilla jigsaw replacement does not rotate it), so it must never be a block with a facing.
"""
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from template import Template  # noqa: E402
import mega_arch_kit as K  # noqa: E402


class CapT:
    """A door cap under construction. `mega_lib.MT` only builds whole modules, a cap is a 3 x 6 x depth plug, so it gets its own small builder with the
    same safety net: `set` checks the block state and the custom-block rule, `finish` writes the jigsaw and fills in the connection states."""

    armor_stand = L.MT.armor_stand
    entity = L.MT.entity

    def __init__(self, name, depth):
        if not 1 <= depth <= 8:
            raise ValueError("a door cap is 1..8 deep, got %d" % depth)
        self.name = name
        self.depth = depth
        self.t = Template(name, 3, 6, depth)
        self.rng = random.Random("nusmp:mega:arch:" + name)

    def set(self, x, y, z, block, props=None, nbt_=None):
        if not (0 <= x < 3 and 0 <= y < 6 and 0 <= z < self.depth):
            raise ValueError("%s: (%d,%d,%d) is outside the 3x6x%d cap" % (self.name, x, y, z, self.depth))
        if ":" not in block:
            block = "minecraft:" + block
        err = L.check_state(block, props)
        if err:
            raise ValueError("%s: bad block state at (%d,%d,%d): %s" % (self.name, x, y, z, err))
        if block.startswith("nusmp:") and block not in L.CUSTOM:
            raise ValueError("%s: %s is not allowed (Vanilla Block Primacy Rule)" % (self.name, block))
        self.t.set(x, y, z, block, props, nbt_)
        return self

    def get(self, x, y, z):
        return self.t.get(x, y, z)

    def remove(self, x, y, z):
        self.t.remove(x, y, z)

    def shell(self, wear=0.12):
        """Floor, roof, both side walls and the back wall in palette stone; the slot (x = 1, rows 1..4, z < depth - 1) stays empty."""
        d = self.depth
        for z in range(d):
            for x in range(3):
                self.set(x, 0, z, L.TILE if (x + z) % 5 else L.POLISHED)
                self.set(x, 5, z, L.TILE)
            for y in range(1, 5):
                for x in (0, 2):
                    self.set(x, y, z, L.CRACKED_BRICK if self.rng.random() < wear else L.BRICK)
        for y in range(1, 5):
            for x in range(3):
                self.set(x, y, d - 1, L.BRICK)
        return self

    def back_band(self):
        """A chiselled band across the top of the back wall so a plain end does not look blank."""
        for x in range(3):
            self.set(x, 4, self.depth - 1, L.CHISELED if x == 1 else L.POLISHED)
        return self

    def finish(self, final_state=L.AIR):
        """Writes the jigsaw at (1,1,0) facing north_up with `final_state`, then fills in pane, bar and stair connection states."""
        self.t.cells.pop((1, 1, 0), None)
        self.t.jigsaw(1, 1, 0, "north_up", L.DOOR, L.DOOR, L.P_EMPTY, final_state=final_state, joint="aligned")
        L.autoconnect(self.t)
        return self

    def save(self, root=L.RES):
        return self.t.save(root)


BARS_EW = {"north": "false", "east": "true", "south": "false", "west": "true", "waterlogged": "false"}   # bars or a pane joined to both side walls
ROD_UP = {"facing": "up", "powered": "false", "waterlogged": "false"}


def plain_wall(variant):
    """The doorway bricked up: a 2-deep plug of the wall's own material. `a` is clean masonry with a few cracks, `b` a hasty infill of rubble."""
    c = CapT("mega/cap/plain_wall_%s" % variant, 2)
    for z in range(2):
        for x in range(3):
            c.set(x, 0, z, L.TILE)
            c.set(x, 5, z, L.TILE)
            for y in range(1, 5):
                if variant == "a":
                    c.set(x, y, z, L.CRACKED_BRICK if c.rng.random() < 0.18 else L.BRICK)
                elif z == 0:
                    c.set(x, y, z, c.rng.choice((L.COBBLED, L.COBBLED, L.CRACKED_BRICK, L.BRICK)))
                else:
                    c.set(x, y, z, L.BRICK)
    if variant == "b":
        c.set(0, 1, 0, L.COBBLED_STAIRS, {"facing": "east", "half": "bottom"})
        c.set(2, 1, 0, L.COBBLED_STAIRS, {"facing": "west", "half": "bottom"})
        c.set(1, 4, 0, L.COBBLED_SLAB, {"type": "top"})
    return c.finish(L.BRICK)


def lantern(variant):
    """A shallow niche with a light: `a` a hanging lantern and a red banner on the back wall, `b` a standing lantern on a shelf and a soul lantern above."""
    c = CapT("mega/cap/alcove_lantern_%s" % variant, 3).shell().back_band()
    if variant == "a":
        c.set(1, 4, 1, L.LANTERN, {"hanging": "true"})
        c.set(1, 3, 1, "minecraft:red_wall_banner", {"facing": "north"})
    else:
        c.set(1, 2, 1, L.POLISHED_SLAB, {"type": "top"})
        c.set(1, 3, 1, L.LANTERN, {"hanging": "false"})
        c.set(1, 4, 0, L.SOUL_LANTERN, {"hanging": "true"})
    return c.finish()


def chest():
    """A closet with a loot chest (the trial table: iron, gold, arrows, bread, experience, magic stones) under a hanging lantern, a cobweb in the corner."""
    c = CapT("mega/cap/alcove_chest", 3).shell().back_band()
    c.t.chest(1, 1, 1, "nusmp:chests/labyrinth_trial", facing="north")
    c.set(1, 4, 0, L.LANTERN, {"hanging": "true"})
    c.set(1, 4, 1, "minecraft:cobweb")
    return c.finish()


def statue(variant):
    """An armoured statue on a plinth looking out of its niche: `a` iron under a lantern, `b` a golden knight in front of a black banner."""
    c = CapT("mega/cap/alcove_statue_%s" % variant, 3 if variant == "a" else 4).shell().back_band()
    K.statue(c, 1, 1, 1, "north", kit="iron" if variant == "a" else "golden")
    if variant == "a":
        c.set(1, 4, 0, L.LANTERN, {"hanging": "true"})
    else:
        c.set(1, 3, 2, "minecraft:black_wall_banner", {"facing": "north"})
        c.set(1, 4, 0, L.SOUL_LANTERN, {"hanging": "true"})
    return c.finish()


def bookcase():
    """A reading nook: a lectern with a book in front of a wall of bookshelves."""
    c = CapT("mega/cap/alcove_bookcase", 3).shell()
    for x in range(3):
        for y in range(1, 4):
            c.set(x, y, 2, "minecraft:bookshelf")
        c.set(x, 4, 2, L.POLISHED)
    K.lectern(c, 1, 1, 1, "north")
    c.set(1, 4, 0, L.LANTERN, {"hanging": "true"})
    return c.finish()


def cell():
    """A prisoner's cell: a grille of iron bars one block into the alcove, behind it a skull on a bone block and a chain from the roof, a soul lantern in front."""
    c = CapT("mega/cap/alcove_cell", 5).shell().back_band()
    for y in range(1, 5):
        c.set(1, y, 1, L.IRON_BARS, BARS_EW)
    c.set(1, 1, 2, "minecraft:bone_block", {"axis": "z"})
    c.set(1, 2, 2, "minecraft:skeleton_skull", {"rotation": "0"})
    for y in (4, 3):
        c.set(1, y, 3, L.CHAIN_BLOCK, {"axis": "y"})
    c.set(1, 4, 0, L.SOUL_LANTERN, {"hanging": "true"})
    return c.finish()


def candles(variant):
    """A shrine ledge: a top slab carrying a cluster of lit candles (the slab is a TOP slab so the candle's cell sits flush on it), a purple banner or a
    hanging soul lantern above."""
    c = CapT("mega/cap/alcove_candles_%s" % variant, 3).shell().back_band()
    c.set(1, 1, 1, L.POLISHED_SLAB, {"type": "top"})
    c.set(1, 2, 1, "minecraft:candle" if variant == "a" else "minecraft:red_candle", {"candles": "4", "lit": "true"})
    if variant == "a":
        c.set(1, 4, 1, "minecraft:purple_wall_banner", {"facing": "north"})
    else:
        c.set(1, 4, 1, "minecraft:soul_lantern", {"hanging": "true"})
    return c.finish()


def banner(variant):
    """A hanging banner on the back wall of a shallow niche, a candle at its foot."""
    c = CapT("mega/cap/alcove_banner_%s" % variant, 3).shell().back_band()
    c.set(1, 4, 1, "minecraft:%s_wall_banner" % ("red" if variant == "a" else "black"), {"facing": "north"})
    c.set(1, 1, 1, "minecraft:candle", {"candles": "3", "lit": "true"})
    return c.finish()


def skulls():
    """Two shelves of skulls on the back wall and a soul lantern."""
    c = CapT("mega/cap/alcove_skulls", 3).shell().back_band()
    c.set(1, 1, 1, L.POLISHED_SLAB, {"type": "top"})
    c.set(1, 2, 1, "minecraft:skeleton_skull", {"rotation": "0"})
    c.set(1, 3, 1, L.POLISHED_SLAB, {"type": "top"})
    c.set(1, 4, 1, "minecraft:wither_skeleton_skull", {"rotation": "0"})
    c.set(1, 4, 0, L.SOUL_LANTERN, {"hanging": "true"})
    return c.finish()


def brazier():
    """A fire basket at the end of the alcove: a campfire under an iron-bar lid on a polished foot, a chain from the roof to the lid."""
    c = CapT("mega/cap/alcove_brazier", 3).shell().back_band()
    c.set(1, 0, 1, L.POLISHED)
    c.set(1, 1, 1, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    c.set(1, 2, 1, L.IRON_BARS, BARS_EW)
    for y in (4, 3):
        c.set(1, y, 1, L.CHAIN_BLOCK, {"axis": "y"})
    return c.finish()


def window(variant):
    """A window that glows: a stained-glass pane in front of a lamp (a froglight in the back wall) with a light block between, so the alcove shows a
    coloured panel of light where there is no outside. `a` purple, `b` cyan."""
    c = CapT("mega/cap/alcove_window_%s" % variant, 4).shell()
    pane = "minecraft:%s_stained_glass_pane" % ("purple" if variant == "a" else "cyan")
    c.set(1, 1, 1, L.POLISHED)
    c.set(1, 4, 1, L.CHISELED)
    for y in (2, 3):
        c.set(1, y, 1, pane, BARS_EW)
        c.set(1, y, 2, "minecraft:light", {"level": "12"})
        c.set(1, y, 3, "minecraft:pearlescent_froglight" if variant == "a" else "minecraft:verdant_froglight", {"axis": "y"})
    return c.finish()


def snake():
    """A miniature snake portal: a frame of waxed oxidised copper, a purple glass membrane in front of a glowing froglight, a blackstone head with an
    end-rod tongue on top and a lightning-rod fin at the foot (the lead's `mega_lib.snake_portal` built to fit the one-block-wide slot)."""
    c = CapT("mega/cap/alcove_snake", 4).shell()
    for y in range(1, 5):
        for x in (0, 2):
            c.set(x, y, 2, "minecraft:waxed_oxidized_cut_copper")
    for y in (1, 2, 3):
        c.set(1, y, 2, "minecraft:purple_stained_glass_pane", BARS_EW)
        c.set(1, y, 3, "minecraft:pearlescent_froglight", {"axis": "y"})
    c.set(1, 4, 2, "minecraft:polished_blackstone")
    c.set(1, 4, 3, L.CHISELED)
    c.set(1, 4, 1, "minecraft:end_rod", {"facing": "north"})
    c.set(1, 1, 1, "minecraft:lightning_rod", ROD_UP)
    return c.finish()


def barrels():
    """Stacked barrels with a lantern on top: a storeroom nook."""
    c = CapT("mega/cap/alcove_barrels", 3).shell().back_band()
    c.set(1, 1, 1, "minecraft:barrel", {"facing": "north", "open": "false"})
    c.set(1, 2, 1, "minecraft:barrel", {"facing": "up", "open": "false"})
    c.set(1, 3, 1, L.LANTERN, {"hanging": "false"})
    return c.finish()


def bones():
    """A heap of bones with a skull and cobwebs: someone got this far."""
    c = CapT("mega/cap/alcove_bones", 3).shell().back_band()
    c.set(1, 1, 1, "minecraft:bone_block", {"axis": "x"})
    c.set(1, 2, 1, "minecraft:skeleton_skull", {"rotation": "0"})
    c.set(1, 4, 1, "minecraft:cobweb")
    c.set(1, 4, 0, "minecraft:cobweb")
    return c.finish()


def cobweb():
    """Choked with cobwebs around a lone skull."""
    c = CapT("mega/cap/alcove_cobweb", 3).shell(wear=0.3).back_band()
    for y in (2, 3, 4):
        c.set(1, y, 1, "minecraft:cobweb")
    c.set(1, 1, 1, "minecraft:wither_skeleton_skull", {"rotation": "0"})
    c.set(1, 4, 0, "minecraft:cobweb")
    return c.finish()


def rack():
    """A weapon rack: an armour stand in a helmet and chain mail holding an axe, a lantern over its head."""
    c = CapT("mega/cap/alcove_rack", 3).shell().back_band()
    c.armor_stand(1, 1, 1, yaw=180.0, head="minecraft:iron_helmet", chest="minecraft:chainmail_chestplate", mainhand="minecraft:iron_axe")
    c.set(1, 4, 0, L.LANTERN, {"hanging": "true"})
    return c.finish()


def build_caps():
    """Every cap of this builder, in a fixed order (the order only matters for the listing)."""
    return [plain_wall("a"), plain_wall("b"), lantern("a"), lantern("b"), chest(), statue("a"), statue("b"), bookcase(), cell(), candles("a"), candles("b"),
            banner("a"), banner("b"), skulls(), brazier(), window("a"), window("b"), snake(), barrels(), bones(), cobweb(), rack()]

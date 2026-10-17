#!/usr/bin/env python3
"""
Static "does it hold up in the game" checks for the circulation templates, the door caps and the Gateway (1.54.0). Run by `gen_mega_arch.py` and
`gen_mega_gateway.py` on every piece before anything is written, so a piece with a defect cannot reach the data folder.

    python3 tools/labyrinth/mega_arch_check.py                      # every template of this builder (mega/** and mega_gateway/**)
    python3 tools/labyrinth/mega_arch_check.py cap/ bend_1x1        # only the templates whose name contains one of the arguments

Why this exists next to `mega_check.py` (the connector contract) and the light check: a flat or even a shape-aware preview cannot tell a lantern that
hangs from a chain from a lantern that hangs from the air one block under the ceiling, or a torch on a wall from a torch in front of a niche. In the game
both look wrong at once (a chain that ends in mid-air, a flame that floats) and an attached block without support also pops off as soon as a neighbour
changes. The generator writes every block of a structure with the "no neighbour updates" flags, so the game itself never checks them while the structure
is placed; this module applies the same rules the game's `canSurvive` methods would, up front:

  * a standing lantern, candle or torch needs a top face under it that covers the middle of the cell (a full block, a TOP slab, an upside-down stair, a
    wall or fence post, bars, a chain); a bottom slab, a bottom stair, a cauldron or a chest leaves a gap or cannot hold it;
  * a hanging lantern needs a chain or a block above whose underside covers the middle of the cell; a vertical chain needs no support to survive, but
    its run must reach a block (an upside-down stair counts) and not end in the air: a chain that ends in mid-air is a floating chain;
  * a wall torch needs a solid block behind it; a wall banner needs a solid block behind it;
  * a carpet or a moss carpet needs a block under it; a vine needs a block on the face it grows from;
  * a gravity block (sand, gravel, anvil, concrete powder) needs a solid block under it, or it falls the moment the chunk loads;
  * a water source must be shut in on its four sides and below (a source next to air spreads as soon as the game ticks it);
  * an armour stand statue must stand on something: on a solid block when its feet are at the cell's floor, on a bottom slab when it is raised half a
    block, and the cell it stands in must be free;
  * bedrock is only ever a hidden core: every face of a bedrock block touches a solid block, so none of it can be seen from a slit, a window or a bite;
  * no block floats: every full block, stair, slab, wall or fence touches at least one neighbour.

Only rules the game applies (or that are plainly visible) are checked; what is not here is not proven fine. The face-coverage tables are simplified
from the shapes in the game (a stair counts as covering the whole top face only when it is upside-down, a wall only with its post) and are written from
memory of the vanilla shapes, since nothing here can load the game.
"""
import glob
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from template import load  # noqa: E402

DIRV = {"north": (0, 0, -1), "south": (0, 0, 1), "east": (1, 0, 0), "west": (-1, 0, 0)}

# blocks that are never a solid support for something attached to them
_PARTIAL_SUFFIX = ("_slab", "_stairs", "_trapdoor", "_carpet", "_button", "_pressure_plate", "_sign", "_wall_sign", "_banner", "_wall_banner", "_skull",
                   "_head", "_candle", "_door", "_fence_gate", "_bed", "_pane", "_bars", "_fence", "_wall", "_torch", "_wall_torch", "_cauldron")
_PARTIAL = {"air", "light", "lantern", "soul_lantern", "chain", "campfire", "soul_campfire", "cobweb", "end_rod", "lightning_rod", "candle", "iron_bars",
            "chest", "trapped_chest", "ender_chest", "lectern", "brewing_stand", "cauldron", "anvil", "chipped_anvil", "damaged_anvil", "grindstone", "vine",
            "moss_carpet", "structure_block", "jigsaw", "flower_pot", "torch", "wall_torch", "soul_torch", "soul_wall_torch", "ladder", "enchanting_table",
            "bell", "water", "lava", "snow", "scaffolding", "sea_pickle", "turtle_egg", "cake", "composter", "hopper", "dragon_egg", "conduit",
            "decorated_pot", "stonecutter", "lantern_x"}
_GRAVITY = ("sand", "gravel", "anvil", "concrete_powder", "dragon_egg")
_FLOAT_OK_SUFFIX = ("_banner", "_wall_banner", "_skull", "_head", "_torch", "_wall_torch", "_candle", "_pane", "_bars")
_FLOAT_OK = {"air", "light", "structure_block", "jigsaw", "cobweb", "chain", "lantern", "soul_lantern", "end_rod", "lightning_rod", "vine", "moss_carpet",
             "candle", "water", "iron_bars", "campfire", "soul_campfire"}


def path_of(block):
    return block.split(":", 1)[1] if ":" in block else block


def is_solid_cube(cell):
    """A full, opaque-enough block (for 'something is attached to it' and 'does it float')."""
    if cell is None:
        return False
    p = path_of(cell.block)
    if p in _PARTIAL or p.endswith(_PARTIAL_SUFFIX) or p.startswith("potted_"):
        return False
    return True


def covers_top(cell):
    """Does the TOP face of this cell cover the middle (what a standing lantern, candle or torch rests on)?"""
    if cell is None:
        return False
    p, pr = path_of(cell.block), cell.props
    if p.endswith("_slab"):
        return pr.get("type") in ("top", "double")
    if p.endswith("_stairs"):
        return pr.get("half") == "top"
    if p.endswith(("_wall", "_fence")) or p in ("iron_bars", "chain", "end_rod"):
        return True
    if p.endswith("_pane") or p.endswith("_trapdoor"):
        return p.endswith("_pane")
    return is_solid_cube(cell)


def covers_bottom(cell):
    """Does the BOTTOM face of this cell cover the middle (what a hanging lantern hangs from)?"""
    if cell is None:
        return False
    p, pr = path_of(cell.block), cell.props
    if p.endswith("_slab"):
        return pr.get("type") in ("bottom", "double")
    if p.endswith("_stairs"):
        return pr.get("half") == "bottom"
    if p.endswith(("_wall", "_fence")) or p in ("iron_bars", "end_rod"):
        return True
    if p == "chain":
        return pr.get("axis", "y") == "y"
    if p.endswith("_pane"):
        return True
    return is_solid_cube(cell)


def touches_bottom(cell):
    """Does something of this cell reach down to the cell's bottom plane (where the top end of a chain meets it)? Unlike `covers_bottom` this also accepts an
    upside-down stair, whose lower step covers only half of the face: a chain needs no support to survive, it only must not end in a visible gap."""
    if cell is None:
        return False
    p, pr = path_of(cell.block), cell.props
    if p.endswith("_slab"):
        return pr.get("type") in ("bottom", "double")
    if p.endswith("_stairs"):
        return True
    if p.endswith("_trapdoor"):
        return pr.get("half") == "bottom"
    return covers_bottom(cell)


def problems(t, name):
    """The list of defects of one template (empty when it holds up)."""
    out = []
    cells = t.cells

    def at(x, y, z):
        return cells.get((x, y, z))

    def bad(pos, text):
        out.append("%s (%d,%d,%d): %s" % (name, pos[0], pos[1], pos[2], text))

    for (x, y, z), c in sorted(cells.items()):
        p, pr = path_of(c.block), c.props
        below, above = at(x, y - 1, z), at(x, y + 1, z)
        if p in ("lantern", "soul_lantern"):
            if pr.get("hanging") == "true":
                if not covers_bottom(above):
                    bad((x, y, z), "hanging %s with nothing to hang from (above: %s)" % (p, above.block if above else "air"))
            elif not covers_top(below):
                bad((x, y, z), "standing %s on %s %s" % (p, below.block if below else "air", dict(below.props) if below else ""))
        elif p == "torch" or p == "soul_torch" or p.endswith("candle"):
            if not covers_top(below):
                bad((x, y, z), "%s on %s %s" % (p, below.block if below else "air", dict(below.props) if below else ""))
        elif (p.endswith("_skull") or p.endswith("_head")) and "wall" not in p:
            if not covers_top(below):
                bad((x, y, z), "floor %s on %s %s (it would float or hang over a gap)" % (p, below.block if below else "air", dict(below.props) if below else ""))
        elif p in ("wall_torch", "soul_wall_torch") or p.endswith("_wall_banner"):
            d = DIRV[pr["facing"]]
            if not is_solid_cube(at(x - d[0], y, z - d[2])):
                bad((x, y, z), "%s with no solid block behind it" % p)
        elif p.endswith("_wall_skull") or p.endswith("_wall_head"):
            d = DIRV[pr["facing"]]
            if at(x - d[0], y, z - d[2]) is None:
                bad((x, y, z), "%s with nothing behind it" % p)
        elif p.endswith("_carpet"):
            if below is None:
                bad((x, y, z), "%s over air" % p)
        elif p == "vine":
            ok = pr.get("up") == "true" and is_solid_cube(above)
            for f in DIRV:
                if pr.get(f) == "true":
                    d = DIRV[f]
                    ok = ok or is_solid_cube(at(x + d[0], y, z + d[2]))
            if not ok:
                bad((x, y, z), "vine with nothing to grow on")
        elif p == "chain" and pr.get("axis", "y") == "y":
            yy = y
            while True:
                nxt = at(x, yy + 1, z)
                if nxt is not None and path_of(nxt.block) == "chain" and nxt.props.get("axis", "y") == "y":
                    yy += 1
                    continue
                break
            if not touches_bottom(at(x, yy + 1, z)):
                bad((x, y, z), "chain that ends in the air (the run's top is row %d)" % yy)
        elif p == "water":
            for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, -1, 0)):
                nb = at(x + dx, y + dy, z + dz)
                if nb is None or (path_of(nb.block) not in ("water",) and not is_solid_cube(nb)):
                    bad((x, y, z), "water source open to the %s side (it would flow)" % ("lower" if dy else "%d,%d" % (dx, dz)))
                    break
        elif p == "bedrock":
            for (dx, dy, dz) in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                nb = at(x + dx, y + dy, z + dz)
                if not is_solid_cube(nb) and not (nb is not None and path_of(nb.block) in ("reinforced_deepslate", "bedrock")):
                    bad((x, y, z), "bedrock with an exposed face (it is meant to be a hidden core): neighbour %s" % (nb.block if nb else "air"))
                    break
        if any(g in p for g in _GRAVITY):
            if not covers_top(below) or below is None:
                bad((x, y, z), "gravity block %s over %s" % (p, below.block if below else "air"))
        # a block that touches nothing at all floats
        if p not in _FLOAT_OK and not p.endswith(_FLOAT_OK_SUFFIX):
            if all(at(x + dx, y + dy, z + dz) is None for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
                bad((x, y, z), "%s floats (no neighbour)" % p)
    for e in t.entities:
        n = e["nbt"]
        if str(n["id"]) != "minecraft:armor_stand":
            continue
        px, py, pz = (float(v) for v in e["pos"])
        bp = [int(v) for v in e["blockPos"]]
        cy = math.floor(py + 1e-6)
        frac = round(py - cy, 3)
        # the stand's feet are at py; a position exactly on a block corner (the middle of a 2x2 plinth) rests on any of the cells that meet there
        xs = {math.floor(px)} | ({math.floor(px) - 1} if px == math.floor(px) else set())
        zs = {math.floor(pz)} | ({math.floor(pz) - 1} if pz == math.floor(pz) else set())
        if frac == 0.0:
            if any(at(x, cy, z) is not None for x in xs for z in zs):
                bad(bp, "armour stand standing inside a block (feet at y=%s)" % py)
            if not any(covers_top(at(x, cy - 1, z)) for x in xs for z in zs):
                bad(bp, "armour stand with nothing under its feet (y=%s)" % py)
        elif frac == 0.5:
            if not any(at(x, cy, z) is not None and path_of(at(x, cy, z).block).endswith("_slab") and at(x, cy, z).props.get("type") == "bottom"
                       for x in xs for z in zs):
                bad(bp, "armour stand raised half a block but no bottom slab is under it (y=%s)" % py)
        else:
            bad(bp, "armour stand at an odd height %s" % py)
    return out


def main(argv):
    root = os.path.join(L.RES, "data", "nusmp", "structure")
    files = sorted(glob.glob(os.path.join(root, "mega", "**", "*.nbt"), recursive=True) + glob.glob(os.path.join(root, "mega_gateway", "*.nbt")))
    total = 0
    n = 0
    for f in files:
        name = os.path.relpath(f, root).replace(os.sep, "/")[:-4]
        if argv and not any(a in name for a in argv):
            continue
        n += 1
        for line in problems(load(f, name), name):
            total += 1
            print(line)
    print("%d template(s) checked, %d problem(s)" % (n, total))
    return 1 if total else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

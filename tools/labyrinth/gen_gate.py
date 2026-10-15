#!/usr/bin/env python3
"""
The two Dungeon Gate pieces of the Labyrinth (pool nusmp:gate/base), generated with the template DSL (docs/labyrinth/a1_foundation.md):

    gate/ruin_a  a Black Clover ruined stone arch on a weathered flagstone platform: broken pillars, moss, vines, chains, lanterns,
                 and inside the arch the 3x4 plane of nusmp:labyrinth_gate framed by crying obsidian and labyrinth glyphs.
    gate/ruin_b  a Solo Leveling style gate: a 7x8 ring of polished blackstone and labyrinth pillars floating one block above a cracked
                 circular dais, the gate plane inside the ring, four obelisks with glyph caps at the corners.

Both are START pieces (no jigsaw blocks: the gate is one piece), rigid projection, so template row y=0 lands on the terrain's surface
block (WORLD_SURFACE_WG + the rigid ground-level delta of 1). Rows y=0..1 are foundation, y=2 is the walkable platform; beard_thin
pulls the terrain up to the bottom of the box on slopes, and the two foundation rows hide the seam.
Deterministic: the same seed gives the same bytes.

    python3 tools/labyrinth/gen_gate.py            # writes src/main/resources/data/nusmp/structure/gate/ruin_{a,b}.nbt
    python3 tools/labyrinth/gen_gate.py --preview  # also prints the ASCII layers
"""
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from template import Template, resources_root  # noqa: E402

BRICKS = "nusmp:labyrinth_bricks"
TILES = "nusmp:labyrinth_tiles"
PILLAR = "nusmp:labyrinth_pillar"
GLYPH = "nusmp:labyrinth_glyph"
LANTERN = "nusmp:labyrinth_lantern"
GATE = "nusmp:labyrinth_gate"      # builder C's portal block: unbreakable, animated; placed by id only
OBSIDIAN = "minecraft:crying_obsidian"

W = 15      # template width and depth; the platform is the 13x13 inside it (x,z 1..13), the outer ring is the crumbling step
C = 7       # centre column
Y_FOUND = (0, 1)
Y_PLAT = 2


def pick(rng, table):
    """Weighted choice from [(weight, value), ...]."""
    total = sum(w for w, _ in table)
    r = rng.random() * total
    for w, v in table:
        r -= w
        if r <= 0:
            return v
    return table[-1][1]


def platform(t, rng, flag_table, found_table, circular=False):
    """Foundation rows + the flagstone top, with weathering gaps on the rim (unset cells: the terrain shows)."""
    for x in range(1, W - 1):
        for z in range(1, W - 1):
            dx, dz = x - C, z - C
            if circular and dx * dx + dz * dz > 6.5 * 6.5:
                continue
            rim = x in (1, W - 2) or z in (1, W - 2) or (circular and dx * dx + dz * dz > 5.5 * 5.5)
            for y in Y_FOUND:
                t.set(x, y, z, pick(rng, found_table))
            if rim and rng.random() < 0.22:
                continue  # a missing flagstone on the rim: the terrain pokes through
            t.set(x, Y_PLAT, z, pick(rng, flag_table))


def step_ring(t, rng, stair, slab, found):
    """A crumbling one-block step around the platform (y=1) so the platform is climbed in two hops instead of a two-block wall."""
    for i in range(W):
        for x, z, facing in ((i, 0, "south"), (i, W - 1, "north"), (0, i, "east"), (W - 1, i, "west")):
            if x in (0, W - 1) and z in (0, W - 1):
                continue  # the corners stay open
            r = rng.random()
            if r < 0.35:
                continue
            t.set(x, 0, z, found)
            if r < 0.6:
                t.set(x, 1, z, slab, {"type": "bottom", "waterlogged": "false"})
            else:
                # the high half of the stair is toward the platform, so `facing` is the direction the step climbs
                t.set(x, 1, z, stair, {"facing": facing, "half": "bottom", "shape": "straight", "waterlogged": "false"})


def gate_plane(t, x0, y0, z, with_frame=True):
    """The 3 wide x 4 tall plane of gate blocks at x0..x0+2, y0..y0+3, framed by crying obsidian and glyphs (x0-1, x0+3, y0+4)."""
    for y in range(y0, y0 + 4):
        for x in range(x0, x0 + 3):
            t.set(x, y, z, GATE)
    if not with_frame:
        return
    for y in range(y0, y0 + 5):
        for x in (x0 - 1, x0 + 3):
            t.set(x, y, z, GLYPH if y in (y0 + 1, y0 + 3) else OBSIDIAN)
    for x in range(x0, x0 + 3):
        t.set(x, y0 + 4, z, GLYPH if x == x0 + 1 else OBSIDIAN)


def ruin_a():
    rng = random.Random(0xA1A)
    t = Template("gate/ruin_a", W, 13, W)
    flags = [(40, "minecraft:stone_bricks"), (22, "minecraft:cobblestone"), (12, "minecraft:mossy_stone_bricks"),
             (10, "minecraft:cracked_stone_bricks"), (16, TILES)]
    found = [(55, "minecraft:cobblestone"), (30, "minecraft:stone_bricks"), (15, "minecraft:mossy_cobblestone")]
    platform(t, rng, flags, found)
    step_ring(t, rng, "minecraft:stone_brick_stairs", "minecraft:stone_brick_slab", "minecraft:cobblestone")
    # the paved way through the arch: labyrinth tiles down the centre line
    for z in range(1, W - 1):
        for x in (C - 1, C, C + 1):
            t.set(x, Y_PLAT, z, TILES if rng.random() < 0.8 else "minecraft:cracked_stone_bricks")

    # --- the arch: 9 wide (x 3..11), 9 tall (y 3..11), 5 deep (z 5..9); pillars 2 thick, opening x 5..9 ---
    y0, top = 3, 11
    for px in (3, 10):
        for x in (px, px + 1):
            for z in range(5, 10):
                for y in range(y0, 10):
                    corner = (x in (3, 11)) and (z in (5, 9))
                    t.set(x, y, z, PILLAR if corner else BRICKS, {"axis": "y"} if corner else None)
    # corbels: the opening narrows under the lintel
    for z in range(5, 10):
        for x in (5, 9):
            t.set(x, 9, z, BRICKS)
    # the lintel and the crown
    for z in range(5, 10):
        for x in range(4, 11):
            t.set(x, 10, z, BRICKS)
        for x in range(5, 10):
            t.set(x, 11, z, BRICKS if x != C else GLYPH)
    t.set(C, 12, 7, GLYPH)          # the keystone sigil
    t.set(C - 1, 12, 7, BRICKS)
    t.set(C + 1, 12, 7, BRICKS)
    # ruin it: bites out of the crown and the west pillar's outer face
    for (x, y, z) in ((4, 10, 5), (10, 10, 9), (5, 11, 5), (9, 11, 9), (6, 11, 9), (8, 11, 5), (3, 8, 5), (3, 9, 6), (3, 9, 5), (11, 9, 9)):
        t.remove(x, y, z)
    for (x, y, z) in ((4, 10, 9), (10, 10, 5), (3, 7, 5)):
        t.set(x, y, z, "minecraft:cobblestone_wall", {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    # lanterns set into the pillar faces on both sides of the way
    for (x, z) in ((4, 5), (10, 5), (4, 9), (10, 9)):
        t.set(x, 7, z, LANTERN)
    # the gate plane in the middle of the opening, standing on the platform
    gate_plane(t, C - 1, y0, 7)
    # chains hanging from the lintel to the frame
    for x in (5, 9):
        for y in (8, 9):
            t.set(x, y, 7, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    for x in (6, 8):
        t.set(x, 9, 6, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
        t.set(x, 9, 8, "minecraft:chain", {"axis": "y", "waterlogged": "false"})

    # --- broken pillars at the platform corners ---
    for (x, z, h) in ((2, 2, 4), (12, 2, 2), (2, 12, 3), (12, 12, 5)):
        t.set(x, Y_PLAT, z, "minecraft:stone_bricks")
        for y in range(y0, y0 + h):
            t.set(x, y, z, PILLAR, {"axis": "y"})
        t.set(x, y0 + h, z, "minecraft:mossy_cobblestone" if h % 2 else "minecraft:cobblestone_wall",
              None if h % 2 else {"up": "true", "north": "none", "south": "none", "east": "none", "west": "none", "waterlogged": "false"})
    # fallen pillar segments lying on the platform
    for x in range(8, 12):
        t.set(x, y0, 11, PILLAR, {"axis": "x"})
    for z in range(2, 5):
        t.set(2, y0, z + 4, PILLAR, {"axis": "z"})

    # --- overgrowth: moss, carpet, grass, vines on the arch's outer faces ---
    for x in range(1, W - 1):
        for z in range(1, W - 1):
            if (x, Y_PLAT, z) in t.cells and (x, y0, z) not in t.cells and t.cells[(x, Y_PLAT, z)].block != TILES:
                r = rng.random()
                if r < 0.10:
                    t.set(x, Y_PLAT, z, "minecraft:moss_block")
                elif r < 0.20:
                    t.set(x, y0, z, "minecraft:moss_carpet")
                elif r < 0.25:
                    t.set(x, y0, z, "minecraft:short_grass")
                elif r < 0.28:
                    t.set(x, y0, z, "minecraft:fern")
    vine = {"up": "false", "north": "false", "south": "false", "east": "false", "west": "false"}
    for z in (6, 7, 8):
        for y in range(y0 + 1, y0 + 1 + rng.randrange(2, 5)):
            if (2, y, z) not in t.cells:
                t.set(2, y, z, "minecraft:vine", dict(vine, east="true"))
        for y in range(y0 + 1, y0 + 1 + rng.randrange(1, 4)):
            if (12, y, z) not in t.cells:
                t.set(12, y, z, "minecraft:vine", dict(vine, west="true"))
    for x in (6, 8):
        t.set(x, 9, 4, "minecraft:vine", dict(vine, south="true", up="true"))
    return t


def ring_cells(w, h):
    """The outline cells of an ellipse w wide and h tall (0-based), one block thick, as a set of (i, j)."""
    a, b = w / 2.0, h / 2.0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0

    def inside(i, j, shrink):
        return ((i - cx) / (a - shrink)) ** 2 + ((j - cy) / (b - shrink)) ** 2 <= 1.0

    return {(i, j) for i in range(w) for j in range(h) if inside(i, j, 0.0) and not inside(i, j, 1.0)}


def ruin_b():
    rng = random.Random(0xB1B)
    t = Template("gate/ruin_b", W, 12, W)
    flags = [(55, TILES), (25, "minecraft:deepslate_tiles"), (12, "minecraft:cracked_deepslate_tiles"), (8, "minecraft:polished_deepslate")]
    found = [(60, "minecraft:cobbled_deepslate"), (40, "minecraft:deepslate_bricks")]
    platform(t, rng, flags, found, circular=True)
    # a ring of glyphs and blackstone round the dais edge, and the cross under the gate
    for x in range(1, W - 1):
        for z in range(1, W - 1):
            dx, dz = x - C, z - C
            d2 = dx * dx + dz * dz
            if 5.5 * 5.5 < d2 <= 6.5 * 6.5 and (x, Y_PLAT, z) in t.cells:
                t.set(x, Y_PLAT, z, GLYPH if (x + z) % 4 == 0 else "minecraft:polished_blackstone")
            elif (dx == 0 or dz == 0) and d2 <= 4 * 4 and (x, Y_PLAT, z) in t.cells:
                t.set(x, Y_PLAT, z, "minecraft:polished_blackstone" if (x + z) % 2 else TILES)
    t.set(C, Y_PLAT, C, GLYPH)

    # --- the floating ring: 7 wide (x 4..10), 8 tall (y 4..11), in the plane z=7, one block above the dais (y=3 is air) ---
    ring = ring_cells(7, 8)
    for (i, j) in ring:
        x, y = 4 + i, 4 + j
        cardinal = i in (0, 6) or j in (0, 7)
        if cardinal:
            axis = "x" if j in (0, 7) else "y"
            t.set(x, y, 7, PILLAR, {"axis": axis})
        else:
            t.set(x, y, 7, "minecraft:polished_blackstone" if rng.random() < 0.75 else "minecraft:polished_blackstone_bricks")
    # glyph studs where the ring is widest
    for (x, y) in ((4, 7), (10, 7), (4, 8), (10, 8)):
        t.set(x, y, 7, GLYPH)
    # the gate plane inside the ring (x 6..8, y 6..9), no stone frame: the ring is the frame
    gate_plane(t, C - 1, 6, 7, with_frame=False)
    # crying obsidian filling the ring's inside around the plane
    for (x, y) in ((5, 6), (5, 7), (5, 8), (5, 9), (9, 6), (9, 7), (9, 8), (9, 9), (6, 5), (7, 5), (8, 5), (6, 10), (7, 10), (8, 10)):
        if (x, y, 7) not in t.cells:
            t.set(x, y, 7, OBSIDIAN)

    # --- four obelisks with glyph caps ---
    for (x, z) in ((2, 2), (12, 2), (2, 12), (12, 12)):
        for y in Y_FOUND:  # the obelisks stand outside the circular dais: their own footing
            t.set(x, y, z, "minecraft:cobbled_deepslate")
        t.set(x, Y_PLAT, z, "minecraft:polished_blackstone")
        for y in range(3, 8):
            t.set(x, y, z, PILLAR, {"axis": "y"})
        t.set(x, 7, z, LANTERN)
        t.set(x, 8, z, GLYPH)
        # a fence post of chain above the cap, like the ring's anchor
        t.set(x, 9, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
    # a loose block or two: the dais is cracked
    for (x, z) in ((4, 11), (11, 4), (10, 10)):
        t.remove(x, Y_PLAT, z)
    return t


def main(argv):
    root = resources_root()
    for build in (ruin_a, ruin_b):
        t = build()
        p = t.save(root)
        print("wrote", os.path.relpath(p, root), "%dx%dx%d" % t.size, len(t.cells), "blocks")
        if "--preview" in argv:
            print(t.ascii_layers())
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

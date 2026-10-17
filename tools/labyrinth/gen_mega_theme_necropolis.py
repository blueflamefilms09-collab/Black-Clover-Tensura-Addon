#!/usr/bin/env python3
"""
THEME-C, theme 2 of 4 (1.59.0, docs/labyrinth/mega_v2_thc.md): the signature rooms of the NECROPOLIS, the evil road's city of the dead. Every room is named
`necropolis_<room>.nbt` so that `data/nusmp/mega/template_themes/necropolis.json` can reserve it for sectors of that theme.

    python3 tools/labyrinth/gen_mega_theme_necropolis.py                     # (re)writes every template
    python3 tools/labyrinth/gen_mega_theme_necropolis.py necropolis_catacombs   # only this one
    python3 tools/labyrinth/gen_mega_theme_necropolis.py --check             # regenerates in memory, compares with the files on disk, runs every check
    python3 tools/labyrinth/gen_mega_theme_necropolis.py --list

Why a generator and not hand-placed blocks: see `gen_mega_theme_cursed_archive.py`; the rules are the same (connector contract, palette constants so the
Region Theme restyles them into sandstone, bone and dust, the kit of `mega_decor_r1.py` and `mega_thc_kit.py`, every template checked before it is written).
The lights of this theme are SOUL lights and ghost-green froglights; the rooms are written with them and checked with them.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_thc_kit as K  # noqa: E402
import mega_decor_r2 as D2  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

REG = K.Registry(__doc__)
room = REG.room
CALM = ("necropolis_burial_vault",)           # no spawn markers needed by the checks (a treasure vault still carries its own spawn cells, see the room)
SOUL = True
GHOST = "minecraft:verdant_froglight"        # a pale green lamp, light 15, that no theme swaps: the glow of the dead
BONE = "minecraft:bone_block"
LOOT = "nusmp:chests/labyrinth_trial"


def skull(h, x, y, z, rot=0, kind="skeleton"):
    """A skull standing on the block below it (a niche shelf, a plinth, a post)."""
    D.skull_at(h, x, y, z, rot=rot, kind=kind)


def ghost_lamps(h, walls, tiers, laterals, depths=(-1,)):
    """A ghost-green froglight let into the masonry or the shelving of each wall in `walls` at every (lateral, tier): in the first of `depths` where the cell
    holds a block with a solid block behind it. Why: soul light is weak (10) and a hall of this height is lit by lamps in its walls, as the Archive is."""
    n = 0
    for w in walls:
        for y in tiers:
            for lat in laterals:
                for d in depths:
                    pos = w.cell(lat, y, d)
                    c = h.get(*pos)
                    if c is not None and c.block != GHOST and D.classify(c.block, c.props)[0] != "pass" and h.mt.t.get(*w.cell(lat, y, d - 1)) is not None:
                        h.set(pos[0], pos[1], pos[2], GHOST, over=True)
                        n += 1
                        break
    return n


def burial_block(h, x0, x1, z0, z1, top, skip=(), niches_on=(), tiers=(10, 12, 14), lamps=(11, 13)):
    """A solid block of tombs from the floor to row `top` (masonry with a few cracked bricks), cells in `skip` left open, and on every face in `niches_on`
    ('w', 'e', 'n', 's': the side the face looks to) loculi: a one-cell niche in every second cell of each tier, holding a skull, a candle or a heap of bones.
    The cap row is a polished slab course. Why: the brief's catacombs, 'wall niches of skulls', made of one repeated detail on a solid mass."""
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            if (x, z) in skip:
                continue
            for y in range(GROUND, top + 1):
                niche = False
                for side in niches_on:
                    edge = (side == "w" and x == x0) or (side == "e" and x == x1) or (side == "n" and z == z0) or (side == "s" and z == z1)
                    along = z if side in ("w", "e") else x
                    if edge and y in tiers and along % 2 == 0 and (z0 + 1 < z < z1 - 1 if side in ("w", "e") else x0 + 1 < x < x1 - 1):
                        niche = True
                if niche:
                    continue
                h.try_set(x, y, z, (L.CRACKED_TILE if h.rng.random() < 0.15 else L.TILE) if y == top else D.brick_mix(h, 0.18, 0.08))
    for side in niches_on:
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                edge = (side == "w" and x == x0) or (side == "e" and x == x1) or (side == "n" and z == z0) or (side == "s" and z == z1)
                along = z if side in ("w", "e") else x
                for y in tiers:
                    if edge and along % 2 == 0 and h.free(x, y, z) and h.get(x, y - 1, z) is not None and (z0 + 1 < z < z1 - 1 if side in ("w", "e") else x0 + 1 < x < x1 - 1):
                        pick = h.rng.random()
                        facing = {"w": "west", "e": "east", "n": "north", "s": "south"}[side]
                        if pick < 0.5:
                            skull(h, x, y, z, rot=K.SKULL_ROT[facing])
                        elif pick < 0.75:
                            D.candles_at(h, x, y, z, 2, "white")
                        else:
                            h.set(x, y, z, BONE, {"axis": "y"})
    for side in niches_on:                                          # a ghost lamp in the masonry between the tiers, one in every fourth cell
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                edge = (side == "w" and x == x0) or (side == "e" and x == x1) or (side == "n" and z == z0) or (side == "s" and z == z1)
                along = z if side in ("w", "e") else x
                if edge and along % 4 == 1 and (x, z) not in skip:
                    for y in lamps:
                        if h.get(x, y, z) is not None and (z0 + 1 < z < z1 - 1 if side in ("w", "e") else x0 + 1 < x < x1 - 1):
                            h.set(x, y, z, GHOST, over=True)


def terrace_parapet(h, cells_edge, y):
    """Wall posts along the open edges of a terrace (a low parapet of polished wall with a skull on every third post)."""
    for i, (x, z) in enumerate(cells_edge):
        if h.try_set(x, y, z, L.POLISHED_WALL) and i % 3 == 0:
            h.try_set(x, y + 1, z, "minecraft:skeleton_skull", {"rotation": str((i * 5) % 16)})


def mausoleum(h, x0, z0, y, door, size=5):
    """A small tomb house `size` square on a terrace (floor row y - 1): brick walls three high under a flat roof, a pediment of chiselled blocks over a one-wide
    gate of iron bars standing ajar on the `door` side, a sarcophagus and candles inside. Why: the brief's mausoleums with carved doors, repeated on the roofs of
    the burial blocks so the terraces read as streets of the dead."""
    xs, zs = range(x0, x0 + size), range(z0, z0 + size)
    mid = x0 + size // 2, z0 + size // 2
    for x in xs:
        for z in zs:
            wall = x in (x0, x0 + size - 1) or z in (z0, z0 + size - 1)
            gate = (door == "east" and x == x0 + size - 1 and z == mid[1]) or (door == "west" and x == x0 and z == mid[1])
            for k in range(3):
                if wall and not (gate and k < 2):
                    h.try_set(x, y + k, z, D.brick_mix(h, 0.12, 0.06) if k < 2 else L.POLISHED)
            h.try_set(x, y + 3, z, L.CHISELED if wall and (x + z) % 2 == 0 else L.POLISHED_SLAB, None if wall and (x + z) % 2 == 0 else {"type": "bottom"})
            if gate:
                h.try_set(x, y + 2, z, L.CHISELED)
                h.try_set(x, y + 3, z, L.CHISELED)
    gx = x0 + size - 1 if door == "east" else x0
    for dz in (-1, 1):
        h.try_set(gx, y, mid[1] + dz, L.IRON_BARS)
        h.try_set(gx, y + 1, mid[1] + dz, L.IRON_BARS)
    for dz in (1, 2, 3):
        h.try_set(mid[0], y, z0 + dz, L.POLISHED)
    for dz in (1, 2):
        h.try_set(mid[0], y + 1, z0 + dz, L.POLISHED_SLAB, {"type": "bottom"})
    h.try_set(mid[0], y + 1, z0 + 3, L.POLISHED)
    h.try_set(mid[0], y + 2, z0 + 3, "minecraft:wither_skeleton_skull", {"rotation": str(K.SKULL_ROT["north"])})
    if h.free(mid[0] - 1, y, z0 + 1):
        D.candles_at(h, mid[0] - 1, y, z0 + 1, 3, "white")
    if h.free(mid[0] + 1, y, z0 + 3):
        D.candles_at(h, mid[0] + 1, y, z0 + 3, 3, "white")


@room("necropolis_catacombs", "hall")
def catacombs():
    """The Terraced Catacombs: two crossing corridors of burial blocks with skull niches in every face, a flight up each of two opposite corners to the
    terraces on top of the blocks, where the tombs of the great families stand under the ceiling and bridges of dark stone carry the dead across the
    corridors at the height of the vault."""
    h = Hall("mega/hall/necropolis_catacombs", 3, 4, 24, wear=0.14)
    for side, off in (("n", 13), ("s", 13), ("w", 13), ("e", 13), ("w", 22), ("e", 22)):
        h.door(side, off)
    h.route(12, 2, 14, 33, label="nave")
    h.route(2, 12, 24, 14, label="north cross")
    h.route(2, 21, 24, 23, label="south cross")
    for side, off in (("n", 13), ("s", 13), ("w", 13), ("e", 13), ("w", 22), ("e", 22)):
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    TOP = GROUND + 7                          # the cap row of the blocks (row 16); you stand on 17
    blocks = {"NW": (2, 11, 2, 11), "NE": (15, 24, 2, 11), "MW": (2, 11, 15, 20), "ME": (15, 24, 15, 20), "SW": (2, 11, 24, 33), "SE": (15, 24, 24, 33)}
    stair_nw = {(x, z) for x in (2, 3, 4) for z in range(3, 12)}
    stair_se = {(x, z) for x in (22, 23, 24) for z in range(24, 33)}
    for name, (x0, x1, z0, z1) in blocks.items():
        faces = []
        if x1 == 11:
            faces.append("e")
        else:
            faces.append("w")
        if z1 == 11:
            faces.append("s")
        elif z0 == 24:
            faces.append("n")
        else:
            faces += ["n", "s"]
        skip = stair_nw if name == "NW" else stair_se if name == "SE" else ()
        burial_block(h, x0, x1, z0, z1, TOP, skip=skip, niches_on=faces)
    # the two flights (the blocks hold their stairwell open): from the cross corridors up to the terraces
    D.stair_flight(h, 2, 10, "north", 8, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=None)
    D.stair_flight(h, 24, 25, "south", 8, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=None)
    for x in (2, 3, 4):
        for z in (2, 3):
            h.try_set(x, TOP, z, L.TILE)
    for x in (22, 23, 24):
        for z in (32, 33):
            h.try_set(x, TOP, z, L.TILE)
    # the six bridges that join the terraces at the height of the vault: deck, a parapet on both long edges
    bridges = [("x", 7, 5, 9), ("x", 30, 28, 32), ("z", 6, 4, 8), ("z", 20, 17, 22), ("z", 6, 4, 8), ("z", 20, 17, 22)]
    over = [("v", 7), ("v", 30), ("h1", 6), ("h1", 20), ("h2", 6), ("h2", 20)]
    for kind, at in over:
        if kind == "v":                                  # across the nave (x = 12..14) between two terraces
            for z in range(at - 2, at + 3):
                for x in (12, 13, 14):
                    h.set(x, TOP, z, L.POLISHED if (x + z) % 2 else L.TILE)
            for x in (12, 13, 14):
                h.set(x, TOP + 1, at - 2, L.POLISHED_WALL)
                h.set(x, TOP + 1, at + 2, L.POLISHED_WALL)
        else:                                            # across a cross corridor (z = 12..14 or 21..23) between two terraces
            zs = (12, 13, 14) if kind == "h1" else (21, 22, 23)
            for x in range(at - 2, at + 3):
                for z in zs:
                    h.set(x, TOP, z, L.POLISHED if (x + z) % 2 else L.TILE)
            for z in zs:
                h.set(at - 2, TOP + 1, z, L.POLISHED_WALL)
                h.set(at + 2, TOP + 1, z, L.POLISHED_WALL)
    # the terrace parapets: a post of polished wall along every edge that looks into a corridor, a skull on every third
    deck = {(x, z) for (x, z), in [((x, z),) for x in range(2, 25) for z in range(2, 34)] if (h.get(x, TOP, z) is not None and h.get(x, TOP + 1, z) is None)}
    edge = []
    for (x, z) in sorted(deck):
        if any((x + dx, z + dz) not in deck and h.get(x + dx, TOP, z + dz) is None and h.inside(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            edge.append((x, z))
    terrace_parapet(h, edge, TOP + 1)
    # four mausoleums on the terraces: a small house with a gate of bars ajar, a sarcophagus inside
    for (x0, z0, door) in ((6, 4, "east"), (17, 4, "west"), (3, 26, "east"), (16, 26, "west")):
        mausoleum(h, x0, z0, TOP + 1, door)
    for (x, z, f) in ((8, 10, "south"), (18, 10, "south"), (8, 16, "east"), (18, 19, "west")):
        if h.can_set(x, TOP + 1, z, L.POLISHED):
            D.statue(h, x, z, facing=f, kit="skull", y=TOP + 1)
    # light: ghost lamps in the end walls and chandeliers over the two crossings
    D.chandelier_wheel(h, 13, 7, r=2, drop=8, soul=SOUL)
    D.chandelier_wheel(h, 13, 18, r=2, drop=8, soul=SOUL)
    D.chandelier_wheel(h, 13, 29, r=2, drop=8, soul=SOUL)
    ghost_lamps(h, (wn, ws), (GROUND + 2, GROUND + 7, GROUND + 12), (5, 21))
    ghost_lamps(h, (ww, we), (GROUND + 2, GROUND + 7, GROUND + 12), (5, 17, 28))
    D.loculi(h, "n", [8, 10, 16, 18], (GROUND + 1, GROUND + 3))
    D.loculi(h, "s", [8, 10, 16, 18], (GROUND + 1, GROUND + 3))
    D.upper_dress(h, [D.it_tall_banner("black", hang=4), D.it_high_bracket(soul=SOUL)], sides="nsw", step=(5, 8))
    D.cornice(h)
    D.cobwebs(h, 0.9)
    D.floor_medallion(h, 13, 13, 4)
    D.floor_medallion(h, 13, 22, 4)
    h.faction("undead", 13, 17)
    K.auto_light(h, soul=SOUL, max_lamps=60)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the bone bridges
PIT_FLOOR = 3                          # the row a person who falls stands on: the pit is solid under it (rows 0..2)


def pit_regions(h):
    """(ledge, island, bridges, pit): the sets of (x, z) of the Bone Bridges' floor plan. The ledge is a five-wide ring along the walls, the island an 8 x 8
    square in the middle, the four bridges six wide join the ledge to the island, the pit is everything else (the bridges hang over it)."""
    interior = [(x, z) for x in range(h.x0, h.x1 + 1) for z in range(h.z0, h.z1 + 1)]
    ledge = {(x, z) for (x, z) in interior if min(x - h.x0, h.x1 - x, z - h.z0, h.z1 - z) < 5}
    island = {(x, z) for (x, z) in interior if 14 <= x <= 21 and 14 <= z <= 21}
    bridges = {(x, z) for (x, z) in interior if (15 <= z <= 20 and (7 <= x <= 13 or 22 <= x <= 28)) or (15 <= x <= 20 and (7 <= z <= 13 or 22 <= z <= 28))}
    pit = set(interior) - ledge - island - bridges
    return ledge, island, bridges, pit


@room("necropolis_bone_bridges", "set")
def bone_bridges():
    """The Bone Bridges: a round of ledges round an abyss, an island in the middle with a bone obelisk crowned by a skull, and four bridges of bone block on piers
    of bone that cross the dark. Whoever falls lands six blocks down among soul fire, bones and a chest, with a ladder out at each side: the abyss costs health
    and time, never the way back. The walls above the ledge are loculi of skulls and the ceiling a forest of stalactites."""
    h = Hall("mega/set/necropolis_bone_bridges", 4, 4, 26, wear=0.08)
    for side, off in (("n", 13), ("e", 22), ("s", 22), ("w", 13)):
        h.door(side, off)
        D.door_trim(h, side, off)
    mt = h.mt
    ledge, island, bridges, pit = pit_regions(h)
    # 1. below the floor: solid everywhere but under the pit and its bridges, where only the bottom three rows are
    for z in range(mt.D):
        for x in range(mt.W):
            hollow = (x, z) in pit or (x, z) in bridges
            for y in range(0, FLOOR):
                if hollow and y >= PIT_FLOOR:
                    continue
                if hollow and y == PIT_FLOOR - 1 and (x, z) in pit:
                    mt.set(x, y, z, h.rng.choice(("minecraft:soul_soil", "minecraft:soul_soil", "minecraft:soul_soil", L.COBBLED)))
                else:
                    mt.set(x, y, z, h.rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))
    for (x, z) in sorted(pit):
        h.carve(x, FLOOR, z)
    # 2. the floor of the ledge and the island: chequered, the island raised on steps
    for (x, z) in sorted(ledge | island):
        if (x + z) % 2 == 0 and h.rng.random() < 0.5:
            h.floor_set(x, z, L.POLISHED)
    for (x, z) in sorted(bridges):
        h.floor_set(x, z, BONE, {"axis": "x" if (7 <= x <= 13 or 22 <= x <= 28) else "z"})
    for (x, z) in sorted(bridges):
        edge = z in (15, 20) if (7 <= x <= 13 or 22 <= x <= 28) else x in (15, 20)
        if edge:
            h.set(x, GROUND, z, L.POLISHED_WALL)
            if (x + z) % 3 == 0:
                h.set(x, GROUND + 1, z, "minecraft:skeleton_skull", {"rotation": str((x * 3 + z) % 16)})
    # 3. piers of bone under the bridges, two pairs on each
    for (px, pz) in ((9, 15), (9, 20), (12, 15), (12, 20), (23, 15), (23, 20), (26, 15), (26, 20), (15, 9), (20, 9), (15, 12), (20, 12), (15, 23), (20, 23), (15, 26), (20, 26)):
        for y in range(PIT_FLOOR, FLOOR):
            mt.set(px, y, pz, BONE, {"axis": "y"})
    # 4. the island: steps all round, a raised dais, a bone obelisk with a lamp and a skull for a crown, four braziers
    for (x, z) in sorted(island):
        edge = x in (14, 21) or z in (14, 21)
        if edge:
            if not any((x + dx, z + dz) in bridges for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                face = "east" if x == 14 else "west" if x == 21 else "south" if z == 14 else "north"
                h.set(x, GROUND, z, L.POLISHED_STAIRS, {"facing": face, "half": "bottom"})
        else:
            h.set(x, GROUND, z, L.POLISHED if (x + z) % 2 else L.TILE)
    for y in range(GROUND + 1, GROUND + 4):
        for x in range(16, 20):
            for z in range(16, 20):
                h.set(x, y, z, BONE, {"axis": "y"})
    for y in range(GROUND + 4, GROUND + 13):
        for x in (17, 18):
            for z in (17, 18):
                if (y - GROUND) % 3:
                    h.set(x, y, z, BONE, {"axis": "y"})
                else:
                    h.set(x, y, z, L.CHISELED)
    for x in (17, 18):
        for z in (17, 18):
            h.set(x, GROUND + 13, z, GHOST)
            h.set(x, GROUND + 14, z, BONE, {"axis": "y"})
            h.set(x, GROUND + 15, z, "minecraft:wither_skeleton_skull", {"rotation": str(((x - 17) * 8 + (z - 17) * 4) % 16)})
    for (bx, bz) in ((15, 15), (20, 15), (15, 20), (20, 20)):
        D.brazier(h, bx, bz, y=GROUND + 1, soul=True)
    # 5. the pit: soul fire on the soul soil, bones, skulls, a chest, a ladder out at each side
    for (x, z) in sorted(pit):
        r = h.rng.random()
        if mt.get(x, PIT_FLOOR - 1, z) is not None and mt.get(x, PIT_FLOOR - 1, z).block == "minecraft:soul_soil" and r < 0.08:
            mt.set(x, PIT_FLOOR, z, "minecraft:soul_fire")
        elif r < 0.16:
            mt.set(x, PIT_FLOOR, z, BONE, {"axis": h.rng.choice(("x", "y", "z"))})
        elif r < 0.19:
            mt.set(x, PIT_FLOOR, z, "minecraft:skeleton_skull", {"rotation": str(int(h.rng.random() * 16))})
    mt.t.chest(9, PIT_FLOOR, 25, LOOT, "east")
    for (lx, lz, face) in ((7, 9, "east"), (28, 26, "west"), (26, 7, "south"), (9, 28, "north")):
        for y in range(PIT_FLOOR, FLOOR + 1):
            mt.set(lx, y, lz, "minecraft:ladder", {"facing": face, "waterlogged": "false"})
    # 6. the walls above the ledge: skull niches, pilasters between the doors, ghost lamps, banners; the ceiling: stalactites, hanging soul lanterns
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    for wl, lats in ((wn, (6, 9, 19, 26, 29)), (ws, (6, 9, 13, 26, 29)), (ww, (6, 9, 19, 26, 29)), (we, (6, 9, 13, 26, 29))):
        for lat in lats:
            D.pilaster(wl, lat, 1, y0=GROUND, y1=GROUND + 9)
    for side in ("n", "s", "w", "e"):
        lats = [lat for lat in range(5, 31, 3) if all(abs(lat - off) > 3 for (sd, off) in h.doors if sd == side)]
        D.loculi(h, side, lats, (GROUND + 1, GROUND + 5, GROUND + 12, GROUND + 15))
    for wl in (wn, ws, ww, we):
        off = [o for (sd, o) in h.doors if sd == wl.side]
        ghost_lamps(h, (wl,), (GROUND + 3, GROUND + 8, GROUND + 17), [lat for lat in range(5, 31, 4) if all(abs(lat - o) > 3 for o in off)])
    for (x, z) in sorted(pit):
        if h.rng.random() < 0.03:
            D2.dripstone(mt, x, h.top, z, h.rng.choice((2, 3, 4)), "down")
    D.lantern_canopy(h, [8, 12, 24, 28], [8, 12, 24, 28], [4, 7, 10, 5, 8], soul=SOUL)
    D.upper_dress(h, [D.it_tall_banner("black", hang=5), D.it_high_bracket(soul=SOUL, drop=4)], sides="nsew", step=(6, 9), door_gap=2)
    D.cobwebs(h, 0.9)
    h.faction("undead", 17, 12)
    K.auto_light(h, soul=SOUL, max_lamps=40)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the grave pits
@room("necropolis_grave_pits", "trap")
def grave_pits():
    """The Grave Pits: a churchyard that looks like every other. A ledge of cracked flags, headstones and soul braziers round a square of fresh graves, which
    are the mod's pitfall tiles: every tile looks alike, one thin zig-zag of them is safe and leads to an open vault with a chest, and over every turn of the
    safe way a ghost light hangs on a chain. A wrong step drops three blocks into a spiked chamber (the Spire's own fall) with a ladder out. Same blocks, same
    chamber and same checks as the two pit halls of ROOMS-2, different dressing and a different hint."""
    import gen_mega_rooms2 as R2
    h = Hall("mega/trap/necropolis_grave_pits", 3, 3, 14, wear=0.16)
    mt = h.mt
    for side in ("n", "s", "e"):
        h.door(side, 13)
        D.door_trim(h, side, 13)
    rng = h.rng
    for z in range(mt.D):                                      # the solid slab under the floor: the chamber and the shaft are carved out of it afterwards
        for x in range(mt.W):
            for y in range(R2.PIT_CHAMBER_FLOOR - 1, FLOOR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))
    field = {(x, z) for x in range(7, 20) for z in range(7, 20)}
    island = {(x, z) for x in range(12, 15) for z in range(12, 15)}
    goals = {(x, z) for (x, z) in field - island if any((x + dx, z + dz) in island for dx, dz in L.STEP.values())}
    route = D2.snake_path(rng, (rng.randint(9, 17), 19), goals, field - island, min_len=16)
    if route is None:
        raise ValueError("no safe route found for the grave pits")
    R2.pit_chamber(mt, field, set(route) | island, rng, ladder=(5, 13))
    for (x, z) in sorted(field):
        h.shell.discard((x, FLOOR, z))
    h.shell.discard((5, FLOOR, 13))
    # the ledge: flags, cracked and dirty, a sandy border; the vault on the island (a chest between candles, a fallen robber)
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in field or (x, z) == (5, 13):
                continue
            if (x + z) % 2 == 0 and rng.random() < 0.4:
                h.floor_set(x, z, L.CRACKED_TILE)
    D.dais(h, 12, 12, 14, 14, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    h.loot_chest(13, GROUND + 1, 13, "south", LOOT)
    for dx, dz in ((-1, -1), (1, -1), (-1, 1)):
        D.candles_at(h, 13 + dx, GROUND + 1, 13 + dz, 3, "white")
    D.skull_at(h, 14, GROUND + 1, 14, rot=4)
    # soul braziers in the corners, headstones on the ledge (two wall posts and a skull, now and then a candle), none in front of a doorway
    for (bx, bz) in ((3, 3), (23, 3), (3, 23), (23, 23)):
        D.brazier(h, bx, bz, soul=SOUL)
    spots = [(x, z) for z in (3, 23) for x in range(6, 21, 3)] + [(x, z) for x in (3, 23) for z in range(6, 21, 4)]
    for i, (x, z) in enumerate(spots):
        if abs(x - 13) <= 2 and z in (3, 23):
            continue
        if all(h.can_set(x, GROUND + k, z, L.COBBLED_WALL) for k in (0, 1, 2)):
            h.set(x, GROUND, z, L.COBBLED_WALL)
            h.set(x, GROUND + 1, z, L.COBBLED_WALL)
            if i % 2:
                D.skull_at(h, x, GROUND + 2, z, rot=(i * 5) % 16)
            else:
                D.candles_at(h, x, GROUND + 2, z, 2, "white")
    # the hint: a ghost light on a chain over every turn of the safe way
    for i in range(1, len(route) - 1):
        a, b, c = route[i - 1], route[i], route[i + 1]
        if (b[0] - a[0], b[1] - a[1]) != (c[0] - b[0], c[1] - b[1]):
            for yy in range(GROUND + 5, h.top + 1):
                h.set(b[0], yy, b[1], L.CHAIN_BLOCK, {"axis": "y"})
            h.set(b[0], GROUND + 4, b[1], GHOST)
    for (x, z) in sorted(field - set(route)):
        if rng.random() < 0.05 and h.free(x, h.top, z):
            D2.dripstone(mt, x, h.top, z, rng.choice((1, 2, 3)), "down")
    for wl in (h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")):
        off = [o for (sd, o) in h.doors if sd == wl.side]
        ghost_lamps(h, (wl,), (GROUND + 3, GROUND + 6), [lat for lat in (5, 8, 11, 14, 17, 20, 23) if all(abs(lat - o) > 3 for o in off)])
    D.upper_dress(h, [D.it_tall_banner("black", hang=3), D.it_high_bracket(soul=SOUL, drop=3)], sides="nsew", step=(5, 8), door_gap=2)
    D.cobwebs(h, 0.9)
    h.faction("undead", 11, 22)
    for (x, z) in route[2::4][:4]:
        h.marker(x, GROUND, z, "trap", type="pit")
    K.auto_light(h, soul=SOUL, max_lamps=40)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


@REG.check("necropolis_grave_pits")
def grave_pits_check(h):
    """The pit halls' own check (a safe route of tiles from the ledge to the chest, most of the field is pits, a sealed chamber with spikes and a way out)."""
    import gen_mega_rooms2 as R2
    return ["pit check: " + e for e in R2.check_pit(h.mt, D2.Analysis(h.mt), None)]


# ----------------------------------------------------------------------------------------------------------------------------------- the burial vault
@room("necropolis_burial_vault", "treasure")
def burial_vault():
    """The Burial Vault: a lord's tomb that was sealed with his wealth. A stepped dais carries a great sarcophagus with its lid shoved aside, two skull-faced
    knights stand guard at its head, and chests wait in niches on both walls; gold lamps (ochre froglights) burn in the base of the dais, candles on the steps,
    and the whole room is a held breath. The calm room of the Necropolis: the dead here are quiet until the loot is touched."""
    h = Hall("mega/treasure/necropolis_burial_vault", 2, 2, 14, wear=0.06)
    h.door("s", 13)
    D.door_trim(h, "s", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_medallion(h, 8, 9, 5)
    D.dais(h, 5, 3, 12, 9, rows=2, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    D.sarcophagus(h.frame(8, GROUND + 2, 5, "south"), length=5, width=2, open_lid=True)
    D.statue(h, 6, 4, facing="south", kit="skull", y=GROUND + 2)
    D.statue(h, 11, 4, facing="south", kit="skull", y=GROUND + 2)
    for (x, z) in ((5, 10), (12, 10)):
        D.candelabra(h, x, z, y=GROUND, n=3, color="white", soul=False)
    # the chests in niches of both side walls, each with candles in front
    for wl in (ww, we):
        for lat in (5, 11):
            cells = D.niche(wl, lat, GROUND, GROUND + 2, width=3)
            x, y, z = wl.cell(lat, GROUND, -1)
            h.loot_chest(x, y, z, wl.into, LOOT)
            for dl in (-1, 1):
                D.candles_at(h, *wl.cell(lat + dl, GROUND, -1), 2, "white")
    D.loculi(h, "n", [4, 6, 10, 12], (GROUND + 4, GROUND + 6))
    ghost_lamps(h, (wn, ws, ww, we), (GROUND + 8,), (4, 8, 12))
    D.chandelier_wheel(h, 8, 9, r=3, drop=3, soul=SOUL)
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("undead", 8, 13)
    K.auto_light(h, soul=SOUL, max_lamps=20)
    D.auto_markers(h, spawns=2, patrols=1, centre=False)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the mausoleum street
def tomb_house(h, side, z0, z1, chest=False):
    """One mausoleum on the west (`side` 'w') or east ('e') of the nave street: a body six wide and `z0..z1` long with a carved front (pilasters, chiselled
    door panels, a lintel and a skull plaque), a gate of bars raised in the doorway, a step up from the street, two lamp posts, and inside a sarcophagus under
    candles (and, when `chest`, a chest at the foot of it). Written for the west side and mirrored for the east, so all four houses are one design."""
    def X(x):
        return x if side == "w" else 26 - x
    face = "west" if side == "w" else "east"
    mid = (z0 + z1) // 2
    # the body: walls of masonry round a hollow chamber, a tile roof and a parapet
    for z in range(z0, z1 + 1):
        for x in range(2, 8):
            wall = x in (2, 7) or z in (z0, z1)
            for y in range(GROUND, GROUND + 5):
                if wall:
                    h.try_set(X(x), y, z, D.brick_mix(h, 0.15, 0.06))
            h.try_set(X(x), GROUND + 5, z, L.TILE)
            if wall:
                h.try_set(X(x), GROUND + 6, z, L.POLISHED_WALL)
    # the front: the doorway two wide under a chiselled lintel, pilasters either side of it, carved door panels, a skull plaque
    for z in (mid, mid + 1):
        for y in (GROUND, GROUND + 1, GROUND + 2):
            h.mt.remove(X(7), y, z)
            h.shell.discard((X(7), y, z))
        h.set(X(7), GROUND + 3, z, L.CHISELED, over=True)
    for z in (mid - 1, mid + 2):
        for y in range(GROUND, GROUND + 5):
            h.set(X(7), y, z, L.POLISHED if y < GROUND + 4 else L.CHISELED, over=True)
    for z in (z0 + 1, z0 + 2, z1 - 2, z1 - 1):
        for y in (GROUND + 1, GROUND + 2, GROUND + 3):
            window = y == GROUND + 2 and z in (z0 + 2, z1 - 2)             # a ghost-green window in each carved panel: it lights the street and the chamber
            h.set(X(7), y, z, GHOST if window else L.CHISELED, over=True)
    h.set(X(2), GROUND + 3, mid, GHOST, over=True)                          # and one lamp let into the back wall, so the chamber is not dark behind the gate
    for z in (mid, mid + 1):                                    # the raised gate: a row of bars hangs in the top of the doorway
        h.set(X(7), GROUND + 3, z, L.IRON_BARS, over=True)
    # the step up from the street and the two lamp posts of the porch
    for z in (mid, mid + 1):
        h.try_set(X(9), GROUND, z, L.POLISHED_STAIRS, {"facing": face, "half": "bottom"})
        h.try_set(X(8), GROUND, z, L.POLISHED)
    for z in (z0 + 1, z1 - 1):
        h.try_set(X(9), GROUND, z, L.POLISHED_WALL)
        h.try_set(X(9), GROUND + 1, z, L.SOUL_LANTERN, {"hanging": False})
    # inside: a sarcophagus along the chamber, candles, a chest at its foot
    for dz in range(0, 4):
        h.try_set(X(4), GROUND, z0 + 2 + dz, L.POLISHED)
    for dz in range(0, 3):
        h.try_set(X(4), GROUND + 1, z0 + 2 + dz, L.POLISHED_SLAB, {"type": "bottom"})
    h.try_set(X(4), GROUND + 1, z0 + 5, L.POLISHED)
    h.try_set(X(4), GROUND + 2, z0 + 5, "minecraft:wither_skeleton_skull", {"rotation": str(K.SKULL_ROT[face])})
    for z in (z0 + 2, z1 - 1):
        if h.free(X(3), GROUND, z):
            D.candles_at(h, X(3), GROUND, z, 3, "white")
    if chest:
        h.loot_chest(X(6), GROUND, z1 - 1, face, LOOT)


@room("necropolis_mausoleum_street", "hall")
def mausoleum_street():
    """The Mausoleum Street: two streets of the dead cross in a flagged plaza with a bone obelisk, and four tomb houses stand on the west and east sides of
    the long street, each with a carved front, a raised gate, a step up and two lamp posts. The houses are small rooms of their own (a sarcophagus, candles, a
    chest in two of them): a street you can walk through, with doors you can walk in."""
    h = Hall("mega/hall/necropolis_mausoleum_street", 3, 3, 16, wear=0.12)
    for side, off in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        h.door(side, off)
        D.door_trim(h, side, off)
    h.route(10, 2, 16, 9, label="north arm")
    h.route(10, 17, 16, 24, label="south arm")
    h.route(2, 10, 9, 16, label="west arm")
    h.route(17, 10, 24, 16, label="east arm")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    for (side, z0, z1, chest) in (("w", 2, 9, True), ("e", 2, 9, False), ("w", 17, 24, False), ("e", 17, 24, True)):
        tomb_house(h, side, z0, z1, chest)
    # the plaza: a medallion in the floor, a bone obelisk on a one-step plinth with a ghost lamp at its tip
    D.floor_medallion(h, 13, 13, 5)
    for x in range(12, 15):
        for z in range(12, 15):
            if (x, z) != (13, 13):
                face = "east" if x == 12 else "west" if x == 14 else "south" if z == 12 else "north"
                h.set(x, GROUND, z, L.POLISHED_STAIRS, {"facing": face, "half": "bottom"})
            else:
                h.set(x, GROUND, z, L.POLISHED)
    for y in range(GROUND + 1, GROUND + 12):
        if (y - GROUND) % 4:
            h.set(13, y, 13, BONE, {"axis": "y"})
        else:
            h.set(13, y, 13, GHOST)                                          # a ghost lamp at every fourth row: the plaza and the four arms are lit from the obelisk
    h.set(13, GROUND + 12, 13, GHOST)
    # the long street's edges: a lamp post at every fifth cell, a few headstones and heaps of bones
    for z in range(4, 24, 5):
        for x in (10, 16):
            if h.can_set(x, GROUND, z, L.POLISHED_WALL) and not (11 <= z <= 15):
                h.set(x, GROUND, z, L.POLISHED_WALL)
                h.set(x, GROUND + 1, z, L.SOUL_LANTERN, {"hanging": False})
    D.bones(h, 9, 9, rot=3)
    D.bones(h, 17, 17, rot=11, big=True)
    ghost_lamps(h, (wn, ws), (GROUND + 3, GROUND + 8), (9, 17))              # x = 5 and 21 would sit behind the houses, so the end walls carry theirs beside the street
    ghost_lamps(h, (ww, we), (GROUND + 3, GROUND + 8), (10, 16))             # the side walls' lamps flank the doorway: the houses fill the rest of those walls
    D.upper_dress(h, [D.it_tall_banner("black", hang=4), D.it_high_bracket(soul=SOUL, drop=3)], sides="nsew", step=(5, 8), door_gap=2)
    D.chandelier_wheel(h, 13, 6, r=2, drop=5, soul=SOUL)
    D.chandelier_wheel(h, 13, 20, r=2, drop=5, soul=SOUL)
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("undead", 13, 20)
    K.auto_light(h, soul=SOUL, max_lamps=40)
    D.auto_markers(h, spawns=7, patrols=2)
    return h


if __name__ == "__main__":
    sys.exit(K.main(REG, sys.argv[1:], calm=CALM))

#!/usr/bin/env python3
"""
The boss-chain templates of the Mega-Dungeon (1.54.0, builder GUARD): the Labyrinth Guardian's hall, the antechamber behind its Gatekeeper and
the Way Onward corridors. Thirteen templates (the first nine of 1.54.0, then a mirrored antechamber and three compact Way Onwards that the assembler needs
where a region is nearly used up), built with `mega_lib.MT` and the helpers of `mega_guard_lib.py`, written to
`src/main/resources/data/nusmp/structure/mega/{arena,antechamber,onward}/`:

    python3 tools/labyrinth/gen_mega_guard.py                 # (re)writes every template
    python3 tools/labyrinth/gen_mega_guard.py --only mega/arena/guardian_hall_a
    python3 tools/labyrinth/gen_mega_guard.py --check         # exit 1 when a file on disk differs from what this script builds (CI-safe, no game)
    python3 tools/labyrinth/gen_mega_guard.py --list

Why generated: the connector contract (docs/labyrinth/mega.md section 4) needs exact jigsaw cells on the nine-block grid, a 3 x 4 x 2 Gatekeeper
slab and a rubble seal that the generator can find again, and a mistake of one block makes a sector that cannot grow. A script builds the same
bytes every time, the checker (`mega_check.py`) reads them back, and a re-run after a palette change is one command.

What each piece is FOR (the owner's brief: a boss-worthy hall, a calm lobby with two roads, a place of passage that ends in rubble):
  * arena/guardian_hall_a  "The Nave of the Sealed Reliquary": a 45 x 45 hall in the spirit of the cathedral references, a square nave ringed by
                           twelve columns and a stepped arcade, lancet windows that glow, a three-tier dais with the Guardian Altar in the middle,
                           a rune ring and a chequerboard floor, a clear fighting floor (nothing within 12 blocks of the altar but the dais), the
                           sealed Gatekeeper in the north wall and the entry in the south wall.
  * arena/guardian_hall_b  "The Chapter of the Eight Stars": the same contract in an octagon (the corners of the footprint cut off), eight columns on
                           a ring, concentric octagonal rings in the floor, a stepped, faceted vault with a glowing lantern at the top.
  * antechamber/antechamber_a  "The Hall of the Two Roads": the calm lobby behind the Gatekeeper: red marks the road to the Crucible Vault (east), blue the
                           Way Onward (west). antechamber_b is its mirror image (Vault west, Way Onward east).
  * onward/{straight,left,right}_{a,b,c}  The corridor from the antechamber to the next sector, ending in a rubble seal with the EXPANSION_SOCKET marker.
                           `a` is a passage of chains and lanterns with a collapsed end; `b` an ossuary of skulls, bars and candles; `c` the passage of `a`
                           in compact size (9 x 18 straight, 18 x 18 for the turns) under blue banners.

Every template ends with `G.ensure_light` (invisible `minecraft:light` blocks for what the visible fixtures leave dark, see mega_guard_lib.py) and has to pass
`mega_check.py`, `mega_arch_check.py` and this script's own `verify`.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_guard_lib as G  # noqa: E402
from mega_lib import MT  # noqa: E402

FLOOR = L.FLOOR_ROW
C = 22                                  # centre cell of the 45 x 45 halls
ALTAR_HEADROOM = [(C, FLOOR + 3 + k, C) for k in range(1, 10)]      # the nine cells above the Guardian Altar stay free for the colossal Guardian
CRYING = "minecraft:crying_obsidian"
SEA = "minecraft:sea_lantern"


# ====================================================================================================================
# The Guardian's hall, A: the Nave of the Sealed Reliquary
# ====================================================================================================================
def round_norm(x, z):
    """The radial measure of the square hall: the plain distance from the centre (rings are circles)."""
    return G.dist(x, z, C, C)


def oct_measure(x, z):
    """The radial measure of the octagonal hall: rings are regular octagons."""
    return G.oct_norm(x - C, z - C)


def hall_floor_and_dais(mt, norm=round_norm, ring_outer=9.7, checker_outer=17.5, node_radius=13.5, node_phase=math.pi / 8, cells=None):
    """The floor of both halls: a polished disc under the dais, a ring of crying obsidian (the rune circle), a chequerboard, a polished border.
    `norm(x, z)` is the radial measure (a circle in the square hall, an octagon in the other); `cells` limits the floor to the cells that have one."""
    for z in range(2, 43):
        for x in range(2, 43):
            if cells is not None and (x, z) not in cells:
                continue
            r = norm(x, z)
            if r <= 8.5:
                mt.set(x, FLOOR, z, L.POLISHED)
            elif r <= ring_outer:
                mt.set(x, FLOOR, z, CRYING)
            elif r <= checker_outer:
                mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            elif r <= checker_outer + 1.0:
                mt.set(x, FLOOR, z, L.POLISHED)
    # eight glowing nodes of the rune circle, set in the chequerboard (a sea lantern is a full block: safe to walk on); a quarter turn off the axes so the
    # processional carpets never cover one
    for k in range(8):
        a = node_phase + k * math.pi / 4
        mt.set(C + int(round(math.cos(a) * node_radius)), FLOOR, C + int(round(math.sin(a) * node_radius)), SEA)


def dais(mt, norm=round_norm):
    """Three tiers: a half-block ring, a full-block disc, a half-block ring and the pedestal; every step is half a block or less, so it can be walked without
    jumping. The Guardian Altar stands on the pedestal at row 11; the GUARDIAN_SPAWN marker is the cell above it."""
    for z in range(C - 10, C + 11):
        for x in range(C - 10, C + 11):
            r = norm(x, z)
            if 6.5 < r <= 8.5:
                G.slab(mt, x, FLOOR + 1, z, L.POLISHED_SLAB)
            elif r <= 6.5:
                mt.set(x, FLOOR + 1, z, L.POLISHED)
            if 2.8 < r <= 4.6:
                G.slab(mt, x, FLOOR + 2, z, L.POLISHED_SLAB)
            elif r <= 2.8:
                mt.set(x, FLOOR + 2, z, L.POLISHED if (x, z) != (C, C) else L.CHISELED)
    mt.set(C, FLOOR + 3, C, "nusmp:guardian_altar")
    mt.marker(C, FLOOR + 4, C, "guardian_spawn")


def porch(mt, side, centre, half_gap=2, y_top=22, deep=2):
    """A portal porch in front of a doorway on `side` (centred on lateral `centre`): two 2-wide pilasters 3 cells either side of the doorway, joined by a stepped
    lintel. It frames the Gatekeeper (and the entry) like the great door of a cathedral and leaves 5 clear cells in front of the 3-wide hole."""
    for sign in (-1, 1):
        base = centre + sign * (half_gap + 1) - (1 if sign < 0 else 0)          # the two lateral cells of the pilaster: just outside the 5-wide opening
        for k in range(2):
            lat = centre + sign * (half_gap + 1 + k)
            for d in range(2, 2 + deep):
                for y in range(FLOOR + 1, y_top + 1):
                    G.put_wall(mt, side, lat, d, y, L.POLISHED if k == 0 else L.BRICK)
                G.put_wall(mt, side, lat, d, FLOOR + 1, L.CHISELED)
    for d in range(2, 2 + deep):                                                 # lintel and its stepped arch
        for lat in range(centre - half_gap - 2, centre + half_gap + 3):
            G.put_wall(mt, side, lat, d, y_top, L.POLISHED)
            G.put_wall(mt, side, lat, d, y_top - 1, L.CHISELED if lat == centre else L.POLISHED)
        for sign in (-1, 1):
            x, y, z = G.cell(mt, side, centre + sign * half_gap, d, y_top - 2)
            G.stair(mt, x, y, z, L.POLISHED_STAIRS, G.lateral_step(side, sign), half="top", shape="straight")


def arcade_gap(mt, orient, line, g0, g1, y_arch=29, y_top=34):
    """The wall of the colonnade between two columns: solid from `y_arch` to `y_top` (three cells thick on the ring `line`) except an arch over the gap
    cells g0..g1. A small gap (4 cells) gets a squared arch with chamfered corners; the wide central gap a stepped arch. A band of chiselled blocks and a
    polished cornice run along the top."""
    width = g1 - g0 + 1
    mid = (g0 + g1) / 2.0
    for t in (line - 1, line, line + 1):
        for a in range(g0, g1 + 1):
            off = abs(a - mid)
            for y in range(y_arch, y_top + 1):
                k = y - y_arch
                if width >= 8:
                    free = off <= (width - 1) / 2.0 - k * 1.0 and k <= 4
                    step_edge = (not free) and off <= (width - 1) / 2.0 - (k - 1) * 1.0 and 1 <= k <= 4
                else:
                    free = (k == 0) or (k == 1 and off <= 0.6)
                    step_edge = (not free) and k == 1
                x, z = (a, t) if orient == "x" else (t, a)
                if free:
                    continue
                if step_edge:
                    toward = ("west" if a < mid else "east") if orient == "x" else ("north" if a < mid else "south")
                    G.stair(mt, x, y, z, L.POLISHED_STAIRS, toward, half="top", shape="straight")
                elif y == y_top:
                    mt.set(x, y, z, L.POLISHED)
                elif y == y_top - 1:
                    mt.set(x, y, z, L.CHISELED)
                else:
                    mt.set(x, y, z, L.BRICK)


def arcade_pier(mt, cx, cz, y_arch=29, y_top=34):
    """The wall of the colonnade over a column (3 x 3, rows y_arch..y_top), so the arches between the columns spring from one continuous arcade."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in range(y_arch, y_top + 1):
                mt.set(cx + dx, y, cz + dz, L.POLISHED if y == y_top else L.CHISELED if y == y_top - 1 else L.BRICK)


def hall_a():
    mt = MT("mega/arena/guardian_hall_a", 5, 5)
    H = 26
    TOP = FLOOR + H                                   # 34, the last free row; the ceiling is rows 35 and 36
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    mt.socket("s", C, pool=L.P_EMPTY, name=L.DOOR, target=L.DOOR)
    mt.socket("n", C, pool=L.P_ANTECHAMBER, name=L.CHAIN, target=L.CHAIN, final_state=G.GATE_STATE, hole=False)

    hall_floor_and_dais(mt)
    dais(mt)

    # the colonnade: twelve columns on the square ring of 8 and 36
    cols = (8, 15, 29, 36)
    for cx in cols:
        for cz in cols:
            if cx in (8, 36) or cz in (8, 36):
                G.column(mt, cx, cz, FLOOR + 1, 28)
                arcade_pier(mt, cx, cz)
    # the arcade walls between them (rows 29..34), seen from the nave as a row of arches
    for line in (8, 36):
        arcade_gap(mt, "x", line, 10, 13)
        arcade_gap(mt, "x", line, 17, 27)
        arcade_gap(mt, "x", line, 31, 34)
        arcade_gap(mt, "z", line, 10, 13)
        arcade_gap(mt, "z", line, 17, 27)
        arcade_gap(mt, "z", line, 31, 34)

    # the walls: pilasters between the windows, lit lancet windows, banners, the portal porches
    for side in "nsew":
        for lat in (15, 29):
            G.pilaster(mt, side, lat, FLOOR + 1, 27)
    for side in "ns":
        for lat0, glass in ((10, "minecraft:cyan_stained_glass"), (31, "minecraft:purple_stained_glass")):
            G.lancet(mt, side, lat0, 4, 14, 14, glass)
    for side in "ew":
        for lat0, glass in ((10, "minecraft:purple_stained_glass"), (31, "minecraft:cyan_stained_glass")):
            G.lancet(mt, side, lat0, 4, 14, 14, glass)
        G.lancet(mt, side, 20, 5, 14, 17, "minecraft:light_blue_stained_glass")
    for side in "ns":
        G.wall_banners(mt, side, 17, 2, 25, ("purple", "black"))
        G.wall_banners(mt, side, 26, 2, 25, ("black", "purple"))
    for side in "ew":
        G.wall_banners(mt, side, 17, 2, 25, ("purple", "black"))
        G.wall_banners(mt, side, 26, 2, 25, ("black", "purple"))
    porch(mt, "n", C)
    porch(mt, "s", C)
    # the seal plaque over the Gatekeeper
    for lat in (21, 22, 23):
        for y in (14, 15, 16):
            edge = lat != 22 or y != 15
            G.put_wall(mt, "n", lat, 1, y, L.CHISELED if edge else CRYING)
    for lat, y in ((22, 14), (22, 16), (21, 15), (23, 15)):
        G.put_wall(mt, "n", lat, 1, y, L.POLISHED)

    # light and ceiling: braziers on the diagonals, a halo of soul lanterns, glowing ceiling rings, beams
    for (bx, bz) in ((12, 12), (32, 12), (12, 32), (32, 32)):
        G.brazier(mt, bx, FLOOR + 1, bz, soul=True)
    G.chandelier_ring(mt, C, C, TOP, 7, 8, 7, soul=True, phase=math.pi / 8)
    G.chandelier_ring(mt, C, C, TOP, 11, 12, 5, soul=False)
    for k in range(16):
        a = k * math.pi / 8
        mt.set(C + int(round(math.cos(a) * 10)), TOP + 1, C + int(round(math.sin(a) * 10)), SEA)
    for z in (12, 32):
        G.beam(mt, 10, z, 34, z, TOP, "x")

    # armoured sentinels in the four corners of the aisles (far from the fight), the processional carpets (flat, harmless to walk on) and candles at the feet of the columns
    for (sx, sz, face) in ((4, 4, "south"), (40, 4, "south"), (4, 40, "north"), (40, 40, "north")):
        L.statue(mt, sx, FLOOR + 1, sz, facing=face)
    for z in list(range(33, 43)) + list(range(2, 12)):
        for x in (21, 23):
            mt.set(x, FLOOR + 1, z, "minecraft:black_carpet")
        mt.set(C, FLOOR + 1, z, "minecraft:purple_carpet")
    for cx in cols:
        for cz in cols:
            if cx in (8, 36) or cz in (8, 36):
                G.candle_cluster(mt, cx + (2 if cx < C else -2), FLOOR + 1, cz + (2 if cz < C else -2), "purple", 3)
    # votive candles along the foot of the walls, three to a block every third cell (the reference's field of lit candles, kept to the aisles: the nave's
    # floor stays open), except in front of the pilasters, the portals and the corner fires
    for side in "nsew":
        for k, lat in enumerate(range(5, 40, 3)):
            if 13 <= lat <= 17 or 27 <= lat <= 31 or (side in "ns" and 17 <= lat <= 27) or lat in (5, 38):
                continue
            x, y, z = G.cell(mt, side, lat, 2, FLOOR + 1)
            G.candle_cluster(mt, x, y, z, ("purple", "white")[k % 2], 3)
    # the corners and the two portals are the darkest places of a hall this tall (a chandelier hung from row 34 lights nothing at the floor): a pair of
    # soul fires beside every corner statue, a warm fire either side of the Gatekeeper and of the entry so both doors read from across the nave
    for (bx, bz) in ((6, 3), (3, 6), (38, 3), (41, 6), (6, 41), (3, 38), (38, 41), (41, 38)):
        G.brazier(mt, bx, FLOOR + 1, bz, soul=True)
    for (bx, bz) in ((16, 4), (28, 4), (16, 40), (28, 40)):
        G.brazier(mt, bx, FLOOR + 1, bz, soul=False)

    mt.finish()
    G.fill_gatekeeper(mt, "n", C)
    mt.marker(C, FLOOR + 2, 2, "gatekeeper", facing="north")
    G.ensure_light(mt, keep_clear=ALTAR_HEADROOM)
    return mt


# ====================================================================================================================
# The Guardian's hall, B: the Chapter of the Eight Stars (an octagon)
# ====================================================================================================================
def oct_cells():
    """(interior, wall) cell sets of the octagonal hall: the 41 x 41 interior with each corner cut off by 12 cells; the wall is the band round it (two thick
    on the axis walls, three thick on the diagonals); the corners of the footprint beyond the band stay empty."""
    interior, wall = set(), set()
    for z in range(45):
        for x in range(45):
            dx, dz = abs(x - C), abs(z - C)
            if G.in_octagon(x, z):
                interior.add((x, z))
            elif dx <= 22 and dz <= 22 and dx + dz <= 31:
                wall.add((x, z))
    return interior, wall


def vault_top(n):
    """The highest free row at octagonal distance n from the centre: 24 at the walls, rising three rows in four cells towards the middle, and a tall
    lantern shaft (to row 44) over the central 5 x 5."""
    if n <= 2.6:
        return 44
    return 24 + int(max(0.0, 20.0 - n) * 0.75)


def line_cells(x1, z1, x2, z2):
    """The integer cells on the segment from (x1, z1) to (x2, z2) (a simple DDA: every step moves one cell along the longer axis)."""
    n = max(abs(x2 - x1), abs(z2 - z1), 1)
    return [(int(round(x1 + (x2 - x1) * i / n)), int(round(z1 + (z2 - z1) * i / n))) for i in range(n + 1)]


def hall_b():
    mt = MT("mega/arena/guardian_hall_b", 5, 5)
    interior, wall = oct_cells()
    top_of = {}
    for (x, z) in interior:
        top_of[(x, z)] = vault_top(oct_measure(x, z))
    # the shell: floor, walls, a stepped faceted vault
    for (x, z) in interior | wall:
        mt.set(x, FLOOR, z, L.CRACKED_TILE if mt.rng.random() < 0.10 else L.TILE)
    for (x, z) in wall:
        for y in range(FLOOR + 1, 27):
            mt.set(x, y, z, L.CRACKED_BRICK if mt.rng.random() < 0.08 else L.BRICK)
    for (x, z), ft in top_of.items():
        n = oct_measure(x, z)
        for k in (1, 2):
            mt.set(x, ft + k, z, L.TILE)
        if 2.6 < n <= 4.2:                                                         # the wall of the lantern shaft: solid from the vault up to the cap
            for y in range(ft + 1, 46):
                mt.set(x, y, z, L.POLISHED if y % 6 else L.CHISELED)
    for (x, z) in top_of:
        if oct_measure(x, z) <= 2.6:
            mt.set(x, 45, z, L.TILE)
            mt.set(x, 46, z, L.TILE)
    for (x, z) in wall:                                                              # the wall closes against the lowest roof ring
        for y in (25, 26):
            mt.set(x, y, z, L.TILE if y == 26 else L.BRICK)

    mt.socket("s", C, pool=L.P_EMPTY, name=L.DOOR, target=L.DOOR)
    mt.socket("n", C, pool=L.P_ANTECHAMBER, name=L.CHAIN, target=L.CHAIN, final_state=G.GATE_STATE, hole=False)

    hall_floor_and_dais(mt, oct_measure, ring_outer=10.2, checker_outer=17.5, node_radius=14.0, node_phase=math.pi / 8, cells=interior)
    dais(mt, oct_measure)

    # eight columns on the ring of radius 16, joined by a lintel. The ring is turned half a step (22.5 degrees) so that no column stands in the axis of a
    # doorway: with one on each axis the entry and the Gatekeeper both looked at a pillar, and the carpet ran into its foot
    cols = [(C + int(round(16 * math.cos(math.pi / 8 + k * math.pi / 4))), C + int(round(16 * math.sin(math.pi / 8 + k * math.pi / 4)))) for k in range(8)]
    for (cx, cz) in cols:
        G.column(mt, cx, cz, FLOOR + 1, 24)
    for i, (a, b) in enumerate(zip(cols, cols[1:] + cols[:1])):
        for (x, z) in line_cells(a[0], a[1], b[0], b[1]):
            if abs(x - a[0]) <= 1 and abs(z - a[1]) <= 1 or abs(x - b[0]) <= 1 and abs(z - b[1]) <= 1:
                continue
            mt.set(x, 25, z, L.POLISHED)
            mt.set(x, 26, z, L.CHISELED if i % 2 == 0 else L.POLISHED)
    for (cx, cz) in cols:                                                             # the lintel's row over each column
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                mt.set(cx + dx, 25, cz + dz, L.POLISHED)
                mt.set(cx + dx, 26, cz + dz, L.CHISELED)

    # windows on the axis walls, banners, the two portal porches, the seal plaque
    for side in "ew":
        G.lancet(mt, side, 20, 5, 13, 11, "minecraft:light_blue_stained_glass")
        for lat0, glass in ((14, "minecraft:cyan_stained_glass"), (28, "minecraft:purple_stained_glass")):
            G.lancet(mt, side, lat0, 3, 13, 10, glass)
        G.wall_banners(mt, side, 17, 2, 21, ("purple", "black"))
        G.wall_banners(mt, side, 26, 2, 21, ("black", "purple"))
    for side in "ns":
        for lat0, glass in ((14, "minecraft:cyan_stained_glass"), (28, "minecraft:purple_stained_glass")):
            G.lancet(mt, side, lat0, 3, 13, 10, glass)
    porch(mt, "n", C, y_top=21)
    porch(mt, "s", C, y_top=21)
    for lat in (21, 22, 23):
        for y in (14, 15, 16):
            edge = lat != 22 or y != 15
            G.put_wall(mt, "n", lat, 1, y, L.CHISELED if edge else CRYING)
    for lat, y in ((22, 14), (22, 16), (21, 15), (23, 15)):
        G.put_wall(mt, "n", lat, 1, y, L.POLISHED)

    # light: braziers at the four diagonal mid-walls, a halo of soul lanterns hung from the faceted vault, a lantern cluster from the shaft, glowing vault rings
    for (bx, bz) in ((C + 14, C + 14), (C - 14, C + 14), (C + 14, C - 14), (C - 14, C - 14)):
        G.brazier(mt, bx - (1 if bx > C else -1), FLOOR + 1, bz - (1 if bz > C else -1), soul=True)
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        x, z = C + int(round(math.cos(a) * 8)), C + int(round(math.sin(a) * 8))
        G.hang_lantern(mt, x, top_of[(x, z)], z, 6, soul=True)
    for k in range(12):
        a = k * math.pi / 6
        x, z = C + int(round(math.cos(a) * 12)), C + int(round(math.sin(a) * 12))
        if (x, z) in top_of:
            G.hang_lantern(mt, x, top_of[(x, z)], z, 4, soul=False)
    for k in range(8):                                                                # glowing vault rings: lights set into the first roof row
        for rr, phase in ((15.0, math.pi / 8), (9.5, 0.0)):
            a = phase + k * math.pi / 4
            x, z = C + int(round(math.cos(a) * rr)), C + int(round(math.sin(a) * rr))
            if (x, z) in top_of:
                mt.set(x, top_of[(x, z)] + 1, z, SEA)
    for (x, z) in top_of:
        if oct_measure(x, z) <= 1.5:
            mt.set(x, 45, z, SEA)
    for y in range(36, 45):                                                           # the hanging lantern cluster: a chain from the shaft cap over the altar
        mt.set(C, y, C, L.CHAIN_BLOCK, {"axis": "y"})
    mt.set(C, 35, C, L.SOUL_LANTERN, {"hanging": "true"})
    # vault ribs: the eight radial lines of the first roof row in polished stone, a chiselled ring half way up
    for (x, z), ft in top_of.items():
        dx, dz = x - C, z - C
        n = oct_measure(x, z)
        if n < 4.4 or n > 19.5:
            continue
        rib = min(abs(dx), abs(dz)) <= 0 or abs(abs(dx) - abs(dz)) <= 0
        if rib or int(n) in (12, 16):
            mt.set(x, ft + 1, z, L.POLISHED if rib else L.CHISELED)

    # processional carpets
    for z in list(range(33, 43)) + list(range(2, 12)):
        for x in (21, 23):
            mt.set(x, FLOOR + 1, z, "minecraft:black_carpet")
        mt.set(C, FLOOR + 1, z, "minecraft:purple_carpet")
    # a warm fire either side of the Gatekeeper and of the entry, so both doors read from across the chapter
    for (bx, bz) in ((16, 4), (28, 4), (16, 40), (28, 40)):
        G.brazier(mt, bx, FLOOR + 1, bz, soul=False)

    mt.finish()
    G.fill_gatekeeper(mt, "n", C)
    mt.marker(C, FLOOR + 2, 2, "gatekeeper", facing="north")
    G.ensure_light(mt, keep_clear=ALTAR_HEADROOM)
    return mt


# ====================================================================================================================
# The antechamber: the Hall of the Two Roads
# ====================================================================================================================
def road_plaque(mt, side, offset, wool, candle, accent):
    """Above a doorway of the antechamber: a plaque of coloured wool in a polished frame with a chiselled sigil, and a ledge of lit candles in front of it.
    Red marks the road to the Crucible Vault, blue the Way Onward, so a party that has just beaten the Guardian reads the choice before it walks."""
    for lat in range(offset - 3, offset + 4):
        for row in range(14, 18):
            frame = lat in (offset - 3, offset + 3) or row in (14, 17)
            sigil = lat == offset and row in (15, 16) or (row == 15 and lat in (offset - 1, offset + 1))
            G.put_wall(mt, side, lat, 1, row, L.POLISHED if frame else L.CHISELED if sigil else wool)
    for lat in range(offset - 2, offset + 3):
        x, y, z = G.cell(mt, side, lat, 2, 14)
        G.slab(mt, x, y, z, L.POLISHED_SLAB, "top")
        x, y, z = G.cell(mt, side, lat, 2, 15)
        mt.set(x, y, z, "minecraft:%s_candle" % candle, {"candles": "3", "lit": "true"})
    x, y, z = G.cell(mt, side, offset, 2, 16)
    mt.set(x, y, z, accent)


def antechamber(name, mirror=False):
    """The Hall of the Two Roads. `mirror` turns the hall into its mirror image: the Crucible Vault road on the WEST and the Way Onward on the EAST (the
    plaques, carpets and window colours follow). Why a mirror: the assembler cannot flip a template, and the two roads leave this hall at its two flanks, so
    the Way Onward always lands on one fixed side of the chain; where that side is shut in by the edge of the region the chain would not fit, while its
    mirror image does (the pool offers both, and the search takes whichever fits)."""
    mt = MT(name, 3, 3)
    H = 18
    TOP = FLOOR + H                                           # 26, the last free row; the ceiling is rows 27 and 28
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.08)
    vside, oside = ("w", "e") if mirror else ("e", "w")      # the side of the Vault road and of the Way Onward
    mt.socket("s", 13, pool=L.P_EMPTY, name=L.CHAIN, target=L.CHAIN)
    mt.socket(vside, 13, pool=L.P_VAULT, name=L.VAULT, target=L.VAULT)
    mt.socket(oside, 13, pool=L.P_ONWARD, name=L.ONWARD, target=L.ONWARD)
    c = 13

    # the floor: a sigil of rings and an eight-pointed star round the middle, a polished border at the walls
    for z in range(2, 25):
        for x in range(2, 25):
            r = G.dist(x, z, c, c)
            dx, dz = abs(x - c), abs(z - c)
            if r <= 1.5:
                mt.set(x, FLOOR, z, L.CHISELED)
            elif r <= 3.5:
                mt.set(x, FLOOR, z, L.POLISHED)
            elif r <= 4.6:
                mt.set(x, FLOOR, z, CRYING)
            elif r <= 8.5:
                mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            elif r <= 9.5:
                mt.set(x, FLOOR, z, L.POLISHED)
            if max(dx, dz) in (10, 11) and min(dx, dz) <= 8:
                mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 else L.CHISELED)
    for dx, dz in ((0, 6), (0, -6), (6, 0), (-6, 0), (4, 4), (4, -4), (-4, 4), (-4, -4)):
        mt.set(c + dx, FLOOR, c + dz, CRYING)
    for dx, dz in ((0, 7), (0, -7), (7, 0), (-7, 0)):
        mt.set(c + dx, FLOOR, c + dz, SEA)

    # four columns carrying a coffered ceiling, with a hanging ring of lanterns round the sigil
    for (cx, cz) in ((6, 6), (20, 6), (6, 20), (20, 20)):
        G.column(mt, cx, cz, FLOOR + 1, TOP - 1)
        G.candle_cluster(mt, cx + (2 if cx < c else -2), FLOOR + 1, cz + (2 if cz < c else -2), "white", 3)
    for z in (6, 20):
        G.beam(mt, 2, z, 24, z, TOP, "x")
    for x in (6, 20):
        G.beam(mt, x, 2, x, 24, TOP, "z")
    G.chandelier_ring(mt, c, c, TOP, 5, 8, 4, soul=True, phase=math.pi / 8)
    for k in range(8):
        a = k * math.pi / 4
        mt.set(c + int(round(math.cos(a) * 9)), TOP + 1, c + int(round(math.sin(a) * 9)), SEA)

    # the walls: lit windows, banners, the memorial niche on the north wall, the two plaques over the roads
    cyan, purple = "minecraft:cyan_stained_glass", "minecraft:purple_stained_glass"
    for side in "ns":
        for lat0, glass in ((3, purple if mirror else cyan), (21, cyan if mirror else purple)):
            G.lancet(mt, side, lat0, 3, 14, 10, glass)
    for side in "ew":
        for lat0, glass in ((3, cyan if mirror else purple), (21, purple if mirror else cyan)):
            G.lancet(mt, side, lat0, 3, 14, 10, glass)
    for side in "ns":
        G.wall_banners(mt, side, 10, 2, 22, ("purple", "black"))
        G.wall_banners(mt, side, 15, 2, 22, ("black", "purple"))
    for side in "ew":
        G.pilaster(mt, side, 6, FLOOR + 1, 21)
        G.pilaster(mt, side, 20, FLOOR + 1, 21)
    road_plaque(mt, vside, 13, "minecraft:red_wool", "red", "minecraft:redstone_lamp")
    road_plaque(mt, oside, 13, "minecraft:blue_wool", "blue", "minecraft:sea_lantern")
    # the memorial niche: two armoured statues and braziers on the north wall, a seal of chiselled stone between them
    for lat in range(11, 16):
        for row in range(14, 19):
            edge = lat in (11, 15) or row in (14, 18)
            G.put_wall(mt, "n", lat, 1, row, L.POLISHED if edge else L.CHISELED if (lat, row) != (13, 16) else CRYING)
    for (sx, face) in ((8, "south"), (18, "south")):
        L.statue(mt, sx, FLOOR + 1, 3, facing=face)
    for bx in (10, 16):
        G.brazier(mt, bx, FLOOR + 1, 3, soul=False)

    # carpets: purple from the entry to the sigil, red to the east door, blue to the west door (flat; the doorways stay clear)
    for z in range(15, 25):
        mt.set(c, FLOOR + 1, z, "minecraft:purple_carpet")
    east, west = range(15, 24), range(3, 12)
    for x in (west if mirror else east):
        mt.set(x, FLOOR + 1, c, "minecraft:red_carpet")
    for x in (east if mirror else west):
        mt.set(x, FLOOR + 1, c, "minecraft:blue_carpet")
    mt.marker(c, FLOOR + 1, c, "patrol")

    mt.finish()
    G.ensure_light(mt)
    return mt


def antechamber_a():
    return antechamber("mega/antechamber/antechamber_a")


def antechamber_b():
    return antechamber("mega/antechamber/antechamber_b", mirror=True)


# ====================================================================================================================
# The Way Onward: straight, left and right corridors, in two styles
# ====================================================================================================================
SKULL_ROTATION = {"south": 0, "west": 4, "north": 8, "east": 12}


class Run:
    """A leg of a corridor seen from its ends: `at(a, u)` is the template cell column at position a along the leg and u across it (u = -2..2)."""

    def __init__(self, leg):
        self.leg = leg

    def at(self, a, u):
        return (self.leg.c + u, a) if self.leg.axis == "z" else (a, self.leg.c + u)

    def toward(self, sign):
        """The compass direction of a step across the leg (+1 / -1)."""
        return ("east" if sign > 0 else "west") if self.leg.axis == "z" else ("south" if sign > 0 else "north")

    def forward(self, sign):
        """The compass direction of a step along the leg (+1 = towards larger a)."""
        return ("south" if sign > 0 else "north") if self.leg.axis == "z" else ("east" if sign > 0 else "west")


def stations(leg, skip_start, skip_end, every=5):
    """The positions along a leg where a rib, a lantern and the wall dressing stand."""
    return list(range(leg.a0 + skip_start, leg.a1 - skip_end + 1, every))


def rib(mt, run, interior, a, block=L.POLISHED):
    """An arch rib across the vault at position a: the first ceiling row, five cells across, in polished stone, with a chiselled keystone."""
    for u in range(-2, 3):
        x, z = run.at(a, u)
        top = interior.get((x, z))
        if top is not None:
            mt.set(x, top + 1, z, L.CHISELED if u == 0 else block)


def sconce(mt, run, interior, a, sign, soul=False):
    """A lantern on a bracket on the wall of the side `sign` (-1 / +1) at position a: a polished slab against the wall and a lantern hanging from its
    underside. (A first version used an upside-down stair; the game lets a lantern hang only from a face that covers the middle of its cell, which a
    stair's step does not, so `mega_arch_check.py` refused it. A bottom slab's underside is a full face.)"""
    x, z = run.at(a, 2 * sign)
    if (x, z) not in interior:
        return
    L_ = L.SOUL_LANTERN if soul else L.LANTERN
    G.slab(mt, x, FLOOR + 5, z, L.POLISHED_SLAB, "bottom")
    mt.set(x, FLOOR + 4, z, L_, {"hanging": "true"})


def collapse(mt, run, interior, a_seal, direction, length=9, heavy=1.0):
    """The collapsed end of a Way Onward: for `length` cells back from the seal (a_seal, `direction` = +1 / -1 along the leg towards the seal) the floor is
    strewn with rubble (slabs and stairs where you walk, blocks at the sides), the ceiling is patched with rougher stone, stalactites and cobwebs hang from it.
    A one cell wide path of slabs and bare floor always stays open down the middle. Deterministic through the template's own random."""
    for i in range(length):
        a = a_seal - direction * (i + 1)
        near = 1.0 - i / float(length)
        p = (0.12 + 0.5 * near) * heavy
        for u in range(-2, 3):
            x, z = run.at(a, u)
            top = interior.get((x, z))
            if top is None:
                continue
            r = mt.rng.random()
            if r < p:
                if abs(u) == 2 or (u != 0 and mt.rng.random() < 0.25):
                    mt.set(x, FLOOR + 1, z, mt.pick(((5, L.COBBLED), (3, L.CRACKED_BRICK), (2, "minecraft:tuff"))))
                    if near > 0.6 and abs(u) == 2 and mt.rng.random() < 0.4:
                        mt.set(x, FLOOR + 2, z, L.COBBLED_SLAB, {"type": "bottom"})
                elif mt.rng.random() < 0.5:
                    mt.set(x, FLOOR + 1, z, L.COBBLED_SLAB, {"type": "bottom"})
                else:
                    G.stair(mt, x, FLOOR + 1, z, L.COBBLED_STAIRS, mt.pick(((1, "north"), (1, "south"), (1, "east"), (1, "west"))))
            if mt.rng.random() < p * 0.9:
                mt.set(x, top + 1, z, mt.pick(((4, L.COBBLED), (3, "minecraft:tuff"), (3, L.CRACKED_TILE))))
            if u == 0 or abs(u) == 1:
                if mt.rng.random() < p * 0.35:
                    mt.set(x, top, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": "tip"})
            if abs(u) == 2 and mt.rng.random() < p * 0.7:
                mt.set(x, top, z, "minecraft:cobweb")


def ossuary_alcove(mt, run, interior, a, sign):
    """A skull niche in the wall of the side `sign` at position a: three cells wide along the wall, two high, cut into the inner wall layer, bone behind a
    skull flanked by two lit candles, and a soul lantern hanging from the wall block over the skull (a lantern cannot stand on a skull: its top is not a
    full face, and `mega_arch_check.py` says so)."""
    for da in (-1, 0, 1):
        x, z = run.at(a + da, 3 * sign)
        bx, bz = run.at(a + da, 4 * sign)
        if (run.at(a + da, 2 * sign)) not in interior:
            return
        for row in (FLOOR + 2, FLOOR + 3):
            mt.set(x, row, z, L.AIR)
            mt.set(bx, row, bz, "minecraft:bone_block", {"axis": "y"})
        if da == 0:
            mt.set(x, FLOOR + 2, z, "minecraft:skeleton_skull", {"rotation": str(SKULL_ROTATION[run.toward(-sign)])})
            mt.set(x, FLOOR + 3, z, L.SOUL_LANTERN, {"hanging": "true"})
        else:
            mt.set(x, FLOOR + 2, z, "minecraft:candle", {"candles": "3", "lit": "true"})


def onward(name, kind, style):
    """One Way Onward template. kind: straight (ends on the north face), right (turns east, ends on the east face) or left (turns west, ends on the west face);
    the entry is always on the south face. style: a (a passage of chains and lanterns), b (an ossuary) or c (the same passage in compact size, under blue
    banners over a blue runner, the colour of the Way Onward's plaque): a and b are 9 x 36 (straight) and 27 x 27 (L); c is 9 x 18 and 18 x 18.
    Why a compact size: the corridor leaves the antechamber at the side of the chain, so where the sector's region is nearly used up on that side only a
    short corridor still fits, and a sector that cannot place its Way Onward cannot grow (the pool offers all sizes and the search takes the first that fits)."""
    compact = style == "c"
    deco = "b" if style == "b" else "a"                                                  # c is dressed like a
    if kind == "straight":
        n = 2 if compact else 4
        mt = MT(name, 1, n)
        last = 9 * n - 3
        legs = [G.Leg("z", 4, 2, last)]
        entry, end, patrol = ("s", 4), ("n", 4), (4, FLOOR + 1, 9 * n // 2 - 1)
        end_leg, end_dir, end_a = legs[0], -1, 2
    elif kind == "right":
        n = 2 if compact else 3
        mt = MT(name, n, n)
        last = 9 * n - 3
        legs = [G.Leg("z", 4, 2, last), G.Leg("x", 4, 2, last)]
        entry, end, patrol = ("s", 4), ("e", 4), (4, FLOOR + 1, 4)
        end_leg, end_dir, end_a = legs[1], 1, last
    else:
        n = 2 if compact else 3
        mt = MT(name, n, n)
        last = 9 * n - 3
        ec = 9 * (n - 1) + 4                                                             # the lateral centre of the entry: the east module column
        legs = [G.Leg("z", ec, 2, last), G.Leg("x", 4, 2, last)]
        entry, end, patrol = ("s", ec), ("w", 4), (ec, FLOOR + 1, 4)
        end_leg, end_dir, end_a = legs[1], -1, 2
    collapse_len = 6 if compact else 9
    interior = G.corridor_shell(mt, legs, wear=0.20 if deco == "b" else 0.14)
    mt.socket(entry[0], entry[1], pool=L.P_EMPTY, name=L.ONWARD, target=L.ONWARD)
    mt.socket(end[0], end[1], pool=L.P_EMPTY, name=L.DOOR, target=L.DOOR, final_state=L.COBBLED, hole=False)

    banner_colours = ("blue", "white", "blue") if compact else ("purple", "black", "blue")
    runs = [Run(leg) for leg in legs]
    for run in runs:
        leg = run.leg
        in_elbow = lambda a: any(o is not leg and o.contains(*run.at(a, 0)) for o in legs)     # noqa: E731
        for a in stations(leg, 3, 3):
            if in_elbow(a) or in_elbow(a - 1) or in_elbow(a + 1):
                continue
            rib(mt, run, interior, a)
            sconce(mt, run, interior, a, -1, soul=(deco == "b"))
            sconce(mt, run, interior, a, 1, soul=(deco == "b"))
            if deco == "a":
                x, z = run.at(a + 2, 0)
                if (x, z) in interior:
                    G.hang_lantern(mt, x, interior[(x, z)], z, 2)
                for sign in (-1, 1):
                    bx, bz = run.at(a + 2, 2 * sign)
                    if (bx, bz) in interior:
                        mt.set(bx, FLOOR + 5, bz, "minecraft:%s_wall_banner" % banner_colours[(a // 5) % 3], {"facing": run.toward(-sign)})
            else:
                ossuary_alcove(mt, run, interior, a + 2, -1)
                ossuary_alcove(mt, run, interior, a + 2, 1)
        if deco == "a":                                                                    # a runner down the middle, except in the collapse
            for a in range(leg.a0, leg.a1 + 1):
                x, z = run.at(a, 0)
                near_seal = leg is end_leg and abs(a - end_a) <= collapse_len
                if not near_seal and not any(o is not leg and o.contains(x, z) for o in legs):
                    mt.set(x, FLOOR + 1, z, "minecraft:blue_carpet" if compact else "minecraft:black_carpet")
    collapse(mt, Run(end_leg), interior, end_a - end_dir, end_dir, length=collapse_len, heavy=1.0 if deco == "a" else 0.7)
    mt.marker(patrol[0], patrol[1], patrol[2], "patrol")
    mt.finish()
    cell = G.rubble_seal(mt, end[0], end[1])
    mt.marker(cell[0], cell[1], cell[2], "expansion_socket", facing=G.DIRS[end[0]], w=3, h=4, d=2)
    G.ensure_light(mt, level=12)                                   # the collapse and the ossuary niches leave dark stretches: cover them
    mt.leak_start = (patrol[0], patrol[1] + 1, patrol[2])
    return mt


# ====================================================================================================================
# self-checks (what a game would tell us)
# ====================================================================================================================
def _marker_cells(mt, kind):
    out = []
    for pos, c in mt.t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None and str(c.nbt.get("metadata", "")).split(";")[0] == "nusmp:" + kind:
            out.append((pos, str(c.nbt.get("metadata", ""))))
    return out


def _is_air(c):
    return c is None or c.block in ("minecraft:air", "minecraft:structure_block", "minecraft:light")      # a light block has no collision: a Guardian walks through it


def verify(name, mt):
    """Problems with a finished template, as a list of strings (empty = fine). It checks what the Java side and the in-game self-tests rely on: closed rooms,
    reachable doorways, the Gatekeeper slab, the altar's headroom, the markers and their arguments, a clear lane for the trait test."""
    problems = []
    t = mt.t
    kind = name.split("/")[1]
    if kind == "arena":
        start = (C, FLOOR + 6, C)
    elif kind == "antechamber":
        start = (13, FLOOR + 2, 13)
    else:
        start = mt.leak_start
    leaks, seen = G.leak_check(mt, start)
    if leaks:
        problems.append("%d cells leak out of the template through something other than a declared open doorway, e.g. %s" % (len(leaks), leaks[0]))
    for side, offset, kw in mt.sockets:                                   # every open doorway is reachable from inside
        if kw["hole"] and kw["final_state"] == L.AIR:
            cx, cy, cz = G.cell(mt, side, offset, 1, L.DOOR_ROW + 1)
            if (cx, cy, cz) not in seen:
                problems.append("the doorway on %s at %d is not reachable from inside the room" % (side, offset))
    jigs = t.jigsaws()
    if kind == "arena":
        altar = [pos for pos, c in t.cells.items() if c.block == "nusmp:guardian_altar"]
        if altar != [(C, FLOOR + 3, C)]:
            problems.append("the Guardian Altar must be the one block at the centre, row %d (found %s)" % (FLOOR + 3, altar))
        spawn = _marker_cells(mt, "guardian_spawn")
        if [p for p, _ in spawn] != [(C, FLOOR + 4, C)]:
            problems.append("guardian_spawn must be the one marker in the cell above the altar (found %s)" % (spawn,))
        for k in range(1, 9):
            if not _is_air(t.get(C, FLOOR + 3 + k, C)):
                problems.append("the cell %d above the altar is filled: the colossal Guardian needs room" % k)
                break
        gate = _marker_cells(mt, "gatekeeper")
        if gate != [((C, FLOOR + 2, 2), "nusmp:gatekeeper;facing=north")]:
            problems.append("the gatekeeper marker must be at (%d, %d, 2) facing north (found %s)" % (C, FLOOR + 2, gate))
        doors = [pos for pos, c in t.cells.items() if c.block == "nusmp:gatekeeper_door"]
        if len(doors) != 23:
            problems.append("the Gatekeeper slab has %d blocks besides its jigsaw cell, expected 23" % len(doors))
        if any(c.props != {"kind": "sector", "open": "false", "half": "lower" if pos[1] < FLOOR + 3 else "upper"} for pos, c in ((p, t.get(*p)) for p in doors)):
            problems.append("a Gatekeeper block has the wrong state (kind=sector, open=false, half by row)")
        if len(jigs) != 2:
            problems.append("an arena has exactly two sockets (entry and exit), found %d" % len(jigs))
        if G.lane_check(mt) is None:
            problems.append("no clear 3 x 11 lane of floor with 8 free rows above it: the in-game trait self-test would fail")
        if _marker_cells(mt, "spawn") or _marker_cells(mt, "patrol"):
            problems.append("the Guardian's hall carries no spawn or patrol markers")
    elif kind == "antechamber":
        if len(jigs) != 3:
            problems.append("the antechamber has three sockets (entry, vault, onward), found %d" % len(jigs))
        names = sorted(str(d["name"]) for _, _, d in jigs)
        if names != sorted([L.CHAIN, L.VAULT, L.ONWARD]):
            problems.append("socket names %s are not chain, vault, onward" % names)
        if len(_marker_cells(mt, "patrol")) != 1:
            problems.append("one patrol marker expected")
        if _marker_cells(mt, "spawn"):
            problems.append("a calm lobby carries no spawn markers")
    else:
        if len(jigs) != 2:
            problems.append("a Way Onward has two sockets (entry and sealed end), found %d" % len(jigs))
        exp = _marker_cells(mt, "expansion_socket")
        if len(exp) != 1:
            problems.append("one expansion_socket marker expected, found %d" % len(exp))
        else:
            (mx, my, mz), meta = exp[0]
            below = t.get(mx, my - 1, mz)
            end = [j for j in jigs if str(j[2]["pool"]) == L.P_EMPTY and str(j[2]["final_state"]) != L.AIR]
            if len(end) != 1 or (mx, my - 1, mz) != end[0][0]:
                problems.append("the expansion_socket marker must stand directly above the end socket's jigsaw cell (found %s, end %s)" % ((mx, my - 1, mz), [e[0] for e in end]))
            if "w=3" not in meta or "h=4" not in meta or "d=2" not in meta or "facing=" not in meta:
                problems.append("expansion_socket needs facing, w=3, h=4, d=2: %s" % meta)
            if below is None or below.block != "minecraft:jigsaw":
                problems.append("the cell below the expansion_socket marker is not the jigsaw")
        if len(_marker_cells(mt, "patrol")) != 1:
            problems.append("one patrol marker expected")
    return problems


# ====================================================================================================================
# registry of templates
# ====================================================================================================================
BUILDERS = {
    "mega/arena/guardian_hall_a": hall_a,
    "mega/arena/guardian_hall_b": hall_b,
    "mega/antechamber/antechamber_a": antechamber_a,
    "mega/antechamber/antechamber_b": antechamber_b,
    "mega/onward/straight_a": lambda: onward("mega/onward/straight_a", "straight", "a"),
    "mega/onward/straight_b": lambda: onward("mega/onward/straight_b", "straight", "b"),
    "mega/onward/straight_c": lambda: onward("mega/onward/straight_c", "straight", "c"),
    "mega/onward/left_c": lambda: onward("mega/onward/left_c", "left", "c"),
    "mega/onward/right_c": lambda: onward("mega/onward/right_c", "right", "c"),
    "mega/onward/left_a": lambda: onward("mega/onward/left_a", "left", "a"),
    "mega/onward/left_b": lambda: onward("mega/onward/left_b", "left", "b"),
    "mega/onward/right_a": lambda: onward("mega/onward/right_a", "right", "a"),
    "mega/onward/right_b": lambda: onward("mega/onward/right_b", "right", "b"),
}


def build(name):
    mt = BUILDERS[name]()
    return mt


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--only", metavar="NAME", help="build one template (name relative to structure/, e.g. mega/arena/guardian_hall_a)")
    ap.add_argument("--check", action="store_true", help="exit 1 when a file on disk differs from what this script builds")
    ap.add_argument("--list", action="store_true", help="list the templates this script builds")
    ap.add_argument("--ascii", metavar="NAME", help="print the ASCII layers of one template (rows 8..14)")
    a = ap.parse_args(argv)
    names = [a.only] if a.only else sorted(BUILDERS)
    if a.list:
        for n in sorted(BUILDERS):
            print(n)
        return 0
    if a.ascii:
        print(build(a.ascii).ascii(L.FLOOR_ROW, L.FLOOR_ROW + 6))
        return 0
    bad = 0
    for n in names:
        mt = build(n)
        problems = verify(n, mt)
        for p_ in problems:
            print("PROBLEM %s: %s" % (n, p_))
        if problems:
            bad += 1
            continue
        if a.check:
            import tempfile
            with tempfile.TemporaryDirectory() as tmp:
                p = mt.save(tmp)
                disk = mt.t.path(L.RES)
                same = os.path.exists(disk) and open(p, "rb").read() == open(disk, "rb").read()
            print("%-4s %s" % ("ok" if same else "DIFF", n))
            bad += 0 if same else 1
        else:
            p = mt.save()
            print("wrote %s (%d blocks)" % (os.path.relpath(p, L.ROOT), len(mt.t.cells)))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

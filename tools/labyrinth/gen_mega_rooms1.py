#!/usr/bin/env python3
"""
ROOMS-1 of the Mega-Dungeon (1.54.0, docs/labyrinth/mega.md, docs/labyrinth/mega_rooms1.md): the furnished halls (`structure/mega/hall/*`: cell blocks,
torture chambers, crypts, an ossuary, libraries, alchemy labs, armory forges, a kitchen larder, barracks, shrines, a great hall) and the treasure closets
(`structure/mega/treasure/*`).

    python3 tools/labyrinth/gen_mega_rooms1.py                  # (re)writes every template
    python3 tools/labyrinth/gen_mega_rooms1.py crypt_a library_b  # only these
    python3 tools/labyrinth/gen_mega_rooms1.py --check          # regenerates in memory, compares with the files on disk, runs every template check
    python3 tools/labyrinth/gen_mega_rooms1.py --list

Why a generator and not hand-placed structure blocks: twenty-two rooms of 20-60 thousand cells must agree on the connector contract (module grid, doorway
holes, jigsaw cells), must be reproducible byte for byte, and must be re-themable (the Region Theme swaps the palette families, so every piece of architecture
here is a palette constant). Every room is furnished with the kit in `mega_decor_r1.py`, which refuses to put decor in a doorway or a declared route and checks
the finished template (walk graph, 3-wide routes, block light, see-through leaks, floating decor, marker sanity) without a game.

Deterministic: every template draws from its own name-seeded random (`MT.rng`), so the same script writes the same bytes.
"""
import argparse
import hashlib
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

ROOMS = {}
LOOT_TRIAL = "nusmp:chests/labyrinth_trial"


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


# ===================================================================================================================
# helpers shared by the rooms of this file
# ===================================================================================================================
def spawn(h, x, z, weight=1, y=GROUND):
    if weight > 1:
        h.marker(x, y, z, "spawn", weight=weight)
    else:
        h.marker(x, y, z, "spawn")


def patrol(h, x, z, y=GROUND):
    h.marker(x, y, z, "patrol")


def rot_for(facing):
    return {"south": 0, "west": 4, "north": 8, "east": 12}[facing]


# ===================================================================================================================
# 1. cell_block_a: the Warden's Row
# ===================================================================================================================
def cell_row(h, side, groups, kinds):
    """Cells along the `side` wall: brick dividers, a polished column and an arched band over every barred front, a stone roof over every cell, and a
    dressed cell behind each front. `groups` is a list of divider-lat tuples (one tuple per run of cells), `kinds` the door state per cell in order:
    'open' (an iron door swung open), 'locked' (closed), 'broken' (bars torn out, a dent dug in the back wall). Why: the reference interior's columns with
    iron bars between them, made into cells a person could be kept in; the roof and the arched band keep the cells from reading as tall shafts."""
    w = h.wall(side)
    top = h.top
    FRONT = 4
    ROOF = GROUND + 5
    axis = "z" if side in ("w", "e") else "x"
    dividers = sorted({lat for g in groups for lat in g})
    cells = [a + 1 for g in groups for a in g[:-1]]
    for lat in dividers:
        for d in range(FRONT):
            for y in range(GROUND, top + 1):
                w.set(lat, y, d, D.brick_mix(h))
        for y in range(GROUND, top + 1):
            w.set(lat, y, FRONT, L.CHISELED if y in (ROOF, top) else L.POLISHED)
        w.set(lat, GROUND, FRONT + 1, L.POLISHED_STAIRS, {"facing": w.out, "half": "bottom"})
        w.set(lat, ROOF, FRONT + 1, L.POLISHED_STAIRS, {"facing": w.out, "half": "top"})
        w.set(lat, top, FRONT + 1, L.POLISHED_STAIRS, {"facing": w.out, "half": "top"})
    for i, a in enumerate(cells):
        kind = kinds[i % len(kinds)]
        dl = a + 1
        for lat in range(a, a + 3):
            # the stone roof over the cell and the arched band over its front
            for d in range(FRONT):
                w.set(lat, ROOF, d, L.BRICK)
            if lat == a:
                w.set(lat, ROOF, FRONT, L.BRICK_STAIRS, {"facing": w.dec, "half": "top"})
            elif lat == a + 2:
                w.set(lat, ROOF, FRONT, L.BRICK_STAIRS, {"facing": w.inc, "half": "top"})
            else:
                w.set(lat, ROOF, FRONT, L.POLISHED)
            for y in range(ROOF + 1, top):
                w.set(lat, y, FRONT, D.brick_mix(h))
            w.set(lat, top, FRONT, L.STRIPPED_LOG, {"axis": axis})
            # the bars
            for y in range(GROUND, ROOF):
                if lat == dl and y < GROUND + 2:
                    continue
                if kind == "broken" and ((lat == a + 2 and GROUND + 1 <= y <= GROUND + 3) or (lat == a and GROUND + 2 <= y <= GROUND + 3)):
                    continue
                w.set(lat, y, FRONT, L.IRON_BARS)
        if kind in ("open", "locked"):
            hinge = "left" if (i % 2 == 0) else "right"
            props = {"facing": w.out if kind == "locked" else w.into, "hinge": hinge, "open": kind == "open", "powered": False}
            w.set(dl, GROUND, FRONT, "minecraft:iron_door", dict(props, half="lower"))
            w.set(dl, GROUND + 1, FRONT, "minecraft:iron_door", dict(props, half="upper"))
        # the cell inside: floor, bed, bucket, shackles, light, bones, cobweb
        for lat in range(a, a + 3):
            for d in range(FRONT):
                x, y, z = w.cell(lat, FLOOR, d)
                h.floor_set(x, z, L.COBBLED if (lat + d) % 3 else L.CRACKED_TILE)
        bale_axis = "z" if side in ("w", "e") else "x"
        w.set(a, GROUND, 0, "minecraft:hay_block", {"axis": bale_axis})
        w.set(a + 1, GROUND, 0, "minecraft:hay_block", {"axis": bale_axis})
        w.set(a, GROUND, 1, "minecraft:hay_block", {"axis": "y"})
        w.set(a + 2, GROUND, 0, "minecraft:cauldron")
        if kind == "broken":
            for y in (GROUND, GROUND + 1):
                w.carve(dl, y, -1)
            x, y, z = w.cell(dl, GROUND, 1)
            D.rubble(h, x, z)
        else:
            D.window_slit(w, dl, GROUND + 3, GROUND + 4)
        for lat in (a, a + 2):
            w.set(lat, GROUND + 4, 0, L.IRON_BARS)
            w.set(lat, GROUND + 3, 0, L.CHAIN_BLOCK, {"axis": "y"})
            w.set(lat, GROUND + 2, 0, L.CHAIN_BLOCK, {"axis": "y"})
        x, y, z = w.cell(dl, GROUND, 2)
        D.hang_lantern(h, x, z, 2, y_top=ROOF - 1, soul=(kind != "open"))
        if i % 2 == 0:
            x, y, z = w.cell(a + 2, GROUND, 2)
            D.bones(h, x, z, rot=3 * i, big=True)
        else:
            x, y, z = w.cell(a, GROUND, 3)
            D.bones(h, x, z, rot=5 * i)
        D.web(h, *w.cell(a + 2, ROOF - 1, 0))


def runner(h, x1, z1, x2, z2, axis, colors=("black", "red", "black"), skip=()):
    """A three-wide carpet runner (borders and a centre stripe) over [x1..x2] x [z1..z2] where the floor is still free; `skip` lists rectangles
    (xa, za, xb, zb) it leaves bare (a drain grate must not be carpeted over)."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            if any(a <= x <= c and b <= z <= d for (a, b, c, d) in skip):
                continue
            k = (x - x1) if axis == "z" else (z - z1)
            col = colors[min(k, len(colors) - 1)]
            h.try_set(x, GROUND, z, "minecraft:%s_carpet" % col)


def guard_post(h):
    """The guards' end of the cell block: a desk against the north wall (a ledger lectern as its end), a key board, chairs, a rack of spears with two
    guard armours standing in front of it, banners. Why: a cell block is a place someone watches; the party meets the watchers' things first."""
    w = h.wall("n")
    f = h.frame(8, GROUND, 2, "east")
    D.table(f, 2)
    D.lectern(h.frame(10, GROUND, 2, "north"))
    h.set(8, GROUND + 1, 2, L.LANTERN, {"hanging": False})
    h.marker(9, GROUND + 1, 2, "furniture", **{"as": "table"})
    D.chair(h.frame(9, GROUND, 3, "north"), seat_marker=True)
    D.chair(h.frame(10, GROUND, 3, "north"))
    for lat in range(8, 11):
        for y in (GROUND + 2, GROUND + 3):
            w.set(lat, y, 0, L.PLANKS)
            w.set(lat, y, 1, "minecraft:tripwire_hook", {"facing": w.into, "attached": False, "powered": False})
    D.spear_rack(w, 16, 3)
    h.armor_stand(16, GROUND, 4, yaw=180.0, head="minecraft:iron_helmet", chest="minecraft:chainmail_chestplate", legs="minecraft:chainmail_leggings",
                  mainhand="minecraft:iron_sword")
    h.armor_stand(18, GROUND, 4, yaw=180.0, head="minecraft:chainmail_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
                  feet="minecraft:iron_boots", mainhand="minecraft:stone_axe")
    w.set(17, GROUND + 4, 0, "minecraft:red_wall_banner", {"facing": w.into})
    w.set(9, GROUND + 4, 0, "minecraft:black_wall_banner", {"facing": w.into})


def torture_corner(h):
    """The other end: a torture chair in the south-west with blood round it, a tool bench (anvil, grindstone, quench cauldron, a barrel), crates, a gutter
    running to the drain. Why: the brief's torture-chair corner; the gutter links it to the drain so the room reads as one working place."""
    ws = h.wall("s")
    D.torture_chair(h.frame(9, GROUND, 23, "north"))
    D.blood(h, [(8, 22), (9, 21), (10, 22), (7, 22), (8, 21), (10, 21), (9, 20), (8, 20), (11, 21), (10, 23), (11, 22)])
    D.candles_at(h, 7, GROUND, 23, 2, "red")
    # the bench
    h.set(15, GROUND, 24, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(16, GROUND, 24, "minecraft:anvil", {"facing": "north"})
    h.set(17, GROUND, 24, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    h.set(18, GROUND, 24, "minecraft:water_cauldron", {"level": 3})
    D.bracket_lantern(ws, 16, GROUND + 3)
    # crates and barrels in the south-east
    for (x, z, hgt) in ((17, 21, 2), (18, 21, 1), (18, 22, 1), (17, 22, 1)):
        for k in range(hgt):
            h.set(x, GROUND + k, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(18, GROUND, 23, "minecraft:chest", {"facing": "west", "type": "single"})
    # the gutter from the chair to the drain
    for z in range(15, 23):
        h.floor_set(9, z, L.COBBLED)
    for x in range(9, 12):
        h.floor_set(x, 15, L.COBBLED)
    h.floor_set(11, 14, L.COBBLED)
    h.floor_set(11, 13, L.COBBLED)


@room("cell_block_a")
def cell_block_a():
    h = Hall("mega/hall/cell_block_a", 3, 3, 9)
    for side in "nsew":
        h.door(side, 13)
    h.route(12, 2, 14, 24, label="aisle")
    h.route(2, 12, 24, 14, label="crosswalk")
    for side, kinds in (("w", ["open", "locked", "broken", "open"]), ("e", ["locked", "open", "open", "broken"])):
        cell_row(h, side, [(2, 6, 10), (16, 20, 24)], kinds)
    D.drain(h, 12, 12)
    for (bx, bz) in ((10, 10), (16, 10), (10, 16), (16, 16)):
        D.brazier(h, bx, bz)
    D.chandelier_wheel(h, 13, 13, r=2, drop=3)
    D.hang_lantern(h, 13, 6, 3)
    D.hang_lantern(h, 13, 20, 3)
    D.hanging_cage(h, 16, 6, GROUND + 2)
    guard_post(h)
    torture_corner(h)
    # lanterns on the cell columns, niches with skulls and candles, banners
    for side in ("w", "e"):
        w = h.wall(side)
        for lat in (6, 10, 16, 20):
            D.bracket_lantern(w, lat, GROUND + 3, arm=1, d0=5)
    for side, lats in (("n", (7, 19)), ("s", (8, 18))):
        w = h.wall(side)
        for lat in lats:
            D.niche(w, lat, GROUND + 3, GROUND + 4, width=1)
            x, y, z = w.cell(lat, GROUND + 3, -1)
            D.candles_at(h, x, y, z, 2, "red")
            x, y, z = w.cell(lat, GROUND + 4, -1)
            h.set(x, y, z, "minecraft:skeleton_wall_skull", {"facing": w.into})
    runner(h, 12, 2, 14, 24, "z", skip=[(12, 12, 14, 14)])
    runner(h, 2, 12, 24, 14, "x", skip=[(12, 12, 14, 14)])
    D.beams(h, "x", [6, 20])
    # the upper third of the walls: dark barred slits, tall black banners, a lantern bracket now and then
    D.upper_dress(h, [D.it_high_slit(None, rows=2, drop=2), D.it_tall_banner("black", hang=2), D.it_high_bracket(drop=2)], sides="nsew", step=(4, 6))
    h.faction("cultist", 11, 15)
    D.auto_light(h)
    D.auto_markers(h)
    D.cornice(h)
    D.plinth(h)
    for side in "nsew":
        D.door_trim(h, side, 13)
    return h


def finish_common(h, trims=True, cornice=True, plinth=True, webs=True, band=GROUND + 4):
    """The finishing pass every hall gets, after its furniture: door trims on every doorway, a string course along the wall at the height of the door heads,
    crown moulding, a skirting where the floor is free, cobwebs in the upper corners."""
    if trims:
        for (side, off) in h.doors:
            D.door_trim(h, side, off)
    if band:
        D.wall_band(h, band)
    if cornice:
        D.cornice(h)
    if plinth:
        D.plinth(h)
    if webs:
        D.cobwebs(h, 0.8)


# ===================================================================================================================
# 2. torture_chamber_a: the Question Room
# ===================================================================================================================
@room("torture_chamber_a")
def torture_chamber_a():
    h = Hall("mega/hall/torture_chamber_a", 2, 3, 8, wear=0.14)
    h.door("n", 13)
    h.door("s", 4)
    h.door("e", 13)
    h.route(12, 2, 14, 16, label="north aisle")
    h.route(11, 12, 15, 14, label="east crossing")
    h.route(3, 17, 15, 19, label="south band")
    ws, wn, ww, we = h.wall("s"), h.wall("n"), h.wall("w"), h.wall("e")
    # the west wall is the row of machines: the iron maiden and three chairs; the rack stands in the middle of the west half
    D.iron_maiden(ww, 3)
    for z in (7, 10, 13):
        D.torture_chair(h.frame(3, GROUND, z, "east"))
    D.stretching_rack(h.frame(8, GROUND, 8, "south"), 7)
    # the inquisitors' table under the north wall
    D.table(h.frame(5, GROUND, 2, "east"), 3)
    D.lectern(h.frame(8, GROUND, 2, "north"))
    h.set(5, GROUND + 1, 2, L.SOUL_LANTERN, {"hanging": False})
    h.marker(6, GROUND + 1, 2, "furniture", **{"as": "table"})
    D.skull_at(h, 7, GROUND + 1, 2, rot=2)
    D.chair(h.frame(6, GROUND, 3, "north"), seat_marker=True)
    D.chair(h.frame(7, GROUND, 3, "north"))
    # the south-east: a chain bed, the brazier with its tongs, the tool bench; two cages over the east aisle
    D.hanging_bed(h, 10, 4, "south")
    D.hanging_bed(h, 12, 20, "south", color="black")
    D.hanging_cage(h, 7, 21, GROUND + 2)
    D.hanging_cage(h, 13, 7, GROUND + 3)
    D.hanging_cage(h, 13, 10, GROUND + 3, skull=False)
    D.brazier(h, 14, 23)
    h.set(11, GROUND, 24, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(12, GROUND, 24, "minecraft:anvil", {"facing": "north"})
    h.set(13, GROUND, 24, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    h.set(10, GROUND, 24, "minecraft:water_cauldron", {"level": 2})
    # the drain and the gutter that runs to it past the rack
    D.drain(h, 9, 15)
    for z in range(8, 15):
        h.floor_set(10, z, L.COBBLED)
    h.floor_set(9, 14, L.COBBLED)
    # light: cold soul lanterns over the machines, warm lanterns over the aisles, the brazier
    for (x, z) in ((9, 8), (10, 12), (5, 18), (10, 7)):
        D.hang_lantern(h, x, z, 2, soul=True)
    D.hang_lantern(h, 5, 11, 3)
    D.hang_lantern(h, 13, 13, 3)
    D.hang_lantern(h, 13, 18, 3)
    D.hang_lantern(h, 12, 4, 3)
    # wall rhythm along what is still free
    D.dress_wall(h, "n", [D.it_skull_sconce(), D.it_banner("red"), D.it_wall_torch(), D.it_candle_niche()], lo=3, hi=11)
    D.dress_wall(h, "s", [D.it_skull_sconce(), D.it_hooks(), D.it_banner("black"), D.it_wall_torch()], lo=6, hi=15)
    D.dress_wall(h, "w", [D.it_wall_torch(), D.it_banner("black"), D.it_shackles(), D.it_skull_sconce(), D.it_candle_niche()], lo=15, hi=23)
    D.dress_wall(h, "e", [D.it_skull_sconce(), D.it_shackles(), D.it_wall_torch(), D.it_hooks(), D.it_banner("red"), D.it_candle_niche()], lo=3, hi=22)
    D.blood(h, [(5, 7), (4, 7), (4, 8), (4, 10), (5, 10), (5, 11), (4, 13), (5, 13), (4, 14), (9, 8), (9, 9), (11, 9), (9, 10), (11, 12), (8, 15),
                (7, 16), (13, 8), (13, 6), (12, 8), (13, 10), (12, 9), (11, 22), (12, 22), (12, 23), (10, 23), (6, 5), (5, 5), (6, 6)])
    D.clutter(h, [D.cl_bones, D.cl_barrels, D.cl_rubble, D.cl_candles], 9)
    D.wall_vines(h, "w", range(15, 24), p=0.4)
    D.beams(h, "x", [5, 11, 17, 23])
    finish_common(h)
    h.faction("demon", 9, 18)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 3. crypt_a: the Catacomb Hall
# ===================================================================================================================
def ossuary_pit(h, x1, z1, x2, z2, floor_row=3, ramp_from="s"):
    """A sunken pit in the floor filled with bones, `x1..x2` by `z1..z2` (the cavity), `FLOOR - floor_row` deep. The cavity is lined (floor and a ring of
    cells) so no void shows; a ramp of bone blocks climbs out on the `ramp_from` side; a fence runs round the rim with a gap at the ramp. Why: the brief's
    sunken ossuary pit using the 8 rows under the floor, with a walkway kept (the ramp and the open rim)."""
    ring = [(x, z) for z in range(z1 - 1, z2 + 2) for x in range(x1 - 1, x2 + 2) if not (x1 <= x <= x2 and z1 <= z <= z2)]
    for (x, z) in ring:
        for y in range(floor_row + 1, FLOOR):
            h.set(x, y, z, D.brick_mix(h, 0.25, 0.25, L.COBBLED))
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            h.set(x, floor_row, z, L.COBBLED)
            h.carve(x, FLOOR, z)
    depth = FLOOR - floor_row
    horizontal = ramp_from in ("e", "w")
    length = (x2 - x1 + 1) if horizontal else (z2 - z1 + 1)
    lats = range(z1, z2 + 1) if horizontal else range(x1, x2 + 1)
    for k in range(length):
        top_row = FLOOR - 1 - min(k, depth - 1)       # k = distance from the ramp end
        for lat in lats:
            if ramp_from == "s":
                x, z = lat, z2 - k
            elif ramp_from == "n":
                x, z = lat, z1 + k
            elif ramp_from == "e":
                x, z = x2 - k, lat
            else:
                x, z = x1 + k, lat
            for y in range(floor_row + 1, top_row + 1):
                h.set(x, y, z, "minecraft:bone_block", {"axis": h.rng.choice(["x", "y", "z"])})
            if h.rng.random() < 0.45:
                h.try_set(x, top_row + 1, z, "minecraft:skeleton_skull", {"rotation": str(h.rng.randrange(16))})
    far = {"s": ((x1 + x2) // 2, z1), "n": ((x1 + x2) // 2, z2), "e": (x1, (z1 + z2) // 2), "w": (x2, (z1 + z2) // 2)}[ramp_from]
    h.set(far[0], floor_row + 1, far[1], L.SOUL_LANTERN, {"hanging": False}, over=True)
    # soul lanterns set into the pit's side walls beside the ramp (the lantern stands in the lining, its light falls on the steps): the ramp is made of bone blocks,
    # which stop light, so the one lantern at the foot of the pit left the middle of the ramp in the dark (the shared light check found block light 3 there)
    for k in (1, 3):
        if k >= length:
            continue
        top_row = FLOOR - 1 - min(k, depth - 1)
        for edge in (min(lats) - 1, max(lats) + 1):
            if ramp_from == "s":
                x, z = edge, z2 - k
            elif ramp_from == "n":
                x, z = edge, z1 + k
            elif ramp_from == "e":
                x, z = x2 - k, edge
            else:
                x, z = x1 + k, edge
            if top_row + 1 < FLOOR:
                h.set(x, top_row + 1, z, L.SOUL_LANTERN, {"hanging": False}, over=True)
                # the lantern is see-through: whatever lies behind it must be stone (the lining may be the only skin between the pit and the void under the floor)
                ox, oz = (0, 0)
                if ramp_from in ("s", "n"):
                    ox = -1 if edge == min(lats) - 1 else 1
                else:
                    oz = -1 if edge == min(lats) - 1 else 1
                if h.free(x + ox, top_row + 1, z + oz):
                    h.set(x + ox, top_row + 1, z + oz, L.COBBLED)
    for (x, z) in ring:
        gap = (ramp_from == "s" and z == z2 + 1 and x1 <= x <= x2) or (ramp_from == "n" and z == z1 - 1 and x1 <= x <= x2) or \
              (ramp_from == "e" and x == x2 + 1 and z1 <= z <= z2) or (ramp_from == "w" and x == x1 - 1 and z1 <= z <= z2)
        if not gap:
            h.try_set(x, GROUND, z, L.FENCE)


@room("crypt_a")
def crypt_a():
    h = Hall("mega/hall/crypt_a", 3, 3, 10, wear=0.16)
    for side in "nsew":
        h.door(side, 13)
    h.route(12, 2, 14, 24, label="nave")
    h.route(2, 12, 24, 14, label="crosswalk")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # a pattern in the floor: a polished processional way with a chiselled medallion where the ways cross
    for z in range(2, 25):
        for x in range(12, 15):
            h.floor_set(x, z, L.POLISHED if (x == 13 or z % 4 != 0) else L.CHISELED)
    for x in range(2, 25):
        for z in range(12, 15):
            if not (12 <= x <= 14):
                h.floor_set(x, z, L.POLISHED if z == 13 else L.TILE)
    for (x, z) in ((12, 12), (14, 12), (12, 14), (14, 14), (13, 13)):
        h.floor_set(x, z, L.CHISELED)
    # the colonnade of the nave
    for x in (9, 17):
        for z in (5, 9, 17, 21):
            D.column(h, x, z)
    # sarcophagi in both aisles (some open), the ossuary pit in the north-east corner
    for z, op in ((3, False), (7, True), (16, False), (20, False)):
        D.sarcophagus(h.frame(4, GROUND, z, "south"), 4, 2, open_lid=op)
    for z, op in ((16, True), (20, False)):
        D.sarcophagus(h.frame(23, GROUND, z, "south"), 4, 2, open_lid=op)
    ossuary_pit(h, 21, 4, 23, 9, ramp_from="s")
    # the priest's corner by the north door
    D.lectern(h.frame(10, GROUND, 2, "north"))
    D.candelabra(h, 9, 2)
    D.candelabra(h, 11, 2)
    # light: soul chandeliers in the nave, candles round the dead, soul lanterns on the walls
    D.chandelier_wheel(h, 13, 7, r=2, drop=5, soul=True)
    D.chandelier_wheel(h, 13, 19, r=2, drop=5, soul=True)
    D.chandelier_wheel(h, 13, 13, r=2, drop=5, soul=True, candles=True)
    D.hang_lantern(h, 13, 3, 3, soul=True)
    D.hang_lantern(h, 13, 23, 3, soul=True)
    D.hang_lantern(h, 5, 13, 3, soul=True)
    D.hang_lantern(h, 21, 13, 3, soul=True)
    for (x, z) in ((7, 3), (7, 10), (7, 16), (7, 23), (19, 16), (19, 23), (19, 3), (19, 10)):
        if h.can_set(x, GROUND, z) and h.can_set(x, GROUND + 1, z):
            D.candelabra(h, x, z, soul=True)
    for z in (5, 9, 18, 22):
        ww.set(z, GROUND + 4, 0, "minecraft:purple_wall_banner", {"facing": ww.into})
    for z in (18, 22):
        we.set(z, GROUND + 4, 0, "minecraft:purple_wall_banner", {"facing": we.into})
    D.loculi(h, "w", range(3, 11, 2), (GROUND + 4, GROUND + 6))
    D.loculi(h, "w", range(16, 24, 2), (GROUND + 4, GROUND + 6))
    D.loculi(h, "e", range(3, 11, 2), (GROUND + 4, GROUND + 6))
    D.loculi(h, "e", range(16, 24, 2), (GROUND + 4, GROUND + 6))
    D.dress_wall(h, "n", [D.it_candle_niche("white"), D.it_banner("purple"), D.it_skull_sconce(), D.it_wall_torch(soul=True)], lo=3, hi=9)
    D.dress_wall(h, "n", [D.it_banner("black"), D.it_skull_sconce(), D.it_candle_niche("white")], lo=17, hi=23)
    D.dress_wall(h, "s", [D.it_candle_niche("white"), D.it_banner("purple"), D.it_skull_sconce(), D.it_wall_torch(soul=True)], lo=3, hi=11)
    D.dress_wall(h, "s", [D.it_banner("black"), D.it_skull_sconce(), D.it_candle_niche("white")], lo=15, hi=23)
    D.dress_wall(h, "w", [D.it_banner("purple", y=GROUND + 3, hang=1), D.it_wall_torch(soul=True)], lo=3, hi=10)
    D.dress_wall(h, "w", [D.it_banner("purple", y=GROUND + 3, hang=1), D.it_wall_torch(soul=True)], lo=16, hi=23)
    D.dress_wall(h, "e", [D.it_banner("black", y=GROUND + 3, hang=1), D.it_wall_torch(soul=True)], lo=16, hi=23)
    D.dress_wall(h, "e", [D.it_wall_torch(soul=True), D.it_skull_sconce()], lo=3, hi=10, step=(3, 4))
    D.dress_wall(h, "w", [D.it_wall_torch(soul=True)], lo=11, hi=15, step=(4, 4))
    D.dress_wall(h, "e", [D.it_wall_torch(soul=True)], lo=11, hi=15, step=(4, 4))
    D.moss_patch(h, 4, 22, 3, 0.5)
    D.moss_patch(h, 21, 20, 2, 0.5)
    D.clutter(h, [D.cl_bones, D.cl_rubble, D.cl_candles], 10)
    D.wall_vines(h, "s", range(3, 12), p=0.35)
    D.beams(h, "x", [5, 9, 17, 21], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.upper_dress(h, [D.it_tall_banner("purple", hang=3), D.it_high_bracket(soul=True, drop=3), D.it_high_niche("white", drop=3), D.it_tall_banner("black", hang=3)],
                  sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("undead", 11, 13)
    D.auto_light(h, soul=True)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 4. cell_block_b: the Menagerie of cages
# ===================================================================================================================
def cage_cluster(h, x0, z0, gates, roof_row=GROUND + 4, soul=True, damage=0.0):
    """Four barred cages in a 9 x 9 footprint (polished posts at x0, x0+4, x0+8 and z0, z0+4, z0+8, a 3x3 cage between each): iron bars on every face
    (not on a face that stands against a wall: the wall is the face) and a bar roof, a cobbled floor, a bed of hay, a bucket, bones and a lantern hung from
    the roof bars in every cage. `gates` lists (x, z, state) of the gate cells in the bar lines (state 'gap': an opening, 'open': an iron door swung open,
    'locked': a closed iron door); `damage` is the share of bars torn out. Why: the see-through variant of the cell block: the party can watch what is in
    the cages (and be watched) instead of the roofed brick cells of cell_block_a."""
    post_cells = {(x0 + a, z0 + b) for a in (0, 4, 8) for b in (0, 4, 8)}
    gate_cells = {(gx, gz) for (gx, gz, _) in gates}
    against = {"x": set(), "z": set()}
    if x0 == h.x0:
        against["x"].add(x0)
    if x0 + 8 == h.x1:
        against["x"].add(x0 + 8)
    if z0 == h.z0:
        against["z"].add(z0)
    if z0 + 8 == h.z1:
        against["z"].add(z0 + 8)
    for (px, pz) in post_cells:
        for y in range(GROUND, roof_row + 1):
            h.set(px, y, pz, L.POLISHED if y < roof_row else L.CHISELED)
    for x in range(x0, x0 + 9):
        for z in range(z0, z0 + 9):
            if (x, z) in post_cells:
                continue
            on_line = x in (x0, x0 + 4, x0 + 8) or z in (z0, z0 + 4, z0 + 8)
            wall_face = x in against["x"] or z in against["z"]
            if on_line and not wall_face:
                low = GROUND + 2 if (x, z) in gate_cells else GROUND
                for y in range(low, roof_row):
                    if damage and h.rng.random() < damage and y > GROUND:
                        continue
                    h.set(x, y, z, L.IRON_BARS)
            h.set(x, roof_row, z, L.IRON_BARS)
    for (gx, gz, st) in gates:
        if st in ("open", "locked"):
            vertical = gx in (x0, x0 + 4, x0 + 8)       # the gate sits in a line running along z
            props = {"facing": "east" if vertical else "south", "hinge": "left", "open": st == "open", "powered": False}
            h.set(gx, GROUND, gz, "minecraft:iron_door", dict(props, half="lower"))
            h.set(gx, GROUND + 1, gz, "minecraft:iron_door", dict(props, half="upper"))
    for i, (cx, cz) in enumerate([(x0 + 1, z0 + 1), (x0 + 5, z0 + 1), (x0 + 1, z0 + 5), (x0 + 5, z0 + 5)]):
        for dx in range(3):
            for dz in range(3):
                h.floor_set(cx + dx, cz + dz, L.COBBLED if (dx + dz) % 2 else L.CRACKED_TILE)
        h.set(cx, GROUND, cz, "minecraft:hay_block", {"axis": "x"})
        h.set(cx + 1, GROUND, cz, "minecraft:hay_block", {"axis": "x"})
        h.set(cx + 2, GROUND, cz + 2, "minecraft:cauldron")
        D.bones(h, cx + (2 if i % 2 == 0 else 0), cz + 2 - (i % 2) * 2, rot=3 * i + 1, big=(i % 2 == 0))
        h.set(cx + 1, roof_row - 1, cz + 1, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": True})


def guard_room_b(h):
    """The guards' room in the north-east quarter of the menagerie: a desk running along the aisle with a lantern and the ledger lectern at its end, three
    chairs behind it, a key board and a bunk against the north wall, a spear rack with two guard armours on the east wall, a brazier. Why: the brief's guard desk
    for the see-through variant: the desk faces the aisle, so the watchers look at every cage and at whoever walks in."""
    wn, we = h.wall("n"), h.wall("e")
    for z in range(2, 11):
        for x in range(16, 25):
            if (x + z) % 2 == 0:
                h.floor_set(x, z, L.POLISHED)
    D.table(h.frame(17, GROUND, 3, "south"), 7)
    h.set(17, GROUND + 1, 4, L.LANTERN, {"hanging": False})
    h.marker(17, GROUND + 1, 6, "furniture", **{"as": "table"})
    D.candles_at(h, 17, GROUND + 1, 8, 2, "orange")
    D.skull_at(h, 17, GROUND + 1, 7, rot=12)
    D.chair(h.frame(18, GROUND, 4, "west"), seat_marker=True)
    D.chair(h.frame(18, GROUND, 6, "west"))
    D.lectern(h.frame(18, GROUND, 9, "west"))
    D.it_hooks()(wn, 18)
    D.bunk(h.frame(22, GROUND, 3, "south"), color="red")
    D.spear_rack(we, 7, 3)
    h.armor_stand(22, GROUND, 7, yaw=90.0, head="minecraft:iron_helmet", chest="minecraft:chainmail_chestplate", legs="minecraft:chainmail_leggings",
                  mainhand="minecraft:iron_sword")
    h.armor_stand(22, GROUND, 9, yaw=90.0, head="minecraft:chainmail_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
                  feet="minecraft:iron_boots", mainhand="minecraft:stone_axe")
    D.brazier(h, 20, 7)
    D.hang_lantern(h, 20, 4, 4)
    we.set(4, GROUND + 4, 0, "minecraft:red_wall_banner", {"facing": we.into})


def torture_corner_b(h):
    """The south-west quarter: two torture chairs against the west wall facing a brazier, stocks, a hanging cage over the tool bench (anvil, grindstone, quench
    cauldron, barrel), blood and a gutter that runs across the floor to the drain. Why: the brief's torture-chair corner, built differently from the one
    in cell_block_a (two chairs round a fire, a pillory, a cage) so the two variants are two different places."""
    ws = h.wall("s")
    for z in range(16, 25):
        for x in range(2, 11):
            if (x * 3 + z) % 5 == 0:
                h.floor_set(x, z, L.CRACKED_TILE)
    D.torture_chair(h.frame(3, GROUND, 18, "east"))
    D.torture_chair(h.frame(3, GROUND, 22, "east"))
    D.brazier(h, 6, 20)
    D.stocks(h.frame(10, GROUND, 17, "south"), 4)
    h.set(5, GROUND, 24, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(6, GROUND, 24, "minecraft:anvil", {"facing": "north"})
    h.set(7, GROUND, 24, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    h.set(8, GROUND, 24, "minecraft:water_cauldron", {"level": 3})
    D.bracket_lantern(ws, 7, GROUND + 3)
    D.hanging_cage(h, 8, 21, GROUND + 3)
    for z in range(15, 21):
        h.floor_set(5 if z < 18 else 4, z, L.COBBLED)
    for x in range(5, 12):
        h.floor_set(x, 15, L.COBBLED)
    for z in (14, 13):
        h.floor_set(11, z, L.COBBLED)
    D.blood(h, [(4, 17), (5, 18), (4, 19), (4, 21), (5, 22), (4, 23), (5, 17), (6, 17), (5, 19), (6, 16), (7, 17), (8, 16), (9, 15)])
    D.candles_at(h, 2, GROUND, 20, 3, "red")


@room("cell_block_b")
def cell_block_b():
    h = Hall("mega/hall/cell_block_b", 3, 3, 11, wear=0.12)
    for side in "nsew":
        h.door(side, 13)
    h.route(12, 2, 14, 24, label="aisle")
    h.route(2, 12, 24, 14, label="crosswalk")
    # two clusters of cages in opposite corners (north-west, south-east), the guards' room north-east, the torture corner south-west; the 5-wide cross aisle
    # between them. Each cluster opens onto the aisles it faces.
    cage_cluster(h, 2, 2, [(10, 4, "open"), (10, 8, "gap"), (4, 10, "locked"), (8, 10, "gap"), (6, 4, "gap"), (6, 8, "open"), (4, 6, "gap"), (8, 6, "gap")])
    cage_cluster(h, 16, 16, [(16, 18, "locked"), (16, 22, "gap"), (18, 16, "gap"), (22, 16, "open"), (20, 18, "gap"), (20, 22, "open"), (18, 20, "gap"), (22, 20, "gap")],
                 damage=0.10)
    guard_room_b(h)
    torture_corner_b(h)
    D.drain(h, 12, 12)
    D.chandelier_wheel(h, 13, 13, r=2, drop=3)
    for (x, z) in ((13, 6), (13, 20), (6, 13), (20, 13)):
        D.hang_lantern(h, x, z, 6)
    # lanterns on the aisle-side posts of the two cage clusters (a fence arm out of the post, the lantern hanging from it)
    for z in (2, 6, 10):
        h.set(11, GROUND + 3, z, L.FENCE)
        h.set(11, GROUND + 2, z, L.LANTERN, {"hanging": True})
    for x in (2, 6, 10):
        h.set(x, GROUND + 3, 11, L.FENCE)
        h.set(x, GROUND + 2, 11, L.LANTERN, {"hanging": True})
    for z in (16, 20, 24):
        h.set(15, GROUND + 3, z, L.FENCE)
        h.set(15, GROUND + 2, z, L.LANTERN, {"hanging": True})
    for x in (16, 20, 24):
        h.set(x, GROUND + 3, 15, L.FENCE)
        h.set(x, GROUND + 2, 15, L.LANTERN, {"hanging": True})
    for side in "nsew":
        D.dress_wall(h, side, [D.it_skull_sconce(), D.it_banner("purple"), D.it_shackles(), D.it_wall_torch(soul=True)], lo=11, hi=15, step=(3, 4))
    D.cobwebs(h, 0.8)
    D.beams(h, "x", [6, 20], y=h.top)
    D.beams(h, "z", [6, 20], y=h.top)
    D.upper_dress(h, [D.it_tall_banner("black", hang=3), D.it_high_slit(None, rows=3, drop=2), D.it_high_bracket(drop=3), D.it_tall_banner("red", hang=3)], sides="nsew",
                  step=(4, 6))
    finish_common(h, plinth=False, webs=False)
    h.faction("cultist", 13, 13)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 5. torture_chamber_b: the Workshop of Pain
# ===================================================================================================================
@room("torture_chamber_b")
def torture_chamber_b():
    h = Hall("mega/hall/torture_chamber_b", 2, 3, 9, wear=0.14)
    h.door("n", 4)
    h.door("s", 13)
    h.door("w", 13)
    h.route(3, 2, 5, 16, label="north aisle")
    h.route(3, 17, 14, 19, label="south band")
    ws, wn, ww, we = h.wall("s"), h.wall("n"), h.wall("w"), h.wall("e")
    # the dissection table in the middle of the east half, with the chains and the lamp above it
    D.table(h.frame(10, GROUND, 6, "south"), 5, width=2)
    D.skull_at(h, 9, GROUND + 1, 7, rot=5)
    D.skull_at(h, 9, GROUND + 1, 9, rot=11)
    h.set(10, GROUND + 1, 8, "minecraft:red_carpet")
    h.set(9, GROUND + 1, 8, "minecraft:red_carpet")
    h.marker(10, GROUND + 1, 6, "furniture", **{"as": "table"})
    for z in (4, 10):
        D.shackles(h, 11, z, y_top=h.top - 1, drop=3)
        D.shackles(h, 8, z, y_top=h.top - 1, drop=3)
    D.hang_lantern(h, 9, 8, 4)
    # two torture chairs west of the table, turned towards it, so the sitters watch what is done on the slab
    D.torture_chair(h.frame(7, GROUND, 7, "east"))
    D.torture_chair(h.frame(7, GROUND, 10, "east"))
    # the north wall: an iron maiden, the branding cauldron with its irons, a cage hung over the cauldron for the next one to be marked
    D.iron_maiden(wn, 7)
    D.branding_cauldron(h, 12, 2)
    D.hanging_cage(h, 12, 3, GROUND + 2)
    # wheel and stocks on the east wall
    D.breaking_wheel(we, 7, GROUND + 3)
    D.wall_torch(we, 7, GROUND + 7)               # a torch over the wheel: its ring of open trapdoors is a ladder to the shared light check, and the wheel should be lit anyway
    D.stocks(h.frame(14, GROUND, 11, "south"), 4)
    # the brazier with its tongs (an anvil) and a second cage over the crossing east of the west door
    D.brazier(h, 12, 14)
    h.set(10, GROUND, 14, "minecraft:anvil", {"facing": "north"})
    D.hanging_cage(h, 7, 13, GROUND + 3, skull=False)
    # chain beds in the south-west, tools and furnaces in the south-east
    D.hanging_bed(h, 5, 21, "south")
    D.hanging_bed(h, 9, 21, "south", color="black")
    h.set(11, GROUND, 24, "minecraft:anvil", {"facing": "north"})
    h.set(10, GROUND, 24, "minecraft:grindstone", {"face": "floor", "facing": "north"})
    h.furnace(9, GROUND, 24, "blast_furnace", "north")
    h.furnace(8, GROUND, 24, "blast_furnace", "north")
    D.hang_lantern(h, 11, 22, 5)
    for z in (5, 10, 15):
        D.hang_lantern(h, 4, z, 4)
    D.hang_lantern(h, 7, 18, 4)
    D.hang_lantern(h, 11, 18, 4, soul=True)
    D.dress_wall(h, "n", [D.it_skull_sconce(), D.it_banner("black"), D.it_wall_torch(), D.it_candle_niche()], lo=3, hi=15)
    D.dress_wall(h, "w", [D.it_skull_sconce(), D.it_shackles(), D.it_wall_torch(), D.it_hooks(), D.it_banner("red"), D.it_candle_niche()], lo=3, hi=11)
    D.dress_wall(h, "w", [D.it_skull_sconce(), D.it_wall_torch(), D.it_shackles(), D.it_banner("black")], lo=15, hi=24)
    D.dress_wall(h, "s", [D.it_skull_sconce(), D.it_hooks(), D.it_wall_torch(), D.it_banner("red")], lo=3, hi=11)
    D.dress_wall(h, "e", [D.it_skull_sconce(), D.it_shackles(), D.it_wall_torch(), D.it_banner("black")], lo=12, hi=24)
    D.blood(h, [(9, 12), (10, 12), (9, 13), (11, 13), (8, 11), (12, 12), (11, 11), (13, 13), (8, 7), (8, 8), (8, 9), (8, 10), (6, 8), (6, 9), (4, 8), (5, 9),
                (13, 6), (13, 7), (12, 8)])
    D.clutter(h, [D.cl_bones, D.cl_barrels, D.cl_rubble, D.cl_candles], 9)
    D.wall_vines(h, "s", range(3, 11), p=0.4)
    D.beams(h, "x", [4, 10, 16, 22])
    D.upper_dress(h, [D.it_tall_banner("black", hang=2), D.it_high_skull(soul=False, drop=3), D.it_tall_banner("red", hang=2)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("demon", 11, 16)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 6. ossuary
# ===================================================================================================================
@room("ossuary")
def ossuary():
    h = Hall("mega/hall/ossuary", 2, 2, 8, wear=0.08)
    h.door("n", 4)
    h.door("s", 13)
    h.door("e", 13)
    h.route(3, 2, 5, 10, label="north aisle")
    h.route(3, 8, 14, 10, label="band")
    D.floor_border(h, 1)
    for side in "nsew":
        D.bone_wall(h, side)
    # the great heap in the north-east, two pillars of bone
    D.bone_pyramid(h, 9, 2, 13, 6, layers=3)
    D.column(h, 7, 5, shaft="minecraft:bone_block")
    D.column(h, 7, 13, shaft="minecraft:bone_block")
    # the bone-setter's bench along the south wall, stacks of bones under the west wall, barrels
    D.table(h.frame(3, GROUND, 15, "east"), 4)
    h.marker(4, GROUND + 1, 15, "furniture", **{"as": "table"})
    D.skull_at(h, 5, GROUND + 1, 15, rot=3)
    D.skull_at(h, 6, GROUND + 1, 15, rot=9)
    h.set(3, GROUND + 1, 15, L.SOUL_LANTERN, {"hanging": False})
    D.chair(h.frame(4, GROUND, 14, "south"), seat_marker=True)
    D.chair(h.frame(5, GROUND, 14, "south"))
    for z in (11, 12, 13, 14):
        D.bone_stack(h, 2, z, 3 if z != 13 else 4)
    for x in (9, 10):
        h.set(x, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(9, GROUND + 1, 15, "minecraft:barrel", {"facing": "up", "open": False})
    h.marker(10, GROUND + 1, 15, "furniture", **{"as": "storage"})
    # the soul-lantern canopy, a few candles, markers
    D.lantern_canopy(h, range(3, 16, 3), range(3, 16, 3), (3, 5, 4, 6))
    D.loculi(h, "n", range(7, 16, 2), (GROUND + 3, GROUND + 5))
    D.loculi(h, "w", range(3, 11, 2), (GROUND + 3, GROUND + 5))
    D.loculi(h, "e", range(3, 11, 2), (GROUND + 3, GROUND + 5))
    D.loculi(h, "s", range(3, 11, 2), (GROUND + 3, GROUND + 5))
    D.loculi(h, "w", range(11, 16, 2), (GROUND + 3, GROUND + 5))
    D.dress_wall(h, "e", [D.it_wall_torch(soul=True), D.it_skull_sconce()], lo=3, hi=11, step=(3, 4))
    D.dress_wall(h, "n", [D.it_wall_torch(soul=True), D.it_skull_sconce()], lo=7, hi=15, step=(3, 4))
    D.clutter(h, [D.cl_bones, D.cl_candles], 12)
    D.beams(h, "x", [6, 12], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    finish_common(h, plinth=False)
    h.faction("undead", 10, 11)
    D.auto_light(h, soul=True)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 7. library_a: the Long Library (a ring gallery over a reading hall)
# ===================================================================================================================
def post(h, x, z, y0, y1, shaft=L.POLISHED, cap=L.CHISELED):
    """A plain one-cell post with a chiselled capital (a column that takes no floor space for its base)."""
    for y in range(y0, y1 + 1):
        h.set(x, y, z, cap if y == y1 else shaft)


@room("library_a")
def library_a():
    h = Hall("mega/hall/library_a", 3, 2, 16, wear=0.10)
    h.door("n", 13)
    h.door("s", 13)
    h.route(12, 2, 14, 15, label="aisle")
    DECK = GROUND + 6
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # the chequered floor of the reading hall, pilasters between the shelf bays, then the gallery decks over both long sides, with the colonnade that carries them
    D.floor_chequer(h, 2, 2, 24, 15)
    for lat in (6, 10, 16, 20):
        D.pilaster(wn, lat, 1)
        D.pilaster(ws, lat, 1)
    D.deck(h, 2, 2, 24, 5, DECK)
    D.deck(h, 2, 12, 24, 15, DECK)
    for x in (6, 10, 16, 20):
        post(h, x, 5, GROUND, DECK - 1)
        post(h, x, 12, GROUND, DECK - 1)
    for x in range(2, 25):
        h.try_set(x, DECK - 1, 5, L.STRIPPED_LOG, {"axis": "x"})
        h.try_set(x, DECK - 1, 12, L.STRIPPED_LOG, {"axis": "x"})
    # the two flights along the end walls
    D.stair_flight(h, 2, 12, "north", 7, 3, rail_r=2)
    D.stair_flight(h, 24, 5, "south", 7, 3, rail_r=2)
    D.fence_run(h, 5, 5, 24, 5, DECK + 1)
    D.fence_run(h, 2, 12, 21, 12, DECK + 1)
    # glowing amber windows in the end walls, above the flights
    for z in (7, 9, 11):
        D.window_slit(ww, z, GROUND + 8, GROUND + 12, glow="minecraft:shroomlight", glass="minecraft:orange_stained_glass")
        D.window_slit(we, z + 1 if z != 7 else 6, GROUND + 8, GROUND + 12, glow="minecraft:shroomlight", glass="minecraft:orange_stained_glass")
    # the fallen shelves (the gap is webbed, the heap lies in front), then shelves in both storeys, ladders, lamps on the pilasters
    D.toppled_shelf(wn, 21, GROUND, 3, 3)
    D.toppled_shelf(ws, 3, GROUND, 3, 3)
    for wl in (wn, ws):
        D.shelf_wall(wl, 3, 23, GROUND, GROUND + 5)
        D.shelf_wall(wl, 3, 23, DECK + 1, DECK + 7)
        for lat in (8, 18):
            D.ladder_run(wl, lat, GROUND, GROUND + 5)
            D.ladder_run(wl, lat, DECK + 1, DECK + 6)
        for lat in (6, 10, 16, 20):
            D.bracket_lantern(wl, lat, GROUND + 4, arm=1, d0=1)
            D.bracket_lantern(wl, lat, DECK + 5, arm=1, d0=1)
    # reading tables in the hall, lecterns, a globe of lanterns, wheel chandeliers
    for x0 in (6, 16):
        D.table(h.frame(x0, GROUND, 8, "east"), 5)
        h.marker(x0 + 2, GROUND + 1, 8, "furniture", **{"as": "table"})
        for x in (x0 + 1, x0 + 3):
            D.chair(h.frame(x, GROUND, 7, "south"), seat_marker=(x == x0 + 1))
            D.chair(h.frame(x, GROUND, 9, "north"))
        h.set(x0, GROUND + 1, 8, L.LANTERN, {"hanging": False})
        D.candles_at(h, x0 + 4, GROUND + 1, 8, 3, "white")
    D.chandelier_wheel(h, 8, 8, r=2, drop=8)
    D.chandelier_wheel(h, 18, 8, r=2, drop=8)
    # four study lecterns in the bays between the pilasters (the reader stands in front of the shelves), and the globe lamps
    D.lectern(h.frame(7, GROUND, 4, "north"))
    D.lectern(h.frame(19, GROUND, 4, "north"))
    D.lectern(h.frame(9, GROUND, 13, "south"))
    D.lectern(h.frame(17, GROUND, 13, "south"))
    D.lantern_globe(h, 13, 8, GROUND + 6)
    D.lantern_globe(h, 13, 4, DECK + 5)
    D.lantern_globe(h, 13, 13, DECK + 5)
    # upper-deck desks
    for x0 in (3, 12, 21):
        D.table(h.frame(x0, DECK + 1, 3, "east"), 3)
        D.chair(h.frame(x0 + 1, DECK + 1, 4, "north"))
        D.table(h.frame(x0, DECK + 1, 14, "east"), 3)
        D.chair(h.frame(x0 + 1, DECK + 1, 13, "south"))
    finish_common(h, plinth=False, webs=False)
    h.faction("cultist", 13, 8)
    D.auto_light(h)
    D.auto_markers(h)
    D.cobwebs(h, 0.8)
    return h


# ===================================================================================================================
# 8. library_b: the Scriptorium (a mezzanine and a spiral tower of shelves)
# ===================================================================================================================
@room("library_b")
def library_b():
    h = Hall("mega/hall/library_b", 3, 2, 16, wear=0.10)
    h.door("n", 13)
    h.door("s", 13)
    h.route(12, 2, 14, 15, label="aisle")
    DECK = GROUND + 6
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_medallion(h, 13, 10, 5)
    for lat in (6, 10, 16, 20):
        D.pilaster(wn, lat, 1)
    for lat in (5, 9, 17, 21):
        D.pilaster(ws, lat, 1)
    # the mezzanine over the north half of the hall, carried by a colonnade and an architrave
    D.deck(h, 2, 2, 24, 6, DECK)
    for x in (5, 9, 17, 21):
        post(h, x, 7, GROUND, DECK - 1)
    for x in range(2, 25):
        h.try_set(x, DECK - 1, 7, L.STRIPPED_LOG, {"axis": "x"})
    # the spiral tower in the east: a core of shelves, steps round it, a landing at the mezzanine
    for z in range(8, 11):
        for x in range(20, 23):
            for y in range(GROUND, h.top + 1):
                h.set(x, y, z, "minecraft:bookshelf" if y < h.top else L.CHISELED)
    nxt = D.spiral_stairs(h, 21, 9, (22, 11), 7)
    h.set(nxt[0], DECK, nxt[1], L.PLANKS)
    D.fence_run(h, 2, 6, 18, 6, DECK + 1)
    D.fence_run(h, 20, 6, 24, 6, DECK + 1)
    # amber windows in the west wall, shelves everywhere, ladders, lamps
    for z in (4, 8, 12):
        D.window_slit(ww, z, GROUND + 8, GROUND + 12, glow="minecraft:shroomlight", glass="minecraft:orange_stained_glass")
    D.toppled_shelf(wn, 3, GROUND, 3, 3)
    D.toppled_shelf(ws, 21, GROUND, 3, 3)
    D.shelf_wall(wn, 3, 23, GROUND, GROUND + 5)
    D.shelf_wall(wn, 3, 23, DECK + 1, DECK + 7)
    D.shelf_wall(ws, 3, 23, GROUND, GROUND + 13)
    for lat in (8, 18):
        D.ladder_run(wn, lat, GROUND, GROUND + 5)
        D.ladder_run(wn, lat, DECK + 1, DECK + 6)
    for lat in (4, 7, 11, 15, 19, 23):
        D.ladder_run(ws, lat, GROUND, GROUND + 6)
    for lat in (6, 10, 16, 20):
        D.bracket_lantern(wn, lat, GROUND + 4, arm=1, d0=1)
        D.bracket_lantern(wn, lat, DECK + 5, arm=1, d0=1)
    for lat in (5, 9, 17, 21):
        D.bracket_lantern(ws, lat, GROUND + 5, arm=1, d0=1)
        D.bracket_lantern(ws, lat, GROUND + 11, arm=1, d0=1)
    # reading tables, scriptorium desks, the lamps of the tower
    for x0, ln in ((5, 5), (15, 4)):
        D.table(h.frame(x0, GROUND, 9, "east"), ln)
        h.marker(x0 + 1, GROUND + 1, 9, "furniture", **{"as": "table"})
        for x in (x0 + 1, x0 + ln - 2):
            D.chair(h.frame(x, GROUND, 8, "south"), seat_marker=(x == x0 + 1))
            D.chair(h.frame(x, GROUND, 10, "north"))
        h.set(x0, GROUND + 1, 9, L.LANTERN, {"hanging": False})
        D.candles_at(h, x0 + ln - 1, GROUND + 1, 9, 2, "white")
    for x in (5, 7, 9):
        D.lectern(h.frame(x, GROUND, 13, "south"))
        D.chair(h.frame(x, GROUND, 12, "south"))
    D.chandelier_wheel(h, 9, 9, r=3, drop=6)
    D.lantern_globe(h, 13, 9, GROUND + 6, glass="minecraft:yellow_stained_glass")
    D.lantern_globe(h, 13, 4, DECK + 5, glass="minecraft:yellow_stained_glass")
    D.hang_lantern(h, 17, 11, 9)
    for (x, z) in ((19, 12), (23, 12), (24, 12), (24, 11)):
        D.hang_lantern(h, x, z, 10)
    # mezzanine desks
    for x0 in (3, 12, 21):
        D.table(h.frame(x0, DECK + 1, 3, "east"), 3)
        D.chair(h.frame(x0 + 1, DECK + 1, 4, "north"))               # the scribe sits at the desk, facing it
    finish_common(h, plinth=False, webs=False)
    h.faction("cultist", 13, 10)
    D.auto_light(h)
    D.auto_markers(h)
    D.cobwebs(h, 0.8)
    return h


# ===================================================================================================================
# 9. crypt_b: the Lord's Tomb
# ===================================================================================================================
@room("crypt_b")
def crypt_b():
    h = Hall("mega/hall/crypt_b", 3, 3, 11, wear=0.18)
    for side in "nsew":
        h.door(side, 13)
    # the doors lead to a 3-wide ring corridor round the central dais
    h.route(12, 2, 14, 8, label="north")
    h.route(12, 18, 14, 24, label="south")
    h.route(2, 12, 8, 14, label="west")
    h.route(18, 12, 24, 14, label="east")
    h.route(6, 6, 20, 8, label="ring north")
    h.route(6, 18, 20, 20, label="ring south")
    h.route(6, 6, 8, 20, label="ring west")
    h.route(18, 6, 20, 20, label="ring east")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # the floor: a polished ring road round the dais
    for z in range(6, 21):
        for x in range(6, 21):
            if not (9 <= x <= 17 and 9 <= z <= 17) and (x in (6, 20) or z in (6, 20)):
                h.floor_set(x, z, L.POLISHED)
    # pilasters between the wall bays
    for lat in (8, 17):
        D.pilaster(wn, lat, 2)
        D.pilaster(ws, lat, 2)
        D.pilaster(ww, lat, 2)
        D.pilaster(we, lat, 2)
    # the dais with the tomb and a baldachin of four posts joined by beams
    top = D.dais(h, 9, 9, 17, 17, rows=1, inlay=L.TILE)
    for (x, z) in ((10, 10), (16, 10), (10, 16), (16, 16)):
        for y in range(top, h.top):
            h.set(x, y, z, L.POLISHED)
        h.set(x, h.top, z, L.CHISELED)
    for k in range(11, 16):
        for (x, z) in ((10, k), (16, k), (k, 10), (k, 16)):
            h.try_set(x, h.top, z, L.POLISHED)
    D.sarcophagus(h.frame(14, top, 11, "south"), 5, 3)
    for x in (12, 14):
        D.candelabra(h, x, 10, y=top)
        D.candelabra(h, x, 16, y=top)
    # the priest's lectern at the head of the tomb between the two candelabras: the reader stands on the dais step and faces the dead
    D.lectern(h.frame(13, top, 10, "south"))
    D.statue(h, 11, 13, "east", kit="netherite", y=top)
    D.statue(h, 15, 13, "west", kit="gold", y=top)
    # four corner chapels with a sarcophagus each
    D.sarcophagus(h.frame(4, GROUND, 3, "south"), 3, 2)
    D.sarcophagus(h.frame(23, GROUND, 3, "south"), 3, 2, open_lid=True)
    D.sarcophagus(h.frame(4, GROUND, 23, "north"), 3, 2, open_lid=True)
    D.sarcophagus(h.frame(23, GROUND, 23, "north"), 3, 2)
    D.chandelier_wheel(h, 13, 13, r=2, drop=5, soul=True)
    for (x, z, f) in ((10, 3, "south"), (16, 3, "south"), (10, 23, "north"), (16, 23, "north")):
        D.statue(h, x, z, f, kit="skull" if (x + z) % 2 else "iron")
    for (x, z) in ((3, 8), (3, 18), (23, 8), (23, 18), (8, 3), (18, 3), (8, 23), (18, 23), (2, 2), (24, 2), (2, 24), (24, 24)):
        if h.can_set(x, GROUND, z) and h.can_set(x, GROUND + 1, z):
            D.candelabra(h, x, z, soul=True)
    for (wl, lo, hi) in ((wn, 3, 23), (ws, 3, 23), (ww, 3, 23), (we, 3, 23)):
        D.loculi(h, wl.side, [lt for lt in range(lo, hi + 1, 2) if not (10 <= lt <= 16)], (GROUND + 4, GROUND + 6))
        D.dress_wall(h, wl.side, [D.it_banner("purple", y=GROUND + 3, hang=1), D.it_skull_sconce(), D.it_candle_niche("white", y=GROUND + 8), D.it_wall_torch(soul=True)],
                     lo=3, hi=23, step=(3, 5))
    # a soul torch on each side of every doorway, a little way from the jamb: the dim nave still keeps the cells beside the doors above the gloom limit
    for wl in (wn, ws, ww, we):
        for lat in (10, 16):
            D.it_wall_torch(soul=True)(wl, lat)
    # the sunken ossuary pit in the east wing, a ramp of bone blocks down from the ring road side
    ossuary_pit(h, 22, 7, 23, 10, floor_row=4, ramp_from="w")
    D.moss_patch(h, 3, 12, 2, 0.5)
    D.moss_patch(h, 23, 14, 2, 0.5)
    D.clutter(h, [D.cl_bones, D.cl_rubble, D.cl_candles], 8, region=(2, 2, 24, 24))
    D.wall_vines(h, "n", range(3, 12), p=0.3)
    D.beams(h, "x", [7, 19], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.beams(h, "z", [7, 19], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.upper_dress(h, [D.it_tall_banner("purple", hang=3), D.it_high_bracket(soul=True, drop=3), D.it_high_niche("white", drop=3), D.it_tall_banner("black", hang=3)],
                  sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("undead", 13, 20)
    D.auto_light(h, soul=True)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 10. alchemy_lab_a: the Brewer's Workshop
# ===================================================================================================================
def bench_items(h, cells, y, glass="lime", kinds=("brew", "cauldron", "pot", "candle", "case", "lantern", "brew2")):
    """Fills the cells (x, z) of a bench top (row y is the first free row above it) with small laboratory things, each cell with one thing: brewing stands,
    a cauldron, flower pots, candles, a skull jar, a lantern; deterministic."""
    for (x, z) in cells:
        k = h.rng.choice(kinds)
        if not h.can_set(x, y, z, "minecraft:cauldron"):
            continue
        if k == "brew":
            h.set(x, y, z, "minecraft:brewing_stand", {"has_bottle_0": True, "has_bottle_1": False, "has_bottle_2": True})
        elif k == "brew2":
            h.set(x, y, z, "minecraft:brewing_stand", {"has_bottle_0": False, "has_bottle_1": True, "has_bottle_2": True})
        elif k == "cauldron":
            if h.rng.random() < 0.34:
                h.set(x, y, z, "minecraft:cauldron")
            else:
                h.set(x, y, z, "minecraft:water_cauldron", {"level": h.rng.choice([1, 2, 3])})
        elif k == "pot":
            h.set(x, y, z, h.rng.choice(["minecraft:flower_pot", "minecraft:potted_dead_bush", "minecraft:potted_brown_mushroom", "minecraft:potted_red_mushroom",
                                          "minecraft:potted_fern", "minecraft:potted_crimson_fungus", "minecraft:potted_warped_fungus"]))
        elif k == "candle":
            D.candles_at(h, x, y, z, h.rng.randint(1, 3), h.rng.choice(["white", "lime", "cyan", "purple"]))
        elif k == "case":
            if h.can_set(x, y + 3, z, "minecraft:glass"):
                D.glass_case(h, x, z, y, glass)
        elif k == "lantern":
            h.set(x, y, z, L.LANTERN, {"hanging": False})


@room("alchemy_lab_a")
def alchemy_lab_a():
    h = Hall("mega/hall/alchemy_lab_a", 2, 3, 9, wear=0.10)
    h.door("n", 13)
    h.door("s", 4)
    h.door("e", 13)
    h.route(12, 2, 14, 16, label="north aisle")
    h.route(11, 12, 15, 14, label="east crossing")
    h.route(3, 17, 15, 19, label="south band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    # three long benches under the west wall, a stool at each
    h.marker(3, GROUND + 1, 3, "furniture", **{"as": "table"})
    for z0 in (3, 8, 13):
        D.table(h.frame(3, GROUND, z0, "south"), 4, width=2)
        bench_items(h, [(x, z) for z in range(z0, z0 + 4) for x in (2, 3)], GROUND + 1)
        D.chair(h.frame(4, GROUND, z0 + 1, "west"), seat_marker=(z0 == 8))      # a stool right at the bench
    # the vats on the island and the chalk circle round them
    h.set(7, GROUND, 10, "minecraft:water_cauldron", {"level": 3})
    h.set(8, GROUND, 10, "minecraft:lava_cauldron")
    h.set(9, GROUND, 10, "minecraft:powder_snow_cauldron", {"level": 3})
    D.chalk_circle(h, 8, 10, 3)
    # the glass cage with its caged slime under the north wall
    D.pane_cage(h, 8, 4, "lime")
    # the still in the south-east, storage, the alchemist's cot with drying herbs over it
    D.alembic(h, 14, 22)
    for x in (11, 12, 13):
        h.set(x, GROUND, 24, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(12, GROUND + 1, 24, "minecraft:barrel", {"facing": "up", "open": False})
    h.marker(11, GROUND + 1, 24, "furniture", **{"as": "storage"})
    D.bed(h.frame(8, GROUND, 21, "south"), color="green")
    D.bed(h.frame(10, GROUND, 21, "south"), color="lime", marker=False)
    for x in (6, 8, 10, 12):
        D.herb_bundle(h, x, 23, y_top=h.top - 1, drop=2)
    for x in (3, 5, 7):
        D.herb_bundle(h, x, 5, y_top=h.top - 1, drop=2)
    D.hang_lantern(h, 6, 8, 3)
    D.hang_lantern(h, 6, 13, 3)
    D.hang_lantern(h, 9, 18, 3)
    D.hang_lantern(h, 13, 9, 3)
    D.dress_wall(h, "w", [D.it_shelf(("candle", "pot", "brew", "skull")), D.it_lantern_bracket()], lo=3, hi=16, step=(1, 2))
    D.dress_wall(h, "n", [D.it_shelf(), D.it_banner("lime"), D.it_wall_torch(), D.it_hooks()], lo=3, hi=11)
    D.dress_wall(h, "s", [D.it_shelf(), D.it_banner("cyan"), D.it_wall_torch()], lo=7, hi=15)
    D.dress_wall(h, "e", [D.it_shelf(("pot", "brew", "candle")), D.it_banner("lime"), D.it_wall_torch(), D.it_shelf(("skull", "pot"))], lo=3, hi=24)
    D.dress_wall(h, "w", [D.it_wall_torch(), D.it_banner("lime"), D.it_shelf()], lo=17, hi=24)
    D.clutter(h, [D.cl_barrels, D.cl_candles], 6)
    # spilt brews on the floor south of the island: carpets in the colours of the potions (walkable, so the routes stay clear)
    for (x, z, col) in ((9, 15, "lime"), (10, 15, "lime"), (10, 16, "lime"), (9, 16, "green"), (5, 18, "purple"), (6, 18, "purple"), (6, 19, "magenta"),
                        (11, 19, "cyan"), (12, 19, "cyan"), (12, 18, "light_blue")):
        h.try_set(x, GROUND, z, "minecraft:%s_carpet" % col)
    D.beams(h, "x", [5, 11, 17, 23])
    D.upper_dress(h, [D.it_high_slit("minecraft:verdant_froglight", "minecraft:lime_stained_glass", rows=2, drop=2), D.it_tall_banner("lime", hang=2),
                      D.it_high_bracket(drop=2)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("slime", 8, 15)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 11. alchemy_lab_b: the Distillery
# ===================================================================================================================
@room("alchemy_lab_b")
def alchemy_lab_b():
    h = Hall("mega/hall/alchemy_lab_b", 2, 3, 10, wear=0.08)
    h.door("n", 4)
    h.door("s", 13)
    h.door("w", 13)
    h.route(3, 2, 5, 16, label="north aisle")
    h.route(3, 17, 14, 19, label="south band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # a chequered laboratory floor in palette blocks
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            if (x + z) % 2 == 0:
                h.floor_set(x, z, L.POLISHED)
    # three glowing reactor tanks along the east wall, a copper main over them, across to the still in the middle and along the north wall
    tanks = ((13, 3, "lime", "minecraft:verdant_froglight"), (13, 7, "cyan", "minecraft:sea_lantern"), (13, 11, "purple", "minecraft:pearlescent_froglight"))
    for (x, z, liquid, glow) in tanks:
        D.glow_tank(h, x, z, liquid, glow, height=7)
    top = h.top
    pipe = "minecraft:waxed_oxidized_copper"
    for z in range(2, 14):
        h.set(14, top, z, pipe)
    D.alembic(h, 8, 8, tube=4)
    h.set(8, top - 1, 8, pipe)
    for x in range(8, 14):
        h.set(x, top, 8, pipe)
    for y in (top - 1, top - 2):
        h.set(14, y, 2, pipe)
    for x in range(7, 14):
        h.set(x, top - 2, 2, pipe)
    # smaller stills, a specimen bench with skull jars under the south wall, a brewing rack under the north wall
    D.alembic(h, 8, 13, tube=2)
    D.alembic(h, 10, 22, tube=3, glass="cyan")
    D.table(h.frame(4, GROUND, 24, "east"), 5)
    h.marker(5, GROUND + 1, 24, "furniture", **{"as": "table"})
    for x in (6, 8):
        D.glass_case(h, x, 24, GROUND + 1, "lime")
    D.glass_case(h, 4, 24, GROUND + 1, "purple")
    for x in (5, 7):
        D.chair(h.frame(x, GROUND, 23, "south"))
    D.hang_lantern(h, 8, 12, 4)
    D.hang_lantern(h, 4, 9, 4)
    D.pane_cage(h, 6, 21, "cyan", head="minecraft:zombie_head")
    # the cauldron row under the north wall (water, lava, powder snow, an empty one), a sample bench with a stool and herbs drying from the beams
    for x, (blk, pr) in zip(range(7, 12), [("minecraft:water_cauldron", {"level": 3}), ("minecraft:lava_cauldron", None),
                                           ("minecraft:powder_snow_cauldron", {"level": 3}), ("minecraft:cauldron", None),
                                           ("minecraft:water_cauldron", {"level": 1})]):
        h.set(x, GROUND, 2, blk, pr)
    D.table(h.frame(7, GROUND, 11, "east"), 4)
    h.marker(8, GROUND + 1, 11, "furniture", **{"as": "table"})
    bench_items(h, [(8, 11), (9, 11), (10, 11)], GROUND + 1, kinds=("brew", "pot", "candle", "brew2", "lantern"))
    D.chair(h.frame(8, GROUND, 12, "north"), seat_marker=True)
    D.chair(h.frame(10, GROUND, 12, "north"))
    for x in (7, 9, 11):
        D.herb_bundle(h, x, 5, y_top=h.top - 1, drop=2)
    for x in (4, 6, 12):
        D.herb_bundle(h, x, 21, y_top=h.top - 1, drop=2)
    D.dress_wall(h, "n", [D.it_shelf(("brew", "brew", "pot")), D.it_banner("cyan"), D.it_wall_torch()], lo=6, hi=11)
    D.dress_wall(h, "w", [D.it_shelf(("pot", "brew", "candle")), D.it_banner("purple"), D.it_wall_torch(), D.it_hooks()], lo=3, hi=11)
    D.dress_wall(h, "w", [D.it_shelf(("skull", "candle", "brew")), D.it_wall_torch(), D.it_banner("lime")], lo=15, hi=24)
    D.dress_wall(h, "s", [D.it_shelf(("pot", "brew")), D.it_banner("cyan"), D.it_wall_torch()], lo=3, hi=11)
    D.dress_wall(h, "e", [D.it_wall_torch(), D.it_banner("purple")], lo=15, hi=24)
    D.clutter(h, [D.cl_barrels, D.cl_candles], 6)
    # a spilt sample on the floor of the fighting half
    for (x, z, col) in ((9, 17, "cyan"), (10, 17, "cyan"), (10, 18, "light_blue"), (11, 18, "lime"), (9, 19, "purple"), (10, 19, "purple")):
        h.try_set(x, GROUND, z, "minecraft:%s_carpet" % col)
    D.beams(h, "x", [5, 16, 21])
    D.upper_dress(h, [D.it_high_slit("minecraft:sea_lantern", "minecraft:cyan_stained_glass", rows=3, drop=2), D.it_tall_banner("cyan", hang=2),
                      D.it_high_bracket(drop=3)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("cultist", 9, 15)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 12. armory_forge_a: the Great Forge
# ===================================================================================================================
def forge_hearth(h, x1, x2, z1=2):
    """A hearth against the north wall: a raised platform (stepped edge), a back row of lit blast furnaces and lava cauldrons, and a stepped chimney breast
    of brick above it narrowing to a flue. Why: the brief's forge with blast furnaces, lava cauldrons and a chain hoist; the breast is what makes it read as a
    forge and not a row of furnaces."""
    for x in range(x1, x2 + 1):
        for z in (z1, z1 + 1):
            h.set(x, GROUND, z, L.POLISHED)
        h.set(x, GROUND, z1 + 2, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for z in (z1, z1 + 1):
        h.set(x1 - 1, GROUND, z, L.POLISHED_STAIRS, {"facing": "east", "half": "bottom"})
        h.set(x2 + 1, GROUND, z, L.POLISHED_STAIRS, {"facing": "west", "half": "bottom"})
    h.set(x1 - 1, GROUND, z1 + 2, L.POLISHED_SLAB, {"type": "bottom"})
    h.set(x2 + 1, GROUND, z1 + 2, L.POLISHED_SLAB, {"type": "bottom"})
    row = GROUND + 1
    pattern = ["lava", "bf", "bf", "lava", "bf", "bf", "bf", "lava", "bf", "bf", "lava"]
    for i, x in enumerate(range(x1, x2 + 1)):
        kind = pattern[i % len(pattern)]
        if kind == "lava":
            h.set(x, row, z1, "minecraft:lava_cauldron")
        else:
            h.furnace(x, row, z1, "blast_furnace", "south")
    # the chimney breast: a ziggurat of brick narrowing to the flue
    top = h.top
    r0 = GROUND + 3
    for k, y in enumerate(range(r0, top + 1)):
        a, b = x1 - 1 + k, x2 + 1 - k
        if a > b - 2:
            a, b = (x1 + x2) // 2 - 1, (x1 + x2) // 2 + 1
        depth = 3 if k == 0 else 2
        for z in range(z1, z1 + depth):
            for x in range(a, b + 1):
                h.set(x, y, z, D.brick_mix(h, 0.15, 0.1))
    return row


@room("armory_forge_a")
def armory_forge_a():
    h = Hall("mega/hall/armory_forge_a", 3, 2, 10, wear=0.12)
    h.door("s", 4)
    h.door("s", 22)
    h.door("e", 13)
    h.route(3, 12, 24, 14, label="south band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    forge_hearth(h, 9, 17)
    h.marker(10, GROUND + 2, 2, "hearth", **{"as": "stove"})
    # scorched floor in front of the hearth
    for z in range(5, 9):
        for x in range(7, 20):
            h.floor_set(x, z, L.COBBLED if (x + z) % 3 else L.CRACKED_TILE)
    # three anvils with their benches, smithing tables, a quench trough, a chain hoist
    for i, x in enumerate((8, 13, 18)):
        h.set(x, GROUND, 7, "minecraft:anvil", {"facing": "north" if i != 1 else "east"})
        D.table(h.frame(x - 1, GROUND, 9, "east"), 3)
        h.set(x - 1, GROUND + 1, 9, "minecraft:grindstone", {"face": "floor", "facing": "south"})
        D.candles_at(h, x, GROUND + 1, 9, 2, "orange")
        h.set(x + 1, GROUND + 1, 9, L.LANTERN, {"hanging": False})
    h.set(11, GROUND, 11, "minecraft:smithing_table")
    h.set(15, GROUND, 11, "minecraft:smithing_table")
    for x in (21, 22, 23):
        h.set(x, GROUND, 10, "minecraft:water_cauldron", {"level": 3})
    D.shackles(h, 13, 7, y_top=h.top, drop=5)
    D.shackles(h, 8, 7, y_top=h.top, drop=5)
    D.shackles(h, 18, 7, y_top=h.top, drop=5)
    # the arms: spear racks and armour stands on the west wall, armoured statues on the east wall
    D.spear_rack(ww, 3, 3)
    D.spear_rack(ww, 8, 3)
    h.armor_stand(4, GROUND, 6, yaw=270.0, head="minecraft:iron_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
                  feet="minecraft:iron_boots", mainhand="minecraft:iron_sword")
    h.armor_stand(4, GROUND, 11, yaw=270.0, head="minecraft:chainmail_helmet", chest="minecraft:chainmail_chestplate", legs="minecraft:chainmail_leggings",
                  feet="minecraft:chainmail_boots", mainhand="minecraft:iron_axe")
    for z, kit in ((3, "gold"), (6, "iron"), (9, "chain")):
        D.statue(h, 23, z, "west", kit=kit)
    # coal and crates in the north-east corner, weapon boards on the south wall
    for (x, z, n) in ((20, 2, 3), (21, 2, 2), (20, 3, 1)):
        for k in range(n):
            h.set(x, GROUND + k, z, "minecraft:coal_block")
    h.set(22, GROUND, 3, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(22, GROUND, 2, "minecraft:barrel", {"facing": "up", "open": False})
    D.dress_wall(h, "s", [D.it_hooks(), D.it_banner("orange"), D.it_wall_torch(), D.it_shelf(("candle", "skull", "pot"))], lo=7, hi=19, step=(2, 3))
    D.dress_wall(h, "n", [D.it_banner("orange", y=GROUND + 5, hang=2), D.it_wall_torch()], lo=3, hi=6, step=(2, 3))
    D.dress_wall(h, "n", [D.it_banner("black", y=GROUND + 5, hang=2), D.it_wall_torch()], lo=21, hi=24, step=(2, 3))
    D.dress_wall(h, "e", [D.it_wall_torch(), D.it_banner("orange")], lo=3, hi=11, step=(3, 4))
    D.dress_wall(h, "w", [D.it_wall_torch(), D.it_banner("black")], lo=12, hi=15, step=(3, 4))
    D.clutter(h, [D.cl_barrels, D.cl_candles], 5)
    D.beams(h, "z", [6, 13, 20])
    D.upper_dress(h, [D.it_tall_banner("orange", hang=3), D.it_high_slit(None, rows=3, drop=2), D.it_high_bracket(drop=3), D.it_tall_banner("black", hang=3)],
                  sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("construct", 9, 11)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 13. armory_forge_b: the Armoury
# ===================================================================================================================
@room("armory_forge_b")
def armory_forge_b():
    h = Hall("mega/hall/armory_forge_b", 3, 2, 9, wear=0.10)
    h.door("n", 22)
    h.door("s", 4)
    h.door("w", 13)
    h.route(21, 2, 23, 12, label="north aisle")
    h.route(3, 10, 23, 12, label="band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 6, 2, 20, 9)
    # armour alcoves along both long walls, between pilasters
    for lat in (7, 11, 15, 19):
        D.pilaster(wn, lat, 1)
    for lat in (7, 11, 15, 19):
        D.pilaster(ws, lat, 1)
    kits = ["iron", "gold", "chain", "diamond", "netherite", "iron", "chain", "gold"]
    for i, lat in enumerate((5, 9, 13, 17)):
        D.armor_alcove(wn, lat, kits[i])
    for i, lat in enumerate((9, 13, 17, 21)):
        D.armor_alcove(ws, lat, kits[4 + i])
    # the forge corner in the west
    for z in (3, 4):
        h.furnace(2, GROUND, z, "blast_furnace", "east")
    h.set(2, GROUND, 6, "minecraft:lava_cauldron")
    h.set(2, GROUND, 7, "minecraft:lava_cauldron")
    h.set(4, GROUND, 5, "minecraft:anvil", {"facing": "north"})
    h.set(4, GROUND, 8, "minecraft:grindstone", {"face": "floor", "facing": "east"})
    h.set(3, GROUND, 9, "minecraft:smithing_table")
    h.set(5, GROUND, 3, "minecraft:water_cauldron", {"level": 3})
    D.chain_hoist(h, 4, 6, drop=4)
    # polearms in recesses of the east wall (the racks of this armoury)
    D.weapon_niche(we, 4)
    D.weapon_niche(we, 12)
    # a row of armoured statues and two display tables in the north half
    for x, kit in ((9, "netherite"), (13, "diamond"), (17, "gold")):
        D.statue(h, x, 5, "south", kit=kit)
    for x0 in (7, 15):
        D.table(h.frame(x0, GROUND, 8, "east"), 5)
        h.marker(x0 + 1, GROUND + 1, 8, "furniture", **{"as": "table"})
        D.candles_at(h, x0 + 2, GROUND + 1, 8, 3, "orange")
        D.skull_at(h, x0 + 3, GROUND + 1, 8, rot=4)
        h.set(x0 + 4, GROUND + 1, 8, L.LANTERN, {"hanging": False})
    # storage along the east wall: barrels, chests, a weapon rack
    for z in (7, 8, 9):
        h.set(24, GROUND, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(24, GROUND + 1, 8, "minecraft:barrel", {"facing": "up", "open": False})
    h.marker(24, GROUND + 2, 8, "furniture", **{"as": "storage"})
    h.set(23, GROUND, 14, "minecraft:chest", {"facing": "west", "type": "single"})
    h.set(24, GROUND, 14, "minecraft:chest", {"facing": "west", "type": "single"})
    D.dress_wall(h, "n", [D.it_banner("red"), D.it_wall_torch(), D.it_banner("black")], lo=3, hi=20, step=(2, 3))
    D.dress_wall(h, "s", [D.it_banner("red"), D.it_wall_torch(), D.it_banner("black")], lo=6, hi=24, step=(2, 3))
    D.dress_wall(h, "w", [D.it_wall_torch(), D.it_banner("red"), D.it_hooks()], lo=15, hi=15)
    D.dress_wall(h, "e", [D.it_wall_torch(), D.it_banner("black"), D.it_shelf(("skull", "candle", "pot"))], lo=10, hi=15, step=(2, 3))
    D.clutter(h, [D.cl_barrels, D.cl_candles], 4)
    D.beams(h, "z", [6, 13, 20])
    D.upper_dress(h, [D.it_tall_banner("red", hang=2), D.it_high_slit(None, rows=2, drop=2), D.it_tall_banner("black", hang=2), D.it_high_bracket(drop=2)],
                  sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("construct", 13, 8)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 14. kitchen_larder
# ===================================================================================================================
@room("kitchen_larder")
def kitchen_larder():
    h = Hall("mega/hall/kitchen_larder", 2, 2, 8, wear=0.10)
    h.door("n", 13)
    h.door("w", 13)
    h.door("s", 4)
    h.route(12, 2, 14, 12, label="north aisle")
    h.route(3, 10, 14, 12, label="band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 2, 5, 14, 10)
    top = h.top
    # the hearth: a cobbled hearthstone, a campfire under a hanging pot, smokers and a furnace beside it, a brick hood
    for x in range(3, 10):
        for z in (2, 3):
            h.floor_set(x, z, L.COBBLED)
    # the fire in the middle (the `hearth;as=stove` marker stands on it: a kitchen mod swaps the campfire for its stove and the party rests there), a stew pot on
    # each side of it on the hearthstone (`furniture;as=cooking_pot`: the SAFE builder's convention, "a cauldron next to a fire"), smokers and furnaces beyond
    h.set(6, GROUND, 2, "minecraft:campfire", {"facing": "south", "lit": True, "signal_fire": False})
    h.marker(6, GROUND + 1, 2, "hearth", **{"as": "stove"})
    h.set(5, GROUND, 2, "minecraft:water_cauldron", {"level": 3})
    h.set(7, GROUND, 2, "minecraft:cauldron")
    h.marker(5, GROUND + 1, 2, "furniture", **{"as": "cooking_pot"})
    h.marker(7, GROUND + 1, 2, "furniture", **{"as": "cooking_pot"})
    for x, kind in ((4, "smoker"), (8, "smoker"), (3, "furnace"), (9, "furnace")):
        h.furnace(x, GROUND, 2, kind, "south")
    # a grease filter of iron bars in the mouth of the hood, above the fire and the two pots
    for x in (5, 6, 7):
        h.set(x, GROUND + 2, 2, "minecraft:iron_bars")
    for k, y in enumerate(range(GROUND + 3, top + 1)):
        a, b = 3 + k, 9 - k
        if a > b:
            break
        for x in range(a, b + 1):
            for z in (2, 3):
                h.try_set(x, y, z, D.brick_mix(h, 0.12, 0.08))
    # the cutting table and a second bench, with the goods on them
    D.table(h.frame(4, GROUND, 6, "east"), 5)
    h.set(5, GROUND + 1, 6, L.PLATE)                 # the cutting board: a wooden pressure plate on the counter (the SAFE builder's convention for `cutting_board`)
    h.marker(5, GROUND + 2, 6, "furniture", **{"as": "cutting_board"})
    D.candles_at(h, 6, GROUND + 1, 6, 2, "orange")
    h.set(7, GROUND + 1, 6, "minecraft:flower_pot")
    h.set(8, GROUND + 1, 6, L.LANTERN, {"hanging": False})
    for x in (5, 7):
        D.chair(h.frame(x, GROUND, 7, "north"), seat_marker=(x == 5))
    D.table(h.frame(4, GROUND, 9, "east"), 3)
    h.marker(5, GROUND + 1, 9, "furniture", **{"as": "counter"})
    h.set(4, GROUND + 1, 9, "minecraft:water_cauldron", {"level": 3})
    D.candles_at(h, 6, GROUND + 1, 9, 1, "white")
    # the larder: barrels along the east wall, crates and chests in the south, smoked meat on hooks from the beams
    for z in range(3, 10):
        h.set(15, GROUND, z, "minecraft:barrel", {"facing": "up" if z % 2 else "west", "open": False})
        if z % 3 != 0:
            h.set(15, GROUND + 1, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.marker(15, GROUND + 2, 4, "furniture", **{"as": "storage"})
    for x in (8, 9, 10, 11):
        h.set(x, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": False})
    for x in (9, 10):
        h.set(x, GROUND + 1, 15, "minecraft:barrel", {"facing": "up", "open": False})
    # a double chest facing north: the RIGHT half is the west one (it joins eastward), the LEFT half the east one (a viewer in front, looking south, has east on the left)
    h.set(12, GROUND, 15, "minecraft:chest", {"facing": "north", "type": "right"})
    h.set(13, GROUND, 15, "minecraft:chest", {"facing": "north", "type": "left"})
    h.set(15, GROUND, 14, "minecraft:hay_block", {"axis": "x"})
    h.set(15, GROUND, 13, "minecraft:hay_block", {"axis": "x"})
    h.set(15, GROUND + 1, 14, "minecraft:hay_block", {"axis": "x"})
    for x in (4, 6, 8):
        for z in (5, 9):
            if h.can_set(x, top, z, "minecraft:chain"):
                D.herb_bundle(h, x, z, y_top=top - 1, drop=2)
    for (x, z) in ((10, 6), (10, 8), (11, 7)):
        for k in range(2):
            h.set(x, top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
        h.set(x, top - 2, z, "minecraft:bone_block", {"axis": "y"})
    D.hang_lantern(h, 6, 8, 3)
    D.hang_lantern(h, 13, 7, 3)
    D.hang_lantern(h, 8, 13, 3)
    D.dress_wall(h, "w", [D.it_shelf(("pot", "candle", "brew")), D.it_wall_torch(), D.it_hooks()], lo=2, hi=11, step=(2, 3))
    D.dress_wall(h, "s", [D.it_shelf(("pot", "candle")), D.it_wall_torch(), D.it_banner("orange")], lo=6, hi=15, step=(2, 3))
    D.dress_wall(h, "e", [D.it_shelf(("pot", "candle", "skull")), D.it_wall_torch()], lo=10, hi=13, step=(2, 3))
    D.dress_wall(h, "n", [D.it_shelf(("pot", "candle")), D.it_hooks()], lo=10, hi=11, step=(2, 3))
    D.clutter(h, [D.cl_barrels, D.cl_candles], 4)
    D.beams(h, "x", [5, 9, 13])
    finish_common(h)
    h.faction("goblin", 8, 8)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 15. barracks_a: the Soldiers' Quarters
# ===================================================================================================================
@room("barracks_a")
def barracks_a():
    h = Hall("mega/hall/barracks_a", 3, 2, 8, wear=0.10)
    h.door("n", 22)
    h.door("s", 4)
    h.door("e", 13)
    h.route(21, 2, 23, 11, label="north aisle")
    h.route(3, 9, 23, 11, label="band")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    top = h.top
    # bunk beds along both long walls
    for i, x in enumerate((4, 8, 12, 16)):
        D.bunk(h.frame(x, GROUND, 4, "north"), color="red" if i % 2 == 0 else "blue", chains=True)
    for i, x in enumerate((8, 12, 16)):
        D.bunk(h.frame(x, GROUND, 13, "south"), color="blue" if i % 2 == 0 else "red", chains=True)
    # the long table between the bunks, a bench on each side, the map table
    D.table(h.frame(7, GROUND, 7, "east"), 11)
    h.marker(8, GROUND + 1, 7, "furniture", **{"as": "table"})
    for x in (9, 12, 15):
        D.candles_at(h, x, GROUND + 1, 7, 2, "orange")
    for x in (10, 13):
        h.set(x, GROUND + 1, 7, L.LANTERN, {"hanging": False})
    D.bench(h.frame(8, GROUND, 6, "east"), 9)
    D.bench(h.frame(8, GROUND, 8, "east"), 9)
    D.chair(h.frame(6, GROUND, 7, "east"), seat_marker=True)
    D.chair(h.frame(18, GROUND, 7, "west"))
    h.set(20, GROUND, 7, "minecraft:cartography_table")
    D.chair(h.frame(20, GROUND, 8, "north"))                        # the officer at the map table sits in front of it, facing it
    # the hearth in the west, lockers of barrels in the north-east and south-east, two guard armours
    D.fireplace(ww, 6, 3)
    h.marker(2, GROUND + 1, 7, "hearth", **{"as": "stove"})
    for x in (18, 19, 20):
        for z in (2, 3):
            h.set(x, GROUND, z, "minecraft:barrel", {"facing": "south", "open": False})
    h.marker(19, GROUND + 1, 2, "furniture", **{"as": "storage"})
    for x in (18, 19):
        for z in (14, 15):
            h.set(x, GROUND, z, "minecraft:barrel", {"facing": "north", "open": False})
    h.armor_stand(19, GROUND, 4, yaw=0.0, head="minecraft:iron_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
                  feet="minecraft:iron_boots", mainhand="minecraft:iron_sword")
    h.armor_stand(20, GROUND, 4, yaw=0.0, head="minecraft:chainmail_helmet", chest="minecraft:chainmail_chestplate", legs="minecraft:chainmail_leggings",
                  mainhand="minecraft:stone_axe")
    # the weapon racks: polearms standing in recesses of the end walls
    D.weapon_niche(we, 4)
    D.weapon_niche(we, 8)
    D.weapon_niche(ww, 13)
    for x in (6, 10, 14):
        D.hang_lantern(h, x, 3, 4)
        D.hang_lantern(h, x + 2 if x == 6 else x, 14, 4)
    D.dress_wall(h, "n", [D.it_banner("red", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=3, hi=17, step=(3, 4))
    D.dress_wall(h, "s", [D.it_banner("blue", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5), D.it_shelf(("candle", "pot", "skull"))], lo=7, hi=24, step=(3, 4))
    D.dress_wall(h, "e", [D.it_banner("red"), D.it_wall_torch(), D.it_shelf(("candle", "skull"))], lo=2, hi=11, step=(3, 4))
    D.clutter(h, [D.cl_barrels, D.cl_candles], 4)
    D.beams(h, "z", [6, 14, 22])
    finish_common(h)
    h.faction("goblin", 13, 7)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 16. barracks_b: the Officers' Cabins
# ===================================================================================================================
def cabin(h, x0, color, door_state="open", z0=2, width=4, depth=4):
    """A timber cabin built into the north end of a hall: planks partitions and front wall with a doorway (a wooden door), a plank roof, and inside a bed,
    a chest, a small desk with a chair and a lantern. Interior x0..x0+width-1, z0..z0+depth-1; the front wall stands at z0+depth. Why: the officers of the
    barracks sleep apart from the soldiers: the cabins give the hall the private rooms the dormitory lacks."""
    roof = GROUND + 5
    front = z0 + depth
    for x in (x0 - 1, x0 + width):
        for z in range(z0, front + 1):
            for y in range(GROUND, roof + 1):
                h.try_set(x, y, z, L.STRIPPED_LOG if z == front else L.PLANKS, {"axis": "y"} if z == front else None)
    for x in range(x0, x0 + width):
        for y in range(GROUND, roof):
            if x == x0 + 1 and y < GROUND + 2:
                continue
            h.set(x, y, front, L.PLANKS)
        h.set(x, roof, front, L.STRIPPED_LOG, {"axis": "x"})
        for z in range(z0, front):
            h.set(x, roof, z, L.PLANKS_SLAB if (x + z) % 2 else L.PLANKS)
    props = {"facing": "south", "hinge": "left", "open": door_state == "open", "powered": False}
    h.set(x0 + 1, GROUND, front, L.WOOD_DOOR, dict(props, half="lower"))
    h.set(x0 + 1, GROUND + 1, front, L.WOOD_DOOR, dict(props, half="upper"))
    # inside: bed against the back wall, desk and chair, chest, lantern
    D.bed(h.frame(x0, GROUND, z0 + 2, "north"), color=color)
    h.set(x0 + width - 1, GROUND, z0, "minecraft:chest", {"facing": "west", "type": "single"})
    D.table(h.frame(x0 + width - 1, GROUND, z0 + 1, "south"), 2)
    D.chair(h.frame(x0 + width - 2, GROUND, z0 + 2, "east"))
    D.candles_at(h, x0 + width - 1, GROUND + 1, z0 + 2, 2, "orange")
    h.set(x0 + 1, GROUND + 4, z0 + 2, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x0 + 1, GROUND + 3, z0 + 2, L.LANTERN, {"hanging": True})


@room("barracks_b")
def barracks_b():
    h = Hall("mega/hall/barracks_b", 3, 2, 9, wear=0.10)
    h.door("w", 13)
    h.door("e", 13)
    h.door("s", 13)
    h.route(2, 12, 24, 14, label="band")
    h.route(12, 11, 14, 15, label="south aisle")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 2, 8, 24, 11)
    for i, x0 in enumerate((3, 8, 13, 18)):
        cabin(h, x0, ("red", "blue", "red", "blue")[i], "open" if i != 2 else "locked")
        h.set(x0 + 2, GROUND + 3, 7, "minecraft:%s_wall_banner" % ("red" if i % 2 == 0 else "blue"), {"facing": "south"})
    # the common room in front of the cabins: a long table with a bench each side, a fireplace on the south wall, two officers' armours
    D.table(h.frame(7, GROUND, 9, "east"), 13)
    h.marker(8, GROUND + 1, 9, "furniture", **{"as": "table"})
    for x in (9, 12, 15, 18):
        D.candles_at(h, x, GROUND + 1, 9, 3, "orange")
    for x in (10, 14, 17):
        h.set(x, GROUND + 1, 9, L.LANTERN, {"hanging": False})
    D.bench(h.frame(8, GROUND, 8, "east"), 11)
    D.bench(h.frame(8, GROUND, 10, "east"), 11)
    D.chair(h.frame(6, GROUND, 9, "east"), seat_marker=True)
    D.chair(h.frame(20, GROUND, 9, "west"))
    D.fireplace(ws, 5, 3)
    h.marker(6, GROUND + 1, 15, "hearth", **{"as": "stove"})
    for z, kit in ((4, "gold"), (8, "iron")):
        D.statue(h, 23, z, "west", kit=kit)
    # the map table at the east end (a cartography table with a candle and a chair), lockers at the west end, polearms in recesses of the south and east walls
    h.set(21, GROUND, 10, "minecraft:cartography_table")
    h.marker(21, GROUND + 1, 10, "furniture", **{"as": "table"})
    D.chair(h.frame(20, GROUND, 10, "east"))
    for z in (8, 9, 10):
        h.set(3, GROUND, z, "minecraft:barrel", {"facing": "east", "open": False})
    D.weapon_niche(ws, 19)
    D.weapon_niche(ws, 23)
    D.weapon_niche(we, 10)
    D.dress_wall(h, "s", [D.it_banner("red"), D.it_wall_torch(), D.it_shelf(("candle", "pot", "skull"))], lo=9, hi=24, step=(3, 4))
    D.dress_wall(h, "e", [D.it_wall_torch(), D.it_banner("blue"), D.it_shelf(("candle", "pot"))], lo=2, hi=11, step=(3, 4))
    D.dress_wall(h, "w", [D.it_wall_torch(), D.it_banner("blue")], lo=7, hi=11, step=(3, 4))
    D.clutter(h, [D.cl_barrels, D.cl_candles], 4)
    D.beams(h, "z", [7, 12, 17, 22])
    D.upper_dress(h, [D.it_tall_banner("red", hang=2), D.it_high_bracket(drop=2), D.it_tall_banner("blue", hang=2)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("ogre", 13, 8)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 17. shrine_a: the Chapel
# ===================================================================================================================
def apse(h, x1, z1, x2, z2, y=GROUND):
    """A raised stage for an altar or a throne: a polished platform one row high over [x1..x2] x [z1..z2] against the north wall, with stairs climbing on the south
    side and the two short sides, resting on row `y` (a second tier goes on top of the first). Returns the standing row on top."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            if z == z2 and x1 < x < x2:
                h.set(x, y, z, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
            elif x == x1 and z < z2:
                h.set(x, y, z, L.POLISHED_STAIRS, {"facing": "east", "half": "bottom"})
            elif x == x2 and z < z2:
                h.set(x, y, z, L.POLISHED_STAIRS, {"facing": "west", "half": "bottom"})
            elif z == z2:
                h.set(x, y, z, L.POLISHED_SLAB, {"type": "bottom"})
            else:
                h.set(x, y, z, L.POLISHED if (x + z) % 2 else L.CHISELED)
    return y + 1


def pew(f, length, marker=False):
    """A pew: a row of stair seats along the frame's right (the sitters face the frame's forward, the stair backs behind them)."""
    for k in range(length):
        f.set(k, 0, 0, L.PLANKS_STAIRS, {"facing": "@back", "half": "bottom"})
    if marker:
        f.marker(0, 1, 0, "seat", facing=f.facing, **{"as": "chair"})


STAINED = (("minecraft:cyan_stained_glass", "minecraft:sea_lantern"), ("minecraft:magenta_stained_glass", "minecraft:pearlescent_froglight"),
           ("minecraft:orange_stained_glass", "minecraft:ochre_froglight"), ("minecraft:lime_stained_glass", "minecraft:verdant_froglight"))


@room("shrine_a")
def shrine_a():
    h = Hall("mega/hall/shrine_a", 2, 3, 12, wear=0.08)
    h.door("s", 4)
    h.door("s", 13)
    h.door("e", 13)
    h.door("w", 13)
    h.route(6, 7, 11, 24, label="nave")
    h.route(2, 12, 15, 14, label="cross aisle")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 6, 6, 11, 24)
    top = h.top
    # the stage with the altar, statues and candelabras
    stage = apse(h, 4, 2, 13, 5)
    for x in range(7, 11):
        h.set(x, stage, 3, L.POLISHED_STAIRS if x in (7, 10) else L.POLISHED, {"facing": "west" if x == 7 else "east", "half": "bottom"} if x in (7, 10) else None)
        h.set(x, stage + 1, 3, L.CHISELED)
    for x in (8, 9):
        h.set(x, stage + 2, 3, "minecraft:purple_carpet")
    D.candles_at(h, 7, stage + 2, 3, 3, "white")
    D.candles_at(h, 10, stage + 2, 3, 3, "white")
    D.statue(h, 5, 3, "south", kit="gold", y=stage)
    D.statue(h, 12, 3, "south", kit="gold", y=stage)
    D.candelabra(h, 6, 5, y=stage, n=4)
    D.candelabra(h, 11, 5, y=stage, n=4)
    D.candelabra(h, 6, 3, y=stage, n=3)
    D.candelabra(h, 11, 3, y=stage, n=3)
    # pews on both sides of the nave, candles between the rows, holy-water fonts at the entrance
    for z in (8, 10, 16, 18):
        pew(h.frame(2, GROUND, z, "north"), 4, marker=(z == 10))
        pew(h.frame(12, GROUND, z, "north"), 4)
    for z in (9, 17):
        for x in (3, 4, 13, 14):
            D.candles_at(h, x, GROUND, z, 3, "white")
    h.set(2, GROUND, 21, "minecraft:water_cauldron", {"level": 3})
    h.set(15, GROUND, 21, "minecraft:water_cauldron", {"level": 3})
    # the prayer carpet: a four-wide runner from the stage steps down the nave, white borders round a purple field, a kneeling cushion of red at its foot
    runner(h, 7, 6, 10, 22, "z", colors=("white", "purple", "purple", "white"))
    for x in (8, 9):
        h.try_set(x, GROUND, 23, "minecraft:red_carpet")
        h.try_set(x, GROUND, 24, "minecraft:red_carpet")
    # tall stained-glass slits in the apse wall and in both side walls
    for i, lat in enumerate((4, 7, 10, 13)):
        glass, glow = STAINED[i % len(STAINED)]
        D.window_slit(wn, lat, GROUND + 3, GROUND + 10, glow=glow, glass=glass)
    for i, z in enumerate((7, 10, 17, 20)):
        glass, glow = STAINED[(i + 1) % len(STAINED)]
        D.window_slit(ww, z, GROUND + 3, GROUND + 9, glow=glow, glass=glass)
        D.window_slit(we, z, GROUND + 3, GROUND + 9, glow=glow, glass=glass)
    # candle chandeliers down the nave and ribs of the vault
    for z in (9, 17):
        D.chandelier_wheel(h, 9, z, r=2, drop=6, candles=True)
    D.hang_lantern(h, 9, 22, 6)
    for side in "ns":
        D.dress_wall(h, side, [D.it_banner("white", y=GROUND + 6, hang=2), D.it_wall_torch()], lo=3, hi=15, step=(3, 4))
    D.dress_wall(h, "w", [D.it_banner("white", y=GROUND + 6, hang=2), D.it_wall_torch()], lo=3, hi=6, step=(2, 3))
    D.dress_wall(h, "e", [D.it_banner("white", y=GROUND + 6, hang=2), D.it_wall_torch()], lo=3, hi=6, step=(2, 3))
    D.dress_wall(h, "w", [D.it_banner("white", y=GROUND + 6, hang=2), D.it_wall_torch()], lo=22, hi=24, step=(2, 3))
    D.dress_wall(h, "e", [D.it_banner("white", y=GROUND + 6, hang=2), D.it_wall_torch()], lo=22, hi=24, step=(2, 3))
    D.beams(h, "x", [6, 12, 18, 24], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.upper_dress(h, [D.it_tall_banner("white", hang=3), D.it_high_bracket(drop=3), D.it_tall_banner("purple", hang=3)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("cultist", 9, 12)
    D.auto_light(h)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 18. shrine_b: the Sanctum
# ===================================================================================================================
@room("shrine_b")
def shrine_b():
    h = Hall("mega/hall/shrine_b", 2, 3, 11, wear=0.14)
    h.door("n", 4)
    h.door("s", 13)
    h.door("w", 13)
    h.door("e", 13)
    h.route(3, 2, 5, 12, label="north aisle")
    h.route(2, 12, 15, 14, label="cross aisle")
    h.route(12, 15, 14, 24, label="south aisle")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 2, 2, 15, 11)
    top = h.top
    D.idol(h, 9, 2)
    D.brazier(h, 7, 8, soul=True)
    D.brazier(h, 12, 8, soul=True)
    # the ritual circle in the south-west, a slab in its middle, a candelabra at each quarter
    D.sacrificial_slab(h, 7, 18, "south")
    for (x, z) in ((4, 16), (10, 16), (4, 22), (10, 22)):
        D.candelabra(h, x, z, n=4, color="red")
    D.chalk_circle(h, 7, 19, 3, ring="minecraft:red_carpet", mark="minecraft:black_carpet")
    # offering bowls (cauldrons) and a kneeling row of carpets before the idol
    for x in (7, 11):
        h.set(x, GROUND, 11, "minecraft:cauldron")
    for x in range(7, 12):
        h.try_set(x, GROUND, 7, "minecraft:red_carpet")
    # windows with a red glow, banners
    for lat in (7, 9, 11):
        D.window_slit(wn, lat, GROUND + 5, GROUND + 9, glow="minecraft:shroomlight", glass="minecraft:red_stained_glass")
    for z in (6, 9, 17, 20):
        D.window_slit(ww, z, GROUND + 3, GROUND + 8, glow="minecraft:shroomlight", glass="minecraft:red_stained_glass")
        D.window_slit(we, z, GROUND + 3, GROUND + 8, glow="minecraft:shroomlight", glass="minecraft:red_stained_glass")
    # hanging censers over the ritual area, a wheel of red candles over the crossing
    D.chandelier_wheel(h, 8, 14, r=2, drop=4, candles=True)
    D.hang_lantern(h, 7, 19, 5, soul=True)
    for side in "nsew":
        D.dress_wall(h, side, [D.it_banner("red", y=GROUND + 6, hang=2), D.it_skull_sconce(soul=False), D.it_wall_torch(), D.it_banner("black", y=GROUND + 6, hang=2)],
                     lo=3, hi=24 if side in "we" else 15, step=(3, 4))
    D.clutter(h, [D.cl_candles, lambda hh, x, z: D.cl_candles(hh, x, z, "red"), D.cl_bones], 26, region=(6, 6, 15, 11))
    D.clutter(h, [D.cl_candles, lambda hh, x, z: D.cl_candles(hh, x, z, "red")], 10, region=(2, 15, 11, 24))
    D.beams(h, "x", [6, 12, 18, 24], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.upper_dress(h, [D.it_tall_banner("black", hang=3), D.it_high_skull(soul=False, drop=3), D.it_tall_banner("red", hang=3)], sides="nsew", step=(4, 6))
    finish_common(h)
    h.faction("cultist", 8, 10)
    D.auto_light(h, soul=True)
    D.auto_markers(h)
    return h


# ===================================================================================================================
# 19. great_hall
# ===================================================================================================================
def throne(h, cx, y, z, crown_rows=3):
    """A throne against the north wall at (cx, y, z): a stair seat facing the hall, slab armrests, a tall back of polished stone with a chiselled crown and a
    skull, two banners on the wall either side. `y` is the row of the seat."""
    h.set(cx, y, z + 1, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})
    h.set(cx - 1, y, z + 1, L.POLISHED_SLAB, {"type": "bottom"})
    h.set(cx + 1, y, z + 1, L.POLISHED_SLAB, {"type": "bottom"})
    for dx in (-1, 0, 1):
        for k in range(crown_rows + 2):
            h.set(cx + dx, y + k, z, L.POLISHED if dx != 0 or k < crown_rows + 1 else L.CHISELED)
    h.set(cx, y + crown_rows + 2, z, "minecraft:wither_skeleton_skull", {"rotation": "0"})
    for dx in (-2, 2):
        h.set(cx + dx, y, z, L.POLISHED)
        h.set(cx + dx, y + 1, z, L.POLISHED_SLAB, {"type": "bottom"})


@room("great_hall")
def great_hall():
    h = Hall("mega/hall/great_hall", 3, 5, 16, wear=0.10)
    h.door("s", 13)
    h.door("w", 22)
    h.door("e", 22)
    h.door("w", 40)
    h.route(11, 8, 15, 42, label="aisle")
    h.route(2, 21, 24, 23, label="cross")
    h.route(2, 39, 15, 41, label="south cross")
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    D.floor_chequer(h, 2, 8, 24, 43)
    top = h.top
    # the dais at the north end: two tiers, the throne, braziers; a fireplace each side
    stage = apse(h, 7, 2, 19, 7)
    inner = apse(h, 10, 2, 16, 4, y=stage)
    throne(h, 13, inner, 2)
    D.brazier(h, 8, 5, y=stage)
    D.brazier(h, 18, 5, y=stage)
    D.fireplace(wn, 3, 3)
    D.fireplace(wn, 21, 3)
    D.statue(h, 9, 4, "south", kit="gold", y=stage)
    D.statue(h, 17, 4, "south", kit="gold", y=stage)
    # the colonnade of both side aisles
    for x in (5, 21):
        for z in (10, 18, 26, 34):
            D.column(h, x, z)
    # four long tables with benches, a cross gap where the side doors are; goods on the tables
    for (tx, bl, br, wb) in ((9, 7, 10, "w"), (18, 16, 19, "e")):
        for z0 in (10, 25):
            D.table(h.frame(tx, GROUND, z0, "south"), 10, width=2)
            D.bench(h.frame(bl, GROUND, z0, "south"), 10)
            D.bench(h.frame(br, GROUND, z0, "south"), 10)
            h.marker(tx, GROUND + 1, z0 + 1, "furniture", **{"as": "table"})
            for k in (2, 5, 8):
                D.candles_at(h, tx, GROUND + 1, z0 + k, 3, "orange")
                h.set(tx - 1, GROUND + 1, z0 + k + 1, L.LANTERN, {"hanging": False})
            h.set(tx - 1, GROUND + 1, z0 + 4, "minecraft:cake")
            h.set(tx, GROUND + 1, z0 + 6, "minecraft:flower_pot")
            h.set(tx - 1, GROUND + 1, z0 + 7, "minecraft:flower_pot")
    # the minstrels' gallery over the south end (the high seat of the family that is not on the dais): a deck on four posts and an architrave, a fence rail with the
    # stair opening at the east end, the flight up along the east wall; under it a buttery of barrels and hanging meat
    GAL = GROUND + 5
    for x in (4, 8, 17, 21):
        post(h, x, 37, GROUND, GAL - 1)
    D.stair_flight(h, 24, 31, "south", 6, 2, rail_r=1)
    for x in range(2, 25):
        h.try_set(x, GAL - 1, 37, L.STRIPPED_LOG, {"axis": "x"})
    D.deck(h, 2, 37, 24, 42, GAL)
    D.fence_run(h, 2, 37, 22, 37, GAL + 1)
    D.table(h.frame(4, GAL + 1, 41, "east"), 7)
    h.marker(5, GAL + 2, 41, "furniture", **{"as": "table"})
    D.candles_at(h, 7, GAL + 2, 41, 3, "orange")
    D.candles_at(h, 9, GAL + 2, 41, 2, "orange")
    for x in (5, 7, 9):
        D.chair(h.frame(x, GAL + 1, 40, "south"), seat_marker=(x == 5))
    D.lectern(h.frame(18, GAL + 1, 41, "north"))
    D.lectern(h.frame(20, GAL + 1, 41, "north"))
    for x in (22, 23, 24):
        h.set(x, GAL + 1, 42, "minecraft:barrel", {"facing": "up", "open": False})
    for x in list(range(3, 11)) + list(range(16, 24)):
        h.set(x, GROUND, 42, "minecraft:barrel", {"facing": "up" if x % 3 else "north", "open": False})
        if x % 3 == 0:
            h.set(x, GROUND + 1, 42, "minecraft:barrel", {"facing": "up", "open": False})
    h.marker(17, GROUND + 1, 42, "furniture", **{"as": "storage"})
    for (x, z) in ((18, 38), (20, 40), (22, 39), (6, 38)):
        for k in range(2):
            h.set(x, GAL - 1 - k, z, L.CHAIN_BLOCK, {"axis": "y"})
        h.set(x, GAL - 3, z, "minecraft:bone_block", {"axis": "y"})
    D.lantern_globe(h, 8, 40, GAL + 5)
    D.lantern_globe(h, 19, 40, GAL + 5)
    # hearth markers, the lamps: three wheel chandeliers down the nave (the last one hung high over the front of the gallery) and a lantern string over each table
    for lat in (4, 22):
        h.marker(lat, GROUND + 1, 2, "hearth", **{"as": "stove"})
    for z, dr in ((15, 8), (28, 8), (37, 5)):
        D.chandelier_wheel(h, 13, z, r=3, drop=dr)
    for (x, z) in ((8, 15), (8, 30), (17, 15), (17, 30)):
        D.hang_lantern(h, x, z, 7)
    # armoured statues between the pillars, banners and glowing windows on the long walls
    for (x, z, f, kit) in ((3, 14, "east", "iron"), (3, 30, "east", "chain"), (23, 14, "west", "iron"), (23, 30, "west", "chain")):
        D.statue(h, x, z, f, kit=kit)
    for side in ("w", "e"):
        wl = h.wall(side)
        for z in (14, 30):
            if wl.all_free([(z, GROUND + 9 - k, 0) for k in range(5)]):
                wl.set(z, GROUND + 9, 0, "minecraft:red_wall_banner", {"facing": wl.into})
        for z in (12, 16, 28, 32):
            D.window_slit(wl, z, GROUND + 5, GROUND + 11, glow="minecraft:shroomlight", glass="minecraft:orange_stained_glass")
    for x in (10, 16):
        wn.set(x, 14, 0, "minecraft:red_wall_banner", {"facing": wn.into})
    D.dress_wall(h, "s", [D.it_banner("red", y=GROUND + 12, hang=3), D.it_wall_torch(y=GROUND + 3), D.it_skull_sconce(soul=False)], lo=3, hi=24, step=(3, 4))
    D.dress_wall(h, "s", [D.it_wall_torch(y=GAL + 3), D.it_banner("black", y=GAL + 8, hang=2)], lo=3, hi=24, step=(3, 4))
    D.dress_wall(h, "w", [D.it_wall_torch(y=GROUND + 3), D.it_banner("black", y=GROUND + 8, hang=3)], lo=36, hi=42, step=(3, 4))
    D.dress_wall(h, "e", [D.it_wall_torch(y=GROUND + 3), D.it_banner("black", y=GROUND + 8, hang=3)], lo=26, hi=42, step=(3, 4))
    D.dress_wall(h, "w", [D.it_wall_torch(y=GROUND + 3), D.it_banner("black", y=GROUND + 8, hang=3)], lo=3, hi=8, step=(3, 4))
    D.dress_wall(h, "e", [D.it_wall_torch(y=GROUND + 3), D.it_banner("black", y=GROUND + 8, hang=3)], lo=3, hi=8, step=(3, 4))
    runner(h, 12, 8, 14, 42, "z", colors=("red", "red", "red"))
    D.clutter(h, [D.cl_barrels, D.cl_candles, D.cl_rubble], 8, region=(2, 36, 24, 42))
    D.beams(h, "x", [9, 17, 25, 33, 41], block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    D.upper_dress(h, [D.it_tall_banner("black", hang=4, drop=2), D.it_high_bracket(drop=3), D.it_tall_banner("red", hang=4, drop=2)], sides="nsew", step=(5, 7))
    finish_common(h)
    h.faction("ogre", 13, 20)
    D.auto_light(h)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


# ===================================================================================================================
# 20. treasure closets (1 x 2 modules, dead ends)
# ===================================================================================================================
def spike_pit(h, x1, z1, x2, z2, p=0.65):
    """A one-deep pit across [x1..x2] x [z1..z2] with pointed-dripstone stalagmites on its lined floor: a fall of one block onto spikes. The cavity is lined
    (bottom and a ring of cells) so no void shows."""
    for z in range(z1 - 1, z2 + 2):
        for x in range(x1 - 1, x2 + 2):
            if x1 <= x <= x2 and z1 <= z <= z2:
                h.set(x, FLOOR - 2, z, L.COBBLED)
            else:
                h.set(x, FLOOR - 1, z, L.COBBLED)
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            h.carve(x, FLOOR, z)
            if h.rng.random() < p:
                h.set(x, FLOOR - 1, z, "minecraft:pointed_dripstone", {"vertical_direction": "up", "thickness": "tip"})


def web_curtain(h, x1, x2, z, y1=GROUND, y2=GROUND + 2):
    """A curtain of cobwebs across a passage."""
    for x in range(x1, x2 + 1):
        for y in range(y1, y2 + 1):
            h.set(x, y, z, "minecraft:cobweb")


@room("closet_a")
def closet_a():
    h = Hall("mega/treasure/closet_a", 1, 2, 7, wear=0.10)
    h.door("n", 4)
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    # the entrance hall: shelves under the west wall, crates under the east wall
    for z in range(3, 7):
        for y in (GROUND, GROUND + 1, GROUND + 2):
            h.set(2, y, z, "minecraft:bookshelf")
    for z in (3, 4):
        h.set(6, GROUND, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(6, GROUND + 1, 3, "minecraft:barrel", {"facing": "up", "open": False})
    # the spike pit with a one-wide ledge along the east wall, a curtain of cobwebs behind it
    spike_pit(h, 2, 8, 5, 10)
    web_curtain(h, 2, 6, 12)
    # the treasure: the chest between two soul candelabras, a guard at each corner behind it
    h.loot_chest(4, GROUND, 14, "north")
    D.candelabra(h, 3, 14, soul=True)
    D.candelabra(h, 5, 14, soul=True)
    D.statue(h, 2, 15, "north", kit="skull")
    D.statue(h, 6, 15, "north", kit="chain")
    D.hang_lantern(h, 4, 9, 3, soul=True)
    D.hang_lantern(h, 4, 4, 3)
    for z in (4, 13):
        D.wall_torch(ww, z, GROUND + 3)
        D.wall_torch(we, z, GROUND + 3)
    h.marker(4, GROUND, 5, "spawn", weight=2)
    h.marker(4, GROUND, 6, "room_center")      # the middle of the closet is the spike pit: aim patrols and spawns at the cell before it
    D.cobwebs(h, 1.0)
    D.beams(h, "x", [5, 11])
    finish_common(h, plinth=False, webs=False)
    return h


@room("closet_b")
def closet_b():
    h = Hall("mega/treasure/closet_b", 1, 2, 7, wear=0.12)
    h.door("n", 4)
    h.door("w", 13)
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    # the bait at the far end: an empty chest between candles, ringed by dripstone spikes; the real chest hides behind a bookshelf in the east wall
    h.set(4, GROUND, 15, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": False})
    for (x, z) in ((3, 15), (5, 15)):
        h.set(x, GROUND, z, "minecraft:pointed_dripstone", {"vertical_direction": "up", "thickness": "tip"})
    D.candelabra(h, 2, 15, n=4, color="orange")
    D.candelabra(h, 6, 15, n=4, color="orange")
    we.carve(8, GROUND, -1)
    h.loot_chest(*we.cell(8, GROUND, -1), "west")
    we.set(8, GROUND, 0, "minecraft:bookshelf")
    we.set(8, GROUND + 1, 0, "minecraft:bookshelf")
    we.set(7, GROUND, 0, "minecraft:bookshelf")
    we.set(9, GROUND, 0, "minecraft:bookshelf")
    D.candles_at(h, *we.cell(8, GROUND + 2, 0), 2, "red")
    D.skull_at(h, *we.cell(7, GROUND + 1, 0), rot=4)
    web_curtain(h, 3, 5, 10, GROUND, GROUND + 3)
    for z in (3, 5):
        D.statue(h, 2, z, "east", kit="iron")
    D.hang_lantern(h, 4, 8, 3)
    D.hang_lantern(h, 4, 13, 3, soul=True)
    D.wall_torch(ww, 8, GROUND + 3)
    D.wall_torch(we, 4, GROUND + 3)
    h.marker(4, GROUND, 5, "spawn", weight=2)
    D.cobwebs(h, 1.0)
    D.beams(h, "x", [4, 9, 14])
    finish_common(h, plinth=False, webs=False)
    return h


@room("closet_c")
def closet_c():
    h = Hall("mega/treasure/closet_c", 1, 2, 7, wear=0.18)
    h.door("n", 4)
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    top = h.top
    # a collapse across the middle: a heap of broken masonry with a way over it
    for z in (7, 8, 9):
        for x in range(2, 7):
            r = h.rng.random()
            if z == 8 and x in (3, 4, 5):
                h.set(x, GROUND, z, L.COBBLED_SLAB, {"type": "bottom"})
            elif r < 0.30:
                h.set(x, GROUND, z, L.COBBLED)
                if h.rng.random() < 0.5 and x in (2, 6):
                    h.set(x, GROUND + 1, z, L.COBBLED_SLAB, {"type": "bottom"})
            elif r < 0.65:
                h.set(x, GROUND, z, L.COBBLED_STAIRS, {"facing": h.rng.choice(["north", "south", "east", "west"]), "half": "bottom"})
            else:
                h.set(x, GROUND, z, L.COBBLED_SLAB, {"type": "bottom"})
    for x in (3, 4, 5):          # a fallen beam lying across the way, on the floor
        h.set(x, GROUND, 10, L.LOG, {"axis": "x"})
    # the cache under a ceiling of stalactites, with the bones of the last one who came
    h.loot_chest(4, GROUND, 14, "north")
    for (x, z, th) in ((3, 13, "tip"), (5, 13, "tip"), (4, 12, "frustum"), (4, 13, "tip"), (3, 14, "tip"), (5, 14, "tip")):
        if th == "frustum":
            h.set(x, top, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": "frustum"})
            h.set(x, top - 1, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": "tip"})
        else:
            h.set(x, top, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": "tip"})
    D.bones(h, 3, 15, rot=5, big=True)
    D.bones(h, 5, 15, rot=11)
    h.armor_stand(2, GROUND, 14, yaw=270.0, head="minecraft:iron_helmet", chest="minecraft:leather_chestplate", mainhand="minecraft:iron_sword")
    D.candles_at(h, 6, GROUND, 14, 3, "white")
    D.candles_at(h, 2, GROUND, 11, 2, "orange")
    D.hang_lantern(h, 4, 5, 3)
    D.hang_lantern(h, 4, 11, 3, soul=True)
    for z in (4, 13):
        D.wall_torch(ww, z, GROUND + 3)
    D.wall_torch(we, 5, GROUND + 3)
    h.marker(4, GROUND, 5, "spawn", weight=2)
    h.marker(4, GROUND, 6, "room_center")      # the middle of the closet is the collapse: aim patrols and spawns at the cell before it
    D.cobwebs(h, 1.0)
    D.wall_vines(h, "w", range(3, 12), p=0.4)
    D.beams(h, "x", [4, 10])
    finish_common(h, plinth=False, webs=False)
    return h


# ===================================================================================================================
# driver
# ===================================================================================================================
def build(name):
    h = ROOMS[name]()
    h.finish()
    return h


def sha(path):
    with open(path, "rb") as f:
        return hashlib.sha256(f.read()).hexdigest()[:12]


def print_table():
    """The template table of docs/labyrinth/mega_rooms1.md (section 2), printed from the files on disk so the doc cannot drift from them: size, doors, markers,
    armour stands, the minimum block light of the kit's model and of the lead's `mega_shape_preview` light report, the number of non-air cells."""
    import collections
    from template import load
    import mega_shape_preview as SP
    print("| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Stands | Light kit / shared (min) | Cells |")
    print("|---|---|---|---|---|---|---|---|---|---|")
    brief_order = ["cell_block", "torture_chamber", "crypt", "ossuary", "library", "alchemy_lab", "armory_forge", "kitchen_larder", "barracks", "shrine", "great_hall",
                   "closet"]
    for name in sorted(ROOMS, key=lambda n: (brief_order.index(n.rsplit("_", 1)[0] if n[-2:] in ("_a", "_b", "_c") else n), n)):
        t = load(os.path.join(L.RES, "data", "nusmp", "structure", "mega", name_path(name) + ".nbt"), "mega/" + name_path(name))
        sx, sy, sz = t.size
        doors = []
        for (x, y, z), o, _d in t.jigsaws():
            side = o.split("_")[0][0]
            doors.append("%s@%d" % (side, x if side in "ns" else z))
        marks = D.markers_of(t)
        count = collections.Counter(k for k, *_ in marks)
        heavy = sum(1 for k, _x, _y, _z, a in marks if k == "spawn" and a.get("weight"))
        other = []
        for k in ("faction", "furniture", "seat", "hearth", "bed", "room_center"):
            if count.get(k):
                if k == "faction":
                    other.append("faction " + [a.get("faction") for kk, _x, _y, _z, a in marks if kk == "faction"][0])
                else:
                    other.append(k if count[k] == 1 else "%s x%d" % (k, count[k]))
        stands = sum(1 for e in t.entities if str(e["nbt"].get("id", "")) == "minecraft:armor_stand")
        lv = D.light_levels(t)
        nodes = D.walk_nodes(t)
        holes = D.door_holes(t)
        vals = sorted(lv.get((x, int(s_ / 2.0 + 1.62), z), 0) for (x, z, s_) in D.reach(t, nodes) if (x, s_ // 2, z) not in holes)
        rep = SP.light_report(t, name)
        print("| `%s` | %dx%d | %dx%d | %s | %d (%d x2) | %d | %s | %d | %d / %d (median %d) | %d |" % (
            name_path(name), sx // 9, sz // 9, sx, sz, " ".join(doors), count["spawn"], heavy, count["patrol"], ", ".join(other) or "-", stands, vals[0], rep["min"],
            vals[len(vals) // 2], len(t.cells)))


def name_path(name):
    """The template path ('hall/crypt_a', 'treasure/closet_a') of a room name of this file."""
    return ("treasure/" if name.startswith("closet_") else "hall/") + name


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("names", nargs="*", help="templates to build (default: all)")
    ap.add_argument("--check", action="store_true", help="do not write; compare with the files on disk and run the template checks")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--table", action="store_true", help="print the markdown table of section 2 of the doc from the files on disk")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--force", action="store_true", help="write a template even when a check fails (to look at it); the exit status stays 1")
    a = ap.parse_args(argv)
    if a.list:
        for n in ROOMS:
            print(n)
        return 0
    if a.table:
        print_table()
        return 0
    names = a.names or list(ROOMS)
    bad = 0
    for n in names:
        if n not in ROOMS:
            print("unknown template", n)
            return 2
        h = build(n)
        problems, stats = h.check()
        status = "ok  " if not problems else "FAIL"
        if not problems:
            pass
        else:
            bad += 1
        if a.check:
            p = h.mt.t.path(L.RES)
            data_equal = os.path.exists(p) and _same(h, p)
            if not data_equal:
                print("STALE %s (the file on disk differs from the generator)" % n)
                bad += 1
        elif not problems or a.force:
            h.save()
        else:
            print("NOT WRITTEN %s: a template with a defect never reaches the data folder (--force writes it anyway)" % n)
        if not a.quiet or problems:
            print("%s %-28s %5d blocks %2d doors %2d stands  light min %s median %s dark %s%%  markers %s" % (
                status, n, stats.get("blocks", 0), len(h.doors), stats.get("armor_stands", 0), stats.get("eye_light_min"), stats.get("eye_light_median"),
                round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
            for p in problems:
                print("     - " + p)
    return 1 if bad else 0


def _same(h, path):
    import io
    import nbt as _nbt
    mine = _nbt.dumps(h.mt.t.to_nbt(), compress=True)
    with open(path, "rb") as f:
        return f.read() == mine


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

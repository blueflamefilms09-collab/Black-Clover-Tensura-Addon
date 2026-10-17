#!/usr/bin/env python3
"""
THEME-C, theme 1 of 4 (1.59.0, docs/labyrinth/mega_v2_thc.md): the signature rooms of the CURSED ARCHIVE, the evil road's massive library of forbidden
knowledge. Every room is named `cursed_archive_<room>.nbt` so that `data/nusmp/mega/template_themes/cursed_archive.json` can reserve it for sectors of that
theme.

    python3 tools/labyrinth/gen_mega_theme_cursed_archive.py                    # (re)writes every template
    python3 tools/labyrinth/gen_mega_theme_cursed_archive.py cursed_archive_stacks   # only this one
    python3 tools/labyrinth/gen_mega_theme_cursed_archive.py --check            # regenerates in memory, compares with the files on disk, runs every check
    python3 tools/labyrinth/gen_mega_theme_cursed_archive.py --list

Why a generator and not hand-placed blocks: the rooms must obey the connector contract (module grid, doorway holes, jigsaw cells), be reproducible byte for
byte, and be re-themable (architecture is written in the palette constants of `mega_lib`; the Region Theme turns them into red-brown brick, black stone and
crying obsidian). The furnishing kit `mega_decor_r1.py` refuses decor in doorways and routes and checks the finished room; `mega_thc_kit.py` adds the props of a
library. The lights of this theme are SOUL lights (the theme turns every warm light of a template into one), so the rooms are written with soul lights and
checked with them.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_thc_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

REG = K.Registry(__doc__)
room = REG.room
CALM = ("cursed_archive_librarians_study", "cursed_archive_card_catalogue")     # no spawn markers: a safe room and a puzzle room
SOUL = True

PURPLE, MAGENTA = "purple", "magenta"
INK = "minecraft:pearlescent_froglight"        # the cursed ink's glow: a pale violet lamp that no theme swaps and that gives full light 15


def gantry(h, x1, x2, z0, y, half=2, hang=True):
    """A hanging bridge: a deck of dark oak `half` * 2 + 1 wide in z centred on z0 from x1 to x2 at row y (the deck row; you stand on row y + 1), a fence rail
    on both long edges, and (hang) a chain from the ceiling to a post at each end of both edges and in the middle, so it reads as slung from the vault."""
    for z in range(z0 - half, z0 + half + 1):
        for x in range(x1, x2 + 1):
            h.try_set(x, y, z, L.PLANKS if (x + z) % 5 else L.STRIPPED_LOG, None if (x + z) % 5 else {"axis": "x"})
    for z in (z0 - half, z0 + half):
        for x in range(x1, x2 + 1):
            h.try_set(x, y + 1, z, L.FENCE)
    if hang:
        for z in (z0 - half, z0 + half):
            for x in (x1 + 1, (x1 + x2) // 2, x2 - 1):
                if h.free(x, y + 2, z) and h.get(x, y + 1, z) is not None:
                    for yy in range(y + 2, h.top + 1):
                        if not h.try_set(x, yy, z, L.CHAIN_BLOCK, {"axis": "y"}):
                            break


def tower(h, x0, z0, x1, z1, y0, y1, bands=(6, 13, 20), hollow=(), niches=()):
    """A free-standing tower of shelves: a solid core of bookshelves (a chiselled one now and then), a polished post at each corner, a band of chiselled
    bookshelf every few rows, a flared stair base and a crying obsidian crown. Why solid: the tower is the silhouette of the room; the players climb the
    stairs and the bridges around it, never the tower. `hollow` boxes (x1, y1, z1, x2, y2, z2) stay empty: the tunnels a gantry passes through."""
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                if any(a <= x <= d and b <= y <= e and c <= z <= f for (a, b, c, d, e, f) in hollow):
                    continue
                if (x, y, z) in niches:
                    continue
                corner = x in (x0, x1) and z in (z0, z1)
                if corner:
                    blk, props, data = (L.CHISELED, None, None) if y == y1 else (L.POLISHED, None, None)
                elif (y - y0) in bands:
                    blk, (props, data) = "minecraft:chiseled_bookshelf", D.chiseled_shelf(h, "south", 0.85)
                else:
                    blk, props, data = "minecraft:bookshelf", None, None
                h.try_set(x, y, z, blk, props, data) if data is None else h.set(x, y, z, blk, props, nbt=data)
    D.ring_stairs(h, x0, z0, x1, z1, y0, "bottom", L.POLISHED_STAIRS)
    for (x, y, z) in niches:
        D.candles_at(h, x, y, z, 3, "purple")


def hang_tome(h, x, z, drop, width=1):
    """A chained tome: a chiselled bookshelf hanging `drop` rows under the ceiling on a chain (several chains for a wide one). Why: the brief's chained books;
    a chain is solid to the connectivity check and holds a block to the roof, so the tome is joined to the building."""
    y = h.top - drop
    if not all(h.can_set(x + dx, yy, z, "minecraft:chain") for dx in range(width) for yy in range(y, h.top + 1)):
        return False                                  # something else already hangs or stands there: the tome simply is not hung
    for dx in range(width):
        for yy in range(y + 1, h.top + 1):
            h.set(x + dx, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
        props, data = D.chiseled_shelf(h, "south", 0.9)
        h.set(x + dx, y, z, "minecraft:chiseled_bookshelf", props, nbt=data)
    return True


def ink_lamps(h, walls, tiers, zs):
    """Pale violet lamps let into the shelving: one froglight in the FRONT shelf of the wall at every (lateral, tier) of the grid. Why: soul lanterns give only
    light 10, a lantern hung from a thirty-block ceiling gives the floor nothing, and a library of the dead is lit by lamps in its walls, so each tier of the
    wall carries its own row of lamps and every stair, bay and gantry is within a few blocks of one."""
    n = 0
    for w in walls:
        for y in tiers:
            for lat in zs:
                for d in (1, 0):
                    pos = w.cell(lat, y, d)
                    c = h.get(*pos)
                    if c is not None and c.block in ("minecraft:bookshelf", "minecraft:chiseled_bookshelf") and h.mt.t.get(*w.cell(lat, y, d - 1)) is not None:
                        h.set(pos[0], pos[1], pos[2], INK, over=True)
                        n += 1
                        break
    return n


def bays(h, fl, x, z, facing, width, base, back_r):
    """Furnishes the reading bays under a hollow flight (`fl` is the list of (first step, last step) ranges `K.hollow_flight` returned): a lectern with a book
    against the shelves at the back, a stool facing it, a purple carpet down the middle and a soul lantern hung from the soffit."""
    fx, fz = D.DV[facing]
    rx, rz = D.DV[D.RIGHT[facing]]
    wall_dir = "west" if back_r == 0 else "east"
    n = 0
    for (k0, k1) in fl:
        if k1 - k0 < 3 or k0 < 6:
            continue
        km = (k0 + k1) // 2

        def cell(r, dk=0):
            return (x + (km + dk) * fx + r * rx, z + (km + dk) * fz + r * rz)
        bx, bz = cell(back_r)
        mx, mz = cell(1)
        cx, cz = cell(width - 1 - back_r)
        D.lectern(h.frame(bx, GROUND, bz, wall_dir))
        D.chair(h.frame(cx, GROUND, cz, wall_dir))
        for dk in range(k0 - km, k1 - km + 1):
            ax, az = cell(1, dk)
            h.try_set(ax, GROUND, az, "minecraft:purple_carpet")
        K.hung_lantern_under(h, mx, mz, GROUND, GROUND + 3)
        n += 1
    return n


@room("cursed_archive_stacks", "hall")
def stacks():
    """The Stacks: a shelf canyon thirty blocks high. Two grand flights climb the long walls over rows of reading bays, three hanging gantries cross the void
    at three heights (one through a tower of books), the ground is a purple-carpeted lane round that tower and the air is a canopy of soul lanterns."""
    h = Hall("mega/hall/cursed_archive_stacks", 3, 4, 30, wear=0.08)
    h.door("n", 13)
    h.door("s", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # the lanes the party walks (nothing solid may stand in them), the centre approach to both doors and the foot of both flights
    h.route(7, 2, 9, 33, label="west lane")
    h.route(17, 2, 19, 33, label="east lane")
    h.route(12, 2, 14, 10, label="north approach")
    h.route(12, 22, 14, 33, label="south approach")
    h.route(4, 33, 6, 33, label="west stair foot")
    h.route(20, 33, 22, 33, label="east stair foot")
    # the doors first (their jambs and lintels), then the walls of books: two shelves deep on the long walls, one on the ends, from the floor to the cornice
    D.door_trim(h, "n", 13)
    D.door_trim(h, "s", 13)
    for wl in (ww, we):
        for d in (0, 1):
            D.shelf_wall(wl, 2, 33, GROUND, h.top - 1, chiseled=0.07, d=d)
    for wl in (wn, ws):
        D.shelf_wall(wl, 2, 24, GROUND, GROUND + 10, chiseled=0.07)
        D.shelf_wall(wl, 2, 9, GROUND + 11, h.top - 1, chiseled=0.07)
        D.shelf_wall(wl, 17, 24, GROUND + 11, h.top - 1, chiseled=0.07)
        for lat in (11, 13, 15):
            D.window_slit(wl, lat, GROUND + 12, GROUND + 27, glow="minecraft:shroomlight", glass="minecraft:purple_stained_glass")
        for lat in (10, 12, 14, 16):
            for y in range(GROUND + 11, h.top):
                D.shelf_wall(wl, lat, lat, y, y, chiseled=0.15)
    # the grand flights: 26 steps climbing north from the south end to a landing at row 34 (you stand on row 35), a rail on the canyon side
    gaps = [(14, 18), (23, 27)]
    fw = K.hollow_flight(h, 4, 32, "north", 26, 3, GROUND, 2, gaps)
    fe = K.hollow_flight(h, 20, 32, "north", 26, 3, GROUND, 0, gaps)
    bays(h, fw, 4, 32, "north", 3, GROUND, 0)
    bays(h, fe, 20, 32, "north", 3, GROUND, 2)
    # the north landing joins the flights across the whole canyon (the top gantry), the two others cross lower down
    gantry(h, 7, 19, 4, 34, half=1, hang=True)
    for x in range(4, 7):
        for z in range(3, 7):
            h.try_set(x, 34, z, L.PLANKS)
            h.try_set(x + 16, 34, z, L.PLANKS)
    gantry(h, 7, 19, 25, 16, half=2)
    gantry(h, 7, 19, 16, 25, half=2)
    # the tower of books in the middle of the canyon, with a tunnel for the middle gantry and a candle niche on every face
    niches = [(11, y, 16) for y in (14, 21)] + [(15, y, 16) for y in (14, 21)] + [(13, y, 12) for y in (14, 21, 30)] + [(13, y, 20) for y in (14, 21, 30)]
    tower(h, 11, 12, 15, 20, GROUND, h.top - 1, hollow=[(11, 25, 14, 15, 29, 18)], niches=niches)
    for (x, y, z) in niches:
        if y == 14:
            h.set(x, y, z, INK, over=True)
    # the floor: a medallion of crying obsidian at each end, purple runners in the lanes
    D.floor_medallion(h, 13, 6, 4)
    D.floor_medallion(h, 13, 28, 4)
    for z in range(3, 33):
        for x in (8, 18):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    # lamps in the shelving, one row per tier, and in the gantries
    for i, y in enumerate((GROUND + 2, GROUND + 6, GROUND + 11, GROUND + 16, GROUND + 21, GROUND + 26)):
        ink_lamps(h, (ww, we), (y,), range(4 + 2 * (i % 2), 33, 5))
        ink_lamps(h, (wn, ws), (y,), (5, 10, 16, 21) if i % 2 == 0 else (6, 20))
    for (z0, y) in ((4, 34), (25, 16), (16, 25)):
        for x in (9, 13, 17):
            h.set(x, y, z0, INK, over=True)
    # light and air: wheel chandeliers of candles, a canopy of soul lanterns, chained tomes
    D.chandelier_wheel(h, 13, 9, r=2, drop=9, candles=True)
    D.chandelier_wheel(h, 13, 30, r=2, drop=9, candles=True)
    D.lantern_canopy(h, [8, 10, 16, 18], list(range(4, 33, 4)), [6, 9, 12, 8, 14], soul=SOUL)
    for (x, z, d) in ((9, 8, 12), (17, 8, 14), (9, 26, 10), (17, 27, 16), (10, 31, 13), (16, 31, 11), (12, 6, 9), (14, 6, 11), (9, 14, 15), (17, 18, 13)):
        hang_tome(h, x, z, d)
    for lat in (10, 16):
        D.ladder_run(wn, lat, GROUND, GROUND + 9)
        D.ladder_run(ws, lat, GROUND, GROUND + 9)
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("cultist", 13, 26)
    K.auto_light(h, soul=SOUL, max_lamps=40)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


def shelves_and_windows(h, wl, lo, hi, windows, glass="minecraft:purple_stained_glass", rows=(GROUND, GROUND + 6), win=(GROUND + 9, GROUND + 12), chiseled=0.08):
    """Shelving on a wall from `lo` to `hi`: full height over `rows`, and above them everywhere except the window columns `windows`, where a tall slit of `glass`
    lit by a shroomlight in the outer wall layer stands instead (a library lives by lamplight, but a hall this high needs somewhere for the dusk to come in)."""
    D.shelf_wall(wl, lo, hi, rows[0], rows[1], chiseled=chiseled)
    for lat in range(lo, hi + 1):
        if lat in windows:
            D.window_slit(wl, lat, win[0], win[1], glow="minecraft:shroomlight", glass=glass)
        else:
            D.shelf_wall(wl, lat, lat, rows[1] + 1, h.top - 1, chiseled=chiseled)


def reading_table(h, x, z0, length, chair_side, marker_k=2, candle_ks=(0, 4)):
    """One long reading table along z with stools down one side, candles and a `furniture;as=table` marker; `chair_side` is the compass direction the sitters
    face (towards the table)."""
    D.table(h.frame(x, GROUND, z0, "south"), length)
    cx = x - 1 if chair_side == "east" else x + 1
    for k in (1, length - 2):
        D.chair(h.frame(cx, GROUND, z0 + k, chair_side), seat_marker=(k == 1))
    for k in candle_ks:
        if 0 <= k < length:
            D.candles_at(h, x, GROUND + 1, z0 + k, 3, "purple")
    h.marker(x, GROUND + 1, z0 + marker_k, "furniture", **{"as": "table"})


@room("cursed_archive_reading_hall", "hall")
def reading_hall():
    """The Reading Hall: a long nave of candle-lit tables with a purple runner between two rows, covered aisles along the walls, shelves to the windows, and a
    wheel of candles over every third table. The place where the scholars still sit."""
    h = Hall("mega/hall/cursed_archive_reading_hall", 2, 4, 14, wear=0.10)
    for off in (4, 13):
        h.door("n", off)
        h.door("s", off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    h.route(3, 2, 5, 33, label="west aisle")
    h.route(12, 2, 14, 33, label="east aisle")
    for off in (4, 13):
        D.door_trim(h, "n", off)
        D.door_trim(h, "s", off)
    for wl in (ww, we):
        shelves_and_windows(h, wl, 2, 33, set(range(5, 33, 5)))
    for wl in (wn, ws):
        D.shelf_wall(wl, 2, 3, GROUND, h.top - 1)
        D.shelf_wall(wl, 15, 15, GROUND, h.top - 1)
        D.shelf_wall(wl, 6, 11, GROUND + 4, h.top - 1)
        D.shelf_wall(wl, 6, 11, GROUND, GROUND + 3)
    # the tables: four blocks of two long tables, with two-wide gaps to cross the nave
    for z0 in (5, 12, 19, 26):
        reading_table(h, 7, z0, 5, "east")
        reading_table(h, 10, z0, 5, "west")
    for z in range(3, 33):
        for x in (8, 9):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    D.floor_border(h, 1)
    for z in (8, 18, 28):
        D.chandelier_wheel(h, 8, z, r=2, drop=4, candles=True)
    D.beams(h, "x", [4, 13, 23, 32], block=L.STRIPPED_LOG)
    ink_lamps(h, (ww, we), (GROUND + 2, GROUND + 6, GROUND + 11), range(3, 33, 6))
    ink_lamps(h, (wn, ws), (GROUND + 6, GROUND + 11), (3, 15))
    D.upper_dress(h, [D.it_tall_banner("purple", hang=4)], sides="ns", step=(5, 7))
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("cultist", 8, 18)
    K.auto_light(h, soul=SOUL, max_lamps=30)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


LOOT = "nusmp:chests/labyrinth_trial"
LOOT_BOOKS = "nusmp:chests/library_ruins"


def scribe_station(h, x, z, facing="north", skeleton=False):
    """A scribe's place: a lectern with a book, a stool facing it, an ink pot (a wither rose in a pot, on a barrel) beside it and, when `skeleton`, the scribe
    himself, an armour stand in leather with a skull for a head. `facing` is the way the scribe looks (towards the lectern)."""
    d = D.DV[facing]
    right = D.DV[D.RIGHT[facing]]
    D.lectern(h.frame(x, GROUND, z, facing))
    sx, sz = x - d[0], z - d[1]
    if skeleton:
        yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
        h.armor_stand(sx, GROUND, sz, yaw=yaw, head="minecraft:skeleton_skull", chest="minecraft:leather_chestplate", legs="minecraft:leather_leggings",
                      feet="minecraft:leather_boots", mainhand="minecraft:feather", dy=0.0)
    else:
        D.chair(h.frame(sx, GROUND, sz, facing))
    h.try_set(x + right[0], GROUND, z + right[1], "minecraft:barrel", {"facing": "up", "open": False})
    h.try_set(x + right[0], GROUND + 1, z + right[1], "minecraft:potted_wither_rose")


@room("cursed_archive_scriptorium", "hall")
def scriptorium():
    """The Scriptorium: six scribes' stations either side of a central aisle (lecterns, stools, ink pots of wither rose, two scribes who never left), a
    bookbinder's bench with a press, vats of ink under black candles, shelves to the rafters and lamps of pale violet in the walls."""
    h = Hall("mega/hall/cursed_archive_scriptorium", 3, 2, 12, wear=0.10)
    h.door("n", 13)
    h.door("s", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    h.route(12, 2, 14, 15, label="aisle")
    D.door_trim(h, "n", 13)
    D.door_trim(h, "s", 13)
    D.floor_chequer(h, 2, 2, 24, 15, block=L.POLISHED)
    for lat in (8, 18):
        D.pilaster(wn, lat, 1, y0=GROUND)
        D.pilaster(ws, lat, 1, y0=GROUND)
    for wl in (ww, we):
        D.shelf_wall(wl, 2, 15, GROUND, h.top - 1, chiseled=0.1)
    for wl in (wn, ws):
        D.shelf_wall(wl, 2, 10, GROUND + 3, h.top - 1, chiseled=0.1)
        D.shelf_wall(wl, 16, 24, GROUND + 3, h.top - 1, chiseled=0.1)
    # the stations: two rows of three on each side of the aisle
    for z, facing_ in ((5, "north"), (11, "north")):
        for x in (4, 7, 10):
            scribe_station(h, x, z, facing_, skeleton=(x == 7 and z == 11))
        for x in (16, 19, 22):
            scribe_station(h, x, z, facing_, skeleton=(x == 19 and z == 5))
    for z in (4, 10):
        for x in range(3, 11):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
        for x in range(16, 24):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    # the binder's bench against the south wall (west) and the vats of ink (east)
    for x in (3, 5, 7):
        h.set(x, GROUND, 14, ["minecraft:cartography_table", "minecraft:fletching_table", "minecraft:smithing_table"][(x - 3) // 2])
    h.set(9, GROUND, 14, "minecraft:iron_block")
    h.set(9, GROUND + 1, 14, "minecraft:anvil", {"facing": "east"})
    for x in (17, 19, 21, 23):
        h.set(x, GROUND, 14, "minecraft:cauldron")
        D.candles_at(h, x, GROUND, 15, 3, "black")
    h.marker(7, GROUND + 1, 14, "furniture", **{"as": "table"})
    ink_lamps(h, (ww, we), (GROUND + 2, GROUND + 6), (4, 10, 14))
    ink_lamps(h, (wn, ws), (GROUND + 4, GROUND + 7), (5, 9, 17, 21))
    D.chandelier_wheel(h, 8, 8, r=2, drop=3, candles=True)
    D.chandelier_wheel(h, 18, 8, r=2, drop=3, candles=True)
    hang_tome(h, 13, 5, 6)
    hang_tome(h, 13, 12, 7)
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("construct", 13, 8)
    K.auto_light(h, soul=SOUL, max_lamps=30)
    D.auto_markers(h, spawns=5, patrols=2)
    return h


@room("cursed_archive_restricted_section", "treasure")
def restricted_section():
    """The Restricted Section: a reading nook with a skeleton librarian at the gate, an iron-barred cage behind it where the forbidden books are chained to
    their lecterns, three chests on plinths among black candles, and a gate left ajar (a one-wide gap between two curtains of chain)."""
    h = Hall("mega/treasure/cursed_archive_restricted_section", 2, 2, 12, wear=0.10)
    h.door("n", 4)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.door_trim(h, "n", 4)
    # the bars across the room under a polished lintel, a one-wide gap at x = 4 with a curtain of chains on either side of it
    for x in range(2, 16):
        h.set(x, GROUND + 4, 9, L.CHISELED if x == 4 else L.POLISHED)
        for y in range(GROUND, GROUND + 4):
            if x == 4:
                continue
            if x in (3, 5):
                h.set(x, y, 9, L.CHAIN_BLOCK, {"axis": "y"})
            else:
                h.set(x, y, 9, L.IRON_BARS)
    # the cage: shelves round it, three plinths with a chest each, lecterns with books that are chained to the roof
    D.shelf_wall(ww, 10, 15, GROUND, h.top - 1)
    D.shelf_wall(we, 10, 15, GROUND, h.top - 1)
    D.shelf_wall(ws, 2, 15, GROUND + 3, h.top - 1)
    for x in (4, 8, 12):
        h.set(x, GROUND, 14, L.POLISHED)
        h.loot_chest(x, GROUND + 1, 14, "north", LOOT_BOOKS)
        for dx in (-1, 1):
            D.candles_at(h, x + dx, GROUND, 14, 3, "black")
    for (x, z) in ((6, 11), (10, 11), (4, 12), (14, 12)):
        D.lectern(h.frame(x, GROUND, z, "south"))
    for (x, z) in ((6, 11), (10, 11)):
        for y in range(GROUND + 1, h.top + 1):
            h.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
    # the reading nook in front of the gate: a desk, the skeleton librarian, a purple runner to the gap
    D.table(h.frame(10, GROUND, 4, "south"), 4)
    D.chair(h.frame(11, GROUND, 5, "west"))
    D.candles_at(h, 10, GROUND + 1, 4, 3, "purple")
    h.marker(10, GROUND + 1, 6, "furniture", **{"as": "table"})
    h.armor_stand(7, GROUND, 8, yaw=180.0, head="minecraft:wither_skeleton_skull", chest="minecraft:leather_chestplate", legs="minecraft:leather_leggings",
                  feet="minecraft:leather_boots", mainhand="minecraft:iron_axe", dy=0.0)
    for x in (3, 4, 5):
        h.try_set(x, GROUND, 8, "minecraft:purple_carpet")
    ink_lamps(h, (ww, we), (GROUND + 3, GROUND + 7), (11, 13))
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("construct", 8, 12)
    K.auto_light(h, soul=SOUL, max_lamps=20)
    D.auto_markers(h, spawns=2, patrols=1, centre=False)
    return h


@room("cursed_archive_librarians_study", "safe")
def librarians_study():
    """The Librarian's Study: the one quiet room of the Archive, a fire in a cobbled hearth, three armchairs, a desk with a lamp, a bed in the corner and a
    rug of purple. Nothing in it is hostile and nothing spawns in it: the rest anchors are the seats, the hearth and the bed."""
    h = Hall("mega/safe/cursed_archive_librarians_study", 2, 2, 12, wear=0.04)
    h.door("n", 4)
    h.door("e", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.door_trim(h, "n", 4)
    D.door_trim(h, "e", 13)
    D.floor_border(h, 1)
    D.floor_medallion(h, 8, 9, 4)
    # the hearth in the middle of the west wall: the campfire, a smoker and a cauldron, a chimney breast; the shelves round it
    fire = D.fireplace(ww, 8, width=3, soul=SOUL)
    h.marker(fire[0], fire[1] + 1, fire[2], "hearth", **{"as": "stove"})
    D.shelf_wall(ww, 2, 15, GROUND, h.top - 1, chiseled=0.12)
    D.shelf_wall(ws, 2, 15, GROUND, h.top - 1, chiseled=0.12)
    D.shelf_wall(wn, 7, 15, GROUND, h.top - 1, chiseled=0.12)
    D.shelf_wall(we, 2, 9, GROUND, h.top - 1, chiseled=0.12)
    for z in (8, 10, 12):
        D.chair(h.frame(5, GROUND, z, "west"), seat_marker=True)
    for z in range(7, 14):
        for x in (6, 7):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    # the desk by the south wall, the bed in the north-east corner
    D.table(h.frame(3, GROUND, 14, "east"), 4)
    D.chair(h.frame(4, GROUND, 13, "south"))
    D.candles_at(h, 6, GROUND + 1, 14, 3, "purple")
    h.marker(5, GROUND + 1, 14, "furniture", **{"as": "table"})
    D.bed(h.frame(13, GROUND, 4, "south"), color="purple")
    ink_lamps(h, (ww, we, wn, ws), (GROUND + 2, GROUND + 6), (4, 12))
    D.chandelier_wheel(h, 9, 9, r=2, drop=3, candles=True)
    D.cornice(h)
    D.cobwebs(h, 0.5)
    K.auto_light(h, soul=SOUL, max_lamps=20)
    h.marker(8, GROUND, 6, "patrol")
    return h


def fire_trap(h, z, ids=(11, 15)):
    """A tripwire across the spine at row z with a hook on a post at each side and a dispenser of fire charges above each post, aimed across the spine at
    chest height: whoever touches the string is shot at from both sides at once. The hook powers the post it hangs on, and the dispenser above that post
    reads the power (the usual vanilla tripwire trap). `ids` are the x of the two posts; the spine between them is x = 12..14, the string at x = 13."""
    import nbt as _nbt
    xw, xe = ids
    charges = _nbt.Compound({"id": "minecraft:dispenser", "Items": _nbt.List(
        [_nbt.Compound({"Slot": _nbt.Byte(i), "id": "minecraft:fire_charge", "count": _nbt.Int(16)}) for i in range(3)], tag_type=_nbt.TAG_COMPOUND)})
    for x, toward in ((xw, "east"), (xe, "west")):
        h.set(x, GROUND, z, L.POLISHED)
        h.set(x, GROUND + 1, z, "minecraft:dispenser", {"facing": toward, "triggered": False}, nbt=charges)
        h.set(x, GROUND + 2, z, L.CHISELED)
    h.set(xw + 1, GROUND, z, "minecraft:tripwire_hook", {"facing": "east", "attached": True, "powered": False})
    h.set(xe - 1, GROUND, z, "minecraft:tripwire_hook", {"facing": "west", "attached": True, "powered": False})
    h.set(13, GROUND, z, "minecraft:tripwire", {"east": True, "west": True, "north": False, "south": False, "attached": True, "powered": False, "disarmed": False})
    h.marker(13, GROUND + 1, z, "trap", type="fire")


@room("cursed_archive_burning_stacks", "trap")
def burning_stacks():
    """The Burning Stacks: a hall of shelf rows either side of a three-wide spine. Three tripwires cross the spine, and each one is guarded by a pair of
    hidden dispensers of fire charges in the pillars at its sides; the shelves are the fuel. Soot, scorched stone and a charred reading desk say what the hall
    is for to anyone who looks. The spine is clear of anything flammable and the walls are masonry, so the fire stays in the hall."""
    h = Hall("mega/trap/cursed_archive_burning_stacks", 3, 3, 14, wear=0.16)
    h.door("n", 13)
    h.door("s", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    h.route(12, 2, 14, 24, label="spine")
    D.door_trim(h, "n", 13)
    D.door_trim(h, "s", 13)
    # the shelf rows: three rows of tall shelves on each side, two deep, with two-wide aisles between them opening on the spine
    for z0 in (4, 9, 14, 19):
        for x0, x1 in ((3, 9), (17, 23)):
            for x in range(x0, x1 + 1):
                for z in range(z0, z0 + 2):
                    for y in range(GROUND, GROUND + 5):
                        if y == GROUND + 4:
                            h.set(x, y, z, L.PLANKS_SLAB if (x + z) % 2 else L.PLANKS, {"type": "bottom"} if (x + z) % 2 else None)
                        else:
                            h.set(x, y, z, "minecraft:bookshelf")
    for z in (9, 15, 21):
        fire_trap(h, z)
    # the scorched stone: soot on the floor of the spine's edges and the aisles, cracked and black brick under the shelf rows
    for z in range(3, 24):
        for x in (3, 10, 16, 23):
            c = h.get(x, FLOOR, z)
            if c is not None and c.block == L.TILE and h.rng.random() < 0.5:
                h.floor_set(x, z, L.CRACKED_TILE)
    D.cobwebs(h, 0.6)
    D.shelf_wall(ww, 2, 24, GROUND, h.top - 1)
    D.shelf_wall(we, 2, 24, GROUND, h.top - 1)
    ink_lamps(h, (ww, we), (GROUND + 2, GROUND + 6), range(4, 24, 5))
    for z0 in (4, 9, 14, 19):                                   # a lamp in the end of every shelf row, facing the spine
        for x in (9, 17):
            h.set(x, GROUND + 2, z0, INK, over=True)
    D.cornice(h)
    h.faction("construct", 13, 12)
    K.auto_light(h, soul=SOUL, max_lamps=30)
    D.auto_markers(h, spawns=5, patrols=2)
    return h


def cabinet(h, x0, x1, z0, z1, rows=3):
    """A bank of card-catalogue drawers: barrels (the drawer fronts) and chiselled bookshelves (the indexes of loose cards) `rows` high with a polished slab on
    top, every face that looks into a lane turned to the lane. Why: the brief's card-catalogue maze, from two vanilla blocks that already look like drawers."""
    for z in range(z0, z1 + 1):
        for x in range(x0, x1 + 1):
            outward = "west" if x == x0 else "east" if x == x1 else "north" if z == z0 else "south" if z == z1 else "up"
            for k in range(rows):
                y = GROUND + k
                if (x + z + k) % 3 == 0 and outward != "up":
                    props, data = D.chiseled_shelf(h, outward, 0.8)
                    h.set(x, y, z, "minecraft:chiseled_bookshelf", props, nbt=data)
                else:
                    h.set(x, y, z, "minecraft:barrel", {"facing": outward, "open": False})
            h.set(x, GROUND + rows, z, L.POLISHED_SLAB, {"type": "bottom"})


SEQUENCE = (1, 0, 3, 2)                 # the order the Riddle Stone asks for: different from both existing sigil halls (2,3,1,0) and (0,2,1,3)


@room("cursed_archive_card_catalogue", "puzzle")
def card_catalogue():
    """The Card Catalogue: banks of drawers make lanes, four dead-end lanes hold the four sigil plates, the Riddle Stone stands at the south wall and the
    puzzle door in the east wall keeps two chests. The same blocks and block entity as the Spire's puzzle room (and the two sigil halls)."""
    import gen_mega_rooms2 as R2
    h = Hall("mega/puzzle/cursed_archive_card_catalogue", 3, 2, 12, wear=0.08)
    h.door("w", 13)
    h.door("n", 13)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    h.route(2, 12, 18, 14, label="main lane")
    h.route(12, 2, 14, 14, label="north lane")
    h.route(7, 3, 9, 11, label="lane one")
    h.route(17, 3, 18, 11, label="lane two")
    R2.sigil_closet(h.mt, L.Frame(h.mt, 18, GROUND, 8, "east"), h.rng)
    D.door_trim(h, "w", 13)
    D.door_trim(h, "n", 13)
    for (a, b, c, d) in ((3, 6, 3, 10), (10, 11, 3, 10), (15, 16, 3, 10)):
        cabinet(h, a, b, c, d)
    for (a, b) in ((2, 8), (10, 12), (14, 16)):
        for x in range(a, b + 1):
            for k in range(3):
                h.try_set(x, GROUND + k, 15, "minecraft:barrel", {"facing": "north", "open": False})
            h.try_set(x, GROUND + 3, 15, L.POLISHED_SLAB, {"type": "bottom"})
    colours = [0, 1, 2, 3]
    h.rng.shuffle(colours)
    for (x, z), c in zip(((8, 4), (17, 4), (9, 15), (17, 15)), colours):
        h.mt.set(x, GROUND, z, R2.PLATE, {"color": str(c), "lit": "false"})
    h.mt.set(13, GROUND, 15, R2.RIDDLE, None, {"id": R2.RIDDLE, "sequence": R2.nbt_list(list(SEQUENCE))})
    K.hung_lantern_under(h, 13, 15, GROUND, GROUND + 3)
    for (x, z) in ((4, 12), (11, 13)):
        h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    for z in range(3, 13):
        for x in (7, 8, 9):
            h.try_set(x, GROUND, z, "minecraft:purple_carpet")
    D.floor_medallion(h, 13, 8, 4)
    ink_lamps(h, (wn, ws), (GROUND + 2,), (4, 8, 16, 20))
    D.chandelier_wheel(h, 8, 8, r=2, drop=3, candles=True)
    D.cornice(h)
    D.cobwebs(h, 0.7)
    h.faction("cultist", 5, 13)
    K.auto_light(h, soul=SOUL, max_lamps=30)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    return h


@REG.check("cursed_archive_card_catalogue")
def card_catalogue_check(h):
    """The shared checks of the Spire's puzzle rooms: one plate of each colour, one stone with a valid order, every gate a puzzle gate, the chests unreachable
    with the gate shut and reachable with it open, everything the stone scans for within its reach."""
    import gen_mega_rooms2 as R2
    return ["puzzle check: " + e for e in R2.check_puzzle(h.mt, None)]


# ----------------------------------------------------------------------------------------------------------------------------------- the rotunda
CX = CZ = 18.0                      # the centre of the 36 x 36 footprint, in cell-centre coordinates (a cell (x, z) has its centre at x + 0.5, z + 0.5)
R_HALL = 16.0                       # the hall is round: every interior cell whose centre is within this of the middle belongs to it, the four corners are masonry
R_SHELF = 13.8                      # the shelves of the outer wall are two cells deep, from here out to R_HALL
R_RING = 10.9                       # the galleries run from here out to R_SHELF
R_DRUM = 4.0                        # the column of chained tomes
R_SPIRAL = (4.9, 7.9)               # the spiral stair round the column: three cells wide
THETA = (0.30, 2.94, 3.34, 5.70, 6.00)      # start, end of the first climb, end of the first landing, end of the second climb, end of the second landing
LEVEL1, LEVEL2 = 19, 28             # the rows the two galleries are stood on (their decks are one row lower)


def rdist(x, z):
    return math.hypot(x + 0.5 - CX, z + 0.5 - CZ)


def rang(x, z):
    return math.atan2(z + 0.5 - CZ, x + 0.5 - CX) % (2 * math.pi)


def spiral_height(theta):
    """The row you stand on at angle `theta` (radians, clockwise seen from above, 0 = east) on the spiral, or None where the spiral is not: a climb to the first
    gallery, a landing, a climb to the second, a landing."""
    a, b, c, d, e = THETA
    if theta < a or theta > e:
        return None
    if theta <= b:
        return 9 + int(round((LEVEL1 - 9) * (theta - a) / (b - a)))
    if theta <= c:
        return LEVEL1
    if theta <= d:
        return LEVEL1 + int(round((LEVEL2 - LEVEL1) * (theta - c) / (d - c)))
    return LEVEL2


@room("cursed_archive_rotunda", "set")
def rotunda():
    """The Rotunda of Chained Tomes: a round reading hall twenty-eight rows high. In the middle a drum of chiselled bookshelves wrapped in hanging chains and
    crowned with weeping obsidian; a spiral stair winds round it from the floor to two galleries that ring the hall in front of the shelves (a bridge leads to
    each); at the foot of the drum, in a niche that faces the east door, stands a lectern of black fire. The hall's four corners are masonry, so the room is a
    cylinder inside the square the grid demands."""
    h = Hall("mega/set/cursed_archive_rotunda", 4, 4, 28, wear=0.06)
    for side, off in (("n", 13), ("e", 22), ("s", 22), ("w", 13)):
        h.door(side, off)
        D.door_trim(h, side, off)
    top = h.top
    cells = [(x, z) for x in range(h.x0, h.x1 + 1) for z in range(h.z0, h.z1 + 1)]
    # 1. the four corners, solid from the floor to the ceiling (the doors' aprons stay open: try_set skips them)
    for (x, z) in cells:
        if rdist(x, z) > R_HALL:
            for y in range(GROUND, top + 1):
                h.try_set(x, y, z, D.brick_mix(h, 0.12, 0.08))
    # 2. the drum of chained tomes: chiselled bookshelves with a band of iron bars every fifth row, a crown of crying obsidian, the niche of the lectern
    niche_cells = {(x, y, z) for x in (21, 22) for z in (16, 17, 18) for y in (GROUND, GROUND + 1, GROUND + 2)}
    for (x, z) in cells:
        if rdist(x, z) <= R_DRUM:
            for y in range(GROUND, top):
                if (x, y, z) in niche_cells:
                    continue
                if y == top - 1:
                    blk, props, data = "minecraft:crying_obsidian", None, None
                elif (y - GROUND) % 5 == 4:
                    blk, props, data = L.IRON_BARS, None, None
                else:
                    blk, (props, data) = "minecraft:chiseled_bookshelf", D.chiseled_shelf(h, "south", 0.9)
                if data is None:
                    h.try_set(x, y, z, blk, props)
                else:
                    h.set(x, y, z, blk, props, nbt=data)
    # 3. the spiral: for every cell of the annulus the row it is stood on; a stair where the cell next to it (behind it along the climb) is one lower, a block elsewhere
    lo, hi = R_SPIRAL
    spiral = {}
    for (x, z) in cells:
        if lo <= rdist(x, z) <= hi:
            level = spiral_height(rang(x, z))
            if level is not None:
                spiral[(x, z)] = level
    for (x, z), level in sorted(spiral.items()):
        if level <= GROUND:
            continue
        below = None
        for (dx, dz, face) in ((1, 0, "east"), (0, 1, "south"), (-1, 0, "west"), (0, -1, "north")):
            nb = spiral.get((x - dx, z - dz), GROUND if (x - dx, z - dz) not in spiral and rdist(x - dx, z - dz) > hi else None)
            if nb == level - 1 and (below is None):
                below = face
        row = level - 1
        rib = (int(rang(x, z) / 0.33) % 3 == 0)
        low = GROUND if rib else max(GROUND, row - 2)
        for y in range(low, row):
            h.try_set(x, y, z, D.brick_mix(h, 0.15, 0.1))
        if below is not None:
            h.set(x, row, z, L.PLANKS_STAIRS, {"facing": below, "half": "bottom"})
        else:
            h.set(x, row, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"})
    # 4. the two bridges (west to the first gallery, east to the second) and the two gallery rings with their posts and rails
    bridge1 = {(x, z) for (x, z) in cells if abs(z + 0.5 - CZ) <= 2.0 and x + 0.5 < CX and hi < rdist(x, z) <= R_SHELF - 0.5}
    bridge2 = {(x, z) for (x, z) in cells if 13 <= z <= 16 and x + 0.5 > CX and hi < rdist(x, z) <= R_SHELF - 0.5}
    for (bridge, level) in ((bridge1, LEVEL1), (bridge2, LEVEL2)):
        for (x, z) in sorted(bridge):
            h.try_set(x, level - 1, z, L.PLANKS if (x + z) % 3 else L.STRIPPED_LOG, None if (x + z) % 3 else {"axis": "x"})
    ring = [(x, z) for (x, z) in cells if R_RING < rdist(x, z) <= R_SHELF]
    for (x, z) in ring:
        for level in (LEVEL1, LEVEL2):
            h.try_set(x, level - 1, z, L.PLANKS if (x + z) % 3 else L.STRIPPED_LOG, None if (x + z) % 3 else {"axis": "z"})
    near_bridge = {(x + dx, z + dz) for (x, z) in bridge1 | bridge2 for dx in (-1, 0, 1) for dz in (-1, 0, 1)}
    for (x, z) in ring:
        if rdist(x, z) <= R_RING + 1.0 and (x, z) not in near_bridge:
            for level in (LEVEL1, LEVEL2):
                h.try_set(x, level, z, L.FENCE)
    for (bridge, level) in ((bridge1, LEVEL1), (bridge2, LEVEL2)):
        for (x, z) in sorted(bridge):
            if (x, z + 1) not in bridge or (x, z - 1) not in bridge:
                h.try_set(x, level, z, L.FENCE)
    for (x, z), level in sorted(spiral.items()):
        edge = rdist(x, z) > hi - 1.2
        if level > GROUND and edge and not any((x + dx, z + dz) in bridge1 | bridge2 for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
            h.try_set(x, level, z, L.FENCE)
    # posts under the rings, twelve of them, and capitals of the rails
    for k in range(12):
        a = k * math.pi / 6 + math.pi / 12
        px, pz = int(CX + (R_SHELF - 0.4) * math.cos(a)), int(CZ + (R_SHELF - 0.4) * math.sin(a))
        for y in range(GROUND, LEVEL1 - 1):
            h.try_set(px, y, pz, L.POLISHED if y < LEVEL1 - 2 else L.CHISELED)
        for y in range(LEVEL1, LEVEL2 - 1):
            h.try_set(px, y, pz, L.POLISHED if y < LEVEL2 - 2 else L.CHISELED)
    # 5. the shelves of the outer wall, two deep, from the floor to the ceiling, with a lamp of ink now and then
    for (x, z) in cells:
        d = rdist(x, z)
        if R_SHELF < d <= R_HALL:
            for y in range(GROUND, top):
                if h.rng.random() < 0.07:
                    props, data = D.chiseled_shelf(h, "south", 0.8)
                    if h.can_set(x, y, z, "minecraft:chiseled_bookshelf"):
                        h.set(x, y, z, "minecraft:chiseled_bookshelf", props, nbt=data)
                else:
                    h.try_set(x, y, z, "minecraft:bookshelf")
    lamps = 0
    for tier, y in enumerate((11, 15, 20, 24, 29, 33)):
        for k in range(24):
            a = (k + 0.5 * (tier % 2)) * math.pi / 12
            px, pz = int(CX + 14.3 * math.cos(a)), int(CZ + 14.3 * math.sin(a))
            c = h.get(px, y, pz)
            if c is not None and c.block in ("minecraft:bookshelf", "minecraft:chiseled_bookshelf") and rdist(px, pz) <= R_HALL - 1.5:
                h.set(px, y, pz, INK, over=True)
                lamps += 1
    for y in (GROUND + 3, GROUND + 12, GROUND + 21):
        for k in range(8):
            a = k * math.pi / 4
            px, pz = int(CX + 3.4 * math.cos(a)), int(CZ + 3.4 * math.sin(a))
            c = h.get(px, y, pz)
            if c is not None and c.block == "minecraft:chiseled_bookshelf":
                h.set(px, y, pz, INK, over=True)
    # 6. the chains round the drum (a wall of them, from the ceiling to the floor, every other cell of the ring) and the lectern of black fire in its niche
    chain_ring = sorted([(rang(x, z), x, z) for (x, z) in cells if R_DRUM < rdist(x, z) <= 4.7 and not (x > 18 and 14 <= z <= 21)])
    for n, (_a, x, z) in enumerate(chain_ring):
        if n % 2 == 0:
            for y in range(GROUND, top + 1):
                h.try_set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
    D.lectern(h.frame(21, GROUND, 17, "west"))
    for z in (16, 18):
        h.set(21, GROUND, z, "minecraft:soul_campfire", {"facing": "west", "lit": True, "signal_fire": False})
        D.candles_at(h, 22, GROUND, z, 4, "black")
    if h.free(21, GROUND + 3, 17):
        h.set(21, GROUND + 3, 17, L.CHISELED)
    # 7. the floor: rings and spokes of polished stone and crying obsidian, a reading station under the first gallery every thirty degrees
    for (x, z) in cells:
        d = rdist(x, z)
        c = h.get(x, FLOOR, z)
        if c is None or c.block != L.TILE:
            continue
        if abs(d - 8.4) < 0.55:
            h.floor_set(x, z, L.POLISHED)
        elif abs(d - 12.0) < 0.55:
            h.floor_set(x, z, L.CHISELED)
    for k in range(12):
        a = k * math.pi / 6
        lx, lz = int(CX + 13.2 * math.cos(a)), int(CZ + 13.2 * math.sin(a))
        facing_out = ("east" if math.cos(a) > 0 else "west") if abs(math.cos(a)) >= abs(math.sin(a)) else ("south" if math.sin(a) > 0 else "north")
        d = D.DV[facing_out]
        sx, sz = lx - d[0], lz - d[1]
        if h.can_set(lx, GROUND, lz, "minecraft:lectern") and h.can_set(sx, GROUND, sz, L.PLANKS_STAIRS):
            D.lectern(h.frame(lx, GROUND, lz, facing_out))
            D.chair(h.frame(sx, GROUND, sz, facing_out))
    # lamps in the masonry beside every doorway's vestibule (the shelves stop at the corners, and a vestibule four cells long is a dark mouth otherwise)
    for (side, off) in h.doors:
        w = h.wall(side)
        for lat in (off - 2, off + 2):
            for d in (1, 3):
                for y in (GROUND + 2, GROUND + 6):
                    pos = w.cell(lat, y, d)
                    c = h.get(*pos)
                    if c is not None and c.block not in (INK,) and D.classify(c.block, c.props)[0] != "pass":
                        h.set(pos[0], pos[1], pos[2], INK, over=True)
    h.faction("cultist", 12, 18)
    K.auto_light(h, soul=SOUL, max_lamps=60)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


if __name__ == "__main__":
    sys.exit(K.main(REG, sys.argv[1:], calm=CALM))

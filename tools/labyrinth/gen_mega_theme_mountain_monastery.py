#!/usr/bin/env python3
"""
MOUNTAIN_MONASTERY (1.59.0, docs/labyrinth/mega_v2_tha.md): the signature rooms of a monks' retreat on the Holy road: timber and paper, bamboo and red lacquer,
stairs cut into a cliff, a garden raked round a few stones. Rooms (all `mountain_monastery_*`, drawn ONLY in sectors of the theme through
`data/nusmp/mega/template_themes/mountain_monastery.json`):

    hall/mountain_monastery_dojo               the training hall: a mat ringed in red, dummies, weapon racks, a shrine shelf and a bell, giant paper lanterns
    hall/mountain_monastery_prayer_wheel_hall  a hall round one great prayer wheel, rows of small wheels along the walls, a gallery over them
    hall/mountain_monastery_scroll_library     two floors of scroll racks and bookshelves, a stair to the gallery, reading desks, paper lanterns
    hall/mountain_monastery_zen_garden         an open courtyard: raked stone, a pond with a red bridge, a cherry tree, bamboo, stone lanterns, a veranda
    set/mountain_monastery_cliff_stairs        the stair cut into the cliff: switchbacks up a rock face, a torii on every landing, a shrine at the top
    safe/mountain_monastery_hot_springs        the bathhouse: two hot pools with benches, a stove, three futons under a hipped roof (a Safe Zone)
    trial/mountain_monastery_duel_ring         the duel ring: a raised mat in a square hall; walking in seals the doors and the guardians of the ring come

    python3 tools/labyrinth/gen_mega_theme_mountain_monastery.py              # (re)write every room, check each one (a room with a defect is NOT written)
    python3 tools/labyrinth/gen_mega_theme_mountain_monastery.py dojo         # only this one
    python3 tools/labyrinth/gen_mega_theme_mountain_monastery.py --check      # write nothing; compare with the files on disk (CI style)

Why a generator: see gen_mega_rooms1.py; the same kit (`mega_decor_r1`) and the same gates run here, plus `mega_tha_kit` for windows, roofs, decks and trials.
Nothing in a room names the theme's wood or stone: the rooms are written in the deepslate palette (`mega_lib`) and the Region Theme turns that into bamboo mosaic,
dark-oak boards, red terracotta and spruce, which is what makes one hall a dojo in this sector and something else in another theme's.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_tha_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, Hall  # noqa: E402

ROOMS = {}
THEME = "mountain_monastery"
PAPER = "minecraft:white_stained_glass_pane"
PAPER_BLOCK = "minecraft:white_stained_glass"
GLOW = "minecraft:glowstone"                                 # behind paper: a warm light, never the cold white of a sea lantern


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


def polar(cx, cz, r, deg):
    return int(round(cx + r * math.cos(math.radians(deg)))), int(round(cz + r * math.sin(math.radians(deg))))


# ===================================================================================================================
# the vocabulary of the monastery: paper screens, paper lanterns, stone lanterns, a torii
# ===================================================================================================================
def shoji(w, lat, y0, y1, width=3):
    """A paper screen set in the wall: `width` cells wide, rows y0..y1, white panes in a lattice of timber (a rail on the bottom, top and middle rows and a stile
    on every second cell), a warm glow behind the paper. Skipped where the wall holds anything but plain wall. Returns the number of paper cells."""
    half = width // 2
    lats = list(range(lat - half, lat + half + 1))
    if not K.window_ok(w, lats, y0, y1):
        return 0
    n = 0
    mid = (y0 + y1) // 2
    for y in range(y0, y1 + 1):
        for i, lt in enumerate(lats):
            frame = y in (y0, y1, mid) or lt in (lats[0], lats[-1]) or (i % 2 == 0 and width > 3)
            if frame:
                w.carve(lt, y, -1)
                w.shell_set(lt, y, -1, L.PLANKS)
            elif K.window_cell(w, lt, y, PAPER, GLOW):
                n += 1
    return n


def band_of_shoji(w, lats, y0, y1, width=3):
    return sum(1 for lt in lats if shoji(w, lt, y0, y1, width))


def paper_lantern(h, x, z, y_centre, y_top):
    """A giant paper lantern: a three by three by three cube of white glass round a lantern that hangs from a chain to the ceiling (or a beam) at row y_top. The
    glass is only a shade (light goes through it), so the lantern lights the room the way an ordinary one does. Why: the brief's `white wool paper-lantern look`; a
    glass shade shows the flame, wool would hide it and make the cube a block of cloth. Returns False when the cells are not free."""
    cube = [(x + dx, y_centre + dy, z + dz) for dz in (-1, 0, 1) for dy in (-1, 0, 1) for dx in (-1, 0, 1)]
    chain = [(x, y, z) for y in range(y_centre + 2, y_top + 1)]
    if not all(h.can_set(*c, PAPER_BLOCK) and h.free(*c) for c in cube if (c[0], c[2]) != (x, z) or c[1] == y_centre - 1):
        return False
    if not all(h.free(*c) for c in chain) or not h.free(x, y_centre + 1, z) or not h.free(x, y_centre, z):
        return False
    for (cx, cy, cz) in cube:
        if (cx, cz) == (x, z) and cy >= y_centre:
            continue
        h.set(cx, cy, cz, PAPER_BLOCK)
    h.set(x, y_centre, z, L.LANTERN, {"hanging": "true"})
    for y in range(y_centre + 1, y_top + 1):
        h.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
    return True


def stone_lantern(h, x, z, y=GROUND):
    """A stone lantern (a toro): two posts of wall, a lantern standing on them, a slab for its roof. Returns False when the cells are not free."""
    cells = [(x, y), (x, y + 1), (x, y + 2), (x, y + 3)]
    if not all(h.free(x, yy, z) for (_x, yy) in cells):
        return False
    h.set(x, y, z, L.COBBLED_WALL)
    h.set(x, y + 1, z, L.COBBLED_WALL)
    h.set(x, y + 2, z, L.LANTERN, {"hanging": "false"})
    h.set(x, y + 3, z, L.COBBLED_SLAB, {"type": "bottom"})
    return True


def torii(h, x, z, axis, span=2, y0=GROUND, height=6, over=False):
    """A red gate centred on (x, z): two posts of the palette's red block (`chiseled`) `span` cells either side, a tie beam two rows under the top, a top beam of
    dark wood one cell longer on each side with a slab on it and a second slab on each tip (the upturned ends). `axis` is the line the posts stand on: 'x' puts
    the posts at x - span and x + span, so the gate is walked through along z; 'z' the other way round. With `over` the gate may stand in solid rock (a gate cut
    into a cliff). Returns the cells of the opening (the row between the posts)."""
    if axis == "x":
        posts = [(x - span, z), (x + span, z)]
        line = [(x + k, z) for k in range(-span - 1, span + 2)]
    else:
        posts = [(x, z - span), (x, z + span)]
        line = [(x, z + k) for k in range(-span - 1, span + 2)]
    opening = line[2:-2]
    for (px, pz) in posts:
        for y in range(y0, y0 + height):
            h.set(px, y, pz, L.CHISELED, over=over)
    for (tx, tz) in opening:
        h.set(tx, y0 + height - 2, tz, L.CHISELED, over=over)
    for (bx, bz) in line:
        h.set(bx, y0 + height, bz, L.PLANKS, over=over)
        h.set(bx, y0 + height + 1, bz, L.PLANKS_SLAB, {"type": "bottom"}, over=over)
    for (bx, bz) in (line[0], line[-1]):
        h.set(bx, y0 + height + 2, bz, L.PLANKS_SLAB, {"type": "bottom"}, over=over)
    return opening


def bamboo(h, x, z, height=7, y0=GROUND):
    """A stalk of bamboo on the soil block that must already be under it at row y0 - 1: plain stalks with a tuft of leaves on the top three cells."""
    for k in range(height):
        leaves = "none" if k < height - 3 else ("small" if k < height - 1 else "large")
        h.try_set(x, y0 + k, z, "minecraft:bamboo", {"age": "1" if k >= height - 3 else "0", "leaves": leaves, "stage": "0"})


def dummy(h, x, z, y=GROUND):
    """A wooden training dummy: a post of two logs, a bale of hay for the head, a fence for the arms."""
    for k in range(2):
        h.set(x, y + k, z, L.LOG, {"axis": "y"})
    h.set(x, y + 2, z, "minecraft:hay_block", {"axis": "y"})
    h.try_set(x - 1, y + 1, z, L.FENCE)
    h.try_set(x + 1, y + 1, z, L.FENCE)


@room("dojo")
def dojo():
    h = Hall("mega/hall/mountain_monastery_dojo", 3, 3, 14, wear=0.05)
    for side in ("s", "w", "e"):
        h.door(side, 13)
    top = h.top                                              # 22
    c = 13
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the floor: boards, a mat of woven bamboo ringed in red lacquer, a red circle in the middle
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            dd = max(abs(x - c), abs(z - c))
            d = math.hypot(x - c, z - c)
            if dd == 7:
                h.floor_set(x, z, L.CHISELED)
            elif dd < 7:
                h.floor_set(x, z, L.CHISELED if 2.6 <= d < 3.6 else (L.POLISHED if d < 1.2 else L.BRICK))
    # ---- the shrine shelf in the north wall: a raised stage with a low altar, a bell on a frame beside it, a banner behind
    D.dais(h, 8, 2, 18, 5, rows=1, top=L.BRICK, step=L.BRICK_STAIRS, corner=L.CHISELED, y=GROUND)
    for x in (11, 12, 13, 14, 15):
        h.set(x, GROUND + 1, 3, L.BRICK)
    for x in (11, 15):
        D.candles_at(h, x, GROUND + 2, 3, 3, "white")
    h.set(13, GROUND + 2, 3, "minecraft:lectern", {"facing": "south", "has_book": "true", "powered": "false"})
    for x in (9, 17):                                        # the frame of the bell: two posts, a beam across
        for y in range(GROUND + 1, GROUND + 6):
            h.set(x, y, 3, L.LOG, {"axis": "y"})
    for x in range(9, 18):
        h.set(x, GROUND + 6, 3, L.CHISELED if x in (9, 17) else L.PLANKS)
    h.set(13, GROUND + 5, 3, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    for x in (11, 15):
        w = wn
        if w.can(x, GROUND + 3, 0) and w.backed(x, GROUND + 3, 0):
            w.set(x, GROUND + 3, 0, "minecraft:white_wall_banner", {"facing": w.into})
    # ---- four dummies at the corners of the mat, a weapon rack of staves on each side wall
    for (x, z) in ((9, 9), (17, 9), (9, 17), (17, 17)):
        dummy(h, x, z)
    for side_w, x in ((ww, 3), (we, 23)):
        for z in (7, 19):
            for y in (GROUND + 1, GROUND + 2):
                for k in range(-1, 2):
                    h.try_set(x, y, z + k, L.FENCE)
            for k in range(-1, 2):
                h.try_set(x, GROUND, z + k, L.LOG, {"axis": "z"})
    # ---- paper lanterns, shoji, beams
    D.beams(h, "x", [6, 13, 20], y=top, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS)
    D.beams(h, "z", [6, 20], y=top - 1, block=L.STRIPPED_LOG, corbel=None)
    for (x, z) in ((9, 13), (17, 13), (13, 9), (13, 17)):
        paper_lantern(h, x, z, top - 5, top)
    for w, lats in ((ws, (6, 20)), (ww, (6, 20)), (we, (6, 20)), (wn, (5, 21))):
        band_of_shoji(w, lats, GROUND + 3, GROUND + 9, width=5)
    h.faction("cultist", 4, 20)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("prayer_wheel_hall")
def prayer_wheel_hall():
    h = Hall("mega/hall/mountain_monastery_prayer_wheel_hall", 3, 3, 26, wear=0.05)
    for side in ("n", "s", "w", "e"):
        h.door(side, 13)
    top = h.top                                              # 34
    c = 13
    deck = GROUND + 7
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the roof: a hipped canopy of timber closing to a lantern of warm light over the wheel
    K.hip_roof(h, 2, 2, 24, 24, top - 11, top, cap=K.glass("yellow"), glow=GLOW)
    # ---- the great wheel: a stepped plinth, a drum of red lacquer with three gold hoops, a lid and a finial
    D.dais(h, 9, 9, 17, 17, rows=2, top=L.BRICK, step=L.BRICK_STAIRS, corner=L.CHISELED, y=GROUND)
    for y in range(GROUND + 2, GROUND + 11):
        for z in range(c - 4, c + 5):
            for x in range(c - 4, c + 5):
                d = math.hypot(x - c, z - c)
                if d > 3.4:
                    continue
                hoop = (y - GROUND) in (3, 6, 9) and d > 2.4
                h.set(x, y, z, "minecraft:gold_block" if hoop else L.CHISELED)
    for z in range(c - 3, c + 4):
        for x in range(c - 3, c + 4):
            d = math.hypot(x - c, z - c)
            if d <= 3.4:
                h.set(x, GROUND + 11, z, L.PLANKS if d > 1.5 else "minecraft:gold_block")
    for y in range(GROUND + 12, GROUND + 16):
        h.set(c, y, c, "minecraft:lightning_rod" if y > GROUND + 12 else "minecraft:gold_block", {"facing": "up", "powered": "false", "waterlogged": "false"} if y > GROUND + 12 else None)
    for (x, z) in ((c - 4, c), (c + 4, c), (c, c - 4), (c, c + 4)):
        D.candelabra(h, x, z, y=GROUND + 2, n=4, color="white")
    # ---- the gallery: a ring deck three wide round the well at row GROUND+7, a rail on the inner edge, posts under the inner edge, a flight up from the south-west
    ring = [(x, z) for z in range(2, 25) for x in range(2, 25) if min(x - 2, 24 - x, z - 2, 24 - z) <= 2]
    stair_cells = {(x, z) for x in (2, 3, 4) for z in range(17, 25)}
    for (x, z) in ring:
        if (x, z) not in stair_cells:
            h.set(x, deck, z, L.PLANKS if (x + z) % 3 else L.TILE)
    K.stairs_up(h, 2, 24, "north", 8, width=3, base=GROUND, fill_down=1, rail_col=2)
    for (x, z) in ring:
        if min(x - 2, 24 - x, z - 2, 24 - z) == 2 and (x, z) not in stair_cells:
            h.try_set(x, deck + 1, z, L.FENCE)
    corners = ((4, 4), (22, 4), (22, 22))
    for (x, z) in corners + ((4, 9), (22, 8), (22, 18), (8, 4), (18, 4), (8, 22), (18, 22)):
        for y in range(GROUND, deck):
            h.set(x, y, z, L.LOG, {"axis": "y"})
        if (x, z) in corners:
            h.set(x, deck + 2, z, L.LANTERN, {"hanging": "false"})     # on the rail post at the corner of the ring
    # ---- the small wheels: a rank of barrels two high along the foot of each wall, outside the doors' aprons
    for w in (wn, ws, ww, we):
        for lat in list(range(5, 11)) + list(range(16, 22)):
            for y in (GROUND, GROUND + 1):
                w.try_set(lat, y, 0, "minecraft:barrel", {"facing": w.into, "open": "false"})
    # ---- windows and lamps
    for w in (wn, ws, ww, we):
        band_of_shoji(w, [7, 19], GROUND + 9, GROUND + 14, width=5)
    for (x, z) in ((7, 13), (19, 13), (13, 7), (13, 19)):   # four giant paper lanterns on chains from the slope of the roof
        y_top = top - 11 + min(x - 2, 24 - x, z - 2, 24 - z) - 1
        paper_lantern(h, x, z, y_top - 4, y_top)
    h.faction("construct", 6, 6)
    K.auto_light(h, max_lamps=240)
    D.auto_markers(h)
    return h


@room("scroll_library")
def scroll_library():
    h = Hall("mega/hall/mountain_monastery_scroll_library", 2, 3, 16, wear=0.06)
    for (side, off) in (("s", 4), ("s", 13), ("w", 13), ("e", 13)):
        h.door(side, off)
    top = h.top                                              # 24
    deck = GROUND + 7
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the nave: a mat of woven bamboo down the middle between two strips of boards
    for z in range(3, 22):
        for x in range(6, 12):
            h.floor_set(x, z, L.BRICK if z % 6 else L.CHISELED)
    # ---- the gallery: a U of decks at row GROUND+7 (west side, east side, and a bridge over the south doors), a flight up the north wall onto the east side, a rail on the
    # inner edges, posts under them
    cells = {(x, z) for z in range(2, 25) for x in (2, 3, 4, 13, 14, 15)} | {(x, z) for z in (22, 23, 24) for x in range(5, 13)}
    for (x, z) in sorted(cells):
        h.set(x, deck, z, L.PLANKS if (x + z) % 3 else L.TILE)
    K.stairs_up(h, 5, 2, "east", 8, width=3, base=GROUND, fill_down=1, rail_col=2)
    for z in range(2, 22):
        for x in (4, 13):
            if not (x == 13 and z <= 4):                     # the flight comes out onto the deck here
                h.try_set(x, deck + 1, z, L.FENCE)
    for x in range(5, 13):
        h.try_set(x, deck + 1, 22, L.FENCE)
    for (x, z) in ((4, 9), (4, 17), (13, 9), (13, 17), (8, 22)):
        for y in range(GROUND, deck):
            h.set(x, y, z, L.LOG, {"axis": "y"})
    # ---- the racks: bookshelves on the west and east walls at both levels (the doors keep their gaps by themselves), on the south wall over the bridge
    for w in (ww, we):
        D.shelf_wall(w, 2, 24, GROUND, GROUND + 5)
        D.shelf_wall(w, 2, 24, deck + 1, deck + 3)
    D.shelf_wall(ws, 5, 12, deck + 1, deck + 4)
    # ---- the reading tables in the nave: two long low desks with lecterns and candles, a red cushion either side of each seat
    for z0 in (6, 14):
        for z in range(z0, z0 + 5):
            for x in (8, 9):
                h.set(x, GROUND, z, L.PLANKS)
        for z in (z0 + 1, z0 + 3):
            D.lectern(h.frame(8, GROUND + 1, z, "west"))
            D.candles_at(h, 9, GROUND + 1, z, 3, "white")
            for x in (7, 10):
                h.set(x, GROUND, z, "minecraft:red_carpet")
    # ---- beams across, giant paper lanterns over the desks, shoji above the shelves
    D.beams(h, "x", [6, 12, 18], y=top, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS)
    for z in (9, 15):
        paper_lantern(h, 9, z, top - 5, top)
    for w in (ww, we):
        band_of_shoji(w, [6, 12, 18], GROUND + 11, GROUND + 15, width=5)
    band_of_shoji(wn, [9], GROUND + 9, GROUND + 15, width=5)
    band_of_shoji(ws, [8], GROUND + 9, GROUND + 15, width=5)
    h.faction("cultist", 8, 12)
    K.auto_light(h, max_lamps=200)
    D.auto_markers(h)
    return h


def boulder(h, cx, cz, rng):
    """A garden rock: a plus-shaped foot two high and a third block on the middle, of stone and cobble, a streak of moss on one side."""
    for (dx, dz) in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
        for k in range(2):
            h.try_set(cx + dx, GROUND + k, cz + dz, rng.choice((L.ROCK, L.COBBLED, L.ROCK, "minecraft:mossy_cobblestone")))
    h.try_set(cx, GROUND + 2, cz, L.ROCK)


@room("zen_garden")
def zen_garden():
    h = Hall("mega/hall/mountain_monastery_zen_garden", 3, 3, 14, wear=0.03)
    for side in ("s", "w", "e"):
        h.door(side, 13)
    top = h.top                                              # 22
    rng = h.rng
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    rocks = [(8, 8), (15, 8)]                                # the strip north of the walkway (z 7..9) holds the rocks, the lanterns the walls and the bamboo the corner: the walkways round the pond stay clear
    pond_c, pond_rx, pond_rz = (12, 16), 3.6, 2.9
    pond = {(x, z) for z in range(6, 25) for x in range(2, 25) if ((x - pond_c[0]) / pond_rx) ** 2 + ((z - pond_c[1]) / pond_rz) ** 2 <= 1.0}
    rim = {(x + dx, z + dz) for (x, z) in pond for dx in (-1, 0, 1) for dz in (-1, 0, 1)} - pond
    # ---- the floor of the whole courtyard is raked white gravel (calcite and polished diorite: sand and gravel would fall into the void under the floor, and plain diorite is outside the palette): stripes along the hall, rings of ripples round each rock; a moss mound under the cherry tree, a podzol bed for the bamboo
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            near = min(min(math.hypot(x - rx, z - rz) for (rx, rz) in rocks), math.hypot(x - pond_c[0], z - pond_c[1]) - 3.0)
            ripple = int(near) % 2 == 0 if near < 5.0 else z % 2 == 0
            blk = "minecraft:calcite" if ripple else "minecraft:polished_diorite"
            if math.hypot(x - 20, z - 9) <= 2.7:
                blk = "minecraft:moss_block"
            elif x <= 5 and 7 <= z <= 9:
                blk = "minecraft:podzol"
            h.floor_set(x, z, blk)
    # ---- the pond: water one deep in a rim of rock, lily pads, a red moon bridge across it on the line z = 16 with a stair at each end
    for (x, z) in sorted(rim):
        h.set(x, GROUND, z, rng.choice((L.ROCK, L.COBBLED, "minecraft:mossy_cobblestone")))
    for (x, z) in sorted(pond):
        h.set(x, GROUND, z, "minecraft:water", {"level": "0"})
    row_g = pond_c[1]
    for z in (row_g - 1, row_g, row_g + 1):                   # the deck: three wide, a red block at each end, rails on the outer lanes
        for x in range(9, 16):
            h.set(x, GROUND + 1, z, L.CHISELED if x in (9, 15) else L.PLANKS)
    for x in range(10, 15):
        h.set(x, GROUND + 2, row_g - 1, L.FENCE)
        h.set(x, GROUND + 2, row_g + 1, L.FENCE)
    for x_stair, row, face in ((8, GROUND + 1, "east"), (7, GROUND, "east"), (16, GROUND + 1, "west"), (17, GROUND, "west")):
        h.set(x_stair, row, row_g, L.PLANKS_STAIRS, {"facing": face, "half": "bottom"})
    for x in (10, 14):                                       # pilings in the water under the deck
        h.set(x, GROUND, row_g, L.LOG, {"axis": "y"}, over=True)
    for (x, z) in sorted(pond):
        if rng.random() < 0.25 and h.get(x, GROUND, z).block == "minecraft:water":
            h.try_set(x, GROUND + 1, z, "minecraft:lily_pad")
    # ---- rocks, stepping stones from the south door, stone lanterns
    for (rx, rz) in rocks:
        boulder(h, rx, rz, rng)
    for z in (20, 22, 24):
        h.floor_set(13, z, L.COBBLED)
    for (x, z) in ((2, 16), (2, 19), (24, 16), (24, 19), (5, 24), (21, 24), (11, 9)):
        stone_lantern(h, x, z)
    # ---- the cherry tree on its mound, petals on the moss, bamboo in the corner
    K.tree(h, 20, 9, trunk=5, radius=3, log="minecraft:cherry_log", leaves="minecraft:cherry_leaves")
    for (x, z) in ((18, 10), (22, 8), (19, 12), (22, 11)):
        h.try_set(x, GROUND, z, "minecraft:pink_petals", {"facing": rng.choice(("north", "south", "east", "west")), "flower_amount": str(rng.randint(1, 4))})
    for z in range(7, 10):
        for x in range(2, 6):
            if (x * 3 + z * 5) % 5 < 3:
                bamboo(h, x, z, height=5 + (x + z) % 5)
    # ---- the veranda along the north wall: a platform one high with a step along its edge, posts, a flat roof, lanterns hung under it
    for z in range(2, 6):
        for x in range(2, 25):
            h.set(x, GROUND, z, L.PLANKS if (x + z) % 4 else L.TILE)
    for x in range(2, 25):
        h.set(x, GROUND, 6, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})
    for x in range(4, 25, 4):
        for y in range(GROUND + 1, GROUND + 6):
            h.set(x, y, 5, L.LOG, {"axis": "y"})
    for x in range(2, 25):
        for z in range(2, 6):
            h.set(x, GROUND + 6, z, L.PLANKS if z < 5 else L.STRIPPED_LOG, None if z < 5 else {"axis": "x"})
    for x in range(6, 25, 4):
        h.set(x, GROUND + 5, 4, L.LANTERN, {"hanging": "true"})
    # ---- the sky: a lid of white glass over the courtyard, a warm glow above it; shoji on every wall
    for z in range(8, 23):
        for x in range(4, 23):
            rib = x % 6 == 4 or z % 6 == 2
            h.shell_set(x, top + 1, z, L.LOG if rib else K.glass("white"), {"axis": "x"} if rib else None)
            h.shell_set(x, top + 2, z, L.LOG if rib else GLOW, {"axis": "x"} if rib else None)
    band_of_shoji(wn, [5, 11, 17, 22], GROUND + 2, GROUND + 5, width=5)
    for w in (ws, ww, we):
        band_of_shoji(w, [5, 21], GROUND + 2, GROUND + 8, width=5)
    h.faction("beast", 4, 20)
    K.auto_light(h)
    D.auto_markers(h)
    return h


def rock(h, rng, mossy=0.12):
    """One block of cliff: mostly stone and cobble, some tuff, now and then mossy cobble."""
    r = rng.random()
    if r < mossy:
        return "minecraft:mossy_cobblestone"
    return rng.choice((L.ROCK, L.ROCK, L.COBBLED, "minecraft:tuff"))


def mass(h, rng, x1, z1, x2, z2, y1, y2, bite=0.0, keep=()):
    """A block of cliff over [x1..x2] x [z1..z2] from row y1 to y2. `bite` is the chance that a boundary cell of its sides is left out (a ragged face); cells in
    `keep` are never left out."""
    for y in range(y1, y2 + 1):
        for z in range(z1, z2 + 1):
            for x in range(x1, x2 + 1):
                edge = x in (x1, x2) or z in (z1, z2)
                if edge and bite and (x, z) not in keep and rng.random() < bite and y > y1:
                    continue
                h.set(x, y, z, rock(h, rng))


@room("cliff_stairs")
def cliff_stairs():
    h = Hall("mega/set/mountain_monastery_cliff_stairs", 3, 3, 34, wear=0.04)
    for (side, off) in (("s", 22), ("w", 13), ("e", 13), ("n", 4)):
        h.door(side, off)
    top = h.top                                              # 42
    rng = h.rng
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    g = GROUND
    # ---- the floor: bare rock round the foot of the mountain, patches of moss
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            r = rng.random()
            h.floor_set(x, z, "minecraft:moss_block" if r < 0.12 else ("minecraft:tuff" if r < 0.4 else L.ROCK))
    # ---- the mountain: three terraces, each narrower than the one below
    mass(h, rng, 7, 4, 19, 16, g, g + 8, bite=0.25, keep={(x, z) for x in range(7, 20) for z in (16,)} | {(19, z) for z in range(4, 17)})
    mass(h, rng, 9, 6, 17, 14, g + 9, g + 18, bite=0.25, keep={(17, z) for z in range(6, 15)})
    mass(h, rng, 11, 8, 15, 12, g + 19, g + 25, bite=0.15, keep={(x, 8) for x in range(11, 16)} | {(11, z) for z in range(8, 13)})
    for (x, z) in sorted({(x, z) for x in range(7, 20) for z in range(4, 17)}):
        if h.get(x, g + 8, z) is not None and h.free(x, g + 9, z) and rng.random() < 0.35:
            h.set(x, g + 8, z, "minecraft:moss_block", over=True)
    # ---- the stair: three flights, each hugging the face of the terrace it climbs, a landing at each turn
    steps = dict(stairs=L.COBBLED_STAIRS, under=L.COBBLED, fill_down=1, rail_col=2)           # the steps are cut from the same stone as the mountain
    K.stairs_up(h, 9, 17, "east", 9, width=3, base=g, **steps)                                 # along the south face, west to east
    K.stairs_up(h, 18, 16, "north", 10, width=3, base=g + 9, **steps)                          # up the east face, south to north
    K.stairs_up(h, 17, 7, "west", 7, width=3, base=g + 19, **steps)                            # along the north face of the top terrace, east to west
    for (x0, z0, row) in ((18, 17, g + 8), (18, 4, g + 18), (8, 5, g + 25)):
        for z in range(z0, z0 + 3):
            for x in range(x0, x0 + 3):
                h.set(x, row, z, L.ROCK if (x + z) % 2 else L.COBBLED)
                h.set(x, row - 1, z, L.COBBLED)
    for z in range(8, 13):                                   # the bridge from the last landing to the summit: a strip along the west face of the top terrace
        for x in (8, 9, 10):
            h.set(x, g + 25, z, L.ROCK if (x + z) % 2 else L.COBBLED)
            h.set(x, g + 24, z, L.COBBLED)
    # ---- red gates: at the foot of the first flight, at the turn to the second, at the summit
    torii(h, 8, 18, "z", span=2, y0=g, height=6, over=True)
    torii(h, 19, 16, "x", span=2, y0=g + 9, height=6)
    for y in range(g, g + 9):
        h.set(21, y, 16, L.COBBLED)
    torii(h, 9, 10, "z", span=2, y0=g + 26, height=5)
    # ---- the summit: a small shrine on the top terrace, four red posts, a hipped roof, an altar with the offering box, stone lanterns
    for (x, z) in ((11, 8), (15, 8), (11, 12), (15, 12)):
        for y in range(g + 26, g + 30):
            h.set(x, y, z, L.CHISELED)
    K.hip_roof(h, 11, 8, 15, 12, g + 30, g + 32, stairs=L.TILE_STAIRS, fill=L.PLANKS)
    h.set(13, g + 26, 10, L.CHISELED)
    h.loot_chest(13, g + 27, 10, "west")
    D.candles_at(h, 12, g + 26, 10, 3, "white")
    D.candles_at(h, 14, g + 26, 10, 3, "white")
    # ---- lights: four giant paper lanterns on long chains in the corners of the hall, a lamp on the post of every landing
    for (x, z) in ((4, 4), (22, 4), (4, 22), (22, 22)):
        paper_lantern(h, x, z, g + 16, top)
    for (x, z, y0) in ((20, 19, g + 9), (20, 4, g + 19), (8, 5, g + 26)):
        K.lamp_post(h, x, z, height=2, y0=y0)
    stone_lantern(h, 7, 13, y=g + 9)                         # on the ledge round the first terrace, where the flights do not come
    stone_lantern(h, 12, 4, y=g + 9)
    h.set(19, g + 9, 12, L.COBBLED_WALL)                     # a low one under the second flight, which leaves only three rows of air there
    h.set(19, g + 10, 12, L.LANTERN, {"hanging": "false"})
    # ---- the sky over it all: a lid of white glass with a warm glow above, shoji on the low walls
    for z in range(3, 24):
        for x in range(3, 24):
            rib = x % 5 == 3 or z % 5 == 3
            h.shell_set(x, top + 1, z, L.LOG if rib else K.glass("white"), {"axis": "x"} if rib else None)
            h.shell_set(x, top + 2, z, L.LOG if rib else GLOW, {"axis": "x"} if rib else None)
    for w in (wn, ws, ww, we):
        band_of_shoji(w, [6, 20], g + 3, g + 12, width=5)
    h.faction("beast", 4, 20)
    K.auto_light(h, max_lamps=240)
    D.auto_markers(h)
    return h


def hot_pool(h, rng, x1, z1, x2, z2):
    """A hot pool: a rim of rock one block high round water one block deep over the floor (the rim and the floor shut the water in on every side), a bucket on the rim.
    The rectangle [x1..x2] x [z1..z2] is the rim; the cells inside it are the water."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            if x in (x1, x2) or z in (z1, z2):
                h.set(x, GROUND, z, rng.choice((L.ROCK, L.COBBLED, L.ROCK, "minecraft:mossy_cobblestone")))
            else:
                h.set(x, GROUND, z, "minecraft:water", {"level": "0"})


@room("hot_springs")
def hot_springs():
    h = Hall("mega/safe/mountain_monastery_hot_springs", 3, 2, 12, wear=0.02)
    h.door("n", 13)
    h.door("s", 13)
    h.safe = True
    K.keep_doors_clear(h)
    top = h.top                                              # 20
    rng = h.rng
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the roof: a hipped canopy of timber closing to a ridge of warm light over the walkway between the pools
    K.hip_roof(h, 2, 2, 24, 15, top - 7, top, cap=K.glass("white"), glow=GLOW)
    # ---- the floor: flags of stone round the pools, boards on the walkway between them
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            if 11 <= x <= 15:
                h.floor_set(x, z, L.TILE)
            elif z >= 5:
                h.floor_set(x, z, L.COBBLED if (x + z) % 3 else L.ROCK)
    # ---- the two pools, a bench on the walkway side of each (a rest anchor for every seat)
    hot_pool(h, rng, 3, 5, 10, 12)
    hot_pool(h, rng, 16, 5, 23, 12)
    for z in (7, 9, 11):
        h.set(11, GROUND, z, L.COBBLED_STAIRS, {"facing": "east", "half": "bottom"})
        h.marker(11, GROUND + 1, z, "seat", facing="west", **{"as": "chair"})
        h.set(15, GROUND, z, L.COBBLED_STAIRS, {"facing": "west", "half": "bottom"})
        h.marker(15, GROUND + 1, z, "seat", facing="east", **{"as": "chair"})
    for (x, z) in ((3, 13), (23, 13), (10, 13), (16, 13)):
        h.set(x, GROUND, z, "minecraft:water_cauldron", {"level": "3"})
    # ---- a screen of bamboo between the pools and the sleeping place, the stove in the north-west corner with a bench in front, three futons in the north-east
    for x in list(range(3, 11)) + list(range(16, 24)):
        h.set(x, GROUND, 4, "minecraft:bamboo_fence")
    for x in range(3, 9):
        for z in (2, 3):
            if (x, z) != (5, 3):
                h.set(x, GROUND, z, L.COBBLED)
    h.set(5, GROUND, 3, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false"})
    h.marker(5, GROUND + 1, 3, "hearth", **{"as": "stove"})
    h.set(5, GROUND, 4, L.COBBLED_STAIRS, {"facing": "north", "half": "bottom"}, over=True)
    h.marker(5, GROUND + 1, 4, "seat", facing="north", **{"as": "chair"})
    for x in (17, 19, 21):
        D.bed(h.frame(x, GROUND, 2, "south"), color="white", marker=True)
    for x in (18, 20):
        for z in (2, 3):
            h.set(x, GROUND, z, "minecraft:bamboo_fence")
    # ---- paper lanterns over the pools on chains from the slope of the roof, shoji on the long walls
    for (x, z) in ((6, 9), (20, 9)):
        y_top = top - 7 + min(x - 2, 24 - x, z - 2, 15 - z) - 1
        paper_lantern(h, x, z, y_top - 4, y_top)
    for w, lats in ((ww, (5, 11)), (we, (5, 11))):
        band_of_shoji(w, lats, GROUND + 2, GROUND + 7, width=5)
    for w, lats in ((wn, (7, 19)), (ws, (6, 20))):
        band_of_shoji(w, lats, GROUND + 2, GROUND + 7, width=5)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    K.auto_light(h, threshold=8)
    return h


@room("duel_ring")
def duel_ring():
    h = Hall("mega/trial/mountain_monastery_duel_ring", 3, 3, 14, wear=0.03)
    h.door("n", 13)
    h.door("s", 13)
    h.no_spawn = True
    top = h.top                                              # 22
    c = 13
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the ring: a low platform of woven bamboo ringed in red lacquer, a red circle in the middle, a rope on posts round it with a gap on each side
    D.dais(h, 7, 7, 19, 19, rows=1, top=L.BRICK, step=L.BRICK_STAIRS, corner=L.CHISELED, y=GROUND)
    for z in range(8, 19):
        for x in range(8, 19):
            d = math.hypot(x - c, z - c)
            if 3.0 <= d < 4.4 or d < 1.2:
                h.set(x, GROUND, z, L.CHISELED, over=True)
    for (x, z) in ((7, 7), (19, 7), (7, 19), (19, 19)):
        h.set(x, GROUND + 1, z, L.FENCE)
        h.set(x, GROUND + 2, z, L.FENCE)
    for k in range(8, 19):
        if 12 <= k <= 14:
            continue
        for (x, z, axis) in ((k, 7, "x"), (k, 19, "x"), (7, k, "z"), (19, k, "z")):
            h.set(x, GROUND + 2, z, L.CHAIN_BLOCK, {"axis": axis})
    glyphs = [(3, 3), (23, 3), (3, 23), (23, 23), (3, 13), (23, 13), (8, 3), (18, 23)]
    K.trial_core(h, [("minecraft:iron_golem", 1), ("nusmp:living_armor", 2)], glyphs, radius=13)
    # ---- a red gate before each door, benches for the watchers along the side walls, racks of staves, a bell by the south door
    torii(h, c, 6, "x", span=2, y0=GROUND, height=6)
    torii(h, c, 20, "x", span=2, y0=GROUND, height=6)
    K.bench(h, 3, 8, "east", length=11)
    K.bench(h, 23, 18, "west", length=11)                    # facing west the row runs north, so it starts at the south end
    for w, lat in ((ww, 5), (ww, 19), (we, 5), (we, 19)):
        D.spear_rack(w, lat, width=3)
    for x in (4, 6):
        for y in range(GROUND, GROUND + 4):
            h.try_set(x, y, 23, L.LOG, {"axis": "y"})
    for x in range(4, 7):
        h.try_set(x, GROUND + 4, 23, L.PLANKS)
    h.try_set(5, GROUND + 3, 23, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    # ---- the prize: a chest in a recess of the west wall under a hanging scroll (a banner), candles on the sill
    D.niche(ww, 13, GROUND, GROUND + 4, width=3)
    x, y, z = ww.cell(13, GROUND, -1)
    h.loot_chest(x, y, z, ww.into)
    for dl in (-1, 1):
        D.candles_at(h, *ww.cell(13 + dl, GROUND, -1), 3, "white")
    # ---- paper lanterns, shoji, beams
    D.beams(h, "x", [4, 13, 22], y=top, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS)
    for (x, z) in ((5, 10), (21, 10), (5, 16), (21, 16)):
        paper_lantern(h, x, z, top - 5, top)
    for w, lats in ((ww, (6, 20)), (we, (6, 13, 20)), (wn, (5, 21)), (ws, (5, 21))):
        band_of_shoji(w, lats, GROUND + 3, GROUND + 9, width=5)
    h.faction("construct", 4, 20)
    K.auto_light(h)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    return h


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, sys.argv[1:], __doc__.split("\n\n")[0]))

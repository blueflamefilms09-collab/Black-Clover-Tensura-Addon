#!/usr/bin/env python3
"""
THEME-C, theme 3 of 4 (1.59.0, docs/labyrinth/mega_v2_thc.md): the signature rooms of the VAMPIRE CASTLE, the evil road's Ravenloft. Every room is named
`vampire_castle_<room>.nbt` so that `data/nusmp/mega/template_themes/vampire_castle.json` can reserve it for sectors of that theme.

    python3 tools/labyrinth/gen_mega_theme_vampire_castle.py                      # (re)writes every template
    python3 tools/labyrinth/gen_mega_theme_vampire_castle.py vampire_castle_great_hall   # only this one
    python3 tools/labyrinth/gen_mega_theme_vampire_castle.py --check              # regenerates in memory, compares with the files on disk, runs every check
    python3 tools/labyrinth/gen_mega_theme_vampire_castle.py --list

Why a generator and not hand-placed blocks: see `gen_mega_theme_cursed_archive.py`; the rules are the same (connector contract, palette constants so the
Region Theme restyles them into grey andesite, blackstone and red nether brick, the kit of `mega_decor_r1.py` and `mega_thc_kit.py`, every template checked
before it is written). Unlike the three other themes of this builder the lights of this one stay WARM: the theme turns the soul lights of a template into
ordinary lanterns and torches, so the rooms are written with ordinary lanterns, candles and braziers and checked with them.
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
CALM = ("vampire_castle_mirror_gallery", "vampire_castle_blood_court")        # a treasure gallery and a trial hall carry no spawn markers of their own
SOUL = False
RED_GLASS = ("minecraft:red_stained_glass", "minecraft:red_stained_glass", "minecraft:purple_stained_glass", "minecraft:red_stained_glass", "minecraft:black_stained_glass")
LOOT = "nusmp:chests/labyrinth_trial"


def opposite(facing):
    return {"north": "south", "south": "north", "east": "west", "west": "east"}[facing]


# ----------------------------------------------------------------------------------------------------------------------------------- the great hall
GH_COLUMN_Z = (8, 18, 26, 36)                    # the arcade: a column at x = 7 and x = 19 at each of these
GH_COLUMN_X = (7, 19)
GH_DECK = GROUND + 7                             # the row of the gallery deck blocks; you stand on the row above


def great_hall_gallery(h, west):
    """One gallery of the Great Hall (the east one is the west one turned half a circle about the room's centre): a deck over the aisle along the long wall, a
    flight of eight steps up to it, a parapet of wall blocks on the nave side with a lantern post every fourth cell, and a pier of polished stone under the
    parapet every sixth. `west` selects which of the two it builds."""
    def P(x, z):
        return (x, z) if west else (26 - x, 44 - z)

    def face(f):
        return f if west else opposite(f)
    z_lo, z_hi = (17, 42) if west else (2, 27)                  # the deck, in the coordinates of the room
    x_in, x_out = (5, 2) if west else (21, 24)                  # the nave edge of the deck and the wall edge
    x_a, x_b = sorted((x_in, x_out))
    for z in range(z_lo, z_hi + 1):
        for x in range(x_a, x_b + 1):
            h.try_set(x, GH_DECK, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"})
    for z in range(z_lo, z_hi + 1):                              # the parapet on the nave edge
        h.try_set(x_in, GH_DECK + 1, z, L.POLISHED_WALL)
    for z in range(z_lo + 1, z_hi + 1, 3):                       # a lantern on every third post, so the whole length of the gallery is lit and no lamp has to hang over it
        h.try_set(x_in, GH_DECK + 2, z, L.LANTERN, {"hanging": False})
    # the flight: eight steps, the last one level with the deck, a rail on the nave side
    if west:
        D.stair_flight(h, 4, 9, "south", 8, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=0)
    else:
        D.stair_flight(h, 22, 35, "north", 8, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=0)
    # the fascia: a course of polished stone under the nave edge of the deck with a stair underneath it turned to the aisle (above head height: no pier stands in
    # the aisle, whose three free cells are the lane the doors are joined by)
    for z in range(z_lo, z_hi + 1):
        h.try_set(x_in, GH_DECK - 1, z, L.POLISHED)


@room("vampire_castle_great_hall", "hall")
def great_hall():
    """The Great Hall: the biggest room of the castle, three storeys of it. A nave nine wide runs the length of the hall between two arcades of columns that carry
    pointed ribs thirty blocks over the floor; behind each arcade a gallery runs over the aisle (a flight of eight steps up to it, a parapet, lanterns), and above
    the galleries tall windows of red glass glow in every bay. Two long tables of dark oak, candles and cake, chairs of stairs; at the north end a dais with the
    high table and a throne under red drapes; braziers, chandeliers on long chains and banners of red cloth. The one place of the castle that is lit like a feast."""
    h = Hall("mega/hall/vampire_castle_great_hall", 3, 5, 26, wear=0.10)
    top = h.top
    doors = (("n", 22), ("s", 4), ("w", 22), ("e", 22))
    for side, off in doors:
        h.door(side, off)
    h.route(12, 9, 14, 42, label="nave lane")
    for side, off in doors:
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # 1. the arcade: columns carry the ribs, the ribs the vault
    y_spring = GROUND + 13
    for x in GH_COLUMN_X:
        for z in GH_COLUMN_Z:
            D.column(h, x, z, 1, y0=GROUND, y1=y_spring)
    for z in GH_COLUMN_Z:
        K.arch_rib(h, "x", z, 8, 18, y_spring + 1, top - (y_spring + 1) - 1)
    # 2. the pilasters of the long walls first (a deck is laid round whatever stands), then the galleries
    for z in GH_COLUMN_Z:                                         # the pilasters stand where the columns do, either side of the flights' gaps
        D.pilaster(ww, z - 1, 2, y0=GROUND, y1=top - 1)
        D.pilaster(we, z, 2, y0=GROUND, y1=top - 1)
    for z in GH_COLUMN_Z:                                         # a lantern on a bracket high on every pilaster: the vault is lit from its springing line too
        for drop in (4, 10):
            D.bracket_lantern(ww, z, top - drop, arm=1, soul=SOUL, d0=1)
            D.bracket_lantern(we, z + 1, top - drop, arm=1, soul=SOUL, d0=1)
    great_hall_gallery(h, True)
    great_hall_gallery(h, False)
    # 3. the long walls: pilasters at the arcade bays, tall red windows between them
    windows = 0
    for wl in (ww, we):
        for z in (13, 22, 31):
            if K.gothic_window(wl, z, GROUND + 10, GROUND + 22, RED_GLASS, width=3):
                windows += 1
    for lat in (10, 13, 16):                                      # the great windows of the two ends
        K.gothic_window(wn, lat, GROUND + 6, GROUND + 21, RED_GLASS, width=3)
        K.gothic_window(ws, lat, GROUND + 6, GROUND + 21, RED_GLASS, width=3)
    # 4. the dais: three steps up at the north end, the high table, the throne, braziers
    D.dais(h, 9, 2, 17, 7, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    D.table(h.frame(9, GROUND + 1, 5, "east"), 9)
    for x in range(9, 18):
        if not 12 <= x <= 14:                                    # the throne takes the middle three
            D.chair(h.frame(x, GROUND + 1, 4, "south"))
    for x in (10, 16):
        D.candles_at(h, x, GROUND + 2, 5, 3, "red")
    h.set(13, GROUND + 1, 4, L.POLISHED)                         # the throne: a block seat between two arms, a tall back of chiselled stone under a banner
    h.set(12, GROUND + 1, 4, L.POLISHED_STAIRS, {"facing": "east", "half": "bottom"})
    h.set(14, GROUND + 1, 4, L.POLISHED_STAIRS, {"facing": "west", "half": "bottom"})
    for y in range(GROUND + 1, GROUND + 6):
        h.try_set(13, y, 3, L.CHISELED if y == GROUND + 5 else L.POLISHED)
    for x in (9, 17):
        D.brazier(h, x, 3, y=GROUND + 1)
    # 5. the feast: two long tables down the nave with a chair on each side, candles and a cake or two
    for x0, chair_in, chair_out in ((10, 9, 11), (16, 15, 17)):
        for (z0, length) in ((11, 11), (25, 11)):
            D.table(h.frame(x0, GROUND, z0, "south"), length)
            for z in range(z0 + 1, z0 + length - 1, 2):
                D.chair(h.frame(chair_in, GROUND, z, "east"))
                D.chair(h.frame(chair_out, GROUND, z, "west"))
            for k in (2, 5, 8):
                D.candles_at(h, x0, GROUND + 1, z0 + k, 3, "red" if k == 5 else "white")
            h.try_set(x0, GROUND + 1, z0 + 3, "minecraft:cake", {"bites": "2"})
            h.try_set(x0, GROUND + 1, z0 + 7, "minecraft:cake", {"bites": "0"})
        h.marker(x0, GROUND + 1, 15, "furniture", **{"as": "table"})
    # 6. the lights of a feast: braziers down the nave, chandeliers on long chains, lanterns on the arcade columns
    for (x, z) in ((8, 14), (18, 14), (8, 30), (18, 30), (8, 40), (18, 40)):
        D.brazier(h, x, z)
    for z in range(19, 43, 5):                                   # the aisles under the galleries: a lantern on a chain from the underside of the deck every fifth cell
        K.hung_lantern_under(h, 4, z, GROUND, GROUND + 3, soul=SOUL)
        K.hung_lantern_under(h, 22, 44 - z, GROUND, GROUND + 3, soul=SOUL)
    for z in (14, 22, 30):                                       # the chandeliers hang low (a ring seven rows over the floor) so that they light the feast, not the vault
        D.chandelier_wheel(h, 13, z, r=3, drop=top - (GROUND + 7), soul=SOUL)
    D.upper_dress(h, [D.it_tall_banner("red", hang=8), D.it_high_bracket(soul=SOUL, drop=3)], sides="nsew", step=(3, 5), door_gap=2)
    D.dress_wall(h, "w", [D.it_wall_torch(y=GROUND + 3)], step=(3, 4), door_gap=2)
    D.dress_wall(h, "e", [D.it_wall_torch(y=GROUND + 3)], step=(3, 4), door_gap=2)
    D.cornice(h)
    D.cobwebs(h, 0.5)
    h.faction("undead", 13, 20)
    K.auto_light(h, soul=SOUL, max_lamps=60, prune=True)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the chapel
def rose_window(w, lat, yc, r, colours, glow="minecraft:shroomlight"):
    """A round window of radius `r` centred on lateral `lat` and row `yc`: stained glass in the inner wall layer, coloured by ring (`colours`, from the middle out),
    a lit block behind every pane and spokes of polished stone between them. Returns False without touching anything when a cell of it is not plain wall."""
    h = w.hall
    cells = []
    for dl in range(-r, r + 1):
        for dy in range(-r, r + 1):
            d2 = dl * dl + dy * dy
            if d2 <= r * r + r // 2:
                cells.append((lat + dl, yc + dy, dl, dy, d2))
    for (lt, y, _dl, _dy, _d2) in cells:
        inner, outer = w.cell(lt, y, -1), w.cell(lt, y, -2)
        ci, co = h.get(*inner), h.get(*outer)
        if ci is None or co is None or inner not in h.shell or outer not in h.shell or inner in h.mt.reserved or ci.block not in D.WALL_BLOCKS or not w.free(lt, y, 0):
            return False
    for (lt, y, dl, dy, d2) in cells:
        w.carve(lt, y, -1)
        spoke = dl == 0 or dy == 0 or abs(dl) == abs(dy)
        if spoke and d2 > 1:
            w.set(lt, y, -1, L.POLISHED)
        else:
            ring = min(len(colours) - 1, int(math.sqrt(d2) * len(colours) / (r + 1)))
            w.set(lt, y, -1, colours[ring])
        w.shell_set(lt, y, -2, glow)
    return True


def break_panes(h, w, lats, y0, y1, p):
    """Takes the glass out of some cells of the windows at `lats` (rows y0..y1): the lit block behind shows through the gap. Returns how many panes went. Why: the
    brief's desecrated chapel has shattered windows; a missing pane is one cell fewer and costs nothing to the checks (the lamp behind it is part of the wall)."""
    n = 0
    for lat in lats:
        for dl in (-1, 0, 1):
            for y in range(y0, y1 + 1):
                pos = w.cell(lat + dl, y, -1)
                c = h.get(*pos)
                if c is not None and c.block.endswith("stained_glass") and h.rng.random() < p:
                    h.mt.remove(*pos)
                    h.deco.pop(pos, None)
                    n += 1
    return n


@room("vampire_castle_chapel", "set")
def chapel():
    """The Desecrated Chapel: a nave twenty-three wide, pews in two blocks with a red runner between them, broken and overturned, the windows shattered
    here and there so that the lamp behind them shows, a round window of red and black glass over an altar on a dais with a cross hung upside down in the
    red stone of the castle, a stained-glass bleeding of red carpet down the steps, caged skulls hanging from the roof on chains, and over the door a choir loft
    with an organ of pipes. The place where the court once prayed, and no longer does."""
    h = Hall("mega/set/vampire_castle_chapel", 3, 4, 22, wear=0.12)
    top = h.top
    doors = (("s", 13), ("w", 13), ("e", 22))
    for side, off in doors:
        h.door(side, off)
    h.route(12, 8, 14, 33, label="nave lane")
    for side, off in doors:
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # 1. pilasters and transverse ribs: a rib springs from the capital of every pilaster and crosses the whole nave
    y1 = GROUND + 13
    for z in (8, 17, 26):                                          # clear of the door frames of both side walls (z 11..15 west, 20..24 east)
        for wl in (ww, we):
            D.pilaster(wl, z, 2, y0=GROUND, y1=y1)
        K.arch_rib(h, "x", z, 2, 24, y1 + 1, top - 1 - (y1 + 1))
    for z in (8, 17, 26):                                          # a lantern on a bracket at head height on every pilaster: the side aisles are lit from their walls
        for wl in (ww, we):
            D.bracket_lantern(wl, z, GROUND + 4, arm=1, soul=SOUL, d0=1)
    # 2. windows between the pilasters, some of them shattered; the round window and the inverted cross at the apse
    lit = 0
    for wl in (ww, we):
        for z in (5, 13, 22, 31):
            if K.gothic_window(wl, z, GROUND + 6, GROUND + 13, RED_GLASS, width=3):
                lit += 1
                break_panes(h, wl, [z], GROUND + 6, GROUND + 13, 0.18)
    rose_window(wn, 13, GROUND + 15, 4, ("minecraft:red_stained_glass", "minecraft:purple_stained_glass", "minecraft:red_stained_glass", "minecraft:black_stained_glass"))
    for y in range(GROUND + 2, GROUND + 11):                       # the cross: the long arm up the wall and the short one low, so that it hangs upside down
        wn.shell_set(13, y, -1, L.CHISELED)
    for lat in (11, 12, 14, 15):
        wn.shell_set(lat, GROUND + 4, -1, L.CHISELED)
    # 3. the dais, the altar, the blood on the steps
    D.dais(h, 9, 2, 17, 7, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    for x in range(11, 16):
        for z in (3, 4):
            h.set(x, GROUND + 1, z, L.POLISHED)
    for x in (11, 15):
        D.candles_at(h, x, GROUND + 2, 3, 3, "red")
        D.candles_at(h, x, GROUND + 2, 4, 4, "red")
    h.set(13, GROUND + 2, 3, "minecraft:wither_skeleton_skull", {"rotation": "0"})
    for x in (12, 13, 14):
        h.try_set(x, GROUND + 1, 5, "minecraft:red_carpet")
        h.try_set(x, GROUND + 1, 6, "minecraft:red_carpet")
    for z in range(8, 31):                                         # the runner down the nave: red, torn in a few places
        for x in (12, 13, 14):
            if h.rng.random() > 0.08:
                h.try_set(x, GROUND, z, "minecraft:red_carpet")
    for (x, z) in ((9, 3), (17, 3)):
        D.brazier(h, x, z, y=GROUND + 1)
    # 4. pews: two blocks of six rows, a few broken or overturned
    for z in (9, 12, 15, 18, 21, 24):
        for x0 in (7, 16):
            r = h.rng.random()
            if r < 0.2:
                K.pew(h, x0, z, 2, "x", "north")
                h.try_set(x0 + 3, GROUND, z, L.PLANKS_SLAB, {"type": "bottom"})
            elif r < 0.3:
                h.try_set(x0 + 1, GROUND, z, L.PLANKS_STAIRS, {"facing": "east", "half": "bottom"})
                h.try_set(x0 + 2, GROUND, z, L.PLANKS_STAIRS, {"facing": "west", "half": "top"})
            else:
                K.pew(h, x0, z, 4, "x", "north")
    for z in (10, 16, 22):
        for x in (11, 15):
            D.candelabra(h, x, z, n=3, color="red", soul=SOUL)
    # 5. the choir loft over the door: a deck five rows up (a low gallery, six steps from the west aisle), a parapet, the organ
    GL = GROUND + 5
    for z in range(25, 34):
        for x in range(2, 25):
            h.try_set(x, GL, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"})
    for x in range(2, 25):
        h.try_set(x, GL + 1, 25, L.POLISHED_WALL)
        h.try_set(x, GL - 1, 25, L.POLISHED)
    for x in range(3, 24, 3):
        h.try_set(x, GL + 2, 25, L.LANTERN, {"hanging": False})
    for x in (5, 9, 13, 17, 21):                                   # under the loft: a lantern on a chain from the underside of the deck
        for z in (28, 32):
            K.hung_lantern_under(h, x, z, GROUND, GROUND + 3, soul=SOUL)
    D.stair_flight(h, 4, 19, "south", 6, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=0)
    for x in range(4, 23):                                         # the organ: pipes of lightning rods of every height along the south wall, a console in front of them
        for k in range(3 + (x * 7) % 5):
            h.try_set(x, GL + 1 + k, 33, "minecraft:lightning_rod", {"facing": "up"})
    for x in (11, 12, 13, 14, 15):
        h.try_set(x, GL + 1, 31, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})
    D.chair(h.frame(13, GL + 1, 30, "south"))
    # 6. what hangs in the nave: cages with skulls on long chains, and the lights of the chapel
    for (x, z) in ((8, 13), (17, 13), (8, 21), (17, 21), (12, 28)):
        D.hanging_cage(h, x, z, GROUND + 9, w=3, d=3, hgt=3)
    for z in (14, 22):
        D.chandelier_wheel(h, 13, z, r=2, drop=top - (GROUND + 7), soul=SOUL)
    for x in (4, 22):                                              # skull-faced knights of the dead court in the side aisles
        D.statue(h, x, 10, facing="east" if x == 4 else "west", kit="skull")
    D.upper_dress(h, [D.it_tall_banner("black", hang=6), D.it_high_bracket(soul=SOUL, drop=3)], sides="nsew", step=(3, 5), door_gap=2)
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("undead", 13, 20)
    K.auto_light(h, soul=SOUL, max_lamps=60, prune=True)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the coffin crypt
@room("vampire_castle_coffin_crypt", "hall")
def coffin_crypt():
    """The Coffin Crypt: a low, dark sleeping hall under oak beams. In the middle the lord's coffin lies open on a dais ringed by four braziers; round it a lane
    three wide, and from the four doors four lanes lead to it. The four quarters between the lanes are rows of the court's own coffins, lids shut or pushed aside,
    a red candle at every head. The hall is deliberately the opposite of the Great Hall: squat where that is tall, close where that is wide, and lit by candle
    and brazier instead of by windows."""
    h = Hall("mega/hall/vampire_castle_coffin_crypt", 3, 3, 14, wear=0.14)
    top = h.top
    doors = (("n", 13), ("s", 13), ("w", 13), ("e", 13))
    for side, off in doors:
        h.door(side, off)
    h.route(12, 2, 14, 9, label="north lane")
    h.route(12, 17, 14, 24, label="south lane")
    h.route(2, 12, 9, 14, label="west lane")
    h.route(17, 12, 24, 14, label="east lane")
    h.route(7, 7, 19, 9, label="ring lane north")
    h.route(7, 17, 19, 19, label="ring lane south")
    h.route(7, 7, 9, 19, label="ring lane west")
    h.route(17, 7, 19, 19, label="ring lane east")
    for side, off in doors:
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # 0. beams under the ceiling and the pilasters of the walls first (the coffins are set round whatever stands)
    D.beams(h, "x", [5, 21], y=top)
    D.beams(h, "z", [5, 21], y=top)
    for wl in (wn, ws, ww, we):
        for lat in (9, 16):
            D.pilaster(wl, lat, 2, y0=GROUND, y1=top - 1)
    # 1. the lord's dais, the open coffin and the four braziers
    D.dais(h, 10, 10, 16, 16, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    K.coffin(h, 12, 12, "z", open_lid=True, y=GROUND + 1)
    for (bx, bz) in ((11, 11), (15, 11), (11, 15), (15, 15)):
        D.brazier(h, bx, bz, y=GROUND + 1)
    # 2. the four quarters: three coffins along the wall of each, a red candle at every head, every third one open
    n = 0
    for (qx, qz) in ((4, 3), (4, 6), (4, 9)):
        for flip_x in (False, True):
            for flip_z in (False, True):
                x = 24 - qx if flip_x else qx
                z = 25 - qz if flip_z else qz
                K.coffin(h, x, z, "x", open_lid=(n % 3 == 0), head_first=not flip_x)
                n += 1
    for side in ("n", "s", "w", "e"):
        D.dress_wall(h, side, [D.it_candle_niche("red", y=GROUND + 3), D.it_banner("black", y=GROUND + 8, hang=3)], step=(2, 3), door_gap=2)
    D.chandelier_wheel(h, 13, 13, r=2, drop=top - (GROUND + 7), soul=SOUL)
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("undead", 5, 5)
    K.auto_light(h, soul=SOUL, max_lamps=50, prune=True)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the clock tower
CT_R1, CT_R2, CT_R3 = GROUND + 7, GROUND + 16, GROUND + 25       # the deck rows of the two galleries and of the belfry floor (you stand on the row above)


def ring_band(lo=2, hi=24, w=3):
    """The (x, z) cells of the square ring `w` wide along the inside of the walls of the tower."""
    return [(x, z) for z in range(lo, hi + 1) for x in range(lo, hi + 1) if min(x - lo, hi - x, z - lo, hi - z) < w]


def gallery(h, row, skip, lantern_every=3):
    """A walkway ring at `row`: planks (a log now and then) on every cell of the band except `skip`, a parapet of wall blocks along its inner edge with a standing
    lantern on every `lantern_every`-th post, and a lantern hung under it every fifth cell of the outer lane (the ground under the ring is the dark part of a
    tower). Returns the cells of its deck."""
    band = ring_band()
    bandset = set(band)
    deck = []
    for (x, z) in band:
        if (x, z) in skip:
            continue
        if h.try_set(x, row, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"}):
            deck.append((x, z))
    deckset = set(deck)
    k = 0
    for (x, z) in deck:
        edge = any((x + dx, z + dz) not in bandset and 2 <= x + dx <= 24 and 2 <= z + dz <= 24 for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge:
            if h.try_set(x, row + 1, z, L.POLISHED_WALL):
                k += 1
                if k % lantern_every == 0:
                    h.try_set(x, row + 2, z, L.LANTERN, {"hanging": False})
    return deckset


@room("vampire_castle_clock_tower", "set")
def clock_tower():
    """The Clock Tower: a square tower thirty-seven rows high with a well in the middle. Two galleries run round the walls, one over the doors and one over that,
    reached by three flights (one in each of three corners); the third leads to the belfry, a floor round a shaft with a great bell on a beam and, in each wall,
    the back of a clock face of white and black glass lit from behind. From the beam over the shaft a chain as long as the tower carries the pendulum, an iron weight
    that hangs over a clock face of chiselled stone in the floor, and wooden gear wheels turn on the walls between the galleries. A set piece: you climb it
    for the view and the bell, and everything in it that lives knows you are coming by the tick."""
    h = Hall("mega/set/vampire_castle_clock_tower", 3, 3, 37, wear=0.10)
    top = h.top
    doors = (("n", 13), ("s", 13), ("w", 13), ("e", 13))
    for side, off in doors:
        h.door(side, off)
    h.route(12, 2, 14, 24, label="north-south lane")
    h.route(2, 12, 24, 14, label="west-east lane")
    for side, off in doors:
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # 1. the floor: a clock face of chiselled stone, twelve marks and a ring
    for z in range(2, 25):
        for x in range(2, 25):
            d = math.hypot(x - 13, z - 13)
            if 8.5 <= d < 9.5:
                h.floor_set(x, z, L.POLISHED)
            elif d < 1.5:
                h.floor_set(x, z, L.CHISELED)
    for k in range(12):
        a = math.radians(30 * k)
        for r in (7, 8):
            h.floor_set(int(round(13 + r * math.sin(a))), int(round(13 - r * math.cos(a))), L.CHISELED)
    # 2. the flights: west wall (north of the door) up to the first gallery, east wall up to the second, south wall up to the belfry
    D.stair_flight(h, 2, 10, "north", 8, 3, GROUND, stairs=L.POLISHED_STAIRS, rail_r=2)
    D.stair_flight(h, 24, 3, "south", 9, 3, GROUND + 8, stairs=L.POLISHED_STAIRS, rail_r=2)
    D.stair_flight(h, 3, 22, "east", 9, 3, GROUND + 17, stairs=L.POLISHED_STAIRS, rail_r=0)
    f0 = {(x, z) for x in (2, 3, 4) for z in range(3, 11)}
    f1 = {(x, z) for x in (22, 23, 24) for z in range(3, 12)}
    f2 = {(x, z) for x in range(3, 12) for z in (22, 23, 24)}
    # solid columns under the corner ends of the galleries are not needed: a gallery is a deck on the wall; the flights carry their own fill
    d1 = gallery(h, CT_R1, f0)
    d2 = gallery(h, CT_R2, f1)
    # the belfry floor: every cell of the tower but the shaft and the top of the third flight
    shaft = {(x, z) for x in range(11, 16) for z in range(11, 16)}
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in shaft or (x, z) in f2:
                continue
            h.try_set(x, CT_R3, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"})
    for z in range(10, 17):                                        # a parapet round the shaft, on the deck cells that border it
        for x in range(10, 17):
            if x in (10, 16) or z in (10, 16):
                h.try_set(x, CT_R3 + 1, z, L.POLISHED_WALL)
                if (x + z) % 3 == 0:
                    h.try_set(x, CT_R3 + 2, z, L.LANTERN, {"hanging": False})
    # 3. the pendulum: a chain from the beam over the shaft to a weight of iron over the clock face, and the beam with the bell
    for x in range(2, 25):                                         # two beams cross under the ceiling over the shaft; the pendulum hangs from the crossing, the bell from the north half
        h.try_set(x, top, 13, L.STRIPPED_LOG, {"axis": "x"})
    for z in range(2, 25):
        h.try_set(13, top, z, L.STRIPPED_LOG, {"axis": "z"})
    for y in range(GROUND + 6, top):
        h.try_set(13, y, 13, L.CHAIN_BLOCK, {"axis": "y"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for dy in (4, 5):
                if (dx, dz) == (0, 0) and dy == 5:
                    continue
                h.try_set(13 + dx, GROUND + dy, 13 + dz, "minecraft:iron_block")
    h.try_set(13, GROUND + 5, 13, L.CHAIN_BLOCK, {"axis": "y"})
    h.try_set(13, top - 1, 8, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": False})
    h.try_set(13, GROUND + 3, 13, L.LANTERN, {"hanging": True})   # a lantern under the weight, or the floor of the clock face would stand in its shadow
    for z in (9, 6, 3):                                            # a wall torch every third step above each flight, on the wall side where the lanterns of the parapets do not reach
        if ww.can(z, GROUND + (10 - z) + 3, 0):
            D.wall_torch(ww, z, GROUND + (10 - z) + 3, soul=SOUL)
    for z in (4, 7, 10):
        if we.can(z, GROUND + 8 + (z - 3) + 3, 0):
            D.wall_torch(we, z, GROUND + 8 + (z - 3) + 3, soul=SOUL)
    for x in (4, 7, 10):
        if ws.can(x, GROUND + 17 + (x - 3) + 3, 0):
            D.wall_torch(ws, x, GROUND + 17 + (x - 3) + 3, soul=SOUL)
    # 4. gears: wooden wheels on the walls between the galleries
    for (wl, lat, y) in ((wn, 8, GROUND + 12), (wn, 18, GROUND + 21), (we, 18, GROUND + 12), (ws, 18, GROUND + 12), (ww, 20, GROUND + 21), (we, 8, GROUND + 21)):
        try:
            D.breaking_wheel(wl, lat, y)
        except ValueError:
            pass
    # 5. the faces: the back of a clock, white and black, in each wall of the belfry, and louvres of bars for the bell
    face = ("minecraft:white_stained_glass", "minecraft:light_gray_stained_glass", "minecraft:white_stained_glass", "minecraft:black_stained_glass")
    for wl in (wn, ws, ww, we):
        if not rose_window(wl, 13, CT_R3 + 6, 4, face, glow="minecraft:sea_lantern"):
            raise ValueError("a clock face does not fit its wall")
        for lat in (6, 20):
            K.gothic_window(wl, lat, CT_R3 + 3, CT_R3 + 9, "minecraft:iron_bars", width=3, glow=None)
    # 6. windows of red glass at the head of each flight and lamps for every tier
    for wl in (wn, ws):
        for lat in (6, 20):
            K.gothic_window(wl, lat, GROUND + 10, GROUND + 14, RED_GLASS, width=3)
            K.gothic_window(wl, lat, GROUND + 19, GROUND + 23, RED_GLASS, width=3)
    for wl in (ww, we):
        for lat in (6, 20):
            K.gothic_window(wl, lat, GROUND + 10, GROUND + 14, RED_GLASS, width=3)
            K.gothic_window(wl, lat, GROUND + 19, GROUND + 23, RED_GLASS, width=3)
    for (x, z) in ((7, 7), (19, 7), (7, 19), (19, 19)):
        D.chandelier_wheel(h, x, z, r=2, drop=(CT_R3 - 1) - (GROUND + 6), y_top=CT_R3 - 1, soul=SOUL)
    for (x, z) in ((9, 5), (17, 5), (9, 21), (17, 21), (6, 9), (6, 17), (20, 9), (20, 17)):
        D.brazier(h, x, z)
    D.upper_dress(h, [D.it_tall_banner("red", hang=5)], sides="nsew", step=(4, 6), door_gap=2, lo=None, hi=None)
    D.cornice(h)
    D.cobwebs(h, 0.6)
    h.faction("construct", 13, 20)
    K.auto_light(h, soul=SOUL, max_lamps=60, prune=True)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the blood court
@room("vampire_castle_blood_court", "trial")
def blood_court():
    """The Blood Court: the throne room of the castle, and its trial. Walk in through either door and the Sealed Archway shuts both (the barriers are the ones
    of the Spire's trial halls, same blocks, same rules): a ring of red sigils in the floor lets the court's thralls and hounds in, the keystone hangs over the
    middle on two chains, and the barriers fall when the last of them is dead. A throne on a dais under a great window of red glass, twelve pillars with lanterns,
    a red runner from door to door, and the court's tribute in a chest at the throne's foot. Same mechanism and checks as the trial halls of ROOMS-2, a different
    room and a different roster (dire wolves and zombies for the hounds and the thralls)."""
    import mega_decor_r2 as D2
    h = Hall("mega/trial/vampire_castle_blood_court", 3, 3, 14, wear=0.10)
    top = h.top
    doors = (("w", 13), ("e", 13))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    c = 13
    # 1. the floor: red and black flags in rings, the runner between the doors
    for z in range(2, 25):
        for x in range(2, 25):
            d = D2.dist(x, z, c, c)
            if d <= 4.5:
                h.floor_set(x, z, L.POLISHED if (x + z) % 2 else L.CHISELED)
            elif 7.5 < d <= 8.5:
                h.floor_set(x, z, L.CHISELED if int(D2.angle_of(x, z, c, c) / 22.5) % 2 == 0 else L.POLISHED)
    for x in range(3, 24):
        for z in (12, 13, 14):
            if D2.dist(x, z, c, c) > 4.5:
                h.try_set(x, GROUND, z, "minecraft:red_carpet")
    # 2. the trial: ward runes in the doorways, glyphs on a ring (none under the throne dais, two more on the south axis), the keystone over the middle
    glyphs = [g for g in D2.around(c, c, 8, 8, 22.5) if not (9 <= g[0] <= 17 and g[1] <= 6)] + [(13, 19), (13, 21)]
    K.trial_core(h, doors, glyphs, [("minecraft:zombie", 3), ("nusmp:dungeon_direwolf", 2)])
    # 3. the throne: a dais at the north end, the throne under a banner, the tribute, braziers, the great window over it
    D.dais(h, 9, 2, 17, 6, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    h.set(13, GROUND + 1, 3, L.POLISHED)
    h.set(12, GROUND + 1, 3, L.POLISHED_STAIRS, {"facing": "east", "half": "bottom"})
    h.set(14, GROUND + 1, 3, L.POLISHED_STAIRS, {"facing": "west", "half": "bottom"})
    for y in range(GROUND + 2, GROUND + 7):
        h.try_set(13, y, 2, L.CHISELED if y == GROUND + 6 else L.POLISHED)
    h.loot_chest(13, GROUND + 1, 5, "south", LOOT)
    for x in (9, 17):
        D.brazier(h, x, 4, y=GROUND + 1)
    K.gothic_window(wn, 13, GROUND + 6, GROUND + 12, RED_GLASS, width=3)
    for k in range(4):
        x, z = D2.polar(c, c, 3.4, 45.0 + 90.0 * k)
        D.brazier(h, x, z, y=GROUND)
    # 4. twelve pillars with a lantern hung beside each, red cloth between them
    for (px, pz) in D2.around(c, c, 10.5, 12, 15.0):
        if all(h.can_set(px + dx, y, pz + dz, L.POLISHED) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in (GROUND, top)):
            D.column(h, px, pz, 1, y0=GROUND, y1=top)
            ix, iz = D2.polar(c, c, D2.dist(px, pz, c, c) - 2.0, D2.angle_of(px, pz, c, c))
            if all(h.can_set(ix, y, iz, "minecraft:chain") for y in range(GROUND + 4, top + 1)):
                D.hang_lantern(h, ix, iz, drop=top - (GROUND + 4), soul=SOUL)
    D.upper_dress(h, [D.it_tall_banner("red", hang=5)], sides="nsew", step=(4, 6), door_gap=2)
    D.cornice(h)
    D.cobwebs(h, 0.6)
    h.faction("undead", 6, 20)
    K.auto_light(h, soul=SOUL, max_lamps=40, prune=True)
    D.auto_markers(h, spawns=0, patrols=3, centre=False)
    return h


@REG.check("vampire_castle_blood_court")
def blood_court_check(h):
    return K.check_trial(h)


# ----------------------------------------------------------------------------------------------------------------------------------- the mirror gallery
@room("vampire_castle_mirror_gallery", "treasure")
def mirror_gallery():
    """The Gallery of Mirrors: the long, quiet room where the court once dressed for its feasts. Six tall mirrors in gilded frames hang on the long walls, and not
    one of them shows anything (they are panes of black glass: the court cast no reflection, so nobody ever looked into them). Candelabra stand between them, a red
    runner leads from the south door to the far end, four guests in golden armour wait before the mirrors for a dance that never comes, and on a dais at the north
    end, under the biggest mirror of all, stand the chests of the house. The calm room of the castle: nothing here spawns, and nothing here is warm."""
    h = Hall("mega/treasure/vampire_castle_mirror_gallery", 2, 3, 12, wear=0.05)
    top = h.top
    doors = (("s", 4), ("n", 13))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    for wl in (ww, we):
        for lat in (9, 15):                                        # pilasters between the mirrors
            D.pilaster(wl, lat, 1, y0=GROUND, y1=top - 1)
    # the mirrors: three on each long wall, a tall one over the dais at the north end
    n = 0
    for wl in (ww, we):
        for lat in (6, 12, 18):
            if K.mirror(wl, lat, GROUND + 2, GROUND + 7, width=3):
                n += 1
    if n < 6:
        raise ValueError("the long walls take %d of six mirrors" % n)
    K.mirror(wn, 6, GROUND + 3, GROUND + 9, width=5)
    # the runner from the south door, the dais and the chests of the house
    for x in range(3, 6):
        for z in range(6, 24):
            h.try_set(x, GROUND, z, "minecraft:red_carpet")
    D.dais(h, 3, 2, 10, 5, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    for x in (5, 8):
        h.loot_chest(x, GROUND + 1, 3, "south", LOOT)
    D.candelabra(h, 6, 3, y=GROUND + 1, n=4, color="red", soul=SOUL)
    D.candelabra(h, 7, 3, y=GROUND + 1, n=4, color="red", soul=SOUL)
    # four guests in golden armour on the east side, before the mirrors that do not show them; candelabra on the west side
    for z in (9, 15, 21):
        D.statue(h, 13, z, facing="west", kit="gold")
    D.statue(h, 10, 12, facing="west", kit="gold")
    for z in (8, 14):
        D.candelabra(h, 7, z, n=4, color="red", soul=SOUL)
    for z in (9, 17):
        D.chandelier_wheel(h, 8, z, r=2, drop=3, soul=SOUL)
    D.upper_dress(h, [D.it_tall_banner("red", hang=3)], sides="ns", step=(4, 6), door_gap=2)
    D.cornice(h)
    D.cobwebs(h, 0.3)
    h.faction("undead", 8, 14)
    K.auto_light(h, soul=SOUL, max_lamps=20, prune=True)
    D.auto_markers(h, spawns=2, patrols=1, centre=False)
    return h


if __name__ == "__main__":
    sys.exit(K.main(REG, sys.argv[1:], calm=CALM))

#!/usr/bin/env python3
"""
CELESTIAL_COURT (1.59.0, docs/labyrinth/mega_v2_tha.md): the signature rooms of the heavenly court of the Holy road: law and light in white and grey stone with
gold and a cyan accent. Rooms (all `celestial_court_*`, drawn ONLY in sectors of the theme through `data/nusmp/mega/template_themes/celestial_court.json`):

    hall/celestial_court_terrace      three marble terraces climbing to a halo window, a fountain on each, colonnaded side walks, a glass roof
    hall/celestial_court_colonnade    the processional hall of the Golden Gate: a double colonnade, a gate of gold and bars, banners, a long carpet
    set/celestial_court_throne_room   the throne room: a stepped dais, a throne with a ring of gold and light behind it, a canopy of glass
    hall/celestial_court_sundial      the sundial court: a round hall with a great sundial in the floor and a gnomon of gold
    hall/celestial_court_banquet      the banquet hall: two long tables laid for a feast, golden candelabra, a fountain wall
    trial/celestial_court_tribunal    the tribunal: a court of tiers and a judges' bench; walking in seals the doors and the court sends its guardians
    safe/celestial_court_fountain_court  a quiet court round a fountain: seats, a hearth and beds for a rest

    python3 tools/labyrinth/gen_mega_theme_celestial_court.py              # (re)write every room, check each one (a room with a defect is NOT written)
    python3 tools/labyrinth/gen_mega_theme_celestial_court.py terrace      # only this one
    python3 tools/labyrinth/gen_mega_theme_celestial_court.py --check      # write nothing; compare with the files on disk (CI style)

Why a generator: see gen_mega_rooms1.py; the same kit (`mega_decor_r1`) and the same gates run here, plus `mega_tha_kit` for windows, arcades, decks and roofs.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_tha_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, Hall  # noqa: E402

ROOMS = {}
THEME = "celestial_court"
GOLD = "minecraft:gold_block"
WHITE = "minecraft:white_concrete"
CYAN = "minecraft:cyan_stained_glass"


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


# the glass of the Court: every window set is white, cyan and a little gold, never the jewel colours of the Church
SKY = (("cyan", "white", "light_blue", "white"), ("white", "cyan", "white", "yellow"), ("light_blue", "cyan", "white", "cyan"), ("yellow", "white", "cyan", "white"))
HALO = ("yellow", "white", "cyan", "light_blue", "white", "yellow")


def basin(h, x1, z1, x2, z2, y, spout=True):
    """A raised fountain basin at row y: a rim of quartz blocks round still water (the water cell is shut in on four sides by the rim and below by the floor), a
    spout of gold and light in the middle of it."""
    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            rim = x in (x1, x2) or z in (z1, z2)
            if rim:
                h.set(x, y, z, L.POLISHED)
            elif spout and (x, z) == (cx, cz):
                h.set(x, y, z, GOLD)
                h.set(x, y + 1, z, GOLD)
                h.set(x, y + 2, z, "minecraft:end_rod", {"facing": "up"})
            else:
                h.set(x, y, z, "minecraft:water", {"level": "0"})


def statue_on(h, x, z, y, face="south", kit="gold"):
    D.statue(h, x, z, face, kit=kit, y=y)


@room("terrace")
def terrace():
    h = Hall("mega/hall/celestial_court_terrace", 3, 4, 22, wear=0.04)
    for side, off in (("s", 13), ("n", 4), ("n", 22), ("w", 22), ("e", 22)):
        h.door(side, off)
    top = h.top                                              # 30
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the terraces: T0 on the floor (z 22..33), T1 two rows up (z 14..19), T2 four rows up (z 3..11); stairs between them; solid underneath
    for z in range(14, 20):
        for x in range(8, 19):
            for y in (GROUND, GROUND + 1):
                h.set(x, y, z, L.COBBLED if y == GROUND else (L.POLISHED if (x + z) % 2 else L.TILE))
    for z in range(3, 12):
        for x in range(8, 19):
            for y in range(GROUND, GROUND + 4):
                h.set(x, y, z, L.COBBLED if y < GROUND + 3 else (L.POLISHED if (x + z) % 2 else L.TILE))
    for k, z in enumerate((21, 20)):                         # T0 -> T1: two steps
        for x in range(8, 19):
            h.set(x, GROUND + k, z, L.POLISHED_STAIRS if 9 <= x <= 17 else L.POLISHED, {"facing": "north", "half": "bottom"} if 9 <= x <= 17 else None)
            for y in range(GROUND, GROUND + k):
                h.set(x, y, z, L.COBBLED)
    for k, z in enumerate((13, 12)):                         # T1 -> T2: two steps
        for x in range(8, 19):
            h.set(x, GROUND + 2 + k, z, L.POLISHED_STAIRS if 9 <= x <= 17 else L.POLISHED, {"facing": "north", "half": "bottom"} if 9 <= x <= 17 else None)
            for y in range(GROUND, GROUND + 2 + k):
                h.set(x, y, z, L.COBBLED)
    # balustrades along the long edges of the terraces
    for (z1, z2, y) in ((14, 19, GROUND + 2), (3, 11, GROUND + 4)):
        for z in range(z1, z2 + 1):
            for x in (8, 18):
                h.set(x, y, z, L.POLISHED_WALL)
    # ---- fountains: a long pool on the lowest terrace, one on each of the others, a gold spout in each
    basin(h, 10, 24, 16, 27, GROUND)
    basin(h, 11, 15, 15, 17, GROUND + 2)
    basin(h, 11, 5, 15, 8, GROUND + 4)
    # ---- the colonnades of the side walks: tall columns on both sides, an architrave over them, banners and windows between
    colz = (9, 13, 17, 28, 32)
    for x in (6, 20):
        for z in colz:
            D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 15, cap=GOLD)
    # ---- the glass roof of the court: a lattice of white and cyan glass over the middle, lit from above; the halo window behind the throne terrace
    for z in range(3, 33):
        for x in range(8, 19):
            rib = (x - 8) % 5 == 0 or (z - 3) % 5 == 0
            col = SKY[((x - 8) // 5 + (z - 3) // 5) % len(SKY)][0]
            h.shell_set(x, top + 1, z, L.POLISHED if rib else K.glass(col))
            h.shell_set(x, top + 2, z, L.POLISHED if rib else "minecraft:sea_lantern")
    K.rose(wn, 13, GROUND + 14, 5, HALO, ring_block=GOLD, hub_block=GOLD, spokes=12)
    for (x, z) in ((9, 4), (17, 4), (9, 10), (17, 10)):
        statue_on(h, x, z, GROUND + 4, "south")
    for (x, z) in ((9, 15), (17, 15)):
        statue_on(h, x, z, GROUND + 2, "south")
    # ---- the outer walls: tall windows in the side walks, cyan banners, braziers of white fire at the doors
    for w, pal in ((ww, SKY), (we, SKY[::-1])):
        K.band_of_windows(w, [6, 10, 14, 18, 26, 30], GROUND + 3, GROUND + 15, pal, width=3)
    K.band_of_windows(ws, [5, 21], GROUND + 3, GROUND + 15, SKY, width=3)
    for side, lats in (("n", (8, 18)), ("s", (8, 18))):
        w = h.wall(side)
        for lat in lats:
            for y in (GROUND + 8,):
                if w.can(lat, y, 0) and w.can(lat, y - 1, 0) and w.backed(lat, y, 0):
                    w.set(lat, y, 0, "minecraft:cyan_wall_banner", {"facing": w.into})
    for z in (9, 22, 30):
        K.wagon_chandelier(h, 13, z, top, 7, r=2, color="white")
    h.faction("construct", 13, 22)
    K.auto_light(h)
    D.auto_markers(h)
    return h


def gate_arch(h, x1, x2, z, y_spring, y_top, thick=2):
    """The Golden Gate: two pylons and a pointed arch of palette stone between them, a tympanum of iron bars in a frame of gold over the opening (a portcullis drawn
    up into the arch), gold bands on the pylons. `x1`..`x2` is the clear opening (even width), `z` the first of `thick` rows."""
    import mega_arch_kit as AK
    n = x2 - x1 + 3
    for dz in range(thick):
        def put(u, y, block, props, dz=dz):
            h.set(x1 - 1 + u, y, z + dz, block, props, over=True)
        AK.pointed_arch(put, n, y_spring, y_top, "polished", "brick", "west", "east")
        for end in (x1 - 1, x2 + 1):
            for y in range(GROUND, y_spring):
                h.set(end, y, z + dz, L.POLISHED if y not in (GROUND + 1,) else GOLD, over=True)
    for y in range(y_spring, y_spring + n // 2):
        lo = x1 + (y - y_spring)
        hi = x2 - (y - y_spring)
        for x in range(lo, hi + 1):
            for dz in range(thick):
                h.try_set(x, y, z + dz, L.IRON_BARS)


@room("colonnade")
def colonnade():
    h = Hall("mega/hall/celestial_court_colonnade", 2, 5, 18, wear=0.04)
    for side, off in (("n", 4), ("s", 13), ("w", 22), ("e", 22)):
        h.door(side, off)
    top = h.top                                              # 26
    wn, ww, we = h.wall("n"), h.wall("w"), h.wall("e")
    colz = (8, 12, 16, 28, 32)
    for x in (5, 12):
        for z in colz:
            D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 15, cap=GOLD)
    # ---- the nave: a white runner with gold borders, a chequered white and slate field, a long skylight of glass and light
    for z in range(2, 43):
        for x in range(6, 12):
            h.floor_set(x, z, L.POLISHED if (x + z) % 2 else L.TILE)
    for z in range(3, 42):
        for x in (7, 8, 9, 10):
            h.try_set(x, GROUND, z, "minecraft:white_carpet")
        if z % 2:
            for x in (6, 11):
                h.try_set(x, GROUND, z, "minecraft:yellow_carpet")
    for z in range(3, 42):
        for x in range(6, 12):
            rib = (z - 3) % 6 == 0 or x in (6, 11)
            col = SKY[(z // 6) % len(SKY)][x % 4]
            h.shell_set(x, top + 1, z, L.POLISHED if rib else K.glass(col))
            h.shell_set(x, top + 2, z, L.POLISHED if rib else "minecraft:sea_lantern")
    # ---- the Golden Gate across the nave, the first third of the way in from the south door
    gate_arch(h, 7, 10, 36, GROUND + 4, GROUND + 12)
    # ---- the side walks: tall windows, banners on the columns, statues of archons between them, chandeliers over the nave
    for w, pal in ((ww, SKY), (we, SKY[::-1])):
        K.band_of_windows(w, [10, 14, 18, 30, 34, 38], GROUND + 3, GROUND + 14, pal, width=3)
    K.lancet(wn, 8, GROUND + 3, GROUND + 15, HALO, width=5)
    for z in (10, 30):
        for x, face in ((3, "east"), (14, "west")):
            statue_on(h, x, z, GROUND, face)
    for z in (14, 24, 33):
        K.wagon_chandelier(h, 8, z, top, 6, r=1, color="white")
    for x, nave_x, face in ((5, 6, "east"), (12, 11, "west")):
        for z in colz:
            h.try_set(nave_x, GROUND + 6, z, "minecraft:cyan_wall_banner", {"facing": face})
    h.faction("construct", 8, 22)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("throne_room")
def throne_room():
    h = Hall("mega/set/celestial_court_throne_room", 3, 4, 24, wear=0.03)
    for side, off in (("s", 13), ("w", 31), ("e", 31)):
        h.door(side, off)
    top = h.top                                              # 32
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the roof: a hip vault from row 18 with a ridge of white glass over the middle of the hall, lit from above
    K.hip_roof(h, 2, 2, 24, 33, GROUND + 9, top, cap=K.glass("white"), glow="minecraft:sea_lantern")
    # ---- the dais: a platform three rows high at the north end, a flight of three steps up to it, and its sides filled to the height of each step
    for x in range(8, 19):
        for z in range(2, 10):
            for y in range(GROUND, GROUND + 3):
                h.set(x, y, z, L.COBBLED if y < GROUND + 2 else (L.POLISHED if (x + z) % 2 else L.TILE))
    K.stairs_up(h, 9, 12, "north", 3, width=9, base=GROUND)
    for x in (8, 18):
        for z, rows in ((12, 1), (11, 2), (10, 3)):
            for y in range(GROUND, GROUND + rows):
                h.set(x, y, z, L.POLISHED)
    # ---- the throne: a seat facing the hall, wall armrests, a high back of polished stone crowned with gold and a rod of light
    h.set(13, GROUND + 3, 4, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for x in (12, 14):
        h.set(x, GROUND + 3, 4, L.POLISHED_WALL)
    for y in range(GROUND + 3, GROUND + 8):
        for x in (12, 13, 14):
            h.set(x, y, 3, L.POLISHED if y < GROUND + 7 else L.CHISELED)
    for x in (12, 13, 14):
        h.set(x, GROUND + 8, 3, GOLD)
    K.rose(wn, 13, GROUND + 5, 4, HALO, ring_block=GOLD, hub_block=GOLD, spokes=12)
    for (x, z) in ((9, 5), (17, 5), (9, 8), (17, 8)):
        statue_on(h, x, z, GROUND + 3, "south")
    # ---- the hall: a long carpet of cyan with gold borders, two rows of columns, tall windows, chandeliers from the ridge
    for z in range(13, 34):
        for x in (12, 13, 14):
            h.try_set(x, GROUND, z, "minecraft:cyan_carpet")
        for x in (11, 15):
            h.try_set(x, GROUND, z, "minecraft:yellow_carpet")
    for x in (6, 20):
        for z in (14, 18, 22, 26):
            D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 8, cap=GOLD)
    for w, pal in ((ww, SKY), (we, SKY[::-1])):
        K.band_of_windows(w, [8, 12, 16, 20, 24, 28], GROUND + 3, GROUND + 8, pal, width=3)
    K.band_of_windows(ws, [6, 20], GROUND + 3, GROUND + 8, SKY, width=3)
    for z in (15, 21):
        K.wagon_chandelier(h, 13, z, GROUND + 19, 7, r=2, color="white")
    for z in (10, 14, 18, 22, 26):
        for x, face in ((2, "east"), (24, "west")):
            h.try_set(x, GROUND + 5, z, "minecraft:cyan_wall_banner", {"facing": face})
    h.faction("construct", 13, 22)
    K.auto_light(h)
    D.auto_markers(h)
    return h


def polar(cx, cz, r, deg):
    """The cell at distance r and angle `deg` (0 = east, 90 = south) from (cx, cz)."""
    import math
    return int(round(cx + r * math.cos(math.radians(deg)))), int(round(cz + r * math.sin(math.radians(deg))))


round_wall = K.round_wall                                   # the drum helper lives in the kit: the Sky Citadel's storm spire is round too


@room("sundial")
def sundial():
    import math
    h = Hall("mega/hall/celestial_court_sundial", 3, 3, 16, wear=0.04)
    for side in ("n", "s", "w", "e"):
        h.door(side, 13)
    top = h.top                                              # 24
    c = 13
    round_wall(h, c, c, 11.3, top)
    # ---- the floor: the dial. A polished disc with a gold hour ring, twelve hour marks, a slate hub, the shadow of the gnomon laid in dark concrete
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if d > 11.3:
                continue
            ang = (math.degrees(math.atan2(z - c, x - c)) + 360.0) % 360.0
            near_hour = min(abs(((ang + 15.0) % 30.0) - 15.0), 30.0) < 3.0 / max(d, 1) * 12.0
            if d <= 3.2:
                blk = L.TILE if d > 1.2 else L.CHISELED
            elif 5.8 <= d < 6.8:
                blk = GOLD
            elif 8.2 <= d < 10.4 and near_hour:
                blk = GOLD if d >= 9.3 else L.CHISELED
            elif 10.4 <= d <= 11.3:
                blk = L.CHISELED if int(ang / 15.0) % 2 == 0 else L.POLISHED
            elif d < 8.2 and (int(ang / 30.0) + int(d)) % 2 == 0:
                blk = L.POLISHED
            else:
                blk = L.TILE
            h.floor_set(x, z, blk)
    for k in range(4, 12):                                   # the shadow: a band of dark concrete from the hub toward the north-west, the way the sun lies at the tenth hour
        x, z = polar(c, c, k, 225)
        for dx, dz in ((0, 0), (1, 0), (0, 1)) if k < 8 else ((0, 0),):
            if 6.8 > math.hypot(x + dx - c, z + dz - c) >= 5.8:
                continue
            h.floor_set(x + dx, z + dz, "minecraft:gray_concrete")
    # the gnomon: a stepped base, a gold shaft, a rod of light on top, rising into the oculus
    D.dais(h, c - 1, c - 1, c + 1, c + 1, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    for y in range(GROUND + 1, GROUND + 13):
        h.set(c, y, c, GOLD if y % 3 else L.CHISELED)
    h.set(c, GROUND + 13, c, "minecraft:lightning_rod", {"facing": "up", "powered": False, "waterlogged": False})
    # ---- the oculus: a ring of gold, a disc of white and cyan glass lit from above
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if d <= 6.5:
                edge = d > 5.5
                h.shell_set(x, top + 1, z, GOLD if edge else K.glass(("white", "cyan", "light_blue")[(int(d) + (x + z)) % 3]))
                h.shell_set(x, top + 2, z, GOLD if edge else "minecraft:sea_lantern")
    # ---- twelve columns round the dial, benches between them, windows cut into the wall mass at the angles between the doors
    for k in range(12):
        px, pz = polar(c, c, 8.8, 15 + 30 * k)
        if all(h.can_set(px + dx, GROUND, pz + dz, L.POLISHED_STAIRS) for dx in (-1, 0, 1) for dz in (-1, 0, 1)):
            D.column(h, px, pz, w=1, y0=GROUND, y1=GROUND + 11, cap=GOLD)
    for k in range(12):
        bx, bz = polar(c, c, 10.0, 30 * k)
        if k % 3 and h.free(bx, GROUND, bz) and h.free(bx, GROUND + 1, bz):
            face = "south" if abs(bz - c) > abs(bx - c) and bz < c else "north" if abs(bz - c) > abs(bx - c) else "east" if bx < c else "west"
            h.set(bx, GROUND, bz, L.POLISHED_STAIRS, {"facing": D.OPP[face], "half": "bottom"})
    for k in range(8):
        ang = 22.5 + 45 * k
        wx, wz = polar(c, c, 11.9, ang)
        bx, bz = polar(c, c, 13.0, ang)
        for y in range(GROUND + 3, GROUND + 12):
            if (wx, y, wz) in h.mt.t.cells and (bx, y, bz) in h.mt.t.cells and h.get(wx, y, wz).block in D.WALL_BLOCKS:
                col = SKY[k % 4][(y - GROUND) // 3 % 4]
                h.mt.t.cells[(wx, y, wz)] = type(h.get(wx, y, wz))(K.glass(col), {}, None)
                h.mt.t.cells[(bx, y, bz)] = type(h.get(bx, y, bz))("minecraft:sea_lantern", {}, None)
    for (x, z) in ((6, 6), (20, 6), (6, 20), (20, 20)):
        if h.free(x, GROUND, z):
            statue_on(h, x, z, GROUND, "south")
    h.faction("construct", c, 4)
    K.auto_light(h)
    D.auto_markers(h)
    return h


def feast_table(h, x1, x2, z1, z2, y=GROUND):
    """A long banqueting table: a bed of top-half slabs with a white cloth over it; on the table, every four rows, a golden candle cluster, a melon, a cake or a pot
    of flowers, so it reads as laid for a feast. (A candle or a cake stands on a top slab, never on a carpet: the carpet is no support.) Chairs are the caller's."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            h.set(x, y, z, L.PLANKS_SLAB, {"type": "top"})
            k = (z - z1) % 4
            if k == 0 and x == x1:
                D.candles_at(h, x, y + 1, z, 3, "yellow")
            elif k == 1 and x == x2:
                h.set(x, y + 1, z, "minecraft:melon")
            elif k == 2 and x == x1:
                if (z - z1) % 8 == 2:
                    h.set(x, y + 1, z, "minecraft:cake", {"bites": "0"})
                else:
                    h.set(x, y + 1, z, "minecraft:potted_azure_bluet")
            elif k == 3 and x == x2:
                h.set(x, y + 1, z, "minecraft:potted_red_tulip")
            else:
                h.set(x, y + 1, z, "minecraft:white_carpet")


@room("banquet")
def banquet():
    h = Hall("mega/hall/celestial_court_banquet", 3, 3, 14, wear=0.04)
    for side, off in (("s", 13), ("w", 13), ("e", 13), ("n", 4)):
        h.door(side, off)
    top = h.top                                              # 22
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    h.route(12, 8, 14, 24, label="the way between the tables")
    # ---- the floor: polished lines either side of a cyan runner down the middle
    for z in range(2, 25):
        for x in (11, 15):
            h.floor_set(x, z, L.POLISHED)
    for z in range(7, 24):
        for x in (12, 13, 14):
            h.try_set(x, GROUND, z, "minecraft:cyan_carpet")
    # ---- the high table on a dais at the north end (one step up), with a seat behind it for every second place, a gilded one in the middle
    for x in range(7, 20):
        for z in range(2, 6):
            h.set(x, GROUND, z, L.POLISHED if (x + z) % 2 else L.TILE)
        h.set(x, GROUND, 6, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for x in range(8, 19):
        h.set(x, GROUND + 1, 4, L.PLANKS_SLAB, {"type": "top"})
        h.set(x, GROUND + 2, 4, "minecraft:white_carpet")
    for x in range(9, 18, 2):
        h.set(x, GROUND + 1, 3, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
    h.set(13, GROUND + 1, 3, L.POLISHED_STAIRS, {"facing": "south", "half": "bottom"}, over=True)
    for x in (9, 13, 17):
        D.candelabra(h, x, 2, y=GROUND + 1, n=4, color="yellow")
    # ---- the two long tables, a row of chairs on each side of each
    for (x1, x2, outer, inner, f_out, f_in) in ((9, 10, 8, 11, "east", "west"), (16, 17, 18, 15, "west", "east")):
        feast_table(h, x1, x2, 9, 23)
        for z in range(9, 24):
            h.set(outer, GROUND, z, L.PLANKS_STAIRS, {"facing": D.OPP[f_out], "half": "bottom"})
            h.set(inner, GROUND, z, L.PLANKS_STAIRS, {"facing": D.OPP[f_in], "half": "bottom"})
    h.marker(8, GROUND + 1, 12, "seat", facing="east", **{"as": "chair"})
    # ---- windows, a halo window behind the high table, basins in the south corners, chandeliers over the tables
    for w, lats in ((ww, [6, 10, 17, 21]), (we, [6, 10, 17, 21]), (ws, [6, 20])):
        K.band_of_windows(w, lats, GROUND + 3, GROUND + 11, SKY, width=3)
    K.rose(wn, 13, GROUND + 7, 4, HALO, ring_block=GOLD, hub_block=GOLD, spokes=12)
    for x1 in (3, 21):
        basin(h, x1, 21, x1 + 2, 23, GROUND, spout=False)
    for z in (11, 21):
        for x in (9, 17):
            K.wagon_chandelier(h, x, z, top, 6, r=1, color="yellow")
    for z in (12, 18):
        K.wagon_chandelier(h, 13, z, top, 5, r=1, color="white")
    h.faction("construct", 4, 8)
    K.auto_light(h)
    D.auto_markers(h)
    return h


@room("tribunal")
def tribunal():
    import math
    h = Hall("mega/trial/celestial_court_tribunal", 3, 3, 14, wear=0.03)
    h.door("w", 13)
    h.door("e", 13)
    h.no_spawn = True
    c = 13
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the arena: a polished disc, a gold ring, a hub; a cross of slate between the two doors
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if 6.4 <= d < 7.6:
                h.floor_set(x, z, GOLD)
            elif d < 1.6:
                h.floor_set(x, z, L.CHISELED)
            elif d < 6.4 and (abs(z - c) <= 1):
                h.floor_set(x, z, L.TILE)
            elif d < 6.4:
                h.floor_set(x, z, L.POLISHED if (x + z) % 2 else L.TILE)
    glyphs = []
    for k in range(8):
        gx, gz = polar(c, c, 5.0, 22.5 + 45 * k)
        glyphs.append((gx, gz))
    K.trial_core(h, [("nusmp:living_armor", 3), ("minecraft:vex", 2)], glyphs, radius=13)
    # ---- the judges' dais at the north end: two steps, a long bench with lecterns, the High Judge's seat with a crown of gold and a halo window above it
    for x in range(7, 20):
        for z in range(2, 6):
            for y in (GROUND, GROUND + 1):
                h.set(x, y, z, L.COBBLED if y == GROUND and z > 2 else (L.POLISHED if (x + z) % 2 else L.TILE))
        h.set(x, GROUND, 6, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for x in range(8, 19):
        if x != 13:
            h.set(x, GROUND + 2, 5, L.POLISHED)
    for x in (10, 16):
        D.lectern(h.frame(x, GROUND + 2, 4, "south"))
    h.set(13, GROUND + 2, 3, L.POLISHED_STAIRS, {"facing": "south", "half": "bottom"})
    for y in range(GROUND + 2, GROUND + 7):
        for x in (12, 13, 14):
            h.set(x, y, 2, L.POLISHED if y < GROUND + 6 else GOLD)
    h.loot_chest(13, GROUND + 2, 4, "south")
    for (x, z) in ((9, 3), (17, 3)):
        statue_on(h, x, z, GROUND + 2, "south")
    K.rose(wn, 13, GROUND + 9, 3, HALO, ring_block=GOLD, hub_block=GOLD, spokes=12)
    # ---- the tiers of the jury on both sides: three rows of benches stepping up toward the walls, a gap at each door
    for side, xs, face in (("w", (5, 4, 3), "east"), ("e", (21, 22, 23), "west")):
        for z in list(range(8, 12)) + list(range(15, 20)):
            for k, x in enumerate(xs):
                for y in range(GROUND, GROUND + k):
                    h.set(x, y, z, L.COBBLED)
                h.set(x, GROUND + k, z, L.PLANKS_STAIRS, {"facing": D.OPP[face], "half": "bottom"})
    for (x, z) in ((7, 10), (19, 10), (7, 16), (19, 16)):
        D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 9, cap=GOLD)
    for w, lats in ((ws, [7, 13, 19]), (ww, [6, 20]), (we, [6, 20])):
        K.band_of_windows(w, lats, GROUND + 3, GROUND + 11, SKY, width=3)
    K.wagon_chandelier(h, 13, 13, GROUND + 10, 4, r=2, color="white")
    h.faction("construct", 4, 4)
    K.auto_light(h)
    D.auto_markers(h, spawns=0, patrols=2)
    return h


@room("fountain_court")
def fountain_court():
    h = Hall("mega/safe/celestial_court_fountain_court", 2, 3, 12, wear=0.02)
    h.door("n", 4)
    h.door("s", 13)
    h.safe = True
    K.keep_doors_clear(h)
    top = h.top                                              # 20
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the fountain in the middle, a skylight of white and cyan glass over it, a medallion of gold round its foot
    D.floor_medallion(h, 9, 12, r=5, ring=GOLD, core=L.CHISELED, field=L.POLISHED)
    basin(h, 7, 10, 11, 14, GROUND)
    for z in range(8, 17):
        for x in range(5, 14):
            h.shell_set(x, top + 1, z, K.glass(SKY[(x + z) % 4][0]))
            h.shell_set(x, top + 2, z, "minecraft:sea_lantern")
    # ---- eight seats round the basin, each facing it, each a rest anchor
    seats = ((6, 11, "east"), (6, 13, "east"), (12, 11, "west"), (12, 13, "west"), (8, 9, "south"), (10, 9, "south"), (8, 15, "north"), (10, 15, "north"))
    for (x, z, sit_toward) in seats:
        h.set(x, GROUND, z, L.POLISHED_STAIRS, {"facing": D.OPP[sit_toward], "half": "bottom"})
        h.marker(x, GROUND + 1, z, "seat", facing=sit_toward, **{"as": "chair"})
    for (x, z) in ((6, 9), (12, 9), (6, 15), (12, 15)):
        K.lamp_post(h, x, z, height=2)
    # ---- the hearth: a white fire in a polished niche in the north-east corner, a bench in front of it
    for x in range(12, 16):
        for z in range(2, 5):
            if (x, z) != (14, 3):
                h.set(x, GROUND, z, L.POLISHED)
    h.set(14, GROUND, 3, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false"})
    h.marker(14, GROUND + 1, 3, "hearth", **{"as": "stove"})
    h.set(14, GROUND, 5, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    h.marker(14, GROUND + 1, 5, "seat", facing="north", **{"as": "chair"})
    # ---- three beds along the west wall, a low wall between them, a banner over each
    for z in (14, 17, 20):
        D.bed(h.frame(2, GROUND, z, "east"), color="white", marker=True)
    for z in (15, 18):
        for x in (2, 3):
            h.set(x, GROUND, z, L.POLISHED_WALL)
    for z in (14, 17, 20):
        h.try_set(2, GROUND + 3, z, "minecraft:cyan_wall_banner", {"facing": "east"})
    for w, lats in ((we, [6, 11, 17, 21]), (ww, [6, 10]), (ws, [5, 20])):
        K.band_of_windows(w, lats, GROUND + 3, GROUND + 9, SKY, width=3)
    K.band_of_windows(wn, [9, 12], GROUND + 3, GROUND + 9, SKY, width=3)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    K.auto_light(h, threshold=8)
    return h


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, sys.argv[1:], __doc__.split("\n\n")[0]))

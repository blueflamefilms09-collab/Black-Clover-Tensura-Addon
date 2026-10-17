#!/usr/bin/env python3
"""
SKY_CITADEL (1.59.0, docs/labyrinth/mega_v2_tha.md): the signature rooms of a cloud giant's castle on the Holy road: white stone and cloud, wind, long drops,
furniture built for something ten times the size of the party. Rooms (all `sky_citadel_*`, drawn ONLY in sectors of the theme through
`data/nusmp/mega/template_themes/sky_citadel.json`):

    hall/sky_citadel_great_hall      the giant's great hall: a table four blocks high you can walk under (and climb, by a ladder up a leg), giant chairs, a hearth
    hall/sky_citadel_wind_gallery    a long gallery open to the sky on one side: arches of bars in front of the glare, a fire in every recess, a ring chandelier of lightning rods
    trap/sky_citadel_cloud_chasm     a round chasm over a floor of lit cloud, four rope bridges (three with planks missing) to an island of straw and storm masts with a chest, two ladders for those who fall
    set/sky_citadel_storm_spire      the Storm Heart: a drum under a dome with an oculus, a copper pillar with lit bulbs, two gantries and ladders up to a heart of light in a glass cage
    treasure/sky_citadel_pantry      the giant's pantry: shelves four boards high of barrels, jars, sacks and cheese, ladders up them, chests among the stores, hams on chains
    trial/sky_citadel_thunder_court  the thunder court: a ring of eight copper masts under a bolt of light; walking in seals the doors and a storm of breezes and vexes comes

    python3 tools/labyrinth/gen_mega_theme_sky_citadel.py              # (re)write every room, check each one (a room with a defect is NOT written)
    python3 tools/labyrinth/gen_mega_theme_sky_citadel.py great_hall   # only this one
    python3 tools/labyrinth/gen_mega_theme_sky_citadel.py --check      # write nothing; compare with the files on disk (CI style)

Why a generator: see gen_mega_rooms1.py; the same kit (`mega_decor_r1`) and the same gates run here, plus `mega_tha_kit` for windows, arcades, decks and roofs.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_tha_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

ROOMS = {}
THEME = "sky_citadel"
COPPER = "minecraft:waxed_oxidized_copper"
WOOL = "minecraft:white_wool"

# the glass of the castle: blue, white and a pale gold, like a sky at the edge of evening
SKY = (("light_blue", "white", "blue", "white"), ("white", "light_blue", "white", "cyan"), ("cyan", "white", "light_blue", "yellow"), ("blue", "light_blue", "white", "light_blue"))


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


def polar(cx, cz, r, deg):
    return int(round(cx + r * math.cos(math.radians(deg)))), int(round(cz + r * math.sin(math.radians(deg))))


def giant_chair(h, x, z, facing, y=GROUND):
    """A chair for a giant, centred on (x, z) and looking `facing` (toward the table): a seat 3 x 3 and two blocks high, a back three wide and seven high behind it,
    and two small steps in front (two stairs, one at the floor and one on the seat's front edge) so a party can climb up and look the giant in the eye. The giants'
    furniture is built to the owner's brief: oversized tables, stairs with 2-block risers; the small stairs are the party's way up."""
    fx, fz = D.DV[facing]
    rx, rz = D.DV[D.RIGHT[facing]]
    for r in (-1, 0, 1):
        for k in (-1, 0, 1):
            cx, cz = x + k * fx + r * rx, z + k * fz + r * rz
            h.set(cx, y, cz, L.LOG, {"axis": "y"})
            if k == 1:
                h.set(cx, y + 1, cz, L.PLANKS_STAIRS, {"facing": D.OPP[facing], "half": "bottom"})
            else:
                h.set(cx, y + 1, cz, L.PLANKS)
        bx, bz = x - 2 * fx + r * rx, z - 2 * fz + r * rz
        for dy in range(0, 7):
            h.set(bx, y + dy, bz, L.PLANKS if dy >= 2 else L.LOG, {"axis": "y"} if dy < 2 else None)
    for r in (-1, 0, 1):
        h.set(x + 2 * fx + r * rx, y, z + 2 * fz + r * rz, L.PLANKS_STAIRS, {"facing": D.OPP[facing], "half": "bottom"})


@room("great_hall")
def great_hall():
    h = Hall("mega/hall/sky_citadel_great_hall", 3, 5, 30, wear=0.04)
    for side, off in (("s", 13), ("n", 4), ("n", 22), ("w", 31), ("e", 13)):
        h.door(side, off)
    top = h.top                                              # 38
    ws, ww, we = h.wall("s"), h.wall("w"), h.wall("e")
    h.route(12, 6, 14, 41, y1=GROUND, y2=GROUND + 2, label="under the table")
    # ---- the hearth: a chimney breast three deep across the north wall, a firebox notched into it, three fires, a lintel and a copper band
    for z in range(2, 5):
        for x in range(7, 20):
            for y in range(GROUND, top + 1):
                if not (9 <= x <= 17 and y < GROUND + 6) and h.free(x, y, z):
                    h.set(x, y, z, D.brick_mix(h, 0.06, 0.05, L.BRICK))
    for x in range(9, 18):
        for z in range(2, 5):
            h.floor_set(x, z, L.COBBLED)
    for x in (10, 13, 16):
        h.set(x, GROUND, 3, "minecraft:campfire", {"facing": "south", "lit": "true", "signal_fire": "false"})
        h.set(x - 1, GROUND, 4, L.LOG, {"axis": "x"})
    for x in range(7, 20):
        h.set(x, GROUND + 6, 5, L.CHISELED if x in (7, 13, 19) else L.POLISHED_SLAB, None if x in (7, 13, 19) else {"type": "top"})
    for y in (GROUND + 7, GROUND + 8):
        for x in range(8, 19, 2):
            h.set(x, y, 5, L.POLISHED_WALL)
    # ---- the table: a top of planks four blocks up with a polished edge, four stout legs, a skirt between them; the way under it is the aisle of the hall
    for z in range(11, 34):
        for x in range(9, 18):
            edge = x in (9, 17) or z in (11, 33)
            h.set(x, GROUND + 4, z, L.POLISHED if edge else L.PLANKS)
    for (lx, lz) in ((9, 11), (15, 11), (9, 31), (15, 31)):
        for z in range(lz, lz + 3):
            for x in range(lx, lx + 3):
                for y in range(GROUND, GROUND + 4):
                    h.set(x, y, z, L.LOG, {"axis": "y"})
    for z in range(14, 31):
        for x in (9, 17):
            h.set(x, GROUND + 3, z, L.PLANKS_STAIRS, {"facing": "east" if x == 9 else "west", "half": "top"})
    for z in (12, 32):                                       # ladders up two legs, on the west face
        for y in range(GROUND, GROUND + 5):
            h.set(8, y, z, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
    # the feast: two loaves, a cheese, mugs, candles, two chests of the giant's own
    for dx in (0, 1):
        for dz in (0, 1):
            for dy in (0, 1):
                h.set(10 + dx, GROUND + 5 + dy, 16 + dz, "minecraft:hay_block", {"axis": "y"})
    for dz in range(3):
        for dx in range(3):
            h.set(14 + dx, GROUND + 5, 20 + dz, "minecraft:honeycomb_block")
    for (bx, bz) in ((10, 24), (16, 26), (12, 28)):
        for dy in (0, 1):
            h.set(bx, GROUND + 5 + dy, bz, "minecraft:barrel", {"facing": "up", "open": "false"})
    h.set(13, GROUND + 5, 23, "minecraft:cauldron")
    for (cx, cz) in ((11, 13), (15, 13), (11, 31), (15, 31)):
        h.set(cx, GROUND + 5, cz, L.POLISHED)
        D.candles_at(h, cx, GROUND + 6, cz, 4, "white")
    h.loot_chest(13, GROUND + 5, 14, "south")
    h.loot_chest(13, GROUND + 5, 30, "north")
    # ---- giant chairs down both long sides (a seat two blocks high, a back seven)
    for z in (17, 24, 28):
        giant_chair(h, 5, z, "east")
        giant_chair(h, 21, z, "west")
    # ---- windows as tall as three men, banners hung between them, chandeliers like wheels
    K.band_of_windows(ww, [10, 18, 25, 38], GROUND + 3, GROUND + 18, SKY, width=5)
    K.band_of_windows(we, [20, 27, 34, 40], GROUND + 3, GROUND + 18, SKY, width=5)
    K.band_of_windows(ws, [6, 20], GROUND + 3, GROUND + 18, SKY, width=5)
    for w, lats in ((ww, (14, 21, 34)), (we, (24, 31, 37))):
        for lat in lats:
            for y in range(GROUND + 6, GROUND + 12):
                if w.can(lat, y, 0) and w.backed(lat, y, 0):
                    w.set(lat, y, 0, "minecraft:%s_wall_banner" % ("light_blue" if lat % 2 else "white"), {"facing": w.into})
    for z in (18, 30):
        K.wagon_chandelier(h, 13, z, top, 10, r=4, color="white")
    h.faction("ogre", 4, 8)
    K.auto_light(h, max_lamps=240)
    D.auto_markers(h)
    return h


@room("wind_gallery")
def wind_gallery():
    h = Hall("mega/hall/sky_citadel_wind_gallery", 2, 5, 14, wear=0.05)
    for side, off in (("n", 4), ("s", 13), ("w", 22), ("e", 22)):
        h.door(side, off)
    top = h.top                                              # 22
    rng = h.rng
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the floor: slate flags with drifts of pale stone, as if cloud had blown across it
    for _ in range(26):
        bx, bz, br = rng.randint(3, 14), rng.randint(3, 41), rng.choice((1, 2, 2, 3))
        for z in range(bz - br, bz + br + 1):
            for x in range(bx - br, bx + br + 1):
                c = h.get(x, FLOOR, z) if h.inside(x, z) else None
                if c is not None and c.block == L.TILE and (x - bx) ** 2 + (z - bz) ** 2 <= br * br + 1:
                    h.floor_set(x, z, L.POLISHED)
    # ---- the open side: arches of bars in front of the sky along the west wall; on the east wall recesses with a giant's fire in each, banners between them
    for lat in (6, 12, 32, 38):
        K.bar_window(ww, lat, GROUND + 1, GROUND + 9, width=5)
    for lat in (7, 13, 31, 37):
        D.niche(we, lat, GROUND, GROUND + 5, width=3)
        h.set(*we.cell(lat, GROUND, -1), "minecraft:campfire", {"facing": "west", "lit": "true", "signal_fire": "false"})
        h.set(*we.cell(lat, GROUND + 5, -1), L.LANTERN, {"hanging": "true"})
    for lat in (10, 16, 28, 34, 40):
        for y in (GROUND + 6,):
            if we.can(lat, y, 0) and we.can(lat, y - 1, 0) and we.backed(lat, y, 0):
                we.set(lat, y, 0, "minecraft:%s_wall_banner" % ("light_blue" if lat % 4 else "white"), {"facing": we.into})
    K.lancet(wn, 10, GROUND + 3, GROUND + 12, SKY[0], width=5)
    K.lancet(ws, 5, GROUND + 3, GROUND + 12, SKY[1], width=5)
    # ---- the roof: pale beams across with a ring chandelier of lightning rods hung from the middle one, two tall columns
    D.beams(h, "x", [8, 15, 22, 29, 36], y=top, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS)
    K.wagon_chandelier(h, 8, 22, top - 1, 6, r=3, color="white", candles=False, rods=True)
    for z in (9, 35):
        D.column(h, 8, z, w=1, y0=GROUND, y1=GROUND + 11, cap=L.CHISELED)
    h.faction("elemental", 4, 20)
    K.auto_light(h)
    D.auto_markers(h)
    return h


# ---- the chasm: a round hole, an island of hay and storm masts in the middle, four bridges (three of them with planks missing), a floor of lit cloud seven blocks down
CHASM_RX, CHASM_RZ = 8.4, 7.4
ISLAND = {(x, z) for x in range(11, 16) for z in range(11, 16)}
#: bridge side -> (axis it runs along, the cells along it from the ledge to the island, the planks that are missing). Three lanes wide: a plank in the middle, a rail on
#: each outer lane, which is what stops a rail from floating over the drop.
BRIDGES = {"n": ("z", (6, 7, 8, 9, 10), (8,)), "s": ("z", (20, 19, 18, 17, 16), (18,)), "w": ("x", (5, 6, 7, 8, 9, 10), (7, 8)), "e": ("x", (21, 20, 19, 18, 17, 16), ())}


@room("cloud_chasm")
def cloud_chasm():
    h = Hall("mega/trap/sky_citadel_cloud_chasm", 3, 3, 18, wear=0.05)
    for side in ("n", "s", "e", "w"):
        h.door(side, 13)
    top = h.top                                              # 26
    rng = h.rng
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    chasm = {(x, z) for z in range(2, 25) for x in range(2, 25) if ((x - 13) / CHASM_RX) ** 2 + ((z - 13) / CHASM_RZ) ** 2 <= 1.0}
    deck = {}
    for side, (axis, along, missing) in BRIDGES.items():
        for a in along:
            for lane in (12, 13, 14):
                deck[(lane, a) if axis == "z" else (a, lane)] = (side, a, lane, a in missing)
    cavity = chasm - ISLAND
    # ---- the drop: the floor of the hall is taken out over the chasm (the planks of the bridges stay), the walls and the floor of cloud go in under it
    for (x, z) in sorted(cavity):
        h.carve(x, FLOOR, z)

    def cloud(x, z):
        r = rng.random()
        return "minecraft:sea_lantern" if r < 0.10 else ("minecraft:snow_block" if r < 0.30 else WOOL)

    K.sunken_lining_cells(h, chasm, 1, block=L.BRICK, floor_fn=cloud)
    # the island hangs on a cone of stone that ends short of the cloud, as if it were the last thing left of a tower
    for k in range(5):
        r = 2 - k // 2
        for z in range(13 - r, 13 + r + 1):
            for x in range(13 - r, 13 + r + 1):
                if k % 2 == 1 and abs(x - 13) == r and abs(z - 13) == r and r > 0:
                    continue
                h.set(x, 7 - k, z, D.brick_mix(h, 0.15, 0.4, L.BRICK))
    # ---- the island: a floor of straw like a nest, the lure chest on a plinth, bones of the ones who came before, a storm mast on each corner
    for (x, z) in sorted(ISLAND):
        if max(abs(x - 13), abs(z - 13)) == 2:
            h.floor_set(x, z, "minecraft:hay_block", {"axis": "y"})
    for (x, z) in ((11, 11), (15, 11), (11, 15), (15, 15)):
        for k in range(3):
            h.set(x, GROUND + k, z, L.POLISHED_WALL)
        h.set(x, GROUND + 3, z, "minecraft:lightning_rod", {"facing": "up", "powered": "false", "waterlogged": "false"})
    h.set(13, GROUND, 13, L.POLISHED)
    h.loot_chest(13, GROUND + 1, 13, "south", K.LOOT_TRIAL)
    h.set(12, GROUND, 14, "minecraft:bone_block")
    h.set(14, GROUND, 12, "minecraft:skeleton_skull", {"rotation": "4"})
    D.candles_at(h, 12, GROUND, 12, 3, "white")
    D.candles_at(h, 14, GROUND, 14, 3, "white")
    # ---- the bridges: planks across the hall's floor row, rails on the outer lanes, a lamp post where the rail begins, a plank or two gone
    for side, (axis, along, missing) in BRIDGES.items():
        started = {12: False, 14: False}
        for a in along:
            for lane in (12, 13, 14):
                x, z = (lane, a) if axis == "z" else (a, lane)
                if a in missing:
                    continue
                h.shell_set(x, FLOOR, z, L.PLANKS if (a + lane) % 5 else L.PLANKS_SLAB, None if (a + lane) % 5 else {"type": "top"})
                if lane in started and h.can_set(x, GROUND, z, L.FENCE):
                    if not started[lane]:
                        K.lamp_post(h, x, z, height=2)
                        started[lane] = True
                    else:
                        h.try_set(x, GROUND, z, L.FENCE)
    # ---- the ladders out: one on the west rim and one on the east, up the wall of the chasm
    zw, ze = 9, 17
    xw = min(x for (x, z) in cavity if z == zw)
    xe = max(x for (x, z) in cavity if z == ze)
    for y in range(2, FLOOR + 1):
        h.set(xw, y, zw, "minecraft:ladder", {"facing": "east", "waterlogged": "false"})
        h.set(xe, y, ze, "minecraft:ladder", {"facing": "west", "waterlogged": "false"})
    # ---- the ledge: a polished kerb along the edge, columns on the four corners, banners and tall windows on the walls
    for (x, z) in sorted(chasm):
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if h.inside(nx, nz) and (nx, nz) not in chasm and (nx, nz) not in ISLAND:
                h.floor_set(nx, nz, L.POLISHED)
    for (x, z) in ((3, 3), (23, 3), (3, 23), (23, 23)):
        D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 15, cap=L.CHISELED)
    for w in (wn, ws):
        K.band_of_windows(w, [5, 21], GROUND + 3, GROUND + 14, SKY, width=5)
    for w in (ww, we):
        K.band_of_windows(w, [5, 21], GROUND + 3, GROUND + 14, SKY, width=5)
        for lat in (9, 17):
            for y in range(GROUND + 5, GROUND + 11):
                if w.can(lat, y, 0) and w.backed(lat, y, 0):
                    w.set(lat, y, 0, "minecraft:%s_wall_banner" % ("light_blue" if lat == 9 else "white"), {"facing": w.into})
    # ---- the roof: a ring of coloured glass over the chasm with a solid heart for the chandelier to hang from
    for z in range(2, 25):
        for x in range(2, 25):
            if ((x - 13) / (CHASM_RX - 1)) ** 2 + ((z - 13) / (CHASM_RZ - 1) ** 1) ** 2 > 1.0 or max(abs(x - 13), abs(z - 13)) <= 2:
                continue
            rib = x % 4 == 1 or z % 4 == 1
            h.shell_set(x, top + 1, z, L.POLISHED if rib else K.glass(SKY[(x // 3 + z // 3) % 4][(x + z) % 4]))
            h.shell_set(x, top + 2, z, L.POLISHED if rib else "minecraft:sea_lantern")
    K.wagon_chandelier(h, 13, 13, top, 9, r=3, color="white", candles=False, rods=True)
    h.faction("beast", 4, 4)
    K.auto_light(h)
    D.auto_markers(h)
    return h


COPPER_CUT = "minecraft:waxed_oxidized_cut_copper"
BULB = "minecraft:waxed_copper_bulb"
ROD = "minecraft:lightning_rod"


def slit(h, cx, cz, r, deg, y0, y1, colors):
    """A tall narrow window in the face of a round wall: the first solid cell along the ray from the middle at angle `deg` becomes glass in bands of `colors`, the
    cell behind it a sea lantern. The mass behind the face is solid, so the slit is a strip of lit glass and never an opening onto the void. Skipped (returns 0)
    when the face is not found, lies in a door's apron, or has no mass behind it. Returns the number of glass cells."""
    face = back = None
    prev = None
    k = r - 1.5
    while k < r + 4.0:
        cell = polar(cx, cz, k, deg)
        if cell != prev:
            if face is None:
                if h.inside(*cell) and not h.free(cell[0], GROUND + 2, cell[1]):
                    face = cell
            elif cell != face:
                back = cell
                break
            prev = cell
        k += 0.1
    if face is None or back is None or not h.inside(*back):
        return 0
    n = 0
    for y in range(y0, y1 + 1):
        if any(h._in_volume((x, y, z)) for (x, z) in (face, back)) or h.free(face[0], y, face[1]) or h.free(back[0], y, back[1]):
            continue
        h.set(face[0], y, face[1], K.glass(colors[((y - y0) // 2) % len(colors)]), over=True)
        h.set(back[0], y, back[1], "minecraft:sea_lantern", over=True)
        n += 1
    return n


@room("storm_spire")
def storm_spire():
    h = Hall("mega/set/sky_citadel_storm_spire", 3, 3, 34, wear=0.04)
    for side in ("n", "s", "e", "w"):
        h.door(side, 13)
    top = h.top                                              # 42
    c, R = 13, 11.3
    round_rows = GROUND + 22                                 # the drum stands to here, the dome closes over it
    K.round_wall(h, c, c, R, top)
    # ---- the floor: a storm rose. Copper hub round the pillar, eight copper spokes to the walls (the four doors lie on four of them), two sky-blue rings
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if d > R:
                continue
            ang = (math.degrees(math.atan2(z - c, x - c)) + 360.0) % 360.0
            off = min(abs(((ang + 22.5) % 45.0) - 22.5), 45.0)
            if d < 2.7:
                blk = COPPER_CUT
            elif 4.6 <= d < 5.6 or 9.0 <= d < 10.0:
                blk = L.CHISELED
            elif d >= 2.7 and math.radians(off) * d < 0.75:
                blk = COPPER_CUT
            else:
                blk = L.TILE if (x + z) % 2 else L.POLISHED
            h.floor_set(x, z, blk)
    # ---- the storm pillar: three by three, copper with a band of lit bulbs every fifth row, up to the heart
    for y in range(GROUND, GROUND + 23):
        for z in (12, 13, 14):
            for x in (12, 13, 14):
                h.set(x, y, z, BULB if (y - GROUND) % 5 == 4 else COPPER_CUT, {"lit": "true", "powered": "false"} if (y - GROUND) % 5 == 4 else None)
    # the heart: a cube of sea lanterns in a cage of pale glass, five lightning rods standing from it, one on each corner of the cage
    for y in range(GROUND + 23, GROUND + 28):
        for z in range(11, 16):
            for x in range(11, 16):
                edge = max(abs(x - c), abs(z - c)) == 2 or y in (GROUND + 23, GROUND + 27)
                h.set(x, y, z, K.glass("light_blue") if edge else "minecraft:sea_lantern")
    for y in range(GROUND + 28, GROUND + 33):
        h.set(c, y, c, ROD, {"facing": "up", "powered": "false", "waterlogged": "false"})
    for (x, z) in ((11, 11), (15, 11), (11, 15), (15, 15)):
        h.set(x, GROUND + 28, z, ROD, {"facing": "up", "powered": "false", "waterlogged": "false"})
    # ---- the two gantries round the pillar, a plank deck with a rail on its outer edge; a ladder up the south face to the first, one up the north face to the second
    def deck(row, d0, d1, ladder_cell):
        for z in range(c - d1, c + d1 + 1):
            for x in range(c - d1, c + d1 + 1):
                dd = max(abs(x - c), abs(z - c))
                if d0 <= dd <= d1 and (x, z) != ladder_cell:
                    h.set(x, row, z, L.PLANKS if (x + z) % 3 else L.LOG, None if (x + z) % 3 else {"axis": "x" if x % 2 else "z"})
                    if dd == d1:
                        h.set(x, row + 1, z, L.FENCE)
    deck(GROUND + 9, 2, 4, (13, 15))
    deck(GROUND + 18, 2, 4, (13, 11))
    for y in range(GROUND, GROUND + 10):
        h.set(13, y, 15, "minecraft:ladder", {"facing": "south", "waterlogged": "false"})
    for y in range(GROUND + 10, GROUND + 19):
        h.set(13, y, 11, "minecraft:ladder", {"facing": "north", "waterlogged": "false"})
    h.set(13, GROUND + 11, 17, L.LANTERN, {"hanging": "false"})
    h.loot_chest(10, GROUND + 19, 10, "east")
    D.candles_at(h, 10, GROUND + 19, 11, 3, "white")
    D.candles_at(h, 11, GROUND + 19, 10, 3, "light_blue")
    # ---- the wall: eight pillars between the doors, a cornice under the dome, slits of coloured glass in between
    for k in range(8):
        px, pz = polar(c, c, 9.4, 22.5 + 45 * k)
        D.column(h, px, pz, w=1, y0=GROUND, y1=round_rows - 2, cap=L.CHISELED)
    for k in range(16):
        slit(h, c, c, R, 11.25 + 22.5 * k, GROUND + 6, GROUND + 19, SKY[k % 4])
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            if math.hypot(x - c, z - c) <= R and h.free(x, round_rows - 1, z):
                near = [(x + dx, z + dz) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)) if not h.free(x + dx, round_rows - 1, z + dz)]
                if near and not h._in_volume((x, round_rows - 1, z)):
                    nx, nz = near[0]
                    face = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(nx - x, nz - z)]
                    h.set(x, round_rows - 1, z, L.POLISHED_STAIRS, {"facing": D.OPP[face], "half": "top"})
    K.dome(h, c, c, R, round_rows, top, r_top=5.5)
    # ---- the oculus: glass over the top of the dome, ribs on the eight spokes
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if d <= 5.5 and h.free(x, top, z):
                ang = (math.degrees(math.atan2(z - c, x - c)) + 360.0) % 360.0
                rib = min(abs(((ang + 22.5) % 45.0) - 22.5), 45.0) * math.pi / 180.0 * d < 0.6 or d > 4.6
                h.shell_set(x, top + 1, z, L.POLISHED if rib else K.glass(SKY[int(ang // 45) % 4][int(d) % 4]))
                h.shell_set(x, top + 2, z, L.POLISHED if rib else "minecraft:sea_lantern")
    for k in range(4):
        bx, bz = polar(c, c, 7.2, 45 + 90 * k)
        D.brazier(h, bx, bz)
    h.faction("elemental", 6, 13)
    K.auto_light(h, max_lamps=200)
    D.auto_markers(h)
    return h


def stock(h, kind, xs, zs, row):
    """What stands on one shelf board of the pantry: the compartment is the cells xs x zs on the two rows over the board at `row`. Everything the giant keeps: barrels
    (two high, lids up), sacks of grain (hay) and flour (white wool), jars (decorated pots), a wheel of cheese (honeycomb), a bolt of cloth, or nothing (`empty`:
    the compartments a climber steps into and the ones that hold a chest)."""
    rng = h.rng
    cells = [(x, z) for z in zs for x in xs]
    if kind == "barrels":
        for (x, z) in cells:
            if rng.random() < 0.7:
                for dy in (1, 2):
                    h.try_set(x, row + dy, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    elif kind == "sacks":
        for (x, z) in cells:
            r = rng.random()
            if r < 0.3:
                h.try_set(x, row + 1, z, "minecraft:hay_block", {"axis": "y"})
                if r < 0.15:
                    h.try_set(x, row + 2, z, "minecraft:hay_block", {"axis": "y"})
            elif r < 0.55:
                h.try_set(x, row + 1, z, WOOL)
    elif kind == "jars":
        for (x, z) in cells:
            if rng.random() < 0.45:
                h.try_set(x, row + 1, z, "minecraft:decorated_pot", {"facing": rng.choice(("north", "south", "east", "west")), "cracked": "false", "waterlogged": "false"})
                if rng.random() < 0.35:
                    h.try_set(x, row + 2, z, "minecraft:decorated_pot", {"facing": "south", "cracked": "false", "waterlogged": "false"})
    elif kind == "cheese":
        for z in list(zs)[:3]:
            for x in xs:
                h.try_set(x, row + 1, z, "minecraft:honeycomb_block")
            h.try_set(sorted(xs)[len(xs) // 2], row + 2, z, "minecraft:honeycomb_block")
    elif kind == "cloth":
        for (x, z) in cells:
            if rng.random() < 0.6:
                h.try_set(x, row + 1, z, "minecraft:%s_wool" % rng.choice(("light_blue", "white", "cyan", "light_gray")))


#: the two blocks of shelving: (cells from the aisle face to the wall, which way a ladder or a chest on them faces, the z of the posts, the first cell of the bays
#: that carry a ladder, the bays (by index) and board rows (by index) that hold a chest instead of stock, the z of that chest)
PANTRY_BLOCKS = (
    ((4, 3, 2), "east", (7, 12, 17, 22), (13, 18), ((1, 1, 15), (2, 2, 20))),
    ((13, 14, 15), "west", (3, 8, 13, 18), (9,), ((1, 1, 11),)),
)


@room("pantry")
def pantry():
    h = Hall("mega/treasure/sky_citadel_pantry", 2, 3, 16, wear=0.05)
    h.door("n", 4)
    h.door("s", 13)
    top = h.top                                              # 24
    rng = h.rng
    wn, ws = h.wall("n"), h.wall("s")
    rows = (GROUND + 3, GROUND + 7, GROUND + 11)
    kinds = ["barrels", "sacks", "jars", "cheese", "cloth", "barrels", "jars", "sacks", "barrels"]
    rng.shuffle(kinds)
    n = 0
    # ---- the shelving: posts floor to ceiling at both faces, three boards between them. A ladder runs up the first cell of the front column of some bays (its
    # support is a log in the middle column); the compartment next to it, and the one that holds a chest, are left empty so a climber can step off and open it.
    for xs, toward, posts, ladders, chests in PANTRY_BLOCKS:
        front, mid, back = xs
        for zp in posts:
            for y in range(GROUND, GROUND + 14):
                h.set(front, y, zp, L.LOG, {"axis": "y"})
                h.set(mid, y, zp, L.PLANKS)
                h.set(back, y, zp, L.LOG, {"axis": "y"})
        for z in ladders:
            for y in range(GROUND, rows[2] + 1):
                h.set(front, y, z, "minecraft:ladder", {"facing": toward, "waterlogged": "false"})
                if y not in rows:                            # the boards make the support full at their own rows
                    h.set(mid, y, z, L.LOG, {"axis": "y"})
        for k, (z0, z1) in enumerate(zip(posts, posts[1:])):
            for z in range(z0 + 1, z1):
                for r in rows:
                    for x in xs:
                        if not (x == front and z in ladders):
                            h.set(x, r, z, L.PLANKS)
            for ri, r in enumerate(rows):
                held = any(bk == k and rk == ri for (bk, rk, _z) in chests)
                kind = "empty" if held or ((z0 + 1) in ladders and ri != 0) else kinds[n % len(kinds)]
                n += 1
                if kind != "empty":
                    stock(h, kind, xs, range(z0 + 1, z1), r)
        for (bk, rk, zc) in chests:
            h.loot_chest(front, rows[rk] + 1, zc, toward)
            D.candles_at(h, mid, rows[rk] + 1, zc, 3, "yellow")
    h.loot_chest(3, GROUND, 24, "east")
    h.loot_chest(14, GROUND, 2, "west")
    # ---- the roof: beams across, hams and sausages on chains over the aisle, lanterns between them
    D.beams(h, "x", [7, 13, 19], y=top, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS)
    for z in (7, 13, 19):
        for x in (7, 9, 11):
            if (x + z) % 2:
                continue
            for y in (top - 1, top - 2):
                h.try_set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
            h.try_set(x, top - 3, z, "minecraft:red_terracotta")
            h.try_set(x, top - 4, z, "minecraft:orange_terracotta")
    for (x, z) in ((8, 10), (10, 16)):
        D.hang_lantern(h, x, z, 6)
    # ---- the aisle: a runner of light-blue carpet from door to door, two high windows on the end walls
    for z in range(2, 25):
        for x in (8, 9):
            h.try_set(x, GROUND, z, "minecraft:light_blue_carpet")
    K.lancet(wn, 10, GROUND + 5, GROUND + 12, SKY[1], width=3)
    K.lancet(ws, 7, GROUND + 5, GROUND + 12, SKY[2], width=3)
    h.faction("ogre", 9, 12)
    K.auto_light(h)
    D.auto_markers(h, spawns=3, patrols=2)
    return h


def line_cells(x0, z0, x1, z1):
    """The cells of a line between two cells, ends included, every cell a side neighbour of the next (never only a corner neighbour): the path a row of bars or a
    chain of lamps needs, because bars join up by their sides only."""
    dx, dz = abs(x1 - x0), abs(z1 - z0)
    sx, sz = (1 if x1 > x0 else -1), (1 if z1 > z0 else -1)
    x, z, ix, iz = x0, z0, 0, 0
    cells = [(x, z)]
    while ix < dx or iz < dz:
        if iz >= dz or (ix < dx and (ix + 0.5) * dz < (iz + 0.5) * dx):
            x += sx
            ix += 1
        else:
            z += sz
            iz += 1
        cells.append((x, z))
    return cells


@room("thunder_court")
def thunder_court():
    h = Hall("mega/trial/sky_citadel_thunder_court", 3, 3, 16, wear=0.03)
    h.door("n", 4)
    h.door("s", 22)
    h.no_spawn = True
    top = h.top                                              # 24
    c = 13
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # ---- the floor: a slate disc with two copper rings, a copper hub under the keystone, eight glyphs where the storm will come from
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            d = math.hypot(x - c, z - c)
            if d < 1.6:
                h.floor_set(x, z, L.CHISELED)
            elif 5.4 <= d < 6.4 or 9.6 <= d < 10.6:
                h.floor_set(x, z, COPPER_CUT)
            elif d < 9.6:
                h.floor_set(x, z, L.POLISHED if (x + z) % 2 else L.TILE)
    glyphs = [polar(c, c, 3.6, 45 * k) for k in range(8)]
    K.trial_core(h, [("minecraft:breeze", 2), ("minecraft:vex", 3)], glyphs, radius=13)
    # ---- eight masts of copper on the outer ring, each with a lit bulb and a lightning rod, and a rim of bars between neighbours at the height of the bulbs
    masts = [polar(c, c, 8.0, 22.5 + 45 * k) for k in range(8)]
    for (x, z) in masts:
        D.column(h, x, z, w=1, y0=GROUND, y1=GROUND + 9, shaft=COPPER_CUT, cap="minecraft:waxed_oxidized_chiseled_copper")
        h.set(x, GROUND + 5, z, BULB, {"lit": "true", "powered": "false"}, over=True)
        for dy in (10, 11, 12):
            h.set(x, GROUND + dy, z, ROD, {"facing": "up", "powered": "false", "waterlogged": "false"})
    for (a, b) in zip(masts, masts[1:] + masts[:1]):
        for (x, z) in line_cells(a[0], a[1], b[0], b[1])[1:-1]:
            h.try_set(x, GROUND + 7, z, L.IRON_BARS)
    # ---- the storm altar in the west wall: three steps up to a plinth with the chest, candles in blue
    for z in range(11, 16):
        for k, x in enumerate((4, 3, 2)):
            for y in range(GROUND, GROUND + k + 1):
                h.set(x, y, z, L.COBBLED if y < GROUND + k else (L.POLISHED if (x + z) % 2 else L.TILE))
    h.loot_chest(2, GROUND + 3, 13, "east")
    for z in (12, 14):
        D.candles_at(h, 2, GROUND + 3, z, 3, "light_blue")
    # ---- the roof: a bolt of light across the ceiling (white glass over sea lanterns, two cells wide), four small wheels of lanterns between its turns
    bolt = [(3, 3), (10, 9), (8, 13), (17, 15), (14, 20), (23, 23)]
    cells = set()
    for (a, b) in zip(bolt, bolt[1:]):
        for (x, z) in line_cells(a[0], a[1], b[0], b[1]):
            cells.update({(x, z), (x + 1, z)})
    for (x, z) in sorted(cells):
        if h.inside(x, z) and not (abs(x - c) <= 1 and abs(z - c) <= 1):
            h.shell_set(x, top + 1, z, K.glass("white" if (x + z) % 3 else "light_blue"))
            h.shell_set(x, top + 2, z, "minecraft:sea_lantern")
    for (x, z) in ((6, 20), (20, 6), (6, 6), (20, 20)):
        K.wagon_chandelier(h, x, z, top, 5, r=1, color="white", candles=False, rods=True)
    # ---- the walls: arches of bars in front of the glare of the sky, banners between them
    for w, lats in ((wn, (13, 22)), (ws, (4, 13)), (ww, (6, 20)), (we, (6, 13, 20))):
        for lat in lats:
            K.bar_window(w, lat, GROUND + 1, GROUND + 11, width=5)
    for w, lats in ((ww, (9, 17)), (we, (9, 17))):
        for lat in lats:
            for y in range(GROUND + 5, GROUND + 11):
                if w.can(lat, y, 0) and w.backed(lat, y, 0):
                    w.set(lat, y, 0, "minecraft:%s_wall_banner" % ("light_blue" if lat == 9 else "white"), {"facing": w.into})
    h.faction("elemental", 4, 20)
    K.auto_light(h)
    D.auto_markers(h, spawns=0, patrols=2, centre=False)
    return h


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, sys.argv[1:], __doc__.split("\n\n")[0]))

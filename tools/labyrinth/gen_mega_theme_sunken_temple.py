#!/usr/bin/env python3
"""
Why: THEME-B (1.59.0, docs/labyrinth/mega_v2_thb.md section 4) gives the Holy road the temple of a forgotten good god that the sea took back: prismarine walls, dark prismarine floors,
sea lanterns where the keystones were (the god's light still burns), canals and basins of knee-deep water that the party wades through, drowned shrines with their offerings, a well that
a party can fall into and climb out of, a set piece where the tide meets at the heart of the temple and a sluice that is a puzzle. This script writes its rooms
(`structure/mega/<folder>/sunken_temple_*.nbt`, the prefix `data/nusmp/mega/template_themes/sunken_temple.json` claims for the theme), so only a sector of the Sunken Temple theme ever
draws them.

    python3 tools/labyrinth/gen_mega_theme_sunken_temple.py                       # builds, checks and writes every room (a room with a defect is not written)
    python3 tools/labyrinth/gen_mega_theme_sunken_temple.py coral_nave --preview    # one room, then render it in its own theme into $THB_PREVIEWS
    python3 tools/labyrinth/gen_mega_theme_sunken_temple.py --check               # CI style: builds in memory, checks, compares with the files on disk

Architecture uses the palette constants of mega_lib (the Region Theme turns them into prismarine bricks, dark prismarine, prismarine, sea lanterns for the chiselled caps, mossy stone and
acacia); the character of the temple is the free vanilla blocks on top: sealed water (every water cell has a full block on four sides and below, so it cannot flow out of its basin and no
doorway is ever wet), live coral in the water and dead coral on the dry walls, sea pickles, seagrass, vines and glow lichen, soul lanterns for the blue light that is not a sea lantern.
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_decor_r2 as D2  # noqa: E402
import gen_mega_rooms2 as R2  # noqa: E402
import mega_theme_kit as K  # noqa: E402
from mega_lib import MT, Frame  # noqa: E402
from mega_decor_r1 import Hall, GROUND, FLOOR  # noqa: E402

T = K.Theme("sunken_temple")
FR = L.FLOOR_ROW

WATER = "minecraft:water"
SEA = "minecraft:sea_lantern"
CORALS = ("tube", "brain", "bubble", "fire", "horn")
FACING_STEP = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0)}


# ===================================================================================================================
# the props of the temple
# ===================================================================================================================
def pool(h, cells, depth=1, bottom=L.TILE, coral=0.0, glow=0.0, rng=None):
    """A sealed basin: the floor cells of `cells` [(x, z)] become `depth` rows of water (the top one at the floor's own row) over a bottom of `bottom` (or, with probability `coral`, a live
    coral block), and the cells beside the basin get a wall of floor blocks under the rim down to the bottom, so that every water cell has a full block on all four sides and below. Why:
    a source block flows into any open side, so a basin is a wall first and water second (`mega_decor_r2.Analysis.fluid_problems` checks every cell); the doorways must never be wet,
    so the caller keeps the basin two cells away from every doorway hole. Returns the set of cells."""
    rng = rng or h.rng
    cellset = set(cells)
    for (x, z) in cells:
        if h.get(x, FLOOR, z) is not None:
            h.carve(x, FLOOR, z)
        for k in range(depth):
            h.set(x, FLOOR - k, z, WATER, {"level": "0"})
        bottom_block = bottom
        r = rng.random()
        if glow and r < glow:
            bottom_block = SEA
        elif coral and r < glow + coral:
            bottom_block = "minecraft:%s_coral_block" % rng.choice(CORALS)
        h.set(x, FLOOR - depth, z, bottom_block)
    for (x, z) in cells:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in cellset:
                continue
            for y in range(FLOOR - depth, FLOOR):
                if h.get(n[0], y, n[1]) is None and (n[0], y, n[1]) not in h.mt.reserved:
                    h.set(n[0], y, n[1], bottom)
    return cellset


def lily_pads(h, cells, p=0.30, rng=None):
    """Lily pads on the water of a basin: on each water cell of `cells` with probability `p`, in the cell over it (the pad needs water under it, the cell over it is free air). Returns
    the number placed. Why a pad and not a coral: a waterlogged plant beside a plain water cell is a block that is neither water nor a full block, which `mega_arch_check` reads as
    an open side (a false alarm: the game keeps its water), so the live corals stand in planters of their own (`planter`) and the basins hold only water, glowing floors and pads."""
    rng = rng or h.rng
    n = 0
    for (x, z) in cells:
        c = h.get(x, FLOOR, z)
        if c is not None and c.block == WATER and rng.random() < p and h.try_set(x, GROUND, z, "minecraft:lily_pad"):
            n += 1
    return n


def planter(h, x, z, rng=None, kind=None):
    """A planter in the dry floor: one floor cell becomes a live coral (a plant, a fan or a sea pickle, waterlogged: it carries its own water, its top level with the floor) over a block
    of live coral, with plain floor on all four sides, so the water cannot flow and the coral does not die. The caller keeps `planter` away from basins and doorways (a planter beside plain water reads as an open side).
    Returns False when a cell is taken."""
    rng = rng or h.rng
    r = rng.random()
    coral = kind or rng.choice(CORALS)
    if r < 0.45:
        blk, props = "minecraft:%s_coral" % coral, {"waterlogged": True}
    elif r < 0.75:
        blk, props = "minecraft:%s_coral_fan" % coral, {"waterlogged": True}
    else:
        blk, props = "minecraft:sea_pickle", {"pickles": str(rng.randint(2, 4)), "waterlogged": True}
    if h.get(x, FLOOR, z) is None or not h.can_set(x, GROUND, z, blk, props):
        return False
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        c = h.get(x + dx, FLOOR, z + dz)
        if c is None or c.block in (WATER,) or c.block.endswith("_coral_block") and c.block.startswith("minecraft:") and (x + dx, GROUND, z + dz) in h.deco:
            return False
    h.carve(x, FLOOR, z)
    h.set(x, FLOOR - 1, z, "minecraft:%s_coral_block" % coral)
    h.set(x, FLOOR, z, blk, props)
    return True


def dead_coral_wall(w, lat, y, rng=None, kind=None):
    """A dead coral fan on the wall face in front of the wall layer at (lat, y): the sea went and left the coral grey. Needs the wall behind it to be a full block."""
    h = w.hall
    rng = rng or h.rng
    kind = kind or rng.choice(CORALS)
    if not w.free(lat, y, 0) or not w.backed(lat, y, 0):
        return False
    h.set(*w.cell(lat, y, 0), "minecraft:dead_%s_coral_wall_fan" % kind, {"facing": w.into, "waterlogged": False})
    return True


def hang_soul(h, x, z, drop, y_top=None):
    """A soul lantern (the blue light the sea lanterns do not give) on a chain from the ceiling or a beam over (x, z), `drop` cells long. False when the column is taken."""
    for yt in ((h.top if y_top is None else y_top), (h.top - 1 if y_top is None else y_top - 1)):
        above = h.get(x, yt + 1, z)
        if above is None or not D.center_support(above.block, above.props, "down"):
            continue
        if all(h.can_set(x, yt - k, z, L.CHAIN_BLOCK) for k in range(drop + 1)):
            return K.hang_chain_light(h, x, z, yt, drop, soul=True)
    return False


def vines(h, p=0.10, rows=None, sides="nsew", door_gap=3):
    """Vines on the walls of the dry parts (the temple is green where it is wet): `p` of the wall faces in `rows` (default the upper half of the wall)."""
    rows = rows or range(GROUND + 5, h.top)
    n = 0
    for side in sides:
        w = h.wall(side)
        lo = (h.x0 if side in ("n", "s") else h.z0) + 1
        hi = (h.x1 if side in ("n", "s") else h.z1) - 1
        doors = [off for (sd, off) in h.doors if sd == side]
        for lat in range(lo, hi + 1):
            if any(abs(lat - off) <= 1 + door_gap for off in doors):
                continue
            for y in rows:
                if h.rng.random() < p and w.free(lat, y, 0) and w.backed(lat, y, 0):
                    w.set(lat, y, 0, "minecraft:vine", {w.out: True})
                    n += 1
    return n


# ===================================================================================================================
# HALL: the coral nave (2 x 5 modules)
# ===================================================================================================================
@T.room("hall", "coral_nave", faction="aquatic", view_h=14,
        cuts=[{"name": "west_half", "x": (0, 9), "y": (0, 24), "z": None}],
        views=[("door0", (13.5, 10.62, 3.5), (8.5, 11.0, 30.0)), ("mid", (8.5, 10.62, 12.0), (8.5, 12.0, 38.0)), ("canal", (4.5, 10.62, 9.0), (4.5, 9.0, 18.0)),
               ("crossing", (8.5, 10.62, 20.0), (4.0, 11.0, 22.0))])
def coral_nave(path):
    """Why: the nave of the temple is what a party sees first: a dry causeway six wide runs the length of the hall between two rows of prismarine pillars (a band of sea lantern round each),
    and either side of it a canal of knee-deep water, four basins in all, a live reef on their floors (corals, fans, sea pickles, seagrass), the light of the reef coming up through the
    water. A cross aisle (dry, the full width) is where the side doors come in. Vines hang on the walls over dead coral fans (the sea went out and left the grey ones), blue soul lanterns
    hang on chains, and the pillars carry the roof's beams. The causeway is the three-wide route; the canals are the places things come out of (drowned, guardians' kin: the aquatic
    faction), and the wading party is slowed by them while the enemy is not. The doors are north, south and one in the middle of each long wall."""
    h = Hall(path, 2, 5, 14, wear=0.04)
    top = h.top
    rng = h.rng
    h.door("n", 13)
    h.door("s", 4)
    h.door("w", 22)
    h.door("e", 22)
    h.route(7, 2, 10, 42, label="the causeway")
    h.route(2, 20, 15, 24, label="the cross aisle")
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    # 1. the canals: four basins, one deep (shallow wading) with a reef floor; the cross aisle and the two end vestibules stay dry
    basins = []
    for (z0, z1) in ((8, 18), (26, 36)):
        for (x0, x1) in ((3, 5), (12, 14)):
            cells = [(x, z) for z in range(z0, z1 + 1) for x in range(x0, x1 + 1)]
            pool(h, cells, depth=1, coral=0.40, glow=0.22)
            basins.append(cells)
    for cells in basins:
        lily_pads(h, cells, p=0.22)
    # 2. the pillars along the causeway (x = 6 and 11), a band of chiselled stone (a sea lantern in the theme) at two heights, the roof beams between them
    for z in (9, 14, 17, 27, 30, 35):
        for x in (6, 11):
            if not h.can_set(x, GROUND, z):
                continue
            for y in range(GROUND, top):
                h.set(x, y, z, L.CHISELED if y in (GROUND + 3, GROUND + 8) else L.POLISHED)
            h.set(x, top, z, L.CHISELED)
    D.beams(h, "x", [9, 17, 27, 35], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    # the live reef in planters along the causeway (not beside a basin), lanterns of the reef: a sea pickle gives light 15 with four pickles
    for z in range(4, 41, 3):
        for x in (7, 10):
            if 6 < z < 38 and (z + x) % 2 == 0 or z in (4, 40):
                planter(h, x, z, rng)
    # 3. the walls: dead coral fans, vines, a flush sea lantern here and there
    for wl in (ww, we):
        for lat in range(4, 41, 2):
            if abs(lat - 22) > 4 and rng.random() < 0.55:
                dead_coral_wall(wl, lat, GROUND + rng.choice((1, 2, 3, 4)))
    vines(h, p=0.07, sides="we")
    vines(h, p=0.07, sides="ns")
    K.wall_lights(h, [SEA], spacing=6, y=GROUND + 4, sides="we", door_gap=3)
    for z in (6, 12, 24, 32, 38):
        for x in (4, 13):
            hang_soul(h, x, z, 7)
    for door in (("n", 13), ("s", 4), ("w", 22), ("e", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    h.faction("aquatic", 8, 24)
    D.auto_light(h, soul=True, max_lamps=16)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ===================================================================================================================
# HALL: the drowned shrines (3 x 3 modules)
# ===================================================================================================================
def acolyte(h, x, y, z, facing, mainhand="minecraft:trident"):
    """A drowned acolyte: an armor stand in a turtle shell helmet with a trident (a statue; it is scenery, the creatures of the place are the roster of the encounter table)."""
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
    h.armor_stand(x, y, z, yaw=yaw, head="minecraft:turtle_helmet", chest=None, legs=None, feet=None, mainhand=mainhand)


@T.room("hall", "drowned_shrines", faction="aquatic", view_h=14,
        cuts=[{"name": "west_half", "x": (0, 14), "y": (0, 24), "z": None}],
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 11.0, 14.0)), ("door1", (23.5, 10.62, 13.5), (13.5, 11.0, 13.5)), ("shrine", (13.5, 10.62, 8.0), (5.0, 11.0, 2.0)),
               ("god", (13.5, 10.62, 20.0), (13.5, 12.0, 13.5))])
def drowned_shrines(path):
    """Why: the temple's second hall is the one where the god was prayed to: a basin in the middle of the floor split in four by a dry cross (the god's statue on its plinth where the arms
    meet: an acolyte's shell helmet, a trident, a lantern at his feet), a ring of eight pillars with a band of sea lantern round each, and in the walls eight shrines, each a niche
    with an acolyte of the temple in it (armor stands in turtle shells with tridents), candles of white and pale blue and a coral fan gone grey. A disc of blue glass in the roof lit
    from above is the sea's surface, and the floor is a mosaic of faded coral. It is a fight in a circle: the cross is the way across, the pools slow whoever wades them (and the
    drowned come up out of them), the pillars are cover."""
    h = Hall(path, 3, 3, 14, wear=0.04)
    top = h.top
    rng = h.rng
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    c = 13
    # 1. the basin: a disc of radius 4.6 round the middle, split by a dry cross two cells wide... three, so a party crosses; four quarter pools, glowing floors, lily pads
    quarter = [(x, z) for (x, z) in K.disc(c, c, 4.6) if abs(x - c) >= 2 and abs(z - c) >= 2]
    pool(h, quarter, depth=1, coral=0.30, glow=0.25)
    lily_pads(h, quarter, p=0.25)
    # 2. the god on his plinth: a stepped plinth of prismarine, the acolyte with a trident, lanterns
    for (x, z) in K.disc(c, c, 1.2):
        h.set(x, GROUND, z, L.POLISHED)
    h.set(c, GROUND + 1, c, L.CHISELED)
    acolyte(h, c, GROUND + 2, c, "south", mainhand="minecraft:trident")
    for (x, z) in ((c - 1, c - 1), (c + 1, c - 1), (c - 1, c + 1), (c + 1, c + 1)):
        h.try_set(x, GROUND + 1, z, L.LANTERN if False else "minecraft:soul_lantern", {"hanging": False})
    # 3. the mosaic: rings of faded coral (dead coral blocks) in the dry floor
    ring_blocks = ["minecraft:dead_%s_coral_block" % k for k in CORALS]
    for z in range(2, 25):
        for x in range(2, 25):
            r = math.hypot(x - c, z - c)
            cell = h.get(x, FLOOR, z)
            if cell is None or cell.block == WATER or r < 1.5:
                continue
            if abs(r - 7.0) < 0.55 or abs(r - 10.0) < 0.55:
                h.floor_set(x, z, ring_blocks[int(K.angle(x, z, c, c) / 72.0) % 5])
            elif abs(r - 12.0) < 0.5:
                h.floor_set(x, z, L.POLISHED)
    # 4. the eight pillars on a ring (r = 8.5), a band of chiselled stone (a sea lantern in the theme) at two heights
    for k in range(8):
        px, pz = K.angle(0, 0, 0, 0), 0
        a = math.radians(22.5 + 45.0 * k)
        px, pz = int(round(c + 8.6 * math.cos(a))), int(round(c + 8.6 * math.sin(a)))
        if not h.can_set(px, GROUND, pz):
            continue
        for y in range(GROUND, top):
            h.set(px, y, pz, L.CHISELED if y in (GROUND + 3, GROUND + 8) else L.POLISHED)
        h.set(px, top, pz, L.CHISELED)
    # 5. the shrines: two niches in every wall (laterals 5 and 21), an acolyte in each, candles and a grey coral fan
    for k, side in enumerate("nesw"):
        wl = h.wall(side)
        for lat in (5, 21):
            D.niche(wl, lat, GROUND, GROUND + 4, width=3, arch=True)
            x, y, z = wl.cell(lat, GROUND, -1)
            acolyte(h, x, y, z, wl.into)
            for dl in (-1, 1):
                cx, cy, cz = wl.cell(lat + dl, GROUND, -1)
                D.candles_at(h, cx, cy, cz, 3, "white" if (k + dl) % 2 else "light_blue")
                fx, fy, fz = wl.cell(lat + dl, GROUND + 1, -1)
                if h.free(fx, fy, fz):
                    h.set(fx, fy, fz, "minecraft:dead_%s_coral_wall_fan" % rng.choice(CORALS), {"facing": wl.into, "waterlogged": False}) if False else None
            lx, ly, lz = wl.cell(lat, GROUND + 4, -1)
            if h.free(lx, ly, lz):
                h.set(lx, ly, lz, "minecraft:soul_lantern", {"hanging": True})
    # 6. the surface: a disc of blue glass in the roof lit from above; the corals in planters on the ring between the pools and the pillars
    for (x, z) in K.disc(c, c, 5.0):
        h.shell_set(x, top + 1, z, "minecraft:light_blue_stained_glass")
        h.shell_set(x, top + 2, z, SEA)
    for k in range(8):
        a = math.radians(45.0 * k)
        planter(h, int(round(c + 6.0 * math.cos(a))), int(round(c + 6.0 * math.sin(a))), rng)
    for k in range(12):                                                 # sea lanterns laid in the mosaic, two rings
        for (rr, off) in ((10.0, 0.0), (7.0, 15.0)):
            a = math.radians(30.0 * k + off)
            x, z = int(round(c + rr * math.cos(a))), int(round(c + rr * math.sin(a)))
            cell = h.get(x, FLOOR, z)
            if cell is not None and cell.block != WATER and h.free(x, GROUND, z) and (x, GROUND, z) not in h.mt.reserved:
                h.floor_set(x, z, SEA)
    for door in (("n", 13), ("e", 13), ("s", 13), ("w", 13)):
        D.door_trim(h, *door)
    D.cornice(h)
    vines(h, p=0.05)
    K.wall_lights(h, [SEA], spacing=5, y=GROUND + 3, door_gap=3)
    K.wall_lights(h, [SEA], spacing=5, y=GROUND + 8, door_gap=3, offset=1)
    h.faction("aquatic", 8, 8)
    D.auto_light(h, soul=True, max_lamps=24)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ===================================================================================================================
# HALL: the coral atrium (3 x 3 modules, 22 high, three levels: the well, the floor, the gallery)
# ===================================================================================================================
@T.room("hall", "coral_atrium", faction="aquatic", view_h=22,
        cuts=[{"name": "west_half", "x": (0, 14), "y": (0, 34), "z": None}, {"name": "north_half", "x": None, "y": (0, 34), "z": (0, 14), "rot": 1}],
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 12.0, 14.0)), ("door1", (23.5, 10.62, 13.5), (13.5, 12.0, 13.5)), ("gallery", (4.5, 17.62, 6.0), (13.5, 16.0, 13.5)),
               ("bridge", (13.5, 17.62, 5.5), (13.5, 17.0, 21.0)), ("well", (13.5, 10.62, 20.5), (13.5, 7.0, 13.5))])
def coral_atrium(path):
    """Why: the temple's grand room is a well. A basin eleven blocks across and three deep (the sea's own door into the temple: it glows from the floor, live coral and sea lanterns
    under the water) fills the middle of a square atrium twenty-two blocks high; a dry floor rings it; a gallery of prismarine, four wide and eight blocks up, runs round the walls on a
    colonnade, reached by two flights of stairs and crossed by a bridge over the well, so the room has three levels and a fall from the top lands in water (no harm: the well is deep
    enough to catch a party, and every edge of it is a step the swimmer climbs out by, so nobody is trapped). Vines hang from the gallery, the walls carry dead coral fans, blue soul
    lanterns hang from a wheel under a glass oculus lit from above. The party fights on the floor and on the gallery, and whatever is thrown into the well comes out wet."""
    h = Hall(path, 3, 3, 22, wear=0.04)
    top = h.top
    rng = h.rng
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    c = 13
    deck_row = 16
    # 1. the well: a disc of radius 4.6, three deep, the floor of coral and sea lanterns; the rim is the floor, a step up from the water (a swimmer climbs out anywhere)
    well = K.disc(c, c, 4.6)
    pool(h, well, depth=3, coral=0.35, glow=0.22)
    lily_pads(h, well, p=0.15)
    # 2. the gallery: a deck of dark prismarine six wide round the walls (cells whose Chebyshev distance from the middle is 6 or more) at the ninth row, cut where the two flights of
    #    stairs climb to it (a pinwheel: the west flight from the south, the east flight from the north), the strip beside each flight left open so the ground floor stays three wide
    zone_a = {(x, z) for x in range(2, 8) for z in range(15, 23)}
    zone_b = {(x, z) for x in range(19, 25) for z in range(4, 12)}
    deck = [(x, z) for z in range(2, 25) for x in range(2, 25) if max(abs(x - c), abs(z - c)) >= 6 and (x, z) not in zone_a and (x, z) not in zone_b]
    deckset = set(deck)
    for (x, z) in deck:
        h.set(x, deck_row, z, L.TILE if (x + z) % 7 else L.CRACKED_TILE)
    D.stair_flight(h, 2, 22, "north", 8, width=3, base=GROUND, stairs=L.POLISHED_STAIRS, rail_r=2, rail=L.POLISHED_WALL)
    D.stair_flight(h, 24, 4, "south", 8, width=3, base=GROUND, stairs=L.POLISHED_STAIRS, rail_r=2, rail=L.POLISHED_WALL)
    # the colonnade under the inner edge
    for (x, z) in ((7, 7), (19, 19), (10, 7), (16, 7), (10, 19), (16, 19), (7, 10), (19, 16)):
        if not h.can_set(x, GROUND, z):
            continue
        for y in range(GROUND, deck_row):
            h.set(x, y, z, L.CHISELED if y == GROUND + 5 else L.POLISHED)
    # 3. the bridge over the well: five wide, at the gallery's level between the north and the south gallery, a rail either side; the rail of the gallery's open edges (not at the mouths)
    bridge = {(x, z) for x in range(11, 16) for z in range(8, 19)}
    for (x, z) in sorted(bridge):
        h.set(x, deck_row, z, L.TILE)
    for (x, z) in sorted(bridge):
        if x in (11, 15):
            h.set(x, deck_row + 1, z, L.POLISHED_WALL)
    for z in range(9, 18, 3):                                              # a soul lantern on the rail post at every third cell of the bridge
        for x in (11, 15):
            h.set(x, deck_row + 2, z, "minecraft:soul_lantern", {"hanging": False})
    mouth = {(x, 7) for x in range(11, 16)} | {(x, 19) for x in range(11, 16)} | {(x, 14) for x in (2, 3, 4)} | {(x, 12) for x in (22, 23, 24)}
    for (x, z) in deck:
        if (x, z) in mouth:
            continue
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if 2 <= n[0] <= 24 and 2 <= n[1] <= 24 and n not in deckset and n not in bridge:
                h.try_set(x, deck_row + 1, z, L.POLISHED_WALL)
                break
    # 4. the oculus and the wheel of blue lanterns, the sea lanterns of the gallery, vines, dead coral, planters on the floor
    for (x, z) in K.disc(c, c, 3.5):
        h.shell_set(x, top + 1, z, "minecraft:light_blue_stained_glass")
        h.shell_set(x, top + 2, z, SEA)
    D.chandelier_wheel(h, c, c, r=3, drop=7, soul=True)
    for (x, z) in ((4, 4), (22, 4), (4, 22), (22, 22), (13, 4), (13, 22), (4, 13), (22, 13)):
        if h.get(x, deck_row, z) is not None and h.free(x, deck_row + 1, z):
            h.set(x, deck_row + 1, z, "minecraft:soul_lantern", {"hanging": False}) if D.center_support(h.get(x, deck_row, z).block, h.get(x, deck_row, z).props, "up") else None
    for (x, z) in deck:                                     # the blue light under the gallery: a soul lantern hung from the deck on every third cell, vines between them
        if (x % 3 == 1 and z % 3 == 1) and h.free(x, deck_row - 1, z):
            h.set(x, deck_row - 1, z, "minecraft:soul_lantern", {"hanging": True})
        elif rng.random() < 0.05 and h.free(x, deck_row - 1, z):
            h.set(x, deck_row - 1, z, "minecraft:vine", {"up": True})
    for wl in (h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")):
        for lat in range(4, 23, 2):
            if abs(lat - 13) > 3 and rng.random() < 0.5:
                dead_coral_wall(wl, lat, GROUND + rng.choice((1, 2, 3)))
    for k in range(8):
        a = math.radians(45.0 * k + 22.5)
        planter(h, int(round(c + 7.2 * math.cos(a))), int(round(c + 7.2 * math.sin(a))), rng)
    for door in (("n", 13), ("e", 13), ("s", 13), ("w", 13)):
        D.door_trim(h, *door)
    K.wall_lights(h, [SEA], spacing=5, y=GROUND + 3, door_gap=3)
    K.wall_lights(h, [SEA], spacing=5, y=deck_row + 3, door_gap=0, offset=1)
    vines(h, p=0.05, rows=range(deck_row + 2, top))
    h.faction("aquatic", 8, 8)
    for side, off in (("n", 13), ("e", 13), ("s", 13), ("w", 13)):          # a sea lantern flush in the wall either side of every doorway, low and high
        wl = h.wall(side)
        for lat in (off - 3, off + 3):
            for y in (GROUND + 1, GROUND + 3):
                wl.shell_set(lat, y, -1, SEA)
    for (x, z) in ((20, 8), (6, 18), (20, 10), (6, 20)):                    # sea lanterns laid in the floor of the two strips beside the flights
        if h.get(x, FLOOR, z) is not None and h.free(x, GROUND, z):
            h.floor_set(x, z, SEA)
    for (side, lat) in (("e", 8), ("e", 10), ("w", 17), ("w", 20)):         # beside the two flights, where the stairs climb along the wall
        h.wall(side).shell_set(lat, GROUND + 2, -1, SEA)
    for (x, z) in ((6, 14), (20, 12)):                                      # a lantern on the rail where each flight meets the gallery
        if h.get(x, deck_row + 1, z) is not None and h.free(x, deck_row + 2, z):
            h.set(x, deck_row + 2, z, "minecraft:soul_lantern", {"hanging": False})
    D.auto_markers(h, spawns=9, patrols=3)
    return h


# ===================================================================================================================
# SET PIECE: the heart of the tides (4 x 4 modules)
# ===================================================================================================================
@T.room("set", "heart_of_tides", faction="aquatic", view_h=22,
        cuts=[{"name": "west_half", "x": (0, 18), "y": (0, 34), "z": None}],
        views=[("door0", (13.5, 10.62, 3.5), (17.5, 13.0, 17.5)), ("door1", (31.5, 10.62, 13.5), (17.5, 13.0, 17.5)), ("arm", (17.5, 10.62, 7.5), (17.5, 14.0, 17.5)),
               ("island", (17.5, 10.62, 12.5), (17.5, 14.0, 22.0)), ("corner", (17.5, 10.62, 24.0), (6.0, 10.0, 6.0))])
def heart_of_tides(path):
    """Why: the set piece of the temple is the place its god was kept: a round basin twenty blocks across and two deep in the middle of a hall thirty-two wide, four causeways of
    prismarine crossing it to an island, and on the island a pearl: a sphere of sea lantern on a pedestal (the god's light, still burning: it lights the whole hall) with four acolytes
    of the temple standing guard on the island's rim. A ring of twelve pillars with sea-lantern bands stands round the basin, a disc of blue glass in the roof lit from above is the
    sea's surface, four tide pools in the corners glow from their floors, planters of live coral stand along the walls, and the walls carry dead coral fans and vines. The way in
    is any door, the way to the pearl is a causeway, and the water on either side is two deep: the party can wade it (slowed) but the drowned come up out of it, so the causeway is the
    one place to hold. Nobody can be trapped: every edge of the basin is a step a swimmer climbs out by."""
    h = Hall(path, 4, 4, 22, wear=0.03)
    top = h.top
    rng = h.rng
    for (side, off) in (("n", 13), ("e", 13), ("s", 22), ("w", 22)):
        h.door(side, off)
    cx = cz = 17.5
    # 1. the basin: a disc of radius 9.5 with the island (radius 3.6) and the four causeways (four wide) left dry
    basin = []
    for z in range(2, 34):
        for x in range(2, 34):
            r = math.hypot(x - cx, z - cz)
            if r <= 9.5 and r > 3.7 and not (16 <= x <= 19 or 16 <= z <= 19):
                basin.append((x, z))
    pool(h, basin, depth=2, coral=0.30, glow=0.20)
    lily_pads(h, basin, p=0.12)
    # 2. the island: a plinth of prismarine, the pedestal of the pearl (2 x 2), the pearl (a sphere of sea lantern, radius 2.4, centred on row 15.5), four acolytes on the rim
    for z in range(14, 22):
        for x in range(14, 22):
            if math.hypot(x - cx, z - cz) <= 3.7 and h.free(x, GROUND, z):
                h.set(x, GROUND, z, L.POLISHED if math.hypot(x - cx, z - cz) > 2.2 else L.CHISELED)
    for y in range(GROUND + 1, GROUND + 5):
        for (x, z) in ((17, 17), (18, 17), (17, 18), (18, 18)):
            h.set(x, y, z, L.POLISHED)
    for y in range(GROUND + 4, GROUND + 11):
        for z in range(14, 22):
            for x in range(14, 22):
                if math.sqrt((x - cx) ** 2 + (y - (GROUND + 7.0)) ** 2 + (z - cz) ** 2) <= 2.5 and h.free(x, y, z):
                    h.set(x, y, z, SEA)
    for (x, z, f) in ((15, 15, "south"), (20, 15, "south"), (15, 20, "north"), (20, 20, "north")):
        if h.free(x, GROUND + 1, z):
            h.set(x, GROUND + 1, z, L.POLISHED)
            acolyte(h, x, GROUND + 2, z, f)
    # 3. the ring of twelve pillars (r = 11.2), a band of chiselled stone (a sea lantern in the theme) at two heights, capitals
    for k in range(12):
        a = math.radians(15.0 + 30.0 * k)
        px, pz = int(round(cx - 0.5 + 11.2 * math.cos(a))), int(round(cz - 0.5 + 11.2 * math.sin(a)))
        if not h.can_set(px, GROUND, pz):
            continue
        for y in range(GROUND, top):
            h.set(px, y, pz, L.CHISELED if y in (GROUND + 3, GROUND + 9) else L.POLISHED)
        h.set(px, top, pz, L.CHISELED)
    # 4. the four corner tide pools (4 x 4, one deep, glowing floors), lily pads, planters one cell from the walls (never in the way of the broad outer band)
    for (x0, z0) in ((3, 3), (29, 3), (3, 29), (29, 29)):
        cells = [(x, z) for z in range(z0, z0 + 4) for x in range(x0, x0 + 4)]
        pool(h, cells, depth=1, coral=0.25, glow=0.35)
        lily_pads(h, cells, p=0.2)
    for lat in range(8, 29, 4):
        for (x, z) in ((3, lat), (32, lat), (lat, 3), (lat, 32)):
            if abs(lat - 13) > 3 and abs(lat - 22) > 3:
                planter(h, x, z, rng)
    # 5. the surface: a disc of blue glass in the roof lit from above; walls with dead coral fans and vines; light
    for z in range(2, 34):
        for x in range(2, 34):
            if math.hypot(x - cx, z - cz) <= 6.5:
                h.shell_set(x, top + 1, z, "minecraft:light_blue_stained_glass")
                h.shell_set(x, top + 2, z, SEA)
    for wl in (h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")):
        for lat in range(4, 32, 2):
            if rng.random() < 0.5:
                dead_coral_wall(wl, lat, GROUND + rng.choice((1, 2, 3, 4)))
    vines(h, p=0.05)
    K.wall_lights(h, [SEA], spacing=6, y=GROUND + 4, door_gap=3)
    for door in (("n", 13), ("e", 13), ("s", 22), ("w", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    h.faction("aquatic", 8, 14)
    D.auto_light(h, soul=True, max_lamps=40)
    D.auto_markers(h, spawns=9, patrols=3)
    return h


# ===================================================================================================================
# PUZZLE: the tidal gate (3 x 2 modules, four sigil plates, a riddle stone, a puzzle door and a closet of two chests)
# ===================================================================================================================
#: the order the Riddle Stone asks: not the order of either sigil hall of 1.54.0 (2 3 1 0 and 0 2 1 3), nor the fairy ring's (1 0 3 2), nor the resonance hall's (3 1 0 2)
TIDE_ORDER = (0, 3, 1, 2)


@T.room("puzzle", "tidal_gate", faction="aquatic", no_spawn=True, kind="puzzle", view_h=12,
        views=[("door0", (3.5, 10.62, 13.5), (12.0, 11.0, 8.0)), ("door1", (13.5, 10.62, 3.5), (13.0, 11.0, 12.0)), ("stones", (13.5, 10.62, 7.0), (9.0, 10.0, 5.0)),
               ("stone", (13.5, 10.62, 9.0), (13.5, 11.0, 15.0))])
def tidal_gate(path):
    """Why: the temple's puzzle is the sluice that keeps the tide out of the vault: the Spire's puzzle (four sigil plates, a Riddle Stone that names the order, a puzzle door that opens
    only to it) staged as a flooded hall. A dry causeway runs from the north door and another from the west door and they cross by the stone; the rest of the front hall is knee-deep
    water (two pools, floors of coral and sea lanterns, lily pads) with four plates on little stepping stones in it, each one cell off a causeway. The party may wade (the water is one
    deep) but the plates are reached by the stones; the door and the closet behind it are the shared mechanic (`gen_mega_rooms2.sigil_closet`, same blocks, same block entity); the
    order is 0 3 1 2. Nothing spawns in the hall (the puzzle is the danger); a raised portcullis of iron bars over each causeway is the sluice."""
    h = Hall(path, 3, 2, 12, wear=0.04)
    mt = h.mt
    rng = h.rng
    top = h.top
    h.door("w", 13)
    h.door("n", 13)
    R2.sigil_closet(mt, Frame(mt, 18, FR + 1, 8, "east"), rng)
    stones = {(10, 5), (11, 5), (15, 5), (16, 5), (10, 9), (11, 9), (15, 9), (16, 9)}
    west = [(x, z) for z in range(3, 12) for x in range(3, 12) if (x, z) not in stones]
    east = [(x, z) for z in range(3, 12) for x in range(15, 18) if (x, z) not in stones]
    pool(h, west, depth=1, coral=0.30, glow=0.30)
    pool(h, east, depth=1, coral=0.30, glow=0.30)
    lily_pads(h, west + east, p=0.18)
    # the plates, one on the far stone of each pair; a block of chiselled stone under each; the stones are floor
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for (x, z), c in zip(((10, 5), (16, 5), (10, 9), (16, 9)), colours):
        mt.set(x, FR, z, L.CHISELED)
        mt.set(x, FR + 1, z, R2.PLATE, {"color": str(c), "lit": "false"})
    # the riddle stone on the south wall, a lantern over it
    mt.set(13, FR + 1, 15, R2.RIDDLE, None, {"id": R2.RIDDLE, "sequence": R2.nbt_list(TIDE_ORDER)})
    mt.set(13, FR + 2, 15, "minecraft:soul_lantern", {"hanging": "false"})
    # the floor of the dry parts: inlays along the causeways, a medallion where they cross
    for z in range(2, 16):
        for x in (12, 14):
            if mt.get(x, FR, z) is not None and z % 2 == 0 and (x, z) not in ((12, 15), (14, 15)):
                mt.set(x, FR, z, L.POLISHED)
    for x in range(2, 19):
        for z in (12, 14):
            if mt.get(x, FR, z) is not None and x % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    for (x, z) in ((13, 13), (12, 13), (14, 13), (13, 12), (13, 14)):
        mt.set(x, FR, z, SEA if (x, z) == (13, 13) else L.CHISELED)
    # the sluice: a raised portcullis of iron bars over each causeway (two rows of bars on chains), planters on the dry margin, vines, light
    for x in (12, 13, 14):
        for y in (FR + 6, FR + 7):
            mt.set(x, y, 7, L.IRON_BARS)
        for y in range(FR + 8, FR + 13):
            mt.set(x, y, 7, L.CHAIN_BLOCK, {"axis": "y"})
    for (x, z) in ((3, 13), (3, 15), (6, 15), (9, 15), (17, 13), (17, 15), (15, 15), (2, 12)):
        planter(h, x, z, rng)
    K.mt_wall_lights(mt, [SEA], spacing=5, y=FR + 3, sides="ns")
    mt.set(19, FR + 3, 4, SEA)
    mt.set(19, FR + 3, 12, SEA)
    for (x, z) in ((4, 8), (8, 8), (16, 12), (4, 14)):
        hang_soul(h, x, z, 4)
    mt.marker(8, FR + 1, 13, "patrol")
    mt.marker(13, FR + 1, 9, "patrol")
    mt.marker(5, FR + 1, 14, "faction", faction="aquatic")
    h.explicit_defaults()
    mt.finish()
    return mt


# ===================================================================================================================
# TREASURE: the offering nook (1 x 2 modules, one door)
# ===================================================================================================================
@T.room("treasure", "offering_nook", faction="aquatic", view_h=8, few_spawns=True,
        views=[("door0", (4.5, 10.62, 3.0), (4.5, 10.5, 15.0)), ("altar", (4.5, 10.62, 8.0), (4.5, 11.0, 15.0))])
def offering_nook(path):
    """Why: the calm room and the reward of the temple: the chapel where the god's last offerings were left, a dead end behind one door. A little basin of knee-deep water, its floor
    sea lanterns and coral, lies across the chapel (the wader's way is the water, the dry way the two narrow margins either side), and beyond it three steps up to an altar of
    prismarine with the chest on it, a candle each side, a grey coral fan and two acolytes of the temple (armor stands in turtle shells with tridents) standing guard. Blue soul lanterns
    hang from the roof and vines hang from the walls. Nothing waits in it: one spawn marker and the room centre are all it has, and the water is the only thing in the way."""
    h = Hall(path, 1, 2, 8, wear=0.04)
    rng = h.rng
    h.door("n", 4)
    h.route(3, 2, 5, 8, label="entrance run")
    h.marker(4, GROUND, 7, "spawn", weight=2)
    h.marker(4, GROUND, 9, "room_center")
    h.faction("aquatic", 6, 14)
    basin = [(x, z) for z in (10, 11, 12) for x in (3, 4, 5)]
    pool(h, basin, depth=1, coral=0.25, glow=0.45)
    lily_pads(h, basin, p=0.4)
    # the altar: a raised plinth three wide and two deep with a step in front, the chest, candles, a grey fan, the two acolytes
    D.dais(h, 3, 13, 5, 15, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    h.loot_chest(4, GROUND + 1, 15, "north")
    for x in (3, 5):
        D.candles_at(h, x, GROUND + 1, 15, 3, "white" if x == 3 else "light_blue")
    h.set(4, GROUND + 1, 14, "minecraft:dead_tube_coral_fan", {"waterlogged": False}) if h.can_set(4, GROUND + 1, 14, "minecraft:dead_tube_coral_fan", {"waterlogged": False}) else None
    for x in (2, 6):
        h.set(x, GROUND, 15, L.POLISHED)
        acolyte(h, x, GROUND + 1, 15, "north")
    for (x, z) in ((3, 4), (5, 7), (4, 14)):
        hang_soul(h, x, z, 3)
    K.wall_lights(h, [SEA], spacing=5, y=GROUND + 2, sides="we", door_gap=3)
    vines(h, p=0.08)
    D.door_trim(h, "n", 4)
    D.cornice(h)
    D.auto_light(h, soul=True, max_lamps=6)
    return h


if __name__ == "__main__":
    sys.exit(K.run(T, sys.argv[1:]))

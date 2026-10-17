#!/usr/bin/env python3
"""
The two Hearth Halls (templates `structure/mega/safe/hearth_hall_a` and `hearth_hall_b`, 27 x 27 blocks): the big rooms of a rest. Why two: a Safe Zone
the player reaches after a hard stretch has to feel like a place somebody lives in, and two halls of the same plan would make every region's safe room
the same room. A is the VAULT HALL (the reference's two-storey chequerboard hall, warm: a masonry fireplace under a stepped chimney breast, a long
table, a timber balcony on the east side with a library, a kitchenette and a lounge, three beds hung from the balcony on chains, an alchemy bench, a
crafting row), B is the INN COMMON ROOM (a free-standing fireplace island with a chimney in the middle of the hall, a bench ring round it, a bar
counter, a stair up to a gallery of sleeping alcoves, panelled walls). Both are 3 x 3 modules with three doors (a door you can bar is a door you can
feel safe behind) and light on every floor cell.
"""
import mega_safe_lib as S
from mega_safe_lib import *  # noqa: F401,F403
from mega_safe_landing import candles, hay_pile, lamp_on, lectern, potted, statue  # noqa: F401


# ------------------------------------------------------------------------------------------------------------------------ shared pieces
def beams(sr, zs, y=None, block=STRIPPED_LOG, x1=None, x2=None):
    """Ceiling beams along x at each z in `zs`, wall to wall, with a plank corbel under each end (the high side toward the wall)."""
    y = sr.top - 1 if y is None else y
    x1 = WALL if x1 is None else x1
    x2 = sr.W - WALL - 1 if x2 is None else x2
    for z in zs:
        for x in range(x1, x2 + 1):
            sr.set_(x, y, z, block, axis="x")
        sr.set_(x1 + 1, y - 1, z, PLANKS_STAIRS, facing="west", half="top")
        sr.set_(x2 - 1, y - 1, z, PLANKS_STAIRS, facing="east", half="top")


def potion_bench(sr, side, lat0, length, y=None):
    """An alchemy bench along `side`'s wall from `lat0` for `length` cells: a plank counter with brewing stands, a cauldron, a pot and candles on it, two
    rows of bookshelves (the potion shelves) above with a free row between (the marker of the cauldron needs the cell above it), and lanterns on top."""
    w = Wall(sr, side)
    y = sr.stand if y is None else y
    pattern = ["stand", "cauldron", "stand", "pot", "stand", "candles", "stand", "stand"]
    for k in range(length):
        lat = lat0 + k
        w.put(lat, y, 0, PLANKS)
        what = pattern[k % len(pattern)]
        if what == "stand":
            bottles = ((1, 0, 1), (0, 1, 0), (1, 1, 0))[k % 3]
            w.put(lat, y + 1, 0, "minecraft:brewing_stand", has_bottle_0=str(bool(bottles[0])).lower(), has_bottle_1=str(bool(bottles[1])).lower(),
                  has_bottle_2=str(bool(bottles[2])).lower())
        elif what == "cauldron":
            w.put(lat, y + 1, 0, "minecraft:cauldron")
            w.hint(lat, y + 1, 0, "cooking_pot")
        elif what == "pot":
            w.put(lat, y + 1, 0, "minecraft:potted_red_mushroom")
        else:
            x, yy, z = w.at(lat, y + 1, 0)
            candles(sr, x, yy, z, 3, "orange")
        w.put(lat, y + 3, 0, "minecraft:bookshelf")
        w.put(lat, y + 4, 0, "minecraft:bookshelf")
        if k % 3 == 1:
            x, yy, z = w.at(lat, y + 5, 0)
            lamp_on(sr, x, yy, z)


def crafting_row(sr, side, lat0, stations, y=None):
    """A row of work stations against a wall: `stations` is a list of (block, props) placed one per lateral cell from `lat0`."""
    w = Wall(sr, side)
    y = sr.stand if y is None else y
    for k, (block, props) in enumerate(stations):
        p = dict(props)
        if "facing" in p and p["facing"] == "room":
            p["facing"] = w.away
        w.put(lat0 + k, y, 0, block, **p)


# ------------------------------------------------------------------------------------------------------------------------ Hearth Hall A
def hearth_hall_a():
    """The vault hall. Plan, north up, x east, z south, interior 2..24 (doors N, S and W at the middle, none on the east, where the balcony is):
         NW  the great hearth: a masonry fireplace in the west wall under a stepped chimney breast, a rug, a sofa and two armchairs facing it
         N   a stair of six steps along the north wall (x 15..20) up to the balcony
         E   the balcony (floor at y=14, a timber edge on posts with brackets, a fence with lamps): a library nook, a kitchenette (smoker, furnace, boards,
             water), a lounge facing the hall; under it three beds hung from its floor on chains, and a storage nook under the stair landing
         centre-east  the long table, five chairs a side, a chair at each end
         SW  the alchemy bench on the west wall, the crafting row on the south wall, two braziers by the south door."""
    sr = SR("mega/safe/hearth_hall_a", 3, 3, h=11, wear=0.05)
    sr.expect = {"seats": 18, "hearths": 4, "beds_min": 3, "beds_max": 3, "patrol": (2, 2), "doors": ["n", "s", "w"], "armor_max": 4}
    for side in "nsw":
        sr.door(side, 13)
    top = sr.top                                                      # 19
    st = sr.stand                                                     # 9
    BY, BS = 14, 15                                                   # the balcony floor row and the row players stand in up there
    wn, ws, ww = Wall(sr, "n"), Wall(sr, "s"), Wall(sr, "w")

    # ---- floor: the chequerboard of the reference, a warm rug under the long table and one before the fire
    L.checker_floor(sr, 2, 2, 24, 24)
    rug(sr, 15, 8, 20, 18, ["brown", "red", "orange"])
    rug(sr, 4, 4, 9, 9, ["red", "orange", "brown"])

    # ---- the balcony on the east side: floor, a timber edge on posts with brackets, the fence, cross beams under it
    for x in range(21, 25):
        for z in range(2, 25):
            sr.set_(x, BY, z, PLANKS)
    for z in range(4, 25):
        sr.set_(21, BY - 1, z, STRIPPED_LOG, axis="z")
    for pz in (5, 11, 17, 23):
        for y in range(st, BY - 1):
            sr.set_(21, y, pz, LOG, axis="y")
        sr.set_(21, BY - 2, pz - 1, PLANKS_STAIRS, facing="south", half="top")
        sr.set_(21, BY - 2, pz + 1, PLANKS_STAIRS, facing="north", half="top")
        for x in (22, 23, 24):
            sr.set_(x, BY - 1, pz, STRIPPED_LOG, axis="x")
    for z in range(5, 25):
        sr.set_(21, BS, z, FENCE)
    for pz in (5, 11, 17, 23):
        lamp_on(sr, 21, BS + 1, pz)

    # ---- the stair along the north wall: six steps climbing east, brick under them, a rail on the open side
    for i, x in enumerate(range(15, 21)):
        ys = st + i
        for z in (2, 3, 4):
            for y in range(st, ys):
                sr.set_(x, y, z, BRICK)
            sr.set_(x, ys, z, PLANKS_STAIRS, facing="east", half="bottom")
        sr.set_(x, ys + 1, 4, FENCE)

    # ---- architecture: cornice, plinth, pillars, beams
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    beams(sr, (7, 13, 19), x2=20)
    for z in (7, 13, 19):                                             # the beams meet the balcony posts' line: a short beam over the balcony too
        for x in range(21, 25):
            sr.set_(x, top - 1, z, STRIPPED_LOG, axis="x")
    for side, lat in (("s", 11), ("s", 15), ("w", 11), ("w", 15), ("n", 11)):
        pilaster(sr, side, lat)

    # ---- the great hearth: a masonry fireplace in the west wall (NW), a stepped chimney breast, the fire on a raised hearth
    for lat in range(4, 9):
        for y in range(st, top + 1):
            for out in (0, 1):
                ww.put(lat, y, out, BRICK)
    for lat in (4, 8):                                                # the shoulders of the breast: stairs, then only the shaft goes on up
        for out in (0, 1):
            ww.stairs(lat, 14, out, BRICK_STAIRS, high="wall")
            for y in range(15, top + 1):
                ww.clear(lat, y, out)
    for lat in range(5, 8):
        for out in (0, 1):
            for y in range(st + 1, st + 4):
                ww.clear(lat, y, out)
    for lat in range(5, 8):
        ww.put(lat, st + 4, 1, CHISELED if lat == 6 else POLISHED)    # the lintel stones on the front of the opening
        ww.put(lat, st, 2, POLISHED_SLAB, type="bottom")              # the hearth apron
    x, y, z = ww.at(6, st + 1, 0)
    sr.hearth(x, y, z, facing="east")
    for lat in (5, 7):
        for out in (0, 1):
            ww.put(lat, st + 1, out, "minecraft:cobblestone" if out == 0 else BRICK)    # the hearthstones either side of the fire
    for lat, y in ((4, st + 1), (8, st + 1)):
        x, yy, z = ww.at(lat, y, 2)
        sr.set_(x, yy, z, "minecraft:barrel", facing="up", open="false")
    x, yy, z = ww.at(4, st + 2, 2)
    lamp_on(sr, x, yy, z)
    x, yy, z = ww.at(8, st + 2, 2)
    candles(sr, x, yy, z, 3, "orange")
    for lat in (4, 8):                                                # firewood stacked beside the breast
        x, yy, z = ww.at(lat, st, 2)
        sr.set_(x, yy, z, "minecraft:barrel", facing="up", open="false")
    sofa_x = 7
    for z in (5, 6, 7):
        sr.seat(sofa_x, st, z, "west", "sofa")
    sr.seat(5, st, 4, "south", "chair")
    sr.seat(5, st, 9, "north", "chair")
    sr.set_(6, st, 4, "minecraft:spruce_slab", type="top")            # a side table between the chairs and the sofa
    candles(sr, 6, st + 1, 4, 2, "red")
    sr.set_(8, st, 8, "minecraft:spruce_slab", type="top")
    sr.set_(8, st + 1, 8, "minecraft:potted_fern")
    banner(sr, "w", 10, 14, "red")
    banner(sr, "n", 5, 14, "red")
    window_slit(sr, "n", 8, 11)
    for lat in (9, 10):
        for y in (st, st + 1):
            wn.put(lat, y, 0, "minecraft:bookshelf")
    for y in (st, st + 1, st + 2):
        wn.put(6, y, 0, "minecraft:bookshelf")

    # ---- the long table with its chairs and the things on it
    table(sr, 18, st, 9, "south", 9, width=2)                         # x 17..18, z 9..17
    for z in (10, 12, 14, 16):
        sr.seat(16, st, z, "east", "chair")
        sr.seat(19, st, z, "west", "chair")
    sr.seat(17, st, 8, "south", "chair")
    sr.seat(18, st, 18, "north", "chair")
    candles(sr, 17, st + 1, 10, 3, "orange")
    candles(sr, 18, st + 1, 14, 2, "red")
    lamp_on(sr, 17, st + 1, 13)
    sr.set_(18, st + 1, 11, "minecraft:flower_pot")
    sr.set_(17, st + 1, 15, PLATE)
    sr.set_(18, st + 1, 16, PLATE)
    for x, z in ((17, 11), (17, 15)):
        hang_chandelier(sr, x, z, arm=1)
    hang_chandelier(sr, 9, 13, arm=2)
    for z in (9, 19):
        hang_chandelier(sr, 12, z, arm=1)

    # ---- under the balcony: three beds hung from its floor, the storage nook under the stair landing, lamps
    for z, color in ((8, "red"), (14, "orange"), (20, "brown")):
        sr.chain_bed(23, 10, z, "east", color, height=4)
        sr.set_(22, st, z, "minecraft:spruce_slab", type="top")       # a step stool in front of each bed
    for z in (8, 14, 20, 3):
        sr.set_(22, BY - 1, z, LANTERN, hanging="true")
    for pz in (11, 17):
        sr.set_(22, BY - 2, pz, LANTERN, hanging="true")
    for x, y, z in ((23, st, 3), (24, st, 3), (24, st, 4), (23, st + 1, 3), (24, st + 1, 3)):
        sr.set_(x, y, z, "minecraft:barrel", facing="up", open="false")
    sr.hint(23, st, 3, "storage")
    sr.set_(22, st, 2, "minecraft:chest", facing="east", type="single", waterlogged="false")

    # ---- the balcony: the stair landing, the library nook, the kitchenette, the lounge
    rug(sr, 22, 15, 24, 16, ["blue", "light_blue"], y=BS)
    rug(sr, 22, 2, 24, 4, ["blue", "light_blue"], y=BS)
    for z in range(6, 11):
        for y in (BS, BS + 1, BS + 2):
            sr.set_(24, y, z, "minecraft:bookshelf")
    lectern(sr, 23, BS, 8, "east")
    sr.seat(22, BS, 8, "east", "chair")
    sr.seat(22, BS, 6, "east", "chair")
    sr.set_(23, BS, 6, "minecraft:spruce_slab", type="top")
    candles(sr, 23, BS + 1, 6, 2, "orange")
    sr.set_(23, BS, 10, "minecraft:flower_pot")
    for z, kind in ((11, "smoker"), (12, "furnace")):
        sr.hearth(24, BS, z, block="minecraft:%s" % kind, facing="west", hint="stove")
    sr.set_(24, BS, 13, PLANKS)
    sr.set_(24, BS + 1, 13, PLATE)
    sr.hint(24, BS + 1, 13, "cutting_board")
    sr.set_(24, BS, 14, "minecraft:water_cauldron", level="3")
    sr.set_(24, BS, 15, PLANKS)
    sr.set_(24, BS + 1, 15, "minecraft:iron_trapdoor", facing="west", half="bottom", open="false")
    sr.hint(24, BS + 1, 15, "skillet")
    sr.set_(24, BS, 16, "minecraft:barrel", facing="up", open="false")
    sr.hint(24, BS, 16, "storage")
    table(sr, 23, BS, 13, "south", 2, width=1)
    sr.seat(22, BS, 13, "east", "chair")
    sr.seat(22, BS, 14, "east", "chair")
    candles(sr, 23, BS + 1, 14, 3, "red")
    for z in range(19, 23):
        sr.seat(24, BS, z, "west", "sofa")
    sr.set_(23, BS, 20, "minecraft:spruce_slab", type="top")
    sr.set_(23, BS, 21, "minecraft:spruce_slab", type="top")
    candles(sr, 23, BS + 1, 20, 3, "purple")
    sr.set_(23, BS + 1, 21, "minecraft:potted_oxeye_daisy")
    sr.set_(24, BS, 18, "minecraft:barrel", facing="up", open="false")
    sr.set_(24, BS, 23, "minecraft:barrel", facing="up", open="false")
    sr.set_(24, BS, 24, "minecraft:chest", facing="west", type="single", waterlogged="false")
    for lat in (5, 14, 18, 23):
        wall_lamp(sr, "e", lat, BS + 1)
    for lat in (11, 15, 19):
        banner(sr, "e", lat, 17 if lat != 15 else 18, "red")

    # ---- the alchemy bench (west wall, south of the door) and the crafting row (south wall, west of the door)
    potion_bench(sr, "w", 17, 7)
    crafting_row(sr, "s", 3, [("minecraft:crafting_table", {}), ("minecraft:smithing_table", {}), ("minecraft:loom", {"facing": "room"}),
                              ("minecraft:cartography_table", {}), ("minecraft:fletching_table", {}), ("minecraft:stonecutter", {"facing": "room"})])
    ws.barrel(9, st, 0, hint=True)
    ws.chest(10, st, 0)
    for lat in (4, 7):
        wall_lamp(sr, "s", lat, 12)
    wall_lamp(sr, "n", 15, 12)
    for x, z in ((10, 21), (17, 21)):
        brazier(sr, x, st, z, hint=(x == 10))
    for lat in range(17, 21):
        ws.seat(lat, st, 0, "sofa")
    sr.set_(18, st, 22, "minecraft:spruce_slab", type="top")
    candles(sr, 18, st + 1, 22, 2, "orange")
    for lat in (8, 18):
        banner(sr, "s", lat, 14, "orange")

    # ---- the runner from the west door, two statues on its way, herbs drying under the beams, clerestory windows high in the walls
    rug(sr, 4, 12, 14, 14, ["brown", "red"])
    statue(sr, 4, st, 10, "east")
    statue(sr, 4, st, 16, "east")
    herbs(sr, ((6, 13), (8, 13), (10, 13), (7, 19), (11, 19), (5, 7), (19, 13), (16, 7)))
    for side, lat in (("s", 6), ("s", 19), ("w", 13), ("n", 8)):                     # (the north one stands over the first window: a tall double window)
        window_slit(sr, side, lat, 15, glass="minecraft:orange_stained_glass")

    # ---- four free-standing columns carrying the roof either side of the runner, each with a banner on its inner face
    for cx, cz, face in ((7, 10, "south"), (11, 10, "south"), (7, 16, "north"), (11, 16, "north")):
        column(sr, cx, cz)
        banner_on(sr, cx, 14, cz, face, "red")

    # ---- the lounge in the south-west: two sofas facing each other over a low table on a rug
    rug(sr, 4, 18, 10, 22, ["orange", "brown"])
    for z in (19, 20, 21):
        sr.seat(5, st, z, "east", "sofa")
        sr.seat(9, st, z, "west", "sofa")
    table(sr, 7, st, 19, "south", 3, width=1)
    candles(sr, 7, st + 1, 19, 3, "orange")
    lamp_on(sr, 7, st + 1, 21)
    hang_chandelier(sr, 7, 19, arm=1)

    # ---- two guards at the south door (the last two of the four armour stands)
    statue(sr, 10, st, 23, "north")
    statue(sr, 16, st, 23, "north")

    # ---- the walkers
    sr.patrol(13, 8)
    sr.patrol(13, 20)
    cobweb(sr, 3, top - 1, 24)
    cobweb(sr, 20, top - 1, 3)
    return sr


# ------------------------------------------------------------------------------------------------------------------------ Hearth Hall B
SPRUCE_STAIRS = "minecraft:spruce_stairs"
SPRUCE_SLAB = "minecraft:spruce_slab"
SPRUCE_PLANKS = "minecraft:spruce_planks"
SPRUCE_FENCE = "minecraft:spruce_fence"


def hood(sr, cx, cz, y, half):
    """One layer of a stepped pyramid: the ring of radius `half` round (cx, cz) in stairs whose high side points at the centre, and a solid core inside it."""
    for x in range(cx - half, cx + half + 1):
        for z in range(cz - half, cz + half + 1):
            dx, dz = x - cx, z - cz
            if max(abs(dx), abs(dz)) < half:
                sr.set_(x, y, z, BRICK)
                continue
            if abs(dx) >= abs(dz):
                face = "east" if dx < 0 else "west"
            else:
                face = "south" if dz < 0 else "north"
            sr.set_(x, y, z, BRICK_STAIRS, facing=face, half="bottom")


def hearth_hall_b():
    """The inn common room. Plan, north up, x east, z south, interior 2..24 (doors in the middle of all four sides):
         centre   a fireplace island under a stepped hood and a chimney (a campfire on a stone base, four pillars, a kerb), a ring of settles round it
         NE       the bar: a counter with six stools, and behind it the kitchen (smoker, furnace, boards, water, kegs)
         NW       two booths against the west wall (a table and a bench each side), a notice wall on the north wall
         E        the crafting row on the east wall; W the stair of six steps along the west wall up to
         S        the sleeping gallery (floor at y=14) over the south side: three beds in three timber alcoves; under it two snugs (tables and benches)
       The floor is a two-wood parquet, the walls carry log pilasters, the ceiling a timber grid."""
    sr = SR("mega/safe/hearth_hall_b", 3, 3, h=10, wear=0.04)
    sr.expect = {"seats": 20, "hearths": 4, "beds_min": 3, "beds_max": 3, "patrol": (2, 2), "doors": ["n", "e", "s", "w"], "armor_max": 4}
    for side in "nesw":
        sr.door(side, 13)
    top = sr.top                                                      # 18
    st = sr.stand                                                     # 9
    GY, GS = 14, 15                                                   # the gallery floor row and the row players stand in up there
    wn, we = Wall(sr, "n"), Wall(sr, "e")

    # ---- floor: a two-wood parquet and a rug round the hearth
    L.checker_floor(sr, 2, 2, 24, 24, a=PLANKS, b=SPRUCE_PLANKS)
    rug(sr, 7, 7, 19, 19, ["brown", "red", "orange", "red"])

    # ---- the gallery on the south side: floor, front beam on posts with brackets, fence, the stair on the west wall that climbs to it
    for x in range(2, 25):
        for z in range(21, 25):
            sr.set_(x, GY, z, PLANKS)
    for x in range(2, 25):
        sr.set_(x, GY - 1, 21, STRIPPED_LOG, axis="x")
    for px in (6, 10, 16, 20):
        for y in range(st, GY - 1):
            sr.set_(px, y, 21, LOG, axis="y")
        sr.set_(px - 1, GY - 2, 21, PLANKS_STAIRS, facing="east", half="top")
        sr.set_(px + 1, GY - 2, 21, PLANKS_STAIRS, facing="west", half="top")
        for z in (22, 23, 24):
            sr.set_(px, GY - 1, z, STRIPPED_LOG, axis="z")
    for x in range(5, 25):
        sr.set_(x, GS, 21, SPRUCE_FENCE)
    for i, z in enumerate(range(15, 21)):
        ys = st + i
        for x in (2, 3, 4):
            for y in range(st, ys):
                sr.set_(x, y, z, BRICK)
            sr.set_(x, ys, z, SPRUCE_STAIRS, facing="south", half="bottom")
        sr.set_(4, ys + 1, z, SPRUCE_FENCE)

    # ---- architecture: cornice, plinth of boards, corner pillars, log pilasters, the timber grid of the ceiling
    cornice(sr)
    plinth(sr, block=PLANKS_SLAB)
    corner_pillars(sr, shaft=LOG, cap=STRIPPED_LOG)
    for side, lat in (("n", 11), ("n", 15), ("s", 11), ("s", 15), ("e", 11), ("e", 15), ("w", 11)):
        pilaster(sr, side, lat, shaft=LOG, cap=STRIPPED_LOG)
    beams(sr, (7, 19))
    for x in (7, 19):
        for z in range(WALL, sr.D - WALL):
            if sr.t.get(x, top - 1, z) is None:
                sr.set_(x, top - 1, z, STRIPPED_LOG, axis="z")

    # ---- the fireplace island: base, kerb, four pillars, hood, chimney, the fire
    for x in range(12, 15):
        for z in range(12, 15):
            sr.set_(x, st, z, BRICK)
    for x in range(11, 16):
        for z in range(11, 16):
            edge_x, edge_z = x in (11, 15), z in (11, 15)
            if edge_x and edge_z:
                for y in range(st, st + 4):
                    sr.set_(x, y, z, POLISHED)
                sr.set_(x, st + 3, z, CHISELED)
            elif edge_x or edge_z:
                sr.set_(x, st, z, BRICK_SLAB, type="bottom")
    for x in range(11, 16):
        for z in range(11, 16):
            sr.set_(x, st + 4, z, BRICK)
    hood(sr, 13, 13, st + 5, 2)
    hood(sr, 13, 13, st + 6, 1)
    for y in range(st + 7, top + 1):
        sr.set_(13, y, 13, POLISHED if y % 2 else CHISELED)
    sr.hearth(13, st + 1, 13, facing="north")
    sr.set_(14, st + 1, 12, "minecraft:barrel", facing="up", open="false")             # firewood, and a pot on the hob, on the corners of the base
    sr.set_(12, st + 1, 14, "minecraft:cauldron")
    sr.hint(12, st + 1, 14, "cooking_pot")
    for z in (10, 11, 15, 16):
        sr.seat(10, st, z, "east", "sofa", block=SPRUCE_STAIRS)
        sr.seat(16, st, z, "west", "sofa", block=SPRUCE_STAIRS)
    for x in (11, 15):
        sr.seat(x, st, 10, "south", "sofa", block=SPRUCE_STAIRS)
        sr.seat(x, st, 16, "north", "sofa", block=SPRUCE_STAIRS)
    for x, z in ((9, 9), (17, 9), (9, 17), (17, 17)):                 # a lamp post in each corner of the ring
        lamp_post(sr, x, st, z, height=2)

    # ---- NE: the bar and the kitchen behind it
    counter(sr, 21, st, 2, "south", 7, wood="spruce")
    for z in range(3, 9):
        sr.seat(20, st, z, "east", "chair", block=SPRUCE_STAIRS)
    for z, what in ((3, "candles"), (5, "pot"), (7, "candles"), (8, "plate")):
        if what == "candles":
            candles(sr, 21, st + 1, z, 2, "orange")
        elif what == "pot":
            sr.set_(21, st + 1, z, "minecraft:flower_pot")
        else:
            sr.set_(21, st + 1, z, PLATE)
    for z in (3, 6):                                                  # two lanterns on long chains over the counter (the lantern in row 11, the chain up to the ceiling)
        hang_lamp(sr, 21, z, chain=top - (st + 2))
    sr.hint(21, st, 6, "counter")
    sr.hearth(24, st, 4, block="minecraft:smoker", facing="west", hint="stove")
    sr.hearth(24, st, 5, block="minecraft:furnace", facing="west", hint="stove")
    sr.set_(24, st, 3, "minecraft:barrel", facing="up", open="false")
    sr.hint(24, st, 3, "storage")
    sr.set_(24, st, 6, PLANKS)
    sr.set_(24, st + 1, 6, PLATE)
    sr.hint(24, st + 1, 6, "cutting_board")
    sr.set_(24, st, 7, "minecraft:water_cauldron", level="3")
    sr.set_(24, st, 8, PLANKS)
    sr.set_(24, st + 1, 8, "minecraft:iron_trapdoor", facing="west", half="bottom", open="false")
    sr.hint(24, st + 1, 8, "skillet")
    sr.set_(24, st, 9, "minecraft:barrel", facing="up", open="false")
    sr.set_(24, st, 10, "minecraft:chest", facing="west", type="single", waterlogged="false")
    for z in range(3, 10):
        if z not in (4, 8):                                           # (the two wall lamps below take their places in the row)
            sr.set_(24, st + 3, z, "minecraft:bookshelf")
    for x in (22, 23):                                                # the kegs along the north wall, two high
        sr.set_(x, st, 2, "minecraft:barrel", facing="south", open="false")
        sr.set_(x, st + 1, 2, "minecraft:barrel", facing="south", open="false")
    sr.hint(23, st, 2, "storage")
    sr.set_(22, st + 2, 2, LANTERN, hanging="false")
    for z in (4, 8):
        wall_lamp(sr, "e", z, st + 3)
    rug(sr, 22, 3, 23, 10, ["red", "brown"])

    # ---- NW: two booths against the west wall, the notice wall on the north wall
    for zc in (4, 8):
        for z in (zc - 1, zc, zc + 1):
            sr.seat(2, st, z, "east", "sofa", block=SPRUCE_STAIRS)
            sr.seat(5, st, z, "west", "sofa", block=SPRUCE_STAIRS)
        table(sr, 4, st, zc - 1, "south", 3, width=2, wood="spruce")      # x 3..4; the marker of the table is above (4, st, zc)
        candles(sr, 3, st + 1, zc, 2, "orange")
        lamp_on(sr, 3, st + 1, zc + 1)
    for lat in (7, 8, 9, 10):
        for y in (st, st + 1, st + 2):
            if lat in (8, 9) and y == st + 1:
                continue
            wn.put(lat, y, 0, "minecraft:bookshelf")
    lectern(sr, *wn.at(8, st, 1), "north")
    candles(sr, *wn.at(9, st, 1), 3, "red")
    banner(sr, "w", 6, 14, "red")
    banner(sr, "n", 5, 14, "red")
    window_slit(sr, "n", 8, 14)

    # ---- E: the crafting row; W: a pair of lamps by the stair
    crafting_row(sr, "e", 16, [("minecraft:crafting_table", {}), ("minecraft:smithing_table", {}), ("minecraft:loom", {"facing": "room"}),
                               ("minecraft:cartography_table", {}), ("minecraft:fletching_table", {})])
    we.barrel(21, st, 0, hint=True)
    for lat in (16, 19):
        wall_lamp(sr, "e", lat, st + 3)
    banner(sr, "e", 17, 14, "orange")
    banner(sr, "e", 19, 14, "orange")
    wall_lamp(sr, "w", 15, st + 4)

    # ---- the gallery: three beds in three alcoves, chests, lamps, the landing at the top of the stair
    for bx, color, box_x, candle in ((8, "red", 9, (7, 24)), (14, "orange", 13, (15, 23)), (20, "brown", 21, (19, 24))):   # (the south pilaster at x 15 runs up through the gallery: nothing stands in it)
        sr.bed(bx, GS, 23, "south", color)
        sr.set_(box_x, GS, 24, "minecraft:barrel", facing="up", open="false")
        candles(sr, candle[0], GS, candle[1], 2, "orange")
    for px in (5, 10, 16, 22):                                        # the partitions of the alcoves leave the front row (z 22) open as the way along the gallery
        for z in (23, 24):
            for y in (GS, GS + 1):
                sr.set_(px, y, z, SPRUCE_PLANKS)
    sr.set_(24, GS, 24, "minecraft:chest", facing="west", type="single", waterlogged="false")
    sr.set_(24, GS, 23, "minecraft:barrel", facing="west", open="false")
    sr.hint(24, GS, 23, "storage")
    rug(sr, 2, 22, 4, 24, ["blue", "light_blue"], y=GS)
    sr.seat(3, GS, 24, "north", "chair", block=SPRUCE_STAIRS)
    sr.set_(2, GS, 24, "minecraft:barrel", facing="up", open="false")
    for x in (3, 8, 14, 20):                                          # one chain link only: the lantern hangs in the top row, above the head of a person on the gallery
        hang_lamp(sr, x, 22, chain=1)
    for x in (6, 12, 18, 23):
        lamp_on(sr, x, GS + 1, 21)

    # ---- under the gallery, south: two snugs (a table, a bench against the wall and chairs) either side of the south door, lamps from the gallery floor
    for x0 in (6, 17):
        table(sr, x0, st, 23, "east", 4, width=1, wood="spruce")
        for x in range(x0, x0 + 4):
            sr.seat(x, st, 22, "south", "chair", block=SPRUCE_STAIRS)
        for x in (x0, x0 + 3):
            sr.seat(x, st, 24, "north", "chair", block=SPRUCE_STAIRS)
        candles(sr, x0 + 1, st + 1, 23, 3, "orange")
        sr.set_(x0 + 3, st + 1, 23, "minecraft:potted_fern")             # the table's marker is above its middle slab (x0 + 2): that cell stays free
    for x in (7, 11, 16, 21):
        sr.set_(x, GY - 1, 22, LANTERN, hanging="true")
    for x in (4, 9, 18, 22):
        sr.set_(x, GY - 1, 24, LANTERN, hanging="true")

    # ---- light, banners, the walkers
    for x, z in ((7, 7), (18, 7), (7, 19), (19, 19), (13, 7), (13, 19), (7, 13), (19, 13)):
        hang_chandelier(sr, x, z, arm=1)
    brazier(sr, 9, st, 4, hint=True)
    brazier(sr, 17, st, 4, hint=False)
    sr.patrol(13, 7)
    sr.patrol(13, 19)
    cobweb(sr, 3, top - 1, 24)
    cobweb(sr, 24, top - 1, 3)
    return sr


if __name__ == "__main__":
    r = hearth_hall_b()
    print(r.finish_and_check().text())

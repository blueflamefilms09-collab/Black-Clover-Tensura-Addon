#!/usr/bin/env python3
"""
The two Landings (templates `structure/mega/landing/landing_a` and `landing_b`, 27 x 27 blocks): the safe hall at the start of every sector. Why two:
a Landing is the first room of every region, so the sectors must not all begin in the same hall; A is the stone-brick WORKSHOP of the owner's first
reference picture (columns with iron bars, window slits with shutters, work stations, a hearth, hay beds), B is the purple deepslate VAULT of the
second (chequerboard floor, a balcony reached by a stair, chains and cages, wall niches with lanterns and chests). Both: four doors at the centre of
each side (the generator attaches one to the previous sector and fills the others), the way home (`nusmp:labyrinth_exit` in a framed alcove, with a
sign), an arrival marker, seats, hearths and beds for the rest system, two patrol points, light on every floor cell.
"""
import mega_safe_lib as S
from mega_safe_lib import *  # noqa: F401,F403


# ------------------------------------------------------------------------------------------------------------------------ shared pieces
def exit_gate(sr, side, lat, y0=None):
    """The way home: a 3x3 plane of `nusmp:labyrinth_exit` blocks standing in the inner wall layer of `side`'s wall, in a framed recess (polished
    pillars, a chiselled lintel), centred at lateral `lat`; the outer wall layer behind it stays solid. The block has no collision: a player walks
    into it. Above the lintel a wall sign says what it is. The ARRIVAL marker goes 3 cells in front (`Wall.xz(lat, 3)`)."""
    y0 = sr.stand if y0 is None else y0
    w = Wall(sr, side)
    for dl in range(-2, 3):
        for k in range(4):
            w.clear(lat + dl, y0 + k, -1)
    for dl in (-2, 2):
        for k in range(4):
            x, z = w.xz(lat + dl, -1)
            sr.set(x, y0 + k, z, POLISHED)
    for dl in range(-2, 3):
        x, z = w.xz(lat + dl, -1)
        sr.set(x, y0 + 3, z, CHISELED if dl == 0 else POLISHED)
    for dl in range(-1, 2):
        for k in range(3):
            x, z = w.xz(lat + dl, -1)
            sr.set(x, y0 + k, z, EXIT)
    x, z = w.xz(lat, 0)
    sign(sr, x, y0 + 4, z, w.away, ["", "WAY HOME", "the Exit Gate", ""])


def statue(sr, x, y, z, facing, armour="iron"):
    """An armoured statue on a plinth (polished block and slab): an armour stand in a full set."""
    sr.set(x, y, z, POLISHED)
    sr.set(x, y + 1, z, POLISHED_SLAB, {"type": "bottom"})
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
    sr.armor_stand(x, y + 1, z, yaw=yaw, head="minecraft:%s_helmet" % armour, chest="minecraft:%s_chestplate" % armour, legs="minecraft:%s_leggings" % armour,
                   feet="minecraft:%s_boots" % armour, mainhand="minecraft:%s_sword" % armour, dy=0.5)


def lectern(sr, x, y, z, facing, book=True):
    sr.set(x, y, z, "minecraft:lectern", {"facing": facing, "has_book": "true" if book else "false", "powered": "false"})


def candles(sr, x, y, z, n=3, color="red"):
    sr.set(x, y, z, "minecraft:%s_candle" % color, {"candles": str(n), "lit": "true", "waterlogged": "false"})


def potted(sr, x, y, z, plant="red_mushroom"):
    sr.set(x, y, z, "minecraft:potted_%s" % plant)


def hay_pile(sr, cells):
    for (x, y, z) in cells:
        sr.set(x, y, z, "minecraft:hay_block", {"axis": "y" if y % 2 else "x"})


def lamp_on(sr, x, y, z):
    """A lantern standing on the block below it."""
    sr.set(x, y, z, LANTERN, {"hanging": "false"})


# ------------------------------------------------------------------------------------------------------------------------ Landing A
def landing_a():
    """The workshop Landing (see the module doc). Plan, north up, x east, z south, interior 2..24:
         NW  the way home (exit gate on the north wall, a runner, statues, a map table on the west wall)
         NE  the workshop (a row of crafting stations under window slits, a long work table with stools, a bookcase wall on the east wall)
         SW  the rest corner (a glazed bay with window seats on the west wall, two beds, hay and barrels)
         SE  the hearth (a raised stone hearth with a campfire and benches on all sides, a kitchen counter on the south wall)
       and the two aisles, 3 wide, that join the four doors."""
    sr = SR("mega/landing/landing_a", 3, 3, h=9, wear=0.06)
    sr.expect = {"seats": 6, "hearths": 3, "beds_min": 2, "beds_max": 3, "patrol": (2, 2), "arrival": True, "doors": ["n", "e", "s", "w"], "armor_max": 6}
    for side in "nesw":
        sr.door(side, 13)
    top = sr.top                                                      # 17
    st = sr.stand                                                     # 9
    wn, ws, ww, we = Wall(sr, "n"), Wall(sr, "s"), Wall(sr, "w"), Wall(sr, "e")

    # ---- floor: the two aisles in polished stone, a chiselled medallion at the crossing, a rug round the hearth
    for x in range(12, 15):
        for z in range(2, 25):
            sr.set_(x, FLOOR_ROW, z, POLISHED)
    for z in range(12, 15):
        for x in range(2, 25):
            sr.set_(x, FLOOR_ROW, z, POLISHED)
    for x in range(10, 17):
        for z in range(10, 17):
            ring = max(abs(x - 13), abs(z - 13))
            sr.set_(x, FLOOR_ROW, z, CHISELED if ring == 3 else (TILE if ring == 2 else POLISHED))
    sr.set_(13, FLOOR_ROW, 13, CHISELED)
    rug(sr, 16, 15, 24, 23, ["red", "orange", "brown"])

    # ---- architecture: cornice, plinth, corner pillars, ceiling beams with corbels
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    for z in (7, 13, 19):
        for x in range(WALL, sr.W - WALL):
            sr.set_(x, top - 1, z, STRIPPED_LOG, axis="x")
        for x, face in ((WALL + 1, "west"), (sr.W - WALL - 2, "east")):
            sr.set_(x, top - 2, z, PLANKS_STAIRS, facing=face, half="top")

    # ---- NW: the way home
    exit_gate(sr, "n", 6)
    rug(sr, 5, 2, 7, 9, ["blue", "light_blue"])
    sr.arrival(*wn.xz(6, 3))
    statue(sr, 3, st, 4, "south")
    statue(sr, 9, st, 4, "south")
    wall_lamp(sr, "n", 3, 11)
    wall_lamp(sr, "n", 9, 11)
    banner(sr, "n", 3, 15, "blue")
    banner(sr, "n", 9, 15, "blue")
    # the map table on the west wall
    ww.barrel(8, st, 0, hint=True)
    ww.put(9, st, 0, "minecraft:cartography_table")
    lectern(sr, *ww.at(10, st, 0), "east")
    rug(sr, 3, 8, 4, 10, ["light_gray", "white"])
    window_slit(sr, "w", 5, 11)
    banner(sr, "w", 7, 14, "red")

    # ---- NE: the workshop
    stations = [(16, "minecraft:crafting_table", {}), (17, "minecraft:smithing_table", {}), (18, "minecraft:anvil", {"facing": "south"}),
                (19, "minecraft:grindstone", {"face": "floor", "facing": "north"}), (20, "minecraft:fletching_table", {}),
                (21, "minecraft:loom", {"facing": "south"}), (22, "minecraft:stonecutter", {"facing": "south"})]
    for lat, block, props in stations:
        wn.put(lat, st, 0, block, **props)
    wn.barrel(23, st, 0, hint=True)
    wn.chest(24, st, 0)
    for lat in (17, 20):
        candles(sr, *wn.at(lat, st + 1, 0), 2, "orange")
    potted(sr, *wn.at(16, st + 1, 0))
    lamp_on(sr, *wn.at(21, st + 1, 0))
    window_slit(sr, "n", 18, 11)
    window_slit(sr, "n", 22, 11)
    table(sr, 17, st, 6, "east", 6, width=2)
    for x in (18, 20, 22):
        sr.seat(x, st, 5, "south", "chair")
        sr.seat(x, st, 8, "north", "chair")
    candles(sr, 18, st + 1, 6, 2, "red")
    lamp_on(sr, 21, st + 1, 7)
    sr.set(19, st + 1, 7, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    sr.set(22, st + 1, 6, "minecraft:flower_pot")
    # the east wall: a bookcase wall, three high, with a reading lectern in the middle
    for z in range(3, 11):
        for y in (st, st + 1, st + 2):
            if (z, y) != (6, st):                                     # the reading lectern takes this place in the row
                we.put(z, y, 0, "minecraft:bookshelf")
    lectern(sr, *we.at(6, st, 0), "west")
    candles(sr, *we.at(8, st + 3, 0), 3, "orange")
    potted(sr, *we.at(4, st + 3, 0), "fern")
    wall_lamp(sr, "e", 4, 13)
    wall_lamp(sr, "e", 9, 13)

    # ---- SW: the rest corner
    for z in (16, 19, 22):                                          # the glazed bay: polished piers, iron bars, orange glass behind
        px, pz = ww.xz(z, -1)
        for y in range(st, st + 7):
            sr.set(px, y, pz, POLISHED)
    for z in (17, 18, 20, 21, 23, 24):
        for y in range(st, st + 7):
            bx, bz = ww.xz(z, -1)
            gx, gz = ww.xz(z, -2)
            sr.set(bx, y, bz, IRON_BARS)
            sr.set(gx, y, gz, "minecraft:orange_stained_glass")
    for z in range(16, 25):
        bx, bz = ww.xz(z, -1)
        sr.set(bx, st + 7, bz, POLISHED_SLAB, {"type": "bottom"})
    for z in (17, 18, 20, 21):
        ww.seat(z, st, 0, "sofa", toward=True)
    sr.bed(5, st, 23, "south", "red")
    sr.bed(8, st, 23, "south", "brown")
    sr.set(6, st, 24, "minecraft:barrel", {"facing": "up", "open": "false"})
    sr.hint(6, st, 24, "storage")
    sr.set(7, st, 24, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": "false"})
    rug(sr, 4, 19, 9, 21, ["brown", "orange"])
    hay_pile(sr, [(10, st, 24), (10, st + 1, 24), (9, st, 24), (10, st, 23)])
    lamp_on(sr, 9, st + 1, 24)                                      # on the hay block beside the second bed: a chest top is 14/16 high and cannot hold a lantern up
    sr.set(4, st, 24, "minecraft:barrel", {"facing": "north", "open": "false"})
    sr.set(3, st, 24, "minecraft:barrel", {"facing": "up", "open": "false"})
    banner(sr, "s", 4, 14, "orange")
    window_slit(sr, "s", 6, 11)

    # ---- SE: the hearth and the kitchen
    for x in range(19, 22):                                          # a raised stone hearth: a 3x3 block base, a brick rim, the campfire in the middle
        for z in range(18, 21):
            sr.set(x, st, z, BRICK)
            if (x, z) != (20, 19):
                outward = "north" if z == 18 else "south" if z == 20 else ("west" if x == 19 else "east")
                sr.set(x, st + 1, z, BRICK_STAIRS, {"facing": OPPOSITE[outward], "half": "bottom"})
    sr.hearth(20, st + 1, 19, facing="north")
    sr.set(21, st + 1, 19, "minecraft:cauldron")
    sr.hint(21, st + 1, 19, "cooking_pot")
    stool_row(sr, 21, st, 16, "south", 3, "sofa")                    # the north bench (sitters look south at the fire)
    stool_row(sr, 19, st, 22, "north", 3, "sofa")                    # the south bench
    for k in range(3):
        sr.seat(17, st, 18 + k, "east", "sofa")
        sr.seat(23, st, 18 + k, "west", "sofa")
    # the kitchen counter along the south wall
    ws.barrel(15, st, 0, hint=True)
    sx, sz = ws.xz(16, 0)
    sr.hearth(sx, st, sz, block="minecraft:smoker", facing="north", hint="stove")
    sx, sz = ws.xz(17, 0)
    sr.hearth(sx, st, sz, block="minecraft:furnace", facing="north", hint="stove")
    for lat, extra in ((18, "plate"), (19, "skillet"), (21, "plate")):
        x, y, z = ws.put(lat, st, 0, PLANKS)
        if extra == "plate":
            sr.set(x, st + 1, z, PLATE)
            sr.hint(x, st + 1, z, "cutting_board")
        else:
            sr.set(x, st + 1, z, "minecraft:iron_trapdoor", {"facing": "north", "half": "bottom", "open": "false"})
            sr.hint(x, st + 1, z, "skillet")
    ws.put(20, st, 0, "minecraft:water_cauldron", level="3")
    ws.barrel(22, st, 0, up=False)
    ws.barrel(23, st, 0)
    ws.chest(24, st, 0)
    window_slit(sr, "e", 17, 11)
    window_slit(sr, "e", 22, 11)

    # ---- light: chandeliers on the beams, lamps beside the doors
    for (x, z) in ((7, 7), (20, 7), (7, 19), (20, 19)):
        chandelier(sr, x, z, drop=2, arm=1, top=top - 2)
    chandelier(sr, 13, 13, drop=2, arm=2, top=top - 2)
    for lat in (10, 16):
        for side in "nsew":
            wall_lamp(sr, side, lat, 12)

    # ---- the walkers, a few cobwebs in the top corners
    sr.patrol(13, 7)
    sr.patrol(13, 19)
    cobweb(sr, 3, top - 1, 3)
    cobweb(sr, sr.W - 4, top - 1, sr.D - 4)
    return sr


# ------------------------------------------------------------------------------------------------------------------------ Landing B
def landing_b():
    """The vault Landing: purple-deepslate chequerboard hall, two storeys. Plan, north up, x east, z south, interior 2..24:
         the NORTH side is a balcony (floor at y=14) over an arcaded gallery with an alchemy bench and a smithy; two beds, a library nook, a small table
         and a kitchen are up there; a stair of six steps climbs to it along the east wall (x 22..24, z 7..11), so the east door stays clear;
         the hall is open to the roof with chandeliers, a hanging lava cauldron and a caged skull, niches with chests and lanterns in the west wall,
         a long crafting table east of the middle, and on the south wall the way home (the exit gate) beside a brazier."""
    sr = SR("mega/landing/landing_b", 3, 3, h=11, wear=0.04)
    sr.expect = {"seats": 4, "hearths": 3, "beds_min": 2, "beds_max": 3, "patrol": (2, 2), "arrival": True, "doors": ["n", "e", "s", "w"], "armor_max": 4}
    for side in "nesw":
        sr.door(side, 13)
    top = sr.top                                                      # 19
    st = sr.stand                                                     # 9
    BY, BS = 14, 15                                                   # the balcony floor row and the row players stand in up there
    wn, ws, ww = Wall(sr, "n"), Wall(sr, "s"), Wall(sr, "w")

    # ---- floor and architecture: the chequerboard of the reference with a medallion rug, cornice, plinth, corner pillars
    L.checker_floor(sr, 2, 2, 24, 24)
    rug(sr, 9, 9, 17, 17, ["purple", "magenta", "white", "purple"])
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)

    # ---- the balcony: floor, underside trim, an arcade of pillars and arches along its edge (z=7), a cherry-wood fence with lamps
    for x in range(2, 25):
        for z in range(2, 7):
            sr.set_(x, BY, z, POLISHED)
        sr.set_(x, BY - 1, 6, POLISHED_STAIRS, facing="north", half="top")
    for px in (3, 7, 11, 15, 19):
        for y in range(st, BY):
            sr.set_(px, y, 7, POLISHED)
        sr.set_(px, BY - 1, 7, CHISELED)
    for px in (3, 7, 11, 15):
        sr.set_(px + 1, BY - 1, 7, POLISHED_STAIRS, facing="west", half="top")
        sr.set_(px + 2, BY - 1, 7, POLISHED_SLAB, type="top")
        sr.set_(px + 3, BY - 1, 7, POLISHED_STAIRS, facing="east", half="top")
    for x in range(2, 23):
        sr.set_(x, BS, 6, "minecraft:cherry_fence")
    for px in (3, 7, 11, 15, 19):
        lamp_on(sr, px, BS + 1, 6)
    for i, z in enumerate(range(11, 5, -1)):                          # the stair: six steps (the high part of step i is i+1 above the floor) from z=11 to the balcony edge (z=6)
        ys = st + i
        for x in (22, 23, 24):
            for y in range(st, ys):
                sr.set_(x, y, z, POLISHED)
            sr.set_(x, ys, z, POLISHED_STAIRS, facing="north", half="bottom")
        sr.set_(22, ys + 1, z, "minecraft:cherry_fence")

    # ---- under the balcony, west: the alchemy bench along the north wall
    counter(sr, 3, st, 2, "east", 7)                                  # x 3..9 (facing east: forward is +x)
    for x, bottles in ((4, (1, 0, 1)), (6, (0, 1, 0)), (8, (1, 1, 0))):
        sr.set_(x, st + 1, 2, "minecraft:brewing_stand", has_bottle_0="true" if bottles[0] else "false", has_bottle_1="true" if bottles[1] else "false",
                has_bottle_2="true" if bottles[2] else "false")
        sr.set_(x, st + 3, 2, "minecraft:skeleton_wall_skull", facing="south")
    sr.set_(5, st + 1, 2, "minecraft:cauldron")
    sr.hint(5, st + 1, 2, "cooking_pot")
    sr.set_(7, st + 1, 2, "minecraft:potted_red_mushroom")
    sr.set_(9, st + 1, 2, "minecraft:skeleton_skull", rotation=skull_rot("south"))
    candles(sr, 3, st + 1, 2, 3, "purple")
    for x in (3, 5, 7, 9):
        sr.set_(x, BY - 1, 3, LANTERN, hanging="true")
    sr.set_(10, st, 3, "minecraft:barrel", facing="up", open="false")
    sr.hint(10, st, 3, "storage")
    sr.set_(10, st, 4, "minecraft:barrel", facing="west", open="false")
    sr.set_(3, st, 5, "minecraft:barrel", facing="up", open="false")
    sr.set_(3, st, 4, "minecraft:barrel", facing="north", open="false")

    # ---- under the balcony, east: the smithy along the north wall
    for lat, block, props in ((17, "minecraft:smithing_table", {}), (18, "minecraft:anvil", {"facing": "west"}),
                              (19, "minecraft:grindstone", {"face": "floor", "facing": "north"}), (20, "minecraft:crafting_table", {}),
                              (21, "minecraft:fletching_table", {})):
        wn.put(lat, st, 0, block, **props)
    sx, sz = wn.xz(22, 0)
    sr.hearth(sx, st, sz, block="minecraft:blast_furnace", facing="south", hint="stove")
    wn.barrel(23, st, 0, hint=True)
    wn.chest(24, st, 0)
    for lat in (17, 19, 21, 23):
        sr.set_(*wn.at(lat, BY - 1, 1), LANTERN, hanging="true")
    candles(sr, *wn.at(17, st + 1, 0), 2, "orange")

    # ---- the balcony: beds, a library nook, a small table, the kitchen
    sr.bed(3, BS, 3, "north", "purple")
    sr.bed(6, BS, 3, "north", "magenta")
    sr.set_(4, BS, 2, "minecraft:barrel", facing="up", open="false")
    sr.hint(4, BS, 2, "storage")
    sr.set_(5, BS, 2, "minecraft:chest", facing="south", type="single", waterlogged="false")
    sr.set_(7, BS, 2, "minecraft:barrel", facing="up", open="false")
    lamp_on(sr, 7, BS + 1, 2)                                         # on the barrel, not on the chest beside it (a chest top is 14/16 high and cannot hold a lantern up)
    for x in (9, 10, 11):
        for y in (BS, BS + 1, BS + 2):
            sr.set_(x, y, 2, "minecraft:bookshelf")
    lectern(sr, 10, BS, 3, "north")
    table(sr, 13, BS, 4, "east", 2, width=1)
    sr.seat(13, BS, 3, "south", "chair")
    sr.seat(14, BS, 5, "north", "chair")
    candles(sr, 14, BS + 1, 4, 3, "purple")
    for lat, kind in ((16, "smoker"), (17, "furnace")):
        sx, sz = wn.xz(lat, 0)
        sr.hearth(sx, BS, sz, block="minecraft:%s" % kind, facing="south", hint="stove")
    for lat in (18, 20):
        x, y, z = wn.put(lat, BS, 0, PLANKS)
        sr.set_(x, BS + 1, z, PLATE)
        sr.hint(x, BS + 1, z, "cutting_board")
    wn.put(19, BS, 0, "minecraft:water_cauldron", level="3")
    wn.barrel(21, BS, 0, hint=True)
    wn.chest(22, BS, 0)
    for x in (4, 9, 14, 19):
        hang_lamp(sr, x, 3, chain=1)
    for dx in (-1, 0, 1):                                             # a niche over the door, with lanterns and a skull
        for k in range(3):
            sr.clear(13 + dx, BS + 1 + k, 1)
    for dx in (-1, 1):
        lamp_on(sr, 13 + dx, BS + 1, 1)
    sr.set(13, BS + 2, 1, "minecraft:skeleton_skull", {"rotation": skull_rot("south")})        # on the keystone of the door's portcullis
    sr.patrol(13, 20)

    # ---- the hall: the long crafting table with its seats, the chandeliers, the hanging cauldron and a cage
    table(sr, 17, st, 15, "south", 7, width=2)                        # x 16..17 (facing south: right is west), z 15..21
    for z in (16, 18, 20):
        sr.seat(15, st, z, "east", "chair")
        sr.seat(18, st, z, "west", "chair")
    candles(sr, 16, st + 1, 16, 4, "purple")
    sr.set_(16, st + 1, 20, "minecraft:brewing_stand", has_bottle_0="true", has_bottle_1="true", has_bottle_2="false")
    lamp_on(sr, 17, st + 1, 19)
    sr.set_(17, st + 1, 21, "minecraft:enchanting_table")
    for (x, z) in ((7, 12), (19, 12), (7, 20)):
        chandelier(sr, x, z, drop=6, arm=1, top=top)
    chandelier(sr, 13, 13, drop=6, arm=2, top=top)
    for y in range(BS + 1, top + 1):
        sr.set_(9, y, 17, CHAIN_BLOCK, axis="y")
    sr.set_(9, BS, 17, "minecraft:lava_cauldron")
    L.cage(sr, 3, st, 21, w=3, d=3, h=4, skull=True)
    for y in range(st + 4, top + 1):
        sr.set_(4, y, 22, CHAIN_BLOCK, axis="y")

    # ---- the west wall: two niches with a chest, a lantern and a skull each
    for lat in (9, 19):
        niche(sr, "w", lat, st + 1, width=3, height=3)
        x, z = ww.xz(lat - 1, -1)
        sr.set(x, st + 1, z, "minecraft:chest", {"facing": "east", "type": "single", "waterlogged": "false"})
        x, z = ww.xz(lat + 1, -1)
        sr.set(x, st + 1, z, LANTERN, {"hanging": "false"})
        x, z = ww.xz(lat, -1)
        sr.set(x, st + 1, z, "minecraft:wither_skeleton_skull", {"rotation": skull_rot("east")})

    # ---- the way home on the south wall, with a brazier on each side; banners; a purple-glass window slit
    exit_gate(sr, "s", 20)
    sr.arrival(*ws.xz(20, 3))
    brazier(sr, 16, st, 22, hint=True)
    brazier(sr, 23, st, 22, hint=False)
    for z in (16, 21):
        banner(sr, "e", z, 14, "purple")
    for lat in (5, 9):
        banner(sr, "s", lat, 14, "black")
    window_slit(sr, "s", 7, 11, glass="minecraft:purple_stained_glass")
    sr.patrol(13, 9)
    return sr


if __name__ == "__main__":
    r = landing_a()
    rep = r.finish_and_check()
    print(rep.text())

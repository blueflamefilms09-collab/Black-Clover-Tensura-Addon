#!/usr/bin/env python3
"""
The four working rooms of the Safe Zones (templates `structure/mega/safe/workshop_a`, `workshop_b`, `refectory`, `chapel`): where the people of a
region would actually live and work if the dungeon had people. Why they exist next to the halls: a rest is also a stop to repair, cook, pray and sleep,
and a safe room that is only a hall teaches players that safety is one kind of place; these four give a region a smithy, an artisans' hall, a kitchen
and a chapel, each with its own light, its own sounds in the mind, and its own rest anchors (the forge fire, the oven, the altar flame, the cots).
workshop_a / workshop_b are 2 x 3 modules (18 x 27), refectory and chapel 2 x 2 (18 x 18). All use the palette of `mega_lib` for architecture (so the
Region Theme restyles them) and anything vanilla for furniture. Doors are on module centres (offsets 4, 13 and 22) and every room has 2 to 4.
"""
import mega_safe_lib as S
from mega_safe_lib import *  # noqa: F401,F403
from mega_safe_halls import beams, crafting_row, potion_bench, SPRUCE_FENCE, SPRUCE_PLANKS, SPRUCE_SLAB, SPRUCE_STAIRS  # noqa: F401
from mega_safe_landing import candles, hay_pile, lamp_on, lectern, potted, statue  # noqa: F401


# ------------------------------------------------------------------------------------------------------------------------ shared pieces
def breast(sr, side, lat0, lat1, narrow0, narrow1, y_shoulder):
    """A masonry chimney breast against `side`'s wall from `lat0` to `lat1` (2 deep, floor to ceiling) that narrows to `narrow0..narrow1` above
    `y_shoulder` (the shoulder row is stairs whose high side is toward the wall, so it reads as a slope)."""
    w = Wall(sr, side)
    for lat in range(lat0, lat1 + 1):
        for y in range(sr.stand, sr.top + 1):
            for out in (0, 1):
                w.put(lat, y, out, BRICK)
    for lat in range(lat0, lat1 + 1):
        if narrow0 <= lat <= narrow1:
            continue
        for out in (0, 1):
            w.stairs(lat, y_shoulder, out, BRICK_STAIRS, high="wall")
            for y in range(y_shoulder + 1, sr.top + 1):
                w.clear(lat, y, out)


def firebox(sr, side, lat0, lat1, rows=3):
    """Carves the firebox of a breast: `lat0..lat1`, both layers, `rows` high above the raised hearth (the row at floor level stays stone), with a
    chiselled keystone in the lintel."""
    w = Wall(sr, side)
    for lat in range(lat0, lat1 + 1):
        for out in (0, 1):
            for y in range(sr.stand + 1, sr.stand + 1 + rows):
                w.clear(lat, y, out)
        w.put(lat, sr.stand + 1 + rows, 1, CHISELED if lat in ((lat0 + lat1) // 2, (lat0 + lat1 + 1) // 2) else POLISHED)


def ceiling_grid(sr, xs, zs, y=None):
    """Timber beams along x at each z in `zs` and along z at each x in `xs`, wall to wall, with corbels where the x beams meet the walls."""
    y = sr.top - 1 if y is None else y
    beams(sr, zs, y=y)
    for x in xs:
        for z in range(WALL, sr.D - WALL):
            if sr.t.get(x, y, z) is None:
                sr.set_(x, y, z, STRIPPED_LOG, axis="z")


def tool_rack(sr, x, y, z, along="x", length=3):
    """A rack of hanging tools against a wall: an iron-bar frame with trapdoor 'blades' in front of it and a lantern above."""
    for k in range(length):
        cx, cz = (x + k, z) if along == "x" else (x, z + k)
        sr.set_(cx, y, cz, IRON_BARS)
        sr.set_(cx, y + 1, cz, IRON_BARS)


# ------------------------------------------------------------------------------------------------------------------------ Workshop A: the smithy
def workshop_a():
    """The smithy, 2 x 3 modules (interior x 2..15, z 2..24). Plan, north up:
         N   the forge: a masonry breast across the north wall (x 5..12) with a firebox of two blast furnaces, two lava cauldrons and two campfires, an
             apron, two anvils and a quenching trough before it; a smithing table and a grindstone either side with a showpiece suit of armour
         mid two work tables with benches (x 3..6 and 11..14) either side of the runner, the crafting row and recipe shelves on the west wall, the racks
             of the store on the east wall; doors in the middle of both long sides
         S   the bunks: three beds along the south wall with chests and barrels between, the south door, a mess corner on the east wall."""
    sr = SR("mega/safe/workshop_a", 2, 3, h=9, wear=0.06)
    sr.expect = {"seats": 12, "hearths": 3, "beds_min": 3, "beds_max": 3, "patrol": (2, 2), "doors": ["e", "s", "w"], "armor_max": 4}
    for side in "esw":
        sr.door(side, 13)
    top = sr.top                                                      # 17
    st = sr.stand                                                     # 9
    wn, ww, we = Wall(sr, "n"), Wall(sr, "w"), Wall(sr, "e")

    # ---- floor: a polished runner from the south door to the forge, the cross aisle between the side doors
    for x in range(7, 11):
        for z in range(5, 25):
            sr.set_(x, FLOOR_ROW, z, POLISHED)
    for z in range(12, 15):
        for x in range(2, 16):
            sr.set_(x, FLOOR_ROW, z, POLISHED)
    for z in (6, 9, 12, 15, 18, 21):
        sr.set_(8, FLOOR_ROW, z, CHISELED)
        sr.set_(9, FLOOR_ROW, z, CHISELED)
    rug(sr, 7, 5, 10, 10, ["red", "orange"])

    # ---- architecture
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    ceiling_grid(sr, (), (8, 16, 22))
    for side, lat in (("w", 11), ("w", 15), ("e", 11), ("e", 15), ("s", 11)):
        pilaster(sr, side, lat)

    # ---- the forge
    breast(sr, "n", 5, 12, 7, 10, 15)
    firebox(sr, "n", 6, 11)
    x, y, z = wn.at(6, st + 1, 0)
    sr.hearth(x, y, z, block="minecraft:blast_furnace", facing="south", hint="stove")
    x, y, z = wn.at(11, st + 1, 0)
    sr.hearth(x, y, z, block="minecraft:blast_furnace", facing="south", hint="stove")
    for lat in (7, 10):
        wn.put(lat, st + 1, 0, "minecraft:lava_cauldron")
    x, y, z = wn.at(8, st + 1, 0)
    sr.hearth(x, y, z, facing="south")
    x, y, z = wn.at(9, st + 1, 0)
    sr.set_(x, y, z, "minecraft:campfire", facing="south", lit="true", signal_fire="false", waterlogged="false")
    for lat in (6, 11):
        wn.put(lat, st + 1, 1, "minecraft:coal_block")
    for lat in range(6, 12):
        wn.put(lat, st, 2, POLISHED_SLAB, type="bottom")              # the apron
    for lat, face in ((7, "east"), (10, "west")):
        wn.put(lat, st, 4, "minecraft:anvil", facing=face)
    for lat in (8, 9):
        wn.put(lat, st, 4, "minecraft:water_cauldron", level="3")
    for lat in (3, 14):
        statue(sr, *wn.at(lat, st, 2), "south")
    wn.put(4, st, 0, "minecraft:smithing_table")
    wn.put(13, st, 0, "minecraft:smithing_table")
    wn.put(3, st, 0, "minecraft:grindstone", face="floor", facing="south")
    wn.put(14, st, 0, "minecraft:grindstone", face="floor", facing="south")
    wn.barrel(2, st, 0, hint=True)
    wn.barrel(15, st, 0)
    for lat in (3, 4, 13, 14):
        wall_lamp(sr, "n", lat, 12)
    banner(sr, "n", 2, 14, "red")
    banner(sr, "n", 15, 14, "red")

    # ---- the work tables with benches, either side of the runner
    for x0 in (3, 11):
        table(sr, x0, st, 8, "east", 4, width=2)                      # x0..x0+3, z 8..9 (facing east: right is south)
        for x in range(x0, x0 + 4):
            sr.seat(x, st, 7, "south", "chair")
            sr.seat(x, st, 10, "north", "chair")
        candles(sr, x0 + 1, st + 1, 8, 2, "orange")
        lamp_on(sr, x0 + 2, st + 1, 9)
        sr.set_(x0 + 3, st + 1, 8, "minecraft:anvil", facing="north")
    for x, z in ((5, 8), (13, 8)):
        hang_chandelier(sr, x, z)

    # ---- the west wall: the crafting row with recipe shelves, the east wall: the store
    crafting_row(sr, "w", 5, [("minecraft:crafting_table", {}), ("minecraft:fletching_table", {}), ("minecraft:stonecutter", {"facing": "room"}),
                              ("minecraft:cartography_table", {}), ("minecraft:loom", {"facing": "room"}), ("minecraft:grindstone", {"face": "floor", "facing": "room"}),
                              ("minecraft:barrel", {"facing": "up", "open": "false"})])
    for lat in range(5, 12):
        for y in (st + 3, st + 4):
            if not (lat in (6, 9) and y == st + 3):                   # the two wall lamps below take their places in the lower row
                ww.put(lat, y, 0, "minecraft:bookshelf")
    for lat in (6, 9):
        wall_lamp(sr, "w", lat, st + 2)
    for lat in range(5, 12):
        for y in (st, st + 1):
            we.put(lat, y, 0, "minecraft:barrel", facing="up" if y == st else "east", open="false")
    we.hint(7, st, 0, "storage")
    we.hint(10, st, 0, "storage")
    for lat in (6, 10):
        wall_lamp(sr, "e", lat, st + 3)
    for lat in (6, 9):
        banner(sr, "e", lat, 14, "orange")

    # ---- the middle of the south half: a long mess table on the west half, a bench along the east wall
    table(sr, 5, st, 17, "south", 4, width=2)                         # x 4..5, z 17..20
    for z in range(17, 21):
        sr.seat(3, st, z, "east", "chair")
    for z in (17, 20):
        sr.seat(6, st, z, "west", "chair")
    candles(sr, 4, st + 1, 18, 3, "orange")
    sr.set_(5, st + 1, 20, "minecraft:flower_pot")                    # not on the middle slab (5, st, 19): its table marker stands above it
    for z in range(17, 21):
        we.seat(z, st, 0, "sofa")
    we.barrel(16, st, 0, hint=True)
    we.barrel(21, st, 0)
    for lat in (18, 20):
        wall_lamp(sr, "e", lat, st + 3)

    # ---- the bunks along the south wall
    for bx, color in ((3, "red"), (6, "blue"), (9, "green")):
        sr.bed(bx, st, 23, "south", color)
    for x in (4, 7):                                                  # a footlocker beside each of the first two beds, a barrel in the gap behind it
        sr.set_(x, st, 23, "minecraft:chest", facing="west", type="single", waterlogged="false")
        sr.set_(x, st, 24, "minecraft:barrel", facing="up", open="false")
    sr.hint(7, st, 24, "storage")
    sr.set_(5, st, 24, "minecraft:barrel", facing="up", open="false")
    sr.set_(8, st, 24, "minecraft:barrel", facing="up", open="false")
    lamp_on(sr, 8, st + 1, 24)
    candles(sr, 4, st + 1, 24, 2, "orange")
    wall_lamp(sr, "s", 5, 12)
    wall_lamp(sr, "s", 8, 12)
    banner(sr, "s", 3, 14, "blue")
    banner(sr, "s", 6, 14, "blue")
    banner(sr, "s", 9, 14, "blue")
    for z in (20, 23):
        window_slit(sr, "w", z, 11)
    window_slit(sr, "e", 4, 11)
    window_slit(sr, "e", 22, 11)

    # ---- the armoury on the west wall, a rug over the cross aisle, hay by the last bunk, herbs under the beams
    statue(sr, 3, st, 15, "east", armour="iron")
    statue(sr, 3, st, 21, "east", armour="iron")
    rug(sr, 2, 12, 15, 14, ["brown", "orange"])
    hay_pile(sr, [(10, st, 24), (10, st + 1, 24), (10, st, 23)])
    herbs(sr, ((4, 8), (11, 8), (6, 16), (11, 16), (4, 22), (12, 22)))

    # ---- light and the walkers
    for x, z in ((5, 16), (12, 16), (5, 22), (12, 20)):
        hang_chandelier(sr, x, z)
    sr.patrol(8, 12)
    sr.patrol(9, 19)
    cobweb(sr, 3, top - 1, 3)
    cobweb(sr, 14, top - 1, 24)
    return sr


# ------------------------------------------------------------------------------------------------------------------------ Workshop B: the artisans' hall
def workshop_b():
    """The artisans' hall, 2 x 3 modules (interior x 2..15, z 2..24), one level, four doors. Plan, north up: bays of craft along both long walls,
       a communal table down the middle and a hearth ring south of it:
         west wall   weaving (looms, wool, dye vats), pottery (a kiln of furnace and smoker, shelves of pots), [west door], carpentry (saws, benches,
                     logs), brewing (an alchemy bench with potion shelves)
         east wall   the scribe (shelves, a lectern, a map table), the tailor (looms, a mannequin), [east door], the tannery (vats, a smoking rack,
                     barrels), and from there along the south wall the dormitory: three beds with chests between
         centre      the long table (x 8..9, z 4..10) with eight chairs and a chair at each end, the hearth ring (a raised campfire, benches on four sides)."""
    sr = SR("mega/safe/workshop_b", 2, 3, h=9, wear=0.06)
    sr.expect = {"seats": 14, "hearths": 4, "beds_min": 3, "beds_max": 3, "patrol": (2, 2), "doors": ["n", "e", "s", "w"], "armor_max": 4}
    sr.door("n", 13)
    sr.door("s", 4)
    sr.door("e", 13)
    sr.door("w", 13)
    top = sr.top                                                      # 17
    st = sr.stand                                                     # 9
    wn, ww, we = Wall(sr, "n"), Wall(sr, "w"), Wall(sr, "e")

    # ---- floor: a chequer of the two deepslate stones with the warm runners over it
    L.checker_floor(sr, 2, 2, 15, 24, a=TILE, b=POLISHED)
    rug(sr, 7, 3, 10, 11, ["brown", "red"])
    rug(sr, 5, 15, 11, 19, ["orange", "brown"])
    rug(sr, 2, 12, 15, 14, ["brown", "orange"])

    # ---- architecture
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    ceiling_grid(sr, (6, 11), (7, 14, 21))
    for side, lat in (("w", 11), ("w", 15), ("e", 11), ("e", 15), ("n", 11), ("s", 7)):
        pilaster(sr, side, lat)
    for lat in (7, 20):
        pilaster(sr, "w", lat)
        pilaster(sr, "e", lat)

    # ---- west wall: weaving (z 2..6)
    for lat in (3, 5):
        ww.put(lat, st, 0, "minecraft:loom", facing=ww.away)
        banner(sr, "w", lat, 13, "red" if lat == 3 else "blue")
    ww.put(4, st, 0, "minecraft:white_wool")
    ww.put(4, st + 1, 0, "minecraft:red_wool")
    ww.put(6, st, 0, "minecraft:water_cauldron", level="3")
    ww.put(2, st, 0, "minecraft:blue_wool")
    for lat in (3, 5):
        ww.put(lat, st + 1, 0, "minecraft:white_carpet")
    sr.set_(4, st, 4, "minecraft:white_wool")
    sr.set_(4, st + 1, 4, "minecraft:light_blue_wool")
    sr.seat(4, st, 3, "west", "chair", block=SPRUCE_STAIRS)

    # ---- west wall: pottery (z 8..11): a kiln, a smoker, shelves of pots and clay
    x, y, z = ww.at(8, st, 0)
    sr.hearth(x, y, z, block="minecraft:furnace", facing="east", hint="stove")
    x, y, z = ww.at(9, st, 0)
    sr.hearth(x, y, z, block="minecraft:smoker", facing="east", hint="stove")
    ww.put(10, st, 0, "minecraft:orange_terracotta")
    ww.put(10, st + 1, 0, "minecraft:white_terracotta")
    ww.put(11, st, 0, "minecraft:clay")
    ww.put(11, st + 1, 0, "minecraft:decorated_pot", facing=ww.away)
    for lat in range(8, 12):
        ww.put(lat, st + 3, 0, SPRUCE_SLAB, type="top")
    ww.put(8, st + 4, 0, "minecraft:decorated_pot", facing=ww.away)
    ww.put(9, st + 4, 0, "minecraft:potted_cornflower")
    ww.put(10, st + 4, 0, "minecraft:decorated_pot", facing=ww.away)
    ww.put(11, st + 4, 0, "minecraft:potted_fern")
    sr.set_(3, st, 9, SPRUCE_SLAB, type="top")
    sr.set_(3, st + 1, 9, "minecraft:flower_pot")
    sr.seat(4, st, 9, "west", "chair", block=SPRUCE_STAIRS)

    # ---- west wall: carpentry (z 15..19)
    for lat, block, props in ((15, "minecraft:stonecutter", {"facing": "room"}), (16, "minecraft:crafting_table", {}), (17, "minecraft:smithing_table", {}),
                              (18, "minecraft:fletching_table", {})):
        p = dict(props)
        if p.get("facing") == "room":
            p["facing"] = ww.away
        ww.put(lat, st, 0, block, **p)
    for y in (st, st + 1, st + 2):
        ww.put(19, y, 0, "minecraft:spruce_log", axis="x" if y != st + 1 else "z")
    tool_rack(sr, 2, st + 2, 15, along="z", length=4)
    sr.set_(3, st, 17, "minecraft:spruce_slab", type="top")
    sr.seat(4, st, 17, "west", "chair", block=SPRUCE_STAIRS)

    # ---- west wall: brewing (z 20..24)
    potion_bench(sr, "w", 20, 5)

    # ---- east wall: the scribe (z 2..6), the tailor (z 7..11)
    for lat in range(2, 7):
        for y in (st, st + 1, st + 2):
            if lat == 4 and y == st:
                continue
            we.put(lat, y, 0, "minecraft:bookshelf")
    sr.set_(14, st, 6, "minecraft:lectern", facing="west", has_book="true", powered="false")
    we.put(4, st, 0, "minecraft:cartography_table")
    sr.seat(13, st, 6, "east", "chair", block=SPRUCE_STAIRS)
    for lat in (8, 10):
        we.put(lat, st, 0, "minecraft:loom", facing=we.away)
    we.put(9, st, 0, "minecraft:white_wool")
    we.put(9, st + 1, 0, "minecraft:magenta_wool")
    we.put(7, st, 0, "minecraft:water_cauldron", level="3")
    we.put(11, st, 0, "minecraft:cauldron")
    sr.armor_stand(14, st, 9, yaw=270.0, head="minecraft:leather_helmet", chest="minecraft:leather_chestplate", legs="minecraft:leather_leggings",
                   feet="minecraft:leather_boots")
    for lat in (8, 10):
        banner(sr, "e", lat, 13, "magenta")

    # ---- east wall: the tannery (z 15..19)
    for lat in (15, 16):
        we.put(lat, st, 0, "minecraft:water_cauldron", level="3")
    x, y, z = we.at(17, st, 0)
    sr.hearth(x, y, z, block="minecraft:smoker", facing="west", hint="stove")
    we.barrel(18, st, 0, hint=True)
    we.chest(19, st, 0)

    # ---- the long table, its chairs, and the hearth ring south of it
    table(sr, 9, st, 4, "south", 7, width=2)                          # x 8..9, z 4..10
    for z in (5, 7, 9):
        sr.seat(7, st, z, "east", "chair", block=SPRUCE_STAIRS)
        sr.seat(10, st, z, "west", "chair", block=SPRUCE_STAIRS)
    sr.seat(8, st, 3, "south", "chair", block=SPRUCE_STAIRS)
    sr.seat(9, st, 11, "north", "chair", block=SPRUCE_STAIRS)
    candles(sr, 8, st + 1, 5, 3, "orange")
    candles(sr, 9, st + 1, 8, 2, "red")
    lamp_on(sr, 8, st + 1, 7)
    sr.set_(9, st + 1, 4, PLATE)
    sr.set_(8, st + 1, 10, "minecraft:flower_pot")
    for x in range(7, 10):
        for z in range(16, 19):
            sr.set_(x, st, z, BRICK)
    sr.hearth(8, st + 1, 17, facing="north")
    sr.set_(9, st + 1, 16, "minecraft:cauldron")
    sr.hint(9, st + 1, 16, "cooking_pot")
    sr.set_(7, st + 1, 18, "minecraft:barrel", facing="up", open="false")
    for x in (7, 8, 9):
        sr.seat(x, st, 15, "south", "sofa", block=SPRUCE_STAIRS)
    for z in (16, 17, 18):
        sr.seat(6, st, z, "east", "sofa", block=SPRUCE_STAIRS)
        sr.seat(10, st, z, "west", "sofa", block=SPRUCE_STAIRS)
    for x in (7, 8, 9):
        sr.seat(x, st, 19, "north", "sofa", block=SPRUCE_STAIRS)

    # ---- the dormitory in the south-east corner
    for bx, color in ((9, "red"), (12, "orange"), (15, "brown")):
        sr.bed(bx, st, 23, "south", color)
    for x in (10, 13):
        sr.set_(x, st, 24, "minecraft:chest", facing="north", type="single", waterlogged="false")
        sr.set_(x + 1, st, 24, "minecraft:barrel", facing="up", open="false")
    sr.hint(11, st, 24, "storage")
    wall_lamp(sr, "s", 10, st + 2)                                    # on brackets over the chests: a chest top is 14/16 high and cannot hold a lantern up
    wall_lamp(sr, "s", 13, st + 2)
    candles(sr, 14, st + 1, 24, 2, "orange")

    # ---- the walls between: the north wall display and the south wall by the door
    for lat, color in ((3, "red"), (9, "orange")):                    # the window slit of the north wall (lat 6, shutters at 5 and 7) sits between them
        banner(sr, "n", lat, 13, color)
    wn.barrel(2, st, 0)
    wn.put(3, st, 0, "minecraft:barrel", facing="up", open="false")
    wn.hint(3, st, 0, "storage")
    for lat in (4, 6, 8, 10):
        wn.put(lat, st, 0, "minecraft:bookshelf")
        wn.put(lat, st + 1, 0, "minecraft:bookshelf")
    for lat in (4, 8):                                                # (between the banners of the north wall, which hang at lat 3, 5, 7 and 9)
        wall_lamp(sr, "n", lat, 12)
    wall_lamp(sr, "s", 9, 12)
    wall_lamp(sr, "s", 14, 12)
    banner(sr, "s", 8, 13, "blue")
    banner(sr, "s", 13, 13, "blue")
    window_slit(sr, "n", 6, 13)
    window_slit(sr, "s", 11, 13)
    window_slit(sr, "w", 17, 13)
    window_slit(sr, "e", 4, 13)

    # ---- herbs drying from the beams, light, the walkers
    herbs(sr, ((4, 7), (13, 7), (4, 14), (13, 14), (4, 21), (13, 21), (8, 14)))
    for x, z in ((6, 7), (11, 7), (6, 14), (11, 14), (6, 21), (11, 21)):
        hang_chandelier(sr, x, z)
    sr.patrol(11, 8)
    sr.patrol(6, 13)
    cobweb(sr, 15, top - 1, 2)
    cobweb(sr, 2, top - 1, 24)
    return sr


# ------------------------------------------------------------------------------------------------------------------------ the refectory
def refectory():
    """The refectory, 2 x 2 modules (interior 2..15 both ways): the kitchen and the long table. Plan, north up:
         N wall   the kitchen: a masonry range in the middle (a smoker, a campfire, a pot and a furnace in the firebox), a counter of cutting board and
                  skillet and a barrel on each side of it
         centre   the long table (x 8..9, z 6..12), three chairs a side and a chair at each end
         W wall   the pantry: kegs and barrels two high, shelves of jars, herbs drying from the beams
         E wall   the cook's two cots with a chest and a barrel between them, behind the pilasters that frame the alcove
         S wall   two doors (x 3..5 and 12..14) and between them the buttery (casks on a rack), a hand basin in the south-west corner; E wall: a third door
                  (z 12..14) south of the cots."""
    sr = SR("mega/safe/refectory", 2, 2, h=8, wear=0.05)
    sr.expect = {"seats": 10, "hearths": 3, "beds_min": 2, "beds_max": 2, "patrol": (1, 2), "doors": ["e", "s", "s"], "armor_max": 2}
    sr.door("s", 4)
    sr.door("s", 13)
    sr.door("e", 13)
    top = sr.top                                                      # 16
    st = sr.stand                                                     # 9
    wn, ws, ww = Wall(sr, "n"), Wall(sr, "s"), Wall(sr, "w")

    # ---- floor: flagstones with a runner down the length of the table and a hearthstone before the range
    for z in range(2, 16):
        for x in range(2, 16):
            sr.set_(x, FLOOR_ROW, z, POLISHED if (x + z) % 3 == 0 else TILE)
    rug(sr, 6, 5, 11, 13, ["brown", "red"])
    rug(sr, 6, 4, 11, 4, ["orange"])

    # ---- architecture
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    ceiling_grid(sr, (), (6, 10, 14))
    for side, lat in (("w", 5), ("w", 11), ("e", 5), ("e", 11), ("s", 6), ("s", 11)):
        pilaster(sr, side, lat)

    # ---- the kitchen range
    breast(sr, "n", 6, 11, 7, 10, 13)
    firebox(sr, "n", 7, 10, rows=3)
    x, y, z = wn.at(7, st + 1, 0)
    sr.hearth(x, y, z, block="minecraft:smoker", facing="south", hint="stove")
    x, y, z = wn.at(8, st + 1, 0)
    sr.hearth(x, y, z, facing="south")
    wn.put(9, st + 1, 0, "minecraft:cauldron")
    wn.hint(9, st + 1, 0, "cooking_pot")
    x, y, z = wn.at(10, st + 1, 0)
    sr.hearth(x, y, z, block="minecraft:furnace", facing="south", hint="stove")
    for lat in (7, 10):
        wn.put(lat, st + 1, 1, "minecraft:coal_block")
    for lat in range(7, 11):
        wn.put(lat, st, 2, POLISHED_SLAB, type="bottom")              # the hearth apron

    # ---- the counters either side of the range: board, skillet, sink, barrels
    for lat, what in ((4, "board"), (5, "skillet"), (12, "board"), (13, "skillet")):
        x, y, z = wn.put(lat, st, 0, PLANKS)
        if what == "board":
            sr.set_(x, st + 1, z, PLATE)
            sr.hint(x, st + 1, z, "cutting_board")
        else:
            sr.set_(x, st + 1, z, "minecraft:iron_trapdoor", facing="south", half="bottom", open="false")
            sr.hint(x, st + 1, z, "skillet")
    wn.put(3, st, 0, "minecraft:water_cauldron", level="3")
    wn.put(14, st, 0, "minecraft:water_cauldron", level="3")
    for lat in (3, 14):
        for y in (st + 2, st + 3):
            wn.put(lat, y, 0, "minecraft:bookshelf")
    for lat in (4, 5, 12, 13):
        wn.put(lat, st + 3, 0, SPRUCE_SLAB, type="top")
    for lat in (4, 12):
        wn.put(lat, st + 4, 0, "minecraft:decorated_pot", facing=wn.away)
    for lat in (5, 13):
        wn.put(lat, st + 4, 0, "minecraft:potted_fern")

    # ---- the long table
    table(sr, 9, st, 6, "south", 7, width=2)                          # x 8..9, z 6..12
    for z in (7, 9, 11):
        sr.seat(7, st, z, "east", "chair", block=SPRUCE_STAIRS)
        sr.seat(10, st, z, "west", "chair", block=SPRUCE_STAIRS)
    for x in range(7, 11):                                            # the bench at the head of the table, between the range and the table
        sr.seat(x, st, 5, "south", "sofa", block=SPRUCE_STAIRS)
    sr.seat(9, st, 13, "north", "chair", block=SPRUCE_STAIRS)
    candles(sr, 8, st + 1, 7, 3, "orange")
    candles(sr, 9, st + 1, 10, 3, "orange")
    sr.set_(8, st + 1, 11, PLATE)
    sr.set_(9, st + 1, 7, PLATE)
    sr.set_(9, st + 1, 12, "minecraft:flower_pot")
    sr.set_(8, st + 1, 6, "minecraft:potted_fern")                    # a plant on the near end of the table, a pot of flowers on the far end
    hang_chandelier(sr, 9, 10, arm=1)
    hang_chandelier(sr, 8, 6, arm=1)

    # ---- the pantry on the west wall
    for lat in range(3, 12):
        ww.put(lat, st, 0, "minecraft:barrel", facing="up", open="false")
    for lat in (4, 7, 10):
        ww.hint(lat, st, 0, "storage")
    for lat in (3, 5, 6, 8, 9, 11):
        ww.put(lat, st + 1, 0, "minecraft:barrel", facing="east", open="false")
    for lat in range(3, 12):
        ww.put(lat, st + 3, 0, "minecraft:bookshelf")
    for lat in (4, 8):
        sr.set_(*ww.at(lat, st + 4, 0), LANTERN, hanging="false")
    herbs(sr, ((4, 6), (4, 10), (13, 6), (13, 10), (6, 14), (11, 14)))

    # ---- the east wall: the cook's two cots behind the pilasters
    sr.bed(14, st, 7, "east", "red")
    sr.bed(14, st, 10, "east", "blue")
    sr.set_(14, st, 8, "minecraft:chest", facing="west", type="single", waterlogged="false")
    sr.set_(14, st, 9, "minecraft:barrel", facing="up", open="false")
    lamp_on(sr, 14, st + 1, 9)
    for lat in (8, 9):
        banner(sr, "e", lat, 12, "red")

    # ---- the south wall: the buttery between the doors, the basin by the west door
    for lat in range(7, 11):
        for y in (st, st + 1, st + 2):
            ws.put(lat, y, 0, "minecraft:barrel", facing="north" if y != st else "up", open="false")
    ws.hint(8, st, 0, "storage")
    wall_lamp(sr, "s", 8, 12)
    ww.put(13, st, 0, "minecraft:water_cauldron", level="3")           # the hand basin in the south-west corner
    banner(sr, "w", 13, 12, "red")
    for lat in (12, 14):
        wall_lamp(sr, "w", lat, 11)

    # ---- the walkers
    sr.patrol(12, 8)
    cobweb(sr, 3, top - 1, 15)
    return sr


# ------------------------------------------------------------------------------------------------------------------------ the chapel
SOUL_BRAZIER_BARS = ((1, 0), (-1, 0), (0, 1), (0, -1))
GLASS_COLORS = ("blue", "light_blue", "purple", "magenta", "cyan")


def soul_brazier(sr, x, y, z, hint="brazier"):
    """A sacred flame: a polished post, a lit SOUL campfire on it (the `hearth` anchor) and a ring of iron bars round the fire. (x, y, z) is the post.
    Why a soul campfire: it is a real campfire the rest system can use, burns blue (so the chapel does not look like the kitchen), and its hint is not a
    stove, so the compat processor never turns an altar flame into a Farmer's Delight stove."""
    sr.set_(x, y, z, POLISHED_WALL)
    sr.hearth(x, y + 1, z, block="minecraft:soul_campfire", hint=hint)
    for dx, dz in SOUL_BRAZIER_BARS:
        sr.set_(x + dx, y + 1, z + dz, IRON_BARS)


def glowing_window(sr, side, lat, y0, height, glass, light_level=12):
    """A tall window slit whose coloured glass glows: the glass is in the outer wall layer, the inner layer is carved, and a `light` block (invisible
    in survival) hangs in the middle of the carved slit, so the glass is lit from its front and the nave is lit by it. Why the light block: coloured
    glass in a windowless dungeon is dark glass; the hidden light is what makes it read as a window."""
    window_slit(sr, side, lat, y0, height=height, glass=glass, shutters=False, sill=False, lintel=True)
    x, z = Wall(sr, side).xz(lat, -1)
    sr.set(x, y0 + height // 2, z, "minecraft:light", {"level": str(light_level), "waterlogged": "false"})


def rose_window(sr, side, lat_c, y_c, radius, light_level=14):
    """A round window of coloured glass above the altar: a disc of cells round (lat_c, y_c) carved from the inner wall layer, glass in the outer layer
    (yellow in the middle, then orange, then blue, magenta and purple petals), and a hidden light block at the centre."""
    w = Wall(sr, side)
    cells = []
    for lat in range(int(lat_c - radius) - 1, int(lat_c + radius) + 2):
        for y in range(int(y_c - radius) - 1, int(y_c + radius) + 2):
            d = ((lat - lat_c) ** 2 + (y - y_c) ** 2) ** 0.5
            if d <= radius:
                cells.append((lat, y, d))
    for lat, y, d in cells:
        petal = int((lat * 3 + y * 5) % 3)
        if d < 1.0:
            glass = "yellow"
        elif d < 2.0:
            glass = "orange"
        else:
            glass = ("blue", "magenta", "purple")[petal]
        w.clear(lat, y, -1)
        x, z = w.xz(lat, -2)
        sr.set(x, y, z, "minecraft:%s_stained_glass" % glass)
    for lat, y, d in cells:
        if d < 0.8:
            x, z = w.xz(lat, -1)
            sr.set(x, y, z, "minecraft:light", {"level": str(light_level), "waterlogged": "false"})


def chapel():
    """The chapel, 2 x 2 modules (interior 2..15 both ways, 11 high): a nave with pews, a chancel with an altar, a rose window and two sacred flames.
       Plan, north up:
         N   the chancel: a dais one step up (x 4..13, z 2..5) with the altar of quartz in the middle, a lectern, two soul-fire braziers, banners and,
             high above the altar, the rose window of glowing glass
         mid the nave: an aisle 4 wide (x 7..10) with a runner, three pews of three seats a side (x 4..6 and 11..13, rows z 7, 9, 11, facing the altar);
             along both side walls tall windows of glowing glass between pilasters, and in the walls three alcoves with a pilgrim's cot each
         S   the narthex (z 12..15): the three doors (south x 12..14, west and east z 12..14), a font by the west corner and a bench along the south wall."""
    sr = SR("mega/safe/chapel", 2, 2, h=11, wear=0.04)
    sr.expect = {"seats": 20, "hearths": 2, "beds_min": 3, "beds_max": 3, "patrol": (1, 2), "doors": ["e", "s", "w"], "armor_max": 2}
    sr.door("s", 13)
    sr.door("w", 13)
    sr.door("e", 13)
    top = sr.top                                                      # 19
    st = sr.stand                                                     # 9
    ws, ww, we = Wall(sr, "s"), Wall(sr, "w"), Wall(sr, "e")

    # ---- floor: the runner of the aisle, a cross of chiselled stone where the nave meets the chancel
    for z in range(6, 16):
        for x in range(7, 11):
            sr.set_(x, FLOOR_ROW, z, POLISHED)
    for x in range(2, 16):
        sr.set_(x, FLOOR_ROW, 6, POLISHED)
    for x in (8, 9):
        for z in (7, 10, 13):
            sr.set_(x, FLOOR_ROW, z, CHISELED)
    rug(sr, 7, 7, 10, 15, ["blue", "light_blue"])

    # ---- architecture: cornice, plinth, corner pillars, the pilasters of the arcade, the trusses
    cornice(sr)
    plinth(sr)
    corner_pillars(sr)
    for side in "we":
        for lat in (6, 10):
            pilaster(sr, side, lat)
    beams(sr, (6, 10, 14))
    for x in (5, 12):
        for z in range(WALL, sr.D - WALL):
            if sr.t.get(x, top - 1, z) is None:
                sr.set_(x, top - 1, z, STRIPPED_LOG, axis="z")
    sr.set_(8, top - 1, 2, STRIPPED_LOG, axis="x")

    # ---- the chancel: the dais with a stair edge, the altar, the lectern, the sacred flames
    for x in range(4, 14):
        for z in range(2, 6):
            sr.set_(x, st, z, POLISHED)
        sr.set_(x, st, 6, POLISHED_STAIRS, facing="north", half="bottom")
    for x in (8, 9):                                                  # the altar: two deep and one high in quartz, candles on the back row, a cloth on the front, a chiselled frontal
        for z in (2, 3):
            sr.set_(x, st + 1, z, "minecraft:quartz_block")
        sr.set_(x, st + 1, 4, "minecraft:chiseled_quartz_block")
        sr.set_(x, st + 2, 3, "minecraft:white_carpet")
    candles(sr, 8, st + 2, 2, 3, "white")                            # on the full top of a quartz block: a candle on a bottom slab would float half a block above it
    candles(sr, 9, st + 2, 2, 2, "white")
    soul_brazier(sr, 6, st + 1, 3)
    soul_brazier(sr, 11, st + 1, 3)
    sr.set_(12, st + 1, 4, "minecraft:lectern", facing="west", has_book="true", powered="false")
    sr.set_(5, st + 1, 4, "minecraft:lectern", facing="east", has_book="true", powered="false")
    for x in (4, 13):
        for z in (3, 5):
            sr.set_(x, st + 1, z, "minecraft:soul_lantern", hanging="false")
    for x in (5, 12):
        sr.seat(x, st + 1, 2, "south", "chair", block="minecraft:quartz_stairs")

    # ---- the rose window and the banners of the north wall
    rose_window(sr, "n", 8.5, 15, 3.1)
    for lat in (4, 13):
        banner(sr, "n", lat, 15, "blue")
    for lat in (3, 14):
        banner(sr, "n", lat, 17, "white")

    # ---- the side walls: glowing windows between the pilasters and the alcoves of the cots
    for k, lat in enumerate((3, 7, 11)):
        glowing_window(sr, "w", lat, 12, 6, "minecraft:%s_stained_glass" % GLASS_COLORS[k % 5])
        glowing_window(sr, "e", lat, 12, 6, "minecraft:%s_stained_glass" % GLASS_COLORS[(k + 2) % 5])
    for z0, color in ((4, "white"), (8, "light_blue")):               # the cots of the west wall, in alcoves cut into the inner layer of the wall
        for lat in (z0, z0 + 1):
            for y in (st, st + 1, st + 2):
                ww.clear(lat, y, -1)
        sr.bed(WALL - 1, st, z0, "south", color)
        sr.set_(WALL - 1, st + 2, z0 + 1, LANTERN, hanging="true")
    for lat in (8, 9):                                                # the cot of the east wall
        for y in (st, st + 1, st + 2):
            we.clear(lat, y, -1)
    sr.bed(sr.W - WALL, st, 8, "south", "light_blue")
    sr.set_(sr.W - WALL, st + 2, 9, LANTERN, hanging="true")
    for lat in (4, 5):                                                # the pilgrims' library bay opposite: shelves and a reading lectern
        for y in (st, st + 1, st + 2):
            we.put(lat, y, 0, "minecraft:bookshelf")
    sr.set_(sr.W - WALL - 2, st, 4, "minecraft:lectern", facing="east", has_book="true", powered="false")
    sr.seat(sr.W - WALL - 3, st, 4, "east", "chair", block="minecraft:quartz_stairs")
    for side, lat in (("w", 5), ("e", 5)):
        wall_lamp(sr, side, lat, 12)

    # ---- the pews: three rows of three seats either side of the aisle, facing the altar
    for z in (7, 9, 11):
        for x in (4, 5, 6, 11, 12, 13):
            sr.seat(x, st, z, "north", "sofa")

    # ---- the narthex: a font, a bench, the candle stand
    ws.put(3, st, 0, "minecraft:water_cauldron", level="3")           # the font, with a banner over it
    banner(sr, "s", 3, 12, "white")
    for x in range(4, 9):
        sr.seat(x, st, 15, "north", "sofa")
    sr.set_(9, st, 15, SPRUCE_SLAB, type="top")                       # a candle stand at the end of the bench
    candles(sr, 9, st + 1, 15, 4, "white")

    # ---- light and the walkers
    for x, z in ((5, 6), (12, 6), (5, 10), (12, 10), (8, 14)):        # none over the narthex doors: a bar at row 12 would be in their 4-high approach
        hang_chandelier(sr, x, z, arm=1, lantern=SOUL_LANTERN if z == 6 else LANTERN)
    for lat in (6, 10):
        wall_lamp(sr, "s", lat, 12)
    sr.patrol(8, 13)
    herbs(sr, ((6, 14), (11, 14)))
    cobweb(sr, 3, top - 1, 3)
    cobweb(sr, 14, top - 1, 14)
    return sr


if __name__ == "__main__":
    r = chapel()
    print(r.finish_and_check().text())

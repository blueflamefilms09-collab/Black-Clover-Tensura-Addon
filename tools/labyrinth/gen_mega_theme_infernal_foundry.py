#!/usr/bin/env python3
"""
Why: the INFERNAL_FOUNDRY theme (1.59.0, THEME-D, docs/labyrinth/mega_v2_thd.md): the Nine Hells as a factory, the evil road's industrial level. A themed sector must read as a NEW place,
so besides the palette swap of `data/nusmp/mega/themes/infernal_foundry.json` (red nether brick walls on deepslate-tile floors, polished blackstone brick trim, gilded blackstone keystones)
it owns its signature rooms, which the assembler may draw only in sectors of this theme (`data/nusmp/mega/template_themes/infernal_foundry.json`, prefix
`nusmp:mega/<folder>/infernal_foundry_`).

    python3 tools/labyrinth/gen_mega_theme_infernal_foundry.py                 # build, check and write every room
    python3 tools/labyrinth/gen_mega_theme_infernal_foundry.py grand_furnace   # only this one
    python3 tools/labyrinth/gen_mega_theme_infernal_foundry.py --check         # build and check in memory, compare with the files on disk (CI style)

Rooms: grand_furnace (hall, two storeys with a lava stack), penal_forge (hall), contract_hall (puzzle), cage_arena (trial), soul_wheel (set piece), smelters_rest (safe), counting_house
(treasure). Lava is only ever put in cells carved from solid blocks and nothing flammable stands within three cells of it (the generator proves both); metal stands in for wood.
Deterministic: every random choice comes from the template's own name (`MT.rng`).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r2 as D  # noqa: E402
import thd_kit as K  # noqa: E402
import gen_mega_rooms2 as G2  # noqa: E402
from thd_kit import FR, GROUND, put, box, clear, st, dist, candles  # noqa: E402

ID = "infernal_foundry"
ROOMS = []   # (folder, name, builder, spec)
LAVA = "minecraft:lava"
LAVA_PROPS = {"level": "0"}
GLASS = "minecraft:orange_stained_glass"


def room(folder, name, **spec):
    """Registers a builder `fn(full_name) -> MT`; `spec` is what the checks expect of the room (see gen_mega_rooms2.check_room)."""
    def deco(fn):
        ROOMS.append((folder, ID + "_" + name, fn, spec))
        return fn
    return deco


def tpl(folder, name, mx, mz):
    return K.new(folder, name, mx, mz)


def lava_box(mt, x0, x1, y0, y1, z0, z1):
    """Fills the inclusive box with lava (source blocks). The caller has built every wall of the box first: the checks refuse a lava cell with an open side."""
    for y in range(y0, y1 + 1):
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                mt.set(x, y, z, LAVA, LAVA_PROPS)


def hanging_ladle(mt, x, y, z, ceiling_row):
    """A lava cauldron on a chain from the ceiling row `ceiling_row` (a block there): a ladle of the foundry, glowing over the floor. A lava cauldron is a light of 15 and a
    plain block to the engine: it does not leak."""
    for yy in range(y + 1, ceiling_row):
        put(mt, x, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
    put(mt, x, y, z, "minecraft:lava_cauldron")


def chain_cage(mt, x, z, y_solid, drop=6, skull="wither_skeleton"):
    """An iron cage with a skull in it, hung by one chain from the solid cell at row `y_solid` (the kit's cage has bars underneath, so the skull rests on something)."""
    K.hanging_cage(mt, x, z, y_solid, drop=drop, skull=skull)


def anvil(mt, x, y, z, facing="east"):
    """An anvil on the floor cell (x, y - 1, z) (a gravity block needs a solid block under it): facing is the long axis's direction."""
    return put(mt, x, y, z, "minecraft:anvil", {"facing": facing})


def furnace(mt, x, y, z, facing, kind="blast_furnace"):
    """A lit furnace written the way the other builders write them (with its fuel time, so it starts lit)."""
    import nbt as _nbt
    data = _nbt.Compound({"id": "minecraft:%s" % kind, "BurnTime": _nbt.Short(32767), "CookTime": _nbt.Short(0), "CookTimeTotal": _nbt.Short(100)})
    return put(mt, x, y, z, "minecraft:%s" % kind, {"facing": facing, "lit": "true"}, nbt_=data)


# =====================================================================================================================
# 1. grand_furnace: a lava stack with a gantry, two storeys
# =====================================================================================================================
def furnace_stack(mt, cx, cz, top):
    """The furnace itself: a base nine across and six high with a lava chamber (seven across, five high) behind windows of orange glass on all four faces, and over it a shaft five
    across up to the roof whose three-by-three core is a column of lava rising out of the chamber, with slits of glass in two bands. Every lava cell is enclosed by brick, polished stone
    or glass, and the lava column ends against the ceiling. Polished corner posts and a plinth ring make the base read as built."""
    bx0, bx1, bz0, bz1 = cx - 4, cx + 4, cz - 4, cz + 4
    box(mt, bx0, bx1, FR + 1, FR + 6, bz0, bz1, L.BRICK)
    for x in range(bx0, bx1 + 1):
        for z in range(bz0, bz1 + 1):
            if x in (bx0, bx1) and z in (bz0, bz1):
                box(mt, x, x, FR + 1, FR + 6, z, z, L.POLISHED, over=True)
            elif x in (bx0, bx1) or z in (bz0, bz1):
                put(mt, x, FR + 1, z, L.POLISHED, over=True)
    clear(mt, cx - 3, cx + 3, FR + 1, FR + 5, cz - 3, cz + 3)
    lava_box(mt, cx - 3, cx + 3, FR + 1, FR + 5, cz - 3, cz + 3)
    for off in (-1, 0, 1):
        for y in (FR + 2, FR + 3, FR + 4):
            for (x, z) in ((cx + off, bz0), (cx + off, bz1), (bx0, cz + off), (bx1, cz + off)):
                mt.set(x, y, z, GLASS)
    box(mt, cx - 2, cx + 2, FR + 7, top, cz - 2, cz + 2, L.BRICK)
    clear(mt, cx - 1, cx + 1, FR + 6, top, cz - 1, cz + 1)
    lava_box(mt, cx - 1, cx + 1, FR + 6, top, cz - 1, cz + 1)
    for y0 in (FR + 9, FR + 18):
        for y in range(y0, y0 + 5):
            for (x, z) in ((cx, cz + 2), (cx, cz - 2), (cx - 2, cz), (cx + 2, cz)):
                mt.set(x, y, z, GLASS)
    for k, y in enumerate((FR + 7, FR + 14, FR + 22, top)):
        for x in range(cx - 2, cx + 3):
            for z in range(cz - 2, cz + 3):
                if x in (cx - 2, cx + 2) or z in (cz - 2, cz + 2):
                    put(mt, x, y, z, L.POLISHED, over=True)


@room("hall", "grand_furnace", faction="demon", strict_light=True)
def grand_furnace(name):
    """Why: the brief's 'grand furnace' and the whole picture of the theme in one hall: a 27 x 36 foundry floor with a colossal furnace in the middle, a square stack of red brick
    whose base (nine across, six high) and shaft (five across, up to the roof) hold lava behind windows of orange glass, so the stack glows from the floor to the ceiling and lights the
    hall; round the shaft a gantry of iron grates at the second storey (a rail of iron bars, hung from the roof by four chains and bolted to the stack), bridges from it to two side platforms on the long walls, and long
    flights of stairs along the walls up to those platforms, carried on one stringer wall each so the space under them stays open; under the platforms the forge bays (lit blast
    furnaces in the wall, anvils, grindstones, smithing tables, a lava cauldron), over the nave chains with lava ladles and cages with skulls. Nothing flammable stands within three
    blocks of lava (the generator proves it): the brick is red nether brick, the walkways iron. The demons work here; the platforms are where their overseers watch from."""
    mt = tpl("hall", name, 3, 4)
    rng = mt.rng
    H = 26
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.12)
    top = FR + H
    for (side, off) in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    DECK = FR + 15                    # the gantry, the bridges and the platforms: the row of the last step of the flights
    CX, CZ = 13, 18
    # ---- the floor: iron plates (tile and polished), a runner of chiselled studs down the middle
    for z in range(2, 34):
        for x in range(2, 25):
            if (x + z) % 2 == 0 and rng.random() < 0.35:
                mt.set(x, FR, z, L.POLISHED)
        if z % 3 == 0:
            for x in (12, 14):
                mt.set(x, FR, z, L.CHISELED)
    furnace_stack(mt, CX, CZ, top)
    # ---- the gantry: a ring of iron grate round the shaft at DECK, a rail of bars on its outer edge, four corner pillars
    ring_cells = [(x, z) for z in range(12, 25) for x in range(7, 20) if not (11 <= x <= 15 and 16 <= z <= 20)]
    for (x, z) in ring_cells:
        K.grate(mt, x, DECK, z)
        if x in (7, 19) or z in (12, 24):
            put(mt, x, DECK + 1, z, L.IRON_BARS)
            put(mt, x, DECK + 2, z, L.IRON_BARS)
    for (x, z) in ((7, 12), (19, 12), (7, 24), (19, 24)):          # the gantry hangs from the roof by four chains and is bolted to the stack: no pillar stands in the walks
        for y in range(DECK + 3, top + 1):
            put(mt, x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
    # ---- the bridges to the side platforms, the platforms, and the flights up to them
    for west in (True, False):
        bx = 6 if west else 20
        px0, px1 = (2, 5) if west else (21, 24)
        for z in range(13, 16):
            K.grate(mt, bx, DECK, z)
        for z in (12, 16):
            put(mt, bx, DECK + 1, z, L.IRON_BARS)
            put(mt, bx, DECK + 2, z, L.IRON_BARS)
        for z in range(8, 19):
            for x in range(px0, px1 + 1):
                mt.set(x, DECK, z, L.TILE if (x + z) % 3 else L.POLISHED)
        edge = px1 if west else px0
        for z in range(8, 19):
            if not 13 <= z <= 15:
                put(mt, edge, DECK + 1, z, L.IRON_BARS)
                put(mt, edge, DECK + 2, z, L.IRON_BARS)
        for x in range(px0, px1 + 1):
            put(mt, x, DECK + 1, 8, L.IRON_BARS)
            put(mt, x, DECK + 2, 8, L.IRON_BARS)
        K.ramp(mt, px0, 33, "north", 14, FR + 1, width=4, stair=L.TILE_STAIRS, fill=L.BRICK, fill_cols=(3,) if west else (0,), rail_left=not west, rail_right=west)
    # ---- the forge bays under the platforms: lit blast furnaces in the walls, anvils, grindstones, smithing tables, a lava cauldron
    for (wx, face, ax) in ((2, "east", 4), (24, "west", 22)):
        for z in (9, 11, 13, 15, 17):
            furnace(mt, wx, FR + 1, z, face)
        for z in (10, 14, 18):
            anvil(mt, ax, FR + 1, z, "south")
        put(mt, wx, FR + 1, 8, "minecraft:smithing_table")
        put(mt, wx, FR + 1, 12, "minecraft:grindstone", {"face": "floor", "facing": "east" if wx == 2 else "west"})
        put(mt, wx, FR + 1, 16, "minecraft:lava_cauldron")
    # ---- over the nave: ladles and cages on chains from girders, lamps, banners
    for z in (8, 14, 26, 31):
        for x in range(3, 24):
            put(mt, x, top, z, L.POLISHED)
    for (x, z, drop) in ((6, 8, 7), (20, 8, 8), (6, 26, 7), (20, 26, 8), (9, 31, 6), (17, 31, 6)):
        hanging_ladle(mt, x, top - 1 - drop, z, top)
    for (x, z) in ((10, 26), (16, 26), (10, 8), (16, 8)):
        chain_cage(mt, x, z, top, drop=8)
    for z in (6, 30):
        for x in (6, 11, 15, 20):
            K.hang(mt, x, top + 1, z, 3, L.LANTERN)
    for z in (6, 22, 30):
        K.banner(mt, 2, FR + 11, z, "east", "red")
        K.banner(mt, 24, FR + 11, z, "west", "black")
    for x in (6, 20):
        K.banner(mt, x, FR + 9, 2, "south", "black")
        K.banner(mt, x, FR + 9, 33, "north", "red")
    # ---- markers
    K.auto_markers(mt, "demon", spawns=8, patrols=2, spacing=6, avoid=[(CX, CZ, 6)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 2. cage_arena: the trial of the holding pens
# =====================================================================================================================
def stall(mt, cx, cz, open_side, glyph=True):
    """A holding pen three across and three high on the floor, bars on three sides and on the top, open on `open_side` (a compass name: the side toward the middle of the arena),
    with a spawn glyph in the floor at its centre. The pen is where a wave of the Sealed Archway appears, so it must have an open side: a monster spawned in a closed cage would
    never come out."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            x, z = cx + dx, cz + dz
            side = None
            if dx == -1:
                side = "west"
            elif dx == 1:
                side = "east"
            if dz == -1:
                side = "north" if side is None else side
            elif dz == 1:
                side = "south" if side is None else side
            on_open = {"north": dz == -1, "south": dz == 1, "west": dx == -1, "east": dx == 1}[open_side]
            for y in range(FR + 1, FR + 4):
                if (dx or dz) and not on_open:
                    put(mt, x, y, z, L.IRON_BARS)
            put(mt, x, FR + 4, z, L.IRON_BARS)
    if glyph:
        mt.set(cx, FR, cz, G2.SPAWN_GLYPH)


@room("trial", "cage_arena", faction="demon", no_spawn=True, kind="trial")
def cage_arena(name):
    """Why: the brief's 'pit of cages' as a trial, the one fight of the dungeon that is fair by construction (docs/labyrinth/e1_trial.md): walk in and the Sealed Archway shuts both
    doors, the glyphs spawn the wave, the barriers fall when the last one dies. The stage is the infernal one: a round floor with a gilded seal in the middle (the party's anchor), eight
    holding pens of iron bars round it at radius nine, each open toward the middle and with a spawn glyph in its floor (so the wave comes out of the cages), four hanging cages with
    skulls on chains over the seal, braziers of soul fire at its corners, pillars with lamps, and a vault alcove opposite the door with the party's chest and the demon's statue
    watching from the other side. The wave is two piglin brutes and three wither skeletons (scaled by the party)."""
    mt = tpl("trial", name, 3, 3)
    rng = mt.rng
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("n", 13)
    mt.socket("s", 13)
    c = 13
    top = FR + 14
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, c, c)
            blk = L.TILE
            if d <= 4.5:
                blk = L.POLISHED
            elif 5.5 < d <= 6.5:
                blk = L.CHISELED if int(D.angle_of(x, z, c, c) / 22.5) % 2 == 0 else L.POLISHED
            elif abs(x - c) <= 1:
                blk = L.POLISHED
            elif rng.random() < 0.14:
                blk = L.CRACKED_TILE
            mt.set(x, FR, z, blk)
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, c, c)
            if d <= 3.5:
                mt.set(x, FR + 1, z, L.POLISHED)
            elif d <= 4.5:
                D.put(mt, x, FR + 1, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(c - x, c - z), "half": "bottom"})
    mt.set(c, FR + 1, c, L.CHISELED)
    glyphs = []
    for (x, z) in D.around(c, c, 9.0, 8, 22.5):
        inward = D.stair_facing(c - x, c - z)
        stall(mt, x, z, inward)
        glyphs.append((x, z))
    # the trial core writes the glyphs again (they are the pens' floors) and the archway keystone with the roster
    G2.trial_core(mt, [("minecraft:piglin_brute", 2), ("minecraft:wither_skeleton", 3)], rng, [("n", 13), ("s", 13)], FR + 12, glyphs)
    for k in range(4):
        x, z = D.polar(c, c, 3.4, 45.0 + 90.0 * k)
        D.brazier(mt, x, FR + 2, z, fire="soul_campfire", height=2, cage=False)
    for (x, z) in D.around(c, c, 6.0, 4, 0.0):
        K.hanging_cage(mt, x, z, top + 1, drop=5, skull="wither_skeleton")
    for (px, pz) in ((5, 5), (21, 5), (5, 21), (21, 21)):
        D.column(mt, px, pz, FR + 1, FR + 13)
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3)
    # the reward in an arched alcove of the west wall, braziers either side; the demon opposite
    f = D.inner_frame(mt, "w", 13)
    D.arched_alcove(mt, f, 5, 5)
    mt.t.chest(*f.at(0, 0, -1), K.LOOT, "east")
    for r in (-2, 2):
        D.candle_cluster(mt, *f.at(r, 0, -1), rng, colors=("red", "orange"), count=4)
    D.brazier(mt, *f.at(-4, 0, 0), cage=True)
    D.brazier(mt, *f.at(4, 0, 0), cage=True)
    f = D.inner_frame(mt, "e", 13)
    D.arched_alcove(mt, f, 5, 5)
    D.alcove_statue(mt, f, ("netherite", "netherite_sword"))
    for side in ("n", "s"):
        for lat in (6, 20):
            D.sconce(mt, D.inner_frame(mt, side, lat), 0, 5, skull="wither_skeleton")
            D.banner_on(mt, *D.inner_frame(mt, side, lat).at(2, 5, 0), "south" if side == "n" else "north", "red")
    for (x, z) in ((13, 6), (13, 20), (6, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(4, FR + 1, 4, "faction", faction="demon")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 3. penal_forge: the line where the damned are worked
# =====================================================================================================================
@room("hall", "penal_forge", faction="demon", strict_light=True)
def penal_forge(name):
    """Why: the brief's 'torture/penal forge' that is NOT one more torture chamber (the dungeon has two): a working line. A 27 x 27 hall with a trough of lava down its middle (seventeen
    long, three wide, carved from a solid slab so it cannot flow, crossed by two bridges of iron grate with a rail of bars on their open sides), over it on chains a row of cages with
    skulls at different heights and lava ladles (the damned are carried over the fire), on the west side four branding bays (a lit blast furnace in the wall, an anvil, a grindstone, a
    quench cauldron, bars between the bays), on the east side three armouries of the overseers (armoured stands in netherite with their weapons, an anvil, a chain hoist), and red
    banners. No wood, wool or hay within three blocks of the lava (the generator proves it). Where the party fights is the two lanes either side of the trough; the bridges are the gamble."""
    mt = tpl("hall", name, 3, 3)
    rng = mt.rng
    G2.lava_shell(mt, rng)
    top = FR + 14
    for (side, off) in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    trough = G2.rect(12, 14, 5, 21)
    bridges = G2.rect(12, 14, 9, 11) | G2.rect(12, 14, 15, 17)
    G2.lava_channel(mt, trough, bridges)
    for z in range(5, 22):
        for x in (11, 15):
            if not (9 <= z <= 11 or 15 <= z <= 17):
                put(mt, x, FR + 1, z, L.POLISHED_WALL, {"up": "true"})
    for z in (8, 12, 14, 18):                       # the open sides of the two bridges: a rail of bars over the lava, standing on the bank posts at its ends
        for x in (12, 13, 14):
            put(mt, x, FR, z, L.IRON_BARS)
            put(mt, x, FR + 1, z, L.IRON_BARS)
    # ---- the floor: iron plates with red stains of terracotta in the lanes
    for z in range(2, 25):
        for x in range(2, 25):
            if mt.get(x, FR, z) is not None and not (12 <= x <= 14 and 5 <= z <= 21):
                if rng.random() < 0.10:
                    mt.set(x, FR, z, "minecraft:red_terracotta")
                elif (x + z) % 2 == 0 and rng.random() < 0.3:
                    mt.set(x, FR, z, L.POLISHED)
    # ---- over the trough: girders, cages and ladles on chains
    for z in (6, 10, 13, 16, 20):
        for x in range(10, 17):
            put(mt, x, top, z, L.POLISHED)
    for (z, drop) in ((6, 6), (10, 8), (16, 5), (20, 7)):
        K.hanging_cage(mt, 13, z, top + 1, drop=drop, skull="wither_skeleton")
    for (x, z, drop) in ((11, 13, 5), (15, 13, 6)):
        hanging_ladle(mt, x, top - 1 - drop, z, top)
    # ---- the branding bays on the west side: four bays between bars
    for z0 in (4, 9, 14, 19):
        for x in range(2, 6):
            for y in (FR + 1, FR + 2, FR + 3):
                put(mt, x, y, z0 + 3, L.IRON_BARS)
        furnace(mt, 2, FR + 1, z0 + 1, "east")
        anvil(mt, 4, FR + 1, z0 + 1, "south")
        put(mt, 2, FR + 1, z0 + 2, "minecraft:grindstone", {"face": "floor", "facing": "east"})
        put(mt, 5, FR + 1, z0, "minecraft:water_cauldron", {"level": "3"})
        K.hang(mt, 3, top + 1, z0 + 2, 4, L.LANTERN)
    # ---- the overseers' armouries on the east side: three stands in netherite with their weapons, an anvil and a chain hoist each
    for z0 in (5, 12, 19):
        for dz in (0, 2):
            L.statue(mt, 23, FR + 1, z0 + dz, "west", head="minecraft:netherite_helmet", chest="minecraft:netherite_chestplate", legs="minecraft:netherite_leggings",
                     feet="minecraft:netherite_boots", weapon="minecraft:netherite_axe" if dz else "minecraft:netherite_sword")
        anvil(mt, 21, FR + 1, z0 + 1, "south")
        for y in range(top - 5, top + 1):
            put(mt, 21, y, z0 + 1, L.CHAIN_BLOCK, {"axis": "y"})
    # ---- banners, braziers, lamps
    for z in (6, 13, 20):
        K.banner(mt, 2, FR + 8, z, "east", "red")
        K.banner(mt, 24, FR + 8, z, "west", "black")
    for (x, z) in ((7, 3), (19, 3), (7, 23), (19, 23)):
        D.brazier(mt, x, FR + 1, z, cage=True)
    for (x, z) in ((7, 9), (7, 18), (19, 9), (19, 18)):
        D.lamp_post(mt, x, FR + 1, z, height=3, lantern=L.LANTERN)
    K.auto_markers(mt, "demon", spawns=7, patrols=2, spacing=6, avoid=[(13, 13, 3)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 4. contract_hall: a bargain you have to read
# =====================================================================================================================
#: the order the Riddle Stone of this hall asks for (colour numbers of the four sigil plates): fixed and unlike the other sigil halls' orders
CONTRACT_ORDER = (1, 3, 0, 2)


@room("puzzle", "contract_hall", faction="cultist", no_spawn=True, kind="puzzle")
def contract_hall(name):
    """Why: the brief's contract hall, 'lecterns with books: a puzzle that is a bargain'. The Nine Hells run on signed clauses, so the puzzle of this theme is a contract: a 27 x 18
    hall with a long table in the middle that carries the great contract (a writable book on a lectern, candles, an inkwell of cauldron), four sigil plates round it that are the four
    seals (red, blue, gold, green), a Riddle Stone by the south door that reads the order in which the seals must be pressed (it is not left to right), a row of clause lecterns along the
    north wall, each with a book and a plaque, and in the east end the vault of the bargain: a puzzle door of the Spire's kind (a Gatekeeper door) in a partition with a closet of two
    chests behind it. A wrong seal stings and resets the contract (the Spire's rule). Nothing here fights: it is a room to read in."""
    mt = tpl("puzzle", name, 3, 2)
    rng = mt.rng
    mt.room(12, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("w", 13)
    mt.socket("s", 13)
    top = FR + 12
    G2.sigil_closet(mt, L.Frame(mt, 18, FR + 1, 8, "east"), rng)
    # ---- the floor: the seal of the contract round the table
    for z in range(2, 16):
        for x in range(2, 19):
            if (x + z) % 2 == 0 and rng.random() < 0.4:
                mt.set(x, FR, z, L.POLISHED)
    for z in range(5, 13):
        for x in range(9, 16):
            d = dist(x, z, 12, 8.5)
            if 3.4 <= d <= 4.5 or d <= 1.0:
                mt.set(x, FR, z, L.CHISELED)
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for (x, z), c in zip(((10, 6), (14, 6), (10, 11), (14, 11)), colours):
        mt.set(x, FR + 1, z, G2.PLATE, {"color": str(c), "lit": "false"})
    mt.set(8, FR + 1, 14, G2.RIDDLE, None, {"id": G2.RIDDLE, "sequence": G2.nbt_list(list(CONTRACT_ORDER))})
    mt.set(8, FR + 2, 14, L.SOUL_LANTERN, {"hanging": "false"})
    K.sign(mt, 8, FR + 3, 15, "north", ["THE BARGAIN", "Read the stone.", "Press the seals", "as it reads."], block="minecraft:crimson_wall_sign")
    # ---- the table with the great contract: slabs on posts, two rows of chairs, candles, a lectern at the head
    for x in range(11, 14):
        mt.set(x, FR + 1, 8, L.POLISHED_SLAB, {"type": "top"})
        mt.set(x, FR + 1, 9, L.POLISHED_SLAB, {"type": "top"})
    for (x, z) in ((11, 8), (13, 9)):
        put(mt, x, FR, z, L.POLISHED)
    K.lectern(mt, 12, FR + 2, 8, "south")
    candles(mt, 11, FR + 2, 9, 3, "red")
    candles(mt, 13, FR + 2, 8, 2, "black")
    put(mt, 12, FR + 2, 9, "minecraft:cauldron")
    # ---- the clause lecterns along the north wall, each with a plaque on the wall above it
    clauses = ["I. THE SOUL", "II. THE DEBT", "III. THE TERM", "IV. THE PRICE", "V. THE WITNESS", "VI. THE SEAL"]
    for k, x in enumerate(range(3, 3 + 2 * len(clauses), 2)):
        if x >= 17:
            break
        K.lectern(mt, x, FR + 1, 2, "south")
        K.sign(mt, x, FR + 3, 2, "south", [clauses[k], "", "Signed in", "blood."], block="minecraft:crimson_wall_sign")
    # ---- chains, cages, braziers, banners, lamps
    for (x, z) in ((6, 5), (6, 11), (16, 5)):
        K.hanging_cage(mt, x, z, top + 1, drop=4, skull="wither_skeleton")
    for (x, z) in ((4, 8), (16, 14)):
        D.brazier(mt, x, FR + 1, z, fire="soul_campfire", cage=True)
    for (x, z) in ((8, 3), (14, 3), (8, 8), (16, 8), (8, 13), (14, 13), (4, 10), (17, 11)):
        K.hang(mt, x, top + 1, z, 5, L.LANTERN)
    for z in (6, 11):
        K.banner(mt, 2, FR + 6, z, "east", "red")
    for x in (8, 16):
        K.banner(mt, x, FR + 6, 15, "north", "black")
    mt.marker(7, FR + 1, 9, "patrol")
    mt.marker(15, FR + 1, 13, "patrol")
    mt.marker(4, FR + 1, 5, "faction", faction="cultist")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 5. soul_wheel: the great wheel of the Hells over a river of lava, the set piece
# =====================================================================================================================
def wheel_cells(cx, cy, r_out, r_in, hub_r, spokes_deg):
    """The cells (x, y) of a wheel drawn in one plane, symmetric about the line x = cx + 0.5 (cx is a half-integer: 17.5 is the line between the cells 17 and 18): the rim (a ring from
    `r_in` to `r_out`), the hub (a disc of radius `hub_r`) and the spokes, one four-connected line of cells per angle of `spokes_deg` (degrees, 0 = east, 90 = up; the angles of the
    right half are drawn and mirrored, so a list of the right half is enough). Returns (rim, hub, spokes) as sets of cells; a cell is in one of them only."""
    rim, hub, spokes = set(), set(), set()
    for y in range(int(cy - r_out) - 1, int(cy + r_out) + 2):
        for x in range(int(cx - r_out) - 2, int(cx + r_out) + 3):
            d = math.hypot(x - cx, y - cy)
            if r_in <= d <= r_out:
                rim.add((x, y))
            elif d <= hub_r:
                hub.add((x, y))
    mid = cx + 0.5
    for deg in spokes_deg:
        a = math.radians(deg)
        last = None
        t = hub_r - 0.4
        while t <= r_in + 0.6:
            cell = (int(math.floor(mid + t * math.cos(a))), int(math.floor(cy + 0.5 + t * math.sin(a))))
            if last is not None and cell[0] != last[0] and cell[1] != last[1]:
                spokes.add((cell[0], last[1]))
            spokes.add(cell)
            last = cell
            t += 0.3
    both = set()
    for (x, y) in spokes:
        both.add((x, y))
        both.add((int(2 * mid) - 1 - x, y))
    return rim, hub, both - rim - hub


@room("set", "soul_wheel", faction="demon", strict_light=True, route_width="wide")
def soul_wheel(name):
    """Why: every sector has one room the party remembers, and for the foundry it is the thing the whole factory is for: the Soul Wheel, a wheel twenty-three blocks across standing on
    its edge against the north wall of a 36 x 36 hall, half sunk in a river of lava, its rim a ring of iron blocks lined with polished stone, its twelve spokes iron bars, its hub a gilded boss with a glowstone eye,
    with soul lanterns hanging inside the rim (the souls that turn it). The river fills the north half of the hall; a bridge of iron grate six wide with a curb and rails on both sides
    crosses it to an island at the wheel's foot where the hall's chests stand between braziers and the demon's statues, behind a rail of wall posts (the lava is a step away from the
    island's edge, so the rail is not decoration). The south half is the party's arena: a rune circle in the floor, four soul stills (soul fire in an iron cage) along the side walls,
    cages on chains over the river banks, two pillars with lamps. Lava is only ever put in cells carved from the slab under the floor, and nothing flammable is within three cells of it."""
    mt = tpl("set", name, 4, 4)
    rng = mt.rng
    H = 26
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    for (side, off) in (("s", 13), ("s", 22), ("w", 22), ("e", 22)):
        mt.socket(side, off)
    # ---- the slab under the room, the river carved out of it (a lava cell is bounded by rock on every side but the top), the island and the bridge left standing
    for z in range(mt.D):
        for x in range(mt.W):
            for y in range(G2.LAVA_BOTTOM - 1, FR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))
    island = G2.rect(10, 25, 6, 9)
    deck = G2.rect(14, 21, 10, 17)
    river = {c for c in G2.rect(4, 31, 2, 17) if c not in island and c not in deck}
    for (x, z) in sorted(river):
        mt.remove(x, FR, z)
        for y in range(G2.LAVA_BOTTOM, G2.LAVA_TOP + 1):
            mt.set(x, y, z, LAVA, LAVA_PROPS)
    for (x, z) in sorted(deck):
        if x in (14, 21):
            mt.set(x, FR, z, L.POLISHED)                        # the curbs
        else:
            K.grate(mt, x, FR, z, over=True)                    # the walk, six wide, flush with the floor, the lava shining through it
    # ---- the floor of the arena: plates, a rune circle round (17.5, 26), the axis from the south doors to the bridge
    for z in range(18, 34):
        for x in range(2, 34):
            d = dist(x, z, 17.5, 26.0)
            if abs(d - 6.0) < 0.75 or d < 1.5:
                mt.set(x, FR, z, L.CHISELED)
            elif abs(d - 9.5) < 0.6 and int(D.angle_of(x, z, 17.5, 26.0) / 15.0) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
            elif x in (16, 17, 18, 19) and z < 20 + 1:
                mt.set(x, FR, z, L.POLISHED)
            elif (x + z) % 2 == 0 and rng.random() < 0.30:
                mt.set(x, FR, z, L.POLISHED)
    for z in range(2, 18):                                      # the banks of the river: plates too
        for x in list(range(2, 4)) + list(range(32, 34)):
            if (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    # ---- the wheel: hub on the line x = 17.5 at row FR + 10, so its lowest rim cells (row FR - 1) stand in the lava; rim, hub and spokes two cells thick (z = 2, 3) against the north wall
    cx, cy = 17.5, FR + 10
    rim, hub, spokes = wheel_cells(cx, cy, 11.3, 9.0, 2.6, [-90, -60, -30, 0, 30, 60, 90])
    for (x, y) in sorted(rim):                                  # the rim: iron on the outside (it must read against the red wall), polished stone inside
        outer = math.hypot(x - cx, y - cy) > 10.0
        for z in (2, 3):
            put(mt, x, y, z, "minecraft:iron_block" if outer else L.POLISHED, over=True)
    for (x, y) in sorted(hub):
        for z in (2, 3):
            put(mt, x, y, z, L.CHISELED, over=True)
    for (x, y) in sorted(spokes):
        for z in (2, 3):
            put(mt, x, y, z, L.IRON_BARS, over=True)
    for (x, y) in sorted(hub):                                  # the boss: a plus of three cells' reach in front of the hub, a glowstone eye in its middle
        if math.hypot(x - cx, y - cy) <= 1.7:
            put(mt, x, y, 4, L.CHISELED, over=True)
    for x in (17, 18):
        put(mt, x, cy, 4, "minecraft:glowstone", over=True)
    for deg in range(0, 360, 30):                               # iron teeth on the rim, the cogs of the wheel (only above the floor: the lower ones stand in the lava)
        a = math.radians(deg + 15.0)
        tx, ty = int(math.floor(18.0 + 12.1 * math.cos(a))), int(math.floor(cy + 0.5 + 11.6 * math.sin(a)))
        if ty > FR + 1:
            for z in (2, 3):
                put(mt, tx, ty, z, L.CHISELED)
    # souls: soul lanterns hanging inside the rim in the upper half (the arch rules want every hanging lantern to hang from something, so each one looks up for its support)
    for x in (9, 11, 13, 15, 20, 22, 24, 26):
        for y in range(FR + 14, FR + 4, -1):
            if mt.get(x, y, 3) is None and mt.get(x, y + 1, 3) is not None:
                D.hang_lantern_from_above(mt, x, y - 1, 3, L.SOUL_LANTERN, reach=3)
                break
    # ---- the island: a dais of two steps, the chests, the braziers, the demon's statues
    for x in range(14, 22):
        mt.set(x, GROUND, 7, L.POLISHED)
        mt.set(x, GROUND, 8, L.POLISHED_STAIRS, st("north"))
    for x in (13, 22):
        mt.set(x, GROUND, 7, L.POLISHED_STAIRS, st("east" if x == 22 else "west"))
    for x in (16, 19):
        mt.t.chest(x, GROUND + 1, 7, K.LOOT, "south")
    for x in (17, 18):
        candles(mt, x, GROUND + 1, 7, 3, "red")
    for (x, z) in ((11, 7), (24, 7)):
        D.brazier(mt, x, GROUND, z, fire="soul_campfire", cage=True)
    for x in (11, 24):
        L.statue(mt, x, GROUND, 8, "south", head="minecraft:netherite_helmet", chest="minecraft:netherite_chestplate", legs="minecraft:netherite_leggings",
                 feet="minecraft:netherite_boots", weapon="minecraft:netherite_sword")
    # ---- the rails: wall posts along the island's open edges and the banks, rails of bars on the bridge's curbs
    rail = set()
    for x in range(10, 26):
        rail.add((x, 6))
        if x < 14 or x > 21:
            rail.add((x, 9))
    for z in range(6, 10):
        rail.add((10, z))
        rail.add((25, z))
    for x in range(3, 33):
        if not 14 <= x <= 21:
            rail.add((x, 18))
    for z in range(2, 18):
        rail.add((3, z))
        rail.add((32, z))
    for (x, z) in sorted(rail):
        put(mt, x, GROUND, z, L.POLISHED_WALL, {"up": "true"})
    for z in range(10, 18):
        for x in (14, 21):
            put(mt, x, GROUND, z, L.POLISHED_WALL, {"up": "true"})
            put(mt, x, GROUND + 1, z, L.IRON_BARS)
    # ---- the arena: soul stills along the side walls, pillars with lamps, cages and chains over the river, lamps, banners
    for (sx, sz) in ((4, 28), (4, 32), (31, 28), (31, 32)):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                mt.set(sx + dx, FR, sz + dz, "minecraft:soul_soil")
                if dx or dz:
                    for y in range(GROUND, GROUND + 3):
                        put(mt, sx + dx, y, sz + dz, L.IRON_BARS)
                put(mt, sx + dx, GROUND + 3, sz + dz, L.IRON_BARS)
        put(mt, sx, GROUND, sz, "minecraft:soul_fire")
    for (px, pz) in ((9, 27), (26, 27)):
        D.column(mt, px, pz, GROUND, FR + 12)
        D.pillar_lantern(mt, px, pz, D.stair_facing(17.5 - px, 26.0 - pz), FR + 3, lantern=L.SOUL_LANTERN)
    for z in (6, 10, 14):
        for x in range(5, 31):
            put(mt, x, top, z, L.POLISHED)
    for (x, z, drop) in ((4, 8, 9), (31, 8, 9), (4, 12, 12), (31, 12, 12), (4, 16, 7), (31, 16, 7)):    # off the wheel's width, so nothing hides it from the south
        K.hanging_cage(mt, x, z, top + 1, drop=drop, skull="wither_skeleton")
    for x in (7, 28):
        for z in range(21, 33, 4):
            K.hang(mt, x, top + 1, z, 7, L.LANTERN)
    for z in (20, 27):
        K.banner(mt, 2, FR + 8, z, "east", "red")
        K.banner(mt, 33, FR + 8, z, "west", "red")
    for x in (6, 29):
        K.banner(mt, x, FR + 9, 33, "north", "black")
    # ---- markers: spawns in the arena and on the island (the chests are guarded), patrols on the bridge and in the arena, the middle of the rune circle
    K.auto_markers(mt, "demon", spawns=8, patrols=3, spacing=6, avoid=[(17.5, 26.0, 2.0)])
    mt.marker(12, GROUND, 8, "spawn", weight=2)
    mt.marker(23, GROUND, 8, "spawn")
    mt.marker(17, GROUND, 14, "patrol")
    mt.marker(17, GROUND, 26, "room_center")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 6. smelters_rest: the break room of the foundry, the calm room
# =====================================================================================================================
@room("safe", "smelters_rest", safe=True, seats=4, hearths=1, beds=3, min_light=6, route_width="wide")
def smelters_rest(name):
    """Why: a sector needs places to rest (the rest system reads seats, hearths and beds in a room of the `safe` folder), and the one place of a foundry where nobody is working is the
    break room of the smelters: 18 x 18, a campfire in a ring of polished plates with four benches round it and a cauldron on a chain hung over it (the smelters' kettle), three bunks with
    red and grey blankets against the west wall with a lantern on a barrel between them, the banked forge in the north wall (cold blast furnaces under a hood and a flue), a row of
    workbenches along the east wall (an anvil, a grindstone, a smithing table), ale barrels and a water cauldron by the south wall, and two plain notices on the north wall. It is warm and
    lit by honest lanterns, the one room of the theme with no lava, no cage and no chain that holds a skull. No spawn, no faction, no chest; the rest anchors (four seats, one hearth, three
    beds) are what the rest system reads."""
    mt = tpl("safe", name, 2, 2)
    H = 10
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.06)
    top = FR + H
    for (side, off) in (("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    # ---- the floor: a ring of polished plates round the hearth, four studs of gilded stone on its diagonals
    for z in range(2, 16):
        for x in range(2, 16):
            d = dist(x, z, 8.5, 8.5)
            if 2.6 <= d <= 4.6:
                mt.set(x, FR, z, L.POLISHED)
            elif (x, z) in ((7, 7), (10, 7), (7, 10), (10, 10)):
                mt.set(x, FR, z, L.CHISELED)
    # ---- the hearth: a campfire in a ring of low plates, four benches, the kettle on its chain
    for (x, z) in ((7, 7), (8, 7), (9, 7), (7, 8), (9, 8), (7, 9), (8, 9), (9, 9)):
        mt.set(x, GROUND, z, L.POLISHED_SLAB, {"type": "bottom"})
    K.hearth(mt, 8, GROUND, 8, block="minecraft:campfire")
    K.seat(mt, 8, GROUND, 5, "south")
    K.seat(mt, 8, GROUND, 11, "north")
    K.seat(mt, 5, GROUND, 8, "east")
    K.seat(mt, 11, GROUND, 8, "west")
    for y in range(GROUND + 4, top + 1):
        put(mt, 8, y, 8, L.CHAIN_BLOCK, {"axis": "y"})
    put(mt, 8, GROUND + 3, 8, "minecraft:cauldron")
    # ---- three bunks against the west wall, a barrel with a lantern between each pair
    for z, colour in ((3, "red"), (6, "gray"), (9, "red")):
        K.bed(mt, 3, GROUND, z, "west", colour)
    for z in (4, 7, 10):
        put(mt, 2, GROUND, z, "minecraft:barrel", {"facing": "up", "open": "false"})
        put(mt, 2, GROUND + 1, z, L.LANTERN, {"hanging": "false"})
    # ---- the banked forge in the north wall: cold blast furnaces under a hood, a flue of polished stone up to the roof
    for x in (6, 7, 8, 9):
        put(mt, x, GROUND, 2, "minecraft:blast_furnace", {"facing": "south", "lit": "false"})
        put(mt, x, GROUND + 1, 2, L.POLISHED_STAIRS, st("north", "top"))
    for x in (7, 8):
        for y in range(GROUND + 2, top + 1):
            put(mt, x, y, 2, L.POLISHED)
            put(mt, x, y, 3, L.POLISHED)
    for x in (5, 10):
        put(mt, x, GROUND, 2, L.POLISHED)
        put(mt, x, GROUND + 1, 2, L.POLISHED)
    K.sign(mt, 3, GROUND + 2, 2, "south", ["REST HERE.", "The foreman", "does not come", "this far."], block="minecraft:crimson_wall_sign")
    K.sign(mt, 13, GROUND + 2, 2, "south", ["Bank the fire.", "Wash the soot.", "Mind the debt."], block="minecraft:crimson_wall_sign")
    # ---- the workbenches along the east wall
    anvil(mt, 15, GROUND, 3, "south")
    put(mt, 15, GROUND, 5, "minecraft:grindstone", {"face": "floor", "facing": "west"})
    put(mt, 15, GROUND, 7, "minecraft:smithing_table")
    put(mt, 15, GROUND, 9, "minecraft:blast_furnace", {"facing": "west", "lit": "false"})
    # ---- the south wall: ale barrels, a water cauldron, a block of coal, the ledger of the house
    for x in range(3, 8):
        put(mt, x, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    put(mt, 8, GROUND, 15, "minecraft:water_cauldron", {"level": "3"})
    put(mt, 3, GROUND + 1, 15, "minecraft:coal_block")
    K.lectern(mt, 10, GROUND, 15, "north")
    # ---- lamps, banners
    for (x, z) in ((5, 5), (11, 5), (5, 11), (11, 11)):
        K.hang(mt, x, top + 1, z, 4, L.LANTERN)
    for (x, z) in ((3, 12), (3, 14), (14, 3), (14, 6)):
        if mt.get(x, GROUND, z) is None:
            D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.LANTERN)
    for z in (6, 11):
        K.banner(mt, 15, GROUND + 4, z, "west", "red")
    mt.marker(12, GROUND, 6, "patrol")
    mt.marker(5, GROUND, 12, "patrol")
    K.finish(mt, ID, minimum=6)
    return mt


# =====================================================================================================================
# 7. counting_house: the treasure room, a bank
# =====================================================================================================================
@room("treasure", "counting_house", faction="demon", strict_light=True, route_width="wide")
def counting_house(name):
    """Why: the treasure room of the theme, and a joke that is also a lesson: in the Hells the hoard is a bank. An 18 x 18 dead end (one doorway) in two halves: a counting hall in the south
    (a long counter of polished stone with lecterns, candles and coin stacks, shelves of ledgers on both side walls, a great balance of iron hung from the roof with a pan of gold in its low
    side) and, through an arch in a wall of iron bars, the vault in the north: three chests on plinths of gilded stone between candles, stacks of gold along the wall, two golden stands that
    watch it, and a chandelier. Guards stand in the vault (spawn markers, the heavy ones), clerks of the demon faction in the hall; nothing in it is a trap, it is a treasure room that has to be
    fought for and not solved."""
    mt = tpl("treasure", name, 2, 2)
    rng = mt.rng
    H = 11
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    mt.socket("s", 13)
    # ---- the floor: a checker of plates in the hall, a border of gilded stone round the vault's plinths
    for z in range(2, 16):
        for x in range(2, 16):
            if z >= 9 and (x + z) % 2 == 0 and rng.random() < 0.55:
                mt.set(x, FR, z, L.POLISHED)
            elif z <= 6 and (z in (2, 6) or x in (2, 15)) and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.CHISELED)
    # ---- the wall of bars with its arch (the gate is four wide, x = 10 .. 13), a polished lintel above the whole line
    for x in range(2, 16):
        if not 10 <= x <= 13:
            for y in range(GROUND, GROUND + 4):
                put(mt, x, y, 8, L.IRON_BARS)
        put(mt, x, GROUND + 4, 8, L.POLISHED)
    for x in (9, 14):
        for y in range(GROUND, GROUND + 4):
            put(mt, x, y, 8, L.POLISHED, over=True)
    # ---- the vault: three chests on plinths between candles, gold along the walls, two golden stands, a chandelier
    for x in (4, 8, 12):
        put(mt, x, GROUND, 3, L.CHISELED)
        mt.t.chest(x, GROUND + 1, 3, K.LOOT, "south")
        for dx in (-1, 1):
            candles(mt, x + dx, GROUND, 3, 3, "orange")
    for (x, z) in ((2, 2), (15, 2), (2, 3), (15, 3)):
        put(mt, x, GROUND, z, "minecraft:gold_block")
    for (x, z) in ((2, 2), (15, 2)):
        put(mt, x, GROUND + 1, z, "minecraft:raw_gold_block")
    for x in (6, 10):
        L.statue(mt, x, GROUND, 6, "south", head="minecraft:golden_helmet", chest="minecraft:golden_chestplate", legs="minecraft:golden_leggings",
                 feet="minecraft:golden_boots", weapon="minecraft:golden_sword")
    L.chandelier(mt, 8, top + 1, 5, drop=3, radius=1, lantern=L.SOUL_LANTERN)
    # ---- the counting hall: a counter of polished stone along the south part, lecterns and coin stacks on it, ledger shelves on the side walls
    for x in range(2, 9):
        put(mt, x, GROUND, 14, L.POLISHED)
    for x in (3, 5, 7):
        K.lectern(mt, x, GROUND + 1, 14, "north")
    candles(mt, 4, GROUND + 1, 14, 3, "red")
    candles(mt, 6, GROUND + 1, 14, 2, "black")
    put(mt, 8, GROUND + 1, 14, "minecraft:gold_block")
    put(mt, 2, GROUND + 1, 14, "minecraft:cauldron")
    for x in range(2, 9):
        put(mt, x, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    for z in range(9, 14):
        for y in range(GROUND, GROUND + 3):
            put(mt, 2, y, z, "minecraft:bookshelf")
    for z in range(9, 12):
        for y in range(GROUND, GROUND + 3):
            put(mt, 15, y, z, "minecraft:bookshelf")
    # ---- the balance: a beam of polished stone under the roof, a pan of bars on a chain at each end, the low one heavy with gold
    for x in range(5, 12):
        put(mt, x, top, 11, L.POLISHED)
    for (x, drop, gold) in ((5, 8, True), (11, 4, False)):
        for y in range(top - drop, top):
            put(mt, x, y, 11, L.CHAIN_BLOCK, {"axis": "y"})
        y_pan = top - drop - 1
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                put(mt, x + dx, y_pan, 11 + dz, L.IRON_BARS)
        if gold:
            put(mt, x, y_pan + 1, 11, "minecraft:gold_block")
    # ---- lamps and banners
    for (x, z) in ((6, 9), (13, 12)):
        K.hang(mt, x, top + 1, z, 5, L.LANTERN)
    for (x, z) in ((3, 10), (14, 15)):
        D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.LANTERN)
    K.banner(mt, 15, GROUND + 5, 11, "west", "red")
    K.banner(mt, 2, GROUND + 5, 5, "east", "black")
    K.auto_markers(mt, "demon", spawns=4, patrols=1, spacing=4, door_gap=5, edge=2)
    K.finish(mt, ID)
    return mt


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, doc=__doc__, theme=ID))

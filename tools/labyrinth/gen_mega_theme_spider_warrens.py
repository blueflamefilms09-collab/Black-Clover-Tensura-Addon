#!/usr/bin/env python3
"""
Why: the SPIDER_WARRENS theme (1.59.0, THEME-D, docs/labyrinth/mega_v2_thd.md): a spider queen's warren, black chitin and white silk. A themed sector must read as a NEW place, so besides the
palette swap of `data/nusmp/mega/themes/spider_warrens.json` (polished blackstone walls, cobbled deepslate floors, white calcite pillars, silk-white wool patches, warm lanterns) it owns its
signature rooms, which the assembler may draw only in sectors of this theme (`data/nusmp/mega/template_themes/spider_warrens.json`, prefix `nusmp:mega/<folder>/spider_warrens_`).

    python3 tools/labyrinth/gen_mega_theme_spider_warrens.py                 # build, check and write every room
    python3 tools/labyrinth/gen_mega_theme_spider_warrens.py web_great_hall  # only this one
    python3 tools/labyrinth/gen_mega_theme_spider_warrens.py --check         # build and check in memory, compare with the files on disk (CI style)

Rooms: web_great_hall (hall, silk bridges over the hall), cocoon_larder (hall), queens_lair (set piece), brood_hatch (trial), silk_nook (safe), egg_cache (treasure).
The rules of the silk in every one of them: a cobweb slows whoever stands in it and the lead's walking model counts it as free air, so the kit's `web_problems` check refuses any cobweb in
the feet cell or the head cell of a cell a player can walk to; the webs are scenery on the walls, in the corners, over the heads and between the pillars, never across a way. Egg sacs are
wool and slime (turtle eggs hatch and sniffer eggs hatch: they are not safe decor). Silk bridges are decks of white wool with wall-post rails, hung from the roof by chains.
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
from thd_kit import FR, GROUND, put, st, dist, candles  # noqa: E402

ID = "spider_warrens"
ROOMS = []   # (folder, name, builder, spec)
WOOL = "minecraft:white_wool"
WEB = "minecraft:cobweb"


def room(folder, name, **spec):
    """Registers a builder `fn(full_name) -> MT`; `spec` is what the checks expect of the room (see gen_mega_rooms2.check_room)."""
    def deco(fn):
        ROOMS.append((folder, ID + "_" + name, fn, spec))
        return fn
    return deco


def tpl(folder, name, mx, mz):
    return K.new(folder, name, mx, mz)


def egg_sac(mt, x, y, z, rng, size=2):
    """An egg sac standing on the cell below (x, y, z): a lump of white wool `size` across with a slime block bulging out of its top (the eggs under the silk). Wool and slime only:
    a turtle egg or a sniffer egg would hatch. Cells that are taken are left alone."""
    for dx in range(size):
        for dz in range(size):
            if rng.random() < 0.85 or (dx, dz) == (0, 0):
                put(mt, x + dx, y, z + dz, WOOL)
    put(mt, x, y + 1, z, "minecraft:slime_block")
    if size > 1 and rng.random() < 0.6 and mt.get(x + 1, y, z + 1) is not None:
        put(mt, x + 1, y + 1, z + 1, WOOL)


def hanging_cocoon(mt, x, y_ceiling, z, length, rng):
    """A bundle of silk hanging from the ceiling block at row `y_ceiling`: a column of white wool `length` long under it, the last cell a slime block (the thing inside it)."""
    for k in range(1, length + 1):
        put(mt, x, y_ceiling - k, z, WOOL)
    if rng.random() < 0.5:
        put(mt, x, y_ceiling - length - 1, z, "minecraft:slime_block")


def victim_cocoon(mt, x, z, facing, rng, kit="iron", y=GROUND):
    """A caught adventurer in a cocoon: a mass of white wool three blocks square and four high standing on the floor cell row `y`, with a hollow in its middle (two cells) in which an
    armour stand in a set of armour stands on the floor and a cobweb hangs in front of its face, and a window of two white stained-glass panes in the wool on the side `facing` so the
    victim can be seen. The hollow is shut by wool and glass on every side, so no web is ever on a cell a player can walk to; the stand's own cell is free and has wool under it, as the arch
    rules want. `facing` is the side the window looks out of (the stand looks that way)."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for dy in range(4):
                if (dx, dz) == (0, 0) and dy in (0, 1):
                    continue
                if dy == 3 and (abs(dx) + abs(dz)) == 2:
                    continue
                put(mt, x + dx, y + dy, z + dz, WOOL)
    fx, fz = L.STEP[facing]
    for dy in (0, 1):
        put(mt, x + fx, y + dy, z + fz, "minecraft:white_stained_glass_pane", {"north": "false", "south": "false", "east": "false", "west": "false", "waterlogged": "false"}, over=True)
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
    gear = {"iron": ("iron_helmet", "iron_chestplate", "iron_leggings", "iron_boots", "iron_sword"),
            "chain": ("chainmail_helmet", "chainmail_chestplate", "chainmail_leggings", "chainmail_boots", "stone_axe"),
            "leather": ("leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots", "bow")}[kit]
    mt.armor_stand(x, y, z, yaw=yaw, head="minecraft:" + gear[0], chest="minecraft:" + gear[1], legs="minecraft:" + gear[2], feet="minecraft:" + gear[3], mainhand="minecraft:" + gear[4])
    mt.set(x, y + 1, z, WEB)


def web_cells(mt, rng, cells, count, y_min, y_max, near_solid=True):
    """Cobwebs scattered over `cells` ((x, z) columns) between rows y_min and y_max, only in free cells that touch a block (a web hangs in a corner, on a wall or from a ceiling) when
    `near_solid`. Returns the number placed. The caller keeps `y_min` above head height wherever a player can walk; the kit's check proves it."""
    pool = sorted(cells)
    rng.shuffle(pool)
    n = 0
    for (x, z) in pool:
        if n >= count:
            break
        y = rng.randint(y_min, y_max)
        if not K.free(mt, x, y, z):
            continue
        if near_solid and not any(mt.get(x + dx, y + dy, z + dz) is not None for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
            continue
        mt.set(x, y, z, WEB)
        n += 1
    return n


def deck_cells(x0, x1, z0, z1):
    """The (x, z) cells of an inclusive rectangle, for the deck builders."""
    return {(x, z) for z in range(z0, z1 + 1) for x in range(x0, x1 + 1)}


def silk_decks(mt, cells, y, interior, open_edges=(), pillars=(), rng=None):
    """A deck of white wool at row `y` over `cells` (a set of (x, z)), with a few plates of black stone laid in the weave, and wall-post rails on every edge cell whose neighbour is not deck
    and not a wall of the hall (`interior` is the set of the hall's floor cells: a neighbour outside it is the wall, so no drop there) and not in `open_edges` (the cells where a flight or
    a bridge joins). The rail posts stand ON the deck's edge cells, so the walk is narrower than the deck by the rails. `pillars` are cells a pillar passes through (neither deck nor rail).
    Returns the list of rail cells."""
    rails = []
    for (x, z) in sorted(cells):
        if (x, z) in pillars:
            continue
        put(mt, x, y, z, WOOL if (x + z) % 5 else L.CRACKED_TILE, over=True)
    for (x, z) in sorted(cells):
        if (x, z) in pillars or (x, z) in open_edges:
            continue
        if any(((x + dx, z + dz) not in cells and (x + dx, z + dz) in interior and (x + dx, z + dz) not in open_edges) for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            put(mt, x, y + 1, z, L.POLISHED_WALL, {"up": "true"})
            rails.append((x, z))
    return rails


def silk_lines(mt, rails, y_deck, y_ceiling, every=5):
    """Chains from some of the rail posts up to the roof: the bridge hangs from the roof by silk. A chain starts on the top of its wall post and ends against the ceiling block, so it is
    held at both ends."""
    n = 0
    for (x, z) in sorted(rails):
        if (x + 2 * z) % every != 0:
            continue
        for y in range(y_deck + 2, y_ceiling):
            put(mt, x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
        n += 1
    return n


def pillar3(mt, cx, cz, y0, y1, bands=(), shaft=L.POLISHED, band=L.CHISELED):
    """A pillar three blocks square centred on (cx, cz) from row y0 to y1: a calcite shaft (the polished slot of this theme), bands of chiselled black stone at the listed rows, a base and a
    capital ring."""
    for y in range(y0, y1 + 1):
        blk = band if (y in bands or y in (y0, y1)) else shaft
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                put(mt, cx + dx, y, cz + dz, blk, over=True)


# =====================================================================================================================
# 1. web_great_hall: the hall of the silk bridges
# =====================================================================================================================
@room("hall", "web_great_hall", faction="insect", strict_light=True)
def web_great_hall(name):
    """Why: the brief's 'web-filled great hall' with 'silk bridges', as the big room of the theme: 23 across and 32 long under a roof 25 high. Six pillars of white calcite, each three blocks
    square and wrapped in bands of black stone, hold the roof in two rows; a lane nine wide runs between them from door to door and two cross lanes join the side doors. The second storey
    is the silk: a flight of nine steps up each side wall (from the cross lanes) to a nest platform in each north corner, a bridge five wide of white wool between the platforms over the
    north door, and from the middle of it a spine running south over the central lane to a round-cornered nest in the middle of the hall where the hall's hoard lies. The bridges have wall-post
    rails (the drop is nine blocks) and hang from the roof by chains of silk; webs fill the roof space over them, between the pillars and in every corner, always above head height wherever a
    player can walk (the kit proves it); egg sacs of wool and slime crowd the walls, bundles hang from the roof, four adventurers are cocooned against the walls with a window of glass
    to see them by. The insect faction (spiders and their kin) holds it; the bridges are where the party gets above them."""
    mt = tpl("hall", name, 3, 4)
    rng = mt.rng
    H = 25
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.14)
    top = FR + H
    DECK = FR + 9
    for (side, off) in (("s", 13), ("n", 13), ("w", 22), ("e", 22)):
        mt.socket(side, off)
    interior = deck_cells(2, 24, 2, 33)
    # ---- the floor: tile with patches of silk (the wool of the cracked slot) and white plates
    for z in range(2, 34):
        for x in range(2, 25):
            r = rng.random()
            if r < 0.10:
                mt.set(x, FR, z, L.CRACKED_TILE)
            elif r < 0.20 and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    # ---- the decks: two corner platforms, the north bridge, the spine and the nest in the middle; the flights up the side walls
    platforms = deck_cells(2, 8, 2, 11) | deck_cells(18, 24, 2, 11)
    bridge = deck_cells(9, 17, 3, 7)
    spine = deck_cells(11, 15, 8, 13)
    nest = deck_cells(9, 17, 14, 22) - {(9, 14), (17, 14), (9, 22), (17, 22)}
    flights = deck_cells(2, 4, 12, 20) | deck_cells(22, 24, 12, 20)
    pillar_cells = {(px + dx, pz + dz) for (px, pz) in ((7, 9), (19, 9), (7, 18), (19, 18), (7, 27), (19, 27)) for dx in (-1, 0, 1) for dz in (-1, 0, 1)}
    joins = deck_cells(2, 4, 11, 11) | deck_cells(22, 24, 11, 11) | deck_cells(8, 9, 3, 7) | deck_cells(17, 18, 3, 7) | deck_cells(12, 14, 7, 8) | deck_cells(12, 14, 13, 14)
    rails = silk_decks(mt, platforms | bridge | spine | nest, DECK, interior | flights, open_edges=joins, pillars=pillar_cells, rng=rng)
    for west in (True, False):
        gx = 2 if west else 22
        K.ramp(mt, gx, 20, "north", 9, FR + 1, width=3, stair=L.TILE_STAIRS, fill=L.BRICK, rail_left=not west, rail_right=west)
    for (px, pz) in ((7, 9), (19, 9), (7, 18), (19, 18), (7, 27), (19, 27)):
        pillar3(mt, px, pz, GROUND, top - 1, bands=(FR + 5, DECK, FR + 14, FR + 19))
    silk_lines(mt, rails, DECK, top + 1)
    for k, (x, z) in enumerate(sorted(rails)):
        if k % 7 == 0:
            put(mt, x, DECK + 2, z, L.LANTERN, {"hanging": "false"})
    # ---- the hoard in the nest: a chest between white candles, bones, a skull
    mt.t.chest(13, DECK + 1, 18, K.LOOT, "south")
    candles(mt, 12, DECK + 1, 18, 3, "white")
    candles(mt, 14, DECK + 1, 18, 3, "white")
    put(mt, 11, DECK + 1, 17, "minecraft:bone_block")
    put(mt, 15, DECK + 1, 19, "minecraft:skeleton_skull", {"rotation": "6"})
    for (x, z) in ((10, 20), (16, 20)):
        egg_sac(mt, x, DECK + 1, z, rng)
    egg_sac(mt, 3, DECK + 1, 3, rng)
    egg_sac(mt, 22, DECK + 1, 3, rng)
    # ---- egg sacs against the walls, bundles from the roof, cocooned adventurers
    for (x, z) in ((2, 3), (2, 6), (2, 9), (23, 3), (23, 6), (23, 9), (2, 28), (2, 31), (23, 28), (23, 31)):
        egg_sac(mt, x, GROUND, z, rng)
    for (x, z, f, kit) in ((4, 31, "east", "iron"), (22, 31, "west", "chain"), (22, 25, "west", "leather"), (4, 25, "east", "iron")):
        victim_cocoon(mt, x, z, f, rng, kit=kit)
    for _ in range(16):
        x, z = rng.randint(4, 22), rng.randint(4, 31)
        if (x, z) not in pillar_cells:
            hanging_cocoon(mt, x, top + 1, z, rng.randint(5, 10), rng)
    # ---- the webs: over the bridges, between the pillars, in every corner (all of them above head height wherever a player can walk)
    all_cells = {(x, z) for (x, z) in interior if (x, z) not in pillar_cells}
    web_cells(mt, rng, all_cells, 150, DECK + 4, top, near_solid=True)
    web_cells(mt, rng, {(x, z) for (x, z) in all_cells if any(abs(x - px) <= 2 and abs(z - pz) <= 2 for (px, pz) in ((7, 9), (19, 9), (7, 18), (19, 18), (7, 27), (19, 27)))}, 70, GROUND + 5, top - 2)
    for (x, z) in ((2, 2), (24, 2), (2, 33), (24, 33)):
        for y in range(top - 6, top + 1):
            if rng.random() < 0.8:
                put(mt, x, y, z, WEB)
    # ---- lamps: posts along the central lane, chandeliers over the south end, the pillars' wrapping
    for (x, z) in ((9, 13), (17, 13), (9, 24), (17, 24), (9, 31), (17, 31)):
        D.lamp_post(mt, x, GROUND, z, height=3, lantern=L.LANTERN)
    for z in (25, 31):
        L.chandelier(mt, 13, top + 1, z, drop=12, radius=2, lantern=L.LANTERN)
    K.auto_markers(mt, "insect", spawns=9, patrols=3, spacing=6, avoid=[])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 2. cocoon_larder: the pantry, low and dense
# =====================================================================================================================
def silk_niche(mt, side, lat, y, rng):
    """A shelf cut into the inner layer of the wall `side` at lateral position `lat`, two cells high from row y, holding a wrapped bundle (a wool block with a slime block over it) or a
    bone, and no web (a niche is a cell a player could step into). The outer wall layer stays, so it is a recess."""
    x, yy, z = mt._lateral(side, 0, lat, 1, y)
    if (x, yy, z) in mt.reserved or (x, yy + 1, z) in mt.reserved:
        return False
    c = mt.get(x, yy, z)
    if c is None or c.block not in (L.BRICK, L.CRACKED_BRICK):
        return False
    mt.remove(x, yy, z)
    mt.remove(x, yy + 1, z)
    kind = rng.random()
    if kind < 0.55:
        mt.set(x, yy, z, WOOL)
        if rng.random() < 0.5:
            mt.set(x, yy + 1, z, "minecraft:slime_block")
    elif kind < 0.8:
        mt.set(x, yy, z, "minecraft:bone_block", {"axis": rng.choice("xz")})
    else:
        mt.set(x, yy, z, "minecraft:skeleton_skull", {"rotation": str({"n": 0, "s": 8, "w": 4, "e": 12}[side])})
    return True


def wool_stack(mt, cx, cz, rng, y=GROUND, hollow=False):
    """A plain cocoon: a mass of white wool three blocks square and four high centred on (cx, cz), its top corners cut (it is a bundle, not a box), a slime block bulging out of the top
    centre. With `hollow` the two middle cells of the centre column are left empty (for a pack or a chest to be put in)."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for dy in range(4):
                if hollow and (dx, dz) == (0, 0) and dy in (0, 1):
                    continue
                if dy == 3 and abs(dx) + abs(dz) == 2:
                    continue
                put(mt, cx + dx, y + dy, cz + dz, WOOL)
    put(mt, cx, y + 4, cz, "minecraft:slime_block")


@room("hall", "cocoon_larder", faction="insect", strict_light=True)
def cocoon_larder(name):
    """Why: the second hall of the theme has to feel nothing like the first: where the great hall is tall and white and open to the roof, the cocoon larder is the pantry under it: 23 across
    and nine high, a cross of lanes five wide that joins the four doors, and in each of the four quarters a block of four cocoons, stacks of white wool three blocks square and four high
    with one-block aisles between them, six of the sixteen with a victim inside (an armour stand in its armour, seen through a window of glass in the silk), one with a gap and a chest in it
    (the pack of the one it held). Bundles hang from the roof over the edges of the lanes, a great one over the crossing, shelves cut into the walls hold wrapped parcels and bones, valances of
    web droop from the roof over the entrances to the quarters (above head height: the kit proves no web is on a walkable cell) and the roof over the quarters is one sheet of web. Insect
    faction; the lanes are where the party stands and the quarters are where it is surprised."""
    mt = tpl("hall", name, 3, 3)
    rng = mt.rng
    H = 10
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.14)
    top = FR + H
    for side in L.SIDES:
        mt.socket(side, 13)
    lanes = deck_cells(11, 15, 2, 24) | deck_cells(2, 24, 11, 15)
    for z in range(2, 25):
        for x in range(2, 25):
            r = rng.random()
            if (x, z) in lanes and r < 0.10:
                mt.set(x, FR, z, L.CRACKED_TILE)
            elif (x, z) not in lanes and r < 0.30:
                mt.set(x, FR, z, L.CRACKED_TILE)
            elif r < 0.18 and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    # ---- four quarters of cocoon stacks: 2 x 2 stacks of 3 x 3 wool (centres four apart), the victims face the lane or the aisle in front of them
    quarters = {"nw": (4, 4), "ne": (18, 4), "sw": (4, 18), "se": (18, 18)}
    victims = {("nw", 1, 1): "east", ("ne", 0, 1): "south", ("sw", 1, 0): "north", ("se", 0, 0): "west", ("nw", 0, 1): "south", ("se", 1, 1): "north"}
    kits = ("iron", "chain", "leather")
    pack = ("ne", 1, 1)
    for q, (qx, qz) in quarters.items():
        for i in range(2):
            for j in range(2):
                cx, cz = qx + 4 * i, qz + 4 * j
                f = victims.get((q, i, j))
                if (q, i, j) == pack:
                    wool_stack(mt, cx, cz, rng, hollow=True)
                    put(mt, cx, GROUND, cz + 1, "minecraft:air", over=True)          # the mouth of the pack, toward the aisle in front of it
                    put(mt, cx, GROUND + 1, cz + 1, "minecraft:air", over=True)
                    mt.t.chest(cx, GROUND, cz, K.LOOT, "south")
                elif f:
                    victim_cocoon(mt, cx, cz, f, rng, kit=kits[(i + 2 * j) % 3])
                else:
                    wool_stack(mt, cx, cz, rng)
    # ---- bundles hanging over the edges of the lanes, one great bundle over the crossing
    for x in (11, 15):
        for z in (4, 8, 18, 22):
            hanging_cocoon(mt, x, top + 1, z, rng.randint(6, 7), rng)
    for z in (11, 15):
        for x in (4, 8, 18, 22):
            hanging_cocoon(mt, x, top + 1, z, rng.randint(6, 7), rng)
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in range(FR + 5, top + 1):
                if y == FR + 5 and abs(dx) + abs(dz) == 2:
                    continue
                put(mt, 13 + dx, y, 13 + dz, WOOL)
    # ---- shelves in the walls: two tiers of wrapped parcels, bones and skulls between the doors
    for side in L.SIDES:
        for lat in (4, 6, 8, 18, 20, 22):
            for y in (GROUND, GROUND + 3):
                silk_niche(mt, side, lat, y, rng)
    # ---- web: a sheet under the roof over the quarters, valances over their entrances, corners (always over head height)
    quarter_cells = {(x, z) for (x, z) in deck_cells(2, 24, 2, 24) if (x, z) not in lanes}
    for (x, z) in sorted(quarter_cells):
        for y in (top - 1, top):
            if rng.random() < 0.55 and K.free(mt, x, y, z):
                mt.set(x, y, z, WEB)
    for (x, z) in sorted({(x, z) for (x, z) in quarter_cells if x in (10, 16) or z in (10, 16)}):
        for y in range(top - 4, top + 1):
            if rng.random() < 0.7 and K.free(mt, x, y, z):
                mt.set(x, y, z, WEB)
    for (x, z) in ((2, 2), (24, 2), (2, 24), (24, 24)):
        for y in range(top - 5, top + 1):
            put(mt, x, y, z, WEB)
    # ---- lamps: posts on the edges of the lanes
    for (x, z) in ((11, 6), (15, 6), (11, 20), (15, 20), (6, 11), (6, 15), (20, 11), (20, 15)):
        D.lamp_post(mt, x, GROUND, z, height=3, lantern=L.LANTERN)
    K.auto_markers(mt, "insect", spawns=8, patrols=3, spacing=5, avoid=[(13, 13, 3.0)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 3. queens_lair: the nest under the legs, the set piece
# =====================================================================================================================
def spider_leg(mt, cx, cz, ang, r0, y0, rise, block=L.BRICK, joint=L.POLISHED, last_deg=82):
    """One leg of the queen: an arc, a quarter of an ellipse, rising from the floor at radius r0 on the ray `ang` (degrees) to the apex over the middle of the room: r = r0 cos(t), y = y0 +
    rise sin(t) for t from 0 to `last_deg`, drawn as a rod three blocks square at its foot and two at its top, with a joint of white stone every ten degrees (a leg has joints, and the white shows them in the dark). Every step of
    the arc is at most one cell from the last in each direction, and the rods overlap, so the leg is one connected piece. Returns the number of cells written."""
    a = math.radians(ang)
    last = None
    n = 0
    for k in range(0, last_deg + 1):
        t = math.radians(k)
        r = r0 * math.cos(t)
        y = y0 + rise * math.sin(t)
        ix, iy, iz = int(round(cx + r * math.cos(a))), int(round(y)), int(round(cz + r * math.sin(a)))
        if (ix, iy, iz) == last:
            continue
        last = (ix, iy, iz)
        blk = joint if (k // 5) % 2 == 1 and k >= 5 else block
        offs = (-1, 0, 1) if k < 30 else (0, 1)
        for dx in offs:
            for dz in offs:
                if put(mt, ix + dx, iy, iz + dz, blk, over=True):
                    n += 1
    return n


def wool_sphere(mt, cx, cy, cz, r, rng, over=False):
    """A ball of white wool of radius r with eggs showing on its surface: slime blocks and, a few of them, verdant froglights (a green glow of light 15 that lights the dome and the legs:
    without it the whole upper room is black and the set piece cannot be seen). Cells already taken are left alone unless `over`."""
    n = 0
    for dy in range(-int(r) - 1, int(r) + 2):
        for dz in range(-int(r) - 1, int(r) + 2):
            for dx in range(-int(r) - 1, int(r) + 2):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if d <= r + 0.3:
                    roll = rng.random()
                    if d > r - 0.7 and roll < 0.16:
                        blk = "minecraft:verdant_froglight"
                    elif d > r - 0.7 and roll < 0.26:
                        blk = "minecraft:slime_block"
                    else:
                        blk = WOOL
                    if put(mt, cx + dx, cy + dy, cz + dz, blk, over=over):
                        n += 1
    return n


@room("set", "queens_lair", faction="insect", strict_light=True, route_width="wide")
def queens_lair(name):
    """Why: every sector has one room the party remembers, and for the Spider Warrens it is the one the whole warren is built round: the queen's lair, a round chamber forty-one across (radius
    twenty) under a low ribbed dome with a door at each point of the compass. Over the middle of it hangs the queen's brood: a ball of silk seven across with the eggs showing in it, from
    the roof on a stalk, and from its sides eight great legs of black stone with joints of white stone arch out and down to the floor at the rim, so the whole chamber stands inside the
    cage of a spider that is not there (the queen herself is the boss the Dungeon Master puts at the room's centre). Under the legs a mound of three terraces of silk and stone with the
    queen's throne of bone on its top and a chest either side of it; egg sacs crowd the floor between the doors (the lanes between the doors stay clear), bundles hang from the roof, webs fill
    the space between the legs (always over head height where anyone can walk), braziers burn on the mound and lamps stand round the floor. The insect faction holds it."""
    mt = tpl("set", name, 5, 5)
    rng = mt.rng
    cx = cz = 22
    R = 20
    drum = FR + 14
    base = drum + 1
    dome_h = 8
    room_fn = lambda x, z: D.in_disc(x, z, cx, cz, R)  # noqa: E731
    for side in L.SIDES:
        mt.socket(side, 22)
    apex = G2.dome_row(0, R + 0.5, base, dome_h)
    G2.solid_mass(mt, room_fn, FR - 1, apex + 1, rng=rng)
    room = {(x, z) for z in range(mt.D) for x in range(mt.W) if room_fn(x, z)}
    # ---- floor: tile, silk patches, plates of white stone; one row of rock under it
    for (x, z) in sorted(room):
        mt.set(x, FR - 1, z, L.ROCK)
        r = rng.random()
        mt.set(x, FR, z, L.CRACKED_TILE if r < 0.16 else (L.POLISHED if (r < 0.26 and (x + z) % 2 == 0) else L.TILE))
    # ---- the dome: a shell two thick
    for (x, z) in sorted(room):
        ys = G2.dome_row(dist(x, z, cx, cz), R + 0.5, base, dome_h)
        for y in (ys, ys + 1):
            mt.set(x, y, z, L.CRACKED_BRICK if rng.random() < 0.08 else L.BRICK)
    # ---- the mound: three terraces, the throne on top, the chests either side
    for (x, z) in sorted(room):
        r = dist(x, z, cx, cz)
        if r <= 7.5:
            top_row = FR + (1 if r > 4.5 else (2 if r > 2.5 else 3))
            for y in range(FR + 1, top_row + 1):
                mt.set(x, y, z, WOOL if (x + y + z) % 3 else L.COBBLED)
    TOP = FR + 4                                                    # the standing row on the top terrace
    for x in (21, 22, 23):
        for y in range(TOP, TOP + 3):
            put(mt, x, y, 20, "minecraft:bone_block", {"axis": "y"})
    for x in (21, 23):
        put(mt, x, TOP, 21, "minecraft:bone_block", {"axis": "x"})
    put(mt, 22, TOP, 21, L.POLISHED_STAIRS, st("north"))
    mt.t.chest(20, TOP, 22, K.LOOT, "east")
    mt.t.chest(24, TOP, 22, K.LOOT, "west")
    for k in range(4):
        x, z = D.polar(cx, cz, 4.0, 45.0 + 90.0 * k)
        D.brazier(mt, x, FR + 2, z, fire="campfire", height=2, cage=True)
    # ---- the brood over the middle on a stalk from the roof, and the eight legs
    ball_y = FR + 18
    wool_sphere(mt, cx, ball_y, cz, 3.4, rng, over=True)
    for y in range(ball_y + 3, apex):
        put(mt, cx, y, cz, WOOL)
    for k in range(8):
        spider_leg(mt, cx, cz, 22.5 + 45.0 * k, 18.5, FR + 1, 19.0)
    # ---- egg sacs between the doors (never on the lanes between them), bundles from the roof, bones
    sacs = [(x, z) for (x, z) in sorted(room) if 9.0 <= dist(x, z, cx, cz) <= 17.5 and abs(x - cx) > 3 and abs(z - cz) > 3 and K.free(mt, x, GROUND, z)]
    rng.shuffle(sacs)
    placed = []
    for (x, z) in sacs:
        if len(placed) >= 16:
            break
        if all(dist(x, z, px, pz) >= 4.5 for (px, pz) in placed) and all(K.free(mt, x + dx, GROUND, z + dz) for dx in (0, 1) for dz in (0, 1)):
            egg_sac(mt, x, GROUND, z, rng)
            placed.append((x, z))
    for _ in range(14):
        a = rng.random() * 360.0
        r = 8.0 + rng.random() * 9.0
        x, z = D.polar(cx, cz, r, a)
        ys = G2.dome_row(dist(x, z, cx, cz), R + 0.5, base, dome_h)
        hanging_cocoon(mt, x, ys, z, rng.randint(3, 6), rng)
    for _ in range(26):
        x, z = D.polar(cx, cz, 9.0 + rng.random() * 9.0, rng.random() * 360.0)
        if K.free(mt, x, GROUND, z) and abs(x - cx) > 3 and abs(z - cz) > 3:
            if rng.random() < 0.5:
                put(mt, x, GROUND, z, "minecraft:bone_block", {"axis": rng.choice("xyz")})
            else:
                put(mt, x, GROUND, z, "minecraft:skeleton_skull", {"rotation": str(rng.randint(0, 15))})
    # ---- webs between the legs and along the wall, always over head height
    web_cells(mt, rng, room, 420, FR + 7, FR + 20, near_solid=True)
    for k in range(40):
        a = rng.random() * 360.0
        x, z, face = G2.wall_cell(cx, cz, R, a, room_fn)
        y = rng.randint(GROUND + 3, FR + 12)
        if K.free(mt, x, y, z):
            mt.set(x, y, z, WEB)
    # ---- lamps round the floor, banners between the doors
    for (x, z) in D.around(cx, cz, 12.5, 8, 22.5):
        if K.free(mt, x, GROUND, z) and mt.get(x, GROUND, z) is None:
            D.lamp_post(mt, x, GROUND, z, height=3, lantern=L.LANTERN)
    for k in range(8):
        x, z, face = G2.wall_cell(cx, cz, R, 22.5 + 45.0 * k, room_fn)
        K.banner(mt, x, FR + 6, z, face, "black")
    K.auto_markers(mt, "insect", spawns=10, patrols=3, spacing=6, door_gap=8, avoid=[(cx, cz, 4.0)])
    mt.marker(cx, TOP, cz + 2, "room_center")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 4. brood_hatch: the trial of the hatchery
# =====================================================================================================================
def pod(mt, cx, cz, open_side):
    """A brood pod: a recess three blocks square and three high built of white wool on the floor, open on `open_side` (the side toward the middle of the arena) with a cap of wool over it
    and a slime block on the cap (the eggs), its floor cell the spawn glyph of the wave. A pod must have an open side: a mob spawned in a closed one would never come out."""
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            x, z = cx + dx, cz + dz
            on_open = {"north": dz == -1, "south": dz == 1, "west": dx == -1, "east": dx == 1}[open_side]
            for y in range(GROUND, GROUND + 3):
                if (dx or dz) and not on_open:
                    put(mt, x, y, z, WOOL)
            put(mt, x, GROUND + 3, z, WOOL)
    put(mt, cx, GROUND + 4, cz, "minecraft:slime_block")


@room("trial", "brood_hatch", faction="insect", no_spawn=True, kind="trial")
def brood_hatch(name):
    """Why: the theme's fair fight (docs/labyrinth/e1_trial.md): walk in and the Sealed Archway shuts both doors, the glyphs spawn the wave, the barriers fall when the last one dies. The stage
    is the hatchery of the warren: a round floor with a mat of white silk in the middle (the party's anchor) in rings of polished stone, eight brood pods of wool round it at radius nine, each
    open toward the middle with a slime-block egg on its cap and a spawn glyph in its floor (so the brood comes out of the walls: spiders and cave spiders, scaled by the party), four pillars of
    calcite with lamps for cover, bundles hanging from the roof, webs on the walls and in the corners, and a vault alcove with the party's chest in the west wall between braziers, the
    alcove of a fallen hero across from it. No spawn marker: the archway's roster is the wave."""
    mt = tpl("trial", name, 3, 3)
    rng = mt.rng
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.12)
    mt.socket("n", 13)
    mt.socket("s", 13)
    c = 13
    top = FR + 14
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, c, c)
            if d <= 3.5:
                mt.set(x, FR, z, WOOL)
            elif d <= 4.5:
                mt.set(x, FR, z, L.CHISELED if int(D.angle_of(x, z, c, c) / 22.5) % 2 == 0 else L.POLISHED)
            elif 5.5 < d <= 6.5:
                mt.set(x, FR, z, L.POLISHED)
            elif abs(x - c) <= 1:
                mt.set(x, FR, z, L.POLISHED)
            elif rng.random() < 0.16:
                mt.set(x, FR, z, L.CRACKED_TILE)
    glyphs = []
    for (x, z) in D.around(c, c, 9.0, 8, 22.5):
        pod(mt, x, z, D.stair_facing(c - x, c - z))
        glyphs.append((x, z))
    G2.trial_core(mt, [("minecraft:spider", 3), ("minecraft:cave_spider", 4)], rng, [("n", 13), ("s", 13)], FR + 12, glyphs)
    for (px, pz) in ((5, 5), (21, 5), (5, 21), (21, 21)):
        D.column(mt, px, pz, FR + 1, FR + 13)
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3)
    f = D.inner_frame(mt, "w", 13)
    D.arched_alcove(mt, f, 5, 5)
    mt.t.chest(*f.at(0, 0, -1), K.LOOT, "east")
    for r in (-2, 2):
        D.candle_cluster(mt, *f.at(r, 0, -1), rng, colors=("white", "light_gray"), count=4)
    D.brazier(mt, *f.at(-4, 0, 0), cage=True)
    D.brazier(mt, *f.at(4, 0, 0), cage=True)
    f = D.inner_frame(mt, "e", 13)
    D.arched_alcove(mt, f, 5, 5)
    D.alcove_statue(mt, f, ("iron", "iron_sword"))
    for side in ("n", "s"):
        for lat in (6, 20):
            D.sconce(mt, D.inner_frame(mt, side, lat), 0, 5, skull="skeleton")
    for _ in range(10):
        x, z = rng.randint(4, 22), rng.randint(4, 22)
        if dist(x, z, c, c) > 10.5:
            hanging_cocoon(mt, x, top + 1, z, rng.randint(3, 6), rng)
    ring = {(x, z) for z in range(2, 25) for x in range(2, 25)}
    web_cells(mt, rng, ring, 120, FR + 6, top, near_solid=True)
    for (x, z) in ((2, 2), (24, 2), (2, 24), (24, 24)):
        for y in range(top - 5, top + 1):
            put(mt, x, y, z, WEB)
    for (x, z) in ((13, 6), (13, 20), (6, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(4, FR + 1, 4, "faction", faction="insect")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 5. silk_nook: the calm room, a spinner's burrow
# =====================================================================================================================
def glow_berries(mt, x, top_row, z, length, rng):
    """A glow-berry vine hanging from the ceiling block at row `top_row` + 1: `length` cells, berries on every cell (a warm light of 14), the head last. Vanilla vines, held by the roof."""
    for k in range(length - 1):
        put(mt, x, top_row - k, z, "minecraft:cave_vines_plant", {"berries": "true"})
    put(mt, x, top_row - length + 1, z, "minecraft:cave_vines", {"age": "20", "berries": "true"})


@room("safe", "silk_nook", safe=True, seats=4, hearths=1, beds=3, min_light=6, route_width="wide")
def silk_nook(name):
    """Why: a sector needs places to rest (the rest system reads seats, hearths and beds in a room of the `safe` folder), and the warren's is a burrow where a spinner lies up: 18 x 18, a
    campfire in a ring of bone with four benches round it, three white hammocks (beds) in a row under the north wall, a loom and a lectern with the spinner's ledger by the hammocks, silk
    hangings of white and black wool on the walls, glow-berry vines drooping from the roof (the one warm light the warren has), webs only in the corners and over the heads where nobody walks,
    stores of bones and bundles in the south corners. It is soft and warm and the only room of the theme where the silk is a blanket and not a trap. No spawn, no faction, no chest; the rest
    anchors (four seats, one hearth, three beds) are what the rest system reads."""
    mt = tpl("safe", name, 2, 2)
    rng = mt.rng
    H = 10
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.06)
    top = FR + H
    for (side, off) in (("s", 13), ("e", 13), ("w", 13)):
        mt.socket(side, off)
    # ---- the floor: a rug of silk round the hearth, plates of white stone beyond it
    for z in range(2, 16):
        for x in range(2, 16):
            d = dist(x, z, 8.5, 8.5)
            if d <= 4.6:
                mt.set(x, FR, z, WOOL if (x + z) % 3 else L.CRACKED_TILE)
            elif d <= 5.6:
                mt.set(x, FR, z, L.POLISHED)
    # ---- the hearth: a campfire in a ring of bone blocks, four benches
    for (x, z) in ((7, 8), (8, 8), (9, 8), (7, 9), (9, 9), (7, 10), (8, 10), (9, 10)):
        mt.set(x, GROUND, z, "minecraft:bone_block", {"axis": "y"})
    K.hearth(mt, 8, GROUND, 9, block="minecraft:campfire")
    K.seat(mt, 8, GROUND, 6, "south")
    K.seat(mt, 8, GROUND, 12, "north")
    K.seat(mt, 5, GROUND, 9, "east")
    K.seat(mt, 11, GROUND, 9, "west")
    # ---- three hammocks under the north wall, the loom and the ledger beside them
    for x in (3, 6, 9):
        K.bed(mt, x, GROUND, 3, "north", "white")
    put(mt, 12, GROUND, 3, "minecraft:loom", {"facing": "south"})
    K.lectern(mt, 14, GROUND, 3, "south")
    put(mt, 13, GROUND, 3, "minecraft:white_carpet")
    # ---- silk hangings on the west and east walls (tapestries of white and black wool in the inner wall layer)
    for z0 in (4, 10):
        for z in range(z0, z0 + 3):
            for y in range(GROUND + 1, GROUND + 5):
                for x in (1, 16):
                    c = mt.get(x, y, z)
                    if c is not None and c.block in (L.BRICK, L.CRACKED_BRICK) and (x, y, z) not in mt.reserved:
                        mt.set(x, y, z, WOOL if (y + z) % 2 == 0 else "minecraft:black_wool")
    # ---- stores: bones and bundles in the south corners, a water cauldron
    egg_sac(mt, 2, GROUND, 14, rng)
    put(mt, 3, GROUND, 11, "minecraft:bone_block", {"axis": "x"})
    put(mt, 14, GROUND, 11, "minecraft:water_cauldron", {"level": "3"})
    # ---- glow berries from the roof (the warm light), lanterns, webs in the corners above head height
    for (x, z) in ((5, 5), (11, 5), (4, 12), (12, 11), (8, 14), (8, 3)):
        glow_berries(mt, x, top, z, rng.randint(3, 5), rng)
    for (x, z) in ((6, 12), (10, 6)):
        K.hang(mt, x, top + 1, z, 4, L.LANTERN)
    for (x, z) in ((2, 2), (15, 2), (2, 15), (15, 15)):
        for y in range(top - 4, top + 1):
            put(mt, x, y, z, WEB)
    mt.marker(12, GROUND, 7, "patrol")
    mt.marker(4, GROUND, 13, "patrol")
    K.finish(mt, ID, minimum=6)
    return mt


# =====================================================================================================================
# 6. egg_cache: the treasure room, a clutch the queen laid round a hoard
# =====================================================================================================================
@room("treasure", "egg_cache", faction="insect", strict_light=True, route_width="wide", few_spawns=True)
def egg_cache(name):
    """Why: the treasure room of the theme: what a spider keeps is eggs, and what it keeps them round is whatever the last party carried. An 18 x 18 dead end (one doorway in the south): a hill
    of wool two to four blocks high fills the south-west of the room, its crest crowded with slime-block eggs and a few verdant froglights (the eggs that glow, the room's light), a lane five
    wide runs along it from the door and turns west under the north wall to the hoard: two chests between white candles and bones in the corner, egg sacs against the walls, bundles hanging
    from the roof and webs in the corners and over the lane (over head height: the kit proves it). The insect faction guards it; the heavy spawn stands by the chests."""
    mt = tpl("treasure", name, 2, 2)
    rng = mt.rng
    H = 10
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    mt.socket("s", 13)
    for z in range(2, 16):
        for x in range(2, 16):
            r = rng.random()
            if r < 0.12:
                mt.set(x, FR, z, L.CRACKED_TILE)
            elif r < 0.22 and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    # ---- the hill of eggs: columns of wool, a crest of slime and froglight
    hill = deck_cells(2, 10, 8, 15)
    for (x, z) in sorted(hill):
        h = 2 + int(3.0 * D.noise3(x / 3.0, 0.7, z / 3.0, 31))
        for y in range(GROUND, GROUND + h):
            put(mt, x, y, z, WOOL)
        roll = rng.random()
        put(mt, x, GROUND + h, z, "minecraft:verdant_froglight" if roll < 0.10 else ("minecraft:slime_block" if roll < 0.35 else WOOL))
    # ---- the hoard: two chests between candles and bones in the north-west corner, a skull on the floor
    for x in (3, 5):
        mt.t.chest(x, GROUND, 3, K.LOOT, "south")
    candles(mt, 4, GROUND, 3, 4, "white")
    candles(mt, 2, GROUND, 3, 2, "white")
    put(mt, 6, GROUND, 3, "minecraft:bone_block", {"axis": "x"})
    put(mt, 3, GROUND, 5, "minecraft:skeleton_skull", {"rotation": "9"})
    for (x, z) in ((2, 5), (7, 2), (10, 3), (14, 2)):
        egg_sac(mt, x, GROUND, z, rng)
    # ---- roof: bundles over the lane and the hoard, webs in the corners
    for (x, z) in ((12, 5), (14, 5), (12, 11), (14, 11), (5, 5), (8, 4)):
        hanging_cocoon(mt, x, top + 1, z, rng.randint(4, 6), rng)
    web_cells(mt, rng, deck_cells(2, 15, 2, 15), 60, FR + 6, top, near_solid=True)
    for (x, z) in ((2, 2), (15, 2), (15, 15), (2, 15)):
        for y in range(top - 4, top + 1):
            put(mt, x, y, z, WEB)
    for (x, z) in ((11, 14), (15, 14), (11, 8), (15, 8)):
        D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.LANTERN)
    K.auto_markers(mt, "insect", spawns=3, patrols=1, spacing=3, door_gap=4, edge=2)
    K.finish(mt, ID)
    return mt


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, doc=__doc__, theme=ID))

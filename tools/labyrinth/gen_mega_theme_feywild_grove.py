#!/usr/bin/env python3
"""
Why: THEME-B (1.59.0, docs/labyrinth/mega_v2_thb.md section 1) gives the Holy road the Feywild: a grove of giant glowing flowers and toadstools, cherry and azalea, will-o'-wisps,
a hollow ancient tree, a ring of standing stones, a feast the fey never stop eating, a court of the four seasons and a pond that keeps the moon. This script writes its rooms
(`structure/mega/<folder>/feywild_grove_*.nbt`, the prefix `data/nusmp/mega/template_themes/feywild_grove.json` claims for the theme), so only a sector of the Feywild Grove
theme ever draws them.

    python3 tools/labyrinth/gen_mega_theme_feywild_grove.py                  # builds, checks and writes every room (a room with a defect is not written)
    python3 tools/labyrinth/gen_mega_theme_feywild_grove.py pixie_nook --preview     # one room, then render it in its own theme into $THB_PREVIEWS
    python3 tools/labyrinth/gen_mega_theme_feywild_grove.py --check          # CI style: builds in memory, checks, compares with the files on disk

Architecture uses the palette constants of mega_lib (the Region Theme turns them into mud bricks, mossy stone, polished diorite, amethyst, rooted dirt and cherry wood);
the character of the grove is the free vanilla blocks on top: leaves, petals, glass blossoms lit by froglights, mushroom blocks, moss, azalea, glow berries, chains with
lanterns (the will-o'-wisps).
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

T = K.Theme("feywild_grove")
FR = L.FLOOR_ROW

PINK = "minecraft:pink_stained_glass"
MAGENTA = "minecraft:magenta_stained_glass"
LIME_PANE = "minecraft:lime_stained_glass_pane"
PEARL = "minecraft:pearlescent_froglight"
VERDANT = "minecraft:verdant_froglight"


# ===================================================================================================================
# the props of the grove
# ===================================================================================================================
def giant_flower(h, x, z, stem, petal=PINK, r=2, core=PEARL, y0=GROUND):
    """A giant glowing flower: a stem of lime glass panes `stem` tall (panes need no support and read as a translucent stalk), a bloom of coloured glass round a froglight
    core (a cup: a brim of radius r under the core, a ring round it, a bud over it). Returns False (and places nothing) when any cell of it is taken. Why: the brief's
    'bioluminescent giant flowers' without a custom block; glass does not stop light, so the froglight in the heart lights the room through the petals."""
    top = y0 + stem
    must = [(x, y, z, LIME_PANE) for y in range(y0, top)] + [(x, top, z, core)]
    petals = [(cx, top - 1, cz, petal) for (cx, cz) in K.disc(x, z, r + 0.2) if (cx, cz) != (x, z)]
    petals += [(cx, top, cz, petal) for (cx, cz) in K.disc(x, z, max(1, r - 1)) if (cx, cz) != (x, z)] + [(x, top + 1, z, petal)]
    if not all(h.can_set(cx, cy, cz, b) for (cx, cy, cz, b) in must):
        return False
    for (cx, cy, cz, b) in must:
        h.set(cx, cy, cz, b)
    for (cx, cy, cz, b) in petals:          # a bloom against a wall or a neighbour is clipped, not refused
        h.try_set(cx, cy, cz, b)
    return True


def toadstool(h, x, z, stem, r=2, cap="minecraft:red_mushroom_block", spots=True, y0=GROUND):
    """A giant toadstool: a stem of mushroom_stem `stem` tall, a cap of `cap` three layers thick (a brim of radius r, then r-1, then a crown), shroomlight spots under the
    brim. Returns False when any cell is taken."""
    top = y0 + stem
    cells = [(x, y, z, "minecraft:mushroom_stem") for y in range(y0, top)]
    for layer, rr in enumerate((r, max(1, r - 1), 1)):
        for (cx, cz) in K.disc(x, z, rr - 0.3):
            cells.append((cx, top + layer, cz, cap))
    stem_cells = [c for c in cells if c[3] == "minecraft:mushroom_stem"]
    if not all(h.can_set(cx, cy, cz, b) for (cx, cy, cz, b) in stem_cells):
        return False
    for (cx, cy, cz, b) in cells:                      # the cap is clipped against walls and neighbours, the stem is not
        if b == "minecraft:mushroom_stem":
            h.set(cx, cy, cz, b)
        else:
            h.try_set(cx, cy, cz, b)
    if spots:
        for dx, dz in ((r - 1, 0), (-(r - 1), 0), (0, r - 1), (0, -(r - 1))):
            h.try_set(x + dx, top - 1, z + dz, "minecraft:shroomlight")
    return True


def wisp(h, x, z, y_top, drop, kind="lantern"):
    """A will-o'-wisp: a small light that hangs in the air on a chain from the solid block over (x, y_top + 1, z): a lantern (warm) or a soul lantern (pale). Returns
    False when the column is taken or nothing solid is over it."""
    above = h.get(x, y_top + 1, z)
    if above is None or not D.center_support(above.block, above.props, "down"):
        return False
    return K.hang_chain_light(h, x, z, y_top, drop, soul=(kind == "soul"))


def standing_stone(h, x, z, height, y0=GROUND, lamp=False):
    """A standing stone: a column of cobbled blocks `height` tall, capped by a block (a slab, or with `lamp` a lantern standing on it: a will-o'-wisp that settled), with
    a little moss at its foot. A menhir of the Feywild ring."""
    if not all(h.can_set(x, y0 + k, z, L.COBBLED) for k in range(height + 2)):
        return False
    for k in range(height):
        h.set(x, y0 + k, z, L.COBBLED if k % 3 else L.CRACKED_TILE)
    if lamp:
        h.set(x, y0 + height, z, L.COBBLED)
        h.set(x, y0 + height + 1, z, L.LANTERN, {"hanging": "false"})
    else:
        h.set(x, y0 + height, z, L.COBBLED_SLAB, {"type": "bottom"})
    for _n, dx, dz in K.DIRS4:
        h.try_set(x + dx, y0, z + dz, L.COBBLED_STAIRS, {"facing": K.toward(-dx, -dz), "half": "bottom"})
    return True


def moss_floor(h, x1, z1, x2, z2, p_carpet=0.35, p_petal=0.12, p_azalea=0.04):
    """Moss, moss carpet, pink petals and a few azalea bushes over the free floor of the rectangle: the grove's ground. Every cell is a decoration on top of the room's
    own floor, wherever the cell above it is free (never on a declared route: `try_set` refuses)."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            if not h.free(x, GROUND, z) or h.get(x, FLOOR, z) is None:
                continue
            r = h.rng.random()
            if r < p_azalea:
                h.floor_set(x, z, "minecraft:moss_block")
                h.try_set(x, GROUND, z, "minecraft:flowering_azalea" if h.rng.random() < 0.5 else "minecraft:azalea")
            elif r < p_azalea + p_petal:
                h.floor_set(x, z, "minecraft:moss_block")
                h.try_set(x, GROUND, z, "minecraft:pink_petals", {"flower_amount": str(h.rng.randint(1, 4)), "facing": h.rng.choice(("north", "east", "south", "west"))})
            elif r < p_azalea + p_petal + p_carpet:
                h.floor_set(x, z, "minecraft:moss_block")
                h.try_set(x, GROUND, z, "minecraft:moss_carpet")


def glow_berries(h, x, y_top, z, length):
    """Cave vines with glow berries hanging from the solid block over (x, y_top + 1, z). The block must be sturdy underneath (leaves are not)."""
    above = h.get(x, y_top + 1, z)
    if above is None or not L.is_full(above.block):
        return False
    if not all(h.free(x, y_top - k, z) for k in range(length)):
        return False
    for k in range(length - 1):
        h.set(x, y_top - k, z, "minecraft:cave_vines_plant", {"berries": "true"})
    # Age 25 is the last stage: a head below it keeps growing downwards on random ticks until the vine reaches the floor. The draw is kept (and ignored) so that the
    # random sequence of the rooms already built does not shift.
    h.rng.randint(8, 24)
    h.set(x, y_top - length + 1, z, "minecraft:cave_vines", {"age": "25", "berries": "true"})
    return True


# ===================================================================================================================
# TREASURE: the pixie nook (1 x 2 modules, one door)
# ===================================================================================================================
@T.room("treasure", "pixie_nook", faction="beast", few_spawns=True)
def pixie_nook(path):
    """Why: the grove's calm room and its reward: a hollow the pixies keep their trinkets in. A dead end behind one door, with a pond the width of the room, a chest on a
    bed of petals under a giant flower, wisps over the pond and glow berries over the chest; the pixies themselves are not here (a nook is quiet), but the beasts of the
    grove den in it, so the faction marker is beast. It is the 'treasure closet' of the theme and chains with nothing else."""
    h = Hall(path, 1, 2, 9, wear=0.10)
    h.door("n", 4)
    h.route(3, 2, 5, 8, label="entrance run")
    # the ground: moss, carpet, petals; a small pond across the middle with a stepping-stone path down the west side
    K.sealed_pool(h, [(x, z) for z in (9, 10) for x in (4, 5, 6)], depth=1)
    for z in (9, 10):
        for x in (4, 5, 6):
            h.set(x, GROUND, z, "minecraft:lily_pad") if (x + z) % 2 == 0 else None
    giant_flower(h, 4, 14, 6, r=2)
    giant_flower(h, 2, 5, 5, r=1, petal=MAGENTA)
    giant_flower(h, 6, 7, 4, r=1)
    h.loot_chest(4, GROUND, 13, "north")
    for x in (3, 5):
        D.candles_at(h, x, GROUND, 13, 3, "pink")
    for (x, z) in ((3, 8), (5, 12), (3, 11)):
        wisp(h, x, z, h.top, 4)
    glow_berries(h, 4, h.top, 12, 3)
    h.marker(4, GROUND, 6, "spawn", weight=2)
    h.marker(4, GROUND, 8, "room_center")
    h.faction("beast", 5, 14)
    ww, we = h.wall("w"), h.wall("e")
    for (lat, y) in ((9, GROUND + 1), (14, GROUND + 3)):
        K.wall_glow(ww, lat, y, VERDANT)
    for (lat, y) in ((11, GROUND + 2), (5, GROUND + 3)):
        K.wall_glow(we, lat, y, PEARL)
    moss_floor(h, 2, 3, 6, 15)
    D.auto_light(h)
    return h


# ===================================================================================================================
# HALL: the feast of the fey (2 x 4 modules)
# ===================================================================================================================
def feast_dishes(h, x0, x1, z0, z1):
    """What is on the long table: cakes, melons, pumpkins, potted blossoms, candle clusters and a few standing lanterns, spread along the cells of the top row of the
    table (z0..z1, x0..x1 at row GROUND + 1). Every item sits on a top slab (a sturdy top face), never on the stair end boards."""
    items = ["minecraft:cake", "minecraft:melon", "minecraft:potted_cherry_sapling", "minecraft:pumpkin", "minecraft:potted_azalea_bush", "minecraft:cake",
             "minecraft:potted_flowering_azalea_bush", "minecraft:melon", "minecraft:potted_pink_tulip", "minecraft:pumpkin"]
    k = 0
    for z in range(z0 + 1, z1, 2):
        for x in range(x0, x1 + 1):
            if (x + z) % 3 == 0:
                continue
            if h.rng.random() < 0.55:
                h.try_set(x, GROUND + 1, z, items[k % len(items)])
                k += 1
        for x in (x0, x1):
            if h.rng.random() < 0.45:
                D.candles_at(h, x, GROUND + 1, z + 1, h.rng.randint(2, 4), h.rng.choice(("pink", "white", "magenta", "light_blue")))
    for z in range(z0 + 3, z1 - 1, 6):
        h.try_set((x0 + x1) // 2, GROUND + 1, z, L.LANTERN, {"hanging": "false"})


@T.room("hall", "feast_hall", faction="cultist")
def feast_hall(path):
    """Why: the feast of the fey is the Feywild's best-known place: a table that is never cleared, a company that never leaves, an invitation nobody refuses. The hall is
    18 x 36 with a long table down the middle (cakes, melons, pumpkins, potted blossoms, candles), a bench each side, wisps hung in a row over it, a dais at the head
    with a throne of cherry wood grown into a giant flower, garlands of azalea leaves under the ceiling beams, windows of coloured glass lit from behind and pilasters of
    log with a capital of leaves. The aisles either side are four and five wide, so the fight (the guests turn on whoever sits down) has room; the dais is the high
    ground and the throne the one place a party should not sit."""
    h = Hall(path, 2, 4, 11, wear=0.10)
    h.door("n", 13)
    h.door("s", 4)
    h.door("e", 4)
    h.door("w", 31)
    h.route(3, 3, 14, 7, label="north cross aisle")
    h.route(3, 26, 14, 28, label="south cross aisle")
    h.route(3, 3, 6, 28, label="west aisle")
    h.route(12, 3, 14, 28, label="east aisle")
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    top = h.top
    # the floor: a polished runner under the feast, moss where the room is wild
    for z in range(8, 28):
        for x in range(6, 13):
            h.floor_set(x, z, L.POLISHED if (x + z) % 5 else L.CHISELED)
    # the table with a bench each side, the dishes, the throne's dais at the head
    D.table(h.frame(10, GROUND, 9, "south"), 17, 3)
    for z in range(9, 26):
        h.set(7, GROUND, z, L.PLANKS_STAIRS, {"facing": "west", "half": "bottom"})
        h.set(11, GROUND, z, L.PLANKS_STAIRS, {"facing": "east", "half": "bottom"})
    feast_dishes(h, 8, 10, 9, 25)
    h.marker(9, GROUND + 1, 17, "furniture", **{"as": "table"})
    h.marker(7, GROUND + 1, 14, "seat", facing="east", **{"as": "chair"})
    D.dais(h, 7, 29, 11, 33, rows=2, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    # the throne: a seat, log arms, a log back and a giant flower grown behind it
    h.set(9, GROUND + 2, 31, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
    h.set(8, GROUND + 2, 31, L.PLANKS_SLAB, {"type": "bottom"})
    h.set(10, GROUND + 2, 31, L.PLANKS_SLAB, {"type": "bottom"})
    for k in range(5):
        h.set(9, GROUND + 2 + k, 32, L.STRIPPED_LOG, {"axis": "y"})
    for dx in (-1, 1):
        for k in range(1, 4):
            h.set(9 + dx, GROUND + 2 + k, 32, L.PLANKS_STAIRS if k == 3 else L.PLANKS_SLAB, {"facing": "south", "half": "bottom"} if k == 3 else {"type": "bottom"})
    h.marker(9, GROUND + 3, 31, "seat", facing="north", **{"as": "chair"})
    giant_flower(h, 9, 30, 8, r=2, petal=MAGENTA)
    giant_flower(h, 13, 32, 6, r=1)
    giant_flower(h, 3, 24, 5, r=1, petal=MAGENTA)
    # the wisps over the table, the garlands under the beams, the windows and the pilasters
    for z in (11, 15, 19, 23):
        for x in (8, 10):
            wisp(h, x, z, top, 4 + (z // 4) % 2)
        wisp(h, 9, z + 2 if z < 23 else z + 1, top, 6, kind="soul")
    D.beams(h, "x", [13, 21, 27], y=top)
    for zb in (13, 21, 27):
        for x in range(3, 15):
            if h.rng.random() < 0.7:
                K.leaf(h, x, top - 1, zb, "flowering_azalea")
        for x in range(3, 15, 3):
            glow_berries(h, x, top - 2, zb, 2)
    for lat in (9, 15, 21, 27):
        for wl in (ww, we):
            K.flush_pilaster(wl, lat, GROUND, top, shaft=L.STRIPPED_LOG, base=L.LOG)
    for lat in (12, 18, 24):
        for wl, glass in ((ww, "minecraft:pink_stained_glass"), (we, "minecraft:magenta_stained_glass")):
            D.window_slit(wl, lat, GROUND + 4, GROUND + 8, glow=PEARL if wl is ww else VERDANT, glass=glass)
    for z in (9, 14, 22):
        for x in (2, 15):
            standing_stone(h, x, z, 2)
    for door in (("n", 13), ("s", 4), ("e", 4), ("w", 31)):
        D.door_trim(h, *door)
    D.cornice(h)
    for z in (11, 17, 23):                           # toadstool booths against the walls: a stem in the first column, a cap that arches over the aisle
        toadstool(h, 2, z, 3, r=2, cap="minecraft:red_mushroom_block")
        toadstool(h, 15, z + 3, 3, r=2, cap="minecraft:brown_mushroom_block")
    K.overgrowth(h, "ns", p_moss=0.08, p_vine=0.0)
    K.overgrowth(h, "we", p_moss=0.05, p_vine=0.18)
    moss_floor(h, 2, 2, 6, 33, p_carpet=0.2, p_petal=0.1, p_azalea=0.02)
    moss_floor(h, 12, 2, 15, 33, p_carpet=0.2, p_petal=0.1, p_azalea=0.02)
    h.faction("cultist", 4, 20)
    D.auto_light(h)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


# ===================================================================================================================
# HALL: the hollow tree (3 x 3 modules, 26 rows: a spiral ramp, a gallery and a bridge inside a trunk)
# ===================================================================================================================
ramp_levels = K.ramp_levels          # the helical-ramp helpers live in the kit now (the Crystal Geode's balcony stairs use them too)
build_ramp = K.build_ramp


@T.room("hall", "hollow_tree", faction="beast", view_h=20,
        cuts=[{"name": "west_half", "x": (0, 13), "y": (8, 36), "z": None}, {"name": "north_half", "x": None, "y": (8, 36), "z": (0, 13), "rot": 1}])
def hollow_tree(path):
    """Why: the Feywild's ancient tree is its cathedral. A trunk nineteen blocks across with four arched entrances stands in the middle of a 27 x 27 hall; inside it is hollow
    to the sky: a pool and a giant flower on the floor, a spiral ramp climbing the inside of the bark to a gallery, a plank bridge across the hollow, and a moon (a disc of
    glass lit from above) shining down the shaft. Outside the trunk the four corners are glades of flowers, toadstools and standing stones under a canopy of cherry leaves
    that hangs from the ceiling. It is the hall of strongly vertical play: the party fights on the floor, on the ramp (one at a time), on the gallery (the high ground)
    and on the bridge (a place to be pushed off). The doors are the four compass points, each leading into an entrance arch of the trunk."""
    h = Hall(path, 3, 3, 26, wear=0.06)
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    cx = cz = 13
    top = h.top
    R_IN, R_OUT = 6.5, 9.5
    # 1. the four entrance tunnels (3 wide, 4 high, through the bark), then the trunk: bark (logs, a quarter of them stripped) from the floor to row 27
    tunnel = set()
    for (ox, oz) in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        for d in range(6, 11):
            for lat in (-1, 0, 1):
                tx = cx + ox * d + (lat if oz else 0)
                tz = cz + oz * d + (lat if ox else 0)
                for y in range(GROUND, GROUND + 4):
                    tunnel.add((tx, y, tz))
    for (x, z) in K.ring(cx, cz, R_OUT, R_IN):
        for y in range(GROUND, GROUND + 19):
            if (x, y, z) in tunnel or not h.can_set(x, y, z, L.LOG):
                continue
            h.set(x, y, z, L.LOG if h.rng.random() < 0.75 else L.STRIPPED_LOG, {"axis": "y"})
    for (ox, oz) in ((0, -1), (0, 1), (-1, 0), (1, 0)):          # the lintel of every arch: a stripped log across, an amethyst keystone, roots at the feet
        for d in range(7, 10):
            for lat in range(-2, 3):
                tx = cx + ox * d + (lat if oz else 0)
                tz = cz + oz * d + (lat if ox else 0)
                h.set(tx, GROUND + 4, tz, L.CHISELED if lat == 0 else L.STRIPPED_LOG, None if lat == 0 else {"axis": "x" if oz else "z"}, over=True)
    for (x, z) in K.disc(cx, cz, 5.0):               # the hollow's floor: moss
        h.floor_set(x, z, "minecraft:moss_block")
    # 2. the spiral ramp (a stair up the inside of the bark), the gallery at its top, the bridge across the hollow
    ramp = ramp_levels(cx, cz, 5.0, 6.5, 100.0, 261.0, 11.5)
    lt = max(ramp.values())
    gal = [(x, z) for (x, z) in K.ring(cx, cz, 6.5, 5.0) if (x, z) not in ramp and (K.angle(x, z, cx, cz) - 261.0) % 360.0 <= 199.0]
    build_ramp(h, ramp, extra={c: lt for c in gal}, cx=cx, cz=cz)
    deck_row = GROUND + lt - 1
    for (x, z) in gal:
        h.try_set(x, deck_row, z, L.PLANKS)
    for x in range(11, 16):
        for z in range(7, 20):
            if K.dist(x, z, cx, cz) <= 6.7:
                h.try_set(x, deck_row, z, L.PLANKS)
    for (x, z) in gal:                                 # the rail: the inner edge of the gallery (not within 12 degrees of the stair's head) and both edges of the bridge
        r = K.dist(x, z, cx, cz)
        if r <= 5.8 and (K.angle(x, z, cx, cz) - 261.0) % 360.0 > 12.0 and not (11 <= x <= 15):
            h.try_set(x, deck_row + 1, z, L.FENCE)
    for z in range(8, 19):
        for x in (11, 15):
            if h.get(x, deck_row, z) is not None:
                h.try_set(x, deck_row + 1, z, L.FENCE)
    # 3. the floor of the hollow: a pool with a giant flower on its island, wisps under the bridge, the moon over all
    K.sealed_pool(h, [c for c in K.disc(cx, cz, 1.0) if c != (cx, cz)], depth=1)
    giant_flower(h, cx, cz, 9, r=2, petal=PINK, core=PEARL)
    for (x, z) in ((cx - 1, cz - 1), (cx + 1, cz + 1), (cx + 1, cz - 1)):
        c = h.get(x, FLOOR, z)
        if c is not None and c.block == "minecraft:water":
            h.try_set(x, GROUND, z, "minecraft:lily_pad")
    for z in (9, 11, 15, 17):
        for x in (12, 14):
            K.hang_chain_light(h, x, z, deck_row - 1, 3 + (x + z) % 3)
    for (x, z) in K.disc(cx, cz, 3.0):
        h.shell_set(x, top + 1, z, "minecraft:white_stained_glass")
        h.shell_set(x, top + 2, z, "minecraft:sea_lantern")
    # 4. the canopy: cherry leaves from an umbrella-shaped underside up to the ceiling, with glowing blossoms; the shaft over the hollow stays open
    leafmix = (("cherry", 0.82), ("flowering_azalea", 0.12), ("azalea", 0.06))
    for z in range(2, 25):
        for x in range(2, 25):
            r = K.dist(x, z, cx, cz)
            if r < 3.2:
                continue
            under = GROUND + 19 + 1 - int(round(4 * min(1.0, r / 11.5) ** 2))
            for y in range(under, top + 1):
                if h.rng.random() < 0.06 and y > under:
                    continue
                u = h.rng.random()
                kind = "cherry"
                acc = 0.0
                for k, wgt in leafmix:
                    acc += wgt
                    if u < acc:
                        kind = k
                        break
                K.leaf(h, x, y, z, kind)
    for k in range(18):                               # glowing blossoms in the underside of the canopy: froglights set into the leaves where they are lowest
        ang = k * 20.0 + 7.0
        r = 4.0 + (k % 4) * 2.2
        x, z = int(round(cx + r * math.cos(math.radians(ang)))), int(round(cz + r * math.sin(math.radians(ang))))
        under = GROUND + 19 + 1 - int(round(4 * min(1.0, K.dist(x, z, cx, cz) / 11.5) ** 2))
        c = h.get(x, under, z)
        if c is not None and c.block.endswith("_leaves"):
            h.set(x, under, z, PEARL if k % 3 else VERDANT, over=True)
    # 5. the four glades outside the trunk: a flower, a toadstool, standing stones, moss and petals
    for (sx_, sz_, fl, pet, cap) in ((1, 1, 8, PINK, "minecraft:red_mushroom_block"), (-1, 1, 6, MAGENTA, "minecraft:brown_mushroom_block"),
                                     (1, -1, 6, MAGENTA, "minecraft:brown_mushroom_block"), (-1, -1, 8, PINK, "minecraft:red_mushroom_block")):
        def at(dx, dz):
            return (3 + dx if sx_ > 0 else 23 - dx, 3 + dz if sz_ > 0 else 23 - dz)
        giant_flower(h, *at(1, 1), fl, r=2, petal=pet)
        toadstool(h, *at(5, 1), 3, r=2, cap=cap)
        standing_stone(h, *at(0, 6), 3, lamp=True)
        standing_stone(h, *at(6, 5), 2, lamp=True)
    for door in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        D.door_trim(h, *door)
    K.overgrowth(h, "nsew", p_moss=0.06, p_vine=0.10, rows=range(GROUND + 1, GROUND + 18), door_gap=3)
    moss_floor(h, 2, 2, 24, 24, p_carpet=0.22, p_petal=0.10, p_azalea=0.025)
    h.faction("beast", 4, 13)
    D.auto_light(h)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


def cherry_tree(h, x, z, height, crown, kind="cherry", glow=PEARL):
    """A cherry tree: a 2 x 2 trunk of log `height` tall (the foot on the floor row GROUND) and a crown of leaves (an ellipsoid of horizontal radius `crown`) round its
    top, a few blossoms that glow (froglights) set into the crown's underside. Returns False when the trunk cells are not free."""
    cells = [(x + dx, y, z + dz) for dx in (0, 1) for dz in (0, 1) for y in range(GROUND, GROUND + height)]
    if not all(h.can_set(cx, cy, cz, L.LOG) for (cx, cy, cz) in cells):
        return False
    for (cx, cy, cz) in cells:
        h.set(cx, cy, cz, L.LOG, {"axis": "y"})
    ctop = GROUND + height
    blob(h, x + 0.5, ctop + 1, z + 0.5, crown, max(2.0, crown * 0.55), crown, lambda a, b, c: K.leaf(h, a, b, c, kind), density=0.93)
    for k in range(3):
        a = x + int(round(crown * 0.7 * (1 if k % 2 == 0 else -1))) + (k - 1)
        c = z + int(round(crown * 0.5 * (1 if k < 2 else -1)))
        cell = h.get(a, ctop, c)
        if cell is not None and cell.block.endswith("_leaves"):
            h.set(a, ctop, c, glow, over=True)
    return True


def blob(h, cx, cy, cz, rx, ry, rz, fn, density=1.0):
    return K.blob(h, cx, cy, cz, rx, ry, rz, fn, density)


@T.room("hall", "moonlit_glade", faction="beast", view_h=17,
        cuts=[{"name": "west_half", "x": (0, 13), "y": (8, 28), "z": None}])
def moonlit_glade(path):
    """Why: the grove has to have a place that is only beautiful, so that the rest can be dangerous by contrast: a glade in a hall. A pond keeps the moon (a disc of glass
    lit from above shines down on it), an arched plank bridge crosses it north to south, a ring path three wide runs round it from door to door, and the margin between the
    path and the walls is the garden: giant glowing flowers, cherry trees with glowing blossoms in the corners, standing stones that carry a settled wisp each, toadstools
    and moss. It is open and bright, which is the trick: nothing in it gives cover, and whatever comes out of the pond does not announce itself."""
    h = Hall(path, 3, 3, 18, wear=0.06)
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    cx = cz = 13
    top = h.top
    # the ring path (three wide) round the pond, declared first so that nothing of the garden can stand on it
    h.route(5, 5, 21, 7, label="north bar")
    h.route(5, 19, 21, 21, label="south bar")
    h.route(5, 5, 7, 21, label="west bar")
    h.route(19, 5, 21, 21, label="east bar")
    # the pond (two deep, shut in by the floor) and the bridge over it: a deck of planks one block over the water, 5 wide, a stair up at each end, rails on the edges
    pool_all = set(K.disc(cx, cz, 4.0))
    K.sealed_pool(h, [c for c in pool_all if not (11 <= c[0] <= 15)], depth=2)
    for z in range(cz - 5, cz + 6):
        for x in range(11, 16):
            if (x, z) in pool_all:
                h.set(x, GROUND, z, L.PLANKS, over=True)
            else:
                h.try_set(x, GROUND, z, L.PLANKS)
    for x in range(11, 16):
        h.try_set(x, GROUND, cz - 6, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
        h.try_set(x, GROUND, cz + 6, L.PLANKS_STAIRS, {"facing": "north", "half": "bottom"})
    for z in range(cz - 5, cz + 6):
        for x in (11, 15):
            h.try_set(x, GROUND + 1, z, L.FENCE)
    for (x, z) in ((cx - 6, cz - 3), (cx + 6, cz + 3)):          # the pond's two guardians: giant flowers on the bank, outside the path
        pass
    # the moon
    for (x, z) in K.disc(cx, cz, 3.5):
        h.shell_set(x, top + 1, z, "minecraft:white_stained_glass")
        h.shell_set(x, top + 2, z, "minecraft:sea_lantern")
    # the garden in the margin outside the path: trees in the corners, flowers, lamps on standing stones, toadstools
    for (tx, tz) in ((2, 2), (23, 2), (2, 23), (23, 23)):
        cherry_tree(h, tx if tx < 13 else tx - 1, tz if tz < 13 else tz - 1, 9, 5.0)
    for k, (x, z) in enumerate(((3, 9), (3, 17), (23, 9), (23, 17), (9, 3), (17, 3), (9, 23), (17, 23))):
        giant_flower(h, x, z, 4 + (k % 3), r=1, petal=PINK if k % 2 else MAGENTA)
    for (x, z) in ((3, 11), (3, 15), (23, 11), (23, 15), (11, 3), (15, 3), (11, 23), (15, 23)):
        standing_stone(h, x, z, 3, lamp=True)
    for (x, z, cap) in ((3, 6, "minecraft:red_mushroom_block"), (23, 20, "minecraft:red_mushroom_block"), (6, 23, "minecraft:brown_mushroom_block"),
                        (20, 3, "minecraft:brown_mushroom_block")):
        toadstool(h, x, z, 3, r=2, cap=cap)
    for door in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        D.door_trim(h, *door)
    K.overgrowth(h, "nsew", p_moss=0.05, p_vine=0.12, door_gap=3)
    moss_floor(h, 2, 2, 24, 24, p_carpet=0.2, p_petal=0.14, p_azalea=0.025)
    h.faction("beast", 9, 8)
    D.auto_light(h)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ===================================================================================================================
# SET PIECE: the Court of the Four Seasons (4 x 4 modules)
# ===================================================================================================================
WHEEL = (("pink_wool", 180, 270), ("lime_wool", 270, 360), ("orange_wool", 0, 90), ("light_blue_wool", 90, 180))     # NW spring, NE summer, SE autumn, SW winter (angle: 0 = +x, 90 = +z)


def season_tree(h, x, z, height, crown, leaf_blocks, log=L.LOG):
    """A tree for one season: a 2 x 2 trunk and a crown of `leaf_blocks` (a list of (block, props or None) picked at random), glued to the trunk. Used for the autumn crown (wool) and the
    winter spruce; the spring and summer trees use real leaves (`cherry_tree`)."""
    cells = [(x + dx, y, z + dz) for dx in (0, 1) for dz in (0, 1) for y in range(GROUND, GROUND + height)]
    if not all(h.can_set(cx, cy, cz, log) for (cx, cy, cz) in cells):
        return False
    for (cx, cy, cz) in cells:
        h.set(cx, cy, cz, log, {"axis": "y"})

    def put(a, b, c):
        blk, props = leaf_blocks[h.rng.randrange(len(leaf_blocks))]
        return h.try_set(a, b, c, blk, props)
    blob(h, x + 0.5, GROUND + height + 1, z + 0.5, crown, max(2.0, crown * 0.6), crown, put, density=0.95)
    return True


def season_quad(h, season, x0, z0, x1, z1):
    """One quarter of the court (x0..x1, z0..z1 is its garden, outside the paths): a tree, a giant flower or an ice spire, the ground and the plants of the season."""
    cx, cz = (x0 + x1) // 2, (z0 + z1) // 2
    sx = -1 if x0 < 18 else 1          # the quarter's direction away from the middle of the court
    sz = -1 if z0 < 18 else 1
    tx = x0 if sx < 0 else x1 - 1
    tz = z0 if sz < 0 else z1 - 1
    if season == "spring":
        cherry_tree(h, tx, tz, 11, 5.0)
        giant_flower(h, cx + 2 * -sx, cz + 2 * -sz, 8, r=2, petal=PINK)
        giant_flower(h, x1 - 1 if sx < 0 else x0 + 1, z1 - 1 if sz < 0 else z0 + 1, 5, r=1, petal=MAGENTA)
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                r = h.rng.random()
                if h.free(x, GROUND, z) and h.get(x, FLOOR, z) is not None:
                    h.floor_set(x, z, "minecraft:moss_block")
                    if r < 0.18:
                        h.try_set(x, GROUND, z, "minecraft:pink_petals", {"flower_amount": str(h.rng.randint(1, 4)), "facing": h.rng.choice(("north", "east", "south", "west"))})
                    elif r < 0.24:
                        h.try_set(x, GROUND, z, "minecraft:flowering_azalea")
    elif season == "summer":
        season_tree(h, tx, tz, 11, 5.0, [("minecraft:oak_leaves", {"persistent": "true", "distance": "1"})] * 6 + [("minecraft:azalea_leaves", {"persistent": "true", "distance": "1"})])
        giant_flower(h, cx + 2 * -sx, cz + 2 * -sz, 8, r=2, petal="minecraft:yellow_stained_glass", core="minecraft:ochre_froglight")
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                r = h.rng.random()
                if h.free(x, GROUND, z) and h.get(x, FLOOR, z) is not None:
                    h.floor_set(x, z, "minecraft:moss_block")
                    if r < 0.30:
                        h.try_set(x, GROUND, z, "minecraft:short_grass")
                    elif r < 0.38:
                        h.try_set(x, GROUND, z, h.rng.choice(("minecraft:dandelion", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:allium", "minecraft:poppy")))
                    elif r < 0.41 and h.free(x, GROUND + 1, z):
                        h.set(x, GROUND, z, "minecraft:sunflower", {"half": "lower"})
                        h.set(x, GROUND + 1, z, "minecraft:sunflower", {"half": "upper"})
    elif season == "autumn":
        crown = [("minecraft:orange_wool", None)] * 3 + [("minecraft:red_wool", None)] * 2 + [("minecraft:brown_wool", None)]
        season_tree(h, tx, tz, 10, 4.8, crown)
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                r = h.rng.random()
                if h.free(x, GROUND, z) and h.get(x, FLOOR, z) is not None:
                    if r < 0.40:
                        h.try_set(x, GROUND, z, h.rng.choice(("minecraft:orange_carpet", "minecraft:red_carpet", "minecraft:brown_carpet", "minecraft:yellow_carpet")))
                    elif r < 0.44:
                        h.try_set(x, GROUND, z, "minecraft:pumpkin")
                    elif r < 0.455:
                        h.try_set(x, GROUND, z, "minecraft:jack_o_lantern", {"facing": h.rng.choice(("north", "east", "south", "west"))})
                    elif r < 0.48:
                        h.try_set(x, GROUND, z, "minecraft:hay_block", {"axis": "y"})
        toadstool(h, cx + 2 * -sx, cz + 2 * -sz, 3, r=2, cap="minecraft:brown_mushroom_block")
    else:
        crown = [("minecraft:spruce_leaves", {"persistent": "true", "distance": "1"})] * 4 + [("minecraft:snow_block", None)] * 2
        season_tree(h, tx, tz, 11, 4.5, crown, log=L.LOG)
        for z in range(z0, z1 + 1):
            for x in range(x0, x1 + 1):
                r = h.rng.random()
                if h.free(x, GROUND, z) and h.get(x, FLOOR, z) is not None:
                    if r < 0.45:
                        h.floor_set(x, z, "minecraft:snow_block")
                    elif r < 0.55:
                        h.floor_set(x, z, "minecraft:packed_ice")
        for k, (dx, dz, hh) in enumerate(((2, 0, 7), (-2, 3, 5), (0, -3, 6), (3, -2, 4))):       # ice spires with a pale lantern in the tip
            ix, iz = cx + dx * -sx, cz + dz * -sz
            if all(h.can_set(ix, GROUND + y, iz, "minecraft:blue_ice") for y in range(hh + 2)):
                for y in range(hh):
                    h.set(ix, GROUND + y, iz, "minecraft:blue_ice" if y % 2 else "minecraft:packed_ice")
                h.set(ix, GROUND + hh, iz, "minecraft:packed_ice")
                h.set(ix, GROUND + hh + 1, iz, L.SOUL_LANTERN, {"hanging": "false"})


@T.room("set", "court_of_seasons", faction="cultist", view_h=22,
        cuts=[{"name": "west_half", "x": (0, 18), "y": (8, 34), "z": None}])
def court_of_seasons(path):
    """Why: the Feywild has courts, and the Seelie and Unseelie courts follow the seasons (spring, summer, autumn, winter) round a throne: the set piece of the theme is that
    court. A hall 36 x 36 is cut into four gardens by a square path along the walls and a cross through the middle, one garden per season (cherry blossom and petals; green
    leaves and sunflowers; wool-red leaves, pumpkins and fallen carpets; snow, blue ice and pale lanterns), a wheel of four coloured wool wedges in the floor where the paths
    meet, a chandelier hung over the wheel, and over each garden a disc of coloured glass lit from above, so every quarter has its own light. The doors are a pinwheel (north
    and east in the first module pair, south and west in the second), so a party enters one season and sees all four. It is the room the party remembers."""
    h = Hall(path, 4, 4, 22, wear=0.05)
    h.door("n", 13)
    h.door("e", 13)
    h.door("s", 22)
    h.door("w", 22)
    top = h.top
    # the ring path along the walls and the two spokes of the cross (declared first: nothing of the gardens may stand on a path)
    h.route(4, 4, 31, 6, label="north walk")
    h.route(4, 29, 31, 31, label="south walk")
    h.route(4, 4, 6, 31, label="west walk")
    h.route(29, 4, 31, 31, label="east walk")
    h.route(17, 7, 19, 13, label="north spoke")
    h.route(17, 23, 19, 28, label="south spoke")
    h.route(7, 17, 13, 19, label="west spoke")
    h.route(23, 17, 28, 19, label="east spoke")
    # the wheel of the seasons in the floor, a pool in its hub, a chandelier over it
    for (x, z) in K.disc(18, 18, 4.0):
        a = K.angle(x, z, 18, 18)
        for (wool, a0, a1) in WHEEL:
            if a0 <= a < a1:
                h.floor_set(x, z, "minecraft:" + wool)
    K.sealed_pool(h, [c for c in K.disc(18, 18, 1.0)], depth=1)
    # the four gardens, one season each
    season_quad(h, "spring", 8, 8, 16, 16)
    season_quad(h, "summer", 20, 8, 28, 16)
    season_quad(h, "autumn", 20, 20, 28, 28)
    season_quad(h, "winter", 8, 20, 16, 28)
    # a disc of coloured glass lit from above over every garden: its own light
    for (gx, gz, glass) in ((12, 12, "minecraft:pink_stained_glass"), (24, 12, "minecraft:lime_stained_glass"), (24, 24, "minecraft:orange_stained_glass"),
                            (12, 24, "minecraft:light_blue_stained_glass")):
        for (x, z) in K.disc(gx, gz, 3.0):
            h.shell_set(x, top + 1, z, glass)
            h.shell_set(x, top + 2, z, "minecraft:sea_lantern")
    D.chandelier_wheel(h, 18, 18, r=3, drop=7)
    for door in (("n", 13), ("e", 13), ("s", 22), ("w", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    for side in "nsew":
        for lat in (9, 27):
            K.flush_pilaster(h.wall(side), lat, GROUND, top, shaft=L.STRIPPED_LOG, base=L.LOG)
    K.overgrowth(h, "nsew", p_moss=0.04, p_vine=0.08, rows=range(GROUND + 1, GROUND + 14), door_gap=3)
    K.wall_lights(h, [PEARL, VERDANT], spacing=5, y=GROUND + 4)
    h.faction("cultist", 10, 5)
    D.auto_light(h)
    D.auto_markers(h, spawns=9, patrols=3)
    return h


# ===================================================================================================================
# PUZZLE: the fairy ring (3 x 2 modules, four sigil plates, a riddle stone, a puzzle door and a closet of two chests)
# ===================================================================================================================
@T.room("puzzle", "fairy_ring", faction="beast", no_spawn=True, kind="puzzle", view_h=12)
def fairy_ring(path):
    """Why: a fairy ring is the oldest trick of the Feywild: step into the circle in the wrong order and you dance until the morning. The puzzle of the Spire (four sigil
    plates, a Riddle Stone that names the order, a puzzle door that opens only to it) is staged as that ring: eight standing stones in a circle, a moon-well in the
    middle, the four plates in the grass inside the ring, a ring of petals and glowing flowers outside it and the stone that tells the order on a stump of the hall's far
    wall. The door and the closet behind it are the shared mechanic (`gen_mega_rooms2.sigil_closet`, same blocks, same block entity); the order is Wrong-Way-Round on
    purpose: 1, 0, 3, 2, neither of the two orders the halls of 1.54.0 ask for."""
    mt = MT(path, 3, 2)
    rng = mt.rng
    mt.room(12, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("w", 13)
    mt.socket("n", 13)
    R2.sigil_closet(mt, Frame(mt, 18, FR + 1, 8, "east"), rng)
    cx, cz = 11, 8
    for z in range(2, 16):
        for x in range(2, 19):
            r = rng.random()
            mt.set(x, FR, z, "minecraft:moss_block" if r < 0.5 else L.TILE if r < 0.92 else L.CRACKED_TILE)
    # the moon-well (a 2 x 2 pool shut in by the floor) and the plates round it
    for (x, z) in ((10, 8), (11, 8), (10, 9), (11, 9)):
        mt.set(x, FR - 1, z, L.TILE)
        mt.set(x, FR, z, "minecraft:water")
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for (x, z), c in zip(((8, 5), (13, 5), (8, 11), (13, 11)), colours):
        mt.set(x, FR + 1, z, R2.PLATE, {"color": str(c), "lit": "false"})
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            mt.set(x + dx, FR, z + dz, L.POLISHED)
        mt.set(x, FR, z, L.CHISELED)
    mt.set(11, FR + 1, 14, R2.RIDDLE, None, {"id": R2.RIDDLE, "sequence": R2.nbt_list(FAIRY_ORDER)})
    mt.set(11, FR + 2, 14, L.LANTERN, {"hanging": "false"})
    # the ring: eight standing stones (three cobbled blocks and a slab), three stripped-log pillars crowned by froglights in the corners, four tall flowers round the edge
    for k in range(8):
        a = math.radians(45 * k + 22.5)
        x, z = int(round(cx + 5.6 * math.cos(a))), int(round(cz + 5.0 * math.sin(a)))
        if not (3 <= x <= 17 and 3 <= z <= 14):
            continue
        for y in range(FR + 1, FR + 4):
            D2.put_free(mt, x, y, z, L.COBBLED)
        D2.put_free(mt, x, FR + 4, z, L.COBBLED_SLAB, {"type": "bottom"})
    for (x, z) in ((4, 3), (17, 3), (4, 14)):
        for y in range(FR + 1, FR + 5):
            D2.put_free(mt, x, y, z, L.STRIPPED_LOG, {"axis": "y"})
        D2.put_free(mt, x, FR + 5, z, PEARL)
    for (x, z, pet) in ((5, 4, PINK), (16, 6, MAGENTA), (6, 13, MAGENTA), (15, 13, PINK)):
        for y in range(FR + 1, FR + 7):
            D2.put_free(mt, x, y, z, LIME_PANE)
        D2.put_free(mt, x, FR + 7, z, PEARL)
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
            D2.put_free(mt, x + dx, FR + 6, z + dz, pet)
    for z in range(2, 16):
        for x in range(2, 19):
            c = mt.get(x, FR, z)
            if c is not None and c.block == "minecraft:moss_block" and mt.get(x, FR + 1, z) is None and rng.random() < 0.16:
                D2.put_free(mt, x, FR + 1, z, "minecraft:pink_petals", {"flower_amount": str(rng.randint(1, 4)), "facing": rng.choice(("north", "east", "south", "west"))})
    L.chandelier(mt, 11, FR + 13, 8, drop=6, radius=2)
    for (side, lat, glow, glass) in (("s", 6, PEARL, "minecraft:pink_stained_glass"), ("s", 13, VERDANT, "minecraft:magenta_stained_glass"),
                                     ("n", 5, VERDANT, "minecraft:pink_stained_glass")):
        D2.lit_window(mt, D2.inner_frame(mt, side, lat), width=1, y0=3, height=5, glass=glass, backing=glow)
    K.mt_wall_lights(mt, [PEARL, VERDANT], spacing=5, y=FR + 4)
    for z in (4, 12):                                     # the partition of the closet carries two glowing blossoms too
        mt.set(19, FR + 3, z, VERDANT if z < 8 else PEARL)
    mt.marker(7, FR + 1, 8, "patrol")
    mt.marker(15, FR + 1, 8, "patrol")
    mt.marker(5, FR + 1, 6, "faction", faction="beast")
    mt.finish()
    return mt


#: the order the Riddle Stone asks: not left to right, not the order of either sigil hall of 1.54.0 (2 3 1 0 and 0 2 1 3)
FAIRY_ORDER = (1, 0, 3, 2)


if __name__ == "__main__":
    sys.exit(K.run(T, sys.argv[1:]))

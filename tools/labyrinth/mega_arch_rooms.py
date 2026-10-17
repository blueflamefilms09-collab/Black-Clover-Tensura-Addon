#!/usr/bin/env python3
"""
The plazas and the crossroads hall of the Mega-Dungeon (1.54.0, docs/labyrinth/mega_arch.md): the big circulation rooms where lanes meet. Imported by
`gen_mega_arch.py`, which writes them to `structure/mega/junction/*.nbt`.

Why they share one skeleton: a plaza (2x2 or 3x3 modules) has a door at EVERY module centre of EVERY side (8 or 12 sockets). Wherever the assembler
attaches something to a door, a three-wide lane of polished stone should lead from it across the room, so every plaza is drawn as a grid of such lanes
(`plaza_floor`), with a rib over every lane line (`plaza_ceiling`), frame columns that run up the walls from every door, banners over the doors, torches
on the columns and a tall niche in every four-cell bay between two doors (`plaza_walls`). The feature in the middle (a fountain, a fire pit, a ring of
statues, a well) is the only thing that differs, plus the variant's dressing; that is why the three variants of one size are recognisably one room and
yet different places.

Why the middle of a 2x2 plaza is a statue and not a pillar: the room is 18 blocks wide, so its centre is the line between two cells. A block cannot stand
on that line, an armour stand (an entity) can.

Water: every water cell is a source block with full blocks on its four sides and below, so the game's fluid tick finds nothing to spread into.
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from mega_lib import MT  # noqa: E402
import mega_arch_kit as K  # noqa: E402
from mega_arch_kit import N, E, S, W, ROW0, FLOOR, Style, Wall, seeded, hang_lantern, marker_standing  # noqa: E402

OPPOSITE = {N: S, S: N, E: W, W: E}


# =====================================================================================================================
# the shared skeleton
# =====================================================================================================================
STYLES = {
    (2, "a"): Style("a", 0.08, floor="runner", lights="lantern"),
    (2, "b"): Style("b", 0.14, floor="checker", lights="mixed"),
    (2, "c"): Style("c", 0.12, floor="runner", lights="mixed"),
    (3, "a"): Style("a", 0.08, floor="runner", lights="lantern"),
    (3, "b"): Style("b", 0.12, floor="checker", lights="mixed"),
    (3, "c"): Style("c", 0.16, floor="runner", lights="mixed", cobwebs=0.3),
}
#: the niche in each four-cell bay between two doors, per variant (the list is cycled round the walls)
BAYS = {
    (2, "a"): ["statue"],
    (2, "b"): ["banners", "brazier"],
    (2, "c"): ["window", "shrine"],
    (3, "a"): ["shrine", "window", "statue", "window"],
    (3, "b"): ["casks", "library", "casks", "banners"],
    (3, "c"): ["banners", "brazier", "skulls", "statue"],
}


def plaza_floor(mt, style, doors, lo, hi, rng):
    """Three-wide lanes of polished stone run from every door across the plaza; between them tile with a few cracks; a medallion where lanes cross."""
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            lane_x = any(abs(x - c) <= 1 for c in doors)
            lane_z = any(abs(z - c) <= 1 for c in doors)
            if lane_x or lane_z:
                if style.floor == "checker" and not (lane_x and lane_z) and (x + z) % 2:
                    mt.set(x, FLOOR, z, L.TILE)
                else:
                    mt.set(x, FLOOR, z, L.POLISHED)
            else:
                mt.set(x, FLOOR, z, L.CRACKED_TILE if rng.random() < 0.12 else L.TILE)
    for cx in doors:
        for cz in doors:
            K.medallion(mt, cx, cz, 1, L.CHISELED, L.TILE)


def plaza_ceiling(mt, doors, lo, hi, top):
    """A cove under the ceiling along all four walls, coved ribs over every lane line, a boss where two lanes cross. Returns the crossing cells.
    Where a rib meets a wall the rib's own corbel stair stands in place of the cove stair."""
    lines = set(doors)
    for t in range(lo, hi + 1):
        for (x, z, f) in ((lo, t, W), (hi, t, E), (t, lo, N), (t, hi, S)):
            if x in (lo, hi) and z in (lo, hi):
                mt.set(x, top, z, L.TILE)                                   # the corner of the cove
            elif (x in lines and z in (lo, hi)) or (z in lines and x in (lo, hi)):
                continue                                                    # a rib ends here
            else:
                K.stair(mt, x, top, z, L.TILE_STAIRS, f, "top")
    for c in doors:
        for t in range(lo, hi + 1):
            mt.set(c, top, t, L.POLISHED)
            mt.set(t, top, c, L.POLISHED)
        for (x, z, f) in ((c, lo, N), (c, hi, S), (lo, c, W), (hi, c, E)):
            K.stair(mt, x, top - 1, z, L.POLISHED_STAIRS, f, "top")
    crossings = []
    for cx in doors:
        for cz in doors:
            mt.set(cx, top, cz, L.CHISELED)
            crossings.append((cx, cz))
    return crossings


def plaza_walls(mt, doors, top, bays, rng, rows=None):
    """Frame columns that run up to the cove, a banner over every door, a torch on one column of every door (warm and soul flames alternate along the
    wall), a tall niche in every four-cell bay between two doors with a chiselled and polished course above it."""
    k = 0
    for face in ("n", "e", "s", "w"):
        w = Wall(mt, face)
        for c in doors:
            for lat in (c - 2, c + 2):
                for y in range(ROW0 + 5, top + 1):
                    w.set(lat, 0, y, L.POLISHED)
            col = ("red", "black", "purple")[(k + doors.index(c)) % 3]
            x, y, z = w.cell(c, 1, ROW0 + 6)
            mt.set(x, y, z, "minecraft:%s_wall_banner" % col, {"facing": w.into})
            x, y, z = w.cell(c - 2 if (doors.index(c) + k) % 2 == 0 else c + 2, 1, ROW0 + 2)
            mt.set(x, y, z, "minecraft:soul_wall_torch" if (doors.index(c) + k) % 3 == 1 else "minecraft:wall_torch", {"facing": w.into})
        for a, b in zip(doors, doors[1:]):
            cells = list(range(a + 3, b - 2))                              # the four cells between the two frames
            kind = bays[k % len(bays)]
            k += 1
            K.tall_niche(w, cells, kind, rng, rows=rows or tuple(range(ROW0 + 1, ROW0 + 7)))
            for lat in cells:
                cur = w.get(lat, 0, ROW0 + 7)
                if cur is not None and cur.block in (L.BRICK, L.CRACKED_BRICK):
                    w.set(lat, 0, ROW0 + 7, L.CHISELED if lat % 2 == 0 else L.POLISHED)


def polish_frames(mt, doors):
    """`MT.finish()` writes a door frame's inner layer in bricks; the columns above it (plaza_walls) are polished, so make the frame match. Done after
    finish(): these cells are beside the doorway holes, never in them."""
    for face in ("n", "e", "s", "w"):
        w = Wall(mt, face)
        for c in doors:
            for lat in (c - 2, c + 2):
                for y in range(ROW0, ROW0 + 5):
                    w.set(lat, 0, y, L.POLISHED)


def lane_markers(mt, doors, faction=None):
    """Two patrol nodes on opposite lane crossings, a room-centre marker on a lane cell (the middle is occupied by the feature), two spawn cells on
    lanes and, where it makes sense, the native faction."""
    lo, hi = doors[0], doors[-1]
    marker_standing(mt, lo, lo, "patrol")
    marker_standing(mt, hi, hi, "patrol")
    marker_standing(mt, doors[1] if len(doors) == 3 else lo + 1, lo + 2 if len(doors) == 3 else (lo + hi) // 2, "room_center")
    marker_standing(mt, hi - 1, lo if len(doors) == 3 else lo + 1, "spawn")
    marker_standing(mt, lo + 1, hi if len(doors) == 3 else hi - 1, "spawn", weight=2)
    if faction:
        marker_standing(mt, lo + 1, lo + 1, "faction", faction=faction)


def plaza(modules, variant):
    """A plaza of `modules` x `modules` modules (2 or 3): 8 or 12 doors, lanes, ribs, a feature in the middle. Interior height 10 (2x2) or 12 (3x3)."""
    name = "mega/junction/plaza_%dx%d_%s" % (modules, modules, variant)
    n = 9 * modules
    H = 10 if modules == 2 else 12
    top = ROW0 + H - 1
    style = STYLES[(modules, variant)]
    mt = MT(name, modules, modules)
    rng = seeded(mt)
    mt.room(H, wear=style.wear)
    mt.all_sockets()
    doors = [9 * k + 4 for k in range(modules)]
    lo, hi = 2, n - 3
    plaza_floor(mt, style, doors, lo, hi, rng)
    crossings = plaza_ceiling(mt, doors, lo, hi, top)
    plaza_walls(mt, doors, top, BAYS[(modules, variant)], rng)
    for i, (cx, cz) in enumerate(crossings):
        if modules == 3 and (cx, cz) == (doors[1], doors[1]):
            continue                                                       # the middle crossing hangs over the feature, which lights itself
        hang_lantern(mt, cx, top, cz, drop=3 if modules == 2 else 5, soul=bool(style.lights == "mixed" and i % 2))
    FEATURES[(modules, variant)](mt, rng, doors, top)
    mt.finish()
    polish_frames(mt, doors)
    return mt


# =====================================================================================================================
# the features of the 2x2 plazas
# =====================================================================================================================
def feature_fountain_2x2(mt, rng, doors, top):
    """A square fountain: a polished rim, a ring of still water, a 2x2 plinth carrying a golden statue exactly on the plaza's centre line, a lamp post
    on every corner of the rim."""
    a, b = 6, 11
    for x in range(a, b + 1):
        for z in range(a, b + 1):
            if x in (a, b) or z in (a, b) or (x in (8, 9) and z in (8, 9)):
                mt.set(x, ROW0, z, L.POLISHED)
            else:
                K.water(mt, x, ROW0, z)
    for x in (8, 9):
        for z in (8, 9):
            mt.set(x, ROW0 + 1, z, L.POLISHED)
            mt.set(x, ROW0 + 2, z, L.CHISELED if (x + z) % 2 == 0 else L.POLISHED)
    mt.armor_stand(8, ROW0 + 3, 8, yaw=0.0, head="minecraft:golden_helmet", chest="minecraft:golden_chestplate", legs="minecraft:golden_leggings",
                   feet="minecraft:golden_boots", mainhand="minecraft:golden_sword", dx=1.0, dy=0.0, dz=1.0)      # feet on the plinth's top face, on the corner between its four blocks
    for (x, z) in ((a, a), (b, a), (a, b), (b, b)):
        mt.set(x, ROW0, z, L.CHISELED)
        K.lamp_post(mt, x, z, y0=ROW0 + 1, height=2, soul=True)
    lane_markers(mt, doors)


def feature_brazier_2x2(mt, rng, doors, top):
    """A fire pit on a one-step dais: four campfires (two soul) behind an iron-bar fence with polished corner posts topped by soul lanterns."""
    a, b = 7, 10
    for x in range(a - 1, b + 2):
        for z in range(a - 1, b + 2):
            on_rim = x in (a - 1, b + 1) or z in (a - 1, b + 1)
            at_corner = x in (a - 1, b + 1) and z in (a - 1, b + 1)
            if on_rim and not at_corner:
                mt.set(x, ROW0, z, L.POLISHED_SLAB, {"type": "bottom"})      # the dais step
    for x in range(a, b + 1):
        for z in range(a, b + 1):
            mt.set(x, ROW0, z, L.POLISHED)
            if x in (a, b) and z in (a, b):
                for y in (ROW0 + 1, ROW0 + 2, ROW0 + 3):
                    mt.set(x, y, z, L.POLISHED_WALL, {"up": "true"})
                mt.set(x, ROW0 + 4, z, L.SOUL_LANTERN, {"hanging": "false"})
            elif x in (a, b) or z in (a, b):
                for y in (ROW0 + 1, ROW0 + 2):
                    mt.set(x, y, z, L.IRON_BARS)
            else:
                mt.set(x, ROW0 + 1, z, "minecraft:soul_campfire" if (x + z) % 2 == 1 else "minecraft:campfire",
                       {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    lane_markers(mt, doors)


def feature_statues_2x2(mt, rng, doors, top):
    """A statue ring: a 6x6 dais one step high, a plinth in the middle carrying two soul-lit posts, a statue of a different kit on every corner facing in."""
    a, b = 6, 11
    for x in range(a, b + 1):
        for z in range(a, b + 1):
            if x in (a, b) or z in (a, b):
                mt.set(x, ROW0, z, L.POLISHED_SLAB, {"type": "bottom"})
            else:
                mt.set(x, ROW0, z, L.POLISHED)
    for x in (8, 9):
        for z in (8, 9):
            mt.set(x, ROW0 + 1, z, L.POLISHED)
            mt.set(x, ROW0 + 2, z, L.CHISELED)
    for (x, z) in ((8, 8), (9, 9)):
        for y in range(ROW0 + 3, ROW0 + 6):
            mt.set(x, y, z, L.POLISHED_WALL, {"up": "true"})
        mt.set(x, ROW0 + 6, z, L.SOUL_LANTERN, {"hanging": "false"})
    for (x, z, f, kit) in ((7, 7, S, "iron"), (10, 7, W, "golden"), (7, 10, E, "diamond"), (10, 10, N, "netherite")):
        K.statue(mt, x, ROW0 + 1, z, f, kit=kit)
    lane_markers(mt, doors, faction="construct")


# =====================================================================================================================
# the features of the 3x3 plazas
# =====================================================================================================================
def quarter_corners():
    """The north-west cell of the 2x2 core of each of the four 6x6 blocks between the lanes of a 3x3 plaza."""
    return [(7, 7), (18, 7), (7, 18), (18, 18)]


def lamp_plinth(mt, x, z):
    """A 2x2 stepped plinth (stairs round a polished block) with a tall lamp post on it: the quarters of the fountain plaza stay open, but not dark."""
    for dx in (-1, 0, 1, 2):
        for dz in (-1, 0, 1, 2):
            if dx in (-1, 2) and dz in (-1, 2):
                continue
            if dx in (0, 1) and dz in (0, 1):
                mt.set(x + dx, ROW0, z + dz, L.POLISHED)
            else:
                toward = E if dx == -1 else W if dx == 2 else S if dz == -1 else N       # the high side of the step is toward the plinth
                K.stair(mt, x + dx, ROW0, z + dz, L.POLISHED_STAIRS, toward, "bottom")
    mt.set(x, ROW0 + 1, z, L.CHISELED)
    K.lamp_post(mt, x + 1, z + 1, y0=ROW0 + 1, height=4, soul=False)
    mt.set(x, ROW0 + 1, z + 1, L.POLISHED_SLAB, {"type": "bottom"})
    mt.set(x + 1, ROW0 + 1, z, L.POLISHED_SLAB, {"type": "bottom"})


def planter(mt, x, z):
    """A raised bed round a 2x2 core: a polished-stair rim, moss inside, a flowering azalea, an azalea, a fern and moss carpet, a lantern post on one
    corner. It covers (x-1..x+2) so the lanes stay clear."""
    for dx in range(-1, 3):
        for dz in range(-1, 3):
            if dx in (-1, 2) and dz in (-1, 2):
                mt.set(x + dx, ROW0, z + dz, L.POLISHED)
            elif dx in (-1, 2) or dz in (-1, 2):
                outward = W if dx == -1 else E if dx == 2 else N                  # (dz == 2 is the south edge; dz == -1 the north edge)
                if dz == 2 and dx not in (-1, 2):
                    outward = S
                K.stair(mt, x + dx, ROW0, z + dz, L.POLISHED_STAIRS, OPPOSITE[outward], "bottom")
            else:
                mt.set(x + dx, ROW0, z + dz, "minecraft:moss_block")
    mt.set(x, ROW0 + 1, z, "minecraft:flowering_azalea")
    mt.set(x + 1, ROW0 + 1, z + 1, "minecraft:azalea")
    mt.set(x + 1, ROW0 + 1, z, "minecraft:fern")
    mt.set(x, ROW0 + 1, z + 1, "minecraft:moss_carpet", {"bottom": "true"})
    mt.set(x - 1, ROW0 + 1, z - 1, L.POLISHED)
    mt.set(x - 1, ROW0 + 2, z - 1, L.LANTERN, {"hanging": "false"})


def feature_fountain_3x3(mt, rng, doors, top):
    """A two-tier round fountain on the plaza's centre cell: an outer pond ringed with a polished rim, a raised inner pool round a pillar topped by a soul
    lantern, four lamp posts on the compass points of the rim and a lamp plinth in each quarter. Every water cell is enclosed by full blocks."""
    cx = cz = 13
    r4, r3, r2, r1 = (set(L.disk_cells(cx, cz, r)) for r in (4, 3, 2, 1))
    for (x, z) in r4 - r3:
        mt.set(x, ROW0, z, L.POLISHED)
    for (x, z) in r3 - r2:
        K.water(mt, x, ROW0, z)
    for (x, z) in r2:
        mt.set(x, ROW0, z, L.POLISHED)
    for (x, z) in r2 - r1:
        mt.set(x, ROW0 + 1, z, L.POLISHED)
    for (x, z) in r1:
        if (x, z) != (cx, cz):
            K.water(mt, x, ROW0 + 1, z)
    for y in range(ROW0 + 1, ROW0 + 6):
        mt.set(cx, y, cz, L.POLISHED if y != ROW0 + 3 else L.CHISELED)
    mt.set(cx, ROW0 + 6, cz, L.SOUL_LANTERN, {"hanging": "false"})
    for (x, z) in ((cx - 4, cz), (cx + 4, cz), (cx, cz - 4), (cx, cz + 4)):
        mt.set(x, ROW0, z, L.CHISELED)
        K.lamp_post(mt, x, z, y0=ROW0 + 1, height=2, soul=True)
    for (x, z) in quarter_corners():
        lamp_plinth(mt, x, z)
    hang_lantern(mt, cx, top, cz, drop=5, soul=False)
    lane_markers(mt, doors)


def feature_well_3x3(mt, rng, doors, top):
    """A stone well under a four-post timber canopy with a lantern on its chain, and a moss-and-azalea planter in every quarter (the one green thing in
    the dungeon's stone). The well's water is a single source sunk in a shaft of full blocks."""
    cx = cz = 13
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            x, z = cx + dx, cz + dz
            if (dx, dz) == (0, 0):
                K.water(mt, x, ROW0, z)
                continue
            mt.set(x, ROW0, z, L.POLISHED)
            if dx != 0 and dz != 0:
                mt.set(x, ROW0 + 1, z, L.POLISHED_SLAB, {"type": "bottom"})
            else:
                outward = {(0, -1): N, (0, 1): S, (-1, 0): W, (1, 0): E}[(dx, dz)]
                K.stair(mt, x, ROW0 + 1, z, L.POLISHED_STAIRS, outward, "bottom")      # the high side outward: a collar round the water
    for (dx, dz) in ((-2, -2), (2, -2), (-2, 2), (2, 2)):
        for y in range(ROW0, ROW0 + 5):
            mt.set(cx + dx, y, cz + dz, L.FENCE if y > ROW0 else L.POLISHED)
    ry = ROW0 + 5
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            ring = max(abs(dx), abs(dz))
            if ring == 3:
                if abs(dx) == 3 and abs(dz) == 3:
                    continue
                f = (W if dx == -3 else E) if abs(dx) == 3 else (N if dz == -3 else S)
                K.stair(mt, cx + dx, ry, cz + dz, L.PLANKS_STAIRS, f, "bottom")
            elif ring == 2:
                f = (W if dx == -2 else E) if abs(dx) == 2 and abs(dz) < 2 else (N if dz == -2 else S) if abs(dz) == 2 and abs(dx) < 2 else None
                if f is None:
                    mt.set(cx + dx, ry, cz + dz, L.PLANKS_SLAB, {"type": "bottom"})
                else:
                    K.stair(mt, cx + dx, ry + 1, cz + dz, L.PLANKS_STAIRS, f, "bottom")
            else:
                mt.set(cx + dx, ry + 1, cz + dz, L.PLANKS)
    mt.set(cx, ry, cz, L.PLANKS)
    for k in range(1, 3):
        K.chain(mt, cx, ry - k, cz)
    K.lantern(mt, cx, ry - 3, cz, hanging=True, soul=False)
    for (x, z, f) in ((cx - 3, cz - 1, W), (cx - 3, cz + 1, W), (cx + 3, cz - 1, E), (cx + 3, cz + 1, E)):
        K.stair(mt, x, ROW0, z, L.PLANKS_STAIRS, f, "bottom")                  # benches against the canopy, their backs outward
    for (x, z) in quarter_corners():
        planter(mt, x, z)
    lane_markers(mt, doors)


def feature_statue_ring_3x3(mt, rng, doors, top):
    """A ring of eight statues round a round dais with a fire basket on it, a small brazier in each quarter. The statues stand four blocks from the middle,
    one per compass point and one per diagonal, all facing it; each wears a different kit."""
    cx = cz = 13
    for (x, z) in L.disk_cells(cx, cz, 3):
        if max(abs(x - cx), abs(z - cz)) >= 3:
            mt.set(x, ROW0, z, L.POLISHED_SLAB, {"type": "bottom"})
        else:
            mt.set(x, ROW0, z, L.POLISHED)
    K.small_brazier(mt, cx, cz, soul=False)
    kits = ("iron", "golden", "diamond", "netherite", "chainmail", "iron", "golden", "diamond")
    spots = [(cx - 4, cz, E), (cx + 4, cz, W), (cx, cz - 4, S), (cx, cz + 4, N), (cx - 3, cz - 3, S), (cx + 3, cz - 3, W), (cx - 3, cz + 3, E), (cx + 3, cz + 3, N)]
    for (x, z, f), kit in zip(spots, kits):
        K.statue(mt, x, ROW0, z, f, kit=kit)
    for (x, z) in quarter_corners():
        K.small_brazier(mt, x, z, soul=(x + z) % 2 == 1)
    hang_lantern(mt, cx, top, cz, drop=6, soul=True)
    lane_markers(mt, doors, faction="construct")


FEATURES = {
    (2, "a"): feature_fountain_2x2,
    (2, "b"): feature_brazier_2x2,
    (2, "c"): feature_statues_2x2,
    (3, "a"): feature_fountain_3x3,
    (3, "b"): feature_well_3x3,
    (3, "c"): feature_statue_ring_3x3,
}


# =====================================================================================================================
# the crossroads hall
# =====================================================================================================================
def chapel_shrine(q):
    """NW chapel set piece: a raised block with a golden statue and candles, a lamp post in the back corner."""
    for x in range(6, 9):
        for z in range(6, 9):
            q.set(x, ROW0, z, L.POLISHED)
    q.set(7, ROW0, 7, L.CHISELED)
    xx, _y, zz = q.cell(7, ROW0 + 1, 7)
    K.statue(q.mt, xx, ROW0 + 1, zz, q.f(N), kit="golden", plinth=False)
    for (x, z) in ((6, 6), (8, 6), (6, 8)):
        q.set(x, ROW0 + 1, z, "minecraft:candle", {"candles": "4", "lit": "true"})
    xx, _y, zz = q.cell(8, ROW0 + 1, 8)
    K.lamp_post(q.mt, xx, zz, y0=ROW0 + 1, height=3, soul=True)


def chapel_armoury(q):
    """NE chapel set piece: three armour stands on a rack along the wall, an anvil, a grindstone, a smithing table, a blast furnace, a lava cauldron.
    The furnace is NOT lit: a furnace block entity with no fuel switches its `lit` state off on its first tick, so a template that shows one burning
    would lose the glow (and its light) the moment the chunk starts ticking."""
    mt = q.mt
    for x in (6, 7, 8):
        q.set(x, ROW0, 8, L.POLISHED_SLAB, {"type": "bottom"})
    for (x, kit) in ((6, "iron"), (7, "chainmail"), (8, "netherite")):
        xx, _y, zz = q.cell(x, ROW0, 8)
        mt.armor_stand(xx, ROW0, zz, yaw=K.YAW[q.f(W)], head="minecraft:%s_helmet" % kit, chest="minecraft:%s_chestplate" % kit,
                       legs="minecraft:%s_leggings" % kit, feet="minecraft:%s_boots" % kit, mainhand="minecraft:iron_sword", dy=0.5)
    q.set(6, ROW0, 6, "minecraft:anvil", {"facing": N})
    q.set(7, ROW0, 6, "minecraft:grindstone", {"face": "floor", "facing": N})
    q.set(8, ROW0, 6, "minecraft:smithing_table")
    q.set(8, ROW0, 7, "minecraft:blast_furnace", {"facing": W, "lit": "false"})
    q.set(6, ROW0, 7, "minecraft:lava_cauldron")


def chapel_library(q):
    """SW chapel set piece: an L of bookshelves with two lanterns on top, a lectern with a book, a reading table with a candle and two chairs."""
    for x in (6, 7, 8):
        for y in (ROW0, ROW0 + 1, ROW0 + 2):
            q.set(x, y, 8, "minecraft:bookshelf")
    for z in (6, 7):
        for y in (ROW0, ROW0 + 1, ROW0 + 2):
            q.set(8, y, z, "minecraft:bookshelf")
    K.lectern(q, 6, ROW0, 6, W)
    q.set(7, ROW0, 6, L.PLANKS_SLAB, {"type": "top"})            # a TOP slab: its upper face is level with the candle's cell, so the candle rests on it
    q.set(7, ROW0 + 1, 6, "minecraft:candle", {"candles": "3", "lit": "true"})
    q.set(6, ROW0, 7, L.PLANKS_STAIRS, {"facing": E, "half": "bottom"})
    q.set(7, ROW0, 7, L.PLANKS_STAIRS, {"facing": W, "half": "bottom"})
    q.set(7, ROW0 + 3, 8, L.LANTERN, {"hanging": "false"})
    q.set(8, ROW0 + 3, 6, L.SOUL_LANTERN, {"hanging": "false"})


def chapel_alchemy(q):
    """SE chapel set piece: a counter of brewing stands and a cauldron with a lantern at its end, two more cauldrons (water, powder snow), a pair of potted fungi."""
    for x in (6, 7, 8):
        q.set(x, ROW0, 8, L.PLANKS_SLAB, {"type": "top"})
    q.set(6, ROW0 + 1, 8, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})
    q.set(7, ROW0 + 1, 8, "minecraft:cauldron")
    q.set(8, ROW0 + 1, 8, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "false", "has_bottle_2": "false"})     # no bottles: a brewing stand
    # block entity resets the drawn bottles to its (empty) contents on its first tick, so only the settled state is written
    q.set(8, ROW0, 6, "minecraft:water_cauldron", {"level": "3"})
    q.set(8, ROW0, 7, "minecraft:powder_snow_cauldron", {"level": "3"})
    q.set(6, ROW0, 6, "minecraft:potted_red_mushroom")
    q.set(6, ROW0, 7, "minecraft:potted_crimson_fungus")
    q.set(5, ROW0, 8, L.PLANKS_SLAB, {"type": "top"})            # the lantern stands on a counter cell of its own (a cauldron cannot carry one)
    q.set(5, ROW0 + 1, 8, L.LANTERN, {"hanging": "false"})


#: (mirror in x, mirror in z) -> the set piece of that chapel (each is written for the north-west chapel and mirrored by `K.Quad`)
CHAPELS = {(0, 0): chapel_shrine, (1, 0): chapel_armoury, (0, 1): chapel_library, (1, 1): chapel_alchemy}
HALL_BAYS = ["banners", "statue", "window", "shrine", "banners", "window", "statue", "shrine"]


def crossroads_hall():
    """27 x 27: a cross of two 7-wide arms between four 7x7 chapels, each opening to the arms through a pointed arch; a dais, a fire basket and a
    chandelier where the arms meet. Twelve doors (three on every side), the four arm ends first in the assembler's queue so the hall is a through-way.
    Why chapels: the corners of a cross would otherwise be dead masonry; as chapels they give the hub four different small rooms (a shrine, an
    armoury, a library, an alchemist's bench) and an extra door each from outside."""
    name = "mega/junction/crossroads_hall_3x3"
    n = 27
    H = 12
    top = ROW0 + H - 1
    style = Style("hall", 0.09, lights="mixed")
    mt = MT(name, 3, 3)
    rng = seeded(mt)
    mt.room(H, wear=style.wear)
    doors = [4, 13, 22]
    for side in ("n", "e", "s", "w"):
        for off in doors:
            mt.socket(side, off, selection_priority=1 if off == 13 else 0)
    lo, hi = 2, n - 3
    for x in range(lo, hi + 1):
        for z in range(lo, hi + 1):
            if 10 <= x <= 16 or 10 <= z <= 16:
                mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            else:
                lane = any(abs(x - c) <= 1 for c in (4, 22)) or any(abs(z - c) <= 1 for c in (4, 22))
                mt.set(x, FLOOR, z, L.POLISHED if lane else (L.CRACKED_TILE if rng.random() < 0.12 else L.TILE))
    for t in list(range(lo, 10)) + list(range(17, hi + 1)):
        for k in (12, 13, 14):
            mt.set(k, FLOOR, t, K.runner_block("red"))
            mt.set(t, FLOOR, k, K.runner_block("red"))
    # the dais: a ring of stairs rising toward the middle and a 5x5 platform carrying a fire basket and four lamp posts
    for x in range(10, 17):
        for z in range(10, 17):
            if x in (10, 16) or z in (10, 16):
                K.stair(mt, x, ROW0, z, L.POLISHED_STAIRS, E if x == 10 else W if x == 16 else S if z == 10 else N, "bottom")
            else:
                mt.set(x, ROW0, z, L.POLISHED if (x + z) % 2 == 0 else L.CHISELED)
    K.small_brazier(mt, 13, 13, soul=False, y0=ROW0 + 1)
    for (x, z) in ((11, 11), (15, 11), (11, 15), (15, 15)):
        K.lamp_post(mt, x, z, y0=ROW0 + 1, height=2, soul=True)
    # arcades: a pointed arch on every inner side of every chapel, a polished pier in each corner of the crossing
    spring = ROW0 + 6
    spans = ((9, 2, 8), (17, 2, 8), (9, 18, 24), (17, 18, 24))
    for (line, a, b) in spans:
        K.arch_span(mt, "z", line, a, b, spring, top)
        K.arch_span(mt, "x", line, a, b, spring, top)
    for (x, z) in ((9, 9), (17, 9), (9, 17), (17, 17)):
        for y in range(ROW0, top + 1):
            mt.set(x, y, z, L.POLISHED)
        mt.set(x, ROW0, z, L.CHISELED)
        mt.set(x, spring - 1, z, L.CHISELED)
    crossings = plaza_ceiling(mt, doors, lo, hi, top)
    plaza_walls(mt, doors, top, HALL_BAYS, rng, rows=tuple(range(ROW0 + 1, ROW0 + 6)))
    for (cx, cz) in crossings:
        if (cx, cz) != (13, 13):
            hang_lantern(mt, cx, top, cz, drop=6, soul=(cx + cz) % 2 == 1)
    K.chandelier(mt, 13, top, 13, drop=6, radius=2, arm_lift=2)
    for (line, a, b) in spans:
        zc = (a + b) // 2
        for (x, z) in ((line, zc), (zc, line)):
            key = next(y for y in range(spring, top + 1) if mt.get(x, y, z) is not None)      # the first solid cell above the opening
            hang_lantern(mt, x, key, z, drop=1, soul=True)
    for (mx, mz), fn in CHAPELS.items():
        q = K.Quad(mt, mx, mz, n)
        fn(q)
        xx, _y, zz = q.cell(2, ROW0 + 1, 2)             # a lamp post in the far corner of every chapel
        K.lamp_post(mt, xx, zz, y0=ROW0 + 1, height=3, soul=(mx + mz) % 2 == 0)
        mt.set(xx, ROW0, zz, L.CHISELED)
    marker_standing(mt, 4, 22, "spawn", weight=2)
    marker_standing(mt, 22, 4, "spawn")
    marker_standing(mt, 13, 6, "patrol")
    marker_standing(mt, 13, 20, "patrol")
    marker_standing(mt, 6, 13, "faction", faction="cultist")        # the chapels: shrine, library, alchemy
    mt.finish()
    polish_frames(mt, doors)
    return mt

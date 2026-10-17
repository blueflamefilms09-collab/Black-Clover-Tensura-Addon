#!/usr/bin/env python3
"""
Why: THEME-B (1.59.0, docs/labyrinth/mega_v2_thb.md section 3) gives the Holy road a giant geode: a white calcite shell, a dark basalt floor, amethyst everywhere the crystals grew, light
that comes out of the stone itself (sea lanterns in the lining, the glow of the Heart), echoing galleries, crystal bridges and a puzzle that is a chime. This script writes its rooms
(`structure/mega/<folder>/crystal_geode_*.nbt`, the prefix `data/nusmp/mega/template_themes/crystal_geode.json` claims for the theme), so only a sector of the Crystal Geode theme ever
draws them.

    python3 tools/labyrinth/gen_mega_theme_crystal_geode.py                       # builds, checks and writes every room (a room with a defect is not written)
    python3 tools/labyrinth/gen_mega_theme_crystal_geode.py great_geode --preview   # one room, then render it in its own theme into $THB_PREVIEWS
    python3 tools/labyrinth/gen_mega_theme_crystal_geode.py --check               # CI style: builds in memory, checks, compares with the files on disk

Architecture uses the palette constants of mega_lib (the Region Theme turns them into calcite walls, smooth basalt floors, amethyst pillars, purpur keystones and tuff); the character of the
geode is the free vanilla blocks on top: amethyst blocks and clusters (the growth that gives light), budding amethyst, tinted glass, sea lanterns, calcite and basalt in the shells, dripstone.
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

T = K.Theme("crystal_geode")
FR = L.FLOOR_ROW

AMETHYST = "minecraft:amethyst_block"
BUDDING = "minecraft:budding_amethyst"
CALCITE = "minecraft:calcite"
BASALT = "minecraft:smooth_basalt"
TUFF = "minecraft:tuff"
SEA = "minecraft:sea_lantern"
TINTED = "minecraft:tinted_glass"
GROWTH = ["minecraft:small_amethyst_bud", "minecraft:medium_amethyst_bud", "minecraft:large_amethyst_bud", "minecraft:amethyst_cluster"]
FACING_STEP = {"north": (0, 0, -1), "south": (0, 0, 1), "west": (-1, 0, 0), "east": (1, 0, 0), "up": (0, 1, 0), "down": (0, -1, 0)}


# ===================================================================================================================
# the props of the geode
# ===================================================================================================================
def grow(h, x, y, z, facing, kind=None, rng=None):
    """An amethyst bud or cluster at (x, y, z) pointing `facing` (away from its support): it needs a full block on the opposite side (the game drops a cluster that has none) and a
    free cell. `kind` is an index into GROWTH (small .. cluster) or None for a random one. Returns whether it was placed. Why: the geode's own light and look are the crystals that
    grew on its lining; every one goes through this call so none can float."""
    rng = rng or h.rng
    dx, dy, dz = FACING_STEP[facing]
    sup = h.get(x - dx, y - dy, z - dz)
    if sup is None or not L.is_full(sup.block) or (x - dx, y - dy, z - dz) in h.mt.reserved:
        return False
    k = rng.randrange(4) if kind is None else kind
    return h.try_set(x, y, z, GROWTH[k], {"facing": facing, "waterlogged": False})


def spire(h, x, z, height, y0=GROUND, base=2, core=AMETHYST, glow=True, body=2):
    """A crystal spire: a stepped column of amethyst (a `base` x `base` foot for the first third, a 2 x 2 body, a single block to the point) with a block of tinted glass every fourth
    row and, if `glow`, a sea lantern in the heart of the foot; a cluster crowns it. Returns False (and places nothing) when a cell is taken."""
    cells = []
    third = max(1, height // 3)
    for k in range(height):
        w = base if k < third else body if k < 2 * third else 1
        for dz in range(w):
            for dx in range(w):
                cells.append((x + dx, y0 + k, z + dz, k))
    if not all(h.can_set(cx, cy, cz, core) for (cx, cy, cz, _k) in cells):
        return False
    for (cx, cy, cz, k) in cells:
        blk = TINTED if (k % 4 == 3 and k >= third) else core
        if glow and k == 1 and (cx, cz) == (x, z):
            blk = SEA
        h.set(cx, cy, cz, blk)
    grow(h, x, y0 + height, z, "up", 3)
    return True


def lining_growth(h, p=0.30, cells=None, skip=None):
    """Grows buds and clusters out of the amethyst blocks of `cells` (default: every amethyst block of the template) that have a free cell beside them: probability `p` per face, at
    most one growth per free cell. `skip(x, y, z)` returns True for a cell that must stay clear. The sizes follow a rough rule (the bigger ones are rarer). Returns the number grown."""
    n = 0
    rng = h.rng
    pool = sorted(cells) if cells is not None else sorted(h.mt.t.cells)
    for (x, y, z) in pool:
        c = h.mt.t.cells.get((x, y, z))
        if c is None or c.block not in (AMETHYST, BUDDING):
            continue
        for facing, (dx, dy, dz) in FACING_STEP.items():
            tx, ty, tz = x + dx, y + dy, z + dz
            if skip is not None and skip(tx, ty, tz):
                continue
            if rng.random() < p and h.free(tx, ty, tz):
                r = rng.random()
                k = 0 if r < 0.34 else 1 if r < 0.62 else 2 if r < 0.84 else 3
                if grow(h, tx, ty, tz, facing, k):
                    n += 1
    return n


def glow_in_wall(h, cells, block=SEA):
    """Sea lanterns set into the lining at the given (x, y, z) cells (they must be full blocks of the lining already): the geode's light comes out of the stone."""
    n = 0
    for (x, y, z) in cells:
        c = h.get(x, y, z)
        if c is not None and (x, y, z) not in h.mt.reserved:
            h.set(x, y, z, block, over=True)
            n += 1
    return n


# ===================================================================================================================
# SET PIECE: the great geode (4 x 4 modules, three levels)
# ===================================================================================================================
CEN = 17.5
RX, RY = 16.4, 25.5


def cavity_s(x, y, z):
    """The normalised radius of the cell centre in the dome-shaped cavity of the great geode: <= 1 is air, > 1 is the lining and the shell round it."""
    r = math.hypot(x - CEN, z - CEN)
    u = (y - 8.5) / RY
    return math.hypot(r / RX, u)


def shell_depth(x, y, z):
    """Roughly how many cells the cell centre lies outside the cavity (0 at its surface): (s - 1) divided by the gradient of s, so the lining is as thick on the dome as on the walls."""
    r = math.hypot(x - CEN, z - CEN)
    u = (y - 8.5) / RY
    s = math.hypot(r / RX, u)
    if s <= 1.0:
        return 0.0
    g = math.hypot(r / (RX * RX), u / RY) / s
    return (s - 1.0) / max(g, 1e-6)


@T.room("set", "great_geode", faction="construct", view_h=26,
        cuts=[{"name": "west_half", "x": (0, 18), "y": (0, 34), "z": None}, {"name": "north_half", "x": None, "y": (0, 34), "z": (0, 18), "rot": 1}],
        views=[("door0", (13.5, 10.62, 3.5), (17.5, 14.0, 17.5)), ("door1", (31.5, 10.62, 13.5), (17.5, 14.0, 17.5)), ("deck", (17.5, 17.62, 8.0), (17.5, 20.0, 20.0)),
               ("crown", (17.5, 22.62, 14.5), (17.5, 22.0, 4.0)), ("heart", (6.5, 10.62, 17.5), (17.5, 20.0, 17.5))])
def great_geode(path):
    """Why: the set piece of the geode is the geode: a dome of 33 blocks across and 26 high whose lining is amethyst (with budding blocks, buds and clusters of every size), whose shell is
    calcite and basalt, and whose heart is a spire of amethyst and sea lanterns. Three levels: the floor, a balcony of basalt round the dome at the ninth row (reached by two curved
    stairs), and a crown platform round the Heart at the thirteenth, to which four bridges of amethyst stairs climb from the balcony. The balcony stands on eight columns, the bridges
    have a rail, and the way down from a bridge is the floor, so the danger of the room is the height and the glitter: the party fights on the floor round the Heart, on the balcony
    (the archers' place) and on the crown (the only place from which the Heart can be seen whole). The doors are a pinwheel, one on each side."""
    h = Hall(path, 4, 4, 26, wear=0.0)
    top = h.top
    rng = h.rng
    for (side, off) in (("n", 13), ("e", 13), ("s", 22), ("w", 22)):
        h.door(side, off)
    # 1. the shell: every interior cell outside the cavity is lining (amethyst, a tenth of it budding), calcite, or basalt and tuff; the doorways' aprons stay clear
    lining = set()
    for z in range(2, 34):
        for x in range(2, 34):
            for y in range(GROUND, top + 1):
                if cavity_s(x, y, z) <= 1.0 or h._in_volume((x, y, z)) is not None:
                    continue
                d = shell_depth(x, y, z)
                if d <= 1.15:
                    h.set(x, y, z, BUDDING if rng.random() < 0.10 else AMETHYST)
                    lining.add((x, y, z))
                elif d <= 2.4:
                    h.set(x, y, z, CALCITE)
                else:
                    h.set(x, y, z, BASALT if rng.random() < 0.65 else TUFF)
    # 2. the floor: basalt with a spectrum of rings and eight spokes in calcite and amethyst
    for z in range(2, 34):
        for x in range(2, 34):
            r = math.hypot(x - CEN, z - CEN)
            a = K.angle(x, z, CEN, CEN)
            c = h.get(x, FLOOR, z)
            if c is None:
                continue
            if abs(r - 4.5) < 0.6 or abs(r - 9.0) < 0.55 or abs(r - 13.5) < 0.55:
                h.floor_set(x, z, L.POLISHED)
            elif r < 15.5 and (min(a % 45.0, 45.0 - a % 45.0) * math.radians(1) * r) < 0.55 and r > 5.0:
                h.floor_set(x, z, L.BRICK)
    # 3. the Heart: a calcite foot, an amethyst body, a 2 x 2 spire of amethyst and tinted glass with bands of sea lantern, a crown of clusters
    for y in range(GROUND, GROUND + 2):
        for z in range(15, 21):
            for x in range(15, 21):
                h.set(x, y, z, L.BRICK if (x in (15, 20) and z in (15, 20)) else L.POLISHED)
    for y in range(GROUND + 2, GROUND + 6):
        for z in range(16, 20):
            for x in range(16, 20):
                h.set(x, y, z, AMETHYST)
    for y in range(GROUND + 6, GROUND + 22):
        band = (y - GROUND) % 5 == 0
        for z in (17, 18):
            for x in (17, 18):
                h.set(x, y, z, SEA if band else (TINTED if (y - GROUND) % 5 == 3 else AMETHYST))
    for (x, z) in ((17, 17), (18, 17), (17, 18), (18, 18)):
        h.try_set(x, GROUND + 22, z, GROWTH[3], {"facing": "up", "waterlogged": False})
    for (x, z, f) in ((16, 17, "west"), (16, 18, "west"), (19, 17, "east"), (19, 18, "east"), (17, 16, "north"), (18, 16, "north"), (17, 19, "south"), (18, 19, "south")):
        for y in (GROUND + 3, GROUND + 5):
            grow(h, x + (-1 if f == "west" else 1 if f == "east" else 0), y, z + (-1 if f == "north" else 1 if f == "south" else 0), f, 2 if y == GROUND + 3 else 1)
    # 4. the balcony: a deck of basalt and amethyst at row 16 between r 9.5 and 14.2, on eight columns; the stairs to it climb the lining in two curved ramps
    ramp_a = K.ramp_levels(CEN, CEN, 11.2, 14.4, 92.0, 150.0, 7.4)
    ramp_b = K.ramp_levels(CEN, CEN, 11.2, 14.4, 272.0, 330.0, 7.4)
    ramp_cells = set(ramp_a) | set(ramp_b)
    deck_row = 16
    for z in range(2, 34):
        for x in range(2, 34):
            r = math.hypot(x - CEN, z - CEN)
            if 9.4 <= r <= 14.2 and cavity_s(x, deck_row, z) <= 1.0 and (x, z) not in ramp_cells and h.free(x, deck_row, z) and h._in_volume((x, deck_row, z)) is None:
                if cavity_s(x, deck_row + 2, z) <= 1.0:
                    h.set(x, deck_row, z, AMETHYST if (int(r * 2) % 7 == 0) else BASALT)
    for k in range(8):
        a = 45.0 * k + 22.5
        px, pz = int(round(CEN + 11.8 * math.cos(math.radians(a)) - 0.5)), int(round(CEN + 11.8 * math.sin(math.radians(a)) - 0.5))
        if any((px + dx, pz + dz) in ramp_cells for dx in (0, 1) for dz in (0, 1)):
            continue
        cells = [(px + dx, y, pz + dz) for dx in (0, 1) for dz in (0, 1) for y in range(GROUND, deck_row)]
        if all(h.can_set(x, y, z) for (x, y, z) in cells):
            for (x, y, z) in cells:
                h.set(x, y, z, AMETHYST if y not in (GROUND, deck_row - 1) else L.BRICK)
    for ramp in (ramp_a, ramp_b):                                             # the curved stairs are an embankment: solid under every step, so they are one with the floor
        for (x, z), lv in sorted(ramp.items()):
            for y in range(GROUND, GROUND + lv - 1):
                h.try_set(x, y, z, BASALT if (x + y + z) % 5 else AMETHYST)
        K.build_ramp(h, ramp, stairs=L.POLISHED_STAIRS, base=GROUND, cx=CEN, cz=CEN)
    # 5. the crown: a disc of amethyst at row 21 round the Heart (r <= 3.6), four bridges of amethyst stairs from the balcony to it, rails
    crown_row = 21
    for z in range(10, 26):
        for x in range(10, 26):
            r = math.hypot(x - CEN, z - CEN)
            if r <= 4.0 and h.free(x, crown_row, z):
                h.set(x, crown_row, z, AMETHYST if r < 2.0 else BASALT)
    for (dx, dz) in ((0, -1), (0, 1), (-1, 0), (1, 0)):                      # the bridges: 5 steps of stairs, 6 wide, from r = 8.5 in to r = 4.5, a banister post on each outer step
        for k in range(5):
            for w in range(6):
                if dz:
                    x, z = 15 + w, (9 + k if dz < 0 else 26 - k)
                    facing = "south" if dz < 0 else "north"
                else:
                    x, z = (9 + k if dx < 0 else 26 - k), 15 + w
                    facing = "east" if dx < 0 else "west"
                h.try_set(x, 17 + k, z, L.POLISHED_STAIRS, {"facing": facing, "half": "bottom"})
                if w in (0, 5):
                    h.try_set(x, 18 + k, z, L.POLISHED_WALL)
    for z in range(10, 26):
        for x in range(10, 26):
            r = math.hypot(x - CEN, z - CEN)
            if 3.2 < r <= 4.0 and h.get(x, crown_row, z) is not None and h.free(x, crown_row + 1, z):
                a = K.angle(x, z, CEN, CEN)
                if min(abs(((a - ang + 180.0) % 360.0) - 180.0) for ang in (0.0, 90.0, 180.0, 270.0)) > 28.0:
                    h.set(x, crown_row + 1, z, L.POLISHED_WALL)
    # the balcony's inner rail (r 9.4 .. 10.4), open where a bridge starts and where a ramp arrives
    for z in range(2, 34):
        for x in range(2, 34):
            r = math.hypot(x - CEN, z - CEN)
            if 9.4 <= r < 10.3 and h.get(x, deck_row, z) is not None and h.free(x, deck_row + 1, z):
                a = K.angle(x, z, CEN, CEN)
                near_bridge = min(abs(((a - ang + 180.0) % 360.0) - 180.0) for ang in (0.0, 90.0, 180.0, 270.0)) <= 20.0
                near_ramp = any((x + dx, z + dz) in ramp_cells for dx in (-1, 0, 1) for dz in (-1, 0, 1))
                if not near_bridge and not near_ramp:
                    h.set(x, deck_row + 1, z, L.POLISHED_WALL)
    # 6. crystals out of the lining, sea lanterns set into it, spires on the floor, a ring of clusters on the balcony's rim
    for (x, y, z) in sorted(lining):
        if rng.random() < 0.035 and any(cavity_s(x + dx, y + dy, z + dz) <= 1.0 for dx, dy, dz in FACING_STEP.values()):
            h.set(x, y, z, SEA, over=True)
    for (x, z, hh) in ((7, 7, 7), (28, 7, 8), (7, 28, 8), (28, 28, 7), (24, 11, 5), (11, 24, 5)):
        spire(h, x, z, hh, base=2)
    lining_growth(h, p=0.22, cells=lining, skip=lambda x, y, z: h._in_volume((x, y, z)) is not None)
    # sea lanterns set into the foot of the lining all round (every 12 degrees) and a second ring higher up: the geode's light comes out of its own wall
    for row in (GROUND + 1, GROUND + 7):
        for k in range(30):
            a = math.radians(12.0 * k + (6.0 if row > GROUND + 3 else 0.0))
            for rr in [11.0 + 0.2 * i for i in range(0, 60)]:
                x, z = int(round(CEN - 0.5 + rr * math.cos(a))), int(round(CEN - 0.5 + rr * math.sin(a)))
                if 2 <= x <= 33 and 2 <= z <= 33 and cavity_s(x, row, z) > 1.0 and h.get(x, row, z) is not None and h._in_volume((x, row, z)) is None:
                    h.set(x, row, z, SEA, over=True)
                    break
    for side in "nsew":                                                     # where the cavity reaches the wall (the four compass points) the wall itself carries the lanterns
        wl = h.wall(side)
        doors = [off for (sd, off) in h.doors if sd == side]
        for row in (GROUND + 1, GROUND + 7):
            for lat in range(4, 32, 3):
                if any(abs(lat - off) <= 3 for off in doors):
                    continue
                if wl.free(lat, row, 0) and wl.free(lat, row, 1):
                    wl.shell_set(lat, row, -1, SEA)
    for door in (("n", 13), ("e", 13), ("s", 22), ("w", 22)):
        D.door_trim(h, *door)
    h.faction("construct", 8, 16)
    D.auto_light(h, max_lamps=20)
    D.auto_markers(h, spawns=9, patrols=3)
    return h


# ===================================================================================================================
# more props: chimes, geode slices, a lining for any cavity
# ===================================================================================================================
def chime(h, x, z, drop, y_top=None):
    """A crystal chime: a chain hung from the ceiling (or a beam), a block of amethyst at its foot and a cluster growing down from that block. `drop` is the chain's length. The block
    holds the cluster (a cluster hung on a chain would be dropped by the game). Returns False when a cell is taken or nothing holds the chain."""
    y_top = h.top if y_top is None else y_top
    above = h.get(x, y_top + 1, z)
    if above is None or not D.center_support(above.block, above.props, "down"):
        return False
    cells = [(x, y_top - k, z) for k in range(drop + 2)]
    if not all(h.can_set(*c) for c in cells):
        return False
    for k in range(drop):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - drop, z, AMETHYST)
    h.set(x, y_top - drop - 1, z, GROWTH[3], {"facing": "down", "waterlogged": False})
    return True


def geode_slice(w, lat, y, radius=3.4):
    """A cut geode in the wall: concentric rings seen from the room (basalt, calcite, amethyst) round a hollow with crystals growing from its back, set into the wall's inner layer
    (the outer layer is the back). Why: a cross-section of a geode is the one picture everyone knows of it, and a relief in the wall costs the room no floor."""
    h = w.hall
    n = 0
    R = int(radius) + 1
    for dl in range(-R, R + 1):
        for dy in range(-R, R + 1):
            d = math.hypot(dl, dy)
            if d > radius:
                continue
            pos = w.cell(lat + dl, y + dy, -1)
            c = h.get(*pos)
            if c is None or pos in h.deco or pos in h.mt.reserved:
                continue
            if d <= radius - 2.3:
                w.carve(lat + dl, y + dy, -1)
                n += 1
            elif d <= radius - 1.1:
                w.shell_set(lat + dl, y + dy, -1, AMETHYST if (dl + dy) % 5 else BUDDING)
            elif d <= radius - 0.2:
                w.shell_set(lat + dl, y + dy, -1, CALCITE)
            else:
                w.shell_set(lat + dl, y + dy, -1, BASALT)
    for dl in range(-R, R + 1):                                    # the crystals: clusters on the hollow's back wall and buds on the amethyst ring, pointing into the room
        for dy in range(-R, R + 1):
            d = math.hypot(dl, dy)
            if d <= radius - 2.3:
                x, yy, z = w.cell(lat + dl, y + dy, -1)
                if h.free(x, yy, z):
                    grow(h, x, yy, z, w.into, 3 if d < 0.8 else 2 if d < 1.4 else 1)
    return n


def lining_mass(h, cavity, rows=None, lining=1, calcite=3, keep=None):
    """Fills every interior cell that is not in `cavity` (a set of (x, y, z)) with the geode's shell by its distance (in cells, 6-neighbour steps) from the cavity: amethyst up to
    `lining` (a tenth of it budding), calcite up to `calcite`, then basalt and tuff. `keep(x, y, z)` leaves a cell clear. Returns the set of lining cells. Why: the great geode used
    an ellipsoid; the trial, the vault and the gallery's ends use shapes of their own, and a distance field makes any of them a geode."""
    from collections import deque
    rows = rows or range(GROUND, h.top + 1)
    cells = [(x, y, z) for z in range(h.z0, h.z1 + 1) for x in range(h.x0, h.x1 + 1) for y in rows]
    dist_ = {}
    dq = deque()
    for c in cells:
        if c in cavity:
            dist_[c] = 0
            dq.append(c)
    while dq:
        x, y, z = dq.popleft()
        d = dist_[(x, y, z)]
        if d >= calcite + 1:
            continue
        for dx, dy, dz in FACING_STEP.values():
            n = (x + dx, y + dy, z + dz)
            if n not in dist_ and n in set_of(cells):
                dist_[n] = d + 1
                dq.append(n)
    out = set()
    for c in cells:
        if c in cavity or (keep is not None and keep(*c)):
            continue
        d = dist_.get(c, calcite + 1)
        if d <= lining:
            h.set(*c, BUDDING if h.rng.random() < 0.10 else AMETHYST)
            out.add(c)
        elif d <= calcite:
            h.set(*c, CALCITE)
        else:
            h.set(*c, BASALT if h.rng.random() < 0.65 else TUFF)
    return out


_SET_CACHE = {}


def set_of(cells):
    """A set view of the list `cells`, built once per list object (the distance field asks for membership a million times)."""
    key = id(cells)
    cached = _SET_CACHE.get(key)
    if cached is None or cached[0] is not cells:
        cached = (cells, set(cells))
        _SET_CACHE[key] = cached
    return cached[1]


# ===================================================================================================================
# HALL: the echo gallery (2 x 5 modules)
# ===================================================================================================================
@T.room("hall", "echo_gallery", faction="slime", view_h=14,
        cuts=[{"name": "west_half", "x": (0, 9), "y": (8, 24), "z": None}],
        views=[("door0", (4.5, 10.62, 3.5), (8.5, 11.0, 30.0)), ("mid", (8.5, 10.62, 12.0), (8.5, 12.0, 38.0)), ("slice", (8.5, 10.62, 11.5), (2.0, 12.0, 9.0)),
               ("chimes", (8.5, 10.62, 30.0), (8.5, 14.0, 12.0))])
def echo_gallery(path):
    """Why: the geode has to have a place that is only sound and light: a gallery forty-one blocks long whose roof is a row of amethyst ribs on flush pilasters, with a chime hung
    between every pair of ribs (a block of amethyst on a chain with a cluster growing from its foot: what a step sets humming), a cut geode set into the wall in every bay (rings of
    basalt, calcite and amethyst round a hollow of crystals), spires of amethyst in the side aisles and sea lanterns laid in the floor of the nave, brighter towards the middle (the
    light gradient of the brief: the ends of the gallery are dim, the centre glitters). The nave is six wide and clear: it is the place where a party is strung out and a fight
    comes from both ends, and the spires in the aisles are cover. Doors north-east, south-west and one in the middle of each long wall."""
    h = Hall(path, 2, 5, 14, wear=0.0)
    top = h.top
    rng = h.rng
    h.door("n", 13)
    h.door("s", 4)
    h.door("w", 22)
    h.door("e", 22)
    h.route(6, 2, 11, 42, label="the nave")
    h.route(2, 21, 15, 23, label="the cross aisle")
    ww, we, wn, ws = h.wall("w"), h.wall("e"), h.wall("n"), h.wall("s")
    ribs = [8, 15, 29, 36]
    # 1. the floor: a calcite runner down the nave between amethyst borders, sea lanterns in the runner (denser in the middle: the light gradient)
    for z in range(2, 43):
        for x in range(2, 16):
            if x in (5, 12):
                h.floor_set(x, z, L.POLISHED)
            elif 6 <= x <= 11 and h.get(x, FLOOR, z) is not None:
                h.floor_set(x, z, L.BRICK)
    for z in range(4, 41, 3):
        gap = abs(z - 22)
        if gap > 18:
            continue
        for x in (7, 10):
            if (z // 3 + (x // 3)) % 2 == 0 or gap < 8:
                h.floor_set(x, z, SEA)
    # 2. the ribs: flush pilasters either side, a beam of amethyst across the roof at every rib, clusters growing down from the beams, a sea lantern in the crown of each rib
    for z in ribs:
        for wl in (ww, we):
            K.flush_pilaster(wl, z, GROUND, top - 1, shaft=L.POLISHED, cap=L.CHISELED, base=L.CHISELED, width=2)
    D.beams(h, "x", ribs + [z + 1 for z in ribs], y=top, block=L.POLISHED, corbel=L.POLISHED_STAIRS)
    for z in ribs:
        h.set(8, top, z, SEA, over=True)
        h.set(7, top, z + 1, SEA, over=True)
        for x in range(4, 14, 2):
            grow(h, x, top - 1, z, "down", rng.randrange(1, 4))
    # 3. the chimes between the ribs, one over each side of the nave, a longer one in the middle
    for (z, drop) in ((11, 4), (12, 3), (19, 4), (25, 4), (32, 4), (33, 3), (39, 4)):
        for x in (7, 10):
            chime(h, x, z, drop + (1 if abs(z - 22) < 6 else 0))
    # 4. a cut geode in every bay of both long walls, the pilasters between them
    for wl in (ww, we):
        for lat in (11, 18, 26, 33):
            geode_slice(wl, lat, GROUND + 5, 3.4)
    # 5. spires of amethyst in the side aisles (against the pilasters), clusters from the pilasters' feet
    for z in (11, 18, 26, 33):
        spire(h, 3, z - 1, 5, base=2, glow=True)
        spire(h, 13, z - 1, 5, base=2, glow=True)
    for door in (("n", 13), ("s", 4), ("w", 22), ("e", 22)):
        D.door_trim(h, *door)
    D.cornice(h)
    h.faction("slime", 8, 17)
    D.auto_light(h, max_lamps=16)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ===================================================================================================================
# HALL: the prism atrium (3 x 3 modules, eighteen high)
# ===================================================================================================================
SPECTRUM = ("red", "orange", "yellow", "lime", "cyan", "light_blue", "blue", "purple")


def prism_tower(h, cx, cz, y0=GROUND, y1=None):
    """A prism tower: a 3 x 3 post of stained glass in the colours of the spectrum from the foot (red) to the head (violet), a sea lantern up the middle of it, a block of amethyst for a
    foot and a chiselled cap. Light goes through coloured glass, so the tower is a column of coloured light. Returns False when a cell is taken."""
    y1 = h.top if y1 is None else y1
    cells = [(cx + dx, y, cz + dz) for dx in (-1, 0, 1) for dz in (-1, 0, 1) for y in range(y0, y1 + 1)]
    if not all(h.can_set(*c) for c in cells):
        return False
    height = y1 - y0 + 1
    for (x, y, z) in cells:
        k = y - y0
        band = min(len(SPECTRUM) - 1, int((k - 2) * len(SPECTRUM) / max(1, height - 3))) if k >= 2 else 0
        centre = (x, z) == (cx, cz)
        if k < 2:
            h.set(x, y, z, AMETHYST)
        elif y == y1:
            h.set(x, y, z, L.CHISELED)
        elif centre:
            h.set(x, y, z, SEA)
        else:
            h.set(x, y, z, "minecraft:%s_stained_glass" % SPECTRUM[band])
    return True


def prism(h, cx, cy, cz, r=4):
    """The hanging prism: an octahedron of radius `r` of stained glass, the colour by the row (red below, violet above), a sea lantern in its heart, on a chain from the ceiling.
    Returns False when a cell is taken."""
    cells = [(cx + dx, cy + dy, cz + dz) for dx in range(-r, r + 1) for dy in range(-r, r + 1) for dz in range(-r, r + 1) if abs(dx) + abs(dy) + abs(dz) <= r]
    if not all(h.can_set(*c) for c in cells):
        return False
    for (x, y, z) in cells:
        d = abs(x - cx) + abs(y - cy) + abs(z - cz)
        if d <= 1:
            h.set(x, y, z, SEA)
        else:
            band = min(len(SPECTRUM) - 1, int((y - (cy - r)) * len(SPECTRUM) / (2 * r + 1)))
            h.set(x, y, z, "minecraft:%s_stained_glass" % SPECTRUM[band])
    for y in range(cy + r + 1, h.top + 1):
        h.set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    return True


@T.room("hall", "prism_atrium", faction="elemental", view_h=18,
        cuts=[{"name": "west_half", "x": (0, 14), "y": (8, 30), "z": None}],
        views=[("door0", (13.5, 10.62, 3.5), (13.5, 14.0, 13.5)), ("door1", (23.5, 10.62, 13.5), (13.5, 14.0, 13.5)), ("floor", (4.5, 12.62, 4.5), (13.5, 9.0, 13.5)),
               ("tower", (13.5, 10.62, 22.5), (6.5, 16.0, 6.5))])
def prism_atrium(path):
    """Why: the geode's brightest hall is the one where its light is sorted into colour: an atrium eighteen blocks high with a prism of stained glass hung in the middle on a chain,
    four towers of coloured glass round it (each with a sea lantern up its heart: a column of coloured light), a disc of glass lit from above in the roof, slits of coloured glass in the
    walls, and a spectrum of terracotta spread across the floor like the light that would fall through the prism. It is the opposite of the echo gallery: wide, bright, open and
    vertical, with four doors on the axes. The towers are cover, the prism hangs four rows over the floor and can be walked under, and the floor is flat: the fight is a skirmish with
    the sprites (vexes) the geode's encounter table sends, in a room where nothing hides in the dark."""
    h = Hall(path, 3, 3, 18, wear=0.0)
    top = h.top
    rng = h.rng
    for side in ("n", "e", "s", "w"):
        h.door(side, 13)
    c = 13
    # 1. the floor: the spectrum in eight wedges (terracotta) to r = 11, an amethyst ring at 11.5 and 6.5, basalt outside
    for z in range(2, 25):
        for x in range(2, 25):
            r = math.hypot(x - c, z - c)
            a = K.angle(x, z, c, c)
            if h.get(x, FLOOR, z) is None:
                continue
            if r <= 11.0:
                wedge = int(((a + 22.5) % 360.0) / 45.0)
                if abs(r - 6.5) < 0.55:
                    h.floor_set(x, z, L.POLISHED)
                elif r < 1.6:
                    h.floor_set(x, z, SEA)
                else:
                    h.floor_set(x, z, "minecraft:%s_terracotta" % SPECTRUM[wedge])
            elif r <= 12.0:
                h.floor_set(x, z, L.POLISHED)
    # 2. the prism, the four towers
    prism(h, c, GROUND + 8, c, 4)
    for (x, z) in ((6, 6), (20, 6), (6, 20), (20, 20)):
        prism_tower(h, x, z)
    # 3. the sky: a disc of coloured glass in the ceiling lit from above (the glass is the inner ceiling layer, a sea lantern sits over each cell)
    for (x, z) in K.disc(c, c, 9.0):
        a = K.angle(x, z, c, c)
        wedge = int(((a + 22.5) % 360.0) / 45.0)
        h.shell_set(x, top + 1, z, "minecraft:%s_stained_glass" % SPECTRUM[wedge])
        h.shell_set(x, top + 2, z, SEA)
    # 4. slits of coloured glass lit from behind in every wall, between the doors
    for k, side in enumerate("nesw"):
        wl = h.wall(side)
        for j, lat in enumerate((6, 20)):
            D.window_slit(wl, lat, GROUND + 5, GROUND + 12, glow=SEA, glass="minecraft:%s_stained_glass" % SPECTRUM[(2 * k + j) % 8])
    # 5. crystal chimes round the rim of the room, wall lights, trim
    for (x, z) in ((3, 9), (3, 17), (23, 9), (23, 17), (9, 3), (17, 3), (9, 23), (17, 23)):
        h.try_set(x, top, z, AMETHYST)
        chime(h, x, z, 5, y_top=top - 1) if h.get(x, top, z) is not None else None
    for door in (("n", 13), ("e", 13), ("s", 13), ("w", 13)):
        D.door_trim(h, *door)
    D.cornice(h)
    K.wall_lights(h, [SEA], spacing=6, y=GROUND + 2, door_gap=3)
    h.faction("elemental", 8, 8)
    D.auto_light(h, max_lamps=16)
    D.auto_markers(h, spawns=8, patrols=3)
    return h


# ===================================================================================================================
# PUZZLE: the resonance hall (3 x 2 modules, four sigil plates, a riddle stone, a puzzle door and a closet of two chests)
# ===================================================================================================================
#: the order the Riddle Stone asks: not the order of either sigil hall of 1.54.0 (2 3 1 0 and 0 2 1 3), nor the fairy ring's (1 0 3 2)
RESONANCE_ORDER = (3, 1, 0, 2)
#: the colour of each plate (mega_lib's PLATE_COLORS: red, blue, gold, green) as the stained glass of its tuning crystal
PLATE_GLASS = ("minecraft:red_stained_glass", "minecraft:blue_stained_glass", "minecraft:yellow_stained_glass", "minecraft:lime_stained_glass")


@T.room("puzzle", "resonance_hall", faction="elemental", no_spawn=True, kind="puzzle", view_h=12,
        views=[("door0", (3.5, 10.62, 13.5), (13.0, 11.0, 8.0)), ("door1", (13.5, 10.62, 3.5), (10.0, 11.0, 12.0)), ("plates", (10.5, 10.62, 9.5), (7.5, 10.0, 4.0)),
               ("stone", (10.5, 10.62, 9.0), (10.5, 11.0, 15.0))])
def resonance_hall(path):
    """Why: the second puzzle of the geode is a chime. The Spire's puzzle (four sigil plates, a Riddle Stone that names the order, a puzzle door that opens only to it) is staged as a
    hall of tuned crystals: each plate has a pillar of amethyst behind it capped in the colour of the plate (red, blue, gold, green: a tuning fork), the south wall is a bank of note
    blocks (the bells the geode rings with when the right order is struck), eight chimes hang over the middle of the hall, and the floor round the plates is a set of ripples in calcite
    and amethyst. The stone that tells the order stands under the bells with a lantern over it. The door and the closet behind it are the shared mechanic (`gen_mega_rooms2.sigil_closet`,
    same blocks, same block entity); the order is 3 1 0 2. Nothing spawns in the hall (the puzzle is the danger): the party is alone with the sound."""
    h = Hall(path, 3, 2, 12, wear=0.0)
    mt = h.mt
    rng = h.rng
    h.door("w", 13)
    h.door("n", 13)
    R2.sigil_closet(mt, Frame(mt, 18, FR + 1, 8, "east"), rng)
    # 1. the floor: ripples round the middle of the hall (10, 8) in calcite with amethyst between, sea lanterns at the centre and on the ripples
    for z in range(2, 16):
        for x in range(2, 19):
            r = math.hypot(x - 10, z - 8)
            if abs(r - 3.0) < 0.55 or abs(r - 6.0) < 0.55 or abs(r - 9.0) < 0.55:
                mt.set(x, FR, z, L.BRICK if int(r) % 2 else L.POLISHED)
            elif rng.random() < 0.08:
                mt.set(x, FR, z, L.CRACKED_TILE)
    for (x, z) in ((10, 8), (5, 8), (15, 8), (10, 3), (10, 13)):
        mt.set(x, FR, z, SEA)
    # 2. the plates, each with its tuning crystal behind it, the plate's foot in polished stone
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for (x, z, back), c in zip(((7, 5, -1), (13, 5, -1), (7, 12, 1), (13, 12, 1)), colours):
        mt.set(x, FR + 1, z, R2.PLATE, {"color": str(c), "lit": "false"})
        for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            mt.set(x + dx, FR, z + dz, L.POLISHED)
        mt.set(x, FR, z, L.CHISELED)
        px, pz = x, z + 2 * back
        for y in range(FR + 1, FR + 6):
            mt.set(px, y, pz, AMETHYST)
        mt.set(px, FR + 6, pz, PLATE_GLASS[c])
        mt.set(px, FR + 7, pz, SEA)
        mt.set(px, FR + 8, pz, PLATE_GLASS[c])
        for (dx, dz, f) in ((1, 0, "east"), (-1, 0, "west"), (0, -back, "north" if back > 0 else "south")):
            D2.put_free(mt, px + dx, FR + 2, pz + dz, GROWTH[2], {"facing": f, "waterlogged": "false"})
    # 3. the riddle stone under the bells, a lantern over it
    mt.set(10, FR + 1, 15, R2.RIDDLE, None, {"id": R2.RIDDLE, "sequence": R2.nbt_list(RESONANCE_ORDER)})
    mt.set(10, FR + 2, 15, L.LANTERN, {"hanging": "false"})
    # 4. the bells: note blocks set into the south wall (three rows, a melody in their notes), a smaller bank either side of the north door
    notes = [0, 4, 7, 12, 16, 19, 24, 19, 16, 12, 7, 4, 0, 7]
    for row in range(3):
        for k, x in enumerate(range(4, 18)):
            mt.set(x, FR + 3 + row, 16, "minecraft:note_block", {"instrument": "chime", "note": str((notes[k] + 5 * row) % 25), "powered": "false"})
    for row in range(3):
        for x in (4, 5, 6, 7, 8):
            mt.set(x, FR + 3 + row, 1, "minecraft:note_block", {"instrument": "bell", "note": str((x * 3 + row * 5) % 25), "powered": "false"})
    # 5. chimes over the middle of the hall, spires in the corners the doors leave free, the walls' light
    for (x, z) in ((3, 3), (17, 3), (17, 14), (8, 14)):
        spire(h, x, z, 5, base=2)
    for (x, z) in ((6, 8), (10, 8), (14, 8), (8, 6), (12, 6), (8, 10), (12, 10)):
        chime(h, x, z, 4)
    K.mt_wall_lights(mt, [SEA], spacing=5, y=FR + 2, sides="ns")
    mt.set(19, FR + 3, 4, SEA)
    mt.set(19, FR + 3, 12, SEA)
    mt.marker(7, FR + 1, 8, "patrol")
    mt.marker(13, FR + 1, 8, "patrol")
    mt.marker(5, FR + 1, 4, "faction", faction="elemental")
    h.explicit_defaults()
    mt.finish()
    return mt


# ===================================================================================================================
# TRIAL: the shattering (3 x 3 modules, a Sealed Archway trial)
# ===================================================================================================================
@T.room("trial", "shatter_trial", faction="elemental", no_spawn=True, kind="trial", view_h=14,
        views=[("door0", (3.5, 10.62, 13.5), (13.0, 11.0, 13.0)), ("heart", (13.5, 10.62, 22.5), (13.0, 11.5, 13.0)), ("corner", (13.5, 10.62, 13.5), (3.5, 12.0, 3.5))])
def shatter_trial(path):
    """Why: the geode's trial is the one fight in the dungeon that is fair by construction (the keystone seals both doors when a party walks in, the glyphs spawn the wave scaled by
    the party, the barriers fall when the last one dies) staged in an octagonal chamber of the geode: the four corners are cut away and lined with amethyst (the geode's own lining,
    buds and clusters growing from it, sea lanterns set into it), the floor is a star of calcite and amethyst, the Resonator (a budding block with a crown of clusters on a platform)
    stands where the keystone hangs, four spires are cover, eight glyphs mark where the wave appears and the reward waits in a niche in the north wall. The wave is the geode's own:
    living armour (crystal golems), vexes (the prism sprites) and silverfish (shard crawlers). The doors are west and east."""
    h = Hall(path, 3, 3, 14, wear=0.0)
    rng = h.rng
    top = h.top
    h.door("w", 13)
    h.door("e", 13)
    c = 13
    # 1. the octagon: the corners are shell, the rest is the cavity
    cavity = set()
    for z in range(2, 25):
        for x in range(2, 25):
            if abs(x - c) + abs(z - c) <= 17:
                for y in range(GROUND, top + 1):
                    cavity.add((x, y, z))
    lining = lining_mass(h, cavity, lining=1, calcite=3)
    # 2. the floor: a star of calcite and amethyst, spokes on the axes and diagonals
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, GROUND, z) not in cavity or h.get(x, FLOOR, z) is None:
                continue
            dx, dz = x - c, z - c
            ch = max(abs(dx), abs(dz))
            oct_ = abs(dx) + abs(dz)
            if ch in (4, 8) or oct_ in (6, 12):
                h.floor_set(x, z, L.POLISHED if (x + z) % 2 else L.BRICK)
            elif abs(dx) == abs(dz) and ch > 3 and ch < 12:
                h.floor_set(x, z, L.CHISELED)
    # 3. the Resonator on its platform under the keystone
    D.dais(h, 10, 10, 16, 16, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    for (x, z) in ((12, 12), (13, 12), (14, 12), (12, 13), (14, 13), (12, 14), (13, 14), (14, 14)):
        h.set(x, GROUND + 1, z, AMETHYST)
    h.set(13, GROUND + 1, 13, BUDDING)
    h.set(13, GROUND + 2, 13, SEA)
    for (x, z, f) in ((12, 13, "west"), (14, 13, "east"), (13, 12, "north"), (13, 14, "south")):
        h.try_set(x, GROUND + 2, z, GROWTH[2], {"facing": "up", "waterlogged": False})
    for (x, z) in ((12, 12), (14, 12), (12, 14), (14, 14)):
        h.try_set(x, GROUND + 2, z, GROWTH[3], {"facing": "up", "waterlogged": False})
    # 4. cover: four spires, a lantern on a standing post by each
    for (x, z) in ((7, 7), (18, 7), (7, 18), (18, 18)):
        spire(h, x, z, 6, base=2)
    # 5. the reward: a niche in the north wall, the chest in it, candles; a statue niche in the south wall; clusters out of the lining
    wn, ws = h.wall("n"), h.wall("s")
    D.niche(wn, 13, GROUND, GROUND + 4, width=5, arch=True)
    h.loot_chest(13, GROUND, 1, "south")
    for x in (11, 15):
        D.candles_at(h, x, GROUND, 1, 4, "purple")
    h.set(13, GROUND + 4, 1, L.LANTERN, {"hanging": True})
    D.armor_alcove(ws, 13, kit="diamond", width=3, rows=4, lantern=True)
    lining_growth(h, p=0.25, cells=lining, skip=lambda x, y, z: h._in_volume((x, y, z)) is not None)
    for (x, y, z) in sorted(lining):
        if rng.random() < 0.05 and y <= GROUND + 9 and any((x + dx, y + dy, z + dz) in cavity for dx, dy, dz in FACING_STEP.values()):
            h.set(x, y, z, SEA, over=True)
    for door in (("w", 13), ("e", 13)):
        D.door_trim(h, *door)
    D.cornice(h)
    glyphs = []
    for k in range(8):
        a = math.radians(22.5 + 45.0 * k)
        glyphs.append((c + int(round(8.0 * math.cos(a))), c + int(round(8.0 * math.sin(a)))))
    h.faction("elemental", 20, 13)
    for (x, z) in ((6, 13), (13, 6), (13, 20)):
        h.marker(x, GROUND, z, "patrol")
    D.auto_light(h, max_lamps=10)
    h.explicit_defaults()
    R2.trial_core(h.mt, [("nusmp:living_armor", 2), ("minecraft:vex", 3), ("minecraft:silverfish", 4)], rng, [("w", 13), ("e", 13)], FR + 12, glyphs)
    return h.mt


# ===================================================================================================================
# TREASURE: the gem vault (1 x 2 modules, one door)
# ===================================================================================================================
@T.room("treasure", "gem_vault", faction="slime", view_h=9, few_spawns=True,
        views=[("door0", (4.5, 10.62, 3.0), (4.5, 10.5, 15.0)), ("chest", (4.5, 10.62, 8.0), (4.5, 11.0, 14.0))])
def gem_vault(path):
    """Why: the calm room and the reward of the geode: a pocket in the lining where the crystals grew thickest, a dead end behind one door. The corridor from the door opens into an oval
    chamber lined with amethyst (budding blocks, buds and clusters of every size) and calcite, a calcite plinth in its far end holds the chest among crystal clusters and purple candles,
    chimes hang from the roof, sea lanterns are set into the lining and the floor so that the vault glitters, and nothing waits in it: the gelatinous cubes of the geode keep the faction
    marker, one spawn marker and the room centre are all it has."""
    h = Hall(path, 1, 2, 9, wear=0.0)
    rng = h.rng
    top = h.top
    h.door("n", 4)
    h.route(3, 2, 5, 8, label="entrance run")
    cavity = set()
    for z in range(2, 16):
        for x in range(2, 7):
            inside_oval = ((x - 4) / 2.75) ** 2 + ((z - 9.5) / 6.9) ** 2 <= 1.0
            if inside_oval or (3 <= x <= 5 and z <= 8):
                for y in range(GROUND, top + 1):
                    cavity.add((x, y, z))
    h.marker(4, GROUND, 7, "spawn", weight=2)
    h.marker(4, GROUND, 9, "room_center")
    h.faction("slime", 5, 12)
    lining = lining_mass(h, cavity, lining=1, calcite=2)
    # the floor: a medallion in amethyst and calcite, sea lanterns in it
    for z in range(2, 16):
        for x in range(2, 7):
            if (x, GROUND, z) in cavity and h.get(x, FLOOR, z) is not None:
                r = math.hypot(x - 4, (z - 11) * 0.8)
                if r < 1.0:
                    h.floor_set(x, z, SEA)
                elif r < 2.3:
                    h.floor_set(x, z, L.POLISHED)
                elif r < 3.3:
                    h.floor_set(x, z, L.BRICK)
    # the plinth and the chest, the crystals round it
    for z in (13, 14):
        for x in (3, 4, 5):
            h.set(x, GROUND, z, CALCITE) if h.can_set(x, GROUND, z) else None
    h.loot_chest(4, GROUND + 1, 14, "north") if h.can_set(4, GROUND + 1, 14) else None
    for (x, z) in ((3, 14), (5, 14)):
        D.candles_at(h, x, GROUND + 1, z, 4, "purple") if h.can_set(x, GROUND + 1, z) else None
    h.try_set(4, GROUND + 1, 13, GROWTH[2], {"facing": "up", "waterlogged": False})
    lining_growth(h, p=0.30, cells=lining, skip=lambda x, y, z: h._in_volume((x, y, z)) is not None)
    for (x, y, z) in sorted(lining):
        if rng.random() < 0.09 and y <= GROUND + 6 and any((x + dx, y + dy, z + dz) in cavity for dx, dy, dz in FACING_STEP.values()):
            h.set(x, y, z, SEA, over=True)
    for (x, z) in ((4, 4), (4, 7), (4, 12)):
        chime(h, x, z, 3)
    D.door_trim(h, "n", 4)
    D.auto_light(h, max_lamps=6)
    return h


if __name__ == "__main__":
    sys.exit(K.run(T, sys.argv[1:]))

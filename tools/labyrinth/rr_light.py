#!/usr/bin/env python3
"""
Rooms rebuilt, part 5: lighting. The dungeon's light is a decision (labyrinth/mega/enc/LightRules: a bright party is met more often, the gloom clock counts block light
<= 4), so a rebuilt room keeps the light of the room it replaces: the same walking-route minimum, no brighter on average. The fixtures are vanilla light sources
(lanterns, torches, candles, froglights) hung from the new roof or set on the new pilasters, placed in patterns:

  chandelier   a chain and a ring of five lanterns under the crown of every bay (what the groin vault's crowns are for)
  hanging      one lantern on a chain under every bay's crown
  pilaster     a wall torch (or soul torch) on the face of every standing pilaster, and a lantern on its capital
  rib          lanterns hung from the rib crossings
  posts        lamp posts (a polished wall post with a lantern on top) at the bay corners against the walls

`balance` then measures the walking light with the same model as the shipped rooms' checks (mega_decor_r1.light_levels) and adds hanging lanterns over the darkest
reachable columns until the minimum is reached or nothing more can be hung.
"""
import mega_lib as L
import mega_decor_r1 as D1
from rr_core import FR, G, DIRS, DV, OPP

LANTERN, SOUL = L.LANTERN, L.SOUL_LANTERN
#: decoration a lamp's chain may replace (cobwebs and vines hang under every ceiling of the shipped rooms)
SOFT = {"minecraft:cobweb", "minecraft:vine", "minecraft:hanging_roots", "minecraft:glow_lichen", "minecraft:cave_vines", "minecraft:cave_vines_plant",
        "minecraft:weeping_vines", "minecraft:weeping_vines_plant"}


def run_free(room, x, z, y0, y1):
    """Is the vertical run y0..y1-1 at (x, z) free for a chain (air or soft decoration, nothing locked)?"""
    for y in range(y0, y1):
        c = room.cells.get((x, y, z))
        if (x, y, z) in room.locked or not room.in_box(x, y, z):
            return False
        if c is not None and c.block not in SOFT:
            return False
    return True


def support_y(room, x, z, boss=False):
    """The row of the lowest solid roof or rib cell directly above the free run under the roof at column (x, z), or None. Chains hang from a FULL block only: when the
    lowest cell is a stair or slab (a bevel or a rib corbel), `boss=True` replaces it with a polished block, a boss stone the lamp hangs from."""
    r = room.roofmap.get((x, z))
    if r is None:
        return None
    y = r
    while (x, y - 1, z) in room.cells and room.cells[(x, y - 1, z)].block not in SOFT:
        y -= 1
        if y < G + 8:
            return None
    cell = room.cells.get((x, y, z))
    if cell is None:
        return None
    if not L.is_full(cell.block):
        if not boss or (x, y, z) in room.locked:
            return None
        room.put_unlocked(x, y, z, L.POLISHED)
    return y


def hang(room, x, z, y_lamp, soul=False):
    """A lantern at row y_lamp hung on a chain from the roof at (x, z). True when it was written."""
    sy = support_y(room, x, z)
    if sy is None:
        sy = support_y(room, x, z, boss=True)
        if sy is None:
            return False
    y_lamp = min(y_lamp, sy - 2)
    if y_lamp < G + 4:
        return False
    if not run_free(room, x, z, y_lamp, sy):
        return False
    for y in range(y_lamp + 1, sy):
        room.put(x, y, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"}, over=True)
    room.put(x, y_lamp, z, SOUL if soul else LANTERN, {"hanging": "true", "waterlogged": "false"}, over=True)
    return True


def chandelier(room, x, z, y_lamp, soul=False, radius=1):
    cols = [(x, z), (x + radius, z), (x - radius, z), (x, z + radius), (x, z - radius)]
    sup = [support_y(room, a, b, boss=True) for a, b in cols]
    if any(s is None for s in sup):
        return False
    y_l = min(y_lamp, min(sup) - 3)
    if y_l < G + 4:
        return False
    for (a, b), s in zip(cols, sup):
        if not run_free(room, a, b, y_l, s):
            return False
    for (a, b), s in zip(cols, sup):
        for y in range(y_l + 1, s):
            room.put(a, y, b, "minecraft:chain", {"axis": "y", "waterlogged": "false"}, over=True)
        room.put(a, y_l, b, SOUL if soul else LANTERN, {"hanging": "true", "waterlogged": "false"}, over=True)
    return True


def bay_centres(room, bay=9):
    out = []
    for z in range(room.z0, room.z1 + 1):
        for x in range(room.x0, room.x1 + 1):
            if x % bay == 4 and z % bay == 4:
                out.append((x, z))
    return out


def scheme(room, kind, y_lamp=16, soul=False, rng=None):
    """Applies a lighting scheme; returns the number of fixtures placed."""
    rng = rng or room.rng
    n = 0
    if kind in ("chandelier", "hanging"):
        for (x, z) in bay_centres(room):
            ok = chandelier(room, x, z, y_lamp, soul) if kind == "chandelier" else hang(room, x, z, y_lamp, soul)
            n += 1 if ok else 0
    elif kind == "rib":
        for (x, z) in sorted(room.roofmap):
            if x % 9 in (0, 8) and z % 9 in (0, 8) and 0 < x < room.W - 1 and 0 < z < room.D - 1:
                if hang(room, x, z, y_lamp, soul):
                    n += 1
    elif kind == "none":
        pass
    return n


def pilaster_lights(room, soul=False, every=1):
    """Torches on the inward face of every standing pilaster (a pilaster whose shaft reaches the interior at depth 2) and lanterns on its capital."""
    n = 0
    count = 0
    for side in ("n", "s", "w", "e"):
        for lat in range(2, room.lat_len(side) - 2):
            x, y, z = room.wcell(side, lat, 2, G + 4)
            cell = room.cells.get((x, y, z))
            if cell is None or cell.block != L.POLISHED:
                continue
            tx, ty, tz = room.wcell(side, lat, 3, G + 4)
            if (tx, ty, tz) in room.cells or (tx, ty, tz) in room.locked:
                continue
            below = room.cells.get(room.wcell(side, lat, 2, G + 4))
            count += 1
            if count % every:
                continue
            f = room.into(side)
            blk = "minecraft:soul_wall_torch" if soul else "minecraft:wall_torch"
            if room.put(tx, ty, tz, blk, {"facing": f}):
                n += 1
    return n


def posts(room, soul=False, height=3):
    """Lamp posts against the walls on the module borders (a polished wall post and a lantern on its top), where the floor in front of the wall is free."""
    n = 0
    for side in ("n", "s", "w", "e"):
        for lat in range(9, room.lat_len(side) - 8, 9):
            cells = [room.wcell(side, lat, 2, G + h) for h in range(height + 1)]
            if all(room.free(*c) for c in cells):
                for h in range(height):
                    room.put(*cells[h], L.POLISHED_WALL, {"up": "true", "north": "none", "east": "none", "south": "none", "west": "none", "waterlogged": "false"})
                room.put(*cells[height], SOUL if soul else LANTERN, {"hanging": "false", "waterlogged": "false"})
                n += 1
    return n


def stats(room, min_eye=5):
    """The light of the walking routes of a room under construction (see stats_t)."""
    return stats_t(room.t, min_eye)


def stats_t(t, min_eye=5):
    """The light of the walking routes (the model of mega_decor_r1.check_hall) of a template: min, median, share below min_eye, share at 13 or more, and the cells."""
    nodes = D1.walk_nodes(t)
    seen = D1.reach(t, nodes)
    lv = D1.light_levels(t)
    holes = D1.door_holes(t)
    vals = []
    cols = []
    for (x, z, s) in seen:
        yf = s // 2
        ye = int(s / 2.0 + 1.62)
        if (x, yf, z) in holes:
            continue
        v = lv.get((x, ye, z), 0)
        vals.append(v)
        cols.append((v, x, z, yf))
    if not vals:
        return {"min": 0, "median": 0, "below": 1.0, "bright": 0.0, "cols": []}
    vs = sorted(vals)
    return {"min": vs[0], "median": vs[len(vs) // 2], "below": sum(1 for v in vals if v < min_eye) / float(len(vals)),
            "bright": sum(1 for v in vals if v >= 13) / float(len(vals)), "cols": sorted(cols)}


def balance(room, target=6, y_lamp=16, soul=False, limit=12):
    """Hangs lanterns over the darkest reachable columns until the minimum reaches `target` (or `limit` more lanterns are placed). Returns how many were added."""
    added = 0
    for _ in range(limit):
        st = stats(room, target)
        if st["min"] >= target or not st["cols"]:
            break
        placed = False
        for v, x, z, yf in st["cols"]:
            if v >= target:
                break
            for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1), (2, 0), (-2, 0), (0, 2), (0, -2)):
                if hang(room, x + dx, z + dz, y_lamp, soul):
                    placed = True
                    added += 1
                    break
            if placed:
                break
        if not placed:
            break
    return added

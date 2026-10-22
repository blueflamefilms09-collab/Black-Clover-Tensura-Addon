#!/usr/bin/env python3
"""
Rooms rebuilt, part 2: the roof. A roof is a height field: `hf(x, z)` says on which row the underside of the roof lies over each interior column, never below the
old roof (rows only go up, so everything that stood in the room still fits) and never above ROOF_MAX. The profiles below are the vault types of the rebuilt rooms:

  flat      the old height, with coffers cut into the roof (a recess in the lowest layer) and ribs along the module lines
  barrel    a tunnel vault along one axis, springing at the old roof height
  groin     a cross vault in every 9 x 9 bay (the module grid): the roof is the higher of the two barrels, ribs run along the groins and the bay borders
  dome      a spherical dome over the middle module, flat beyond it
  gable     an A-frame roof along one axis with timber tie beams
  hip       a pyramid roof (four slopes) over the whole room

Why a height field: the roof is closed against the void by construction (every column is filled up to the highest neighbour), the bevel that rounds each one-row step
is one rule, and the same code draws every vault, so a new profile is a ten-line function.
"""
import math

import mega_lib as L
from rr_core import *  # noqa: F401,F403
from rr_core import DIRS, DV, OPP, ROOF_MAX, FR, G, is_palette, STAIRISH


def barrel(room, rise, axis="z", spring=None):
    """A tunnel vault: the roof rises over the cross axis `axis`-perpendicular. axis='z' means the tunnel RUNS along z (the arch is over x)."""
    c = room.ceil if spring is None else spring
    if axis == "z":
        mid, half = (room.x0 + room.x1) / 2.0, (room.x1 - room.x0 + 1) / 2.0
        return lambda x, z: c + rise * math.sqrt(max(0.0, 1.0 - ((x - mid) / half) ** 2))
    mid, half = (room.z0 + room.z1) / 2.0, (room.z1 - room.z0 + 1) / 2.0
    return lambda x, z: c + rise * math.sqrt(max(0.0, 1.0 - ((z - mid) / half) ** 2))


def groin(room, rise, bay=9, spring=None):
    """A cross vault in every `bay` x `bay` module cell: the higher of the two barrels (the groins are where they meet)."""
    c = room.ceil if spring is None else spring
    half = bay / 2.0

    def f(x, z):
        u = (x % bay) + 0.5 - half
        v = (z % bay) + 0.5 - half
        a = math.sqrt(max(0.0, 1.0 - (u / half) ** 2))
        b = math.sqrt(max(0.0, 1.0 - (v / half) ** 2))
        return c + rise * max(a, b)
    return f


def dome(room, rise, radius=None, spring=None, flat=0.0):
    c = room.ceil if spring is None else spring
    cx, cz = (room.x0 + room.x1) / 2.0, (room.z0 + room.z1) / 2.0
    R = radius or min(room.x1 - room.x0 + 1, room.z1 - room.z0 + 1) / 2.0
    return lambda x, z: c + (rise * math.sqrt(max(0.0, 1.0 - ((x - cx) ** 2 + (z - cz) ** 2) / (R * R))) if (x - cx) ** 2 + (z - cz) ** 2 <= R * R else flat)


def gable(room, rise, axis="z", spring=None):
    """An A-frame: ridge along `axis`, slopes down to the long sides."""
    c = room.ceil if spring is None else spring
    if axis == "z":
        mid, half = (room.x0 + room.x1) / 2.0, (room.x1 - room.x0 + 1) / 2.0
        return lambda x, z: c + rise * (1.0 - min(1.0, abs(x - mid) / half))
    mid, half = (room.z0 + room.z1) / 2.0, (room.z1 - room.z0 + 1) / 2.0
    return lambda x, z: c + rise * (1.0 - min(1.0, abs(z - mid) / half))


def hip(room, rise, spring=None):
    c = room.ceil if spring is None else spring
    cx, cz = (room.x0 + room.x1) / 2.0, (room.z0 + room.z1) / 2.0
    hx, hz = (room.x1 - room.x0 + 1) / 2.0, (room.z1 - room.z0 + 1) / 2.0
    return lambda x, z: c + rise * (1.0 - max(abs(x - cx) / hx, abs(z - cz) / hz))


def flat(room, rise=0):
    c = room.ceil + rise
    return lambda x, z: c


def clamp_rise(room, want):
    """The largest rise (<= want) that keeps the roof inside the envelope: the underside of the roof may start at ROOF_MAX at the most."""
    return max(0, min(want, ROOF_MAX - room.ceil))


def build_roof(room, hf, surf=L.TILE, wall=L.BRICK, bevel=L.TILE_STAIRS, rib=None, rib_block=L.POLISHED, bevel_on=True, thickness=2):
    """Builds the roof for height field `hf`. The old roof must have been removed (`strip_roof`). The roof is `thickness` layers thick (at least 2), closed against
    every neighbour that is higher, the walls are carried up to it, and every one-row step is bevelled with an upside-down stair. `rib(x, z)` marks the interior
    columns that get a rib block hanging one row under the surface. Returns {(x, z): row}."""
    c = room.ceil
    r = {}
    for z in range(room.z0, room.z1 + 1):
        for x in range(room.x0, room.x1 + 1):
            row = int(round(hf(x, z)))
            r[(x, z)] = max(c, min(ROOF_MAX, row))

    def rn(x, z):
        return r[(min(max(x, room.x0), room.x1), min(max(z, room.z0), room.z1))]
    for z in range(room.D):
        for x in range(room.W):
            if room.inside(x, z):
                continue
            top = rn(x, z) + thickness - 1
            for y in range(c, top + 1):
                room.put_unlocked(x, y, z, wall)
    for (x, z), row in r.items():
        m = row + thickness - 1
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = r.get((x + dx, z + dz))
            if q is not None and q > m:
                m = q
        for y in range(row, m + 1):
            room.put_unlocked(x, y, z, surf)
    if bevel_on:
        for (x, z), row in r.items():
            if row <= c:
                continue
            for d in DIRS:
                dx, dz = DV[d]
                q = r.get((x + dx, z + dz))
                if q is not None and q == row - 1:
                    if room.stair(x, row - 1, z, bevel, d, "top"):
                        break
    if rib is not None:
        for (x, z), row in r.items():
            if rib(x, z):
                room.put(x, row - 1, z, rib_block, over=False)
    room.roofmap = r
    return r


def extend_up(room):
    """Carries what stood up to the old roof on up to the new one: chains (their lanterns and cages stay hung) and palette pillars (a column that stops under the
    old ceiling would hold nothing up). Returns the number of cells written."""
    c = room.ceil
    n = 0
    for (x, z), row in room.roofmap.items():
        top = room.cells.get((x, c - 1, z))
        if top is None or row <= c:
            continue
        blk = top.block
        if blk == "minecraft:chain" and top.props.get("axis", "y") == "y":
            for y in range(c, row):
                if room.put_unlocked(x, y, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"}):
                    n += 1
        elif is_palette(blk):
            below = room.cells.get((x, c - 2, z))
            if below is None or (is_palette(below.block) and below.block.endswith(STAIRISH)):
                continue
            fill = L.POLISHED if blk.endswith(STAIRISH) else blk
            start = c - 1 if blk.endswith(STAIRISH) else c
            for y in range(start, row):
                if room.put_unlocked(x, y, z, fill):
                    n += 1
    return n


def rib_lines(room, bay=9, width=1, axes="xz", diag=False):
    """Rib predicate: columns on the module borders (x or z a multiple of `bay`, `width` cells either side), and the bay diagonals when `diag`."""
    def f(x, z):
        mx, mz = x % bay, z % bay
        on_x = mx < width or mx >= bay - width
        on_z = mz < width or mz >= bay - width
        if "x" in axes and on_x and 0 < x < room.W - 1:
            return True
        if "z" in axes and on_z and 0 < z < room.D - 1:
            return True
        if diag:
            u, v = mx - (bay - 1) / 2.0, mz - (bay - 1) / 2.0
            return abs(abs(u) - abs(v)) < 0.6
        return False
    return f


def coffers(room, pitch=4, glow=None, glow_every=5):
    """Coffers in a flat part of the roof: every interior column whose cell inside a (pitch - 1) x (pitch - 1) grid lies away from the grid lines loses the lowest roof cell (the
    layer above stays, so the roof stays closed), leaving square recesses; every `glow_every`-th coffer gets a light block of `glow` (a froglight, shroomlight, glowstone or sea
    lantern) as its floor, a recessed ceiling light. Only columns whose roof is exactly as high as their four neighbours' are carved. Returns the number of recesses."""
    r = room.roofmap
    n = 0
    k = 0
    for z in range(room.z0, room.z1 + 1):
        for x in range(room.x0, room.x1 + 1):
            row = r[(x, z)]
            if x % pitch == 0 or z % pitch == 0:
                continue
            if any(r.get((x + dx, z + dz), row) != row for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                continue
            if (x, row, z) not in room.cells or (x, row - 1, z) in room.cells:
                continue                                      # something hangs here (a rib, a lamp): leave it
            if (x, row + 1, z) not in room.cells:
                continue
            if x % pitch == pitch // 2 and z % pitch == pitch // 2:
                k += 1
                if glow and k % glow_every == 0:
                    room.put_unlocked(x, row, z, glow)
                    n += 1
                    continue
            room.cells.pop((x, row, z))
            n += 1
    return n


def fix_hangers(room):
    """Re-hangs what the old ribs held: a chain (or a hanging lantern) whose top has air above it gets chain blocks up to the first solid block above, if the run is free.
    Why: the shipped rooms hang their lamps and cages from ribs under the roof; the rebuilt roof has other ribs, so every stack is checked and, where it hangs from
    nothing, carried up (never down, never into a cell that holds something)."""
    n = 0
    tops = []
    for (x, y, z), c in list(room.cells.items()):
        if c.block == "minecraft:chain" and c.props.get("axis", "y") == "y":
            above = room.cells.get((x, y + 1, z))
            if above is None or (above.block != "minecraft:chain"):
                if above is None:
                    tops.append((x, y, z))
        elif c.block in ("minecraft:lantern", "minecraft:soul_lantern") and c.props.get("hanging") == "true":
            if (x, y + 1, z) not in room.cells:
                tops.append((x, y, z))
    for (x, y, z) in tops:
        yy = y + 1
        run = []
        while yy < room.H - 2 and (x, yy, z) not in room.cells:
            run.append(yy)
            yy += 1
        sup = room.cells.get((x, yy, z))
        if sup is None or not L.is_full(sup.block) or not run or len(run) > 30:
            continue
        for ry in run:
            room.put_unlocked(x, ry, z, "minecraft:chain", {"axis": "y", "waterlogged": "false"})
            n += 1
    return n

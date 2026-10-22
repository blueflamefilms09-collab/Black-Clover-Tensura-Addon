#!/usr/bin/env python3
"""
Rooms rebuilt, part 3: floor patterns. Only the palette floor of the shipped room is repainted (deepslate tiles, cracked tiles, polished, chiselled, bricks): carpets,
water, lava, wool, special floors and every cell outside the interior are left alone. All patterns are written on the module grid (9 x 9) so a door's lane and a
bay's medallion line up whatever the room's size, and they are made of palette blocks only (the Region Theme restyles them with the rest of the room).

  border     a frame of polished stone one cell in from the walls (chiselled corners) and a second one three cells further in
  grid       polished lines on the module borders, a chiselled medallion (a disc and a ring) in the middle of every module
  checker    tiles and polished stone by (x + z), inside a border
  runner     a lane three wide from every door to the middle of the room: polished edges, a wool runner down the middle
  diamond    a diagonal lattice of polished lines with chiselled crossings
  rosette    concentric rings round the middle of the room
  inlay      the plain floor, with a large chiselled cross through the middle of the room and the module medallions
  ruin       broken tiles: cracked, cobbled and bricks in patches (no pattern)
"""
import math

import mega_lib as L
from rr_core import PAL_FLOOR, FR, G

T, TC, P, C, B, BC, CB = L.TILE, L.CRACKED_TILE, L.POLISHED, L.CHISELED, L.BRICK, L.CRACKED_BRICK, L.COBBLED


def _cells(room):
    for z in range(room.z0, room.z1 + 1):
        for x in range(room.x0, room.x1 + 1):
            c = room.cells.get((x, FR, z))
            if c is not None and c.block in PAL_FLOOR:
                yield x, z


def _paint(room, x, z, block):
    c = room.cells.get((x, FR, z))
    if c is not None and c.block in PAL_FLOOR:
        room.force(x, FR, z, block)


def pattern(room, kind, rng=None, runner="red_wool"):
    rng = rng or room.rng
    W, D = room.W, room.D
    cx, cz = (room.x0 + room.x1) / 2.0, (room.z0 + room.z1) / 2.0
    if kind == "plain":
        return
    if kind == "border":
        for x, z in _cells(room):
            r = min(x - room.x0, room.x1 - x, z - room.z0, room.z1 - z)
            corner = min(x - room.x0, room.x1 - x) == r and min(z - room.z0, room.z1 - z) == r and (x in (room.x0 + r, room.x1 - r)) and (z in (room.z0 + r, room.z1 - r))
            if r == 1:
                _paint(room, x, z, C if corner else P)
            elif r == 4:
                _paint(room, x, z, B if not corner else P)
            elif r == 0:
                _paint(room, x, z, BC if rng.random() < 0.15 else B)
    elif kind == "grid":
        for x, z in _cells(room):
            mx, mz = x % 9, z % 9
            line = mx in (0, 8) or mz in (0, 8)
            u, v = mx - 4, mz - 4
            d2 = u * u + v * v
            if d2 <= 1:
                _paint(room, x, z, C)
            elif d2 <= 5 and (u == 0 or v == 0 or abs(u) == abs(v)):
                _paint(room, x, z, P)
            elif 14 <= d2 <= 18:
                _paint(room, x, z, B)
            elif line:
                _paint(room, x, z, P)
    elif kind == "checker":
        for x, z in _cells(room):
            r = min(x - room.x0, room.x1 - x, z - room.z0, room.z1 - z)
            if r == 0:
                _paint(room, x, z, B)
            elif r == 1:
                _paint(room, x, z, P)
            elif (x + z) % 2 == 0:
                _paint(room, x, z, P)
    elif kind == "runner":
        lanes = set()
        for side, lat, (jx, jy, jz) in room.doors:
            if side in ("n", "s"):
                z0, z1 = (room.z0, int(cz)) if side == "n" else (int(cz), room.z1)
                for z in range(z0, z1 + 1):
                    lanes.add((lat, z, "x"))
            else:
                x0, x1 = (room.x0, int(cx)) if side == "w" else (int(cx), room.x1)
                for x in range(x0, x1 + 1):
                    lanes.add((x, lat, "z"))
        for x, z in _cells(room):
            r = min(x - room.x0, room.x1 - x, z - room.z0, room.z1 - z)
            if r == 0:
                _paint(room, x, z, B)
        for (x, z, ax) in lanes:
            for off in (-2, -1, 0, 1, 2):
                xx, zz = (x + off, z) if ax == "x" else (x, z + off)
                if not room.inside(xx, zz):
                    continue
                cell = room.cells.get((xx, FR, zz))
                if cell is None or cell.block not in PAL_FLOOR:
                    continue
                if abs(off) == 2:
                    _paint(room, xx, zz, P)
                elif off == 0:
                    room.force(xx, FR, zz, "minecraft:" + runner)
                else:
                    _paint(room, xx, zz, T)
    elif kind == "diamond":
        for x, z in _cells(room):
            a, b = (x + z) % 6, (x - z) % 6
            if a == 0 and b == 0:
                _paint(room, x, z, C)
            elif a == 0 or b == 0:
                _paint(room, x, z, P)
    elif kind == "rosette":
        R = min(room.x1 - room.x0, room.z1 - room.z0) / 2.0
        for x, z in _cells(room):
            d = math.hypot(x - cx, z - cz)
            if d > R:
                if min(x - room.x0, room.x1 - x, z - room.z0, room.z1 - z) == 0:
                    _paint(room, x, z, B)
                continue
            ring = int(d)
            if d < 1.5:
                _paint(room, x, z, C)
            elif ring % 4 == 0:
                _paint(room, x, z, P)
            elif ring % 4 == 2:
                _paint(room, x, z, B)
    elif kind == "inlay":
        for x, z in _cells(room):
            if abs(x - cx) < 1.0 or abs(z - cz) < 1.0:
                _paint(room, x, z, P)
            mx, mz = x % 9, z % 9
            if (mx - 4) ** 2 + (mz - 4) ** 2 <= 2:
                _paint(room, x, z, C)
    elif kind == "ruin":
        for x, z in _cells(room):
            n = rng.random()
            if n < 0.16:
                _paint(room, x, z, TC)
            elif n < 0.22:
                _paint(room, x, z, CB)
            elif n < 0.26:
                _paint(room, x, z, BC)
    else:
        raise ValueError("unknown floor pattern %r" % kind)


KINDS = ("plain", "border", "grid", "checker", "runner", "diamond", "rosette", "inlay", "ruin")

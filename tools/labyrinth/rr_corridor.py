#!/usr/bin/env python3
"""
Rooms rebuilt, part 8: hallways. Corridors (straight, 1 x 2 to 1 x 4 modules), chambers (bend, tee, cross) and the link pieces are drawn FROM SCRATCH on the module
grid, native to the 7 x 7 minimum clearance: the walking lane is the whole interior, x = 1..7, rows 9..15 (the box the Clearance7 cut would carve), and nothing stands in it.
So the architecture lives where the lane is not: in the wall faces (flush bands, posts, blind arches, embedded lights), in the floor, and above row 15 in the vault.

  * footprint, size, sockets (every jigsaw block with its own name, pool, joint and priorities) and data markers are copied cell for cell from the shipped piece, so
    the plan, the loop planner and the pruner see the same piece; walls are one layer thick (the lane takes the other), the end walls keep the 3 x 4 hole of the socket
    (a door cap fits it; a connected door is cut to 7 x 7 by the generator as before).
  * variants: 1 barrel vault with pilaster strips, 2 groin vault with blind arcades, 3 timber gable with tie beams, 4 coffered flat roof. The `_c` ruins add wear and a
    breach in the vault (an attic above it: the piece stays closed).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import rr_core as C  # noqa: E402
import rr_roof as R  # noqa: E402
import rr_floor as F  # noqa: E402
import rr_light as LT  # noqa: E402
import rr_dress as DR  # noqa: E402
from rr_core import FR, G  # noqa: E402
from template import Template, load, Cell  # noqa: E402

P, CH, B, T = L.POLISHED, L.CHISELED, L.BRICK, L.TILE
SPRING = 16            # first row above the 7 x 7 lane (rows 9..15)
LAMP = 16


def copy_functional(base, t):
    """Copies the sockets (jigsaw blocks with all their data) and the data markers of the shipped piece into the new template, cell for cell."""
    for (x, y, z), c in base.cells.items():
        if c.block in ("minecraft:jigsaw", "minecraft:structure_block"):
            t.cells[(x, y, z)] = c


def _cell(side, lat, y, W, D):
    return {"n": (lat, y, 0), "s": (lat, y, D - 1), "w": (0, y, lat), "e": (W - 1, y, lat)}[side]


def door_hole(t, side, lat, W, D):
    """The 3 x 4 hole of a socket in the outer wall layer, its frame (lateral +-2, rows 9..13) and the lintel; the jigsaw cell itself is kept."""
    for dl in range(-1, 2):
        for y in range(G, G + 4):
            c = _cell(side, lat + dl, y, W, D)
            cell = t.cells.get(c)
            if cell is None or cell.block != "minecraft:jigsaw":
                t.cells.pop(c, None)
    for dl in (-2, 2):
        for y in range(G, G + 5):
            t.set(*_cell(side, lat + dl, y, W, D), P)
    for dl in range(-2, 3):
        t.set(*_cell(side, lat + dl, G + 4, W, D), CH if dl == 0 else P)


def build(name, variant, rise_scale=1.0, restraint=0):
    base = load(C.template_path(name), name)
    W, H, D = base.size
    if W != 9:
        raise ValueError("%s: a corridor piece is one module wide" % name)
    stem = name.split("/")[-1]
    ruin = stem.endswith("_c")
    t = Template("scratch/" + stem, W, H, D)
    copy_functional(base, t)
    room = C.Room(name, variant, template=t, interior=(1, W - 2, 1, D - 2), ceil=SPRING, base=base)
    rng = room.rng
    chamber = D == 9
    # the lane is the interior, rows 9..15: nothing but markers may stand in it
    for z in range(room.z0, room.z1 + 1):
        for x in range(room.x0, room.x1 + 1):
            for y in range(G, SPRING):
                room.locked.add((x, y, z))
    for z in range(D):
        for x in range(W):
            room.force(x, FR, z, T)
    for y in range(G, SPRING):
        for z in range(D):
            for x in range(W):
                if (x in (0, W - 1) or z in (0, D - 1)) and (x, y, z) not in room.cells:
                    room.force(x, y, z, B)
    for side, lat, _ in room.doors:
        door_hole(t, side, lat, W, D)
    # the roof (a breach lifts a patch of it; the closure of the roof builder keeps the piece sealed)
    kind, want = {1: ("barrel", 3), 2: ("groin", 3), 3: ("gable", 4), 4: ("flat", 1)}[variant]
    rise = max(0, min(int(round(want * rise_scale)), C.ROOF_MAX - SPRING))
    if chamber:
        if variant in (1, 3):
            hf = R.dome(room, max(rise, 3), radius=4.6, spring=SPRING, flat=SPRING)
        elif variant == 2:
            hf = R.groin(room, max(rise, 3), bay=9, spring=SPRING)
        else:
            hf = R.flat(room, 3)
    elif kind == "barrel":
        hf = R.barrel(room, rise, "z", spring=SPRING)
    elif kind == "groin":
        hf = R.groin(room, rise, bay=9, spring=SPRING)
    elif kind == "gable":
        hf = R.gable(room, rise, "z", spring=SPRING)
    else:
        hf = R.flat(room, 2)
    patch = None
    if ruin and not chamber and D > 9:
        z0 = rng.randrange(3, D - 8)
        patch = (z0, z0 + 4)
        base_hf = hf
        hf = lambda x, z, _b=base_hf, _p=patch: _b(x, z) + (4 if (_p[0] <= z <= _p[1] and 2 <= x <= 6) else 0)
    if chamber:
        rib = R.rib_lines(room, diag=(variant == 2))
    elif variant in (1, 3):
        rib = lambda x, z: z % 4 == 0 and 0 < z < D - 1  # noqa: E731
    else:
        rib = R.rib_lines(room, diag=(variant == 2))
    R.build_roof(room, hf, rib=rib)
    F.pattern(room, ["runner", "inlay", "checker", "diamond"][variant - 1])
    walls(room, variant, rng)
    lights(room, variant, ruin, patch)
    DR.crack(room, wall=0.12 if ruin else 0.05, floor=0.14 if ruin else 0.05, roof=0.06)
    if ruin or variant == 3:
        DR.cobwebs(room, 4 if ruin else 2)
    DR.strip_stands(room)
    room.patch = patch
    return room


def walls(room, variant, rng):
    """Flat wall faces at x = 0 and x = 8 (and the end walls), rows 9..15: bands, posts, blind arches, embedded lights. Only palette blocks and flush lights."""
    W, D = room.W, room.D
    glow = rng.choice(["minecraft:shroomlight", "minecraft:ochre_froglight", "minecraft:sea_lantern", "minecraft:glowstone"])
    top = SPRING - 1
    if D == 9:
        # a chamber: every wall is a short face; glow blocks flush in the wall at lateral 1 and 7 of all four sides (eye height), nothing in the lane
        for side in ("n", "s", "w", "e"):
            for lat in (1, 7):
                x, y, z = room.wcell(side, lat, 0, G + 3)
                if not any(abs(lat - l) <= 2 for sd, l, _ in room.doors if sd == side):
                    room.force(x, y, z, glow)
    for side in ("w", "e"):
        x = 0 if side == "w" else W - 1
        face = "east" if side == "w" else "west"
        for z in range(1, D - 1):
            if any(abs(z - lat) <= 2 for s, lat, _ in room.doors if s == side):
                continue
            post = z % 4 == 0
            if variant == 1:                      # pilaster strips every four blocks, chiselled capital and base, dado rail
                if post:
                    for y in range(G, SPRING):
                        room.force(x, y, z, CH if y in (G, top) else P)
                room.force(x, G + 2, z, P)
                room.force(x, top, z, P)
            elif variant == 2:                    # blind arcade: posts and an arch of stairs between them
                if post:
                    for y in range(G, SPRING):
                        room.force(x, y, z, CH if y in (G, top) else P)
                elif z % 4 == 2:
                    room.force(x, G + 5, z, CH)
                    room.force(x, G + 4, z, P)
                    room.force(x, G + 3, z, L.CRACKED_BRICK if (z // 4) % 2 else B)
                else:
                    room.force(x, G + 4, z, P)
                    if z % 4 == 3:
                        room.force(x, G + 3, z, P)
                room.force(x, top, z, P)
            elif variant == 3:                    # timber: plank wainscot and log posts
                for y in range(G, G + 3):
                    room.force(x, y, z, L.PLANKS)
                if post:
                    for y in range(G, SPRING):
                        room.force(x, y, z, L.STRIPPED_LOG, {"axis": "y"})
                room.force(x, G + 3, z, L.STRIPPED_LOG, {"axis": "z"})
                room.force(x, top, z, L.STRIPPED_LOG, {"axis": "z"})
            else:                                 # panels: a polished frame round brick panels
                if z % 6 in (0, 5):
                    for y in range(G, SPRING):
                        room.force(x, y, z, P)
                for y in (G, top):
                    room.force(x, y, z, P)
                if z % 6 == 2:
                    room.force(x, G + 3, z, CH)
            if z % 8 == 2 and variant != 3 or z % 8 == 6 and variant == 3:
                room.force(x, G + 3, z, glow)
    for side in ("n", "s"):
        z = 0 if side == "n" else D - 1
        for x in range(1, W - 1):
            if abs(x - 4) <= 2:
                continue
            for y in (G, G + 2, top):
                room.force(x, y, z, P)


def lights(room, variant, ruin, patch):
    """Hanging lanterns above the lane (row 16 and up) and, in the chambers, a chandelier under the crown. The flush wall lights come with the walls."""
    D = room.D
    soul = False                  # a soul lantern hung above the lane gives light 4 at eye level: hanging lamps are warm
    if D == 9:
        LT.chandelier(room, 4, 4, LAMP, soul)
        return
    step = 6 if variant in (1, 3) else 8
    for z in range(4, D - 3, step):
        if patch and patch[0] - 1 <= z <= patch[1] + 1:
            continue
        LT.hang(room, 4, z, LAMP + (1 if variant == 2 else 0), soul)
    LT.balance(room, 6, LAMP, soul)

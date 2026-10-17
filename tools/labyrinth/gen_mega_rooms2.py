#!/usr/bin/env python3
"""
Why: ROOMS-2 of the Mega-Dungeon (1.54.0, docs/labyrinth/mega_rooms2.md) is the set of grand and mechanical rooms that make a sector memorable: the
Cathedral Rotunda, round and colonnade halls, flooded cisterns, lava halls, the mirror hall, three kinds of cavern, the pit and dark trap rooms, the sigil
puzzle halls and the sealed-archway trial halls. Every one is a Jigsaw template on the 9-block grid (docs/labyrinth/mega.md, "Connector contract"), drawn
from vanilla blocks only (the Spire's 1.36.0 trap blocks and the trial blocks excepted: they are the mechanics) and written from this one script, so the
doorways, the markers and the block states cannot drift apart between twenty-one templates, and so `--check` can prove each room before anybody walks in it.

    python3 tools/labyrinth/gen_mega_rooms2.py                 # (re)writes every template under structure/mega/{set,hall,cavern,trap,puzzle,trial}
    python3 tools/labyrinth/gen_mega_rooms2.py --only round_hall_a --only cistern_b
    python3 tools/labyrinth/gen_mega_rooms2.py --check         # builds in memory, runs the analysis, verifies the files on disk are identical

Deterministic: every template is seeded by its own name (`MT.rng`, `random.Random(name)`), so the same script writes the same bytes and `--check`
can compare. Architecture uses only the palette constants of `mega_lib` (the Region Theme restyles them); glass, copper, candles, lanterns, lava,
water, chains and bars are neutral accents the theme leaves alone.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r2 as D  # noqa: E402
from mega_lib import MT, Frame  # noqa: E402

FR = L.FLOOR_ROW
ROOMS = []   # (folder, name, builder, spec)


def room(folder, name, **spec):
    """Registers a template builder: `builder(name) -> MT`. `spec` carries what the analysis should expect of the room (see `check_room`)."""
    def deco(fn):
        """Records the builder in ROOMS and hands it back unchanged, so the function can still be called by name."""
        ROOMS.append((folder, name, fn, spec))
        return fn
    return deco


# =====================================================================================================================
# shared building blocks of the round rooms
# =====================================================================================================================

def solid_mass(mt, in_room, y0, y1, surface_depth=2, rng=None, wear=0.10, surface=L.BRICK, deep=(L.COBBLED, L.COBBLED, L.ROCK)):
    """Fills every box cell for which `in_room(x, z)` is false, rows y0..y1, with architecture: palette bricks on the surface that the room sees
    (cells within `surface_depth` of the room) and cobbled / rock deeper in. Cells in a doorway hole are skipped. Why solid: a round room in a square box
    must not leave the corners open, or an unused doorway of a neighbour would look into a hollow instead of a wall."""
    rng = rng or mt.rng
    near = {}
    for z in range(mt.D):
        for x in range(mt.W):
            if in_room(x, z):
                continue
            d = None
            for dz in range(-surface_depth, surface_depth + 1):
                for dx in range(-surface_depth, surface_depth + 1):
                    if abs(dx) + abs(dz) <= surface_depth and in_room(x + dx, z + dz):
                        d = True
                        break
                if d:
                    break
            near[(x, z)] = bool(d)
    for (x, z), is_surface in sorted(near.items()):
        for y in range(y0, y1 + 1):
            if (x, y, z) in mt.reserved:
                continue
            if is_surface:
                mt.set(x, y, z, L.CRACKED_BRICK if rng.random() < wear else surface)
            else:
                mt.set(x, y, z, rng.choice(deep))


def dome_row(r, R, base, height):
    """The first solid row of a dome whose rim (radius R, the wall top) is at row `base` and whose apex is `height` rows higher: an ellipsoid."""
    t = 1.0 - (r / R) ** 2
    return base + int(height * math.sqrt(max(0.0, t)) + 0.5)


# =====================================================================================================================
# ROUND HALL (hall/round_hall_a, round_hall_b): 3x3, a circular room with a ring of pillars and a central dais
# =====================================================================================================================

def wall_cell(cx, cz, R, ang, inside_fn=None):
    """The outermost interior cell of a round room along the ray at `ang` degrees from its centre cell (interior = distance <= R + 0.5), and the cardinal
    direction that points from the wall into the room there (the facing of a banner, sconce or statue standing in that cell)."""
    inside_fn = inside_fn or (lambda x, z: D.in_disc(x, z, cx, cz, R))
    r = R + 1.5
    x, z = D.polar(cx, cz, r, ang)
    while r > 3 and not inside_fn(x, z):
        r -= 0.2
        x, z = D.polar(cx, cz, r, ang)
    inward = (cx - x, cz - z)
    best = None
    for f, (dx, dz) in L.STEP.items():
        if inside_fn(x + dx, z + dz) and not inside_fn(x - dx, z - dz):
            sc = dx * inward[0] + dz * inward[1]
            if best is None or sc > best[0]:
                best = (sc, f)
    if best is None:
        best = (0, D.stair_facing(inward[0], inward[1]))
    return x, z, best[1]


def build_round_hall(name, variant):
    """Why: the round hall is the dungeon's 'meeting place': a circular room with a ring of pillars and a dais, so a sector has a room that is not a box.
    Both variants: radius 11 drum 12 rows high under a ribbed brick dome, twelve pillars at radius 8 joined by a ring beam with a lantern on every one, four
    doors at the compass points (between pillars, so each door looks straight at the dais), banners and sconces on the wall. (a) a stepped dais with a ring
    of eight armoured statues round a chiselled obelisk; (b) a sunken ring of fire: a brazier pit in the middle with a stair down to it on four sides."""
    mt = MT("mega/hall/" + name, 3, 3)
    rng = mt.rng
    cx = cz = 13
    R = 11
    top = FR + 12                       # last interior row of the drum
    base = top + 1                      # dome rim row
    dome_h = 8
    room_fn = lambda x, z: D.in_disc(x, z, cx, cz, R)  # noqa: E731
    for side in L.SIDES:
        mt.socket(side, 13)
    apex = dome_row(0, R + 0.5, base, dome_h)
    # the mass round the room: rows 7 (under the floor) to the dome apex + shell
    solid_mass(mt, room_fn, FR - 1, apex + 1, rng=rng)
    # floor: two thick under the room, patterned
    for z in range(mt.D):
        for x in range(mt.W):
            if room_fn(x, z):
                mt.set(x, FR - 1, z, L.ROCK)
                r = D.dist(x, z, cx, cz)
                blk = L.TILE
                if r > 7.5 and r <= 8.5:
                    blk = L.POLISHED
                elif r > 10.0:
                    blk = L.POLISHED if (x + z) % 2 == 0 else L.TILE
                elif rng.random() < 0.12:
                    blk = L.CRACKED_TILE
                mt.set(x, FR, z, blk)
    # axis strips (the doors look at the dais): polished with a chiselled stud every third cell
    for z in range(mt.D):
        for x in range(mt.W):
            if not room_fn(x, z):
                continue
            r = D.dist(x, z, cx, cz)
            if r < 5.5:
                continue
            if abs(x - cx) <= 1 or abs(z - cz) <= 1:
                lateral = min(abs(x - cx), abs(z - cz))
                blk = L.POLISHED
                if lateral == 0 and int(r) % 3 == 0:
                    blk = L.CHISELED
                mt.set(x, FR, z, blk)
    # the dome shell (2 thick) with ribs and rings
    for z in range(mt.D):
        for x in range(mt.W):
            if not room_fn(x, z):
                continue
            r = D.dist(x, z, cx, cz)
            ys = dome_row(r, R + 0.5, base, dome_h)
            ang = D.angle_of(x, z, cx, cz)
            rib = min(abs(((ang - 15.0 - 30.0 * k + 180.0) % 360.0) - 180.0) for k in range(12)) * math.radians(1.0) * r < 0.62
            for y in (ys, ys + 1):
                mt.set(x, y, z, L.POLISHED if (rib and y == ys) else (L.CRACKED_BRICK if rng.random() < 0.08 else L.BRICK))
    return mt, cx, cz, R, top, base, dome_h, room_fn


@room("hall", "round_hall_a", faction="construct")
def round_hall_a(name):
    """Why: the first round hall is the ring rooms' meeting place: twelve pillars on a ring joined by a ring beam with a lantern at each, a stepped dais in
    the middle with a chiselled obelisk and a ring of eight armoured statues (the party's cover and high ground), niches, window slits and banners in the
    wall, and a chandelier from the dome. The four doors on the compass points look straight at the dais."""
    mt, cx, cz, R, top, base, dome_h, room_fn = build_round_hall(name, "a")
    rng = mt.rng
    apex = dome_row(0, R + 0.5, base, dome_h)
    # pillars
    pill = D.around(cx, cz, 8, 12, 15.0)
    for (x, z) in pill:
        D.column(mt, x, z, FR + 1, top - 1)
    ring_beam(mt, cx, cz, 7.2, 8.8, top, pill)
    for (x, z) in pill:
        d_ = D.dist(x, z, cx, cz)
        ix, iz = D.polar(cx, cz, d_ - 1.4, D.angle_of(x, z, cx, cz))
        D.hang_lantern(mt, ix, top, iz, drop=2)
    # the dais
    for z in range(mt.D):
        for x in range(mt.W):
            r = D.dist(x, z, cx, cz)
            if r <= 4.5:
                mt.set(x, FR + 1, z, L.POLISHED)
            elif r <= 5.6:
                f = D.stair_facing(cx - x, cz - z)
                D.put(mt, x, FR + 1, z, L.POLISHED_STAIRS, {"facing": f, "half": "bottom"})
    mt.set(cx, FR + 1, cz, L.CHISELED)
    for k, (x, z) in enumerate(D.around(cx, cz, 3, 8, 0.0)):
        L.statue(mt, x, FR + 2, z, facing=["east", "south", "south", "west", "west", "north", "north", "east"][k])
    for y in range(FR + 2, FR + 8):
        mt.set(cx, y, cz, L.CHISELED if y in (FR + 2, FR + 7) else L.POLISHED)
    mt.set(cx, FR + 8, cz, L.SOUL_LANTERN, {"hanging": "false"})
    L.chandelier(mt, cx, apex, cz, drop=6, radius=2)
    dress_round_wall(mt, cx, cz, R, room_fn, top, rng, "a")
    markers_round(mt, cx, cz, "construct")
    mt.finish()
    return mt


def ring_beam(mt, cx, cz, r0, r1, y, pillars):
    """A cornice ring joining the capitals of a ring of pillars: a block row with a slab course over it."""
    for (x, z) in D.cells_between(cx, cz, r0, r1):
        if not D.inside(mt, x, y, z):
            continue
        D.put_free(mt, x, y, z, L.POLISHED)
    for (x, z) in D.cells_between(cx, cz, r0 + 0.4, r1 - 0.4):
        D.put_free(mt, x, y + 1, z, L.POLISHED_SLAB, {"type": "bottom"})


def dress_round_wall(mt, cx, cz, R, room_fn, top, rng, variant="a"):
    """The wall of a round hall: a niche behind the wall cell at the eight angles between the pillars that are not door axes (a statue and a lantern, or a
    shelf of candles, or a brazier), a window slit with a light behind it above each niche, a banner and a skull sconce between niches, and a cornice of
    upside-down stairs round the top of the drum."""
    gear = (("iron", "iron_sword"), ("diamond", "diamond_sword"), ("chainmail", "iron_axe"), ("netherite", "netherite_sword"))
    n_niche = 0
    for k in range(12):
        ang = 30.0 * k
        x, z, f = wall_cell(cx, cz, R, ang, room_fn)
        dx, dz = L.STEP[f]
        wx, wz = x - dx, z - dz
        if k % 3 == 0:
            continue                                   # a door axis
        # window slit above the niche (bars in the first wall cell, a glowing block behind them in the second: the wall is 3+ thick here)
        for y in range(top - 5, top - 1):
            D.put(mt, wx, y, wz, L.IRON_BARS)
            D.put(mt, wx - dx, y, wz - dz, "minecraft:shroomlight" if variant == "a" else "minecraft:sea_lantern")     # a glowing backing you can see, not an invisible light
        for y in range(FR + 1, FR + 6):
            mt.remove(wx, y, wz)
        kind = (n_niche + (0 if variant == "a" else 1)) % 3
        if kind == 0:
            g = gear[n_niche % len(gear)]
            L.statue(mt, wx, FR + 1, wz, facing=f, head="minecraft:%s_helmet" % g[0], chest="minecraft:%s_chestplate" % g[0],
                     legs="minecraft:%s_leggings" % g[0], feet="minecraft:%s_boots" % g[0], weapon="minecraft:" + g[1])
            D.hang_lantern_from_above(mt, wx, FR + 5, wz)
        elif kind == 1:
            mt.set(wx, FR + 1, wz, L.POLISHED)
            D.candle_cluster(mt, wx, FR + 2, wz, rng, count=4)
            D.hang_lantern_from_above(mt, wx, FR + 5, wz, lantern=L.SOUL_LANTERN if variant == "b" else L.LANTERN)
        else:
            mt.set(wx, FR + 1, wz, L.POLISHED)
            mt.set(wx, FR + 2, wz, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
        n_niche += 1
    for k in range(12):
        ang = 30.0 * k + 15.0
        x, z, f = wall_cell(cx, cz, R, ang, room_fn)
        if D.is_air(mt, x, FR + 7, z):
            D.banner_on(mt, x, FR + 7, z, f, "red" if variant == "a" else "black")
        if D.is_air(mt, x, FR + 4, z) and D.is_air(mt, x, FR + 5, z) and D.is_air(mt, x, FR + 3, z):
            L.skull_lamp(mt, x, FR + 3, z, facing=f, style="sconce", skull="wither_skeleton" if variant == "b" else "skeleton")
    for z in range(mt.D):
        for x in range(mt.W):
            d = D.dist(x, z, cx, cz)
            if R - 0.5 < d <= R + 0.5 and D.is_air(mt, x, top, z):
                D.put_free(mt, x, top, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(x - cx, z - cz), "half": "top"})


def markers_round(mt, cx, cz, faction):
    """Spawn markers on a ring between the pillars and the wall (eight), three patrol nodes on the door-to-door axes and the faction marker: the same layout
    for both round halls."""
    for k in range(8):
        x, z = D.polar(cx, cz, 9.6, 45.0 * k + 22.5)
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for (x, z) in ((cx, cz - 7), (cx, cz + 7), (cx + 7, cz)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(cx + 6, FR + 1, cz + 6, "faction", faction=faction)


# =====================================================================================================================
# PIT HALL (trap/pit_hall_a, pit_hall_b): 3x3, a field of nusmp:pitfall_tile with one thin safe route to a treasure island
# =====================================================================================================================

PITFALL = "nusmp:pitfall_tile"
LOOT_TRIAL = "nusmp:chests/labyrinth_trial"
PIT_CHAMBER_FLOOR = FR - 3        # row 5: the fall from the tiles (feet at row 9) onto it is exactly 3 blocks, the Spire's fall (docs/labyrinth/f1_traps.md)


def pit_hall_shell(mt, h=14):
    """The room and the solid slab under it (rows 4..7 over the whole box): the chamber and the ladder shaft are carved out of it afterwards, so every
    cavity under the floor is sealed by construction (a hole to unset cells would let a player walk off the world)."""
    mt.room(h, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    rng = mt.rng
    for z in range(mt.D):
        for x in range(mt.W):
            for y in range(PIT_CHAMBER_FLOOR - 1, FR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))


def pit_chamber(mt, field, safe, rng, ladder, foot_free=3):
    """Carves the chamber under `field` (cells rows 6..7, floor row 5), fills the field with pitfall tiles (safe on `safe`), lays pointed dripstone on a
    lattice of the chamber floor and builds the escape ladder: `ladder` = (x, z) of the shaft cell on the ledge; the shaft runs down to the chamber through
    the first field-edge cell beside it. Returns the list of spike cells."""
    for (x, z) in sorted(field):
        mt.remove(x, FR - 2, z)
        mt.remove(x, FR - 1, z)
        mt.set(x, FR, z, PITFALL, {"safe": "true" if (x, z) in safe else "false", "open": "false"})
    lx, lz = ladder
    # the shaft: from the ledge cell down to row 6, joined to the chamber through the cell between it and the field
    # find the neighbour of the shaft that touches the field (distance 1 from it): the connection cell sits between shaft and field
    conn = None
    for dx, dz in L.STEP.values():
        a = (lx + dx, lz + dz)
        b = (lx + 2 * dx, lz + 2 * dz)
        if a not in field and b in field:
            conn = (a, (dx, dz))
            break
    if conn is None:
        raise ValueError("%s: the ladder shaft at %s is not next to the pit field" % (mt.name, ladder))
    (cx_, cz_), (dx, dz) = conn
    mt.remove(lx, FR - 2, lz)
    mt.remove(lx, FR - 1, lz)
    mt.remove(cx_, FR - 2, cz_)
    mt.remove(cx_, FR - 1, cz_)
    face = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(dx, dz)]   # a ladder faces away from the block it hangs on: toward the field
    back = (lx - dx, lz - dz)
    for y in (FR - 2, FR - 1):
        mt.set(back[0], y, back[1], L.ROCK)                         # the block the ladder hangs on (under the ledge it must be there, whatever the shell left)
    for y in (FR - 2, FR - 1, FR):
        mt.set(lx, y, lz, "minecraft:ladder", {"facing": face, "waterlogged": "false"})
    mt.set(back[0], FR, back[1], L.TILE)
    spikes = []
    for (x, z) in sorted(field):
        if x % 2 == 0 and z % 2 == 0 and max(abs(x - lx), abs(z - lz)) > foot_free and rng.random() < 0.8:
            if D.put_free(mt, x, FR - 2, z, "minecraft:pointed_dripstone", {"vertical_direction": "up", "thickness": "tip", "waterlogged": "false"}):
                spikes.append((x, z))
    return spikes


@room("trap", "pit_hall_a", faction="goblin", kind="pit")
def pit_hall_a(name):
    """Why: the Spire's 1.36.0 pit room was a dead-end side room; the Mega-Dungeon needs the same resource drain in a room that can be walked THROUGH, so the
    hall keeps a 5-wide ledge round a 13x13 field of pitfall tiles and puts the lure (a treasure island with a chest) in the middle. Every tile of the field
    looks the same; one thin zig-zag of safe tiles leads to the island, and a lantern hangs from the ceiling over every turn of it (look up). A wrong step
    drops the party three blocks into a spiked chamber (the Spire's fall) with a ladder out, so the trap costs health and time but never traps anyone."""
    mt = MT("mega/trap/" + name, 3, 3)
    rng = mt.rng
    pit_hall_shell(mt)
    for side in ("n", "s", "e"):
        mt.socket(side, 13)
    field = {(x, z) for x in range(7, 20) for z in range(7, 20)}
    island = {(x, z) for x in range(12, 15) for z in range(12, 15)}
    goals = {(x, z) for (x, z) in field - island if any((x + dx, z + dz) in island for dx, dz in L.STEP.values())}
    route = D.snake_path(rng, (rng.randint(9, 17), 19), goals, field - island, min_len=16)
    if route is None:
        raise ValueError("%s: no safe route found" % name)
    safe = set(route) | island
    pit_chamber(mt, field, safe, rng, ladder=(5, 13))
    # ledge floor: checkered; the ladder hole is the one cell of it that is not floor
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in field or (x, z) == (5, 13):
                continue
            mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else (L.CRACKED_TILE if rng.random() < 0.1 else L.TILE))
    # the island: a chest, candles, a dead adventurer
    mt.t.chest(13, FR + 1, 13, LOOT_TRIAL, "south")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        D.candle_cluster(mt, 13 + dx, FR + 1, 13 + dz, rng, colors=("orange", "red"), count=3)
    mt.set(12, FR + 1, 13, "minecraft:bone_block")
    mt.set(14, FR + 1, 13, "minecraft:skeleton_skull", {"rotation": "4"})
    # pillars on the field's corners and edge middles, braziers on the ledge corners
    for (px, pz) in ((6, 6), (13, 6), (20, 6), (6, 13), (20, 13), (6, 20), (13, 20), (20, 20)):
        D.column(mt, px, pz, FR + 1, FR + 13)
        D.pillar_lantern(mt, px, pz, D.stair_facing(13 - px, 13 - pz), FR + 3)
    for (bx, bz) in ((3, 3), (23, 3), (3, 23), (23, 23)):
        D.brazier(mt, bx, FR + 1, bz)
    # the hint: a lantern over every turn of the route
    for i in range(1, len(route) - 1):
        a, b, c = route[i - 1], route[i], route[i + 1]
        if (b[0] - a[0], b[1] - a[1]) != (c[0] - b[0], c[1] - b[1]):
            D.hang_lantern(mt, b[0], FR + 15, b[1], drop=8)
    # stalactites over the field, banners and sconces on the walls
    for (x, z) in sorted(field - set(route)):
        if rng.random() < 0.05 and mt.get(x, FR + 14, z) is None:
            D.dripstone(mt, x, FR + 14, z, rng.choice((1, 2, 3)), "down")
    for side in ("n", "s", "e", "w"):
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4)
    # markers
    for k, (x, z) in enumerate(((3, 8), (3, 18), (23, 8), (23, 18), (8, 3), (18, 23), (8, 23), (18, 3))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    mt.marker(4, FR + 1, 13, "patrol")
    mt.marker(22, FR + 1, 13, "patrol")
    mt.marker(13, FR + 1, 4, "patrol")
    mt.marker(11, FR + 1, 22, "faction", faction="goblin")
    for (x, z) in [c for c in route[2::4]][:4]:
        mt.marker(x, FR + 1, z, "trap", type="pit")
    D.dress_walls(mt, rng, [(3, D.prop_barrels), (3, D.prop_bones), (2, D.prop_rubble)], 9, depth=2, spacing=4)
    D.corner_webs(mt, rng)
    mt.finish()
    return mt


# =====================================================================================================================
# COLONNADE HALL (hall/colonnade_hall_a, colonnade_hall_b): 2x5 (18 x 45), a basilica: nave, arcaded aisles, a raised end
# =====================================================================================================================

def colonnade_arcades(mt, rng, x_lines, z_pillars, wall_top, spring, jamb=6, span=3):
    """Two arcade walls (one per x line): brick from the floor to `wall_top`, a pointed stair arch between every pair of pillars, polished pillars with a
    chiselled foot and corbel brackets at the spring line. Why walls and not free columns: an arcade is what makes a hall read as nave and aisles, and
    the walls carry the banners, the clerestory and the vault."""
    z_min, z_max = z_pillars[0], z_pillars[-1]
    for x in x_lines:
        for z in range(z_min, z_max + 1):
            for y in range(FR + 1, wall_top + 1):
                mt.set(x, y, z, L.CRACKED_BRICK if rng.random() < 0.06 else L.BRICK)
        for z0, z1 in zip(z_pillars, z_pillars[1:]):
            zc = (z0 + z1) // 2
            f = Frame(mt, x, FR + 1, zc, "east")
            D.arch(mt, f, span, jamb, depth=1)
        for pz in z_pillars:
            for y in range(FR + 1, wall_top + 1):
                mt.set(x, y, pz, L.POLISHED)
            mt.set(x, FR + 1, pz, L.CHISELED)
            mt.set(x, FR + spring, pz, L.CHISELED)
            for dx, f_ in ((1, "west"), (-1, "east")):          # tall side against the pillar: a corbel under the spring line and a plinth step at the foot
                D.put_free(mt, x + dx, FR + spring, pz, L.POLISHED_STAIRS, {"facing": f_, "half": "top"})
                D.put_free(mt, x + dx, FR + 1, pz, L.POLISHED_STAIRS, {"facing": f_, "half": "bottom"})


def build_colonnade_hall(name, variant):
    """Why: the room shell of the first colonnade hall (2 x 5 modules, a nave 16 rows high, aisle roofs at row 21), kept apart from the dressing so the second
    hall can build its own vault. Returns (template, width, depth, nave rows, aisle roof row)."""
    mt = MT("mega/hall/" + name, 2, 5)
    W, Dd = mt.W, mt.D
    nave_h = 16                      # interior rows 9..24
    aisle_roof = FR + 13             # row 21: the aisle roofs
    mt.room(nave_h, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    return mt, W, Dd, nave_h, aisle_roof


def wide_chandelier(mt, x, y_ceiling, z, drop=4, axis="x", lantern=L.LANTERN):
    """A chandelier on two chains side by side (so it is centred on a 4 or 6 wide nave): two chains hanging from the ceiling cell row `y_ceiling` and a
    ring of lanterns round their foot."""
    xs = [(x, z), (x + 1, z)] if axis == "x" else [(x, z), (x, z + 1)]
    for (cx_, cz_) in xs:
        for k in range(1, drop + 1):
            D.put(mt, cx_, y_ceiling - k, cz_, L.CHAIN_BLOCK, {"axis": "y"})
    y = y_ceiling - drop - 1
    ring = []
    for (cx_, cz_) in xs:
        ring.append((cx_, cz_))
    for (cx_, cz_) in ring:
        D.put(mt, cx_, y, cz_, lantern, {"hanging": "true"})
    ends = [(x - 1, z), (x + 2, z), (x, z - 1), (x + 1, z - 1), (x, z + 1), (x + 1, z + 1)] if axis == "x" else \
           [(x, z - 1), (x, z + 2), (x - 1, z), (x - 1, z + 1), (x + 1, z), (x + 1, z + 1)]
    for (cx_, cz_) in ends:
        D.put(mt, cx_, y + 1, cz_, L.CHAIN_BLOCK, {"axis": "y"})
        D.put(mt, cx_, y, cz_, lantern, {"hanging": "true"})


def colonnade_hall_common(mt, rng, nave_h, aisle_roof, pillars):
    """What both colonnade halls share: aisle roofs with gables, nave beams, the arcades, clerestory slits glowing behind the arcade walls, banners on
    the pillars, pilasters and arched alcoves on the outer walls, the carpet. Returns the list of alcove centres (z)."""
    Dd = mt.D
    z0, z1 = pillars[0], pillars[-1]
    # aisle roofs (a lower ceiling over the aisles) and the gables that close the attic above them
    for x in list(range(2, 6)) + list(range(12, 16)):
        for z in range(z0, z1 + 1):
            mt.set(x, aisle_roof, z, L.TILE)
        for z in (z0, z1):
            for y in range(aisle_roof + 1, FR + nave_h + 1):
                mt.set(x, y, z, L.BRICK)
    colonnade_arcades(mt, rng, (6, 11), pillars, FR + nave_h, 7)
    # clerestory slits: bars in the arcade wall over the aisle roof, a light behind them in the attic
    for zc in [(a + b) // 2 for a, b in zip(pillars, pillars[1:])]:
        for (xw, xa) in ((6, 5), (11, 12)):
            for y in (aisle_roof + 1, aisle_roof + 2):
                mt.set(xw, y, zc, L.IRON_BARS)
            D.put_free(mt, xa, aisle_roof + 1, zc, "minecraft:light", {"level": "13", "waterlogged": "false"})
    # beams over the nave at every pillar pair
    for pz in pillars:
        for x in range(7, 11):
            D.put_free(mt, x, FR + nave_h, pz, L.POLISHED_STAIRS, {"facing": "east" if x < 9 else "west", "half": "top"})
    # banners on the pillars (nave side), ribs and lanterns over the aisles
    for i, pz in enumerate(pillars):
        col = "red" if i % 2 == 0 else "black"
        D.banner_on(mt, 7, FR + 11, pz, "east", col)
        D.banner_on(mt, 10, FR + 11, pz, "west", col)
        for x in list(range(2, 6)) + list(range(12, 16)):
            D.put_free(mt, x, aisle_roof - 1, pz, L.POLISHED_SLAB, {"type": "top"})
    for zc in [(a + b) // 2 for a, b in zip(pillars, pillars[1:])]:
        for x in (3, 14):
            D.hang_lantern(mt, x, aisle_roof, zc, drop=2)
    # the carpet down the nave
    for z in range(2, Dd - 2):
        for x in (8, 9):
            D.put_free(mt, x, FR + 1, z, "minecraft:red_carpet")


@room("hall", "colonnade_hall_a", faction="undead")
def colonnade_hall_a(name):
    """Why: a long hall is the dungeon's cathedral nave in miniature: the party enters at one end between two portals and sees the whole length at once, with
    the aisles as cover along the sides. Two arcades of pointed arches divide a 4-wide nave from two 4-wide aisles; the nave rises to row 24 under beams and
    three chandeliers, the aisles are lower (row 21) with ribs and hanging lanterns; a red carpet runs to a one-step dais with an altar, braziers, banners and a
    red stained-glass window at the north end. Six arched alcoves with statues, sarcophagi and candles line each outer wall: that is where the dead wait."""
    mt, W, Dd, nave_h, aisle_roof = build_colonnade_hall(name, "a")
    rng = mt.rng
    for off in (4, 13):
        mt.socket("n", off)
        mt.socket("s", off)
    mt.socket("w", 22)
    mt.socket("e", 22)
    pillars = list(range(6, 39, 4))
    colonnade_hall_common(mt, rng, nave_h, aisle_roof, pillars)
    # floors: aisles checkered, the nave dark with chiselled studs
    for z in range(2, Dd - 2):
        for x in range(2, W - 2):
            if x in (6, 11):
                continue
            if 7 <= x <= 10:
                mt.set(x, FR, z, L.CHISELED if (z in (14, 22, 30) and x in (7, 10)) else (L.CRACKED_TILE if rng.random() < 0.12 else L.TILE))
            else:
                mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
    # the raised end: a one-step dais, an altar, braziers and a window
    for z in range(2, 5):
        for x in range(6, 12):
            mt.set(x, FR + 1, z, L.POLISHED)
    for x in range(6, 12):
        mt.set(x, FR + 1, 5, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for x in (8, 9):
        mt.set(x, FR + 2, 3, L.CHISELED)
        D.candle_cluster(mt, x, FR + 3, 3, rng, colors=("white", "red"), count=4)
    for x in (6, 11):
        D.brazier(mt, x, FR + 2, 3, cage=False)
    D.put(mt, 8, FR + 2, 2, L.POLISHED_SLAB, {"type": "bottom"})
    D.put(mt, 9, FR + 2, 2, L.POLISHED_SLAB, {"type": "bottom"})
    D.lit_window(mt, D.inner_frame(mt, "n", 9), width=2, y0=6, height=8, glass="minecraft:red_stained_glass", backing="minecraft:shroomlight")
    for x in (7, 10):
        D.banner_on(mt, x, FR + 5, 2, "south", "red")
    D.lit_window(mt, D.inner_frame(mt, "s", 9), width=2, y0=6, height=8, glass="minecraft:cyan_stained_glass", backing="minecraft:sea_lantern")
    for x in (6, 11):
        D.brazier(mt, x, FR + 1, Dd - 4, cage=True)
    # chandeliers over the nave (two chains each, so they sit on the centre line)
    ceil_row = FR + nave_h + 1
    for z in (12, 22, 32):
        wide_chandelier(mt, 8, ceil_row, z, drop=5)
    # the outer walls: pilasters at the pillar rows, arched alcoves between them, slits above
    alcoves = [8, 12, 16, 28, 32, 36]
    gear = (("iron", "iron_sword"), ("diamond", "diamond_sword"), ("chainmail", "iron_axe"))
    for side, face in (("w", "east"), ("e", "west")):
        for pz in pillars:
            if pz == 22:
                continue
            f = D.inner_frame(mt, side, pz)
            x, y, z = f.at(0, 0, 0)
            D.pilaster(mt, x, z, FR + 1, aisle_roof - 1, face)
        for i, zc in enumerate(alcoves):
            f = D.inner_frame(mt, side, zc)
            D.arched_alcove(mt, f, 3, 4)
            kind = (i + (0 if side == "w" else 1)) % 3
            if kind == 0:
                D.alcove_statue(mt, f, gear[(i + (side == "e")) % 3])
            elif kind == 1:
                D.alcove_sarcophagus(mt, f)
            else:
                D.alcove_candles(mt, f, rng)
            D.lit_window(mt, f, width=1, y0=7, height=4, glass=L.IRON_BARS, backing="minecraft:shroomlight")
    spawn_cells = [(8, 12), (9, 17), (8, 27), (9, 32), (8, 37), (3, 14), (14, 30), (3, 38)]
    for k, (x, z) in enumerate(spawn_cells):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 3 == 0 else {}))
    for z in (10, 22, 34):
        mt.marker(8, FR + 1, z, "patrol")
    mt.marker(9, FR + 1, 40, "faction", faction="undead")
    D.dress_walls(mt, rng, [(3, D.prop_bones), (3, D.prop_rubble), (2, D.prop_candles)], 8, spacing=5)
    D.corner_webs(mt, rng)
    mt.finish()
    return mt


# =====================================================================================================================
# THE CATHEDRAL ROTUNDA (set/cathedral_rotunda): 5x5, the set piece of a sector
# =====================================================================================================================

ROT_C = 22                  # centre cell (x = z)
ROT_R = 19                  # interior: cells with distance <= 19.5 from the centre cell
ROT_G1, ROT_G2 = FR + 8, FR + 16   # the gallery floors (floor + 8, floor + 16)
ROT_RIM = 34                # first row of the glass dome
ROT_HD = 10                 # dome height: the apex glass is at row 44, backing 45, shell 46-47
ROT_PILLAR_R = 13           # the colonnade radius (cells at distance 12.5..13.5)
ROT_PILLARS = 16            # per tier; at 11.25 + 22.5k degrees so the four compass doors look through an arch of free cells


def rot_d(x, z):
    """Distance of a cell from the centre of the rotunda."""
    return math.hypot(x - ROT_C, z - ROT_C)


def rot_inside(x, z):
    """True when the cell is inside the round room (its centre is within the radius plus one half)."""
    return rot_d(x, z) <= ROT_R + 0.5


def rot_ring(x, z, a, b):
    """a < distance <= b."""
    d = rot_d(x, z)
    return a < d <= b


def rot_glass_row(x, z):
    """The row of the dome skin over a column: an ellipsoid whose rim is at ROT_RIM and whose apex is ROT_HD rows higher."""
    d = rot_d(x, z)
    t = min(1.0, d / (ROT_R + 0.5))
    return ROT_RIM + int(ROT_HD * math.sqrt(max(0.0, 1.0 - t * t)) + 0.5)


def rot_dome_columns():
    """For every interior column: (free_to, glass_lo, glass_hi, backing, shell_hi): the cells below glass_lo are the room, glass_lo..glass_hi the glass skin (every
    solid cell that touches the room is glass, so no riser of backing shows), glass_hi+1 the lit backing, then two shell rows."""
    out = {}
    for z in range(2 * ROT_C + 1):
        for x in range(2 * ROT_C + 1):
            if not rot_inside(x, z):
                continue
            g = rot_glass_row(x, z)
            hi = g
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if rot_inside(x + dx, z + dz):
                    hi = max(hi, rot_glass_row(x + dx, z + dz) - 1)
            out[(x, z)] = (g - 1, g, hi, hi + 1, hi + 3)
    return out


COPPER = "minecraft:waxed_oxidized_cut_copper"
COPPER_STAIRS = "minecraft:waxed_oxidized_cut_copper_stairs"
COPPER_SLAB = "minecraft:waxed_oxidized_cut_copper_slab"
CYAN_GLASS = "minecraft:cyan_stained_glass"
TINT_GLASS = "minecraft:tinted_glass"


def rot_rib_distance(x, z):
    """How far (in cells, along the arc) a column is from the nearest of the 16 meridian ribs (they stand over the pillars)."""
    d = rot_d(x, z)
    ang = D.angle_of(x, z, ROT_C, ROT_C)
    best = min(abs(((ang - 11.25 - 22.5 * k + 180.0) % 360.0) - 180.0) for k in range(ROT_PILLARS))
    return best * math.radians(1.0) * d


def rot_dome(mt, cols, rng):
    """Glass skin (cyan, every solid cell that touches the room), 16 copper ribs from the pillars to the apex with latitude rings, a backing of dark
    prismarine studded with sea lanterns (the dome glows teal from behind: the clip's light), two rows of shell above. The inner face of each rib is
    a copper slab hung from the glass, so the ribs stand out of the dome instead of being painted on it."""
    rings = (4.0, 8.0, 12.0, 15.5, 18.2)
    for (x, z), (free_to, glo, ghi, back, shell) in sorted(cols.items()):
        d = rot_d(x, z)
        rib = d > 1.8 and rot_rib_distance(x, z) < 0.62
        ring = any(abs(d - r) < 0.55 for r in rings)
        for y in range(glo, ghi + 1):
            if rib or ring:
                mt.set(x, y, z, COPPER)
            elif d < 1.6:
                mt.set(x, y, z, "minecraft:sea_lantern")
            else:
                mt.set(x, y, z, TINT_GLASS if (x + z + y) % 7 == 0 else CYAN_GLASS)
        if rib and glo - 1 > ROT_RIM - 1:
            D.put_free(mt, x, glo - 1, z, COPPER_SLAB, {"type": "top"})
        lantern = (x % 4 == 0 and z % 4 == 0) or (rib and (x + z) % 3 == 0)
        mt.set(x, back, z, "minecraft:sea_lantern" if lantern else "minecraft:dark_prismarine")
        for y in (back + 1, back + 2):
            mt.set(x, y, z, L.TILE)


def rot_door_cells(mt):
    """The third wall layer of each doorway (the rotunda's wall is three thick at the compass points): kept clear, framed by hand."""
    extra = []
    for side in L.SIDES:
        for lat in range(ROT_C - 1, ROT_C + 2):
            for y in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                extra.append(mt._lateral(side, ROT_C, lat, 2, y))
    return extra


def rot_pillar_cells():
    """The sixteen gallery pillars on their ring, in the same order for every part of the room that needs them."""
    return D.around(ROT_C, ROT_C, ROT_PILLAR_R, ROT_PILLARS, 11.25)


@room("set", "cathedral_rotunda", faction="cultist")
def cathedral_rotunda(name):
    """Why: the owner's reference clip is a round cathedral hall, and the Mega-Dungeon needs one unforgettable room per sector to break the rhythm of
    boxes: a round hall 39 across (a nave 26 wide inside its colonnade) under a teal glass dome on copper ribs, three tiers (the ground ambulatory, two
    colonnaded galleries at floor + 8 and + 16 joined by two curved stairs), a field of lit candles on a dark floor, and in the middle a crimson cross on a
    stepped plinth with fire at its foot. It is the one place where the whole sector can be seen at once, so it is also where a fight with room to move (and
    to flee up the galleries) happens."""
    mt = MT("mega/set/" + name, 5, 5)
    rng = mt.rng
    for side in L.SIDES:
        mt.socket(side, ROT_C)
    for c in rot_door_cells(mt):
        mt.reserved.add(c)
    cols = rot_dome_columns()
    # ---- the mass round the room (corners solid), skirted up the dome so the glass never meets the outside
    top_of = {}
    for z in range(mt.D):
        for x in range(mt.W):
            if rot_inside(x, z):
                continue
            t = ROT_RIM
            for dz in range(-3, 4):
                for dx in range(-3, 4):
                    c = cols.get((x + dx, z + dz))
                    if c is not None:
                        t = max(t, c[4])
            top_of[(x, z)] = t
    for (x, z), t in sorted(top_of.items()):
        near = any(rot_inside(x + dx, z + dz) for dx in range(-2, 3) for dz in range(-2, 3) if abs(dx) + abs(dz) <= 2)
        for y in range(FR - 1, t + 1):
            if (x, y, z) in mt.reserved:
                continue
            if near:
                mt.set(x, y, z, L.CRACKED_BRICK if rng.random() < 0.08 else L.BRICK)
            else:
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK)))
    # ---- the dome: glass skin on copper ribs, lit backing, shell
    rot_dome(mt, cols, rng)
    rot_ground_floor(mt, rng)
    rot_galleries(mt, rng)
    rot_door_frames(mt)
    rot_stairs(mt)
    rot_plinth_and_cross(mt, rng)
    rot_candles(mt, rng)
    rot_wall(mt, rng)
    rot_lights(mt, rng)
    rot_extras(mt, rng)
    rot_vestibule(mt)
    rot_markers(mt, rng)
    mt.finish()
    return mt


def rot_inward(side):
    """The direction that points from the wall `side` into the room."""
    return {"n": "south", "s": "north", "w": "east", "e": "west"}[side]


def rot_extras(mt, rng):
    """The finishing touches that make the hall a place: a pointed portico round each of the three plain doors, string courses round the wall at every
    tier, banners between the windows, braziers in the ambulatory and at the corners of the plinth, red runners from the doors to the plinth, an inlaid ring
    of red terracotta in the candle field (a colour accent: a Region Theme must not turn it grey), soul lanterns on chains under the arms of the cross."""
    c = ROT_C
    # red runners (a carpet on the polished axis strips) and the inlaid ring of the candle field
    for z in range(mt.D):
        for x in range(mt.W):
            d = rot_d(x, z)
            if 8.3 < d <= 19.4 and (abs(x - c) == 0 or abs(z - c) == 0) and not (z < 12 and abs(x - c) <= 9):    # none near the lava of the north entrance
                D.put_free(mt, x, FR + 1, z, "minecraft:red_carpet")
            if 10.0 < d <= 10.9 and mt.get(x, FR, z) is not None and mt.get(x, FR, z).block in (L.TILE, L.CRACKED_TILE) and abs(x - c) > 1 and abs(z - c) > 1:
                mt.set(x, FR, z, "minecraft:red_terracotta" if (x + z) % 3 else "minecraft:brown_terracotta")
    # a portico round each door except the north one (that one is the lava vestibule): pilasters either side and a pointed arch of span 5 over the 5-wide gap
    for side in ("e", "s", "w"):
        face = rot_inward(side)
        x, y, z = mt._lateral(side, ROT_C, ROT_C, 3, FR + 1)
        f = Frame(mt, x, y, z, face)
        for r in (-3, 3):
            for u in range(0, 8):
                D.put_free(mt, *f.at(r, u, 0), L.POLISHED)
            D.put_free(mt, *f.at(r, 0, 0), L.CHISELED)
        D.arch(mt, f, 5, 6, depth=1, carve=False)
        for u in (7, 8):
            for r in (-3, 3):
                D.put_free(mt, *f.at(r, u, 0), L.POLISHED)
    # string courses: a ring of upside-down stairs round the inner face of the outer wall under each gallery floor and at the drum top
    for row in (ROT_G1 - 1, ROT_G2 - 1, ROT_G2 + 9):
        for z in range(mt.D):
            for x in range(mt.W):
                if rot_ring(x, z, ROT_R - 0.5, ROT_R + 0.5):
                    f = D.stair_facing(x - c, z - c)                  # toward the wall: the cornice's full-height part is against it
                    D.put_free(mt, x, row, z, L.POLISHED_STAIRS, {"facing": f, "half": "top"})
    # banners on the tier walls beside every window (between window and pilaster)
    for k in range(16):
        for off in (-5.5, 5.5):
            ang = 22.5 * k + off
            x, z, f = rot_wall_cell(ang)
            for row in (ROT_G1 + 6, ROT_G2 + 6):
                D.banner_on(mt, x, row, z, f, "red" if k % 2 == 0 else "black")
    # braziers in the ambulatory and on the plinth
    for ang in (56.25, 78.75, 213.75, 236.25, 303.75, 326.25, 11.25 + 22.5 * 14, 33.75):
        if any(a0 - 4 <= ang <= a1 + 4 for (_n, a0, a1, foot, _s) in ROT_STAIRS if foot == FR):
            continue
        x, z, f = rot_wall_cell(ang)
        dx, dz = L.STEP[f]
        bx, bz = x + dx * 2, z + dz * 2
        if mt.get(bx, FR + 1, bz) is None and mt.get(bx, FR + 2, bz) is None:
            D.brazier(mt, bx, FR + 1, bz, cage=True)
    for k in range(4):
        x, z = D.polar(c, c, 6.0, 45.0 + 90.0 * k)
        D.brazier(mt, x, FR + 2, z, height=2, cage=True)
    # soul lanterns under the arms of the cross
    for (dx, dz) in ((8, 0), (-8, 0), (0, 8), (0, -8)):
        for k in (1, 2, 3):
            mt.set(c + dx, FR + 22 - k, c + dz, L.CHAIN_BLOCK, {"axis": "y"})
        mt.set(c + dx, FR + 22 - 4, c + dz, L.SOUL_LANTERN, {"hanging": "true"})


def rot_wall_cell(ang):
    """`wall_cell` of the rotunda: the outermost interior cell along `ang` and the way into the room."""
    return wall_cell(ROT_C, ROT_C, ROT_R, ang)


ROT_GEAR = (("iron", "iron_sword"), ("chainmail", "iron_sword"), ("diamond", "diamond_sword"), ("iron", "iron_axe"))


def rot_wall(mt, rng):
    """The outer wall: pilasters over every pillar, window slits that glow, and twelve niches with an armoured statue and a hanging lantern."""
    for k in range(ROT_PILLARS):
        ang = 11.25 + 22.5 * k
        x, z, f = rot_wall_cell(ang)
        for (y0, y1) in ((FR + 1, ROT_G1 - 1), (ROT_G1 + 1, ROT_G2 - 1), (ROT_G2 + 1, ROT_G2 + 8)):
            D.pilaster(mt, x, z, y0, y1, f)
    stand = 0
    for k in range(16):
        ang = 22.5 * k
        x, z, f = rot_wall_cell(ang)
        dx, dz = L.STEP[f]
        wx, wz = x - dx, z - dz
        for (y0, y1) in ((ROT_G1 + 2, ROT_G1 + 5), (ROT_G2 + 2, ROT_G2 + 5)):
            for y in range(y0, y1 + 1):
                if mt.get(x, y, z) is None:
                    mt.set(x, y, z, L.IRON_BARS)
            for y in range(y0, y1 + 1):
                if mt.get(wx, y, wz) is not None:
                    mt.set(wx, y, wz, L.IRON_BARS)
                    mt.set(wx - dx, y, wz - dz, "minecraft:shroomlight")
        if k % 4 == 0:
            continue
        # a niche in the wall behind the interior cell
        for y in range(FR + 1, FR + 6):
            mt.remove(wx, y, wz)
        gear = ROT_GEAR[stand % len(ROT_GEAR)]
        L.statue(mt, wx, FR + 1, wz, facing=f, head="minecraft:%s_helmet" % gear[0], chest="minecraft:%s_chestplate" % gear[0],
                 legs="minecraft:%s_leggings" % gear[0], feet="minecraft:%s_boots" % gear[0], weapon="minecraft:" + gear[1])
        stand += 1
        D.hang_lantern_from_above(mt, wx, FR + 5, wz)
    return stand


def rot_lights(mt, rng):
    """A lantern on a corbel on the nave side of every pillar of every tier, a hanging ring of lanterns over the nave, and the fire at the cross's foot."""
    for (y1) in (FR + 7, ROT_G1 + 7, ROT_G2 + 8):
        for (x, z) in rot_pillar_cells():
            f = D.stair_facing(ROT_C - x, ROT_C - z)
            dx, dz = L.STEP[f]
            ix, iz = x + dx, z + dz
            if mt.get(ix, y1 - 1, iz) is None and mt.get(ix, y1 - 2, iz) is None and mt.get(ix, y1 - 3, iz) is None:
                mt.set(ix, y1 - 1, iz, L.POLISHED_SLAB, {"type": "bottom"})
                mt.set(ix, y1 - 2, iz, L.CHAIN_BLOCK, {"axis": "y"})
                mt.set(ix, y1 - 3, iz, L.LANTERN, {"hanging": "true"})
    cols = rot_dome_columns()
    for k in range(4):
        x, z = D.polar(ROT_C, ROT_C, 9, 45.0 + 90.0 * k)
        glo = cols[(x, z)][1]
        L.chandelier(mt, x, glo - 1, z, drop=5, radius=2)
    c = ROT_C
    for k in range(8):
        x, z = D.polar(c, c, 3.0, 22.5 + 45.0 * k)
        mt.set(x, FR + 3, z, "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for dx, dz in ((2, 2), (-2, 2), (2, -2), (-2, -2)):
        mt.set(c + dx, FR + 3, c + dz, "minecraft:lava_cauldron")


def rot_vestibule(mt):
    """The north entrance: an iron-grate floor over a lava pit between two lava shafts behind amber glass (the clip's first frame)."""
    # the pit under the grate: x 20..24, z 3..7, lava rows 4..7, sealed by brick
    for z in range(2, 9):
        for x in range(19, 26):
            for y in range(3, FR):
                mt.set(x, y, z, L.BRICK)
    for z in range(3, 8):
        for x in range(20, 25):
            for y in range(4, FR):
                mt.set(x, y, z, "minecraft:lava", {"level": "0"})
            mt.set(x, FR, z, "minecraft:iron_trapdoor", {"facing": "north", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})
    # two shafts: lava x 14..18 / 26..30, z 3..7, rows 6..15 behind glass at x = 19 / 25
    for (x0, x1, gx) in ((14, 18, 19), (26, 30, 25)):
        for z in range(2, 9):
            for x in range(x0 - 1, x1 + 2):
                for y in range(5, ROT_G1):
                    inside = x0 <= x <= x1 and 3 <= z <= 7 and y >= 6
                    if inside:
                        mt.set(x, y, z, "minecraft:lava", {"level": "0"})
                    else:
                        mt.set(x, y, z, L.BRICK)
        for z in range(3, 8):
            for y in range(FR + 1, ROT_G1):
                mt.set(gx, y, z, "minecraft:orange_stained_glass")
        for y in range(FR + 1, ROT_G1):
            mt.set(gx, y, 8, L.POLISHED)
            mt.set(gx, y, 2, L.POLISHED)
        # the lava reaches only to row 15: the first gallery floor above it is solid already
    for z in range(3, 8):
        for x in (19, 25):
            mt.set(x, ROT_G1 - 1, z, "minecraft:orange_stained_glass")


def rot_markers(mt, rng):
    """Why: the Director spawns on SPAWN markers and a wandering boss walks PATROL nodes, so the rotunda gets ten spawn cells (eight on a ring in the candle
    field, two in the ambulatory), three patrol nodes (one on the ground axis, one on each gallery, on opposite sides), the faction marker and a room centre.
    The counts are the contract's maxima (ten spawns, three patrols), not more, even for the biggest room."""
    c = ROT_C
    spawns = []
    for k in range(8):
        x, z = D.polar(c, c, 10.4, 22.5 + 45.0 * k)
        spawns.append((x, z))
    for k, (x, z) in enumerate(spawns):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 3 == 0 else {}))
    for ang in (75.0, 225.0):
        x, z = D.polar(c, c, 14.6, ang)
        mt.marker(x, FR + 1, z, "spawn")
    # three patrol nodes (the contract's most): one on the ground axis and one on each gallery, on opposite sides, so a wandering boss walks all three tiers
    mt.marker(c, FR + 1, c + 10, "patrol")
    for (floor_row, ang) in ((ROT_G1, 40.0), (ROT_G2, 220.0)):
        x, z = D.polar(c, c, 14.6, ang)
        mt.marker(x, floor_row + 1, z, "patrol")
    mt.marker(c + 9, FR + 1, c + 3, "room_center")
    mt.marker(c - 9, FR + 1, c - 3, "faction", faction="cultist")


def rot_door_frames(mt):
    """The third layer of each doorway tunnel gets a polished frame and a lintel like the two that `MT.finish` writes."""
    for side in L.SIDES:
        for lat in (ROT_C - 2, ROT_C + 2):
            for y in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H + 1):
                x, yy, z = mt._lateral(side, ROT_C, lat, 2, y)
                mt.set(x, yy, z, L.POLISHED)
        for lat in range(ROT_C - 2, ROT_C + 3):
            x, yy, z = mt._lateral(side, ROT_C, lat, 2, L.DOOR_ROW + L.DOOR_H)
            mt.set(x, yy, z, L.CHISELED if lat == ROT_C else L.POLISHED)


# the two curved stairs: (first angle, last angle, floor row of the foot, number of steps); angles grow clockwise seen from above (0 = +x, 90 = +z)
ROT_STAIRS = (("A", 100.0, 154.0, FR, 8), ("B", 280.0, 334.0, ROT_G1, 8))
ROT_STAIR_LANE = (16.5, ROT_R + 0.5)       # distance range of the treads (3 wide); the strip inside the cheek stays level and free
ROT_CHEEK = (15.5, 16.5)                    # the inner stringer wall


def rot_stair_cells(a0, a1, steps):
    """Cells of one curved stair: {(x, z): (k, first_half, lane)}: k = step index, first_half = the cell takes the stair block (the second half is a full
    block, so two cells make one rise and every rise is two half steps: nobody has to jump), lane = 'tread' or 'cheek'."""
    out = {}
    span = (a1 - a0) / steps
    for z in range(2 * ROT_C + 1):
        for x in range(2 * ROT_C + 1):
            d = rot_d(x, z)
            lane = None
            if ROT_STAIR_LANE[0] < d <= ROT_STAIR_LANE[1]:
                lane = "tread"
            elif ROT_CHEEK[0] < d <= ROT_CHEEK[1]:
                lane = "cheek"
            if lane is None:
                continue
            ang = D.angle_of(x, z, ROT_C, ROT_C)
            if not (a0 <= ang < a1):
                continue
            k = int((ang - a0) / span)
            first = ((ang - a0) / span - k) < 0.5
            out[(x, z)] = (k, first, lane)
    return out


def rot_stairs(mt):
    """Why curved stairs of stair blocks: a stair block climbs half a block, so alternating stair and full block gives a gentle two-cell rise that every
    player and mob can walk without jumping, and following the ring keeps the stairs inside the ambulatory where nobody needs to cross the nave."""
    for (name, a0, a1, foot, steps) in ROT_STAIRS:
        cells = rot_stair_cells(a0, a1, steps)
        for (x, z), (k, first, lane) in sorted(cells.items()):
            row = foot + 1 + k
            ang = D.angle_of(x, z, ROT_C, ROT_C)
            tx, tz = -math.sin(math.radians(ang)), math.cos(math.radians(ang))
            # headroom: the gallery floor above the upper steps is opened where a tread would hit it
            for y in range(foot + 1, row + 4):
                c = mt.get(x, y, z)
                if c is not None and (y >= row or lane == "tread"):
                    mt.remove(x, y, z)
            for y in range(foot + 1, row):
                mt.set(x, y, z, L.BRICK)
            if lane == "cheek":
                mt.set(x, row, z, L.POLISHED)
                mt.set(x, row + 1, z, L.POLISHED_WALL, {"up": "true"})
            elif first:
                mt.set(x, row, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(tx, tz), "half": "bottom"})
            else:
                mt.set(x, row, z, L.POLISHED)


def rot_plinth_and_cross(mt, rng):
    """Why: the cross is the room's landmark, so it gets a two-step plinth (each step edged with stairs), a pedestal, the shaft of nether-wart blocks banded
    with shroomlight, the two arms (floor + 22 to 24, eight cells each way, a shroomlight tip) and a shroomlight capstone with a chain up to the dome. The
    fire at its foot is the four caged braziers that `rot_extras` stands on the plinth."""
    c = ROT_C
    for z in range(mt.D):
        for x in range(mt.W):
            d = rot_d(x, z)
            if d <= 7.5:
                if d > 6.5:
                    mt.set(x, FR + 1, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(c - x, c - z), "half": "bottom"})
                else:
                    mt.set(x, FR + 1, z, L.POLISHED if int(d) % 2 else L.TILE)
            if d <= 4.5:
                if d > 3.5:
                    mt.set(x, FR + 2, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(c - x, c - z), "half": "bottom"})
                else:
                    mt.set(x, FR + 2, z, L.POLISHED)
    # the pedestal and the cross
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for y in (FR + 3, FR + 4, FR + 5):
                mt.set(c + dx, y, c + dz, L.CHISELED if (dx and dz) else L.POLISHED)
    red, glow = "minecraft:nether_wart_block", "minecraft:shroomlight"
    for y in range(FR + 6, FR + 31):
        ring = (y % 4 == 2)
        for dx, dz in ((0, 0), (1, 0), (-1, 0), (0, 1), (0, -1)):
            mt.set(c + dx, y, c + dz, glow if (ring and (dx or dz)) else red)
    for y in (FR + 22, FR + 23, FR + 24):
        for k in range(-8, 9):
            if k:
                mt.set(c + k, y, c, glow if abs(k) == 8 else red)
                mt.set(c, y, c + k, glow if abs(k) == 8 else red)
    mt.set(c, FR + 31, c, glow)
    for y in range(FR + 32, ROT_RIM + ROT_HD):
        mt.set(c, y, c, L.CHAIN_BLOCK, {"axis": "y"})


def rot_candles(mt, rng):
    """The field of lit candles: clusters of 1-4 in several colours scattered on the nave floor, kept off the axis runners (the doors look straight
    at the plinth) and off the stair feet."""
    n = 0
    for z in range(mt.D):
        for x in range(mt.W):
            d = rot_d(x, z)
            if not (8.2 < d <= 12.0):
                continue
            if abs(x - ROT_C) <= 2 or abs(z - ROT_C) <= 2:
                continue
            if rng.random() < 0.20 and D.candle_cluster(mt, x, FR + 1, z, rng):
                n += 1
    return n


def rot_ground_floor(mt, rng):
    """The ground floor: dark floor tile (a tenth of it cracked) over a rock slab, a chequer of polished tiles beyond radius 12 (the ambulatory), and plain
    floor tile under the walls outside the circle so no cell of the floor row is left unset."""
    for z in range(mt.D):
        for x in range(mt.W):
            if not rot_inside(x, z):
                if (x, FR, z) not in mt.reserved:
                    mt.set(x, FR, z, L.TILE)
                continue
            mt.set(x, FR - 1, z, L.ROCK)
            d = rot_d(x, z)
            blk = L.TILE
            if d > 12.0:
                blk = L.POLISHED if (x + z) % 2 == 0 else L.TILE
            elif rng.random() < 0.10:
                blk = L.CRACKED_TILE
            mt.set(x, FR, z, blk)


def rot_galleries(mt, rng):
    """The two gallery floors (rings), the three tiers of pillars and the balustrades."""
    pillars = rot_pillar_cells()
    for (floor_row, lo, hi) in ((ROT_G1, 11.5, ROT_R + 0.5), (ROT_G2, 11.5, ROT_R + 0.5)):
        for z in range(mt.D):
            for x in range(mt.W):
                d = rot_d(x, z)
                if lo < d <= hi:
                    mt.set(x, floor_row, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
    for (y0, y1) in ((FR + 1, ROT_G1 - 1), (ROT_G1 + 1, ROT_G2 - 1), (ROT_G2 + 1, ROT_G2 + 8)):
        for (x, z) in pillars:
            D.column(mt, x, z, y0, y1)
    # the top ring beam over the upper colonnade
    for z in range(mt.D):
        for x in range(mt.W):
            if rot_ring(x, z, 11.5, 14.5):
                D.put_free(mt, x, ROT_G2 + 9, z, L.POLISHED)
    for (floor_row) in (ROT_G1, ROT_G2):
        for z in range(mt.D):
            for x in range(mt.W):
                if rot_ring(x, z, 12.5, 13.5) and mt.get(x, floor_row + 1, z) is None:
                    mt.set(x, floor_row + 1, z, L.POLISHED_WALL, {"up": "true"})


# =====================================================================================================================
# DARK ROOM (trap/dark_room): 2x2, the Spire's snuffer glyphs in a crypt with no light of its own and a chest behind a cobweb curtain
# =====================================================================================================================

SNUFFER = "nusmp:snuffer_glyph"


@room("trap", "dark_room", faction="undead", dark=True, few_spawns=True, kind="dark")
def dark_room(name):
    """Why: the second resource drain of the 1.36.0 trap set (docs/labyrinth/f1_traps.md) was a dark room: snuffer glyphs in the floor put out every torch,
    lantern and candle near them, forbid placing new ones and darken the screen, so the party's own light, the dungeon's real resource, is the price. In the
    Mega-Dungeon it is a crypt: pillars, sarcophagi, sculk and skulls, not one light source in the whole room (the generator proves it), two glyphs that
    cover it, and a chest in an arched alcove behind a cobweb curtain that the darkness hides. The doors are on opposite corners, so crossing the room
    means crossing the glyphs' reach."""
    mt = MT("mega/trap/" + name, 2, 2)
    rng = mt.rng
    mt.room(13, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("n", 4)
    mt.socket("s", 13)
    glyphs = ((5, 5), (12, 12))
    for (gx, gz) in glyphs:
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                mt.set(gx + dx, FR, gz + dz, "minecraft:polished_blackstone")
        mt.set(gx, FR, gz, SNUFFER, {"active": "false"})
    for (px, pz) in ((7, 5), (10, 12), (5, 10), (12, 5)):
        D.column(mt, px, pz, FR + 1, FR + 11)
    # the hidden chest (first, so that nothing is hung on the wall it is cut into): an arched alcove in the east wall behind a cobweb curtain (z 8..10), within the second glyph's six blocks
    f = D.inner_frame(mt, "e", 9)
    D.arched_alcove(mt, f, 3, 4)
    mt.t.chest(*f.at(0, 0, -1), LOOT_TRIAL, "west")
    for dz in (-1, 0, 1):
        for y in (FR + 1, FR + 2, FR + 3):
            D.cobweb(mt, f.at(dz, 0, 0)[0], y, f.at(dz, 0, 0)[2])
    # sculk on the floor in patches, veins on the walls, cobwebs in the corners, bones and skulls
    for z in range(2, 16):
        for x in range(2, 16):
            if mt.get(x, FR, z).block == SNUFFER or mt.get(x, FR, z).block == "minecraft:polished_blackstone":
                continue
            if rng.random() < 0.10:
                mt.set(x, FR, z, "minecraft:sculk")
    for k in range(40):
        side = rng.choice(L.SIDES)
        f = D.inner_frame(mt, side, rng.randint(3, 14))
        if mt.get(*f.at(0, 0, 0)) is None and mt.get(*f.at(0, 0, -1)) is not None and f.at(0, 0, -1) not in mt.reserved:
            wall = f.dir("back")
            D.put_free(mt, *f.at(0, 0, 0), "minecraft:sculk_vein", {wall: "true", "down": "true", "waterlogged": "false"})
    for (x, z) in ((2, 2), (15, 2), (2, 15), (15, 15)):
        for y in (FR + 10, FR + 11, FR + 12):
            D.cobweb(mt, x, y, z)
    # sarcophagi along the west wall
    for zc in (7, 12):
        f = D.inner_frame(mt, "w", zc)
        for r in (-1, 0, 1):
            mt.set(*f.at(r, 0, 0), L.POLISHED)
            if r == 1:
                mt.set(*f.at(r, 1, 0), L.POLISHED)                  # the foot end is a whole block: a candle cannot stand on a slab
            else:
                mt.set(*f.at(r, 1, 0), L.POLISHED_SLAB, {"type": "bottom"})
        mt.set(*f.at(-1, 2, 0), "minecraft:wither_skeleton_skull", {"rotation": "12"})
        D.put_free(mt, *f.at(1, 2, 0), "minecraft:white_candle", {"candles": "2", "lit": "false", "waterlogged": "false"})
    for (x, z) in ((10, 3), (4, 9), (13, 9), (7, 14)):
        mt.set(x, FR + 1, z, "minecraft:bone_block")
        mt.set(x + 1, FR + 1, z, "minecraft:skeleton_skull", {"rotation": str(rng.randint(0, 15))})
    for k, (x, z) in enumerate(((4, 13), (13, 4), (9, 3), (3, 8), (14, 14), (8, 14))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k == 0 else {}))
    mt.marker(9, FR + 1, 9, "patrol")
    mt.marker(7, FR + 1, 13, "faction", faction="undead")
    mt.finish()
    return mt


# =====================================================================================================================
# SIGIL HALL (puzzle/sigil_hall_a, sigil_hall_b): 3x2, four sigil plates, a riddle stone, a puzzle door and a treasure closet
# =====================================================================================================================

PLATE = "nusmp:sigil_plate"
RIDDLE = "nusmp:riddle_stone"
GATE = "nusmp:gatekeeper_door"
PLATE_COLORS = ("red", "blue", "gold", "green")


def sigil_closet(mt, f, rng):
    """The partition, the puzzle door and the treasure closet behind it. Frame `f` stands on the FRONT-hall cell at the door's first column (r = 0), facing
    into the closet (f = 1 is the partition, f = 2.. the closet): the door is 2 wide (r = 0, 1) and 3 tall; the closet is 4 deep and 6 wide (r = -2 .. 3).
    Everything is written once in the frame, so the room can be mirrored. Returns the closet's floor cells."""
    for r in range(-6, 8):
        for u in range(0, 12):
            for fo in range(1, 6):
                mt.set(*f.at(r, u, fo), L.BRICK)
    for r in range(-2, 4):
        for u in range(0, 5):
            for fo in range(2, 6):
                mt.remove(*f.at(r, u, fo))
    # the door: lower half in the bottom row, upper halves above, closed
    for r in (0, 1):
        mt.set(*f.at(r, 0, 1), GATE, {"kind": "puzzle", "open": "false", "half": "lower"})
        for u in (1, 2):
            mt.set(*f.at(r, u, 1), GATE, {"kind": "puzzle", "open": "false", "half": "upper"})
    for r in (-1, 2):
        for u in range(0, 4):
            mt.set(*f.at(r, u, 1), L.POLISHED)
        mt.set(*f.at(r, 0, 1), L.CHISELED)
    for r in (0, 1):
        mt.set(*f.at(r, 3, 1), L.CHISELED)
    cells = []
    for r in range(-2, 4):
        for fo in range(2, 6):
            cells.append(f.at(r, -1, fo))
            mt.set(*f.at(r, -1, fo), L.POLISHED if (r + fo) % 2 == 0 else L.TILE)
    # the reward
    mt.t.chest(*f.at(1, 0, 5), LOOT_TRIAL, f.dir("back"))
    mt.t.chest(*f.at(0, 0, 5), LOOT_TRIAL, f.dir("back"))
    for r in (-2, 3):
        D.candle_cluster(mt, *f.at(r, 0, 5), rng, colors=("yellow", "white"), count=4)
    mt.set(*f.at(-2, 0, 3), "minecraft:soul_lantern", {"hanging": "false"})
    mt.set(*f.at(3, 0, 3), "minecraft:soul_lantern", {"hanging": "false"})
    return cells


@room("puzzle", "sigil_hall_a", faction="cultist", no_spawn=True, kind="puzzle")
def sigil_hall_a(name):
    """Why: the 1.36.0 puzzle room asked the party to think, not fight: four sigil plates, a Riddle Stone that states the order, a teal puzzle door that
    opens only to that order and hides a small treasure. The hall keeps all of it (same blocks, same block entity) on the 9-grid: a 27 x 18 room with two
    doors (west and north, both in the front hall), the plates in a rectangle round a chiselled medallion, the stone on a plinth by the south wall with a
    lantern over it, bookshelves and banners, and the partition at x = 19 with the door and the 4 x 6 closet behind it. Everything the stone scans
    for (plates and door within 12 cells) is in reach of it, and the closet cannot be entered while the door is shut (both are checked)."""
    mt = MT("mega/puzzle/" + name, 3, 2)
    rng = mt.rng
    mt.room(12, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("w", 13)
    mt.socket("n", 13)
    sigil_closet(mt, Frame(mt, 18, FR + 1, 8, "east"), rng)
    # the front hall floor: a chiselled medallion round the plates
    for z in range(2, 16):
        for x in range(2, 19):
            if (x + z) % 2 == 0 and rng.random() < 0.5:
                mt.set(x, FR, z, L.POLISHED)
            elif rng.random() < 0.1:
                mt.set(x, FR, z, L.CRACKED_TILE)
    for z in (7, 8, 9, 10):
        for x in (11, 12, 13):
            mt.set(x, FR, z, L.CHISELED if (x == 12 and z in (8, 9)) else L.POLISHED)
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for (x, z), c in zip(((10, 6), (14, 6), (10, 11), (14, 11)), colours):
        mt.set(x, FR + 1, z, PLATE, {"color": str(c), "lit": "false"})
    seq = list(SIGIL_ORDER[name])
    mt.set(12, FR + 1, 14, RIDDLE, None, {"id": RIDDLE, "sequence": nbt_list(seq)})
    mt.set(12, FR + 2, 14, L.LANTERN, {"hanging": "false"})
    for x in (11, 13):
        D.banner_on(mt, x, FR + 4, 15, "north", "purple")
    # bookshelves along the north wall (a scholar's hall), pillars, braziers by the door
    for x in range(3, 10):
        for y in (FR + 1, FR + 2, FR + 3):
            mt.set(x, y, 2, "minecraft:bookshelf")
    for x in range(16, 19):
        mt.set(x, FR + 1, 2, "minecraft:bookshelf")
    for (px, pz) in ((5, 5), (5, 12), (16, 4), (16, 13)):
        D.column(mt, px, pz, FR + 1, FR + 10)
        D.pillar_lantern(mt, px, pz, "east" if px < 12 else "west", FR + 3)
    D.brazier(mt, 18, FR + 1, 6, cage=True)
    D.brazier(mt, 18, FR + 1, 11, cage=True)
    L.chandelier(mt, 12, FR + 13, 8, drop=7, radius=2)
    mt.marker(7, FR + 1, 9, "patrol")
    mt.marker(15, FR + 1, 9, "patrol")
    mt.marker(5, FR + 1, 8, "faction", faction="cultist")
    D.dress_walls(mt, rng, [(3, D.prop_barrels), (2, D.prop_candles)], 5, spacing=4)
    mt.finish()
    return mt


#: The colour order each sigil hall asks for (the Riddle Stone's `sequence`). Fixed and different, so a party that solved one hall learns nothing about the other,
#: and neither order is just "left to right" over the plates as they stand.
SIGIL_ORDER = {"sigil_hall_a": (2, 3, 1, 0), "sigil_hall_b": (0, 2, 1, 3)}


def nbt_list(values):
    """An NBT list of ints, the shape the Riddle Stone's `sequence` field has in the Spire templates."""
    import nbt as _nbt
    return _nbt.List([_nbt.Int(v) for v in values], tag_type=_nbt.TAG_INT)


# =====================================================================================================================
# TRIAL HALL (trial/trial_hall_a, trial_hall_b): 3x3, a Sealed Archway trial: the doors seal, the hall spawns its own wave
# =====================================================================================================================

ARCHWAY = "nusmp:sealed_archway"
WARD_RUNE = "nusmp:ward_rune"
SPAWN_GLYPH = "nusmp:spawn_glyph"


def archway_data(radius, height, mobs, reset):
    """The Sealed Archway block entity tag exactly as the Spire's trial rooms write it (gen_spire.archway_nbt): the watched box, the roster, the reset time."""
    import nbt as _nbt
    return {"id": ARCHWAY, "radius": _nbt.Int(radius), "height": _nbt.Int(height),
            "mobs": _nbt.List([_nbt.Compound({"id": i, "count": _nbt.Int(c)}) for i, c in mobs], tag_type=_nbt.TAG_COMPOUND), "reset": _nbt.Int(reset)}


def trial_core(mt, mobs, rng, doors, eye_row, glyphs):
    """What every trial hall has: ward runes in the floor of each doorway (the inner layer), the spawn glyphs where the wave appears, and the keystone
    hanging from the ceiling centre on two chains, its box exactly the 27 x 27 footprint (radius 13) from the floor to the ceiling."""
    for (side, off) in doors:
        for lat in range(off - 1, off + 2):
            x, y, z = mt._lateral(side, off, lat, 1, FR)
            mt.set(x, y, z, WARD_RUNE)
    for (x, z) in glyphs:
        mt.set(x, FR, z, SPAWN_GLYPH)
    cx = cz = 13
    for y in range(eye_row + 1, FR + 15):
        mt.set(cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    mt.set(cx, eye_row, cz, ARCHWAY, None, archway_data(13, eye_row - (FR - 1), mobs, 60))


@room("trial", "trial_hall_a", faction="undead", no_spawn=True, kind="trial")
def trial_hall_a(name):
    """Why: the Spire's trial chamber (docs/labyrinth/e1_trial.md) is the one fight in the dungeon that is fair by construction: walk in through a doorway and
    the archway seals both doors with magic barriers, the glyphs spawn the wave (zombies and skeletons, scaled by the party), and the barriers fall when the last
    one dies. The Mega-Dungeon hall gives it a stage: a round dais with a chiselled sigil in the middle (the party's anchor), eight glyphs on a ring, twelve
    pillars with lanterns for cover, a hanging eye (the keystone) over the dais and a chest in an arched alcove opposite the door that the party earns."""
    mt = MT("mega/trial/" + name, 3, 3)
    rng = mt.rng
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("n", 13)
    mt.socket("s", 13)
    c = 13
    # floor: concentric rings, the axis between the doors polished
    for z in range(2, 25):
        for x in range(2, 25):
            d = D.dist(x, z, c, c)
            blk = L.TILE
            if d <= 4.5:
                blk = L.POLISHED
            elif 7.5 < d <= 8.5:
                blk = L.CHISELED if int(D.angle_of(x, z, c, c) / 22.5) % 2 == 0 else L.POLISHED
            elif abs(x - c) <= 1:
                blk = L.POLISHED
            elif rng.random() < 0.12:
                blk = L.CRACKED_TILE
            mt.set(x, FR, z, blk)
    for z in range(2, 25):
        for x in range(2, 25):
            d = D.dist(x, z, c, c)
            if d <= 3.5:
                mt.set(x, FR + 1, z, L.POLISHED)
            elif d <= 4.5:
                D.put(mt, x, FR + 1, z, L.POLISHED_STAIRS, {"facing": D.stair_facing(c - x, c - z), "half": "bottom"})
    mt.set(c, FR + 1, c, L.CHISELED)
    glyphs = D.around(c, c, 8, 8, 22.5)
    trial_core(mt, [("minecraft:zombie", 3), ("minecraft:skeleton", 2)], rng, [("n", 13), ("s", 13)], FR + 12, glyphs)
    for (px, pz) in D.around(c, c, 10.5, 12, 15.0):
        D.column(mt, px, pz, FR + 1, FR + 13)
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3)
        ix, iz = D.polar(c, c, D.dist(px, pz, c, c) - 1.5, D.angle_of(px, pz, c, c))
        if D.put_free(mt, ix, FR + 12, iz, L.POLISHED_SLAB, {"type": "bottom"}):
            mt.set(ix, FR + 11, iz, L.CHAIN_BLOCK, {"axis": "y"})
            mt.set(ix, FR + 10, iz, L.LANTERN, {"hanging": "true"})
    for k in range(4):
        x, z = D.polar(c, c, 3.4, 45.0 + 90.0 * k)
        D.brazier(mt, x, FR + 2, z, height=2, cage=False)
    # the reward: an arched alcove in the west wall, braziers either side; a statue alcove opposite
    f = D.inner_frame(mt, "w", 13)
    D.arched_alcove(mt, f, 5, 5)
    mt.t.chest(*f.at(0, 0, -1), LOOT_TRIAL, "east")
    for r in (-2, 2):
        D.candle_cluster(mt, *f.at(r, 0, -1), rng, colors=("yellow", "orange"), count=4)
    D.brazier(mt, *f.at(-4, 0, 0), cage=True)
    D.brazier(mt, *f.at(4, 0, 0), cage=True)
    f = D.inner_frame(mt, "e", 13)
    D.arched_alcove(mt, f, 5, 5)
    D.alcove_statue(mt, f, ("diamond", "diamond_sword"))
    for side in ("n", "s"):
        for lat in (6, 20):
            D.sconce(mt, D.inner_frame(mt, side, lat), 0, 5)
            D.banner_on(mt, *D.inner_frame(mt, side, lat).at(2, 5, 0), "south" if side == "n" else "north", "black")
    for (x, z) in ((13, 6), (13, 20), (6, 13)):         # two on the door-to-door route, one beside it (at most three per room)
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(4, FR + 1, 4, "faction", faction="undead")
    D.dress_walls(mt, rng, [(3, D.prop_rack), (2, D.prop_bones), (2, D.prop_rubble)], 8, depth=2, spacing=4)
    mt.finish()
    return mt


@room("trial", "trial_hall_b", faction="construct", no_spawn=True, kind="trial")
def trial_hall_b(name):
    """Why: the second trial chamber of the Spire sent living armour and a dire wolf. Here it is a hall of armour: a throne alcove at the north end with the
    chest on a three-step dais, two rows of pillars with lanterns, a carpet from the west door to the east door, eight armoured statues in niches that watch
    the party (they are scenery; the wave is the roster of the keystone: living armour and a dire wolf), six glyphs flanking the carpet. The doors are west
    and east, so a party that fights with its back to the carpet has pillars on both sides."""
    mt = MT("mega/trial/" + name, 3, 3)
    rng = mt.rng
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("w", 13)
    mt.socket("e", 13)
    c = 13
    for z in range(2, 25):
        for x in range(2, 25):
            blk = L.TILE
            if 12 <= z <= 14:
                blk = L.POLISHED
            elif (x + z) % 2 == 0 and abs(z - c) >= 8:
                blk = L.POLISHED
            elif rng.random() < 0.12:
                blk = L.CRACKED_TILE
            mt.set(x, FR, z, blk)
    for x in range(2, 25):
        for z in range(12, 15):
            D.put_free(mt, x, FR + 1, z, "minecraft:black_carpet")
    # the throne dais against the north wall: a stair row and a block row per level (every rise is two half steps), the chest on the top level
    for x in range(9, 18):
        mt.set(x, FR + 1, 8, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
        mt.set(x, FR + 1, 7, L.POLISHED)
        mt.set(x, FR + 1, 6, L.BRICK)
        mt.set(x, FR + 2, 6, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
        for y in (FR + 1, FR + 2):
            mt.set(x, y, 5, L.BRICK if y == FR + 1 else L.POLISHED)
        for y in (FR + 1, FR + 2):
            mt.set(x, y, 4, L.BRICK)
        mt.set(x, FR + 3, 4, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
        for z in (2, 3):
            for y in (FR + 1, FR + 2):
                mt.set(x, y, z, L.BRICK)
            mt.set(x, FR + 3, z, L.POLISHED)
    mt.t.chest(13, FR + 4, 3, LOOT_TRIAL, "south")
    for x in (11, 15):
        D.brazier(mt, x, FR + 4, 3, cage=False)
    D.lit_window(mt, D.inner_frame(mt, "n", 13), width=3, y0=6, height=8, glass="minecraft:red_stained_glass", backing="minecraft:shroomlight")
    glyphs = [(x, z) for x in (8, 13, 18) for z in (9, 17)]
    trial_core(mt, [("nusmp:living_armor", 2), ("nusmp:dungeon_direwolf", 1)], rng, [("w", 13), ("e", 13)], FR + 12, glyphs)
    for pz in (7, 19):
        for px in (5, 9, 13, 17, 21):
            if pz == 7 and 9 <= px <= 17:
                continue
            D.column(mt, px, pz, FR + 1, FR + 13)
            D.pillar_lantern(mt, px, pz, "south" if pz == 7 else "north", FR + 3)
            iz = pz + (1 if pz == 7 else -1)
            if D.put_free(mt, px, FR + 12, iz, L.POLISHED_SLAB, {"type": "bottom"}):
                mt.set(px, FR + 11, iz, L.CHAIN_BLOCK, {"axis": "y"})
                mt.set(px, FR + 10, iz, L.LANTERN, {"hanging": "true"})
    gear = (("iron", "iron_sword"), ("diamond", "diamond_sword"), ("netherite", "netherite_sword"), ("chainmail", "iron_axe"))
    for i, zx in enumerate((5, 9, 13, 17, 21)):
        f = D.inner_frame(mt, "s", zx)
        D.arched_alcove(mt, f, 3, 5)
        D.alcove_statue(mt, f, gear[i % 4]) if i % 2 == 0 else D.alcove_candles(mt, f, rng)
    for i, zx in enumerate((5, 21)):
        f = D.inner_frame(mt, "n", 4 if i == 0 else 22)
        D.arched_alcove(mt, f, 3, 5)
        D.alcove_statue(mt, f, gear[(i + 1) % 4])
    for (x, z) in ((6, 6), (20, 6), (6, 20), (20, 20), (13, 22)):
        mt.marker(x, FR + 1, z, "patrol") if (x, z) in ((6, 20), (20, 20), (13, 22)) else None
    mt.marker(4, FR + 1, 9, "faction", faction="construct")
    D.dress_walls(mt, rng, [(3, D.prop_rubble), (2, D.prop_bones)], 6, depth=2, spacing=4)
    mt.finish()
    return mt


@room("puzzle", "sigil_hall_b", faction="construct", no_spawn=True, kind="puzzle")
def sigil_hall_b(name):
    """Why: the second sigil hall turns the first one round (the closet is at the west end, the doors east and south) and changes the furniture, so a party
    that has solved one does not recognise the other: the four plates stand in a row across the middle (not round a medallion), the stone is on the north wall,
    a ring of eight lanterns hangs from the ceiling like the orrery the puzzle belongs to, and lecterns with books line the south wall."""
    mt = MT("mega/puzzle/" + name, 3, 2)
    rng = mt.rng
    mt.room(12, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    mt.socket("e", 4)
    mt.socket("s", 13)
    sigil_closet(mt, Frame(mt, 9, FR + 1, 9, "west"), rng)
    for z in range(2, 16):
        for x in range(9, 25):
            if abs(z - 8.5) <= 1.5 and (x % 2 == 0):
                mt.set(x, FR, z, L.POLISHED)
            elif rng.random() < 0.12:
                mt.set(x, FR, z, L.CRACKED_TILE)
    colours = [0, 1, 2, 3]
    rng.shuffle(colours)
    for x, c in zip((12, 15, 18, 21), colours):
        mt.set(x, FR + 1, 11, PLATE, {"color": str(c), "lit": "false"})
    seq = list(SIGIL_ORDER[name])
    mt.set(14, FR + 1, 3, RIDDLE, None, {"id": RIDDLE, "sequence": nbt_list(seq)})
    mt.set(14, FR + 2, 3, L.LANTERN, {"hanging": "false"})
    for x in (13, 15):
        D.banner_on(mt, x, FR + 4, 2, "south", "cyan")
    for x in (17, 19, 21, 23):
        mt.set(x, FR + 1, 15, "minecraft:lectern", {"facing": "north", "has_book": "true", "powered": "false"})
    for (x, z) in D.around(18, 8, 4.0, 8, 0.0):
        if z < 15:
            D.hang_lantern(mt, x, FR + 13, z, drop=3)
    for (px, pz) in ((12, 5), (22, 5), (12, 13), (22, 13)):
        D.column(mt, px, pz, FR + 1, FR + 10)
        D.pillar_lantern(mt, px, pz, "west" if px > 17 else "east", FR + 3)
    D.brazier(mt, 10, FR + 1, 6, cage=True)
    D.brazier(mt, 10, FR + 1, 11, cage=True)
    mt.marker(14, FR + 1, 8, "patrol")
    mt.marker(21, FR + 1, 8, "patrol")
    mt.marker(21, FR + 1, 4, "faction", faction="construct")
    D.dress_walls(mt, rng, [(3, D.prop_candles), (2, D.prop_rubble)], 4, spacing=4)
    mt.finish()
    return mt


@room("trap", "pit_hall_b", faction="construct", kind="pit")
def pit_hall_b(name):
    """Why: the second pit hall is round (a disc of pitfall tiles of radius 7 in a square hall, so the ledge is wide in the corners and narrow at the compass
    points) and gives the party a different clue than the first: the route to the treasure island is traced overhead as a trail of sea lanterns set in the
    ceiling over every second safe tile (look up, and the stars are the path). A ring of eight pillars stands on the rim, iron cages with skulls hang over the
    corners, and the ladder out of the spiked chamber is at the east rim."""
    mt = MT("mega/trap/" + name, 3, 3)
    rng = mt.rng
    pit_hall_shell(mt)
    for side in ("w", "e", "n"):
        mt.socket(side, 13)
    c = 13
    field = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 7)}
    island = {(x, z) for x in range(12, 15) for z in range(12, 15)}
    goals = {(x, z) for (x, z) in field - island if any((x + dx, z + dz) in island for dx, dz in L.STEP.values())}
    start = rng.choice(sorted(cell for cell in field if cell[1] == 6))
    route = D.snake_path(rng, start, goals, field - island, min_len=14)
    if route is None:
        raise ValueError("%s: no safe route found" % name)
    safe = set(route) | island
    pit_chamber(mt, field, safe, rng, ladder=(22, 13))
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in field or (x, z) == (22, 13):
                continue
            mt.set(x, FR, z, L.POLISHED if (x // 2 + z // 2) % 2 == 0 else (L.CRACKED_TILE if rng.random() < 0.1 else L.TILE))
    mt.t.chest(13, FR + 1, 13, LOOT_TRIAL, "north")
    for dx, dz in ((-1, 0), (1, 0), (0, 1), (0, -1)):
        D.candle_cluster(mt, 13 + dx, FR + 1, 13 + dz, rng, colors=("purple", "magenta"), count=3)
    for (px, pz) in D.around(c, c, 8.6, 8, 0.0):
        D.column(mt, px, pz, FR + 1, FR + 12)
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3)
    for i, (x, z) in enumerate(route):
        if i % 2 == 0:
            mt.set(x, FR + 15, z, "minecraft:sea_lantern")
    for (x, z) in ((4, 4), (22, 4), (4, 22), (22, 22)):
        y_top = FR + 15
        for y in range(y_top - 4, y_top):
            mt.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
        L.cage(mt, x - 1, y_top - 8, z - 1, 3, 3, 4)
        D.brazier(mt, x + (1 if x < 13 else -1), FR + 1, z + (1 if z < 13 else -1), cage=True)
    for side in ("n", "s", "e", "w"):
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4)
    for k, (x, z) in enumerate(((3, 8), (3, 18), (23, 8), (23, 18), (8, 3), (18, 23), (8, 23), (18, 3))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for ang in (67.5, 157.5, 247.5):
        x, z = D.polar(c, c, 10.0, ang)
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(11, FR + 1, 22, "faction", faction="construct")
    for (x, z) in [c_ for c_ in route[1::4]][:4]:
        mt.marker(x, FR + 1, z, "trap", type="pit")
    D.dress_walls(mt, rng, [(3, D.prop_rubble), (2, D.prop_rack), (2, D.prop_bones)], 8, depth=2, spacing=4)
    D.corner_webs(mt, rng, 0.6)
    mt.finish()
    return mt


# =====================================================================================================================
# CISTERN (hall/cistern_a, cistern_b): 3x3, a flooded hall: water in the rows under the floor, walkways, bridges and arches over it
# =====================================================================================================================

WATER = "minecraft:water"
WATER_TOP = FR - 1          # row 7: the top water row; the surface is just under the walkways
WATER_BOTTOM = 3            # row 3: the lowest water row; rows 1-2 are the basin floor


def rect(x0, x1, z0, z1):
    """The set of (x, z) cells of an inclusive rectangle."""
    return {(x, z) for x in range(x0, x1 + 1) for z in range(z0, z1 + 1)}


def cistern_shell(mt, walk, rng):
    """The room, the sealed basin under it and the walkway floor. Why a sealed basin built first: every water source block flows into any open neighbour, so
    the whole volume (rows 3..7 over the interior) is bounded by a solid boundary (rows 1..7 under the two wall layers), a two-thick basin floor and, between
    the pools, pillars and the walkway slabs; `Analysis.fluid_problems` re-checks every water cell. The floor row exists only on `walk`."""
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    W, Dd = mt.W, mt.D
    for z in range(Dd):
        for x in range(W):
            interior = 2 <= x <= W - 3 and 2 <= z <= Dd - 3
            for y in range(1, FR):
                if not interior:
                    mt.set(x, y, z, rng.choice((L.COBBLED, L.ROCK, L.BRICK)))
                elif y <= 2:
                    mt.set(x, y, z, rng.choice((L.COBBLED, L.ROCK)))
                else:
                    mt.set(x, y, z, WATER, {"level": "0"})
            if interior:
                if (x, z) in walk:
                    mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else (L.CRACKED_TILE if rng.random() < 0.1 else L.TILE))
                else:
                    mt.remove(x, FR, z)


def submerged_pillar(mt, x, z, top):
    """A pillar standing on the basin floor: plain brick in the water, a chiselled foot at the walkway, a polished shaft, a capital. No stair skirt at the
    foot (a stair next to water would be waterlogged by it)."""
    for y in range(WATER_BOTTOM, FR + 1):
        mt.set(x, y, z, L.BRICK)
    mt.set(x, FR + 1, z, L.CHISELED)
    for y in range(FR + 2, top):
        mt.set(x, y, z, L.POLISHED)
    mt.set(x, top, z, L.CHISELED)
    for dx, dz, f in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        D.put_free(mt, x + dx, top, z + dz, L.POLISHED_STAIRS, {"facing": f, "half": "top"})


def landing_steps(mt, edge, away):
    """Three stairs from the water up to the walkway cell `edge`, going `away` (dx, dz) from it: row 6, row 7 (both waterlogged: they stand in the water)
    and row 8 (flush with the walkway), each with its tall side toward the walkway. So a swimmer can always climb out."""
    ex, ez = edge
    dx, dz = away
    facing = {(1, 0): "west", (-1, 0): "east", (0, 1): "north", (0, -1): "south"}[(dx, dz)]
    for k, row in ((1, FR), (2, FR - 1), (3, FR - 2)):
        x, z = ex + k * dx, ez + k * dz
        wl = "true" if row < FR else "false"
        mt.set(x, row, z, L.POLISHED_STAIRS, {"facing": facing, "half": "bottom", "waterlogged": wl})


def pier_lamps(mt, spots, height=3):
    """Why: the sea lanterns glow up through the water, but the surface only gets a dim light from them, and the arms of a cistern are long and narrow:
    a lamp post standing in the water beside the walkway (a brick shaft from the basin floor, polished wall posts over the walkway and a lantern on top)
    lights the arm at head height like a pier lamp does. A spot that is not an open water cell (a bridge, a landing step, a pillar) is skipped.
    Returns the spots that were built."""
    built = []
    for (x, z) in spots:
        below = mt.get(x, WATER_TOP, z)
        if below is None or below.block != WATER or mt.get(x, FR, z) is not None:
            continue
        for y in range(WATER_BOTTOM, FR + 1):
            mt.set(x, y, z, L.BRICK)
        D.lamp_post(mt, x, FR + 1, z, height=height)
        built.append((x, z))
    return built


def basin_lights(mt, rng, walk, pillars, count=22):
    """Sea lanterns in the basin floor (they shine up through the water: the main light of the hall) and glow lichen on the basin walls under the surface."""
    cells = [(x, z) for z in range(3, mt.D - 3) for x in range(3, mt.W - 3) if (x, z) not in pillars]
    rng.shuffle(cells)
    placed = []
    for (x, z) in cells:
        if all(abs(x - px) + abs(z - pz) > 4 for (px, pz) in placed):
            mt.set(x, 2, z, "minecraft:sea_lantern")
            placed.append((x, z))
            if len(placed) >= count:
                break
    for k in range(34):
        side = rng.choice(L.SIDES)
        f = D.inner_frame(mt, side, rng.randint(3, mt.W - 4 if side in ("n", "s") else mt.D - 4), y=rng.randint(WATER_BOTTOM, FR - 2))
        pos = f.at(0, 0, 0)
        c = mt.get(*pos)
        if c is not None and c.block == WATER:
            mt.set(*pos, "minecraft:glow_lichen", {f.dir("back"): "true", "waterlogged": "true"})


def drip_chains(mt, rng, cells, walk, ceiling_row, soul=True):
    """Chains hanging from the ceiling over the water, some ending in a soul lantern, and stalactites between them."""
    for (x, z) in cells:
        if (x, z) in walk or mt.get(x, ceiling_row - 1, z) is not None:
            continue
        n = rng.randint(4, 9)
        ok = all(mt.get(x, ceiling_row - 1 - k, z) is None for k in range(n + 1))
        if not ok:
            continue
        for k in range(n):
            mt.set(x, ceiling_row - 1 - k, z, L.CHAIN_BLOCK, {"axis": "y"})
        if soul and rng.random() < 0.6:
            mt.set(x, ceiling_row - 1 - n, z, L.SOUL_LANTERN, {"hanging": "true"})


def stalactites(mt, rng, cells, walk, ceiling_row, count):
    """Why: dripstone over the water of a cistern is what makes it read as a cave of water: up to `count` stalactites hung from the ceiling on seeded cells
    that are not over a walkway."""
    cells = sorted(cells)
    rng.shuffle(cells)
    n = 0
    for (x, z) in cells:
        if (x, z) in walk:
            continue
        if D.dripstone(mt, x, ceiling_row - 1, z, rng.choice((1, 2, 3, 4)), "down"):
            n += 1
            if n >= count:
                break


@room("hall", "cistern_a", faction="aquatic")
def cistern_a(name):
    """Why: a flooded cistern is the dungeon's water level without the dungeon having to flood: the hall is 5 rows of water under a stone cross of walkways
    (3 wide) that meets in an octagonal plaza with a statue, four gate arches on pillars that stand in the water, two corner islands joined by bridges and two
    that must be swum to (the chests are there), landing stairs so nobody is stranded, sea lanterns glowing up through the water and soul lanterns on chains. The
    party fights on the cross with water on both sides (the aquatic faction comes out of it); nothing but the walkways can be stood on."""
    mt = MT("mega/hall/" + name, 3, 3)
    rng = mt.rng
    arm_ns, arm_ew = rect(12, 14, 2, 24), rect(2, 24, 12, 14)
    plaza = {(x, z) for x in range(9, 18) for z in range(9, 18) if not (abs(x - 13) == 4 and abs(z - 13) == 4)}
    nw, ne, sw, se = rect(2, 7, 2, 7), rect(19, 24, 2, 7), rect(2, 7, 19, 24), rect(19, 24, 19, 24)
    bridges = rect(8, 11, 3, 5) | rect(15, 18, 21, 23)
    walk = arm_ns | arm_ew | plaza | nw | ne | sw | se | bridges
    cistern_shell(mt, walk, rng)
    for side in L.SIDES:
        mt.socket(side, 13)
    gates = [(11, 8), (15, 8), (11, 18), (15, 18), (8, 11), (8, 15), (18, 11), (18, 15)]
    corner_pillars = [(9, 9), (17, 9), (9, 17), (17, 17)]
    pillars = set(gates) | set(corner_pillars)
    top = FR + 8
    for (px, pz) in gates:
        submerged_pillar(mt, px, pz, top)
    for (px, pz) in corner_pillars:
        submerged_pillar(mt, px, pz, top - 2)
        mt.set(px, top - 2 + 1, pz, L.LANTERN, {"hanging": "false"})
    for z in (8, 18):
        D.arch(mt, Frame(mt, 13, FR + 1, z, "south"), 3, 6, depth=1, carve=False)
    for x in (8, 18):
        D.arch(mt, Frame(mt, x, FR + 1, 13, "east"), 3, 6, depth=1, carve=False)
    for (px, pz) in gates:
        mt.set(px, top, pz, L.CHISELED)
    # the plaza: a statue on a plinth with candles; landing steps at the arm ends and beside the islands
    L.statue(mt, 13, FR + 1, 13, facing="south", head="minecraft:diamond_helmet", chest="minecraft:diamond_chestplate", legs="minecraft:diamond_leggings",
             feet="minecraft:diamond_boots", weapon="minecraft:trident")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        D.candle_cluster(mt, 13 + dx, FR + 1, 13 + dz, rng, colors=("light_blue", "cyan", "white"), count=3)
    for (edge, away) in (((12, 3), (-1, 0)), ((14, 23), (1, 0)), ((3, 12), (0, -1)), ((23, 14), (0, 1)), ((12, 20), (-1, 0)), ((14, 6), (1, 0))):
        landing_steps(mt, edge, away)
    # islands: NW and SE hold braziers and banners (reached by the bridges), NE and SW hold the chests (swim for them)
    for (cx_, cz_) in ((4, 4), (22, 22)):
        D.brazier(mt, cx_, FR + 1, cz_, cage=True)
        D.brazier(mt, cx_ + (2 if cx_ < 13 else -2), FR + 1, cz_ + (2 if cx_ < 13 else -2), cage=True)
    mt.t.chest(22, FR + 1, 3, LOOT_TRIAL, "south")
    mt.t.chest(3, FR + 1, 23, LOOT_TRIAL, "north")
    for (cx_, cz_, f_) in ((21, 4, "south"), (23, 4, "south"), (3, 22, "north"), (5, 22, "north")):
        D.candle_cluster(mt, cx_, FR + 1, cz_, rng, colors=("light_blue", "cyan"), count=4)
    pillars |= set(pier_lamps(mt, ((5, 11), (5, 15), (21, 11), (21, 15), (15, 4), (11, 22))))
    basin_lights(mt, rng, walk, pillars)
    ceil = FR + 15
    chain_cells = [(x, z) for z in range(3, 24, 2) for x in range(3, 24, 2) if (x + z) % 4 == 0]
    drip_chains(mt, rng, chain_cells, walk, ceil)
    stalactites(mt, rng, [(x, z) for z in range(3, 24) for x in range(3, 24)], walk, ceil, 16)
    for side in L.SIDES:
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4, skull="skeleton")
    for k, (x, z) in enumerate([(13, 5), (13, 21), (5, 13), (21, 13), (11, 13), (15, 13), (13, 11), (13, 15)]):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for (x, z) in ((13, 8), (13, 18), (8, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(10, FR + 1, 13, "faction", faction="aquatic")
    mt.marker(13, FR + 1, 9, "room_center")
    mt.finish()
    return mt


@room("hall", "cistern_b", faction="aquatic")
def cistern_b(name):
    """Why: the second cistern is round: a plaza (radius 4) joined by four spokes to a ring bridge (radius 8 to 10, three wide) that circles it, four spokes
    from the ring to the doors, gate arches on pillars where the inner spokes meet the ring, eight pillars round the plaza carrying a ring beam of lanterns,
    and four three-by-three islets in the corner pools (two with chests, two with statues) that can only be swum to. Where the first cistern is a cross,
    this one is a wheel: the party can circle the plaza and the water is in four inner and four outer pools."""
    mt = MT("mega/hall/" + name, 3, 3)
    rng = mt.rng
    c = 13
    plaza = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 4)}
    ring = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 10) and not D.in_disc(x, z, c, c, 7)}
    spokes = rect(12, 14, 2, 24) | rect(2, 24, 12, 14)
    islets = rect(3, 5, 3, 5) | rect(21, 23, 3, 5) | rect(3, 5, 21, 23) | rect(21, 23, 21, 23)
    walk = plaza | ring | spokes | islets
    cistern_shell(mt, walk, rng)
    for side in L.SIDES:
        mt.socket(side, 13)
    gates = [(11, 6), (15, 6), (11, 20), (15, 20), (6, 11), (6, 15), (20, 11), (20, 15)]
    rim = D.around(c, c, 5.8, 8, 22.5)
    pillars = set(gates) | set(rim)
    top = FR + 8
    for (px, pz) in gates:
        submerged_pillar(mt, px, pz, top)
        mt.set(px, top, pz, L.CHISELED)
    for z in (6, 20):
        D.arch(mt, Frame(mt, 13, FR + 1, z, "south"), 3, 6, depth=1, carve=False)
    for x in (6, 20):
        D.arch(mt, Frame(mt, x, FR + 1, 13, "east"), 3, 6, depth=1, carve=False)
    for (px, pz) in rim:
        if (px, pz) in walk:
            D.column(mt, px, pz, FR + 1, FR + 7)
        else:
            submerged_pillar(mt, px, pz, FR + 7)
        mt.set(px, FR + 8, pz, L.LANTERN, {"hanging": "false"})
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3)
    # the plaza: a fountain-like brazier of lanterns round a statue
    L.statue(mt, c, FR + 1, c, facing="south", head="minecraft:netherite_helmet", chest="minecraft:netherite_chestplate", legs="minecraft:netherite_leggings",
             feet="minecraft:netherite_boots", weapon="minecraft:trident")
    for dx, dz in ((-1, 0), (1, 0), (0, -1), (0, 1)):
        D.candle_cluster(mt, c + dx, FR + 1, c + dz, rng, colors=("light_blue", "white"), count=4)
    for (edge, away) in (((12, 3), (-1, 0)), ((14, 23), (1, 0)), ((3, 12), (0, -1)), ((23, 14), (0, 1)), ((14, 11), (1, 0)), ((12, 15), (-1, 0))):
        landing_steps(mt, edge, away)
    mt.t.chest(4, FR + 1, 4, LOOT_TRIAL, "south")
    mt.t.chest(22, FR + 1, 22, LOOT_TRIAL, "north")
    L.statue(mt, 22, FR + 1, 4, facing="south")
    L.statue(mt, 4, FR + 1, 22, facing="north")
    basin_lights(mt, rng, walk, pillars)
    ceil = FR + 15
    chain_cells = [(x, z) for z in range(3, 24, 2) for x in range(3, 24, 2) if (x * 3 + z) % 5 == 0]
    drip_chains(mt, rng, chain_cells, walk, ceil)
    stalactites(mt, rng, [(x, z) for z in range(3, 24) for x in range(3, 24)], walk, ceil, 18)
    for side in L.SIDES:
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4, skull="skeleton")
    for k, ang in enumerate(D.around(c, c, 8.9, 8, 22.5)):
        x, z = ang
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for (x, z) in ((13, 4), (13, 22), (4, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(c, FR + 1, c + 3, "faction", faction="aquatic")
    mt.marker(c, FR + 1, c - 3, "room_center")
    mt.finish()
    return mt


# =====================================================================================================================
# LAVA HALL (hall/lava_hall_a, lava_hall_b): 3x3, lava channels crossed by iron-grate bridges, glass-cage lava shafts
# =====================================================================================================================

LAVA = "minecraft:lava"
GRATE = "minecraft:iron_trapdoor"
LAVA_TOP = FR - 1           # row 7
LAVA_BOTTOM = FR - 3        # row 5; row 4 is the channel floor


def lava_shell(mt, rng):
    """The room over a solid slab (rows 3..7 under the whole box): the lava channels are carved out of it, so every lava cell is bounded by solid blocks by
    construction (and `Analysis.fluid_problems` checks it). Lava is only ever placed in cells carved from the slab, never against air."""
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    for z in range(mt.D):
        for x in range(mt.W):
            for y in range(LAVA_BOTTOM - 1, FR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))


def lava_channel(mt, cells, bridge_cells):
    """Carves `cells` (a set of (x, z)) into lava rows 5..7 (floor row 4 stays) and closes the bridge cells with iron-grate trapdoors flush with the floor
    (half = top: the top face is level with the floor, and the lava shows through the grate)."""
    for (x, z) in sorted(cells):
        mt.remove(x, FR, z)
        for y in range(LAVA_BOTTOM, LAVA_TOP + 1):
            mt.set(x, y, z, LAVA, {"level": "0"})
        if (x, z) in bridge_cells:
            mt.set(x, FR, z, GRATE, {"facing": "north", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"})


def lava_cage(mt, cx, cz, top=FR + 9):
    """A glass-cage lava shaft: one lava column from the channel level up to `top`, a ring of orange glass round it and a glass cap. The column and the ring
    stand on the floor row; the lava cells under the floor were carved from the slab."""
    for y in range(LAVA_BOTTOM, top + 1):
        mt.set(cx, y, cz, LAVA, {"level": "0"})
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            if dx or dz:
                for y in range(FR + 1, top + 1):
                    mt.set(cx + dx, y, cz + dz, "minecraft:orange_stained_glass")
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            mt.set(cx + dx, top + 1, cz + dz, "minecraft:orange_stained_glass")


def hanging_cage(mt, x, z, ceiling_row, drop=5, skull="skeleton"):
    """An iron-bar cage with a skull in it, hung from the ceiling by one chain (a cage is `mega_lib.cage`: bars on every side and the top)."""
    for y in range(ceiling_row - drop, ceiling_row):
        mt.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
    L.cage(mt, x - 1, ceiling_row - drop - 4, z - 1, 3, 3, 4)
    mt.set(x, ceiling_row - drop, z, L.IRON_BARS)
    mt.set(x, ceiling_row - drop - 4, z, "minecraft:%s_skull" % skull, {"rotation": "3"})


def magma_cracks(mt, rng, count):
    """Glowing cracks: magma blocks set into the inner layer of the walls (never on a floor, magma burns whoever stands on it)."""
    n = 0
    tries = 0
    while n < count and tries < 400:
        tries += 1
        side = rng.choice(L.SIDES)
        lat = rng.randint(3, (mt.W if side in ("n", "s") else mt.D) - 4)
        f = D.inner_frame(mt, side, lat, y=FR + rng.randint(1, 10))
        pos = f.at(0, 0, -1)
        c = mt.get(*pos)
        if c is not None and c.block in (L.BRICK, L.CRACKED_BRICK) and mt.get(*f.at(0, 0, 0)) is None:
            mt.set(*pos, "minecraft:magma_block")
            n += 1


@room("hall", "lava_hall_a", faction="elemental")
def lava_hall_a(name):
    """Why: the lava hall is the dungeon's fire level: two three-wide rivers of lava run the length of the hall (rows 5..7 under the floor, carved from a
    solid slab so they can never flow), crossed by iron-grate bridges (iron trapdoors laid flush with the floor, the lava glowing through them) at the door axis
    and twice more, with a railing of polished walls along the banks so nobody walks in by accident. In the middle band two glass cages hold a column of lava
    that rises to the ceiling, hanging cages with skulls swing over the rivers, and magma cracks glow in the walls. Stone only within three blocks of the lava
    (lava sets wood and wool alight), which the generator proves. The elementals come from the rivers."""
    mt = MT("mega/hall/" + name, 3, 3)
    rng = mt.rng
    lava_shell(mt, rng)
    for side in L.SIDES:
        mt.socket(side, 13)
    channels = rect(8, 10, 2, 24) | rect(16, 18, 2, 24)
    bridges = rect(8, 18, 12, 14) | rect(8, 18, 4, 6) | rect(8, 18, 20, 22)
    lava_channel(mt, channels, bridges & channels)
    # railings along the banks, open at the bridge mouths
    for z in range(2, 25):
        for x in (7, 11, 15, 19):
            on_bridge = any(z in range(a, b + 1) for (a, b) in ((12, 14), (4, 6), (20, 22)))
            if not on_bridge and mt.get(x, FR + 1, z) is None:
                mt.set(x, FR + 1, z, L.POLISHED_WALL, {"up": "true"})
    lava_cage(mt, 13, 8)
    lava_cage(mt, 13, 18)
    for (x, z) in ((5, 8), (21, 8), (5, 18), (21, 18)):
        D.column(mt, x, z, FR + 1, FR + 13)
    for (x, z) in ((9, 9), (17, 9), (9, 17), (17, 17)):
        hanging_cage(mt, x, z, FR + 15)
    for (x, z) in ((5, 3), (21, 3), (5, 23), (21, 23), (13, 3), (13, 23)):
        D.brazier(mt, x, FR + 1, z, cage=True)
    for x, z in ((9, 8), (17, 8), (9, 18), (17, 18), (9, 13), (17, 13)):
        D.light(mt, x, FR + 11, z, 11)
    magma_cracks(mt, rng, 26)
    for side in L.SIDES:
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4)
    for k, (x, z) in enumerate(((13, 4), (13, 22), (13, 11), (13, 15), (4, 13), (22, 13), (4, 5), (22, 21))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for (x, z) in ((13, 13), (4, 13), (22, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(12, FR + 1, 5, "faction", faction="elemental")
    D.dress_walls(mt, rng, [(3, D.prop_bones), (3, D.prop_rubble), (1, D.prop_rack)], 8, depth=2, spacing=4)
    mt.finish()
    return mt


@room("hall", "lava_hall_b", faction="demon")
def lava_hall_b(name):
    """Why: the second lava hall is a demon's pit: a round lake of lava (radius 7, rows 5..7) with a crossing of iron-grate bridges to a stepped island, four
    glass-cage lava shafts in the corners, pillars of crying obsidian and hanging cages with skulls, a stair of lanterns and braziers on the rim. The ledge
    round the lake joins the four doors, so the party can fight on the rim and use the bridges as a gamble; the demons answer to the lake."""
    mt = MT("mega/hall/" + name, 3, 3)
    rng = mt.rng
    lava_shell(mt, rng)
    for side in L.SIDES:
        mt.socket(side, 13)
    c = 13
    lake = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 6)} - rect(12, 14, 12, 14)
    bridges = {(x, z) for (x, z) in lake if abs(x - c) <= 1 or abs(z - c) <= 1}
    lava_channel(mt, lake, bridges)
    for (x, z) in sorted(rect(12, 14, 12, 14)):
        mt.set(x, FR, z, L.CHISELED if (x, z) == (c, c) else L.POLISHED)
    L.statue(mt, c, FR + 1, c, facing="south", head="minecraft:wither_skeleton_skull", chest="minecraft:netherite_chestplate", legs="minecraft:netherite_leggings",
             feet="minecraft:netherite_boots", weapon="minecraft:netherite_sword")
    for dx, dz in ((-1, -1), (1, -1), (-1, 1), (1, 1)):
        D.brazier(mt, c + dx, FR + 1, c + dz, height=1, cage=False)
    for (x, z) in D.cells_between(c, c, 6.5, 7.5):          # the railing round the lake, open at the four bridges
        if abs(x - c) > 1 and abs(z - c) > 1 and mt.get(x, FR + 1, z) is None:
            mt.set(x, FR + 1, z, L.POLISHED_WALL, {"up": "true"})
    for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):      # gate posts of crying obsidian either side of every bridge mouth, on the railing line
        for lat in (-2, 2):
            px, pz = c + dx * 7 + dz * lat, c + dz * 7 + dx * lat
            D.column(mt, px, pz, FR + 1, FR + 12, shaft="minecraft:crying_obsidian")
    for (cx_, cz_) in ((3, 3), (23, 3), (3, 23), (23, 23)):
        lava_cage(mt, cx_, cz_, top=FR + 10)
    for (x, z) in D.around(c, c, 5.0, 4, 45.0):
        hanging_cage(mt, x, z, FR + 15, skull="wither_skeleton")
    for x, z in D.around(c, c, 6.0, 8, 0.0):
        D.light(mt, x, FR + 4, z, 10)
    magma_cracks(mt, rng, 26)
    for side in L.SIDES:
        for lat in (6, 20):
            f = D.inner_frame(mt, side, lat)
            if mt.get(*f.at(0, 4, 0)) is None and mt.get(*f.at(0, 5, 0)) is None:
                D.sconce(mt, f, 0, 4, skull="wither_skeleton")
    for k, (x, z) in enumerate(D.around(c, c, 9.8, 8, 0.0)):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for (x, z) in ((c, 4), (c, 22), (4, c)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(c + 1, FR + 1, c + 3, "faction", faction="demon")
    mt.marker(c, FR + 1, c - 4, "room_center")
    D.dress_walls(mt, rng, [(3, D.prop_bones), (3, D.prop_rubble)], 8, depth=2, spacing=4)
    mt.finish()
    return mt


# =====================================================================================================================
# CAVERNS (cavern/fungal_grotto_a, fungal_grotto_b, overgrown_cavern, crystal_cavern): a noise cave carved in solid rock
# =====================================================================================================================

TUFF = "minecraft:tuff"
DRIPSTONE_BLOCK = "minecraft:dripstone_block"
MOSS = "minecraft:moss_block"
WALL_DIRS = ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north"))


def cavern_mass(mt, cav, rng):
    """The solid rock of a cavern: every box cell that is not air, from row 3 (the cave floor is at least two rows thick under the deepest pool) to the ceiling
    plus three rows over each cave column, and a skirt of at least that height round it, so no cell of the cave touches the void. Cells that touch the cave
    show palette rock with patches of tuff and dripstone block; the rest is plain rock. Doorway holes are skipped (they stay open)."""
    W, Dd = mt.W, mt.D
    seed = cav.seed
    for z in range(Dd):
        for x in range(W):
            col = cav.air.get((x, z))
            t = FR + 3
            for dz in range(-3, 4):
                for dx in range(-3, 4):
                    cc = cav.air.get((x + dx, z + dz))
                    if cc is not None:
                        t = max(t, cc[-1] + 4)           # a skirt: every column is solid up to the highest ceiling within three cells
            if col is not None:
                f = cav.floor[(x, z)]
                c = col[-1] + 1
                rows = list(range(max(3, f - 2), f + 1)) + list(range(c, min(47, max(t, c + 3)) + 1))
            else:
                rows = list(range(3, min(47, t) + 1))
            for y in rows:
                if (x, y, z) in mt.reserved:
                    continue
                visible = any(cav.free(x + dx, y + dy, z + dz) for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)))
                if not visible:
                    mt.set(x, y, z, L.ROCK if rng.random() < 0.6 else L.COBBLED)
                    continue
                n = D.noise3(x / 3.5, y / 3.5, z / 3.5, seed + 5)
                if n > 0.70:
                    blk = TUFF
                elif n > 0.63 and col is not None and y >= col[-1] + 1:
                    blk = DRIPSTONE_BLOCK
                elif n > 0.47:
                    blk = L.COBBLED
                else:
                    blk = L.ROCK
                mt.set(x, y, z, blk)


def standing_cells(cav, min_d2=0.0, min_clear=4):
    """(x, y, z) of every cave floor cell you can stand on (the first free cell over the floor, with `min_clear` free rows) and its inward distance."""
    out = []
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        if cav.d2[x][z] >= min_d2 and len(col) >= min_clear:
            out.append((x, col[0], z))
    return out


def far_from_doors(cav, x, z, dist):
    """True when the cell is at least `dist` (Manhattan) from every door anchor, so a marker never sits in the way of the first steps into the room."""
    for side, off in cav.doors:
        ax, az = cav.lane_anchor(side, off)
        if abs(x - ax) + abs(z - az) < dist:
            return False
    return True


def cave_lichen(mt, cav, rng, prob, level_boost=0):
    """Glow lichen on rock faces of the cave: in a free cell next to rock, attached to every rock face it touches."""
    n = 0
    for (x, z) in cav.columns():
        for y in cav.air[(x, z)]:
            if mt.get(x, y, z) is not None or rng.random() >= prob:
                continue
            props = {}
            for dx, dz, nm in WALL_DIRS:
                c = mt.get(x + dx, y, z + dz)
                if c is not None and D.classify(c) == "solid" and not c.block.startswith("nusmp:"):
                    props[nm] = "true"
            for dy, nm in ((1, "up"), (-1, "down")):
                c = mt.get(x, y + dy, z)
                if c is not None and D.classify(c) == "solid":
                    props[nm] = "true"
            if props:
                props["waterlogged"] = "false"
                mt.set(x, y, z, "minecraft:glow_lichen", props)
                n += 1
    return n


def cave_vines(mt, cav, rng, prob, berries=0.45, max_len=8):
    """Hanging cave vines (glow berries light 14): from a ceiling cell down, body plant then the growing head, a seeded share of them with berries."""
    n = 0
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        top = col[-1]
        if rng.random() >= prob or mt.get(x, top, z) is not None:
            continue
        length = rng.randint(2, max_len)
        length = min(length, len(col) - 3)
        if length < 1 or not all(mt.get(x, top - k, z) is None for k in range(length)):
            continue
        has = "true" if rng.random() < berries else "false"
        for k in range(length - 1):
            mt.set(x, top - k, z, "minecraft:cave_vines_plant", {"berries": has})
        mt.set(x, top - length + 1, z, "minecraft:cave_vines", {"age": str(rng.randint(10, 25)), "berries": has})
        n += 1
    return n


def cave_stalactites(mt, cav, rng, prob, longest=5, guard=False):
    """Hangs pointed dripstone from the ceiling of a cave on a seeded share `prob` of the columns that are tall enough; returns how many were placed. With
    `guard` each one is kept only if the three-wide route between the doors survives it (`keep_if_route`)."""
    n = 0
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        top = col[-1]
        if rng.random() < prob and len(col) > 6:
            length = rng.randint(1, longest)
            if guard:
                ok = keep_if_route(mt, lambda: D.dripstone(mt, x, top, z, length, "down"))
            else:
                ok = D.dripstone(mt, x, top, z, length, "down")
            if ok:
                n += 1
    return n


def cave_stalagmites(mt, cav, rng, prob, longest=4, guard=False):
    """Grows pointed dripstone up from the floor on a seeded share `prob` of the columns, never on a door lane; returns how many were placed. With `guard`
    each one is kept only if the three-wide route between the doors survives it (`keep_if_route`): a stalagmite is a one-cell pillar in the way."""
    n = 0
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        if rng.random() < prob and (x, z) not in cav.lanes:
            length = rng.randint(1, longest)
            if guard:
                ok = keep_if_route(mt, lambda: D.dripstone(mt, x, col[0], z, length, "up"))
            else:
                ok = D.dripstone(mt, x, col[0], z, length, "up")
            if ok:
                n += 1
    return n


def cave_markers(mt, cav, rng, faction, count=8, patrol=3):
    """Spawn markers on open cave floor away from the doors, patrol markers on open floor too, the faction marker once. The spacing rules relax in
    stages (a small cave has little open floor) but never below: 4 free rows over the cell, 5 cells from a door, 4 cells apart."""
    need = count + patrol + 1
    chosen = []
    for (min_d2, door_gap, spacing) in ((3.0, 7, 6), (2.5, 6, 5), (2.0, 5, 4)):
        cells = [c for c in standing_cells(cav, min_d2=min_d2, min_clear=5) if far_from_doors(cav, c[0], c[2], door_gap) and mt.get(c[0], c[1], c[2]) is None
                 and mt.get(c[0], c[1] - 1, c[2]) is not None and mt.get(c[0], c[1] - 1, c[2]).block != "minecraft:water"]
        rng.shuffle(cells)
        chosen = []
        for c in cells:
            if all(abs(c[0] - o[0]) + abs(c[2] - o[2]) >= spacing for o in chosen):
                chosen.append(c)
                if len(chosen) >= need:
                    break
        if len(chosen) >= need:
            break
    if len(chosen) < 5:
        raise ValueError("%s: not enough open floor for the markers (%d cells)" % (mt.name, len(chosen)))
    spawns = max(3, len(chosen) - patrol - 1) if len(chosen) < need else count
    for k, (x, y, z) in enumerate(chosen[:spawns]):
        mt.marker(x, y, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    rest = chosen[spawns:]
    for (x, y, z) in rest[:patrol]:
        mt.marker(x, y, z, "patrol")
    x, y, z = rest[patrol] if len(rest) > patrol else rest[-1]
    mt.marker(x, y, z, "faction", faction=faction)


def plant(mt, x, y, z, block, props=None):
    """A plant block in an unset cell (never over anything else)."""
    return D.put_free(mt, x, y, z, block, props)


def cavern_open(mt, cav, rng, rock=True):
    """Shared start of every cavern: sockets are already placed; build the rock, the door tunnel floors and the pond water."""
    cavern_mass(mt, cav, rng)
    for side, off in cav.doors:
        for depth in (0, 1):
            for lat in range(off - 1, off + 2):
                x, y, z = mt._lateral(side, off, lat, depth, FR)
                mt.set(x, y, z, L.TILE)
    for (x, z) in sorted(cav.pool):
        mt.set(x, FR, z, WATER, {"level": "0"})


def ground_cover(mt, cav, rng, mix, seed_off=0, skip_lane=True):
    """Replaces the cave floor block (row f) by a ground cover chosen by smooth noise: `mix` is [(threshold, block)] tried from the top (the first whose
    threshold the noise exceeds wins; the rest keep the rock). Pond cells and door lanes are left alone."""
    for (x, z) in cav.columns():
        if (x, z) in cav.pool or (skip_lane and (x, z) in cav.lanes):
            continue
        f = cav.floor[(x, z)]
        n = D.noise3(x / 4.5, 0.3, z / 4.5, cav.seed + 211 + seed_off)
        for th, blk in mix:
            if n > th:
                mt.set(x, f, z, blk)
                break


def mushroom(mt, cav, x, z, height, radius, cap, stem, stem_props, spot, rng, spot_share=0.14):
    """A giant mushroom: a stem of `height` blocks and a three-layer cap (a brim disc of `radius`, then radius - 1, then radius - 2) with glowing spots.
    All cells must be free air of the cave and still unset, or nothing is placed. Returns the number of cells placed."""
    f = cav.floor[(x, z)]
    cells = []
    for k in range(1, height + 1):
        cells.append((x, f + k, z, stem, stem_props))
    top = f + height + 1
    for layer, r in enumerate((radius, radius - 1, max(1, radius - 2))):
        y = top + layer
        if layer == 2 and radius < 3:
            cells.append((x, y, z, cap, None))
            continue
        for (cx_, cz_) in L.disk_cells(x, z, r):
            if layer == 0 and (cx_, cz_) == (x, z):
                continue
            cells.append((cx_, y, cz_, spot if (layer < 2 and rng.random() < spot_share and (cx_, cz_) != (x, z)) else cap, None))
    for (cx_, y, cz_, b, pr) in cells:
        if not cav.free(cx_, y, cz_) or mt.get(cx_, y, cz_) is not None:
            return 0
    for (cx_, y, cz_, b, pr) in cells:
        mt.set(cx_, y, cz_, b, pr)
    return len(cells)


def route_ok(mt):
    """True when every doorway of the template under construction is still joined to the first by a three-wide route: the real test of `check_room` (a door
    and every cell within four of it count as open), run on the cells written so far."""
    an = D.Analysis(mt)
    anchors = an.anchors()
    if any(a is None for a in anchors):
        return False
    start = anchors[0]

    def near_door(n):
        return any(abs(n[0] - a[0]) + abs(n[2] - a[2]) <= 4 for a in anchors)

    wide_reach = an.reach(start, allow=lambda n: near_door(n) or an.wide(n))
    return all(a in wide_reach for a in anchors)


def keep_if_route(mt, build):
    """Why: decoration must never close the way between the doors, and the cheapest proof is to try it: `build()` places something and says whether it did;
    the result is kept only when `route_ok` still holds, otherwise every cell of the template is put back exactly as it was. Returns True when kept."""
    before = dict(mt.t.cells)
    if build() and route_ok(mt):
        return True
    mt.t.cells.clear()
    mt.t.cells.update(before)
    return False


def grow_mushrooms(mt, cav, rng, sites, kind_of, want):
    """Why: a mushroom cave is only a mushroom cave if the mushrooms are there, and a cave narrows as it rises, so a cap that was wished for at a site often does
    not fit: every site tries its wished size first and then smaller caps and shorter stems until one fits in the air that is really there. Visits `sites` in
    order until `want` mushrooms stand. `kind_of(k)` gives (cap block, stem block, stem props, spot block) for the k-th mushroom. Returns the number placed."""
    placed = 0
    blocked = set()
    for (x, z) in sites:
        if placed >= want:
            break
        if not wide_connected(cav, blocked | {(x, z)}):
            continue                                             # this stem would pinch the route between the doors (the quick model)
        cap, stem, stem_props, spot = kind_of(placed)
        wish_r, wish_h = rng.choice((3, 3, 4)), rng.randint(5, 9)

        def fit():
            for r in range(wish_r, 0, -1):
                for h in range(wish_h, 2, -1):
                    if mushroom(mt, cav, x, z, h, r, cap, stem, stem_props, spot, rng):
                        return True
            return False

        if keep_if_route(mt, fit):                               # and the real test, on the cells written so far
            placed += 1
            blocked.add((x, z))
    return placed


def wide_connected(cav, blocked):
    """Why: the three-wide route between the doors must survive the decoration, and a giant mushroom's stem is a one-cell pillar in the middle of the cave
    floor, so before each stem is placed this small model of `Analysis.wide` asks whether every door anchor is still joined to the first by cells whose whole
    3 x 3 neighbourhood is open cave floor (inside the air, not a pond, not in `blocked`, floor steps of at most one block), the cells within four of a door
    counting as open like the real test does. It is a quick pre-filter: `keep_if_route` runs the real test. Returns True when they are joined."""
    anchors = [cav.lane_anchor(side, off) for side, off in cav.doors]

    def open_cell(c):
        return c in cav.air and c not in cav.pool and c not in blocked

    def wide(c):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                n = (c[0] + dx, c[1] + dz)
                if not open_cell(n) or abs(cav.floor[n] - cav.floor[c]) > 1:
                    return False
        return True

    def near_door(c):
        return any(abs(c[0] - a[0]) + abs(c[1] - a[1]) <= 4 for a in anchors)

    start = anchors[0]
    if not open_cell(start):
        return False
    seen = {start}
    queue = [start]
    for (x, z) in queue:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            n = (x + dx, z + dz)
            if n in seen or not open_cell(n) or abs(cav.floor[n] - cav.floor[(x, z)]) > 1:
                continue
            if near_door(n) or wide(n):
                seen.add(n)
                queue.append(n)
    return all(a in seen for a in anchors)


def pick_sites(cav, rng, count, spacing, min_d2, avoid=()):
    """Spread-out floor sites (x, z) deep inside the cave, away from the door lanes: the mushrooms, the crystals, the standing stones go here."""
    cols = [c for c in cav.columns() if cav.d2[c[0]][c[1]] >= min_d2 and (c not in cav.lanes) and c not in cav.pool and far_from_doors(cav, c[0], c[1], 8)]
    rng.shuffle(cols)
    out = []
    for c in cols:
        if all(math.hypot(c[0] - o[0], c[1] - o[1]) >= spacing for o in out) and all(math.hypot(c[0] - o[0], c[1] - o[1]) >= spacing for o in avoid):
            out.append(c)
            if len(out) >= count:
                break
    return out


def hidden_lights(mt, cav, rng, spacing, level, height=7):
    """Invisible light blocks in the open air of the cave on a loose grid, `height` rows over the floor (the cave is lit like a lantern-lit cave, without
    a lantern in sight): one per `spacing` cells of footprint, only where the air is free and deep enough. Candidate columns are visited in a seeded order
    so the grid is irregular."""
    cols = [c for c in cav.columns() if cav.d2[c[0]][c[1]] >= 1.5]
    rng.shuffle(cols)
    placed = []
    n = 0
    for (x, z) in cols:
        if any(math.hypot(x - px, z - pz) < spacing for (px, pz) in placed):
            continue
        col = cav.air[(x, z)]
        y = min(col[0] + height, col[-1] - 1)
        if y > col[0] and D.light(mt, x, y, z, level):
            placed.append((x, z))
            n += 1
    return n


@room("cavern", "fungal_grotto_a", faction="insect")
def fungal_grotto_a(name):
    """Why: the grotto is the dungeon's mushroom cave: a rolling noise cave 36 x 36, doors in a pinwheel, a mycelium and podzol floor, red and brown giant
    mushrooms with glowing shroomlight spots, hanging glow berries and lichen, a pond. It is the owner's green cavern of the map in a different key: the light
    is the mushrooms', so the party sees by the thing that lives here, and the insect faction hunts between the stems."""
    mt = MT("mega/cavern/" + name, 4, 4)
    rng = mt.rng
    doors = [("n", 13), ("e", 22), ("s", 22), ("w", 13)]
    for side, off in doors:
        mt.socket(side, off)
    cav = D.Cavern(name, mt.W, mt.D, doors, [(18, 18, 11, 10), (12, 13, 7, 6), (25, 24, 8, 7)], shrink=0.42, pools=[("auto", 2.5)])
    cavern_open(mt, cav, rng)
    ground_cover(mt, cav, rng, [(0.62, "minecraft:mycelium"), (0.50, "minecraft:podzol"), (0.40, MOSS)])
    sites = pick_sites(cav, rng, 60, 3.8, 1.8)
    n_m = grow_mushrooms(mt, cav, rng, sites, lambda k: ("minecraft:red_mushroom_block" if k % 2 == 0 else "minecraft:brown_mushroom_block",
                                                         "minecraft:mushroom_stem", None, "minecraft:shroomlight"), 14)
    if n_m < 10:
        raise ValueError("%s: only %d giant mushrooms fit" % (name, n_m))
    cave_markers(mt, cav, rng, "insect")
    for (x, z) in cav.columns():
        f = cav.floor[(x, z)]
        g = mt.get(x, f, z)
        if (x, z) in cav.lanes or g is None:
            continue
        if g.block in ("minecraft:mycelium", "minecraft:podzol") and rng.random() < 0.10:
            plant(mt, x, f + 1, z, "minecraft:brown_mushroom" if rng.random() < 0.5 else "minecraft:red_mushroom")
        elif g.block == MOSS and rng.random() < 0.18:
            plant(mt, x, f + 1, z, "minecraft:moss_carpet")
    cave_vines(mt, cav, rng, 0.05)
    cave_lichen(mt, cav, rng, 0.04)
    cave_stalactites(mt, cav, rng, 0.03)
    hidden_lights(mt, cav, rng, 5.0, 11, height=3)
    mt.finish()
    return mt


@room("cavern", "fungal_grotto_b", faction="slime")
def fungal_grotto_b(name):
    """Why: the second grotto is warped: the same noise-cave engine with another seed and another layout (two chambers joined by a neck, doors in the other
    pinwheel), teal warped-wart caps on warped stems, nylium and moss on the floor, a larger pond, nether sprouts and warped roots: the green and the slime of the
    owner's map as a cold bioluminescent cave. Different blocks, different colour, different silhouette, so the two grottos are never the same room."""
    mt = MT("mega/cavern/" + name, 4, 4)
    rng = mt.rng
    doors = [("n", 22), ("e", 13), ("s", 13), ("w", 22)]
    for side, off in doors:
        mt.socket(side, off)
    cav = D.Cavern(name, mt.W, mt.D, doors, [(11, 12, 8, 8), (25, 24, 9, 8), (18, 18, 6, 5)], shrink=0.40, pools=[("auto", 3.0)])
    cavern_open(mt, cav, rng)
    ground_cover(mt, cav, rng, [(0.60, "minecraft:warped_nylium"), (0.48, MOSS)])
    sites = pick_sites(cav, rng, 60, 3.8, 1.8)
    n_m = grow_mushrooms(mt, cav, rng, sites, lambda k: ("minecraft:warped_wart_block", "minecraft:warped_stem", {"axis": "y"}, "minecraft:shroomlight"), 14)
    if n_m < 10:
        raise ValueError("%s: only %d giant warped fungi fit" % (name, n_m))
    cave_markers(mt, cav, rng, "slime")
    for (x, z) in cav.columns():
        f = cav.floor[(x, z)]
        g = mt.get(x, f, z)
        if (x, z) in cav.lanes or g is None:
            continue
        if g.block == "minecraft:warped_nylium" and rng.random() < 0.16:
            plant(mt, x, f + 1, z, rng.choice(("minecraft:warped_roots", "minecraft:nether_sprouts", "minecraft:warped_fungus")))
        elif g.block == MOSS and rng.random() < 0.14:
            plant(mt, x, f + 1, z, "minecraft:moss_carpet")
    cave_vines(mt, cav, rng, 0.05, berries=0.3)
    cave_lichen(mt, cav, rng, 0.04)
    cave_stalactites(mt, cav, rng, 0.03)
    hidden_lights(mt, cav, rng, 5.0, 11, height=3)
    mt.finish()
    return mt


def azalea_bush(mt, x, y, z, flowering):
    """An azalea (flowering or not) in an unset cell."""
    return plant(mt, x, y, z, "minecraft:flowering_azalea" if flowering else "minecraft:azalea")


def wall_vines(mt, cav, rng, prob):
    """Plain vines on the rock faces (the cave's 'leaves-free' vines): a column of vine blocks, each attached to the same rock face, hanging from a seeded
    height down a few cells."""
    n = 0
    for (x, z) in cav.columns():
        for y in cav.air[(x, z)]:
            if rng.random() >= prob or mt.get(x, y, z) is not None:
                continue
            for dx, dz, nm in WALL_DIRS:
                c = mt.get(x + dx, y, z + dz)
                if c is not None and D.classify(c) == "solid" and not c.block.startswith("nusmp:"):
                    length = rng.randint(2, 6)
                    for k in range(length):
                        cc = mt.get(x + dx, y - k, z + dz)
                        if mt.get(x, y - k, z) is not None or cc is None or D.classify(cc) != "solid":
                            break
                        mt.set(x, y - k, z, "minecraft:vine", {nm: "true"})
                    n += 1
                    break
    return n


@room("cavern", "overgrown_cavern", faction="beast")
def overgrown_cavern(name):
    """Why: the owner's tabletop map has one green cavern in the middle of its rooms and corridors, overgrown and bright against the grey; this is it: the
    biggest cave of the set (45 x 36, ceilings to row 40), a moss floor with carpets, azaleas and ferns, a pond, glow berries and spore blossoms hanging from the
    dome, vines on the walls and light that seems to come from the plants (hidden light blocks under the canopy of vines). The beasts den here."""
    mt = MT("mega/cavern/" + name, 5, 4)
    rng = mt.rng
    doors = [("n", 22), ("s", 22), ("w", 13), ("e", 22)]
    for side, off in doors:
        mt.socket(side, off)
    cav = D.Cavern(name, mt.W, mt.D, doors, [(22, 18, 17, 12), (12, 12, 8, 7), (34, 25, 9, 7)], shrink=0.30, flat=4.0, floor_amp=2.4, pools=[("auto", 4.0)])
    cavern_open(mt, cav, rng)
    ground_cover(mt, cav, rng, [(0.40, MOSS), (0.32, "minecraft:podzol")])
    cave_markers(mt, cav, rng, "beast", count=10)
    for (x, z) in cav.columns():
        f = cav.floor[(x, z)]
        g = mt.get(x, f, z)
        if (x, z) in cav.lanes or g is None or (x, z) in cav.pool:
            continue
        r = rng.random()
        if g.block == MOSS:
            if r < 0.20:
                plant(mt, x, f + 1, z, "minecraft:moss_carpet")
            elif r < 0.25:
                azalea_bush(mt, x, f + 1, z, rng.random() < 0.5)
            elif r < 0.33:
                plant(mt, x, f + 1, z, "minecraft:short_grass")
            elif r < 0.36:
                plant(mt, x, f + 1, z, "minecraft:fern")
        elif g.block == "minecraft:podzol" and r < 0.10:
            plant(mt, x, f + 1, z, rng.choice(("minecraft:brown_mushroom", "minecraft:red_mushroom")))
    for (x, z) in cav.pool:
        if rng.random() < 0.25:
            plant(mt, x, FR + 1, z, "minecraft:lily_pad")
    cave_vines(mt, cav, rng, 0.10, berries=0.55, max_len=10)
    wall_vines(mt, cav, rng, 0.05)
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        top = col[-1]
        r = rng.random()
        if r < 0.015 and mt.get(x, top, z) is None:
            mt.set(x, top, z, "minecraft:spore_blossom")
        elif r < 0.05 and mt.get(x, top, z) is None:
            mt.set(x, top, z, "minecraft:hanging_roots", {"waterlogged": "false"})
    cave_lichen(mt, cav, rng, 0.05)
    cave_stalactites(mt, cav, rng, 0.02)
    hidden_lights(mt, cav, rng, 6.0, 11, height=3)
    hidden_lights(mt, cav, rng, 9.0, 12, height=14)
    mt.finish()
    return mt


def crystal_spires(mt, cav, rng, want):
    """Why: the crystal cavern is the geode, so its floor needs the big crystals a geode has: columns of amethyst blocks (every second one budding, so they
    grow more) three to eight high with clusters on every side and a cluster for a tip. Each is shrunk to the air the cave has there and none may pinch the
    route between the doors (`wide_connected`). Returns how many stand."""
    sites = pick_sites(cav, rng, 60, 3.4, 1.5)
    blocked = set()
    placed = 0
    for (x, z) in sites:
        if placed >= want:
            break
        if not wide_connected(cav, blocked | {(x, z)}):
            continue
        f = cav.floor[(x, z)]
        h = rng.randint(3, 8)
        while h >= 3 and not all(cav.free(x, f + k, z) and mt.get(x, f + k, z) is None for k in range(1, h + 2)):
            h -= 1
        if h < 3:
            continue

        def raise_spire():
            for k in range(1, h + 1):
                mt.set(x, f + k, z, "minecraft:budding_amethyst" if k % 2 == 0 else "minecraft:amethyst_block")
            for k in range(1, h + 1):
                for dx, dz, face in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
                    if rng.random() < 0.6:
                        D.put_free(mt, x + dx, f + k, z + dz, "minecraft:large_amethyst_bud" if k <= 2 else "minecraft:amethyst_cluster",
                                   {"facing": face, "waterlogged": "false"})
            D.put_free(mt, x, f + h + 1, z, "minecraft:amethyst_cluster", {"facing": "up", "waterlogged": "false"})
            return True

        if keep_if_route(mt, raise_spire):
            blocked.add((x, z))
            placed += 1
    return placed


def crystal_stalactites(mt, cav, rng, want):
    """Hanging crystals: amethyst blocks growing down from the ceiling (two to five long, ending at least three blocks over the floor) with a cluster for a
    tip, the geode's answer to the dripstone. Returns how many hang."""
    cols = [c for c in cav.columns() if cav.d2[c[0]][c[1]] >= 2.0 and c not in cav.lanes]
    rng.shuffle(cols)
    placed = 0
    chosen = []
    for (x, z) in cols:
        if placed >= want:
            break
        if any(math.hypot(x - ox, z - oz) < 4.5 for (ox, oz) in chosen):
            continue
        col = cav.air[(x, z)]
        top = col[-1]
        length = rng.randint(2, 5)
        if top - length - 1 < col[0] + 3:
            continue
        cells = [(x, top - k, z) for k in range(length)]
        if not all(mt.get(*c_) is None for c_ in cells) or mt.get(x, top + 1, z) is None or D.classify(mt.get(x, top + 1, z)) != "solid":
            continue
        for k, c_ in enumerate(cells):
            mt.set(*c_, "minecraft:budding_amethyst" if k % 2 else "minecraft:amethyst_block")
        D.put_free(mt, x, top - length, z, "minecraft:amethyst_cluster", {"facing": "down", "waterlogged": "false"})
        for dx, dz, face in ((1, 0, "east"), (-1, 0, "west"), (0, 1, "south"), (0, -1, "north")):
            if rng.random() < 0.4:
                D.put_free(mt, x + dx, top - length + 1, z + dz, "minecraft:medium_amethyst_bud", {"facing": face, "waterlogged": "false"})
        chosen.append((x, z))
        placed += 1
    return placed


@room("cavern", "crystal_cavern", faction="insect")
def crystal_cavern(name):
    """Why: the crystal cavern is the dungeon's treasure geode: a 27 x 27 cave whose walls are budding amethyst, amethyst and calcite, with clusters growing
    out of every face (clusters give off light: 5, the large bud 4, the medium 2, the small 1) and a few invisible light blocks so the party sees the glitter
    without a torch. The floor is calcite and tuff patches; crystals are scenery, never in the way (they stand on the cave floor between the lanes)."""
    mt = MT("mega/cavern/" + name, 3, 3)
    rng = mt.rng
    doors = [("n", 13), ("s", 13), ("e", 13)]
    for side, off in doors:
        mt.socket(side, off)
    cav = D.Cavern(name, mt.W, mt.D, doors, [(13, 13, 7, 7), (9, 17, 4.5, 4.5), (18, 9, 4.5, 4.5)], shrink=0.44, flat=3.0, floor_amp=1.6, margin=3)
    cavern_open(mt, cav, rng)
    ground_cover(mt, cav, rng, [(0.60, "minecraft:calcite"), (0.46, TUFF)])
    cave_markers(mt, cav, rng, "insect", count=7)
    # the walls: every rock cell that touches the cave may become amethyst, budding amethyst or calcite (smooth noise, so it comes in veins)
    for (x, z) in cav.columns():
        for y in cav.air[(x, z)]:
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                p_ = (x + dx, y + dy, z + dz)
                c = mt.get(*p_)
                if c is None or c.block not in (L.ROCK, L.COBBLED, TUFF, DRIPSTONE_BLOCK) or p_ in mt.reserved:
                    continue
                if dy == -1 and (x, z) in cav.lanes:
                    continue
                n = D.noise3(p_[0] / 2.5, p_[1] / 2.5, p_[2] / 2.5, cav.seed + 77)
                if n > 0.66:
                    mt.set(*p_, "minecraft:budding_amethyst" if rng.random() < 0.4 else "minecraft:amethyst_block")
                elif n > 0.58:
                    mt.set(*p_, "minecraft:calcite")
    # clusters grow out of the amethyst and the rock, away from the face they stand on
    sizes = (("minecraft:small_amethyst_bud", 0.3), ("minecraft:medium_amethyst_bud", 0.25), ("minecraft:large_amethyst_bud", 0.2), ("minecraft:amethyst_cluster", 0.25))
    for (x, z) in cav.columns():
        for y in cav.air[(x, z)]:
            if mt.get(x, y, z) is not None or rng.random() > 0.095:
                continue
            for dx, dy, dz, face in ((0, -1, 0, "up"), (0, 1, 0, "down"), (1, 0, 0, "west"), (-1, 0, 0, "east"), (0, 0, 1, "north"), (0, 0, -1, "south")):
                c = mt.get(x + dx, y + dy, z + dz)
                if c is None or D.classify(c) != "solid" or c.block.startswith("nusmp:"):
                    continue
                if dy == -1 and ((x, z) in cav.lanes or mt.get(x, y - 1, z).block in ("minecraft:water",)):
                    continue
                blk = rng.choices([b for b, _ in sizes], weights=[w for _, w in sizes])[0]
                mt.set(x, y, z, blk, {"facing": face, "waterlogged": "false"})
                break
    n_up = crystal_spires(mt, cav, rng, 9)
    n_down = crystal_stalactites(mt, cav, rng, 9)
    if n_up < 5 or n_down < 5:
        raise ValueError("%s: only %d crystal spires and %d hanging crystals fit" % (name, n_up, n_down))
    cave_stalactites(mt, cav, rng, 0.03, longest=4, guard=True)
    cave_stalagmites(mt, cav, rng, 0.01, longest=3, guard=True)
    hidden_lights(mt, cav, rng, 4.5, 12, height=3)
    hidden_lights(mt, cav, rng, 8.0, 11, height=12)
    mt.finish()
    return mt


# =====================================================================================================================
# MIRROR HALL (hall/mirror_hall): 2x3, a symmetrical hall of dark mirrors and doors that lead nowhere
# =====================================================================================================================

def mirror_portal(mt, x, z, facing):
    """A decorative snake portal (`mega_lib.snake_portal`) on a long wall, with its hidden `light` block swapped for a sea lantern: the outer wall layer is the
    template boundary, and an invisible block there would let the portal look out into the void. (x, z) is the portal's cell in the inner wall layer."""
    L.snake_portal(mt, x, FR + 1, z, facing=facing, w=7, h=9)
    f = Frame(mt, x, FR + 1, z, facing)
    mt.set(*f.at(0, 9 // 2, -1), "minecraft:sea_lantern")


@room("hall", "mirror_hall", faction="cultist")
def mirror_hall(name):
    """Why: the owner's brief asks for snake portals (lightning rods, trapdoors, glass) as 'doors that lead nowhere'. This hall is their stage: two portals on
    each long wall, a dark mirror (tinted glass in a polished frame) between them and two on the end walls, a strip of the same dark glass down the floor and
    again in the ceiling over a row of sea lanterns, end rods hanging over the pillar lines, and a perfect left-right symmetry so that the hall seems to
    repeat itself. The four real doors are at the ends (two on each, either side of the central mirror); the party that follows a portal finds a wall."""
    mt = MT("mega/hall/" + name, 2, 3)
    rng = mt.rng
    mt.room(15, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    for off in (4, 13):
        mt.socket("n", off)
        mt.socket("s", off)
    W, Dd = mt.W, mt.D
    TINT = "minecraft:tinted_glass"
    for z in range(2, Dd - 2):
        for x in range(2, W - 2):
            if 8 <= x <= 9 and 3 <= z <= Dd - 4:
                mt.set(x, FR, z, TINT)
            elif x in (7, 10):
                mt.set(x, FR, z, L.POLISHED)
            elif 2 <= x <= 5 or 12 <= x <= 15:
                mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            elif rng.random() < 0.1:
                mt.set(x, FR, z, L.CRACKED_TILE)
    for z in (7, 19):
        for x in (7, 8, 9, 10):
            D.put(mt, x, FR, z, L.CHISELED)
    # the ceiling: a strip of dark glass over sea lanterns
    for z in range(3, Dd - 3):
        for x in range(7, 11):
            mt.set(x, FR + 16, z, TINT)
            mt.set(x, FR + 17, z, "minecraft:sea_lantern")
    for z in (7, 19):
        mirror_portal(mt, 1, z, "east")
        mirror_portal(mt, W - 2, z, "west")
    for side in ("w", "e"):
        for lat in (11, 15):
            f = D.inner_frame(mt, side, lat)
            for u in range(0, 10):
                mt.set(*f.at(0, u, -1), L.POLISHED)
            mt.set(*f.at(0, 0, -1), L.CHISELED)
        f = D.inner_frame(mt, side, 13)
        D.lit_window(mt, f, width=3, y0=2, height=7, glass=TINT, backing=L.BRICK)
        for r in (-1, 0, 1):
            mt.set(*f.at(r, 1, -1), L.CHISELED)
            mt.set(*f.at(r, 9, -1), L.CHISELED)
    for side in ("n", "s"):
        f = D.inner_frame(mt, side, 9)
        for r in (-1, 0):
            for u in range(1, 8):
                mt.set(*f.at(r, u, -1), TINT)
        for r in (-2, 1):
            for u in range(0, 9):
                mt.set(*f.at(r, u, -1), L.POLISHED)
    for (px, pz) in ((6, 6), (11, 6), (6, 13), (11, 13), (6, 20), (11, 20)):
        D.column(mt, px, pz, FR + 1, FR + 13, wall_shaft=True)
        D.pillar_lantern(mt, px, pz, "east" if px == 6 else "west", FR + 3, lantern=L.SOUL_LANTERN)
    for z in (4, 9, 13, 17, 22):                              # landing lights: end rods standing along both edges of the dark glass strip
        for x in (7, 10):
            D.put_free(mt, x, FR + 1, z, "minecraft:end_rod", {"facing": "up"})
    for (x, z) in ((6, 4), (11, 4), (6, 9), (11, 9), (6, 11), (11, 11), (6, 15), (11, 15), (6, 17), (11, 17), (6, 22), (11, 22)):
        D.put_free(mt, x, FR + 15, z, "minecraft:end_rod", {"facing": "down"})
    for z in (4, 22):
        for x in (3, 14):
            D.hang_lantern(mt, x, FR + 16, z, drop=3, lantern=L.SOUL_LANTERN)
    for side in ("w", "e"):
        for lat in (11, 15):                                   # on the polished posts beside the window: a wall torch needs a solid block behind it, the portals' fins are not
            D.sconce(mt, D.inner_frame(mt, side, lat), 0, 4, skull="wither_skeleton")
    for k, (x, z) in enumerate(((3, 6), (14, 6), (3, 20), (14, 20), (8, 10), (9, 16))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 3 == 0 else {}))
    for z in (6, 13, 20):
        mt.marker(8 if z != 13 else 9, FR + 1, z, "patrol")
    mt.marker(8, FR + 1, 23, "faction", faction="cultist")
    mt.finish()
    return mt


@room("hall", "colonnade_hall_b", faction="construct")
def colonnade_hall_b(name):
    """Why: the second colonnade hall is the first one's opposite: a reflecting pool runs down the nave (water two blocks deep, sealed in a basin, with a
    bridge at the side-door row and landing steps at both ends), the pillars carry free-standing pointed arches (no arcade wall), the whole hall is covered by
    a ribbed barrel vault, armoured statues stand in niches, and the raised end is at the south with a stair-built high seat between two braziers and banners.
    The party fights on the aisles with the water between them (the constructs wade out of the niches)."""
    mt = MT("mega/hall/" + name, 2, 5)
    rng = mt.rng
    W, Dd = mt.W, mt.D
    top_row = FR + 17                      # last interior row 25
    mt.room(17, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE)
    for lat in (13,):
        mt.socket("n", lat)
    mt.socket("s", 4)
    mt.socket("w", 13)
    mt.socket("w", 31)
    mt.socket("e", 22)
    # the basin under the nave: a solid slab (rows 3..7) with the pool carved out of it
    for z in range(Dd):
        for x in range(W):
            for y in range(FR - 5, FR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.ROCK, L.BRICK)))
    pool = rect(7, 10, 7, 37)
    bridge = rect(7, 10, 21, 23)
    for (x, z) in sorted(pool):
        mt.remove(x, FR, z)
        for y in (FR - 2, FR - 1):
            mt.set(x, y, z, WATER, {"level": "0"})
        if (x, z) in bridge:
            mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
    landing_steps(mt, (8, 6), (0, 1))
    landing_steps(mt, (9, 6), (0, 1))
    landing_steps(mt, (8, 38), (0, -1))
    landing_steps(mt, (9, 38), (0, -1))
    # the floor: aisles checkered, ends patterned
    for z in range(2, Dd - 2):
        for x in range(2, W - 2):
            if (x, z) in pool:
                continue
            if x in (7, 8, 9, 10):
                mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            else:
                mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else (L.CRACKED_TILE if rng.random() < 0.1 else L.TILE))
    # the barrel vault: solid over a profile from the wall tops (row 22) to the apex (row 25), ribs hanging at the pillar rows
    def vault_row(x):
        """The first solid row of the barrel vault over the column x: a half ellipse across the nave."""
        a = abs(x - 8.5)
        return FR + 14 + int(3 * math.sqrt(max(0.0, 1.0 - (a / 7.3) ** 2)) + 0.5)
    pillars = list(range(6, 39, 4))
    for z in range(2, Dd - 2):
        for x in range(2, W - 2):
            v = vault_row(x)
            for y in range(v, top_row + 1):
                mt.set(x, y, z, L.CRACKED_BRICK if rng.random() < 0.06 else L.BRICK)
            if z in pillars:
                mt.set(x, v - 1, z, L.POLISHED if x not in (8, 9) else L.CHISELED)
    # pillars with free arches between them, lanterns on corbels
    for px in (6, 11):
        for pz in pillars:
            v = vault_row(px)
            D.column(mt, px, pz, FR + 1, v - 2)
            mt.set(px, FR + 7, pz, L.CHISELED)
        for z0, z1 in zip(pillars, pillars[1:]):
            zc = (z0 + z1) // 2
            D.arch(mt, Frame(mt, px, FR + 1, zc, "east"), 3, 6, depth=1, carve=False)
            D.hang_lantern(mt, px + (1 if px == 6 else -1), vault_row(px + (1 if px == 6 else -1)), zc, drop=8)
    # banners on the pillars, a carpet over the bridge
    for i, pz in enumerate(pillars):
        col = "blue" if i % 2 == 0 else "cyan"
        D.banner_on(mt, 7, FR + 10, pz, "east", col)
        D.banner_on(mt, 10, FR + 10, pz, "west", col)
    for z in (21, 22, 23):
        for x in (8, 9):
            D.put_free(mt, x, FR + 1, z, "minecraft:blue_carpet")
    # the raised end (south): a two-step dais, a high seat of stairs with armrests, banners and braziers
    for z in range(39, 43):
        for x in range(6, 12):
            for y in range(FR + 1, FR + 1 + (2 if z >= 41 else 1)):
                mt.set(x, y, z, L.POLISHED if y == FR + (2 if z >= 41 else 1) else L.BRICK)
    for x in range(6, 12):
        mt.set(x, FR + 1, 38, L.POLISHED_STAIRS, {"facing": "south", "half": "bottom"})
        mt.set(x, FR + 2, 40, L.POLISHED_STAIRS, {"facing": "south", "half": "bottom"})
    for x in (8, 9):
        mt.set(x, FR + 3, 42, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
        for y in (FR + 3, FR + 4, FR + 5):
            D.put_free(mt, x, y, 43, L.CHISELED)
        D.banner_on(mt, x, FR + 5, 42, "north", "blue")
    for x in (7, 10):
        mt.set(x, FR + 3, 42, L.POLISHED_SLAB, {"type": "bottom"})
    for x in (6, 11):
        D.brazier(mt, x, FR + 3, 42, cage=True)
    D.lit_window(mt, D.inner_frame(mt, "s", 9), width=2, y0=7, height=9, glass="minecraft:blue_stained_glass", backing="minecraft:sea_lantern")
    D.lit_window(mt, D.inner_frame(mt, "n", 9), width=2, y0=7, height=9, glass="minecraft:cyan_stained_glass", backing="minecraft:sea_lantern")
    # the outer walls: niches with armoured statues or candles between pilasters at the pillar rows
    gear = (("iron", "iron_sword"), ("diamond", "diamond_sword"), ("netherite", "netherite_sword"))
    n_stat = 0
    for side, face in (("w", "east"), ("e", "west")):
        for pz in pillars:
            if not D.wall_free(mt, side, pz, 0):
                continue
            f = D.inner_frame(mt, side, pz)
            x, y, z = f.at(0, 0, 0)
            D.pilaster(mt, x, z, FR + 1, FR + 13, face)
        for i, zc in enumerate([c_ for c_ in (8, 12, 16, 20, 24, 28, 32, 36, 40) if D.wall_free(mt, side, c_, 2)]):
            f = D.inner_frame(mt, side, zc)
            D.arched_alcove(mt, f, 3, 5)
            if (i + (side == "e")) % 2 == 0 and n_stat < 8:
                D.alcove_statue(mt, f, gear[n_stat % 3])
                n_stat += 1
            else:
                D.alcove_candles(mt, f, rng, colors=("light_blue", "white"))
    for k, (x, z) in enumerate(((3, 12), (4, 20), (13, 28), (14, 36), (3, 36), (14, 12), (14, 40), (3, 8))):
        mt.marker(x, FR + 1, z, "spawn", **({"weight": 2} if k % 4 == 0 else {}))
    for z in (10, 22, 34):
        mt.marker(13, FR + 1, z, "patrol")
    mt.marker(4, FR + 1, 40, "faction", faction="construct")
    D.dress_walls(mt, rng, [(3, D.prop_rubble), (2, D.prop_rack), (1, D.prop_candles)], 6, spacing=5)
    mt.finish()
    return mt


@room("hall", "round_hall_b", faction="cultist")
def round_hall_b(name):
    """Why: the second round hall is the first one's fire: instead of a raised dais with statues, a sunken fire pit (radius 4, two blocks deep) in the middle with
    a lava cauldron, campfires, soul campfires and candles in it and a flight of stairs down on each side, so a fight happens round the pit and the party can
    use the pit as a refuge or a trap. Eight pillars joined by a ring beam carry banners between them; the dome has an oculus of sea lanterns."""
    mt, cx, cz, R, top, base, dome_h, room_fn = build_round_hall(name, "b")
    rng = mt.rng
    # solid under the pit, then the pit itself: floor removed down to row 6 inside radius 4.5
    for z in range(mt.D):
        for x in range(mt.W):
            if room_fn(x, z):
                for y in range(FR - 4, FR):
                    mt.set(x, y, z, L.ROCK)
    pit = {(x, z) for z in range(mt.D) for x in range(mt.W) if D.in_disc(x, z, cx, cz, 3)}
    for (x, z) in sorted(pit):
        mt.remove(x, FR, z)
        mt.remove(x, FR - 1, z)
        mt.set(x, FR - 2, z, L.TILE if (x + z) % 2 else L.POLISHED)
    for side, (dx, dz) in L.STEP.items():
        for lat in (-1, 0, 1):
            lx, lz = cx + dz * lat, cz + dx * lat
            a = (lx + dx * 3, lz + dz * 3)
            b = (lx + dx * 4, lz + dz * 4)
            mt.set(a[0], FR - 1, a[1], L.POLISHED_STAIRS, {"facing": side, "half": "bottom"})
            mt.set(b[0], FR, b[1], L.POLISHED_STAIRS, {"facing": side, "half": "bottom"})
    mt.set(cx, FR - 1, cz, "minecraft:lava_cauldron")
    for dx, dz in ((2, 2), (-2, 2), (2, -2), (-2, -2)):
        mt.set(cx + dx, FR - 1, cz + dz, "minecraft:soul_campfire", {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    for k in range(8):
        x, z = D.polar(cx, cz, 2.1, 45.0 * k + 22.5)
        D.candle_cluster(mt, x, FR - 1, z, rng, colors=("red", "orange", "yellow"))
    # the rim: a railing of polished walls on the ring between the stairs, eight pillars with a ring beam, banners between them
    for (x, z) in D.cells_between(cx, cz, 3.5, 4.5):
        c_ = mt.get(x, FR, z)
        if c_ is not None and c_.block != L.POLISHED_STAIRS and mt.get(x, FR + 1, z) is None:
            mt.set(x, FR + 1, z, L.POLISHED_WALL, {"up": "true"})
    pill = D.around(cx, cz, 9, 8, 22.5)
    for (x, z) in pill:
        D.column(mt, x, z, FR + 1, top - 1)
    ring_beam(mt, cx, cz, 8.2, 9.8, top, pill)
    for (x, z) in pill:
        d_ = D.dist(x, z, cx, cz)
        ix, iz = D.polar(cx, cz, d_ - 1.4, D.angle_of(x, z, cx, cz))
        D.hang_lantern(mt, ix, top, iz, drop=2, lantern=L.SOUL_LANTERN)
    dress_round_wall(mt, cx, cz, R, room_fn, top, rng, "b")
    for (x, z) in D.cells_between(cx, cz, 0, 1.6):
        y0 = dome_row(D.dist(x, z, cx, cz), R + 0.5, base, dome_h)
        for y in (y0, y0 + 1):
            mt.set(x, y, z, "minecraft:sea_lantern")
    markers_round(mt, cx, cz, "cultist")
    mt.finish()
    return mt


# =====================================================================================================================
# checks and output
# =====================================================================================================================

def read_markers(mt):
    """[(kind, args, (x, y, z))] of every data marker of the template."""
    out = []
    for pos, c in sorted(mt.t.cells.items()):
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            meta = str(c.nbt.get("metadata", ""))
            parts = meta.split(";")
            args = {}
            for a in parts[1:]:
                if "=" in a:
                    k, v = a.split("=", 1)
                    args[k] = v
            out.append((parts[0][6:], args, pos))
    return out


def check_room(mt, spec, verbose=True):
    """Runs the analysis on a finished template (see mega_decor_r2.Analysis) and returns (errors, warnings, info lines).
    spec keys: faction (str) the native faction marker expected; dark (bool) the room is meant to be dark; unreachable_spawn_ok (bool);
    route_width ('wide' default | 'narrow' | 'none'); no_spawn (bool: trial and puzzle halls carry no spawn markers); max_armor (default 16)."""
    errors, warns, info = [], [], []
    an = D.Analysis(mt)
    anchors = an.anchors()
    for d, a in zip(an.doors, anchors):
        if a is None:
            errors.append("socket %s@%d: no standing cell inside the doorway (anchor %s)" % (d["side"], d["offset"], d["anchor"]))
    live = [a for a in anchors if a is not None]
    reach = {}
    if live:
        reach = an.reach(live[0])
        for d, a in zip(an.doors, anchors):
            if a is not None and a not in reach:
                errors.append("socket %s@%d cannot be reached from socket %s@%d" % (d["side"], d["offset"], an.doors[0]["side"], an.doors[0]["offset"]))
        info.append("walkable nodes from the first door: %d" % len(reach))
        width = spec.get("route_width", "wide")
        if width != "none" and len(live) > 1:
            def near_door(n):
                """True when the node is within four cells of a doorway anchor (the 3-wide test does not apply to the doorway tunnels themselves)."""
                return any(abs(n[0] - a[0]) + abs(n[2] - a[2]) <= 4 for a in live)
            wide_reach = an.reach(live[0], allow=lambda n: near_door(n) or an.wide(n))
            lost = [d["side"] + str(d["offset"]) for d, a in zip(an.doors, anchors) if a is not None and a not in wide_reach]
            if lost:
                (errors if width == "wide" else warns).append("no 3-wide route from %s%d to %s" % (an.doors[0]["side"], an.doors[0]["offset"], ", ".join(lost)))
            else:
                info.append("3-wide routes between all %d doors" % len(live))
    # markers
    kinds = {}
    for kind, args, pos in read_markers(mt):
        kinds[kind] = kinds.get(kind, 0) + 1
        if kind in ("spawn", "patrol", "room_center"):
            if not an.standable(*pos):
                errors.append("%s marker at %s is not on a floor you can stand on" % (kind, pos))
            elif live and pos not in reach and not spec.get("unreachable_spawn_ok"):
                errors.append("%s marker at %s cannot be reached from the doors" % (kind, pos))
        if kind == "faction" and spec.get("faction") and args.get("faction") != spec["faction"]:
            errors.append("faction marker %r differs from the spec %r" % (args.get("faction"), spec["faction"]))
    info.append("markers: " + ", ".join("%s=%d" % kv for kv in sorted(kinds.items())))
    if spec.get("no_spawn") and kinds.get("spawn"):
        errors.append("this room must carry no spawn markers (its mechanic spawns its own mobs)")
    if not spec.get("no_spawn") and not (4 <= kinds.get("spawn", 0) <= 12) and not spec.get("few_spawns"):
        warns.append("%d spawn markers (4-12 expected)" % kinds.get("spawn", 0))
    if kinds.get("faction", 0) != 1:
        errors.append("exactly one faction marker expected, found %d" % kinds.get("faction", 0))
    # armor stands
    stands = sum(1 for e in mt.t.entities if str(e["nbt"].get("id", "")) == "minecraft:armor_stand")
    if stands > spec.get("max_armor", 16):
        errors.append("%d armor stands (at most 16 per room)" % stands)
    info.append("armor stands: %d" % stands)
    # light
    if not spec.get("dark"):
        lv = an.light_map()
        hole = set()
        for d in an.doors:
            hole.update(d["cells"])
        dark = [n for n in reach if lv.get((n[0], n[1] + 1, n[2]), 0) <= 4 and n not in hole]
        pct = 100.0 * len(dark) / max(1, len(reach))
        info.append("light: %d of %d reachable standing cells (%.1f%%) are at or under the gloom limit (block light 4)" % (len(dark), len(reach), pct))
        limit = spec.get("dark_limit", 12.0)
        if pct > limit:
            warns.append("%.1f%% of the floor is dark (limit %.0f%%), e.g. %s" % (pct, limit, sorted(dark)[:3]))
    # leaks (sight into the void) and fluids
    if live:
        leaks, _seen = an.leaks([live[0]])
        if leaks:
            errors.append("the shell leaks: %d boundary cells can be seen from inside, e.g. %s" % (len(leaks), sorted(set(leaks))[:4]))
    for p in an.fluid_problems()[:6]:
        errors.append("fluid: " + p)
    flam = an.flammable_near_lava()
    if flam:
        errors.append("%d flammable blocks within 3 of lava, e.g. %s" % (len(flam), sorted(flam.items())[:3]))
    held = an.attachment_problems()
    for sev, pos, block, text in held[:6]:
        (errors if sev == "error" else warns).append("attachment: %s at %s: %s" % (block.replace("minecraft:", ""), pos, text))
    if len(held) > 6:
        errors.append("... and %d more blocks that nothing holds" % (len(held) - 6))
    kind = spec.get("kind")
    if kind == "pit":
        errors.extend(check_pit(mt, an, spec))
    elif kind == "puzzle":
        errors.extend(check_puzzle(mt, spec))
    elif kind == "trial":
        errors.extend(check_trial(mt, an, spec))
    elif kind == "dark":
        errors.extend(check_dark(mt, an, spec))
    return errors, warns, info


# ---- checks that belong to one kind of room (the mechanic each template carries)

def _cells_of(mt, block):
    """The (position, cell) pairs of one block id, sorted so the checks are deterministic."""
    return [(pos, c) for pos, c in sorted(mt.t.cells.items()) if c.block == block]


def check_pit(mt, an, spec):
    """The pit halls: a safe route of tiles from the ledge to the chest, most of the field is pits, every pit has a chamber below it that is sealed, its free
    cells all reach the ladder, and there are spikes (the Spire's own `pit_problems`, on the 9-grid)."""
    errs = []
    tiles = {(x, z): c.props.get("safe") == "true" for (x, y, z), c in _cells_of(mt, PITFALL)}
    if not tiles:
        return ["no pitfall tiles"]
    unsafe = sum(1 for v in tiles.values() if not v)
    if unsafe < 0.55 * len(tiles):
        errs.append("only %d of %d pitfall tiles are pits" % (unsafe, len(tiles)))
    chests = [pos for pos, c in _cells_of(mt, "minecraft:chest")]
    if len(chests) != 1:
        return errs + ["expected exactly one chest, found %d" % len(chests)]
    cx_, cy_, cz_ = chests[0]
    # the safe route: 4-connected safe tiles from a tile that touches the ledge to the tile under the chest
    start = [t for t in tiles if tiles[t] and any((t[0] + dx, t[1] + dz) not in tiles and an.standable(t[0] + dx, FR + 1, t[1] + dz)
                                                  for dx, dz in L.STEP.values())]
    seen = set(start)
    stack = list(start)
    while stack:
        x, z = stack.pop()
        for dx, dz in L.STEP.values():
            n = (x + dx, z + dz)
            if tiles.get(n) and n not in seen:
                seen.add(n)
                stack.append(n)
    if (cx_, cz_) not in seen:
        errs.append("no route of safe tiles from the ledge to the chest")
    # the chamber under the field: its free cells (rows 6..7) all reach the foot of the ladder; spikes present
    ladders = [pos for pos, c in _cells_of(mt, "minecraft:ladder")]
    spikes = [pos for pos, c in _cells_of(mt, "minecraft:pointed_dripstone") if pos[1] == FR - 2]
    if len(spikes) < 10:
        errs.append("only %d spikes in the chamber" % len(spikes))
    if not ladders:
        errs.append("no ladder out of the chamber")
    else:
        ladder_cols = {(l[0], l[2]) for l in ladders}

        def open_cell(q):
            """A cell of the spiked chamber that a person could be in (empty, or a ladder)."""
            c = mt.get(*q)
            return q[1] in (FR - 2, FR - 1) and (c is None or _path_of(c) == "ladder")

        low = min(ladders, key=lambda q: q[1])
        reach_c = set()
        stack = [low]
        while stack:
            q = stack.pop()
            if q in reach_c:
                continue
            reach_c.add(q)
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 0, 1), (0, 0, -1), (0, 1, 0), (0, -1, 0)):
                n = (q[0] + dx, q[1] + dy, q[2] + dz)
                if n not in reach_c and open_cell(n) and ((n[0], n[2]) in tiles or (n[0], n[2]) in ladder_cols or (n[0], n[2]) in
                                                          {(l[0] + dx_, l[2] + dz_) for l in ladders for dx_, dz_ in L.STEP.values()}):
                    stack.append(n)
        chamber = {(x, y, z) for (x, z) in tiles for y in (FR - 2, FR - 1) if mt.get(x, y, z) is None}
        lost = [q for q in sorted(chamber) if q not in reach_c]
        if lost:
            errs.append("%d free chamber cells cannot reach the ladder, e.g. %s" % (len(lost), lost[:2]))
    # the fall: every pit tile has air two rows under it and the chamber floor three blocks down
    for (x, z), safe in tiles.items():
        if not safe:
            floor = mt.get(x, FR - 3, z)
            if floor is None or D.classify(floor) != "solid":
                errs.append("pit tile (%d,%d) has no chamber floor" % (x, z))
                break
    return errs


def _path_of(cell):
    """The block id of a cell without its namespace."""
    return cell.block.split(":", 1)[1] if ":" in cell.block else cell.block


def check_puzzle(mt, spec):
    """The sigil halls: one plate of each colour, one riddle stone with a valid sequence, every gate kind=puzzle, the chests only reachable with the gate open, and
    everything the stone scans for (plates and gates, +-12 cells, -3 .. +5 rows) within its reach, plates finding the stone from their side (+-12, -5 .. +3)."""
    errs = []
    plates = _cells_of(mt, PLATE)
    if sorted(c.props.get("color") for _p, c in plates) != ["0", "1", "2", "3"]:
        errs.append("the plates must be one of each colour")
    stones = _cells_of(mt, RIDDLE)
    if len(stones) != 1:
        return errs + ["expected one riddle stone, found %d" % len(stones)]
    sp, sc = stones[0]
    seq = [int(v) for v in sc.nbt["sequence"]]
    if not (1 <= len(seq) <= 8 and all(0 <= v <= 3 for v in seq)):
        errs.append("bad riddle sequence %s" % seq)
    gates = _cells_of(mt, GATE)
    if not gates or any(c.props.get("kind") != "puzzle" for _p, c in gates):
        errs.append("every gatekeeper door must be kind=puzzle")
    for (x, y, z), _c in plates:
        if abs(x - sp[0]) > 12 or abs(z - sp[2]) > 12 or not (-3 <= y - sp[1] <= 5):
            errs.append("plate at %s is outside the stone's scan box" % ((x, y, z),))
        if not (-5 <= sp[1] - y <= 3):
            errs.append("the stone is outside the scan box of the plate at %s" % ((x, y, z),))
    for (x, y, z), _c in gates:
        if abs(x - sp[0]) > 12 or abs(z - sp[2]) > 12 or not (-3 <= y - sp[1] <= 5):
            errs.append("gate cell %s is outside the stone's scan box" % ((x, y, z),))
    chests = [pos for pos, c in _cells_of(mt, "minecraft:chest")]
    shut, opened = D.Analysis(mt, open_gates=False), D.Analysis(mt, open_gates=True)
    anchors = [a for a in shut.anchors() if a]
    reach_shut = shut.reach(anchors[0])
    reach_open = opened.reach(anchors[0])

    def beside(reach, pos):
        """True when one of the four neighbours of `pos` is in the reach set."""
        return any((pos[0] + dx, pos[1], pos[2] + dz) in reach for dx, dz in L.STEP.values())
    for pos in chests:
        if beside(reach_shut, pos):
            errs.append("the chest at %s is reachable with the puzzle door shut" % (pos,))
        if not beside(reach_open, pos):
            errs.append("the chest at %s is not reachable with the puzzle door open" % (pos,))
    for (pos, _c) in plates + stones:
        if not beside(reach_shut, pos):
            errs.append("%s at %s cannot be reached from the entrance" % (_c.block, pos))
    return errs


def check_trial(mt, an, spec):
    """The trial halls: one keystone whose watched box is the whole footprint from the floor to the ceiling, three ward runes in the inner layer of every
    doorway, at least six spawn glyphs on reachable floor, a reachable chest, and none of the vanilla spawn markers."""
    errs = []
    keys = _cells_of(mt, ARCHWAY)
    if len(keys) != 1:
        return ["expected one sealed archway, found %d" % len(keys)]
    (kx, ky, kz), kc = keys[0]
    radius, height = int(kc.nbt["radius"]), int(kc.nbt["height"])
    if kx - radius > 0 or kx + radius < mt.W - 1 or kz - radius > 0 or kz + radius < mt.D - 1:
        errs.append("the keystone box (radius %d at %d,%d) does not cover the %dx%d footprint" % (radius, kx, kz, mt.W, mt.D))
    if ky - height > FR or ky + 2 < FR + 10:
        errs.append("the keystone box rows %d..%d do not cover the floor and the hall" % (ky - height, ky + 2))
    if len(kc.nbt["mobs"]) < 1:
        errs.append("the keystone has no roster")
    for side, off, _kw in mt.sockets:
        runes = [(x, y, z) for lat in range(off - 1, off + 2) for (x, y, z) in [mt._lateral(side, off, lat, 1, FR)]
                 if mt.get(x, y, z) is not None and mt.get(x, y, z).block == WARD_RUNE]
        if len(runes) != 3:
            errs.append("doorway %s@%d has %d ward runes (3 expected)" % (side, off, len(runes)))
        for (x, y, z) in runes:
            if any(mt.get(x, FR + k, z) is not None for k in range(1, 5)):
                errs.append("the ward rune at %s has no clear column of four cells above it for the barrier" % ((x, y, z),))
    glyphs = [pos for pos, c in _cells_of(mt, SPAWN_GLYPH)]
    if len(glyphs) < 6:
        errs.append("only %d spawn glyphs" % len(glyphs))
    anchors = [a for a in an.anchors() if a]
    reach = an.reach(anchors[0]) if anchors else {}
    for (x, y, z) in glyphs:
        if (x, y + 1, z) not in reach:
            errs.append("the spawn glyph at %s is not on reachable floor" % ((x, y, z),))
    chests = [pos for pos, c in _cells_of(mt, "minecraft:chest")]
    if not chests:
        errs.append("no reward chest")
    for pos in chests:
        if not any((pos[0] + dx, pos[1], pos[2] + dz) in reach for dx, dz in L.STEP.values()):
            errs.append("the chest at %s cannot be reached" % (pos,))
    return errs


def check_dark(mt, an, spec):
    """The dark room: not one light-giving block anywhere (a glowing cell defeats the darkness), at least two snuffer glyphs, and every chest inside the six
    blocks of a glyph's darkness."""
    errs = []
    for pos, c in sorted(mt.t.cells.items()):
        if D.light_emission(c) > 0:
            errs.append("a light source (%s) at %s defeats the darkness" % (c.block, pos))
            if len(errs) > 4:
                break
    glyphs = [pos for pos, c in _cells_of(mt, SNUFFER)]
    if len(glyphs) < 2:
        errs.append("fewer than two snuffer glyphs")
    for pos, c in _cells_of(mt, "minecraft:chest"):
        d = min(math.sqrt((pos[0] - g[0]) ** 2 + (pos[1] - g[1]) ** 2 + (pos[2] - g[2]) ** 2) for g in glyphs)
        if d > 6.0:
            errs.append("the chest at %s is %.1f from the nearest glyph, outside its darkness" % (pos, d))
    return errs


def build(only=None):
    """Builds every template (or only the named ones) in memory and returns [(folder, name, template, spec)]."""
    out = []
    for folder, name, fn, spec in ROOMS:
        if only and name not in only:
            continue
        mt = fn(name)
        out.append((folder, name, mt, spec))
    return out


def disk_matches(mt):
    """Does the template file on disk hold exactly the bytes this script would write now?"""
    import nbt as _nbt
    path = mt.t.path(L.RES)
    if not os.path.exists(path):
        return False
    with open(path, "rb") as fh:
        return fh.read() == _nbt.dumps(mt.t.to_nbt(), compress=True)


def table_row(folder, name, mt):
    """One row of the markdown table of templates: name, size in blocks and modules, block count, doors, spawn / patrol markers and the faction."""
    kinds = {}
    for kind, _args, _pos in read_markers(mt):
        kinds[kind] = kinds.get(kind, 0) + 1
    doors = ", ".join("%s%d" % (sd, off) for sd, off, _kw in mt.sockets)
    return "| `%s/%s` | %dx%d (%dx%d modules) | %d | %s | %d / %d | %s |" % (
        folder, name, mt.W, mt.D, mt.mx, mt.mz, len(mt.t.cells), doors, kinds.get("spawn", 0), kinds.get("patrol", 0),
        ", ".join(args.get("faction", "") for kind, args, _p in read_markers(mt) if kind == "faction"))


def main(argv=None):
    """Command line entry: build, analyse, and (unless --check or --no-write) write the templates; exits 1 when an analysis error was found or, with --check,
    when a file on disk differs."""
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--only", action="append", default=[], help="build only this template name (repeatable)")
    ap.add_argument("--check", action="store_true", help="build in memory, run the analysis, fail when a file on disk differs from what would be written")
    ap.add_argument("--no-write", action="store_true", help="build and analyse without writing any file")
    ap.add_argument("--table", action="store_true", help="print a markdown table of the templates (name, size, blocks, doors, spawn / patrol, faction)")
    a = ap.parse_args(argv)
    known = {name for _folder, name, _fn, _spec in ROOMS}
    unknown = sorted(set(a.only) - known)
    if unknown:
        ap.error("no template named %s (known: %s)" % (", ".join(unknown), ", ".join(sorted(known))))
    bad = 0
    rows = []
    for folder, name, mt, spec in build(set(a.only) or None):
        errors, warns, info = check_room(mt, spec)
        if a.check and not disk_matches(mt):
            errors.append("the file on disk is not what this script writes now: run it without --check")
        status = "FAIL" if errors else "ok"
        print("%-4s %s/%s  %d blocks, %d entities" % (status, folder, name, len(mt.t.cells), len(mt.t.entities)))
        for e in errors:
            print("     ERROR " + e)
        for w in warns:
            print("     warn  " + w)
        for i in info:
            print("     " + i)
        bad += len(errors)
        rows.append(table_row(folder, name, mt))
        if not a.check and not a.no_write:
            path = mt.save(L.RES)
            print("     wrote %s (%d KB)" % (os.path.relpath(path, L.ROOT), os.path.getsize(path) // 1024))
    if a.table:
        print("| template | size | blocks | doors | spawn / patrol | faction |")
        print("|---|---|---|---|---|---|")
        for r in rows:
            print(r)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())

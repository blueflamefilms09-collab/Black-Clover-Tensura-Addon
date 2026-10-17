#!/usr/bin/env python3
"""
The Crucible Vault templates of the Mega-Dungeon (1.54.0, docs/labyrinth/mega_theme.md): `structure/mega/vault/{one,two,three}_{a,b}.nbt`, the room at the
end of every sector where the party chooses ONE relic and the rest crumble.

    python3 tools/labyrinth/gen_mega_vault.py                  # (re)writes every template
    python3 tools/labyrinth/gen_mega_vault.py three_a two_b    # only these
    python3 tools/labyrinth/gen_mega_vault.py --check          # regenerates in memory, compares with the files on disk, runs the vault checks
    python3 tools/labyrinth/gen_mega_vault.py --list

Two layouts, each for one, two and three relics (the number in the name is the number of pedestals):
  * `*_a`, THE ROTUNDA: a round hall under a ribbed dome, 27 x 27: the red carpet runs from the door across a gold medallion in the floor, a horseshoe of
    six pillars with lightning-rod spires closes round the approach, the relics stand on raised daises in an arc at the far side, four shrines with armoured
    statues are cut into the corners, chains hold cages and a wheel of soul lanterns over the medallion. A round room has no corner to hide in and room to
    run, which is what the catastrophes of the Crucible need.
  * `*_b`, THE NAVE: a long vaulted hall (27 x 27, and 27 x 36 for three relics) with a double row of pillars, transverse arches, a red carpet to a stepped
    chancel at the far end where the relics stand in a row before a gold-trimmed screen, cages and chandeliers hanging from the vault, shrines in the side
    walls.
Both are drawn in the shared palette (the deepslate and dark oak families) plus neutral accents (gold blocks, red carpet, soul lanterns, candles, iron bars,
chains, skulls, lightning rods, banners), so the Region Theme restyles the architecture and leaves the accents alone. The only custom block is the Relic
Pedestal, placed WITHOUT block-entity data: the Crucible fills it when the first person comes in. Every template has ONE socket, the door on the south face
at offset 13 (`nusmp:mega_vault`, an open hole: the vault is attached TO the antechamber), one `vault_center` marker, one `pedestal;slot=i` marker in the air
cell directly above each pedestal, no spawn marker and no other door: the way out is the way in.

Deterministic: every template draws from its own name-seeded random (`MT.rng`), so the same script writes the same bytes. `--check` also runs the analysis
of `mega_vault_kit.py` (sealed room, walkable route, pedestals, markers, light, floating blocks) on every template.
"""
import argparse
import hashlib
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import nbt  # noqa: E402
import mega_vault_kit as K  # noqa: E402
from mega_vault_kit import GROUND, FLOOR, GOLD, PEDESTAL, VaultMT  # noqa: E402

VAULTS = {}


def vault(name, pedestals):
    def deco(fn):
        VAULTS[name] = (fn, pedestals)
        return fn
    return deco


# ===================================================================================================================
# shared dressing
# ===================================================================================================================
def brick_mix(mt, p_cracked=0.12):
    return L.CRACKED_BRICK if mt.rng.random() < p_cracked else L.BRICK


def door_vestibule(mt, z_outer, z_inner, w_half=1):
    """The passage from the door hole to the room through wall mass that is thicker than the two wall layers: a 3 wide, 4 high tunnel with polished jambs
    and a chiselled lintel, for the cells z_outer..z_inner (z_outer is the one next to the hole)."""
    lo, hi = min(z_outer, z_inner), max(z_outer, z_inner)
    for z in range(lo, hi + 1):
        for x in range(13 - w_half, 13 + w_half + 1):
            for y in range(GROUND, GROUND + 4):
                mt.carve(x, y, z)
        for y in range(GROUND, GROUND + 4):
            mt.force(13 - w_half - 1, y, z, L.POLISHED)
            mt.force(13 + w_half + 1, y, z, L.POLISHED)
        for x in range(13 - w_half - 1, 13 + w_half + 2):
            mt.force(x, GROUND + 4, z, L.CHISELED if x == 13 else L.POLISHED)


def carpet(mt, x1, z1, x2, z2, y=GROUND, color="red"):
    for x in range(min(x1, x2), max(x1, x2) + 1):
        for z in range(min(z1, z2), max(z1, z2) + 1):
            mt.try_set(x, y, z, "minecraft:%s_carpet" % color)


def stalactite(mt, x, z, length):
    """A pointed-dripstone stalactite hanging from the first solid cell above the room at (x, z)."""
    ceil = K.ceiling_at(mt, x, z, GROUND + 8)
    if ceil is None:
        return
    for k in range(length):
        y = ceil - 1 - k
        thick = "base" if k == 0 and length > 1 else ("tip" if k == length - 1 else "frustum")
        mt.try_set(x, y, z, "minecraft:pointed_dripstone", {"vertical_direction": "down", "thickness": thick})


# ===================================================================================================================
# THE ROTUNDA
# ===================================================================================================================
CX = CZ = 13
RADIUS2 = 100                 # the room is the disc of cells with (x-13)^2 + (z-13)^2 <= 100: ten cells from the middle to the wall
SPRING = 17                   # the row the dome springs from; the drum stands below it
DOME_IN2, DOME_SKIN2, DOME_OUT2 = 100, 121, 144
DOOR_ANGLE = 270


def ang_diff(a, b):
    return abs((a - b + 180.0) % 360.0 - 180.0)


def rotunda(name, n):
    mt = VaultMT(name, 3, 3)
    mt.room(21, wear=0.07)
    mt.socket("s", 13, pool=L.P_EMPTY, name=L.VAULT, target=L.VAULT)
    drum(mt)
    floor(mt)
    door_vestibule(mt, 24, 23)
    carpet(mt, 12, 17, 14, 24)
    mt.keep_clear(12, 10, 14, 24)                 # from the door to the north dais: nothing solid is ever put on this strip
    dome(mt)
    pedestals = rotunda_daises(mt, n)
    for (px, pz) in ((6, 14), (7, 17), (10, 19), (16, 19), (19, 17), (20, 14)):
        K.column(mt, px, pz, GROUND, GROUND + 13, spire=3)
    for ang in (45, 135, 225, 315):
        shrine(mt, ang)
    rotunda_lights(mt)
    rotunda_dressing(mt)
    mt.marker(CX, GROUND, CZ, "vault_center")
    return mt, pedestals


def drum(mt):
    """The solid ring of wall round the disc up to the springline: a polished plinth, a gold-and-chiselled course, polished pilasters between the bays and a
    cornice of upside-down stairs under the dome; the stained windows are cut in last."""
    for z in range(2, 25):
        for x in range(2, 25):
            d2 = K.dist2(x, z, CX, CZ)
            if d2 <= RADIUS2:
                continue
            skin = d2 <= 121
            ang = K.angle_of(x, z, CX, CZ)
            nearest = round(ang / 30.0) * 30.0
            pilaster = skin and 10.5 * math.radians(ang_diff(ang, nearest)) < 0.9 and ang_diff(ang, DOOR_ANGLE) > 14
            for y in range(GROUND, SPRING + 1):
                if skin and y <= GROUND + 1:
                    b = L.POLISHED
                elif skin and y == GROUND + 5:
                    b = GOLD if (x + z) % 2 == 0 else L.CHISELED
                elif skin and y == SPRING:
                    b = None
                elif pilaster and y < SPRING - 1:
                    b = L.POLISHED
                elif pilaster and y == SPRING - 1:
                    b = L.CHISELED
                else:
                    b = brick_mix(mt, 0.10 if skin else 0.0)
                if b is not None:
                    mt.force(x, y, z, b)
            if skin:
                mt.force(x, SPRING, z, L.POLISHED_STAIRS, {"facing": K.toward(x, z, CX, CZ), "half": "top"})
    panes = ["minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane", "minecraft:red_stained_glass_pane"]
    for i, a in enumerate((15, 75, 105, 165, 195, 345)):
        for t in (10.0, 10.2, 10.4, 10.6, 10.8):
            x, z = K.rpolar(CX, CZ, a, t)
            if K.dist2(x, z, CX, CZ) > RADIUS2:
                break
        out = K.DIRS[K.away(x, z, CX, CZ)]
        for y in range(SPRING - 3, SPRING):
            K.window(mt, x, y, z, out, panes[i % 3], 12)


def floor(mt):
    """The floor of the disc: a chequerboard of tile and polished stone, a gold-spoked medallion in the middle and a polished ring near the wall."""
    for z in range(2, 25):
        for x in range(2, 25):
            d2 = K.dist2(x, z, CX, CZ)
            if d2 > RADIUS2:
                continue
            rho = math.sqrt(d2)
            ang = K.angle_of(x, z, CX, CZ)
            ray = ang_diff(((ang + 22.5) % 45.0) - 22.5, 0.0) < 3.4
            if rho <= 1.6:
                b = GOLD
            elif rho <= 2.6:
                b = L.CHISELED
            elif rho <= 3.6:
                b = L.POLISHED
            elif rho <= 4.8:
                b = GOLD if ray else (L.TILE if (x + z) % 2 == 0 else L.POLISHED)
            elif 8.7 < rho <= 9.5:
                b = L.POLISHED if (x + z) % 2 == 0 else L.CHISELED
            else:
                b = L.TILE if (x + z) % 2 == 0 else L.POLISHED
                if b == L.TILE and mt.rng.random() < 0.10:
                    b = L.CRACKED_TILE
            mt.force(x, FLOOR, z, b)


def dome(mt):
    """The dome: a skin of brick with twelve polished ribs, a chiselled ring and a gold eye at the top, over a plain outer layer."""
    for y in range(SPRING, SPRING + 13):
        for z in range(0, 27):
            for x in range(0, 27):
                d2 = (x - CX) ** 2 + (z - CZ) ** 2 + (y - SPRING) ** 2
                if not (DOME_IN2 < d2 <= DOME_OUT2) or mt.get(x, y, z) is not None:
                    continue
                if d2 > DOME_SKIN2:
                    mt.set(x, y, z, L.BRICK)
                    continue
                rho = math.sqrt((x - CX) ** 2 + (z - CZ) ** 2)
                ang = K.angle_of(x, z, CX, CZ)
                lateral = rho * math.sin(math.radians(((ang + 15.0) % 30.0) - 15.0)) if rho > 0 else 0.0
                if rho < 2.2 and y >= SPRING + 10:
                    b = GOLD
                elif abs(lateral) < 0.75 and rho > 2.0:
                    b = L.POLISHED
                elif y == SPRING + 4 or y == SPRING + 8:
                    b = L.CHISELED
                else:
                    b = brick_mix(mt, 0.14)
                mt.set(x, y, z, b)


def rotunda_daises(mt, n):
    spots = {"W": (7, 10, "east"), "N": (13, 6, "south"), "E": (19, 10, "west")}
    plan = {3: [("W", 0), ("N", 1), ("E", 2)], 2: [("W", 0), ("E", 1)], 1: [("N", 0)]}[n]
    used = {k for k, _ in plan}
    pedestals = []
    for key, slot in plan:
        cx, cz, front = spots[key]
        pedestals.append(K.dais(mt, cx, cz, front, True, slot=slot))
    for key in ("W", "N", "E"):
        if key in used:
            continue
        cx, cz, front = spots[key]
        K.dais(mt, cx, cz, front, False)
        K.statue_shrine(mt, cx, GROUND + 1, cz, K.YAW[front], head="minecraft:golden_helmet", chest="minecraft:golden_chestplate", legs="minecraft:golden_leggings",
                        feet="minecraft:golden_boots", weapon="minecraft:golden_sword")
    return pedestals


def shrine(mt, ang):
    """A corner shrine: a round alcove cut into the thick wall at the angle `ang`, roofed by half a sphere, with a statue on a plinth, candles, a hanging
    soul lantern, wall skulls and a cobweb."""
    px, pz = K.polar(CX, CZ, ang, 11.7)
    r = 2.4
    cell_x, cell_z = int(round(px)), int(round(pz))
    for z in range(int(pz - r) - 1, int(pz + r) + 2):
        for x in range(int(px - r) - 1, int(px + r) + 2):
            if 2 <= x <= 24 and 2 <= z <= 24 and (x - px) ** 2 + (z - pz) ** 2 <= r * r:
                for y in range(GROUND, GROUND + 6):
                    mt.carve(x, y, z)
                for y in range(GROUND + 6, GROUND + 9):
                    if (x - px) ** 2 + (z - pz) ** 2 + (y - (GROUND + 6)) ** 2 <= r * r:
                        mt.carve(x, y, z)
    yaw = (math.degrees(math.atan2(-(CX - px), CZ - pz))) % 360.0
    K.statue_shrine(mt, cell_x, GROUND, cell_z, yaw)
    for ox, oz in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        if mt.free(cell_x + ox, GROUND, cell_z + oz) and (cell_x + ox - px) ** 2 + (cell_z + oz - pz) ** 2 <= (r - 0.3) ** 2:
            K.candle_cluster(mt, cell_x + ox, GROUND, cell_z + oz, 4, "white")
    ceil = K.ceiling_at(mt, cell_x, cell_z, GROUND + 4)
    if ceil is not None:
        mt.try_set(cell_x, ceil - 1, cell_z, L.CHAIN_BLOCK, {"axis": "y"})
        mt.try_set(cell_x, ceil - 2, cell_z, L.SOUL_LANTERN, {"hanging": "true"})


def rotunda_lights(mt):
    """Light near the floor, where it is needed: four braziers round the medallion, two at the door, wall torches and banners round the drum, a field of
    lit candles round the medallion (the reference rotunda's field of candles), a ring of hanging lanterns high in the dome, the wheel over the medallion."""
    for (x, z) in ((9, 17), (17, 17), (8, 13), (18, 13)):
        K.brazier(mt, x, z)
    for (x, z) in ((10, 22), (16, 22)):
        K.brazier(mt, x, z, height=2)
    mt.set(12, GROUND + 2, 24, "minecraft:soul_wall_torch", {"facing": "east"})          # the passage through the wall mass
    mt.set(14, GROUND + 2, 24, "minecraft:soul_wall_torch", {"facing": "west"})
    for a in range(0, 360, 15):
        if ang_diff(a, DOOR_ANGLE) <= 15:
            continue
        x, z = K.rpolar(CX, CZ, a, 9.6)
        if K.dist2(x, z, CX, CZ) > RADIUS2:
            continue
        if a % 30 == 0:
            K.wall_item(mt, x, GROUND + 3, z, "soul_torch")
        else:
            K.wall_item(mt, x, GROUND + 7, z, "banner", color="red")
    colors = ("white", "white", "red", "white", "gray")
    for z in range(2, 25):
        for x in range(2, 25):
            rho = math.sqrt(K.dist2(x, z, CX, CZ))
            if not (4.9 <= rho <= 8.1) or (11 <= x <= 15 and z >= 17):
                continue
            if mt.rng.random() < 0.34:
                K.candle_cluster(mt, x, GROUND, z, mt.rng.choice((1, 2, 2, 3, 4)), mt.rng.choice(colors), if_free=True)
    for ang in range(0, 360, 45):
        x, z = K.rpolar(CX, CZ, ang + 22, 7.0)
        ceil = K.ceiling_at(mt, x, z, GROUND + 6)
        if ceil is None:
            continue
        for y in range(ceil - 2, ceil):
            mt.try_set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
        mt.try_set(x, ceil - 3, z, L.SOUL_LANTERN, {"hanging": "true"})
    K.chandelier_wheel(mt, CX, CZ, SPRING + 4, r=3)
    for (cx, cz) in ((9, 15), (17, 15), (13, 20)):
        K.cage_on_chains(mt, cx, cz, SPRING - 2, w=3, d=3, h=4)


def emblem(mt, cx, cy, half=4):
    """A gold-and-chiselled eye on the north wall behind the middle dais: a diamond of `half` cells to each side, drawn on the wall cell nearest the room at
    every column (the wall is round, so the picture follows the curve by a block at most)."""
    for dx in range(-half, half + 1):
        x = cx + dx
        zw = None
        for z in range(13, 1, -1):
            if K.dist2(x, z, CX, CZ) > RADIUS2:
                zw = z
                break
        if zw is None:
            continue
        for dy in range(-half, half + 1):
            r = abs(dx) + abs(dy)
            if r > half:
                continue
            b = GOLD if (r == half or r == 0) else (L.CHISELED if r <= 2 else L.POLISHED)
            mt.force(x, cy + dy, zw, b)


def rotunda_dressing(mt):
    emblem(mt, CX, GROUND + 8)
    for (x, z) in ((5, 10), (21, 10), (4, 16), (22, 16)):
        K.rubble_pile(mt, x, z)
    for ang, rho, ln in ((20, 6.5, 3), (110, 8.0, 2), (200, 7.0, 3), (290, 5.0, 2), (330, 8.5, 2), (160, 8.0, 2)):
        x, z = K.rpolar(CX, CZ, ang, rho)
        stalactite(mt, x, z, ln)


vault("one_a", 1)(lambda name: rotunda(name, 1))
vault("two_a", 2)(lambda name: rotunda(name, 2))
vault("three_a", 3)(lambda name: rotunda(name, 3))


# ===================================================================================================================
# THE NAVE
# ===================================================================================================================
RI2, RSKIN2, RO2 = 132.25, 156.25, 182.25           # the barrel vault: inner radius 11.5, a skin to 12.5 and an outer layer to 13.5, springing from row SPRING
CHANCEL_Z0, CHANCEL_Z1 = 4, 8                       # the platform of the chancel, z rows (the back wall fills rows 2 and 3)
CHANCEL_TOP = GROUND + 2                            # the first free row on the platform
PILLAR_X = (7, 19)
SIDES = (  # (inner wall column, pane column, step to the light block behind a pane, aisle column beside the wall, direction a statue there looks)
    (2, 1, (-1, 0), 3, "east"),
    (24, 25, (1, 0), 23, "west"),
)
PANES = ("minecraft:purple_stained_glass_pane", "minecraft:blue_stained_glass_pane", "minecraft:red_stained_glass_pane")


def nave(name, n, dm):
    d = 9 * dm
    mt = VaultMT(name, 3, dm)
    mt.room(21, wear=0.07)
    mt.socket("s", 13, pool=L.P_EMPTY, name=L.VAULT, target=L.VAULT)
    rows = list(range(14, d - 4, 4))                          # the pillar rows, and the transverse arches over them
    first = CHANCEL_Z1 + 3                                     # the first row of the nave floor proper, at the foot of the chancel steps
    nave_shell(mt, d, rows)
    nave_floor(mt, d)
    carpet(mt, 12, first, 14, d - 3)
    mt.keep_clear(12, first, 14, d - 3)                        # from the door to the foot of the chancel nothing solid is ever put on this strip
    pedestals = chancel(mt, n)
    for z in rows:
        for px in PILLAR_X:
            K.column(mt, px, z, GROUND, GROUND + 14, spire=3)
    nave_walls(mt, d, rows)
    nave_lights(mt, d, rows)
    nave_dressing(mt, d, rows)
    mt.marker(13, GROUND + 1, (d - 1) // 2, "vault_center")
    return mt, pedestals


def nave_shell(mt, d, rows):
    """The barrel vault along the whole hall, its skin ribbed with polished arches over every pillar row and a chiselled ridge, and the back wall that
    fills the end of the hall up to the vault."""
    for z in range(2, d - 2):
        for x in range(2, 25):
            dx = x - 13
            for y in range(GROUND, SPRING + 14):
                d2 = dx * dx + (y - SPRING) ** 2
                if y < SPRING or d2 <= RI2:                            # inside the hall: air, except where the back wall stands
                    if z < CHANCEL_Z0:
                        mt.force(x, y, z, L.POLISHED if y < GROUND + 2 else brick_mix(mt, 0.08))
                    continue
                if d2 > RO2:
                    continue
                if d2 > RSKIN2:
                    mt.set(x, y, z, L.BRICK)
                elif z in rows:
                    mt.set(x, y, z, L.POLISHED)
                elif x == 13:
                    mt.set(x, y, z, L.CHISELED)
                else:
                    mt.set(x, y, z, brick_mix(mt, 0.14))


def nave_floor(mt, d):
    """The floor: the chequerboard of tile and polished stone with a gold line along each edge of the carpet."""
    for z in range(CHANCEL_Z0, d - 2):
        for x in range(2, 25):
            if x in (11, 15) and z >= CHANCEL_Z1 + 3:
                b = GOLD
            else:
                b = L.TILE if (x + z) % 2 == 0 else L.POLISHED
                if b == L.TILE and mt.rng.random() < 0.10:
                    b = L.CRACKED_TILE
            mt.force(x, FLOOR, z, b)


def chancel(mt, n):
    """The raised end of the hall: a platform two blocks high across the width with two steps down to the nave, the pedestals in a row on gold bases with
    candles round them, a niche in the back wall behind every pedestal (and every statue), a gold course and an emblem above, braziers between."""
    spots = {3: (7, 13, 19), 2: (9, 17), 1: (13,)}[n]
    statues = {3: (), 2: (13,), 1: (7, 19)}[n]
    braziers = {3: (10, 16), 2: (11, 15), 1: (10, 16)}[n]
    for z in range(CHANCEL_Z0, CHANCEL_Z1 + 1):
        for x in range(2, 25):
            mt.force(x, GROUND, z, L.POLISHED)
            mt.force(x, GROUND + 1, z, GOLD if (z == CHANCEL_Z1 and x % 2 == 0) else (L.TILE if (x + z) % 2 == 0 else L.POLISHED))
    for x in range(2, 25):
        mt.force(x, GROUND, CHANCEL_Z1 + 1, L.POLISHED)
        K.stairs(mt, x, GROUND + 1, CHANCEL_Z1 + 1, "north", "bottom", L.POLISHED_STAIRS, force=True)
        K.stairs(mt, x, GROUND, CHANCEL_Z1 + 2, "north", "bottom", L.POLISHED_STAIRS, force=True)
    pedestals = []
    for slot, px in enumerate(spots):
        for x in range(px - 1, px + 2):
            for z in (4, 5, 6):
                mt.force(x, GROUND + 1, z, L.CHISELED if (x, z) == (px, 5) else GOLD)
        for ox, oz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            K.candle_cluster(mt, px + ox, CHANCEL_TOP, 5 + oz, 4, "white")
        mt.force(px, CHANCEL_TOP, 5, PEDESTAL)
        mt.marker(px, CHANCEL_TOP + 1, 5, "pedestal", slot=slot)
        pedestals.append((px, CHANCEL_TOP, 5))
    for px in statues:
        K.statue_shrine(mt, px, CHANCEL_TOP, 5, 0.0, head="minecraft:golden_helmet", chest="minecraft:golden_chestplate", legs="minecraft:golden_leggings",
                        feet="minecraft:golden_boots", weapon="minecraft:golden_sword")
    for px in sorted(list(spots) + list(statues)):                # a niche, two deep, in the back wall behind every pedestal and statue
        for x in range(px - 1, px + 2):
            for y in range(CHANCEL_TOP + 1, CHANCEL_TOP + 5):
                for z in (2, 3):
                    mt.carve(x, y, z)
        for z in (2, 3):
            for x in (px - 1, px + 1):
                K.stairs(mt, x, CHANCEL_TOP + 4, z, K.toward(x, z, px, z), "top", L.POLISHED_STAIRS, force=True)
        mt.set(px, CHANCEL_TOP + 4, 3, L.CHAIN_BLOCK, {"axis": "y"})
        mt.set(px, CHANCEL_TOP + 3, 3, L.SOUL_LANTERN, {"hanging": "true"})
        K.wall_item(mt, px, CHANCEL_TOP + 2, 2, "banner", color="red")
    for x in range(2, 25):
        mt.force(x, CHANCEL_TOP + 5, 3, GOLD if x % 2 == 0 else L.CHISELED)         # the gold course over the niches
        mt.force(x, SPRING + 1, 3, GOLD if x % 2 == 0 else L.CHISELED)              # and the frieze under the emblem
    for x in (10, 16, 4, 22):
        K.wall_item(mt, x, CHANCEL_TOP + 2, 3, "banner", color="red")
    back_emblem(mt, 13, SPRING + 6)
    for bx in braziers:
        K.brazier(mt, bx, 5, y=CHANCEL_TOP, height=1)
    for bx in (4, 22):
        K.brazier(mt, bx, 6, y=CHANCEL_TOP, height=2)
    return pedestals


def back_emblem(mt, cx, cy, half=4):
    """The gold-and-chiselled eye on the back wall above the niches: a diamond of `half` cells to each side, flat on the wall face (row 3)."""
    for dx in range(-half, half + 1):
        for dy in range(-half, half + 1):
            r = abs(dx) + abs(dy)
            if r <= half:
                mt.force(cx + dx, cy + dy, 3, GOLD if (r == half or r == 0) else (L.CHISELED if r <= 2 else L.POLISHED))


def nave_walls(mt, d, rows):
    """Both side walls: a polished wainscot, a pilaster under every transverse arch that runs up into the rib, a tall stained window in every bay with an
    invisible light block behind each pane so that it glows, a soul torch and a red banner on every pilaster, and a statue before every second bay (a
    brazier before the others)."""
    bays = [rows[0] - 2] + [z + 2 for z in rows[:-1]]                  # the window bays sit between two arches (and one before the first)
    for si, (xi, xp, behind, xa, face) in enumerate(SIDES):
        for z in range(CHANCEL_Z1 + 3, d - 2):
            for y in (GROUND, GROUND + 1):
                mt.force(xi, y, z, L.POLISHED)
        for z in rows:
            for y in range(GROUND + 2, SPRING + 4):
                mt.force(xi, y, z, L.POLISHED)
        for k, z in enumerate(bays):
            for dz in (-1, 0, 1):
                for y in range(GROUND + 3, SPRING + 3):
                    K.window(mt, xp, y, z + dz, behind, PANES[(k + si) % 3], 12)
        for z in rows:
            K.wall_item(mt, xa, GROUND + 3, z, "soul_torch")
            K.wall_item(mt, xa, GROUND + 7, z, "banner", color="red")
        for k, z in enumerate(bays):
            if k % 2 == 0:
                K.statue_shrine(mt, xa, GROUND, z, K.YAW[face])
            else:
                K.brazier(mt, xa, z)


def nave_lights(mt, d, rows):
    """Lit candles along both gold lines of the carpet, two tall braziers and two banners at the door, a spoked chandelier over the carpet between every
    second pair of arches, and a hanging cage in every second bay of the aisles."""
    first = CHANCEL_Z1 + 3
    for z in range(first + 1, d - 3, 3):
        for x in (11, 15):
            K.candle_cluster(mt, x, GROUND, z, 4, "white", if_free=True)
    for x in (9, 17):
        K.brazier(mt, x, d - 5, height=2)
    for x in (10, 16):
        K.wall_item(mt, x, GROUND + 3, d - 3, "soul_torch")
    for x in (9, 17):
        K.wall_item(mt, x, GROUND + 6, d - 3, "banner", color="red")
    for z in range(rows[0] + 2, d - 5, 6):
        K.chandelier_wheel(mt, 13, z, SPRING + 6, r=3)
    for i, z in enumerate(rows):
        for cx in (4, 22):
            if (i + (cx > 10)) % 2 == 1:
                K.cage_on_chains(mt, cx, z, SPRING - 3, w=3, d=3, h=4)


def nave_dressing(mt, d, rows):
    """Rubble in the aisles and stalactites from the vault: the hall looks as if it could come down."""
    for (x, z) in ((4, 12), (22, 12), (4, d - 6), (22, d - 6)):
        K.rubble_pile(mt, x, z)
    for x, z, ln in ((10, 17, 3), (16, 20, 2), (11, 24, 2), (15, 21, 3)):
        if z < d - 4:
            stalactite(mt, x, z, ln)


vault("one_b", 1)(lambda name: nave(name, 1, 3))
vault("two_b", 2)(lambda name: nave(name, 2, 3))
vault("three_b", 3)(lambda name: nave(name, 3, 4))


# ===================================================================================================================
# driver
# ===================================================================================================================
def build(name):
    fn, n = VAULTS[name]
    mt, pedestals = fn("mega/vault/" + name)
    mt.finish()
    return mt, n


def entry_cell(mt):
    return (13, GROUND, mt.D - 3)


def analyze(name, mt, n, quiet=False):
    problems, stats = K.analyze(mt.t, entry_cell(mt), n, clear=mt.clear)
    return problems, stats


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[1])
    ap.add_argument("names", nargs="*")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--no-write", action="store_true")
    args = ap.parse_args(argv)
    if args.list:
        for k in sorted(VAULTS):
            print(k, VAULTS[k][1], "pedestal(s)")
        return 0
    names = args.names or sorted(VAULTS)
    bad = 0
    for name in names:
        if name not in VAULTS:
            print("unknown vault", name)
            return 2
        mt, n = build(name)
        problems, stats = analyze(name, mt, n)
        path = mt.t.path(L.RES)
        data = None
        if args.check:
            import io
            import tempfile
            with tempfile.TemporaryDirectory() as tmp:
                p = mt.t.save(tmp)
                data = open(p, "rb").read()
            on_disk = open(path, "rb").read() if os.path.exists(path) else b""
            if data != on_disk:
                print("STALE  %s: the file on disk differs from what the generator writes" % name)
                bad += 1
        elif not args.no_write:
            mt.save()
        print("%-8s %s  %6d blocks  air %5d  walkable %4d  light min/median %s/%s  dark %d/%d  %s" % (
            name, "x".join(str(v) for v in stats["size"]), stats["blocks"], stats["air_cells"], stats["walkable_cells"], stats.get("light_min", "-"),
            stats.get("light_median", "-"), stats.get("dark_floor_cells", 0), stats.get("floor_cells", 0), hashlib.sha1(nbt.dumps(mt.t.to_nbt())).hexdigest()[:8]))
        for pr in problems:
            print("  PROBLEM %s: %s" % (name, pr))
            bad += 1
    print("%d template(s), %d problem(s)" % (len(names), bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

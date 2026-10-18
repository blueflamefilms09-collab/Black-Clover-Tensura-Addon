#!/usr/bin/env python3
"""
ROOMS-3 of the Mega-Dungeon (1.64.0): six functional halls taken from the room catalogue of the Dungeon Keeper games (hatchery, training room, lair, temple, graveyard,
guard post) and from the usual room-idea lists (a gambling hall), as `structure/mega/hall/*` templates next to the 69 halls that already exist. Nothing is replaced.

    python3 tools/labyrinth/gen_mega_rooms3.py                  # (re)writes every template
    python3 tools/labyrinth/gen_mega_rooms3.py hatchery_a       # only these
    python3 tools/labyrinth/gen_mega_rooms3.py --check          # regenerates in memory, compares with the files on disk, runs every template check
    python3 tools/labyrinth/gen_mega_rooms3.py --list

Why a sibling of gen_mega_rooms1.py and not an edit of it: the owner's rule is "add the new, keep the old", and the rooms-1 generator's output is compared byte for byte by its own
--check. This file reuses its kit (Hall, the decor library, finish_common, the checks and the writer) and adds only the rooms. Why these rooms: the existing halls already cover the
barracks, cells, torture chamber, crypt, library, workshop (armory forge, alchemy lab), kitchen and shrine of the catalogue; what was missing is the livestock room, the drill
ground, the monsters' lair, the temple with its idol, the open graveyard, the gatehouse and a room whose purpose is a gamble. Each one gives the encounter director a different
kind of fight (a pen that breaks open, a cage of beasts, a boneyard that stirs) and gives the player a different thing to read in the room (what lives here, what is trained here).
Same footprint and doorways as the Soldiers' Quarters (3 x 2 modules, doors north, south and east), so every room fits wherever a hall fits.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import gen_mega_rooms1 as R1  # noqa: E402
from mega_decor_r1 import GROUND, Hall  # noqa: E402

ROOMS = {}


def room(name):
    def deco(fn):
        ROOMS[name] = fn
        return fn
    return deco


def hall(name, wear=0.10):
    """A 3 x 2 hall with the doorways and routes of the Soldiers' Quarters (barracks_a): doors north (22), south (4) and east (13), the north aisle and the band across."""
    h = Hall("mega/hall/" + name, 3, 2, 8, wear=wear)
    h.door("n", 22)
    h.door("s", 4)
    h.door("e", 13)
    h.route(21, 2, 23, 11, label="north aisle")
    h.route(3, 9, 23, 11, label="band")
    return h


def finish(h, faction=None):
    R1.finish_common(h)
    if faction:
        h.faction(faction, 13, 7)
    D.auto_light(h)
    D.auto_markers(h)
    return h


def hay(h, x, z, y=GROUND):
    h.try_set(x, y, z, "minecraft:hay_block", {"axis": "y"})


def pen(h, x1, z1, x2, z2, gap_x):
    """A fenced pen over the rectangle with a gap in its south side at gap_x (the animals are kept in, the party walks in)."""
    D.fence_run(h, x1, z1, x2, z1, GROUND)
    D.fence_run(h, x1, z1, x1, z2, GROUND)
    D.fence_run(h, x2, z1, x2, z2, GROUND)
    D.fence_run(h, x1, z2, x2, z2, GROUND, skip={(gap_x, z2)})


# ===================================================================================================================
# 1. hatchery_a: the Hatchery (Dungeon Keeper: the room where the livestock is bred)
# ===================================================================================================================
@room("hatchery_a")
def hatchery_a():
    h = hall("hatchery_a")
    for x1, x2 in ((4, 8), (10, 14), (16, 19)):
        pen(h, x1, 2, x2, 7, (x1 + x2) // 2)
        hay(h, x1 + 1, 3)
        hay(h, x2 - 1, 3)
        h.try_set(x1 + 1, GROUND + 1, 3, "minecraft:turtle_egg")
        h.try_set((x1 + x2) // 2, GROUND, 4, "minecraft:water_cauldron", {"level": "3"})
    for x in (6, 10, 14):                                        # feed troughs and sacks along the south wall
        h.try_set(x, GROUND, 14, "minecraft:composter")
        hay(h, x + 1, 14)
        hay(h, x + 1, 15)
        h.try_set(x + 2, GROUND, 14, "minecraft:barrel", {"facing": "north", "open": False})
    h.try_set(18, GROUND, 14, "minecraft:chest", {"facing": "north", "type": "single"})
    h.marker(18, GROUND + 1, 14, "furniture", **{"as": "storage"})
    D.clutter(h, [D.cl_barrels], 3)
    D.beams(h, "z", [6, 14, 22])
    return finish(h, "beast")


# ===================================================================================================================
# 2. training_hall_a: the Training Room (drill ground: dummies, targets, a sand ring)
# ===================================================================================================================
@room("training_hall_a")
def training_hall_a():
    h = hall("training_hall_a")
    wn, we, ww = h.wall("n"), h.wall("e"), h.wall("w")
    for x in range(5, 20):                                         # the sand ring of the drill ground
        for z in range(3, 8):
            h.floor_set(x, z, "minecraft:packed_mud")
    for i, x in enumerate((6, 10, 14, 18)):                       # straw dummies on posts, each wearing a helmet
        h.set(x, GROUND, 5, L.FENCE)
        h.set(x, GROUND + 1, 5, L.FENCE)
        h.armor_stand(x, GROUND + 2, 5, yaw=180.0, head="minecraft:iron_helmet" if i % 2 == 0 else "minecraft:leather_helmet", chest="minecraft:leather_chestplate", dy=0.0)
    for x in (4, 6, 8, 10, 12, 14, 16, 18):                       # archery targets along the north wall
        h.try_set(x, GROUND + 2, 2, "minecraft:target")
    D.weapon_niche(we, 4)
    D.weapon_niche(ww, 13)
    for z in (13, 15):                                             # benches for the recruits
        D.bench(h.frame(6, GROUND, z, "east"), 9)
    D.beams(h, "z", [6, 14, 22])
    D.dress_wall(h, "s", [D.it_banner("red", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=7, hi=24, step=(3, 4))
    return finish(h, "goblin")


# ===================================================================================================================
# 3. beast_den_a: the Lair (cages of beasts, hay, bones)
# ===================================================================================================================
@room("beast_den_a")
def beast_den_a():
    h = hall("beast_den_a", wear=0.14)
    for x1 in (4, 9, 14):
        x2, z1, z2 = x1 + 4, 2, 7
        for y in range(GROUND, GROUND + 3):                         # iron-bar cages, open on the south side
            D.fence_run(h, x1, z1, x2, z1, y, L.IRON_BARS)
            D.fence_run(h, x1, z1, x1, z2, y, L.IRON_BARS)
            D.fence_run(h, x2, z1, x2, z2, y, L.IRON_BARS)
            D.fence_run(h, x1, z2, x2, z2, y, L.IRON_BARS, skip={(x1 + 2, z2)})
        hay(h, x1 + 1, 3)
        hay(h, x1 + 3, 3)
        D.bones(h, x1 + 2, 4)
    for x in (6, 11, 16):
        D.bones(h, x, 14, rot=4, big=(x == 11))
    D.blood(h, [(8, 12), (9, 13), (14, 12)])
    D.clutter(h, [D.cl_bones, D.cl_rubble], 5)
    D.beams(h, "z", [6, 14, 22])
    return finish(h, "beast")


# ===================================================================================================================
# 4. temple_a: the Temple (an idol, a sacrificial slab, pews, braziers)
# ===================================================================================================================
@room("temple_a")
def temple_a():
    h = hall("temple_a", wear=0.12)
    D.idol(h, 13, 2)
    D.sacrificial_slab(h, 13, 6)
    for x in (4, 19):
        D.brazier(h, x, 3)
    D.brazier(h, 18, 14)
    for z in (13, 15):
        D.bench(h.frame(7, GROUND, z, "east"), 5)
        D.bench(h.frame(14, GROUND, z, "east"), 5)
    D.beams(h, "z", [6, 14, 22])
    return finish(h, "undead")


# ===================================================================================================================
# 5. graveyard_hall_a: the Boneyard (graves under the vault, headstones, a dead tree)
# ===================================================================================================================
@room("graveyard_hall_a")
def graveyard_hall_a():
    h = hall("graveyard_hall_a", wear=0.16)
    for z in (3, 5, 13, 15):                                       # rows of graves: a mound of coarse dirt with a headstone at its head
        for x in (5, 8, 11, 14, 17):
            if z in (13, 15) and x < 8:                                # the south door's apron stays clear
                continue
            h.floor_set(x, z, "minecraft:packed_mud")
            h.floor_set(x + 1, z, "minecraft:packed_mud")
            h.try_set(x, GROUND, z - 1, L.COBBLED_WALL)
            if (x + z) % 3 == 0:
                D.skull_at(h, x, GROUND + 1, z - 1)
    for y in range(GROUND, GROUND + 4):                            # a dead tree in the north-west corner
        h.try_set(3, y, 3, "minecraft:stripped_dark_oak_log", {"axis": "y"})
    for dx, dz in ((-1, 0), (1, 0), (0, 1)):
        h.try_set(3 + dx, GROUND + 3, 3 + dz, "minecraft:stripped_dark_oak_wood", {"axis": "x" if dx else "z"})
    D.blood(h, [(9, 13)], "red")
    D.cobwebs(h, 0.5)
    D.clutter(h, [D.cl_bones], 4)
    D.beams(h, "z", [6, 14, 22])
    return finish(h, "undead")


# ===================================================================================================================
# 6. guard_post_a: the Guard Post (the watch desk, a bell, bunks, a weapon rack)
# ===================================================================================================================
@room("guard_post_a")
def guard_post_a():
    h = hall("guard_post_a")
    we, ww = h.wall("e"), h.wall("w")
    D.table(h.frame(6, GROUND, 4, "east"), 6)                      # the watch desk with its ledger candles and a chair
    h.marker(7, GROUND + 1, 4, "furniture", **{"as": "table"})
    D.chair(h.frame(5, GROUND, 4, "east"), seat_marker=True)
    D.candles_at(h, 8, GROUND + 1, 4, 2, "orange")
    h.set(15, GROUND, 3, "minecraft:cartography_table")
    for i, x in enumerate((10, 14, 18)):
        D.bunk(h.frame(x, GROUND, 13, "south"), color="blue" if i % 2 == 0 else "red", chains=True)
    D.hang_lantern(h, 12, 8, 3)
    D.weapon_niche(we, 4)
    D.weapon_niche(ww, 13)
    h.armor_stand(5, GROUND, 8, yaw=0.0, head="minecraft:iron_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings", feet="minecraft:iron_boots", mainhand="minecraft:iron_sword")
    D.clutter(h, [D.cl_barrels, D.cl_candles], 3)
    D.beams(h, "z", [6, 14, 22])
    return finish(h, "goblin")


# ===================================================================================================================
# 7. casino_hall_a: the Gambler's Hall (felt tables, a bar, the house always wins)
# ===================================================================================================================
@room("casino_hall_a")
def casino_hall_a():
    h = hall("casino_hall_a", wear=0.06)
    for x, z in ((6, 3), (12, 3), (6, 6), (12, 6)):                 # four felt tables, each with its candles, and stools
        for dx in range(3):
            for dz in range(2):
                h.set(x + dx, GROUND, z + dz, L.PLANKS)
                h.set(x + dx, GROUND + 1, z + dz, "minecraft:green_carpet")
        D.candles_at(h, x + 1, GROUND + 2, z, 2, "yellow") if False else None
        for dx in range(3):
            h.try_set(x + dx, GROUND, z - 1, L.PLANKS_STAIRS, {"facing": "south", "half": "bottom"})
    h.marker(7, GROUND + 2, 3, "furniture", **{"as": "table"})
    for x in range(14, 20):                                         # the bar: a counter of barrels, planks and slabs with a merchant's stool
        h.set(x, GROUND, 13, "minecraft:barrel", {"facing": "north", "open": False})
        h.set(x, GROUND, 14, L.PLANKS_SLAB, {"type": "bottom"})
    h.marker(16, GROUND + 1, 13, "furniture", **{"as": "counter"})
    D.chair(h.frame(6, GROUND, 14, "north"))
    D.chair(h.frame(9, GROUND, 14, "north"))
    D.chandelier_wheel(h, 9, 7, 2, 3)
    D.dress_wall(h, "s", [D.it_banner("yellow", y=GROUND + 6, hang=2), D.it_wall_torch(y=GROUND + 5)], lo=7, hi=24, step=(3, 4))
    D.beams(h, "z", [6, 14, 22])
    return finish(h)


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("names", nargs="*")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--force", action="store_true")
    a = ap.parse_args(argv)
    if a.list:
        print("\n".join(ROOMS))
        return 0
    bad = 0
    for n in a.names or list(ROOMS):
        if n not in ROOMS:
            print("unknown template", n)
            return 2
        h = ROOMS[n]()
        h.finish()
        problems, stats = h.check()
        if problems:
            bad += 1
        if a.check:
            p = h.mt.t.path(L.RES)
            if not (os.path.exists(p) and R1._same(h, p)):
                print("STALE %s (the file on disk differs from the generator)" % n)
                bad += 1
        elif not problems or a.force:
            h.save()
        else:
            print("NOT WRITTEN %s" % n)
        print("%s %-22s %5d blocks %2d doors %2d stands light min %s median %s dark %s%% markers %s" % ("ok  " if not problems else "FAIL", n, stats.get("blocks", 0), len(h.doors),
              stats.get("armor_stands", 0), stats.get("eye_light_min"), stats.get("eye_light_median"), round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
        for p in problems:
            print("     - " + p)
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

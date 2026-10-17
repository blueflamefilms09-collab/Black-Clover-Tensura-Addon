#!/usr/bin/env python3
"""
THEME-C, theme 4 of 4 (1.59.0, docs/labyrinth/mega_v2_thc.md): the signature rooms of the HAUNTED MANOR, the evil road's house that remembers. Every room is named
`haunted_manor_<room>.nbt` so that `data/nusmp/mega/template_themes/haunted_manor.json` can reserve it for sectors of that theme.

    python3 tools/labyrinth/gen_mega_theme_haunted_manor.py                      # (re)writes every template
    python3 tools/labyrinth/gen_mega_theme_haunted_manor.py haunted_manor_grand_stair   # only this one
    python3 tools/labyrinth/gen_mega_theme_haunted_manor.py --check              # regenerates in memory, compares with the files on disk, runs every check
    python3 tools/labyrinth/gen_mega_theme_haunted_manor.py --list

Why a generator and not hand-placed blocks: see `gen_mega_theme_cursed_archive.py`; the rules are the same (connector contract, palette constants so the
Region Theme restyles them into pale plaster, dark tiles and spruce, the kit of `mega_decor_r1.py` and `mega_thc_kit.py`, every template checked before it is
written). The lights of this theme are left as the templates draw them: the cold teal of soul lanterns next to the warm gold of candles and lanterns, because a
house where two kinds of light disagree is a house where something is wrong; the windows are lit by pale green froglights (the moon on the glass).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
import mega_thc_kit as K  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall  # noqa: E402

REG = K.Registry(__doc__)
room = REG.room
CALM = ("haunted_manor_master_bedroom", "haunted_manor_ballroom")      # a safe room and a trial hall carry no spawn markers of their own
SOUL = True
MOON = "minecraft:verdant_froglight"             # the pale green lamp behind every window: light 15, never swapped by a theme
PANES = ("minecraft:light_gray_stained_glass", "minecraft:cyan_stained_glass", "minecraft:light_gray_stained_glass", "minecraft:white_stained_glass")
LOOT = "nusmp:chests/labyrinth_trial"


def sheet(h, x, z, w=1, d=1, hgt=1):
    """A dust sheet over a piece of furniture: a block of white wool `w` x `d` x `hgt`, wherever the cells are free. Why: the brief's house of covered furniture; wool is
    flammable, so no room of this theme holds lava (checked by `K.lava_fire_problems`)."""
    for k in range(hgt):
        for dz in range(d):
            for dx in range(w):
                h.try_set(x + dx, GROUND + k, z + dz, "minecraft:white_wool")


def portrait(w, lat, y0, y1, width=2):
    """A portrait: a pane of dark glass in a frame of the wood family let into the inner wall layer (the wall behind it is the painting, black). Returns False when
    the wall there is not plain."""
    return K.mirror(w, lat, y0, y1, width=width, glass="minecraft:gray_stained_glass", frame=L.PLANKS, side=L.PLANKS)


def grandfather_clock(h, x, z):
    """A tall case clock, 1 x 1 x 5: three rows of planks, a face of white glass, a hood of a bottom slab. Why: the brief's house stopped at one hour; a clock is the
    smallest thing that says so."""
    for y in range(GROUND, GROUND + 3):
        h.set(x, y, z, L.PLANKS)
    h.set(x, GROUND + 3, z, "minecraft:white_stained_glass")
    h.set(x, GROUND + 4, z, L.PLANKS_SLAB, {"type": "bottom"})


def rail_both_sides(h, x0, z0, facing, steps, width, base):
    """Fence posts on the top of the steps of a flight along both of its long edges (`stair_flight` rails only one)."""
    fx, fz = D.DV[facing]
    rx, rz = D.DV[D.RIGHT[facing]]
    for k in range(steps - 1):
        for r in (0, width - 1):
            h.try_set(x0 + k * fx + r * rx, base + k + 1, z0 + k * fz + r * rz, L.FENCE)


# ----------------------------------------------------------------------------------------------------------------------------------- the grand stair
GS_LANDING = GROUND + 4          # the row of the half landing; you stand on the row above
GS_GALLERY = GROUND + 8          # the row of the gallery deck


def gallery_deck(h, cells, row, lantern_every=3):
    """Planks (a log now and then) on `cells` at `row`, a parapet of wall blocks along every edge that looks into open air, a standing lantern on every
    `lantern_every`-th post. Returns the set of deck cells."""
    deck = set()
    for (x, z) in cells:
        if h.try_set(x, row, z, L.PLANKS if (x + z) % 4 else L.STRIPPED_LOG, None if (x + z) % 4 else {"axis": "x"}):
            deck.add((x, z))
    k = 0
    for (x, z) in sorted(deck):
        edge = any(h.inside(x + dx, z + dz) and h.free(x + dx, row, z + dz) and (x + dx, z + dz) not in deck for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if edge and h.try_set(x, row + 1, z, L.POLISHED_WALL):
            k += 1
            if k % lantern_every == 0:
                h.try_set(x, row + 2, z, L.SOUL_LANTERN if SOUL else L.LANTERN, {"hanging": False})
    return deck


@room("haunted_manor_grand_stair", "hall")
def grand_stair():
    """The Grand Stair: the hall of the house, two storeys of it. A broad flight climbs from the front door to a half landing, two return flights carry on from its
    ends to a gallery that runs round three sides of the hall, a chandelier of soul lanterns hangs over the well, and the walls are hung with dark portraits and
    black mirrors, with a grandfather clock by the door that has stopped at the hour of the death. The first room the party sees of the house, and the one that
    remembers it."""
    h = Hall("mega/hall/haunted_manor_grand_stair", 3, 3, 24, wear=0.12)
    top = h.top
    doors = (("s", 13), ("n", 13), ("w", 22), ("e", 22))
    for side, off in doors:
        h.door(side, off)
    for side, off in doors:
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    # 1. the stair: the central flight (5 steps) to the half landing, then a return flight (4 steps) from each end up to the gallery deck
    D.stair_flight(h, 11, 19, "north", 5, 5, GROUND, stairs=L.POLISHED_STAIRS)
    rail_both_sides(h, 11, 19, "north", 5, 5, GROUND)
    for z in range(12, 15):
        for x in range(9, 18):
            for y in range(GROUND, GS_LANDING + 1):
                h.set(x, y, z, L.POLISHED if y == GS_LANDING else D.brick_mix(h, 0.15, 0.1))
    D.stair_flight(h, 8, 14, "west", 4, 3, GROUND + 5, stairs=L.POLISHED_STAIRS, rail_r=0)
    D.stair_flight(h, 18, 12, "east", 4, 3, GROUND + 5, stairs=L.POLISHED_STAIRS, rail_r=2)
    for z in range(12, 15):
        for x in (5, 6, 7, 8, 18, 19, 20, 21):
            for y in range(GROUND, GROUND + 5):
                h.try_set(x, y, z, D.brick_mix(h, 0.15, 0.1))
    # 2. the gallery: a deck round the north, west and east sides, reached by the return flights
    band = [(x, z) for z in range(2, 15) for x in range(2, 25) if (x <= 4 or x >= 22 or z <= 4)]
    gallery_deck(h, band, GS_GALLERY)
    # 3. walls: tall windows lit by the moon over the gallery, portraits and black mirrors below it, the clocks by the front door
    for lat in (6, 13, 20):
        K.gothic_window(wn, lat, GROUND + 10, GROUND + 19, PANES, width=3, glow=MOON)
    for wl in (ww, we):
        for lat in (6, 16):
            K.gothic_window(wl, lat, GROUND + 10, GROUND + 19, PANES, width=3, glow=MOON)
    for lat in (6, 20):
        K.gothic_window(ws, lat, GROUND + 6, GROUND + 15, PANES, width=3, glow=MOON)
    for lat in (9, 17):
        K.mirror(ws, lat, GROUND + 2, GROUND + 6, width=3)
    for wl in (ww, we):
        for lat in (8, 16):
            portrait(wl, lat, GROUND + 2, GROUND + 5, width=2)
    portrait(wn, 8, GROUND + 2, GROUND + 5, width=2)
    portrait(wn, 18, GROUND + 2, GROUND + 5, width=2)
    grandfather_clock(h, 9, 24)
    grandfather_clock(h, 17, 24)
    for (x, z, w, d) in ((3, 17, 2, 2), (23, 17, 1, 2), (3, 7, 1, 2), (23, 7, 1, 1)):
        sheet(h, x, z, w, d, hgt=2 if w == 2 else 1)
    for x in (12, 13, 14):
        for z in range(20, 25):
            h.try_set(x, GROUND, z, "minecraft:gray_carpet")
    # 4. light: chandeliers over the well and over the door, lanterns under the gallery
    D.chandelier_wheel(h, 13, 8, r=3, drop=top - (GROUND + 7), soul=False)
    D.chandelier_wheel(h, 13, 21, r=2, drop=top - (GROUND + 7), soul=False)
    for (x, z) in ((7, 8), (19, 8), (6, 18), (20, 18)):
        D.chandelier_wheel(h, x, z, r=1, drop=top - (GROUND + 7), soul=False)
    for (x, z) in ((3, 20), (3, 24), (23, 20), (23, 24)):          # beside the side doors
        D.candelabra(h, x, z, soul=True)
    D.candelabra(h, 10, 12, y=GS_LANDING + 1, soul=True)
    D.candelabra(h, 16, 12, y=GS_LANDING + 1, soul=True)
    for z in (4, 9, 13):
        K.hung_lantern_under(h, 3, z, GROUND, GROUND + 3, soul=SOUL)
        K.hung_lantern_under(h, 23, z, GROUND, GROUND + 3, soul=SOUL)
    for x in (7, 11, 13, 15, 19):
        K.hung_lantern_under(h, x, 3, GROUND, GROUND + 3, soul=SOUL)
    D.floor_border(h, 1)
    D.floor_medallion(h, 13, 8, 4)
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("undead", 13, 8)
    K.auto_light(h, soul=SOUL, max_lamps=50, prune=True)
    D.auto_markers(h, spawns=7, patrols=3)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the ballroom
def piano(h, x, z, y=GROUND):
    """A grand piano on the floor row `y`, 3 cells along x and 2 along z: a case of planks with a bottom slab for a lid, a strip of white carpet for the keys in front
    of it and a stool. The keyboard side is the south one (the player sits on the south side of the case)."""
    for dx in range(3):
        for dz in range(2):
            h.set(x + dx, y, z + dz, L.PLANKS)
            h.set(x + dx, y + 1, z + dz, L.PLANKS_SLAB, {"type": "bottom"})
        h.try_set(x + dx, y, z + 2, "minecraft:white_carpet")
    D.chair(h.frame(x + 1, y, z + 3, "north"))


def dancers(h, pairs, yaw_of):
    """Armor stands in pairs, facing each other: a lady in a gown (leather, a skull for a face) and a gentleman (chain mail, the dark skull). `pairs` is a list of
    ((x, z), (x, z)) of cells; `yaw_of(a, b)` gives the yaw of a stand at `a` looking at `b`. Counts against the twelve armor stands of a room."""
    for (a, b) in pairs:
        h.armor_stand(a[0], GROUND, a[1], yaw=yaw_of(a, b), head="minecraft:skeleton_skull", chest="minecraft:leather_chestplate",
                      legs="minecraft:leather_leggings", feet="minecraft:leather_boots", mainhand=None, dy=0.0)
        h.armor_stand(b[0], GROUND, b[1], yaw=yaw_of(b, a), head="minecraft:wither_skeleton_skull", chest="minecraft:chainmail_chestplate",
                      legs="minecraft:chainmail_leggings", feet="minecraft:chainmail_boots", mainhand=None, dy=0.0)


@room("haunted_manor_ballroom", "trial")
def ballroom():
    """The Ballroom: the great room of the house, and its trial. A parquet floor of spruce and pale stone with a star in the middle, a stage at the north end with a
    grand piano and four chairs and stands for the quartet, tall windows of moon-green glass, black mirrors along the walls, and six pairs of dancers in the middle
    of a waltz that stopped. Walk in at either door and the Sealed Archway shuts both; the sigils in the floor let in whoever the house still has (skeletons for the
    dancers, the dead footmen and the hag who kept the kitchen) and the doors open again when the last is down. Same mechanism and checks as the trial halls of
    ROOMS-2; the stage is the room's own, the reward chest stands on it."""
    import mega_decor_r2 as D2
    h = Hall("mega/trial/haunted_manor_ballroom", 3, 3, 14, wear=0.12)
    top = h.top
    doors = (("w", 13), ("e", 13))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    c = 13
    # 1. the floor: a parquet of planks and pale stone inside a border, a star of chiselled stone in the middle
    for z in range(4, 23):
        for x in range(4, 23):
            if (x + z) % 2 == 0:
                h.floor_set(x, z, L.PLANKS)
    D.floor_border(h, 1)
    D.floor_medallion(h, c, c, 5)
    # 2. the trial: ward runes, a ring of glyphs (none under the stage), the keystone
    columns = ((7, 9), (19, 9), (7, 17), (19, 17))
    under_columns = {(cx + dx, cz + dz) for (cx, cz) in columns for dx in (-1, 0, 1) for dz in (-1, 0, 1)}
    glyphs = [g for g in D2.around(c, c, 8, 8, 22.5) if g[1] > 7 and g not in under_columns] + [(13, 22), (8, 13), (18, 13), (13, 17), (11, 16), (15, 16)]
    K.trial_core(h, doors, glyphs, [("minecraft:skeleton", 3), ("minecraft:zombie", 2), ("minecraft:witch", 1)])
    # 3. the stage: a dais at the north end, a piano, a quartet's chairs and stands, the chest, the moon-lit windows behind it
    D.dais(h, 7, 2, 19, 6, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND)
    piano(h, 12, 3, y=GROUND + 1)
    for x in (8, 10, 16, 18):
        D.chair(h.frame(x, GROUND + 1, 4, "south"))
        D.lectern(h.frame(x, GROUND + 1, 5, "north"))
    h.loot_chest(9, GROUND + 1, 3, "south", LOOT)
    for lat in (9, 13, 17):
        K.gothic_window(wn, lat, GROUND + 5, GROUND + 12, PANES, width=3, glow=MOON)
    # 4. mirrors and windows on the other walls, columns, chandeliers
    for lat in (6, 13, 20):
        K.mirror(ws, lat, GROUND + 2, GROUND + 7, width=3)
    for wl in (ww, we):
        for lat in (6, 20):
            K.mirror(wl, lat, GROUND + 2, GROUND + 7, width=3)
        for lat in (9, 17):
            K.gothic_window(wl, lat, GROUND + 5, GROUND + 12, PANES, width=3, glow=MOON)
    for (px, pz) in columns:
        D.column(h, px, pz, 1, y0=GROUND, y1=top)
    for (x, z) in ((10, 11), (16, 11), (10, 20), (16, 20), (4, 16), (22, 16)):     # warm gold lanterns, so that the cold teal of the sconces disagrees with them
        D.chandelier_wheel(h, x, z, r=1, drop=top - (GROUND + 6), soul=False)
    for z in (6, 9, 18, 22):                                       # cold lights on posts along the walls, the teal that disagrees with the gold overhead
        D.candelabra(h, 3, z, soul=True)
        D.candelabra(h, 23, z, soul=True)
    for x in (6, 9, 17, 20):
        D.candelabra(h, x, 23, soul=True)
    # 5. six pairs of dancers in the middle of the floor, away from the lane between the doors
    def yaw_of(a, b):
        dx, dz = b[0] - a[0], b[1] - a[1]
        return {(0, 1): 0.0, (-1, 0): 90.0, (0, -1): 180.0, (1, 0): 270.0}[(int(math.copysign(1, dx)) if dx else 0, int(math.copysign(1, dz)) if dz else 0)]
    pairs = [((9, 9), (10, 9)), ((16, 9), (17, 9)), ((9, 17), (9, 18)), ((16, 17), (17, 17)), ((12, 20), (13, 20)), ((4, 12), (4, 13))]
    dancers(h, pairs, yaw_of)
    D.upper_dress(h, [D.it_tall_banner("black", hang=4)], sides="s", step=(4, 6), door_gap=2)
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("undead", 6, 20)
    K.auto_light(h, soul=SOUL, max_lamps=40, prune=True)
    D.auto_markers(h, spawns=0, patrols=3, centre=False)
    return h


@REG.check("haunted_manor_ballroom")
def ballroom_check(h):
    return K.check_trial(h)


# ----------------------------------------------------------------------------------------------------------------------------------- the nursery
STRIPE_A, STRIPE_B = "minecraft:light_blue_terracotta", "minecraft:white_terracotta"      # the wallpaper: terracotta does not burn and no theme swaps it


def wallpaper(h, sides, y0, y1):
    """Striped paper on the inner layer of the given walls, rows y0..y1, over every cell that is still plain wall (door frames, windows and niches are left as they
    are). Returns how many cells it papered. Why: a nursery is the one room of a house with a pattern on its walls, and the pattern is what makes the horror work."""
    n = 0
    for side in sides:
        w = h.wall(side)
        lo = (h.x0 if side in ("n", "s") else h.z0)
        hi = (h.x1 if side in ("n", "s") else h.z1)
        for lat in range(lo, hi + 1):
            for y in range(y0, y1 + 1):
                pos = w.cell(lat, y, -1)
                c = h.get(*pos)
                if c is None or pos in h.mt.reserved or c.block not in D.WALL_BLOCKS or pos not in h.shell:
                    continue
                w.shell_set(lat, y, -1, STRIPE_A if lat % 2 == 0 else STRIPE_B)
                n += 1
    return n


def crib(h, x, z, axis="z"):
    """A crib 2 wide and 3 long: a base of planks, a fence post at each corner two rows high, a white carpet in the middle row. `axis` is the long direction."""
    cells = [(x + (k if axis == "x" else r), z + (r if axis == "x" else k), k, r) for k in range(3) for r in range(2)]
    for (cx, cz, k, r) in cells:
        h.try_set(cx, GROUND, cz, L.PLANKS)
        if k in (0, 2):
            h.try_set(cx, GROUND + 1, cz, L.FENCE)
            h.try_set(cx, GROUND + 2, cz, L.FENCE)
        else:
            h.try_set(cx, GROUND + 1, cz, "minecraft:white_carpet")


def mobile(h, x, z, drop=4):
    """A mobile over a crib: a chain from the ceiling to a plank block with a bell under it. Why: the brief's nursery; a bell that nobody touched and that rings."""
    top = h.top
    y_block = top - drop
    if all(h.can_set(x, y, z, "minecraft:chain") for y in range(y_block + 1, top + 1)) and h.can_set(x, y_block, z, L.PLANKS) and h.can_set(x, y_block - 1, z, "minecraft:bell"):
        for y in range(y_block + 1, top + 1):
            h.set(x, y, z, L.CHAIN_BLOCK, {"axis": "y"})
        h.set(x, y_block, z, L.PLANKS)
        h.set(x, y_block - 1, z, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": False})


def rocking_horse(h, x, z, axis="z"):
    """A rocking horse three cells long: a rocker of top slabs (so the body rests on them flush and there is a hand of air under the belly), a body of planks, a
    neck and head of stairs rising at the front, a fence post for the ears and a tail of fence at the back."""
    if axis == "z":
        cells = [(x, z), (x, z + 1), (x, z + 2)]
        head_face, tail_face = "north", "south"
    else:
        cells = [(x, z), (x + 1, z), (x + 2, z)]
        head_face, tail_face = "west", "east"
    for (cx, cz) in cells:
        h.try_set(cx, GROUND, cz, L.PLANKS_SLAB, {"type": "top"})
    (hx, hz), (bx, bz), (tx, tz) = cells
    h.try_set(bx, GROUND + 1, bz, L.PLANKS)
    h.try_set(tx, GROUND + 1, tz, L.PLANKS_STAIRS, {"facing": tail_face, "half": "bottom"})
    h.try_set(hx, GROUND + 1, hz, L.PLANKS)
    h.try_set(hx, GROUND + 2, hz, L.PLANKS_STAIRS, {"facing": head_face, "half": "bottom"})
    h.try_set(tx, GROUND + 2, tz, L.FENCE)


def dollhouse(h, x, z):
    """A dolls' house on the floor, 3 x 3 x 3: a floor of planks, walls of white and blue terracotta with a gap for a door, a roof of stairs, a candle lit inside.
    Its tiny front is the house the party is standing in."""
    for dx in range(3):
        for dz in range(3):
            h.try_set(x + dx, GROUND, z + dz, L.PLANKS)
            wall = dx in (0, 2) or dz in (0, 2)
            if wall:
                gap = dx == 1 and dz == 2
                for k in (1, 2):
                    if not (gap and k == 1):
                        h.try_set(x + dx, GROUND + k, z + dz, STRIPE_B if (dx + dz) % 2 else STRIPE_A)
    for dx in range(3):
        for dz in range(3):
            h.try_set(x + dx, GROUND + 3, z + dz, L.PLANKS_SLAB, {"type": "bottom"})
    D.candles_at(h, x + 1, GROUND + 1, z + 1, 1, "yellow")


@room("haunted_manor_nursery", "set")
def nursery():
    """The Nursery: striped wallpaper in blue and white, three cribs with a bell hung over each on a chain, two rocking horses, a dolls' house that is a small copy
    of the room, a rug, a tea table laid for four with four dolls at it, a toy chest, shelves of small things and alphabet blocks on the floor. Nothing in it moves,
    and the horses are still rocking. The house's set piece of small horrors."""
    h = Hall("mega/set/haunted_manor_nursery", 2, 2, 10, wear=0.08)
    top = h.top
    doors = (("s", 13), ("w", 4))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    wallpaper(h, "nsew", GROUND, GROUND + 5)
    D.wall_band(h, GROUND + 6, block=L.PLANKS, accent=L.PLANKS, sides="nsew", plain=(L.BRICK, L.CRACKED_BRICK, L.COBBLED))
    # cribs along the north wall (clear of the west door), a bell over the middle of each, two rocking horses, the dolls' house
    for x in (8, 11, 14):
        crib(h, x, 3, "z")
        mobile(h, x, 4, drop=3)
    rocking_horse(h, 3, 8, "z")
    rocking_horse(h, 15, 12, "z")
    dollhouse(h, 13, 7)
    # the tea table in the south-west: a table for four, a doll at two places and a candle at the others
    D.table(h.frame(5, GROUND, 12, "south"), 2, width=2)                # a 2 x 2 tea table over x 4..5 (a frame facing south grows to its right, the west)
    for (cx, cz, face) in ((3, 12, "east"), (3, 13, "east"), (6, 12, "west"), (6, 13, "west")):
        D.chair(h.frame(cx, GROUND, cz, face))
    for (dx, dz, rot) in ((0, 0, 12), (1, 1, 4)):
        h.set(4 + dx, GROUND + 1, 12 + dz, "minecraft:player_head", {"rotation": str(rot)})
        D.candles_at(h, 5 - dx, GROUND + 1, 12 + dz, 2, "pink")
    h.marker(4, GROUND + 2, 12, "furniture", **{"as": "table"})
    # shelves, a toy chest with a music box on it, alphabet blocks
    D.dress_wall(h, "e", [D.it_shelf(stuff=("candle", "pot"))], step=(3, 4), door_gap=2, lo=11)
    D.dress_wall(h, "w", [D.it_shelf(stuff=("candle", "pot"))], step=(3, 4), door_gap=2, lo=8)
    h.set(3, GROUND, 15, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": False})
    h.set(4, GROUND, 15, L.PLANKS)
    h.set(4, GROUND + 1, 15, "minecraft:jukebox")
    for (x, z, hgt, color) in ((2, 6, 2, "pink"), (3, 6, 1, "light_blue"), (2, 7, 1, "yellow"), (15, 10, 2, "white"), (7, 15, 2, "light_blue")):
        for k in range(hgt):
            h.try_set(x, GROUND + k, z, "minecraft:%s_wool" % color)
    # the rug last: carpet goes round whatever stands on the floor
    for z in range(5, 12):
        for x in range(5, 12):
            d = math.hypot(x - 8, z - 8)
            if d <= 3.2:
                h.try_set(x, GROUND, z, "minecraft:pink_carpet" if d <= 1.2 else "minecraft:white_carpet" if d <= 2.2 else "minecraft:light_blue_carpet")
    for lat in (6, 12):
        K.gothic_window(wn, lat, GROUND + 3, GROUND + 6, PANES, width=1, glow=MOON, pointed=False)
    D.cornice(h)
    D.cobwebs(h, 0.8)
    h.faction("undead", 9, 9)
    K.auto_light(h, soul=SOUL, max_lamps=30, prune=True)
    D.auto_markers(h, spawns=5, patrols=2)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the kitchen
@room("haunted_manor_kitchen", "hall")
def kitchen():
    """The Kitchen: a long range of furnaces and smokers under a brick hood along the north wall, a scrubbed worktable with a chopping block and a brewing stand or
    two that nobody in a house like this should own, herbs and hams hung from the beams, a larder of barrels and shelves against the east wall, and in the west a
    cauldron on a stool of planks with a soul fire beside it, bones and a skull on the lid of a barrel. The hag cooked here, and the house still eats what she made."""
    h = Hall("mega/hall/haunted_manor_kitchen", 2, 2, 10, wear=0.14)
    top = h.top
    doors = (("s", 4), ("e", 13))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.beams(h, "z", [5, 10], y=top)
    # the range: five fire boxes along the north wall (lit when the template is placed) under a hood of brick narrowing to the ceiling
    kinds = ("furnace", "smoker", "blast_furnace", "smoker", "furnace")
    for k, kind in enumerate(kinds):
        h.furnace(5 + k, GROUND, 2, kind, "south")
    for k, y in enumerate(range(GROUND + 2, top + 1)):
        a, b = 4 + k, 10 - k
        if a > b:
            break
        for x in range(a, b + 1):
            h.try_set(x, y, 2, D.brick_mix(h, 0.15, 0.08))
            if y >= GROUND + 3:
                h.try_set(x, y, 3, D.brick_mix(h, 0.15, 0.08))
    h.marker(7, GROUND + 1, 2, "hearth", **{"as": "stove"})
    h.set(4, GROUND, 2, "minecraft:cauldron")
    h.set(10, GROUND, 2, "minecraft:water_cauldron", {"level": "3"})
    # the worktable in the middle: a long table of slabs, a chopping block, stools, a brewing stand on a block of planks
    D.table(h.frame(6, GROUND, 6, "south"), 6)
    h.marker(6, GROUND + 1, 8, "furniture", **{"as": "cutting_board"})
    D.candles_at(h, 6, GROUND + 1, 6, 3, "white")
    for z in (7, 10):
        D.chair(h.frame(5, GROUND, z, "east"))
        D.chair(h.frame(7, GROUND, z, "west"))
    h.set(6, GROUND + 1, 11, "minecraft:skeleton_skull", {"rotation": "8"})
    h.set(9, GROUND, 6, L.PLANKS)
    h.set(9, GROUND + 1, 6, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
    h.set(10, GROUND, 6, L.PLANKS)
    h.set(10, GROUND + 1, 6, "minecraft:brewing_stand", {"has_bottle_0": "false", "has_bottle_1": "true", "has_bottle_2": "false"})
    # the hag's corner: a cauldron on a stool of planks, a soul fire beside it, bones and a barrel
    h.set(3, GROUND, 7, L.PLANKS)
    h.set(3, GROUND + 1, 7, "minecraft:cauldron")
    h.set(4, GROUND, 7, "minecraft:soul_campfire", {"facing": "north", "lit": True, "signal_fire": False, "waterlogged": False})
    D.bones(h, 3, 9, rot=5, big=True)
    h.set(2, GROUND, 5, "minecraft:barrel", {"facing": "up", "open": False})
    h.set(2, GROUND + 1, 5, "minecraft:skeleton_skull", {"rotation": "4"})
    # the larder on the east wall: barrels two high, a shelf, herbs and hams from the beams
    for z in (3, 4, 5, 6):
        h.set(15, GROUND, z, "minecraft:barrel", {"facing": "up", "open": False})
        if z % 2:
            h.set(15, GROUND + 1, z, "minecraft:barrel", {"facing": "west", "open": False})
    D.dress_wall(h, "e", [D.it_shelf(stuff=("candle", "pot", "brew"))], step=(3, 4), door_gap=2, lo=3)
    for (x, z) in ((6, 4), (8, 4), (11, 8), (9, 9), (12, 11), (3, 8)):
        D.herb_bundle(h, x, z, drop=2, kind="hanging_roots")
    for (x, z) in ((4, 5), (12, 5), (8, 12)):
        D.hang_lantern(h, x, z, drop=3, soul=False)
    D.cornice(h)
    D.cobwebs(h, 1.0)
    h.faction("undead", 9, 11)
    K.auto_light(h, soul=SOUL, max_lamps=30, prune=True)
    D.auto_markers(h, spawns=5, patrols=2)
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the master bedroom
def four_poster(h, x, z, facing="south", color="red"):
    """A four-poster bed: a bed with its foot at (x, z) and its head one cell on in `facing`, a post of stripped log three high at each of the four corners round it,
    a canopy of bottom slabs on the posts, a footlocker at the foot and a night stand with a candle at the head. Returns nothing; the bed carries a `bed` marker."""
    f = h.frame(x, GROUND, z, facing)
    D.bed(f, color=color)
    for r in (-1, 1):
        for ff in (-1, 2):
            for u in range(3):
                f.try_set(r, u, ff, L.STRIPPED_LOG, {"axis": "y"})
    for r in (-1, 0, 1):
        for ff in (-1, 0, 1, 2):
            f.try_set(r, 3, ff, L.PLANKS_SLAB, {"type": "bottom"})


@room("haunted_manor_master_bedroom", "safe")
def master_bedroom():
    """The Master Bedroom: the one room of the house where nothing is wrong. A four-poster bed under a canopy, a fire in a hearth with two armchairs drawn up to it, a
    wardrobe, a dressing table with a mirror, a rug, candles, a footlocker, a window with the moon in it. The rest anchors are the bed, the armchairs and the hearth;
    no marker of a monster is in it. It is a safe room, and it is the safest looking."""
    h = Hall("mega/safe/haunted_manor_master_bedroom", 2, 2, 10, wear=0.04)
    top = h.top
    doors = (("s", 13), ("w", 4))
    for side, off in doors:
        h.door(side, off)
        D.door_trim(h, side, off)
    wn, ws, ww, we = h.wall("n"), h.wall("s"), h.wall("w"), h.wall("e")
    D.floor_border(h, 1)
    # the hearth in the north wall with two armchairs turned to it
    fire = D.fireplace(wn, 7, width=3, soul=False)
    h.marker(fire[0], fire[1] + 1, fire[2], "hearth", **{"as": "stove"})
    D.chair(h.frame(7, GROUND, 5, "north"), seat_marker=True)
    D.chair(h.frame(9, GROUND, 5, "north"), seat_marker=True)
    # the bed in the north-east corner, the footlocker at its foot, a night stand with a candle
    four_poster(h, 13, 4, "south", color="red")
    h.set(13, GROUND, 8, "minecraft:chest", {"facing": "north", "type": "single", "waterlogged": False})
    h.set(11, GROUND, 4, L.PLANKS)
    D.candles_at(h, 11, GROUND + 1, 4, 3, "white")
    # the wardrobe against the west wall, the dressing table and its mirror along the south wall
    for y in range(GROUND, GROUND + 3):
        for z in (8, 9):
            h.try_set(3, y, z, L.PLANKS)
    for z in (8, 9):
        h.try_set(4, GROUND + 1, z, L.TRAPDOOR, {"facing": "east", "half": "bottom", "open": False})
    D.table(h.frame(4, GROUND, 14, "east"), 3)
    D.chair(h.frame(5, GROUND, 13, "south"))
    D.candles_at(h, 4, GROUND + 1, 14, 2, "white")
    D.candles_at(h, 6, GROUND + 1, 14, 2, "white")
    K.mirror(ws, 5, GROUND + 2, GROUND + 5, width=3)
    # a rug of red and white under the bed and the chairs, laid round whatever stands on the floor; a window with the moon in it
    for z in range(5, 12):
        for x in range(5, 12):
            edge = min(x - 5, 11 - x, z - 5, 11 - z)
            h.try_set(x, GROUND, z, "minecraft:red_carpet" if edge == 0 or (edge == 1 and (x + z) % 2) else "minecraft:white_carpet")
    K.gothic_window(we, 9, GROUND + 3, GROUND + 7, PANES, width=3, glow=MOON)
    D.chandelier_wheel(h, 8, 9, r=2, drop=3, soul=False)
    D.cornice(h)
    D.cobwebs(h, 0.2)
    D.upper_dress(h, [D.it_tall_banner("white", hang=3)], sides="s", step=(4, 6), door_gap=2)
    K.auto_light(h, soul=False, max_lamps=20, prune=True)
    h.marker(9, GROUND, 12, "patrol")
    return h


# ----------------------------------------------------------------------------------------------------------------------------------- the attic
def pitched_roof(h, y0, step=1):
    """The slope of a gabled roof seen from inside: from row `y0` up, every row the planks come `step` cells further in from the two long walls (west and east), the
    innermost cell of each row an upside-down stair turned to the middle, so the underside of the roof is a clean slope. Why: an attic is a room under a roof, and the
    one thing that says so is the slant; the rows start above head height and leave the doorways and the pit field alone."""
    for k, y in enumerate(range(y0, h.top + 1)):
        for x0, face, sgn in ((h.x0, "east", 1), (h.x1, "west", -1)):
            for d in range(0, (k + 1) * step):
                x = x0 + sgn * d
                for z in range(h.z0, h.z1 + 1):
                    if d == (k + 1) * step - 1:
                        h.try_set(x, y, z, L.PLANKS_STAIRS, {"facing": face, "half": "top"})
                    else:
                        h.try_set(x, y, z, L.PLANKS)


def trunk(h, x, z, axis="x"):
    """An old trunk: a block of log with a lid of one bottom slab on it. (Not a chest: the pit check counts the chests of a trap room, and the one in this attic is the
    reward on the island.)"""
    h.try_set(x, GROUND, z, L.LOG, {"axis": axis})
    h.try_set(x, GROUND + 1, z, L.PLANKS_SLAB, {"type": "bottom"})


@room("haunted_manor_attic", "trap")
def attic():
    """The Attic: a long room under a pitched roof, with tie beams and a skylight of moon-green glass, trunks and boxes along the walls, a dressmaker's dummy, a
    spinning wheel, a crib with a doll in it, dust sheets over the rest. The floorboards are the trap: a field of boards that look alike (the mod's pitfall tiles,
    the same blocks, the same spiked chamber and ladder, the same checks as the pit halls of ROOMS-2), one thin zig-zag of them sound and leading to a trunk on an island
    in the middle, and a lantern on a chain over every turn of the safe way. A wrong step drops three blocks into the room below, never the way back."""
    import gen_mega_rooms2 as R2
    import mega_decor_r2 as D2
    h = Hall("mega/trap/haunted_manor_attic", 3, 3, 14, wear=0.18)
    mt = h.mt
    top = h.top
    for side in ("n", "s", "w"):
        h.door(side, 13)
        D.door_trim(h, side, 13)
    rng = h.rng
    for z in range(mt.D):                                      # the solid slab under the floor: the chamber and the shaft are carved out of it afterwards
        for x in range(mt.W):
            for y in range(R2.PIT_CHAMBER_FLOOR - 1, FLOOR):
                mt.set(x, y, z, rng.choice((L.COBBLED, L.COBBLED, L.ROCK, L.BRICK)))
    field = {(x, z) for x in range(7, 20) for z in range(7, 20)}
    island = {(x, z) for x in range(12, 15) for z in range(12, 15)}
    goals = {(x, z) for (x, z) in field - island if any((x + dx, z + dz) in island for dx, dz in L.STEP.values())}
    route = D2.snake_path(rng, (rng.randint(9, 17), 19), goals, field - island, min_len=16)
    if route is None:
        raise ValueError("no safe route found for the attic")
    R2.pit_chamber(mt, field, set(route) | island, rng, ladder=(21, 13))
    for (x, z) in sorted(field):
        h.shell.discard((x, FLOOR, z))
    h.shell.discard((21, FLOOR, 13))
    # the roof, the tie beams, the skylight
    pitched_roof(h, GROUND + 6)
    D.beams(h, "x", [5, 9, 17, 21], y=GROUND + 5, corbel=None)
    for (x, z) in ((11, 5), (15, 5), (11, 21), (15, 21)):
        mt.remove(x, top + 1, z)
        h.shell.discard((x, top + 1, z))
        h.set(x, top + 1, z, "minecraft:light_gray_stained_glass")
        mt.set(x, top + 2, z, MOON)
    # floorboards on the ledge (the field is bare), the island: a trunk with candles and a doll
    for z in range(2, 25):
        for x in range(2, 25):
            if (x, z) in field or (x, z) == (21, 13):
                continue
            if (x + z) % 2 == 0 and rng.random() < 0.6:
                h.floor_set(x, z, L.PLANKS)
    D.dais(h, 12, 12, 14, 14, rows=1, top=L.PLANKS, step=L.PLANKS_STAIRS, corner=L.LOG, y=GROUND)
    h.loot_chest(13, GROUND + 1, 13, "south", LOOT)
    for dx, dz in ((-1, -1), (1, -1), (-1, 1)):
        D.candles_at(h, 13 + dx, GROUND + 1, 13 + dz, 3, "white")
    h.set(14, GROUND + 1, 14, "minecraft:player_head", {"rotation": "4"})
    # the things of an attic along the walls: only in the outer two cells of the ledge, so that three cells of it stay free for the lane that joins the doors
    for (x, z, ax) in ((3, 4, "z"), (3, 8, "z"), (3, 18, "z"), (3, 22, "z"), (24, 8, "z"), (24, 18, "z"), (24, 21, "z"), (9, 2, "x"), (17, 2, "x"), (8, 24, "x"), (18, 24, "x")):
        trunk(h, x, z, ax)
    for (x, z, w, d, hgt) in ((2, 5, 1, 2, 1), (23, 21, 2, 2, 1), (2, 20, 2, 1, 2), (6, 2, 2, 1, 1), (21, 23, 2, 1, 1)):
        sheet(h, x, z, w, d, hgt)
    for (x, z) in ((11, 2), (15, 2), (11, 24), (15, 24)):
        for k in range(2):
            h.try_set(x, GROUND + k, z, "minecraft:barrel", {"facing": "up", "open": False})
    h.armor_stand(3, GROUND, 11, yaw=90.0, head=None, chest="minecraft:leather_chestplate", legs=None, feet=None, mainhand=None, dy=0.0)
    crib(h, 23, 4, "z")
    # the hint: a lantern on a chain over every turn of the safe way
    for i in range(1, len(route) - 1):
        a, b, c = route[i - 1], route[i], route[i + 1]
        if (b[0] - a[0], b[1] - a[1]) != (c[0] - b[0], c[1] - b[1]):
            for yy in range(GROUND + 5, top + 1):
                h.try_set(b[0], yy, b[1], L.CHAIN_BLOCK, {"axis": "y"})
            h.try_set(b[0], GROUND + 4, b[1], L.LANTERN, {"hanging": True})
    for (x, z) in sorted(field - set(route)):
        if rng.random() < 0.04 and h.free(x, top, z):
            D2.dripstone(mt, x, top, z, rng.choice((1, 2)), "down")
    for (x, z) in ((3, 3), (23, 3), (3, 23), (23, 23), (3, 10), (10, 3), (16, 3), (10, 23), (16, 23)):
        D.candelabra(h, x, z, soul=False)
    for (x, z) in ((6, 9), (6, 17), (20, 9), (20, 17), (4, 5), (22, 5), (4, 21), (22, 21), (9, 5), (17, 5), (9, 21), (17, 21)):       # lanterns hung under the tie beams
        K.hung_lantern_under(h, x, z, GROUND, GROUND + 3, soul=False)
    D.cornice(h, skip_sides=("w", "e"))
    D.cobwebs(h, 1.0)
    h.faction("undead", 11, 22)
    for (x, z) in route[2::4][:4]:
        h.marker(x, GROUND, z, "trap", type="pit")
    K.auto_light(h, soul=SOUL, max_lamps=40, prune=True)
    D.auto_markers(h, spawns=6, patrols=2)
    return h


@REG.check("haunted_manor_attic")
def attic_check(h):
    """The pit halls' own check (a safe route of tiles from the ledge to the chest, most of the field is pits, a sealed chamber with spikes and a way out)."""
    import gen_mega_rooms2 as R2
    import mega_decor_r2 as D2
    return ["pit check: " + e for e in R2.check_pit(h.mt, D2.Analysis(h.mt), None)]


if __name__ == "__main__":
    sys.exit(K.main(REG, sys.argv[1:], calm=CALM))

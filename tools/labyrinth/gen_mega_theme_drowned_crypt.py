#!/usr/bin/env python3
"""
Why: the DROWNED_CRYPT theme (1.59.0, THEME-D, docs/labyrinth/mega_v2_thd.md): a burial vault the sea took back. A themed sector must read as a NEW place, so besides the palette swap
of `data/nusmp/mega/themes/drowned_crypt.json` (dark prismarine walls, mossy stone brick floors, deepslate brick pillars, cold soul lights) it owns its signature rooms, which the assembler
may draw only in sectors of this theme (`data/nusmp/mega/template_themes/drowned_crypt.json`, prefix `nusmp:mega/<folder>/drowned_crypt_`).

    python3 tools/labyrinth/gen_mega_theme_drowned_crypt.py                 # build, check and write every room
    python3 tools/labyrinth/gen_mega_theme_drowned_crypt.py flooded_nave    # only this one
    python3 tools/labyrinth/gen_mega_theme_drowned_crypt.py --check         # build and check in memory, compare with the files on disk (CI style)

Rooms: flooded_nave (hall, two storeys), sarcophagus_vault (hall), tide_gate (puzzle), drowned_font (trial), drowned_chapel (set piece), air_bell (safe), pearl_reliquary (treasure).
The rules of the water in every one of them: a water source flows into any open neighbour, so a flooded area is a basin carved out of a solid slab (the rows under the floor are rock), every
side and the bed of every water cell is a full block or more water (stairs, bars and chains that stand in water are waterlogged), no water touches a doorway, and every walking surface of
a room is DRY (the lead's walking model treats a water cell as a wall): the party walks on causeways, galleries and dais, the flood is what lies between them, and a swimmer can always climb
out (the water's surface is at the level of the causeways or a step under it). Deterministic: every random choice comes from the template's own name (`MT.rng`).
"""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r2 as D  # noqa: E402
import thd_kit as K  # noqa: E402
import gen_mega_rooms2 as G2  # noqa: E402
from thd_kit import FR, GROUND, put, box, st, dist, candles  # noqa: E402

ID = "drowned_crypt"
ROOMS = []   # (folder, name, builder, spec)
BED = 3      # the bed row of every flood: rows 1..3 are rock, the water fills rows 4..FR (the surface is level with the causeways' top face minus a hair)
GLASS = ("minecraft:cyan_stained_glass", "minecraft:light_blue_stained_glass", "minecraft:blue_stained_glass")


def room(folder, name, **spec):
    """Registers a builder `fn(full_name) -> MT`; `spec` is what the checks expect of the room (see gen_mega_rooms2.check_room)."""
    def deco(fn):
        ROOMS.append((folder, ID + "_" + name, fn, spec))
        return fn
    return deco


def tpl(folder, name, mx, mz):
    return K.new(folder, name, mx, mz)


def pillar_in_flood(mt, x, z, y_top, bed=BED, shaft_from=None):
    """A pillar that stands in the flood: plain brick from the bed up to the floor row (the water hides it), a chiselled foot just over the surface, a polished shaft to `y_top` and
    a capital (the kit's column). A pillar is written AFTER the flood, so it replaces the water cells it stands in."""
    for y in range(bed + 1, FR + 1):
        mt.set(x, y, z, L.BRICK)
    mt.set(x, FR + 1, z, L.CHISELED)
    D.column(mt, x, z, FR + 2, y_top, base=False)


def benches(mt, x0, x1, z_rows, bed=BED, rng=None):
    """Rows of plank benches on the bed of a flood (the old congregation's pews, full blocks only: a stair or a slab next to water is refused by the lead's arch check): a seat row one
    block high with a taller end post every fifth cell, a gap in the middle of the row. Only cells that are water right above the bed are used."""
    n = 0
    for z in z_rows:
        for x in range(x0, x1 + 1):
            if (x - x0) % 6 == 5:
                continue
            c = mt.get(x, bed + 1, z)
            if c is not None and c.block == K.WATER and mt.get(x, bed, z) is not None:
                mt.set(x, bed + 1, z, L.PLANKS)
                if (x - x0) % 6 in (0, 4):
                    c2 = mt.get(x, bed + 2, z)
                    if c2 is not None and c2.block == K.WATER:
                        mt.set(x, bed + 2, z, L.LOG, {"axis": "y"})
                n += 1
    return n


def bed_growth(mt, cells, rng, moss=0.10, bones=0.025, coral=0.05, bed=BED):
    """What lies on the bed of a flood, as full blocks only (no plant may touch a water block under the lead's checks): patches of moss (the green of the weed), crusts of dead coral and
    heaps of bone blocks (the drowned congregation), written into the bed row itself or one block above it when that cell is water. Returns (moss, coral, bones) counts."""
    cnt = [0, 0, 0]
    for (x, z) in sorted(cells):
        r = rng.random()
        if r < moss:
            mt.set(x, bed, z, "minecraft:moss_block")
            cnt[0] += 1
        elif r < moss + coral:
            mt.set(x, bed, z, rng.choice(("minecraft:dead_tube_coral_block", "minecraft:dead_brain_coral_block", "minecraft:dead_horn_coral_block")))
            cnt[1] += 1
        elif r < moss + coral + bones:
            c = mt.get(x, bed + 1, z)
            if c is not None and c.block == K.WATER:
                mt.set(x, bed + 1, z, "minecraft:bone_block", {"axis": rng.choice("xyz")})
                cnt[2] += 1
    return tuple(cnt)


def bed_lamps(mt, cells, rng, count, bed=BED, min_gap=5.0):
    """Sea lanterns set into the bed: they glow up through the water (the water costs the light one extra level per cell, so these are a mood, not a light source)."""
    pool = sorted(cells)
    rng.shuffle(pool)
    placed = []
    for (x, z) in pool:
        if len(placed) >= count:
            break
        if any(dist(x, z, px, pz) < min_gap for (px, pz) in placed):
            continue
        c = mt.get(x, bed, z)
        if c is not None and c.block != K.WATER and mt.get(x, bed + 1, z) is not None and mt.get(x, bed + 1, z).block == K.WATER:
            mt.set(x, bed, z, "minecraft:sea_lantern")
            placed.append((x, z))
    return placed


def barnacles(mt, rng, count, y0=FR + 1, y1=FR + 6):
    """Barnacle crust on the walls: dead coral blocks swapped into the inner wall layer at random places (dead coral is inert: no reef, no water needed)."""
    n = 0
    for _ in range(count * 6):
        if n >= count:
            break
        side = rng.choice(L.SIDES)
        lat = rng.randint(3, (mt.W if side in ("n", "s") else mt.D) - 4)
        y = rng.randint(y0, y1)
        x, yy, z = mt._lateral(side, 0, lat, 1, y)
        c = mt.get(x, yy, z)
        if c is not None and c.block == L.BRICK and (x, yy, z) not in mt.reserved:
            mt.set(x, yy, z, rng.choice(("minecraft:dead_tube_coral_block", "minecraft:dead_brain_coral_block", "minecraft:dead_horn_coral_block")))
            n += 1
    return n


def water_mark(mt, rows, block=L.POLISHED):
    """A band of `block` in the inner layer of every wall at the given rows: the line the sea left on the stone. Skips the doorways (the frame posts reach two cells either side of an
    offset) and every wall cell that is not plain brick (windows, niches, the partition of a closet)."""
    n = 0
    for side in L.SIDES:
        length = mt.W if side in ("n", "s") else mt.D
        for lat in range(2, length - 2):
            if any(sd == side and abs(off - lat) <= 3 for sd, off, _kw in mt.sockets):
                continue
            for y in rows:
                x, yy, z = mt._lateral(side, 0, lat, 1, y)
                c = mt.get(x, yy, z)
                if c is not None and c.block in (L.BRICK, L.CRACKED_BRICK) and (x, yy, z) not in mt.reserved:
                    mt.set(x, yy, z, block)
                    n += 1
    return n


# =====================================================================================================================
# 1. flooded_nave: a basilica drowned to its floor, two storeys
# =====================================================================================================================
@room("hall", "flooded_nave", faction="aquatic", strict_light=True)
def flooded_nave(name):
    """Why: the brief's 'sunken crypt' as the big room of the theme: a basilica the sea took back, 23 across and 32 long under a roof 27 high. The nave is flooded; what the party walks on is
    a processional way five wide that runs from the south door to the altar (and two cross strips to the side doors), with the flood on both sides of it at the level of its floor (a swimmer
    climbs out anywhere), the benches of the old congregation standing under the water, kelp growing from the bed to the surface and sea pickles glowing on it. The second storey is the choir
    galleries: two decks, four across, along the side walls over the flooded aisles, reached by two flights of eight steps from the cross strips, with a parapet toward the nave. Over them a
    clerestory of eleven lancet windows of blue stained glass that glow by themselves (a sea lantern behind each), at the far end a rose window over the altar, and between the roof beams six
    chandeliers of cold lanterns on very long chains, which are what lights the way. The aquatic faction (the drowned, the lizardfolk) lives in the flood and comes out onto the way."""
    mt = tpl("hall", name, 3, 4)
    rng = mt.rng
    H = 26
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.12)
    top = FR + H
    for (side, off) in (("s", 13), ("w", 22), ("e", 22)):
        mt.socket(side, off)
    K.under_floor(mt, rng)
    way = G2.rect(11, 15, 9, 33)
    strips = G2.rect(2, 10, 20, 24) | G2.rect(16, 24, 20, 24)
    dais = G2.rect(9, 17, 2, 8)
    flights = G2.rect(2, 4, 12, 19) | G2.rect(22, 24, 12, 19)
    dry = way | strips | dais | flights
    interior = G2.rect(2, 24, 2, 33)
    wet = interior - dry
    K.flood(mt, wet, BED, FR)
    # ---- the way: a runner of polished plates with chiselled studs, edged in polished stone
    for (x, z) in sorted(way | strips):
        if x in (11, 15) and (z % 2 == 0):
            mt.set(x, FR, z, L.POLISHED)
        elif x == 13 and z % 4 == 1:
            mt.set(x, FR, z, L.CHISELED)
    # ---- the flights up to the galleries (the mass under them is brick) and the decks
    for west in (True, False):
        gx = 2 if west else 22
        K.ramp(mt, gx, 19, "north", 8, FR + 1, width=3, stair=L.TILE_STAIRS, fill=L.BRICK, rail_left=not west, rail_right=west)
        dx0, dx1 = (2, 5) if west else (21, 24)
        for z in range(2, 12):
            for x in range(dx0, dx1 + 1):
                mt.set(x, FR + 8, z, L.TILE if (x + z) % 3 else L.POLISHED)
        edge = 5 if west else 21
        for z in range(2, 12):
            put(mt, edge, FR + 9, z, L.POLISHED_WALL, {"up": "true"})
            put(mt, edge, FR + 10, z, L.IRON_BARS)
        for z in (3, 7, 11):
            pillar_in_flood(mt, edge, z, FR + 7)
    # ---- the arcade: two lines of pillars either side of the way, up to the roof beams
    for z in (12, 17, 28, 32):
        for x in (9, 17):
            pillar_in_flood(mt, x, z, top - 2)
    # ---- the dais: three steps up to a platform, cheeks, the altar between candles, two armoured priests, the chest behind
    for x in range(10, 17):
        put(mt, x, FR + 1, 8, L.TILE_STAIRS, st("north"))
        put(mt, x, FR + 1, 7, L.BRICK)
        put(mt, x, FR + 2, 7, L.TILE_STAIRS, st("north"))
        for y in (FR + 1, FR + 2):
            put(mt, x, y, 6, L.BRICK)
        put(mt, x, FR + 3, 6, L.TILE_STAIRS, st("north"))
    for z in range(2, 6):
        for x in range(10, 17):
            box(mt, x, x, FR + 1, FR + 3, z, z, L.BRICK)
    for x in (9, 17):
        for z in range(2, 9):
            rows = FR + 3 if z <= 6 else (FR + 2 if z == 7 else FR + 1)
            box(mt, x, x, FR + 1, rows, z, z, L.BRICK)
    for x in range(10, 17):
        for z in range(2, 6):
            mt.set(x, FR + 3, z, L.TILE if (x + z) % 2 else L.POLISHED)
    for x in (12, 13, 14):
        put(mt, x, FR + 4, 4, L.POLISHED)
        put(mt, x, FR + 4, 3, L.CHISELED)
    candles(mt, 11, FR + 4, 4, 3, "light_blue")
    candles(mt, 15, FR + 4, 4, 3, "light_blue")
    mt.t.chest(13, FR + 4, 2, K.LOOT, "south")
    for x in (10, 16):
        L.statue(mt, x, FR + 4, 5, "south", head="minecraft:diamond_helmet", chest="minecraft:diamond_chestplate", legs="minecraft:diamond_leggings",
                 feet="minecraft:diamond_boots", weapon="minecraft:trident")
    # ---- the rose window over the altar: glass in the inner layer, sea lanterns behind
    rose = (13, FR + 16)
    for x in range(5, 22):
        for y in range(FR + 8, FR + 25):
            d = math.hypot(x - rose[0], y - rose[1])
            if d > 6.6:
                continue
            ang = (math.degrees(math.atan2(y - rose[1], x - rose[0])) + 360.0) % 360.0
            if d <= 1.5:
                mt.set(x, y, 1, L.CHISELED)
                mt.set(x, y, 0, L.BRICK)
            elif d >= 5.5 or min(abs(((ang - 45.0 * k + 180.0) % 360.0) - 180.0) for k in range(8)) * math.radians(1.0) * d < 0.62:
                mt.set(x, y, 1, L.POLISHED)
                mt.set(x, y, 0, L.BRICK)
            else:
                mt.set(x, y, 1, GLASS[(int(d) + int(ang / 45.0)) % 3])
                mt.set(x, y, 0, "minecraft:sea_lantern")
    # ---- clerestory: lancet windows between the pillars on both side walls
    for lat in (6, 11, 16, 28, 32):
        for side in ("w", "e"):
            K.lancet(mt, side, lat, FR + 12, width=3, height=11, glass=GLASS)
    # ---- the roof: cross beams over every arcade bay, two long beams, chandeliers on long chains over the way
    for z in (8, 14, 20, 26, 32):
        for x in range(2, 25):
            put(mt, x, top, z, L.POLISHED)
    for x in (9, 17):
        for z in range(2, 34):
            put(mt, x, top, z, L.POLISHED)
    for z in (11, 17, 24, 29, 33):
        L.chandelier(mt, 13, top + 1, z, drop=16, radius=2, lantern=L.LANTERN)
    # ---- the flood: benches on the bed, moss, coral crust and bones, sea lanterns in the bed
    for (x0, x1) in ((3, 8), (18, 23)):
        benches(mt, x0, x1, (12, 15, 18, 27, 30), rng=rng)
    water_cols = {(x, z) for (x, z) in wet if all((mt.get(x, y, z) is not None and mt.get(x, y, z).block == K.WATER) for y in range(BED + 1, FR + 1))}
    bed_growth(mt, water_cols, rng)
    bed_lamps(mt, water_cols, rng, 10)
    # ---- the walls: barnacles, banners over the strips, lamps on the way
    barnacles(mt, rng, 70)
    for (x, z) in ((11, 14), (15, 14), (11, 24), (15, 24), (11, 30), (15, 30)):
        if mt.get(x, GROUND, z) is None and mt.get(x, FR, z) is not None and mt.get(x, FR, z).block != K.WATER:
            D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.LANTERN)
    # ---- markers: spawns on the way and the strips and the galleries, patrols along the way
    K.auto_markers(mt, "aquatic", spawns=8, patrols=3, spacing=6, avoid=[(13, 5, 6)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 2. sarcophagus_vault: the dry crypt, low, dripping, the dead in stone boxes
# =====================================================================================================================
def sarcophagus(mt, x0, z0, length=6, width=3, kelp_grown=False, rng=None, lid_gaps=0.12):
    """A stone sarcophagus `length` long (along x) and `width` across (along z) with its footprint's minimum corner at (x0, z0): a body of two rows of brick with a trough one cell wide in
    the middle (the cells between the two end blocks), a lid of slabs over everything but the trough, a chiselled effigy block at the head end. A KELP-GROWN one has its trough full of
    kelp (a kelp plant two cells tall that never grows, standing in the trough's own water: the plant holds the water, so the trough is walled on every side by full blocks and no plain water
    block touches it); the others hold a skull and a bone block. Slabs of the lid are left out at random (`lid_gaps`): the lids have been shifted by whatever is inside."""
    rng = rng or mt.rng
    mid = z0 + width // 2
    trough = {(x, mid) for x in range(x0 + 1, x0 + length - 1)}
    for z in range(z0, z0 + width):
        for x in range(x0, x0 + length):
            if (x, z) in trough:
                continue
            for y in (GROUND, GROUND + 1):
                mt.set(x, y, z, L.BRICK if (x, z) != (x0, mid) else L.CHISELED)
            if rng.random() > lid_gaps:
                mt.set(x, GROUND + 2, z, L.POLISHED_SLAB, {"type": "bottom"})
    if kelp_grown:
        for (x, z) in trough:
            K.kelp(mt, x, GROUND - 1, z, GROUND + 1)
    else:
        cells = sorted(trough)
        mt.set(cells[0][0], GROUND, cells[0][1], "minecraft:skeleton_skull", {"rotation": str(rng.choice((4, 12)))})
        mt.set(cells[1][0], GROUND, cells[1][1], "minecraft:bone_block", {"axis": "x"})
    return trough


def niche(mt, side, lat, y, rng, candle=False):
    """A burial niche cut into the inner layer of the wall `side` at lateral position `lat`, two cells high from row y, with a skull on its floor looking into the room (the outer layer of the
    wall stays, so the niche is a recess and not a hole) and sometimes a candle beside it."""
    x, yy, z = mt._lateral(side, 0, lat, 1, y)
    if (x, yy, z) in mt.reserved or (x, yy + 1, z) in mt.reserved:
        return False
    c = mt.get(x, yy, z)
    if c is None or c.block not in (L.BRICK, L.CRACKED_BRICK):
        return False
    mt.remove(x, yy, z)
    mt.remove(x, yy + 1, z)
    rot = {"n": 0, "s": 8, "w": 4, "e": 12}[side]
    mt.set(x, yy, z, "minecraft:skeleton_skull" if not candle else "minecraft:%s_candle" % rng.choice(("white", "light_blue")),
           {"rotation": str(rot)} if not candle else {"candles": str(rng.randint(1, 3)), "lit": "true", "waterlogged": "false"})
    return True


@room("hall", "sarcophagus_vault", faction="undead", strict_light=True)
def sarcophagus_vault(name):
    """Why: the second hall of the theme has to feel nothing like the first: where the flooded nave is a cathedral (tall, open, lit from above, water to the floor), the sarcophagus vault is the
    crypt under it: 23 across and nine high, every sound a drip. A cross of five-wide lanes joins the four doors; in the four quarters between them lie eight stone sarcophagi in two rows
    (two thick rows of brick under a lid of slabs, an effigy at the head), four of them lidded open to show a trough of kelp (the sea has got in, and the kelp has grown where the dead
    lie), the others holding a skull and a bone; twelve pillars flank the lanes with lanterns on corbels, the walls are pitted with burial niches that each keep a skull or a candle, stalactites
    drip over the open coffins and into sunken puddles in the floor, barnacles crust the walls, and over the crossing hangs one ring of cold lanterns. The dead (undead faction) lie in wait
    between the coffins; the lanes are where the party can stand together."""
    mt = tpl("hall", name, 3, 3)
    rng = mt.rng
    H = 9
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.18)
    top = FR + H
    for side in L.SIDES:
        mt.socket(side, 13)
    lanes = G2.rect(11, 15, 2, 24) | G2.rect(2, 24, 11, 15)
    # ---- floor: a ribbon down the middle of both lanes, studs of chiselled stone where they cross
    for (x, z) in sorted(lanes):
        if x == 13 and z % 3 == 1 or z == 13 and x % 3 == 1:
            mt.set(x, FR, z, L.POLISHED)
    for (x, z) in G2.rect(12, 14, 12, 14):
        mt.set(x, FR, z, L.CHISELED if (x + z) % 2 == 0 else L.POLISHED)
    # ---- eight sarcophagi, four of them kelp-grown
    plan = [((3, 3), True), ((3, 7), False), ((18, 3), False), ((18, 7), True), ((3, 17), False), ((3, 21), True), ((18, 17), True), ((18, 21), False)]
    troughs = []
    for (x0, z0), kelp_grown in plan:
        t = sarcophagus(mt, x0, z0, kelp_grown=kelp_grown, rng=rng)
        if kelp_grown:
            troughs.append(t)
    # ---- the pillars along the lanes
    pillars = [(x, z) for x in (10, 16) for z in (5, 10, 16, 21)] + [(x, z) for z in (10, 16) for x in (5, 21)]
    for (px, pz) in pillars:
        D.column(mt, px, pz, GROUND, top - 1)
    for (px, pz) in pillars:
        toward = D.stair_facing(13 - px, 13 - pz)
        D.pillar_lantern(mt, px, pz, toward, FR + 4, lantern=L.LANTERN)
    # ---- roof ribs and the ring of lanterns over the crossing
    for x in (10, 16):
        for z in range(2, 25):
            put(mt, x, top, z, L.POLISHED)
    for z in (10, 16):
        for x in range(2, 25):
            put(mt, x, top, z, L.POLISHED)
    L.chandelier(mt, 13, top + 1, 13, drop=3, radius=1, lantern=L.LANTERN)
    # ---- drip: stalactites over the open coffins and over four sunken puddles
    for t in troughs:
        xs = sorted(x for (x, _z) in t)
        z = list(t)[0][1]
        D.dripstone(mt, xs[len(xs) // 2], top, z, rng.randint(2, 3), "down")
    free_cells = [(x, z) for z in range(2, 25) for x in range(2, 25)
                  if (x, z) not in lanes and mt.get(x, GROUND, z) is None and mt.get(x, FR, z) is not None and mt.get(x, FR, z).block in (L.TILE, L.CRACKED_TILE)
                  and all(mt.get(x + dx, GROUND, z + dz) is None for dx in (-1, 0, 1) for dz in (-1, 0, 1))]
    rng.shuffle(free_cells)
    puddles = []
    for (x, z) in free_cells:
        if len(puddles) >= 6:
            break
        if all(dist(x, z, px, pz) > 4 for (px, pz) in puddles) and all(mt.get(x + dx, FR, z + dz) is not None and mt.get(x + dx, FR, z + dz).block != K.WATER
                                                                  for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            mt.set(x, FR - 1, z, L.ROCK)
            mt.set(x, FR, z, K.WATER, K.WATER_PROPS)
            puddles.append((x, z))
            D.dripstone(mt, x, top, z, rng.randint(1, 3), "down")
    # ---- the walls: burial niches (two tiers) between the doors, barnacles, a few candles
    for side in L.SIDES:
        for lat in (4, 6, 8, 18, 20, 22):
            for k, y in enumerate((GROUND, GROUND + 3)):
                niche(mt, side, lat, y, rng, candle=(lat + k) % 5 == 0)
    barnacles(mt, rng, 36, y0=GROUND, y1=FR + 6)
    for (x, z) in ((2, 2), (24, 2), (2, 24), (24, 24)):
        D.candle_cluster(mt, x, GROUND, z, rng, colors=("light_blue", "white"), count=4)
    # ---- markers: the dead lie between the coffins and in the lanes
    K.auto_markers(mt, "undead", spawns=8, patrols=3, spacing=5, avoid=[(13, 13, 2)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 3. tide_gate: four tide pools of different depth, read them in order
# =====================================================================================================================
#: the pools of the Tide Gate, left to right: (colour number of the plate, depth of the pool). The Riddle Stone's order is the colours by depth, shallowest first.
TIDE_POOLS = ((1, 4), (2, 1), (3, 3), (0, 2))
POOL_BED = {0: "minecraft:red_concrete", 1: "minecraft:blue_concrete", 2: "minecraft:yellow_concrete", 3: "minecraft:lime_concrete"}
TIDE_ORDER = tuple(c for c, _d in sorted(TIDE_POOLS, key=lambda cd: cd[1]))      # (2, 0, 3, 1)


@room("puzzle", "tide_gate", faction="aquatic", no_spawn=True, kind="puzzle")
def tide_gate(name):
    """Why: the brief's 'tide gates that flood and drain (a timed puzzle that is safe)', built with what the game can really do on its own: the party cannot be made to wait for a tide
    (nothing in a template ticks), so the tide is made a thing to read. Along the north wall stand four tide pools, each three blocks square and each of a different depth (one, two, three
    and four blocks of water over a bed of red, blue, gold or green stone), and in front of each a sigil plate of the same colour; the Riddle Stone by the south wall asks for the order in
    which the plates must be pressed, and the order is the depth of the pools, the shallows first and the deep last (gold, red, green, blue). A wrong plate stings and resets the sequence
    (the Spire's rule). Behind the east partition is the Tide Gate itself, the puzzle door of the mod, with the treasure closet; in the middle of the hall a round well (four blocks of
    water, a sea lantern at the bottom) is the 'tide' that the riddle speaks of. Every plate and the stone stand on dry floor, the pools are sunk into the floor with a rim round them
    (nobody can fall in, and nobody needs to swim), and the 3-wide route from the doors runs between the pools and the well."""
    mt = tpl("puzzle", name, 3, 2)
    rng = mt.rng
    H = 12
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.14)
    top = FR + H
    mt.socket("w", 13)
    mt.socket("s", 13)
    G2.sigil_closet(mt, L.Frame(mt, 18, FR + 1, 8, "east"), rng)
    K.under_floor(mt, rng)
    # ---- the floor: plates of tile and polished stone, a runner to the gate
    for z in range(2, 16):
        for x in range(2, 19):
            if (x + z) % 2 == 0 and rng.random() < 0.4:
                mt.set(x, FR, z, L.POLISHED)
    for x in range(13, 19):
        for z in (8, 9):
            mt.set(x, FR, z, L.CHISELED if x == 18 else L.POLISHED)
    # ---- the four pools: rim round them, a coloured bed, the water to the floor's level
    for k, (colour, depth) in enumerate(TIDE_POOLS):
        x0 = 3 + 4 * k
        cells = G2.rect(x0, x0 + 2, 3, 5)
        for (x, z) in G2.rect(x0 - 1, x0 + 3, 2, 6) - cells:
            mt.set(x, FR, z, L.POLISHED)
        K.flood(mt, cells, FR - depth, FR)
        for (x, z) in cells:
            mt.set(x, FR - depth, z, POOL_BED[colour])
        mt.set(x0 + 1, FR + 1, 7, G2.PLATE, {"color": str(colour), "lit": "false"})
        K.sign(mt, x0 + 1, FR + 3, 2, "south", ["", "THE %s" % ("SHALLOWS", "LOW WATER", "RISING", "THE DEEP")[depth - 1], "", ""], block="minecraft:crimson_wall_sign")
    for x in (6, 10, 14):                                           # the dividers rise into columns
        D.column(mt, x, 4, FR + 1, top - 1)
    # ---- the tide well: a round pool four deep with a sea lantern at the bottom, a polished curb, four chains with lanterns over it
    wx, wz = 8, 12
    well = {(x, z) for z in range(8, 17) for x in range(2, 19) if D.in_disc(x, z, wx, wz, 2) and 6 <= x <= 10 and 10 <= z <= 14}
    for (x, z) in {(x, z) for z in range(8, 17) for x in range(2, 19) if D.in_disc(x, z, wx, wz, 3)} - well:
        mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.CHISELED)
    K.flood(mt, well, FR - 4, FR)
    mt.set(wx, FR - 4, wz, "minecraft:sea_lantern")
    for (dx, dz) in ((-3, 0), (3, 0), (0, -3), (0, 3)):
        K.hang(mt, wx + dx, top + 1, wz + dz, 6, L.LANTERN)
    # ---- the riddle stone on a plinth by the south wall, with a lantern over it; two signs that say what the room is
    mt.set(16, FR + 1, 15, G2.RIDDLE, None, {"id": G2.RIDDLE, "sequence": G2.nbt_list(list(TIDE_ORDER))})
    mt.set(16, FR + 2, 15, L.LANTERN, {"hanging": "false"})
    K.sign(mt, 17, FR + 3, 15, "north", ["THE TIDE GATE", "Press the seals", "as the water rises:", "shallow to deep."], block="minecraft:crimson_wall_sign")
    K.sign(mt, 2, FR + 3, 8, "east", ["The sea gave", "the gate its", "key: it is", "the depth."], block="minecraft:crimson_wall_sign")
    # ---- the walls: a water mark of polished stone, barnacles, banners
    water_mark(mt, (FR + 2, FR + 3))
    barnacles(mt, rng, 30, y0=GROUND, y1=FR + 5)
    for z in (6, 11):
        K.banner(mt, 2, FR + 6, z, "east", "light_blue")
    for x in (8, 15):
        K.banner(mt, x, FR + 6, 15, "north", "cyan")
    D.brazier(mt, 18, FR + 1, 6, cage=True)
    D.brazier(mt, 18, FR + 1, 11, cage=True)
    mt.marker(7, FR + 1, 8, "patrol")
    mt.marker(15, FR + 1, 12, "patrol")
    mt.marker(4, FR + 1, 14, "faction", faction="aquatic")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 4. drowned_font: the trial of the baptistery
# =====================================================================================================================
@room("trial", "drowned_font", faction="aquatic", no_spawn=True, kind="trial")
def drowned_font(name):
    """Why: the theme's fair fight (docs/labyrinth/e1_trial.md): walk in and the Sealed Archway shuts both doors, the glyphs spawn the wave, the barriers fall when the last one dies. The
    stage is the baptistery of the drowned church: a round font in the middle of the floor (a pool seven across and three deep, a sea lantern on its bed, a curb of polished stone with
    chiselled spokes round it), eight spawn glyphs on a ring at radius nine from which the wave climbs out (the drowned and the skeletons of the crypt), four pillars with lamps beside the ring for
    cover, glowing lancet windows in the side walls, and in the west wall an arched alcove with the party's chest between two braziers of cold fire, in the east wall the arched alcove of a
    drowned saint who watches. The font is sunk into the floor with a curb all round: nobody falls in by accident, a swimmer climbs out onto the curb, and the wave's drowned can fight
    from the water or out of it. No spawn marker: the archway's roster is the wave."""
    mt = tpl("trial", name, 3, 3)
    rng = mt.rng
    mt.room(14, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.12)
    mt.socket("n", 13)
    mt.socket("s", 13)
    K.under_floor(mt, rng)
    c = 13
    font = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 3)}
    curb = {(x, z) for z in range(2, 25) for x in range(2, 25) if D.in_disc(x, z, c, c, 4.4)} - font
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, c, c)
            if (x, z) in curb:
                mt.set(x, FR, z, L.CHISELED if int(D.angle_of(x, z, c, c) / 22.5) % 2 == 0 else L.POLISHED)
            elif 5.5 < d <= 6.5:
                mt.set(x, FR, z, L.POLISHED)
            elif abs(x - c) <= 1:
                mt.set(x, FR, z, L.POLISHED)
            elif rng.random() < 0.14:
                mt.set(x, FR, z, L.CRACKED_TILE)
    K.flood(mt, font, FR - 3, FR)
    mt.set(c, FR - 3, c, "minecraft:sea_lantern")
    glyphs = [(x, z) for (x, z) in D.around(c, c, 9.0, 8, 22.5)]
    G2.trial_core(mt, [("minecraft:drowned", 4), ("minecraft:skeleton", 2)], rng, [("n", 13), ("s", 13)], FR + 12, glyphs)
    for (px, pz) in ((8, 8), (18, 8), (8, 18), (18, 18)):
        D.column(mt, px, pz, FR + 1, FR + 13)
        D.pillar_lantern(mt, px, pz, D.stair_facing(c - px, c - pz), FR + 3, lantern=L.LANTERN)
    f = D.inner_frame(mt, "w", 13)
    D.arched_alcove(mt, f, 5, 5)
    mt.t.chest(*f.at(0, 0, -1), K.LOOT, "east")
    for r in (-2, 2):
        D.candle_cluster(mt, *f.at(r, 0, -1), rng, colors=("light_blue", "white"), count=4)
    D.brazier(mt, *f.at(-4, 0, 0), cage=True)
    D.brazier(mt, *f.at(4, 0, 0), cage=True)
    f = D.inner_frame(mt, "e", 13)
    D.arched_alcove(mt, f, 5, 5)
    D.alcove_statue(mt, f, ("diamond", "trident"))
    for side in ("n", "s"):
        for lat in (6, 20):
            D.sconce(mt, D.inner_frame(mt, side, lat), 0, 5, skull="skeleton")
    for lat in (6, 20):
        for side in ("w", "e"):
            K.lancet(mt, side, lat, FR + 4, width=3, height=8, glass=GLASS)
    barnacles(mt, rng, 40, y0=GROUND, y1=FR + 8)
    for (x, z) in ((13, 6), (13, 20), (6, 13)):
        mt.marker(x, FR + 1, z, "patrol")
    mt.marker(4, FR + 1, 4, "faction", faction="aquatic")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 5. drowned_chapel: the rotunda of the drowned church, the set piece
# =====================================================================================================================
def round_window(mt, cx, cz, R, ang, y0, height, room_fn, glass, step_deg=3.0):
    """A lancet window in the curved drum wall of a round room, three columns wide (the narrow top two rows one column): the glass goes into the surface cell of the wall (the cell
    behind the room's outermost interior cell on the ray at `ang`), a sea lantern into the cell behind that (so the window glows and the shell stays closed). The three columns are the rays
    at ang - step_deg, ang, ang + step_deg, which is about one cell each on a drum of radius twenty. Returns the number of glass cells written."""
    cols = []
    for k in (-1, 0, 1):
        x, z, face = G2.wall_cell(cx, cz, R, ang + k * step_deg, room_fn)
        dx, dz = L.STEP[face]
        cols.append((k, (x - dx, z - dz), (x - 2 * dx, z - 2 * dz)))
    n = 0
    for u in range(height):
        reach = 1 if u < height - 2 else 0
        for (k, (wx, wz), (bx, bz)) in cols:
            if abs(k) > reach or room_fn(wx, wz):
                continue
            c = mt.get(wx, y0 + u, wz)
            b = mt.get(bx, y0 + u, bz)
            if c is None or b is None or (wx, y0 + u, wz) in mt.reserved or (bx, y0 + u, bz) in mt.reserved:
                continue
            mt.set(wx, y0 + u, wz, L.POLISHED if (k == 0 and u < height - 3) else glass[(u // 2 + abs(k)) % len(glass)])
            mt.set(bx, y0 + u, bz, "minecraft:sea_lantern")
            n += 1
    return n


@room("set", "drowned_chapel", faction="aquatic", strict_light=True, route_width="wide")
def drowned_chapel(name):
    """Why: every sector has one room the party remembers, and the Drowned Crypt's is the chapel the crypt was built under: a rotunda forty-one across (radius twenty) under a ribbed dome,
    flooded to the level of its floor. What the party walks on is a cross of causeways five wide that joins the three doors (south, west and east) at a round dais in the middle, where a font (a pool three
    blocks deep, a sea lantern on its bed) lies under the church's bell, hung on a long chain from the apex of the dome; the north arm climbs three steps to the altar between two drowned
    saints, the chest behind it, a rose window of stained glass over it. Round the flood stand twelve pillars carrying a ring beam with cold lanterns on chains, in the drum between them
    twelve lancet windows glow by themselves, and on the bed under the water lie the benches of the congregation, moss, dead coral, the bones of those who were at prayer and lamps.
    The aquatic faction lives in the flood and comes out onto the arms; the dais is where the party can hold."""
    mt = tpl("set", name, 5, 5)
    rng = mt.rng
    cx = cz = 22
    R = 20
    drum = FR + 18
    base = drum + 1
    dome_h = 10
    room_fn = lambda x, z: D.in_disc(x, z, cx, cz, R)  # noqa: E731
    for side in ("s", "w", "e"):                                # the north end is the altar: no door behind it
        mt.socket(side, 22)
    apex = G2.dome_row(0, R + 0.5, base, dome_h)
    G2.solid_mass(mt, room_fn, FR - 1, apex + 1, rng=rng)
    K.under_floor(mt, rng)
    # ---- the floor plan: the cross of causeways, the round dais, the altar platform; the rest of the circle is the flood
    room = {(x, z) for z in range(mt.D) for x in range(mt.W) if room_fn(x, z)}
    arms = {(x, z) for (x, z) in room if abs(x - cx) <= 2 or abs(z - cz) <= 2}
    dais = {(x, z) for (x, z) in room if D.in_disc(x, z, cx, cz, 6.5)}
    platform = {(x, z) for (x, z) in room if 18 <= x <= 26 and z <= 11}
    dry = arms | dais | platform
    wet = room - dry
    for (x, z) in sorted(room):
        mt.set(x, FR, z, L.TILE if (x + z) % 5 else L.POLISHED)
    K.flood(mt, wet, BED, FR)
    # ---- the dais: rings of stone, the font in the middle, braziers
    font = {(x, z) for (x, z) in dais if D.in_disc(x, z, cx, cz, 1.5)}
    for (x, z) in sorted(dais - font):
        d = dist(x, z, cx, cz)
        if d <= 4.6:
            mt.set(x, FR, z, L.CHISELED if int(D.angle_of(x, z, cx, cz) / 22.5) % 2 == 0 else L.POLISHED)
        elif d <= 6.6 and (abs(x - cx) <= 0 or abs(z - cz) <= 0):
            mt.set(x, FR, z, L.CHISELED)
    K.flood(mt, font, FR - 3, FR)
    mt.set(cx, FR - 3, cz, "minecraft:sea_lantern")
    # ---- the arms: a runner down the middle, lamp posts at their edges (a post on the edge keeps the three middle columns free)
    for (x, z) in sorted(arms - dais):
        if (abs(x - cx) == 0 and z % 3 == 1) or (abs(z - cz) == 0 and x % 3 == 1):
            mt.set(x, FR, z, L.CHISELED)
    for dist_from_centre in (9, 15):
        for (ex, ez) in ((cx - 2, cz + dist_from_centre), (cx + 2, cz + dist_from_centre), (cx - dist_from_centre, cz - 2), (cx - dist_from_centre, cz + 2),
                         (cx + dist_from_centre, cz - 2), (cx + dist_from_centre, cz + 2), (cx - 2, cz - dist_from_centre), (cx + 2, cz - dist_from_centre)):
            if (ex, ez) in arms and (ex, ez) not in platform and mt.get(ex, GROUND, ez) is None:
                D.lamp_post(mt, ex, GROUND, ez, height=2, lantern=L.LANTERN)
    # ---- the altar platform at the end of the north arm: three steps, the platform, the altar, the chest, two saints
    for x in range(20, 25):
        put(mt, x, FR + 1, 11, L.TILE_STAIRS, st("north"))
        put(mt, x, FR + 1, 10, L.BRICK)
        put(mt, x, FR + 2, 10, L.TILE_STAIRS, st("north"))
        for y in (FR + 1, FR + 2):
            put(mt, x, y, 9, L.BRICK)
        put(mt, x, FR + 3, 9, L.TILE_STAIRS, st("north"))
    for z in range(3, 9):
        for x in range(19, 26):
            box(mt, x, x, FR + 1, FR + 3, z, z, L.BRICK)
    for z in (9, 10, 11):
        for x in (18, 19, 25, 26):
            if (x, z) in platform:
                box(mt, x, x, FR + 1, FR + (3 if z == 9 else (2 if z == 10 else 1)), z, z, L.BRICK)
    for x in range(19, 26):
        for z in range(3, 9):
            mt.set(x, FR + 3, z, L.TILE if (x + z) % 2 else L.POLISHED)
    for x in range(20, 25):
        put(mt, x, FR + 4, 6, L.POLISHED)
        put(mt, x, FR + 4, 5, L.CHISELED)
    candles(mt, 19, FR + 4, 6, 3, "light_blue")
    candles(mt, 25, FR + 4, 6, 3, "light_blue")
    mt.t.chest(22, FR + 4, 3, K.LOOT, "south")
    for x in (20, 24):
        L.statue(mt, x, FR + 4, 8, "south", head="minecraft:diamond_helmet", chest="minecraft:diamond_chestplate", legs="minecraft:diamond_leggings",
                 feet="minecraft:diamond_boots", weapon="minecraft:trident")
    # rose window over the altar: glass in the surface layer, sea lanterns behind (the flat part of the north wall is x = 18 .. 26)
    for x in range(18, 27):
        for y in range(FR + 9, FR + 19):
            d = math.hypot(x - cx, y - (FR + 13))
            if d > 4.6:
                continue
            ang = (math.degrees(math.atan2(y - (FR + 13), x - cx)) + 360.0) % 360.0
            if d <= 1.2:
                mt.set(x, y, 1, L.CHISELED)
            elif d >= 3.9 or min(abs(((ang - 45.0 * k + 180.0) % 360.0) - 180.0) for k in range(8)) * math.radians(1.0) * d < 0.6:
                mt.set(x, y, 1, L.POLISHED)
            else:
                mt.set(x, y, 1, GLASS[(int(d) + int(ang / 45.0)) % 3])
                mt.set(x, y, 0, "minecraft:sea_lantern")
    # ---- the dome (a ribbed shell two thick), the drum's windows between the ribs
    for (x, z) in sorted(room):
        r = dist(x, z, cx, cz)
        ys = G2.dome_row(r, R + 0.5, base, dome_h)
        rib = min(abs(((D.angle_of(x, z, cx, cz) - 15.0 - 30.0 * k + 180.0) % 360.0) - 180.0) for k in range(12)) * math.radians(1.0) * r < 0.7
        for y in (ys, ys + 1):
            mt.set(x, y, z, L.POLISHED if (rib and y == ys) else (L.CRACKED_BRICK if rng.random() < 0.08 else L.BRICK))
    for k in range(12):
        ang = 30.0 * k + 15.0
        if 255.0 <= ang <= 285.0:
            continue
        round_window(mt, cx, cz, R, ang, FR + 6, 12, room_fn, GLASS)
    # ---- the pillars standing in the flood and the ring beam on them
    ring_pillars = [(x, z) for (x, z) in D.around(cx, cz, 14.5, 12, 15.0)]
    for (px, pz) in ring_pillars:
        if (px, pz) in dry or mt.get(px, FR, pz) is None or mt.get(px, FR, pz).block != K.WATER:
            continue
        pillar_in_flood(mt, px, pz, drum - 2)
    for (x, z) in sorted(room):
        if 14.0 <= dist(x, z, cx, cz) <= 15.0 and mt.get(x, drum - 1, z) is None:
            mt.set(x, drum - 1, z, L.POLISHED)
    for (px, pz) in ring_pillars:
        c0 = mt.get(px, drum - 2, pz)
        if c0 is not None and c0.block != K.WATER:
            K.hang(mt, px, drum - 1, pz, 3, L.LANTERN)
    # ---- the bell: a chain from the apex of the dome to a block, the bell under it over the font
    for y in range(FR + 17, apex):
        put(mt, cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    put(mt, cx, FR + 16, cz, L.POLISHED)
    put(mt, cx, FR + 15, cz, "minecraft:bell", {"attachment": "ceiling", "facing": "north", "powered": "false"})
    # ---- the flood: benches on the bed, moss, coral crust, bones, lamps
    for (x0, x1) in ((6, 16), (28, 38)):
        benches(mt, x0, x1, (8, 11, 14, 30, 33, 36), rng=rng)
    water_cols = {(x, z) for (x, z) in wet if all((mt.get(x, y, z) is not None and mt.get(x, y, z).block == K.WATER) for y in range(BED + 1, FR + 1))}
    bed_growth(mt, water_cols, rng)
    bed_lamps(mt, water_cols, rng, 22, min_gap=4.5)
    barnacles(mt, rng, 40, y0=GROUND, y1=FR + 6)
    # ---- markers: spawns on the arms, patrols on the dais
    K.auto_markers(mt, "aquatic", spawns=8, patrols=3, spacing=7, door_gap=8, avoid=[(cx, cz, 3.0)])
    mt.marker(cx, GROUND, cz + 5, "room_center")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 6. air_bell: the air pocket, the calm room, with the sea on the other side of a glass wall
# =====================================================================================================================
@room("safe", "air_bell", safe=True, seats=4, hearths=1, beds=3, min_light=6, route_width="wide")
def air_bell(name):
    """Why: a sector needs places to rest (the rest system reads seats, hearths and beds in a room of the `safe` folder), and in a drowned crypt the place to rest is the air pocket: 18 x 18,
    dry, with a glass wall along its whole north side and behind the glass the flood: a tank fourteen across, four deep and floor to roof high, lit by sea lanterns in its back wall,
    with a reef of living coral on its bed (the one living thing of the theme: colour in a grey sea), three sunken pillars and an arch of the old crypt. In the dry half: a campfire in a
    ring of low plates with four benches round it, three bunks with blue blankets against the west wall between barrels with lanterns on them, a chart table, stores and a diver's suit
    on a stand. It is lit and quiet; the sea is on the other side of the glass. No spawn, no faction, no chest; the rest anchors (four seats, one hearth, three beds) are what the rest system
    reads. The tank is closed on every side by full blocks (the glass is whole blocks, so it holds the water), nothing in it is a plant, so the lead's fluid checks pass."""
    mt = tpl("safe", name, 2, 2)
    rng = mt.rng
    H = 11
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.06)
    top = FR + H
    for (side, off) in (("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    K.under_floor(mt, rng)
    # ---- the tank: water from the floor row to the roof over x = 2 .. 15, z = 2 .. 5, the glass wall at z = 6 with polished mullions and a lintel
    tank = G2.rect(2, 15, 2, 5)
    K.flood(mt, tank, FR - 1, top)
    for x in range(2, 16):
        for y in range(GROUND, top):
            mt.set(x, y, 6, L.POLISHED if x in (5, 9, 13) else "minecraft:glass")
        mt.set(x, top, 6, L.POLISHED)
    for (x, y) in ((4, FR + 3), (8, FR + 3), (12, FR + 3), (4, FR + 7), (8, FR + 7), (12, FR + 7), (6, FR + 5), (10, FR + 5)):
        mt.set(x, y, 1, "minecraft:sea_lantern")                              # recessed into the inner layer of the back wall
    # the reef on the bed, the sunken pillars and the arch (full blocks only: nothing that is not a cube may touch the water)
    corals = ("minecraft:tube_coral_block", "minecraft:brain_coral_block", "minecraft:bubble_coral_block", "minecraft:fire_coral_block", "minecraft:horn_coral_block")
    for (x, z) in sorted(tank):
        if rng.random() < 0.32:
            for y in range(FR, FR + rng.choice((1, 1, 2, 3))):
                mt.set(x, y, z, rng.choice(corals))
    for (x, z, h) in ((3, 3, 8), (14, 4, 6), (9, 5, 4)):
        for y in range(FR, FR + h):
            mt.set(x, y, z, L.BRICK)
    for y in range(FR, FR + 6):
        for x in (6, 11):
            mt.set(x, y, 3, L.POLISHED)
    for x in range(6, 12):
        mt.set(x, FR + 6, 3, L.POLISHED)
    for (x, z) in ((5, 4), (12, 3), (8, 2)):
        mt.set(x, FR, z, "minecraft:sea_lantern")
    # ---- the dry half: the floor, a rug, the hearth in a ring of low plates, four benches
    for z in range(7, 16):
        for x in range(2, 16):
            d = dist(x, z, 8.5, 9.5)
            if 2.4 <= d <= 4.0:
                mt.set(x, FR, z, L.POLISHED)
    for (x, z) in ((7, 8), (8, 8), (9, 8), (7, 9), (9, 9), (7, 10), (8, 10), (9, 10)):
        mt.set(x, GROUND, z, L.POLISHED_SLAB, {"type": "bottom"})
    K.hearth(mt, 8, GROUND, 9, block="minecraft:campfire")
    K.seat(mt, 8, GROUND, 7, "south")
    K.seat(mt, 8, GROUND, 11, "north")
    K.seat(mt, 5, GROUND, 9, "east")
    K.seat(mt, 11, GROUND, 9, "west")
    # ---- three bunks against the west wall with blue blankets, barrels with lanterns between
    for z, colour in ((7, "cyan"), (9, "light_blue"), (11, "cyan")):
        K.bed(mt, 3, GROUND, z, "west", colour)
    for z in (8, 10):
        put(mt, 2, GROUND, z, "minecraft:barrel", {"facing": "up", "open": "false"})
        put(mt, 2, GROUND + 1, z, L.LANTERN, {"hanging": "false"})
    # ---- the chart table, stores, the diver
    for x in (13, 14, 15):
        put(mt, x, GROUND, 8, L.POLISHED_SLAB, {"type": "top"})
    K.lectern(mt, 14, GROUND + 1, 8, "south")
    put(mt, 15, GROUND, 7, "minecraft:cartography_table")
    for x in range(3, 9):
        put(mt, x, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    put(mt, 9, GROUND, 15, "minecraft:water_cauldron", {"level": "3"})
    L.statue(mt, 15, GROUND, 11, "west", head="minecraft:turtle_helmet", chest="minecraft:iron_chestplate", legs="minecraft:iron_leggings",
             feet="minecraft:iron_boots", weapon="minecraft:trident")
    K.sign(mt, 6, FR + 3, 15, "north", ["AIR POCKET.", "Rest here.", "The sea is on", "the other side."], block="minecraft:crimson_wall_sign")
    # ---- lamps, banners
    for (x, z) in ((5, 8), (11, 8), (5, 12), (11, 13)):
        K.hang(mt, x, top + 1, z, 5, L.LANTERN)
    for (x, z) in ((3, 13), (3, 15), (15, 14)):
        if mt.get(x, GROUND, z) is None:
            D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.LANTERN)
    for x in (4, 12):
        K.banner(mt, x, GROUND + 5, 15, "north", "cyan")
    mt.marker(12, GROUND, 11, "patrol")
    mt.marker(5, GROUND, 13, "patrol")
    K.finish(mt, ID, minimum=6)
    return mt


# =====================================================================================================================
# 7. pearl_reliquary: the treasure room, a giant clam on an island in a flooded vault
# =====================================================================================================================
@room("treasure", "pearl_reliquary", faction="aquatic", strict_light=True, route_width="wide", few_spawns=True)
def pearl_reliquary(name):
    """Why: the treasure room of the theme: the thing the sea keeps best is a pearl. An 18 x 18 dead end (one doorway in the south) whose floor is a pool four blocks deep; a causeway
    three wide leads from the door to a round island at the far end, and on the island lies a giant clam of white calcite, its lower shell a bowl, its upper shell a half dome propped open
    over it, in it two chests either side of a pearl (a sea lantern on a plinth of bone) and the island's own light; four pillars stand in the pool at the corners with lamps on them, the
    bed is strewn with moss, coral crust, bone and lamps, burial niches with skulls and candles line the walls. The aquatic faction guards it (the heavy spawns on the island), and a swimmer
    can go round the island to the chests from the water as well. Closed on every side by full blocks, no plant in the water, so the lead's fluid checks pass."""
    mt = tpl("treasure", name, 2, 2)
    rng = mt.rng
    H = 11
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    mt.socket("s", 13)
    K.under_floor(mt, rng)
    ix, iz, ir = 8, 7, 3.5
    island = {(x, z) for z in range(2, 16) for x in range(2, 16) if D.in_disc(x, z, ix, iz, ir)}
    causeway = G2.rect(12, 14, 8, 15) | G2.rect(7, 14, 12, 15)
    dry = island | causeway
    wet = G2.rect(2, 15, 2, 15) - dry
    K.flood(mt, wet, FR - 4, FR)
    for (x, z) in sorted(causeway | island):
        mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
    # the island's bank in chiselled stone, so the clam stands on a visible plinth
    for (x, z) in sorted(island):
        if dist(x, z, ix, iz) > 2.6:
            mt.set(x, FR, z, L.CHISELED)
    # ---- the clam: a calcite bowl, the open upper shell on its far (north) side, the chests and the pearl
    for (x, z) in sorted(island):
        d = dist(x, z, ix, iz)
        if d <= 2.5:
            put(mt, x, GROUND, z, "minecraft:calcite")
        if 2.5 < d <= 3.6 and z <= iz:
            for y in range(GROUND + 1, GROUND + 5):
                dy = y - (GROUND + 0.5)
                if 2.4 <= math.sqrt(d * d + dy * dy) <= 4.3:
                    put(mt, x, y, z, "minecraft:calcite" if (x + y) % 3 else "minecraft:bone_block")
    for x in (7, 9):
        mt.t.chest(x, GROUND + 1, 7, K.LOOT, "south")
    put(mt, 8, GROUND + 1, 7, "minecraft:bone_block")
    put(mt, 8, GROUND + 2, 7, "minecraft:sea_lantern")
    # ---- pillars in the pool at the corners with lamps, bed growth, lamps in the bed
    for (px, pz) in ((3, 3), (14, 3), (3, 14)):
        for y in range(FR - 3, FR + 1):
            mt.set(px, y, pz, L.BRICK)
        D.lamp_post(mt, px, GROUND, pz, height=3, lantern=L.LANTERN)
    water_cols = {(x, z) for (x, z) in wet if all((mt.get(x, y, z) is not None and mt.get(x, y, z).block == K.WATER) for y in range(FR - 3, FR + 1))}
    bed_growth(mt, water_cols, rng, moss=0.12, bones=0.04, coral=0.08, bed=FR - 4)
    bed_lamps(mt, water_cols, rng, 7, bed=FR - 4, min_gap=3.5)
    # ---- the walls: burial niches, barnacles, banners, a chandelier over the island
    for side in ("n", "w", "e"):
        for lat in (4, 7, 10, 13):
            for k, y in enumerate((GROUND, GROUND + 3)):
                niche(mt, side, lat, y, rng, candle=(lat + k) % 3 == 0)
    barnacles(mt, rng, 18, y0=GROUND, y1=FR + 6)
    L.chandelier(mt, ix, top + 1, iz, drop=4, radius=1, lantern=L.LANTERN)
    K.banner(mt, 15, GROUND + 5, 10, "west", "cyan")
    K.banner(mt, 2, GROUND + 5, 10, "east", "cyan")
    K.auto_markers(mt, "aquatic", spawns=3, patrols=1, spacing=3, door_gap=4, edge=2)
    K.finish(mt, ID)
    return mt


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, doc=__doc__, theme=ID))

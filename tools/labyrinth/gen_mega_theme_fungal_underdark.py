#!/usr/bin/env python3
"""
Why: the FUNGAL_UNDERDARK theme (1.59.0, THEME-D, docs/labyrinth/mega_v2_thd.md): the evil road's Underdark. A themed sector must read as a NEW place, so besides the
palette swap of `data/nusmp/mega/themes/fungal_underdark.json` (packed mud walls with mud brick trim, polished blackstone floors, obsidian and crying obsidian trim, warped wood) it owns its signature
rooms, which the assembler may draw only in sectors of this theme (`data/nusmp/mega/template_themes/fungal_underdark.json`, prefix `nusmp:mega/<folder>/fungal_underdark_`).

    python3 tools/labyrinth/gen_mega_theme_fungal_underdark.py                 # build, check and write every room
    python3 tools/labyrinth/gen_mega_theme_fungal_underdark.py drow_enclave    # only this one
    python3 tools/labyrinth/gen_mega_theme_fungal_underdark.py --check         # build and check in memory, compare with the files on disk (CI style)

Rooms (all in the palette of `mega_lib`, so the theme restyles them; mushrooms, obsidian-only props, glass and lights are neutral accents the swap leaves alone):
spore_forest (cavern), drow_enclave (hall, two storeys), fungus_farm (hall), myconid_circle (set piece), chasm_ledges (trap), darklake (cavern), myconid_garden (safe).
Each builder says WHY in its own docstring. Deterministic: every random choice comes from the template's own name (`MT.rng`).
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
from thd_kit import candles  # noqa: E402
from thd_kit import FR, GROUND, put, st, dist  # noqa: E402

ID = "fungal_underdark"
ROOMS = []   # (folder, name, builder, spec)


def room(folder, name, **spec):
    """Registers a builder `fn(full_name) -> MT`; `spec` is what the checks expect of the room (see gen_mega_rooms2.check_room)."""
    def deco(fn):
        ROOMS.append((folder, ID + "_" + name, fn, spec))
        return fn
    return deco


def tpl(folder, name, mx, mz):
    return K.new(folder, name, mx, mz)


# =====================================================================================================================
# 1. drow_enclave: a drow house's gate hall, two storeys
# =====================================================================================================================
def web_medallion(mt, cx, cz, rmax, rings=(3.4, 6.4, 9.4), spokes=8):
    """A spider-web inlay in the floor: `spokes` radial lines of chiselled blocks and octagonal rings of them (the theme turns chiselled blocks into crying obsidian, so the web
    glows faintly violet on a black floor), the cells between kept as floor."""
    for z in range(cz - rmax - 1, cz + rmax + 2):
        for x in range(cx - rmax - 1, cx + rmax + 2):
            d = dist(x, z, cx, cz)
            if d > rmax or d < 0.8 or mt.get(x, FR, z) is None:
                continue
            ang = math.degrees(math.atan2(z - cz, x - cx)) % 360.0
            on_spoke = any(min(abs(ang - 360.0 / spokes * k), 360.0 - abs(ang - 360.0 / spokes * k)) * math.pi / 180.0 * d < 0.62 for k in range(spokes))
            on_ring = any(abs(d - r) < 0.55 for r in rings)
            if on_spoke or on_ring:
                mt.set(x, FR, z, L.CHISELED)


def spider_statue(mt, cx, y, cz, facing="south"):
    """The spider-goddess on her plinth: a polished body (abdomen behind, thorax and head in front, glowing end-rod eyes), eight legs of polished wall posts that arch up and out
    and down to the dais. (cx, y, cz) is the first row of the body's middle; `facing` is the head's direction. Blocks only: the legs are wall posts so the silhouette is thin, and every
    leg is one face-connected run so nothing floats."""
    f = L.Frame(mt, cx, y, cz, facing)
    for r in (-1, 0, 1):
        for fo in range(-2, 3):
            f.put(r, 0, fo, L.POLISHED)
    for r in (-1, 0, 1):
        for fo in (-2, -1, 0):
            f.put(r, 1, fo, L.POLISHED if r == 0 or fo == -1 else L.POLISHED_SLAB, None if (r == 0 or fo == -1) else {"type": "bottom"})
    for fo in (-2, -1):
        f.put(0, 2, fo, L.POLISHED_SLAB, {"type": "bottom"})
    for r in (-1, 0, 1):
        for fo in (1, 2):
            f.put(r, 1, fo, L.POLISHED_SLAB, {"type": "bottom"})
    for r in (-1, 1):
        f.put(r, 1, 3, "minecraft:end_rod", {"facing": f.dir("forward")})
    for r in (-1, 0, 1):
        f.put(r, 2, 2, "minecraft:end_rod", {"facing": "up"})
    for fo in (-2, -1, 1, 2):
        for sign in (-1, 1):
            for (rr, uu) in ((2, 0), (2, 1), (3, 1), (4, 1), (4, 0), (4, -1)):
                f.put(sign * rr, uu, fo, L.POLISHED_WALL, {"up": "true"})


@room("hall", "drow_enclave", faction="cultist", strict_light=True)
def drow_enclave(name):
    """Why: the owner's brief for the Underdark asks for 'drow-like black-and-purple architecture' and a place the party remembers. This is the gate hall of a drow house:
    a 27 x 36 hall two storeys high, entered from the south through a portico, a long nave of black stone with a spider web inlaid in the floor in chiselled blocks (crying
    obsidian once the theme has swapped them: the web glows violet), four stalagmite spires with glowing tips standing in the nave, a U-shaped gallery at floor +9 on pillars and
    arches (reached by two ramps at the entrance end), globes of violet glass hung from the roof (faerie fire without a vanilla particle: stained glass round an invisible light block),
    tall windows of violet glass lit from behind, and at the far end, on a stepped dais under the gallery's north arm, the spider-goddess: a statue of polished stone with glowing
    eyes, flanked by two soul braziers. It is the big multi-storey hall of the theme; the party fights on the nave under the gallery's archers (spawn markers on the nave, the
    aisles and the gallery) and the dais at the north end is the high ground."""
    mt = tpl("hall", name, 3, 4)
    rng = mt.rng
    H = 20
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    for (side, off) in (("s", 13), ("n", 4), ("n", 22), ("w", 13), ("e", 22)):
        mt.socket(side, off)
    Z0, Z1 = 2, 33
    GAL = FR + 9                       # gallery deck row (players stand at GAL + 1)
    COLS = (4, 9, 14, 19, 24)
    # ---- the floor: a polished chequer in the aisles, the web in the nave
    for z in range(Z0, Z1 + 1):
        for x in list(range(2, 7)) + list(range(20, 25)):
            if (x + z) % 2 == 0 and rng.random() < 0.55:
                mt.set(x, FR, z, L.POLISHED)
    web_medallion(mt, 13, 20, 10)
    # ---- the gallery arms: a deck of 6 cells (the edge cell sits on the colonnade) from the north wall to z = 24
    for (xa, xb, edge) in ((2, 7, 7), (19, 24, 19)):
        for z in range(Z0, 25):
            for x in range(xa, xb + 1):
                mt.set(x, GAL, z, L.POLISHED if (x == edge) else (L.TILE if (x + z) % 4 else L.POLISHED))
        for z in range(Z0, 25):
            if z in COLS:
                for y in range(FR + 1, GAL):
                    mt.set(edge, y, z, L.POLISHED)
                mt.set(edge, FR + 1, z, L.CHISELED)
            else:
                mt.set(edge, GAL - 1, z, L.POLISHED_SLAB, {"type": "top"})
            mt.set(edge, GAL + 1, z, L.POLISHED_WALL, {"up": "true"})
    # the north arm joins them over the dais (deck z 2..6 between the arms), held by two columns and a beam
    for z in range(Z0, 7):
        for x in range(8, 19):
            mt.set(x, GAL, z, L.TILE if (x + z) % 4 else L.POLISHED)
    for x in range(8, 19):
        mt.set(x, GAL - 1, 6, L.POLISHED)
        mt.set(x, GAL + 1, 6, L.POLISHED_WALL, {"up": "true"})
    for x in (9, 17):
        for y in range(FR + 1, GAL - 1):
            mt.set(x, y, 6, L.POLISHED)
    # ---- the ramps up from the entrance end: nine steps along z from 33 to 25, a stepped mass of stone under them, entered from the nave over the first two steps
    for (xa, xb) in ((2, 6), (20, 24)):
        west = xa == 2
        for k in range(9):
            z = Z1 - k
            for x in range(xa, xb + 1):
                for y in range(FR + 1, FR + 1 + k):
                    mt.set(x, y, z, L.BRICK)
                nave_edge = (x == xb) if west else (x == xa)
                wall_edge = (x == xa) if west else (x == xb)
                if wall_edge or (nave_edge and k >= 2):
                    mt.set(x, FR + 1 + k, z, L.POLISHED)
                    if nave_edge:
                        mt.set(x, FR + 2 + k, z, L.POLISHED_WALL, {"up": "true"})
                else:
                    mt.set(x, FR + 1 + k, z, L.TILE_STAIRS, st("north"))
    # ---- the dais: two tiers with stairs, the statue, two caged soul braziers
    for z in range(2, 9):
        for x in range(9, 18):
            mt.set(x, FR + 1, z, L.TILE)
    for x in range(9, 18):
        mt.set(x, FR + 1, 9, L.TILE_STAIRS, st("north"))
    for z in range(2, 7):
        for x in range(11, 16):
            mt.set(x, FR + 2, z, L.POLISHED)
    for x in range(11, 16):
        mt.set(x, FR + 2, 7, L.POLISHED_STAIRS, st("north"))
    spider_statue(mt, 13, FR + 3, 4, "south")
    D.brazier(mt, 10, FR + 2, 8, fire="soul_campfire", cage=True)
    D.brazier(mt, 16, FR + 2, 8, fire="soul_campfire", cage=True)
    # ---- four stalagmite spires with a glowing tip, flanking the nave
    for (sx, sz) in ((9, 13), (17, 13), (9, 28), (17, 28)):
        K.spire(mt, sx, sz, FR + 1, 17, 1.6, L.POLISHED, tip=L.CHISELED)
        for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
            put(mt, sx + dx, FR + 1, sz + dz, L.POLISHED_STAIRS, st({(2, 0): "west", (-2, 0): "east", (0, 2): "north", (0, -2): "south"}[(dx, dz)]))
    # ---- faerie fire: violet globes on chains from the roof, lamps on the gallery rail, soul lanterns on the colonnade
    for (gx, gz, gy) in ((13, 11, 17), (13, 20, 18), (13, 30, 17), (13, 15, 14), (13, 26, 14)):
        K.hang_sphere(mt, gx, gy, gz, top + 1, r=1, glass="minecraft:purple_stained_glass", level=12)
    for z in (4, 14, 24):
        for edge in (7, 19):
            put(mt, edge, GAL + 2, z, L.LANTERN, {"hanging": "false"})
    for z in COLS:
        for (edge, toward) in ((7, "west"), (19, "east")):
            D.pillar_lantern(mt, edge, z, toward, FR + 4, lantern=L.SOUL_LANTERN)
    # ---- the windows: tall violet slits in the upper wall of both long sides and a great one behind the goddess, lit from the outer layer
    for z in (8, 14, 20):
        for y in range(GAL + 3, GAL + 9):
            for dz in (0, 1, 2):
                # glass in the inner wall layer, a sea lantern behind it in the outer layer; a cell the template does not have (the wall is only two thick) is skipped
                for (glass_x, lamp_x) in ((1, 0), (25, 26)):
                    if (glass_x, y, z + dz) in mt.t.cells:
                        mt.set(glass_x, y, z + dz, "minecraft:purple_stained_glass")
                    if (lamp_x, y, z + dz) in mt.t.cells:
                        mt.set(lamp_x, y, z + dz, "minecraft:sea_lantern")
    for x in range(11, 16):
        for y in range(GAL + 2, GAL + 9):
            if (x, y, 1) in mt.t.cells:
                mt.set(x, y, 1, "minecraft:purple_stained_glass")
                mt.set(x, y, 0, "minecraft:sea_lantern")
    # ---- pilasters on the long walls (the rhythm of a noble house), under and over the gallery
    for z in COLS:
        D.pilaster(mt, 2, z, FR + 1, GAL - 1, "east")
        D.pilaster(mt, 24, z, FR + 1, GAL - 1, "west")
        D.pilaster(mt, 2, z, GAL + 1, top - 1, "east")
        D.pilaster(mt, 24, z, GAL + 1, top - 1, "west")
    # ---- banners, webs, guards
    for z in (7, 12, 17, 22):
        K.banner(mt, 2, GAL + 6, z, "east", "purple")
        K.banner(mt, 24, GAL + 6, z, "west", "black")
    for (x, z) in ((3, 2), (23, 2), (3, 33), (23, 33), (8, 2), (18, 2)):
        for dy in (0, 1):
            K.cobweb(mt, x, top - dy, z)
    for (gx, gz, face) in ((9, 32, "north"), (17, 32, "north")):
        L.statue(mt, gx, FR + 1, gz, face, head="minecraft:netherite_helmet", chest="minecraft:netherite_chestplate", legs="minecraft:netherite_leggings",
                 feet="minecraft:netherite_boots", weapon="minecraft:iron_sword")
    K.auto_markers(mt, "cultist", spawns=7, patrols=2, avoid=[(13, 4, 5)])
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 2. spore_forest: a cavern of colossal mushrooms
# =====================================================================================================================
#: (stem height, stem radius, cap radius, cap rise, flat umbrella) from the largest a site is offered down to the smallest that is still a giant mushroom
FOREST_SIZES = [(15, 1, 8, 4, False), (13, 1, 7, 4, False), (11, 1, 6, 3, False), (10, 1, 7, 2, True), (9, 1, 5, 3, False), (8, 1, 5, 2, True), (7, 0.5, 4, 2, False), (6, 0.5, 3, 2, False)]


def sphere_fits(mt, cav, cx, cy, cz, r):
    """True when every cell of a glow sphere of radius r lies in the cave's air and is still unset (a spore cloud never grows out of the rock)."""
    for dy in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                if dx * dx + dy * dy + dz * dz <= (r + 0.4) ** 2 and (not cav.free(cx + dx, cy + dy, cz + dz) or mt.get(cx + dx, cy + dy, cz + dz) is not None):
                    return False
    return True


def spore_clouds(mt, cav, rng, count, colours, r=2, min_d2=4.0, lo=7, hi=11):
    """`count` glass spore clouds floating in the open air of the cave (each is a glow sphere: stained glass round an invisible light), at least five cells apart."""
    cols = [c for c in cav.columns() if cav.d2[c[0]][c[1]] >= min_d2 and len(cav.air[c]) > hi + 6]
    rng.shuffle(cols)
    placed = []
    for (x, z) in cols:
        if len(placed) >= count:
            break
        if any(dist(x, z, px, pz) < 7 for (px, pz) in placed):
            continue
        y = cav.air[(x, z)][0] + rng.randint(lo, hi)
        if sphere_fits(mt, cav, x, y, z, r):
            K.glow_sphere(mt, x, y, z, r, colours[len(placed) % len(colours)], level=11)
            placed.append((x, z))
    return len(placed)


def ceiling_growth(mt, cav, rng, blossom, roots):
    """Spore blossoms (they shed their own spores, a vanilla block's behaviour, not a particle call) and hanging roots under the cave ceiling."""
    n = 0
    for (x, z) in cav.columns():
        col = cav.air[(x, z)]
        top = col[-1]
        if mt.get(x, top, z) is not None or mt.get(x, top + 1, z) is None:
            continue
        r = rng.random()
        if r < blossom:
            mt.set(x, top, z, "minecraft:spore_blossom")
            n += 1
        elif r < blossom + roots:
            mt.set(x, top, z, "minecraft:hanging_roots", {"waterlogged": "false"})
            n += 1
    return n


@room("cavern", "spore_forest", faction="insect", strict_light=False, dark_limit=10.0)
def spore_forest(name):
    """Why: the owner's brief asks for 'forests of huge mushrooms' and 'glowing spore clouds (no vanilla particles: lights and blocks only)'. The rooms2 grottos already have
    mushrooms of three rows of cap; this is the forest itself, on a 45 x 36 floor with a tall cave over it: colossal red, brown and warped caps (up to 8 across and 18 rows
    high, the biggest ones flat umbrellas) that close into a canopy over a mycelium floor, shroomlight spots in their tops, hanging spore blossoms and roots under the
    cave roof, glowing glass clouds floating between the caps (stained glass round an invisible light block), a shallow pond and glow-berry vines. The party walks on the forest
    floor between stems that are three wide, and the lit canopy is what it sees; the insect faction (cave spiders, gricks) lives between the stems. The two ledges of the
    brief ('narrow ledges over chasms') are the next room's."""
    mt = tpl("cavern", name, 5, 4)
    rng = mt.rng
    doors = [("n", 13), ("n", 31), ("s", 22), ("w", 13)]
    for side, off in doors:
        mt.socket(side, off)
    cav = D.Cavern(name, mt.W, mt.D, doors, [(22, 17, 15, 11), (11, 9, 8, 7), (35, 26, 9, 7), (10, 27, 7, 6), (37, 9, 6, 6)], shrink=0.28, flat=9.0, floor_amp=1.8,
                   max_row=41, pools=[("auto", 2.5)])
    G2.cavern_open(mt, cav, rng)
    G2.ground_cover(mt, cav, rng, [(0.60, "minecraft:mycelium"), (0.47, "minecraft:podzol"), (0.36, "minecraft:moss_block")])
    kinds = [(K.MUSH["red"], None), (K.MUSH["brown"], None), (K.WART["warped"], "minecraft:warped_stem"), (K.MUSH["red"], None), (K.MUSH["brown"], None),
             (K.WART["warped"], "minecraft:warped_stem")]
    sites = G2.pick_sites(cav, rng, 70, 7.5, 3.5)
    placed, blocked = 0, set()
    for (x, z) in sites:
        if placed >= 9:
            break
        if not G2.wide_connected(cav, blocked | {(x, z)}):
            continue
        cap_block, stem_block = kinds[placed % len(kinds)]
        y0 = cav.floor[(x, z)] + 1

        def fit():
            for (h, sr, cr, rise, plate) in FOREST_SIZES:
                if K.giant_mushroom(mt, x, z, y0, h, sr, cr, rise, cap_block, plate=plate, stem_block=stem_block, rng=rng, spot_share=0.10):
                    return True
            return False
        if G2.keep_if_route(mt, fit):
            placed += 1
            blocked.add((x, z))
    if placed < 6:
        raise ValueError("%s: only %d colossal mushrooms fit" % (name, placed))
    G2.cave_markers(mt, cav, rng, "insect", count=8, patrol=3)
    for (x, z) in cav.columns():
        f = cav.floor[(x, z)]
        g = mt.get(x, f, z)
        if (x, z) in cav.lanes or g is None:
            continue
        if g.block in ("minecraft:mycelium", "minecraft:podzol") and rng.random() < 0.12:
            G2.plant(mt, x, f + 1, z, "minecraft:brown_mushroom" if rng.random() < 0.5 else "minecraft:red_mushroom")
        elif g.block == "minecraft:moss_block" and rng.random() < 0.2:
            G2.plant(mt, x, f + 1, z, "minecraft:moss_carpet")
    spore_clouds(mt, cav, rng, 7, ["minecraft:lime_stained_glass", "minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass", "minecraft:purple_stained_glass"])
    ceiling_growth(mt, cav, rng, 0.05, 0.06)
    G2.cave_vines(mt, cav, rng, 0.05, berries=0.5)
    G2.cave_lichen(mt, cav, rng, 0.05)
    G2.hidden_lights(mt, cav, rng, 6.0, 11, height=3)
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 3. fungus_farm: the terraced fungus beds of the myconids' servants
# =====================================================================================================================
def mini_mushroom(mt, x, y, z, height, cap, stem=None, plus=True):
    """A small cultivated mushroom whose stem starts at row `y`: a stem of `height` and a plus-shaped cap (or a single cap block), written with the same face states as the giant
    ones. Nothing is written when a cell is taken. Returns whether it was placed."""
    stem_cells = {(x, y + k, z) for k in range(height)}
    cap_cells_ = {(x, y + height, z)}
    if plus:
        cap_cells_ |= {(x + 1, y + height - 1, z), (x - 1, y + height - 1, z), (x, y + height - 1, z + 1), (x, y + height - 1, z - 1),
                       (x + 1, y + height, z), (x - 1, y + height, z), (x, y + height, z + 1), (x, y + height, z - 1)}
        stem_cells = {(x, y + k, z) for k in range(height - 1)}
    for c in stem_cells | cap_cells_:
        if not K.free(mt, *c):
            return False
    K.lay_mushroom(mt, cap_cells_, stem_cells, cap, spots=False, stem_block=stem)
    return True


def bed_ring(mt, qx, qz, size=7):
    """The stone frame of a bed: the outer ring of a size x size square at row FR + 1, alternating blocks and low slabs."""
    for z in range(qz, qz + size):
        for x in range(qx, qx + size):
            if x in (qx, qx + size - 1) or z in (qz, qz + size - 1):
                if (x + z) % 2:
                    mt.set(x, FR + 1, z, L.POLISHED_SLAB, {"type": "bottom"})
                else:
                    mt.set(x, FR + 1, z, L.POLISHED)


def fill_soil(mt, qx, qz, soil, size=7):
    """The soil of a bed inside its frame: a full tier at row FR + 1 and a 3 x 3 second tier at row FR + 2."""
    for z in range(qz + 1, qz + size - 1):
        for x in range(qx + 1, qx + size - 1):
            mt.set(x, FR + 1, z, soil)
            if qx + 2 <= x <= qx + size - 3 and qz + 2 <= z <= qz + size - 3:
                mt.set(x, FR + 2, z, soil)


def deck_with_flight(mt, west):
    """The overseers' loft on one long wall: a deck at row FR + 5 (stand row FR + 6) over the doorway of that wall (z 10..16, five cells deep), a rail on its open edges, and a
    three-wide flight of six steps up to it from the aisle beside the beds, with a solid mass under the steps. `west` picks the side."""
    GAL = FR + 5
    xa, xb = (2, 7) if west else (19, 24)
    inner = xb if west else xa
    for z in range(10, 17):
        for x in range(xa, xb + 1):
            mt.set(x, GAL, z, L.TILE if (x + z) % 3 else L.POLISHED)
    for z in range(10, 17):
        mt.set(inner, GAL + 1, z, L.POLISHED_WALL, {"up": "true"})
    for x in range(min(xa, xb), max(xa, xb) + 1):
        if x != (2 if west else 24):
            mt.set(x, GAL + 1, 16, L.POLISHED_WALL, {"up": "true"})
    # the flight: x 8..10 (west) or 16..18 (east), climbing north from z = 16 to z = 11; the steps end at z = 10 against the deck
    fx = (8, 9, 10) if west else (16, 17, 18)
    for k in range(6):
        z = 16 - k
        for x in fx:
            for y in range(FR + 1, FR + 1 + k):
                mt.set(x, y, z, L.BRICK)
            mt.set(x, FR + 1 + k, z, L.TILE_STAIRS, st("north"))
    for x in fx:
        mt.set(x, GAL, 10, L.TILE)


@room("hall", "fungus_farm", faction="goblin", strict_light=True)
def fungus_farm(name):
    """Why: the brief asks for 'a fungus farm' among the signature spaces of the Underdark: the places the myconids' servants grow what the deep eats. This hall is that farm
    on a 27 x 27 floor: four terraced beds (red mushrooms on mycelium with a grown one in the middle, brown ones on podzol, warped fungus on nylium with a small warped tree, and a
    processing corner with composters, barrels, a grindstone, cauldrons and potted crops), a five-wide crossing of walks between the four doors, two lofts for the overseers over the
    west and east doors (reached by flights of six steps; a bed, chests, a ledger and barrels up there, a rail round them) and grow-lamps (shroomlight blocks hung on chains over each
    bed). Everything on the floor is raised or low, so the walks stay clear and the beds are cover for a fight; the lofts are the high ground. The goblin faction (the farm's hands) comes
    out of the stalks. It is the calm-looking hall of the theme (no chasm, no spire), and its colours (the red, brown and teal of the crops against black stone) make it unmistakable."""
    mt = tpl("hall", name, 3, 3)
    rng = mt.rng
    H = 12
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    for (side, off) in (("n", 13), ("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    GAL = FR + 5
    # ---- the floor: a polished border, a spore medallion where the walks cross
    for z in range(2, 25):
        for x in range(2, 25):
            if (x in (2, 24) or z in (2, 24)) and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    web_medallion(mt, 13, 13, 4, rings=(2.5,), spokes=8)
    # ---- the four quadrants: 7 x 7 at x and z 3 and 17
    quads = {"nw": (3, 3), "ne": (17, 3), "sw": (3, 17), "se": (17, 17)}
    for key, (qx, qz) in quads.items():
        bed_ring(mt, qx, qz)
    fill_soil(mt, 3, 3, "minecraft:mycelium")
    fill_soil(mt, 17, 3, "minecraft:podzol")
    fill_soil(mt, 3, 17, "minecraft:warped_nylium")
    # crops: red and brown mushrooms in a chequer, a grown red one in the middle of the north-west bed
    for z in range(4, 9):
        for x in range(4, 9):
            y = FR + 3 if (5 <= x <= 7 and 5 <= z <= 7) else FR + 2
            if (x, z) == (6, 6):
                mini_mushroom(mt, x, y, z, 4, K.MUSH["red"])
            elif K.free(mt, x, y, z):
                mt.set(x, y, z, "minecraft:red_mushroom" if (x + z) % 2 == 0 else "minecraft:brown_mushroom")
    for z in range(4, 9):
        for x in range(18, 23):
            y = FR + 3 if (19 <= x <= 21 and 5 <= z <= 7) else FR + 2
            if (x, z) == (20, 6):
                mini_mushroom(mt, x, y, z, 4, K.MUSH["brown"])
            elif (x, z) in ((19, 5), (21, 7)):
                mini_mushroom(mt, x, y, z, 2, K.MUSH["brown"], plus=False)
            elif K.free(mt, x, y, z) and (x + z) % 3:
                mt.set(x, y, z, "minecraft:brown_mushroom")
    for z in range(18, 23):
        for x in range(4, 9):
            y = FR + 3 if (5 <= x <= 7 and 19 <= z <= 21) else FR + 2
            if (x, z) == (6, 20):
                for k in range(4):
                    mt.set(x, y + k, z, "minecraft:warped_stem", {"axis": "y"})
                for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1), (0, 0)):
                    put(mt, x + dx, y + 4, z + dz, "minecraft:warped_wart_block")
                for (dx, dz) in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    put(mt, x + dx, y + 3, z + dz, "minecraft:warped_wart_block")
                put(mt, x, y + 5, z, "minecraft:shroomlight")
            elif K.free(mt, x, y, z):
                mt.set(x, y, z, rng.choice(("minecraft:warped_fungus", "minecraft:warped_roots", "minecraft:nether_sprouts", "minecraft:warped_roots")))
    # ---- the processing corner (south-east): composters, barrels, a grindstone, cauldrons, hay, a lectern with the farmer's ledger
    for z in range(18, 23):
        for x in range(18, 23):
            put(mt, x, FR + 1, z, L.TILE)
    for x in (18, 19, 20):
        mt.set(x, FR + 2, 22, "minecraft:composter", {"level": str(rng.randint(2, 7))})
    mt.set(22, FR + 2, 22, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(22, FR + 3, 22, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(22, FR + 2, 21, "minecraft:barrel", {"facing": "west", "open": "false"})
    mt.set(22, FR + 2, 20, "minecraft:cauldron")
    mt.set(22, FR + 2, 19, "minecraft:water_cauldron", {"level": "3"})
    mt.set(18, FR + 2, 18, "minecraft:grindstone", {"face": "floor", "facing": "east"})
    mt.set(20, FR + 2, 18, "minecraft:lectern", {"facing": "south", "has_book": "false", "powered": "false"})
    for (x, z) in ((19, 20), (20, 20), (19, 21), (20, 21)):
        mt.set(x, FR + 2, z, "minecraft:hay_block", {"axis": "y"})
    mt.set(19, FR + 3, 20, "minecraft:hay_block", {"axis": "x"})
    mt.set(21, FR + 2, 19, "minecraft:potted_red_mushroom")
    mt.set(18, FR + 2, 20, "minecraft:potted_brown_mushroom")
    # ---- the lofts and their flights, then what stands on them
    deck_with_flight(mt, True)
    deck_with_flight(mt, False)
    f = L.Frame(mt, 3, GAL + 1, 12, "east")
    f.put(0, 0, 0, "minecraft:purple_bed", {"facing": "east", "part": "foot"})
    f.put(0, 0, 1, "minecraft:purple_bed", {"facing": "east", "part": "head"})
    mt.t.chest(3, GAL + 1, 14, K.LOOT, "east")
    mt.set(3, GAL + 1, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(5, GAL + 1, 11, "minecraft:potted_warped_fungus")
    mt.set(23, GAL + 1, 11, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    for z in (13, 14, 15):
        mt.set(23, GAL + 1, z, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(23, GAL + 2, 14, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(21, GAL + 1, 12, "minecraft:hay_block", {"axis": "y"})
    # ---- grow-lamps: a shroomlight on chains over the middle of every bed, soul lanterns on slender posts at the corners of the walks
    for (lx, lz) in ((6, 6), (20, 6), (6, 20), (20, 20)):
        for y in range(FR + 9, top + 1):
            mt.set(lx, y, lz, L.CHAIN_BLOCK, {"axis": "y"})
        mt.set(lx, FR + 8, lz, "minecraft:shroomlight")
    for (px, pz) in ((10, 8), (16, 8), (10, 18), (16, 18)):
        D.lamp_post(mt, px, FR + 1, pz, height=3, lantern=L.SOUL_LANTERN)
    # ---- the walls: shelves of potted crops between the doors, banners
    for z in (5, 6, 20, 21):
        for wx in (2, 24):
            put(mt, wx, FR + 3, z, L.POLISHED_SLAB, {"type": "top"})
            put(mt, wx, FR + 4, z, rng.choice(("minecraft:potted_red_mushroom", "minecraft:potted_brown_mushroom", "minecraft:potted_warped_fungus")))
    for x in (5, 6, 20, 21):
        for wz in (2, 24):
            put(mt, x, FR + 3, wz, L.POLISHED_SLAB, {"type": "top"})
            put(mt, x, FR + 4, wz, rng.choice(("minecraft:potted_red_mushroom", "minecraft:potted_brown_mushroom", "minecraft:potted_warped_fungus")))
    for z in (9, 17):
        K.banner(mt, 2, FR + 8, z, "east", "purple")
        K.banner(mt, 24, FR + 8, z, "west", "purple")
    for x in (9, 17):
        K.banner(mt, x, FR + 8, 2, "south", "purple")
        K.banner(mt, x, FR + 8, 24, "north", "purple")
    K.auto_markers(mt, "goblin", spawns=6, patrols=2, spacing=5)
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 4. myconid_circle: the melding circle, the set piece of the theme
# =====================================================================================================================
@room("set", "myconid_circle", faction="slime", strict_light=True, route_width="wide")
def myconid_circle(name):
    """Why: every sector has one room the party remembers; for the Underdark that is the place the brief names 'a myconid circle': the round clearing where the mushroom folk meld
    their minds. A 45 x 45 domed chamber (radius 20, four doors on the compass points) with twelve elder mushrooms standing in a ring at radius 14.5, their caps (five to six wide)
    touching to close a canopy over the outer walk; inside the ring a lawn of mycelium, moss and podzol; at the heart a round pit one block deep with stairs down on the four axes,
    six toadstool stools round a plain mycelium floor, and over it a great glowing cloud (a glass sphere three across round an invisible light) with six smaller clouds in orbit,
    each a different colour: the spores of the meld, made of glass and light and no particle. The party fights on the lawn and the pit (spawn markers on both), the stools and the
    elders' stems are cover, and the canopy makes it the one room of the theme that is lit from above."""
    mt = tpl("set", name, 5, 5)
    rng = mt.rng
    cx = cz = 22
    R = 20
    drum = FR + 18
    base = drum + 1
    dome_h = 10
    room_fn = lambda x, z: D.in_disc(x, z, cx, cz, R)  # noqa: E731
    for side in L.SIDES:
        mt.socket(side, 22)
    apex = G2.dome_row(0, R + 0.5, base, dome_h)
    G2.solid_mass(mt, room_fn, FR - 1, apex + 1, rng=rng)
    PIT = 7.5
    # ---- floor: two rows of rock under the room, a lawn of living soil on the walk, tile on the four axes, a pit with a stair on each axis
    for z in range(mt.D):
        for x in range(mt.W):
            if not room_fn(x, z):
                continue
            r = dist(x, z, cx, cz)
            mt.set(x, FR - 1, z, L.ROCK)
            on_axis = min(abs(x - cx), abs(z - cz)) <= 1
            if r <= PIT:
                mt.set(x, FR - 1, z, L.TILE if r > 4.2 else "minecraft:mycelium")
                continue
            n = D.noise3(x / 4.0, 0.4, z / 4.0, 77)
            if on_axis and r > PIT + 1.2:
                mt.set(x, FR, z, L.POLISHED if (x + z) % 2 == 0 else L.TILE)
            elif r > 9.0 and n > 0.62:
                mt.set(x, FR, z, "minecraft:mycelium")
            elif r > 9.0 and n > 0.5:
                mt.set(x, FR, z, "minecraft:podzol")
            elif r > 9.0 and n > 0.38:
                mt.set(x, FR, z, "minecraft:moss_block")
            else:
                mt.set(x, FR, z, L.CRACKED_TILE if rng.random() < 0.16 else L.TILE)
    for (dx, dz, face) in ((0, -1, "north"), (0, 1, "south"), (-1, 0, "west"), (1, 0, "east")):
        for lat in (-1, 0, 1):
            for r in (8, 9):
                x, z = cx + (dx * r if dx else lat), cz + (dz * r if dz else lat)
                if PIT < dist(x, z, cx, cz) <= PIT + 1.7:
                    mt.set(x, FR, z, L.TILE_STAIRS, st(face))
    # ---- the dome: a shell two thick, ribbed, over the drum
    for z in range(mt.D):
        for x in range(mt.W):
            if not room_fn(x, z):
                continue
            r = dist(x, z, cx, cz)
            ys = G2.dome_row(r, R + 0.5, base, dome_h)
            rib = min(abs(((D.angle_of(x, z, cx, cz) - 15.0 - 30.0 * k + 180.0) % 360.0) - 180.0) for k in range(12)) * math.radians(1.0) * r < 0.7
            for y in (ys, ys + 1):
                mt.set(x, y, z, L.POLISHED if (rib and y == ys) else (L.CRACKED_BRICK if rng.random() < 0.08 else L.BRICK))
    # ---- twelve elders at radius 14.5, their caps meeting overhead
    kinds = [(K.MUSH["red"], None), (K.MUSH["brown"], None), (K.WART["warped"], "minecraft:warped_stem")]
    for k, (ex, ez) in enumerate(D.around(cx, cz, 14.5, 12, 15.0)):
        cap_block, stem_block = kinds[k % 3]
        for (h, cr, rise) in ((9, 5, 3), (8, 5, 3), (7, 4, 3), (6, 4, 2)):
            if K.giant_mushroom(mt, ex, ez, FR + 1, h, 1, cr, rise, cap_block, stem_block=stem_block, rng=rng, spot_share=0.14):
                break
    # ---- the pit: stools of toadstool, the great cloud and its six companions
    for (sx, sz) in D.around(cx, cz, 4.6, 6, 30.0):
        mini_mushroom(mt, sx, FR, sz, 2, K.MUSH["brown"] if (sx + sz) % 2 else K.MUSH["red"])
    K.glow_sphere(mt, cx, FR + 8, cz, 3, "minecraft:lime_stained_glass", level=13)
    for k, (ox, oz) in enumerate(D.around(cx, cz, 6.2, 6, 0.0)):
        K.glow_sphere(mt, ox, FR + 6 + (k % 2), oz, 1, ["minecraft:magenta_stained_glass", "minecraft:light_blue_stained_glass", "minecraft:purple_stained_glass"][k % 3], level=11)
    # ---- the wall: ribs of lamps on corbels, banners, glow lichen
    for k, ang in enumerate([30.0 * i + 15.0 for i in range(12)]):
        x, z, face = G2.wall_cell(cx, cz, R, ang, room_fn)
        if (x, FR + 4, z) not in mt.reserved and mt.get(x, FR + 4, z) is None:
            mt.set(x, FR + 4, z, L.POLISHED_SLAB, {"type": "top"})
            put(mt, x, FR + 5, z, L.SOUL_LANTERN, {"hanging": "false"})
        if k % 2 == 0:
            K.banner(mt, x, FR + 9, z, face, "purple")
    for k in range(40):
        ang = rng.random() * 360.0
        x, z, face = G2.wall_cell(cx, cz, R, ang, room_fn)
        y = FR + rng.randint(2, 8)
        c = mt.get(x - L.STEP[face][0], y, z - L.STEP[face][1])
        if c is not None and mt.get(x, y, z) is None and (x, y, z) not in mt.reserved:
            props = {"north": "false", "south": "false", "east": "false", "west": "false", "up": "false", "down": "false", "waterlogged": "false"}
            props[{"north": "south", "south": "north", "east": "west", "west": "east"}[face]] = "true"
            mt.set(x, y, z, "minecraft:glow_lichen", props)
    # ---- markers: spawns on the lawn and in the pit, patrols on the ring, the centre in the pit
    K.auto_markers(mt, "slime", spawns=8, patrols=3, spacing=6, door_gap=7, avoid=[(cx, cz, 3.0)])
    mt.marker(cx, FR, cz, "room_center")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 5. chasm_ledges: narrow ledges over a chasm that ends in a dark lake
# =====================================================================================================================
@room("trap", "chasm_ledges", faction="insect", strict_light=True)
def chasm_ledges(name):
    """Why: the brief names 'narrow ledges over chasms' among the Underdark's places, and the theme needs a danger room that is not a fight: a 27 x 27 hall whose middle is a round
    chasm 15 across, a four-wide walk round it that joins the four doors (the safe way, with a three-wide route), and in the chasm a rock island with the room's chest, reached by a
    ledge one block wide that winds out from the north rim. A step off the ledge is a fall of four blocks into the dark lake at the bottom (water: no fall damage, three blocks deep,
    walled all round), from which a stepped shore on the south side climbs back to the walk, so the trap costs time and a soaking, never a life, and never leaves anyone stuck. The island
    is guarded: the insect faction nests on it (spawn markers) and three `trap;type=pit` markers sit on the ledge for the Dungeon Master to arm."""
    mt = tpl("trap", name, 3, 3)
    rng = mt.rng
    H = 14
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.10)
    top = FR + H
    for (side, off) in (("n", 13), ("s", 13), ("e", 13), ("w", 13)):
        mt.socket(side, off)
    cx = cz = 13
    CH = 6.9                                    # radius of the chasm's rim: the walk round it is five cells wide on the axes

    def rad(row):
        return CH - (FR - row) * 0.15           # the chasm narrows as it deepens

    # ---- the rock under the room, the chasm cut into it, the lake at its foot
    for z in range(mt.D):
        for x in range(mt.W):
            for row in range(1, FR):
                if (x, row, z) in mt.reserved:
                    continue
                mt.set(x, row, z, L.ROCK if (x * 7 + z * 3 + row) % 5 else L.COBBLED)
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, cx, cz)
            if d > CH:
                continue
            mt.remove(x, FR, z)
            for row in range(2, FR):
                if d <= rad(row):
                    if row <= 4 and d <= rad(row) - 0.7:
                        mt.set(x, row, z, "minecraft:water", {"level": "0"})
                    elif row > 4:
                        mt.remove(x, row, z)
                    else:
                        mt.set(x, row, z, L.ROCK)
    # the island: a rock column from the lake bed to the floor row, five cells across, and the ledge that reaches it
    island = [(x, z) for z in range(2, 25) for x in range(2, 25) if dist(x, z, cx, cz) <= 2.6]
    for (x, z) in island:
        for row in range(2, FR + 1):
            mt.set(x, row, z, L.ROCK if row < FR else ("minecraft:mycelium" if (x + z) % 2 else L.TILE))
    ledge = [(13, 5), (13, 6), (13, 7), (12, 7), (11, 7), (11, 8), (11, 9), (11, 10), (12, 10), (12, 11)]
    for (x, z) in ledge:
        mt.set(x, FR, z, L.COBBLED if (x + z) % 2 else L.TILE)
        for row, p_ in ((FR - 1, 0.7), (FR - 2, 0.35)):
            if rng.random() < p_:
                mt.set(x, row, z, L.COBBLED)
    for (x, z) in ledge[3:]:
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, z + dz) not in ledge and dist(x + dx, z + dz, cx, cz) > 2.6 and rng.random() < 0.18 and mt.get(x + dx, FR, z + dz) is None:
                mt.set(x + dx, FR, z + dz, L.COBBLED)
    # the shore: a stepped bank on the south side from the lake up to the walk (the water's surface is the top of row 4, the four steps are rows 5 to 8)
    for k, z in enumerate((16, 17, 18, 19)):
        for x in (12, 13, 14):
            for row in range(2, 5 + k):
                mt.set(x, row, z, L.ROCK)
            mt.set(x, 5 + k, z, L.TILE_STAIRS, st("south"))
            for row in range(6 + k, FR):
                mt.remove(x, row, z)
    # ---- the walk and the rim: a polished border, stalagmites
    for z in range(2, 25):
        for x in range(2, 25):
            if CH < dist(x, z, cx, cz) <= CH + 1.1 and mt.get(x, FR, z) is not None and (x + z) % 2 == 0:
                mt.set(x, FR, z, L.POLISHED)
    for (x, z) in ((2, 2), (24, 2), (2, 24), (24, 24), (6, 2), (20, 2), (2, 20), (24, 19), (7, 24), (19, 24), (2, 6), (24, 7)):
        D.dripstone(mt, x, FR + 1, z, rng.randint(2, 4), "up")
    for k in range(18):
        x, z = rng.randint(3, 23), rng.randint(3, 23)
        if dist(x, z, cx, cz) > 5.0 and mt.get(x, top, z) is None:
            D.dripstone(mt, x, top, z, rng.randint(2, 5), "down")
    for (px, py, pz) in ((3, FR + 1, 3), (23, FR + 1, 4), (4, FR + 1, 23), (22, FR + 1, 22)):
        mini_mushroom(mt, px, py, pz, 3, K.MUSH["red"] if px < 10 else K.MUSH["brown"])
    # ---- light: clouds over the chasm, lichen on the chasm wall, lamps on the walk
    for (sx, sy, sz, col) in ((13, FR + 8, 8, "minecraft:purple_stained_glass"), (8, FR + 7, 14, "minecraft:lime_stained_glass"), (18, FR + 9, 15, "minecraft:light_blue_stained_glass")):
        K.hang_sphere(mt, sx, sy, sz, top + 1, r=1, glass=col, level=12)
    for z in range(2, 25):
        for x in range(2, 25):
            d = dist(x, z, cx, cz)
            if 5.4 < d <= CH and rng.random() < 0.22:
                for row in (5, 6, 7):
                    c = mt.get(x, row, z)
                    if c is None:
                        for dx, dz, nm in G2.WALL_DIRS:
                            n = mt.get(x + dx, row, z + dz)
                            if n is not None and D.classify(n) == "solid" and dist(x + dx, z + dz, cx, cz) > d:
                                props = {k2: "false" for k2 in ("north", "south", "east", "west", "up", "down")}
                                props[nm] = "true"
                                props["waterlogged"] = "false"
                                mt.set(x, row, z, "minecraft:glow_lichen", props)
                                break
    for (lx, lz) in ((2, 9), (24, 9), (2, 17), (24, 17), (9, 2), (17, 2), (9, 24), (17, 24)):
        D.lamp_post(mt, lx, FR + 1, lz, height=2, lantern=L.SOUL_LANTERN)
    # ---- the island's treasure and the guard
    mt.t.chest(13, FR + 1, 14, K.LOOT, "north")
    candles(mt, 12, FR + 1, 15, 3, "purple")
    candles(mt, 14, FR + 1, 15, 2, "purple")
    for (bx, bz, rot) in ((12, 13, 5), (14, 12, 11)):
        mt.set(bx, FR + 1, bz, "minecraft:skeleton_skull", {"rotation": str(rot)})
    K.cobweb(mt, 11, FR + 3, 14)
    K.cobweb(mt, 15, FR + 3, 13)
    # ---- markers: the walk and the island, and three trap sites on the ledge
    K.auto_markers(mt, "insect", spawns=6, patrols=2, spacing=5, door_gap=5, edge=2)
    mt.marker(13, FR + 1, 12, "spawn", weight=2)
    mt.marker(14, FR + 1, 13, "spawn")
    for (x, z) in ((13, 6), (11, 8), (11, 10)):
        mt.marker(x, FR + 1, z, "trap", type="pit")
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 6. darklake: a black lake with a causeway and an island
# =====================================================================================================================
def lake_floor_fill(mt, cav, cx, cz, radius):
    """Rock under every cave column within `radius` of (cx, cz), from row 3 up to the column's floor, where nothing is set: the lake is dug four blocks deep, and a water cell with an
    empty cell beside it would flow out, so the banks must be solid all the way down (the cave builder only keeps the visible two rows under a floor)."""
    n = 0
    for (x, z) in cav.columns():
        if dist(x, z, cx, cz) <= radius:
            for row in range(3, cav.floor[(x, z)] + 1):
                if mt.get(x, row, z) is None and (x, row, z) not in mt.reserved:
                    mt.set(x, row, z, L.ROCK)
                    n += 1
    return n


@room("cavern", "darklake", faction="aquatic", strict_light=False, dark_limit=10.0)
def darklake(name):
    """Why: the brief names 'a cavern lake' among the Underdark's places: Darklake, the black water under the world. This is a 36 x 36 cave with a lake seventeen across and four deep
    in the middle (water to the floor's level, a bed of rock, banks that are solid all the way down), a dike three wide that carries the party across it from the west bank to the east
    one, a narrow spur of the dike to an island with an obelisk of black stone, a chest and candles, glowing clouds floating over the water, shore mushrooms, hanging glow vines and
    stalactites. A party that wades off the dike swims (the water is deep but the banks are flat with the water, so anyone can climb out); the aquatic faction (lizardmen and the
    drowned) comes out of the water on the shores."""
    mt = tpl("cavern", name, 4, 4)
    rng = mt.rng
    doors = [("w", 22), ("e", 13), ("s", 13)]
    for side, off in doors:
        mt.socket(side, off)
    LX, LZ, LR = 18, 18, 7.5
    cav = D.Cavern(name, mt.W, mt.D, doors, [(18, 17, 15, 12), (9, 26, 7, 6), (28, 9, 7, 6), (27, 28, 7, 6)], shrink=0.36, flat=7.0, floor_amp=1.4, max_row=36,
                   pools=[(LX, LZ, LR)])
    G2.cavern_open(mt, cav, rng)
    if len(cav.pool) < 120:
        raise ValueError("%s: the lake did not fit (%d cells)" % (name, len(cav.pool)))
    lake_floor_fill(mt, cav, LX, LZ, LR + 4)
    # the dike: three wide along z = 18 from the west bank to the east bank, and a one-wide spur south to the island at (18, 22)
    dike = {(x, z) for (x, z) in cav.pool if abs(z - 18) <= 1} | {(x, z) for (x, z) in cav.pool if x == 18 and 18 <= z <= 20}
    island = {(x, z) for z in range(19, 28) for x in range(12, 25) if dist(x, z, 18, 22) <= 2.6 and (x, z) in cav.pool}
    for (x, z) in cav.pool:
        if (x, z) in dike or (x, z) in island:
            for row in range(3, FR):
                put(mt, x, row, z, L.ROCK, over=True)
            mt.set(x, FR, z, L.TILE if (x + z) % 3 else L.POLISHED)
            continue
        put(mt, x, 3, z, L.ROCK)
        mt.set(x, 4, z, L.ROCK)
        for row in range(5, FR + 1):
            mt.set(x, row, z, "minecraft:water", {"level": "0"})
    for (x, z) in island:
        mt.set(x, FR, z, "minecraft:mycelium" if (x + z) % 2 else L.TILE)
    K.spire(mt, 18, 22, FR + 1, 9, 1.6, L.POLISHED, tip=L.CHISELED)
    for (dx, dz) in ((2, 0), (-2, 0), (0, 2), (0, -2)):
        put(mt, 18 + dx, FR + 1, 22 + dz, L.POLISHED_STAIRS, st({(2, 0): "west", (-2, 0): "east", (0, 2): "north", (0, -2): "south"}[(dx, dz)]))
    mt.t.chest(20, FR + 1, 23, K.LOOT, "west")
    candles(mt, 16, FR + 1, 23, 3, "purple")
    candles(mt, 19, FR + 1, 20, 2, "purple")
    # ---- shores, mushrooms, the cave's own growth
    G2.ground_cover(mt, cav, rng, [(0.60, "minecraft:mycelium"), (0.48, "minecraft:podzol"), (0.38, "minecraft:moss_block")], seed_off=3)
    kinds = [(K.MUSH["brown"], None), (K.MUSH["red"], None), (K.WART["warped"], "minecraft:warped_stem")]
    sites = [c for c in G2.pick_sites(cav, rng, 40, 8.0, 3.0) if dist(c[0], c[1], LX, LZ) > LR + 3.0]
    placed, blocked = 0, set()
    for (x, z) in sites:
        if placed >= 5:
            break
        if not G2.wide_connected(cav, blocked | {(x, z)}):
            continue
        cap_block, stem_block = kinds[placed % 3]
        y0 = cav.floor[(x, z)] + 1

        def fit():
            for (h, sr, cr, rise, plate) in FOREST_SIZES[2:]:
                if K.giant_mushroom(mt, x, z, y0, h, sr, cr, rise, cap_block, plate=plate, stem_block=stem_block, rng=rng, spot_share=0.10):
                    return True
            return False
        if G2.keep_if_route(mt, fit):
            placed += 1
            blocked.add((x, z))
    G2.cave_markers(mt, cav, rng, "aquatic", count=7, patrol=3)
    for (x, z) in cav.columns():
        f = cav.floor[(x, z)]
        g = mt.get(x, f, z)
        if (x, z) in cav.lanes or g is None or (x, z) in cav.pool:
            continue
        if g.block in ("minecraft:mycelium", "minecraft:podzol") and rng.random() < 0.10:
            G2.plant(mt, x, f + 1, z, "minecraft:brown_mushroom" if rng.random() < 0.5 else "minecraft:red_mushroom")
    spore_clouds(mt, cav, rng, 5, ["minecraft:light_blue_stained_glass", "minecraft:purple_stained_glass", "minecraft:lime_stained_glass"], r=2, min_d2=3.0, lo=5, hi=9)
    ceiling_growth(mt, cav, rng, 0.03, 0.04)
    G2.cave_vines(mt, cav, rng, 0.06, berries=0.5)
    G2.cave_lichen(mt, cav, rng, 0.05)
    G2.cave_stalactites(mt, cav, rng, 0.04)
    G2.hidden_lights(mt, cav, rng, 6.0, 11, height=3)
    K.finish(mt, ID)
    return mt


# =====================================================================================================================
# 7. myconid_garden: the calm room, a safe zone under a mushroom umbrella
# =====================================================================================================================
@room("safe", "myconid_garden", safe=True, seats=4, hearths=1, beds=3, min_light=6, route_width="wide")
def myconid_garden(name):
    """Why: a sector needs places to rest (the rest system reads seats, hearths and beds in a room of the `safe` folder), and the Underdark's is the one place where the mushroom folk
    tolerate strangers: a refuge garden of 18 x 18 under a flat umbrella of a giant mushroom held up by four slender stems, a soul fire in a ring of stone at its middle with four
    stool seats round it, three cots against the north wall, planters of glowing crops along the south wall, glow-berry vines hanging from the roof and a warm shroomlight under the cap, so
    it is the one bright warm room of a cold theme. No spawn, no faction, no chest: it is a room to breathe in, and the rest anchors (four seats, one hearth, three beds) are what the rest
    system reads."""
    mt = tpl("safe", name, 2, 2)
    rng = mt.rng
    H = 11
    mt.room(H, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE, wear=0.06)
    top = FR + H
    for (side, off) in (("s", 13), ("w", 13), ("e", 13)):
        mt.socket(side, off)
    # ---- the floor: moss and mycelium round the hearth, a polished ring
    for z in range(2, 16):
        for x in range(2, 16):
            d = dist(x, z, 8.5, 8.5)
            if d <= 4.4:
                mt.set(x, FR, z, "minecraft:moss_block" if (x + z) % 3 else "minecraft:mycelium")
            elif d <= 5.4:
                mt.set(x, FR, z, L.POLISHED)
    # ---- the hearth: a soul fire in a ring of low wall, four seats facing it
    for (x, z) in ((7, 7), (8, 7), (9, 7), (7, 8), (9, 8), (7, 9), (8, 9), (9, 9)):
        mt.set(x, GROUND, z, L.POLISHED_SLAB, {"type": "bottom"})
    K.hearth(mt, 8, GROUND, 8)
    K.seat(mt, 8, GROUND, 5, "south")
    K.seat(mt, 8, GROUND, 12, "north")
    K.seat(mt, 5, GROUND, 8, "east")
    K.seat(mt, 11, GROUND, 8, "west")
    # ---- the umbrella: four stems and a flat cap, a shroomlight under it
    stems = {(x, y, z) for (x, z) in ((4, 4), (12, 4), (4, 12), (12, 12)) for y in range(GROUND, FR + 8)}
    cap = K.cap_cells(8, FR + 8, 8, 6, 1, plate=True)
    K.lay_mushroom(mt, cap, stems, K.MUSH["red"], spots=True, spot_share=0.10, rng=rng)
    for (x, z) in ((8, 8), (6, 8), (10, 8), (8, 6), (8, 10)):
        put(mt, x, FR + 8, z, "minecraft:shroomlight", over=True)
    # ---- the cots on the north wall, with soul lanterns, and a lectern with the refuge's ledger
    for x in (3, 8, 13):
        K.bed(mt, x, GROUND, 3, "north", "gray" if x != 8 else "purple")
    for x in (5, 10):
        put(mt, x, GROUND, 2, "minecraft:potted_brown_mushroom")
    # ---- the planters of the south wall: raised soil with glowing crops, a composter, barrels, a water cauldron
    for x in range(3, 9):
        mt.set(x, GROUND, 14, L.POLISHED_SLAB, {"type": "bottom"})
        mt.set(x, GROUND, 15, "minecraft:mycelium")
        mt.set(x, GROUND + 1, 15, "minecraft:red_mushroom" if x % 2 else "minecraft:brown_mushroom")
    for x in (10, 11):
        mt.set(x, GROUND, 15, "minecraft:composter", {"level": "4"})
    mt.set(13, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(14, GROUND, 15, "minecraft:barrel", {"facing": "up", "open": "false"})
    mt.set(14, GROUND, 14, "minecraft:water_cauldron", {"level": "3"})
    mt.set(14, GROUND, 4, "minecraft:lectern", {"facing": "west", "has_book": "false", "powered": "false"})
    # ---- glow-berry vines and soft clouds under the roof, soul lanterns on the walls, banners
    for (x, z) in ((3, 6), (14, 6), (3, 10), (14, 11), (6, 14), (11, 3)):
        top_y = top
        has = "true" if rng.random() < 0.7 else "false"
        n = rng.randint(3, 5)
        for k in range(n - 1):
            put(mt, x, top_y - k, z, "minecraft:cave_vines_plant", {"berries": has})
        put(mt, x, top_y - n + 1, z, "minecraft:cave_vines", {"age": "20", "berries": has})
    for (x, z) in ((2, 5), (2, 12), (15, 5), (15, 12), (5, 2), (12, 2), (5, 15), (12, 15)):
        if mt.get(x, GROUND, z) is None:
            D.lamp_post(mt, x, GROUND, z, height=2, lantern=L.SOUL_LANTERN)
    for z in (6, 11):
        K.banner(mt, 2, GROUND + 4, z, "east", "lime")
        K.banner(mt, 15, GROUND + 4, z, "west", "lime")
    mt.marker(12, GROUND, 6, "patrol")
    mt.marker(5, GROUND, 11, "patrol")
    K.finish(mt, ID, minimum=6)
    return mt


if __name__ == "__main__":
    sys.exit(K.run(ROOMS, doc=__doc__, theme=ID))

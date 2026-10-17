#!/usr/bin/env python3
"""
The circulation set of the Mega-Dungeon (1.54.0, docs/labyrinth/mega.md and docs/labyrinth/mega_arch.md): the corridors, bends, tees, crossroads,
galleries, plazas and the crossroads hall a player walks between rooms, the door caps that end every unused doorway, and the straight link pieces that
join two sectors. Written with `mega_lib.MT`, so every piece obeys the connector contract (9-block module, height 48, floor row 8, 2-thick walls, 3x4
doorways at module centres) by construction.

    python3 tools/labyrinth/gen_mega_arch.py             # (re)writes every template of this builder
    python3 tools/labyrinth/gen_mega_arch.py --list      # names, sizes, jigsaws
    python3 tools/labyrinth/gen_mega_arch.py --check     # regenerates in memory and compares with the files on disk
    python3 tools/labyrinth/gen_mega_arch.py --light     # block light and walkability of every piece (exit 1 below the bar)
    python3 tools/labyrinth/mega_arch_check.py           # supports, floating blocks, open water, armour stands, on the files that ship
    python3 tools/labyrinth/gen_mega_arch.py bend_1x1    # only the pieces whose name contains the argument

Files: this one (corridors, junction chambers, galleries, link pieces, the command line), `mega_arch_kit.py` (the shared motifs: pilasters, ribs, niches,
arches, lamp posts, statues ...), `mega_arch_rooms.py` (the plazas and the crossroads hall), `mega_arch_caps.py` (the door caps), `mega_arch_check.py`
(static checks that run on every piece before it is written). Previews: `mega_shape_preview.py` (shape-aware isometric views, elevations, block light,
walkability) and `mega_walk_preview.py` (first person).

Why generated and not hand-built: about fifty pieces must agree on the same grid, the same door frames and the same palette, and the owner wants them
to look hand-crafted, which means rhythm (piers, ribs, niches, lanterns at a steady beat), variety (every piece is dressed three ways) and details in
the right places. Code gives the rhythm exactly and the variety deterministically: every random choice comes from a generator seeded with the
template's own name, so the same script writes the same bytes (`--check` proves it).

Why these shapes: the reference tabletop map (docs/labyrinth/mega.md section 14) is made of long straight corridors, bends, tees, crossroads, plazas and
colonnaded halls packed so that they touch and line up. All of them sit on the 9-grid with sockets at module centres, so any two touching pieces either
connect or are walled, and unused doorways become alcoves (the caps) instead of holes into the void.

Why block light is checked: the dungeon's gloom clock (labyrinth/dnd/economy/GloomClock) punishes every second at block light <= 4 with nothing lit in hand,
so a template that looks atmospheric but has a dark corner on the walking route would silently hurt players. `--light` floods the light from every
emitter and fails a piece whose darkest standing cell is below 5, whose sockets cannot be walked to from each other, or whose spawn / patrol markers sit
where nobody can stand.
"""
import argparse
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
from mega_lib import MT  # noqa: E402
import mega_arch_kit as K  # noqa: E402
from mega_arch_kit import N, E, S, W, ROW0, FLOOR, Style, Wall, seeded, hang_lantern, put_if_free, marker_standing  # noqa: E402
import mega_arch_rooms as R  # noqa: E402
import mega_arch_caps as C  # noqa: E402
import mega_arch_check as CK  # noqa: E402


# =====================================================================================================================
# the straight corridors (9 wide, 2, 3 or 4 modules long)
# =====================================================================================================================
STRAIGHT_STYLES = {
    # a: stone and lanterns with a red runner; b: dark oak ribs, wooden skirting, brown runner, shelves; c: the ruin: soul light, broken ribs, rubble, bones
    "a": Style("a", 0.10, rib="stone", floor="runner", lights="lantern", ends="banner", skirt="stone", carpet="red", carpet_w=3,
               clutter=[(2, "barrel"), (1, "none")]),
    "b": Style("b", 0.12, rib="timber", floor="checker", lights="mixed", carpet="brown", carpet_w=3, ends="lantern", skirt="wood",
               niches=[(3, "lantern"), (3, "banner"), (2, "shelf"), (2, "candles"), (1, "plain")], clutter=[(2, "barrel"), (1, "none")]),
    "c": Style("c", 0.26, rib="stone", floor="runner", lights="mixed", cobwebs=0.5, ends="sconce", light_every=2, skirt="stone", rubble=0.45, broken_ribs=0.4,
               niches=[(2, "sconce"), (2, "skull"), (1, "candles"), (2, "slit"), (1, "plain"), (1, "lantern")], clutter=[(2, "bones"), (1, "none")]),
}


def corridor_clutter(mt, style, side, cells, rng, top):
    """Props along the wall foot of a bay: a barrel or two, a pile of bones, a rubble heap, a cobweb in the upper corner."""
    x = K.side_x(side, 1)
    zc = cells[len(cells) // 2]
    if rng.random() < style.rubble:
        K.rubble_heap(mt, x, zc, K.inward(side), rng, size=rng.choice((2, 3)))
        return
    kind = rng.choice([k for w_, k in style.clutter for _ in range(w_)] or ["none"])
    if kind == "barrel" and mt.get(x, ROW0, zc) is None:
        K.barrel(mt, x, ROW0, zc)
        if rng.random() < 0.5:
            K.barrel(mt, x, ROW0 + 1, zc)
    elif kind == "bones" and mt.get(x, ROW0, zc) is None:
        K.bone_pile(mt, x, zc, rng)
    if rng.random() < style.cobwebs:
        put_if_free(mt, K.side_x(side, 2), top - 1, rng.choice([cells[0], cells[-1]]), "minecraft:cobweb")


def straight_corridor(modules, variant):
    """A 9 x 9*modules corridor with sockets N and S at offset 4 (the 1x3 and 1x4 also have side doors E and W at their inner module centres).
    A row of flush pilasters with coved ribs over them divides it into bays, each bay's walls carry a niche, the middle has a floor medallion and a
    patrol node, lanterns hang down the middle. The far ends of the long ones are drawn first by the assembler (selection priority 1) so a corridor goes
    somewhere before its side doors branch off."""
    name = "mega/corridor/straight_1x%d_%s" % (modules, variant)
    D = 9 * modules
    style = STRAIGHT_STYLES[variant]
    mt = MT(name, 1, modules)
    rng = seeded(mt)
    mt.room(6, wear=style.wear)
    z0, z1 = 2, D - 3
    top = ROW0 + 5
    side_doors = [9 * k + 4 for k in range(1, modules - 1)] if modules >= 3 else []
    piers = K.bay_layout(z0, z1, side_doors)
    through = 1 if modules >= 3 else 0
    mt.socket("n", 4, selection_priority=through)       # sockets first: they reserve the holes so nothing below can block a doorway
    mt.socket("s", 4, selection_priority=through)
    for c in side_doors:
        mt.socket("e", c)
        mt.socket("w", c)
    K.floor_pattern(mt, style, z0, z1, rng)
    K.cove(mt, z0, z1, set(piers))
    for z in piers:
        for side in ("w", "e"):
            K.pilaster(mt, side, z, style)
        K.rib(mt, z, style, broken=(z not in (z0, z1) and rng.random() < style.broken_ribs))
    door_skip = {z for z in range(z0, z1 + 1) if K.door_holes(side_doors, z, 1)}
    for side in ("w", "e"):
        if style.skirt:
            K.skirting(mt, side, z0, z1, door_skip, style.skirt)
    K.medallion(mt, 4, D // 2)
    kinds = [k for w_, k in style.niches for _ in range(w_)]
    for side in ("w", "e"):                              # one feature per gap between pilasters, never the same twice in a row on one side
        wall = Wall(mt, side)
        last = None
        for a, b in zip(piers, piers[1:]):
            cells = list(range(a + 1, b))
            if any(K.door_holes(side_doors, z, 1) for z in cells):
                continue
            kind = rng.choice([k for k in kinds if k != last] or kinds)
            K.niche(wall, cells, kind, rng, style)
            last = kind
            corridor_clutter(mt, style, side, cells, rng, top)
    for side in ("w", "e"):
        K.string_course(mt, side, z0, z1, skip=set(piers) | door_skip)
    K.lights_for(mt, style, [(a + b) // 2 for a, b in zip(piers, piers[1:])][::style.light_every])
    K.end_dressing(mt, style, z0, S, rng)
    K.end_dressing(mt, style, z1, N, rng)
    mid = D // 2
    marker_standing(mt, 4, mid, "patrol")
    for z in ([mid - 6, mid + 6] if modules >= 4 else ([mid + 5] if modules >= 2 else [])):
        marker_standing(mt, 4, z, "spawn")
    if variant == "c":
        marker_standing(mt, 3, mid, "faction", faction="undead")      # the ruin is bones and skulls: the dead hold it
    mt.finish()
    return mt


# =====================================================================================================================
# the 1x1 junction chambers: bend, tee, cross
# =====================================================================================================================
CHAMBER_SOCKETS = {"bend": ("n", "e"), "tee": ("n", "e", "w"), "cross": ("n", "e", "s", "w")}
CHAMBER_STYLES = {
    "a": Style("a", 0.10, rib="stone", floor="runner", lights="lantern"),
    "b": Style("b", 0.12, rib="timber", floor="checker", lights="mixed", carpet="red"),
    "c": Style("c", 0.28, rib="stone", floor="runner", lights="mixed", cobwebs=0.7),
}
#: what fills the blank wall(s) of a bend (two walls) or a tee (one wall), per variant
CHAMBER_FEATURES = {
    ("bend", "a"): {"s": "statue", "w": "brazier"},
    ("bend", "b"): {"s": "library", "w": "window"},
    ("bend", "c"): {"s": "ruin", "w": "skulls"},
    ("tee", "a"): {"s": "shrine"},
    ("tee", "b"): {"s": "library"},
    ("tee", "c"): {"s": "ruin"},
}
CHAMBER_SPAWN = {"bend": (4, 3), "tee": (4, 3), "cross": (3, 3)}


def chamber(kind, variant):
    """A 9x9 junction chamber: a 5x5 interior with a plus-shaped floor inlay, four corner piers carrying banners, a cross of coved ribs with a boss and a
    chandelier. The blank walls of a bend and a tee hold a tall niche with a feature (a statue, a shrine, a brazier, a library, a window, a ruin).
    Why corner piers: the doorways of a 9x9 module are 3 wide in a 5-wide wall, so the chamber's plan is already a plus; the piers fill the four
    corners the way a vault's groin ribs would and give the ribs something to stand on."""
    name = "mega/corridor/%s_1x1_%s" % (kind, variant)
    style = CHAMBER_STYLES[variant]
    mt = MT(name, 1, 1)
    rng = seeded(mt)
    H = 7
    top = ROW0 + H - 1
    mt.room(H, wear=style.wear)
    socks = CHAMBER_SOCKETS[kind]
    for f in socks:
        mt.socket(f, 4)
    for x in range(2, 7):
        for z in range(2, 7):
            if 3 <= x <= 5 or 3 <= z <= 5:
                mt.set(x, FLOOR, z, L.POLISHED if (x + z) % 2 == 0 or style.floor == "runner" else L.TILE)
            elif rng.random() < 0.25:
                mt.set(x, FLOOR, z, L.CRACKED_TILE)
    K.medallion(mt, 4, 4, 1, L.CHISELED, L.TILE)
    if style.carpet:
        for f in socks:
            w = Wall(mt, f)
            for d in range(1, 3):
                x, y, z = w.cell(4, d, FLOOR)
                mt.set(x, y, z, K.runner_block(style.carpet))
        mt.set(4, FLOOR, 4, K.runner_block(style.carpet))
    for cx, cz in ((2, 2), (6, 2), (2, 6), (6, 6)):
        K.pillar(mt, cx, cz, ROW0, top, L.STRIPPED_LOG if style.rib == "timber" else L.POLISHED, "y")
        mt.set(cx, ROW0, cz, L.CHISELED)
        mt.set(cx, ROW0 + 3, cz, L.CHISELED)
        mt.set(cx, top, cz, L.POLISHED)
    for z in range(3, 6):
        K.stair(mt, 2, top, z, L.TILE_STAIRS, W, "top")
        K.stair(mt, 6, top, z, L.TILE_STAIRS, E, "top")
    for x in range(3, 6):
        K.stair(mt, x, top, 2, L.TILE_STAIRS, N, "top")
        K.stair(mt, x, top, 6, L.TILE_STAIRS, S, "top")
    band = L.LOG if style.rib == "timber" else L.POLISHED
    for x in range(2, 7):
        mt.set(x, top, 4, band, {"axis": "x"} if style.rib == "timber" else None)
    for z in range(2, 7):
        if z != 4:
            mt.set(4, top, z, band, {"axis": "z"} if style.rib == "timber" else None)
    mt.set(4, top, 4, L.CHISELED)
    corbel = L.PLANKS_STAIRS if style.rib == "timber" else L.POLISHED_STAIRS
    for z, f in ((2, N), (6, S)):
        K.stair(mt, 4, top - 1, z, corbel, f, "top")
    for x, f in ((2, W), (6, E)):
        K.stair(mt, x, top - 1, 4, corbel, f, "top")
    K.chandelier(mt, 4, top, 4, drop=2, radius=1, soul=(variant == "c"), arm_lift=0)       # a flat plus of five lanterns: the arms as low as the middle keeps the c variants lit
    if variant != "c":                                    # banners on the inner faces of the corner piers
        cols = ("red", "black") if variant == "a" else ("purple", "purple")
        for (cx, cz, fx, fz) in ((2, 2, E, S), (6, 2, W, S), (2, 6, E, N), (6, 6, W, N)):
            put_if_free(mt, cx + (1 if fx == E else -1), ROW0 + 3, cz, "minecraft:%s_wall_banner" % cols[0], {"facing": fx})
            put_if_free(mt, cx, ROW0 + 3, cz + (1 if fz == S else -1), "minecraft:%s_wall_banner" % cols[1], {"facing": fz})
    feats = CHAMBER_FEATURES.get((kind, variant), {})
    for f in ("n", "e", "s", "w"):
        if f not in socks and feats.get(f):
            K.tall_niche(Wall(mt, f), (3, 4, 5), feats[f], rng)
    for cx, cz in ((3, 3), (5, 3), (3, 5), (5, 5)):
        if rng.random() < style.cobwebs:
            put_if_free(mt, cx, top - 1, cz, "minecraft:cobweb")
    marker_standing(mt, 4, 4, "patrol")
    marker_standing(mt, *CHAMBER_SPAWN[kind], kind="spawn")
    if variant == "c":
        marker_standing(mt, 5, 5, "faction", faction="undead")
    mt.finish()
    return mt


# =====================================================================================================================
# the gallery: a colonnaded hall, 18 x 36, two arcades of pointed arches between a nave and two aisles
# =====================================================================================================================
GALLERY_STYLES = {
    "a": Style("a", 0.10, rib="stone", floor="runner", lights="lantern", carpet="red", niches=[(3, "lantern"), (3, "banner"), (1, "candles")]),
    "b": Style("b", 0.14, rib="stone", floor="checker", lights="mixed", cobwebs=0.4, niches=[(2, "sconce"), (2, "banner"), (2, "candles"), (1, "skull")]),
}
GALLERY_DOORS = (4, 13, 22, 31)
GALLERY_PIERS = [(8, 9), (17, 18), (26, 27)]
GALLERY_SPANS = [(2, 7), (10, 16), (19, 25), (28, 33)]
GALLERY_BAYS = ((7, 10), (16, 19), (25, 28))


def arcade_line(mt, x, top, y_spring=ROW0 + 6):
    """The arcade wall along x: pointed arches over the free spans between the pier pairs, polished piers (chiselled at the base, the springing and the
    top) between them, wall above the arches up to row `top`."""
    for (za, zb) in GALLERY_SPANS:
        K.arch_span(mt, "z", x, za, zb, y_spring, top)
    for (za, zb) in GALLERY_PIERS:
        for z in range(za, zb + 1):
            for y in range(ROW0, top + 1):
                mt.set(x, y, z, L.POLISHED)
            mt.set(x, ROW0, z, L.CHISELED)
            mt.set(x, y_spring - 1, z, L.CHISELED)
            mt.set(x, top, z, L.CHISELED)


def gallery(variant):
    """18 x 36, interior 14 x 32 and 12 high: a four-wide nave between two four-wide aisles, divided by arcades of 7- and 6-wide pointed arches on
    pier pairs, ribs across the whole width over the piers, chandeliers in the nave, lamps in the aisles and under every keystone, a glowing apse window
    at each end of the nave. Twelve doors: all four module centres of both long sides and both of each end (the ends are the through-ways).
    `a` is the processional (red runner, banners), `b` the hall of statues (a gauntlet of armour in front of the piers, lamp posts, cobwebs)."""
    name = "mega/corridor/gallery_2x4_%s" % variant
    style = GALLERY_STYLES[variant]
    mt = MT(name, 2, 4)
    rng = seeded(mt)
    H = 12
    top = ROW0 + H - 1
    mt.room(H, wear=style.wear)
    for z in GALLERY_DOORS:
        mt.socket("e", z)
        mt.socket("w", z)
    for x in (4, 13):
        mt.socket("n", x, selection_priority=1)
        mt.socket("s", x, selection_priority=1)
    z0, z1 = 2, 33
    for z in range(z0, z1 + 1):
        for x in range(2, 16):
            if 7 <= x <= 10:
                mt.set(x, FLOOR, z, L.POLISHED if x in (8, 9) or z % 2 == 0 else L.TILE)
            elif x in (6, 11):
                mt.set(x, FLOOR, z, L.CHISELED if (z - 2) % 9 in (6, 7) else L.POLISHED)
            elif rng.random() < 0.12:
                mt.set(x, FLOOR, z, L.CRACKED_TILE)
    for dz in GALLERY_DOORS:
        for x in range(2, 6):
            for z in range(dz - 1, dz + 2):
                mt.set(x, FLOOR, z, L.POLISHED)
                mt.set(17 - x, FLOOR, z, L.POLISHED)
    if style.carpet:
        for z in range(z0, z1 + 1):
            for x in (8, 9):
                mt.set(x, FLOOR, z, K.runner_block(style.carpet))
    for x in (6, 11):
        arcade_line(mt, x, top)
    rib_zs = {z for pair in GALLERY_PIERS for z in pair}
    for z in rib_zs:
        for x in range(2, 16):
            if mt.get(x, top, z) is None:
                mt.set(x, top, z, L.POLISHED)
        K.stair(mt, 2, top - 1, z, L.POLISHED_STAIRS, W, "top")
        K.stair(mt, 15, top - 1, z, L.POLISHED_STAIRS, E, "top")
    for z in range(z0, z1 + 1):
        if z in rib_zs:
            continue
        K.stair(mt, 2, top, z, L.TILE_STAIRS, W, "top")
        K.stair(mt, 15, top, z, L.TILE_STAIRS, E, "top")
        for x, f in ((5, E), (7, W), (10, E), (12, W)):
            if mt.get(x, top, z) is None:
                K.stair(mt, x, top, z, L.TILE_STAIRS, f, "top")
    skip = {z for z in range(z0, z1 + 1) if K.door_holes(GALLERY_DOORS, z, 1)}
    kinds = [k for w_, k in style.niches for _ in range(w_)]
    for side in ("w", "e"):
        K.skirting(mt, side, z0, z1, skip, "stone")
        wall = Wall(mt, side)
        last = None
        for a, b in GALLERY_BAYS:
            kind = rng.choice([k for k in kinds if k != last] or kinds)
            K.niche(wall, list(range(a, b + 1)), kind, rng, style)
            last = kind
        K.string_course(mt, side, z0, z1, skip=skip | rib_zs | {z for a, b in GALLERY_BAYS for z in range(a, b + 1)})
    for (za, zb) in GALLERY_PIERS:
        for z, col in ((za, "red"), (zb, "black")):
            K.wall_banner(mt, 7, ROW0 + 5, z, E, col)
            K.wall_banner(mt, 10, ROW0 + 5, z, W, col)
            if variant == "b":
                K.wall_banner(mt, 5, ROW0 + 5, z, W, "purple")
                K.wall_banner(mt, 12, ROW0 + 5, z, E, "purple")
        for x in (8, 9):
            hang_lantern(mt, x, top, za, drop=4, soul=(style.lights == "mixed" and x == 9))
        hang_lantern(mt, 3, top, za, drop=5, soul=True)
        hang_lantern(mt, 14, top, zb, drop=5, soul=True)
    for z in (5, 13, 22, 30):                             # the bay centres carry no rib, so these chains start at the ceiling row itself
        for x in (3, 14):
            hang_lantern(mt, x, top + 1, z, drop=6, soul=(style.lights == "soul"))
    for x in (6, 11):                                    # a lantern under the keystone of every arch lights the nave and both aisles at once
        for (za, zb) in GALLERY_SPANS:
            zc = (za + zb) // 2
            key = next(y for y in range(ROW0 + 6, top + 1) if mt.get(x, y, zc) is not None)      # the first solid cell above the opening
            hang_lantern(mt, x, key, zc, drop=1, soul=(style.lights == "mixed" and x == 11))
    for face in ("n", "s"):                              # the nave's ends: a tall apse niche between the end doors, a glowing window over an altar
        w = Wall(mt, face)
        cells = [7, 8, 9, 10]
        K.carve_niche(w, cells, tuple(range(ROW0 + 1, ROW0 + 9)))
        for c in cells:
            w.set(c, 0, ROW0 + 1, L.POLISHED)
        for c in (8, 9):
            w.set(c, 0, ROW0 + 2, L.CHISELED)
            w.set(c, 0, ROW0 + 3, L.POLISHED_SLAB, {"type": "top"})
        w.set(8, 0, ROW0 + 4, "minecraft:candle", {"candles": "4", "lit": "true"})
        w.set(9, 0, ROW0 + 4, "minecraft:candle", {"candles": "3", "lit": "true"})
        for c in (8, 9):
            for y in range(ROW0 + 5, ROW0 + 8):
                w.set(c, 0, y, "minecraft:%s_stained_glass" % ("purple" if variant == "a" else "red"))
                w.set(c, -1, y, "minecraft:ochre_froglight", {"axis": "y"})
        for c in (7, 10):
            w.set(c, 0, ROW0 + 7, "minecraft:%s_wall_banner" % ("red" if variant == "a" else "black"), {"facing": w.into})
    if variant == "b":
        kits = ["iron", "netherite", "golden", "diamond", "chainmail", "iron"]
        for k, (za, zb) in enumerate(GALLERY_PIERS):
            K.statue(mt, 7, ROW0, za, E, kit=kits[2 * k])
            K.statue(mt, 10, ROW0, zb, W, kit=kits[2 * k + 1])
            mt.set(3, ROW0, za, L.CHISELED)
            K.lamp_post(mt, 3, za, y0=ROW0 + 1, height=3, soul=True)
            mt.set(14, ROW0, zb, L.CHISELED)
            K.lamp_post(mt, 14, zb, y0=ROW0 + 1, height=3, soul=True)
    marker_standing(mt, 8, 17, "patrol")
    marker_standing(mt, 3, 20, "spawn")
    marker_standing(mt, 14, 15, "spawn")
    if variant == "b":
        marker_standing(mt, 9, 13, "faction", faction="construct")   # the hall of statues: animated armour
    mt.finish()
    return mt


# =====================================================================================================================
# link pieces: straight tunnel segments between two sectors (9x9, sockets N and S with the pool nusmp:mega/link)
# =====================================================================================================================
def link_straight(variant):
    """A plain 5-wide corridor module: a coved rib and two lanterns in the middle, a lit niche in each side wall, a runner. `a` is stone, `b` timber.
    Why a module of its own: when the next sector's region would collide with an old one, CORE chains these between the two (a waystation every eighth),
    so a tunnel of any length is built from pieces that line up on the grid and cost no new rules."""
    name = "mega/link/straight_%s" % variant
    style = Style(variant, 0.10 if variant == "a" else 0.14, rib="stone" if variant == "a" else "timber", floor="runner" if variant == "a" else "checker",
                  lights="lantern", carpet=None if variant == "a" else "gray", skirt="stone" if variant == "a" else "wood")
    mt = MT(name, 1, 1)
    rng = seeded(mt)
    mt.room(6, wear=style.wear)
    mt.socket("n", 4, pool=L.P_LINK)
    mt.socket("s", 4, pool=L.P_LINK)
    K.floor_pattern(mt, style, 2, 6, rng)
    top = ROW0 + 5
    K.cove(mt, 2, 6, {4})
    K.rib(mt, 4, style)
    for side in ("w", "e"):
        K.pilaster(mt, side, 2, style)
        K.pilaster(mt, side, 6, style)
        K.skirting(mt, side, 2, 6, set(), style.skirt)
        K.niche(Wall(mt, side), (3, 4, 5), "lantern" if variant == "a" else "candles", rng, style)
    hang_lantern(mt, 4, top + 1, 3, drop=2)               # the rib is only at z = 4: these two hang from the ceiling row
    hang_lantern(mt, 4, top + 1, 5, drop=2)
    marker_standing(mt, 4, 4, "patrol")
    mt.finish()
    return mt


def link_waystation():
    """A rest stop on a long tunnel: a bench along the west wall, a fire basket in a tall niche in the east wall, lanterns, a patrol node."""
    name = "mega/link/waystation"
    style = Style("w", 0.10, rib="timber", floor="checker", lights="mixed", skirt=None)
    mt = MT(name, 1, 1)
    rng = seeded(mt)
    mt.room(6, wear=style.wear)
    mt.socket("n", 4, pool=L.P_LINK)
    mt.socket("s", 4, pool=L.P_LINK)
    K.floor_pattern(mt, style, 2, 6, rng)
    top = ROW0 + 5
    K.cove(mt, 2, 6, {4})
    K.rib(mt, 4, style)
    for side in ("w", "e"):
        K.pilaster(mt, side, 2, style)
        K.pilaster(mt, side, 6, style)
    K.bench_row(mt, Wall(mt, "w"), (3, 4, 5), depth=1, wood=True)
    K.tall_niche(Wall(mt, "e"), (3, 4, 5), "brazier", rng)
    hang_lantern(mt, 4, top + 1, 3, drop=2, soul=True)
    hang_lantern(mt, 4, top + 1, 5, drop=2)
    marker_standing(mt, 5, 4, "patrol")
    mt.finish()
    return mt


# =====================================================================================================================
# registry and command line
# =====================================================================================================================
def build_all(only=None):
    """Every piece of this builder as a finished builder object (`MT` or `CapT`), deterministic; `only` keeps the pieces whose name contains one of them."""
    out = []

    def add(piece):
        if not only or any(o in piece.name for o in only):
            out.append(piece)

    def want(fragment):
        return not only or any(o in fragment or fragment in o for o in only)

    for modules in (2, 3, 4):
        for v in "abc":
            if want("straight_1x%d_%s" % (modules, v)):
                add(straight_corridor(modules, v))
    for kind in ("bend", "tee", "cross"):
        for v in "abc":
            if want("%s_1x1_%s" % (kind, v)):
                add(chamber(kind, v))
    for v in "ab":
        if want("gallery_2x4_%s" % v):
            add(gallery(v))
    for modules in (2, 3):
        for v in "abc":
            if want("plaza_%dx%d_%s" % (modules, modules, v)):
                add(R.plaza(modules, v))
    if want("crossroads_hall"):
        add(R.crossroads_hall())
    for v in "ab":
        if want("link/straight_%s" % v):
            add(link_straight(v))
    if want("waystation"):
        add(link_waystation())
    for c in C.build_caps():
        add(c)
    return out


def main(argv):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("only", nargs="*", help="only pieces whose name contains one of these")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--light", action="store_true")
    ap.add_argument("--root", default=L.RES, help="resources root to write into (default: the repository's src/main/resources)")
    a = ap.parse_args(argv)
    pieces = build_all(a.only)
    defects = [line for p in pieces for line in CK.problems(p.t, p.name)]
    if defects:                                           # a piece that would float a chain or hang a lantern on nothing is never written
        print("\n".join(defects))
        print("%d defect(s): nothing written" % len(defects))
        return 1
    if a.list:
        for p in pieces:
            sx, sy, sz = p.t.size
            print("%-40s %3dx%2dx%3d  %2d jigsaws  %5d blocks  %d entities" % (p.name, sx, sy, sz, len(p.t.jigsaws()), len(p.t.cells), len(p.t.entities)))
        return 0
    if a.light:
        import mega_shape_preview as SP
        bad = 0
        for p in pieces:
            if p.name.startswith("mega/cap/"):
                continue
            r = SP.light_report(p.t, p.name)
            line = SP.format_report(p.name, r)
            bs, bm = SP.connectivity(p.t)
            if r["min"] < 5 or bs or bm:
                bad += 1
                line += "   <-- " + ("dark; " if r["min"] < 5 else "") + ("sockets not walkable %s; " % bs if bs else "") + ("markers not walkable %s" % bm if bm else "")
            print(line)
        print("%d piece(s) below the light or walkability bar" % bad)
        return 1 if bad else 0
    if a.check:
        import nbt
        stale = 0
        for p in pieces:
            path = p.t.path(a.root)
            have = nbt.dumps(nbt.loads(open(path, "rb").read())[1]) if os.path.exists(path) else None
            if have != nbt.dumps(p.t.to_nbt()):
                stale += 1
                print("STALE %s" % p.name)
        print("%d piece(s), %d stale" % (len(pieces), stale))
        return 1 if stale else 0
    for p in pieces:
        path = p.save(a.root)
        print("wrote %s  %dx%dx%d  %d blocks" % (os.path.relpath(path, a.root), p.t.size[0], p.t.size[1], p.t.size[2], len(p.t.cells)))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

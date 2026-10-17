#!/usr/bin/env python3
"""
The kit of the THEME-C builder (1.59.0): the four evil-road themes (Cursed Archive, Necropolis, Vampire Castle, Haunted Manor) and their signature rooms.

Why a kit of its own: the furnishing kit of ROOMS-1 (`mega_decor_r1.py`) already refuses decor in doorways, walks the finished room and measures the light, and
this module sits on top of it (it is never edited here) with the three things only these themes need: (1) the props of a library, a graveyard, a gothic
castle and a haunted house that no earlier room has (shelf towers with tunnels, hanging gantries, chained tomes, coffins, mausoleum fronts, rocking horses,
cribs, picture frames ...); (2) the common command line of the four generators (`build`, `--check`, `--list`, `--force`, the same contract as
`gen_mega_rooms1.py`) so the four files only hold their rooms; (3) the extra checks that the evil road's rooms owe the game (a room must pass the light check
with the theme's own lights applied, nothing flammable may touch lava, ...).

Everything here writes vanilla blocks only, architecture only in the palette constants of `mega_lib` (the Region Theme restyles them), and goes through
`Hall.set` / `Wall.set` / `Fr.set`, so a prop can never overwrite the shell or another prop, or stand in a declared route.
"""
import argparse
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D  # noqa: E402
from mega_decor_r1 import GROUND, FLOOR, Hall, DV, RIGHT, LEFT, OPP  # noqa: E402,F401

SKULL_ROT = {"south": 0, "west": 4, "north": 8, "east": 12}

# The furnishing kit's second table of block states does not know the blocks of a redstone trap. They are real vanilla blocks with these properties (checked against
# `vanilla_block_states_1_21_1.json` by `mega_lib.check_state` on every `set`), so the kit's table is extended here, in this module, instead of being edited.
D._STRICT[0:0] = [
    (re.compile(r"dispenser|dropper"), {"facing": D._H6S, "triggered": D._BOOL}),
    (re.compile(r"tripwire"), {"attached": D._BOOL, "disarmed": D._BOOL, "east": D._BOOL, "north": D._BOOL, "powered": D._BOOL, "south": D._BOOL, "west": D._BOOL}),
    (re.compile(r"piston|sticky_piston"), {"extended": D._BOOL, "facing": D._H6S}),
    # the mod's own mechanical blocks (mega_lib.CUSTOM): their properties are read from their Java classes by the builders that made them
    (re.compile(r"gatekeeper_door"), {"kind": None, "half": ("upper", "lower"), "open": D._BOOL}),
    (re.compile(r"sigil_plate"), {"color": None, "lit": D._BOOL}),
    (re.compile(r"pitfall_tile"), {"safe": D._BOOL, "open": D._BOOL}),
    (re.compile(r"bell"), {"attachment": ("floor", "ceiling", "single_wall", "double_wall"), "facing": D._H4S, "powered": D._BOOL}),
    (re.compile(r"snuffer_glyph"), {"active": D._BOOL}),
    (re.compile(r"riddle_stone|sealed_archway|ward_rune|spawn_glyph|magic_barrier"), {}),
]


# The furnishing kit does not know the mod's trial floor blocks: to it a ward rune or a spawn glyph is an obstacle that lets the light and the eye through, so a
# doorway with a rune in its floor "is not three wide" and the shell "leaks" under every glyph. They are plain full cubes you walk on (see WardRuneBlock and
# SpawnGlyphBlock: "a plain, ordinary block on purpose"), so the kit's two tables are told so here, in this module, instead of being edited. (The pit tiles and
# the sigil plates stay obstacles to the kit: a spawn marker must never be put on a trap.)
_FLOOR_BLOCKS = ("nusmp:ward_rune", "nusmp:spawn_glyph")
_classify_original = D.classify


def _classify(block, props):
    if block in _FLOOR_BLOCKS:
        return ("floor", 2)
    return _classify_original(block, props)


D.classify = _classify
D._OPAQUE_EXTRA = re.compile(D._OPAQUE_EXTRA.pattern + "|ward_rune|spawn_glyph")


# =============================================================================================================================== the command line
class Registry:
    """The rooms of one generator file: `@reg.room("cursed_archive_stacks", "hall")` registers the builder under its file name and folder."""

    def __init__(self, doc):
        self.rooms = {}
        self.folder = {}
        self.extra = {}
        self.doc = doc

    def check(self, name):
        """Registers a room-specific check `fn(hall) -> [problem, ...]` that `main` runs after the shared ones (the puzzle rooms prove their closet is locked)."""
        def deco(fn):
            self.extra[name] = fn
            return fn
        return deco

    def room(self, name, folder):
        def deco(fn):
            self.rooms[name] = fn
            self.folder[name] = folder
            return fn
        return deco

    def path(self, name):
        return os.path.join(L.RES, "data", "nusmp", "structure", "mega", self.folder[name], name + ".nbt")


def _same(h, path):
    import nbt as _nbt
    mine = _nbt.dumps(h.mt.t.to_nbt(), compress=True)
    if not os.path.exists(path):
        return False
    with open(path, "rb") as f:
        return f.read() == mine


def main(reg, argv, calm=()):
    """The command line of a generator. `calm` names the rooms that are safe zones (no spawn marker; the check accepts that)."""
    ap = argparse.ArgumentParser(description=(reg.doc or "").split("\n\n")[0])
    ap.add_argument("names", nargs="*", help="templates to build (default: all)")
    ap.add_argument("--check", action="store_true", help="do not write; compare with the files on disk and run every check")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--force", action="store_true", help="write a template even when a check fails (to look at it); the exit status stays 1")
    a = ap.parse_args(argv)
    if a.list:
        for n in reg.rooms:
            print("%s/%s" % (reg.folder[n], n))
        return 0
    bad = 0
    for n in a.names or list(reg.rooms):
        if n not in reg.rooms:
            print("unknown template", n)
            return 2
        h = reg.rooms[n]()
        h.finish()
        problems, stats = h.check()
        if n in calm:
            problems = [p for p in problems if p != "no spawn marker"]
        problems += lava_fire_problems(h)
        if n in reg.extra:
            problems += reg.extra[n](h)
        if problems:
            bad += 1
        if a.check:
            if not _same(h, reg.path(n)):
                print("STALE %s (the file on disk differs from the generator)" % n)
                bad += 1
        elif not problems or a.force:
            h.save()
        else:
            print("NOT WRITTEN %s: a template with a defect never reaches the data folder (--force writes it anyway)" % n)
        if not a.quiet or problems:
            print("%s %-34s %5d blocks %2d doors %2d stands  light min %s median %s dark %s%%  markers %s" % (
                "ok  " if not problems else "FAIL", n, stats.get("blocks", 0), len(h.doors), stats.get("armor_stands", 0), stats.get("eye_light_min"),
                stats.get("eye_light_median"), round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
            for p in problems:
                print("     - " + p)
    return 1 if bad else 0


WOODS = ("oak", "spruce", "birch", "jungle", "acacia", "mangrove", "cherry", "bamboo")        # the nether woods (crimson, warped) do not burn
BURNS = ("_planks", "_log", "_wood", "_wool", "_carpet", "_fence", "_fence_gate", "_trapdoor", "_door", "_sign", "_hanging_sign", "_banner", "_wall_banner")
BURNS_PLAIN = ("bookshelf", "chiseled_bookshelf", "hay_block", "vine", "cobweb", "lectern", "barrel", "composter", "scaffolding", "crafting_table", "loom",
               "note_block", "jukebox", "chest", "trapped_chest", "bamboo", "moss_carpet", "hanging_roots", "dried_kelp_block", "cartography_table", "fletching_table",
               "smithing_table")


def burns(block):
    """True for a block that lava or fire can set alight (wood, wool, carpet, books, vines ...), a deliberate overestimate: stairs and slabs count only in a
    burning wood."""
    p = D.path_of(block)
    if p in BURNS_PLAIN:
        return True
    if p.endswith(("_stairs", "_slab", "_button", "_pressure_plate")):
        return p.split("_")[0] in WOODS
    if p.endswith(BURNS):
        return not p.startswith(("crimson_", "warped_", "stripped_crimson", "stripped_warped"))
    return False


def lava_fire_problems(h):
    """Nothing flammable within three blocks of lava (lava sets wood, wool and carpet alight, and a burning sector is the one failure nobody can undo): the
    same rule `gen_mega_rooms2` proves for its lava halls, applied to every room of this builder that holds lava at all."""
    cells = h.mt.t.cells
    lava = [p for p, c in cells.items() if c.block == "minecraft:lava"]
    out = []
    for (x, y, z) in lava:
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                for dz in range(-3, 4):
                    c = cells.get((x + dx, y + dy, z + dz))
                    if c is not None and burns(c.block):
                        out.append("flammable %s at %s within three blocks of lava at %s" % (c.block, (x + dx, y + dy, z + dz), (x, y, z)))
                        if len(out) > 5:
                            return out
    return out


# =============================================================================================================================== architecture
def hollow_flight(h, x, z, facing, steps, width, base, rail_r=None, gaps=(), rib_every=6, rib_phase=5, stairs=L.PLANKS_STAIRS, soffit=2):
    """A long flight of stairs that is not a solid wedge: every step is a stair over a `soffit`-thick underside of masonry, and every `rib_every`-th step
    (counting from `rib_phase`) is a solid rib down to the floor, so under the flight there is a row of bays between the ribs, open on every flank that no wall
    closes. The flight starts at (x, z), climbs `steps` cells towards `facing` and is `width` columns wide, counted from the column at (x, z) to the right of the
    climb. A fence rail runs along column `rail_r` (None: no rail) except where the coordinate along the climb lies in one of the ranges `gaps` (a bridge leaves the
    flight there). Returns the bays as a list of (first step, last step) pairs in the order of the climb.
    Why: a solid wedge twenty-five blocks high is a brown mountain in the middle of a library; ribs and a soffit are what a stone staircase looks like from below,
    and the bays under it become reading alcoves (the generator lights and furnishes them)."""
    fx, fz = DV[facing]
    rx, rz = DV[RIGHT[facing]]
    ribs = [k for k in range(steps) if k % rib_every == rib_phase]
    for k in range(steps):
        b = base + k
        rib = k in ribs
        for r in range(width):
            cx, cz = x + k * fx + r * rx, z + k * fz + r * rz
            h.set(cx, b, cz, stairs, {"facing": facing, "half": "bottom"})
            lo = base if rib else max(base, b - soffit)
            for yy in range(lo, b):
                h.set(cx, yy, cz, D.brick_mix(h, 0.15, 0.1))
            if rail_r is not None and r == rail_r and k < steps - 1 and not any(a <= (cz if fx == 0 and fz != 0 else cx) <= c for (a, c) in gaps):
                h.try_set(cx, b + 1, cz, L.FENCE)
    bays = []
    start = 0
    for k in ribs + [steps]:
        if k - 1 >= start:
            bays.append((start, k - 1))
        start = k + 1
    return bays


def hung_lantern_under(h, x, z, y_floor_above, y_lamp, soul=True):
    """A lantern on a chain hung from the underside of whatever solid block is first met above the cell (x, y_lamp, z); returns True when it was hung. Why: the
    bays under a flight have a ceiling that is the soffit of the stair, not the roof of the hall, so `D.auto_light` (which hangs from the roof) cannot light them."""
    top = None
    for yy in range(y_lamp + 1, h.top + 1):
        c = h.get(x, yy, z)
        if c is not None:
            if D.classify(c.block, c.props)[0] != "pass" and c.block != "minecraft:chain":
                top = yy
            break
        if not h.can_set(x, yy, z, "minecraft:chain"):
            return False
    if top is None or top - y_lamp < 1:
        return False
    for yy in range(y_lamp + 1, top):
        h.set(x, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_lamp, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})
    return True


# =============================================================================================================================== light
def auto_light(h, soul=True, threshold=5, max_lamps=60, radius=2, min_clear=3, prune=False):
    """`mega_decor_r1.auto_light`, with two more rules. (1) A lantern (and the chain above it) is never hung in a column that holds a standable surface at or above
    its own row minus the head room, so no lamp ever lands in the head room of a stair, a gantry or a landing. (2) With `prune`, a lamp that did not make its dark cell
    any lighter is taken out again and that cell is given up (off by default, so the rooms written before it existed come out byte for byte as they did). Why: in a thirty-block room the dark spot is often under a flight or a gallery, and the kit's version happily hung
    a lantern in the air above the first steps, which the walk graph then (rightly) refused to climb; and a dark cell under a deck can never be lit from the roof,
    so the original kept hanging lamps round it until its budget was gone, a forest of chains over the deck."""
    t = h.mt.t
    starts = D._door_starts(h)
    nodes = D.walk_nodes(t)
    seen = D.reach(t, nodes, starts)

    def tops():
        out = {}
        for (x, z), ss in nodes.items():
            for s_ in ss:
                if s_ <= 2 * h.top:
                    out[(x, z)] = max(out.get((x, z), 0), (s_ + 1) // 2)
        return out
    top_node = tops()
    blocked = set()
    n = 0
    last = None
    for _ in range(max_lamps * 2 if prune else max_lamps):
        lv = D.light_levels(t)
        if last is not None:
            (lx, lz, ls, lbefore, placed) = last
            if prune and lv.get((lx, int(ls / 2.0 + 1.62), lz), 0) <= lbefore:       # the lamp did not help: take it out again and stop trying for this cell
                for pos in placed:
                    h.mt.remove(*pos)
                    h.deco.pop(pos, None)
                n -= 1
                blocked.add((lx, lz))
                lv = D.light_levels(t)
            last = None
        if n >= max_lamps:
            break
        worst = None
        for (x, z, s_) in seen:
            if (x, z) in blocked:
                continue
            ye = int(s_ / 2.0 + 1.62)
            v = lv.get((x, ye, z), 0)
            if v < threshold and (worst is None or v < worst[0]):
                worst = (v, x, z, s_)
        if worst is None:
            break
        v, x, z, s_ = worst
        feet = s_ // 2 if s_ % 2 == 0 else (s_ - 1) // 2 + 1
        row_min = feet + min_clear
        done = None
        for rad in (radius, radius + 2, radius + 4):
            cands = sorted(((dx * dx + dz * dz, x + dx, z + dz) for dx in range(-rad, rad + 1) for dz in range(-rad, rad + 1)))
            for (_, cx, cz) in cands:
                if not h.inside(cx, cz):
                    continue
                floor_of_col = top_node.get((cx, cz), D.GROUND)
                for y_top in (h.top, h.top - 1):
                    above = h.get(cx, y_top + 1, cz)
                    if above is None or D.classify(above.block, above.props)[0] == "pass":
                        continue
                    for yl in range(max(row_min, floor_of_col + 3), y_top):
                        if all(h.can_set(cx, yy, cz, "minecraft:chain") for yy in range(yl, y_top + 1)):
                            placed = []
                            for yy in range(yl + 1, y_top + 1):
                                h.set(cx, yy, cz, L.CHAIN_BLOCK, {"axis": "y"})
                                placed.append((cx, yy, cz))
                            h.set(cx, yl, cz, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})
                            placed.append((cx, yl, cz))
                            done = placed
                            break
                    if done:
                        break
                if done:
                    break
            if done:
                break
        if done:
            n += 1
            last = (x, z, s_, v, done)
            nodes = D.walk_nodes(t)
            top_node = tops()
        else:
            blocked.add((x, z))
    return n


# =============================================================================================================================== props of the gothic and haunted rooms
def gothic_window(w, lat, y0, y1, glass, width=3, glow="minecraft:shroomlight", pointed=True):
    """A tall window of `width` columns (1 or 3) centred on lateral `lat`, rows y0..y1: stained glass in the inner wall layer, a glowing block behind it in the outer
    layer (none when `glow` is None: a dark louvre of iron bars, the wall behind it stays), and (pointed) side columns that stop two rows lower for every column
    from the middle, so the head is a point. `glass` is one block id or a tuple that is cycled over the cells. Returns False without touching anything when a cell is not plain wall (a pilaster, a door frame, an earlier window) or the cell in
    front of it is taken. Why: the brief's gothic halls live by their windows, and a window that is lit from behind is also a lamp the light check can count."""
    h = w.hall
    half = width // 2
    cells = []
    for dl in range(-half, half + 1):
        top = y1 - (2 * abs(dl) if pointed else 0)
        for y in range(y0, top + 1):
            cells.append((lat + dl, y))
    for (lt, y) in cells:
        inner, outer = w.cell(lt, y, -1), w.cell(lt, y, -2)
        ci, co = h.get(*inner), h.get(*outer)
        if ci is None or co is None or inner not in h.shell or outer not in h.shell or inner in h.mt.reserved or outer in h.mt.reserved:
            return False
        if ci.block not in D.WALL_BLOCKS or not w.free(lt, y, 0):
            return False
    for (lt, y) in cells:
        w.carve(lt, y, -1)
        w.set(lt, y, -1, glass[(lt + y) % len(glass)] if isinstance(glass, (tuple, list)) else glass)
        if glow is not None:
            w.shell_set(lt, y, -2, glow)
    return True


def pointed_profile(span, rise_max, ratio=1.0):
    """The height of a pointed arch over each of `span` cells (the first and the last next to its piers): two circular arcs of radius `ratio * span` whose centres
    are the opposite springing points, cut to at most `rise_max`. Why: a pointed arch is the one line the eye reads as gothic, and a table of heights is all a voxel
    arch needs."""
    import math
    r = max(1.0, ratio * span)
    ys = []
    for t in range(span):
        u = t + 0.5
        u = min(u, span - u)                         # the left half; the right half is its mirror
        ys.append(math.sqrt(max(0.0, r * r - (span - u) ** 2)))
    top = max(ys) or 1.0
    scale = min(1.0, rise_max / top)
    return [int(round(v * scale)) for v in ys]


def arch_rib(h, axis, fixed, a, b, y_spring, rise_max, block=L.POLISHED, keystone=L.CHISELED, ratio=1.0):
    """A pointed rib of single blocks across the cells `a..b` (inclusive) of the line `axis` ('x': the rib lies in the plane z = `fixed`; 'z': in x = `fixed`),
    springing from row `y_spring` at both ends. Vertical runs fill the steep flanks so the rib is one face-connected line; the cells at the crown are `keystone`.
    Returns the list of cells placed. Why: ribs under a flat ceiling turn a box into a vault, and every cell is joined to its neighbour and to the piers."""
    span = b - a + 1
    ys = pointed_profile(span, rise_max, ratio)
    crown = max(ys)
    placed = []
    for t in range(span):
        prev = ys[t - 1] if t > 0 else 0
        nxt = ys[t + 1] if t + 1 < span else 0
        lo = min(ys[t], prev if t <= span // 2 else nxt)
        hi = max(ys[t], prev if t <= span // 2 else nxt)
        for dy in range(lo, hi + 1):
            pos = (a + t, y_spring + dy, fixed) if axis == "x" else (fixed, y_spring + dy, a + t)
            if h.try_set(pos[0], pos[1], pos[2], keystone if ys[t] == crown and dy == crown else block):
                placed.append(pos)
    return placed


def coffin(h, x, z, axis="z", open_lid=False, head_first=True, candle="red", y=GROUND):
    """A coffin 2 wide and 3 long standing on the row below `y`: a box of the wood family, raised head and foot ends (full blocks, so the candles stand flush) and a
    lid of slabs between them; with `open_lid` the lid is gone and a red carpet lines the box, a skull on the head end. `axis` is the long direction ('z' or 'x'),
    (x, z) the minimum corner. Why: the brief's coffin crypts from vanilla blocks; the same shape serves the Vampire Castle (open and empty) and the Haunted Manor's cellar."""
    cells = [(x + (k if axis == "x" else r), z + (r if axis == "x" else k), k) for k in range(3) for r in range(2)]
    for (cx, cz, k) in cells:
        h.try_set(cx, y, cz, L.PLANKS)
        if k in (0, 2):
            h.try_set(cx, y + 1, cz, L.PLANKS)
        elif open_lid:
            h.try_set(cx, y + 1, cz, "minecraft:red_carpet")
        else:
            h.try_set(cx, y + 1, cz, L.PLANKS_SLAB, {"type": "bottom"})
    head_k = 0 if head_first else 2
    head = [(cx, cz) for (cx, cz, k) in cells if k == head_k]
    foot = [(cx, cz) for (cx, cz, k) in cells if k == 2 - head_k]
    cx, cz = head[0]
    if open_lid:
        h.try_set(cx, y + 2, cz, "minecraft:skeleton_skull", {"rotation": str(SKULL_ROT["south"] if axis == "z" else SKULL_ROT["east"])})
    elif h.free(cx, y + 2, cz):
        D.candles_at(h, cx, y + 2, cz, 2, candle)
    if h.free(foot[-1][0], y + 2, foot[-1][1]):
        D.candles_at(h, foot[-1][0], y + 2, foot[-1][1], 1, candle)


def mirror(w, lat, y0, y1, width=3, glass="minecraft:tinted_glass", frame=L.CHISELED, side=L.POLISHED):
    """A mirror that reflects nothing: a panel of dark tinted glass `width` wide in the inner wall layer from y0 to y1, framed by a course of `frame` blocks at its
    top and bottom and `side` blocks at its sides, a candle on the sill. Returns False and does nothing when the wall there is not plain. Why: the brief's mirrors that
    reflect nothing; tinted glass is the one vanilla glass that is black, and a gilded frame turns a dark pane into a mirror."""
    h = w.hall
    half = width // 2
    cells = [(lt, y) for lt in range(lat - half - 1, lat - half + width + 1) for y in range(y0 - 1, y1 + 2)]
    for (lt, y) in cells:
        inner = w.cell(lt, y, -1)
        c = h.get(*inner)
        if c is None or inner not in h.shell or inner in h.mt.reserved or c.block not in D.WALL_BLOCKS:
            return False
        if y in range(y0, y1 + 1) and lt in range(lat - half, lat - half + width) and (not w.free(lt, y, 0) or h.get(*w.cell(lt, y, -2)) is None):
            return False
    for (lt, y) in cells:
        inside = lt in range(lat - half, lat - half + width) and y in range(y0, y1 + 1)
        if inside:
            w.carve(lt, y, -1)
            w.set(lt, y, -1, glass)
        else:
            w.shell_set(lt, y, -1, frame if (y in (y0 - 1, y1 + 1) and lt in (lat - half - 1, lat - half + width)) else side if lt in (lat - half - 1, lat - half + width) else frame)
    return True


def pew(h, x, z, length, axis="x", facing="north", arms=False):
    """A pew: a bench of `length` cells of stairs (the back of every stair towards the aisle's far side), with a fence post at each end when `arms` is set. `facing`
    is the way the sitters look. Why: a chapel is rows of pews, and a chair of stairs is the vanilla bench the checks already understand; the arms are off by
    default because a post in every row of a pew block turns the block into a fence for the 3-wide walk check."""
    back = {"north": "south", "south": "north", "east": "west", "west": "east"}[facing]
    for k in range(length):
        cx, cz = (x + k, z) if axis == "x" else (x, z + k)
        h.try_set(cx, GROUND, cz, L.PLANKS_STAIRS, {"facing": back, "half": "bottom"})
    if arms:
        for k in (0, length - 1):
            cx, cz = (x + k, z) if axis == "x" else (x, z + k)
            h.try_set(cx, GROUND + 1, cz, L.FENCE)


# =============================================================================================================================== the mechanics of a trial hall
def trial_core(h, doors, glyphs, mobs, reset=60):
    """What every trial hall of this builder has, the way `gen_mega_rooms2.trial_core` writes it (same blocks, same box, same chain): ward runes in the floor of the
    inner wall layer of every doorway (three per door), a spawn glyph in the floor at every cell of `glyphs`, and the Sealed Archway keystone hanging over the middle
    of the 27 x 27 hall on a chain from the ceiling, its watched box the whole footprint from the floor to the ceiling and its roster `mobs` ((entity id, count) pairs).
    Why here: the Blood Court and the Ballroom are trial halls too, and the two of them write the same blocks as the Spire's own trial rooms so that the same
    block entity, the same barriers and the same rules apply, with a different stage and a different roster."""
    import gen_mega_rooms2 as R2
    mt = h.mt
    for (side, off) in doors:
        w = h.wall(side)
        for lat in range(off - 1, off + 2):
            x, y, z = w.cell(lat, FLOOR, -1)
            mt.set(x, y, z, R2.WARD_RUNE)
    for (x, z) in glyphs:
        mt.set(x, FLOOR, z, R2.SPAWN_GLYPH)
    c = 13
    for y in range(FLOOR + 13, h.top + 1):
        mt.set(c, y, c, L.CHAIN_BLOCK, {"axis": "y"})
    mt.set(c, FLOOR + 12, c, R2.ARCHWAY, None, R2.archway_data(13, 13, mobs, reset))


def check_trial(h):
    """The trial halls' own check, `gen_mega_rooms2.check_trial` (the keystone's box covers the footprint, three ward runes per doorway, at least six glyphs on
    reachable floor, a reachable chest), as a list of problem strings."""
    import gen_mega_rooms2 as R2
    import mega_decor_r2 as D2
    return ["trial check: " + e for e in R2.check_trial(h.mt, D2.Analysis(h.mt), None)]

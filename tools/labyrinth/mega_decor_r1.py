#!/usr/bin/env python3
"""
The furnishing kit of the ROOMS-1 builder (1.54.0 Mega-Dungeon): the furnished halls (cell blocks, torture chambers, crypts, libraries ...) and the
treasure closets are written with it by `gen_mega_rooms1.py`.

Why a kit and not raw `MT.set` calls: a hall is 20 to 60 thousand cells of which a few thousand are furniture, and nothing here can be tried in the game.
So the kit does three things the bare toolkit (`mega_lib.py`) does not:
  1. It writes furniture ONCE, orientation-aware (`Fr`, `Wall`), as vanilla build hacks (tables from stairs and slabs, bunk beds, weapon racks
     of lightning rods, sarcophagi, hearths with a pot over a campfire ...), so the same prop is correct on any wall and in any rotation.
  2. It refuses mistakes while building (`Hall.set`): decor never overwrites the wall shell or other decor by accident, and never blocks a
     declared walking route (the apron in front of every doorway, the aisles). A hall that builds is a hall whose circulation holds by construction.
  3. It checks the finished template without a game (`check_hall`): the walk graph from every doorway (a half-block-accurate flood fill with
     the step-up and head-room rules of a player), the 3-wide-route test, block light (the same propagation rules as the game, to prove the
     Gloom clock of 1.36.0 is not triggered along the routes), see-through leaks in the shell (no void visible), floating decor (chains and
     lanterns that hang from nothing, wall torches without a wall), spawn markers that are cramped or unreachable, and the armor-stand budget.
Architecture uses only the palette constants of `mega_lib.py` (the Region Theme restyles them); furniture and decoration are free vanilla blocks.
"""
import collections
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

FLOOR = L.FLOOR_ROW          # 8: the floor row (its top face is the walking surface)
GROUND = L.FLOOR_ROW + 1     # 9: the row a player stands in
OPP = L.OPPOSITE
RIGHT = L.RIGHT_OF
LEFT = L.LEFT_OF
DIRS = ("north", "east", "south", "west")
DV = L.STEP                  # direction -> (dx, dz)


def path_of(block):
    return block.split(":", 1)[1] if ":" in block else block


def rot_dir(d, k):
    """`d` turned k quarter turns clockwise (seen from above)."""
    for _ in range(k % 4):
        d = RIGHT[d]
    return d


# ===================================================================================================================
# block classification (walk graph and light): the simplified rules of the game, written down once
# ===================================================================================================================
_PASS = re.compile(r"air|cave_air|void_air|light|structure_block|jigsaw|cobweb|.*_carpet|.*_wall_banner|.*_banner|.*_sign|.*_wall_sign|.*_hanging_sign|"
                   r".*_wall_hanging_sign|.*_button|.*_pressure_plate|(soul_|redstone_)?(wall_)?torch|lever|tripwire|tripwire_hook|vine|glow_lichen|sculk_vein|"
                   r"hanging_roots|crimson_roots|warped_roots|short_grass|tall_grass|fern|large_fern|weeping_vines.*|twisting_vines.*|cave_vines.*|"
                   r"spore_blossom|snow|ladder|moss_carpet|fire|soul_fire|dead_bush|.*_fungus")
PASS = ("pass", 0)
BLOCK = ("block", 0)
_cls_cache = {}


def classify(block, props):
    """('pass', 0): walk through; ('floor', t): stand on it, its top face is t half-blocks above its cell base (1 or 2); ('block', 0): an obstacle you can
    neither stand on nor walk through (a fence, a chain, a lantern, a chest ...). Deliberately conservative: a thin or low block counts as an obstacle."""
    key = (block, tuple(sorted(props.items())))
    r = _cls_cache.get(key)
    if r is not None:
        return r
    p = path_of(block)
    if _PASS.fullmatch(p):
        r = PASS
    elif p.endswith("_fence_gate"):
        r = PASS if props.get("open") == "true" else BLOCK
    elif p.endswith("_trapdoor"):
        if props.get("open") == "true":
            r = BLOCK
        elif props.get("half") == "top":
            r = ("floor", 2)
        else:
            r = PASS
    elif p.endswith("_stairs"):
        r = ("floor", 2 if props.get("half") == "top" else 1)
    elif p.endswith("_slab"):
        r = ("floor", 1 if props.get("type", "bottom") == "bottom" else 2)
    elif L.is_full(block) and not re.fullmatch(r"(.*_)?skull|.*_head", p):
        r = ("floor", 2)
    else:
        r = BLOCK
    _cls_cache[key] = r
    return r


_OPAQUE_NOT = re.compile(r"glass|.*_stained_glass|ice|slime_block|honey_block")


_OPAQUE_EXTRA = re.compile(r".*_froglight|shroomlight|sea_lantern|glowstone|redstone_lamp|honeycomb_block|copper_.*|waxed_.*copper.*|.*_wool|.*_concrete|.*terracotta")


def opaque(block):
    """True for a block that stops light and sight: a full cube other than glass and ice (mega_lib's table of full cubes lacks the froglights)."""
    p = path_of(block)
    if _OPAQUE_NOT.fullmatch(p):
        return False
    return L.is_full(block) or bool(_OPAQUE_EXTRA.fullmatch(p) and not re.fullmatch(r".*(stairs|slab|wall|bars|chain|rod|grate|bulb|door|trapdoor).*", p))


_EMIT = [
    (r"lantern", lambda pr: 15), (r"soul_lantern", lambda pr: 10),
    (r"(wall_)?torch", lambda pr: 14), (r"soul_(wall_)?torch", lambda pr: 10),
    (r"campfire", lambda pr: 15 if pr.get("lit", "true") == "true" else 0), (r"soul_campfire", lambda pr: 10 if pr.get("lit", "true") == "true" else 0),
    (r".*candle", lambda pr: 3 * int(pr.get("candles", "1")) if pr.get("lit") == "true" else 0),
    (r"lava|lava_cauldron|shroomlight|glowstone|sea_lantern|jack_o_lantern|ochre_froglight|verdant_froglight|pearlescent_froglight|fire|beacon|conduit", lambda pr: 15),
    (r"soul_fire", lambda pr: 10), (r"light", lambda pr: int(pr.get("level", "15"))),
    (r"(furnace|blast_furnace|smoker)", lambda pr: 0),      # lit at first, cold after ~27 minutes of loaded time: never counted as light
    (r"end_rod", lambda pr: 14), (r"magma_block", lambda pr: 3), (r"crying_obsidian", lambda pr: 10), (r"amethyst_cluster", lambda pr: 5),
    (r"large_amethyst_bud", lambda pr: 4), (r"medium_amethyst_bud", lambda pr: 2), (r"small_amethyst_bud", lambda pr: 1), (r"glow_lichen", lambda pr: 7),
    (r"redstone_lamp", lambda pr: 15 if pr.get("lit") == "true" else 0), (r"enchanting_table|ender_chest", lambda pr: 7), (r"brewing_stand", lambda pr: 1),
    (r"cave_vines|cave_vines_plant", lambda pr: 14 if pr.get("berries") == "true" else 0), (r"sculk_catalyst", lambda pr: 6),
]
_EMIT = [(re.compile(a), f) for a, f in _EMIT]


def emission(block, props):
    p = path_of(block)
    for pat, f in _EMIT:
        if pat.fullmatch(p):
            return f(props)
    return 0


def light_levels(t):
    """Block light of every non-opaque cell of template `t`, by the game's rule (a source gives its level, each step through a non-opaque cell costs 1).
    Cells outside the template are ignored (a hall is lit by its own lamps). Returns {(x, y, z): level}."""
    W, H, D = t.size
    opq = {}
    buckets = collections.defaultdict(list)
    lv = {}
    for pos, c in t.cells.items():
        e = emission(c.block, c.props)
        if e > 0:
            if lv.get(pos, 0) < e:
                lv[pos] = e
                buckets[e].append(pos)
        opq[pos] = opaque(c.block)
    for level in range(15, 1, -1):
        for pos in buckets.get(level, ()):
            if lv.get(pos, 0) != level:
                continue
            x, y, z = pos
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                n = (x + dx, y + dy, z + dz)
                if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                    continue
                if opq.get(n, False):
                    continue
                if lv.get(n, 0) < level - 1:
                    lv[n] = level - 1
                    buckets[level - 1].append(n)
    return {p: v for p, v in lv.items() if not opq.get(p, False)}


# ===================================================================================================================
# the walk graph
# ===================================================================================================================
def _passable_fn(t):
    W, H, D = t.size
    cells = t.cells

    def passable(x, y, z):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < D):
            return False
        c = cells.get((x, y, z))
        if c is None:
            return True
        return classify(c.block, c.props)[0] == "pass"

    def surface(x, y, z):
        """(is_floor, top) of the cell as a thing to stand on."""
        if not (0 <= x < W and 0 <= y < H and 0 <= z < D):
            return None
        c = cells.get((x, y, z))
        if c is None:
            return None
        k, top = classify(c.block, c.props)
        return top if k == "floor" else None

    return passable, surface


def walk_nodes(t):
    """{(x, z): [s, ...]}: every standable surface of every column; s is the height of the standing plane in half-blocks above y=0 (a full floor block at
    row 8 gives s = 18). A node needs two cells of head room above its plane."""
    W, H, D = t.size
    passable, surface = _passable_fn(t)
    nodes = collections.defaultdict(list)
    for z in range(D):
        for x in range(W):
            for y in range(0, H - 2):
                top = surface(x, y, z)
                if top is None:
                    continue
                s = 2 * y + top
                cb = s // 2
                need = (cb, cb + 1) if s % 2 == 0 else (cb + 1, cb + 2)
                if all(passable(x, yy, z) for yy in need):
                    nodes[(x, z)].append(s)
    return nodes


def _neighbors(nodes, passable, x, z, s):
    for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        for s2 in nodes.get((x + dx, z + dz), ()):
            d = s2 - s
            if abs(d) > 2:
                continue
            if d == 2:   # a one-block step up is a jump: the cell above the head must be free as well
                cb = s // 2
                extra = cb + 2 if s % 2 == 0 else cb + 3
                if not passable(x, extra, z):
                    continue
            yield (x + dx, z + dz, s2)


def socket_starts(t):
    """Start nodes: the floor cell just inside the room (two cells in from the outer face) of every door socket (jigsaw)."""
    out = []
    for (x, y, z), o, d in t.jigsaws():
        face = o.split("_")[0]
        dx, dz = DV[OPP[face]]
        out.append((x + 2 * dx, z + 2 * dz, 2 * (y - 1) + 2))
    return out


def reach(t, nodes=None, starts=None, wide=False):
    """Set of nodes (x, z, s) reachable from the doorways (all doorways, or the given starts). With wide=True only nodes whose 3x3 neighbourhood is
    walkable at the same level are used: a route that is at least 3 wide."""
    nodes = nodes if nodes is not None else walk_nodes(t)
    passable, _ = _passable_fn(t)
    starts = starts if starts is not None else socket_starts(t)

    def is_wide(x, z, s):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                if not any(abs(s2 - s) <= 2 for s2 in nodes.get((x + dx, z + dz), ())):
                    return False
        return True

    seen = set()
    dq = collections.deque()
    for (x, z, s) in starts:
        if s in nodes.get((x, z), ()) and (not wide or is_wide(x, z, s)):
            seen.add((x, z, s))
            dq.append((x, z, s))
    while dq:
        x, z, s = dq.popleft()
        for n in _neighbors(nodes, passable, x, z, s):
            if n in seen:
                continue
            if wide and not is_wide(*n):
                continue
            seen.add(n)
            dq.append(n)
    return seen


def reachable_columns(t):
    return {(x, z) for (x, z, s) in reach(t)}


# ===================================================================================================================
# checks of a finished template
# ===================================================================================================================
def markers_of(t):
    out = []
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            meta = str(c.nbt.get("metadata", ""))
            parts = meta.split(";")
            args = {}
            for a in parts[1:]:
                if "=" in a:
                    k, v = a.split("=", 1)
                    args[k] = v
            out.append((parts[0][6:], x, y, z, args))
    return out


def door_holes(t):
    """The cells of every doorway hole (3 wide, 4 tall, through both wall layers) of template `t`, from its jigsaws."""
    hole = set()
    for (x, y, z), o, d in t.jigsaws():
        face = o.split("_")[0]
        for depth in range(L.WALL):
            for lat in range(-1, 2):
                for yy in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                    if face in ("north", "south"):
                        hole.add((x + lat, yy, z + (depth if face == "north" else -depth)))
                    else:
                        hole.add((x + (depth if face == "west" else -depth), yy, z + lat))
    return hole


def leaks(t):
    """See-through gaps in the shell: flood fill from the room interior through every NON-OPAQUE cell (air, bars, glass, banners ...); any boundary cell of the
    template reached, other than the doorway holes, is void visible from inside. Returns a list of boundary cells."""
    W, H, D = t.size
    cells = t.cells
    hole = door_holes(t)
    # start: any air cell at row 9 well inside
    start = None
    for z in range(3, D - 3):
        for x in range(3, W - 3):
            if cells.get((x, GROUND, z)) is None:
                start = (x, GROUND, z)
                break
        if start:
            break
    if start is None:
        return []
    seen = {start}
    dq = collections.deque([start])
    bad = []
    while dq:
        x, y, z = dq.popleft()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in seen:
                continue
            if not (0 <= n[0] < W and 0 <= n[1] < H and 0 <= n[2] < D):
                continue
            c = cells.get(n)
            if c is not None and opaque(c.block):
                continue
            seen.add(n)
            if (n[0] in (0, W - 1) or n[2] in (0, D - 1) or n[1] in (0, H - 1)) and n not in hole:
                bad.append(n)
                continue
            dq.append(n)
    return bad


_HANG = re.compile(r"chain|(soul_)?lantern")


def floating(t):
    """Decor with nothing to hold it: hanging chains and lanterns without a block at the top of the stack, wall torches, wall banners, wall skulls and
    ladders without a wall behind, standing decor over air."""
    cells = t.cells
    bad = []
    for (x, y, z), c in cells.items():
        p = path_of(c.block)
        pr = c.props
        if p == "chain" and pr.get("axis", "y") == "y":
            yy = y + 1
            while True:
                n = cells.get((x, yy, z))
                if n is not None and path_of(n.block) == "chain" and n.props.get("axis", "y") == "y":
                    yy += 1
                    continue
                break
            above = cells.get((x, yy, z))
            if above is None:
                bad.append(((x, y, z), "chain hangs from air"))
            elif classify(above.block, above.props)[0] == "pass":
                bad.append(((x, y, z), "chain hangs from a non-solid block (%s)" % above.block))
        elif p in ("lantern", "soul_lantern"):
            if pr.get("hanging") == "true":
                above = cells.get((x, y + 1, z))
                if above is None or classify(above.block, above.props)[0] == "pass":
                    bad.append(((x, y, z), "hanging lantern without a support above"))
            else:
                below = cells.get((x, y - 1, z))
                if below is None:
                    bad.append(((x, y, z), "standing lantern over air"))
        elif p.endswith("_wall_banner") or p.endswith("_wall_skull") or p.endswith("_wall_head") or p in ("wall_torch", "soul_wall_torch", "ladder"):
            f = pr.get("facing", "north")
            bx, bz = DV[OPP[f]]
            back = cells.get((x + bx, y, z + bz))
            if back is None or classify(back.block, back.props)[0] == "pass":
                bad.append(((x, y, z), "%s has no wall behind it (facing %s)" % (p, f)))
            elif not p.endswith("_skull") and not p.endswith("_head") and not L.is_full(back.block):
                # a torch, banner or ladder needs a sturdy full face to hold on to; the game drops it otherwise when the piece is placed
                bad.append(((x, y, z), "%s hangs on %s, which is not a full block" % (p, back.block)))
        elif p in ("torch", "soul_torch") or p.endswith("candle") or p.endswith("_skull") or p.endswith("_head") or p in (
                "flower_pot", "brewing_stand", "campfire", "soul_campfire", "cauldron", "water_cauldron", "lava_cauldron", "powder_snow_cauldron", "anvil",
                "grindstone", "lectern", "chest", "barrel", "cake", "bell") or p.endswith("_bed") or p.startswith("potted_") or p.endswith("_pressure_plate"):
            if p == "bell" and pr.get("attachment") in ("ceiling", "single_wall", "double_wall"):
                continue
            if p == "grindstone" and pr.get("face") in ("wall", "ceiling"):
                continue
            below = cells.get((x, y - 1, z))
            if below is None:
                bad.append(((x, y, z), "%s stands on air" % p))
    # a block with air on all six sides is floating, whatever it is (markers and jigsaws are not blocks in the world)
    for (x, y, z), c in cells.items():
        if c.block in ("minecraft:structure_block", "minecraft:jigsaw", "minecraft:light", "minecraft:air"):
            continue
        if all((x + dx, y + dy, z + dz) not in cells for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1))):
            bad.append(((x, y, z), "isolated block %s" % c.block))
    return bad


def disconnected(t):
    """Blocks that are not joined to the building: flood fill (6-connected) through every non-passable cell, starting from the floor, the two wall layers and the ceiling;
    any such block the fill does not reach hangs in the air whatever it touches (a heap resting on a cobweb, a lantern on a broken chain). Why: the simple 'air on all six
    sides' test misses a block that touches a cobweb or a banner, which is not support."""
    W, H, D = t.size
    cells = t.cells
    solid = {}
    for pos, c in cells.items():
        if c.block in ("minecraft:structure_block", "minecraft:jigsaw", "minecraft:light"):
            continue
        if classify(c.block, c.props)[0] != "pass":
            solid[pos] = c
    # the ceiling row: the lowest row above the floor in which every interior column holds a solid block
    ceil_y = None
    for y in range(FLOOR + 2, H):
        if all((x, y, z) in solid for x in range(2, W - 2) for z in range(2, D - 2)):
            ceil_y = y
            break
    seen = set()
    stack = []
    for (x, y, z) in solid:
        if x <= 1 or z <= 1 or x >= W - 2 or z >= D - 2 or y <= FLOOR or (ceil_y is not None and y >= ceil_y):
            seen.add((x, y, z))
            stack.append((x, y, z))
    while stack:
        x, y, z = stack.pop()
        for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
            n = (x + dx, y + dy, z + dz)
            if n in solid and n not in seen:
                seen.add(n)
                stack.append(n)
    return [(pos, "%s is not joined to the building" % solid[pos].block) for pos in sorted(solid) if pos not in seen]


def center_support(block, props, face):
    """True when the block gives the CENTRE of the given face ('up' or 'down') a sturdy surface to stand on or hang from, by the game's `canSupportCenter` rule as far as
    it matters here: a full cube does; a slab does on its flat side only (a top slab holds things on its top, a bottom slab does not); a stair does on its flat back side
    only (an upside-down stair holds things on its top); fences, walls, bars and chains do on the centre post. Why: the game removes a candle, a lantern or a torch whose
    support is not sturdy as soon as a neighbouring block updates, and a template is not shape-checked when it is placed, so the mistake would not show until a
    player touched something next to it."""
    p = path_of(block)
    if L.is_full(block):
        return True
    if p.endswith("_slab"):
        t = props.get("type", "bottom")
        return t == "double" or (t == "top" and face == "up") or (t == "bottom" and face == "down")
    if p.endswith("_stairs"):
        half = props.get("half", "bottom")
        return (half == "top" and face == "up") or (half == "bottom" and face == "down")
    if p.endswith("_fence") or p.endswith("_wall") or p in ("iron_bars", "chain"):
        return True
    return False


def visual_support(block, props):
    """True when something that is not subject to the game's support rule (a skull, a pot, a cauldron) rests flush on the block: a flat surface at the cell top, or an
    open trapdoor (a thin panel, but one cell tall, so what stands on it sits at the same height), or a campfire (a pot hung over the flame)."""
    p = path_of(block)
    return center_support(block, props, "up") or (p.endswith("_trapdoor") and props.get("open") == "true") or p in ("campfire", "soul_campfire")


_NEEDS_TOP = re.compile(r"torch|soul_torch|(.*_)?candle|redstone_torch")
# small things that survive anywhere but would hang half a block above a bottom slab (a block above a bottom slab starts at the cell boundary, not at the slab's top)
_SITS_FLAT = re.compile(r"(?!.*_wall_)(.*_skull|.*_head)|flower_pot|potted_.*|brewing_stand|cake|(water_|lava_|powder_snow_)?cauldron|anvil|grindstone|lectern|bell")


def unsupported(t):
    """Decor the game would drop on its first neighbour update: standing torches, candles and lanterns on a surface that is not sturdy, hanging lanterns and roots under
    something that cannot hold them, pointed dripstone without a sturdy base, vines and tripwire hooks without a full block to cling to."""
    cells = t.cells
    bad = []
    for (x, y, z), c in cells.items():
        p = path_of(c.block)
        pr = c.props
        below = cells.get((x, y - 1, z))
        above = cells.get((x, y + 1, z))
        if (_NEEDS_TOP.fullmatch(p) and not p.endswith("wall_torch")) or (p in ("lantern", "soul_lantern") and pr.get("hanging") != "true"):
            if below is None or not center_support(below.block, below.props, "up"):
                bad.append(((x, y, z), "%s stands on %s, which cannot hold it" % (p, below.block if below else "air")))
        elif _SITS_FLAT.fullmatch(p) and not (p == "grindstone" and pr.get("face") in ("wall", "ceiling")) and not (p == "bell" and pr.get("attachment") != "floor"):
            if below is not None and not visual_support(below.block, below.props) and classify(below.block, below.props)[0] != "pass":
                bad.append(((x, y, z), "%s sits on %s and would float half a block above its top" % (p, below.block)))
        elif p in ("lantern", "soul_lantern") and pr.get("hanging") == "true":
            if above is None or not center_support(above.block, above.props, "down"):
                bad.append(((x, y, z), "hanging %s under %s, which cannot hold it" % (p, above.block if above else "air")))
        elif p in ("hanging_roots", "spore_blossom"):
            if above is None or not L.is_full(above.block):
                bad.append(((x, y, z), "%s needs a full block above it, has %s" % (p, above.block if above else "air")))
        elif p == "pointed_dripstone":
            n = cells.get((x, y - 1, z)) if pr.get("vertical_direction") == "up" else cells.get((x, y + 1, z))
            if n is None or not (L.is_full(n.block) or path_of(n.block) == "pointed_dripstone"):
                bad.append(((x, y, z), "pointed dripstone has no base"))
        elif p == "vine":
            ok = False
            for d, (dx, dz) in DV.items():
                if pr.get(d) == "true":
                    n = cells.get((x + dx, y, z + dz))
                    ok = ok or (n is not None and L.is_full(n.block))
            if pr.get("up") == "true":
                n = cells.get((x, y + 1, z))
                ok = ok or (n is not None and L.is_full(n.block))
            if not ok:
                bad.append(((x, y, z), "vine clings to nothing"))
        elif p == "tripwire_hook":
            f = pr.get("facing", "north")
            dx, dz = DV[OPP[f]]
            n = cells.get((x + dx, y, z + dz))
            if n is None or not L.is_full(n.block):
                bad.append(((x, y, z), "tripwire hook has no full block behind it"))
        elif p == "cake":
            if below is None or classify(below.block, below.props)[0] != "floor":
                bad.append(((x, y, z), "cake needs a solid block below"))
        elif p.endswith("_carpet") or p == "moss_carpet":
            if below is None:
                bad.append(((x, y, z), "%s over air" % p))
        elif p in ("anvil", "chipped_anvil", "damaged_anvil", "sand", "red_sand", "gravel", "dragon_egg") or p.endswith("_concrete_powder"):
            if below is None or classify(below.block, below.props)[0] == "pass":
                bad.append(((x, y, z), "%s would fall (nothing under it)" % p))
        elif p.endswith("_door") and pr.get("half") == "lower":
            if below is None or not L.is_full(below.block):
                bad.append(((x, y, z), "%s needs a full block below" % p))
    return bad


_H4S = ("north", "south", "east", "west")
_H6S = _H4S + ("up", "down")
_BOOL = ("true", "false")
_SIDE3S = ("none", "low", "tall")
_ROT16 = tuple(str(i) for i in range(16))
#: the block-state properties vanilla 1.21.1 really gives each kind of block this kit uses (a block that matches no rule has none). mega_lib's own
#: table lets any property through on a block it does not list, which is how `axis` on a polished block would reach the file; this one does not.
_STRICT = [
    (r".*_stairs", {"facing": _H4S, "half": ("top", "bottom"), "shape": ("straight", "inner_left", "inner_right", "outer_left", "outer_right"), "waterlogged": _BOOL}),
    (r".*_slab", {"type": ("top", "bottom", "double"), "waterlogged": _BOOL}),
    (r".*_wall", {"up": _BOOL, "north": _SIDE3S, "east": _SIDE3S, "south": _SIDE3S, "west": _SIDE3S, "waterlogged": _BOOL}),
    (r".*_fence", {"north": _BOOL, "east": _BOOL, "south": _BOOL, "west": _BOOL, "waterlogged": _BOOL}),
    (r".*_fence_gate", {"facing": _H4S, "open": _BOOL, "powered": _BOOL, "in_wall": _BOOL}),
    (r".*_trapdoor", {"facing": _H4S, "half": ("top", "bottom"), "open": _BOOL, "powered": _BOOL, "waterlogged": _BOOL}),
    (r".*_door", {"facing": _H4S, "half": ("upper", "lower"), "hinge": ("left", "right"), "open": _BOOL, "powered": _BOOL}),
    (r".*_bed", {"facing": _H4S, "part": ("head", "foot"), "occupied": _BOOL}),
    (r".*_wall_banner", {"facing": _H4S}),
    (r".*_banner", {"rotation": _ROT16}),
    (r".*_wall_(skull|head)", {"facing": _H4S, "powered": _BOOL}),
    (r".*_(skull|head)", {"rotation": _ROT16, "powered": _BOOL}),
    (r".*_log|.*_wood|bone_block|hay_block|.*_pillar|stripped_.*", {"axis": ("x", "y", "z")}),
    (r"iron_bars|.*_glass_pane|glass_pane", {"north": _BOOL, "east": _BOOL, "south": _BOOL, "west": _BOOL, "waterlogged": _BOOL}),
    (r"chain", {"axis": ("x", "y", "z"), "waterlogged": _BOOL}),
    (r"(soul_)?lantern", {"hanging": _BOOL, "waterlogged": _BOOL}),
    (r"(.*_)?candle", {"candles": ("1", "2", "3", "4"), "lit": _BOOL, "waterlogged": _BOOL}),
    (r"(soul_)?campfire", {"facing": _H4S, "lit": _BOOL, "signal_fire": _BOOL, "waterlogged": _BOOL}),
    (r"(soul_)?wall_torch", {"facing": _H4S}),
    (r"furnace|blast_furnace|smoker", {"facing": _H4S, "lit": _BOOL}),
    (r"barrel", {"facing": _H6S, "open": _BOOL}),
    (r"chest", {"facing": _H4S, "type": ("single", "left", "right"), "waterlogged": _BOOL}),
    (r"chiseled_bookshelf", dict({"facing": _H4S}, **{"slot_%d_occupied" % i: _BOOL for i in range(6)})),
    (r"ladder", {"facing": _H4S, "waterlogged": _BOOL}),
    (r"lectern", {"facing": _H4S, "has_book": _BOOL, "powered": _BOOL}),
    (r"lightning_rod", {"facing": _H6S, "powered": _BOOL, "waterlogged": _BOOL}),
    (r"pointed_dripstone", {"vertical_direction": ("up", "down"), "thickness": ("tip_merge", "tip", "frustum", "middle", "base"), "waterlogged": _BOOL}),
    (r"anvil|chipped_anvil|damaged_anvil", {"facing": _H4S}),
    (r"grindstone", {"face": ("floor", "wall", "ceiling"), "facing": _H4S}),
    (r"brewing_stand", {"has_bottle_0": _BOOL, "has_bottle_1": _BOOL, "has_bottle_2": _BOOL}),
    (r"water_cauldron|powder_snow_cauldron", {"level": ("1", "2", "3")}),
    (r"tripwire_hook", {"facing": _H4S, "attached": _BOOL, "powered": _BOOL}),
    (r"vine", {"up": _BOOL, "north": _BOOL, "east": _BOOL, "south": _BOOL, "west": _BOOL}),
    (r"structure_block", {"mode": ("save", "load", "corner", "data")}),
    (r"jigsaw", {"orientation": L.ORIENTATIONS}),
    (r"cake", {"bites": tuple(str(i) for i in range(7))}),
    (r"light", {"level": tuple(str(i) for i in range(16)), "waterlogged": _BOOL}),
    (r"hanging_roots|spore_blossom_x", {"waterlogged": _BOOL}),
    (r"end_rod", {"facing": _H6S}),
    (r"pale_hanging_moss_x", {}),
]
_STRICT = [(re.compile(a), b) for a, b in _STRICT]


def strict_state_problems(t):
    """Every (block, property) of template `t` that vanilla 1.21.1 does not have, as a sorted list of strings."""
    seen = set()
    bad = []
    for pos, c in t.cells.items():
        p = path_of(c.block)
        key = (c.block, tuple(sorted(c.props.items())))
        if key in seen:
            continue
        seen.add(key)
        rule = None
        for pat, allowed in _STRICT:
            if pat.fullmatch(p):
                rule = allowed
                break
        rule = rule if rule is not None else {}
        for k, v in c.props.items():
            if k not in rule:
                bad.append("%s has no property %s (at %s)" % (c.block, k, pos))
            elif rule[k] is not None and v not in rule[k]:
                bad.append("%s: %s=%s is not one of %s (at %s)" % (c.block, k, v, "/".join(rule[k]), pos))
    return sorted(set(bad))


def check_hall(t, quiet=False, min_eye_light=5, light_floor_pct=0.04):
    """Everything that can be verified without the game. Returns (problems, stats): problems are strings (empty list = clean)."""
    problems = []
    stats = {}
    W, H, D = t.size
    for msg in strict_state_problems(t):
        problems.append("block state: " + msg)
    nodes = walk_nodes(t)
    starts = socket_starts(t)
    if not starts:
        problems.append("no door socket")
    seen = reach(t, nodes)
    wide_seen = reach(t, nodes, wide=True)
    # every socket must be able to reach every other socket on the walk graph and on the 3-wide graph
    for i, st in enumerate(starts):
        one = reach(t, nodes, [st])
        for j, other in enumerate(starts):
            if i != j and other not in one:
                problems.append("door %d (%s) cannot reach door %d (%s) on foot" % (i, st[:2], j, other[:2]))
        w1 = reach(t, nodes, [st], wide=True)
        if st not in w1:
            problems.append("door %d (%s): the cells in front of it are not 3 wide" % (i, st[:2]))
        for j, other in enumerate(starts):
            if i != j and other not in w1:
                problems.append("no 3-wide route from door %d (%s) to door %d (%s)" % (i, st[:2], j, other[:2]))
    cols = {(x, z) for x, z, s in seen}
    stats["walkable_columns"] = len(cols)
    # markers
    mk = markers_of(t)
    counts = collections.Counter(k for k, *_ in mk)
    stats["markers"] = dict(counts)
    wide_local = _wide_nodes(t, nodes)
    for kind, x, y, z, args in mk:
        if kind in ("spawn", "patrol"):
            s = next((s2 for s2 in nodes.get((x, z), ()) if abs(s2 - 2 * y) <= 1), None)
            if s is None or (x, z, s) not in seen:
                problems.append("%s marker at (%d,%d,%d) is not reachable on foot" % (kind, x, y, z))
            if kind == "spawn" and s is not None:
                if (x, z, s) not in wide_local:
                    problems.append("spawn marker at (%d,%d,%d) is cramped (no free 3x3 around it)" % (x, y, z))
                if x < 4 or z < 4 or x > W - 5 or z > D - 5:
                    problems.append("spawn marker at (%d,%d,%d) is within 2 blocks of a wall" % (x, y, z))
        c = t.cells.get((x, y - 1, z))
        if kind in ("seat", "hearth", "bed", "furniture") and c is None:
            problems.append("%s marker at (%d,%d,%d) stands over air" % (kind, x, y, z))
    if counts.get("spawn", 0) < 1:
        problems.append("no spawn marker")
    # armor stands
    stands = sum(1 for e in t.entities if str(e["nbt"].get("id", "")) == "minecraft:armor_stand")
    stats["armor_stands"] = stands
    if stands > 12:
        problems.append("%d armor stands (budget 12)" % stands)
    # shell and floating
    lk = leaks(t)
    if lk:
        problems.append("%d see-through leak cells in the shell, e.g. %s" % (len(lk), lk[:3]))
    fl = floating(t)
    for pos, why in fl:
        problems.append("floating decor at %s: %s" % (pos, why))
    for pos, why in unsupported(t):
        problems.append("unsupported decor at %s: %s" % (pos, why))
    for pos, why in disconnected(t):
        problems.append("floating decor at %s: %s" % (pos, why))
    # light
    lv = light_levels(t)
    eye = []
    holes = door_holes(t)
    for (x, z, s) in seen:
        yfeet = s // 2
        yeye = int(s / 2.0 + 1.62)
        if (x, yfeet, z) in holes:
            continue    # inside a doorway tunnel: lit by the room on the other side
        eye.append(((x, z), lv.get((x, yeye, z), 0), lv.get((x, yfeet, z), 0)))
    if eye:
        vals = sorted(v for _, v, _ in eye)
        stats["eye_light_min"] = vals[0]
        stats["eye_light_median"] = vals[len(vals) // 2]
        stats["eye_light_mean"] = round(sum(vals) / len(vals), 2)
        dark = [(p, v) for p, v, _ in eye if v < min_eye_light]
        stats["dark_columns"] = len(dark)
        stats["dark_fraction"] = round(len(dark) / len(eye), 3)
        if len(dark) / len(eye) > light_floor_pct:
            problems.append("%.1f%% of the floor is darker than block light %d at eye level (budget %.0f%%)" % (100 * len(dark) / len(eye), min_eye_light,
                                                                                                            100 * light_floor_pct))
    stats["blocks"] = len(t.cells)
    stats["entities"] = len(t.entities)
    return problems, stats


# ===================================================================================================================
# the builder: a hall under construction, with the safety rails
# ===================================================================================================================
_B2 = ("true", "false")
_RAW_BLOCKS = {
    "minecraft:chiseled_bookshelf": dict({"facing": ("north", "south", "east", "west")}, **{"slot_%d_occupied" % i: _B2 for i in range(6)}),
}


def _norm(props):
    out = {}
    for k, v in (props or {}).items():
        out[str(k)] = ("true" if v else "false") if isinstance(v, bool) else str(v)
    return out


class Hall:
    """A furnished hall: a `mx` x `mz` module shell of height `h` (interior rows) plus everything written through `set`.

    Why the rails: `set` refuses to overwrite the wall shell or earlier decor (a chest inside a stair is a bug, not a style) unless `over=True`, and refuses
    a non-walkable block inside a declared route (`route`, and the apron `door` declares in front of every doorway). `try_set` is the polite version for
    finishing passes (cobwebs, trim, wall lamps): it skips whatever is in the way."""

    def __init__(self, name, mx, mz, h, wear=0.10, floor=L.TILE, wall=L.BRICK, ceiling=L.TILE):
        self.name = name
        self.mt = L.MT(name, mx, mz)
        self.mt.room(h, floor=floor, wall=wall, ceiling=ceiling, wear=wear)
        self.W, self.D, self.h = self.mt.W, self.mt.D, h
        self.top = FLOOR + h
        self.x0, self.x1 = L.WALL, self.W - 1 - L.WALL
        self.z0, self.z1 = L.WALL, self.D - 1 - L.WALL
        self.cx, self.cz = self.W // 2, self.D // 2
        self.rng = self.mt.rng
        self.shell = set(self.mt.t.cells)
        self.deco = {}
        self.volumes = []
        self.doors = []
        self.stands = 0

    # ---- queries
    def inside(self, x, z):
        return self.x0 <= x <= self.x1 and self.z0 <= z <= self.z1

    def get(self, x, y, z):
        return self.mt.t.get(x, y, z)

    def free(self, x, y, z):
        """True when nothing is placed at the cell (the world keeps air there) and it is not a doorway hole."""
        return (x, y, z) not in self.mt.t.cells and (x, y, z) not in self.mt.reserved and 0 <= x < self.W and 0 <= y < L.HEIGHT and 0 <= z < self.D

    def _in_volume(self, pos):
        x, y, z = pos
        for (x1, y1, z1, x2, y2, z2, label) in self.volumes:
            if x1 <= x <= x2 and y1 <= y <= y2 and z1 <= z <= z2:
                return label
        return None

    # ---- writing
    def set(self, x, y, z, block, props=None, nbt=None, over=False):
        pos = (x, y, z)
        props = _norm(props)
        if ":" not in block:
            block = "minecraft:" + block
        if not over:
            if pos in self.shell:
                raise ValueError("%s: decor %s at %s would overwrite the wall/floor/ceiling shell (use carve/shell_set/over=True)" % (self.name, block, pos))
            if pos in self.mt.t.cells:
                raise ValueError("%s: %s at %s would overwrite decor %s" % (self.name, block, pos, self.mt.t.cells[pos].block))
        vol = self._in_volume(pos)
        if vol is not None and classify(block, props)[0] != "pass":
            raise ValueError("%s: %s at %s blocks the route '%s'" % (self.name, block, pos, vol))
        spec = _RAW_BLOCKS.get(block)
        if spec is not None:
            # a block whose property table in mega_lib is wrong or missing (its pattern `chiseled_.*` claims an `axis`): check it here and write it directly
            for k, v in props.items():
                if k not in spec or v not in spec[k]:
                    raise ValueError("%s: bad block state at %s: %s has no property %s=%s" % (self.name, pos, block, k, v))
            if pos in self.mt.reserved:
                raise ValueError("%s: %s at %s is inside a doorway hole" % (self.name, block, pos))
            self.mt.t.set(x, y, z, block, props, nbt)
        else:
            self.mt.set(x, y, z, block, props, nbt)
        self.shell.discard(pos)
        self.deco[pos] = block
        return self

    def can_set(self, x, y, z, block="minecraft:stone", props=None):
        """True when `set` would succeed on a free cell (free, and not a solid block inside a declared route)."""
        if not self.free(x, y, z):
            return False
        vol = self._in_volume((x, y, z))
        if vol is not None and classify(block if ":" in block else "minecraft:" + block, _norm(props))[0] != "pass":
            return False
        return True

    def try_set(self, x, y, z, block, props=None, nbt=None):
        """Sets the block only when the cell is completely free; returns whether it did."""
        if not self.free(x, y, z):
            return False
        props = _norm(props)
        vol = self._in_volume((x, y, z))
        if vol is not None and classify(block if ":" in block else "minecraft:" + block, props)[0] != "pass":
            return False
        self.set(x, y, z, block, props, nbt)
        return True

    def furnace(self, x, y, z, kind, facing, burn=32767):
        """A furnace, blast furnace or smoker that starts LIT: the block state `lit=true` alone is reset by the block entity on its first tick (no fuel), so the
        template also carries `BurnTime` (ticks of fire left, the largest a short holds: about 27 minutes of loaded time) and the matching cook fields. After that it
        goes cold like any furnace; the room's light never depends on it (see `emission`)."""
        import nbt as _nbt
        data = _nbt.Compound({"id": "minecraft:%s" % kind, "BurnTime": _nbt.Short(burn), "CookTime": _nbt.Short(0),
                              "CookTimeTotal": _nbt.Short(100 if kind in ("blast_furnace", "smoker") else 200)})
        return self.set(x, y, z, "minecraft:%s" % kind, {"facing": facing, "lit": True}, nbt=data)

    def loot_chest(self, x, y, z, facing, table="nusmp:chests/labyrinth_trial"):
        """A chest that rolls `table` on first open (LootTableSeed 0 = random per world), written the way Template.chest does."""
        import nbt as _nbt
        data = _nbt.Compound({"id": "minecraft:chest", "LootTable": table, "LootTableSeed": _nbt.Long(0)})
        return self.set(x, y, z, "minecraft:chest", {"facing": facing, "type": "single", "waterlogged": False}, nbt=data)

    def fill(self, a, b, block, props=None, over=False, skip_blocked=False):
        (x1, y1, z1), (x2, y2, z2) = a, b
        for y in range(min(y1, y2), max(y1, y2) + 1):
            for z in range(min(z1, z2), max(z1, z2) + 1):
                for x in range(min(x1, x2), max(x1, x2) + 1):
                    if skip_blocked:
                        self.try_set(x, y, z, block, props)
                    else:
                        self.set(x, y, z, block, props, over=over)
        return self

    def carve(self, x, y, z):
        """Removes a shell cell (a niche, a window, a hole in the floor): the cell becomes air and may then hold decor."""
        if (x, y, z) in self.mt.reserved:
            raise ValueError("%s: (%d,%d,%d) is a doorway hole" % (self.name, x, y, z))
        self.mt.remove(x, y, z)
        self.shell.discard((x, y, z))
        return self

    def shell_set(self, x, y, z, block, props=None):
        """Replaces a shell cell by another block (an inlay in the floor, a trim course in the wall); it stays part of the shell."""
        self.mt.set(x, y, z, block, _norm(props))
        self.shell.add((x, y, z))
        return self

    def floor_set(self, x, z, block, props=None):
        return self.shell_set(x, FLOOR, z, block, props)

    def route(self, x1, z1, x2, z2, y1=GROUND, y2=GROUND + 2, label=""):
        """Declares a box that must stay walkable (nothing solid may be set in it)."""
        self.volumes.append((min(x1, x2), y1, min(z1, z2), max(x1, x2), y2, max(z1, z2), label or "route"))
        return self

    def door(self, side, offset, apron=5):
        """A door socket plus a clear apron (3 wide, `apron` deep, 3 high) in front of it so no furniture can ever block the doorway."""
        self.mt.socket(side, offset)
        self.doors.append((side, offset))
        w = Wall(self, side)
        a = w.cell(offset - 1, GROUND, 0)
        b = w.cell(offset + 1, GROUND + 2, apron - 1)
        self.volumes.append((min(a[0], b[0]), GROUND, min(a[2], b[2]), max(a[0], b[0]), GROUND + 2, max(a[2], b[2]), "door %s@%d" % (side, offset)))
        return self

    def wall(self, side):
        return Wall(self, side)

    def frame(self, x, y, z, facing):
        return Fr(self, x, y, z, facing)

    # ---- markers and entities
    def marker(self, x, y, z, kind, **args):
        if (x, y, z) in self.mt.t.cells:
            raise ValueError("%s: marker %s at (%d,%d,%d) lands on %s" % (self.name, kind, x, y, z, self.mt.t.cells[(x, y, z)].block))
        self.mt.marker(x, y, z, kind, **args)
        return self

    def armor_stand(self, x, y, z, **kw):
        self.stands += 1
        if self.stands > 12:
            raise ValueError("%s: more than 12 armor stands" % self.name)
        self.mt.armor_stand(x, y, z, **kw)
        return self

    def faction(self, faction, x=None, z=None):
        x = self.cx if x is None else x
        z = self.cz if z is None else z
        y = GROUND
        # a faction marker is a hint for the whole room: park it where nothing else stands
        for dz in range(0, 6):
            for dx in range(0, 6):
                for (sx, sz) in ((x + dx, z + dz), (x - dx, z - dz), (x + dx, z - dz), (x - dx, z + dz)):
                    if self.inside(sx, sz) and self.free(sx, y, sz) and self.free(sx, y + 1, sz):
                        self.marker(sx, y, sz, "faction", faction=faction)
                        return self
        raise ValueError("%s: no free cell for the faction marker" % self.name)

    # ---- output
    def finish(self):
        self.mt.finish()
        return self

    def check(self, **kw):
        return check_hall(self.mt.t, **kw)

    def save(self):
        return self.mt.save()


class Wall:
    """One of the four interior wall faces, addressed by a lateral coordinate along the wall (`lat`, absolute: x for the north and south walls, z for the
    west and east walls), a row `y` and a depth `d`: 0 is the first interior cell in front of the wall, -1 the inner wall layer, -2 the outer wall layer,
    1.. further into the room. Why: wall dressing is written once and works on all four walls; `into` and `out` are the compass names to put in
    `facing` (a wall banner faces `into`, a stair that hugs the wall faces `out`)."""

    def __init__(self, hall, side):
        if side not in ("n", "s", "w", "e"):
            raise ValueError("side must be n, s, w or e")
        self.hall, self.side = hall, side
        self.into = {"n": "south", "s": "north", "w": "east", "e": "west"}[side]
        self.out = OPP[self.into]
        self.inc = "east" if side in ("n", "s") else "south"   # the direction in which `lat` grows
        self.dec = OPP[self.inc]
        self.length = hall.W if side in ("n", "s") else hall.D

    def cell(self, lat, y, d):
        h = self.hall
        if self.side == "n":
            return (lat, y, h.z0 + d)
        if self.side == "s":
            return (lat, y, h.z1 - d)
        if self.side == "w":
            return (h.x0 + d, y, lat)
        return (h.x1 - d, y, lat)

    def set(self, lat, y, d, block, props=None, **kw):
        return self.hall.set(*self.cell(lat, y, d), block, props, **kw)

    def try_set(self, lat, y, d, block, props=None, **kw):
        return self.hall.try_set(*self.cell(lat, y, d), block, props, **kw)

    def shell_set(self, lat, y, d, block, props=None):
        return self.hall.shell_set(*self.cell(lat, y, d), block, props)

    def carve(self, lat, y, d=-1):
        return self.hall.carve(*self.cell(lat, y, d))

    def free(self, lat, y, d=0):
        return self.hall.free(*self.cell(lat, y, d))

    def can(self, lat, y, d=0, block="minecraft:stone", props=None):
        return self.hall.can_set(*self.cell(lat, y, d), block, props)

    def backed(self, lat, y, d=0):
        """True when the cell one layer behind (lat, y, d) is a full cube: something hung there (a wall torch, a banner, a ladder) has a wall to hang on. Why a
        FULL cube and not just 'not air': the game checks the face a torch or banner hangs on when the template is placed and drops the item when the face is
        not sturdy, so a torch on iron bars or on a stair back would be gone in the world."""
        pos = self.cell(lat, y, d - 1)
        if pos in self.hall.mt.reserved:
            return False      # the cell behind is a doorway hole that will be carved out
        c = self.hall.get(*pos)
        return c is not None and L.is_full(c.block)

    def all_free(self, cells, block="minecraft:stone"):
        """cells: [(lat, y, d), ...]; True when every one can take a solid block."""
        return all(self.can(lat, y, d, block) and (d != 0 or self.backed(lat, y, 0)) for (lat, y, d) in cells)

    def fill(self, lat1, lat2, y1, y2, d1, d2, block, props=None, **kw):
        for lat in range(min(lat1, lat2), max(lat1, lat2) + 1):
            for y in range(min(y1, y2), max(y1, y2) + 1):
                for d in range(min(d1, d2), max(d1, d2) + 1):
                    self.set(lat, y, d, block, props, **kw)
        return self

    def frame(self, lat, y=GROUND, d=0):
        x, yy, z = self.cell(lat, y, d)
        return Fr(self.hall, x, yy, z, self.into)

    def marker(self, lat, y, d, kind, **args):
        return self.hall.marker(*self.cell(lat, y, d), kind, **args)


class Fr:
    """A frame at a cell looking along `facing`: (r, u, f) = (to the right, up, forward). Props whose value is '@forward', '@back', '@left' or '@right' are
    turned into the compass name for the frame, so a prop is written once and is right whichever way it faces."""

    def __init__(self, hall, x, y, z, facing):
        if facing not in DV:
            raise ValueError("facing must be north/east/south/west, got %r" % (facing,))
        self.hall, self.x, self.y, self.z, self.facing = hall, x, y, z, facing
        self.fx, self.fz = DV[facing]
        self.rx, self.rz = DV[RIGHT[facing]]

    def at(self, r, u, f):
        return (self.x + r * self.rx + f * self.fx, self.y + u, self.z + r * self.rz + f * self.fz)

    def dir(self, rel):
        return {"forward": self.facing, "back": OPP[self.facing], "right": RIGHT[self.facing], "left": LEFT[self.facing]}[rel]

    def _props(self, props):
        if not props:
            return props
        out = {}
        for k, v in props.items():
            if isinstance(v, str) and v.startswith("@"):
                v = self.dir(v[1:])
            out[k] = v
        return out

    def set(self, r, u, f, block, props=None, **kw):
        return self.hall.set(*self.at(r, u, f), block, self._props(props), **kw)

    def try_set(self, r, u, f, block, props=None, **kw):
        return self.hall.try_set(*self.at(r, u, f), block, self._props(props), **kw)

    def fill(self, r1, u1, f1, r2, u2, f2, block, props=None, **kw):
        for r in range(min(r1, r2), max(r1, r2) + 1):
            for u in range(min(u1, u2), max(u1, u2) + 1):
                for f in range(min(f1, f2), max(f1, f2) + 1):
                    self.set(r, u, f, block, props, **kw)
        return self

    def free(self, r, u, f):
        return self.hall.free(*self.at(r, u, f))

    def marker(self, r, u, f, kind, **args):
        return self.hall.marker(*self.at(r, u, f), kind, **args)

    def sub(self, r, u, f, rel="forward"):
        x, y, z = self.at(r, u, f)
        return Fr(self.hall, x, y, z, self.dir(rel))


# ===================================================================================================================
# shell dressing: the architecture that makes a box read as a built room (all in palette blocks, so the Region Theme restyles it)
# ===================================================================================================================
def ring_stairs(h, x1, z1, x2, z2, y, half="bottom", stairs=L.POLISHED_STAIRS, over=False):
    """A ring of stairs around the rectangle [x1..x2] x [z1..z2] at row y, every tall side towards the rectangle and the four corners as outer-corner
    stairs: a bevelled base (half=bottom) or a flared capital (half=top) for a pillar. The rectangle itself is not touched."""
    for z in range(z1 - 1, z2 + 2):
        for x in range(x1 - 1, x2 + 2):
            inside_x, inside_z = x1 <= x <= x2, z1 <= z <= z2
            if inside_x and inside_z:
                continue
            if not inside_x and not inside_z:
                fz = "south" if z < z1 else "north"
                fx = "east" if x < x1 else "west"
                shape = "outer_left" if fx == LEFT[fz] else "outer_right"
                h.set(x, y, z, stairs, {"facing": fz, "half": half, "shape": shape}, over=over)
            elif inside_x:
                h.set(x, y, z, stairs, {"facing": "south" if z < z1 else "north", "half": half}, over=over)
            else:
                h.set(x, y, z, stairs, {"facing": "east" if x < x1 else "west", "half": half}, over=over)


def column(h, x, z, w=1, y0=GROUND, y1=None, shaft=L.POLISHED, stairs=L.POLISHED_STAIRS, cap=L.CHISELED, rings=True):
    """A free-standing pillar: a `w` x `w` shaft from row y0 to y1 (default: up to the ceiling), a ring of stairs round its foot and a flared ring under
    the ceiling (a chiselled block tops the shaft). Footprint 3x3 (w=1) or 4x4 (w=2) at the two ring rows, w x w in between."""
    y1 = h.top if y1 is None else y1
    for yy in range(y0, y1 + 1):
        blk = cap if yy == y1 and cap else shaft
        for dz in range(w):
            for dx in range(w):
                h.set(x + dx, yy, z + dz, blk)
    if rings:
        ring_stairs(h, x, z, x + w - 1, z + w - 1, y0, "bottom", stairs)
        if y1 > y0 + 2:
            ring_stairs(h, x, z, x + w - 1, z + w - 1, y1, "top", stairs)


def pilaster(w, lat, width=2, y0=GROUND, y1=None, shaft=L.POLISHED, stairs=L.POLISHED_STAIRS, cap=L.CHISELED, depth=1):
    """A pilaster (a flat column engaged in the wall): shaft `depth` deep with a stair base and a flared capital one cell further out. On a wall face it
    breaks the long plane into bays and gives the lamps and banners somewhere to hang."""
    h = w.hall
    y1 = h.top if y1 is None else y1
    for lt in range(lat, lat + width):
        for d in range(depth):
            for y in range(y0, y1 + 1):
                w.set(lt, y, d, cap if y == y1 and cap else shaft)
        w.set(lt, y0, depth, stairs, {"facing": w.out, "half": "bottom"})
        if y1 > y0 + 2:
            w.set(lt, y1, depth, stairs, {"facing": w.out, "half": "top"})


def cornice(h, y=None, stairs=L.POLISHED_STAIRS, corner=L.POLISHED, skip_sides=()):
    """Crown moulding round the whole interior at row y (default: the top row): upside-down stairs hugging the wall, a full block in each corner."""
    y = h.top if y is None else y
    for side in ("n", "s", "w", "e"):
        if side in skip_sides:
            continue
        w = h.wall(side)
        lo = (h.x0 if side in ("n", "s") else h.z0) + 1
        hi = (h.x1 if side in ("n", "s") else h.z1) - 1
        for lat in range(lo, hi + 1):
            w.try_set(lat, y, 0, stairs, {"facing": w.out, "half": "top"})
    for (x, z) in ((h.x0, h.z0), (h.x1, h.z0), (h.x0, h.z1), (h.x1, h.z1)):
        h.try_set(x, y, z, corner)


def plinth(h, block=L.POLISHED_SLAB, row=GROUND):
    """A low skirting of bottom slabs round the foot of every wall, wherever the floor is still free (it never blocks a doorway apron or a prop)."""
    for side in ("n", "s", "w", "e"):
        w = h.wall(side)
        lo = h.x0 if side in ("n", "s") else h.z0
        hi = h.x1 if side in ("n", "s") else h.z1
        for lat in range(lo, hi + 1):
            w.try_set(lat, row, 0, block, {"type": "bottom"})


def door_trim(h, side, offset, lintel_row=None):
    """The interior face of a doorway: a polished jamb on each side, a lintel with a chiselled keystone, and the two top corners of the opening bevelled by
    upside-down stairs. Why: the frame `MT.socket` writes lives inside the 2-thick wall; this carries it one cell into the room so the opening reads as a
    portal, and it is the same on every door of every room."""
    w = h.wall(side)
    top_row = L.DOOR_ROW + L.DOOR_H   # 13: the lintel row of the frame inside the wall
    for sgn in (-1, 1):
        for y in range(GROUND, top_row + 1):
            w.try_set(offset + 2 * sgn, y, 0, L.POLISHED if y < top_row else L.CHISELED)
        w.try_set(offset + sgn, top_row - 1, 0, L.POLISHED_STAIRS, {"facing": w.dec if sgn < 0 else w.inc, "half": "top"})
    for lt in range(offset - 1, offset + 2):
        w.try_set(lt, top_row, 0, L.CHISELED if lt == offset else L.POLISHED)


def wall_band(h, y, block=L.POLISHED, accent=L.CHISELED, every=4, sides="nsew", plain=None):
    """A string course: a horizontal band of `block` (a chiselled `accent` every `every` cells) along the inner wall layer at row `y`, wherever the wall is still plain
    masonry (so windows, niches, doorway frames, bone facings and anything carved are left alone: call it after the dressers). Row 13 is the lintel row of every
    doorway, so a band there runs on from the door heads. Why: a long plain wall reads as a box; one continuous course at door-head height ties the doors, pilasters
    and banners into one order and costs only palette blocks, so the Region Theme restyles it with the rest."""
    plain = plain or (L.BRICK, L.CRACKED_BRICK, L.COBBLED)
    n = 0
    for side in sides:
        w = h.wall(side)
        lo = (h.x0 if side in ("n", "s") else h.z0) - 1
        hi = (h.x1 if side in ("n", "s") else h.z1) + 1
        for lat in range(lo, hi + 1):
            pos = w.cell(lat, y, -1)
            c = h.get(*pos)
            if c is None or pos in h.mt.reserved or c.block not in plain:
                continue
            w.shell_set(lat, y, -1, accent if lat % every == 0 else block)
            n += 1
    return n


def floor_chequer(h, x1, z1, x2, z2, block=L.POLISHED, parity=0):
    """A chequerboard over [x1..x2] x [z1..z2]: `block` on every cell whose (x + z) has the given parity, wherever the floor is still the plain palette floor."""
    n = 0
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            c = h.get(x, FLOOR, z)
            if c is not None and c.block == L.TILE and (x + z) % 2 == parity:
                h.floor_set(x, z, block)
                n += 1
    return n


def floor_border(h, inset=1, block=L.POLISHED, corner=L.CHISELED, width=1):
    """A frame of `block` in the floor, `inset` cells in from the walls and `width` wide, with `corner` on its four corners (plain palette floor only). Why: a
    border of a different stone is the cheapest way to make a floor read as laid out rather than poured."""
    n = 0
    for z in range(h.z0, h.z1 + 1):
        for x in range(h.x0, h.x1 + 1):
            c = h.get(x, FLOOR, z)
            if c is None or c.block != L.TILE:
                continue
            dx = min(x - h.x0, h.x1 - x)
            dz = min(z - h.z0, h.z1 - z)
            r = min(dx, dz)
            if inset <= r < inset + width:
                h.floor_set(x, z, corner if (dx < inset + width and dz < inset + width) else block)
                n += 1
    return n


def floor_medallion(h, cx, cz, r=3, ring=L.POLISHED, core=L.CHISELED, field=None):
    """A diamond medallion in the floor round (cx, cz): a ring of `ring` at Manhattan distance r (and r-2 when r >= 4), `core` at the centre and its four
    neighbours (plain palette floor only)."""
    n = 0
    for z in range(cz - r, cz + r + 1):
        for x in range(cx - r, cx + r + 1):
            c = h.get(x, FLOOR, z)
            if c is None or c.block != L.TILE:
                continue
            d = abs(x - cx) + abs(z - cz)
            if d == r or (r >= 4 and d == r - 2):
                h.floor_set(x, z, ring)
                n += 1
            elif d <= 1:
                h.floor_set(x, z, core)
                n += 1
            elif field is not None and d < r:
                h.floor_set(x, z, field)
                n += 1
    return n


def niche(w, lat, y0, y1, width=3, arch=True):
    """Carves a recess `width` wide in the inner wall layer (the outer layer stays as its back), rows y0..y1, with the two top corners bevelled by stairs.
    Returns the list of carved (lat, y) cells. The caller furnishes it (a lantern on the sill, a chest, skulls ...)."""
    half = width // 2
    out = []
    for lt in range(lat - half, lat - half + width):
        for y in range(y0, y1 + 1):
            w.carve(lt, y, -1)
            out.append((lt, y))
    if arch and width >= 3:
        w.set(lat - half, y1, -1, L.POLISHED_STAIRS, {"facing": w.dec, "half": "top"})
        w.set(lat - half + width - 1, y1, -1, L.POLISHED_STAIRS, {"facing": w.inc, "half": "top"})
    return out


def window_slit(w, lat, y0, y1, glow=None, glass="minecraft:light_blue_stained_glass"):
    """A narrow window in the wall that never shows the void: a dark slit (iron bars set into the inner layer, the outer layer is the back) or, with `glow`
    (a bright block id such as minecraft:shroomlight), a stained-glass slit lit from behind (glass in the inner layer, the glow block in the outer layer)."""
    for y in range(y0, y1 + 1):
        w.carve(lat, y, -1)
        if glow is None:
            w.set(lat, y, -1, L.IRON_BARS)
        else:
            w.set(lat, y, -1, glass)
            w.shell_set(lat, y, -2, glow)


def beams(h, axis, positions, y=None, block=L.STRIPPED_LOG, corbel=L.PLANKS_STAIRS, span=None):
    """Ceiling beams of `block` just under the ceiling, along `axis` ('x' or 'z'), one at each position (a z for axis x, an x for axis z), with a stair
    corbel under each end. Skips cells that are already used (a pillar, a chain). A log beam is laid along its axis; any other block (polished stone)
    has no axis to set."""
    y = h.top if y is None else y
    has_axis = bool(re.fullmatch(r".*_log|.*_wood|stripped_.*", path_of(block)))
    for pos in positions:
        if axis == "x":
            lo, hi = h.x0, h.x1
            for x in range(lo, hi + 1):
                h.try_set(x, y, pos, block, {"axis": "x"} if has_axis else None)
            if corbel:
                h.try_set(lo, y - 1, pos, corbel, {"facing": "west", "half": "top"})
                h.try_set(hi, y - 1, pos, corbel, {"facing": "east", "half": "top"})
        else:
            lo, hi = h.z0, h.z1
            for z in range(lo, hi + 1):
                h.try_set(pos, y, z, block, {"axis": "z"} if has_axis else None)
            if corbel:
                h.try_set(pos, y - 1, lo, corbel, {"facing": "north", "half": "top"})
                h.try_set(pos, y - 1, hi, corbel, {"facing": "south", "half": "top"})


# ===================================================================================================================
# light: every fixture is a real vanilla light source so the Gloom clock counts it
# ===================================================================================================================
def hang_lantern(h, x, z, drop, y_top=None, soul=False):
    """A lantern on a chain from row y_top (default: the top row) down `drop` cells."""
    y_top = h.top if y_top is None else y_top
    for k in range(drop):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - drop, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})


def chandelier_wheel(h, x, z, r=2, drop=3, y_top=None, soul=False, candles=False):
    """A wheel chandelier: four chains down from the ceiling to a square ring of horizontal chains, a lantern hung under every corner and under the
    middle of every side (or, with candles=True, lit candles standing on the ring). Why: vanilla chains lie on any axis, so a ring needs no custom block."""
    y_top = h.top if y_top is None else y_top
    yb = y_top - drop
    lant = L.SOUL_LANTERN if soul else L.LANTERN
    for sx in (-1, 1):
        for sz in (-1, 1):
            for yy in range(yb, y_top + 1):
                h.set(x + sx * r, yy, z + sz * r, L.CHAIN_BLOCK, {"axis": "y"})
    for k in range(-r + 1, r):
        h.set(x + k, yb, z - r, L.CHAIN_BLOCK, {"axis": "x"})
        h.set(x + k, yb, z + r, L.CHAIN_BLOCK, {"axis": "x"})
        h.set(x - r, yb, z + k, L.CHAIN_BLOCK, {"axis": "z"})
        h.set(x + r, yb, z + k, L.CHAIN_BLOCK, {"axis": "z"})
    if candles:
        for k in range(-r + 1, r):
            for (px, pz) in ((x + k, z - r), (x + k, z + r), (x - r, z + k), (x + r, z + k)):
                h.set(px, yb + 1, pz, "minecraft:candle", {"candles": "2", "lit": "true"})
    else:
        for (px, pz) in ((x - r, z - r), (x + r, z - r), (x - r, z + r), (x + r, z + r), (x, z - r), (x, z + r), (x - r, z), (x + r, z)):
            h.set(px, yb - 1, pz, lant, {"hanging": "true"})
    # the middle chain carries one more lantern so the wheel is not hollow
    for yy in range(yb, y_top + 1):
        h.set(x, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, yb - 1, z, lant, {"hanging": "true"})


def lantern_globe(h, x, z, y_centre, y_top=None, glass="minecraft:orange_stained_glass", soul=False):
    """A globe lamp: a rounded 3 x 3 x 3 shell of stained glass (the eight corner cells left out, so it reads as a sphere) hung on a chain from `y_top` (default:
    the top row), with a lantern hanging inside from the cap. `y_centre` is the row of the lantern. Glass does not stop light, so the globe lights the room like
    the lantern it holds and glows in the colour of the glass. Why: the brief's 'globes of lanterns' for the libraries: a reading lamp that is a thing to look at."""
    y_top = h.top if y_top is None else y_top
    for dy in (-1, 0, 1):
        for dz in (-1, 0, 1):
            for dx in (-1, 0, 1):
                if dx == 0 and dy == 0 and dz == 0:
                    continue
                if abs(dx) + abs(dy) + abs(dz) == 3:
                    continue
                h.set(x + dx, y_centre + dy, z + dz, glass)
    for yy in range(y_centre + 2, y_top + 1):
        h.set(x, yy, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_centre, z, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})


def bracket_lantern(w, lat, y, arm=1, soul=False, d0=0):
    """A lantern on a wall bracket: a fence arm `arm` cells long out of whatever stands at depth d0-1 (the wall, or a column) with the lantern hanging from
    its tip."""
    for d in range(d0, d0 + arm):
        w.set(lat, y, d, L.FENCE)
    w.set(lat, y - 1, d0 + arm - 1, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})


def sconce(w, lat, y, soul=False, kind="torch"):
    """A torch (or soul torch) on a stone corbel: an upside-down stair against the wall and the torch standing on it."""
    w.set(lat, y, 0, L.POLISHED_STAIRS, {"facing": w.out, "half": "top"})
    w.set(lat, y + 1, 0, "minecraft:soul_torch" if soul else "minecraft:torch")


def wall_torch(w, lat, y, soul=False):
    w.set(lat, y, 0, "minecraft:soul_wall_torch" if soul else "minecraft:wall_torch", {"facing": w.into})


def brazier(h, x, z, y=GROUND, soul=False, post=L.POLISHED_WALL):
    """A standing fire bowl: a wall post, a campfire on it and four open iron trapdoors flaring round the flame like a cage."""
    h.set(x, y, z, post)
    h.set(x, y + 1, z, "minecraft:soul_campfire" if soul else "minecraft:campfire", {"facing": "north", "lit": "true", "signal_fire": "false"})
    for d in DIRS:
        dx, dz = DV[d]
        h.set(x + dx, y + 1, z + dz, "minecraft:iron_trapdoor", {"facing": d, "half": "bottom", "open": "true"})


def skull_sconce(w, lat, y, kind="wither_skeleton", soul=True):
    """A skull on the wall with a torch burning on the wall block above its head: a flaming-skull sconce (mega_lib.skull_lamp 'sconce', through the rails)."""
    w.set(lat, y, 0, "minecraft:%s_wall_skull" % kind, {"facing": w.into})
    w.set(lat, y + 1, 0, "minecraft:soul_wall_torch" if soul else "minecraft:wall_torch", {"facing": w.into})


def candles_at(h, x, y, z, n=3, color="white", lit=True):
    h.set(x, y, z, "minecraft:%s_candle" % color if color else "minecraft:candle", {"candles": str(n), "lit": "true" if lit else "false"})


def skull_at(h, x, y, z, rot=0, kind="skeleton"):
    h.set(x, y, z, "minecraft:%s_skull" % kind, {"rotation": str(rot % 16)})


def wall_skull(w, lat, y, kind="skeleton"):
    w.set(lat, y, 0, "minecraft:%s_wall_skull" % kind, {"facing": w.into})


# ===================================================================================================================
# furniture
# ===================================================================================================================
def chair(f, r=0, u=0, ff=0, stairs=L.PLANKS_STAIRS, seat_marker=False, hint="chair"):
    """A stair chair at (r, u, ff) of frame f; the sitter faces the frame's forward. With seat_marker a `seat` rest anchor is written above it."""
    f.set(r, u, ff, stairs, {"facing": "@back", "half": "bottom"})
    if seat_marker:
        f.marker(r, u + 1, ff, "seat", facing=f.facing, **{"as": hint})


def table(f, length, width=1, u=0, stairs=L.PLANKS_STAIRS, slab=L.PLANKS_SLAB):
    """A table whose top is 1 block above the floor: top-half slabs with an upside-down stair as the leg board at each end. Runs `length` cells forward and
    `width` cells to the right from the frame's cell."""
    for k in range(length):
        for r in range(width):
            if length >= 2 and k == 0:
                f.set(r, u, k, stairs, {"facing": "@back", "half": "top"})
            elif length >= 2 and k == length - 1:
                f.set(r, u, k, stairs, {"facing": "@forward", "half": "top"})
            else:
                f.set(r, u, k, slab, {"type": "top"})


def bench(f, length, u=0, slab=L.PLANKS_SLAB):
    """A bench: bottom slabs `length` cells forward."""
    for k in range(length):
        f.set(0, u, k, slab, {"type": "bottom"})


def lectern(f, r=0, u=0, ff=0, book=True):
    """A lectern whose reader stands in front, looking along the frame's forward."""
    f.set(r, u, ff, "minecraft:lectern", {"facing": "@back", "has_book": book, "powered": False})


def bed(f, r=0, u=0, ff=0, color="red", marker=True):
    """A bed: foot at (r, u, ff), head one cell forward. A `bed` rest anchor goes above the foot."""
    f.set(r, u, ff, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "foot"})
    f.set(r, u, ff + 1, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "head"})
    if marker:
        f.marker(r, u + 1, ff, "bed", **{"as": "bed"})


def bunk(f, color="red", wood_post=L.LOG, chains=False, marker=True):
    """A bunk bed (two beds one above the other in a log frame) lying along the frame's forward, the lower bed at the floor, the upper on a slab deck two
    rows up. Four corner posts. Occupies 3 wide (r -1..1) and 4 long (ff -1..2)."""
    for r in (-1, 1):
        for ff in (-1, 2):
            for u in range(0, 4):
                f.set(r, u, ff, wood_post, {"axis": "y"})
    f.set(0, 0, 0, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "foot"})
    f.set(0, 0, 1, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "head"})
    for ff in (0, 1):
        f.set(0, 1, ff, L.PLANKS_SLAB, {"type": "top"})
    f.set(0, 2, 0, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "foot"})
    f.set(0, 2, 1, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "head"})
    for r in (-1, 1):
        f.set(r, 2, 0, L.TRAPDOOR, {"facing": "@right" if r > 0 else "@left", "half": "bottom", "open": True})
        f.set(r, 2, 1, L.TRAPDOOR, {"facing": "@right" if r > 0 else "@left", "half": "bottom", "open": True})
    if marker:
        f.marker(0, 3, 0, "bed", **{"as": "bed"})


def spear_rack(w, lat, width=3):
    """A rack of polearms against a wall: stacked lightning rods (a copper-tipped spear each, alternately two and three long) standing against the wall behind
    a fence rail held by two posts. Why: a lightning rod is a thin pole with a point, which is what a spear looks like, and it needs no custom block."""
    for k in range(width):
        w.set(lat + k, GROUND, 0, "minecraft:lightning_rod", {"facing": "up"})
        w.set(lat + k, GROUND + 1, 0, "minecraft:lightning_rod", {"facing": "up"})
        if k % 2 == 0:
            w.set(lat + k, GROUND + 2, 0, "minecraft:lightning_rod", {"facing": "up"})
    for lt in (lat, lat + width - 1):
        w.set(lt, GROUND, 1, L.FENCE)
        w.set(lt, GROUND + 1, 1, L.FENCE)
    for lt in range(lat + 1, lat + width - 1):
        w.set(lt, GROUND + 1, 1, L.FENCE)


def weapon_niche(w, lat, width=3, rows=3, lantern=True):
    """A weapon recess: a niche `width` wide and `rows` + 1 tall carved into the inner wall layer (arched by two upside-down stairs), polearms standing in it (lightning
    rods, alternately two and three cells long) and, hung in the arch, a lantern. Why: a spear rack needs a post and a rail in front of the wall (two cells of depth)
    that the declared aisles often do not leave; the recess keeps the whole rack inside the wall so it works anywhere along a wall and still reads as a rack."""
    h = w.hall
    cells = niche(w, lat, GROUND, GROUND + rows, width=width)
    half = width // 2
    for k, lt in enumerate(range(lat - half, lat - half + width)):
        for y in range(GROUND, GROUND + (rows - 1 if k % 2 == 0 else rows)):
            w.set(lt, y, -1, "minecraft:lightning_rod", {"facing": "up"})
    if lantern:
        w.set(lat, GROUND + rows, -1, L.LANTERN, {"hanging": True})
    return cells


def chain_hoist(h, x, z, y_top=None, drop=5, load="minecraft:iron_block"):
    """A chain hoist over a work place: a chain from the ceiling (or a beam) to an iron-bar hook, a block of iron hanging from the hook. A hanging stone block is
    safe in a template (it is not a falling block); an anvil would drop."""
    y_top = h.top if y_top is None else y_top
    for k in range(drop):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - drop, z, L.IRON_BARS)
    h.set(x, y_top - drop - 1, z, load)


# ===================================================================================================================
# the story: dirt, blood, bones and cobwebs
# ===================================================================================================================
def web(h, x, y, z):
    return h.try_set(x, y, z, "minecraft:cobweb")


def blood(h, cells, color="red"):
    """Patches of red carpet on the floor (and the odd red candle): blood without a custom block."""
    for (x, z) in cells:
        h.try_set(x, GROUND, z, "minecraft:%s_carpet" % color)


def bones(h, x, z, rot=0, big=False):
    """A small heap of bones: a bone block with a skull on top (and a second block when big)."""
    if h.free(x, GROUND, z):
        h.set(x, GROUND, z, "minecraft:bone_block", {"axis": "y"})
        h.try_set(x, GROUND + 1, z, "minecraft:skeleton_skull", {"rotation": str(rot % 16)})
    if big:
        for (dx, dz, ax) in ((1, 0, "x"), (0, 1, "z")):
            h.try_set(x + dx, GROUND, z + dz, "minecraft:bone_block", {"axis": ax})


# ===================================================================================================================
# set pieces shared by several rooms
# ===================================================================================================================
def drain(h, x, z, w=3, soul_light=True, lining=L.COBBLED):
    """A floor drain: iron trapdoors (top half, so flush with the floor) over a one-cell cavity lined with `lining`, with a soul lantern at the bottom so the
    grate glows from below. (x, z) is the min corner of the w x w grate. Why: the brief's drain made of trapdoors; the liner (bottom and a ring of
    cells) means nothing but dark stone is ever visible through the grate, so the shell stays closed."""
    for dz in range(-1, w + 1):
        for dx in range(-1, w + 1):
            if dx in (-1, w) or dz in (-1, w):
                h.set(x + dx, FLOOR - 1, z + dz, lining)
    for dz in range(w):
        for dx in range(w):
            h.set(x + dx, FLOOR - 2, z + dz, lining)
            h.carve(x + dx, FLOOR, z + dz)
            h.set(x + dx, FLOOR, z + dz, "minecraft:iron_trapdoor", {"facing": "north", "half": "top", "open": False})
    if soul_light:
        h.set(x + w // 2, FLOOR - 1, z + w // 2, L.SOUL_LANTERN, {"hanging": "false"})


def hanging_cage(h, x, z, y_bottom, w=3, d=3, hgt=3, y_top=None, skull=True):
    """A cage of iron bars hung from the ceiling on a chain. (x, z) is the min corner, y_bottom the lowest bar row; a skull sits on the floor bars."""
    y_top = h.top if y_top is None else y_top
    for dx in range(w):
        for dz in range(d):
            for dy in range(hgt):
                if dx in (0, w - 1) or dz in (0, d - 1) or dy in (0, hgt - 1):
                    h.set(x + dx, y_bottom + dy, z + dz, L.IRON_BARS)
    for yy in range(y_bottom + hgt, y_top + 1):
        h.set(x + w // 2, yy, z + d // 2, L.CHAIN_BLOCK, {"axis": "y"})
    if skull:
        h.set(x + w // 2, y_bottom + 1, z + d // 2, "minecraft:skeleton_skull", {"rotation": "5"})


def torture_chair(f, spikes=True):
    """A torture chair for a sitter looking along the frame's forward: a stair seat, a backrest of open iron trapdoors bristling with lightning-rod spikes,
    iron-bar arm restraints hung with chains, a skull on the headrest and (spikes) a dripstone point in front. The frame's cell is the seat; the back is
    one cell behind it. Why: the owner's 'torture chairs' from vanilla blocks only (see mega_lib.torture_chair); this one writes through the Hall rails."""
    f.set(0, 0, 0, L.PLANKS_STAIRS, {"facing": "@back", "half": "bottom"})
    f.set(-1, 0, 0, L.IRON_BARS)
    f.set(1, 0, 0, L.IRON_BARS)
    for u in (1, 2):
        f.set(0, u, -1, "minecraft:iron_trapdoor", {"facing": "@forward", "half": "bottom", "open": True})
        f.set(0, u, 0, "minecraft:lightning_rod", {"facing": "@forward"})
    rot = {"south": 0, "west": 4, "north": 8, "east": 12}[f.facing]
    f.set(0, 3, -1, "minecraft:skeleton_skull", {"rotation": str(rot)})
    for r in (-1, 1):
        f.set(r, 1, 0, L.CHAIN_BLOCK, {"axis": "y"})
        f.set(r, 2, 0, L.CHAIN_BLOCK, {"axis": "y"})
        f.set(r, 3, 0, L.IRON_BARS)
    if spikes:
        f.set(0, 0, 1, "minecraft:pointed_dripstone", {"vertical_direction": "up", "thickness": "tip"})


# ===================================================================================================================
# grime: wear, moss, vines, cobwebs and rubble, so nothing looks freshly built
# ===================================================================================================================
def brick_mix(h, p_cracked=0.12, p_cobbled=0.06, base=L.BRICK):
    """A wall block for hand-placed masonry: mostly `base`, sometimes cracked brick, sometimes cobbled stone (deterministic from the hall's random)."""
    r = h.rng.random()
    if r < p_cracked:
        return L.CRACKED_BRICK
    if r < p_cracked + p_cobbled:
        return L.COBBLED
    return base


def moss_patch(h, cx, cz, r, density=0.7, block="minecraft:moss_carpet"):
    """Moss carpet on the free floor within radius r of (cx, cz)."""
    for z in range(cz - r, cz + r + 1):
        for x in range(cx - r, cx + r + 1):
            if (x - cx) ** 2 + (z - cz) ** 2 <= r * r + 1 and h.inside(x, z) and h.rng.random() < density:
                h.try_set(x, GROUND, z, block)


def wall_vines(h, side, lats, rows=(GROUND + 1, GROUND + 2, GROUND + 3), p=0.6):
    """Vines clinging to a wall at the given laterals and rows (each cell with probability p)."""
    w = h.wall(side)
    for lat in lats:
        for y in rows:
            if h.rng.random() < p and w.backed(lat, y, 0):
                w.try_set(lat, y, 0, "minecraft:vine", {w.out: True})


def cobwebs(h, p=1.0):
    """Cobwebs in the four upper corners of the room (the cells touching two walls just under the ceiling)."""
    t = h.top
    for (x, z) in ((h.x0, h.z0), (h.x1, h.z0), (h.x0, h.z1), (h.x1, h.z1)):
        if h.rng.random() < p:
            h.try_set(x, t, z, "minecraft:cobweb")
            if h.rng.random() < 0.5:
                h.try_set(x, t - 1, z, "minecraft:cobweb")


def rubble(h, x, z, rows=2):
    """A small heap of broken masonry: cobbled blocks, stairs and slabs falling away from a point."""
    h.try_set(x, GROUND, z, L.COBBLED)
    for (dx, dz, face) in ((1, 0, "west"), (-1, 0, "east"), (0, 1, "north"), (0, -1, "south")):
        if h.rng.random() < 0.8:
            h.try_set(x + dx, GROUND, z + dz, L.COBBLED_STAIRS, {"facing": face, "half": "bottom"})
    if rows > 1:
        h.try_set(x, GROUND + 1, z, L.COBBLED_SLAB, {"type": "bottom"})
    for (dx, dz) in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        if h.rng.random() < 0.6:
            h.try_set(x + dx, GROUND, z + dz, L.COBBLED_SLAB, {"type": "bottom"})


# ===================================================================================================================
# torture furniture (shared by the cell blocks and the torture chambers)
# ===================================================================================================================
def stretching_rack(f, length=7):
    """A stretching rack, 3 wide and `length` long along the frame's forward: raised log side beams, a slab bed between them, a roller (a barrel lying
    across) at each end with a crank handle (an open trapdoor), ropes (chains) and a victim's skull. Why: the brief's rack from stairs, trapdoors and
    chains; the beams make it read as a frame and the rollers as the machine."""
    last = length - 1
    for k in range(length):
        for r in (0, 2):
            f.set(r, 0, k, L.STRIPPED_LOG, {"axis": "x" if f.facing in ("east", "west") else "z"})
        if 0 < k < last:
            f.set(1, 0, k, L.PLANKS_SLAB, {"type": "bottom"})
    for k in (0, last):
        f.set(1, 0, k, "minecraft:barrel", {"facing": "east" if f.facing in ("north", "south") else "north", "open": False})
    f.set(-1, 0, 0, L.TRAPDOOR, {"facing": "@left", "half": "bottom", "open": True})
    f.set(3, 0, last, L.TRAPDOOR, {"facing": "@right", "half": "bottom", "open": True})
    for k in (1, last - 1):
        f.set(1, 1, k, L.CHAIN_BLOCK, {"axis": "x" if f.facing in ("east", "west") else "z"})
    f.set(0, 1, length // 2, "minecraft:skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[f.facing])})


def hanging_bed(h, x, z, facing, y=GROUND + 1, color="red", y_top=None):
    """A bed hung from the ceiling on four chains with an iron-bar frame under it; (x, z) is the foot cell, the head is one cell `facing`-ward."""
    y_top = h.top if y_top is None else y_top
    f = Fr(h, x, y, z, facing)
    f.set(0, 0, 0, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "foot"})
    f.set(0, 0, 1, "minecraft:%s_bed" % color, {"facing": "@forward", "part": "head"})
    f.set(0, -1, 0, L.IRON_BARS)
    f.set(0, -1, 1, L.IRON_BARS)
    for r in (-1, 1):
        for ff in (0, 1):
            for yy in range(y, y_top + 1):
                h.set(*f.at(r, yy - y, ff), L.CHAIN_BLOCK, {"axis": "y"})


def iron_maiden(w, lat, rows=4):
    """An iron maiden on a wall: a cage of iron bars 3 wide and 2 deep with an open front and a skull on top, spiked inside with lightning rods pointing in from
    the back and the sides. (lat is the first of three laterals.) Why: the brief's 'iron maiden of iron bars + lightning-rod spikes'."""
    top = GROUND + rows - 1
    for lt in range(lat, lat + 3):
        for y in range(GROUND, top + 1):
            if lt == lat + 1 and y <= top - 1:
                pass
            else:
                w.set(lt, y, 1, L.IRON_BARS)
        w.set(lt, top, 0, L.IRON_BARS)
    for y in range(GROUND, top):
        w.set(lat + 1, y, 0, "minecraft:lightning_rod", {"facing": w.into})
        w.set(lat, y, 0, "minecraft:lightning_rod", {"facing": w.inc})
        w.set(lat + 2, y, 0, "minecraft:lightning_rod", {"facing": w.dec})
    w.set(lat + 1, top + 1, 1, "minecraft:wither_skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[w.into])})
    w.set(lat + 1, GROUND, 2, "minecraft:red_carpet")


def shackles(h, x, z, y_top=None, drop=3):
    """A chain hung from the ceiling or a beam with an iron-bar manacle at its foot."""
    y_top = h.top if y_top is None else y_top
    for k in range(drop):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - drop, z, L.IRON_BARS)


# ===================================================================================================================
# dressers: wall rhythm and floor clutter placed along whatever is still free (deterministic, never blocks a route)
# ===================================================================================================================
def dress_wall(h, side, items, lo=None, hi=None, step=(3, 5), start=None):
    """Walks along a wall and offers each position to the next item in `items` (callables `fn(w, lat) -> bool`, True when it placed something). An item
    checks that its cells are free, so the dresser fills only the gaps the furniture left. Why: hand-placing every banner and sconce of 22 rooms makes
    the walls uniform or leaves them bare; a rhythm of varied small items along the free stretches gives every wall the layered look of the references."""
    w = h.wall(side)
    lo = (h.x0 if side in ("n", "s") else h.z0) + 1 if lo is None else lo
    hi = (h.x1 if side in ("n", "s") else h.z1) - 1 if hi is None else hi
    lat = lo + (h.rng.randint(0, 2) if start is None else start)
    i = h.rng.randrange(len(items))
    placed = 0
    tries = 0
    while lat <= hi and tries < 400:
        tries += 1
        fn = items[i % len(items)]
        if fn(w, lat):
            placed += 1
            i += 1
            lat += h.rng.randint(*step)
        else:
            i += 1
            if i % len(items) == 0:
                lat += 1
    return placed


def it_sconce(soul=False):
    def fn(w, lat):
        if not w.all_free([(lat, GROUND + 2, 0), (lat, GROUND + 3, 0)]):
            return False
        sconce(w, lat, GROUND + 2, soul=soul)
        return True
    return fn


def it_wall_torch(soul=False, y=GROUND + 2):
    def fn(w, lat):
        if not (w.can(lat, y, 0) and w.backed(lat, y, 0)):
            return False
        wall_torch(w, lat, y, soul=soul)
        return True
    return fn


def it_banner(color="red", y=GROUND + 4, hang=2):
    def fn(w, lat):
        if not w.all_free([(lat, y - k, 0) for k in range(hang + 1)]):
            return False
        w.set(lat, y, 0, "minecraft:%s_wall_banner" % color, {"facing": w.into})
        return True
    return fn


def it_skull_sconce(kind="wither_skeleton", soul=True, y=GROUND + 2):
    def fn(w, lat):
        if not w.all_free([(lat, y, 0), (lat, y + 1, 0)]):
            return False
        skull_sconce(w, lat, y, kind, soul)
        return True
    return fn


def it_shackles(y=GROUND + 4):
    """A pair of manacles on the wall: an iron-bar anchor at row y with a chain hanging below it."""
    def fn(w, lat):
        if not w.all_free([(lat, y, 0), (lat, y - 1, 0), (lat, y - 2, 0), (lat + 2, y, 0), (lat + 2, y - 1, 0), (lat + 2, y - 2, 0)]):
            return False
        for lt in (lat, lat + 2):
            w.set(lt, y, 0, L.IRON_BARS)
            w.set(lt, y - 1, 0, L.CHAIN_BLOCK, {"axis": "y"})
            w.set(lt, y - 2, 0, L.CHAIN_BLOCK, {"axis": "y"})
        return True
    return fn


WALL_BLOCKS = (L.BRICK, L.CRACKED_BRICK, L.COBBLED, "minecraft:bone_block")


def it_candle_niche(color="red", y=GROUND + 3, skull=True, wall_blocks=WALL_BLOCKS):
    """A one-wide recess in the inner wall layer with a candle cluster on its sill and (optionally) a skull above it."""
    def fn(w, lat):
        h = w.hall
        cells = [w.cell(lat, y, -1), w.cell(lat, y + 1, -1)]
        for (x, yy, z) in cells:
            if (x, yy, z) not in h.shell or (x, yy, z) in h.mt.reserved:
                return False
        if not w.all_free([(lat, y, 0), (lat, y + 1, 0)]):
            return False
        # keep clear of doorway frames (they are polished jambs inside the wall layers)
        for dl in (-1, 0, 1):
            c = h.get(*w.cell(lat + dl, y, -1))
            if c is None or c.block not in wall_blocks:
                return False
        niche(w, lat, y, y + 1, width=1)
        candles_at(h, *w.cell(lat, y, -1), 2, color)
        if skull:
            h.set(*w.cell(lat, y + 1, -1), "minecraft:skeleton_wall_skull", {"facing": w.into})
        return True
    return fn


def it_shelf(stuff=("candle", "skull", "pot", "brew")):
    """A three-wide wall shelf: top slabs on the wall at row y with small things on it (candles, a skull, a flower pot, a brewing stand)."""
    def fn(w, lat):
        y = GROUND + 2
        if not w.all_free([(lat + k, yy, 0) for k in range(3) for yy in (y, y + 1)]):
            return False
        h = w.hall
        for k in range(3):
            w.set(lat + k, y, 0, L.PLANKS_SLAB, {"type": "top"})
        picks = list(stuff)
        for k in range(3):
            what = picks[h.rng.randrange(len(picks))]
            x, yy, z = w.cell(lat + k, y + 1, 0)
            if what == "candle":
                candles_at(h, x, yy, z, h.rng.randint(1, 3), "white")
            elif what == "skull":
                h.set(x, yy, z, "minecraft:skeleton_skull", {"rotation": str({"south": 0, "west": 4, "north": 8, "east": 12}[w.into])})
            elif what == "pot":
                h.set(x, yy, z, h.rng.choice(["minecraft:flower_pot", "minecraft:potted_dead_bush", "minecraft:potted_brown_mushroom", "minecraft:potted_fern"]))
            elif what == "brew":
                h.set(x, yy, z, "minecraft:brewing_stand", {"has_bottle_0": "true", "has_bottle_1": "false", "has_bottle_2": "true"})
        return True
    return fn


def it_hooks():
    """A plank board with tripwire hooks (keys, tools) on it."""
    def fn(w, lat):
        y = GROUND + 2
        cells = [(lat + k, yy, 0) for k in range(3) for yy in (y, y + 1)] + [(lat + k, yy, 1) for k in range(3) for yy in (y, y + 1)]
        if not w.all_free(cells):
            return False
        for k in range(3):
            for yy in (y, y + 1):
                w.set(lat + k, yy, 0, L.PLANKS)
                w.set(lat + k, yy, 1, "minecraft:tripwire_hook", {"facing": w.into, "attached": False, "powered": False})
        return True
    return fn


def it_lantern_bracket(soul=False, y=GROUND + 3):
    def fn(w, lat):
        if not w.all_free([(lat, y, 0), (lat, y - 1, 0)]):
            return False
        bracket_lantern(w, lat, y, arm=1, soul=soul)
        return True
    return fn


def it_slit(y0=GROUND + 3, y1=GROUND + 4):
    """A dark barred window slit in the wall."""
    def fn(w, lat):
        h = w.hall
        for y in range(y0, y1 + 1):
            for dl in (-1, 0, 1):
                c = h.get(*w.cell(lat + dl, y, -1))
                if c is None or c.block not in WALL_BLOCKS:
                    return False
            if not w.free(lat, y, 0):
                return False
        window_slit(w, lat, y0, y1)
        return True
    return fn


def clutter(h, kinds, count, margin=1, region=None, tries=200):
    """Scatters small clusters on the free floor, next to walls and furniture rather than in open space: kinds are callables `fn(h, x, z) -> bool`.
    A cluster is only placed on a cell with a neighbour that is solid (a wall or a prop) and that is not in a route volume."""
    placed = 0
    x1, z1, x2, z2 = region if region else (h.x0, h.z0, h.x1, h.z1)
    for _ in range(tries):
        if placed >= count:
            break
        x = h.rng.randint(x1, x2)
        z = h.rng.randint(z1, z2)
        if not h.free(x, GROUND, z):
            continue
        near = False
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            c = h.get(nx, GROUND, nz)
            if (c is not None and classify(c.block, c.props)[0] != "pass") or not h.inside(nx, nz):
                near = True
        if not near:
            continue
        fn = kinds[h.rng.randrange(len(kinds))]
        if fn(h, x, z):
            placed += 1
    return placed


def cl_barrels(h, x, z):
    if not h.can_set(x, GROUND, z, "minecraft:barrel"):
        return False
    h.set(x, GROUND, z, "minecraft:barrel", {"facing": h.rng.choice(["up", "up", "north", "east"]), "open": False})
    if h.rng.random() < 0.5 and h.can_set(x, GROUND + 1, z, "minecraft:barrel"):
        h.set(x, GROUND + 1, z, "minecraft:barrel", {"facing": "up", "open": False})
    return True


def cl_bones(h, x, z):
    if not h.can_set(x, GROUND, z, "minecraft:bone_block"):
        return False
    bones(h, x, z, rot=h.rng.randrange(16), big=h.rng.random() < 0.5)
    return True


def cl_rubble(h, x, z):
    if not h.can_set(x, GROUND, z, L.COBBLED):
        return False
    rubble(h, x, z, rows=1)
    return True


def cl_candles(h, x, z, color="white"):
    if not h.can_set(x, GROUND, z, "minecraft:candle"):
        return False
    candles_at(h, x, GROUND, z, h.rng.randint(1, 4), color)
    return True


# ===================================================================================================================
# crypt and grave furniture
# ===================================================================================================================
def sarcophagus(f, length=4, width=2, open_lid=False, banner=None):
    """A stone sarcophagus `width` x `length` (forward) at the frame's cell: a polished base, a lid of bottom slabs between a raised head stone and a raised foot stone
    (full blocks, so the skull and the candles standing on them rest flush: a block above a bottom slab would hang half a block above its top), a skull on the head
    stone. With open_lid the lid is shoved aside over its second half and a skeleton lies in the coffin (bone block and a skull). Why: the brief's sarcophagi from
    stone slabs, skulls and candles, in palette blocks so the Region Theme restyles them."""
    h = f.hall
    rot = {"south": 0, "west": 4, "north": 8, "east": 12}[OPP[f.facing]]
    for k in range(length):
        for r in range(width):
            f.set(r, 0, k, L.POLISHED)
    for k in range(length):
        for r in range(width):
            if open_lid and k >= length // 2:
                continue
            if k in (0, length - 1):
                f.set(r, 1, k, L.POLISHED)
            else:
                f.set(r, 1, k, L.POLISHED_SLAB, {"type": "bottom"})
    f.set(0, 2, 0, "minecraft:wither_skeleton_skull", {"rotation": str(rot)})
    if f.free(width - 1, 2, 0):
        candles_at(h, *f.at(width - 1, 2, 0), 2, "purple")
    if not open_lid:
        if f.free(0, 2, length - 1):
            candles_at(h, *f.at(0, 2, length - 1), 3, "white")
    else:
        f.set(0, 1, length // 2, L.POLISHED_SLAB, {"type": "bottom"})
        f.set(width - 1, 1, length - 1, "minecraft:bone_block", {"axis": "z" if f.facing in ("north", "south") else "x"})
        if f.free(width - 1, 2, length - 1):
            f.set(width - 1, 2, length - 1, "minecraft:skeleton_skull", {"rotation": str(rot)})


def candelabra(h, x, z, y=GROUND, n=3, color="white", post=L.POLISHED_WALL, soul=False):
    """A standing candelabra: a wall post with a cluster of lit candles on it (or a soul lantern when soul=True)."""
    h.set(x, y, z, post)
    if soul:
        h.set(x, y + 1, z, L.SOUL_LANTERN, {"hanging": False})
    else:
        candles_at(h, x, y + 1, z, n, color)


def loculi(h, side, lats, rows, skull_p=0.8, candle_p=0.3):
    """A columbarium: a grid of one-cell recesses in the inner wall layer, each holding a skull (and sometimes a candle). Skips cells that are not plain wall
    (doorway jambs, windows)."""
    w = h.wall(side)
    placed = 0
    for lat in lats:
        for y in rows:
            ok = True
            for dl in (-1, 0, 1):
                c = h.get(*w.cell(lat + dl, y, -1))
                if c is None or c.block not in WALL_BLOCKS:
                    ok = False
            if not ok or not w.free(lat, y, 0):
                continue
            w.carve(lat, y, -1)
            rot = {"south": 0, "west": 4, "north": 8, "east": 12}[w.into]
            if h.rng.random() < skull_p:
                w.set(lat, y, -1, "minecraft:skeleton_skull" if h.rng.random() < 0.8 else "minecraft:wither_skeleton_skull", {"rotation": str(rot)})
            elif h.rng.random() < candle_p:
                w.set(lat, y, -1, "minecraft:candle", {"candles": str(h.rng.randint(1, 3)), "lit": "true"})
            placed += 1
    return placed



def breaking_wheel(w, lat, y, radius_ring=True):
    """A breaking wheel mounted flat on a wall: a ring of open trapdoors (thin panels, flush with the wall), a barrel hub whose round face looks into the room
    and chain spokes. (lat, y) is the hub. Why: a wheel from vanilla blocks: open trapdoors are thin panels, a barrel's end is a circle."""
    ring = [(2, 0), (-2, 0), (0, 2), (0, -2), (2, 1), (2, -1), (-2, 1), (-2, -1), (1, 2), (-1, 2), (1, -2), (-1, -2)]
    ax = "x" if w.side in ("n", "s") else "z"
    for (dx, dy) in ring:
        w.set(lat + dx, y + dy, 0, L.TRAPDOOR, {"facing": w.into, "half": "bottom", "open": True})
    w.set(lat, y, 0, "minecraft:barrel", {"facing": w.into, "open": False})
    for (dx, dy) in ((1, 0), (-1, 0)):
        w.set(lat + dx, y, 0, L.CHAIN_BLOCK, {"axis": ax})
    for (dx, dy) in ((0, 1), (0, -1)):
        w.set(lat, y + dy, 0, L.CHAIN_BLOCK, {"axis": "y"})


def stocks(f, length=4):
    """Wooden stocks (a pillory): two log posts and two boards one block apart, forward `length` cells from the frame's cell."""
    last = length - 1
    for u in range(0, 3):
        f.set(0, u, 0, L.LOG, {"axis": "y"})
        f.set(0, u, last, L.LOG, {"axis": "y"})
    for k in range(1, last):
        f.set(0, 0, k, L.PLANKS_SLAB, {"type": "bottom"})
        f.set(0, 1, k, L.PLANKS_SLAB, {"type": "top"})


def branding_cauldron(h, x, z):
    """A lava cauldron with branding irons (lightning rods standing in a rack of two fences) beside it."""
    h.set(x, GROUND, z, "minecraft:lava_cauldron")
    h.set(x + 1, GROUND, z, L.FENCE)
    h.set(x + 2, GROUND, z, L.FENCE)
    h.set(x + 1, GROUND + 1, z, "minecraft:lightning_rod", {"facing": "up"})
    h.set(x + 2, GROUND + 1, z, "minecraft:lightning_rod", {"facing": "up"})


def bone_wall(h, side, rows=(GROUND, GROUND + 5), skip=()):
    """Faces the inner layer of a wall with bone blocks (a wall of the dead), rows y0..y1, leaving the doorway frames and any lat in `skip` alone."""
    w = h.wall(side)
    lo = h.x0 if side in ("n", "s") else h.z0
    hi = h.x1 if side in ("n", "s") else h.z1
    doors = [off for (sd, off) in h.doors if sd == side]
    for lat in range(lo, hi + 1):
        if lat in skip or any(abs(lat - off) <= 2 for off in doors):
            continue
        for y in range(rows[0], rows[1] + 1):
            c = h.get(*w.cell(lat, y, -1))
            if c is not None and c.block in (L.BRICK, L.CRACKED_BRICK, L.COBBLED):
                w.shell_set(lat, y, -1, "minecraft:bone_block", {"axis": h.rng.choice(["x", "y", "z"])})


def bone_pyramid(h, x1, z1, x2, z2, layers=3, skulls=True):
    """A stepped heap of bone blocks over [x1..x2] x [z1..z2]: each layer one cell smaller all round, a crowning skull."""
    for k in range(layers):
        a, b, c, d = x1 + k, z1 + k, x2 - k, z2 - k
        if a > c or b > d:
            break
        for z in range(b, d + 1):
            for x in range(a, c + 1):
                h.try_set(x, GROUND + k, z, "minecraft:bone_block", {"axis": h.rng.choice(["x", "y", "z"])})
    top_k = min(layers, (min(x2 - x1, z2 - z1) // 2) + 1) - 1
    cx, cz = (x1 + x2) // 2, (z1 + z2) // 2
    if skulls:
        h.try_set(cx, GROUND + top_k + 1, cz, "minecraft:wither_skeleton_skull", {"rotation": "0"})


def lantern_canopy(h, xs, zs, drops, soul=True, y_top=None, skip=()):
    """A canopy of lanterns on chains of varying length under the ceiling: one at every (x, z) of the grid whose cells are free and that is not in `skip`,
    the chain length taken in turn from `drops`. Returns how many were hung. Why: the owner's 'soul-lantern ceiling' of the ossuary, and a cheap way to
    light a whole hall without placing a lamp by hand."""
    y_top = h.top if y_top is None else y_top
    n = 0
    for i, z in enumerate(zs):
        for j, x in enumerate(xs):
            if (x, z) in skip:
                continue
            drop = drops[(i + 2 * j) % len(drops)]
            cells = [(x, y_top - k, z) for k in range(drop + 1)]
            if not all(h.can_set(cx, cy, cz, "minecraft:chain") for (cx, cy, cz) in cells):
                continue
            hang_lantern(h, x, z, drop, y_top=y_top, soul=soul)
            n += 1
    return n


def bone_stack(h, x, z, height=3, axis_flip=True):
    """A stack of bone blocks like firewood: alternating axes, `height` high."""
    for k in range(height):
        h.try_set(x, GROUND + k, z, "minecraft:bone_block", {"axis": ("x" if (k % 2 == 0) else "z") if axis_flip else "y"})


def auto_light(h, soul=False, threshold=5, max_lamps=80, radius=2, min_clear=3):
    """Hangs lanterns over the darkest walkable cells until every cell reachable from the doorways has block light >= `threshold` at eye level (or no free
    column is left). Each lamp is a chain from the ceiling (or from a beam under it) with a lantern at the lowest row that keeps `min_clear` rows over the
    floor, found on a free column within `radius` of the dark spot. Returns the number of lamps hung. Why: the Gloom clock of 1.36.0 punishes real darkness,
    so the halls must be lit along every walkable cell; the furniture lights the room by design and this pass only fills the holes, deterministically, so a
    prop moved in a later edit cannot silently create a dark corner."""
    t = h.mt.t
    starts = []
    for (side, off) in h.doors:
        w = Wall(h, side)
        x, y, z = w.cell(off, GROUND, 0)
        starts.append((x, z, 2 * FLOOR + 2))
    nodes = walk_nodes(t)
    seen = reach(t, nodes, starts)
    blocked = set()
    n = 0
    for _ in range(max_lamps):
        lv = light_levels(t)
        worst = None
        for (x, z, s) in seen:
            if (x, z) in blocked:
                continue
            ye = int(s / 2.0 + 1.62)
            v = lv.get((x, ye, z), 0)
            if v < threshold and (worst is None or v < worst[0]):
                worst = (v, x, z, s)
        if worst is None:
            break
        v, x, z, s = worst
        feet = s // 2 if s % 2 == 0 else (s - 1) // 2 + 1
        row_min = feet + min_clear
        done = False
        for rad in (radius, radius + 2):
            cands = sorted(((dx * dx + dz * dz, x + dx, z + dz) for dx in range(-rad, rad + 1) for dz in range(-rad, rad + 1)))
            for (_, cx, cz) in cands:
                if not h.inside(cx, cz):
                    continue
                for y_top in (h.top, h.top - 1):
                    above = h.get(cx, y_top + 1, cz)
                    if above is None or classify(above.block, above.props)[0] == "pass":
                        continue
                    for yl in range(row_min, y_top):
                        if all(h.can_set(cx, yy, cz, "minecraft:chain") for yy in range(yl, y_top + 1)):
                            for yy in range(yl + 1, y_top + 1):
                                h.set(cx, yy, cz, L.CHAIN_BLOCK, {"axis": "y"})
                            h.set(cx, yl, cz, L.SOUL_LANTERN if soul else L.LANTERN, {"hanging": "true"})
                            done = True
                            break
                    if done:
                        break
                if done:
                    break
            if done:
                break
        if done:
            n += 1
        else:
            blocked.add((x, z))
    return n


def _door_starts(h):
    out = []
    for (side, off) in h.doors:
        x, y, z = Wall(h, side).cell(off, GROUND, 0)
        out.append((x, z, 2 * FLOOR + 2))
    return out


def _wide_nodes(t, nodes):
    passable, _ = _passable_fn(t)
    wide = set()
    for (x, z), ss in nodes.items():
        for s in ss:
            ok = True
            for dx in (-1, 0, 1):
                for dz in (-1, 0, 1):
                    if not any(abs(s2 - s) <= 1 for s2 in nodes.get((x + dx, z + dz), ())):
                        ok = False
            if ok:
                wide.add((x, z, s))
    return wide


def auto_markers(h, spawns=5, patrols=2, spawn_weights=(2, 2, 1, 1, 1, 1, 1, 1), min_gap=6, door_gap=4, centre=True):
    """Places the encounter markers where the finished room allows them: `spawn` markers on wide, flat, reachable floor at least 2 cells from every wall and
    `door_gap` from every doorway, spread out by farthest-point sampling from the room centre; `patrol` markers on the wide cells closest to the middle of
    all doorway paths (the main route), `min_gap` apart. Why: the Encounter Director needs ambush spots that are really open and reachable and the Wandering
    Bosses need route nodes that are really on the way; computing them from the finished geometry (instead of by hand) keeps both true after any edit."""
    t = h.mt.t
    starts = _door_starts(h)
    nodes = walk_nodes(t)
    seen = reach(t, nodes, starts)
    wide = _wide_nodes(t, nodes) & seen
    dist_sum = collections.defaultdict(int)
    passable, _ = _passable_fn(t)
    for st in starts:
        d = {st: 0}
        dq = collections.deque([st])
        while dq:
            cur = dq.popleft()
            for n in _neighbors(nodes, passable, *cur):
                if n not in d:
                    d[n] = d[cur] + 1
                    dq.append(n)
        for n, dd in d.items():
            dist_sum[n] += dd
    door_cells = [(x, z) for (x, z, s) in starts]
    cand = []
    for (x, z, s) in wide:
        y = s // 2
        if not (x >= h.x0 + 2 and x <= h.x1 - 2 and z >= h.z0 + 2 and z <= h.z1 - 2):
            continue
        if s % 2 != 0 or not h.free(x, y, z) or h.get(x, y - 1, z) is None:
            continue
        if any(abs(x - dx) + abs(z - dz) < door_gap for (dx, dz) in door_cells):
            continue
        cand.append((x, y, z, s))
    cand.sort(key=lambda c: (c[0], c[2], c[1]))
    placed = []
    # patrol markers: closest to the middle of the door paths
    pcand = sorted(cand, key=lambda c: (dist_sum.get((c[0], c[2], c[3]), 10 ** 6), c[0], c[2]))
    patrol_cells = []
    for c in pcand:
        if len(patrol_cells) >= patrols:
            break
        if all(abs(c[0] - p[0]) + abs(c[2] - p[2]) >= min_gap for p in patrol_cells):
            patrol_cells.append(c)
    for c in patrol_cells:
        h.marker(c[0], c[1], c[2], "patrol")
    # spawn markers: farthest-point sampling
    cx, cz = h.cx, h.cz
    pool = [c for c in cand if c not in patrol_cells and h.free(c[0], c[1], c[2])]
    chosen = []
    while pool and len(chosen) < spawns:
        if not chosen:
            best = min(pool, key=lambda c: (abs(c[0] - cx) + abs(c[2] - cz), c[0], c[2]))
        else:
            best = max(pool, key=lambda c: (min(abs(c[0] - o[0]) + abs(c[2] - o[2]) for o in chosen + patrol_cells), -c[0], -c[2]))
            if min(abs(best[0] - o[0]) + abs(best[2] - o[2]) for o in chosen + patrol_cells) < min_gap // 2:
                break
        chosen.append(best)
        pool = [c for c in pool if abs(c[0] - best[0]) + abs(c[2] - best[2]) >= min_gap // 2]
    for i, c in enumerate(chosen):
        wt = spawn_weights[i] if i < len(spawn_weights) else 1
        if wt > 1:
            h.marker(c[0], c[1], c[2], "spawn", weight=wt)
        else:
            h.marker(c[0], c[1], c[2], "spawn")
    # the room centre: when the geometric centre is blocked or cramped (furniture, a dais, a tomb) the Director must not aim at it, so a `room_center` marker names
    # the open standing cell nearest to it. A room whose centre is open (or already holds a spawn/patrol marker) gets none.
    if centre:
        placed_cells = {(c[0], c[2]) for c in chosen} | {(c[0], c[2]) for c in patrol_cells}
        ok = (cx, cz) in placed_cells or any((cx, cz, s2) in wide for s2 in nodes.get((cx, cz), ()))
        if not ok:
            best = None
            for (x, z, s2) in wide:
                if s2 % 2 != 0 or not h.free(x, s2 // 2, z) or h.get(x, s2 // 2 - 1, z) is None:
                    continue
                key = (abs(x - cx) + abs(z - cz), x, z)
                if best is None or key < best[0]:
                    best = (key, x, s2 // 2, z)
            if best is not None:
                h.marker(best[1], best[2], best[3], "room_center")
    return len(chosen), len(patrol_cells)


# ===================================================================================================================
# two-storey construction: decks, stair flights, rails, shelves
# ===================================================================================================================
def deck(h, x1, z1, x2, z2, y, block=L.PLANKS):
    """A floor of `block` at row y over the rectangle, wherever the cell is still free (a pilaster passing through is left alone)."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            h.try_set(x, y, z, block)


def stair_flight(h, x, z, facing, steps, width=3, base=GROUND, stairs=L.POLISHED_STAIRS, rail_r=None, rail=L.FENCE):
    """A flight of stairs climbing in `facing` from the cell (x, z), first step at row `base`, `width` wide to the right of the climb, solid brick under every
    step, and (rail_r) a fence rail along the cell column r = rail_r on top of the steps. Why: a library's galleries must be reachable; a solid-filled stair
    flight is the simplest stair that cannot be walked under into a void and that the walk-graph check can verify."""
    fx, fz = DV[facing]
    rx, rz = DV[RIGHT[facing]]
    for k in range(steps):
        for r in range(width):
            cx = x + k * fx + r * rx
            cz = z + k * fz + r * rz
            h.set(cx, base + k, cz, stairs, {"facing": facing, "half": "bottom"})
            for yy in range(base, base + k):
                h.set(cx, yy, cz, brick_mix(h, 0.15, 0.1))
            if rail_r is not None and r == rail_r and k < steps - 1:
                h.try_set(cx, base + k + 1, cz, rail)


def fence_run(h, x1, z1, x2, z2, y, block=L.FENCE, skip=()):
    """A straight run of fence (or any block) between two cells at row y, leaving out the cells in `skip`."""
    for z in range(min(z1, z2), max(z1, z2) + 1):
        for x in range(min(x1, x2), max(x1, x2) + 1):
            if (x, z) not in skip:
                h.try_set(x, y, z, block)


#: Chiselled bookshelves are real vanilla blocks with a `facing` and six `slot_N_occupied` states, but the shared checker (`mega_lib.check_state`, pattern `chiseled_.*`)
#: only knows an `axis` for them and reports every one as an error. Until that table is fixed (one entry, see docs/labyrinth/mega_rooms1.md) the libraries use plain
#: bookshelves; flip this to bring the chiselled ones back (the kit writes and validates them itself).
CHISELED_SHELVES = False


def shelf_wall(w, lat1, lat2, y1, y2, chiseled=0.12, d=0):
    """Bookshelves over a stretch of wall (full bookshelf blocks, now and then a chiselled one when CHISELED_SHELVES) wherever the cells are free; returns how many
    were placed."""
    h = w.hall
    n = 0
    for lat in range(lat1, lat2 + 1):
        for y in range(y1, y2 + 1):
            blk = "minecraft:chiseled_bookshelf" if (CHISELED_SHELVES and h.rng.random() < chiseled) else "minecraft:bookshelf"
            props = {"facing": w.into} if blk.endswith("chiseled_bookshelf") else None
            if w.can(lat, y, d, blk, props):
                w.set(lat, y, d, blk, props)
                n += 1
    return n


def toppled_shelf(w, lat, y0=GROUND, width=3, rows=3):
    """A shelf that has come down: a gap `width` x `rows` in the shelving at the given laterals (cobwebs hang in the hole, so the shelf pass leaves it alone) and, on the floor
    in front, the fallen shelves as a short heap of bookshelf blocks with a broken plank or two. Call it BEFORE `shelf_wall`. Why: the brief's 'toppled shelf' story detail;
    it also breaks the long even wall of shelving so the reading hall does not look stamped out."""
    h = w.hall
    for lt in range(lat, lat + width):
        for y in range(y0, y0 + rows):
            if w.can(lt, y, 0, "minecraft:cobweb"):
                w.set(lt, y, 0, "minecraft:cobweb")
    for k, lt in enumerate(range(lat, lat + width)):
        if y0 != GROUND:
            break                      # a heap needs the floor under it: only a gap at floor level gets one
        if w.can(lt, y0, 1, "minecraft:bookshelf"):
            w.set(lt, y0, 1, "minecraft:bookshelf")
            if k % 2 == 0 and w.can(lt, y0 + 1, 1, "minecraft:bookshelf"):
                w.set(lt, y0 + 1, 1, "minecraft:bookshelf")
        if w.can(lt, y0, 2, L.PLANKS_STAIRS, {"facing": w.into, "half": "bottom"}):
            w.set(lt, y0, 2, L.PLANKS_STAIRS if k != 1 else L.PLANKS_SLAB, {"facing": w.into, "half": "bottom"} if k != 1 else {"type": "bottom"})


def ladder_run(w, lat, y1, y2, d=1):
    """A ladder up the face of a shelf (attached to the block at depth d-1) from row y1 to y2."""
    for y in range(y1, y2 + 1):
        if w.backed(lat, y, d):          # only where a full block (a shelf) is behind it: the game drops a ladder that has nothing to hang on
            w.try_set(lat, y, d, "minecraft:ladder", {"facing": w.into})


def ring_cells(cx, cz, r=2):
    """The cells of the square ring of radius r round (cx, cz), clockwise seen from above (north up), starting at the south-east corner."""
    out = []
    for x in range(cx + r, cx - r - 1, -1):
        out.append((x, cz + r))
    for z in range(cz + r - 1, cz - r - 1, -1):
        out.append((cx - r, z))
    for x in range(cx - r + 1, cx + r + 1):
        out.append((x, cz - r))
    for z in range(cz - r + 1, cz + r):
        out.append((cx + r, z))
    return out


def spiral_stairs(h, cx, cz, start, steps, base=GROUND, stairs=L.POLISHED_STAIRS, fill=True, r=2):
    """A spiral stair round a core centred on (cx, cz): `steps` stairs along the ring of radius r, clockwise, the first at the ring cell `start`, each one row
    higher than the one before and facing the next. Returns the cell after the last step (where a landing goes). Solid brick under every step when fill."""
    ring = ring_cells(cx, cz, r)
    i0 = ring.index(start)
    last_dir = None
    for k in range(steps):
        x, z = ring[(i0 + k) % len(ring)]
        nx, nz = ring[(i0 + k + 1) % len(ring)]
        d = {(1, 0): "east", (-1, 0): "west", (0, 1): "south", (0, -1): "north"}[(nx - x, nz - z)]
        h.set(x, base + k, z, stairs, {"facing": d, "half": "bottom"})
        if fill:
            for yy in range(base, base + k):
                h.set(x, yy, z, brick_mix(h, 0.15, 0.1))
        last_dir = d
    return ring[(i0 + steps) % len(ring)]


_ARMOR_SETS = {
    "iron": ("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots", "minecraft:iron_sword"),
    "chain": ("minecraft:chainmail_helmet", "minecraft:chainmail_chestplate", "minecraft:chainmail_leggings", "minecraft:chainmail_boots", "minecraft:stone_sword"),
    "gold": ("minecraft:golden_helmet", "minecraft:golden_chestplate", "minecraft:golden_leggings", "minecraft:golden_boots", "minecraft:golden_sword"),
    "diamond": ("minecraft:diamond_helmet", "minecraft:diamond_chestplate", "minecraft:diamond_leggings", "minecraft:diamond_boots", "minecraft:diamond_sword"),
    "netherite": ("minecraft:netherite_helmet", "minecraft:netherite_chestplate", "minecraft:netherite_leggings", "minecraft:netherite_boots", "minecraft:netherite_sword"),
    "leather": ("minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings", "minecraft:leather_boots", "minecraft:wooden_sword"),
    "skull": ("minecraft:wither_skeleton_skull", "minecraft:iron_chestplate", "minecraft:iron_leggings", None, "minecraft:stone_sword"),
}


def statue(h, x, z, facing="south", kit="iron", y=GROUND, weapon=None, pedestal=True):
    """An armoured statue: a polished plinth with a slab and an armor stand in a full set standing on it (the stand is an entity of the template, no custom
    block). `y` is the row of the plinth. Counts against the room's twelve armor stands."""
    helm, chest, legs, boots, sword = _ARMOR_SETS[kit]
    if pedestal:
        h.set(x, y, z, L.POLISHED)
        h.set(x, y + 1, z, L.POLISHED_SLAB, {"type": "bottom"})
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[facing]
    h.armor_stand(x, y + 1, z, yaw=yaw, head=helm, chest=chest, legs=legs, feet=boots, mainhand=weapon or sword, dy=0.5)


def dais(h, x1, z1, x2, z2, rows=1, top=L.POLISHED, step=L.POLISHED_STAIRS, corner=L.CHISELED, y=GROUND, inlay=None):
    """A raised platform over [x1..x2] x [z1..z2] (the outer ring of cells is the stepped edge: stairs climbing inward, corner blocks), `rows` high; the
    inner cells are solid `top` blocks, with `inlay` on every second cell of the top layer when given (a chequerboard). Returns the standing row on top."""
    for z in range(z1, z2 + 1):
        for x in range(x1, x2 + 1):
            edge_x = x in (x1, x2)
            edge_z = z in (z1, z2)
            for k in range(rows):
                if edge_x and edge_z:
                    h.set(x, y + k, z, corner)
                elif edge_x or edge_z:
                    if k == rows - 1:
                        facing = "east" if x == x1 else "west" if x == x2 else "south" if z == z1 else "north"
                        h.set(x, y + k, z, step, {"facing": facing, "half": "bottom"})
                    else:
                        h.set(x, y + k, z, top)
                else:
                    h.set(x, y + k, z, inlay if (inlay and k == rows - 1 and (x + z) % 2 == 0) else top)
    return y + rows


# ===================================================================================================================
# alchemy and kitchen furniture
# ===================================================================================================================
def herb_bundle(h, x, z, y_top=None, drop=2, kind="hanging_roots"):
    """A bunch of herbs drying from the ceiling or a beam: a chain `drop` long with hanging roots (or a spore blossom) at its foot."""
    y_top = h.top if y_top is None else y_top
    for k in range(drop - 1):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - (drop - 1), z, "minecraft:dried_kelp_block")
    h.set(x, y_top - drop, z, "minecraft:%s" % kind)


def glass_case(h, x, z, y=GROUND + 1, color="lime", skull=True):
    """A small display case on a table: a 1x1x2 column of stained-glass panes on a slab with a skull inside (a creature in a jar). `y` is the row of the
    slab (the table top surface is the row below)."""
    if skull:
        h.set(x, y, z, "minecraft:skeleton_skull", {"rotation": "6"})
    h.set(x, y + 1, z, "minecraft:%s_stained_glass_pane" % color)
    h.set(x, y + 2, z, "minecraft:%s_stained_glass_pane" % color)
    h.set(x, y + 3, z, "minecraft:%s_stained_glass" % color)


def pane_cage(h, cx, cz, color="lime", head="minecraft:creeper_head", y=GROUND, creature="minecraft:slime_block"):
    """A glass-pane cage 3 x 3 x 3 centred on (cx, cz): a ring of stained-glass panes three high, a glass cap, and inside a caged creature (a block of slime with a
    mob head on it) lit by a soul lantern hung from the cap. Why: the brief's glass-pane cages with creatures-as-skulls; the panes join round the ring by themselves, so
    the cage reads as one glass box, and the block of slime inside makes it a specimen, not an empty case."""
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            if dx == 0 and dz == 0:
                continue
            for k in range(3):
                h.set(cx + dx, y + k, cz + dz, "minecraft:%s_stained_glass_pane" % color)
    for dz in (-1, 0, 1):
        for dx in (-1, 0, 1):
            h.set(cx + dx, y + 3, cz + dz, "minecraft:%s_stained_glass" % color)
    h.set(cx, y, cz, creature)
    h.set(cx, y + 1, cz, head, {"rotation": "8"})
    h.set(cx, y + 2, cz, L.SOUL_LANTERN, {"hanging": "true"})


def alembic(h, x, z, y=GROUND, glass="lime", tube=4):
    """A still: a lit furnace, a cauldron of brew on it, a copper head, a glass tube rising to a copper cap, a hanging lantern beside. Vanilla blocks only."""
    h.furnace(x, y, z, "furnace", "south")
    h.set(x, y + 1, z, "minecraft:water_cauldron", {"level": 3})
    h.set(x, y + 2, z, "minecraft:waxed_copper_block")
    for k in range(tube):
        h.set(x, y + 3 + k, z, "minecraft:%s_stained_glass" % glass)
    h.set(x, y + 3 + tube, z, "minecraft:waxed_copper_block")


def chalk_circle(h, cx, cz, r=3, ring="minecraft:white_carpet", mark="minecraft:purple_carpet"):
    """A ritual circle of carpets on the floor: an outer ring at radius r, an inner ring at r-2 (when r >= 3), and the centre and the four cells next to it in
    `mark`. Carpets are walkable, so it never blocks a route."""
    for z in range(cz - r - 1, cz + r + 2):
        for x in range(cx - r - 1, cx + r + 2):
            d2 = (x - cx) ** 2 + (z - cz) ** 2
            if (r - 0.5) ** 2 <= d2 <= (r + 0.5) ** 2:
                h.try_set(x, GROUND, z, ring)
            elif r >= 3 and (r - 2.5) ** 2 <= d2 <= (r - 1.5) ** 2:
                h.try_set(x, GROUND, z, ring)
            elif d2 <= 1:
                h.try_set(x, GROUND, z, mark)


def glow_tank(h, x, z, liquid="lime", glow="minecraft:verdant_froglight", height=7, y=GROUND, base=L.POLISHED, lid="minecraft:waxed_copper_block"):
    """A glass tank 3 x 3 at (x..x+2, z..z+2): a plinth, a ring of clear glass, a column of coloured glass in the middle (the liquid) with a froglight (or sea
    lantern) at its foot that makes the whole tank glow, and a copper lid. Why: a reactor vessel from vanilla blocks without a single fluid: the colour comes
    from stained glass and the light from a froglight, so nothing can leak or flow."""
    for dz in range(3):
        for dx in range(3):
            h.set(x + dx, y, z + dz, base)
            h.set(x + dx, y + height + 1, z + dz, lid)
    for yy in range(y + 1, y + height + 1):
        for dz in range(3):
            for dx in range(3):
                centre = dx == 1 and dz == 1
                if centre:
                    blk = glow if yy == y + 1 else "minecraft:%s_stained_glass" % liquid
                    h.set(x + 1, yy, z + 1, blk)
                else:
                    h.set(x + dx, yy, z + dz, "minecraft:glass")


def armor_alcove(w, lat, kit="iron", width=3, rows=4, lantern=True):
    """An armour alcove: a recess `width` wide in the inner wall layer with a bevelled top and an armour stand standing in it facing the room, a lantern hung in
    the arch. Why: the brief's armoury displays; the niche gives the stand a frame and the wall a depth."""
    h = w.hall
    cells = niche(w, lat, GROUND, GROUND + rows - 1, width=width)
    helm, chest, legs, boots, sword = _ARMOR_SETS[kit]
    x, y, z = w.cell(lat, GROUND, -1)
    yaw = {"south": 0.0, "west": 90.0, "north": 180.0, "east": 270.0}[w.into]
    h.armor_stand(x, y, z, yaw=yaw, head=helm, chest=chest, legs=legs, feet=boots, mainhand=sword)
    if lantern:
        h.set(*w.cell(lat, GROUND + rows - 1, -1), L.LANTERN, {"hanging": True})
    return cells


def fireplace(w, lat, width=3, soul=False):
    """A fireplace against a wall: a cobbled hearthstone `width` wide, a campfire in the middle, a smoker and a cauldron either side, and a brick chimney breast
    narrowing to the ceiling. `lat` is the first of the laterals. Returns the cell of the campfire."""
    h = w.hall
    top = h.top
    mid = lat + width // 2
    for lt in range(lat, lat + width):
        for d in (0, 1):
            x, y, z = w.cell(lt, FLOOR, d)
            h.floor_set(x, z, L.COBBLED)
    w.set(mid, GROUND, 0, "minecraft:soul_campfire" if soul else "minecraft:campfire", {"facing": w.into, "lit": True, "signal_fire": False})
    h.furnace(*w.cell(lat, GROUND, 0), "smoker", w.into)
    w.set(lat + width - 1, GROUND, 0, "minecraft:cauldron")
    for k, y in enumerate(range(GROUND + 2, top + 1)):
        a, b = lat - 1 + k, lat + width - k
        if a > b:
            break
        for lt in range(a, b + 1):
            for d in (0, 1):
                w.try_set(lt, y, d, brick_mix(h, 0.12, 0.08))
    return w.cell(mid, GROUND, 0)


def idol(h, cx, z0, height=7):
    """A looming idol against the north wall: a stepped plinth, a body of polished stone, a chiselled head with two glowing eyes (shroomlight) and horns of
    lightning rods, arms of stairs reaching out, skulls at its feet. `cx` is the centre column, `z0` the first row from the wall. Vanilla blocks only."""
    for dz in range(0, 4):
        for dx in range(-3, 4):
            if dz < 3 or abs(dx) <= 2:
                if (abs(dx) <= 2 and dz < 3) or (dz == 3 and dx in (-2, 0, 2)):
                    h.set(cx + dx, GROUND, z0 + dz, L.POLISHED)      # the skull pedestals of the front row are full blocks so the skulls rest flush
                else:
                    h.set(cx + dx, GROUND, z0 + dz, L.POLISHED_SLAB, {"type": "bottom"})
    for dx in range(-2, 3):
        h.set(cx + dx, GROUND + 1, z0, L.POLISHED)
        h.set(cx + dx, GROUND + 1, z0 + 1, L.POLISHED_STAIRS, {"facing": "north", "half": "bottom"})
    for y in range(GROUND + 1, GROUND + height - 1):
        for dx in (-1, 0, 1):
            for dz in (0, 1):
                h.try_set(cx + dx, y, z0 + dz, L.POLISHED if y < GROUND + height - 3 else L.BRICK)
    head_y = GROUND + height - 3
    for dx in (-1, 0, 1):
        for dz in (0, 1):
            for y in (head_y, head_y + 1):
                h.try_set(cx + dx, y, z0 + dz, L.CHISELED)
    h.set(cx - 1, head_y + 1, z0 + 2, "minecraft:shroomlight")
    h.set(cx + 1, head_y + 1, z0 + 2, "minecraft:shroomlight")
    h.set(cx, head_y, z0 + 2, "minecraft:iron_bars")
    for dx in (-1, 1):
        for k in range(2):
            h.try_set(cx + dx, head_y + 2 + k, z0, "minecraft:lightning_rod", {"facing": "up"})
    for dx in (-2, 2):
        for k in range(3):
            h.try_set(cx + dx, GROUND + 2 + k, z0 + 1, L.POLISHED_STAIRS if k == 0 else L.POLISHED, {"facing": "south" if dx < 0 else "south", "half": "top"} if k == 0 else None)
    for dx, rot in ((-2, 4), (2, 12), (0, 0)):
        h.try_set(cx + dx, GROUND + 1, z0 + 3, "minecraft:wither_skeleton_skull", {"rotation": str(rot)})


def sacrificial_slab(h, x, z, facing="south"):
    """A stone slab for a ritual: a polished block bed with chiselled ends, chains and manacles hanging from bars above it, and candles at the four corners."""
    f = Fr(h, x, GROUND, z, facing)
    for r in (-1, 0, 1):
        for k in range(3):
            if r == 0 or k in (0, 2):
                f.set(r, 0, k, L.POLISHED)
            else:
                f.set(r, 0, k, L.POLISHED_SLAB, {"type": "bottom"})
    f.set(0, 1, 0, L.POLISHED_SLAB, {"type": "bottom"})
    for r in (-1, 1):
        for k in (0, 2):
            candles_at(h, *f.at(r, 1, k), 3, "red")

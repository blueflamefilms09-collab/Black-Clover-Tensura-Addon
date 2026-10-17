#!/usr/bin/env python3
"""
The "walk through it without a game" tools of the Landing and Safe Zone templates (1.54.0, docs/labyrinth/mega_safe.md): a verifier and a renderer.

    verify(mt, ...)   checks a FINISHED `mega_lib.MT` template and returns a Report (errors, warnings, numbers)
    render(mt, out, ...)   an isometric cutaway or a plan, with a richer colour table than `mega_preview.py` (dye colours, woods, lights, markers)

Why this exists: nothing can be run in the game here, and these rooms are where a player rests. A seat that cannot be reached, a Landing so dark
that the Gloom clock bites on arrival, a lantern hanging from nothing, a wall with a hole to the void: each is found here, at generation time,
instead of by the owner in game. The checks are deliberately conservative (4-neighbour movement, 2-high clearance, one block of step only onto
a stair, slab or bed, optimistic light through partial blocks plus a margin), so "passes" means "a person can walk it".

What is checked (see `verify`):
  shell      floor, ceiling and the outer wall layer are closed everywhere except the doorways (no void visible);
  doors      every doorway is clear 3 wide, 4 tall, and 3 deep inside the room, and every door is reachable from every other;
  anchors    every seat, bed, hearth, patrol point and the arrival cell can be reached on foot;
  light      the block light on every walkable floor cell is at least `min_light` (the Gloom clock counts 4 or less as dark);
  support    chains, hanging lanterns, wall torches, banners, ladders, floor candles, carpets ... have something to hang from or stand on;
  markers    every marker stands where `MarkerKind.java` and the Java reader expect it (seat above stairs, bed above a bed foot, hearth above a fire);
  counts     seats, hearths, beds, patrols, armour stands, entities, file size;
"""
import collections
import math
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402

FLOOR_ROW = L.FLOOR_ROW


def _p(block):
    return block.split(":", 1)[1] if ":" in block else block


# ------------------------------------------------------------------------------------------------------------------------ block classes
_PASS = re.compile(
    r"air|cave_air|void_air|light|structure_block|structure_void|jigsaw|.*_carpet|.*_banner|.*_wall_banner|.*_sign|.*_wall_sign|(soul_|redstone_)?(wall_)?torch|"
    r".*_button|lever|.*_pressure_plate|tripwire|tripwire_hook|string|ladder|vine|glow_lichen|sculk_vein|cobweb|hanging_roots|moss_carpet|short_grass|tall_grass|"
    r"fern|large_fern|dead_bush|.*_sapling|.*_roots|poppy|dandelion|azure_bluet|.*_tulip|cornflower|lily_of_the_valley|.*_mushroom|snow|rail|powered_rail|"
    r"detector_rail|activator_rail|fire|soul_fire|water|redstone_wire|spore_blossom|small_dripleaf|.*_wall_skull|.*_wall_head|.*_trapdoor")
_NOT_SUPPORT = re.compile(
    r".*_fence|.*_fence_gate|.*_wall|.*_pane|glass_pane|iron_bars|chain|(soul_)?lantern|(soul_)?campfire|(.*_)?candle|.*_skull|.*_head|flower_pot|potted_.*|"
    r"lightning_rod|.*_lightning_rod|end_rod|pointed_dripstone|bell|brewing_stand|.*_door|.*_trapdoor|cobweb|scaffolding|ladder|hopper|decorated_pot|"
    r"respawn_anchor|conduit|chorus_.*|sea_pickle|turtle_egg|.*_amethyst_bud|.*_amethyst_cluster|cake|lectern_not")
_HALF_STEP = re.compile(r".*_stairs|.*_slab|.*_bed")
_TRANSPARENT = re.compile(
    r"air|cave_air|void_air|light|.*glass|.*_pane|glass_pane|iron_bars|.*_stairs|.*_slab|.*_wall|.*_fence|.*_fence_gate|.*_trapdoor|.*_door|(soul_)?lantern|chain|"
    r".*_carpet|.*torch.*|.*_banner|.*_sign|.*_wall_sign|(.*_)?candle|(soul_)?campfire|flower_pot|potted_.*|.*_skull|.*_head|.*cauldron.*|.*_bed|chest|trapped_chest|"
    r"ender_chest|ladder|vine|cobweb|lectern|anvil|chipped_anvil|damaged_anvil|grindstone|brewing_stand|enchanting_table|bell|lightning_rod|.*_lightning_rod|"
    r"end_rod|pointed_dripstone|hanging_roots|moss_carpet|scaffolding|hopper|decorated_pot|composter|lever|.*_button|.*_pressure_plate|tripwire|tripwire_hook|"
    r"conduit|water|lava|string|glow_lichen|sculk_vein|structure_block|jigsaw|stonecutter|.*_amethyst_cluster|.*_amethyst_bud|fire|soul_fire|"
    r"short_grass|tall_grass|fern|large_fern|dead_bush|.*_sapling|spore_blossom|cave_vines|cave_vines_plant|.*_mushroom|snow")

_DYE = {"white": (230, 230, 230), "orange": (230, 130, 40), "magenta": (190, 70, 190), "light_blue": (100, 160, 220), "yellow": (230, 200, 50),
        "lime": (110, 200, 50), "pink": (230, 140, 170), "gray": (80, 80, 85), "light_gray": (150, 150, 150), "cyan": (40, 140, 150),
        "purple": (130, 60, 170), "blue": (50, 70, 170), "brown": (110, 70, 40), "green": (70, 100, 40), "red": (170, 40, 40), "black": (30, 30, 35)}
_WOOD = {"dark_oak": (70, 50, 28), "spruce": (110, 80, 50), "oak": (165, 130, 80), "birch": (205, 195, 140), "jungle": (150, 110, 75), "acacia": (170, 90, 50),
         "mangrove": (120, 50, 50), "cherry": (225, 165, 175), "bamboo": (200, 180, 70), "crimson": (125, 50, 70), "warped": (45, 115, 105)}


def color_of(block):
    n = _p(block)
    if n == "labyrinth_exit":
        return (255, 235, 150, 255)
    if n in ("structure_block", "jigsaw"):
        return (255, 0, 255, 255)
    if re.search(r"lava|magma_block|shroomlight|glowstone|campfire|^fire$|torch|lantern|sea_lantern|redstone_lamp|^light$|froglight|jack_o_lantern|end_rod", n):
        return (255, 170, 50, 255)
    if "stained_glass" in n or n.endswith("_pane"):
        for k, v in _DYE.items():
            if n.startswith(k + "_"):
                return v + (140,)
    if n in ("glass", "tinted_glass", "glass_pane"):
        return (170, 210, 225, 110)
    if "water" in n:
        return (60, 110, 200, 200)
    for k, v in sorted(_DYE.items(), key=lambda kv: -len(kv[0])):
        if n.startswith(k + "_") and re.search(r"wool|carpet|bed|banner|concrete|terracotta|candle|shulker", n):
            return v + (255,)
    for k, v in sorted(_WOOD.items(), key=lambda kv: -len(kv[0])):
        if n.startswith(k + "_") or n.startswith("stripped_" + k):
            return v + (255,)
    table = [(r"hay_block", (215, 185, 60)), (r"barrel|chest|crafting_table|fletching_table|smithing_table|cartography_table|loom|lectern|bookshelf|composter", (115, 85, 50)),
             (r"iron_bars|chain|anvil|grindstone|cauldron|.*furnace.*|smoker|hopper|iron_.*|stonecutter|bell", (150, 150, 158)),
             (r".*deepslate.*|.*tuff.*", (72, 74, 80)), (r".*blackstone.*|obsidian|crying_obsidian|basalt.*", (45, 38, 56)),
             (r".*nether_brick.*|netherrack|red_nether.*", (110, 40, 45)), (r".*quartz.*|calcite|.*diorite.*|snow.*|.*ice", (225, 225, 220)),
             (r".*stone_brick.*|stone.*|.*andesite.*|.*granite.*|cobblestone.*", (128, 128, 130)), (r".*copper.*", (190, 120, 90)),
             (r".*moss.*|.*leaves|.*azalea.*|.*fern|.*grass", (70, 120, 55)), (r".*amethyst.*|purpur.*|end_stone.*", (150, 120, 190)),
             (r".*gold.*|honey.*|.*sand.*", (215, 185, 70)), (r".*skull.*|.*head|bone_block|cobweb", (232, 228, 205)),
             (r"potted_.*|flower_pot|.*mushroom", (200, 90, 90)), (r"brewing_stand|enchanting_table", (130, 80, 160)), (r".*planks.*|.*log|.*fence.*|.*door", (110, 80, 50))]
    for pat, c in table:
        if re.fullmatch(pat, n):
            return c + (255,)
    return (140, 130, 120, 255)


def shade(c, f):
    return (min(255, int(c[0] * f)), min(255, int(c[1] * f)), min(255, int(c[2] * f)), c[3])


# ------------------------------------------------------------------------------------------------------------------------ the report
class Report:
    def __init__(self, name):
        self.name = name
        self.errors = []
        self.warns = []
        self.stats = collections.OrderedDict()

    def err(self, msg):
        self.errors.append(msg)

    def warn(self, msg):
        self.warns.append(msg)

    @property
    def ok(self):
        return not self.errors

    def text(self):
        out = ["%s: %s" % (self.name, "OK" if self.ok else "%d ERROR(S)" % len(self.errors))]
        for k, v in self.stats.items():
            out.append("   %-22s %s" % (k, v))
        out += ["   ERROR " + e for e in self.errors[:40]]
        if len(self.errors) > 40:
            out.append("   ... %d more errors" % (len(self.errors) - 40))
        out += ["   warn  " + w for w in self.warns[:25]]
        return "\n".join(out)


# ------------------------------------------------------------------------------------------------------------------------ the checker
class Grid:
    """Read-only view of a finished template for the checks."""

    def __init__(self, mt):
        self.mt = mt
        self.cells = mt.t.cells
        self.W, self.D, self.H = mt.W, mt.D, L.HEIGHT

    def block(self, x, y, z):
        c = self.cells.get((x, y, z))
        return None if c is None else _p(c.block)

    def cell(self, x, y, z):
        return self.cells.get((x, y, z))

    def in_box(self, x, y, z):
        return 0 <= x < self.W and 0 <= y < self.H and 0 <= z < self.D

    def passable(self, x, y, z):
        """Can a body occupy this cell (nothing in it, or something with no collision, or an open door / gate / trapdoor panel to the side)?"""
        c = self.cells.get((x, y, z))
        if c is None:
            return True
        n = _p(c.block)
        if n.endswith("_door") or n.endswith("_fence_gate"):
            return c.props.get("open") == "true"
        return bool(_PASS.fullmatch(n))

    def support(self, x, y, z):
        c = self.cells.get((x, y, z))
        if c is None:
            return False
        n = _p(c.block)
        if _PASS.fullmatch(n) and not n.endswith("_trapdoor"):
            return False
        if n.endswith("_trapdoor"):
            return False
        return not _NOT_SUPPORT.fullmatch(n)

    def half_step(self, x, y, z):
        c = self.cells.get((x, y, z))
        return c is not None and bool(_HALF_STEP.fullmatch(_p(c.block)))

    def ladder(self, x, y, z):
        return self.block(x, y, z) == "ladder"

    def standing(self, x, y, z):
        """Can a player stand with their feet in this cell?"""
        if not self.in_box(x, y, z) or not self.in_box(x, y + 1, z):
            return False
        if not self.passable(x, y, z) or not self.passable(x, y + 1, z):
            return False
        return self.support(x, y - 1, z) or self.ladder(x, y, z)

    def neighbours(self, pos):
        """Standing cells a walker can step to from `pos`: 4 directions, same level, one up onto a half block, down up to 3."""
        x, y, z = pos
        out = []
        for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            nx, nz = x + dx, z + dz
            if not (0 <= nx < self.W and 0 <= nz < self.D):
                continue
            # level
            if self.standing(nx, y, nz):
                out.append((nx, y, nz))
            # one up, onto a stair / slab / bed
            elif self.standing(nx, y + 1, nz) and self.half_step(nx, y, nz) and self.passable(x, y + 2, z):
                out.append((nx, y + 1, nz))
            else:
                for drop in (1, 2, 3):
                    if self.passable(nx, y, nz) and self.passable(nx, y + 1, nz) and self.standing(nx, y - drop, nz):
                        out.append((nx, y - drop, nz))
                        break
        # ladders
        if self.ladder(x, y, z):
            for ny in (y + 1, y - 1):
                if self.in_box(x, ny, z) and (self.ladder(x, ny, z) or self.standing(x, ny, z)):
                    out.append((x, ny, z))
        return out


def reach(grid, starts, limit=None):
    seen = set()
    dq = collections.deque()
    for s in starts:
        if grid.standing(*s):
            seen.add(s)
            dq.append(s)
    while dq:
        cur = dq.popleft()
        for n in grid.neighbours(cur):
            if n not in seen:
                seen.add(n)
                dq.append(n)
    return seen


# ---- block light
_EMIT = [
    (r"lantern", lambda c: 15), (r"soul_lantern", lambda c: 10), (r"torch|wall_torch", lambda c: 14), (r"soul_torch|soul_wall_torch", lambda c: 10),
    (r"redstone_torch|redstone_wall_torch", lambda c: 7),
    (r"campfire", lambda c: 15 if c.props.get("lit", "true") == "true" else 0), (r"soul_campfire", lambda c: 10 if c.props.get("lit", "true") == "true" else 0),
    (r"(.*_)?candle", lambda c: 3 * int(c.props.get("candles", "1")) if c.props.get("lit") == "true" else 0),
    (r"glowstone|sea_lantern|shroomlight|.*_froglight|jack_o_lantern|beacon|conduit|lava|lava_cauldron|fire", lambda c: 15), (r"soul_fire", lambda c: 10),
    (r"furnace|blast_furnace|smoker", lambda c: 0), (r"end_rod", lambda c: 14), (r"enchanting_table", lambda c: 7),   # lit furnaces are NOT counted as light: whether one with no fuel stays lit is the game's business, the rooms must be bright without them
    (r"magma_block", lambda c: 3), (r"crying_obsidian", lambda c: 10), (r"redstone_lamp", lambda c: 15 if c.props.get("lit") == "true" else 0),
    (r"light", lambda c: int(c.props.get("level", "15"))), (r"brewing_stand", lambda c: 1), (r"glow_lichen", lambda c: 7), (r"sculk_catalyst", lambda c: 6),
    (r"large_amethyst_bud", lambda c: 4), (r"medium_amethyst_bud", lambda c: 2), (r"amethyst_cluster", lambda c: 5), (r"small_amethyst_bud", lambda c: 1),
]
_EMIT_RE = [(re.compile(p), f) for p, f in _EMIT]


def emission(c):
    n = _p(c.block)
    best = 0
    for pat, f in _EMIT_RE:
        if pat.fullmatch(n):
            best = max(best, f(c))
    return best


def light_map(grid):
    """Block light of every cell (optimistic through partial blocks), by a multi-source flood: an emitter's neighbours get level-1 ..."""
    level = {}
    buckets = collections.defaultdict(list)
    for pos, c in grid.cells.items():
        e = emission(c)
        if e > 0:
            level[pos] = e
            buckets[e].append(pos)
    for lv in range(15, 1, -1):
        for pos in buckets.get(lv, []):
            if level.get(pos, 0) != lv:
                continue
            x, y, z = pos
            for dx, dy, dz in ((1, 0, 0), (-1, 0, 0), (0, 1, 0), (0, -1, 0), (0, 0, 1), (0, 0, -1)):
                np_ = (x + dx, y + dy, z + dz)
                if not grid.in_box(*np_):
                    continue
                c = grid.cells.get(np_)
                if c is not None and not _TRANSPARENT.fullmatch(_p(c.block)):
                    continue                                               # an opaque block holds no light and passes none
                if level.get(np_, 0) < lv - 1:
                    level[np_] = lv - 1
                    buckets[lv - 1].append(np_)
    return level


# Blocks that matter once placed: a later call that replaces one of these is almost always a mistake (the verifier lists it as a warning).
IMPORTANT = re.compile(
    r".*_bed|chest|barrel|campfire|soul_campfire|furnace|blast_furnace|smoker|lectern|anvil|brewing_stand|cauldron|lava_cauldron|water_cauldron|crafting_table|"
    r"smithing_table|fletching_table|cartography_table|loom|stonecutter|grindstone|enchanting_table|.*_wall_banner|lantern|soul_lantern|decorated_pot|bookshelf|"
    r"bell|.*_candle|candle|hay_block|.*_skull|.*_wall_skull|potted_.*|flower_pot|iron_trapdoor")


# ---- the checks
def verify(mt, expect=None, min_light=6, check_light=True):
    """Runs every check on a finished template; `expect` may carry: seats, hearths, beds (min counts), patrol (min, max), arrival (bool), gate (bool), doors
    (list of sides), entity_max, armor_max."""
    expect = expect or {}
    rep = Report(mt.name)
    grid = Grid(mt)
    top = getattr(mt, "top", FLOOR_ROW + getattr(mt, "h", 8))
    stand = FLOOR_ROW + 1
    W, D = mt.W, mt.D

    # ---- shell: floor, ceiling, outer wall layer
    holes = set()
    for side, off, kw in mt.sockets:
        for depth in range(L.WALL):
            for lat in range(off - 1, off + 2):
                for y in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                    holes.add(mt._lateral(side, off, lat, depth, y))
    missing_floor = [(x, z) for z in range(D) for x in range(W) if grid.cell(x, FLOOR_ROW, z) is None]
    if missing_floor:
        rep.err("floor row has %d open cells, e.g. %s" % (len(missing_floor), missing_floor[0]))
    for y in range(top + 1, top + 3):
        gap = [(x, z) for z in range(D) for x in range(W) if grid.cell(x, y, z) is None]
        if gap:
            rep.err("ceiling row %d has %d open cells, e.g. %s" % (y, len(gap), gap[0]))
            break
    void = []
    for y in range(FLOOR_ROW + 1, top + 1):
        for z in range(D):
            for x in range(W):
                outer = x == 0 or z == 0 or x == W - 1 or z == D - 1
                if outer and (x, y, z) not in holes and grid.cell(x, y, z) is None:
                    void.append((x, y, z))
                elif outer and (x, y, z) not in holes:
                    n = grid.block(x, y, z)
                    if n == "air":
                        void.append((x, y, z))
    if void:
        rep.err("the outer wall layer has %d open cells (void visible), e.g. %s" % (len(void), void[0]))

    # ---- doors: clear holes and approaches, and reachability
    door_inner = []
    for side, off, kw in mt.sockets:
        inner = mt._lateral(side, off, off, 2, stand)
        door_inner.append((side, off, inner))
        # approach: 3 wide, 4 tall, depth 2..4 inside
        blocked = []
        for depth in (2, 3, 4):
            for lat in range(off - 1, off + 2):
                for y in range(L.DOOR_ROW, L.DOOR_ROW + L.DOOR_H):
                    x, yy, z = mt._lateral(side, off, lat, depth, y)
                    if grid.in_box(x, yy, z) and not grid.passable(x, yy, z):
                        blocked.append((x, yy, z))
        if blocked:
            rep.err("the approach of the %s door at %d is blocked at %d cells, e.g. %s (%s)" % (side, off, len(blocked), blocked[0], grid.block(*blocked[0])))
    starts = [d[2] for d in door_inner]
    reachable = set()
    if starts:
        for i, s in enumerate(starts):
            if not grid.standing(*s):
                rep.err("the inside of the %s door at %d, %s, is not a standing cell (floor or clearance missing)" % (door_inner[i][0], door_inner[i][1], s))
        r0 = reach(grid, [starts[0]])
        reachable = r0
        for side, off, inner in door_inner[1:]:
            if inner not in r0:
                rep.err("the %s door at %d cannot be reached on foot from the %s door at %d" % (side, off, door_inner[0][0], door_inner[0][1]))
        # reverse direction too (drops are one-way)
        for side, off, inner in door_inner[1:]:
            if starts[0] not in reach(grid, [inner]):
                rep.err("the %s door at %d cannot be reached on foot from the %s door at %d" % (door_inner[0][0], door_inner[0][1], side, off))
    rep.stats["doors"] = ",".join("%s%d" % (s, o) for s, o, _ in door_inner)
    exp_doors = expect.get("doors")
    if exp_doors is not None and sorted(s for s, _, _ in door_inner) != sorted(exp_doors):
        rep.err("expected doors on %s, found %s" % (sorted(exp_doors), sorted(s for s, _, _ in door_inner)))

    # ---- markers
    markers = []
    for (x, y, z), c in mt.t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            meta = str(c.nbt.get("metadata", ""))
            parts = meta.split(";")
            args = {}
            for a in parts[1:]:
                if "=" in a:
                    k, v = a.split("=", 1)
                    args[k] = v
            markers.append((parts[0][6:], (x, y, z), args))
    for (x, y, z, kind) in getattr(mt, "intended", []):
        c = grid.cell(x, y, z)
        if c is None or c.block != "minecraft:structure_block":
            rep.err("the %s marker placed at %s was overwritten by %s" % (kind, (x, y, z), None if c is None else c.block))
    kinds = collections.Counter(m[0] for m in markers)
    rep.stats["markers"] = dict(sorted(kinds.items()))
    for forbidden in ("spawn", "faction", "trap", "pedestal", "guardian_spawn", "gatekeeper", "expansion_socket", "vault_center"):
        if kinds.get(forbidden):
            rep.err("a Landing or Safe Zone must not carry %s markers (it has %d)" % (forbidden, kinds[forbidden]))
    for kind, (x, y, z), args in markers:
        below = grid.cell(x, y - 1, z)
        bn = None if below is None else _p(below.block)
        if kind == "seat" and not (bn and bn.endswith("_stairs")):
            rep.err("seat marker at %s is not above a stair (it is above %s)" % ((x, y, z), bn))
        if kind == "seat" and "facing" not in args:
            rep.err("seat marker at %s has no facing" % ((x, y, z),))
        if kind == "hearth" and not (bn and re.fullmatch(r"(soul_)?campfire|furnace|blast_furnace|smoker|.*cauldron.*|fire|lava_cauldron", bn)):
            rep.err("hearth marker at %s is not above a fire (it is above %s)" % ((x, y, z), bn))
        if kind == "bed" and not (bn and bn.endswith("_bed") and below.props.get("part") == "foot"):
            rep.err("bed marker at %s is not above a bed FOOT (it is above %s %s)" % ((x, y, z), bn, None if below is None else below.props))
        if kind == "furniture" and bn in (None, "air"):
            rep.err("furniture marker at %s is above nothing" % ((x, y, z),))
        if kind in ("seat", "hearth", "bed", "furniture") and "as" not in args:
            rep.err("%s marker at %s has no as= hint" % (kind, (x, y, z)))
        if kind in ("arrival", "patrol"):
            if not grid.standing(x, y, z):
                rep.err("%s marker at %s is not a standing cell" % (kind, (x, y, z)))
            elif reachable and (x, y, z) not in reachable:
                rep.err("%s marker at %s cannot be reached on foot from the doors" % (kind, (x, y, z)))
    # anchors reachable: any reachable standing cell within 2 (seat, bed) or 3 (hearth) horizontally and 1 vertically
    def near_reachable(pos, rad):
        px, py, pz = pos
        for (rx, ry, rz) in reachable:
            if abs(rx - px) <= rad and abs(rz - pz) <= rad and abs(ry - py) <= 2:
                return True
        return False
    for kind, pos, args in markers:
        if kind in ("seat", "bed") and reachable and not near_reachable((pos[0], pos[1] - 1, pos[2]), 2):
            rep.err("%s at %s has no reachable standing cell next to it" % (kind, pos))
        if kind == "hearth" and reachable and not near_reachable((pos[0], pos[1] - 1, pos[2]), 3):
            rep.err("hearth at %s has no reachable standing cell within 3 blocks" % (pos,))
    n_seat, n_hearth, n_bed, n_patrol = kinds.get("seat", 0), kinds.get("hearth", 0), kinds.get("bed", 0), kinds.get("patrol", 0)
    if n_seat < expect.get("seats", 2):
        rep.err("needs at least %d seats, has %d" % (expect.get("seats", 2), n_seat))
    if n_hearth < expect.get("hearths", 1):
        rep.err("needs at least %d hearth, has %d" % (expect.get("hearths", 1), n_hearth))
    if not (expect.get("beds_min", 1) <= n_bed <= expect.get("beds_max", 3)):
        rep.err("needs %d..%d beds, has %d" % (expect.get("beds_min", 1), expect.get("beds_max", 3), n_bed))
    pmin, pmax = expect.get("patrol", (1, 2))
    if not (pmin <= n_patrol <= pmax):
        rep.err("needs %d..%d patrol markers, has %d" % (pmin, pmax, n_patrol))
    if expect.get("arrival"):
        if kinds.get("arrival", 0) != 1:
            rep.err("a Landing needs exactly one arrival marker, has %d" % kinds.get("arrival", 0))
        exits = [pos for pos, c in mt.t.cells.items() if c.block == "nusmp:labyrinth_exit"]
        if len(exits) != 9:
            rep.err("a Landing needs a 3x3 plane of labyrinth_exit blocks, has %d" % len(exits))
    elif kinds.get("arrival"):
        rep.err("only a Landing carries an arrival marker")

    # ---- support / attachment
    for (x, y, z), c in mt.t.cells.items():
        n = _p(c.block)
        above = grid.cell(x, y + 1, z)
        below = grid.cell(x, y - 1, z)
        if n == "chain" and c.props.get("axis", "y") == "y":
            if above is None or _p(above.block) == "air":
                rep.err("chain at %s hangs from nothing" % ((x, y, z),))
        elif n in ("lantern", "soul_lantern"):
            if c.props.get("hanging") == "true":
                if above is None or _p(above.block) == "air":
                    rep.err("hanging lantern at %s hangs from nothing" % ((x, y, z),))
            elif below is None or _p(below.block) == "air":
                rep.err("lantern at %s stands on nothing" % ((x, y, z),))
        elif re.fullmatch(r"(soul_|redstone_)?wall_torch|.*_wall_banner|.*_wall_sign|ladder|.*_wall_skull", n):
            f = c.props.get("facing")
            if f in STEP_VEC:
                dx, dz = STEP_VEC[f]
                behind = grid.cell(x - dx, y, z - dz)
                if behind is None or _p(behind.block) == "air":
                    rep.err("%s at %s has no wall behind it (facing %s)" % (n, (x, y, z), f))
        elif re.fullmatch(r"(soul_|redstone_)?torch|.*_carpet|(.*_)?candle|.*_skull|.*_pressure_plate|flower_pot|potted_.*|.*_banner|.*_sign", n) and not n.endswith("_wall_banner") and not n.endswith("_wall_sign") and not n.endswith("_wall_skull"):
            if below is None or _p(below.block) == "air" or (_p(below.block).endswith("_carpet") and not n.endswith("_carpet")):
                rep.err("%s at %s stands on nothing" % (n, (x, y, z)))
        elif re.fullmatch(r".*_bed|(soul_)?campfire|.*cauldron.*|brewing_stand|anvil|.*lectern.*", n):
            hung = n.endswith("cauldron") and above is not None and _p(above.block) == "chain"      # a cauldron on a chain is a hanging pot
            if (below is None or _p(below.block) == "air") and not hung:
                rep.err("%s at %s floats" % (n, (x, y, z)))
        elif n.endswith("_door") and c.props.get("half") == "upper":
            if below is None or not _p(below.block).endswith("_door"):
                rep.err("upper door half at %s has no lower half" % ((x, y, z),))

    # ---- light on the floor
    if check_light:
        lm = light_map(grid)
        floor_cells = []
        for z in range(L.WALL, D - L.WALL):
            for x in range(L.WALL, W - L.WALL):
                if grid.standing(x, stand, z) and grid.support(x, FLOOR_ROW, z):
                    floor_cells.append((x, z))
        levels = [lm.get((x, stand, z), 0) for x, z in floor_cells]
        if levels:
            dark = [(x, z) for (x, z), lv in zip(floor_cells, levels) if lv < min_light]
            rep.stats["light floor min/avg"] = "%d / %.1f over %d cells" % (min(levels), sum(levels) / len(levels), len(levels))
            if dark:
                rep.err("%d floor cells have block light below %d (the Gloom clock counts 4 or less as dark), e.g. %s" % (len(dark), min_light, dark[0]))
        for kind, (x, y, z), args in markers:
            if kind in ("seat", "bed", "hearth") and lm.get((x, y, z), 0) < min_light + 2:
                rep.err("%s at %s is too dark (%d)" % (kind, (x, y, z), lm.get((x, y, z), 0)))
        rep.light = lm

    # ---- blocks that mattered and were replaced by a later call
    for (cell, was, now) in getattr(mt, "overwrites", []):
        rep.warn("%s at %s was replaced by %s" % (_p(was), cell, _p(now)))

    # ---- counts
    stands = sum(1 for e in mt.t.entities if str(e["nbt"].get("id", "")) == "minecraft:armor_stand")
    rep.stats["blocks / entities"] = "%d / %d (%d armor stands)" % (len(mt.t.cells), len(mt.t.entities), stands)
    if stands > expect.get("armor_max", 12):
        rep.err("%d armor stands (the limit is %d)" % (stands, expect.get("armor_max", 12)))
    return rep


STEP_VEC = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


# ------------------------------------------------------------------------------------------------------------------------ the renderer
def _turned(t, turn):
    """The template's cells with the plan turned `turn` quarter turns clockwise (so every wall can be looked at from the south-east)."""
    sx, sy, sz = t.size
    cells = dict(t.cells)
    for _ in range(turn % 4):
        cells = {(sz - 1 - z, y, x): c for (x, y, z), c in cells.items()}
        sx, sz = sz, sx
    return cells, (sx, sy, sz)


def render(mt, out, xr=None, yr=None, zr=None, scale=8, top=False, light=None, turn=0, markers=True):
    """Isometric cutaway (from the south-east, the two near walls cut away with xr / zr) or, with top=True, a plan of the highest block in each column
    (markers drawn magenta). `light` = a light map from `verify` to draw a block-light heat map instead of colours. `turn` looks from another side."""
    from PIL import Image, ImageDraw
    t = mt.t
    all_cells, (sx, sy, sz) = _turned(t, turn)
    xr = xr or (0, sx - 1)
    yr = yr or (0, sy - 1)
    zr = zr or (0, sz - 1)
    cells = {p: c for p, c in all_cells.items() if xr[0] <= p[0] <= xr[1] and yr[0] <= p[1] <= yr[1] and zr[0] <= p[2] <= zr[1] and c.block != "minecraft:air"
             and (markers or c.block != "minecraft:structure_block")}
    if top:
        best = {}
        for (x, y, z), c in cells.items():
            if (x, z) not in best or y > best[(x, z)][0]:
                best[(x, z)] = (y, c)
        img = Image.new("RGBA", ((xr[1] - xr[0] + 1) * scale, (zr[1] - zr[0] + 1) * scale), (18, 18, 24, 255))
        d = ImageDraw.Draw(img)
        for (x, z), (y, c) in best.items():
            col = color_of(c.block)
            if light is not None:
                lv = light.get((x, FLOOR_ROW + 1, z), 0)
                col = (int(255 * min(1, lv / 15.0)), int(160 * min(1, lv / 15.0)), 40, 255)
            else:
                col = shade(col, 0.6 + 0.4 * min(1.0, max(0, y - FLOOR_ROW) / 12.0))
            d.rectangle([(x - xr[0]) * scale, (z - zr[0]) * scale, (x - xr[0] + 1) * scale - 1, (z - zr[0] + 1) * scale - 1], fill=col)
        img.save(out)
        return out
    tw, th, vh = 2 * scale, scale, scale
    xs = [(x - z) * tw // 2 for (x, y, z) in cells] or [0]
    ys = [(x + z) * th // 2 - y * vh for (x, y, z) in cells] or [0]
    ox, oy = -min(xs) + tw, -min(ys) + vh * 2
    img = Image.new("RGBA", (max(xs) - min(xs) + tw * 2 + 2, max(ys) - min(ys) + vh * 3 + th + 4), (18, 18, 24, 255))
    d = ImageDraw.Draw(img, "RGBA")
    for (x, y, z) in sorted(cells, key=lambda p: (p[0] + p[1] + p[2], p[1])):
        if all((x + dx, y + dy, z + dz) in cells for dx, dy, dz in ((1, 0, 0), (0, 0, 1), (0, 1, 0))):
            continue
        c = color_of(cells[(x, y, z)].block)
        px = (x - z) * tw // 2 + ox
        py = (x + z) * th // 2 - y * vh + oy
        top_pts = [(px, py - th // 2), (px + tw // 2, py), (px, py + th // 2), (px - tw // 2, py)]
        left_pts = [(px - tw // 2, py), (px, py + th // 2), (px, py + th // 2 + vh), (px - tw // 2, py + vh)]
        right_pts = [(px + tw // 2, py), (px, py + th // 2), (px, py + th // 2 + vh), (px + tw // 2, py + vh)]
        d.polygon(left_pts, fill=shade(c, 0.72))
        d.polygon(right_pts, fill=shade(c, 0.55))
        d.polygon(top_pts, fill=shade(c, 1.0))
    img.save(out)
    return out

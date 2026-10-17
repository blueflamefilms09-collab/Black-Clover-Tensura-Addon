#!/usr/bin/env python3
"""
The Overworld Gateway of the Mega-Dungeon (1.54.0, docs/labyrinth/mega.md section 1, docs/labyrinth/mega_arch.md): the ONE entrance of the dungeon, a ruined
citadel gate on the surface. Generates the template `structure/mega_gateway/gateway.nbt` and the data files that place it exactly once per world.

    python3 tools/labyrinth/gen_mega_gateway.py            # writes the template and the data files
    python3 tools/labyrinth/gen_mega_gateway.py --check    # regenerates in memory and compares with the files on disk
    python3 tools/labyrinth/gen_mega_gateway.py --ascii    # prints a few ASCII layers

What it is: a 39 x 39 circular platform of three terraces (each one step up) on a foundation that runs from the bottom of the box up to the paving
(buried on flat ground, a retaining skirt on a slope); a reinforced curb ring round it; four obelisks, four braziers, eight armoured statues on
plinths; and in the middle two stout pylons (six wide, seven deep) joined by a bridge over a nine-wide pointed arch whose opening is a plane of
`nusmp:labyrinth_gate` blocks, with rose windows and banners above it.

Why three kinds of hard block: the Overworld is not held by the Dungeon Guard (the Mega-Dungeon's dimension is), so nothing but the blocks themselves
protects the Gateway.
  1. The portal blocks (`nusmp:labyrinth_gate`) are indestructible by their own properties (labyrinth/gate/GateBlock, the same strength as bedrock).
  2. The cores of the pylons and of the towers, hidden behind a brick shell, are `minecraft:bedrock`: nobody mines bedrock in survival and no explosion
     breaks it, so a griefer who digs into a pylon reaches a wall he cannot pass. They are never visible (the cells behind the arrow slits are brick).
  3. Everything visible that touches the portal plane (the tunnel faces, the arch and the bridge) and the curb ring round the platform is
     `minecraft:reinforced_deepslate`: not obtainable in survival and, as far as I remember the block (not checked offline), a blast resistance of
     1200 like obsidian (no creeper, TNT, wither or crystal chain breaks it) but a hardness of 55 with NO tool requirement, which makes it mineable
     in survival by hand in about a minute and a half a block (it drops nothing). So the visible frame survives every explosion and stops casual
     damage, but it is not "unbreakable in survival play": a patient survivor can chew a hole in it. What he can never remove is the gate and the cores.
What is NOT protected on purpose: the terraces, steps, statues, banners and braziers (a ruined citadel may lose them). Creative players, commands and
mods that ignore hardness can remove anything.

Why a concentric-rings placement: that is the way vanilla says "exactly this many per world" (strongholds). With count 1 and distance 5 the one Gateway
is drawn 14 to 26 chunks (about 220 to 420 blocks) from the world origin. A fresh structure set (not an edit of `dungeon_gates`) keeps the old arches
untouched; they still lead into the Mega-Dungeon (config `mega.legacyGates`). The structure may START in any Overworld biome (so it always generates, as
a stronghold does), while the ring position is nudged to the nearest LAND biome within 112 blocks (`preferred_biomes`), so it is not drawn on the sea
floor when land is near. In a world whose chunks around that position were generated before this version, no Gateway appears; the old arches and
`/multiverse labyrinth enter` still work.

Why `start_height` -6 with `project_start_to_heightmap` WORLD_SURFACE_WG and a rigid pool element: the template's row 6 then lands on the surface block
(see gen_gate.py: the rigid ground-level delta is 1), so the six rows under it are foundation and everything above stands on the terrain. `beard_thin`
pulls the terrain up to the bottom of the box on a slope and carves hills out of the box, like a village.
"""
import json
import os
import random
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_arch_kit as K  # noqa: E402
import mega_arch_check as CK  # noqa: E402
from template import Template  # noqa: E402

W = 39            # footprint edge (odd: the arch is on the centre line)
C = 19            # centre cell
H = 46            # template height
S = 6             # row of the surface block (template row S lands on the terrain's surface block)
GATE = "nusmp:labyrinth_gate"
REINFORCED = "minecraft:reinforced_deepslate"       # the visible hard shell: the tunnel faces, the arch, the bridge, the curb ring
HIDDEN = "minecraft:bedrock"                        # the cores no one ever sees (inside the pylons and towers): unbreakable in survival
NAME = "mega_gateway/gateway"
DATA = os.path.join(L.RES, "data", "nusmp")
N, E, S_, Wd = "north", "east", "south", "west"
OPPOSITE = {N: S_, S_: N, E: Wd, Wd: E}

#: terraces: (outer radius, floor row). Seen from the ground: the curb, then the 1st court, a step up, the 2nd, a step up, the 3rd with the gate.
TIERS = [(18.5, 7), (15.5, 8), (11.5, 9)]
CURB_R = 18.5     # beyond this radius (to 19.5) the reinforced curb stands


class GateT:
    """The Gateway template under construction: the same safety net as `mega_lib.MT.set` (block states, no unknown custom blocks)."""
    armor_stand = L.MT.armor_stand
    entity = L.MT.entity

    def __init__(self):
        self.t = Template(NAME, W, H, W)
        self.name = NAME
        self.rng = random.Random("nusmp:mega:gateway")

    def set(self, x, y, z, block, props=None, nbt_=None):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < W):
            raise ValueError("gateway: (%d,%d,%d) is outside the template" % (x, y, z))
        if ":" not in block:
            block = "minecraft:" + block
        err = L.check_state(block, props)
        if err:
            raise ValueError("gateway: bad block state at (%d,%d,%d): %s" % (x, y, z, err))
        if block.startswith("nusmp:") and block != GATE:
            raise ValueError("gateway: %s is not allowed here (the gate block is the only custom block)" % block)
        self.t.set(x, y, z, block, props, nbt_)

    def get(self, x, y, z):
        return self.t.get(x, y, z)

    def remove(self, x, y, z):
        self.t.remove(x, y, z)


def r_of(x, z):
    return ((x - C) ** 2 + (z - C) ** 2) ** 0.5


def toward_centre(x, z):
    """The compass facing that points toward the middle (the high side of a stair that climbs toward it)."""
    dx, dz = C - x, C - z
    if abs(dx) >= abs(dz):
        return E if dx > 0 else Wd
    return S_ if dz > 0 else N


def tier_of(x, z):
    """Index of the highest terrace that holds the cell (-1: outside the platform)."""
    r = r_of(x, z)
    return max([i for i, (rm, _f) in enumerate(TIERS) if r <= rm] or [-1])


def floor_row(x, z):
    """Row of the paving at this cell (the terrace's floor row), or 6 outside the platform (the surface block)."""
    t = tier_of(x, z)
    return TIERS[t][1] if t >= 0 else S


def foundation_block(rng):
    return rng.choice((L.COBBLED, L.COBBLED, L.COBBLED, L.ROCK, L.BRICK, L.CRACKED_BRICK))


def floor_block(rng, tier, x, z):
    """The paving of a terrace: polished and tile in a loose chequer, cracks, and moss in the outer court."""
    r = rng.random()
    if tier == 0:
        if r < 0.10:
            return "minecraft:moss_block"
        if r < 0.28:
            return L.CRACKED_TILE
        return L.TILE if (x + z) % 2 else L.POLISHED
    if tier == 1:
        if r < 0.16:
            return L.CRACKED_TILE
        return L.TILE if (x + z) % 2 else L.POLISHED
    return L.POLISHED if r > 0.1 else L.CRACKED_BRICK


def build_platform(g):
    """Foundation, curb, three terraces, the steps between them and the four approach gaps."""
    rng = g.rng
    gap = lambda x, z: (abs(x - C) <= 2 and abs(z - C) > 12) or (abs(z - C) <= 2 and abs(x - C) > 12)  # noqa: E731
    for (x, z) in L.disk_cells(C, C, 19):
        if not (0 <= x < W and 0 <= z < W):
            continue
        r = r_of(x, z)
        tier = max([i for i, (rm, _f) in enumerate(TIERS) if r <= rm] or [-1])
        if r > CURB_R:
            # the curb ring: reinforced from the deepest row up to two rows above the first court, with four gaps for the approach stairs
            top = 8
            for y in range(0, top + 1):
                if y >= 7 and gap(x, z):
                    continue
                g.set(x, y, z, REINFORCED)
            if gap(x, z):
                g.set(x, 7, z, L.POLISHED_STAIRS, {"facing": toward_centre(x, z), "half": "bottom"})
            continue
        floor = TIERS[tier][1]
        for y in range(0, floor):
            g.set(x, y, z, foundation_block(rng))
        g.set(x, floor, z, floor_block(rng, tier, x, z))
        # the step rings: stairs one row above the lower terrace, on the ring of cells just outside the next terrace
        for k in (1, 2):
            rmax, frow = TIERS[k]
            if rmax < r <= rmax + 1.0 and tier == k - 1:
                g.set(x, frow, z, L.POLISHED_STAIRS, {"facing": toward_centre(x, z), "half": "bottom"})
    # a polished processional path along the gate's axis, from the north and south gaps to the dais, with chiselled studs
    for z in range(0, W):
        for x in range(C - 2, C + 3):
            r = r_of(x, z)
            if r > CURB_R or abs(z - C) < 10:
                continue
            tier = max([i for i, (rm, _f) in enumerate(TIERS) if r <= rm] or [-1])
            if tier < 0:
                continue
            cur = g.get(x, TIERS[tier][1], z)
            if cur is not None and "stairs" not in cur.block:
                g.set(x, TIERS[tier][1], z, L.CHISELED if (z % 3 == 0 and x == C) else L.POLISHED)
    # a ring of chiselled blocks and crying obsidian on the dais round the gate
    for (x, z) in L.ring_cells(C, C, 10, 1):
        if r_of(x, z) <= 11.5 and abs(z - C) > 4:
            g.set(x, 9, z, "minecraft:crying_obsidian" if (x + z) % 4 == 0 else L.CHISELED)


PY_Z0, PY_Z1 = 16, 22            # the pylons and the bridge are seven deep
PYLONS = ((9, 14), (24, 29))     # x ranges (six wide); the arch opening between them is x = 15..23
OPEN_X0, OPEN_X1 = 15, 23
SPRING = 20                      # first row of the arch ring (the jambs are rows 10..19)
BRIDGE_TOP = 32
TOWER_TOP = 40


def pylon(g, x0, x1, rng):
    """A pylon x0..x1 by z 16..22: a reinforced core, a brick shell with polished corner columns and chiselled bands up to row 32, a setback tower with
    a crenellated top above it, arrow slits, banners, brackets with hanging lanterns."""
    z0, z1 = PY_Z0, PY_Z1
    for y in range(10, BRIDGE_TOP + 1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                core = (x0 < x < x1) and (z0 < z < z1)
                corner = x in (x0, x1) and z in (z0, z1)
                if core:
                    g.set(x, y, z, HIDDEN)
                elif corner:
                    g.set(x, y, z, L.POLISHED)
                elif y in (10, 14, 20, 26, 32):
                    g.set(x, y, z, L.CHISELED if y != 10 else L.POLISHED)
                else:
                    g.set(x, y, z, L.CRACKED_BRICK if rng.random() < 0.14 else L.BRICK)
    # the tower on top: four by five, reinforced inside, brick outside, to row 39, merlons on row 40
    for y in range(BRIDGE_TOP + 1, TOWER_TOP):
        for x in range(x0 + 1, x1):
            for z in range(z0 + 1, z1):
                inner = (x0 + 1 < x < x1 - 1) and (z0 + 1 < z < z1 - 1)
                if inner:
                    g.set(x, y, z, HIDDEN if y < TOWER_TOP - 1 else L.POLISHED)        # the roof of the tower is polished stone, the bedrock stays inside
                else:
                    g.set(x, y, z, L.POLISHED if y in (BRIDGE_TOP + 4,) else L.BRICK)
    for x in range(x0 + 1, x1):
        for z in range(z0 + 1, z1):
            if x in (x0 + 1, x1 - 1) or z in (z0 + 1, z1 - 1):
                if (x + z) % 2 == 0:
                    g.set(x, TOWER_TOP, z, L.BRICK)
                    if x in (x0 + 1, x1 - 1) and z in (z0 + 1, z1 - 1):
                        g.set(x, TOWER_TOP + 1, z, L.POLISHED_WALL, {"up": "true"})
                        if x == x0 + 1 and z == z0 + 1:          # one beacon rod on the north-west merlon post of each tower
                            g.set(x, TOWER_TOP + 2, z, "minecraft:end_rod", {"facing": "up"})
    # the walkway ring round the tower's foot (row 32 top surface): a low parapet on its outer edge
    for x in range(x0, x1 + 1):
        for z in range(z0, z1 + 1):
            if (x in (x0, x1) or z in (z0, z1)) and (x + z) % 2 == 0:
                g.set(x, BRIDGE_TOP + 1, z, L.BRICK)
    # arrow slits on the faces toward the approach and on the outer faces
    xm = (x0 + x1) // 2
    for z, zin in ((z0, z0 + 1), (z1, z1 - 1)):
        for x in (xm, xm + 1):
            for y in (18, 19):
                g.set(x, y, z, L.IRON_BARS)
                g.set(x, y, zin, L.BRICK)                 # what shows through the slit is masonry, never the bedrock core
    # banners on the north and south faces, three or four wide
    for z, f in ((z0 - 1, N), (z1 + 1, S_)):
        for x, col in zip(range(x0 + 1, x1), ("red", "black", "black", "red")):
            g.set(x, 29, z, "minecraft:%s_wall_banner" % col, {"facing": f})
    # brackets with hanging lanterns on the faces, beside the opening
    inner_x = x1 if x0 < OPEN_X0 else x0
    for z, f in ((z0 - 1, N), (z1 + 1, S_)):
        for x in (inner_x, x0 if inner_x == x1 else x1):
            g.set(x, 24, z, L.POLISHED_STAIRS, {"facing": f, "half": "top"})
            K.chain(g, x, 23, z)
            K.chain(g, x, 22, z)
            K.lantern(g, x, 21, z, hanging=True, soul=(x == inner_x))


def arch_and_bridge(g, rng):
    """The pointed arch between the pylons (nine wide, x 15..23), the bridge above it up to row 32 with a parapet, a rose window on both faces and the
    portal plane in the middle of the tunnel."""
    n = OPEN_X1 - OPEN_X0 + 1
    faces = {}
    core_cells = {}

    def put_face(z):
        def put(u, y, block, props):
            faces[(OPEN_X0 + u, y, z)] = (block, props)
        return put

    def put_core(u, y, block, props):
        core_cells[(OPEN_X0 + u, y)] = (REINFORCED, None)
    for z in (PY_Z0, PY_Z1):
        K.pointed_arch(put_face(z), n, SPRING, BRIDGE_TOP, "polished", "brick", Wd, E)
    K.pointed_arch(put_core, n, SPRING, BRIDGE_TOP, "polished", "brick", Wd, E)
    for (x, y, z), (block, props) in faces.items():
        g.set(x, y, z, block, props)
    for z in range(PY_Z0 + 1, PY_Z1):
        for (x, y), (block, props) in core_cells.items():
            g.set(x, y, z, block, props)
    # the portal: every cell of the opening (rows 10..SPRING-1 full width, the pointed top above) at the middle layer of the tunnel
    for y in range(10, SPRING):
        for x in range(OPEN_X0, OPEN_X1 + 1):
            g.set(x, y, 19, GATE)
    for y in range(SPRING, BRIDGE_TOP + 1):
        for x in range(OPEN_X0, OPEN_X1 + 1):
            if (x, y) not in core_cells:
                g.set(x, y, 19, GATE)
    # glyph studs on the jambs (chiselled blocks in the inner faces of the pylons)
    for y in (12, 15, 18):
        for x in (OPEN_X0 - 1, OPEN_X1 + 1):
            for z in (PY_Z0, PY_Z1):
                g.set(x, y, z, L.CHISELED)
    # rose windows on both faces of the bridge: glass in the outer layer, a froglight behind each glass cell, a polished ring round it
    cx, cy = 19, 28
    for dx in range(-4, 5):
        for dy in range(-4, 5):
            d2 = dx * dx + dy * dy
            x, y = cx + dx, cy + dy
            if d2 <= 9:
                col = "red" if d2 <= 1 else ("purple" if d2 <= 4 else "cyan")
                for z, zi in ((PY_Z0, PY_Z0 + 1), (PY_Z1, PY_Z1 - 1)):
                    g.set(x, y, z, "minecraft:%s_stained_glass" % col)
                    g.set(x, y, zi, "minecraft:ochre_froglight", {"axis": "y"})
            elif d2 <= 14:
                for z in (PY_Z0, PY_Z1):
                    g.set(x, y, z, L.POLISHED if (dx + dy) % 2 else L.CHISELED)
    # a parapet on the bridge's front and back edges and a keystone glyph over the arch
    for x in range(OPEN_X0, OPEN_X1 + 1):
        for z in (PY_Z0, PY_Z1):
            if x % 2 == 1:
                g.set(x, BRIDGE_TOP + 1, z, L.BRICK)
    # chains and soul lanterns hanging in front of the arch's crown
    for z, f in ((PY_Z0 - 1, N), (PY_Z1 + 1, S_)):
        for x in (17, 21):
            g.set(x, SPRING + 5, z, L.POLISHED_STAIRS, {"facing": f, "half": "top"})
            K.chain(g, x, SPRING + 4, z)
            K.chain(g, x, SPRING + 3, z)
            K.lantern(g, x, SPRING + 2, z, hanging=True, soul=True)


def obelisk(g, x, z, rng):
    """A 14-tall obelisk on the outer court: a stepped base, a polished shaft with chiselled bands, a stair pyramid, a crying obsidian tip with an end rod."""
    base = floor_row(x, z) + 1
    for dx in (-1, 0, 1):
        for dz in (-1, 0, 1):
            g.set(x + dx, base, z + dz, L.POLISHED)
    for dx, dz, f in ((0, -2, S_), (0, 2, N), (-2, 0, E), (2, 0, Wd)):
        g.set(x + dx, base, z + dz, L.POLISHED_STAIRS, {"facing": f, "half": "bottom"})
    for y in range(base + 1, base + 12):
        g.set(x, y, z, L.CHISELED if y in (base + 3, base + 7) else L.POLISHED)
    for dx, dz, f in ((0, -1, S_), (0, 1, N), (-1, 0, E), (1, 0, Wd)):
        g.set(x + dx, base + 1, z + dz, L.POLISHED)
        g.set(x + dx, base + 2, z + dz, L.POLISHED_STAIRS, {"facing": f, "half": "bottom"})
    for dx, dz, f in ((0, -1, S_), (0, 1, N), (-1, 0, E), (1, 0, Wd)):
        g.set(x + dx, base + 12, z + dz, L.POLISHED_STAIRS, {"facing": f, "half": "bottom"})
    g.set(x, base + 12, z, L.CHISELED)
    g.set(x, base + 13, z, "minecraft:crying_obsidian")
    g.set(x, base + 14, z, "minecraft:end_rod", {"facing": "up"})


def statues_and_braziers(g, rng):
    kits = ["netherite", "iron", "diamond", "golden", "iron", "netherite", "golden", "diamond"]
    spots = [(11, 14, N), (27, 14, N), (11, 24, S_), (27, 24, S_),        # at the feet of the pylons (third court)
             (16, 7, S_), (22, 7, S_), (16, 31, N), (22, 31, N)]            # guards beside the processional path (second court)
    for (x, z, f), kit in zip(spots, kits):
        K.statue(g, x, floor_row(x, z) + 1, z, f, kit=kit)
    for (x, z) in ((14, 4), (24, 4), (14, 34), (24, 34)):
        K.small_brazier(g, x, z, soul=((x + z) % 4 == 0), y0=floor_row(x, z) + 1)


def weathering(g, rng):
    """Moss carpet and vines, rubble at the pylon feet, bites out of the merlons: a ruin, not a showroom."""
    for _ in range(110):
        x, z = rng.randrange(0, W), rng.randrange(0, W)
        t = tier_of(x, z)
        if t < 0 or r_of(x, z) > CURB_R:
            continue
        fr = TIERS[t][1]
        cur = g.get(x, fr, z)
        if g.get(x, fr + 1, z) is None and cur is not None and "stairs" not in cur.block:
            g.set(x, fr + 1, z, "minecraft:moss_carpet")
    # rubble heaps at the pylon corners on the third court
    for (x, z, f) in ((8, 16, Wd), (30, 22, E), (8, 22, Wd), (30, 16, E)):
        fr = floor_row(x, z)
        if g.get(x, fr + 1, z) is None and tier_of(x, z) >= 0:
            g.set(x, fr + 1, z, L.COBBLED_SLAB, {"type": "bottom"})
            xx = x + (1 if f == Wd else -1)
            if g.get(xx, fr + 1, z) is None:
                g.set(xx, fr + 1, z, L.COBBLED_STAIRS, {"facing": OPPOSITE[f], "half": "bottom"})
    # vines hanging on the north faces of the pylons and the bridge
    for x in list(range(9, 15)) + list(range(24, 30)):
        for y in range(rng.randrange(24, 30), 31):
            if g.get(x, y, PY_Z0 - 1) is None and rng.random() < 0.55:
                g.set(x, y, PY_Z0 - 1, "minecraft:vine", {"up": "false", "north": "false", "east": "false", "south": "true", "west": "false"})
    # bites out of the crenellation
    for _ in range(8):
        x, z = rng.choice([10, 13, 25, 28]), rng.choice([17, 21])
        if g.get(x, TOWER_TOP, z) is not None:
            g.remove(x, TOWER_TOP, z)
            for y in (TOWER_TOP + 1, TOWER_TOP + 2):               # whatever stood on the merlon (a wall post, a beacon rod) goes with it
                if g.get(x, y, z) is not None:
                    g.remove(x, y, z)


def build():
    g = GateT()
    rng = g.rng
    build_platform(g)
    for (x0, x1) in PYLONS:
        pylon(g, x0, x1, rng)
    arch_and_bridge(g, rng)
    for (x, z) in ((7, 7), (31, 7), (7, 31), (31, 31)):
        obelisk(g, x, z, rng)
    statues_and_braziers(g, rng)
    weathering(g, rng)
    L.autoconnect(g.t)
    return g.t


# ---------------------------------------------------------------------------------------------------------------------
# the data files that place it
# ---------------------------------------------------------------------------------------------------------------------
def data_files():
    """{relative path under data/nusmp: json text}"""
    def js(o):
        return json.dumps(o, indent=2) + "\n"
    return {
        "worldgen/template_pool/mega_gateway/base.json": js({
            "fallback": "minecraft:empty",
            "elements": [{"weight": 1, "element": {"element_type": "minecraft:single_pool_element", "location": "nusmp:mega_gateway/gateway",
                                                   "processors": "nusmp:gate_weathering", "projection": "rigid"}}]}),
        "worldgen/structure/mega_gateway.json": js({
            "type": "minecraft:jigsaw", "biomes": "#nusmp:has_structure/mega_gateway", "step": "surface_structures", "spawn_overrides": {},
            "terrain_adaptation": "beard_thin", "start_pool": "nusmp:mega_gateway/base", "size": 1, "start_height": {"absolute": -S},
            "project_start_to_heightmap": "WORLD_SURFACE_WG", "max_distance_from_center": 48, "use_expansion_hack": False}),
        "worldgen/structure_set/mega_gateway.json": js({
            "structures": [{"structure": "nusmp:mega_gateway", "weight": 1}],
            "placement": {"type": "minecraft:concentric_rings", "distance": 5, "spread": 1, "count": 1,
                          "preferred_biomes": "#nusmp:has_structure/mega_gateway_preferred", "salt": 1540154}}),
        "tags/worldgen/structure/mega_gateway.json": js({"replace": False, "values": ["nusmp:mega_gateway"]}),
        "tags/worldgen/biome/has_structure/mega_gateway.json": js({"replace": False, "values": ["#minecraft:is_overworld"]}),
        "tags/worldgen/biome/has_structure/mega_gateway_preferred.json": js({"replace": False, "values": [
            "#minecraft:is_forest", "#minecraft:is_taiga", "#minecraft:is_mountain", "#minecraft:is_hill", "#minecraft:is_jungle", "#minecraft:is_savanna",
            "#minecraft:is_badlands", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:desert",
            "minecraft:swamp", "minecraft:mangrove_swamp", "minecraft:meadow", "minecraft:cherry_grove"]}),
    }


def dungeon_gate_tag():
    """The existing tag with the Gateway appended (one added value; nothing removed)."""
    path = os.path.join(DATA, "tags", "worldgen", "structure", "dungeon_gate.json")
    doc = json.load(open(path))
    if "nusmp:mega_gateway" not in doc["values"]:
        doc["values"].append("nusmp:mega_gateway")
    return path, json.dumps(doc, indent=2) + "\n"


def main(argv):
    t = build()
    defects = CK.problems(t, NAME)
    if defects:                                           # the same static checks as the circulation pieces: nothing floats, nothing hangs on air
        print("\n".join(defects))
        print("%d defect(s): nothing written" % len(defects))
        return 1
    import nbt
    path = t.path(L.RES)
    files = dict(data_files())
    gate_tag_path, gate_tag_text = dungeon_gate_tag()
    if "--ascii" in argv:
        print(t.ascii_layers())
        return 0
    if "--check" in argv:
        bad = 0
        have = nbt.dumps(nbt.loads(open(path, "rb").read())[1]) if os.path.exists(path) else None
        if have != nbt.dumps(t.to_nbt()):
            bad += 1
            print("STALE %s" % NAME)
        for rel, text in files.items():
            p = os.path.join(DATA, rel)
            if not os.path.exists(p) or open(p).read() != text:
                bad += 1
                print("STALE %s" % rel)
        if not os.path.exists(gate_tag_path) or open(gate_tag_path).read() != gate_tag_text:
            bad += 1
            print("STALE tags/worldgen/structure/dungeon_gate.json")
        print("%d stale" % bad)
        return 1 if bad else 0
    t.save(L.RES)
    print("wrote %s  %dx%dx%d  %d blocks, %d entities" % (os.path.relpath(path, L.RES), t.size[0], t.size[1], t.size[2], len(t.cells), len(t.entities)))
    for rel, text in files.items():
        p = os.path.join(DATA, rel)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        open(p, "w").write(text)
        print("wrote data/nusmp/" + rel)
    open(gate_tag_path, "w").write(gate_tag_text)
    print("wrote data/nusmp/tags/worldgen/structure/dungeon_gate.json (appended nusmp:mega_gateway)")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

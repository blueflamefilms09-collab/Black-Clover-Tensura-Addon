#!/usr/bin/env python3
"""
The plaza of Floor 0, the Sanctuary Hub (1.55.0, docs/labyrinth/mega_hub.md): one Structure Template, `data/nusmp/structure/hub/plaza.nbt`,
generated from this script so the numbers cannot drift from the Java that places it.

    python3 tools/labyrinth/gen_mega_hub.py                 # (re)write plaza.nbt
    python3 tools/labyrinth/gen_mega_hub.py --check         # rebuild in memory, verify the file on disk and every rule below
    python3 tools/labyrinth/gen_mega_hub.py --ascii         # the layers as text
    python3 tools/labyrinth/gen_mega_hub.py --preview out   # out_top.png (plan) and out_iso0..3.png (3D views from the four sides)

Why a template and not blocks written by the chunk generator: the plaza is AUTHORED (a fountain, stalls, signposts, an arch round the way home).
A noise field cannot draw those, a template can, and a template is stamped once, by `HubPlaza`, when the first player arrives, so the terrain
generator stays a pure function of the seed. Why a script and not a structure block in game: the template must put its home gate exactly on
`HubLayout.HOME_GATE_*` and be exactly `HubPlazaLayout.SIZE_*` big; this script READS those two Java files (it parses their integer constants)
instead of keeping a second copy, and `--check` fails when the file on disk is not what the script would write. Deterministic bytes: every random
choice comes from one seeded generator and the NBT writer sorts its cells, so two runs write the same file.

The template lives under `structure/hub/`, NOT under `structure/mega/`, so the Mega-Dungeon checker (`mega_check.py`, grid and jigsaw rules) ignores it.
Vanilla Block Primacy: the only mod block is `nusmp:labyrinth_gate`, the plane that leads home. Everything else is a vanilla block, none of them
harmful to a passive animal (no fire, lava, cactus, berry bush, cobweb, campfire, powder snow), and nothing is a hole a player could fall into.
Frame: plaza coordinates (px, pz) have the middle of the plaza at (0, 0), x to the east, z to the south (the way the world's axes run); `row` is the
template row (FLOOR is the paving, STAND = FLOOR + 1 is where feet stand). Every part of the build is a `feature`; a feature may overwrite the
ground (paving, dirt, cleared air) but never another feature's cell, so two parts that collide fail loudly here instead of looking odd in game.
"""
import argparse
import math
import os
import random
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import nbt  # noqa: E402
import mega_lib as L  # noqa: E402  (check_state, autoconnect, ROOT, RES: used, never edited)
from template import Template, load  # noqa: E402

ROOT = L.ROOT
RES = L.RES
JAVA_MEGA = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "labyrinth", "mega")
JAVA_FILES = (os.path.join(JAVA_MEGA, "HubLayout.java"), os.path.join(JAVA_MEGA, "hub", "HubPlazaLayout.java"))
TEMPLATE_NAME = "hub/plaza"
GATE = "nusmp:labyrinth_gate"
SEED = "nusmp:hub:plaza"


# ------------------------------------------------------------------ the numbers (parsed from the Java, never copied)
def _strip_comments(text):
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def _split_top(s):
    parts, depth, cur = [], 0, ""
    for ch in s:
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        parts.append(cur)
    return parts


def java_ints(paths=JAVA_FILES):
    """Every `public static final int NAME = expr;` of the given Java files (expr: ints, earlier names, + - * and parentheses)."""
    consts = {}
    for path in paths:
        with open(path, encoding="utf-8") as f:
            text = _strip_comments(f.read())
        for m in re.finditer(r"public\s+static\s+final\s+int\s+([^;]+);", text):
            for part in _split_top(m.group(1)):
                name, expr = [x.strip() for x in part.split("=", 1)]
                if not re.fullmatch(r"[A-Za-z0-9_+\-*() ]+", expr):
                    raise ValueError("%s: cannot evaluate %r" % (path, expr))
                consts[name] = int(eval(expr, {"__builtins__": {}}, dict(consts)))  # noqa: S307 (the expression is checked above)
    return consts


K = java_ints()
SURFACE_Y = K["SURFACE_Y"]
SX, SY, SZ = K["SIZE_X"], K["SIZE_Y"], K["SIZE_Z"]
FLOOR = K["FLOOR_ROW"]
STAND = FLOOR + 1
CX, CZ = SX // 2, SZ // 2                       # template-local position of the plaza's middle
ORIGIN = (K["PLAZA_X"] - SX // 2, SURFACE_Y - 1 - FLOOR, K["PLAZA_Z"] - SZ // 2)
HOME = (K["HOME_GATE_X"] - K["PLAZA_X"], K["HOME_GATE_Y"] - ORIGIN[1], K["HOME_GATE_Z"] - K["PLAZA_Z"])   # (px, row, pz) of the gate centre
SPAWN = (K["SPAWN_X"] - K["PLAZA_X"], K["SPAWN_Z"] - K["PLAZA_Z"])                                         # (px, pz)
GATE_W, GATE_BELOW, GATE_ABOVE = K["GATE_HALF_WIDTH"], K["GATE_BELOW"], K["GATE_ABOVE"]
RADIUS = 16.45                                   # the paved disc
CLEAR_RADIUS = 16.95                             # the disc that is cleared of everything above the paving (trees, grass tufts)

HAZARDS = re.compile(r"(lava|fire|magma|cactus|campfire|sweet_berry|wither_rose|powder_snow|cobweb|pointed_dripstone|tnt|_bed|nether_portal|"
                     r"end_portal|trial_spawner|spawner|command_block|barrier|bedrock|structure_block|jigsaw)")


def vanilla_blocks():
    with open(os.path.join(HERE, "vanilla_blocks_1_21_1.txt")) as f:
        return {"minecraft:" + ln.strip() for ln in f if ln.strip() and not ln.startswith("#")}


VANILLA = vanilla_blocks()
_B = L._B
_H4 = L._H4
_H6 = L._H6
_ROT = tuple(str(i) for i in range(16))

#: property checks for blocks mega_lib's schema does not know (a wrong property is silently ignored by the game, so catch it here)
EXTRA_SCHEMA = [
    (r".*_wall_sign", {"facing": _H4, "waterlogged": _B}),
    (r".*_sign", {"rotation": _ROT, "waterlogged": _B}),
    (r"grass_block", {"snowy": _B}),
    (r"(rose_bush|peony|lilac|sunflower|tall_grass|large_fern)", {"half": ("lower", "upper")}),
    (r"hay_block", {"axis": ("x", "y", "z")}),
    (r"(azalea_leaves|flowering_azalea_leaves)", {"distance": tuple(str(i) for i in range(1, 8)), "persistent": _B, "waterlogged": _B}),
    (r"barrel", {"facing": _H6, "open": _B}),
    (r"water", {"level": tuple(str(i) for i in range(16))}),
    (r"(.*_wool|.*_carpet|potted_.*|flower_pot|poppy|dandelion|cornflower|azure_bluet|oxeye_daisy|allium|lily_of_the_valley|red_tulip|"
     r"pink_tulip|white_tulip|orange_tulip|short_grass|fern|dirt|smooth_stone|stone_bricks|cracked_stone_bricks|mossy_stone_bricks|"
     r"chiseled_stone_bricks|polished_andesite|andesite|sea_lantern|light_blue_stained_glass|spruce_planks|air)", {}),
]


def check_props(block, props):
    err = L.check_state(block, props)
    if err:
        return err
    path = block.split(":", 1)[1]
    for pat, allowed in EXTRA_SCHEMA:
        if re.fullmatch(pat, path):
            for k, v in props.items():
                if k not in allowed:
                    return "%s has no property %r (it has %s)" % (block, k, ", ".join(sorted(allowed)) or "none")
                if str(v) not in allowed[k]:
                    return "%s property %s=%r is not one of %s" % (block, k, v, ", ".join(allowed[k]))
            return None
    return None


# ------------------------------------------------------------------ signs
def sign_nbt(lines, color="black", waxed=True):
    """Block entity data of a sign (1.21.1: four JSON text components per face, `is_waxed` so nobody edits the signpost)."""
    if len(lines) > 4:
        raise ValueError("a sign has four lines, got %r" % (lines,))
    for ln in lines:
        if len(ln) > 15:
            raise ValueError("sign line %r is longer than 15 characters: it may be cut off in game" % ln)
        if not re.fullmatch(r"[A-Za-z0-9 .,:()<>!?'-]*", ln):
            raise ValueError("sign line %r has a character outside the plain set" % ln)
    msgs = ['{"text":"%s"}' % ln for ln in (list(lines) + [""] * 4)[:4]]
    blank = ['{"text":""}'] * 4

    def face(m):
        return nbt.Compound({"messages": nbt.List(m, tag_type=nbt.TAG_STRING), "color": color, "has_glowing_text": nbt.Byte(0)})

    return nbt.Compound({"id": "minecraft:sign", "is_waxed": nbt.Byte(1 if waxed else 0), "front_text": face(msgs), "back_text": face(blank)})


# ------------------------------------------------------------------ the builder
class Plaza:
    """A SX x SY x SZ template in plaza coordinates. `put` validates every block id and block state and tracks which feature wrote each cell."""

    def __init__(self):
        self.t = Template(TEMPLATE_NAME, SX, SY, SZ)
        self.rng = random.Random(SEED)
        self.feature = "ground"
        self.owner = {}

    def at(self, px, row, pz):
        return self.t.get(px + CX, row, pz + CZ)

    def put(self, px, row, pz, block, props=None, nbt_=None):
        tx, tz = px + CX, pz + CZ
        if not (0 <= tx < SX and 0 <= row < SY and 0 <= tz < SZ):
            raise ValueError("%s: (%d,%d,%d) is outside the template (plaza coordinates)" % (self.feature, px, row, pz))
        if ":" not in block:
            block = "minecraft:" + block
        props = {k: ("true" if v is True else "false" if v is False else str(v)) for k, v in (props or {}).items()}
        if block != GATE and block not in VANILLA:
            raise ValueError("%s: %s is not a vanilla block id of 1.21.1 (Vanilla Block Primacy; typo?)" % (self.feature, block))
        if HAZARDS.search(block.split(":", 1)[1]):
            raise ValueError("%s: %s is a hazard a passive animal must not meet" % (self.feature, block))
        err = check_props(block, props)
        if err:
            raise ValueError("%s: bad block state at (%d,%d,%d): %s" % (self.feature, px, row, pz, err))
        key = (tx, row, tz)
        prev = self.owner.get(key)
        if prev not in (None, "ground", self.feature):
            raise ValueError("%s overwrites a cell of %s at plaza (%d, %d, %d): %s" % (self.feature, prev, px, row, pz, self.at(px, row, pz)))
        self.owner[key] = self.feature
        self.t.set(tx, row, tz, block, props, nbt_)
        return self

    def box(self, px1, row1, pz1, px2, row2, pz2, block, props=None):
        for r in range(min(row1, row2), max(row1, row2) + 1):
            for pz in range(min(pz1, pz2), max(pz1, pz2) + 1):
                for px in range(min(px1, px2), max(px1, px2) + 1):
                    self.put(px, r, pz, block, props)
        return self

    def sign(self, px, row, pz, facing, lines, wood="spruce"):
        return self.put(px, row, pz, "%s_wall_sign" % wood, {"facing": facing}, sign_nbt(lines))

    def lamp_post(self, px, pz, height=3):
        """A lantern post: a stone-brick wall as the foot, spruce fence above it, a lantern on top. Stands on the paving at (px, pz)."""
        self.put(px, STAND, pz, "stone_brick_wall")
        for k in range(1, height):
            self.put(px, STAND + k, pz, "spruce_fence")
        self.put(px, STAND + height, pz, "lantern", {"hanging": False})
        return self


def facing_towards(px, pz, tx, tz):
    """The compass direction a stair at (px, pz) must face so that its high side points at (tx, tz) (dominant axis)."""
    dx, dz = tx - px, tz - pz
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


# ------------------------------------------------------------------ parts
def paving(p, px, pz):
    """The block of the paving layer at (px, pz): rings, avenues and a sunburst of eight lines, aged by a seeded scatter."""
    r = math.hypot(px, pz)
    if r > RADIUS:
        return None
    if r <= 2.45:
        return "smooth_stone"
    if r <= 3.55:
        return "stone_bricks"
    if r <= 4.55:
        return "polished_andesite"
    if r <= 5.55:
        return "smooth_stone"
    if abs(pz) <= 1 or abs(px) <= 1:
        return "smooth_stone" if (pz == 0 or px == 0) else "polished_andesite"
    if r > 15.45:
        return "polished_andesite"
    if 9.55 < r <= 10.55:
        return "smooth_stone"
    for k in range(8):
        a = k * math.pi / 4
        d = abs(-px * math.sin(a) + pz * math.cos(a))
        along = px * math.cos(a) + pz * math.sin(a)
        if along > 0 and d < 0.55:
            return "andesite"
    roll = p.rng.random()
    if roll < 0.10:
        return "cracked_stone_bricks"
    if roll < 0.18:
        return "mossy_stone_bricks"
    return "stone_bricks"


def build_ground(p):
    """Foundation, paving and the cleared air above it (the clearing removes a tree or tall grass the terrain put there before the stamp)."""
    p.feature = "ground"
    for pz in range(-17, 18):
        for px in range(-17, 18):
            r = math.hypot(px, pz)
            if r <= RADIUS:
                p.put(px, 0, pz, "dirt")
                p.put(px, 1, pz, "dirt")
                p.put(px, FLOOR, pz, paving(p, px, pz))
            if r <= CLEAR_RADIUS:
                for row in range(STAND, SY):
                    p.put(px, row, pz, "air")


def build_fountain(p):
    """A round basin: water in the middle (lit from below by sea lanterns), a rim of stone bricks capped with slabs, a ring of inward stairs.
    The water touches only full blocks, so nothing flows and nothing can spill onto the paving."""
    p.feature = "fountain"
    for pz in range(-5, 6):
        for px in range(-5, 6):
            r = math.hypot(px, pz)
            if r <= 2.45:
                p.put(px, STAND, pz, "water", {"level": 0})
            elif r <= 3.55:
                p.put(px, STAND, pz, "stone_bricks")
                p.put(px, STAND + 1, pz, "stone_brick_slab", {"type": "bottom"})
            elif r <= 4.55:
                p.put(px, STAND, pz, "stone_brick_stairs", {"facing": facing_towards(px, pz, 0, 0), "half": "bottom"})
    for px, pz in ((0, 0), (0, 2), (0, -2), (2, 0), (-2, 0)):
        p.put(px, FLOOR, pz, "sea_lantern")
    p.put(0, STAND, 0, "chiseled_stone_bricks")
    p.put(0, STAND + 1, 0, "light_blue_stained_glass")
    p.put(0, STAND + 2, 0, "light_blue_stained_glass")
    p.put(0, STAND + 3, 0, "stone_brick_wall")
    p.put(0, STAND + 4, 0, "lantern", {"hanging": False})


def build_gatehouse(p):
    """The way home on the north edge: two pillars, a lintel with a stepped crown, a back wall, a banner on each pillar, a sign, the gate plane."""
    p.feature = "gatehouse"
    gx, gz = HOME[0], HOME[2]                       # centre of the plane (px, pz)
    top_open = STAND + GATE_BELOW + GATE_ABOVE      # last row of the plane
    for dz in (-1, 0, 1):
        for side in (-1, 1):
            for off in (2, 3):
                px = gx + side * off
                p.put(px, STAND, gz + dz, "polished_andesite")
                for row in range(STAND + 1, top_open + 2):
                    p.put(px, row, gz + dz, "stone_bricks")
                p.put(px, top_open + 2, gz + dz, "chiseled_stone_bricks" if off == 2 else "polished_andesite")
    lint = top_open + 1
    for dz in (-1, 0, 1):
        for px in range(gx - 1, gx + 2):
            p.put(px, lint, gz + dz, "stone_bricks")
    p.put(gx, lint, gz + 1, "chiseled_stone_bricks")
    crown = top_open + 2
    for dz in (-1, 0, 1):
        for px in range(gx - 1, gx + 2):
            p.put(px, crown, gz + dz, "stone_bricks")
        for px in range(gx - 1, gx + 2):
            p.put(px, crown + 1, gz + dz, "stone_bricks")
        p.put(gx - 1, crown + 2, gz + dz, "stone_brick_stairs", {"facing": "east", "half": "bottom"})
        p.put(gx + 1, crown + 2, gz + dz, "stone_brick_stairs", {"facing": "west", "half": "bottom"})
        p.put(gx, crown + 2, gz + dz, "stone_brick_slab", {"type": "bottom"})
    for side in (-1, 1):
        for dz in (-1, 1):
            p.put(gx + side * 3, crown + 1, gz + dz, "lantern", {"hanging": False})
    for row in range(STAND, top_open + 1):          # back wall: the gate is a recessed doorway, entered from the south
        for px in range(gx - GATE_W, gx + GATE_W + 1):
            p.put(px, row, gz - 1, "stone_bricks")
    p.put(gx - 2, STAND + 2, gz + 2, "white_wall_banner", {"facing": "south"})
    p.put(gx + 2, STAND + 2, gz + 2, "black_wall_banner", {"facing": "south"})
    p.sign(gx, lint, gz + 2, "south", ["", "Overworld", "(home)", ""])
    for row in range(STAND, top_open + 1):          # the gate plane: 3 wide, 4 high, from the standing level up
        for px in range(gx - GATE_W, gx + GATE_W + 1):
            p.put(px, row, gz, GATE)
    for px in range(gx - 2, gx + 3):                # a dais of polished andesite in front of it
        for dz in (1, 2):
            p.put(px, FLOOR, gz + dz, "polished_andesite")


def build_exits(p):
    """The two avenue ends: a signpost for the Spire (east) and one for the Chasm (west), and a pair of banner pillars (white / black) on each."""
    p.feature = "exits"
    posts = ((1, ["Ascension", "the Holy Spire", "", ">> east >>"], "white"), (-1, ["Descent", "the Abyssal", "Chasm", "<< west <<"], "black"))
    for side, lines, color in posts:
        px, pz = side * 12, -3
        for k in range(4):
            p.put(px, STAND + k, pz, "stripped_spruce_log", {"axis": "y"})
        p.put(px, STAND + 4, pz, "lantern", {"hanging": False})
        p.sign(px, STAND + 2, pz + 1, "south", lines)
        p.sign(px - side, STAND + 2, pz, "west" if side > 0 else "east", lines)
        for dz in (-2, 2):
            for k in range(4):
                p.put(side * 15, STAND + k, dz, "stone_bricks" if k else "polished_andesite")
            p.put(side * 15, STAND + 4, dz, "lantern", {"hanging": False})
            p.put(side * 14, STAND + 2, dz, "%s_wall_banner" % color, {"facing": "west" if side > 0 else "east"})


def build_noticeboard(p):
    """South of the arrival cell, so a newcomer (who spawns facing south) reads it at once."""
    p.feature = "noticeboard"
    pz = 14
    for px in range(-4, 5):
        for row in (STAND, STAND + 1, STAND + 2):
            p.put(px, row, pz, "spruce_planks")
    for px in (-5, 5):
        for row in range(STAND, STAND + 4):
            p.put(px, row, pz, "spruce_log", {"axis": "y"})
    for px in range(-5, 6):
        p.put(px, STAND + 3, pz, "spruce_slab", {"type": "bottom"})
    texts = [
        ["Floor 0", "THE SANCTUARY", "", "Safe ground"],
        ["Only gentle", "creatures roam", "here. Nothing", "hostile lives."],
        ["Build a base", "anywhere but", "the paved", "square."],
        ["East: climb", "the Holy Spire", "West: descend", "the Chasm"],
    ]
    for px, lines in zip((-3, -1, 1, 3), texts):
        p.sign(px, STAND + 1, pz - 1, "north", lines)


def build_dais(p):
    """The arrival cell: a round dais, kept clear above."""
    p.feature = "dais"
    sx, sz = SPAWN
    for pz in range(sz - 2, sz + 3):
        for px in range(sx - 2, sx + 3):
            r = math.hypot(px - sx, pz - sz)
            if r <= 2.45:
                p.put(px, FLOOR, pz, "chiseled_stone_bricks" if r < 0.5 else "polished_andesite" if r <= 1.55 else "smooth_stone")


def region_cells(pred, extent=17):
    return {(px, pz) for pz in range(-extent, extent + 1) for px in range(-extent, extent + 1) if pred(px, pz)}


def build_beds(p):
    """Four curved flower beds on the diagonals: a low border of slabs, grass inside, flowers on it. Nothing thorny, so safe for animals."""
    p.feature = "beds"
    flowers = ["poppy", "dandelion", "cornflower", "azure_bluet", "oxeye_daisy", "allium", "red_tulip", "pink_tulip", "white_tulip", "orange_tulip",
               "lily_of_the_valley"]
    doubles = ["rose_bush", "peony", "lilac"]
    for sx_, sz_ in ((1, 1), (-1, 1), (1, -1), (-1, -1)):
        def inside(px, pz, sx_=sx_, sz_=sz_):
            r = math.hypot(px, pz)
            ang = math.degrees(math.atan2(abs(pz), abs(px)))
            return px * sx_ > 0 and pz * sz_ > 0 and 6.0 <= r <= 10.0 and abs(ang - 45) <= 26
        cells = region_cells(inside)
        for (px, pz) in sorted(cells):
            edge = any((px + dx, pz + dz) not in cells for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                p.put(px, STAND, pz, "stone_brick_slab", {"type": "bottom"})
            else:
                p.put(px, FLOOR, pz, "grass_block", {"snowy": False})
                roll = p.rng.random()
                if roll < 0.2:
                    d = p.rng.choice(doubles)
                    p.put(px, STAND, pz, d, {"half": "lower"})
                    p.put(px, STAND + 1, pz, d, {"half": "upper"})
                elif roll < 0.88:
                    p.put(px, STAND, pz, p.rng.choice(flowers))
                else:
                    p.put(px, STAND, pz, "short_grass")


def build_benches(p):
    """Eight benches: two beside each arm of the avenues. A bench is three stairs; the sitter looks across the avenue, so the stair backs turn away."""
    p.feature = "benches"
    for cx, cz, back in ((9, -2, "north"), (9, 2, "south"), (-9, -2, "north"), (-9, 2, "south")):
        for dx in (-1, 0, 1):
            p.put(cx + dx, STAND, cz, "spruce_stairs", {"facing": back, "half": "bottom"})
    for cx, cz, back in ((-2, -9, "west"), (2, -9, "east")):
        for dz in (-1, 0, 1):
            p.put(cx, STAND, cz + dz, "spruce_stairs", {"facing": back, "half": "bottom"})


def build_stalls(p):
    """Four market stalls on the diagonals: awnings striped in two colours, banners on the valance, a counter with flowers for sale, barrels behind
    and a rug on the floor. The open side looks at the avenue."""
    colors = {(1, -1): "red", (-1, -1): "blue", (1, 1): "yellow", (-1, 1): "green"}
    for (sx_, sz_), color in colors.items():
        p.feature = "stall_%s" % color
        cx, cz = sx_ * 10, sz_ * 8
        face = "south" if sz_ < 0 else "north"
        front = 1 if face == "south" else -1
        back = -front
        for dx in (-2, 2):
            for dz in (-1, 1):
                for k in range(3):
                    p.put(cx + dx, STAND + k, cz + dz, "spruce_fence")
        for dz in (-1, 0, 1):
            for dx in range(-2, 3):
                p.put(cx + dx, STAND + 3, cz + dz, ("%s_wool" % color) if dx % 2 == 0 else "white_wool")
        for dx in (-2, 0, 2):
            p.put(cx + dx, STAND + 3, cz + 2 * front, "%s_wall_banner" % color, {"facing": face})
        pots = ["potted_poppy", "potted_cornflower", "potted_azure_bluet", "flower_pot", "potted_allium", "potted_dandelion"]
        for dx in (-1, 0, 1):
            p.put(cx + dx, STAND, cz + front, "spruce_planks")
            p.put(cx + dx, STAND + 1, cz + front, p.rng.choice(pots))
            p.put(cx + dx, STAND, cz, "%s_carpet" % color)
            p.put(cx + dx, STAND, cz + back, "barrel", {"facing": "up", "open": False})
        p.lamp_post(cx + sx_ * 3, cz + back, 2)


def build_lamps(p):
    """Eight lantern posts on a ring between the avenues and the stalls, and two flanking the arrival dais (a newcomer faces south, towards them)."""
    p.feature = "lamps"
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        p.lamp_post(int(round(14.2 * math.cos(a))), int(round(14.2 * math.sin(a))))
    for side in (-3, 3):
        p.lamp_post(SPAWN[0] + side, SPAWN[1] + 2)


def build_hedge(p):
    """A low hedge of azalea leaves (persistent, so they never decay) on the outermost ring, with gaps for the gatehouse, the notice board and both
    avenue exits. Built last: it never takes a cell another part already owns."""
    p.feature = "hedge"
    for pz in range(-17, 18):
        for px in range(-17, 18):
            r = math.hypot(px, pz)
            if not (15.45 < r <= RADIUS):
                continue
            if (abs(px) <= 6 and pz < 0) or (abs(px) <= 7 and pz > 0) or (abs(pz) <= 3):
                continue
            if any(p.owner.get((px + CX, row, pz + CZ)) not in (None, "ground") for row in range(FLOOR, FLOOR + 3)):
                continue
            p.put(px, FLOOR, pz, "grass_block", {"snowy": False})
            leaf = "flowering_azalea_leaves" if p.rng.random() < 0.18 else "azalea_leaves"
            p.put(px, STAND, pz, leaf, {"persistent": True, "distance": 1})
            if (px + pz) % 3 == 0:
                p.put(px, STAND + 1, pz, "azalea_leaves", {"persistent": True, "distance": 1})


def build():
    p = Plaza()
    build_ground(p)
    build_fountain(p)
    build_dais(p)
    build_gatehouse(p)
    build_exits(p)
    build_noticeboard(p)
    build_beds(p)
    build_benches(p)
    build_stalls(p)
    build_lamps(p)
    build_hedge(p)
    return p


def finish(p):
    L.autoconnect(p.t)
    return p.t


# ------------------------------------------------------------------ the checks (the substitute for "stamp it in the game and look")
#: blocks a wall sign, a banner or a standing lantern may rest on (a full-enough block: the game's `canSurvive` asks for a solid face)
SUPPORTS = re.compile(r"(stone_bricks|cracked_stone_bricks|mossy_stone_bricks|chiseled_stone_bricks|polished_andesite|andesite|smooth_stone|"
                      r"spruce_planks|spruce_log|stripped_spruce_log|.*_wool|barrel|stone_brick_wall|spruce_fence|sea_lantern|stone_brick_slab|"
                      r"stone_brick_stairs|spruce_slab|spruce_stairs|dirt|grass_block)")
#: what water may touch on its four sides: only full blocks (a slab or a stair would be waterlogged, and the water would creep on)
WATER_WALLS = re.compile(r"(stone_bricks|chiseled_stone_bricks|polished_andesite|andesite|smooth_stone|sea_lantern|water)")
PLANTS_ON = re.compile(r"(grass_block|dirt)")
PLANTS = re.compile(r"(poppy|dandelion|cornflower|azure_bluet|oxeye_daisy|allium|lily_of_the_valley|red_tulip|pink_tulip|white_tulip|orange_tulip|"
                    r"short_grass|fern|rose_bush|peony|lilac)")


def _path(c):
    return None if c is None else c.block.split(":", 1)[1]


def check_template(t):
    """Every rule of the plaza; returns a list of problems (empty = fine)."""
    errors = []
    sx, sy, sz = t.size
    if t.size != (SX, SY, SZ):
        errors.append("template size %s is not HubPlazaLayout's %s" % (t.size, (SX, SY, SZ)))
    errors += ["template: " + m for m in __import__("template").validate(t)]

    def cell(px, row, pz):
        return t.cells.get((px + CX, row, pz + CZ))

    # the way home: a 3 x 4 plane of the gate block whose recorded centre is HomeGate
    gx, grow, gz = HOME
    plane = [(gx + dx, STAND + k, gz) for dx in range(-GATE_W, GATE_W + 1) for k in range(0, GATE_BELOW + GATE_ABOVE + 1)]
    for (px, row, pz) in plane:
        c = cell(px, row, pz)
        if c is None or c.block != GATE:
            errors.append("the home gate plane has no gate block at plaza (%d,%d,%d)" % (px, row, pz))
    c = cell(gx, grow, gz)
    if c is None or c.block != GATE:
        errors.append("the block at HOME_GATE (plaza %s) is not the gate" % ((gx, grow, gz),))
    if sum(1 for c in t.cells.values() if c.block == GATE) != len(plane):
        errors.append("the gate block appears outside the home plane")
    if (gx + CX, grow, gz + CZ) != (K["HOME_GATE_X"] - ORIGIN[0], K["HOME_GATE_Y"] - ORIGIN[1], K["HOME_GATE_Z"] - ORIGIN[2]):
        errors.append("the gate centre does not land on HubLayout.HOME_GATE once the template is placed at its origin")
    for dz in (1, 2):                                            # the way in must be clear and paved
        for dx in range(-GATE_W, GATE_W + 1):
            for k in range(0, GATE_BELOW + GATE_ABOVE + 1):
                c = cell(gx + dx, STAND + k, gz + dz)
                if c is not None and c.block != "minecraft:air":
                    errors.append("the walk-in to the gate is blocked at plaza (%d,%d,%d) by %s" % (gx + dx, STAND + k, gz + dz, c.block))
    # the arrival cell and the two cells above it are free, the ground under it is solid paving
    sxp, szp = SPAWN
    for k in range(0, 3):
        c = cell(sxp, STAND + k, szp)
        if c is not None and c.block != "minecraft:air":
            errors.append("the arrival cell column is blocked at row %d by %s" % (STAND + k, c.block))
    c = cell(sxp, FLOOR, szp)
    if c is None or c.block in ("minecraft:air", "minecraft:water"):
        errors.append("nothing solid under the arrival cell")
    # the paving is complete inside the disc and there is no hole (nothing a player or animal could fall into)
    for pz in range(-17, 18):
        for px in range(-17, 18):
            r = math.hypot(px, pz)
            if r <= RADIUS:
                c = cell(px, FLOOR, pz)
                if c is None or c.block == "minecraft:air":
                    errors.append("a hole in the paving at plaza (%d,%d)" % (px, pz))
                if cell(px, 0, pz) is None or cell(px, 1, pz) is None:
                    errors.append("no foundation under plaza (%d,%d)" % (px, pz))
    # everything above the paving stays inside the cleared disc, so the template never leaves a half tree behind
    for (x, y, z), c in t.cells.items():
        if y >= STAND and c.block != "minecraft:air" and math.hypot(x - CX, z - CZ) > CLEAR_RADIUS:
            errors.append("%s at plaza (%d,%d,%d) lies outside the cleared disc" % (c.block, x - CX, y, z - CZ))
    # support rules
    for (x, y, z), c in t.cells.items():
        path = _path(c)
        px, pz = x - CX, z - CZ
        if path.endswith("_wall_sign") or path.endswith("_wall_banner"):
            d = c.props.get("facing")
            back = {"north": (0, 1), "south": (0, -1), "east": (-1, 0), "west": (1, 0)}[d]
            b = t.cells.get((x + back[0], y, z + back[1]))
            if b is None or not SUPPORTS.fullmatch(_path(b)):
                errors.append("%s at plaza (%d,%d,%d) facing %s has nothing to hang on (%s)" % (path, px, y, pz, d, None if b is None else b.block))
        elif path == "lantern" and c.props.get("hanging") == "false":
            b = t.cells.get((x, y - 1, z))
            if b is None or _path(b) not in ("stone_brick_wall", "spruce_fence", "stone_bricks", "polished_andesite", "chiseled_stone_bricks", "stripped_spruce_log", "spruce_log"):
                errors.append("a standing lantern at plaza (%d,%d,%d) rests on %s" % (px, y, pz, None if b is None else b.block))
        elif path.endswith("_carpet"):
            b = t.cells.get((x, y - 1, z))
            if b is None or _path(b) == "air":
                errors.append("a carpet floats at plaza (%d,%d,%d)" % (px, y, pz))
        elif PLANTS.fullmatch(path):
            half = c.props.get("half")
            below = t.cells.get((x, y - 1, z))
            if half == "upper":
                if below is None or _path(below) != path or below.props.get("half") != "lower":
                    errors.append("the upper half of %s at plaza (%d,%d,%d) has no lower half" % (path, px, y, pz))
            elif below is None or not PLANTS_ON.fullmatch(_path(below)):
                errors.append("%s at plaza (%d,%d,%d) is not on grass or dirt (%s)" % (path, px, y, pz, None if below is None else below.block))
            if half == "lower":
                above = t.cells.get((x, y + 1, z))
                if above is None or _path(above) != path or above.props.get("half") != "upper":
                    errors.append("the lower half of %s at plaza (%d,%d,%d) has no upper half" % (path, px, y, pz))
        elif path.startswith("potted_") or path == "flower_pot":
            b = t.cells.get((x, y - 1, z))
            if b is None or _path(b) == "air":
                errors.append("a flower pot floats at plaza (%d,%d,%d)" % (px, y, pz))
        elif path == "water":
            below = t.cells.get((x, y - 1, z))
            if below is None or not WATER_WALLS.fullmatch(_path(below)):
                errors.append("water at plaza (%d,%d,%d) has no solid floor (%s)" % (px, y, pz, None if below is None else below.block))
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = t.cells.get((x + dx, y, z + dz))
                if n is None or not WATER_WALLS.fullmatch(_path(n)):
                    errors.append("water at plaza (%d,%d,%d) leaks to the %s: %s" % (px, y, pz, "ESWN"[((1, 0), (0, 1), (-1, 0), (0, -1)).index((dx, dz))], None if n is None else n.block))
    # signs: every one is waxed, has four lines per face, a plain text and names a place of the Hub
    seen = 0
    for (x, y, z), c in t.cells.items():
        if _path(c).endswith("_sign"):
            seen += 1
            if c.nbt is None or int(c.nbt.get("is_waxed", 0)) != 1:
                errors.append("a sign at plaza (%d,%d,%d) is not waxed" % (x - CX, y, z - CZ))
    if seen < 8:
        errors.append("expected the signposts, the notice board and the home sign (8 or more signs), found %d" % seen)
    needed = {"Ascension": False, "the Holy Spire": False, "Descent": False, "Chasm": False, "Overworld": False}
    for c in t.cells.values():
        if c.nbt is not None and "front_text" in c.nbt:
            for m in c.nbt["front_text"]["messages"]:
                for k in needed:
                    if '"%s"' % k in m or ('"text":"%s' % k) in m:
                        needed[k] = True
    for k, v in needed.items():
        if not v:
            errors.append("no sign says %r" % k)
    return errors


def check(verbose=True):
    t = finish(build())
    errors = check_template(t)
    again = finish(build())
    if bytes_of(t) != bytes_of(again):
        errors.append("two builds differ: the generator is not deterministic")
    out = path_of()
    if not os.path.exists(out):
        errors.append("%s does not exist: run the generator" % out)
    else:
        with open(out, "rb") as f:
            raw = f.read()
        if raw != bytes_of(t):
            errors.append("%s is not what this script writes: run it again" % out)
        try:
            back = load(out, TEMPLATE_NAME)
            if back != t:
                errors.append("the file does not round-trip to the template")
            if int(nbt.loads(raw)[1].get("DataVersion", 0)) != 3955:
                errors.append("DataVersion is not 3955 (1.21.1)")
        except Exception as e:  # a corrupt file is a failure, not a crash
            errors.append("cannot read %s: %s" % (out, e))
    for e in errors:
        print("FAIL", e)
    if verbose:
        kinds = {}
        for c in t.cells.values():
            kinds[c.block] = kinds.get(c.block, 0) + 1
        print("%s: %dx%dx%d, %d cells, %d distinct blocks, %d problem(s)" % (TEMPLATE_NAME, SX, SY, SZ, len(t.cells), len(kinds), len(errors)))
    return not errors


# ------------------------------------------------------------------ files
def path_of(root=RES):
    return os.path.join(root, "data", "nusmp", "structure", "hub", "plaza.nbt")


def bytes_of(t):
    return nbt.dumps(t.to_nbt(), compress=True)


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n\n")[0])
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--ascii", action="store_true")
    ap.add_argument("--preview", metavar="PREFIX")
    a = ap.parse_args(argv)
    if a.check:
        return 0 if check() else 1
    t = finish(build())
    if a.ascii:
        print(t.ascii_layers())
        return 0
    if a.preview:
        import gen_mega_hub_preview as V
        V.render(t, a.preview)
        return 0
    out = path_of()
    os.makedirs(os.path.dirname(out), exist_ok=True)
    with open(out, "wb") as f:
        f.write(bytes_of(t))
    print("wrote", out, "%d blocks" % len(t.cells))
    return 0


if __name__ == "__main__":
    sys.exit(main())

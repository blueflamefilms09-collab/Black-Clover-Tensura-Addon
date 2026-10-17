#!/usr/bin/env python3
"""
Why: THEME-D (docs/labyrinth/mega_v2_thd.md) builds twenty-odd rooms for four themes (the Underdark, the Infernal Foundry, the Drowned Crypt, the Spider Warrens) and
they all need the same handful of things the older kits (`mega_lib`, `mega_decor_r1`, `mega_decor_r2`, `gen_mega_rooms2`) do not give them: giant mushrooms with the
right face states, spires and cones, spore clouds made of glass and an invisible light, web scenery, a way to place markers and fix dark spots without a `Hall`, and ONE
checking entry point that runs every offline check the lead's tools have (the rooms2 analysis, the arch survival rules, the multi-block pairing and the shared light report)
before a template is written. This module is that kit and the runner every `gen_mega_theme_<id>.py` ends with, so four generators read like floor plans and cannot
drift apart in what they check.

Nothing here is a custom block: architecture is the palette of `mega_lib` (the Region Theme restyles it per sector, see the four theme files), everything else is vanilla.
Deterministic: every random choice comes from the template's own name-seeded `MT.rng`.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r2 as D  # noqa: E402
import mega_decor_r1 as R1  # noqa: E402
import mega_arch_check as AC  # noqa: E402
import mega_shape_preview as SP  # noqa: E402
import gen_mega_rooms2 as G2  # noqa: E402
import nbt as _nbt  # noqa: E402
from mega_lib import MT, Frame  # noqa: E402

FR = L.FLOOR_ROW
GROUND = FR + 1
LOOT = "nusmp:chests/labyrinth_trial"
DIRS4 = (("north", 0, -1), ("east", 1, 0), ("south", 0, 1), ("west", -1, 0))


# =====================================================================================================================
# safe placement
# =====================================================================================================================
def inside(mt, x, y, z):
    return 0 <= x < mt.W and 0 <= y < L.HEIGHT and 0 <= z < mt.D


def free(mt, x, y, z):
    """True when the cell is in the box, still unset and not part of a doorway hole."""
    return inside(mt, x, y, z) and mt.get(x, y, z) is None and (x, y, z) not in mt.reserved


def put(mt, x, y, z, block, props=None, over=False, nbt_=None):
    """Sets a block when the cell is in the box and not a doorway hole, and (unless `over`) still unset. Returns whether it did. Why: decoration must never fight a
    doorway or overwrite architecture, and a round room draws circles that graze the edge of the box."""
    if not inside(mt, x, y, z) or (x, y, z) in mt.reserved:
        return False
    if not over and mt.get(x, y, z) is not None:
        return False
    mt.set(x, y, z, block, props, nbt_)
    return True


def box(mt, x0, x1, y0, y1, z0, z1, block, props=None, over=False):
    """Fills the inclusive box with `block` through `put`; returns the number of cells written."""
    n = 0
    for y in range(min(y0, y1), max(y0, y1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                if put(mt, x, y, z, block, props, over):
                    n += 1
    return n


def clear(mt, x0, x1, y0, y1, z0, z1):
    """Removes every cell of the inclusive box (air), except doorway holes (which are already air at finish)."""
    for y in range(min(y0, y1), max(y0, y1) + 1):
        for z in range(min(z0, z1), max(z0, z1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                if inside(mt, x, y, z) and (x, y, z) not in mt.reserved:
                    mt.remove(x, y, z)


def st(facing, half="bottom"):
    """Stair state: `facing` is the direction of the tall side (the way UP the flight), as vanilla has it."""
    return {"facing": facing, "half": half}


def slab(kind="bottom"):
    return {"type": kind}


def disk(cx, cz, r):
    return L.disk_cells(cx, cz, r)


def ring(cx, cz, r, thick=1):
    return L.ring_cells(cx, cz, r, thick)


def dist(x, z, cx, cz):
    return math.hypot(x - cx, z - cz)


def opposite(f):
    return L.OPPOSITE[f]


# =====================================================================================================================
# lights and lamps that survive the arch rules
# =====================================================================================================================
def hang(mt, x, y_top, z, drop, lantern=L.LANTERN):
    """A lantern on `drop` chain links under the solid cell (x, y_top, z)."""
    D.hang_lantern(mt, x, y_top, z, drop, lantern)


def lamp_post(mt, x, y, z, height=3, lantern=L.LANTERN):
    D.lamp_post(mt, x, y, z, height, lantern)


def hidden_light(mt, x, y, z, level=10):
    """An invisible light block in a free cell (the theme shifts it, never under level 6)."""
    return D.light(mt, x, y, z, level)


def glow_sphere(mt, cx, cy, cz, r=2, glass="minecraft:purple_stained_glass", level=11):
    """A floating shell of stained glass of radius `r` round the cell (cx, cy, cz) with an invisible light block in its middle: a faerie-fire or spore globe that glows
    without a vanilla particle (no particle spam) and without a lantern. The shell keeps its own cells touching each other, so nothing counts as floating."""
    n = 0
    for dy in range(-r, r + 1):
        for dz in range(-r, r + 1):
            for dx in range(-r, r + 1):
                d = math.sqrt(dx * dx + dy * dy + dz * dz)
                if r - 0.9 < d <= r + 0.35:
                    if put(mt, cx + dx, cy + dy, cz + dz, glass):
                        n += 1
    put(mt, cx, cy, cz, "minecraft:light", {"level": str(level), "waterlogged": "false"})
    return n


def hang_sphere(mt, cx, cy, cz, y_ceiling, r=1, glass="minecraft:purple_stained_glass", level=12):
    """A glow sphere hung from the solid cell at `y_ceiling` by a chain (the chain runs from the sphere's top to the ceiling)."""
    for y in range(cy + r + 1, y_ceiling):
        put(mt, cx, y, cz, L.CHAIN_BLOCK, {"axis": "y"})
    return glow_sphere(mt, cx, cy, cz, r, glass, level)


def candles(mt, x, y, z, n=3, color="white", lit=True):
    """Candles on the FULL block under (x, y, z)."""
    return put(mt, x, y, z, "minecraft:%s_candle" % color, {"candles": str(max(1, min(4, n))), "lit": "true" if lit else "false", "waterlogged": "false"})


def cobweb(mt, x, y, z):
    return put(mt, x, y, z, "minecraft:cobweb")


def banner(mt, x, y, z, facing, color="purple"):
    """A wall banner on the wall behind (x, y, z); `facing` is away from the wall (checked by the arch rules)."""
    return put(mt, x, y, z, "minecraft:%s_wall_banner" % color, {"facing": facing})


# =====================================================================================================================
# giant mushrooms
# =====================================================================================================================
MUSH = {"red": "minecraft:red_mushroom_block", "brown": "minecraft:brown_mushroom_block", "stem": "minecraft:mushroom_stem"}
WART = {"warped": "minecraft:warped_wart_block", "nether": "minecraft:nether_wart_block"}


def cap_cells(cx, cy, cz, r, rise, thick=1.6, plate=False):
    """The cells of a mushroom cap whose brim is the ring of radius `r` at row `cy` and whose crown is `rise` rows above it: a hollow dome (an ellipsoid shell about
    `thick` thick; the brim row is the ring only, so the underside stays open) or, with `plate`, a flat umbrella: a full disc two rows thick at the crown with a one-row
    drooping skirt below its rim. Returns a set of (x, y, z)."""
    out = set()
    if plate:
        top = cy + rise
        for (x, z) in disk(cx, cz, r):
            out.add((x, top, z))
        for (x, z) in disk(cx, cz, max(0.5, r - 2.5)):
            out.add((x, top - 1, z))
        for (x, z) in ring(cx, cz, r, 1):
            out.add((x, top - 1, z))
        return out
    for dy in range(0, rise + 1):
        for dz in range(-r - 1, r + 2):
            for dx in range(-r - 1, r + 2):
                rho = math.hypot(dx, dz)
                e_out = (rho / (r + 0.35)) ** 2 + (dy / (rise + 0.5)) ** 2
                e_in = (rho / max(0.5, r - thick)) ** 2 + (dy / max(0.5, rise - thick + 0.5)) ** 2
                if e_out <= 1.0 and (e_in > 1.0 or dy == rise):
                    if dy == 0 and rho < r - 1.4:
                        continue
                    out.add((cx + dx, cy + dy, cz + dz))
    return out


def stem_cells(cx, cz, y0, y1, radius):
    """The cells of a stem column: radius 0 is one cell, 1 a plus-shaped 3 x 3, 2 a rounded 5 x 5, 3 a rounded 7 x 7."""
    out = set()
    for (x, z) in ([(cx, cz)] if radius <= 0 else disk(cx, cz, radius)):
        for y in range(y0, y1 + 1):
            out.add((x, y, z))
    return out


def lay_mushroom(mt, cap, stem, cap_block, spots=None, spot_block="minecraft:shroomlight", spot_share=0.12, rng=None, over=False, stem_block=None):
    """Writes a mushroom: every cap cell as a mushroom block whose faces are set the way the game does it (a face shows the cap texture when nothing of the mushroom
    touches it, the underside shows the pores), every stem cell as `mushroom_stem` (sides on, top and bottom off), and a share of the cap's TOP cells as glowing spots
    (`spot_block`). `cap_block` and `stem_block` may also be the warped fungus's `warped_wart_block` / `warped_stem` (no faces, an axis) or the crimson ones. A cell that is
    already built is left alone (unless `over`). Returns the number of cells written."""
    rng = rng or mt.rng
    n = 0
    stem_block = stem_block or MUSH["stem"]
    stem = set(stem)
    cap = set(cap)
    mass = cap | stem

    def face(c, d):
        x, y, z = c
        dx, dy, dz = d
        return "false" if (x + dx, y + dy, z + dz) in mass else "true"

    faces_stem = stem_block == MUSH["stem"]
    faces_cap = cap_block in (MUSH["red"], MUSH["brown"])
    for c in sorted(stem):
        if faces_stem:
            props = {"north": face(c, (0, 0, -1)), "south": face(c, (0, 0, 1)), "east": face(c, (1, 0, 0)), "west": face(c, (-1, 0, 0)), "up": "false", "down": "false"}
        else:
            props = {"axis": "y"}
        if put(mt, *c, stem_block, props, over):
            n += 1
    for c in sorted(cap):
        x, y, z = c
        if spots and (x, y + 1, z) not in cap and rng.random() < spot_share:
            if put(mt, x, y, z, spot_block, None, over):
                n += 1
            continue
        props = None
        if faces_cap:
            props = {"north": face(c, (0, 0, -1)), "south": face(c, (0, 0, 1)), "east": face(c, (1, 0, 0)), "west": face(c, (-1, 0, 0)),
                     "up": face(c, (0, 1, 0)), "down": "false"}
        if put(mt, x, y, z, cap_block, props, over):
            n += 1
    return n


def giant_mushroom(mt, cx, cz, y0, stem_h, stem_r, cap_r, rise, cap_block, spots=True, plate=False, rng=None, over=False, thick=1.6, stem_block=None, spot_share=0.12):
    """A giant mushroom whose stem stands on the floor cell row `y0` (the first stem cell is at y0), is `stem_h` tall with a stem of radius `stem_r`, and whose cap brim
    is the ring of radius `cap_r` just under the crown. Returns (cap cell set, stem cell set); nothing is written when any needed cell is taken."""
    stem = stem_cells(cx, cz, y0, y0 + stem_h - 1, stem_r)
    cap = cap_cells(cx, y0 + stem_h, cz, cap_r, rise, thick=thick, plate=plate)
    for c in stem | cap:
        if not inside(mt, *c) or mt.get(*c) is not None or c in mt.reserved:
            return None
    lay_mushroom(mt, cap, stem, cap_block, spots=spots, rng=rng, over=over, stem_block=stem_block, spot_share=spot_share)
    return cap, stem


def spire(mt, cx, cz, y0, h, r, block, tip=None, over=False):
    """A cone of `block` standing on row y0 (the first row), `h` rows tall with base radius `r`, ending in a single cell (the `tip` block when given): a stalagmite tower or
    the spire of a drow house."""
    n = 0
    for k in range(h):
        rk = r * (1.0 - k / float(h)) ** 0.85
        for (x, z) in (disk(cx, cz, rk) if rk >= 0.5 else [(cx, cz)]):
            if put(mt, x, y0 + k, z, tip if (tip and k == h - 1) else block, None, over):
                n += 1
    return n


def stalactite_cone(mt, cx, cz, y_top, h, r, block, over=False):
    """A cone hanging from the ceiling row `y_top` (its first row) down `h` rows."""
    n = 0
    for k in range(h):
        rk = r * (1.0 - k / float(h)) ** 0.85
        for (x, z) in (disk(cx, cz, rk) if rk >= 0.5 else [(cx, cz)]):
            if put(mt, x, y_top - k, z, block, None, over):
                n += 1
    return n


# =====================================================================================================================
# markers
# =====================================================================================================================
def read_markers(mt):
    return G2.read_markers(mt)


def _door_anchors(an):
    return [a for a in an.anchors() if a is not None]


def auto_markers(mt, faction, spawns=6, patrols=2, spacing=6, door_gap=6, edge=3, heavy=2, avoid=(), prefer=None, room_center=False):
    """Spawn, patrol and faction markers on reachable, flat, wide floor, chosen by farthest-point sampling from the door anchors, never near a doorway or a wall (the
    spacing relaxes in steps for a small room, never below four). `avoid` is a list of (x, z, radius) discs to keep clear; `prefer(x, y, z)` may veto a cell. Markers stand in the
    FREE standing cell, so nothing is overwritten. The first `heavy` spawns carry `;weight=2`. Returns the chosen cells."""
    an = D.Analysis(mt)
    anchors = _door_anchors(an)
    if not anchors:
        raise ValueError("%s: no walkable doorway to start the marker search from" % mt.name)
    reach = an.reach(anchors[0])
    need = spawns + patrols + 1
    cells = []
    for gap, space, wide_r in ((door_gap, spacing, 1), (door_gap - 1, spacing - 1, 1), (max(4, door_gap - 2), max(4, spacing - 2), 1), (3, 4, 0)):
        cand = []
        for n in sorted(reach):
            x, y, z = n
            if x < L.WALL + edge or z < L.WALL + edge or x > mt.W - 1 - L.WALL - edge or z > mt.D - 1 - L.WALL - edge:
                continue
            if any(abs(x - a[0]) + abs(z - a[2]) < gap for a in anchors):
                continue
            if any(dist(x, z, ax, az) <= ar for (ax, az, ar) in avoid):
                continue
            if mt.get(x, y, z) is not None or (prefer is not None and not prefer(x, y, z)):
                continue
            if not an.wide(n, wide_r):
                continue
            cand.append(n)
        if len(cand) < 4:
            continue
        chosen = []
        seeds = list(anchors)
        pool = list(cand)
        mt.rng.shuffle(pool)
        while pool and len(chosen) < need:
            best, bd = None, -1.0
            for c in pool:
                d = min([abs(c[0] - s[0]) + abs(c[2] - s[2]) for s in seeds + chosen])
                if d > bd:
                    best, bd = c, d
            if best is None or bd < space:
                break
            chosen.append(best)
            pool.remove(best)
        if len(chosen) >= need:
            cells = chosen
            break
        if len(chosen) > len(cells):
            cells = chosen
    if len(cells) < min(need, 6):
        raise ValueError("%s: only %d marker cells found (%d wanted)" % (mt.name, len(cells), need))
    sp = max(3, min(spawns, len(cells) - patrols - 1))
    for k, (x, y, z) in enumerate(cells[:sp]):
        if k < heavy:
            mt.marker(x, y, z, "spawn", weight=2)
        else:
            mt.marker(x, y, z, "spawn")
    rest = cells[sp:]
    # patrols: the two rest cells nearest to the middle of the room
    mid = (mt.W / 2.0, mt.D / 2.0)
    rest.sort(key=lambda c: dist(c[0], c[2], mid[0], mid[1]))
    for (x, y, z) in rest[:patrols]:
        mt.marker(x, y, z, "patrol")
    leftover = rest[patrols:]
    fx, fy, fz = leftover[0] if leftover else rest[-1]
    if faction:
        mt.marker(fx, fy, fz, "faction", faction=faction)
    if room_center:
        mt.marker(int(mid[0]), GROUND, int(mid[1]), "room_center")
    return cells


_THEME_CACHE = {}


def themed_copy(mt, theme_id):
    """A deep copy of the template with the theme's block swap applied (the same port of ThemeSwap the previews use): the cells the game will really place, so the light of a
    soul lantern (10) instead of a lantern (15) and a light block moved by the theme's shift are what the checks see. `mt` itself is not touched."""
    import copy
    import json
    import mega_theme_preview as MTP
    if "exists" not in _THEME_CACHE:
        _THEME_CACHE["exists"] = MTP.vanilla_ids().__contains__
        _THEME_CACHE["states"] = json.load(open(MTP.STATES, encoding="utf-8"))
    clone = copy.copy(mt)
    clone.t = copy.deepcopy(mt.t)
    if theme_id:
        MTP.retheme(clone.t, theme_id, _THEME_CACHE["exists"], _THEME_CACHE["states"])
    return clone


def theme_shift(theme_id):
    """The `light` shift of a theme file (0 for none)."""
    import json
    import mega_theme_preview as MTP
    if not theme_id:
        return 0
    return int(json.load(open(os.path.join(MTP.THEMES, theme_id + ".json"), encoding="utf-8")).get("light", 0))


def shift_level(level, shift):
    """ThemeSwap.shiftLight: moved by the shift, never above 15, never under 6 (or the block's own lower level); level 0 stays 0."""
    if level <= 0:
        return level
    return max(min(level, 6), min(15, level + shift))


def fix_light(mt, theme_id=None, minimum=5, target=10, max_rounds=40, per_round=14, height=3, spread=6):
    """Invisible light blocks over the dark spots of the walking floor, judged on the SWAPPED template (what the game will place: warm lanterns may become soul lanterns, light blocks
    are shifted by the theme): while a standing cell of the lead's light report (`mega_shape_preview.standable_cells`) has block light at eye level under `minimum`, a light block goes
    `height` rows over the dark cell with the most dark cells round it (in a free air cell; a few cells up, down and aside are tried), up to `per_round` per round (blocks that are
    `spread` apart), at most `max_rounds` rounds. The level written is the one that is `target` AFTER the theme's shift. Why: the Gloom clock counts block light of 4 or less as dark
    and punishes it, and a cave or a ruin with a few dark pockets is the commonest defect of a generated room; the real fixtures stay where the design put them and this only fills what
    they missed (the rooms2 caverns do the same with `hidden_lights`). Returns the number added."""
    added = 0
    holes = set()
    for side, offset, _kw in mt.sockets:
        holes.update(mt._hole_cells(side, offset))
    shift = theme_shift(theme_id)
    level = max(1, min(15, target - shift)) if shift < 0 else target - shift
    level = max(6, min(15, level))
    after = shift_level(level, shift)
    th = themed_copy(mt, theme_id)
    for _ in range(max_rounds):
        lf = SP.light_field(th.t)
        cells = SP.standable_cells(th.t)
        dark = [c for c in sorted(cells) if lf.get((c[0], c[1] + 1, c[2]), 0) < minimum]
        if not dark:
            return added
        round_added = 0
        pool = list(dark)
        while pool and round_added < per_round:
            best = max(pool, key=lambda n: sum(1 for m in pool if abs(m[0] - n[0]) <= 3 and abs(m[2] - n[2]) <= 3 and abs(m[1] - n[1]) <= 2))
            spots = [(0, dy, 0) for dy in (height, height + 1, height - 1, 2, height + 2)]
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1), (-1, -1), (1, -1), (-1, 1)):
                spots += [(dx, dy, dz) for dy in (height, height - 1, 2, height + 1)]
            placed = False
            for (dx, dy, dz) in spots:
                x, y, z = best[0] + dx, best[1] + dy, best[2] + dz
                if free(mt, x, y, z) and (x, y, z) not in holes and 1 <= y < L.HEIGHT - 3 and L.WALL <= x < mt.W - L.WALL and L.WALL <= z < mt.D - L.WALL \
                        and th.t.get(x, y, z) is None:
                    mt.set(x, y, z, "minecraft:light", {"level": str(level), "waterlogged": "false"})
                    th.t.set(x, y, z, "minecraft:light", {"level": str(after), "waterlogged": "false"})
                    placed = True
                    break
            pool = [m for m in pool if abs(m[0] - best[0]) + abs(m[2] - best[2]) > spread and m != best]
            if placed:
                round_added += 1
                added += 1
        if round_added == 0:
            return added
    return added


def explicit_defaults(mt):
    """Writes the default `type` of a slab and the `half` of a stair and of a trapdoor (and `open`) into every cell that lacks them, so the file says what it means
    (the game reads a missing property as its default; a template reader that does not know that sees a slab that carries nothing)."""
    cells = mt.t.cells
    for pos, c in list(cells.items()):
        p = c.block.split(":", 1)[1]
        add = {}
        if p.endswith("_slab") and "type" not in c.props:
            add["type"] = "bottom"
        elif p.endswith("_stairs") and "half" not in c.props:
            add["half"] = "bottom"
        elif p.endswith("_trapdoor"):
            if "half" not in c.props:
                add["half"] = "bottom"
            if "open" not in c.props:
                add["open"] = "false"
        if add:
            cells[pos] = type(c)(c.block, dict(c.props, **add), c.nbt)


def finish(mt, theme=None, light=True, minimum=5):
    """Writes the defaults, carves the doorways (`MT.finish`), then fills dark pockets with hidden light blocks (`fix_light`: it must run AFTER the doorways exist, because the
    lead's walking model starts from the jigsaw of every socket)."""
    explicit_defaults(mt)
    mt.finish()
    if light:
        fix_light(mt, theme, minimum=minimum)
    return mt


def new(folder, name, mx, mz):
    """A template `mega/<folder>/<name>` of mx x mz modules."""
    return MT("mega/%s/%s" % (folder, name), mx, mz)


def seat(mt, x, y, z, looks, block=L.PLANKS_STAIRS, hint="chair"):
    """A seat for the rest system: a stair whose tall side is behind the sitter, and a `seat` marker in the cell above. `looks` is the direction the sitter looks."""
    mt.set(x, y, z, block, {"facing": opposite(looks), "half": "bottom"})
    mt.marker(x, y + 1, z, "seat", **{"as": hint, "facing": looks})


def hearth(mt, x, y, z, block="minecraft:soul_campfire", hint="brazier"):
    """A fire for the rest system: a lit campfire (or another fire block) and a `hearth` marker above it. The hint `brazier` is one no compat file lists on purpose: a refuge's fire
    must never turn into a kitchen stove."""
    mt.set(x, y, z, block, {"facing": "north", "lit": "true", "signal_fire": "false", "waterlogged": "false"})
    mt.marker(x, y + 1, z, "hearth", **{"as": hint})


def bed(mt, x, y, z, facing, color="gray"):
    """A bed: the FOOT at (x, y, z), the head one cell toward `facing`, and a `bed` marker above the foot (the long rest's anchor)."""
    f = Frame(mt, x, y, z, facing)
    f.put(0, 0, 0, "minecraft:%s_bed" % color, {"facing": facing, "part": "foot"})
    f.put(0, 0, 1, "minecraft:%s_bed" % color, {"facing": facing, "part": "head"})
    mt.marker(x, y + 1, z, "bed", **{"as": "bed"})


def sign(mt, x, y, z, facing, lines, block="minecraft:dark_oak_wall_sign"):
    """A waxed wall sign with up to four lines of plain text (the 1.21.1 `front_text.messages` format of `mega_safe_lib.sign`). `facing` is away from the wall."""
    def msg(t):
        return _nbt.String('{"text":"%s"}' % t.replace('"', "'"))
    empty = _nbt.String('{"text":""}')
    lines = list(lines) + [""] * (4 - len(lines))
    text = _nbt.Compound({"messages": _nbt.List([msg(t) if t else empty for t in lines[:4]], tag_type=_nbt.TAG_STRING), "color": "black", "has_glowing_text": _nbt.Byte(0)})
    back = _nbt.Compound({"messages": _nbt.List([empty] * 4, tag_type=_nbt.TAG_STRING), "color": "black", "has_glowing_text": _nbt.Byte(0)})
    data = _nbt.Compound({"id": "minecraft:sign", "is_waxed": _nbt.Byte(1), "front_text": text, "back_text": back})
    return put(mt, x, y, z, block, {"facing": facing}, nbt_=data)


def lectern(mt, x, y, z, facing, book=True):
    """A lectern that really holds a writable book (the block state alone only DRAWS a book; see `mega_arch_kit.lectern`). `facing` is the direction the reader looks away from the
    lectern's back, as vanilla has it."""
    if not book:
        return put(mt, x, y, z, "minecraft:lectern", {"facing": facing, "has_book": "false", "powered": "false"})
    bk = _nbt.Compound({"id": "minecraft:writable_book", "count": _nbt.Int(1)})
    return put(mt, x, y, z, "minecraft:lectern", {"facing": facing, "has_book": "true", "powered": "false"},
               nbt_=_nbt.Compound({"id": "minecraft:lectern", "Book": bk, "Page": _nbt.Int(0)}))


def ramp(mt, x, z, direction, steps, row0, width=3, stair=L.TILE_STAIRS, fill=L.BRICK, fill_cols=None, rail_left=False, rail_right=False):
    """A straight flight of `steps` stairs climbing toward `direction` from the cell (x, z): the first step's stair block is at row `row0`, each next cell is one row higher. The flight
    is `width` cells wide, measured to the RIGHT of a walker facing `direction` (r = 0 is the left edge). Under every step there is a solid mass of `fill` (only in the columns `fill_cols`
    when given: a flight carried by one stringer wall leaves the space under it open). `rail_left` / `rail_right` turn the edge column into a polished curb with a wall post on top (a
    rail); the curb is as high as the step beside it. A stair's last step reaches the height of the NEXT block row, so a deck that the flight leads to must be at the row of its last
    step. Returns the (x, z) of the last step."""
    f = Frame(mt, x, row0, z, direction)
    last = (x, z)
    for k in range(steps):
        for r in range(width):
            px, py, pz = f.at(r, 0, k)
            if fill is not None and (fill_cols is None or r in fill_cols):
                for yy in range(FR + 1, row0 + k):
                    put(mt, px, yy, pz, fill)
            if (r == 0 and rail_left) or (r == width - 1 and rail_right):
                put(mt, px, row0 + k, pz, L.POLISHED)
                put(mt, px, row0 + k + 1, pz, L.POLISHED_WALL, {"up": "true"})
            else:
                put(mt, px, row0 + k, pz, stair, st(direction))
        last = (f.at(0, 0, k)[0], f.at(0, 0, k)[2])
    return last


def grate(mt, x, y, z, over=False):
    """An iron grate: a closed iron trapdoor lying in the TOP half of its cell, so its top face is level with the cell's top: a walkway you can see through and that light passes."""
    return put(mt, x, y, z, "minecraft:iron_trapdoor", {"facing": "north", "half": "top", "open": "false", "powered": "false", "waterlogged": "false"}, over=over)


def hanging_cage(mt, x, z, y_solid, drop=6, skull="skeleton"):
    """An iron-bar cage three across and four high hung by one chain from the solid cell (x, y_solid, z): bars on every side, on the top and on the bottom (so the skull inside rests on
    something: the arch rules refuse a skull over air), the chain running from the top bar up to the solid cell."""
    y_top = y_solid - drop - 1
    for k in range(drop):
        put(mt, x, y_solid - 1 - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    y0 = y_top - 3
    for dy in range(4):
        for dx in (-1, 0, 1):
            for dz in (-1, 0, 1):
                edge = dx != 0 or dz != 0
                if edge or dy in (0, 3):
                    put(mt, x + dx, y0 + dy, z + dz, L.IRON_BARS)
    put(mt, x, y0 + 1, z, "minecraft:%s_skull" % skull, {"rotation": str(mt.rng.randint(0, 15))})


# =====================================================================================================================
# water: basins, kelp, windows (the Drowned Crypt)
# =====================================================================================================================
WATER = "minecraft:water"
WATER_PROPS = {"level": "0"}


def under_floor(mt, rng, y0=1, y1=FR - 1, blocks=None):
    """Fills every cell of the box in rows y0..y1 (under the floor row) that is still unset with rock, like the cistern halls do: a template with water in it must have no void under
    its basin (a water source flows into any open neighbour and a hole in the floor shows the void from the room above)."""
    blocks = blocks or (L.COBBLED, L.ROCK, L.ROCK)
    for z in range(mt.D):
        for x in range(mt.W):
            for y in range(y0, y1 + 1):
                if mt.get(x, y, z) is None:
                    mt.set(x, y, z, rng.choice(blocks))


def flood(mt, cells, bed_row, top_row, bed_blocks=None, rng=None):
    """Turns the columns (x, z) of `cells` into a basin: the cell at `bed_row` becomes a bed block (rock, unless one is set), the cells bed_row + 1 .. top_row become water sources
    (any floor block there is removed first). The caller keeps every side of the basin closed (full blocks or more water: `Analysis.fluid_problems` checks it). Returns the cell count."""
    rng = rng or mt.rng
    bed_blocks = bed_blocks or (L.COBBLED, L.ROCK, L.ROCK)
    n = 0
    for (x, z) in sorted(cells):
        if not inside(mt, x, bed_row, z) or (x, bed_row, z) in mt.reserved:
            continue
        c = mt.get(x, bed_row, z)
        if c is None or c.block.startswith("minecraft:water"):
            mt.set(x, bed_row, z, rng.choice(bed_blocks))
        for y in range(bed_row + 1, top_row + 1):
            mt.set(x, y, z, WATER, WATER_PROPS)
            n += 1
    return n


def kelp(mt, x, bed_row, z, top_row, age=25):
    """A kelp plant growing from the bed cell (x, bed_row, z) up to row `top_row` (the plant's head): kelp_plant cells and one `kelp` head with its age set to the maximum so it
    never grows. Every kelp cell is a water source of its own (it holds water), and both of the lead's fluid checks refuse a kelp cell that touches a plain water cell (kelp is not a
    full block for `Analysis.fluid_problems`), so a kelp plant may only stand where NO `minecraft:water` block touches it: a sealed trough or a glass case that holds nothing but kelp."""
    for y in range(bed_row + 1, top_row):
        mt.set(x, y, z, "minecraft:kelp_plant")
    mt.set(x, top_row, z, "minecraft:kelp", {"age": str(age)})


def sea_pickle(mt, x, y, z, n=3):
    """A sea pickle cluster on the sturdy cell below (x, y, z), NOT waterlogged: on dry stone it is a green barnacle crust that does not glow (a waterlogged one glows, but a waterlogged
    block next to water is refused by the lead's arch check, and next to nothing else it is a puddle that spreads)."""
    mt.set(x, y, z, "minecraft:sea_pickle", {"pickles": str(max(1, min(4, n))), "waterlogged": "false"})


def lancet(mt, side, lat, y0, width=3, height=9, glass=("minecraft:cyan_stained_glass",), back="minecraft:sea_lantern", mullion=True):
    """A pointed (lancet) window in the two-thick wall `side` ('n','e','s','w') centred at `lat` (x for n/s, z for e/w), its sill at row y0: full stained-glass blocks in the inner
    layer, a sea lantern behind each in the outer layer (so the window glows by itself and the wall stays closed to the void), a stone mullion down the middle (for width >= 3) and the
    top two rows narrowed by one cell each side. `glass` is a tuple the rows cycle through. Returns the number of cells written."""
    n = 0
    half = width // 2
    for u in range(height):
        reach = half - (1 if u >= height - 2 else 0) - (1 if u == height - 1 and half >= 2 else 0)
        for r in range(-reach, reach + 1):
            x_in, y, z_in = mt._lateral(side, 0, lat + r, 1, y0 + u)
            x_out, _y, z_out = mt._lateral(side, 0, lat + r, 0, y0 + u)
            if (x_in, y, z_in) in mt.reserved or (x_out, y, z_out) in mt.reserved:
                continue
            if mullion and r == 0 and width >= 3 and u < height - 3:
                mt.set(x_in, y, z_in, L.POLISHED)
            else:
                mt.set(x_in, y, z_in, glass[(u // 2 + abs(r)) % len(glass)])
            mt.set(x_out, y, z_out, back)
            n += 1
    return n


def debug_wide(mt, y=GROUND):
    """Prints the 3-wide reach from the first doorway at standing row `y` (# reached, w wide but not reached, . standable, blank otherwise): the map that shows where a prop closes the route."""
    an = D.Analysis(mt)
    anch = [a for a in an.anchors() if a is not None]

    def near(n):
        return any(abs(n[0] - a[0]) + abs(n[2] - a[2]) <= 4 for a in anch)
    wr = an.reach(anch[0], allow=lambda n: near(n) or an.wide(n))
    print("   " + "".join(str(x % 10) for x in range(0, mt.W)))
    for z in range(0, mt.D):
        line = ""
        for x in range(0, mt.W):
            if (x, y, z) in wr:
                line += "#"
            elif an.wide((x, y, z)) and an.standable(x, y, z):
                line += "w"
            elif an.standable(x, y, z):
                line += "."
            else:
                line += " "
        print("%2d %s" % (z, line))


def loot_chest(mt, x, y, z, facing, table=LOOT):
    mt.t.chest(x, y, z, table, facing)


# =====================================================================================================================
# checking and writing
# =====================================================================================================================
def web_problems(mt, limit=8):
    """Cobwebs on the walking route. Why: a cobweb slows whoever stands in it to a crawl and the lead's walking model counts it as free air, so a web in a cell a player can walk through
    (the feet cell or the head cell of any standing cell reachable from a doorway) is invisible to every other check. The brief for the Spider Warrens is explicit: webs are scenery on the
    sides and ceilings, never across the only way through. Returns up to `limit` lines."""
    an = D.Analysis(mt)
    nodes = set()
    for a in an.anchors():
        if a is not None and a not in nodes:
            nodes.update(an.reach(a))
    out = []
    for (x, y, z) in sorted(nodes):
        for yy in (y, y + 1):
            c = mt.t.cells.get((x, yy, z))
            if c is not None and c.block == "minecraft:cobweb":
                out.append("cobweb at %s on a walkable cell (%s)" % ((x, yy, z), (x, y, z)))
                break
        if len(out) >= limit:
            break
    return out


def check_all(mt, spec, theme=None):
    """Every offline check of the lead's tools over a finished template, run on the template AS THE GAME WILL PLACE IT (the theme's block swap applied: `themed_copy`): the rooms2 analysis
    (`G2.check_room`: doors, 3-wide routes, markers, light share, leaks, fluids, fire, attachments), the arch survival rules, the pairing of beds, doors and double chests, and the shared
    light report (the darkest cell of the walking route). The arch rules and the pairing run on the raw template as well. Why the swapped one: a theme turns chiselled blocks into crying
    obsidian (a light source), warm lanterns into soul lanterns (dimmer) and shifts every light block, so the raw deepslate template is not the room the player sees. Returns (errors, warnings, info)."""
    th = themed_copy(mt, theme) if theme else mt
    errors, warns, info = G2.check_room(th, spec)
    if spec.get("safe"):
        # a Safe Zone or a calm room carries no faction and no spawn markers; what it must carry are the rest anchors the rest system reads (seat, hearth, bed)
        errors = [e for e in errors if "faction marker expected" not in e]
        warns = [w for w in warns if "spawn markers" not in w]
        kinds = {}
        for kind, _a, _p in G2.read_markers(th):
            kinds[kind] = kinds.get(kind, 0) + 1
        if kinds.get("spawn"):
            errors.append("a safe room carries no spawn marker")
        for need, lo in (("seat", spec.get("seats", 2)), ("hearth", spec.get("hearths", 1)), ("bed", spec.get("beds", 2))):
            if kinds.get(need, 0) < lo:
                errors.append("a safe room needs at least %d %s marker(s), has %d" % (lo, need, kinds.get(need, 0)))
    for line in web_problems(th):
        errors.append("web: " + line)
    for line in AC.problems(mt.t, mt.name):
        errors.append("arch (raw): " + line)
    if theme:
        for line in AC.problems(th.t, mt.name):
            if ("arch (raw): " + line) not in errors:
                errors.append("arch (swapped): " + line)
    for line in R1.multiblock_problems(mt.t):
        errors.append("multiblock: " + line)
    rep = SP.light_report(th.t, mt.name)
    if rep["cells"]:
        info.append("light of the swapped template (%s): min %d, %.1f%% at or under 4, %.1f%% at or under 7" % (theme or "raw", rep["min"], 100.0 * rep["dark4"], 100.0 * rep["dim7"]))
        floor_min = spec.get("min_light", 5)
        if not spec.get("dark") and rep["min"] < floor_min and spec.get("strict_light", True):
            errors.append("light check on the swapped template: the walking route has block light %d at its darkest (%.1f%% at or under 4); the minimum is %d" % (rep["min"], 100.0 * rep["dark4"], floor_min))
    bs, bm = SP.connectivity(th.t)
    if bs or bm:
        errors.append("not walkable: sockets %s markers %s" % (bs, bm))
    return errors, warns, info


def disk_matches(mt):
    return G2.disk_matches(mt)


def run(rooms, argv=None, doc="", theme=None):
    """The command line every theme generator ends with. `rooms` is [(folder, name, builder, spec)]; `builder(name)` returns a finished MT.
    python3 tools/labyrinth/gen_mega_theme_<id>.py [names...] [--check] [--no-write] [--force] [--list]."""
    ap = argparse.ArgumentParser(description=doc.split("\n\n")[0] if doc else None)
    ap.add_argument("names", nargs="*", help="build only these rooms")
    ap.add_argument("--check", action="store_true", help="build and check in memory and compare with the files on disk; write nothing")
    ap.add_argument("--no-write", action="store_true", help="build and check, write nothing")
    ap.add_argument("--force", action="store_true", help="write a room even when a check fails (to look at it); the exit status stays 1")
    ap.add_argument("--list", action="store_true")
    a = ap.parse_args(argv)
    if a.list:
        for folder, name, _fn, _spec in rooms:
            print("%s/%s" % (folder, name))
        return 0
    known = {name for _f, name, _fn, _s in rooms}

    def wanted(name):
        return not a.names or any(name == n or name.endswith("_" + n) for n in a.names)

    unknown = sorted(n for n in a.names if not any(k == n or k.endswith("_" + n) for k in known))
    if unknown:
        ap.error("no room named %s (known: %s)" % (", ".join(unknown), ", ".join(sorted(known))))
    bad = 0
    for folder, name, fn, spec in rooms:
        if not wanted(name):
            continue
        mt = fn(name)
        errors, warns, info = check_all(mt, spec, theme)
        stale = a.check and not disk_matches(mt)
        if stale:
            errors.append("the file on disk is not what this script writes now: run it without --check")
        print("%-4s %s/%s  %d blocks, %d entities" % ("FAIL" if errors else "ok", folder, name, len(mt.t.cells), len(mt.t.entities)))
        for e in errors:
            print("     ERROR " + e)
        for w in warns:
            print("     warn  " + w)
        for i in info:
            print("     " + i)
        bad += len(errors)
        if not a.check and not a.no_write and (not errors or a.force):
            path = mt.save(L.RES)
            print("     wrote %s (%d KB)" % (os.path.relpath(path, L.ROOT), os.path.getsize(path) // 1024))
        elif errors and not a.check and not a.no_write:
            print("     NOT WRITTEN (--force writes it to look at it)")
    return 1 if bad else 0

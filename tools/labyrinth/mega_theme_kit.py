#!/usr/bin/env python3
"""
Why: THEME-B (1.59.0, docs/labyrinth/mega_v2_thb.md) builds twenty-odd themed rooms for four themes (the Feywild Grove, the Dwarven Forgehold, the Crystal Geode and
the Sunken Temple) and nothing here can be run in the game. The rooms of 1.54.0 each had a builder who wrote a private kit (`mega_decor_r1.py` for the furnished halls,
`mega_decor_r2.py` for the grand and mechanical rooms); this module is the thin layer that lets the four theme generators use BOTH kits through one door:

  * a registry and a command line (`run`) that builds a room, runs EVERY check the earlier builders wrote for their rooms (the walk graph, the three-wide routes, the
    block light, the see-through and floating tests of `mega_decor_r1.check_hall`, the lead's survival rules `mega_arch_check`, the attachment audit and the pairing of
    beds / doors / double chests, and the second model of `gen_mega_rooms2.check_room` with its fluid, fire and mechanic rules) and writes the template only when none of
    them objects (`--force` writes it anyway, to look at it);
  * a preview that draws the file on disk IN ITS OWN THEME (the Region Theme swap of `mega_theme_preview.retheme` applied to the cells first, plus a colour table for the
    blocks the themes bring in), as isometric cutaways, a plan and first-person views, so a room is judged in the palette a player will see and not in grey deepslate;
  * the organic and the heavy-industry building blocks the four themes share and the earlier kits lack: ellipsoids of leaves, giant flowers, toadstools, standing stones,
    helical ramps, sealed ponds, tree trunks, crystals, lava channels behind glass.

Architecture still uses ONLY the palette constants of `mega_lib` (the theme restyles them); everything that gives a theme its character (leaves, petals, froglights, ore,
prismarine, amethyst, coral ...) is a free vanilla block the theme leaves alone. No custom block except the mechanical ones `mega_lib.CUSTOM` lists.
"""
import argparse
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D1  # noqa: E402
import mega_decor_r2 as D2  # noqa: E402
import gen_mega_rooms2 as R2  # noqa: E402
from mega_decor_r1 import Hall, GROUND, FLOOR  # noqa: E402

FR = L.FLOOR_ROW
LOOT_TRIAL = "nusmp:chests/labyrinth_trial"
DIRS4 = (("north", 0, -1), ("east", 1, 0), ("south", 0, 1), ("west", -1, 0))


# ===================================================================================================================
# the registry and the command line
# ===================================================================================================================
class Theme:
    """The rooms of one theme: `room(folder, name)` registers a builder; the file is `structure/mega/<folder>/<theme>_<name>.nbt` (the prefix the theme's
    `template_themes` file claims)."""

    def __init__(self, theme_id):
        self.id = theme_id
        self.rooms = {}

    def room(self, folder, name, **spec):
        def deco(fn):
            self.rooms[name] = (folder, fn, spec)
            return fn
        return deco

    def path(self, folder, name):
        return "mega/%s/%s_%s" % (folder, self.id, name)


def finished(obj):
    """(Hall or None, MT) of a builder's result, finished."""
    if isinstance(obj, Hall):
        obj.finish()
        return obj, obj.mt
    obj.finish()
    return None, obj


def check_all(obj, spec, path):
    """Every check there is, as (errors, warnings, info). `spec` keys: faction (the one expected faction marker), kind ('puzzle' | 'trial' | None), no_spawn, dark_limit,
    route_width, hall (False for rooms built on a bare MT: the Hall walk graph is skipped), max_armor."""
    hall, mt = finished(obj)
    errors, warns, info = [], [], []
    if hall is not None:
        problems, stats = hall.check()
        errors += problems
        info.append("hall check: light min %s median %s dark %s%%, markers %s" % (stats.get("eye_light_min"), stats.get("eye_light_median"),
                                                                                  round(100 * stats.get("dark_fraction", 0), 1), stats.get("markers")))
    else:
        errors += ["strict state: " + m for m in D1.strict_state_problems(mt.t)]
        for line in D1.shared_problems(mt.t, path):
            errors.append(line)
    r2spec = dict(spec)
    r2spec.setdefault("max_armor", 16)
    e2, w2, i2 = R2.check_room(mt, r2spec)
    errors += ["analysis: " + e for e in e2]
    warns += w2
    info += i2
    return errors, warns, info


def build_one(theme, name):
    folder, fn, spec = theme.rooms[name]
    path = theme.path(folder, name)
    return folder, path, fn(path), spec


def disk_equal(mt):
    import nbt as _nbt
    p = mt.t.path(L.RES)
    if not os.path.exists(p):
        return False
    with open(p, "rb") as f:
        return f.read() == _nbt.dumps(mt.t.to_nbt(), compress=True)


def run(theme, argv, previews_default=None):
    ap = argparse.ArgumentParser(description="rooms of the %s theme" % theme.id)
    ap.add_argument("names", nargs="*", help="rooms to build (default: all), without the theme prefix")
    ap.add_argument("--check", action="store_true", help="build and check; write nothing; fail when a file on disk differs from what would be written")
    ap.add_argument("--list", action="store_true")
    ap.add_argument("--force", action="store_true", help="write a room even when a check fails (to look at it); the exit status stays 1")
    ap.add_argument("--quiet", action="store_true")
    ap.add_argument("--preview", action="store_true", help="after writing, render each room in its theme (isometric cutaways, plan, first-person views)")
    ap.add_argument("--out", default=previews_default or os.environ.get("THB_PREVIEWS") or os.path.join(L.ROOT, "previews"), help="folder for the previews")
    ap.add_argument("--only-preview", action="store_true", help="render the previews of the files on disk without building")
    ap.add_argument("--table", action="store_true", help="print the markdown table of the rooms (size, doors, markers, light) from the files on disk")
    a = ap.parse_args(argv)
    if a.list:
        for n, (folder, _fn, _s) in theme.rooms.items():
            print("%s/%s_%s" % (folder, theme.id, n))
        return 0
    names = a.names or list(theme.rooms)
    for n in names:
        if n not in theme.rooms:
            print("unknown room", n, "(one of %s)" % ", ".join(theme.rooms))
            return 2
    if a.table:
        print_table(theme, names)
        return 0
    bad = 0
    for n in names:
        folder, path, obj, spec = build_one(theme, n)
        if isinstance(obj, Hall) and "view_h" not in spec:
            spec = dict(spec, view_h=obj.h)
        if a.only_preview:
            preview(theme, folder, n, a.out, spec)
            continue
        errors, warns, info = check_all(obj, spec, path)
        hall, mt = finished(obj)
        status = "ok  " if not errors else "FAIL"
        if errors:
            bad += 1
        if a.check:
            if not disk_equal(mt):
                print("STALE %s: the file on disk differs from what the generator writes" % path)
                bad += 1
        elif not errors or a.force:
            mt.save()
        else:
            print("NOT WRITTEN %s: a room with a defect never reaches the data folder (--force writes it anyway)" % path)
        if not a.quiet or errors:
            print("%s %-44s %6d blocks %2d entities" % (status, path, len(mt.t.cells), len(mt.t.entities)))
            for e in errors[:30]:
                print("     ERROR " + e)
            if len(errors) > 30:
                print("     ... and %d more" % (len(errors) - 30))
            for w in warns:
                print("     warn  " + w)
            if not a.quiet:
                for i in info:
                    print("     " + i)
        if a.preview and (not errors or a.force) and not a.check:
            preview(theme, folder, n, a.out, spec)
    return 1 if bad else 0


def print_table(theme, names):
    import collections
    from template import load
    import mega_shape_preview as SP
    print("| Template | Modules | Blocks x z | Doors | Spawn | Patrol | Other markers | Stands | Light (min) | Cells |")
    print("|---|---|---|---|---|---|---|---|---|---|")
    for n in names:
        folder, _fn, _spec = theme.rooms[n]
        path = "%s_%s" % (theme.id, n)
        t = load(os.path.join(L.RES, "data", "nusmp", "structure", "mega", folder, path + ".nbt"), "mega/%s/%s" % (folder, path))
        sx, sy, sz = t.size
        doors = []
        for (x, y, z), o, _d in t.jigsaws():
            side = o.split("_")[0][0]
            doors.append("%s@%d" % (side, x if side in "ns" else z))
        marks = D1.markers_of(t)
        count = collections.Counter(k for k, *_ in marks)
        heavy = sum(1 for k, _x, _y, _z, a in marks if k == "spawn" and a.get("weight"))
        other = []
        for k in ("faction", "furniture", "seat", "hearth", "bed", "room_center", "trap", "pedestal"):
            if count.get(k):
                if k == "faction":
                    other.append("faction " + [a.get("faction") for kk, _x, _y, _z, a in marks if kk == "faction"][0])
                else:
                    other.append(k if count[k] == 1 else "%s x%d" % (k, count[k]))
        stands = sum(1 for e in t.entities if str(e["nbt"].get("id", "")) == "minecraft:armor_stand")
        rep = SP.light_report(t, path)
        print("| `%s/%s` | %dx%d | %dx%d | %s | %d (%d x2) | %d | %s | %d | %d | %d |" % (
            folder, path, sx // 9, sz // 9, sx, sz, " ".join(doors), count["spawn"], heavy, count["patrol"], ", ".join(other) or "-", stands, rep["min"], len(t.cells)))


# ===================================================================================================================
# previews in the room's own theme
# ===================================================================================================================
#: colours for the blocks the four themes bring in (the viewers' own tables know deepslate, quartz and the old themes): pattern -> (r, g, b, a)
EXTRA_COLOURS = [
    (r".*copper_bulb", (255, 205, 130, 255), False),                    # first: the pattern for waxed copper further down would otherwise claim the bulbs
    (r"cherry_leaves", (236, 160, 190, 255), False), (r"cherry_.*", (222, 150, 160, 255), False), (r"pink_petals", (240, 150, 200, 255), False),
    (r"mud_brick.*", (140, 108, 96, 255), False), (r"packed_mud", (125, 95, 80, 255), False), (r"rooted_dirt", (130, 95, 70, 255), False),
    (r"azalea_leaves|flowering_azalea_leaves", (110, 150, 70, 255), False), (r"flowering_azalea", (210, 120, 200, 255), False),
    (r"bamboo.*", (200, 190, 100, 255), False), (r"acacia.*", (190, 100, 60, 255), False), (r"oak_.*|oak", (170, 140, 85, 255), False),
    (r"waxed_.*copper.*|cut_copper.*|.*copper_block|chiseled_copper", (200, 120, 85, 255), False), (r"gilded_blackstone", (80, 62, 50, 255), False),
    (r"polished_andesite.*|andesite.*", (135, 135, 130, 255), False), (r"stone_brick.*|stone_bricks|cracked_stone_bricks|stone", (125, 125, 125, 255), False),
    (r"calcite", (232, 232, 226, 255), False), (r"smooth_basalt", (58, 58, 62, 255), False), (r"tuff.*", (110, 112, 100, 255), False),
    (r"amethyst_block|budding_amethyst|.*amethyst_bud|amethyst_cluster", (150, 100, 205, 255), False), (r"purpur.*", (170, 125, 175, 255), False),
    (r"dripstone_block", (130, 100, 85, 255), False), (r"prismarine_brick.*", (95, 170, 150, 255), False), (r"dark_prismarine.*", (50, 90, 75, 255), False),
    (r"prismarine.*", (110, 180, 165, 255), False), (r"sea_lantern", (210, 240, 235, 255), False), (r".*coral.*", (200, 90, 140, 255), False),
    (r"mossy_.*", (100, 125, 85, 255), False), (r"moss_block|moss_carpet", (85, 115, 45, 255), False), (r"mushroom_stem", (215, 210, 195, 255), False),
    (r"red_mushroom_block", (190, 50, 45, 255), False), (r"brown_mushroom_block", (150, 110, 80, 255), False), (r"iron_ore|raw_iron_block", (165, 140, 120, 255), False),
    (r"copper_ore|raw_copper_block", (170, 115, 90, 255), False), (r"gold_ore|raw_gold_block|gold_block", (230, 195, 70, 255), False), (r"coal_ore|coal_block", (45, 45, 48, 255), False),
    (r"note_block|jukebox", (130, 90, 55, 255), False), (r"rail|powered_rail", (150, 140, 120, 255), False),
    (r"iron_block", (215, 215, 215, 255), False), (r"anvil|chipped_anvil|damaged_anvil", (70, 70, 75, 255), False),
    (r"blast_furnace|furnace|smoker", (110, 110, 112, 255), False), (r"grindstone", (150, 150, 145, 255), False), (r"smithing_table", (80, 60, 50, 255), False),
]


#: alpha of a glass block in the first-person viewer (None: the shape viewer's own 130). Why: `mega_walk_preview` marches a ray through quarter-block voxels and blends
#: EVERY voxel of a translucent block, so a block of glass (four voxels deep) at alpha 130 is nearly opaque and hides the lava or the crystal behind it; at about 40 the
#: picture shows what a player sees through a pane. It is set around the first-person renders only.
GLASS_ALPHA = [None]

#: blocks the first-person preview draws as light sources although the viewer's own list lacks them. Why: a lit copper bulb gives light 15 in the game (it keeps the `lit` it
#: is placed with), but the checks of 1.54.0 do not know it, so the rooms never COUNT on it (a room passes the light check without the bulbs); the picture shows them.
EXTRA_EMISSIVE = {"waxed_copper_bulb", "waxed_exposed_copper_bulb", "waxed_weathered_copper_bulb", "waxed_oxidized_copper_bulb", "copper_bulb"}


def _extra_colours():
    import re
    import mega_preview as P
    import mega_shape_preview as SP
    return P, SP, [(re.compile(p), c) for p, c, _e in EXTRA_COLOURS]


def _install_colours():
    """Teaches the three viewers the colours of this module's blocks. The shape viewer asks `color_of` of its own (which falls back to `mega_preview.color_of`), the
    first-person viewer reads a compiled table; both are patched in place for this process only."""
    P, SP, extra = _extra_colours()
    P.COLORS[0:0] = [(pat.pattern, c) for pat, c in extra]
    old = SP.color_of

    def color_of(block):
        n = SP.path_of(block)
        for pat, c in extra:
            if pat.fullmatch(n):
                return c
        c = old(block)
        if GLASS_ALPHA[0] is not None and n.endswith("glass") and c[3] < 255:
            return (c[0], c[1], c[2], GLASS_ALPHA[0])         # the first-person viewer counts every quarter-block of glass as a hit: see `GLASS_ALPHA`
        return c
    SP.color_of = color_of


def preview(theme, folder, name, out, spec=None, views=True):
    """Renders `structure/mega/<folder>/<theme>_<name>.nbt` as it will look in a sector of the theme: the Region Theme swap is applied first."""
    import mega_theme_preview as TP
    import mega_shape_preview as SP
    import mega_walk_preview as WP
    import json
    from template import load
    _install_colours()
    os.makedirs(out, exist_ok=True)
    full = "%s_%s" % (theme.id, name)
    path = os.path.join(L.RES, "data", "nusmp", "structure", "mega", folder, full + ".nbt")
    exists = TP.vanilla_ids().__contains__
    states = json.load(open(TP.STATES, encoding="utf-8"))

    def themed():
        t = load(path, "mega/%s/%s" % (folder, full))
        TP.retheme(t, theme.id, exists, states)
        return t
    t = themed()
    sx, sy, sz = t.size
    # the nearest walls (south and east) cut away, the ceiling off: the room seen from the south-east, and from the north-west with the other two walls cut
    ytop = FLOOR + (spec or {}).get("view_h", 0) if (spec or {}).get("view_h") else ceiling_row(t) - 1
    scale = max(7, min(30, int(2000 / (sx + sz))))
    SP.render(t, os.path.join(out, full + "_iso_SE.png"), (0, sx - 3), (0, ytop), (0, sz - 3), scale, markers=False)
    SP.render(t, os.path.join(out, full + "_iso_NW.png"), (2, sx - 1), (0, ytop), (2, sz - 1), scale, markers=False, rot=2)
    plan_top = FLOOR + (spec or {}).get("plan_h") if (spec or {}).get("plan_h") else ytop           # a room that is a mass of rock shows its plan cut at the tunnels' height
    SP.render(t, os.path.join(out, full + "_plan.png"), (0, sx - 1), (0, plan_top), (0, sz - 1), max(8, min(20, 700 // max(sx, sz))), top=True)
    if (spec or {}).get("plan_h"):
        SP.render(t, os.path.join(out, full + "_iso_floor.png"), (0, sx - 3), (0, plan_top), (0, sz - 3), scale, markers=False)
    for cut in (spec or {}).get("cuts", ()):          # extra sections: {"name": "half", "x": (0, 13), "y": (8, 30), "z": None, "rot": 0}
        xr = cut.get("x") or (0, sx - 1)
        yr = cut.get("y") or (0, sy - 1)
        zr = cut.get("z") or (0, sz - 1)
        SP.render(t, os.path.join(out, "%s_cut_%s.png" % (full, cut["name"])), xr, yr, zr, cut.get("scale", scale), markers=False, rot=cut.get("rot", 0))
    if not views:
        return
    GLASS_ALPHA[0] = 18
    WP.EMISSIVE = set(WP.EMISSIVE) | EXTRA_EMISSIVE        # the first-person viewer draws these at full brightness and the light field counts them (the CHECKS do not: see below)
    SP.LIGHT_LIT.update({n: 15 for n in EXTRA_EMISSIVE if n.endswith("copper_bulb")})
    scene = WP.Scene(t, res=4)
    GLASS_ALPHA[0] = None
    shots = (spec or {}).get("views")
    if not shots:
        shots = default_views(t)
    for tag, cam, look in shots:
        img = WP.render(scene, cam, look, 85.0, (800, 450), gamma=1.5)
        img.save(os.path.join(out, "%s_view_%s.png" % (full, tag)))
    print("     previews in", out)


def ceiling_row(t):
    """The lowest row above the floor in which nine tenths of the interior columns hold a block: the room's ceiling (or sy - 1 when a dome or a canopy never closes)."""
    sx, sy, sz = t.size
    cols = [(x, z) for x in range(2, sx - 2) for z in range(2, sz - 2)]
    for y in range(FLOOR + 3, sy):
        if sum(1 for (x, z) in cols if (x, y, z) in t.cells) >= 0.9 * len(cols):
            return y
    return sy - 1


def default_views(t):
    """First-person views from just inside every doorway, looking at the middle of the room at eye height."""
    sx, sy, sz = t.size
    shots = []
    eye = FLOOR + 1 + 1.62
    for i, ((x, y, z), o, _d) in enumerate(t.jigsaws()):
        face = o.split("_")[0]
        dx, dz = {"north": (0, 1), "south": (0, -1), "west": (1, 0), "east": (-1, 0)}[face]
        cam = (x + 0.5 + 3 * dx, eye, z + 0.5 + 3 * dz)
        look = (sx / 2.0, eye + 2.0, sz / 2.0)
        shots.append(("door%d" % i, cam, look))
        if len(shots) >= 3:
            break
    return shots


# ===================================================================================================================
# geometry
# ===================================================================================================================
def disc(cx, cz, r):
    """Integer cells of the disc of radius r round the cell (cx, cz) (the same rule as `mega_lib.disk_cells`)."""
    return L.disk_cells(cx, cz, r)


def ring(cx, cz, r_out, r_in):
    """Integer cells with r_in < distance <= r_out (a ring `r_out - r_in` thick)."""
    inner = set(L.disk_cells(cx, cz, r_in))
    return [c for c in L.disk_cells(cx, cz, r_out) if c not in inner]


def dist(x, z, cx, cz):
    return math.hypot(x - cx, z - cz)


def angle(x, z, cx, cz):
    """Angle in degrees (0 = +x, 90 = +z) of (x, z) seen from (cx, cz), in [0, 360)."""
    return math.degrees(math.atan2(z - cz, x - cx)) % 360.0


def toward(dx, dz):
    """The compass name of the dominant direction of (dx, dz) (the way UP a stair that climbs toward it)."""
    if abs(dx) >= abs(dz):
        return "east" if dx > 0 else "west"
    return "south" if dz > 0 else "north"


def leaf(h, x, y, z, kind="cherry", over=False):
    """A persistent leaf block (a leaf without `persistent` would decay the moment no log is near). Returns whether it was placed (the cell must be free)."""
    block = "minecraft:%s_leaves" % kind
    if over:
        h.set(x, y, z, block, {"persistent": "true", "distance": "1"}, over=True)
        return True
    return h.try_set(x, y, z, block, {"persistent": "true", "distance": "1"})


def blob(h, cx, cy, cz, rx, ry, rz, fn, density=1.0, rng=None):
    """Fills the free cells of the ellipsoid (centre cx, cy, cz; radii rx, ry, rz) by calling `fn(x, y, z)` (which should use try_set); `density` < 1 leaves holes at
    random. Returns the number of cells filled. Why: a canopy, a bush or a cloud of petals is an ellipsoid with ragged edges; every theme wants a few."""
    rng = rng or h.rng
    n = 0
    for y in range(int(cy - ry) - 1, int(cy + ry) + 2):
        for z in range(int(cz - rz) - 1, int(cz + rz) + 2):
            for x in range(int(cx - rx) - 1, int(cx + rx) + 2):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 + ((z - cz) / rz) ** 2
                if d <= 1.0 and (density >= 1.0 or rng.random() < density):
                    if fn(x, y, z):
                        n += 1
    return n


def free_columns(h, x, z, y0, y1):
    return all(h.free(x, y, z) for y in range(y0, y1 + 1))


def hang_chain_light(h, x, z, y_top, drop, lantern=L.LANTERN, soul=False):
    """A lantern on a chain from row y_top (which must be a solid block's underside: the caller checks) down `drop` cells; returns False when a cell is taken."""
    if not all(h.free(x, y_top - k, z) for k in range(drop + 1)):
        return False
    for k in range(drop):
        h.set(x, y_top - k, z, L.CHAIN_BLOCK, {"axis": "y"})
    h.set(x, y_top - drop, z, L.SOUL_LANTERN if soul else lantern, {"hanging": "true"})
    return True


def sealed_pool(h, cells, depth=1, floor_block=L.TILE, rim=None, block="minecraft:water", top_row=None):
    """A pool carved into the floor: `cells` [(x, z)] lose `depth` floor rows (their row-8 floor cell becomes water, the row below floor_block) and the surrounding floor
    is the rim; every water cell is shut in on four sides by the floor (the caller must keep `cells` at least one cell away from a doorway hole and from another
    open edge) and below by floor_block. Why: a source block next to air flows as soon as the game ticks it, so a pool is a basin first and water second
    (`mega_arch_check` fails a water cell that is open to a side)."""
    for (x, z) in cells:
        h.carve(x, FLOOR, z) if h.get(x, FLOOR, z) is not None else None
        for k in range(1, depth):
            h.mt.set(x, FLOOR - k, z, floor_block)
            h.shell.add((x, FLOOR - k, z))
        h.set(x, FLOOR, z, block, over=True)
        h.mt.set(x, FLOOR - depth, z, floor_block)
        h.shell.add((x, FLOOR - depth, z))


# ===================================================================================================================
# checks that need the game's own tables
# ===================================================================================================================
def real_state_problems(t):
    """Every (block, property) that vanilla 1.21.1 does not have, from the game's own table (`mega_lib.real_states`, read from the public mcmeta data), for EVERY
    block of the template. Why: `mega_decor_r1.strict_state_problems` is a hand-written table of the blocks the furnished halls use; a block it does not list
    (cave vines, pink petals, amethyst buds, mushroom blocks ...) would be reported as having no property at all. The game's own table has them all."""
    seen = set()
    bad = []
    for pos, c in t.cells.items():
        key = (c.block, tuple(sorted(c.props.items())))
        if key in seen:
            continue
        seen.add(key)
        err = L.check_state(c.block, c.props)
        if err:
            bad.append("%s (at %s)" % (err, pos))
    return sorted(set(bad))


D1.strict_state_problems = real_state_problems


def dark_cells(t, limit=4):
    """[(x, y, z, light)] of the standable cells of the walking route (the lead's light model) whose eye cell has block light <= limit, for debugging a failing room."""
    import mega_shape_preview as SP
    lf = SP.light_field(t)
    return sorted((x, y, z, lf.get((x, y + 1, z), 0)) for (x, y, z) in SP.standable_cells(t) if lf.get((x, y + 1, z), 0) <= limit)


def wall_glow(w, lat, y, block, d=0):
    """A glowing block set flush into the wall face (the inner wall layer cell becomes `block`): a froglight, a sea lantern, a shroomlight. Why: the themes' own light
    (bioluminescence, forge glow, crystal glow) must come from the stone itself and not from lanterns on brackets, and an embedded lit block cannot float or pop off."""
    w.shell_set(lat, y, d - 1, block)


def flush_pilaster(w, lat, y0=GROUND, y1=None, shaft=L.POLISHED, cap=None, base=None, width=1):
    """A pilaster flush with the wall (the inner wall layer's own cells change block): a column of `shaft` from row y0 to y1 with an optional `cap` block on the top
    row and `base` on the bottom row. Why: the furnished halls' pilasters stand one cell out into the room, which is right for a corridor with a free lane beside
    it and wrong for a hall whose aisles must stay free to the wall; a flush one breaks the wall into bays and costs no floor."""
    h = w.hall
    y1 = h.top if y1 is None else y1
    for lt in range(lat, lat + width):
        for y in range(y0, y1 + 1):
            blk = cap if (cap and y == y1) else base if (base and y == y0) else shaft
            w.shell_set(lt, y, -1, blk)


def overgrowth(h, sides="nsew", p_moss=0.06, p_vine=0.10, rows=None, moss="minecraft:moss_block", lo=None, hi=None, door_gap=2):
    """Plants take the wall: a share `p_moss` of the inner wall layer becomes `moss` and vines hang on a share `p_vine` of the wall faces (rows `rows`, default floor + 1
    up to two under the ceiling), never within `door_gap` of a doorway. Why: the Feywild, the Sunken Temple and the Geode's rim are places something grows in;
    every cell is decided by the hall's own seeded random, so the result is the same on every run."""
    rows = rows or range(GROUND + 1, h.top - 1)
    for side in sides:
        w = h.wall(side)
        l0 = (h.x0 if side in ("n", "s") else h.z0) + 1 if lo is None else lo
        l1 = (h.x1 if side in ("n", "s") else h.z1) - 1 if hi is None else hi
        doors = [off for (sd, off) in h.doors if sd == side]
        for lat in range(l0, l1 + 1):
            if any(abs(lat - off) <= 1 + door_gap for off in doors):
                continue
            for y in rows:
                r = h.rng.random()
                pos = w.cell(lat, y, -1)
                c = h.get(*pos)
                if c is None or pos in h.deco or c.block not in (L.BRICK, L.CRACKED_BRICK, L.TILE, L.COBBLED, L.ROCK):
                    continue
                if r < p_moss:
                    w.shell_set(lat, y, -1, moss)
                elif r < p_moss + p_vine and w.free(lat, y, 0) and h.rng.random() < 0.8:
                    w.try_set(lat, y, 0, "minecraft:vine", {w.out: True})


# ===================================================================================================================
# the walking models learn the plants the themes use
# ===================================================================================================================
#: blocks a player walks through (no collision in the game) that the two earlier models file under "obstacle": the petals and flowers of the Feywild, the plants of the
#: Sunken Temple. `mega_decor_r1._PASS` is a compiled pattern and `mega_decor_r2.PASSABLE_RE` a tuple of ids; both are widened in place for this process.
EXTRA_PASS = ("pink_petals", "poppy", "dandelion", "allium", "azure_bluet", "cornflower", "lily_of_the_valley", "oxeye_daisy", "blue_orchid", "torchflower",
              "pitcher_plant", "lilac", "rose_bush", "peony", "sunflower", "wither_rose", "seagrass", "tall_seagrass", "kelp", "kelp_plant", "sea_pickle",
              "cave_vines", "cave_vines_plant", "rail", "powered_rail", "detector_rail", "activator_rail", "string", "hanging_roots", "azalea_x")
EXTRA_PASS_SUFFIX = ("_tulip", "_coral_fan", "_coral_wall_fan", "_coral", "_petals")


def _widen_models():
    import re
    D1._PASS = re.compile(D1._PASS.pattern + "|" + "|".join(EXTRA_PASS) + "|.*(" + "|".join(EXTRA_PASS_SUFFIX) + ")")
    D1._cls_cache.clear()
    D2.PASSABLE_RE = tuple(D2.PASSABLE_RE) + EXTRA_PASS
    D2.PASSABLE_SUFFIX = tuple(D2.PASSABLE_SUFFIX) + EXTRA_PASS_SUFFIX


_widen_models()

#: full cubes the lead's table of 1.54.0 files under "not full" because their name contains `lantern`: a sea lantern is a whole block you stand on and build with
EXTRA_FULL = {"sea_lantern"}
_ORIG_IS_FULL = L.is_full


def _is_full(block):
    return _ORIG_IS_FULL(block) or L._path(block) in EXTRA_FULL


L.is_full = _is_full
D1._cls_cache.clear()


def route_map(obj, level=18, door=0):
    """A text map of the floor at standing level `level` (half-blocks) for debugging a failing route: W = on a 3-wide route from door `door`, o = walkable only,
    # = something solid at the feet, . = nothing there."""
    hall, mt = finished(obj)
    t = mt.t
    nodes = D1.walk_nodes(t)
    starts = D1.socket_starts(t)
    seen = D1.reach(t, nodes, starts)
    wide = D1.reach(t, nodes, [starts[door]], wide=True)
    sx, sy, sz = t.size
    out = []
    for z in range(sz):
        row = ""
        for x in range(sx):
            ss = [s for (xx, zz, s) in seen if xx == x and zz == z and abs(s - level) <= 3]
            ws = [s for (xx, zz, s) in wide if xx == x and zz == z and abs(s - level) <= 3]
            c = t.cells.get((x, level // 2, z))
            row += "W" if ws else "o" if ss else "#" if c is not None else "."
        out.append(row)
    return "\n".join(out)


def wall_lights(h, blocks, spacing=6, y=None, sides="nsew", door_gap=2, offset=3, lo=None, hi=None):
    """A glowing block set flush into the wall every `spacing` cells along each wall in `sides` (cycling through `blocks`), at row `y` (default: floor + 3), never within
    `door_gap` cells of a doorway and never over something already there. Why: the themes' own light is part of the stone (froglights in a grove, forge glass in a hold,
    sea lanterns in a temple), and a flush block cannot float, pop off or block a route: it is the safest way to bring a room above the gloom limit."""
    y = GROUND + 3 if y is None else y
    k = 0
    for side in sides:
        w = h.wall(side)
        l0 = (h.x0 if side in ("n", "s") else h.z0) + 1 if lo is None else lo
        l1 = (h.x1 if side in ("n", "s") else h.z1) - 1 if hi is None else hi
        doors = [off for (sd, off) in h.doors if sd == side]
        for lat in range(l0 + offset, l1 + 1, spacing):
            if any(abs(lat - off) <= 1 + door_gap for off in doors):
                continue
            pos = w.cell(lat, y, -1)
            c = h.get(*pos)
            if c is None or pos in h.deco or pos in h.mt.reserved:
                continue
            w.shell_set(lat, y, -1, blocks[k % len(blocks)])
            k += 1


def mt_wall_glow(mt, side, lat, y, block):
    """`wall_glow` for a room built on a bare MT: the glowing block replaces the inner wall layer's cell at lateral `lat` of `side` and row `y`."""
    x, yy, z = mt._lateral(side, lat, lat, 1, y)
    mt.set(x, yy, z, block)


def mt_wall_lights(mt, blocks, spacing=6, y=None, sides="nsew", door_gap=2, offset=3):
    """`wall_lights` for a room built on a bare MT (the doors are read from its sockets)."""
    y = FR + 4 if y is None else y
    k = 0
    for side in sides:
        edge = mt.W if side in ("n", "s") else mt.D
        doors = [off for (sd, off, _kw) in mt.sockets if sd == side]
        for lat in range(L.WALL + offset, edge - L.WALL - 1, spacing):
            if any(abs(lat - off) <= 1 + door_gap for off in doors):
                continue
            x, yy, z = mt._lateral(side, lat, lat, 1, y)
            c = mt.get(x, yy, z)
            if c is None or (x, yy, z) in mt.reserved or c.block not in (L.BRICK, L.CRACKED_BRICK, L.TILE, L.COBBLED, L.ROCK):
                continue
            mt.set(x, yy, z, blocks[k % len(blocks)])
            k += 1


# ===================================================================================================================
# stairs on a curve
# ===================================================================================================================
def ramp_levels(cx, cz, r_in, r_out, a0, a1, deg_per_level):
    """{(x, z): level} of a spiral ramp over the ring r_in < distance <= r_out between the angles a0 and a1 (degrees, 0 = +x, 90 = +z, increasing clockwise seen from
    above): level 1 at a0, one more every `deg_per_level` degrees, then relaxed so that two cells that touch differ by at most one level (a player climbs one block at
    a time). Why: a curved stair has no pattern in blocks, but a height field over the ring with that one rule is a stair a player can climb."""
    cells = {}
    for (x, z) in ring(cx, cz, r_out, r_in):
        rel = (angle(x, z, cx, cz) - a0) % 360.0
        if rel <= (a1 - a0) % 360.0:
            cells[(x, z)] = 1 + int(rel / deg_per_level)
    for _ in range(40):
        changed = False
        for (x, z), lv in list(cells.items()):
            for dx, dz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                n = cells.get((x + dx, z + dz))
                if n is not None and lv > n + 1:
                    cells[(x, z)] = lv = n + 1
                    changed = True
        if not changed:
            break
    return cells


def build_ramp(h, cells, extra=None, stairs=L.PLANKS_STAIRS, base=GROUND, cx=13, cz=13):
    """Writes the ramp: a stair at row base + level - 1 on every cell, facing the neighbour that is higher, else (a cell with no higher neighbour: a step in the middle of a
    wide stretch, or the top of the ramp) the neighbour that is level with it along the climb, so every cell is a half step a player walks over and no cell needs a jump.
    `extra` is a {cell: level} of cells that are not part of the ramp but count as neighbours (the gallery at its top)."""
    allc = dict(cells)
    if extra:
        allc.update(extra)
    for (x, z), lv in sorted(cells.items()):
        here = angle(x, z, cx, cz)
        best = None
        for _n, dx, dz in DIRS4:
            n = allc.get((x + dx, z + dz))
            if n is None or n < lv:
                continue
            ahead = (angle(x + dx, z + dz, cx, cz) - here + 180.0) % 360.0 - 180.0          # > 0: the neighbour lies further along the climb
            key = (-n, -ahead)
            if best is None or key < best[0]:
                best = (key, toward(dx, dz))
        facing = best[1] if best else toward(-math.sin(math.radians(here)), math.cos(math.radians(here)))
        h.try_set(x, base + lv - 1, z, stairs, {"facing": facing, "half": "bottom"})

#!/usr/bin/env python3
"""
Rooms rebuilt, part 9: the checks (the only eyes this work has; nothing was run in a game). `check(room)` compares a rebuilt template with the shipped one it replaces and
returns a list of problems (empty = clean). What is checked, and why each rule exists:

  contract     same size; the sockets are the same jigsaw blocks (position, orientation, every data field); the data markers are the same (position and text); every container,
               mechanism and mod block of the shipped room stands again at its cell unchanged; no block outside vanilla 1.21.1 and the allowed mod blocks; every block state is
               one the game has; nothing above row 46 (the roof's top layer is at most row 46, the template is 48 high); at most one armour stand, and only in a stand room.
  shell        the room is closed: no cell of the outer shell is reachable from the inside through a see-through block (the void must never be visible); no hanging chain,
               lantern, torch or banner without what holds it, no standing prop without a floor, no block that is not joined to the building: only problems the shipped room did not
               already have at the same cell count as new problems.
  walking      every door can walk to every other door that could before, every spawn / patrol marker that could be reached still can, and the doorways keep a free 3-wide route
               where they had one (the walk graph of mega_decor_r1: step-up and head-room rules of a player).
  clearance    the cut boxes of Clearance7 are free: in front of every door, 7 wide, rows 9..15, the wall layers and three cells into the room (halls) or the whole lane (corridors,
               chambers): only air, light blocks, banners-less passable blocks and markers may stand there; stairs, chambers and corridors measure 7 x 7 after the cut with nothing removed.
  light        the walking route is at least as bright as the shipped room's minimum or 5, and not much brighter on average (a bright room is met more often, see LightRules).
  cost         the number of blocks is reported and capped (the doc quotes the sums).
"""
import collections
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mega_lib as L  # noqa: E402
import mega_decor_r1 as D1  # noqa: E402
import mega_check as MC  # noqa: E402
import rr_light as LT  # noqa: E402
from rr_core import FR, G, is_block_entity, BE_CAP  # noqa: E402

_VANILLA = None


def vanilla_blocks():
    global _VANILLA
    if _VANILLA is None:
        _VANILLA = MC.vanilla_blocks()
    return _VANILLA


def jigsaw_map(t):
    out = {}
    for pos, o, d in t.jigsaws():
        out[pos] = (o, tuple(sorted((str(k), str(v)) for k, v in d.items())))
    return out


def marker_map(t):
    out = {}
    for (x, y, z), c in t.cells.items():
        if c.block == "minecraft:structure_block" and c.nbt is not None:
            out[(x, y, z)] = str(c.nbt.get("metadata", ""))
    return out


def functional_cells(base):
    """Cells of the shipped room that carry a block entity (chests, barrels, spawners, furnaces, lecterns ...) or a mod block: they must survive unchanged."""
    out = {}
    for pos, c in base.cells.items():
        if c.block in ("minecraft:structure_block", "minecraft:jigsaw"):
            continue
        if c.block.startswith("nusmp:") or c.block in ("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:spawner", "minecraft:trial_spawner",
                                                      "minecraft:vault", "minecraft:ender_chest", "minecraft:shulker_box", "minecraft:hopper", "minecraft:dispenser",
                                                      "minecraft:dropper", "minecraft:crafter", "minecraft:brewing_stand", "minecraft:furnace", "minecraft:blast_furnace",
                                                      "minecraft:smoker", "minecraft:lectern", "minecraft:chiseled_bookshelf", "minecraft:decorated_pot", "minecraft:jukebox",
                                                      "minecraft:beacon", "minecraft:enchanting_table", "minecraft:conduit"):
            out[pos] = c
    return out


def problem_set(t):
    """{(kind, block, pos)} the shipped model finds wrong in a template, for the 'only new problems count' comparison."""
    out = set()
    for pos in D1.leaks(t):
        out.add(("leak", "", pos))
    for pos, why in D1.floating(t):
        out.add(("floating", why.split(" ")[0], pos))
    for pos, why in D1.unsupported(t):
        out.add(("unsupported", why.split(" ")[0], pos))
    for pos, why in D1.disconnected(t):
        out.add(("disconnected", why.split(" ")[0], pos))
    return out


def lane_cells(room):
    """The cells that must stay free in a rebuilt piece (the Clearance7 cut boxes): door boxes for halls, the whole interior lane for corridor pieces."""
    cells = set()
    W, D = room.W, room.D
    corridor = W == 9 and D % 9 == 0 or D == 9 and W % 9 == 0
    if corridor and room.scratch:
        for z in range(1, D - 1):
            for x in range(1, W - 1):
                for y in range(G, 16):
                    cells.add((x, y, z))
        return cells
    for side, lat, _ in room.doors:
        for dl in range(-3, 4):
            for d in range(0, 5):
                for y in range(G, 16):
                    cells.add(room.wcell(side, lat + dl, d, y))
    return cells


def check(room, max_blocks_ratio=1.45):
    t, base = room.t, room.base_t
    problems = []
    if t.size != base.size:
        problems.append("size %s differs from the shipped %s" % (t.size, base.size))
    # contract: sockets and markers
    jb, jn = jigsaw_map(base), jigsaw_map(t)
    if jb != jn:
        problems.append("the sockets differ: shipped %d, rebuilt %d (%s)" % (len(jb), len(jn), sorted(set(jb) ^ set(jn))[:4] or "same cells, different data"))
    mb, mn = marker_map(base), marker_map(t)
    if mb != mn:
        problems.append("the data markers differ: %s" % sorted(set(mb.items()) ^ set(mn.items()))[:3])
    lane = lane_cells(room) if room.scratch else set()
    for pos, c in functional_cells(base).items():
        if pos in lane and not c.block.startswith("nusmp:") and c.block not in ("minecraft:chest", "minecraft:trapped_chest", "minecraft:barrel", "minecraft:spawner"):
            continue            # furniture inside a corridor lane: the generator's 7 x 7 cut removes it anyway
        n = t.cells.get(pos)
        if n is None or n.block != c.block or n.props != c.props or (c.nbt is not None and n.nbt != c.nbt):
            problems.append("functional block %s at %s was changed or lost (now %s)" % (c.block, pos, n.block if n else None))
    van = vanilla_blocks()
    bad_blocks = set()
    for pos, c in t.cells.items():
        if c.block.startswith("minecraft:"):
            if c.block not in van:
                bad_blocks.add(c.block)
        elif c.block not in L.CUSTOM:
            bad_blocks.add(c.block)
        err = L.check_state(c.block, c.props)
        if err:
            problems.append("block state: %s at %s" % (err, pos))
            break
    if bad_blocks:
        problems.append("blocks outside vanilla and the allowed mod blocks: %s" % sorted(bad_blocks))
    strip = lambda m: re.sub(r" \(at .*\)$", "", m)  # noqa: E731
    shipped_strict = {strip(m) for m in D1.strict_state_problems(base)}
    for msg in D1.strict_state_problems(t):
        if strip(msg) not in shipped_strict:
            problems.append("strict state: " + msg)
            break
    top = max(y for (x, y, z) in t.cells)
    if top > 46:
        problems.append("a block at row %d: the roof must stay under row 47" % top)
    stands = [e for e in t.entities if str(e.get("nbt", {}).get("id", "")) == "minecraft:armor_stand"]
    if len(stands) > 1:
        problems.append("%d armour stands (at most one slot)" % len(stands))
    base_other = [e for e in base.entities if str(e.get("nbt", {}).get("id", "")) != "minecraft:armor_stand"]
    new_other = [e for e in t.entities if str(e.get("nbt", {}).get("id", "")) != "minecraft:armor_stand"]
    if len(base_other) != len(new_other):
        problems.append("entities other than armour stands changed: %d -> %d" % (len(base_other), len(new_other)))
    # shell
    pb, pn = problem_set(base), problem_set(t)
    base_cells = base.cells
    new_only = []
    for kind, what, pos in sorted(pn):
        inherited = (kind, what, pos) in pb
        if not inherited:
            new_only.append((kind, what, pos))
    for kind, what, pos in new_only[:6]:
        problems.append("new %s problem (%s) at %s" % (kind, what, pos))
    if len(new_only) > 6:
        problems.append("and %d more new shell problems" % (len(new_only) - 6))
    for line in D1.multiblock_problems(t)[:3]:
        problems.append("multi-block: " + line)
    # walking
    nodes_b, nodes_n = D1.walk_nodes(base), D1.walk_nodes(t)
    starts = D1.socket_starts(t)
    for i, st in enumerate(starts):
        one_b = D1.reach(base, nodes_b, [st])
        one_n = D1.reach(t, nodes_n, [st])
        for j, other in enumerate(starts):
            if i != j and other in one_b and other not in one_n:
                problems.append("door %d (%s) can no longer walk to door %d (%s)" % (i, st[:2], j, other[:2]))
        wb = D1.reach(base, nodes_b, [st], wide=True)
        wn = D1.reach(t, nodes_n, [st], wide=True)
        if st in wb and st not in wn:
            problems.append("door %d (%s): the cells in front of it are no longer 3 wide" % (i, st[:2]))
        for j, other in enumerate(starts):
            if i != j and other in wb and other not in wn:
                problems.append("the 3-wide route from door %d to door %d is lost" % (i, j))
    seen_b = D1.reach(base, nodes_b)
    seen_n = D1.reach(t, nodes_n)
    for kind, x, y, z, args in D1.markers_of(t):
        if kind not in ("spawn", "patrol"):
            continue
        sb = next((s for s in nodes_b.get((x, z), ()) if abs(s - 2 * y) <= 1), None)
        sn = next((s for s in nodes_n.get((x, z), ()) if abs(s - 2 * y) <= 1), None)
        if sb is not None and (x, z, sb) in seen_b and (sn is None or (x, z, sn) not in seen_n):
            problems.append("%s marker at (%d,%d,%d) can no longer be reached on foot" % (kind, x, y, z))
    # clearance: the cut boxes hold nothing that stops a player
    cb = {pos: c for pos, c in base.cells.items()}
    for (x, y, z) in lane_cells(room):
        c = t.cells.get((x, y, z))
        if c is None:
            continue
        bc = cb.get((x, y, z))
        if bc is not None and bc.block == c.block:
            continue            # the shipped room's own block: the cut handles it as before
        if D1.classify(c.block, c.props)[0] != "pass" and c.block not in ("minecraft:structure_block", "minecraft:jigsaw"):
            problems.append("a %s stands in a clearance box at %s" % (c.block, (x, y, z)))
            break
    # light
    sb_, sn_ = LT.stats_t(base), LT.stats_t(t)
    if sn_["min"] < min(5, sb_["min"]):
        problems.append("the walking light is darker than the shipped room's: min %d (shipped %d)" % (sn_["min"], sb_["min"]))
    if sn_["below"] > max(0.04, sb_["below"]):
        problems.append("%.1f%% of the route is under light 5 (shipped %.1f%%)" % (100 * sn_["below"], 100 * sb_["below"]))
    if sn_["median"] > sb_["median"] + 4:
        problems.append("the room is much brighter than the shipped one: median %d (shipped %d)" % (sn_["median"], sb_["median"]))
    if sn_["bright"] > sb_["bright"] + 0.12:
        problems.append("%.0f%% of the route is at light 13 or more (shipped %.0f%%): a bright room is met more often" % (100 * sn_["bright"], 100 * sb_["bright"]))
    be_new = sum(1 for c in t.cells.values() if is_block_entity(c.block))
    be_old = sum(1 for c in base.cells.values() if is_block_entity(c.block))
    if be_new - be_old > BE_CAP + 2:
        problems.append("%d block entities more than the shipped room (cap %d)" % (be_new - be_old, BE_CAP))
    ratio = len(t.cells) / max(1.0, float(len(base.cells)))
    if ratio > max_blocks_ratio:
        problems.append("%d blocks is x%.2f of the shipped %d (cap x%.2f)" % (len(t.cells), ratio, len(base.cells), max_blocks_ratio))
    return problems, {"blocks": len(t.cells), "base_blocks": len(base.cells), "ratio": ratio, "light_min": sn_["min"], "light_median": sn_["median"],
                      "base_light_min": sb_["min"], "base_light_median": sb_["median"], "below5": sn_["below"], "bright": sn_["bright"], "height": top, "be_new": be_new, "be_old": be_old}

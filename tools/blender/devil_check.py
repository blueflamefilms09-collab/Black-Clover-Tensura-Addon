"""Checks the devil mesh files the way BossMeshWave2Test / EntityMeshData check the boss meshes (no Blender needed):
header, a pivot for every part, parents before children, the 8 role parts present with quads, at most 3000 quads, LOD lighter and without glow, skins square powers of two.

    python tools/blender/devil_check.py [id ...]      (default: the group 2 ids; prints quads and heights, exit 1 on a failure)
"""
import os
import struct
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
CORE = ["torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r"]
ALL = CORE + ["wing_l", "wing_r", "tail"]
IDS = ["fox_devil", "curse_devil", "future_devil", "ghost_devil", "sebastian", "hall_demon"]


def parse(path):
    scale, pivots, order, quads, cur = 1 / 16, {}, [], {}, None
    q_body = q_glow = 0
    lo, hi = 1e9, -1e9
    for line in open(path):
        t = line.split()
        if not t or t[0] == "#":
            continue
        if t[0] == "entity":
            scale = float(t[2])
        elif t[0] == "pivot":
            if t[2] != "-" and t[2] not in pivots:
                raise SystemExit(f"{path}: parent {t[2]} of {t[1]} comes after it")
            pivots[t[1]] = (t[2], tuple(map(float, t[3:6])))
            order.append(t[1])
        elif t[0] == "part":
            if t[1] not in pivots:
                raise SystemExit(f"{path}: part {t[1]} has no pivot")
            cur = t[1]
            quads.setdefault(cur, 0)
        elif t[0] == "qn":
            layer = int(t[1])
            v = list(map(float, t[2:]))
            if len(v) != 32:
                raise SystemExit(f"{path}: a qn line with {len(v)} numbers")
            if layer == 0:
                quads[cur] += 1
                q_body += 1
            else:
                q_glow += 1
            for k in range(4):
                y = v[k * 8 + 1]
                lo, hi = min(lo, y), max(hi, y)
    return scale, pivots, quads, q_body, q_glow, lo * scale, hi * scale


def png_size(path):
    with open(path, "rb") as fh:
        h = fh.read(24)
    return struct.unpack(">II", h[16:24])


def main(ids):
    bad = 0
    for i in ids:
        eid = "devil_" + i
        m = os.path.join(ASSETS, "entity_meshes")
        sc, piv, q, body, glow, lo, hi = parse(os.path.join(m, eid + ".txt"))
        _, piv2, q2, body2, glow2, _, _ = parse(os.path.join(m, eid + "_lod.txt"))
        errs = []
        for r in CORE:
            if r not in piv or q.get(r, 0) == 0:
                errs.append("part " + r)
            if r not in piv2 or q2.get(r, 0) == 0:
                errs.append("LOD part " + r)
        errs += ["unknown part " + p for p in piv if p not in ALL]
        if body + glow > 3000:
            errs.append(f"{body + glow} quads over 3000")
        if not glow:
            errs.append("no glow quads")
        if glow2:
            errs.append("the LOD has a glow pass")
        if (body2 + glow2) * 2 >= body + glow:
            errs.append(f"LOD {body2} is not under half of {body + glow}")
        if not -0.2 < lo < 0.2:
            errs.append(f"feet at {lo:.2f}")
        for s in (eid + ".png", eid + "_glow.png"):
            w, h = png_size(os.path.join(ASSETS, "textures", "entity", "boss_mesh", s))
            if w != h or w > 1024 or w & (w - 1):
                errs.append(f"{s} is {w}x{h}")
        print(f"{i:13s} parts {len(piv):2d}  body {body:4d} + glow {glow:3d} = {body + glow:4d}   LOD {body2:4d}   y {lo:.2f}..{hi:.2f} blocks   {'OK' if not errs else errs}")
        bad += bool(errs)
    return bad


if __name__ == "__main__":
    sys.exit(1 if main(sys.argv[1:] or IDS) else 0)

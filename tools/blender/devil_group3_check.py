"""Checks the group 3 devil meshes with the rules of BossMeshWave2Test / EntityMeshData (no Blender needed):
parent-before-child pivots, every body role present with quads, at most 3000 quad lines, feet on the floor, a player-scale height, a LOD copy under half of the full
mesh with no glow pass, glow quads present, square power-of-two skins up to 1024 px, previews under 300 KB.   python3 tools/blender/devil_group3_check.py"""
import os
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", ".."))
MESH = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "entity_meshes")
SKIN = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse", "textures", "entity", "boss_mesh")
PREV = os.path.join(ROOT, "docs", "devil_forms", "previews")
IDS = ["kin_of_iblis", "kin_of_egyn", "kin_of_amaimon", "kin_of_astaroth", "kin_of_lucifer", "daemon_noir", "daemon_blanc"]
ROLES = ["torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r"]
OPTIONAL = {"wing_l", "wing_r", "tail"}


def parse(path):
    parts, pivots, quads, cur, scale = [], {}, {}, None, None
    for line in open(path):
        w = line.split()
        if not w or w[0] == "#":
            continue
        if w[0] == "entity":
            scale = float(w[2])
        elif w[0] == "pivot":
            if w[2] != "-" and w[2] not in pivots:
                raise SystemExit(f"{path}: parent {w[2]} after child {w[1]}")
            pivots[w[1]] = w[2]
        elif w[0] == "part":
            cur = w[1]
            if cur not in pivots:
                raise SystemExit(f"{path}: part {cur} has no pivot")
            quads[cur] = {0: [], 2: []}
        elif w[0] == "qn":
            quads[cur][int(w[1])].append([float(x) for x in w[2:]])
    return scale, pivots, quads


def main():
    bad = 0
    for i in IDS:
        n = "devil_" + i
        sc, pv, q = parse(os.path.join(MESH, n + ".txt"))
        lsc, lpv, lq = parse(os.path.join(MESH, n + "_lod.txt"))
        total = sum(len(v[0]) + len(v[2]) for v in q.values())
        ltotal = sum(len(v[0]) + len(v[2]) for v in lq.values())
        glow = sum(len(v[2]) for v in q.values())
        lglow = sum(len(v[2]) for v in lq.values())
        ys = [quad[1 + 8 * k] for v in q.values() for quad in v[0] for k in range(4)]
        lo, hi = min(ys) * sc, max(ys) * sc
        body_hi = max(quad[1 + 8 * k] for quad in q["head"][0] for k in range(4)) * sc       # includes horns / hats on the head
        errs = []
        for r in ROLES:
            if r not in q or not q[r][0]:
                errs.append("role " + r)
        if set(q) - set(ROLES) - OPTIONAL:
            errs.append("unknown parts " + str(set(q) - set(ROLES) - OPTIONAL))
        if total > 2800:
            errs.append(f"{total} quads")
        if not (-0.2 < lo < 0.2):
            errs.append(f"feet at {lo:.2f}")
        if not (1.7 < body_hi < 2.7):
            errs.append(f"head top at {body_hi:.2f} blocks")
        if not ltotal < total / 2:
            errs.append(f"LOD {ltotal} not under half of {total}")
        if lglow or not glow:
            errs.append("glow")
        for s in (n + ".png", n + "_glow.png"):
            im = Image.open(os.path.join(SKIN, s))
            if im.width != im.height or im.width > 1024 or im.width & (im.width - 1):
                errs.append(s + " size")
        for s in (i + ".png", i + "_back.png"):
            if os.path.getsize(os.path.join(PREV, s)) > 300 * 1024:
                errs.append(s + " over 300 KB")
        print(f"{i:16s} parts {len(q):2d}  quads {total - glow}+{glow}={total}  LOD {ltotal}  height {lo:.2f}..{hi:.2f} (head top {body_hi:.2f})  {'OK' if not errs else errs}")
        bad += bool(errs)
    sys.exit(1 if bad else 0)


if __name__ == "__main__":
    main()

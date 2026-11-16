"""Checks the group 4 devil meshes against the rules of BossMeshWave2Test / EntityMeshData (pure Python, no Blender): python tools/blender/devil_group4_check.py"""
import os
import struct
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
A = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
IDS = ["daemon_jaune", "daemon_violet", "daemon_rouge", "daemon_vert", "daemon_bleu"]
ROLES = ["torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r"]


def parse(path):
    scale, pivots, parts, cur, quads, glow = 1 / 16, {}, {}, None, 0, 0
    for line in open(path):
        w = line.split()
        if not w or w[0] == "#":
            continue
        if w[0] == "entity":
            scale = float(w[2])
        elif w[0] == "pivot":
            pivots[w[1]] = (w[2], tuple(map(float, w[3:6])))
        elif w[0] == "part":
            cur = w[1]
            parts[cur] = []
        elif w[0] == "qn":
            quads += 1
            layer = int(w[1])
            glow += layer == 2
            v = list(map(float, w[2:]))
            parts[cur].append((layer, [(v[i], v[i + 1], v[i + 2]) for i in range(0, 32, 8)]))
    return scale, pivots, parts, quads, glow


def png_size(p):
    with open(p, "rb") as f:
        h = f.read(24)
    return struct.unpack(">II", h[16:24])


bad = 0
for i in IDS:
    e = "devil_" + i
    sc, pv, parts, q, g = parse(os.path.join(A, "entity_meshes", e + ".txt"))
    _, _, lparts, lq, lg = parse(os.path.join(A, "entity_meshes", e + "_lod.txt"))
    errs = []
    for r in ROLES:
        if r not in parts or not parts[r]:
            errs.append("part " + r)
    for n, (par, _) in pv.items():
        if par != "-" and (par not in pv or list(pv).index(par) >= list(pv).index(n)):
            errs.append("pivot order " + n)
    if set(parts) != set(pv):
        errs.append("parts without pivot")
    ys = [p[1] for pl in parts.values() for _, vs in pl for p in vs]
    lo, hi = min(ys) * sc, max(ys) * sc
    if not (-0.2 < lo < 0.2):
        errs.append(f"feet at {lo:.2f}")
    if not (1.7 < hi < 2.6):
        errs.append(f"height {hi:.2f}")
    if q > 2800 or q > 3000:
        errs.append(f"{q} quads")
    if g <= 0:
        errs.append("no glow")
    if not lq < q / 2:
        errs.append(f"LOD {lq} not under half of {q}")
    if lg != 0:
        errs.append("LOD has glow")
    for suf in (".png", "_glow.png"):
        p = os.path.join(A, "textures", "entity", "boss_mesh", e + suf)
        w, h = png_size(p)
        if w != h or w > 1024 or w & (w - 1):
            errs.append(f"{suf}: {w}x{h} {os.path.getsize(p) // 1024} KB")
    pv_ = os.path.join(ROOT, "docs", "devil_forms", "previews", i + ".png")
    if not os.path.exists(pv_) or os.path.getsize(pv_) >= 300 * 1024:
        errs.append("preview missing or over 300 KB")
    body = q - g
    print(f"{e}: {q} quads ({body} body + {g} glow), LOD {lq} ({lq * 100 // max(body, 1)}% of body), height {hi:.2f} blocks, parts {sorted(parts)}", "OK" if not errs else "FAIL " + "; ".join(errs))
    bad += bool(errs)
sys.exit(1 if bad else 0)

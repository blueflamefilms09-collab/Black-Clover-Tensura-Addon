#!/usr/bin/env python3
"""Numbers behind the armour fit of the Goku and Sinbad looks (docs/boss_redesign_goku_sinbad.md, "Armour overlap"). Pure python, reads the repo's files, writes nothing.

  python3 -B tools/bc_playermodels/gs_fit_report.py

1. OUTER LAYER VS BODY: every cube of every pmx_gs_*.geo.json against the vanilla cube of its bone (the same boxes as GsArmorFit.java): buried in the body, buried in the skin's second layer
   (+0.25, head +0.5), sharing a face plane with the body (within 0.02). The game test GsArmorFitTest fails on the first and the third.
2. LOOK VS DJINN EQUIP: for each Sinbad body and each piece of the djinn's own armour set (assets/anime_multiverse/djinn/u_<id>/<piece>.txt, drawn on the model part it is anchored to), the share of
   the piece's quads whose centre lies within 0.8 pixel of a cube of the look on the same bone. Pieces with a high share are drawn on top of geometry the look already has; those are the
   ones GsArmorFit.hiddenPieces lets dissolve into the look.
"""
import glob
import itertools
import json
import math
import os

A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "src", "main", "resources", "assets", "anime_multiverse") + os.sep
BASE = {"x_head": (-4, 24, -4, 4, 32, 4), "x_body": (-4, 12, -2, 4, 24, 2), "x_arm_r": (-8, 12, -2, -4, 24, 2), "x_arm_l": (4, 12, -2, 8, 24, 2),
        "x_leg_r": (-4, 0, -2, 0, 12, 2), "x_leg_l": (0, 0, -2, 4, 12, 2)}
PIV_Y = {"x_head": 24, "x_body": 24, "x_arm_r": 22, "x_arm_l": 22, "x_leg_r": 12, "x_leg_l": 12}
PIV_X = {"x_arm_r": -5, "x_arm_l": 5, "x_leg_r": -1.9, "x_leg_l": 1.9}
ANCHOR = {"helm": "x_head", "horn_l": "x_head", "horn_r": "x_head", "ears": "x_head", "crest": "x_head", "chest": "x_body", "belt": "x_body", "mantle": "x_body",
          "pauldron_r": "x_arm_r", "vambrace_r": "x_arm_r", "gauntlet_r": "x_arm_r", "pauldron_l": "x_arm_l", "vambrace_l": "x_arm_l", "gauntlet_l": "x_arm_l",
          "greave_r": "x_leg_r", "greave_l": "x_leg_l"}


def box(c):
    o, s = c["origin"], c["size"]
    lo, hi = list(o), [o[i] + s[i] for i in range(3)]
    if "rotation" in c:
        p = c["pivot"]
        rx, ry, rz = [math.radians(a) for a in c["rotation"]]
        pts = []
        for x, y, z in itertools.product(*zip(lo, hi)):
            x, y, z = x - p[0], y - p[1], z - p[2]
            x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
            x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
            y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
            pts.append((x + p[0], y + p[1], z + p[2]))
        lo = [min(q[i] for q in pts) for i in range(3)]
        hi = [max(q[i] for q in pts) for i in range(3)]
    return lo, hi


def inside(lo, hi, b, g=0.0):
    return all(lo[i] >= b[i] - g - 1e-6 and hi[i] <= b[i + 3] + g + 1e-6 for i in range(3))


def fights(lo, hi, b, tol=0.02):
    for ax in range(3):
        o = [a for a in range(3) if a != ax]
        if not all(min(hi[a], b[a + 3]) - max(lo[a], b[a]) > 0.05 for a in o) or not (hi[ax] > b[ax] + tol and lo[ax] < b[ax + 3] - tol):
            continue
        if any(abs(f - pl) <= tol for pl in (b[ax], b[ax + 3]) for f in (lo[ax], hi[ax])):
            return True
    return False


def geo_bones(name):
    return {b["name"]: b for b in json.load(open(A + "geo/entity/" + name + ".geo.json"))["minecraft:geometry"][0]["bones"]}


def quad_centres(path):
    out = []
    for ln in open(path):
        if ln.startswith("qn"):
            v = list(map(float, ln.split()[2:]))
            out.append([sum(v[q * 8 + i] for q in range(4)) / 4 for i in range(3)])
    return out


def main():
    cubes = buried = skin = fight = 0
    for f in sorted(glob.glob(A + "geo/entity/pmx_gs_*.geo.json")):
        for bn, b in geo_bones(os.path.basename(f)[:-9]).items():
            if bn not in BASE:
                continue
            g = tuple(BASE[bn][i] + (-1 if i < 3 else 1) * (0.5 if bn == "x_head" else 0.25) for i in range(6))
            for c in b.get("cubes", []):
                lo, hi = box(c)
                cubes += 1
                if inside(lo, hi, BASE[bn]):
                    buried += 1
                elif inside(lo, hi, g):
                    skin += 1
                elif fights(lo, hi, BASE[bn]):
                    fight += 1
                if fights(lo, hi, BASE[bn]) and inside(lo, hi, BASE[bn]) is False:
                    pass
    print("1. outer layer vs body: %d cubes; buried in the body %d; buried in the second skin layer %d (the hair shell, 0.05 off the skull on purpose; the layer is empty); sharing a face plane %d" % (cubes, buried, skin, fight))
    print("2. share of each djinn piece's quads within 0.8 px of a cube of the look on the same bone:")
    for dj in ("baal", "valefor", "zepar", "focalor", "vepar", "crocell", "furfur"):
        bones = geo_bones("pmx_gs_sinbad_" + dj)
        row = []
        for piece, bone in ANCHOR.items():
            p = A + "djinn/u_%s/%s.txt" % (dj, piece)
            if not os.path.exists(p):
                p = A + "djinn/%s/%s.txt" % (dj, piece)
            if not os.path.exists(p):
                continue
            boxes = []
            for c in bones[bone].get("cubes", []):
                lo, hi = box(c)
                boxes.append(([lo[0] - PIV_X.get(bone, 0), PIV_Y[bone] - hi[1], lo[2]], [hi[0] - PIV_X.get(bone, 0), PIV_Y[bone] - lo[1], hi[2]]))
            qs = quad_centres(p)
            near = sum(1 for q in qs if any(math.sqrt(sum(max(l[i] - q[i], 0, q[i] - h[i]) ** 2 for i in range(3))) <= 0.8 for l, h in boxes))
            row.append("%s=%.2f" % (piece, near / len(qs)))
        print("   %-8s %s" % (dj, " ".join(row)))


if __name__ == "__main__":
    main()

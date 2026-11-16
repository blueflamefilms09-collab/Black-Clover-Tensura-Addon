"""Black Sword Greed, hand forms: the gauntlets become one glove per hand and the dual blades become one sword per hand.

    python tools/greed_hands/split_meshes.py [--check]

Why: tools/blender/black_sword_greed.py models the gauntlets as ONE mesh holding a pair of gloves and the dual blades as ONE mesh holding a hilt with two blades. In the game a
pair of gloves floated in front of the fist and the "two swords" were one hilt, so neither felt like two hands. This script derives the per-hand meshes from those Blender
meshes (Blender is not installed in the build container; the geometry, UVs and skins are the Blender ones, only regrouped), deterministic and idempotent:

  gauntlets    weapon_meshes/black_sword_greed_gauntlets.txt        the RIGHT glove (the half of the pair that has its thumb on -x), recentred on its own axis
               weapon_meshes/black_sword_greed_gauntlets_left.txt   the LEFT glove (thumb on +x)
  dual blades  weapon_meshes/black_sword_greed_dual_blades.txt      ONE sword: the whole hilt (guard, boss, jaws, maw) and ONE blade, put back on the hilt's axis
  kept         weapon_meshes/classic/<id>.txt                       the old meshes (the pair, the double blade), drawn when the client setting greed.classicHands is on
Skins: the new meshes use the same UV layout, so black_sword_greed_gauntlets_left gets a copy of the gauntlet skin / glow skin and the classic folder keeps copies of the old ones.

The pieces are found by connected components (quads that share a vertex belong to one Blender object), not by guessing from coordinates.
"""
import math
import os
import shutil
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
MESH = os.path.join(ASSETS, "weapon_meshes")
CLASSIC = os.path.join(MESH, "classic")
SKIN = os.path.join(ASSETS, "textures", "entity", "weapon")
SKIN_CLASSIC = os.path.join(SKIN, "classic")

GLOVE_PAIR_OFFSET = 12.5            # the pair was built with the right glove at x + 12.5 and its mirror at x - 12.5 (black_sword_greed.py gauntlets())
BLADE_PIVOT = (3.4, 31.0)           # the blades were moved 3.4 right and leant -4 degrees about (3.4, 31) (black_sword_greed.py dual_blades())
BLADE_LEAN = -4.0


def load(path):
    head, quads = [], []
    with open(path) as fh:
        for line in fh:
            v = line.split()
            if not v:
                continue
            if v[0] == "qn":
                quads.append((int(v[1]), np.array([float(x) for x in v[2:34]]).reshape(4, 8)))
            elif v[0] != "#" or not (len(v) > 1 and v[1] == "part"):
                head.append(line.rstrip("\n"))
    return head, quads


def fmt(layer, a):
    cells = []
    for r in a:
        cells.append("%.3f %.3f %.3f %.3f %.3f %.4f %.4f %.4f" % tuple(r))
    return "qn %d %s" % (layer, " ".join(cells))


def components(quads):
    parent = list(range(len(quads)))

    def find(a):
        while parent[a] != a:
            parent[a] = parent[parent[a]]
            a = parent[a]
        return a
    seen = {}
    for i, (layer, a) in enumerate(quads):
        for v in a[:, :3]:
            k = (layer == 2,) + tuple(np.round(v, 2))
            if k in seen:
                ra, rb = find(i), find(seen[k])
                if ra != rb:
                    parent[ra] = rb
            else:
                seen[k] = i
    out = {}
    for i in range(len(quads)):
        out.setdefault(find(i), []).append(i)
    return list(out.values())


def bbox(quads, idx):
    p = np.array([quads[i][1][:, :3] for i in idx]).reshape(-1, 3)
    return p.min(0), p.max(0)


def write(path, head, quads, note):
    lines = ["# %s" % note] + [h for h in head if not h.startswith("# made") and not h.startswith("# derived")]
    lines += [fmt(l, a) for l, a in quads]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines))


def source(item_id):
    """The Blender mesh: the classic copy when it already exists (so a second run reads the original), else the shipped file, which is then copied to classic/."""
    classic = os.path.join(CLASSIC, item_id + ".txt")
    if os.path.exists(classic):
        return classic
    os.makedirs(CLASSIC, exist_ok=True)
    shutil.copyfile(os.path.join(MESH, item_id + ".txt"), classic)
    return classic


def keep_skins(item_id):
    os.makedirs(SKIN_CLASSIC, exist_ok=True)
    for suffix in ("", "_glow"):
        dst = os.path.join(SKIN_CLASSIC, item_id + suffix + ".png")
        if not os.path.exists(dst):
            shutil.copyfile(os.path.join(SKIN, item_id + suffix + ".png"), dst)


def gauntlets():
    iid = "black_sword_greed_gauntlets"
    keep_skins(iid)
    head, quads = load(source(iid))
    comps = components(quads)
    sides = {"R": [], "L": []}
    for c in comps:
        lo, hi = bbox(quads, c)
        if (lo[0] + hi[0]) / 2.0 == 0.0 or lo[0] < 0.0 < hi[0]:
            raise SystemExit("a gauntlet piece straddles the middle: %s %s" % (lo, hi))
        sides["L" if lo[0] > 0 else "R"] += c                # the glove at +x has its thumb on +x: a left hand seen from its back; the one at -x is the right
    out = {}
    for key, idx in sides.items():
        sh = -GLOVE_PAIR_OFFSET if key == "L" else GLOVE_PAIR_OFFSET
        qs = []
        for i in sorted(idx):
            layer, a = quads[i]
            a = a.copy()
            a[:, 0] += sh
            qs.append((layer, a))
        out[key] = qs
    write(os.path.join(MESH, iid + ".txt"), head, out["R"], "derived from the Blender pair mesh by tools/greed_hands/split_meshes.py: the RIGHT glove (wrist at y = 0, fingers +y, back of the hand +z, thumb -x)")
    write(os.path.join(MESH, iid + "_left.txt"), head, out["L"], "derived from the Blender pair mesh by tools/greed_hands/split_meshes.py: the LEFT glove (wrist at y = 0, fingers +y, back of the hand +z, thumb +x)")
    for suffix in ("", "_glow"):
        dst = os.path.join(SKIN, iid + "_left" + suffix + ".png")
        if not os.path.exists(dst):
            shutil.copyfile(os.path.join(SKIN, iid + suffix + ".png"), dst)
    return len(out["R"]), len(out["L"])


def rot_z(a, deg, pivot):
    """Turns a vertex block about the z axis through pivot (x, y); the normals turn with it."""
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    a = a.copy()
    x, y = a[:, 0] - pivot[0], a[:, 1] - pivot[1]
    a[:, 0], a[:, 1] = pivot[0] + x * c - y * s, pivot[1] + x * s + y * c
    nx, ny = a[:, 5].copy(), a[:, 6].copy()
    a[:, 5], a[:, 6] = nx * c - ny * s, nx * s + ny * c
    return a


def dual_blades():
    iid = "black_sword_greed_dual_blades"
    keep_skins(iid)
    head, quads = load(source(iid))
    comps = components(quads)
    keep, right, left = [], [], []
    for c in comps:
        lo, hi = bbox(quads, c)
        if hi[1] < 37.0 or lo[0] < -5.0 and hi[0] > 5.0 or hi[1] <= 31.7:
            keep += c                                         # the hilt, the guard boss, the jaws, the maw: symmetric about the axis (the blades and their collars reach y >= 37 and lie on one side)
        elif lo[0] > -1.4:
            right += c
        elif hi[0] < 1.4:
            left += c
        else:
            keep += c
    if not right or not left:
        raise SystemExit("did not find the two blades")
    out = [quads[i] for i in sorted(keep)]
    for i in sorted(right):                                   # the right blade and its collar, put back: undo the lean, then the 3.4 shift
        layer, a = quads[i]
        a = rot_z(a, -BLADE_LEAN, BLADE_PIVOT)
        a[:, 0] -= BLADE_PIVOT[0]
        out.append((layer, a))
    write(os.path.join(MESH, iid + ".txt"), head, out, "derived from the Blender double-blade mesh by tools/greed_hands/split_meshes.py: ONE sword (the whole hilt, one blade on its axis); the left hand draws a second copy")
    return len(out), len(right), len(left)


def main(argv):
    if "--check" in argv:
        print("derived meshes exist:", all(os.path.exists(os.path.join(MESH, f)) for f in ("black_sword_greed_gauntlets_left.txt", "classic/black_sword_greed_dual_blades.txt")))
        return 0
    print("gauntlets (right quads, left quads):", gauntlets())
    print("dual blades (single sword quads, right blade quads, left blade quads):", dual_blades())
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

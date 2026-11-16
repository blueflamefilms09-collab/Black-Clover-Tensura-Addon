"""Checks the devil entity meshes the way EntityMeshData / BossMeshWave2Test read them (no Java needed): python3 tools/blender/devil_mesh_check.py [id ...].

For each devil_<id>: the header (entity id, scale, glow, a pivot per part, a parent before its children), the eight roles every body has (+ wing_l, wing_r, tail where the devil has them), quads in every
role, at most 3000 quad lines (the build budget is 2800), the LOD copy under half and with no glow pass, the feet on the floor, the body's height, and the skins (square, a power of two up to 1024 px).
"""
import os
import struct
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "anime_multiverse")
IDS = ["liebe", "lucifero", "beelzebub", "astaroth", "megicula", "lucifugus", "god_hand", "etherious"]
ROLES = ["torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r"]
WINGS = {"lucifero", "liebe", "beelzebub", "astaroth", "megicula", "lucifugus", "god_hand", "etherious"}
TAILS = {"astaroth", "megicula", "etherious"}


def parse(path):
    names, parent, pivot, body, glow = [], [], [], [], []
    cur = -1
    quads = 0
    scale = None
    with open(path) as fh:
        for line in fh:
            v = line.split()
            if not v or v[0] == "#":
                continue
            if v[0] == "entity":
                scale = float(v[2])
                eid = v[1]
            elif v[0] == "pivot":
                assert v[1] not in names, "pivot twice " + v[1]
                p = -1 if v[2] == "-" else names.index(v[2])        # ValueError = parent after its child
                names.append(v[1])
                parent.append(p)
                pivot.append(tuple(map(float, v[3:6])))
                body.append([])
                glow.append([])
            elif v[0] == "part":
                cur = names.index(v[1])
            elif v[0] == "qn":
                assert cur >= 0 and len(v) == 2 + 32, "bad quad line"
                quads += 1
                vals = list(map(float, v[2:]))
                (glow if int(v[1]) == 2 else body)[cur].append(vals)
    assert quads <= 3000, f"{quads} quads over the 3000 cap"
    return dict(id=eid, scale=scale, names=names, parent=parent, pivot=pivot, body=body, glow=glow, quads=quads)


def png_size(path):
    with open(path, "rb") as fh:
        head = fh.read(33)
    assert head[:8] == b"\x89PNG\r\n\x1a\n", path
    return struct.unpack(">II", head[16:24])


def check(did):
    m = parse(os.path.join(ASSETS, "entity_meshes", f"devil_{did}.txt"))
    lod = parse(os.path.join(ASSETS, "entity_meshes", f"devil_{did}_lod.txt"))
    assert m["id"] == f"devil_{did}", m["id"]
    assert abs(m["scale"] - 0.0625) < 1e-6
    for r in ROLES:
        assert r in m["names"] and m["body"][m["names"].index(r)], "role " + r
        assert r in lod["names"] and lod["body"][lod["names"].index(r)], "LOD role " + r
    for r in ("wing_l", "wing_r"):
        assert (r in m["names"]) == (did in WINGS), r
    assert ("tail" in m["names"]) == (did in TAILS), "tail"
    assert m["parent"][m["names"].index("torso")] == -1
    assert m["names"][m["parent"][m["names"].index("forearm_l")]] == "arm_l"
    assert m["quads"] <= 2800, f"{m['quads']} quads over the 2800 build budget"
    nb = sum(len(b) for b in m["body"])
    ng = sum(len(b) for b in m["glow"])
    assert ng > 0, "no glow quads"
    assert lod["quads"] < m["quads"] / 2, "LOD not under half"
    assert sum(len(b) for b in lod["glow"]) == 0, "LOD has a glow pass"
    ys = [q[1 + 8 * k] for b in m["body"] for q in b for k in range(4)]
    lo, hi = min(ys) * m["scale"], max(ys) * m["scale"]
    assert -0.2 < lo < 0.2, f"feet {lo}"
    head_top = max(q[1 + 8 * k] for q in m["body"][m["names"].index("head")] for k in range(4)) * m["scale"]
    for suffix in ("", "_glow"):
        w, h = png_size(os.path.join(ASSETS, "textures", "entity", "boss_mesh", f"devil_{did}{suffix}.png"))
        assert w == h and w <= 1024 and bin(w).count("1") == 1, f"skin {w}x{h}"
    return f"{did:10s} parts {len(m['names']):2d}  quads {nb} + {ng} = {m['quads']} (LOD {lod['quads']})  height to the horns/wings {hi:.2f} blocks, head-part top {head_top:.2f}"


if __name__ == "__main__":
    bad = 0
    for d in (sys.argv[1:] or IDS):
        try:
            print("OK  " + check(d))
        except Exception as e:                                    # noqa: BLE001
            bad += 1
            print(f"FAIL {d}: {type(e).__name__}: {e}")
    sys.exit(1 if bad else 0)

"""The GeoAssetsTest rules (src/test/java/.../client/geo/GeoAssetsTest.java) for any set of models, without Minecraft or Gradle:

    python3 -B tools/geo_assets_check.py spirit_sylph_fairy spirit_sylph_fairy_wings ...      # model ids
    python3 -B tools/geo_assets_check.py --prefix spiritdive_arm_ --prefix spirit_undine_mermaid

Checks per model: every bone's parent exists, no negative cube size, every uv (box or per-face) lies inside the texture, the texture exists and has exactly the declared size, the glow map (if
any) has the same size, and every bone an animation file moves exists in the model. Exit code 1 on a problem. (geo_check.py is the lighter check the generators run.)
"""
import json
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp")


def check(mid):
    probs = []
    g = json.load(open(os.path.join(ROOT, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    names = {b["name"] for b in g["bones"]}
    for b in g["bones"]:
        if b.get("parent") and b["parent"] not in names:
            probs.append("bone %s: unknown parent %s" % (b["name"], b["parent"]))
        for c in b.get("cubes", []):
            if any(v < 0 for v in c["size"]):
                probs.append("bone %s: negative size" % b["name"])
            uv = c["uv"]
            rects = []
            if isinstance(uv, list):
                sx, sy, sz = c["size"]
                rects.append((uv[0], uv[1], uv[0] + 2 * (sx + sz), uv[1] + sz + sy))
            else:
                for f, d in uv.items():
                    rects.append((d["uv"][0], d["uv"][1], d["uv"][0] + d["uv_size"][0], d["uv"][1] + d["uv_size"][1]))
            for x0, y0, x1, y1 in rects:
                if x0 < -0.01 or y0 < -0.01 or x1 > tw + 0.01 or y1 > th + 0.01:
                    probs.append("bone %s: uv (%s %s %s %s) outside %dx%d" % (b["name"], x0, y0, x1, y1, tw, th))
    tex = os.path.join(ROOT, "textures", "entity", mid + ".png")
    if not os.path.isfile(tex):
        probs.append("missing texture")
    else:
        im = Image.open(tex)
        if im.size != (tw, th):
            probs.append("texture is %s, the model declares %dx%d" % (im.size, tw, th))
        glow = os.path.join(ROOT, "textures", "entity", mid + "_glow.png")
        if os.path.isfile(glow) and Image.open(glow).size != im.size:
            probs.append("glow map size differs")
    ap = os.path.join(ROOT, "animations", "entity", mid + ".animation.json")
    if os.path.isfile(ap):
        for cn, cl in json.load(open(ap))["animations"].items():
            for bn in cl.get("bones", {}):
                if bn not in names:
                    probs.append("animation %s moves the unknown bone %s" % (cn, bn))
    return probs


def main(argv):
    ids, prefixes = [], []
    i = 0
    while i < len(argv):
        if argv[i] == "--prefix":
            i += 1
            prefixes.append(argv[i])
        else:
            ids.append(argv[i])
        i += 1
    geo = os.path.join(ROOT, "geo", "entity")
    for f in sorted(os.listdir(geo)):
        if f.endswith(".geo.json") and any(f.startswith(p) for p in prefixes):
            ids.append(f[:-len(".geo.json")])
    bad = 0
    for mid in ids:
        p = check(mid)
        if p:
            bad += 1
            print("PROBLEMS", mid)
            for x in p:
                print("   ", x)
    print("%d models checked, %d with problems" % (len(ids), bad))
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

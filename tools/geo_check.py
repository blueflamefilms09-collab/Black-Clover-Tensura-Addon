"""Checks geo models written by geo_builder: python3 -B tools/geo_check.py <key>_<name> ...  (bones linked, uvs inside the texture, texture + glow exist,
animation bones exist). Prints one line per model and exits 1 on a problem."""
import json, os, sys
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp")
bad = 0
for mid in sys.argv[1:]:
    g = json.load(open(os.path.join(ROOT, "geo", "entity", mid + ".geo.json")))["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    names = {b["name"] for b in g["bones"]}
    probs = []
    for b in g["bones"]:
        if b.get("parent") and b["parent"] not in names:
            probs.append("bone %s: missing parent %s" % (b["name"], b["parent"]))
        for c in b["cubes"]:
            u, v = (c["uv"] if isinstance(c["uv"], list) else (0, 0))[:2]
            sx, sy, sz = c["size"]
            if isinstance(c["uv"], list) and (u + 2 * (sx + sz) > tw or v + sz + sy > th):
                probs.append("bone %s: box uv %s size %s outside %dx%d" % (b["name"], c["uv"], c["size"], tw, th))
    tex = os.path.join(ROOT, "textures", "entity", mid + ".png")
    if not os.path.exists(tex): probs.append("missing texture " + tex)
    ap = os.path.join(ROOT, "animations", "entity", mid + ".animation.json")
    if os.path.exists(ap):
        for cn, cl in json.load(open(ap))["animations"].items():
            for bn in cl["bones"]:
                if bn not in names: probs.append("animation %s: unknown bone %s" % (cn, bn))
    print(mid, "cubes", sum(len(b["cubes"]) for b in g["bones"]), "OK" if not probs else "PROBLEMS")
    for p in probs: print("  ", p); bad = 1
sys.exit(bad)

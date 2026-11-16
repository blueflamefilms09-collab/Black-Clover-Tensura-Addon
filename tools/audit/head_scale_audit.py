#!/usr/bin/env python3
"""Head-scale audit (docs/body_parts_pass.md): for every geo model with a head / skull / helmet bone, reports the size of the head against the player's 8x8x8 head.

skull ratio = the biggest chunky cube (longest side at most 3 times its shortest: a skull, not a brim, rod or plane) of the head bone and its children, minus decor bones (mane, horn, ear,
              hair, ...), divided by 8
bbox ratio  = the longest side of the box around those cubes / 8
silhouette  = (Beast Will head pieces only) the width and the height of the whole head piece, mane, ears and horns included, divided by 8: that is what reads as "the size of the head"

A model is an OUTLIER when its skull ratio is above LIMIT (1.6) and no EXEMPT rule explains it (a huge creature, a spirit drawn at a small scale, a weapon's head ...). The exempt ones are listed
with their reason, never dropped silently. The Beast Will `_v1` / `_v2` files are the kept old looks and are skipped. Cube rotations are ignored (sizes as written) and the renderer's own scale is
not applied except where EXEMPT says so: both are caveats in the doc.

  python3 tools/audit/head_scale_audit.py            # outliers, exempt (with reason), Beast Will silhouettes, Blender head parts
  python3 tools/audit/head_scale_audit.py --all      # every model that has a head
  python3 tools/audit/head_scale_audit.py --json     # machine-readable rows
"""
import glob
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
RES = os.path.join(ROOT, "src", "main", "resources", "assets", "anime_multiverse")
GEO = os.path.join(RES, "geo")
MESHES = os.path.join(RES, "entity_meshes")
PLAYER_HEAD = 8.0
LIMIT = 1.6                  # skull ratio above this is an outlier
SILHOUETTE_LIMIT = 2.05      # Beast Will head piece (decor included) above this is an outlier; Fenrir is held to 1.75 by the JUnit test
HEAD_NAMES = re.compile(r"^(x_)?(head|skull\d*|helm|hood|bipedhead|armorhead|head\d+)$", re.I)
DECOR = re.compile(r"(mane|horn|ear|hair|crest|hat|ruff|antler|fur|spike|crown|halo|tail|jaw|fin|frill|tusk|beard|wing)", re.I)
# (file regex, reason): large by design, drawn small, or not a head at all. Order matters: the first match is the reason shown.
EXEMPT = [
    (re.compile(r"(nail_hammer|copper_mace)"), "the 'head' of a hammer / mace weapon model, not a body part"),
    (re.compile(r"gnome_full"), "the giant Earth spirit (drawn at 0.82, a body about 8 blocks tall)"),
    (re.compile(r"spiritlight_(cub|seraph)"), "spirit cub drawn at scale 0.25, seraph fx at 0.46 (client/prop/SpiritLightDraw)"),
    (re.compile(r"spirit_(space_wisp|undine|dark_powered)"), "spirit wisp / flat fx quad, not a humanoid head"),
    (re.compile(r"abyssal_(hezrou|lolth)"), "Abyssal demon bosses (a toad-demon and a spider-queen), large by design"),
    (re.compile(r"(dragon|goliath|golem|charybdis_spawn|leviathan|titan|colossus|behemoth)"), "huge creature / boss, large by design"),
    (re.compile(r"(recombination_bull|sandstone_sphinx|beast_form_ghost)"), "large construct / quadruped creature, large by design"),
]
# Beast Will head pieces that read tall only because of a horn / tentacle / ear crown on a normal 1.1x skull: listed, not hidden
BWF_EXEMPT = {
    "kraken": "tentacle crown makes the piece tall; the skull is 1.10x",
    "nidhoggr": "dragon of the world tree, horn crown; the skull is 1.15x",
    "dragon": "dragon horns sweep up; the skull is 1.15x",
    "kitsune": "tall fox ears, 0.11x over the limit; the skull is 1.15x",
}
BWF_HEAD = re.compile(r"^bwf_(?P<stem>[a-z0-9_]+)_arm_head\.geo\.json$")


def reason_of(rel):
    for rx, why in EXEMPT:
        if rx.search(rel):
            return why
    return None


def children(bones, name):
    out, todo = {name}, [name]
    while todo:
        p = todo.pop()
        for b in bones:
            if b.get("parent") == p and b["name"] not in out:
                out.add(b["name"])
                todo.append(b["name"])
    return out


def chunky(s):
    return min(s) > 0 and max(s) <= 3.0 * min(s)


def measure(path):
    """-> list of (identifier, bone, skull_px, bbox_px) for each head bone of the file"""
    try:
        d = json.load(open(path))
    except Exception:
        return []
    res = []
    for g in d.get("minecraft:geometry", []):
        bones = g.get("bones", [])
        ident = g.get("description", {}).get("identifier", "?")
        for hb in [b for b in bones if HEAD_NAMES.match(b["name"])]:
            names = {n for n in children(bones, hb["name"]) if n == hb["name"] or not DECOR.search(n)}
            boxes = [(c["origin"], c["size"]) for b in bones if b["name"] in names for c in b.get("cubes", []) if min(c["size"]) > 0.2]
            if not boxes:
                continue
            solid = [s for o, s in boxes if chunky(s)]
            skull = max(max(s) for s in solid) if solid else 0.0
            lo = [min(o[i] for o, s in boxes) for i in range(3)]
            hi = [max(o[i] + s[i] for o, s in boxes) for i in range(3)]
            res.append((ident, hb["name"], round(skull, 2), round(max(hi[i] - lo[i] for i in range(3)), 2)))
    return res


def audit():
    rows = []
    for f in sorted(glob.glob(os.path.join(GEO, "**", "*.geo.json"), recursive=True)):
        rel = os.path.relpath(f, GEO).replace(os.sep, "/")
        if re.search(r"_v[12]_arm_", rel):
            continue                                                         # the kept old looks
        for ident, bone, skull, bbox in measure(f):
            rows.append({"file": rel, "id": ident, "bone": bone, "skull": skull, "bbox": bbox, "skullRatio": round(skull / PLAYER_HEAD, 2),
                         "bboxRatio": round(bbox / PLAYER_HEAD, 2), "exempt": reason_of(rel)})
    return rows


def beast_will_heads(old=False):
    """the whole head piece of every Beast Will form: -> [{stem, width, height, length, skull}] in pixels"""
    out = []
    for f in sorted(glob.glob(os.path.join(GEO, "entity", "bwf_*_arm_head.geo.json"))):
        m = BWF_HEAD.match(os.path.basename(f))
        if not m or (m.group("stem").endswith("_v2") != old) or m.group("stem").endswith("_v1"):
            continue
        g = json.load(open(f))["minecraft:geometry"][0]
        lo, hi, big = [1e9] * 3, [-1e9] * 3, 0.0
        for b in g["bones"]:
            for c in b.get("cubes", []):
                o, s = c["origin"], c["size"]
                if chunky(s):
                    big = max(big, max(s))
                for i in range(3):
                    lo[i], hi[i] = min(lo[i], o[i]), max(hi[i], o[i] + s[i])
        out.append({"stem": m.group("stem"), "width": round(hi[0] - lo[0], 2), "height": round(hi[1] - lo[1], 2), "length": round(hi[2] - lo[2], 2), "skull": round(big, 2)})
    return out


def mesh_heads():
    """the Blender entity meshes (devils, bosses): the part called head, horns / hair / wings included (their skull cannot be told apart in a mesh) -> [(file, width, height)] in pixels"""
    res = []
    for f in sorted(glob.glob(os.path.join(MESHES, "*.txt"))):
        n = os.path.basename(f)
        if n.endswith("_lod.txt") or n.startswith(("bwtail", "saiyan")):
            continue
        cur, lo, hi = None, [1e9] * 3, [-1e9] * 3
        for line in open(f):
            v = line.split()
            if not v:
                continue
            if v[0] == "part":
                cur = v[1]
            elif v[0] == "qn" and cur == "head" and v[1] == "0":
                for i in range(4):
                    for k in range(3):
                        x = float(v[2 + 8 * i + k])
                        lo[k], hi[k] = min(lo[k], x), max(hi[k], x)
        if hi[0] > lo[0]:
            res.append((n[:-4], round(hi[0] - lo[0], 1), round(hi[1] - lo[1], 1)))
    return res


def main():
    rows = audit()
    if "--json" in sys.argv:
        print(json.dumps({"rows": rows, "beastWill": beast_will_heads()}, indent=1))
        return
    show = rows if "--all" in sys.argv else [r for r in rows if r["skullRatio"] > LIMIT]
    out = [r for r in show if not r["exempt"]]
    ex = [r for r in show if r["exempt"]]
    print("models with a head bone: %d   skull above %.1fx: %d   of them exempt: %d   OUTLIERS left: %d" % (len(rows), LIMIT, len(show), len(ex), len(out)))
    print("\n== OUTLIERS (skull ratio above %.1fx, no exemption) ==" % LIMIT)
    for r in sorted(out, key=lambda r: -r["skullRatio"]):
        print("%5.2fx skull %5.1fpx  bbox %5.2fx  %-62s %s" % (r["skullRatio"], r["skull"], r["bboxRatio"], r["file"], r["bone"]))
    print("\n== EXEMPT (listed, with the reason) ==")
    for r in sorted(ex, key=lambda r: -r["skullRatio"]):
        print("%5.2fx skull %5.1fpx  %-62s %s" % (r["skullRatio"], r["skull"], r["file"], r["exempt"]))
    print("\n== BEAST WILL head pieces: silhouette (mane, ears, horns included) as a multiple of the player's 8 px head; limit %.2fx ==" % SILHOUETTE_LIMIT)
    for b in sorted(beast_will_heads(), key=lambda b: -max(b["width"], b["height"])):
        w, h = b["width"] / PLAYER_HEAD, b["height"] / PLAYER_HEAD
        print("%-18s width %4.2fx  height %4.2fx  length %4.2fx  skull %4.2fx  %s" % (b["stem"], w, h, b["length"] / PLAYER_HEAD, b["skull"] / PLAYER_HEAD, ("EXEMPT: " + BWF_EXEMPT[b["stem"]] if b["stem"] in BWF_EXEMPT else "OVER") if max(w, h) > SILHOUETTE_LIMIT else ""))
    print("\n== BEAST WILL head pieces BEFORE the pass (the kept _v2 models) ==")
    for b in sorted(beast_will_heads(True), key=lambda b: -max(b["width"], b["height"])):
        print("%-22s width %4.2fx  height %4.2fx  length %4.2fx  skull %4.2fx" % (b["stem"], b["width"] / PLAYER_HEAD, b["height"] / PLAYER_HEAD, b["length"] / PLAYER_HEAD, b["skull"] / PLAYER_HEAD))
    print("\n== BLENDER entity meshes, part 'head' (horns, hair and wings included; the skull is not separable, so this is information, not a verdict) ==")
    for n, w, h in sorted(mesh_heads(), key=lambda t: -max(t[1], t[2])):
        print("%-28s width %5.1fpx (%4.2fx)  height %5.1fpx (%4.2fx)" % (n, w, w / PLAYER_HEAD, h, h / PLAYER_HEAD))


if __name__ == "__main__":
    main()

"""Tiny Djinn generator: one chibi geo model per Djinn (the Lesser Summon's figure, client/relics/djinn/tiny), a shared animation file and the textures.

    python3 -B tools/gen_tiny_djinn.py                 # all 36 Djinn
    python3 -B tools/gen_tiny_djinn.py alloces baal    # some of them
    python3 -B tools/gen_tiny_djinn.py --check         # verify what is on disk (cube counts, bones, uv, textures, glow, animation bones); exit 1 on a problem
    python3 -B tools/tiny_djinn/preview.py             # contact sheets into docs/previews/tiny_djinn/

Reads tools/tiny_djinn/specs.json (the hand-curated table: palette, face, hair, headgear, ears, cape, weapon, tail, accessory of each Djinn; make_specs.py writes it),
builds each model from the recipes of tools/tiny_djinn/tdparts.py and paints it with tdpaint.py. Deterministic (hash noise, no random numbers): two runs give identical files.
Writes under src/main/resources/assets/nusmp/:
    geo/entity/tinydjinn_<id>.geo.json                       the model (25 bones, 60 to 170 cubes, 64 x 64)
    textures/entity/tinydjinn_<id>.png + _glow.png           the Djinn in its own light (the glow map is drawn additive and full bright)
    textures/entity/tinydjinn_<id>_dark.png + _dark_glow.png the Dark Djinn: the same geo, black metal and gold, the thorn crown and eye crest only painted here
    animations/entity/tinydjinn_anim.animation.json          idle / cast / attack / hop / poof / exit, shared by all of them
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "tiny_djinn"))
sys.path.insert(0, HERE)

import tdlib as L          # noqa: E402
import tdparts as P        # noqa: E402
import tdanim              # noqa: E402

ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp")
SPECS = os.path.join(HERE, "tiny_djinn", "specs.json")
TEXDIR = os.path.join(ROOT, "textures", "entity")
GEODIR = os.path.join(ROOT, "geo", "entity")
ANIMDIR = os.path.join(ROOT, "animations", "entity")
ANIM_NAME = "tinydjinn_anim"


def load_specs():
    with open(SPECS) as f:
        return json.load(f)["djinn"]


def build(spec):
    c = L.Ctx(spec)
    P.body(c)
    P.HAIR[spec.get("hair", "short")](c)
    for h in spec.get("head", []):
        P.HEAD[h](c)
    P.EARS[spec.get("ears", "none")](c)
    P.CAPE[spec.get("cape", "none")](c)
    P.WEAPON[spec["weapon"]](c)
    P.tail_decor(c, spec.get("tail", "smoke"))
    P.ACC[spec.get("acc", "none")](c)
    return c


def textures(c, spec):
    lp = L.light_palette(spec)
    out = {}
    for tag, pal in (("", lp), ("_dark", L.dark_palette(spec, lp))):
        T = L.Canvas()
        T.paint_band(pal)
        for rects, painter in c.jobs:
            painter(T, pal, rects)
        out[tag] = T.images()
    return out


def write(spec):
    c = build(spec)
    os.makedirs(GEODIR, exist_ok=True)
    os.makedirs(TEXDIR, exist_ok=True)
    c.m.save(None)
    for tag, (img, glow) in textures(c, spec).items():
        img.save(os.path.join(TEXDIR, "tinydjinn_%s%s.png" % (spec["id"], tag)))
        glow.save(os.path.join(TEXDIR, "tinydjinn_%s%s_glow.png" % (spec["id"], tag)))
    return c.count()


def write_anim():
    os.makedirs(ANIMDIR, exist_ok=True)
    a = tdanim.build()
    with open(os.path.join(ANIMDIR, ANIM_NAME + ".animation.json"), "w") as f:
        json.dump(a.data(), f, indent=1)
    return a


def check(specs):
    """The GeoAssetsTest rules for the 36 models, plus the ones of this feature (counts, shared bone set, clip bones, dark texture, glow maps)."""
    from PIL import Image
    bad = 0
    bones_expected = set(L.BONE_NAMES)
    with open(os.path.join(ANIMDIR, ANIM_NAME + ".animation.json")) as f:
        anim = json.load(f)["animations"]
    for cname in tdanim.CLIPS:
        if cname not in anim:
            print("missing clip", cname)
            bad = 1
    for spec in specs:
        mid = "tinydjinn_" + spec["id"]
        probs = []
        g = json.load(open(os.path.join(GEODIR, mid + ".geo.json")))["minecraft:geometry"][0]
        tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
        names = {b["name"] for b in g["bones"]}
        if names != bones_expected:
            probs.append("bones differ: %s" % sorted(names ^ bones_expected))
        n = 0
        for b in g["bones"]:
            if b.get("parent") and b["parent"] not in names:
                probs.append("bone %s: unknown parent" % b["name"])
            for cu in b["cubes"]:
                n += 1
                if any(v < 0 for v in cu["size"]):
                    probs.append("negative size in " + b["name"])
                uv = cu["uv"]
                rects = []
                if isinstance(uv, list):
                    sx, sy, sz = cu["size"]
                    rects.append((uv[0], uv[1], uv[0] + 2 * (sx + sz), uv[1] + sz + sy))
                else:
                    for fd in uv.values():
                        rects.append((fd["uv"][0], fd["uv"][1], fd["uv"][0] + fd["uv_size"][0], fd["uv"][1] + fd["uv_size"][1]))
                for x0, y0, x1, y1 in rects:
                    if x0 < -0.01 or y0 < -0.01 or x1 > tw + 0.01 or y1 > th + 0.01:
                        probs.append("uv outside in " + b["name"])
        if not 55 <= n <= 175:
            probs.append("cube count %d outside 55..175" % n)
        for tag in ("", "_dark"):
            tp = os.path.join(TEXDIR, mid + tag + ".png")
            gp = os.path.join(TEXDIR, mid + tag + "_glow.png")
            if not os.path.isfile(tp) or Image.open(tp).size != (tw, th):
                probs.append("texture %s missing or wrong size" % tag)
            if not os.path.isfile(gp) or Image.open(gp).size != (tw, th):
                probs.append("glow %s missing or wrong size" % tag)
            elif Image.open(gp).convert("RGBA").getchannel("A").getextrema()[1] == 0:
                probs.append("glow %s is empty" % tag)
        for cn, cl in anim.items():
            for bn in cl["bones"]:
                if bn not in names:
                    probs.append("clip %s moves the unknown bone %s" % (cn, bn))
        print("%-16s cubes %3d  %s" % (spec["id"], n, "OK" if not probs else "PROBLEMS"))
        for p in probs:
            print("   ", p)
            bad = 1
    return bad


def main(argv):
    specs = load_specs()
    if "--check" in argv:
        return check(specs)
    ids = [a for a in argv if not a.startswith("-")]
    todo = [s for s in specs if not ids or s["id"] in ids]
    write_anim()
    for s in todo:
        n = write(s)
        print("%-16s %3d cubes" % (s["id"], n))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))

"""Spirit Dive, Djinn-equip style: builds the armour sets of the four dives (Wind / Sylph, Fire / Salamander, Water / Undine, Earth / Gnome) at both tiers, piece by piece.

    python3 -B tools/gen_spirit_dive_djinn.py                       # every set (wind fire water earth, tiers s and c; earth has the standard tier only in the game)
    python3 -B tools/gen_spirit_dive_djinn.py wind                  # one element (both tiers)
    python3 -B tools/gen_spirit_dive_djinn.py wind:c                # one set
    python3 -B tools/gen_spirit_dive_djinn.py wind:c --only helm,crest_c       # some pieces of it
    python3 -B tools/gen_spirit_dive_djinn.py --fit                 # re-fit the panel thinning of every set to the vertex cap (writes tools/sdj_fit.json), then build
    python3 -B tools/gen_spirit_dive_djinn.py --report              # only the numbers of the files already written (faces, vertices, budget)

Files (assets/nusmp): geo/entity/sdj_<element>_<tier>_arm_<piece>.geo.json, textures/entity/sdj_<element>_<tier>_arm_<piece>(.png|_glow.png), animations/entity/<id>.animation.json for the pieces that
move. The piece names of each set are the table book/SpiritDiveDjinnPieces.java (the unit tests compare the files with it).

How it is made: the same geometry kit and painter as the Djinn-quality Requip armours (tools/erza_armour_kit.py, erza_armour_paint.py, gen_erza_armour_core.py: curved plates on lofted surfaces, shingled
lames, engraved glow veins, gems, spikes, blades) with this style's own palettes and specs (tools/sdj_sets.py) and the pieces only the spirits have (tools/sdj_parts.py: crowns and crests, plumes, wings of
plates, mantles, crystal clusters). The panel count of a plate is thinned by COARSE (the draw budget: the vertices of a diver stay under the Djinn Full Equip's, see docs/magic/spirit_dives_djinn.md).
"""
import json
import os
import sys
import time

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import erza_armour_kit as K  # noqa: E402
import erza_armour_paint as PT  # noqa: E402
import gen_erza_armour_parts as PARTS  # noqa: E402
import gen_erza_armour_limbs as LIMBS  # noqa: E402
from gen_erza_armour_core import Ctx  # noqa: E402
import sdj_sets as SETS  # noqa: E402

ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.environ.get("SDJ_ROOT") or os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

# texels per model pixel of a tier (the Djinn atlas is 5.4 to 7.3: the standard tier stays near it, the character tier above it) and the panel thinning (1 = as the Requip armours)
DENSITY = {"s": float(os.environ.get("SDJ_DS", "7")), "c": float(os.environ.get("SDJ_DC", "10"))}
COARSE = {"s": float(os.environ.get("SDJ_KS", "1.4")), "c": float(os.environ.get("SDJ_KC", "1.75"))}
_coarse = [1.0]

_shell = K.Piece.shell


def _thin_shell(self, bone, S, nu, nv, mat, **kw):
    k = _coarse[0]
    if k > 1.001:
        nu = max(1 if nu <= 2 else 2, int(round(nu / k)))
        if nv > 3:
            nv = max(3, int(round(nv / (1.0 + (k - 1.0) * 0.6))))
    return _shell(self, bone, S, nu, nv, mat, **kw)


K.Piece.shell = _thin_shell

# a box thinner than this (model pixels) is a PLATE: the Djinn's own pieces are quads without thickness, so only its two broad faces are written (the edge slivers are 1/32 of a block wide and cost
# a quad each, in two passes); the standard tier thins more plates than the character tier
THIN = {"s": float(os.environ.get("SDJ_TS", "0.58")), "c": float(os.environ.get("SDJ_TC", "0.52"))}
_tier = ["c"]
_cull0 = K.Piece._cull
AXIS_OF = {"west": 0, "east": 0, "up": 1, "down": 1, "north": 2, "south": 2}


def _cull_thin(self, anim):
    _cull0(self, anim)
    thr = THIN[_tier[0]]
    for cu in self.cubes:
        a = min(range(3), key=lambda i: cu.s[i])
        if cu.s[a] > thr:
            continue
        for f in K.FACES:
            if AXIS_OF[f] != a and cu.faces[f] and f not in cu.keep:
                cu.faces[f] = False


K.Piece._cull = _cull_thin


# the shapes the Requip painter does not have
def _leaf():
    import math
    pts = []
    for i in range(25):
        t = i / 24.0
        pts.append((0.5 * math.sin(math.pi * t) ** 0.85 * (1.0 if t < 0.6 else 0.8), -1 + 2 * t))
    return [pts + [(-x, y) for x, y in reversed(pts)]]


PT.SHAPES["leaf"] = _leaf()
PT.SHAPES["crest"] = [[(0, -1), (0.25, -0.5), (0.62, -0.15), (0.35, 0.05), (0.5, 0.6), (0, 0.3), (-0.5, 0.6), (-0.35, 0.05), (-0.62, -0.15), (-0.25, -0.5)]]


def clean_glow(path):
    """The SafeAdditive rule: a glow map carries no colour under alpha 0 (the additive blend of the game would draw it as a box)."""
    import numpy as np
    from PIL import Image
    a = np.array(Image.open(path).convert("RGBA"))
    a[a[..., 3] == 0, :3] = 0
    Image.fromarray(a, "RGBA").save(path, optimize=True)


class DCtx(Ctx):
    """A Requip context keyed for this kit: files are sdj_<element>_<tier>_arm_<piece>."""

    def __init__(self, el, tier):
        super().__init__(el + "_" + tier, SETS.PAL[(el, tier)], SETS.SPEC[(el, tier)], density=DENSITY[tier])
        self.el, self.tier = el, tier
        self.key = "sdj_%s_%s" % (el, tier)
        self.ornate = tier == "c"

    def done(self, p, anim=None):
        r = p.save(anim, root=ASSETS)
        clean_glow(os.path.join(ASSETS, "textures", "entity", p.id + "_glow.png"))
        self.made.append(r)
        return r


STANDARD = ("helm", "gorget", "chest", "backplate", "belt", "skirt", "pauldron", "vambrace", "gauntlet", "cuisse", "greave", "sabaton")


# the vertex cap of a set (the Djinn Full Equip's quads x 4, minus the vein layer and the extras: book/SpiritDiveDjinnPieces.pieceVertexCap; the Java test pins the real numbers)
CAP = {"c": 4351 * 4 - 21 * 4 - 576, "s": 2856 * 4 - 13 * 4 - 576}
FIT_FILE = os.path.join(HERE, "sdj_fit.json")


def fit_of(el, tier):
    """The panel thinning and the sliver threshold of a set: the fitted numbers (sdj_fit.json, written by --fit) or the tier's defaults."""
    try:
        with open(FIT_FILE) as fh:
            f = json.load(fh).get("%s:%s" % (el, tier))
    except (OSError, ValueError):
        f = None
    return (f["k"], f["thin"]) if f else (COARSE[tier], THIN[tier])


def vertices(made, tier):
    glow = SETS.GLOW_C if tier == "c" else SETS.GLOW_S
    return sum(r["faces"] * 4 * (2 if r["id"].split("_arm_")[1] in glow else 1) for r in made)


def build(el, tier, only=None, fit=None, quiet=False):
    import sdj_parts as XP
    c = DCtx(el, tier)
    k, thin = fit or fit_of(el, tier)
    _coarse[0] = k
    THIN[tier] = thin
    _tier[0] = tier
    t0 = time.time()
    want = set(SETS.CORE)
    if tier == "c":
        want |= set(SETS.ORNATE)
    extra = set(SETS.EXTRA[(el, tier)])
    for n in extra:
        if n in ("cuisse", "skirt"):
            want.add(n)
    for name in STANDARD:
        fn = getattr(PARTS, name, None) or getattr(LIMBS, name, None)
        if fn is None or name not in want or (only and name not in only and not any(o.startswith(name) for o in only)):
            continue
        fn(c)
    for ex in sorted(extra - {"cuisse", "skirt"}, key=lambda n: XP.ORDER.index(n.split("_")[0]) if n.split("_")[0] in XP.ORDER else 99):
        if only and ex not in only:
            continue
        XP.build(c, ex)
    cubes = sum(r["cubes"] for r in c.made)
    faces = sum(r["faces"] for r in c.made)
    verts = vertices(c.made, tier)
    if quiet:
        return verts
    print("%-10s pieces %2d cubes %5d faces %5d vertices (body + glow passes) %6d of %d  (K %.2f, plate %.2f)  (%.1f s)" % (el + ":" + tier, len(c.made), cubes, faces, verts, CAP[tier], k, thin, time.time() - t0))
    for r in c.made:
        print("   %-34s cubes %4d faces %4d tex %s" % (r["id"], r["cubes"], r["faces"], "%dx%d" % r["tex"]))
    return c.made


def fit_all(todo):
    """Finds, for every set, the least thinning (K, then the sliver threshold) that brings its vertices under the cap, and writes sdj_fit.json (the normal build reads it, so the files are reproducible)."""
    global ASSETS
    real = ASSETS
    import tempfile
    out = {}
    with tempfile.TemporaryDirectory(prefix="sdj_fit_") as tmp:
        ASSETS = tmp
        try:
            for el, t in todo:
                k0, th0 = COARSE[t], THIN[t]
                best, v = None, 0
                for step in range(0, 40):
                    k = round(k0 + 0.1 * step, 2)
                    v = build(el, t, fit=(k, th0), quiet=True)
                    if v <= CAP[t]:
                        best = (k, th0, v)
                        break
                if best is None:
                    for th in (0.62, 0.7, 0.8):
                        v = build(el, t, fit=(4.0, th), quiet=True)
                        if v <= CAP[t]:
                            best = (4.0, th, v)
                            break
                if best is None:
                    raise SystemExit("%s:%s cannot be fitted under %d vertices (last %d)" % (el, t, CAP[t], v))
                out["%s:%s" % (el, t)] = {"k": best[0], "thin": best[1], "vertices": best[2], "cap": CAP[t]}
                print("fit %s:%s  K %.2f  plate %.2f  -> %d of %d vertices" % (el, t, best[0], best[1], best[2], CAP[t]))
        finally:
            ASSETS = real
    try:
        with open(FIT_FILE) as fh:
            old = json.load(fh)
    except (OSError, ValueError):
        old = {}
    old.update(out)
    with open(FIT_FILE, "w") as fh:
        json.dump(old, fh, indent=1, sort_keys=True)
        fh.write("\n")


def report():
    geo = os.path.join(ASSETS, "geo", "entity")
    tot = {}
    for fn in sorted(os.listdir(geo)):
        if not fn.startswith("sdj_") or not fn.endswith(".geo.json"):
            continue
        with open(os.path.join(geo, fn)) as fh:
            g = json.load(fh)
        faces = cubes = 0
        for b in g["minecraft:geometry"][0]["bones"]:
            for cu in b["cubes"]:
                cubes += 1
                faces += len(cu["uv"])
        key = fn.split("_arm_")[0]
        t = tot.setdefault(key, [0, 0, 0, 0])
        glow = SETS.GLOW_C if key.endswith("_c") else SETS.GLOW_S
        t[0] += 1
        t[1] += cubes
        t[2] += faces
        t[3] += faces * 4 * (2 if fn.split("_arm_")[1][:-len(".geo.json")] in glow else 1)
    for k, (n, cu, f, v) in tot.items():
        print("%-12s pieces %2d cubes %5d faces %5d vertices (body + glow passes) %6d" % (k, n, cu, f, v))


def main(argv):
    only = None
    if "--only" in argv:
        i = argv.index("--only")
        only = set(argv[i + 1].split(","))
        argv = argv[:i] + argv[i + 2:]
    if "--report" in argv:
        report()
        return
    fit = "--fit" in argv
    todo = []
    for a in [a for a in argv if not a.startswith("--")] or list(SETS.ELEMENTS):
        el, _, tier = a.partition(":")
        for t in ([tier] if tier else SETS.TIERS):
            if el == "earth" and t == "c":
                continue                                                  # Earth has no character in the mod: the standard set only
            todo.append((el, t))
    if fit:
        fit_all(todo)
    for el, t in todo:
        build(el, t, only)


if __name__ == "__main__":
    main(sys.argv[1:])

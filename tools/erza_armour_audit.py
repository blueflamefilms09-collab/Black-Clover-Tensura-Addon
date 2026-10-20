"""Erza armour audit: measures the Djinn armour sets (the quality bar the owner named for the Erza remake) and the Erza Requip armours against the same numbers.

    python3 -B tools/erza_armour_audit.py                    # the tables on the screen
    python3 -B tools/erza_armour_audit.py --md out.md        # the same as a Markdown fragment (docs/erza_armour_remake.md embeds it)
    python3 -B tools/erza_armour_audit.py --write-bar        # writes src/test/resources/erza_armour_bar.json (the numbers the JUnit audit compares with)
    python3 -B tools/erza_armour_audit.py --json out.json    # every number of every set and piece
    python3 -B tools/erza_armour_audit.py --erza             # only the Erza sets
    python3 -B tools/erza_armour_audit.py --djinn            # only the Djinn sets

What is measured (all of it from the files in the jar, nothing is assumed):

  Djinn  assets/nusmp/djinn/<set>/<piece>.txt   (baked quad meshes, one set per Djinn look: base, u_ = unique, d_ = dark), textures/entity/djinn/<id>_[u_|d_]armor[_glow].png
  Erza   assets/nusmp/geo/entity/erza_<armour>_arm_<piece>.geo.json (+ texture + _glow)   (GeoLite boxes with a per-face uv)

  pieces          model files of the set
  faces           drawn polygons: a Djinn quad, a GeoLite box face that has a uv (a box has up to six). This is the number to compare ("quads" and "faces" are the same thing here)
  cubes           GeoLite boxes (Djinn: faces / 6, the number of boxes it would take at one box per six faces, for scale only)
  glow faces      polygons of the emissive pass (Djinn: layer 2 quads; Erza: every face of a piece that has a glow map with any painted texel, because the glow pass redraws the model)
  texture         size of the skin of the set (Djinn: one atlas for the whole set) / the largest and the sum of the pieces' textures (Erza), and the painted texels (alpha > 0)
  density         texels per model pixel (1/16 block): sqrt(uv area in texels / surface area in model pixels), per polygon, area weighted; median over the set
  palette         distinct colours (5 bits per channel) that hold at least 0.1 percent of the painted texels of the skin: the richness of the paint
  glow palette    the same for the emissive map
  extents         the bounding box of the standard slots (helm, chest, pauldron, vambrace, gauntlet, greave) in the body frame (feet y = 0, shoulders y = 24, the right arm at
                  x = -5, front on -z) and its ratio to the body part it is anchored on: > 1.02 means the solid overlay is at least 2 percent larger than the skin
"""
import glob
import json
import math
import os
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
BAR_FILE = os.path.join(ROOT, "src", "test", "resources", "erza_armour_bar.json")

# the vanilla player model: pivot of each anchor part (x, y down from the neck line, z) and the box of the skin it carries, in the body frame (feet y = 0)
ANCHORS = {"head": (0, 0, 0), "body": (0, 0, 0), "arm_r": (-5, 2, 0), "arm_l": (5, 2, 0), "leg_r": (-1.9, 12, 0), "leg_l": (1.9, 12, 0)}
SKIN = {"head": ((-4, 24, -4), (4, 32, 4)), "body": ((-4, 12, -2), (4, 24, 2)), "arm_r": ((-8, 12, -2), (-4, 24, 2)), "arm_l": ((4, 12, -2), (8, 24, 2)),
        "leg_r": ((-3.9, 0, -2), (0, 12, 2)), "leg_l": ((0, 0, -2), (3.9, 12, 2))}
# the slot of a Djinn piece (DjinnArmorLayer anchors) and of an Erza piece (requip/ArmourSets anchors)
DJINN_SLOT = {"helm": "head", "chest": "body", "belt": "body", "pauldron_r": "arm_r", "pauldron_l": "arm_l", "vambrace_r": "arm_r", "vambrace_l": "arm_l",
              "gauntlet_r": "arm_r", "gauntlet_l": "arm_l", "greave_r": "leg_r", "greave_l": "leg_l"}
STANDARD = ("helm", "chest", "pauldron_r", "vambrace_r", "gauntlet_r", "greave_r")


# ====================================================================================================================== helpers
def _palette(png, min_share=0.001):
    """(painted texels, distinct 5-bit colours holding >= min_share of them, size) of a texture, or (0, 0, (0, 0)) when it is missing."""
    if not os.path.isfile(png):
        return 0, 0, (0, 0)
    im = np.asarray(Image.open(png).convert("RGBA"))
    h, w = im.shape[:2]
    op = im[..., 3] > 0
    n = int(op.sum())
    if n == 0:
        return 0, 0, (w, h)
    q = (im[..., 0].astype(np.uint32) >> 3) << 10 | (im[..., 1].astype(np.uint32) >> 3) << 5 | (im[..., 2].astype(np.uint32) >> 3)
    _, counts = np.unique(q[op], return_counts=True)
    return n, int((counts >= max(1, n * min_share)).sum()), (w, h)


def _x2(a, b):
    return float(a[0] * b[1] - a[1] * b[0])


def _median(xs):
    xs = sorted(xs)
    return xs[len(xs) // 2] if xs else 0.0


def _wmedian(vals, weights):
    """Area weighted median."""
    if not vals:
        return 0.0
    o = np.argsort(vals)
    v = np.asarray(vals)[o]
    w = np.asarray(weights)[o]
    c = np.cumsum(w)
    return float(v[int(np.searchsorted(c, c[-1] / 2.0))])


def _bbox(pts):
    a = np.asarray(pts, dtype=np.float64)
    return a.min(axis=0).tolist(), a.max(axis=0).tolist()


def _ratio(bb, slot):
    (x0, y0, z0), (x1, y1, z1) = bb
    (sx0, sy0, sz0), (sx1, sy1, sz1) = SKIN[slot]
    return [round((x1 - x0) / (sx1 - sx0), 3), round((y1 - y0) / (sy1 - sy0), 3), round((z1 - z0) / (sz1 - sz0), 3)]


# ====================================================================================================================== Djinn
def djinn_set(name):
    """Measures one Djinn set directory name ('agares', 'u_agares', 'd_agares')."""
    kind = "unique" if name.startswith("u_") else "dark" if name.startswith("d_") else "base"
    did = name[2:] if kind != "base" else name
    folder = os.path.join(ASSETS, "djinn", name)
    suffix = {"base": "", "unique": "_u", "dark": "_d"}[kind]
    skin = os.path.join(ASSETS, "textures", "entity", "djinn", "%s%s_armor.png" % (did, suffix))
    glow = os.path.join(ASSETS, "textures", "entity", "djinn", "%s%s_armor_glow.png" % (did, suffix))
    tw, th = Image.open(skin).size if os.path.isfile(skin) else (1024, 1024)
    k = (tw / 128.0) * (th / 128.0)                                   # uv units are 1/128 of the texture: texel area of one uv unit squared
    pieces, faces, glow_faces = {}, 0, 0
    dens, wts = [], []
    ext = {}
    for f in sorted(glob.glob(os.path.join(folder, "*.txt"))):
        pname = os.path.basename(f)[:-4]
        pts, n, ng = [], 0, 0
        for line in open(f):
            if not line.startswith("q"):
                continue
            p = line.split()
            layer = int(p[1]) if p[0] in ("qn", "q") else 0
            vals = [float(x) for x in p[2:]]
            stride = 8 if p[0] == "qn" else 5
            v = [vals[i * stride:i * stride + stride] for i in range(4)]
            n += 1
            if layer == 2:
                ng += 1
            xyz = np.array([x[:3] for x in v])
            uv = np.array([x[3:5] for x in v])
            pts.extend(xyz.tolist())
            wa = 0.5 * (np.linalg.norm(np.cross(xyz[1] - xyz[0], xyz[2] - xyz[0])) + np.linalg.norm(np.cross(xyz[2] - xyz[0], xyz[3] - xyz[0])))
            ua = 0.5 * (abs(_x2(uv[1] - uv[0], uv[2] - uv[0])) + abs(_x2(uv[2] - uv[0], uv[3] - uv[0]))) * k
            if wa > 1e-4 and layer == 0:
                dens.append(math.sqrt(ua / wa))
                wts.append(wa)
        faces += n
        glow_faces += ng
        pieces[pname] = n
        if pname in DJINN_SLOT and pts:
            slot = DJINN_SLOT[pname]
            ax, ay, az = ANCHORS[slot]
            body = [(ax + x, 24.0 - (ay + y), az + z) for x, y, z in pts]       # the mesh is in the part's own frame (y down): to the body frame (y up)
            bb = _bbox(body)
            ext[pname] = {"slot": slot, "min": [round(v, 2) for v in bb[0]], "max": [round(v, 2) for v in bb[1]], "ratio": _ratio(bb, slot)}
    painted, pal, _ = _palette(skin)
    gpainted, gpal, gsize = _palette(glow)
    return {"set": name, "kind": kind, "id": did, "pieces": len(pieces), "faces": faces, "glow_faces": glow_faces, "cubes_equiv": round(faces / 6.0, 1),
            "texture": [tw, th], "glow_texture": list(gsize), "painted_texels": painted, "glow_painted_texels": gpainted, "palette": pal, "glow_palette": gpal,
            "density_median": round(_wmedian(dens, wts), 2), "density_mean": round(float(np.average(dens, weights=wts)) if dens else 0.0, 2),
            "passes": 3, "extents": ext, "piece_faces": pieces}


def djinn_all():
    out = []
    root = os.path.join(ASSETS, "djinn")
    for name in sorted(os.listdir(root)):
        if os.path.isdir(os.path.join(root, name)):
            out.append(djinn_set(name))
    return out


# ====================================================================================================================== Erza
SLOT_OF_PIECE = {"helm": "head", "gorget": "body", "chest": "body", "backplate": "body", "belt": "body", "skirt": "body", "pauldron_r": "arm_r", "pauldron_l": "arm_l",
                 "vambrace_r": "arm_r", "vambrace_l": "arm_l", "gauntlet_r": "arm_r", "gauntlet_l": "arm_l", "greave_r": "leg_r", "greave_l": "leg_l",
                 "cuisse_r": "leg_r", "cuisse_l": "leg_l", "sabaton_r": "leg_r", "sabaton_l": "leg_l"}


def _rot(rx, ry, rz):
    """The rotation matrix of a GeoLite cube rotation (degrees, the file's y up frame); only the extents matter here, the convention (x, then y, then z) is the file's."""
    a, b, c = map(math.radians, (rx, ry, rz))
    ca, sa, cb, sb, cc, sc = math.cos(a), math.sin(a), math.cos(b), math.sin(b), math.cos(c), math.sin(c)
    mx = np.array([[1, 0, 0], [0, ca, -sa], [0, sa, ca]])
    my = np.array([[cb, 0, sb], [0, 1, 0], [-sb, 0, cb]])
    mz = np.array([[cc, -sc, 0], [sc, cc, 0], [0, 0, 1]])
    return mz @ my @ mx


def geo_stats(path):
    """Cubes, drawn faces, density samples and corner points of one geo file."""
    g = json.load(open(path))["minecraft:geometry"][0]
    tw, th = g["description"]["texture_width"], g["description"]["texture_height"]
    cubes = faces = 0
    dens, wts, pts = [], [], []
    bones = g["bones"]
    for b in bones:
        for c in b["cubes"]:
            cubes += 1
            ox, oy, oz = c["origin"]
            sx, sy, sz = c["size"]
            inf = c.get("inflate", 0.0)
            lo = np.array([ox - inf, oy - inf, oz - inf])
            hi = np.array([ox + sx + inf, oy + sy + inf, oz + sz + inf])
            corners = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
            if "rotation" in c:
                piv = np.array(c.get("pivot", b["pivot"]))
                corners = (corners - piv) @ _rot(*c["rotation"]).T + piv
            pts.extend(corners.tolist())
            uv = c["uv"]
            dims = {"west": (sz + 2 * inf, sy + 2 * inf), "east": (sz + 2 * inf, sy + 2 * inf), "north": (sx + 2 * inf, sy + 2 * inf), "south": (sx + 2 * inf, sy + 2 * inf),
                    "up": (sx + 2 * inf, sz + 2 * inf), "down": (sx + 2 * inf, sz + 2 * inf)}
            if isinstance(uv, dict):
                for face, fd in uv.items():
                    faces += 1
                    # Bedrock names its sides from the other end of x: east = the model's -x side; the area does not care which
                    name = {"east": "west", "west": "east"}.get(face, face)
                    wa = dims[name][0] * dims[name][1]
                    ua = abs(fd["uv_size"][0] * fd["uv_size"][1])
                    if wa > 1e-4:
                        dens.append(math.sqrt(ua / wa))
                        wts.append(wa)
            else:
                faces += 6
                for name, (a, bb) in dims.items():
                    wa = a * bb
                    ua = {"west": sz * sy, "east": sz * sy, "north": sx * sy, "south": sx * sy, "up": sx * sz, "down": sx * sz}[name]
                    if wa > 1e-4:
                        dens.append(math.sqrt(ua / wa))
                        wts.append(wa)
    return {"cubes": cubes, "faces": faces, "tex": [tw, th], "bones": len(bones), "dens": dens, "wts": wts, "pts": pts}


def erza_armours():
    """The armour keys from the files (erza_<armour>_arm_<piece>.geo.json), longest key first so heavens_wheel does not eat grand_chariot."""
    keys = set()
    for f in glob.glob(os.path.join(ASSETS, "geo", "entity", "erza_*_arm_*.geo.json")):
        n = os.path.basename(f)[:-len(".geo.json")]
        keys.add(n.split("_arm_")[0])
    return sorted(keys)


def erza_set(key):
    pieces, faces, cubes, glow_faces = {}, 0, 0, 0
    dens, wts = [], []
    tex_max, tex_sum, painted, gpainted, gtex = 0, 0, 0, 0, 0
    pal_total, gpal_total = {}, {}
    ext, sizes = {}, {}
    folder = os.path.join(ASSETS, "geo", "entity")
    anim_pieces = 0
    all_pts = []
    for f in sorted(glob.glob(os.path.join(folder, key + "_arm_*.geo.json"))):
        pname = os.path.basename(f)[:-len(".geo.json")].split("_arm_", 1)[1]
        s = geo_stats(f)
        pieces[pname] = s["faces"]
        faces += s["faces"]
        cubes += s["cubes"]
        dens += s["dens"]
        wts += s["wts"]
        tex_max = max(tex_max, s["tex"][0])
        tex_sum += s["tex"][0] * s["tex"][1]
        sizes[pname] = s["tex"][0]
        base = os.path.join(ASSETS, "textures", "entity", key + "_arm_" + pname)
        pn, pal, _ = _palette(base + ".png")
        gn, gpal, gsz = _palette(base + "_glow.png")
        painted += pn
        gpainted += gn
        gtex += gsz[0] * gsz[1]
        if gn > 0:
            glow_faces += s["faces"]
        if os.path.isfile(os.path.join(ASSETS, "animations", "entity", key + "_arm_" + pname + ".animation.json")):
            anim_pieces += 1
        for kk, v in (("p", pal), ):
            pal_total[pname] = pal
        gpal_total[pname] = gpal
        if pname in SLOT_OF_PIECE and s["pts"]:
            bb = _bbox(s["pts"])
            slot = SLOT_OF_PIECE[pname]
            ext[pname] = {"slot": slot, "min": [round(v, 2) for v in bb[0]], "max": [round(v, 2) for v in bb[1]], "ratio": _ratio(bb, slot)}
    return {"set": key, "kind": "erza", "id": key[5:], "pieces": len(pieces), "faces": faces, "glow_faces": glow_faces, "cubes": cubes, "texture_max": tex_max,
            "texture_sum_texels": tex_sum, "painted_texels": painted, "glow_painted_texels": gpainted, "palette": max(pal_total.values()) if pal_total else 0,
            "palette_median": _median(list(pal_total.values())), "glow_palette": max(gpal_total.values()) if gpal_total else 0,
            "density_median": round(_wmedian(dens, wts), 2), "density_mean": round(float(np.average(dens, weights=wts)) if dens else 0.0, 2),
            "animated_pieces": anim_pieces, "passes": 3, "extents": ext, "piece_faces": pieces, "piece_texture": sizes}


def erza_all():
    return [erza_set(k) for k in erza_armours()]


# ====================================================================================================================== the bar
def bar_of(djinn):
    """The Djinn minimum the Erza sets must reach: over the 36 unique full-equip sets (u_), the sets that carry the Djinn's own look, the number that counts as 'the Djinn standard'."""
    u = [d for d in djinn if d["kind"] == "unique"]
    allsets = djinn
    def mn(key, sets=u):
        return min(d[key] for d in sets)
    return {
        "source": "min over the %d unique Djinn sets (assets/nusmp/djinn/u_*), measured by tools/erza_armour_audit.py" % len(u),
        "pieces_min": mn("pieces"), "faces_min": mn("faces"), "glow_faces_min": mn("glow_faces"),
        "painted_texels_min": mn("painted_texels"), "glow_painted_texels_min": mn("glow_painted_texels"),
        "palette_min": mn("palette"), "glow_palette_min": mn("glow_palette"),
        "density_min": min(d["density_median"] for d in u),
        "texture_side_min": min(d["texture"][0] for d in u),
        "all_sets_pieces_min": min(d["pieces"] for d in allsets), "all_sets_faces_min": min(d["faces"] for d in allsets),
        "all_sets_count": len(allsets),
    }


# ====================================================================================================================== output
def stat(xs, f=lambda v: v):
    return "%s / %s / %s" % (f(min(xs)), f(sorted(xs)[len(xs) // 2]), f(max(xs)))


def tables(djinn, erza):
    L = []
    if djinn:
        L.append("### Djinn sets (min / median / max)\n")
        L.append("| Look | Sets | Pieces | Faces (quads) | Glow faces | Skin texture | Painted texels | Palette | Glow palette | Density (texels per model px, median) |")
        L.append("|---|---|---|---|---|---|---|---|---|---|")
        for kind, label in (("base", "shared rig"), ("unique", "unique look (u_)"), ("dark", "dark look (d_)")):
            s = [d for d in djinn if d["kind"] == kind]
            if not s:
                continue
            L.append("| %s | %d | %s | %s | %s | %s | %s | %s | %s | %s |" % (
                label, len(s), stat([d["pieces"] for d in s]), stat([d["faces"] for d in s]), stat([d["glow_faces"] for d in s]),
                "%dx%d" % tuple(s[0]["texture"]) + (" (all)" if len({tuple(d["texture"]) for d in s}) == 1 else " (mixed)"),
                stat([d["painted_texels"] for d in s]), stat([d["palette"] for d in s]), stat([d["glow_palette"] for d in s]), stat([d["density_median"] for d in s])))
        L.append("")
    if erza:
        L.append("### Erza sets\n")
        L.append("| Armour | Pieces | Cubes | Faces | Glow faces | Largest piece texture | Sum of texture texels | Painted texels | Palette (best piece) | Glow palette | Density (median) | Animated pieces |")
        L.append("|---|---|---|---|---|---|---|---|---|---|---|---|")
        for e in erza:
            L.append("| %s | %d | %d | %d | %d | %d | %d | %d | %d | %d | %s | %d |" % (e["id"], e["pieces"], e["cubes"], e["faces"], e["glow_faces"], e["texture_max"],
                                                                                    e["texture_sum_texels"], e["painted_texels"], e["palette"], e["glow_palette"], e["density_median"], e["animated_pieces"]))
        L.append("")
    return "\n".join(L)


def extents_table(sets, title, pick=STANDARD):
    L = ["### %s: silhouette of the standard slots (size in model px / ratio to the body part, x y z)\n" % title, "| Set | " + " | ".join(pick) + " |", "|---|" + "---|" * len(pick)]
    for s in sets:
        cells = []
        for p in pick:
            e = s["extents"].get(p)
            if not e:
                cells.append("-")
                continue
            sz = [round(e["max"][i] - e["min"][i], 1) for i in range(3)]
            cells.append("%sx%sx%s (%s)" % (sz[0], sz[1], sz[2], "/".join("%.2f" % v for v in e["ratio"])))
        L.append("| %s | %s |" % (s["set"], " | ".join(cells)))
    L.append("")
    return "\n".join(L)


def main(argv):
    do_djinn = "--erza" not in argv
    do_erza = "--djinn" not in argv
    djinn = djinn_all() if do_djinn else []
    erza = erza_all() if do_erza else []
    txt = tables(djinn, erza)
    if djinn:
        bar = bar_of(djinn)
        txt += "\n### The bar (the Djinn minimum the Erza armours must reach)\n\n" + "\n".join("* `%s`: %s" % (k, v) for k, v in bar.items()) + "\n"
        if "--write-bar" in argv:
            os.makedirs(os.path.dirname(BAR_FILE), exist_ok=True)
            json.dump(bar, open(BAR_FILE, "w"), indent=1, sort_keys=True)
            print("wrote", os.path.relpath(BAR_FILE, ROOT))
    if "--md" in argv:
        out = argv[argv.index("--md") + 1]
        body = txt
        if djinn:
            sample = [d for d in djinn if d["id"] in ("agares", "zagan", "valefor") and d["kind"] == "unique"] + [d for d in erza]
            body += "\n" + extents_table(sample, "Slots")
        open(out, "w").write(body)
        print("wrote", out)
    if "--json" in argv:
        out = argv[argv.index("--json") + 1]
        json.dump({"djinn": djinn, "erza": erza}, open(out, "w"), indent=1)
        print("wrote", out)
    print(txt)
    if djinn and erza:
        print(extents_table([d for d in djinn if d["set"] in ("u_agares", "u_zagan")] + erza, "Slots"))


if __name__ == "__main__":
    main(sys.argv[1:])

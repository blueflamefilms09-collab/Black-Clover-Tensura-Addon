"""weapon_sizes: the in-hand size rule of daggers and katanas (1.110.0, docs/visual_upgrade/weapon_sizes.md).

The owner's report: the dagger is tiny in first and third person, and the katanas are thin and small looking. The measured cause (see --table):
  * every dagger / katana blade is a sliver: a katana blade is 1.1 pixels wide and 0.2 pixels thick in the hand (a vanilla sword's blade is about 2.5 pixels
    wide), its grip is 1 pixel, and a dagger blade is 0.4 to 1 pixels wide;
  * four daggers have a hand-written first-person display with scale 0.383 (a 0.27 block weapon in the hand), and the others sit at 0.44 blocks.

This tool keeps every old mesh file and writes NEW thickened copies next to them, which the renderer prefers when they exist:
    assets/nusmp/weapon_meshes/<id>.txt            ->  assets/nusmp/weapon_meshes_sz/<id>.txt
    assets/nusmp/weapon_meshes/up/<id>[_lod].txt   ->  assets/nusmp/weapon_meshes_sz/up/<id>[_lod].txt        (the Genesis / God melee upgrade meshes)
    assets/nusmp/weapon_meshes_vu/<id>[_lod].txt   ->  assets/nusmp/weapon_meshes_vu_sz/<id>[_lod].txt        (the Genesis visual upgrade meshes)
and rewrites ONLY firstperson_* of the model json of a dagger (its first-person scale), as display_presets.py does.

THE RULE (the numbers are assets/nusmp/weapon_sizes.json, read by the Java class visual/sizes/WeaponSizeRule and by its tests)
  Mesh space: y along the weapon, x across the blade (width), z through it (thickness). The mesh is cut into 32 slices along y (weapon_orientation.measure).
  * Blade section: the median width / depth of the slices at 45 to 70 percent of the length (the same stretch for every weapon: past every guard, before the taper).
  * Length (daggers only): scaled about the grip up to `len3p` apparent third-person blocks (never smaller); katanas keep their length.
  * Cross section: the width is scaled about the blade's own centre line (a curved katana stays curved) by kb_x so the blade reaches `bw_min_1p` apparent blocks in
    FIRST person (the first-person display scale of the model json) and `bw_min_3p` in third person; the thickness by kb_z up to `bd_min` blocks (the visible spine).
    The handle (below the guard) grows to `handle_min` blocks, the guard (tsuba / crossguard) to `guard_min` and at least `guard_over_blade` times the new blade base
    width. The factors run along the weapon as a smooth profile (handle -> guard -> blade with a short flared collar), so no vertex is dragged across the mesh:
    quads that cross a knot are cut at it first.
  Everything is applied to the body, void, glow and vein layers of the static group (the halo, flares and orbiting groups of an upgraded mesh stay as they are).

    python tools/relics/weapon_sizes.py --table            before / after table of every dagger and katana (and the other sword classes for comparison)
    python tools/relics/weapon_sizes.py --apply            write the meshes and the dagger model jsons
    python tools/relics/weapon_sizes.py --check            exit 1 if --apply would change anything
    python tools/relics/weapon_sizes.py --ids a,b          limit to some ids
"""
import argparse
import glob
import json
import math
import os
import statistics
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import weapon_classes as wc          # noqa: E402
import weapon_orientation as wo      # noqa: E402
import display_presets as dp         # noqa: E402

ROOT = wc.ROOT
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MODELS = os.path.join(ASSETS, "models", "item")
RULES_JSON = os.path.join(ASSETS, "weapon_sizes.json")
DIRS = (("weapon_meshes", "weapon_meshes_sz", "old"), ("weapon_meshes/up", "weapon_meshes_sz/up", "up"), ("weapon_meshes_vu", "weapon_meshes_vu_sz", "vu"))
BINS = wo.BINS
BLADE_FROM, BLADE_TO = 0.45, 0.70        # the stretch of the length measured as "the blade section" (a guard never reaches it, a taper has not begun)
MARK = "# nusmp_weapon_sizes "


def rules():
    with open(RULES_JSON, encoding="utf-8") as f:
        return json.load(f)


# ================================================================ files
class MeshFile:
    """A weapon mesh text: its lines, with the quads of the static body parsed so they can be cut and moved."""

    def __init__(self, path, lines=None):
        self.path = path
        self.lines = []
        self.vu = False
        if lines is not None:
            self.lines = list(lines)
        else:
            with open(path, encoding="utf-8") as f:
                for raw in f:
                    self.lines.append(raw.rstrip("\n"))
        for ln in self.lines:
            if ln.startswith("vu "):
                self.vu = True
                break
        self.place = None
        for ln in self.lines:
            v = ln.split()
            if v and v[0] == "place":
                self.place = [float(t) for t in v[1:6]]
                break

    def records(self):
        """Yield (index, kind, layer, verts, normal|None, transformable) for every quad line. verts: 4 lists (x y z u v [nx ny nz])."""
        group = "-"
        for i, ln in enumerate(self.lines):
            v = ln.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "g":
                group = v[1] if len(v) > 1 else "-"
            elif v[0] == "q":
                layer = int(v[1])
                verts = [[float(t) for t in v[2 + k * 5:7 + k * 5]] for k in range(4)]
                normal = [float(t) for t in v[22:25]]
                ok = layer in (0, 1, 2) and (not self.vu)
                yield i, "q", layer, verts, normal, ok
            elif v[0] == "qn":
                layer = int(v[1])
                verts = [[float(t) for t in v[2 + k * 8:10 + k * 8]] for k in range(4)]
                ok = (layer in (0, 1, 2, 3) and group == "-") if self.vu else layer in (0, 1, 2)
                yield i, "qn", layer, verts, None, ok

    def body_quads(self):
        qs = []
        for _i, _k, layer, verts, _n, ok in self.records():
            if layer == 0 and ok:
                qs.append((0, [tuple(p[:3]) for p in verts], None))
        return qs


def fmt(v):
    s = "%.3f" % v
    return "0.000" if s == "-0.000" else s


def quad_line(kind, layer, verts, normal):
    toks = [kind, str(layer)]
    for p in verts:
        toks += ["%.3f" % t for t in p]
    if kind == "q":
        toks += [fmt(t) for t in normal]
    return " ".join(toks)


# ================================================================ measuring
def blade_stats(quads, scale):
    """Per-slice measures of the body quads (mesh units): the Measures of weapon_orientation plus the median blade section."""
    m = wo.measure(quads, scale)
    if m.empty:
        return None
    return m


def section(m, scale):
    """(width, depth) in blocks of the blade section: the median of the slices at BLADE_FROM..BLADE_TO of the length."""
    lo, hi = int(BLADE_FROM * BINS), int(math.ceil(BLADE_TO * BINS))
    ws = [m.width[b] for b in range(lo, hi)]
    ds = [m.depth[b] for b in range(lo, hi)]
    return statistics.median(ws), statistics.median(ds)


def display_scale(item_id, key):
    for i in (item_id, wc.base_id(item_id)):
        p = os.path.join(MODELS, i + ".json")
        if os.path.exists(p):
            try:
                s = json.load(open(p, encoding="utf-8")).get("display", {}).get(key, {}).get("scale")
                if s:
                    return float(s[0])
            except (ValueError, OSError):
                pass
    return None


def detect_regions(m, cls):
    """(gs, ge) the guard's first slice and one past its last (indices in BINS), from the width profile; the class default when there is no clear guard."""
    w = m.width
    lo, hi = 3, 18
    peak = max(range(lo, hi), key=lambda b: w[b])
    after = [w[b] for b in range(peak + 2, BINS - 3) if w[b] > 0]
    ref = statistics.median(after) if after else 0.0
    if ref > 0 and w[peak] >= 1.35 * ref:
        gs = peak
        while gs > 0 and w[gs - 1] >= 0.6 * w[peak]:
            gs -= 1
        ge = peak + 1
        while ge < BINS and w[ge] >= 0.6 * w[peak]:
            ge += 1
        return gs, ge
    d = int(round(cls["guard_default"] * BINS))
    return d, d


def plan_mesh(mf, cls, s1, s3, refine=(1.0, 1.0)):
    """The cross-section plan of one mesh file: dict(knots=[(y, kx, kz)...], profile=(xc, zc by slice), k_len, ...) in mesh units; None when nothing is to be done."""
    quads = mf.body_quads()
    if not quads or mf.place is None:
        return None
    ox, oy, ang, sc, ymin = mf.place
    m = blade_stats(quads, sc)
    if m is None:
        return None
    L = m.length_mu
    # ---- length (daggers): scaled about the grip so the apparent third-person length reaches len3p
    k_len = 1.0
    if cls.get("len3p"):
        game_len = (max(p[1] for _l, P, _n in mf_all_main(mf) for p in P) - ymin) * sc
        k_len = max(1.0, min(cls["k_len_max"], cls["len3p"] / (game_len * s3))) if game_len > 0 else 1.0
        if abs(k_len - 1.0) < 0.005:
            k_len = 1.0
    sc2 = sc * k_len                                           # blocks per mesh unit after the length scale
    bw, bd = section(m, sc)                                    # blocks at the OLD scale
    bw_mu, bd_mu = bw / sc, bd / sc
    # targets in blocks (apparent in hand), converted to mesh units of the new scale
    need_w = max(cls["bw_min_1p"] / s1, cls["bw_min_3p"] / s3) / sc2
    need_d = cls["bd_min"] / s3 / sc2                         # the thickness minimum is apparent in third person
    kb_x = min(cls["k_blade_max"], max(1.0, need_w / bw_mu * refine[0])) if bw_mu > 1e-9 else cls["k_blade_max"]
    kb_z = min(cls["k_blade_max"], max(1.0, need_d / bd_mu * refine[1])) if bd_mu > 1e-9 else cls["k_blade_max"]
    gs, ge = detect_regions(m, cls)
    bh = L / BINS
    y0 = m.y0
    ygs, yge = y0 + gs * bh, y0 + ge * bh
    # handle: slices 1 .. gs-1
    hb = [b for b in range(1, gs) if m.width[b] > 0]
    kh_x = kh_z = 1.0
    if len(hb) >= 2:
        hw = statistics.median([m.width[b] for b in hb]) / sc
        hd = statistics.median([m.depth[b] for b in hb]) / sc
        need_h = cls["handle_min"] / sc2
        kh_x = min(cls["k_handle_max"], max(1.0, need_h / hw)) if hw > 1e-9 else 1.0
        kh_z = min(cls["k_handle_max"], max(1.0, need_h / hd)) if hd > 1e-9 else 1.0
    # guard: slices gs .. ge-1
    gws = [m.width[b] for b in range(gs, max(ge, gs + 1)) if b < BINS]
    gds = [m.depth[b] for b in range(gs, max(ge, gs + 1)) if b < BINS]
    gw_mu = max(gws) / sc if gws else 0.0
    gd_mu = max(gds) / sc if gds else 0.0
    base_w = max([m.width[b] for b in range(ge, min(BINS, ge + 3))] or [0.0]) / sc * kb_x     # the blade's new base width right above the guard
    base_d = max([m.depth[b] for b in range(ge, min(BINS, ge + 3))] or [0.0]) / sc * kb_z
    need_g = cls["guard_min"] / sc2
    kg_x = min(cls["k_guard_max"], max(1.0, need_g / gw_mu, cls["guard_over_blade"] * base_w / gw_mu)) if gw_mu > 1e-9 else 1.0
    kg_z = min(cls["k_guard_max"], max(1.0, need_g / gd_mu if gd_mu > 1e-9 else 1.0, cls["guard_over_blade"] * base_d / gd_mu if gd_mu > 1e-9 else 1.0)) if gd_mu > 1e-9 else 1.0
    kg_z = min(kg_z, cls["k_guard_z_max"])
    if gs == ge:                                               # no guard: the handle runs into the blade over one ramp
        kg_x, kg_z = kh_x, kh_z
    r = cls["ramp"] * L
    knots = [(y0, kh_x, kh_z), (ygs - r, kh_x, kh_z), (ygs, kg_x, kg_z), (yge, kg_x, kg_z), (yge + r, kb_x, kb_z), (m.y1 + 1.0, kb_x, kb_z)]
    # the centre line of every slice (mesh units); empty slices take the nearest one
    cx, cz = [], []
    for b in range(BINS):
        if m.xmax[b] >= m.xmin[b]:
            cx.append((m.xmin[b] + m.xmax[b]) / 2.0)
            cz.append((m.zmin[b] + m.zmax[b]) / 2.0)
        else:
            cx.append(None)
            cz.append(None)
    for arr in (cx, cz):
        last = None
        for b in range(BINS):
            if arr[b] is None:
                arr[b] = last
            else:
                last = arr[b]
        nxt = None
        for b in range(BINS - 1, -1, -1):
            if arr[b] is None:
                arr[b] = nxt
            else:
                nxt = arr[b]
    return {"need": (need_w * sc2, need_d * sc2), "knots": knots, "cx": cx, "cz": cz, "y0": y0, "bh": bh, "k_len": k_len, "sc": sc, "sc2": sc2, "gs": gs, "ge": ge,
            "kb": (kb_x, kb_z), "kh": (kh_x, kh_z), "kg": (kg_x, kg_z), "section": (bw, bd)}


def mf_all_main(mf):
    """Every quad of the static group over every layer (the length is measured over every layer, as WeaponRenderer does)."""
    out = []
    for _i, _k, layer, verts, _n, ok in mf.records():
        if layer in (0, 1, 2) and (ok or layer == 0):
            out.append((layer, [tuple(p[:3]) for p in verts], None))
    return out


def factor_at(plan, y):
    kn = plan["knots"]
    if y <= kn[0][0]:
        return kn[0][1], kn[0][2]
    for a, b in zip(kn, kn[1:]):
        if y <= b[0]:
            t = 0.0 if b[0] - a[0] < 1e-9 else (y - a[0]) / (b[0] - a[0])
            return a[1] + t * (b[1] - a[1]), a[2] + t * (b[2] - a[2])
    return kn[-1][1], kn[-1][2]


def centre_at(plan, y):
    bh, y0 = plan["bh"], plan["y0"]
    f = (y - y0) / bh - 0.5
    b0 = max(0, min(BINS - 1, int(math.floor(f))))
    b1 = min(BINS - 1, b0 + 1)
    t = max(0.0, min(1.0, f - b0))
    return plan["cx"][b0] + t * (plan["cx"][b1] - plan["cx"][b0]), plan["cz"][b0] + t * (plan["cz"][b1] - plan["cz"][b0])


# ================================================================ cutting quads at the knots
def _lerp(a, b, t):
    return [x + t * (y - x) for x, y in zip(a, b)]


def clip_poly(poly, y, keep_ge):
    out = []
    n = len(poly)
    for k in range(n):
        a, b = poly[k], poly[(k + 1) % n]
        ina = a[1] >= y if keep_ge else a[1] <= y
        inb = b[1] >= y if keep_ge else b[1] <= y
        if ina:
            out.append(a)
        if ina != inb and abs(b[1] - a[1]) > 1e-12:
            t = (y - a[1]) / (b[1] - a[1])
            c = _lerp(a, b, t)
            c[1] = y
            out.append(c)
    return out


def to_quads(poly):
    """A convex polygon of 3 to 5 vertices as quads (a triangle repeats its last vertex)."""
    if len(poly) < 3:
        return []
    if len(poly) == 3:
        return [[poly[0], poly[1], poly[2], poly[2]]]
    if len(poly) == 4:
        return [poly]
    return [[poly[0], poly[1], poly[2], poly[3]], [poly[0], poly[3], poly[4], poly[4]]]


def split_quad(verts, ys):
    """Cut a quad at each y of ys it crosses (strictly inside by more than 1e-4); returns a list of quads."""
    polys = [verts]
    for y in ys:
        nxt = []
        for p in polys:
            lo, hi = min(v[1] for v in p), max(v[1] for v in p)
            if lo + 1e-4 < y < hi - 1e-4:
                for keep_ge in (False, True):
                    c = clip_poly(p, y, keep_ge)
                    nxt.extend(to_quads(c))
            else:
                nxt.append(p)
        polys = nxt
    return polys


# ================================================================ producing a thickened file
def thicken(mf, plan, k_len=None):
    """The new lines of the mesh file with the plan applied; the place line carries the length scale and the shifted origin."""
    ys = sorted({kn[0] for kn in plan["knots"][1:-1]})
    out = list(mf.lines)
    repl = {}
    for i, kind, layer, verts, normal, ok in mf.records():
        if not ok:
            continue
        new = []
        for q in split_quad(verts, ys):
            nq = []
            for p in q:
                x, y, z = p[0], p[1], p[2]
                kx, kz = factor_at(plan, y)
                cx, cz = centre_at(plan, y)
                np_ = list(p)
                np_[0] = cx + kx * (x - cx)
                np_[2] = cz + kz * (z - cz)
                nq.append(np_)
            new.append(quad_line(kind, layer, nq, normal))
        repl[i] = new
    res = []
    for i, ln in enumerate(out):
        if i in repl:
            res.extend(repl[i])
        elif ln.startswith("place "):
            ox, oy, ang, sc, ymin = mf.place
            k = plan["k_len"]
            if k != 1.0:
                grip = plan["grip"]
                length = (plan["ymax_all"] - ymin) * sc
                a = math.radians(ang)
                ox += math.cos(a) * grip * length * (1 - k)
                oy += math.sin(a) * grip * length * (1 - k)
                sc *= k
            res.append("place %.5f %.5f %.3f %.6f %.3f" % (ox, oy, ang, sc, ymin))
        else:
            res.append(ln)
    return res


# ================================================================ the set of weapons
EXCLUDED = tuple(json.load(open(RULES_JSON, encoding="utf-8")).get("excluded", []))      # gauntlets: wide by design, not blades


def class_ids():
    data = json.load(open(os.path.join(ASSETS, "weapon_classes.json"), encoding="utf-8"))
    ids = {}
    for d, _dst, tag in DIRS:
        for p in sorted(glob.glob(os.path.join(ASSETS, d, "*.txt"))):
            i = os.path.basename(p)[:-4]
            base = wc.base_id(i)
            for suf in ("_lod", "_orbit"):
                if base.endswith(suf):
                    base = base[:-len(suf)]
            cls = wc.class_of(base, data)
            if cls in ("dagger", "katana") and base not in EXCLUDED:
                ids.setdefault(base, []).append((tag, i, p))
    return ids, data


def rel_dst(tag, name):
    return {"old": "weapon_meshes_sz", "up": "weapon_meshes_sz/up", "vu": "weapon_meshes_vu_sz"}[tag] + "/" + name + ".txt"


def first_person_scale(item_id, cls_rule, lengths_blocks):
    """The first-person display scale of a dagger: len1p over the longest variant, between the vanilla 0.68 and the `fp_scale_max`."""
    if not cls_rule.get("len1p"):
        return None
    s = cls_rule["len1p"] / max(lengths_blocks)
    return round(max(dp.BASE_SCALE, min(cls_rule["fp_scale_max"], s)), 3)


def measure_lines(lines):
    """(blade width, blade depth) in blocks, the apparent-in-mesh section of thickened `lines` (None when it has no body)."""
    mf = MeshFile("", lines)
    quads = mf.body_quads()
    if not quads or mf.place is None:
        return None
    m = wo.measure(quads, mf.place[3])
    return section(m, mf.place[3]) if not m.empty else None


def build(ids_filter=None, apply=False, quiet=False):
    """Compute (and with apply: write) every thickened mesh and dagger json. Returns (rows, changed_paths)."""
    R = rules()
    ids, data = class_ids()
    rows, changed = [], []
    for base, files in sorted(ids.items()):
        if ids_filter and base not in ids_filter:
            continue
        cname = wc.class_of(base, data)
        cls = R["classes"][cname]
        s3 = display_scale(base, "thirdperson_righthand") or 0.85
        s1_old = display_scale(base, "firstperson_righthand") or dp.BASE_SCALE
        # pass 1: the length each variant gets, so the dagger's first-person scale suits the longest
        main = {tag: (name, p) for tag, name, p in files if not name.endswith("_lod")}
        plans = {}
        lens = []
        for tag, name, p in files:
            if name.endswith("_lod") or name.endswith("_orbit"):
                continue
            mf = MeshFile(p)
            allq = mf_all_main(mf)
            ymax_all = max(pt[1] for _l, P, _n in allq for pt in P)
            plans[tag] = (name, p, mf, ymax_all)
        # first-person scale: a dagger gets the rule's scale, a katana keeps its json (the camera bound of display_presets decides it)
        lengths = []
        for tag, (name, p, mf, ymax_all) in plans.items():
            sc = mf.place[3]
            game_len = (ymax_all - mf.place[4]) * sc
            k = max(1.0, min(cls.get("k_len_max", 1.0), cls["len3p"] / (game_len * s3))) if cls.get("len3p") else 1.0
            lengths.append(game_len * k)
        s1 = first_person_scale(base, cls, lengths) or s1_old
        for tag, (name, p, mf, ymax_all) in plans.items():
            plan = plan_mesh(mf, cls, s1, s3)
            if plan is None:
                continue
            plan["grip"] = cls["grip"]
            plan["ymax_all"] = ymax_all
            lines = thicken(mf, plan)
            refine = (1.0, 1.0)
            for _ in range(4):                                   # the slice extents of a curved or tapered blade do not scale linearly: correct until the section reaches the minimum
                got = measure_lines(lines)
                need_w, need_d = plan["need"]
                fx = need_w / got[0] if got and got[0] > 1e-9 else 1.0
                fz = need_d / got[1] if got and got[1] > 1e-9 else 1.0
                if fx <= 1.002 and fz <= 1.002:
                    break
                refine = (refine[0] * max(1.0, fx) * 1.002, refine[1] * max(1.0, fz) * 1.002)
                plan = plan_mesh(mf, cls, s1, s3, refine)
                plan["grip"] = cls["grip"]
                plan["ymax_all"] = ymax_all
                lines = thicken(mf, plan)
            dst_rel = rel_dst(tag, name)
            dst = os.path.join(ASSETS, dst_rel)
            files_out = [(dst, lines)]
            lod = [f for f in files if f[0] == tag and f[1] == name + "_lod"]
            if lod:                                              # the level-of-detail mesh follows the same plan
                lmf = MeshFile(lod[0][2])
                lplan = dict(plan)
                lplan["grip"], lplan["ymax_all"] = cls["grip"], plan["ymax_all"]                 # the same place line as the full mesh
                files_out.append((os.path.join(ASSETS, rel_dst(tag, name + "_lod")), thicken(lmf, lplan)))
            for path, ls in files_out:
                text = "\n".join(["# nusmp_weapon_sizes (1.110.0): thickened copy of the old mesh by tools/relics/weapon_sizes.py; the old file is unchanged"] + ls) + "\n"
                old = open(path, encoding="utf-8").read() if os.path.exists(path) else None
                if old != text:
                    changed.append(os.path.relpath(path, ROOT))
                    if apply:
                        os.makedirs(os.path.dirname(path), exist_ok=True)
                        with open(path, "w", encoding="utf-8", newline="\n") as f:
                            f.write(text)
            rows.append({"id": base, "class": cname, "variant": tag, "s1": s1, "s3": s3, "k_len": plan["k_len"], "kb": plan["kb"], "kh": plan["kh"], "kg": plan["kg"]})
        # the model json of a dagger: first-person scale and translation, the grip held where an ordinary sword's hilt is
        if cls.get("len1p") and "old" in plans:
            mjson = os.path.join(MODELS, base + ".json")
            if os.path.exists(mjson):
                raw = open(mjson, encoding="utf-8").read()
                model = json.loads(raw)
                if model.get(dp.MARKER) != "manual" and isinstance(model.get("display"), dict):
                    sz = os.path.join(ASSETS, rel_dst("old", plans["old"][0]))
                    mesh = dp.read_mesh(sz) if os.path.exists(sz) else dp.read_mesh(plans["old"][1])
                    if mesh is not None:
                        new = json.loads(raw)
                        for key in dp.FIRST:
                            rot = dp._rotation_of(new["display"], key)
                            t = dp.translation_for(mesh, dp.BASE_ROTATION["firstperson_righthand"], s1)
                            new["display"][key] = {"rotation": list(rot), "translation": [round(x, 2) for x in t], "scale": [s1, s1, s1]}
                        new["nusmp_weapon_sizes"] = {"first_person_scale": s1, "rule": "tools/relics/weapon_sizes.py"}
                        text = json.dumps(new, indent=2)
                        if text != raw:
                            changed.append(os.path.relpath(mjson, ROOT))
                            if apply:
                                with open(mjson, "w", encoding="utf-8", newline="\n") as f:
                                    f.write(text)
    return rows, changed


# ================================================================ first-person camera check
def first_person_camera(item_id, mesh_path):
    """Camera-space (x right, y up, -z forward) of the mesh box's eight corners and its tip under the model json's first-person display of the right hand:
    c = H + T + R * (s * (p - P)) (display_presets doc). Returns (nearest depth of any corner, tip x/depth, tip y/depth)."""
    d = json.load(open(os.path.join(MODELS, item_id + ".json"), encoding="utf-8"))["display"]["firstperson_righthand"]
    mesh = dp.read_mesh(mesh_path)
    r = dp.rot_matrix(*d["rotation"])
    s = d["scale"][0]
    t = dp.scale(tuple(d["translation"]), 1 / 16.0)
    P = (0.5, 0.5, 0.5)

    def cam(q):
        return dp.add(dp.add(dp.HAND, t), dp.mat_vec(r, dp.scale(dp.sub(q, P), s)))
    depth = min(-cam(q)[2] for q in mesh.corners())
    tip = cam(mesh.tip())
    td = -tip[2]
    return depth, (tip[0] / td if td > 1e-9 else 99.0), (tip[1] / td if td > 1e-9 else 99.0)


# ================================================================ the table
BASELINE = os.path.join(ROOT, "docs", "visual_upgrade", "weapon_sizes_before.json")
COMPARE = ("dagger", "katana", "shortsword", "sabre", "rapier", "longsword", "greatsword")


def measure_file(path):
    mf = MeshFile(path)
    quads = mf.body_quads()
    if not quads or mf.place is None:
        return None
    sc = mf.place[3]
    m = wo.measure(quads, sc)
    if m.empty:
        return None
    allq = mf_all_main(mf)
    ymax_all = max(pt[1] for _l, P, _n in allq for pt in P)
    bw, bd = section(m, sc)
    return {"len": round((ymax_all - mf.place[4]) * sc, 4), "bw": round(bw, 4), "bd": round(bd, 4)}


def all_variants(classes):
    """{(base id, variant, file name): path} of every mesh of the classes (state meshes of a base id are their own rows)."""
    data = json.load(open(os.path.join(ASSETS, "weapon_classes.json"), encoding="utf-8"))
    out = {}
    for d, _dst, tag in DIRS:
        for p in sorted(glob.glob(os.path.join(ASSETS, d, "*.txt"))):
            name = os.path.basename(p)[:-4]
            if name.endswith(("_lod", "_orbit")) or name.startswith("djinn_weapon_"):
                continue
            base = wc.base_id(name)
            if wc.class_of(base, data) in classes:
                out[(base, tag, name)] = p
    return out, data


def write_baseline():
    """The BEFORE numbers (old meshes and model jsons as they are at 1.109.0): run once, before --apply."""
    files, data = all_variants(COMPARE)
    rows = []
    for (base, tag, name), p in sorted(files.items()):
        m = measure_file(p)
        if m:
            rows.append({"id": base, "variant": tag, "mesh": name, "class": wc.class_of(base, data), "s1": display_scale(base, "firstperson_righthand") or dp.BASE_SCALE,
                         "s3": display_scale(base, "thirdperson_righthand") or 0.85, **m})
    os.makedirs(os.path.dirname(BASELINE), exist_ok=True)
    with open(BASELINE, "w", encoding="utf-8", newline="\n") as f:
        json.dump({"note": "1.109.0 measures of every dagger, katana and comparison sword: blocks, mesh length / blade width (median of 45-70 percent of the length) / blade thickness", "rows": rows}, f, indent=1)
        f.write("\n")
    return rows


def table_rows(ids_filter=None):
    base = {(r["id"], r["variant"], r["mesh"]): r for r in json.load(open(BASELINE, encoding="utf-8"))["rows"]}
    files, data = all_variants(COMPARE)
    rows = []
    for key, b in sorted(base.items()):
        if ids_filter and key[0] not in ids_filter:
            continue
        szp = os.path.join(ASSETS, rel_dst(key[1], key[2]))
        a = measure_file(szp) if os.path.exists(szp) else measure_file(files[key])
        s1a = display_scale(key[0], "firstperson_righthand") or dp.BASE_SCALE
        s3a = display_scale(key[0], "thirdperson_righthand") or 0.85
        rows.append({"id": key[0], "variant": key[1], "class": b["class"], "changed": os.path.exists(szp),
                     "b": {"L3": b["len"] * b["s3"], "L1": b["len"] * b["s1"], "w1": b["bw"] * b["s1"], "w3": b["bw"] * b["s3"], "d3": b["bd"] * b["s3"], "s1": b["s1"]},
                     "a": {"L3": a["len"] * s3a, "L1": a["len"] * s1a, "w1": a["bw"] * s1a, "w3": a["bw"] * s3a, "d3": a["bd"] * s3a, "s1": s1a}})
    return rows


def print_table(rows):
    print("%-30s %-10s %-3s | %5s %5s | %5s %5s | %6s %6s | %6s %6s | %5s %5s" % ("id", "class", "var", "L3b", "L3a", "L1b", "L1a", "w1b", "w1a", "w3b", "w3a", "d3b", "d3a"))
    for r in rows:
        b, a = r["b"], r["a"]
        print("%-30s %-10s %-3s | %5.2f %5.2f | %5.2f %5.2f | %6.3f %6.3f | %6.3f %6.3f | %5.3f %5.3f%s" % (r["id"], r["class"], r["variant"], b["L3"], a["L3"], b["L1"], a["L1"],
              b["w1"], a["w1"], b["w3"], a["w3"], b["d3"], a["d3"], "" if r["changed"] else "  (unchanged)"))


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--table", action="store_true")
    ap.add_argument("--baseline", action="store_true", help="write the BEFORE numbers (once, before --apply)")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--ids")
    args = ap.parse_args(argv)
    flt = set(args.ids.split(",")) if args.ids else None
    if args.baseline:
        print("%d rows" % len(write_baseline()))
        return 0
    if args.apply or args.check:
        rows, changed = build(flt, apply=args.apply)
        print("%d meshes planned; %d files %s" % (len(rows), len(changed), "written" if args.apply else "would change"))
        for r in rows:
            print("  %-30s %-3s s1=%.3f len x%.2f blade k=(%.2f, %.2f) handle k=(%.2f, %.2f) guard k=(%.2f, %.2f)" % (r["id"], r["variant"], r["s1"], r["k_len"], r["kb"][0], r["kb"][1], r["kh"][0], r["kh"][1], r["kg"][0], r["kg"][1]))
        if args.check and changed:
            print("would change:", ", ".join(changed[:12]))
            return 1
        return 0
    print_table(table_rows(flt))
    return 0


if __name__ == "__main__":
    sys.exit(main())

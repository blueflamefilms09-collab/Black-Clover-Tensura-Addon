"""apply_weapon_fixes: the per-id size and orientation corrections of the baked weapon meshes (docs/weapon_scale_orientation.md).

    python tools/relics/apply_weapon_fixes.py             dry run: what each fix would change (apparent length before -> after)
    python tools/relics/apply_weapon_fixes.py --apply     rewrite assets/nusmp/weapon_meshes/<id>.txt
    python tools/relics/apply_weapon_fixes.py --check     exit 1 if --apply would change anything (CI)
    python tools/relics/apply_weapon_fixes.py --suggest   print a proposed tools/relics/weapon_fixes.json for every weapon below its class minimum,
                                                          every head too small and every head on the wrong side (--write merges the missing ids in)

tools/relics/weapon_fixes.json maps an item id (a state mesh uses its base id's entry: bianca_pull1 -> bianca) to
    "apparent"  target length in blocks as seen in the third-person hand ((ymax - ymin) * scale * the model json's thirdperson scale)
    "extent"    the same for a held item: the largest dimension of the mesh
    "scale"     a plain multiplier of the place scale (instead of apparent / extent)
    "grip"      share of the length (from the butt) that stays where it is when the weapon grows (default: the class's `grip`)
    "flip_y"    true (1.34.0): the mesh was built with its HILT at the far (high y) end, so the hand holds the blade (the Windcutter of screenshot 68):
                a half turn about the x axis through the middle of the length (y -> -y, z -> -z; every x stays, so the edge keeps its side), and the
                directive `grip 0.12` is written so the runtime normaliser knows the grip is verified
    "flip_x"    true: mirror the mesh across x (edge / head to the other side) relative to what the generator wrote
    "rotate_z"  degrees: turn the mesh about z through (0, middle of its length) (180 = a tip-down mesh tip-up)
    "head"      {"k": 1.5, "ky": .., "kz": .., "cut": <mesh y, default: where the head starts>, "band": 0.06}: the head (everything above `cut`) is
                grown across by k (along by ky = 1 + 0.6 (k - 1), through by kz = 1 + 0.4 (k - 1) unless given) about the haft axis, easing in
                over `band` of the length: an axe head that is too small is made bold without touching the haft
    "orient_manual"  true: the mesh is hand-tuned (`orient manual`); the runtime normaliser never touches it
    "note"      why
The fixes are applied to the baked text, reversibly: the rewritten file ends with a `# nusmp_fix {...}` line that records the request and the
original place line, so a changed request first undoes the old one. Running the tool twice does nothing the second time. mesh_lib's
Mesh.write_txt calls apply_file after writing, so a regenerated mesh gets its fixes again.
"""
import argparse
import glob
import json
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import weapon_classes as wc          # noqa: E402
import weapon_orientation as wo      # noqa: E402

ROOT = wc.ROOT
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")
MESHES = os.path.join(ASSETS, "weapon_meshes")
FIXES_JSON = os.path.join(HERE, "weapon_fixes.json")
MARK = "# nusmp_fix "
DEFAULT_BAND = 0.06
GRIP_VERIFIED = 0.12                 # the `grip` directive written to a mesh whose hilt end was checked (share of the length from the butt)
SKIP_PREFIXES = ("djinn_weapon_",)   # 1.34.0: the Djinn temporary weapons belong to another agent / release


# ================================================================ tokens <-> quads
def _stride(tok):
    return 5 if tok[0] == "q" else 8


def _fmt(v):
    s = "%.3f" % v
    return "0.000" if s == "-0.000" else s


def _fmtn(v, digits):
    s = ("%." + str(digits) + "f") % v
    return ("%." + str(digits) + "f") % 0.0 if s.startswith("-") and float(s) == 0.0 else s


def parse_lines(text):
    """-> (lines, quads): lines as written, quads = {line index: token list} for every q / qn line."""
    lines = text.split("\n")
    if lines and lines[-1] == "":
        lines.pop()
    quads = {}
    for i, ln in enumerate(lines):
        t = ln.split()
        if t and t[0] in ("q", "qn"):
            quads[i] = t
    return lines, quads


def to_quads(quads):
    """Token lists -> weapon_orientation's (layer, [P*4], [N*4]) quads (in line order)."""
    out = []
    for i in sorted(quads):
        t = quads[i]
        st = _stride(t)
        P = [(float(t[2 + k * st]), float(t[3 + k * st]), float(t[4 + k * st])) for k in range(4)]
        if t[0] == "q":
            N = [(float(t[22]), float(t[23]), float(t[24]))] * 4
        else:
            N = [(float(t[7 + k * 8]), float(t[8 + k * 8]), float(t[9 + k * 8])) for k in range(4)]
        out.append((int(t[1]), P, N))
    return out


def _map_vertices(t, fp, fn=None, order=None):
    """A new token list for the quad `t`: positions through fp((x, y, z)) -> (x, y, z), normals through fn((nx, ny, nz)), vertices reordered."""
    st = _stride(t)
    verts = []
    for k in range(4):
        o = 2 + k * st
        p = fp((float(t[o]), float(t[o + 1]), float(t[o + 2])))
        verts.append([_fmt(p[0]), _fmt(p[1]), _fmt(p[2]), t[o + 3], t[o + 4]] + ([] if st == 5 else list(t[o + 5:o + 8])))
    if t[0] == "qn":
        for k in range(4):
            if fn:
                n = fn((float(verts[k][5]), float(verts[k][6]), float(verts[k][7])))
                verts[k][5:8] = [_fmtn(n[0], 4), _fmtn(n[1], 4), _fmtn(n[2], 4)]
    tail = []
    if t[0] == "q":
        n = (float(t[22]), float(t[23]), float(t[24]))
        if fn:
            n = fn(n)
            tail = [_fmtn(n[0], 3), _fmtn(n[1], 3), _fmtn(n[2], 3)]
        else:
            tail = list(t[22:25])
    order = order or (0, 1, 2, 3)
    flat = [x for k in order for x in verts[k]]
    return [t[0], t[1]] + flat + tail


# ================================================================ the operations (each has an exact inverse)
def op_flip_x(quads, px=0.0):
    """Mirrors across the plane x = px (the haft axis): x -> 2 px - x, normals' x negated, quad winding reversed (v0 v3 v2 v1)."""
    for i, t in quads.items():
        quads[i] = _map_vertices(t, lambda p: (2 * px - p[0], p[1], p[2]), lambda n: (-n[0], n[1], n[2]), order=(0, 3, 2, 1))


def op_flip_y(quads, pivot_y):
    """The grip end swap (an involution): a half turn about the x axis through (., pivot_y, 0): y -> 2 pivot_y - y, z -> -z, normals' y and z negated.
    A proper rotation, so the winding stays and no x changes (the edge keeps its side)."""
    for i, t in quads.items():
        quads[i] = _map_vertices(t, lambda p: (p[0], 2 * pivot_y - p[1], -p[2]), lambda n: (n[0], -n[1], -n[2]))


def op_rotate_z(quads, deg, pivot_y):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    for i, t in quads.items():
        quads[i] = _map_vertices(t, lambda p: (p[0] * c - (p[1] - pivot_y) * s, p[0] * s + (p[1] - pivot_y) * c + pivot_y, p[2]),
                                 lambda n: (n[0] * c - n[1] * s, n[0] * s + n[1] * c, n[2]))


def _smooth(t):
    t = min(1.0, max(0.0, t))
    return t * t * (3.0 - 2.0 * t)


def head_ks(h):
    """(kx, ky, kz) of a head request: `k` across, `ky` along (default 1 + 0.6 (k - 1)), `kz` through (default 1 + 0.4 (k - 1))."""
    k = float(h["k"])
    return k, float(h.get("ky", 1.0 + 0.6 * (k - 1.0))), float(h.get("kz", 1.0 + 0.4 * (k - 1.0)))


def op_head(quads, h, inverse=False):
    """Grows everything above h['cut']: y about the cut by ky, x about h['px'] by kx, z about 0 by kz; the sideways growth eases in over h['band_mu'].
    h = {"k", "ky", "kz", "cut", "px", "band_mu"} (all resolved). inverse=True undoes it exactly."""
    kx, ky, kz = head_ks(h)
    cut, px, band = h["cut"], h["px"], h["band_mu"]

    def eased(y):
        e = _smooth((y - cut) / band)
        return 1.0 + (kx - 1.0) * e, 1.0 + (kz - 1.0) * e
    if inverse:
        def fp(p):
            if p[1] <= cut:
                return p
            y = cut + (p[1] - cut) / ky
            fx, fz = eased(y)
            return (px + (p[0] - px) / fx, y, p[2] / fz)
    else:
        def fp(p):
            if p[1] <= cut:
                return p
            fx, fz = eased(p[1])
            return (px + (p[0] - px) * fx, cut + (p[1] - cut) * ky, p[2] * fz)
    for i, t in quads.items():
        quads[i] = _map_vertices(t, fp)


# ================================================================ one file
def s3p_of(item_id, models_dir=None):
    import weapon_sheet
    return weapon_sheet.display_of(item_id, models_dir)[2][0]


def place_of(lines):
    for i, ln in enumerate(lines):
        t = ln.split()
        if t and t[0] == "place":
            return i, [float(x) for x in t[1:6]]
    return None, None


def _place_line(p):
    return "place %.5f %.5f %.3f %.6f %.3f" % tuple(p)


def game_length(quads, place):
    """The length the game uses (WeaponRenderer.mesh: (max y over every layer - the place ymin) * the place scale), in blocks."""
    ymax = max(float(t[2 + k * _stride(t) + 1]) for t in quads.values() for k in range(4))
    return (ymax - place[4]) * place[3]


def extent_of(quads, place):
    xs = [float(t[2 + k * _stride(t)]) for t in quads.values() if t[1] == "0" for k in range(4)]
    zs = [float(t[2 + k * _stride(t) + 2]) for t in quads.values() if t[1] == "0" for k in range(4)]
    return max(game_length(quads, place), (max(xs) - min(xs)) * place[3], (max(zs) - min(zs)) * place[3])


def _reindex(lines, quads):
    """Rebuilds the text from the (line index -> tokens) map after operations (quad lines are replaced by their tokens) and parses it again."""
    out = list(lines)
    for i, t in quads.items():
        out[i] = " ".join(t)
    return parse_lines("\n".join(out) + "\n")


def undo_marker(lines, quads):
    """Undoes the recorded fix: returns (marker or None, lines, quads) of the mesh as the generator wrote it."""
    mi = next((i for i, ln in enumerate(lines) if ln.startswith(MARK)), None)
    if mi is None:
        return None, lines, quads
    mk = json.loads(lines[mi][len(MARK):])
    lines, quads = parse_lines("\n".join(lines[:mi] + lines[mi + 1:]) + "\n")
    if mk.get("head"):
        op_head(quads, mk["head"], inverse=True)
    if mk.get("rotate_z"):
        op_rotate_z(quads, -mk["rotate_z"], mk["rotate_pivot"])
    if mk.get("flip_x"):
        op_flip_x(quads, mk.get("flip_px", 0.0))
    if mk.get("flip_y"):
        op_flip_y(quads, mk["flip_y_pivot"])
    lines, quads = _reindex(lines, quads)
    pi, _ = place_of(lines)
    if pi is not None and mk.get("orig_place"):
        lines[pi] = mk["orig_place"]
    if mk.get("orient_manual"):
        lines = [ln for ln in lines if ln.strip() != "orient manual"]
    if mk.get("grip_line"):
        if mk.get("grip_prev"):                                  # the generator's own `grip` line was rewritten: put it back
            lines = [mk["grip_prev"] if ln.strip() == mk["grip_line"] else ln for ln in lines]
        else:
            lines = [ln for ln in lines if ln.strip() != mk["grip_line"]]
    lines, quads = parse_lines("\n".join(lines) + "\n")
    return mk, lines, quads


def head_cut(m, head_bins):
    """Where the head starts: one slice below the lowest slice of the topmost run of head slices (gaps of one slice allowed)."""
    lo = head_bins[-1]
    for b in reversed(head_bins[:-1]):
        if lo - b <= 2:
            lo = b
        else:
            break
    return m.y0 + max(lo - 1, 0) * m.length_mu / wo.BINS


def apply_to_text(text, item_id, fix, data=None, models_dir=None):
    """The mesh text with `fix` applied (after undoing any earlier recorded fix). Returns (new_text, report dict)."""
    data = data or wc.load()
    lines, quads = parse_lines(text if text.endswith("\n") else text + "\n")
    prev_marker = next((ln for ln in lines if ln.startswith(MARK)), None)
    if prev_marker is not None:
        mk0 = json.loads(prev_marker[len(MARK):])
        if mk0.get("req") == fix:
            return text, {"changed": False, "reason": "up to date"}
    mk, lines, quads = undo_marker(lines, quads)
    pi, place = place_of(lines)
    report = {"changed": False, "before": None, "after": None}
    if pi is None or not quads:
        return text, {"changed": False, "reason": "no place line or no quads"}
    s3p = s3p_of(item_id, models_dir)
    cls = wc.class_of(item_id, data)
    tab = wc.table(data)[cls]
    report["before"] = round(game_length(quads, place) * s3p, 3)
    marker = {"req": fix, "orig_place": lines[pi]}
    base_text = "\n".join(_reindex(lines, quads)[0])
    if not fix:
        return base_text + "\n", {"changed": base_text + "\n" != text, "before": report["before"], "after": report["before"]}
    if fix.get("flip_y"):
        ys = [float(t[2 + k * _stride(t) + 1]) for t in quads.values() for k in range(4)]
        pivot = (min(ys) + max(ys)) / 2.0
        op_flip_y(quads, pivot)
        marker["flip_y"], marker["flip_y_pivot"] = True, pivot
    if fix.get("flip_x"):
        px = round(wo.measure(to_quads(quads), place[3]).axis_x, 3)
        op_flip_x(quads, px)
        marker["flip_x"], marker["flip_px"] = True, px
    if fix.get("rotate_z"):
        ys = [float(t[2 + k * _stride(t) + 1]) for t in quads.values() for k in range(4)]
        pivot = (min(ys) + max(ys)) / 2.0
        op_rotate_z(quads, float(fix["rotate_z"]), pivot)
        marker["rotate_z"], marker["rotate_pivot"] = float(fix["rotate_z"]), pivot
    if fix.get("head"):
        h = dict(fix["head"])
        m = wo.measure(to_quads(quads), place[3])
        if "cut" in h:
            cut = float(h["cut"])
        elif m.head_bins:
            cut = head_cut(m, m.head_bins)
        else:
            cut = m.y0 + 0.6 * m.length_mu
        head = {"k": float(h["k"]), "ky": head_ks(h)[1], "kz": head_ks(h)[2], "cut": round(cut, 3), "px": round(m.axis_x, 3), "band_mu": round(float(h.get("band", DEFAULT_BAND)) * m.length_mu, 3)}
        op_head(quads, head)
        marker["head"] = head
    k = 1.0
    if fix.get("scale"):
        k = float(fix["scale"])
    elif fix.get("apparent") or fix.get("extent"):
        if fix.get("apparent"):
            cur = game_length(quads, place) * s3p
            k = float(fix["apparent"]) / cur if cur > 0 else 1.0
        else:
            cur = extent_of(quads, place) * s3p
            k = float(fix["extent"]) / cur if cur > 0 else 1.0
    if abs(k - 1.0) > 1e-6:
        grip = float(fix.get("grip", tab["grip"]))
        ox, oy, ang, sc, ymin = place
        Lc = game_length(quads, place)
        a = math.radians(ang)
        ox2 = ox + math.cos(a) * grip * Lc * (1.0 - k)
        oy2 = oy + math.sin(a) * grip * Lc * (1.0 - k)
        place = [ox2, oy2, ang, sc * k, ymin]
        marker["scale_k"] = round(k, 5)
    lines, quads = _reindex(lines, quads)
    lines[pi] = _place_line(place) if marker.get("scale_k") else lines[pi]
    if fix.get("orient_manual"):
        lines.insert(pi + 1, "orient manual")
        marker["orient_manual"] = True
    if fix.get("grip_line") or fix.get("flip_y"):                  # 1.34.0: the mesh states where its grip is (verified): runtime normaliser trusts it
        gl = "grip %.3f" % float(fix.get("grip_at", GRIP_VERIFIED))
        prev = next((i for i, ln in enumerate(lines) if ln.split()[:1] == ["grip"]), None)
        if prev is not None:                                      # a generator that states its own grip (mesh_lib derives it from the part names): replaced, restored on undo
            marker["grip_prev"] = lines[prev]
            lines[prev] = gl
        else:
            lines.insert(pi + 1, gl)
        marker["grip_line"] = gl
    lines.append(MARK + json.dumps(marker, separators=(",", ":"), sort_keys=True))
    new = "\n".join(lines) + "\n"
    _l, q2 = parse_lines(new)
    report["after"] = round(game_length(q2, place) * s3p, 3)
    report["changed"] = new != text
    return new, report


def apply_file(path, item_id=None, fixes=None, data=None, write=True):
    """Applies tools/relics/weapon_fixes.json to one mesh file; used by mesh_lib.Mesh.write_txt after a generator writes the mesh. Returns the report."""
    item_id = item_id or os.path.basename(path)[:-4]
    if item_id.startswith(SKIP_PREFIXES):
        return {"changed": False, "reason": "skipped (another agent's mesh)"}
    fixes = fixes if fixes is not None else load_fixes()
    fix = fixes.get(wc.base_id(item_id))
    if fix is None:
        return {"changed": False, "reason": "no fix"}
    with open(path, encoding="utf-8") as f:
        text = f.read()
    new, rep = apply_to_text(text, item_id, fix, data)
    if rep.get("changed") and write:
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write(new)
    return rep


def load_fixes(path=FIXES_JSON):
    if not os.path.exists(path):
        return {}
    return {k: v for k, v in json.load(open(path, encoding="utf-8")).items() if not k.startswith("_")}


# ================================================================ suggestions
TARGET_FRACTION = 0.35               # a weapon below its class range is raised to this share of the range (min + 0.35 * (max - min))
HEAD_TARGET_FACTOR = 1.12            # a head below its class minimum is grown to this multiple of the minimum


def class_target(cls, tab):
    return round(tab["min"] + TARGET_FRACTION * (tab["max"] - tab["min"]), 3)


def head_apparent(text, item_id, fix, data):
    """The widest part (the head) in blocks as seen in the hand after `fix` is applied to the mesh text."""
    new, _rep = apply_to_text(text, item_id, fix, data)
    _l, q = parse_lines(new)
    pi, place = place_of(_l)
    m = wo.measure(to_quads(q), place[3])
    return m.max_width * s3p_of(item_id)


def solve_head_k(text, item_id, base_fix, target_head, data):
    """The head scale k at which the head is `target_head` blocks wide in the hand (bisection; the apparent length target is kept)."""
    lo, hi = 1.0, 3.2
    if head_apparent(text, item_id, dict(base_fix, head={"k": hi}), data) < target_head:
        return hi
    for _ in range(14):
        mid = (lo + hi) / 2
        if head_apparent(text, item_id, dict(base_fix, head={"k": mid}), data) < target_head:
            lo = mid
        else:
            hi = mid
    return round(hi, 2)


def suggest(data=None, meshes_dir=MESHES):
    """A proposal: a non-item weapon below its class minimum -> apparent = class_target; a head below its class minimum -> a head k (bisection);
    a head on the +x side (a `handed` class) -> flip_x; a tip-down mesh -> rotate_z 180; a held item below the item minimum -> extent."""
    import audit_weapon_scale as aud
    data = data or wc.load()
    out = {}
    for r in aud.audit_all(data, meshes_dir):
        if wc.base_id(r["id"]) != r["id"]:
            continue
        tab = wc.table(data)[r["class"]]
        fix = {}
        if wo.FLAG_GRIP_FAR_END in r["flags"]:
            fix["flip_y"] = True
        if wo.FLAG_HEAD_TOWARD_PLAYER in r["flags"]:
            fix["flip_x"] = True
        if wo.FLAG_TIP_DOWN in r["flags"]:
            fix["rotate_z"] = 180
        if "small" in r["status"]:
            if wc.is_item_class(r["class"]):
                fix["extent"] = round(data.get("item_min_extent", wc.ITEM_MIN_EXTENT) * 1.08, 3)
            else:
                fix["apparent"] = class_target(r["class"], tab)
        elif "head_small" in r["status"]:
            fix["apparent"] = round(r["apparent"], 3)                  # the head grows, the length the weapon has stays
        if "head_small" in r["status"]:
            text = open(os.path.join(meshes_dir, r["id"] + ".txt"), encoding="utf-8").read()
            k = solve_head_k(text, r["id"], dict(fix), tab["head_min"] * HEAD_TARGET_FACTOR, data)
            fix["head"] = {"k": k}
        if fix:
            fix["note"] = "auto: " + ",".join(r["status"] + r["flags"])
            out[r["id"]] = fix
    return out


def run(apply=False, fixes=None, data=None, meshes_dir=MESHES):
    data = data or wc.load()
    fixes = fixes if fixes is not None else load_fixes()
    rows = []
    changed = []
    for path in sorted(glob.glob(os.path.join(meshes_dir, "*.txt"))):
        item_id = os.path.basename(path)[:-4]
        if item_id.startswith(SKIP_PREFIXES):
            continue
        fix = fixes.get(wc.base_id(item_id))
        if fix is None:
            continue
        with open(path, encoding="utf-8") as fh:
            text = fh.read()
        new, rep = apply_to_text(text, item_id, fix, data)
        rows.append((item_id, rep))
        if rep.get("changed"):
            changed.append(item_id)
            if apply:
                with open(path, "w", encoding="utf-8", newline="\n") as f:
                    f.write(new)
    return rows, changed


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--suggest", action="store_true")
    ap.add_argument("--write", action="store_true")
    a = ap.parse_args(argv)
    if a.suggest:
        sg = suggest()
        print(json.dumps(sg, indent=1))
        if a.write:
            cur = {}
            if os.path.exists(FIXES_JSON):
                cur = json.load(open(FIXES_JSON, encoding="utf-8"))
            for k, v in sg.items():
                cur.setdefault(k, v)
            json.dump(cur, open(FIXES_JSON, "w", encoding="utf-8"), indent=1)
            print("merged into", os.path.relpath(FIXES_JSON, ROOT))
        return 0
    rows, changed = run(apply=a.apply)
    for item_id, rep in rows:
        print("%-34s %s  %s -> %s" % (item_id, "CHANGED" if rep.get("changed") else "ok     ", rep.get("before"), rep.get("after")))
    print("%d meshes with a fix; %d %s" % (len(rows), len(changed), "rewritten" if a.apply else "would change"))
    return 1 if (a.check and changed) else 0


if __name__ == "__main__":
    sys.exit(main())

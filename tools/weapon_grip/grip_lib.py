"""grip_lib: the GRIP POINT of every weapon mesh and where the vanilla hand lands on it (docs/weapon_grip.md).

This is the Python twin of client/relics/item/GripFinder.java and GripMath.java (a golden file, tools/weapon_grip/grip_golden.json, written by
`python tools/weapon_grip/grip_table.py --write`, is checked against the Java in GripTableTest). Same numbers, same order.

Mesh space (assets/nusmp/weapon_meshes/<id>.txt): the butt of the weapon is at the low y end, mesh y runs along the blade; WeaponRenderer places it as
    translate(ox, oy, 0.5); rotateZ(angle - 90); scale(sc); translate(0, -ymin, 0)
into the item's unit cube. The vanilla hand holds one point of that cube: with the display transform  M = T(t/16) * R * S(s) * T(-0.5)  a point p of the
cube lands at  hand + t/16 + R * (s * (p - 0.5)),  so the point that sits exactly at the hand is
    H = 0.5 - (1/s) * R^-1 * (t/16)
(for the left hand vanilla mirrors the entry: x translation, y rotation and z rotation change sign). Before 1.111.0 H was never looked at: the mesh was
put on the old sprite's diagonal, so the hand held whatever part of the weapon happened to lie at H (a quarter to a third of the way up, i.e. the guard
or the blade of a greatsword, the blade of a stretched katana).
"""
import json
import math
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp")

# ---- the finder's named constants (GripFinder.java repeats them)
HANDLE_MODE = {"dagger", "shortsword", "longsword", "rapier", "katana", "sabre", "dadao", "greatsword", "cleaver"}
GUARD_RATIO = 1.8          # a level this many times wider than the handle is the guard
POMMEL_RATIO = 1.3         # a level up to this many times the handle width belongs to the handle
SEARCH_MAX = 0.5           # the handle lies in the lower half of the weapon
MIN_HANDLE = 0.03          # a handle shorter than this share of the length is not trusted (the class fraction decides)
FRAC_MIN, FRAC_MAX = 0.06, 0.5
MIDDLE_HELD = {"item", "handitem", "bow", "crossbow", "shield"}
WINDOW = 0.04
LEVEL_EPS = 1e-3


def load_mesh(path):
    """(place[5], grip directive or None, explicit `gripxyz`, body vertices [(x, y, z)]) of a weapon_meshes file; the body is layer 0 (else layer 2). An upgraded mesh
    (weapon_meshes_vu, `vu 1` header, `g <group>` lines) contributes only its static group "-" when that has body quads (GripFinder works on the same set)."""
    place, directive, xyz, group = None, None, None, "-"
    layers = {0: [], 1: [], 2: []}
    static0 = []
    with open(path, encoding="utf-8") as f:
        for line in f:
            v = line.split()
            if not v or v[0].startswith("#"):
                continue
            if v[0] == "place":
                place = [float(t) for t in v[1:6]]
            elif v[0] == "grip" and len(v) > 1:
                directive = float(v[1])
            elif v[0] == "gripxyz" and len(v) > 3:
                xyz = (float(v[1]), float(v[2]), float(v[3]))
            elif v[0] == "g":
                group = v[1] if len(v) > 1 else "-"
            elif v[0] in ("q", "qn"):
                layer = int(v[1])
                if layer > 2:
                    continue
                st = 5 if v[0] == "q" else 8
                for k in range(4):
                    o = 2 + k * st
                    pt = (float(v[o]), float(v[o + 1]), float(v[o + 2]))
                    layers[layer].append(pt)
                    if layer == 0 and group == "-":
                        static0.append(pt)
    body = static0 or layers[0] or layers[2]
    return place, directive, xyz, body


SAMPLES = 64               # cross-section samples along the length


def slice_widths(body, n=SAMPLES):
    """Width (max of the x extent and the z extent) of the mesh at n evenly spaced heights, 0 where nothing is cut. Cut = the points where the edges of
    the quads cross the height (plus the vertices on it). Returns (y0, y1, [w])."""
    ys = [v[1] for v in body]
    y0, y1 = min(ys), max(ys)
    length = y1 - y0
    if length <= 0:
        return y0, y1, [0.0] * n
    lo = [[1e30, 1e30] for _ in range(n)]
    hi = [[-1e30, -1e30] for _ in range(n)]
    for q in range(0, len(body) - 3, 4):
        quad = body[q:q + 4]
        for e in range(4):
            a, b = quad[e], quad[(e + 1) % 4]
            ya, yb = a[1], b[1]
            i0 = int(math.floor(((min(ya, yb) - y0) / length) * n - 0.5)) + 1
            i1 = int(math.floor(((max(ya, yb) - y0) / length) * n - 0.5))
            for i in range(max(0, i0), min(n - 1, i1) + 1):
                sy = y0 + (i + 0.5) * length / n
                if yb == ya:
                    pts = (a, b)
                else:
                    t = (sy - ya) / (yb - ya)
                    pts = ((a[0] + (b[0] - a[0]) * t, sy, a[2] + (b[2] - a[2]) * t),)
                for pt in pts:
                    lo[i][0] = min(lo[i][0], pt[0]); hi[i][0] = max(hi[i][0], pt[0])
                    lo[i][1] = min(lo[i][1], pt[2]); hi[i][1] = max(hi[i][1], pt[2])
    w = []
    for i in range(n):
        w.append(0.0 if lo[i][0] > hi[i][0] else max(hi[i][0] - lo[i][0], hi[i][1] - lo[i][1]))
    return y0, y1, w


def handle_range(body):
    """(y_start, y_end) of the handle of a sword-like mesh, in mesh units, or None when no guard follows a handle."""
    y0, y1, w = slice_widths(body)
    n = len(w)
    length = y1 - y0
    if length <= 0:
        return None
    low = sorted(x for i, x in enumerate(w) if (i + 0.5) / n <= 0.40 and x > 1e-6)
    if not low:
        return None
    hw = low[len(low) // 5]                                     # the 20th percentile: the handle, not a neck or a pommel
    start = None
    for i in range(n):
        if (i + 0.5) / n > SEARCH_MAX:
            break
        if start is None:
            if 1e-6 < w[i] <= POMMEL_RATIO * hw:
                start = i
            continue
        if w[i] >= GUARD_RATIO * hw:
            return (y0 + start * length / n, y0 + i * length / n)
    return None


def grip_point(body, cls, class_grip, override=None, directive=None, explicit=None):
    """(x, y, z, fraction, source) in mesh space. Priority: gripxyz line, per-weapon override fraction, the mesh's own `grip` line, handle of a sword-like mesh, class
    fraction. The twin of GripFinder.find (directive = the grip line measured from the low y end of the mesh as drawn)."""
    ys = [v[1] for v in body]
    y0, y1 = min(ys), max(ys)
    length = y1 - y0
    if explicit is not None:
        return explicit[0], explicit[1], explicit[2], (explicit[1] - y0) / length if length else 0.0, "gripxyz"
    middle = cls in MIDDLE_HELD
    if override is not None:
        frac, src = override, "override"
    elif directive is not None and not middle:
        frac, src = directive, "directive"
    else:
        frac, src = None, "class"
        if cls in HANDLE_MODE:
            hr = handle_range(body)
            if hr is not None and (hr[1] - hr[0]) >= MIN_HANDLE * length:
                frac, src = ((hr[0] + hr[1]) / 2 - y0) / length, "handle"
        if frac is None:
            frac = class_grip
    if not middle:
        frac = min(FRAC_MAX, max(FRAC_MIN, frac))
    frac = min(1.0, max(0.0, frac))
    gy = y0 + frac * length
    win = float("inf") if cls in ("item", "handitem") and src == "class" else WINDOW * length
    near = [v for v in body if abs(v[1] - gy) <= win]
    if near:
        gx = (min(v[0] for v in near) + max(v[0] for v in near)) / 2
        gz = (min(v[2] for v in near) + max(v[2] for v in near)) / 2
    else:
        gx = gz = 0.0
    return gx, gy, gz, frac, src


# ================================================================ the hand
def rot_matrix(rx, ry, rz):
    ax, ay, az = (math.radians(a) for a in (rx, ry, rz))
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    rxm = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    rym = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    rzm = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))
    mul = lambda a, b: tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))
    return mul(rxm, mul(rym, rzm))


def hand_point(rotation, translation_px, scale, left=False):
    """The point of the item's unit cube that lies exactly at the hand origin: H = 0.5 - (1/s) R^-1 (t/16). `left`: vanilla's mirror of the entry."""
    rx, ry, rz = rotation
    tx, ty, tz = translation_px
    if left:
        ry, rz, tx = -ry, -rz, -tx
    r = rot_matrix(rx, ry, rz)
    t = (tx / 16.0, ty / 16.0, tz / 16.0)
    v = tuple(sum(r[j][i] * t[j] for j in range(3)) for i in range(3))        # R^-1 = R^T
    return tuple(0.5 - x / scale for x in v)


def cube_point(place, mx, my, mz):
    """Mesh coordinates -> the unit cube (WeaponRenderer's place)."""
    ox, oy, angle, sc, ymin = place
    a = math.radians(angle)
    along = (my - ymin) * sc
    across = mx * sc
    return (ox + along * math.cos(a) + across * math.sin(a), oy + along * math.sin(a) - across * math.cos(a), 0.5 + mz * sc)


def shift_for(place, grip, target):
    """The translation (in the unit cube, applied after the placement) that puts the mesh's grip point on `target`."""
    g = cube_point(place, *grip)
    return tuple(t - c for t, c in zip(target, g))


def read_display(model_json, key):
    """(rotation, translation px, scale) of a display entry of an item model json (vanilla's NO_TRANSFORM when absent)."""
    d = (model_json.get("display") or {}).get(key)
    if not isinstance(d, dict):
        return (0, 0, 0), (0, 0, 0), 1.0
    sc = d.get("scale", [1, 1, 1])
    return tuple(d.get("rotation", [0, 0, 0])), tuple(d.get("translation", [0, 0, 0])), float(sc[0]) if sc else 1.0


def display_for(model_json, ctx):
    """The hand point for 'thirdperson_righthand', 'thirdperson_lefthand', 'firstperson_righthand' or 'firstperson_lefthand'."""
    rot, tr, sc = read_display(model_json, ctx)
    return hand_point(rot, tr, sc, left=ctx.endswith("lefthand"))


HAND_FP = (0.56, -0.52, -0.72)
HAND_FP_LEFT = (-0.56, -0.52, -0.72)
BASE_SCALE = 0.68
BASE_TRANSLATION = (1.13, 3.2, 1.13)
SPRITE_HILT = (0.25, 0.25, 0.5)


def fp_target(disp, left=False):
    """GripMath.firstPersonTarget: the point of the unit cube that lies where an ordinary sprite sword's hilt lies in first person under this entry."""
    rot, tr, sc = disp
    if left:
        rot, tr = (rot[0], -rot[1], -rot[2]), (-tr[0], tr[1], tr[2])
    r = rot_matrix(*rot)
    sx = -1.0 if left else 1.0
    a = [sx * BASE_TRANSLATION[0] / 16.0, BASE_TRANSLATION[1] / 16.0, BASE_TRANSLATION[2] / 16.0]
    hilt = [SPRITE_HILT[i] - 0.5 for i in range(3)]
    for i in range(3):
        a[i] += sum(r[i][j] * BASE_SCALE * hilt[j] for j in range(3))
    d = [a[0] - tr[0] / 16.0, a[1] - tr[1] / 16.0, a[2] - tr[2] / 16.0]
    v = [sum(r[j][i] * d[j] for j in range(3)) for i in range(3)]
    return tuple(0.5 + x / sc for x in v)


def grip_class(item_id, classes_json, table):
    """The grip class of an id: weapon_classes.json (id, then base id), then weapon_grips.json classOf, then the keyword guess (GripTable.gripClass)."""
    w = classes_json["weapons"]
    base = STATE_SUFFIX.sub("", item_id)
    if item_id in w:
        return w[item_id]
    if base in w:
        return w[base]
    co = (table or {}).get("classOf") or {}
    if item_id in co:
        return co[item_id]
    if base in co:
        return co[base]
    import weapon_classes
    return weapon_classes.guess_class(item_id)


def classes():
    with open(os.path.join(ASSETS, "weapon_classes.json"), encoding="utf-8") as f:
        return json.load(f)


STATE_SUFFIX = re.compile(r"(_(pull[123]|charged|spent|genesis|5leaf|liquid|off|blue|left|curse)|(?<=lantern)_red)$")


# ---------------------------------------------------------------- first-person view limit (GripMath.firstPersonShare)
Z_MARGIN, X_VIEW = 0.16, 1.32
Y_VIEW = 0.92 * math.tan(math.radians(35.0))


def camera(disp, left, p):
    """GripMath.camera: a point of the unit cube in first-person camera space."""
    rot, tr, sc = disp
    if left:
        rot, tr = (rot[0], -rot[1], -rot[2]), (-tr[0], tr[1], tr[2])
    r = rot_matrix(*rot)
    h = HAND_FP_LEFT if left else HAND_FP
    q = [sc * (p[i] - 0.5) for i in range(3)]
    return tuple(h[i] + tr[i] / 16.0 + sum(r[i][j] * q[j] for j in range(3)) for i in range(3))


def in_view(place, box, delta, disp, left):
    x0, x1, y0, y1, z0, z1 = box
    for i in range(8):
        c = cube_point(place, (x0, x1)[i & 1], (y0, y1)[i >> 1 & 1], (z0, z1)[i >> 2 & 1])
        if camera(disp, left, [c[k] + delta[k] for k in range(3)])[2] > -Z_MARGIN:
            return False
    t = cube_point(place, (x0 + x1) / 2, y1, (z0 + z1) / 2)
    c = camera(disp, left, [t[k] + delta[k] for k in range(3)])
    d = -c[2]
    return d >= Z_MARGIN - 1e-6 and abs(c[1]) <= Y_VIEW * d + 1e-6 and abs(c[0]) <= X_VIEW * d + 1e-6


def fp_share(place, box, delta, disp, left):
    """How much of the full first-person shift to apply (1 = all, 0 = none): the largest share that keeps the weapon in view."""
    if box is None or not disp[2] > 0:
        return 1.0
    if in_view(place, box, delta, disp, left):
        return 1.0
    if not in_view(place, box, (0, 0, 0), disp, left):
        return 0.0
    lo, hi = 0.0, 1.0
    for _ in range(24):
        mid = (lo + hi) / 2
        if in_view(place, box, [d * mid for d in delta], disp, left):
            lo = mid
        else:
            hi = mid
    return lo


def body_box(body):
    xs, ys, zs = [v[0] for v in body], [v[1] for v in body], [v[2] for v in body]
    return (min(xs), max(xs), min(ys), max(ys), min(zs), max(zs))

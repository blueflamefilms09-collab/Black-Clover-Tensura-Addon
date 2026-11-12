"""The seven Seven Star Weapons, made in Blender (3.80.0): hammer, claw, axe, whip, staff, gauntlet, projectile.

Run: python tools/blender/seven_star.py [--only hammer,axe,...] [--trial DIR] [--fast]
  --trial DIR  writes the meshes, skins and previews into DIR instead of the mod's assets (for looking before replacing);
  --fast       few samples and a 512 px bake (for iterating; the release run uses the defaults of bl_lib).

Each weapon keeps the box, the grip position and the header (`place`, `grip`, `glow`) of the 3.79.0 mesh it replaces (HEADERS below), so
the hand, the display transforms, the size class and the grip table still fit; the claw and the gauntlet keep the layout HandWrap needs
(the forearm along y from the mouth at y = 0, the bracer hollow round the arm). See bl_lib.py for what Blender does here.
Worst case in game: the largest mesh is under bl_lib.QUAD_BUDGET (2800 quad lines); drawing cost only.
"""
import math
import os
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402
from mathutils import Vector  # noqa: E402

RUNE_FONT = "/usr/share/fonts/truetype/freefont/FreeMono.ttf"               # has the runic block
PI = math.pi
FAST = {}
SAMPLES = 24

# the header lines of the meshes these replace (3.79.0): the place on the sprite, the grip share, the glow pulse
HEADERS = {
    "vassal_hammer": ["place -0.05567 -0.05567 45.000 0.009482 -11.000", "grip 0.254", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_claw": ["place 0.07656 0.07656 45.000 0.006981 0.000", "grip 0.110", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_axe": ["place -0.08164 -0.08164 45.000 0.009754 -12.000", "grip 0.264", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_whip": ["place 0.00474 0.00474 45.000 0.008958 -24.000", "grip 0.212", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_staff": ["place -0.31331 -0.31331 45.000 0.008735 -9.000", "grip 0.381", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_gauntlet": ["place 0.10970 0.10970 45.000 0.011993 -2.852", "grip 0.259", "glow FFFFFF 0.14 0.45 1.0"],
    "vassal_projectile": ["place 0.15310 0.15310 45.000 0.005458 -24.633", "grip 0.394", "glow FFFFFF 0.14 0.45 1.0"],
}
# the box each mesh must stay inside (x0, x1, y0, y1, z0, z1): the box of the mesh it replaces; y0 and y1 must be met
BOXES = {
    "vassal_hammer": (-49.6, 40.4, -11.0, 190.0, -39.6, 39.6),
    "vassal_claw": (-46.0, 33.4, 0.0, 182.0, -17.6, 17.6),
    "vassal_axe": (-80.9, 66.5, -12.0, 207.5, -18.8, 18.8),
    "vassal_whip": (-21.2, 28.8, -24.0, 198.0, -21.6, 14.5),
    "vassal_staff": (-38.2, 38.2, -9.0, 287.3, -21.3, 21.3),
    "vassal_gauntlet": (-13.3, 17.0, -2.852, 85.4, -17.9, 20.3),
    "vassal_projectile": (-18.1, 17.6, -24.633, 109.0, -5.7, 5.7),
}


# ================================================================ materials
def mats():
    m = {}
    m["steel"] = bl.mat("steel", rgb(200, 208, 224), metallic=1.0, rough=0.22, bump="brushed", bstr=0.16, bevel=0.25, groove=rgb(125, 136, 162), gamount=0.55)
    m["dark"] = bl.mat("dark_steel", rgb(62, 72, 104), metallic=1.0, rough=0.34, bump="hammer", bstr=0.12, bevel=0.3, groove=rgb(32, 38, 60), gamount=0.5)
    m["navy"] = bl.mat("navy_steel", rgb(40, 50, 96), metallic=1.0, rough=0.36, bump="brushed", bstr=0.14, bevel=0.15, groove=rgb(18, 24, 52), gamount=0.6)
    m["gold"] = bl.mat("gold", rgb(240, 186, 82), metallic=1.0, rough=0.24, bump="hammer", bstr=0.06, bevel=0.25)
    m["leather"] = bl.mat("leather", rgb(104, 62, 32), rough=0.6, bump="cross", bstr=0.9, groove=rgb(30, 16, 8), gamount=1.0, bevel=0.0)
    # for bake-only details (thin floating plates): no Bevel node
    m["gold_line"] = bl.mat("gold_line", rgb(246, 196, 96), metallic=1.0, rough=0.25, bevel=0.0)
    m["blued"] = bl.mat("blued_steel", rgb(46, 62, 110), metallic=1.0, rough=0.34, bump="brushed", bstr=0.15, bevel=0.0, groove=rgb(22, 30, 62), gamount=0.6)
    return m


def gemmat(name, base, emit, strength=0.32):
    return bl.mat(name, base, metallic=0.35, rough=0.08, emit=emit, strength=strength, bevel=0.05)


def glowmat(name, emit, strength=1.5, base=(0.01, 0.01, 0.012, 1.0)):
    return bl.mat(name, base, rough=0.5, emit=emit, strength=strength, bevel=0.0)


# ================================================================ helpers
def rect(x0, y0, x1, y1):
    return [(x0, y0), (x1, y0), (x1, y1), (x0, y1)]


def star_outline(cx, cy, r, pts=4, inner=0.38, rot=0.0):
    out = []
    for k in range(2 * pts):
        a = rot + PI * k / pts
        rr = r if k % 2 == 0 else r * inner
        out.append((cx + rr * math.sin(a), cy + rr * math.cos(a)))
    return out


def line_outline(p0, p1, w):
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    n = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / n * w, dx / n * w
    return [(p0[0] + nx, p0[1] + ny), (p1[0] + nx, p1[1] + ny), (p1[0] - nx, p1[1] - ny), (p0[0] - nx, p0[1] - ny)]


def ramp_line(name, p0, z0, p1, z1, w, material, thick=0.05):
    """A bake-only line between two points of a face whose height changes (z0 at p0, z1 at p1; negative on the -z face)."""
    o = line_outline(p0, p1, w)
    zs = [z0, z1, z1, z0]
    sg = 1.0 if z0 >= 0 else -1.0
    verts = [(x, y, z - 0.02 * sg) for (x, y), z in zip(o, zs)] + [(x, y, z + thick * sg) for (x, y), z in zip(o, zs)]
    faces = [(0, 1, 2, 3), (4, 5, 6, 7)] + [(k, (k + 1) % 4, (k + 1) % 4 + 4, k + 4) for k in range(4)]
    return bl.obj(name, verts, faces, material, detail=True)


def flat(name, outline, z, material, thick=0.06):
    """A bake-only plate lying on a face at height z (z < 0: on the -z face)."""
    lo, hi = (z - 0.02, z + thick) if z >= 0 else (z - thick, z + 0.02)
    return bl.plate(name, outline, lo, hi, material, detail=True)


def constellation(prefix, pts, links, z, sm, lm, r=2.0, w=0.32, first=1.35):
    """Seven stars and the lines between them, as bake-only plates on the face at height z; returns the objects (to turn onto another face)."""
    obs = []
    for i, (a, b) in enumerate(links):
        obs.append(flat("%sl%d" % (prefix, i), line_outline(pts[a], pts[b], w), z, lm, 0.04))
    for i, (x, y) in enumerate(pts):
        obs.append(flat("%ss%d" % (prefix, i), star_outline(x, y, r * (first if i == 0 else 1.0)), z, sm, 0.07))
    return obs


DIPPER = [(-11.5, 6.0), (-6.5, 8.5), (-1.5, 5.5), (3.0, 1.5), (4.0, -6.0), (10.5, -8.0), (11.5, -1.0)]
DIPPER_LINKS = [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 3)]


def face_x(obs, s):
    """Turns plates drawn on the +z face (u, y, h) onto the +x face (s = 1: (h, y, -u)) or the -x face (s = -1: (-h, y, u))."""
    for o in obs if isinstance(obs, (list, tuple)) else [obs]:
        bl.move(o, rot=(0, 90 * s, 0))


def torus(name, R, r, material, center=(0, 0, 0), axis="z", n=24, m=8, part=None, squash=1.0, a0=0.0, a1=2 * PI, detail=False):
    """A ring of radius R and tube radius r round `axis` (squash: the tube's size along the axis)."""
    X, Y, Z = Vector((1, 0, 0)), Vector((0, 1, 0)), Vector((0, 0, 1))
    e1, e2, e3 = {"z": (X, Y, Z), "x": (Y, Z, X), "y": (Z, X, Y)}[axis]
    C = Vector(center)
    full = abs(a1 - a0 - 2 * PI) < 1e-6
    rings = []
    for i in range(n + 1):
        a = a0 + (a1 - a0) * i / n
        rad = e1 * math.cos(a) + e2 * math.sin(a)
        c = C + rad * R
        rings.append([c + rad * (r * math.cos(2 * PI * k / m)) + e3 * (r * squash * math.sin(2 * PI * k / m)) for k in range(m)])
    return bl.loft(name, rings, material, caps=(not full, not full), part=part, detail=detail)


def xring(x, sec, cy=0.0, s=1.0, sz=None):
    """A cross-section [(y, z)] placed at x (for a piece that runs along x)."""
    sz = s if sz is None else sz
    return [Vector((x, cy + py * s, pz * sz)) for py, pz in sec]


def octa(a, b):
    k = 1.0 / math.cos(PI / 8)
    return [(a * k * math.cos(PI / 8 + PI / 4 * i), b * k * math.sin(PI / 8 + PI / 4 * i)) for i in range(8)]


def ptube(name, pts, radius, material, sides=8, sub=4, part=None, caps=(True, True), detail=False):
    """A round tube along a path in space (parallel-transport frames, so it never pinches); returns (object, [(point, tangent, normal)])."""
    path = bl.smooth_path(pts, sub) if sub > 1 else [Vector(p) for p in pts]
    n = len(path)
    T = [(path[min(i + 1, n - 1)] - path[max(i - 1, 0)]).normalized() for i in range(n)]
    nrm = T[0].orthogonal().normalized()
    rings, frames = [], []
    for i, p in enumerate(path):
        nrm = (nrm - T[i] * nrm.dot(T[i])).normalized()
        b = T[i].cross(nrm)
        r = radius(i / (n - 1)) if callable(radius) else radius
        rings.append([p + (nrm * math.cos(2 * PI * k / sides) + b * math.sin(2 * PI * k / sides)) * r for k in range(sides)])
        frames.append((p.copy(), T[i].copy(), nrm.copy()))
    return bl.loft(name, rings, material, caps, part, detail=detail), frames


def spike(name, base, tip, r, material, sides=6, part=None, r1=0.04):
    return bl.tube(name, [base, tip], None, material, sides=sides, sub=1, part=part, taper=(r, r1))


def band(name, y0, y1, r_in, r_out, material, part, sides=10, squash=1.0, lip=0.6):
    """A ring round the y axis (a fitting on a haft)."""
    return bl.lathe(name, [(r_in, y0), (r_out, y0 + lip), (r_out, y1 - lip), (r_in, y1)], material, sides=sides, part=part, squash=squash, caps=(False, False))


def fan_blade(name, inner, outer, thick, material, t0=0.0, t1=1.0, nt=6, bulge=0.0, part=None):
    """An axe blade between an inner and an outer line (same number of points, top to bottom): thick(t) is its half thickness from the
    root (t = 0) to the edge (t = 1, where it is 0); bulge pulls the top and bottom outlines in. Returns the object."""
    ns = len(inner)
    rings = []
    for i in range(ns):
        up, dn = [], []
        for j in range(nt + 1):
            t = t0 + (t1 - t0) * j / nt
            p = blade_point(inner, outer, i / (ns - 1), t, bulge)
            up.append(Vector((p[0], p[1], thick(t))))
            dn.append(Vector((p[0], p[1], -thick(t))))
        rings.append(up + (dn[-2::-1] if thick(t1) < 1e-3 else dn[::-1]))
    return bl.loft(name, rings, material, part=part)


def blade_point(inner, outer, s, t, bulge):
    f = s * (len(inner) - 1)
    i = min(int(f), len(inner) - 2)
    u = f - i
    a = (inner[i][0] + (inner[i + 1][0] - inner[i][0]) * u, inner[i][1] + (inner[i + 1][1] - inner[i][1]) * u)
    b = (outer[i][0] + (outer[i + 1][0] - outer[i][0]) * u, outer[i][1] + (outer[i + 1][1] - outer[i][1]) * u)
    return (a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t - bulge * (1 - 2 * s) * math.sin(PI * t))


def done(item_id, out):
    """Bakes and writes the weapon, then puts the header of the mesh it replaces back (bl_lib writes `orient manual` and derives its own
    grip share from the quad counts; these meshes are normalised by the renderer and are held where the old ones were)."""
    hdr = os.path.join(tempfile.gettempdir(), item_id + "_header.txt")
    with open(hdr, "w") as fh:
        fh.write("\n".join(HEADERS[item_id]) + "\n")
    mesh, atlas, stats = bl.finish(item_id, glow=("FFFFFF", 0.14, 0.45, 1.0), header_from=hdr, **out, **FAST)
    path = os.path.join(out.get("out_mesh") or bl.MESH_DIR, item_id + ".txt")
    with open(path) as fh:
        lines = fh.read().split("\n")
    body = [ln for ln in lines[1:] if ln.split(" ")[0] not in ("place", "grip", "orient", "glow")]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join([lines[0]] + HEADERS[item_id] + body))
    check_box(item_id, stats)
    return mesh, atlas, stats


def check_box(item_id, stats):
    """The self-check: the mesh is inside the box of the mesh it replaces and meets its length exactly (0.15 cm)."""
    x0, x1, y0, y1, z0, z1 = BOXES[item_id]
    lo, hi = stats["bbox_min"], stats["bbox_max"]
    e = 0.06
    bad = []
    if lo[0] < x0 - e or hi[0] > x1 + e:
        bad.append("x %.2f..%.2f outside %.1f..%.1f" % (lo[0], hi[0], x0, x1))
    if lo[2] < z0 - e or hi[2] > z1 + e:
        bad.append("z %.2f..%.2f outside %.1f..%.1f" % (lo[2], hi[2], z0, z1))
    if abs(lo[1] - y0) > 0.15 or abs(hi[1] - y1) > 0.15:
        bad.append("y %.2f..%.2f is not %.2f..%.2f" % (lo[1], hi[1], y0, y1))
    if bad:
        raise SystemExit(item_id + ": " + "; ".join(bad))


# ================================================================ the Hammer
def hammer(out):
    bl.reset(SAMPLES)
    M = mats()
    amber = gemmat("amber", rgb(255, 150, 30), rgb(255, 180, 50))
    starm = glowmat("starglow", rgb(255, 228, 160))
    fire = glowmat("fire", rgb(255, 150, 40), 1.4)
    # ---- haft, grip, fittings, pommel
    bl.lathe("haft", [(4.3, 0), (4.3, 64), (5.0, 70), (5.4, 146)], M["navy"], sides=10)
    bl.lathe("grip_wrap", [(4.4, 18), (4.85, 21), (4.85, 59), (4.4, 62)], M["leather"], sides=10)
    for i, (y0, y1) in enumerate(((14.0, 17.6), (62.4, 66.5), (88, 91.5), (117.5, 122))):
        band("fit%d" % i, y0, y1, 4.3, 5.55, M["gold"], "haft_fittings")
    bl.lathe("pommel_cap", [(0.01, -11), (2.0, -9.2), (2.6, -6.2), (6.6, -3.6), (7.0, -1), (6.6, 2), (5.0, 5), (4.5, 11)], M["gold"], sides=12)
    bl.lathe("neck_collar", [(5.5, 93), (7.2, 95), (7.6, 100), (10.5, 105.5), (13.6, 108.5), (13.8, 111)], M["gold"], sides=12)
    # ---- the head: an eight-sided barrel along x, banded in gold
    cy = 144.0
    sec = octa(30.5, 27.5)
    bl.bevel(bl.loft("head_blocks", [xring(x, sec, cy, s) for x, s in ((-41, 0.93), (-39, 1.0), (30, 1.0), (32, 0.93))], M["dark"]), 0.9, 2)
    for i, (x0, x1) in enumerate(((-37.5, -31.0), (21.5, 28.0))):
        bl.bevel(bl.loft("rim%d" % i, [xring(x0, sec, cy, 1.07), xring(x1, sec, cy, 1.07)], M["gold"], part="head_rim_bands"), 0.5)
    bl.bevel(bl.loft("head_hub", [xring(x, sec, cy, s) for x, s in ((-7, 1.05), (-5.5, 1.1), (5.5, 1.1), (7, 1.05))], M["steel"]), 0.6)
    spike("finial", (0, 176.5, 0), (0, 190, 0), 4.4, M["gold"], sides=8, part="head_rim_bands")
    for sgn, x0 in ((-1, -41.0), (1, 32.0)):
        bl.bevel(bl.loft("face%d" % sgn, [xring(x0 + sgn * dx, sec, cy, s) for dx, s in ((0, 0.86), (4.0, 0.86), (6.5, 0.72))], M["steel"], part="face_plates"), 0.5)
        bl.bevel(bl.loft("face_rim%d" % sgn, [xring(x0 + sgn * 0.8, sec, cy, 0.91), xring(x0 + sgn * 3.0, sec, cy, 0.91)], M["gold"], part="face_plates"), 0.35)
        xf = x0 + sgn * 6.5
        torus("fire%d" % sgn, 16.2, 1.4, fire, center=(xf + sgn * 0.5, cy, 0), axis="x", n=24, m=6, part="face_fire_rings")
        pts = [(u, cy + v) for u, v in DIPPER]
        face_x(constellation("dip%d" % sgn, pts, DIPPER_LINKS, abs(xf) + 0.08, starm, M["gold_line"], r=2.3, w=0.35), sgn)
    # ---- medallions with the amber stones on the two sides, runes and rivets on the flats
    for s in (1, -1):
        med = bl.lathe("medallion", [(17, 26.5), (17, 30.8), (15.2, 32.4), (12.6, 33.4), (0.01, 33.4)], M["gold"], sides=16, origin=(0, cy, 0), axis="z", caps=(False, False), part="medallions")
        bez = bl.lathe("bezel", [(12.2, 33.2), (12.4, 34.6), (11.2, 36.0), (9.6, 36.0), (9.2, 34.2)], M["gold"], sides=16, origin=(0, cy, 0), axis="z", caps=(False, False), part="gem_bezels")
        g = bl.gem("gems", 9.2, amber, center=(0, cy, 34.8), depth=0.35, crown=0.2, sides=8, axis="z")
        if s < 0:
            for o in (med, bez, g):
                bl.move(o, scale=(1, 1, -1))
        bl.text("runes%d" % s, "ᛏᛉ", 11.0, starm, (-24.0, cy, 27.62 * s), rot=(0, 0 if s > 0 else 180, 0), spacing=1.0, font=RUNE_FONT)
        for x in (-34.25, 24.75):
            for dy in (-8.0, 8.0):
                flat("rivet%d%d%d" % (s, x > 0, dy > 0), star_outline(x, cy + dy, 1.5, 6, 0.86), 29.5 * s, M["gold_line"], 0.1)
    return done("vassal_hammer", out)


# ================================================================ the Axe
def axe(out):
    bl.reset(SAMPLES)
    M = mats()
    teal = gemmat("teal", rgb(20, 190, 170), rgb(50, 235, 205))
    tglow = glowmat("tealglow", rgb(90, 245, 220), 1.6)
    edgem = glowmat("edgeglow", rgb(50, 232, 200), 1.05, base=rgb(30, 120, 110))
    wood = bl.mat("wood", rgb(120, 78, 40), rough=0.55, bump="grain", bstr=0.5, groove=rgb(58, 34, 16), gamount=0.8, bevel=0.1)
    # ---- haft
    bl.lathe("haft", [(4.4, 0), (4.4, 76), (5.0, 82), (5.2, 150)], wood, sides=10)
    bl.lathe("grip_wrap", [(4.5, 18), (5.1, 21), (5.1, 71), (4.5, 74)], M["leather"], sides=10)
    bl.lathe("butt", [(0.01, -12), (2.4, -9.6), (3.0, -7), (5.8, -5), (5.9, 0), (4.6, 3)], M["gold"], sides=10, part="haft_fittings")
    for i, (y0, y1) in enumerate(((14.5, 17.8), (74.4, 78), (100, 103.5), (122.5, 127))):
        band("fit%d" % i, y0, y1, 4.4, 5.85, M["gold"], "haft_fittings")
    # ---- hub with the stones
    hub = bl.loft("head_hub", [bl.ring(y, bl.rrect(13 * s, 13 * s, 3.2)) for y, s in ((127, 0.92), (130, 1.0), (170, 1.0), (173, 0.92))], M["dark"])
    bl.bevel(hub, 0.6)
    for i, (y0, y1) in enumerate(((126.0, 130.2), (169.8, 173.4))):
        bl.bevel(bl.loft("trim%d" % i, [bl.ring(y0, bl.rrect(13.7, 13.7, 3.4)), bl.ring(y1, bl.rrect(13.7, 13.7, 3.4))], M["gold"], part="hub_trim"), 0.4)
    for s in (1, -1):
        bez = bl.lathe("bezel", [(11.4, 12.8), (11.6, 14.2), (10.2, 15.2), (8.9, 15.2), (8.6, 13.6)], M["gold"], sides=16, origin=(0, 150, 0), axis="z", caps=(False, False), part="gem_bezels")
        g = bl.gem("gems", 8.6, teal, center=(0, 150, 14.3), depth=0.35, crown=0.2, sides=8, axis="z")
        if s < 0:
            bl.move(bez, scale=(1, 1, -1))
            bl.move(g, scale=(1, 1, -1))
    # ---- the blade: a bearded crescent, thick at the hub, an edge of light
    edge = bl.smooth_path([(-50, 206.9, 0), (-71, 181, 0), (-80, 141, 0), (-73.5, 100, 0), (-52, 65.2, 0)], 3)
    outer = [(p.x, p.y) for p in edge]
    ns = len(outer)
    inner = [(-9.0, 172.0 - 44.0 * i / (ns - 1)) for i in range(ns)]
    thick = lambda t: 5.2 * max(1.0 - t, 0.0) ** 0.85                         # noqa: E731
    fan_blade("head_blade", inner, outer, thick, M["steel"], 0.0, 0.86, 6, bulge=17.0)
    fan_blade("edge_fire", inner, outer, thick, edgem, 0.86, 1.0, 2, bulge=17.0)
    st = [(0.14, 0.52), (0.28, 0.30), (0.41, 0.58), (0.53, 0.28), (0.62, 0.62), (0.77, 0.36), (0.88, 0.60)]
    pts = [blade_point(inner, outer, s, t, 17.0) for s, t in st]
    for sg in (1, -1):
        for i, ((s, t), p) in enumerate(zip(st, pts)):
            flat("bst%d%d" % (sg, i), star_outline(p[0], p[1], 3.0 if i == 3 else 2.3), (thick(t) + 0.12) * sg, tglow, 0.07)
        for i in range(6):
            (s0, t0), (s1, t1) = st[i], st[i + 1]
            ramp_line("bln%d%d" % (sg, i), pts[i], (thick(t0) + 0.08) * sg, pts[i + 1], (thick(t1) + 0.08) * sg, 0.32, tglow)
        # a gold band along the root of the blade
        strip = [blade_point(inner, outer, k / 10.0, 0.10, 17.0) for k in range(11)] + [blade_point(inner, outer, k / 10.0, 0.16, 17.0) for k in range(10, -1, -1)]
        flat("bgold%d" % sg, strip, (thick(0.10) + 0.03) * sg, M["gold_line"], 0.05)
    # ---- poll, back spike, top spike
    poll = bl.loft("poll_block", [xring(x, bl.rrect(hy, hz, 3.0), 150.0) for x, hy, hz in ((12, 17.0, 13.0), (16, 19.6, 17.0), (29, 19.6, 17.0), (33.5, 15.0, 12.0), (39.5, 11.0, 8.5))], M["dark"])
    bl.bevel(poll, 0.6)
    bl.bevel(bl.loft("poll_band", [xring(19.5, bl.rrect(20.3, 17.6, 3.2), 150.0), xring(25.5, bl.rrect(20.3, 17.6, 3.2), 150.0)], M["gold"], part="poll_block"), 0.4)
    bl.sweep("back_spike", [(37, 150, 0), (46, 150, 0), (66.4, 150, 0)], bl.lozenge(1.0, 1.0, flat=0.25), M["steel"], scale=lambda t: (10.5 * (1 - t) ** 0.9 + 0.1, 6.8 * (1 - t) ** 0.8 + 0.08))
    for sg in (1, -1):
        path = bl.smooth_path([(31, 150 + sg * 12, 0), (40, 150 + sg * 19, 0), (48, 150 + sg * 24.5, 0), (55, 150 + sg * 26.6, 0)], 3)
        bl.sweep("flange%d" % sg, path, bl.lozenge(1.0, 1.0), M["gold"], scale=lambda t: (3.4 * (1 - t) ** 0.8 + 0.1, 3.0 * (1 - t) ** 0.7 + 0.1), part="back_spike")
    bl.loft("haft_cap", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.3)) for y, w, t in ((172.5, 6.0, 4.6), (179, 11.8, 4.7), (189, 8.0, 3.4), (200, 3.6, 1.6), (207.5, 0.1, 0.06))], M["steel"])
    return done("vassal_axe", out)


# ================================================================ the Staff
def staff(out):
    bl.reset(SAMPLES)
    M = mats()
    crystal = bl.mat("crystal", rgb(120, 180, 255), metallic=0.4, rough=0.06, emit=rgb(105, 170, 255), strength=0.34, bevel=0.05)
    moon = glowmat("moonglow", rgb(170, 215, 255), 1.6)
    starm = glowmat("starglow", rgb(235, 240, 255))
    wrap = bl.mat("wrap_navy", rgb(52, 64, 132), rough=0.55, bump="cross", bstr=0.9, groove=rgb(10, 14, 40), gamount=1.0, bevel=0.0)
    # ---- shaft
    bl.lathe("shaft", [(4.2, 0), (4.2, 190)], M["navy"], sides=10)
    bl.lathe("grip_wrap", [(4.3, 80), (4.95, 83), (4.95, 125), (4.3, 128)], wrap, sides=10)
    for i, (y0, y1) in enumerate(((76.6, 79.6), (128.4, 131.4), (150, 153), (172, 175))):
        band("sb%d" % i, y0, y1, 4.2, 5.4, M["steel"], "shaft_bands")
    for i, (y0, y1) in enumerate(((74.0, 75.6), (133.0, 134.6), (146.5, 148.1), (168.3, 169.9))):
        band("gt%d" % i, y0, y1, 4.2, 5.1, M["gold"], "gold_trim", lip=0.4)
    bl.lathe("foot", [(0.01, -9), (1.6, -6), (2.6, 0), (5.6, 3), (5.6, 6), (4.5, 9), (4.2, 13)], M["steel"], sides=10)
    for s in (1, -1):
        for i, y in enumerate((20, 31, 42, 53, 64)):
            flat("sstar%d%d" % (s, i), star_outline(0, y, 1.3 if i % 2 == 0 else 0.95), 4.06 * s, starm, 0.07)
        for i, y in enumerate((137.5, 142.5, 157, 162, 167)):
            flat("sstar_u%d%d" % (s, i), star_outline(0, y, 1.1), 4.06 * s, starm, 0.07)
    bl.lathe("collar", [(4.6, 184), (6.5, 187), (7.0, 192), (9.8, 200), (10.0, 204), (7.6, 210), (5.0, 214.5)], M["gold"], sides=12)
    # ---- the head: a ring with a moon over it, a crystal turning inside, arms holding it
    C = (0, 241, 0)
    torus("head_ring", 25.3, 2.6, M["steel"], C, "z", n=32, m=8, squash=1.1)
    torus("head_ring_inner", 21.4, 0.75, moon, C, "z", n=32, m=4)
    torus("orbit_ring", 20.2, 1.0, M["gold"], C, "x", n=24, m=6)
    bl.loft("gem", [bl.ring(y, bl.circle(r, 6)) for y, r in ((220.0, 0.05), (229.5, 8.6), (238.0, 11.6), (246.5, 8.6), (255.7, 0.05))], crystal)
    arm = bl.sweep("prong", bl.smooth_path([(4, 204, 0), (14, 207, 0), (24, 214, 0), (31, 225, 0), (33.2, 238, 0)], 3), bl.lozenge(1.0, 1.0, flat=0.3), M["steel"],
                   scale=lambda t: (2.6 * (1 - t) ** 0.6 + 0.2, 3.1 * (1 - t) ** 0.5 + 0.25), part="prongs")
    bl.mirror(arm, "x")
    leaf = bl.sweep("leaf", bl.smooth_path([(5, 210, 0), (12, 216, 0), (18, 224, 0), (20.4, 232, 0)], 3), bl.lozenge(1.0, 1.0), M["gold"],
                    scale=lambda t: (1.9 * (1 - t) ** 0.7 + 0.15, 2.2 * (1 - t) ** 0.6 + 0.2), part="prongs")
    bl.mirror(leaf, "x")
    n = 16
    a0, a1 = math.radians(12), math.radians(168)
    path = [(35 * math.cos(a0 + (a1 - a0) * i / n), 241 + 35 * math.sin(a0 + (a1 - a0) * i / n), 0) for i in range(n + 1)]
    cw = lambda t: 11.05 * math.sin(PI * t) ** 0.75 + 0.25                    # noqa: E731
    ct = lambda t: 2.6 * math.sin(PI * t) ** 0.5 + 0.25                       # noqa: E731
    bl.sweep("crescent", path, bl.lozenge(1.0, 1.0, flat=0.34), M["gold"], scale=lambda t: (cw(t), ct(t)))
    for s in (1, -1):
        for i in range(7):
            ang = math.radians(30 + 20 * i)
            t = (ang - a0) / (a1 - a0)
            flat("cst%d%d" % (s, i), star_outline(35 * math.cos(ang), 241 + 35 * math.sin(ang), 2.6 if i == 3 else 1.5), (ct(t) + 0.08) * s, starm, 0.07)
    return done("vassal_staff", out)


# ================================================================ the Claw (worn on the hand: the mouth of the bracer at y = 0)
def claw(out):
    bl.reset(SAMPLES)
    M = mats()
    amber = gemmat("amber", rgb(255, 150, 30), rgb(255, 180, 50))
    starm = glowmat("starglow", rgb(255, 228, 160))
    fire = glowmat("fire", rgb(255, 160, 45), 1.5)
    lining = bl.mat("lining", rgb(60, 36, 22), rough=0.8, bump="leather", bstr=0.4, bevel=0.0)
    oval = lambda rx, rz: bl.circle(rx, 16, rz=rz)                            # noqa: E731
    # ---- bracer: a hollow sleeve round the forearm, three flared lames over it
    bl.loft("cuff", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0, 17.5, 12.2), (5, 17.0, 12.0), (34, 16.9, 12.0), (40, 17.2, 12.2))], M["steel"], caps=(False, False))
    li = bl.loft("cuff_lining", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0.5, 16.3, 11.3), (34, 16.0, 11.1))], lining, caps=(False, False))
    li.data.flip_normals()
    for k in range(3):
        y0 = 1.0 + 9.2 * k
        top = 21.5 + 1.7 * k
        # each lame is wide at its lower edge (toward the elbow) and closes on the sleeve above: its face looks up, into the light
        bl.loft("lame%d" % k, [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((y0 + 1.4, top, 13.3), (y0 + 6.0, 18.8, 12.9), (y0 + 8.6, 17.8, 12.5))], M["dark"], caps=(False, False), part="cuff_lames")
        bl.loft("lame_lip%d" % k, [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((y0 + 1.7, top + 0.3, 13.4), (y0 - 0.1, top + 0.3, 13.4), (y0 - 0.4, top - 0.8, 12.9), (y0 + 0.3, 17.4, 12.3))], M["gold"], caps=(False, False), part="cuff_lames")
    for s in (1, -1):
        bk = bl.plate("buckle%d" % s, rect(-4.0, 8.6, 4.0, 28.4), 12.4 if s > 0 else -14.3, 14.3 if s > 0 else -12.4, M["gold"], part="buckles")
        bl.bevel(bk, 0.5)
        flat("buckle_in%d" % s, rect(-2.2, 11, 2.2, 26), 14.32 * s, M["blued"], 0.05)
    bl.loft("hand_collar", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((31, 17.0, 12.2), (32.6, 17.5, 12.5), (36.6, 17.5, 12.5), (38.2, 17.0, 12.2))], M["gold"], caps=(False, False))
    # ---- the hand shell with the stone and seven stars on each face
    bl.bevel(bl.loft("back_plate", [bl.ring(y, bl.rrect(w, t, 5.0)) for y, w, t in ((34, 16.6, 12.4), (38, 18.0, 13.8), (66, 18.0, 13.8), (70.5, 16.8, 13.8), (74, 13.5, 10.5))], M["steel"], caps=(False, True)), 0.5)
    ring7 = [(-11, 43.5), (-12.3, 54.5), (-9.5, 65), (0, 69.2), (9.5, 65), (12.3, 54.5), (11, 43.5)]
    for s in (1, -1):
        bez = bl.lathe("bezel", [(10.1, 13.5), (10.3, 14.6), (9.0, 15.4), (7.7, 15.4), (7.4, 14.0)], M["gold"], sides=16, origin=(0, 54.5, 0), axis="z", caps=(False, False), part="gem_bezels")
        g = bl.gem("gems", 7.4, amber, center=(0, 54.5, 14.2), depth=0.3, crown=0.18, sides=8, axis="z")
        if s < 0:
            bl.move(bez, scale=(1, 1, -1))
            bl.move(g, scale=(1, 1, -1))
        constellation("c7%d" % s, ring7, [(i, i + 1) for i in range(6)], 13.86 * s, starm, M["gold_line"], r=1.7, w=0.28, first=1.0)
    bl.bevel(bl.loft("knuckle_bar", [bl.ring(y, bl.rrect(w, t, 4.0)) for y, w, t in ((70.5, 19.5, 12.8), (73, 22.0, 14.3), (81, 22.0, 14.3), (84.4, 18.5, 11.8))], M["gold"]), 0.6)
    # ---- the blades: three long ones leaning to the thumb side, a hook and a thumb claw
    def blade(name, pts, w, th, glow=True):
        path = bl.smooth_path([(x, y, 0) for x, y in pts], 4)
        fw = lambda t: w * (1 - t) ** 0.7 + 0.12                              # noqa: E731
        ft = lambda t: th * (1 - t) ** 0.6 + 0.1                              # noqa: E731
        bl.sweep(name, path, bl.lozenge(1.0, 1.0, flat=0.3), M["steel"], scale=lambda t: (fw(t), ft(t)), part="blades")
        if glow:
            for s in (1, -1):
                k = (len(path) - 3.0) / (len(path) - 1.0)                     # the ribbon stops two points short of the tip
                sec = lambda t, s=s, k=k: [(-0.2 * fw(t * k), s * ft(t * k) + 0.04 * s - 0.03), (0.2 * fw(t * k), s * ft(t * k) + 0.04 * s - 0.03),      # noqa: E731
                                           (0.2 * fw(t * k), s * ft(t * k) + 0.04 * s + 0.03), (-0.2 * fw(t * k), s * ft(t * k) + 0.04 * s + 0.03)]
                bl.sweep("%s_glow%d" % (name, s), path[:-2], sec, fire, detail=True)
    blade("blade_a", [(-13, 80), (-22, 110), (-34, 140), (-45.4, 163)], 5.4, 3.4)
    blade("blade_b", [(-1, 82), (-6, 115), (-13, 150), (-21.4, 181.9)], 5.8, 3.5)
    blade("blade_c", [(11, 80), (11.5, 110), (8.5, 140), (3, 167)], 5.4, 3.4)
    blade("hook", [(20, 73), (27, 69), (31, 63), (33.0, 56.5)], 3.0, 2.4, glow=False)
    blade("thumb", [(-18, 62), (-25, 55), (-29, 47), (-30.2, 38.2)], 3.0, 2.4, glow=False)
    return done("vassal_claw", out)


# ================================================================ the Gauntlet (worn: back of the hand = +x, thumb = +z, mouth at y = 0)
def gauntlet(out):
    bl.reset(SAMPLES)
    M = mats()
    amber = gemmat("amber", rgb(255, 150, 30), rgb(255, 180, 50))
    starm = glowmat("starglow", rgb(255, 228, 160))
    lining = bl.mat("lining", rgb(60, 36, 22), rough=0.8, bump="leather", bstr=0.4, bevel=0.0)
    fur = bl.mat("fur", rgb(236, 232, 222), rough=0.9, bump="leather", bstr=1.0, bscale=1.6, groove=rgb(150, 142, 128), gamount=0.9, bevel=0.0)
    strap = bl.mat("strap", rgb(104, 62, 32), rough=0.6, bump="leather", bstr=0.5, groove=rgb(40, 22, 10), gamount=0.7, bevel=0.1)
    oval = lambda rx, rz, n=16: bl.circle(rx, n, rz=rz)                       # noqa: E731
    # ---- vambrace: a hollow sleeve, lames flaring to the back of the arm, fins
    bl.loft("vambrace", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0, 11.5, 13.6), (4, 11.2, 13.3), (30, 10.8, 12.9), (38, 10.6, 12.8))], M["steel"], caps=(False, False))
    li = bl.loft("vambrace_lining", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((0.5, 10.7, 12.7), (30, 10.3, 12.3))], lining, caps=(False, False))
    li.data.flip_normals()
    for k in range(3):
        y0 = 3.0 + 7.6 * k
        bl.loft("lame%d" % k, [bl.ring(y, oval(rx, rz), ox=ox) for y, rx, rz, ox in ((y0, 11.4, 13.5, 0.0), (y0 + 2.2, 11.9, 13.9, 0.2), (y0 + 6.6, 12.6, 14.6, 1.0))], M["dark"], caps=(False, False), part="vambrace_lames")
        bl.loft("lame_lip%d" % k, [bl.ring(y, oval(rx, rz), ox=ox) for y, rx, rz, ox in ((y0 + 6.4, 12.7, 14.7, 1.0), (y0 + 8.0, 12.9, 14.9, 1.1), (y0 + 8.3, 12.0, 14.1, 0.9), (y0 + 7.6, 11.0, 13.2, 0.2))], M["gold"], caps=(False, False), part="vambrace_lames")
        fin = bl.plate("fin%d" % k, [(12.6, y0 + 1.0), (16.9, y0 + 9.5), (15.0, y0 + 10.4), (12.4, y0 + 6.6)], -0.7, 0.7, M["gold"], part="vambrace_lames")
        bl.bevel(fin, 0.25)
    bl.loft("cuff_trim", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((-0.8, 11.6, 13.7), (-0.4, 12.2, 14.5), (2.2, 12.2, 14.5), (2.8, 11.3, 13.4))], M["gold"], caps=(False, False))
    bl.loft("cuff_trim2", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((33.0, 10.7, 12.9), (33.6, 11.6, 13.8), (35.5, 11.6, 13.8), (36.1, 10.7, 12.9))], M["gold"], caps=(False, False), part="cuff_trim")
    bl.loft("cuff_fur", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((1.7, 12.25, 14.55), (0.4, 13.3, 15.6), (-1.6, 13.3, 15.6), (-2.852, 12.3, 14.5), (-2.852, 11.0, 13.1), (-0.6, 10.9, 13.0))], fur, caps=(False, False))
    bl.loft("wrist_strap", [bl.ring(y, oval(rx, rz)) for y, rx, rz in ((27, 10.9, 13.0), (27.5, 11.3, 13.4), (32.5, 11.2, 13.3), (33, 10.8, 12.9))], strap, caps=(False, False))
    for s in (1, -1):
        bl.bevel(bl.plate("buckles%d" % s, rect(-3.2, 28.2, 3.2, 31.8), 13.0 if s > 0 else -14.4, 14.4 if s > 0 else -13.0, M["gold"], part="buckles"), 0.35)
    # ---- the fist: the block of the hand, four curled fingers, the thumb
    bl.bevel(bl.loft("back_plate", [bl.ring(y, bl.rrect(w, t, 3.6)) for y, w, t in ((36, 10.2, 12.6), (40, 10.5, 13.0), (66, 10.5, 13.0), (71.5, 9.6, 12.5))], M["steel"], caps=(False, True)), 0.5)
    for i, zc in enumerate((-9.6, -3.2, 3.2, 9.6)):
        z0, z1 = zc - 2.95, zc + 2.95
        for j, outline in enumerate(([(10.8, 70.5), (10.8, 77.2), (8.8, 79.9), (-2.4, 79.9), (-4.4, 78.0), (-4.4, 70.5)],
                                     [(-6.2, 60.5), (-0.4, 60.5), (-0.4, 75.6), (-4.0, 77.2), (-6.2, 75.4)],
                                     [(-5.4, 57.7), (2.6, 57.7), (3.4, 59.6), (3.4, 62.6), (-5.4, 62.6)])):
            bl.bevel(bl.plate("finger%d%d" % (i, j), outline, z0, z1, M["steel"] if j else M["dark"], part="fingers"), 0.7)
        spike("stud%d" % i, (7.6, 78.2, zc), (9.4, 85.4, zc), 2.5, M["gold"], sides=6, part="knuckle_studs")
    th = bl.plate("thumb", [(-3.0, 42.0), (6.0, 41.4), (8.5, 52.0), (7.4, 65.0), (4.0, 73.6), (-1.0, 72.6), (-3.6, 60.0)], 13.0, 19.4, M["steel"], part="fingers")
    bl.bevel(th, 0.9, 2)
    # ---- the plate on the back of the hand with its stone and seven stars; a second stone on the outer side
    bp = bl.plate("hand_plate", [(-9.2, 44), (-9.2, 70), (-6.5, 78), (6.5, 78), (9.2, 70), (9.2, 44), (0, 39)], 10.3, 11.5, M["dark"], part="back_plate")
    bl.bevel(bp, 0.4)
    face_x(bp, 1)
    rim = flat("hand_rim", [(-8.4, 44.6), (-8.4, 69.6), (-6.0, 77.0), (6.0, 77.0), (8.4, 69.6), (8.4, 44.6), (0, 40.2)], 11.55, M["gold_line"], 0.04)
    inn = flat("hand_in", [(-7.4, 45.2), (-7.4, 69.3), (-5.3, 76.0), (5.3, 76.0), (7.4, 69.3), (7.4, 45.2), (0, 41.6)], 11.57, M["blued"], 0.05)
    ring7 = [(-5.6, 46.5), (-6.4, 61), (-4.6, 70), (0, 73.2), (4.6, 70), (6.4, 61), (5.6, 46.5)]
    face_x([rim, inn] + constellation("g7", ring7, [(i, i + 1) for i in range(6)], 11.66, starm, M["gold_line"], r=1.25, w=0.22, first=1.0), 1)
    bl.lathe("bezel_x", [(7.7, 11.3), (7.9, 12.3), (6.8, 12.9), (5.9, 12.9), (5.7, 11.9)], M["gold"], sides=14, origin=(0, 54, 0), axis="x", caps=(False, False), part="gem_bezels")
    bl.gem("gem_x", 5.8, amber, center=(12.4, 54, 0), depth=0.3, crown=0.18, sides=8, axis="x", part="gems")
    bez = bl.lathe("bezel_z", [(7.0, 12.8), (7.2, 13.9), (6.2, 14.6), (5.5, 14.6), (5.3, 13.6)], M["gold"], sides=14, origin=(0, 52, 0), axis="z", caps=(False, False), part="gem_bezels")
    g = bl.gem("gem_z", 5.4, amber, center=(0, 52, 14.5), depth=0.3, crown=0.18, sides=8, axis="z", part="gems")
    bl.move(bez, scale=(1, 1, -1))
    bl.move(g, scale=(1, 1, -1))
    return done("vassal_gauntlet", out)


# ================================================================ the Whip
def whip(out):
    bl.reset(SAMPLES)
    M = mats()
    violet = gemmat("violet", rgb(140, 60, 250), rgb(170, 100, 255), 0.36)
    vglow = glowmat("violetglow", rgb(185, 130, 255), 1.6)
    lash = bl.mat("lash", rgb(124, 70, 132), rough=0.55, bump="leather", bstr=0.8, bscale=1.4, groove=rgb(30, 14, 40), gamount=0.85, bevel=0.0)
    # ---- handle
    bl.lathe("grip_braid", [(4.5, -2), (5.0, 3), (5.0, 41), (4.5, 46)], M["leather"], sides=10, squash=0.96)
    for i, (y0, y1) in enumerate(((-3.5, -0.4), (12.6, 15.6), (27.4, 30.4), (39.6, 42.6))):
        band("hr%d" % i, y0, y1, 4.6, 5.9, M["gold"], "handle_rings")
    disc = bl.plate("guard_disc", [(16.5 * math.sin(PI * k / 8) * (1.0 if k % 2 == 0 else 0.6), 44.6 + 13.0 * math.cos(PI * k / 8) * (1.0 if k % 2 == 0 else 0.6)) for k in range(16)], -2.2, 2.2, M["gold"])
    bl.bevel(disc, 0.6)
    for s in (1, -1):
        torus("disc_ring%d" % s, 7.6, 0.28, vglow, (0, 44.6, 2.24 * s), "z", n=24, m=4, detail=True)
        for k in range(8):
            a = PI * k / 4
            flat("disc_star%d%d" % (s, k), star_outline(11.2 * math.sin(a), 44.6 + 8.8 * math.cos(a), 1.3), 2.22 * s, vglow, 0.07)
    # ---- pommel: a gold ring holding the violet stone, a drop below
    bl.lathe("pommel", [(6.9, 2.5), (8.3, 3.2), (9.6, 2.3), (9.8, 0), (9.6, -2.3), (8.3, -3.2), (6.9, -2.5), (6.9, 2.5)], M["gold"], sides=18, origin=(0, -6.1, 0), axis="z", caps=(False, False))
    spike("pommel_drop", (0, -15.2, 0), (0, -24, 0), 2.7, M["gold"], sides=8, part="pommel")
    for s in (1, -1):
        g = bl.gem("gems", 6.8, violet, center=(0, -6.1, 3.0), depth=0.5, crown=0.3, sides=8, axis="z")
        if s < 0:
            bl.move(g, scale=(1, 1, -1))
    # ---- the lash: one coil over the guard, then an S up to the barbed tip
    pts = [(0, 47, 0), (-2, 55, 1.5)]
    for k in range(11):
        a = PI + 2.5 * PI * k / 10
        pts.append((10 + 13.5 * math.cos(a), 60 + 50 * k / 10, 2 + 9.5 * math.sin(a)))
    pts += [(3, 122, -9), (-7, 133, -6), (-9, 143, -3.5), (-2, 151, -3.2), (6.4, 158, -3.6)]
    _ob, frames = ptube("coil_and_lash", pts, lambda t: 3.0 - 1.5 * t, lash, sides=8, sub=3)
    for j, i in enumerate(range(5, len(frames) - 3, 6)):                     # gold cuffs along the lash
        p, tan, _n = frames[i]
        r = 3.0 - 1.5 * i / (len(frames) - 1) + 0.55
        ptube("cuff%d" % j, [p - tan * 0.9, p + tan * 0.9], r, M["gold"], sides=8, sub=1, part="coil_and_lash")
    k = 0
    for i in range(8, len(frames) - 4, 6):
        p, _t, nrm = frames[i]
        x0, x1, _y0, _y1, z0, z1 = BOXES["vassal_whip"]
        for d in ((nrm, -nrm) if k % 2 == 0 else (-nrm, nrm)):               # a barb that would leave the weapon's box points the other way, or is left out
            tip = p + d * 6.0
            if x0 + 0.3 < tip.x < x1 - 0.3 and z0 + 0.3 < tip.z < z1 - 0.3:
                spike("barb%d" % k, p + d * 1.2, tip, 1.3, M["gold"], sides=5, part="tip_spikes")
                break
        k += 1
    tc = Vector((6.4, 169, -3.6))
    bl.loft("tip_gem", [[Vector((tc.x + px, y, tc.z + pz)) for px, pz in bl.circle(r, 6)] for y, r in ((161.6, 0.05), (166.5, 4.6), (169.5, 5.6), (173, 4.2), (177.1, 0.05))], violet)
    for k in range(4):
        a = PI / 4 + PI / 2 * k
        dx, dz = math.cos(a), math.sin(a)
        ptube("cage%d" % k, [(tc.x + dx * 2.2, 157.5, tc.z + dz * 2.2), (tc.x + dx * 6.6, 163, tc.z + dz * 6.6), (tc.x + dx * 7.0, 170, tc.z + dz * 7.0), (tc.x + dx * 4.6, 177.5, tc.z + dz * 4.6), (tc.x + dx * 1.2, 182.5, tc.z + dz * 1.2)],
              lambda t: 1.15 - 0.35 * t, M["gold"], sides=6, sub=3, part="tip_cage")
    bl.lathe("cage_base", [(2.4, 154.5), (3.6, 156), (3.6, 158.5), (2.4, 160)], M["gold"], sides=8, origin=(tc.x, 0, tc.z), part="tip_cage")
    bl.loft("stinger", [bl.ring(y, bl.lozenge(w, t), ox=tc.x, oz=tc.z) for y, w, t in ((180, 1.6, 1.6), (185, 3.4, 2.2), (191, 2.2, 1.4), (198, 0.08, 0.05))], M["steel"], part="tip_spikes")
    return done("vassal_whip", out)


# ================================================================ the Projectile (a crystal throwing blade)
def projectile(out):
    bl.reset(SAMPLES)
    M = mats()
    cryst = bl.mat("crystal_teal", rgb(56, 196, 186), metallic=0.6, rough=0.12, emit=rgb(60, 225, 200), strength=0.07, bevel=0.2)
    cyan = gemmat("cyan", rgb(30, 205, 225), rgb(80, 240, 250), 0.4)
    cglow = glowmat("cyanglow", rgb(150, 255, 245), 1.8)
    tmetal = bl.mat("teal_metal", rgb(22, 84, 92), metallic=1.0, rough=0.32, bump="hammer", bstr=0.1, bevel=0.2, groove=rgb(8, 36, 42), gamount=0.5)
    twrap = bl.mat("teal_wrap", rgb(26, 96, 100), rough=0.55, bump="wrap", bstr=0.9, groove=rgb(4, 26, 30), gamount=1.0, bevel=0.0)
    core = bl.mat("core", rgb(20, 96, 108), metallic=0.7, rough=0.22, emit=rgb(60, 215, 205), strength=0.3, bevel=0.0)
    # ---- blade
    st = [(44, 5.0, 3.0), (50, 9.2, 3.5), (58, 9.8, 3.5), (70, 9.2, 3.5), (84, 7.6, 3.3), (97, 5.0, 2.0), (104, 2.6, 1.1), (109, 0.1, 0.06)]
    bl.loft("blade", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.34)) for y, w, t in st], cryst)
    for s in (1, -1):
        flat("core%d" % s, [(-2.6, 56), (2.6, 56), (2.5, 69), (0, 78), (-2.5, 69)], 3.52 * s, core, 0.05)
        bl.text("runes%d" % s, "ᛉᛊᛏ", 3.4, cglow, (0.0, 63.5, 3.6 * s), rot=(0, 0 if s > 0 else 180, 90), spacing=1.35, font=RUNE_FONT)
    # ---- guard: a crescent with its horns along the blade, the stone in its middle
    gpath = bl.smooth_path([(-17.1, 59.8, 0), (-13.5, 47, 0), (-7, 38.6, 0), (0, 35.6, 0), (7, 38.6, 0), (13.5, 47, 0), (17.1, 59.8, 0)], 3)
    bl.sweep("guard", gpath, bl.lozenge(1.0, 1.0, flat=0.3), tmetal, scale=lambda t: (3.2 * math.sin(PI * t) ** 0.6 + 0.25, 3.8 * math.sin(PI * t) ** 0.5 + 0.25))
    for s in (1, -1):
        b1 = torus("bezel_a", 5.7, 0.8, M["gold"], (0, 47.5, 3.7), "z", n=16, m=6, part="gem_bezels")
        g1 = bl.gem("gem_a", 5.0, cyan, center=(0, 47.5, 3.5), depth=0.28, crown=0.16, sides=8, axis="z", part="gem")
        g2 = bl.gem("gem_b", 5.4, cyan, center=(0, 6, 1.6), depth=0.45, crown=0.28, sides=8, axis="z", part="gem")
        if s < 0:
            for o in (b1, g1, g2):
                bl.move(o, scale=(1, 1, -1))
    # ---- grip, the ring pommel, the cord with its bead, two floating shards
    bl.lathe("grip", [(3.2, 15), (3.7, 19), (3.7, 37), (3.2, 41)], twrap, sides=10)
    torus("pommel_ring", 8.2, 2.6, tmetal, (0, 6, 0), "z", n=24, m=8)
    ptube("cord_loop", [(-4.6, 1.0, 0), (-8, -6, 1.2), (-9.8, -13, 0), (-8.4, -18, -1.0), (-6, -20.5, 0)], 0.9, twrap, sides=6, sub=3, caps=(True, True))
    bl.lathe("cord_bead", [(0.01, -24.633), (1.7, -23.6), (2.2, -22.2), (1.7, -20.8), (0.01, -19.9)], cyan, sides=8, origin=(-6, 0, 0), part="cord_loop")
    for sx in (1, -1):
        bl.loft("shards%d" % sx, [bl.ring(y, bl.circle(r, 6), ox=15.2 * sx) for y, r in ((71.2, 0.05), (78, 2.2), (82, 2.2), (89.8, 0.05))], cyan, part="shards")
    return done("vassal_projectile", out)


BUILDERS = {"hammer": hammer, "claw": claw, "axe": axe, "whip": whip, "staff": staff, "gauntlet": gauntlet, "projectile": projectile}


def main(argv):
    global SAMPLES
    only = trial = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
        if a == "--fast":
            SAMPLES = 6
            FAST.update(bake=512)
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, "vassal_" + name + "_preview.png"))
        fn(out)


if __name__ == "__main__":
    main(sys.argv[1:])

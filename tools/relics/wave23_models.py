"""Relics of Many Worlds, part 4, wave 23: the nine hand-held relics as 3D models, built with the owner's model method (tools/relics/mesh_lib.py).

    python3 -B tools/relics/wave23_models.py [id ...]        (no ids = all nine; idempotent, deterministic)
    W23_PREVIEW=<dir> python3 -B tools/relics/wave23_models.py divine_mercy      (also renders the preview beside the card)

Run tools/relics/wave23_icons.py first: every mesh is placed on its 16 x 16 sprite (the sprite stays the inventory icon).
Per id it writes assets/nusmp/weapon_meshes/<id>.txt (smoothed per-vertex "qn" normals), textures/entity/weapon/<id>.png and <id>_glow.png (one 512 px skin
each) and, for the four longbows and the Warlute, the draw states <id>_pull1..3 (and <id>_charged for the crossbow) through the core's own state rig
(tools/relics/ranged_meshes.py moves the limbs and the string of the finished rest mesh; the arrow of a bow rides the nock).
Budget: 8 to 14 named parts and at most 2800 quads per mesh.

The method, in order, for every weapon (each build_<id> below follows it):
  0 target decided: the card's art and the printed text.
  1 measure the reference: pixels along one known length -> centimetres; every proportion is a NAMED CONSTANT with a comment saying where it came from.
  2 geometry from real cross-sections (loft / sweep / lathe), never a box or a four-sided frustum for a long form.
  3 the small raised parts are the detail: each is its own named Part with its own constants.
  4 normals smoothed by angle per part, UVs projected on the dominant axis (mesh_lib.Atlas.layout), one skin per weapon.
  5 painted material plus a second additive glow skin.
  6 rendered beside the reference (W23_PREVIEW) and corrected: silhouette, then sections and bevels, then the raised parts, then the texture.

Axes. Bows and shields stand upright on their icon (placed with fit=True): y is the long axis (a bow's limbs), x points to the target (the belly of a bow
is at +x, its string at x = 0, the archer's side is -x), z is the thickness. A shield's face looks toward +z and its handle is behind (-z). The Warlute is
built like the mod's other crossbows: y along the instrument, the head (muzzle) at +y, the prod across x, the face of the body toward +z.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
os.environ["W1_PREVIEW"] = os.environ.get("W23_PREVIEW", "")             # wave1_models' finish() / reference() read this one
import wave1_models as w                                                # noqa: E402
import mesh_lib as ml                                                   # noqa: E402
import ranged_meshes as rm                                              # noqa: E402

CARDS = os.environ.get("RELICS4", "/tmp/claude-0/-home-user-Black-Clover-Tensura-Addon/2c8ee7c0-aec7-5d43-b651-54cec528bb71/scratchpad/relics4")
w.ZIP = CARDS                                                           # the cards of this set: <CARDS>/img/<file>

V = np.array


def add(m, name, material, **kw):
    return w.add(m, name, material, **kw)


def glow_all(atlas, part, rgb, strength=0.5, core=0.3):
    w.paint_part(atlas, part, lambda c: w.set_glow(c, rgb, strength, core=core))


def unit(v):
    v = V(v, float)
    return v / max(float(np.linalg.norm(v)), 1e-9)


# ================================================================ bows: shared geometry
def bow_x(y, half, belly, power=1.8):
    """The stave's centre line: x at height y (the grip at x = belly, the tips at x = 0, where the string is)."""
    return belly * (1.0 - (abs(y) / half) ** power)


def limb_points(sign, half, belly, n=16, y0=1.0, power=1.8, z=0.0, tip_curl=0.0):
    """The centre line of one limb from the grip (y0) out to the tip (y = half), sign = +1 upper / -1 lower; tip_curl bends the last fifth forward (a recurve)."""
    pts = []
    for i in range(n + 1):
        y = y0 + (half - y0) * i / n
        x = bow_x(y, half, belly, power)
        if tip_curl and i > n * 0.8:
            x += tip_curl * ((i - n * 0.8) / (n * 0.2)) ** 2
        pts.append((x, sign * y, z))
    return pts


def tube(m, name, material, pts, r0, r1=None, sides=8, thick=1.0, base=None, glow=False, smooth=60, end_point=False, wear=0.5):
    """A round-ish tube along a polyline, r0 at the start shrinking to r1 (0 = a point) at the end."""
    p = add(m, name, material, base=base, glow=glow, smooth=smooth, uv_mode="loft", wear=wear)
    r1 = r0 if r1 is None else r1

    def sc(t):
        r = r0 + (r1 - r0) * t
        if end_point and t > 0.97:
            return (0.0, 0.0)
        return (r, r * thick)
    ml.sweep(p, pts, ml.circle(1.0, sides), scales=sc, up=(0, 0, 1))
    return p


def stave(m, name, material, pts, wide0, wide1, thick0, thick1, base=None, sides=8, smooth=55, end_point=True, glow=False, wear=0.5):
    """A bow limb: a flat-oval section whose width (in the bow's plane) and thickness (z) taper from the grip to the tip."""
    p = add(m, name, material, base=base, smooth=smooth, uv_mode="loft", glow=glow, wear=wear)

    def sc(t):
        if end_point and t > 0.985:
            return (0.0, 0.0)
        return (wide0 + (wide1 - wide0) * t, thick0 + (thick1 - thick0) * t)
    ml.sweep(p, pts, ml.ellipse(1.0, 1.0, sides), scales=sc, up=(0, 0, 1))
    return p


def string_part(m, half, radius=0.2, n=12, name="string", x=0.0, z=0.0, base=(222, 214, 196)):
    """The string: a thin tube straight from tip to tip at x (many points, so the states can bend it into the V)."""
    s = add(m, name, "cloth", base=base, wear=0.1, smooth=70, uv_mode="loft")
    pts = [(x, half - 2.0 * half * i / n, z) for i in range(n + 1)]
    ml.sweep(s, pts, ml.circle(1.0, 5), scales=lambda t: (radius, radius))
    return s


def arrow_parts(m, length=72.0, z=2.0, head="steel", head_base=None, fletch=(176, 42, 40), shaft_r=0.42, head_len=5.0, glow_head=False, barbed=False):
    """A nocked arrow lying along +x at height z (y = 0): shaft, head, fletching. Drawn only in the pull states."""
    sh = add(m, "arrow_shaft", "wood", base=(196, 160, 104), smooth=60, uv_mode="loft")
    ml.lathe(sh, "x", (0, 0, z), [(-1.5, shaft_r * 1.5), (0.0, shaft_r), (length - head_len, shaft_r)], sides=6)
    hd = add(m, "arrow_head", head, base=head_base, glow=glow_head, smooth=40)
    ml.lathe(hd, "x", (0, 0, z), [(length - head_len - 0.4, shaft_r * 1.1), (length - head_len * 0.55, shaft_r * 2.1), (length, 0.0)], sides=6, caps=(True, False))
    if barbed:
        for sgn in (-1, 1):
            ml.bar(hd, (length - head_len * 0.9, sgn * shaft_r * 1.8, z), (length - head_len * 1.5, sgn * (shaft_r * 3.4 + 0.4), z), 0.5, 0.2, up=(0, 0, 1), taper=0.8)
    fl = add(m, "arrow_fletch", "cloth", base=fletch, smooth=30)
    for k in range(3):
        a = k * 2 * math.pi / 3
        ml.bar(fl, (1.2, 0.0, z), (11.0, 1.9 * math.cos(a), z + 1.9 * math.sin(a)), 0.55, 0.12, up=(0, 0, 1) if k else (0, 1, 0), taper=3.0, end_scale=0.9)
    return sh, hd, fl


def emit_bow(mesh, rig, item_id, atlas):
    """The draw states of a bow (the core's rig, the same maths as rm.emit_states), except that the arrow rides the nock exactly: the nock moves by
    flex + max_draw (the string's apex), so the arrow moves by the same."""
    mesh.finish()
    paths = {}
    nock_travel = rig.flex + rig.max_draw

    def build(f, with_arrow):
        out = ml.Mesh(mesh.name)
        out.place_cfg, out.glow_cfg = mesh.place_cfg, mesh.glow_cfg
        for p in mesh.parts:
            role = rm._role(p.name)
            if role == "arrow" and not with_arrow:
                continue
            c = rm._copy_part(p)
            if role == "limb":
                c.V = [r for r in rm.displace_limb(rig, np.array(p.V), 0.0, f)]
            elif role == "string":
                c.V = [r for r in rm.displace_string(rig, np.array(p.V), 0.0, f)]
            elif role == "arrow":
                c.V = [r for r in np.atleast_2d(np.array(p.V)) + rig.d() * nock_travel * f]
            out.add(c)
        return out

    path = os.path.join(rm.MESH_DIR, item_id + ".txt")
    build(0.0, False).write_txt(path, atlas, "vertex", comment="rest: ranged_meshes.py")
    paths[""] = path
    for suffix, f, _ in rm.BOW_STATES:
        path = os.path.join(rm.MESH_DIR, item_id + suffix + ".txt")
        build(f, True).write_txt(path, atlas, "vertex", comment="state %s f=%.2f: ranged_meshes.py" % (suffix, f))
        paths[suffix] = path
    return paths


def finish_bow(item_id, m, atlas, half, belly, flex, max_draw, ref=None, real_m=None):
    """Places the mesh on its sprite, writes the rest mesh and the three draw states and the skins, optionally renders the preview."""
    icon = os.path.join(w.ICON_DIR, item_id + ".png")
    if not os.path.exists(icon):
        raise SystemExit("run tools/relics/wave23_icons.py first: " + icon + " is missing")
    m.place_on_sprite(icon, fit=True)
    m.set_glow("FFFFFF", 0.14, 0.45, 1.0)
    rig = rm.Rig(tip_a=(0.0, half, 0.0), tip_b=(0.0, -half, 0.0), draw_dir=(-1, 0, 0), max_draw=max_draw, flex=flex, grip=(belly, 0.0, 0.0), limb_axis=(0, 1, 0), limb_span=half)
    paths = emit_bow(m, rig, item_id, atlas)
    atlas.save(item_id)
    st = m.stats(atlas)
    print(f"{item_id}: {len(m.parts)} parts, quads {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px; states {sorted(paths)}")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        ml.preview(m, ref, os.path.join(prev, item_id + ".png"), atlas=atlas, normals="vertex")
        preview_state(item_id, "_pull3", os.path.join(prev, item_id + "_pull3.png"))
    return st


def finish_still(item_id, m, atlas, ref=None, fit=True, real_m=None):
    """A mesh with no states (the shields)."""
    icon = os.path.join(w.ICON_DIR, item_id + ".png")
    if not os.path.exists(icon):
        raise SystemExit("run tools/relics/wave23_icons.py first: " + icon + " is missing")
    m.place_on_sprite(icon, real_m=real_m, fit=fit)
    m.set_glow("FFFFFF", 0.14, 0.45, 1.0)
    os.makedirs(w.MESH_DIR, exist_ok=True)
    n = m.write_txt(os.path.join(w.MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment="generated by tools/relics/wave23_models.py")
    atlas.save(item_id)
    st = m.stats(atlas)
    print(f"{item_id}: {len(m.parts)} parts, {n} quads {st['quads']}, length {st['length']:.1f}, atlas {atlas.size}px at {atlas.ppu:.1f} px/cm")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        ml.preview(m, ref, os.path.join(prev, item_id + ".png"), atlas=atlas, normals="vertex")
    return st


def preview_state(item_id, suffix, out_png):
    """Renders a state mesh (<id><suffix>.txt) beside nothing: the preview finds the skin by the mesh's file name, so the skins are copied for a moment."""
    import shutil
    td = os.path.join(ml.ASSETS, "textures", "entity", "weapon")
    made = []
    for ext in (".png", "_glow.png"):
        src, dst = os.path.join(td, item_id + ext), os.path.join(td, item_id + suffix + ext)
        if os.path.exists(src):
            shutil.copy(src, dst)
            made.append(dst)
    try:
        ml.preview(os.path.join(rm.MESH_DIR, item_id + suffix + ".txt"), None, out_png, normals="vertex")
    finally:
        for f in made:
            os.remove(f)


def reference(card_file, box, out_name):
    return w.reference(card_file, box, out_name)


def lay(m):
    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    return atlas


# ================================================================ Divine Mercy
def build_divine_mercy():
    """Divine Mercy (card: legendary longbow): the limbs are two large golden-orange feathered angel wings with red-tipped feathers, joined at a small
    ring grip with red ribbons, strung with a thin string.

    Reference: the card, 736 x 736: the wing tips are 595 px apart (60, 300) to (650, 380); a longbow of this kind is about 150 cm tip to tip, so
    K = 0.25 cm per px. Each wing is ~290 px long (74 cm along the spine), the feathers are 90 to 150 px (22 to 37 cm).
    """
    HALF = 74.0                  # half the tip-to-tip length: the card, 295 px per wing at 0.25 cm / px
    BELLY = 24.0                 # how far the grip stands in front of the string line (a longbow drawn at 70 cm)
    FEATHERS = 9                 # primary feathers per wing (the card shows 8 to 10)
    m = ml.Mesh("divine_mercy")
    upper = limb_points(1, HALF, BELLY, n=18, y0=3.0, power=2.1)
    lower = limb_points(-1, HALF, BELLY, n=18, y0=3.0, power=2.1)
    stave(m, "limb_wing_upper", "gold", upper, 1.7, 0.5, 1.0, 0.45, base=(226, 150, 52))
    stave(m, "limb_wing_lower", "gold", lower, 1.7, 0.5, 1.0, 0.45, base=(226, 150, 52))
    gold = add(m, "limb_feathers", "gold", base=(236, 170, 70), smooth=25, wear=0.3)
    cov = add(m, "limb_coverts", "gold", base=(250, 206, 112), smooth=25, wear=0.2)
    red = add(m, "limb_feather_tips", "cloth", base=(190, 40, 34), smooth=25, wear=0.2)
    for pts, sgn in ((upper, 1), (lower, -1)):
        n = len(pts) - 1
        for i in range(FEATHERS):
            t = 0.10 + 0.84 * i / (FEATHERS - 1)
            k = min(n, int(round(t * n)))
            root = V(pts[k])
            tang = unit(V(pts[min(n, k + 1)]) - V(pts[max(0, k - 1)]))
            ang = math.radians(28 + 14 * (1 - t))                       # inner feathers fan out further back
            d = unit(tang * math.cos(ang) + V((-1.0, 0.0, 0.0)) * math.sin(ang))
            length = 17.0 + 24.0 * math.sin(math.pi * min(1.0, t * 1.05)) + (10.0 if i == FEATHERS - 1 else 0.0)
            z = 0.22 * ((i % 3) - 1)
            tip = root + d * length + V((0, 0, z))
            wd = 3.0 - 0.9 * t
            ml.bar(gold, root + V((0, 0, z)), tip, wd, 0.16, up=(0, 0, 1), taper=length * 0.6, end_scale=0.08)
            if i % 2 == 0 or i == FEATHERS - 1:                         # the red-tipped feathers
                ml.bar(red, root + d * length * 0.7 + V((0, 0, z + 0.2)), tip + V((0, 0, 0.2)), wd * 0.8, 0.12, up=(0, 0, 1), taper=length * 0.22, end_scale=0.08)
        for i in range(5):                                              # shorter covert feathers over the shoulder
            k = int(n * (0.05 + 0.10 * i))
            root = V(pts[k])
            d = unit(V((-0.8, sgn * 0.55, 0.0)))
            ml.bar(cov, root + V((0, 0, 0.34)), root + d * (9.0 + 2.2 * i) + V((0, 0, 0.34)), 2.4, 0.14, up=(0, 0, 1), taper=5.0, end_scale=0.1)
    gr = add(m, "grip", "gold", base=(214, 164, 70), smooth=50, uv_mode="loft")                    # the pole of the grip
    ml.loft(gr, "y", (BELLY, 0, 0), [(-5.0, 1.0, 1.0), (-3.5, 1.7, 1.7), (3.5, 1.7, 1.7), (5.0, 1.0, 1.0)], ml.circle(1.0, 10))
    rg = add(m, "grip_ring", "silver", base=(206, 208, 222), smooth=60, uv_mode="loft")            # the small ring grip, in the bow's plane
    ml.sweep(rg, [(BELLY + 4.4 * math.cos(a), 4.4 * math.sin(a), 0.0) for a in np.linspace(0, 2 * math.pi, 17)], ml.circle(1.0, 6), scales=lambda t: (0.7, 0.7), caps=(False, False), up=(0, 0, 1))
    rb = add(m, "grip_ribbons", "cloth", base=(176, 36, 56), smooth=30, wear=0.2)
    for sgn in (-1, 1):
        for dz, ln in ((0.6, 14.0), (-0.6, 10.0)):
            ml.bar(rb, (BELLY - 1.2, sgn * 2.4, dz), (BELLY - 9.0, sgn * (4.0 + ln * 0.5), dz + 1.0), 2.0, 0.15, up=(0, 0, 1), taper=ln * 0.5, end_scale=0.2)
    string_part(m, HALF, 0.2)
    arrow_parts(m, length=70.0, z=2.3, head="gold", head_base=(240, 210, 110), fletch=(214, 190, 120))
    atlas = lay(m)
    for nm in ("limb_feather_tips",):
        glow_all(atlas, m.part(nm), (255, 90, 60), 0.35, 0.2)
    for nm in ("limb_feathers", "limb_coverts", "grip_ring"):
        glow_all(atlas, m.part(nm), (255, 214, 120), 0.22, 0.4)
    ref = reference("img/784541197571846933.jpg", (40, 120, 680, 420), "divine_mercy")
    finish_bow("divine_mercy", m, atlas, HALF, BELLY, flex=8.0, max_draw=26.0, ref=ref)


# ================================================================ more shared geometry: helices, shield outlines, straps
def helix_around(pts, r, turns, phase=0.0):
    """A polyline that spirals round another polyline (a vine round a stave): in the bow's plane the offset is along the normal, out of the plane along z."""
    P = [V(p, float) for p in pts]
    n = len(P)
    out = []
    for k in range(n):
        tang = unit(P[min(k + 1, n - 1)] - P[max(k - 1, 0)])
        n1 = unit(np.cross(tang, V((0, 0, 1.0))))
        a = phase + 2 * math.pi * turns * k / max(1, n - 1)
        out.append(tuple(P[k] + n1 * r * math.cos(a) + V((0, 0, 1.0)) * r * math.sin(a)))
    return out


def point_on(pts, t):
    """The point and the unit tangent at fraction t of a polyline."""
    n = len(pts) - 1
    k = max(0, min(n - 1, int(t * n)))
    f = t * n - k
    a, b = V(pts[k], float), V(pts[k + 1], float)
    return a + (b - a) * f, unit(b - a)


def kite_outline(half_w, half_h, n=14, round_bottom=0.0, top_dip=0.0):
    """A kite shield's outline, counter-clockwise from the top left: straight shoulders, sides tapering to a point (a rounded one if round_bottom > 0)."""
    right = []
    for i in range(n + 1):
        s = i / n                                                   # 0 top .. 1 bottom
        hw = half_w * (1.0 - max(0.0, (s - 0.34) / 0.66) ** 1.5 * (1.0 - round_bottom))
        if s > 0.995:
            hw = half_w * round_bottom * 0.1
        right.append((hw, half_h - 2 * half_h * s))
    left = [(-x, y) for (x, y) in reversed(right)]
    pts = left[:-1] + [(0.0, -half_h)] + right[::-1][1:]            # top-left .. bottom point .. top-right
    top = [(-half_w, half_h - top_dip), (0.0, half_h - 0.0), (half_w, half_h - top_dip)]
    pts = [pt for pt in pts if pt[1] < half_h - 1e-6 or abs(pt[0]) < 1e-6]
    return pts


def shield_ring(half_w, half_h, kind, n=28):
    """Counter-clockwise outline (list of (x, y), n points) of a shield: 'kite', 'heater' (tapered, rounded bottom) or 'round'."""
    pts = []
    for i in range(n):
        a = 2 * math.pi * i / n + math.pi / 2                       # start at the top, go counter-clockwise (to the left first)
        c, s = math.cos(a), math.sin(a)
        if kind == "round":
            pts.append((half_w * c, half_h * s))
        elif kind == "heater":                                      # wide shoulders, sides closing in, a rounded point
            k = (1 - s) / 2                                         # 0 top .. 1 bottom
            wdt = 1.0 - 0.42 * max(0.0, k - 0.15) ** 1.2 * 1.6
            pts.append((half_w * c * max(0.12, wdt) * (1.0 if s > -0.2 else 1.0 - 0.0), half_h * s))
        else:                                                       # kite: a rounded-square top fading into a point
            k = (1 - s) / 2
            fw = 1.0 - max(0.0, (k - 0.30) / 0.70) ** 1.35
            sq = math.copysign(abs(c) ** 0.55, c)                  # squarish shoulders
            pts.append((half_w * sq * max(fw, 0.0), half_h * s))
    return pts


def poly_ring(points, n=36):
    """A closed polygon (counter-clockwise (x, y)) resampled to n points: a shield outline with real corners."""
    return [tuple(q) for q in ml.resample(points, n)]


KITE = [(-28, 42), (-28, 24), (-18, -14), (0, -42), (18, -14), (28, 24), (28, 42), (0, 40.5)]
HEATER = [(-27, 39), (-31, 34), (-21, -24), (0, -39), (21, -24), (31, 34), (27, 39), (0, 38)]


def shield_body(m, name, material, ring, depth_back=1.8, depth_front=2.4, dome=0.9, base=None, smooth=45, wear=0.5, glow=False):
    """The shield plate: the outline lofted along z (back, edge, domed front), closed at both ends."""
    p = add(m, name, material, base=base, smooth=smooth, wear=wear, glow=glow)
    ml.loft(p, "z", (0, 0, 0), [(-depth_back, 0.96, 0.96), (-depth_back * 0.4, 1.0, 1.0), (depth_front * 0.35, 1.0, 1.0), (depth_front, dome, dome)], ring)
    return p


def rim_loop(m, name, material, ring, r, z=0.0, base=None, scale=1.0, smooth=60, sides=8, wear=0.5, glow=False):
    """A tube round the whole outline (a rim): the loop is closed by repeating the first point."""
    p = add(m, name, material, base=base, smooth=smooth, uv_mode="loft", wear=wear, glow=glow)
    pts = [(x * scale, y * scale, z) for (x, y) in ring] + [(ring[0][0] * scale, ring[0][1] * scale, z)]
    ml.sweep(p, pts, ml.circle(r, sides), caps=(False, False), up=(0, 0, 1))
    return p


def back_straps(m, half_w, half_h, z=-2.6, name="back_straps"):
    """The arm strap and the hand grip behind the shield (a leather band across, a grip bar): where the wielder holds it."""
    p = add(m, name, "leather", base=(92, 58, 36), smooth=45, wear=0.7)
    ml.loft(p, "y", (0, 0, z - 0.8), [-half_h * 0.34, half_h * 0.34], ml.rounded_rect(4.4, 1.2, 0.5, 2))          # the arm strap, running up and down
    ml.loft(p, "x", (0, 0.0, z - 2.2), [-5.0, -3.8, 3.8, 5.0], ml.rounded_rect(1.8, 2.0, 0.6, 2))                     # the grip across it
    ml.box(p, (-5.4, -half_h * 0.34 - 0.4, z - 0.5), (5.4, -half_h * 0.34 + 1.2, z + 0.4))
    ml.box(p, (-5.4, half_h * 0.34 - 1.2, z - 0.5), (5.4, half_h * 0.34 + 0.4, z + 0.4))
    return p


def polyline_bar(part, pts, width, height, up=(0, 0, 1), taper=0.0):
    """A raised flat bar along a polyline: one lozenge bar per segment (cheap, gives the filigree its angular, forged look)."""
    for a, b in zip(pts[:-1], pts[1:]):
        if np.linalg.norm(V(b, float) - V(a, float)) > 0.2:
            ml.bar(part, a, b, width, height, up=up, taper=taper)


# ================================================================ Embrasure
def build_embrasure():
    """Embrasure (card: uncommon longbow): a living-wood longbow of twisted green vine-like limbs sprouting pale leaves with red-pink tips, a red crystal
    grip, a dark gnarled claw at the lower tip.

    Reference: the card, 540 x 1432 (the art is the top 520 px): the bow runs from (62, 500) to (500, 40) on the diagonal, 635 px; a longbow of this kind
    is 150 cm, so K = 0.236 cm per px. The red crystal grip is 22 px = 5.2 cm long ... made 12 cm (grip proportion) to hold in a hand.
    """
    HALF, BELLY = 72.0, 22.0
    m = ml.Mesh("embrasure")
    up = limb_points(1, HALF, BELLY, n=18, y0=5.0, power=1.9, tip_curl=4.0)
    lo = limb_points(-1, HALF, BELLY, n=18, y0=5.0, power=1.9, tip_curl=4.0)
    for pts, nm in ((up, "limb_vine_upper"), (lo, "limb_vine_lower")):
        p = add(m, nm, "wood", base=(98, 142, 70), smooth=55, uv_mode="loft", wear=0.6)

        def sc(t):
            if t > 0.985:
                return (0.0, 0.0)
            gnarl = 1.0 + 0.16 * math.sin(t * 23.0) + 0.1 * math.sin(t * 9.0)       # the knotty vine
            return ((1.9 - 1.1 * t) * gnarl, (1.5 - 0.8 * t) * gnarl)
        ml.sweep(p, pts, ml.ellipse(1.0, 1.0, 8), scales=sc, up=(0, 0, 1))
    tw = add(m, "limb_twine", "wood", base=(70, 112, 52), smooth=60, uv_mode="loft", wear=0.7)              # a second vine wound round each limb
    for pts, ph in ((up, 0.0), (lo, math.pi)):
        path = helix_around(pts, 1.55, 3.2, ph)
        ml.sweep(tw, path, ml.circle(1.0, 5), scales=lambda t: (0.55 * (1 - 0.7 * t) if t < 0.96 else 0.0,) * 2, caps=(True, True))
    lv = add(m, "limb_leaves", "cloth", base=(190, 222, 150), smooth=25, wear=0.2)
    lt = add(m, "limb_leaf_tips", "cloth", base=(226, 84, 108), smooth=25, wear=0.2)
    kn = add(m, "limb_knots", "wood", base=(60, 86, 46), smooth=60, wear=0.8)
    for pts, sgn in ((up, 1), (lo, -1)):
        for i, t in enumerate((0.14, 0.26, 0.40, 0.52, 0.66, 0.78, 0.9)):
            c, tg = point_on(pts, t)
            side = 1.0 if i % 2 == 0 else -1.0
            nrm = unit(np.cross(tg, V((0, 0, 1.0)))) * side
            d = unit(nrm * 0.75 + tg * 0.6 + V((0, 0, 0.35 * (1 if i % 3 == 0 else -1))))
            ln = 14.0 - 3.5 * t + (3.0 if i == 6 else 0.0)
            ml.bar(lv, c, c + d * ln, 5.2 - 1.2 * t, 0.16, up=(0, 0, 1) if abs(d[2]) < 0.8 else (1, 0, 0), taper=ln * 0.6, end_scale=0.08)
            ml.bar(lt, c + d * ln * 0.72, c + d * ln * 1.02, 1.7, 0.12, up=(0, 0, 1) if abs(d[2]) < 0.8 else (1, 0, 0), taper=ln * 0.2, end_scale=0.08)
            w.ellipsoid(kn, tuple(c + nrm * 0.9), 1.5, 1.7, 1.4, sides=8, stacks=5)
    hk = add(m, "limb_tip_hooks", "obsidian", base=(40, 52, 36), smooth=50, uv_mode="loft")                 # the dark claws at the tips
    for sgn in (1, -1):
        tip = V((bow_x(HALF, HALF, BELLY, 1.9) + 4.0, sgn * HALF, 0.0))
        pts = [tuple(tip + V((-0.5 * k, sgn * 1.2 * k, 0.0)) + V((0.9 * k * k * 0.25, 0, 0))) for k in range(0, 6)]
        ml.sweep(hk, pts, ml.circle(1.0, 5), scales=lambda t: (1.1 * (1 - 0.85 * t) if t < 0.96 else 0.0,) * 2)
    gr = add(m, "grip_crystal", "crystal", base=(206, 40, 70), glow=True, smooth=30)                         # the red crystal grip
    ml.loft(gr, "y", (BELLY, 0, 0), [(-6.5, 0.55, 0.55), (-5.5, 1.0, 1.0), (5.5, 1.0, 1.0), (6.5, 0.55, 0.55)], ml.octagon(3.4, 3.4, 1.0))
    gc = add(m, "grip_collars", "bronze", base=(150, 120, 62), smooth=45, wear=0.7)
    for y in (-7.0, 6.3):
        ml.collar(gc, y, 2.3, 0.9, lip=0.25, sides=10, axis="y", origin=(BELLY, 0, 0))
    ml.collar(gc, -0.5, 2.0, 1.0, lip=0.3, sides=10, axis="y", origin=(BELLY, 0, 0))
    string_part(m, HALF, 0.2, base=(214, 206, 184))
    arrow_parts(m, length=68.0, z=2.4, head="steel", fletch=(150, 206, 120))
    atlas = lay(m)
    glow_all(atlas, m.part("grip_crystal"), (255, 70, 110), 0.85, 0.35)
    glow_all(atlas, m.part("limb_leaf_tips"), (255, 110, 140), 0.3, 0.2)
    ref = reference("img/Monsters & More (@monsters-and-more) on Tumblr (2).jpg", (30, 20, 520, 520), "embrasure")
    finish_bow("embrasure", m, atlas, HALF, BELLY, flex=7.0, max_draw=26.0, ref=ref)


# ================================================================ The Dragon's Spirit
def build_dragons_spirit():
    """The Dragon's Spirit (card: rare longbow): a slender recurved longbow of dark carved wood with swirling grey-brown panels, jagged teal dragon-claw
    crystal tips at both limbs, a gold-brown wrapped grip and a thin string.

    Reference: the card, 640 x 828: the bow runs from (484, 70) to (588, 692), 630 px tip to tip along its curve; a longbow of this kind is 160 cm, so
    K = 0.254 cm per px. The grip wrap is 30 px (7.6 cm), the claw crystals 40 to 60 px (10 to 15 cm).
    """
    HALF, BELLY = 78.0, 20.0
    m = ml.Mesh("the_dragons_spirit")
    up = limb_points(1, HALF, BELLY, n=18, y0=4.5, power=1.7, tip_curl=7.0)
    lo = limb_points(-1, HALF, BELLY, n=18, y0=4.5, power=1.7, tip_curl=7.0)
    stave(m, "limb_upper", "wood", up, 2.8, 1.1, 1.3, 0.55, base=(64, 52, 58), wear=0.6)
    stave(m, "limb_lower", "wood", lo, 2.8, 1.1, 1.3, 0.55, base=(64, 52, 58), wear=0.6)
    pn = add(m, "limb_panels", "bronze", base=(118, 100, 104), smooth=40, wear=0.7)                         # the swirling grey-brown panels
    for pts in (up, lo):
        for (t0, t1, ph) in ((0.10, 0.34, 0.0), (0.38, 0.62, 1.7), (0.66, 0.9, 3.1)):
            path = []
            for k in range(9):
                t = t0 + (t1 - t0) * k / 8
                c, tg = point_on(pts, t)
                nrm = unit(np.cross(tg, V((0, 0, 1.0))))
                path.append(tuple(c + nrm * 0.75 * math.sin(ph + k * 1.5) + V((0, 0, 0.68 - 0.3 * t))))
            polyline_bar(pn, path, 0.8, 0.14)
    cl = add(m, "limb_claws", "crystal", base=(60, 188, 180), glow=True, smooth=0)                          # the teal dragon-claw crystals
    for pts, sgn in ((up, 1), (lo, -1)):
        tip = V(pts[-1])
        tg = unit(V(pts[-1]) - V(pts[-3]))
        for (ln, spread, off) in ((16.0, 0.0, 0.0), (10.0, 0.6, 1.0), (9.0, -0.5, -1.0)):
            ax = unit(tg + V((spread, 0, 0)) + V((0, 0, 0.0)))
            ml.gem(cl, tuple(tip - tg * 8.0 + V((0, 0, off * 0.5))), 1.5, long=ln / 3.0, sides=5, axis=tuple(ax))
        for t, side, ln in ((0.32, -1, 9.0), (0.56, 1, 7.0)):                                               # shards standing out of the limb
            c, tgt = point_on(pts, t)
            ml.gem(cl, tuple(c + V((side * 0.6, 0, 1.0))), 1.1, long=ln / 2.4, sides=4, axis=tuple(unit(V((side * 0.9, sgn * 0.35, 0.3)))))
    tc = add(m, "limb_tip_caps", "steel", base=(120, 128, 140), smooth=45, wear=0.7)
    for pts in (up, lo):
        c, tg = point_on(pts, 0.93)
        ml.collar(tc, 0.0, 1.3, 1.2, lip=0.3, sides=8, axis=tuple(tg), origin=tuple(c))
    gr = add(m, "grip", "wood", base=(70, 54, 46), smooth=50, uv_mode="loft")
    ml.loft(gr, "y", (BELLY, 0, 0), [(-6.5, 1.0, 1.0), (-4.5, 1.7, 1.5), (4.5, 1.7, 1.5), (6.5, 1.0, 1.0)], ml.circle(1.0, 10))
    gw = add(m, "grip_wrap", "leather", base=(150, 112, 62), smooth=60, wear=0.6)
    for k in range(7):
        ml.collar(gw, -4.4 + k * 1.3, 1.78, 1.0, lip=0.3, sides=10, axis="y", origin=(BELLY, 0, 0))
    string_part(m, HALF, 0.17, base=(226, 220, 204))
    sv = add(m, "string_serving", "gold", base=(190, 150, 70), smooth=50, wear=0.4)                          # the serving of the string at the nock
    ml.collar(sv, -3.5, 0.36, 7.0, lip=0.0, sides=6, axis="y", origin=(0, 0, 0))
    arrow_parts(m, length=72.0, z=2.2, head="steel", head_base=(190, 220, 222), fletch=(60, 170, 164))
    atlas = lay(m)
    glow_all(atlas, m.part("limb_claws"), (110, 255, 238), 0.85, 0.4)
    w.paint_part(atlas, m.part("limb_panels"), lambda c: w.set_glow(c, (110, 230, 220), 0.18, core=0.2))
    ref = reference("img/784541197571909220.jpg", (380, 40, 640, 720), "dragons_spirit")
    finish_bow("the_dragons_spirit", m, atlas, HALF, BELLY, flex=8.0, max_draw=28.0, ref=ref)


# ================================================================ The Reaver
def build_reaver():
    """The Reaver (card: very rare longbow): a black thorny jagged longbow with red glowing cracks and a red-wrapped grip, a spike at the lower limb, and a
    barbed red-fletched arrow trailing a long iron chain.

    Reference: the card, 736 x 1019: the bow runs from (546, 48) to (548, 740), 692 px tip to tip; a longbow of this kind is 165 cm, so K = 0.238 cm per
    px. The grip wrap is 60 px (14 cm) tall; the lower spike is 110 px (26 cm) long.
    """
    HALF, BELLY = 80.0, 22.0
    m = ml.Mesh("the_reaver")
    up = limb_points(1, HALF, BELLY, n=20, y0=5.0, power=1.8, tip_curl=3.0)
    lo = limb_points(-1, HALF, BELLY, n=20, y0=5.0, power=1.8, tip_curl=3.0)
    for pts, nm in ((up, "limb_upper"), (lo, "limb_lower")):
        p = add(m, nm, "obsidian", base=(30, 26, 36), smooth=50, uv_mode="loft", wear=0.5)

        def sc(t):
            if t > 0.985:
                return (0.0, 0.0)
            ragged = 1.0 + 0.12 * math.sin(t * 31.0)
            return ((3.1 - 2.2 * t) * ragged, (1.5 - 0.85 * t) * ragged)
        ml.sweep(p, pts, ml.ellipse(1.0, 1.0, 8), scales=sc, up=(0, 0, 1))
    th = add(m, "limb_thorns", "obsidian", base=(24, 20, 30), smooth=30, wear=0.5)
    for pts, sgn in ((up, 1), (lo, -1)):
        for i, t in enumerate((0.07, 0.15, 0.24, 0.33, 0.43, 0.53, 0.63, 0.73, 0.83, 0.92)):
            c, tg = point_on(pts, t)
            nrm = unit(np.cross(tg, V((0, 0, 1.0))))
            for side in (1, -1):
                if (i + (0 if side > 0 else 1)) % 3 == 2:
                    continue
                width = 3.1 - 2.2 * t
                ln = (7.5 - 4.0 * t) * (1.2 if side < 0 else 0.9)
                d = unit(nrm * side * 0.8 + tg * 0.55)
                ml.loft(th, tuple(d), tuple(c + nrm * side * width * 0.55), [(0.0, 1.0, 0.8), (ln * 0.55, 0.7, 0.55), (ln, 0.0, 0.0)], ml.circle(0.9, 5), caps=(False, False))
    cr = add(m, "limb_cracks", "obsidian", base=(214, 46, 34), glow=True, smooth=0)
    for pts in (up, lo):
        for (t0, t1, off) in ((0.10, 0.22, 0.2), (0.30, 0.46, -0.3), (0.55, 0.70, 0.3), (0.76, 0.86, -0.2)):
            path = []
            for k in range(5):
                t = t0 + (t1 - t0) * k / 4
                c, tg = point_on(pts, t)
                nrm = unit(np.cross(tg, V((0, 0, 1.0))))
                path.append(tuple(c + nrm * (off + 0.35 * math.sin(k * 2.1)) + V((0, 0, 1.05 - 0.55 * t))))
            polyline_bar(cr, path, 0.55, 0.12)
    sp = add(m, "limb_balance_spike", "steel", base=(70, 60, 72), smooth=40, wear=0.6)                         # the spike at the lower limb
    c, tg = point_on(lo, 0.22)
    ml.loft(sp, (0.45, -0.9, 0.0), tuple(c + V((0.6, 0, 0))), [(-1.0, 2.2, 1.3), (0.0, 2.2, 1.3), (14.0, 1.3, 0.8), (27.0, 0.0, 0.0)], ml.ellipse(1.0, 1.0, 6), up=(0, 0, 1))
    gr = add(m, "grip", "obsidian", base=(36, 30, 40), smooth=50, uv_mode="loft")
    ml.loft(gr, "y", (BELLY, 0, 0), [(-7.5, 0.9, 0.9), (-5.5, 1.8, 1.6), (5.5, 1.8, 1.6), (7.5, 0.9, 0.9)], ml.circle(1.0, 10))
    gw = add(m, "grip_wrap", "cloth", base=(172, 28, 36), smooth=50, wear=0.4)
    for k in range(8):
        ml.collar(gw, -5.4 + k * 1.4, 1.92, 1.1, lip=0.3, sides=10, axis="y", origin=(BELLY, 0, 0))
    string_part(m, HALF, 0.18, base=(150, 138, 136))
    arrow_parts(m, length=74.0, z=2.5, head="steel", head_base=(150, 30, 36), fletch=(190, 34, 40), barbed=True, glow_head=True)
    ch = add(m, "chain_links", "iron", base=(88, 90, 104), smooth=55, wear=0.8)                                # the demonic chain hanging from the grip
    for k in range(9):
        cy = -9.0 - k * 4.6
        cx = BELLY - 2.0 - 1.2 * math.sin(k * 0.7)
        cz = 3.4 + 0.5 * k * 0.0
        plane = k % 2
        pts = []
        for a in np.linspace(0, 2 * math.pi, 9):
            ex, ey = 1.5 * math.cos(a), 3.0 * math.sin(a)
            pts.append((cx + ex, cy + ey, cz) if plane == 0 else (cx, cy + ey, cz + ex))
        ml.sweep(ch, pts, ml.circle(0.5, 5), caps=(False, False), up=(0, 0, 1) if plane == 0 else (1, 0, 0))
    atlas = lay(m)
    glow_all(atlas, m.part("limb_cracks"), (255, 70, 40), 0.95, 0.35)
    glow_all(atlas, m.part("arrow_head"), (255, 60, 50), 0.6, 0.25)
    ref = reference("img/The Reaver - D&D Item.jpg", (230, 30, 736, 780), "reaver")
    finish_bow("the_reaver", m, atlas, HALF, BELLY, flex=7.0, max_draw=28.0, ref=ref)


# ================================================================ Warlute
def build_warlute():
    """Warlute (card: rare lute / crossbow): a lute-shaped weapon with a crimson body and silver filigree plating, a curved blade at the bottom, a spike at
    the end of the body, and a small crossbow prod with a string at the neck's head.

    Reference: the card, 736 x 696: the instrument runs from the spike (70, 222) to the prod's far tip (640, 222): 570 px; a warlute is a short lute-crossbow
    of 110 cm, so K = 0.193 cm per px. The body is 195 px long (38 cm) and 130 px across (25 cm), the neck 330 px (64 cm) ... compressed to fit one hand.
    Axes: y along the instrument, the head at +y, the face of the body toward +z.
    """
    m = ml.Mesh("warlute")
    Y_SPIKE, Y_BODY0, Y_BODY1 = -54.0, -46.0, 4.0           # the spike tip, the body's bottom, the body's join with the neck
    Y_NECK1, Y_PROD = 52.0, 55.0                             # the nut / head, where the prod crosses
    SPAN, FLEX = 24.0, 3.0                                   # half span of the prod, how far each tip comes back at full draw
    Y_NOCK = 8.0
    width = w.hermite([(Y_BODY0, 0.0), (Y_BODY0 + 2.0, 6.5), (-38.0, 12.5), (-28.0, 14.5), (-18.0, 13.5), (-9.0, 9.0), (-2.0, 6.0), (Y_BODY1, 4.4)])
    ys = [Y_BODY0 + (Y_BODY1 - Y_BODY0) * i / 14 for i in range(15)]
    bb = add(m, "body_back", "wood", base=(138, 26, 40), smooth=45, uv_mode="loft", wear=0.4)
    ml.loft(bb, "y", (0, 0, 0), [(y, max(width(y), 0.0), max(width(y), 0.0) * 0.30 + 1.0 if width(y) > 0 else 0.0) for y in ys], ml.rounded_rect(2.0, 2.0, 0.7, 3))
    br = add(m, "body_rim", "silver", base=(200, 204, 218), smooth=55, uv_mode="loft", wear=0.4)              # the silver frame round the body's face
    for sgn in (-1, 1):
        pts = [(sgn * (width(y) * 0.98 + 0.4), y, width(y) * 0.30 + 1.4) for y in ys[1:]]
        ml.sweep(br, pts, ml.circle(0.75, 6), caps=(True, True), up=(0, 0, 1))
    bw = add(m, "body_wings", "silver", base=(184, 190, 206), smooth=40, uv_mode="loft", wear=0.4)             # the swept horn-like plates of the card
    for sgn in (-1, 1):
        pts = [(sgn * (9.0 + 5.0 * math.sin(t * 2.2)), -8.0 + 30.0 * t, 4.8 + 1.1 * t) for t in np.linspace(0, 1, 9)]
        ml.sweep(bw, pts, ml.bevelled(2.0, 1.0, 0.4), scales=lambda t: (3.2 * (1 - 0.8 * t) if t < 0.97 else 0.0, 0.5 if t < 0.97 else 0.0), up=(0, 0, 1))
    bi = add(m, "body_inlay", "cloth", base=(150, 20, 34), smooth=40, wear=0.2)                                # the red inlay with the rose in it
    ml.loft(bi, "y", (0, -22.0, 0), [(-14.0, 0.0, 0.0), (-10.0, 6.0, 0.3), (0.0, 7.5, 0.4), (10.0, 6.0, 0.3), (14.0, 0.0, 0.0)], ml.ellipse(1.0, 1.0, 12), up=(0, 1, 0)) if False else None
    ml.loft(bi, "z", (0, -22.0, 4.6), [(0.0, 1.0, 1.0), (0.5, 0.96, 0.96)], ml.ellipse(8.8, 15.0, 16))
    bf = add(m, "body_filigree", "silver", base=(210, 214, 226), smooth=30, wear=0.3)
    rose = [(5.0 * math.cos(a), -22.0 + 5.0 * math.sin(a), 5.1) for a in np.linspace(0, 2 * math.pi, 13)]
    polyline_bar(bf, rose, 0.9, 0.2)
    for sgn in (-1, 1):
        polyline_bar(bf, [(sgn * 3.0, -33.0, 5.1), (sgn * 6.5, -30.0, 5.1), (sgn * 6.0, -26.0, 5.1)], 0.7, 0.18)
        polyline_bar(bf, [(sgn * 3.0, -11.0, 5.1), (sgn * 6.2, -14.0, 5.1), (sgn * 5.5, -17.5, 5.1)], 0.7, 0.18)
        polyline_bar(bf, [(sgn * 7.0, -22.0, 5.1), (sgn * 10.5, -20.0, 5.1), (sgn * 10.8, -25.0, 5.1)], 0.7, 0.18)
    bl = add(m, "blade", "steel", base=(184, 190, 204), smooth=45, uv_mode="loft", wear=0.5)                    # the curved blade under the body, and the spike
    pts = [(-8.0 - 20.0 * math.sin(t * 1.5), -34.0 - 14.0 * t + 6.0 * (t ** 2), 0.0) for t in np.linspace(0, 1, 12)]
    ml.sweep(bl, pts, ml.bevelled(2.0, 1.0, 0.45), scales=lambda t: (4.2 * (1 - 0.82 * t) if t < 0.975 else 0.0, 0.55 if t < 0.975 else 0.0), up=(0, 0, 1))
    ml.loft(bl, "y", (0, Y_BODY0 + 1.0, 0), [(0.0, 3.0, 3.0), (-3.0, 1.9, 1.9), (Y_SPIKE - Y_BODY0, 0.0, 0.0)], ml.circle(1.0, 8), caps=(True, True))
    nk = add(m, "neck", "wood", base=(52, 40, 46), smooth=45, uv_mode="loft", wear=0.6)
    ml.loft(nk, "y", (0, 0, 0), [(Y_BODY1 - 3.0, 3.0, 2.2), (14.0, 2.5, 1.9), (Y_NECK1 - 4.0, 2.4, 1.8), (Y_NECK1, 3.0, 2.2)], ml.rounded_rect(2.0, 2.0, 0.6, 3))
    fr = add(m, "frets", "silver", base=(196, 200, 214), smooth=40, wear=0.5)
    for k in range(9):
        y = 16.0 + k * 3.7
        ml.bar(fr, (-2.35, y, 2.4), (2.35, y, 2.4), 0.5, 0.22, up=(0, 0, 1))
    ls = add(m, "lutestring", "silver", base=(206, 208, 220), smooth=70, uv_mode="loft", wear=0.2)
    for k in range(4):
        x = -1.2 + 0.8 * k
        ml.sweep(ls, [(x * 1.4, -18.0, 5.4), (x, 10.0, 3.3), (x, Y_NECK1 - 3.0, 2.9)], ml.circle(1.0, 4), scales=lambda t: (0.11, 0.11))
    pm = add(m, "prod_mount", "silver", base=(188, 192, 208), smooth=40, wear=0.5)                              # the head of the neck that carries the prod, and the pegs
    ml.loft(pm, "y", (0, 0, 0), [(Y_NECK1 - 1.0, 3.4, 2.8), (Y_PROD - 1.0, 4.6, 3.0), (Y_PROD + 3.5, 4.0, 2.6), (Y_PROD + 6.0, 0.0, 0.0)], ml.octagon(2.0, 2.0, 0.5))
    for sgn in (-1, 1):
        for k in range(2):
            ml.lathe(pm, "x", (sgn * 3.8, Y_NECK1 - 4.0 + 2.8 * k, 0.5), [(0.0, 0.5), (sgn * 1.6, 0.5), (sgn * 2.4, 0.9)], sides=6)
    lp = add(m, "limb_prod", "obsidian", base=(36, 22, 28), smooth=45, uv_mode="loft", wear=0.5)                 # the small black prod, tips swept forward
    tips = {}
    for sgn in (-1, 1):
        pts = [(sgn * (2.0 + (SPAN - 2.0) * t), Y_PROD + 0.0 + 6.0 * t ** 2.2, 0.0) for t in np.linspace(0, 1, 12)]
        tips[sgn] = pts[-1]
        ml.sweep(lp, pts, ml.bevelled(2.0, 1.0, 0.4), scales=lambda t: (3.2 - 2.1 * t if t < 0.985 else 0.0, 0.85 - 0.4 * t if t < 0.985 else 0.0), up=(0, 0, 1))
    A, B = (tips[-1][0], tips[-1][1] - 0.2, 0.8), (tips[1][0], tips[1][1] - 0.2, 0.8)
    st = add(m, "string", "cloth", base=(214, 210, 220), wear=0.1, smooth=70, uv_mode="loft")
    ml.sweep(st, [(A[0] + (B[0] - A[0]) * i / 10, A[1], 0.8) for i in range(11)], ml.circle(1.0, 4), scales=lambda t: (0.14, 0.14))
    bo = add(m, "bolt", "steel", base=(176, 180, 196), smooth=45)                                                 # the bolt on the strings (drawn in _charged only)
    ml.lathe(bo, "y", (0, 0, 3.5), [(Y_NOCK + 0.5, 0.2), (Y_NOCK + 1.5, 0.28), (Y_NOCK + 40.0, 0.28), (Y_NOCK + 46.0, 0.0)], sides=6)
    ml.lathe(bo, "y", (0, 0, 3.5), [(Y_NOCK + 38.0, 0.0), (Y_NOCK + 38.8, 0.55), (Y_NOCK + 44.0, 0.45), (Y_NOCK + 47.0, 0.0)], sides=6)
    atlas = lay(m)
    w.paint_part(atlas, m.part("body_filigree"), lambda c: w.set_glow(c, (255, 214, 140), 0.45, core=0.3))
    w.paint_part(atlas, m.part("body_rim"), lambda c: w.set_glow(c, (255, 224, 170), 0.18, core=0.3))
    icon = os.path.join(w.ICON_DIR, "warlute.png")
    m.place_on_sprite(icon, real_m=1.1)
    m.set_glow("FFFFFF", 0.14, 0.45, 1.0)
    rig = rm.Rig(tip_a=A, tip_b=B, draw_dir=(0, -1, 0), max_draw=A[1] - Y_NOCK - FLEX, flex=FLEX, grip=(0.0, Y_PROD, 0.0), limb_axis=(1, 0, 0), limb_span=SPAN)
    paths = rm.emit_states(m, rig, "crossbow", "warlute", atlas=atlas, normals="vertex")
    atlas.save("warlute")
    st_ = m.stats(atlas)
    print(f"warlute: {len(m.parts)} parts, quads {st_['quads']}, length {st_['length']:.1f}; states {sorted(paths)}")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        os.makedirs(prev, exist_ok=True)
        ref = reference("img/Picture memes dTzFVcw77_ 2 comments #U2014 iFunny.jpg", (50, 100, 700, 340), "warlute")
        ml.preview(m, ref, os.path.join(prev, "warlute.png"), atlas=atlas, normals="vertex")
        preview_state("warlute", "_charged", os.path.join(prev, "warlute_charged.png"))


# ================================================================ Aegis of the Abyss
def build_aegis():
    """Aegis of the Abyss (card: legendary shield): a dark grey-black tapered shield sculpted as a snarling demonic face with bulging eyes, whose open fanged
    mouth holds a swirling purple starry galaxy.

    Reference: the card, 736 x 736: the shield is 290 px across the shoulders and 340 px tall; a heater shield of this kind is 62 cm across and 78 cm tall, so
    K = 0.215 cm per px. The mouth is 140 px (30 cm) wide.
    """
    W2, H2 = 31.0, 39.0
    m = ml.Mesh("aegis_of_the_abyss")
    ring = poly_ring(HEATER, 36)
    shield_body(m, "body", "obsidian", ring, base=(46, 46, 56), depth_back=2.0, depth_front=2.8, dome=0.93, smooth=40)
    rim_loop(m, "rim", "silver", ring, 1.5, z=0.2, base=(176, 180, 194), scale=1.0)
    z0 = 2.3                                                             # the face is raised from the dome's top
    br = add(m, "brow", "obsidian", base=(58, 58, 70), smooth=50, uv_mode="loft")
    for sgn in (-1, 1):
        pts = [(sgn * (4.0 + 17.0 * t), 22.0 + 4.0 * math.sin(math.pi * t * 0.8) - 5.0 * t * t, z0 + 1.2 + 0.8 * t) for t in np.linspace(0, 1, 9)]
        ml.sweep(br, pts, ml.circle(1.0, 6), scales=lambda t: (2.2 * (1 - 0.5 * t) if t < 0.97 else 0.0,) * 2)
    ey = add(m, "eyes", "silver", base=(206, 206, 216), smooth=60)
    pu = add(m, "pupils", "obsidian", base=(20, 18, 26), smooth=60)
    for sgn in (-1, 1):
        w.ellipsoid(ey, (sgn * 11.5, 16.0, z0 + 2.0), 4.2, 4.0, 3.2, sides=12, stacks=8)
        w.ellipsoid(pu, (sgn * 11.0, 15.2, z0 + 4.8), 1.7, 1.7, 1.0, sides=8, stacks=5)
    ns = add(m, "nose", "obsidian", base=(54, 54, 64), smooth=50, uv_mode="loft")
    ml.loft(ns, "-y", (0, 17.0, z0 + 1.6), [(0.0, 2.2, 1.4), (6.0, 3.6, 2.4), (12.0, 4.8, 2.2), (14.0, 3.4, 1.2)], ml.ellipse(1.0, 1.0, 10))
    ck = add(m, "cheeks", "obsidian", base=(52, 52, 62), smooth=60)
    for sgn in (-1, 1):
        w.ellipsoid(ck, (sgn * 22.0, 2.0, z0 + 0.4), 6.0, 7.5, 2.8, sides=12, stacks=8)
        w.ellipsoid(ck, (sgn * 17.0, 6.5, z0 + 1.2), 4.5, 3.0, 2.3, sides=10, stacks=6)
    mg = add(m, "mouth_galaxy", "crystal", base=(118, 62, 214), glow=True, smooth=20)                  # the mouth: a recessed pane of starry galaxy
    ml.loft(mg, "z", (0, -14.0, z0 - 0.4), [(0.0, 1.0, 1.0), (1.0, 0.98, 0.98)], ml.ellipse(15.5, 10.5, 24))
    lp = add(m, "lips", "obsidian", base=(40, 40, 50), smooth=60, uv_mode="loft")
    ml.sweep(lp, [(17.5 * math.cos(a), -14.0 + 12.4 * math.sin(a), z0 + 1.2) for a in np.linspace(0, 2 * math.pi, 25)], ml.circle(1.0, 8), scales=lambda t: (2.0, 2.0), caps=(False, False), up=(0, 0, 1))
    fg = add(m, "fangs", "bone", base=(232, 232, 240), smooth=30)
    for (x, y, d, ln) in ((-10.0, -5.0, -1, 7.0), (10.0, -5.0, -1, 7.0), (-4.0, -4.2, -1, 4.0), (4.0, -4.2, -1, 4.0), (-7.0, -23.5, 1, 6.0), (7.0, -23.5, 1, 6.0)):
        ml.loft(fg, (0, d, 0), (x, y, z0 + 1.5), [(0.0, 1.4, 0.9), (ln * 0.6, 0.9, 0.6), (ln, 0.0, 0.0)], ml.circle(1.0, 5), caps=(False, False))
    back_straps(m, W2, H2, z=-2.2)
    atlas = lay(m)
    glow_all(atlas, m.part("mouth_galaxy"), (130, 60, 230), 0.45, 0.1)
    w.paint_part(atlas, m.part("mouth_galaxy"), lambda c: _stars(c))
    ref = reference("img/784541197571847566.jpg", (200, 120, 540, 480), "aegis")
    finish_still("aegis_of_the_abyss", m, atlas, ref=ref)


def _stars(c):
    """Little white stars in a purple swirl (the galaxy of the mouth): drawn into the skin and the glow."""
    sh = c.img.shape[:2]
    rng = np.random.default_rng(7)
    n = w.noise2(sh, 6, 3)
    c.img[..., 0] = 70 + 120 * n
    c.img[..., 1] = 30 + 40 * n
    c.img[..., 2] = 150 + 100 * n
    for _ in range(max(6, sh[0] * sh[1] // 160)):
        y, x = int(rng.integers(0, sh[0])), int(rng.integers(0, sh[1]))
        c.img[y, x, :3] = 255
        c.glow[y, x] = (255, 255, 255, 255)


# ================================================================ Celestial Support
def build_celestial():
    """Celestial Support (card: very rare shield): a kite shield of speckled grey stone-like metal with a blue rim, gold corner studs and a blue winged emblem,
    with golden wings glowing behind it.

    Reference: the card, 736 x 736: the shield is 205 px across and 305 px tall; a kite shield is 56 cm across and 84 cm tall, so K = 0.275 cm per px.
    The gold studs are 14 px (3.8 cm); the golden wings reach 190 px (52 cm) out from each side.
    """
    W2, H2 = 28.0, 42.0
    m = ml.Mesh("celestial_support")
    ring = poly_ring(KITE, 36)
    shield_body(m, "body", "stone", ring, base=(150, 156, 170), depth_back=2.0, depth_front=2.6, dome=0.94, smooth=40, wear=0.6)
    rim_loop(m, "rim", "steel", ring, 1.6, z=0.3, base=(56, 116, 206))
    cs = add(m, "corner_studs", "gold", base=(232, 188, 70), smooth=40, wear=0.4)
    for (x, y) in ((-W2 + 4.5, H2 - 4.0), (W2 - 4.5, H2 - 4.0), (0.0, -H2 + 6.5)):
        ml.stud(cs, (x, y, 2.3), 2.3, 1.4, normal=(0, 0, 1), sides=8, dome=0.5)
    rv = add(m, "rim_rivets", "gold", base=(214, 170, 64), smooth=40, wear=0.4)
    for i in range(0, 36, 4):
        x, y = ring[i]
        ml.stud(rv, (x * 0.9, y * 0.9, 2.0), 0.7, 0.6, sides=6, dome=0.5)
    ew = add(m, "emblem_wings", "steel", base=(70, 140, 232), glow=True, smooth=40, uv_mode="loft")           # the blue winged emblem
    for sgn in (-1, 1):
        for k, (ox, top, bow) in enumerate(((3.0, 20.0, 9.0), (4.5, 15.0, 7.0), (6.0, 9.0, 5.0))):
            pts = [(sgn * (ox + bow * math.sin(t * 1.6) * (1.0 + 0.3 * k)), -8.0 + (top + 8.0) * t ** 1.15, 2.7 + 0.1 * k) for t in np.linspace(0, 1, 9)]
            ml.sweep(ew, pts, ml.bevelled(2.0, 1.0, 0.4), scales=lambda t: (2.6 * (1 - 0.85 * t) if t < 0.97 else 0.0, 0.55 if t < 0.97 else 0.0), up=(0, 0, 1))
    cr = add(m, "center_ridge", "silver", base=(206, 210, 224), smooth=40, wear=0.4)
    ml.bar(cr, (0.0, -H2 + 9.0, 2.5), (0.0, H2 - 5.0, 2.7), 1.6, 0.5, up=(0, 0, 1), taper=5.0)
    for sgn in (-1, 1):
        ml.bar(cr, (sgn * 7.0, H2 - 4.0, 2.7), (0.0, H2 - 12.0, 2.7), 1.2, 0.35, up=(0, 0, 1), taper=2.0)           # the V at the top
    lm = add(m, "lower_marks", "steel", base=(70, 140, 232), glow=True, smooth=30)
    for sgn in (-1, 1):
        polyline_bar(lm, [(sgn * 3.0, -26.0, 2.7), (sgn * 7.0, -23.0, 2.7), (sgn * 6.0, -28.0, 2.7), (sgn * 10.0, -25.0, 2.7)], 1.0, 0.2)
    hw = add(m, "halo_wings", "cloth", base=(255, 224, 120), glow=True, smooth=20)                              # the golden wings glowing behind
    for sgn in (-1, 1):
        for (ang, ln, wd) in ((8.0, 46.0, 9.0), (-6.0, 40.0, 8.0), (22.0, 36.0, 7.0), (-18.0, 30.0, 6.5), (36.0, 26.0, 5.5)):
            a = math.radians(ang)
            root = V((sgn * (W2 - 6.0), 6.0, -3.4))
            tip = root + V((sgn * math.cos(a) * ln, math.sin(a) * ln, 0.0))
            ml.bar(hw, tuple(root), tuple(tip), wd, 0.15, up=(0, 0, 1), taper=ln * 0.55, end_scale=0.06)
    back_straps(m, W2, H2, z=-2.2)
    atlas = lay(m)
    glow_all(atlas, m.part("emblem_wings"), (120, 190, 255), 0.55, 0.3)
    glow_all(atlas, m.part("lower_marks"), (120, 190, 255), 0.6, 0.3)
    glow_all(atlas, m.part("halo_wings"), (255, 226, 130), 0.55, 0.4)
    ref = reference("img/784541197571292144.jpg", (80, 120, 660, 480), "celestial")
    finish_still("celestial_support", m, atlas, ref=ref)


# ================================================================ Hero's Shield
def build_heros_shield():
    """Hero's Shield (card: legendary shield): a kite-shaped pale ice-blue shield with a raised silver filigree maze pattern and a glowing blue gem in the
    centre.

    Reference: the card, 540 x 1469 (the art is the top 520 px): the shield is 305 px across and 440 px tall; a kite shield is 58 cm across and 84 cm tall, so
    K = 0.19 cm per px. The gem is 40 px (7.6 cm) tall; the silver bands are 18 px (3.4 cm) wide.
    """
    W2, H2 = 29.0, 42.0
    m = ml.Mesh("heros_shield")
    ring = poly_ring(KITE, 36)
    shield_body(m, "body", "crystal", ring, base=(150, 210, 228), depth_back=2.0, depth_front=2.4, dome=0.95, smooth=45, wear=0.4)
    rim_loop(m, "rim", "silver", ring, 1.3, z=0.2, base=(206, 212, 224))
    z = 2.45
    mc = add(m, "maze_core", "silver", base=(214, 220, 232), smooth=30, wear=0.4)
    ctr = (0.0, 6.0)
    for r, gap in ((8.0, 0.0), (14.0, 2.2), (20.0, 4.4)):                                       # nested squarish loops round the gem, each broken once
        path = []
        for k in range(0, 17):
            a = gap + k * (2 * math.pi - 0.8) / 16 + 0.4
            sq = 1.0 / max(abs(math.cos(a)), abs(math.sin(a))) ** 0.25
            path.append((ctr[0] + r * 0.9 * math.cos(a) * sq, ctr[1] + r * 1.1 * math.sin(a) * sq, z))
        polyline_bar(mc, path, 2.2, 0.55)
    mf = add(m, "maze_frame", "silver", base=(208, 214, 228), smooth=30, wear=0.4)              # the tangle in the top left and the bars down the right side
    polyline_bar(mf, [(-26.0, 36.0, z), (-20.0, 38.0, z), (-14.0, 33.0, z), (-18.0, 28.0, z), (-12.0, 24.0, z)], 2.2, 0.55)
    polyline_bar(mf, [(-8.0, 38.0, z), (-3.0, 33.0, z), (-4.0, 27.0, z)], 2.0, 0.5)
    polyline_bar(mf, [(8.0, 36.0, z), (14.0, 39.0, z), (20.0, 36.0, z), (24.0, 30.0, z)], 2.0, 0.5)
    polyline_bar(mf, [(26.0, 22.0, z), (25.0, 14.0, z), (26.0, 6.0, z)], 2.0, 0.5)
    mu = add(m, "maze_curl", "silver", base=(204, 210, 224), smooth=30, wear=0.4)                 # the curl and the bars along the bottom
    polyline_bar(mu, [(-22.0, -12.0, z), (-14.0, -16.0, z), (-8.0, -14.0, z), (-9.0, -9.0, z)], 2.2, 0.5)
    polyline_bar(mu, [(-18.0, -22.0, z), (-10.0, -26.0, z), (-3.0, -24.0, z), (-2.0, -30.0, z)], 2.0, 0.5)
    polyline_bar(mu, [(8.0, -16.0, z), (14.0, -14.0, z), (18.0, -9.0, z)], 2.0, 0.5)
    polyline_bar(mu, [(6.0, -26.0, z), (2.0, -32.0, z), (0.0, -37.0, z)], 1.8, 0.45)
    gm = add(m, "gem", "crystal", base=(60, 214, 252), glow=True, smooth=0)
    ml.gem(gm, (ctr[0], ctr[1], z + 1.0), 2.8, long=1.9, sides=6, axis="y")
    gs = add(m, "gem_setting", "silver", base=(210, 216, 228), smooth=45, wear=0.4)
    ml.collar(gs, -0.4, 3.9, 1.4, lip=0.4, sides=10, axis="z", origin=(ctr[0], ctr[1], z - 0.2), oval=1.35)
    back_straps(m, W2, H2, z=-2.2)
    atlas = lay(m)
    glow_all(atlas, m.part("gem"), (90, 230, 255), 1.0, 0.45)
    w.paint_part(atlas, m.part("maze_core"), lambda c: w.set_glow(c, (170, 235, 255), 0.18, core=0.3))
    ref = reference("img/784541197571909161.jpg", (100, 30, 450, 500), "heros")
    finish_still("heros_shield", m, atlas, ref=ref)


# ================================================================ Thunder Shield
def build_thunder_shield():
    """Thunder Shield (card, no rarity printed): a round dark steel-blue shield cracked with swirling veins of crackling blue lightning, small rune marks at
    the rim and a face boss at the centre.

    Reference: the card, 628 x 811: the shield is 310 px across; a round shield of this kind is 66 cm across, so K = 0.213 cm per px. The boss is 60 px
    (13 cm) across; the six lightning arms each run about 130 px (28 cm).
    """
    R = 33.0
    m = ml.Mesh("thunder_shield")
    ring = shield_ring(R, R, "round", 24)
    p = add(m, "body", "iron", base=(50, 62, 92), smooth=45, uv_mode=None, wear=0.7)
    ml.loft(p, "z", (0, 0, 0), [(-2.0, 0.97, 0.97), (0.0, 1.0, 1.0), (2.2, 0.9, 0.9), (4.2, 0.6, 0.6), (5.5, 0.0, 0.0)], ring)
    rim_loop(m, "rim", "steel", ring, 1.9, z=0.0, base=(96, 116, 154), wear=0.7)
    ro = add(m, "ring_ridge", "steel", base=(84, 104, 144), smooth=60, uv_mode="loft", wear=0.6)
    ml.sweep(ro, [(0.66 * R * math.cos(a), 0.66 * R * math.sin(a), 2.3) for a in np.linspace(0, 2 * math.pi, 25)], ml.circle(0.7, 5), caps=(False, False), up=(0, 0, 1))
    sw = add(m, "swirl_cracks", "crystal", base=(120, 206, 255), glow=True, smooth=0)
    for k in range(5):
        th0 = k * 2 * math.pi / 5
        path = []
        for i in range(9):
            r = 7.0 + (R * 0.86 - 7.0) * i / 8
            th = th0 + 1.7 * (i / 8) ** 0.9 + 0.09 * math.sin(i * 2.7 + k)
            zz = 2.6 + (3.2 * (1 - (r / R) ** 2)) * 0.9
            path.append((r * math.cos(th), r * math.sin(th), zz + 0.3))
        polyline_bar(sw, path, 1.1, 0.25)
        i = 5                                                                                    # a forked branch
        r = 7.0 + (R * 0.86 - 7.0) * i / 8
        th = th0 + 1.7 * (i / 8) ** 0.9
        polyline_bar(sw, [(r * math.cos(th), r * math.sin(th), 3.2), ((r + 3.5) * math.cos(th - 0.28), (r + 3.5) * math.sin(th - 0.28), 3.0), ((r + 7.0) * math.cos(th - 0.34), (r + 7.0) * math.sin(th - 0.34), 2.8)], 0.7, 0.2)
    bs = add(m, "boss", "steel", base=(88, 100, 132), smooth=50, wear=0.6)
    ml.stud(bs, (0, 0, 4.4), 6.4, 3.0, normal=(0, 0, 1), sides=14, dome=0.5)
    bf = add(m, "boss_face", "obsidian", base=(30, 36, 56), glow=True, smooth=40)
    for sgn in (-1, 1):
        ml.gem(bf, (sgn * 2.4, 1.2, 7.4), 0.9, long=1.2, sides=5, axis="z")
    ml.bar(bf, (-2.6, -2.4, 7.2), (2.6, -2.4, 7.2), 0.9, 0.3, up=(0, 0, 1), taper=0.8)
    ml.bar(bf, (0.0, 0.3, 7.4), (0.0, -1.6, 7.4), 0.8, 0.3, up=(0, 0, 1), taper=0.4)
    rn = add(m, "runes", "crystal", base=(150, 226, 255), glow=True, smooth=0)
    for k in range(5):
        a = math.radians(90 + k * 72)
        cx, cy = 0.8 * R * math.cos(a), 0.8 * R * math.sin(a)
        tx, ty = -math.sin(a), math.cos(a)
        rx, ry = math.cos(a), math.sin(a)
        zr = 2.1 + 3.2 * (1 - 0.64) * 0.9
        polyline_bar(rn, [(cx - tx * 2.0 + rx * 1.4, cy - ty * 2.0 + ry * 1.4, zr), (cx + tx * 2.0 + rx * 1.4, cy + ty * 2.0 + ry * 1.4, zr), (cx - tx * 2.0 - rx * 1.4, cy - ty * 2.0 - ry * 1.4, zr), (cx + tx * 2.0 - rx * 1.4, cy + ty * 2.0 - ry * 1.4, zr)], 0.8, 0.2)
    rs = add(m, "rim_studs", "steel", base=(120, 138, 172), smooth=40, wear=0.6)
    for k in range(8):
        a = 2 * math.pi * k / 8 + 0.26
        ml.stud(rs, (0.93 * R * math.cos(a), 0.93 * R * math.sin(a), 1.3), 0.9, 0.8, sides=6, dome=0.5)
    back_straps(m, R, R, z=-2.2)
    atlas = lay(m)
    glow_all(atlas, m.part("swirl_cracks"), (110, 210, 255), 1.0, 0.5)
    glow_all(atlas, m.part("runes"), (140, 225, 255), 0.9, 0.4)
    glow_all(atlas, m.part("boss_face"), (120, 210, 255), 0.7, 0.3)
    ref = reference("img/784541197571292132.jpg", (150, 180, 500, 500), "thunder")
    finish_still("thunder_shield", m, atlas, ref=ref)


BUILDERS = {"divine_mercy": build_divine_mercy, "embrasure": build_embrasure, "the_dragons_spirit": build_dragons_spirit, "the_reaver": build_reaver,
            "warlute": build_warlute, "aegis_of_the_abyss": build_aegis, "celestial_support": build_celestial, "heros_shield": build_heros_shield,
            "thunder_shield": build_thunder_shield}


def main(only=None):
    for item_id, fn in BUILDERS.items():
        if only and item_id not in only:
            continue
        fn()


if __name__ == "__main__":
    main(sys.argv[1:])

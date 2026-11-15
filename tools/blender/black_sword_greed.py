"""The Black Sword Greed, made in Blender: the eight forms of the relic of Berserk of Gluttony (Fate Graphite Reincarnation).

Run: python tools/blender/black_sword_greed.py [--only sword,bow,scythe,shield,staff,gauntlets,spear,dual_blades] [--trial DIR] [--quick] [--no-icons]

--trial DIR  writes the meshes, skins, icons, model jsons and previews into DIR instead of the mod's assets (for looking before replacing).
--quick      few samples and a 512 px bake (for iterating; the release run uses the defaults of bl_lib).

One item id per form (the form switch swaps the item id), each with its own mesh, skin, glow skin, 16 x 16 inventory sprite and item model:
  black_sword_greed (sword)  _bow  _scythe  _shield  _staff  _gauntlets  _spear  _dual_blades ("Level Zero", the most refined form: a thin crimson edge)
Design language shared by all eight: matte black metal with a faint engraving, crimson (#C8102E) veins and runes that only the glow skin carries
as light, and a recurring maw / eye motif (a ring of fangs closing over a slit eye) at the guard, the head, the emblem. The glow skin carries the
crimson (the game lights the mesh, matte black alone would be lost); the cost in game is only the quads of the mesh (budget 2800 quad lines per mesh
including the glow copies, the runtime cap is 3000); the engraving, the veins and the runes are baked into the skin and cost nothing.
See bl_lib.py for what Blender does here and legendary.py for the weapons this one is built like.
"""
import json
import math
import os
import random
import sys
import tempfile

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
import legendary as lg  # noqa: E402   (crack, jag, spike, rune_font: the vein and rune helpers)
from bl_lib import rgb  # noqa: E402
from mathutils import Vector  # noqa: E402

PI = math.pi
RUNES = lg.RUNES
ASSETS = bl.ASSETS
ROOT = bl.ROOT

# the place line of each form: item-space origin of y = ymin, the axis angle (45 = the sprite diagonal, 225 for a bow, 90 for a shield), item units per mesh unit, ymin
PLACE = {
    "black_sword_greed": (0.14, 0.13, 45.0, 0.013333, 0.0),                  # 135 cm -> 1.8 blocks, a longsword
    "black_sword_greed_bow": (0.92426, 0.92426, 225.0, 0.007123, -84.239),   # the place line of the Legendary Bow: 168 cm tip to tip -> 1.2 blocks
    "black_sword_greed_scythe": (-0.32086, -0.32086, 45.0, 0.0105, 0.0),     # 240 cm -> 2.52 blocks
    "black_sword_greed_shield": (0.5, -0.0075, 90.0, 0.0145, -35.0),         # a shield mesh is upright (90) and centred on the item cube
    "black_sword_greed_staff": (-0.31331, -0.31331, 45.0, 0.008735, -9.0),   # the place line of the Vassal Staff: 2.5 blocks
    "black_sword_greed_gauntlets": (0.20, 0.18, 45.0, 0.0145, 0.0),          # a pair held by the wrists: 0.8 blocks
    "black_sword_greed_spear": (-0.62, -0.62, 45.0, 0.0115, 0.0),            # 290 cm -> 3.3 blocks
    "black_sword_greed_dual_blades": (0.15, 0.13, 45.0, 0.012, 0.0),         # 100 cm -> 1.2 blocks
}
# the `grip` line (share of the length from the butt where the hand holds it) of each form; None = no line (a bow is held at its riser, a shield on the frame)
GRIP = {
    "black_sword_greed": 0.10, "black_sword_greed_bow": None, "black_sword_greed_scythe": 0.13, "black_sword_greed_shield": None, "black_sword_greed_staff": 0.38,
    "black_sword_greed_gauntlets": 0.20, "black_sword_greed_spear": 0.26, "black_sword_greed_dual_blades": 0.12,
}
CLASS = {
    "black_sword_greed": "longsword", "black_sword_greed_bow": "bow", "black_sword_greed_scythe": "scythe", "black_sword_greed_shield": "shield",
    "black_sword_greed_staff": "staff", "black_sword_greed_gauntlets": "handitem", "black_sword_greed_spear": "spear", "black_sword_greed_dual_blades": "shortsword",
}
GLOW = ("FFFFFF", 0.12, 0.5, 1.0)                                               # the pulse of the glow layer; FFFFFF keeps the glow skin's own crimson
SAMPLES = 24


# ================================================================ materials
def metals():
    m = {}
    # matte black with a faint engraving (the hammer bump), lifted enough that the bake's facets and bevels read
    m["black"] = bl.mat("bsg_black", rgb(36, 36, 45), metallic=0.9, rough=0.36, bump="hammer", bscale=0.5, bstr=0.10, bevel=0.2, groove=rgb(14, 14, 20), gamount=0.55)
    m["blade"] = bl.mat("bsg_blade", rgb(50, 50, 62), metallic=1.0, rough=0.30, bump="brushed", bstr=0.12, bevel=0.2, groove=rgb(20, 20, 28), gamount=0.5)
    m["deep"] = bl.mat("bsg_deep", rgb(18, 18, 24), metallic=0.8, rough=0.5, bevel=0.1)
    m["edge"] = bl.mat("bsg_edge", rgb(104, 104, 122), metallic=1.0, rough=0.26, bevel=0.0)       # a polished bake-only edge
    m["inlay"] = bl.mat("bsg_inlay", rgb(12, 12, 17), metallic=1.0, rough=0.42, bump="brushed", bstr=0.1, bevel=0.0)
    m["wrap"] = bl.mat("bsg_wrap", rgb(30, 14, 18), rough=0.62, bump="cross", bscale=1.0, bstr=0.9, groove=rgb(150, 16, 32), gamount=1.0, bevel=0.0)   # black cord, crimson thread
    # the light: the glow skin carries these (#C8102E and a hotter core)
    # the bake writes the linear value as the pixel, so the emission colour is given as the pixel colour itself: #C8102E and a hotter core
    m["vein"] = bl.mat("bsg_vein", (0.02, 0.0, 0.004, 1.0), rough=0.5, emit=(0.78, 0.063, 0.18, 1.0), strength=1.2, bevel=0.0)
    m["hot"] = bl.mat("bsg_hot", (0.04, 0.004, 0.006, 1.0), rough=0.5, emit=(1.0, 0.27, 0.26, 1.0), strength=1.0, bevel=0.0)
    m["pupil"] = bl.mat("bsg_pupil", rgb(3, 2, 4), rough=0.3, bevel=0.0)
    m["eye"] = bl.mat("bsg_eye", rgb(150, 12, 28), metallic=0.3, rough=0.08, emit=(0.78, 0.063, 0.18, 1.0), strength=0.8, bevel=0.05)
    return m


# ================================================================ helpers shared by the eight forms
def frames(path):
    """[(point, normal, tangent)] of a path drawn in the xy plane, the frame bl.sweep uses with up = z (the normal is up x tangent)."""
    P = [Vector(p) for p in path]
    out = []
    for i, p in enumerate(P):
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        out.append((p, Vector((0, 0, 1)).cross(tan).normalized(), tan))
    return out


def fang(name, base, tip, r, mat, part=None, bend=(0, 0, 0), sides=3):
    """A cheap tooth: three sides, two spans (8 quads), thickest at the base; bend pushes the middle sideways (a hooked fang)."""
    b, t = Vector(base), Vector(tip)
    mid = (b + t) * 0.5 + Vector(bend)
    ax = (t - b).normalized()
    up = (0, 0, 1) if abs(ax.z) < 0.9 else (1, 0, 0)
    return bl.sweep(name, [b, mid, t], bl.circle(1.0, sides), mat, up=up, scale=lambda u: r * (1.0 - u) ** 0.8 + 0.05, part=part)


def maw_eye(name, cx, cy, z, r, M, teeth=10, part=None, faces=(1, -1), sock=True):
    """The recurring motif: a ring of fangs closing over a slit eye, lying on a face at height z (the table), radius r. The socket and the fangs are
    geometry (about 25 + 8 per fang quads a face), the iris and the pupil are baked into the skin. faces: which z faces get one."""
    for s in faces:
        if s < 0:
            continue                                                           # built on +z and copied to -z below
        if sock:
            prof = [(r * 1.3, z - 0.3), (r * 1.22, z + 0.9), (r * 0.98, z + 1.35), (r * 0.72, z + 0.55), (r * 0.72, z - 0.3)]
            bl.lathe(name + "_socket", prof, M["deep"], sides=16, axis="z", origin=(cx, cy, 0), caps=(False, False), part=part)
        bl.lathe(name + "_iris", [(0.001, z + 0.2), (r * 0.66, z + 0.2)], M["vein"], sides=20, axis="z", origin=(cx, cy, 0), caps=(False, False), detail=True)
        n = 9
        lens = [(cx + 0.3 * r * math.sin(PI * k / n), cy - r * 0.62 + 1.24 * r * k / n) for k in range(n + 1)]
        lens += [(cx - 0.3 * r * math.sin(PI * k / n), cy - r * 0.62 + 1.24 * r * k / n) for k in range(n - 1, 0, -1)]
        bl.plate(name + "_pupil", lens, z + 0.26, z + 0.34, M["pupil"], detail=True)
        for k in range(teeth):
            a = 2 * PI * (k + 0.5) / teeth
            ln = 0.5 if k % 2 == 0 else 0.34                                   # the fangs closing in, a long and a short one in turn
            R = r * 1.08
            fang("%s_fang%d" % (name, k), (cx + R * math.cos(a), cy + R * math.sin(a), z + 0.7), (cx + R * (1 - ln) * math.cos(a), cy + R * (1 - ln) * math.sin(a), z + 1.9), r * 0.2, M["black"], part=part)
    if -1 in faces:
        for ob in list(bl.bpy.context.scene.collection.objects):
            if ob.name.startswith(name + "_") and ob.type == "MESH":
                c = bl.copy(ob, ob.name + "_back", scale=(1, 1, -1))
                c["part"] = ob["part"]


def veins(prefix, rng, start, heading, length, step, jitter, zfn, inside, s, w, mat, depth=1, spawn=0.3):
    """A jagged vein of crimson light that forks: a bake-only strip following the surface zfn(x, y) (signed z), stopping where inside(x, y) fails."""
    x, y = start
    a = math.radians(heading)
    pts = [(x, y)]
    forks = []
    for i in range(int(length / step)):
        a += math.radians(rng.uniform(-jitter, jitter))
        x, y = x + step * math.cos(a), y + step * math.sin(a)
        if not inside(x, y):
            break
        pts.append((x, y))
        if depth > 0 and i > 0 and rng.random() < spawn:
            forks.append((x, y, a))
    if len(pts) >= 2:
        lg.crack(prefix, pts, zfn, s, w, mat)
    for j, (fx, fy, fa) in enumerate(forks):
        side = rng.choice((-1, 1))
        veins("%sf%d" % (prefix, j), rng, (fx, fy), math.degrees(fa) + side * rng.uniform(28, 52), length * rng.uniform(0.3, 0.5), step, jitter * 1.3, zfn, inside, s, w * 0.7, mat, depth - 1, spawn * 0.8)


def tilt(obs, pivot, deg):
    """Turns objects about the z axis through pivot (x, y)."""
    for o in obs:
        bl.move(o, (-pivot[0], -pivot[1], 0))
        bl.move(o, rot=(0, 0, deg))
        bl.move(o, (pivot[0], pivot[1], 0))


def objs():
    return list(bl.bpy.context.scene.collection.objects)


# ================================================================ finishing: the place line, the grip line, the glow
_TMP = tempfile.mkdtemp(prefix="bsg_")


def done(item_id, out, sc=1.0):
    """bl.finish with this form's place line, then the grip line."""
    hdr = os.path.join(_TMP, item_id + ".txt")
    with open(hdr, "w") as fh:
        fh.write("place %.5f %.5f %.3f %.6f %.3f\n" % PLACE[item_id])
    kw = dict(out)
    res = bl.finish(item_id, glow=GLOW, header_from=hdr, **kw)
    if kw.get("write", True):
        set_grip(os.path.join(kw.get("out_mesh") or bl.MESH_DIR, item_id + ".txt"), GRIP[item_id])
    return res


def set_grip(path, value):
    """Pins (or removes) the mesh's `grip` line: the name-based guess of mesh_lib reads a spear's shaft as its hilt."""
    with open(path) as fh:
        lines = [l for l in fh.read().split("\n") if not l.startswith("grip ")]
    if value is not None:
        i = next(k for k, l in enumerate(lines) if l.startswith("place "))
        lines.insert(i + 1, "grip %.3f" % value)
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines))


# ================================================================ 1. the sword: a big black broadsword (135 cm)
BLADE_ST = [(38.0, 6.6, 1.7), (46.0, 7.7, 1.7), (60.0, 7.5, 1.6), (80.0, 7.0, 1.55), (100.0, 6.4, 1.4), (114.0, 5.6, 1.25), (124.0, 4.5, 1.0), (131.0, 2.6, 0.6), (135.0, 0.15, 0.12)]
TEETH = [54.0 + 8.0 * k for k in range(8)]                                      # barbs on the left edge, hooked back toward the hilt
TOOTH = 2.4


def interp(st, y):
    for (y0, w0, t0), (y1, w1, t1) in zip(st, st[1:]):
        if y0 <= y <= y1:
            k = (y - y0) / (y1 - y0)
            return w0 + (w1 - w0) * k, t0 + (t1 - t0) * k
    return st[-1][1], st[-1][2]


def tooth(y):
    for y0 in TEETH:
        if y0 <= y < y0 + 8.0:
            return TOOTH * (1.0 - (y - y0) / 8.0)
    return 0.0


def hexsec(wl, wr, t, k=0.3):
    return [(wr, 0), (wr * k, t), (-wl * k, t), (-wl, 0), (-wl * k, -t), (wr * k, -t)]


def sword(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed"
    # ---- blade: a broad hexagonal section, flared shoulders, barbs on the left edge, a long point
    ys = sorted(set([s[0] for s in BLADE_ST] + [y for y0 in TEETH for y in (y0 - 0.01, y0)]))
    rings = []
    for y in ys:
        w, t = interp(BLADE_ST, y)
        rings.append(bl.ring(y, hexsec(w + tooth(y), w, t)))
    bl.loft("blade", rings, M["blade"])

    def zfn(s):
        def f(x, y):
            w, t = interp(BLADE_ST, y)
            wx = w + (tooth(y) if x < 0 else 0.0)
            fl = 0.3 * wx
            return s * (t * (1.0 if abs(x) <= fl else max(0.0, (wx - abs(x)) / (wx - fl))) + 0.02)
        return f

    def inside(x, y):
        w, _t = interp(BLADE_ST, y)
        return 44.0 <= y <= 124.0 and (-0.8 * (w + 1.0) if x < 0 else -0.8 * w) <= x <= 0.8 * w
    rng = random.Random(3)
    for s in (1, -1):
        z = zfn(s)
        # the core: a dark inlay over the flat, and the spine of light down it that forks toward the edges like veins
        lg.crack("core%d" % s, [(0.0, 46.0 + 6.4 * i) for i in range(13)], z, s, 2.0, M["inlay"])
        zs = lambda x, y, z=z, s=s: z(x, y) + 0.06 * s                          # noqa: E731  (the light lies over the inlay)
        lg.crack("spine%d" % s, [(0.25 * math.sin(i * 1.3), 44.0 + 8.0 * i) for i in range(11)], zs, s, 0.5, M["vein"])
        for i in range(10):
            y0 = 47.0 + 7.6 * i
            sx = 1 if i % 2 == 0 else -1
            veins("vein%d_%d" % (s, i), rng, (0.2 * sx, y0), 90 - sx * rng.uniform(40, 62), 9.5, 1.6, 22, zs, inside, s, 0.36, M["vein"], depth=1, spawn=0.28)
        bl.text("runes%d" % s, RUNES[:5] if s > 0 else RUNES[5:10], 2.3, M["hot"], (0.0, 40.6, 1.74 * s), rot=(0, 0 if s > 0 else 180, 90), spacing=1.3, font=lg.rune_font())
    # ---- collar
    bl.bevel(bl.loft("blade_collar", [bl.ring(y, bl.rrect(w, 2.6, 0.8)) for y, w in ((34.5, 7.0), (37.0, 8.0), (41.5, 7.6), (43.0, 6.6))], M["black"], part="guard"), 0.25)
    # ---- guard: a boss with the maw, jaws sweeping down toward the hilt and barbs up toward the point
    bl.lathe("guard_boss", [(6.0, -3.0), (6.8, -1.5), (6.8, 1.5), (6.0, 3.0)], M["black"], sides=18, axis="z", origin=(0, 32.0, 0), part="guard")
    jaws = [("jaw_a", [(5.5, 34.5), (12.0, 36.0), (18.0, 34.0), (21.5, 29.0), (21.8, 22.5)], 3.2, 2.2),
            ("jaw_b", [(5.5, 30.5), (10.5, 29.5), (14.5, 26.0), (16.0, 20.5), (15.2, 15.5)], 2.5, 1.8),
            ("jaw_c", [(5.0, 37.0), (9.0, 41.0), (11.0, 46.5), (10.5, 52.0)], 2.0, 1.5)]
    for nm, pts, w, t in jaws:
        path = bl.smooth_path([(x, y, 0) for x, y in pts], 3)
        ob = bl.sweep(nm, path, bl.lozenge(1.0, 1.0, flat=0.3), M["black"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.8 + 0.12, t * (1 - u) ** 0.7 + 0.1), part="guard")
        bl.mirror(ob, "x")
        for s in (1, -1):                                                      # a crimson line along each jaw, on both faces
            v = bl.sweep(nm + "_v%d" % s, path[: len(path) * 3 // 4], [(-0.25, t * s - 0.04), (0.25, t * s - 0.04), (0.25, t * s + 0.06), (-0.25, t * s + 0.06)], M["vein"],
                         scale=lambda u, w=w, t=t: (w * 0.4 * (1 - u) ** 0.8 + 0.08, (1 - 0.7 * u) ** 0.7 + 0.03), detail=True)
            bl.mirror(v, "x")
    maw_eye("maw", 0.0, 32.0, 3.0, 5.0, M, teeth=10, part="guard")
    # ---- grip: black cord with crimson thread, three rings, a pommel with four thorns and a small eye
    bl.lathe("grip", [(2.0, 9.0), (2.25, 14.0), (2.35, 19.0), (2.25, 24.0), (2.0, 29.0)], M["wrap"], sides=10, squash=0.9)
    for i, y in enumerate((9.4, 19.0, 28.4)):
        bl.lathe("ring%d" % i, [(2.1, y - 0.9), (2.9, y - 0.55), (2.9, y + 0.55), (2.1, y + 0.9)], M["black"], sides=10, squash=0.92, part="grip_rings")
    bl.lathe("pommel", [(0.01, 0.0), (2.6, 0.6), (4.2, 2.6), (4.5, 5.0), (3.8, 7.2), (2.4, 8.6), (2.1, 9.4)], M["black"], sides=12)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        fang("pommel_thorn%d" % k, (3.4 * math.cos(a), 4.6, 3.4 * math.sin(a)), (7.4 * math.cos(a), 3.4, 7.4 * math.sin(a)), 1.0, M["black"], part="pommel", bend=(0, 1.0, 0), sides=4)
    for s in (1, -1):
        bl.lathe("pommel_eye%d" % s, [(0.001, 0.0), (1.5, 0.0)], M["hot"], sides=12, axis="z", origin=(0, 4.8, 4.5 * s), caps=(False, False), detail=True)
    return done(iid, out)


# ================================================================ a ribbon of light over a surface
def lay(name, pts3, s, w, mat, taper=0.0, h=0.07):
    """A bake-only ribbon floating a hair above a surface: pts3 are points of the surface (x, y, signed z), s the face it lies on (+1 / -1), w its half width."""
    return bl.sweep(name, pts3, [(-1.0, 0.02), (1.0, 0.02), (1.0, h), (-1.0, h)], mat, up=(0, 0, s), scale=lambda t: (w * (1.0 - taper * t) + 0.03, 1.0), detail=True)


def flame_mat(y0, y1, name="bsg_flame"):
    """Black fire: matte black that glows crimson from y0 up to y1 (the emission follows the height in mesh space and a noise, so the flame is lit in licks)."""
    m = bl.mat(name, rgb(14, 14, 20), metallic=0.5, rough=0.4, bevel=0.1)
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    bs.inputs["Emission Color"].default_value = (0.78, 0.063, 0.18, 1.0)
    tc = N.new("ShaderNodeTexCoord")
    sep = N.new("ShaderNodeSeparateXYZ")
    L.new(tc.outputs["Object"], sep.inputs[0])
    h = bl._clamp(nt, bl._math(nt, "MULTIPLY_ADD", sep.outputs["Y"], 1.0 / (y1 - y0), -y0 / (y1 - y0)))
    nz = N.new("ShaderNodeTexNoise")
    nz.inputs["Scale"].default_value = 0.35
    nz.inputs["Detail"].default_value = 3.0
    L.new(tc.outputs["Object"], nz.inputs["Vector"])
    lick = bl._clamp(nt, bl._math(nt, "MULTIPLY_ADD", nz.outputs["Fac"], 5.0, -1.8))
    L.new(bl._math(nt, "MULTIPLY", bl._math(nt, "POWER", h, 1.3), bl._math(nt, "MULTIPLY_ADD", lick, 0.8, 0.35)), bs.inputs["Emission Strength"])
    m["glow"] = True
    return m


# ================================================================ 2. the bow: a black recurve with a string of crimson light (168 cm tip to tip)
def bow(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_bow"
    BH, BB, BR, BC = lg.BW_HALF, lg.BW_BELLY, lg.BW_RISER, lg.BW_CURL
    n = 14
    ys = [BR - 1.0 + (BH - (BR - 1.0)) * i / n for i in range(n + 1)]
    path = [(lg.bow_x(y), y, 0.0) for y in ys]
    # ---- limbs: a flat black stave tapering to the tips, a crimson line of light baked on both faces, a polished fin along the front
    up = bl.sweep("limb_upper", path, bl.rrect(1.0, 1.0, 0.32), M["black"], scale=lambda t: (1.45 - 0.75 * t, 3.4 - 2.3 * t))
    lo = bl.mirror(up, "y", "limb_lower")
    lo["part"] = "limb_lower"
    rng = random.Random(11)
    for s in (1, -1):
        zf = lambda t: (3.4 - 2.3 * t) * s                                    # noqa: E731
        lit = bl.sweep("lit%d" % s, path[1:-2], lambda t: [(-0.2, zf((1 + t * 11) / 14) - 0.03), (0.2, zf((1 + t * 11) / 14) - 0.03), (0.2, zf((1 + t * 11) / 14) + 0.07), (-0.2, zf((1 + t * 11) / 14) + 0.07)], M["vein"], detail=True)
        bl.mirror(lit, "y")
        for i in range(5):                                                    # short forks off the line of light
            k = 2 + 2 * i
            x0, y0 = path[k][0], path[k][1]
            zz = lambda x, y, s=s: s * ((3.4 - 2.3 * (y - ys[0]) / (ys[-1] - ys[0])) + 0.02)         # noqa: E731
            veins("bv%d_%d" % (s, i), rng, (x0, y0), 90 + rng.choice((-1, 1)) * rng.uniform(25, 40), 4.5, 1.5, 15, zz, lambda x, y: True, s, 0.22, M["vein"], depth=0)
    fin = bl.sweep("limb_blades", path[1:-1], [(-1.2, 0.3), (-1.2, -0.3), (-3.1, 0.0)], M["blade"], scale=lambda t: (1.0 - 0.45 * t, 1.0))
    bl.mirror(fin, "y")
    tip = bl.sweep("limb_tips", bl.smooth_path([(7.0, 79.2, 0), (8.6, 82.0, 0), (11.8, 83.6, 0), (16.0, 82.2, 0)], 3), bl.circle(1.0, 6), M["black"], scale=lambda t: (1.25 * (1 - t) ** 0.7 + 0.12, 1.3 * (1 - t) ** 0.7 + 0.12))
    bl.move(tip, scale=(1.0, 84.239 / max(v.co.y for v in tip.data.vertices), 1.0))      # the bow's length is the place line's: tip to tip 2 x 84.239
    bl.mirror(tip, "y")
    for i, (u, ln) in enumerate(((0.28, 5.5), (0.5, 6.5), (0.72, 5.0))):    # thorns on the back of each limb, leaning toward the tips
        k = int(u * n)
        th = fang("thorn%d" % i, (path[k][0] + 1.3, path[k][1], 0), (path[k][0] + 1.3 + ln, path[k][1] + ln * 0.9, 0), 1.0, M["black"], part="limb_thorns", sides=4)
        bl.mirror(th, "y")
    # ---- riser: swelling round the maw, the hand wrap below it
    rs = [(-15.0, 2.4, 2.9), (-11.5, 3.2, 3.4), (-9.8, 2.9, 3.2), (2.0, 2.9, 3.2), (4.0, 4.2, 3.4), (7.6, 5.4, 3.4), (11.2, 4.0, 3.4), (15.0, 2.5, 2.9)]
    bl.loft("riser", [bl.ring(y, bl.rrect(w, t, 0.9), ox=lg.bow_x(y)) for y, w, t in rs], M["black"])
    bl.loft("grip_wrap", [bl.ring(y, bl.rrect(w, t, 1.2), ox=BB) for y, w, t in ((-9.6, 3.1, 3.5), (-7.0, 3.5, 3.9), (-1.0, 3.5, 3.9), (1.8, 3.1, 3.5))], M["wrap"], caps=(False, False))
    maw_eye("maw", BB, 7.6, 3.4, 3.9, M, teeth=8, part="riser")
    for k, (a, ln) in enumerate(((-62, 9.0), (-32, 10.2), (0, 9.2), (32, 10.2), (62, 9.0))):       # the crest: five fangs fanning forward from the riser
        c, sn = math.cos(math.radians(a)), math.sin(math.radians(a))
        lg.spike("crest%d" % k, (BB + 4.6 * c, 7.6 + 4.6 * sn, 0), (BB + 4.6 * c + ln * c, 7.6 + (4.6 + ln) * sn, 0), 1.5, M["black"], part="crest")
    # ---- the string: light
    bl.sweep("string", [(BC, BH - 2.0 * BH * i / 12, 0.0) for i in range(13)], bl.circle(0.3, 4), M["vein"])
    return done(iid, out)


# ================================================================ 3. the scythe: two-handed, a crescent that hooks back, a maw at the heel (240 cm)
def scythe(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_scythe"
    shaft = bl.mat("bsg_shaft", rgb(26, 26, 34), metallic=0.6, rough=0.5, bump="grain", bstr=0.4, groove=rgb(10, 10, 14), gamount=0.6, bevel=0.0)
    # ---- shaft: a black snath with two cord-wrapped hand grips, bands, a spiked butt
    bl.lathe("shaft", [(2.6, 16.0), (2.9, 60.0), (3.1, 120.0), (2.9, 180.0), (2.6, 206.0)], shaft, sides=10)
    for i, (y0, y1) in enumerate(((28.0, 78.0), (104.0, 146.0))):
        bl.lathe("grip_wrap%d" % i, [(3.0, y0), (3.35, y0 + 3), (3.35, y1 - 3), (3.0, y1)], M["wrap"], sides=10, part="grip_wrap")
    for i, y in enumerate((24.0, 82.0, 100.0, 150.0, 196.0)):
        bl.lathe("band%d" % i, [(3.0, y - 1.2), (4.2, y - 0.7), (4.2, y + 0.7), (3.0, y + 1.2)], M["black"], sides=10, part="shaft_bands")
    bl.lathe("butt", [(0.01, 0.0), (1.2, 4.0), (2.6, 10.0), (3.6, 15.0), (3.6, 18.0), (2.7, 20.0)], M["black"], sides=10)
    bl.lathe("collar", [(2.8, 198.0), (4.4, 203.0), (6.2, 208.0), (6.4, 212.0), (4.2, 214.0)], M["black"], sides=12)
    bl.lathe("top_spike", [(3.6, 212.0), (3.0, 224.0), (1.6, 234.0), (0.01, 240.0)], M["blade"], sides=8)
    # ---- blade: a great crescent with a back hook
    pts = [(8, 204.0, 0), (-8, 212.0, 0), (-36, 220.0, 0), (-66, 213.0, 0), (-90, 188.0, 0), (-104, 144.0, 0)]
    pth = bl.smooth_path(pts, 5)
    fr = frames(pth)
    N = len(fr)

    def wid(t):
        return 0.25 + 10.2 * (math.sin(PI * min(1.0, t * 1.05 + 0.12)) ** 0.75) * (1 - t ** 3.0)

    def thk(t):
        return 1.6 if t < 0.8 else 1.6 * (1 - (t - 0.8) / 0.2) + 0.12

    unit = [(1, 0), (0.15, 1), (-0.82, 1), (-1, 0.55), (-1, -0.55), (-0.82, -1), (0.15, -1)]
    bl.sweep("blade", pth, unit, M["blade"], scale=lambda t: (wid(t), thk(t)))
    hook = bl.smooth_path([(4, 205, 0), (14, 207.5, 0), (23, 211.5, 0), (29.8, 217.5, 0)], 3)
    bl.sweep("blade_hook", hook[::-1], unit, M["blade"], scale=lambda t: (0.3 + 5.4 * t ** 0.8, 0.3 + 1.2 * t), part="blade")
    for k, f in enumerate((0.16, 0.30, 0.44, 0.58, 0.72)):                      # thorns on the back, leaning toward the tip
        i = int(f * (N - 1))
        p, nr, tan = fr[i]
        w = wid(f)
        fang("thorn%d" % k, p - nr * (w - 0.6), p - nr * (w + 7.0 - 0.6 * k) + tan * (4.5 - 0.3 * k), 1.5, M["black"], part="blade_thorns", sides=4)

    def tnear(x, y):                                                            # the blade parameter nearest a point (for the surface height)
        i = min(range(N), key=lambda j: (fr[j][0].x - x) ** 2 + (fr[j][0].y - y) ** 2)
        return i / (N - 1)
    rng = random.Random(5)
    i1 = int(N * 0.82)
    for s in (1, -1):
        zf = lambda x, y, s=s: s * (thk(tnear(x, y)) + 0.02)                       # noqa: E731
        zs = lambda x, y, s=s: s * (thk(tnear(x, y)) + 0.07)                       # noqa: E731
        core = [fr[i][0] + fr[i][1] * (-0.3 * wid(i / (N - 1))) for i in range(2, i1)]
        lg.crack("core%d" % s, [(c.x, c.y) for c in core], zf, s, 3.0, M["inlay"])
        lg.crack("spine%d" % s, [(c.x, c.y) for c in core], zs, s, 0.5, M["vein"])
        for i in range(5, i1 - 2, 3):                                              # forks off the spine, out toward the edge and the back
            p, nr, tan = fr[i]
            w = wid(i / (N - 1))
            c = p + nr * (-0.3 * w)
            side = 1 if (i // 3) % 2 == 0 else -1
            d = (nr * (side * 0.8) + tan * 0.6).normalized()
            a = [(c + d * (k * 0.28 * w) + nr * (rng.uniform(-0.04, 0.04) * w)) for k in range(4)]
            lg.crack("fork%d_%d" % (s, i), [(q.x, q.y) for q in a], zs, s, 0.36, M["vein"])
        for i in range(4, 9):                                                      # runes down the shaft
            bl.text("srune%d_%d" % (s, i), RUNES[(i * 3 + (0 if s > 0 else 5)) % len(RUNES)], 3.0, M["hot"], (0.0, 150.0 + 6.5 * (i - 4) + 18.0, 3.12 * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, font=lg.rune_font())
    # ---- the heel: a boss with the maw
    bl.lathe("heel_boss", [(7.4, -3.4), (8.0, -1.8), (8.0, 1.8), (7.4, 3.4)], M["black"], sides=18, axis="z", origin=(-2.0, 207.0, 0), part="heel")
    maw_eye("maw", -2.0, 207.0, 3.4, 5.4, M, teeth=10, part="heel")
    return done(iid, out)


# ================================================================ 4. the shield: a black kite shield with a maw emblem (70 cm tall, face +z, straps -z)
KITE = [(0.0, 31.0), (6.0, 33.8), (14.0, 35.0), (20.5, 33.0), (23.4, 26.5), (23.0, 13.0), (19.5, -3.0), (12.0, -20.5), (0.0, -35.0)]


def shield(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_shield"
    half = bl.smooth_path([(x, y, 0) for x, y in KITE], 3)
    outline = [(p.x, p.y) for p in half] + [(-p.x, p.y) for p in reversed(half[1:-1])]       # the kite, once round
    n = len(outline)
    C = (0.0, 2.0)

    def zf(r):
        return 2.0 + 4.6 * (1.0 - r * r)

    def zb(r):
        return -1.6 + 3.0 * (1.0 - r * r)

    def at(k, r, front=True):
        """The point of the surface at outline index k (a float) and radius share r."""
        k %= n
        i = int(k)
        f = k - i
        x = outline[i][0] + (outline[(i + 1) % n][0] - outline[i][0]) * f
        y = outline[i][1] + (outline[(i + 1) % n][1] - outline[i][1]) * f
        return Vector((C[0] + (x - C[0]) * r, C[1] + (y - C[1]) * r, zf(r) if front else zb(r)))

    def ringp(r, front=True):
        return [at(k, r, front) for k in range(n)]
    rf = [0.04, 0.22, 0.42, 0.62, 0.80, 0.92, 1.0]
    rb = [0.93, 0.8, 0.6, 0.38, 0.04]
    rings = [ringp(r) for r in rf] + [ringp(1.0, False)] + [ringp(r, False) for r in rb]
    bl.loft("face", rings, M["black"])
    # ---- the rim: a raised black band round the edge
    rim = [[at(k, r, True) + Vector((0, 0, dz)) for k in range(n)] for r, dz in ((0.9, 0.0), (0.95, 0.9), (1.0, 0.8), (1.02, -0.2))]
    bl.loft("rim", rim + [[at(k, 1.02, False) for k in range(n)]], M["blade"], caps=(False, False), part="rim")
    # ---- the emblem: a boss with the maw
    bl.lathe("boss", [(10.8, 4.6), (10.8, 6.0), (9.5, 7.0), (0.01, 7.0)], M["black"], sides=20, axis="z", origin=(0, 6.0, 0), part="emblem")
    maw_eye("maw", 0.0, 6.0, 7.0, 7.4, M, teeth=14, part="emblem", faces=(1,))
    # ---- straps on the back: a bar between two posts
    bl.lathe("strap_bar", [(1.5, -8.0), (1.5, 8.0)], M["deep"], sides=8, axis="x", origin=(0, 2.0, -4.6), part="straps")
    for sx in (1, -1):
        bl.lathe("strap_post%d" % sx, [(1.6, -4.6), (1.6, 0.8)], M["deep"], sides=8, axis="z", origin=(sx * 8.0, 2.0, 0), part="straps")
    bl.lathe("strap_grip", [(1.9, -4.0), (2.2, -2.0), (2.2, 2.0), (1.9, 4.0)], M["wrap"], sides=8, axis="x", origin=(0, 2.0, -4.6), part="straps")
    # ---- the light: a border, a second line, rays and forks from the emblem, runes below it
    rng = random.Random(9)
    for rr, w in ((0.88, 0.5), (0.80, 0.22)):
        pts = [at(k, rr) + Vector((0, 0, 0.03)) for k in range(n)] + [at(0, rr) + Vector((0, 0, 0.03))]
        lay("border%.2f" % rr, pts, 1, w, M["vein"])
    for j in range(11):
        k0 = (n * (j + 0.5) / 11.0) % n
        d = rng.uniform(-0.6, 0.6)
        pts = []
        for m, r in enumerate((0.36, 0.46, 0.57, 0.67, 0.77, 0.86)):
            pts.append(at(k0 + d * m + rng.uniform(-0.5, 0.5) * (m > 0), r) + Vector((0, 0, 0.03)))
        if abs(pts[0].x) < 12.0 and 0.0 < pts[0].y < 18.0:
            continue                                                            # not across the emblem
        lay("ray%d" % j, pts, 1, 0.3, M["vein"], taper=0.6)
    for i in range(3):
        bl.text("rune%d" % i, RUNES[(i * 7) % len(RUNES)], 4.6, M["hot"], (0.0, -10.0 - 7.2 * i, zf(0.4 + 0.1 * i) + 0.03), rot=(0, 0, 0), extrude=0.04, font=lg.rune_font())
    return done(iid, out)


# ================================================================ 5. the staff: a flame cradle holding black fire (297 cm)
def staff(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_staff"
    shaft = bl.mat("bsg_shaft", rgb(26, 26, 34), metallic=0.6, rough=0.5, bump="grain", bstr=0.4, groove=rgb(10, 10, 14), gamount=0.6, bevel=0.0)
    fire = flame_mat(232.0, 286.0)
    bl.lathe("foot", [(0.01, -9.0), (1.4, -5.0), (2.8, 0.0), (4.6, 3.0), (4.6, 6.0), (3.6, 9.0), (3.4, 12.0)], M["black"], sides=10)
    bl.lathe("shaft", [(3.4, 10.0), (3.7, 100.0), (3.4, 202.0)], shaft, sides=10)
    bl.lathe("grip_wrap", [(3.7, 80.0), (4.15, 83.0), (4.15, 137.0), (3.7, 140.0)], M["wrap"], sides=10)
    for i, y in enumerate((22.0, 76.0, 144.0, 192.0)):
        bl.lathe("band%d" % i, [(3.6, y - 1.4), (4.9, y - 0.8), (4.9, y + 0.8), (3.6, y + 1.4)], M["black"], sides=10, part="shaft_bands")
    # ---- the collar: a block with the maw on both faces, the six prongs of the cradle springing from it
    bl.bevel(bl.loft("collar", [bl.ring(y, bl.rrect(w, t, 1.2)) for y, w, t in ((200.0, 4.6, 4.6), (204.0, 6.4, 5.4), (214.0, 6.6, 5.4), (218.0, 5.0, 4.8))], M["black"]), 0.3)
    maw_eye("maw", 0.0, 209.0, 5.4, 4.0, M, teeth=8, part="collar")
    for k in range(6):
        pr = bl.sweep("prong%d" % k, bl.smooth_path([(3.6, 214.0, 0), (8.5, 222.0, 0), (15.0, 236.0, 0), (18.0, 252.0, 0), (14.5, 268.0, 0), (8.0, 280.0, 0)], 3), bl.lozenge(1.0, 1.0, flat=0.3), M["black"],
                      scale=lambda t: (2.3 * (1 - t) ** 0.6 + 0.2, 2.6 * (1 - t) ** 0.5 + 0.25), part="prongs")
        bl.move(pr, rot=(0, 60.0 * k, 0))
    # ---- the black fire: three flames in the cradle, glowing crimson toward their tips
    def flame(nm, pts, w, twist, part="flame"):
        pth = bl.smooth_path(pts, 3)
        return bl.sweep(nm, pth, bl.lozenge(1.0, 0.8, flat=0.25), fire, scale=lambda t: w * (math.sin(PI * min(1.0, t * 0.9 + 0.08)) ** 0.8) * (1 - t) ** 0.45 + 0.1, twist=lambda t: twist * t, part=part)
    flame("flame_c", [(0, 226, 0), (1.5, 245, 0), (-1.5, 262, 0), (2.0, 276, 0), (0, 286, 0)], 8.0, 2.4)
    for sx, tw in ((1, 2.0), (-1, -2.0)):
        f = flame("flame_s%d" % sx, [(sx * 5, 226, 0), (sx * 9, 238, 0), (sx * 10.5, 250, 0), (sx * 8, 262, 0), (sx * 7, 268, 0)], 5.2, tw)
    for i in range(3):
        for s in (1, -1):
            bl.text("hrune%d_%d" % (s, i), RUNES[(i * 5 + 2 + (0 if s > 0 else 9)) % len(RUNES)], 3.0, M["hot"], (0.0, 152.0 + 8.0 * i, 3.76 * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, font=lg.rune_font())
    return done(iid, out)


# ================================================================ 6. the gauntlets: a pair of clawed gauntlets held by their wrists (62 cm)
def gauntlets(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_gauntlets"
    before = set(o.name for o in objs())
    # ---- one gauntlet, axis y (wrist at 0, fingers toward +y), the back of the hand toward +z
    cuff = [(0.0, 7.0, 6.2), (4.0, 8.2, 7.2), (20.0, 9.4, 8.0), (26.0, 9.0, 7.8)]
    bl.loft("bracer", [bl.ring(y, bl.rrect(w, t, 2.4)) for y, w, t in cuff], M["black"], caps=(False, True), part="bracer")
    fist = [(26.0, 9.0, 7.8), (32.0, 10.2, 8.4), (40.0, 9.8, 8.0), (46.0, 8.0, 6.6), (48.0, 7.0, 5.4)]
    bl.loft("fist", [bl.ring(y, bl.rrect(w, t, 2.6)) for y, w, t in fist], M["black"], part="fist")
    for i, y in enumerate((8.0, 14.0, 20.0)):                                   # three raised rings round the bracer
        rows = []
        for d, e in ((-1.0, 0.0), (-0.4, 0.8), (0.4, 0.8), (1.0, 0.0)):
            w, t = interp_cuff(cuff, y + d)
            rows.append(bl.ring(y + d, bl.rrect(w + e, t + e, 2.4)))
        bl.loft("bracer_ring%d" % i, rows, M["blade"], caps=(False, False), part="bracer_rings")
    bl.loft("knuckles", [bl.ring(y, bl.rrect(w, t, 1.4)) for y, w, t in ((43.0, 9.0, 7.0), (45.0, 9.6, 7.4), (47.4, 9.4, 7.0), (48.6, 8.2, 6.0))], M["blade"], part="knuckles")
    for k, x in enumerate((-5.6, -1.9, 1.9, 5.6)):                              # four claws, curling toward the palm
        L = 12.0 - 1.4 * abs(x) / 5.6
        pts = [(x, 46.5, -0.5), (x * 1.08, 46.5 + L * 0.5, -1.4), (x * 1.14, 46.5 + L * 0.85, -3.6), (x * 1.18, 46.5 + L, -6.4)]
        bl.sweep("claw%d" % k, bl.smooth_path(pts, 3), bl.lozenge(1.0, 1.0), M["blade"], up=(1, 0, 0), scale=lambda t: (1.7 * (1 - t) ** 0.7 + 0.12, 1.7 * (1 - t) ** 0.7 + 0.12), part="claws")
    th = [(10.0, 34.0, 0.0), (13.2, 39.0, -0.5), (15.4, 46.0, -2.0), (15.9, 51.0, -4.0)]
    bl.sweep("thumb_claw", bl.smooth_path(th, 3), bl.lozenge(1.0, 1.0), M["blade"], up=(0, 0, 1), scale=lambda t: (1.6 * (1 - t) ** 0.7 + 0.12, 1.6 * (1 - t) ** 0.7 + 0.12), part="claws")
    maw_eye("maw", 0.0, 36.5, 8.3, 4.2, M, teeth=9, part="fist", faces=(1,))
    rng = random.Random(13)

    def zb(x, y):
        return interp_cuff(cuff + fist[1:], y)[1] + 0.02
    for i, (x0, y0) in enumerate(((-3.0, 22.0), (3.0, 24.0), (-4.5, 12.0), (4.5, 14.0), (0.0, 5.0))):
        veins("gv%d" % i, rng, (x0, y0), 90 + rng.uniform(-20, 20), 10.0, 1.6, 20, zb, lambda x, y: abs(x) < 6.0 and y < 45.0, 1, 0.3, M["vein"], depth=1, spawn=0.25)
    for k in range(3):
        yy = 11.0 + 6.0 * k
        bl.text("grune%d" % k, RUNES[(k * 4) % len(RUNES)], 2.6, M["hot"], (0.0, yy, interp_cuff(cuff, yy)[1] + 0.04), rot=(0, 0, 90), extrude=0.03, font=lg.rune_font())
    # ---- the pair: this one to the right, a mirror of it to the left
    mine = [o for o in objs() if o.name not in before]
    for o in mine:
        bl.copy(o, o.name + "_L", loc=(-12.5, 0, 0), scale=(-1, 1, 1))
        bl.move(o, (12.5, 0, 0))
    return done(iid, out)


def interp_cuff(st, y):
    """(half width, half thickness) of a list of (y, w, t) at y."""
    for (y0, w0, t0), (y1, w1, t1) in zip(st, st[1:]):
        if y0 <= y <= y1:
            k = (y - y0) / (y1 - y0)
            return w0 + (w1 - w0) * k, t0 + (t1 - t0) * k
    return st[-1][1], st[-1][2]


# ================================================================ 7. the spear: a long black lance with a leaf blade (290 cm)
SPEAR_ST = [(238.0, 2.6, 1.4), (246.0, 6.4, 1.7), (258.0, 8.8, 1.8), (270.0, 7.6, 1.6), (282.0, 4.4, 1.1), (290.0, 0.12, 0.1)]


def spear(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_spear"
    shaft = bl.mat("bsg_shaft", rgb(26, 26, 34), metallic=0.6, rough=0.5, bump="grain", bstr=0.4, groove=rgb(10, 10, 14), gamount=0.6, bevel=0.0)
    bl.lathe("butt", [(0.01, 0.0), (1.6, 3.0), (3.0, 9.0), (3.6, 16.0), (3.2, 22.0)], M["black"], sides=10)
    bl.lathe("shaft", [(2.6, 20.0), (2.8, 120.0), (2.6, 226.0)], shaft, sides=10)
    for i, (y0, y1) in enumerate(((50.0, 112.0), (128.0, 170.0))):
        bl.lathe("grip_wrap%d" % i, [(2.9, y0), (3.25, y0 + 3), (3.25, y1 - 3), (2.9, y1)], M["wrap"], sides=10, part="grip_wrap")
    for i, y in enumerate((36.0, 118.0, 176.0, 212.0)):
        bl.lathe("band%d" % i, [(2.9, y - 1.2), (4.1, y - 0.7), (4.1, y + 0.7), (2.9, y + 1.2)], M["black"], sides=10, part="shaft_bands")
    bl.lathe("socket", [(2.8, 218.0), (4.4, 222.0), (4.9, 228.0), (3.8, 236.0), (2.8, 240.0)], M["black"], sides=12)
    ys = sorted(set(s[0] for s in SPEAR_ST) | {250.0, 262.0, 276.0})
    bl.loft("blade", [bl.ring(y, bl.lozenge(interp(SPEAR_ST, y)[0], interp(SPEAR_ST, y)[1], flat=0.3 * interp(SPEAR_ST, y)[0])) for y in ys], M["blade"])
    # the wings: two short barbs sweeping back from the socket
    for nm, pts, w, t in (("wing", [(3.0, 238.0), (9.5, 241.0), (14.5, 236.0), (16.0, 227.0)], 2.6, 1.8),):
        ob = bl.sweep(nm, bl.smooth_path([(x, y, 0) for x, y in pts], 3), bl.lozenge(1.0, 1.0, flat=0.3), M["black"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.8 + 0.12, t * (1 - u) ** 0.7 + 0.1), part="wings")
        bl.mirror(ob, "x")
    bl.lathe("maw_boss", [(5.4, -3.6), (5.9, -1.8), (5.9, 1.8), (5.4, 3.6)], M["black"], sides=16, axis="z", origin=(0, 229.0, 0), part="wings")
    maw_eye("maw", 0.0, 229.0, 3.6, 3.7, M, teeth=8, part="wings")

    def zfn(s):
        def f(x, y):
            w, t = interp(SPEAR_ST, y)
            fl = 0.3 * w
            return s * (t * (1.0 if abs(x) <= fl else max(0.0, (w - abs(x)) / (w - fl))) + 0.02)
        return f
    rng = random.Random(21)
    for s in (1, -1):
        z = zfn(s)
        zs = lambda x, y, z=z, s=s: z(x, y) + 0.06 * s                          # noqa: E731
        lg.crack("core%d" % s, [(0.0, 246.0 + 3.0 * i) for i in range(14)], z, s, 2.2, M["inlay"])
        lg.crack("spine%d" % s, [(0.2 * math.sin(i * 1.4), 244.0 + 3.2 * i) for i in range(14)], zs, s, 0.45, M["vein"])
        for i in range(6):
            y0 = 248.0 + 6.2 * i
            sx = 1 if i % 2 == 0 else -1
            veins("sv%d_%d" % (s, i), rng, (0.2 * sx, y0), 90 - sx * rng.uniform(40, 60), 7.0, 1.4, 20, zs, lambda x, y: 246.0 <= y <= 280.0 and abs(x) < 0.78 * interp(SPEAR_ST, y)[0], s, 0.32, M["vein"], depth=1, spawn=0.28)
        for i in range(5):
            bl.text("srune%d_%d" % (s, i), RUNES[(i * 3 + (0 if s > 0 else 7)) % len(RUNES)], 3.0, M["hot"], (0.0, 182.0 + 6.5 * i, 2.62 * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, font=lg.rune_font())
    return done(iid, out)


# ================================================================ 8. Level Zero: two slim black blades on one hilt, a thin crimson edge (100 cm)
DUAL_ST = [(34.0, 2.7, 1.0), (42.0, 3.2, 1.0), (62.0, 3.0, 0.95), (82.0, 2.5, 0.85), (94.0, 1.6, 0.6), (101.5, 0.1, 0.12)]


def dual_blades(out):
    bl.reset(SAMPLES)
    M = metals()
    iid = "black_sword_greed_dual_blades"
    # ---- one hilt: a pommel with the eye, a cord grip, a boss with the maw and short jaws
    bl.lathe("grip", [(2.0, 8.0), (2.3, 13.0), (2.4, 19.0), (2.2, 25.0), (1.9, 29.0)], M["wrap"], sides=10, squash=0.9)
    for i, y in enumerate((8.4, 18.0, 28.4)):
        bl.lathe("ring%d" % i, [(2.0, y - 0.9), (2.8, y - 0.55), (2.8, y + 0.55), (2.0, y + 0.9)], M["black"], sides=10, squash=0.92, part="grip_rings")
    bl.lathe("pommel", [(0.01, 0.0), (2.4, 0.6), (3.8, 2.4), (4.0, 4.6), (3.4, 6.6), (2.3, 7.8), (2.0, 8.6)], M["black"], sides=12)
    for s in (1, -1):
        bl.lathe("pommel_eye%d" % s, [(0.001, 0.0), (1.4, 0.0)], M["hot"], sides=12, axis="z", origin=(0, 4.4, 4.0 * s), caps=(False, False), detail=True)
    bl.lathe("guard_boss", [(5.8, -3.0), (6.4, -1.5), (6.4, 1.5), (5.8, 3.0)], M["black"], sides=16, axis="z", origin=(0, 31.0, 0), part="guard")
    maw_eye("maw", 0.0, 31.0, 3.0, 4.6, M, teeth=8, part="guard")
    for nm, pts, w, t in (("jaw_a", [(5.0, 32.0), (10.0, 33.0), (14.0, 30.5), (15.0, 25.5)], 2.5, 1.7), ("jaw_b", [(4.5, 29.5), (8.5, 27.5), (11.0, 23.5), (11.5, 19.5)], 2.0, 1.4)):
        ob = bl.sweep(nm, bl.smooth_path([(x, y, 0) for x, y in pts], 3), bl.lozenge(1.0, 1.0, flat=0.3), M["black"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.8 + 0.12, t * (1 - u) ** 0.7 + 0.1), part="guard")
        bl.mirror(ob, "x")
    # ---- one blade, then its twin: slim, single point, a crimson line along each edge and down the middle, the blades leaning apart
    before = set(o.name for o in objs())
    ys = sorted(set(s[0] for s in DUAL_ST) | {50.0, 72.0, 88.0})

    def wt(y):
        return interp(DUAL_ST, y)

    bl.loft("blade", [bl.ring(y, hexsec(wt(y)[0], wt(y)[0], wt(y)[1], 0.25)) for y in ys], M["blade"])

    def z(s, x, y):
        w, t = wt(y)
        fl = 0.25 * w
        return s * (t * (1.0 if abs(x) <= fl else max(0.0, (w - abs(x)) / (w - fl))) + 0.02)
    for s in (1, -1):
        for sx in (1, -1):                                                         # the edge: a thin line of light a little in from each cutting edge
            pts = [(sx * (wt(y)[0] - 0.55), y, z(s, sx * (wt(y)[0] - 0.55), y)) for y in [36.0 + 3.0 * i for i in range(21)] if y < 96.0]
            lay("edge%d%d" % (s, sx), pts, s, 0.26, M["vein"], taper=0.5)
        lg.crack("mid%d" % s, [(0.0, 38.0 + 3.0 * i) for i in range(19)], lambda x, y, s=s: z(s, x, y) + 0.04 * s, s, 0.3, M["vein"])
        bl.text("brune%d" % s, RUNES[:3] if s > 0 else RUNES[3:6], 2.0, M["hot"], (0.0, 37.0, 1.04 * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, font=lg.rune_font())
    bl.bevel(bl.loft("blade_collar", [bl.ring(y, bl.rrect(w, 1.5, 0.5)) for y, w in ((32.0, 3.2), (33.5, 3.9), (36.0, 3.7), (37.0, 3.0))], M["black"], part="guard"), 0.2)
    mine = [o for o in objs() if o.name not in before]
    for o in mine:
        bl.move(o, (3.4, 0, 0))
    tilt(mine, (3.4, 31.0), -6.0)
    for o in mine:
        c = bl.mirror(o, "x")
        c["part"] = o["part"]
    return done(iid, out)


# ================================================================ the builders, in the order of the release
BUILDERS = {"sword": sword, "bow": bow, "scythe": scythe, "shield": shield, "staff": staff, "gauntlets": gauntlets, "spear": spear, "dual_blades": dual_blades}
IDS = {"sword": "black_sword_greed", "bow": "black_sword_greed_bow", "scythe": "black_sword_greed_scythe", "shield": "black_sword_greed_shield", "staff": "black_sword_greed_staff",
       "gauntlets": "black_sword_greed_gauntlets", "spear": "black_sword_greed_spear", "dual_blades": "black_sword_greed_dual_blades"}


def main(argv):
    only = None
    trial = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
    quick = "--quick" in argv
    if quick:
        full = bl.reset
        bl.reset = lambda samples=6: full(samples)
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, IDS[name] + "_preview.png"))
        if quick:
            out["bake"] = 512
        fn(out)


if __name__ == "__main__":
    main(sys.argv[1:])

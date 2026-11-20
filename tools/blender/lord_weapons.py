"""The four Lord weapons, made in Blender: Black Axe of Sloth, Black Gunblade of Envy, Black Spear of Vanity (Vitality), Seifort Holy Sword, three forms each.

Run: python tools/blender/lord_weapons.py [--only axe,axe_titan,...] [--trial DIR] [--quick] [--no-icons]

--trial DIR  writes the meshes, skins, icons, model jsons and previews into DIR instead of the mod's assets (for looking before replacing).
--quick      few samples and a 512 px bake (for iterating).

One item id per form (the form switch swaps the item id), each with its own mesh, skin, glow skin, 16 x 16 icon and item model. The first form of each weapon
REPLACES the mesh of the old id (the old mesh and skins are kept in weapon_meshes/classic and textures/entity/weapon/classic behind the client setting
lords.classicModels). The design language of each sin (see docs/berserk_weapons_remake.md): sloth a half-closed, heavy-lidded eye on dark violet iron and chains;
envy a revolver cylinder, a barrel on the spine and a serpent coil in green; vanity a mirror, a peacock fan of eyes (ocelli) and a cracked pane in magenta;
holy a cross guard of feathers, a halo and a golden light. The glow skin carries the accent colour; matte metal alone would be lost in the game's light.
Built on the helpers of black_sword_greed.py (one pipeline: bl_lib.py, legendary.py). Quad budget 2800 per mesh like Greed's.
"""
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
import legendary as lg  # noqa: E402
import black_sword_greed as bsg  # noqa: E402   (fang, veins, interp, set_grip, write_models, sprite: the shared helpers)
from bl_lib import rgb  # noqa: E402
from mathutils import Vector  # noqa: E402

PI = math.pi
SAMPLES = 24
GLOW = ("FFFFFF", 0.12, 0.5, 1.0)
ASSETS = bl.ASSETS
interp = bsg.interp

# id -> (length of the mesh in mesh units, length in blocks, grip share, weapon class)
SPEC = {
    "black_axe_of_sloth": (156.0, 2.0, 0.30, "battleaxe"),
    "black_axe_of_sloth_titan": (140.0, 2.4, 0.30, "greataxe"),
    "black_axe_of_sloth_chains": (156.0, 2.0, 0.30, "battleaxe"),
    "black_gunblade_of_envy": (126.0, 1.8, 0.14, "longsword"),
    "black_gunblade_of_envy_rifle": (178.0, 2.2, 0.20, "greatsword"),
    "black_gunblade_of_envy_coil": (112.0, 1.8, 0.14, "longsword"),
    "black_spear_of_vanity": (298.0, 3.0, 0.30, "spear"),
    "black_spear_of_vanity_mirror": (298.0, 3.0, 0.30, "spear"),
    "black_spear_of_vanity_peacock": (298.0, 3.0, 0.30, "spear"),
    "seifort_holy_sword": (122.0, 1.8, 0.12, "longsword"),
    "seifort_holy_sword_radiant": (122.0, 1.8, 0.12, "longsword"),
    "seifort_holy_sword_sent": (156.0, 2.3, 0.14, "greatsword"),
}
PLACE = {}
for _i, (_L, _b, _g, _c) in SPEC.items():
    _o = 0.14 - 0.6 * (_b - 1.8)
    PLACE[_i] = (_o, _o - 0.01, 45.0, _b / _L, 0.0)
GRIP = {i: s[2] for i, s in SPEC.items()}
CLASS = {i: s[3] for i, s in SPEC.items()}
FORMS_OF = {"axe": ["black_axe_of_sloth", "black_axe_of_sloth_titan", "black_axe_of_sloth_chains"],
            "gunblade": ["black_gunblade_of_envy", "black_gunblade_of_envy_rifle", "black_gunblade_of_envy_coil"],
            "spear": ["black_spear_of_vanity", "black_spear_of_vanity_mirror", "black_spear_of_vanity_peacock"],
            "holy": ["seifort_holy_sword", "seifort_holy_sword_radiant", "seifort_holy_sword_sent"]}


# ================================================================ materials: one palette per sin, the same keys as Greed's so the helpers fit
def pal(tag, body, blade, edge, acc, hot, trim=None):
    """body / blade / edge: 0..255 colours of the metals; acc: the accent (0..255) the glow skin lights; hot: its bright core (0..1)."""
    m = {}
    g = lambda c, k: rgb(*[v * k for v in c])                                                    # noqa: E731
    m["black"] = bl.mat(tag + "_body", rgb(*body), metallic=0.9, rough=0.36, bump="hammer", bscale=0.5, bstr=0.10, bevel=0.2, groove=g(body, 0.4), gamount=0.55)
    m["blade"] = bl.mat(tag + "_blade", rgb(*blade), metallic=1.0, rough=0.28, bump="brushed", bstr=0.12, bevel=0.2, groove=g(blade, 0.4), gamount=0.5)
    m["deep"] = bl.mat(tag + "_deep", rgb(*[v * 0.45 for v in body]), metallic=0.8, rough=0.5, bevel=0.1)
    m["edge"] = bl.mat(tag + "_edge", rgb(*edge), metallic=1.0, rough=0.24, bevel=0.0)
    m["inlay"] = bl.mat(tag + "_inlay", rgb(*[v * 0.3 for v in blade]), metallic=1.0, rough=0.42, bump="brushed", bstr=0.1, bevel=0.0)
    m["wrap"] = bl.mat(tag + "_wrap", rgb(*[v * 0.5 for v in body]), rough=0.62, bump="cross", bscale=1.0, bstr=0.9, groove=rgb(*acc), gamount=0.9, bevel=0.0)
    lin = tuple(c / 255.0 for c in acc)
    m["vein"] = bl.mat(tag + "_vein", (lin[0] * 0.05, lin[1] * 0.05, lin[2] * 0.05, 1.0), rough=0.5, emit=lin + (1.0,), strength=1.2, bevel=0.0)
    m["hot"] = bl.mat(tag + "_hot", (0.04, 0.04, 0.04, 1.0), rough=0.5, emit=tuple(hot) + (1.0,), strength=1.0, bevel=0.0)
    m["pupil"] = bl.mat(tag + "_pupil", rgb(3, 2, 4), rough=0.3, bevel=0.0)
    m["eye"] = bl.mat(tag + "_eye", rgb(*[v * 0.6 for v in acc]), metallic=0.3, rough=0.08, emit=lin + (1.0,), strength=0.8, bevel=0.05)
    m["trim"] = bl.mat(tag + "_trim", rgb(*(trim or edge)), metallic=1.0, rough=0.3, bump="hammer", bstr=0.06, bevel=0.2)
    m["shaft"] = bl.mat(tag + "_shaft", rgb(*[v * 0.55 for v in body]), metallic=0.6, rough=0.5, bump="grain", bstr=0.4, groove=rgb(*[v * 0.2 for v in body]), gamount=0.6, bevel=0.0)
    return m


def palette(kind):
    if kind == "sloth":
        return pal("sl", (52, 44, 70), (74, 64, 96), (150, 138, 188), (122, 63, 209), (0.72, 0.52, 1.0))
    if kind == "envy":
        return pal("en", (34, 46, 42), (58, 76, 70), (140, 176, 160), (47, 191, 90), (0.55, 1.0, 0.7))
    if kind == "vanity":
        return pal("va", (52, 34, 50), (84, 58, 82), (190, 160, 190), (214, 79, 176), (1.0, 0.6, 0.9))
    return pal("ho", (214, 198, 160), (232, 224, 204), (255, 244, 214), (232, 185, 138), (1.0, 0.95, 0.8), trim=(238, 190, 98))


# ================================================================ shared pieces
def objs():
    return list(bl.bpy.context.scene.collection.objects)


def blade_mesh(name, st, M, mat="blade", flat=0.3, extra=(), part=None, sec=None):
    ys = sorted(set([s[0] for s in st]) | set(extra))
    rings = []
    for y in ys:
        w, t = interp(st, y)
        rings.append(bl.ring(y, sec(w, t) if sec else bl.lozenge(w, t, flat=flat * w)))
    return bl.loft(name, rings, M[mat], part=part)


def zfun(st, flat=0.3):
    def mk(s):
        def f(x, y):
            w, t = interp(st, y)
            fl = flat * w
            return s * (t * (1.0 if abs(x) <= fl else max(0.0, (w - abs(x)) / (w - fl))) + 0.02)
        return f
    return mk


def blade_light(tag, st, y0, y1, M, seed, forks=7, flat=0.3, core=1.8, vein_w=0.34):
    """The inlay and the spine of light on both faces of a symmetric blade, with veins forking off it (all bake-only, no quads)."""
    mk = zfun(st, flat)
    rng = random.Random(seed)
    n = max(3, int((y1 - y0) / 6))
    for s in (1, -1):
        z = mk(s)
        zs = lambda x, y, z=z, s=s: z(x, y) + 0.06 * s                                            # noqa: E731
        lg.crack(tag + "core%d" % s, [(0.0, y0 + (y1 - y0) * i / n) for i in range(n + 1)], z, s, core, M["inlay"])
        lg.crack(tag + "spine%d" % s, [(0.25 * math.sin(i * 1.3), y0 + (y1 - y0) * i / n) for i in range(n + 1)], zs, s, 0.45, M["vein"])
        for i in range(forks):
            yy = y0 + (y1 - y0) * (i + 0.5) / forks
            sx = 1 if i % 2 == 0 else -1
            bsg.veins(tag + "v%d_%d" % (s, i), rng, (0.2 * sx, yy), 90 - sx * rng.uniform(40, 62), 8.0, 1.5, 22, zs,
                      lambda x, y: y0 <= y <= y1 and abs(x) < 0.8 * interp(st, y)[0], s, vein_w, M["vein"], depth=1, spawn=0.28)


def grip_and_pommel(M, y0, y1, r=2.3, rings=3, eye_rgb=True, thorns=0):
    bl.lathe("grip", [(r - 0.3, y0), (r, y0 + (y1 - y0) * 0.3), (r + 0.1, y0 + (y1 - y0) * 0.65), (r - 0.1, y1)], M["wrap"], sides=10, squash=0.9)
    for i in range(rings):
        y = y0 + (y1 - y0) * (i + 0.5) / rings
        bl.lathe("ring%d" % i, [(r - 0.2, y - 0.9), (r + 0.6, y - 0.55), (r + 0.6, y + 0.55), (r - 0.2, y + 0.9)], M["black"], sides=10, squash=0.92, part="grip_rings")
    bl.lathe("pommel", [(0.01, y0 - 9.0), (r + 0.3, y0 - 8.4), (r + 1.9, y0 - 6.4), (r + 2.2, y0 - 4.0), (r + 1.5, y0 - 1.8), (r + 0.1, y0)], M["black"], sides=12)
    for k in range(thorns):
        a = math.radians(45 + 90 * k)
        bsg.fang("pommel_thorn%d" % k, ((r + 1.0) * math.cos(a), y0 - 4.4, (r + 1.0) * math.sin(a)), ((r + 4.6) * math.cos(a), y0 - 5.6, (r + 4.6) * math.sin(a)), 0.9, M["black"], part="pommel", bend=(0, 1.0, 0), sides=4)
    if eye_rgb:
        for s in (1, -1):
            bl.lathe("pommel_eye%d" % s, [(0.001, 0.0), (1.3, 0.0)], M["vein"], sides=12, axis="z", origin=(0, y0 - 4.6, (r + 2.1) * s), caps=(False, False), detail=True)


def back_copy(prefix):
    """A z-mirrored twin of every object named prefix_*: a motif built on the +z face gets its -z face."""
    for ob in list(bl.bpy.context.scene.collection.objects):
        if ob.name.startswith(prefix + "_") and ob.type == "MESH" and not ob.name.endswith("_back"):
            c = bl.copy(ob, ob.name + "_back", scale=(1, 1, -1))
            c["part"] = ob["part"]


def eye(name, cx, cy, z, r, M, lid=0.0, part=None, back=True, brow=False):
    """A slit eye on a face at height z: a socket (geometry), an iris and a pupil (baked), optionally a heavy lid over the upper part (sloth)."""
    bl.lathe(name + "_rim", [(r * 1.3, z - 0.3), (r * 1.22, z + 0.9), (r * 0.98, z + 1.35), (r * 0.72, z + 0.55), (r * 0.72, z - 0.3)], M["deep"], sides=16, axis="z", origin=(cx, cy, 0), caps=(False, False), part=part)
    bl.lathe(name + "_iris", [(0.001, z + 0.2), (r * 0.66, z + 0.2)], M["vein"], sides=20, axis="z", origin=(cx, cy, 0), caps=(False, False), detail=True)
    n = 9
    lens = [(cx + 0.3 * r * math.sin(PI * k / n), cy - r * 0.62 + 1.24 * r * k / n) for k in range(n + 1)]
    lens += [(cx - 0.3 * r * math.sin(PI * k / n), cy - r * 0.62 + 1.24 * r * k / n) for k in range(n - 1, 0, -1)]
    bl.plate(name + "_pupil", lens, z + 0.26, z + 0.34, M["pupil"], detail=True)
    if lid > 0:                                                                            # the sleepy lid: a half disc closing down over the iris
        yl = cy + r * 0.72 - 2.0 * r * 0.72 * lid
        pts = [(cx + r * 0.95 * math.cos(a), cy + r * 0.95 * math.sin(a)) for a in [PI * k / 10 for k in range(11)]]
        pts = [(x, max(y, yl)) for x, y in pts] + [(cx - r * 0.95, yl), (cx + r * 0.95, yl)][::-1] * 0
        pts = [(cx + r * 0.95, yl)] + [(x, y) for x, y in [(cx + r * 0.95 * math.cos(a), cy + r * 0.95 * math.sin(a)) for a in [PI * k / 10 for k in range(11)]] if y >= yl] + [(cx - r * 0.95, yl)]
        bl.plate(name + "_lid", pts, z + 0.2, z + 1.5, M["black"], part=part)
    if brow:
        for k in range(5):
            a = PI * (0.18 + 0.16 * k)
            bsg.fang("%s_lash%d" % (name, k), (cx + r * 1.1 * math.cos(a), cy + r * 1.1 * math.sin(a), z + 0.5), (cx + r * 1.75 * math.cos(a), cy + r * 1.75 * math.sin(a), z + 1.1), r * 0.12, M["black"], part=part)
    if back:
        back_copy(name)


def ocellus(name, cx, cy, z, r, M, mats=("hot", "vein", "pupil", "vein"), back=False):
    """A peacock eye: concentric discs (baked), outermost first."""
    for i, k in enumerate(mats):
        rr = r * (1.0 - 0.22 * i)
        bl.lathe("%s_o%d" % (name, i), [(0.001, z + 0.2 + 0.02 * i), (rr, z + 0.2 + 0.02 * i)], M[k], sides=16, axis="z", origin=(cx, cy, 0), caps=(False, False), detail=True)
    if back:
        back_copy(name)


def rot_pts(pts, deg, o):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    return [(o[0] + x * c - y * s, o[1] + x * s + y * c) for x, y in pts]


def smooth_xy(pts, sub=3):
    return [(p.x, p.y) for p in bl.smooth_path([(x, y, 0) for x, y in pts], sub)]


def mesh_extent():
    ys = [v.co.y for o in objs() if o.type == "MESH" and not o.get("detail") for v in o.data.vertices]
    return min(ys), max(ys)


def done(item_id, out):
    """The place line from the mesh itself: the scale makes the weapon the length in blocks of SPEC, ymin is the lowest point (the butt)."""
    lo, hi = mesh_extent()
    o = PLACE[item_id][0]
    hdr = os.path.join(bsg._TMP, item_id + ".txt")
    with open(hdr, "w") as fh:
        fh.write("place %.5f %.5f %.3f %.6f %.3f\n" % (o, o - 0.01, 45.0, SPEC[item_id][1] / (hi - lo), lo))
    kw = dict(out)
    res = bl.finish(item_id, glow=GLOW, header_from=hdr, **kw)
    if kw.get("write", True):
        bsg.set_grip(os.path.join(kw.get("out_mesh") or bl.MESH_DIR, item_id + ".txt"), GRIP[item_id])
    return res


def start(kind):
    bl.reset(SAMPLES)
    return palette(kind)


# ================================================================ 1. the Black Axe of Sloth
def axe_head(M, sc, y0, both, z=2.0, eye_r=6.0, tag="h"):
    """A crescent axe head on the haft x = 0, its edge on +x (and on -x for both): a plate, a bright edge bead along the outer arc, a sleepy eye on each face."""
    outer = [(1.0, y0 + 50 * sc), (14 * sc, y0 + 56 * sc), (30 * sc, y0 + 48 * sc), (40 * sc, y0 + 26 * sc), (38 * sc, y0 + 4 * sc), (28 * sc, y0 - 12 * sc), (12 * sc, y0 - 18 * sc), (1.0, y0 - 12 * sc)]
    inner = [(11 * sc, y0 - 4 * sc), (19 * sc, y0 + 10 * sc), (19 * sc, y0 + 28 * sc), (11 * sc, y0 + 42 * sc)]
    arc = smooth_xy(outer[1:-1], 4)
    outline = [(1.0, y0 + 50 * sc)] + arc[1:-1] + [(1.0, y0 - 12 * sc)] + [(11 * sc, y0 - 4 * sc)] + smooth_xy(inner, 3)[1:-1] + [(11 * sc, y0 + 42 * sc)]
    sides = (1, -1) if both else (-1,)
    for s in sides:
        o = [(s * x, y) for x, y in outline]
        pl = bl.plate("%s_head%d" % (tag, s), o if s > 0 else o[::-1], -z, z, M["black"], part="head")
        bl.bevel(pl, 0.35)
        bl.sweep("%s_edge%d" % (tag, s), [(s * x, y, 0) for x, y in arc], bl.lozenge(1.0, z * 0.9), M["edge"], scale=lambda t: (1.0, 1.0), part="head_edge")
        eye("%s_eye%d" % (tag, s), s * 24 * sc, y0 + 22 * sc, z, eye_r * sc, M, lid=0.45, part="head", brow=True)
        # the veins of light across the head, bake-only
        rng = random.Random(7 + s)
        for zs in (1, -1):
            for i in range(5):
                bsg.veins("%s_hv%d_%d_%d" % (tag, s, zs, i), rng, (s * 14 * sc, y0 + (i * 9 - 8) * sc), 0 if s > 0 else 180, 14 * sc, 2.0, 24,
                          lambda x, y, zs=zs: zs * (z + 0.05), lambda x, y: abs(x) < 38 * sc, zs, 0.3, M["vein"], depth=1, spawn=0.3)
    return outline


def haft(M, y0, y1, r=3.0, both_ends=True):
    bl.lathe("haft", [(r - 0.3, y0), (r, y0 + 10), (r, y1 - 10), (r - 0.3, y1)], M["shaft"], sides=10)
    for i, (a, b) in enumerate(((y0 + 20, y0 + 54), (y0 + 66, y0 + 92))):
        bl.lathe("wrap%d" % i, [(r - 0.1, a), (r + 0.35, a + 3), (r + 0.35, b - 3), (r - 0.1, b)], M["wrap"], sides=10, part="grip_wrap")
    for i, y in enumerate((y0 + 14, y0 + 60, y0 + 98)):
        bl.lathe("band%d" % i, [(r - 0.1, y - 1.3), (r + 1.1, y - 0.8), (r + 1.1, y + 0.8), (r - 0.1, y + 1.3)], M["black"], sides=10, part="bands")
    bl.lathe("butt", [(0.01, y0 - 7), (r * 0.7, y0 - 6), (r + 1.3, y0 - 3), (r + 1.1, y0 + 4), (r, y0 + 8)], M["black"], sides=10)
    bsg.fang("butt_spike", (0, y0 - 6, 0), (0, y0 - 16, 0), r * 0.9, M["black"], part="butt")


def chain(M, pts, n, r=2.0, minor=0.55):
    """A chain of round links along a path (alternating the ring's plane): each link a torus, 6 x 8 quads."""
    path = bl.smooth_path(pts, 6)
    for k in range(n):
        p = path[min(len(path) - 1, int(k * (len(path) - 1) / max(1, n - 1)))]
        prof = [(r + minor * math.cos(a), minor * math.sin(a)) for a in [2 * PI * j / 6 for j in range(6)]]
        bl.lathe("link%d" % k, prof, M["trim"] if k % 5 == 0 else M["black"], sides=8, axis="z" if k % 2 == 0 else "x", origin=(p.x, p.y, p.z), caps=(False, False), part="chain")


def axe(out, form):
    M = start("sloth")
    iid = {"base": "black_axe_of_sloth", "titan": "black_axe_of_sloth_titan", "chains": "black_axe_of_sloth_chains"}[form]
    L = SPEC[iid][0]
    if form == "titan":
        haft(M, 0, 128, r=3.4)
        axe_head(M, 1.0, 96, True, z=2.4, eye_r=7.0)
        bl.lathe("collar", [(3.4, 84), (5.4, 88), (5.4, 100), (3.4, 104)], M["black"], sides=12, part="collar")
        bsg.fang("crown", (0, 130, 0), (0, 160, 0), 2.4, M["black"], part="crown", sides=4)
    elif form == "chains":
        haft(M, 0, 140, r=2.8)
        axe_head(M, 0.78, 104, False, z=1.8, eye_r=6.0)
        bl.lathe("collar", [(2.8, 96), (4.4, 100), (4.4, 108), (2.8, 112)], M["black"], sides=12, part="collar")
        bsg.fang("beard_hook", (0, 100, 0), (10, 96, 0), 2.0, M["black"], part="hook", sides=4, bend=(2, 0, 0))
        bsg.fang("crown", (0, 136, 0), (0, L, 0), 2.0, M["black"], part="crown", sides=4)
        for y in (46, 94):                                                                 # the chain's two anchors on the haft
            bl.lathe("anchor%d" % y, [(3.0, y - 2), (4.6, y - 1), (4.6, y + 1), (3.0, y + 2)], M["trim"], sides=10, part="chain")
        chain(M, [(-5, 94, 0), (-12, 88, 0), (-17, 74, 0), (-16, 60, 0), (-11, 50, 0), (-5, 46, 0)], 15, r=3.0, minor=0.8)
    else:
        haft(M, 0, 144, r=3.0)
        axe_head(M, 1.0, 104, False, z=2.0, eye_r=6.0)
        bl.lathe("collar", [(3.0, 92), (4.8, 96), (4.8, 112), (3.0, 116)], M["black"], sides=12, part="collar")
        bsg.fang("beard_spike", (0, 118, 0), (14, 124, 0), 2.4, M["black"], part="spike", sides=4, bend=(1, 2, 0))
        bsg.fang("crown", (0, 140, 0), (0, L, 0), 2.0, M["black"], part="crown", sides=4)
        for s in (1, -1):                                                                  # the drowsy rune: a line of light down the haft
            lg.crack("haftline%d" % s, [(0.0, 10 + 7 * i) for i in range(12)], lambda x, y, s=s: s * 3.1, s, 0.5, M["vein"])
    return done(iid, out)


# ================================================================ 2. the Black Gunblade of Envy
GUN_ST = [(40.0, 5.4, 1.4), (50.0, 6.6, 1.6), (84.0, 6.2, 1.45), (106.0, 5.2, 1.2), (118.0, 3.2, 0.8), (126.0, 0.15, 0.12)]


def gun_sec(w, t):
    """A single-edged section: the edge on +x, a thick flat spine on -x."""
    return [(w, 0), (0.3 * w, t), (-0.6 * w, t), (-0.78 * w, 0.55 * t), (-0.78 * w, -0.55 * t), (-0.6 * w, -t), (0.3 * w, -t)]


def gun_blade(M, st, y0, y1, seed):
    blade_mesh("blade", st, M, extra=[y0 + 6 * k for k in range(1, 20) if y0 + 6 * k < y1], sec=gun_sec)
    # the barrel on the spine: a tube from the cylinder to the muzzle at the point
    ys = [y0 + (y1 - y0 - 8) * i / 6 for i in range(7)]
    bl.tube("barrel", [(-0.78 * interp(st, y)[0] - 1.6, y, 0) for y in ys], 1.45, M["black"], sides=8, sub=2, part="barrel")
    bl.lathe("muzzle", [(1.1, y1 - 14), (2.0, y1 - 12), (2.0, y1 - 8), (1.4, y1 - 6)], M["trim"], sides=8, origin=(-0.78 * interp(st, y1 - 10)[0] - 1.6, 0, 0), part="barrel")
    rng = random.Random(seed)
    for s in (1, -1):
        zs = lambda x, y, s=s: s * (interp(st, y)[1] * (1.0 if x < 0.3 * interp(st, y)[0] else max(0.0, (interp(st, y)[0] - x) / (0.7 * interp(st, y)[0]))) + 0.08)    # noqa: E731
        lg.crack("gcore%d" % s, [(1.0, y0 + 8 + 6.2 * i) for i in range(int((y1 - y0 - 18) / 6.2))], zs, s, 2.0, M["inlay"])
        lg.crack("gspine%d" % s, [(1.0 + 0.3 * math.sin(i * 1.3), y0 + 8 + 6.2 * i) for i in range(int((y1 - y0 - 18) / 6.2))], lambda x, y, zs=zs, s=s: zs(x, y) + 0.05 * s, s, 0.45, M["vein"])
        for i in range(5):
            yy = y0 + 14 + 11 * i
            bsg.veins("gv%d_%d" % (s, i), rng, (1.2, yy), 90 - rng.uniform(35, 60), 7.0, 1.5, 22, lambda x, y, zs=zs, s=s: zs(x, y) + 0.05 * s,
                      lambda x, y: y0 + 6 <= y <= y1 - 8 and -0.5 * interp(st, y)[0] < x < 0.8 * interp(st, y)[0], s, 0.3, M["vein"], depth=1, spawn=0.25)


def gun_body(M, big=False):
    """The cylinder at the guard (chambers on its end face), the trigger guard and trigger, the hammer on the spine and the grip."""
    grip_and_pommel(M, 11.0, 30.0, r=2.3, rings=3, thorns=0)
    bl.lathe("cylinder", [(4.2, 31.0), (5.0, 32.0), (5.0, 40.0), (4.2, 41.0)], M["black"], sides=12, origin=(-1.0, 0, 0), part="cylinder")
    bl.lathe("cyl_cap", [(0.01, 41.0), (4.0, 41.0)], M["deep"], sides=12, origin=(-1.0, 0, 0), caps=(False, False), part="cylinder")
    for k in range(6):                                                                    # the chambers: green light in each
        a = 2 * PI * k / 6
        bl.lathe("chamber%d" % k, [(0.001, 41.1), (1.05, 41.1)], M["vein"], sides=10, origin=(-1.0 + 2.8 * math.cos(a), 0, 2.8 * math.sin(a)), caps=(False, False), detail=True)
    for k in range(6):
        a = 2 * PI * k / 6 + PI / 6
        bl.lathe("flute%d" % k, [(0.001, 33.0), (0.5, 33.0), (0.5, 39.0), (0.001, 39.0)], M["deep"], sides=4, origin=(-1.0 + 4.9 * math.cos(a), 0, 4.9 * math.sin(a)), caps=(False, False), part="cylinder")
    bl.lathe("frame", [(2.6, 28.0), (3.6, 30.0), (3.6, 33.0), (2.6, 35.0)], M["black"], sides=10, squash=0.8, origin=(-1.0, 0, 0), part="cylinder")
    tg = bl.smooth_path([(3.0, 31.0, 0), (9.0, 29.0, 0), (11.0, 22.0, 0), (7.0, 16.0, 0), (3.0, 17.0, 0)], 3)
    bl.tube("trigger_guard", tg, 0.75, M["black"], sides=6, sub=1, part="trigger")
    bsg.fang("trigger", (3.0, 27.0, 0), (6.5, 20.0, 0), 0.8, M["trim"], part="trigger", sides=4, bend=(1.0, 0, 0))
    bsg.fang("hammer", (-5.0, 32.0, 0), (-10.5, 36.0, 0), 1.5, M["black"], part="hammer", sides=4, bend=(-1.2, 1.4, 0))
    # the slit eye of Envy on each flank of the frame
    for s in (1,):
        eye("gun_eye", -1.0, 36.5, 5.0, 2.6, M, part="cylinder", back=True)


def gunblade(out, form):
    M = start("envy")
    iid = {"base": "black_gunblade_of_envy", "rifle": "black_gunblade_of_envy_rifle", "coil": "black_gunblade_of_envy_coil"}[form]
    if form == "rifle":
        # a rifle: stock, receiver with the cylinder as a drum magazine, a long ribbed barrel and a short bayonet blade at the muzzle
        bl.bevel(bl.loft("stock", [bl.ring(y, bl.rrect(w, 3.0, 1.2)) for y, w in ((0.0, 3.4), (6.0, 5.4), (22.0, 5.0), (36.0, 3.8), (46.0, 3.8))], M["black"], part="stock"), 0.2)
        bl.lathe("butt_plate", [(0.01, -1.0), (3.4, -1.0), (3.6, 1.0), (0.01, 1.0)], M["trim"], sides=4, squash=0.9, part="stock")
        bl.loft("receiver", [bl.ring(y, bl.rrect(4.6, 3.4, 1.0)) for y in (44.0, 50.0, 66.0, 74.0)], M["black"], part="receiver")
        bl.lathe("drum", [(6.4, -3.6), (6.6, -3.2), (6.6, 3.2), (6.4, 3.6)], M["black"], sides=14, axis="z", origin=(0, 58.0, 0), part="cylinder")
        for s in (1, -1):
            bl.lathe("drum_core%d" % s, [(0.001, 3.5 * s), (3.4, 3.5 * s)], M["vein"], sides=14, axis="z", origin=(0, 58.0, 0), caps=(False, False), detail=True)
        bl.lathe("shroud", [(3.4, 72.0), (3.6, 74.0), (3.6, 104.0), (3.2, 106.0)], M["black"], sides=10, part="barrel")
        for i in range(6):
            bl.lathe("vent%d" % i, [(3.4, 78.0 + 4.2 * i), (4.0, 78.5 + 4.2 * i), (4.0, 80.5 + 4.2 * i), (3.4, 81.0 + 4.2 * i)], M["deep"], sides=10, part="barrel")
        bl.tube("barrel", [(0, 100.0, 0), (0, 130.0, 0), (0, 156.0, 0)], 2.0, M["dark"] if "dark" in M else M["black"], sides=8, sub=2, part="barrel")
        bl.lathe("muzzle", [(2.0, 148.0), (3.2, 150.0), (3.2, 158.0), (2.0, 160.0)], M["trim"], sides=8, part="barrel")
        blade_mesh("bayonet", [(158.0, 2.6, 1.3), (166.0, 2.6, 1.2), (174.0, 1.4, 0.7), (178.0, 0.12, 0.1)], M, part="bayonet")
        bl.loft("sight_rail", [bl.ring(y, bl.rrect(1.0, 0.8, 0.3)) for y in (76.0, 150.0)], M["trim"], part="rail", ).location = (0, 0, 0)
        bsg.fang("front_sight", (0, 152.0, 0.8), (0, 152.0, 4.0), 0.7, M["black"], part="rail", sides=4)
        # a grip and the trigger below the receiver, an eye on each flank, light in the receiver
        bl.lathe("pistol_grip", [(1.9, 22.0), (2.2, 28.0), (2.0, 36.0)], M["wrap"], sides=10, squash=0.9)
        tg = bl.smooth_path([(1.5, 46.0, 0), (6.0, 44.0, 0), (6.5, 38.0, 0), (2.0, 35.0, 0)], 3)
        bl.tube("trigger_guard", tg, 0.7, M["black"], sides=6, sub=1, part="trigger")
        for s in (1, -1):
            lg.crack("rifle_line%d" % s, [(0.0, 78 + 6 * i) for i in range(10)], lambda x, y, s=s: s * 3.45, s, 0.4, M["vein"])
        return done(iid, out)
    gun_body(M)
    if form == "coil":
        st = [(41.0, 5.0, 1.4), (50.0, 5.8, 1.5), (84.0, 5.4, 1.35), (100.0, 4.2, 1.0), (108.0, 2.4, 0.6), (112.0, 0.15, 0.12)]
        gun_blade(M, st, 41.0, 112.0, 5)
        # the serpent: a coil round the blade, its head over the point
        pts = []
        for i in range(37):
            u = i / 36.0
            a = 2 * PI * 3.0 * u
            pts.append((1.0 + (interp(st, 48 + 54 * u)[0] + 1.6) * math.cos(a), 48.0 + 54.0 * u, 3.4 * math.sin(a)))
        bl.tube("coil", pts, 0.95, M["trim"], sides=6, sub=1, taper=(1.0, 0.55), part="coil")
        bsg.fang("serpent_head", (1.0, 100.0, 0), (1.0, 118.0, 0), 2.6, M["trim"], part="coil", sides=5, bend=(2.5, 0, 0))
        for s in (1, -1):
            bl.lathe("serpent_eye%d" % s, [(0.001, 0.0), (0.9, 0.0)], M["vein"], sides=8, axis="z", origin=(2.0, 108.0, 1.5 * s), caps=(False, False), detail=True)
        return done(iid, out)
    gun_blade(M, GUN_ST, 41.0, 126.0, 3)
    for k in range(3):                                                                    # the fang of envy: three barbs on the edge, hooked back
        y = 70.0 + 14.0 * k
        bsg.fang("barb%d" % k, (interp(GUN_ST, y)[0] - 0.4, y, 0), (interp(GUN_ST, y)[0] + 5.5, y - 7.0, 0), 1.3, M["black"], part="barbs", sides=4, bend=(0.5, 1.5, 0))
    return done(iid, out)


# ================================================================ 3. the Black Spear of Vanity (Vitality)
SPEAR_ST = [(242.0, 1.6, 1.0), (250.0, 5.0, 1.4), (264.0, 6.6, 1.5), (280.0, 4.6, 1.15), (292.0, 1.9, 0.6), (298.0, 0.1, 0.1)]


def spear_shaft(M):
    bl.lathe("butt", [(0.01, 0.0), (1.6, 3.0), (3.0, 9.0), (3.6, 16.0), (3.2, 22.0)], M["black"], sides=10)
    bl.lathe("shaft", [(2.6, 20.0), (2.8, 130.0), (2.6, 232.0)], M["shaft"], sides=10)
    for i, (y0, y1) in enumerate(((52.0, 112.0), (130.0, 172.0))):
        bl.lathe("grip_wrap%d" % i, [(2.9, y0), (3.25, y0 + 3), (3.25, y1 - 3), (2.9, y1)], M["wrap"], sides=10, part="grip_wrap")
    for i, y in enumerate((36.0, 120.0, 178.0, 214.0)):
        bl.lathe("band%d" % i, [(2.9, y - 1.2), (4.1, y - 0.7), (4.1, y + 0.7), (2.9, y + 1.2)], M["black"], sides=10, part="shaft_bands")
    bl.lathe("socket", [(2.8, 226.0), (4.4, 230.0), (4.9, 236.0), (3.8, 244.0), (2.8, 248.0)], M["black"], sides=12)
    blade_mesh("blade", SPEAR_ST, M, extra=(256.0, 270.0, 286.0), flat=0.3)
    blade_light("sp", SPEAR_ST, 252.0, 288.0, M, 41, forks=5)
    for s in (1, -1):                                                                     # runes up the shaft, cracked-mirror lines down it
        for i in range(4):
            bl.text("srune%d_%d" % (s, i), bsg.RUNES[(i * 3 + (0 if s > 0 else 7)) % len(bsg.RUNES)], 3.0, M["hot"], (0.0, 186.0 + 7.0 * i, 2.7 * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, font=lg.rune_font())


def wings(M, y, span, tag="wing", droop=11.0):
    for nm, pts, w, t in ((tag, [(3.0, y + 4), (span * 0.55, y + 10), (span, y + 4), (span * 1.05, y - droop)], 2.4, 1.6),):
        ob = bl.sweep(nm, bl.smooth_path([(x, yy, 0) for x, yy in pts], 3), bl.lozenge(1.0, 1.0, flat=0.3), M["black"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.8 + 0.12, t * (1 - u) ** 0.7 + 0.1), part="wings")
        bl.mirror(ob, "x")


def spear(out, form):
    M = start("vanity")
    iid = {"base": "black_spear_of_vanity", "mirror": "black_spear_of_vanity_mirror", "peacock": "black_spear_of_vanity_peacock"}[form]
    spear_shaft(M)
    if form == "base":
        wings(M, 236.0, 15.0)
        bl.lathe("boss", [(5.2, -3.4), (5.7, -1.7), (5.7, 1.7), (5.2, 3.4)], M["black"], sides=16, axis="z", origin=(0, 232.0, 0), part="wings")
        eye("vain", 0.0, 232.0, 3.4, 3.4, M, part="wings", back=True)
    elif form == "mirror":
        # a round mirror behind the head: a ring of black iron, a pane (baked bright, cracked by veins of light), four spokes and eight short rays
        R = 13.0
        prof = [(R + 1.5 * math.cos(a), 1.5 * math.sin(a)) for a in [2 * PI * j / 8 for j in range(8)]]
        bl.lathe("mirror_ring", prof, M["black"], sides=24, axis="z", origin=(0, 226.0, 0), caps=(False, False), part="mirror")
        for s in (1, -1):
            bl.lathe("pane%d" % s, [(0.001, 0.5 * s), (R - 0.6, 0.5 * s)], M["edge"], sides=24, axis="z", origin=(0, 226.0, 0), caps=(False, False), part="pane")
            rng = random.Random(9 + s)
            for i in range(6):
                a0 = rng.uniform(0, 360)
                bsg.veins("crack%d_%d" % (s, i), rng, (0.0, 226.0), a0, 11.0, 1.8, 26, lambda x, y, s=s: s * 0.7, lambda x, y: (x * x + (y - 226.0) ** 2) < (R - 0.8) ** 2, s, 0.22, M["vein"], depth=1, spawn=0.3)
        for k in range(4):
            a = PI / 4 + PI / 2 * k
            bl.sweep("spoke%d" % k, [(0, 226.0, 0), (R * math.cos(a), 226.0 + R * math.sin(a), 0)], bl.lozenge(0.7, 1.0), M["trim"], part="mirror")
        for k in range(8):
            a = PI / 8 + PI / 4 * k
            bsg.fang("ray%d" % k, ((R + 1.0) * math.cos(a), 226.0 + (R + 1.0) * math.sin(a), 0), ((R + 7.0) * math.cos(a), 226.0 + (R + 7.0) * math.sin(a), 0), 0.9, M["black"], part="mirror", sides=4)
        bl.lathe("boss", [(5.2, -3.0), (5.7, -1.5), (5.7, 1.5), (5.2, 3.0)], M["black"], sides=16, axis="z", origin=(0, 226.0, 0), part="mirror")
        eye("vain", 0.0, 226.0, 3.0, 3.2, M, part="mirror", back=True)
    else:
        # the peacock: a fan of seven feather blades behind the head, an eye (ocellus) at each tip
        o = (0.0, 232.0)
        leaf = [(0, 0), (2.2, 8), (3.6, 20), (3.0, 32), (0, 44), (-3.0, 32), (-3.6, 20), (-2.2, 8)]
        for k in range(-3, 4):
            ang = k * 19.0
            L = 1.0 - 0.1 * abs(k)
            pts = rot_pts([(x, y * L) for x, y in leaf], ang, o)
            fe = bl.plate("feather%d" % (k + 3), pts, -0.7, 0.7, M["black"], part="fan")
            bl.bevel(fe, 0.2)
            tip = rot_pts([(0.0, 31.0 * L)], ang, o)[0]
            ocellus("pe%d" % (k + 3), tip[0], tip[1], 0.7, 3.0, M, mats=("hot", "vein", "pupil", "vein"))
            ocellus("pe%d_b" % (k + 3), tip[0], tip[1], 0.7, 3.0, M, mats=("hot", "vein", "pupil", "vein"))
            for ob in [x for x in objs() if x.name.startswith("pe%d_b_" % (k + 3))]:
                ob.data.transform(bl.Matrix.Diagonal((1, 1, -1, 1)))
                ob.data.flip_normals()
            edge_pts = rot_pts([(0.0, 3.0 * L), (0.0, 40.0 * L)], ang, o)
            for s in (1, -1):
                lg.crack("rib%d_%d" % (k + 3, s), edge_pts, lambda x, y, s=s: s * 0.78, s, 0.28, M["vein"])
        wings(M, 238.0, 11.0, droop=8.0)
        bl.lathe("boss", [(5.2, -3.0), (5.7, -1.5), (5.7, 1.5), (5.2, 3.0)], M["black"], sides=16, axis="z", origin=(0, 232.0, 0), part="fan")
        eye("vain", 0.0, 232.0, 3.0, 3.2, M, part="fan", back=True)
    return done(iid, out)


# ================================================================ 4. the Seifort Holy Sword
HOLY_ST = [(34.0, 5.4, 1.5), (44.0, 6.4, 1.6), (70.0, 6.0, 1.5), (96.0, 5.2, 1.3), (110.0, 3.8, 1.0), (118.0, 2.0, 0.6), (122.0, 0.12, 0.1)]
SENT_ST = [(40.0, 3.8, 1.2), (52.0, 4.4, 1.3), (96.0, 3.8, 1.15), (130.0, 2.8, 0.8), (150.0, 1.2, 0.4), (156.0, 0.1, 0.1)]


def holy_guard(M, y, span, tiers, tag="g"):
    """The cross guard of feathers: each tier is a layered feather plate sweeping out and up from the hilt, mirrored."""
    for i in range(tiers):
        L = span * (1.0 - 0.18 * i)
        up = 7.0 + 4.0 * i
        base = y + 1.2 * i
        outline = [(0.0, base + 2.0), (L * 0.35, base + 3.4 + 0.4 * i), (L * 0.75, base + 3.0 + up * 0.6), (L, base + 2.0 + up), (L * 0.9, base + 0.4 + up * 0.4), (L * 0.6, base - 1.6), (L * 0.25, base - 2.4), (0.0, base - 1.8)]
        outline = smooth_xy(outline, 2)
        for s in (1, -1):
            o = [(s * x, yy) for x, yy in outline]
            pl = bl.plate("%s_f%d_%d" % (tag, i, s), o if s > 0 else o[::-1], -1.1 - 0.15 * i, 1.1 + 0.15 * i, M["trim"] if i == 0 else M["black"], part="guard")
            bl.bevel(pl, 0.25)


def holy(out, form):
    M = start("holy")
    iid = {"base": "seifort_holy_sword", "radiant": "seifort_holy_sword_radiant", "sent": "seifort_holy_sword_sent"}[form]
    if form == "sent":
        grip_and_pommel(M, 14.0, 34.0, r=2.1, rings=3, eye_rgb=False)
        bl.lathe("guard_boss", [(4.4, -2.4), (5.0, -1.2), (5.0, 1.2), (4.4, 2.4)], M["trim"], sides=16, axis="z", origin=(0, 37.0, 0), part="guard")
        blade_mesh("blade", SENT_ST, M, extra=(60.0, 80.0, 110.0), flat=0.2)
        blade_light("sn", SENT_ST, 50.0, 140.0, M, 51, forks=8, flat=0.2, core=1.2)
        # the sent light: a halo over the guard and six blades of light ringing the blade, each pointing the way it will fly
        prof = [(14.0 + 0.8 * math.cos(a), 0.8 * math.sin(a)) for a in [2 * PI * j / 6 for j in range(6)]]
        bl.lathe("halo", prof, M["trim"], sides=28, axis="z", origin=(0, 40.0, 0), caps=(False, False), part="halo")
        for k in range(6):
            a = 2 * PI * k / 6
            r = 8.0
            y0 = 76.0 + 8.0 * (k % 2)
            bsg.fang("shard%d" % k, (r * math.cos(a), y0, r * math.sin(a)), (r * math.cos(a), y0 + 26.0, r * math.sin(a)), 1.6, M["trim"], part="shards", sides=4)
        for i, y in enumerate((100.0, 124.0)):
            prof = [(5.2 + 0.5 * math.cos(a), y + 0.5 * math.sin(a)) for a in [2 * PI * j / 6 for j in range(6)]]
            bl.lathe("ringlet%d" % i, [(5.6, y - 1.0), (6.2, y - 0.5), (6.2, y + 0.5), (5.6, y + 1.0)], M["trim"], sides=14, part="halo")
        return done(iid, out)
    grip_and_pommel(M, 11.0, 26.0, r=2.2, rings=3, eye_rgb=False)
    bl.lathe("guard_boss", [(4.8, -2.6), (5.4, -1.3), (5.4, 1.3), (4.8, 2.6)], M["trim"], sides=16, axis="z", origin=(0, 29.0, 0), part="guard")
    holy_guard(M, 29.0, 20.0 if form == "base" else 24.0, 2 if form == "base" else 3)
    blade_mesh("blade", HOLY_ST, M, extra=(52.0, 84.0), flat=0.3)
    bl.bevel(bl.loft("collar", [bl.ring(y, bl.rrect(w, 2.0, 0.7)) for y, w in ((31.0, 5.0), (34.0, 6.2), (38.0, 6.0), (40.0, 5.6))], M["trim"], part="guard"), 0.2)
    blade_light("hs", HOLY_ST, 44.0, 112.0, M, 61, forks=8, core=1.8)
    for s in (1, -1):                                                                     # the cross of the Seifort in the fuller
        lg.crack("cross_a%d" % s, [(0.0, 48.0 + 5.0 * i) for i in range(5)], lambda x, y, s=s: s * 1.62, s, 0.9, M["hot"])
        lg.crack("cross_b%d" % s, [(-2.2 + 1.1 * i, 58.0) for i in range(5)], lambda x, y, s=s: s * 1.62, s, 0.9, M["hot"])
    if form == "radiant":
        prof = [(17.0 + 0.9 * math.cos(a), 0.9 * math.sin(a)) for a in [2 * PI * j / 6 for j in range(6)]]
        bl.lathe("halo", prof, M["trim"], sides=32, axis="z", origin=(0, 40.0, 0), caps=(False, False), part="halo")
        for k in range(12):
            a = 2 * PI * k / 12
            bsg.fang("ray%d" % k, (17.8 * math.cos(a), 40.0 + 17.8 * math.sin(a), 0), (22.5 * math.cos(a), 40.0 + 22.5 * math.sin(a), 0), 0.8, M["trim"], part="halo", sides=4)
        for s in (1, -1):
            bl.lathe("halo_light%d" % s, [(15.6, 0.6 * s), (16.4, 0.6 * s)], M["hot"], sides=32, axis="z", origin=(0, 40.0, 0), caps=(False, False), detail=True)
    return done(iid, out)


# ================================================================ icons: 16 x 16, a sprite per new form (the four old ids keep their old sprites)
def icons():
    from PIL import Image
    bsg.COL.update({'v': (122, 63, 209), 'V': (190, 150, 255), 'e': (47, 191, 90), 'E': (150, 255, 180), 'p': (214, 79, 176), 'P': (255, 170, 230), 'y': (232, 185, 138), 'Y': (255, 244, 214), 'm': (84, 72, 110), 'n': (54, 78, 70)})
    L, S, R, sh = bsg.line, bsg.sprite, bsg.rect, bsg.shift
    out = {}
    h = L((1, 14), (11, 4))
    out["black_axe_of_sloth_titan"] = S([('m', h), ('g', [(0, 15), (1, 15)]), ('m', R(8, 0, 14, 8)), ('h', L((14, 1), (14, 7))), ('v', [(11, 4), (12, 4)]), ('V', [(11, 3)]), ('d', R(7, 3, 8, 6)), ('h', L((8, 0), (14, 0)))])
    out["black_axe_of_sloth_chains"] = S([('m', h), ('g', [(0, 15), (1, 15)]), ('m', R(9, 1, 13, 6)), ('h', L((13, 1), (13, 6))), ('v', [(11, 3), (12, 3)]), ('V', [(11, 3)]),
                                          ('h', [(5, 10), (4, 11), (3, 10), (3, 12), (4, 13), (6, 12), (6, 10)]), ('v', [(4, 12)])])
    gb = L((14, 2), (7, 9))
    out["black_gunblade_of_envy_rifle"] = S([('n', L((15, 1), (5, 11))), ('h', L((15, 0), (14, 1))), ('e', [(12, 4), (10, 6)]), ('g', R(3, 9, 6, 12)), ('d', L((2, 14), (5, 11))), ('E', [(4, 10)]), ('n', L((3, 12), (1, 14)))])
    out["black_gunblade_of_envy_coil"] = S([('n', gb), ('h', sh(gb, 1, 0)), ('e', [(12, 4), (10, 6), (8, 8)]), ('E', [(11, 5), (9, 7)]), ('g', L((4, 8), (8, 12))), ('E', [(6, 10)]), ('d', L((4, 12), (2, 14)))])
    sp = L((1, 14), (10, 5))
    out["black_spear_of_vanity_mirror"] = S([('m', sp), ('g', [(0, 15), (1, 15)]), ('g', L((11, 4), (14, 1))), ('p', [(10, 2), (9, 3), (9, 4), (10, 5), (11, 5), (12, 4), (12, 3), (11, 2)]), ('P', [(10, 3), (11, 3)]), ('h', [(12, 5)])])
    out["black_spear_of_vanity_peacock"] = S([('m', sp), ('g', [(0, 15), (1, 15)]), ('g', L((11, 4), (14, 1))), ('p', [(6, 3), (8, 1), (10, 1), (12, 1), (13, 3), (14, 6)]), ('P', [(6, 2), (8, 0), (10, 0), (12, 0), (14, 2)]), ('v', [(9, 4)])])
    out["seifort_holy_sword_radiant"] = S([('Y', L((13, 2), (7, 8))), ('y', sh(L((13, 2), (7, 8)), 1, 0)), ('y', L((3, 7), (9, 13))), ('Y', [(5, 9), (7, 11)]), ('y', [(2, 4), (3, 3), (4, 2), (3, 5), (2, 6)]), ('g', L((4, 11), (2, 13)))])
    out["seifort_holy_sword_sent"] = S([('Y', L((15, 0), (6, 9))), ('y', sh(L((15, 0), (6, 9)), 1, 0)), ('y', [(10, 4), (12, 6), (8, 2)]), ('Y', [(9, 3), (11, 5)]), ('y', L((4, 9), (8, 13))), ('g', L((3, 12), (1, 14)))])
    return out


def write_icons(outdir):
    os.makedirs(outdir, exist_ok=True)
    for iid, im in icons().items():
        im.save(os.path.join(outdir, iid + ".png"), optimize=True)


BUILDERS = {}
for _k, _ids in FORMS_OF.items():
    for _n, _f in zip(_ids, ("base", None, None)):
        pass
FORM_NAMES = {"axe": ("base", "titan", "chains"), "gunblade": ("base", "rifle", "coil"), "spear": ("base", "mirror", "peacock"), "holy": ("base", "radiant", "sent")}
FUNC = {"axe": axe, "gunblade": gunblade, "spear": spear, "holy": holy}
for _k in FUNC:
    for _id, _form in zip(FORMS_OF[_k], FORM_NAMES[_k]):
        BUILDERS[_id] = (lambda out, f=FUNC[_k], form=_form: f(out, form))


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
    for iid, fn in BUILDERS.items():
        if only and iid not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, iid + "_preview.png"))
        if quick:
            out["bake"] = 512
        fn(out)
        bsg.write_models(iid, trial or os.path.join(ASSETS, "models", "item"))
    if "--no-icons" not in argv:
        write_icons(os.path.join(trial, "icons") if trial else os.path.join(ASSETS, "textures", "item"))


if __name__ == "__main__":
    main(sys.argv[1:])

"""Five Vassal Weapons made in Blender (3.80.0): the carriage, the book, the mirror, the instrument and the sewing set.

Run: python tools/blender/vassal_b.py [--only carriage,book,mirror,instrument,sewing_set] [--trial DIR] [--preview DIR] [--fast]
  --trial DIR  writes meshes, skins and previews into DIR instead of the mod's assets (for looking before replacing)
  --preview DIR  writes the previews into DIR while the files go to the mod's assets
  --fast       fewer samples and a 512 px bake (for iterating; the final run is without it)

Each weapon keeps the box, the part names and the `place` line of the 3.79.0 model it replaces (PLACE below), so the hand, the display
transforms, the size class, the grip table and the carriage entity (CarriageMeshContractTest) still fit. Unlike the Legendary Weapons
these five carry no `orient manual` line (the runtime normaliser places them as before), so the files are written here, not by
bl_lib.finish. See bl_lib.py for what Blender does and what the mod's tools do.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Vector  # noqa: E402

FAST = False
RUNE_FONTS = ["/usr/share/fonts/truetype/freefont/FreeMono.ttf", "/usr/share/fonts/opentype/unifont/unifont.otf"]      # fonts with the runic block
RUNES = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ"

# the `place` line and the glow pulse of the 3.79.0 mesh each one replaces (same box, so the same place)
PLACE = {
    "vassal_carriage": ((0.00000, 0.08590, 0.000, 0.008333, -0.137), ("FFFFFF", 0.12, 0.45, 1.0)),
    "vassal_book": ((0.48240, -0.08687, 90.000, 0.021667, -8.900), ("FFFFFF", 0.14, 0.45, 1.0)),
    "vassal_mirror": ((0.02701, 0.00511, 45.000, 0.014213, -12.500), ("FFFFFF", 0.1, 0.4, 1.0)),
    "vassal_instrument": ((-0.22553, -0.27484, 45.000, 0.018240, -1.200), ("FFFFFF", 0.14, 0.4, 1.0)),
    "vassal_sewing_set": ((0.01557, 0.05307, 45.000, 0.012475, -11.600), ("FFFFFF", 0.12, 0.4, 1.0)),
}
# the box of the 3.79.0 mesh (x0, x1, y0, y1, z0, z1): the new mesh must have the same y range and may not be wider or thicker
BOX = {
    "vassal_carriage": (-99.386, -0.023, -0.137, 119.866, -36.0, 36.0),
    "vassal_book": (-15.5, 17.125, -8.9, 45.272, -5.179, 8.0),
    "vassal_mirror": (-16.046, 16.204, -12.5, 86.83, -3.022, 6.024),
    "vassal_instrument": (-15.725, 11.688, -1.2, 72.973, -4.82, 7.332),
    "vassal_sewing_set": (-47.867, 8.18, -11.6, 111.0, -8.18, 8.18),
}


# ================================================================ helpers (bl_lib is shared and not edited from here)
def rune_font():
    return next((f for f in RUNE_FONTS if os.path.exists(f)), None)


def start():
    bl.reset(8 if FAST else 24)


def box(name, p0, p1, material, **kw):
    """An axis-aligned box from corner p0 to corner p1."""
    (x0, y0, z0), (x1, y1, z1) = p0, p1
    return bl.plate(name, [(x0, y0), (x1, y0), (x1, y1), (x0, y1)], min(z0, z1), max(z0, z1), material, **kw)


def prism(name, pts, offset, material, **kw):
    """Any flat outline (3D points) pushed along an offset vector: a plate in any orientation."""
    n = len(pts)
    o = Vector(offset)
    verts = [Vector(p) for p in pts] + [Vector(p) + o for p in pts]
    faces = [(k, (k + 1) % n, (k + 1) % n + n, k + n) for k in range(n)] + [tuple(range(n - 1, -1, -1)), tuple(range(n, 2 * n))]
    return bl.obj(name, verts, faces, material, **kw)


def frame(name, outer, inner, z0, z1, material, **kw):
    """A flat ring between two outlines of the same point count (a picture frame, a line round a shape), from z0 to z1."""
    n = len(outer)
    verts = [(x, y, z1) for x, y in outer] + [(x, y, z1) for x, y in inner] + [(x, y, z0) for x, y in outer] + [(x, y, z0) for x, y in inner]
    faces = []
    for k in range(n):
        j = (k + 1) % n
        faces += [(k, j, n + j, n + k), (2 * n + k, 2 * n + j, 3 * n + j, 3 * n + k), (k, j, 2 * n + j, 2 * n + k), (n + k, n + j, 3 * n + j, 3 * n + k)]
    return bl.obj(name, verts, faces, material, **kw)


def flat(name, outline, z, material, t=0.03, **kw):
    """A bake-only plate lying on a +-z face at height z."""
    return bl.plate(name, outline, z - t, z + t, material, detail=True, **kw)


def ring_line(name, outline, width, z, material):
    """A bake-only line of the given width following a closed outline inward."""
    cx = sum(p[0] for p in outline) / len(outline)
    cy = sum(p[1] for p in outline) / len(outline)
    inner = []
    for x, y in outline:
        d = math.hypot(x - cx, y - cy) or 1.0
        inner.append((x - (x - cx) / d * width, y - (y - cy) / d * width))
    return frame(name, outline, inner, z - 0.03, z + 0.03, material, detail=True)


def stroke(name, p, q, width, z, material, w1=None):
    """A bake-only straight line from p to q on a +-z face."""
    (px, py), (qx, qy) = p, q
    d = math.hypot(qx - px, qy - py) or 1.0
    nx, ny = -(qy - py) / d * width / 2, (qx - px) / d * width / 2
    k = (w1 / width) if w1 is not None else 1.0
    return bl.plate(name, [(px - nx, py - ny), (qx - nx * k, qy - ny * k), (qx + nx * k, qy + ny * k), (px + nx, py + ny)], z - 0.03, z + 0.035, material, detail=True)


def strokes(name, pts, width, z, material, closed=True):
    """Bake-only lines along an outline (for long shapes, where ring_line's inset toward the middle would thin the long sides)."""
    seq = list(zip(pts, pts[1:] + pts[:1])) if closed else list(zip(pts, pts[1:]))
    return [stroke("%s%d" % (name, i), p, q, width, z, material) for i, (p, q) in enumerate(seq)]


def snap_y(ob, ymax):
    """Moves an object along y so its highest point is exactly ymax (the piece that fixes the mesh's length)."""
    return bl.move(ob, (0, ymax - max(v.co.y for v in ob.data.vertices), 0))


def ell(rx, ry, n=24, c=(0.0, 0.0), start=0.0):
    return [(c[0] + rx * math.cos(start + 2 * math.pi * k / n), c[1] + ry * math.sin(start + 2 * math.pi * k / n)) for k in range(n)]


def star(c, ro, ri, n, rot=0.0):
    pts = []
    for k in range(2 * n):
        r = ro if k % 2 == 0 else ri
        a = rot + math.pi * k / n
        pts.append((c[0] + r * math.cos(a), c[1] + r * math.sin(a)))
    return pts


def scaled(outline, s, c):
    return [(c[0] + (x - c[0]) * s, c[1] + (y - c[1]) * s) for x, y in outline]


def leaf(base, tip, hw, n=4):
    """A pointed leaf / feather outline from base to tip, half width hw."""
    b, t = Vector((*base, 0)), Vector((*tip, 0))
    d = t - b
    nrm = Vector((-d.y, d.x, 0)).normalized()
    prof = [(0.0, 0.12), (0.18, 0.8), (0.42, 1.0), (0.7, 0.72), (0.9, 0.3)]
    right = [b + d * u + nrm * (hw * w) for u, w in prof]
    left = [b + d * u - nrm * (hw * w) for u, w in reversed(prof)]
    return [(p.x, p.y) for p in right + [t] + left]


_PLANES = {"xy": lambda p, q, v: (p, q, v), "xz": lambda p, q, v: (p, v, q), "zy": lambda p, q, v: (v, q, p), "yz": lambda p, q, v: (v, p, q)}


def band(name, c, rx, ry, section, material, n=24, plane="xy", **kw):
    """A closed ring: a section [(u outward, v across)] carried round an ellipse of radii rx, ry in a plane, centred on c.
    plane 'xy' (axis z), 'xz' (axis y), 'yz' / 'zy' (axis x; the first letter takes rx)."""
    f = _PLANES[plane]
    rings = []
    for i in range(n + 1):
        a = 2 * math.pi * (i % n) / n
        ca, sa = math.cos(a), math.sin(a)
        nx, ny = ca / rx, sa / ry
        d = math.hypot(nx, ny)
        nx, ny = nx / d, ny / d
        rings.append([Vector(c) + Vector(f(rx * ca + u * nx, ry * sa + u * ny, v)) for u, v in section])
    return bl.loft(name, rings, material, caps=(False, False), **kw)


def dome(name, r, h, material, origin, axis="z", sides=12, squash=1.0, steps=4, sign=1, **kw):
    """A cabochon / cap: a quarter-circle profile of radius r rising h along the axis from origin (sign -1: falling)."""
    prof = [(r * math.cos(0.5 * math.pi * k / steps) if k < steps else 0.01, sign * h * math.sin(0.5 * math.pi * k / steps)) for k in range(steps + 1)]
    if sign < 0:
        prof = prof[::-1]
    return bl.lathe(name, prof, material, sides=sides, squash=squash, origin=origin, axis=axis, **kw)


def group_move(obs, loc=(0, 0, 0), rot=(0, 0, 0), scale=(1, 1, 1)):
    for o in obs:
        bl.move(o, loc, rot, scale)
    return obs


def tune(material, node_type, **inputs):
    """Sets inputs of the first node of a type in a material bl.mat made (a pattern's Mapping scale, for one)."""
    for n in material.node_tree.nodes:
        if n.type == node_type:
            for k, v in inputs.items():
                n.inputs[k].default_value = v
            return material
    raise KeyError(node_type)


def glow(name, base, emit, strength=0.5, rough=0.25, metallic=0.0, bevel=0.0):
    return bl.mat(name, base, metallic=metallic, rough=rough, emit=emit, strength=strength, bevel=bevel)


def mats():
    m = {}
    m["silver"] = bl.mat("silver", rgb(214, 221, 232), metallic=1.0, rough=0.22, bump="brushed", bstr=0.1, bevel=0.18)
    m["silver_dark"] = bl.mat("silver_dark", rgb(108, 120, 146), metallic=1.0, rough=0.34, bump="brushed", bstr=0.12, bevel=0.1)
    m["gold"] = bl.mat("gold", rgb(240, 186, 82), metallic=1.0, rough=0.24, bump="hammer", bstr=0.06, bevel=0.2)
    m["gold_deep"] = bl.mat("gold_deep", rgb(168, 112, 38), metallic=1.0, rough=0.36, bevel=0.1)
    # for bake-only details (thin floating plates): no Bevel node, it would average a thin plate's top with its underside
    m["gold_f"] = bl.mat("gold_f", rgb(246, 196, 96), metallic=1.0, rough=0.25, bevel=0.0)
    m["gold_deep_f"] = bl.mat("gold_deep_f", rgb(150, 98, 30), metallic=1.0, rough=0.4, bevel=0.0)
    m["silver_f"] = bl.mat("silver_f", rgb(226, 232, 242), metallic=1.0, rough=0.22, bevel=0.0)
    m["dark_f"] = bl.mat("dark_f", rgb(70, 82, 110), metallic=1.0, rough=0.36, bevel=0.0)
    m["black_f"] = bl.mat("black_f", rgb(14, 10, 10), rough=0.7, bevel=0.0)
    return m


def check_box(item_id, mesh):
    """The new mesh keeps the old y range (so the place line fits) and is not wider or thicker than the old one."""
    lo, hi = mesh.bbox()
    x0, x1, y0, y1, z0, z1 = BOX[item_id]
    e = 0.02
    assert abs(lo[1] - y0) < e and abs(hi[1] - y1) < e, "%s: y %.3f..%.3f, must be %.3f..%.3f" % (item_id, lo[1], hi[1], y0, y1)
    assert lo[0] >= x0 - e and hi[0] <= x1 + e, "%s: x %.3f..%.3f outside %.3f..%.3f" % (item_id, lo[0], hi[0], x0, x1)
    assert lo[2] >= z0 - e and hi[2] <= z1 + e, "%s: z %.3f..%.3f outside %.3f..%.3f" % (item_id, lo[2], hi[2], z0, z1)


def write(item_id, out, **kw):
    """bl_lib.finish without its write, then the files without `orient manual` (these five are placed by the runtime normaliser)."""
    place, glw = PLACE[item_id]
    if FAST:
        kw.setdefault("bake", 512)
    kw.setdefault("cage", 0.25)
    mesh, atlas, stats = bl.finish(item_id, glow=glw, write=False, preview=out.get("preview"), **kw)
    check_box(item_id, mesh)
    mesh.orient_cfg = None
    mesh.place(*place)
    comment = "made in Blender by tools/blender/vassal_b.py"
    mesh.write_txt(os.path.join(out.get("out_mesh") or bl.MESH_DIR, item_id + ".txt"), atlas, normals="vertex", comment=comment)
    from PIL import Image
    d = out.get("out_skin") or bl.SKIN_DIR
    os.makedirs(d, exist_ok=True)
    Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, item_id + ".png"), optimize=True)
    Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(d, item_id + "_glow.png"), optimize=True)
    return mesh, atlas, stats


# ================================================================ the book
def book(out):
    start()
    M = mats()
    leather = bl.mat("leather", rgb(28, 68, 102), rough=0.5, bump="leather", bstr=0.55, groove=rgb(10, 28, 48), gamount=0.55, bevel=0.2)
    spine_l = bl.mat("spine_leather", rgb(56, 112, 156), rough=0.5, bump="leather", bstr=0.55, groove=rgb(16, 40, 64), gamount=0.55, bevel=0.0)      # the spine looks sideways, where the bake light is low
    panel = bl.mat("panel", rgb(30, 78, 104), rough=0.45, bump="leather", bstr=0.5, groove=rgb(12, 34, 52), gamount=0.6, bevel=0.0)
    paper = tune(bl.mat("paper", rgb(238, 228, 198), rough=0.8, bump="brushed", bstr=0.7, groove=rgb(170, 140, 92), gamount=0.6, bevel=0.0),
                 "MAPPING", Scale=(0.02, 0.02, 1.3))                           # the page edges: layers along z only
    strap = bl.mat("strap", rgb(84, 46, 26), rough=0.6, bump="leather", bstr=0.6, groove=rgb(40, 20, 10), gamount=0.6, bevel=0.1)
    cloth = bl.mat("ribbon", rgb(170, 24, 36), rough=0.6, bevel=0.0)
    gemm = glow("gem", rgb(36, 124, 232), rgb(50, 150, 255), 0.32, rough=0.06, metallic=0.3, bevel=0.05)
    lit = glow("lit", rgb(20, 60, 90), rgb(60, 175, 255), 1.0)
    sheet = glow("sheet", rgb(236, 230, 208), rgb(150, 210, 255), 0.22, rough=0.8)
    cx, cy = 0.3, 18.0
    # ---- covers, pages, spine
    for nm, z0, z1 in (("cover_front", 3.4, 4.2), ("cover_back", -4.2, -3.4)):
        bl.bevel(box(nm, (-13.1, 0.0, z0), (13.7, 36.0, z1), leather), 0.22)
    box("pages", (-12.1, 0.7, -3.39), (12.9, 35.3, 3.39), paper)
    def dsec(r, w, y, lap=True):                                          # the rounded back of the spine at height y; lap: a band also laps onto the covers
        pts = [Vector((-12.3 + r * math.cos(math.radians(a)), y, w * math.sin(math.radians(a)))) for a in range(90, 271, 20)]
        return pts + ([Vector((-10.1, y, -w)), Vector((-10.1, y, w))] if lap else [])
    bl.loft("spine", [dsec(2.6, 4.2, 0.0, False), dsec(2.6, 4.2, 36.0, False)], spine_l)   # no flat on the covers: it would lie in their plane and flicker
    for i, y in enumerate((2.2, 8.6, 15.0, 21.4, 27.8, 33.8)):
        bl.loft("band%d" % i, [dsec(3.2, 5.17, y - 0.75), dsec(3.2, 5.17, y + 0.75)], M["gold"], part="spine_bands")
    for i, y in enumerate((5.4, 11.8, 18.2, 24.6, 30.8)):               # a blue jewel in each panel of the spine
        bl.lathe("spine_gem%d" % i, [(0.01, -0.6), (0.7, -0.4), (0.9, 0.0)], gemm, sides=6, origin=(-14.85, y, 0.0), axis="x", part="spine_bands", caps=(True, False))
    # ---- the front: frame, corner caps with bosses, the sun mount and its stone, the feather
    fr_o = [(-10.8, 1.6), (11.8, 1.6), (11.8, 34.4), (-10.8, 34.4)]
    fr_i = [(-9.6, 2.8), (10.6, 2.8), (10.6, 33.2), (-9.6, 33.2)]
    bl.bevel(frame("cover_frame", fr_o, fr_i, 4.15, 4.55, M["gold"]), 0.1)
    flat("panel", [(-9.6, 2.8), (10.6, 2.8), (10.6, 33.2), (-9.6, 33.2)], 4.2, panel)
    rr = [(cx + x, cy + y) for x, y in bl.rrect(8.6, 13.6, 2.6, n=3)]
    ring_line("lit_line", rr, 0.32, 4.27, lit)
    ring_line("gilt_line", [(cx + x, cy + y) for x, y in bl.rrect(9.4, 14.4, 2.2, n=3)], 0.2, 4.27, M["gold_f"])
    for s, (ex, ey) in enumerate(((-13.1, 0.0), (13.7, 0.0), (13.7, 36.0), (-13.1, 36.0))):
        sx, sy = (1 if ex < 0 else -1), (1 if ey < 1 else -1)
        outl = [(ex, ey), (ex + sx * 7.4, ey), (ex + sx * 6.2, ey + sy * 1.6), (ex + sx * 2.9, ey + sy * 2.9), (ex + sx * 1.6, ey + sy * 6.2), (ex, ey + sy * 7.4)]
        bl.bevel(bl.plate("cap_f%d" % s, outl, 4.12, 4.95, M["gold"], part="corner_caps"), 0.2)
        bl.bevel(bl.plate("cap_b%d" % s, outl, -4.95, -4.12, M["gold"], part="corner_caps"), 0.2)
        bl.lathe("boss%d" % s, [(1.15, 4.9), (1.15, 5.35), (0.7, 5.95), (0.01, 6.2)], M["gold"], sides=8, origin=(ex + sx * 2.3, ey + sy * 2.3, 0.0), axis="z", part="corner_caps", caps=(False, True))
        flat("cap_in%d" % s, [(ex + sx * 0.8, ey + sy * 0.8), (ex + sx * 5.2, ey + sy * 0.8), (ex + sx * 2.6, ey + sy * 2.6), (ex + sx * 0.8, ey + sy * 5.2)], 4.97, M["gold_deep_f"])
    mc = (0.6, 21.0)
    bl.bevel(bl.plate("sun_under", star(mc, 9.3, 6.4, 8, math.radians(22.5)), 4.12, 4.7, M["gold_deep"], part="gem_mount"), 0.15)
    bl.bevel(bl.plate("sun", star(mc, 9.55, 6.6, 8), 4.12, 5.35, M["gold"], part="gem_mount"), 0.25)
    for k in range(8):                                                   # a cut line down each ray of the sun
        a = math.pi * k / 4
        stroke("ray%d" % k, (mc[0] + 6.7 * math.cos(a), mc[1] + 6.7 * math.sin(a)), (mc[0] + 9.0 * math.cos(a), mc[1] + 9.0 * math.sin(a)), 0.34, 5.37, M["gold_deep_f"], w1=0.08)
    bl.lathe("bezel", [(5.0, 5.2), (5.0, 6.5), (5.45, 7.0), (6.05, 7.0), (6.5, 6.5), (6.5, 5.2)], M["gold"], sides=16, origin=(mc[0], mc[1], 0.0), axis="z", caps=(False, False), part="gem_mount")
    dome("gem", 4.95, 2.8, gemm, (mc[0], mc[1], 5.2), axis="z", sides=14, steps=4)
    for k in range(8):                                                   # light ticks between the sun's rays, and a spark at each end
        a = math.pi * (k + 0.5) / 4
        p = (mc[0] + 7.6 * math.cos(a), mc[1] + 7.6 * math.sin(a))
        q = (mc[0] + 10.6 * math.cos(a), mc[1] + 10.6 * math.sin(a))
        if abs(q[0] - cx) < 8.0:
            stroke("tick%d" % k, p, q, 0.3, 4.27, lit, w1=0.1)
    bl.bevel(bl.plate("emblem", leaf((-6.1, 4.3), (6.9, 9.5), 2.1), 4.12, 4.75, M["gold"]), 0.14)
    stroke("quill", (-6.4, 4.2), (6.4, 9.3), 0.3, 4.78, M["gold_deep_f"], w1=0.1)
    for k in range(5):
        t = 0.2 + 0.15 * k
        bx, by = -6.1 + 13.0 * t, 4.3 + 5.2 * t
        stroke("barb_a%d" % k, (bx, by), (bx + 1.5, by + 1.5), 0.16, 4.78, M["gold_deep_f"])
        stroke("barb_b%d" % k, (bx, by), (bx + 1.9, by - 0.7), 0.16, 4.78, M["gold_deep_f"])
    bl.text("title", RUNES[:6], 2.5, M["gold_f"], (cx, 31.7, 4.3), spacing=1.3, font=rune_font())
    # ---- the back: a tooled panel, a ring of light and gilt runes
    flat("panel_b", [(-10.4, 2.4), (11.0, 2.4), (11.0, 33.6), (-10.4, 33.6)], -4.2, panel)
    ring_line("gilt_b", [(cx + x, cy + y) for x, y in bl.rrect(10.2, 15.2, 1.2, n=2)], 0.3, -4.27, M["gold_f"])
    ring_line("ring_b", ell(6.6, 6.6, 28, (cx, cy)), 0.34, -4.27, lit)
    flat("star_b", star((cx, cy), 5.2, 2.2, 8), -4.27, M["gold_f"])
    bl.text("runes_b", RUNES[9:18], 1.9, M["gold_f"], (cx, 29.6, -4.3), rot=(0, 180, 0), spacing=1.25, font=rune_font())
    bl.text("runes_b2", RUNES[15:24], 1.9, M["gold_f"], (cx, 6.4, -4.3), rot=(0, 180, 0), spacing=1.25, font=rune_font())
    # ---- strap round the fore-edge, buckle, ribbon, loose pages
    path = bl.smooth_path([(10.6, 17.5, 4.32), (14.2, 17.5, 4.9), (16.5, 17.5, 3.6), (16.85, 17.5, 0.0), (16.5, 17.5, -3.6), (14.2, 17.5, -4.9), (10.6, 17.5, -4.32)], 3)
    bl.sweep("clasp_strap", path, [(-0.26, -5.6), (0.26, -5.6), (0.26, 5.6), (-0.26, 5.6)], strap, up=(0, 1, 0))
    bl.bevel(frame("buckle", [(10.9, 11.2), (14.3, 11.2), (14.3, 23.8), (10.9, 23.8)], [(11.7, 12.0), (13.5, 12.0), (13.5, 23.0), (11.7, 23.0)], 4.7, 5.35, M["gold"]), 0.12)
    box("prong", (11.0, 17.1, 5.05), (14.2, 17.9, 5.5), M["gold"], part="buckle")
    bl.plate("ribbon", [(-4.4, 0.7), (-2.1, 0.7), (-2.1, -8.9), (-3.25, -7.3), (-4.4, -8.9)], 0.0, 0.3, cloth)
    for i, (px, lean, tilt, top) in enumerate(((-6.4, 16.0, 10.0, 44.4), (0.4, -6.0, 18.0, 45.272), (6.6, -20.0, 8.0, 43.6))):
        h = 12.4
        sh = box("page%d" % i, (-4.4, 0.0, -0.07), (4.4, h, 0.07), sheet, part="loose_pages")
        marks = [stroke("pl%d%d" % (i, k), (-3.2, 2.2 + 2.0 * k), (3.2 - (k % 2) * 1.4, 2.2 + 2.0 * k), 0.32, 0.1, lit) for k in range(5)]
        marks += [stroke("pb%d%d" % (i, k), (-3.2, 2.2 + 2.0 * k), (3.2 - ((k + 1) % 2) * 1.4, 2.2 + 2.0 * k), 0.32, -0.1, lit) for k in range(5)]
        group_move([sh] + marks, rot=(tilt, 0, lean))
        ys = [v.co.y for v in sh.data.vertices]
        group_move([sh] + marks, loc=(px, top - max(ys), 1.4))
    return write("vassal_book", out)


# ================================================================ the mirror
def mirror(out):
    start()
    M = mats()
    glass = glow("glass", rgb(146, 198, 234), rgb(140, 205, 255), 0.1, rough=0.14, metallic=0.4)
    shine = glow("shine", rgb(240, 250, 255), rgb(235, 250, 255), 1.3)
    gemm = glow("gem", rgb(16, 150, 190), rgb(30, 205, 235), 0.3, rough=0.06, metallic=0.3, bevel=0.05)
    lit = glow("lit", rgb(20, 70, 80), rgb(60, 220, 245), 1.0)
    pearl = bl.mat("pearl", rgb(246, 247, 250), rough=0.16, bevel=0.0)
    twist = bl.mat("twist", rgb(206, 214, 228), metallic=1.0, rough=0.26, bump="wrap", bscale=0.9, bstr=0.9, groove=rgb(84, 96, 122), gamount=1.0, bevel=0.0)
    C = (0.0, 51.0)
    # ---- head: rim, bezel, pearls, glass, back
    band("head_rim", (C[0], C[1], 0.0), 13.0, 18.4, bl.rrect(1.68, 1.3, 0.5, n=1), M["silver"], n=36)
    frame("rim_inlay", ell(13.85, 19.25, 36, C), ell(12.15, 17.55, 36, C), 1.27, 1.33, M["dark_f"], detail=True)
    frame("rim_inlay_b", ell(13.85, 19.25, 36, C), ell(12.15, 17.55, 36, C), -1.33, -1.27, M["dark_f"], detail=True)
    band("head_bezel", (C[0], C[1], 1.2), 10.6, 15.9, bl.circle(0.6, 6), M["silver"], n=32)
    for k in range(18):
        a = 2 * math.pi * (k + 0.5) / 18
        dome("bead%d" % k, 0.82, 0.85, pearl, (C[0] + 13.0 * math.cos(a), C[1] + 18.4 * math.sin(a), 1.25), axis="z", sides=5, steps=2, part="head_beads")
    bl.plate("head_glass", ell(10.8, 16.1, 32, C), -0.5, 0.5, glass)
    for i, (dx, w) in enumerate(((-4.6, 1.5), (-1.6, 0.7), (3.4, 1.1))):   # three slanting bars of light on the glass
        flat("shine%d" % i, [(dx - 2.2, 44.0), (dx - 2.2 + w, 44.0), (dx + 2.6 + w, 58.0), (dx + 2.6, 58.0)], 0.5, shine)
    flat("spark", star((3.4, 41.0), 1.9, 0.35, 4), 0.5, shine)
    bl.plate("back_plate", ell(12.4, 17.7, 32, C), -1.6, -0.7, M["silver"])
    ring_line("back_l1", ell(11.0, 16.3, 32, C), 0.3, -1.6, M["dark_f"])
    ring_line("back_l2", ell(7.4, 11.4, 28, C), 0.28, -1.6, lit)
    ring_line("back_l3", ell(4.4, 4.4, 24, C), 0.24, -1.6, M["dark_f"])
    for k in range(8):
        a = math.pi * k / 4
        stroke("back_ray%d" % k, (C[0] + 4.6 * math.cos(a), C[1] + 4.6 * math.sin(a)), (C[0] + 7.0 * math.cos(a), C[1] + 10.8 * math.sin(a)), 0.3, -1.6, M["dark_f"], w1=0.08)
    bl.text("back_runes", RUNES[:7], 1.8, lit, (C[0], C[1] + 13.6, -1.67), rot=(0, 180, 0), spacing=1.25, font=rune_font())
    bl.text("back_runes2", RUNES[7:14], 1.8, lit, (C[0], C[1] - 13.6, -1.67), rot=(0, 180, 0), spacing=1.25, font=rune_font())
    bl.lathe("back_boss", [(2.5, -1.6), (2.3, -2.3), (1.3, -2.8), (0.01, -2.95)][::-1], M["silver"], sides=10, origin=(C[0], C[1], 0.0), axis="z", caps=(True, False))
    # ---- scrolls beside the head
    pts = [(14.9, 57.0), (15.2, 50.0), (14.7, 43.6), (13.4, 38.4), (11.6, 34.8), (10.2, 32.0), (11.2, 30.0), (13.4, 29.9), (14.6, 31.6), (13.9, 33.4), (12.6, 33.3)]
    sc = bl.tube("scrolls", [(x, y, 0.6) for x, y in pts], None, M["silver"], sides=5, sub=2, taper=(0.62, 1.15))
    bl.mirror(sc, "x")
    for sx in (1, -1):
        dome("scroll_eye%d" % sx, 1.0, 0.8, pearl, (sx * 12.6, 33.3, 1.5), axis="z", sides=5, steps=2, part="scrolls")
    # ---- crest of five leaves, the stone and its claws
    G = (0.0, 69.3)
    for i, (ang, ln, hw) in enumerate(((0.0, 17.5, 3.7), (27.0, 14.6, 3.1), (-27.0, 14.6, 3.1), (54.0, 12.0, 2.6), (-54.0, 12.0, 2.6))):
        a = math.radians(90.0 + ang)
        tip = (G[0] + ln * math.cos(a), G[1] + ln * math.sin(a))
        z1 = 1.0 - 0.3 * (i > 0) - 0.3 * (i > 2)
        lf = bl.plate("crest%d" % i, leaf(G, tip, hw), z1 - 1.3, z1, M["silver"], part="crest")
        if i == 0:
            snap_y(lf, BOX["vassal_mirror"][3])                          # the centre leaf's tip is the mesh's top: no bevel modifier on it
        else:
            bl.bevel(lf, 0.28)
        base = (G[0] + 4.6 * math.cos(a), G[1] + 5.4 * math.sin(a))
        stroke("vein%d" % i, base, (G[0] + (ln - 1.2) * math.cos(a), G[1] + (ln - 1.2) * math.sin(a)), 0.36, z1, lit if i == 0 else M["dark_f"], w1=0.08)
    band("gem_ring", (G[0], G[1], 1.2), 3.95, 5.0, bl.circle(0.62, 5), M["silver"], n=18, part="gem_claws")
    g = dome("head_gem", 3.7, 4.6, gemm, (0, 0, 1.1), axis="z", sides=12, steps=4)
    bl.move(g, scale=(1, 1.27, 1))
    bl.move(g, (G[0], G[1], 0))
    for k in range(4):
        a = math.radians(45 + 90 * k)
        ca, sa = math.cos(a), math.sin(a)
        bl.tube("claw%d" % k, [(G[0] + 4.1 * ca, G[1] + 5.2 * sa, 1.4), (G[0] + 3.7 * ca, G[1] + 4.7 * sa, 3.5), (G[0] + 2.3 * ca, G[1] + 2.9 * sa, 5.1)], None,
                M["silver"], sides=4, sub=3, taper=(0.62, 0.3), part="gem_claws")
    # ---- handle
    bl.lathe("collar", [(1.7, 23.2), (2.5, 24.3), (2.1, 25.6), (2.6, 27.0), (4.6, 29.4), (5.75, 31.6), (5.2, 33.6)], M["silver"], sides=12, squash=0.5)
    bl.lathe("grip", [(1.55, 5.8), (1.95, 8.0), (1.82, 14.5), (1.95, 21.2), (1.55, 23.6)], twist, sides=10)
    for i, y in enumerate((6.4, 23.0)):
        band("grip_ring%d" % i, (0, y, 0), 1.75, 1.75, bl.circle(0.42, 4), M["silver"], n=10, plane="xz", part="grip")      # part grip: mesh_lib weighs the `grip` line by quads, this keeps the hand where 3.79.0 had it
    bl.lathe("pommel", [(0.01, 0.0), (1.8, 0.4), (2.9, 2.2), (2.8, 3.6), (1.9, 5.2), (1.6, 6.0)], M["silver"], sides=10)
    band("link1", (0, -3.3, 0), 0.95, 2.4, bl.circle(0.32, 5), M["silver"], n=12, plane="xy", part="chain_loop")
    band("link2", (0, -7.4, 0), 1.35, 2.4, bl.circle(0.32, 5), M["silver"], n=12, plane="zy", part="chain_loop")
    bl.lathe("drop", [(0.01, -12.5), (0.75, -12.0), (0.95, -11.3), (0.6, -10.4), (0.2, -9.6)], gemm, sides=6, part="chain_loop")
    return write("vassal_mirror", out)


# ================================================================ the instrument
def instrument(out):
    start()
    M = mats()
    wood = bl.mat("wood", rgb(222, 132, 48), rough=0.28, bump="grain", bscale=0.55, bstr=0.3, groove=rgb(156, 76, 22), gamount=0.45, bevel=0.2)
    wood_f = bl.mat("wood_f", rgb(150, 74, 22), rough=0.32, bump="grain", bscale=0.55, bstr=0.3, groove=rgb(104, 46, 12), gamount=0.5, bevel=0.0)
    flame = tune(bl.mat("flame", rgb(214, 122, 42), rough=0.3, bump="brushed", bstr=0.3, groove=rgb(140, 64, 18), gamount=0.5, bevel=0.2),
                 "MAPPING", Scale=(0.05, 0.7, 0.05))                          # the flamed maple of the back: stripes across
    ebony = bl.mat("ebony", rgb(30, 22, 22), rough=0.3, bevel=0.14)
    maple = bl.mat("maple", rgb(220, 178, 118), rough=0.45, bump="grain", bstr=0.3, bevel=0.05)
    gemm = glow("gem", rgb(226, 70, 150), rgb(255, 90, 180), 0.3, rough=0.06, metallic=0.3, bevel=0.05)
    string = glow("string", rgb(236, 214, 150), rgb(255, 225, 150), 0.45, rough=0.3, metallic=0.6)
    hair = glow("hair", rgb(240, 236, 222), rgb(255, 245, 225), 0.12, rough=0.7)
    lit = glow("lit", rgb(200, 150, 60), rgb(255, 200, 100), 0.8, rough=0.3, metallic=0.6)
    right = [(0.0, 0.0), (4.0, 0.25), (8.0, 1.8), (10.6, 4.8), (11.5, 8.8), (11.0, 12.6), (9.3, 15.4), (7.7, 16.6), (6.9, 18.6), (6.8, 20.6), (7.7, 22.6),
             (8.9, 24.2), (9.4, 27.2), (8.8, 30.6), (6.8, 33.4), (3.6, 35.0), (0.0, 35.5)]
    outline = right + [(-x, y) for x, y in reversed(right[1:-1])]
    c = (0.0, 17.75)

    def rings(zs):
        return [[Vector((x, y, z)) for x, y in scaled(outline, s, c)] for z, s in zs]
    bl.loft("body_top", rings([(0.0, 1.0), (1.1, 0.99), (1.6, 0.93)]), wood, caps=(False, True))
    bl.loft("body_back", rings([(-4.82, 0.93), (-4.3, 0.99), (-3.3, 1.0)]), flame, caps=(True, False))
    bl.loft("ribs", rings([(-3.3, 0.975), (0.0, 0.975)]), flame, caps=(False, False))
    # the top's inlay: a lit purfling line, a darker edge, f-holes
    frame("purfling", scaled(outline, 0.885, c), scaled(outline, 0.86, c), 1.57, 1.63, lit, detail=True)
    frame("edge_dark", scaled(outline, 0.93, c), scaled(outline, 0.9, c), 1.57, 1.62, wood_f, detail=True)
    frame("purfling_b", scaled(outline, 0.885, c), scaled(outline, 0.86, c), -4.85, -4.79, lit, detail=True)
    frame("purfling_in", scaled(outline, 0.83, c), scaled(outline, 0.815, c), 1.57, 1.62, wood_f, detail=True)
    for sx in (1, -1):                                                   # little lit stars on the bouts, a rosette and runes on the back
        flat("bout_star%d" % sx, star((sx * 5.0, 28.2), 1.5, 0.45, 4), 1.6, lit)
        flat("bout_star_l%d" % sx, star((sx * 7.6, 7.6), 1.7, 0.5, 4), 1.6, lit)
    flat("rose_b", star((0.0, 9.6), 5.6, 2.4, 8), -4.82, M["gold_f"])
    ring_line("rose_ring_b", ell(7.4, 7.4, 24, (0.0, 9.6)), 0.4, -4.82, lit)
    flat("rose_c", ell(1.5, 1.5, 10, (0.0, 9.6)), -4.86, lit)
    bl.text("runes_b", RUNES[:6], 2.2, lit, (0.0, 27.4, -4.9), rot=(0, 180, 0), spacing=1.25, font=rune_font())
    for sx in (1, -1):
        fh = [(sx * 4.2, 13.0), (sx * 4.9, 13.4), (sx * 5.0, 15.5), (sx * 4.4, 18.5), (sx * 4.6, 20.6), (sx * 5.3, 21.0), (sx * 4.6, 21.4), (sx * 4.0, 20.6), (sx * 3.9, 18.4),
              (sx * 4.5, 15.6), (sx * 4.4, 13.8), (sx * 3.7, 13.4)]
        flat("fhole%d" % sx, fh if sx > 0 else fh[::-1], 1.6, M["black_f"])
        flat("fdot_a%d" % sx, ell(0.55, 0.55, 8, (sx * 4.0, 12.9)), 1.6, M["black_f"])
        flat("fdot_b%d" % sx, ell(0.55, 0.55, 8, (sx * 5.0, 21.4)), 1.6, M["black_f"])
    # ---- neck, pegbox, scroll
    nk = [(31.0, 1.3, -3.3, 2.9), (36.3, 1.3, -1.6, 2.9), (39.0, 1.25, 0.9, 2.9), (49.4, 1.05, 1.2, 2.9), (50.0, 1.5, 0.5, 3.3), (57.6, 1.5, 0.3, 3.3)]
    bl.loft("neck_scroll", [[Vector((sx * w, y, z)) for sx, z in ((1, z0), (1, z1), (-1, z1), (-1, z0))] for y, w, z0, z1 in nk], wood)
    sp = []
    for k in range(25):
        t = k / 24.0
        a = math.radians(-100.0 - 600.0 * t)
        r = 4.05 * (1 - t) ** 0.9 + 0.75
        sp.append((0.0, 60.2 + r * math.cos(a), 1.1 + r * math.sin(a)))
    bl.sweep("scroll", sp, bl.rrect(0.75, 2.05, 0.3, n=1), wood, up=(1, 0, 0), scale=lambda t: (1.0 - 0.45 * t, 1.0), part="neck_scroll")
    g = bl.gem("gem", 1.95, gemm, center=(2.2, 60.2, 1.1), depth=0.3, crown=0.3, sides=8, axis="x")
    bl.mirror(g, "x")
    for sx in (1, -1):
        band("gem_ring%d" % sx, (sx * 2.25, 60.2, 1.1), 2.3, 2.3, bl.circle(0.36, 5), M["gold"], n=14, plane="yz", part="gold_fittings")
    for i, y in enumerate((39.6, 48.4)):
        band("neck_band%d" % i, (0, y, 1.95), 1.5, 1.25, bl.circle(0.3, 5), M["gold"], n=12, plane="xz", part="gold_fittings")
    bl.loft("fingerboard", [[Vector((sx * w, y, z)) for sx, z in ((1, 2.9), (1, 3.9), (-1, 3.9), (-1, 2.9))] for y, w in ((21.5, 1.6), (48.9, 1.12))], ebony)
    for i, (y, sx) in enumerate(((51.2, 1), (53.2, -1), (55.2, 1), (57.0, -1))):
        prof = [(0.32, 0.0), (0.32, 1.9), (0.95, 2.15), (0.95, 2.85), (0.3, 3.0)]
        bl.lathe("peg%d" % i, [(r, sx * h) for r, h in prof], ebony, sides=6, origin=(0.0, y, 1.9), axis="x", squash=0.42, part="pegs")
        dome("peg_tip%d" % i, 0.3, 0.25, M["gold"], (sx * 3.0, y, 1.9), axis="x", sides=6, steps=1, sign=sx, part="gold_fittings")
    # ---- bridge, tailpiece, chin rest, strings
    br = bl.plate("bridge", [(-2.1, 1.5), (2.1, 1.5), (1.9, 3.6), (1.3, 4.75), (0.0, 5.1), (-1.3, 4.75), (-1.9, 3.6)], -0.1, 0.1, maple)
    bl.move(br, rot=(90, 0, 0))
    bl.move(br, (0, 16.0, 0))
    bl.bevel(bl.plate("tailpiece", [(-0.9, 1.4), (0.9, 1.4), (1.75, 10.6), (-1.75, 10.6)], 2.35, 3.0, ebony, part="fittings"), 0.15)
    flat("tail_inlay", star((0.0, 7.4), 1.2, 0.45, 4), 3.0, M["gold_f"])
    flat("tail_line", [(-1.5, 9.6), (1.5, 9.6), (1.55, 10.0), (-1.55, 10.0)], 3.0, M["gold_f"])
    box("saddle", (-1.0, -0.2, 0.6), (1.0, 1.5, 2.5), ebony, part="fittings")
    bl.bevel(bl.plate("chinrest", ell(3.2, 2.9, 14, (-6.0, 3.6)), 1.5, 3.0, ebony, part="fittings"), 0.45)
    bl.lathe("button", [(0.01, -1.2), (0.8, -1.0), (0.9, -0.5), (0.5, 0.1)], ebony, sides=8, origin=(0, 0, -1.6), part="fittings")
    box("fret_nut", (-1.15, 48.6, 3.9), (1.15, 49.1, 4.15), M["gold"], part="gold_fittings")
    for i, x in enumerate((-1.05, -0.35, 0.35, 1.05)):
        bl.tube("string%d" % i, [(x, 10.4, 3.05), (x, 16.0, 5.1), (x * 0.72, 48.9, 4.2)], 0.11, string, sides=4, sub=1, part="strings")
    # ---- the bow, lying across the front
    A, B = Vector((6.2, 3.4, 6.5)), Vector((-15.0, 72.4, 6.5))
    d = (B - A).normalized()
    side = Vector((-d.y, d.x, 0.0))
    stick = [A + (B - A) * t + Vector((0, 0, -0.9)) * math.sin(math.pi * t) for t in (0.0, 0.2, 0.4, 0.6, 0.8, 1.0)]
    bl.tube("bow_stick", stick, None, wood_f_solid(), sides=6, sub=2, taper=(0.42, 0.3))
    tip = prism("bow_tip", [B + side * 0.3 + Vector((0, 0, 0.6)), B + d * 0.55 + side * 0.3 + Vector((0, 0, 0.2)), B + d * 0.3 + side * 0.3 + Vector((0, 0, -2.0)),
                            B - d * 1.6 + side * 0.3 + Vector((0, 0, -1.9)), B - d * 1.2 + side * 0.3 + Vector((0, 0, 0.3))], -side * 0.6, maple, part="bow_stick")
    snap_y(tip, BOX["vassal_instrument"][3])
    hs, he = A + d * 4.6 - side * 1.0 + Vector((0, 0, -2.6)), B - d * 1.0 - side * 0.5 + Vector((0, 0, -2.0))
    bl.sweep("bow_hair", [hs, hs + (he - hs) * 0.5, he], [(-0.42, -0.1), (0.42, -0.1), (0.42, 0.1), (-0.42, 0.1)], hair)
    ang = math.degrees(math.atan2(-d.x, d.y))
    fr = bl.bevel(box("bow_frog", (-0.72, -3.6, -2.75), (0.72, 3.2, 0.35), ebony), 0.22)
    eye = [bl.lathe("frog_eye%d" % sx, [(0.62, 0.0), (0.5, 0.14 * sx), (0.01, 0.2 * sx)][::sx], lit, sides=8, origin=(0.72 * sx, 0.2, -1.3), axis="x", part="bow_frog",
                    caps=(False, True) if sx > 0 else (True, False)) for sx in (1, -1)]
    fer = box("ferrule", (-0.8, 2.5, -2.85), (0.8, 3.4, -1.6), M["gold"], part="gold_fittings")
    btn = bl.lathe("bow_button", [(0.01, -5.0), (0.5, -4.8), (0.55, -3.7), (0.4, -3.5)], M["gold"], sides=6, part="gold_fittings")
    group_move([fr, fer, btn] + eye, rot=(0, 0, ang))
    group_move([fr, fer, btn] + eye, loc=tuple(A + d * 2.2))
    return write("vassal_instrument", out)


def wood_f_solid():
    return bl.mat("bow_wood", rgb(120, 58, 22), rough=0.3, bump="grain", bstr=0.25, bevel=0.0)


# ================================================================ the sewing set
def sewing_set(out):
    start()
    M = mats()
    thread = bl.mat("thread", rgb(196, 34, 116), rough=0.55, bump="rings", bscale=4.0, bstr=0.8, groove=rgb(108, 10, 60), gamount=0.8, bevel=0.0)
    dimple = bl.mat("thimble", rgb(204, 212, 226), metallic=1.0, rough=0.28, bump="hammer", bscale=3.2, bstr=0.9, groove=rgb(96, 106, 132), gamount=0.9, bevel=0.1)
    needle = bl.mat("needle", rgb(222, 228, 240), metallic=1.0, rough=0.16, bump="wrap", bscale=0.22, bstr=0.5, groove=rgb(120, 132, 168), gamount=0.45, bevel=0.0)
    gemm = glow("gem", rgb(120, 50, 210), rgb(150, 70, 255), 0.32, rough=0.06, metallic=0.3, bevel=0.05)
    silk = glow("silk", rgb(250, 110, 200), rgb(255, 90, 200), 0.8)
    cordm = bl.mat("cord", rgb(120, 96, 150), rough=0.6, bump="wrap", bscale=1.5, bstr=0.7, bevel=0.0)
    # ---- the spool (the grip), its thread, the thimble below
    bl.lathe("grip_core", [(3.0, 0.0), (7.5, 0.0), (7.9, 0.5), (7.9, 1.5), (7.0, 2.0), (3.2, 2.5), (3.2, 21.5), (7.0, 22.0), (7.9, 22.5), (7.9, 23.5), (7.5, 24.0), (3.0, 24.0)],
             M["silver"], sides=16)
    for i, y in enumerate((1.0, 23.0)):
        band("rim%d" % i, (0, y, 0), 7.72, 7.72, bl.circle(0.45, 5), M["gold"], n=20, plane="xz", part="grip_rims")
    bl.lathe("grip_thread", [(5.0, 2.3), (5.55, 5.0), (5.7, 12.0), (5.55, 19.0), (5.0, 21.7)], thread, sides=16, caps=(False, False))
    bl.lathe("grip_thimble", [(0.01, -11.6), (3.4, -11.25), (4.9, -9.8), (5.6, -6.0), (5.8, -1.9), (6.3, -1.2), (6.3, 0.2)], dimple, sides=12, squash=0.97)      # few quads: mesh_lib weighs the `grip` line by quads, a heavy thimble pulls the hand down
    for i, (a, y, col) in enumerate(((0.6, 15.0, rgb(250, 210, 90)), (2.5, 9.0, rgb(120, 220, 255)), (4.4, 13.0, rgb(250, 250, 250)))):   # pins stuck in the thread
        ca, sa = math.cos(a), math.sin(a)
        bl.tube("pin%d" % i, [(5.2 * ca, y, 5.2 * sa), (7.3 * ca, y + 1.7, 7.3 * sa)], 0.16, M["silver"], sides=4, sub=1, part="grip_thread")
        pm = bl.mat("pin_head%d" % i, col, rough=0.15, bevel=0.0)
        bl.lathe("pin_head%d" % i, [(0.01, -0.55), (0.5, -0.3), (0.5, 0.3), (0.01, 0.55)], pm, sides=5, origin=(7.45 * ca, y + 1.8, 7.45 * sa), part="grip_thread")
    # ---- the stone on the spool, in a crown of claws
    bl.lathe("gem_bezel", [(5.0, 23.9), (6.1, 24.5), (6.0, 25.5), (5.1, 26.2)], M["silver"], sides=16, caps=(False, False))
    for k in range(8):
        a = 2 * math.pi * (k + 0.5) / 8
        ca, sa = math.cos(a), math.sin(a)
        bl.tube("claw%d" % k, [(5.7 * ca, 25.2, 5.7 * sa), (5.3 * ca, 27.0, 5.3 * sa), (4.1 * ca, 28.5, 4.1 * sa)], None, M["silver"], sides=4, sub=2, taper=(0.55, 0.22), part="gem_bezel")
    bl.lathe("gem", [(4.4, 24.4), (4.9, 25.6), (4.5, 27.4), (3.3, 29.3), (1.6, 30.35), (0.01, 30.7)], gemm, sides=8, caps=(False, True))
    # ---- the needle: its eye, its long point with a spiral cut
    band("needle_eye", (0, 37.1, 0), 2.6, 8.6, bl.circle(0.9, 6), needle, n=22, plane="xy")
    bl.lathe("needle_point", [(1.3, 45.0), (2.25, 48.0), (2.2, 60.0), (1.75, 84.0), (0.95, 102.0), (0.01, 111.0)], needle, sides=10, caps=(False, True))
    # ---- the silk: from the spool through the eye, wound up the needle, then flying free
    def nr(y):
        return 2.25 if y < 60 else 2.2 - 0.45 * (y - 60) / 24 if y < 84 else 1.75 - 0.8 * (y - 84) / 18 if y < 102 else max(0.95 - 0.95 * (y - 102) / 9, 0.05)
    path = bl.smooth_path([(5.5, 21.3, 1.5), (5.2, 26.0, 3.2), (2.4, 33.0, 2.6), (0.0, 39.0, 0.0), (-1.2, 44.0, -2.0)], 3)
    turns, n = 4.5, 44
    for k in range(n + 1):
        t = k / n
        y = 47.0 + 53.0 * t
        a = math.pi + 2 * math.pi * turns * t
        r = nr(y) + 0.75
        path.append(Vector((r * math.cos(a) * (1 if t > 0.04 else 0.6), y, r * math.sin(a))))
    path += bl.smooth_path([tuple(path[-1]), (2.6, 104.5, 2.6), (-2.0, 107.9, 3.0), (-8.0, 104.0, 0.5), (-13.2, 93.0, -2.0), (-15.6, 80.0, 0.0), (-12.6, 69.0, 2.4),
                            (-15.4, 58.0, 0.6)], 3)[1:]
    bl.sweep("thread_trail", path, bl.circle(0.45, 4), silk)
    # ---- the cord round the spool and the scissors that hang from it
    band("cord", (0, 18.6, 0), 6.0, 6.0, bl.circle(0.4, 5), cordm, n=18, plane="xz")
    bl.tube("cord_end", [(-5.9, 18.6, 0.0), (-9.5, 19.6, 0.0), (-13.0, 18.6, 0.0), (-15.6, 16.2, 0.0)], 0.36, cordm, sides=5, sub=3, part="cord")
    P = (-28.0, 11.0, 0.0)
    for s in (1, -1):
        z0, z1 = (0.06, 0.56) if s > 0 else (-0.56, -0.06)
        blade = bl.bevel(bl.plate("blade%d" % s, [(1.6, -0.95 * s), (1.6, 0.95 * s), (-6.0, 1.25 * s), (-19.85, 0.1 * s), (-6.0, -0.85 * s)], z0, z1, M["silver"], part="scissors"), 0.12)
        shank = bl.plate("shank%d" % s, [(1.2, -0.7 * s), (1.2, 0.7 * s), (7.9, -1.9 * s), (8.5, -3.0 * s)], z0, z1, M["silver"], part="scissors")
        ringo = band("sring%d" % s, (10.7, -3.7 * s, (z0 + z1) / 2), 2.55, 2.55, bl.circle(0.5, 5), M["silver"], n=14, plane="xy", part="scissors")
        edge = stroke("sedge%d" % s, (-6.0, -0.6 * s), (-19.0, 0.0), 0.3, z1 if s > 0 else z0, M["dark_f"], w1=0.06)
        group_move([blade, shank, ringo, edge], rot=(0, 0, -6.0 * s))
        group_move([blade, shank, ringo, edge], loc=P)
    bl.lathe("pivot", [(0.01, -1.05), (0.8, -0.85), (0.8, 0.85), (0.01, 1.05)], M["gold"], sides=8, origin=P, axis="z", part="scissors")
    return write("vassal_sewing_set", out)


# ================================================================ the carriage
def carriage(out):
    """Mesh space of the carriage page: +y the nose, -x up (the roof), z the width, the ground at x = 0. Built as (u up, f forward, s side)."""
    start()
    M = mats()
    cream = bl.mat("cream", rgb(242, 234, 210), rough=0.5, bevel=0.3)
    blue = bl.mat("blue", rgb(34, 62, 150), rough=0.45, bump="leather", bstr=0.3, groove=rgb(14, 26, 78), gamount=0.5, bevel=0.3)
    blue_f = bl.mat("blue_f", rgb(40, 74, 170), rough=0.45, bevel=0.0)
    wood = bl.mat("wood", rgb(120, 70, 36), rough=0.45, bump="grain", bscale=0.4, bstr=0.25, groove=rgb(78, 42, 20), gamount=0.4, bevel=0.2)
    twist = bl.mat("twist", rgb(240, 186, 82), metallic=1.0, rough=0.26, bump="wrap", bscale=0.5, bstr=0.9, groove=rgb(150, 98, 30), gamount=1.0, bevel=0.0)
    amber = glow("amber", rgb(240, 140, 30), rgb(255, 160, 40), 0.38, rough=0.08, metallic=0.2, bevel=0.05)
    lit = glow("lit", rgb(200, 150, 50), rgb(255, 195, 90), 0.9)
    plume = glow("plume", rgb(232, 240, 252), rgb(190, 220, 255), 0.18, rough=0.6)
    plume_f = bl.mat("plume_f", rgb(150, 180, 226), rough=0.6, bevel=0.0)
    Y0, Y1 = -0.137, 119.866

    def C(u, f, s):
        return Vector((-u, f, s))
    # ---- wheels: tyre, felloe, twelve spokes, hub with a lit boss; an axle between
    WU, WF = 20.4, 38.0
    for nm, s0, sg in (("wheel_left", -33.0, -1), ("wheel_right", 33.0, 1)):
        band(nm, (-WU, WF, s0), 19.4, 19.4, bl.rrect(0.977, 1.6, 0.4, n=1), M["gold"], n=24, plane="xy")
        band(nm + "_felloe", (-WU, WF, s0), 17.6, 17.6, [(0.9, 1.1), (-0.9, 1.1), (-0.9, -1.1), (0.9, -1.1)], wood, n=24, plane="xy", part=nm)
        for k in range(12):
            a = 2 * math.pi * k / 12
            ca, sa = math.cos(a), math.sin(a)
            p0, p1 = Vector((-WU + 3.4 * ca, WF + 3.4 * sa, s0)), Vector((-WU + 16.9 * ca, WF + 16.9 * sa, s0))
            t = Vector((-sa, ca, 0.0))
            rg = [[p + t * (w * i) + Vector((0, 0, h * j)) for i, j in ((1, 1), (-1, 1), (-1, -1), (1, -1))] for p, w, h in ((p0, 1.0, 0.9), (p1, 0.62, 0.7))]
            bl.loft("%s_spoke%d" % (nm, k), rg, wood, part=nm, caps=(False, False))
        zo, zi = (35.3 * sg, 29.5 * sg)
        dome(nm + "_light", 1.7, 0.7, amber, (-WU, WF, zo), axis="z", sides=8, steps=2, sign=sg, part="gem")
        prof = [(0.01, zo), (2.0, zo - 0.25 * sg), (2.9, zo - 1.0 * sg), (4.4, zo - 1.6 * sg), (4.4, zi + 1.2 * sg), (2.6, zi)]
        bl.lathe(nm + "_hub", prof if sg < 0 else prof[::-1], M["gold"], sides=12, origin=(-WU, WF, 0.0), axis="z", part=nm, caps=(False, False))
    bl.lathe("axle", [(1.9, -30.0), (1.9, 30.0)], wood, sides=8, origin=(-WU, WF, 0.0), axis="z", caps=(False, False))
    # ---- chassis and hull
    bl.bevel(box("chassis", (-13.0, 0.4, -25.6), (-4.8, 84.0, 25.6), wood), 0.6)
    for sg in (1, -1):                                                   # the hull's sides: high at the back, dipped at the door, a raised dash
        top = [(0.0, 38.5), (14.0, 38.5), (24.0, 36.0), (32.0, 28.5), (44.0, 27.0), (52.0, 30.0), (62.0, 34.5), (74.0, 35.5), (87.1, 31.0)]
        outl = [(-12.3, Y0), (-12.3, 87.1)] + [(-u, f if f > 0.01 else Y0) for f, u in reversed(top)]
        z0, z1 = (25.3, 27.3) if sg > 0 else (-27.3, -25.3)
        bl.bevel(bl.plate("hull%d" % sg, outl, z0, z1, cream, part="cabin"), 0.3)
        zf = 27.3 * sg
        trim = [(-13.8, 2.0), (-13.8, 85.2)] + [(-u + 1.7, min(max(f, 2.0), 85.2)) for f, u in reversed(top)]
        belt = [(-u + 3.4, min(max(f, 2.2), 85.0)) for f, u in top]
        for i, (p, q) in enumerate(zip(belt, belt[1:])):                 # a royal blue belt under the gold line of the hull's top edge
            stroke("belt%d%d" % (sg, i), p, q, 3.0, zf - 0.01 * sg, blue_f)
        strokes("hull_trim%d" % sg, trim, 0.6, zf + 0.012 * sg, M["gold_f"])
        door = [(-14.8, 30.0), (-14.8, 52.0), (-25.0, 52.0), (-26.4, 44.0), (-26.4, 33.0), (-25.0, 30.0)]
        flat("door%d" % sg, door, zf, blue_f)
        strokes("door_line%d" % sg, door, 0.45, zf + 0.012 * sg, M["gold_f"])
        flat("panel_a%d" % sg, star((-24.5, 9.0), 5.0, 2.0, 8), zf, lit)
        ring_line("panel_ar%d" % sg, ell(6.4, 6.4, 20, (-24.5, 9.0)), 0.5, zf, M["gold_f"])
        flat("panel_b%d" % sg, star((-23.0, 72.5), 4.4, 1.8, 4), zf, M["gold_f"])
        # a scalloped valance under the eave, gold-edged, a light at each point
        val = [(-60.2, 0.6), (-60.2, 88.4)]
        for k in range(8, 0, -1):
            f1, f0 = 0.6 + 87.8 * k / 8, 0.6 + 87.8 * (k - 1) / 8
            val += [(-55.6, f1 - 1.2), (-53.4, (f0 + f1) / 2), (-55.6, f0 + 1.2)]
        zv = 31.9 * sg
        bl.plate("valance%d" % sg, val, zv - 0.3, zv + 0.3, blue, part="canopy")
        for k in range(8):
            fm = 0.6 + 87.8 * (k + 0.5) / 8
            for zz in (zv + 0.3, zv - 0.3):
                flat("val_dot%d%d%d" % (sg, k, zz > zv), ell(0.9, 0.9, 8, (-55.2, fm)), zz, lit)
                stroke("val_l%d%d%d" % (sg, k, zz > zv), (-56.2, fm - 4.6), (-54.0, fm), 0.45, zz, M["gold_f"])
                stroke("val_r%d%d%d" % (sg, k, zz > zv), (-54.0, fm), (-56.2, fm + 4.6), 0.45, zz, M["gold_f"])
    box("hull_rear", (-38.5, Y0, -25.3), (-12.3, 1.9, 25.3), cream, part="cabin")
    def rear(nm, pts, m, lift=0.0):                                      # a bake-only plate on the hull's back (it looks along -y)
        return prism(nm, [(x, Y0 + 0.03 - lift, z) for x, z in pts], (0, -0.06, 0), m, detail=True)
    rear("rear_field", [(-36.0, -22.5), (-14.8, -22.5), (-14.8, 22.5), (-36.0, 22.5)], blue_f)
    rear("rear_star", star((-25.4, 0.0), 7.6, 3.0, 8), lit, 0.02)
    for i, (a, b) in enumerate((((-36.6, -23.2), (-36.6, 23.2)), ((-14.2, -23.2), (-14.2, 23.2)), ((-36.6, -23.2), (-14.2, -23.2)), ((-36.6, 23.2), (-14.2, 23.2)))):
        d = math.hypot(b[0] - a[0], b[1] - a[1])
        nx, nz = -(b[1] - a[1]) / d * 0.35, (b[0] - a[0]) / d * 0.35
        rear("rear_line%d" % i, [(a[0] - nx, a[1] - nz), (b[0] - nx, b[1] - nz), (b[0] + nx, b[1] + nz), (a[0] + nx, a[1] + nz)], M["gold_f"], 0.01)
    box("hull_dash", (-31.0, 85.2, -25.3), (-12.3, 87.1, 25.3), cream, part="cabin")
    box("hull_floor", (-14.2, 1.9, -25.3), (-12.3, 85.2, 25.3), wood, part="cabin")
    # ---- benches (the seat surfaces are where CarriageNumbers.SEATS puts the riders: 18.75 above the ground)
    def bench(nm, f0, f1, half, back_top, cuts):
        bl.loft(nm, [[C(14.2, f0, s), C(18.75, f0, s), C(18.75, f1, s), C(14.2, f1, s)] for s in cuts], blue, caps=(True, True))
        bl.loft(nm + "_back", [[C(18.4, f0 - 1.6, s), C(back_top, f0 - 1.6, s), C(back_top, f0 + 1.2, s), C(18.4, f0 + 1.2, s)] for s in cuts], blue, part=nm)
        for i, s in enumerate(cuts[1:-1]):
            for j, f in enumerate((f0 + (f1 - f0) * 0.3, f0 + (f1 - f0) * 0.7)):
                prism("%s_btn%d%d" % (nm, i, j), [C(18.72, f + 0.7 * math.cos(a), s + 0.7 * math.sin(a)) for a in [2 * math.pi * k / 6 for k in range(6)]], (-0.06, 0, 0), M["gold_f"], detail=True)
    bench("bench_front", 54.0, 70.4, 12.0, 27.5, [-12.0, -4.0, 4.0, 12.0])
    bench("bench_rear", 9.6, 27.2, 24.0, 34.5, [-24.0, -17.0, -8.5, 0.0, 8.5, 17.0, 24.0])
    # ---- posts, mudguards and their stays, the crown round the roof jewel
    for i, (f, s) in enumerate(((6.0, 24.2), (6.0, -24.2), (72.0, 24.2), (72.0, -24.2))):
        post(i, f, s, twist)
    for sg in (1, -1):
        pa = []
        for k in range(13):
            a = math.radians(18.0 + 144.0 * k / 12)
            pa.append(Vector((-WU - 23.6 * math.sin(a), WF + 23.6 * math.cos(a), 32.6 * sg)))
        bl.sweep("mudguard%d" % sg, pa, [(-0.45, -3.0), (0.45, -3.0), (0.45, 3.0), (-0.45, 3.0)], M["gold"], part="posts")
        for j, k in enumerate((2, 6, 10)):
            bl.tube("stay%d%d" % (sg, j), [tuple(pa[k] + Vector((0, 0, -2.6 * sg))), (pa[k].x * 0.25 - 26.0 * 0.75, pa[k].y, 27.0 * sg)], 0.5, M["gold"], sides=4, sub=1, part="posts")
    # ---- the canopy: a hipped roof with a flat crown, a star map set in it
    cuts = [(-32.1, 60.0), (-24.0, 64.6), (-12.0, 69.6), (0.0, 69.6), (12.0, 69.6), (24.0, 64.6), (32.1, 60.0)]
    fs = [0.0, 11.0, 22.0, 33.0, 44.5, 56.0, 67.0, 78.0, 89.0]
    rings = []
    for i, f in enumerate(fs):
        k = 0.82 if i in (0, len(fs) - 1) else 1.0                         # the ends drop a little: a hip at the front and the back
        top = [C(58.0 + (u - 58.0) * k, f, s) for s, u in cuts]
        rings.append(top + [C(58.0, f, s) for s, _u in reversed(cuts)])
    bl.loft("canopy", rings, blue)
    def roof(nm, pts, m, lift=0.0):
        return prism(nm, [C(69.57 + lift, f, s) for f, s in pts], (-0.06, 0, 0), m, detail=True)
    roof("roof_field", [(13.0, -10.6), (76.0, -10.6), (76.0, 10.6), (13.0, 10.6)], blue_f)
    stars = [(20.0, -6.0), (30.0, 4.5), (38.0, -3.0), (52.0, 6.5), (60.0, -5.5), (70.0, 2.0)]
    for i, (f, s) in enumerate(stars):
        roof("roof_star%d" % i, [(f + (1.5 if k % 2 == 0 else 0.5) * math.cos(math.pi * k / 4), s + (1.5 if k % 2 == 0 else 0.5) * math.sin(math.pi * k / 4)) for k in range(8)], lit, 0.02)
        if i:
            (f0, s0), (f1, s1) = stars[i - 1], (f, s)
            d = math.hypot(f1 - f0, s1 - s0)
            nx, ny = -(s1 - s0) / d * 0.14, (f1 - f0) / d * 0.14
            roof("roof_link%d" % i, [(f0 - nx, s0 - ny), (f1 - nx, s1 - ny), (f1 + nx, s1 + ny), (f0 + nx, s0 + ny)], M["gold_f"], 0.01)
    for sg in (1, -1):                                                   # gold rails along the eaves
        bl.tube("eave%d" % sg, [C(59.4, 0.5, 32.1 * sg), C(60.3, 44.5, 32.1 * sg), C(59.4, 88.5, 32.1 * sg)], 0.9, M["gold"], sides=5, sub=4, part="posts")
    band("crown", (-70.4, 44.5, 0.0), 4.7, 4.7, bl.circle(0.6, 4), M["gold"], n=12, plane="yz", part="posts")
    for k in range(8):
        a = 2 * math.pi * k / 8
        bl.tube("prong%d" % k, [C(70.4, 44.5 + 4.7 * math.cos(a), 4.7 * math.sin(a)), C(73.2, 44.5 + 5.2 * math.cos(a), 5.2 * math.sin(a)), C(74.7, 44.5 + 4.2 * math.cos(a), 4.2 * math.sin(a))],
                None, M["gold"], sides=4, sub=1, taper=(0.5, 0.15), part="posts")
    bl.lathe("roof_gem", [(0.01, -77.0), (2.7, -74.4), (3.4, -72.6), (2.2, -70.6), (0.01, -69.5)], amber, sides=6, origin=(0.0, 44.5, 0.0), axis="x", part="gem")
    # ---- draught pole and yoke
    bl.tube("pole", [C(11.0, 78.0, 0), C(13.5, 90.0, 0), C(19.5, 103.0, 0), (-26.6, 115.2, 0.0)], None, wood, sides=8, sub=3, taper=(2.05, 1.5))
    bl.tube("yoke", [(-28.4, 113.4, -17.6), (-26.4, 115.2, -12.0), (-26.6, 115.4, 0.0), (-26.4, 115.2, 12.0), (-28.4, 113.4, 17.6)], 1.45, wood, sides=6, sub=2)
    for sg in (1, -1):
        bl.lathe("yoke_cap%d" % sg, [(1.7, 16.6 * sg), (1.9, 17.4 * sg), (1.2, 18.1 * sg), (0.01, 18.3 * sg)][::sg], M["gold"], sides=8, origin=(-28.5, 113.3, 0.0), axis="z", part="yoke",
                 caps=(False, False))
    bl.lathe("pole_tip", [(1.5, 114.6), (2.1, 116.0), (1.7, 117.6), (0.9, 118.9), (0.01, Y1)], M["gold"], sides=8, origin=(-26.6, 0.0, 0.0), part="yoke", caps=(False, True))
    # ---- lantern on its bracket at the front of the roof
    LF = 92.2
    bl.tube("lantern_arm", [C(63.5, 88.6, 0), C(65.0, 90.6, 0), C(64.2, LF, 0)], 0.6, M["gold"], sides=5, sub=3, part="lantern")
    bl.lathe("lantern_top", [(0.01, -64.3), (1.0, -63.6), (3.4, -61.0), (3.6, -60.2), (2.6, -60.0)], M["gold"], sides=8, origin=(0.0, LF, 0.0), axis="x", part="lantern", caps=(False, False))
    bl.lathe("lantern_base", [(2.6, -52.6), (3.5, -52.2), (3.2, -51.2), (1.4, -49.6), (0.01, -48.6)], M["gold"], sides=8, origin=(0.0, LF, 0.0), axis="x", part="lantern", caps=(False, False))
    bl.lathe("lantern_light", [(2.5, -60.1), (3.0, -58.0), (3.0, -54.6), (2.5, -52.5)], amber, sides=8, origin=(0.0, LF, 0.0), axis="x", part="gem", caps=(False, False))
    for k in range(4):
        a = math.pi / 4 + math.pi * k / 2
        bl.tube("lantern_bar%d" % k, [C(60.1, LF + 3.15 * math.cos(a), 3.15 * math.sin(a)), C(52.5, LF + 3.15 * math.cos(a), 3.15 * math.sin(a))], 0.3, M["gold"], sides=4, sub=1, part="lantern")
    # ---- plumes: three feathers a side, rising from the front of the roof
    for sg in (1, -1):
        for i, (bf, tf, tu, hw, s) in enumerate(((80.0, 88.4, 92.0, 4.2, 20.0), (78.0, 78.0, 99.386, 4.8, 23.5), (76.0, 67.6, 91.0, 4.0, 27.0))):
            z = s * sg
            bl.plate("plume%d%d" % (sg > 0, i), leaf((-63.6, bf), (-tu, tf), hw, 5), z - 0.4, z + 0.4, plume, part="plumes")
            for zz in (z + 0.4, z - 0.4):
                stroke("pvein%d%d%d" % (sg > 0, i, zz > z), (-64.5, bf), (-tu + 2.0, tf + (bf - tf) * 0.06), 0.5, zz, plume_f, w1=0.1)
    return write("vassal_carriage", out)


def post(i, f, s, material):
    """A twisted corner post from the hull to the roof (the lathe's axis is x: up)."""
    return bl.lathe("post%d" % i, [(1.9, -28.0), (1.3, -30.5), (1.3, -56.5), (1.9, -58.2), (2.4, -59.6)], material, sides=6, origin=(0.0, f, s), axis="x", part="posts", caps=(False, False))


BUILDERS = {"carriage": carriage, "book": book, "mirror": mirror, "instrument": instrument, "sewing_set": sewing_set}


def main(argv):
    global FAST
    only = trial = prev = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
        if a == "--preview":
            prev = argv[i + 1]
        if a == "--fast":
            FAST = True
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, "vassal_" + name + "_preview.png"))
        elif prev:
            out = dict(preview=os.path.join(prev, "vassal_" + name + "_preview.png"))
        fn(out)


if __name__ == "__main__":
    main(sys.argv[1:])

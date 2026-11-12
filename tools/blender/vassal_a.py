"""Five Vassal Weapons made in Blender (3.80.0): katana, scythe, fan, harpoon, chain.
Run: python tools/blender/vassal_a.py [--only katana,scythe,fan,harpoon,chain] [--trial DIR] [--fast]

--trial DIR writes the meshes, skins and previews into DIR instead of the mod's assets; --fast bakes small and noisy (for looking only).
Each weapon keeps the box (length, grip position, width, thickness) of the mesh it replaces and that mesh's `place`, `grip` and `orient`
lines (--headers DIR: copies of the replaced mesh files; default: the files in the assets), so the hand, the display transforms, the
size class and the grip table still fit. See bl_lib.py for what Blender does and what the mod's tools do.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402
from legendary import RUNES, metals, rune_font  # noqa: E402
from mathutils import Vector  # noqa: E402

FAST = False
HEADERS = bl.MESH_DIR


# ================================================================ helpers
def reset():
    bl.reset(samples=8 if FAST else 24)


def done(item_id, out, glow):
    """Bakes and writes the weapon, then gives the file the `grip` / `orient` lines of the mesh it replaces (bl_lib always writes
    `orient manual` and derives `grip` from part names; these weapons are held exactly as before only with the old lines)."""
    hdr = os.path.join(HEADERS, item_id + ".txt")
    keep = {}
    with open(hdr) as fh:
        for line in fh:
            w = line.split()
            if w and w[0] in ("grip", "orient"):
                keep[w[0]] = line.rstrip("\n")
            if w and w[0] in ("q", "qn"):
                break
    res = bl.finish(item_id, glow=glow, header_from=hdr, bake=512 if FAST else 1024, **out)
    path = os.path.join(out.get("out_mesh") or bl.MESH_DIR, item_id + ".txt")
    lines = []
    with open(path) as fh:
        for line in fh.read().split("\n"):
            w = line.split()
            if w and w[0] in ("grip", "orient"):
                continue
            lines.append(line)
            if w and w[0] == "place":
                lines += [keep[k] for k in ("grip", "orient") if k in keep]
    with open(path, "w", newline="\n") as fh:
        fh.write("\n".join(lines))
    return res


def sharpen(m, lo, hi):
    """Makes a material's two colours meet at an edge instead of a blend: a smooth step on the factor of its colour mix (bl.mat mixes
    colour and groove linearly, which greys a wrap whose gaps should show clean ray skin)."""
    nt = m.node_tree
    mix = next(n for n in nt.nodes if n.type == "MIX_RGB")
    src = mix.inputs[0].links[0].from_socket
    mr = nt.nodes.new("ShaderNodeMapRange")
    mr.interpolation_type = "SMOOTHSTEP"
    mr.inputs["From Min"].default_value = lo
    mr.inputs["From Max"].default_value = hi
    nt.links.new(src, mr.inputs["Value"])
    nt.links.new(mr.outputs[0], mix.inputs[0])
    return m


def frames(path):
    """[(point, in-plane normal)] of a path drawn in the xy plane, the same frame bl.sweep uses with up = z."""
    P = [Vector(p) for p in path]
    out = []
    for i, p in enumerate(P):
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        out.append((p, Vector((0, 0, 1)).cross(tan).normalized(), tan))
    return out


def band(name, fr, a, b, z0, z1, material, detail=True, part=None):
    """A flat strip beside a path: between the offsets a and b (numbers or functions of t in 0..1) along the path's normal."""
    n = len(fr)
    fa = a if callable(a) else (lambda t: a)
    fb = b if callable(b) else (lambda t: b)
    out = [(p + nr * fa(i / (n - 1))) for i, (p, nr, _t) in enumerate(fr)] + [(p + nr * fb(i / (n - 1))) for i, (p, nr, _t) in reversed(list(enumerate(fr)))]
    return bl.plate(name, [(v.x, v.y) for v in out], min(z0, z1), max(z0, z1), material, part=part, detail=detail)


def sheet(name, rows, material, thick=0.04, s=1, detail=True, part=None):
    """A thin slab over a surface: rows of 3D points (each row the same length) are its top; it is `thick` deep towards -s z."""
    n, m = len(rows), len(rows[0])
    verts = [Vector(p) for r in rows for p in r] + [Vector(p) - Vector((0, 0, thick * s)) for r in rows for p in r]
    faces = []
    for i in range(n - 1):
        for k in range(m - 1):
            a = i * m + k
            faces.append((a, a + 1, a + m + 1, a + m))
            faces.append((a + n * m, a + m + n * m, a + m + 1 + n * m, a + 1 + n * m))
    edge = [k for k in range(m)] + [i * m + m - 1 for i in range(1, n)] + [(n - 1) * m + k for k in range(m - 2, -1, -1)] + [i * m for i in range(n - 2, 0, -1)]
    for a, b in zip(edge, edge[1:] + edge[:1]):
        faces.append((a, b, b + n * m, a + n * m))
    return bl.obj(name, verts, faces, material, part, detail)


def yplate(name, outline, y0, y1, material, part=None, detail=False):
    """A flat piece lying across the weapon: an outline [(x, z)] from height y0 to y1."""
    n = len(outline)
    verts = [(x, y0, z) for x, z in outline] + [(x, y1, z) for x, z in outline]
    faces = [(k, (k + 1) % n, (k + 1) % n + n, k + n) for k in range(n)] + [tuple(range(n - 1, -1, -1)), tuple(range(n, 2 * n))]
    return bl.obj(name, verts, faces, material, part, detail)


def arc(r, a0, a1, n, c=(0.0, 0.0)):
    return [(c[0] + r * math.cos(math.radians(a0 + (a1 - a0) * k / (n - 1))), c[1] + r * math.sin(math.radians(a0 + (a1 - a0) * k / (n - 1)))) for k in range(n)]


def sector(name, r0, r1, a0, a1, n, z0, z1, material, c=(0.0, 0.0), detail=True, part=None):
    return bl.plate(name, arc(r1, a0, a1, n, c) + arc(r0, a1, a0, n, c), min(z0, z1), max(z0, z1), material, part=part, detail=detail)


def cone(name, r, h, material, loc, rot=(0, 0, 0), sides=6, lift=0.0, part=None, waist=0.45):
    """A spike: built along +y, lifted, turned (degrees, XYZ), then moved."""
    ob = bl.lathe(name, [(r, 0.0), (r * waist, h * 0.6), (0.01, h)], material, sides=sides, part=part)
    bl.move(ob, (0, lift, 0))
    bl.move(ob, rot=rot)
    return bl.move(ob, loc)


def flower(name, r, z0, z1, material, c, petals=5, turn=0.0, notch=0.0):
    """A blossom outline in the xy plane (a tsuba's or a painted flower's): petals round a centre."""
    pts = []
    n = petals * 12
    for k in range(n):
        a = 2 * math.pi * k / n
        ph = a - turn
        lobe = abs(math.cos(petals * ph / 2.0)) ** 0.5
        d = ((ph * petals / (2 * math.pi)) % 1.0)
        d = min(d, 1.0 - d) * (2 * math.pi / petals)
        rr = r * (0.6 + 0.4 * lobe - notch * math.exp(-(d / 0.07) ** 2))
        pts.append((c[0] + rr * math.cos(a), c[1] + rr * math.sin(a)))
    return bl.plate(name, pts, min(z0, z1), max(z0, z1), material, detail=True)


# ================================================================ katana
def katana(out):
    reset()
    M = metals()
    steel = bl.mat("k_steel", rgb(150, 160, 186), metallic=1.0, rough=0.28, bump="brushed", bstr=0.14, bevel=0.08, groove=rgb(104, 114, 146), gamount=0.5)
    ha = bl.mat("k_ha", rgb(248, 250, 255), metallic=1.0, rough=0.1, bevel=0.0)
    hi = bl.mat("k_hi", rgb(30, 34, 62), metallic=1.0, rough=0.4, bevel=0.0)
    pinkline = bl.mat("k_pinkline", rgb(60, 10, 30), rough=0.5, emit=rgb(255, 105, 175), strength=1.15, bevel=0.0)
    pink = bl.mat("k_pink", rgb(255, 96, 160), metallic=0.3, rough=0.08, emit=rgb(255, 95, 160), strength=0.45, bevel=0.04)
    lacquer = bl.mat("k_lacquer", rgb(20, 16, 28), rough=0.25, bevel=0.0)
    wrap = sharpen(bl.mat("k_wrap", rgb(24, 32, 96), rough=0.6, bump="cross", bscale=1.25, bstr=0.9, groove=rgb(236, 230, 208), gamount=1.0, bevel=0.0), 0.18, 0.42)
    cord = bl.mat("k_cord", rgb(200, 26, 70), rough=0.6, bump="wrap", bscale=2.5, bstr=0.6, groove=rgb(90, 8, 30), gamount=0.8, bevel=0.0)
    habaki = bl.mat("k_habaki", rgb(240, 186, 82), metallic=1.0, rough=0.24, bump="rings", bscale=3.2, bstr=0.25, bevel=0.15, groove=rgb(170, 112, 38), gamount=0.5)
    Y0, Y1, T, W0 = 30.0, 102.6, 0.85, 2.8
    YK = 92.0                                                                # the kissaki begins

    def hw(y):
        if y <= YK:
            return W0 - 0.45 * (y - Y0) / (YK - Y0)
        u = (y - YK) / (Y1 - YK)
        return max(0.05, (W0 - 0.45) * (1.0 - u ** 1.7))

    def cx(y):                                                               # the back line keeps its curve, the edge runs to it in the point
        u = max(0.0, (y - Y0) / (Y1 - Y0))
        return -3.25 * u * u - ((W0 - 0.45) - hw(y) if y > YK else 0.0)

    def th(y):
        return T if y <= YK else T * (0.35 + 0.65 * (1 - (y - YK) / (Y1 - YK)))

    def sec(y):
        w, t = hw(y), th(y)
        return [(w, 0), (-0.2 * w, t), (-w, 0.6 * t), (-w, -0.6 * t), (-0.2 * w, -t)]

    def ji(y, k, s, lift):                                                   # a point on the edge-side face: k 0 at the ridge, 1 at the edge
        w, t = hw(y), th(y)
        xr = -0.2 * w + k * 1.2 * w
        return (cx(y) + xr, y, s * (t * (1 - k) + lift))

    def shi(y, k, s, lift):                                                  # a point on the back-side face: k 0 at the back, 1 at the ridge
        w, t = hw(y), th(y)
        return (cx(y) - w + k * 0.8 * w, y, s * (t * (0.6 + 0.4 * k) + lift))

    ys = [30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 89, 92, 95, 97.5, 99.5, 101.2, 102.2, 102.6]
    bl.loft("blade", [bl.ring(y, sec(y), ox=cx(y)) for y in ys], steel)
    for s in (1, -1):
        yy = [38 + 0.75 * i for i in range(81)]                              # the hamon: a polished wave along the edge
        rows = []
        for y in yy:
            k0 = 0.42 + 0.13 * math.sin(y * 0.82) + 0.06 * math.sin(y * 2.1 + 1.0)
            k0 = k0 + (1 - k0) * 0.0 if y < YK else min(0.9, k0 + 0.5 * (y - YK) / (Y1 - YK))
            rows.append([ji(y, k0 + (0.985 - k0) * j / 3.0, s, 0.05) for j in range(4)])
        sheet("hamon%d" % s, rows, ha, s=s)
        yy = [40 + 2.0 * i for i in range(24)]                               # the groove of the back face, and its line of light
        sheet("hi%d" % s, [[shi(y, 0.2 + 0.6 * j / 2.0, s, 0.05) for j in range(3)] for y in yy], hi, s=s)
        sheet("hiline%d" % s, [[shi(y, 0.44 + 0.12 * j, s, 0.1) for j in range(2)] for y in yy[1:-1]], pinkline, s=s)
        yy = [36.5, 40, 44, 47, 50.5]                                        # a gold tongue up the back face from the habaki
        sheet("langet%d" % s, [[shi(y, 0.05, s, 0.13), shi(y, 0.05 + (0.9 if y < 47 else 0.9 * (50.5 - y) / 3.5 + 0.02), s, 0.13)] for y in yy], M["gold_line"], s=s)
    # ---- habaki with a blossom-pink stone on both faces
    bl.loft("habaki", [bl.ring(y, bl.rrect(w, t, 0.55), ox=cx(y)) for y, w, t in ((28.8, 3.35, 1.95), (32.5, 3.3, 1.9), (35.6, 3.1, 1.55), (37.1, 2.95, 1.2))], habaki)
    g = bl.gem("habaki_gem", 1.5, pink, center=(0, 32.6, 1.78), depth=0.3, crown=0.2, sides=6, axis="z")
    bl.copy(g, "habaki_gem_b", scale=(1, 1, -1))
    # ---- tsuba: a cherry blossom of gold, lacquer and light
    def blossom(R, n=60, notch=0.08):
        pts = []
        for k in range(n):
            a = 2 * math.pi * k / n
            ph = a - math.pi / 2
            d = ((ph * 5 / (2 * math.pi)) % 1.0)
            d = min(d, 1.0 - d) * (2 * math.pi / 5)
            rr = R * (0.66 + 0.34 * abs(math.cos(2.5 * ph)) ** 0.5 - notch * math.exp(-(d / 0.08) ** 2))
            pts.append((rr * math.cos(a), rr * math.sin(a)))
        return pts
    yplate("tsuba", blossom(5.7), 29.4, 30.4, M["gold"])
    rim = [(x, 29.9, z) for x, z in blossom(5.72)]                            # a raised rim round the blossom
    bl.sweep("tsuba_rim", rim + rim[:1], [(0.33, 0.45), (-0.33, 0.7), (-0.33, -0.7), (0.33, -0.45)], M["gold"], up=(0, 1, 0), caps=(False, False), part="tsuba")
    for s, y in ((1, 30.4), (-1, 29.4)):
        yplate("ts_pink%d" % s, blossom(5.05), y + 0.02 * s, y + 0.05 * s, pinkline, detail=True)
        yplate("ts_lac%d" % s, blossom(4.65, notch=0.1), y + 0.05 * s, y + 0.09 * s, lacquer, detail=True)
        for k in range(5):                                                   # five gold petals on the lacquer
            a = math.pi / 2 + 2 * math.pi * k / 5
            c, sn = math.cos(a), math.sin(a)
            pet = [(2.6, 0.0), (3.2, 0.55), (3.85, 0.42), (4.2, 0.0), (3.85, -0.42), (3.2, -0.55)]
            yplate("ts_petal%d%d" % (s, k), [(u * c - v * sn, u * sn + v * c) for u, v in pet], y + 0.09 * s, y + 0.13 * s, M["gold_line"], detail=True)
    bl.lathe("guard_collar", [(2.1, 27.8), (2.5, 28.3), (2.5, 29.1), (2.2, 29.5)], M["gold"], sides=12, squash=0.86)
    # ---- the hilt: wrap over ray skin, menuki, kashira, the blossom drop
    bl.lathe("tsuka_wrap", [(1.95, 28.2), (1.88, 25.5), (1.82, 22.5), (1.78, 19.5), (1.76, 16.5), (1.76, 13.5), (1.78, 10.5), (1.82, 7.5), (1.88, 4.5), (1.95, 1.2)],
             wrap, sides=12, squash=0.86)
    m = bl.gem("menuki", 0.8, M["gold"], center=(0, 13.5, 1.5), depth=0.3, crown=0.25, sides=6, axis="z", rx=0.75, ry=4.6)
    bl.copy(m, "menuki_b", scale=(1, 1, -1))
    bl.lathe("pommel_cap", [(1.9, 3.4), (2.4, 2.6), (2.48, 1.2), (2.05, 0.25), (1.2, 0.0)], M["gold"], sides=12, squash=0.85)
    bl.lathe("pommel_gem", [(1.0, 0.45), (1.95, -1.1), (1.85, -2.2), (0.95, -3.6), (0.01, -4.3)], pink, sides=8, squash=0.92)
    # ---- sageo: a crimson cord and tassel
    bl.tube("sageo", [(2.0, 26.9, 0.6), (4.6, 26.6, 0.9), (6.3, 22.5, 0.4), (6.5, 16.5, 0.0), (6.45, 11.6, 0.0)], 0.42, cord, sides=5, sub=3)
    bl.lathe("sageo_knot", [(0.01, 12.3), (0.75, 12.0), (0.85, 11.4), (0.5, 10.9)], M["gold"], sides=6, origin=(6.45, 0, 0), part="sageo")
    bl.lathe("sageo_tassel", [(0.5, 11.0), (0.88, 10.0), (0.95, 6.2)], cord, sides=7, origin=(6.45, 0, 0), part="sageo")
    return done("vassal_katana", out, ("FFFFFF", 0.12, 0.4, 1.0))


# ================================================================ scythe
def scythe(out):
    reset()
    M = metals()
    steel = bl.mat("s_steel", rgb(206, 212, 228), metallic=1.0, rough=0.2, bump="brushed_x", bstr=0.1, bevel=0.2)
    ha = bl.mat("s_ha", rgb(250, 250, 255), metallic=1.0, rough=0.08, bevel=0.0)
    core = bl.mat("s_core", rgb(54, 40, 96), metallic=1.0, rough=0.36, bump="brushed_x", bstr=0.15, bevel=0.0, groove=rgb(26, 18, 52), gamount=0.6)
    violet = bl.mat("s_violet", rgb(40, 10, 70), rough=0.5, emit=rgb(185, 105, 255), strength=1.15, bevel=0.0)
    gemm = bl.mat("s_gem", rgb(140, 60, 235), metallic=0.3, rough=0.07, emit=rgb(150, 70, 255), strength=0.45, bevel=0.05)
    halo = bl.mat("s_halo", rgb(150, 110, 225), metallic=1.0, rough=0.3, emit=rgb(185, 120, 255), strength=0.7, bevel=0.0)
    shaft = bl.mat("s_shaft", rgb(46, 38, 64), rough=0.5, bump="grain", bstr=0.4, groove=rgb(18, 14, 28), gamount=0.6, bevel=0.0)
    wrap = bl.mat("s_wrap", rgb(170, 26, 48), rough=0.6, bump="cross", bscale=0.8, bstr=0.9, groove=rgb(50, 6, 16), gamount=1.0, bevel=0.0)
    dark = bl.mat("s_dark", rgb(60, 50, 90), metallic=1.0, rough=0.34, bump="hammer", bstr=0.1, bevel=0.2)
    cloth = bl.mat("s_cloth", rgb(120, 60, 200), rough=0.7, bump="brushed", bscale=0.6, bstr=0.3, groove=rgb(60, 24, 110), gamount=0.6, bevel=0.0)
    # ---- the blade: a great crescent with a back hook
    path = bl.smooth_path([(8, 213.5, 0), (-10, 222.5, 0), (-38, 231.0, 0), (-68, 223.5, 0), (-91, 196, 0), (-106.3, 151.5, 0)], 5)
    fr = frames(path)

    def wid(t):
        return 0.25 + 11.2 * (math.sin(math.pi * min(1.0, t * 1.05 + 0.12)) ** 0.75) * (1 - t ** 3.0)

    def thk(t):
        return 1.5 if t < 0.8 else 1.5 * (1 - (t - 0.8) / 0.2) + 0.12

    unit = [(1, 0), (0.15, 1), (-0.82, 1), (-1, 0.55), (-1, -0.55), (-0.82, -1), (0.15, -1)]
    bl.sweep("blade", path, unit, steel, scale=lambda t: (wid(t), thk(t)))
    hook = bl.smooth_path([(4, 213, 0), (14, 215.5, 0), (23, 219.5, 0), (29.6, 225.5, 0)], 3)
    bl.sweep("blade_hook", hook[::-1], unit, steel, scale=lambda t: (0.3 + 5.6 * t ** 0.8, 0.3 + 1.2 * t), part="blade")
    n = len(fr)
    i1 = int(n * 0.8)
    for s in (1, -1):
        z = 1.5 * s
        fl = fr[1:i1]
        tt = lambda t: (1 + t * (i1 - 2)) / (n - 1)                          # noqa: E731  (t along the strip to t along the path)
        band("s_core%d" % s, fl, lambda t: -0.78 * wid(tt(t)), lambda t: 0.1 * wid(tt(t)), z - 0.02 * s, z + 0.04 * s, core)
        band("s_line%d" % s, fl, lambda t: -0.6 * wid(tt(t)), lambda t: -0.6 * wid(tt(t)) + 0.7, z + 0.04 * s, z + 0.09 * s, violet)
        band("s_gold%d" % s, fl, lambda t: 0.1 * wid(tt(t)), lambda t: 0.1 * wid(tt(t)) + 0.45, z + 0.0, z + 0.07 * s, M["gold_line"])
        rows = []                                                            # the bright edge bevel
        for i in range(1, n - 2):
            p, nr, _t = fr[i]
            t = i / (n - 1)
            w, tk = wid(t), thk(t)
            rows.append([p + nr * (w * (0.19 + 0.79 * j / 2.0)) + Vector((0, 0, s * (tk * (1 - (0.04 + 0.79 * j / 2.0) / 0.85) + 0.05))) for j in range(3)])
        sheet("s_ha%d" % s, rows, ha, s=s)
        k = 0                                                                # runes along the blade's core, turned with the curve
        for i in range(6, i1 - 3, 3):
            p, nr, tan = fr[i]
            c = p + nr * (-0.25 * wid(i / (n - 1)))
            ang = math.degrees(math.atan2(-tan.y, -tan.x))
            bl.text("s_rune%d_%d" % (s, i), RUNES[(k * 5 + (0 if s > 0 else 3)) % len(RUNES)], 6.4, violet, (c.x, c.y, z + 0.1 * s),
                    rot=(0 if s > 0 else 180, 0, ang if s > 0 else -ang), extrude=0.03, font=rune_font())
            k += 1
    # the spine: a gold band and five fins
    bl.sweep("blade_spine_band", [p + nr * (-wid(i / (n - 1)) + 0.2) for i, (p, nr, _t) in enumerate(fr)][2:int(n * 0.74)], bl.rrect(1.0, 2.25, 0.5), M["gold"],
             scale=lambda t: (1.0 - 0.5 * t, 1.0 - 0.45 * t))
    fins = []
    for j, f in enumerate((0.14, 0.27, 0.40, 0.53, 0.66)):
        i = int(f * (n - 1))
        p, nr, tan = fr[i]
        w = wid(i / (n - 1))
        b0, b1 = p - nr * (w - 0.3) - tan * 3.2, p - nr * (w - 0.3) + tan * 3.2
        tip = p - nr * (w + 9.2 - 1.2 * j) - tan * (4.5 + j)
        fins.append([(b0.x, b0.y), (b1.x, b1.y), (tip.x, tip.y)])
    top = max(range(len(fins)), key=lambda j: fins[j][2][1])
    fins[top][2] = (fins[top][2][0], 253.0)                                   # the weapon's height is the replaced mesh's, exactly
    for j, tri in enumerate(fins):
        bl.plate("fin%d" % j, tri, -0.7, 0.7, steel, part="blade_spine_band")
    # ---- the heel: plate, bezel, stone and halo
    hp = bl.plate("heel_plate", [(-14.2, 206), (-10, 201), (0, 200.2), (7.6, 204), (8.3, 214), (4, 221.5), (-6, 222.8), (-13, 218)], -2.45, 2.45, M["gold"])
    bl.bevel(hp, 0.5)
    C = (-3.0, 211.5)
    prof = [(5.3, 2.6), (5.7, 3.7), (6.6, 3.7), (7.0, 2.6), (7.0, -2.6), (6.6, -3.7), (5.7, -3.7), (5.3, -2.6), (5.3, 2.6)]
    bl.lathe("gem_setting", prof, M["gold"], sides=16, origin=(C[0], C[1], 0), axis="z", caps=(False, False))
    g = bl.gem("gem", 5.2, gemm, center=(C[0], C[1], 2.6), depth=0.55, crown=0.35, sides=8, axis="z")
    bl.copy(g, "gem_b", scale=(1, 1, -1))
    ring = [(17.9, 0.0), (18.75, 0.8), (19.6, 0.0), (18.75, -0.8), (17.9, 0.0)]
    bl.lathe("disc_halo", ring, halo, sides=28, origin=(C[0], C[1], 0), axis="z", caps=(False, False))
    for k in range(4):                                                       # four rays hold the halo
        a = math.radians(45 + 90 * k)
        p0 = (C[0] + 7.0 * math.cos(a), C[1] + 7.0 * math.sin(a), 0)
        p1 = (C[0] + 18.2 * math.cos(a), C[1] + 18.2 * math.sin(a), 0)
        bl.sweep("ray%d" % k, [p0, p1], bl.lozenge(1.0, 1.0), M["gold"], scale=[(1.3, 1.1), (0.45, 0.5)], part="disc_halo")
    # ---- the snath and its fittings
    bl.lathe("snath", [(3.0, 22.9), (3.0, 60), (2.9, 110), (3.0, 160), (3.2, 210)], shaft, sides=10, squash=0.85)
    bl.lathe("cuff", [(3.6, 184), (4.6, 186), (4.2, 192), (4.4, 200), (5.2, 206), (5.0, 212), (3.4, 212.0)], M["gold"], sides=12)
    for i, (a, b) in enumerate(((52, 84), (96, 150))):
        bl.lathe("grip_wraps%d" % i, [(3.5, a), (3.6, (a + b) / 2), (3.5, b)], wrap, sides=10, squash=0.87, part="grip_wraps")
    for i, y in enumerate((50.4, 85.6, 94.4, 151.6, 172.0)):
        bl.lathe("snath_ring%d" % i, [(3.3, y - 1.5), (4.3, y - 0.9), (4.3, y + 0.9), (3.3, y + 1.5)], M["gold"], sides=10, squash=0.9, part="snath_rings")
    bl.lathe("butt_cone", [(0.01, 0.0), (1.3, 7), (2.7, 16), (3.8, 22.5), (3.3, 26.0)], steel, sides=10)
    star = []
    for k in range(16):
        a = 2 * math.pi * k / 16
        r = 19.1 if k % 2 == 0 else 8.2
        star.append((r * math.cos(a), r * math.sin(a)))
    bl.bevel(yplate("butt_disc", star, 22.4, 25.2, dark), 0.5)
    bl.lathe("butt_hub", [(5.6, 21.3), (6.6, 22.4), (6.6, 25.2), (5.0, 26.3)], M["gold"], sides=12, part="butt_disc")
    # ---- two violet ribbons from the cuff
    for i, (pts, z) in enumerate((([(-3.8, 193.5), (-9, 186), (-13, 172), (-17.2, 153.2)], 1.3), ([(-3.4, 190), (-6.5, 181), (-9.5, 170), (-11.5, 158.5)], -1.3))):
        bl.sweep("plume%d" % i, bl.smooth_path([(x, y, z) for x, y in pts], 3), bl.rrect(1.0, 1.0, 0.3), cloth, scale=lambda t: (2.3 * (1 - 0.45 * t), 0.3), part="plume")
    return done("vassal_scythe", out, ("FFFFFF", 0.12, 0.4, 1.0))


# ================================================================ fan
def fan(out):
    reset()
    M = metals()
    leaf = bl.mat("f_leaf", rgb(22, 26, 78), rough=0.45, bump="leather", bscale=1.6, bstr=0.12, bevel=0.0)
    leaf2 = bl.mat("f_leaf2", rgb(34, 42, 112), rough=0.45, bevel=0.0)
    bone = bl.mat("f_bone", rgb(226, 230, 240), metallic=1.0, rough=0.24, bump="brushed", bstr=0.1, bevel=0.12)
    dark = bl.mat("f_dark", rgb(40, 44, 70), metallic=1.0, rough=0.3, bevel=0.15)
    cyan = bl.mat("f_cyan", rgb(10, 50, 70), rough=0.4, emit=rgb(80, 220, 255), strength=1.15, bevel=0.0)
    gemm = bl.mat("f_gem", rgb(40, 190, 240), metallic=0.3, rough=0.07, emit=rgb(60, 205, 255), strength=0.45, bevel=0.05)
    white = bl.mat("f_white", rgb(250, 246, 250), rough=0.5, emit=rgb(255, 235, 245), strength=0.3, bevel=0.0)
    pink = bl.mat("f_pink", rgb(255, 150, 190), rough=0.5, emit=rgb(255, 130, 180), strength=0.6, bevel=0.0)
    moon = bl.mat("f_moon", rgb(236, 232, 214), rough=0.6, emit=rgb(255, 248, 220), strength=0.22, bevel=0.0)
    branch = bl.mat("f_branch", rgb(16, 10, 14), rough=0.6, bevel=0.0)
    red = bl.mat("f_red", rgb(200, 30, 44), rough=0.6, bump="brushed", bscale=0.8, bstr=0.4, groove=rgb(90, 8, 16), gamount=0.7, bevel=0.0)
    lac = bl.mat("f_lac", rgb(26, 24, 40), rough=0.3, bump="rings", bscale=1.2, bstr=0.2, bevel=0.1)
    PV = (0.0, 2.8)
    A0, A1, NR = 8.5, 171.5, 13
    step = (A1 - A0) / (NR - 1)
    ZL = 0.35
    bl.plate("fan_leaf", arc(45.2, A0, A1, 37, PV) + arc(10.0, A1, A0, 13, PV), -ZL, ZL, leaf)
    for s in (1, -1):
        z = ZL * s
        for k in range(0, NR - 1, 2):                                        # every second fold a shade lighter
            sector("f_fold%d%d" % (s, k), 10.2, 45.0, A0 + k * step, A0 + (k + 1) * step, 5, z - 0.01 * s, z + 0.03 * s, leaf2, PV)
        sector("f_goldband%d" % s, 40.6, 42.0, A0 + 0.5, A1 - 0.5, 37, z + 0.03 * s, z + 0.07 * s, M["gold_line"], PV)
        sector("f_goldband_b%d" % s, 11.2, 11.9, A0 + 0.5, A1 - 0.5, 19, z + 0.03 * s, z + 0.07 * s, M["gold_line"], PV)
        sector("f_cyanline%d" % s, 42.7, 43.3, A0 + 0.5, A1 - 0.5, 37, z + 0.03 * s, z + 0.07 * s, cyan, PV)
        mx = -17.0 * s                                                       # the same painting seen from both sides
        bl.plate("f_moon%d" % s, arc(8.6, 0, 360, 25, (mx, 31.0))[:-1], z + 0.03 * s, z + 0.06 * s, moon, detail=True)
        sgn = s
        tw = [(26 * sgn, 13.5), (17 * sgn, 19), (7 * sgn, 22.5), (-3 * sgn, 28.5), (-11 * sgn, 31), (-21 * sgn, 36.5)]
        fr = frames(bl.smooth_path([(x, y, 0) for x, y in tw], 4))
        band("f_branch%d" % s, fr, lambda t: -0.8 * (1 - 0.7 * t), lambda t: 0.8 * (1 - 0.7 * t), z + 0.07 * s, z + 0.1 * s, branch)
        for j, (b0, b1) in enumerate((((7 * sgn, 22.5), (9 * sgn, 33.5)), ((-3 * sgn, 28.5), (-1.5 * sgn, 38.5)), ((17 * sgn, 19), (22 * sgn, 27.5)))):
            fr2 = frames([(b0[0], b0[1], 0), ((b0[0] + b1[0]) / 2 + 0.8, (b0[1] + b1[1]) / 2, 0), (b1[0], b1[1], 0)])
            band("f_twig%d%d" % (s, j), fr2, lambda t: -0.45 * (1 - 0.6 * t), lambda t: 0.45 * (1 - 0.6 * t), z + 0.07 * s, z + 0.1 * s, branch)
        blooms = [(9, 33.5, 3.9), (-1.5, 38.5, 3.5), (22, 27.5, 3.5), (12, 21.5, 3.0), (-7, 30.5, 3.1), (-21, 36.5, 2.7), (3, 26, 2.6), (27, 15, 2.5), (-14, 33, 2.3)]
        for j, (x, y, r) in enumerate(blooms):
            flower("f_bloom%d%d" % (s, j), r, z + 0.1 * s, z + 0.13 * s, white if j % 3 else pink, (x * sgn, y), turn=0.5 * j)
            bl.plate("f_eye%d%d" % (s, j), arc(r * 0.26, 0, 360, 9, (x * sgn, y))[:-1], z + 0.13 * s, z + 0.16 * s, pink if j % 3 else M["gold_line"], detail=True)
    # the lit rim of the leaf
    bl.plate("leaf_rim", arc(46.3, A0, A1, 31, PV) + arc(44.9, A1, A0, 31, PV), -0.85, 0.85, cyan)
    # ---- ribs, their teeth, and the two guards
    crease = bl.mat("f_crease", rgb(64, 78, 156), rough=0.45, bevel=0.0)
    for k in range(1, NR - 1):
        a = math.radians(A0 + k * step)
        d = Vector((math.cos(a), math.sin(a), 0))
        nrm = Vector((-d.y, d.x, 0))
        O = Vector((PV[0], PV[1], 0))
        rings = []
        for r, w, t in ((5.0, 0.8, 0.62), (10.0, 0.75, 0.74), (16.0, 0.6, 0.7), (21.0, 0.2, 0.42)):
            c = O + d * r
            rings.append([c + nrm * w + Vector((0, 0, t)), c - nrm * w + Vector((0, 0, t)), c - nrm * w - Vector((0, 0, t)), c + nrm * w - Vector((0, 0, t))])
        bl.loft("fan_rib%d" % k, rings, bone, part="fan_ribs")
        for s in (1, -1):                                                    # the fold over each rib, painted on the leaf
            q = [O + d * 20.5 + nrm * 0.22, O + d * 44.8 + nrm * 0.16, O + d * 44.8 - nrm * 0.16, O + d * 20.5 - nrm * 0.22]
            bl.plate("f_crease%d%d" % (k, s), [(v.x, v.y) for v in q], min(ZL * s + 0.03 * s, ZL * s + 0.06 * s), max(ZL * s + 0.03 * s, ZL * s + 0.06 * s), crease, detail=True)
        c0 = O + d * 45.3
        tip = O + d * (50.5 if k % 2 == 0 else 48.6)
        tooth = [c0 + nrm * 1.35, tip, c0 - nrm * 1.35]
        bl.plate("fan_tooth%d" % k, [(v.x, v.y) for v in tooth], -0.6, 0.6, bone, part="fan_teeth")
    for sx in (1, -1):
        a = math.radians(A0 - 4.2) if sx > 0 else math.radians(A1 + 4.2)
        d = Vector((math.cos(a), math.sin(a), 0))
        nrm = Vector((-d.y, d.x, 0)) * sx
        rings = []
        for r, w0, w1, t in ((3.0, 1.3, 1.3, 0.8), (14.0, 1.9, 1.5, 0.88), (40.0, 2.3, 1.5, 0.88), (50.5, 1.7, 1.2, 0.7), (55.9, 0.12, 0.1, 0.25)):
            c = Vector((PV[0], PV[1], 0)) + d * r
            rings.append([c + nrm * w0 + Vector((0, 0, t)), c - nrm * w1 + Vector((0, 0, t)), c - nrm * w1 - Vector((0, 0, t)), c + nrm * w0 - Vector((0, 0, t))])
        bl.loft("fan_guard%d" % sx, rings, dark, part="fan_guards")
        for s in (1, -1):                                                    # a gold line and a cyan one along each guard
            p0, p1 = Vector((PV[0], PV[1], 0)) + d * 8, Vector((PV[0], PV[1], 0)) + d * 49
            for nm, off, hwid, mt in (("gg", 0.9, 0.22, M["gold_line"]), ("gc", -0.2, 0.2, cyan)):
                q = [p0 + nrm * (off + hwid), p1 + nrm * (off * 0.6 + hwid), p1 + nrm * (off * 0.6 - hwid), p0 + nrm * (off - hwid)]
                bl.plate("%s%d%d" % (nm, sx, s), [(v.x, v.y) for v in q], min(0.8 * s, 0.8 * s + 0.12 * s), max(0.8 * s, 0.8 * s + 0.12 * s), mt, detail=True)
    # ---- pivot, stone, handle and charm
    bl.lathe("pivot_plate", [(0.01, 2.1), (4.6, 2.1), (6.2, 1.2), (6.3, 0.0), (6.2, -1.2), (4.6, -2.1), (0.01, -2.1)], M["gold"], sides=16, origin=(PV[0], PV[1], 0), axis="z")
    g = bl.gem("pivot_gem", 3.8, gemm, center=(PV[0], PV[1], 2.0), depth=0.35, crown=0.3, sides=8, axis="z")
    bl.copy(g, "pivot_gem_b", scale=(1, 1, -1))
    bl.lathe("handle", [(1.6, 1.0), (2.2, -2.0), (2.45, -7.0), (2.3, -12.5), (1.7, -15.4), (0.9, -16.0)], lac, sides=10, squash=0.78)
    for i, y in enumerate((-2.6, -8.6, -14.2)):
        bl.lathe("handle_ring%d" % i, [(2.0, y - 0.7), (2.48, y - 0.4), (2.48, y + 0.4), (2.0, y + 0.7)], M["gold"], sides=10, squash=0.72, part="handle_rings")
    bl.tube("charm_cord", [(0.0, -15.8, 0), (0.9, -18.5, 0), (1.3, -22, 0), (0.7, -26.5, 0), (0.8, -30.5, 0)], 0.32, red, sides=5, sub=2)
    bl.lathe("charm_bead_red", [(0.01, -18.9), (0.95, -19.5), (1.15, -20.3), (0.95, -21.1), (0.01, -21.6)], red, sides=8, origin=(1.1, 0, 0))
    bl.lathe("charm_bead", [(0.01, -22.0), (1.2, -22.8), (1.6, -23.9), (1.2, -25.0), (0.01, -25.8)], gemm, sides=8, origin=(1.0, 0, 0))
    bl.lathe("charm_tassel", [(0.4, -30.0), (1.3, -31.5), (1.5, -39.8)], red, sides=8, origin=(0.8, 0, 0), squash=0.45, part="charm_cord")
    return done("vassal_fan", out, ("FFFFFF", 0.12, 0.4, 1.0))


# ================================================================ harpoon
def harpoon(out):
    reset()
    M = metals()
    steel = bl.mat("h_steel", rgb(196, 208, 226), metallic=1.0, rough=0.22, bump="brushed", bstr=0.12, bevel=0.2, groove=rgb(130, 146, 176), gamount=0.5)
    dark = bl.mat("h_dark", rgb(58, 70, 96), metallic=1.0, rough=0.34, bump="hammer", bstr=0.12, bevel=0.2)
    core = bl.mat("h_core", rgb(34, 52, 96), metallic=1.0, rough=0.36, bevel=0.0)
    bolt = bl.mat("h_bolt", rgb(10, 30, 60), rough=0.4, emit=rgb(120, 215, 255), strength=1.2, bevel=0.0)
    gemm = bl.mat("h_gem", rgb(40, 150, 240), metallic=0.3, rough=0.06, emit=rgb(90, 205, 255), strength=0.45, bevel=0.05)
    wood = bl.mat("h_wood", rgb(120, 72, 34), rough=0.6, bump="grain", bstr=0.5, groove=rgb(56, 30, 12), gamount=0.6, bevel=0.0)
    leather = bl.mat("h_leather", rgb(74, 42, 22), rough=0.7, bump="cross", bscale=0.7, bstr=0.9, groove=rgb(24, 12, 6), gamount=1.0, bevel=0.0)
    rope = bl.mat("h_rope", rgb(214, 172, 92), rough=0.7, bump="wrap", bscale=1.6, bstr=0.9, groove=rgb(110, 76, 30), gamount=0.9, bevel=0.0)
    ha = bl.mat("h_ha", rgb(248, 252, 255), metallic=1.0, rough=0.1, bevel=0.0)
    # ---- head: a broad leaf blade with a lightning core
    st = [(248.4, 4.4, 1.38), (252, 7.6, 1.38), (256.5, 9.8, 1.38), (262, 9.2, 1.38), (268, 7.0, 1.3), (273.5, 4.2, 0.9), (277, 1.9, 0.5), (279.0, 0.1, 0.08)]
    bl.loft("head_blade", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.34)) for y, w, t in st], steel)
    for s in (1, -1):
        z = 1.38 * s
        bl.plate("h_core%d" % s, [(-1.3, 249.5), (-2.9, 256.5), (-2.6, 262.5), (0, 269.5), (2.6, 262.5), (2.9, 256.5), (1.3, 249.5)], min(z - 0.03 * s, z + 0.04 * s), max(z - 0.03 * s, z + 0.04 * s), core, detail=True)
        zig = [(-0.5, 250), (0.9, 253.5), (-0.9, 256.2), (1.1, 259.5), (-0.6, 262.5), (0.5, 265.2), (0, 268.2)]
        fr = frames([(x, y, 0) for x, y in zig])
        band("h_bolt%d" % s, fr, -0.42, 0.42, z + 0.04 * s, z + 0.1 * s, bolt)
        for sx in (1, -1):                                                   # the bright edge bevels
            rows = []
            for y, w, t in st[:-2]:
                rows.append([(sx * (w * 0.36 + (w * 0.62) * j / 2.0), y + 0.3, s * (t * (1 - (0.02 + 0.62 * j / 2.0) / 0.66) + 0.05)) for j in range(3)])
            sheet("h_ha%d%d" % (s, sx), rows, ha, s=s)
    # the barbs: a great pair swept back, a smaller pair under them
    unit = bl.lozenge(1.0, 1.0, flat=0.3)
    for nm, pts, w, t in (("barb_a", [(3.6, 247.5), (10, 256.4), (16.6, 258.0), (21.0, 251), (21.75, 237), (20.3, 221)], 3.4, 0.9),
                          ("barb_b", [(3.6, 241), (8.5, 244.5), (12.6, 240.5), (13.4, 231.5), (12.4, 224)], 2.3, 0.8)):
        p = bl.smooth_path([(x, y, 0) for x, y in pts], 3)
        ob = bl.sweep(nm, p, unit, steel, scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.7 + 0.1, t * (1 - u) ** 0.6 + 0.08), part="head_barbs")
        bl.mirror(ob, "x")
        for sx in (1, -1):                                                   # a line of light down each great barb
            if nm == "barb_a":
                fr = frames([(sx * q.x, q.y, 0) for q in p[2:-4]])
                for s in (1, -1):
                    band("h_barbline%d%d" % (sx, s), fr, -0.3, 0.3, 0.9 * s - 0.25 * s, 0.9 * s - 0.05 * s, bolt)
    # ---- neck with the stone on its face, socket and bands
    bl.lathe("head_neck", [(4.2, 236.2), (5.6, 238.5), (5.8, 243), (5.4, 247.5), (4.0, 249.8)], dark, sides=12)
    bl.lathe("gem_bezel", [(2.9, 0.0), (3.5, 0.9), (4.1, 0.9), (4.4, 0.0), (2.9, 0.0)], M["gold"], sides=12, origin=(0, 243.0, 5.4), axis="z", caps=(False, False), part="head_neck")
    bl.gem("gem", 3.3, gemm, center=(0, 243.0, 5.5), depth=0.35, crown=0.35, sides=8, axis="z")
    bl.lathe("socket", [(3.4, 196), (4.0, 199), (4.4, 212), (5.0, 226), (5.6, 233), (5.4, 238)], steel, sides=12)
    for i, y in enumerate((201.5, 216, 230)):
        bl.lathe("socket_band%d" % i, [(4.1 + 0.025 * (y - 200), y - 1.6), (4.9 + 0.025 * (y - 200), y - 1.0), (4.9 + 0.025 * (y - 200), y + 1.0), (4.1 + 0.025 * (y - 200), y + 1.6)],
                 M["gold"], sides=12, part="socket_bands")
    for s in (1, -1):                                                        # runes of storm on the socket's faces
        bl.text("h_runes%d" % s, RUNES[2:6] if s > 0 else RUNES[14:18], 3.6, bolt, (0.0, 208.5 + 0.0, (4.45) * s), rot=(0, 0 if s > 0 else 180, 90), extrude=0.03, spacing=1.3, font=rune_font())
    # ---- shaft, grip, bands, butt
    bl.lathe("shaft", [(3.0, 12.4), (3.1, 60), (3.2, 130), (3.1, 200)], wood, sides=10)
    bl.lathe("grip_wrap", [(3.4, 32), (3.6, 50), (3.6, 90), (3.4, 108)], leather, sides=10)
    for i, y in enumerate((30.2, 109.8, 150, 191.6)):
        bl.lathe("band%d" % i, [(3.3, y - 1.8), (3.8, y - 1.1), (3.8, y + 1.1), (3.3, y + 1.8)], M["gold"], sides=10, part="bands")
    bl.lathe("butt", [(0.01, 0.0), (2.2, 1.2), (4.3, 4.5), (4.5, 7.5), (3.6, 10.5), (3.3, 12.4)], dark, sides=12)
    # ---- the line: wound round the shaft, through the eyelet, a hanging coil
    bl.lathe("rope_wind", [(3.5, 160), (3.9, 163), (3.9, 180), (3.5, 183)], rope, sides=10, part="rope")
    bl.lathe("eyelet", [(2.2, -1.1), (2.8, 0.0), (2.2, 1.1), (1.6, 0.0), (2.2, -1.1)], steel, sides=10, origin=(-6.4, 187.0, 0), axis="z", caps=(False, False))
    bl.sweep("eyelet_stem", [(-3.0, 187, 0), (-4.6, 187, 0)], bl.rrect(1.0, 1.0, 0.3), steel, scale=[(1.3, 1.1), (1.0, 0.9)], part="eyelet")
    loops = [[(-3.6, 181.5, 1.5), (-6.4, 185.5, 0.6), (-9.5, 180, 0.0), (-13.6, 160, -0.6), (-14.0, 135, 0.0), (-10.5, 114.5, 0.8), (-5.2, 118, 1.6), (-4.6, 140, 2.2), (-4.2, 158, 2.6)],
             [(-6.6, 186, -0.6), (-8.6, 176, -1.4), (-11.4, 152, -2.0), (-11.0, 128, -1.6), (-7.4, 120.5, -1.0), (-4.4, 126, -2.4)]]
    for i, pts in enumerate(loops):
        bl.tube("rope%d" % i, pts, 0.75, rope, sides=6, sub=3, part="rope")
    return done("vassal_harpoon", out, ("FFFFFF", 0.22, 0.45, 1.0))


# ================================================================ chain
def chain(out):
    reset()
    M = metals()
    steel = bl.mat("c_steel", rgb(190, 198, 214), metallic=1.0, rough=0.24, bump="hammer", bstr=0.08, bevel=0.3)
    iron = bl.mat("c_iron", rgb(52, 58, 76), metallic=1.0, rough=0.36, bump="hammer", bscale=0.5, bstr=0.3, bevel=0.3, groove=rgb(20, 22, 32), gamount=0.6)
    cyan = bl.mat("c_cyan", rgb(10, 40, 60), rough=0.4, emit=rgb(120, 225, 255), strength=1.2, bevel=0.0)
    gemm = bl.mat("c_gem", rgb(60, 170, 240), metallic=0.3, rough=0.06, emit=rgb(120, 215, 255), strength=0.45, bevel=0.05)
    leather = bl.mat("c_leather", rgb(104, 62, 30), rough=0.7, bump="cross", bscale=0.9, bstr=0.9, groove=rgb(34, 18, 8), gamount=1.0, bevel=0.0)
    strapm = bl.mat("c_strap", rgb(84, 48, 24), rough=0.7, bump="leather", bstr=0.3, bevel=0.0)
    # ---- the weight: an iron drum banded in gold, a seam of light, spikes
    W = (-92.4, 155.8, 0.0)
    bl.lathe("weight", [(0.01, -20.2), (6.4, -19.0), (10.6, -15.2), (12.4, -8.5), (12.6, 0.0), (12.4, 8.5), (10.6, 15.2), (6.4, 19.0), (0.01, 20.2)], iron, sides=12, origin=W, axis="x")
    for i, h in enumerate((-9.2, 9.2)):
        bl.lathe("weight_band%d" % i, [(12.1, h - 1.9), (13.1, h - 1.2), (13.1, h + 1.2), (12.1, h + 1.9)], M["gold"], sides=12, origin=W, axis="x", part="weight")
    bl.lathe("weight_seam", [(12.5, -0.7), (13.0, 0.0), (12.5, 0.7)], cyan, sides=12, origin=W, axis="x", part="weight", caps=(False, False))
    for k in range(6):
        a = 60.0 * k
        cone("spike_a%d" % k, 2.9, 10.6, steel, (W[0] - 4.6, W[1], 0), rot=(a, 0, 0), lift=11.4, part="spikes")
        cone("spike_b%d" % k, 2.3, 7.6, steel, (W[0] + 13.6, W[1], 0), rot=(a + 30.0, 0, 0), lift=9.6, part="spikes")
    cone("spike_tip", 3.6, 12.2, steel, (W[0] - 19.4, W[1], 0), rot=(0, 0, 90), part="spikes")
    bl.lathe("swivel", [(0.01, -4.2), (2.4, -3.0), (3.0, 0.0), (2.4, 3.0), (0.01, 4.2)], steel, sides=8, origin=(-68.5, 153.4, 0), axis="x")
    # ---- the links
    path = bl.smooth_path([(0.0, 64.5, 0), (1.2, 86, 0), (-4.5, 112, 0), (-18.5, 135.5, 0), (-40, 149.5, 0), (-63.5, 153.6, 0)], 8)
    acc = [0.0]
    for a, b in zip(path, path[1:]):
        acc.append(acc[-1] + (b - a).length)
    NL, L, Wd, rho = 14, 6.3, 3.9, 1.0
    for j in range(NL):
        sgoal = acc[-1] * (j + 0.5) / NL
        i = max(1, min(len(path) - 1, next(k for k, a in enumerate(acc) if a >= sgoal)))
        f = (sgoal - acc[i - 1]) / max(acc[i] - acc[i - 1], 1e-6)
        c = path[i - 1].lerp(path[i], f)
        d = (path[i] - path[i - 1]).normalized()
        e = Vector((0, 0, 1)) if j % 2 else Vector((-d.y, d.x, 0))
        gdir = d.cross(e).normalized()
        loop = []
        for q in range(14):                                                  # a stadium: two half circles and two straights
            if q < 7:
                a = -math.pi / 2 + math.pi * q / 6.0
                loop.append((c + d * ((L - Wd) + Wd * math.cos(a)) + e * (Wd * math.sin(a)), d * math.cos(a) + e * math.sin(a)))
            else:
                a = math.pi / 2 + math.pi * (q - 7) / 6.0
                loop.append((c + d * (-(L - Wd) + Wd * math.cos(a)) + e * (Wd * math.sin(a)), d * math.cos(a) + e * math.sin(a)))
        loop.append(loop[0])
        rings = [[p + rad * (rho * math.cos(ph)) + gdir * (rho * math.sin(ph)) for ph in (0, math.pi / 2, math.pi, 1.5 * math.pi)] for p, rad in loop]
        bl.loft("link%d" % j, rings, steel, caps=(False, False), part="links")
    # ---- the clasp with the stone on its face
    bl.lathe("clasp", [(3.5, 45.0), (6.0, 46.6), (6.8, 51.5), (6.3, 57.5), (4.4, 62.5), (2.8, 66.9)], iron, sides=12)
    for i, y in enumerate((46.4, 59.6)):
        r = 6.25 if i == 0 else 5.6
        bl.lathe("clasp_band%d" % i, [(r - 0.3, y - 1.2), (r + 0.45, y - 0.7), (r + 0.45, y + 0.7), (r - 0.3, y + 1.2)], M["gold"], sides=12, part="clasp")
    bl.lathe("clasp_bezel", [(3.9, 0.0), (4.4, 1.4), (5.2, 1.4), (5.8, 0.0), (3.9, 0.0)], M["gold"], sides=12, origin=(0, 52.0, 6.2), axis="z", caps=(False, False))
    bl.gem("gem", 4.2, gemm, center=(0, 52.0, 6.4), depth=0.3, crown=0.35, sides=8, axis="z")
    # ---- the grip: leather, cap, knuckle bow with studs, wrist strap
    bl.lathe("grip", [(3.0, 3.2), (3.35, 12), (3.4, 24), (3.35, 36), (3.0, 46)], leather, sides=10)
    bl.lathe("grip_cap", [(0.01, -6.5), (3.2, -5.6), (5.1, -2.6), (5.3, 0.4), (4.2, 2.6), (3.2, 3.6)], iron, sides=12)
    bl.lathe("grip_cap_band", [(4.9, -1.2), (5.5, -0.6), (5.5, 0.6), (4.9, 1.2)], M["gold"], sides=12, part="grip_cap")
    bow = bl.smooth_path([(3.0, 44.6, 0), (8.2, 45.6, 0), (11.4, 38.5, 0), (11.6, 24, 0), (11.3, 10.5, 0), (8.0, 3.4, 0), (3.0, 2.6, 0)], 3)
    bl.sweep("knuckle_bow", bow, bl.rrect(0.7, 1.5, 0.3), steel)
    for i, y in enumerate((14.5, 24.0, 33.5)):
        cone("stud%d" % i, 1.2, 1.0, steel, (12.25, y, 0), rot=(0, 0, -90), sides=4, part="knuckle_bow", waist=0.6)
    bl.sweep("strap", bl.smooth_path([(-2.8, 24.6, 0), (-8.6, 23.2, 0), (-14.1, 15.2, 0), (-11.8, 7.2, 0), (-2.8, 4.4, 0)], 4), bl.rrect(0.55, 0.3, 0.12), strapm)
    return done("vassal_chain", out, ("FFFFFF", 0.045, 0.45, 1.0))


BUILDERS = {"katana": katana, "scythe": scythe, "fan": fan, "harpoon": harpoon, "chain": chain}


def main(argv):
    global FAST, HEADERS
    only = None
    trial = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
        if a == "--headers":
            HEADERS = argv[i + 1]
        if a == "--fast":
            FAST = True
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

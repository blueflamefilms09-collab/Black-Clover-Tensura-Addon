"""The four Legendary Weapons, made in Blender (3.80.0). Run: python tools/blender/legendary.py [--only sword,shield,spear,bow] [--trial DIR] [--preview DIR] [--quick]

--trial DIR writes the meshes, skins and previews into DIR instead of the mod's assets (for looking before replacing).
Each weapon keeps the length, the grip position and the `place` line of the model it replaces, so the hand, the display transforms, the
size class and the grip table still fit. See bl_lib.py for what Blender does and what the mod's tools do.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bl_lib as bl  # noqa: E402
from bl_lib import rgb  # noqa: E402

CLASSIC = os.path.join(bl.MESH_DIR, "classic")
RUNE_FONTS = ["/usr/share/fonts/truetype/freefont/FreeMono.ttf", "/usr/share/fonts/opentype/unifont/unifont.otf"]      # fonts with the runic block
RUNES = "ᚠᚢᚦᚨᚱᚲᚷᚹᚺᚾᛁᛃᛇᛈᛉᛊᛏᛒᛖᛗᛚᛜᛞᛟ"


def rune_font():
    for f in RUNE_FONTS:
        if os.path.exists(f):
            return f
    return None


def metals():
    m = {}
    m["steel"] = bl.mat("steel", rgb(196, 205, 222), metallic=1.0, rough=0.22, bump="brushed", bstr=0.18, bevel=0.22, groove=rgb(120, 132, 160), gamount=0.55)
    m["dark"] = bl.mat("dark_steel", rgb(70, 78, 98), metallic=1.0, rough=0.32, bump="brushed", bstr=0.12, bevel=0.1)
    m["gold"] = bl.mat("gold", rgb(240, 186, 82), metallic=1.0, rough=0.24, bump="hammer", bstr=0.06, bevel=0.25)
    m["gold_deep"] = bl.mat("gold_deep", rgb(170, 112, 38), metallic=1.0, rough=0.38, bevel=0.0)
    # for bake-only details (thin floating plates): no Bevel node, it would average a thin plate's top with its underside
    m["blued"] = bl.mat("blued_steel", rgb(46, 62, 110), metallic=1.0, rough=0.34, bump="brushed", bstr=0.15, bevel=0.0, groove=rgb(22, 30, 62), gamount=0.6)
    m["gold_line"] = bl.mat("gold_line", rgb(246, 196, 96), metallic=1.0, rough=0.25, bevel=0.0)
    return m


# ================================================================ the curse looks (3.83.0): the same Blender models, corrupted by their sin
SIN = {"shield": (255, 58, 30), "sword": (255, 186, 38), "spear": (255, 58, 196), "bow": (176, 96, 255)}      # the fire of each curse look (tools/heroweapon/art_hart1.py: wrath, greed, lust, pride)


def curse_metals(sin, line=1.25):
    """metals() with the same keys, corrupted: the steel is black iron with a purple cast, the gold a paler iron, the blued core is scorched, and the
    gold lines burn in the sin's colour. Also: "sin" (a line of that fire), "hot" (its bright core, for runes), "eye" (the stone turned into an eye)."""
    r, g, b = sin
    hot = (min(255, r + 40), min(255, g + 70), min(255, b + 70))
    m = {}
    m["steel"] = bl.mat("black_iron", rgb(58, 52, 76), metallic=1.0, rough=0.36, bump="hammer", bscale=0.6, bstr=0.25, bevel=0.22, groove=rgb(22, 18, 30), gamount=0.6)
    m["dark"] = bl.mat("pit_iron", rgb(30, 26, 38), metallic=1.0, rough=0.45, bevel=0.1)
    m["gold"] = bl.mat("pale_iron", rgb(98, 86, 114), metallic=1.0, rough=0.34, bump="hammer", bstr=0.2, bevel=0.25, groove=rgb(40, 32, 50), gamount=0.6)
    m["gold_deep"] = bl.mat("ember", rgb(r * 0.3, g * 0.2, b * 0.2), rough=0.5, emit=rgb(r, g * 0.75, b * 0.75), strength=0.7, bevel=0.0)
    m["blued"] = bl.mat("scorched", rgb(22, 18, 28), metallic=1.0, rough=0.5, bump="brushed", bstr=0.2, bevel=0.0)
    m["gold_line"] = m["sin"] = bl.mat("sin_line", rgb(r * 0.3, g * 0.3, b * 0.3), rough=0.5, emit=rgb(r, g, b), strength=line, bevel=0.0)
    m["hot"] = bl.mat("sin_hot", rgb(r * 0.3, g * 0.3, b * 0.3), rough=0.5, emit=rgb(*hot), strength=2.4, bevel=0.0)
    m["eye"] = bl.mat("eye", rgb(r * 0.75, g * 0.75, b * 0.75), metallic=0.3, rough=0.08, emit=rgb(r, g, b), strength=0.45, bevel=0.05)
    m["pupil"] = bl.mat("pupil", rgb(4, 2, 6), rough=0.3, bevel=0.0)
    return m


def jag(rng, p0, p1, n, amp):
    """A jagged line of n spans from p0 to p1 (2D), each inner point thrown sideways by up to amp."""
    dx, dy = p1[0] - p0[0], p1[1] - p0[1]
    ln = math.hypot(dx, dy) or 1.0
    px, py = -dy / ln, dx / ln
    pts = []
    for i in range(n + 1):
        j = rng.uniform(-amp, amp) if 0 < i < n else 0.0
        pts.append((p0[0] + dx * i / n + px * j, p0[1] + dy * i / n + py * j))
    return pts


def crack(name, pts, zfn, s, w, material):
    """A crack with light in it: a thin strip over 2D points (x, y), lying a hair above the surface zfn(x, y) on the +z (s = 1) or -z face, tapering to its
    end. Bake-only."""
    path = [(x, y, zfn(x, y)) for x, y in pts]
    return bl.sweep(name, path, [(-1.0, 0.02), (1.0, 0.02), (1.0, 0.09), (-1.0, 0.09)], material, up=(0, 0, s), scale=lambda t: (w * (1.0 - 0.75 * t) + 0.04, 1.0), detail=True)


def spike(name, base, tip, r, material, sides=4, part=None, bend=(0, 0, 0)):
    """A thorn from base to tip (3D points), r thick at its root; bend pushes its middle sideways (a hooked thorn)."""
    b, t = bl.Vector(base), bl.Vector(tip)
    mid = (b + t) * 0.5 + bl.Vector(bend)
    ax = (t - b).normalized()
    up = (0, 0, 1) if abs(ax.z) < 0.9 else (1, 0, 0)
    return bl.sweep(name, bl.smooth_path([b, mid, t], 2), bl.circle(1.0, sides), material, up=up, scale=lambda u: r * (1.0 - u) ** 0.8 + 0.04, part=part)


def eye(name, r, center, z, M, s=1, slit=0.3):
    """Turns a stone lying on a z face into an eye: a bright iris ring and a slit pupil on its table (bake-only). z: the table's height, r: its radius."""
    cx, cy = center
    z0, z1 = (z + 0.02, z + 0.08) if s > 0 else (-z - 0.08, -z - 0.02)
    bl.lathe(name + "_iris", [(r * 0.8, z0), (r * 0.8, z1), (r * 0.94, z1), (r * 0.94, z0), (r * 0.8, z0)], M["hot"], sides=20, axis="z", origin=(cx, cy, 0), caps=(False, False), detail=True)
    n = 8
    lens = [(cx + slit * r * math.sin(math.pi * k / n), cy - r * 0.95 + 1.9 * r * k / n) for k in range(n + 1)]
    lens += [(cx - slit * r * math.sin(math.pi * k / n), cy - r * 0.95 + 1.9 * r * k / n) for k in range(n - 1, 0, -1)]
    z0, z1 = (z + 0.05, z + 0.12) if s > 0 else (-z - 0.12, -z - 0.05)
    bl.plate(name + "_pupil", lens, z0, z1, M["pupil"], detail=True)


def fit_extent(obs, axis, target):
    """Stretches objects along one axis about 0 so their farthest point lies exactly at +-target (a curse look keeps the box of the look it replaces)."""
    i = "xyz".index(axis)
    far = max(abs(v.co[i]) for o in obs for v in o.data.vertices)
    sc = [1.0, 1.0, 1.0]
    sc[i] = target / far
    for o in obs:
        bl.move(o, scale=tuple(sc))


def sword(out, curse=False):
    import random
    bl.reset()
    iid = "legendary_sword_curse" if curse else "legendary_sword"
    M = curse_metals(SIN["sword"]) if curse else metals()
    if curse:
        wrap = bl.mat("wrap", rgb(40, 32, 52), rough=0.6, bump="cross", bscale=1.0, bstr=0.9, groove=rgb(150, 100, 14), gamount=1.0, bevel=0.0)       # greed: gold glints between the black cords
        gemm, rune = M["eye"], M["hot"]
    else:
        wrap = bl.mat("wrap", rgb(36, 58, 150), rough=0.55, bump="cross", bscale=1.0, bstr=0.9, groove=rgb(8, 12, 40), gamount=1.0, bevel=0.0)
        gemm = bl.mat("gem", rgb(24, 90, 235), metallic=0.35, rough=0.08, emit=rgb(40, 130, 255), strength=0.55, bevel=0.05)
        rune = bl.mat("rune", rgb(10, 20, 40), rough=0.5, emit=rgb(120, 205, 255), strength=3.0, bevel=0.0)
    # ---- blade: a broad hexagonal section with a flat for the fuller, flared shoulders, a long point
    st = [(32, 6.6, 1.3), (36, 7.6, 1.3), (41, 6.9, 1.3), (48, 6.2, 1.3), (62, 6.0, 1.3), (80, 5.9, 1.3), (98, 5.7, 1.3), (114, 5.3, 1.3),      # the same thickness as far as the fuller runs
          (122, 4.9, 1.2), (130, 4.4, 0.95), (138, 3.5, 0.7), (145, 2.0, 0.42), (150, 0.12, 0.08)]
    def half(y, k):                                                      # the half width of the blade's flat at height y, times k
        for (y0, w0, _t0), (y1, w1, _t1) in zip(st, st[1:]):
            if y0 <= y <= y1:
                return k * 0.36 * (w0 + (w1 - w0) * (y - y0) / (y1 - y0))
        return 0.0
    if curse:                                                            # the same blade with a row of backward-hooked teeth on both edges
        rings = [bl.ring(y, bl.lozenge(w, t, flat=w * 0.36)) for y, w, t in st[:3]]
        y = 44.0
        while y < 122.0:
            f, w = half(y, 1.0), half(y, 1.0) / 0.36
            rings.append(bl.ring(y, bl.lozenge(w, 1.3, flat=f)))
            rings.append(bl.ring(y + 4.4, bl.lozenge(w + 1.9, 1.3, flat=half(y + 4.4, 1.0))))
            rings.append(bl.ring(y + 4.9, bl.lozenge(half(y + 4.9, 1.0) / 0.36 - 0.5, 1.3, flat=half(y + 4.9, 1.0))))
            y += 6.5
        rings += [bl.ring(y, bl.lozenge(w, t, flat=w * 0.36)) for y, w, t in st[8:]]
        bl.loft("blade", rings, M["steel"])
        rng = random.Random(7)
        for s in (1, -1):                                                # cracks of gold light from the core out to the teeth, on both faces
            for i in range(11):
                y0, sx = 49.0 + 6.5 * i, (1 if i % 2 == 0 else -1)
                f, w = half(y0, 1.0), half(y0, 1.0) / 0.36
                zf = lambda x, y, f=f, w=w: 1.3 * s * (1.0 if abs(x) <= f else max(0.0, (w - abs(x)) / (w - f))) + 0.02 * s      # noqa: E731
                crack("crack%d%d" % (s, i), jag(rng, (sx * f * 0.8, y0), (sx * (w + 0.7), y0 + 4.2), 4, 0.45), zf, s, 0.4, M["sin"])
    else:
        bl.loft("blade", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.36)) for y, w, t in st], M["steel"])
    # the blade's core: blued steel between two gold lines, with glowing runes; baked into the skin (no quads)
    ys = [44, 50, 62, 80, 98, 110, 116]
    for s in (1, -1):
        core = [(-half(y, 0.86), y) for y in ys] + [(0, 123)] + [(half(y, 0.86), y) for y in reversed(ys)]
        bl.plate("core%d" % s, core[::-1], 1.30 * s - 0.03, 1.30 * s + 0.03, M["blued"], detail=True)
        for sx in (1, -1):
            line = [(sx * half(y, 0.86), y) for y in ys] + [(0, 123.6)] + [(sx * half(y, 1.0), y) for y in reversed(ys)]
            bl.plate("line%d%d" % (s, sx), line if sx < 0 else line[::-1], 1.30 * s - 0.03, 1.30 * s + 0.045, M["gold_line"], detail=True)
        bl.text("runes%d" % s, RUNES[:12] if s > 0 else RUNES[12:], 3.4, rune, (0.0, 80.0, 1.37 * s), rot=(0, 0 if s > 0 else 180, 90), spacing=1.45, font=rune_font())
    # ---- collar and langets
    bl.bevel(bl.loft("blade_collar", [bl.ring(y, bl.rrect(w, 2.0, 0.7)) for y, w in ((33.0, 7.4), (35.5, 8.3), (39.5, 8.1), (41.0, 7.2))], M["gold"]), 0.25)
    for s in (1, -1):
        lg = bl.plate("langets%d" % s, [(-4.6, 39.5), (4.6, 39.5), (3.6, 50), (1.6, 58), (0, 66), (-1.6, 58), (-3.6, 50)], 1.25 if s > 0 else -2.0, 2.0 if s > 0 else -1.25, M["gold"], part="langets")
        bl.bevel(lg, 0.3)
        bl.plate("langet_in%d" % s, [(-2.6, 41.5), (2.6, 41.5), (2.0, 49), (0, 58), (-2.0, 49)], 2.0 * s - 0.03, 2.0 * s + 0.04, M["gold_deep"], detail=True)
    # ---- guard: a bar, and three swept feather-wings a side (the curse look: three horns a side, drawn in to the old curse look's width, with thorns)
    bl.sweep("guard", [(-10.5, 29.5, 0), (-6, 29.5, 0), (0, 29.5, 0), (6, 29.5, 0), (10.5, 29.5, 0)], bl.rrect(3.4, 2.5, 0.9), M["gold"],
             scale=lambda t: (1.0 - 0.45 * abs(2 * t - 1), 1.0 - 0.2 * abs(2 * t - 1)))
    kx = 0.97 if curse else 1.0                                              # the old curse look's box is 18.84 wide a side
    if curse:
        wings = [("wing_a", [(7, 31.0), (12.5, 31.0), (16.6, 35.0), (18.0, 41.0), (17.2, 46.3)], 3.3, 2.1),
                 ("wing_b", [(7, 28.5), (11.5, 26.2), (15.2, 25.4), (17.6, 27.6), (18.3, 31.5)], 2.5, 1.8),
                 ("wing_c", [(6, 26.5), (9.5, 22.8), (13.0, 20.4), (16.2, 20.3), (18.2, 22.0)], 2.0, 1.5)]
    else:
        wings = [("wing_a", [(7, 31.0), (14, 32.0), (21, 36.5), (25.6, 42.5), (27, 46)], 3.4, 2.1),
                 ("wing_b", [(7, 28.5), (13, 26.5), (19, 26.0), (24, 28.5), (26.6, 32.5)], 2.6, 1.8),
                 ("wing_c", [(6, 26.5), (10.5, 22.5), (15.5, 20.2), (20.5, 20.6), (23.5, 22.8)], 2.1, 1.5)]
    for nm, pts, w, t in wings:
        path = bl.smooth_path([(x * kx, y, 0) for x, y in pts], 3)
        ob = bl.sweep(nm, path, bl.lozenge(1.0, 1.0, flat=0.3), M["gold"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.8 + 0.12, t * (1 - u) ** 0.7 + 0.1), part="guard")
        bl.mirror(ob, "x")
        if curse:
            for s in (1, -1):                                            # a vein of gold fire along each horn, on both faces
                v = bl.sweep(nm + "_vein%d" % s, path[: len(path) * 3 // 4], [(-0.3, t * s - 0.04), (0.3, t * s - 0.04), (0.3, t * s + 0.06), (-0.3, t * s + 0.06)], M["sin"],
                             scale=lambda u, w=w, t=t: (w * 0.45 * (1 - u) ** 0.8 + 0.1, (1 - 0.75 * u) ** 0.7 + 0.03), detail=True)
                bl.mirror(v, "x")
    if curse:
        for k, (x, y, dx, dy) in enumerate(((10.5, 32.9, -0.2, 3.4), (14.6, 35.6, -1.0, 3.2), (16.4, 39.6, -2.4, 2.2), (10.0, 24.4, 0.6, -3.0), (13.8, 23.9, 1.2, -2.8))):
            bl.mirror(spike("thorn%d" % k, (x * kx, y, 0), ((x + dx) * kx, y + dy, 0), 0.75, M["steel"], part="guard"), "x")
    # ---- the gem in its bezel, on both faces
    prof = [(5.1, 2.3), (5.5, 3.2), (6.9, 3.2), (7.4, 2.3), (7.4, -2.3), (6.9, -3.2), (5.5, -3.2), (5.1, -2.3), (5.1, 2.3)]
    bl.lathe("gem_setting", prof, M["gold"], sides=16, origin=(0, 29.5, 0), axis="z", caps=(False, False))
    g = bl.gem("gem", 5.0, gemm, center=(0, 29.5, 2.2), depth=0.55, crown=0.3, sides=8, axis="z")
    bl.copy(g, "gem_back", scale=(1, 1, -1))
    if curse:                                                                # the stone is an eye, on both faces
        for s in (1, -1):
            eye("eye%d" % s, 2.75, (0, 29.5), 6.175, M, s)
    for k in range(8):                                                       # eight claws round the stone
        a = 2 * math.pi * (k + 0.5) / 8
        for s in (1, -1):
            c = bl.plate("claw%d%d" % (k, s), [(-0.5, 0), (0.5, 0), (0.25, 1.6), (-0.25, 1.6)], 3.1 * s - 0.05, 3.1 * s + 0.3 * s if s > 0 else 3.1 * s + 0.05, M["gold"], detail=True)
            bl.move(c, (0, -6.6, 0))
            bl.move(c, rot=(0, 0, math.degrees(a)))
            bl.move(c, (0, 29.5, 0))
    # ---- grip, rings, pommel
    bl.lathe("grip", [(2.0, 7.0), (2.25, 11.5), (2.35, 16.0), (2.25, 20.5), (2.0, 25.5)], wrap, sides=10, squash=0.9)
    for i, y in enumerate((7.4, 16.0, 24.8)):
        bl.lathe("ring%d" % i, [(2.1, y - 0.9), (2.75, y - 0.55), (2.75, y + 0.55), (2.1, y + 0.9)], M["gold"], sides=10, squash=0.92, part="grip_rings")
    bl.lathe("pommel", [(0.01, 0.0), (2.6, 0.5), (4.3, 2.3), (4.6, 3.9), (3.9, 5.6), (2.5, 6.8), (2.1, 7.4)], M["gold"], sides=12)
    pg = bl.gem("pommel_gem", 1.7, gemm, center=(0, 3.6, 4.2), depth=0.5, crown=0.3, sides=6, axis="z")
    bl.copy(pg, "pommel_gem_back", scale=(1, 1, -1))
    if curse:                                                                # six spikes round the pommel: a hungry mace head
        for k in range(6):
            a = math.radians(60 * k)
            spike("pommel_spike%d" % k, (3.6 * math.cos(a), 3.8, 3.6 * math.sin(a)), (7.35 * math.cos(a), 3.4, 7.35 * math.sin(a)), 1.0, M["steel"], part="pommel")
    res = bl.finish(iid, header_from=os.path.join(CLASSIC, iid + ".txt"), **out)
    if curse and out.get("write", True):
        patch_header(iid, out, "grip", "0.076")                              # the hand grip is where it was: the line the old curse look carried
    return res


# ================================================================ helpers of the shield, the spear and the bow
def patch_header(item_id, out, key, value):
    """Pins one header line of a written mesh (the spear's `grip`: its hand grip is where it was, so the line stays what the hand tables know)."""
    path = os.path.join(out.get("out_mesh") or bl.MESH_DIR, item_id + ".txt")
    lines = open(path).read().split("\n")
    lines = [("%s %s" % (key, value)) if l.startswith(key + " ") else l for l in lines]
    assert any(l.startswith(key + " ") for l in lines), key
    open(path, "w", newline="\n").write("\n".join(lines))


def core_inlay(st, ys, tip_y, z, k, blued, gold, rune, glyphs, size, mid_y, flat=0.36, spacing=1.45):
    """The blade core of the sword, for any blade made of stations (y, half width, half thickness): blued steel between two gold lines with
    glowing runes, on both faces, all bake-only."""
    def half(y, kk):
        for (y0, w0, _t0), (y1, w1, _t1) in zip(st, st[1:]):
            if y0 <= y <= y1:
                return kk * flat * (w0 + (w1 - w0) * (y - y0) / (y1 - y0))
        return 0.0
    for s in (1, -1):
        core = [(-half(y, k), y) for y in ys] + [(0, tip_y)] + [(half(y, k), y) for y in reversed(ys)]
        bl.plate("core%d" % s, core[::-1], z * s - 0.03, z * s + 0.03, blued, detail=True)
        for sx in (1, -1):
            line = [(sx * half(y, k), y) for y in ys] + [(0, tip_y + 0.6)] + [(sx * half(y, k + 0.14), y) for y in reversed(ys)]
            bl.plate("line%d%d" % (s, sx), line if sx < 0 else line[::-1], z * s - 0.03, z * s + 0.045, gold, detail=True)
        bl.text("runes%d" % s, glyphs if s > 0 else glyphs[::-1], size, rune, (0.0, mid_y, (z + 0.07) * s), rot=(0, 0 if s > 0 else 180, 90), spacing=spacing, font=rune_font())


def flat_ring(name, r0, r1, z0, z1, material, sides=48, detail=True, part=None):
    """A flat ring lying on a face (round the z axis)."""
    return bl.lathe(name, [(r0, z0), (r0, z1), (r1, z1), (r1, z0), (r0, z0)], material, sides=sides, axis="z", caps=(False, False), detail=detail, part=part)


def spun(ob, deg, center=(0, 0, 0)):
    """Turns an object about the z axis through `center`."""
    bl.move(ob, (-center[0], -center[1], -center[2]))
    bl.move(ob, rot=(0, 0, deg))
    return bl.move(ob, center)


# ================================================================ the Legendary Shield
def shield(out, curse=False):
    import random
    bl.reset()
    iid = "legendary_shield_curse" if curse else "legendary_shield"
    M = curse_metals(SIN["shield"]) if curse else metals()
    if curse:                                                                # the Shield of Wrath
        plate_m, gemm, rune = M["steel"], M["eye"], M["hot"]
        leather = bl.mat("leather", rgb(44, 30, 36), rough=0.7, bump="leather", bstr=0.5, bevel=0.1, groove=rgb(14, 8, 10), gamount=0.5)
    else:
        plate_m = bl.mat("plate_steel", rgb(186, 196, 214), metallic=1.0, rough=0.26, bump="hammer", bscale=0.5, bstr=0.1, bevel=0.2, groove=rgb(128, 140, 166), gamount=0.4)
        gemm = bl.mat("gem", rgb(8, 150, 76), metallic=0.4, rough=0.06, emit=rgb(30, 225, 120), strength=0.35, bevel=0.05)
        rune = bl.mat("rune", rgb(6, 30, 16), rough=0.5, emit=rgb(90, 255, 160), strength=3.0, bevel=0.0)
        leather = bl.mat("leather", rgb(96, 58, 30), rough=0.7, bump="leather", bstr=0.5, bevel=0.1, groove=rgb(40, 22, 10), gamount=0.5)
    # ---- the plate: a low dome with a flat band for the runes, its back closed
    sides = 32 if curse else 40                                              # the curse look pays for its horns and its glowing cracks here
    bl.lathe("plate", [(0.01, 3.6), (12.0, 3.6), (13.0, 2.6), (18.8, 2.6), (19.6, 1.5), (27.0, 1.5), (28.4, 1.0), (28.4, -1.2), (0.01, -1.7)], plate_m, sides=sides, axis="z")
    # the inner field: sixteen engraved sun rays between two gold lines
    flat_ring("field_in", 13.1, 13.6, 2.57, 2.66, M["gold_line"])
    flat_ring("field_out", 18.1, 18.7, 2.57, 2.66, M["gold_line"])
    if curse:                                                                # the sun rays have split into cracks of fire, and the fire runs on to the rim
        rng = random.Random(11)
        for k in range(16):
            a = math.radians(11.25 + 22.5 * k)
            c, sn = math.cos(a), math.sin(a)
            crack("crack_in%d" % k, jag(rng, (13.7 * c, 13.7 * sn), (18.0 * c, 18.0 * sn), 4, 0.7), lambda x, y: 2.62, 1, 0.5, M["sin"])
            crack("crack_out%d" % k, jag(rng, (19.9 * c, 19.9 * sn), (27.6 * c, 27.6 * sn), 5, 0.9), lambda x, y: 1.6, 1, 0.42, M["sin"])
    else:
        for k in range(16):
            spun(bl.plate("sun%d" % k, [(0, 13.9), (0.75, 15.0), (0, 17.8), (-0.75, 15.0)], 2.57, 2.64, M["blued"], detail=True), 11.25 + 22.5 * k)
    # the rune band: blued steel between two gold lines, 24 glowing runes in four arcs between the long rays
    flat_ring("band", 21.2, 26.2, 1.47, 1.53, M["blued"])
    flat_ring("band_in", 20.6, 21.2, 1.47, 1.56, M["gold_line"])
    flat_ring("band_out", 26.2, 26.8, 1.47, 1.56, M["gold_line"])
    for q in range(4):
        for k in range(6):
            a = math.radians(q * 90 + 15 + k * 12)
            bl.text("rune%d%d" % (q, k), RUNES[q * 6 + k], 3.5, rune, (23.7 * math.cos(a), 23.7 * math.sin(a), 1.6), rot=(0, 0, math.degrees(a) - 90), font=rune_font())
    # ---- the rim: a heavy gold ring with eight green stones
    bl.lathe("rim", [(27.9, 1.6), (28.6, 2.6), (30.3, 2.5), (31.0, 1.3), (31.0, -1.2), (30.2, -1.8), (28.4, -1.8), (27.9, -1.0), (27.9, 1.6)], M["gold"], sides=sides, axis="z", caps=(False, False))
    for k in range(8):
        a = math.radians(22.5 + 45 * k)
        bl.gem("rim_gem%d" % k, 1.55, gemm, center=(29.5 * math.cos(a), 29.5 * math.sin(a), 2.45), depth=0.4, crown=0.35, sides=4 if curse else 6, axis="z", part="rim_gems")
    # ---- the compass star: four long gold rays over the band, four short ones between them, each with a blued inlay
    for k in range(4):
        ray = bl.bevel(bl.plate("ray%d" % k, [(0, 10.5), (3.0, 15.0), (0, 28.2), (-3.0, 15.0)], 1.0, 3.3, M["gold"], part="wing_plates"), 0.4)
        spun(ray, 90 * k)
        spun(bl.plate("ray_in%d" % k, [(0, 13.2), (1.1, 15.2), (0, 23.6), (-1.1, 15.2)], 3.27, 3.34, M["gold_deep"] if curse else M["blued"], detail=True), 90 * k)
        short = bl.bevel(bl.plate("short%d" % k, [(0, 10.5), (2.6, 14.0), (0, 21.0), (-2.6, 14.0)], 1.0, 3.5, M["gold"], part="ribs"), 0.35)
        spun(short, 45 + 90 * k)
        spun(bl.plate("short_in%d" % k, [(0, 12.9), (1.1, 14.1), (0, 18.6), (-1.1, 14.1)], 3.47, 3.54, M["steel"], detail=True), 45 + 90 * k)
    # ---- the boss: a stepped gold ring, eight studs, the great stone in its setting
    bl.lathe("boss", [(12.6, 2.0), (12.6, 4.5), (11.5, 5.6), (9.4, 6.0), (9.0, 5.2)], M["gold"], sides=32, axis="z", caps=(False, False))
    flat_ring("boss_line", 10.1, 10.6, 5.7, 5.95, M["gold_deep"], sides=32)
    for k in range(8):
        a = math.radians(45 * k)
        bl.lathe("stud%d" % k, [(0.9, 0.0), (0.8, 0.5), (0.45, 0.85), (0.01, 0.95)], M["gold"], sides=6, axis="z", origin=(11.6 * math.cos(a), 11.6 * math.sin(a), 5.3), caps=(False, True), part="studs")
    bl.lathe("gem_setting", [(9.2, 5.0), (8.8, 8.3), (7.9, 9.5), (6.8, 9.5), (6.5, 8.7), (6.5, 5.0)], M["gold"], sides=24, axis="z", caps=(False, False))
    bl.gem("gem", 6.6, gemm, center=(0, 0, 6.3), depth=0.5, crown=0.33, sides=8, axis="z")
    if curse:
        eye("eye", 3.63, (0, 0), 11.448, M, slit=0.42)                                  # the great stone is the eye of wrath
        for k in range(6):                                                   # six claws close over it
            a = math.radians(30 + 60 * k)
            c, sn = math.cos(a), math.sin(a)
            bl.tube("eye_claw%d" % k, [(9.0 * c, 9.0 * sn, 6.0), (8.6 * c, 8.6 * sn, 9.6), (6.6 * c, 6.6 * sn, 11.6), (4.3 * c, 4.3 * sn, 12.1)], 0.8, M["steel"], sides=4, sub=2, part="gem_setting", taper=(0.95, 0.12))
        horns = []                                                           # twelve hooked horns round the rim: the long ones stand on the compass points
        for k in range(12):
            a0, ln = math.radians(30 * k), (10.2 if k % 3 == 0 else 7.2)
            pts = []
            for i in range(6):
                u = i / 5.0
                a = a0 + math.radians(13.0) * u ** 1.6
                pts.append(((29.6 + ln * u) * math.cos(a), (29.6 + ln * u) * math.sin(a), 0.6 + 3.0 * u ** 1.5))
            w = 3.0 if k % 3 == 0 else 2.3
            horns.append(bl.sweep("horn%d" % k, pts, bl.lozenge(1.0, 1.0), M["steel"], scale=lambda u, w=w: (w * (1 - u) ** 0.85 + 0.08, 1.7 * (1 - u) ** 0.8 + 0.08), part="horns"))
        fit_extent(horns, "x", 39.249)                                       # the box of the old curse look: the shield frame scales by it
        fit_extent(horns, "y", 41.734)
        for k in range(12):                                                  # the root of each horn glows
            a = math.radians(30 * k + 2.0)
            spun(bl.plate("horn_fire%d" % k, [(-1.1, 30.9), (1.1, 30.9), (0.5, 34.5), (0, 36.5), (-0.5, 34.5)], 1.9, 2.0, M["gold_deep"], detail=True), math.degrees(a) - 90)
    # ---- the back: the arm strap and the grip bar
    bl.sweep("arm_strap", bl.smooth_path([(-15.0, -9, -1.5), (-9, -9, -3.9), (0, -9, -4.7), (9, -9, -3.9), (15.0, -9, -1.5)], 3), bl.rrect(3.0, 0.45, 0.2), leather)
    bl.tube("grip_bar", [(-6.4, 6, -1.2), (-5.6, 6, -4.6), (0, 6, -5.5), (5.6, 6, -4.6), (6.4, 6, -1.2)], 0.75, leather, sides=6, sub=3)
    return bl.finish(iid, glow=("FFFFFF", 0.1, 0.55, 1.0), header_from=os.path.join(CLASSIC, iid + ".txt"), **out)


# ================================================================ the Legendary Spear
def spear(out, curse=False):
    import random
    bl.reset()
    iid = "legendary_spear_curse" if curse else "legendary_spear"
    M = curse_metals(SIN["spear"]) if curse else metals()
    if curse:                                                                # the Spear of Lust
        pearl = M["steel"]
        wrap = bl.mat("wrap", rgb(36, 24, 44), rough=0.55, bump="cross", bstr=0.9, groove=rgb(150, 20, 110), gamount=1.0, bevel=0.0)
        gemm, rune = M["eye"], M["hot"]
    else:
        pearl = bl.mat("pearl", rgb(226, 230, 238), metallic=0.85, rough=0.3, bump="brushed", bstr=0.12, bevel=0.15, groove=rgb(170, 178, 198), gamount=0.5)
        wrap = bl.mat("wrap", rgb(150, 24, 34), rough=0.55, bump="cross", bstr=0.9, groove=rgb(40, 4, 10), gamount=1.0, bevel=0.0)
        gemm = bl.mat("gem", rgb(225, 30, 40), metallic=0.3, rough=0.08, emit=rgb(255, 60, 60), strength=0.55, bevel=0.05)
        rune = bl.mat("rune", rgb(40, 8, 8), rough=0.5, emit=rgb(255, 120, 100), strength=3.0, bevel=0.0)
    kw, kx = (0.62, 0.52) if curse else (1.0, 1.0)                           # the old curse look's box is 12.49 wide a side: a narrower head, the blades drawn in
    # ---- butt, shaft (in short lengths: a 2 m strip would set the scale of the whole skin), hand wrap, bands
    bl.lathe("butt_cap", [(0.01, 0.0), (1.1, 1.6), (2.4, 6.0), (3.6, 9.4), (3.8, 11.4), (3.0, 12.8), (2.9, 16.4)], M["gold"], sides=10)
    for i, (y0, y1) in enumerate(((16, 58), (58, 98), (124, 170), (170, 217))):
        bl.lathe("shaft%d" % i, [(2.4, y0), (2.4, y1)], pearl, sides=10, squash=0.94, caps=(False, False), part="shaft")
    bl.lathe("grip_wrap", [(2.6, 98.0), (2.8, 104), (2.8, 118), (2.6, 124.0)], wrap, sides=10, squash=0.94, caps=(False, False))
    for i, y in enumerate((18.6, 58.0, 96.8, 125.2, 170.0, 207.5)):
        bl.lathe("band%d" % i, [(2.5, y - 1.1), (2.95, y - 0.7), (2.95, y + 0.7), (2.5, y + 1.1)], M["gold"], sides=10, squash=0.95, caps=(False, False), part="bands")
    # ---- socket, the stone in four claws, collar
    bl.lathe("socket", [(2.5, 216.0), (3.5, 218.0), (3.0, 224.0), (3.1, 230.0), (3.7, 235.0), (3.3, 238.0)], M["gold"], sides=10)
    bl.lathe("gem", [(0.01, 240.0), (3.3, 241.3), (5.0, 243.6), (5.4, 245.5), (5.0, 247.4), (3.3, 249.7), (0.01, 251.0)], gemm, sides=10)
    for k in range(4):
        a = math.radians(45 + 90 * k)
        c, sn = math.cos(a), math.sin(a)
        bl.tube("claw%d" % k, [(3.0 * c, 236.5, 3.0 * sn), (4.6 * c, 240.5, 4.6 * sn), (4.85 * c, 245.5, 4.85 * sn), (4.3 * c, 250.5, 4.3 * sn), (3.0 * c, 254.0, 3.0 * sn)], 0.8, M["gold"], sides=5, sub=2, part="claws", taper=(0.95, 0.6))
    bl.lathe("head_collar", [(3.2, 248.8), (4.0, 249.8), (4.0, 253.6), (3.2, 254.8)], M["gold"], sides=10, caps=(False, False))
    # ---- the head: a broad leaf with a blued, rune-lit core
    st = [(252, 4.0, 2.2), (256, 9.2, 2.2), (262, 12.4, 2.2), (270, 13.3, 2.2), (284, 11.6, 2.2), (298, 8.6, 2.2), (310, 5.8, 1.8), (320, 3.2, 1.2), (326, 1.5, 0.7), (330, 0.1, 0.08)]
    st = [(y, w * kw, t) for y, w, t in st]
    bl.loft("spearhead", [bl.ring(y, bl.lozenge(w, t, flat=w * 0.4)) for y, w, t in st], M["steel"])
    core_inlay(st, [258, 262, 270, 284, 296], 306, 2.2, 0.8, M["blued"], M["gold_line"], rune, RUNES[3:9], 3.6 if curse else 4.6, 277.0, flat=0.4, spacing=1.5)
    # ---- wings below the head and lugs beside it: swept blades, a steel edge on a gold root
    blades = [("wings", [(3.4, 225.0), (10.5, 222.6), (17.2, 226.5), (21.0, 237.0), (21.7, 253.6)], 3.3, 1.0),
              ("lugs", [(4.6, 255.0), (11.5, 256.0), (18.0, 262.5), (21.9, 273.0), (23.2, 285.0)], 2.5, 1.0)]
    for nm, pts, w, t in blades:
        path = bl.smooth_path([(x * kx, y, 0) for x, y in pts], 3)
        ob = bl.sweep(nm, path, bl.lozenge(1.0, 1.0, flat=0.35), M["steel"], scale=lambda u, w=w, t=t: (w * (1 - u) ** 0.75 + 0.1, t * (1 - u) ** 0.6 + 0.08))
        bl.mirror(ob, "x")
        for s in (1, -1):                                                    # a gold spine baked along each blade's first half
            sp = bl.sweep(nm + "_spine%d" % s, path[: len(path) // 2 + 1], [(-0.45, t * s - 0.04), (0.45, t * s - 0.04), (0.45, t * s + 0.05), (-0.45, t * s + 0.05)], M["gold_line"],
                          scale=lambda u, w=w: (w * 0.5 * (1 - 0.5 * u) ** 0.75 + 0.1, 1.0), detail=True)
            bl.mirror(sp, "x")
    if curse:
        rng = random.Random(23)
        # a thorned vine winds up the shaft to the head, a line of magenta light along it
        turns, y0, y1 = 3.5, 150.0, 240.0
        helix = lambda u, r: (r * math.cos(2 * math.pi * turns * u), y0 + (y1 - y0) * u, r * math.sin(2 * math.pi * turns * u))      # noqa: E731
        bl.tube("vine", [helix(i / 28.0, 3.0) for i in range(29)], 0.7, M["gold"], sides=4, sub=1, taper=(0.5, 0.8))
        bl.tube("vine_light", [helix(i / 28.0, 3.8) for i in range(29)], 0.3, M["sin"], sides=3, sub=1, detail=True)
        for k in range(14):                                                  # thorns: on the vine, then up the socket and the edges of the head
            u = (k + 0.5) / 14.0
            b, t = helix(u, 3.2), helix(u + 0.012, 5.9 if k % 2 else 5.0)
            spike("thorn%d" % k, b, (t[0], t[1] + 2.2, t[2]), 0.62, M["steel"], sides=3, part="thorns")
        for k in range(6):
            y = 262.0 + 8.0 * k
            w = next(w0 + (w1 - w0) * (y - ya) / (yb - ya) for (ya, w0, _), (yb, w1, _) in zip(st, st[1:]) if ya <= y <= yb)
            bl.mirror(spike("barb%d" % k, (w - 0.5, y, 0), (w + 2.6, y - 3.0, 0), 0.7, M["steel"], sides=3, part="thorns", bend=(0.5, 0.6, 0)), "x")
        for s in (1, -1):                                                    # cracks of light from the core to the edges of the head
            for i in range(6):
                yc, sx = 261.0 + 6.5 * i, (1 if i % 2 == 0 else -1)
                w = next(w0 + (w1 - w0) * (yc - ya) / (yb - ya) for (ya, w0, _), (yb, w1, _) in zip(st, st[1:]) if ya <= yc <= yb)
                f = 0.4 * w
                zf = lambda x, y, f=f, w=w: 2.2 * s * (1.0 if abs(x) <= f else max(0.0, (w - abs(x)) / (w - f))) + 0.02 * s      # noqa: E731
                crack("crack%d%d" % (s, i), jag(rng, (sx * f * 0.75, yc), (sx * (w - 0.1), yc + 3.6), 4, 0.4), zf, s, 0.34, M["sin"])
        # two chains hang from the wing tips
        for sx in (1, -1):
            x0 = sx * 20.3 * kx
            for k in range(5):
                yc = 250.0 - 5.2 * k
                loop = [(0.0, 3.1), (1.25, 2.2), (1.25, -2.2), (0.0, -3.1), (-1.25, -2.2), (-1.25, 2.2), (0.0, 3.1)]
                pts = [((x0 + a, yc + b, 0.0) if k % 2 == 0 else (x0, yc + b, a)) for a, b in loop]
                bl.tube("chain%d%d" % (sx > 0, k), pts, 0.42, M["gold"], sides=3, sub=1, part="chains")
    res = bl.finish(iid, header_from=os.path.join(CLASSIC, iid + ".txt"), **out)
    if out.get("write", True):
        patch_header(iid, out, "grip", "0.224" if curse else "0.233")        # the hand wrap is where it was (y 98 to 124)
    return res


# ================================================================ the Legendary Bow (and its three draw stages)
BW_HALF, BW_BELLY, BW_RISER, BW_POWER, BW_CURL = 80.0, 22.0, 12.0, 1.9, 7.0     # the stave of tools/heroweapon/art_hart1.py: the rig of the draw stages depends on it
BW_ARROW_Z, BW_DRAW, BW_FLEX, BW_FAR = 3.3, 32.0, 8.0, 400.0                  # BW_FAR: the arrow is baked this far off in z, so it neither shadows nor lights the bow


def bow_x(y):
    u = max(0.0, (abs(y) - BW_RISER) / (BW_HALF - BW_RISER))
    c = max(0.0, (u - 0.78) / 0.22)
    return BW_BELLY * (1.0 - u ** BW_POWER) + BW_CURL * c * c


def bow(out, curse=False):
    import numpy as np
    sys.path.insert(0, os.path.join(bl.ROOT, "tools", "relics"))
    import ranged_meshes as rm
    ml = bl.ml
    bl.reset()
    iid = "legendary_bow_curse" if curse else "legendary_bow"
    M = curse_metals(SIN["bow"], line=0.85) if curse else metals()      # violet clips to pink sooner than the other fires
    if curse:                                                                # the Bow of Pride
        pearl = M["steel"]
        wrap = bl.mat("wrap", rgb(34, 26, 46), rough=0.6, bump="wrap", bstr=0.9, groove=rgb(96, 50, 150), gamount=1.0, bevel=0.0)
        gemm, rune = M["eye"], M["sin"]
        light = bl.mat("light", rgb(230, 205, 255), rough=0.5, emit=rgb(205, 150, 255), strength=1.6, bevel=0.0)
    else:
        pearl = bl.mat("pearl", rgb(228, 232, 240), metallic=0.85, rough=0.28, bump="brushed", bstr=0.1, bevel=0.18, groove=rgb(176, 184, 204), gamount=0.5)
        wrap = bl.mat("wrap", rgb(196, 160, 104), rough=0.6, bump="wrap", bstr=0.9, groove=rgb(84, 58, 28), gamount=1.0, bevel=0.0)
        gemm = bl.mat("gem", rgb(235, 150, 10), metallic=0.4, rough=0.06, emit=rgb(255, 190, 40), strength=0.3, bevel=0.05)
        light = bl.mat("light", rgb(255, 244, 205), rough=0.5, emit=rgb(255, 238, 170), strength=1.6, bevel=0.0)
        rune = bl.mat("rune", rgb(40, 30, 6), rough=0.5, emit=rgb(255, 225, 120), strength=3.0, bevel=0.0)
    gz = 2.6 if curse else 3.0                                               # the old curse look's box is thinner (z 4.945): the eye sits lower in the riser
    n = 14
    ys = [BW_RISER - 1.0 + (BW_HALF - (BW_RISER - 1.0)) * i / n for i in range(n + 1)]
    path = [(bow_x(y), y, 0.0) for y in ys]
    # ---- limbs: a flat pearl stave tapering to the tips, a gold inlay and a line of light baked on both faces, a steel fin along the front
    up = bl.sweep("limb_upper", path, bl.rrect(1.0, 1.0, 0.32), pearl, scale=lambda t: (1.45 - 0.75 * t, 3.4 - 2.3 * t))
    lo = bl.mirror(up, "y", "limb_lower")
    lo["part"] = "limb_lower"
    for s in (1, -1):
        zf = lambda t: (3.4 - 2.3 * t) * s                                    # noqa: E731
        inl = bl.sweep("inlay%d" % s, path[:-1], lambda t: [(-0.62 + 0.3 * t, zf(t * 13 / 14) - 0.03), (0.62 - 0.3 * t, zf(t * 13 / 14) - 0.03), (0.62 - 0.3 * t, zf(t * 13 / 14) + 0.04), (-0.62 + 0.3 * t, zf(t * 13 / 14) + 0.04)], M["gold_line"], detail=True)
        bl.mirror(inl, "y")
        lit = bl.sweep("lit%d" % s, path[1:-2], lambda t: [(-0.16, zf((1 + t * 11) / 14) - 0.03), (0.16, zf((1 + t * 11) / 14) - 0.03), (0.16, zf((1 + t * 11) / 14) + 0.07), (-0.16, zf((1 + t * 11) / 14) + 0.07)], rune, detail=True)
        bl.mirror(lit, "y")
    fin = bl.sweep("limb_blades", path[1:-1], [(-1.2, 0.3), (-1.2, -0.3), (-3.1, 0.0)], M["steel"], scale=lambda t: (1.0 - 0.45 * t, 1.0))
    # the sweep's u axis points to the string on the upper limb: -u is the front of the bow
    bl.mirror(fin, "y")
    tip = bl.sweep("limb_tips", bl.smooth_path([(7.0, 79.2, 0), (8.2, 82.2, 0), (11.2, 83.9, 0), (15.3, 83.0, 0)], 3), bl.circle(1.0, 6), M["gold"], scale=lambda t: (1.25 * (1 - t) ** 0.7 + 0.12, 1.3 * (1 - t) ** 0.7 + 0.12))
    bl.move(tip, scale=(1.0, 84.239 / max(v.co.y for v in tip.data.vertices), 1.0))      # the bow's length is the place line's: tip to tip 2 x 84.239
    bl.mirror(tip, "y")
    # ---- riser: gold, swelling round the stone, with the hand wrap below it
    rs = [(-15.0, 2.4, 2.9), (-11.5, 3.2, 3.4), (-9.8, 2.9, 3.2), (2.0, 2.9, 3.2), (4.0, 4.2, 3.4), (7.6, 5.4, 3.4), (11.2, 4.0, 3.4), (15.0, 2.5, 2.9)]
    bl.loft("riser", [bl.ring(y, bl.rrect(w, t, 0.9), ox=bow_x(y)) for y, w, t in rs], M["gold"])
    bl.loft("grip_wrap", [bl.ring(y, bl.rrect(w, t, 1.2), ox=BW_BELLY) for y, w, t in ((-9.6, 3.1, 3.5), (-7.0, 3.5, 3.9), (-1.0, 3.5, 3.9), (1.8, 3.1, 3.5))], wrap, caps=(False, False))
    prof = [(4.8, 3.3), (5.2, 4.0), (5.9, 4.0), (5.9, -4.0), (5.2, -4.0), (4.8, -3.3)]
    bl.lathe("gem_setting", prof + prof[:1], M["gold_deep"], sides=16, origin=(BW_BELLY, 7.6, 0), axis="z", caps=(False, False))
    g = bl.gem("gem", 4.7, gemm, center=(BW_BELLY, 7.6, gz), depth=0.24, crown=0.28, sides=8, axis="z")
    bl.copy(g, "gem_back", scale=(1, 1, -1))
    if curse:
        for s in (1, -1):
            eye("eye%d" % s, 2.585, (BW_BELLY, 7.6), gz + 4.7 * (0.24 * 0.9 + 0.28), M, s)
    # ---- wings: four feathers a side fanning forward from the riser (they do not flex)
    feathers = [([(24.0, 4.5), (28.5, 9.0), (32.0, 15.0), (33.5, 22.0)], 2.5), ([(24.2, 8.5), (28.0, 16.0), (31.2, 24.5), (32.8, 33.0)], 2.7),
                ([(23.8, 11.5), (26.2, 21.0), (28.6, 31.5), (29.6, 41.0)], 2.6), ([(22.6, 13.5), (23.6, 25.0), (25.0, 37.0), (25.4, 47.0)], 2.3)]
    fx = 0.9 if curse else 1.0                                               # the curse look's feathers are drawn in (its box ends at x 32.83)
    for i, (pts, w) in enumerate(feathers):
        fpath = bl.smooth_path([(22.0 + (x - 22.0) * fx, y, 0) for x, y in pts], 3)
        f = bl.sweep("feather%d" % i, fpath, bl.lozenge(1.0, 1.0, flat=0.3), M["gold"] if i % 2 == 0 else M["steel"], scale=lambda u, w=w: (w * math.sin(math.pi * (0.12 + 0.88 * u)) ** 0.6 * (1 - 0.55 * u) + 0.1, 0.75 * (1 - u) ** 0.5 + 0.08), part="wings")
        bl.mirror(f, "y")
        if curse:
            for s in (1, -1):                                                # a vein of violet light down each blade
                v = bl.sweep("feather_vein%d%d" % (i, s), fpath[1:-2], [(-0.22, 0.66 * s - 0.04), (0.22, 0.66 * s - 0.04), (0.22, 0.66 * s + 0.06), (-0.22, 0.66 * s + 0.06)], M["sin"], scale=lambda u: (1.0 - 0.6 * u, (1 - 0.8 * u) ** 0.5 + 0.02), detail=True)
                bl.mirror(v, "y")
    if curse:
        # a horn grows out of each limb tip and curls back; it flexes with the limb (its part name starts with "limb")
        horn = bl.sweep("limb_horns", bl.smooth_path([(8.6, 78.0, 0), (7.6, 84.0, 0), (5.9, 90.0, 0), (4.3, 94.0, 0), (3.4, 96.0, 0)], 2), bl.lozenge(1.0, 1.0), M["steel"], scale=lambda u: (1.7 * (1 - u) ** 0.8 + 0.06, 1.25 * (1 - u) ** 0.8 + 0.06))
        bl.move(horn, scale=(1.0, 96.0 / max(v.co.y for v in horn.data.vertices), 1.0))
        bl.mirror(horn, "y")
        for s in (1, -1):
            hv = bl.sweep("horn_vein%d" % s, [(8.2, 80.0, 0), (7.3, 85.0, 0), (5.9, 90.0, 0), (4.6, 93.2, 0)], [(-0.25, 0.7 * s - 0.04), (0.25, 0.7 * s - 0.04), (0.25, 0.7 * s + 0.07), (-0.25, 0.7 * s + 0.07)], M["sin"], scale=lambda u: (1.0 - 0.6 * u, 1.0 - 0.72 * u), detail=True)
            bl.mirror(hv, "y")
        # the crest: five spikes fanning forward from the riser round the eye
        for k, (a, ln) in enumerate(((-62, 9.0), (-32, 10.2), (0, 9.2), (32, 10.2), (62, 9.0))):
            c, sn = math.cos(math.radians(a)), math.sin(math.radians(a))
            spike("crest%d" % k, (BW_BELLY + 1.5 * c, 7.6 + 4.6 * sn, 0), (BW_BELLY + 1.5 + ln * c, 7.6 + (4.6 + ln) * sn, 0), 1.5, M["steel"], part="crest")
    # ---- string and the arrow of light (the arrow is drawn only in the draw stages)
    bl.sweep("string", [(BW_CURL, BW_HALF - 2.0 * BW_HALF * i / 12, 0.0) for i in range(13)], bl.circle(0.24, 4), light)
    x0, z = BW_CURL, BW_ARROW_Z + BW_FAR
    bl.lathe("arrow_shaft", [(0.62, x0 - 1.5), (0.4, x0), (0.4, x0 + 68.0)], light, sides=6, axis="x", origin=(0, 0, z))
    bl.lathe("arrow_head", [(0.5, x0 + 67.6), (1.15, x0 + 70.6), (0.01, x0 + 74.0)], light, sides=6, axis="x", origin=(0, 0, z), caps=(True, False))
    for k in range(3):
        v = bl.plate("vane%d" % k, [(x0 + 1.4, 0.35), (x0 + 12.0, 0.35), (x0 + 12.0, 2.0 if curse else 2.2), (x0 + 4.0, 1.75 if curse else 1.9)], -0.07, 0.07, light, part="arrow_fletch")
        bl.move(v, rot=(120 * k, 0, 0))
        bl.move(v, (0, 0, z))
    kw = dict(out)
    preview = kw.pop("preview", None)
    kw["write"] = False
    mesh, atlas, stats = bl.finish(iid, **kw)
    # ---- the four meshes: the rest (no arrow) and three draws, with the rig the code-built bow used (tools/relics/ranged_meshes.py)
    with open(os.path.join(CLASSIC, iid + ".txt")) as fh:
        place = next([float(x) for x in l.split()[1:6]] for l in fh if l.startswith("place "))
    rig = rm.Rig(tip_a=(BW_CURL, BW_HALF, 0.0), tip_b=(BW_CURL, -BW_HALF, 0.0), draw_dir=(-1, 0, 0), max_draw=BW_DRAW, flex=BW_FLEX, grip=(BW_BELLY, 0.0, 0.0), limb_axis=(0, 1, 0), limb_span=BW_HALF)
    nock = rig.flex + rig.max_draw

    def state(f, with_arrow):
        m2 = ml.Mesh(iid)
        m2.finish = lambda: None
        m2.place(*place)
        m2.set_glow("FFFFFF", 0.12, 0.5, 1.0)
        m2.orient_cfg = "manual"
        for p in mesh.parts:
            role = rm._role(p.name)
            if role == "arrow" and not with_arrow:
                continue
            c = rm._copy_part(p)
            if role == "limb":
                c.V = list(rm.displace_limb(rig, np.array(p.V), 0.0, f))
            elif role == "string":
                c.V = list(rm.displace_string(rig, np.array(p.V), 0.0, f))
            elif role == "arrow":
                c.V = list(np.array(p.V) + rig.d() * nock * f - np.array((0.0, 0.0, BW_FAR)))
            m2.add(c)
        return m2

    states = [("", state(0.0, False))] + [(suf, state(f, True)) for suf, f, _ in rm.BOW_STATES]
    for suf, m2 in states:
        lines = sum(m2.stats(atlas)["quads"].values())
        lo_, hi_ = m2.bbox()
        print("%s%s: %d quad lines, box %s .. %s" % (iid, suf, lines, lo_.round(3).tolist(), hi_.round(3).tolist()))
        assert lines <= bl.QUAD_BUDGET, lines
    if preview:
        ml.preview(states[0][1], None, preview, atlas=atlas, normals="vertex")
        ml.preview(states[3][1], None, preview.replace("_preview", "_pull3_preview"), atlas=atlas, normals="vertex")
    if out.get("write", True):
        from PIL import Image
        md, sd = out.get("out_mesh") or bl.MESH_DIR, out.get("out_skin") or bl.SKIN_DIR
        for suf, m2 in states:
            m2.write_txt(os.path.join(md, iid + suf + ".txt"), atlas, normals="vertex", comment="made in Blender by tools/blender (legendary.py)" + (" (%s)" % suf if suf else ""))
        Image.fromarray(np.clip(atlas.img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, iid + ".png"), optimize=True)
        Image.fromarray(np.clip(atlas.glow, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(sd, iid + "_glow.png"), optimize=True)
    return states[0][1], atlas, stats


BUILDERS = {"sword": sword, "shield": shield, "spear": spear, "bow": bow,
            "sword_curse": lambda out: sword(out, True), "shield_curse": lambda out: shield(out, True),
            "spear_curse": lambda out: spear(out, True), "bow_curse": lambda out: bow(out, True)}


def main(argv):
    only = None
    trial = None
    for i, a in enumerate(argv):
        if a == "--only":
            only = argv[i + 1].split(",")
        if a == "--trial":
            trial = argv[i + 1]
    quick = "--quick" in argv                                               # a fast look while modelling: few samples, a 512 px bake
    if quick:
        full = bl.reset
        bl.reset = lambda samples=6: full(samples)
    for name, fn in BUILDERS.items():
        if only and name not in only:
            continue
        out = {}
        if trial:
            os.makedirs(trial, exist_ok=True)
            out = dict(out_mesh=trial, out_skin=trial, preview=os.path.join(trial, "legendary_" + name + "_preview.png"))
        if quick:
            out["bake"] = 512
        if "--preview" in argv:                                             # also when writing into the assets
            pdir = argv[argv.index("--preview") + 1]
            os.makedirs(pdir, exist_ok=True)
            out["preview"] = os.path.join(pdir, "legendary_" + name + "_preview.png")
        fn(out)


if __name__ == "__main__":
    main(sys.argv[1:])

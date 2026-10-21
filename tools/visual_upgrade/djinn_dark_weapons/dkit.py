"""The geometry and painting kit of the slice `djinn_dark_weapons`: motifs that are added to the frame of an old Djinn weapon (frame.py), each a function of the build context `K`.

A motif adds NAMED PARTS to `K.m` (a mesh_lib Mesh, built with the owner's method: real sections, lofts, sweeps, lathes) and queues painters in `K.post` that run once the skin is laid out
(core glow, energy veins, engraved runes). The motifs are grouped the way the Djinn rows of docs/djinn_roster_36.json are: by HORNS, WINGS, TAIL and HALO style (the look of the Djinn's
armour set, so the weapon carries the same heraldry), by AFFINITY (lightning, heat, water, wind, life, strength, sound, light) and by what every dark weapon shares (the dark rig:
grip bands, a crown of thorns at the pommel, a name plate, an eye gem, a chain).
`K.d` is the detail factor: it scales segment, side and repeat counts so a build can be sized to its quad budget (`ditems.size_detail`).
Units are the mesh's own; +y runs along the weapon from the butt to the tip, the edge faces -x, the flat faces +z (docs/weapon_scale_orientation.md).
"""
import math

import numpy as np

import dlib as D
from frame import Frame

ml, nl, gb, w = D.ml, D.nl, D.gb, D.w
P = nl.P
TAU = 2 * math.pi


def unit(v):
    v = np.asarray(v, float)
    n = np.linalg.norm(v)
    return v / n if n > 1e-9 else v


def basis(n):
    """Two unit vectors perpendicular to n (and to each other)."""
    n = unit(n)
    h = np.array([0.0, 1.0, 0.0]) if abs(n[1]) < 0.9 else np.array([1.0, 0.0, 0.0])
    u = unit(np.cross(n, h))
    return u, unit(np.cross(n, u))


def mix(a, b, t):
    return tuple(int(round(a[i] * (1 - t) + b[i] * t)) for i in range(3))


def hex_rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


class Ctx:
    def __init__(self, item, spec, old, detail=1.0):
        self.item, self.sp, self.old, self.d = item, spec, old, detail
        self.F = Frame(old.quads, spec["kind"])
        self.m = ml.Mesh(item)
        self.post = []
        self.cnt = {}
        F = self.F
        L = F.length
        self.rng = np.random.default_rng(D.hash_seed(item))
        # palette: dark metal tinted by the Djinn's colour, gold-bronze trim, the Djinn's own colour for crystals and light
        c, g = hex_rgb(spec["color"]), hex_rgb(spec["glow"])
        self.col, self.glow = c, g
        self.iron = mix((26, 24, 34), c, 0.18)
        self.iron2 = mix((44, 40, 52), c, 0.26)
        self.trim = mix((176, 136, 54), g, 0.18)
        self.cloth = mix((34, 22, 36), c, 0.42)
        self.gem = mix(c, g, 0.35)
        self.vein = g
        # anchors
        ov = spec.get("anchors", {})
        self.yh = ov.get("head", F.head_start())
        self.crest = ov.get("crest", self.yh + 0.12 * (F.y1 - self.yh))
        self.grip_lo = ov.get("grip_lo", F.y0 + 0.05 * L)
        self.grip_hi = ov.get("grip_hi", F.y0 + 0.30 * L)
        self.head_r = ov.get("head_r", max(F.radius(y) for y in np.linspace(self.yh, F.y1, 8)))
        self.head_c = F.axis(ov.get("head_mid", (self.yh + F.y1) / 2))
        self.butt = F.axis(F.y0)

    def S(self, n, lo=3):
        return max(lo, int(round(n * self.d)))

    def name(self, base):
        self.cnt[base] = self.cnt.get(base, 0) + 1
        return "%s_%d" % (base, self.cnt[base])

    def part_count(self):
        return sum(len(p.F) for p in self.m.parts)


# ================================================================ painters (queued, run after the layout)
def q_glow(K, part, rgb=None, strength=0.95, core=0.45):
    K.post.append(lambda atlas, g2, p=part: nl.paint_glow(atlas, p, rgb or K.glow, strength, core))


def q_vein(K, part, rgb=None, strength=0.95, core=0.5):
    K.post.append(lambda atlas, g2, p=part: gb.glow_part2(atlas, g2, p, rgb or K.vein, strength, core))


def q_runes(K, part, rgb=None, seed=1, on_core=False):
    K.post.append(lambda atlas, g2, p=part: engrave_runes(atlas, g2, p, rgb or K.vein, seed, on_core))


def q_vein_lines(K, part, rgb=None, strength=0.95):
    """Energy lines on a WIDE part (a ribbon, a wing blade, a tail blade): a centre line and two edge lines on every UV group instead of a lit fill, so the part keeps its dark body."""
    K.post.append(lambda atlas, g2, p=part: paint_lines(atlas, g2, p, rgb or K.vein, strength))


def paint_lines(atlas, g2, part, rgb, strength):
    from PIL import Image, ImageDraw, ImageFilter
    for g in part.groups:
        x, y, wd, ht = g.rect
        if wd < 8 or ht < 8:
            continue
        mask = Image.new("L", (wd, ht), 0)
        dr = ImageDraw.Draw(mask)
        long_is_h = ht >= wd
        wlin = max(1, int(round(min(wd, ht) * 0.06)))
        if long_is_h:
            for cx in (wd * 0.5, 2.0, wd - 3.0):
                dr.line([(cx, 0), (cx, ht)], fill=255, width=wlin)
        else:
            for cy in (ht * 0.5, 2.0, ht - 3.0):
                dr.line([(0, cy), (wd, cy)], fill=255, width=wlin)
        m = np.asarray(mask.filter(ImageFilter.GaussianBlur(0.6)), np.float32) / 255.0
        col = np.array(rgb, np.float32)
        hot = col + (255 - col) * 0.5
        out = np.zeros((ht, wd, 4), np.float32)
        out[..., :3] = col + (hot - col) * m[..., None]
        out[..., 3] = m * 255 * strength
        view = g2[y:y + ht, x:x + wd]
        view[...] = np.maximum(view, out)


_GLYPHS = {}


def glyph_strokes(seed):
    """The strokes of one rune in a unit box: a vertical stem and two to three side strokes (deterministic by seed)."""
    if seed in _GLYPHS:
        return _GLYPHS[seed]
    r = np.random.default_rng(seed * 977 + 13)
    segs = []
    if r.random() < 0.8:
        segs.append(((0.5, 0.0), (0.5, 1.0)))
    for _ in range(int(r.integers(2, 4))):
        y0 = float(r.choice([0.0, 0.2, 0.4, 0.6]))
        y1 = float(min(1.0, y0 + r.choice([0.3, 0.5])))
        s = float(r.choice([-1, 1]))
        segs.append(((0.5, y0), (0.5 + s * float(r.choice([0.3, 0.45])), y1)))
    if r.random() < 0.4:
        yy = float(r.choice([0.3, 0.55, 0.8]))
        segs.append(((0.1, yy), (0.9, yy)))
    _GLYPHS[seed] = segs
    return segs


def engrave_runes(atlas, g2, part, rgb, seed, on_core):
    """A band of runes burnt into every UV group of the part: a dark groove with a light rim on the skin, lit on the second emissive map (or the core glow)."""
    from PIL import Image, ImageDraw, ImageFilter
    for gi, g in enumerate(part.groups):
        x, y, wd, ht = g.rect
        if wd < 10 or ht < 10:
            continue
        cell = max(8.0, min(wd, ht) * 0.9)
        mask = Image.new("L", (wd, ht), 0)
        dr = ImageDraw.Draw(mask)
        long_is_h = ht >= wd
        n = max(1, int((ht if long_is_h else wd) / (cell * 1.25)))
        rng = np.random.default_rng(D.hash_seed(part.name, seed + gi))
        for i in range(n):
            gl = glyph_strokes(int(rng.integers(0, 4000)))
            off = i * cell * 1.25 + cell * 0.1
            for (a, b) in gl:
                if long_is_h:
                    p0, p1 = ((wd * 0.5 + (a[0] - 0.5) * cell * 0.7), off + a[1] * cell), ((wd * 0.5 + (b[0] - 0.5) * cell * 0.7), off + b[1] * cell)
                else:
                    p0, p1 = (off + a[1] * cell, ht * 0.5 + (a[0] - 0.5) * cell * 0.7), (off + b[1] * cell, ht * 0.5 + (b[0] - 0.5) * cell * 0.7)
                dr.line([p0, p1], fill=255, width=max(1, int(round(cell * 0.14))))
        m = np.asarray(mask.filter(ImageFilter.GaussianBlur(0.5)), np.float32) / 255.0
        if m.max() < 0.05:
            continue
        img = atlas.img[y:y + ht, x:x + wd]
        img[..., :3] *= (1.0 - 0.55 * m)[..., None]
        tgt = atlas.glow if on_core else g2
        view = tgt[y:y + ht, x:x + wd]
        col = np.array(rgb, np.float32)
        hot = col + (255 - col) * 0.5
        out = np.zeros((ht, wd, 4), np.float32)
        out[..., :3] = col + (hot - col) * m[..., None]
        out[..., 3] = m * 255 * 0.95
        view[...] = np.maximum(view, out)


# ================================================================ primitives
MAXLEN = 44.0                  # the longest stretch of one tube part: a loft's UV strip is as long as the tube, and a strip longer than the skin would force the whole skin to a lower texel density


def tube_pts(K, base, pts, r0, r1=None, sides=6, material="iron", color=None, glow=False, vein=False, tip=False, smooth=60, runes=False):
    """A swept tube along a polyline; a long one is cut into parts of at most MAXLEN (they meet end to end), each painted the same. Returns the last part."""
    pts = [np.asarray(q, float) for q in pts]
    r1 = r0 if r1 is None else r1
    seg = [float(np.linalg.norm(pts[i + 1] - pts[i])) for i in range(len(pts) - 1)]
    total = sum(seg) or 1.0
    chunks, cur, acc, start = [], [pts[0]], 0.0, 0.0
    for i, q in enumerate(pts[1:], 1):
        cur.append(q)
        acc += seg[i - 1]
        if acc >= MAXLEN and i < len(pts) - 1:
            chunks.append((cur, start, start + acc))
            start += acc
            cur, acc = [q], 0.0
    chunks.append((cur, start, total))
    p = None
    for ci, (cp, s0, s1) in enumerate(chunks):
        ra, rb = r0 + (r1 - r0) * s0 / total, r0 + (r1 - r0) * s1 / total
        p = nl.tube(K.m, K.name(base), material, [tuple(q) for q in cp], ra, rb, sides=sides, base=color, glow=glow, smooth=smooth, tip=tip and ci == len(chunks) - 1)
        if glow:
            q_glow(K, p)
        if vein:
            q_vein(K, p)
        if runes:
            q_runes(K, p, seed=len(K.m.parts))
    return p


def ring3(K, base, c, normal, r, tr, n=24, sides=5, material="iron", color=None, glow=False, vein=False, runes=False, a0=0.0, arc=1.0):
    """A ring (a halo, a hoop, a link) of radius r round the axis `normal` through c, swept tube of radius tr; `arc` < 1 leaves a gap (a broken ring)."""
    u, v = basis(normal)
    nn = max(6, int(n))
    pts = [np.asarray(c) + r * (math.cos(a0 + TAU * arc * i / nn) * u + math.sin(a0 + TAU * arc * i / nn) * v) for i in range(nn + 1)]
    return tube_pts(K, base, pts, tr, tr, sides=sides, material=material, color=color, glow=glow, vein=vein, runes=runes)


def cone(K, base, b, t, r, sides=5, material="obsidian", color=None, glow=False, vein=False, smooth=25):
    """A spike from base point b to tip t."""
    p = P(K.m, K.name(base), material, base=color, glow=glow, smooth=smooth, uv_mode="loft")
    nl.spike(p, b, t, r, sides)
    if glow:
        q_glow(K, p)
    if vein:
        q_vein(K, p)
    return p


def spikes(K, base, items, r, sides=5, material="obsidian", color=None, glow=False, vein=False):
    """Several spikes in ONE part; items = [(base point, tip point)] (fewer parts = fewer UV groups)."""
    p = P(K.m, K.name(base), material, base=color, glow=glow, smooth=25, uv_mode="loft")
    for (b, t) in items:
        nl.spike(p, b, t, r, sides)
    if glow:
        q_glow(K, p)
    if vein:
        q_vein(K, p)
    return p


def gem_at(K, base, c, r, long=1.5, axis="y", sides=6, color=None, glow=True):
    p = nl.crystal(K.m, K.name(base), "crystal", c, r, 2 * r * long, base=color or K.gem, glow=glow, sides=sides, axis=axis)
    if glow:
        q_glow(K, p, strength=1.0, core=0.55)
    return p


def orb_at(K, base, c, r, color=None, glow=True, sides=10, stacks=6):
    p = nl.orb(K.m, K.name(base), "crystal", c, r, base=color or K.gem, glow=glow, sides=sides, stacks=stacks)
    if glow:
        q_glow(K, p, strength=1.0, core=0.55)
    return p


def band(K, base, y, r, h, sides=14, material="gold", color=None, studs=0, glow=False, vein=False, runes=False, cx=None, cz=None):
    """A collar round the weapon at height y (with its centre following the weapon's axis), optionally with studs."""
    a = K.F.axis(y)
    cxx = a[0] if cx is None else cx
    czz = a[2] if cz is None else cz
    p = nl.lathe_y(K.m, K.name(base), material, y - h / 2, [(0.0, r * 0.94), (h * 0.2, r), (h * 0.8, r), (h, r * 0.94)], sides=sides, base=color, x=cxx, z=czz, glow=glow, smooth=45)
    if glow:
        q_glow(K, p)
    if vein:
        q_vein(K, p)
    if runes:
        q_runes(K, p, seed=len(K.m.parts))
    if studs:
        pts, nrm = [], []
        for i in range(studs):
            ang = TAU * i / studs
            pts.append((cxx + (r + 0.05) * math.cos(ang), y, czz + (r + 0.05) * math.sin(ang)))
            nrm.append((math.cos(ang), 0, math.sin(ang)))
        sp = P(K.m, K.name(base + "_studs"), material, base=color, smooth=40)
        for pt, nv in zip(pts, nrm):
            ml.stud(sp, pt, max(0.28, h * 0.32), max(0.3, h * 0.4), nv, 5, 0.5)
    return p


# ================================================================ the dark rig (every dark weapon)
def grip_bands(K, n=4):
    F = K.F
    n = K.S(n, 2)
    for i in range(n):
        y = K.grip_lo + (K.grip_hi - K.grip_lo) * (i + 0.5) / n
        r = F.radius(y) + 0.28
        band(K, "gripband", y, r, 0.9, sides=K.S(12, 6), material="gold" if i % 2 == 0 else "iron", color=K.trim if i % 2 == 0 else K.iron2, studs=K.S(6, 0) if i % 2 == 0 else 0)


def pommel_crown(K, n=7):
    F = K.F
    n = K.S(n, 4)
    a = F.axis(F.y0)
    r = max(0.9, F.radius(F.y0 + 1.0) + 0.5)
    items = []
    for i in range(n):
        ang = TAU * i / n
        d = np.array([math.cos(ang), 0, math.sin(ang)])
        items.append((a + d * r * 0.8 + np.array([0, 0.2, 0]), a + d * r * 1.9 + np.array([0, -1.9 - 0.7 * (i % 2), 0])))
    spikes(K, "pommel_thorn", items, r=0.34, sides=5, material="obsidian", color=mix((20, 18, 26), K.col, 0.2))
    nl.lathe_y(K.m, K.name("pommel_cap"), "gold", F.y0 - 0.6, [(0.0, 0.5), (0.5, r * 1.15), (1.1, r * 1.15), (1.6, r * 0.7)], sides=K.S(12, 6), base=K.trim, x=a[0], z=a[2], smooth=45)
    gem_at(K, "pommel_gem", a + np.array([0, -1.7, 0]), 0.62, 1.1)


def thorn_collar(K, y=None, n=8, length=3.0, rad=0.4, up=0.5):
    y = K.crest if y is None else y
    F = K.F
    n = K.S(n, 5)
    a = F.axis(y)
    r = F.radius(y) + 0.5
    items = []
    for i in range(n):
        ang = TAU * i / n + 0.2
        d = np.array([math.cos(ang), 0, math.sin(ang)])
        items.append((a + d * r, a + d * (r + length) + np.array([0, length * up, 0])))
    spikes(K, "thorn", items, r=rad, sides=5, material="obsidian", color=mix((22, 20, 28), K.col, 0.25))


def gem_ring(K, y=None, n=6, r=None, gr=0.55):
    y = K.crest if y is None else y
    a = K.F.axis(y)
    rr = (K.F.radius(y) + 0.9) if r is None else r
    n = K.S(n, 3)
    for i in range(n):
        ang = TAU * i / n + 0.4
        gem_at(K, "ringgem", a + np.array([rr * math.cos(ang), 0, rr * math.sin(ang)]), gr, 1.4)


def eye_gem(K, y=None, r=1.0):
    """An eye of the dark Djinn: a glowing gem in a slit-pupilled setting on the +z face of the head."""
    y = K.crest if y is None else y
    a = K.F.axis(y)
    zf = K.F.at(y)[3] + 0.4
    nl.lathe_y(K.m, K.name("eye_socket"), "gold", y - r * 0.3, [(0, r * 1.05), (r * 0.25, r * 1.3), (r * 0.6, r * 1.1)], sides=K.S(10, 6), base=K.trim, x=a[0], z=a[2] + zf, smooth=45)
    p = nl.orb(K.m, K.name("eye"), "crystal", (a[0], y + r * 0.1, a[2] + zf + 0.35), r * 0.85, base=mix(K.gem, (255, 255, 255), 0.2), glow=True, sides=K.S(10, 6), stacks=K.S(6, 4), rz=r * 0.55)
    q_glow(K, p, strength=1.0, core=0.6)


def chain(K, y_from=None, links=8, side=1.0, length=None):
    """A chain hanging from the pommel region: alternating links turned by 90 degrees."""
    F = K.F
    links = K.S(links, 4)
    y_from = F.y0 + 3.0 if y_from is None else y_from
    a = F.axis(y_from) + np.array([side * (F.radius(y_from) + 0.7), 0, 0])
    step = (length or 10.0) / links
    pts_pos = []
    for i in range(links):
        c = a + np.array([side * 0.8 * math.sin(i * 0.5) * (i / links), -step * i - 0.5, 0.35 * math.cos(i * 0.9)])
        normal = (0, 0, 1) if i % 2 == 0 else (1, 0, 0)
        ring3(K, "chain", c, normal, step * 0.58, 0.2, n=K.S(8, 6), sides=4, material="iron", color=K.iron2)
        pts_pos.append(c)
    tip = pts_pos[-1] + np.array([0, -step * 1.1, 0])
    gem_at(K, "chain_charm", tip, 0.6, 1.5)


def name_plate(K, y=None, text=None, length=None):
    """A small bronze plate on the +z face of the head or the guard: the Djinn's name engraved and lit."""
    y = K.crest if y is None else y
    a = K.F.axis(y)
    zf = K.F.at(y)[3] + 0.25
    ln = length or max(5.0, 1.15 * len(text or K.sp["text"]))
    t = text or K.sp["text"]
    p = P(K.m, K.name("nameplate"), "gold", base=K.trim, smooth=35)
    ml.box(p, (a[0] - 1.0, y - ln / 2, a[2] + zf - 0.2), (a[0] + 1.0, y + ln / 2, a[2] + zf + 0.35), chamfer=0.18)
    K.post.append(lambda atlas, g2, pp=p, tt=t: _plate_text(atlas, g2, pp, tt, K.vein))


def _plate_text(atlas, g2, part, text, rgb):
    """The letters of the name, one under the other, on the +z face of the plate (plan_uv's dominant group (2, +1): a = x right, b = y up)."""
    from PIL import Image, ImageDraw, ImageFilter
    import skin_glyphs
    for g in part.groups:
        if g.kind != "dom" or g.key[0] != 2 or g.key[1] < 0:
            continue
        x, y, wd, ht = g.rect
        if wd < 8 or ht < 24:
            continue
        mask = Image.new("L", (wd, ht), 0)
        dr = ImageDraw.Draw(mask)
        n = len(text)
        ch = ht / (n * 1.25 + 0.3)
        cw = min(wd * 0.8, ch * 0.7)
        for i, c in enumerate(text):
            top = ch * 0.25 + i * ch * 1.25
            for (a, b) in skin_glyphs.strokes(c):
                dr.line([(wd * 0.5 + (a[0] - 0.5) * cw, top + a[1] * ch), (wd * 0.5 + (b[0] - 0.5) * cw, top + b[1] * ch)], fill=255, width=max(1, int(round(ch * 0.11))))
        m = np.asarray(mask.filter(ImageFilter.GaussianBlur(0.5)), np.float32) / 255.0
        atlas.img[y:y + ht, x:x + wd, :3] *= (1.0 - 0.6 * m)[..., None]
        col = np.array(rgb, np.float32)
        out = np.zeros((ht, wd, 4), np.float32)
        out[..., :3] = col + (255 - col) * 0.5 * m[..., None]
        out[..., 3] = m * 255 * 0.95
        atlas.glow[y:y + ht, x:x + wd] = np.maximum(atlas.glow[y:y + ht, x:x + wd], out)


def halo_rings(K, n=2, r=None, y=None, tilt=0.55):
    """Rings of lit rune-script round the head: the energy line (second emissive map)."""
    y = (K.yh + K.F.y1) / 2 if y is None else y
    a = K.F.axis(y)
    r = r or (K.head_r + 3.2)
    for i in range(n):
        ang = TAU * i / max(1, n) + 0.3
        normal = np.array([math.sin(tilt) * math.cos(ang), math.cos(tilt) * 0.2 + 0.8, math.sin(tilt) * math.sin(ang)])
        ring3(K, "haloring", a + np.array([0, (i - (n - 1) / 2) * 3.0, 0]), normal, r * (1.0 - 0.08 * i), 0.32, n=K.S(28, 12), sides=K.S(5, 4), material="gold", color=K.trim, vein=True, runes=True)


# ================================================================ HORNS
def horns(K, style):
    F = K.F
    y = K.crest
    a = F.axis(y)
    hr = F.radius(y) + 0.6
    if style == "CROWN":
        n = K.S(9, 5)
        items = []
        for i in range(n):
            ang = TAU * i / n
            d = np.array([math.cos(ang), 0, math.sin(ang)])
            hgt = 3.2 + 1.8 * (i % 2)
            items.append((a + d * hr + np.array([0, 0.4, 0]), a + d * (hr + 0.5) + np.array([0, hgt, 0])))
        spikes(K, "crown", items, r=0.5, sides=5, material="gold", color=K.trim)
        gem_at(K, "crown_gem", a + np.array([0, 1.2, hr]), 0.55, 1.4)
    elif style == "CRESCENT":
        for sx in (-1, 1):
            pts = [a + np.array([sx * (hr + 1.5 * math.sin(t * 1.5) + t * 4.2), 0.8 + 6.5 * (t ** 1.6) + 2.0 * t, 0.0]) for t in np.linspace(0, 1, K.S(9, 5))]
            pts = [a + np.array([sx * (hr + 5.0 * math.sin(t * 1.9)), 1.0 + 6.0 * t ** 1.3 + 0.7 * math.sin(t * 3), 0]) for t in np.linspace(0, 1, K.S(10, 5))]
            tube_pts(K, "horn", pts, 0.75, 0.0, sides=K.S(6, 4), material="obsidian", color=mix((22, 20, 30), K.col, 0.3), tip=True)
    elif style == "STAG":
        for sx in (-1, 1):
            main = [a + np.array([sx * (hr + 0.7 * t * 5.2), 1.0 + t * 9.0, -0.3 * t]) for t in np.linspace(0, 1, K.S(8, 5))]
            tube_pts(K, "antler", main, 0.55, 0.12, sides=K.S(5, 4), material="bone", color=mix((120, 108, 96), K.col, 0.15), tip=True)
            for k, tt in enumerate((0.35, 0.6, 0.82)):
                b = a + np.array([sx * (hr + 0.7 * tt * 5.2), 1.0 + tt * 9.0, -0.3 * tt])
                tip = b + np.array([sx * (2.4 - 0.5 * k), 3.0 + 0.4 * k, 0.4])
                cone(K, "tine", b, tip, 0.34, sides=4, material="bone", color=mix((120, 108, 96), K.col, 0.15))
    elif style == "RAM":
        for sx in (-1, 1):
            pts = []
            for t in np.linspace(0, 1, K.S(12, 6)):
                ang = TAU * 0.82 * t
                rr = 4.4 - 2.4 * t
                pts.append(a + np.array([sx * (hr + 1.2 + rr * math.sin(ang)), 2.0 - rr * (1 - math.cos(ang)) * 0.8 + 2.5 * t, 0.9 * math.sin(ang * 0.5)]))
            tube_pts(K, "ramhorn", pts, 0.95, 0.15, sides=K.S(6, 4), material="bone", color=mix((92, 78, 70), K.col, 0.2), tip=True)
    elif style == "SPIKE":
        items = []
        for sx, hgt in ((-1, 7.5), (1, 7.5), (0, 10.5)):
            items.append((a + np.array([sx * (hr + 0.2), 0.8, 0]), a + np.array([sx * (hr + 1.4), hgt, 0])))
        spikes(K, "bigspike", items, r=0.8, sides=5, material="obsidian", color=mix((20, 18, 28), K.col, 0.3))


# ================================================================ WINGS
def wings(K, style):
    F = K.F
    y = K.yh + 0.25 * (F.y1 - K.yh)
    a = F.axis(y)
    hr = F.radius(y) + 0.5
    if style == "BLADE":
        for sx in (-1, 1):
            for k in range(K.S(3, 2)):
                L = 9.0 - 1.6 * k
                pts = [a + np.array([sx * (hr + 0.6 + t * L * 0.9), -0.5 + t * (4.0 + 2.4 * k) - 4.0 * t * t, -0.6 - 0.7 * k]) for t in np.linspace(0, 1, K.S(7, 4))]
                p = P(K.m, K.name("wingblade"), "steel", base=mix((58, 56, 70), K.col, 0.35), smooth=40)
                ml.sweep(p, [tuple(q) for q in pts], ml.bevelled(2.0, 0.5, 0.5), scales=lambda t: ((1.5 * (1 - 0.85 * t)) + 0.05, 0.28 * (1 - 0.6 * t)) if t < 0.97 else (0.0, 0.0), up=(0, 0, 1))
                q_vein_lines(K, p) if k == 0 else None
    elif style == "FEATHER":
        for sx in (-1, 1):
            for k in range(K.S(4, 3)):
                L = 10.0 - 1.4 * k
                ang = 0.25 + 0.2 * k
                pts = [a + np.array([sx * (hr + 0.5 + t * L * math.cos(ang)), 1.5 - 3.2 * k * 0.5 + t * L * math.sin(ang) * (1 - 0.3 * t), -0.5 - 0.6 * k]) for t in np.linspace(0, 1, K.S(7, 4))]
                p = P(K.m, K.name("feather"), "bone", base=mix((46, 42, 54), K.col, 0.3), smooth=40)
                ml.sweep(p, [tuple(q) for q in pts], ml.lozenge(1.0, 0.3), scales=lambda t: (1.9 * math.sin(math.pi * min(0.97, max(0.05, t)) * 0.85) + 0.1, 0.5), up=(0, 0, 1))
    elif style == "FIRE":
        for sx in (-1, 1):
            for k in range(K.S(4, 3)):
                pts = [a + np.array([sx * (hr + 0.5 + 1.6 * t * (1 + 0.6 * k) + 0.8 * math.sin(t * 5 + k)), 0.5 + 9.0 * t ** 1.2 + 1.0 * k, -0.4 * k]) for t in np.linspace(0, 1, K.S(8, 5))]
                tube_pts(K, "firewing", pts, 0.65, 0.0, sides=K.S(5, 4), material="crystal", color=mix(K.col, (255, 120, 40), 0.35), glow=True, tip=True)
    elif style == "VEIL":
        for sx in (-1, 1):
            for k in range(K.S(2, 2)):
                pts = [a + np.array([sx * (hr + 0.6 + t * 5.5 + 1.8 * math.sin(t * 6 + k * 2)), -1.0 - t * 15.0, 0.9 * math.sin(t * 4 + k) + 0.5 * k]) for t in np.linspace(0, 1, K.S(12, 6))]
                p = P(K.m, K.name("veil"), "cloth", base=K.cloth, smooth=50)
                ml.sweep(p, [tuple(q) for q in pts], ml.rect_ring(1.0, 0.16), scales=lambda t: (1.7 * (1 - 0.8 * t) + 0.2, 1.0), up=(0, 0, 1))
    elif style == "MEMBRANE":
        for sx in (-1, 1):
            ribs = []
            for k in range(K.S(3, 2)):
                tip = a + np.array([sx * (hr + 8.5 - 1.6 * k), 5.5 - 3.6 * k, -0.7])
                pts = [a + (tip - a) * t + np.array([0, 2.5 * math.sin(math.pi * t), 0]) for t in np.linspace(0, 1, K.S(6, 4))]
                tube_pts(K, "wingrib", pts, 0.34, 0.1, sides=4, material="obsidian", color=mix((20, 18, 28), K.col, 0.3), tip=True)
                ribs.append(tip)
            for i in range(len(ribs) - 1):                                         # the membrane: a thin slab between two ribs
                p = P(K.m, K.name("membrane"), "cloth", base=mix(K.cloth, K.col, 0.2), smooth=60)
                t0, t1 = ribs[i], ribs[i + 1]
                ml.loft(p, "z", (0, 0, -0.9), [(0.0, 1.0, 1.0), (0.18, 1.0, 1.0)], [tuple(a[:2] + np.array([0.0, 0.0])), tuple(t0[:2]), tuple(t1[:2])], caps=(True, True))


# ================================================================ TAIL
def tail(K, style):
    F = K.F
    a = F.axis(F.y0 + 1.0)
    s = 1.0
    if style == "WHIP":
        pts = [a + np.array([s * (2.5 * math.sin(t * 5.5) * (0.3 + t)), -t * 16.0, 0.9 * math.cos(t * 4.0)]) for t in np.linspace(0, 1, K.S(14, 7))]
        tube_pts(K, "whip", pts, 0.5, 0.0, sides=K.S(6, 4), material="obsidian", color=mix((20, 18, 28), K.col, 0.3), tip=True)
        gem_at(K, "whip_tip", pts[-3], 0.4, 1.4)
    elif style == "FLAME":
        for k in range(K.S(3, 2)):
            pts = [a + np.array([s * (1.2 * k - 1.2) + 1.4 * math.sin(t * 6 + k), -t * (11.0 - 2.0 * k), 0.6 * math.cos(t * 5 + k)]) for t in np.linspace(0, 1, K.S(8, 5))]
            tube_pts(K, "tailflame", pts, 0.7 - 0.12 * k, 0.0, sides=K.S(5, 4), material="crystal", color=mix(K.col, (255, 120, 40), 0.3), glow=True, tip=True)
    elif style == "TENDRIL":
        for k in range(K.S(3, 2)):
            ph = k * 2.1
            pts = [a + np.array([2.0 * math.sin(t * 7 + ph) * (0.4 + t), -t * 13.0, 1.4 * math.cos(t * 6 + ph)]) for t in np.linspace(0, 1, K.S(12, 6))]
            tube_pts(K, "tendril", pts, 0.42, 0.08, sides=K.S(5, 4), material="cloth", color=mix(K.cloth, K.col, 0.3), tip=True)
    elif style == "BLADE":
        pts = [a + np.array([0, -t * 12.0, 0]) for t in np.linspace(0, 1, 5)]
        p = P(K.m, K.name("tailblade"), "steel", base=mix((58, 56, 70), K.col, 0.35), smooth=40)
        ml.sweep(p, [tuple(q) for q in pts], ml.bevelled(2.0, 0.6, 0.5), scales=lambda t: ((1.3 * (1 - t) + 0.08), 0.3 * (1 - 0.7 * t)) if t < 0.97 else (0.0, 0.0), up=(0, 0, 1))
        q_vein_lines(K, p)


# ================================================================ HALO
def halo(K, style):
    F = K.F
    y = (K.yh + F.y1) / 2
    a = F.axis(y)
    r = K.head_r + 4.5
    c = a + np.array([0, 0, -1.2])
    if style == "RING":
        ring3(K, "halo", c, (0, 0, 1), r, 0.5, n=K.S(36, 16), sides=K.S(6, 4), material="gold", color=K.trim)
        ring3(K, "halo_in", c, (0, 0, 1), r - 1.2, 0.2, n=K.S(36, 16), sides=4, material="iron", color=K.iron2, vein=True)
    elif style == "SPOKES":
        ring3(K, "halo", c, (0, 0, 1), r, 0.45, n=K.S(32, 14), sides=K.S(5, 4), material="gold", color=K.trim)
        items = []
        n = K.S(12, 6)
        for i in range(n):
            ang = TAU * i / n
            d = np.array([math.cos(ang), math.sin(ang), 0])
            items.append((c + d * r * 0.98, c + d * (r + 2.6 + 1.2 * (i % 2))))
        spikes(K, "halospoke", items, r=0.36, sides=4, material="gold", color=K.trim)
    elif style == "RUNES":
        ring3(K, "halo", c, (0, 0, 1), r, 0.62, n=K.S(34, 16), sides=K.S(6, 4), material="gold", color=mix(K.trim, K.col, 0.2), runes=True)
        ring3(K, "halo_in", c, (0, 0, 1), r - 1.6, 0.22, n=K.S(34, 16), sides=4, material="iron", color=K.iron2, vein=True)


# ================================================================ AFFINITY packs
def aff_lightning(K):
    F = K.F
    # a counter-wound coil of lit lightning round the shaft and forked bolts leaping off the head
    y0, y1 = K.grip_hi + 2.0, K.yh - 1.0
    if y1 - y0 > 6:
        pts = []
        n = K.S(40, 16)
        for i in range(n + 1):
            t = i / n
            y = y0 + (y1 - y0) * t
            a = F.axis(y)
            r = F.radius(y) + 0.55
            ang = TAU * 3.0 * t
            pts.append((a[0] + r * math.cos(ang) + (0.5 if i % 2 else -0.5) * 0.4, y, a[2] + r * math.sin(ang)))
        tube_pts(K, "coil", pts, 0.2, 0.2, sides=4, material="crystal", color=K.gem, glow=True, vein=True)
    a = F.axis(K.crest)
    for k in range(K.S(5, 3)):
        ang = TAU * k / max(1, K.S(5, 3)) + 0.6
        d = np.array([math.cos(ang), 0.0, math.sin(ang)])
        pts = [a + d * (K.head_r + 0.6) + np.array([0, 0.5, 0])]
        for j in range(1, 6):
            pts.append(pts[-1] + d * (1.5 + 0.3 * j) * 0.6 + np.array([0.7 * (1 if j % 2 else -1) * 0.0, 1.8 + 0.4 * j, 0.0]) + np.array([0, 0, 0.8 * (1 if j % 2 else -1)]))
        tube_pts(K, "bolt", pts, 0.28, 0.05, sides=4, material="crystal", color=K.gem, glow=True, tip=True)


def aff_heat(K):
    F = K.F
    a = F.axis(K.crest)
    n = K.S(7, 4)
    for k in range(n):
        ang = TAU * k / n + 0.35
        d = np.array([math.cos(ang), 0.0, math.sin(ang)])
        b = a + d * (K.head_r + 0.4)
        pts = [b + np.array([0, 0.2, 0]) + d * (1.7 * t * (1 + 0.4 * math.sin(k))) + np.array([0, 7.0 * t ** 1.1 + 1.0 * (k % 3), 0]) + np.array([0.9 * math.sin(t * 5 + k) * 0.5, 0, 0]) for t in np.linspace(0, 1, K.S(7, 4))]
        tube_pts(K, "ember", pts, 0.6, 0.0, sides=K.S(5, 4), material="crystal", color=mix(K.col, (255, 110, 30), 0.35), glow=True, tip=True)
    for k in range(K.S(4, 2)):
        y = K.grip_hi + 3.0 + k * 3.2
        if y < K.yh - 1:
            band(K, "emberband", y, F.radius(y) + 0.45, 0.7, sides=K.S(12, 6), material="gold", color=K.trim, vein=True)


def aff_water(K):
    F = K.F
    y0, y1 = K.grip_hi + 1.0, min(K.yh + 0.1 * K.F.length, F.y1 - 3.0)
    for k in range(2):
        pts = []
        n = K.S(28, 12)
        for i in range(n + 1):
            t = i / n
            y = y0 + (y1 - y0) * t
            a = F.axis(y)
            r = F.radius(y) + 0.8 + 0.7 * math.sin(t * TAU * 2)
            ang = TAU * 1.5 * t + k * math.pi
            pts.append((a[0] + r * math.cos(ang), y, a[2] + r * math.sin(ang)))
        p = P(K.m, K.name("tide"), "cloth", base=mix(K.cloth, K.col, 0.45), smooth=50)
        ml.sweep(p, pts, ml.rect_ring(1.0, 0.18), scales=lambda t: (1.0 * (1 - 0.6 * t) + 0.25, 1.0), up=(0, 1, 0))
        q_vein_lines(K, p)
    a = F.axis(K.crest)
    for k in range(K.S(6, 3)):
        ang = TAU * k / max(1, K.S(6, 3))
        orb_at(K, "pearl", a + np.array([(K.head_r + 2.2) * math.cos(ang), 1.2 + 1.5 * math.sin(k * 1.7), (K.head_r + 2.2) * math.sin(ang)]), 0.55, color=mix(K.col, (230, 250, 255), 0.45))


def aff_wind(K):
    F = K.F
    for k in range(K.S(3, 2)):
        y = K.grip_hi + 2.0 + k * ((K.yh - K.grip_hi - 3.0) / 3.0)
        a = F.axis(y)
        ring3(K, "windring", a, (0, 1, 0), F.radius(y) + 1.6 + 0.5 * k, 0.16, n=K.S(22, 10), sides=4, material="silver", color=mix((150, 160, 172), K.col, 0.35), vein=True)
    a = F.axis(K.crest)
    for sx in (-1, 1):
        pts = [a + np.array([sx * (K.head_r + 0.8 + 6.5 * t), 2.0 + 3.5 * math.sin(t * 4.0), 0.0]) for t in np.linspace(0, 1, K.S(10, 5))]
        pts = [a + np.array([sx * (K.head_r + 0.6 + 7.0 * t), 3.0 * math.sin(t * 3.5), 1.0 * math.cos(t * 5)]) for t in np.linspace(0, 1, K.S(10, 5))]
        tube_pts(K, "gust", pts, 0.38, 0.0, sides=4, material="silver", color=mix((170, 180, 190), K.col, 0.3), vein=True, tip=True)


def aff_life(K):
    F = K.F
    y0, y1 = K.grip_hi - 4.0, min(K.yh + 4.0, F.y1 - 2.0)
    pts = []
    n = K.S(44, 18)
    for i in range(n + 1):
        t = i / n
        y = y0 + (y1 - y0) * t
        a = F.axis(y)
        r = F.radius(y) + 0.45
        ang = TAU * 3.5 * t
        pts.append((a[0] + r * math.cos(ang), y, a[2] + r * math.sin(ang)))
    tube_pts(K, "vine", pts, 0.26, 0.2, sides=K.S(5, 4), material="leather", color=mix((38, 52, 30), K.col, 0.3))
    items = []
    for i in range(3, n, 4):
        p0 = np.array(pts[i])
        a = F.axis(p0[1])
        d = unit(np.array([p0[0] - a[0], 0, p0[2] - a[2]]))
        items.append((p0, p0 + d * 2.6 + np.array([0, 1.8, 0])))
    spikes(K, "leaf", items, r=0.55, sides=4, material="leather", color=mix((46, 74, 36), K.col, 0.3))
    a = F.axis(K.crest)
    for k in range(K.S(3, 2)):
        ang = TAU * k / 3 + 1.0
        orb_at(K, "bud", a + np.array([(K.head_r + 1.4) * math.cos(ang), 1.0 + k, (K.head_r + 1.4) * math.sin(ang)]), 0.65, color=mix(K.col, (255, 190, 220), 0.35))


def aff_strength(K):
    F = K.F
    for k in range(K.S(3, 2)):
        y = K.grip_hi + 2.0 + k * 3.4
        band(K, "heavyband", y, F.radius(y) + 0.7, 1.6, sides=K.S(14, 6), material="iron", color=K.iron2, studs=K.S(8, 4), runes=True)
    a = F.axis(K.crest)
    for sx in (-1, 1):
        pts = [a + np.array([sx * (K.head_r + 0.5 + 3.2 * t), 0.5 + 6.0 * t ** 1.5, 0]) for t in np.linspace(0, 1, K.S(7, 4))]
        tube_pts(K, "spur", pts, 0.8, 0.0, sides=K.S(5, 4), material="bone", color=mix((100, 90, 84), K.col, 0.2), tip=True)


def aff_sound(K):
    F = K.F
    n = K.S(5, 3)
    for k in range(n):
        y = K.grip_hi + 2.5 + k * ((K.yh - K.grip_hi - 3.0) / max(1, n))
        a = F.axis(y)
        ring3(K, "resonance", a, (0, 1, 0), F.radius(y) + 1.1 + 0.7 * k, 0.2 + 0.03 * k, n=K.S(24, 10), sides=4, material="gold", color=mix(K.trim, K.col, 0.25), vein=True)
    a = F.axis(K.crest)
    for sx in (-1, 1):
        nl.lathe_y(K.m, K.name("bell"), "gold", K.crest - 1.0, [(0.0, 0.3), (0.5, 1.4), (1.6, 2.1), (2.2, 1.7)], sides=K.S(10, 6), base=K.trim, x=a[0] + sx * (K.head_r + 2.2), z=a[2], smooth=40)
        orb_at(K, "clapper", (a[0] + sx * (K.head_r + 2.2), K.crest - 1.1, a[2]), 0.45)


def aff_light(K):
    F = K.F
    a = F.axis(K.crest)
    n = K.S(10, 6)
    items = []
    for i in range(n):
        ang = TAU * i / n
        d = np.array([math.cos(ang), math.sin(ang) * 0.4, math.sin(ang)])
        items.append((a + d * (K.head_r + 1.0), a + d * (K.head_r + 4.0 + 2.0 * (i % 2)) + np.array([0, 1.5, 0])))
    spikes(K, "ray", items, r=0.38, sides=4, material="gold", color=K.trim, glow=True)
    for k in range(K.S(3, 2)):
        y = K.grip_hi + 3.0 + k * 4.5
        if y < K.yh:
            gem_at(K, "starstud", F.axis(y) + np.array([0, 0, F.at(y)[3] + 0.5]), 0.5, 1.6, axis="y")


AFFINITY = {"Lightning": aff_lightning, "Heat": aff_heat, "Water": aff_water, "Wind": aff_wind, "Life": aff_life, "Strength": aff_strength, "Sound": aff_sound, "Light": aff_light}


# ================================================================ the orbiting piece (separate mesh, drawn N times by the renderer)
def orbit_piece(K, kind):
    """One orbiting piece, centred on the origin, about 5 units across; parts are named orbit_*."""
    m = K.m
    col = K.gem
    if kind == "shard":
        gem_part = nl.crystal(m, "orbit_shard", "crystal", (0, 0, 0), 0.9, 4.2, base=col, glow=True, sides=5)
        q_glow(K, gem_part, strength=1.0, core=0.6)
    elif kind == "feather":
        p = P(m, "orbit_feather", "bone", base=mix((46, 42, 54), K.col, 0.35), smooth=40)
        ml.sweep(p, [(0, -2.6, 0), (0.4, -0.8, 0), (0.5, 0.9, 0), (0, 2.8, 0)], ml.lozenge(1.0, 0.3), scales=lambda t: (1.5 * math.sin(math.pi * min(0.97, max(0.05, t))) + 0.1, 0.4), up=(0, 0, 1))
        q_vein_lines(K, p)
    elif kind == "flame":
        p = P(m, "orbit_flame", "crystal", base=mix(K.col, (255, 120, 40), 0.35), glow=True, smooth=40, uv_mode="loft")
        nl.spike(p, (0, -1.6, 0), (0, 2.8, 0), 0.95, 5)
        q_glow(K, p)
    elif kind == "bubble":
        p = nl.orb(m, "orbit_bubble", "crystal", (0, 0, 0), 1.3, base=mix(K.col, (230, 250, 255), 0.4), glow=True, sides=8, stacks=5)
        q_glow(K, p, strength=0.85, core=0.5)
    elif kind == "bolt":
        pts = [(0, 2.8, 0), (0.8, 1.2, 0.3), (-0.5, 0.2, -0.3), (0.7, -1.2, 0.2), (0, -2.8, 0)]
        p = nl.tube(m, "orbit_bolt", "crystal", pts, 0.34, 0.1, sides=4, base=col, glow=True, smooth=30)
        q_glow(K, p)
    elif kind == "rune":
        p = P(m, "orbit_rune", "gold", base=K.trim, smooth=30)
        ml.box(p, (-1.5, -1.5, -0.2), (1.5, 1.5, 0.35), chamfer=0.35)
        K.post.append(lambda atlas, g2, pp=p: engrave_runes(atlas, g2, pp, K.vein, 3, True))
    elif kind == "leaf":
        p = P(m, "orbit_leaf", "leather", base=mix((46, 74, 36), K.col, 0.3), smooth=40)
        ml.sweep(p, [(0, -2.4, 0), (0.5, -0.6, 0), (0.4, 1.2, 0), (0, 2.6, 0)], ml.lozenge(1.0, 0.3), scales=lambda t: (1.2 * math.sin(math.pi * min(0.97, max(0.05, t))) + 0.1, 0.35), up=(0, 0, 1))
    elif kind == "note":
        p = nl.orb(m, "orbit_note", "gold", (0, -1.2, 0), 0.9, base=K.trim, glow=False, sides=8, stacks=5, rx=1.1, ry=0.8)
        q = P(m, "orbit_note_stem", "gold", base=K.trim, smooth=40)
        ml.box(q, (0.7, -1.2, -0.15), (1.1, 2.6, 0.15))
        g = nl.orb(m, "orbit_note_glow", "crystal", (1.2, 2.6, 0), 0.55, base=col, glow=True, sides=6, stacks=4)
        q_glow(K, g)
    elif kind == "star":
        p = nl.crystal(m, "orbit_star", "crystal", (0, 0, 0), 0.8, 4.6, base=col, glow=True, sides=4)
        q2 = nl.crystal(m, "orbit_star2", "crystal", (0, 0, 0), 0.8, 4.6, base=col, glow=True, sides=4, axis="x")
        q_glow(K, p, strength=1.0, core=0.65)
        q_glow(K, q2, strength=1.0, core=0.65)
    elif kind == "claw":
        p = P(m, "orbit_claw", "bone", base=mix((100, 90, 84), K.col, 0.2), smooth=30, uv_mode="loft")
        pts = [(0.0, -2.4, 0.0), (0.9, -0.8, 0), (1.1, 1.0, 0), (0.2, 2.6, 0)]
        p = nl.tube(m, "orbit_claw", "bone", pts, 0.7, 0.0, sides=5, base=mix((100, 90, 84), K.col, 0.2), tip=True)
    else:                                                                              # "skull-gem": a dark orb with a lit eye
        p = nl.orb(m, "orbit_orb", "obsidian", (0, 0, 0), 1.2, base=mix((20, 18, 28), K.col, 0.3), glow=False, sides=8, stacks=5)
        g = nl.orb(m, "orbit_orb_eye", "crystal", (0, 0, 1.05), 0.55, base=col, glow=True, sides=6, stacks=4, rz=0.3)
        q_glow(K, g)

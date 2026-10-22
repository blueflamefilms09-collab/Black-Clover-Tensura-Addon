"""The hand-painted sheets of the Salamander (Fuegoleon Vermillion's Spirit Lord, docs/magic/fire_casting.md), redone to match the four reference stills of the Black Clover wiki (English wiki first,
https://blackclover.fandom.com/wiki/Salamander): the SMALL form is the RED scaled lizard-dragon of the shoulder (angular overlapping plates with dark outlines, dark maroon shadows, a lighter belly,
orange bat wings with dark red finger bones and veins, big yellow slit eyes, pale hooked claws); the BIG form is the GOLDEN-ORANGE dragon (smooth skin with thin crease lines, red ring spots and
leopard rosettes, sculpted chest, ribs and abs, red claws, veined golden wings, a spiny frill and white fangs).

The look is the anime cel look: flat colours, one shadow tone, one light tone and a dark outline painted INTO the texture (every shape is drawn at 3x and shrunk). Deterministic (every painter is seeded).
Used by tools/gen_salamander_geo.py. Painters return (colour RGBA, glow RGBA) float arrays of their size, or only the colour array where noted; a texel with alpha 0 is not drawn (the cards cut their shape).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from firecast_geo_lib import rgb, smoothstep, fbm  # noqa: E402

SS = 3                                                    # supersampling of the drawn shapes

SMALL = dict(base="#D8482E", dark="#8A2418", deep="#4E140F", light="#F2704C", outline="#2A0C0A", line="#3A100C", ring="#F6B04C", ring_in="#E5582F",
             belly="#EE8C46", belly_dark="#B8502A", belly_line="#6E2412", membrane="#F4A24C", membrane2="#E27C3A", membrane_hi="#FAC070", vein="#B0381C", bone="#A02818",
             bone_light="#D8583A", eye="#FFE95A", eye_dark="#1C0707", tooth="#F8EEDA", claw="#F0D8A8", claw_dark="#7A3A22", spike="#E4502C", spike_tip="#F6B060", mouth="#6A0E10",
             glow="#FFD45A", spots=0.0, cell=8, seam=0.55, big=False)
BIG = dict(base="#F6A83F", dark="#E0802C", deep="#B05A22", light="#FFC966", outline="#6A3012", line="#8E4418", ring="#B8472A", ring_in="#EEA85A", belly="#F6D088", belly_dark="#DDA050",
           belly_line="#B26A2E", membrane="#F3BE68", membrane2="#EAA04C", membrane_hi="#FAD890", vein="#C0502A", bone="#C2622C", bone_light="#EE9A52", eye="#F8E858", eye_dark="#1E0A04",
           tooth="#FBF2DE", claw="#A82E1E", claw_dark="#5A140C", spike="#F29A44", spike_tip="#FBD98A", mouth="#7A1A12", glow="#FFE070", spots=0.30, cell=11, seam=0.24, big=True)


def col(h, a=255):
    c = rgb(h)
    return int(c[0]), int(c[1]), int(c[2]), a


def canvas(w, h, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (w * SS, h * SS), fill)


def done(img, w, h):
    return np.asarray(img.resize((w, h), Image.LANCZOS), dtype=np.float32)


def over(base, layer):
    """Alpha-composites an RGBA float layer over an RGBA float base (straight alpha), returns RGBA float."""
    a = layer[..., 3:4] / 255.0
    ba = base[..., 3:4] / 255.0
    oa = a + ba * (1 - a)
    rgbo = np.where(oa > 1e-5, (layer[..., :3] * a + base[..., :3] * ba * (1 - a)) / np.maximum(oa, 1e-5), 0.0)
    return np.concatenate([rgbo, oa * 255.0], axis=-1)


def solid(w, h, c):
    out = np.zeros((h, w, 4), dtype=np.float32)
    out[..., :3] = rgb(c)
    out[..., 3] = 255
    return out


def clear(w, h):
    return np.zeros((h, w, 4), dtype=np.float32)


def S(pts):
    return [(x * SS, y * SS) for x, y in pts]


def poly(dr, pts, fill, outline=None, width=1.2):
    dr.polygon(S(pts), fill=fill)
    if outline is not None:
        dr.line(S(list(pts) + [pts[0]]), fill=outline, width=max(1, int(width * SS)), joint="curve")


def line(dr, pts, fill, width=1.2):
    dr.line(S(pts), fill=fill, width=max(1, int(width * SS)), joint="curve")


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def flipx(c, g):
    return c[:, ::-1].copy(), g[:, ::-1].copy()


def bez(p0, p1, p2, n=14):
    return [((1 - t) ** 2 * p0[0] + 2 * (1 - t) * t * p1[0] + t * t * p2[0], (1 - t) ** 2 * p0[1] + 2 * (1 - t) * t * p1[1] + t * t * p2[1]) for t in np.linspace(0, 1, n)]


# ------------------------------------------------------------------------------------------------------------------ skins
def plates(w, h, pal, seed, cell=None, squash=0.78):
    """The small form's armour: rows of overlapping shield-shaped scales (pointed downward), each flat red with a lighter upper edge, a darker lower edge and a dark outline; the lower rows are
    drawn first so every scale overlaps the one under it, like the chevrons of the reference."""
    rng = np.random.default_rng(seed)
    cw = float(cell or pal["cell"])
    ch = cw * squash
    img = canvas(w, h, col(pal["deep"]))
    dr = ImageDraw.Draw(img)
    base, dark, light, outline = rgb(pal["base"]), rgb(pal["dark"]), rgb(pal["light"]), col(pal["line"])
    rows = int(h / ch) + 3
    cols = int(w / cw) + 3
    for r in range(rows, -2, -1):
        y = r * ch
        off = (r % 2) * cw * 0.5
        for c in range(-1, cols):
            x = c * cw + off
            t = rng.random()
            shade = (t - 0.5) * 0.35
            body = mix(base, light if shade > 0 else dark, abs(shade) * 0.8)
            tip = [(x, y), (x + cw, y), (x + cw * 1.02, y + ch * 0.62), (x + cw * 0.5, y + ch * 1.42), (x - cw * 0.02, y + ch * 0.62)]
            poly(dr, tip, body, outline, 1.15)
            hi = [(x + cw * 0.10, y + ch * 0.10), (x + cw * 0.90, y + ch * 0.10), (x + cw * 0.84, y + ch * 0.34), (x + cw * 0.50, y + ch * 0.46), (x + cw * 0.16, y + ch * 0.34)]
            dr.polygon(S(hi), fill=mix(body, light, 0.40))
            sh = [(x + cw * 0.10, y + ch * 0.70), (x + cw * 0.50, y + ch * 1.06), (x + cw * 0.90, y + ch * 0.70), (x + cw * 0.50, y + ch * 0.92)]
            dr.polygon(S(sh), fill=mix(body, dark, 0.45))
    out = done(img, w, h)
    out[..., 3] = 255
    return out


def smooth_skin(w, h, pal, seed, spots=True):
    """The big form's skin: smooth golden orange with flat darker patches (cel shadows), thin crease lines and ring spots (a red ring round an orange centre, outlined), a few leopard rosettes."""
    rng = np.random.default_rng(seed)
    base, dark, light = rgb(pal["base"]), rgb(pal["dark"]), rgb(pal["light"])
    n = fbm(rng, w, h, 4, 3)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    c = np.broadcast_to(base, (h, w, 3)).copy()
    c = np.where((n > 0.60)[..., None], base * 0.45 + dark * 0.55, c)
    c = np.where((n < 0.38)[..., None], base * 0.6 + light * 0.4, c)
    out = np.concatenate([c, np.full((h, w, 1), 255.0, np.float32)], axis=-1)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    for _ in range(max(6, (w * h) // 900)):                                                    # crease lines: short bowed strokes
        x, y = rng.random() * w, rng.random() * h
        a = rng.random() * math.tau
        ln = 5 + rng.random() * 12
        p0 = (x, y)
        p2 = (x + math.cos(a) * ln, y + math.sin(a) * ln)
        p1 = ((p0[0] + p2[0]) / 2 + math.sin(a) * ln * 0.25, (p0[1] + p2[1]) / 2 - math.cos(a) * ln * 0.25)
        line(dr, bez(p0, p1, p2, 8), col(pal["line"], 150), 0.8)
    out = over(out, done(lay, w, h))
    if spots and pal["spots"] > 0:
        out = ring_spots(out, w, h, pal, rng)
    return out


def ring_spots(base_img, w, h, pal, rng):
    """Ring spots: a dark red ring round an orange centre with a thin outline, scattered on a jittered grid; some are open rosettes (a broken ring with a dot), like the leopard spots of the giant spirit."""
    big = canvas(w, h)
    dr = ImageDraw.Draw(big)
    gap = 34
    ringc, inner, line_c = col(pal["ring"], 255), col(pal["ring_in"], 255), col(pal["line"], 255)
    for gy in range(-1, h // gap + 2):
        for gx in range(-1, w // gap + 2):
            if rng.random() > pal["spots"]:
                continue
            px = (gx + 0.5 + (rng.random() - 0.5) * 0.7) * gap + (gy % 2) * gap * 0.5
            py = (gy + 0.5 + (rng.random() - 0.5) * 0.7) * gap
            r = gap * (0.21 + 0.09 * rng.random())
            rosette = rng.random() < 0.35
            for ox in (-w, 0, w):                                                # wraps over the left and right edge so the window of a face never shows a cut ring
                x0, y0 = (px + ox) * SS, py * SS
                dr.ellipse([x0 - (r + 0.9) * SS, y0 - (r * 0.86 + 0.9) * SS, x0 + (r + 0.9) * SS, y0 + (r * 0.86 + 0.9) * SS], fill=line_c)
                dr.ellipse([x0 - r * SS, y0 - r * 0.86 * SS, x0 + r * SS, y0 + r * 0.86 * SS], fill=ringc)
                dr.ellipse([x0 - r * 0.62 * SS, y0 - r * 0.52 * SS, x0 + r * 0.62 * SS, y0 + r * 0.52 * SS], fill=inner)
                if rosette:
                    dr.ellipse([x0 - r * 0.22 * SS, y0 - r * 0.2 * SS, x0 + r * 0.22 * SS, y0 + r * 0.2 * SS], fill=ringc)
    return over(base_img, done(big, w, h))


def skin(w, h, pal, seed, cell=None, spots=True):
    return plates(w, h, pal, seed, cell) if not pal["big"] else smooth_skin(w, h, pal, seed, spots=spots)


def belly(w, h, pal, seed):
    """Belly plates: wide bands across, each lit at its top edge and dark under it, outlined; a faint line along the middle."""
    rng = np.random.default_rng(seed)
    b, dk, ln = rgb(pal["belly"]), rgb(pal["belly_dark"]), rgb(pal["belly_line"])
    band = max(5.0, h / 9.0)
    img = canvas(w, h, col(pal["belly_dark"]))
    dr = ImageDraw.Draw(img)
    rows = int(h / band) + 2
    for r in range(rows):
        y = r * band
        t = rng.random()
        poly(dr, [(0, y + band * 0.08), (w, y + band * 0.08), (w, y + band * 0.92), (0, y + band * 0.92)], mix(b, rgb(pal["light"]), 0.12 * t), col(pal["belly_line"]), 1.1)
        dr.rectangle([0, int((y + band * 0.62) * SS), w * SS, int((y + band * 0.90) * SS)], fill=mix(b, dk, 0.35))
    line(dr, [(w / 2, 0), (w / 2, h)], col(pal["belly_line"], 150), 0.8)
    out = done(img, w, h)
    out[..., 3] = 255
    return out


def tail_skin(w, h, pal, seed):
    """The tail's big plates: each segment is a wide shield with a dark gap, lit on top (the tail of the small form is a column of large overlapping plates)."""
    return plates(w, h, pal, seed, cell=max(6, w / 3.0), squash=0.9) if not pal["big"] else smooth_skin(w, h, pal, seed)


# ------------------------------------------------------------------------------------------------------------------ spikes, claws, fangs
def spike(w, h, pal, seed):
    """A flat spike card (the picture's top is its tip, leaning to the left = backward on a model's right side): outline, red foot, golden tip, a lit left edge; transparent outside."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    pts = [(w * 0.10, h), (w * 0.96, h), (w * 0.70, h * 0.46), (w * 0.10, 0.5)]
    poly(dr, pts, col(pal["line"]))
    inset = [(w * 0.20, h - 0.9), (w * 0.88, h - 0.9), (w * 0.66, h * 0.47), (w * 0.12, 2.2)]
    poly(dr, inset, col(pal["spike"]))
    tip = [(w * 0.12, 3.5), (w * 0.36, h * 0.34), (w * 0.66, h * 0.5), (w * 0.64, h * 0.48)]
    dr.polygon(S([(w * 0.12, 3.5), (w * 0.34, h * 0.30), (w * 0.52, h * 0.42), (w * 0.64, h * 0.48), (w * 0.40, h * 0.30)]), fill=col(pal["spike_tip"]))
    line(dr, [(w * 0.30, h - 2), (w * 0.14, 5)], col(pal["light"], 220), 0.8)
    return done(img, w, h), clear(w, h)


def horn(w, h, pal, seed):
    """A curved horn card: a tall swept shape, ringed, pale at the tip; the big form's two head horns and the frill spines."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    left = bez((w * 0.18, h), (w * 0.10, h * 0.45), (w * 0.02, 1.0), 16)
    right = bez((w * 0.96, h), (w * 0.62, h * 0.55), (w * 0.02, 1.0), 16)
    pts = left + right[::-1]
    poly(dr, pts, col(pal["line"]))
    l2 = bez((w * 0.26, h - 1.4), (w * 0.17, h * 0.46), (w * 0.08, 4.0), 16)
    r2 = bez((w * 0.86, h - 1.4), (w * 0.58, h * 0.56), (w * 0.08, 4.0), 16)
    poly(dr, l2 + r2[::-1], col(pal["spike"]))
    for k in range(1, 5):
        y = h * (1 - k * 0.19)
        line(dr, [(w * 0.18 + (0.5 - k * 0.07) * w * 0.4, y), (w * 0.8 - k * 0.12 * w * 0.5, y + 1.0)], col(pal["line"], 190), 0.8)
    tipc = bez((w * 0.08, 4.0), (w * 0.12, h * 0.22), (w * 0.24, h * 0.36), 10)
    line(dr, tipc, col(pal["spike_tip"]), 1.6)
    return done(img, w, h), clear(w, h)


def claw(w, h, pal, seed):
    """A claw card: a curved hook, pale on the small form and deep red on the big one, outlined, dark at the root; the picture's top is the tip."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    left = bez((w * 0.10, h), (w * 0.14, h * 0.40), (w * 0.62, 0.5), 14)
    right = bez((w * 0.92, h), (w * 0.84, h * 0.50), (w * 0.62, 0.5), 14)
    poly(dr, left + right[::-1], col(pal["line"]))
    l2 = bez((w * 0.24, h - 1.2), (w * 0.26, h * 0.46), (w * 0.60, 3.0), 14)
    r2 = bez((w * 0.80, h - 1.2), (w * 0.72, h * 0.52), (w * 0.60, 3.0), 14)
    poly(dr, l2 + r2[::-1], col(pal["claw"]))
    dr.polygon(S(l2[: len(l2) // 2] + [(w * 0.5, h * 0.55), (w * 0.36, h - 1.2)]), fill=mix(rgb(pal["claw"]), rgb("#FFFFFF"), 0.25))
    dr.polygon(S([(w * 0.22, h - 1.2), (w * 0.82, h - 1.2), (w * 0.78, h * 0.80), (w * 0.26, h * 0.80)]), fill=col(pal["claw_dark"]))
    return done(img, w, h), clear(w, h)


def fang(w, h, pal, seed):
    """A white fang card (tip at the picture's top)."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    poly(dr, [(w * 0.08, h), (w * 0.92, h), (w * 0.5, 0.4)], col(pal["tooth"]), col(pal["line"]), 0.8)
    return done(img, w, h), clear(w, h)


def bone_bar(w, h, pal, seed):
    """The arm bone of a wing and the finger bones: dark red with a lit top edge, lighter knuckle rings, an outline line along each long edge."""
    rng = np.random.default_rng(seed)
    b, lt = rgb(pal["bone"]), rgb(pal["bone_light"])
    img = canvas(w, h, col(pal["bone"]))
    dr = ImageDraw.Draw(img)
    dr.rectangle([0, 0, w * SS, int(h * 0.34 * SS)], fill=col(pal["bone_light"]))
    dr.rectangle([0, int(h * 0.80 * SS), w * SS, h * SS], fill=col(pal["deep"]))
    for x in np.arange(w * 0.18, w, max(8.0, w / 5.0)):
        line(dr, [(x, 0), (x, h)], col(pal["line"], 200), 0.9)
    line(dr, [(0, 0.4), (w, 0.4)], col(pal["line"]), 0.9)
    line(dr, [(0, h - 0.4), (w, h - 0.4)], col(pal["line"]), 0.9)
    out = done(img, w, h)
    out[..., 3] = 255
    return out


# ------------------------------------------------------------------------------------------------------------------ the head
def _eye(dr, cx, cy, rx, ry, pal, tilt):
    def pt(a, r, sx=1.0, sy=1.0):
        return (cx + (math.cos(a) * r * rx * sx) * math.cos(tilt) - (math.sin(a) * r * ry * sy) * math.sin(tilt),
                cy + (math.cos(a) * r * rx * sx) * math.sin(tilt) + (math.sin(a) * r * ry * sy) * math.cos(tilt))
    ring = [pt(a, 1.0) for a in np.linspace(0, math.tau, 40, endpoint=False)]
    poly(dr, ring, col(pal["line"]))
    iris = [pt(a, 0.84) for a in np.linspace(0, math.tau, 40, endpoint=False)]
    poly(dr, iris, col(pal["eye"]))
    low = [pt(a, 0.84) for a in np.linspace(0.15, math.pi - 0.15, 20)]
    dr.polygon(S(low + [pt(math.pi - 0.15, 0.3), pt(0.15, 0.3)]), fill=mix(rgb(pal["eye"]), rgb("#E8A020"), 0.45))
    slit = [pt(-math.pi / 2, 0.86, 0.0, 1.0), pt(0, 0.22, 1.0, 0.0), pt(math.pi / 2, 0.86, 0.0, 1.0), pt(math.pi, 0.22, 1.0, 0.0)]
    dr.polygon(S(slit), fill=col(pal["eye_dark"]))
    sx, sy = pt(math.radians(-58), 0.5)
    dr.ellipse([(sx - 0.8) * SS, (sy - 0.8) * SS, (sx + 0.8) * SS, (sy + 0.8) * SS], fill=(255, 255, 255, 255))
    lid = [pt(a, 1.08) for a in np.linspace(math.pi * 1.02, math.pi * 1.98, 18)]
    line(dr, lid, col(pal["line"]), 1.8)


def eye_card(w, h, pal, seed, mirror=False):
    """The eye as a card (a big round eye with a slit pupil, outlined, a heavy upper lid) over a transparent ground; the glow sheet lights the iris. Picture painted facing LEFT (front at the left)."""
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    _eye(dr, w * 0.5, h * 0.5, w * 0.46, h * 0.46, pal, math.radians(-16))
    c = done(lay, w, h)
    gl = canvas(w, h)
    dg = ImageDraw.Draw(gl)
    r = 0.34
    dg.ellipse([(w * 0.5 - w * r) * SS, (h * 0.5 - h * r) * SS, (w * 0.5 + w * r) * SS, (h * 0.5 + h * r) * SS], fill=col(pal["glow"], 255))
    ga = done(gl, w, h)
    sl = canvas(w, h)
    ds = ImageDraw.Draw(sl)
    ds.polygon(S([(w * 0.5, h * 0.16), (w * 0.5 + 1.3, h * 0.5), (w * 0.5, h * 0.84), (w * 0.5 - 1.3, h * 0.5)]), fill=(0, 0, 0, 255))
    sa = done(sl, w, h)
    g = np.zeros((h, w, 4), np.float32)
    g[..., :3] = ga[..., :3]
    g[..., 3] = np.clip(ga[..., 3] * (1.0 - sa[..., 3] / 255.0), 0, 255)
    if mirror:
        c, g = flipx(c, g)
    return c, g


def skull_side(w, h, pal, seed, mirror=False):
    """The skull's side (picture facing LEFT, front at the left): plates, a dark brow ridge line, the cheek line, a temple spine row; for the BIG form smooth skin with creases."""
    img = skin(w, h, pal, seed, cell=max(5.0, w / 7.0), spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    brow = [(w * 0.02, h * 0.34), (w * 0.60, h * 0.12), (w * 0.62, h * 0.30), (w * 0.06, h * 0.50)]
    poly(dr, brow, col(pal["dark"], 200), col(pal["line"]), 1.3)
    line(dr, [(w * 0.10, h * 0.78), (w * 0.60, h * 0.70), (w * 0.98, h * 0.82)], col(pal["line"], 220), 1.2)
    out = over(img, done(lay, w, h))
    g = clear(w, h)
    if mirror:
        out, g = flipx(out, g)
    return out, g


def snout_side(w, h, pal, seed, mirror=False):
    """The upper jaw's side (front at the left): plates, a dark mouth line along the bottom with a row of teeth hanging below it, a nostril near the tip, a pale lip line."""
    img = skin(w, h, pal, seed, cell=max(5.0, w / 8.0), spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    y0 = h * 0.80
    poly(dr, [(0, y0), (w, y0 - 1.2), (w, h), (0, h)], col(pal["mouth"]))
    n = max(4, int(w / 13))
    for i in range(n):
        x = (i + 0.5) * w / n
        t = 0.8 + 0.4 * abs(math.sin(i * 2.1))
        poly(dr, [(x - w / n * 0.26, y0 - 1), (x + w / n * 0.26, y0 - 1), (x, y0 + (h - y0) * 0.98 * t)], col(pal["tooth"]), col(pal["line"]), 0.6)
    line(dr, [(0, y0), (w, y0 - 1.2)], col(pal["line"]), 1.5)
    dr.ellipse([w * 0.10 * SS, h * 0.26 * SS, (w * 0.10 + 4.6) * SS, (h * 0.26 + 3.0) * SS], fill=col(pal["line"]))
    out = over(img, done(lay, w, h))
    g = clear(w, h)
    if mirror:
        out, g = flipx(out, g)
    return out, g


def jaw_side(w, h, pal, seed, mirror=False):
    """The lower jaw's side (front at the left): plates, a lighter chin band along the bottom, teeth standing up from the top edge, a dark line under the teeth."""
    img = skin(w, h, pal, seed, cell=max(5.0, w / 8.0), spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    poly(dr, [(0, h * 0.70), (w, h * 0.66), (w, h), (0, h)], col(pal["belly"], 235), None)
    line(dr, [(0, h * 0.70), (w, h * 0.66)], col(pal["line"], 200), 1.2)
    n = max(4, int(w / 9))
    for i in range(n):
        x = (i + 0.5) * w / n
        t = 0.7 + 0.5 * abs(math.sin(i * 1.7 + 1))
        poly(dr, [(x - w / n * 0.27, h * 0.32), (x + w / n * 0.27, h * 0.32), (x, h * 0.32 * (1 - t))], col(pal["tooth"]), col(pal["line"]), 0.6)
    line(dr, [(0, h * 0.32), (w, h * 0.32)], col(pal["line"]), 1.5)
    out = over(img, done(lay, w, h))
    g = clear(w, h)
    if mirror:
        out, g = flipx(out, g)
    return out, g


def jaw_in(w, h, pal, seed):
    """The inside of the mouth seen on the lower jaw's top: dark red, a pink tongue down the middle, teeth along both edges. Glows (a furnace in the throat)."""
    c = solid(w, h, pal["mouth"])
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    poly(dr, [(w * 0.30, 0), (w * 0.70, 0), (w * 0.62, h), (w * 0.38, h)], col("#D8503A"), col(pal["line"]), 0.8)
    n = max(4, int(h / 7))
    for i in range(n):
        y = (i + 0.5) * h / n
        for side in (0, 1):
            x0 = 0 if side == 0 else w
            sgn = 1 if side == 0 else -1
            poly(dr, [(x0, y - h / n * 0.28), (x0, y + h / n * 0.28), (x0 + sgn * w * 0.18, y)], col(pal["tooth"]), col(pal["line"]), 0.5)
    out = over(c, done(lay, w, h))
    g = clear(w, h)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    inner = np.clip(1.0 - np.abs(xx / w - 0.5) * 3.0, 0, 1) * 0.55
    g[..., :3] = rgb("#FF8A30")
    g[..., 3] = inner * 200
    return out, g


def head_top(w, h, pal, seed):
    """The skull's top (front at the BOTTOM of the picture, see the sheet notes): plates, a centre ridge line, two brow lines."""
    img = skin(w, h, pal, seed, cell=max(5.0, w / 7.0), spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    line(dr, [(w * 0.5, 0), (w * 0.5, h)], col(pal["line"], 190), 1.0)
    line(dr, [(w * 0.18, h), (w * 0.22, h * 0.45), (w * 0.12, 0)], col(pal["line"], 200), 1.1)
    line(dr, [(w * 0.82, h), (w * 0.78, h * 0.45), (w * 0.88, 0)], col(pal["line"], 200), 1.1)
    return over(img, done(lay, w, h)), clear(w, h)


def snout_front(w, h, pal, seed):
    """The tip of the snout seen from the front: plates, two nostrils."""
    img = skin(w, h, pal, seed, cell=max(4.0, w / 4.0), spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    for fx in (0.30, 0.70):
        dr.ellipse([(w * fx - 2.2) * SS, (h * 0.40 - 1.6) * SS, (w * fx + 2.2) * SS, (h * 0.40 + 1.6) * SS], fill=col(pal["line"]))
    return over(img, done(lay, w, h)), clear(w, h)


def frill(w, h, pal, seed, mirror=False):
    """The cheek frill: a fan of three swept spines joined by a thin web, outlined (the picture's left is its root, the spines sweep to the right)."""
    img = canvas(w, h)
    dr = ImageDraw.Draw(img)
    tips = [(w * 0.98, h * 0.12), (w * 0.88, h * 0.50), (w * 0.70, h * 0.92)]
    root = (w * 0.02, h * 0.50)
    web = [root, (w * 0.30, h * 0.08)] + [tips[0], (w * 0.74, h * 0.34), tips[1], (w * 0.60, h * 0.72), tips[2], (w * 0.30, h * 0.96)]
    poly(dr, web, col(pal["dark"] if not pal["big"] else pal["membrane2"]), col(pal["line"]), 1.1)
    for t in tips:
        line(dr, [root, t], col(pal["line"]), 2.2)
        line(dr, [root, t], col(pal["bone_light"]), 1.0)
    return done(img, w, h), clear(w, h)


# ------------------------------------------------------------------------------------------------------------------ the dragon's body (sculpted by lines)
def chest_front(w, h, pal, seed):
    """The big form's chest seen from the front: two heavy pectorals with a sternum line, collar bones, the shoulder caps; outline creases and soft shadows under each muscle (the cel look of the still)."""
    img = smooth_skin(w, h, pal, seed, spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    sh = col(pal["deep"], 120)
    lc = col(pal["line"], 235)
    cx = w / 2
    for sx in (-1, 1):
        pec = [(cx + sx * 1.5, h * 0.12), (cx + sx * w * 0.20, h * 0.10), (cx + sx * w * 0.42, h * 0.20), (cx + sx * w * 0.46, h * 0.42), (cx + sx * w * 0.30, h * 0.62), (cx + sx * w * 0.08, h * 0.60), (cx + sx * 1.5, h * 0.50)]
        under = [(cx + sx * w * 0.44, h * 0.44), (cx + sx * w * 0.30, h * 0.66), (cx + sx * w * 0.06, h * 0.64), (cx + sx * w * 0.06, h * 0.58), (cx + sx * w * 0.30, h * 0.58)]
        dr.polygon(S(under), fill=sh)
        line(dr, bez((cx + sx * w * 0.44, h * 0.38), (cx + sx * w * 0.40, h * 0.62), (cx + sx * w * 0.08, h * 0.62), 14), lc, 1.4)
        line(dr, bez((cx + sx * w * 0.44, h * 0.18), (cx + sx * w * 0.28, h * 0.06), (cx + sx * 2.0, h * 0.14), 14), lc, 1.1)
        line(dr, bez((cx + sx * w * 0.10, h * 0.66), (cx + sx * w * 0.16, h * 0.82), (cx + sx * w * 0.22, h * 0.96), 10), lc, 1.0)
    line(dr, [(cx, h * 0.10), (cx, h * 0.62)], lc, 1.5)
    for k in range(4):                                                                      # the ribs under the pecs, soft
        y = h * (0.70 + k * 0.07)
        for sx in (-1, 1):
            line(dr, bez((cx + sx * 3, y), (cx + sx * w * 0.20, y + 3 + k), (cx + sx * w * 0.36, y - 2 + k * 2), 10), col(pal["line"], 150), 0.9)
    return over(img, done(lay, w, h)), clear(w, h)


def torso_front(w, h, pal, seed, x_half=27.0, y_top=60.0, y_span=62.0):
    """The whole front of the big form's torso as ONE picture (cut into windows over the stacked slices of the torso): from the hips (bottom) to the collar bones (top), x in [-x_half, x_half].
    Cel look: the skin, flat lit shapes on the pectorals and every block of the abs, soft shadows under the muscles, dark outlines: the sternum line, the pec borders, the ribs, the six-pack with its
    centre line, the V of the obliques into the hips and the collar bones."""
    img = smooth_skin(w, h, pal, seed, spots=False)
    sx_, sy_ = w / (2 * x_half), h / y_span

    def P(x, y):
        return ((x + x_half) * sx_, (y_top - y) * sy_)

    def Q(pts):
        return [P(x, y) for x, y in pts]
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    lc, lc2 = col(pal["line"], 240), col(pal["line"], 160)
    shade, lite = col(pal["deep"], 105), col(pal["light"], 150)
    for sgn in (-1, 1):
        def X(x, y):
            return P(sgn * x, y)
        pec = [X(1.2, 56.0), X(10, 57.5), X(21, 55.0), X(25.5, 49.0), X(22, 43.0), X(12, 40.5), X(3, 42.0), X(1.2, 46.0)]
        dr.polygon(S(pec), fill=lite)
        under = [X(25.5, 49.0), X(22, 43.0), X(12, 40.5), X(3, 42.0), X(3, 40.0), X(12, 38.2), X(23, 41.2), X(27, 48.0)]
        dr.polygon(S(under), fill=shade)
        line(dr, [X(25.5, 49.5), X(23.5, 43.5), X(12, 40.5), X(3, 42.0)], lc, 1.5)
        line(dr, [X(2, 57.5), X(12, 59.0), X(24, 56.4)], lc2, 1.1)                                      # collar bone
        line(dr, [X(25.5, 50.5), X(26.4, 52.5)], lc2, 1.0)
        for k, y in enumerate((38.0, 34.2, 30.6)):                                                     # the ribs
            line(dr, bez(X(5, y + 0.5), X(15, y + 1.2), X(23, y - 1.0), 10), lc2, 1.0)
        for k in range(3):                                                                              # the six-pack: three blocks each side
            y0 = 29.0 - k * 7.2
            blk = [X(1.4, y0), X(10.5, y0 + 0.4), X(10.8, y0 - 6.0), X(1.4, y0 - 6.4)]
            dr.polygon(S(blk), fill=lite)
            dr.polygon(S([X(1.4, y0 - 5.0), X(10.8, y0 - 4.8), X(10.8, y0 - 6.0), X(1.4, y0 - 6.4)]), fill=shade)
            line(dr, [X(1.4, y0 - 6.4), X(10.8, y0 - 6.0)], lc, 1.3)
            line(dr, [X(10.8, y0 + 0.4), X(11.4, y0 - 6.0)], lc2, 1.0)
        line(dr, bez(X(19.5, 36), X(18.0, 20), X(8.0, 2.0), 14), lc, 1.4)                                # the V of the obliques
        line(dr, bez(X(23.5, 33), X(22.5, 18), X(12.0, 3.0), 12), lc2, 1.0)
    line(dr, [P(0, 58.5), P(0, 0.5)], lc, 1.6)
    line(dr, [P(-2.5, 58.2), P(0, 56.2), P(2.5, 58.2)], lc2, 1.0)
    return over(img, done(lay, w, h)), clear(w, h)


def abs_front(w, h, pal, seed):
    """The big form's belly seen from the front: the sternum line running down, the six-pack with its centre line, the two oblique lines to the hips (V), soft shadows."""
    img = smooth_skin(w, h, pal, seed + 1, spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    cx = w / 2
    lc = col(pal["line"], 235)
    line(dr, [(cx, 0), (cx, h * 0.96)], lc, 1.3)
    for k in range(4):
        y = h * (0.10 + k * 0.20)
        for sx in (-1, 1):
            blk = [(cx + sx * 2, y), (cx + sx * w * 0.30, y + 0.5), (cx + sx * w * 0.32, y + h * 0.15), (cx + sx * 2, y + h * 0.16)]
            dr.polygon(S([(cx + sx * 2, y + h * 0.11), (cx + sx * w * 0.32, y + h * 0.11), (cx + sx * w * 0.32, y + h * 0.16), (cx + sx * 2, y + h * 0.16)]), fill=col(pal["deep"], 90))
            line(dr, [(cx + sx * 2, y + h * 0.16), (cx + sx * w * 0.32, y + h * 0.155)], lc, 1.1)
    for sx in (-1, 1):
        line(dr, bez((cx + sx * w * 0.36, h * 0.08), (cx + sx * w * 0.44, h * 0.50), (cx + sx * w * 0.22, h * 0.98), 14), lc, 1.2)
        line(dr, bez((cx + sx * w * 0.20, h * 0.78), (cx + sx * w * 0.18, h * 0.92), (cx + sx * w * 0.06, h * 1.0), 10), col(pal["line"], 160), 1.0)
    return over(img, done(lay, w, h)), clear(w, h)


def muscle_side(w, h, pal, seed):
    """A limb or torso side with muscle lines: a long bowed crease and two short ones (the big form's arms and thighs, the neck's tendons)."""
    img = smooth_skin(w, h, pal, seed + 3, spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    lc = col(pal["line"], 220)
    line(dr, bez((w * 0.1, h * 0.12), (w * 0.62, h * 0.30), (w * 0.34, h * 0.9), 16), lc, 1.2)
    line(dr, bez((w * 0.55, h * 0.18), (w * 0.8, h * 0.4), (w * 0.72, h * 0.8), 14), col(pal["line"], 150), 1.0)
    line(dr, bez((w * 0.2, h * 0.5), (w * 0.28, h * 0.6), (w * 0.2, h * 0.7), 8), col(pal["line"], 150), 0.9)
    return over(img, done(lay, w, h)), clear(w, h)


def neck_front(w, h, pal, seed):
    """The long throat of the big form: a centre tendon line, two bowed side lines, soft plates (belly colour) near the jaw."""
    img = smooth_skin(w, h, pal, seed + 5, spots=False)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    lc = col(pal["line"], 220)
    cx = w / 2
    line(dr, [(cx, 0), (cx, h)], lc, 1.3)
    for sx in (-1, 1):
        line(dr, bez((cx + sx * w * 0.2, 0), (cx + sx * w * 0.36, h * 0.5), (cx + sx * w * 0.26, h), 16), lc, 1.1)
        for k in range(6):
            y = h * (0.08 + k * 0.15)
            line(dr, [(cx + sx * 2, y), (cx + sx * w * 0.30, y + 1.5)], col(pal["line"], 110), 0.8)
    return over(img, done(lay, w, h)), clear(w, h)


# ------------------------------------------------------------------------------------------------------------------ the wing
def wing_panel(w, h, pal, seed, poly_px, bones_px, veins_px, edge_px):
    """One panel of the membrane: the polygon (picture pixels, outward = right, backward = down) filled with two flat orange tones (a darker one near the bones), veins as thin dark red lines, the
    finger bones drawn as outlined bars, the scalloped outer edge as a dark outline. Transparent outside the polygon."""
    rng = np.random.default_rng(seed)
    m1, m2, hi = rgb(pal["membrane"]), rgb(pal["membrane2"]), rgb(pal["membrane_hi"])
    mem = canvas(w, h)
    dm = ImageDraw.Draw(mem)
    dm.polygon(S(poly_px), fill=col(pal["membrane"]))
    marr = done(mem, w, h)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    if bones_px:
        b0 = bones_px[0][0]
        rr = np.hypot(xx - b0[0], yy - b0[1]) / (max(w, h) * 0.9)
    else:
        rr = np.zeros((h, w), np.float32)
    t = smoothstep(rr, 0.12, 0.8)[..., None]
    n = fbm(rng, w, h, 3, 3)[..., None]
    mc = m2 + (m1 - m2) * t
    mc = np.where(n > 0.62, mc * 0.93 + hi * 0.07 + 6, mc)
    out = np.concatenate([mc, marr[..., 3:4]], axis=-1)
    lay = canvas(w, h)
    dr = ImageDraw.Draw(lay)
    for v in veins_px:
        line(dr, v, col(pal["vein"], 220), 1.0)
    for bn in bones_px:
        wd = bn[2] if len(bn) > 2 else 2.4
        line(dr, [bn[0], bn[1]], col(pal["line"]), wd + 1.6)
        line(dr, [bn[0], bn[1]], col(pal["bone"]), wd)
        line(dr, [bn[0], bn[1]], col(pal["bone_light"], 200), max(0.6, wd * 0.3))
    if edge_px:
        line(dr, edge_px, col(pal["line"], 240), 1.3)
    out = over(out, done(lay, w, h))
    out[..., 3] = np.minimum(out[..., 3], 255.0)
    out[..., 3] = np.where(out[..., 3] > 40, 255.0, 0.0) * np.where(out[..., 3] > 0, 1.0, 0.0)
    out[..., :3] = np.where(out[..., 3:4] > 0, out[..., :3], 0.0)
    return out, clear(w, h)


# ------------------------------------------------------------------------------------------------------------------ a lick of flame (glow sheet only)
def flame_lick(w, h, pal, seed):
    """A tongue of flame standing on its foot (the bottom of the picture): a red-orange body, a yellow heart, a white-hot core, a curling tip; transparent outside. Painted into the GLOW sheet only
    (the glow-only model draws it additive), so the colour sheet stays empty there."""
    rng = np.random.default_rng(seed)
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    v = 1.0 - yy / h
    sway = 0.10 * np.sin(v * 5.0 + 1.0) * (v ** 1.2)
    half = (0.40 * (1.0 - v ** 1.5) + 0.04) * (0.85 + 0.15 * np.sin(v * 17.0))
    d = np.abs((xx / w - 0.5) - sway)
    body = np.clip(1.0 - d / np.maximum(half, 1e-3), 0.0, 1.0)
    n = fbm(rng, w, h, 3, 4)
    body = np.clip(body * (0.85 + 0.3 * n), 0.0, 1.0)
    a = smoothstep(body, 0.05, 0.22)
    t = np.clip(body * 1.05 - 0.12, 0.0, 1.0)[..., None]
    red, org, yel, wht = rgb("#E8341A"), rgb("#FF8A20"), rgb("#FFD860"), rgb("#FFF6D0")
    c = red + (org - red) * np.clip(t * 2.0, 0, 1) + (yel - org) * np.clip(t * 2.0 - 1.0, 0, 1) * 0.9 + (wht - yel) * np.clip(t * 2.6 - 2.0, 0, 1)
    g = np.zeros((h, w, 4), dtype=np.float32)
    g[..., :3] = np.clip(c, 0, 255) * a[..., None]
    g[..., 3] = a * 255.0
    return np.zeros((h, w, 4), dtype=np.float32), g

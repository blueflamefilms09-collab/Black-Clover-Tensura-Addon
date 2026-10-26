"""Generates the Veldanava "Sandalphon, Lord of Judgment" VFX textures (vfx.client.layer.JudgmentLayer).

Deterministic and font-free (every machine writes the same pixels). Written from scratch for this mod; shares no code or asset with any other mod.
Output: src/main/resources/assets/nusmp/textures/particle/judgment_*.png  (new files only; nothing existing is touched)

    python tools/gen_judgment_vfx_textures.py

Theme: executioner angel. Cold white-gold light, pale steel, thin black cracks, ticking clock / stutter motifs, a heavenly cross.
Every texture is white (+ alpha) so the vertex colour tints it: gold light is drawn ADD, the black cracks / black blade core are drawn
ALPHA with a near-black tint.

    judgment_ring       thin crisp circle with a halo and two faint inner rings (shock rings, the field rim, the sigil rim)
    judgment_wave       the edict wave: a bright leading edge, a long trailing gradient, faint ripples
    judgment_sigil      the judgment seal: double rim, minute ticks, tally-mark groups, inner hexagram, cardinal lines
    judgment_clock      a clock ring: 60 ticks (12 bold), four chevrons, a second hairline ring
    judgment_glyphs     a 16 x 1 band of text-like marks (angular pseudo-script), tiles horizontally
    judgment_flare      a four-pointed cross flare with faint diagonal rays and a core
    judgment_mote       a tiny diamond mote
    judgment_needle     the bullet: pointed head at the top (v = 0), long fading tail
    judgment_beam       a vertical light shaft with fine striations, seamless in V (it scrolls)
    judgment_shaft      a solid rod of light (flat-topped across, soft edges, seamless in V): the cores of the cross
    judgment_crack      a hairline crack for a ribbon (seamless in V, tiles along the trail)
    judgment_cracks_ground   a radial crack network for a ground plane
    judgment_slash      the guillotine crescent: bright hair-line outer edge, soft body, speed lines (u along, v across, v = 0 outer)
    judgment_blade      the hovering glyph: a blade pointing down whose cross-guard is a balance scale
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
WHITE = (255, 255, 255, 255)


def save(im, name):
    os.makedirs(OUT, exist_ok=True)
    im.save(os.path.join(OUT, name + ".png"), optimize=True)
    print("wrote", name, im.size)


def from_alpha(a, tint=(255, 255, 255)):
    """White (or tinted) RGBA image from an alpha array 0..1."""
    a = np.clip(a, 0, 1)
    img = np.dstack([np.full_like(a, tint[0]), np.full_like(a, tint[1]), np.full_like(a, tint[2]), a * 255]).astype(np.uint8)
    return Image.fromarray(img, "RGBA")


def alpha_of(im):
    return np.asarray(im.split()[3], dtype=np.float32) / 255.0


def smooth(e0, e1, x):
    t = np.clip((x - e0) / (e1 - e0), 0, 1)
    return t * t * (3 - 2 * t)


def grid(w, h):
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    return xx, yy


def disc_coords(size):
    xx, yy = grid(size, size)
    c = (size - 1) / 2
    x, y = (xx - c) / c, (yy - c) / c
    return x, y, np.sqrt(x * x + y * y), np.arctan2(y, x)


def glow_add(im, radius, gain):
    """The image plus a blurred copy of its alpha (a soft bloom around thin lines)."""
    a = alpha_of(im)
    b = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(radius)), dtype=np.float32) / 255.0
    return np.clip(a + b * gain, 0, 1)


# ---------------------------------------------------------------------------------------------------- ring
def ring(size=512):
    x, y, r, th = disc_coords(size)
    core = np.exp(-(((r - 0.965) / 0.0062) ** 2))
    halo = np.exp(-(((r - 0.965) / 0.032) ** 2)) * 0.34
    inner = np.exp(-(((r - 0.925) / 0.0042) ** 2)) * 0.34
    inner2 = np.exp(-(((r - 0.885) / 0.0034) ** 2)) * 0.17
    outer = np.exp(-(((r - 0.992) / 0.0030) ** 2)) * 0.22
    a = (core + halo + inner + inner2 + outer) * (0.9 + 0.1 * np.cos(th * 12))
    a *= smooth(1.0, 0.985, r)
    save(from_alpha(a), "judgment_ring")


# ---------------------------------------------------------------------------------------------------- wave
def wave(size=512):
    x, y, r, th = disc_coords(size)
    rng = np.random.default_rng(7)
    phase = rng.uniform(0, 6.28, 6)
    streak = 0.82 + 0.18 * sum(np.sin(th * (5 + 3 * k) + phase[k]) for k in range(6)) / 6.0 * 2.0
    lead = np.exp(-(((r - 0.955) / 0.010) ** 2))
    d = np.clip(0.955 - r, 0, 1)
    body = np.where(r < 0.955, np.exp(-d / 0.085) * 0.50, 0.0)
    tail = np.where(r < 0.955, np.exp(-d / 0.30) * 0.13, 0.0)
    ripple = np.where(r < 0.955, (0.5 + 0.5 * np.cos(d * 110)) ** 3 * np.exp(-d / 0.22) * 0.30, 0.0)
    outer = np.where(r >= 0.955, np.exp(-((r - 0.955) / 0.022) ** 2) * 0.5, 0.0)
    a = (lead * 1.0 + body * streak + tail + ripple * streak + outer)
    a *= smooth(1.0, 0.975, r) * smooth(0.0, 0.10, r)
    save(from_alpha(a), "judgment_wave")


# ---------------------------------------------------------------------------------------------------- sigil
def sigil(size=512, ss=3):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def circle(rad, w, fill=WHITE):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=fill, width=max(1, int(w * ss)))

    def pol(rad, ang):
        return c + math.cos(ang) * rad * R, c + math.sin(ang) * rad * R

    def seg(r0, a0, r1, a1, w):
        d.line([pol(r0, a0), pol(r1, a1)], fill=WHITE, width=max(1, int(w * ss)))

    circle(0.985, 3.2)                                     # double rim
    circle(0.955, 1.4)
    for k in range(60):                                    # minute ticks
        a = math.radians(k * 6)
        if k % 5 == 0:
            seg(0.90, a, 0.945, a, 2.6)
        else:
            seg(0.915, a, 0.94, a, 1.2)
    circle(0.895, 1.2)
    circle(0.725, 1.2)
    for g in range(12):                                    # tally groups: four strokes struck through by a fifth
        ag = math.radians(g * 30 + 15)
        for k in range(4):
            off = (k - 1.5) * 0.026
            seg(0.745, ag + off, 0.875, ag + off, 2.0)
        seg(0.755, ag - 0.047, 0.865, ag + 0.047, 2.0)
    for g in range(12):                                    # a small diamond between the groups
        a = math.radians(g * 30)
        px, py = pol(0.81, a)
        rr = 3.6 * ss
        d.polygon([(px - math.sin(a) * rr, py + math.cos(a) * rr), (px + math.cos(a) * rr * 1.6, py + math.sin(a) * rr * 1.6),
                   (px + math.sin(a) * rr, py - math.cos(a) * rr), (px - math.cos(a) * rr * 1.6, py - math.sin(a) * rr * 1.6)], fill=WHITE)
    circle(0.685, 2.4)
    for k in range(12):                                    # inward chevrons
        a = math.radians(k * 30)
        p0, p1, p2 = pol(0.655, a - 0.035), pol(0.600, a), pol(0.655, a + 0.035)
        d.line([p0, p1, p2], fill=WHITE, width=2 * ss)
    circle(0.56, 1.3)
    for tri in (0, 1):                                     # the hexagram of the scale
        pts = [pol(0.54, math.radians(-90 + 120 * k + 60 * tri)) for k in range(3)]
        d.line(pts + [pts[0]], fill=WHITE, width=int(1.6 * ss))
    circle(0.28, 1.4)
    circle(0.10, 2.0)
    for k in range(4):                                     # cardinal lines
        a = math.radians(90 * k)
        seg(0.10, a, 0.28, a, 1.6)
        seg(0.56, a, 0.685, a, 2.4)
    big = im.resize((size, size), Image.LANCZOS)
    save(from_alpha(glow_add(big, 2.2, 0.55)), "judgment_sigil")


# ---------------------------------------------------------------------------------------------------- clock
def clock(size=256, ss=4):
    S = size * ss
    c = S / 2
    R = c * 0.985
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def pol(rad, ang):
        return c + math.sin(ang) * rad * R, c - math.cos(ang) * rad * R

    def circle(rad, w):
        d.ellipse([c - rad * R, c - rad * R, c + rad * R, c + rad * R], outline=WHITE, width=max(1, int(w * ss)))

    circle(0.985, 2.4)
    circle(0.74, 1.0)
    for k in range(60):
        a = math.radians(k * 6)
        if k % 5 == 0:
            d.line([pol(0.80, a), pol(0.965, a)], fill=WHITE, width=int(3.2 * ss))
        else:
            d.line([pol(0.905, a), pol(0.965, a)], fill=WHITE, width=int(1.3 * ss))
    for k in range(4):                                     # bold chevrons at 12 / 3 / 6 / 9
        a = math.radians(k * 90)
        d.polygon([pol(0.70, a), pol(0.60, a - 0.11), pol(0.635, a), pol(0.60, a + 0.11)], fill=WHITE)
    for k in range(12):                                    # small dots on the inner ring
        a = math.radians(k * 30 + 15)
        px, py = pol(0.74, a)
        d.ellipse([px - 2.2 * ss, py - 2.2 * ss, px + 2.2 * ss, py + 2.2 * ss], fill=WHITE)
    big = im.resize((size, size), Image.LANCZOS)
    save(from_alpha(glow_add(big, 1.6, 0.5)), "judgment_clock")


# ---------------------------------------------------------------------------------------------------- glyph band
def glyph_cell(d, ox, oy, cw, ch, rng, ss):
    """One angular pseudo-script mark inside the cell (ox, oy, cw, ch). Upright; the top of the cell points outward on the ring."""
    pad = cw * 0.18
    x0, x1, y0, y1 = ox + pad, ox + cw - pad, oy + ch * 0.16, oy + ch * 0.84
    w = max(2, int(3.4 * ss))
    cx = (x0 + x1) / 2
    kind = rng.integers(0, 6)
    if kind == 0:                                          # stem with side ticks
        d.line([(cx, y0), (cx, y1)], fill=WHITE, width=w)
        for k in range(rng.integers(1, 4)):
            yy = y0 + (y1 - y0) * (0.18 + 0.28 * k + rng.uniform(-0.04, 0.04))
            if rng.random() < 0.5:
                d.line([(cx, yy), (x1, yy - (y1 - y0) * 0.10)], fill=WHITE, width=w)
            else:
                d.line([(cx, yy), (x0, yy - (y1 - y0) * 0.10)], fill=WHITE, width=w)
    elif kind == 1:                                        # ring over a bar
        rr = (x1 - x0) * 0.32
        d.ellipse([cx - rr, y0, cx + rr, y0 + 2 * rr], outline=WHITE, width=w)
        d.line([(cx, y0 + 2 * rr), (cx, y1)], fill=WHITE, width=w)
        d.line([(x0, y1), (x1, y1)], fill=WHITE, width=w)
    elif kind == 2:                                        # triangle with a dot
        d.polygon([(cx, y0), (x1, y1), (x0, y1)], outline=WHITE, width=w)
        d.ellipse([cx - 1.7 * ss, y0 + (y1 - y0) * 0.55 - 1.7 * ss, cx + 1.7 * ss, y0 + (y1 - y0) * 0.55 + 1.7 * ss], fill=WHITE)
    elif kind == 3:                                        # two bars joined by a diagonal (a tally struck through)
        d.line([(x0 + (x1 - x0) * 0.25, y0), (x0 + (x1 - x0) * 0.25, y1)], fill=WHITE, width=w)
        d.line([(x0 + (x1 - x0) * 0.75, y0), (x0 + (x1 - x0) * 0.75, y1)], fill=WHITE, width=w)
        d.line([(x0, y0 + (y1 - y0) * 0.78), (x1, y0 + (y1 - y0) * 0.22)], fill=WHITE, width=w)
    elif kind == 4:                                        # hook and bar
        d.arc([x0, y0, x1, y0 + (x1 - x0) * 1.2], 180, 360, fill=WHITE, width=w)
        d.line([(x1, y0 + (x1 - x0) * 0.6), (x1, y1)], fill=WHITE, width=w)
        d.line([(x0, y1), (x1, y1)], fill=WHITE, width=w)
    else:                                                  # arrowhead on a stem
        d.line([(cx, y0), (cx, y1)], fill=WHITE, width=w)
        d.line([(x0, y0 + (y1 - y0) * 0.30), (cx, y0), (x1, y0 + (y1 - y0) * 0.30)], fill=WHITE, width=w)
        d.line([(x0, y1), (x1, y1)], fill=WHITE, width=w)


def glyphs(w=1024, h=64, ss=4, cells=16):
    rng = np.random.default_rng(2024)
    W, H = w * ss, h * ss
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cw = W / cells
    for k in range(cells):
        glyph_cell(d, k * cw, 0, cw, H, rng, ss)
        if k % 4 == 3:                                     # a word break: a small diamond
            px, py = (k + 1) * cw, H / 2
            r = 3.2 * ss
            d.polygon([(px, py - r), (px + r, py), (px, py + r), (px - r, py)], fill=WHITE)
    d.line([(0, H * 0.05), (W, H * 0.05)], fill=(255, 255, 255, 150), width=int(1.2 * ss))        # faint rules top and bottom
    d.line([(0, H * 0.95), (W, H * 0.95)], fill=(255, 255, 255, 150), width=int(1.2 * ss))
    small = im.resize((w, h), Image.LANCZOS)
    save(from_alpha(glow_add(small, 1.4, 0.45)), "judgment_glyphs")


# ---------------------------------------------------------------------------------------------------- flare / mote
def flare(size=128):
    x, y, r, th = disc_coords(size)
    ax, ay = np.abs(x), np.abs(y)
    core = np.exp(-(r * r) / 0.010)
    halo = np.exp(-(r * r) / 0.09) * 0.45
    hor = np.exp(-((ay / 0.020) ** 2)) * np.clip(1 - ax, 0, 1) ** 2.4
    ver = np.exp(-((ax / 0.020) ** 2)) * np.clip(1 - ay, 0, 1) ** 2.4
    u, v = (x + y) * 0.7071, (x - y) * 0.7071
    d1 = np.exp(-((np.abs(v) / 0.026) ** 2)) * np.clip(1 - np.abs(u) * 1.7, 0, 1) ** 2.2 * 0.5
    d2 = np.exp(-((np.abs(u) / 0.026) ** 2)) * np.clip(1 - np.abs(v) * 1.7, 0, 1) ** 2.2 * 0.5
    a = (core + halo + hor + ver + d1 + d2) * smooth(1.0, 0.92, r) * 1.0
    save(from_alpha(a), "judgment_flare")


def mote(size=32):
    x, y, r, th = disc_coords(size)
    dia = np.clip(1 - (np.abs(x) + np.abs(y)) * 1.25, 0, 1) ** 1.5
    core = np.exp(-(r * r) / 0.05)
    a = np.clip(dia * 1.1 + core * 0.6, 0, 1) * smooth(1.0, 0.85, r)
    save(from_alpha(a), "judgment_mote")


# ---------------------------------------------------------------------------------------------------- needle / beam
def needle(w=32, h=256):
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / (h - 1)                                       # 0 = head (top), 1 = tail
    wmax = 0.92
    hw = wmax * smooth(0.0, 0.14, v) ** 0.65 * (1 - 0.72 * v)
    hw = np.maximum(hw, 1e-3)
    core = np.exp(-((x / (hw * 0.30)) ** 2))
    halo = np.exp(-((x / hw) ** 2)) * 0.46
    bright = (1 - v) ** 1.35 * 0.94 + 0.06
    head = np.exp(-((v - 0.10) / 0.05) ** 2) * np.exp(-((x / 0.5) ** 2)) * 0.5
    a = (core + halo) * bright + head
    a *= smooth(1.0, 0.94, v)
    save(from_alpha(a), "judgment_needle")


def beam(w=64, h=256):
    rng = np.random.default_rng(99)
    xx, yy = grid(w, h)
    x = (xx - (w - 1) / 2) / ((w - 1) / 2)
    v = yy / h
    core = np.exp(-((x / 0.075) ** 2))
    mid = np.exp(-((x / 0.22) ** 2)) * 0.55
    halo = np.exp(-((x / 0.62) ** 2)) * 0.30
    a = core + mid + halo
    for _ in range(20):                                    # fine light striations, periodic in V so the texture can scroll
        xk = rng.uniform(-0.75, 0.75)
        length = rng.uniform(0.22, 0.6)
        v0 = rng.uniform(0, 1)
        phase = ((v - v0) % 1.0) / length
        along = np.where(phase < 1.0, np.sin(np.clip(phase, 0, 1) * math.pi), 0.0)
        a = a + np.exp(-(((x - xk) / rng.uniform(0.018, 0.04)) ** 2)) * along * rng.uniform(0.15, 0.38)
    a *= np.clip(1.0 - 0.0 * v, 0, 1) * smooth(1.0, 0.86, np.abs(x))
    save(from_alpha(a), "judgment_beam")


def shaft(w=64, h=256):
    """A solid rod of light: a flat white top across the middle, soft edges, a few faint periodic striations along it (seamless in V)."""
    rng = np.random.default_rng(404)
    xx, yy = grid(w, h)
    x = np.abs((xx - (w - 1) / 2) / ((w - 1) / 2))
    v = yy / h
    a = (0.84 + 0.16 * np.exp(-((x / 0.32) ** 2))) * smooth(0.95, 0.52, x)
    stri = np.zeros_like(a)
    for _ in range(9):
        xk = rng.uniform(0.0, 0.6)
        length = rng.uniform(0.2, 0.55)
        v0 = rng.uniform(0, 1)
        phase = ((v - v0) % 1.0) / length
        along = np.where(phase < 1.0, np.sin(np.clip(phase, 0, 1) * math.pi), 0.0)
        stri = stri + np.exp(-(((x - xk) / 0.05) ** 2)) * along * 0.10
    a = np.clip(a - stri * 0.5, 0, 1)
    save(from_alpha(a), "judgment_shaft")


# ---------------------------------------------------------------------------------------------------- cracks
def jagged(rng, p0, p1, steps, amp):
    """A jagged polyline from p0 to p1 (pinned at both ends)."""
    p0, p1 = np.array(p0, float), np.array(p1, float)
    d = p1 - p0
    n = np.array([-d[1], d[0]]) / (np.linalg.norm(d) + 1e-9)
    pts = []
    offs = np.cumsum(rng.uniform(-1, 1, steps + 1))
    offs -= np.linspace(offs[0], offs[-1], steps + 1)
    offs = offs / (np.abs(offs).max() + 1e-9) * amp
    for i in range(steps + 1):
        t = i / steps
        pts.append(tuple(p0 + d * t + n * offs[i] * math.sin(math.pi * t) ** 0.5))
    return pts


def crack(w=64, h=256, ss=4):
    rng = np.random.default_rng(31337)
    W, H = w * ss, h * ss
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx = W / 2
    pts = jagged(rng, (cx, 0), (cx, H), 26, W * 0.17)
    d.line(pts, fill=WHITE, width=int(4.6 * ss), joint="curve")
    for _ in range(9):                                     # side branches
        i = rng.integers(2, len(pts) - 3)
        side = 1 if rng.random() < 0.5 else -1
        ang = math.radians(rng.uniform(25, 58))
        ln = rng.uniform(0.07, 0.15) * H
        p0 = np.array(pts[i])
        dirv = np.array([math.sin(ang) * side, math.cos(ang)])
        p1 = p0 + dirv * ln
        p1[0] = np.clip(p1[0], W * 0.08, W * 0.92)
        br = jagged(rng, tuple(p0), tuple(p1), 5, W * 0.05)
        d.line(br, fill=WHITE, width=int(2.6 * ss), joint="curve")
        if rng.random() < 0.5:
            j = br[3]
            p2 = np.array(j) + np.array([math.sin(ang * 0.5 + 0.6) * -side, 0.8]) * ln * 0.35
            p2[0] = np.clip(p2[0], W * 0.08, W * 0.92)
            d.line([j, tuple(p2)], fill=WHITE, width=int(1.7 * ss))
    small = im.resize((w, h), Image.LANCZOS)
    a = alpha_of(small)
    halo = np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(3.0)), dtype=np.float32) / 255.0
    save(from_alpha(np.clip(a + halo * 0.35, 0, 1)), "judgment_crack")


def cracks_ground(size=512, ss=2):
    rng = np.random.default_rng(555)
    S = size * ss
    c = S / 2
    R = c * 0.97
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    def walk(p, heading, length, width, depth):
        """A jagged crack walker: steps along a wandering heading, tapering, now and then splitting off a thinner branch."""
        step = R * 0.032
        n = max(2, int(length / step))
        x, y = p
        for i in range(n):
            heading += rng.uniform(-0.42, 0.42)
            nx, ny = x + math.cos(heading) * step, y + math.sin(heading) * step
            if math.hypot(nx - c, ny - c) > R:
                return
            t = i / n
            d.line([(x, y), (nx, ny)], fill=WHITE, width=max(1, int(width * (1 - 0.65 * t) * ss)))
            if depth > 0 and i >= 2 and rng.random() < 0.20:
                walk((nx, ny), heading + rng.choice([-1, 1]) * math.radians(rng.uniform(28, 62)), length * rng.uniform(0.25, 0.5), width * 0.62, depth - 1)
            x, y = nx, ny

    n = 15
    for k in range(n):
        a0 = k / n * math.tau + rng.uniform(-0.12, 0.12)
        walk((c + math.cos(a0) * R * 0.03, c + math.sin(a0) * R * 0.03), a0, R * rng.uniform(0.55, 1.0), 3.6, 2)
    for rad, count in ((0.34, 5), (0.62, 7)):              # broken concentric hairlines
        for k in range(count):
            a = rng.uniform(0, math.tau)
            ln = rng.uniform(0.15, 0.55)
            d.arc([c - rad * R, c - rad * R, c + rad * R, c + rad * R], math.degrees(a), math.degrees(a + ln), fill=WHITE, width=int(1.4 * ss))
    big = im.resize((size, size), Image.LANCZOS)
    a = alpha_of(big)
    xx, yy = grid(size, size)
    r = np.sqrt(((xx - size / 2) / (size / 2)) ** 2 + ((yy - size / 2) / (size / 2)) ** 2)
    a *= smooth(1.0, 0.93, r)
    save(from_alpha(np.clip(a, 0, 1)), "judgment_cracks_ground")


# ---------------------------------------------------------------------------------------------------- slash
def slash(w=256, h=64):
    rng = np.random.default_rng(77)
    xx, yy = grid(w, h)
    u = xx / (w - 1)                                       # 0 tail .. 1 head
    v = yy / (h - 1)                                       # 0 outer edge .. 1 inner
    edge = np.exp(-((v / 0.045) ** 2))
    edge_halo = np.exp(-((v / 0.14) ** 2)) * 0.5
    body = np.exp(-((v / 0.5) ** 2)) * 0.5 * (1 - smooth(0.55, 1.0, v))
    a = edge + edge_halo + body
    for _ in range(11):                                    # speed lines along the sweep
        vk = rng.uniform(0.12, 0.8)
        u0, u1 = rng.uniform(0.0, 0.5), rng.uniform(0.55, 1.0)
        along = np.clip((u - u0) / (u1 - u0), 0, 1)
        a = a + np.exp(-(((v - vk) / 0.018) ** 2)) * along ** 1.5 * (u <= u1) * rng.uniform(0.18, 0.4)
    a *= 0.78 + 0.22 * u
    a *= smooth(1.0, 0.97, v) * smooth(0.0, 0.015, u) * smooth(1.0, 0.985, u)
    save(from_alpha(a), "judgment_slash")


# ---------------------------------------------------------------------------------------------------- blade glyph
def blade(w=96, h=256, ss=4):
    W, H = w * ss, h * ss
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    cx = W / 2
    # grip and pommel
    d.rectangle([cx - W * 0.032, H * 0.105, cx + W * 0.032, H * 0.27], fill=WHITE)
    for k in range(4):                                     # grip wraps
        d.line([(cx - W * 0.05, H * (0.125 + k * 0.035)), (cx + W * 0.05, H * (0.14 + k * 0.035))], fill=(255, 255, 255, 255), width=int(1.4 * ss))
    d.ellipse([cx - W * 0.075, H * 0.045, cx + W * 0.075, H * 0.045 + W * 0.15], outline=WHITE, width=int(2.4 * ss))
    d.ellipse([cx - W * 0.03, H * 0.066, cx + W * 0.03, H * 0.066 + W * 0.06], fill=WHITE)
    # the balance: a beam with a pivot, and two hanging pans
    by = H * 0.285
    d.rectangle([W * 0.07, by - H * 0.008, W * 0.93, by + H * 0.008], fill=WHITE)
    d.polygon([(cx, by - H * 0.032), (cx + W * 0.07, by), (cx, by + H * 0.012), (cx - W * 0.07, by)], fill=WHITE)
    for sx in (-1, 1):
        ex = cx + sx * W * 0.43
        d.ellipse([ex - W * 0.025, by - H * 0.012, ex + W * 0.025, by + H * 0.012], fill=WHITE)
        pan_y = by + H * 0.105
        d.line([(ex, by), (ex - W * 0.13, pan_y)], fill=WHITE, width=int(1.3 * ss))
        d.line([(ex, by), (ex + W * 0.13, pan_y)], fill=WHITE, width=int(1.3 * ss))
        d.pieslice([ex - W * 0.15, pan_y - H * 0.045, ex + W * 0.15, pan_y + H * 0.045], 0, 180, fill=WHITE)
    # the blade: a wide guillotine blade, bevelled, pointing down
    d.polygon([(cx - W * 0.115, by + H * 0.02), (cx + W * 0.115, by + H * 0.02), (cx + W * 0.095, H * 0.80), (cx, H * 0.985), (cx - W * 0.095, H * 0.80)], fill=WHITE)
    d.line([(cx, by + H * 0.06), (cx, H * 0.90)], fill=(255, 255, 255, 0), width=int(2.2 * ss))        # the fuller is cut out
    big = im
    # the fuller as a lowered-alpha stripe (PIL line with alpha 0 does not erase on RGBA, so it is done on the array)
    arr = np.asarray(big, dtype=np.float32)
    a = arr[..., 3] / 255.0
    xs = np.arange(W, dtype=np.float32)[None, :]
    ys = np.arange(H, dtype=np.float32)[:, None]
    fuller = np.exp(-(((xs - cx) / (W * 0.016)) ** 2)) * smooth(by + H * 0.07, by + H * 0.11, ys) * (1 - smooth(H * 0.80, H * 0.93, ys))
    a = a * (1 - 0.55 * fuller)
    small = Image.fromarray((np.clip(a, 0, 1) * 255).astype(np.uint8), "L").resize((w, h), Image.LANCZOS)
    arr_s = np.asarray(small, dtype=np.float32) / 255.0
    bloom = np.asarray(small.filter(ImageFilter.GaussianBlur(2.6)), dtype=np.float32) / 255.0
    save(from_alpha(np.clip(arr_s + bloom * 0.5, 0, 1)), "judgment_blade")


if __name__ == "__main__":
    ring()
    wave()
    sigil()
    clock()
    glyphs()
    flare()
    mote()
    needle()
    beam()
    shaft()
    crack()
    cracks_ground()
    slash()
    blade()

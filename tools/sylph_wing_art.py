"""Painter of the Sylph's dragonfly wings, second version (docs/magic/sylph.md, section "The wings"): long, narrow, tapered insect wings after the owner's pictures (the small form of pictures 119 / 120:
pale yellow-green membrane with a pastel rainbow sheen; the powered form of picture 121: a stronger green membrane with a clear main vein and many fine cross veins that make small cells).

One wing is ONE picture (root at the left, tip at the right, the leading edge on top); the generator cuts it into a few cards along its length (a bone each), so the silhouette is a cut-out (alpha) of the
card and the curve of the wing is the hinge of the cards. Three things come out of it:

    wing_base(...)        the membrane: pale translucent colour with a static pastel sheen, the vein network (a main vein along the leading edge, long veins out of the root that fork near the margin,
                          a dark pterostigma near the tip, fine cross veins in staggered rows) and a subtle darker outline
    wing_sheen(..., k)    the SHEEN, phase k of K: an additive hue-shifting gradient (pastel rainbow bands) masked by the silhouette, black (zero) under every transparent texel. The K phases are stacked in one
                          sheet and the client picks and blends two of them by shifting the V coordinate, so the sheen sweeps slowly over the membrane (client/prop/SylphDraw)
    SPECS                 the numbers of each wing (length, chord, where it is widest ...), the same table the generator uses to place the cards

Everything is deterministic (seeded) and made from nothing; the lines are drawn at 4x and reduced.
"""
import colorsys
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import sylph_art as A  # noqa: E402

K = 6                                                      # phases of the sheen sheet
SS = A.SS

# wu x hu: the size of the picture in model units; maxw: the widest chord; peak: where along the wing it is widest; bend / hook: the S-curve of the leading edge and the hook of the tip;
# nlong: long veins (besides the main vein); seg: where the generator cuts the wing into cards (fractions of the length)
SPECS = {
    ("small", "fore"): dict(wu=30.0, ppu=20, maxw=7.4, peak=0.50, bend=0.05, hook=0.03, nlong=6, seed=5, cuts=(0.0, 0.24, 0.50, 0.76, 1.0)),
    ("small", "hind"): dict(wu=21.0, ppu=20, maxw=5.8, peak=0.48, bend=0.05, hook=0.03, nlong=5, seed=6, cuts=(0.0, 0.26, 0.52, 0.78, 1.0)),
    ("powered", "fore"): dict(wu=54.0, ppu=12, maxw=14.0, peak=0.50, bend=0.05, hook=0.03, nlong=8, seed=15, cuts=(0.0, 0.20, 0.40, 0.60, 0.80, 1.0)),
    ("powered", "hind"): dict(wu=36.0, ppu=12, maxw=10.0, peak=0.48, bend=0.05, hook=0.03, nlong=7, seed=16, cuts=(0.0, 0.22, 0.44, 0.66, 0.84, 1.0)),
}

# membrane colours: (base, rainbow weight, alpha, vein colour, vein edge colour, sheen strength)
TONE = {"small": dict(base=(238, 247, 178), rainbow=0.55, alpha=0.34, vein=(255, 255, 244), vein_a=0.55, edge=(112, 150, 118), edge_a=0.40, sheen=0.60),
        "powered": dict(base=(92, 208, 96), rainbow=0.22, alpha=0.62, vein=(226, 255, 176), vein_a=0.92, edge=(30, 112, 58), edge_a=0.80, sheen=0.55)}


def _smooth(x):
    x = np.clip(x, 0.0, 1.0)
    return x * x * (3 - 2 * x)


def profile(u, peak):
    """The width of the wing along its length (0..1): a rounded base that grows to its widest at `peak`, then tapers to a point."""
    u = np.asarray(u, np.float64)
    a = np.sin(np.clip(u / peak, 0, 1) * math.pi / 2) ** 1.25
    b = np.cos(np.clip((u - peak) / (1 - peak), 0, 1) * math.pi / 2) ** 0.8
    return np.where(u < peak, a, b)


def edges(spec, n=400):
    """(u, lead, trail): the leading and the trailing edge in picture units (y down) at n points along the wing. The wing is an S: the middle sags towards the trailing side, the tip hooks
    back up to the leading side; the trailing edge is the fuller one (the belly)."""
    u = np.linspace(0.0, 1.0, n)
    mw = spec["maxw"]
    w = mw * profile(u, spec["peak"])
    # the leading edge is a nearly straight line with a gentle S and a hook at the tip; the trailing edge is the leading edge plus the width (the belly)
    lead = mw * (spec["bend"] * np.sin(2 * math.pi * u * 0.9) - spec["hook"] * u ** 3.0)
    trail = lead + w
    y0 = _MARGIN - lead.min()
    return u, lead + y0, trail + y0


_MARGIN = 0.45                                               # units of empty card around the wing


def _fit(spec):
    """Fills in the card height `hu` (the wing's extent plus a margin, a whole number of pixels) and `root_y` (the picture row of the root, where the bone pivot sits)."""
    u, lead, trail = edges(spec)
    spec["root_y"] = float(lead[0])
    spec["hu"] = math.ceil((trail.max() + _MARGIN) * spec["ppu"]) / float(spec["ppu"])


for _s in SPECS.values():
    _fit(_s)


def _rng(seed):
    return np.random.default_rng(seed)


def pastel(h, s=0.45, v=1.0):
    r, g, b = colorsys.hsv_to_rgb(h % 1.0, s, v)
    return np.array([r * 255, g * 255, b * 255], np.float32)


def _hsv_to_rgb(h, s, v):
    """Vectorised hsv (all in 0..1) -> rgb (0..1) arrays."""
    h = (h % 1.0) * 6.0
    i = np.floor(h).astype(np.int32) % 6
    f = h - np.floor(h)
    p, q, t = v * (1 - s), v * (1 - s * f), v * (1 - s * (1 - f))
    r = np.choose(i, [v, q, p, p, t, v])
    g = np.choose(i, [t, v, v, q, p, p])
    b = np.choose(i, [p, p, t, v, v, q])
    return np.stack([r, g, b], -1)


class Wing:
    """One wing picture: masks, vein network and the gradients, computed once; wing_base() and wing_sheen() read it."""

    def __init__(self, form, kind):
        self.form, self.kind = form, kind
        self.spec = s = SPECS[(form, kind)]
        self.tone = TONE[form]
        wu, hu, ppu = s["wu"], s["hu"], s["ppu"]
        self.W, self.H = int(round(wu * ppu)), int(round(hu * ppu))
        u, lead, trail = edges(s)
        self.u, self.lead, self.trail = u, lead, trail
        assert lead.min() > 0.25 and trail.max() < hu - 0.25, "the wing does not fit its card: %s %s" % (lead.min(), trail.max())
        rng = _rng(s["seed"])
        self.L = wu * 0.985
        # silhouette
        pts = [(self.L * ui, li) for ui, li in zip(u, lead)] + [(self.L * ui, ti) for ui, ti in zip(u[::-1], trail[::-1])]
        cv = A.Canvas(wu, hu, ppu)
        cv.poly(pts, (255, 255, 255, 255))
        self.mask = np.asarray(cv.done())[..., 3].astype(np.float32) / 255.0
        # per pixel coordinates: u along the wing, f from the leading (0) to the trailing (1) edge
        yy, xx = np.mgrid[0:self.H, 0:self.W].astype(np.float32)
        self.xu = (xx + 0.5) / ppu / self.L
        ux = np.clip(self.xu[0], 0, 1)
        ld = np.interp(ux, u, lead).astype(np.float32)
        tr = np.interp(ux, u, trail).astype(np.float32)
        self.f = np.clip(((yy + 0.5) / ppu - ld[None, :]) / np.maximum(tr - ld, 1e-3)[None, :], 0, 1)
        self._veins(rng)
        self._tint(rng)

    # ------------------------------------------------------------------------------------------------ the vein network
    def P(self, u, f):
        """Picture position (units) of the wing point (u along, f across)."""
        l = float(np.interp(u, self.u, self.lead))
        t = float(np.interp(u, self.u, self.trail))
        return (self.L * u, l + f * (t - l))

    def _line(self, cv, us, f, width, color):
        cv.curve([self.P(u, f(u) if callable(f) else f) for u in us], color, width, n=10)

    def _veins(self, rng):
        s, W, H = self.spec, self.W, self.H
        wu, hu, ppu = s["wu"], s["hu"], s["ppu"]
        powered = self.form == "powered"
        unit = 1.0 if not powered else 1.5                  # the powered wing is drawn at 12 px per unit: its lines are wider in units so they stay about as thick in pixels
        long_cv = A.Canvas(wu, hu, ppu)
        cross_cv = A.Canvas(wu, hu, ppu)
        main_cv = A.Canvas(wu, hu, ppu)
        stig_cv = A.Canvas(wu, hu, ppu)
        n = s["nlong"]
        # the long veins: f positions across the wing (0 = leading edge); each starts at the root and ends near the margin; some fork
        fs = [0.075 + 0.84 * (i + 0.0) / max(1, n - 1) for i in range(n)]
        fs[0] = 0.08
        ends = [0.93 - 0.02 * (i % 3) for i in range(n)]
        starts = [0.02 + 0.012 * i for i in range(n)]
        veins = []                                          # (f of the vein as a function of u, u start, u end)
        for i in range(n):
            f0 = fs[i]
            drift = (rng.uniform(-0.03, 0.03))
            veins.append(((lambda u, f0=f0, drift=drift: f0 + drift * math.sin(u * math.pi)), starts[i], ends[i]))
        forks = []
        for i in range(1, n):
            if i % 2 == 1:                                  # fork: a branch leaves the vein at u0 and runs to the margin between this vein and the next
                u0 = 0.50 + 0.04 * (i % 3)
                f_a = fs[i]
                f_b = fs[i + 1] if i + 1 < n else 1.0
                forks.append(((lambda u, f_a=f_a, f_b=f_b, u0=u0: f_a + (f_b - f_a) * 0.5 * _smooth((u - u0) / 0.30)), u0, 0.96))
        thick = 0.075 * unit
        for fn, a, b in veins + forks:
            us = np.linspace(a, b, 28)
            self._line(long_cv, us, fn, thick, (255, 255, 255, 255))
        # the main vein: along the leading edge, thick at the root, thin at the tip, plus the sub-costa just under it
        us = np.linspace(0.015, 0.985, 60)
        pts = [self.P(u, 0.0) for u in us]
        for j in range(len(pts) - 1):
            wd = (0.20 - 0.11 * us[j]) * unit
            main_cv.line([pts[j], pts[j + 1]], (255, 255, 255, 255), wd)
        # pterostigma: a small dark cell on the leading edge near the tip
        u0, u1 = 0.74, 0.80
        quad = [self.P(u0, 0.0), self.P(u1 + 0.012, 0.0), self.P(u1, 0.13), self.P(u0 - 0.012, 0.13)]
        stig_cv.poly(quad, (255, 255, 255, 255))
        # cross veins: short links between neighbouring long veins in staggered rows; cells are about square, so the spacing follows the chord
        allv = sorted(veins, key=lambda v: v[0](0.5))
        order = [v for v in allv]
        cthick = 0.050 * unit
        for i in range(len(order) - 1):
            fa, a0, a1 = order[i]
            fb, b0, b1 = order[i + 1]
            lo, hi = max(a0, b0) + 0.04, min(a1, b1) - 0.005
            u = lo + rng.uniform(0.0, 0.03)
            while u < hi:
                gap = (fb(u) - fa(u)) * float(np.interp(u, self.u, self.trail) - np.interp(u, self.u, self.lead))
                step = max(0.022, min(0.075, gap / self.L * 1.05))
                slant = rng.uniform(-0.006, 0.014)
                cross_cv.line([self.P(u, fa(u)), self.P(u + slant, fb(u + slant))], (255, 255, 255, 255), cthick, False)
                u += step * rng.uniform(0.85, 1.2)
        # the marginal ring: a thin vein just inside the trailing edge, with tiny links to the last long vein
        mu = np.linspace(0.30, 0.985, 40)
        self._line(cross_cv, mu, lambda u: 0.965 - 0.02 * u, 0.04 * unit, (255, 255, 255, 255))
        for u in np.arange(0.34, 0.95, 0.032):
            cross_cv.line([self.P(u, 0.965 - 0.02 * u), self.P(u + 0.008, 0.90)], (255, 255, 255, 255), 0.035 * unit, False)
        g = lambda c: np.asarray(c.done())[..., 3].astype(np.float32) / 255.0
        self.long, self.cross, self.main, self.stig = g(long_cv), g(cross_cv), g(main_cv), g(stig_cv)
        out = A.Canvas(wu, hu, ppu)
        edge = [self.P(u, 0.0) for u in np.linspace(0, 1, 90)] + [self.P(u, 1.0) for u in np.linspace(1, 0, 90)]
        out.line(edge + [edge[0]], (255, 255, 255, 255), 0.09 * unit)
        self.outline = g(out) * (self.mask > 0.2)
        for a in ("long", "cross", "main", "stig"):
            setattr(self, a, getattr(self, a) * (self.mask > 0.05))

    def _tint(self, rng):
        """The smooth fields of the sheen: hue and strength over the wing."""
        ph = rng.uniform(0, 1, 3)
        self.hue_field = self.xu * 1.15 + self.f * 0.55 + 0.08 * np.sin(self.xu * 9.0 + ph[0] * 6.28)
        self.band = 0.55 + 0.45 * np.cos(math.tau * (self.xu * 1.4 - self.f * 0.35 + ph[1]))

    # ------------------------------------------------------------------------------------------------ the pictures
    def base(self):
        t = self.tone
        base = np.array(t["base"], np.float32)
        m = self.mask
        rainbow = _hsv_to_rgb(self.hue_field + 0.12, np.full_like(self.f, 0.36), np.full_like(self.f, 1.0)) * 255.0
        col = base[None, None] * (1 - t["rainbow"]) + rainbow * t["rainbow"]
        # the leading half is the denser one, the trailing margin is clearer; a slightly stronger tone near the root
        dens = 1.0 - 0.34 * _smooth(self.f) - 0.10 * _smooth(self.xu)
        if self.form == "powered":
            col = col * (0.82 + 0.18 * (1 - self.f)[..., None])
        alpha = t["alpha"] * dens * (0.88 + 0.22 * self.band)
        out = np.zeros((self.H, self.W, 4), np.float32)
        out[..., :3] = col
        out[..., 3] = alpha * 255.0
        # the cells: a faint tint inside every cell is not drawn (the veins are); the veins on top: dark edge first, then the light core
        ve = np.clip(self.long * 0.95 + self.cross * 0.75, 0, 1)
        rim = np.clip(self.outline + self.main, 0, 1)
        edge = np.array(t["edge"], np.float32)
        vein = np.array(t["vein"], np.float32)
        halo = np.clip(_blur(ve, 1.3) * 1.6, 0, 1) * (1 - ve)        # the thin dark edge beside a vein
        out = _over(out, edge, halo * t["edge_a"] * 0.55 * self.mask)
        out = _over(out, vein, ve * t["vein_a"] * self.mask)
        out = _over(out, edge, rim * t["edge_a"] * self.mask)
        out = _over(out, vein, self.main * 0.55 * self.mask)
        out = _over(out, np.array([112, 98, 52] if self.form == "small" else [30, 96, 40], np.float32), self.stig * 0.85 * self.mask)
        out[..., 3] *= (self.mask > 0.02)
        out[..., :3] *= (out[..., 3:4] > 0)
        return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")

    def sheen(self, k):
        """Phase k of the sheen (additive; RGB straight, zero under alpha 0): pastel rainbow bands whose hue and brightness shift with k / K, strongest on the veins and in the broad middle."""
        t = self.tone
        ph = k / float(K)
        hue = self.hue_field - ph
        rgb = _hsv_to_rgb(hue, np.full_like(self.f, 0.52), np.full_like(self.f, 1.0))
        glint = 0.50 + 0.50 * np.cos(math.pi * (self.xu * 1.1 + self.f * 0.45 - ph)) ** 2          # a bright band that sweeps along the wing as k grows
        a = t["sheen"] * (0.35 + 0.65 * self.band) * glint
        a = a * (0.55 + 0.45 * (1 - 0.6 * self.f)) * self.mask
        veins = np.clip(self.long + self.cross * 0.7 + self.main * 0.8, 0, 1)
        a = np.clip(a * (0.65 + 0.8 * veins), 0, 0.95)
        out = np.zeros((self.H, self.W, 4), np.float32)
        out[..., :3] = rgb * 255.0
        out[..., 3] = a * 255.0
        out[..., :3] *= (a > 0.004)[..., None]
        out[..., 3] *= (a > 0.004)
        return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def _blur(a, r):
    from PIL import ImageFilter
    im = Image.fromarray(np.clip(a * 255, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im).astype(np.float32) / 255.0


def _over(dst, rgb, a):
    """Composite the colour rgb with coverage a (HxW, 0..1) over the straight-alpha picture dst (HxWx4, 0..255)."""
    da = dst[..., 3:4] / 255.0
    sa = a[..., None]
    oa = sa + da * (1 - sa)
    num = rgb[None, None] * sa + dst[..., :3] * da * (1 - sa)
    col = np.where(oa > 1e-5, num / np.maximum(oa, 1e-5), 0)
    return np.concatenate([col, oa * 255.0], -1)


_CACHE = {}


def wing(form, kind):
    key = (form, kind)
    if key not in _CACHE:
        _CACHE[key] = Wing(form, kind)
    return _CACHE[key]


if __name__ == "__main__":
    out = sys.argv[1] if len(sys.argv) > 1 else "."
    for form in ("small", "powered"):
        for kind in ("fore", "hind"):
            w = wing(form, kind)
            bg = Image.new("RGBA", (w.W, w.H), (90, 120, 150, 255))
            b = Image.alpha_composite(bg, w.base())
            s = w.sheen(2)
            sh = np.asarray(s).astype(np.float32)
            comp = np.asarray(b).astype(np.float32)
            comp[..., :3] = np.clip(comp[..., :3] + sh[..., :3] * (sh[..., 3:4] / 255.0), 0, 255)
            Image.fromarray(comp.astype(np.uint8), "RGBA").save(os.path.join(out, "wing2_%s_%s.png" % (form, kind)))
            print(form, kind, w.W, w.H)

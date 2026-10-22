"""The hand-painted sheets of the Undine (Noelle Silva's water Spirit Lord, the Black Clover wiki's Undine art: a tall woman of water with very pale skin, a calm face with pale blue eyes, fin
ears, enormous wavy pale-blue hair that streams out in big scalloped ribbons, and a strapless gown of blue water with foam stripes). Deterministic (every painter is seeded). Colours and
shapes are read off the two stills of the request (images 122 and 123); the look is the anime cel look: flat colours, one darker shade, white foam streaks, thin dark linework, light hatching.

The painters of the skin, face and gown are in undine_art_body.py, the fx / wave painters in undine_art_fx.py; this file has the hair, the region tables and the dispatch used by
tools/gen_undine_geo.py. Run it alone with --sheet FILE to write a contact sheet of the body regions.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import undine_art_lib as A  # noqa: E402
import undine_art_body as B  # noqa: E402
import undine_art_fx as F  # noqa: E402
from undine_art_lib import SS, col, canvas, mask_canvas, done, fill_mask, blur, shifted, minus, torn, smooth_ribbon  # noqa: E402

TAU = math.tau


def mul(a, b):
    return ImageChops.multiply(a, b)


# ====================================================================================================================== hair ribbons
def _curl(d, cx, cy, r0, turns, direction, n=24):
    """A curl at the end of a ribbon: discs whose size shrinks along a spiral."""
    for i in range(n):
        t = i / (n - 1.0)
        a = direction * t * turns * TAU
        rr = r0 * (1 - 0.8 * t)
        rad = r0 * 0.62 * (1 - 0.85 * t) + 0.6 * SS
        d.ellipse([cx + math.cos(a) * rr - rad, cy + math.sin(a) * rr - rad, cx + math.cos(a) * rr + rad, cy + math.sin(a) * rr + rad], fill=255)


def _shade_hair(img, mask, w, h, rng, centre_fn, strands=9, crescent=1.0):
    """The cel shading of a hair shape: the darker underside crescent, a thin darker rim, a light rim on top, strand lines clipped to the shape (centre_fn(t) -> (x, y) is the flow line)."""
    fill_mask(img, mask, col(A.HAIR))
    h = min(w, h) if h > w else h                                                   # the shade's size follows the thinner side
    cres = minus(mask, shifted(mask, -h * 0.03 * crescent, -h * 0.07 * crescent))
    fill_mask(img, cres, col(A.HAIR_SHADE))
    rim = minus(cres, shifted(cres, -h * 0.012, -h * 0.022))
    fill_mask(img, mul(rim, cres), col(A.HAIR_SHADE2))
    hi = minus(mask, shifted(mask, h * 0.012, h * 0.03))
    fill_mask(img, hi, col(A.HAIR_LIGHT))
    lines = canvas(w, h)
    ld = ImageDraw.Draw(lines)
    for k in range(strands):
        off = rng.uniform(-1.0, 1.0)
        u0 = rng.uniform(0.0, 0.3)
        u1 = rng.uniform(0.55, 0.98)
        m = 26
        pts = []
        for i in range(m):
            t = u0 + (u1 - u0) * i / (m - 1.0)
            x, y = centre_fn(t, off)
            pts.append((x * SS, (y + math.sin(i * 0.8 + k) * h * 0.006) * SS))
        ld.line(pts, fill=col(A.HAIR_LIGHT if k % 3 else A.HAIR_SHADE), width=int(SS * (1.8 if k % 3 else 2.4)))
    lines.putalpha(mul(lines.split()[3], mask))
    img.alpha_composite(lines)


def hair_ribbon(w, h, seed, curl=1.0, branch=1, thick=0.15, waves=1.5):
    """One streaming hair ribbon, the root on the left and the tip on the right: a wavy band whose edges are lobed like a cloud (discs along a path), a curl at the tip and a finger or two that
    leave it part-way, the underside shaded darker blue, strand lines in white and blue."""
    rng = np.random.default_rng(seed)
    S_ = SS
    mask = mask_canvas(w, h)
    d = ImageDraw.Draw(mask)
    n = 60
    x_end = 0.80
    xs = np.linspace(0.02 * w, x_end * w, n)
    tt = xs / w
    a1 = h * rng.uniform(0.15, 0.22)
    p1 = rng.uniform(0, TAU)
    yc = h * 0.5 + a1 * np.sin(tt / x_end * waves * TAU + p1) * (0.45 + 0.7 * tt / x_end) + h * 0.03 * np.sin(tt * 19.0 + rng.uniform(0, TAU))
    prof = (0.7 + 0.3 * np.sin(np.clip(tt / 0.18, 0, 1) * math.pi / 2)) * (1.0 - 0.45 * np.clip((tt / x_end - 0.5) / 0.5, 0, 1) ** 1.5)
    th = h * thick * prof
    rad = th * rng.uniform(0.94, 1.12, n)
    for x, y, r in zip(xs, yc, rad):
        d.ellipse([(x - r) * S_, (y - r) * S_, (x + r) * S_, (y + r) * S_], fill=255)
    for i in range(3, n - 5, 6):                                                    # bigger lobes on the upper edge
        r = rad[i] * rng.uniform(0.62, 0.85)
        off = th[i] * 0.80
        d.ellipse([(xs[i] - r) * S_, (yc[i] - off - r) * S_, (xs[i] + r) * S_, (yc[i] - off + r) * S_], fill=255)
    for i in range(5, n - 6, 8):                                                    # smaller ones under
        r = rad[i] * rng.uniform(0.4, 0.6)
        off = th[i] * 0.88
        d.ellipse([(xs[i] - r) * S_, (yc[i] + off - r) * S_, (xs[i] + r) * S_, (yc[i] + off + r) * S_], fill=255)
    ex, ey = xs[-1], yc[-1]
    direc = 1 if rng.random() < 0.5 else -1
    _curl(d, (ex + h * 0.02) * S_, (ey + direc * h * 0.14) * S_, h * 0.2 * curl * S_, 0.9, direc)
    for b in range(branch):                                                         # a finger that leaves the band part-way and ends in a small curl
        i0 = int(n * rng.uniform(0.40, 0.62))
        bx, by = xs[i0], yc[i0]
        ang = rng.uniform(0.40, 0.80) * (1 if b % 2 == 0 else -1)
        ln = w * rng.uniform(0.14, 0.20)
        last = None
        for k in range(14):
            u = k / 13.0
            px = bx + math.cos(ang * (1 - 0.45 * u)) * ln * u
            py = by + math.sin(ang * (1 - 0.45 * u)) * ln * u + math.sin(u * 5.0) * h * 0.03
            r = th[min(n - 1, i0 + 3)] * (0.85 - 0.6 * u)
            d.ellipse([(px - r) * S_, (py - r) * S_, (px + r) * S_, (py + r) * S_], fill=255)
            last = (px, py)
        _curl(d, last[0] * S_, (last[1] + (1 if ang > 0 else -1) * h * 0.05) * S_, h * 0.09 * S_, 1.0, -1 if ang > 0 else 1)
    mask = mask.filter(ImageFilter.GaussianBlur(S_ * 1.3)).point(lambda v: 255 if v > 120 else 0)
    img = canvas(w, h)

    def flow(t, off):
        x = xs[0] + (xs[-1] - xs[0]) * t
        return x, np.interp(x, xs, yc) + off * np.interp(x, xs, th) * 0.8
    _shade_hair(img, mask, w, h, rng, flow)
    return done(img, w, h)


def hair_surface(w, h, seed, mode="flow", cut=None):
    """A face of the hair shell (top, back, side or front with its bangs) or a lock: a pale blue field with soft darker bands, white and blue strand lines following the flow, light hatching.
    `cut` is an optional function that returns the mask (L, supersampled) of what is kept (alpha)."""
    rng = np.random.default_rng(seed)
    img = canvas(w, h, col(A.HAIR))
    d = ImageDraw.Draw(img)
    if mode == "top":
        cx, cy = w * 0.5, h * 0.62
        for k in range(34):
            a = (k / 34.0) * TAU
            pts = [(cx + math.cos(a + 0.5 * t) * w * (0.04 + 0.62 * t), cy + math.sin(a + 0.5 * t) * h * (0.04 + 0.62 * t)) for t in np.linspace(0, 1, 18)]
            d.line([pt_ * 1 for pt_ in [(x * SS, y * SS) for x, y in pts]], fill=col(A.HAIR_LIGHT if k % 2 else A.HAIR_SHADE), width=int(SS * 2.4))
        d.line([(cx * SS, 0), (cx * SS, h * SS)], fill=col(A.HAIR_SHADE2), width=int(SS * 4))
    else:
        for k in range(14):                                                           # soft bands
            x0 = (k + rng.uniform(-0.3, 0.3)) / 14.0 * w
            pts = [((x0 + math.sin(t * 5 + k) * w * 0.03) * SS, t * h * SS) for t in np.linspace(0, 1, 30)]
            d.line(pts, fill=col(A.HAIR_SHADE, 70), width=int(SS * w * 0.03))
        for k in range(14):
            x0 = (k + rng.uniform(-0.4, 0.4)) / 14.0 * w
            pts = [((x0 + math.sin(t * 6 + k * 1.7) * w * 0.025) * SS, t * h * SS) for t in np.linspace(0, 1, 30)]
            d.line(pts, fill=col(A.HAIR_LIGHT if k % 3 else A.HAIR_SHADE), width=int(SS * (2.0 if k % 3 else 2.6)))
    if cut is not None:
        img.putalpha(cut(w, h, rng))
    return done(img, w, h)


def cap_front(w, h):
    """The front of the hair shell: a centre parting and the bangs sweeping to both sides, the opening where the face shows left transparent, two swaths framing the cheeks."""
    def cut(w_, h_, rng):
        m = mask_canvas(w_, h_)
        md = ImageDraw.Draw(m)
        md.rectangle([0, 0, w_ * SS, h_ * SS], fill=255)
        cx = w_ / 2.0
        hole = []
        # the opening: the hairline arches from the parting at the top centre out and down to the temples, then the cheeks
        for i in range(41):
            t = i / 40.0
            x = cx - (w_ * 0.5 * 0.80) * math.sin(t * math.pi / 2) ** 0.9
            y = h_ * (0.17 + 0.20 * t ** 1.3) + (h_ * 0.55 * (t ** 3.0) if t > 0.5 else 0)
            hole.append((x, y))
        left = [(x, y) for x, y in hole]
        left += [(w_ * 0.20, h_ * 1.02), (cx, h_ * 1.02)]
        right = [(2 * cx - x, y) for x, y in left[:-2]][::-1]
        poly = left[:-1] + [(cx, h_ * 1.02)] + [(2 * cx - w_ * 0.20, h_ * 1.02)] + right
        md.polygon([(x * SS, y * SS) for x, y in poly], fill=0)
        md.polygon([(cx * SS, h_ * 0.13 * SS), ((cx - w_ * 0.045) * SS, h_ * 0.2 * SS), ((cx + w_ * 0.045) * SS, h_ * 0.2 * SS)], fill=255)
        return m.filter(ImageFilter.GaussianBlur(SS * 1.0)).point(lambda v: 255 if v > 120 else 0)
    return hair_surface(w, h, 411, "flow", cut)


def cap_top(w, h):
    return hair_surface(w, h, 412, "top")


def cap_back(w, h):
    return hair_surface(w, h, 413, "flow")


def cap_side(w, h):
    def cut(w_, h_, rng):
        m = mask_canvas(w_, h_)
        md = ImageDraw.Draw(m)
        md.rectangle([0, 0, w_ * SS, h_ * SS], fill=255)
        md.polygon([(-2, h_ * 0.40 * SS), (w_ * 0.26 * SS, h_ * 0.46 * SS), (w_ * 0.20 * SS, h_ * 1.02 * SS), (-2, h_ * 1.02 * SS)], fill=0)
        return m.filter(ImageFilter.GaussianBlur(SS * 1.0)).point(lambda v: 255 if v > 120 else 0)
    return hair_surface(w, h, 414, "flow", cut)


def lock(w, h):
    """A thin lock of hair that falls in front of the shoulder: a wavy tapered strand with lobed edge."""
    rng = np.random.default_rng(420)
    mask = mask_canvas(w, h)
    d = ImageDraw.Draw(mask)
    n = 60
    ys = np.linspace(0.01 * h, 0.97 * h, n)
    t = ys / h
    xc = w * 0.5 + w * 0.17 * np.sin(t * 2.3 * TAU) * (0.4 + 0.6 * t)
    th = w * (0.34 - 0.2 * t ** 1.4)
    for x, y, r in zip(xc, ys, th):
        d.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(SS * 1.2)).point(lambda v: 255 if v > 120 else 0)
    img = canvas(w, h)

    def flow(tt_, off):
        y = ys[0] + (ys[-1] - ys[0]) * tt_
        return np.interp(y, ys, xc) + off * np.interp(y, ys, th) * 0.8, y
    _shade_hair(img, mask, w, h, rng, lambda t_, o: flow(t_, o), strands=5, crescent=0.5)
    return done(img, w, h)


def back_mass(w, h):
    """The long hair at the back: a broad sheet that flows down behind the body and ends in three wavy lobes."""
    rng = np.random.default_rng(430)
    mask = mask_canvas(w, h)
    d = ImageDraw.Draw(mask)
    for i in range(60):
        t = i / 59.0
        y = h * (0.04 + 0.78 * t)
        half = w * (0.26 + 0.20 * t ** 0.8)
        d.rectangle([(w / 2 - half) * SS, (y - h * 0.02) * SS, (w / 2 + half) * SS, (y + h * 0.02) * SS], fill=255)
    for k in range(3):                                                                # the lobes at the end
        cx = w * (0.22 + 0.28 * k)
        for i in range(30):
            t = i / 29.0
            y = h * (0.78 + 0.2 * t)
            r = w * 0.15 * (1 - t ** 1.5)
            x = cx + math.sin(t * 3.0 + k) * w * 0.04
            d.ellipse([(x - r) * SS, (y - r) * SS, (x + r) * SS, (y + r) * SS], fill=255)
    mask = mask.filter(ImageFilter.GaussianBlur(SS * 2.0)).point(lambda v: 255 if v > 120 else 0)
    img = canvas(w, h)

    def flow(t, off):
        return w * (0.5 + off * 0.42 + 0.03 * math.sin(t * 9 + off * 5)), h * (0.02 + 0.95 * t)
    _shade_hair(img, mask, w, h, rng, flow, strands=24, crescent=0.5)
    return done(img, w, h)


# ====================================================================================================================== the regions of the sheets
BODY_REGIONS = {
    "face": (320, 392), "head_side": (300, 392), "cap_front": (344, 380), "cap_top": (344, 320), "cap_back": (344, 380), "cap_side": (320, 380), "ear": (200, 260),
    "neck": (100, 132), "shoulder_front": (420, 140), "shoulder_back": (420, 140), "arm": (104, 420), "forearm": (92, 392), "hand_palm": (132, 172), "hand_back": (132, 172), "hand_side": (60, 172),
    "fingers": (160, 170), "thumb": (110, 130), "bodice_front": (360, 320), "bodice_back": (360, 320), "bodice_side": (200, 320),
    "rib_a": (672, 200), "rib_b": (560, 180), "rib_c": (480, 160), "rib_d": (420, 250), "rib_e": (360, 160), "rib_f": (520, 180),
    "lock": (96, 520), "back_mass": (400, 640), "skirt_in": (1440, 240),
}
WATER_REGIONS = {"skirt_out": (1536, 380), "shell": (256, 640), "splash": (256, 160), "foam_band": (256, 96), "foam_tear": (192, 64), "droplet": (256, 64)}
FX_REGIONS = {"streak": (96, 384), "glint": (96, 96), "glow": (96, 96)}
BASE_REGIONS = {"wave_a": (512, 240), "wave_b": (512, 224), "wave_c": (512, 192)}

def regions(lod):
    """The regions of a level of detail's sheet: the full list at 1, 1/2 and 1/4 of the size (the lowest level leaves out what it does not draw)."""
    k = {0: 1.0, 1: 0.5, 2: 0.25}[lod]
    skip = {2: {"lock", "fingers", "thumb", "hand_palm", "hand_back", "ear", "rib_d", "rib_e", "rib_f", "shoulder_back", "neck"}}.get(lod, set())
    out = {}
    for name, (w, h) in BODY_REGIONS.items():
        if name in skip:
            continue
        out[name] = (max(8, int(round(w * k / 4.0)) * 4) if k < 1 else w, max(8, int(round(h * k / 4.0)) * 4) if k < 1 else h)
    if lod == 1:                                                                    # a skirt of 12 strips of 40 texels each, and the outer layer a strip for each two
        out["skirt_in"] = (12 * 56, 128)
    if lod == 2:
        out["skirt_in"] = (8 * 40, 128)
    return out


FACE_KEYS = ("smile", "sad", "stern", "angry", "surprised", "serene")
FACE_SHEET = (320, 392)                                                                   # one expression per texture


def face_overlay(key, w, h):
    """An expression drawn over the calm face: the painted face of the expression, kept only inside a soft window around the eyes, brows and mouth, and only where the hair of the front shell leaves the
    face open (the sheet is drawn after the body, so it must not paint over the bangs). The skin under it is the same, so the edge of the window does not show."""
    face = B.face_expr(w, h, key).astype(np.float32)
    cap = cap_front(344, 380)[..., 3]
    ys, xs = np.mgrid[0:h, 0:w]
    u, v = (xs + 0.5) / w, (ys + 0.5) / h
    uc = np.clip(((0.4 + 7.4 * u) / 8.2 * 344).astype(int), 0, 343)
    vc = np.clip(((0.7 + 9.0 * v) / 9.8 * 380).astype(int), 0, 379)
    open_ = 1.0 - cap[vc, uc] / 255.0
    win = np.clip(np.minimum.reduce([(u - 0.08) / 0.10, (0.92 - u) / 0.10, (v - 0.22) / 0.07, (0.86 - v) / 0.07]), 0.0, 1.0)
    out = face.copy()
    out[..., 3] = face[..., 3] * win * open_
    out[out[..., 3] <= 0] = 0
    return out


PAINTERS = {
    "face": B.face, "head_side": B.head_side, "ear": B.ear, "neck": B.neck, "shoulder_front": B.shoulder_front, "shoulder_back": B.shoulder_back, "arm": B.arm, "forearm": B.forearm,
    "hand_palm": B.hand_palm, "hand_back": B.hand_back, "hand_side": B.hand_side, "fingers": B.fingers, "thumb": B.thumb,
    "bodice_front": B.bodice_front, "bodice_back": B.bodice_back, "bodice_side": B.bodice_side, "skirt_in": B.skirt_in, "skirt_out": B.skirt_out,
    "shell": F.shell, "splash": F.splash, "foam_band": F.foam_band, "foam_tear": F.foam_tear, "droplet": F.droplet, "streak": F.streak, "glint": F.glint, "glow": F.glow, "ring": F.ring,
    "wave_a": F.wave_a, "wave_b": F.wave_b, "wave_c": F.wave_c,
    "cap_front": cap_front, "cap_top": cap_top, "cap_back": cap_back, "cap_side": cap_side, "lock": lock, "back_mass": back_mass,
    "rib_a": lambda w, h: hair_ribbon(w, h, 11, 1.0, 1, 0.22, 1.3),
    "rib_b": lambda w, h: hair_ribbon(w, h, 12, 1.1, 2, 0.23, 1.2),
    "rib_c": lambda w, h: hair_ribbon(w, h, 13, 1.3, 0, 0.24, 1.1),
    "rib_d": lambda w, h: hair_ribbon(w, h, 14, 1.4, 1, 0.24, 0.9),
    "rib_e": lambda w, h: hair_ribbon(w, h, 15, 1.2, 0, 0.25, 1.0),
    "rib_f": lambda w, h: hair_ribbon(w, h, 16, 1.0, 1, 0.21, 1.4),
}


def paint(name, w, h):
    fn = PAINTERS.get(name)
    if fn is None:
        raise KeyError("no painter for " + name)
    return fn(w, h)


# ====================================================================================================================== contact sheet (for looking at the regions)
def contact(regions, path, cols=3, bg=(88, 96, 112)):
    pad = 8
    tiles = []
    for name, arr in regions:
        tiles.append((name, Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")))
    rows = (len(tiles) + cols - 1) // cols
    cw = max(t[1].width for t in tiles) + pad
    ch = max(t[1].height for t in tiles) + pad + 14
    sheet = Image.new("RGBA", (cols * cw + pad, rows * ch + pad), bg + (255,))
    dd = ImageDraw.Draw(sheet)
    for i, (name, im) in enumerate(tiles):
        x, y = pad + (i % cols) * cw, pad + (i // cols) * ch
        sheet.alpha_composite(im, (x, y + 12))
        dd.text((x, y), name, fill=(255, 255, 255, 255))
    sheet.convert("RGB").save(path)


if __name__ == "__main__":
    out = sys.argv[sys.argv.index("--sheet") + 1] if "--sheet" in sys.argv else None
    names = [a for a in sys.argv[1:] if a in BODY_REGIONS]
    if out:
        regs = [(n, paint(n, *BODY_REGIONS[n])) for n in (names or ["face", "cap_front", "rib_a", "rib_b"])]
        contact(regs, out, cols=2)

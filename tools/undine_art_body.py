"""Painters of the Undine's skin, face, hands, bodice and gown (tools/gen_undine_art.py gathers them): the anime cel look of the Black Clover stills, flat colours with one darker shade, white foam
streaks, thin dark linework and light hatching. Every function paints one region of a sheet and returns straight-alpha RGBA floats of (h, w)."""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import undine_art_lib as A  # noqa: E402
from undine_art_lib import SS, SK, col, canvas, mask_canvas, done, fill_mask, blur, shifted, minus, torn, smooth_ribbon, wave_pts, hatch  # noqa: E402

TAU = math.tau


def S(v):
    return int(round(v * SS))


def pt(x, y):
    return (x * SS, y * SS)


# ====================================================================================================================== the face
def _eye(d, cx, cy, w, h, flip, mode="open", iris=1.0):
    """One calm almond eye with a pale blue iris, a thin upper lid with a flick, thin lower lash, and two highlights. Modes: open, wide, narrow, happy (the lower lid lifts into a smile), sad (the outer
    corner droops, the eye is wet), closed (a curve with lashes)."""
    sx = -1 if flip else 1
    if mode == "closed":
        pts = [pt(cx - sx * w * 0.5 + sx * w * i / 20.0, cy + h * 0.18 * math.sin(math.pi * i / 20.0) ** 0.9 - h * 0.05) for i in range(21)]
        smooth_ribbon(d, pts, [h * (0.07 + 0.10 * math.sin(math.pi * (0.1 + 0.8 * i / 20.0))) * SS for i in range(21)], col("#2C303E"))
        for k in range(3):                                                         # three short lashes at the outer end
            ox, oy = cx + sx * w * (0.30 + 0.07 * k), cy + h * 0.10
            smooth_ribbon(d, [pt(ox, oy), pt(ox + sx * w * 0.05, oy + h * (0.22 + 0.03 * k))], [h * 0.05 * SS, h * 0.02 * SS], col("#2C303E"))
        return
    droop = h * 0.16 if mode == "sad" else 0.0
    lid_top = [(cx - sx * w * 0.5 + sx * w * i / 24.0, cy - h * 0.55 * math.sin(math.pi * (0.04 + 0.92 * i / 24.0)) ** 0.8 + (h * 0.1 if i > 18 else 0) + droop * (i / 24.0)) for i in range(25)]
    low_lift = h * 0.30 if mode == "happy" else 0.0
    low = [(cx + sx * w * 0.5 - sx * w * i / 24.0, cy + h * 0.38 * math.sin(math.pi * (0.04 + 0.92 * i / 24.0)) ** 1.0 - low_lift * math.sin(math.pi * i / 24.0) + droop * (1 - i / 24.0)) for i in range(25)]
    d.polygon([pt(x, y) for x, y in lid_top + low], fill=col("#F8FAFF"))
    ir = h * 0.70 * iris
    ix, iy = cx + sx * w * 0.02, cy + h * 0.02 + (droop * 0.4 if mode == "sad" else 0.0)
    d.ellipse([S(ix - ir), S(iy - ir * 1.12), S(ix + ir), S(iy + ir * 1.12)], fill=col(A.IRIS))
    d.ellipse([S(ix - ir * 0.9), S(iy - ir * 1.0), S(ix + ir * 0.9), S(iy + ir * 0.1)], fill=col(A.IRIS_DARK))
    d.ellipse([S(ix - ir * 0.78), S(iy - ir * 0.55), S(ix + ir * 0.78), S(iy + ir * 1.02)], fill=col("#B9D9EE"))
    d.ellipse([S(ix - ir * 0.30), S(iy - ir * 0.30), S(ix + ir * 0.30), S(iy + ir * 0.32)], fill=col("#34465A"))
    d.ellipse([S(ix - ir * 0.52), S(iy - ir * 0.62), S(ix - ir * 0.14), S(iy - ir * 0.24)], fill=col("#FFFFFF"))
    d.ellipse([S(ix + ir * 0.2), S(iy + ir * 0.28), S(ix + ir * 0.42), S(iy + ir * 0.5)], fill=col("#E8F6FF"))
    if mode == "sad":                                                              # a wet eye: a bigger glint, a tear on the lower lid
        d.ellipse([S(ix - ir * 0.55), S(iy - ir * 0.70), S(ix + ir * 0.05), S(iy - ir * 0.10)], fill=col("#FFFFFF"))
        tx, ty = cx + sx * w * 0.38, cy + h * 0.62
        d.polygon([pt(tx, ty - h * 0.30), pt(tx - h * 0.14, ty + h * 0.05), pt(tx + h * 0.14, ty + h * 0.05)], fill=col("#CFE8F8"))
        d.ellipse([S(tx - h * 0.14), S(ty - h * 0.08), S(tx + h * 0.14), S(ty + h * 0.2)], fill=col("#CFE8F8"))
    widths = [h * (0.17 * math.sin(math.pi * (0.1 + 0.8 * i / 24.0)) ** 0.6 + 0.05) for i in range(25)]
    if mode == "narrow":
        widths = [w_ * 1.4 for w_ in widths]
    smooth_ribbon(d, [pt(x, y) for x, y in lid_top], [wv * SS for wv in widths], col("#2C303E"))
    ox, oy = cx + sx * w * 0.5, cy - h * 0.05 + (droop if mode == "sad" else 0.0)
    smooth_ribbon(d, [pt(ox - sx * w * 0.06, oy), pt(ox + sx * w * 0.12, oy - h * 0.16), pt(ox + sx * w * 0.2, oy - h * 0.3)], [h * 0.12 * SS, h * 0.07 * SS, h * 0.02 * SS], col("#2C303E"))
    smooth_ribbon(d, [pt(x, y) for x, y in low[2:-3]], [h * 0.055 * SS] * len(low[2:-3]), col("#7C849A"))


# the expressions: eye mode and height (of the face height), brow (inner y, mid y, outer y as fractions of the face height, thickness), mouth ('line', 'smile', 'frown', 'open', 'flat', 'tight') and blush
EXPR = {
    "calm": dict(eye="open", eh=0.092, brow=(0.415, 0.392, 0.405, 1.0), mouth="line", blush=1.0, iris=1.0),
    "smile": dict(eye="happy", eh=0.090, brow=(0.405, 0.385, 0.395, 1.0), mouth="smile", blush=1.9, iris=1.0),
    "sad": dict(eye="sad", eh=0.094, brow=(0.372, 0.388, 0.425, 1.0), mouth="frown", blush=0.4, iris=1.05),
    "stern": dict(eye="narrow", eh=0.074, brow=(0.402, 0.392, 0.398, 1.7), mouth="flat", blush=0.2, iris=0.95),
    "angry": dict(eye="narrow", eh=0.068, brow=(0.408, 0.386, 0.358, 2.0), mouth="tight", blush=0.0, iris=0.85),
    "surprised": dict(eye="wide", eh=0.128, brow=(0.352, 0.330, 0.352, 1.0), mouth="open", blush=0.8, iris=0.80),
    "serene": dict(eye="closed", eh=0.092, brow=(0.405, 0.385, 0.395, 0.9), mouth="smile_small", blush=1.4, iris=1.0),
}


def face_expr(w, h, expr="calm"):
    ex = EXPR[expr]
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    cx = w / 2.0
    outline = []
    for i in range(61):
        t = i / 60.0
        y = h * t
        half = w * 0.50
        if t > 0.55:
            k = (t - 0.55) / 0.45
            half = w * 0.50 * (1 - 0.82 * k ** 1.35) if k < 1 else w * 0.09
        outline.append((cx - half, y))
    right = [(cx + (cx - x), y) for x, y in outline][::-1]
    mask = mask_canvas(w, h)
    md = ImageDraw.Draw(mask)
    md.polygon([pt(x, y) for x, y in outline + right], fill=255)
    md.ellipse([S(cx - w * 0.095), S(h - w * 0.07), S(cx + w * 0.095), S(h + w * 0.02)], fill=255)
    fill_mask(img, mask, col(A.SKIN))
    sh = mask_canvas(w, h)
    sd = ImageDraw.Draw(sh)
    sd.polygon([pt(0, h * 0.18), pt(w * 0.16, h * 0.30), pt(w * 0.12, h * 0.70), pt(0, h * 0.60)], fill=255)
    sd.polygon([pt(w, h * 0.18), pt(w * 0.84, h * 0.30), pt(w * 0.88, h * 0.70), pt(w, h * 0.60)], fill=255)
    sd.polygon([pt(w * 0.07, h * 0.72), pt(w * 0.26, h * 0.88), pt(cx, h * 0.97), pt(w * 0.74, h * 0.88), pt(w * 0.93, h * 0.72), pt(w * 0.80, h * 0.90), pt(cx, h * 1.02), pt(w * 0.20, h * 0.90)], fill=255)
    from PIL import ImageChops
    fill_mask(img, ImageChops.multiply(sh, mask), col(A.SKIN_SHADE2, 190))
    if expr == "angry":                                                            # a shadow over the eyes
        bs = mask_canvas(w, h)
        ImageDraw.Draw(bs).rectangle([S(w * 0.08), S(h * 0.34), S(w * 0.92), S(h * 0.47)], fill=90)
        fill_mask(img, ImageChops.multiply(blur(bs, 14), mask), col("#9FA8C0", 110))
    if ex["blush"] > 0:
        cheek = mask_canvas(w, h)
        cd = ImageDraw.Draw(cheek)
        for sx in (-1, 1):
            cd.ellipse([S(cx + sx * w * 0.26 - w * 0.1), S(h * 0.62 - h * 0.03), S(cx + sx * w * 0.26 + w * 0.1), S(h * 0.62 + h * 0.03)], fill=int(min(255, 110 * ex["blush"])))
        fill_mask(img, ImageChops.multiply(blur(cheek, 7), mask), col("#F0CFD2", int(min(255, 150 * min(1.6, ex["blush"])))))
    for sx in (-1, 1):
        exx = cx + sx * w * 0.225
        ey = h * 0.515
        _eye(d, exx, ey, w * 0.255, h * ex["eh"], sx > 0, ex["eye"], ex["iris"])
        bi, bm, bo, bt = ex["brow"]
        smooth_ribbon(d, [pt(exx - sx * w * 0.12, h * bi), pt(exx - sx * w * 0.03, h * ((bi + bm) / 2.0)), pt(exx + sx * w * 0.08, h * bm), pt(exx + sx * w * 0.15, h * bo)],
                      [w * 0.010 * bt * SS, w * 0.014 * bt * SS, w * 0.012 * bt * SS, w * 0.005 * bt * SS], col("#8E9AB4" if bt > 1.4 else "#9DAAC2"))
    smooth_ribbon(d, [pt(cx - w * 0.012, h * 0.55), pt(cx - w * 0.02, h * 0.64), pt(cx + w * 0.015, h * 0.675)], [w * 0.008 * SS, w * 0.012 * SS, w * 0.008 * SS], col("#C9AEB2"))
    my = h * 0.775
    mk = ex["mouth"]
    if mk in ("line", "smile", "smile_small"):
        up = 0.0 if mk == "line" else (0.020 if mk == "smile" else 0.012)
        d.polygon([pt(cx - w * 0.075, my - h * up), pt(cx - w * 0.03, my - h * 0.012), pt(cx, my - h * 0.008), pt(cx + w * 0.03, my - h * 0.012), pt(cx + w * 0.075, my - h * up), pt(cx, my + h * 0.004)], fill=col("#C98C90"))
        d.polygon([pt(cx - w * 0.06, my + h * 0.003 - h * up * 0.6), pt(cx + w * 0.06, my + h * 0.003 - h * up * 0.6), pt(cx + w * 0.035, my + h * (0.03 + up * 0.2)), pt(cx, my + h * (0.036 + up * 0.2)), pt(cx - w * 0.035, my + h * (0.03 + up * 0.2))], fill=col(A.LIP))
        smooth_ribbon(d, [pt(cx - w * 0.078, my - h * up), pt(cx, my + h * 0.006), pt(cx + w * 0.078, my - h * up)], [w * 0.008 * SS] * 3, col("#7A5560"))
    elif mk == "frown":
        smooth_ribbon(d, [pt(cx - w * 0.07, my + h * 0.012), pt(cx, my - h * 0.004), pt(cx + w * 0.07, my + h * 0.012)], [w * 0.010 * SS] * 3, col("#7A5560"))
        d.polygon([pt(cx - w * 0.05, my + h * 0.004), pt(cx + w * 0.05, my + h * 0.004), pt(cx, my + h * 0.03)], fill=col(A.LIP))
    elif mk in ("flat", "tight"):
        smooth_ribbon(d, [pt(cx - w * 0.065, my), pt(cx, my + h * (0.002 if mk == "flat" else -0.004)), pt(cx + w * 0.065, my)], [w * (0.011 if mk == "flat" else 0.014) * SS] * 3, col("#6E4B56"))
        d.polygon([pt(cx - w * 0.05, my + h * 0.004), pt(cx + w * 0.05, my + h * 0.004), pt(cx + w * 0.03, my + h * 0.02), pt(cx - w * 0.03, my + h * 0.02)], fill=col(A.LIP))
    elif mk == "open":
        d.ellipse([S(cx - w * 0.04), S(my - h * 0.012), S(cx + w * 0.04), S(my + h * 0.05)], fill=col("#7A4450"))
        d.ellipse([S(cx - w * 0.03), S(my + h * 0.02), S(cx + w * 0.03), S(my + h * 0.046)], fill=col("#C97F86"))
        smooth_ribbon(d, [pt(cx - w * 0.05, my), pt(cx, my - h * 0.016), pt(cx + w * 0.05, my)], [w * 0.008 * SS] * 3, col("#7A5560"))
    jaw = [(x, y) for x, y in outline if y > h * 0.5 and y > h * 0.62]
    smooth_ribbon(d, [pt(x, y) for x, y in jaw], [w * 0.008 * SS] * len(jaw), col(A.LINE, 170))
    smooth_ribbon(d, [pt(2 * cx - x, y) for x, y in jaw], [w * 0.008 * SS] * len(jaw), col(A.LINE, 170))
    return done(img, w, h)


def face(w, h):
    return face_expr(w, h, "calm")


def head_side(w, h):
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([S(w * 0.55), 0, w * SS, h * SS], fill=col(A.SKIN_SHADE2, 200))                  # the part under the hair
    d.polygon([pt(0, h * 0.62), pt(w * 0.5, h * 0.78), pt(w * 0.3, h)], fill=col(A.SKIN_SHADE, 180))
    return done(img, w, h)


def ear(w, h):
    """A fin ear: a base at the left, five rays fanned up and out, a pale membrane scalloped between the ray tips, thin dark rays."""
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    base = (w * 0.06, h * 0.80)
    angs = [-100, -70, -42, -16, 12]
    lens = [h * 0.62, h * 0.80, w * 0.90, w * 0.80, w * 0.56]
    tips = []
    for a, ln in zip(angs, lens):
        r = math.radians(a)
        tips.append((base[0] + math.cos(r) * ln * 0.95, base[1] + math.sin(r) * ln * 0.95))
    poly = [base]
    for i, tp in enumerate(tips):
        poly.append(tp)
        if i + 1 < len(tips):
            mx, my = (tp[0] + tips[i + 1][0]) / 2, (tp[1] + tips[i + 1][1]) / 2
            poly.append((base[0] + (mx - base[0]) * 0.74, base[1] + (my - base[1]) * 0.74))
    d.polygon([pt(x, y) for x, y in poly], fill=col("#CBE4F6"))
    inner = [base] + [(base[0] + (x - base[0]) * 0.55, base[1] + (y - base[1]) * 0.55) for x, y in tips] + []
    d.polygon([pt(x, y) for x, y in inner], fill=col("#A9CCE9"))
    for tp in tips:
        smooth_ribbon(d, [pt(*base), pt(*tp)], [w * 0.02 * SS, w * 0.008 * SS], col("#4F6F98"))
    smooth_ribbon(d, [pt(x, y) for x, y in poly + [poly[0]]], [w * 0.013 * SS] * (len(poly) + 1), col("#4F6F98"))
    return done(img, w, h)


def skin_tube(w, h, seed=5):
    """An arm or neck segment, seen on any of its four sides: skin with a bluish shade down the right, thin dark outline down both edges."""
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([S(w * 0.62), 0, w * SS, h * SS], fill=col(A.SKIN_SHADE2, 170))
    d.rectangle([0, 0, S(w * 0.02), h * SS], fill=col(A.LINE_SOFT, 150))
    d.rectangle([S(w * 0.98), 0, w * SS, h * SS], fill=col(A.LINE_SOFT, 150))
    return done(img, w, h)


def arm(w, h):
    a = skin_tube(w, h)
    return a


def forearm(w, h):
    return skin_tube(w, h, 6)


def neck(w, h):
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w * SS, S(h * 0.38)], fill=col(A.SKIN_SHADE2, 200))                          # the shadow of the chin
    d.rectangle([S(w * 0.64), 0, w * SS, h * SS], fill=col(A.SKIN_SHADE2, 150))
    d.rectangle([0, 0, S(w * 0.03), h * SS], fill=col(A.LINE_SOFT, 150))
    d.rectangle([S(w * 0.97), 0, w * SS, h * SS], fill=col(A.LINE_SOFT, 150))
    return done(img, w, h)


def shoulder_front(w, h):
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([0, S(h * 0.62), w * SS, h * SS], fill=col(A.SKIN_SHADE2, 90))
    for sx in (-1, 1):                                                                                # the collarbones
        smooth_ribbon(d, [pt(w / 2 + sx * w * 0.05, h * 0.22), pt(w / 2 + sx * w * 0.18, h * 0.30), pt(w / 2 + sx * w * 0.36, h * 0.22)], [w * 0.008 * SS, w * 0.011 * SS, w * 0.006 * SS], col("#C8B4B8"))
    d.rectangle([0, 0, S(w * 0.015), h * SS], fill=col(A.LINE, 235))
    d.rectangle([S(w * 0.985), 0, w * SS, h * SS], fill=col(A.LINE, 235))
    return done(img, w, h)


def shoulder_back(w, h):
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([0, S(h * 0.5), w * SS, h * SS], fill=col(A.SKIN_SHADE2, 110))
    return done(img, w, h)


def hand_palm(w, h):
    """The open palm, seen from the front: the palm, three creases, the finger roots; the fingers themselves are their own card."""
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    d.rectangle([0, S(h * 0.70), w * SS, h * SS], fill=col(A.SKIN_SHADE2, 130))
    for k, y in enumerate((0.30, 0.46, 0.6)):
        smooth_ribbon(d, [pt(w * 0.14, h * (y + 0.04)), pt(w * 0.5, h * (y - 0.03 * (k + 1))), pt(w * 0.86, h * (y + 0.02))], [w * 0.014 * SS] * 3, col("#B79BA3"))
    d.rectangle([0, 0, S(w * 0.05), h * SS], fill=col(A.LINE, 235))
    d.rectangle([S(w * 0.95), 0, w * SS, h * SS], fill=col(A.LINE, 235))
    return done(img, w, h)


def hand_back(w, h):
    img = canvas(w, h, col(A.SKIN))
    d = ImageDraw.Draw(img)
    for x in (0.2, 0.4, 0.6, 0.8):
        smooth_ribbon(d, [pt(w * x, h * 0.05), pt(w * x, h * 0.45)], [w * 0.012 * SS] * 2, col("#C9B3B8"))
    d.rectangle([0, 0, S(w * 0.05), h * SS], fill=col(A.LINE, 235))
    d.rectangle([S(w * 0.95), 0, w * SS, h * SS], fill=col(A.LINE, 235))
    return done(img, w, h)


def hand_side(w, h):
    img = canvas(w, h, col(A.SKIN))
    return done(img, w, h)


def fingers(w, h):
    """Four slender fingers fanned out, hanging from the top: skin with a dark outline and a joint line each; transparent between."""
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    for k in range(4):
        ang = math.radians(-18 + 12 * k)
        x0, y0 = w * (0.18 + 0.21 * k), 0.0
        ln = h * (0.82 - 0.12 * abs(k - 1.4) / 1.5)
        pts = [pt(x0 + math.sin(ang) * ln * u, y0 + math.cos(ang) * ln * u) for u in np.linspace(0, 1, 10)]
        wd = [w * 0.17 * SS * (1 - 0.35 * u) for u in np.linspace(0, 1, 10)]
        smooth_ribbon(d, pts, [x + w * 0.02 * SS for x in wd], col(A.LINE))
        smooth_ribbon(d, pts, wd, col(A.SKIN))
        mid = pts[5]
        d.line([mid, (mid[0] + w * 0.05 * SS, mid[1])], fill=col("#BFA6AC"), width=S(w * 0.012))
    return done(img, w, h)


def thumb(w, h):
    img = canvas(w, h)
    d = ImageDraw.Draw(img)
    pts = [pt(w * 0.5 + math.sin(0.35 * u * 2) * w * 0.12, h * (0.05 + 0.9 * u)) for u in np.linspace(0, 1, 9)]
    wd = [w * 0.34 * SS * (1 - 0.4 * u) for u in np.linspace(0, 1, 9)]
    smooth_ribbon(d, pts, [x + w * 0.04 * SS for x in wd], col(A.LINE))
    smooth_ribbon(d, pts, wd, col(A.SKIN))
    return done(img, w, h)


# ====================================================================================================================== the bodice of water
def _foam_edge(d, w, y, amp, n, seed, fill, thick):
    """A scalloped foam band along y: discs and a rough lower edge."""
    rng = np.random.default_rng(seed)
    xs = np.linspace(-thick, w + thick, n)
    for x in xs:
        r = thick * rng.uniform(0.7, 1.3)
        yy = y + rng.uniform(-amp, amp)
        d.ellipse([S(x - r), S(yy - r), S(x + r), S(yy + r)], fill=fill)


def bodice(w, h, front=True, seed=31):
    """The gown's top: blue water with a darker blue shade at the sides and under the bust, hatching, a foamy scalloped neckline (front: dipping to a V), foam streaks."""
    rng = np.random.default_rng(seed)
    img = canvas(w, h, col(A.GOWN))
    d = ImageDraw.Draw(img)
    for sx in (0, 1):                                                                                  # side shade
        x0 = 0 if sx == 0 else w * 0.78
        d.rectangle([S(x0), 0, S(x0 + w * 0.22), h * SS], fill=col(A.GOWN_DARK, 200))
    d.polygon([pt(0, h * 0.62), pt(w, h * 0.62), pt(w, h), pt(0, h)], fill=col(A.GOWN_DARK, 150))
    d.ellipse([S(w * 0.18), S(h * 0.38), S(w * 0.5), S(h * 0.7)], fill=col(A.GOWN_DARK, 180))         # under the bust
    d.ellipse([S(w * 0.5), S(h * 0.38), S(w * 0.82), S(h * 0.7)], fill=col(A.GOWN_DARK, 180))
    hatch(d, rng, (w * 0.05, h * 0.2, w * 0.95, h * 0.95), 70, h * 0.08, 70, SS * 1.2, col(A.GOWN_MID))
    for k in range(2):
        pts = wave_pts(w * (0.3 + 0.4 * k), h * 0.5, w * (0.34 + 0.34 * k), h * 1.0, w * 0.03, 1.5, k)
        smooth_ribbon(d, [pt(*p) for p in pts], [w * 0.035 * SS] * len(pts), col(A.FOAM))
    mask = mask_canvas(w, h)
    md = ImageDraw.Draw(mask)
    md.rectangle([0, 0, w * SS, h * SS], fill=255)
    if front:                                                                                          # the V of the neckline cut out
        md.polygon([pt(w * 0.40, -2), pt(w * 0.60, -2), pt(w * 0.5, h * 0.34)], fill=0)
    alpha = Image.new("L", img.size, 255)
    alpha.paste(mask, (0, 0))
    img.putalpha(alpha)
    d2 = ImageDraw.Draw(img)
    _foam_edge(d2, w, 0, h * 0.04, 22, seed + 1, col(A.FOAM), h * 0.06)                                 # the foamy top
    if front:
        smooth_ribbon(d2, [pt(w * 0.40, 0), pt(w * 0.5, h * 0.34), pt(w * 0.60, 0)], [w * 0.03 * SS, w * 0.04 * SS, w * 0.03 * SS], col(A.FOAM))
    out = done(img, w, h)
    return out


def bodice_front(w, h):
    return bodice(w, h, True, 31)


def bodice_back(w, h):
    return bodice(w, h, False, 32)


def bodice_side(w, h):
    return bodice(w, h, False, 33)


# ====================================================================================================================== the skirt
def _periodic_stripe(d, W, H, u0, amp, freq, phase, width0, width1, fill, margin=0.0, steps=70):
    """A wavy stripe that runs from the top to the bottom of the unwrapped skirt, wrapping round; width goes from width0 at the top to width1 at the bottom."""
    for wrap in (-W, 0, W):
        pts, wds = [], []
        for i in range(steps):
            t = i / (steps - 1.0)
            x = u0 + wrap + amp * math.sin(t * freq * TAU + phase) * (0.5 + 0.9 * t) + 0.15 * amp * math.sin(t * freq * 2.7 * TAU + phase * 2)
            pts.append(pt(x, H * (margin + (1 - margin) * t)))
            wds.append((width0 + (width1 - width0) * t) * SS * (1 + 0.18 * math.sin(t * 13 + phase)))
        smooth_ribbon(d, pts, wds, fill)


def skirt_unwrapped(W, H, outer):
    """The unwrapped gown, u round the waist from the front (u = 0) through the wearer's left (+x), v down. The inner layer is the water: flat blue with lighter and darker bands; along nine wavy
    streams (denser at the front, as in the still) a white foam stripe with a darker blue tongue of deeper water on one side and a pale band on the other, all widening toward the hem; light hatching. The
    outer layer is only what stands proud of it: the same stripes and tongues with the rest transparent, plus the foamy fringe at the hem."""
    rng = np.random.default_rng(900)
    img = canvas(W, H) if outer else canvas(W, H, col(A.GOWN))
    d = ImageDraw.Draw(img)
    if not outer:
        g = A.vgrad(W * SS, H * SS, "#92C4EC", "#9CCDF3")
        img = Image.fromarray(np.dstack([g, np.full(g.shape[:2], 255, np.float32)]).astype(np.uint8), "RGBA")
        d = ImageDraw.Draw(img)
        for k in range(14):                                                                         # broad lighter and darker bands
            u0 = (k + rng.uniform(-0.3, 0.3)) * W / 14.0
            _periodic_stripe(d, W, H, u0, W * 0.02, rng.uniform(0.6, 1.2), rng.uniform(0, TAU), W * 0.02, W * 0.04, col(A.GOWN_MID if k % 2 else "#86B7E0", 150))
    # the streams: (u, amplitude, frequency, phase, foam width at the hem as a fraction of W)
    streams = [(0.0, 0.05, 0.9, 0.3, 0.020), (-0.055, 0.045, 0.8, 2.0, 0.013), (0.06, 0.05, 1.1, 3.8, 0.015), (-0.15, 0.04, 1.0, 1.0, 0.010), (0.16, 0.04, 0.9, 4.4, 0.011),
               (0.27, 0.035, 1.2, 0.8, 0.010), (-0.28, 0.035, 1.0, 2.7, 0.009), (0.42, 0.03, 0.9, 5.0, 0.009), (-0.42, 0.03, 1.1, 3.1, 0.009)]
    for k, (u, amp, fr, ph, fw) in enumerate(streams):
        u0 = u * W
        _periodic_stripe(d, W, H, u0 + W * fw * 0.7, W * amp, fr, ph, W * fw * 0.25, W * fw * 1.9, col(A.GOWN_DARK, 215 if outer else 255), margin=0.02)
        _periodic_stripe(d, W, H, u0 - W * fw * 0.8, W * amp, fr, ph, W * fw * 0.1, W * fw * 0.7, col(A.GOWN_MID, 200 if outer else 255), margin=0.02)
        _periodic_stripe(d, W, H, u0, W * amp, fr, ph, W * fw * 0.18, W * fw * 1.15, col(A.FOAM), margin=0.0)
    hatch(d, rng, (0, H * 0.1, W, H * 0.95), int(W / 14), H * 0.07, 78, SS * 1.3, col(A.GOWN_MID, 235 if not outer else 200))
    if outer:
        # the hem fringe: foam discs along a ragged line, below it the alpha ends
        mask = Image.new("L", img.size, 255)
        md = ImageDraw.Draw(mask)
        md.rectangle([0, S(H * FRINGE), W * SS, H * SS], fill=0)
        for i in range(int(W / 9)):
            x = i * 9 + rng.uniform(-3, 3)
            r = rng.uniform(4, 9)
            y = H * FRINGE + rng.uniform(-4, 4)
            md.ellipse([S(x - r), S(y - r * 0.8), S(x + r), S(y + r * 1.3)], fill=255)
            d.ellipse([S(x - r), S(y - r * 0.8 - 1), S(x + r), S(y + r * 1.3)], fill=col(A.FOAM))
        rough = torn(mask, rng, 3.0, 10.0)
        al = np.asarray(img.split()[3], dtype=np.float32) * (np.asarray(rough, dtype=np.float32) / 255.0)
        img.putalpha(Image.fromarray(al.astype(np.uint8), "L"))
    return done(img, W, H)


FRINGE = 0.94           # where the foamy hem fringe of the outer layer sits (fraction of the strip's length)


def _panels(U, n, pw, cw, length, r_top, tilt_deg, over):
    """Cuts the unwrapped gown (H x W x 4 floats, u round the circle) into n strips as the model hangs them: strip i is a flat card of width cw units and `length` units, hung from the waist at azimuth
    (i + 0.5) / n and tilted out by tilt_deg; a texel at card-x xl and card-y (a fraction v down) lies on the cone at the angle xl / r(v) from the strip's centre, r(v) being the cone's radius there. So
    the stripes run on from one strip to the next and close in toward the waist exactly as on the real cone; the texels outside the strip's own sector are transparent."""
    H, W = U.shape[0], U.shape[1]
    P = U.copy()
    P[..., :3] *= P[..., 3:4] / 255.0                                           # premultiplied for the sampling
    out = np.zeros((H, n * pw, 4), dtype=np.float32)
    ys = (np.arange(H) + 0.5) / H
    rs = r_top + ys * length * math.sin(math.radians(tilt_deg))
    xs = ((np.arange(pw) + 0.5) / pw - 0.5) * cw
    th = xs[None, :] / rs[:, None]                                              # H x pw
    ok = np.abs(th) <= (math.pi / n) * over
    for i in range(n):
        theta = (i + 0.5) / n * TAU + th
        fu = (theta / TAU * W) % W
        u0 = np.floor(fu).astype(int)
        f = (fu - u0)[..., None]
        u1 = (u0 + 1) % W
        rows = np.arange(H)[:, None]
        a = P[rows, u0] * (1 - f) + P[rows, u1] * f
        al = a[..., 3:4]
        rgb3 = np.where(al > 1e-3, a[..., :3] / np.maximum(al / 255.0, 1e-3), 0.0)
        res = np.concatenate([rgb3, al], axis=-1)
        res[~ok] = 0.0
        out[:, i * pw:(i + 1) * pw] = res
    return out


CTX = {"INNER_N": SK["INNER_N"], "OUTER_N": SK["OUTER_N"]}        # the number of strips of the level of detail being painted (set by tools/gen_undine_geo.py)


def skirt_region(w, h, outer):
    n = CTX["OUTER_N"] if outer else CTX["INNER_N"]
    pw = w // n
    slant = math.hypot(SK["WAIST_Y"], SK["HEM_R"] - SK["WAIST_R"])
    tilt = math.degrees(math.atan2(SK["HEM_R"] - SK["WAIST_R"], SK["WAIST_Y"]))
    cw = 2 * math.pi * SK["HEM_R"] / n * (SK["OUTER_OVER"] if outer else SK["INNER_OVER"])
    length = slant + (SK["OUTER_EXTRA"] if outer else 0.0)
    r_top = SK["WAIST_R"] - 0.2 + (SK["OUTER_PUSH"] if outer else 0.0)
    Wu = 2048
    U = skirt_unwrapped(Wu, h, outer)
    return _panels(U, n, pw, cw, length, r_top, tilt + (0.6 if outer else 0.0), 1.06)


def skirt_in(w, h):
    return skirt_region(w, h, False)


def skirt_out(w, h):
    return skirt_region(w, h, True)

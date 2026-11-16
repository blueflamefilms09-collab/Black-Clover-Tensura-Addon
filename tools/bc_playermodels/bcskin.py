"""The skins of the Black Clover boss models: the character's existing 256x256 HD skin (kept, it is the body and the outfit) with a NEW face and a cleared hat layer.

Why a retouch and not a repaint: the outfits of the family were painted at 4x detail by tools/arc_hd (docs/arc/hd_arc_bc5.md) and are right; what the reference has and they do not is the face (large
anime eyes with a lit iris, brows, a nose, a mouth, the canon marks) and, round it, the 3D hair, crown and the other parts of bcparts.py. So the head's front is redrawn here from a small spec and the
second layer of the head (the hat layer, the old painted hair) is cleared, because the cube hair of the extras replaces it. Everything else is carried over byte for byte.

Face coordinates are model pixels of the 8x8 front of the head: x 0..8 left to right as the viewer sees it (the entity's RIGHT eye is at x ~ 2.4), y 0..8 top to bottom (the brow line ~ 3.5, the eyes ~ 4.9,
the mouth ~ 6.9).
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import bclib as L  # noqa: E402,F401  (path setup)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import hd_core as C  # noqa: E402
import hd_detail as H  # noqa: E402

ASSET_TEX = os.path.join(L.ASSETS, "textures", "entity", "playermodel")


def _load(path):
    return np.asarray(Image.open(path).convert("RGBA"), np.float32) / 255.0


def load_canvas(skin_stem, slim):
    """A Canvas holding the existing skin and its glow map (the glow map is stored premultiplied; the canvas wants straight alpha)."""
    cv = C.Canvas(256, slim)
    cv.px[...] = _load(os.path.join(ASSET_TEX, skin_stem + ".png"))
    gp = os.path.join(ASSET_TEX, skin_stem + "_glow.png")
    if os.path.isfile(gp):
        g = _load(gp)
        a = g[..., 3:4]
        g[..., :3] = np.where(a > 1e-4, g[..., :3] / np.maximum(a, 1e-4), 0.0)
        cv.gl[...] = g
    return cv


def clear_hat(cv):
    """The hat layer of the head is the old painted hair: the cube hair replaces it."""
    for nm in C.FACES:
        cv.face("head", nm, True).clear()


# ================================================================================================ the face
def eye_pair(f, e):
    """Two anime eyes from the spec e: iris (dark, mid, light), lash, sclera, w, h, cy, cx (x of the right and the left eye), tilt, glow, slit, hi."""
    iris = e.get("iris", ("#2a3a70", "#4a78d0", "#bcd8ff"))
    cxs = e.get("cx", (2.35, 5.65))
    for i, cx in enumerate(cxs):
        if i in e.get("skip", ()):
            continue
        H.eye(f, cx, e.get("cy", 4.95), e.get("w", 2.55), e.get("h", 2.15), iris, lash=e.get("lash", "#1c1226"), sclera=e.get("sclera", "#fffefb"), tilt=e.get("tilt", 0.10),
              pupil=iris[1] if e.get("slit") else e.get("pupil", "#0c0814"), iris_r=e.get("iris_r", 1.0), glow=e.get("glow"), hi=e.get("hi", 0.95), lid_shadow=e.get("lid", 0.30),
              lash_w=e.get("lash_w", 0.40), flick=e.get("flick", 0.36))
        if e.get("slit"):
            cy = e.get("cy", 4.95) + 0.04
            f.fill(f.m_ellipse(cx, cy, 0.17, e.get("h", 2.15) * 0.42, soft=1.5), e.get("pupil", "#10060a"), 1.0)
            f.fill(f.m_ellipse(cx - 0.05, cy - 0.5, 0.08, 0.14, soft=1.5), "#ffffff", 0.7)
        if e.get("dark_sclera"):
            # a dark ring round the iris (the devils' eyes: dark sclera)
            w, h = e.get("w", 2.55), e.get("h", 2.15)
            ring = f.m_ellipse(cx, e.get("cy", 4.95), w * 0.50, h * 0.52, soft=1.5) * (1.0 - f.m_ellipse(cx, e.get("cy", 4.95) + 0.04, w * 0.33, h * 0.48, soft=1.5))
            f.fill(ring, e["dark_sclera"], 0.92)


def brows(f, b):
    c = b.get("color", "#1c1620")
    y = b.get("y", 3.45)
    ang = b.get("angle", 0.0)        # + = angry (inner end low)
    t = b.get("thick", 0.26)
    for sgn, cx in ((-1, 2.35), (1, 5.65)):
        inner = (cx + (1.45 if sgn < 0 else -1.45), y + 0.28 * ang)
        outer = (cx + (-1.45 if sgn < 0 else 1.45) * 0.9, y - 0.15 - 0.18 * ang)
        if b.get("skip") and (0 if sgn < 0 else 1) in b["skip"]:
            continue
        H.brow(f, inner, outer, c, t, t * 0.45, arch=b.get("arch", 0.12))


def mouth(f, m):
    k = m.get("kind", "smirk")
    cx, cy = m.get("cx", 4.0), m.get("cy", 6.85)
    col = m.get("color", "#a84a4a")
    lip = m.get("lip", "#d8847a")
    if k == "flat":
        H.mouth_smile(f, cx, cy, m.get("w", 1.7), color=col, lip=lip, depth=0.0, thick=0.17)
    elif k == "smirk":
        pts = [(cx - 0.9, cy + 0.05), (cx - 0.1, cy + 0.12), (cx + 0.7, cy - 0.02), (cx + 1.05, cy - 0.28)]
        f.polyline(pts, col, 0.17)
        f.polyline([(x, y + 0.14) for x, y in pts[1:3]], lip, 0.1, a=0.5)
    elif k == "open":
        H.mouth_smile(f, cx, cy, m.get("w", 2.0), color=col, lip=lip, depth=0.2, thick=0.16, teeth="#fffaf0", open_h=m.get("open", 0.55))
    elif k == "fang":
        H.mouth_smile(f, cx, cy, m.get("w", 2.2), color=col, lip=lip, depth=0.12, thick=0.16)
        for sx in (-0.55, 0.55):
            f.poly([(cx + sx - 0.14, cy + 0.04), (cx + sx + 0.14, cy + 0.04), (cx + sx, cy + 0.62)], m.get("fang", "#fffaf0"), 1.0)
    elif k == "grin":
        H.mouth_smile(f, cx, cy - 0.1, m.get("w", 2.8), color=col, lip=lip, depth=0.35, thick=0.17, teeth=m.get("teeth", "#fffaf0"), open_h=m.get("open", 0.45))
    elif k == "teeth_dark":
        H.mouth_smile(f, cx, cy, m.get("w", 2.6), color="#10060a", lip="#10060a", depth=0.18, thick=0.14, teeth=m.get("teeth", "#2a1a28"), open_h=0.5)


def paint_face(cv, spec):
    """Redraws the front of the head (base layer) from `spec` and lights the glow map for the eyes and marks."""
    f = cv.face("head", "front")
    f.clear()
    skin = spec.get("skin", ("#d9a98a", "#f0c8a8", "#ffe2c8"))
    hair = spec.get("hair")
    H.skin_face(f, tuple(skin), hair=hair, hairline=spec.get("hairline", 1.9), side=0.10, chin=0.08, shadow=0.20, wave=spec.get("wave", 0.35))
    sk = H.col(skin[1])
    # a narrow jaw: a touch of shade at both lower corners and a cheek light
    f.shade(f.m_ellipse(0.6, 6.9, 1.0, 1.4, soft=3.0), 0.9, 0.7)
    f.shade(f.m_ellipse(7.4, 6.9, 1.0, 1.4, soft=3.0), 0.9, 0.7)
    for cb in spec.get("cheek_lines", ()):
        f.polyline(cb, H.tone(skin[1], 0.78), 0.1, a=0.6)
    if spec.get("blush"):
        for cx in (1.5, 6.5):
            H.blush(f, cx, 6.0, 1.0, 0.55, spec.get("blush_color", "#f08890"), spec["blush"])
    H.nose(f, 4.0, 5.95, color="#c8847a", size=0.28, a=0.7)
    brows(f, spec.get("brow", {}))
    for fn in spec.get("pre_eye", ()):
        fn(f)
    eye_pair(f, spec.get("eyes", {}))
    mouth(f, spec.get("mouth", {}))
    for fn in spec.get("marks", ()):
        fn(f)
    return f


def build_skin(skin_stem, slim, spec, out_png, out_glow=None, clear=True):
    cv = load_canvas(skin_stem, slim)
    if clear:
        clear_hat(cv)
    # the old front-of-head glow (eyes of the old face) goes with the old face
    paint_face(cv, spec)
    for fn in spec.get("after", ()):
        fn(cv)
    img, glow = cv.finish()
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    img.save(out_png, optimize=True)
    if glow is not None and out_glow:
        glow.save(out_glow, optimize=True)
    return img, glow


# ================================================================================================ marks
def mark_sun(f, cx=2.0, cy=1.35, r=0.52, color="#14101a", rays=8, glow=None):
    """A sun-like mark (Lucius): a disc with rays."""
    f.ellipse(cx, cy, r, r, color, a=0.92)
    for i in range(rays):
        a = i / rays * 6.2832
        import math
        p0 = (cx + math.cos(a) * r * 0.9, cy + math.sin(a) * r * 0.9)
        p1 = (cx + math.cos(a) * r * 1.9, cy + math.sin(a) * r * 1.9)
        f.line(p0, p1, color, 0.16, 0.05, a=0.9)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, r * 0.55, r * 0.55), glow, 0.8)


def mark_band(f, y0=0.6, y1=1.35, color="#0e0c12", a=0.95, x0=0.0, x1=8.0, glow=None, inset=0.0):
    """A black band across the forehead (Dante)."""
    f.fill(f.m_rect(x0, y0, x1, y1, soft=1.0) * 1.0, color, a)
    if inset:
        f.fill(f.m_rect(x0, y0 + inset, x1, y1 - inset, soft=1.0), H.tone(color, 1.6), 0.35)
    if glow:
        f.glow_fill(f.m_rect(x0, y0 + 0.2, x1, y1 - 0.2), glow, 0.6)


def mark_lines(f, xs=(1.2, 2.2, 3.2, 4.8, 5.8, 6.8), y0=0.2, y1=5.6, color="#0e0c12", a=0.9, glow=None, w=0.16):
    """Vertical lines up into the hairline and down over the eyes to the cheeks (Dante in his devil form)."""
    for i, x in enumerate(xs):
        yy1 = y1 + 0.5 * ((i % 3) - 1)
        f.line((x, y0), (x + (0.25 if x < 4 else -0.25), yy1), color, w, w * 0.4, a=a)
        if glow:
            f.glow_fill(f.m_line((x, y0), (x + (0.25 if x < 4 else -0.25), yy1), w, w * 0.4), glow, 0.7)


def mark_cross(f, cx=5.65, cy=2.4, arm=1.2, color="#0e0c12", a=0.95, glow=None, down=3.9):
    """A cross-shaped mark that runs down across the eye (Zenon)."""
    f.line((cx, cy - arm * 1.1), (cx, cy + down), color, 0.34, 0.18, a=a)
    f.line((cx - arm, cy), (cx + arm, cy), color, 0.3, 0.3, a=a)
    if glow:
        f.glow_fill(f.m_line((cx, cy - arm * 1.1), (cx, cy + down), 0.2, 0.1), glow, 0.6)


def mark_eyepatch(f, cx=2.35, cy=4.95, color="#3a2a22", gold="#e8b830", strap=True):
    """An eyepatch with a gold spade over the entity's right eye (Vanica); its strap crosses the face to the temple."""
    if strap:
        f.line((0.0, 2.2), (cx - 0.6, cy - 0.9), "#2a1c18", 0.30, 0.30, a=1.0)
        f.line((cx + 0.5, cy - 0.9), (5.2, 0.4), "#2a1c18", 0.3, 0.3, a=1.0)
    f.ellipse(cx, cy, 1.45, 1.35, color, a=1.0)
    f.fill(f.m_ellipse(cx, cy, 1.45, 1.35) * (1.0 - f.m_ellipse(cx, cy, 1.22, 1.12, soft=2.0)), H.tone(color, 1.6), 0.7)
    f.fill(f.m_ellipse(cx - 0.3, cy - 0.35, 0.7, 0.45, soft=3.0), H.tone(color, 1.35), 0.4)
    # the spade
    p = cx
    f.ellipse(p - 0.28, cy + 0.0, 0.34, 0.32, gold, a=1.0)
    f.ellipse(p + 0.28, cy + 0.0, 0.34, 0.32, gold, a=1.0)
    f.poly([(p, cy - 0.78), (p - 0.62, cy - 0.05), (p + 0.62, cy - 0.05)], gold, a=1.0)
    f.poly([(p - 0.1, cy + 0.2), (p + 0.1, cy + 0.2), (p + 0.2, cy + 0.72), (p - 0.2, cy + 0.72)], gold, a=1.0)


def mark_moustache(f, color="#1a1214", cx=4.0, y=6.35, goatee=True):
    """A short moustache and a goatee (Dante)."""
    for s in (-1, 1):
        f.poly([(cx + s * 0.1, y), (cx + s * 1.5, y - 0.12), (cx + s * 1.85, y + 0.35), (cx + s * 1.0, y + 0.3), (cx + s * 0.1, y + 0.28)], color, a=0.95)
    if goatee:
        f.poly([(cx - 0.7, 7.45), (cx + 0.7, 7.45), (cx + 0.55, 8.0), (cx - 0.55, 8.0)], color, a=0.95)


def mark_wrinkles(f, color="#7a6a78", a=0.6):
    """Twisted, wrinkled skin (the half-manifested Lucifero)."""
    for pts in (((0.6, 2.4), (1.4, 2.7), (2.2, 2.5)), ((5.8, 2.5), (6.6, 2.7), (7.4, 2.4)), ((1.1, 6.3), (1.5, 6.9), (1.8, 7.4)), ((6.9, 6.3), (6.5, 6.9), (6.2, 7.4)),
                ((3.3, 2.2), (4.0, 2.0), (4.7, 2.2)), ((2.0, 7.4), (4.0, 7.7), (6.0, 7.4))):
        f.polyline(list(pts), color, 0.12, a=a)


def mark_third_eye(f, cx=4.0, cy=1.4, color="#2a1018", glow="#ff3a4a"):
    f.ellipse(cx, cy, 0.62, 0.4, color, a=1.0)
    f.ellipse(cx, cy, 0.34, 0.26, "#e8e0e8", a=0.95)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, 0.5, 0.32), glow, 0.7)


def mark_spade_forehead(f, cx=4.0, cy=1.5, color="#c8283a", glow="#ff4a5a"):
    f.ellipse(cx - 0.22, cy, 0.26, 0.25, color, a=1.0)
    f.ellipse(cx + 0.22, cy, 0.26, 0.25, color, a=1.0)
    f.poly([(cx, cy - 0.55), (cx - 0.48, cy - 0.05), (cx + 0.48, cy - 0.05)], color, a=1.0)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, 0.5, 0.45), glow, 0.7)


def mark_tear(f, cx, y0=5.8, y1=7.6, color="#8a1a2a", glow=None):
    f.line((cx, y0), (cx + 0.1, y1), color, 0.2, 0.08, a=0.9)
    if glow:
        f.glow_fill(f.m_line((cx, y0), (cx + 0.1, y1), 0.12, 0.05), glow, 0.7)

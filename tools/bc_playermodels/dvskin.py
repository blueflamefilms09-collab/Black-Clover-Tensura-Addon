"""The skins of the devil player models (docs/devil_looks.md): a 256x256 skin painted from a small outfit spec (the devils have no earlier skin to retouch, unlike the Black Clover bosses).

The layout is the vanilla 64x64 one at 4x4 texels per pixel (tools/tier_bosses/hd/hd_core.py): this module fills every UV island of the base layer (skin, cloth, trim, boots, belt, gloves), leaves the
second layer empty (the cube hair and gear replace hat, jacket and sleeves) and then lets bcskin paint the anime face over the front of the head. Glow: the trims and the chest stone in the devil's
colour, the eyes (bcskin) and the marks.

Outfit spec (all optional but `skin`):
  skin, body_skin   (shadow, base, light) tones of the head / hands and of the rest (a devil whose body is black under a pale head)
  hair              the head's top and back (under the cube hair); None = the skin shows (bald)
  kind              'coat' (jacket, belt, boots), 'bare' (bare chest), 'open' (open coat, bare chest), 'dress' (dress to the knee, boots), 'robe' (to the feet), 'armor' (plate), 'butler' (tailcoat, gloves)
  top, trim, pants, boots, belt, glove, accent   colours (accent also drives the glow)
  sleeve            how many of the arm's 12 pixels the sleeve covers (12 = long, 5 = short, 0 = bare)
  marks             callables(cv) that paint over the body (glowing lines, tattoos)
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
sys.dont_write_bytecode = True

import numpy as np  # noqa: E402

import bclib as L  # noqa: E402,F401  (path setup)
import bcskin as S  # noqa: E402

import hd_core as C  # noqa: E402
import hd_detail as H  # noqa: E402

DEFAULT_SKIN = ("#b49a98", "#d4bcb4", "#ecdcd6")


def _mouth_smile_safe(face, cx, cy, w, color="#a84a4a", lip="#d8847a", depth=0.28, thick=0.16, teeth=None, open_h=0.0, a=1.0):
    """hd_detail.mouth_smile with the base of its power clamped to 0 (an open mouth whose end point rounds to 1.0000000000000002 gives a complex number there, for some widths).
    The same picture otherwise; installed over it for this generator only (hd_detail is shared and stays as it is)."""
    n = 12
    pts = [(cx + (i / n * 2 - 1) * w / 2.0, cy + depth * (1.0 - (i / n * 2 - 1) ** 2)) for i in range(n + 1)]

    def bow(x):
        return max(0.0, 1.0 - ((x - cx) / (w / 2.0)) ** 2) ** 0.8
    if open_h > 0:
        lower = [(x, y + open_h * bow(x)) for x, y in pts]
        shape = face.m_poly(pts + lower[::-1])
        face.fill(shape, H.tone(color, 0.42), a)
        if teeth:
            face.fill(face.m_poly(pts + [(x, y + open_h * 0.45 * bow(x)) for x, y in pts[::-1]]) * shape, teeth, a)
        face.polyline(lower, lip, thick * 0.9, a=a * 0.9)
    face.polyline(pts, color, thick, a=a)
    face.polyline([(x, y + thick * 0.8) for x, y in pts[2:-2]], lip, thick * 0.55, a=0.55 * a)
    return face


H.mouth_smile = _mouth_smile_safe


def _skin(f, sk):
    f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, sk[2]), (0.5, sk[1]), (1, sk[0])]))


def _head_back(cv, o):
    hair = o.get("hair")
    sk = o["skin"]
    for nm in ("top", "back", "west", "east", "bottom"):
        f = cv.face("head", nm, False)
        _skin(f, sk)
        if hair is None or nm == "bottom":
            continue
        hm = f.m_all()
        if nm in ("west", "east"):
            x0, x1 = (3.6, 8.0) if nm == "west" else (0.0, 4.4)              # the face-side half below the brow is skin
            face_half = f.m_poly([(x0, 2.0), (x1, 2.0), (x1, 8.0), (x0, 8.0)], soft=1.5)
            hm = np.clip(hm - face_half, 0, 1)
            hm = hm * (1.0 - f.m_rect(0.0, 6.2, 8.0, 8.0, soft=2.0) * 0.85)    # under the hair line at the back of the neck: skin
        elif nm == "back":
            hm = hm * (1.0 - f.m_rect(0.0, 6.4, 8.0, 8.0, soft=2.0) * 0.85)
        f.fill(hm, H.lin((0, 0), (0, f.h), [(0, H.tone(hair, 1.12)), (0.6, hair), (1, H.tone(hair, 0.7))]))


def _cloth(f, mask, color, top=1.12, bottom=0.8, folds=()):
    H.cloth(f, mask, color, top=top, bottom=bottom, fold_xs=list(folds) or None)


def _torso(cv, o):
    kind = o.get("kind", "coat")
    bs = o.get("body_skin", o["skin"])
    top, trim, acc = o.get("top", "#2a2630"), o.get("trim", "#c8a040"), o.get("accent", "#ff4a8a")
    belt = o.get("belt", trim)
    for nm in ("front", "back", "west", "east", "top", "bottom"):
        f = cv.face("body", nm, False)
        _skin(f, bs)
        if kind == "bare":
            if nm == "front":
                f.shade(f.m_ellipse(2.3, 3.2, 1.9, 1.4, soft=3.0), 1.1, 0.5)
                f.shade(f.m_ellipse(5.7, 3.2, 1.9, 1.4, soft=3.0), 1.1, 0.5)
                f.polyline([(4.0, 2.2), (4.0, 9.0)], H.tone(bs[1], 0.7), 0.1, a=0.55)
                for y in (5.2, 7.0):
                    f.polyline([(2.6, y), (5.4, y)], H.tone(bs[1], 0.74), 0.09, a=0.45)
            continue
        if nm in ("top", "bottom"):
            _cloth(f, f.m_all(), top)
            continue
        if kind == "open" and nm == "front":
            _cloth(f, f.m_all(), top, folds=(2.0, 6.0))
            f.fill(f.m_poly([(1.6, -0.1), (6.4, -0.1), (5.8, 12.1), (2.2, 12.1)], soft=1.6), H.lin((0, 0), (0, 12), [(0, bs[2]), (0.5, bs[1]), (1, bs[0])]))
            f.shade(f.m_ellipse(3.2, 3.2, 1.3, 1.2, soft=3.0), 1.1, 0.4)
            f.shade(f.m_ellipse(4.8, 3.2, 1.3, 1.2, soft=3.0), 1.1, 0.4)
            H.trim_line(f, [(1.6, 0.0), (2.2, 12.0)], trim, width=0.22, spec=0.3, outline=0.4)
            H.trim_line(f, [(6.4, 0.0), (5.8, 12.0)], trim, width=0.22, spec=0.3, outline=0.4)
        elif kind == "armor":
            f.fill(f.m_all(), H.metal_paint(0, 12, top, 0.8))
            H.bevel(f, f.m_all(), depth=0.5, gain=1.0, spec=0.2, spec_pow=18)
            if nm == "front":
                f.polyline([(4.0, 0.0), (4.0, 12.0)], H.tone(top, 0.45), 0.12)
                f.polyline([(0.4, 4.4), (4.0, 5.4), (7.6, 4.4)], H.tone(top, 0.45), 0.12)
        else:
            _cloth(f, f.m_all(), top, folds=(2.0, 6.0) if nm in ("front", "back") else (2.0,))
            if nm == "front":
                if kind in ("coat", "butler"):
                    f.fill(f.m_poly([(3.0, -0.1), (5.0, -0.1), (4.0, 2.2)], soft=1.3), bs[1])           # the V of the collar
                    H.trim_line(f, [(3.0, 0.0), (4.0, 2.3), (5.0, 0.0)], trim, width=0.16, spec=0.3, glow=acc if kind == "coat" else None)
                    if kind == "butler":
                        f.fill(f.m_poly([(3.1, 1.2), (4.9, 1.2), (4.9, 9.8), (3.1, 9.8)], soft=1.3), "#7a7a84", 0.95)      # the grey vest
                        for y in (2.8, 4.6, 6.4, 8.2):
                            H.stud(f, 4.0, y, 0.2, "#c8c8d0")
                    else:
                        H.trim_line(f, [(4.0, 2.3), (4.0, 9.0)], trim, width=0.1, spec=0.2, outline=0.3, a=0.8)
                elif kind == "dress":
                    H.trim_line(f, [(0.5, 2.6), (4.0, 3.6), (7.5, 2.6)], trim, width=0.2, spec=0.3, glow=acc)
                elif kind == "robe":
                    H.trim_line(f, [(4.0, 0.0), (4.0, 12.0)], trim, width=0.34, spec=0.35, glow=acc)
        if nm in ("front", "back") and kind != "robe":
            H.trim_band(f, 9.0, 10.5, belt, spec=0.35, glow=acc if kind in ("coat", "dress") else None)
            if nm == "front":
                H.stud(f, 4.0, 9.75, 0.55, trim)
        if nm == "front" and o.get("gem", kind in ("armor", "coat", "dress", "robe")):
            H.gem(f, 4.0, 5.2, 0.62, acc, setting=trim, glow=True)
        if nm == "back" and o.get("back_mark"):
            o["back_mark"](f)


def _arms(cv, o):
    kind = o.get("kind", "coat")
    sk = o.get("body_skin", o["skin"])
    top, trim, acc = o.get("top", "#2a2630"), o.get("trim", "#c8a040"), o.get("accent", "#ff4a8a")
    glove = o.get("glove")
    sleeve = float(o.get("sleeve", 12 if kind not in ("bare",) else 0))
    for part in ("arm_r", "arm_l"):
        for nm in ("front", "back", "west", "east", "top", "bottom"):
            f = cv.face(part, nm, False)
            _skin(f, sk)
            if nm == "bottom":
                continue
            if sleeve > 0:
                m = f.m_rect(-0.2, -0.2, f.w + 0.2, sleeve, soft=1.0)
                if kind == "armor":
                    f.fill(m, H.metal_paint(0, sleeve, top, 0.8))
                    H.bevel(f, m, depth=0.4, gain=1.0, spec=0.15)
                else:
                    _cloth(f, m, top, folds=(1.3, 2.7) if nm in ("front", "back") else ())
                if sleeve < 11.5 and nm != "top":
                    H.trim_band(f, sleeve - 1.0, sleeve + 0.2, trim, spec=0.3, glow=acc if kind == "coat" else None)
            if glove and nm != "top":
                gm = f.m_rect(-0.2, 8.6, f.w + 0.2, 12.2, soft=1.0)
                _cloth(f, gm, glove, top=1.1, bottom=0.85)
                H.trim_band(f, 8.4, 9.2, H.tone(glove, 0.8), spec=0.1, outline=0.3)
            if nm in ("front", "back") and kind in ("coat", "dress", "robe", "butler") and sleeve >= 11.5:
                H.trim_band(f, 7.6, 8.5, trim, spec=0.3, glow=acc if kind != "butler" else None)


def _legs(cv, o):
    kind = o.get("kind", "coat")
    sk = o.get("body_skin", o["skin"])
    pants, boots, trim, acc = o.get("pants", "#1c1a22"), o.get("boots", "#14121a"), o.get("trim", "#c8a040"), o.get("accent", "#ff4a8a")
    for part in ("leg_r", "leg_l"):
        for nm in ("front", "back", "west", "east", "top", "bottom"):
            f = cv.face(part, nm, False)
            _skin(f, sk)
            cover = {"dress": 5.0, "robe": 12.0}.get(kind, 12.0)
            if o.get("legs_bare"):
                cover = 0.0
            if nm == "top":
                _cloth(f, f.m_all(), pants)
                continue
            if nm == "bottom":
                if boots:
                    _cloth(f, f.m_all(), boots)
                continue
            if cover > 0:
                m = f.m_rect(-0.2, -0.2, f.w + 0.2, cover, soft=1.0)
                col = o.get("top", pants) if kind in ("dress", "robe") else pants
                _cloth(f, m, col, folds=(1.4, 2.6) if nm in ("front", "back") else ())
                if kind == "robe":
                    H.trim_band(f, 11.0, 12.0, trim, spec=0.3, glow=acc)
            if kind != "robe" and boots:
                bm = f.m_rect(-0.2, 8.8, f.w + 0.2, 12.2, soft=1.0)
                _cloth(f, bm, boots, top=1.15, bottom=0.78)
                H.trim_band(f, 8.6, 9.5, trim, spec=0.3, glow=acc if kind in ("coat", "armor") else None)
                f.shade(f.m_rect(0, 11.2, f.w, 12.2), 0.7, 0.7)


def build_skin(spec, slim, out_png, out_glow):
    """The skin of one devil: body from the outfit spec, face from the face spec; -> (image, glow image or None)."""
    o = dict(spec["outfit"])
    o.setdefault("skin", DEFAULT_SKIN)
    o["hair"] = spec.get("hair")
    cv = C.Canvas(256, slim)
    _head_back(cv, o)
    _torso(cv, o)
    _arms(cv, o)
    _legs(cv, o)
    S.paint_face(cv, spec["face"])
    for fn in o.get("marks", ()):
        fn(cv)
    img, glow = cv.finish()
    os.makedirs(os.path.dirname(out_png), exist_ok=True)
    img.save(out_png, optimize=True)
    if glow is not None and out_glow:
        glow.save(out_glow, optimize=True)
    return img, glow

"""The faces of Fuegoleon's Salamander (docs/magic/fire_casting.md, section 4.2): alert, angry, sleepy, smug and roaring, as plate swaps on the head. The model's own slit eyes are the calm face; every
other face is a tiny model per form that holds ONLY the bones from the root to the head and the jaw (same pivots and rest rotations as the full model, so the one animation file poses it exactly like the
body) and, on the head, two flat eye plates (one quad each) laid 0.25 units proud of the model's own eye cards, painted opaque so they cover them: a lidded eye socket with the eye of the mood in it (a wide
round eye, a slanted lid, a heavy sleepy lid, a half lid with a raised lower lid and a sideways look, a squeezed roaring eye with a heavy brow). The roaring face adds a mouth plate on the jaw (the
glowing inside of an open mouth). Each face has its own sheet (a colour sheet and a same-size glow sheet that lights the iris, drawn additive), so a face costs at most 3 quads (2 eyes + the mouth) and
one small texture pair; the game draws it on the full level of detail only, over the body model and before the glow-only model.

    python3 -B tools/gen_salamander_faces.py            writes geo/entity/firecast_{salamander,dragon}_fx_face_<face>.geo.json and textures/entity/<same>.png and <same>_glow.png
    python3 -B tools/gen_salamander_faces.py --count    prints the quads of every face model

The ids contain "_fx_" on purpose: the safe-additive texture scan already covers every texture of that name (the glow-only models) and so covers these glow sheets too.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import firecast_geo_lib as L  # noqa: E402
import gen_salamander_art as A  # noqa: E402
import gen_salamander_geo as G  # noqa: E402

from firecast_geo_lib import vec  # noqa: E402

FACES = ["alert", "angry", "sleepy", "smug", "roaring"]
PLATE_K = 1.25                      # the plate is this much bigger than the model's own eye card (it has a socket and a brow)
PLATE_OUT = 0.25                    # and stands this far (head units) in front of the eye card, so it covers it


def col(h, a=255):
    return A.col(h, a)


def _ellipse(dr, cx, cy, rx, ry, tilt, fill, outline=None, width=1.2, n=48):
    pts = []
    for a in np.linspace(0, math.tau, n, endpoint=False):
        x, y = math.cos(a) * rx, math.sin(a) * ry
        pts.append((cx + x * math.cos(tilt) - y * math.sin(tilt), cy + x * math.sin(tilt) + y * math.cos(tilt)))
    A.poly(dr, pts, fill, outline, width)
    return pts


def eye_face(w, h, pal, face, mirror):
    """One eye plate (the picture faces LEFT: the snout is at the left, the back of the head at the right) and its glow, for a face. Returns (colour RGBA, glow RGBA) float arrays."""
    lay, gl = A.canvas(w, h), A.canvas(w, h)
    dr, dg = ImageDraw.Draw(lay), ImageDraw.Draw(gl)
    clear = (0, 0, 0, 0)
    skin, shade, line = col(pal["base"]), col(pal["dark"]), col(pal["line"])
    # the socket: an opaque, outlined blob in the skin that hides the model's own eye
    cx, cy = w * 0.5, h * 0.52
    _ellipse(dr, cx, cy, w * 0.475, h * 0.45, 0.0, skin, None)
    _ellipse(dr, cx, cy + h * 0.03, w * 0.40, h * 0.37, 0.0, mix_(pal["base"], pal["dark"], 0.45))
    # the eye of the mood: (rx, ry of the eye, tilt, slit half-width, iris colour, upper lid line (front y, back y), lower lid line (front y, mid y, back y) or None, pupil shift, brow)
    P = {
        "alert": dict(rx=0.42, ry=0.40, tilt=0.0, slit=0.07, iris=pal["eye"], up=None, low=None, look=0.0, brow=[(0.10, 0.18), (0.42, 0.08), (0.90, 0.14)], hi=1.2),
        "angry": dict(rx=0.42, ry=0.36, tilt=-0.20, slit=0.06, iris="#FF9A2E", up=(0.50, 0.14), low=None, look=0.0, brow=[(0.02, 0.50), (0.50, 0.30), (0.98, 0.16)], hi=0.9),
        "sleepy": dict(rx=0.42, ry=0.34, tilt=0.0, slit=0.17, iris="#D8B83C", up=(0.56, 0.62), low=(0.80, 0.74, 0.80), look=0.0, brow=None, hi=0.0),
        "smug": dict(rx=0.42, ry=0.36, tilt=0.0, slit=0.10, iris=pal["eye"], up=(0.47, 0.43), low=(0.76, 0.58, 0.76), look=-0.16, brow=[(0.06, 0.34), (0.5, 0.26), (0.94, 0.20)], hi=1.0),
        "roaring": dict(rx=0.44, ry=0.34, tilt=-0.28, slit=0.05, iris="#FFB63A", up=(0.64, 0.18), low=(0.80, 0.68, 0.80), look=0.0, brow=[(0.0, 0.58), (0.5, 0.34), (1.0, 0.12)], hi=0.0),
    }[face]
    rx, ry, tilt = w * P["rx"], h * P["ry"], P["tilt"]
    ecx = cx + w * P["look"] * 0.0
    ring = _ellipse(dr, ecx, cy, rx, ry, tilt, line)
    _ellipse(dr, ecx, cy, rx * 0.86, ry * 0.86, tilt, col(P["iris"]))
    _ellipse(dg, ecx, cy, rx * 0.80, ry * 0.80, tilt, col(pal["glow"] if face != "angry" and face != "roaring" else "#FFC040"))
    px = ecx + w * P["look"]
    sl = P["slit"]
    for d_, c_ in ((dr, col(pal["eye_dark"])), (dg, clear)):                                    # the pupil (a slit): dark in the colour sheet, a hole in the glow
        d_.polygon(A.S([(px, cy - ry * 0.80), (px + w * sl, cy), (px, cy + ry * 0.80), (px - w * sl, cy)]), fill=c_)
    if P["hi"]:
        hx, hy = px + rx * 0.30, cy - ry * 0.36
        dr.ellipse([(hx - P["hi"]) * A.SS, (hy - P["hi"]) * A.SS, (hx + P["hi"]) * A.SS, (hy + P["hi"]) * A.SS], fill=(255, 255, 255, 255))
        dg.ellipse([(hx - P["hi"]) * A.SS, (hy - P["hi"]) * A.SS, (hx + P["hi"]) * A.SS, (hy + P["hi"]) * A.SS], fill=clear)
    top = P["up"]
    lid = mix_(pal["base"], pal["dark"], 0.28)
    if top:                                                                                    # the upper lid: everything above the line (front y, back y) is lid
        f_, b_ = top
        _lid(dr, w, h, f_, b_, lid, line, skin)
        dg.polygon(A.S([(0, 0), (w, 0), (w, b_ * h), (0, f_ * h)]), fill=clear)
    low = P["low"]
    if low:                                                                                    # the lower lid: a curve rising into the eye
        f_, m_, b_ = low
        pts = A.bez((0, f_ * h), (w * 0.5, (2 * m_ - (f_ + b_) / 2) * h), (w, b_ * h), 18)
        pts = [(x, y) for x, y in pts] + [(w, h), (0, h)]
        dr.polygon(A.S(pts), fill=skin)
        dr.line(A.S(pts[:18]), fill=line, width=max(1, int(1.6 * A.SS)))
        dg.polygon(A.S(pts), fill=clear)
    brow = P["brow"]
    if brow:                                                                                   # the brow: a heavy dark line (a ridge) above the eye
        pts = [(x * w, y * h) for x, y in brow]
        A.line(dr, pts, line, 2.6 if face in ("angry", "roaring") else 1.8)
        A.line(dr, [(x, y + 1.0) for x, y in pts], shade, 1.2)
    if face == "roaring":                                                                      # crinkles at the back of the eye
        A.line(dr, [(w * 0.84, h * 0.34), (w * 0.97, h * 0.30)], line, 1.1)
        A.line(dr, [(w * 0.82, h * 0.66), (w * 0.96, h * 0.72)], line, 1.1)
    sock = Image.new("L", lay.size, 0)                                                         # everything stays inside the socket: the lids are cut to its outline, which is stroked last
    ImageDraw.Draw(sock).polygon(A.S(_ellipse(ImageDraw.Draw(Image.new("L", (1, 1))), cx, cy, w * 0.475, h * 0.45, 0.0, None, None)), fill=255)
    for im in (lay, gl):
        im.putalpha(ImageChops.multiply(im.getchannel("A"), sock))
    A.line(dr, _ellipse(ImageDraw.Draw(Image.new("L", (1, 1))), cx, cy, w * 0.475, h * 0.45, 0.0, None, None) + [(cx + w * 0.475, cy)], line, 1.5)
    c, g = A.done(lay, w, h), A.done(gl, w, h)
    if mirror:
        c, g = A.flipx(c, g)
    return c, g


def _lid(dr, w, h, f_, b_, lid, line, skin):
    """The upper lid: the area above the line from (front, f_) to (back, b_) in the lid colour, with a dark edge and a lighter rim above it."""
    area = [(0.0, 0.02 * h), (w, 0.02 * h), (w, b_ * h), (0.0, f_ * h)]
    dr.polygon(A.S(area), fill=lid)
    A.line(dr, [(0.0, f_ * h), (w, b_ * h)], line, 1.8)
    A.line(dr, [(0.0, f_ * h - 1.6), (w, b_ * h - 1.6)], skin, 1.0)


def mouth_face(w, h, pal, mirror=False):
    """The inside of an open mouth, seen from above (the picture's top is the snout tip, symmetric): dark red walls, a paler tongue, a glowing throat at the back, a row of teeth along each edge."""
    lay, gl = A.canvas(w, h), A.canvas(w, h)
    dr, dg = ImageDraw.Draw(lay), ImageDraw.Draw(gl)
    outline = [(w * 0.50, h * 0.0), (w * 0.90, h * 0.12), (w * 0.98, h * 0.55), (w * 0.86, h * 1.0), (w * 0.14, h * 1.0), (w * 0.02, h * 0.55), (w * 0.10, h * 0.12)]
    A.poly(dr, outline, col(pal["mouth"]), col(pal["line"]), 1.4)
    A.poly(dr, [(w * 0.50, h * 0.10), (w * 0.72, h * 0.28), (w * 0.74, h * 0.80), (w * 0.26, h * 0.80), (w * 0.28, h * 0.28)], mix_(pal["mouth"], "#E0584A", 0.55))        # the tongue
    A.line(dr, [(w * 0.50, h * 0.14), (w * 0.50, h * 0.78)], col(pal["mouth"], 200), 1.0)
    for i in range(7):                                                                          # teeth along both edges
        y = h * (0.16 + 0.11 * i)
        for sx in (-1, 1):
            x = w * (0.5 + sx * (0.36 + 0.06 * math.sin(i / 6.0 * math.pi)))
            A.poly(dr, [(x, y), (x - sx * w * 0.07, y + h * 0.04), (x, y + h * 0.07)], col(pal["tooth"]))
    A.poly(dg, [(w * 0.50, h * 0.50), (w * 0.80, h * 0.72), (w * 0.72, h * 1.0), (w * 0.28, h * 1.0), (w * 0.20, h * 0.72)], col("#FF7A22"))                                # the glowing throat
    A.poly(dg, [(w * 0.50, h * 0.66), (w * 0.64, h * 0.82), (w * 0.60, h * 1.0), (w * 0.40, h * 1.0), (w * 0.36, h * 0.82)], col("#FFD36A"))
    return A.done(lay, w, h), A.done(gl, w, h)


def mix_(a, b, t):
    ca, cb = L.rgb(a), L.rgb(b)
    r = ca + (cb - ca) * t
    return int(r[0]), int(r[1]), int(r[2]), 255


# ====================================================================================================================== the models
def head_chain(full):
    """The bones from the root to the head (and the jaw) of a built full model: name -> (pivot, parent, rotation)."""
    names = []
    n = "head"
    while n and n != "root":
        names.append(n)
        n = full.bones[n].get("parent")
    names = names[::-1] + ["jaw"]
    return names


def build_face(form, face, full, S):
    pal = A.BIG if form == "dragon" else A.SMALL
    big = form == "dragon"
    ident = "firecast_%s_fx_face_%s" % (form, face)
    W, H = (512, 128) if big else (256, 64)
    at = L.Atlas(W, H)
    e = 56 * S
    at.region("eye_w", e, e, lambda w, h: eye_face(w, h, pal, face, True))
    at.region("eye_e", e, e, lambda w, h: eye_face(w, h, pal, face, False))
    if face == "roaring":
        at.region("mouth", 96 * S, 64 * S, lambda w, h: mouth_face(w, h, pal))
    m = L.Model(ident, at, seed=1, tex_scale=1.0)
    for name in head_chain(full):
        b = full.bones[name]
        m.bone(name, b["pivot"], b.get("parent"), b.get("rotation"))
    hs = G.BIG_HS if big else 1.0
    ws = 1.0 if big else 1.2
    JH = vec(0, 103, -36) if big else vec(0, 8.8, -19.5)
    H3 = lambda x, y, z: JH + vec(x, y, z) * hs
    es = (3.3 if big else 3.9) * hs
    for sx in (-1, 1):
        ex = sx * (3.55 * ws + 0.82 + PLATE_OUT)
        right = vec(0, 0, -1) if sx < 0 else vec(0, 0, 1)
        m.card("head", H3(ex, 1.5, -4.7), right, vec(0, 1, 0), es * PLATE_K, es * PLATE_K, ("eye_w" if sx < 0 else "eye_e", 0))
    if face == "roaring":
        sl = 1.0 if big else 0.58
        zend = -(6.9 + 4.6 * sl + 3.8 * sl - 0.8)
        jlen = -zend - 1.2
        m.card("jaw", H3(0, -1.72 + 0.2, -1.2 - jlen / 2), vec(1, 0, 0), vec(0, 0, -1), 4.5 * ws * hs, jlen * hs * 0.96, ("mouth", 0))
    return m, at


def save(m, at, ident):
    c = at.color
    c[c[..., 3] == 0] = 0                                                      # nothing under a fully transparent texel (the safe additive rule covers these sheets)
    m.save()
    at.save_color(ident, 1.0)
    at.save_glow(ident + "_glow", 1.0)


def main():
    count_only = "--count" in sys.argv
    for form, atlas, builder, S in (("salamander", G.small_atlas, G.build_small, 1), ("dragon", G.big_atlas, G.build_dragon, 2)):
        full = builder(0, atlas(), False)
        for face in FACES:
            m, at = build_face(form, face, full, S)
            print("%-40s cubes %2d  quads %2d" % (m.id, m.cubes(), m.quads()))
            if not count_only:
                save(m, at, m.id)


if __name__ == "__main__":
    main()

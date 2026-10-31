"""Shared drawing helpers of the hand-painted Black Clover characters of group bc5 (the Zogratis family: Dante, Vanica, Zenon and Lucius, and Fana of the Eye of the Midnight Sun, with their
second forms), docs/arc/hd_arc_bc5.md.

Not a character module (the leading underscore keeps it out of the generator and of the module test). It is a private copy of the group-bc4 kit (`_bc4.py`) so the painters of this group
do not change when another group edits its own kit, plus the helpers the ten looks need that no earlier kit has (at the end of the file: a sibling loader, bandage wraps, stubble, a bone
plate, jagged cracks, a mark of glow). Everything works in MODEL PIXELS of a face (4 texels at 256); at 256 a line thinner than 0.2 model pixel breaks into dots, so lines are kept at texel width. Deterministic, numpy and
Pillow only (no random numbers: `H.hsh` is a hash).

The three things every character does the same way:
  * `start()`: the 64x64 blueprint (colour blocking and layout stay) put through the baseline bridge, except the front of the head, which is redrawn; the second-layer coverage of the blueprint is
    remembered so the redraw stays inside it (`hold_cover`).
  * `tidy_head()`: the sides, back, top and bottom of the head are repainted as two clean regions (skin, hair) with the classification of the 64x64 sheet, so nothing speckled shows where the
    3D hair does not reach.
  * `finish()`: the clip to the blueprint and the export.
"""
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.dirname(HERE))
import arc_hdlib as A  # noqa: E402

H = A.H
C = A.C
SIDES = ("front", "back", "west", "east")
LIMBS = ("body", "arm_r", "arm_l", "leg_r", "leg_l")
FACES5 = ("front", "back", "west", "east", "top", "bottom")


# ================================================================================================ the contract helpers (the second layer stays inside the 64x64 blueprint)
def cover_of(cv, part, name):
    """The second-layer coverage of the blueprint (what the 64x64 sheet draws on the second layer)."""
    return (cv.face(part, name, True).m_alpha() > 0.5).astype(np.float32)


def all_covers(cv, parts=LIMBS, names=SIDES):
    return {(p, n): cover_of(cv, p, n) for p in parts for n in names}


def hold_cover(cv, covers):
    """Clips every second-layer island to its blueprint coverage after the drawing."""
    for (part, name), m in covers.items():
        f = cv.face(part, name, True)
        f.fill(1.0 - m, "#000000", mode="erase")
        f.glow[..., 3] *= m


def start(cid, spec, masks, style, hair_hex, hair_rgb):
    """-> (canvas, covers, hair style id, slim, blueprint 64 image). The head front (base layer) is skipped: the module draws it."""
    spec["hair"]["color"] = hair_hex
    img64, glow64, hair, hrgb, slim = A.blueprint(cid, spec, masks, hair=style)
    cv = H.Canvas(256, slim)
    A.bridge(cv, img64, glow64, spec, hair_rgb, skip=lambda part, face, ov: part == "head" and face == "front" and not ov)
    return cv, all_covers(cv), hair, slim, img64


def finish(cv, covers, hair, hair_rgb, slim, extra_covers=None):
    hold_cover(cv, covers)
    if extra_covers:
        hold_cover(cv, extra_covers)
    skin, glow = cv.finish()
    return skin, glow, hair, tuple(int(c) for c in hair_rgb), slim


# ================================================================================================ colour
def lit(c, k):
    """The colour c lit by k > 1 without washing out toward white (H.tone mixes toward a warm white and turns dark leather grey): a plain scale with a touch of warmth."""
    a = H.col(c)
    return np.clip(a * k + (k - 1.0) * np.array([0.03, 0.02, 0.0], np.float32), 0, 1).astype(np.float32)


def deep(c, k=0.55):
    """A shadow of c that keeps the hue (no cool shift): used for outlines and seams."""
    return np.clip(H.col(c) * k, 0, 1).astype(np.float32)


def hair_pal(base, light_k=1.16, dark_k=0.58):
    """dict(d, m, l, h) for a hair colour: shadow, body, light, glint (the light and the glint keep the hue, no washing out towards white)."""
    b = H.col(base)
    light = np.clip(b * light_k + 0.035, 0, 1).astype(np.float32)
    return {"d": np.clip(b * dark_k, 0, 1).astype(np.float32), "m": b, "l": light, "h": np.clip(light + (1.0 - light) * 0.34, 0, 1).astype(np.float32)}


def skin3(spec_skin):
    """(shadow, base, light) skin tones of a sheet skin name, as hex strings, plus the deep crease tone."""
    p = [c for c in C.skin_palette(spec_skin)]
    hx = lambda c: "#%02x%02x%02x" % tuple(int(v) for v in c[:3])  # noqa: E731
    return (hx(p[1]), hx(p[2]), hx(p[3])), hx(p[0])


# ================================================================================================ cloth, seams, lines
def cloth(face, mask, color, top=1.12, bottom=0.86, round_=0.10, fold_xs=None, fold_depth=0.07, fold_y0=0.0, fold_y1=None, a=1.0):
    """H.cloth with a lit top that keeps the hue (H.tone whitens: a dark red jacket turned pink and black trousers turned grey at top = 1.25), no weave."""
    mask = np.asarray(mask, np.float32)
    ys = np.where(mask > 0.5)[0]
    y0 = face.Y[ys.min(), 0] - 0.5 / face.f if len(ys) else 0.0
    y1 = face.Y[ys.max(), 0] + 0.5 / face.f if len(ys) else face.h
    t = lit(color, top) if top > 1.0 else H.tone(color, top)
    face.fill(mask, H.lin((0, y0), (0, y1), [(0, t), (0.55, color), (1, H.tone(color, bottom))]), a)
    away = face.X if face.light[0] > 0 else face.w - face.X
    face.shade(mask, 1.0 - round_ * H.smoothstep(0.45 * face.w, face.w, away), a)
    if fold_xs:
        H.folds(face, mask, fold_xs, depth=fold_depth, width=0.25, y0=fold_y0 if fold_y0 else y0, y1=fold_y1 if fold_y1 else y1)
    return face


def seam(f, pts, color="#c9a867", width=0.07, a=0.8, dash=(0.34, 0.2), shadow=0.28, mask=None):
    """A stitched line kept at texel scale (width at least 0.2, dashes at least 0.5 / 0.36)."""
    d = None if dash is None else (max(dash[0], 0.5), max(dash[1], 0.36))
    H.stitch(f, pts, color, width=max(width, 0.2), dash=d, shadow=shadow, a=a, mask=mask)


def line(f, pts, color, width=0.2, a=1.0, mask=None):
    """A solid line of at least one texel (0.25 px) width."""
    m = f.m_polyline(pts, max(width, 0.22))
    f.fill(m if mask is None else m * mask, color, a)


def edge(f, mask, color, width=0.22, a=0.9, inside=True):
    """A dark (or light) outline along the border of a mask: the colour-blocking line of the references. inside=True keeps it within the mask."""
    from_mask = np.asarray(mask, np.float32)
    ring = np.clip(from_mask - H.blur(from_mask, max(0.8, width * f.f * 0.9)) * 1.0, 0, 1) if inside else np.clip(H.blur(from_mask, max(0.8, width * f.f * 0.9)) - from_mask, 0, 1)
    f.fill(np.clip(ring * 2.2, 0, 1) * (from_mask if inside else 1.0), color, a)


def rim_light(f, mask, x0, x1, base, k=1.55, a=0.22):
    f.fill(mask * f.m_rect(x0, f.Y.min(), x1, f.Y.max() + 1.0), lit(base, k), a)


def sheen(f, mask, xs, y0, y1, strength=0.2, color="#ffffff", width=0.2, tilt=0.03):
    H.sheen(f, mask, xs, width=width, strength=strength, tilt=tilt, y0=y0, y1=y1, color=color)


def button(f, cx, cy, r, color, spec=0.8):
    H.stud(f, cx, cy, r, color, spec=spec)


def band(f, y0, y1, color, spec=0.12, outline=0.7, x0=0.0, x1=None, mask=None):
    """A thin trim band y0..y1 (a cuff, a hem, a boot top): a gradient (lit top, dark bottom), a dark line above and below it, a thin light line on the lit edge and a soft bevel. Unlike H.trim_band it
    does not whiten when the band is only 4 to 5 texels high."""
    x1 = f.w if x1 is None else x1
    m = f.m_rect(x0, y0, x1, y1)
    if mask is not None:
        m = m * np.asarray(mask, np.float32)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, lit(color, 1.35)), (0.45, color), (1, H.tone(color, 0.62))]))
    h = y1 - y0
    f.fill(m * f.m_rect(x0, y0, x1, y0 + min(0.2, h * 0.3)), lit(color, 1.9), 0.55)
    f.fill(m * f.m_rect(x0, y1 - min(0.22, h * 0.3), x1, y1), deep(color, 0.35), 0.7)
    f.fill(f.m_rect(x0, y0 - 0.2, x1, y0 - 0.02) * (1.0 if mask is None else np.asarray(mask, np.float32)), deep(color, 0.3), outline)
    if spec > 0:
        H.bevel(f, m, depth=max(0.12, h * 0.3), gain=0.9, spec=spec)
    return m


# ================================================================================================ skin
def limb_skin(f, skin, y0, y1, tendon=True, knuckles=False):
    """Bare skin of an arm or a leg from y0 to y1: a soft gradient, darker at both sides (the roundness), a sheen, the shadow of whatever hangs above, a tendon line."""
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, H.tone(skin[1], 0.86)), (0.2, skin[1]), (0.7, H.tone(skin[1], 0.98)), (1, H.tone(skin[0], 0.92))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(m, 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 0.9, side)), 0.9)
    H.sheen(f, m, [f.w * 0.42], width=0.4, strength=0.12, tilt=0.0, y0=y0 + 0.6, y1=y1 - 0.4, color="#fff0e0")
    H.shadow_under(f, y0 + 0.05, 0.9, 0.25, mask=m)
    if tendon:
        f.fill(f.m_line((f.w * 0.64, y0 + 1.6), (f.w * 0.58, y0 + 3.4), 0.2, 0.2) * m, H.tone(skin[0], 0.8), 0.4)
    f.fill(f.m_band(y1 - 0.7, y1), H.tone(skin[0], 0.9), 0.25)
    return m


def fingers(f, skin, y0=10.9, y1=11.9, n=3):
    """The bare hand: finger gaps on the front and the back (a gap is one texel wide)."""
    if f.name in ("front", "back"):
        for i in range(1, n + 1):
            x = f.w * i / (n + 1)
            f.fill(f.m_line((x, y0), (x, y1), 0.24, 0.2), H.tone(skin[0], 0.7), 0.65)
        f.fill(f.m_band(y0 - 0.1, y0 + 0.08), H.tone(skin[0], 0.72), 0.5)


# ================================================================================================ the head
def sheet_mask(f, img64, pred):
    """A texel mask (nearest upscale, soft edge) of the 64x64 sheet's pixels of this island for which pred(rgba uint8 array h x w x 4) is true."""
    px = np.asarray(img64.convert("RGBA"))
    x0, y0 = f.x0 // f.f, f.y0 // f.f
    sub = px[y0:y0 + int(f.h), x0:x0 + int(f.w)]
    m = pred(sub).astype(np.float32)
    big = np.kron(m, np.ones((f.f, f.f), np.float32))
    return np.clip(H.blur(big, f.f * 0.45) * 1.25 - 0.1, 0, 1)


def hair_region(f, img64, spec, hair_rgb):
    """The part of an island of the head where the 64x64 sheet has hair (not skin)."""
    skins = np.array([c[:3] for c in C.skin_palette(spec["skin"])], np.float32)
    hairs = np.array([c[:3] for c in C.hair_shades(hair_rgb, 4)], np.float32)

    def pred(sub):
        rgb = sub[..., :3].astype(np.float32)
        ds = np.min(np.linalg.norm(rgb[:, :, None, :] - skins[None, None], axis=-1), axis=-1)
        dh = np.min(np.linalg.norm(rgb[:, :, None, :] - hairs[None, None], axis=-1), axis=-1)
        return (dh < ds) & (sub[..., 3] > 10)

    return sheet_mask(f, img64, pred)


def tidy_head(cv, img64, spec, hair_hex, hair_rgb, skin, faces=("west", "east", "back", "top", "bottom"), ears=True, ear_y=4.7, earring=None):
    """Repaints the base layer of the head islands other than the front as two clean regions: the scalp (a hair gradient with the light of the top) and the skin (a gradient, darker toward the
    back and the neck). The 3D hair covers most of it; this is what shows through at the hairline, at the neck and in the far LOD."""
    pal = hair_pal(hair_hex)
    for nm in faces:
        f = cv.face("head", nm, False)
        hm = hair_region(f, img64, spec, hair_rgb)
        skin_m = np.clip(1.0 - hm, 0, 1)
        f.fill(skin_m, H.lin((0, 0), (0, f.h), [(0, skin[2]), (0.5, skin[1]), (1, skin[0])]))
        side = np.minimum(f.X, f.w - f.X)
        f.shade(skin_m, 1.0 - 0.07 * (1.0 - H.smoothstep(0.0, 1.0, side)), 1.0)
        if nm in ("west", "east", "back"):
            f.fill(hm, H.lin((0, 0), (0, f.h), [(0, pal["l"]), (0.35, pal["m"]), (1, H.tone(pal["m"], 0.72))]))
            for x in (1.4, 3.0, 4.7, 6.3):
                f.fill(f.m_line((x, 0.0), (x + 0.2, f.h * 0.9), 0.22, 0.14) * hm, pal["d"], 0.4)
            f.fill(f.m_line((0.0, 0.0), (f.w, 0.0), 0.5) * hm, pal["l"], 0.0)
        elif nm == "top":
            f.fill(hm, H.rad((f.w * 0.35, f.h * 0.3), f.w * 0.9, [(0, pal["l"]), (0.6, pal["m"]), (1, H.tone(pal["m"], 0.78))]))
            for x in (1.2, 2.6, 4.0, 5.4, 6.8):
                f.fill(f.m_line((x, 0.0), (x, f.h), 0.22, 0.2) * hm, pal["d"], 0.35)
        else:
            f.fill(hm, H.tone(pal["m"], 0.7))
    if ears:
        for nm in ("west", "east"):
            ear(cv.face("head", nm, False), nm, skin, y=ear_y, earring=earring)


def ear(f, name, skin, y=4.7, earring=None, jewel=None, mask=None):
    """An ear on a side face of the head (west = the entity's right side, its front at the right end): a skin ellipse with an inner shadow and a rim; optionally a stud and a drop earring
    (colour, (gem colour, glow))."""
    cx = 3.85 if name == "west" else 4.15
    s0, s1, s2 = skin
    m = f.m_ellipse(cx, y, 0.95, 1.3) if mask is None else f.m_ellipse(cx, y, 0.95, 1.3) * mask
    f.fill(m, H.lin((0, y - 1.3), (0, y + 1.3), [(0, s2), (0.5, s1), (1, s0)]))
    f.fill(f.m_ellipse(cx + (0.1 if name == "west" else -0.1), y + 0.05, 0.45, 0.85) * (1.0 if mask is None else mask), H.tone(s0, 0.84), 0.75)
    f.fill(f.m_ring(cx, y, 1.02, 0.2) * f.m_rect(0, y - 1.6, 8, y + 1.6) * (1.0 if mask is None else mask), H.tone(s0, 0.7), 0.55)
    f.fill(f.m_ellipse(cx - 0.25, y - 0.7, 0.3, 0.2), H.tone(s2, 1.05), 0.6)
    if earring:
        H.stud(f, cx, y + 1.35, 0.3, earring, spec=0.9)
        f.fill(f.m_line((cx, y + 1.5), (cx, y + 2.3), 0.22, 0.2), earring, 0.95)
        if jewel:
            H.gem(f, cx, y + 2.45, 0.28, jewel[0], setting=earring, facets=4, spec=0.8, glow=jewel[1])


def face_base(f, skin, side=0.09, chin=0.07):
    """The skin of the front of the head, with the rounded sides and the chin shade (no hair: the fringe is drawn on top)."""
    H.skin_face(f, skin, hair=None, side=side, chin=chin, shadow=0.0)


def fringe_mass(f, pal, poly, y_top=0.0, y_bot=2.2):
    """The hair mass of the forehead (a polygon of the face, model pixels) with the soft shadow it throws on the skin below it. Returns its mask."""
    mass = f.m_poly(poly)
    ymax = max(p[1] for p in poly)
    f.shade((1.0 - mass) * np.clip(1.0 - (f.Y - ymax + 0.3) / 2.2, 0, 1) * (f.Y > ymax - 0.5), 0.87)
    f.fill(mass, H.lin((0, y_top), (0, y_bot), [(0, H.mix(pal["d"], pal["m"], 0.45)), (0.5, pal["m"]), (1, H.tone(pal["m"], 0.9))]))
    return mass


def lock(f, pal, cx, w, top, length, lean, shade=1.0, edge_w=0.2, curve=1.6):
    """One tapered lock of hair hanging from y=top: a teardrop that leans, a dark edge on its shadow side, a light streak down its middle."""
    n = 9
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** curve)
        x = cx + lean * t * t
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["m"], pal["d"], 0.25 * (1 - shade))), (0.55, H.mix(pal["l"], pal["m"], 0.2 * (1 - shade))), (1, H.mix(pal["m"], pal["d"], 0.3 + 0.3 * (1 - shade)))]))
    f.fill(f.m_polyline(left[2:], edge_w) * m, pal["d"], 0.8)
    f.fill(f.m_polyline(right[2:], edge_w) * m, H.mix(pal["d"], "#000000", 0.25), 0.85)
    mid = [(cx + lean * (i / n) ** 2 * 0.9, top + length * 0.12 + length * 0.7 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.22) * m, pal["h"], 0.55 * shade)
    return m


def strands(f, pal, xs, y0, y1, mask, a=0.35):
    for x in xs:
        f.fill(f.m_line((x, y0), (x + 0.12, y1), 0.22, 0.2) * mask, pal["h"], a)


def spike(f, pal, cx, w, top, length, lean, shade=1.0, edge_w=0.2):
    """A thick pointed clump of hair (a spike): wide at the root, a pointed tip that leans, a dark shadow edge, a light streak."""
    n = 8
    left, right = [], []
    for i in range(n + 1):
        t = i / n
        half = w / 2.0 * (1.0 - t ** 1.25)
        x = cx + lean * t ** 1.5
        y = top + length * t
        left.append((x - half, y))
        right.append((x + half, y))
    m = f.m_poly(left + right[::-1])
    f.fill(m, H.lin((cx - w / 2.0, top), (cx + w / 2.0, top + length), [(0, H.mix(pal["l"], pal["m"], 0.3 + 0.4 * (1 - shade))), (0.5, pal["m"]), (1, H.mix(pal["m"], pal["d"], 0.55))]))
    f.fill(f.m_polyline(left[1:], edge_w) * m, pal["d"], 0.75)
    f.fill(f.m_polyline(right[1:], edge_w) * m, H.mix(pal["d"], "#000000", 0.2), 0.9)
    mid = [(cx - w * 0.12 + lean * (i / n) ** 1.5 * 0.9, top + length * 0.1 + length * 0.62 * (i / n)) for i in range(n)]
    f.fill(f.m_polyline(mid, 0.22) * m, pal["h"], 0.6 * shade)
    return m


# ================================================================================================ garment pieces
def cuff(f, y0, y1, color, pipe=None, stud_color=None, outline=0.6, studs=True, spec=0.3):
    """A bulky cuff band y0..y1: a bevelled trim band, a lit top edge, a piping line and two studs."""
    band(f, y0, y1, color, spec=min(spec, 0.15), outline=outline)
    if pipe:
        seam(f, [(0.12, (y0 + y1) / 2.0), (f.w - 0.12, (y0 + y1) / 2.0)], pipe, 0.065, 0.85, (0.3, 0.18), 0.2)
    if studs and stud_color:
        for x in (0.6, f.w - 0.6):
            H.stud(f, x, (y0 + y1) / 2.0, min(0.17, (y1 - y0) * 0.22), stud_color, spec=0.6)
    H.shadow_under(f, y1, 0.5, 0.2)


def belt_band(f, y0, y1, color, stitch=None, edge_k=1.45):
    """A strap across a face y0..y1: gradient, a lit top edge, a dark lower edge, optional stitching; a cast shadow under it."""
    f.fill(f.m_rect(0.0, y0, f.w, y1), H.lin((0, y0), (0, y1), [(0, lit(color, edge_k)), (0.35, color), (1, H.tone(color, 0.68))]))
    f.fill(f.m_rect(0.0, y0, f.w, y0 + 0.18), lit(color, 1.9), 0.45)
    f.fill(f.m_rect(0.0, y1 - 0.14, f.w, y1), "#0a0809", 0.55)
    if stitch:
        seam(f, [(0.0, y0 + (y1 - y0) * 0.5), (f.w, y0 + (y1 - y0) * 0.5)], stitch, 0.055, 0.7, (0.3, 0.18), 0.2)
    H.shadow_under(f, y1, 0.5, 0.28)


def buckle(f, cx, cy, w, h, color, hole="#1c130c", r=0.22, spec=0.7):
    """A rectangular belt buckle: a dark rim, a bevelled metal frame, a dark opening and a prong."""
    x0, x1, y0, y1 = cx - w / 2.0, cx + w / 2.0, cy - h / 2.0, cy + h / 2.0
    f.rect(x0 - 0.07, y0 - 0.07, x1 + 0.07, y1 + 0.07, H.tone(color, 0.35), r=r + 0.05)
    f.rect(x0, y0, x1, y1, H.metal_paint(y0, y1, color, 1.15), r=r)
    H.bevel(f, f.m_rect(x0, y0, x1, y1, r), depth=0.22, gain=1.3, spec=spec)
    f.rect(x0 + w * 0.2, y0 + h * 0.22, x1 - w * 0.2, y1 - h * 0.22, hole, r=0.1)
    f.line((x0 + w * 0.32, cy), (cx + w * 0.1, cy), H.metal_paint(cy - 0.2, cy + 0.2, color, 1.1), 0.24, 0.2)
    f.ellipse(x0 + w * 0.22, y0 + h * 0.14, 0.2, 0.11, "#fffbe0", a=0.8)


def boot(f, color, y0, name, strap=None, strap_color="#8a6a48", gold="#e0b43e", cuff_color=None, cuff_h=0.9, toe=True, sole="#0d0b0a", welt="#6e4e38", shaft_end=11.25, laces=False):
    """A boot from y0 down to the foot line (shaft_end) with welt and sole to 12: a gradient, a sheen, ankle wrinkles, an optional turned cuff, an optional strap with a buckle across the shin and
    a polished toe cap."""
    boot_m = f.m_band(y0, shaft_end)
    f.fill(boot_m, H.lin((0, y0), (0, shaft_end), [(0, lit(color, 1.35)), (0.5, color), (1, H.tone(color, 0.72))]))
    side = np.minimum(f.X, f.w - f.X)
    f.shade(boot_m, 1.0 - 0.14 * (1.0 - H.smoothstep(0.0, 0.8, side)), 0.9)
    H.sheen(f, boot_m, [f.w * 0.3], width=0.2, strength=0.26, tilt=0.04, y0=y0 + 0.5, y1=10.6, color="#c8a898")
    f.fill(f.m_polyline([(0.0, 9.45), (f.w * 0.5, 9.7), (f.w, 9.5)], 0.14), H.tone(color, 0.45), 0.7)
    f.fill(f.m_polyline([(0.0, 9.78), (f.w * 0.5, 10.0), (f.w, 9.8)], 0.22), H.tone(color, 1.8), 0.3)
    if cuff_color:
        band(f, y0, y0 + cuff_h, cuff_color)
        H.shadow_under(f, y0 + cuff_h, 0.55, 0.3)
    if strap and name == "front":
        sy = y0 + cuff_h + 0.9 if cuff_color else y0 + 1.0
        f.rect(0.0, sy, f.w, sy + 0.8, H.tone(strap_color, 1.05), r=0.0)
        f.fill(f.m_rect(0.0, sy, f.w, sy + 0.2), lit(strap_color, 1.6), 0.8)
        f.fill(f.m_rect(0.0, sy + 0.7, f.w, sy + 0.8), "#0e0a08", 0.7)
        H.stud(f, f.w * 0.5, sy + 0.4, 0.3, gold)
    if toe and name == "front":
        f.ellipse(f.w * 0.5, 10.25, 1.2, 0.65, lit(color, 1.9), a=0.38, soft=2.0)
    f.fill(f.m_band(shaft_end - 0.25, shaft_end + 0.1), welt, 0.95)
    f.fill(f.m_band(shaft_end + 0.05, 12.0), H.lin((0, shaft_end + 0.05), (0, 12.0), [(0, "#1c1512"), (1, sole)]))
    f.fill(f.m_band(shaft_end - 0.25, shaft_end - 0.12), lit(welt, 1.9), 0.6)


def glove(f, color, y0, y1, name, finger_n=3, cuff_color=None, cuff_h=0.7, knuckle_k=1.25, thumb=True):
    """A glove from y0 to y1 (the fingers end at y1): a smooth gradient, finger gaps on the front and the back, a knuckle line, a thumb crease on the sides, an optional turned cuff."""
    m = f.m_band(y0, y1)
    cloth(f, m, color, top=1.12, bottom=0.84, round_=0.1)
    gap = deep(color, 0.55)
    if name in ("front", "back"):
        for i in range(1, finger_n + 1):
            x = f.w * i / (finger_n + 1)
            f.fill(f.m_line((x, y1 - 1.5), (x, y1 - 0.02), 0.24, 0.2) * m, gap, 0.8)
        f.fill(f.m_band(y1 - 1.6, y1 - 1.45) * m, gap, 0.45)
        f.fill(f.m_ellipse(f.w * 0.5, y1 - 2.2, f.w * 0.36, 0.28) * m, lit(color, knuckle_k), 0.5)
    elif thumb:
        f.fill(f.m_line((f.w * 0.28, y1 - 3.0), (f.w * 0.4, y1 - 1.1), 0.24, 0.2) * m, gap, 0.7)
    f.fill(f.m_band(y1 - 0.18, y1) * m, gap, 0.6)
    if cuff_color:
        H.trim_band(f, y0, y0 + cuff_h, cuff_color, spec=0.35, outline=0.6)
        H.shadow_under(f, y0 + cuff_h, 0.5, 0.26)


def trousers(f, color, y0, y1, name, knee=True, fold_xs=(1.2, 2.8), seam_color="#8a7e9a", top=1.3, bottom=0.8):
    cloth(f, f.m_band(y0, y1), color, top=top, bottom=bottom, round_=0.12, fold_xs=list(fold_xs), fold_depth=0.10, fold_y0=y0 + 0.4, fold_y1=y1 - 0.4)
    if knee:
        for y, k in ((2.6, 0.6), (3.7, 0.45)):
            f.fill(f.m_polyline([(0.2, y), (f.w * 0.5, y + 0.28), (f.w - 0.2, y + 0.05)], 0.14) * f.m_band(y0, y1), H.tone(color, 0.5), k)
            f.fill(f.m_polyline([(0.2, y - 0.15), (f.w * 0.5, y + 0.13), (f.w - 0.2, y - 0.10)], 0.22) * f.m_band(y0, y1), H.tone(color, 1.8), k * 0.45)
    if name in ("west", "east"):
        seam(f, [(f.w * 0.5, y0 + 0.2), (f.w * 0.5, y1 - 0.4)], seam_color, 0.06, 0.55)


def glow_line(f, pts, color, width=0.22, a=0.95, glow=True, core="#ffffff", core_a=0.5):
    """A line of light on the skin: a coloured stroke with a bright core and a glow-map twin."""
    m = f.m_polyline(pts, width)
    f.fill(m, color, a, glow=glow)
    f.fill(f.m_polyline(pts, width * 0.4) * m, core, core_a)
    return m


def pair(cv, part, name):
    """(base island, second-layer island) of a face: draw the crisp things on BOTH (the game drops the second layer and the hair beyond 24 blocks, PlayerLod)."""
    return A.lay(cv, part, name)


# ================================================================================================ added for group bc4
def each(cv, part, fn, names=SIDES):
    """Calls fn(face, name, is_overlay) for every side of a part, base layer then second layer: the crisp things are drawn on both (the second layer and the hair are dropped beyond 24 blocks)."""
    for nm in names:
        for f, isov in zip(A.lay(cv, part, nm), (False, True)):
            fn(f, nm, isov)


def cap(cv, part, name, color, edge=None, k_top=1.2, overlay=False):
    """The top or bottom face of a part (shoulder board, collar top, sole): a plain gradient of `color`, optionally a darker rim."""
    f = cv.face(part, name, overlay)
    f.fill(f.m_all(), H.lin((0, 0), (0, f.h), [(0, lit(color, k_top)), (1, H.tone(color, 0.8))]))
    if edge is not None:
        inner = f.m_rect(0.28, 0.28, f.w - 0.28, f.h - 0.28)
        f.fill(1.0 - inner, edge, 0.85)
    return f


def fur_band(f, y0, y1, base, light, dark, tuft=0.55, seed=0, mask=None):
    """A band of fluffy fur from y0 to y1 across the whole width of a face: a mottled body with short curved strokes in three tones and a tufted lower edge that hangs `tuft` model pixels
    below y1 (keep y1 + tuft inside the face). Deterministic. Returns its mask."""
    n = max(4, int(round(f.w / 0.5)))
    pts = [(-0.3, y0 - 0.3), (f.w + 0.3, y0 - 0.3), (f.w + 0.3, y1 - 0.1)]
    for i in range(n, -1, -1):
        x = f.w * i / n
        ln = tuft * (0.45 + 0.55 * H.hsh(seed, i, 1))
        pts.append((x + 0.12 * (H.hsh(seed, i, 2) - 0.5), y1 + (ln if i % 2 == 0 else ln * 0.45)))
    pts.append((-0.3, y1 - 0.1))
    m = f.m_poly(pts)
    if mask is not None:
        m = m * np.asarray(mask, np.float32)
    f.fill(m, H.lin((0, y0), (0, y1 + tuft), [(0, light), (0.5, base), (1, dark)]))
    for i in range(int(f.w / 0.34) + 1):
        x = 0.17 + 0.34 * i
        for j in range(int((y1 - y0 + tuft) / 0.5) + 1):
            y = y0 + 0.5 * j + 0.2 * H.hsh(seed, i, j, 3)
            r = H.hsh(seed, i, j)
            col = light if r > 0.6 else (dark if r < 0.25 else base)
            lean = (H.hsh(seed, i, j, 4) - 0.5) * 0.3
            f.fill(f.m_line((x, y), (x + lean, y + 0.42), 0.22, 0.1) * m, col, 0.7)
    f.fill(f.m_polyline(pts[3:-1], 0.12) * m, dark, 0.55)
    return m


def scar(f, pts, color="#c98a7a", dark="#8a5448", width=0.22):
    f.fill(f.m_polyline(pts, width + 0.12), dark, 0.5)
    f.fill(f.m_polyline(pts, width), color, 0.9)


def muscle(f, mask, skin, line_pts, light_pts=None, k=0.7, a=0.5):
    """A shaded muscle edge: a dark curved line along a separation (`line_pts`), a lit line next to it (`light_pts`)."""
    f.fill(f.m_polyline(line_pts, 0.26) * mask, H.tone(skin[0], k), a)
    if light_pts:
        f.fill(f.m_polyline(light_pts, 0.3) * mask, H.tone(skin[2], 1.04), a * 0.7)


# ================================================================================================ added for group bc5
def sibling(name):
    """Loads tools/arc_hd/chars/<name>.py by path under a private module name (a form-2 module reuses the painter of its base character and swaps the palette)."""
    import importlib.util
    path = os.path.join(HERE, name + ".py")
    sp = importlib.util.spec_from_file_location("bc5_" + name, path)
    mod = importlib.util.module_from_spec(sp)
    sp.loader.exec_module(mod)
    return mod


def wraps(f, y0, y1, color, gap=None, slant=0.45, per=1.2, end_tail=True, knot_y=None):
    """A bandage wrap from y0 to y1 across a face: slanted turns, a dark gap (about a texel and a half) at the lower edge of every turn, a light streak on each turn, a rolled top edge. The turns run
    on all four sides, so the wrap is continuous round the limb. `end_tail` hangs a loose end from the top roll on the front."""
    gap = gap or deep(color, 0.45)
    m = f.m_band(y0, y1)
    f.fill(m, H.lin((0, y0), (0, y1), [(0, lit(color, 1.06)), (0.5, color), (1, H.tone(color, 0.84))]))
    ph = ((f.Y - slant * (f.X - f.w / 2.0) - y0) % per) / per
    f.shade(m * np.clip((0.34 - ph * per) / 0.12, 0, 1), 0.62, 0.95)
    f.fill(m * np.clip(1.0 - np.abs(ph - 0.5) / 0.18, 0, 1), "#ffffff", 0.16)
    roll = f.m_band(y0, y0 + 0.62)
    f.fill(roll, H.lin((0, y0), (0, y0 + 0.62), [(0, lit(color, 1.12)), (1, H.tone(color, 0.8))]))
    f.fill(f.m_rect(0.0, y0 - 0.02, f.w, y0 + 0.14), gap, 0.85)
    f.fill(f.m_rect(0.0, y0 + 0.55, f.w, y0 + 0.68), H.tone(color, 0.6), 0.7)
    H.shadow_under(f, y0 + 0.1, 0.6, 0.32)
    f.fill(f.m_band(y1 - 0.45, y1), H.tone(color, 0.8), 0.35)
    if end_tail and f.name == "front":
        tail = f.m_poly([(0.7, y0 + 0.5), (1.9, y0 + 0.5), (1.75, y0 + 2.3), (1.3, y0 + 2.0), (0.85, y0 + 2.4)])
        f.fill(tail, H.lin((0, y0 + 0.5), (0, y0 + 2.4), [(0, color), (1, H.tone(color, 0.78))]))
        f.fill(f.m_polyline([(0.7, y0 + 0.5), (0.85, y0 + 2.4), (1.3, y0 + 2.0), (1.75, y0 + 2.3), (1.9, y0 + 0.5)], 0.2), gap, 0.8)
    return m


def stubble(f, mask, color, a=0.5, step=0.34, seed=1, p=0.55):
    """A dither of short dark dots over a mask (a beard shadow): deterministic."""
    out = np.zeros_like(f.X)
    n = int(f.w / step) + 1
    k = int(f.h / step) + 1
    for i in range(n):
        for j in range(k):
            if H.hsh(seed, i, j) < p:
                x = step * (i + 0.5 + 0.3 * (H.hsh(seed, i, j, 7) - 0.5))
                y = step * (j + 0.5)
                out = np.maximum(out, f.m_ellipse(x, y, 0.13, 0.12))
    f.fill(out * np.asarray(mask, np.float32), color, a)


def jag(f, pts, color, width=0.2, a=0.95, glow=None, core=None, core_a=0.6, dark="#0a0610", dark_a=0.7):
    """A jagged line (a crack, a vein): a dark edge, the colour, an optional bright core and a glow-map twin."""
    f.fill(f.m_polyline(pts, width + 0.16), dark, dark_a)
    m = f.m_polyline(pts, width)
    f.fill(m, color, a, glow=glow)
    if core:
        f.fill(f.m_polyline(pts, width * 0.4) * m, core, core_a)
    return m


def plate(f, pts, color, edge=None, spec=0.4, depth=0.3, glow=None):
    """A bevelled plate (bone, armour) over a polygon of the face: a gradient, a bevel and a dark edge line."""
    m = f.m_poly(pts)
    ys = [p[1] for p in pts]
    f.fill(m, H.lin((0, min(ys)), (0, max(ys)), [(0, lit(color, 1.3)), (0.5, color), (1, H.tone(color, 0.62))]))
    H.bevel(f, m, depth=depth, gain=1.15, spec=spec)
    if edge is not None:
        f.fill(f.m_polyline(pts + [pts[0]], 0.2) * m, edge, 0.85)
    if glow:
        f.glow_fill(f.m_polyline(pts + [pts[0]], 0.16) * m, glow, 0.7)
    return m


def clear_glow(f):
    """Erases the glow map of an island (the blueprint's glow of the 64x64 face is replaced by the redraw's own)."""
    f.glow[...] = 0.0


def spade(f, cx, cy, w, color, edge=None, spec=0.5, glow=None, a=1.0):
    """A spade (a heart turned over with a short stem) of width w centred on (cx, cy): a bevelled metal fill, a dark edge, a light on the left lobe. Returns its mask."""
    pts = H.heart_pts(0.0, 0.0, w)
    ys = [p[1] for p in pts]
    mid = (min(ys) + max(ys)) / 2.0
    top = [(cx + x, cy - (y - mid) - w * 0.08) for x, y in pts]
    h = (max(ys) - min(ys))
    base_y = cy + h / 2.0 - w * 0.08
    stem = [(cx - w * 0.07, base_y - w * 0.05), (cx + w * 0.07, base_y - w * 0.05), (cx + w * 0.2, base_y + w * 0.3), (cx - w * 0.2, base_y + w * 0.3)]
    m = np.maximum(f.m_poly(top), f.m_poly(stem))
    edge = edge or deep(color, 0.35)
    f.fill(m, H.lin((0, cy - h / 2.0), (0, cy + h / 2.0 + w * 0.3), [(0, lit(color, 1.35)), (0.5, color), (1, H.tone(color, 0.62))]), a)
    H.bevel(f, m, depth=max(0.14, w * 0.14), gain=1.1, spec=spec)
    ring = np.clip(m - H.blur(m, max(0.8, 0.07 * w * f.f)) * 1.0, 0, 1)
    f.fill(np.clip(ring * 2.4, 0, 1) * m, edge, 0.85 * a)
    if glow:
        f.glow_fill(f.m_ellipse(cx, cy, w * 0.28, w * 0.28, soft=3.0), glow, 0.85)
    return m

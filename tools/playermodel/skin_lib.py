"""Why: the characters of the player-model kit wear 64x64 skins (the vanilla layout: head, body, arms, legs and their hat, jacket, sleeve and pants layers, wide or slim arms). This is the
painter that turns a character sheet (skin tone, face, hair colour, outfit, armour, accessories, glow) into that skin and its glow map, so a skin can be redone at any time by a script
(tools/playermodel/gen_skins.py). The painting is pixel art: hard-edged tones from a ramp, a one-pixel shade under every hem, fabric flecks, and the face drawn the way the arc art kit draws it
(eyes, brows, nose, mouth, marks, masks). Garments are painted on the base layer AND on the overlay layer (jacket, sleeves, pants) so coats and robes have thickness.

Faces of a part, as the 64x64 sheet lays them out: `front` x runs from the entity's right (0) to its left; `back` runs the other way; `west` is the entity's right side with its front at the
RIGHT end of the rectangle, `east` the entity's left side with its front at the left end; `top` has its front edge at the bottom of the rectangle. Every painting function below is written in
(lx, ly, lz) cell coordinates of the part's box (lx from the entity's right, ly from the top, lz from the front), so nothing depends on those flips.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

from arc_kit import color as C  # noqa: E402


def hsh(*vals):
    h = 2166136261
    for v in vals:
        h = ((h ^ (int(v) & 0xFFFFFFFF)) * 16777619) & 0xFFFFFFFF
        h ^= h >> 15
    return (h & 0xFFFF) / 65535.0


# part name -> (box size w h d, base uv, overlay uv, overlay inflate) for the WIDE model; slim arms are 3 wide
PARTS = {
    "head": ((8, 8, 8), (0, 0), (32, 0)),
    "body": ((8, 12, 4), (16, 16), (16, 32)),
    "arm_r": ((4, 12, 4), (40, 16), (40, 32)),
    "arm_l": ((4, 12, 4), (32, 48), (48, 48)),
    "leg_r": ((4, 12, 4), (0, 16), (0, 32)),
    "leg_l": ((4, 12, 4), (16, 48), (0, 48)),
}


def rects(u, v, w, h, d):
    return {"west": (u, v + d, d, h), "front": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h), "top": (u + d, v, w, d),
            "bottom": (u + d + w, v, w, d)}


def cell(face, x, y, w, h, d):
    """The (lx, ly, lz) cell of a texel of a face of a w x h x d box."""
    if face == "front":
        return x, y, 0
    if face == "back":
        return w - 1 - x, y, d - 1
    if face == "west":
        return 0, y, d - 1 - x
    if face == "east":
        return w - 1, y, x
    if face == "top":
        return x, 0, d - 1 - y
    return x, h - 1, y


class Canvas:
    def __init__(self, slim=False):
        self.slim = slim
        self.px = np.zeros((64, 64, 4), np.uint8)

    def size(self, part):
        (w, h, d), _, _ = PARTS[part]
        if part.startswith("arm") and self.slim:
            w = 3
        return w, h, d

    def uv(self, part, overlay):
        _, b, o = PARTS[part]
        return o if overlay else b

    def put(self, x, y, rgba):
        if 0 <= x < 64 and 0 <= y < 64:
            self.px[y, x] = rgba

    def paint(self, part, fn, overlay=False, faces=("front", "back", "west", "east", "top", "bottom")):
        """Calls fn(face, lx, ly, lz, w, h, d) for every texel of the part's faces; fn returns an (r, g, b[, a]) tuple, or None to leave the texel alone."""
        w, h, d = self.size(part)
        u, v = self.uv(part, overlay)
        for face, (x0, y0, rw, rh) in rects(u, v, w, h, d).items():
            if face not in faces:
                continue
            for y in range(rh):
                for x in range(rw):
                    lx, ly, lz = cell(face, x, y, w, h, d)
                    c = fn(face, lx, ly, lz, w, h, d)
                    if c is None:
                        continue
                    self.put(x0 + x, y0 + y, c if len(c) == 4 else (c[0], c[1], c[2], 255))

    def front_rect(self, part, overlay=False):
        w, h, d = self.size(part)
        u, v = self.uv(part, overlay)
        return rects(u, v, w, h, d)["front"]

    def image(self):
        return Image.fromarray(self.px, "RGBA")


def clamp8(v):
    return max(0, min(255, int(round(v))))


def shade(c, k):
    return (clamp8(c[0] * k), clamp8(c[1] * k), clamp8(c[2] * k))


def fleck(c, x, y, seed, amt=0.05):
    k = 1.0 + (hsh(x, y, seed) - 0.5) * amt * 2
    return shade(c, k)


# ================================================================================================ the body
def paint_skin_tone(cv, tones, seed):
    """The bare body in the four tones of the skin (0 darkest .. 3 lightest): the light from the top, soft shade at the edges."""
    t = tones

    def fn(face, lx, ly, lz, w, h, d):
        base = t[2]
        if face == "top":
            base = t[3]
        elif face == "bottom":
            base = t[0]
        elif face in ("west", "east"):
            base = t[1]
        c = base
        if hsh(lx, ly, lz + seed) > 0.93:
            c = t[1] if base != t[1] else t[2]
        return c

    for part in PARTS:
        cv.paint(part, fn)


def paint_pecs(cv, tones, muscle):
    """A little muscle shading on a bare torso and arms: a chest line and abs, an arm highlight."""
    t = tones

    def torso(face, lx, ly, lz, w, h, d):
        if face != "front":
            return None
        if ly == 3 and lx in (1, 2, 5, 6):
            return t[0]
        if ly in (5, 7) and lx in (3, 4):
            return t[1]
        if lx in (3, 4) and 4 <= ly <= 8 and ly % 2 == 0:
            return t[1]
        return None

    cv.paint("body", torso)
    if muscle:
        def arm(face, lx, ly, lz, w, h, d):
            if face in ("west", "east", "front") and ly == 3:
                return t[1]
            return None
        cv.paint("arm_r", arm)
        cv.paint("arm_l", arm)


# ================================================================================================ the face
EYE_SHAPES = {
    # name: (rows of the eye as strings: 'w' white, 'i' iris, 'p' pupil, 'l' lid/lash, '.' skin) for the viewer's LEFT eye (the entity's right), 2 or 3 wide
    "round": ["ww", "ip"],
    "big": ["ww", "ip", "ii"],
    "sharp": ["lll", ".ip"],
    "narrow": ["lll", "ipw"],
    "slit": ["lll", ".pi"],
    "smile": ["...", "lll"],
    "closed": ["...", "lll"],
    "tired": ["lll", "wip", "..."],
    "wide": ["www", "ipw"],
    "fierce": ["lll", "ipi"],
    "glow": ["ww", "ii"],
    "empty": ["ww", "ww"],
}


def paint_face(cv, spec, tones, seed, hair_rgb):
    """The face on the front of the head (and the hat layer for masks). `spec` is the normalized face dict of the arc kit."""
    f = spec["face"]
    t = tones
    eye_rgb = C.rgb(f.get("eye_color") or "#4a6aa8")
    glow_rgb = C.rgb(f["eye_glow"]) if f.get("eye_glow") else None
    brow_rgb = C.rgb(f["brow_color"]) if f.get("brow_color") else hair_rgb
    lip_rgb = C.rgb(f["lip_color"]) if f.get("lip_color") else C.mix(t[1], (150, 60, 70), 0.55)
    ink = (22, 20, 30)
    white = (240, 238, 244)
    if f.get("sclera"):
        white = C.rgb(f["sclera"])
    style = f.get("eyes") or "round"
    shape = EYE_SHAPES.get(style, EYE_SHAPES["round"])
    fx, fy, fw, fh = cv.front_rect("head")
    eyeshift = f.get("eye_shift", 0)
    glow = []

    def px(x, y, c, g=False):
        cv.put(fx + x, fy + y, c + (255,) if len(c) == 3 else c)
        if g:
            glow.append((fx + x, fy + y, c))

    # eyes: two of them, mirrored; the viewer's left eye at x 1..3, the right at x 4..6 (row 4 is the top of the eye)
    y0 = 4 + eyeshift
    ew = len(shape[0])
    for side in (0, 1):
        for r, row in enumerate(shape):
            for k, ch in enumerate(row):
                x = 1 + k if side == 0 else 6 - k
                if ch == ".":
                    continue
                if ch == "w":
                    c = white
                elif ch == "i":
                    c = eye_rgb
                elif ch == "p":
                    c = ink
                else:
                    c = shade(t[0], 0.55) if style not in ("sharp", "narrow", "slit", "fierce", "tired") else ink
                if style == "glow" and glow_rgb:
                    c = glow_rgb
                px(x, y0 + r, c, g=(glow_rgb is not None and ch in ("i", "p", "w") and (f.get("eye_glow") is not None)))
    if f.get("lashes"):
        for x in (1, 6):
            px(x, y0 - 1, ink)
    # brows
    brow = f.get("brows") or "flat"
    by = y0 - 1 - (1 if style in ("sharp", "narrow", "slit", "fierce", "tired") else 0)
    if by >= 1:
        thick = brow in ("thick", "angry", "heavy")
        for side in (0, 1):
            xs = (1, 2, 3) if side == 0 else (6, 5, 4)
            for j, x in enumerate(xs[:3 if thick else 2]):
                yy = by
                if brow == "angry":
                    yy = by + (1 if j == 2 else 0)
                elif brow == "sad":
                    yy = by + (1 if j == 0 else 0)
                elif brow in ("arched", "raised"):
                    yy = by - (1 if (j == 1 or brow == "raised") else 0)
                px(x, yy, brow_rgb)
    # nose
    nose = f.get("nose") or "small"
    if nose != "none":
        px(3, 6, t[1])
        px(4, 6, t[1])
        if nose == "wide":
            px(3, 5, t[1])
            px(4, 5, t[1])
        elif nose == "sharp":
            px(3, 5, t[1])
    # mouth
    mouth = f.get("mouth") or "neutral"
    my = 7
    mc = lip_rgb
    if mouth == "smile":
        px(2, my - 0, mc)
        px(5, my - 0, mc)
        px(3, my, mc)
        px(4, my, mc)
    elif mouth == "smirk":
        px(3, my, mc)
        px(4, my, mc)
        px(5, my - 1, mc)
    elif mouth == "frown":
        px(3, my, mc)
        px(4, my, mc)
    elif mouth == "grin":
        for x in range(2, 6):
            px(x, my, white if x in (3, 4) else mc)
    elif mouth == "open":
        px(3, my, (70, 20, 30))
        px(4, my, (70, 20, 30))
    elif mouth == "fangs":
        px(3, my, mc)
        px(4, my, mc)
        px(2, my, white)
        px(5, my, white)
    elif mouth == "line":
        for x in (2, 3, 4, 5):
            px(x, my, shade(t[0], 0.8))
    elif mouth == "pout":
        px(3, my, mc)
        px(4, my, mc)
    elif mouth != "none":
        px(3, my, mc)
        px(4, my, mc)
    # marks
    for m in f.get("marks") or []:
        kind = m if isinstance(m, str) else m.get("kind")
        col = None if isinstance(m, str) else m.get("color")
        mark(cv, fx, fy, kind, t, seed, col, glow, white)
    return glow


def mark(cv, fx, fy, kind, t, seed, col, glow, white):
    scar = shade(t[1], 0.72)
    scar2 = C.mix(t[3], (210, 120, 120), 0.4)

    def px(x, y, c, g=False):
        cv.put(fx + x, fy + y, c + (255,))
        if g:
            glow.append((fx + x, fy + y, c))

    if kind in ("scar_cheek_l",):
        for i in range(3):
            px(6 - (1 if i else 0), 5 + i, scar)
    elif kind == "scar_cheek_r":
        for i in range(3):
            px(1 + (1 if i else 0), 5 + i, scar)
    elif kind in ("scar_eye_l", "scar_brow_l"):
        for i in range(3):
            px(5 + (i // 2), 2 + i, scar)
    elif kind in ("scar_eye_r", "scar_brow_r"):
        for i in range(3):
            px(2 - (i // 2), 2 + i, scar)
    elif kind in ("scar_diag", "scar_diag_r"):
        for i in range(5):
            px(1 + i if kind == "scar_diag_r" else 6 - i, 2 + i, scar)
    elif kind == "stubble":
        for y in (6, 7):
            for x in range(1, 7):
                if hsh(x, y, seed, 5) > 0.5:
                    px(x, y, shade(t[1], 0.8))
    elif kind in ("moustache",):
        for x in (2, 3, 4, 5):
            px(x, 6, shade(t[0], 0.6))
    elif kind in ("goatee", "beard"):
        for x in (3, 4):
            px(x, 7, shade(t[0], 0.6))
        if kind == "beard":
            for x in (1, 2, 5, 6):
                px(x, 7, shade(t[0], 0.65))
            px(1, 6, shade(t[0], 0.65))
            px(6, 6, shade(t[0], 0.65))
    elif kind == "blush":
        for x in (1, 2, 5, 6):
            px(x, 6, C.mix(t[2], (240, 120, 130), 0.45))
    elif kind == "lipstick":
        pass
    elif kind == "eye_bags":
        for x in (1, 2, 5, 6):
            px(x, 6, shade(t[1], 0.88))
    elif kind in ("cracks", "veins"):
        cc = C.rgb(col) if col else (180, 120, 255)
        for (x, y) in ((1, 2), (1, 3), (2, 4), (6, 3), (5, 5), (6, 6), (4, 2)):
            px(x, y, cc, g=True)
    elif kind in ("forehead_sigil", "forehead_gem", "third_eye"):
        cc = C.rgb(col) if col else (255, 90, 90)
        px(3, 2, cc, g=True)
        px(4, 2, cc, g=True)
        if kind == "forehead_sigil":
            px(3, 1, cc, g=True)
            px(4, 1, cc, g=True)
    elif kind == "mole":
        px(2, 6, shade(t[0], 0.5))
    elif kind == "war_paint":
        for x in (1, 2, 5, 6):
            px(x, 5, (170, 40, 40))
    elif kind in ("eyeshadow",):
        for x in (1, 2, 5, 6):
            px(x, 3, C.mix(t[2], (120, 60, 150), 0.5))
    elif kind in ("cheek_lines", "light_lines"):
        for x in (1, 6):
            px(x, 6, shade(t[1], 0.8))
            px(x, 7, shade(t[1], 0.8))


def paint_mask(cv, mask, tones, seed):
    """A face mask on the hat layer's front: skull, full, half, visor, oni, veil, plague, fox."""
    kind = mask.get("kind", "half")
    col = C.rgb(mask.get("color") or "#d8d0b4")
    col2 = C.rgb(mask.get("color2") or "#2a2024")
    eg = C.rgb(mask["eye_glow"]) if mask.get("eye_glow") else None
    glow = []
    w, h, d = cv.size("head")
    u, v = cv.uv("head", True)
    fx, fy, fw, fh = rects(u, v, w, h, d)["front"]

    def px(x, y, c, g=False):
        cv.put(fx + x, fy + y, c + (255,))
        if g:
            glow.append((fx + x, fy + y, c))

    rows = {"half": range(4, 8), "full": range(1, 8), "visor": range(3, 6), "skull": range(2, 8), "oni": range(2, 8), "veil": range(5, 8), "plague": range(3, 8), "fox": range(2, 8)}
    for y in rows.get(kind, range(4, 8)):
        for x in range(0, 8):
            c = shade(col, 0.92 + 0.1 * hsh(x, y, seed))
            if x in (0, 7):
                c = shade(col, 0.78)
            px(x, y, c)
    if kind in ("skull", "full", "oni", "fox", "plague"):
        for x in (1, 2, 5, 6):
            px(x, 4, col2, g=False)
            px(x, 5, col2)
        if eg:
            for x in (2, 5):
                px(x, 4, eg, g=True)
        px(3, 6, col2)
        px(4, 6, col2)
        for x in (2, 3, 4, 5):
            if kind == "skull":
                px(x, 7, col2 if x % 2 == 0 else col)
    if kind == "visor":
        for x in range(1, 7):
            px(x, 4, col2)
        if eg:
            for x in (2, 3, 4, 5):
                px(x, 4, eg, g=True)
    # the same mask carries round to the sides and back of the hat layer so it is not a sticker
    return glow


# ================================================================================================ the outfit
def tone_of(c, n=5, contrast=1.0):
    return C.shades(c, n, contrast)


def paint_torso(cv, top, inner, sleeves, seed, tones, legs_color):
    """The shirt, coat, robe ... on the body and its jacket layer, and the arms' sleeves. `top` is the normalized outfit.top dict."""
    kind = top["kind"]
    base = top.get("color") or "#4a5a7a"
    pal = tone_of(base)
    trim = C.rgb(top["trim"]) if top.get("trim") else None
    if kind == "bare":
        return
    length = top.get("length") or "hip"
    collar = top.get("collar") or "none"
    open_front = bool(top.get("open"))
    inner_kind = inner.get("kind")
    inner_pal = tone_of(inner.get("color") or "#ecebea")
    pattern = top.get("pattern") or "plain"
    pat_col = C.rgb(top["pattern_color"]) if top.get("pattern_color") else None
    emblem = top.get("emblem")

    def torso(face, lx, ly, lz, w, h, d):
        c = pal[2]
        if kind == "tank":
            if face in ("front", "back"):
                if lx in (0, 7) and ly < 4 and ly > 0:
                    return None                      # the straps leave the shoulders bare
                if ly < 2 and lx in (1, 6) and False:
                    return None
            else:
                if ly < 5:
                    return None
        if kind == "vest" and face == "front":
            if 2 <= lx <= 5 and ly >= 1:
                if inner_kind in (None, "bare", "none"):
                    return None
                c = inner_pal[2]
                return c
        if open_front and face == "front" and lx in (3, 4) and ly >= 1:
            if inner_kind in (None, "bare", "none"):
                return tones[1]
            return inner_pal[2] if ly % 2 == 0 else inner_pal[1]
        if face == "top":
            c = pal[3]
        elif face == "bottom":
            c = pal[0]
        elif face in ("west", "east"):
            c = pal[1]
        if face in ("front", "back") and pattern not in ("plain", None):
            c = pattern_color(pattern, pal, pat_col, lx, ly, seed, c)
        if ly >= h - 1 and face != "top":
            c = pal[1]                                        # the shade under the hem
            if top.get("hem_trim") and trim:
                c = trim
        if ly == 0 and face != "bottom":
            c = pal[3]
        if collar != "none" and ly <= 1 and face in ("front", "back", "west", "east"):
            cc = C.rgb(top["collar_color"]) if top.get("collar_color") else (trim if trim else pal[3])
            if collar in ("stand", "high") or (collar == "v" and face != "front") or (collar == "round" and ly == 0) or collar == "fur":
                c = cc
            if collar == "v" and face == "front" and 3 <= lx <= 4 and ly <= 2:
                c = tones[2]
        if trim and face == "front" and (lx in (0, 7) and kind in ("coat", "robe", "uniform", "tabard") or (lx in (3, 4) and kind in ("coat", "uniform") and open_front)):
            c = trim
        if top.get("buttons") and face == "front" and lx in (3, 4) and ly % 3 == 2:
            c = pal[4]
        if emblem and emblem.get("kind") not in (None, "none") and face == ("back" if emblem.get("where") == "back" else "front"):
            e = emblem_pixel(emblem, lx, ly, pal, trim)
            if e is not None:
                c = e
        return fleck(c, lx, ly, seed, 0.03)

    cv.paint("body", torso)
    if kind in ("coat", "robe", "uniform", "tunic", "dress", "tabard", "jumpsuit", "shirt", "kimono"):
        cv.paint("body", torso, overlay=True)
    # sleeves
    sk = sleeves.get("kind", "auto")
    if sk == "auto":
        sk = {"coat": "long", "robe": "wide", "kimono": "wide", "uniform": "long", "tunic": "short", "dress": "none", "tabard": "none", "jumpsuit": "long", "shirt": "long", "tank": "none",
              "vest": "none"}.get(kind, "none")
    cuff = C.rgb(sleeves["cuff"]) if sleeves.get("cuff") else (trim if trim else None)
    scol = tone_of(sleeves["color"]) if sleeves.get("color") else pal
    rows = {"long": 12, "wide": 12, "bell": 12, "torn": 9, "short": 4, "none": 0}.get(sk, 0)

    def arm(face, lx, ly, lz, w, h, d):
        if ly >= rows:
            return None
        if face == "bottom":
            return scol[0]
        c = scol[2] if face in ("front", "back") else scol[1]
        if face == "top":
            c = scol[3]
        if ly == rows - 1:
            c = cuff if cuff else scol[1]
        if sk == "torn" and ly >= 6 and hsh(lx, ly, lz, seed) > 0.55:
            return None
        if ly == 0:
            c = scol[3]
        return fleck(c, lx, ly, seed + 3, 0.03)

    if rows:
        for p in ("arm_r", "arm_l"):
            cv.paint(p, arm)
            cv.paint(p, arm, overlay=True)
    # hem continuing over the legs for long garments
    reach = {"none": 0, "hip": 0, "thigh": 4, "knee": 7, "calf": 9, "ankle": 11, "floor": 12, "mini": 2, "long": 11}.get(length, 0)
    if kind in ("dress", "robe", "coat", "tunic", "tabard", "uniform", "kimono") and reach > 0:
        def leg(face, lx, ly, lz, w, h, d):
            if ly >= reach or face in ("top",):
                return None
            if face == "bottom":
                return None
            c = pal[2] if face in ("front", "back") else pal[1]
            if face in ("east", "west") and kind in ("coat", "uniform", "tunic") and False:
                return None
            if ly == reach - 1:
                c = trim if (trim and top.get("hem_trim")) else pal[1]
            if kind in ("coat", "uniform", "tunic") and face in ("east", "west") and lx in (0, 3) and False:
                c = pal[1]
            return fleck(c, lx, ly, seed + 9, 0.03)
        for p in ("leg_r", "leg_l"):
            cv.paint(p, leg, overlay=True)
            cv.paint(p, leg)


def pattern_color(pattern, pal, pat_col, lx, ly, seed, c):
    pc = pat_col if pat_col else pal[3]
    if pattern == "stripes":
        return pc if ly % 3 == 0 else c
    if pattern == "checks":
        return pc if (lx + ly) % 2 == 0 else c
    if pattern == "patches":
        return pc if (hsh(lx // 3, ly // 3, seed) > 0.78) else c
    if pattern == "runes":
        return pc if (hsh(lx, ly, seed) > 0.86 and lx % 2 == 1) else c
    if pattern == "dots":
        return pc if lx % 3 == 1 and ly % 3 == 1 else c
    if pattern == "scales":
        return pal[3] if (lx + (ly // 2) * 2) % 4 == 0 and ly % 2 == 0 else c
    if pattern == "flames":
        return pc if ly >= 7 and (lx + ly) % 3 == 0 else c
    if pattern == "diag":
        return pc if (lx + ly) % 4 == 0 else c
    return c


def emblem_pixel(emblem, lx, ly, pal, trim):
    col = C.rgb(emblem["color"]) if emblem.get("color") else (trim if trim else pal[4])
    size = int(emblem.get("size") or 6)
    cx, cy = 3.5 + emblem.get("dx", 0) / 2.0, 2.0 + (emblem.get("y", 3) / 2.0)
    r = max(1.0, min(3.0, size / 3.0))
    d = abs(lx - cx) + abs(ly - cy)
    if d <= r:
        return col if d < r - 0.5 else shade(col, 0.8)
    return None


def paint_legs(cv, legs, boots, seed, tones, top_kind):
    kind = legs.get("kind", "trousers")
    pal = tone_of(legs.get("color") or "#2c2c38")
    bk = boots.get("kind", "boots")
    bpal = tone_of(boots.get("color") or "#2a2220")
    bh = {"ankle": 3, "calf": 6, "knee": 8}.get(boots.get("height", "calf"), 6)
    bcuff = C.rgb(boots["cuff"]) if boots.get("cuff") else None
    chaps = C.rgb(legs.get("chaps") or "#c8b48c")
    rows = {"trousers": 12, "leggings": 12, "baggy": 12, "hakama": 12, "chaps": 12, "shorts": 5, "skirt": 4, "bare": 0}.get(kind, 12)
    stripe = C.rgb(legs["stripe"]) if legs.get("stripe") else None

    def leg(face, lx, ly, lz, w, h, d):
        if face == "top":
            return None
        if bk != "bare" and ly >= h - bh and bk in ("boots", "shoes", "sandals") and (bk != "sandals" or ly >= h - 2):
            if bk == "shoes" and ly < h - 2:
                pass
            elif bk == "sandals":
                if lx in (0, 3) and ly == h - 1 and False:
                    return None
                c = bpal[2] if (lx + ly) % 2 == 0 else tones[2]
                if face == "bottom":
                    return bpal[0]
                return c
            else:
                c = bpal[2] if face in ("front", "back") else bpal[1]
                if face == "bottom":
                    return bpal[0]
                if ly == h - 1:
                    c = bpal[0]
                elif ly == h - bh and bcuff:
                    c = bcuff
                elif ly == h - bh:
                    c = bpal[3]
                return fleck(c, lx, ly, seed + 5, 0.03)
        if ly >= rows:
            return None
        if kind == "bare":
            return None
        c = pal[2] if face in ("front", "back") else pal[1]
        if kind == "leggings":
            c = pal[1] if face in ("front", "back") else pal[0]
            if lx == 1 and face == "front":
                c = pal[2]
        if kind == "chaps" and face in ("front", "west", "east") and lx in (0, 3):
            c = chaps
        if stripe and face in ("west", "east"):
            c = stripe if lz in (1, 2) else c
        if ly == rows - 1 and kind in ("shorts", "skirt", "baggy", "hakama"):
            c = pal[1]
        if kind in ("baggy", "hakama") and ly > 6 and hsh(lx, ly, seed) > 0.8:
            c = pal[3]
        return fleck(c, lx, ly, seed + 7, 0.03)

    for p in ("leg_r", "leg_l"):
        cv.paint(p, leg)
        if kind != "bare":
            cv.paint(p, leg, overlay=True)


def paint_gloves(cv, gloves, tones, seed):
    kind = gloves.get("kind", "none")
    if kind in (None, "none"):
        return
    pal = tone_of(gloves.get("color") or "#2a2a30")
    side = gloves.get("side", "both")
    rows = {"gloves": 4, "gauntlets": 6, "fingerless": 3, "wraps": 5}.get(kind, 3)

    def hand(face, lx, ly, lz, w, h, d):
        if ly < h - rows or face == "top":
            return None
        if kind == "fingerless" and ly == h - 1:
            return None
        c = pal[2] if face in ("front", "back") else pal[1]
        if kind == "wraps" and (ly % 2 == 0):
            c = pal[3]
        if kind == "gauntlets" and ly == h - rows:
            c = pal[4]
        if face == "bottom":
            c = pal[0]
        return fleck(c, lx, ly, seed + 11, 0.03)

    for p in ("arm_r", "arm_l"):
        if side == "both" or (side == "right" and p == "arm_r") or (side == "left" and p == "arm_l"):
            cv.paint(p, hand)
            cv.paint(p, hand, overlay=True)


def paint_belt(cv, belt, seed, torso_kind):
    if not belt or torso_kind in ("bare",):
        return
    col = tone_of(belt.get("color") or "#3c281c")
    buckle = C.rgb(belt.get("buckle") or "#c8a040")
    kind = belt.get("kind", "plain")
    rows = (9, 10) if kind in ("double", "obi", "sash") else (9,)

    def b(face, lx, ly, lz, w, h, d):
        if ly not in rows or face in ("top", "bottom"):
            return None
        c = col[2] if ly == rows[0] else col[1]
        if kind in ("sash", "obi"):
            c = col[3] if ly == rows[0] else col[2]
        if face == "front" and lx in (3, 4) and ly == rows[0]:
            c = buckle
        if belt.get("pouches") and face == "front" and lx in (1, 6) and ly in (9, 10):
            c = col[3]
        return fleck(c, lx, ly, seed + 13, 0.03)

    cv.paint("body", b)
    cv.paint("body", b, overlay=True)


def paint_armor(cv, armor, tones, seed):
    """Armour pieces painted onto the overlay layers (plates are 'thick' because the overlay is). Returns the glow texels."""
    glow = []
    if not armor or not armor.get("pieces"):
        return glow
    pal = tone_of(armor.get("color") or "#8c98a8", 5, 1.25)
    trim = C.rgb(armor.get("trim") or "#c8a040")
    glow_rgb = C.rgb(armor["glow"]) if armor.get("glow") else None
    style = armor.get("style", "plate")
    pieces = armor["pieces"]

    def plate(c, face, lx, ly, edge):
        if edge:
            return trim
        return pal[3] if face == "top" else (pal[2] if face in ("front", "back") else pal[1])

    if "breastplate" in pieces or "chest" in pieces or "cuirass" in pieces:
        def bp(face, lx, ly, lz, w, h, d):
            if ly > 8 or face in ("top", "bottom"):
                return None
            edge = ly in (0, 8) or lx in (0, 7) and face in ("front", "back")
            c = plate(None, face, lx, ly, edge)
            if face == "front" and lx in (3, 4) and ly in (3, 4) and glow_rgb:
                glow.append(("body_front", lx, ly, glow_rgb))
                return glow_rgb
            if style == "chain":
                c = pal[2] if (lx + ly) % 2 == 0 else pal[1]
            return fleck(c, lx, ly, seed + 21, 0.04)
        cv.paint("body", bp, overlay=True)
    if "pauldrons" in pieces or "shoulders" in pieces:
        def pd(face, lx, ly, lz, w, h, d):
            if ly > 3:
                return None
            if face == "bottom":
                return None
            edge = ly == 3
            return fleck(plate(None, face, lx, ly, edge), lx, ly, seed + 23, 0.04)
        cv.paint("arm_r", pd, overlay=True)
        cv.paint("arm_l", pd, overlay=True)
    if "bracers" in pieces or "vambraces" in pieces:
        def br(face, lx, ly, lz, w, h, d):
            if ly < 6 or ly > 10 or face in ("top", "bottom"):
                return None
            return fleck(plate(None, face, lx, ly, ly in (6, 10)), lx, ly, seed + 25, 0.04)
        cv.paint("arm_r", br, overlay=True)
        cv.paint("arm_l", br, overlay=True)
    if "greaves" in pieces or "boots" in pieces or "legs" in pieces:
        def gr(face, lx, ly, lz, w, h, d):
            if ly < 6 or face == "top":
                return None
            if face == "bottom":
                return pal[0]
            return fleck(plate(None, face, lx, ly, ly == 6), lx, ly, seed + 27, 0.04)
        cv.paint("leg_r", gr, overlay=True)
        cv.paint("leg_l", gr, overlay=True)
    if "helm" in pieces or "helmet" in pieces:
        def hm(face, lx, ly, lz, w, h, d):
            if face == "bottom":
                return None
            if face == "front" and 1 <= lx <= 6 and ly >= 3:
                if ly == 4:
                    if glow_rgb:
                        glow.append(("head_front", lx, ly, glow_rgb))
                        return glow_rgb
                    return (14, 14, 20)
                return None                                 # the face opening
            if face in ("west", "east") and lz < 2 and ly >= 3:
                return None
            if ly > 5 and face in ("west", "east", "back"):
                return None if ly > 6 else shade(pal[1], 1.0)
            return fleck(plate(None, face, lx, ly, ly == 0 or (face != "top" and ly == 5)), lx, ly, seed + 29, 0.04)
        cv.paint("head", hm, overlay=True)
    return glow


def paint_hair_scalp(cv, mask, rgb, seed):
    """Paints the scalp in the hair colour on the base head where the haircut covers it, so a head under a helmet or a hood still has hair colour, and the cap never sits on bare skin."""
    if mask is None:
        return
    pal = C.hair_shades(rgb, 4)

    def f(face, lx, ly, lz, w, h, d):
        if face == "bottom":
            return None
        if mask(face, lx, ly, lz):
            return pal[1] if (lx + ly + lz) % 3 else pal[2]
        return None

    cv.paint("head", f)


def paint_accessories(cv, accs, tones, hair_rgb, seed):
    """Flat accessories on the hat layer and the face: headband, bandana, glasses, eyepatch, goggles, earrings, mask_band, monocle."""
    glow = []
    w, h, d = cv.size("head")
    u, v = cv.uv("head", True)
    fr = rects(u, v, w, h, d)
    fx, fy = fr["front"][0], fr["front"][1]
    for a in accs:
        kind = a["kind"]
        col = C.rgb(a["color"]) if a.get("color") else None
        col2 = C.rgb(a["color2"]) if a.get("color2") else None
        if kind in ("headband", "bandana", "circlet", "tiara", "crown", "ribbon", "mask_band", "goggles"):
            c = col or (170, 40, 40)
            if kind in ("circlet", "tiara", "crown"):
                c = col or (224, 180, 72)
            row = 1 if kind in ("headband", "bandana", "mask_band", "goggles") else 1

            def band(face, lx, ly, lz, w_, h_, d_, c=c, row=row, kind=kind):
                if ly != row and not (kind == "bandana" and ly == row + 1 and face in ("front",)):
                    return None
                if face in ("top", "bottom"):
                    return None
                if kind in ("circlet", "tiara", "crown") and face == "front" and lx == 3 and col2:
                    return col2
                return shade(c, 0.9 + 0.2 * hsh(lx, lz, seed))
            cv.paint("head", band, overlay=True)
        elif kind in ("glasses", "monocle", "goggles_eyes"):
            c = col or (30, 30, 40)

            def gl(face, lx, ly, lz, w_, h_, d_, c=c, kind=kind):
                if face != "front":
                    return None
                if kind == "monocle":
                    return c if (lx in (5, 6) and ly in (3, 6)) or (lx in (4, 7) and ly in (4, 5)) else None
                return c if ly in (3, 6) and 0 < lx < 7 and lx not in (3, 4) or (ly in (4, 5) and lx in (0, 3, 4, 7)) else None
            cv.paint("head", gl, overlay=True)
        elif kind == "eyepatch":
            c = col or (20, 18, 24)

            def ep(face, lx, ly, lz, w_, h_, d_, c=c):
                if face != "front":
                    return None
                return c if (lx in (1, 2) and ly in (3, 4, 5)) or (lx == 0 and ly == 1) or (lx == 3 and ly == 2) else None
            cv.paint("head", ep, overlay=True)
        elif kind in ("earrings", "ear_cuffs"):
            c = col or (232, 190, 70)
            for face in ("west", "east"):
                xx, yy, rw, rh = fr[face]
                for y in (5, 6):
                    cv.put(xx + (3 if face == "west" else 4), yy + y, c + (255,))
        elif kind == "flower":
            c = col or (240, 120, 160)
            cv.put(fx + 6, fy + 0, c + (255,))
            cv.put(fx + 7, fy + 0, (col2 or (255, 230, 120)) + (255,))
    return glow

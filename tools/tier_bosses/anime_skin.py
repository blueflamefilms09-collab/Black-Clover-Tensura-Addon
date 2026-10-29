"""3.33.1: the anime skin style of the Tier List bosses (the owner's reference sheet of anime Minecraft skins): large shaded eyes with a lash line, iris gradient and a highlight, thin brows, a
tiny mouth, blush, soft light-to-shade gradients on every garment with cool shadows and warm lights, clean fabric (no noise), clearer folds and hems. It reuses the structure painters of the
player-model kit (tools/playermodel/skin_lib.py: garments, belts, gloves, armour, accessories, marks) and repaints the face and the lighting on top, so every outfit kind of the art kit still works.

    paint_character(spec, name, hair_masks) -> (skin image, glow image or None, hair style id, hair rgb, slim)      (the same result as gen_skins.paint_character)
"""
import os
import sys

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
for p in (os.path.join(TOOLS, "playermodel"), TOOLS):
    if p not in sys.path:
        sys.path.insert(0, p)

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import characters as CH  # noqa: E402
import gen_skins as GS  # noqa: E402
import skin_lib as K  # noqa: E402
from arc_kit import color as C  # noqa: E402

COOL = (70, 60, 130)
WARM = (255, 244, 226)
INK = (30, 24, 40)


def mixc(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def vivid(c, vmin=0.62, smin=0.45):
    """An iris colour that reads at 8 pixels: never darker than vmin or greyer than smin (the sheets give some eyes as near-black brown)."""
    import colorsys
    h, sat, v = colorsys.rgb_to_hsv(c[0] / 255.0, c[1] / 255.0, c[2] / 255.0)
    sat = max(sat, smin)
    v = max(v, vmin)
    r, g, b = colorsys.hsv_to_rgb(h, min(1.0, sat), min(1.0, v))
    return (int(r * 255), int(g * 255), int(b * 255))


def lit(c, k):
    """Light k (1 = as is, < 1 shadow, > 1 light) with the anime hue shift: shadows go cool and a little richer, lights go warm."""
    if k < 1.0:
        c = mixc(c, COOL, min(0.5, (1.0 - k) * 0.9))
        return tuple(max(0, min(255, int(v * (0.55 + 0.45 * k)))) for v in c)
    return mixc(c, WARM, min(0.55, (k - 1.0) * 1.6))


# ================================================================================================ the face
def paint_face(cv, spec, tones, seed, hair_rgb):
    f = spec["face"]
    t = tones
    eye = vivid(C.rgb(f.get("eye_color") or "#4a6aa8"))
    glow_rgb = C.rgb(f["eye_glow"]) if f.get("eye_glow") else None
    brow_rgb = lit(C.rgb(f["brow_color"]) if f.get("brow_color") else hair_rgb, 0.7)
    lip = C.rgb(f["lip_color"]) if f.get("lip_color") else mixc(t[1], (190, 80, 95), 0.5)
    style = f.get("eyes") or "round"
    expr = f.get("expression") or "calm"
    brow = f.get("brows") or "flat"
    fx, fy, fw, fh = cv.front_rect("head")
    glow = []

    def px(x, y, c, g=False):
        cv.put(fx + x, fy + y, tuple(c) + (255,))
        if g:
            glow.append((fx + x, fy + y, tuple(c)))

    # the face base: light from the top, a faint shade at the temples and under the chin, a blush on the cheeks
    for y in range(8):
        for x in range(8):
            k = 1.06 - 0.10 * (y / 7.0)
            if x in (0, 7):
                k -= 0.05
            if y == 7:
                k -= 0.03
            px(x, y, lit(t[2], k))
    sharp = style in ("sharp", "narrow", "slit", "fierce", "tired", "cold")
    big = style in ("big", "round", "wide")
    lash = lit(mixc(hair_rgb, INK, 0.65), 0.7)
    ey = 4
    bright = tuple(min(255, int(v * 1.0)) for v in eye)
    for side in (0, 1):
        xs = (1, 2) if side == 0 else (6, 5)
        outer, inner = xs
        flick = 0 if side == 0 else 7
        px(outer, ey, lash)
        px(inner, ey, lit(eye, 0.55))
        if f.get("lashes") or big:
            px(flick, ey, lash)
        if style == "tired":
            px(outer, ey + 1, lit(t[1], 0.9))
            px(inner, ey + 1, lit(eye, 0.6))
            continue
        if style == "glow" and glow_rgb:
            for yy in (ey + 1, ey + 2):
                for xx in xs:
                    px(xx, yy, glow_rgb, g=True)
            continue
        if sharp:
            px(outer, ey + 1, lit(eye, 0.8))
            px(inner, ey + 1, lit(eye, 1.3))
            if glow_rgb:
                px(outer, ey + 1, glow_rgb, g=True)
                px(inner, ey + 1, glow_rgb, g=True)
        else:
            px(outer, ey + 1, lit(eye, 0.78))
            px(inner, ey + 1, (252, 250, 255))                          # the highlight
            px(outer, ey + 2, lit(eye, 1.18))
            px(inner, ey + 2, lit(eye, 1.45))
            if glow_rgb:
                px(outer, ey + 2, glow_rgb, g=True)
                px(inner, ey + 2, glow_rgb, g=True)
    # brows: one pixel pair over each eye, tilted by the mood
    by = 3
    for side in (0, 1):
        xs = (1, 2) if side == 0 else (6, 5)
        o, i = xs
        yo, yi = by, by
        if brow in ("angry", "heavy") or expr in ("angry", "mad", "sinister"):
            yi = by + 1
            if by + 1 >= ey:
                yi = by
        elif brow == "sad" or expr in ("sad", "tired"):
            yo = by + 0
            yi = by - 0
            o, i = (o, i)
        elif brow in ("arched", "raised"):
            yo = by - 1 if by >= 1 else by
        if brow != "none":
            px(o, yo, brow_rgb)
            px(i, yi, brow_rgb)
    # blush: soft pink on the cheeks (a pixel under each outer eye), a touch more for cheerful faces
    pink = (236, 128, 140)
    if expr not in ("angry", "mad", "cold"):
        for x in (0, 7):
            px(x, 6, mixc(lit(t[2], 1.0), pink, 0.38))
    # mouth: tiny, one or two pixels, shaped by the mood
    mouth = f.get("mouth") or "line"
    my = 7
    ml = mixc(t[1], lip, 0.8)
    if mouth in ("grin",):
        px(3, my, ml)
        px(4, my, ml)
        px(2, my, lit(ml, 0.9))
        px(5, my, lit(ml, 0.9))
    elif mouth in ("smile", "smirk"):
        px(3, my, ml)
        px(4, my, ml if mouth == "smile" else lit(t[1], 0.95))
        px(5 if mouth == "smirk" else 2, my - 1 if mouth == "smirk" else my - 1, lit(t[1], 0.9))
    elif mouth in ("frown", "line", "none"):
        if mouth != "none":
            px(3, my, lit(t[1], 0.85))
            px(4, my, lit(t[1], 0.85))
    elif mouth == "open":
        px(3, my, (120, 40, 55))
        px(4, my, (120, 40, 55))
    else:
        px(3, my, ml)
        px(4, my, ml)
    if "lipstick" in [m if isinstance(m, str) else m.get("kind") for m in f.get("marks") or []]:
        px(3, my, (200, 40, 80))
        px(4, my, (200, 40, 80))
    sc = (255, 255, 255)
    for m in f.get("marks") or []:
        kind = m if isinstance(m, str) else m.get("kind")
        if kind == "lipstick":
            continue
        col = None if isinstance(m, str) else m.get("color")
        K.mark(cv, fx, fy, kind, t, seed, col, glow, sc)
    return glow



# ================================================================================================ the hair (painted on the hat layer, like the skins of the reference sheet)
STYLE_CLASS = {"short": "short", "crop": "short", "side_part": "side", "slick": "slick", "long": "long", "long_wavy": "long", "bob": "bob", "spiky": "spiky", "spiky_back": "spiky",
               "messy": "spiky", "shaggy": "spiky", "afro": "volume", "wild_mane": "mane", "topknot": "topknot", "bun": "topknot", "ponytail": "long", "high_ponytail": "long",
               "twin_tails": "long", "hood": "hood", "mohawk": "spiky", "undercut": "slick", "pompadour": "volume", "bangs_long": "long", "bald": "bald", "braid": "long", "drills": "long"}


def hair_tones(rgb):
    return [lit(rgb, k) for k in (0.55, 0.78, 1.0, 1.2, 1.42)]


def hair_cover(cls, face, lx, ly, lz, seed):
    """Is this texel of the hat layer hair? (the shape of each haircut on the six faces of the head)"""
    j = K.hsh(lx, seed, 3)
    if face == "top":
        return True
    if face == "bottom":
        return False
    if cls == "bald":
        return False
    if face == "front":
        side = lx in (0, 7)
        if cls == "long":
            if ly <= 1:
                return True
            if side:
                return ly <= 7
            if lx in (1, 6):
                return ly <= 4
            return ly == 2 or (ly == 3 and lx in (2, 5))      # a centre parting, long side locks, fringe tips
        if cls == "bob":
            if ly <= 1:
                return True
            return (side and ly <= 4) or (ly == 2 and lx in (1, 2, 5, 6))
        if cls == "mane":
            if ly <= 1:
                return True
            return (side and ly <= 7) or (ly == 2 and j > 0.45) or (lx in (1, 6) and ly <= 4)
        if cls == "volume":
            return ly <= 1 or (ly == 2 and (side or j > 0.5)) or (side and ly <= 4)
        if cls == "spiky":
            return ly <= 1 or (ly == 2 and lx in (0, 2, 4, 6, 7)) or (side and ly <= 3)
        if cls == "slick":
            return ly == 0 or (ly == 1 and (side or lx in (1, 6)))
        if cls == "side":
            return ly <= 1 or (ly == 2 and lx <= 3)
        if cls == "topknot":
            return ly == 0 or (ly == 1 and (side or j > 0.4))
        return ly <= 1 or (ly == 2 and (lx in (0, 1, 3, 4, 6, 7))) or (ly == 3 and lx in (0, 7)) or (ly == 3 and lx == 3 and j > 0.5)      # short, with a ragged fringe
    if face == "back":
        if cls == "long":
            return True
        if cls == "mane":
            return True
        if cls == "bob":
            return ly <= 5
        if cls == "volume":
            return ly <= 5
        return ly <= 5
    # west / east
    behind = lz >= 3                    # behind the ear
    if cls in ("long", "mane"):
        return ly <= 2 or behind or (lz <= 1 and ly <= 6)
    if cls == "bob":
        return ly <= 2 or (behind and ly <= 5)
    if cls == "volume":
        return ly <= 3 or (behind and ly <= 5)
    if cls == "spiky":
        return ly <= 2 or (behind and ly <= 4)
    if cls == "slick":
        return ly <= 1 or (behind and ly <= 3)
    return ly <= 2 or (behind and ly <= 4)


def paint_hair(cv, spec, hair_rgb, seed):
    """Paints the hair on the hat layer (and its roots on the base head, so nothing bare shows through), shaded like anime hair: a lit band across the crown, darker roots and tips, strands."""
    cls = STYLE_CLASS.get(spec["hair"]["style"], "short")
    tones = hair_tones(hair_rgb)
    if cls == "hood":
        return paint_hood(cv, spec, seed)

    def fn(face, lx, ly, lz, w, h, d):
        if not hair_cover(cls, face, lx, ly, lz, seed):
            return None
        i = 2
        if face == "top":
            # a swirl from the crown: lighter toward the front, a parting line
            i = 3 if (lz <= 2 or (lx + lz) % 4 == 0) else 2
            if lx in (3, 4) and lz in (2, 3):
                i = 1
        elif face == "front":
            i = 3 if ly == 1 else (1 if ly == 0 else 2)
            if ly >= 2:
                i = 1 if (lx + ly) % 2 == 0 else 2
        else:
            band = 2 if face in ("west", "east") else 2
            i = 3 if ly == band and (lx + lz) % 3 else 2
            if ly == 0:
                i = 1
            if ly >= 5:
                i = 1 if (lx + ly) % 2 else 0
            if (lx * 2 + lz + ly) % 5 == 0:
                i = max(0, i - 1)
        return tones[max(0, min(4, i))]

    cv.paint("head", fn, overlay=True, faces=("front", "back", "west", "east", "top"))
    # roots on the base head (under the hat layer), so the hairline never shows bare skin
    base = tones[1]

    def root(face, lx, ly, lz, w, h, d):
        if face == "bottom":
            return None
        if face == "front":
            return base if ly <= 1 else None
        if face == "top" or (face in ("west", "east", "back") and ly <= 3):
            return base
        return None
    cv.paint("head", root)
    # long hair also hangs down the back, on the jacket layer
    if cls in ("long", "mane"):
        n = 9 if cls == "long" else 12

        def back(face, lx, ly, lz, w, h, d):
            if face != "back" or ly >= n:
                return None
            if cls == "long" and ly > 6 and lx in (0, 7):
                return None
            i = 2 if ly < 3 else (1 if (lx + ly) % 2 else 2)
            if ly >= n - 2:
                i = 0 if (lx + ly) % 2 else 1
            return tones[i]
        cv.paint("body", back, overlay=True)


def paint_hood(cv, spec, seed):
    col = C.rgb(spec["hair"]["color"])
    tones = hair_tones(col)

    def fn(face, lx, ly, lz, w, h, d):
        if face == "bottom":
            return None
        if face == "front":
            if 1 <= lx <= 6 and ly >= 2:
                return None
            if lx in (1, 6) and ly == 1:
                return tones[1]
            return tones[3] if ly == 0 else tones[2]
        return tones[3] if ly == 0 else (tones[2] if ly < 6 else tones[1])
    cv.paint("head", fn, overlay=True)
    cv.paint("head", lambda face, lx, ly, lz, w, h, d: None if face == "bottom" else tones[1])


# ================================================================================================ outfit details
def details(cv, spec, tones, seed):
    """What makes an outfit read as designed, not generic, at 8 pixels: a cuff band on the sleeves, a stripe down the outer side of trousers, a zig-zag ruffle on skirts, dresses and robes,
    knee socks under skirts, a bright collar edge. All in the sheet's own colours (top, trim, legs), on the overlay layers."""
    o = spec["outfit"]
    top = o["top"]
    kind = top["kind"]
    trim = C.rgb(top.get("trim")) if top.get("trim") else C.rgb(spec["glow"]["color"] or "#ffffff") if spec["glow"].get("color") else (240, 240, 240)
    tcol = C.rgb(top["color"])
    legs = o["legs"]
    lcol = C.rgb(legs.get("color") or "#444444")
    lk = legs.get("kind")
    sleeves = o.get("sleeves") or {}
    # cuff band: the last two rows of a long sleeve, in the trim colour (not on bare arms)
    if sleeves.get("kind") in ("long", None) and kind not in ("bare", "tank", "vest"):
        for part in ("arm_r", "arm_l"):
            def cuff(face, lx, ly, lz, w, h, d, trim=trim):
                if ly in (10, 11) and face != "top":
                    return lit(trim, 1.0 if ly == 10 else 0.85)
                return None
            cv.paint(part, cuff, overlay=True, faces=("front", "back", "west", "east", "bottom"))
        # a thin shoulder band
        for part in ("arm_r", "arm_l"):
            def band(face, lx, ly, lz, w, h, d, tcol=tcol):
                if ly == 3 and face in ("front", "back", "west", "east"):
                    return lit(tcol, 0.8)
                return None
            cv.paint(part, band, overlay=True, faces=("front", "back", "west", "east"))
    # trouser stripe on the outer side of each leg
    if lk in ("trousers", "leggings", "hakama", None) and kind not in ("dress", "kimono"):
        def stripe_r(face, lx, ly, lz, w, h, d):
            return lit(trim, 0.95) if face == "west" and lz in (1, 2) and ly <= 8 else None

        def stripe_l(face, lx, ly, lz, w, h, d):
            return lit(trim, 0.95) if face == "east" and lz in (1, 2) and ly <= 8 else None
        cv.paint("leg_r", stripe_r, overlay=True, faces=("west",))
        cv.paint("leg_l", stripe_l, overlay=True, faces=("east",))
    # ruffle and socks under skirts, dresses, robes and kimonos
    if kind in ("dress", "robe", "kimono") or lk == "skirt":
        w, h, d = cv.size("body")
        u, v = cv.uv("body", True)
        x0, y0, rw, rh = K.rects(u, v, w, h, d)["front"]
        for face in ("front", "back"):
            fx0, fy0, frw, frh = K.rects(u, v, w, h, d)[face]
            for x in range(frw):
                if x % 2 == 0:
                    cv.put(fx0 + x, fy0 + frh - 1, lit(trim, 1.05) + (255,))
        if lk == "skirt" or kind == "dress":
            def sock(face, lx, ly, lz, w, h, d, trim=trim):
                if face == "bottom":
                    return None
                if ly >= 8:
                    return lit((246, 244, 250), 1.0 - 0.04 * (ly - 8)) if ly > 8 else lit(trim, 0.9)
                return None
            for part in ("leg_r", "leg_l"):
                cv.paint(part, sock, overlay=False if False else True)
    # a bright collar edge
    if kind in ("coat", "robe", "uniform", "kimono", "dress", "vest"):
        w, h, d = cv.size("body")
        u, v = cv.uv("body", True)
        x0, y0, rw, rh = K.rects(u, v, w, h, d)["front"]
        for x in range(1, rw - 1):
            p = cv.px[y0, x0 + x]
            if p[3]:
                cv.put(x0 + x, y0, lit(trim, 1.1) + (255,))


# ================================================================================================ the light
def light_pass(cv, skip_front_of_head=True):
    """Soft gradient light over every texel of the body, the arms and the legs (both layers), and the sides and top of the head: bright at the top, darker at the bottom, darker on the
    sides and the back, a rim of shade at the outer columns; hue-shifted (see lit)."""
    for part in K.PARTS:
        for overlay in (False, True):
            w, h, d = cv.size(part)
            u, v = cv.uv(part, overlay)
            for face, (x0, y0, rw, rh) in K.rects(u, v, w, h, d).items():
                for y in range(rh):
                    for x in range(rw):
                        X, Y = x0 + x, y0 + y
                        p = cv.px[Y, X]
                        if p[3] == 0:
                            continue
                        if part == "head" and face == "front" and not overlay:
                            continue
                        lx, ly, lz = K.cell(face, x, y, w, h, d)
                        f = ly / max(1.0, h - 1.0)
                        k = 1.07 - 0.20 * f
                        if face in ("west", "east"):
                            k -= 0.09
                        elif face == "back":
                            k -= 0.05
                        elif face == "bottom":
                            k -= 0.28
                        elif face == "top":
                            k += 0.06
                        if face in ("front", "back") and (lx == 0 or lx == w - 1):
                            k -= 0.05
                        c = lit((int(p[0]), int(p[1]), int(p[2])), k)
                        cv.px[Y, X] = (c[0], c[1], c[2], p[3])


def is_skin_like(c, tones):
    return any(sum(abs(int(c[i]) - tones[j][i]) for i in range(3)) < 30 for j in range(4))


def hem_and_folds(cv, tones, spec):
    """A one-pixel darker hem line under skirts, robes and coats and a soft fold line down the front of long garments, on the base layer of the legs and the jacket layer of the body."""
    top = spec["outfit"]["top"]
    if top["kind"] not in ("robe", "coat", "dress", "kimono", "uniform"):
        return
    w, h, d = cv.size("body")
    u, v = cv.uv("body", True)
    r = K.rects(u, v, w, h, d)
    x0, y0, rw, rh = r["front"]
    for x in range(rw):
        p = cv.px[y0 + rh - 1, x0 + x]
        if p[3]:
            c = lit((int(p[0]), int(p[1]), int(p[2])), 0.82)
            cv.px[y0 + rh - 1, x0 + x] = (c[0], c[1], c[2], p[3])
    # a fold: two darker texels low on the front, off centre
    for y in range(rh - 4, rh - 1):
        for x in (1, 6):
            p = cv.px[y0 + y, x0 + x]
            if p[3]:
                c = lit((int(p[0]), int(p[1]), int(p[2])), 0.9)
                cv.px[y0 + y, x0 + x] = (c[0], c[1], c[2], p[3])


# ================================================================================================ the painter
def paint_character(spec, name, hair_masks, post=None):
    saved = K.fleck
    K.fleck = lambda c, x, y, seed, amt=0.05: c            # clean fabric: no noise
    try:
        seed = GS.seed_of(name)
        slim = GS.is_slim(spec)
        cv = K.Canvas(slim=slim)
        tones = C.skin_palette(spec["skin"])
        K.paint_skin_tone(cv, tones, seed)
        o = spec["outfit"]
        muscle = spec["build"] in ("broad", "huge") or o["top"]["kind"] in ("bare", "tank", "vest")
        if o["top"]["kind"] in ("bare", "tank", "vest"):
            K.paint_pecs(cv, tones, muscle)
        hair_style = CH.HAIR_MAP.get(spec["hair"]["style"], "short")
        hair_rgb = GS.to_rgb(spec["hair"]["color"])
        hair_done = False
        K.paint_legs(cv, o["legs"], o["boots"], seed, tones, o["top"]["kind"])
        K.paint_torso(cv, o["top"], o["inner"], o["sleeves"], seed, tones, o["legs"].get("color"))
        K.paint_gloves(cv, o["gloves"], tones, seed)
        K.paint_belt(cv, o["belt"], seed, o["top"]["kind"])
        armor_glow = K.paint_armor(cv, spec.get("armor"), tones, seed)
        paint_hair(cv, spec, hair_rgb, seed)
        K.paint_accessories(cv, spec.get("accessories") or [], tones, hair_rgb, seed)
        details(cv, spec, tones, seed)
        hem_and_folds(cv, tones, spec)
        light_pass(cv)
        face_glow = paint_face(cv, spec, tones, seed, hair_rgb)
        if spec["face"].get("mask"):
            face_glow = face_glow + K.paint_mask(cv, spec["face"]["mask"], tones, seed)
    finally:
        K.fleck = saved
    glow = np.zeros((64, 64, 4), np.uint8)
    any_glow = False
    for (x, y, c) in face_glow:
        glow[y, x] = (c[0], c[1], c[2], 255)
        any_glow = True
    ou, ov = cv.uv("head", True)
    ofx, ofy, _, _ = K.rects(ou, ov, 8, 8, 8)["front"]
    bu, bv = cv.uv("body", True)
    bfx, bfy, _, _ = K.rects(bu, bv, 8, 12, 4)["front"]
    for (where, lx, ly, c) in armor_glow:
        if where == "head_front":
            glow[ofy + ly, ofx + lx] = (c[0], c[1], c[2], 255)
        else:
            glow[bfy + ly, bfx + lx] = (c[0], c[1], c[2], 255)
        any_glow = True
    g = spec["glow"]
    if g.get("trim") and g.get("color"):
        gc = C.rgb(g["color"])
        em = o["top"].get("emblem")
        if em and em.get("glow"):
            for y in range(12):
                for x in range(8):
                    p = cv.px[bfy + y, bfx + x]
                    if p[3] and K.emblem_pixel(em, x, y, C.shades(o["top"]["color"]), None) is not None and em.get("where") != "back":
                        glow[bfy + y, bfx + x] = (gc[0], gc[1], gc[2], 255)
                        any_glow = True
    if post is not None and post(cv, spec, seed, glow):
        any_glow = True
    return cv.image(), (Image.fromarray(glow, "RGBA") if any_glow else None), "bald", hair_rgb, slim

"""3.33.1: the anime skin style of the Tier List bosses (the owner's reference sheet of anime Minecraft skins): large shaded eyes with a lash line, iris gradient and a highlight, thin brows, a
tiny mouth, blush, soft light-to-shade gradients on every garment with cool shadows and warm lights, clean fabric (no noise), clearer folds and hems. It reuses the structure painters of the
player-model kit (tools/playermodel/skin_lib.py: garments, belts, gloves, armour, accessories, marks) and repaints the face and the lighting on top, so every outfit kind of the art kit still works.

    paint_character(spec, name, hair_masks) -> (skin image, glow image or None, hair style id, hair rgb, slim)      (the same result as gen_skins.paint_character)

3.37 "anime skin style v2" (volume mode, `paint_character(..., volume=True)`, the default of gen_tier_looks.py; `--flat` keeps the 3.33.1 look): the hair is the kit's volumetric hair (the anime_*
styles of tools/playermodel/gen_hair.py) and only the scalp is painted on the skin; shoulder plates, forearm guards, a standing collar, a tabard down the front, cuffed boots and belt buckles are
painted on the overlay layers (jacket, sleeves, trousers) with bevelled edges, like the owner's reference skins. Everything comes from the sheet (top, armor, boots, belt, accessories) and the
optional `_v2` block of a sheet (see docs/bosses/anime_style_v2.md); a sheet without it still works.
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


# ================================================================================================ volume mode (3.37, anime skin style v2)
ANIME_HAIR = {"short": "anime_short", "crop": "anime_short", "spiky": "anime_spiky", "spiky_back": "anime_spiky", "messy": "anime_spiky", "side_part": "anime_swept", "slick": "anime_swept",
              "long": "anime_long", "long_wavy": "anime_long", "afro": "anime_afro", "topknot": "anime_topknot", "bun": "anime_topknot", "wild_mane": "anime_mane", "hood": "anime_hood"}
OPEN_BROW = ("anime_short", "anime_long")                       # these have an _open twin without the centre fringe, for crowns, tiaras and headbands
HEADGEAR = ("crown", "tiara", "circlet", "headband", "ribbon", "bandana", "mask_band")


def volume_hair_style(spec):
    """The kit hair style of a sheet in volume mode: an anime_* cut where there is one, else the classic kit cut."""
    st = spec["hair"]["style"]
    anime = ANIME_HAIR.get(st)
    if anime is None:
        return CH.HAIR_MAP.get(st, "short")
    if anime in OPEN_BROW and any(a.get("kind") in HEADGEAR for a in spec.get("accessories") or []):
        anime += "_open"
    return anime


def paint_scalp(cv, spec, hair_rgb, seed):
    """Volume mode: only the scalp under the 3D hair is painted on the base head layer (hairline rows on the front, the crown, the sides and the back); the hat layer stays free."""
    cls = STYLE_CLASS.get(spec["hair"]["style"], "short")
    if cls == "hood":
        tones = hair_tones(C.rgb(spec["hair"]["color"]))

        def hood(face, lx, ly, lz, w, h, d):
            if face == "bottom" or (face == "front" and 1 <= lx <= 6 and ly >= 2):
                return None
            return tones[3] if ly == 0 else (tones[2] if ly < 6 else tones[1])
        cv.paint("head", hood)
        return
    tones = hair_tones(hair_rgb)

    def root(face, lx, ly, lz, w, h, d):
        if face == "bottom":
            return None
        if face == "front":
            if ly == 0:
                return tones[3]
            if ly == 1:
                return tones[2]
            if ly == 2 and lx in (0, 7):
                return tones[1]
            return None
        if face == "top":
            return tones[3] if lz <= 3 else tones[2]
        if face in ("west", "east"):
            return (tones[2] if ly <= 1 else tones[1]) if (ly <= 3 or (lz >= 3 and ly <= 5)) else None
        return tones[1] if ly <= 6 else None
    cv.paint("head", root)


def paint_headgear(cv, accs, seed):
    """Volume mode: circlets, crowns, tiaras, headbands, ribbons and bandanas sit on row 2 of the hat layer (the 3D hair covers rows 0 and 1), with a bright upper edge and a jewel in the
    middle of the front; the other accessories (glasses, eyepatch, earrings ...) are painted by the kit."""
    rest = []
    glow = []
    w, h, d = cv.size("head")
    for a in accs:
        kind = a.get("kind")
        if kind not in HEADGEAR:
            rest.append(a)
            continue
        col = C.rgb(a["color"]) if a.get("color") else (224, 180, 72)
        col2 = C.rgb(a["color2"]) if a.get("color2") else None
        gem = C.rgb(a["glow"]) if a.get("glow") else (col2 or lit(col, 1.35))

        def band(face, lx, ly, lz, w_, h_, d_, col=col, gem=gem, kind=kind):
            if face in ("top", "bottom") or ly != 2:
                return None
            if face == "front" and lx in (3, 4) and kind in ("crown", "tiara", "circlet"):
                return gem
            if face in ("west", "east") and lz > 3:
                return lit(col, 0.75)
            return lit(col, 1.18 if (lx + lz) % 2 == 0 else 1.0)
        cv.paint("head", band, overlay=True)
        if kind in ("crown", "tiara"):
            fx, fy, _, _ = cv.front_rect("head", True)
            for x in (1, 3, 4, 6):                                       # the points of the crown stand up on row 1 where the notch of the fringe leaves it free
                if x in (3, 4):
                    cv.put(fx + x, fy + 1, lit(col, 1.25) + (255,))
    K.paint_accessories(cv, rest, None, (0, 0, 0), seed)
    return glow


def plate_pal(c):
    return [lit(c, 0.6), lit(c, 0.8), c, lit(c, 1.12), lit(c, 1.26)]


def volume_pass(cv, spec, tones, seed):
    """Armour and costume pieces that stand off the body on the overlay layers, bevelled (bright upper edge, dark lower edge, a bright rivet): shoulder plates, forearm guards, a standing collar,
    a tabard down the front and over the legs, cuffed boots, a buckle block. Driven by the sheet's top / armor / boots / belt and the optional `_v2` block."""
    o = spec["outfit"]
    top = o["top"]
    kind = top["kind"]
    v2 = spec.get("_v2") or {}
    armor = spec.get("armor") or {}
    pieces = list(armor.get("pieces") or [])
    trim = C.rgb(top["trim"]) if top.get("trim") else None
    tcol = C.rgb(top["color"])
    gold = C.rgb(v2["plate_trim"]) if v2.get("plate_trim") else (C.rgb(armor["trim"]) if armor.get("trim") else (trim or lit(tcol, 1.25)))
    pcol = C.rgb(v2["plate"]) if v2.get("plate") else (C.rgb(armor["color"]) if armor.get("color") else (trim or lit(tcol, 1.1)))
    P = plate_pal(pcol)
    G = plate_pal(gold)

    # ---- shoulder plates on the sleeve overlays
    if "pauldrons" in pieces or "shoulders" in pieces or v2.get("pauldrons"):
        for part in ("arm_r", "arm_l"):
            outer = "west" if part == "arm_r" else "east"

            def pd(face, lx, ly, lz, w, h, d, outer=outer):
                n = 5 if face == outer else 4
                if face == "bottom" or ly >= n:
                    return None
                if face == "top":
                    return P[4] if (lx + lz) % 3 == 0 else P[3]
                if ly == n - 1:
                    return G[2] if ly >= 3 else P[1]
                if ly == 0:
                    return P[4]
                if face in ("front", "back"):
                    return P[3] if lx == 0 or (lx == 1 and ly == 1) else P[2]
                return P[2] if lz in (1, 2) else P[1]
            cv.paint(part, pd, overlay=True)

    # ---- forearm guards (gauntlets) on the sleeve overlays
    if "bracers" in pieces or "vambraces" in pieces or "gauntlets" in pieces or v2.get("gauntlets"):
        for part in ("arm_r", "arm_l"):
            def gt(face, lx, ly, lz, w, h, d):
                if face in ("top", "bottom") or ly < 5 or ly > 11:
                    return None
                if ly == 5:
                    return G[3]
                if ly == 6:
                    return P[4]
                if ly == 11:
                    return G[1]
                if ly == 10:
                    return P[1]
                if face in ("front", "back"):
                    return P[3] if lx == 1 and ly in (7, 8) else P[2]
                return P[1] if lz in (0, 3) else P[2]
            cv.paint(part, gt, overlay=True)

    # ---- the standing collar on the jacket layer
    collar = top.get("collar") or "none"
    if v2.get("collar_color") or (collar != "none" and kind in ("coat", "robe", "uniform", "kimono", "dress", "vest", "tabard")):
        cc = C.rgb(v2["collar_color"]) if v2.get("collar_color") else (C.rgb(top["collar_color"]) if top.get("collar_color") else (trim or lit(tcol, 1.2)))
        C_ = plate_pal(cc)
        hi = 3 if (collar == "high" or v2.get("collar_high")) else 2
        opened = bool(top.get("open")) or collar == "v"

        def col_fn(face, lx, ly, lz, w, h, d):
            if face == "bottom":
                return None
            if face == "top":
                return C_[3]
            n = hi if face != "front" else hi - 1
            if ly >= n:
                return None
            if face == "front" and opened and lx in (3, 4):
                return None
            if ly == 0:
                return C_[4]
            if ly == n - 1:
                return C_[1]
            return C_[2] if lx % 2 == 0 else C_[3]
        cv.paint("body", col_fn, overlay=True)

    # ---- a tabard (a panel down the front, over the body and the legs)
    tab = v2.get("tabard") or (top.get("color") if kind == "tabard" else None)
    if tab:
        T = plate_pal(C.rgb(tab))
        edge = plate_pal(C.rgb(v2["tabard_trim"])) if v2.get("tabard_trim") else G
        reach = {"hip": 0, "thigh": 4, "knee": 7, "calf": 9, "ankle": 11, "floor": 12}.get(top.get("length") or "knee", 7)
        reach = max(reach, 5)

        def tb(face, lx, ly, lz, w, h, d):
            if face != "front" or not (2 <= lx <= 5) or ly < 2:
                return None
            if lx in (2, 5):
                return edge[2] if ly % 4 != 0 else edge[4]
            if ly == 11:
                return edge[1]
            return T[3] if (lx == 3 and ly in (4, 5)) else (T[2] if lx == 3 else T[1])
        cv.paint("body", tb, overlay=True, faces=("front",))

        def tbl(face, lx, ly, lz, w, h, d, inner=3):
            if face != "front" or ly >= reach:
                return None
            if lx not in (inner, inner - 1):
                return None
            if lx == inner:
                return edge[2] if ly % 4 != 3 else edge[4]
            if ly == reach - 1:
                return edge[1]
            return T[2] if lx == inner - 1 else None
        cv.paint("leg_r", lambda f, a, b, c, w, h, d: tbl(f, a, b, c, w, h, d, 3), overlay=True, faces=("front",))
        cv.paint("leg_l", lambda f, a, b, c, w, h, d: tbl(f, 3 - a, b, c, w, h, d, 3), overlay=True, faces=("front",))

    # ---- cuffed boots: a wide bright cuff under the knee and a lit toe cap
    boots = o["boots"]
    if boots.get("kind") in ("boots", "shoes") and not v2.get("no_boot_cuff"):
        bh = {"ankle": 3, "calf": 6, "knee": 8}.get(boots.get("height", "calf"), 6)
        bc = C.rgb(v2["boot_cuff"]) if v2.get("boot_cuff") else (trim if trim else lit(C.rgb(boots.get("color") or "#2a2220"), 1.6))
        B = plate_pal(bc)
        bcol = plate_pal(C.rgb(boots.get("color") or "#2a2220"))
        y0 = 12 - bh
        if boots.get("kind") == "boots":
            def bt(face, lx, ly, lz, w, h, d):
                if face in ("top", "bottom"):
                    return None
                if ly == y0:
                    return B[4]
                if ly == y0 + 1:
                    return B[2] if face in ("front", "back") else B[1]
                if ly == y0 + 2 and face == "front":
                    return bcol[0]
                if ly == 11 and face == "front":
                    return bcol[3]
                if ly == 10 and face == "front" and lx in (1, 2):
                    return bcol[3]
                return None
            for part in ("leg_r", "leg_l"):
                cv.paint(part, bt, overlay=True)

    # ---- a buckle block (two rows) on the belt
    belt = o.get("belt")
    if belt and kind != "bare":
        bk = C.rgb(belt.get("buckle") or "#c8a040")
        Bk = plate_pal(bk)

        def bu(face, lx, ly, lz, w, h, d):
            if face != "front" or ly not in (9, 10):
                return None
            if lx in (3, 4):
                return Bk[4] if (ly == 9 and lx == 3) else (Bk[2] if ly == 9 else Bk[1])
            if lx in (2, 5) and ly == 9:
                return Bk[0]
            return None
        cv.paint("body", bu, overlay=True, faces=("front",))

    # ---- light clusters: a few bright highlight texels on the lit side of every garment, a darker column at the outer edge of each sleeve
    for part in ("body", "arm_r", "arm_l", "leg_r", "leg_l"):
        w, h, d = cv.size(part)
        u, v = cv.uv(part, True)
        for face, (x0, y0, rw, rh) in K.rects(u, v, w, h, d).items():
            if face not in ("front", "west", "east"):
                continue
            for y in range(rh):
                for x in range(rw):
                    p = cv.px[y0 + y, x0 + x]
                    if p[3] == 0:
                        continue
                    lx, ly, lz = K.cell(face, x, y, w, h, d)
                    if 1 <= ly <= 8 and face == "front" and (int(p[0]) + int(p[1]) + int(p[2])) > 330 and K.hsh(lx, ly + 7 * lz, seed + 91) > 0.965:
                        c = lit((int(p[0]), int(p[1]), int(p[2])), 1.28)
                        cv.px[y0 + y, x0 + x] = (c[0], c[1], c[2], p[3])


# ================================================================================================ the light
def light_pass(cv, skip_front_of_head=True, stepped=False):
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
                        if stepped:                      # (volume mode) cel shading: four light bands per part, a fold cluster on loose garments
                            k = (1.12, 1.03, 0.95, 0.86)[min(3, int(f * 4.0))]
                            if part != "head" and face in ("front", "back") and (lx * 2 + ly) % 9 in (0, 1):
                                k -= 0.06
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
def paint_character(spec, name, hair_masks, post=None, volume=False):
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
        hair_style = volume_hair_style(spec) if volume else "bald"
        hair_rgb = GS.to_rgb(spec["hair"]["color"])
        K.paint_legs(cv, o["legs"], o["boots"], seed, tones, o["top"]["kind"])
        K.paint_torso(cv, o["top"], o["inner"], o["sleeves"], seed, tones, o["legs"].get("color"))
        K.paint_gloves(cv, o["gloves"], tones, seed)
        K.paint_belt(cv, o["belt"], seed, o["top"]["kind"])
        armor_glow = K.paint_armor(cv, spec.get("armor"), tones, seed)
        if volume:
            paint_scalp(cv, spec, hair_rgb, seed)
            paint_headgear(cv, spec.get("accessories") or [], seed)
        else:
            paint_hair(cv, spec, hair_rgb, seed)
            K.paint_accessories(cv, spec.get("accessories") or [], tones, hair_rgb, seed)
        details(cv, spec, tones, seed)
        hem_and_folds(cv, tones, spec)
        light_pass(cv, stepped=volume)
        if volume:
            volume_pass(cv, spec, tones, seed)
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
    return cv.image(), (Image.fromarray(glow, "RGBA") if any_glow else None), hair_style, hair_rgb, slim

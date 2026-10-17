"""Why: the face is what makes a character recognisable at a glance, and one hand-drawn face per character is the slowest part of a model. This module draws the
front of the head (32 x 32 texels, 4 texels per model pixel, like the Lucius face) from a few named choices: eye style, eyebrows, mouth, nose, marks (scars,
stubble, blush, sigils, stitches ...) and an optional mask. The hair fringe is drawn by hair.py onto the same canvas (before and after the features), the
sides of the head (ears, sideburns) by `head_painter`.

Canvas layout (texels, x to the viewer's right = the model's left): hair line y 0..8, brows y 12..15, eyes y 16..20 (the viewer's-left eye is x 7..13, the other
x 18..24), nose y 18..22, mouth y 24..28, chin y 29..31.
"""
from .core import Canvas, px, pal, mix, clamp, hsh, nz, FACE_T

# ------------------------------------------------------------------------------------------------------------------------ eye templates
# 7 columns, 5 rows; the OUTER corner is column 0 (the template is mirrored for the other eye).
#   # lid line   e lash flick   w sclera   d iris top (dark)   i iris   l iris low (light)   P pupil   h highlight   s soft shadow below   g glowing eye
#   v slit pupil   . nothing
EYES = {
    "round": [".#####.",
              "wwdddww",
              "wwiPiww",
              "wwlllww",
              ".sssss."],
    "big": ["e#####.",
            "wdddddw",
            "widhiiw",
            "wiPPiiw",
            "wlllllw"],
    "sharp": ["###....",
              ".######",
              ".wdPdw.",
              ".wiiiw.",
              "..sss.."],
    "narrow": [".......",
               "#######",
               ".widiw.",
               "..sss..",
               "......."],
    "slit": [".......",
             "e######",
             ".dvvvd.",
             ".lvvvl.",
             "..sss.."],
    "tired": [".......",
              "#######",
              "wwdddww",
              ".wiiiw.",
              "sssssss"],
    "fierce": ["##.....",
               ".######",
               ".dPPPd.",
               ".wiiiw.",
               "..sss.."],
    "wide": [".#####.",
             "wdddddw",
             "wiiPiiw",
             "wiiPiiw",
             "wlllllw"],
    "closed": [".......",
               ".......",
               "#######",
               ".sssss.",
               "......."],
    "glow": [".#####.",
             "wwgggww",
             "wwgggww",
             "wwgggww",
             ".sssss."],
    "empty": [".......",
              ".#####.",
              "wwwwwww",
              "wwwwwww",
              ".sssss."],
    "smile": [".......",
              "..###..",
              ".#ddd#.",
              "wwiPiww",
              "..sss.."],
}

BROWS = {
    # a list of (dx, dy) texel offsets for the OUTER half to the INNER end, drawn for the left eye (mirrored); dx 0 = outer end. 6 texels wide.
    "none": [],
    "thin": [(0, 1), (1, 1), (2, 0), (3, 0), (4, 0), (5, 0)],
    "thick": [(0, 1), (1, 1), (2, 0), (3, 0), (4, 0), (5, 0), (0, 2), (1, 2), (2, 1), (3, 1), (4, 1), (5, 1)],
    "angry": [(0, 0), (1, 0), (2, 1), (3, 1), (4, 2), (5, 2), (1, 1), (2, 2), (3, 2), (4, 3)],
    "sad": [(0, 2), (1, 2), (2, 1), (3, 1), (4, 0), (5, 0)],
    "raised": [(0, 2), (1, 1), (2, 0), (3, 0), (4, 0), (5, 1)],
    "flat": [(0, 1), (1, 1), (2, 1), (3, 1), (4, 1), (5, 1)],
    "heavy": [(0, 0), (1, 0), (2, 0), (3, 1), (4, 1), (5, 2), (0, 1), (1, 1), (2, 1), (3, 2), (4, 2), (5, 3)],
    "arched": [(0, 2), (1, 1), (2, 0), (3, 0), (4, 1), (5, 2)],
}

EXPRESSIONS = {
    "calm": dict(eyes="round", brows="thin", mouth="neutral"),
    "stern": dict(eyes="sharp", brows="thick", mouth="line"),
    "angry": dict(eyes="fierce", brows="angry", mouth="frown"),
    "smirk": dict(eyes="sharp", brows="thin", mouth="smirk"),
    "cheerful": dict(eyes="big", brows="raised", mouth="smile"),
    "grin": dict(eyes="round", brows="thick", mouth="grin"),
    "sad": dict(eyes="round", brows="sad", mouth="frown"),
    "tired": dict(eyes="tired", brows="flat", mouth="line"),
    "sinister": dict(eyes="narrow", brows="angry", mouth="grin"),
    "mad": dict(eyes="wide", brows="angry", mouth="fangs"),
    "cold": dict(eyes="narrow", brows="flat", mouth="line"),
    "shy": dict(eyes="big", brows="sad", mouth="smile"),
    "dead": dict(eyes="empty", brows="none", mouth="line"),
    "demon": dict(eyes="slit", brows="angry", mouth="fangs"),
}
EYE_STYLES = tuple(EYES)
BROW_STYLES = tuple(BROWS)
MOUTH_STYLES = ("neutral", "smile", "smirk", "frown", "grin", "open", "fangs", "line", "pout", "none")
NOSE_STYLES = ("small", "none", "sharp", "wide")
MARKS = ("scar_diag", "scar_diag_r", "scar_eye_l", "scar_eye_r", "scar_cheek_l", "scar_cheek_r", "scar_brow_l", "scar_brow_r", "stubble", "moustache", "goatee",
         "beard", "blush", "freckles", "tear_l", "tear_r", "fang_marks", "forehead_sigil", "third_eye", "cheek_lines", "eye_bags", "stitches", "cracks", "veins",
         "war_paint", "lipstick", "eyeshadow", "forehead_gem", "light_lines", "mole")


def _lid_color(hair_cols):
    c = hair_cols[0]
    return (max(6, c[0] // 2), max(6, c[1] // 2), max(8, c[2] // 2))


class FacePalette:
    """The colours the face art uses, resolved once from the spec (skin = 4 tones dark -> light, hair = 4+ tones)."""

    def __init__(self, skin, hair, eye, eye_glow=None, sclera=(238, 234, 238), brow=None, lip=None, lid=None):
        self.skin = skin
        self.hair = hair
        self.eye = eye
        self.eye_glow = eye_glow
        self.sclera = sclera
        self.brow = brow or mix(hair[0], (0, 0, 0), 0.25)
        self.lip = lip or mix(skin[1], (170, 60, 70), 0.45)
        self.lip_dark = mix(self.lip, (30, 10, 20), 0.55)
        self.lid = lid or _lid_color(hair)
        self.eye_dark = mix(eye, (0, 0, 10), 0.45)
        self.eye_light = mix(eye, (255, 255, 255), 0.45)


def _skin_base(cv, sk):
    """Flat tone bands, light from the upper left: the cheek on the far side and the jaw fall into shade with a dithered edge."""
    for y in range(32):
        for x in range(32):
            t = 0.70
            if y >= 27:
                t = 0.5
            if y >= 30:
                t = 0.34
            if x <= 1:
                t -= 0.14
            if x >= 24 and y > 14:
                t -= 0.24 * min(1.0, (x - 23) / 7.0)
            cv.put(x, y, pal(sk, t, x, y, 0.0))


def _draw_eye(cv, x0, y0, outer_left, style, P, lashes):
    tpl = EYES[style]
    for r, row in enumerate(tpl):
        for c, ch in enumerate(row):
            if ch == ".":
                continue
            x = x0 + (c if outer_left else 6 - c)
            y = y0 + r
            col, glow, ga = None, None, 255
            if ch == "#":
                col = P.lid
            elif ch == "e":
                col = P.lid
            elif ch == "w":
                col = P.sclera if r < 4 else mix(P.sclera, P.skin[1], 0.4)
            elif ch == "d":
                col = P.eye_dark
            elif ch == "i":
                col = P.eye
            elif ch == "l":
                col = P.eye_light
            elif ch == "P":
                col = (14, 10, 18)
            elif ch == "v":
                col = (14, 10, 18)
            elif ch == "h":
                col = (255, 255, 255)
            elif ch == "s":
                col = mix(P.skin[1], P.skin[0], 0.5)
            elif ch == "g":
                col = P.eye
            if col is None:
                continue
            if P.eye_glow is not None and ch in ("d", "i", "l", "g", "P", "v"):
                glow = P.eye_glow
                ga = 235 if ch != "P" else 170
            cv.put(x, y, col, 255, glow, ga)
    if lashes:                                                   # a flick at the outer corner of the lid
        fx = x0 - 1 if outer_left else x0 + 7
        cv.put(fx, y0 - 1, P.lid)
        cv.put(fx + (1 if outer_left else -1), y0 - 1 if style in ("sharp", "fierce") else y0, P.lid)


def _draw_brow(cv, x0, y0, outer_left, style, P):
    for dx, dy in BROWS[style]:
        x = x0 + (dx if outer_left else 5 - dx)
        cv.put(x, y0 + dy, P.brow)


def _nose(cv, sk, style):
    if style == "none":
        return
    if style == "small":
        for y in range(18, 22):
            cv.put(17, y, sk[1])
        for x in (14, 15, 16, 17):
            cv.put(x, 22, sk[1])
        cv.put(15, 22, sk[0])
        cv.put(16, 22, sk[0])
        cv.put(15, 20, sk[3])
        cv.put(15, 19, sk[3])
    elif style == "sharp":
        for y in range(16, 23):
            cv.put(17 if y > 17 else 16, y, sk[1])
        for x in (15, 16, 17, 18):
            cv.put(x, 22, sk[0])
        cv.put(15, 20, sk[3])
        cv.put(15, 19, sk[3])
        cv.put(15, 18, sk[3])
    elif style == "wide":
        for y in range(18, 22):
            cv.put(17, y, sk[1])
        for x in (13, 14, 15, 16, 17, 18):
            cv.put(x, 22, sk[0] if x in (13, 18) else sk[1])
        cv.put(14, 23, sk[0])
        cv.put(17, 23, sk[0])
        cv.put(15, 19, sk[3])
        cv.put(15, 20, sk[3])


def _mouth(cv, P, style, fang=False):
    sk = P.skin
    lip, dk = P.lip, P.lip_dark
    teeth = (248, 242, 240)
    if style == "none":
        return
    if style == "neutral":
        for x in range(13, 19):
            cv.put(x, 26, dk)
        cv.put(12, 25, sk[1])
        cv.put(19, 25, sk[1])
        for x in range(14, 18):
            cv.put(x, 27, mix(lip, sk[1], 0.5))
    elif style == "line":
        for x in range(12, 20):
            cv.put(x, 26, dk)
        for x in range(14, 18):
            cv.put(x, 27, sk[1])
    elif style == "smile":
        for x, y in ((11, 25), (12, 26), (13, 27), (14, 27), (15, 27), (16, 27), (17, 27), (18, 27), (19, 26), (20, 25)):
            cv.put(x, y, dk)
        for x in range(14, 18):
            cv.put(x, 28, sk[1])
        cv.put(10, 24, sk[1])
        cv.put(21, 24, sk[1])
    elif style == "smirk":
        for x, y in ((12, 27), (13, 27), (14, 27), (15, 27), (16, 26), (17, 26), (18, 25), (19, 25), (20, 24)):
            cv.put(x, y, dk)
        cv.put(21, 23, sk[1])
        for x in range(14, 18):
            cv.put(x, 28, sk[1])
    elif style == "frown":
        for x, y in ((11, 28), (12, 27), (13, 26), (14, 26), (15, 26), (16, 26), (17, 26), (18, 26), (19, 27), (20, 28)):
            cv.put(x, y, dk)
        for x in range(14, 18):
            cv.put(x, 29, sk[1])
    elif style == "grin":
        for x, y in ((10, 24), (11, 25), (12, 26), (13, 27), (14, 27), (15, 27), (16, 27), (17, 27), (18, 27), (19, 26), (20, 25), (21, 24)):
            cv.put(x, y, dk)
        for x in range(12, 20):
            cv.put(x, 26, teeth)
            if 13 <= x <= 18:
                cv.put(x, 27, teeth if x % 2 else (226, 218, 224))
        for x in (13, 15, 17):
            cv.put(x, 26, (214, 204, 212))
        for x in range(13, 19):
            cv.put(x, 28, mix(lip, dk, 0.25))
    elif style == "open":
        for y in range(25, 29):
            for x in range(13, 19):
                cv.put(x, y, (60, 18, 28))
        for x in range(13, 19):
            cv.put(x, 25, teeth)
        for x in range(14, 18):
            cv.put(x, 28, (190, 70, 84))
        for x, y in ((12, 24), (19, 24), (12, 29), (19, 29)):
            cv.put(x, y, dk)
    elif style == "fangs":
        for x in range(12, 20):
            cv.put(x, 26, dk)
        for x in (13, 18):
            cv.put(x, 27, teeth)
            cv.put(x, 28, teeth)
        cv.put(12, 25, sk[1])
        cv.put(19, 25, sk[1])
    elif style == "pout":
        for x in range(13, 19):
            cv.put(x, 26, lip)
            cv.put(x, 27, lip)
        for x in range(14, 18):
            cv.put(x, 25, mix(lip, sk[1], 0.4))
        for x in range(13, 19):
            cv.put(x, 28, mix(lip, dk, 0.4))


def _scar_line(cv, pts, col, hi=None, glow=None):
    for a, b in zip(pts, pts[1:]):
        cv.line(a[0], a[1], b[0], b[1], col, glow=glow, ga=200)
    if hi is not None:
        for a, b in zip(pts, pts[1:]):
            cv.line(a[0] + 1, a[1], b[0] + 1, b[1], hi)


def _mark(cv, mk, P, flags):
    kind = mk if isinstance(mk, str) else mk.get("kind")
    opt = {} if isinstance(mk, str) else mk
    sk = P.skin
    scar = mix(sk[1], (150, 60, 70), 0.55)
    scar_hi = mix(sk[3], (255, 200, 200), 0.3)
    col = None
    if opt.get("color") is not None:
        from .color import rgb
        col = rgb(opt["color"])
    glow = None
    if opt.get("glow") is not None:
        from .color import rgb
        glow = rgb(opt["glow"])
    if kind in ("scar_diag", "scar_diag_r"):
        # from the character's right forehead (viewer's left) to the left cheek, over the nose bridge; _r is the mirrored one
        pts = [(9, 3), (11, 8), (14, 13), (17, 19), (20, 24), (22, 28)]
        if kind == "scar_diag_r":
            pts = [(31 - x, y) for x, y in pts]
        _scar_line(cv, pts, col or scar, scar_hi, glow)
        for (x, y) in pts[1:-1]:                                  # the stitch marks across it
            cv.put(x - 1, y, col or scar)
            cv.put(x + 2, y, col or scar)
    elif kind in ("scar_eye_l", "scar_eye_r"):
        cx = 10 if kind == "scar_eye_r" else 21                  # _r = over the character's right eye (the viewer's left)
        _scar_line(cv, [(cx + 1, 12), (cx, 17), (cx - 1, 22)], col or scar, scar_hi, glow)
        for y in (15, 19):
            cv.put(cx - 1, y, col or scar)
            cv.put(cx + 2, y, col or scar)
    elif kind in ("scar_cheek_l", "scar_cheek_r"):
        cx = 22 if kind == "scar_cheek_l" else 9
        _scar_line(cv, [(cx - 2, 21), (cx, 23), (cx + 2, 25)], col or scar, scar_hi, glow)
    elif kind in ("scar_brow_l", "scar_brow_r"):
        cx = 21 if kind == "scar_brow_l" else 10
        _scar_line(cv, [(cx, 10), (cx, 14), (cx - 1, 17)], col or scar, scar_hi, glow)
    elif kind == "stubble":
        c2 = col or mix(sk[1], P.hair[0], 0.30)
        for y in range(22, 32):
            for x in range(5, 27):
                if (hsh(x, y, 5) > 0.74) and not (12 <= x <= 19 and 24 <= y <= 28) and (y > 23 or x < 9 or x > 22):
                    cv.put(x, y, c2)
        for x in range(10, 22):
            if hsh(x, 23, 9) > 0.45 and not (11 <= x <= 20 and y < 0):
                cv.put(x, 23, c2)
    elif kind == "moustache":
        c2 = col or mix(P.hair[0], (0, 0, 0), 0.2)
        for x in range(10, 22):
            cv.put(x, 23, c2)
        for x in range(11, 21):
            cv.put(x, 24, c2) if not 13 <= x <= 18 or x in (13, 18) else None
        for x in (9, 10, 21, 22):
            cv.put(x, 24, c2)
            cv.put(x, 25, c2) if x in (9, 22) else None
    elif kind == "goatee":
        c2 = col or mix(P.hair[0], (0, 0, 0), 0.2)
        for y in range(28, 32):
            for x in range(12 + (y - 28) // 2, 20 - (y - 28) // 2):
                cv.put(x, y, c2)
        for x in range(13, 19):
            cv.put(x, 27, c2) if x in (13, 18) else None
    elif kind == "beard":
        c2 = col or P.hair[1]
        c3 = mix(c2, (255, 255, 255), 0.15)
        for y in range(22, 32):
            for x in range(0, 32):
                edge = x < 4 + (y - 22) * 0.3 or x > 27 - (y - 22) * 0.3
                if y >= 27 or edge or (y >= 23 and not (11 <= x <= 20 and y <= 28) and (x < 8 or x > 23)):
                    if not (12 <= x <= 19 and 24 <= y <= 28) and not (y < 26 and 10 < x < 21 and y < 24):
                        cv.put(x, y, c3 if hsh(x, y, 2) > 0.72 else c2)
        for x in range(10, 22):
            cv.put(x, 23, c2) if (x < 13 or x > 18) else None
    elif kind == "blush":
        c2 = col or mix(sk[2], (230, 110, 120), 0.45)
        for (x0, x1) in ((6, 10), (21, 25)):
            for y in (22, 23):
                for x in range(x0, x1 + 1):
                    cv.put(x, y, c2)
    elif kind == "freckles":
        c2 = col or mix(sk[1], (150, 90, 60), 0.6)
        for (x, y) in ((8, 21), (10, 22), (12, 21), (9, 23), (19, 21), (21, 22), (23, 21), (22, 23)):
            cv.put(x, y, c2)
    elif kind in ("tear_l", "tear_r"):
        x = 22 if kind == "tear_l" else 9
        c2 = col or (70, 120, 220)
        for y in range(22, 27):
            cv.put(x, y, c2, 255, glow, 200)
    elif kind == "fang_marks":
        c2 = col or scar
        for x in (6, 25):
            for y in range(22, 26):
                cv.put(x, y, c2)
    elif kind == "forehead_sigil":
        c2 = col or (230, 70, 70)
        g2 = glow or c2
        for (x, y) in ((15, 6), (16, 6), (14, 7), (17, 7), (14, 8), (17, 8), (15, 9), (16, 9)):
            cv.put(x, y, c2, 255, g2, 230)
        cv.put(15, 7, c2, 255, g2, 255)
        cv.put(16, 8, c2, 255, g2, 255)
    elif kind == "forehead_gem":
        c2 = col or (230, 70, 70)
        g2 = glow or c2
        for (x, y) in ((15, 6), (16, 6), (15, 7), (16, 7), (14, 7), (17, 7), (15, 8), (16, 8)):
            cv.put(x, y, mix(c2, (255, 255, 255), 0.3 if y == 6 else 0.0), 255, g2, 255)
    elif kind == "third_eye":
        c2 = col or (230, 190, 70)
        g2 = glow or c2
        for x in range(13, 19):
            cv.put(x, 8, P.lid)
        for x in range(14, 18):
            cv.put(x, 9, c2, 255, g2, 255)
        cv.put(15, 9, (20, 8, 8), 255, g2, 200)
        cv.put(16, 9, (20, 8, 8), 255, g2, 200)
    elif kind == "cheek_lines":
        c2 = col or P.lid
        for y in (22, 24):
            for x in (5, 6, 7, 24, 25, 26):
                cv.put(x, y, c2)
    elif kind == "eye_bags":
        c2 = col or mix(sk[1], (110, 80, 120), 0.45)
        for x in list(range(7, 14)) + list(range(18, 25)):
            cv.put(x, 21, c2)
    elif kind == "stitches":
        c2 = col or (50, 36, 40)
        for y in range(3, 8):
            cv.put(25, y, c2)
        for y in (3, 5, 7):
            for x in (24, 26):
                cv.put(x, y, c2)
        for x in range(12, 21):
            cv.put(x, 29, c2) if x % 2 == 0 else cv.put(x, 28, c2)
    elif kind == "cracks":
        c2 = col or (60, 40, 50)
        g2 = glow or (255, 80, 120)
        for pts in (((5, 12), (7, 15), (6, 19), (8, 23)), ((26, 10), (24, 14), (25, 18), (23, 22))):
            for a, b in zip(pts, pts[1:]):
                cv.line(a[0], a[1], b[0], b[1], c2, glow=g2, ga=180)
    elif kind == "veins":
        c2 = col or mix(sk[1], (80, 60, 120), 0.5)
        for pts in (((4, 11), (6, 14), (5, 18), (7, 21)), ((27, 11), (25, 14), (26, 18), (24, 21))):
            for a, b in zip(pts, pts[1:]):
                cv.line(a[0], a[1], b[0], b[1], c2, glow=glow, ga=160)
    elif kind == "war_paint":
        c2 = col or (200, 40, 40)
        for y in (21, 22):
            for x in list(range(5, 12)) + list(range(20, 27)):
                cv.put(x, y, c2, 255, glow, 160)
    elif kind == "lipstick":
        flags["lip"] = col or (190, 40, 70)
    elif kind == "eyeshadow":
        c2 = col or (120, 70, 150)
        for x in list(range(7, 14)) + list(range(18, 25)):
            cv.put(x, 15, mix(sk[1], c2, 0.6))
    elif kind == "light_lines":
        c2 = col or (255, 255, 235)
        g2 = glow or (255, 240, 170)
        for (x, y) in ((8, 15), (7, 16), (7, 17), (8, 18), (23, 15), (24, 16), (24, 17), (23, 18), (15, 3), (15, 4), (15, 5), (16, 3), (16, 4), (16, 5)):
            cv.put(x, y, c2, 255, g2, 255)
    elif kind == "mole":
        cv.put(22, 25, col or (90, 56, 50))


def face_canvas(face, P, hair_fringe=None, hair_style=None):
    """The 32 x 32 front of the head. face = the normalised face spec (eyes, brows, mouth, nose, marks, lashes ...); P = FacePalette. hair_fringe(cv, stage)
    draws the hair onto the canvas (stage 0 before the features, stage 1 over the brows and eyes)."""
    cv = Canvas(32, 32)
    sk = P.skin
    _skin_base(cv, sk)
    flags = {}
    if hair_fringe:
        hair_fringe(cv, 0)
    marks = list(face.get("marks", ()))
    pre = [m for m in marks if (m if isinstance(m, str) else m.get("kind")) in ("eyeshadow", "blush", "freckles", "eye_bags", "war_paint", "beard", "stubble")]
    post = [m for m in marks if m not in pre]
    for m in pre:
        _mark(cv, m, P, flags)
    _nose(cv, sk, face.get("nose", "small"))
    if flags.get("lip"):
        P.lip = flags["lip"]
        P.lip_dark = mix(P.lip, (30, 10, 20), 0.5)
    for m in marks:
        if (m if isinstance(m, str) else m.get("kind")) == "lipstick":
            _mark(cv, m, P, flags)
    if flags.get("lip"):
        P.lip = flags["lip"]
        P.lip_dark = mix(P.lip, (30, 10, 20), 0.5)
    _mouth(cv, P, face.get("mouth", "neutral"))
    eyes = face.get("eyes", "round")
    eye_y = 16 + int(face.get("eye_shift", 0))
    lashes = bool(face.get("lashes", False))
    _draw_eye(cv, 7, eye_y, True, eyes, P, lashes)
    _draw_eye(cv, 18, eye_y, False, eyes, P, lashes)
    by = 12 + int(face.get("eye_shift", 0)) + int(face.get("brow_shift", 0))
    _draw_brow(cv, 7, by, True, face.get("brows", "thin"), P)
    _draw_brow(cv, 19, by, False, face.get("brows", "thin"), P)
    if hair_fringe:
        hair_fringe(cv, 1)
    for m in post:
        if (m if isinstance(m, str) else m.get("kind")) != "lipstick":
            _mark(cv, m, P, flags)
    return cv


def head_painter(front, P, hair_mask, hair_p, skin_p, ear_p=None, skin_cover=None):
    """The head box painter: the front from the canvas, the other faces from the hair where hair_mask says so and from the skin elsewhere (the ear on the
    sides). hair_mask(face, i, j, fw, fh) -> True where the head shows hair on a 16-texel face; hair_p / skin_p are material painters."""
    sk = P.skin

    def p(face, i, j, fw, fh):
        if face == "front":
            return front.at(i, j)
        if hair_mask(face, i, j, fw, fh):
            return hair_p(face, i, j, fw, fh)
        if face in ("left", "right"):
            f = i if face == "left" else fw - 1 - i                     # distance from the front edge
            if 7 <= f <= 11 and 9 <= j <= 13 and ear_p is not False:
                c = sk[1] if (f in (7, 11) or j in (9, 13)) else sk[2]
                return px(c), None
        return skin_p(face, i, j, fw, fh)
    return p

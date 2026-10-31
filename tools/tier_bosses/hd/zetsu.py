"""Zetsu (Naruto, the Akatsuki spy), HD 256x256 (3.38.0, group a of docs/bosses/hd_outfits.md, details in docs/bosses/hd_outfits_a.md).

Colour blocking is the 64x64 remake's (tools/tier_bosses/remake/zetsu.py): a face split down the middle (pale viewer-left half, black viewer-right half) with big glowing yellow slit-pupil eyes and a
sinister grin, the green Venus-flytrap clamp, the black Akatsuki cloak with white-outlined red clouds and a tall red-lined collar, one white hand and one black hand, black trousers with white shin
wraps, dark sandals. HD adds what 4x texels allow: a hand-drawn cloud with a swirl, cloth that really folds, a toothed leaf rim round the face, spiralling shin wraps, a sandal strap, glowing eyes.
Hair: `anime_mane` (a ring of spiked leaves with a mantle and cheek locks = the flytrap round the head; `anime_hood` was a flat green helmet), leaf green.
Original art from memory of the canon look (no wiki could be fetched, see the doc). Glow: the two eyes.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import numpy as np  # noqa: E402
import hdlib as H  # noqa: E402

LEAF, LEAF_D, LEAF_L = "#46b83a", "#1f6a24", "#9be66a"
BK, BK_D, BK_L = "#17171f", "#0a0a10", "#3c3c50"          # the cloak
WH, WH_D = "#e9e6df", "#a9a6a2"                            # the white half
RED, RED_D, RED_L = "#d42a2c", "#7e0e18", "#ff6a58"
CLOUD_W = "#f7f5ee"
YEL = ("#8a5a00", "#ffd21a", "#fffbb0")                    # iris: dark top, middle, light bottom
SIDES = ("front", "back", "west", "east")
HAIR = (70, 184, 58)


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


# ================================================================================================ the head
def slit_eye(f, cx, cy, w, h, pale):
    """A big yellow eye with a vertical black slit pupil; a pale lid on the white half, a dark sclera on the black half."""
    H.eye(f, cx, cy, w, h, YEL, lash="#050507" if pale else "#d8d070", sclera="#ecebd0" if pale else "#23231c", tilt=0.14, pupil="#000000",
          glow="#a07800", lid_shadow=0.34, lash_w=0.40, flick=0.2, hi=0.5, iris_r=1.05)
    f.ellipse(cx, cy + h * 0.04, 0.16, h * 0.42, "#000000", soft=1.5)
    f.ellipse(cx - 0.35, cy - h * 0.2, 0.10, 0.12, "#ffffff", a=0.9, soft=1.5)


def leaf_mask(f):
    """The clamp seen from the front: a leaf band over the forehead with long teeth and two leaf walls at the cheeks with teeth pointing in."""
    parts = [f.m_rect(-0.2, -0.2, 8.2, 1.25)]
    for i in range(6):                                                         # the upper teeth
        x = 0.7 + i * 1.3
        parts.append(f.m_poly([(x - 0.62, 1.1), (x + 0.62, 1.1), (x + 0.1 * (-1) ** i, 2.0 + 0.55 * ((i * 3) % 4) / 3.0)]))
    for side in (0, 1):
        x0 = 0.0 if side == 0 else 8.0
        sg = 1.0 if side == 0 else -1.0
        parts.append(f.m_rect(x0 - 0.2, 0.0, x0 + sg * 0.72, 8.2) if side == 0 else f.m_rect(x0 - 0.72, 0.0, x0 + 0.2, 8.2))
        for k in range(5):                                                       # the cheek teeth
            y = 2.5 + k * 1.1
            parts.append(f.m_poly([(x0 + sg * 0.6, y - 0.45), (x0 + sg * 0.6, y + 0.45), (x0 + sg * (1.35 + 0.12 * (k % 2)), y + 0.1)]))
    return H.union(*parts)


def paint_leaf(f, m):
    f.fill(m, H.lin((0, 0), (0, 8.0), [(0, LEAF_L), (0.3, LEAF), (1, LEAF_D)]))
    f.fill(m * f.m_band(1.2, 1.32), "#c8f08a", a=0.6)
    H.bevel(f, m, depth=0.22, gain=0.9)
    for i in range(6):
        x = 0.7 + i * 1.3
        f.line((x, 0.1), (x, 1.9), LEAF_D, 0.06, 0.03, a=0.45) 


def grin(f):
    """A wide sinister grin: a dark mouth with two rows of small pointed teeth, a pale lip line on the black half."""
    top = [(1.3 + 5.4 * i / 12.0, 6.55 + 0.38 * (1.0 - ((i - 6) / 6.0) ** 2)) for i in range(13)]
    bot = [(x, y + 0.55 * max(0.0, 1.0 - ((x - 4.0) / 2.7) ** 2) ** 0.7) for x, y in top]
    mouth = f.m_poly(top + bot[::-1])
    f.fill(mouth, "#1a0608")
    for i in range(12):
        x0, y0 = top[i]
        x1, y1 = top[i + 1]
        f.poly([(x0, y0), (x1, y1), ((x0 + x1) / 2, (y0 + y1) / 2 + 0.34)], "#f4f0c8", a=1.0)
    for i in range(1, 11, 2):
        x0, y0 = bot[i]
        x1, y1 = bot[i + 1]
        f.poly([(x0, y0), (x1, y1), ((x0 + x1) / 2, (y0 + y1) / 2 - 0.26)], "#d8d4a8", a=1.0)
    f.fill(f.m_polyline(top, 0.12) * f.m_rect(4.0, 0.0, 8.0, 8.0), "#8a8aa0", a=0.9)
    f.fill(f.m_polyline(bot, 0.1) * f.m_rect(4.0, 0.0, 8.0, 8.0), "#5a5a70", a=0.8)
    f.fill(f.m_polyline(top, 0.12) * f.m_rect(0.0, 0.0, 4.0, 8.0), "#3a3a40", a=0.9)
    f.fill(f.m_polyline(bot, 0.1) * f.m_rect(0.0, 0.0, 4.0, 8.0), "#6a6862", a=0.8)


def head(cv):
    f = cv.face("head", "front")
    f.fill(f.m_rect(0.0, 0.0, 4.0, 8.0), H.lin((0, 0), (0, 8), [(0, "#f8f6f0"), (0.5, WH), (1, "#b4b1ab")]))
    f.fill(f.m_rect(4.0, 0.0, 8.0, 8.0), H.lin((0, 0), (0, 8), [(0, "#2e2e3a"), (0.5, "#17171f"), (1, "#0a0a10")]))
    f.fill(f.m_rect(3.3, 2.6, 3.95, 8.0, soft=3.0), "#8a867e", a=0.28)                  # the pale half shades toward the seam
    f.fill(f.m_rect(4.05, 2.6, 5.0, 7.6, soft=3.0), "#40404f", a=0.45)                   # a cool rim light on the nose side of the black half
    f.fill(f.m_rect(3.94, 0.0, 4.06, 8.0), "#000000", a=0.9)                              # the seam
    f.shade(f.m_all(), 1.0 - 0.12 * (1.0 - H.smoothstep(0.0, 1.0, np.minimum(f.X, 8.0 - f.X))))
    H.brow(f, (0.9, 3.5), (3.5, 3.1), "#6c6a66", 0.30, 0.20, arch=0.06)
    H.brow(f, (7.1, 3.5), (4.5, 3.1), "#5a5a70", 0.30, 0.20, arch=0.06)
    slit_eye(f, 2.3, 4.7, 2.2, 1.75, True)
    slit_eye(f, 5.7, 4.7, 2.2, 1.75, False)
    f.line((3.7, 5.7), (3.88, 6.5), "#8c8984", 0.16, 0.26, a=0.65)                       # the nose on the split
    f.ellipse(4.34, 6.5, 0.26, 0.17, "#030305", a=0.9)
    grin(f)
    for ov in (False, True):
        g = cv.face("head", "front", ov)
        paint_leaf(g, leaf_mask(g))
    for nm in ("west", "east", "back"):
        for ov in (False, True):
            g = cv.face("head", nm, ov)
            g.fill(g.m_all(), H.lin((0, 0), (0, 8), [(0, LEAF_L), (0.4, LEAF), (1, LEAF_D)]))
            g.line((g.w / 2, 0.0), (g.w / 2, g.h), "#c8f08a", 0.12, 0.12, a=0.5)
            for k in range(7):
                g.line((g.w / 2, 0.6 + k * 1.1), (g.w / 2 + (2.2 if k % 2 else -2.2), 1.3 + k * 1.1), LEAF_D, 0.06, 0.03, a=0.5)
    t = cv.face("head", "top")
    t.fill(t.m_all(), H.lin((0, 0), (0, 8), [(0, LEAF_L), (1, LEAF)]))
    cv.face("head", "bottom").fill(cv.face("head", "bottom").m_all(), "#1a1a22")


# ================================================================================================ the cloak
def cloud(f, cx, cy, s, flip=False):
    """The Akatsuki cloud: a red cumulus with a white outline, three round lobes on a flat belly, a curled tail and a swirl inside each lobe; s = its size in model pixels."""
    k = -1.0 if flip else 1.0
    lobes = [(0.0, 0.14, 1.0, 0.36), (-0.62 * k, -0.1, 0.4, 0.4), (-0.02 * k, -0.3, 0.5, 0.5), (0.62 * k, -0.06, 0.38, 0.38), (-1.05 * k, 0.2, 0.22, 0.22), (1.05 * k, 0.16, 0.2, 0.2),
             (-0.5 * k, 0.4, 0.34, 0.26), (0.18 * k, 0.46, 0.4, 0.28), (0.78 * k, 0.4, 0.3, 0.24)]   # the underside is bumpy too, like the sleeve patch

    def mk(extra):
        return H.union(*[f.m_ellipse(cx + dx * s, cy + dy * s, rx * s + extra, ry * s + extra) for dx, dy, rx, ry in lobes])
    inner = mk(0.0)
    f.fill(mk(0.3), "#05050a", a=0.45)                                                     # a soft shadow round the outline
    f.fill(mk(0.17), CLOUD_W)
    f.fill(mk(0.17) * (1.0 - mk(0.07)), H.tone(CLOUD_W, 0.82), a=0.8)
    f.fill(inner, H.lin((0, cy - 0.7 * s), (0, cy + 0.6 * s), [(0, RED_L), (0.4, RED), (1, RED_D)]))
    H.bevel(f, inner, depth=0.28, gain=0.9)
    for dx, dy, r in ((-0.6 * k, -0.08, 0.2), (-0.02 * k, -0.3, 0.26), (0.6 * k, -0.04, 0.18)):          # the swirl of each lobe: a ring that opens toward the belly
        f.fill(f.m_ring(cx + dx * s, cy + dy * s, r * s, 0.07) * inner, RED_D, a=0.9)
        f.fill(f.m_ring(cx + dx * s, cy + dy * s, r * s * 0.5, 0.06) * inner, RED_D, a=0.7)
    f.ellipse(cx - 0.15 * s * k, cy - 0.52 * s, 0.3 * s, 0.06 * s, "#ffffff", a=0.4, soft=2.5)


def cloak(f, mask, folds, y0=0.0, y1=None):
    H.cloth(f, mask, BK, top=1.16, bottom=0.82, round_=0.14, fold_xs=list(folds), fold_depth=0.38, fold_y0=y0, fold_y1=y1, weave_k=0.05)
    H.sheen(f, mask, [x + 0.28 for x in folds], width=0.14, strength=0.22, tilt=0.04, color="#6a6a88", y0=y0, y1=y1)


def body(cv):
    for nm in SIDES:
        for f in lay(cv, "body", nm):
            cloak(f, f.m_all(), (1.1, 2.6, 5.4, 6.9) if f.w > 6 else (1.2, 2.8))
    for f in lay(cv, "body", "front"):
        f.line((4.0, 1.5), (4.0, 12.0), BK_D, 0.20, 0.20, a=0.95)                          # the centre seam, its zip and the stitched front edges
        f.line((4.14, 1.5), (4.14, 12.0), BK_L, 0.06, 0.06, a=0.8)
        for y in [1.9 + i * 0.95 for i in range(11)]:
            f.line((3.82, y), (3.94, y), "#8a8aa0", 0.09, 0.09, a=0.6)
        for x in (0.55, 7.45):
            H.stitch(f, [(x, 1.5), (x, 12.0)], "#52526a", width=0.05, dash=(0.3, 0.2), shadow=0.0)
        f.fill(f.m_band(8.6, 9.3), H.lin((0, 8.6), (0, 9.3), [(0, BK_L), (1, BK_D)]), a=0.5)
        collar_front(f)
        cloud(f, 2.4, 10.2, 1.75)
        cloud(f, 5.6, 6.0, 1.35, flip=True)
    for f in lay(cv, "body", "back"):
        f.line((4.0, 1.6), (4.0, 12.0), BK_D, 0.14, 0.14, a=0.7)
        collar_plain(f, 2.0)
        cloud(f, 4.0, 5.5, 2.5)
        cloud(f, 2.0, 10.3, 1.4, flip=True)
        cloud(f, 6.0, 10.6, 1.4)
    for nm in ("west", "east"):
        for f in lay(cv, "body", nm):
            collar_plain(f, 2.2)
            cloud(f, 2.0, 7.6, 1.2, flip=(nm == "east"))
    t = cv.face("body", "top", False)
    t.fill(t.m_all(), H.lin((0, 0), (0, 4), [(0, RED), (1, RED_D)]))
    t = cv.face("body", "top", True)
    t.fill(t.m_all(), H.rad((4, 2), 5, [(0, RED), (1, RED_D)]))
    t.fill(t.m_rect(1.2, 0.9, 6.8, 3.1, soft=1.0), "#000000", mode="erase")
    cv.face("body", "bottom").fill(cv.face("body", "bottom").m_all(), BK_D)


def collar_front(f):
    """The black stand collar; its red lining shows as a V at the throat with a white seam."""
    f.fill(f.m_band(0.0, 2.7), H.lin((0, 0), (0, 2.7), [(0, BK_L), (1, BK)]))
    v = f.m_poly([(1.0, -0.1), (7.0, -0.1), (4.0, 2.7)])
    f.fill(v, H.lin((0, 0), (0, 2.7), [(0, RED), (0.5, "#a81c24"), (1, RED_D)]))
    H.folds(f, v, [2.0, 3.0, 5.0, 6.0], depth=0.18, width=0.16, y0=0.0, y1=2.6)
    for pts in ([(1.0, 0.0), (4.0, 2.7)], [(7.0, 0.0), (4.0, 2.7)]):
        f.polyline(pts, "#f2efe6", 0.14, a=0.95)
        f.polyline([(pts[0][0], pts[0][1] + 0.1), (pts[1][0], pts[1][1] - 0.1)], "#05050a", 0.07, a=0.5)
    f.fill(f.m_band(0.0, 0.35), H.tone(RED, 1.15), a=0.9)


def collar_plain(f, h):
    f.fill(f.m_band(0.0, h), H.lin((0, 0), (0, h), [(0, BK_L), (1, BK)]))
    f.fill(f.m_band(0.0, 0.35), RED, a=0.9)
    H.shadow_under(f, h, 0.5, 0.3)
    H.trim_line(f, [(0.0, h + 0.05), (f.w, h + 0.05)], "#d8d6d0", width=0.08, outline=0.5, spec=0.3)


# ================================================================================================ the sleeves
def arm(cv, part):
    right = part == "arm_r"
    hand = ("#fbfaf4", WH, "#bdbab4") if right else ("#2c2c3a", "#17171f", "#08080c")
    outer = "west" if right else "east"
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            cloak(f, f.m_band(0.0, 10.9), (1.0, 2.6) if nm in ("front", "back") else (1.3,), y1=10.0)
            f.fill(f.m_band(0.0, 1.6), H.lin((0, 0), (0, 1.6), [(0, BK_L), (1, BK)]), a=0.9)           # the broad shoulder
            f.fill(f.m_band(8.4, 10.9), H.lin((0, 8.4), (0, 10.9), [(0, BK), (1, BK_D)]))            # the wide cuff: a red lining roll and a black hem
            H.folds(f, f.m_band(8.4, 10.9), [0.8, 2.0, 3.2], depth=0.2, width=0.2, y0=8.5, y1=10.9)
            H.trim_band(f, 8.2, 8.75, RED, outline=0.4, spec=0.3)
            f.fill(f.m_band(10.55, 10.9), RED_D, a=0.9)
        ov.fill(ov.m_band(10.85, 12.0), "#000000", mode="erase")
        base.fill(base.m_band(10.9, 12.0), H.lin((0, 10.9), (0, 12.0), [(0, hand[1]), (1, hand[2])]))
        if nm == "front":
            base.fill(base.m_rect(1.0, 11.0, 3.0, 11.8, soft=3.0), hand[0], a=0.45)
            for x in (1.0, 2.0, 3.0):
                base.line((x, 11.15), (x, 11.95), H.tone(hand[2], 0.8), 0.05, 0.05, a=0.7)
    for f in lay(cv, part, "front"):
        cloud(f, 2.0, 5.2, 1.5, flip=not right)
    for f in lay(cv, part, "back"):
        cloud(f, 2.0, 5.2, 1.5, flip=right)
    for f in lay(cv, part, outer):
        cloud(f, 2.0, 5.3, 1.5, flip=not right)
    t = cv.face(part, "top", False)
    t.fill(t.m_all(), H.rad((2, 2), 3, [(0, BK_L), (1, BK)]))
    t = cv.face(part, "top", True)
    t.fill(t.m_all(), H.rad((2, 2), 3, [(0, BK_L), (1, BK)]))
    b = cv.face(part, "bottom", False)
    b.fill(b.m_all(), hand[1])


# ================================================================================================ the legs
def trousers(f):
    H.cloth(f, f.m_all(), "#101017", top=1.12, bottom=0.82, round_=0.12, fold_xs=[1.0, 2.8], fold_depth=0.2, weave_k=0.05)
    for i in range(4):                                                                      # shin wraps: a slanted spiral of white bandage under the knee
        y0 = 7.35 + i * 0.92
        strip = f.m_poly([(-0.1, y0), (f.w + 0.1, y0 - 0.3), (f.w + 0.1, y0 + 0.62), (-0.1, y0 + 0.92)])
        f.fill(strip, H.lin((0, y0), (0, y0 + 0.9), [(0, "#fbf9f2"), (0.55, "#e2dfd6"), (1, "#b2afa8")]))
        f.fill(f.m_line((0.0, y0 + 0.93), (f.w, y0 + 0.63), 0.05, 0.05), "#05050a", a=0.45)
    f.fill(f.m_rect(0.0, 7.1, f.w, 7.5), "#d8d5cc")                                      # the tie of the wraps
    H.shadow_under(f, 7.1, 0.35, 0.25)
    f.fill(f.m_band(10.55, 12.0), H.lin((0, 10.55), (0, 12.0), [(0, "#22222c"), (1, "#07070a")]))         # the sandal and its strap
    f.line((0.0, 10.85), (f.w, 10.85), "#d8d6d0", 0.16, 0.16, a=0.9)
    f.fill(f.m_band(11.65, 12.0), "#000000", a=0.8)


def skirt(f):
    """The cloak's lower skirt on the second layer: cloak to the shin, a red lining line at the hem."""
    m = f.m_band(0.0, 7.1)
    cloak(f, m, (1.0, 2.8), y1=7.0)
    hem = f.m_band(6.45, 7.1)
    f.fill(hem, H.lin((0, 6.45), (0, 7.1), [(0, BK), (0.55, RED_D), (1, RED)]))
    H.shadow_under(f, 7.1, 0.0, 0.0)
    f.fill(f.m_band(6.2, 6.5), H.tone(BK, 0.7), a=0.5)


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        trousers(base)
        skirt(ov)
    cv.face(part, "bottom").fill(cv.face(part, "bottom").m_all(), "#050508")
    cv.face(part, "top").fill(cv.face(part, "top").m_all(), BK)


def paint(spec, cid, masks):
    cv = H.Canvas(256, False)
    for f in cv.faces(overlay=(False,)):
        f.fill(f.m_all(), BK)
    head(cv)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, "anime_mane", HAIR, False

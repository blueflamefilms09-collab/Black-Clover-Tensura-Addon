"""Mr. 2 Bon Clay (One Piece, Bentham, the Okama Kenpo ballet master), HD 256x256 (3.38.0, pilot 2 of docs/bosses/hd_outfits.md).

Colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/mr_2_bon_clay.py) through the HD bridge; then drawn again at 4x detail: the made-up face (arched brows, blue-violet
eyeshadow, big purple lashed eyes, pink cheeks, a wide red-lipped grin with teeth), the hot-pink satin leotard with the white heart and the deep V, the white feather boa and feather epaulettes with pink tips,
the magenta sash with a rhinestone, the layered tulle tutu, the white satin gloves with pink feather cuffs, white tights and the ballet boots with blue ribbon lacing and pink soles.
Hair: `anime_afro_wide` (a wider, fuller afro attached to the head) once the hair table has it, `anime_afro` until then. Original art from memory of the canon look.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

PINK, PINK_D, PINK_L = "#ff4f9a", "#b81e66", "#ff9ac6"
MAG = "#d6286f"
WHITE, WHITE_D = "#f7f2fb", "#d8cfe4"
SKIN = ("#f1cfc6", "#fbe4da", "#fff1ea")
BLUE = "#5864e8"
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    return (cv.face(part, name, False), cv.face(part, name, True))


def head(cv, hair_rgb):
    f = cv.face("head", "front")
    H.skin_face(f, SKIN, hair=H.tone(hair_rgb, 1.0), hairline=1.7, wave=0.5, shadow=0.16)
    # eyeshadow: blue-violet feathered arcs over the eyes, up to the brow line
    for cx, rot in ((2.3, -12), (5.7, 12)):
        m = f.m_ellipse(cx, 3.95, 1.75, 1.05, rot=rot, soft=7.0)
        f.fill(m, H.lin((0, 3.0), (0, 5.0), [(0, "#3a3ac8"), (0.5, "#6a68f0"), (1, "#a8a4ff")]), 0.88)
    H.brow(f, (3.3, 2.5), (0.85, 2.15), "#1c1830", 0.20, 0.08, arch=0.28)
    H.brow(f, (4.7, 2.5), (7.15, 2.15), "#1c1830", 0.20, 0.08, arch=0.28)
    iris = ("#2c0f6a", "#8650d8", "#e6ccff")
    for cx in (2.3, 5.7):
        H.eye(f, cx, 5.15, 2.6, 2.4, iris, lash="#140c24", tilt=0.18, flick=0.46, lash_w=0.42, lid_shadow=0.22)
    for cx, sg in ((2.3, -1), (5.7, 1)):                                                         # an extra pair of lash flicks at the outer corner
        for k, (dx, dy) in enumerate(((0.55, -0.55), (0.75, -0.18))):
            x0 = cx + sg * 1.3
            f.line((x0, 4.55 + k * 0.28), (x0 + sg * dx, 4.55 + k * 0.28 + dy), "#140c24", 0.16, 0.05)
    H.blush(f, 0.95, 6.35, 1.05, 0.75, "#ff7a9c", 0.62)
    H.blush(f, 7.05, 6.35, 1.05, 0.75, "#ff7a9c", 0.62)
    H.nose(f, 4.0, 6.35, "#e4a8a0", 0.2)
    H.mouth_smile(f, 4.0, 6.95, 4.0, color="#c4122e", lip="#ff5870", depth=0.45, thick=0.30, teeth="#ffffff", open_h=0.6)
    f.ellipse(3.0, 7.0, 0.55, 0.12, "#ffffff", a=0.35, soft=3.0)


def heart(f, cx, cy, w, glow=False):
    pts = H.heart_pts(cx, cy, w)
    m = f.m_poly(pts)
    f.fill(f.m_poly(H.heart_pts(cx, cy, w + 0.34)), H.tone(PINK_D, 0.8), 0.9)
    f.fill(m, H.rad((cx - w * 0.2, cy - w * 0.25), w * 0.9, [(0, "#ffffff"), (0.6, WHITE), (1, WHITE_D)]))
    H.bevel(f, m, depth=0.32, gain=0.9, spec=0.5, spec_pow=10)
    f.ellipse(cx - w * 0.22, cy - w * 0.2, w * 0.13, w * 0.07, "#ffffff", a=0.9, rot=-35, soft=1.6)
    H.sparkle(f, cx + w * 0.26, cy + w * 0.06, w * 0.17, "#ffffff", 0.9)


def bodice(f):
    """The hot-pink satin bodice with the deep V, the heart and the rhinestone sash (base layer front)."""
    f.fill(f.m_band(0.0, 7.6), H.lin((0, 0), (0, 7.6), [(0, H.tone(PINK, 1.05)), (0.6, PINK), (1, H.tone(PINK, 0.82))]))
    f.shade(f.m_band(0.0, 7.6), 1.0 - 0.10 * H.smoothstep(2.4, 4.0, f.X) * (1 - H.smoothstep(4.0, 8.0, f.X)) * 0.0)
    H.sheen(f, f.m_band(0.0, 7.6), [1.25, 6.7], width=0.28, strength=0.20, tilt=0.12, y0=0.6, y1=7.2, color="#ffd0e6")
    v = f.m_poly([(2.3, -0.2), (5.7, -0.2), (4.0, 3.15)])
    f.fill(v, H.lin((0, 0), (0, 3.2), [(0, SKIN[1]), (1, SKIN[0])]))
    H.trim_line(f, [(2.3, 0.0), (4.0, 3.15), (5.7, 0.0)], PINK_D, width=0.2, outline=0.0, spec=0.0)
    H.trim_line(f, [(2.45, 0.0), (4.0, 2.95), (5.55, 0.0)], PINK_L, width=0.07, outline=0.0, spec=0.0, a=0.8)
    heart(f, 4.0, 4.0, 3.5)


def sash_and_tutu(f, back=False):
    sash = f.m_band(7.1, 9.0)
    f.fill(sash, H.lin((0, 7.1), (0, 9.0), [(0, "#f0508e"), (0.45, MAG), (1, "#8e1450")]))
    H.folds(f, sash, [1.4, 3.1, 5.0, 6.6], depth=0.12, width=0.2, y0=7.1, y1=9.0)
    H.sheen(f, sash, [2.2, 5.9], width=0.2, strength=0.25, tilt=0.0, y0=7.2, y1=8.9, color="#ffb0d0")
    H.shadow_under(f, 9.0, 0.5, 0.2)
    if not back:
        H.gem(f, 4.0, 8.05, 0.5, "#ff5aa8", setting="#e8e0f4", glow=True)
    else:
        H.stud(f, 4.0, 8.05, 0.32, "#e8e0f4")
    H.scallops(f, f.m_band(8.7, 12.0), 8.9, rows=2, cell=2.0, depth=2.2, base="#fff4fa", tip="#ffc4de", edge=PINK_L, edge_a=0.45, shadow=0.20)


def boa(f, notch=True):
    m = f.m_band(0.0, 2.4)
    if notch:
        m = H.minus(m, f.m_poly([(3.0, 0.9), (5.0, 0.9), (4.0, 2.9)], soft=1.4))
    H.scallops(f, m, 0.0, rows=2, cell=1.34, depth=1.6, base="#f8e4f0", tip="#ffffff", edge="#f0b4d4", edge_a=0.28, shadow=0.14)


def bow(f, cx, cy):
    """A satin bow: two loops, two ribbon tails and a knot, each with a gradient, a bevel and a highlight."""
    sat = lambda a, b: H.lin((0, cy - 1.2), (0, cy + 2.2), [(0, a), (1, b)])
    for sg in (-1, 1):                                                                                  # the tails
        tail = f.m_poly([(cx + sg * 0.2, cy + 0.2), (cx + sg * 1.5, cy + 2.3), (cx + sg * 0.8, cy + 2.5), (cx + sg * 0.1, cy + 0.9)])
        f.fill(f.m_poly([(cx + sg * 0.2, cy + 0.2), (cx + sg * 1.62, cy + 2.4), (cx + sg * 0.74, cy + 2.62), (cx, cy + 0.9)]), H.tone(MAG, 0.55), 0.8)
        f.fill(tail, sat("#f0508e", "#a01a58"))
    for sg in (-1, 1):                                                                                  # the loops
        loop = f.m_ellipse(cx + sg * 1.45, cy - 0.15, 1.45, 0.88, rot=sg * 22)
        f.fill(f.m_ellipse(cx + sg * 1.45, cy - 0.05, 1.6, 1.0, rot=sg * 22), H.tone(MAG, 0.55), 0.85)
        f.fill(loop, sat("#ff7ab2", "#b01e62"))
        hole = f.m_ellipse(cx + sg * 1.55, cy - 0.1, 0.9, 0.45, rot=sg * 22)
        f.fill(hole, H.tone(MAG, 0.62), 0.9)
        H.bevel(f, loop, depth=0.3, gain=0.8, spec=0.5, spec_pow=8)
        f.ellipse(cx + sg * 1.2, cy - 0.55, 0.65, 0.12, "#ffffff", rot=sg * 22, a=0.55, soft=2.5)
    knot = f.m_ellipse(cx, cy + 0.05, 0.5, 0.62)
    f.fill(f.m_ellipse(cx, cy + 0.12, 0.6, 0.72), H.tone(MAG, 0.5), 0.9)
    f.fill(knot, H.rad((cx - 0.15, cy - 0.2), 0.8, [(0, "#ff9ac6"), (1, "#c8266e")]))
    H.bevel(f, knot, depth=0.25, gain=0.8, spec=0.6, spec_pow=8)
    H.sparkle(f, cx + 0.1, cy - 0.15, 0.3, "#ffffff", 0.8)


def body(cv):
    f = cv.face("body", "front", False)
    bodice(f)
    for f in lay(cv, "body", "front"):
        if f.overlay:
            f.clear(f.m_band(2.5, 7.0))                                                            # the bodice shows between the boa and the sash
        sash_and_tutu(f)
        boa(f)
    f = cv.face("body", "back", False)
    f.fill(f.m_band(0.0, 7.6), H.lin((0, 0), (0, 7.6), [(0, H.tone(PINK, 0.98)), (0.6, H.tone(PINK, 0.9)), (1, H.tone(PINK, 0.74))]))
    H.sheen(f, f.m_band(0.0, 7.6), [2.0, 6.0], width=0.3, strength=0.14, tilt=0.1, y0=0.8, y1=7.0, color="#ffd0e6")
    for nm in ("west", "east"):
        g = cv.face("body", nm, False)
        g.fill(g.m_band(0.0, 7.3), H.lin((0, 0), (0, 7.3), [(0, H.tone(PINK, 0.95)), (1, H.tone(PINK, 0.76))]))
    for nm in ("back", "west", "east"):
        for f in lay(cv, "body", nm):
            if f.overlay:
                f.clear(f.m_band(2.5, 7.0))
            sash_and_tutu(f, back=(nm == "back"))
            boa(f, notch=False)
    # the big satin bow on the back: two loops, two tails, a knot (base layer; the overlay is cleared there)
    f = cv.face("body", "back", False)
    bow(f, 4.0, 4.5)


def arm(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        base.fill(base.m_band(0.0, 7.4), H.lin((0, 0), (0, 7.4), [(0, SKIN[2]), (0.5, SKIN[1]), (1, SKIN[0])]))
        base.shade(base.m_band(0.0, 7.4), 1.0 - 0.07 * H.smoothstep(0.45 * base.w, base.w, base.X if base.light[0] > 0 else base.w - base.X))
        for f in (base, ov):
            glove = f.m_band(7.0, 12.0)
            H.cloth(f, glove, WHITE, top=1.02, bottom=0.90, fold_xs=[], weave_k=0.0)
            H.sheen(f, glove, [1.0], width=0.20, strength=0.32, tilt=0.0, y0=8.4, y1=11.6)
            band = f.m_band(4.15, 5.25)
            f.fill(band, H.lin((0, 4.15), (0, 5.25), [(0, PINK_L), (0.5, PINK), (1, PINK_D)]))
            H.sheen(f, band, [1.0], width=0.18, strength=0.3, y0=4.2, y1=5.2, color="#ffffff")
        ov.fill(ov.m_band(5.3, 7.0), "#000000", mode="erase")                                   # the bare arm shows between the epaulette, the armband and the cuff
        for f in (base, ov):
            if f is ov:
                pass
        for f in (ov,):
            H.scallops(f, f.m_band(0.0, 4.6), 0.0, rows=2, cell=1.34, depth=2.4, base="#f9e6f1", tip="#ffffff", edge="#f0b4d4", edge_a=0.28, shadow=0.14)
            for x in (0.5, 2.0, 3.5):
                f.ellipse(x, 3.9, 0.22, 0.30, PINK_L, a=0.55, soft=2.5)
            H.scallops(f, f.m_band(6.9, 8.6), 6.9, rows=2, cell=1.34, depth=1.2, base=PINK_L, tip=PINK, edge=PINK_D, edge_a=0.35, shadow=0.24)
        base.fill(base.m_band(0.0, 4.6), H.lin((0, 0), (0, 4.6), [(0, "#fff5fa"), (1, "#f5d8e8")]))
    for f in lay(cv, part, "top"):
        f.fill(f.m_all(), H.rad((f.w * 0.45, f.h * 0.45), f.w, [(0, "#ffffff"), (1, "#f0d4e4")]))


def leg(cv, part):
    for nm in SIDES:
        base, ov = lay(cv, part, nm)
        for f in (base, ov):
            tights = f.m_band(0.0, 12.0)
            H.cloth(f, tights, WHITE, top=1.03, bottom=0.90, round_=0.10, fold_xs=[], weave_k=0.0)
            H.sheen(f, tights, [1.0], width=0.2, strength=0.30, tilt=0.0, y0=0.5, y1=11.0)
        # the tutu's lower edge on the thigh
        H.scallops(ov, ov.m_band(0.0, 4.6), -0.1, rows=2, cell=2.0, depth=2.6, base="#fff4fa", tip="#ffc4de", edge=PINK_L, edge_a=0.45, shadow=0.2)
        base.fill(base.m_band(0.0, 3.0), "#fdf2f8")
        # ballet boot: pink feather top, blue ribbon lacing, pink sole
        for f in (ov, base):
            boot = f.m_band(6.8, 12.0)
            H.cloth(f, boot, WHITE, top=1.03, bottom=0.88, round_=0.10, fold_xs=[], weave_k=0.0)
            H.sheen(f, boot, [1.1], width=0.2, strength=0.35, y0=8.0, y1=11.0)
            if nm == "front":
                for x0, x1 in ((0.7, 3.3), (3.3, 0.7)):
                    H.trim_line(f, [(x0, 8.2), (x1, 10.4)], BLUE, width=0.26, outline=0.35, spec=0.3)
                H.trim_line(f, [(0.5, 8.0), (3.5, 8.0)], BLUE, width=0.24, outline=0.3, spec=0.3)
                H.trim_line(f, [(0.5, 10.7), (3.5, 10.7)], BLUE, width=0.24, outline=0.3, spec=0.3)
            else:
                H.trim_line(f, [(0.0, 8.1), (f.w, 8.1)], BLUE, width=0.2, outline=0.3, spec=0.3)
            sole = f.m_band(11.3, 12.0)
            f.fill(sole, H.lin((0, 11.3), (0, 12.0), [(0, PINK), (1, PINK_D)]))
        H.scallops(ov, ov.m_band(6.2, 8.2), 6.2, rows=2, cell=1.34, depth=1.2, base=PINK_L, tip="#ffffff", edge=PINK, edge_a=0.35, shadow=0.24)
        ov.fill(ov.m_band(4.5, 6.0), "#000000", mode="erase")                                    # bare tights between the tutu and the boot top
    for f in lay(cv, part, "bottom"):
        f.fill(f.m_all(), PINK_D)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    if masks.get("anime_afro_wide") is not None or "anime_afro_wide" in masks:
        hair = "anime_afro_wide"
    cv = H.Canvas(256, slim)
    mats = {"skin": [H.near("#fbe4da", 0.12), H.near("#f1cfc6", 0.1), H.near("#e8c0b6", 0.1)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arm(cv, "arm_r")
    arm(cv, "arm_l")
    leg(cv, "leg_r")
    leg(cv, "leg_l")
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

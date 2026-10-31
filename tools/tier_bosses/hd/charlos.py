"""Charlos (Saint Charlos, a Celestial Dragon of One Piece), HD 256x256 (3.38.0, pilot 1 of docs/bosses/hd_outfits.md).

The colour blocking and layout come from the 64x64 hand-painted remake (tools/tier_bosses/remake/charlos.py) through the HD bridge (smooth upscale, gradients, outlines, cloth folds, metal bevel);
then the things that must be crisp are drawn again at 4x detail on the second layer: the anime face with amber eyes, the gold circlet with its ruby, the ruffled collar, the crimson waistcoat with
gold piping and studs, the sash and its buckle, the cape's gold sun crest, the epaulettes with fringe, the ruffle cuffs, the gold-striped trousers and the crimson boots with gold cuffs.
Original art from memory of the character's look (see remake_a.md), nothing traced. Glow: studs, buckle, crest, ruby.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import hdlib as H  # noqa: E402

GOLD, GOLD_D = "#e3b232", "#a8741a"
CRIM, CRIM_D = "#b3283a", "#7a1a2c"
CREAM = ("#a89a86", "#c4b9a6", "#dcd3c0", "#eee8d8", "#fbf7ea", "#ffffff")
SKIN = ("#e9b997", "#f7d6bb", "#fee8d4")
SIDES = ("front", "back", "west", "east")


def lay(cv, part, name):
    """The base island and the second-layer island of a face: draw the crisp things on BOTH (the second layer is what the game shows near, the base alone is what it shows beyond the LOD distance)."""
    return (cv.face(part, name, False), cv.face(part, name, True))


def head(cv, hair):
    base = cv.face("head", "front")
    H.skin_face(base, SKIN, hair=H.tone(hair, 0.92), hairline=1.9, wave=0.45)
    # eyes: big amber anime eyes, the brows a little uneven (a smug look)
    iris = ("#4a2208", "#b86c26", "#f6c878")
    H.brow(base, (3.4, 3.35), (1.0, 3.0), "#a67a18", 0.26, 0.12, arch=0.10)
    H.brow(base, (4.6, 3.15), (7.0, 2.6), "#a67a18", 0.26, 0.12, arch=0.14)
    H.eye(base, 2.3, 4.95, 2.55, 2.25, iris, lash="#3a1c0c", tilt=0.10)
    H.eye(base, 5.7, 4.95, 2.55, 2.25, iris, lash="#3a1c0c", tilt=0.10)
    H.blush(base, 1.2, 6.2, 0.95, 0.55, "#f08a88", 0.5)
    H.blush(base, 6.8, 6.2, 0.95, 0.55, "#f08a88", 0.5)
    H.nose(base, 4.0, 6.35, "#d19a84", 0.24)
    H.mouth_smile(base, 4.2, 7.15, 2.1, color="#a8403e", lip="#e08a80", depth=0.22, thick=0.15)
    base.line((5.15, 7.30), (5.45, 7.02), "#a8403e", 0.14, 0.10, a=0.9)                  # the smug corner
    # the gold circlet with the ruby and the white-gold neck ring on the hat layer
    for nm in SIDES:
        f = cv.face("head", nm, True)
        H.trim_band(f, 2.0, 2.95, GOLD, outline=0.5)
        H.trim_band(f, 7.05, 7.95, "#f0e2a8", outline=0.45, spec=0.35)
    f = cv.face("head", "front", True)
    H.gem(f, 4.0, 2.48, 0.46, "#e0183c", setting=GOLD, glow=True)
    for x in (2.2, 5.8):
        H.stud(f, x, 7.5, 0.16, GOLD)
    for nm in ("west", "east", "back"):
        f = cv.face("head", nm, True)
        for x in (1.3, 3.3, 5.3, 7.3) if nm == "back" else (1.5, 3.2, 5.0, 6.6):
            H.stud(f, x, 2.47, 0.15, "#f6d868")


def frill(f, y0, rows=2, cell=1.6, depth=1.5):
    H.scallops(f, f.m_band(y0, y0 + depth + 0.62 * (rows - 1), soft=1), y0, rows=rows, cell=cell, depth=depth, base=H.tone(CREAM[3], 0.88), tip="#ffffff", edge=CREAM[1], edge_a=0.55, shadow=0.26)


WHITE = "#f4efe0"


def body_front(f):
    H.cloth(f, f.m_band(2.5, 11.05), WHITE, top=1.05, bottom=0.90, fold_xs=[0.5, 7.5], fold_depth=0.08)
    vest = f.m_poly([(1.0, 2.7), (7.0, 2.7), (7.0, 8.2), (1.0, 8.2)], soft=1.0)
    f.fill(vest, H.lin((0, 2.7), (0, 8.2), [(0, H.tone(CRIM, 0.78)), (0.5, CRIM), (1, H.tone(CRIM, 1.12))]))
    H.folds(f, vest, [2.2, 5.9], depth=0.13, width=0.22, y0=3.0, y1=8.2)
    H.sheen(f, vest, [2.7], width=0.22, strength=0.22, tilt=0.05, y0=3.4, y1=7.8, color="#ff9aa6")
    for x in (1.15, 6.85):
        H.trim_line(f, [(x, 2.7), (x, 8.3)], GOLD, width=0.32)
        H.stitch(f, [(x + (0.38 if x < 4 else -0.38), 3.0), (x + (0.38 if x < 4 else -0.38), 8.0)], "#f2dca0", width=0.07, dash=(0.34, 0.2))
    H.trim_line(f, [(1.0, 2.75), (7.0, 2.75)], GOLD, width=0.26)
    sash = f.m_rect(0.0, 8.15, 8.0, 9.85)
    f.fill(sash, H.lin((0, 8.15), (0, 9.85), [(0, H.tone(CRIM, 1.15)), (0.5, CRIM), (1, H.tone(CRIM, 0.7))]))
    H.folds(f, sash, [1.0, 3.0, 5.2, 7.0], depth=0.12, width=0.18, y0=8.2, y1=9.8)
    H.shadow_under(f, 9.85, 0.5, 0.22)
    frill(f, 0.0)
    H.trim_band(f, 2.1, 2.95, GOLD, x0=2.8, x1=5.2, r=0.3)
    for y in (4.5, 6.5):
        H.stud(f, 4.0, y, 0.38, GOLD, glow=True)
    H.trim_band(f, 8.1, 9.95, GOLD, x0=2.6, x1=5.4, r=0.3, outline=0.7, glow=None)          # the buckle
    f.rect(3.25, 8.62, 4.75, 9.43, H.tone(CRIM_D, 0.75), r=0.15)
    f.rect(3.4, 8.9, 4.6, 9.15, H.metal_paint(8.9, 9.15, GOLD), r=0.1, glow=GOLD)
    H.bevel(f, f.m_rect(3.4, 8.9, 4.6, 9.15, 0.1), depth=0.12, gain=1.2, spec=0.5)
    H.shadow_under(f, 10.0, 0.5, 0.15)
    H.trim_band(f, 11.0, 12.0, GOLD)
    for x in (0.9, 2.3, 3.7, 5.1, 6.5):
        H.stud(f, x, 11.5, 0.17, "#f6dc7c")


def body_back(f):
    cape = f.m_rect(0.0, 2.7, 8.0, 11.0)
    f.fill(cape, H.lin((0, 2.7), (0, 11.0), [(0, H.tone(CRIM, 0.85)), (0.6, CRIM), (1, H.tone(CRIM, 0.75))]))
    H.folds(f, cape, [1.3, 2.7, 5.3, 6.7], depth=0.15, width=0.28, y0=3.0, y1=11.0, sway=0.3)
    frill(f, 0.0)
    for x in (0.3, 7.7):
        H.trim_line(f, [(x, 2.7), (x, 11.0)], GOLD, width=0.46)
    cx, cy = 4.0, 7.0
    for i in range(14):
        a0 = math.tau * i / 14
        a1 = a0 + math.tau / 24
        a2 = a0 - math.tau / 24
        tip = (cx + math.cos(a0) * 2.6, cy + math.sin(a0) * 2.6)
        m = f.m_poly([(cx + math.cos(a1) * 1.3, cy + math.sin(a1) * 1.3), tip, (cx + math.cos(a2) * 1.3, cy + math.sin(a2) * 1.3)])
        f.fill(m, H.lin((cx, cy - 2.4), (cx, cy + 2.4), [(0, "#f6d868"), (1, GOLD_D)]), glow="#e8b830")
        H.bevel(f, m, depth=0.12, gain=1.0)
    f.ellipse(cx, cy, 1.3, 1.3, GOLD_D, glow=GOLD)
    f.ellipse(cx, cy, 1.18, 1.18, H.rad((cx - 0.4, cy - 0.5), 1.8, [(0, "#fff0a0"), (0.5, GOLD), (1, GOLD_D)]), glow=GOLD)
    f.fill(f.m_ring(cx, cy, 0.86, 0.10), GOLD_D, 0.7)
    H.gem(f, cx, cy, 0.46, "#e0183c", setting=None, glow=True)
    H.trim_band(f, 11.0, 12.0, GOLD)
    H.shadow_under(f, 10.9, 0.3, 0.1)


def body_side(f):
    H.cloth(f, f.m_band(2.5, 11.05), WHITE, top=1.05, bottom=0.90, fold_xs=[1.2, 2.8], fold_depth=0.07)
    sash = f.m_rect(0.0, 8.15, 4.0, 9.85)
    f.fill(sash, H.lin((0, 8.15), (0, 9.85), [(0, H.tone(CRIM, 1.15)), (0.5, CRIM), (1, H.tone(CRIM, 0.7))]))
    frill(f, 0.0, cell=1.0)
    H.trim_band(f, 11.0, 12.0, GOLD)


def body(cv):
    for f in lay(cv, "body", "front"):
        body_front(f)
    for f in lay(cv, "body", "back"):
        body_back(f)
    for nm in ("west", "east"):
        for f in lay(cv, "body", nm):
            body_side(f)


def sleeve(f, part):
    H.cloth(f, f.m_band(2.5, 11.0), WHITE, top=1.05, bottom=0.86, fold_xs=[1.0, 2.7], fold_depth=0.08, fold_y0=4.0, fold_y1=9.5)
    ep = f.m_rect(0.0, 0.0, f.w, 3.6)
    f.fill(ep, H.metal_paint(0.0, 3.6, GOLD))
    H.scallops(f, f.m_band(2.6, 4.6), 2.5, rows=1, cell=0.8, depth=1.3, base=GOLD, tip=GOLD_D, edge=GOLD_D)
    H.bevel(f, f.m_rect(0.0, 0.0, f.w, 3.4), depth=0.3, gain=1.1, spec=0.5)
    H.fringe(f, 0.25, f.w - 0.25, 3.6, 1.0, n=9, color="#e8bc3c", dark=GOLD_D, seed=(0 if part == "arm_r" else 5))
    H.stud(f, f.w / 2.0, 1.4, 0.3, "#f6d868", glow=True)
    H.trim_band(f, 9.05, 9.95, GOLD)
    H.scallops(f, f.m_band(9.9, 11.5), 9.85, rows=2, cell=0.9, depth=1.0, base=CREAM[3], tip=CREAM[5], edge=CREAM[1])


def arms(cv):
    for part in ("arm_r", "arm_l"):
        for nm in SIDES:
            base, ov = lay(cv, part, nm)
            sleeve(base, part)
            sleeve(ov, part)
            ov.fill(ov.m_band(11.45, 12.0), "#000000", mode="erase")                  # the hand shows under the ruffle
            base.fill(base.m_band(11.45, 12.0), H.lin((0, 11.45), (0, 12.0), [(0, SKIN[0]), (1, SKIN[1])]))
        for f in lay(cv, part, "top"):
            f.fill(f.m_all(), H.rad((f.w * 0.4, f.h * 0.4), f.w, [(0, "#f6d868"), (1, GOLD_D)]))
            H.stud(f, f.w / 2.0, f.h / 2.0, 0.6, GOLD, glow=True)


def leg(f, part, nm):
    H.cloth(f, f.m_band(0.0, 7.2), WHITE, top=1.05, bottom=0.86, fold_xs=[1.2, 2.8], fold_depth=0.08, fold_y0=0.5, fold_y1=7.0)
    H.trim_band(f, 7.0, 8.6, GOLD)
    boot = f.m_band(8.6, 12.0)
    f.fill(boot, H.lin((0, 8.6), (0, 12.0), [(0, H.tone(CRIM, 1.05)), (0.5, CRIM), (1, H.tone(CRIM_D, 0.8))]))
    H.sheen(f, boot, [1.0], width=0.2, strength=0.22, tilt=0.06, y0=8.9, y1=11.4, color="#ff9aa6")
    H.shadow_under(f, 8.6, 0.5, 0.25)
    H.trim_line(f, [(0.0, 10.5), (f.w, 10.5)], GOLD, width=0.2, outline=0.3)
    H.stud(f, f.w / 2.0, 10.5, 0.2, "#f6d868")
    f.fill(f.m_band(11.6, 12.0), H.tone(CRIM_D, 0.6), 0.9)


def legs(cv):
    for part in ("leg_r", "leg_l"):
        for nm in SIDES:
            for f in lay(cv, part, nm):
                leg(f, part, nm)
        side = "west" if part == "leg_r" else "east"
        for f in lay(cv, part, side):
            H.trim_line(f, [(2.0, 0.0), (2.0, 7.0)], GOLD, width=0.62)
            H.stitch(f, [(1.55, 0.2), (1.55, 6.8)], "#fff0b8", width=0.06, dash=(0.3, 0.2), shadow=0.2)


def paint(spec, cid, masks):
    img64, glow64, hair, hrgb, slim = H.blueprint(cid, spec, masks)
    hrgb = (255, 208, 8)                                                           # 3.38 group a: a more saturated gold (the hair tint pipeline renders the remake's #ffe040 as khaki)
    cv = H.Canvas(256, slim)
    mats = {"metal": [H.family(GOLD, 20, 0.5)], "gem": [H.near("#ff6a78", 0.2), H.near("#c81c40", 0.2)]}
    H.from64(cv, img64, None, mats, skip=lambda part, name, ov: part == "head" and name == "front" and not ov, soft=0.75)
    head(cv, hrgb)
    body(cv)
    arms(cv)
    legs(cv)
    skin, glow = cv.finish()
    return skin, glow, hair, hrgb, slim

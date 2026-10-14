"""Needler (card: "Needler", weapon, one-of-a-kind): a deep purple crossbow with sleek engravings along the metalwork: a jagged, swept-back
purple blade-like prod with silvery grey arms, a drawn string, silver fittings and a silver grip. The 3D model, built with the owner's method, in the
core's five states (tools/relics/ranged_meshes.py moves the limbs and the string of the finished rest mesh): `needler` (string slack across the limbs),
`needler_pull1..3` (loading) and `needler_charged` (cocked, a needle on the rail).

Reference: the card, 240 x 240 (very small): the crossbow's stock runs from the butt (190, 150) to the muzzle's stirrup (40, 195)... the span of the
prod (the long way across) is 150 px of the picture; a repeating crossbow has a 50 cm prod, so K = 0.333 cm per px. The stock is 62 cm.
Axis (as the other crossbows of the mod): y along the stock, the butt at -26, the muzzle at +36; the prod across x at y = +22, flat in the xy plane; the
grip hangs toward -z.
"""
import math

import os

import numpy as np

import mesh_lib as ml
import ranged_meshes as rm
import wave1_models as w
from wave10_models import card, tube, glow_all

# ---------------------------------------------------------------- 1 measure
Y_BUTT, Y_MUZZLE = -26.0, 36.0                        # the stock's ends (62 cm)
Y_PROD = 22.0                                         # where the prod crosses the stock (a third from the muzzle... the card's prod sits well forward)
SPAN = 24.0                                           # half span of the prod (the card: 150 px = 50 cm across)
SWEEP = 12.0                                          # the limbs' tips are swept back this far (the jagged wing-like shape of the card)
LIMB_W0, LIMB_W1 = 5.0, 1.4                           # half width of a limb in plane, at the root and at the tip
LIMB_T = 0.55                                         # half thickness (limbs are blade-flat)
STOCK_W0, STOCK_W1 = 2.6, 1.5                         # half width of the stock at the butt and the muzzle
STOCK_T = 2.3                                         # half depth of the stock
N_TEETH = 3                                           # jagged teeth along the rear edge of each limb
FLEX = 3.0                                            # how far each limb tip comes back at full draw
Y_NOCK = 2.0                                          # where the cocked string is held (at the trigger)


def limb_path(sgn):
    pts = []
    for i in range(0, 13):
        t = i / 12
        x = sgn * (1.5 + (SPAN - 1.5) * t)
        y = Y_PROD - SWEEP * t ** 1.6 + 1.4 * math.sin(math.pi * t) * 0.6
        pts.append((x, y, 0.0))
    return pts


def build():
    name = "needler"
    m = ml.Mesh(name)
    # ---- 1 the stock: a long tapered loft of a rounded rectangle
    sk = w.add(m, "stock", "iron", base=(74, 30, 126), smooth=40, uv_mode="loft")
    ys = [Y_BUTT, Y_BUTT + 5, -8, 4, 16, 28, Y_MUZZLE]
    wd = [STOCK_W0, STOCK_W0 * 1.05, 2.2, 2.0, 1.9, STOCK_W1 * 1.1, STOCK_W1]
    ml.loft(sk, "y", (0, 0, 0), [(y, wd[i], STOCK_T * (1.0 if i < 5 else 0.85)) for i, y in enumerate(ys)], ml.rounded_rect(2.0, 2.0, 0.55, 2))
    # ---- 2 the rail: a silver strip along the top of the stock with the bolt's groove between two ridges
    rl = w.add(m, "rail", "silver", base=(178, 182, 204), wear=0.7, smooth=45)
    ml.fuller(rl, (0, Y_BUTT + 8, STOCK_T + 0.1), (0, Y_MUZZLE - 2.0, STOCK_T * 0.85 + 0.1), (0, 0, 1), 0.9, 0.28, 0.2, taper=3.0)
    # ---- 3 the prod: two swept-back blade limbs of purple metal; 4 the jagged teeth on their rear edge
    for sgn, nm in ((-1, "limb_l"), (1, "limb_r")):
        lb = w.add(m, nm, "obsidian", base=(88, 30, 156), smooth=45, uv_mode="loft")
        ml.sweep(lb, limb_path(sgn), ml.bevelled(2.0, 1.0, 0.5), scales=lambda t: (LIMB_W0 + (LIMB_W1 - LIMB_W0) * t ** 0.8 if t < 0.985 else 0.0, LIMB_T * (1 - 0.35 * t) if t < 0.985 else 0.0), up=(0, 0, 1))
    tt = w.add(m, "limb_teeth", "obsidian", base=(120, 44, 196), smooth=20)
    for sgn in (-1, 1):
        base_pts = limb_path(sgn)
        for k in range(N_TEETH):
            i = 3 + 3 * k
            px, py, _ = base_pts[i]
            wi = LIMB_W0 + (LIMB_W1 - LIMB_W0) * (i / 12) ** 0.8
            ln = 4.2 - 0.8 * k
            ml.sweep(tt, [(px, py - wi * 0.6, 0.0), (px + sgn * 0.5, py - wi * 0.6 - ln * 0.5, 0.0), (px + sgn * 1.5, py - wi * 0.6 - ln, 0.0)], ml.bevelled(2.0, 1.0, 0.5),
                     scales=lambda t, ln=ln: (1.5 * (1 - t) + 0.05 if t < 0.97 else 0.0, 0.4 * (1 - 0.6 * t) if t < 0.97 else 0.0), up=(0, 0, 1))
    # ---- 5 the silvery arms: a silver tube along the front edge of every limb
    ar = w.add(m, "limb_arms", "silver", base=(176, 180, 198), wear=0.6, smooth=60)
    for sgn in (-1, 1):
        pts = [(x, y + LIMB_W0 * 0.55 * (1 - i / 12) + 0.3, 0.0) for i, (x, y, _) in enumerate(limb_path(sgn))]
        ml.sweep(ar, pts, ml.circle(1.0, 6), scales=lambda t: (0.5 * (1 - 0.4 * t),) * 2)
    # ---- 6 the prod mount, where the limbs meet the stock
    pmn = w.add(m, "prod_mount", "silver", base=(168, 172, 192), wear=0.5, smooth=45)
    ml.loft(pmn, "x", (0, Y_PROD - 0.5, 0), [-3.4, -2.6, 2.6, 3.4], ml.octagon(5.6, 4.0, 1.0))
    # ---- 7 the string
    st = w.add(m, "string", "cloth", base=(206, 202, 214), wear=0.1, smooth=70)
    tipL, tipR = limb_path(-1)[-1], limb_path(1)[-1]
    A = (tipL[0], tipL[1] - 0.2, 0.9)                                                  # the string's anchors at rest: the limb tips
    B = (tipR[0], tipR[1] - 0.2, 0.9)
    pts = [(A[0] + (B[0] - A[0]) * i / 10, A[1], 0.9) for i in range(11)]            # slack at rest: a straight line; ranged_meshes bends it into the V when loading
    ml.sweep(st, pts, ml.circle(1.0, 4), scales=lambda t: (0.14, 0.14))
    # ---- 8 the grip and the trigger guard
    gp = w.add(m, "grip", "silver", base=(158, 162, 184), wear=0.6, smooth=50)
    ml.loft(gp, "-z", (0, -4.0, -STOCK_T * 0.7), [0, 3, 8, 13], ml.rounded_rect(3.2, 3.8, 0.8, 2), caps=(True, True))
    tg = w.add(m, "trigger_guard", "silver", base=(188, 192, 210), wear=0.5, smooth=60)
    ml.sweep(tg, [(0, 1.6, -STOCK_T * 0.9), (0, 3.4, -STOCK_T - 1.8), (0, 3.0, -STOCK_T - 3.4), (0, -0.4, -STOCK_T - 3.4), (0, -2.0, -STOCK_T - 1.2)], ml.circle(1.0, 5), scales=lambda t: (0.28, 0.28))
    ml.box(tg, (-0.15, 0.2, -STOCK_T - 1.8), (0.15, 0.7, -STOCK_T * 0.9), 0.0)             # the trigger
    # ---- 9 the stirrup ring at the muzzle, 10 the butt plate
    sr = w.add(m, "stirrup", "silver", base=(188, 192, 210), wear=0.5, smooth=55)
    ml.sweep(sr, [(1.8 * math.sin(a), Y_MUZZLE + 1.2 + 2.6 * math.cos(a) * 0.6 + 1.0, 0.0) for a in np.linspace(0, 2 * math.pi, 15)], ml.circle(1.0, 5), scales=lambda t: (0.42, 0.42), caps=(False, False))
    bp = w.add(m, "butt_plate", "silver", base=(172, 176, 196), wear=0.6, smooth=45)
    ml.loft(bp, "y", (0, 0, 0), [(Y_BUTT - 0.5, 0.0, 0.0), (Y_BUTT - 0.2, STOCK_W0, STOCK_T), (Y_BUTT + 0.9, STOCK_W0 * 1.05, STOCK_T * 1.05)], ml.rounded_rect(2.0, 2.0, 0.6, 2))
    # ---- 11 the engravings: slender raised lines along both sides of the stock and across the limbs
    en = w.add(m, "engravings", "silver", base=(214, 190, 255), wear=0.2, glow=True, smooth=30)
    for sgn in (-1, 1):
        ml.bar(en, (sgn * (STOCK_W0 * 0.98), -20, 0.0), (sgn * (2.2 * 0.98), -4, 0.0), 0.28, 0.12, up=(sgn, 0, 0), taper=3.0)
        ml.bar(en, (sgn * (2.0 * 0.98), 0, 0.0), (sgn * (STOCK_W1 * 1.02), 30, 0.0), 0.24, 0.12, up=(sgn, 0, 0), taper=3.0)
    for sgn in (-1, 1):
        for k in range(3):
            x = sgn * (6 + k * 5.5)
            ml.bar(en, (x, Y_PROD - 3.2 - k * 0.9, LIMB_T * 0.9), (x + sgn * 2.2, Y_PROD - 3.8 - k * 1.6, LIMB_T * 0.7), 0.22, 0.1, up=(0, 0, 1), taper=0.6)
    # ---- 12 the crystal that powers it, set in the stock above the grip
    cr = w.add(m, "core_gem", "crystal", base=(170, 90, 255), glow=True, smooth=0)
    ml.gem(cr, (0, 8.0, STOCK_T + 0.25), 1.1, long=1.5, sides=6, axis="y")
    if True:                                                                              # ---- 13 the needle on the rail (ranged_meshes draws it in _charged only)
        nd = w.add(m, "bolt_needle", "silver", base=(206, 208, 222), wear=0.3, smooth=45)
        ml.lathe(nd, "y", (0, 0, STOCK_T + 0.55), [(Y_NOCK + 0.6, 0.18), (Y_NOCK + 1.4, 0.24), (Y_NOCK + 34.0, 0.24), (Y_NOCK + 38.0, 0.0)], sides=6)
        tp = w.add(m, "bolt_tip", "crystal", base=(190, 120, 255), glow=True, smooth=0)
        ml.lathe(tp, "y", (0, 0, STOCK_T + 0.55), [(Y_NOCK + 30.0, 0.0), (Y_NOCK + 30.6, 0.34), (Y_NOCK + 37.0, 0.3), (Y_NOCK + 39.0, 0.0)], sides=6)

    atlas = ml.Atlas(512)
    atlas.layout(m)
    atlas.paint(m)
    _paint(m, atlas)
    m.place_on_sprite(os.path.join(w.ICON_DIR, "needler.png"), real_m=0.62)
    m.set_glow("FFFFFF", 0.16, 0.4, 1.0)
    rig = rm.Rig(tip_a=A, tip_b=B, draw_dir=(0, -1, 0), max_draw=A[1] - Y_NOCK - FLEX, flex=FLEX, grip=(0.0, Y_PROD, 0.0), limb_axis=(1, 0, 0), limb_span=SPAN)
    paths = rm.emit_states(m, rig, "crossbow", "needler", atlas=atlas, normals="vertex")        # needler.txt (rest), _pull1.._pull3, _charged
    atlas.save("needler")
    for suffix, path in sorted(paths.items()):
        print("needler" + suffix, os.path.getsize(path) // 1024, "KB")
    prev = os.environ.get("W1_PREVIEW")
    if prev:
        ref = card("p3/Relics of the world part 3/d1bdb6a18a6ad9c4c3355329169b9bff.jpg", (0, 20, 240, 240), "needler")
        import shutil
        for suffix in ("_charged", "_pull2"):                                                   # the preview finds the skin by the mesh name: copy the state to a scratch id
            shutil.copy(paths[suffix], os.path.join(w.MESH_DIR, "needler.txt.state" + suffix))
        ml.preview(m, ref, os.path.join(prev, "needler.png"), atlas=atlas, normals="vertex")
        os.remove(os.path.join(w.MESH_DIR, "needler.txt.state_charged"))
        os.remove(os.path.join(w.MESH_DIR, "needler.txt.state_pull2"))


def _paint(m, atlas):
    for nm in ("limb_l", "limb_r"):
        def limb(c):
            sh = c.img.shape[:2]
            n = w.noise2(sh, 8, 6)
            c.img[..., :3] = np.clip(c.img[..., :3] * (0.85 + 0.35 * n[..., None]), 0, 255)
            edge = np.clip((np.abs(c.S - 0.5) - 0.36) * 8, 0, 1)
            w.set_glow(c, (170, 90, 255), 0.4, mask=edge * 0.7, core=0.2)
        w.paint_part(atlas, m.part(nm), limb)
    glow_all(atlas, m.part("engravings"), (190, 120, 255), 0.7, 0.3)
    glow_all(atlas, m.part("core_gem"), (190, 120, 255), 1.0, 0.5)
    glow_all(atlas, m.part("bolt_tip"), (200, 140, 255), 1.0, 0.5)

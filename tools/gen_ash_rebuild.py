"""Ash Magic rebuild (1.93.0): the temporary armour pieces, the summon models (ash soldier, ash trap) and the VFX textures.

    python3 -B tools/gen_ash_rebuild.py

Armour (client/aura/AshAura, one geo model per piece), the Ash Shroud: burnt charcoal plate cracked and glowing with embers, heaps of pale ash, and the bare
branches of an ash tree:
  ash_helm      a hood of drifting ash with two ember slits and a crown of dead branches like antlers
  ash_chest     a cracked charcoal plate over a ribcage of bare branches with an ember core, heaped ash on the collar
  ash_belt      a belt of charred lumps with hanging rags of ash
  ash_back      the ash tree on the back: five branches a side spreading like wings, with twigs and glowing embers (clip idle: sway)
  ash_pauld_r / _l   a charred plate under a heap of ash with splintered branch spikes
  ash_vamb_r / _l    cracked charcoal vambraces with ash cuffs and ember knuckles
  ash_greave_r / _l  charred boots with ash drifts at the soles
Summons (client/prop/AshPropPainter): ash_soldier (a figure of charcoal and ash with ember eyes and a cracked, glowing core; idle, strike, crumble),
ash_trap (a mound of ash with splintered branches under it that spring up; idle, spring).
Particles: ash_flake, ash_cloud, ash_ember, ash_sigil, ash_tendril, ash_ring.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402

KEY = "ash"


def mats():
    return {
        "char": L.sheet_ash(11, [(24, 22, 24), (44, 42, 46), (68, 66, 72), (96, 94, 100), (128, 126, 132)], ember=(255, 120, 50), crack_amt=1.0),
        "ash": L.sheet_ash(23, [(110, 108, 112), (146, 144, 148), (178, 176, 180), (206, 204, 208), (232, 230, 234)], ember=(255, 140, 70), crack_amt=0.3),
        "bark": L.sheet_ash(37, [(30, 24, 22), (52, 42, 38), (76, 62, 54), (104, 88, 76), (136, 118, 104)], ember=(255, 130, 60), crack_amt=0.5),
        "ember": L.sheet_solid((255, 150, 70), glow=0.95, glow_col=(255, 140, 60), noise=0.1, seed=3),
        "core": L.sheet_solid((255, 214, 150), glow=1.0, glow_col=(255, 190, 110), noise=0.05, seed=4),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def branch(P, bone, pts, thick, mat="bark", share=None, twigs=0, side=1):
    P.chain(bone, pts, thick, mat, taper=0.6, share=share)
    for k in range(twigs):                                           # short twigs off the branch
        i = min(len(pts) - 2, 1 + k)
        a, b = pts[i], pts[i + 1]
        mid = ((a[0] + b[0]) / 2.0, (a[1] + b[1]) / 2.0, (a[2] + b[2]) / 2.0)
        P.bar(bone, mid, (mid[0] + side * 2.6, mid[1] + 2.4, mid[2] + 0.4), 1, mat, share=(share + "tw%d" % k) if share else None)


def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("hood", "root", (0, 24, 0))
    for i in range(5):
        a = 2 * math.pi * i / 5
        P.puff("hood", (math.cos(a) * 4.1, 32.4, math.sin(a) * 4.1), 2.7, "ash", share="hc")
    for s in (-1, 1):
        P.puff("hood", (s * 4.8, 28.0, 0.4), 2.4, "ash", share="hs")
        P.puff("hood", (s * 4.5, 25.4, 1.6), 1.9, "char", share="hd")
    for k in range(3):
        P.puff("hood", (0, 30.5 - 2.6 * k, 4.6 + 0.4 * k), 2.6, "ash" if k % 2 == 0 else "char", share="hb%d" % k)
    P.cube("hood", (-4.7, 30.2, -4.8), (9.4, 1.8, 1.2), "char", inflate=0.1, share="brow")
    P.cube("hood", (-3.1, 30.4, -5.4), (2, 1, 1), "ember", share="e1")
    P.cube("hood", (1.1, 30.4, -5.4), (2, 1, 1), "ember", share="e1")
    for s in (-1, 1):                                                # antlers of dead branches
        branch(P, "hood", [(s * 2.4, 33.4, 0), (s * 4.0, 36.2, -0.4), (s * 5.4, 39.4, -0.6), (s * 5.0, 42.4, 0)], 2, share="ant%d" % (s > 0), twigs=2, side=s)
    P.anim.clip("idle", 3.0, True)
    P.anim.rot("idle", "hood", L.sine3(3.0, (0.8, 0, 0), 0.0))
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    for i in range(6):
        a = 2 * math.pi * i / 6
        P.puff("torso", (math.cos(a) * 4.1, 25.0, math.sin(a) * 3.1), 2.4, "ash", share="col")
    P.cube("torso", (-4.5, 14.8, -2.9), (9, 9, 1), "char", inflate=0.25, share="plate")
    P.cube("torso", (-4.5, 14.4, 2.0), (9, 9.4, 1), "char", inflate=0.25, share="back")
    P.cube("torso", (-5.0, 22.0, -2.7), (10, 2, 5.4), "char", inflate=0.1, share="yoke")
    for k in range(4):                                               # the ribs: bare branches curving across the chest
        y = 21.0 - 1.9 * k
        for s in (-1, 1):
            P.chain("torso", [(0.8 * s, y, -3.7), (3.0 * s, y + 0.4, -3.5), (4.6 * s, y - 0.4, -2.2)], 1, "ash", share="rib%d%d" % (k, s > 0))
    P.puff("torso", (0, 18.4, -3.9), 1.5, "core", share="heart")
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.bone("rags", "torso", (0, 12.5, 0))
    for i in range(7):
        a = 2 * math.pi * i / 7
        P.puff("torso", (math.cos(a) * 4.5, 12.4, math.sin(a) * 2.9), 2.0, "char", share="b")
    P.cube("torso", (-4.4, 11.6, -2.8), (8.8, 1.8, 5.6), "ash", inflate=0.2, share="sash")
    for x, ln in ((-3.2, 6), (-1.0, 8), (1.2, 7), (3.4, 5)):          # rags of ash hanging from the belt
        P.cube("rags", (x - 0.8, 11.0 - ln, -3.2), (1.6, ln, 1), "ash", share="rg%d" % ln)
    P.anim.clip("idle", 2.4, True)
    P.anim.rot("idle", "rags", L.sine3(2.4, (5, 0, 2), 0.0))
    P.save()
    return P


def back():
    P = piece("back", 64, 128, 4)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("tree", "root", (0, 18, 3))
    for side, tag in ((-1, "r"), (1, "l")):
        P.bone("br_" + tag, "tree", (side * 2, 20, 3.4))
        spec = [(6, 24, 14), (9, 20, 12), (11.5, 15, 10), (10, 9, 8), (7, 4, 7)]            # five branches fanning up and out
        for k, (ex, ey, ez) in enumerate(spec):
            base = (side * 1.5, 18 + k * 1.2, 3.4)
            mid = (side * (ex * 0.5), ey * 0.6 + 8, 5.0 + 0.4 * k)
            tip = (side * ex * 1.9, ey + 6 - k * 0.5, 6.0 + 0.5 * k)
            branch(P, "br_" + tag, [base, mid, tip], 2, share="b%d" % k, twigs=2, side=side)
            P.puff("br_" + tag, tip, 1.0, "ember", share="te")
        P.puff("br_" + tag, (side * 14, 28.5, 6), 1.0, "ember", share="te")
    P.puff("tree", (0, 19, 3.4), 3.0, "bark", share="trunk")
    P.cube("tree", (-1.5, 10, 2.4), (3, 10, 2), "bark", share="trunk2")
    a = P.anim
    a.clip("idle", 4.0, True)
    a.rot("idle", "br_r", L.sine3(4.0, (2, 3, -3), 0.0))
    a.rot("idle", "br_l", L.sine3(4.0, (2, -3, 3), 0.3))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    sx = side * 6.2
    P.cube("arm", (sx - 3.4, 22.2, -3.0), (6.8, 2, 6), "char", inflate=0.3, share="pl")
    P.puff("arm", (sx, 24.8, 0), 3.4, "ash", share="heap")
    P.puff("arm", (sx + side * 1.6, 24.0, 1.2), 2.2, "char", share="h2")
    branch(P, "arm", [(sx + side * 0.5, 26.2, 0), (sx + side * 2.2, 29.0, -0.4), (sx + side * 3.0, 31.6, -0.6)], 2, share="sp1", twigs=1, side=side)
    branch(P, "arm", [(sx - side * 1.0, 26.0, -1.5), (sx - side * 0.4, 28.6, -2.8), (sx + side * 0.2, 30.4, -3.8)], 2, share="sp2")
    P.puff("arm", (sx, 24.8, -3.0), 0.9, "ember", share="em")
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 14.8, -2.5), (5, 5, 5), "char", inflate=0.5, share="cuff")
    for i in range(5):
        a = 2 * math.pi * i / 5
        P.puff("arm", (cx + math.cos(a) * 2.9, 19.6, math.sin(a) * 2.9), 1.6, "ash", share="ac")
    for k in range(3):
        P.cube("arm", (cx - 2.4 + 2.2 * k, 11.0, 2.4), (1.2, 4, 1), "ash", share="dr%d" % k)
    P.cube("arm", (cx - 2.3, 10.8, -2.3), (4.6, 3.4, 4.6), "char", inflate=0.4, share="fist")
    P.puff("arm", (cx, 11.4, -2.8), 0.9, "ember", share="kn")
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 2.2, -2.5), (4.8, 7, 5), "char", inflate=0.4, share="boot")
    P.puff("leg", (cx, 9.6, -2.0), 2.2, "ash", share="knee")
    P.puff("leg", (cx, 1.6, -1.2), 3.0, "ash", share="drift")
    P.puff("leg", (cx + side * 1.8, 1.0, 1.6), 2.2, "char", share="d2")
    P.puff("leg", (cx, 5.6, -3.0), 0.9, "ember", share="em")
    P.save()
    return P


def soldier():
    P = piece("soldier", 128, 128, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("body", "root", (0, 14, 0))
    P.bone("head", "body", (0, 26, 0))
    P.bone("arm_r", "body", (-6, 24, 0))
    P.bone("arm_l", "body", (6, 24, 0))
    P.bone("leg_r", "root", (-2.2, 12, 0))
    P.bone("leg_l", "root", (2.2, 12, 0))
    P.cube("body", (-5, 12, -3), (10, 14, 6), "char", share="torso")
    P.cube("body", (-5.6, 22, -3.6), (11.2, 4, 7.2), "ash", share="shoulder")
    P.cube("body", (-1.6, 16, -3.8), (3.2, 5, 1), "core", share="crack")
    P.cube("body", (-3.8, 14.5, -3.7), (1, 6, 1), "ember", rot=(0, 0, 14), share="cr2")
    P.cube("body", (2.8, 17, -3.7), (1, 5, 1), "ember", rot=(0, 0, -12), share="cr3")
    P.puff("head", (0, 30.5, 0), 4.2, "ash", share="hd")
    P.cube("head", (-3, 30.6, -4.7), (2, 1, 1), "ember", share="eye")
    P.cube("head", (1, 30.6, -4.7), (2, 1, 1), "ember", share="eye")
    for s in (-1, 1):
        branch(P, "head", [(s * 2.0, 33.4, 0), (s * 3.4, 36.0, 0), (s * 4.0, 38.6, 0)], 2, share="an%d" % (s > 0))
    for s, bn in ((-1, "arm_r"), (1, "arm_l")):
        P.puff(bn, (s * 7.2, 24, 0), 3.0, "ash", share="sh")
        P.cube(bn, (s * 7.2 - 2, 10, -2), (4, 13, 4), "char", share="arm")
        P.puff(bn, (s * 7.2, 9.4, 0), 3.0, "ash", share="fist")
    for s, bn in ((-1, "leg_r"), (1, "leg_l")):
        P.cube(bn, (s * 2.2 - 2.2, 0, -2.4), (4.4, 12, 4.8), "char", share="leg")
        P.puff(bn, (s * 2.2, 1.2, -0.6), 2.8, "ash", share="foot")
    a = P.anim
    a.clip("idle", 2.8, True)
    a.pos("idle", "body", L.sine(2.8, 0.5, 0.0))
    a.rot("idle", "arm_r", L.sine3(2.8, (5, 0, 3), 0.0))
    a.rot("idle", "arm_l", L.sine3(2.8, (5, 0, -3), 0.5))
    a.rot("idle", "head", L.sine3(2.8, (1.5, 3, 0), 0.3))
    a.clip("strike", 0.8, False)
    a.rot("strike", "arm_r", {0.0: (0, 0, 0), 0.25: (-120, 0, 8), 0.45: (35, 0, 0), 0.8: (0, 0, 0)})
    a.rot("strike", "body", {0.0: (0, 0, 0), 0.25: (-10, 25, 0), 0.45: (14, -20, 0), 0.8: (0, 0, 0)})
    a.clip("crumble", 1.0, False)
    a.pos("crumble", "root", {0.0: (0, 0, 0), 1.0: (0, -20, 0)})
    a.scale("crumble", "root", {0.0: (1, 1, 1), 1.0: (1.2, 0.4, 1.2)})
    P.save()
    return P


def trap():
    P = piece("trap", 128, 128, 30)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("spikes", "root", (0, 0, 0))
    P.puff("root", (0, 2, 0), 7.0, "ash", share="m", squash=0.45)
    for k in range(6):
        a = math.pi / 3 * k
        P.puff("root", (math.cos(a) * 8.5, 1.2, math.sin(a) * 8.5), 2.6, "char", share="d%d" % (k % 2))
    P.cube("root", (-2, 4, -2), (4, 1, 4), "core", share="sig")
    for k in range(7):                                               # splintered branches under the mound that spring up
        a = 2 * math.pi * k / 7
        r = 3.0 + 3.5 * (k % 2)
        base = (math.cos(a) * r, 1, math.sin(a) * r)
        tip = (math.cos(a) * (r + 3.2), 16 + 5 * (k % 3), math.sin(a) * (r + 3.2))
        branch(P, "spikes", [base, ((base[0] + tip[0]) / 2, 8 + 1.5 * (k % 2), (base[2] + tip[2]) / 2), tip], 2, share="sp%d" % k, twigs=1, side=1)
        P.puff("spikes", tip, 0.9, "ember", share="te")
    a = P.anim
    a.clip("idle", 3.0, True)
    a.scale("idle", "spikes", {0.0: (1, 0.25, 1), 1.5: (1, 0.3, 1), 3.0: (1, 0.25, 1)})
    a.clip("spring", 0.5, False)
    a.scale("spring", "spikes", {0.0: (1, 0.25, 1), 0.15: (1.15, 1.25, 1.15), 0.5: (1, 1, 1)})
    P.save()
    return P


def textures():
    L.particle_cloud("ash_cloud", 601, 128, lobes=8, detail=0.6)

    def flake(d, S):                                                 # an irregular curled flake of burnt paper
        c = S / 2.0
        pts = []
        import random
        rnd = random.Random(7)
        for k in range(11):
            a = 2 * math.pi * k / 11
            r = c * (0.55 + 0.4 * rnd.random())
            pts.append((c + math.cos(a) * r, c + math.sin(a) * r * 0.7))
        d.polygon(pts, fill=255)
        d.line((c - c * 0.4, c, c + c * 0.4, c - c * 0.1), fill=120, width=max(1, S // 50))
    L.particle_shape("ash_flake", flake, 64, blur=0.5, glow=0.1)
    L.particle_soft("ash_ember", 64, 2.6)

    def sigil(d, S):                                                 # a sigil of an ash tree: a trunk, five branches a side, a ring and a root ring
        c = S / 2.0
        w = max(2, S // 70)
        d.ellipse((c - c * 0.95, c - c * 0.95, c + c * 0.95, c + c * 0.95), outline=255, width=w)
        d.ellipse((c - c * 0.55, c - c * 0.55, c + c * 0.55, c + c * 0.55), outline=255, width=w)
        d.line((c, c + c * 0.5, c, c - c * 0.35), fill=255, width=w * 2)
        for k in range(5):
            y = c + c * 0.35 - k * c * 0.17
            ln = c * (0.12 + 0.14 * math.sin(k / 4.0 * math.pi) + 0.05 * k)
            for s in (-1, 1):
                d.line((c, y, c + s * ln, y - ln * 0.7), fill=255, width=w)
                d.line((c + s * ln * 0.6, y - ln * 0.42, c + s * ln * 0.95, y - ln * 0.45 - c * 0.08), fill=255, width=max(1, w - 1))
        for k in range(12):
            a = math.pi / 6 * k
            d.line((c + math.cos(a) * c * 0.95, c + math.sin(a) * c * 0.95, c + math.cos(a) * c * 0.85, c + math.sin(a) * c * 0.85), fill=255, width=w)
    L.particle_shape("ash_sigil", sigil, 256, blur=0.8, glow=0.15)

    def tendril(d, S):                                               # a jagged splintered branch reaching up
        c = S / 2.0
        pts = [(c, S * 0.97), (c - S * 0.06, S * 0.7), (c + S * 0.08, S * 0.45), (c - S * 0.05, S * 0.22), (c + S * 0.02, S * 0.03)]
        for i in range(len(pts) - 1):
            d.line((pts[i], pts[i + 1]), fill=255, width=max(2, int(S * 0.07 * (1 - i / 4.0) + 2)))
        for (x, y, dx) in ((pts[1][0], pts[1][1], 1), (pts[2][0], pts[2][1], -1), (pts[3][0], pts[3][1], 1)):
            d.line((x, y, x + dx * S * 0.2, y - S * 0.1), fill=255, width=max(2, S // 40))
    L.particle_shape("ash_tendril", tendril, 128, blur=0.7, glow=0.12)
    L.particle_ring("ash_ring", 256, 0.84, 0.97, 0.06, ticks=0, seed=604, wobble=0.06)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), soldier(), trap()]
    textures()
    print("models:", " ".join(p.m.id for p in out))

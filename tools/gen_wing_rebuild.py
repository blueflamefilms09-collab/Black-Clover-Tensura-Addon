"""Wing Magic rebuild (1.93.0): the temporary armour pieces, the summon models (feather hawk, feather blade) and the VFX textures.

    python3 -B tools/gen_wing_rebuild.py

Armour (client/aura/WingAura, one geo model per piece):
  wing_helm      a circlet with two feathered fins at the temples and a crest plume
  wing_chest     a breastplate of overlapping feather scales with a golden winged emblem
  wing_belt      a gold belt with tassets of hanging feathers
  wing_back      the WINGS: two articulated wings (an inner arm bone and an outer bone, ten flight feathers and coverts each, clip idle: a beat with the outer
                 half lagging behind the inner) and a halo of gold over the head
  wing_pauld_r / _l   three stacked feather plates edged in gold
  wing_vamb_r / _l    a vambrace with three feathers sweeping back along the forearm, a gold gauntlet
  wing_greave_r / _l  boots with a small wing at the ankle (the winged boots)
Summons (client/prop/WingPropPainter): wing_hawk (idle: wing beat, dive: folded wings), wing_blade (a feather blade, spin).
Particles: wing_feather, wing_wing, wing_streak, wing_halo, wing_glint, wing_down.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402

KEY = "wing"


def mats():
    return {
        "feather": L.sheet_feather(11, [(176, 170, 206), (206, 202, 228), (230, 228, 244), (246, 244, 252), (255, 255, 255)], shaft=(255, 238, 190), glow_col=(255, 236, 170)),
        "silver": L.sheet_feather(23, [(120, 126, 170), (156, 162, 204), (190, 196, 228), (222, 226, 246), (246, 248, 255)], shaft=(255, 240, 200), glow_col=(255, 236, 170)),
        "gold": L.sheet_solid((236, 196, 104), glow=0.35, glow_col=(255, 220, 130), noise=0.12, seed=3),
        "core": L.sheet_solid((255, 244, 200), glow=1.0, glow_col=(255, 236, 170), noise=0.04, seed=4),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def feather(P, bone, base, d, length, width, mat, tip="gold"):
    """One flat feather: a blade from base along d = (dx, dy) in the wing plane (z fixed), a gold-tipped end. Thickness 1."""
    dx, dy = d
    n = math.hypot(dx, dy)
    dx, dy = dx / n, dy / n
    z = base[2]
    end = (base[0] + dx * length, base[1] + dy * length, z)
    mid = ((base[0] + end[0]) / 2.0, (base[1] + end[1]) / 2.0, z)
    L_ = max(2, int(round(length)))
    o = (mid[0] - width / 2.0, mid[1] - L_ / 2.0, z - 0.5)
    P.cube(bone, o, (width, L_, 1), mat, rot=L.orient_y(dx, dy, 0), share="f%d_%d_%s" % (width, L_, mat))
    if tip:
        tl = 2
        tm = (end[0] - dx * 0.5, end[1] - dy * 0.5, z)
        P.cube(bone, (tm[0] - width / 2.0 + 0.3, tm[1] - tl / 2.0, z - 0.5), (max(1, width - 1), tl, 1), tip, rot=L.orient_y(dx, dy, 0), share="t%d" % width)


def wing(P, side, nm_in, nm_out):
    """One wing in the file frame, side -1 = the character's right (x negative). Shoulder joint, elbow and wrist in the xy plane, the wing a little behind the back (z +4)."""
    z = 4.2
    sh = (side * 3.0, 22.0, z)
    el = (side * 13.0, 30.0, z + 0.4)
    wr = (side * 24.0, 27.0, z + 0.8)
    P.bar(nm_in, sh, el, 2, "silver", share="arm1")
    P.bar(nm_out, el, wr, 2, "silver", share="arm2")
    P.puff(nm_in, el, 1.6, "gold", share="elbow")
    for k in range(10):
        t = k / 9.0
        if t < 0.4:
            bone = nm_in
            tt = t / 0.4
            base = (sh[0] + (el[0] - sh[0]) * tt, sh[1] + (el[1] - sh[1]) * tt, z - 0.2)
        else:
            bone = nm_out
            tt = (t - 0.4) / 0.6
            base = (el[0] + (wr[0] - el[0]) * tt, el[1] + (wr[1] - el[1]) * tt, z - 0.2)
        phi = math.radians(8 + 72 * t)
        d = (side * math.sin(phi), -math.cos(phi))
        feather(P, bone, base, d, 11 + 7 * t if t > 0.3 else 9 + 3 * t, 2 if t < 0.8 else 2, "feather" if k % 2 == 0 else "silver")
    for k in range(5):                                               # coverts: short plates along the arm
        t = k / 4.0
        base = (sh[0] + (wr[0] - sh[0]) * t, sh[1] + (el[1] - sh[1]) * min(1.0, t * 2) - (t > 0.5) * (t - 0.5) * 6, z + 0.9)
        bone = nm_in if t < 0.5 else nm_out
        P.cube(bone, (base[0] - 2, base[1] - 2.5, base[2]), (4, 5, 1), "feather", share="cov")


def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("head", "root", (0, 24, 0))
    P.cube("head", (-4.6, 30.0, -4.7), (9.2, 1.6, 9.4), "gold", inflate=0.1, share="band")
    P.cube("head", (-1, 31.2, -5.0), (2, 2, 1), "core", share="gem")
    for s in (-1, 1):
        for k, (dy, ln) in enumerate(((0.3, 8), (-0.35, 6), (-0.9, 5))):                     # the temple fins: three feathers each, sweeping back and up
            feather(P, "head", (s * 4.8, 30.6 - 1.3 * k, 0.5 + k * 0.3), (s * 1.0, 0.45 - 0.5 * k + 0.5), ln, 2, "feather")
    for k in range(3):                                               # a crest plume
        feather(P, "head", (0, 32.2, -1 + 1.8 * k), (0.0, 1.0), 6 - k, 2, "silver")
    P.puff("head", (0, 30.8, 4.0), 2.0, "feather", share="nape")
    P.anim.clip("idle", 2.4, True)
    P.anim.rot("idle", "head", L.sine3(2.4, (0.8, 0, 0), 0.0))
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.cube("torso", (-4.5, 15.2, -2.9), (9, 8.5, 1), "silver", inflate=0.2, share="plate")
    P.cube("torso", (-5.0, 22.2, -2.7), (10, 2, 5.4), "gold", inflate=0.1, share="yoke")
    P.cube("torso", (-4.5, 14.6, 2.0), (9, 9, 1), "silver", inflate=0.2, share="back")
    for row in range(3):                                             # overlapping feather scales on the breast
        for col in range(4):
            x = -3.6 + col * 2.4 + (row % 2) * 1.2
            P.cube("torso", (x - 1.1, 21.2 - row * 2.6, -3.7), (2.4, 3, 1), "feather", rot=(10, 0, 0), share="sc")
    P.cube("torso", (-1.3, 18.0, -4.5), (2.6, 2.6, 1), "core", share="heart")
    for s in (-1, 1):                                                # the winged emblem
        feather(P, "torso", (s * 1.6, 18.6, -4.6), (s * 1.0, 0.35), 5, 1, "gold", tip=None)
        feather(P, "torso", (s * 1.6, 17.6, -4.6), (s * 1.0, -0.1), 4, 1, "gold", tip=None)
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.bone("tassets", "torso", (0, 13, 0))
    P.cube("torso", (-4.5, 11.4, -2.8), (9, 2.2, 5.6), "gold", inflate=0.25, share="band")
    P.cube("torso", (-1.2, 11.4, -3.6), (2.4, 2.4, 1), "core", share="buckle")
    for x in (-3.4, -1.1, 1.1, 3.4):
        feather(P, "tassets", (x, 11.0, -3.2), (0.0, -1.0), 7 - abs(x) * 0.5, 2, "feather")
    for s in (-1, 1):
        feather(P, "tassets", (s * 4.8, 11.0, 0), (s * 0.2, -1.0), 7, 2, "silver")
    P.anim.clip("idle", 2.0, True)
    P.anim.rot("idle", "tassets", L.sine3(2.0, (4, 0, 0), 0.0))
    P.save()
    return P


def back():
    P = piece("back", 128, 128, 4)
    P.bone("root", pivot=(0, 24, 0))
    for side, tag in ((-1, "r"), (1, "l")):
        P.bone("wing_" + tag, "root", (side * 3.0, 22.0, 4.2))
        P.bone("outer_" + tag, "wing_" + tag, (side * 13.0, 30.0, 4.6))
        wing(P, side, "wing_" + tag, "outer_" + tag)
    P.bone("halo", "root", (0, 36, 0))
    P.ring("halo", (0, 36.6, 1.0), 6.2, 1.4, 10, "gold", axis="y", depth=1.6, share="halo")
    a = P.anim
    a.clip("idle", 1.8, True)
    for side, tag in ((-1, "r"), (1, "l")):
        a.rot("idle", "wing_" + tag, L.loop(1.8, lambda p, s=side: (0, s * 10 * math.sin(p * 2 * math.pi), s * 16 * math.sin(p * 2 * math.pi))))
        a.rot("idle", "outer_" + tag, L.loop(1.8, lambda p, s=side: (0, s * 8 * math.sin((p - 0.12) * 2 * math.pi), s * 22 * math.sin((p - 0.12) * 2 * math.pi))))
    a.pos("idle", "halo", L.sine(1.8, 0.5, 0.0))
    a.rot("idle", "halo", L.spin(1.8, axis=1, steps=4))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    sx = side * 6.2
    for k in range(3):
        P.cube("arm", (sx - 3.2 + side * 0.6 * k, 24.2 - 1.6 * k, -2.8 - 0.1 * k), (6.4 + k, 1.6, 5.8), "feather" if k % 2 == 0 else "silver", rot=(0, 0, side * (-8 - 10 * k)), share="pl%d" % k)
    P.cube("arm", (sx - 3.4, 21.8, -2.9), (6.8, 1, 5.8), "gold", share="edge")
    feather(P, "arm", (sx + side * 1.2, 25.2, 0), (side * 0.7, 0.8), 5, 2, "silver")
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 14.8, -2.5), (5, 5, 5), "silver", inflate=0.5, share="cuff")
    P.cube("arm", (cx - 2.6, 19.6, -2.6), (5.2, 1, 5.2), "gold", inflate=0.3, share="rim")
    for k in range(3):
        feather(P, "arm", (cx + side * (2.4 + 0.3 * k), 19.0 - 1.8 * k, 1.6), (side * 0.25, -0.2), 7 - k, 2, "feather")
    P.cube("arm", (cx - 2.3, 10.8, -2.3), (4.6, 3.4, 4.6), "gold", inflate=0.4, share="fist")
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 2.0, -2.5), (4.8, 7, 5), "silver", inflate=0.4, share="boot")
    P.cube("leg", (cx - 2.5, 8.6, -2.6), (5, 1, 5.2), "gold", inflate=0.3, share="rim")
    for k in range(3):                                               # the winged boot: a small wing at the ankle
        feather(P, "leg", (cx + side * 2.6, 5.4 - 0.8 * k, 1.2), (side * 0.9, 0.5 - 0.6 * k + 0.2), 6 - k, 2, "feather")
    P.puff("leg", (cx, 1.8, -1.6), 2.4, "feather", share="toe")
    P.save()
    return P


def hawk():
    P = piece("hawk", 128, 128, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("body", "root", (0, 10, 0))
    P.bone("head", "body", (0, 12, -7))
    P.bone("wing_r", "body", (-3, 12, 0))
    P.bone("wing_l", "body", (3, 12, 0))
    P.bone("tail", "body", (0, 10, 6))
    P.puff("body", (0, 10, 0), 4.6, "feather", share="b")
    P.puff("body", (0, 10, 4.4), 3.4, "silver", share="b2")
    P.cube("body", (-2, 7, -6), (4, 6, 3), "silver", share="chest")
    P.puff("head", (0, 12.4, -8), 3.0, "feather", share="h")
    P.cube("head", (-0.9, 11.4, -12.6), (1.8, 1.4, 3.2), "gold", share="beak")
    P.cube("head", (-0.6, 10.2, -13.6), (1.2, 1.4, 2.6), "gold", share="beak2")
    P.cube("head", (-2.6, 13.2, -10.6), (1.4, 1, 1), "core", share="eye")
    P.cube("head", (1.2, 13.2, -10.6), (1.4, 1, 1), "core", share="eye")
    for k in range(5):
        P.cube("tail", (-3.0 + k * 1.3, 8.6, 6 + k % 2), (1.6, 1, 8 - abs(2 - k)), "feather" if k % 2 == 0 else "silver", share="tl%d" % abs(2 - k))
    for side, tag in ((-1, "r"), (1, "l")):
        base = (side * 3, 12, 0)
        P.cube("wing_" + tag, (side * 3 - 1 if side > 0 else side * 3 - 7, 11.5, -2), (8, 1.4, 5), "silver", share="wa")
        for k in range(7):
            t = k / 6.0
            P.cube("wing_" + tag, ((side * (7 + 2.4 * k)) - 1, 11.2 - 0.5 * k, -1.6 + 0.7 * k), (2, 1, 9 - 0.6 * k), "feather" if k % 2 == 0 else "silver", share="wf%d" % k)
        P.cube("wing_" + tag, (side * 24 - 1, 10.0, 3.5), (2, 1, 5), "gold", share="wt")
    a = P.anim
    a.clip("idle", 0.8, True)
    a.rot("idle", "wing_r", L.loop(0.8, lambda p: (0, 0, -34 * math.sin(p * 2 * math.pi))))
    a.rot("idle", "wing_l", L.loop(0.8, lambda p: (0, 0, 34 * math.sin(p * 2 * math.pi))))
    a.pos("idle", "body", L.sine(0.8, 0.8, 0.25))
    a.rot("idle", "tail", L.sine3(0.8, (6, 0, 0), 0.1))
    a.clip("dive", 0.6, False)
    a.rot("dive", "wing_r", {0.0: (0, 0, 0), 0.2: (0, 0, 55), 0.6: (0, 0, 55)})
    a.rot("dive", "wing_l", {0.0: (0, 0, 0), 0.2: (0, 0, -55), 0.6: (0, 0, -55)})
    a.rot("dive", "body", {0.0: (0, 0, 0), 0.2: (50, 0, 0), 0.6: (50, 0, 0)})
    P.save()
    return P


def blade():
    P = piece("blade", 64, 64, 30)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("spin", "root", (0, 8, 0))
    P.cube("spin", (-1, -2, -1), (2, 28, 1), "core", share="shaft")
    for k in range(7):
        ln = 8 - 0.6 * k
        P.cube("spin", (-1 - 3 + 0.0, 2 + 3 * k, -0.5), (3, ln, 1), "feather" if k % 2 == 0 else "silver", rot=(0, 0, 18 + 3 * k), share="L%d" % k)
        P.cube("spin", (1, 2 + 3 * k, -0.5), (3, ln, 1), "feather" if k % 2 == 0 else "silver", rot=(0, 0, -(18 + 3 * k)), share="R%d" % k)
    P.cube("spin", (-1.5, 26, -1), (3, 4, 2), "gold", share="tip")
    a = P.anim
    a.clip("idle", 1.2, True)
    a.rot("idle", "spin", L.spin(1.2, axis=1, steps=4))
    a.pos("idle", "spin", L.sine(1.2, 0.6, 0.0))
    P.save()
    return P


def textures():
    def feather_sprite(d, S):
        c = S / 2.0
        d.polygon([(c, S * 0.02), (c + S * 0.22, S * 0.3), (c + S * 0.18, S * 0.7), (c + S * 0.03, S * 0.98), (c - S * 0.03, S * 0.98), (c - S * 0.18, S * 0.7), (c - S * 0.22, S * 0.3)], fill=255)
        for k in range(14):                                          # barbs: darker diagonal notches
            y = S * (0.12 + 0.06 * k)
            w = S * 0.2 * (1 - abs(k - 6) / 9.0)
            d.line((c, y, c + w, y - S * 0.05), fill=120, width=max(1, S // 80))
            d.line((c, y, c - w, y - S * 0.05), fill=120, width=max(1, S // 80))
        d.line((c, S * 0.04, c, S * 0.98), fill=255, width=max(2, S // 50))
    L.particle_shape("wing_feather", feather_sprite, 128, blur=0.5, glow=0.12)

    def wing_sprite(d, S):                                           # a spread wing: arm bone from the bottom left to the wrist, a fan of flight feathers
        pts_arm = [(S * 0.06, S * 0.9), (S * 0.3, S * 0.5), (S * 0.62, S * 0.34)]
        d.line(pts_arm, fill=255, width=max(3, S // 36))
        for k in range(11):
            t = k / 10.0
            bx = S * (0.1 + 0.55 * t)
            by = S * (0.86 - 0.52 * t)
            ang = math.radians(300 - 75 * t)                       # fanning from straight down-right to right
            ex = bx + math.cos(ang) * S * (0.34 + 0.25 * t)
            ey = by - math.sin(ang) * S * (0.34 + 0.25 * t) * -1
            ex, ey = bx + S * (0.12 + 0.3 * t) * (0.6 + 0.4 * t), by + S * (0.55 - 0.38 * t) * 0.9
            w = S * 0.045
            d.polygon([(bx - w, by), (bx + w, by), (ex + w * 0.5, ey), (ex - w * 0.2, ey + S * 0.02)], fill=255)
    L.particle_shape("wing_wing", wing_sprite, 256, blur=0.8, glow=0.18)
    L.particle_streak("wing_streak", 64, 256, 303, core=0.07, wisp=0.55)
    L.particle_ring("wing_halo", 256, 0.80, 0.95, 0.05, ticks=16, seed=304, wobble=0.0)

    def glint(d, S):
        c = S / 2.0
        d.polygon([(c, 0), (c + S * 0.06, c - S * 0.06), (S, c), (c + S * 0.06, c + S * 0.06), (c, S), (c - S * 0.06, c + S * 0.06), (0, c), (c - S * 0.06, c - S * 0.06)], fill=255)
    L.particle_shape("wing_glint", glint, 64, blur=0.5, glow=0.3)

    def down(d, S):                                                  # a small curled down feather
        c = S / 2.0
        pts = []
        for i in range(24):
            t = i / 23.0
            pts.append((c + S * 0.18 * math.sin(t * 4.0) * (1 - t * 0.4), S * (0.08 + 0.84 * t)))
        for i in range(len(pts) - 1):
            d.line((pts[i], pts[i + 1]), fill=255, width=max(2, int(S * 0.05 * (1 - abs(i / 23.0 - 0.4)))))
        for i in range(2, 22, 3):
            x, y = pts[i]
            d.line((x, y, x + S * 0.16, y - S * 0.06), fill=200, width=max(1, S // 60))
            d.line((x, y, x - S * 0.16, y - S * 0.06), fill=200, width=max(1, S // 60))
    L.particle_shape("wing_down", down, 64, blur=0.5, glow=0.1)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), hawk(), blade()]
    textures()
    print("models:", " ".join(p.m.id for p in out))

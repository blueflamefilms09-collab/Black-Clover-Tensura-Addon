"""Vortex Magic rebuild (1.93.0): the temporary armour pieces, the summon models (vortex core, cyclone funnel) and the VFX textures.

    python3 -B tools/gen_vortex_rebuild.py

Armour (client/aura/VortexAura, one geo model per piece):
  vortex_helm      a brow band with a coiled spiral crest (a cone of turning rings) and two swept swirl fins (clip idle: the crest turns)
  vortex_chest     a plate with a spiral emblem of concentric rings and a glowing eye, wind-wrapped shoulders
  vortex_belt      three tilted rings whirling round the waist (clip idle)
  vortex_back      the whirling mantle: three tilted bands of wind turning round the body at three speeds and a tail of tapering rings behind (clip idle)
  vortex_pauld_r / _l   a spiral shell of stacked, turning plates
  vortex_vamb_r / _l    two tilted wind rings round the forearm and a cyclone gauntlet
  vortex_greave_r / _l  spiral boots: rings at the ankle and swept shin vanes
Summons (client/prop/VortexPropPainter): vortex_core (a glowing core in three gyroscope rings, orbiting motes), vortex_funnel (a cyclone of nine rings, spinning).
Particles: vortex_spiral, vortex_ring, vortex_streak, vortex_core, vortex_dust, vortex_arc.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402

KEY = "vortex"


def mats():
    return {
        "wind": L.sheet_swirl(11, [(18, 70, 92), (30, 120, 148), (60, 172, 196), (140, 224, 236), (240, 255, 255)], glow_col=(170, 250, 255)),
        "band": L.sheet_swirl(23, [(40, 112, 140), (70, 160, 190), (120, 210, 228), (190, 240, 248), (255, 255, 255)], glow_col=(190, 255, 255), arms=4),
        "steel": L.sheet_solid((46, 96, 118), glow=0.0, glow_col=(150, 240, 255), noise=0.12, seed=3),
        "core": L.sheet_solid((210, 255, 255), glow=1.0, glow_col=(160, 250, 255), noise=0.04, seed=4),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("head", "root", (0, 24, 0))
    P.bone("crest", "head", (0, 32, 0))
    P.cube("head", (-4.6, 30.0, -4.7), (9.2, 1.6, 9.4), "steel", inflate=0.1, share="band")
    P.cube("head", (-1, 30.4, -5.2), (2, 1, 1), "core", share="eye")
    for k in range(5):                                               # a cone of turning rings: the spiral crest
        r = 4.2 - 0.8 * k
        P.ring("crest", (0, 32.6 + 1.9 * k, 0), r, 1.4, max(5, 9 - k), "band", axis="y", depth=1.6, share="cr%d" % k)
    for s in (-1, 1):
        pts = [(s * 4.6, 29.6, 0.4), (s * 6.2, 29.6, 2.4), (s * 6.8, 30.6, 5.4), (s * 5.6, 32.0, 8.0)]
        P.chain("head", pts, 2, "wind", taper=0.5, share="fin%d" % (s > 0))
    P.anim.clip("idle", 2.0, True)
    P.anim.rot("idle", "crest", L.spin(2.0, axis=1, steps=4))
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.cube("torso", (-4.5, 15.0, -2.9), (9, 9, 1), "steel", inflate=0.2, share="plate")
    P.cube("torso", (-5.1, 22.0, -2.7), (10.2, 2.2, 5.4), "wind", inflate=0.1, share="yoke")
    P.cube("torso", (-4.5, 14.4, 2.0), (9, 9.4, 1), "steel", inflate=0.2, share="back")
    for k, r in enumerate((3.4, 2.4, 1.4)):                         # the spiral emblem: concentric rings, a glowing eye at the heart
        P.ring("torso", (0, 19.4, -3.9), r, 0.8, 10 - 2 * k, "band", axis="z", depth=0.8, share="em%d" % k)
    P.cube("torso", (-1.0, 18.4, -4.4), (2, 2, 1), "core", share="heart")
    for s in (-1, 1):
        P.ring("torso", (s * 5.4, 23.0, 0), 3.2, 1.2, 8, "wind", axis="x", depth=1.4, share="sh")
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    for k, tilt in enumerate((0, 22, -22)):
        P.bone("ring%d" % k, "torso", (0, 12.4, 0), rotation=(tilt * 0.5, 0, tilt))
        P.ring("ring%d" % k, (0, 12.4, 0), 6.4 + 0.5 * k, 1.4, 12, "band", axis="y", depth=1.6, share="rg")
    P.cube("torso", (-4.5, 11.4, -2.8), (9, 2.0, 5.6), "steel", inflate=0.25, share="band")
    a = P.anim
    a.clip("idle", 3.0, True)
    for k in range(3):
        a.rot("idle", "ring%d" % k, L.spin(3.0 - 0.6 * k, axis=1, steps=4) if k != 1 else {t: tuple(-v for v in vv) for t, vv in L.spin(3.0, axis=1, steps=4).items()})
    P.save()
    return P


def back():
    P = piece("back", 64, 128, 4)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("mantle", "root", (0, 18, 2))
    for k, (rad, y, tilt) in enumerate(((9.6, 24, 14), (10.6, 18, -10), (9.0, 12, 18))):   # three bands turning round the body at three heights
        P.bone("band%d" % k, "mantle", (0, y, 0), rotation=(tilt, 0, -tilt * 0.6))
        P.ring("band%d" % k, (0, y, 0), rad, 1.8, 11, "band", axis="y", depth=2.2, share="b%d" % k)
        for j in range(3):
            a = 2 * math.pi * j / 3
            P.puff("band%d" % k, (math.cos(a) * rad, y, math.sin(a) * rad), 1.6, "core", share="m")
    P.bone("tail", "mantle", (0, 22, 6))
    for k in range(5):                                               # a tail of tapering rings streaming behind
        P.ring("tail", (0, 22 - 3.6 * k, 6.5 + 1.4 * k), 4.4 - 0.7 * k, 1.4, 8, "wind", axis="y", depth=1.6, share="t%d" % k)
    a = P.anim
    a.clip("idle", 4.0, True)
    a.rot("idle", "band0", L.spin(2.0, axis=1, steps=4))
    a.rot("idle", "band1", {t: tuple(-v for v in vv) for t, vv in L.spin(2.7, axis=1, steps=4).items()})
    a.rot("idle", "band2", L.spin(3.4, axis=1, steps=4))
    a.rot("idle", "tail", L.sine3(4.0, (6, 0, 8), 0.0))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    P.bone("shell", "arm", (side * 6.2, 24, 0))
    sx = side * 6.2
    for k in range(4):                                               # a shell of stacked plates, each turned a little further
        w = 7.6 - 1.4 * k
        P.cube("shell", (sx - w / 2.0, 23.4 + 1.5 * k, -w / 2.0), (w, 1.5, w), "band" if k % 2 == 0 else "wind", rot=(0, 18 * k, 0), share="sp%d" % k)
    P.cube("shell", (sx - 0.8, 29.4, -0.8), (1.6, 1.6, 1.6), "core", share="pt")
    P.anim.clip("idle", 3.0, True)
    P.anim.rot("idle", "shell", L.spin(3.0, axis=1, steps=4))
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 14.8, -2.5), (5, 5, 5), "steel", inflate=0.4, share="cuff")
    for k in range(2):
        P.bone("wr%d" % k, "arm", (cx, 17.4 + 2.4 * k, 0), rotation=(12 * (1 - 2 * k), 0, 14 * (1 - 2 * k)))
        P.ring("wr%d" % k, (cx, 17.4 + 2.4 * k, 0), 4.2, 1.2, 9, "band", axis="y", depth=1.4, share="wr")
    P.cube("arm", (cx - 2.3, 10.8, -2.3), (4.6, 3.4, 4.6), "wind", inflate=0.4, share="fist")
    P.cube("arm", (cx - 0.8, 11.2, -3.0), (1.6, 1.6, 1), "core", share="kn")
    P.anim.clip("idle", 2.0, True)
    P.anim.rot("idle", "wr0", L.spin(1.6, axis=1, steps=4))
    P.anim.rot("idle", "wr1", {t: tuple(-v for v in vv) for t, vv in L.spin(1.4, axis=1, steps=4).items()})
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    P.bone("ank", "leg", (side * 1.9, 4, 0), rotation=(0, 0, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 2.0, -2.5), (4.8, 7, 5), "steel", inflate=0.4, share="boot")
    P.ring("ank", (cx, 4.4, 0), 4.0, 1.2, 9, "band", axis="y", depth=1.4, share="ar")
    for k in range(3):
        P.bar("leg", (cx + side * 2.8, 8.8 - 1.6 * k, 0.5 + k), (cx + side * 4.8, 7.2 - 1.8 * k, 3.0 + 2.0 * k), 1.4, "wind", share="vn%d" % k)
    P.puff("leg", (cx, 1.8, -1.6), 2.2, "core", share="toe")
    P.anim.clip("idle", 2.0, True)
    P.anim.rot("idle", "ank", L.spin(1.5, axis=1, steps=4))
    P.save()
    return P


def core():
    P = piece("core", 128, 128, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("heart", "root", (0, 12, 0))
    for k, (rad, ax) in enumerate(((10.0, "y"), (8.0, "x"), (6.0, "z"))):                 # a gyroscope: three rings on three axes
        P.bone("gyro%d" % k, "heart", (0, 12, 0))
        P.ring("gyro%d" % k, (0, 12, 0), rad, 1.8, 14 - 2 * k, "band", axis=ax, depth=2.0, share="g%d" % k)
    P.puff("heart", (0, 12, 0), 3.6, "core", share="c")
    P.bone("motes", "heart", (0, 12, 0))
    for k in range(5):
        a = 2 * math.pi * k / 5
        P.puff("motes", (math.cos(a) * 13, 12 + 3 * math.sin(k * 2.1), math.sin(a) * 13), 1.4, "wind", share="mt")
    a = P.anim
    a.clip("idle", 3.0, True)
    a.rot("idle", "gyro0", L.spin(1.5, axis=1, steps=4))
    a.rot("idle", "gyro1", L.spin(2.0, axis=0, steps=4))
    a.rot("idle", "gyro2", L.spin(2.5, axis=2, steps=4))
    a.rot("idle", "motes", L.spin(1.2, axis=1, steps=4))
    a.pos("idle", "heart", L.sine(3.0, 0.8, 0.0))
    a.clip("implode", 0.5, False)
    a.scale("implode", "root", {0.0: (1, 1, 1), 0.5: (0.1, 0.1, 0.1)})
    P.save()
    return P


def funnel():
    P = piece("funnel", 128, 128, 30)
    P.bone("root", pivot=(0, 0, 0))
    n = 8
    for k in range(n):
        t = k / float(n - 1)
        y = 3 + k * 6.5
        r = 3.0 + 12.0 * t ** 0.9
        P.bone("ring%d" % k, "root", (0, y, 0))
        P.ring("ring%d" % k, (0, y, 0), r, 2.0 + t, 8 + k, "band" if k % 2 == 0 else "wind", axis="y", depth=2.6, share="f%d" % k)
        if k % 2 == 1:
            P.puff("ring%d" % k, (math.cos(k) * r, y, math.sin(k) * r), 1.8, "core", share="d")
    a = P.anim
    a.clip("idle", 2.0, True)
    for k in range(n):
        v = L.spin(0.9 + 0.12 * k, axis=1, steps=4)
        a.rot("idle", "ring%d" % k, v if k % 2 == 0 else {t: tuple(-x for x in vv) for t, vv in v.items()})
    P.save()
    return P


def textures():
    def spiral(d, S):                                                # a spiral of four tapering arms
        c = S / 2.0
        for arm in range(4):
            pts = []
            for i in range(60):
                t = i / 59.0
                ang = arm * math.pi / 2 + t * 4.2
                pts.append((c + math.cos(ang) * c * (0.06 + 0.9 * t), c + math.sin(ang) * c * (0.06 + 0.9 * t)))
            for i in range(len(pts) - 1):
                w = max(2, int(S * 0.045 * (1 - i / 59.0) + S * 0.01))
                d.line((pts[i], pts[i + 1]), fill=255 if i > 4 else 200, width=w)
    L.particle_shape("vortex_spiral", spiral, 256, blur=0.9, glow=0.15)
    L.particle_ring("vortex_ring", 256, 0.80, 0.97, 0.06, ticks=0, seed=404, wobble=0.03)
    L.particle_streak("vortex_streak", 64, 256, 403, core=0.06, wisp=0.4)
    L.particle_soft("vortex_core", 128, 2.0)
    L.particle_cloud("vortex_dust", 405, 128, lobes=5, detail=0.5, hard=0.1)

    def arc(d, S):                                                   # a curved gust blade: a thick crescent with a thin tail
        c = S / 2.0
        outer = [(S * 0.04 + S * 0.92 * i / 30.0, S * 0.62 - S * 0.5 * math.sin(math.pi * i / 30.0)) for i in range(31)]
        inner = [(S * 0.04 + S * 0.92 * i / 30.0, S * 0.62 - S * 0.5 * math.sin(math.pi * i / 30.0) * 0.4) for i in range(30, -1, -1)]
        d.polygon(outer + inner, fill=255)
    L.particle_shape("vortex_arc", arc, 128, blur=0.7, glow=0.2)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), core(), funnel()]
    textures()
    print("models:", " ".join(p.m.id for p in out))

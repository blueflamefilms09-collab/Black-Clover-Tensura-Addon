"""Smoke Magic rebuild (1.93.0): the temporary armour pieces, the summon models (smoke clone, smoke serpent) and the VFX textures.

    python3 -B tools/gen_smoke_rebuild.py          # writes geo / animations / textures and prints the cube counts

Armour (Space.PLAYER pieces of client/aura/SmokeAura, one geo model each, drawn on the model part it was built for):
  smoke_helm      a hood of smoke: crown billows, cheek puffs, a dark brow band with two ember sparks, two plumes curling back (clip idle: flutter)
  smoke_chest     cloud collar, a banded chest plate of dense smoke, a glowing ember core on the sternum
  smoke_belt      a sash of puffs with three tassets of falling smoke
  smoke_back      the smoke mantle: three tiers of billows hanging from the shoulders to the knees and two plumes rising off the shoulder blades (clip idle: sway)
  smoke_pauld_r / _l   a heaped pauldron of billows with an ember in the middle
  smoke_vamb_r / _l    a cuff of puffs round the forearm and a fist of smoke
  smoke_greave_r / _l  a boot of smoke with a knee billow and a trailing wisp
Summons (Space.PROP, client/prop/SmokePropPainter):
  smoke_clone     a caster-shaped figure of billows that dissolves into a plume instead of legs (idle, strike, vanish)
  smoke_serpent   a ten-segment serpent of smoke with ember eyes and a jaw (idle, strike)
VFX textures (textures/particle/smoke_*.png): puff, wisp, ring, sigil, ember, curl.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402
from PIL import ImageDraw         # noqa: E402

KEY = "smoke"


def mats():
    return {
        "smoke": L.sheet_smoke(11, [(92, 86, 114), (132, 124, 158), (172, 164, 196), (208, 202, 228), (240, 236, 250)]),
        "dense": L.sheet_smoke(23, [(62, 57, 84), (92, 86, 120), (126, 118, 154), (160, 152, 188), (196, 190, 220)], ember_amt=0.35),
        "wisp": L.sheet_smoke(37, [(104, 96, 124), (146, 138, 166), (184, 176, 204), (222, 216, 238), (246, 244, 252)], ember_amt=0.0),
        "ember": L.sheet_solid((255, 150, 80), glow=0.95, glow_col=(255, 150, 70), noise=0.1, seed=3),
        "core": L.sheet_solid((255, 214, 150), glow=1.0, glow_col=(255, 190, 110), noise=0.05, seed=4),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def side_x(side, v):
    return v if side > 0 else -v


# ----------------------------------------------------------------------------------------------------- armour
def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("hood", "root", (0, 24, 0))
    P.bone("plume_l", "hood", (3, 32, 3))
    P.bone("plume_r", "hood", (-3, 32, 3))
    for i in range(5):                                               # the crown of billows
        a = 2 * math.pi * i / 5
        P.puff("hood", (math.cos(a) * 4.0, 32.6 + 0.4 * (i % 2), math.sin(a) * 4.0), 2.8, "smoke", share="hc")
    P.puff("hood", (0, 34.4, 0), 2.6, "wisp", share="hw")
    for s in (-1, 1):                                                # cheek and ear puffs
        P.puff("hood", (s * 4.9, 28.2, 0.5), 2.2, "smoke", share="hs")
        P.puff("hood", (s * 4.6, 25.6, 1.5), 1.8, "dense", share="hd")
    for k, (z, y) in enumerate(((4.4, 30), (4.9, 27), (4.6, 24.4))):  # the drape down the back of the neck
        P.puff("hood", (0, y, z), 2.6 - 0.2 * k, "dense", share="hb")
    P.cube("hood", (-4.7, 30.2, -4.8), (9, 2, 1), "dense", inflate=0.1, share="brow")
    P.cube("hood", (-3, 30.4, -5.4), (2, 1, 1), "ember", share="e1")
    P.cube("hood", (1, 30.4, -5.4), (2, 1, 1), "ember", share="e1")
    for s, bn in ((1, "plume_l"), (-1, "plume_r")):                   # two plumes curling back like horns of smoke
        pts = [(s * 3, 32.5, 2.5), (s * 4.2, 35, 3.6), (s * 4.0, 37.6, 6.0), (s * 2.6, 39.0, 9.6)]
        for i, p in enumerate(pts):
            P.puff(bn, p, 1.9 - 0.25 * i if i < 4 else 1.0, "wisp" if i > 1 else "smoke", share="pl%d" % i)
    a = P.anim
    a.clip("idle", 2.4, True)
    a.rot("idle", "plume_l", L.sine3(2.4, (6, 0, 5), 0.0))
    a.rot("idle", "plume_r", L.sine3(2.4, (6, 0, -5), 0.25))
    a.rot("idle", "hood", L.sine3(2.4, (1.2, 0, 0), 0.5))
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    for i in range(6):                                               # the cloud collar
        a = 2 * math.pi * i / 6
        P.puff("torso", (math.cos(a) * 4.0, 25.2, math.sin(a) * 3.0), 2.5, "smoke", share="col")
    P.cube("torso", (-4.5, 15.4, -2.9), (9, 8, 1), "dense", inflate=0.2, share="front")       # the plate: bands of dense smoke over the ribs
    for k in range(3):
        P.cube("torso", (-4.9, 16.2 + k * 2.6, -3.4), (10, 2, 1), "smoke", inflate=0.1, share="band")
    P.puff("torso", (0, 19.4, -3.4), 2.2, "core", share="core")
    P.puff("torso", (-3.2, 21.6, -3.2), 2.0, "smoke", share="sh")
    P.puff("torso", (3.2, 21.6, -3.2), 2.0, "smoke", share="sh")
    P.cube("torso", (-4.5, 14.5, 2.0), (9, 9, 1), "dense", inflate=0.2, share="back")
    P.puff("torso", (0, 19, 3.4), 2.4, "smoke", share="bk")
    P.puff("torso", (-3, 16.5, 3.2), 1.8, "dense", share="bk2")
    P.puff("torso", (3, 16.5, 3.2), 1.8, "dense", share="bk2")
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.bone("tassets", "torso", (0, 13, 0))
    for i in range(7):
        a = 2 * math.pi * i / 7
        P.puff("torso", (math.cos(a) * 4.5, 12.4, math.sin(a) * 2.9), 2.2, "dense", share="b")
    P.cube("torso", (-4.4, 11.6, -2.8), (9, 2, 1), "wisp", inflate=0.2, share="sash")
    P.puff("torso", (0, 12.2, -3.8), 1.0, "ember", share="bm")
    for x, ln in ((-3, 4), (0, 6), (3, 4)):                           # tassets: smoke hanging in strips
        for k in range(ln // 2):
            P.puff("tassets", (x, 10.2 - k * 2.6, -3.1), 1.7 - 0.2 * k, "smoke", share="ts%d" % k)
    P.anim.clip("idle", 2.0, True)
    P.anim.rot("idle", "tassets", L.sine3(2.0, (5, 0, 0), 0.0))
    P.save()
    return P


def back():
    P = piece("back", 64, 64, 4)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("mantle", "root", (0, 24, 3))
    cols = (("col_l", 4.4), ("col_c", 0.0), ("col_r", -4.4))
    for n, x in cols:
        P.bone(n, "mantle", (x, 24, 3.2))
    P.bone("plume_l", "mantle", (2.5, 22, 3))
    P.bone("plume_r", "mantle", (-2.5, 22, 3))
    for n, x in cols:                                                 # tiers of billows hanging from the shoulders to the knees
        for k in range(4):
            y = 23.2 - k * 5.2
            z = 3.6 + 0.9 * k
            r = 2.6 + 0.5 * math.sin(k * 1.1) - 0.1 * k
            P.puff(n, (x * (1 + 0.12 * k), y, z), max(1.6, r + (0.8 if n == "col_c" else 0)), "smoke" if (k + (x > 0)) % 2 == 0 else "dense", share="m%d" % k)
    P.cube("mantle", (-4.6, 21.5, 2.6), (9, 3, 1), "dense", inflate=0.2, share="yoke")
    for s, bn in ((1, "plume_l"), (-1, "plume_r")):                   # plumes rising off the shoulder blades, embers at their feet
        P.puff(bn, (s * 2.6, 22.0, 3.0), 1.5, "ember", share="pe")
        for i in range(4):
            P.puff(bn, (s * (2.8 + 0.5 * i), 24.2 + 3.0 * i, 3.4 + 0.9 * i), 2.6 - 0.4 * i, "wisp" if i > 2 else "smoke", share="pp%d" % i)
    a = P.anim
    a.clip("idle", 3.0, True)
    for i, (n, x) in enumerate(cols):
        a.rot("idle", n, L.sine3(3.0, (7, 0, 4 * (1 if x >= 0 else -1)), i * 0.18))
    a.rot("idle", "plume_l", L.sine3(3.0, (6, 0, 6), 0.1))
    a.rot("idle", "plume_r", L.sine3(3.0, (6, 0, -6), 0.35))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    sx = side * 6.2
    P.puff("arm", (sx, 24.2, 0), 3.4, "smoke", share="pb")
    P.puff("arm", (sx + side * 1.6, 23.0, 0.6), 2.5, "dense", share="pd")
    P.puff("arm", (sx - side * 0.6, 25.8, -1.2), 2.0, "wisp", share="pw")
    P.puff("arm", (sx + side * 0.2, 24.6, -2.6), 1.3, "ember", share="pe")
    P.puff("arm", (sx + side * 0.4, 22.2, 2.2), 2.0, "smoke", share="pb2")
    P.cube("arm", (sx - 3.4, 21.6, -2.6), (7, 1, 5), "dense", inflate=0.3, share="skirt")
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 15.2, -2.5), (5, 5, 5), "dense", inflate=0.5, share="cuff")
    for i in range(4):
        a = math.pi / 2 * i + 0.4
        P.puff("arm", (cx + math.cos(a) * 2.9, 17.4, math.sin(a) * 2.9), 1.6, "smoke", share="cp")
    P.puff("arm", (cx, 12.6, 0), 2.6, "smoke", share="fist")
    P.puff("arm", (cx, 11.6, -1.6), 0.8, "ember", share="knuck")
    P.puff("arm", (cx + side * 1.6, 13.4, 0.8), 1.5, "wisp", share="fw")
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 3.2, -2.5), (4.8, 6, 5), "dense", inflate=0.4, share="shin")
    P.puff("leg", (cx, 11.0, -2.2), 2.4, "smoke", share="knee")
    P.puff("leg", (cx, 2.2, -1.2), 2.8, "smoke", share="boot")
    P.puff("leg", (cx, 1.0, -3.4), 1.8, "dense", share="toe")
    P.puff("leg", (cx, 4.4, 3.4), 2.0, "wisp", share="tr0")
    P.save()
    return P


# ----------------------------------------------------------------------------------------------------- summons
def clone():
    P = piece("clone", 128, 128, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("body", "root", (0, 14, 0))
    P.bone("head", "body", (0, 26, 0))
    P.bone("arm_r", "body", (-6, 24, 0))
    P.bone("arm_l", "body", (6, 24, 0))
    P.bone("plume", "root", (0, 12, 0))
    for k, (y, r, m) in enumerate(((18, 4.6, "dense"), (22, 5.2, "smoke"), (26, 5.0, "smoke"))):      # the torso: three stacked billows
        P.puff("body", (0, y, 0), r, m, share="t%d" % k)
    P.puff("body", (0, 22, -3.4), 2.0, "core", share="core")
    P.puff("head", (0, 30.5, 0), 4.3, "smoke", share="hd")
    P.puff("head", (0, 33.8, 0), 2.6, "wisp", share="hw")
    P.cube("head", (-3, 30.6, -4.7), (2, 1, 1), "ember", share="eye")
    P.cube("head", (1, 30.6, -4.7), (2, 1, 1), "ember", share="eye")
    for s, bn in ((-1, "arm_r"), (1, "arm_l")):
        P.puff(bn, (s * 7.5, 24, 0), 2.8, "smoke", share="sh")
        P.puff(bn, (s * 8, 18.5, 0), 2.4, "dense", share="fo")
        P.puff(bn, (s * 8, 14, -1.0), 2.5, "wisp", share="fi")
    for k in range(6):                                               # no legs: the body thins into a plume
        P.puff("plume", (0.8 * math.sin(k * 1.3), 12 - k * 2.6, 0.6 * math.cos(k * 1.3)), max(1.2, 4.2 - 0.55 * k), "smoke" if k % 2 == 0 else "dense", share="pl%d" % k)
    a = P.anim
    a.clip("idle", 2.4, True)
    a.pos("idle", "root", L.sine(2.4, 1.2, 0.0, axis=1))
    a.rot("idle", "arm_r", L.sine3(2.4, (8, 0, 6), 0.0))
    a.rot("idle", "arm_l", L.sine3(2.4, (8, 0, -6), 0.5))
    a.rot("idle", "plume", L.sine3(2.4, (4, 0, 6), 0.25))
    a.rot("idle", "head", L.sine3(2.4, (3, 4, 0), 0.7))
    a.clip("strike", 0.8, False)
    a.rot("strike", "arm_r", {0.0: (0, 0, 0), 0.25: (-110, 0, 10), 0.45: (35, 0, 0), 0.8: (0, 0, 0)})
    a.rot("strike", "body", {0.0: (0, 0, 0), 0.25: (-12, 25, 0), 0.45: (14, -20, 0), 0.8: (0, 0, 0)})
    a.clip("vanish", 1.0, False)
    a.scale("vanish", "root", {0.0: (1, 1, 1), 0.6: (1.2, 0.8, 1.2), 1.0: (0.05, 0.05, 0.05)})
    a.rot("vanish", "root", {0.0: (0, 0, 0), 1.0: (0, 360, 0)})
    P.save()
    return P


def serpent():
    P = piece("serpent", 128, 128, 30)
    P.bone("root", pivot=(0, 0, 0))
    n = 10
    prev = "root"
    for i in range(n):
        P.bone("seg%d" % i, prev, (0, 8, i * 7))
        prev = "seg%d" % i
    for i in range(n):                                               # tail at +z, head at -z; radius grows towards the head
        t = i / float(n - 1)
        r = 1.4 + 3.4 * (1 - t) ** 0.8
        P.puff("seg%d" % i, (0, 8, i * 7), r, "dense" if i % 2 else "smoke", share="s%d" % i)
        if i % 3 == 1:
            P.puff("seg%d" % i, (0, 8 + r * 0.9, i * 7), max(1.0, r * 0.45), "wisp", share="sw%d" % i)
    P.bone("head", "seg0", (0, 8, -3))
    P.puff("head", (0, 8.2, -5.6), 4.2, "smoke", share="h")
    P.cube("head", (-2.4, 11.4, -8.7), (2, 1, 1), "ember", share="e")
    P.cube("head", (0.4, 11.4, -8.7), (2, 1, 1), "ember", share="e")
    P.bone("jaw", "head", (0, 6, -4))
    P.cube("jaw", (-2.2, 5.0, -10.2), (4, 1.4, 5), "dense", share="jaw")
    for s in (-1, 1):
        P.cube("head", (s * 1.4 - 0.4, 6.2, -10.0), (1, 2, 1), "core", share="fang")
    a = P.anim
    a.clip("idle", 2.6, True)
    for i in range(n):
        a.rot("idle", "seg%d" % i, L.sine3(2.6, (0, 13, 0), -i * 0.12))
    a.rot("idle", "jaw", L.sine3(2.6, (6, 0, 0), 0.2))
    a.clip("strike", 0.9, False)
    for i in range(n):
        a.rot("strike", "seg%d" % i, {0.0: (0, 0, 0), 0.3: (-6, 22 * (1 if i % 2 else -1), 0), 0.55: (4, -18 * (1 if i % 2 else -1), 0), 0.9: (0, 0, 0)})
    a.pos("strike", "root", {0.0: (0, 0, 0), 0.3: (0, 0, 14), 0.55: (0, 0, -22), 0.9: (0, 0, 0)})
    a.rot("strike", "jaw", {0.0: (0, 0, 0), 0.3: (0, 0, 0), 0.5: (34, 0, 0), 0.9: (0, 0, 0)})
    P.save()
    return P


# ----------------------------------------------------------------------------------------------------- particles
def textures():
    L.particle_cloud("smoke_puff", 101, 128, lobes=8, detail=0.55)
    L.particle_cloud("smoke_billow", 102, 128, lobes=5, detail=0.35, hard=0.15)
    L.particle_streak("smoke_wisp", 64, 256, 103, core=0.10, wisp=0.62)
    L.particle_ring("smoke_ring", 256, 0.74, 0.96, 0.10, ticks=0, seed=104, wobble=0.07)

    def sigil(d, S):                                                 # the sigil on the ground: two rings, a swirl of three smoke arms and an eye of embers
        c = S / 2.0
        for r0, r1 in ((0.94, 0.90), (0.62, 0.59)):
            d.ellipse((c - c * r0, c - c * r0, c + c * r0, c + c * r0), outline=255, width=max(2, int(S * 0.012)))
        for arm in range(3):
            pts = []
            for i in range(40):
                t = i / 39.0
                ang = arm * 2 * math.pi / 3 + t * 2.6
                r = c * (0.12 + 0.46 * t)
                pts.append((c + math.cos(ang) * r, c + math.sin(ang) * r))
            d.line(pts, fill=255, width=max(3, int(S * 0.02)))
        for i in range(12):
            a = i * math.pi / 6
            d.line((c + math.cos(a) * c * 0.9, c + math.sin(a) * c * 0.9, c + math.cos(a) * c * 0.8, c + math.sin(a) * c * 0.8), fill=255, width=max(2, int(S * 0.012)))
        d.ellipse((c - c * 0.1, c - c * 0.1, c + c * 0.1, c + c * 0.1), fill=255)
    L.particle_shape("smoke_sigil", sigil, 256, blur=0.8, glow=0.2)

    def curl(d, S):                                                  # a curling tendril of smoke (an S shaped wisp with a bulb at the end)
        pts = []
        for i in range(60):
            t = i / 59.0
            pts.append((S * (0.5 + 0.28 * math.sin(t * 5.2) * (1 - 0.3 * t)), S * (0.94 - 0.84 * t)))
        for i in range(len(pts) - 1):
            w = max(2, int(S * (0.015 + 0.05 * math.sin(i / 59.0 * math.pi))))
            d.line((pts[i], pts[i + 1]), fill=255, width=w)
        d.ellipse((pts[-1][0] - S * 0.05, pts[-1][1] - S * 0.05, pts[-1][0] + S * 0.05, pts[-1][1] + S * 0.05), fill=255)
    L.particle_shape("smoke_curl", curl, 128, blur=1.0, glow=0.15)
    L.particle_soft("smoke_ember", 64, 2.6)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), clone(), serpent()]
    textures()
    ids = [p.m.id for p in out]
    print("models:", " ".join(ids))

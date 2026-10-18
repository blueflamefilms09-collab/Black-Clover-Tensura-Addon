"""Snow Magic rebuild (1.93.0): the temporary armour pieces, the summon models (snow golem, frost crystal) and the VFX textures.

    python3 -B tools/gen_snow_rebuild.py

Armour (client/aura/SnowAura, one geo model per piece):
  snow_helm      a hood of packed snow edged with fur, a crown of five icicles (glow: cold blue seams)
  snow_chest     a drift-white chest plate with a six-armed snowflake of ice and a glowing core, a fur collar
  snow_belt      a fur belt with a ring of hanging icicles
  snow_back      a snowflake halo of ice turning behind the back (clip idle: slow spin) and a cape of icicles
  snow_pauld_r / _l   a snow drift on the shoulder with three icicle spears
  snow_vamb_r / _l    a fur cuff and an ice gauntlet with frost thorns on the knuckles
  snow_greave_r / _l  snow boots with a fur cuff and shin spikes of ice
Summons (client/prop/SnowPropPainter): snow_golem (idle, strike, crumble), snow_crystal (idle spin, grow).
Particles: snow_flake, snow_puff, snow_streak, snow_ring, snow_glint, snow_shard.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402

KEY = "snow"


def mats():
    return {
        "snow": L.sheet_snow(11, [(150, 176, 206), (190, 212, 232), (222, 236, 248), (244, 250, 255), (255, 255, 255)]),
        "fur": L.sheet_snow(23, [(170, 190, 214), (206, 222, 238), (232, 242, 252), (250, 253, 255), (255, 255, 255)], vein=(200, 220, 240)),
        "ice": L.sheet_snow(37, [(54, 118, 190), (92, 168, 232), (140, 208, 250), (190, 236, 255), (236, 250, 255)], glint=(170, 235, 255), vein=(220, 245, 255)),
        "core": L.sheet_solid((190, 240, 255), glow=1.0, glow_col=(150, 225, 255), noise=0.05, seed=5),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("hood", "root", (0, 24, 0))
    for i in range(6):                                               # the hood: packed snow over the crown and round the ears
        a = 2 * math.pi * i / 6
        P.puff("hood", (math.cos(a) * 4.0, 32.4, math.sin(a) * 4.0), 2.8, "snow", share="hc")
    P.puff("hood", (0, 34.0, 0), 2.8, "snow", share="ht")
    for s in (-1, 1):
        P.puff("hood", (s * 4.8, 28.0, 0.3), 2.5, "snow", share="hs")
        P.puff("hood", (s * 4.6, 25.5, 1.5), 2.0, "fur", share="hf")
        P.cube("hood", (s * 4.2 - 0.6, 24.6, -4.6), (1.2, 5, 1.2), "fur", share="cheek")
    for k in range(3):
        P.puff("hood", (0, 30.5 - 2.6 * k, 4.6 + 0.4 * k), 2.6, "snow", share="hb")
    P.cube("hood", (-4.7, 30.4, -4.8), (9, 1.6, 1.2), "fur", inflate=0.1, share="fringe")        # fur band over the brow
    for i, (x, h) in enumerate(((-3.2, 4), (-1.6, 6), (0, 8), (1.6, 6), (3.2, 4))):               # the crown of icicles
        P.spike("hood", (x, 33.6, -2.2 + 0.5 * abs(x)), "+y", h, 2 if h < 8 else 3, "ice", layers=3, share="ic%d" % i)
    P.cube("hood", (-1, 31.2, -5.2), (2, 2, 1), "core", share="gem")
    P.anim.clip("idle", 3.0, True)
    P.anim.rot("idle", "hood", L.sine3(3.0, (0.8, 0, 0), 0.0))
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    for i in range(7):
        a = 2 * math.pi * i / 7
        P.puff("torso", (math.cos(a) * 4.1, 25.2, math.sin(a) * 3.1), 2.3, "fur", share="col")
    P.cube("torso", (-4.5, 14.8, -2.9), (9, 9, 1), "snow", inflate=0.25, share="plate")
    P.cube("torso", (-5.0, 22.0, -2.6), (10, 2.4, 5.2), "snow", inflate=0.1, share="yoke")
    P.cube("torso", (-4.5, 14.2, 2.0), (9, 9.5, 1), "snow", inflate=0.25, share="back")
    # the snowflake: three bars crossing at the heart of the chest, a glowing core
    for k, rz in enumerate((0, 60, 120)):
        P.cube("torso", (-0.6, 15.8, -3.9), (1.2, 7.4, 0.8), "ice", rot=(0, 0, rz), pivot=(0, 19.5, -3.5), share="fl")
    P.cube("torso", (-1.4, 18.1, -4.3), (2.8, 2.8, 1), "core", share="heart")
    for i in range(6):
        a = math.pi / 3 * i
        P.cube("torso", (math.cos(a) * 3.6 - 0.6, 19.5 + math.sin(a) * 3.6 - 0.6, -4.2), (1.2, 1.2, 0.8), "ice", share="tip")
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.cube("torso", (-4.5, 11.4, -2.8), (9, 2.4, 5.6), "fur", inflate=0.25, share="band")
    P.cube("torso", (-1.2, 11.4, -3.6), (2.4, 2.4, 1), "ice", share="buckle")
    for i in range(7):                                               # icicle tassets all round the belt
        a = 2 * math.pi * i / 7
        P.spike("torso", (math.cos(a) * 4.4, 11.2, math.sin(a) * 3.0), "-y", 5 if i % 2 else 3, 2, "ice", layers=2, share="t%d" % (i % 2))
    P.save()
    return P


def back():
    P = piece("back", 64, 128, 4)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("halo", "root", (0, 29, 7))
    P.bone("inner", "halo", (0, 29, 7))
    P.bone("cape", "root", (0, 23, 3))
    P.ring("halo", (0, 29, 7), 10.5, 1.6, 14, "ice", axis="z", depth=1.6, share="hr")
    for k in range(6):                                               # the six arms of the flake, ice bars with a cross piece and a tip each
        a = math.pi / 3 * k
        P.bar("inner", (0, 29, 7), (math.sin(a) * 9.0, 29 + math.cos(a) * 9.0, 7), 1.6, "snow", share="arm")
        mx, my = math.sin(a) * 5.4, 29 + math.cos(a) * 5.4
        P.bar("inner", (mx - math.cos(a) * 1.8, my + math.sin(a) * 1.8, 7), (mx + math.cos(a) * 1.8, my - math.sin(a) * 1.8, 7), 1.2, "ice", share="cross")
        P.cube("inner", (math.sin(a) * 9.6 - 1, 29 + math.cos(a) * 9.6 - 1, 6.4), (2, 2, 1.6), "core", share="tipc")
    P.cube("inner", (-1.5, 27.5, 6.2), (3, 3, 1.6), "core", share="hub")
    for k, x in enumerate((-4, -2, 0, 2, 4)):                         # a cape of icicles hanging from the shoulders
        P.spike("cape", (x, 22.5, 3.2 + 0.2 * k), "-y", 12 - 2 * abs(x) // 1, 2, "ice", layers=3, share="cp%d" % abs(int(x)))
    P.puff("cape", (0, 23.0, 3.2), 2.4, "fur", share="cf")
    P.puff("cape", (-3.4, 23.0, 3.0), 2.0, "fur", share="cf2")
    P.puff("cape", (3.4, 23.0, 3.0), 2.0, "fur", share="cf2")
    a = P.anim
    a.clip("idle", 8.0, True)
    a.rot("idle", "inner", L.spin(8.0, axis=2, steps=4))
    a.rot("idle", "halo", L.sine3(8.0, (2, 0, 0), 0.0))
    a.rot("idle", "cape", L.sine3(3.0, (3, 0, 2), 0.0))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    sx = side * 6.4
    P.puff("arm", (sx, 24.2, 0), 3.6, "snow", share="pb")
    P.puff("arm", (sx + side * 1.8, 23.0, 0.8), 2.6, "fur", share="pf")
    P.puff("arm", (sx - side * 0.8, 25.8, -1.4), 2.1, "snow", share="pw")
    P.spike("arm", (sx + side * 0.5, 26.6, 0), "+y", 7, 3, "ice", layers=3, share="s1")
    P.spike("arm", (sx + side * 3.4, 25.2, -0.5), "+x" if side > 0 else "-x", 6, 2, "ice", layers=3, share="s2")
    P.spike("arm", (sx + side * 0.5, 25.4, -3.0), "-z", 5, 2, "ice", layers=2, share="s3")
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 14.6, -2.5), (5, 5.4, 5), "ice", inflate=0.5, share="cuff")
    for i in range(5):
        a = 2 * math.pi * i / 5
        P.puff("arm", (cx + math.cos(a) * 2.9, 19.4, math.sin(a) * 2.9), 1.8, "fur", share="fc")
    P.cube("arm", (cx - 2.3, 10.8, -2.3), (4.6, 3.4, 4.6), "ice", inflate=0.4, share="fist")
    for k in range(3):
        P.spike("arm", (cx - 1.6 + 1.6 * k, 11.8, -2.9), "-z", 3, 1, "snow", layers=2, share="kn%d" % k)
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 1.8, -2.5), (4.8, 7, 5), "snow", inflate=0.4, share="boot")
    for i in range(5):
        a = 2 * math.pi * i / 5
        P.puff("leg", (cx + math.cos(a) * 2.4, 9.2, math.sin(a) * 2.4), 1.8, "fur", share="fc")
    P.spike("leg", (cx, 10.8, -2.9), "-z", 4, 2, "ice", layers=2, share="knee")
    P.spike("leg", (cx, 8.0, -2.9), "-z", 5, 2, "ice", layers=2, share="shin")
    P.puff("leg", (cx, 1.8, -1.6), 2.6, "snow", share="toe")
    P.save()
    return P


def golem():
    P = piece("golem", 128, 128, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("body", "root", (0, 14, 0))
    P.bone("head", "body", (0, 30, 0))
    P.bone("arm_r", "body", (-10, 28, 0))
    P.bone("arm_l", "body", (10, 28, 0))
    P.bone("leg_r", "root", (-4.5, 14, 0))
    P.bone("leg_l", "root", (4.5, 14, 0))
    P.cube("body", (-7, 14, -5), (14, 16, 10), "snow", share="torso")
    P.cube("body", (-8, 26, -5.5), (16, 4, 11), "fur", share="yoke")
    P.cube("body", (-3, 18, -5.9), (6, 6, 1.4), "ice", share="plate")
    P.cube("body", (-1.6, 19.4, -6.6), (3.2, 3.2, 1), "core", share="heart")
    P.puff("body", (0, 14, 0), 6.0, "snow", share="belly")
    P.puff("head", (0, 34, 0), 5.2, "snow", share="hd")
    P.puff("head", (0, 38.4, 0), 2.6, "snow", share="hd2")
    P.cube("head", (-3.4, 34, -5.4), (2.2, 2.2, 1), "core", share="eye")
    P.cube("head", (1.2, 34, -5.4), (2.2, 2.2, 1), "core", share="eye")
    P.spike("head", (0, 38.8, 0), "+y", 7, 3, "ice", layers=3, share="horn")
    P.spike("head", (-3.4, 38, 0), "+y", 5, 2, "ice", layers=2, share="horn2")
    P.spike("head", (3.4, 38, 0), "+y", 5, 2, "ice", layers=2, share="horn2")
    for s, bn in ((-1, "arm_r"), (1, "arm_l")):
        P.puff(bn, (s * 11, 28, 0), 4.0, "snow", share="sh")
        P.cube(bn, (s * 11 - 2.5, 14, -2.5), (5, 13, 5), "snow", share="arm")
        P.puff(bn, (s * 11, 12, 0), 3.6, "snow", share="fist")
        P.spike(bn, (s * 11, 31, 0), "+y", 7, 3, "ice", layers=3, share="sp")
        P.spike(bn, (s * 11 + s * 3, 29, 0), "+x" if s > 0 else "-x", 6, 2, "ice", layers=2, share="sp2")
    for s, bn in ((-1, "leg_r"), (1, "leg_l")):
        P.cube(bn, (s * 4.5 - 3, 0, -3), (6, 14, 6), "snow", share="leg")
        P.puff(bn, (s * 4.5, 1.5, -1), 3.4, "snow", share="foot")
    a = P.anim
    a.clip("idle", 3.0, True)
    a.pos("idle", "body", L.sine(3.0, 0.6, 0.0))
    a.rot("idle", "arm_r", L.sine3(3.0, (4, 0, 3), 0.0))
    a.rot("idle", "arm_l", L.sine3(3.0, (4, 0, -3), 0.5))
    a.rot("idle", "head", L.sine3(3.0, (1.5, 3, 0), 0.3))
    a.clip("strike", 0.9, False)
    a.rot("strike", "arm_r", {0.0: (0, 0, 0), 0.3: (-150, 0, 15), 0.5: (30, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "arm_l", {0.0: (0, 0, 0), 0.3: (-150, 0, -15), 0.5: (30, 0, 0), 0.9: (0, 0, 0)})
    a.rot("strike", "body", {0.0: (0, 0, 0), 0.3: (-14, 0, 0), 0.5: (16, 0, 0), 0.9: (0, 0, 0)})
    a.clip("crumble", 1.2, False)
    a.pos("crumble", "root", {0.0: (0, 0, 0), 1.2: (0, -26, 0)})
    a.scale("crumble", "root", {0.0: (1, 1, 1), 1.2: (1.15, 0.5, 1.15)})
    a.rot("crumble", "head", {0.0: (0, 0, 0), 1.2: (35, 0, 20)})
    P.save()
    return P


def crystal():
    P = piece("crystal", 128, 128, 30)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("core", "root", (0, 10, 0))
    P.bone("ring", "root", (0, 4, 0))
    P.spike("core", (0, 0, 0), "+y", 26, 8, "ice", layers=5, share="main")
    P.cube("core", (-1.5, 8, -1.5), (3, 10, 3), "core", share="glow")
    for k in range(6):
        a = math.pi / 3 * k
        x, z = math.sin(a) * 6, math.cos(a) * 6
        P.cube("core", (x - 1.6, 0.5, z - 1.6), (3.2, 12 - (k % 2) * 3, 3.2), "ice", rot=(math.cos(a) * 18, 0, -math.sin(a) * 18), share="sat%d" % (k % 2))
    P.ring("ring", (0, 3, 0), 9.5, 1.8, 10, "snow", axis="y", depth=2.2, share="base")
    for k in range(6):
        a = math.pi / 3 * k + 0.4
        P.puff("ring", (math.sin(a) * 9.5, 1.6, math.cos(a) * 9.5), 2.2, "snow", share="drift")
    a = P.anim
    a.clip("idle", 4.0, True)
    a.rot("idle", "ring", L.spin(4.0, axis=1, steps=4))
    a.pos("idle", "core", L.sine(4.0, 0.8, 0.0))
    a.rot("idle", "core", L.sine3(4.0, (0, 6, 0), 0.0))
    a.clip("grow", 0.6, False)
    a.scale("grow", "root", {0.0: (0.1, 0.05, 0.1), 0.35: (1.1, 1.25, 1.1), 0.6: (1, 1, 1)})
    P.save()
    return P


def textures():
    L.particle_cloud("snow_puff", 201, 128, lobes=7, detail=0.4)

    def flake(d, S):                                                 # a six-armed snowflake with side branches and a hexagonal heart
        c = S / 2.0
        w = max(2, int(S * 0.025))
        for k in range(6):
            a = math.pi / 3 * k
            ex, ey = c + math.cos(a) * c * 0.92, c + math.sin(a) * c * 0.92
            d.line((c, c, ex, ey), fill=255, width=w)
            for fr, ln in ((0.38, 0.28), (0.62, 0.2), (0.8, 0.12)):
                bx, by = c + math.cos(a) * c * fr, c + math.sin(a) * c * fr
                for sgn in (-1, 1):
                    b = a + sgn * math.radians(55)
                    d.line((bx, by, bx + math.cos(b) * c * ln, by + math.sin(b) * c * ln), fill=255, width=max(2, w - 1))
        hex_ = [(c + math.cos(math.pi / 3 * k + 0.5236) * c * 0.16, c + math.sin(math.pi / 3 * k + 0.5236) * c * 0.16) for k in range(6)]
        d.polygon(hex_, fill=255)
    L.particle_shape("snow_flake", flake, 256, blur=0.7, glow=0.2)
    L.particle_streak("snow_streak", 64, 256, 203, core=0.08, wisp=0.5)
    L.particle_ring("snow_ring", 256, 0.85, 0.97, 0.05, ticks=24, seed=204, wobble=0.04)

    def glint(d, S):                                                 # a four pointed glint
        c = S / 2.0
        d.polygon([(c, 0), (c + S * 0.07, c - S * 0.07), (S, c), (c + S * 0.07, c + S * 0.07), (c, S), (c - S * 0.07, c + S * 0.07), (0, c), (c - S * 0.07, c - S * 0.07)], fill=255)
    L.particle_shape("snow_glint", glint, 64, blur=0.5, glow=0.3)

    def shard(d, S):                                                 # an ice shard: a long kite
        c = S / 2.0
        d.polygon([(c, S * 0.02), (c + S * 0.22, c * 0.9), (c + S * 0.06, S * 0.98), (c - S * 0.2, c * 1.1)], fill=255)
        d.line((c, S * 0.1, c + S * 0.02, S * 0.9), fill=140, width=max(1, S // 40))
    L.particle_shape("snow_shard", shard, 128, blur=0.6, glow=0.15)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), golem(), crystal()]
    textures()
    print("models:", " ".join(p.m.id for p in out))

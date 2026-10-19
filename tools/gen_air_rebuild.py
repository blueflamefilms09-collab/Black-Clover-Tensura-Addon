"""Air Magic rebuild (1.93.0): the temporary armour pieces, the summon models (air blade, pressure orb) and the VFX textures.

    python3 -B tools/gen_air_rebuild.py

The Air armour is made of compressed air: glassy, translucent plates (the texture has an alpha channel; the aura draws every piece translucent), pale
cyan with white streamlines, hard white edges and glowing compression cores.
  air_helm      a sleek helm with a tall blade fin and cheek vanes
  air_chest     thin layered plates, four compressed-air orbs on the chest
  air_belt      a ring of floating orbs and a fine band
  air_back      a halo of vanes behind the back, five orbs orbiting (clip idle), two pairs of blade vanes swept back
  air_pauld_r / _l   three blades fanned over the shoulder
  air_vamb_r / _l    two long vanes along the forearm and a pressure orb in the fist
  air_greave_r / _l  boots with vanes and a cone of rushing air under the sole
Summons (client/prop/AirPropPainter): air_blade (a crescent of compressed air, spin), air_orb (a pressure orb: core, two shell rings, six vanes; clip idle spins, swell grows).
Particles: air_ring, air_streak, air_swirl, air_orb, air_slash, air_glint.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import gen_air_rebuild_lib as L   # noqa: E402

KEY = "air"


def mats():
    return {
        "air": L.sheet_air(11, [(120, 196, 226), (160, 224, 244), (196, 242, 252), (226, 250, 255), (250, 255, 255)], alpha=(80, 190)),
        "pane": L.sheet_air(23, [(150, 212, 238), (186, 234, 250), (214, 247, 255), (238, 252, 255), (255, 255, 255)], alpha=(60, 140)),
        "edge": L.sheet_solid((236, 252, 255), glow=0.35, glow_col=(200, 250, 255), alpha=235, noise=0.05, seed=3),
        "core": L.sheet_solid((255, 255, 255), glow=1.0, glow_col=(190, 250, 255), alpha=245, noise=0.03, seed=4),
    }


def piece(name, w=64, h=64, seed=1):
    return L.Piece(KEY, name, w, h, seed=seed, mats=mats())


def helm():
    P = piece("helm", 64, 64, 1)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("head", "root", (0, 24, 0))
    P.cube("head", (-4.6, 30.0, -4.7), (9.2, 1.6, 9.4), "edge", inflate=0.1, share="band")
    P.cube("head", (-4.8, 31.6, -4.8), (9.6, 2.2, 9.6), "air", inflate=0.1, share="cap")
    for k, (h, z) in enumerate(((8, -2.5), (10, 0.0), (7, 2.5), (5, 4.5))):                # a tall blade fin along the crown
        P.cube("head", (-0.5, 33.8 + (0 if k else 0), z - 1), (1, h, 2.4), "pane", rot=(-12 * (k - 1), 0, 0), share="fin%d" % k)
    for s in (-1, 1):
        for k in range(2):
            P.cube("head", (s * 4.8 - 0.5, 27.4 - 2.2 * k, -2.5 + 2.6 * k), (1, 2.2, 6 - k * 2), "air", rot=(0, s * 12, 0), share="ch%d" % k)
    P.cube("head", (-1, 30.4, -5.2), (2, 1, 1), "core", share="gem")
    P.save()
    return P


def chest():
    P = piece("chest", 64, 64, 2)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.cube("torso", (-4.6, 15.0, -3.0), (9.2, 9, 1), "air", inflate=0.3, share="plate")
    P.cube("torso", (-5.0, 22.0, -2.8), (10, 1.6, 5.6), "edge", inflate=0.1, share="yoke")
    P.cube("torso", (-4.6, 14.4, 2.0), (9.2, 9.4, 1), "air", inflate=0.3, share="back")
    for k in range(3):
        P.cube("torso", (-4.2, 16.0 + 2.8 * k, -3.6), (8.4, 1.2, 0.8), "pane", share="ln")
    for k, (x, y) in enumerate(((-2.4, 20.4), (2.4, 20.4), (-2.4, 17.0), (2.4, 17.0))):
        P.puff("torso", (x, y, -4.2), 1.5, "core", share="orb")
    P.save()
    return P


def belt():
    P = piece("belt", 64, 64, 3)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("torso", "root", (0, 24, 0))
    P.bone("orbs", "torso", (0, 12.4, 0))
    P.cube("torso", (-4.5, 11.8, -2.8), (9, 1.4, 5.6), "edge", inflate=0.25, share="band")
    for k in range(5):
        a = 2 * math.pi * k / 5
        P.puff("orbs", (math.cos(a) * 6.8, 12.4 + (k % 2) * 1.2, math.sin(a) * 6.0), 1.7, "air", share="o")
        P.puff("orbs", (math.cos(a) * 6.8, 12.4 + (k % 2) * 1.2, math.sin(a) * 6.0), 0.8, "core", share="oc")
    P.anim.clip("idle", 3.0, True)
    P.anim.rot("idle", "orbs", L.spin(3.0, axis=1, steps=4))
    P.save()
    return P


def back():
    P = piece("back", 128, 128, 4)
    P.bone("root", pivot=(0, 24, 0))
    P.bone("halo", "root", (0, 24, 6))
    P.bone("orbs", "root", (0, 22, 5))
    P.ring("halo", (0, 24, 6.5), 11.5, 1.2, 16, "air", axis="z", depth=1.2, share="h")
    P.ring("halo", (0, 24, 6.5), 8.5, 1.0, 12, "pane", axis="z", depth=1.0, share="h2")
    for k in range(8):                                               # eight vanes between the two rings
        a = math.pi / 4 * k
        P.bar("halo", (math.cos(a) * 8.5, 24 + math.sin(a) * 8.5, 6.5), (math.cos(a) * 11.5, 24 + math.sin(a) * 11.5, 6.5), 1.2, "edge", share="vane")
    for k in range(5):
        a = 2 * math.pi * k / 5
        P.puff("orbs", (math.cos(a) * 12, 22 + 4 * math.sin(k * 1.9), 5 + math.sin(a) * 4), 1.8, "core", share="ob")
    for s in (-1, 1):                                                # two pairs of blade vanes swept back from the shoulder blades
        for k in range(3):
            P.cube("halo", (s * (3 + 3 * k) - 0.5, 20 - 3.4 * k, 4.4 + 1.5 * k), (1.4, 12 - 2 * k, 1), "pane", rot=(0, 0, s * (20 + 14 * k)), share="bv%d" % k)
    a = P.anim
    a.clip("idle", 5.0, True)
    a.rot("idle", "halo", L.spin(10.0, axis=2, steps=8))
    a.rot("idle", "orbs", L.spin(3.0, axis=1, steps=4))
    P.save()
    return P


def pauld(side):
    nm = "pauld_r" if side < 0 else "pauld_l"
    P = piece(nm, 64, 64, 5 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    sx = side * 6.2
    for k in range(3):                                               # three blades fanned over the shoulder
        P.cube("arm", (sx - 0.7 + side * 0.8 * k, 22.2, -4 + 2.6 * k), (1.4, 5, 5.5 - k), "air", rot=(0, 0, side * (-18 - 22 * k)), share="bl%d" % k)
    P.cube("arm", (sx - 3.2, 22.0, -2.6), (6.4, 1.0, 5.2), "edge", share="pl")
    P.puff("arm", (sx, 24.4, 0), 1.6, "core", share="pc")
    P.save()
    return P


def vamb(side):
    nm = "vamb_r" if side < 0 else "vamb_l"
    P = piece(nm, 64, 64, 7 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("arm", "root", (side * 5, 22, 0))
    cx = side * 5.7
    P.cube("arm", (cx - 2.5, 14.8, -2.5), (5, 5, 5), "air", inflate=0.5, share="cuff")
    for k in range(2):
        P.cube("arm", (cx + side * (2.6 + 0.4 * k) - 0.5, 14.2, -3 + 4 * k), (1, 8, 1.6), "pane", rot=(0, 0, side * (-8)), share="vn")
    P.puff("arm", (cx, 12.0, 0), 2.6, "air", share="fist")
    P.puff("arm", (cx, 12.0, 0), 1.2, "core", share="fc")
    P.save()
    return P


def greave(side):
    nm = "greave_r" if side < 0 else "greave_l"
    P = piece(nm, 64, 64, 9 + (side > 0))
    P.bone("root", pivot=(0, 24, 0))
    P.bone("leg", "root", (side * 1.9, 12, 0))
    cx = side * 1.9
    P.cube("leg", (cx - 2.4, 2.0, -2.5), (4.8, 7, 5), "air", inflate=0.4, share="boot")
    P.cube("leg", (cx - 2.5, 8.6, -2.6), (5, 1, 5.2), "edge", inflate=0.3, share="rim")
    for k in range(2):
        P.cube("leg", (cx + side * 2.8 - 0.5, 3.0 + 2.4 * k, -1 + 2.2 * k), (1, 5, 3), "pane", rot=(0, 0, side * (-14)), share="gv")
    P.spike("leg", (cx, 2.0, 0), "-y", 3, 4, "pane", layers=2, share="jet")                  # rushing air under the sole
    P.save()
    return P


def blade():
    P = piece("blade", 64, 64, 20)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("spin", "root", (0, 8, 0))
    n = 11
    for k in range(n):                                               # a crescent: cubes along an arc, thick in the middle, thin at the horns
        t = k / float(n - 1)
        ang = math.radians(-70 + 140 * t)
        x, z = math.sin(ang) * 12, -math.cos(ang) * 12 + 12
        w = 1.4 + 3.2 * math.sin(math.pi * t)
        P.cube("spin", (x - w / 2.0, 7.5, z - 2.2 - 6), (w, 1.4, 4.4), "air", rot=(0, -math.degrees(ang), 0), share="c%d" % k)
    P.cube("spin", (-0.8, 7.6, -6.2 + 0.6), (1.6, 1.0, 10), "core", share="edge")
    P.puff("spin", (0, 8, 0), 1.6, "core", share="hub")
    a = P.anim
    a.clip("idle", 1.0, True)
    a.rot("idle", "spin", L.spin(0.6, axis=1, steps=4))
    P.save()
    return P


def orb():
    P = piece("orb", 64, 64, 30)
    P.bone("root", pivot=(0, 0, 0))
    P.bone("heart", "root", (0, 12, 0))
    P.bone("shell", "heart", (0, 12, 0))
    P.bone("vanes", "heart", (0, 12, 0))
    P.puff("heart", (0, 12, 0), 3.4, "core", share="c")
    P.puff("heart", (0, 12, 0), 5.4, "air", share="s")
    P.ring("shell", (0, 12, 0), 7.6, 1.0, 12, "pane", axis="y", depth=1.2, share="r1")
    P.ring("shell", (0, 12, 0), 7.6, 1.0, 12, "pane", axis="x", depth=1.2, share="r2")
    for k in range(6):
        a = math.pi / 3 * k
        P.cube("vanes", (math.cos(a) * 9.5 - 0.5, 9.5, math.sin(a) * 9.5 - 2), (1, 5, 4), "edge", rot=(0, -math.degrees(a), 0), share="v")
    a = P.anim
    a.clip("idle", 2.0, True)
    a.rot("idle", "shell", L.spin(1.6, axis=1, steps=4))
    a.rot("idle", "vanes", {t: tuple(-v for v in vv) for t, vv in L.spin(1.1, axis=1, steps=4).items()})
    a.scale("idle", "heart", L.loop(2.0, lambda p: (1 + 0.06 * math.sin(p * 6.283),) * 3))
    a.clip("swell", 1.0, False)
    a.scale("swell", "heart", {0.0: (0.6, 0.6, 0.6), 1.0: (1.5, 1.5, 1.5)})
    a.rot("swell", "shell", L.spin(1.0, axis=1, steps=4, turns=3))
    P.save()
    return P


def textures():
    L.particle_ring("air_ring", 256, 0.86, 0.97, 0.05, ticks=0, seed=504, wobble=0.02)
    L.particle_streak("air_streak", 64, 256, 503, core=0.05, wisp=0.3)

    def swirl(d, S):                                                 # a loose double swirl of thin wind lines
        c = S / 2.0
        for arm in range(2):
            pts = []
            for i in range(70):
                t = i / 69.0
                ang = arm * math.pi + t * 5.0
                pts.append((c + math.cos(ang) * c * (0.08 + 0.86 * t), c + math.sin(ang) * c * (0.08 + 0.86 * t)))
            for i in range(len(pts) - 1):
                d.line((pts[i], pts[i + 1]), fill=255 if i > 6 else 160, width=max(2, int(S * 0.02 * (1 + i / 69.0))))
    L.particle_shape("air_swirl", swirl, 256, blur=0.9, glow=0.1)
    L.particle_soft("air_orb", 128, 1.6)

    def slash(d, S):                                                 # a thin crescent of cut air
        outer = [(S * 0.04 + S * 0.92 * i / 40.0, S * 0.7 - S * 0.56 * math.sin(math.pi * i / 40.0)) for i in range(41)]
        inner = [(S * 0.04 + S * 0.92 * i / 40.0, S * 0.7 - S * 0.56 * math.sin(math.pi * i / 40.0) * 0.8 + S * 0.01) for i in range(40, -1, -1)]
        d.polygon(outer + inner, fill=255)
    L.particle_shape("air_slash", slash, 256, blur=0.7, glow=0.15)

    def glint(d, S):
        c = S / 2.0
        d.polygon([(c, 0), (c + S * 0.05, c - S * 0.05), (S, c), (c + S * 0.05, c + S * 0.05), (c, S), (c - S * 0.05, c + S * 0.05), (0, c), (c - S * 0.05, c - S * 0.05)], fill=255)
    L.particle_shape("air_glint", glint, 64, blur=0.5, glow=0.3)


if __name__ == "__main__":
    out = [helm(), chest(), belt(), back(), pauld(-1), pauld(1), vamb(-1), vamb(1), greave(-1), greave(1), blade(), orb()]
    textures()
    print("models:", " ".join(p.m.id for p in out))

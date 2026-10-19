"""Mirror Magic geo models (1.93.0 rebuild): the Glass Garb (the temporary armour), the standing mirrors and the mirror blade the spells put in the world.

    python3 -B tools/gen_mirror_geo.py            # writes every model, animation and texture, runs geo_check on all of them

Armour (Space.PLAYER, drawn by client/aura/MirrorAura.java through SpatialArmourKit and EquipAnimation; the Real Doubles wear the same pieces, MirrorDoubleGarbLayer):
  helm   (head)   a crown of five tall mirror panes fanned on a violet-silver band, a small oval mirror on the brow, shard ears
  chest  (body)   a frame cuirass with an ornate oval mirror breastplate, two shoulder panes standing at the collar, a gem at the throat
  arm_r / arm_l   a large angled pane on a pauldron cap, two framed panes on the forearm, a small framed gauntlet
  leg_r / leg_l   a framed shin pane, a round knee mirror, a framed boot
  back   (body)   a fan of seven floating mirror panes of different shapes behind the back, swinging, each bobbing on its own beat; clip idle
  halo   (head)   a tilted ring of eight small shards circling the head; clip idle
Things the spells make:
  oval / round / arch / diamond   a standing ornate mirror (Reflect Refrain, Full Reflection, Mirror Step, Real Double): a frame of segments with gems and a crest, a
                                  translucent glass pane, shard chips drifting round it; clips idle (the glass shimmers) and open (it unfolds from a line)
  blade   the blade-edged mirror (Mirrors Slash, Mirrors Meteorite): a kite of glass with a sharp white edge, a point and a small grip; clips idle, spin
The look: silver-white glass (translucent, so the pieces are drawn on the translucent layer) in violet-silver frames with rose gems; the glow map is the frame edges, the gems
and a bright rim round every pane.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from spatial_geo_kit import Atlas, MATS, mat, loop, spin, mirror_x, mrot, check   # noqa: E402

KEY = "mirror"

mat("glass", "glass", [(70, 76, 112), (140, 152, 196), (206, 218, 246), (240, 246, 255), (255, 255, 255)], glow=(200, 214, 255), edge=170, alpha=105)
mat("frame", "metal", [(34, 24, 64), (84, 64, 134), (146, 126, 198), (208, 194, 240), (250, 244, 255)], glow=(190, 150, 255), edge=100)
mat("gem", "energy", [(120, 40, 120), (200, 80, 190), (255, 140, 230), (255, 200, 245), (255, 255, 255)], glow=(255, 160, 235), core=255)
mat("edge", "energy", [(150, 160, 210), (200, 212, 250), (235, 242, 255), (250, 252, 255), (255, 255, 255)], glow=(230, 238, 255), core=255)
mat("gold", "metal", [(70, 48, 18), (136, 98, 38), (210, 166, 72), (250, 216, 128), (255, 248, 204)], glow=(255, 208, 120), edge=100)


def ellipse_frame(A, bone, cx, cy, z, rx, ry, thick, depth, material, n=14, share=None, arc=(0, 360)):
    """A frame on an ellipse (in the xy plane facing -z): n segments, each turned to the local normal."""
    a0, a1 = arc
    for k in range(n):
        a = math.radians(a0 + (a1 - a0) * (k + 0.5) / n)
        x, y = rx * math.cos(a), ry * math.sin(a)
        da = math.radians((a1 - a0) / n)
        L = 2.0 * math.hypot(rx * math.sin(a), ry * math.cos(a)) * math.sin(da / 2) + 1.0
        phi = math.degrees(math.atan2(math.sin(a) / ry, math.cos(a) / rx))
        A.box_c(bone, (cx + x, cy + y, z), (thick, L, depth), material, rot=(0, 0, phi), share=(share + str(k % 2)) if share else None)


def glass_rows(A, bone, cx, cy, z, hw_fn, h, material="glass", step=2, share=None):
    """The glass of a mirror as horizontal slabs: hw_fn(y offset) gives the half width at that height (0 = nothing)."""
    y = -h
    while y < h:
        w = hw_fn((y + step / 2.0))
        if w > 0.5:
            A.cube(bone, (cx - w, cy + y, z), (2 * w, step, 1), material, share=(share + "%d" % int(round(w))) if share else None)
        y += step


# ====================================================================================================================
def helm():
    A = Atlas(KEY, "helm", 128, 128, seed=71)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 30, -4), (8, 2, 8), "frame", inflate=0.6)
    for k, (x, h, ang) in enumerate(((-3.4, 7, 22), (-1.7, 9, 11), (0, 11, 0), (1.7, 9, -11), (3.4, 7, -22))):
        c("root", (x - 0.6 - (0 if k != 2 else 0.2), 32, 0.4 - 1.2 * (k == 2)), (1.4 if k != 2 else 2, h, 1), "glass", rot=(0, 0, ang), share="cp%d" % k)
        c("root", (x - 0.4, 32 + h - 0.6, 0.4 - 1.2 * (k == 2)), (1, 1, 1), "edge", rot=(0, 0, ang), share="ct")
    ellipse_frame(A, "root", 0, 29.5, -4.8, 1.8, 2.3, 0.8, 1, "frame", n=8, share="bro")
    c("root", (-1, 28.6, -4.6), (2, 2, 1), "glass")
    c("root", (-0.5, 31.0, -5.0), (1, 1, 1), "gem")
    for s in (-1, 1):
        A.spike("root", (s * 5.2, 28.5, -0.5), "+y", 5, 2, "glass", layers=3, share="ear")
        c("root", (s * 4.9 - 0.5, 26, -1), (1, 4, 3), "frame")
    c("root", (-4, 24.5, 4), (8, 7, 1), "frame", inflate=0.4)
    return A.save()


def chest():
    A = Atlas(KEY, "chest", 128, 128, seed=72)
    A.bone("root", pivot=(0, 24, 0))
    c = A.cube
    c("root", (-4, 12, -2), (8, 12, 4), "frame", inflate=0.7)
    c("root", (-4, 22, -2), (8, 2, 4), "gold", inflate=1.0)
    c("root", (-4, 12, -2), (8, 2, 4), "gold", inflate=1.0)
    ellipse_frame(A, "root", 0, 18, -3.4, 3.2, 4.4, 1.0, 1, "gold", n=16, share="bp")
    glass_rows(A, "root", 0, 18, -3.2, lambda y: 3.0 * math.sqrt(max(0.0, 1 - (y / 4.0) ** 2)), 4.0, share="bg")
    c("root", (-0.5, 21.6, -3.9), (1, 1, 1), "gem")
    c("root", (-0.5, 14.4, -3.9), (1, 1, 1), "gem")
    for s in (-1, 1):                                                                     # shoulder panes standing at the collar
        c("root", (s * 5.6 - 0.5, 23, -2), (1, 7, 4), "glass", rot=(0, 0, -s * 22), share="sp")
        c("root", (s * 5.6 - 0.5, 22.4, -2.4), (1, 1, 5), "frame", rot=(0, 0, -s * 22), share="spf")
    c("root", (-1, 22.8, -3.2), (2, 2, 1), "gem")
    for k in (-2, 2):
        c("root", (k - 0.5, 14, 3.0), (1, 7, 1), "gold")
    return A.save()


def arm(side, tag):
    A = Atlas(KEY, "arm_" + tag, 128, 128, seed=73 + (side > 0))
    A.bone("root", pivot=(side * 5, 22, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    c("root", (o(-9.4, 6), 22.2, -3.2), (6, 2, 6), "frame", inflate=0.2)
    c("root", (o(-10.6, 1), 18.5, -3.2), (1, 8, 6), "glass", rot=(0, 0, side * 16), share="pp")               # the big angled pane
    c("root", (o(-10.8, 1), 17.7, -3.4), (1, 1, 7), "frame", rot=(0, 0, side * 16), share="ppf")
    c("root", (o(-8.6, 5), 14, -2.5), (5, 5, 5), "frame", inflate=0.3)
    c("root", (o(-8.6, 5), 14.2, -3.5), (5, 5, 1), "glass", share="vf")                                       # framed panes front and back of the forearm
    c("root", (o(-8.6, 5), 14.2, 2.5), (5, 5, 1), "glass", share="vf")
    c("root", (o(-8.9, 1), 13.6, -3.2), (1, 1, 6), "gold", share="vg")
    c("root", (o(-8.6, 5), 12, -2.5), (5, 2, 5), "gold", inflate=0.4)
    c("root", (o(-6.5, 1), 12.2, -3.2), (1, 1, 1), "gem")
    return A.save()


def leg(side, tag):
    A = Atlas(KEY, "leg_" + tag, 128, 128, seed=75 + (side > 0))
    A.bone("root", pivot=(side * 2, 12, 0))
    c = A.cube

    def o(x0, w):
        return mirror_x((x0, 0, 0), (w, 0, 0), side)[0]
    c("root", (o(-4.4, 5), 0, -3.4), (5, 3, 6), "frame", inflate=0.3)
    c("root", (o(-4.4, 5), 3, -2.5), (5, 6, 5), "frame", inflate=0.3)
    c("root", (o(-3.6, 4), 3.4, -3.4), (4, 5.4, 1), "glass", share="sh")
    ellipse_frame(A, "root", side * 2, 9.4, -3.3, 2.2, 2.2, 0.8, 1, "gold", n=10, share="kn" + tag)
    c("root", (o(-3.0, 2), 8.4, -3.2), (2, 2, 1), "glass", share="kg")
    c("root", (o(-4.5, 5), 10.8, -2.6), (5, 1, 5), "gold", inflate=0.5)
    return A.save()


def back():
    A = Atlas(KEY, "back", 128, 128, seed=77)
    A.bone("root", pivot=(0, 19, 0))
    A.bone("fan", "root", (0, 19, 6))
    A.cube("fan", (-3, 22.6, 3.6), (6, 2, 2), "frame", inflate=0.3)
    shapes = ((6, 12), (5, 14), (7, 10), (5, 16), (7, 10), (5, 14), (6, 12))
    for k, (w, h) in enumerate(shapes):
        ang = -66 + k * 22.0
        a = math.radians(ang)
        x, y = 11.5 * math.sin(a), 19 + 11.5 * math.cos(a) - 6
        A.bone("pane%d" % k, "fan", (round(x, 1), round(y, 1), 6))
        A.cube("pane%d" % k, (round(x - w / 2.0, 1), round(y - h / 2.0, 1), 6), (w, h, 1), "glass", rot=(0, 0, -ang), share="pg%d" % k)
        A.cube("pane%d" % k, (round(x - w / 2.0 - 0.6, 1), round(y - h / 2.0 - 0.6, 1), 5.6), (w + 1.2, 1, 1.6), "frame", rot=(0, 0, -ang), share="pf%d" % k)
        A.cube("pane%d" % k, (round(x - w / 2.0 - 0.6, 1), round(y + h / 2.0 - 0.4, 1), 5.6), (w + 1.2, 1, 1.6), "frame", rot=(0, 0, -ang), share="pf%d" % k)
        A.cube("pane%d" % k, (round(x - 0.5, 1), round(y + h / 2.0 + 0.4, 1), 6), (1, 1, 1), "gem", rot=(0, 0, -ang), share="pgm")
    a = A.anim
    a.clip("idle", length=8.0, loop=True)
    a.rot("idle", "fan", loop(8.0, lambda p: (0, 0, 7 * math.sin(p * 2 * math.pi))))
    for k in range(7):
        a.rot("idle", "pane%d" % k, loop(8.0, lambda p, k=k: (6 * math.sin((p + k / 7.0) * 2 * math.pi), 8 * math.sin((p * 2 + k / 7.0) * 2 * math.pi), 0)))
        a.pos("idle", "pane%d" % k, loop(8.0, lambda p, k=k: (0, 0.8 * math.sin((p + k / 7.0) * 2 * math.pi), 0.6 * math.sin((p + k / 3.0) * 2 * math.pi))))
    return A.save()


def halo():
    A = Atlas(KEY, "halo", 128, 128, seed=78)
    A.bone("root", pivot=(0, 36, 0))
    A.bone("tilt", "root", (0, 36, 0), rotation=(12, 0, 6))
    A.bone("ring", "tilt", (0, 36, 0))
    n = 8
    for k in range(n):
        a = 360.0 * k / n
        x, z = 6.4 * math.cos(math.radians(a)), -6.4 * math.sin(math.radians(a))
        A.cube("ring", (round(x - 1, 1), 34.6, round(z - 0.5, 1)), (2, 4, 1), "glass", rot=(0, a - 90, 12), share="hs")
        A.cube("ring", (round(x - 0.5, 1), 38.4, round(z - 0.5, 1)), (1, 1, 1), "edge", share="hsd")
    A.ring("ring", (0, 36, 0), "y", 6.4, 1, 1, "frame", n=12, share="hr")
    a = A.anim
    a.clip("idle", length=7.0, loop=True)
    a.rot("idle", "ring", spin(7.0, 1, 1, 1))
    a.pos("idle", "root", loop(7.0, lambda p: (0, 0.6 * math.sin(p * 2 * math.pi), 0)))
    return A.save()


# ====================================================================================================================
def standing(name, kind):
    """A standing ornate mirror: kind oval / round / arch / diamond, about 44 px tall, centred on y = 24, the front on -z."""
    A = Atlas(KEY, name, 128, 128, seed=80 + len(name))
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 24, 0))
    c = A.cube
    cx, cy = 0, 24
    if kind == "oval":
        rx, ry = 13, 21
        ellipse_frame(A, "body", cx, cy, 0, rx + 1, ry + 1, 3, 3, "frame", n=22, share="of")
        glass_rows(A, "body", cx, cy, 0.4, lambda y: rx * math.sqrt(max(0.0, 1 - (y / float(ry)) ** 2)), ry, share="og")
        top = cy + ry + 2
    elif kind == "round":
        rx = ry = 18
        ellipse_frame(A, "body", cx, cy, 0, rx + 1, ry + 1, 3, 3, "frame", n=22, share="rf")
        glass_rows(A, "body", cx, cy, 0.4, lambda y: rx * math.sqrt(max(0.0, 1 - (y / float(ry)) ** 2)), ry, share="rg")
        top = cy + ry + 2
    elif kind == "arch":
        w, h = 13, 20
        c("body", (-w - 2, cy - h - 2, -1.5), (3, h + 4, 3), "frame", share="ab")
        c("body", (w - 1, cy - h - 2, -1.5), (3, h + 4, 3), "frame", share="ab")
        c("body", (-w - 2, cy - h - 3, -1.5), (2 * w + 4, 3, 3), "frame", share="ac")
        ellipse_frame(A, "body", 0, cy + h - 1, 0, w + 0.5, 11, 3, 3, "frame", n=12, share="at", arc=(0, 180))
        glass_rows(A, "body", 0, cy, 0.4, lambda y: (w if y < h - 1 else w * math.sqrt(max(0.0, 1 - ((y - h + 1) / 11.0) ** 2))), h + 10, step=2, share="ag")
        top = cy + h + 11
    else:  # diamond
        ry = 22
        for s in (-1, 1):
            for t in (-1, 1):
                A.box_c("body", (s * 7.5, cy + t * 11, 0), (3, 26, 3), "frame", rot=(0, 0, s * t * -27), share="df")
        glass_rows(A, "body", 0, cy, 0.4, lambda y: max(0.0, 13 * (1 - abs(y) / float(ry))), ry, share="dg")
        top = cy + ry + 2
    # the crest, the gems, the foot
    c("body", (-2, top - 1, -1.2), (4, 3, 2.4), "gold", share="cr")
    c("body", (-1, top + 2, -1), (2, 3, 2), "gold", share="cr2")
    c("body", (-1, top - 0.6, -1.6), (2, 2, 1), "gem")
    for s in (-1, 1):
        c("body", (s * (rx_of(kind) + 1) - 1, cy - 1, -1.8), (2, 2, 1), "gem", share="sg")
    c("body", (-1, cy - 21 - 4, -1.2), (2, 3, 2.4), "gold", share="ft")
    A.bone("chips", "root", (0, 24, 0))
    for k in range(5):
        a = k * 72.0
        x, z = 24 * math.cos(math.radians(a)), 24 * math.sin(math.radians(a))
        A.bone("chip%d" % k, "chips", (round(x, 1), 22 + (k % 3) * 5, round(z, 1)))
        c("chip%d" % k, (round(x - 1, 1), 22 + (k % 3) * 5, round(z - 0.5, 1)), (2, 4, 1), "glass", rot=(15, a, 20), share="chip")
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.scale("idle", "body", loop(4.0, lambda p: (1, 1 + 0.015 * math.sin(p * 4 * math.pi), 1)))
    a.rot("idle", "chips", spin(4.0, 1, 1, 1))
    a.clip("open", length=0.5, loop=False)
    a.scale("open", "body", {0.0: (0.04, 1, 1), 0.5: (1, 1, 1)})
    return A.save()


def rx_of(kind):
    return {"oval": 13, "round": 18, "arch": 13, "diamond": 13}[kind]


def blade():
    A = Atlas(KEY, "blade", 128, 128, seed=91)
    A.bone("root", pivot=(0, 16, 0))
    A.bone("spin", "root", (0, 16, 0))
    c = A.cube
    rows = ((0, 2), (3, 4), (6, 5), (9, 6), (12, 7), (15, 7), (18, 6), (21, 5), (24, 4), (27, 3), (30, 2))     # a kite of glass along -z
    for k, (z, w) in enumerate(rows):
        c("spin", (-0.5, 16 - w, -z - 3), (1, 2 * w, 3), "glass", share="bg%d" % k)
        c("spin", (-0.9, 16 - w - 0.4, -z - 3), (1.8, 1, 3), "edge", share="be%d" % k)
        c("spin", (-0.9, 16 + w - 0.6, -z - 3), (1.8, 1, 3), "edge", share="be%d" % k)
    A.spike("spin", (0, 16, -33), "-z", 6, 2, "edge", layers=3, share="bp")
    c("spin", (-1, 14, 0), (2, 4, 6), "frame")
    c("spin", (-1.5, 13, 5), (3, 6, 2), "gold")
    c("spin", (-0.5, 15.2, -2), (1, 1, 1), "gem")
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    a.rot("idle", "spin", loop(2.0, lambda p: (0, 0, 8 * math.sin(p * 2 * math.pi))))
    a.clip("spin", length=0.6, loop=True)
    a.rot("spin", "spin", {0.0: (0, 0, 0), 0.15: (0, 0, 90), 0.3: (0, 0, 180), 0.45: (0, 0, 270), 0.6: (0, 0, 360)})
    return A.save()


if __name__ == "__main__":
    ids = [helm(), chest(), arm(-1, "r"), arm(1, "l"), leg(-1, "r"), leg(1, "l"), back(), halo(),
           standing("oval", "oval"), standing("round", "round"), standing("arch", "arch"), standing("diamond", "diamond"), blade()]
    check(*ids)

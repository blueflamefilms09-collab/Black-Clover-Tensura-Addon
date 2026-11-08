#!/usr/bin/env python3
"""Per-will Minecraft-detailed form builders. build(id) -> (Scene). Pass 1: the lion."""
import json, math, os, sys
import numpy as np
sys.dont_write_bytecode = True
from forms_lib import *
from forms_cube import *

HERE = os.path.dirname(os.path.abspath(__file__))
DATA = {w["id"]: w for w in json.load(open(os.path.join(HERE, "beastwill_remake_v3.json")))}


def cols(wid):
    c = DATA[wid]["spirit"]["colors"]
    return hx(c[0]), hx(c[1]), hx(c[2])


def ring_tufts(sc, part, mat_a, mat_b, cy, r, drop, out, n, a0, a1, w=2.6, t=1.8, ry=None, zr=None, seed=0, face_gap=None):
    """a ring of angled fur tufts round (0, cy, 0): angle 0 = the front (-z); a0..a1 degrees round the back"""
    rng = np.random.default_rng(seed)
    for i in range(n):
        a = math.radians(a0 + (a1 - a0) * i / max(1, n - 1))
        d = V(math.sin(a), 0, -math.cos(a))
        rr = r * (1 + rng.uniform(-.06, .06))
        base = V(0, cy + rng.uniform(-.5, .5), 0) + d * V(rr, 1, rr * (zr or 1))
        tip = base + d * out * rng.uniform(.8, 1.2) + V(0, -drop * rng.uniform(.75, 1.25), 0)
        sc.seg(part, base, tip, w * rng.uniform(.85, 1.15), t, mat_a if i % 2 == 0 else mat_b, up=(0, 1, 0), pad=.6, dt=int(rng.integers(-1, 2)))


def lion():
    M, A1, A2 = cols("lion_will")
    sh = Sheet(256, 256); sc = Scene(sh)
    gold = Mat("gold", M * .86, "fur", dark=M * .30, light=mixc(M, [255, 236, 170], .45), seed=3)
    back = Mat("back", M * .58, "fur", dark=M * .2, light=M * .8, seed=13)
    cream = Mat("cream", "D8C48C", "fur", dark="6E5A30", light="F4E8BC", seed=23)
    mane = Mat("mane", "92301A", "fur", dark="3A0E08", light="D4602C", seed=4)
    mane2 = Mat("mane2", "6E1E12", "fur", dark="2A0806", light="B04424", seed=14)
    plate = Mat("plate", "C9962A", "plate", dark="4E3206", light="F6D676", glow="FFC866", glow_amt=.3, seed=5)
    leather = Mat("leather", "4A2A18", "cloth", dark="1E0F08", light="8A5A34", seed=6)
    cloth = Mat("cloth", "A82418", "cloth", dark="4A0C08", light="E8503A", seed=7)
    claw = Mat("claw", "F2EAD6", "bone", dark="A09880", light="FFFFFF", seed=8)
    dark = Mat("darkn", "2A1810", "skin", dark="100806", light="5A3A28", seed=9)
    eye = Mat("eye", "FFE890", "energy", dark="E8B020", light="FFFFFF", glow="FFE070", glow_amt=1.0, seed=10)
    # =============================================================== head
    H = V(0, 28, 0)
    sc.cube("head", H, (9.2, 9.0, 9.0), gold)                                   # skull
    sc.cube("head", H + V(0, 4.5, 0), (8.2, 1.2, 8.2), back, dt=-1)              # brow-to-crown shading
    sc.cube("head", H + V(0, 0.2, -4.7), (8.8, 1.4, 1.2), back, dt=-1)           # heavy brow
    sc.cube("head", V(0, 26.2, -5.9), (4.8, 3.4, 4.0), cream)                    # muzzle
    sc.cube("head", V(0, 26.9, -5.9), (3.6, 1.4, 4.1), gold, dt=1)               # nose bridge
    sc.cube("head", V(0, 27.4, -8.0), (2.4, 1.4, .8), dark)                      # nose
    sc.cube("head", V(0, 24.7, -5.4), (3.8, 1.2, 3.6), cream, dt=-1)             # lower jaw
    for sx in (-1, 1):
        sc.cube("head", V(sx * 1.35, 25.2, -7.6), (.8, 1.8, .7), claw)           # fangs
        sc.cube("head", V(sx * 1.2, 24.5, -7.0), (.7, 1.2, .7), claw)
        sc.cube("head", V(sx * 2.45, 28.5, -4.7), (2.0, .85, .5), eye)            # slit eyes
        sc.cube("head", V(sx * 2.45, 29.2, -4.8), (2.6, .5, .5), dark)            # brow line over the eye
        sc.cube("head", V(sx * 3.9, 33.0, 0.2), (2.6, 2.4, 1.6), gold)            # round ears
        sc.cube("head", V(sx * 3.9, 33.0, -.7), (1.6, 1.4, .5), dark)
        sc.cube("head", V(sx * 3.9, 33.0, 0.9), (2.0, 1.8, .6), mane2)
    # mane: two rings of shaggy tufts + a drape over the shoulders and the chest
    ring_tufts(sc, "head", mane, mane2, 31.2, 5.0, 3.6, 1.6, 15, 52, 308, w=3.0, t=2.0, seed=1)
    ring_tufts(sc, "head", mane2, mane, 28.6, 5.4, 4.2, 2.1, 17, 40, 320, w=3.2, t=2.2, seed=2)
    ring_tufts(sc, "head", mane, mane2, 26.4, 5.6, 3.0, 2.2, 15, 50, 310, w=3.4, t=2.4, seed=3)
    for i, a in enumerate(range(-4, 5)):                                          # crown tufts standing up
        sc.seg("head", V(a * .95, 32.6, 1.6), V(a * 1.2, 35.0 - abs(a) * .25, 2.2), 1.5, 1.4, mane if i % 2 else mane2, pad=.4)
    # =============================================================== body
    sc.cube("body", V(0, 18, 0), (9.0, 12.2, 4.8), gold)
    sc.cube("body", V(0, 17.2, -2.5), (6.4, 9.0, .9), cream)                      # chest and belly fur
    for i in range(3): sc.cube("body", V(0, 20.2 - i * 2.4, -3.0), (5.6 - i * .5, 1.7, .7), cream, dt=1 if i % 2 else 0)
    sc.cube("body", V(0, 18.4, 2.55), (8.2, 10.6, .8), back)
    sc.cube("body", V(0, 24.2, 0), (9.6, 1.6, 5.4), plate)                         # gorget
    sc.cube("body", V(0, 25.0, -.2), (8.4, 1.0, 4.6), plate, dt=1)
    sc.cube("body", V(0, 19.8, -3.3), (2.4, 2.4, .8), plate, rot=(0, 0, 45))      # the lion sun
    sc.cube("body", V(0, 19.8, -3.3), (1.0, 1.0, 1.0), eye)
    for k in range(8):
        a = math.radians(k * 45); sc.cube("body", V(math.sin(a) * 2.5, 19.8 + math.cos(a) * 2.5, -3.2), (.6, 1.4, .6), plate, rot=(0, 0, -k * 45), dt=1)
    sc.cube("body", V(0, 12.6, 0), (9.6, 1.8, 5.2), leather)                       # belt
    sc.cube("body", V(0, 12.6, -2.8), (2.6, 2.0, .7), plate)
    sc.cube("body", V(0, 12.6, -3.2), (1.0, 1.0, .5), eye)
    for i, x in enumerate((-3.4, -1.1, 1.1, 3.4)):                                  # loincloth
        sc.cube("body", V(x, 9.6 - (i % 2) * .6, -2.75), (2.1, 4.4 - (i % 2) * 1.2, .8), cloth, dt=(i % 2) - 0)
    sc.cube("body", V(0, 9.6, 2.7), (5.6, 4.6, .8), cloth, dt=-1)
    # =============================================================== arms (right = -x). shoulder pivot (6, 22)
    def arm(side, pn):
        x = side * 6.0
        sc.cube(pn, V(x, 22.4, 0), (6.8, 2.2, 6.6), plate)                                              # pauldron
        sc.cube(pn, V(x, 21.0, 0), (6.2, 1.2, 6.0), leather)
        sc.cube(pn, V(x, 23.8, 0), (5.6, 1.4, 5.4), plate, dt=1)
        sc.seg(pn, V(x + side * 1.0, 24.4, 0), V(x + side * 2.8, 27.6, 0.2), 1.8, 1.8, plate, dt=1, pad=.3)
        sc.cube(pn, V(x + side * 0.5, 24.7, -2.4), (1.2, 1.2, .6), eye, rot=(0, 0, 45))
        for k in range(3): sc.seg(pn, V(x + side * (2.2 + k * .2), 22.2 - k * .5, -1.6 + k * 1.6), V(x + side * (3.6 + k * .2), 21.2 - k * .5, -1.6 + k * 1.6), 1.0, 1.4, plate, pad=.3)
        sc.cube(pn, V(x, 18.0, 0), (5.0, 6.8, 5.0), gold)                                            # upper arm fur
        sc.cube(pn, V(x, 18.0, -2.55), (3.6, 5.8, .6), cream)
        sc.cube(pn, V(x, 12.9, 0), (5.2, 4.6, 5.2), plate)                                             # bracer
        sc.cube(pn, V(x, 14.5, 0), (5.6, .9, 5.6), plate, dt=1)
        sc.cube(pn, V(x, 11.3, 0), (5.6, .9, 5.6), leather)
        sc.cube(pn, V(x, 9.5, -.2), (4.8, 3.4, 4.8), gold)                                             # the paw
        for k, zz in enumerate((-1.5, 0, 1.5)):                                                        # claws curving down and forward
            sc.seg(pn, V(x + (k - 1) * 1.3, 8.3, -2.2), V(x + (k - 1) * 1.5, 6.2, -3.2 - (k == 1) * .4), .8, .9, claw, pad=.3)
            sc.cube(pn, V(x + (k - 1) * 1.55, 5.7, -3.5 - (k == 1) * .4), (.6, .8, .6), claw)
    arm(1, "armL"); arm(-1, "armR")
    # =============================================================== legs
    def leg(side, pn):
        x = side * 2.0
        sc.cube(pn, V(x, 9.4, 0), (4.8, 5.4, 5.0), gold)                                              # thigh
        sc.cube(pn, V(x, 9.4, -2.6), (3.2, 4.4, .5), cream)
        sc.cube(pn, V(x, 6.4, 0), (4.6, 1.0, 4.9), plate)
        sc.cube(pn, V(x, 4.0, .2), (4.4, 4.6, 4.6), plate, dt=0)                                      # greave
        sc.cube(pn, V(x, 6.2, -2.4), (3.4, 1.4, .6), plate, dt=1)                                     # knee guard
        sc.cube(pn, V(x, 1.0, -1.8), (4.4, 2.0, 6.6), gold)                                            # paw
        sc.cube(pn, V(x, 0.8, -3.8), (4.0, 1.5, 2.8), cream)
        for k in range(3): sc.seg(pn, V(x + (k - 1) * 1.3, .6, -5.0), V(x + (k - 1) * 1.4, .4, -6.5), .8, .8, claw, pad=.3)
        sc.seg(pn, V(x, 3.4, 2.4), V(x, 1.4, 3.6), 1.0, 1.0, claw, pad=.2)                            # heel spur
    leg(1, "legL"); leg(-1, "legR")
    # =============================================================== tail (swings)
    pts = [V(0, 13.0, 2.6), V(0, 11.6, 5.2), V(0, 9.4, 7.6), V(0, 7.6, 9.8), V(0, 7.0, 12.2), V(0, 8.4, 14.2)]
    sc.chain("back", pts, [2.6, 2.4, 2.1, 1.8, 1.6, 1.4], [2.6, 2.4, 2.1, 1.8, 1.6, 1.4], gold, bone="tail", mat2=back)
    tp = pts[-1]
    for k, (dx, dy, dz) in enumerate(((0, 0, 1.4), (1.2, .8, 1.0), (-1.2, .8, 1.0), (0, 1.6, .6), (0.7, -.6, 1.1), (-.7, -.6, 1.1))):
        sc.seg("back", tp, tp + V(dx, dy, dz) * 1.9, 1.7, 1.7, mane if k % 2 == 0 else mane2, pad=.5, bone="tail")
    return sc


BUILDERS = {"lion_will": lion}


def build(wid):
    return BUILDERS[wid]()


if __name__ == "__main__":
    from forms_preview import *
    wid = sys.argv[1] if len(sys.argv) > 1 else "lion_will"
    sc = build(wid)
    cards = sc.cards(); pl = player_cards(head=False)
    views = [(0, 8), (35, 8), (90, 8), (150, 8)]
    ims = [render(pl + cards, sc.sh, yaw=y, pitch=p, W=360, H=470, scale=10.5) for y, p in views]
    out = os.path.join(HERE, "prev_%s.png" % wid)
    sheet_of(ims, ["front", "3/4", "side", "back 3/4"], out, cw=360, ch=470)
    print(out, len(sc.boxes), "cubes", len(cards), "quads")

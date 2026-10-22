"""Noelle's Sea Dragon God Form and Black Dragon Form (1.99.0): the shared builders of tools/gen_noelle_forms_drg.py (key ndg) and tools/gen_noelle_forms_blk.py (key nbk).

Both forms are one silhouette in two palettes (the owner's reference: Dragon Form: a horned, winged girl in dragon-scale armour; Black Dragon Form: the same, dark, with a devil-influenced look):
horned helm, scale cuirass with a gem in the breast, spiked pauldrons, clawed vambraces, taloned greaves, a scale skirt, great dragon wings (arm, finger spars, membranes), a long tail with a
fin, and a dragon-head cannon on the right forearm. The black form has jagged horns, torn wings, more spikes and a flame-edged tail.

    pal  a Palette (materials); `black` selects the harsher shapes.
Pieces: helm chest pauldron_r/l vambrace_r/l greave_r/l tassets wings tail cannon_r   |   character: ridge horns2 wings2 cannon_l
"""
import math

from noelle_forms_lib import *  # noqa: F401,F403


class Palette:
    def __init__(self, key, black):
        self.key, self.black = key, black
        k = 100 if black else 0
        if not black:
            self.scale = dscale(k + 1, base="1F63B0", dark="08224E", light="86D0F4", seam="46E4FF", cell=8.0)
            self.scale2 = dscale(k + 2, base="2A78C8", dark="0A2E64", light="A4E0FF", seam="6CF0FF", cell=7.0)
            self.belly = dscale(k + 3, base="CFE8F2", dark="6C98B4", light="FFFFFF", seam="9CF0FF", belly_rows=True, belly="D6EEF6", cell=8.0, glow_k=0.5)
            self.horn = horn(k + 4, base="EFE9D6", dark="6E6248", light="FFFFF2", glow="BFF4FF", glow_a=0.35)
            self.metal = gold(k + 5, base="D4E2F0", dark="5A7898", light="FFFFFF", glow="BFF4FF", glow_a=0.3)
            self.trim = gold(k + 6, base="E6BE5E", dark="7C5818", light="FFF4C4", glow="FFE49A", glow_a=0.35)
            self.membrane = membrane(k + 7, base="2E90DC", dark="0E3470", light="C4F2FF", vein="CFF8FF", glow="6CE8FF", alpha=185)
            self.membrane_f = membrane(k + 8, base="2E90DC", dark="0E3470", light="C4F2FF", vein="CFF8FF", glow="6CE8FF", alpha=185, flip=True)
            self.gem = gem(k + 9, c="49D8FF", glow="B8FAFF")
            self.fin = membrane(k + 10, base="3CA6E8", dark="144C8C", light="D4F8FF", vein="E0FFFF", glow="8CF4FF", alpha=190, panels=4)
            self.claw = horn(k + 11, base="E4F0F8", dark="6888A4", light="FFFFFF", glow="BFF4FF", glow_a=0.4, rings=3.0)
            self.glowc = "46E4FF"
        else:
            self.scale = dscale(k + 1, base="141822", dark="04050A", light="3A4666", seam="2C7CFF", cell=8.0, sheen="1A3C8C", glow_k=0.7)
            self.scale2 = dscale(k + 2, base="1C2230", dark="05060C", light="46547C", seam="3C8CFF", cell=7.0, sheen="223C7C", glow_k=0.7)
            self.belly = dscale(k + 3, base="3A4054", dark="10121C", light="6A7498", seam="2C7CFF", belly_rows=True, belly="4A5068", cell=8.0, glow_k=0.6)
            self.horn = horn(k + 4, base="1A1A22", dark="05050A", light="4A3A4A", glow="FF3A3A", glow_a=0.6)
            self.metal = gold(k + 5, base="2A3042", dark="080A12", light="7C88B0", glow="2C7CFF", glow_a=0.4)
            self.trim = gold(k + 6, base="6A1018", dark="1A0306", light="D04050", glow="FF3A3A", glow_a=0.5)
            self.membrane = membrane(k + 7, base="0C1424", dark="02030A", light="2C4A9C", vein="5CA0FF", glow="2C7CFF", alpha=200, ragged=0.9)
            self.membrane_f = membrane(k + 8, base="0C1424", dark="02030A", light="2C4A9C", vein="5CA0FF", glow="2C7CFF", alpha=200, ragged=0.9, flip=True)
            self.gem = gem(k + 9, c="FF3A3A", glow="FF9A9A")
            self.fin = membrane(k + 10, base="0C1424", dark="02030A", light="2C4A9C", vein="5CA0FF", glow="2C7CFF", alpha=205, ragged=0.7, panels=4)
            self.claw = horn(k + 11, base="241C26", dark="06040A", light="5A3A5A", glow="FF3A3A", glow_a=0.6, rings=3.0)
            self.glowc = "2C7CFF"
        self.flame = ink(k + 12, flame="2C7CFF", flame2="9CD6FF") if black else waterband(k + 12, alpha=170, c0="3EA6E4", c1="D4F6FF", glow="9CF8FF")


def piece(pal, name, d=3):
    return Piece(pal.key, "arm_" + name, density=d)


# ---------------------------------------------------------------------------------------------------------------------- standard pieces
def helm(pal):
    """The dragon helm: a brow guard and a crest, two long horns swept back and up (three bent segments, ringed), cheek spikes and jaw fins. The face stays open."""
    b = pal.black
    p = piece(pal, "helm")
    for (o, s) in (((-4.7, 29.2, -4.7), (9.4, 1.6, 0.9)), ((-4.7, 29.2, 3.8), (9.4, 1.6, 0.9)), ((-4.7, 29.2, -3.8), (0.9, 1.6, 7.6)), ((3.8, 29.2, -3.8), (0.9, 1.6, 7.6))):
        p.box("root", o, s, pal.metal)
    p.box("root", (-1.0, 30.8, -4.9), (2.0, 5.2 + (1.4 if b else 0.0), 1.0), pal.scale, rot=(-10, 0, 0), pivot=(0, 30.8, -4.9))        # the crest on the brow
    p.box("root", (-0.8, 29.3, -5.5), (1.6, 1.6, 0.9), pal.gem, rot=(0, 0, 45), pivot=(0, 30.1, -5.2))                               # the brow gem
    for sx in (-1, 1):
        p.box("root", (sx * 4.2 - 0.8, 28.0, -4.8), (1.6, 2.4, 1.6), pal.scale2, rot=(-20, sx * -10, 0), pivot=(sx * 4.2, 28.0, -4.0))   # the brow guards over the eyes
        # the horn: three segments, each bent further back and up
        x0, y0, z0 = sx * 3.4, 30.0, -1.0
        prev_rot = (-30, sx * -8, sx * 12)
        for k, (L, w) in enumerate(((5.0, 2.2), (5.0, 1.8), (4.4 + (2.0 if b else 0.0), 1.3))):
            p.box("root", (x0 - w / 2, y0, z0 - w / 2 + k * 2.4), (w, L, w), pal.horn, rot=(-34 - 22 * k, sx * (-8 - 4 * k), sx * (12 + 6 * k)), pivot=(x0, y0, z0 + k * 2.4))
            y0 += L * 0.78
            z0 += 0.0
        p.box("root", (sx * 4.5 - 0.4, 25.6, -3.4), (0.8, 2.6, 2.0), pal.scale, rot=(0, 0, sx * -18), pivot=(sx * 4.5, 27.0, -3.0))        # a cheek fin
        p.box("root", (sx * 4.55 - 0.4, 25.2, -0.8), (0.8, 2.2, 2.6), pal.scale2, rot=(0, 0, sx * -14), pivot=(sx * 4.5, 27.0, -0.6))
    if b:
        for sx in (-1, 1):                                                                                                         # the black form's second, jagged pair at the temples
            p.box("root", (sx * 4.9 - 0.5, 30.2, 0.2), (1.0, 5.0, 1.0), pal.horn, rot=(-50, 0, sx * 38), pivot=(sx * 4.9, 30.2, 0.2))
    p.save()
    return p


def chest(pal):
    b = pal.black
    p = piece(pal, "chest")
    for sx in (-1, 1):
        p.box("root", (sx * 2.3 - 2.25, 16.4, -3.05), (4.5, 7.2, 1.15), pal.scale, rot=(0, sx * -5, 0), pivot=(sx * 2.3, 20.0, -2.0))     # the two breast plates
    p.box("root", (-1.5, 15.0, -3.5), (3.0, 9.0, 1.0), pal.belly)                                                                       # the belly plates down the middle
    p.box("root", (-1.2, 18.4, -4.3), (2.4, 3.4, 1.0), pal.gem, rot=(0, 0, 45), pivot=(0, 20.1, -3.8))                                  # the gem
    p.box("root", (-2.0, 17.8, -4.0), (4.0, 4.4, 0.5), pal.metal, rot=(0, 0, 45), pivot=(0, 20.1, -3.8))
    for k in range(3):                                                                                                                  # fauld bands
        p.box("root", (-4.4 + 0.1 * k, 14.6 - 1.7 * k, -2.85 + 0.12 * k), (8.8 - 0.2 * k, 1.9, 1.0), pal.scale2 if k % 2 else pal.scale)
    p.box("root", (-4.9, 23.2, -3.0), (9.8, 1.9, 6.0), pal.scale2)                                                                      # the gorget
    p.box("root", (-4.95, 24.9, -3.05), (9.9, 0.7, 6.1), pal.metal)
    for sx in (-1, 1):
        p.box("root", (sx * 4.5 - 0.5, 24.0, -0.2), (1.0, 6.0 + (2.0 if b else 0.0), 3.0), pal.scale, rot=(-8, 0, sx * 14), pivot=(sx * 4.5, 24.0, 0.8))     # collar spikes
        p.box("root", (sx * 4.85 - 0.45, 12.4, -1.8), (0.9, 8.4, 3.6), pal.scale2)
    p.box("root", (-4.6, 12.0, 2.05), (9.2, 11.4, 1.0), pal.scale2)                                                                     # the back plate
    for k in range(5 if not b else 6):                                                                                                  # the spine ridge
        h = (3.4 - 0.3 * k) + (1.2 if b else 0.0)
        p.box("root", (-0.45, 22.0 - k * 2.0, 3.0), (0.9, h, 1.6 + 0.2 * (4 - k)), pal.horn if b else pal.metal, rot=(-18, 0, 0), pivot=(0, 22.0 - k * 2.0, 3.0))
    p.save()
    return p


def tassets(pal):
    p = piece(pal, "tassets")
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    p.box("root", (-4.75, 11.4, -2.75), (9.5, 1.4, 5.5), pal.trim)
    parts = [("f0", -2.7, -3.0), ("f1", 0.0, -3.3), ("f2", 2.7, -3.0)]
    for i, (n, cx, cz) in enumerate(parts):
        p.bone(n, (cx, 11.4, cz))
        p.box(n, (cx - 1.5, 5.0, cz - 0.6), (3.0, 6.6, 1.0), pal.scale if i % 2 == 0 else pal.scale2, rot=(-9 - 3 * (i == 1), 0, 0), pivot=(cx, 11.4, cz))
        p.box(n, (cx - 0.5, 3.6, cz - 0.7), (1.0, 2.0, 0.7), pal.horn, rot=(-9, 0, 0), pivot=(cx, 11.4, cz))
        sine_rot(a, "idle", n, 2.6, 0, 4.0, phase=i * 0.12)
    for i, (n, cx, cz, sg) in enumerate([("s0", -4.9, 0.0, -1), ("s1", 4.9, 0.0, 1)]):
        p.bone(n, (cx, 11.4, cz))
        p.box(n, (cx - 0.5, 4.0, cz - 2.2), (1.0, 7.6, 4.4), pal.scale2, rot=(0, 0, 10 * sg), pivot=(cx, 11.4, cz))
        sine_rot(a, "idle", n, 2.6, 2, 5.0, phase=0.3 + i * 0.1)
    p.save(a)
    return p


def pauldron(pal, side):
    n = "pauldron_" + ("l" if side else "r")
    b = pal.black
    p = piece(pal, n)
    for k in range(3):
        mbox(p, "root", side, (-9.6 + 0.5 * k, 21.0 + 1.0 * k, -3.6 + 0.2 * k), (5.8 - 0.9 * k, 3.2, 7.2 - 0.4 * k), pal.scale if k != 1 else pal.scale2)
    mbox(p, "root", side, (-10.0, 20.6, -3.7), (1.0, 3.4, 7.4), pal.metal)
    for k in range(3 + (1 if b else 0)):                                                                                              # the spikes standing up from the shoulder
        h = (5.2 - 0.7 * k) * (1.3 if b else 1.0)
        mbox(p, "root", side, (-8.8 + 0.6 * k, 24.4, -2.4 + 1.8 * k), (1.1, h, 1.1), pal.horn, rot=(0, 0, -14 - 6 * k), pivot=(-8.2 + 0.6 * k, 24.4, -1.9 + 1.8 * k))
    p.save()
    return p


def vambrace(pal, side):
    """A scale bracer with a blade fin along the outside of the forearm and clawed fingers."""
    n = "vambrace_" + ("l" if side else "r")
    b = pal.black
    p = piece(pal, n)
    mbox(p, "root", side, (-8.5, 14.4, -2.6), (5.0, 4.2, 5.2), pal.scale)
    mbox(p, "root", side, (-8.7, 18.4, -2.8), (5.4, 0.9, 5.6), pal.metal)
    mbox(p, "root", side, (-8.7, 14.0, -2.8), (5.4, 0.9, 5.6), pal.metal)
    for k in range(2 + (1 if b else 0)):
        bx = -8.9 if side == 0 else 8.9
        p.box("root", (bx - (0.5 if side == 0 else 0.0), 14.6 + 1.4 * k, 0.4 + 0.6 * k), (0.5, 1.6, 5.4 - 0.8 * k), pal.horn, rot=(-10 + 12 * k, 0, 0), pivot=(bx, 15.0, 1.0))
    mbox(p, "root", side, (-8.4, 11.6, -2.6), (4.8, 2.6, 5.2), pal.scale2)                                                            # the back of the hand
    for k in range(4):
        cx = (-8.0 + k * 1.15) if side == 0 else (4.2 - k * 1.15)
        p.box("root", (cx, 8.6, -2.6), (0.9, 3.4, 0.9), pal.claw, rot=(-8, 0, 0), pivot=(cx, 11.6, -2.6))
    p.save()
    return p


def greave(pal, side):
    n = "greave_" + ("l" if side else "r")
    b = pal.black
    p = piece(pal, n)
    mbox(p, "root", side, (-4.5, 6.8, -2.65), (4.9, 4.4, 5.3), pal.scale2)
    mbox(p, "root", side, (-4.65, 6.2, -3.4), (5.2, 1.8, 1.4), pal.metal)
    mbox(p, "root", side, (-4.0, 6.4, -4.6), (1.0, 1.4 + (1.0 if b else 0.0), 1.6), pal.horn, rot=(-20, 0, 0), pivot=(-4.0, 6.8, -3.4))   # the knee spike
    mbox(p, "root", side, (-4.45, 1.4, -2.55), (4.9, 4.8, 5.1), pal.scale)
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), pal.metal)
    for k in range(3):
        o = (-4.1 + k * 1.5, 0.0, -6.4)
        mbox(p, "root", side, o, (1.0, 1.0, 3.6), pal.claw, rot=(0, 0, 0))
    mbox(p, "root", side, (-2.6, 1.4, 2.6), (1.0, 3.4 + (1.0 if b else 0.0), 1.2), pal.horn, rot=(24, 0, 0), pivot=(-2.4, 1.6, 2.6))     # the heel spur
    p.save()
    return p


def _dragon_wing(pal, p, a, S, tattered=False, scale=1.0, tag="", length=2.6, amp=5.0, d=2, fingers=None):
    """A dragon wing pair (both sides in one model): an arm of two bones (shoulder -> elbow -> wrist), a thumb claw, `fingers` spars hinged at the wrist and a membrane gore between each pair of
    spars, plus the membrane along the arm back to the body. Every bone beats with a lag. Rolls: negative up, positive down (as feather_wing)."""
    fingers = fingers or [(-62, 32.0), (-32, 31.0), (-2, 27.0), (28, 21.0)]
    for side in (0, 1):
        sg = 1.0 if side else -1.0
        Sx, Sy, Sz = sg * abs(S[0]), S[1], S[2]
        up1, up2 = 56.0, 26.0
        a1, a2 = 7.5 * scale, 10.0 * scale
        e = (Sx + sg * a1 * math.cos(math.radians(up1)), Sy + a1 * math.sin(math.radians(up1)))
        w = (e[0] + sg * a2 * math.cos(math.radians(up2)), e[1] + a2 * math.sin(math.radians(up2)))
        r, el, wh = ("wl", "el", "hl") if side else ("wr", "er", "hr")
        r, el, wh = r + tag, el + tag, wh + tag
        p.bone(r, (Sx, Sy, Sz))
        p.bone(el, (e[0], e[1], Sz), parent=r)
        p.bone(wh, (w[0], w[1], Sz), parent=el)
        sine_rot(a, "idle", r, length, 2, -amp * sg, 0.0)
        sine_rot(a, "idle", el, length, 2, -amp * 0.7 * sg, 0.10)
        sine_rot(a, "idle", wh, length, 2, -amp * 0.9 * sg, 0.20)
        p.box(r, (Sx - (a1 if side == 0 else 0.0), Sy - 1.0, Sz - 0.8), (a1, 2.0, 1.8), pal.horn, rot=(0, 0, sg * up1), pivot=(Sx, Sy, Sz), d=3)
        p.box(el, (e[0] - (a2 if side == 0 else 0.0), e[1] - 0.9, Sz - 0.7), (a2, 1.8, 1.6), pal.horn, rot=(0, 0, sg * up2), pivot=(e[0], e[1], Sz), d=3)
        p.box(wh, (w[0] - (3.0 if side == 0 else 0.0), w[1] - 0.8, Sz - 0.6), (3.0, 1.6, 1.4), pal.claw, rot=(0, 0, sg * -70), pivot=(w[0], w[1], Sz), d=3)            # the thumb claw
        prev = None
        for i, (roll, L) in enumerate(fingers):
            L *= scale
            b = "%sf%d" % (wh, i)
            p.bone(b, (w[0], w[1], Sz), parent=wh, rotation=(0, sg * (8 + i * 1.5), -sg * roll))
            o = (w[0] - L, w[1] - 0.6, Sz) if side == 0 else (w[0], w[1] - 0.6, Sz)
            p.box(b, o, (L, 1.2, 1.2), pal.horn, d=d)                                                                                  # the finger spar
            sine_rot(a, "idle", b, length, 2, amp * 0.5 * sg, 0.30 + i * 0.05)
            if prev is not None and not (tattered and i == 2):                                                                          # the membrane gore between this spar and the last
                pr, pL = prev
                mid = (roll + pr) / 2.0
                Lm = (L + pL) / 2.0 * 0.96
                width = max(3.0, 2.0 * Lm * math.sin(math.radians(abs(roll - pr) / 2.0)))
                bm = "%sm%d" % (wh, i)
                p.bone(bm, (w[0], w[1], Sz), parent=wh, rotation=(0, sg * (8 + i * 1.5), -sg * mid))
                om = (w[0] - Lm, w[1] - width / 2, Sz + 0.1) if side == 0 else (w[0], w[1] - width / 2, Sz + 0.1)
                p.box(bm, om, (Lm, width, 0.4), (pal.membrane_f if side else pal.membrane), d=d)
                sine_rot(a, "idle", bm, length, 2, amp * 0.5 * sg, 0.33 + i * 0.05)
            prev = (roll, L)
        # the membrane along the arm, from the shoulder to the wrist, hanging down toward the body
        bm = r + "_p"
        p.bone(bm, (Sx, Sy, Sz), parent=r, rotation=(0, sg * 10, -sg * 70))
        Lp = (a1 + a2) * 0.9
        p.box(bm, (Sx - Lp if side == 0 else Sx, Sy - 4.0, Sz + 0.1), (Lp, 8.0, 0.4), pal.membrane, d=d)
        sine_rot(a, "idle", bm, length, 2, amp * 0.4 * sg, 0.12)


def wings(pal):
    p = piece(pal, "wings", 2)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    _dragon_wing(pal, p, a, (3.0, 21.0, 3.2), tattered=pal.black, scale=1.0)
    p.save(a)
    return p


def tail(pal):
    """The tail: nine scaled segments leaving the lower back, running back along the ground and rising at the end into a fin (a water fin; the black form a blade of ink-flame)."""
    p = piece(pal, "tail", 3)
    a = Anim()
    a.clip("idle", length=2.8, loop=True)
    n_seg = 9
    lean = [78.0, 6.0, 3.0, 0.0, -3.0, -5.0, -7.0, -9.0, -10.0]
    prev, y = "root", 12.0
    for i in range(n_seg):
        n = "t%d" % i
        L = 5.0 - 0.18 * i
        p.bone(n, (0, y, 2.5), parent=prev, rotation=(-lean[i], 0, 0))
        w = max(1.8, 7.0 - 0.6 * i)
        p.box(n, (-w / 2, y - L - 0.3, 2.5 - w * 0.4), (w, L + 0.6, w * 0.8), pal.scale if i % 2 == 0 else pal.scale2, d=3)
        if i in (1, 3, 5, 7):
            p.box(n, (-0.35, y - L, 2.5 + w * 0.3), (0.7, L - 0.4, 2.2), pal.horn, d=3)
        sine_rot(a, "idle", n, 2.8, 1, 5.0 + 0.9 * i, phase=0.06 * i)
        sine_rot(a, "idle", n, 2.8, 0, 2.0 + 0.4 * i, phase=0.05 * i + 0.3)
        prev, y = n, y - L
    p.bone("fin", (0, y, 2.5), parent=prev)
    if pal.black:
        for k, (dx, h) in enumerate(((0.0, 9.0), (-2.6, 6.0), (2.6, 6.0))):
            p.box("fin", (dx - 0.7, y - h, 2.5 - 0.3), (1.4, h, 0.6), pal.fin, rot=(0, 0, dx * 6), pivot=(0, y, 2.5), d=3)
        p.box("fin", (-0.5, y - 12.0, 2.5 - 0.3), (1.0, 4.0, 0.6), pal.flame, pivot=(0, y, 2.5), d=3)
    else:
        p.box("fin", (-9.0, y - 11.0, 2.5 - 0.3), (18.0, 11.0, 0.6), pal.fin, d=3)
        p.box("fin", (-0.5, y - 12.5, 2.5 - 0.5), (1.0, 12.5, 1.0), pal.horn, d=3)
    sine_rot(a, "idle", "fin", 2.8, 1, 10.0, phase=0.6)
    p.save(a)
    return p


def cannon(pal, side):
    """The dragon-head water cannon on the forearm: a scaled gauntlet and a dragon head at the fist pointing along the arm, jaw open, a glowing throat, horns, teeth."""
    n = "cannon_" + ("l" if side else "r")
    b = pal.black
    p = piece(pal, n)
    a = Anim()
    a.clip("idle", length=2.0, loop=True)
    mbox(p, "root", side, (-8.9, 13.4, -3.0), (5.8, 5.2, 6.0), pal.scale2)                                                            # the housing over the forearm
    mbox(p, "root", side, (-9.1, 18.0, -3.2), (6.2, 1.0, 6.4), pal.metal)
    mbox(p, "root", side, (-9.1, 13.0, -3.2), (6.2, 1.0, 6.4), pal.metal)
    hx = -6.1 if side == 0 else 6.1
    p.bone("head", (hx, 12.6, 0.0))
    mbox(p, "head", side, (-8.2, 8.0, -2.6), (4.4, 5.0, 5.2), pal.scale)                                                              # the skull
    mbox(p, "head", side, (-7.9, 3.4, -2.2), (3.8, 4.8, 4.4), pal.scale2)                                                             # the snout
    mbox(p, "head", side, (-7.6, 3.0, -1.8), (3.2, 1.0, 3.6), pal.horn)
    p.bone("jaw", (hx, 6.0, 0.0), parent="head")
    mbox(p, "jaw", side, (-7.7, 1.6, -1.9), (3.4, 4.2, 3.8), pal.scale2, rot=(-18, 0, 0), pivot=(-6.1, 6.0, 0.0))        # the lower jaw, hanging open
    for k in range(3):                                                                                                                # teeth
        mbox(p, "jaw", side, (-7.5 + k * 1.1, 4.8, -1.9), (0.5, 1.8, 0.5), pal.claw, rot=(-18, 0, 0), pivot=(-6.1, 6.0, 0.0))
        mbox(p, "head", side, (-7.5 + k * 1.1, 2.4, -1.6), (0.5, 1.8, 0.5), pal.claw)
    mbox(p, "head", side, (-7.2, 5.0, -1.2), (2.4, 2.4, 2.4), pal.flame)                                                              # the glowing throat
    for sx2 in (0, 1):
        mbox(p, "head", side, (-8.4 + 0.2 * sx2, 12.0, -2.4 + sx2 * 4.0), (0.9, 4.4 + (1.6 if b else 0.0), 0.9), pal.horn, rot=(-24, 0, 0), pivot=(-6.1, 12.6, 0.0))     # horns on the skull
    sine_rot(a, "idle", "jaw", 2.0, 0, 6.0, phase=0.0)
    p.save(a)
    return p


# ---------------------------------------------------------------------------------------------------------------------- character pieces
def ridge(pal):
    """A ridge of dorsal fins down the spine from the neck to the tail (membrane between spines), swaying."""
    p = piece(pal, "ridge")
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    for i in range(7):
        n = "r%d" % i
        y = 24.0 - i * 1.9
        p.bone(n, (0, y, 3.4))
        h = (8.0 - 0.7 * i) * (1.2 if pal.black else 1.0)
        p.box(n, (-0.35, y, 3.4), (0.7, h, 4.0 + 0.2 * (3 - abs(3 - i))), pal.fin, rot=(-18, 0, 0), pivot=(0, y, 3.4), d=3)
        sine_rot(a, "idle", n, 3.0, 0, 5.0, phase=i * 0.06)
    p.save(a)
    return p


def horns2(pal):
    """A crown of short horns above the first pair (the second pair, curved the other way) and a ring of spikes behind the head."""
    p = piece(pal, "horns2")
    for k in range(5):
        ang = -60 + 30 * k
        p.box("root", (-0.7, 31.0, -0.7), (1.4, 5.0 + (2.0 if pal.black else 0.0) - abs(k - 2) * 0.8, 1.4), pal.horn, rot=(-26, 0, ang), pivot=(0, 31.0, 0.0))
    p.save()
    return p


def wings2(pal):
    """A smaller lower pair of wings swept down and back."""
    p = piece(pal, "wings2", 2)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    _dragon_wing(pal, p, a, (2.4, 15.0, 3.8), tattered=True, scale=0.62, tag="b", length=3.0, amp=6.0, fingers=[(0, 26.0), (26, 25.0), (52, 21.0)])
    p.save(a)
    return p


STANDARD = [helm, chest, tassets, wings, tail]
PAIRED = [pauldron, vambrace, greave]
CHARACTER = [ridge, horns2, wings2]


def build_form(key, black):
    pal = Palette(key, black)
    made = []
    for f in STANDARD + CHARACTER:
        made.append(f(pal))
    for f in PAIRED:
        for s in (0, 1):
            made.append(f(pal, s))
    for s in (0, 1):
        made.append(cannon(pal, s))
    return made

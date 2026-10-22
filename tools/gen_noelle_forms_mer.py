"""Noelle's Mermaid Form (1.99.0): the pieces of Aura.WATER style 2 (standard tier) and 18 (character tier).

    python3 -B tools/gen_noelle_forms_geo.py mer          # writes geo/entity/nml_arm_*, their textures, glow maps and animations

Reference: the anime still: Noelle as a mermaid with twin tails of hair, a sea-blue scaled tail, fin ears, a shell top and a trident of ice-water. The legs of the player stay under their own
animation, so the tail is built as two scaled leggings (each on its own leg) that read as one tail when she stands, ending in ankle fins, with a long tail TRAIN behind the hips (swaying, ending
in a great fluke that rests on the ground behind her). Standard pieces: crown hair bust leg_r leg_l train veil vambrace_r/l trident. Character: dorsal scarf coral.
"""
import math

from noelle_forms_lib import *  # noqa: F401,F403
import gen_undine_mermaid as um  # noqa: E402  (the mermaid's hair, shells and fins)

KEY = "nml"

SHELL = [um.shell_mat(1), um.shell_mat(2), um.shell_mat(3), um.shell_mat(4)]
PEARL = gold(51, base="F2EEF6", dark="9C90B0", light="FFFFFF", glow="FFE8F8", glow_a=0.3, stripe=False)
GOLDM = gold(52, base="DDE8F0", dark="6C88A8", light="FFFFFF", glow="CFF4FF", glow_a=0.35)
HAIR = [um.hair_mat(60 + i, tip=(i > 6), light=0.04 * (i % 3 - 1)) for i in range(12)]
CLOTH = waterband(53, alpha=150, c0="52C8EC", c1="E8FEFF", glow="9CF8FF")
CLOTH2 = waterband(54, alpha=140, c0="3EA6E4", c1="D4F6FF", glow="9CF8FF")
CRYSTAL = Mat("crystal", "8CE0FF", dark="2E9ADC", light="F4FFFF", glow="B4FAFF", glow_amount=0.85, alpha=200, cells=5, seed=55)
ICE = Mat("crystal", "DDF8FF", dark="7CC4E8", light="FFFFFF", glow="E0FFFF", glow_amount=0.95, alpha=215, cells=4, seed=56)
CORAL = Mat("plate", "FFB4C8", dark="C86A8A", light="FFF0F4", glow="FFD4E4", glow_amount=0.5, seed=57)
GEM = gem(58, c="FF9CD8", glow="FFD0F0")
GEM_B = gem(59, c="49D8FF", glow="B8FAFF")


def S(i, t0, t1, cell=4.5):
    return tailscale(t0, t1, 70 + i, c0="9CD8F0", c1="2E8CE0", c2="5CF4FF", cell=cell)


def P(name, density=3):
    return Piece(KEY, "arm_" + name, density=density)


def crown():
    """A pearl circlet with a shell at the brow, two big fin-ears swept back and up, small coral spires; translucent fin membranes between ribs."""
    p = P("crown", 3)
    for (o, s) in (((-4.7, 29.2, -4.7), (9.4, 1.4, 0.8)), ((-4.7, 29.2, 3.9), (9.4, 1.4, 0.8)), ((-4.7, 29.2, -3.9), (0.8, 1.4, 7.8)), ((3.9, 29.2, -3.9), (0.8, 1.4, 7.8))):
        p.box("root", o, s, PEARL)
    p.box("root", (-2.0, 29.8, -5.5), (4.0, 3.4, 1.8), SHELL[0], d=4, rot=(-14, 0, 0), pivot=(0, 30.0, -4.8))                    # the shell at the brow
    p.box("root", (-0.7, 30.0, -5.9), (1.4, 1.4, 1.2), GEM, rot=(0, 0, 45), pivot=(0, 30.7, -5.5))
    for sx, t in ((1, "l"), (-1, "r")):
        p.bone("ear_" + t, (sx * 4.2, 28.4, 0.0), rotation=(0, sx * 24, sx * 14))
        p.box("ear_" + t, (sx * 4.2 - 0.3, 25.4, -0.8), (0.6, 8.0, 10.0), um.fin_mat("ear", seed=2, W=10.0, H=8.0), d=4)
        p.box("ear_" + t, (sx * 4.3 - 0.45, 27.2, -1.4), (0.9, 2.6, 1.8), SHELL[1], d=4)
        for k in range(2):                                                                                                        # pearls strung along the circlet
            p.box("root", (sx * (2.8 + k * 1.6) - 0.6, 30.2, -4.9), (1.2, 1.2, 1.2), PEARL)
    p.save()
    return p


def hair():
    """Noelle's twin tails: from each side of the head two interleaved strands of long wavy hair fall past the shoulders to the hips (nine segments, each waving with a lag), a pearl tie at the
    root, the tips thinning and glowing."""
    p = P("hair", 2)
    a = Anim()
    a.clip("idle", length=3.4, loop=True)
    for sx, tag in ((1, "l"), (-1, "r")):
        px = sx * 5.4
        p.box("root", (px - 1.5, 29.2, -0.2), (3.0, 3.0, 3.0), PEARL)                                                              # the tie
        for strand in range(2):
            prev, y = "root", 31.0
            ox = sx * (0.0 if strand == 0 else 1.6)
            zz = 0.6 + 1.6 * strand
            for i in range(9):
                n = "%s%d_%d" % (tag, strand, i)
                rz = sx * ((12.0 if strand == 0 else 20.0) if i == 0 else 8.0 * math.cos(0.9 * i + (sx > 0) + strand))
                p.bone(n, (px + ox, y, zz), parent=prev, rotation=(0, 0, rz))
                w = (4.4 if strand == 0 else 3.0) - 0.26 * i
                p.box(n, (px + ox - w / 2, y - 5.6, zz - 0.6), (w, 5.8, 1.6), HAIR[(i + strand * 5 + (3 if sx > 0 else 0)) % 12], d=2)
                sine_rot(a, "idle", n, 3.4, 2, 5.0 + 0.5 * i, phase=0.06 * i + 0.18 * strand + (0.0 if sx > 0 else 0.35))
                sine_rot(a, "idle", n, 3.4, 0, 3.0 + 0.3 * i, phase=0.08 * i + 0.25)
                prev, y = n, y - 5.2
    p.save(a)
    return p


def bust():
    """The shell top: two scallop shells and a pearl between them, a band of scales at the waist with a gem, a chain of pearls across the hips."""
    p = P("bust", 3)
    for sx in (-1, 1):
        p.box("root", (sx * 2.3 - 2.2, 18.8, -3.5), (4.4, 4.2, 1.8), SHELL[(sx > 0) * 2], d=4, rot=(-10, sx * 8, 0), pivot=(sx * 2.3, 21.0, -2.6))
        p.box("root", (sx * 2.3 - 1.4, 22.8, -3.2), (2.8, 1.0, 1.0), SHELL[3], d=4, rot=(-10, sx * 8, 0), pivot=(sx * 2.3, 21.0, -2.6))
    p.box("root", (-1.1, 19.8, -4.0), (2.2, 2.2, 1.2), GEM, rot=(0, 0, 45), pivot=(0, 20.9, -3.5))
    p.box("root", (-4.4, 16.4, -2.7), (8.8, 1.2, 0.9), PEARL)                                                                      # the band below the shells
    p.box("root", (-4.2, 12.2, -2.65), (8.4, 3.4, 0.9), S(0, 0.0, 0.15, 4.0), d=3)                                                 # scales at the waist
    p.box("root", (-4.4, 15.4, -2.8), (8.8, 0.8, 1.0), GOLDM)
    p.box("root", (-1.0, 13.2, -3.3), (2.0, 2.0, 1.0), GEM_B, rot=(0, 0, 45), pivot=(0, 14.2, -2.9))
    p.box("root", (-4.5, 23.4, -2.5), (9.0, 1.2, 5.0), GOLDM)                                                                      # a collar of pearl and silver
    p.box("root", (-4.5, 12.0, 2.0), (9.0, 11.0, 0.9), S(1, 0.0, 0.1, 4.0), d=3)                                                   # scales over the back
    p.save()
    return p


def leg(side):
    """A legging of scales on one leg: thigh and shin, a dorsal fin along the outside, and a fin at the ankle; together the two read as one tail."""
    n = "leg_" + ("l" if side else "r")
    p = P(n, 3)
    mbox(p, "root", side, (-4.35, 6.0, -2.6), (4.7, 6.2, 5.2), S(2 + side, 0.12, 0.34, 4.0), d=3)
    mbox(p, "root", side, (-4.3, 0.6, -2.5), (4.6, 5.8, 5.0), S(4 + side, 0.30, 0.5, 4.0), d=3)
    mbox(p, "root", side, (-4.5, 5.6, -2.7), (4.9, 0.9, 5.4), GOLDM)
    mbox(p, "root", side, (-5.1, 1.0, 0.2), (0.5, 7.4, 4.0), um.fin_mat("hip", seed=5, W=4.0, H=7.4), d=4)                           # a fin along the outside
    mbox(p, "root", side, (-4.5, 0.0, -3.0), (4.9, 1.0, 6.0), PEARL)
    p.save()
    return p


def train():
    """The tail train behind the hips: seven scaled segments that start straight down from the hips and bend back until they run flat along the ground, ending in a great fluke that spreads
    behind her; swaying with a lag."""
    p = P("train", 3)
    a = Anim()
    a.clip("idle", length=2.8, loop=True)
    Ls = [4.4, 4.2, 4.0, 3.8, 3.6, 3.4, 3.2]
    lean = [26.0, 20.0, 16.0, 12.0, 8.0, 5.0, 3.0]                       # how much more each segment leans back than the one above it
    widths = [8.6, 7.8, 7.0, 6.0, 5.0, 4.0, 3.2]
    prev, y = "root", 12.0
    for i, L in enumerate(Ls):
        n = "t%d" % i
        p.bone(n, (0, y, 2.5), parent=prev, rotation=(-lean[i], 0, 0))
        w = widths[i]
        p.box(n, (-w / 2, y - L - 0.2, 2.5 - 2.2 + 0.12 * i), (w, L + 0.4, 4.4 - 0.25 * i), S(10 + i, 0.4 + i / 7.0 * 0.6, 0.4 + (i + 1) / 7.0 * 0.6, 4.5), d=3)
        if i in (1, 3, 5):
            p.box(n, (-0.3, y - L, 2.5 + 1.0), (0.6, L - 0.4, 2.6), um.fin_mat("hip", seed=6 + i, W=2.6, H=4.0), d=4)
        sine_rot(a, "idle", n, 2.8, 1, 5.0 + 0.8 * i, phase=0.07 * i)
        sine_rot(a, "idle", n, 2.8, 0, 2.0 + 0.3 * i, phase=0.05 * i + 0.3)
        prev, y = n, y - L
    p.bone("fluke", (0, y, 2.5), parent=prev)
    p.box("fluke", (-15.0, y - 14.0, 2.5 - 0.35), (30.0, 14.0, 0.7), um.fin_mat("fluke", seed=3, W=30.0, H=14.0), d=3)
    sine_rot(a, "idle", "fluke", 2.8, 1, 10.0, phase=0.6)
    sine_rot(a, "idle", "fluke", 2.8, 0, 4.0, phase=0.3)
    p.save(a)
    return p


def veil():
    """Translucent hip fins and a dorsal fin along the spine: they flare from the waist and sway."""
    p = P("veil", 3)
    a = Anim()
    a.clip("idle", length=2.4, loop=True)
    for i, (x, z, rz, h) in enumerate([(-4.8, 0.0, -24, 9.0), (4.8, 0.0, 24, 9.0), (-4.0, 2.4, -18, 8.0), (4.0, 2.4, 18, 8.0)]):
        n = "f%d" % i
        p.bone(n, (x, 13.0, z))
        p.box(n, (x - (4.6 if x < 0 else 0.0), 13.0 - h, z - 0.3), (4.6, h, 0.6), um.fin_mat("hip", seed=7 + i, W=4.6, H=h), d=3, rot=(0, 0, rz), pivot=(x, 13.0, z))
        sine_rot(a, "idle", n, 2.4, 2, 6.0, phase=0.12 * i)
    p.bone("dorsal", (0, 23.0, 2.6))
    p.box("dorsal", (-0.3, 12.0, 2.6), (0.6, 12.0, 5.0), um.fin_mat("hip", seed=11, W=5.0, H=12.0), d=3)
    sine_rot(a, "idle", "dorsal", 2.4, 0, 4.0, phase=0.2)
    p.save(a)
    return p


def vambrace(side):
    """A pearl cuff at the wrist and a spread fin along the forearm, pointing back."""
    n = "vambrace_" + ("l" if side else "r")
    p = P(n, 3)
    a = Anim()
    a.clip("idle", length=2.2, loop=True)
    mbox(p, "root", side, (-8.4, 14.0, -2.5), (4.8, 3.4, 5.0), S(20 + side, 0.1, 0.3, 4.0), d=3)
    mbox(p, "root", side, (-8.6, 17.4, -2.7), (5.2, 0.9, 5.4), PEARL)
    mbox(p, "root", side, (-8.6, 13.6, -2.7), (5.2, 0.9, 5.4), PEARL)
    bx = -8.8 if side == 0 else 8.8
    p.bone("fin", (bx, 16.0, 1.0))
    mbox(p, "fin", side, (-9.2, 12.4, 0.6), (0.5, 7.0, 6.0), um.fin_mat("hip", seed=12 + side, W=6.0, H=7.0), d=4)
    sine_rot(a, "idle", "fin", 2.2, 1, 7.0, phase=0.15)
    mbox(p, "root", side, (-8.4, 11.6, -2.6), (4.8, 2.4, 5.2), PEARL)                                                              # the back of the hand
    for k in range(4):
        cx = (-8.0 + k * 1.15) if side == 0 else (4.2 - k * 1.15)
        p.box("root", (cx, 9.4, -2.5), (0.8, 2.4, 0.8), PEARL)
    p.save(a)
    return p


def trident():
    """The trident of ice-water hovering upright at the right hand: a crystal shaft with a silver grip, a three-pronged head (the centre prong long, the side prongs curved and barbed), a wave-shaped
    guard and a spiral of water that turns round the shaft."""
    p = P("trident", 3)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    cx, cz = -12.5, -5.0
    p.bone("t", (cx, 18.0, cz), rotation=(-8, 0, 6))
    p.box("t", (cx - 1.0, 3.0, cz - 1.0), (2.0, 25.0, 2.0), ICE)
    p.box("t", (cx - 1.5, 8.0, cz - 1.5), (3.0, 4.0, 3.0), GOLDM)
    p.box("t", (cx - 1.7, 3.0, cz - 1.7), (3.4, 1.4, 3.4), GOLDM)
    p.box("t", (cx - 4.4, 27.8, cz - 0.8), (8.8, 1.6, 1.6), GOLDM)                                                                  # the guard
    for sx in (-1, 1):
        p.box("t", (cx + sx * 3.2 - 0.9, 29.0, cz - 0.9), (1.8, 7.0, 1.8), ICE, rot=(0, 0, sx * -8), pivot=(cx + sx * 3.2, 29.0, cz))      # the side prongs, curving in
        p.box("t", (cx + sx * 3.8 - 0.7, 35.4, cz - 0.7), (1.4, 3.0, 1.4), CRYSTAL, rot=(0, 0, sx * -16), pivot=(cx + sx * 3.4, 35.0, cz))
        p.box("t", (cx + sx * 2.0 - 0.5, 31.0, cz - 0.5), (1.0, 2.6, 1.0), CRYSTAL, rot=(0, 0, sx * 40), pivot=(cx + sx * 2.4, 31.0, cz))   # a barb
    p.box("t", (cx - 1.1, 29.0, cz - 1.1), (2.2, 12.0, 2.2), ICE)                                                                   # the centre prong
    p.box("t", (cx - 0.8, 40.4, cz - 0.8), (1.6, 3.2, 1.6), CRYSTAL)
    p.box("t", (cx - 1.8, 38.4, cz - 0.6), (3.6, 1.2, 1.2), CRYSTAL, rot=(0, 0, 45), pivot=(cx, 39.0, cz))
    p.bone("spiral", (cx, 18.0, cz), parent="t")
    for k in range(8):
        w = 6.0 - 0.45 * k
        p.box("spiral", (cx - w / 2, 4.0 + k * 3.0, cz - w / 2), (w, 1.1, w), CLOTH, rot=(0, k * 40.0, 0), pivot=(cx, 4.5 + k * 3.0, cz))
    spin_y(a, "idle", "spiral", 3.0, turns=2, steps=8)
    a.pos("idle", "t", {0.0: (0, 0, 0), 1.5: (0, 1.4, 0), 3.0: (0, 0, 0)})
    p.save(a)
    return p


# ---------------------------------------------------------------------------------------------------------------------- character pieces
def dorsal():
    """A great sail-like dorsal fin rising behind the shoulders and down the back, translucent, with ribs, swaying."""
    p = P("dorsal", 3)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    p.bone("d", (0, 18.0, 3.4))
    p.box("d", (-0.4, 14.0, 3.4), (0.8, 26.0, 11.0), um.fin_mat("hip", seed=21, W=11.0, H=26.0), d=3)
    for sx in (-1, 1):
        p.bone("s%d" % (sx > 0), (sx * 1.0, 20.0, 3.6), parent="d")
        p.box("s%d" % (sx > 0), (sx * 1.0 - 0.3 + (0 if sx > 0 else -3.4), 12.0, 3.4), (3.4, 16.0, 0.6), um.fin_mat("hip", seed=22 + (sx > 0), W=3.4, H=16.0), d=3, rot=(0, 0, sx * 20), pivot=(sx * 1.0, 20.0, 3.6))
        sine_rot(a, "idle", "s%d" % (sx > 0), 3.0, 2, 6.0, phase=0.2 * (sx > 0))
    sine_rot(a, "idle", "d", 3.0, 0, 3.0)
    p.save(a)
    return p


def scarf():
    """Two long translucent scarves of water that start at the shoulders and stream back and down; they sway with a lag."""
    p = P("scarf", 2)
    a = Anim()
    a.clip("idle", length=3.2, loop=True)
    for sx, t in ((1, "l"), (-1, "r")):
        prev, y, z = "root", 23.0, 2.4
        for i in range(7):
            n = "%s%d" % (t, i)
            p.bone(n, (sx * 5.0, y, z), parent=prev, rotation=(8.0 if i == 0 else 4.0, 0, sx * (10.0 if i == 0 else 3.0)))
            w = 5.0 - 0.3 * i
            p.box(n, (sx * 5.0 - w / 2, y - 5.2, z), (w, 5.4, 0.6), CLOTH if i % 2 else CLOTH2, d=3)
            sine_rot(a, "idle", n, 3.2, 0, 7.0 + 0.6 * i, phase=0.07 * i + (0.3 if sx < 0 else 0.0))
            sine_rot(a, "idle", n, 3.2, 2, 4.0 + 0.4 * i, phase=0.09 * i + 0.2)
            prev, y, z = n, y - 4.9, z + 0.9
    p.save(a)
    return p


def coral():
    """A tiara of shells and pearls above the circlet (the character tier): five scallop shells fanned over the brow, a pearl between each, a gem in the middle."""
    p = P("coral", 3)
    a = Anim()
    a.clip("idle", length=6.0, loop=True)
    p.bone("c", (0, 31.0, -3.0))
    for k in range(5):
        ang = -50 + 25 * k
        h = 5.4 - abs(k - 2) * 0.9
        p.box("c", (-1.8, 31.4, -4.2), (3.6, h, 1.1), SHELL[k % 4], d=4, rot=(-8, 0, ang), pivot=(0, 31.0, -4.0))
        p.box("c", (-0.6 + math.sin(math.radians(ang + 12)) * 4.6 * -1, 31.2, -4.5), (1.2, 1.2, 1.2), PEARL)
    p.box("c", (-1.0, 33.4, -5.0), (2.0, 2.0, 1.2), GEM, rot=(0, 0, 45), pivot=(0, 34.4, -4.6))
    sine_rot(a, "idle", "c", 6.0, 2, 1.0)
    p.save(a)
    return p


STANDARD = [crown, hair, bust, train, veil, trident]
PAIRED = [leg, vambrace]
CHARACTER = [dorsal, scarf, coral]


def build():
    made = []
    for f in STANDARD + CHARACTER:
        made.append(f())
    for f in PAIRED:
        for s in (0, 1):
            made.append(f(s))
    return made

"""Noelle's Valkyrie Dress / Saint Valkyrie Dress (1.99.0): the pieces of Aura.WATER style 1 (standard tier) and style 17 (character tier: the Saint Valkyrie Dress).

    python3 -B tools/gen_noelle_forms_geo.py val          # (called by gen_noelle_forms_geo.py) writes geo/entity/nvl_arm_*, their textures, glow maps and animations

Reference: the manga still of the Saint Valkyrie Dress (docs/magic/noelle_forms.md): large angelic wings, ornate armour, a halo, flowing water. Pearl-white enamel with a gold border and a
wave engraving, a blue gem on the breast, layered feather wings (primaries, secondaries, coverts) white at the quill and glowing blue at the point, a halo, a skirt of water cloth. The
character tier adds a second pair of lower wings, the aureole (a ring behind the back), a gold sash with feathers, and a hovering drill lance of water.

Pieces (the painter client/aura/NoelleFormsAura lists the same names):
  standard   helm chest skirt pauldron_r/l vambrace_r/l gauntlet_r/l greave_r/l wings halo
  character  wings2 aureole sash lance
"""
import math

from noelle_forms_lib import *  # noqa: F401,F403

KEY = "nvl"

ENAMEL = enamel(11, gem="49BFFF")
ENAMEL_B = enamel(12, wave=0.7)
ENAMEL_D = enamel(13, base="D9E8FA", dark="7E9AC4", wave=0.7)
ENAMEL_HELM = enamel(14, gem="49BFFF", wave=0.3)
GOLD = gold(15)
GOLD_P = gold(16, stripe=False)
GEM = gem(17)
CLOTH = waterband(18, alpha=165)
CLOTH2 = waterband(19, alpha=150, c0="3E8EE8", c1="CFF2FF")
FEATHER_A = wfeather(21, flip=False)
FEATHER_AF = wfeather(22, flip=True)
FEATHER_B = wfeather(23, root="FFFFFF", mid="D2F0FF", tip="7CC8FF", flip=False)
FEATHER_BF = wfeather(24, root="FFFFFF", mid="D2F0FF", tip="7CC8FF", flip=True)
FEATHER_C = wfeather(25, root="FFFFFF", mid="E6F6FF", tip="A8E0FF", flip=False, barb=1.4)
FEATHER_CF = wfeather(26, root="FFFFFF", mid="E6F6FF", tip="A8E0FF", flip=True, barb=1.4)
FEATHER_GA = wfeather(27, root="F8FCFF", mid="DDF0FF", tip="7CC8FF", flip=False, gold_edge="E6BE5E", glow="BFF4FF", glow_k=0.7)
FEATHER_GAF = wfeather(28, root="F8FCFF", mid="DDF0FF", tip="7CC8FF", flip=True, gold_edge="E6BE5E", glow="BFF4FF", glow_k=0.7)
CRYSTAL = Mat("crystal", "6CC0FF", dark="2A78D0", light="F0FCFF", glow="9CF4FF", glow_amount=0.8, alpha=205, cells=5, seed=33)
CRYSTAL_W = Mat("crystal", "DDF4FF", dark="8CB8E6", light="FFFFFF", glow="CFF8FF", glow_amount=0.9, alpha=200, cells=4, seed=34)


def P(name, density=3):
    return Piece(KEY, "arm_" + name, density=density)


# ---------------------------------------------------------------------------------------------------------------------- standard pieces
def helm():
    """A tiara: a gold circlet round the head, a tall pearl fin-crest at the brow with the blue gem, two swept feather blades at the temples, drops of water behind the ears."""
    p = P("helm", 3)
    for (o, s) in (((-4.7, 29.0, -4.7), (9.4, 1.6, 0.9)), ((-4.7, 29.0, 3.8), (9.4, 1.6, 0.9)), ((-4.7, 29.0, -3.8), (0.9, 1.6, 7.6)), ((3.8, 29.0, -3.8), (0.9, 1.6, 7.6))):
        p.box("root", o, s, GOLD)
    p.box("root", (-1.1, 30.4, -4.9), (2.2, 6.2, 0.8), ENAMEL_HELM, rot=(-6, 0, 0), pivot=(0, 30.4, -4.9))          # the crest at the brow
    p.box("root", (-0.45, 36.2, -4.95), (0.9, 2.2, 0.7), GOLD)
    p.box("root", (-0.9, 29.2, -5.2), (1.8, 1.8, 0.7), GEM, rot=(0, 0, 45), pivot=(0, 30.1, -5.0))                    # the brow gem
    for sx in (-1, 1):
        p.box("root", (sx * 4.6 - 0.45, 29.6, -2.6), (0.9, 7.4, 3.0), ENAMEL, rot=(-28, 0, sx * 16), pivot=(sx * 4.6, 29.6, -1.0))       # a blade swept up and back at the temple
        p.box("root", (sx * 4.7 - 0.35, 28.6, 0.6), (0.7, 5.6, 2.4), CRYSTAL_W, rot=(-12, sx * -14, sx * 12), pivot=(sx * 4.7, 29.0, 0.6))     # a second, shorter, behind it
        p.box("root", (sx * 4.9 - 0.5, 26.2, -0.6), (1.0, 1.6, 1.0), GEM, rot=(0, 0, 45))                                          # a drop at the ear
    p.save()
    return p


def chest():
    """The cuirass: pearl enamel, a gold border, the blue gem, a high winged collar; the back plate and the spine; gold sash line from shoulder to hip."""
    p = P("chest", 3)
    for sx in (-1, 1):
        p.box("root", (sx * 2.3 - 2.25, 17.0, -3.05), (4.5, 6.6, 1.15), ENAMEL, rot=(0, sx * -5, 0), pivot=(sx * 2.3, 20.0, -2.0))        # the two breast plates, a little angled
    p.box("root", (-1.3, 18.6, -3.7), (2.6, 3.6, 0.9), GEM, rot=(0, 0, 45), pivot=(0, 20.4, -3.2))                                      # the gem between them, in a gold setting
    p.box("root", (-1.9, 18.2, -3.4), (3.8, 4.4, 0.5), GOLD, rot=(0, 0, 45), pivot=(0, 20.4, -3.2))
    p.box("root", (-4.5, 15.2, -2.85), (9.0, 1.7, 1.0), GOLD_P)                                                                          # the belt of the cuirass
    p.box("root", (-4.35, 13.2, -2.75), (8.7, 2.1, 0.95), ENAMEL_B)
    p.box("root", (-4.2, 11.8, -2.6), (8.4, 1.5, 0.85), ENAMEL_D)
    p.box("root", (-4.9, 23.2, -3.0), (9.8, 1.9, 6.0), ENAMEL_B)                                                                         # the gorget
    p.box("root", (-4.95, 24.9, -3.05), (9.9, 0.7, 6.1), GOLD)
    for sx in (-1, 1):
        p.box("root", (sx * 4.35 - 0.5, 24.0, -0.4), (1.0, 6.4, 3.4), ENAMEL, rot=(-8, 0, sx * 12), pivot=(sx * 4.35, 24.0, 0.8))      # the high collar at the sides of the neck
        p.box("root", (sx * 4.85 - 0.45, 12.2, -1.8), (0.9, 8.6, 3.6), ENAMEL_D)                                                    # side plates over the ribs
    p.box("root", (-4.6, 12.0, 2.05), (9.2, 11.4, 1.0), ENAMEL_B)                                                                       # the back plate
    p.box("root", (-0.8, 13.0, 3.0), (1.6, 9.8, 0.7), GOLD_P)                                                                           # the spine
    p.box("root", (-1.0, 17.4, 3.1), (2.0, 3.0, 0.8), GEM, rot=(0, 0, 45), pivot=(0, 19.0, 3.3))
    for k in range(3):                                                                                                                  # the sash line
        p.box("root", (-4.7 + k * 0.0, 20.0 - k * 1.2, 2.9), (9.4, 0.9, 0.5), GOLD)
    p.save()
    return p


def skirt():
    """A skirt of water cloth: long panels front, sides and back flowing from a gold belt, the front ones armoured with enamel tassets; they sway."""
    p = P("skirt", 3)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    p.box("root", (-4.75, 11.4, -2.75), (9.5, 1.3, 5.5), GOLD_P)
    tass = [("f0", -2.6, -3.0, 0), ("f1", 0.0, -3.3, 1), ("f2", 2.6, -3.0, 0)]
    for i, (n, cx, cz, k) in enumerate(tass):
        p.bone(n, (cx, 11.4, cz))
        p.box(n, (cx - 1.4, 4.6, cz - 0.6), (2.8, 6.8, 0.9), ENAMEL_D if k else ENAMEL_B, rot=(-9 - 4 * k, 0, 0), pivot=(cx, 11.4, cz))
        p.box(n, (cx - 1.4, 3.8, cz - 0.7), (2.8, 1.0, 0.9), GOLD_P, rot=(-9 - 4 * k, 0, 0), pivot=(cx, 11.4, cz))
        sine_rot(a, "idle", n, 2.6, 0, 4.0, phase=i * 0.12)
    sides = [("s0", -4.9, 0.0, -1), ("s1", 4.9, 0.0, 1), ("s2", -3.4, 2.7, -1), ("s3", 3.4, 2.7, 1)]
    for i, (n, cx, cz, sg) in enumerate(sides):
        p.bone(n, (cx, 11.4, cz))
        p.box(n, (cx - 0.5, 1.2, cz - 2.0), (1.0, 10.4, 4.0), CLOTH if i % 2 else CLOTH2, rot=(0, 0, 10 * sg), pivot=(cx, 11.4, cz))
        sine_rot(a, "idle", n, 2.6, 2, 6.0, phase=0.3 + i * 0.1)
    back = [("b0", -3.0, 3.2), ("b1", 0.0, 3.6), ("b2", 3.0, 3.2)]
    for i, (n, cx, cz) in enumerate(back):
        p.bone(n, (cx, 11.4, cz))
        p.box(n, (cx - 1.6, -0.6, cz - 0.4), (3.2, 12.2, 0.8), CLOTH2 if i % 2 else CLOTH, rot=(12, 0, 0), pivot=(cx, 11.4, cz))
        sine_rot(a, "idle", n, 2.6, 0, 5.0, phase=0.45 + i * 0.1)
    p.save(a)
    return p


def pauldron(side):
    """Three layered shells over the shoulder, a gold rim, three feathers rising from it."""
    n = "pauldron_" + ("l" if side else "r")
    p = P(n, 3)
    for k in range(3):
        mbox(p, "root", side, (-9.4 + 0.5 * k, 21.0 + 1.0 * k, -3.4 + 0.2 * k), (5.6 - 0.9 * k, 3.2, 6.8 - 0.4 * k), ENAMEL_B if k != 1 else ENAMEL, rot=(0, 0, 0))
    mbox(p, "root", side, (-9.9, 20.6, -3.5), (1.0, 3.4, 7.0), GOLD_P)
    mbox(p, "root", side, (-8.8, 24.4, -3.0), (4.2, 0.8, 6.0), GOLD)
    for k in range(3):
        mbox(p, "root", side, (-8.4 + 0.6 * k, 24.8, -1.6 + 1.6 * k), (0.7, 5.0 - 0.6 * k, 1.4), CRYSTAL_W, rot=(0, 0, -10 - 6 * k), pivot=(-8.0 + 0.6 * k, 24.8, -1.0 + 1.6 * k))
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = P(n, 3)
    mbox(p, "root", side, (-8.5, 14.4, -2.6), (5.0, 4.2, 5.2), ENAMEL_B)
    mbox(p, "root", side, (-8.7, 18.4, -2.8), (5.4, 0.9, 5.6), GOLD_P)
    mbox(p, "root", side, (-8.7, 14.0, -2.8), (5.4, 0.9, 5.6), GOLD_P)
    for k in range(3):                                                                                                                  # a fan of three feathers along the outside of the forearm, pointing back
        bx = -8.9 if side == 0 else 8.9
        p.box("root", (bx - (0.4 if side == 0 else 0.0), 15.6 + 1.2 * k, 0.4 + 0.4 * k), (0.4, 1.4, 5.0 - 0.6 * k), FEATHER_BF if side == 0 else FEATHER_B, rot=(-14 + 12 * k, 0, 0), pivot=(bx, 16.0, 1.0))
    p.save()
    return p


def gauntlet(side):
    n = "gauntlet_" + ("l" if side else "r")
    p = P(n, 3)
    mbox(p, "root", side, (-8.4, 11.6, -2.6), (4.8, 2.6, 5.2), ENAMEL)
    mbox(p, "root", side, (-8.5, 11.4, -3.0), (5.0, 0.8, 1.0), GOLD_P)
    for k in range(4):
        cx = (-8.0 + k * 1.15) if side == 0 else (4.2 - k * 1.15)
        p.box("root", (cx, 9.0, -2.6), (0.9, 2.8, 0.9), ENAMEL_B)
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = P(n, 3)
    mbox(p, "root", side, (-4.5, 7.0, -2.65), (4.9, 4.2, 5.3), ENAMEL_B)                                                              # the thigh guard
    mbox(p, "root", side, (-4.65, 6.4, -3.2), (5.2, 1.6, 1.0), GOLD_P)                                                                # the knee guard, a gold cap
    mbox(p, "root", side, (-4.45, 1.4, -2.55), (4.9, 4.8, 5.1), ENAMEL)                                                               # the shin plate
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.5, 6.0), GOLD_P)                                                                # the boot rim
    for k in range(3):                                                                                                                # the little wings at the ankle
        mbox(p, "root", side, (-6.0 - 0.6 * k, 2.4 + 0.9 * k, 0.8), (3.4 - 0.4 * k, 0.5, 1.7), FEATHER_B if side else FEATHER_BF, rot=(0, 0, -8 + 10 * k), pivot=(-4.6, 3.2 + 0.9 * k, 1.6))
    p.save()
    return p


def _wing(p, a, rows, P0, length, mats, arm=True, amp=7.0, yaw=24.0, zs=(0.0, -0.5, -1.0), clip="idle", d=3):
    """Layered feather wings: `rows` = [[(roll, length, width), ...], ...] one list per row (primaries, secondaries, coverts); both wings in one model (bones wr / wl); each feather is a flat
    cube on its own bone, hinged at the shoulder, every bone beats with a lag. mats[row] = (right material, left material)."""
    for side in (0, 1):
        sg = 1.0 if side else -1.0
        Px = sg * abs(P0[0])
        root = "wl" if side else "wr"
        p.bone(root, (Px, P0[1], P0[2]))
        sine_rot(a, clip, root, length, 2, -amp * sg, 0.0)
        sine_rot(a, clip, root, length, 1, amp * 0.5 * sg, 0.15)
        sine_rot(a, clip, root, length, 0, 2.0, 0.3)
        for r, row in enumerate(rows):
            z0 = P0[2] + zs[r]
            for i, (roll, L, w) in enumerate(row):
                b = "%sf%d_%d" % (root, r, i)
                p.bone(b, (Px, P0[1], z0), parent=root, rotation=(0, sg * (yaw + i * 2.5 + r * 3), -sg * roll))
                o = (Px - L, P0[1] - w / 2, z0) if side == 0 else (Px, P0[1] - w / 2, z0)
                p.box(b, o, (L, w, 0.5), mats[r][side], d=d)
                sine_rot(a, clip, b, length, 2, amp * 0.55 * sg, 0.18 + i * 0.045 + r * 0.04)
        if arm:                                                                                                                       # the wing's arm: two gold-and-pearl bones
            p.box(root, (Px - (11.0 if side == 0 else 0.0), P0[1] - 0.8, P0[2] - 0.5), (11.0, 1.6, 1.4), GOLD_P, rot=(0, 0, sg * 42), pivot=(Px, P0[1], P0[2]), d=3)


def _bird_wing(p, a, S, arm1, arm2, up1, up2, prim, sec, cov, mats, length=2.2, amp=6.0, zs=(0.0, 0.6, 1.2), d=2, clip="idle", arm_mat=None, yaw=10.0):
    """A skeletal wing (both sides in one model): shoulder -> elbow -> wrist bones (the arm leads, rising `up1` / `up2` degrees), the primaries hinged at the wrist, the secondaries and the coverts
    along the arm, each feather its own bone with a lag in the beat. prim = [(roll, length, width)], sec / cov = [(t along the arm 0..1 from the wrist back, roll, length, width)].
    mats = ((primary R, L), (secondary R, L), (covert R, L)). Rolls: negative is up, positive down (as feather_wing)."""
    for side in (0, 1):
        sg = 1.0 if side else -1.0
        Sx, Sy, Sz = sg * abs(S[0]), S[1], S[2]
        e = (Sx + sg * arm1 * math.cos(math.radians(up1)), Sy + arm1 * math.sin(math.radians(up1)))
        w = (e[0] + sg * arm2 * math.cos(math.radians(up2)), e[1] + arm2 * math.sin(math.radians(up2)))
        r, el, wh = ("wl", "el", "hl") if side else ("wr", "er", "hr")
        p.bone(r, (Sx, Sy, Sz))
        p.bone(el, (e[0], e[1], Sz), parent=r)
        p.bone(wh, (w[0], w[1], Sz), parent=el)
        sine_rot(a, clip, r, length, 2, -amp * sg, 0.0)
        sine_rot(a, clip, el, length, 2, -amp * 0.7 * sg, 0.10)
        sine_rot(a, clip, wh, length, 2, -amp * 0.8 * sg, 0.20)
        if arm_mat is not None:                                                                                                       # the arm of the wing: two gold bones
            p.box(r, (Sx - (arm1 if side == 0 else 0.0), Sy - 0.8, Sz - 0.6), (arm1, 1.6, 1.4), arm_mat, rot=(0, 0, sg * up1), pivot=(Sx, Sy, Sz), d=3)
            p.box(el, (e[0] - (arm2 if side == 0 else 0.0), e[1] - 0.7, Sz - 0.5), (arm2, 1.4, 1.2), arm_mat, rot=(0, 0, sg * up2), pivot=(e[0], e[1], Sz), d=3)
        # primaries: at the wrist
        for i, (roll, L, wd) in enumerate(prim):
            b = "%sp%d" % (wh, i)
            p.bone(b, (w[0], w[1], Sz), parent=wh, rotation=(0, sg * (yaw + i * 1.5), -sg * roll))
            o = (w[0] - L, w[1] - wd / 2, Sz + zs[0]) if side == 0 else (w[0], w[1] - wd / 2, Sz + zs[0])
            p.box(b, o, (L, wd, 0.5), mats[0][side], d=d)
            sine_rot(a, clip, b, length, 2, amp * 0.5 * sg, 0.30 + i * 0.04)
        for row, (items, z) in enumerate(((sec, zs[1]), (cov, zs[2]))):
            for i, (t, roll, L, wd) in enumerate(items):
                # t = 0 at the wrist, 1 at the shoulder, along wrist -> elbow -> shoulder
                if t <= 0.5:
                    k = t / 0.5
                    pv = (w[0] + (e[0] - w[0]) * k, w[1] + (e[1] - w[1]) * k)
                    par, parent_pivot = el, e
                else:
                    k = (t - 0.5) / 0.5
                    pv = (e[0] + (Sx - e[0]) * k, e[1] + (Sy - e[1]) * k)
                    par, parent_pivot = r, (Sx, Sy)
                b = "%s%s%d" % (wh, "s" if row == 0 else "c", i)
                p.bone(b, (pv[0], pv[1], Sz), parent=par, rotation=(0, sg * (yaw + 4 + i * 1.5), -sg * roll))
                o = (pv[0] - L, pv[1] - wd / 2, Sz + z) if side == 0 else (pv[0], pv[1] - wd / 2, Sz + z)
                p.box(b, o, (L, wd, 0.5), mats[1 + row][side], d=d)
                sine_rot(a, clip, b, length, 2, amp * 0.5 * sg, 0.15 + i * 0.04)


def wings():
    p = P("wings", 2)
    a = Anim()
    a.clip("idle", length=2.2, loop=True)
    prim = [(-40 + i * 8.5, L, 5.0) for i, L in enumerate([18, 22, 25, 27, 27, 26, 24, 21, 18])]
    sec = [(0.05 + i * 0.12, 52 + i * 3.0, 21 - i * 1.5, 4.8) for i in range(8)]
    cov = [(0.05 + i * 0.14, 38 + i * 4.0, 10 - i * 0.6, 4.0) for i in range(7)]
    _bird_wing(p, a, (3.0, 21.5, 3.4), 6.5, 9.0, 52, 22, prim, sec, cov, [(FEATHER_A, FEATHER_AF), (FEATHER_B, FEATHER_BF), (FEATHER_C, FEATHER_CF)], amp=6.0, arm_mat=GOLD_P)
    p.save(a)
    return p


def halo():
    """A gold halo ring over the head: eight plates and four drops of water on it, turning slowly."""
    p = P("halo", 3)
    a = Anim()
    a.clip("idle", length=14.0, loop=True)
    p.bone("spin", (0, 36.5, 0))
    for i in range(8):
        p.box("spin", (-2.2, 36.2, -8.0), (4.4, 0.7, 1.4), GOLD_P, rot=(0, i * 45.0, 0), pivot=(0, 36.5, 0))
    for i in range(4):
        p.box("spin", (-0.5, 36.9, -8.2), (1.0, 1.9, 1.0), GEM, rot=(0, i * 90.0 + 22.5, 0), pivot=(0, 36.5, 0))
    spin_y(a, "idle", "spin", 14.0, turns=1, steps=8)
    p.save(a)
    return p


# ---------------------------------------------------------------------------------------------------------------------- character pieces
def wings2():
    """The lower pair of the Saint Valkyrie's wings: smaller, swept down and back, gold-edged primaries."""
    p = P("wings2", 3)
    a = Anim()
    a.clip("idle", length=2.6, loop=True)
    prim = [(10 + i * 11.0, L, 4.2) for i, L in enumerate([11, 14, 17, 19, 20, 18, 14])]
    sec = [(14 + i * 12.0, L, 3.6) for i, L in enumerate([8, 10, 12, 13, 11])]
    _wing(p, a, [prim, sec], (2.6, 16.5, 3.9), 2.6, [(FEATHER_GA, FEATHER_GAF), (FEATHER_C, FEATHER_CF)], arm=False, amp=6.0, yaw=34.0, zs=(0.0, -0.5))
    p.save(a)
    return p


def aureole():
    """The aureole: a ring of plates behind the back with a sunburst of long blades; it turns slowly (the seal sprite sits inside it)."""
    p = P("aureole", 2)
    a = Anim()
    a.clip("idle", length=40.0, loop=True)
    cy, cz, R = 22.0, 9.5, 14.0
    p.bone("disc", (0, cy, cz))
    for i in range(18):
        p.box("disc", (-2.7, cy + R, cz), (5.4, 1.2, 0.8), GOLD_P if i % 2 else ENAMEL_B, rot=(0, 0, i * 20.0), pivot=(0, cy, cz))
    for i in range(16):
        L = 9.0 if i % 2 == 0 else 5.5
        p.box("disc", (-0.7, cy + R + 1.0, cz - 0.1), (1.4, L, 0.5), CRYSTAL_W if i % 2 else FEATHER_B, rot=(0, 0, i * 22.5 + 11.25), pivot=(0, cy, cz))
    a.rot("idle", "disc", {40.0 * i / 8: (0, 0, -360.0 * i / 8) for i in range(9)})
    p.save(a)
    return p


def sash():
    """A gold sash across the hip with five hanging feathers that sway."""
    p = P("sash", 3)
    a = Anim()
    a.clip("idle", length=2.2, loop=True)
    p.box("root", (-5.0, 11.7, -3.0), (10.0, 1.2, 0.9), GOLD_P)
    p.box("root", (3.2, 10.2, -3.1), (2.0, 2.2, 1.0), GEM, rot=(0, 0, 45), pivot=(4.2, 11.2, -2.6))
    for i in range(5):
        n = "t%d" % i
        x = 1.8 + i * 1.5
        p.bone(n, (x, 11.4, -3.1))
        p.box(n, (x - 0.6, 2.0, -3.4), (1.2, 9.6, 0.5), FEATHER_BF if i % 2 else FEATHER_B, d=3)
        sine_rot(a, "idle", n, 2.2, 0, 6.0, phase=i * 0.12)
        sine_rot(a, "idle", n, 2.2, 2, 3.0, phase=0.3 + i * 0.12)
    p.save(a)
    return p


def lance():
    """The Valkyrie's lance, hovering upright at the right hand: a gold shaft with a feathered guard and a great drill of water (seven cones, each turned a little more), spinning."""
    p = P("lance", 3)
    a = Anim()
    a.clip("idle", length=3.0, loop=True)
    cx, cz = -12.5, -5.0
    p.bone("lance", (cx, 18.0, cz), rotation=(-10, 0, 8))
    p.box("lance", (cx - 0.8, 2.0, cz - 0.8), (1.6, 24.0, 1.6), GOLD_P)
    p.box("lance", (cx - 1.6, 8.0, cz - 1.6), (3.2, 1.4, 3.2), GOLD)
    p.box("lance", (cx - 1.4, 25.8, cz - 1.4), (2.8, 1.4, 2.8), GOLD)
    for k in range(4):
        p.box("lance", (cx - 0.5, 24.8, cz - 4.6), (1.0, 4.6, 0.6), FEATHER_B, rot=(0, 90.0 * k, 0), pivot=(cx, 25.0, cz))
    p.bone("drill", (cx, 27.0, cz), parent="lance")
    for k in range(7):
        w = 6.6 - 0.85 * k
        p.box("drill", (cx - w / 2, 27.0 + k * 2.6, cz - w / 2), (w, 2.8, w), CRYSTAL, rot=(0, 18.0 * k, 0), pivot=(cx, 27.0 + k * 2.6, cz))
    spin_y(a, "idle", "drill", 3.0, turns=2, steps=8)
    a.pos("idle", "lance", {0.0: (0, 0, 0), 1.5: (0, 1.4, 0), 3.0: (0, 0, 0)})
    p.save(a)
    return p


STANDARD = [helm, chest, skirt, wings, halo]
PAIRED = [pauldron, vambrace, gauntlet, greave]
CHARACTER = [wings2, aureole, sash, lance]


def build():
    made = []
    for f in STANDARD + CHARACTER:
        made.append(f())
    for f in PAIRED:
        for s in (0, 1):
            made.append(f(s))
    return made

"""Lightning Magic (rebuild 1.93.0): the temporary armour of the Raging Lightning style (Luck Voltia's white-and-gold lightning armour): white enamel plate with gold chevrons and a
glowing bolt inlay, a winged circlet with a crown of bolts, swept pauldron spikes, boots with lightning fins, wings and a tail made of zig-zag bolts, a ring of electrodes over the
head and the Raijin thunder-drum ring behind the back. Pieces are GeoLite models in whole-body coordinates (tools/water_aura_lib.py); the painter is client/aura/LightningAura.java:

    python3 -B tools/gen_lightning_aura_geo.py && python3 -B tools/geo_check.py lightning_arm_helm ...
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from water_aura_lib import Mat, Piece, Anim, mbox, stepped_spike, sine_rot, spin_y, shell_texture  # noqa: E402

KEY = "lightning"

ENAMEL = Mat("bolt", "E6ECF8", dark="8794B4", light="FFFFFF", glow="6AB4FF", glow_amount=1.0, seed=71)
ENAMEL2 = Mat("plate", "DCE4F4", dark="7E8CB0", light="FFFFFF", glow="6AB4FF", glow_amount=0.6, inlay=1, seed=72)
GOLD = Mat("metal", "E8C050", dark="7A5A10", light="FFF4B0", glow="FFE070", glow_amount=0.55, seam=1, seed=73)
GOLD_P = Mat("plate", "EAC458", dark="805E14", light="FFF6B8", glow="FFE880", glow_amount=0.6, inlay=1, seed=74)
BOLT = Mat("flame", "8CC8FF", dark="2E6CE8", light="F4FCFF", glow="9CD4FF", glow_amount=1.0, seed=75, grain=0.03)
BOLT_W = Mat("plate", "EAF6FF", dark="9CC8FF", light="FFFFFF", glow="C8E8FF", glow_amount=1.0, seed=76)
DRUM = Mat("plate", "D8E0F2", dark="6A7AA4", light="FFFFFF", glow="7AC0FF", glow_amount=0.9, inlay=1, seed=77)


def zigzag(p, bone, base, length, width, n, mat, swing=2.6, lean_deg=24.0, thick=0.7, flat="z"):
    """A bolt: n cubes, each leaning alternately left and right, stacked from `base` along +y (or -y when length < 0); the whole bolt in the xy plane."""
    x, y, z = base
    seg = length / n
    for i in range(n):
        sg = 1 if i % 2 == 0 else -1
        h = abs(seg) * 1.25
        w = width * (1.0 - 0.55 * i / max(1, n - 1))
        oy = y + (seg * i if seg > 0 else seg * (i + 1))
        p.box(bone, (x + sg * swing * 0.5 - w / 2, oy, z - thick / 2), (w, h, thick), mat, rot=(0, 0, sg * lean_deg), pivot=(x + sg * swing * 0.5, oy + h / 2, z))


def helm():
    p = Piece(KEY, "arm_helm", density=3)
    p.box("root", (-4.75, 29.2, -4.75), (9.5, 1.8, 0.8), GOLD_P)                 # the circlet
    p.box("root", (-4.75, 29.2, 3.95), (9.5, 1.8, 0.8), GOLD_P)
    p.box("root", (-4.75, 29.2, -3.95), (0.8, 1.8, 7.9), GOLD_P)
    p.box("root", (3.95, 29.2, -3.95), (0.8, 1.8, 7.9), GOLD_P)
    for sx in (-1, 1):                                                          # swept-up wings at the temples
        p.box("root", (sx * 5.0 - 0.4, 28.8, -2.0), (0.8, 5.0, 4.0), ENAMEL, rot=(0, 0, -sx * 26), pivot=(sx * 5.0, 29.0, -1.0))
        p.box("root", (sx * 5.6 - 0.35, 31.4, -1.6), (0.7, 3.8, 3.0), ENAMEL2, rot=(0, 0, -sx * 44), pivot=(sx * 5.4, 31.0, -1.0))
    zigzag(p, "root", (0.0, 31.4, -2.0), 6.4, 2.2, 3, BOLT, swing=2.2)           # the crown of bolts
    zigzag(p, "root", (-3.0, 31.4, 0.8), 4.4, 1.7, 2, BOLT, swing=1.8)
    zigzag(p, "root", (3.0, 31.4, 0.8), 4.4, 1.7, 2, BOLT, swing=1.8)
    p.box("root", (-4.5, 31.0, -4.5), (9.0, 1.0, 9.0), ENAMEL2)
    p.box("root", (-0.8, 30.2, -5.0), (1.6, 1.6, 0.8), BOLT_W, rot=(0, 0, 45), pivot=(0, 31.0, -4.8))
    p.save()
    return p


def chest():
    p = Piece(KEY, "arm_chest", density=3)
    p.box("root", (-4.6, 16.4, -2.75), (9.2, 7.6, 1.1), ENAMEL)
    p.box("root", (-4.45, 14.7, -2.65), (8.9, 1.7, 0.95), GOLD_P)
    p.box("root", (-4.3, 12.9, -2.55), (8.6, 1.7, 0.85), ENAMEL2)
    p.box("root", (-2.6, 21.2, -3.2), (5.2, 0.9, 0.6), GOLD, rot=(0, 0, 14), pivot=(0, 21.6, -3.0))    # the gold chevrons on the breastplate
    p.box("root", (-2.6, 19.6, -3.2), (5.2, 0.9, 0.6), GOLD, rot=(0, 0, -14), pivot=(0, 20.0, -3.0))
    p.box("root", (-4.8, 23.4, -2.9), (9.6, 1.9, 5.8), GOLD)
    p.box("root", (-4.7, 12.2, 2.05), (9.4, 11.2, 0.95), ENAMEL2)
    p.box("root", (-0.6, 14.0, 3.0), (1.2, 8.0, 0.5), BOLT_W)
    mbox(p, "root", 0, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), ENAMEL2)
    mbox(p, "root", 1, (-4.95, 13.2, -1.9), (0.9, 8.6, 3.8), ENAMEL2)
    p.save()
    return p


def skirt():
    p = Piece(KEY, "arm_skirt", density=2)
    a = Anim()
    a.clip("idle", length=1.6, loop=True)
    p.box("root", (-4.7, 11.8, -2.65), (9.4, 1.4, 5.3), GOLD_P)
    for i, cx in enumerate((-2.9, 0.0, 2.9)):
        n = "f%d" % i
        p.bone(n, (cx, 11.8, -3.0))
        p.box(n, (cx - 1.4, 8.6, -3.4), (2.8, 3.3, 0.9), ENAMEL if i % 2 == 0 else ENAMEL2)
        sine_rot(a, "idle", n, 1.6, 0, 6.0, phase=i * 0.2)
    p.bone("b0", (0, 11.8, 2.9))
    p.box("b0", (-3.2, 8.6, 2.55), (6.4, 3.4, 0.9), ENAMEL2)
    sine_rot(a, "idle", "b0", 1.6, 0, -6.0, phase=0.4)
    p.save(a)
    return p


def pauldron(side):
    n = "pauldron_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-9.8, 21.0, -3.6), (5.6, 3.6, 7.2), ENAMEL)
    mbox(p, "root", side, (-9.3, 24.0, -3.2), (4.7, 1.3, 6.4), GOLD_P)
    mbox(p, "root", side, (-10.3, 18.9, -2.9), (1.0, 2.5, 5.8), ENAMEL2)
    for dz, L in ((-1.9, 4.6), (1.9, 3.4)):                                      # two swept spikes, raked outward and up
        mbox(p, "root", side, (-11.6, 24.0, dz - 0.8), (L, 1.6, 1.6), GOLD, rot=(0, 0, -22), pivot=(-8.5, 24.0, dz))
    p.save()
    return p


def vambrace(side):
    n = "vambrace_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.55, 14.3, -2.6), (4.9, 4.9, 5.2), ENAMEL)
    mbox(p, "root", side, (-8.75, 18.2, -2.8), (5.3, 1.0, 5.6), GOLD_P)
    bx = -8.7 if side == 0 else 8.7
    stepped_spike(p, "root", (bx, 16.6, 0.0), "-x" if side == 0 else "+x", 3.4, 2.4, GOLD, steps=2, thick=0.9)
    p.save()
    return p


def gauntlet(side):
    n = "gauntlet_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-8.5, 11.5, -2.6), (4.9, 2.8, 5.2), ENAMEL2)
    mbox(p, "root", side, (-8.65, 11.4, -3.0), (5.2, 1.0, 1.0), GOLD_P)
    for k in range(4):                                                          # gold finger plates
        cx = (-8.0 + k * 1.05) if side == 0 else (6.95 - k * 1.05)
        p.box("root", (cx, 8.8, -2.4 + 0.25 * (k % 2)), (0.95, 3.0, 0.8), GOLD)
    p.save()
    return p


def greave(side):
    n = "greave_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    mbox(p, "root", side, (-4.5, 6.4, -2.55), (4.9, 4.8, 5.1), ENAMEL)
    mbox(p, "root", side, (-4.4, 5.2, -3.1), (4.7, 2.2, 1.1), GOLD_P)
    mbox(p, "root", side, (-4.45, 1.5, -2.55), (4.9, 3.6, 5.1), ENAMEL2)
    mbox(p, "root", side, (-4.55, 0.0, -3.0), (5.1, 1.6, 6.0), GOLD)
    mbox(p, "root", side, (-5.9, 1.0, 0.0), (1.6, 4.6, 2.6), BOLT_W, rot=(0, 0, -14), pivot=(-4.6, 1.5, 1.0))        # the winged heel: thunder boots
    mbox(p, "root", side, (-6.4, 3.8, 0.4), (1.4, 3.4, 2.0), BOLT_W, rot=(0, 0, -30), pivot=(-4.6, 3.0, 1.0))
    p.save()
    return p


def wing(side):
    """Wings of lightning: three zig-zag bolts fanning out from the shoulder blade, crackling."""
    n = "wing_" + ("l" if side else "r")
    p = Piece(KEY, "arm_" + n, density=3)
    a = Anim()
    a.clip("idle", length=0.9, loop=True)
    sg = 1.0 if side else -1.0
    P = (sg * 2.4, 21.8, 2.6)
    p.bone("w", P)
    sine_rot(a, "idle", "w", 0.9, 2, -7.0 * sg, 0.0)
    for i, (roll, L) in enumerate(((-52, 21.0), (-22, 24.0), (8, 19.0))):
        b = "f%d" % i
        p.bone(b, P, parent="w", rotation=(0, sg * (22 + i * 4), -sg * roll))
        # a bolt lying along the outward axis: a zig-zag polyline, one cube per leg (the leg's own length and angle)
        k = 4
        seg = L / k
        pts = [(P[0], P[1])]
        for j in range(1, k + 1):
            off = (4.6 if j % 2 else -4.6) * (1.0 - 0.12 * j)
            pts.append((P[0] + sg * seg * j, P[1] + (off if j < k else 0.0)))
        for j in range(k):
            (x0, y0), (x1, y1) = pts[j], pts[j + 1]
            dx, dy = x1 - x0, y1 - y0
            ln = (dx * dx + dy * dy) ** 0.5
            ang = math.degrees(math.atan2(dy, dx))
            mx, my = (x0 + x1) / 2, (y0 + y1) / 2
            wd = 1.7 - 0.25 * j
            p.box(b, (mx - ln / 2 - 0.2, my - wd / 2, P[2]), (ln + 0.4, wd, 0.6), BOLT if j % 2 == 0 else BOLT_W, rot=(0, 0, ang), pivot=(mx, my, P[2]))
        sine_rot(a, "idle", b, 0.9, 2, 7.0 * sg, 0.1 + i * 0.12)
    p.box("w", (P[0] - (6.0 if sg < 0 else 0.0), P[1] - 0.6, P[2] - 0.2), (6.0, 1.2, 1.0), GOLD, rot=(0, 0, sg * 36), pivot=P)
    p.save(a)
    return p


def tail():
    """A tail of lightning: a zig-zag of gold-white cubes streaming back and down."""
    p = Piece(KEY, "arm_tail", density=3)
    a = Anim()
    a.clip("idle", length=1.2, loop=True)
    parent = "root"
    y, z = 11.0, 2.4
    for i in range(5):
        b = "t%d" % i
        p.bone(b, (0, y, z), parent=parent)
        s2 = 1 if i % 2 == 0 else -1
        p.box(b, (-1.0 + s2 * 0.6, y - 1.0, z), (2.0 - 0.2 * i, 2.0 - 0.2 * i, 4.4), BOLT if i % 2 == 0 else BOLT_W, rot=(14, s2 * 20, 0), pivot=(0, y, z))
        sine_rot(a, "idle", b, 1.2, 1, 11.0, phase=-i * 0.12)
        parent = b
        z += 3.6
        y -= 1.4
    p.save(a)
    return p


def halo():
    """A ring of electrodes over the head, spinning fast: eight gold rods with a bolt cube on each."""
    p = Piece(KEY, "arm_halo", density=3)
    a = Anim()
    a.clip("idle", length=4.0, loop=True)
    p.bone("spin", (0, 34.5, 0))
    for i in range(8):
        p.box("spin", (-1.5, 34.0, -7.4), (3.0, 0.9, 2.2), GOLD_P, rot=(0, i * 45.0, 0), pivot=(0, 34.5, 0))
        p.box("spin", (-0.5, 34.9, -7.0), (1.0, 2.2 + (i % 2) * 1.2, 1.0), BOLT_W, rot=(0, i * 45.0, 0), pivot=(0, 34.5, 0))
    spin_y(a, "idle", "spin", 4.0, turns=1, steps=8)
    p.save(a)
    return p


def backdrop():
    """The Raijin thunder drums: seven drums on a ring behind the back, turning, joined by a gold ring."""
    p = Piece(KEY, "arm_backdrop", density=3)
    a = Anim()
    a.clip("idle", length=10.0, loop=True)
    cy, cz = 19.0, 9.5
    p.bone("disc", (0, cy, cz))
    for i in range(7):
        ang = i * 360.0 / 7
        p.box("disc", (-1.5, cy + 7.2, cz - 0.7), (3.0, 3.0, 2.8), DRUM, rot=(0, 0, ang), pivot=(0, cy, cz))
        p.box("disc", (-1.0, cy + 7.7, cz - 1.0), (2.0, 2.0, 0.5), BOLT_W, rot=(0, 0, ang), pivot=(0, cy, cz))
    for i in range(4):
        p.box("disc", (-2.6, cy + 7.9, cz), (5.2, 0.9, 0.6), GOLD_P, rot=(0, 0, i * 90.0 + 25.0), pivot=(0, cy, cz))
    a.rot("idle", "disc", {10.0 * i / 8: (0, 0, 360.0 * i / 8) for i in range(9)})
    p.save(a)
    return p


def shells():
    shell_texture("lightning_shell", Mat("bolt", "C8D4EC", dark="5F6E96", light="F4F8FF", glow="6AB4FF", glow_amount=1.0, alpha=160, seed=81))
    shell_texture("lightning_haze", Mat("bolt", "9CCCFF", dark="3A78F0", light="FFFFFF", glow="BCE0FF", glow_amount=1.0, alpha=255, seed=82))


def main():
    shells()
    made = [helm(), chest(), skirt()]
    for s in (0, 1):
        made += [pauldron(s), vambrace(s), gauntlet(s), greave(s), wing(s)]
    made += [tail(), halo(), backdrop()]
    total = 0
    for m in made:
        print("%-28s cubes %3d" % (m.id, m.cube_count()))
        total += m.cube_count()
    print("total cubes", total)


if __name__ == "__main__":
    main()

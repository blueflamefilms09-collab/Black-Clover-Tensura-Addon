"""Why: the generic LICH of the abyssal bosses (D&D's lich: a skeletal spellcaster in tattered robes, a spiked crown, a phylactery on its belt, soul fire in the sockets, a bone staff that floats
at its side, a ghostly aura). Built as the owner asked: a 64x64 player skin (skull, ribs, robe layers) plus separate GeoLite extras (crown, phylactery, skirt panels, staff, wisps, shoulder
spikes, eye flames) keyed by the look id; the three phases are three skins and three extras sets (phase 2: the phylactery cracks and the soul fire flares, phase 3: it breaks and leaks).
Reused by Acererak, the Lich King and Vecna through `ornaments()` and `base_rig()`.
"""
import math
import lib
from lib import (Humanoid, AnimRig, over, on, rgb, shade, mix, bone, cloth, metal, emissive, glowfill, wisp, halo, flat, plate, humanoid_motion, beam, ring, spike, rotpt, TAU, keys, smooth)
import undead

LOOK = dict(id="lich", name="Lich", kind="humanoid", slim=False, height=2.0, glow="#7dffa8", boost=1.0, dust=True, shadow=0.55, grow_xz=2.4, grow_up=1.8, undead=True,
            captions=["", "The phylactery cracks, and soul fire pours out of the lich's sockets.", "The phylactery breaks. The lich burns with everything it has bound."])

P = dict(id="lich", bone="#d9d1b4", dark="#120c18", robe="#3a2c58", trim="#8e66d8", eye="#7dffa8", eye2="#b46cff", soul="#5cff98", rune="#b46cff", gem="#7dffa8", belt="#1c1420",
         buckle="#9a8a5a")


def panel(c, name, x, w, z, thick, top, length, paint, parent="body", tilt=0.0):
    c.bone(name, parent, pivot=(x, top, z))
    c.box(name, (x - w / 2, top - length, z - thick / 2), (w, length, thick), paint, k=2)


def skirt(c, P, phase, hem=None, length=10.5, wide=1.0):
    """Hanging robe panels from the hips: a ragged front, back and two sides, each its own bone so they swing apart."""
    rob = cloth(P["robe"], 3, tatter=3, trim=P["trim"])
    names = []
    lens = [9.0, 11.5, 8.5]
    for i, x in enumerate((-2.9 * wide, 0.0, 2.9 * wide)):
        panel(c, "x_skirt_f%d" % i, x, 2.9 * wide + 0.3, -2.75, 0.8, 12.3, lens[i] * length / 10.5, rob)
        panel(c, "x_skirt_b%d" % i, x, 2.9 * wide + 0.3, 2.75, 0.8, 12.3, lens[(i + 1) % 3] * length / 10.5, rob)
        names += ["x_skirt_f%d" % i, "x_skirt_b%d" % i]
    for i, s in enumerate((-1, 1)):
        c.bone("x_skirt_s%d" % i, "body", pivot=(s * 4.4, 12.3, 0))
        c.box("x_skirt_s%d" % i, (s * 4.4 - 0.4, 12.3 - 9.0 * length / 10.5, -2.5), (0.8, 9.0 * length / 10.5, 5), rob, k=2)
        names.append("x_skirt_s%d" % i)
    return names


def crown(c, P, phase, tall=1.0, metal_c="#8a7a58", gem="#7dffa8", rays=False):
    mt = metal(metal_c, edge=shade(metal_c, 1.5))
    c.bone("x_crown", "head", pivot=(0, 32, 0))
    ring(c, "x_crown", 32.3, 1.7, 4.5, 4.5, 0.7, mt)
    pts = [((0, -5.1), (0, 0, 0)), ((-3.6, -4.6), (0, 0, 10)), ((3.6, -4.6), (0, 0, -10)), ((-5.1, 0), (0, 0, 14)), ((5.1, 0), (0, 0, -14)),
           ((-3.4, 4.7), (-8, 0, 8)), ((3.4, 4.7), (-8, 0, -8)), ((0, 5.1), (-14, 0, 0))]
    for i, ((x, z), r) in enumerate(pts):
        hh = (5.6 if i == 0 else 4.2) * tall
        spike(c, "x_crown", (x, 33.6, z), hh, 1.5, mt, rot=r, n=3, taper=0.55)
    c.box("x_crown", (-0.7, 32.7, -5.45), (1.4, 1.3, 0.6), emissive(gem), k=1)
    for x in (-2.4, 2.4):
        c.box("x_crown", (x - 0.5, 32.9, -5.3), (1.0, 1.0, 0.5), emissive(P["eye2"]), k=1)
    return ["x_crown"]


def eye_fire(c, P, phase, scale=1.0, colors=None, which=(0, 1)):
    cols = colors or (P["eye"], P["eye2"])
    names = []
    for i, x in enumerate((-2.0, 2.0)):
        if i not in which:
            continue
        n = "x_eyefire%d" % i
        c.bone(n, "head", pivot=(x, 28.6, -4.4))
        h = (2.2 + 1.3 * (phase - 1)) * scale
        c.box(n, (x - 0.8, 28.6, -5.0), (1.6, h, 1.0), glowfill(cols[0]), k=1)
        c.box(n, (x - 0.5, 28.6 + h, -4.8), (1.0, h * 0.5, 0.8), glowfill(cols[1]), k=1)
        names.append(n)
    return names


def wisps(c, P, phase, count=None, colors=None, height=(9, 16), radius=(9.5, 13.5)):
    """Ghostly aura: tall glow-only strips that drift up round the body (additive pass only, so they read as light, not as cubes)."""
    cols = colors or (P["eye"], P["eye2"])
    n = count or (4, 8, 12)[phase - 1]
    names = []
    for i in range(n):
        a = TAU * (i + 0.37 * (i % 3)) / n
        r = radius[0] + (radius[1] - radius[0]) * ((i * 7) % 5) / 4
        x, z = math.cos(a) * r, math.sin(a) * r
        h = height[0] + (height[1] - height[0]) * ((i * 5) % 4) / 3
        nm = "x_wisp%d" % i
        c.bone(nm, "root", pivot=(x, 4 + (i % 4) * 4, z))
        c.box(nm, (x - 0.9, 4 + (i % 4) * 4, z - 0.9), (1.8, h, 1.8), wisp(cols[i % 2]), k=2)
        names.append(nm)
    return names


def phylactery(c, P, phase, anchor_x=3.6):
    """The soul vessel on a chain at the belt: whole in phase 1, cracked with a glowing seam in 2, split open and leaking in 3."""
    gold = metal("#a89050", edge="#e8d890")
    glow = P["rune"]
    c.bone("x_phyl", "body", pivot=(anchor_x, 14.4, -3.0))
    for i in range(3):
        c.box("x_phyl", (anchor_x - 0.3, 14.0 - i * 1.0 - 0.9, -3.3), (0.6, 0.9, 0.6), gold, k=1)
    cx, cy, cz = anchor_x, 10.2, -3.1
    core = emissive(glow, soft=0.2)
    if phase == 1:
        c.box("x_phyl", (cx - 1.4, cy - 1.2, cz - 1.4), (2.8, 2.4, 2.8), core, k=2)
        c.box("x_phyl", (cx - 0.9, cy + 1.2, cz - 0.9), (1.8, 1.0, 1.8), gold, k=1)
        c.box("x_phyl", (cx - 0.9, cy - 2.2, cz - 0.9), (1.8, 1.0, 1.8), gold, k=1)
        for dx in (-1.6, 1.6):
            c.box("x_phyl", (cx + dx - 0.2, cy - 1.6, cz - 1.5), (0.4, 3.2, 0.4), gold, k=1)
    elif phase == 2:
        for sgn in (-1, 1):                                   # two halves with a glowing seam between them
            c.box("x_phyl", (cx + sgn * 0.45 - (1.5 if sgn < 0 else 0), cy - 1.3, cz - 1.5), (1.5, 2.6, 3.0), over(core, on(("front", "back"), lambda cv: cv.line(0, 0, cv.w - 1, cv.h - 1, "#102010", "#ffffff"))), k=2)
        c.box("x_phyl", (cx - 0.25, cy - 1.8, cz - 1.0), (0.5, 3.6, 2.0), glowfill("#ffffff"), k=1)
        c.box("x_phyl", (cx - 1.0, cy + 1.3, cz - 1.0), (2.0, 0.9, 2.0), gold, k=1)
        c.box("x_phyl", (cx - 1.0, cy - 2.2, cz - 1.0), (2.0, 0.9, 2.0), gold, k=1)
        c.box("x_phyl", (cx - 0.3, cy + 2.0, cz - 0.3), (0.6, 4.5, 0.6), glowfill(glow), k=1)
    else:
        c.box("x_phyl", (cx - 2.6, cy - 1.0, cz - 1.3), (1.6, 2.2, 2.6), core, k=2, rot=(0, 0, 18), pivot=(cx, cy, cz))
        c.box("x_phyl", (cx + 1.0, cy - 1.0, cz - 1.3), (1.6, 2.2, 2.6), core, k=2, rot=(0, 0, -18), pivot=(cx, cy, cz))
        c.box("x_phyl", (cx - 1.0, cy + 1.3, cz - 1.0), (2.0, 0.9, 2.0), gold, k=1)
        c.box("x_phyl", (cx - 0.6, cy - 2.5, cz - 0.6), (1.2, 4.0, 1.2), glowfill("#ffffff"), k=1)
        c.box("x_phyl", (cx - 0.5, cy + 2.0, cz - 0.5), (1.0, 12.0, 1.0), glowfill(glow), k=1)
        for i in range(4):
            c.bone("x_phylshard%d" % i, "body", pivot=(cx, cy, cz))
            a = TAU * i / 4
            c.box("x_phylshard%d" % i, (cx + math.cos(a) * 3.2 - 0.5, cy + (i % 2) * 2 - 1, cz + math.sin(a) * 3.2 - 0.5), (1.0, 1.4, 1.0), core, k=1)
    return ["x_phyl"] + (["x_phylshard%d" % i for i in range(4)] if phase == 3 else [])


def staff(c, P, phase, x=-11.0, z=-3.0, orb="#7dffa8", length=29.0):
    """A bone staff that floats at the lich's right side instead of being held: skull and curled prongs round a soul orb on top, knuckle rings down the shaft."""
    bn = bone("#d3cbb0", 0.06)
    c.bone("x_staff", "root", pivot=(x, 16, z))
    y0 = 2.5
    c.box("x_staff", (x - 0.7, y0, z - 0.7), (1.4, length, 1.4), bn, k=2)
    for yy in (7.0, 14.0, 20.0):
        c.box("x_staff", (x - 1.1, yy, z - 1.1), (2.2, 1.2, 2.2), bone("#b8b094", 0.08), k=1)
    top = y0 + length
    c.box("x_staff", (x - 1.6, top, z - 1.6), (3.2, 2.6, 3.2), bn, k=2)                      # the skull
    c.box("x_staff", (x - 1.3, top + 0.5, z - 1.7), (0.9, 0.9, 0.5), emissive(P["eye"]), k=1)
    c.box("x_staff", (x + 0.4, top + 0.5, z - 1.7), (0.9, 0.9, 0.5), emissive(P["eye"]), k=1)
    for s in (-1, 1):
        beam(c, "x_staff", (x + s * 1.1, top + 2.4, z), 2.4, 0.9, 0.9, (0, 0, -s * 22), bn, k=1)
        beam(c, "x_staff", (x + s * 1.1 + s * 0.9, top + 4.7, z), 1.8, 0.8, 0.8, (0, 0, s * 18), bn, k=1)
    c.box("x_staff", (x - 1.3, top + 3.0, z - 1.3), (2.6, 2.6, 2.6), emissive(orb, soft=0.15), k=1)
    c.box("x_staff", (x - 2.0, top + 2.4, z - 2.0), (4.0, 4.0, 4.0), halo(orb, 0.9), k=1) if phase >= 2 else None
    return ["x_staff"]


def shoulders(c, P, phase, spiked=True, color="#d3cbb0"):
    bn = bone(color, 0.06)
    names = []
    for s, arm in ((-1, "arm_r"), (1, "arm_l")):
        c.box(arm, (s * 7.5 - 2.0 if s < 0 else 5.5, 22.2, -2.9), (3.6 if s > 0 else 3.6, 1.4, 5.8), bn, k=1) if False else None
        c.box(arm, (s * 6.5 - 2.4, 21.6, -2.9), (4.8, 1.6, 5.8), bn, k=2)
        if spiked:
            spike(c, arm, (s * 7.4, 23.0, -1.4), 5.0, 1.6, bn, rot=(0, 0, -s * 16), n=3)
            spike(c, arm, (s * 7.4, 23.0, 1.4), 4.0, 1.4, bn, rot=(0, 0, -s * 16), n=3)
    return names


def ornaments(c, P, phase, **kw):
    """The lich's hanging extras, registered on the creature's list."""
    c.note_extra("skirt", "body", skirt(c, P, phase, wide=kw.get("wide", 1.0)))
    c.note_extra("crown", "head", crown(c, P, phase, gem=P["gem"]))
    c.note_extra("eye_fire", "head", eye_fire(c, P, phase))
    c.note_extra("phylactery", "body", phylactery(c, P, phase))
    c.note_extra("staff", "root", staff(c, P, phase, orb=P["eye"]))
    c.note_extra("wisps", "root", wisps(c, P, phase))
    shoulders(c, P, phase)
    c.note_extra("shoulder_spikes", "arm_r", [])


def build(phase):
    sk = undead.make_skin(P, phase)
    c = Humanoid("lich", phase, sk)
    ornaments(c, P, phase)
    base_rig(c, phase, RIG)
    return c


def base_rig(c, phase, R=None):
    """Secondary motion of the lich's hanging extras (registered only for bones this model has)."""
    R = R or RIG

    def S(bone_, *a, **k):
        if bone_ in c.m.bones:
            R.sway(bone_, *a, **k)
    for i in range(3):
        S("x_skirt_f%d" % i, 0, 5, 1, 0.1 * i)
        S("x_skirt_b%d" % i, 0, -5, 1, 0.1 * i + 0.3)
    for i in range(2):
        S("x_skirt_s%d" % i, 2, 5 if i else -5, 1, 0.2 * i)
    S("x_staff", 2, 4, 1, 0.0)
    S("x_staff", 1, 6, 1, 0.25)
    S("x_staff", 1, 1.4, 1, 0.0, kind="pos")
    S("x_phyl", 2, 8, 2, 0.0)
    S("x_phyl", 0, 4, 2, 0.25)
    S("x_crown", 1, 1.0, 1, 0.0)
    for i in range(4):
        S("x_phylshard%d" % i, 1, 70, 1, 0.0)
    for i in range(12):
        S("x_wisp%d" % i, 1, 3.0 + (i % 3), 1, i * 0.083, kind="pos")
        S("x_wisp%d" % i, 3, 0.25, 2, i * 0.11, kind="scale")
    for i in range(2):
        S("x_eyefire%d" % i, 3, 0.22, 3, i * 0.3, kind="scale")


def _extra(clip, t, L):
    d = {}
    if clip == "cast":
        d["x_staff"] = {"pos": (0, 3, -4), "rot": (-18, 0, 0)}
    if clip == "cast_big":
        k = t / L
        up = keys(k, [(0.0, 0.0), (0.35, 1.0), (0.8, 1.1), (0.9, 0.0), (1.0, 0.0)])
        d["x_staff"] = {"pos": (6 * up, 6 * up, -6 * up), "rot": (-25 * up, 0, 0)}
        d["x_crown"] = {"pos": (0, 1.2 * up, 0)}
    if clip == "death":
        k = t / L
        f = smooth(k / 0.8)
        d["x_staff"] = {"pos": (0, -14 * f, 0), "rot": (0, 0, 40 * f)}
        d["x_crown"] = {"pos": (0, -2 * f, 0), "rot": (20 * f, 0, 14 * f)}
        for i in range(12):
            d["x_wisp%d" % i] = {"pos": (0, 6 * f, 0)}
    if clip in ("walk", "run"):
        d["x_staff"] = {"pos": (0, 0, -1.5 if clip == "run" else 0)}
    return d


RIG = AnimRig(humanoid_motion(_extra, stride=26, arm_swing=18, bob=0.4))

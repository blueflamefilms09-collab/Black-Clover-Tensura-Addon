"""The Spirit of Space, both forms (docs/magic/spirit_space.md): the geo models of the orb's rift ring and of the wisp-dragon's small horned head, their animations, their sheets and their levels of detail.

    python3 -B tools/gen_spirit_space_geo.py            writes assets/nusmp/{geo,animations,textures}/entity/spirit_space_{orb,wisp}[_lod1|_lod2|_fx]*
    python3 -B tools/gen_spirit_space_geo.py --count    prints the quads of every model

    spirit_space_orb       LOD 0   the rift ring: a ring of 16 cracked-glass shards and 5 fragments, drawn ADDITIVE (the dark of the sheet adds nothing), animated: the ring spins, the fragments orbit
    spirit_space_orb_lod1  LOD 1   10 shards, 2 fragments            spirit_space_orb_lod2  LOD 2   6 shards
    spirit_space_wisp      LOD 0   the head of the wisp: skull, muzzle, jaw, two horns with an antler prong each, two whiskers of three segments each (cutout, with the thin cel linework)
    spirit_space_wisp_lod1 / _lod2 fewer parts                         spirit_space_wisp_fx   the glow-only model: the eyes, the throat, the horn tips, the whisker beads, the mane and cheek fins (additive)

The BODIES of both forms are not geo models: the orb's core is a UV sphere and the wisp's body a tube along an undulating spine, both drawn in the client by client.prop.SpiritSpaceDraw as shells of
scrolling starfield (the three layers of parallax). Authored at 4 times the size (units are 1/16 block, front -z, y up) and drawn at 0.25 so that the sheets carry four times the detail.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import spirit_space_lib as L  # noqa: E402
import spirit_space_paint as P  # noqa: E402


# ====================================================================================================================== the orb
def orb_atlas():
    at = L.Atlas(256, 128)
    at.region("crystal_a", 128, 128, lambda w, h: P.crystal(w, h, "crystal_a", 1.0))
    at.region("crystal_b", 128, 128, lambda w, h: P.crystal(w, h, "crystal_b", 0.85))
    return at


def build_orb(lod, atlas):
    """The rift ring. lod 0: 16 shards on a tilted ring and 5 orbiting fragments; lod 1: 10 shards and 2 fragments; lod 2: 6 shards."""
    ident = "spirit_space_orb" + ("" if lod == 0 else "_lod%d" % lod)
    m = L.Model(ident, atlas, seed=100 + lod, tex_scale={0: 1.0, 1: 0.5, 2: 0.25}[lod])
    m.bone("tilt", (0, 0, 0), "root", rotation=(24, 0, 8))
    m.bone("ring", (0, 0, 0), "tilt")
    rng = np.random.default_rng(77)
    n = {0: 16, 1: 10, 2: 6}[lod]
    faces5 = {"up": ("crystal_a", 5), "north": ("crystal_b", 5), "south": ("crystal_a", 5), "west": ("crystal_b", 5), "east": ("crystal_a", 5)}
    faces3 = {"up": ("crystal_a", 5), "north": ("crystal_b", 5), "south": ("crystal_a", 5)}
    faces = faces3 if lod == 2 else faces5
    R = 29.0
    for i in range(n):
        th = 360.0 * i / n
        ln = float(rng.uniform(8, 13)) * (1.0 + (16 - n) * 0.04)          # tangent length: fewer shards are a little longer
        rad = float(rng.uniform(2.0, 3.4))
        hv = float(rng.uniform(1.8, 2.8))
        jr = float(rng.uniform(-2.0, 2.0))
        m.cube("ring", (R + jr - rad / 2, -hv / 2, -ln / 2), (rad, hv, ln), faces, rot=(float(rng.uniform(-14, 14)), th, float(rng.uniform(-16, 16))), pivot=(0, 0, 0))
    nf = {0: 5, 1: 2, 2: 0}[lod]
    for i in range(nf):
        m.bone("frag%d" % i, (0, 0, 0), "root")
        r = 40.0 + 5.0 * (i % 3)
        s = 3.2 + (i % 2) * 1.0
        m.cube("frag%d" % i, (r - s / 2, -s * 0.9, -s / 2), (s, s * 1.8, s), faces5, rot=(0, float(i * 50), float(20 * (i % 2) - 10)), pivot=(r, 0, 0))
    m.save()
    return m


def orb_animations():
    T = 8.0
    clips = {}
    ts = [T * i / 8 for i in range(9)]
    bones = {"ring": {"rotation": {t: (0, 360.0 * t / T, 0) for t in ts}},
             "tilt": {"rotation": L.merge(L.sine(T, 5.0, 0, 0.0, offset=(24, 0, 8)), L.sine(T, 3.0, 2, 0.25))},
             "root": {"scale": {t: (1.0 + 0.035 * math.sin(t / T * math.tau * 2), 1.0 + 0.035 * math.sin(t / T * math.tau * 2), 1.0 + 0.035 * math.sin(t / T * math.tau * 2)) for t in ts}}}
    for i in range(5):
        k = 1.0 + 0.35 * i
        bones["frag%d" % i] = {"rotation": {t: (28.0 * math.sin(t / T * math.tau + i), 360.0 * t / T * k * (1 if i % 2 == 0 else -1) / 1.0, 18.0 * math.cos(t / T * math.tau * 2 + i)) for t in ts}}
    clips["idle"] = {"loop": True, "length": T, "bones": bones}
    CT = 1.0                                                                    # cast: the ring flares out and spins up, then settles; the fragments fly out
    tt = [0.0, 0.2 * CT, 0.55 * CT, CT]
    cb = {"ring": {"rotation": {0.0: (0, 0, 0), 0.2: (0, 120, 0), 0.55: (0, 400, 0), 1.0: (0, 720, 0)}},
          "tilt": {"rotation": dict(zip(tt, [(24, 0, 8), (38, 0, 4), (30, 0, 6), (24, 0, 8)])), "scale": dict(zip(tt, [(1, 1, 1), (1.35, 1.35, 1.35), (1.2, 1.2, 1.2), (1, 1, 1)]))}}
    for i in range(5):
        cb["frag%d" % i] = {"position": dict(zip(tt, [(0, 0, 0), (0, 0, 0), (0, 0, 0), (0, 0, 0)])), "scale": dict(zip(tt, [(1, 1, 1), (1.8, 1.8, 1.8), (1.4, 1.4, 1.4), (1, 1, 1)]))}
    clips["cast"] = {"loop": False, "length": CT, "bones": cb}
    return clips


# ====================================================================================================================== the wisp's head
def wisp_atlas():
    at = L.Atlas(512, 512)
    z = lambda w, h: np.zeros((h, w, 4), np.float32)
    at.region("skin", 128, 128, lambda w, h: (P.vgrad(P.dark_surface(w, h, "wisp_skin", lift=0.18), 1.1, 0.7), z(w, h)))
    at.region("head_r", 96, 72, lambda w, h: P.head_side(w, h, mirror=False))
    at.region("head_l", 96, 72, lambda w, h: P.head_side(w, h, mirror=True))
    at.region("head_top", 84, 96, lambda w, h: _flip(P.head_top(w, h)))
    at.region("snout_r", 72, 45, lambda w, h: P.snout(w, h, "side", mirror=False))
    at.region("snout_l", 72, 45, lambda w, h: P.snout(w, h, "side", mirror=True))
    at.region("snout_top", 54, 72, lambda w, h: _flip(P.snout(w, h, "top")))
    at.region("snout_front", 44, 36, lambda w, h: P.snout(w, h, "front"))
    at.region("jaw_r", 132, 17, lambda w, h: P.jaw(w, h, mirror=False))
    at.region("jaw_l", 132, 17, lambda w, h: P.jaw(w, h, mirror=True))
    at.region("jaw_in", 47, 132, lambda w, h: _flip(P.jaw_in(w, h)))
    at.region("horn", 48, 64, P.horn)
    at.region("whisker", 16, 48, P.whisker)
    at.region("fin", 64, 64, P.fin_glow)
    at.region("eye", 24, 24, lambda w, h: P.glow_solid(w, h, "cyan", "pale"))
    at.region("tip", 24, 24, lambda w, h: P.glow_solid(w, h, "violet", "cyan", 0.9))
    return at


def _flip(pair):
    c, g = pair
    return np.flipud(c).copy(), np.flipud(g).copy()


def build_wisp(lod, atlas, fx=False):
    """The wisp's head. lod 0 = full, 1 = fewer parts, 2 = the skull and muzzle only; fx = only the glow parts, same bones."""
    ident = "spirit_space_wisp" + ("_fx" if fx else "" if lod == 0 else "_lod%d" % lod)
    m = L.Model(ident, atlas, seed=300 + lod, tex_scale=0.5 if fx else {0: 1.0, 1: 0.5, 2: 0.25}[lod])

    def C(bone, o, s, faces, rot=None, pivot=None, d=0):
        if not fx:
            m.cube(bone, o, s, faces, rot, pivot, d)

    def X(bone, o, s, faces, rot=None, pivot=None):
        if fx:
            m.cube(bone, o, s, faces, rot, pivot, 0)

    sk = ("skin", 5.0)
    m.bone("head", (0, 0, 0), "root")
    # the skull and the muzzle
    C("head", (-7, -6, -16), (14, 12, 16), {"up": "head_top", "west": "head_r", "east": "head_l", "down": sk, "north": ("skin", 5.0)})
    C("head", (-5, -5, 0), (10, 10, 6), {"up": sk, "west": sk, "east": sk, "down": sk})                                             # the neck collar the body tube plugs into
    C("head", (-4.5, -4, -28), (9, 7.5, 12), {"up": "snout_top", "west": "snout_r", "east": "snout_l", "down": sk})
    C("head", (-3.6, -3.6, -36), (7.2, 6, 8), {"up": ("snout_top", 4), "west": ("snout_r", 6), "east": ("snout_l", 6), "north": "snout_front", "down": sk})
    if lod < 2:
        C("head", (-2, 3.6, -34), (4, 2, 5), {"up": ("snout_top", 4), "west": ("skin", 5), "east": ("skin", 5), "north": ("skin", 5)})              # the nose bump
        for sgn in (-1, 1):                                                                                                        # upper fangs, cheek spines
            C("head", (sgn * 2.6 - 0.6, -6.6, -35.4), (1.2, 3.2, 1.2), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)})
            if lod == 0:
                C("head", (sgn * 7.0 - 0.8, -1.5, -1.0), (1.6, 1.6, 8), {"up": ("horn", 6), "down": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)}, rot=(0, sgn * -24, 0), pivot=(sgn * 7.0, -1.5, -1.0))
        for sgn in (-1, 1):                                                                                                        # the brow ridges
            C("head", (sgn * 5.2 - 1.5, 4.8, -15), (3, 2.4, 8), {"up": sk, "west" if sgn < 0 else "east": sk, "north": sk})
    # the jaw (opens)
    m.bone("jaw", (0, -4.2, -12), "head")
    C("jaw", (-3.9, -6.9, -35), (7.8, 2.8, 22), {"up": "jaw_in", "west": "jaw_r", "east": "jaw_l", "down": sk, "north": sk})
    if lod == 0:
        for sgn in (-1, 1):
            C("jaw", (sgn * 3.0 - 0.5, -5.2, -34), (1, 2.6, 1), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)})
    X("jaw", (-3.0, -4.3, -33), (6, 0.5, 18), {"up": "tip"})                                                                         # the glow of the throat
    # the horns: three segments each, swept back and up; an antler prong on the first
    for sgn, side in ((-1, "r"), (1, "l")):
        base_rot = (-52, sgn * 8, sgn * 20)
        m.bone("horn_%s1" % side, (sgn * 4.4, 5.0, -3.0), "head", rotation=base_rot)
        C("horn_%s1" % side, (sgn * 4.4 - 1.5, 5.0, -4.5), (3, 9, 3), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)})
        if lod == 0:
            C("horn_%s1" % side, (sgn * 4.4 + (1.2 if sgn > 0 else -3.6), 8.0, -4.0), (2.4, 4.5, 2), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)}, rot=(-20, 0, sgn * 24), pivot=(sgn * 4.4, 8.0, -3.0))
        m.bone("horn_%s2" % side, (sgn * 4.4, 14.0, -3.0), "horn_%s1" % side, rotation=(-16, 0, 0))
        C("horn_%s2" % side, (sgn * 4.4 - 1.1, 14.0, -4.1), (2.2, 8, 2.2), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6)})
        if lod < 2:
            m.bone("horn_%s3" % side, (sgn * 4.4, 22.0, -3.0), "horn_%s2" % side, rotation=(-20, 0, 0))
            C("horn_%s3" % side, (sgn * 4.4 - 0.7, 22.0, -3.7), (1.4, 7, 1.4), {"north": ("horn", 6), "south": ("horn", 6), "west": ("horn", 6), "east": ("horn", 6), "up": ("horn", 6)})
            X("horn_%s3" % side, (sgn * 4.4 - 0.9, 27.6, -3.9), (1.8, 2.6, 1.8), {"*": "tip"})                                         # the glowing tip
    # the whiskers: three thin segments each, they sway
    if lod < 2:
        for sgn, side in ((-1, "r"), (1, "l")):
            m.bone("whisk_%s1" % side, (sgn * 4.4, -1.5, -27), "head", rotation=(8, sgn * -52, sgn * 6))
            C("whisk_%s1" % side, (sgn * 4.4 - 0.5, -2.0, -27), (1, 1, 9), {"up": "whisker", "down": "whisker", "west": "whisker", "east": "whisker"})
            m.bone("whisk_%s2" % side, (sgn * 4.4, -1.5, -18), "whisk_%s1" % side, rotation=(10, sgn * -22, 0))
            C("whisk_%s2" % side, (sgn * 4.4 - 0.5, -2.0, -18), (1, 1, 9), {"up": "whisker", "down": "whisker", "west": "whisker", "east": "whisker"})
            m.bone("whisk_%s3" % side, (sgn * 4.4, -1.5, -9), "whisk_%s2" % side, rotation=(12, sgn * -22, 0))
            C("whisk_%s3" % side, (sgn * 4.4 - 0.5, -2.0, -9), (1, 1, 9), {"up": "whisker", "down": "whisker", "west": "whisker", "east": "whisker"})
            X("whisk_%s3" % side, (sgn * 4.4 - 0.8, -2.3, 0), (1.6, 1.6, 1.6), {"*": "tip"})
    # the glow parts: eyes, mane, cheek fins
    for sgn in (-1, 1):
        X("head", (sgn * 7.15 - (0.0 if sgn > 0 else 0.35), -0.8, -13.6), (0.5, 3.0, 5.0), {"west" if sgn < 0 else "east": "eye"})
    if lod < 2:
        for i, (hgt, ln, z0) in enumerate(((13, 13, -2), (11, 12, 4), (9, 10, 10))):                                                   # the mane: three blades
            m.bone("mane%d" % i, (0, 6, z0), "head", rotation=(-34 - 6 * i, 0, 0))
            X("mane%d" % i, (-0.4, 6, z0), (0.8, hgt, ln), {"west": "fin", "east": "fin"})
        for sgn, side in ((-1, "r"), (1, "l")):
            m.bone("cheek_%s" % side, (sgn * 6.5, -2.0, -3), "head", rotation=(0, sgn * -30, sgn * 6))
            X("cheek_%s" % side, (sgn * 6.5 - (0 if sgn > 0 else 9), -2.4, -6), (9, 0.8, 8), {"up": "fin", "down": "fin"})
    m.save()
    return m


def wisp_animations():
    clips = {}
    T = 3.2
    st = np.linspace(0, T, 13)
    bones = {"head": {"rotation": L.merge(L.sine(T, 3.0, 1, 0.0, steps=12), L.sine(T, 2.0, 0, 0.2, steps=12))},
             "jaw": {"rotation": L.sine(T, 1.5, 0, 0.3, steps=12, bias=3.0)}}
    for side, sgn in (("r", -1), ("l", 1)):
        for i in range(1, 4):
            bones["horn_%s%d" % (side, i)] = {"rotation": L.sine(T, 2.0 + i, 0, 0.1 * i, steps=12)}
            bones["whisk_%s%d" % (side, i)] = {"rotation": L.merge(L.sine(T, 9.0 + 3 * i, 1, -0.15 * i + (0.0 if sgn < 0 else 0.5), steps=12), L.sine(T, 4.0, 0, 0.2 * i, steps=12))}
        bones["cheek_" + side] = {"rotation": L.sine(T, 7.0, 2, 0.0 if sgn < 0 else 0.5, steps=12)}
    for i in range(3):
        bones["mane%d" % i] = {"rotation": L.sine(T, 5.0, 0, 0.08 * i, steps=12, offset=(-34 - 6 * i, 0, 0))}
    clips["idle"] = {"loop": True, "length": T, "bones": bones}
    CT = 1.2                                                                    # cast: the head rears, the jaw opens wide, the mane flares, the whiskers fling out, then back
    tt = [0.0, 0.2 * CT, 0.45 * CT, 0.75 * CT, CT]
    cb = {"head": {"rotation": dict(zip(tt, [(0, 0, 0), (-16, 0, 0), (6, 0, 0), (2, 0, 0), (0, 0, 0)]))},
          "jaw": {"rotation": dict(zip(tt, [(3, 0, 0), (10, 0, 0), (34, 0, 0), (20, 0, 0), (3, 0, 0)]))}}
    for i in range(3):
        cb["mane%d" % i] = {"rotation": dict(zip(tt, [(-34 - 6 * i, 0, 0), (-60 - 8 * i, 0, 0), (-18 - 4 * i, 0, 0), (-30 - 5 * i, 0, 0), (-34 - 6 * i, 0, 0)]))}
    for side, sgn in (("r", -1), ("l", 1)):
        for i in range(1, 4):
            cb["whisk_%s%d" % (side, i)] = {"rotation": dict(zip(tt, [(0, 0, 0), (0, sgn * 30, 0), (-8, sgn * -20, 0), (0, sgn * 6, 0), (0, 0, 0)]))}
        cb["cheek_" + side] = {"rotation": dict(zip(tt, [(0, 0, 0), (0, 0, sgn * 24), (0, 0, sgn * -8), (0, 0, sgn * 4), (0, 0, 0)]))}
    clips["cast"] = {"loop": False, "length": CT, "bones": cb}
    return clips


# ====================================================================================================================== main
def main():
    count_only = "--count" in sys.argv
    rows = []
    at = orb_atlas()
    for lod in (0, 1, 2):
        m = build_orb(lod, at) if not count_only else _count(build_orb, lod, at)
        rows.append((m.id, m.cubes(), m.quads()))
        if not count_only:
            at.save_color(m.id, m.tex_scale)
    if not count_only:
        L.write_animations("spirit_space_orb", orb_animations())
    aw = wisp_atlas()
    for lod, fx in ((0, False), (1, False), (2, False), (0, True)):
        m = build_wisp(lod, aw, fx)
        rows.append((m.id, m.cubes(), m.quads()))
        if not count_only:
            if fx:
                aw.save_glow(m.id, 0.5)
            else:
                aw.save_color(m.id, m.tex_scale)
    if not count_only:
        L.write_animations("spirit_space_wisp", wisp_animations())
    for r in rows:
        print("%-28s cubes %4d  quads %5d" % r)


def _count(fn, lod, at):
    return fn(lod, at)


if __name__ == "__main__":
    main()

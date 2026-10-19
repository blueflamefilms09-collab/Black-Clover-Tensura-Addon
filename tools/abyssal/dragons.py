"""Why: Tiamat (five heads), Bahamut (one, sleek, whiskered), the Dracolich (all bone and soul fire) and the Tarrasque (a horned, plated, wingless horror) are four-legged scaled beasts with a neck,
a head with a hinged jaw, a tail and often wings: one builder with a spec for each so they share the skeleton, the gait and the clip names. Everything is in the file frame (px, y up, front -z).

Skeleton: root > body > (neck_<h> chain > head_<h> > jaw_<h>), (tail chain), wing_r / wing_l (+2), and root > leg_fl / leg_fr / leg_bl / leg_br each with a shin_ child.
"""
import math
import lib
from lib import (Creature, over, on, shade, flat, scales, emissive, halo, wisp, bone, metal, flesh, membrane, TAU, keys, smooth, tilt_rot, horn, spike, beam, bat_wing, feathers)
import beasts as B


def head(c, parent, pre, base, pal, s=1.0, skeletal=False, kind="dragon", whiskers=False, crest=False, glow_mouth=False):
    """A dragon head whose back of skull is at `base` (straight layout, snout towards -z). pal: scale, belly, horn, eye, tooth. Returns the jaw bone name."""
    x, y, z = base
    sc = pal["scale"]
    mat = bone(pal.get("bone", "#d6d0b4"), 0.06) if skeletal else scales(sc, 3)
    tooth = bone(pal.get("tooth", "#e8e0c8"), 0.05)
    hornm = bone(pal["horn"], 0.1)
    sk, hs = (1.0, 1.0)
    if kind == "tarrasque":
        sk, hs = 1.35, 1.2
    c.box(parent, (x - 3.6 * s * sk, y - 3.0 * s * hs, z - 8.0 * s), (7.2 * s * sk, 6.4 * s * hs, 8.0 * s), mat, k=2)
    c.box(parent, (x - 2.7 * s * sk, y - 2.6 * s * hs, z - 15.0 * s), (5.4 * s * sk, 4.6 * s * hs, 7.2 * s), mat, k=2)
    c.box(parent, (x - 2.9 * s * sk, y + 0.9 * s * hs, z - 15.4 * s), (5.8 * s * sk, 0.9 * s, 7.6 * s), hornm if not skeletal else mat, k=1)             # the brow of the snout
    for sx in (-1, 1):
        c.box(parent, (x + sx * 1.3 * s - 0.5 * s, y + 0.6 * s, z - 15.6 * s), (1.0 * s, 1.0 * s, 0.6 * s), flat("#10080a", 0.05), k=1)              # nostrils
        c.box(parent, (x + sx * 3.4 * s * sk - (0.7 * s if sx > 0 else 0.1 * s), y + 0.2 * s, z - 7.0 * s), (0.8 * s, 1.6 * s, 2.4 * s), emissive(pal["eye"], pal["eye"], soft=0.1), k=1)
        c.box(parent, (x + sx * 3.9 * s * sk - (1.6 * s if sx > 0 else 0), y - 0.6 * s, z - 8.6 * s), (1.6 * s, 4.0 * s, 4.4 * s), halo(pal["eye"], 0.35), k=1)
        c.box(parent, (x + sx * 3.2 * s * sk - (1.0 * s if sx > 0 else 0.0), y + 1.8 * s, z - 8.0 * s), (1.0 * s, 1.0 * s, 4.8 * s), hornm if not skeletal else mat, k=1)
        horn(c, parent, (x + sx * 2.6 * s * sk, y + 2.4 * s * hs, z - 1.5 * s), sx * 0.45, 1.0, [4.0 * s, 4.0 * s, 3.2 * s], [1.8 * s, 1.3 * s, 0.9 * s], [52, 64, 74], hornm, k=1)
    jaw = pre + "_jaw"
    c.bone(jaw, parent, pivot=(x, y - 2.6 * s * hs, z - 4.0 * s))
    c.box(jaw, (x - 2.5 * s * sk, y - 4.6 * s * hs, z - 14.0 * s), (5.0 * s * sk, 2.0 * s, 11.0 * s), mat, k=2)
    c.box(jaw, (x - 2.0 * s * sk, y - 3.0 * s * hs, z - 13.0 * s), (4.0 * s * sk, 0.8 * s, 9.0 * s), flat(pal.get("mouth", "#8a2a3a"), 0.08), k=1)
    if glow_mouth:
        c.box(jaw, (x - 2.0 * s * sk, y - 2.4 * s * hs, z - 12.0 * s), (4.0 * s * sk, 1.4 * s, 8.0 * s), halo(pal["eye"], 1.0), k=1)
    for i in range(3):
        zz = z - (3.5 + i * 3.6) * s
        h = 1.7 * s * (1 if i % 2 == 0 else 0.7)
        for sx in (-1, 1):
            c.box(parent, (x + sx * 2.3 * s * sk - 0.4 * s, y - 2.6 * s * hs - h + 0.2, zz - 0.4 * s), (0.8 * s, h, 0.8 * s), tooth, k=1)
            c.box(jaw, (x + sx * 2.1 * s * sk - 0.4 * s, y - 3.0 * s * hs, zz - 0.4 * s), (0.8 * s, h * 0.9, 0.8 * s), tooth, k=1)
    if whiskers:
        for sx in (-1, 1):
            for j, (dy, ang) in enumerate(((0.5, 40), (-0.9, 62))):
                p = (x + sx * 2.8 * s, y + dy * s, z - 13.0 * s)
                for ln, wd, a in ((5 * s, 0.7, ang), (5 * s, 0.6, ang + 22), (5 * s, 0.5, ang + 44), (5 * s, 0.4, ang + 66)):
                    p = beam(c, parent, p, ln, wd, wd, (0, 0, -sx * a), hornm, k=1)
    if crest:
        for i in range(5):
            spike(c, parent, (x - 0.5 * s, y + 3.0 * s * hs, z - (1 + i * 1.8) * s), (4.0 - i * 0.5) * s, 1.1 * s, hornm, rot=(-10, 0, 0), n=2)
    return jaw


def dragon(c, spec):
    """Builds the dragon of `spec`. Returns {necks: {h: [bones]}, jaws: [bones], tail: [bones], legs: (fl, fr, bl, br), wings: [(bone, side)]}.
    spec: pal (main palette), heads (list of dict(pal, angle, rise, len, s)), W, H, skeletal, wings (None | dict), tail_len, carapace, kind."""
    pal = spec["pal"]
    W, H = spec.get("W", 18), spec.get("H", 14)
    skeletal = spec.get("skeletal", False)
    kind = spec.get("kind", "dragon")
    c.bone("root", pivot=(0, 0, 0))
    c.bone("body", "root", pivot=(0, 24, 0))
    scl = bone(pal.get("bone", "#d6d0b4"), 0.06) if skeletal else scales(pal["scale"], 3)
    belly = bone("#b8b094", 0.08) if skeletal else scales(pal["belly"], 3)
    y0 = 18.0
    if skeletal:
        c.box("body", (-1.4, y0 + H - 3.0, -16), (2.8, 3.0, 34), scl, k=2)                                  # the spine
        for i in range(9):
            zz = -14 + i * 3.4
            for sx in (-1, 1):
                p = (sx * 1.4, y0 + H - 2.0, zz)
                for ln, wd, a in ((4.0, 1.2, 62), (4.5, 1.1, 30), (4.0, 1.0, -8)):
                    p = beam(c, "body", p, ln, wd, wd, (0, 0, -sx * a), scl, k=1, along="-y")
        c.box("body", (-W / 2 + 3, y0 + 2, -8), (W - 6, H - 6, 16), halo(pal["eye"], 0.9), k=1)
        c.box("body", (-3.0, y0 + 4.0, -4.0), (6.0, 6.0, 6.0), emissive(pal["eye"], pal["eye"], soft=0.2), k=1)
        c.box("body", (-3.5, y0 - 1, 13.0), (7.0, 5.0, 6.0), scl, k=2)                                      # pelvis
    else:
        c.box("body", (-W / 2, y0, -16), (W, H, 18), scl, k=2)
        c.box("body", (-W / 2 + 0.5, y0, 0), (W - 1.0, H - 1.0, 16), scl, k=2)
        c.box("body", (-W / 2 + 1.5, y0 - 0.8, -15), (W - 3.0, 2.4, 30), belly, k=2)
    nb = max(5, int(30 / 5))
    for i in range(nb):                                                                                     # the dorsal ridge
        zz = -14 + i * 5.0
        spike(c, "body", (0, y0 + H - 0.2, zz), 3.8 if i % 2 == 0 else 3.0, 1.8, bone(pal["horn"], 0.1), rot=(-8, 0, 0), n=2)
    if kind == "tarrasque":
        sh = flesh("#4a3a2c", "#2a2018", 0.01)
        plate_m = over(flesh("#5a4636", "#2a2018", 0.01), on(("top",), lambda cv: cv.rect(1, 1, cv.w - 2, 1, "#8a7050")))
        for i in range(5):
            zz = -15 + i * 6.2
            c.box("body", (-W / 2 - 1.0, y0 + H - 3.0 + (i % 2), zz), (W + 2.0, 4.2, 6.6), plate_m, k=2, rot=(0, 0, 0))
            for sx in (-1, 1):
                spike(c, "body", (sx * (W / 2 + 0.5), y0 + H + 0.5, zz + 3.3), 5.0, 2.4, bone("#8a7050", 0.1), rot=(0, 0, -sx * 28), n=3)
            c.box("body", (-3.0, y0 + H + 0.6 + (i % 2), zz + 0.4), (6.0, 0.6, 5.6), halo(pal["eye"], 0.65), k=1)
    # legs
    legs = []
    for nm, sx, zz in (("leg_fl", 1, -10), ("leg_fr", -1, -10), ("leg_bl", 1, 9), ("leg_br", -1, 9)):
        t = 5.6 if kind == "tarrasque" else 4.6
        x = sx * (W / 2 - 1.0)
        c.bone(nm, "root", pivot=(x, 22.0, zz))
        c.box(nm, (x - t / 2, 13.0, zz - t / 2), (t, 9.5, t), scl, k=2)
        c.bone("shin_" + nm[4:], nm, pivot=(x, 13.0, zz), rotation=(-18, 0, 0))
        c.box("shin_" + nm[4:], (x - t / 2 + 0.3, 3.5, zz - t / 2 + 0.3), (t - 0.6, 9.8, t - 0.6), scl, k=2)
        c.box("shin_" + nm[4:], (x - t / 2 - 0.5, 0.0, zz - t / 2 - 3.0), (t + 1.0, 3.6, t + 3.2), scl, k=2)
        for cl in (-1, 0, 1):
            c.box("shin_" + nm[4:], (x + cl * 1.5 - 0.5, 0.0, zz - t / 2 - 4.6), (1.0, 1.6, 2.0), bone(pal.get("tooth", "#e8e0c8"), 0.05), k=1)
        legs.append(nm)
    # necks and heads
    necks, jaws = {}, []
    for h, hs in enumerate(spec["heads"]):
        ang = hs.get("angle", 0)
        rise = hs.get("rise", 1.0)
        ln = hs.get("len", [6, 6, 6, 5])
        wd = hs.get("w", [5.8, 5.2, 4.6, 4.0])
        s = hs.get("s", 1.0)
        start = (hs.get("x", 0.0), y0 + H - 3.0, -15.0)
        names = []
        px, py, pz = start
        par = "body"
        for i, (l_, w_) in enumerate(zip(ln, wd)):
            bn = "neck_%d_%d" % (h, i)
            c.bone(bn, par, pivot=(px, py, pz), rotation=((36.0, 12.0, -20.0, -28.0)[min(i, 3)] * rise, ang if i == 0 else -ang * 0.26, 0))
            c.box(bn, (px - w_ / 2, py - w_ / 2, pz - l_), (w_, w_, l_), hs["pal"].get("neck") and scales(hs["pal"]["neck"], 3) or (bone(pal.get("bone", "#d6d0b4"), 0.06) if skeletal else scales(hs["pal"]["scale"], 3)), k=2)
            if i % 2 == 0 and not skeletal:
                spike(c, bn, (px, py + w_ / 2 - 0.2, pz - l_ / 2), 2.4, 1.4, bone(hs["pal"]["horn"], 0.1), rot=(-10, 0, 0), n=2)
            names.append(bn)
            pz -= l_
            par = bn
        hp = hs["pal"]
        jaws.append(head(c, par, "head_%d" % h, (px, py, pz), hp, s=s, skeletal=skeletal, kind=kind, whiskers=hs.get("whiskers", False), crest=hs.get("crest", False), glow_mouth=hs.get("glow_mouth", False)))
        necks[h] = names
    # the tail
    tl = spec.get("tail", [8, 8, 8, 7, 7, 6, 6, 5, 5])
    tw = [W * 0.55 * (1 - i / (len(tl) + 1)) + 1.6 for i in range(len(tl))]
    tmat = bone(pal.get("bone", "#d6d0b4"), 0.06) if skeletal else scales(pal["scale"], 3)
    tail = c.chain("tail", "body", (0, y0 + H * 0.55, 15.0), "+z", tl, tw, tmat, curl=(-3, 0, 0), k=2)
    spike_m = bone(pal["horn"], 0.1)
    last = tail[-1]
    ex = 15.0 + sum(tl)
    if kind == "tarrasque":
        c.box(last, (-5.0, y0 + H * 0.55 - 5.0, ex - 1.0), (10.0, 10.0, 9.0), flesh("#5a4636", "#2a2018", 0.01), k=2)
        for sx in (-1, 1):
            spike(c, last, (sx * 5.0, y0 + H * 0.55, ex + 3.0), 6.0, 2.4, spike_m, rot=(0, 0, -sx * 70), n=3)
        spike(c, last, (0, y0 + H * 0.55 + 5.0, ex + 3.0), 6.0, 2.4, spike_m, rot=(0, 0, 0), n=3)
    elif spec.get("tail_barb"):
        spike(c, last, (0, y0 + H * 0.55, ex), 7.0, 3.0, spike_m, rot=(90, 0, 0), n=3)
    # the wings
    wings = []
    w = spec.get("wings")
    if w:
        if w["type"] == "bat":
            for nm, sd in (("wing_r", -1), ("wing_l", 1)):
                wings += bat_wing(c, "body", nm, (sd * 4.5, y0 + H - 1.5, -5.0), sd, w["span"], bone(pal.get("wingbone", "#2a1c1c"), 0.1), membrane(w["membrane"], w.get("vein", None), ragged=w.get("ragged", False)),
                                  lift=w.get("lift", 22), droop=w["span"] * 0.8, sweep=w.get("sweep", 24))
        else:
            f1, f2 = feathers(w["c1"], w.get("tip"), w.get("glow")), feathers(w["c2"], w.get("tip"), w.get("glow"))
            for nm, sd in (("wing_r", -1), ("wing_l", 1)):
                wings += lib.feather_wing(c, "body", nm, (sd * 4.5, y0 + H - 1.5, -5.0), sd, w["span"], f1, f2, rows=6, height=w["span"] * 0.8, sweep=w.get("sweep", 26), raise_=w.get("raise", 20))
    return {"necks": necks, "jaws": jaws, "tail": tail, "legs": legs, "wings": [(n, -1 if n.endswith("_r") else 1) for n in wings if not n.endswith("2")]}


def rig(R, info, tail_amp=6.0, neck_amp=5.0, wing_amp=7.0):
    """The secondary motion every dragon shares: the tail rolls, the necks snake (each its own phase), the wings breathe."""
    B.sway_names(R, info["tail"], 1, tail_amp, 1, 0.16, 0, 3)
    for h, names in info["necks"].items():
        B.sway_names(R, names, 1, neck_amp, 1, 0.1, 0, 3, phase=h * 0.2)
    for n, sd in info["wings"]:
        R.sway(n, 2, wing_amp * sd, 1, 0.0)
        R.sway(n + "2", 2, wing_amp * 1.4 * sd, 1, 0.14)
    for j in info["jaws"]:
        R.sway(j, 0, -3, 2, 0.0)


def extra_motion(info):
    """Jaws open on an attack, a cast and a death; the heads of a many-headed beast strike one after another."""
    heads = len(info["jaws"])

    def extra(clip, t, L):
        d = {}
        k = t / L
        if clip == "attack":
            for h, j in enumerate(info["jaws"]):
                lg = keys((k - 0.07 * h) % 1.0 if k >= 0.07 * h else 0.0, [(0, 0.0), (0.25, 0.1), (0.42, 1.0), (0.75, 0.2), (1, 0.0)])
                d[j] = {"rot": (-40 * lg, 0, 0)}
        if clip in ("cast", "cast_big"):
            up = 0.7 if clip == "cast" else keys(k, [(0, 0), (0.35, 1.0), (0.8, 1.2), (0.9, 0), (1, 0)])
            for j in info["jaws"]:
                d[j] = {"rot": (-34 * up, 0, 0)}
        if clip == "death":
            for j in info["jaws"]:
                d[j] = {"rot": (-30 * smooth(k / 0.6), 0, 0)}
        return d
    return extra

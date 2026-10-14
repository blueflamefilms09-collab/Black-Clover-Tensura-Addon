"""djinn_unique: the per-Djinn armour builders (Half Equip and Full Equip), the second generation of tools/relics/djinn_parts.py.

djinn_parts.py builds ONE rig for all twelve Djinn and varies it by palette and a style row. This module gives every Djinn its OWN builders:
its own headgear (horns / crest / crown / mask), wings, weapon, back piece, tail, halo, mantle, pauldron and chest emblem. The old rig and
its meshes (assets/nusmp/djinn/<id>/*.txt) stay in the repo untouched as the fallback the game uses when a unique piece is missing.

Coordinates: "author space" exactly as in djinn_parts.py (pixels = 1/16 block, x to the viewer's right = the character's left, y UP, z toward
the viewer = the front), each piece authored in the frame of the model part it is drawn on. Pieces written to assets/nusmp/djinn/u_<id>/ (the
unique look) and d_<id>/ (the Dark Djinn look). The WEAPON piece is the one exception to the author -> model flip: it is drawn in the held item's
own space (y up, the grip at the origin, the blade along +y), so it is written as authored.

Proportion notes (measured on the owner's references; the player model is 32 px tall, the head 8 px):
    ref 59 tile 1 (Baal): the ring behind the figure is about 0.9 x the head-to-hip height (24 px of 26)            -> BAAL_RING_R = 12
    ref 59 tile 9 (Paimon): a moon disc of about 1.1 x the head width sits behind the head                           -> PAIMON_MOON_R = 5.6
    ref 61 (Sitri): the halo is 1.6 x the head width (255 of 160 px), a thin tilted ring; hair falls to the knee      -> HALO_R = 6.4, hair 30 px
    ref 62 (the eye crest): 190 px wide on a 210 px eye distance, so about as wide as the head (8 px x 0.6)           -> EYE_W = 5.0, EYE_H = 2.4
    ref 60 (black wings): the wing is as long as the whole figure (about 1.2 x body height)                          -> wing span 25-30 px
The preview tool (gen_djinn_unique.py) renders each one beside the reference crops.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                   # noqa: E402
import djinn_parts as dp                                # noqa: E402
from djinn_parts import leaf, spike, collar, rune, C, S, lerp3, rgb   # noqa: E402,F401

PIECES_NEW = ["weapon", "eye", "backdrop", "mantle"]
PIECES = dp.PIECES + PIECES_NEW
INNER = 11.0          # the wing's inner segment (the game moves the outer segment 11 px along it)
OUTER = 14.0


# ================================================================ small geometry helpers
def curve(points, n=8):
    """dp.curve at half the density (about 2 samples a span): the armour is drawn every frame for every bearer in sight."""
    return dp.curve(points, max(2, n // 2))


def unit(v):
    v = np.asarray(v, float)
    n = float(np.linalg.norm(v))
    return v / n if n > 1e-9 else v


def arc_path(base, ang, length, curl, n=6, plane="xy", sx=1.0):
    """A path of n+1 points from `base` heading `ang` degrees from +x in the plane (xy | xz | yz), turning by `curl` degrees over `length`.
    sx mirrors the x of the heading (the right-hand wing)."""
    pts, a, p = [], ang, np.asarray(base, float)
    for i in range(n + 1):
        pts.append(tuple(p))
        a = ang + curl * (i + 0.5) / n
        r = math.radians(a)
        c, s = math.cos(r), math.sin(r)
        d = {"xy": (sx * c, s, 0.0), "xz": (sx * c, 0.0, s), "yz": (0.0, c, s)}[plane]
        p = p + np.array(d) * (length / n)
    return pts


def pf_feather(t):
    return (0.45 + 0.55 * min(1.0, t * 5.0)) * (1.0 - t ** 2.2)


def pf_blade(t):
    return min(1.0, 0.35 + t * 4.0) * (1.0 - t ** 1.5)


def pf_flame(t):
    return (math.sin(math.pi * min(1.0, t ** 0.7)) ** 0.8) * (1.0 - 0.15 * t) + 0.04 * (1 - t)


def pf_ribbon(t):
    return 0.45 + 0.55 * math.sin(min(1.0, t * 1.1) * math.pi * 0.85) * (1.0 - 0.25 * t)


def pf_taper(t):
    return max(0.0, 1.0 - t)


def pf_flat(t):
    return 1.0


UP = {"xy": (0, 0, 1), "xz": (0, 1, 0), "yz": (1, 0, 0)}


def sweep_flat(part, path, w, t, prof=pf_feather, up=(0, 0, 1), caps=(False, False), section=None):
    """A flat tapering blade swept along `path`: width w * prof(s) in the plane the up vector is normal to, thickness t."""
    sec = section or ml.lozenge(1.0, 1.0)
    ml.sweep(part, path, sec, scales=lambda s: (max(0.0, w * prof(s)), max(0.0, t * (0.3 + 0.7 * prof(s)))), caps=caps, up=up)


def feather(part, base, ang, length, w, t, curl=0.0, plane="xy", n=5, prof=pf_feather, sx=1.0, caps=(False, False)):
    sweep_flat(part, arc_path(base, ang, length, curl, n, plane, sx), w, t, prof, UP[plane], caps)


def ring_path(center, R, a0, a1, plane="xy", n=12):
    """An arc of a circle: angles in degrees measured in the plane's own (first axis, second axis) pair."""
    cx, cy, cz = center
    out = []
    for a in np.linspace(math.radians(a0), math.radians(a1), n + 1):
        c, s = R * math.cos(a), R * math.sin(a)
        out.append({"xy": (cx + c, cy + s, cz), "xz": (cx + c, cy, cz + s), "yz": (cx, cy + c, cz + s)}[plane])
    return out


def sweep_tube(part, path, r0, r1, sides=6, base_flare=0.0):
    """dp.sweep_tube with half the stations (2 per span) and open ends: the armour's tubes are thin, the long ones cost the most."""
    pts = curve(path, 4)

    def sc(t):
        s = r0 + (r1 - r0) * t
        s *= 1 + base_flare * max(0.0, 1 - t * 5)
        return (s, s)
    ml.sweep(part, pts, C(1.0, max(5, sides)), scales=sc, caps=(False, False))


def tube_direct(part, pts, r0, r1, sides=5):
    """A tube straight along already dense points (an arc_path / ring_path): no smoothing, open ends, closes to the thin end."""
    n = len(pts)
    ml.sweep(part, pts, C(1.0, max(4, sides)), scales=lambda t: (r0 + (r1 - r0) * t,) * 2, caps=(False, False))


def tube(part, path, r0, r1=None, sides=6, mid=1.0):
    """A round tube along a short control path (Catmull-Rom smoothed), radius r0 -> r1."""
    r1 = r0 if r1 is None else r1
    pts = curve(path, 4)

    def sc(t):
        s = r0 + (r1 - r0) * t
        return (s * (1 + (mid - 1) * math.sin(math.pi * t)), s * (1 + (mid - 1) * math.sin(math.pi * t)))
    ml.sweep(part, pts, C(1.0, sides), scales=sc)


def tongue(part, base, direction, length, w, t=0.5, wobble=1.0, phase=0.0, plane="xy", n=6, prof=pf_flame, caps=(False, False)):
    """A flame / water tongue: a flat blade whose spine wobbles sideways (an S in the plane)."""
    d = unit(direction)
    side = np.array([-d[1], d[0], 0.0]) if plane == "xy" else np.array([0.0, -d[2], d[1]]) if plane == "yz" else np.array([-d[2], 0.0, d[0]])
    path = []
    for i in range(n + 1):
        s = i / n
        path.append(tuple(np.asarray(base, float) + d * length * s + side * wobble * math.sin(s * math.pi * 1.6 + phase) * s))
    sweep_flat(part, path, w, t, prof, UP[plane], caps)


def sweep(part, path, section, scales=None, caps=(False, False), up=None):
    """ml.sweep with open ends by default (the armour is drawn every frame: a cap nobody sees is a wasted quad)."""
    return ml.sweep(part, path, section, scales=scales, caps=caps, up=up)


def zig(part, pts, w, h, up=(0, 0, 1)):
    """A zigzag bar through the points (lightning): ONE sweep along the polyline with a lozenge section, closing to a point at both ends."""
    P = [np.asarray(p, float) for p in pts]
    n = len(P)
    seg = [float(np.linalg.norm(P[i + 1] - P[i])) for i in range(n - 1)]
    cum = np.concatenate([[0.0], np.cumsum(seg)])
    L = max(float(cum[-1]), 1e-6)

    def sc(t, w=w, h=h):
        s = float(cum[min(int(round(t * (n - 1))), n - 1)]) / L
        f = min(1.0, 0.25 + 4.0 * s, 0.25 + 4.0 * (1 - s)) if 0 < s < 1 else 0.0
        return (w * max(f, 0.0), h * max(f, 0.0) * 0.9 + 1e-3)
    sweep(part, [tuple(p) for p in P], ml.lozenge(1.0, 1.0), scales=sc, up=up)


def disc(part, center, r, thick, axis="z", sides=14, dome=0.0):
    """A flat disc (a coin, a moon, a sun): a lathe with a short profile."""
    prof = [(-thick / 2, r * 0.96), (-thick / 2 + 0.12, r), (thick / 2 - 0.12, r), (thick / 2, r * 0.96)]
    if dome:
        prof = [(-thick / 2, r * 0.9), (0, r), (thick / 2 + dome, r * 0.55)]
    return ml.lathe(part, axis, center, prof, sides=sides, caps=(True, True))


def orb(part, center, r, sides=6):
    """A small sphere (a bead, a pearl, a bubble): a lathe through a three-band profile, six sides at most."""
    prof = [(-r, 0.0), (-r * 0.5, r * 0.87), (r * 0.5, r * 0.87), (r, 0.0)]
    return ml.lathe(part, "y", center, prof, sides=min(sides, 6), caps=(False, False))


def crescent(part, center, R, width, thick, a0=-60, a1=200, plane="xy", n=10):
    """A crescent blade: an arc of radius R whose width swells in the middle and tapers to two points (a moon, a sickle, a scythe edge)."""
    path = ring_path(center, R, a0, a1, plane, n)
    sweep_flat(part, path, width, thick, lambda s: math.sin(math.pi * s) ** 0.8, UP[plane])


# ---- cheaper raised details (the same shapes as mesh_lib's, with the quads nobody sees left out)
def bar(part, p0, p1, w, h, up=None, taper=0.0, section="lozenge", end_scale=0.0):
    """A thin raised bar from p0 to p1: ONE lozenge loft of three stations closing to points at both ends (8 quads, mesh_lib's bar is 24)."""
    p0, p1 = np.asarray(p0, float), np.asarray(p1, float)
    L = float(np.linalg.norm(p1 - p0))
    if L < 1e-6:
        return None
    return ml.loft(part, p1 - p0, p0, [(0, 0.0, 0.0), (L * 0.5, 1, 1), (L, 0.0, 0.0)], ml.lozenge(w, 2 * h) if section == "lozenge" else ml.rect_ring(w, 2 * h), up=up, caps=(False, False))


def stud(part, pos, r, h, normal=(0, 0, 1), sides=6, dome=0.55):
    """A domed stud: two stations, five sides at most (mesh_lib's is four stations of eight)."""
    n = unit(normal)
    return ml.loft(part, n, pos, [(-h * 0.15, r), (h, r * 0.45)], C(1.0, min(5, sides)), caps=(False, True))


def gem(part, center, r, long=1.4, sides=6, axis="y"):
    """A cut gem with at most five sides."""
    c = np.asarray(center, float)
    return ml.lathe(part, axis, c, [(-r * long, 0.0), (-r * 0.15, r), (r * 0.15, r), (r * long * 0.7, 0.0)], min(5, sides), caps=(False, False))


def box(part, p0, p1, chamfer=0.0):
    """ml.box, chamfered only when the block is big enough to show it."""
    big = min(abs(p1[0] - p0[0]), abs(p1[2] - p0[2])) > 3.0
    return ml.box(part, p0, p1, chamfer=chamfer if big else 0.0)


def spike(part, base, direction, length, r, sides=5):
    """A cone: four or five sides, open at the base (it stands on a surface)."""
    d = unit(direction)
    return ml.loft(part, d, base, [(0, r, r), (length, 0.0, 0.0)], C(1.0, max(4, min(5, sides))), caps=(False, False))


def mirror_x(p, name):
    return p.mirrored("x", name=name)


# ================================================================ materials: the Djinn's own set, a unique palette and a dark palette
def mix(a, b, t):
    return lerp3(a, b, t)


WHITE, BLACK = (255, 255, 255), (0, 0, 0)
GOLD_B, GOLD_H = (206, 160, 62), (255, 236, 150)


def _mat(kind, base, hi):
    return dict(ml.MATERIALS[kind], base=tuple(int(x) for x in base), hi=tuple(int(x) for x in hi))


# (plate kind, plate bias, trim kind, trim colour mode, cloth base, cloth hi, bone kind) per Djinn: the stuff each is MADE of
PALETTE = {
    "BAAL":       dict(plate=("obsidian", 0.80, (10, 12, 26)), trim=("gold", 0.30), cloth=((24, 30, 64), 0.35), extra=("iron", 0.35)),
    "AMON":       dict(plate=("bronze", 0.45, (70, 30, 12)), trim=("gold", 0.55), cloth=((228, 222, 206), 0.10), extra=("bronze", 0.5)),
    "VINEA":      dict(plate=("silver", 0.45, (170, 210, 230)), trim=("gold", 0.25), cloth=((70, 150, 190), 0.45), extra=("bone", 0.25)),
    "FOCALOR":    dict(plate=("silver", 0.50, (210, 240, 226)), trim=("gold", 0.40), cloth=((226, 244, 236), 0.30), extra=("bone", 0.30)),
    "ZAGAN":      dict(plate=("wood", 0.55, (50, 84, 40)), trim=("bronze", 0.40), cloth=((46, 110, 52), 0.45), extra=("wood", 0.60)),
    "ZEPAR":      dict(plate=("stone", 0.55, (92, 40, 38)), trim=("bronze", 0.30), cloth=((92, 30, 30), 0.35), extra=("stone", 0.50)),
    "FURFUR":     dict(plate=("obsidian", 0.70, (34, 20, 60)), trim=("silver", 0.45), cloth=((60, 36, 110), 0.45), extra=("silver", 0.45)),
    "MARCHOSIAS": dict(plate=("iron", 0.60, (60, 22, 18)), trim=("bronze", 0.50), cloth=((120, 54, 28), 0.40), extra=("bone", 0.30)),
    "SITRI":      dict(plate=("gold", 0.30, (255, 214, 120)), trim=("gold", 0.70), cloth=((250, 246, 232), 0.20), extra=("silver", 0.40)),
    "BIFRON":     dict(plate=("iron", 0.60, (34, 24, 74)), trim=("silver", 0.50), cloth=((44, 30, 96), 0.40), extra=("silver", 0.50)),
    "PAIMON":     dict(plate=("silver", 0.40, (214, 238, 246)), trim=("gold", 0.45), cloth=((240, 248, 252), 0.15), extra=("bone", 0.20)),
    "VASSAGO":    dict(plate=("bone", 0.55, (28, 40, 90)), trim=("silver", 0.35), cloth=((16, 28, 80), 0.45), extra=("iron", 0.50)),
}


def register_materials(did, name, color, glow, variant):
    """Registers <did>_<variant>_<plate|trim|cloth|crystal|dark|extra|gold|bone>: 'u' = the Djinn's own palette, 'd' = the Dark Djinn palette
    (black metal and cloth, gold trim, the Djinn's accent kept in the crystals)."""
    c, g = rgb(color), rgb(glow)
    pre = f"{did}_{variant}_"
    pal = PALETTE[name]
    if variant == "u":
        kind, bias, tgt = pal["plate"]
        ml.MATERIALS[pre + "plate"] = _mat(kind, mix(c, tgt, bias), mix(c, WHITE, 0.55))
        tk, tb = pal["trim"]
        tbase = {"gold": GOLD_B, "bronze": (176, 120, 66), "silver": (196, 202, 214)}[tk]
        ml.MATERIALS[pre + "trim"] = _mat(tk, mix(tbase, g, tb * 0.5), mix(g, WHITE, 0.8))
        cb, cm = pal["cloth"]
        ml.MATERIALS[pre + "cloth"] = _mat("cloth", mix(cb, c, cm), mix(g, WHITE, 0.4))
        ml.MATERIALS[pre + "crystal"] = _mat("crystal", mix(c, WHITE, 0.3), mix(g, WHITE, 0.82))
        ml.MATERIALS[pre + "dark"] = _mat("obsidian", mix(c, (12, 12, 22), 0.84), mix(c, WHITE, 0.45))
        ek, eb = pal["extra"]
        ml.MATERIALS[pre + "extra"] = _mat(ek, mix(mix(c, WHITE, 0.2), tgt, eb * 0.6), mix(g, WHITE, 0.6))
        ml.MATERIALS[pre + "bone"] = _mat("bone", mix((226, 214, 184), c, 0.18), (250, 244, 224))
        ml.MATERIALS[pre + "gold"] = _mat("gold", mix(GOLD_B, g, 0.2), GOLD_H)
    else:
        ml.MATERIALS[pre + "plate"] = _mat("obsidian", mix((20, 18, 26), c, 0.10), mix((120, 110, 140), g, 0.18))
        ml.MATERIALS[pre + "trim"] = _mat("gold", mix(GOLD_B, g, 0.10), GOLD_H)
        ml.MATERIALS[pre + "cloth"] = _mat("cloth", mix((22, 18, 30), c, 0.12), mix((90, 70, 120), g, 0.25))
        ml.MATERIALS[pre + "crystal"] = _mat("crystal", mix(c, (14, 10, 22), 0.5), mix(g, WHITE, 0.55))
        ml.MATERIALS[pre + "dark"] = _mat("obsidian", (10, 9, 14), mix((60, 52, 78), g, 0.2))
        ml.MATERIALS[pre + "extra"] = _mat("iron", mix((38, 34, 46), c, 0.12), mix((150, 140, 170), g, 0.2))
        ml.MATERIALS[pre + "bone"] = _mat("bone", (64, 58, 66), (150, 142, 150))
        ml.MATERIALS[pre + "gold"] = _mat("gold", GOLD_B, GOLD_H)


# ================================================================ the kit
class UKit(dp.Kit):
    """dp.Kit with the unique / dark material set and the variant flag the builders read."""

    def __init__(self, mesh, did, djinn, variant):
        super().__init__(mesh, did, djinn)
        self.variant = variant
        self.dark = variant == "d"
        self.name = djinn["name"]

    def part(self, piece, name, mat="plate", glow=False, layer=0, smooth=35.0, uv_mode=None, wear=0.5):
        p = ml.Part(f"{piece}__{name}", layer=layer, material=f"{self.did}_{self.variant}_{mat}", glow=glow, smooth=smooth, uv_mode=uv_mode, wear=wear)
        return self.m.add(p)

    def trim(self):
        """The trim material: the Djinn's own metal, or gold for the Dark Djinn (its trim IS gold)."""
        return "trim"

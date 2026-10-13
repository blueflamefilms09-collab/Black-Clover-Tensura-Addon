"""djinn_parts: the shared parts library of the Djinn armour (Half Equip and Full Equip), built with tools/relics/mesh_lib.py.

Every Djinn is the same set of pieces (helm, horns, ears, crest, chest, belt, pauldrons, vambraces, gauntlets, greaves, wings, tail, halo,
sheath) drawn from the same builders and varied by parameters: the Djinn's palette, its Look (horns / wings / tail / halo styles from
relics/djinn/Djinn.java) and a style row here (helm, pauldron, vambrace, greave shapes). Twelve silhouettes, one library.

Coordinates ("author space", pixels = 1/16 block): x to the viewer's right when facing the character, y UP, z towards the viewer (the front).
Each piece is authored in the frame of the player-model part it is attached to (the part's pivot is the origin):
    head (neck centre; the head is x +-4, y 0..8, z +-4)       body (neck centre; x +-4, y 0..-12, z +-2)
    arm_r / arm_l (the shoulder joint; the arm is x -3..1 / -1..3, y +2..-10)      leg_r / leg_l (the hip joint; x +-2, y 0..-12)
    wing_l / wing_r (the wing root, the wing extends along +x / -x)      tail (the tail root, extends along -z, the back)
    halo (its centre; flat in xz)      sheath (the item's grip, the blade along +y)
The game's model space is this one turned upside down: (x, y, z) -> (x, -y, -z), a rotation, so no face changes its winding.
"""
import math
import os
import sys

import numpy as np

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import mesh_lib as ml                                   # noqa: E402

# ---- per Djinn: the shapes the Look table does not decide
STYLE = {
    "BAAL":       dict(helm="open",    pauldron="fin",   vambrace="fin",   greave="fin"),
    "AMON":       dict(helm="visor",   pauldron="spike", vambrace="claw",  greave="spike"),
    "VINEA":      dict(helm="circlet", pauldron="tier",  vambrace="plain", greave="plain"),
    "FOCALOR":    dict(helm="open",    pauldron="fin",   vambrace="fin",   greave="fin"),
    "ZAGAN":      dict(helm="circlet", pauldron="dome",  vambrace="plain", greave="plain"),
    "ZEPAR":      dict(helm="visor",   pauldron="spike", vambrace="claw",  greave="spike"),
    "FURFUR":     dict(helm="circlet", pauldron="tier",  vambrace="fin",   greave="plain"),
    "MARCHOSIAS": dict(helm="open",    pauldron="spike", vambrace="claw",  greave="spike"),
    "SITRI":      dict(helm="circlet", pauldron="dome",  vambrace="plain", greave="plain"),
    "BIFRON":     dict(helm="visor",   pauldron="fin",   vambrace="fin",   greave="fin"),
    "PAIMON":     dict(helm="open",    pauldron="tier",  vambrace="fin",   greave="plain"),
    "VASSAGO":    dict(helm="visor",   pauldron="dome",  vambrace="claw",  greave="spike"),
}

PIECES = ["helm", "horn_l", "horn_r", "ears", "crest", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l",
          "gauntlet_r", "greave_l", "greave_r", "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r", "tail_seg", "tail_tip", "halo", "sheath"]

# where each piece sits in the preview mannequin (author space offsets of the attached part's pivot)
ANCHOR = {"helm": (0, 0, 0), "horn_l": (0, 0, 0), "horn_r": (0, 0, 0), "ears": (0, 0, 0), "crest": (0, 0, 0), "chest": (0, 0, 0), "belt": (0, 0, 0),
          "pauldron_l": (5, -2, 0), "vambrace_l": (5, -2, 0), "gauntlet_l": (5, -2, 0), "pauldron_r": (-5, -2, 0), "vambrace_r": (-5, -2, 0),
          "gauntlet_r": (-5, -2, 0), "greave_l": (1.9, -12, 0), "greave_r": (-1.9, -12, 0)}


def lerp3(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def rgb(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def register_materials(did, color, glow):
    """The Djinn's own materials, derived from mesh_lib's steel / gold / cloth / crystal / obsidian."""
    c, g = rgb(color), rgb(glow)
    ml.MATERIALS[did + "_plate"] = dict(ml.MATERIALS["steel"], base=lerp3(c, (40, 42, 54), 0.34), hi=lerp3(c, (255, 255, 255), 0.58))
    ml.MATERIALS[did + "_trim"] = dict(ml.MATERIALS["gold"], base=lerp3(g, (176, 138, 60), 0.55), hi=lerp3(g, (255, 255, 255), 0.75))
    ml.MATERIALS[did + "_cloth"] = dict(ml.MATERIALS["cloth"], base=lerp3(c, (20, 20, 30), 0.42), hi=lerp3(c, (255, 255, 255), 0.30))
    ml.MATERIALS[did + "_crystal"] = dict(ml.MATERIALS["crystal"], base=lerp3(c, (255, 255, 255), 0.28), hi=lerp3(g, (255, 255, 255), 0.8))
    ml.MATERIALS[did + "_dark"] = dict(ml.MATERIALS["obsidian"], base=lerp3(c, (14, 14, 22), 0.82), hi=lerp3(c, (255, 255, 255), 0.45))


class Kit:
    """What a builder needs: the mesh being filled, the Djinn's data, and a part factory."""

    def __init__(self, mesh, did, djinn):
        self.m, self.did, self.dj = mesh, did, djinn
        self.st = STYLE[djinn["name"]]
        self.look = djinn["look"]

    def part(self, piece, name, mat="plate", glow=False, layer=0, smooth=35.0, uv_mode=None, wear=0.5):
        p = ml.Part(f"{piece}__{name}", layer=layer, material=f"{self.did}_{mat}", glow=glow, smooth=smooth, uv_mode=uv_mode, wear=wear)
        return self.m.add(p)


C = ml.circle


def S(n):
    """Side count of a round section, thinned: the armour is drawn every frame for every Djinn bearer in sight, so every quad counts."""
    return max(5, int(round(n * 0.5)))



def collar(*a, **k):
    """mesh_lib's collar without the end caps (a band round a part: its ends are never seen)."""
    k.setdefault("caps", (False, False))
    return ml.collar(*a, **k)


def rune(part, p0, p1, normal, gap=0.5, w=0.2, h=0.1):
    """An engraved glowing line: one raised bar (mesh_lib's ridge_pair builds two; the armour is drawn every frame, so one is enough)."""
    ml.bar(part, p0, p1, w * 2.2, h * 2.2, up=normal, section="rect")


def curve(points, n=14):
    n = max(2, n // 2)
    """A Catmull-Rom curve through the points, n samples per span: a path for sweep()."""
    P = [np.asarray(p, float) for p in points]
    P = [P[0]] + P + [P[-1]]
    out = []
    for i in range(1, len(P) - 2):
        for k in range(n):
            t = k / n
            a, b, c, d = P[i - 1], P[i], P[i + 1], P[i + 2]
            out.append(0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t * t + (-a + 3 * b - 3 * c + d) * t ** 3))
    out.append(P[-2])
    return out


def horn_path(style):
    return {
        "CRESCENT": [(3.0, 6.4, 0.4), (4.4, 8.2, 0.3), (5.3, 10.6, 0.0), (5.0, 12.8, -0.6), (3.7, 14.6, -1.3)],
        "RAM": [(3.4, 5.6, 0.0), (5.4, 6.6, -1.0), (6.9, 5.2, -2.3), (6.6, 2.9, -1.8), (5.2, 1.9, 0.2)],
        "STAG": [(2.6, 6.8, -0.2), (3.6, 9.2, -0.2), (4.2, 12.0, -0.5), (4.0, 15.0, -0.9)],
        "SPIKE": [(3.4, 6.0, -0.6), (4.3, 7.0, -2.6), (5.0, 7.7, -5.4)],
    }[style]


def sweep_tube(part, path, r0, r1, sides=8, base_flare=0.0):
    pts = curve(path, 8)

    def sc(t):
        s = r0 + (r1 - r0) * t
        s *= 1 + base_flare * max(0.0, 1 - t * 5)
        return (s, s)
    ml.sweep(part, pts, C(1.0, sides), scales=sc)


def blade(part, base, direction, length, width, thick, bulge=0.5, tip=0.0):
    """A flat tapering blade (a feather, an energy blade, a fin) from `base` along `direction`; flat in the plane the section's width lies in."""
    d = np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    ml.loft(part, d, base, [(0, 0.55, 1.0), (length * 0.18, 1.0, 1.0), (length * (0.35 + 0.3 * bulge), 0.92, 1.0), (length * 0.85, 0.55, 0.9), (length, tip, tip)],
            ml.lozenge(width, thick), up=(0, 0, 1))


def leaf(part, pts):
    """A flat two-sided convex polygon (a membrane panel, a ribbon end): the points in order, seen from the front."""
    ids = [part.add_vertex(p) for p in pts]
    ids2 = [part.add_vertex(p) for p in pts]
    for i in range(1, len(ids) - 1):
        part.add_face(ids[0], ids[i], ids[i + 1], ids[i + 1])
        part.add_face(ids2[0], ids2[i + 1], ids2[i], ids2[i])


def spike(part, base, direction, length, r, sides=6):
    d = np.asarray(direction, float)
    ml.loft(part, d / np.linalg.norm(d), base, [(0, r, r), (length, 0.0, 0.0)], C(1.0, sides))


def mirror_into(kit, piece_from, piece_to):
    """Copies every part of piece_from into piece_to mirrored in x."""
    for p in list(kit.m.parts):
        if p.name.startswith(piece_from + "__"):
            q = p.mirrored("x", name=piece_to + "__" + p.name.split("__", 1)[1])
            kit.m.add(q)


# =============================================================== head
def build_helm(k):
    st, look = k.st["helm"], k.look
    cap = k.part("helm", "cap")
    if st == "circlet":
        ml.lathe(cap, "y", (0, 0, 0), [(5.0, 4.55), (5.5, 4.6), (6.0, 4.55)], sides=S(16), caps=(False, False))
    else:
        # a skullcap that starts above the brow, so the face (the eyes sit at y 3-4) stays open
        ml.lathe(cap, "y", (0, 0, 0), [(4.9, 4.7), (5.8, 4.75), (6.8, 4.45), (7.8, 3.6), (8.6, 2.2), (9.1, 0.0)], sides=S(16), caps=(False, True))
        brim = k.part("helm", "brim", "trim")
        collar(brim, 4.7, 4.95, 0.7, lip=0.2, sides=S(16))
    band = k.part("helm", "band", "trim")
    collar(band, 5.9 if st == "circlet" else 5.5, 4.7 if st == "circlet" else 4.95, 0.6, lip=0.18, sides=S(16))
    # neck guard and cheek plates
    back = k.part("helm", "neckguard")
    ml.box(back, (-3.7, 0.4, -4.95), (3.7, 4.2, -4.2), chamfer=0.5)
    for sgn, nm in ((-1, "cheek_r"), (1, "cheek_l")):
        ch = k.part("helm", nm)
        ml.box(ch, (sgn * 4.25 - 0.4, 1.6, -1.2), (sgn * 4.25 + 0.4, 4.6, 3.0), chamfer=0.3)
    if st in ("visor", "open"):
        nas = k.part("helm", "nasal", "trim")
        ml.bar(nas, (0, 5.4, 4.45), (0, 3.2 if st == "visor" else 4.0, 4.55), 0.9, 0.35, up=(0, 0, 1), taper=0.4)
    if st == "visor":
        v = k.part("helm", "visor")
        ml.box(v, (-3.9, 1.2, 4.1), (3.9, 3.0, 4.6), chamfer=0.4)                       # a mouth guard: the eyes stay free
        for i in (-2, 0, 2):
            ml.bar(k.part("helm", f"vent_{i}", "dark"), (i * 0.9, 1.5, 4.65), (i * 0.9, 2.7, 4.65), 0.35, 0.12, up=(0, 0, 1))
    # crest ridge over the cap
    crest = k.part("helm", "ridge", "trim")
    path = [(0, 4.6, -4.6), (0, 7.4, -3.4), (0, 9.5, -0.8), (0, 9.8, 1.8), (0, 8.4, 3.9)]
    ml.sweep(crest, curve(path, 8), ml.lozenge(0.8, 2.4), scales=lambda t: (1, 0.55 + 0.45 * math.sin(math.pi * t) ** 0.5))
    # a brow gem
    gem = k.part("helm", "brow_gem", "crystal", glow=True, smooth=0)
    ml.gem(gem, (0, 6.0, 4.55), 0.7, long=1.2, sides=6, axis="z")
    # hair (Djinn with ears wear it long): ribbons down the back of the head
    if look["ears"]:
        for i, x in enumerate((-2.4, 0.0, 2.4)):
            h = k.part("helm", f"hair_{i}", "cloth", uv_mode="loft")
            pth = [(x, 7.4, -3.6), (x * 1.2, 5.2, -5.4), (x * 1.5, 2.0, -6.0), (x * 1.8, -2.5, -5.4), (x * 2.0, -6.5, -4.4)]
            ml.sweep(h, curve(pth, 6), ml.rect_ring(1.9, 0.35), scales=lambda t: (1.0 - 0.7 * t, 1.0))


def build_horns(k):
    style = k.look["horns"]
    if style == "NONE":
        return
    if style == "CROWN":
        for i, a in enumerate(np.linspace(-95, 95, 7)):
            r = math.radians(a)
            p = k.part("horn_l" if a >= 0 else "horn_r", f"spike_{i}", "trim")
            base = np.array([math.sin(r) * 4.5, 8.3 - abs(a) * 0.012, math.cos(r) * 4.5 + 0.0])
            d = np.array([math.sin(r) * 0.45, 1.0, math.cos(r) * 0.45])
            L = 5.6 - abs(a) / 95 * 2.3
            spike(p, base * np.array([1, 1, 1]), d, L, 0.85, 6)
        return
    for sgn, piece in ((1, "horn_l"), (-1, "horn_r")):
        path = horn_path(style)
        path = [(sgn * x, y, z) for (x, y, z) in path]
        h = k.part(piece, "horn", "trim", smooth=40)
        sweep_tube(h, path, 1.25, 0.12, 8, base_flare=0.5)
        ring = k.part(piece, "ring", "plate")
        collar(ring, 0.0, 1.55, 0.7, lip=0.2, sides=S(10), axis=tuple(np.array(path[1]) - np.array(path[0])), origin=path[0])
        if style == "STAG":
            for j, (t0, side) in enumerate(((0.35, 1), (0.7, -1))):
                i0 = int(len(path) * 0.0)
                a = np.array(path[1] if j == 0 else path[2])
                tine = [tuple(a), tuple(a + np.array([sgn * 2.0 * side, 1.7, 0.4])), tuple(a + np.array([sgn * 2.7 * side, 4.2, 0.5]))]
                t = k.part(piece, f"tine_{j}", "trim", smooth=40)
                sweep_tube(t, tine, 0.7, 0.08, 6)
        if style == "RAM":
            for j in range(3):
                t = (0.3 + 0.2 * j)
                c = curve(path, 8)[int(len(curve(path, 8)) * t)]
                rb = k.part(piece, f"ridge_{j}", "plate")
                collar(rb, 0, 1.15 - 0.2 * j, 0.45, lip=0.1, sides=S(8), axis=(0, 1, 0), origin=tuple(c))


def build_ears(k):
    if not k.look["ears"]:
        return
    for sgn in (-1, 1):
        e = k.part("ears", "ear_l" if sgn > 0 else "ear_r", "cloth", smooth=45)
        path = [(sgn * 4.3, 3.6, 0.2), (sgn * 6.6, 4.9, -0.2), (sgn * 8.6, 6.8, -0.6)]
        ml.sweep(e, curve(path, 8), ml.lozenge(1.7, 0.45), scales=lambda t: (1.0 - 0.9 * t, 1.0))
        rim = k.part("ears", "rim_l" if sgn > 0 else "rim_r", "trim")
        ml.sweep(rim, curve(path, 8), ml.lozenge(0.35, 0.7), scales=lambda t: (1.0 - 0.8 * t, 1.0))


def build_crest(k):
    """Half Equip's small crest: a brow jewel on a thin circlet with two fins."""
    g = k.part("crest", "jewel", "crystal", glow=True, smooth=0)
    ml.gem(g, (0, 5.9, 4.6), 0.85, long=1.3, sides=6, axis="z")
    c = k.part("crest", "circlet", "trim")
    collar(c, 5.2, 4.6, 0.55, lip=0.15, sides=S(16))
    for sgn in (-1, 1):
        f = k.part("crest", "fin_" + ("l" if sgn > 0 else "r"), "plate", smooth=40)
        ml.sweep(f, curve([(sgn * 1.0, 5.6, 4.5), (sgn * 2.8, 6.4, 4.3), (sgn * 4.2, 8.1, 3.4)], 6), ml.lozenge(0.9, 0.4), scales=lambda t: (1.0 - 0.85 * t, 1.0))


# =============================================================== body
def build_chest(k):
    ch = k.part("chest", "plate")
    ml.loft(ch, "-y", (0, 0.5, 0), [(0, 1.0, 1.0), (1.8, 1.04, 1.05), (4.5, 1.02, 1.08), (7.5, 0.9, 0.98), (10.0, 0.84, 0.92)], ml.ellipse(4.55, 2.75, S(16)), caps=(False, False))
    gor = k.part("chest", "gorget", "trim")
    collar(gor, -0.2, 3.2, 1.9, lip=0.35, sides=S(14), oval=0.85)
    for i, y in enumerate((-4.2, -6.6, -8.8)):                                                   # ab plates
        ab = k.part("chest", f"band_{i}", "trim")
        collar(ab, y, 4.2 - i * 0.18, 0.55, lip=0.15, sides=S(16), oval=0.66)
    core = k.part("chest", "core", "crystal", glow=True, smooth=0)
    ml.gem(core, (0, -2.6, 3.15), 1.05, long=1.3, sides=6, axis="z")
    cradle = k.part("chest", "cradle", "trim")
    collar(cradle, 3.0, 1.55, 0.5, lip=0.15, sides=S(12), axis="z", origin=(0, -2.6, 0), oval=1.0)
    for sgn, nm in ((-1, "r"), (1, "l")):                                                         # raised pectoral rune lines
        rl = k.part("chest", "rune_" + nm, "crystal", glow=True)
        rune(rl, (sgn * 0.6, -0.9, 3.0), (sgn * 3.6, -2.4, 2.8), (0, 0, 1), 0.5, w=0.2, h=0.1)
        rl2 = k.part("chest", "rune2_" + nm, "crystal", glow=True)
        rune(rl2, (sgn * 0.5, -3.6, 3.1), (sgn * 3.3, -5.6, 2.9), (0, 0, 1), 0.5, w=0.2, h=0.1)
    spine = k.part("chest", "spine", "trim")
    ml.bar(spine, (0, -0.4, -2.95), (0, -9.2, -2.75), 1.0, 0.35, up=(0, 0, -1), taper=1.2)
    socket = k.part("chest", "socket", "trim")
    ml.lathe(socket, "z", (0, -2.6, -2.9), [(-0.2, 1.8), (-0.5, 1.8), (-0.9, 1.0)], sides=S(12))
    sg = k.part("chest", "socket_gem", "crystal", glow=True, smooth=0)
    ml.gem(sg, (0, -2.6, -3.5), 0.8, long=1.0, sides=6, axis="-z")
    for sgn, nm in ((-1, "r"), (1, "l")):                                                          # shoulder-blade plates
        sb = k.part("chest", "blade_" + nm, "plate")
        ml.loft(sb, "-y", (sgn * 2.3, -0.8, -2.75), [(0, 1.0, 1.0), (3.2, 1.2, 1.0), (6.0, 0.4, 0.6)], ml.lozenge(2.6, 0.7))


def build_belt(k):
    b = k.part("belt", "band", "trim")
    ml.loft(b, "-y", (0, -9.4, 0), [(0, 1.0, 1.0), (1.9, 1.0, 1.0)], ml.ellipse(4.5, 2.75, S(16)), caps=(False, False))
    bk = k.part("belt", "buckle", "crystal", glow=True, smooth=0)
    ml.gem(bk, (0, -10.4, 3.0), 1.1, long=1.0, sides=6, axis="z")
    br = k.part("belt", "buckle_ring", "plate")
    collar(br, 2.8, 1.7, 0.4, lip=0.12, sides=S(12), axis="z", origin=(0, -10.4, 0))
    for i, x in enumerate((-2.9, 0.0, 2.9)):
        t = k.part("belt", f"tasset_{i}", "plate")
        L = 5.4 if i == 1 else 4.5
        ml.loft(t, "-y", (x, -11.2, 2.95), [(0, 1.0, 1.0), (L * 0.6, 0.92, 1.0), (L, 0.15, 0.8)], ml.lozenge(2.6, 0.55), up=(0, 0, 1))
        ml.bar(k.part("belt", f"tasset_rune_{i}", "crystal", glow=True), (x, -11.6, 3.28), (x, -11.2 - L * 0.7, 3.2), 0.35, 0.1, up=(0, 0, 1), taper=0.6)
    for sgn, nm in ((-1, "r"), (1, "l")):
        s = k.part("belt", "hip_" + nm, "plate")
        ml.loft(s, "-y", (sgn * 4.7, -10.6, 0.0), [(0, 1.0, 1.0), (4.0, 0.8, 1.0), (5.2, 0.1, 0.8)], ml.lozenge(0.6, 3.6), up=(1, 0, 0))
    r = k.part("belt", "rear", "cloth")
    ml.loft(r, "-y", (0, -10.8, -2.95), [(0, 1.0, 1.0), (5.0, 1.1, 1.0), (7.0, 0.5, 0.8)], ml.rect_ring(4.4, 0.4))


def build_pauldron(k):
    st = k.st["pauldron"]
    for sgn, piece in ((-1, "pauldron_r"), (1, "pauldron_l")):
        cx = sgn * 1.0                                                                     # the arm part's centre line
        dome = k.part(piece, "dome")
        ml.lathe(dome, "y", (cx + sgn * 0.5, 0, 0), [(-1.0, 3.9), (0.2, 4.45), (1.5, 4.2), (2.7, 3.3), (3.6, 1.8), (4.1, 0)], sides=S(14), caps=(False, True))
        for i, (y, r) in enumerate(((-1.2, 4.3), (-2.3, 4.0), (-3.3, 3.7))):
            if st == "tier" or i == 0:
                rim = k.part(piece, f"rim_{i}", "trim")
                collar(rim, y, r, 0.9, lip=0.25, sides=S(14), origin=(cx + sgn * 0.5, 0, 0))
        cap = k.part(piece, "stud", "crystal", glow=True, smooth=0)
        ml.gem(cap, (cx + sgn * 2.4, 2.9, 0), 0.7, long=1.0, sides=6, axis=(sgn, 1, 0))
        if st == "spike":
            for j, (dx, dy, L) in enumerate(((3.5, 1.6, 4.6), (3.0, 3.2, 3.0), (3.6, -0.2, 2.8))):
                sp = k.part(piece, f"spike_{j}", "trim")
                spike(sp, (cx + sgn * 2.6, 1.2 + (dy - 1.6) * 0.5, 0), (sgn * dx, dy + 0.6, -0.3 * j), L, 0.9 - 0.15 * j, 6)
        elif st == "fin":
            fn = k.part(piece, "fin", "plate", smooth=45)
            ml.sweep(fn, curve([(cx + sgn * 2.8, 2.6, -0.4), (cx + sgn * 4.6, 5.0, -1.4), (cx + sgn * 5.2, 8.2, -3.0)], 8), ml.lozenge(2.4, 0.5), scales=lambda t: (1.0 - 0.85 * t, 1.0))
            fr = k.part(piece, "fin_edge", "crystal", glow=True)
            ml.sweep(fr, curve([(cx + sgn * 3.0, 2.8, 0.1), (cx + sgn * 4.8, 5.2, -0.9), (cx + sgn * 5.4, 8.4, -2.6)], 8), ml.lozenge(0.3, 0.3), scales=lambda t: (1.0 - 0.7 * t, 1.0))
        elif st == "dome":
            for j in range(3):
                ridge = k.part(piece, f"ridge_{j}", "trim")
                ml.bar(ridge, (cx + sgn * (0.5 + 1.1 * j), 3.6 - 0.3 * j, 1.0 - 0.6 * j), (cx + sgn * (3.0 + 0.4 * j), 0.0, 0.2), 0.45, 0.2, up=(0, 1, 0), taper=0.5)


def build_vambrace(k):
    st = k.st["vambrace"]
    for sgn, piece in ((-1, "vambrace_r"), (1, "vambrace_l")):
        cx = sgn * 1.0
        v = k.part(piece, "tube")
        ml.loft(v, "-y", (cx, -3.0, 0), [(0, 1.0, 1.0), (2.8, 0.95, 0.95), (6.2, 0.82, 0.82)], C(2.75, S(12)), caps=(False, False))
        for i, y in enumerate((-3.0, -8.4)):
            c = k.part(piece, f"cuff_{i}", "trim")
            collar(c, y, 2.95 - i * 0.3, 0.7, lip=0.2, sides=S(12), origin=(cx, 0, 0))
        r = k.part(piece, "rune", "crystal", glow=True)
        rune(r, (cx, -3.8, 2.7), (cx, -8.0, 2.35), (0, 0, 1), 0.5, w=0.2, h=0.1)
        if st == "fin":
            f = k.part(piece, "fin", "plate", smooth=45)
            ml.sweep(f, curve([(cx + sgn * 2.7, -3.4, 0), (cx + sgn * 4.2, -6.0, -0.3), (cx + sgn * 4.6, -9.6, -0.9)], 8), ml.lozenge(1.8, 0.45), scales=lambda t: (1.0 - 0.8 * t, 1.0))
        elif st == "claw":
            for j in range(3):
                sp = k.part(piece, f"claw_{j}", "trim")
                spike(sp, (cx + sgn * 2.5, -4.0 - 2.0 * j, 0.3 * (j - 1)), (sgn * 1.0, -0.5 - 0.35 * j, 0.0), 3.4 - 0.6 * j, 0.55, 5)
        else:
            e = k.part(piece, "ridge", "trim")
            ml.bar(e, (cx, -3.4, -2.6), (cx, -8.4, -2.3), 0.9, 0.3, up=(0, 0, -1), taper=1.0)


def build_gauntlet(k):
    for sgn, piece in ((-1, "gauntlet_r"), (1, "gauntlet_l")):
        cx = sgn * 1.0
        g = k.part(piece, "palm")
        ml.loft(g, "-y", (cx, -9.0, 0), [(0, 1.0, 1.0), (1.6, 1.02, 1.02), (3.2, 0.92, 0.88)], ml.octagon(4.4, 4.4, 0.9), caps=(False, True))
        kn = k.part(piece, "knuckle", "trim")
        ml.box(kn, (cx - 2.1, -11.7, 2.0), (cx + 2.1, -10.7, 2.7), chamfer=0.25)
        for j, x in enumerate((-1.5, -0.5, 0.5, 1.5)):
            s = k.part(piece, f"stud_{j}", "trim")
            ml.stud(s, (cx + x, -11.2, 2.7), 0.4, 0.45, (0, 0, 1), 6)
        for j, x in enumerate((-1.2, 0.0, 1.2)):
            c = k.part(piece, f"claw_{j}", "crystal", glow=True)
            spike(c, (cx + x, -11.9, 1.0 + 0.0), (0, -1.0, 0.5), 2.6, 0.45, 5)
        bk = k.part(piece, "back", "plate")
        ml.box(bk, (cx - 1.9, -10.3, -2.5), (cx + 1.9, -9.2, -1.9), chamfer=0.3)


def build_greave(k):
    st = k.st["greave"]
    for sgn, piece in ((-1, "greave_r"), (1, "greave_l")):
        th = k.part(piece, "cuisse")
        ml.loft(th, "-y", (0, -0.6, 0), [(0, 1.0, 1.0), (2.6, 1.0, 1.0), (5.2, 0.93, 0.93)], C(2.65, S(12)), caps=(False, False))
        kn = k.part(piece, "knee", "trim")
        ml.lathe(kn, "z", (0, -6.3, 2.0), [(0.0, 1.5), (0.35, 1.7), (0.9, 1.25), (1.3, 0.0)], sides=S(10))
        sh = k.part(piece, "shin")
        ml.loft(sh, "-y", (0, -6.6, 0), [(0, 1.0, 1.0), (2.2, 0.95, 0.92), (4.6, 0.86, 0.84)], C(2.5, S(12)), caps=(False, False))
        bt = k.part(piece, "boot", "dark")
        ml.loft(bt, "-y", (0, -10.5, 0), [(0, 1.0, 1.0), (1.6, 1.08, 1.12)], ml.octagon(4.5, 4.7, 0.9), caps=(False, True))
        toe = k.part(piece, "toe", "trim")
        ml.box(toe, (-2.0, -12.0, 2.2), (2.0, -10.8, 3.3), chamfer=0.4)
        for i, y in enumerate((-6.2, -10.4)):
            c = k.part(piece, f"band_{i}", "trim")
            collar(c, y, 2.75 - 0.15 * i, 0.5, lip=0.14, sides=S(12))
        r = k.part(piece, "rune", "crystal", glow=True)
        rune(r, (0, -7.2, 2.4), (0, -10.2, 2.25), (0, 0, 1), 0.45, w=0.18, h=0.09)
        if st == "fin":
            f = k.part(piece, "fin", "plate", smooth=45)
            ml.sweep(f, curve([(sgn * 2.5, -6.8, 0), (sgn * 4.2, -8.4, -0.8), (sgn * 4.4, -11.4, -2.2)], 8), ml.lozenge(2.0, 0.45), scales=lambda t: (1.0 - 0.8 * t, 1.0))
        elif st == "spike":
            sp = k.part(piece, "knee_spike", "trim")
            spike(sp, (0, -6.3, 3.3), (0, 0.2, 1.0), 3.6, 0.8, 6)
            for j in (-1, 1):
                s2 = k.part(piece, f"shin_spike_{j}", "trim")
                spike(s2, (sgn * 2.4, -8.2 - j, 0), (sgn * 1.0, -0.3, -0.4), 2.6, 0.5, 5)


# =============================================================== back pieces
WING_PLAN = {
    # style: (inner blades, outer blades) each (angle in degrees in the xy plane from +x, start s along the segment 0..1, length, width, thick)
    "FEATHER": ([(-12, 0.1, 9, 2.2, 0.45), (-28, 0.35, 9.5, 2.3, 0.45), (-44, 0.6, 9, 2.2, 0.45), (-58, 0.85, 8.5, 2.1, 0.45)],
                [(8, 0.0, 14, 2.2, 0.5), (-8, 0.1, 15.5, 2.4, 0.5), (-24, 0.25, 16, 2.4, 0.5), (-40, 0.4, 15, 2.3, 0.5), (-56, 0.55, 13.5, 2.2, 0.5), (-72, 0.7, 11, 2.0, 0.5)]),
    "BLADE": ([(-20, 0.2, 10, 1.3, 0.6), (-42, 0.6, 9.5, 1.3, 0.6)],
              [(14, 0.0, 17, 1.5, 0.7), (-4, 0.15, 19, 1.6, 0.7), (-24, 0.3, 18, 1.6, 0.7), (-46, 0.5, 15, 1.5, 0.7), (-68, 0.7, 11, 1.4, 0.7)]),
    "FIRE": ([(-10, 0.1, 9, 3.2, 0.4), (-30, 0.45, 9.5, 3.4, 0.4), (-52, 0.8, 8.5, 3.0, 0.4)],
             [(10, 0.0, 15, 3.4, 0.45), (-10, 0.15, 17, 3.8, 0.45), (-32, 0.3, 16.5, 3.8, 0.45), (-54, 0.5, 14, 3.4, 0.45), (-74, 0.7, 11, 3.0, 0.45)]),
}


def build_wings(k):
    style = k.look["wings"]
    if style == "NONE":
        return
    for sgn, side in ((1, "l"), (-1, "r")):
        inn, out = "wing_in_" + side, "wing_out_" + side
        bone = k.part(inn, "bone", "plate")
        ml.loft(bone, "x", (0, 0, 0), [(0, 1.35, 1.35), (5.5, 1.0, 1.0), (11.0, 0.8, 0.8)], C(1.0, S(8)), caps=(True, True))
        sh = k.part(inn, "shoulder", "trim")
        ml.lathe(sh, "x", (0, 0, 0), [(-1.2, 0.0), (-0.6, 1.9), (0.8, 1.9), (1.4, 0.0)], sides=S(10))
        eb = k.part(out, "bone", "plate")
        ml.loft(eb, "x", (0, 0, 0), [(0, 0.8, 0.8), (7, 0.62, 0.62), (14, 0.2, 0.2)], C(1.0, S(8)))
        elb = k.part(out, "joint", "trim")
        ml.lathe(elb, "x", (0, 0, 0), [(-0.9, 0.0), (-0.3, 1.2), (0.3, 1.2), (0.9, 0.0)], sides=S(8))
        if style in WING_PLAN:
            mat = "crystal" if style == "BLADE" else "plate"
            layer = 0
            for seg, blades, L in ((inn, WING_PLAN[style][0], 11.0), (out, WING_PLAN[style][1], 14.0)):
                for j, (ang, s, ln, w, t) in enumerate(blades):
                    a = math.radians(ang)
                    bpart = k.part(seg, f"blade_{j}", mat if style != "FIRE" else "crystal", glow=(style in ("BLADE", "FIRE")), layer=layer, smooth=45)
                    base = (sgn * s * L, -0.3 * j * 0, -0.15 * (j % 2))
                    d = (sgn * math.cos(a), math.sin(a), 0.0)
                    blade(bpart, base, d, ln, w, t, bulge=0.5, tip=0.0 if style != "FIRE" else 0.12)
                    if style == "FEATHER":
                        sp = k.part(seg, f"quill_{j}", "trim")
                        ml.bar(sp, base, tuple(np.array(base) + np.array(d) * ln * 0.78), 0.35, 0.28, up=(0, 0, 1), taper=ln * 0.2)
        elif style == "MEMBRANE":
            tips = [(sgn * 6.0, 15.5, 0.0), (sgn * 14.0, 10.5, 0.0), (sgn * 21.0, 3.5, 0.0), (sgn * 24.0, -6.0, 0.0)]
            tips2 = [(sgn * 11.0, 0, 0), (sgn * 9.0, 2.0, 0), (sgn * 11.0, 4.0, 0), (sgn * 13.0, 1.0, 0)]
            for j, t in enumerate(tips[:3]):
                st_ = k.part(out, f"strut_{j}", "plate", smooth=45)
                ml.loft(st_, np.array(t) - np.array((sgn * 11.0, 0, 0)) , (sgn * 11.0, 0, 0), [(0, 0.55, 0.55), (float(np.linalg.norm(np.array(t) - np.array((sgn * 11.0, 0, 0)))), 0.15, 0.15)], C(1.0, S(6)))
            pts = [(sgn * 0.0, 0.0, 0.0)] + [(sgn * 11.0, 0.0, 0.0)]
            panels = [[(0, 0, -0.05), (sgn * 11.0, 0, -0.05), tips[0], (sgn * 2.0, 8.0, -0.05)],
                      [(sgn * 11.0, 0, -0.05), tips[0], tips[1]],
                      [(sgn * 11.0, 0, -0.05), tips[1], tips[2]],
                      [(sgn * 11.0, 0, -0.05), tips[2], tips[3], (sgn * 14.0, -7.0, -0.05), (sgn * 4.0, -5.0, -0.05)]]
            for j, poly in enumerate(panels):
                pl = k.part(out if j > 0 else inn, f"panel_{j}", "cloth", uv_mode="dominant")
                P = [tuple(np.array(p, float) + (0, 0, 0)) for p in poly]
                if sgn < 0:
                    P = P[::-1]
                leaf(pl, P)
            vein = k.part(out, "vein", "crystal", glow=True)
            for t in tips[:3]:
                ml.bar(vein, (sgn * 11.0, 0, 0.35), (t[0], t[1], 0.35), 0.3, 0.1, up=(0, 0, 1), taper=1.0)
        elif style == "VEIL":
            for j, (ang, ln, w) in enumerate(((-18, 24, 5.0), (-42, 26, 4.6), (-66, 24, 4.2))):
                a = math.radians(ang)
                rb = k.part(inn if j == 0 else out, f"ribbon_{j}", "cloth", uv_mode="loft")
                path = [(sgn * 2.0, 0, 0), (sgn * (2 + ln * 0.45) * math.cos(a), (2 + ln * 0.45) * math.sin(a) * 0.6, 0.6), (sgn * ln * math.cos(a) * 0.92, ln * math.sin(a), 0.2),
                        (sgn * (ln + 2) * math.cos(a) * 0.95, (ln + 2) * math.sin(a) * 1.02, -0.4)]
                ml.sweep(rb, curve(path, 8), ml.rect_ring(1.0, 0.18), scales=lambda t, w=w: (w * (0.35 + 0.65 * math.sin(min(1, t * 1.15) * math.pi * 0.85)), 1.0))
                tr = k.part(inn if j == 0 else out, f"trim_{j}", "crystal", glow=True)
                ml.sweep(tr, curve(path, 8), ml.rect_ring(0.35, 0.3), scales=lambda t: (1.0, 1.0))


def build_tail(k):
    style = k.look["tail"]
    if style == "NONE":
        return
    seg = k.part("tail_seg", "seg", "plate" if style in ("WHIP", "BLADE") else "cloth", glow=(style == "FLAME"), layer=(2 if style == "FLAME" else 0))
    r = {"WHIP": 1.15, "FLAME": 1.5, "BLADE": 1.2, "TENDRIL": 0.85}[style]
    ml.loft(seg, "-z", (0, 0, 0), [(0, r, r), (3.0, r * 0.92, r * 0.92), (6.0, r * 0.8, r * 0.8)], C(1.0, S(8)), caps=(False, False))
    ring = k.part("tail_seg", "ring", "trim")
    collar(ring, -0.25, r * 1.1, 0.7, lip=0.2, sides=S(8), axis="-z")
    if style == "BLADE":
        fin = k.part("tail_seg", "fin", "plate", smooth=45)
        ml.loft(fin, "-z", (0, r * 0.8, 0), [(0, 0.2, 0.2), (3.0, 1.0, 1.0), (6.0, 0.2, 0.2)], ml.lozenge(0.4, 2.0), up=(1, 0, 0))
    if style == "TENDRIL":
        bead = k.part("tail_seg", "bead", "crystal", glow=True, smooth=0)
        ml.gem(bead, (0, 0, -3.0), 0.9, long=1.0, sides=6, axis="z")
    tip = k.part("tail_tip", "tip", "crystal" if style != "WHIP" else "trim", glow=(style != "WHIP"), layer=(2 if style == "FLAME" else 0), smooth=40)
    if style == "WHIP":
        ml.loft(tip, "-z", (0, 0, 0), [(0, 0.9, 0.9), (2.0, 1.3, 1.3), (4.0, 0.7, 0.7), (8.0, 0.0, 0.0)], C(1.0, S(8)))
    elif style == "FLAME":
        ml.loft(tip, "-z", (0, 0, 0), [(0, 1.5, 1.5), (3, 2.0, 2.0), (7, 1.2, 1.2), (12, 0.0, 0.0)], C(1.0, S(8)))
    elif style == "BLADE":
        ml.loft(tip, "-z", (0, 0, 0), [(0, 0.6, 0.6), (2, 2.6, 0.9), (6, 2.4, 0.8), (11, 0.0, 0.0)], ml.lozenge(1.0, 0.5), up=(1, 0, 0))
    else:
        ml.loft(tip, "-z", (0, 0, 0), [(0, 0.7, 0.7), (3, 0.8, 0.8), (6, 0.5, 0.5), (8, 0.0, 0.0)], C(1.0, S(6)))


def build_halo(k):
    style = k.look["halo"]
    if style == "NONE":
        return
    R = 6.2
    ring = k.part("halo", "ring", "trim", glow=True)
    path = [(R * math.cos(a), 0, R * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)]
    if style == "RUNES":
        for i in range(6):
            a0, a1 = i * math.pi / 3 + 0.12, (i + 1) * math.pi / 3 - 0.12
            arc = k.part("halo", f"arc_{i}", "trim", glow=True)
            ml.sweep(arc, [(R * math.cos(a), 0, R * math.sin(a)) for a in np.linspace(a0, a1, 7)], C(0.55, S(6)))
            g = k.part("halo", f"rune_{i}", "crystal", glow=True, smooth=0)
            am = (a0 + a1) / 2
            ml.gem(g, (R * math.cos(am), 0, R * math.sin(am)), 0.8, long=1.2, sides=6, axis=(-math.sin(am), 0, math.cos(am)))
    else:
        ml.sweep(ring, path, C(0.55, S(6)))
    if style == "RING":
        inner = k.part("halo", "inner", "crystal", glow=True)
        ml.sweep(inner, [(R * 0.72 * math.cos(a), 0, R * 0.72 * math.sin(a)) for a in np.linspace(0, 2 * math.pi, 17)], C(0.3, S(5)))
        for i in range(4):
            a = i * math.pi / 2
            gm = k.part("halo", f"gem_{i}", "crystal", glow=True, smooth=0)
            ml.gem(gm, (R * math.cos(a), 0, R * math.sin(a)), 0.95, long=1.1, sides=6, axis=(math.cos(a), 0, math.sin(a)))
    if style == "SPOKES":
        for i in range(10):
            a = i * 2 * math.pi / 10
            sp = k.part("halo", f"spoke_{i}", "crystal", glow=True)
            spike(sp, (R * math.cos(a), 0, R * math.sin(a)), (math.cos(a), 0, math.sin(a)), 3.4 if i % 2 == 0 else 2.2, 0.45, 5)


def build_sheath(k):
    """The elemental sheath of the held weapon: glow-only blades along +y (grip at the origin)."""
    aff = k.dj["affinity"]
    core = k.part("sheath", "core", "crystal", layer=2, glow=True, smooth=40)
    ml.loft(core, "y", (0, 4.0, 0), [(0, 0.2, 0.2), (3, 1.0, 1.0), (13, 0.9, 0.9), (20, 0.35, 0.35), (23, 0.0, 0.0)], ml.lozenge(2.6, 1.2), up=(0, 0, 1))
    n = {"HEAT": 4, "LIGHTNING": 3, "WATER": 3, "WIND": 4, "LIFE": 3, "STRENGTH": 3, "SOUND": 4, "LIGHT": 5}[aff]
    for i in range(n):
        a = (i - (n - 1) / 2) * 0.5
        f = k.part("sheath", f"lick_{i}", "crystal", layer=2, glow=True, smooth=40)
        base_y = 6.0 + 2.2 * i
        L = 10 - 1.4 * abs(i - (n - 1) / 2) * 1.5
        d = (math.sin(a) * 0.6, 1.0, 0.0)
        ml.loft(f, np.array(d) / np.linalg.norm(d), (math.sin(a) * 1.4, base_y, 0.2 * (i % 2)), [(0, 0.3, 0.3), (L * 0.3, 1.0, 1.0), (L, 0.0, 0.0)], ml.lozenge(1.8, 0.8), up=(0, 0, 1))

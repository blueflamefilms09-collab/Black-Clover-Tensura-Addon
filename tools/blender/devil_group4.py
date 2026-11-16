"""Devil Contract bodies, group 4: the daemons of five Primordial lines (Jaune, Violet, Rouge, Vert, Bleu), MADE IN BLENDER (docs/devil_forms/group4.md).

Five player-scale upright devils (about 1.9 blocks to the top of the head, horns and crests reach 2.1 to 2.2): daemon_jaune (flame), daemon_violet (rot), daemon_rouge (soul),
daemon_vert (earth) and daemon_bleu (water). Each has its own silhouette, horns, armour, wings / tail and palette; they share only the helpers below and the part rig.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    python -B tools/blender/devil_group4.py [--only daemon_jaune] [--trial DIR] [--quick] [--check]
--check builds the meshes and prints the face counts without baking. Writes entity_meshes/devil_<id>.txt (+ _lod), textures/entity/boss_mesh/devil_<id>(.png, _glow.png)
and docs/devil_forms/previews/<id>.png.
"""
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402,I001
from mathutils import Vector  # noqa: E402

import bl_lib as bl  # noqa: E402
import bl_entity as be  # noqa: E402

PREVIEW_DIR = os.path.join(bl.ROOT, "docs", "devil_forms", "previews")
V = Vector


# ================================================================ rig (1/16 block units, y up, face +z, the devil's left = +x)
def make_rig(hip=15.0, leg_x=2.6, sh_x=5.6, sh_y=23.5, el_y=16.5, neck=25.0, wings=None, tail=None):
    rig = {
        "torso": (None, (0.0, hip, 0.0)),
        "head": ("torso", (0.0, neck, 0.0)),
        "arm_l": ("torso", (sh_x, sh_y, 0.0)),
        "arm_r": ("torso", (-sh_x, sh_y, 0.0)),
        "forearm_l": ("arm_l", (sh_x + 0.8, el_y, 0.0)),
        "forearm_r": ("arm_r", (-sh_x - 0.8, el_y, 0.0)),
        "leg_l": (None, (leg_x, hip, 0.0)),
        "leg_r": (None, (-leg_x, hip, 0.0)),
    }
    if wings:
        rig["wing_l"] = ("torso", wings)
        rig["wing_r"] = ("torso", (-wings[0], wings[1], wings[2]))
    if tail:
        rig["tail"] = ("torso", tail)
    return rig


# ================================================================ materials
def hexrgb(h):
    return bl.rgb((h >> 16) & 255, (h >> 8) & 255, h & 255)


def skin(name, base, rough=0.6, bump="leather", bscale=0.6, bstr=0.25, groove=None, gamount=0.5, bevel=0.12, metallic=0.0):
    return bl.mat(name, hexrgb(base), metallic=metallic, rough=rough, bump=bump, bscale=bscale, bstr=bstr, bevel=bevel,
                  groove=None if groove is None else hexrgb(groove), gamount=gamount)


def glow(name, color, strength=1.6):
    return bl.mat(name, hexrgb(color), rough=0.3, emit=hexrgb(color), strength=strength, bevel=0.0)


def _base(m):
    bs = m.node_tree.nodes["Principled BSDF"]
    lk = bs.inputs["Base Color"].links
    return (lk[0].from_socket if lk else None), bs


def _map(nt, val, lo, hi):
    mr = nt.nodes.new("ShaderNodeMapRange")
    mr.inputs["From Min"].default_value = lo
    mr.inputs["From Max"].default_value = hi
    nt.links.new(val, mr.inputs["Value"])
    return mr.outputs["Result"]


def blots(m, color, scale=0.3, lo=0.55, hi=0.7, detail=4.0):
    """Blots of `color` over the base colour where a noise is high (rot patches, moss, rust); returns the mask."""
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    src, bs = _base(m)
    nz = N.new("ShaderNodeTexNoise")
    nz.inputs["Scale"].default_value = scale
    nz.inputs["Detail"].default_value = detail
    L.new(N.new("ShaderNodeTexCoord").outputs["Object"], nz.inputs["Vector"])
    mask = _map(nt, nz.outputs[0], lo, hi)
    mix = N.new("ShaderNodeMixRGB")
    if src is not None:
        L.new(src, mix.inputs[1])
    else:
        mix.inputs[1].default_value = bs.inputs["Base Color"].default_value
    mix.inputs[2].default_value = hexrgb(color)
    L.new(mask, mix.inputs[0])
    L.new(mix.outputs[0], bs.inputs["Base Color"])
    return mask


def veins(m, color, strength=1.4, scale=0.35, edge=0.07, region=0.0, rscale=0.2):
    """Thin glowing lines (Voronoi edges) over the surface; region > 0 limits them to where a slow noise is above it."""
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    co = N.new("ShaderNodeTexCoord").outputs["Object"]
    vor = N.new("ShaderNodeTexVoronoi")
    vor.feature = "DISTANCE_TO_EDGE"
    vor.inputs["Scale"].default_value = scale
    L.new(co, vor.inputs["Vector"])
    mask = bl._math(nt, "LESS_THAN", vor.outputs["Distance"], edge)
    if region > 0:
        rn = N.new("ShaderNodeTexNoise")
        rn.inputs["Scale"].default_value = rscale
        L.new(co, rn.inputs["Vector"])
        mask = bl._math(nt, "MULTIPLY", mask, bl._math(nt, "GREATER_THAN", rn.outputs[0], region))
    bs.inputs["Emission Color"].default_value = hexrgb(color)
    L.new(bl._math(nt, "MULTIPLY", mask, strength), bs.inputs["Emission Strength"])
    m["glow"] = True
    return mask


def tip_glow(m, color, axis, y0, y1, strength=1.6):
    """An emission that rises from nothing at `y0` to full at `y1` along an axis (flame tongues, fin tips, glowing horn points)."""
    nt = m.node_tree
    N, L = nt.nodes, nt.links
    bs = N["Principled BSDF"]
    sep = N.new("ShaderNodeSeparateXYZ")
    L.new(N.new("ShaderNodeTexCoord").outputs["Object"], sep.inputs[0])
    f = _map(nt, sep.outputs["XYZ".index(axis.upper())], y0, y1)
    bs.inputs["Emission Color"].default_value = hexrgb(color)
    L.new(bl._math(nt, "MULTIPLY", f, strength), bs.inputs["Emission Strength"])
    m["glow"] = True
    return f


# ================================================================ modelling helpers
def _sect(n, w, d, p=2.0):
    out = []
    for k in range(n):
        a = 2 * math.pi * k / n
        c, s = math.cos(a), math.sin(a)
        out.append((w * math.copysign(abs(c) ** (2.0 / p), c), d * math.copysign(abs(s) ** (2.0 / p), s)))
    return out


def lof(name, part, rows, n, mat, p=2.0, caps=(True, True), sub=None):
    """rows = [(y, half width, half depth, x offset, z offset)]: rings up a body piece, smooth (superellipse p), optionally subdivided."""
    rings = [[V((ox + u, y, oz + v)) for u, v in _sect(n, w, d, p)] for y, w, d, ox, oz in rows]
    o = bl.loft(name, rings, mat, caps, part=part)
    sub = (1 if name in ("head", "torso", "chest", "abdomen") else 0) if sub is None else sub
    if sub:
        bl.subsurf(o, sub)
    return o


def limb(name, part, path, radii, n, mat, flat=1.0, p=2.0, sub=0):
    P = [V(q) for q in path]
    n += 2                                                                   # no subdivision on limbs (quad budget): a finer ring instead
    rings = []
    for i, q in enumerate(P):
        tan = (P[min(i + 1, len(P) - 1)] - P[max(i - 1, 0)]).normalized()
        side = V((0, 0, 1)).cross(tan)
        if side.length < 1e-6:
            side = V((1, 0, 0))
        side.normalize()
        fwd = tan.cross(side).normalized()
        r = radii[i]
        rings.append([q + side * u + fwd * v for u, v in _sect(n, r, r * flat, p)])
    o = bl.loft(name, rings, mat, (True, True), part=part)
    if sub:
        bl.subsurf(o, sub)
    return o


def pair(ob, part=None):
    """The right-hand twin of a left piece (mirrored in x); part: the right part's name for a limb piece."""
    c = bl.mirror(ob, "x", ob.name + "_r")
    if part:
        c["part"] = part
    return c


def horn(name, part, pts, r, mat, sides=5, sub=3, taper=(1.0, 0.12)):
    sides, sub = min(sides, 5), min(sub, 2)                                   # quad budget
    return bl.tube(name, pts, r, mat, sides=sides, sub=sub, part=part, taper=taper)


def blade(name, part, base, tip, w, t, mat, normal=(0, 0, 1), prof=(0.6, 1.0, 0.85, 0.5, 0.08), bend=(0, 0, 0)):
    """A flat leaf / blade / feather / fin from base to tip: width w across, thickness t, `normal` the flat's facing, `bend` the offset of the middle."""
    b, tp = V(base), V(tip)
    ax = tp - b
    nrm = V(normal).normalized()
    side = nrm.cross(ax).normalized()
    nrm = ax.cross(side).normalized()
    rings = []
    for i, k in enumerate(prof):
        f = i / (len(prof) - 1)
        c = b + ax * f + V(bend) * math.sin(math.pi * f)
        ww, tt = w * k, t * (0.4 + 0.6 * k)
        rings.append([c + side * ww, c + nrm * tt, c - side * ww, c - nrm * tt])
    return bl.loft(name, rings, mat, (True, True), part=part)


def torus(name, part, center, R, r, mat, plane="z", n=28, m=6):
    """A ring (halo, hoop) round `center`, lying in the xy plane (plane z), the xz plane (y) or the yz plane (x)."""
    rings = []
    for i in range(n):
        a = 2 * math.pi * i / n
        ring = []
        for k in range(m):
            b = 2 * math.pi * k / m
            rr = R + r * math.cos(b)
            u, v, w = rr * math.cos(a), rr * math.sin(a), r * math.sin(b)
            ring.append(V({"z": (u, v, w), "y": (u, w, v), "x": (w, u, v)}[plane]) + V(center))
        rings.append(ring)
    faces = []
    for i in range(n):
        j = (i + 1) % n
        for k in range(m):
            q = (k + 1) % m
            faces.append((i * m + k, i * m + q, j * m + q, j * m + k))
    return bl.obj(name, [p for rg in rings for p in rg], faces, mat, part=part)


def eyes(part, y, x, z, size, mat, tilt=18.0, flat=0.45, n="eye"):
    """A slanted pair of glowing slit eyes."""
    for sx in (1, -1):
        g = bl.gem(f"{n}_{sx}", size, mat, depth=0.5, crown=0.3, sides=6, axis="z", rx=1.0, ry=flat, part=part)
        bl.move(g, (x * sx, y, z), (0, 0, -tilt * sx))


def hand(name, part, wrist, fingers, length, claw, mat, spread=0.7, curl=1.0, r=0.38, thumb=True):
    """A left hand hanging from `wrist`: a palm, `fingers` tube claws curling forward, a thumb; the twin is made by the caller."""
    wx, wy, wz = wrist
    be.boulder(name + "_palm", part, (wx, wy - 0.9, wz + 0.2), 1.25, mat, squash=(1.1, 0.8, 0.7), seg=(6, 4), jag=0.03)
    for k in range(fingers):
        o = (k - (fingers - 1) / 2) * spread
        bl.tube(f"{name}_f{k}", [(wx + o, wy - 1.2, wz + 0.3), (wx + o * 1.2, wy - 1.2 - length * 0.55, wz + 0.5 + curl * 0.5),
                                  (wx + o * 1.2, wy - 1.0 - length, wz + 0.5 + curl * 1.6)], r, claw, sides=4, sub=2, part=part, taper=(r, r * 0.25))
    if thumb:
        bl.tube(name + "_th", [(wx - 1.0, wy - 0.8, wz + 0.8), (wx - 1.3, wy - 1.9, wz + 1.5), (wx - 1.2, wy - 2.8, wz + 2.1)], r, claw, sides=4, sub=2, part=part, taper=(r, r * 0.25))


def boot(name, part, x, toe, mat, w=1.9, tall=2.6):
    """A foot: heel to toe, flat sole on y = 0."""
    return lof(name, part, [(0.0, w * 0.9, toe * 0.55, x, toe * 0.1), (tall * 0.5, w, toe * 0.6, x, toe * 0.1), (tall, w * 0.8, toe * 0.45, x, -0.1)], 8, mat, sub=0)


def swing(objs, pivot, rot):
    """Turns pieces about a pivot point (degrees, XYZ): a wing swept back from its root."""
    for o in objs:
        bl.move(o, (-pivot[0], -pivot[1], -pivot[2]))
        bl.move(o, (0, 0, 0), rot)
        bl.move(o, pivot)


def count():
    return sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")


# ================================================================ the five revised bodies (accuracy pass: docs/devil_forms/group4.md, per-devil canon table)
# Source read this session: ja.wikipedia.org "転生したらスライムだった件" (character section): the seven Primordials are Noir = Diablo, Rouge = Guy Crimson (a red-haired,
# alluring handsome man), Bleu = Rain and Vert = Misery (blue- and green-haired women in dark-crimson maid uniforms), Blanc = Testarossa, Jaune = Carrera (a blonde woman),
# Violet = Ultima (purple hair in a side ponytail, a cheerful-looking girl). The article names no horns, wings or tails for any of them, so none are modelled.
def fit(k, rig):
    """Scales everything built so far (feet stay on y = 0) and the rig's pivots by k: a smaller or taller body from the same numbers."""
    if abs(k - 1.0) > 1e-6:
        for o in bpy.context.scene.collection.objects:
            if o.type == "MESH":
                bl.move(o, scale=(k, k, k))
    return {n: (p, tuple(c * k for c in pv)) for n, (p, pv) in rig.items()}


def person(skin_c, eye_c, cloth, trim, boot, male=False, sleeve=None, glove=None, trouser=None, sh_x=4.8, style=""):
    """The shared human-shaped demon body: legs and boots, a slim torso, arms with plain (clawless) hands, a head with a face. Returns the materials used."""
    sk = skin("%s_skin" % style, skin_c, rough=0.5, bump="leather", bscale=0.5, bstr=0.08, groove=None, bevel=0.1)
    eye = glow("%s_eye" % style, eye_c, 1.9)
    lip = skin("%s_lip" % style, 0xA06070 if not male else 0xB08070, rough=0.5, bevel=0.0, bump=None)
    sleeve = sleeve or cloth
    glove = glove or sk
    trouser = trouser or cloth
    r = (2.3, 1.8, 1.5, 1.3) if male else (2.0, 1.55, 1.3, 1.15)
    lx = 2.7 if male else 2.4
    lg = limb("leg_l", "leg_l", [(lx, 16.2, 0), (lx + 0.2, 9.5, 0.4), (lx + 0.2, 4.8, -0.1), (lx + 0.2, 1.8, 0.2)], r, 8, trouser, sub=1)
    pair(lg, "leg_r")
    bt = lof("boot_l", "leg_l", [(0.0, 1.75, 3.3, lx + 0.2, 1.0), (1.4, 1.85, 3.4, lx + 0.2, 1.0), (3.0, 1.85, 2.1, lx + 0.2, 0.2), (7.6, 2.0, 2.1, lx + 0.2, 0.3), (8.4, 2.05, 2.15, lx + 0.2, 0.3)], 8, boot, p=2.3)
    pair(bt, "leg_r")
    rows = ([(14.6, 3.7, 2.4, 0, 0), (17.5, 3.0, 2.1, 0, 0), (21.0, 3.9, 2.5, 0, 0), (23.2, 5.0, 2.9, 0, 0), (24.7, 3.7, 2.4, 0, 0), (25.7, 2.0, 1.9, 0, 0)] if male else
            [(14.6, 3.4, 2.2, 0, 0), (17.5, 2.7, 1.9, 0, 0), (20.5, 3.2, 2.2, 0, 0.1), (23.0, 4.0, 2.6, 0, 0.1), (24.6, 3.0, 2.2, 0, 0), (25.7, 1.8, 1.7, 0, 0)])
    lof("torso", "torso", rows, 10, cloth, p=2.2)
    ar = (1.8, 1.5, 1.35, 1.4, 1.1) if male else (1.55, 1.25, 1.1, 1.15, 0.95)
    ua = limb("arm_l", "arm_l", [(sh_x + 0.2, 23.4, 0), (sh_x + 0.5, 20.0, 0), (sh_x + 0.8, 16.6, 0)], ar[:3], 8, sleeve, sub=1)
    pair(ua, "arm_r")
    fa = limb("forearm_l", "forearm_l", [(sh_x + 1.0, 16.6, 0), (sh_x + 1.3, 13.0, 0.4), (sh_x + 1.5, 10.2, 0.8)], ar[2:], 8, sleeve, sub=1)
    pair(fa, "forearm_r")
    hand("hand_l", "forearm_l", (sh_x + 1.5, 10.2, 0.8), 4, 1.9, glove, glove, spread=0.5, curl=0.35, r=0.3)
    for o in [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.name.startswith("hand_l") and o["part"] == "forearm_l"]:
        pair(o, "forearm_r")
    w = 3.0 if male else 2.8
    rows = [(25.2, 1.4, 1.7, 0, 1.0), (26.4, w - 0.4, 2.5, 0, 0.8), (28.4, w, 3.0, 0, 0.4), (30.4, w + 0.1, 3.1, 0, 0.1), (31.8, w - 0.6, 2.5, 0, 0)]
    lof("head", "head", rows, 8, sk)
    bl.tube("nose", [(0, 28.2, 3.5), (0, 27.6, 4.2), (0, 27.0, 3.9)], 0.28, sk, sides=4, sub=2, part="head")
    bl.tube("lips", [(-0.8, 26.5, 3.3), (0, 26.35, 3.55), (0.8, 26.5, 3.3)], 0.17, lip, sides=4, sub=2, part="head")
    for sx in (1, -1):
        bl.tube(f"brow_{sx}", [(0.7 * sx, 29.4, 3.5), (2.5 * sx, 29.8, 3.2)], 0.17, glove if male else sk, sides=4, sub=1, part="head")
        be.boulder(f"ear_{sx}", "head", (w * sx + 0.1 * sx, 28.0, 0.4), 0.9, sk, squash=(0.5, 1.2, 0.8), seg=(5, 3), jag=0.02)
    eyes("head", 28.7, 1.6, 3.5, 0.85, eye, tilt=10, flat=0.42)
    return sk


def hair_cap(m, extra=0.0):
    lof("hair_cap", "head", [(29.4, 3.3 + extra, 3.4 + extra, 0, 0.2), (31.2, 3.3 + extra, 3.3 + extra, 0, 0.05), (32.4, 2.5, 2.6, 0, 0), (32.9, 1.0, 1.1, 0, 0)], 8, m, sub=0)


def fringe(m, xs, y0=32.0, y1=29.7, z=3.9):
    for k, x in enumerate(xs):
        blade(f"fringe_{k}", "head", (x * 0.8, y0, 2.4), (x * 1.1, y1 + (k % 2) * 0.5, z), 0.7, 0.22, m, normal=(0, 0, 1), prof=(0.9, 1.0, 0.85, 0.5, 0.1))


def long_hair(m, n, bottom, spread=3.4, wave=0.0, r=0.85):
    for k in range(n):
        a = (k - (n - 1) / 2) / ((n - 1) / 2)
        x0 = spread * a
        pts = [(x0, 31.8, -1.2), (x0 * 1.15, 28.5, -3.7), (x0 * 1.25 + wave * math.sin(k), 21.0, -4.2), (x0 * 1.2 + wave * math.sin(k + 2.0), bottom + abs(a) * 1.8, -3.9 + wave * 0.3)]
        bl.tube(f"hair_{k}", pts, r, m, sides=4, sub=2, part="head", taper=(r, r * 0.25))


def build_jaune():
    """Carrera line, Primordial Jaune (yellow): a blonde woman (ja.wikipedia: 金髪の女性型悪魔). Long loose golden hair, gold eyes, a fitted black jacket with glowing gold piping and tall boots;
    no horns, wings or tail (none are named)."""
    be.seed(31)
    cloth = skin("jaune_cloth", 0x1C1A20, rough=0.7, bump="wrap", bscale=0.6, bstr=0.2, groove=0x08080A, gamount=0.5)
    boot = skin("jaune_boot", 0x16141A, rough=0.45, bump="leather", bscale=0.6, bstr=0.15, groove=0x050506, gamount=0.5)
    trim = glow("jaune_trim", 0xF0D030, 1.5)
    gold = skin("jaune_gold", 0xD8B028, rough=0.3, bump="hammer", bscale=0.5, bstr=0.2, groove=0x7A5A10, gamount=0.5, metallic=0.9)
    hairm = skin("jaune_hair", 0xE8C838, rough=0.45, bump="brushed", bscale=2.2, bstr=0.25, groove=0xA8861C, gamount=0.5)
    person(0xEBD8C6, 0xF0D030, cloth, trim, boot, style="jaune")
    # a short fitted jacket over the bodice: a high collar, gold piping down the front and at the cuffs, a belt with a plain gold buckle
    lof("jacket", "torso", [(19.6, 3.5, 2.4, 0, 0), (22.0, 4.2, 2.8, 0, 0.1), (24.0, 4.2, 2.8, 0, 0), (25.0, 3.1, 2.3, 0, 0), (25.8, 2.1, 1.9, 0, 0)], 10, cloth, p=2.2, sub=0)
    lof("collar", "torso", [(25.0, 2.35, 2.2, 0, 0), (27.0, 2.2, 2.1, 0, 0), (27.4, 2.15, 2.05, 0, 0)], 10, cloth, caps=(False, False), sub=0)
    for sx in (1, -1):
        bl.tube(f"pipe_{sx}", [(0.55 * sx, 25.6, 2.65), (0.6 * sx, 21.0, 3.0), (0.5 * sx, 16.4, 2.5)], 0.16, trim, sides=4, sub=2, part="torso")
        blade(f"jtail_{sx}", "torso", (2.0 * sx, 15.4, -2.1), (2.6 * sx, 7.0, -3.0), 1.5, 0.25, cloth, normal=(0, 0, 1), prof=(1.0, 1.0, 0.9, 0.8, 0.5))
        torus(f"cuff_{sx}", "forearm_l" if sx == 1 else "forearm_r", (6.0 * sx, 13.4, 0.5), 1.6, 0.2, trim, plane="y", n=10, m=3)
    lof("belt", "torso", [(14.8, 3.55, 2.35, 0, 0), (16.2, 3.4, 2.3, 0, 0)], 10, boot, sub=0)
    bl.gem("buckle", 0.9, gold, center=(0, 15.5, 2.4), depth=0.4, crown=0.2, sides=6, axis="z", part="torso")
    # long loose blonde hair with a side-swept fringe
    hair_cap(hairm)
    fringe(hairm, (-2.6, -1.6, -0.6, 0.5, 1.6, 2.7))
    long_hair(hairm, 9, 12.5, wave=0.0)
    for sx in (1, -1):
        bl.tube(f"lock_{sx}", [(3.1 * sx, 30.5, 2.2), (3.7 * sx, 26.5, 2.8), (3.5 * sx, 21.5, 2.7)], 0.5, hairm, sides=4, sub=2, part="head", taper=(0.55, 0.15))
    return fit(1.0, make_rig(hip=15.5, leg_x=2.4, sh_x=4.8))


def build_violet():
    """Ultima line, Primordial Violet (purple): a cheerful-looking girl (一見可愛げのある明るい少女) with purple hair in a side ponytail; shorter and lighter than the others. Black-violet short dress with
    glowing violet piping, violet eyes; no horns, wings or tail (none are named)."""
    be.seed(41)
    cloth = skin("violet_cloth", 0x22152E, rough=0.7, bump="wrap", bscale=0.6, bstr=0.2, groove=0x0A0610, gamount=0.5)
    boot = skin("violet_boot", 0x16101C, rough=0.45, bump="leather", bscale=0.6, bstr=0.15, groove=0x060408, gamount=0.5)
    trim = glow("violet_trim", 0x8A3AC8, 1.6)
    glove = skin("violet_glove", 0x2E1C3C, rough=0.55, bump="leather", bscale=0.7, bstr=0.15, groove=0x0A0610, gamount=0.5)
    hairm = skin("violet_hair", 0x7A38B8, rough=0.45, bump="brushed", bscale=2.2, bstr=0.25, groove=0x3A1860, gamount=0.5)
    person(0xF0DCD4, 0xB060FF, cloth, trim, boot, glove=glove, style="violet")
    # a short flared dress: a ruffled hem with glowing piping, a sash, puffed shoulders
    lof("skirt", "torso", [(16.2, 3.6, 2.4, 0, 0), (13.0, 5.0, 3.6, 0, 0), (9.6, 6.4, 5.0, 0, 0)], 12, cloth, caps=(False, False), p=2.0, sub=0)
    torus("hem", "torso", (0, 9.6, 0), 5.7, 0.3, trim, plane="y", n=16, m=3)
    lof("sash", "torso", [(15.2, 3.45, 2.3, 0, 0), (16.8, 3.35, 2.25, 0, 0)], 10, trim, sub=0)
    for sx in (1, -1):
        be.boulder(f"puff_{sx}", "torso", (5.0 * sx, 23.0, 0), 2.1, cloth, squash=(1.1, 0.9, 1.0), seg=(6, 4), jag=0.02)
        torus(f"cuff_{sx}", "forearm_l" if sx == 1 else "forearm_r", (6.0 * sx, 13.4, 0.5), 1.45, 0.18, trim, plane="y", n=10, m=3)
    bl.tube("collar", [(-2.1, 25.2, 2.0), (0, 24.8, 2.9), (2.1, 25.2, 2.0)], 0.3, trim, sides=4, sub=2, part="torso")
    # purple hair in a side ponytail (her left): a tie high on the side, a thick tail down past the shoulder
    hair_cap(hairm)
    fringe(hairm, (-2.4, -1.4, -0.4, 0.6, 1.6, 2.6))
    for k in range(5):                                                        # a short back so the head is not bald behind the ear
        x = (k - 2) * 1.3
        bl.tube(f"nape_{k}", [(x, 31.4, -1.6), (x * 1.1, 28.8, -3.5), (x * 1.2, 26.2, -3.3)], 0.8, hairm, sides=4, sub=1, part="head", taper=(0.85, 0.3))
    bl.tube("tail_a", [(3.0, 31.6, 0.2), (5.4, 30.6, -0.8), (6.8, 27.0, -1.8), (7.2, 22.0, -2.4), (6.8, 17.0, -2.4)], 1.5, hairm, sides=5, sub=2, part="head", taper=(1.4, 0.35))
    bl.tube("tail_b", [(3.2, 31.0, -0.6), (5.8, 29.6, -1.6), (7.6, 25.6, -2.6), (8.2, 20.0, -2.8)], 1.1, hairm, sides=4, sub=2, part="head", taper=(1.0, 0.3))
    torus("tail_tie", "head", (4.1, 31.2, -0.4), 1.2, 0.3, trim, plane="x", n=10, m=3)
    return fit(0.9, make_rig(hip=15.5, leg_x=2.4, sh_x=4.8))


def build_rouge():
    """Guy Crimson's line, Primordial Rouge (red), Supreme: a tall, alluring handsome man with red hair (妖艶な赤髪を持つ艶のある美丈夫). Long red hair swept back, red eyes, a long black coat with glowing crimson
    piping and a high collar; broad shoulders. No horns, wings or tail (none are named)."""
    be.seed(53)
    cloth = skin("rouge_cloth", 0x18101A, rough=0.62, bump="wrap", bscale=0.5, bstr=0.2, groove=0x060408, gamount=0.5)
    boot = skin("rouge_boot", 0x120C14, rough=0.4, bump="leather", bscale=0.6, bstr=0.15, groove=0x040204, gamount=0.5)
    trim = glow("rouge_trim", 0xC81E2D, 1.6)
    lining = skin("rouge_lining", 0x8A1220, rough=0.5, bump="grain", bscale=0.7, bstr=0.1, groove=0x3A0610, gamount=0.5)
    glove = skin("rouge_glove", 0x1C1218, rough=0.5, bump="leather", bscale=0.7, bstr=0.15, groove=0x060406, gamount=0.5)
    hairm = skin("rouge_hair", 0xB01828, rough=0.45, bump="brushed", bscale=2.2, bstr=0.25, groove=0x560A12, gamount=0.5)
    person(0xE6D2C6, 0xFF3040, cloth, trim, boot, male=True, glove=glove, sh_x=5.6, style="rouge")
    # a long coat to the shins, split behind, crimson lining, high collar with glowing piping, a chest strap
    lof("coat", "torso", [(23.4, 5.2, 3.1, 0, 0), (19.5, 4.4, 2.8, 0, 0), (14.5, 4.9, 3.1, 0, 0), (8.5, 5.5, 3.5, 0, -0.2), (4.0, 5.8, 3.7, 0, -0.4)], 12, cloth, caps=(False, False), p=2.3, sub=0)
    for sx in (1, -1):
        blade(f"lining_{sx}", "torso", (2.0 * sx, 14.5, 3.3), (3.6 * sx, 4.2, 3.9), 1.4, 0.2, lining, normal=(0, 0, 1), prof=(1.0, 1.0, 1.0, 0.95, 0.85))
        blade(f"collar_{sx}", "torso", (1.5 * sx, 24.4, 1.9), (3.7 * sx, 29.6, 0.6), 1.5, 0.28, cloth, normal=(1 * sx, 0, 0.4), prof=(0.9, 1.0, 1.0, 0.8, 0.4))
        bl.tube(f"pipe_{sx}", [(0.7 * sx, 24.4, 3.2), (1.0 * sx, 19.0, 3.4), (1.3 * sx, 13.0, 3.5), (2.2 * sx, 5.0, 4.0)], 0.17, trim, sides=4, sub=2, part="torso")
        be.boulder(f"epaulette_{sx}", "torso", (6.0 * sx, 24.0, 0), 2.1, cloth, squash=(1.2, 0.6, 1.1), seg=(6, 4), jag=0.02)
        torus(f"cuff_{sx}", "forearm_l" if sx == 1 else "forearm_r", (6.8 * sx, 13.4, 0.5), 1.75, 0.2, trim, plane="y", n=10, m=3)
    lof("belt", "torso", [(14.8, 4.5, 3.0, 0, 0), (16.2, 4.4, 2.95, 0, 0)], 12, boot, sub=0)
    # long red hair swept back from the forehead to the middle of the back, two locks at the cheeks
    hair_cap(hairm)
    for k, x in enumerate((-2.4, -1.2, 0.0, 1.2, 2.4)):
        blade(f"forelock_{k}", "head", (x * 0.7, 32.0, 2.0), (x * 1.5, 29.4 + abs(x) * 0.5, 3.3 - abs(x) * 0.3), 0.8, 0.25, hairm, normal=(0, 0, 1), prof=(0.9, 1.0, 0.8, 0.45, 0.1))
    long_hair(hairm, 9, 10.5, spread=3.6, wave=0.5)
    for sx in (1, -1):
        bl.tube(f"lock_{sx}", [(3.2 * sx, 30.2, 2.2), (3.8 * sx, 26.0, 2.9), (3.6 * sx, 21.0, 2.8)], 0.45, hairm, sides=4, sub=2, part="head", taper=(0.5, 0.12))
    return fit(1.04, make_rig(hip=15.0, leg_x=2.7, sh_x=5.6))


def maid(hair_c, eye_c, style, hair_fn, size, name):
    """The dark-crimson maid uniform both Misery (Vert) and Rain (Bleu) wear (暗紅色のメイド服): a dark crimson dress with a long skirt, white apron, collar and cuffs, black stockings and shoes."""
    be.seed(67 if style == "vert" else 79)
    cloth = skin(style + "_dress", 0x5E0E1E, rough=0.7, bump="wrap", bscale=0.6, bstr=0.2, groove=0x240510, gamount=0.5)
    white = skin(style + "_apron", 0xEEE8E4, rough=0.75, bump="wrap", bscale=0.8, bstr=0.15, groove=0xA89A94, gamount=0.5)
    boot = skin(style + "_shoe", 0x141014, rough=0.4, bump="leather", bscale=0.6, bstr=0.1, groove=0x040304, gamount=0.5)
    stock = skin(style + "_stocking", 0x1A161C, rough=0.6, bump="wrap", bscale=1.2, bstr=0.1, groove=0x060406, gamount=0.5)
    trim = glow(style + "_trim", hair_c, 1.5)
    person(0xEFE0D2, eye_c, cloth, trim, boot, glove=white, trouser=stock, sleeve=cloth, style=style)
    lof("skirt", "torso", [(16.4, 3.5, 2.3, 0, 0), (12.0, 5.2, 3.8, 0, 0), (7.4, 6.8, 5.2, 0, 0), (6.0, 7.0, 5.4, 0, 0)], 12, cloth, caps=(False, False), p=2.0, sub=0)
    apron = blade("apron", "torso", (0, 16.2, 3.3), (0, 7.2, 5.6), 3.2, 0.25, white, normal=(0, 0, 1), prof=(0.5, 0.8, 1.0, 1.0, 0.95), bend=(0, 0, 0.4))
    blade("bib", "torso", (0, 24.0, 2.7), (0, 16.4, 3.0), 2.0, 0.22, white, normal=(0, 0, 1), prof=(0.6, 0.8, 1.0, 1.0, 0.8))
    for sx in (1, -1):
        bl.tube(f"strap_{sx}", [(1.7 * sx, 24.0, 2.6), (2.5 * sx, 25.4, 0.8), (2.6 * sx, 24.6, -1.4)], 0.22, white, sides=4, sub=2, part="torso")
        torus(f"cuff_{sx}", "forearm_l" if sx == 1 else "forearm_r", (6.0 * sx, 13.4, 0.5), 1.45, 0.3, white, plane="y", n=10, m=3)
    lof("collar", "torso", [(24.4, 2.3, 2.2, 0, 0), (25.8, 2.1, 2.0, 0, 0)], 10, white, caps=(False, False), sub=0)
    lof("waistband", "torso", [(15.6, 3.1, 2.1, 0, 0.1), (16.6, 3.05, 2.1, 0, 0.1)], 10, white, sub=0)
    # the headdress: a frilled white band across the top of the head
    bl.tube("headband", [(-3.3, 30.2, 1.0), (-1.8, 32.2, 2.4), (0, 32.7, 2.6), (1.8, 32.2, 2.4), (3.3, 30.2, 1.0)], 0.42, white, sides=5, sub=2, part="head")
    for k in range(5):
        blade(f"frill_{k}", "head", ((k - 2) * 1.2, 32.6 - abs(k - 2) * 0.3, 2.6), ((k - 2) * 1.2, 33.8 - abs(k - 2) * 0.4, 2.0), 0.6, 0.2, white, normal=(0, 0, 1), prof=(0.8, 1.0, 0.8, 0.4, 0.1))
    mh = skin(style + "_hair", hair_c, rough=0.45, bump="brushed", bscale=2.2, bstr=0.25, groove=hair_c & 0x7F7F7F, gamount=0.5)
    hair_cap(mh)
    hair_fn(mh)
    return fit(size, make_rig(hip=15.5, leg_x=2.4, sh_x=4.8))


def build_vert():
    """Misery's line, Primordial Vert (green): a green-haired woman in the dark-crimson maid uniform (外見は緑髪の美女で、暗紅色のメイド服). Diligent: a neat bun at the back of the head, a straight fringe; slightly taller."""
    def hair(m):
        fringe(m, (-2.4, -1.2, 0.0, 1.2, 2.4))
        be.boulder("bun", "head", (0, 30.2, -4.4), 2.3, m, squash=(1.0, 1.0, 0.9), seg=(7, 5), jag=0.02)
        for k in range(5):
            x = (k - 2) * 1.2
            bl.tube(f"nape_{k}", [(x, 31.4, -1.6), (x * 1.1, 29.4, -3.8)], 0.8, m, sides=4, sub=1, part="head", taper=(0.85, 0.5))
        for sx in (1, -1):
            bl.tube(f"lock_{sx}", [(3.1 * sx, 30.5, 2.2), (3.6 * sx, 26.8, 2.8), (3.4 * sx, 23.2, 2.6)], 0.4, m, sides=4, sub=2, part="head", taper=(0.45, 0.12))
    return maid(0x3AA85A, 0x60F080, "vert", hair, 1.02, "vert")


def build_bleu():
    """Rain's line, Primordial Bleu (blue): a blue-haired woman in the dark-crimson maid uniform (外見は青髪の美女で、暗紅色のメイド服). Loose and easy-going (a lazy, spoiled youngest): long
    loose wavy hair down to the waist with a crooked fringe; a little shorter than Misery."""
    def hair(m):
        for k, x in enumerate((-2.6, -1.7, -0.6, 0.4, 1.5, 2.7)):
            blade(f"fringe_{k}", "head", (x * 0.8, 32.0, 2.4), (x * 1.2 + 0.6, 29.4 + (k % 3) * 0.5, 3.9), 0.75, 0.22, m, normal=(0, 0, 1), prof=(0.9, 1.0, 0.85, 0.5, 0.1))
        long_hair(m, 9, 11.0, spread=3.5, wave=0.8)
        for sx in (1, -1):
            bl.tube(f"lock_{sx}", [(3.1 * sx, 30.5, 2.2), (3.9 * sx, 26.0, 2.9), (3.6 * sx, 20.0, 2.6)], 0.5, m, sides=4, sub=2, part="head", taper=(0.55, 0.15))
    return maid(0x3A6AE8, 0x8AB0FF, "bleu", hair, 0.97, "bleu")


BUILDERS = {"daemon_jaune": build_jaune, "daemon_violet": build_violet, "daemon_rouge": build_rouge, "daemon_vert": build_vert, "daemon_bleu": build_bleu}
GLOW = {"daemon_jaune": "F0D030", "daemon_violet": "8A3AC8", "daemon_rouge": "C81E2D", "daemon_vert": "3AA85A", "daemon_bleu": "3A6AE8"}
SEED = {"daemon_jaune": 31, "daemon_violet": 41, "daemon_rouge": 53, "daemon_vert": 67, "daemon_bleu": 79}


def main(argv):
    arg = lambda k, d=None: argv[argv.index(k) + 1] if k in argv else d   # noqa: E731
    trial, only = arg("--trial"), arg("--only")
    quick, check = "--quick" in argv, "--check" in argv
    pv = trial or PREVIEW_DIR
    orig = be.render_previews

    def crop_previews(low, names, rig, atlas, previews, poses):
        orig(low, names, rig, atlas, previews, poses, res=(900, 1100))
        from PIL import Image, ImageChops
        for path in previews:                                                # crop the render to the figure so it fills the picture
            im = Image.open(path).convert("RGB")
            bg = Image.new("RGB", im.size, im.getpixel((2, 2)))
            box = ImageChops.difference(im, bg).convert("L").point(lambda v: 255 if v > 14 else 0).getbbox()
            if box:
                m = 24
                box = (max(box[0] - m, 0), max(box[1] - m, 0), min(box[2] + m, im.width), min(box[3] + m, im.height))
                im = im.crop(box)
            im.thumbnail((640, 760), Image.LANCZOS)
            im.quantize(colors=200, method=Image.Quantize.MEDIANCUT).save(path, optimize=True)

    be.render_previews = crop_previews
    for did, fn in BUILDERS.items():
        if only and only != did:
            continue
        bl.reset(samples=8 if quick else 24)
        rig = fn()
        eid = "devil_" + did
        print(did, "faces before export:", count())
        if check:
            top = sorted((o for o in bpy.context.scene.collection.objects if o.type == "MESH"), key=lambda o: -len(o.data.polygons))[:14]
            print("   biggest:", ", ".join(f"{o.name}={len(o.data.polygons)}" for o in top))
            continue
        be.finish_entity(eid, rig, glow=(GLOW[did], 0.1, 0.55, 1.0), skin=1024 if not quick else 512, bake=2048 if not quick else 512, lod_ratio=0.28,
                         out_mesh=trial, out_skin=trial, previews={os.path.join(pv, did + ".png"): (28, None)}, poses={}, cage=0.2)
        from PIL import Image                                                  # a 256-colour skin: the baked noise would otherwise make it about 1 MB
        sp = os.path.join(trial or be.ENTITY_SKIN_DIR, eid + ".png")
        Image.open(sp).convert("RGBA").quantize(colors=256, method=Image.Quantize.FASTOCTREE).save(sp, optimize=True)


if __name__ == "__main__":
    main(sys.argv[1:])

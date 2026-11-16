"""Devil bodies, group 2 (Devil Contract system): fox_devil, curse_devil, future_devil, ghost_devil, sebastian, hall_demon.
MADE IN BLENDER (bmesh lofts, sweeps, Subdivision Surface, Solidify, Decimate, Smart UV, Cycles bake) through tools/blender/bl_lib.py + bl_entity.py; see docs/devil_forms/group2.md.

Every devil is an upright player-scale humanoid (the model unit is 1/16 block, the player is about 29 units tall; these stand 29 to 33 units, horns and ears excluded),
parts torso head arm_l arm_r forearm_l forearm_r leg_l leg_r (+ wing_l wing_r / tail where the devil has them), pivots at the joints, left = +x, face = +z.

Run (Blender 5.x as a Python module, see docs/heroweapon/blender_pipeline.md):
    python -B tools/blender/devil_group2.py [--only fox_devil,sebastian] [--trial DIR] [--quick]
Writes entity_meshes/devil_<id>.txt (+ _lod), textures/entity/boss_mesh/devil_<id>(.png, _glow.png) and docs/devil_forms/previews/<id>.png.
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
ROLES = ["torso", "head", "arm_l", "arm_r", "forearm_l", "forearm_r", "leg_l", "leg_r", "wing_l", "wing_r", "tail"]
def V(*a):
    return Vector(a[0] if len(a) == 1 else a)


PI = math.pi


# ================================================================ shared helpers
def M(name, rgb, glow=None, strength=0.0, **kw):
    """A material from sRGB 0..255; glow=(r, g, b) + strength makes it emit (it then bakes into the glow skin)."""
    kw.setdefault("bevel", 0.1)
    if glow is not None:
        return bl.mat(name, bl.rgb(*rgb), emit=bl.rgb(*glow), strength=strength, **kw)
    return bl.mat(name, bl.rgb(*rgb), **kw)


def Glow(name, rgb, strength=10.0):
    return bl.mat(name, bl.rgb(*rgb), rough=0.3, emit=bl.rgb(*rgb), strength=strength * 0.17, bevel=0.0)   # a low strength keeps the hue (a high one bakes to white)


def ring_pts(y, w, d, ox=0.0, oz=0.0, n=10, jit=0.0):
    out = []
    for k in range(n):
        a = 2 * PI * k / n
        r = 1.0 + (be.RNG.uniform(-jit, jit) if jit else 0.0)
        out.append(V((ox + w * math.cos(a) * r, y, oz + d * math.sin(a) * r)))
    return out


def torso(name, rows, mat, n=10, part="torso", sub=1, caps=(True, True), jit=0.0):
    """rows = [(y, half width, half depth, x offset, z offset)] lofted up a body piece, smoothed."""
    ob = bl.loft(name, [ring_pts(*r, n=n, jit=jit) for r in rows], mat, caps, part=part)
    if sub:
        bl.subsurf(ob, sub)
    return ob


def limb(name, part, pts, radii, mat, sides=8, flat=1.0, sub=3, caps=(True, True), up=(0, 0, 1)):
    """A smooth round limb through points with a radius per point (an oval when flat != 1)."""
    path = bl.smooth_path(pts, sub)

    def sc(t):
        f = t * (len(radii) - 1)
        i = min(int(f), len(radii) - 2)
        u = f - i
        r = radii[i] * (1 - u) + radii[i + 1] * u
        return (r, r * flat)
    return bl.sweep(name, path, bl.circle(1.0, sides), mat, scale=sc, part=part, caps=caps, up=up)


def horn(name, part, pts, r, mat, sides=5, sub=2, tip=0.12):
    return bl.tube(name, pts, r, mat, sides=sides, sub=sub, part=part, taper=(r, r * tip))


def twin(ob):
    p = ob["part"]
    return be.side_copy(ob, p[:-2] + "_r" if p.endswith("_l") else p)


def hand(tag, part, wrist, mat, claw, s=1.0, fingers=4, claw_len=1.2, spread=0.65, curl=1.0, rad=0.32, thumb=True, mirror=True, palm=True):
    """A hand hanging from `wrist` (left side, +x): palm, fingers with claws, a thumb to the front. mirror=True makes the right twin."""
    w = V(wrist)
    made = []
    if palm:
        made.append(be.boulder(f"palm_{tag}_l", part, w + V(0, -1.3 * s, 0.3 * s), 1.35 * s, mat, squash=(1.0, 1.1, 0.55), seg=(6, 4), jag=0.03))
    for k in range(fingers):
        dx = (k - (fingers - 1) / 2) * spread * s
        p0, p1, p2 = w + V(dx, -2.0 * s, 0.3 * s), w + V(dx * 1.1, -3.7 * s, 0.6 * s * curl), w + V(dx * 1.2, -5.2 * s, 1.5 * s * curl)
        made.append(bl.tube(f"finger_{tag}_l{k}", [tuple(p0), tuple(p1), tuple(p2)], rad * s, mat, sides=4, sub=1, part=part, taper=(rad * s, rad * s * 0.55)))
        if claw is not None and claw_len > 0:
            made.append(be.shard(f"claw_{tag}_l{k}", part, p2, p2 + V(0, -claw_len, 0.7 * curl), rad * s * 0.8, claw, n=4))
    if thumb:
        t0, t1 = w + V(-0.8 * s, -1.3 * s, 0.6 * s), w + V(-0.9 * s, -2.5 * s, 1.9 * s)
        made.append(bl.tube(f"thumb_{tag}_l", [tuple(t0), tuple(t1)], rad * s, mat, sides=4, sub=1, part=part, taper=(rad * s, rad * s * 0.6)))
    if mirror:
        for o in made:
            twin(o)
    return made


def foot(tag, part, ankle, mat, l=3.2, w=1.8, h=1.0, toes=0, claw=None, mirror=True, toe_len=1.4):
    """A foot: a low wedge forward of the ankle, optional toes with claws."""
    a = V(ankle)
    made = [be.boulder(f"foot_{tag}_l", part, a + V(0, h * 0.8, l * 0.45), l * 0.55, mat, squash=(w / (l * 0.55), h / (l * 0.55), 1.0), seg=(7, 4), jag=0.02)]
    for k in range(toes):
        dx = (k - (toes - 1) / 2) * (w * 1.4 / max(toes, 1))
        p0, p1 = a + V(dx, 0.7, l * 0.85), a + V(dx * 1.2, 0.35, l * 0.85 + toe_len)
        made.append(bl.tube(f"toe_{tag}_l{k}", [tuple(p0), tuple(p1)], 0.4, mat, sides=4, sub=1, part=part, taper=(0.45, 0.28)))
        if claw is not None:
            made.append(be.shard(f"toeclaw_{tag}_l{k}", part, p1, p1 + V(0, -0.25, 1.0), 0.28, claw, n=4))
    if mirror:
        for o in made:
            twin(o)
    return made


def snout(name, part, profile, loc, rot, mat, sides=8, squash=0.75):
    """A muzzle: a profile [(radius, length)] turned about the z axis (the face direction), then moved and tilted (degrees)."""
    ob = bl.lathe(name, profile, mat, sides=sides, part=part, squash=squash, axis="z")
    bl.move(ob, loc, rot)
    return ob


def std_rig(hip=14.5, sh=23.0, neck=24.5, sw=5.4, el=16.5, hipx=1.9, headz=0.0, wing=None, tail=None, elx=0.4):
    rig = {"torso": (None, (0.0, hip, 0.0)), "head": ("torso", (0.0, neck, headz)),
           "arm_l": ("torso", (sw, sh, 0.0)), "arm_r": ("torso", (-sw, sh, 0.0)),
           "forearm_l": ("arm_l", (sw + elx, el, 0.2)), "forearm_r": ("arm_r", (-(sw + elx), el, 0.2)),
           "leg_l": (None, (hipx, hip, 0.0)), "leg_r": (None, (-hipx, hip, 0.0))}
    if wing:
        rig["wing_l"] = ("torso", tuple(wing))
        rig["wing_r"] = ("torso", (-wing[0], wing[1], wing[2]))
    if tail:
        rig["tail"] = ("torso", tuple(tail))
    return rig


def arms(sw, sh, el, wr, r, mat, cuff=None, sleeve=None, flat=1.0, bend=(0.4, 0.2), sides=8, r_el=None, r_wr=None):
    """Upper arm (arm_l) and forearm (forearm_l) with their right twins. Returns the wrist point."""
    ex, ez = sw + bend[0], bend[1]
    r_el = r * 0.85 if r_el is None else r_el
    r_wr = r * 0.7 if r_wr is None else r_wr
    ua = limb("arm_l", "arm_l", [(sw, sh, 0), (sw + 0.2, (sh + el) / 2, 0.1), (ex, el, ez)], [r, r * 0.95, r_el], mat, sides=sides, flat=flat)
    twin(ua)
    fa = limb("forearm_l", "forearm_l", [(ex, el, ez), (ex + 0.2, (el + wr) / 2, ez + 0.4), (ex + 0.4, wr, ez + 0.5)], [r_el, r_el * 0.95, r_wr], sleeve or mat, sides=sides, flat=flat)
    twin(fa)
    return V(ex + 0.4, wr, ez + 0.5)


def dome(name, part, y0, y1, w, d, oz, gap, mat, rows=7, n=12, thick=0.45, flare=0.0):
    """An open hood: a dome of rings from y0 up to y1 (half width w, half depth d, centred at z = oz), open at the front over an arc of `gap` radians; solidified."""
    rings = []
    for i in range(rows):
        t = i / (rows - 1)
        e = math.sqrt(max(1.0 - t ** 2 * 0.96, 0.04))
        a0, a1 = PI / 2 + gap / 2, PI / 2 + 2 * PI - gap / 2
        rings.append([V(w * (1 + flare * (1 - t)) * e * math.cos(a0 + (a1 - a0) * k / (n - 1)), y0 + (y1 - y0) * t,
                        oz + d * (1 + flare * (1 - t)) * e * math.sin(a0 + (a1 - a0) * k / (n - 1))) for k in range(n)])
    ob = bl.loft(name, rings, mat, (False, False), part=part, closed=False)
    bl.solidify(ob, thick)
    return ob


def scale_part(part, k, pivot):
    """Scales every piece of one part about a point (a big head on a small body)."""
    from mathutils import Matrix
    m = Matrix.Translation(V(pivot)) @ Matrix.Diagonal((k, k, k, 1.0)) @ Matrix.Translation(-V(pivot))
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and o.get("part") == part:
            o.data.transform(m)
            o.data.update()


def scale_all(rig, k):
    """The whole devil (every mesh and every pivot) scaled about the feet: ears, horns, halos and raised hands make the total taller than the body, so a few are fitted to the player."""
    from mathutils import Matrix
    m = Matrix.Diagonal((k, k, k, 1.0))
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH":
            o.data.transform(m)
            o.data.update()
    return {n: (par, tuple(c * k for c in p)) for n, (par, p) in rig.items()}


def order_parts():
    """bl_entity needs the parts in order (a parent before its children): re-link the objects in the role order."""
    sc = bpy.context.scene.collection
    obs = [o for o in sc.objects]
    obs.sort(key=lambda o: ROLES.index(o["part"]) if o.get("part") in ROLES else 99)
    for o in obs:
        sc.objects.unlink(o)
    for o in obs:
        sc.objects.link(o)


def count():
    return sum(bl.quads_of(o) for o in bpy.context.scene.collection.objects if o.type == "MESH")


# ================================================================ 1. the Fox Devil
def fox_devil():
    """Chainsaw Man's Fox Devil (a she; a friendly devil whose head and body parts are summoned through the fox hand sign "Kon"): a white fox with a huge head, long muzzle and wide-open jaws, tall pointed ears,
    orange (the devil colour) ear tips and face markings. Nothing else is invented: no tail, no extra heads, no armour."""
    be.seed(31)
    fur = M("fox_fur", (238, 230, 214), rough=0.9, bump="leather", bscale=1.6, bstr=0.35, groove=bl.rgb(178, 168, 150), gamount=0.5)
    cream = M("fox_cream", (252, 248, 238), rough=0.9, bump="leather", bscale=1.6, bstr=0.3, groove=bl.rgb(204, 194, 174), gamount=0.5)
    orange = M("fox_orange", (224, 138, 42), rough=0.9, bump="leather", bscale=1.6, bstr=0.35, groove=bl.rgb(150, 76, 18), gamount=0.5)
    black = M("fox_black", (30, 24, 26), rough=0.8, bump="leather", bscale=1.6, bstr=0.3)
    fang = M("fox_fang", (246, 240, 224), rough=0.4)
    maw = Glow("fox_maw", (255, 90, 30), 9.0)
    amber = Glow("fox_amber", (255, 170, 50), 12.0)
    # ---- torso: lean, a cream bib, a soft ruff of fur at the neck
    torso("torso", [(11, 3.6, 2.3, 0, 0), (15, 3.0, 2.1, 0, 0), (19, 3.7, 2.4, 0, 0.2), (23, 4.6, 2.8, 0, 0.2), (25.5, 3.0, 2.2, 0, 0.3)], fur, n=10)
    be.boulder("bib", "torso", (0, 19.5, 2.2), 3.4, cream, squash=(0.95, 1.45, 0.45), seg=(7, 5), jag=0.03)
    for k in range(13):
        a = 2 * PI * k / 13
        be.shard(f"ruff{k}", "torso", (2.6 * math.cos(a), 24.4, 1.6 * math.sin(a)), (4.4 * math.cos(a), 21.8, 2.6 * math.sin(a) + 0.4), 0.9, cream, n=4)
    # ---- head: a big fox head, a long muzzle, the lower jaw hanging wide open on a glowing mouth, fangs, tall ears, orange face markings
    torso("head", [(23.6, 1.9, 2.0, 0, 0.3), (25.2, 3.2, 3.4, 0, 0.4), (27.4, 3.6, 3.6, 0, 0.2), (29.4, 3.0, 3.0, 0, -0.1), (30.4, 1.6, 1.8, 0, -0.2)], fur, n=10)
    snout("muzzle", "head", [(2.6, 2.5), (1.9, 6.0), (1.1, 9.5), (0.5, 11.0)], (0, 26.4, 0), (4, 0, 0), cream, sides=8, squash=0.7)
    be.boulder("nose", "head", (0, 25.9, 11.1), 0.85, black, squash=(1.1, 0.8, 0.8), seg=(5, 4), jag=0.02)
    snout("jaw", "head", [(1.9, 2.0), (1.5, 5.0), (0.9, 7.6), (0.4, 8.6)], (0, 23.6, 1.0), (-26, 0, 0), cream, sides=8, squash=0.55)
    snout("mouth_glow", "head", [(1.55, 2.4), (1.25, 5.0), (0.7, 7.2)], (0, 24.8, 1.0), (-14, 0, 0), maw, sides=6, squash=0.5)
    for sx in (1, -1):
        horn(f"fang_up{sx}", "head", [(1.0 * sx, 25.4, 8.2), (1.05 * sx, 23.4, 8.9)], 0.42, fang, sides=4, sub=1)
        horn(f"fang_low{sx}", "head", [(0.8 * sx, 20.6, 7.4), (0.85 * sx, 22.8, 8.0)], 0.38, fang, sides=4, sub=1)
        bl.gem(f"eye_{sx}", 0.75, amber, center=(2.1 * sx, 27.6, 3.4), depth=0.45, crown=0.3, sides=6, axis="z", rx=1.2, ry=0.5, part="head")
        be.shard(f"ear{sx}", "head", (2.1 * sx, 29.0, 0.0), (4.2 * sx, 36.5, -1.4), 1.9, fur, n=4)
        be.shard(f"ear_tip{sx}", "head", (3.4 * sx, 33.6, -0.9), (4.2 * sx, 37.0, -1.5), 0.95, orange, n=4)
        be.shard(f"ear_in{sx}", "head", (2.1 * sx, 29.3, 0.5), (3.6 * sx, 34.6, -0.2), 1.0, orange, n=4)
        for k in range(2):
            be.shard(f"cheek{sx}_{k}", "head", (3.3 * sx, 25.8 - k * 1.1, 1.4), (5.6 * sx, 24.7 - k * 1.5, 0.2), 0.7, cream, n=4)
            bl.tube(f"mark{sx}_{k}", [(3.52 * sx, 28.1 - k * 0.8, 3.0), (3.45 * sx, 27.2 - k * 0.8, 3.6), (3.2 * sx, 26.4 - k * 0.8, 4.3)], 0.2, orange, sides=4, sub=1, part="head")
    # ---- arms: long, orange forearms like mitts, white paws with claws
    wr = arms(5.2, 22.0, 15.5, 9.5, 1.45, fur, sleeve=orange)
    hand("paw", "forearm_l", wr, fur, fang, s=1.0, claw_len=1.5)
    # ---- digitigrade legs: thigh forward, hock back, the paw flat
    lg = limb("leg_l", "leg_l", [(1.9, 14.5, 0), (2.2, 10.2, 1.6), (2.1, 5.6, -1.2), (2.0, 1.6, 0.6)], [2.5, 2.1, 1.4, 1.1], fur, sides=8)
    twin(lg)
    foot("paw", "leg_l", (2.0, 0.0, -0.2), orange, l=4.2, w=1.5, h=1.0, toes=3, claw=fang)
    scale_part("head", 1.3, (0, 24.0, 0.5))                                   # the fox head is the Devil: it is big
    return std_rig(hip=14.5, sh=22.0, neck=24.0, sw=5.2, el=15.5, headz=0.5), ("FF8A2A", 0.14, 0.6, 1.0)


# ================================================================ 2. the Curse Devil
def curse_devil():
    """The Curse Devil (Chainsaw Man): the canon only says it is the fear of curses, that a contractor stabs the victim with a sword three times and it manifests to crush them (Aki's and Santa Claus's contract).
    Its looks are not known to the builder, so this is a neutral figure: a gaunt hooded shape in tattered violet-black cloth with long arms and violet-lit eyes. No sigils, thorns or nails."""
    be.seed(43)
    robe = M("cu_robe", (36, 20, 54), rough=0.9, bump="wrap", bscale=0.5, bstr=0.35, groove=bl.rgb(14, 8, 24), gamount=0.6)
    skin = M("cu_skin", (150, 128, 172), rough=0.7, bump="leather", bscale=0.6, bstr=0.2, groove=bl.rgb(90, 70, 110), gamount=0.4)
    void = M("cu_void", (8, 4, 14), rough=1.0, bevel=0.0)
    curse = Glow("cu_curse", (176, 84, 255), 12.0)
    torso("torso", [(11, 3.4, 2.3, 0, 0), (15, 2.8, 2.0, 0, 0.3), (19, 3.4, 2.4, 0, 0.6), (23, 4.6, 2.8, 0, 0.9), (25.5, 3.0, 2.2, 0, 1.2)], robe, n=10)
    rows = [(12.5, 4.2, 3.0, 0, 0.2), (9.0, 4.9, 3.6, 0, 0.2), (5.5, 5.4, 4.0, 0, 0.2), (2.4, 5.8, 4.4, 0, 0.2)]
    sk = bl.loft("skirt", [ring_pts(*r, n=12, jit=0.14) for r in rows], robe, (False, False), part="torso")
    bl.solidify(sk, 0.35)
    for k, (x, ln) in enumerate([(-3.2, 7), (-1.0, 9), (1.4, 6), (3.3, 8)]):  # hanging rags
        bl.plate(f"rag{k}", [(x - 0.9, 12.8), (x + 0.9, 12.8), (x + 0.6, 12.8 - ln), (x - 0.4, 11.8 - ln)], -0.12, 0.12, robe, part="torso")
        bl.move(bpy.data.objects[f"rag{k}"], (0, 0, 4.6))
    torso("head", [(23.8, 1.8, 1.9, 0, 1.2), (25.5, 2.7, 3.0, 0, 1.4), (28.2, 2.9, 3.2, 0, 1.2), (30.0, 2.1, 2.4, 0, 0.8)], void, n=10)
    dome("hood", "head", 23.4, 33.0, 4.2, 4.6, 0.6, 1.5, robe, rows=7, n=11, thick=0.5, flare=0.12)
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.85, curse, center=(1.15 * sx, 27.2, 3.9), depth=0.5, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.38, part="head")
    ua = limb("arm_l", "arm_l", [(5.0, 22.2, 0), (5.2, 17.0, 0.2), (5.6, 12.6, 0.8)], [1.5, 1.2, 1.0], robe, sides=8)
    twin(ua)
    sl = limb("forearm_l", "forearm_l", [(5.6, 12.6, 0.8), (5.8, 9.6, 1.4), (6.0, 7.0, 1.8)], [1.5, 2.0, 2.6], robe, sides=10, caps=(True, False))
    twin(sl)
    hand("claw", "forearm_l", V(6.0, 7.2, 1.8), skin, skin, s=0.85, claw_len=1.4, spread=0.55, rad=0.24)
    lg = limb("leg_l", "leg_l", [(1.8, 14.5, 0), (2.0, 9.5, 1.0), (2.0, 5.0, -0.4), (2.0, 1.4, 0.5)], [1.9, 1.4, 1.0, 0.9], skin, sides=7)
    twin(lg)
    foot("bare", "leg_l", (2.0, 0.0, -0.1), skin, l=3.6, w=1.2, h=0.8, toes=3, claw=None, toe_len=1.8)
    return std_rig(hip=14.5, sh=22.2, neck=24.0, sw=5.0, el=12.6, headz=1.0, elx=0.6), ("B054FF", 0.12, 0.5, 1.0)


# ================================================================ 3. the Future Devil
def future_devil():
    """The Future Devil (Chainsaw Man, a he): the devil of the fear of the future, who sees the future of whoever he touches; he made a contract with Aki and lives in his right eye. The canon looks are not known to
    the builder, so this is a neutral slim figure in a plain dark-teal coat with one big glowing right eye (the eye he lives in) and the left eye shut. No halo, wings, armour or dials."""
    be.seed(57)
    cloth = M("fu_cloth", (24, 70, 82), rough=0.55, bump="cross", bscale=1.2, bstr=0.15, groove=bl.rgb(12, 36, 44), gamount=0.4)
    dark = M("fu_dark", (12, 30, 38), rough=0.5)
    skin = M("fu_skin", (214, 226, 228), rough=0.5)
    hair = M("fu_hair", (16, 40, 48), rough=0.4, bump="brushed", bscale=2.0, bstr=0.2)
    cy = Glow("fu_cyan", (58, 220, 220), 13.0)
    torso("torso", [(12, 3.4, 2.2, 0, 0), (15.5, 2.8, 2.0, 0, 0), (19, 3.6, 2.4, 0, 0.1), (22.5, 4.5, 2.7, 0, 0.1), (25, 2.6, 2.0, 0, 0.2)], cloth, n=8)
    for sx in (1, -1):                                                       # a plain coat open below the waist
        for z, nm in ((2.4, "f"), (-2.2, "b")):
            bl.plate(f"coat{nm}{sx}", [(0.4 * sx, 14.0), (3.6 * sx, 14.0), (3.2 * sx, 5.0), (1.8 * sx, 1.8)], -0.2, 0.2, dark if nm == "b" else cloth, part="torso")
            bl.move(bpy.data.objects[f"coat{nm}{sx}"], (0, 0, z))
    torso("head", [(23.8, 1.7, 1.8, 0, 0.2), (25.4, 2.5, 2.7, 0, 0.3), (27.6, 2.8, 2.9, 0, 0.1), (29.6, 2.2, 2.4, 0, -0.1), (30.6, 1.0, 1.2, 0, -0.2)], skin, n=10)
    torso("hair_cap", [(27.8, 2.9, 3.0, 0, -0.1), (29.6, 2.8, 2.9, 0, -0.1), (30.9, 2.0, 2.1, 0, -0.2), (31.4, 0.8, 0.9, 0, -0.2)], hair, n=10, part="head")
    bl.gem("eye_big", 1.45, cy, center=(-1.25, 27.0, 3.0), depth=0.5, crown=0.45, sides=8, axis="z", rx=1.5, ry=1.35, part="head")   # the right eye (the character's right = -x)
    bl.tube("eye_ring", [(-1.25 + 1.95 * math.cos(2 * PI * k / 8), 27.0 + 1.8 * math.sin(2 * PI * k / 8), 2.95) for k in range(9)], 0.16, dark, sides=4, sub=1, part="head")
    bl.tube("eye_sealed", [(0.5, 27.0, 3.3), (1.6, 26.9, 3.35), (2.5, 27.1, 3.15)], 0.16, dark, sides=4, sub=1, part="head")
    bl.tube("nose", [(0, 26.4, 3.1), (0, 25.8, 3.6), (0, 25.2, 3.3)], 0.28, skin, sides=4, sub=2, part="head")
    wr = arms(5.2, 22.6, 16.0, 10.0, 1.3, cloth, sleeve=cloth)
    hand("fine", "forearm_l", wr, skin, None, s=0.95, claw_len=0, rad=0.26, spread=0.55)
    lg = limb("leg_l", "leg_l", [(1.9, 14.5, 0), (2.1, 9.5, 0.6), (2.1, 5.0, 0.0), (2.1, 1.6, 0.2)], [2.1, 1.7, 1.4, 1.2], cloth, sides=8)
    twin(lg)
    boot = be.boulder("boot_l", "leg_l", (2.1, 1.0, 2.0), 2.3, dark, squash=(0.7, 0.45, 1.7), seg=(7, 4), jag=0.02)
    twin(boot)
    return std_rig(hip=14.5, sh=22.6, neck=24.2, sw=5.2, el=16.0, headz=0.3), ("3AE0E0", 0.16, 0.55, 1.0)


# ================================================================ 4. the Ghost Devil
def ghost_devil():
    """The Ghost Devil: a tall pale shrouded figure with a blank mask face; eight reaching arms rise from its back as its wings, a wisp for a tail."""
    be.seed(71)
    pale = M("gh_pale", (196, 206, 228), rough=0.95, bump="leather", bscale=0.7, bstr=0.2, groove=bl.rgb(120, 134, 170), gamount=0.45)
    shroud = M("gh_shroud", (160, 172, 204), rough=0.95, bump="wrap", bscale=0.4, bstr=0.3, groove=bl.rgb(84, 98, 140), gamount=0.6)
    hollow = M("gh_hollow", (14, 16, 28), rough=1.0, bevel=0.0)
    soul = Glow("gh_soul", (150, 200, 255), 10.0)
    ghostarm = M("gh_arm", (186, 198, 226), rough=0.9, bevel=0.05)
    # ---- torso: a gaunt chest under a shroud that flows to a ragged hem, a lantern of soul-light in the ribs
    torso("torso", [(11, 3.2, 2.2, 0, 0), (15, 2.6, 1.9, 0, 0), (19, 3.1, 2.2, 0, 0.1), (23, 4.2, 2.6, 0, 0.1), (25.5, 2.8, 2.0, 0, 0.2)], pale, n=10)
    bl.gem("soul_core", 1.2, soul, center=(0, 19.8, 2.0), depth=0.5, crown=0.45, sides=8, axis="z", rx=1.2, ry=1.7, part="torso")
    for k in range(3):
        bl.tube(f"rib{k}", [(-2.1, 21.8 - k * 1.8, 1.4), (0, 21.4 - k * 1.8, 2.5), (2.1, 21.8 - k * 1.8, 1.4)], 0.25, pale, sides=4, sub=2, part="torso")
    rows = [(14.2, 4.0, 3.0, 0, 0), (10.0, 5.2, 4.0, 0, -0.2), (6.0, 6.0, 4.8, 0, -0.5), (2.0, 6.4, 5.2, 0, -0.8)]
    sk = bl.loft("shroud", [ring_pts(*r, n=14, jit=0.18) for r in rows], shroud, (False, False), part="torso")
    bl.solidify(sk, 0.3)
    cape = bl.loft("veil", [[V((6.2 * math.cos(a) * (1 + 0.1 * i), 25.6 - i * 3.4, 4.8 * math.sin(a) * (1 + 0.1 * i) - 0.8)) for a in
                             [PI + 0.35 + (PI - 0.7) * k / 8 for k in range(9)]] for i in range(5)], shroud, (False, False), part="torso", closed=False)
    bl.solidify(cape, 0.3)
    for k in range(5):                                                       # tatters that curl like fingers off the hem
        x = -4.8 + k * 2.4
        be.shard(f"tatter{k}", "torso", (x, 4.0, 3.6 - abs(x) * 0.1), (x * 1.2, 0.8, 4.6 + (k % 2) * 1.2), 0.9, shroud, n=4)
    # ---- head: a blank long mask, two hollow eyes with a pinprick of soul-light, a thin glowing mouth, a hanging cowl
    torso("head", [(23.8, 1.8, 1.9, 0, 0.5), (25.6, 2.6, 3.0, 0, 0.6), (28.0, 2.8, 3.2, 0, 0.3), (30.4, 2.4, 2.8, 0, 0.0), (32.4, 1.4, 1.6, 0, -0.4)], pale, n=10)
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.9, hollow, center=(1.2 * sx, 27.8, 3.55), depth=0.5, crown=0.2, sides=6, axis="z", rx=0.8, ry=1.3, part="head")
        bl.gem(f"pupil_{sx}", 0.34, soul, center=(1.2 * sx, 27.4, 3.9), depth=0.4, crown=0.3, sides=4, axis="z", part="head")
    bl.tube("mouth", [(-1.5, 24.9, 3.0), (0, 24.5, 3.3), (1.5, 24.9, 3.0)], 0.14, soul, sides=4, sub=2, part="head")
    dome("cowl", "head", 23.4, 34.2, 3.8, 4.2, 0.5, 1.2, shroud, rows=7, n=11, thick=0.4, flare=0.1)
    # ---- the two arms: very long and thin with big hands; and the many hands (the wings)
    ua = limb("arm_l", "arm_l", [(4.8, 22.6, 0), (5.0, 17.5, 0.2), (5.4, 13.0, 0.8)], [1.3, 1.0, 0.85], pale, sides=7)
    twin(ua)
    fa = limb("forearm_l", "forearm_l", [(5.4, 13.0, 0.8), (5.6, 9.8, 1.3), (5.8, 7.0, 1.8)], [0.85, 0.8, 0.7], pale, sides=7)
    twin(fa)
    hand("gh", "forearm_l", V(5.8, 7.2, 1.8), pale, soul, s=1.1, claw_len=0.8, rad=0.24, spread=0.62)
    rays = [(7, 39, -4), (15, 34, -5), (19, 25, -5), (13, 15, -4)]
    for k, (ex, ey, ez) in enumerate(rays):
        path = [(1.8, 21.5, -2.2), (ex * 0.5 + 1.5, 21.5 + (ey - 21.5) * 0.5 + 2.0, -3.4 - k * 0.2), (ex, ey, ez)]
        a = limb(f"wing_arm{k}_l", "wing_l", path, [0.9, 0.7, 0.45], ghostarm, sides=5, sub=2, caps=(True, True))
        twin(a)
        dirv = (V(ex, ey, ez) - V(ex * 0.5 + 1.5, 21.5 + (ey - 21.5) * 0.5 + 2.0, -3.4 - k * 0.2)).normalized()
        hs = hand_at(f"wh{k}", "wing_l", V(ex, ey, ez), dirv, ghostarm, soul)
        for o in hs:
            twin(o)
    # ---- legs: thin, tapering to a point (the ghost does not quite stand), a trailing wisp for a tail
    lg = limb("leg_l", "leg_l", [(1.8, 14.5, 0), (1.9, 9.0, 0.3), (1.8, 4.0, 0.2), (1.7, 0.3, 0.3)], [1.8, 1.2, 0.8, 0.12], pale, sides=7, caps=(True, False))
    twin(lg)
    limb("tail", "tail", [(0, 11.0, -3.0), (0, 8.0, -8.0), (0, 4.2, -13.0), (0, 1.2, -18.5)], [1.7, 1.5, 1.0, 0.12], shroud, sides=7, sub=3, caps=(True, False), up=(1, 0, 0))
    for k in range(3):
        bl.gem(f"wisp{k}", 0.6, soul, center=(0.0, 9.0 - k * 3.2, -7.0 - k * 4.4), depth=0.5, crown=0.4, sides=6, axis="y", part="tail")
    return std_rig(hip=14.5, sh=22.6, neck=24.0, sw=4.8, el=13.0, headz=0.6, elx=0.6, wing=(1.8, 21.5, -2.2), tail=(0, 11.0, -3.0)), ("9AB8FF", 0.1, 0.5, 1.0)


def hand_at(tag, part, tip, direction, mat, glow, s=0.85):
    """A little open hand at the end of a reaching arm, fingers along `direction` (used by the Ghost Devil's many hands, left side)."""
    d = V(direction).normalized()
    side = V(0, 0, 1).cross(d)
    if side.length < 1e-6:
        side = V(1, 0, 0)
    side.normalize()
    nrm = d.cross(side).normalized()
    out = [be.boulder(f"palm_{tag}_l", part, tip + d * 0.7 * s, 1.0 * s, mat, squash=(1.0, 1.0, 1.0), seg=(5, 3), jag=0.03)]
    for k in range(3):
        o = (k - 1.0) * 0.78 * s
        p0, p1 = tip + d * 1.3 * s + side * o, tip + d * 3.9 * s + side * o * 1.5 + nrm * 0.4 * s
        out.append(bl.tube(f"fing_{tag}_l{k}", [tuple(p0), tuple(p1)], 0.24 * s, mat, sides=4, sub=1, part=part, taper=(0.26 * s, 0.1 * s)))
    return out


# ================================================================ 5. Sebastian Michaelis
def sebastian():
    """Black Butler's Sebastian Michaelis: a tall slim butler in a black tailcoat, white gloves with long black nails, swept black hair, glowing slit eyes, the contract mark (an inverted pentacle) on the back of his left hand, and a pair of black feather wings folded behind: the demon under the human look."""
    be.seed(83)
    coat = M("se_coat", (18, 18, 24), rough=0.75, bump="cross", bscale=1.2, bstr=0.15, groove=bl.rgb(8, 8, 12), gamount=0.4)
    vest = M("se_vest", (40, 36, 52), rough=0.6, bump="cross", bscale=1.4, bstr=0.2)
    shirt = M("se_shirt", (236, 236, 242), rough=0.7)
    glove = M("se_glove", (240, 240, 246), rough=0.7)
    skin = M("se_skin", (232, 218, 214), rough=0.55)
    hair = M("se_hair", (14, 14, 20), rough=0.3, bump="brushed", bscale=2.0, bstr=0.25)
    gold = M("se_gold", (196, 200, 212), metallic=1.0, rough=0.25)   # silver: his pocket watch, links and buttons
    silver = M("se_silver", (196, 200, 212), metallic=1.0, rough=0.2)
    nail = M("se_nail", (10, 10, 14), metallic=0.4, rough=0.25)
    feather = M("se_feather", (22, 20, 30), rough=0.4, bump="brushed_x", bscale=2.0, bstr=0.25, groove=bl.rgb(10, 8, 16), gamount=0.5)
    seal = Glow("se_seal", (156, 80, 255), 13.0)
    eye = Glow("se_eye", (226, 40, 70), 14.0)                              # crimson slit eyes (the demon eyes)
    # ---- torso: a long tailcoat, nipped waist, white shirt and tie, waistcoat, tails behind
    torso("torso", [(13, 3.5, 2.2, 0, 0), (16, 2.7, 1.9, 0, 0), (20, 3.5, 2.3, 0, 0.1), (24, 4.6, 2.6, 0, 0.1), (26, 2.5, 1.9, 0, 0.2)], coat, n=10)
    bl.plate("vest", [(-2.1, 14.6), (2.1, 14.6), (2.9, 21.0), (1.0, 24.2), (-1.0, 24.2), (-2.9, 21.0)], 0.0, 0.3, vest, part="torso")
    bl.move(bpy.data.objects["vest"], (0, 0, 2.2))
    bl.plate("shirt_v", [(-1.0, 24.6), (1.0, 24.6), (0, 20.2)], 0.0, 0.3, shirt, part="torso")
    bl.move(bpy.data.objects["shirt_v"], (0, 0, 2.3))
    bl.plate("tie", [(-0.5, 24.2), (0.5, 24.2), (0.4, 20.8), (0, 19.6), (-0.4, 20.8)], 0.0, 0.25, hair, part="torso")
    bl.move(bpy.data.objects["tie"], (0, 0, 2.55))
    for k in range(3):
        bl.gem(f"button{k}", 0.4, gold, center=(0, 18.6 - k * 1.5, 2.45), depth=0.5, crown=0.35, sides=6, axis="z", part="torso")
    bl.tube("chain", [(1.5, 18.8, 2.5), (0.2, 17.2, 2.75), (-1.2, 18.4, 2.55), (-2.2, 20.4, 2.45)], 0.13, gold, sides=4, sub=2, part="torso")
    bl.lathe("watch", [(0.7, 0), (0.7, 0.25), (0.001, 0.25)], gold, sides=10, part="torso", axis="z", origin=(1.5, 18.8, 2.5))
    for sx in (1, -1):
        bl.plate(f"lapel{sx}", [(0.8 * sx, 24.6), (3.8 * sx, 23.6), (2.6 * sx, 21.0), (1.0 * sx, 20.0)], 0.0, 0.28, coat, part="torso")
        bl.move(bpy.data.objects[f"lapel{sx}"], (0, 0, 2.4))
        tail = bl.loft(f"coattail{sx}", [ring_pts(y, w, d, 1.6 * sx, -1.6, n=8) for y, w, d in [(14.0, 1.9, 1.2), (10.0, 1.6, 0.7), (6.2, 1.2, 0.5), (3.6, 0.5, 0.3)]], coat, (True, True), part="torso")
        bl.subsurf(tail, 1)
    # ---- head: a narrow human head, pointed ears, swept black hair with long locks over the face, glowing slit eyes
    torso("head", [(24.4, 1.7, 1.8, 0, 0.3), (26.0, 2.5, 2.7, 0, 0.4), (28.2, 2.7, 2.9, 0, 0.1), (30.0, 2.2, 2.4, 0, -0.1)], skin, n=10)
    bl.tube("nose", [(0, 27.2, 3.1), (0, 26.6, 3.7), (0, 26.0, 3.4)], 0.3, skin, sides=4, sub=2, part="head")
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.7, eye, center=(1.15 * sx, 27.6, 3.05), depth=0.45, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.38, part="head")
        be.boulder(f"ear{sx}", "head", (2.75 * sx, 27.0, 0.2), 0.85, skin, squash=(0.45, 1.0, 0.8), seg=(5, 3), jag=0.02)
        bl.tube(f"brow{sx}", [(0.6 * sx, 28.5, 3.2), (1.7 * sx, 28.6, 3.0), (2.4 * sx, 28.2, 2.6)], 0.17, hair, sides=4, sub=1, part="head")
    torso("hair_cap", [(27.2, 2.8, 3.0, 0, -0.1), (29.2, 3.0, 3.1, 0, -0.2), (30.6, 2.3, 2.4, 0, -0.2), (31.2, 1.0, 1.1, 0, -0.2)], hair, n=10, part="head")
    for k, (x, ln) in enumerate([(-2.2, 3.0), (-1.0, 3.6), (0.4, 3.2), (1.4, 3.8), (2.4, 2.8)]):   # the fringe
        horn(f"fringe{k}", "head", [(x, 30.2, 2.4), (x * 1.1, 28.8, 3.2 + 0.1 * k), (x * 1.15, 30.2 - ln, 3.3)], 0.45, hair, sides=4, sub=2, tip=0.15)
    for sx in (1, -1):
        horn(f"side_lock{sx}", "head", [(2.8 * sx, 29.4, 1.0), (3.1 * sx, 26.4, 1.6), (2.9 * sx, 23.9, 1.8)], 0.5, hair, sides=4, sub=2, tip=0.15)
    # ---- arms: black sleeves, white cuffs with gold links, white gloves with long black nails; the seal on the back of the left hand
    wr = arms(5.2, 23.6, 16.5, 10.5, 1.3, coat, sleeve=coat, r_wr=1.15)
    for sx in (1, -1):
        part = "forearm_l" if sx > 0 else "forearm_r"
        limb(f"cuff_{sx}", part, [(5.95 * sx, 11.8, 0.7), (6.05 * sx, 10.8, 0.75), (6.15 * sx, 10.0, 0.8)], [1.35, 1.3, 1.25], shirt, sides=8)
        bl.gem(f"link_{sx}", 0.4, gold, center=(6.0 * sx, 11.2, 1.9), depth=0.5, crown=0.3, sides=6, axis="z", part=part)
    hand("glove", "forearm_l", wr, glove, nail, s=0.95, claw_len=1.8, rad=0.3, spread=0.58)
    # the contract mark: an inverted pentacle in a circle on the back of the LEFT hand (the back of a hanging hand faces outward, +x; source: ja.wikipedia 黒執事の登場人物)
    hc = V(7.5, 9.3, 0.9)
    bl.tube("seal_hand_ring", [tuple(hc + V(0.0, 1.05 * math.cos(2 * PI * k / 12), 1.05 * math.sin(2 * PI * k / 12))) for k in range(13)], 0.12, seal, sides=4, sub=1, part="forearm_l")
    bl.tube("seal_hand_star", [tuple(hc + V(0.02, -0.95 * math.cos(2 * PI * k * 2 / 5), 0.95 * math.sin(2 * PI * k * 2 / 5))) for k in range(6)], 0.1, seal, sides=4, sub=1, part="forearm_l")
    # ---- legs: black trousers, polished black shoes
    lg = limb("leg_l", "leg_l", [(1.9, 14.5, 0), (2.0, 9.5, 0.3), (2.0, 5.0, 0.1), (2.0, 1.6, 0.2)], [2.2, 1.8, 1.55, 1.45], coat, sides=8)
    twin(lg)
    sh = be.boulder("shoe_l", "leg_l", (2.0, 1.1, 2.0), 2.4, hair, squash=(0.7, 0.45, 1.75), seg=(7, 4), jag=0.02)
    twin(sh)
    # ---- the demon: black feather wings folded behind (two rows of feathers a side)
    for row, (ln0, base_y, z) in enumerate([(15.0, 24.0, -2.9), (11.0, 22.0, -3.4)]):
        for k in range(7):
            ln = ln0 * (1 - 0.07 * k) - row * 1.0
            f = bl.plate(f"feather{row}_{k}_l", [(0, 0), (1.5, -1.8), (1.6, -ln * 0.6), (0, -ln), (-1.6, -ln * 0.6), (-1.5, -1.8)], -0.14, 0.14, feather, part="wing_l")
            bl.move(f, (2.2 + k * 0.45, base_y - k * 0.5, z - k * 0.1), (-6 - row * 4, 0, -4 - k * 3.2))
            twin(f)
    return std_rig(hip=15.0, sh=23.6, neck=25.0, sw=5.2, el=16.5, headz=0.3, wing=(2.4, 23.0, -2.8)), ("B060FF", 0.12, 0.55, 1.0)


# ================================================================ 6. the Hall of Hell demon
def hall_demon():
    """A demon of the Hall of Hell (Dororo): the 48 demons are 48 wooden statues of fierce demons (carved by a Buddhist sculptor, in the Hall of Hell where Daigo prayed for power; source: ja.wikipedia "どろろ").
    So this is a carved wooden idol in the muscular fierce-guardian style: dark brown weathered wood with chisel grain, horns, a snarling fanged face, a loincloth, amber-lit eyes and mouth. No metal, no flames, no wings."""
    be.seed(97)
    wood = M("ha_wood", (138, 106, 58), rough=0.8, bump="grain", bscale=1.4, bstr=0.45, groove=bl.rgb(66, 46, 22), gamount=0.65)
    dark = M("ha_dark", (92, 66, 36), rough=0.85, bump="grain", bscale=1.4, bstr=0.45, groove=bl.rgb(44, 30, 14), gamount=0.65)
    pale = M("ha_pale", (176, 146, 96), rough=0.75, bump="grain", bscale=1.6, bstr=0.4, groove=bl.rgb(100, 76, 40), gamount=0.6)
    cloth = M("ha_cloth", (112, 62, 44), rough=0.9, bump="wrap", bscale=0.6, bstr=0.3, groove=bl.rgb(56, 28, 20), gamount=0.6)
    bone = M("ha_bone", (222, 210, 178), rough=0.6)
    amber = Glow("ha_amber", (255, 170, 56), 13.0)
    # ---- torso: a barrel chest, carved pecs and belly, big deltoids, a loincloth
    torso("torso", [(11, 4.6, 3.4, 0, 0), (14.5, 4.4, 3.8, 0, 0.5), (18, 4.8, 3.6, 0, 0.2), (22, 6.6, 3.6, 0, 0.2), (25, 3.6, 2.6, 0, 0.2)], wood, n=10)
    be.boulder("belly", "torso", (0, 14.6, 3.2), 4.0, dark, squash=(1.1, 0.9, 0.7), seg=(7, 5), jag=0.04)
    for sx in (1, -1):
        be.boulder(f"pec{sx}", "torso", (3.0 * sx, 21.0, 2.8), 2.9, dark, squash=(1.1, 0.8, 0.6), seg=(7, 5), jag=0.04)
        be.boulder(f"shoulder_pad{sx}", "torso", (6.5 * sx, 24.0, 0), 3.2, dark, squash=(1.1, 0.8, 1.0), seg=(7, 5), jag=0.05)
    bl.lathe("belt", [(5.6, 0), (5.4, 1.4), (5.0, 1.4), (5.0, 0)], cloth, sides=12, part="torso", origin=(0, 10.5, 0.4), squash=0.8)
    rows = [(10.2, 5.3, 4.0, 0, 0.3), (6.0, 5.5, 4.2, 0, 0.5), (3.2, 5.2, 3.8, 0, 0.5)]
    cl = bl.loft("loincloth", [ring_pts(*r, n=10, jit=0.05) for r in rows], cloth, (False, False), part="torso")
    bl.solidify(cl, 0.4)
    # ---- head: a broad fierce face under a heavy brow, a flat nose, a snarling jaw with tusks, long ear lobes, carved hair, two horns
    torso("head", [(23.6, 2.6, 2.6, 0, 0.5), (25.0, 3.4, 3.6, 0, 0.6), (27.4, 3.8, 3.8, 0, 0.3), (29.6, 3.4, 3.4, 0, 0.0), (30.6, 1.8, 2.0, 0, -0.2)], wood, n=10)
    be.boulder("brow", "head", (0, 28.0, 3.9), 3.2, dark, squash=(1.2, 0.35, 0.5), seg=(8, 5), jag=0.04)
    be.boulder("nose", "head", (0, 26.4, 4.5), 1.3, wood, squash=(1.2, 0.8, 0.9), seg=(6, 4), jag=0.03)
    be.boulder("jaw", "head", (0, 24.0, 3.0), 3.1, dark, squash=(1.2, 0.55, 0.85), seg=(8, 5), jag=0.04)
    bl.gem("maw", 1.6, amber, center=(0, 24.8, 4.3), depth=0.35, crown=0.2, sides=6, axis="z", rx=2.0, ry=0.8, part="head")
    for sx in (1, -1):
        bl.gem(f"eye_{sx}", 0.75, amber, center=(1.7 * sx, 27.0, 3.95), depth=0.45, crown=0.3, sides=6, axis="z", rx=1.0, ry=0.55, part="head")
        horn(f"tusk{sx}", "head", [(1.6 * sx, 24.4, 4.2), (2.2 * sx, 25.4, 4.9), (2.0 * sx, 27.0, 4.9)], 0.55, bone, sides=5, sub=2)
        horn(f"horn{sx}", "head", [(1.6 * sx, 30.0, 1.4), (3.6 * sx, 32.0, 1.6), (4.8 * sx, 34.8, 1.4), (4.0 * sx, 37.0, 1.6)], 0.95, pale, sides=6, sub=3, tip=0.12)
        be.boulder(f"ear{sx}", "head", (3.9 * sx, 25.4, 0.2), 1.3, wood, squash=(0.5, 1.9, 0.9), seg=(6, 4), jag=0.04)
    for k in range(7):                                                       # carved hair: curled locks swept back
        a = PI * (0.12 + 0.76 * k / 6)
        be.shard(f"hair{k}", "head", (math.cos(a) * 2.8, 28.6 + math.sin(a) * 2.6, -1.4), (math.cos(a) * 6.4, 29.4 + math.sin(a) * 5.6, -4.0), 0.8, dark, n=4)
    # ---- arms: thick, carved muscle, cloth wraps on the wrists, big fists with nails
    wr = arms(6.6, 23.0, 15.0, 8.4, 2.1, wood, sleeve=wood, r_el=1.9, r_wr=1.8)
    for sx in (1, -1):
        limb(f"wrap_{sx}", "forearm_l" if sx > 0 else "forearm_r", [(7.1 * sx, 11.4, 0.7), (7.3 * sx, 10.2, 0.9), (7.4 * sx, 9.0, 1.1)], [2.15, 2.2, 2.0], cloth, sides=8)
    hand("fist", "forearm_l", wr, dark, bone, s=1.35, claw_len=0.9, rad=0.42, spread=0.62)
    # ---- legs: short, thick, a wide stance, big feet with toenails
    lg = limb("leg_l", "leg_l", [(2.6, 14.0, 0), (3.2, 9.5, 1.0), (3.0, 5.0, 0.2), (3.0, 1.8, 0.2)], [3.3, 2.7, 2.3, 2.2], wood, sides=8)
    twin(lg)
    kn = be.boulder("knee_l", "leg_l", (3.2, 9.4, 2.7), 1.8, dark, squash=(1.0, 1.0, 0.7), seg=(6, 4), jag=0.05)
    twin(kn)
    foot("claw", "leg_l", (3.0, 0.0, 0.0), dark, l=5.0, w=2.6, h=1.6, toes=3, claw=bone, toe_len=1.8)
    return std_rig(hip=14.0, sh=23.0, neck=24.0, sw=6.6, el=15.0, hipx=2.6, headz=0.6, elx=0.6), ("FFAA38", 0.11, 0.55, 1.0)


def fit_skin(path, kb=300):
    """A baked skin is noisy, so its 256-colour palette PNG can still pass 300 KB: re-quantise to fewer colours (128, 96, 64, 48) until it fits. Run on a palette or an RGBA skin."""
    from PIL import Image
    im = Image.open(path).convert("RGBA")
    for n in (256, 128, 96, 64, 48):
        try:
            import imagequant
            q = imagequant.quantize_pil_image(im, dithering_level=0.0, max_colors=n)
        except ImportError:
            q = im.quantize(colors=n, method=Image.Quantize.FASTOCTREE, dither=Image.Dither.NONE)
        q.save(path, optimize=True)
        if os.path.getsize(path) <= kb * 1024:
            break
    return os.path.getsize(path)


def crop_shrink(path):
    """The preview is cropped to the figure (the studio background is flat) and quantised."""
    from PIL import Image, ImageChops
    im = Image.open(path).convert("RGB")
    bgc = im.getpixel((2, 2))
    box = ImageChops.difference(im, Image.new("RGB", im.size, bgc)).point(lambda v: 255 if v > 6 else 0).getbbox()
    if box:
        m = 30
        im = im.crop((max(box[0] - m, 0), max(box[1] - m, 0), min(box[2] + m, im.width), min(box[3] + m, im.height)))
    im.quantize(colors=200, method=Image.Quantize.MEDIANCUT).save(path, optimize=True)


def tight_has_glow(self, uv, thresh=2.0):
    """Atlas.has_glow looks at the whole UV box of a quad, so a thin quad beside a glowing island is flagged (hundreds of needless glow quads on a body made of many
    small pieces). This one samples the quad itself: its middle and five points a little inside its corners."""
    k = self.k
    c = uv.mean(axis=0)
    for p in [c] + [c + (q - c) * 0.6 for q in uv]:
        x, y = int(p[0] * k), int(p[1] * k)
        if 0 <= x < self.size and 0 <= y < self.size and self.glow[y, x, 3] > thresh:
            return True
    return False


BUILDERS = {"fox_devil": fox_devil, "curse_devil": curse_devil, "future_devil": future_devil, "ghost_devil": ghost_devil, "sebastian": sebastian, "hall_demon": hall_demon}


FIT = {"fox_devil": 0.92, "curse_devil": 0.93, "future_devil": 0.96, "ghost_devil": 0.92, "hall_demon": 0.93}   # whole-model fit to the player


def main(argv):
    trial = argv[argv.index("--trial") + 1] if "--trial" in argv else None
    quick = "--quick" in argv
    only = argv[argv.index("--only") + 1].split(",") if "--only" in argv else list(BUILDERS)
    pv = trial or PREVIEW_DIR
    orig = be.render_previews
    be.render_previews = lambda *a, **k: orig(*a, res=(900, 1100), samples=24, **k)   # a larger preview than the bosses'
    be._shrink = crop_shrink
    bl.ml.Atlas.has_glow = tight_has_glow
    for did in only:
        bl.reset(samples=8 if quick else 20)
        rig, glow = BUILDERS[did]()
        if FIT.get(did, 1.0) != 1.0:
            rig = scale_all(rig, FIT[did])
        order_parts()
        print(did, "faces before export:", count())
        be.finish_entity("devil_" + did, rig, glow=glow, skin=1024 if not quick else 512, bake=2048 if not quick else 512, lod_ratio=0.27,
                         out_mesh=trial, out_skin=trial, previews={os.path.join(pv, did + ".png"): (30, None), os.path.join(pv, did + "_back.png"): (205, None)}, poses={}, cage=0.2)
        fit_skin(os.path.join(trial or be.ENTITY_SKIN_DIR, f"devil_{did}.png"))


if __name__ == "__main__":
    main(sys.argv[1:])

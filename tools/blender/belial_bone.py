"""Belial's BONE DRAGON Djinn Equip: the part set `djinn/belial_bone/` (helm, horns, brow crest, ribcage chest, belt, pauldrons, vambraces, gauntlets, greaves,
bat wings, bone-spine tail, the bone dragon backdrop, the bat-wing mantle and the in-hand scythe), white bone with teal-blue glow, one atlas
`textures/entity/djinn/belial_bone_armor.png` + `_glow.png`. Made in Blender with forms_kit.py (like belial_scythe.py, whose scythe is the `weapon` piece at lod 1).

    python3 -B tools/blender/belial_bone.py                    # bake into the mod's asset folders
    python3 -B tools/blender/belial_bone.py --trial DIR        # write into DIR (djinn/belial_bone/, textures/entity/djinn/) instead
    python3 -B tools/blender/belial_bone.py --quick            # no bake: quad table per piece
    python3 -B tools/blender/belial_bone.py --only helm,crest  # build only some pieces (with --quick, to look at them)
    python3 -B tools/blender/belial_bone.py --preview DIR [--from TRIALDIR]   # software preview (the game's rasteriser) of the written pieces on a mannequin

Frames: every piece is AUTHORED y up, +z the front, in its own local frame (the frame the old sets were authored in, tools/relics/djinn_parts.py ANCHOR:
helm/horns/crest origin = the neck centre, the head spans y 0..8; chest/belt/mantle origin = the top of the torso, the torso spans y 0 .. -12; arm pieces
origin = the shoulder joint, hand at y -10; greaves origin = the hip joint, foot at y -12; wings origin = the shoulder blade, +x outward; tail along -z; the
backdrop origin 2 above the neck, 5.5 behind the torso). The files are written the way the layer reads them: (x, y, z) -> (x, -y, -z) (model space, y down,
-z the front); the WEAPON stays in item space (grip at the origin, blade along +y, the head toward +x). UV space 128 on one 1024 px atlas.
Each piece is built away from the others in the scene (a grid of offsets that is taken off again on export), so the bake of one piece never sees another.
"""
import math
import os
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bpy  # noqa: E402
import numpy as np  # noqa: E402
from mathutils import Matrix  # noqa: E402
import forms_kit as K  # noqa: E402
import belial_scythe as BS  # noqa: E402
from forms_kit import V, TAU, rgb  # noqa: E402

bl = K.bl
SET = "belial_bone"
PIECES = ["helm", "horn_l", "horn_r", "crest", "chest", "belt", "pauldron_l", "pauldron_r", "vambrace_l", "vambrace_r", "gauntlet_l", "gauntlet_r", "greave_l", "greave_r",
          "wing_in_l", "wing_in_r", "wing_out_l", "wing_out_r", "tail_seg", "tail_tip", "backdrop", "mantle", "weapon", "arm2_l", "arm2_r"]      # arm2_* appended last (the canon four-arm look); the first 23 keep their order and offsets
OFF = {}                                    # piece -> grid offset in the scene (taken off again on export)


# ================================================================ materials
def palette():
    P = BS.palette(1.0)
    S = 1.0
    P["leather"] = BS.surf(S, "bb_leather", rgb(9, 5, 18), 0.0, 0.5, bump="leather", bump_scale=0.6, bump_strength=0.5, mottle=0.6, mottle_color=rgb(34, 18, 56), wear=0.35,
                           wear_color=rgb(92, 66, 138), wear_metal=0.0, dirt=0.6, dirt_color=(0.006, 0.003, 0.012, 1), scratches=0.3, fine=0.5, seed=61, bevel=0.05)
    P["under"] = BS.surf(S, "bb_under", rgb(12, 38, 44), 0.0, 0.7, bump="cross", bump_scale=1.4, bump_strength=0.5, groove=rgb(4, 14, 18), groove_amount=0.8, mottle=0.8,
                         mottle_color=rgb(24, 70, 78), veins=2.4, vein_color=rgb(70, 232, 244), vein_strength=0.6, wear=0.4, wear_color=rgb(110, 170, 180), wear_metal=0.0, seed=62, bevel=0.0)
    P["hair"] = BS.surf(S, "bb_hair", rgb(232, 234, 242), 0.15, 0.38, bump="brushed", bump_scale=2.2, bump_strength=0.45, mottle=0.7, mottle_color=rgb(176, 184, 204), wear=0.7,
                        wear_color=rgb(255, 255, 255), wear_metal=0.0, dirt=0.4, dirt_color=(0.04, 0.045, 0.06, 1), fine=0.4, seed=63, bevel=0.0)
    return P


# ================================================================ scene bookkeeping
class Piece:
    """with Piece("helm"): ... builds the piece's objects; on exit they are renamed helm__<part> and moved out to the piece's own grid cell."""
    def __init__(self, name):
        self.name = name

    def __enter__(self):
        self.before = {o.name for o in bpy.context.scene.collection.objects}
        return self

    def __exit__(self, *a):
        k = len(OFF)
        OFF[self.name] = (k * 400.0 % 4000.0, -(k // 10) * 300.0, 0.0)
        M = Matrix.Translation(OFF[self.name])
        for o in bpy.context.scene.collection.objects:
            if o.name in self.before or o.type != "MESH":
                continue
            o["part"] = "%s__%s" % (self.name, o.get("part", o.name))
            K.xf(o, M)
        return False


def section_hex(rw, hw):
    return [(-rw, -hw * 0.6), (-rw * 0.2, -hw), (rw, -hw * 0.7), (rw, hw * 0.7), (-rw * 0.2, hw), (-rw, hw * 0.6)]


def ring_h(name, c, rx, rz, section, n, mat, part, rot=0.0):
    """A horizontal ring (closed solid) round the vertical axis through c: ellipse rx x rz, section(theta) -> [(radial offset, dy)]."""
    verts, faces = [], []
    m = len(section(0.0))
    for i in range(n):
        th = rot + TAU * i / n
        ca, sa = math.cos(th), math.sin(th)
        for (ro, dy) in section(th):
            verts.append((c[0] + (rx + ro) * ca, c[1] + dy, c[2] + (rz + ro) * sa))
    for i in range(n):
        j = (i + 1) % n
        for k in range(m):
            k2 = (k + 1) % m
            faces.append((i * m + k, j * m + k, j * m + k2, i * m + k2))
    return K.mk(name, verts, faces, mat, part, False, True, 55.0)


def shell(name, stack, mat, part, sides=10, a0=0.0, a1=TAU, closed=True):
    """An open lathe-like sheet through a stack of (y, rx, rz, cx, cz) ellipses (a bracer, a greave, a cuff), turned outward. a0..a1 limits the angle (a half shell)."""
    rings = []
    n = sides if (closed and abs(a1 - a0 - TAU) < 1e-6) else sides + 1
    for (y, rx, rz, cx, cz) in stack:
        rings.append([V((cx + rx * math.cos(a0 + (a1 - a0) * i / sides), y, cz + rz * math.sin(a0 + (a1 - a0) * i / sides))) for i in range(n if not closed else sides)])
    ob = K._loft(name, rings, mat, (False, False), part, False, True, 55.0)
    # make the faces point away from the axis: compare face 0's normal with the outward radial
    me = ob.data
    pl = me.polygons[0]
    ctr = pl.center
    ref = stack[len(stack) // 2]
    radial = V((ctr.x - ref[3], 0.0, ctr.z - ref[4]))
    if pl.normal.dot(radial) < 0:
        me.flip_normals()
        me.update()
    return ob


def T(name, pts, r0, r1, mat, part, sides=5, sub=2, bulge=None):
    return K.tapered(name, [V(p) for p in pts], r0, r1, mat, sides=sides, sub=sub, part=part, bulge=bulge)


def blobs(name, c, radii, mat, part, seed=0, rings=4, segs=6, lump=0.08):
    return K.blob(name, c, radii, mat, seed=seed, lump=lump, rings=rings, segs=segs, part=part)


def fin(name, base, direction, length, width, mat, part, rr, facing=(1, 0, 0), rows=5, curl=0.0, bend=(0, 0, 0), thick=0.22):
    return K.feather(name, base, direction, length, width, mat, bend=bend, curl=curl, rows=rows, serrate=0.0, facing=facing, thick=thick, part=part, seed=rr.randint(0, 999),
                     profile=lambda t: (math.sin(math.pi * min(1.0, t ** 0.7)) ** 0.8) * (1.0 - 0.15 * t), ridge=1.5)


# ================================================================ the pieces
def build_helm(P, rr):
    # circlet round the brow, a chamfered band, uneven width
    ring_h("circlet", (0, 6.5, 0), 4.5, 4.6, lambda th: section_hex(0.55 + 0.12 * math.sin(th * 3), 0.95 + 0.12 * math.cos(th * 5)), 12, P["bone"], "circlet")
    # the frill: bone fins fanned up and back from behind the crown, different lengths, one cracked short
    for i, (x, ln, lean) in enumerate(((-3.4, 6.5, -0.7), (-1.5, 9.8, -0.35), (0.0, 12.0, 0.0), (3.5, 5.8, 0.75))):
        d = V((lean * 0.55, 0.85, -0.5 - 0.2 * abs(lean)))
        fin("frill%d" % i, V((x, 7.2, -3.9)), d, ln * (0.65 if i == 3 else 1.0), 2.9 - 0.4 * abs(lean), P["bone"], "frill", rr, facing=(1.0, 0.0, 0.25 * lean), rows=3, bend=(0.5 * lean, 0, -1.4))
    # temple plates: overlapping scales over each ear, a small teal stone in the top one
    for sgn, seed_ in ((1, 3), (-1, 5)):
        for k in range(2 if sgn > 0 else 1):
            K.scale_plate("temple%d_%d" % (sgn, k), (4.65 * sgn, 6.4 - 1.5 * k + 0.2 * seed_ * 0.1, 0.8 - 1.0 * k), (sgn, 0.0, 0.2 * k), 1.35 - 0.18 * k, P["bone"], up=(0, 1, 0),
                          part="temple", rows=2, tilt=18, shape="point")
        blobs("temple_gem%d" % sgn, (4.9 * sgn, 6.5, 0.9), (0.4, 0.5, 0.35), P["gem"], "helm_glow", seed=sgn, rings=3, segs=4, lump=0.0)
    # a median ridge of small thorns over the skull
    K.spike_array("crown_thorn", [V((0.0, 8.35 - 0.35 * abs(i - 2), 2.6 - 1.7 * i)) for i in range(3)], [V((0, 1.0, -0.35 * (i - 1.0))) for i in range(3)], [2.0, 3.2, 2.1], 0.45, P["bone"], rr,
                  bend=0.2, sides=3, part="crown_thorns", len_jit=0.15)


def build_helm_hair(P, rr):
    """Long white-silver hair swept back from the brow over the crown and down past the nape (five locks, tapering), and a few gold studs on the circlet (bake-only relief)."""
    for i, (x, ln) in enumerate(((-3.2, 0.9), (-1.0, 1.1), (1.2, 1.15), (3.3, 0.85))):
        lean = x / 3.3
        pts = [(x * 0.9, 7.9, 3.7), (x * 1.0, 8.9, 0.6), (x * 1.2, 8.5, -3.6), (x * 1.35 + lean * 1.0, 6.2 + (1 - ln) * 3, -6.6 * ln - 0.6), (x * 1.45 + lean * 2.4, 3.0 + (1 - ln) * 4, -8.4 * ln - 0.6)]
        T("hair%d" % i, pts, 0.52, 0.06, P["hair"], "hair", 3, 1)
    gold = []
    for a in (-1.0, -0.5, 0.0, 0.5, 1.0):
        ca, sa = math.cos(a), math.sin(a)
        gold.append((V((4.5 * sa * 1.12, 6.0 + 0.05 * abs(a), 5.2 * ca * 0.97)), (sa, 0.0, ca)))
    K.rivets("circlet_gold", gold, P["gold"], r=0.42, part="circlet_gold", detail=True, sides=6)


def build_helm_eyes(P, rr):
    """The canon Belial's two extra eyes: small teal slit eyes in dark sockets on the front of the circlet, above the brow (helm frame, +z the front)."""
    for sgn in (1, -1):
        blobs("eye3_socket%d" % sgn, (1.45 * sgn, 7.0, 4.85), (0.5, 0.42, 0.28), P["pupil"], "helm_dark", seed=sgn, rings=3, segs=5, lump=0.0)
        blobs("eye3_%d" % sgn, (1.45 * sgn, 7.0, 5.02), (0.15, 0.36, 0.16), P["gem"], "helm_glow", seed=sgn, rings=3, segs=5, lump=0.0)


def build_crest(P, rr):
    pts = [(-4.5, 5.6, 1.4), (-3.3, 6.8, 3.4), (-1.7, 7.4, 4.3), (0.0, 7.6, 4.6), (1.8, 7.3, 4.3), (3.2, 6.8, 3.5), (4.5, 5.7, 1.6)]
    K.tube("brow", pts, 0.5, P["bone"], sides=4, sub=1, part="brow", taper=None, bumps=0.2, seed=4)
    # brow fang + two lesser spikes + the teal brow stone in a claw setting
    T("brow_fang", [(0.0, 7.6, 4.6), (0.0, 8.8, 5.3), (0.1, 10.0, 5.4)], 0.65, 0.08, P["bone"], "brow_horn", 5, 2)
    T("brow_spike_l", [(2.4, 7.0, 4.0), (3.1, 8.0, 4.4), (3.6, 9.1, 4.0)], 0.4, 0.06, P["bone"], "brow_horn", 4, 1)
    T("brow_spike_r", [(-2.4, 7.0, 4.0), (-2.9, 7.8, 4.4), (-3.1, 8.6, 4.1)], 0.4, 0.06, P["bone"], "brow_horn", 4, 1)
    K.gem_setting("brow_stone", (0.0, 6.2, 4.75), 0.62, P["gem"], P["trim"], normal=(0, 0.1, 1), claws=3, sides=5, part="brow_gem", rng_=rr, claw_r=0.5, bezel=False)


def build_horn(P, rr, side):
    sgn = 1 if side == "l" else -1
    broken = side == "r"
    pts = [(4.3, 5.8, -0.2), (6.6, 7.4, -0.9), (9.1, 9.9, -2.8), (10.8, 12.2, -5.8), (10.9, 13.9, -9.4)]
    if broken:
        pts = [(4.2, 5.9, 0.2), (6.4, 7.2, -0.5), (8.5, 9.0, -2.2), (9.8, 10.8, -4.4), (10.2, 11.4, -6.0)]
    pts = [(p[0] * sgn, p[1], p[2]) for p in pts]
    bulge = lambda t: 1.0 + 0.16 * math.sin(t * TAU * 5.0) + 0.0     # noqa: E731  (growth rings)
    T("horn", pts[:4] if not broken else pts, 1.3, 0.55 if not broken else 0.4, P["bone"], "horn", 6, 3, bulge)
    if not broken:
        T("horn_tip", pts[3:], 0.58, 0.07, P["gem"], "horn_glow", 5, 2)
    T("horn_spur", [(pts[0][0] * 0.98, pts[0][1] + 0.5, pts[0][2] + 0.3), (pts[0][0] * 0.94 + sgn * 1.3, pts[0][1] + 2.4, pts[0][2] + 1.3), (pts[0][0] + sgn * 2.6, pts[0][1] + 3.8, pts[0][2] + 1.8)],
      0.6, 0.06, P["bone"], "horn_spur", 4, 1)
    K.rivets("horn_ring", [(V((pts[1][0], pts[1][1], pts[1][2])), (0, 1, 0))], P["trim"], r=0.8, part="horn_band", detail=True, sides=6)


def build_chest(P, rr):
    # linen undershirt over a dark teal lining that glows in veins between the ribs
    sh = [(1.6, 4.2, 2.45, 0, 0.0), (0.0, 4.6, 2.7, 0, 0.0), (-4.0, 4.5, 2.6, 0, 0.0), (-8.0, 4.4, 2.5, 0, 0.0), (-10.0, 4.5, 2.5, 0, 0.0)]
    shell("under", sh, P["under"], "under", sides=10)
    # collar: two clavicle bars, a standing bone ruff behind the neck
    for sgn, wob in ((1, 0.2), (-1, -0.3)):
        T("clavicle%d" % sgn, [(1.3 * sgn, 0.2, 2.5), (2.9 * sgn, 0.9 + wob, 2.9), (4.5 * sgn, 0.2, 2.2)], 0.65, 0.5, P["bone"], "collar", 4, 1)
    for i, (x, ln) in enumerate(((-2.4, 4.4), (0.2, 6.2), (2.5, 4.9))):
        fin("ruff%d" % i, V((x, 0.5, -2.4)), V((x * 0.12, 0.8, -0.55)), ln, 2.2, P["bone"], "ruff", rr, facing=(1, 0, 0.2), rows=3, bend=(0, 0, -0.8))
    # the ribcage: a sternum bar and five pairs of ribs wrapping the front, uneven, the lowest pair floating
    K.bar("sternum", V((0.0, 0.3, 3.2)), V((0.0, -7.6, 3.1)), 0.5, 0.45, P["bone_smooth"], part="sternum", taper=0.8, bevel_w=0.0, up=(0, 0, 1))
    for i in range(4):
        y = -0.9 - 1.9 * i + rr.uniform(-0.12, 0.12)
        for sgn in (1, -1):
            w = 3.9 - 0.18 * i + (0.25 * rr.uniform(-1, 1))
            pts = [(0.3 * sgn, y, 3.15), (1.9 * sgn, y + 0.25, 3.0), (3.4 * sgn, y + 0.55, 2.0), (w * sgn, y + 0.85 - 0.35 * i * 0.2, 0.4)]
            if i == 3:
                pts = pts[:3]
            K.tube("rib%d_%d" % (i, sgn), pts, 0.5, P["bone"], sides=3, sub=1, part="ribs", taper=(0.6, 0.3), bumps=0.12, seed=i * 3 + (sgn > 0))
    # the back: spine knuckles with a thorn each, and two shoulder-blade plates
    K.tube("spine_ridge", [(0.0, -0.2, -2.8), (0.15, -3.0, -2.95), (-0.1, -6.0, -2.85), (0.0, -9.0, -2.8)], 0.5, P["bone"], sides=4, sub=2, part="spine", taper=(0.7, 0.5), bumps=0.3, seed=2)
    for i in range(3):
        y = -1.4 - 2.8 * i
        T("vert_thorn%d" % i, [(0, y, -3.1), (0, y + 0.3, -3.9), (0, y + 0.9, -4.6)], 0.4, 0.05, P["bone"], "spine_thorns", 3, 1)
    for sgn in (1, -1):
        K.scale_plate("blade_plate%d" % sgn, (2.4 * sgn, -1.0, -2.9), (0.15 * sgn, 0.0, -1.0), 2.0, P["bone"], up=(0, 1, 0), part="back_plates", rows=2, tilt=14, shape="point")
    # the bead-chain necklace: a swag of teal and bone beads on a cord, a dragon-eye pendant
    sw = [(3.7, 0.6, 2.7), (2.4, -1.2, 3.55), (0.9, -2.5, 3.9), (0.0, -2.9, 4.0), (-1.1, -2.6, 3.9), (-2.6, -1.1, 3.5), (-3.8, 0.7, 2.6)]
    K.tube("necklace_cord", sw, 0.14, P["cord"], sides=3, sub=2, part="necklace_cord")
    smp = bl.smooth_path([V(p) for p in sw], 3)
    for b in range(7):
        q = smp[1 + int((len(smp) - 3) * b / 6.0)]
        K.blob("nbead%d" % b, tuple(q), (0.34, 0.34, 0.34), P["gem"] if b % 2 == 0 else P["bone_smooth"], seed=b, lump=0.0, rings=3, segs=4, part="necklace_glow" if b % 2 == 0 else "necklace")
    K.blob("pendant", (0.0, -3.9, 4.15), (0.55, 0.85, 0.4), P["gem"], seed=2, lump=0.04, rings=3, segs=6, part="pendant_glow")
    K.seam("sternum_glow", [(0.0, 0.0 - 0.8 * i) for i in range(10)], P["teal"], w=0.16, depth=0.05, z=3.7, side=1, part="sternum_glow", detail=True)


def build_belt(P, rr):
    ring_h("waistband", (0, -10.2, 0), 4.5, 2.65, lambda th: section_hex(0.5, 1.2 + 0.1 * math.sin(th * 4)), 12, P["linen"], "waistband")
    ring_h("bone_belt", (0, -9.6, 0), 4.7, 2.85, lambda th: section_hex(0.35, 0.34), 10, P["bone_smooth"], "bone_belt", rot=0.2)
    # a small dragon-skull buckle at the front, tassels hanging from the waist (linen and teal beads), a bone sacrum plate at the back
    blobs("buckle", (0.0, -9.9, 3.05), (1.1, 1.2, 0.8), P["bone"], "buckle", seed=3, rings=3, segs=6, lump=0.12)
    T("buckle_horn_l", [(0.7, -9.5, 3.2), (1.3, -8.8, 3.6), (1.5, -8.2, 3.4)], 0.3, 0.04, P["bone"], "buckle", 4, 1)
    T("buckle_horn_r", [(-0.7, -9.5, 3.2), (-1.2, -8.9, 3.6), (-1.2, -8.4, 3.5)], 0.3, 0.04, P["bone"], "buckle", 4, 1)
    blobs("buckle_eye_l", (0.45, -9.8, 3.7), (0.22, 0.2, 0.15), P["gem"], "buckle_glow", rings=3, segs=4, lump=0.0)
    blobs("buckle_eye_r", (-0.45, -9.8, 3.7), (0.22, 0.2, 0.15), P["gem"], "buckle_glow", rings=3, segs=4, lump=0.0)
    for i, (x, z, ln, ph) in enumerate(((0.0, 2.9, 7.2, 0.0), (2.9, 2.2, 6.0, 1.0), (-2.7, 2.4, 6.6, 2.0), (-1.9, -2.8, 6.4, 6.0))[:3]):
        path = [V((x + 0.35 * k * math.copysign(1, x or 1) * 0.3 + 0.4 * math.sin(k * 0.6 + ph), -10.8 - k * ln / 4.0, z + 0.3 * math.sin(k * 0.8 + ph) + (0.2 * k if z > 0 else -0.2 * k) * 0.2)) for k in range(5)]
        K.ribbon("hang%d" % i, path, lambda t: 2.4 * (1.0 - 0.35 * t) + 0.25, P["linen"], facing=(math.sin(math.atan2(x, 1) + 1.57) * 0 + 0.0, 0.0, 1.0 if z > 0 else -1.0) if abs(x) < 3 else (1.0 if x > 0 else -1.0, 0, 0),
                 thick=0.07, part="hang", fray=0.8, rng_=rr, edge_wave=0.1, notch=1.0 if i % 3 == 0 else 0.0)
    blobs("tail_plate", (0.0, -10.4, -3.0), (1.7, 1.5, 0.7), P["bone"], "sacrum", seed=6, rings=3, segs=6, lump=0.1)
    for i, (x, y) in enumerate(((2.2, -12.3), (-2.0, -12.6))):
        K.blob("bead_end%d" % i, (x * 1.0, y - 5.4 - 0.5 * i, 2.7 if i < 2 else -2.9), (0.38, 0.38, 0.38), P["gem"], seed=i, lump=0.0, rings=3, segs=4, part="belt_glow")


def build_pauldron(P, rr, side):
    sgn = 1 if side == "l" else -1
    seed = 11 if side == "l" else 23
    # three stacked lames over the shoulder, each a stretched scale plate pitched down the arm; the lowest hangs a little, the uppermost carries the spike
    for k, (dy, sz, tilt) in enumerate(((1.4, 3.7, 16), (-0.6, 3.6, 20))):
        for m in range(3):
            z = (m - 1) * 2.7 + rr.uniform(-0.2, 0.2)
            K.scale_plate("lame%d_%d" % (k, m), (sgn * (1.2 + 1.1 * k + 0.6 * abs(m - 1) * 0.3), dy + 1.0 + 0.4 * (m == 1), z), (sgn * 0.75, 0.9, 0.1 * (m - 1)), sz * (1.0 - 0.14 * abs(m - 1)), P["bone"],
                          up=(sgn * 0.6, -0.7, 0), part="lames", rows=2, tilt=tilt, shape="point" if m == 1 else "round")
    # spike: tall and curved on the left, snapped short on the right
    L = 7.4 if side == "l" else 4.6
    T("spike", [(sgn * 0.8, 2.6, 0.4), (sgn * 1.8, 4.6, 0.8), (sgn * 3.6, 7.0 * L / 7.4, 0.5), (sgn * 5.4, 9.2 * L / 7.4, -0.4)], 1.45, 0.12 if side == "l" else 0.45, P["bone"], "spike", 5, 1,
      lambda t: 1.0 + 0.12 * math.sin(t * TAU * 3.0))
    # dragon vertebrae grafted onto the upper arm: two bone knuckles with a swept spine each, down the outside of the arm
    for g, (gy, gr) in enumerate(((-1.4, 1.0), (-3.4, 0.85))):
        blobs("graft%d" % g, (sgn * 3.2, gy, -0.6), (gr, gr * 0.85, gr), P["bone"], "graft", seed=seed + g, rings=3, segs=4, lump=0.12)
        T("graft_spine%d" % g, [(sgn * 3.3, gy + 0.5, -1.2), (sgn * 3.9, gy + 1.3, -2.2), (sgn * 4.6, gy + 1.8, -3.4)], 0.42, 0.05, P["bone"], "graft", 3, 1)
    K.blob("pauldron_stone", (sgn * 3.1, 2.1, 2.9), (0.6, 0.45, 0.5), P["gem"], seed=seed, lump=0.0, rings=3, segs=5, part="pauldron_glow")
    K.rivets("lame_rivets", [(V((sgn * (2.4 + 0.9 * j), 2.5 - 1.2 * j, 3.15)), (0, 0.3, 1)) for j in range(3)], P["trim"], r=0.75, part="lame_rivets", detail=True, sides=6, rng_=rr, jitter=0.2)
    # a linen strap holding the lames: a loop under the arm
    K.ribbon("strap", [V((sgn * (3.2 + 0.2 * k), 0.2 - 1.1 * k, 2.55)) for k in range(5)], 1.1, P["linen"], facing=(0, 0, 1), thick=0.08, part="strap", fray=0.5, rng_=rr, edge_wave=0.1)


def build_vambrace(P, rr, side):
    sgn = 1 if side == "l" else -1
    cx = 1.0 * sgn
    # linen wrap at the wrist and two bone bracer plates (half shells that open to the inside of the arm)
    shell("bracer_a", [(-2.2, 2.3, 2.3, cx, 0), (-2.4, 2.65, 2.65, cx, 0), (-3.6, 2.8, 2.8, cx, 0), (-4.9, 2.6, 2.6, cx, 0), (-5.1, 2.3, 2.3, cx, 0)], P["bone"], "bracer", sides=8,
          a0=-1.9, a1=1.9, closed=False)
    shell("bracer_b", [(-5.6, 2.2, 2.2, cx, 0), (-5.8, 2.55, 2.55, cx, 0), (-7.2, 2.7, 2.7, cx, 0), (-8.4, 2.35, 2.35, cx, 0), (-8.6, 2.1, 2.1, cx, 0)], P["bone"], "bracer", sides=10,
          a0=-1.8, a1=1.8, closed=False)
    shell("wrist_wrap", [(-8.7, 2.05, 2.05, cx, 0), (-8.9, 2.2, 2.2, cx, 0), (-9.3, 2.2, 2.2, cx, 0), (-9.5, 2.05, 2.05, cx, 0)], P["linen"], "wrap", sides=8)
    # the elbow: a thorn, and an outward fin (a bone dragon fin along the outside of the forearm)
    T("elbow_thorn", [(cx + sgn * 1.9, -3.8, -0.3), (cx + sgn * 3.0, -3.3, -1.4), (cx + sgn * 4.3, -2.2, -2.2)], 0.65, 0.06, P["bone"], "fin", 4, 2)
    fin("forearm_fin", V((cx + sgn * 2.4, -4.4, 0.0)), V((sgn * 0.55, -0.82, 0.0)), 5.6 if side == "l" else 4.4, 2.0, P["bone"], "fin", rr, facing=(0, 0, 1), rows=4)
    K.seam("bracer_glow", [(cx + sgn * 2.55, -3.0 - 0.45 * i) for i in range(10)], P["teal"], w=0.14, depth=0.05, z=0.9, side=1, part="bracer_glow", detail=True)
    K.rivets("bracer_rivets", [(V((cx + sgn * (1.8 - 0.3 * j % 1), -2.6 - 1.5 * j, 2.62)), (0, 0, 1)) for j in range(4)], P["trim"], r=0.6, part="bracer_rivets", detail=True, sides=6, rng_=rr, jitter=0.2)


def build_gauntlet(P, rr, side):
    sgn = 1 if side == "l" else -1
    cx = 1.0 * sgn
    shell("cuff", [(-9.1, 2.2, 2.2, cx, 0), (-9.3, 2.5, 2.5, cx, 0), (-10.4, 2.4, 2.4, cx, 0), (-10.6, 2.15, 2.15, cx, 0)], P["bone"], "cuff", sides=8)
    # back-of-hand plate with a knuckle ridge, four claws and a thumb claw, curved and uneven
    K.scale_plate("hand_plate", (cx, -10.6, 2.2), (0, 0.15, 1.0), 2.1, P["bone"], up=(0, -1, 0), part="hand_plate", rows=3, tilt=10, shape="round")
    for i in range(4):
        z = -1.35 + 0.9 * i
        ln = 4.0 - 0.45 * abs(i - 1.2) + rr.uniform(-0.2, 0.3)
        if side == "r" and i == 3:
            ln *= 0.55
        T("finger%d" % i, [(cx + 0.25 * (i - 1.5), -10.7, 1.5 - 0.9 * i * 0.0 + (z)), (cx + 0.3 * (i - 1.5), -11.8, z * 1.0 + 0.5), (cx + 0.2 * (i - 1.5), -10.7 - ln, z * 1.0 + 1.1 - 0.0)], 0.5, 0.05, P["bone"], "claw", 3, 1)
    T("thumb", [(cx - sgn * 1.6, -10.3, 0.6), (cx - sgn * 2.5, -11.4, 1.0), (cx - sgn * 3.0, -12.6, 1.6)], 0.5, 0.05, P["bone"], "claw", 3, 1)
    K.seam("glove_glow", [(cx + 0.2 * math.sin(i), -9.6 - 0.35 * i) for i in range(5)], P["teal"], w=0.15, depth=0.05, z=2.5, side=1, part="cuff_glow", detail=True)


def build_arm2(P, rr, side):
    """The canon Belial's second pair of arms: a thin white bone arm with a slim vambrace and a clawed gauntlet in one piece (authored like a vambrace + gauntlet pair: origin = the
    shoulder joint of the extra arm, the hand at y -12, +z the front, +x the wearer's left for `l`). The client layer has no anchor for it yet."""
    sgn = 1 if side == "l" else -1
    cx = 0.5 * sgn
    T("arm_bone", [(cx, -0.4, 0.0), (cx + 0.2 * sgn, -2.6, 0.3), (cx + 0.5 * sgn, -4.8, 0.1)], 0.75, 0.55, P["bone"], "arm", 4, 1, lambda t: 1.0 + 0.1 * math.sin(t * TAU * 2.0))
    blobs("elbow", (cx + 0.5 * sgn, -5.0, 0.1), (0.95, 0.9, 0.95), P["bone"], "arm", seed=sgn + 4, rings=3, segs=5, lump=0.1)
    shell("bracer", [(-5.4, 1.45, 1.45, cx + 0.6 * sgn, 0), (-5.6, 1.75, 1.75, cx + 0.6 * sgn, 0), (-7.4, 1.65, 1.65, cx + 0.7 * sgn, 0), (-8.6, 1.4, 1.4, cx + 0.8 * sgn, 0)], P["bone"], "bracer",
          sides=6, a0=-1.9, a1=1.9, closed=False)
    shell("wrist_wrap", [(-8.7, 1.3, 1.3, cx + 0.8 * sgn, 0), (-9.0, 1.45, 1.45, cx + 0.8 * sgn, 0), (-9.6, 1.35, 1.35, cx + 0.8 * sgn, 0)], P["linen"], "wrap", sides=6)
    T("elbow_thorn", [(cx + 1.2 * sgn, -4.4, -0.2), (cx + 2.4 * sgn, -4.0, -0.9), (cx + 3.4 * sgn, -3.0, -1.4)], 0.5, 0.05, P["bone"], "fin", 3, 1)
    K.scale_plate("hand_plate", (cx + 0.8 * sgn, -9.7, 1.3), (0, 0.15, 1.0), 1.35, P["bone"], up=(0, -1, 0), part="hand_plate", rows=2, tilt=10, shape="round")
    for i in range(3):
        z = -0.7 + 0.7 * i
        ln = 3.0 - 0.4 * abs(i - 1) + rr.uniform(-0.2, 0.2)
        T("finger%d" % i, [(cx + 0.8 * sgn + 0.2 * (i - 1), -9.9, z * 0.6 + 0.8), (cx + 0.8 * sgn + 0.25 * (i - 1), -11.0, z + 0.4), (cx + 0.8 * sgn + 0.15 * (i - 1), -9.9 - ln, z + 1.0)], 0.38, 0.05, P["bone"], "claw", 3, 1)
    T("thumb", [(cx - 0.4 * sgn, -9.7, 0.4), (cx - 1.2 * sgn, -10.6, 0.8), (cx - 1.6 * sgn, -11.6, 1.3)], 0.36, 0.05, P["bone"], "claw", 3, 1)
    K.seam("arm2_glow", [(cx + 0.8 * sgn + 1.7 * sgn, -6.0 - 0.4 * i) for i in range(6)], P["teal"], w=0.12, depth=0.05, z=0.5, side=1, part="bracer_glow", detail=True)


def build_greave(P, rr, side):
    sgn = 1 if side == "l" else -1
    seed = 31 if side == "l" else 41
    # shin guard: a half shell round the front of the leg, a ridge down its middle, a knee cap with a thorn, a calf plate behind, a linen boot cuff
    shell("shin", [(-1.3, 2.25, 2.25, 0, 0), (-1.5, 2.55, 2.6, 0, 0), (-4.0, 2.65, 2.7, 0, 0), (-8.2, 2.35, 2.4, 0, 0), (-9.8, 2.2, 2.2, 0, 0), (-10.0, 2.0, 2.0, 0, 0)], P["bone"], "shin", sides=8,
          a0=0.55, a1=TAU / 2 - 0.55, closed=False)
    T("shin_ridge", [(0, -2.0, 2.85), (0.2 * sgn, -5.0, 2.95), (0, -9.5, 2.5)], 0.45, 0.3, P["bone_smooth"], "shin_ridge", 4, 2)
    blobs("knee", (0.0, -1.0, 2.4), (2.4, 1.5, 1.6), P["bone"], "knee", seed=seed, rings=3, segs=6, lump=0.1)
    T("knee_thorn", [(0.0, -0.8, 3.4), (0.0, -0.2, 4.4), (0.0, 0.9, 5.3)], 0.95, 0.08, P["bone"], "knee_thorn", 3, 2)
    K.scale_plate("calf", (0.0, -3.4, -2.6), (0, 0, -1), 2.3, P["bone"], up=(0, 1, 0), part="calf", rows=3, tilt=12, shape="round")
    shell("boot_cuff", [(-9.6, 2.35, 2.4, 0, 0), (-9.9, 2.7, 2.75, 0, 0), (-11.1, 2.6, 2.6, 0, 0), (-11.4, 2.3, 2.3, 0, 0)], P["linen"], "cuff", sides=8)
    # an outer fin (blade-like) on the outside of the shin, shorter on the right; teal slit
    fin("shin_fin", V((sgn * 2.3, -3.0, 0.2)), V((sgn * 0.6, -0.78, 0.15)), 6.8 if side == "l" else 5.0, 2.1, P["bone"], "fin", rr, facing=(0, 0, 1), rows=4)
    K.seam("shin_glow", [(0.0 + 0.1 * math.sin(i), -3.0 - 0.6 * i) for i in range(10)], P["teal"], w=0.16, depth=0.05, z=3.05, side=1, part="shin_glow", detail=True)


def build_wing_in(P, rr, side):
    sgn = 1 if side == "l" else -1
    seed = 3 if side == "l" else 8
    K.bone("humerus", V((0, 0, 0)), V((sgn * 11.0, 0.5, 0)), 0.95, P["bone"], knob=1.5, sub=2, sides=4, part="humerus", bow=V((0, 0.9, 0.3)), seed=seed)
    T("elbow_spur", [(sgn * 10.4, 0.8, 0.0), (sgn * 11.5, 2.3, 0.4), (sgn * 12.4, 4.3, 0.2)], 0.7, 0.06, P["bone"], "spur", 4, 2)
    # the inner membrane: hangs between the humerus and the body, scalloped, black leather with a lit hem
    rows = []
    for j in range(10):
        t = j / 9.0
        x = sgn * (0.8 + 9.6 * t)
        drop = 2.2 + 4.2 * math.sin(math.pi * min(t * 1.0, 1.0)) ** 0.8 - 1.4 * abs(math.sin(t * 9.0 + seed)) * (0.4 + t)
        rows.append((V((x, -0.4, 0.0)), V((x * 0.98, -0.4 - max(drop, 0.5), 0.0))))
    K.strip("membrane_in", rows, P["leather"], (-1.0, 0.0, 1.0), (0.3, 1.0, 0.0), 0.12, part="membrane", up=(0, 0, 1), wobble=lambda q, t: (0, 0, 0.5 * math.sin(t * 5 + seed) * 0.5))
    K.seam("hem_in", [(sgn * (0.8 + 9.6 * j / 9.0), -0.4 - max(2.2 + 4.2 * math.sin(math.pi * j / 9.0) ** 0.8, 0.5) * 0.97) for j in range(1, 9)], P["teal"], w=0.14, depth=0.04, z=0.1, side=1, part="hem_glow",
           detail=True)


def build_wing_out(P, rr, side):
    sgn = 1 if side == "l" else -1
    W = V((sgn * 10.0, 0.6, 0.0))
    tips = [V((sgn * 23.6, -1.4, 0)), V((sgn * 21.4, -9.6, 0)), V((sgn * 15.4, -16.6, 0)), V((sgn * 8.2, -21.6, 0))]
    if side == "r":
        tips = [V((sgn * 22.0, -1.0, 0)), V((sgn * 22.2, -8.8, 0)), V((sgn * 16.8, -17.4, 0)), V((sgn * 8.6, -21.0, 0))]
    elbow = V((0.0, -5.5, 0.0))
    K.bone("forearm", V((0, 0, 0)), W, 0.9, P["bone"], knob=1.5, sub=2, sides=4, part="forearm", bow=V((0, 0.5, 0.2)), seed=4)
    K.blob("wrist", tuple(W), (1.35, 1.35, 1.35), P["bone"], seed=5, lump=0.12, rings=3, segs=6, part="wrist")
    T("thumb_claw", [tuple(W + V((0, 0.6, 0))), tuple(W + V((sgn * 1.5, 3.2, 0.2))), tuple(W + V((sgn * 3.8, 5.4, 0.5))), tuple(W + V((sgn * 5.6, 5.0, 0.4)))], 1.0, 0.07, P["bone"], "thumb", 4, 1)
    for i, tp in enumerate(tips):
        mid = (W + tp) * 0.5 + V((sgn * 0.8, -1.8 - 0.4 * i, 0))
        K.tube("finger%d" % i, [tuple(W), tuple(mid), tuple(tp)], 0.5, P["bone"], sides=4, sub=2, part="fingers", taper=(0.8, 0.14), bumps=0.2, seed=i + 7)
        K.blob("knuckle%d" % i, tuple(mid), (0.55, 0.55, 0.55), P["bone"], seed=i, lump=0.1, rings=3, segs=4, part="fingers")
    # the membrane: a fan from the wrist over a scalloped, torn trailing edge, billowing a little
    edge = []
    seg_pts = tips + [elbow]
    for i in range(len(seg_pts) - 1):
        a, b = seg_pts[i], seg_pts[i + 1]
        m = (a + b) * 0.5
        inward = (W - m).normalized()
        depth = (3.4 + 1.2 * rr.random()) if i < 3 else 2.2
        for u in range(0, 6 if i < 3 else 4):
            f = u / (5.0 if i < 3 else 3.0)
            q = a.lerp(b, f) + inward * depth * (1.0 if u % 2 == 0 else 0.4) * math.sin(math.pi * f) ** 0.8            # jagged: every other point of the trailing edge is a deep notch
            if side == "l" and i == 2 and 0.5 < f < 0.62:                         # a tear
                q = q + inward * 2.4
            edge.append(q)
    rows = [(W, q) for q in edge]
    K.strip("membrane_out", rows, P["leather"], (-1.0, 0.0, 1.0), (0.3, 1.0, 0.0), 0.13, part="membrane", up=(0, 0, 1),
            wobble=lambda q, t: (0, 0, 0.7 * math.sin(t * 8.0 + (2 if side == "l" else 5)) * min(1.0, (q - W).length / 8.0)))
    K.seam("hem_out", [(q.x, q.y) for q in edge[1::2]], P["teal"], w=0.15, depth=0.04, z=0.12, side=1, part="hem_glow", detail=True)
    K.seam("hem_out_b", [(q.x, q.y) for q in edge[1::2]], P["teal"], w=0.15, depth=0.04, z=0.12, side=-1, part="hem_glow", detail=True)


def build_tail_seg(P, rr):
    v = bl.lathe("vertebra", [(0.55, 0.0), (1.1, -0.8), (1.3, -1.8), (0.95, -3.0), (1.25, -4.2), (1.15, -5.0), (0.6, -5.8)], P["bone"], sides=5, part="vertebra", axis="z")
    K._finish_obj(v, "vertebra", True, 55.0)
    T("dorsal", [(0.0, 1.0, -2.0), (0.0, 2.3, -2.6), (0.0, 3.5, -3.7)], 0.7, 0.07, P["bone"], "spines", 3, 1)
    T("flank_l", [(1.0, 0.1, -3.0), (2.0, 0.2, -3.5), (2.9, -0.1, -4.4)], 0.5, 0.05, P["bone"], "spines", 3, 1)
    T("flank_r", [(-1.0, 0.1, -3.0), (-1.9, 0.2, -3.6), (-2.6, -0.2, -4.2)], 0.5, 0.05, P["bone"], "spines", 3, 1)
    bl_ = bl.lathe("joint_glow", [(1.3, -4.6), (1.45, -4.95), (1.2, -5.35)], P["teal"], sides=5, part="joint_glow", axis="z")
    K._finish_obj(bl_, "joint_glow", True, 55.0)


def build_tail_tip(P, rr):
    # a bone blade (a stinger) along -z, two barbs and a teal stone at its root
    rows = [(V((-0.0 - 1.9 * math.sin(math.pi * min(t * 1.0, 1.0)) ** 0.7 + 0.0, 0.0, -t * 12.0)), V((1.9 * math.sin(math.pi * min(t, 1.0)) ** 0.7 + 0.0, 0.0, -t * 12.0))) for t in [i / 6.0 for i in range(7)]]
    K.strip("stinger", rows, P["bone"], (-1.0, -0.2, 0.2, 1.0), (0.0, 1.0, 1.0, 0.0), lambda t: 0.5 * (1.0 - 0.7 * t), part="stinger", up=(0, 1, 0))
    T("barb_l", [(1.2, 0.0, -3.8), (2.6, 0.4, -4.8), (3.5, 0.2, -6.4)], 0.4, 0.05, P["bone"], "barbs", 4, 1)
    T("barb_r", [(-1.2, 0.0, -4.4), (-2.4, -0.3, -5.5), (-3.0, -0.2, -7.1)], 0.4, 0.05, P["bone"], "barbs", 4, 1)
    K.blob("tip_stone", (0.0, 0.2, -1.2), (0.8, 0.8, 0.9), P["gem"], seed=2, lump=0.0, rings=3, segs=6, part="tip_glow")


def build_mantle(P, rr):
    # a bat-wing cape: a leather sheet from the shoulders, a scalloped hem of five points, bone ribs from the collar to every point, a teal-lit hem
    top = [V((x, 0.4 - 0.25 * abs(x) * 0.12, -2.9 - 0.18 * abs(x))) for x in np.linspace(-7.0, 7.0, 13)]
    hem = []
    for i, p in enumerate(top):
        x = p.x
        u = (x + 7.0) / 14.0 * 5.0                                          # five scallops
        sc = abs(math.sin(u * math.pi))
        y = -23.5 - 5.0 * (1.0 - sc) ** 1.1 + 1.5 * math.sin(x * 0.7)
        hem.append(V((x * 1.45, y, -4.2 - 2.4 * math.sin(math.pi * (i / 12.0)) - 0.12 * abs(x))))
    rows = list(zip(top, hem))
    K.strip("cape", rows, P["leather"], (-1.0, -0.3, 0.3, 1.0), (0.0, 1.0, 1.0, 0.0), 0.13, part="cape", up=(0, 0, 1), wobble=lambda q, t: (0, 0, 0.5 * math.sin(t * 17.0 + q.y * 0.4)))
    for i in range(0, 13, 4):
        K.tube("cape_rib%d" % i, [tuple(top[i] + V((0, 0, -0.1))), tuple((top[i] + hem[i]) * 0.5 + V((0, 0, -0.3))), tuple(hem[i] + V((0, 0.6, -0.1)))], 0.3, P["bone"], sides=3, sub=2, part="cape_ribs", taper=(0.45, 0.14))
    K.seam("cape_hem", [(h.x, h.y + 0.5) for h in hem], P["teal"], w=0.16, depth=0.04, z=-4.5, side=-1, part="cape_glow", detail=True)
    # a ruff of thorns on the collar
    K.spike_array("cape_thorn", [V((x, 1.0, -2.9)) for x in (-4.2, -1.4, 1.4, 4.2)], [V((x * 0.1, 0.9, -0.45)) for x in (-4.2, -1.4, 1.4, 4.2)], [3.6, 5.2, 4.9, 3.2], 0.5,
                  P["bone"], rr, bend=0.2, sides=3, part="cape_collar", len_jit=0.15)


# ---- the bone dragon's skull stations (local frame: origin = cranium centre, +z the snout, +y up, +x the dragon's left): z, half width, top, bottom, nasal ridge, brow swell
SKULL_ST = [(-3.5, 1.2, 1.1, 1.0, 0.0, 0.0), (-2.7, 2.5, 2.3, 1.7, 0.1, 0.0), (-1.2, 3.1, 2.8, 1.9, 0.35, 0.0), (0.4, 3.2, 2.7, 1.9, 0.3, 0.1), (1.9, 3.3, 2.4, 1.7, 0.1, 0.45),
            (3.2, 2.7, 1.8, 1.4, 0.2, 0.2), (4.7, 2.4, 1.5, 1.15, 0.3, 0.0), (6.4, 2.0, 1.3, 1.0, 0.35, 0.0), (8.1, 1.7, 1.15, 0.9, 0.3, 0.0), (9.7, 1.65, 1.2, 0.85, 0.15, 0.0),
            (10.7, 1.2, 0.9, 0.7, 0.0, 0.0), (11.1, 0.5, 0.4, 0.35, 0.0, 0.0)]


def skull_at(z, k):
    """Interpolated station value k (1 half width, 2 top, 3 bottom) at z."""
    zs = [s[0] for s in SKULL_ST]
    for i in range(len(zs) - 1):
        if zs[i] <= z <= zs[i + 1]:
            f = (z - zs[i]) / (zs[i + 1] - zs[i])
            return SKULL_ST[i][k] * (1 - f) + SKULL_ST[i + 1][k] * f
    return SKULL_ST[-1][k]


def skull_ring(st, n=12):
    z, W, top, bot, ridge, brow = st
    pts = []
    for i in range(n):
        a = TAU * i / n
        ca, sa = math.cos(a), math.sin(a)
        y = sa * (top if sa > 0 else bot * 0.85)
        x = W * ca
        if sa > 0:
            y += ridge * max(0.0, sa - 0.5) * 2.0 + brow * max(0.0, sa - 0.2) * abs(ca) * 1.4         # the nasal / sagittal ridge on top, the swollen brow over the orbits
            x *= 1.0 + 0.05 * brow
        pts.append(V((x, y, z)))
    return pts


def sweep_st(name, stations, section, mat, part, up=(0, 1, 0), caps=(True, True)):
    """stations: [(pos, su, sv)] used as given (no smoothing): a flat blade-like limb (a jaw ramus)."""
    ob = bl.sweep(name, [V(s[0]) for s in stations], section, mat, scale=[(s[1], s[2]) for s in stations], up=up, part=part, caps=caps)
    return K._finish_obj(ob, part, True, 55.0)


def build_skull(P, rr, c):
    C = lambda x, y, z: V((c.x + x, c.y + y, c.z + z))                       # noqa: E731
    Cp = lambda p: C(*p)                                                      # noqa: E731
    # ---- cranium + snout: one skinned loft, so the cheeks, the brow and the muzzle flow into each other
    rings = [[C(p.x, p.y, p.z) for p in skull_ring(st)] for st in SKULL_ST]
    sk = K._loft("skull_main", rings, P["bone"], (True, True), "skull", False, True, 60.0)
    K.displace(sk, 0.05, 0.45, seed=21, octaves=2)
    for sg in (1, -1):
        # brow arch over the orbit, cheekbone (jugal) arch under it, the orbit between them is the eye socket
        K.tube("brow_arch%d" % sg, [Cp((sg * a, b, d)) for a, b, d in ((1.2, 1.55, 4.7), (2.1, 2.1, 3.7), (3.0, 1.95, 2.5), (3.4, 1.4, 1.2))], 0.4, P["bone"], sides=4, sub=1, part="skull", bumps=0.2, seed=5 + sg)
        K.tube("jugal%d" % sg, [Cp((sg * a, b, d)) for a, b, d in ((2.3, -0.3, 4.6), (3.0, -0.7, 3.3), (3.6, -0.6, 1.9), (3.5, -1.0, 0.2))], 0.34, P["bone"], sides=4, sub=1, part="skull", taper=(0.9, 0.5))
        T("cheek_horn%d" % sg, [Cp((sg * a, b, d)) for a, b, d in ((3.4, -0.7, 1.4), (5.0, -1.1, 0.7), (6.4, -2.3, -0.9))], 0.62, 0.05, P["bone"], "skull_horns", 3, 1)
        T("brow_horn%d" % sg, [Cp((sg * a, b, d)) for a, b, d in ((2.3, 2.0, 3.4), (3.2, 3.2, 3.0), (3.8, 4.1, 2.0))], 0.5, 0.05, P["bone"], "skull_horns", 3, 1)
        K.blob("socket%d" % sg, tuple(Cp((sg * 2.2, 0.75, 3.0))), (0.55, 0.85, 0.9), P["pupil"], seed=sg, lump=0.0, rings=3, segs=5, part="skull_dark")
        K.blob("eye%d" % sg, tuple(Cp((sg * 2.62, 0.75, 3.1))), (0.3, 0.45, 0.36), P["gem"], seed=sg, lump=0.0, rings=3, segs=5, part="skull_glow")
        K.blob("nostril%d" % sg, tuple(Cp((sg * 0.5, 0.0, 10.95))), (0.3, 0.3, 0.2), P["pupil"], seed=sg, lump=0.0, rings=3, segs=4, part="skull_dark")
    # sagittal crest: three small spines over the cranium, the nasal horn on the bridge
    for i, (z, ln) in enumerate(((-1.2, 2.1), (0.5, 1.7))):
        y0 = skull_at(z, 2) + 0.35
        T("crest_spine%d" % i, [Cp((0, y0, z)), Cp((0, y0 + ln * 0.55, z - 0.5)), Cp((0, y0 + ln, z - 1.2))], 0.38, 0.05, P["bone"], "skull_horns", 3, 1)
    T("nasal_horn", [Cp((0, 1.55, 6.3)), Cp((0, 2.3, 6.5)), Cp((0, 2.9, 6.2))], 0.4, 0.05, P["bone"], "skull_horns", 3, 1)
    # ---- the two big horns, swept back and up with growth rings, one snapped
    hl = [(2.4, 2.2, -0.8), (4.2, 3.9, -2.3), (5.1, 6.0, -4.7), (4.3, 7.7, -7.8), (2.6, 8.2, -10.3)]
    T("skull_horn_l", [Cp(p) for p in hl], 1.05, 0.07, P["bone"], "skull_horns", 4, 2, lambda t: 1.0 + 0.13 * math.sin(t * TAU * 2.0))
    T("skull_horn_r", [Cp((-p[0], p[1] * 0.95, p[2])) for p in hl[:4]], 1.0, 0.42, P["bone"], "skull_horns", 5, 1, lambda t: 1.0 + 0.13 * math.sin(t * TAU * 2.0))
    # ---- lower jaw: hinged at the quadrate below the cranium, hung open about 24 degrees; two rami meeting at the chin
    hy, hz, ang = -0.9, -0.7, math.radians(24.0)
    ca, sa = math.cos(ang), math.sin(ang)

    def J(x, y, z):                                                           # a point of the closed jaw -> the opened jaw (turn about the hinge axis, chin down)
        dy, dz = y - hy, z - hz
        return Cp((x, hy + dy * ca - dz * sa, hz + dy * sa + dz * ca))
    sec = [(0.55, 0.0), (0.3, 0.85), (-0.3, 0.85), (-0.55, 0.0), (-0.3, -0.55), (0.3, -0.55)]
    for sg in (1, -1):
        st = [(J(sg * 2.55, -0.9, -0.7), 1.3, 1.7), (J(sg * 2.45, -1.7, 1.3), 1.4, 1.2), (J(sg * 2.0, -2.0, 3.8), 1.25, 1.0), (J(sg * 1.35, -2.0, 6.3), 1.1, 0.9), (J(sg * 0.8, -1.95, 8.2), 1.0, 0.8),
              (J(sg * 0.35, -1.9, 9.4), 0.85, 0.7)]
        sweep_st("ramus%d" % sg, st, sec, P["bone"], "skull_jaw", up=(0, 1, 0))
        K.blob("hinge%d" % sg, tuple(J(sg * 2.7, hy, hz)), (0.55, 0.6, 0.6), P["bone_smooth"], seed=sg, lump=0.0, rings=3, segs=4, part="skull_jaw")
        T("coronoid%d" % sg, [J(sg * 2.5, -0.7, -0.9), J(sg * 2.4, 0.5, -1.2), J(sg * 2.1, 1.5, -1.0)], 0.5, 0.06, P["bone"], "skull_jaw", 3, 1)
    K.blob("chin", tuple(J(0, -1.95, 9.4)), (1.0, 0.6, 0.8), P["bone"], seed=4, lump=0.05, rings=3, segs=5, part="skull_jaw")
    # ---- teeth: a fang and a short row on each side, upper and lower; the lower row turns with the jaw
    for sg in (1, -1):
        zf = 5.6
        T("fang_u%d" % sg, [Cp((sg * skull_at(zf, 1) * 0.8, -skull_at(zf, 3) * 0.45, zf)), Cp((sg * skull_at(zf, 1) * 0.78, -skull_at(zf, 3) * 0.45 - 1.4, zf + 0.15)),
                            Cp((sg * skull_at(zf, 1) * 0.74, -skull_at(zf, 3) * 0.45 - 2.7, zf + 0.5))], 0.44, 0.04, P["bone_smooth"], "skull_teeth", 3, 1)
        for i, zt in enumerate((4.2, 7.3, 9.0)):
            wd = skull_at(zt, 1) * 0.78
            yb = -skull_at(zt, 3) * 0.45
            T("tooth_u%d_%d" % (sg, i), [Cp((sg * wd, yb, zt)), Cp((sg * wd * 0.96, yb - 0.95 - 0.25 * (i % 2), zt + 0.1))], 0.26, 0.04, P["bone_smooth"], "skull_teeth", 3, 1)
        T("fang_l%d" % sg, [J(sg * 1.1, -1.7, 6.9), J(sg * 1.1, -0.5, 7.0), J(sg * 1.05, 0.8, 7.2)], 0.4, 0.04, P["bone_smooth"], "skull_teeth", 3, 1)
        for i, (zt, xx) in enumerate(((4.0, 1.9), (8.3, 0.7))):
            T("tooth_l%d_%d" % (sg, i), [J(sg * xx, -1.7, zt), J(sg * xx, -0.8, zt + 0.1)], 0.24, 0.04, P["bone_smooth"], "skull_teeth", 3, 1)
    K.blob("throat_glow", tuple(J(0, -1.5, 3.2)), (1.1, 0.5, 1.5), P["gem"], seed=3, lump=0.0, rings=3, segs=6, part="skull_glow")


def build_backdrop(P, rr):
    """The bone dragon: a spine of real vertebrae (a spinous process on each, deep grooves between them) rising behind the wearer from a curled tail, curved ribs along it, two
    clawed forelimbs reaching forward past the wearer's sides as if to restrain, and a horned skull above the head (brow ridges, cheekbones, eye sockets, a nasal ridge, a hinged
    lower jaw hung open, teeth), teal in the eye sockets and the throat. Origin 2 above the neck, 5.5 behind the torso; +z is the front."""
    ctrl = [(-8.5, -21.0, -4.0), (-10.5, -13.0, -9.0), (-8.0, -5.0, -12.0), (-2.0, 2.5, -11.0), (3.8, 9.0, -8.0), (5.8, 14.5, -3.0), (4.6, 16.6, 1.5)]
    path = [V(p) for p in bl.smooth_path([V(p) for p in ctrl], 8)]
    Ls = [0.0]
    for i in range(1, len(path)):
        Ls.append(Ls[-1] + (path[i] - path[i - 1]).length)

    def at(s):
        d = s * Ls[-1]
        for i in range(1, len(path)):
            if Ls[i] >= d:
                f = (d - Ls[i - 1]) / max(Ls[i] - Ls[i - 1], 1e-9)
                return path[i - 1].lerp(path[i], f), (path[i] - path[i - 1]).normalized()
        return path[-1], (path[-1] - path[-2]).normalized()
    nv = 11
    base = lambda t: (0.85 + 0.95 * math.sin(math.pi * min(t * 1.12, 1.0)) ** 0.7) * (1.0 - 0.5 * (1 - t) ** 3)      # noqa: E731
    pts, scs = [], []
    for k in range(3 * nv + 1):
        t = k / (3.0 * nv)
        p, _ = at(t)
        r = (0.55, 1.0, 1.0)[k % 3] if k < 3 * nv else 0.55
        pts.append(p)
        scs.append((base(t) * r * 1.05, base(t) * r * 0.92))
    sp = bl.sweep("spine", pts, bl.circle(1.0, 5, None, 0.3), P["bone"], scale=scs, up=(0, 0, 1), part="spine")
    K._finish_obj(sp, "spine", True, 62.0)
    K.displace(sp, 0.05, 0.4, seed=6, octaves=2)
    # spinous processes: a swept-back blade on every vertebra, taller toward the shoulders; the ribs hang from the middle ones
    k = 0
    for i in range(nv):
        t = (3 * i + 1.5) / (3.0 * nv)
        p, tn = at(t)
        up = V((0, 1, 0)) - tn * tn.y
        up = up.normalized() if up.length > 0.1 else V((0, 1, 0))
        ln = 1.4 + 3.4 * math.sin(math.pi * t) ** 1.2
        b = p + up * base(t) * 0.8
        T("neural%d" % i, [tuple(b), tuple(b + up * ln - tn * 0.8)], 0.5, 0.05, P["bone"], "neural", 3, 1)
        if 0.3 < t < 0.9 and k < 4:
            sd = tn.cross(V((0, 0, 1)))
            sd = sd.normalized() if sd.length > 0.1 else V((1, 0, 0))
            L = 4.2 + 6.6 * math.sin(math.pi * t) ** 1.3
            for sg in (1, -1):
                q = p + sd * (sg * base(t) * 0.7)
                K.tube("rib%d_%d" % (k, sg), [tuple(q), tuple(q + sd * (sg * L * 0.7) + V((0, 0.3, 0.0))), tuple(q + sd * (sg * L * 0.92) + V((0, -L * 0.45, 1.0))),
                                              tuple(q + sd * (sg * L * 0.5) + V((0, -L * 0.92, 2.4)))], 0.4, P["bone"], sides=3, sub=1,
                       part="ribs", taper=(0.62, 0.12), bumps=0.12, seed=k + (sg > 0) * 7)
            k += 1
    # two clawed forelimbs: shoulder blade, arm bones that reach forward past the wearer's sides, claws curled in toward the wearer's front (they restrain)
    for sg, lim in ((1, 1.0), (-1, 0.82)):
        arm = [(sg * 5.0, 8.0, -7.0), (sg * 9.0, 8.6, -2.5), (sg * 12.4, 6.6, 3.4), (sg * 12.6, 3.6, 9.6)]
        K.tube("arm%d" % sg, arm, 0.7 * lim, P["bone"], sides=4, sub=1, part="foreclaw", taper=(1.0 * lim, 0.65 * lim), bumps=0.14, seed=3 + sg)
        wr = arm[-1]
        K.blob("wrist_k%d" % sg, wr, (1.15 * lim, 1.05 * lim, 1.15 * lim), P["bone"], seed=8, lump=0.14, rings=3, segs=4, part="foreclaw")
        for i in range(3):
            a = (i - 1.0) * 0.55
            T("claw%d_%d" % (sg, i), [wr, (wr[0] - sg * (0.7 + 0.5 * i), wr[1] - 1.0 - 0.3 * abs(a), wr[2] + 1.5), (wr[0] - sg * (2.6 + 0.8 * i), wr[1] - 1.8 + 0.4 * a, wr[2] + 3.0)],
              0.4 * lim, 0.05, P["bone"], "foreclaw", 3, 1)
    build_skull(P, rr, V((4.6, 17.8, 4.6)))
    K.blob("tail_tip_k", tuple(path[0]), (1.0, 1.0, 1.0), P["gem"], seed=2, lump=0.0, rings=3, segs=5, part="skull_glow")


# ================================================================ driver
def build_helm_all(P, rr):
    build_helm(P, rr)
    build_helm_hair(P, rr)
    build_helm_eyes(P, rr)


BUILDERS = {
    "helm": build_helm_all, "crest": build_crest,
    "horn_l": lambda P, rr: build_horn(P, rr, "l"), "horn_r": lambda P, rr: build_horn(P, rr, "r"),
    "chest": build_chest, "belt": build_belt,
    "pauldron_l": lambda P, rr: build_pauldron(P, rr, "l"), "pauldron_r": lambda P, rr: build_pauldron(P, rr, "r"),
    "vambrace_l": lambda P, rr: build_vambrace(P, rr, "l"), "vambrace_r": lambda P, rr: build_vambrace(P, rr, "r"),
    "gauntlet_l": lambda P, rr: build_gauntlet(P, rr, "l"), "gauntlet_r": lambda P, rr: build_gauntlet(P, rr, "r"),
    "greave_l": lambda P, rr: build_greave(P, rr, "l"), "greave_r": lambda P, rr: build_greave(P, rr, "r"),
    "wing_in_l": lambda P, rr: build_wing_in(P, rr, "l"), "wing_in_r": lambda P, rr: build_wing_in(P, rr, "r"),
    "wing_out_l": lambda P, rr: build_wing_out(P, rr, "l"), "wing_out_r": lambda P, rr: build_wing_out(P, rr, "r"),
    "tail_seg": build_tail_seg, "tail_tip": build_tail_tip, "backdrop": build_backdrop, "mantle": build_mantle,
    "arm2_l": lambda P, rr: build_arm2(P, rr, "l"), "arm2_r": lambda P, rr: build_arm2(P, rr, "r"),
}


def build_all(only=None, weapon=True):
    P = palette()
    rr = K.rng(20260)
    for name in PIECES:
        if only and name not in only:
            continue
        if name == "weapon":
            if not weapon:
                continue
            with Piece("weapon") as pc:
                BS.build(lod=1, S=0.22, seed=1088)
                BS.transform_scene(0.22, True, 60.0, [o for o in bpy.context.scene.collection.objects if o.type == "MESH" and o.name not in pc.before])
            continue
        with Piece(name):
            BUILDERS[name](P, K.rng(abs(hash(name)) % 100000 if False else sum(map(ord, name)) * 17 + 3))
    return K.quad_estimate()


def piece_table():
    t = {}
    for o in bpy.context.scene.collection.objects:
        if o.type == "MESH" and not o.get("detail"):
            k = o.get("part", o.name).split("__", 1)[0]
            for p in o.data.polygons:
                n = len(p.vertices)
                t[k] = t.get(k, 0) + (1 if n <= 4 else (n - 2 + 1) // 2)
    return t


def export_pieces(mesh_txt, out_dir):
    """Splits the joined mesh file into one djinn piece file per piece: subtract the grid offset, flip to model space (the weapon stays item space)."""
    lines = [ln.rstrip("\n") for ln in open(mesh_txt)]
    quads = [ln for ln in lines if ln.startswith("qn ")]
    runs = []
    for ln in lines:
        if ln.startswith("# part "):
            _, _, nm, first, cnt = ln.split()
            runs.append((nm, int(first), int(cnt)))
    os.makedirs(out_dir, exist_ok=True)
    for f in os.listdir(out_dir):
        if f.endswith(".txt"):
            os.remove(os.path.join(out_dir, f))
    by = {}
    for nm, first, cnt in runs:
        by.setdefault(nm.split("__", 1)[0], []).extend(quads[first:first + cnt])
    counts = {}
    for piece, qs in by.items():
        off = np.array(OFF[piece])
        flip = np.ones(3) if piece == "weapon" else np.array([1.0, -1.0, -1.0])
        out = ["# %s %s: made in Blender by tools/blender/belial_bone.py (forms_kit); %s; UV space 128" % (SET, piece, "item space (grip at the origin, blade along +y, head toward +x)" if piece == "weapon" else "model space")]
        for q in qs:
            v = q.split()
            vals = [float(x) for x in v[2:]]
            row = []
            for k in range(4):
                o = k * 8
                P = (np.array(vals[o:o + 3]) - off) * flip
                N = np.array(vals[o + 5:o + 8]) * flip
                row.append("%.2f %.2f %.2f %.2f %.2f %.3f %.3f %.3f" % (*P, vals[o + 3], vals[o + 4], *N))
            out.append("qn %s %s" % (v[1], " ".join(row)))
        with open(os.path.join(out_dir, piece + ".txt"), "w", newline="\n") as fh:
            fh.write("\n".join(out) + "\n")
        counts[piece] = (sum(1 for q in qs if q.split()[1] == "0"), sum(1 for q in qs if q.split()[1] == "2"))
    return counts


UV_WEIGHTS = {"backdrop__skull": 1.7, "backdrop__skull_jaw": 1.35, "backdrop__skull_horns": 1.2, "backdrop__skull_teeth": 1.1, "backdrop__spine": 1.3, "backdrop__neural": 1.1,
              "backdrop__ribs": 1.0, "backdrop__foreclaw": 1.1, "weapon__blade": 1.6, "weapon__emblem": 1.3, "weapon__grip": 0.8, "weapon__shaft": 0.8, "weapon__blade_spine": 1.1,
              "helm__circlet": 1.2, "crest__brow": 1.2, "tail_seg__vertebra": 0.9, "tail_seg__spines": 0.7, "belt__hang": 0.8, "belt__waistband": 0.8, "mantle__cape": 0.7,
              "wing_out_l__membrane": 0.7, "wing_out_r__membrane": 0.7, "wing_out_l__fingers": 1.2, "wing_out_r__fingers": 1.2, "wing_in_l__humerus": 1.2, "wing_in_r__humerus": 1.2,
              "wing_out_l__forearm": 1.2, "wing_out_r__forearm": 1.2}          # linear texel density per part: the skull, the spine and the wing bones get more, the cloth and hidden parts less


def pack_skin(path, colors=96, denoise=1.0, median=7, blur=1.5):
    """The atlas skin as a palette PNG with `colors` colours (alpha binary): a median pass (blended `denoise`) takes the bake's sampling speckle out first, so the palette
    is spent on the shapes' shading and not on noise (blocky texels come from a too small palette fed with noise). Returns (kb before, kb after, psnr)."""
    from PIL import Image, ImageFilter
    before = os.path.getsize(path) / 1024.0
    a = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    alpha = a[..., 3]
    rgb_ = Image.fromarray(a[..., :3].astype(np.uint8))
    med = np.asarray(rgb_.filter(ImageFilter.MedianFilter(median))).astype(np.float32)
    sm = np.asarray(Image.fromarray(med.astype(np.uint8)).filter(ImageFilter.GaussianBlur(blur))).astype(np.float32)
    den = a[..., :3] * (1 - denoise) + sm * denoise
    q = Image.fromarray(np.clip(den, 0, 255).astype(np.uint8)).quantize(colors=colors - 1, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)
    idx = np.asarray(q).copy()
    idx[alpha < 128] = colors - 1
    pal = q.getpalette()[: (colors - 1) * 3]
    pal += [0] * ((colors - 1) * 3 - len(pal)) + [0, 0, 0]
    out = Image.fromarray(idx, "P")
    out.putpalette(pal)
    out.save(path, optimize=True, transparency=bytes([255] * (colors - 1) + [0]))
    r = np.asarray(Image.open(path).convert("RGBA")).astype(np.float32)
    w = alpha[..., None] / 255.0
    mse = float((((a[..., :3] - r[..., :3]) ** 2) * w).sum() / max(1.0, float(w.sum()) * 3.0))
    return before, os.path.getsize(path) / 1024.0, 10.0 * math.log10(255.0 ** 2 / max(mse, 1e-9))


def main(argv):
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("--trial")
    ap.add_argument("--quick", action="store_true")
    ap.add_argument("--only")
    ap.add_argument("--samples", type=int, default=24)
    ap.add_argument("--threads", type=int, default=2)
    ap.add_argument("--bake", type=int, default=1024)
    ap.add_argument("--skin", type=int, default=1024)
    ap.add_argument("--beauty")
    ap.add_argument("--keep-raw", help="also keep the unpacked atlas in this folder (to tune the packing)")
    ap.add_argument("--colors", type=int, default=96)
    ap.add_argument("--denoise", type=float, default=1.0)
    a = ap.parse_args(argv)
    sc = K.begin(a.samples)
    sc.render.threads_mode = "FIXED"
    sc.render.threads = a.threads
    t0 = time.time()
    q = build_all(a.only.split(",") if a.only else None)
    tab = piece_table()
    print("pieces:", sorted(tab.items(), key=lambda kv: -kv[1]))
    print("[%s] built: ~%d body quads in %.1f s" % (SET, q, time.time() - t0))
    if a.beauty:
        os.makedirs(a.beauty, exist_ok=True)
        K.beauty(os.path.join(a.beauty, "bone_all.png"), view="front", samples=12, size=(1400, 900), lens=50)
    if a.quick:
        return
    root = a.trial or bl.ASSETS
    mesh_dir = os.path.join(root, "weapon_meshes") if a.trial else bl.MESH_DIR
    skin_dir = os.path.join(root, "textures", "entity", "djinn") if a.trial else os.path.join(bl.ASSETS, "textures", "entity", "djinn")
    tmp = os.path.join(a.trial or "/tmp", "_belial_bone_tmp")
    os.makedirs(tmp, exist_ok=True)
    os.makedirs(skin_dir, exist_ok=True)
    hdr = os.path.join(bl.MESH_DIR, "vassal_scythe.txt")
    mesh, atlas, stats = bl.finish("_belial_bone_all", glow=("FFFFFF", 0.12, 0.45, 1.0), header_from=hdr, bake=a.bake, skin=a.skin, out_mesh=tmp, out_skin=tmp, preview=None, write=True,
                                   budget=10 ** 6, cage=0.5, uv_weights=UV_WEIGHTS)
    counts = export_pieces(os.path.join(tmp, "_belial_bone_all.txt"), os.path.join(root, "djinn", SET))
    # the atlas: pack and name it the way DjinnLook.skinPath would (textures/entity/djinn/<id>_<kind>_armor[_glow].png: here <id> = belial, kind `bone`)
    import shutil
    for suffix, dst in (("", "belial_bone_armor.png"), ("_glow", "belial_bone_armor_glow.png")):
        shutil.copy(os.path.join(tmp, "_belial_bone_all%s.png" % suffix), os.path.join(skin_dir, dst))
    if a.keep_raw:
        os.makedirs(a.keep_raw, exist_ok=True)
        for f in ("belial_bone_armor.png", "belial_bone_armor_glow.png"):
            shutil.copy(os.path.join(skin_dir, f), os.path.join(a.keep_raw, f))
    b, af, ps = pack_skin(os.path.join(skin_dir, "belial_bone_armor.png"), colors=a.colors, denoise=a.denoise)
    print("[pack] belial_bone_armor.png %.0f KB -> %.0f KB (%d colours, psnr %.1f dB)" % (b, af, a.colors, ps))
    b, af, ps = K.pack_png(os.path.join(skin_dir, "belial_bone_armor_glow.png"), colors=40, denoise=0.9)
    print("[pack] belial_bone_armor_glow.png %.0f KB -> %.0f KB (psnr %.1f dB)" % (b, af, ps))
    tot_b = sum(c[0] for c in counts.values())
    tot_g = sum(c[1] for c in counts.values())
    print("[%s] pieces:" % SET, {k: v for k, v in sorted(counts.items())})
    print("[%s] total %d body + %d glow = %d quad lines; %.0f s" % (SET, tot_b, tot_g, tot_b + tot_g, time.time() - t0))
    shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main(sys.argv[1:])

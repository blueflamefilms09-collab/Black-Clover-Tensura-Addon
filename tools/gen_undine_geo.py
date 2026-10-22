"""The Undine, Noelle Silva's water Spirit Lord (docs/magic/undine.md): a tall woman of water. Models (Bedrock geo, 1/16 block units, y up, front -z), hand-painted sheets (tools/gen_undine_art.py),
one animation file, three levels of detail, a translucent water pass, an additive foam pass and the wave base of the full form:

    python3 -B tools/gen_undine_geo.py              writes assets/nusmp/{geo,animations,textures}/entity/undine_*
    python3 -B tools/gen_undine_geo.py --count      prints the quads of every model

    undine_body       LOD 0 (cutout: skin, face, hair, the gown)     undine_body_lod1   LOD 1      undine_body_lod2   LOD 2 (low poly)
    undine_water      LOD 0 (translucent: the flowing outer gown, foam bands, droplets)     undine_water_lod1
    undine_fx         additive soft foam highlights (SafeAdditive)
    undine_base       the wave base of the full form (translucent)

The figure is authored at the size of the FULL form (3.1 blocks from the hem to the crown, arms spread, palms up); the companion form is the same model drawn smaller (client.prop.UndineDraw).
Detail is texture and the geometry is spent on the face, the hair ribbons and the outline of the gown: the hair is many single-quad CARDS (a cube flat in z) with a painted scalloped outline, the
gown a cone of strips. Every LOD uses the same bone names, so the one animation file drives them all (a bone a model does not have is ignored).
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import undine_geo_lib as L  # noqa: E402
import gen_undine_art as A  # noqa: E402
from undine_geo_lib import rot_m, euler_of, rx_m, ry_m, rz_m  # noqa: E402

# ====================================================================================================================== numbers (model units: 1/16 block)
from undine_art_lib import SK  # noqa: E402
HEM_R, WAIST_R, WAIST_Y = SK['HEM_R'], SK['WAIST_R'], SK['WAIST_Y']
ARM_LEN, FORE_LEN = 13.4, 12.4
SHOULDER = (4.3, 39.0)
ARM_SPREAD = 40.0           # degrees the upper arm leaves the vertical in the full (spread) pose
SKIRT_SECTORS = 12          # sway groups round the skirt


def lod_cfg(lod):
    return {
        0: dict(ribbons=1.0, panels=SK['INNER_N'], streams=SK['OUTER_N'], shell=24, splash=24, tears=2, drops=24, hair_layers=3, locks=4, fingers=True, base_n=(10, 13, 16)),
        1: dict(ribbons=0.5, panels=12, streams=12, shell=12, splash=12, tears=1, drops=8, hair_layers=2, locks=2, fingers=False, base_n=(8, 10, 12)),
        2: dict(ribbons=0.18, panels=8, streams=0, shell=0, splash=0, tears=0, drops=0, hair_layers=1, locks=0, fingers=False, base_n=(6, 8, 0)),
    }[lod]


# ====================================================================================================================== the sheets
def make_body_atlas(art, lod=0):
    k = {0: 1.0, 1: 0.5, 2: 0.25}[lod]
    cfg = lod_cfg(lod)
    art.B.CTX["INNER_N"], art.B.CTX["OUTER_N"] = cfg["panels"], max(1, cfg["streams"])
    regs = art.regions(lod)
    width = int(2048 * k)
    at = L.Atlas(width, L.needed_height(regs, width))
    for name, (w, h) in sorted(regs.items(), key=lambda kv: -kv[1][1]):
        at.region(name, w, h, lambda ww, hh, n=name: art.paint(n, ww, hh))
    return at


def make_water_atlas(art):
    at = L.Atlas(1024, 1024)
    for name, (w, h) in sorted(art.WATER_REGIONS.items(), key=lambda kv: -kv[1][1]):
        at.region(name, w, h, lambda ww, hh, n=name: art.paint(n, ww, hh))
    return at


def make_fx_atlas(art):
    at = L.Atlas(512, 512)
    for name, (w, h) in sorted(art.FX_REGIONS.items(), key=lambda kv: -kv[1][1]):
        at.region(name, w, h, lambda ww, hh, n=name: art.paint(n, ww, hh))
    return at


def make_base_atlas(art):
    at = L.Atlas(1024, 1024)
    for name, (w, h) in sorted(art.BASE_REGIONS.items(), key=lambda kv: -kv[1][1]):
        at.region(name, w, h, lambda ww, hh, n=name: art.paint(n, ww, hh))
    return at


# ====================================================================================================================== skeleton and pose
class Rig:
    """The bones every model shares. The arm chain is authored hanging straight down (the cubes are absolute and unrotated); the pose is the bones' rest rotations."""

    def __init__(self):
        sx, sy = SHOULDER
        self.sh = {+1: (sx, sy, 0.0), -1: (-sx, sy, 0.0)}
        self.elbow = {s: (self.sh[s][0], sy - ARM_LEN, 0.0) for s in (1, -1)}
        self.wrist = {s: (self.sh[s][0], sy - ARM_LEN - FORE_LEN, 0.0) for s in (1, -1)}
        # rest pose of the spread arms: the upper arm leaves the vertical outward, the forearm bends a little forward and keeps going out, the palm turns up
        self.arm_rot = {s: (0.0, 0.0, s * ARM_SPREAD) for s in (1, -1)}
        self.fore_rot = {s: (-18.0, 0.0, s * 8.0) for s in (1, -1)}
        self.hand_rot = {s: self._hand_rot(s) for s in (1, -1)}

    def _hand_rot(self, s):
        ra = rot_m(self.arm_rot[s])
        rf = rot_m(self.fore_rot[s])
        chain = ra @ rf                                                  # (bone chain: the arm first, the forearm inside it) applied to a vector: forearm first
        chain = ra @ rf
        d = chain @ np.array([0.0, -1.0, 0.0])                           # where the fingers point (they hang down in the unrotated hand)
        d = d / np.linalg.norm(d)
        want_n = np.array([0.18 * s, 0.93, -0.30])                       # the palm faces up and a little forward and outward
        want_n = want_n - d * np.dot(want_n, d)
        want_n /= np.linalg.norm(want_n)
        y = -d
        z = -want_n                                                      # the hand's own front (-z) is its palm
        x = np.cross(y, z)
        x /= np.linalg.norm(x)
        target = np.stack([x, y, z], axis=1)
        local = chain.T @ target
        return tuple(euler_of(local))


RIG = Rig()


def add_skeleton(m):
    m.bone("float", (0, 0, 0))
    m.bone("hip", (0, 30, 0), "float")
    m.bone("torso", (0, WAIST_Y, 0), "hip")
    m.bone("neck", (0, 39.4, 0), "torso")
    m.bone("head", (0, 41.2, 0), "neck")
    for s, tag in ((1, "l"), (-1, "r")):
        m.bone("arm_" + tag, RIG.sh[s], "torso", rotation=RIG.arm_rot[s])
        m.bone("fore_" + tag, RIG.elbow[s], "arm_" + tag, rotation=RIG.fore_rot[s])
        m.bone("hand_" + tag, RIG.wrist[s], "fore_" + tag, rotation=RIG.hand_rot[s])
    for k in range(SKIRT_SECTORS):
        m.bone("sk%d" % k, (0, WAIST_Y, 0), "hip")
    m.bone("hair_back", (0, 44.0, 2.0), "head")
    for s, tag in ((1, "l"), (-1, "r")):
        m.bone("band_" + tag, (RIG.sh[s][0], RIG.elbow[s][1] + 6.2, 0.0), "arm_" + tag)


# ====================================================================================================================== builders
def sector_of(angle_deg):
    return int(((angle_deg % 360.0) / 360.0) * SKIRT_SECTORS) % SKIRT_SECTORS


def build_body(at, lod, ident):
    cfg = lod_cfg(lod)
    m = L.Model(ident, at)
    add_skeleton(m)
    rng = np.random.default_rng(777 + lod)

    # ------------------------------------------------------------------ head: the face and the shell of hair over the skull
    m.cube("head", (-3.7, 41.0, -3.4), (7.4, 9.0, 6.8), {"north": "face", "west": ("head_side", True), "east": "head_side"})
    m.cube("head", (-4.1, 40.9, -3.8), (8.2, 9.8, 8.0), {"north": "cap_front", "up": "cap_top", "south": "cap_back", "west": ("cap_side", True), "east": "cap_side"})
    # the fin ears: a fan of rays at each side of the head
    for s in (1, -1):
        if "ear" not in at.regions:
            break
        m.card("head", (s * 4.9, 46.4, -0.4), (4.6, 6.0), (0, s * -18.0, s * -12.0), "ear", flip=(s < 0))
        if lod == 0:
            m.card("head", (s * 4.6, 46.4, 0.2), (4.6, 6.0), (0, s * 60.0, s * -8.0), "ear", flip=(s < 0))
    # the neck and the shoulders (skin), the bodice of water
    nk = "neck" if "neck" in at.regions else "arm"
    m.cube("neck", (-1.3, 38.4, -1.2), (2.6, 3.3, 2.5), {"north": nk, "south": nk, "west": nk, "east": nk})
    m.cube("torso", (-4.9, 36.6, -2.4), (9.8, 3.2, 4.8), {"north": "shoulder_front", "south": "shoulder_back" if "shoulder_back" in at.regions else "shoulder_front", "up": nk, "west": nk, "east": nk})
    up_w, lo_w = (0.0, 0.0, 1.0, 0.55), (0.09, 0.55, 0.91, 1.0)
    m.cube("torso", (-4.5, 33.9, -2.7), (9.0, 4.4, 5.4), {"north": ("bodice_front", False, up_w), "south": ("bodice_back", False, up_w), "west": ("bodice_side", True, up_w), "east": ("bodice_side", False, up_w)})
    m.cube("torso", (-3.7, 30.4, -2.4), (7.4, 3.6, 4.8), {"north": ("bodice_front", False, lo_w), "south": ("bodice_back", False, lo_w), "west": ("bodice_side", True, lo_w), "east": ("bodice_side", False, lo_w)})

    # ------------------------------------------------------------------ arms: cubes hanging straight down in the unrotated frame, the pose is in the bones
    for s, tag in ((1, "l"), (-1, "r")):
        sx, sy, _ = RIG.sh[s]
        ex, ey, _ = RIG.elbow[s]
        wx, wy, _ = RIG.wrist[s]
        m.cube("arm_" + tag, (sx - 1.5, ey, -1.5), (3.0, ARM_LEN + 0.6, 3.0), {"north": "arm", "south": "arm", "west": "arm", "east": "arm"})
        m.cube("fore_" + tag, (ex - 1.3, wy, -1.3), (2.6, FORE_LEN + 0.3, 2.6), {"north": "forearm", "south": "forearm", "west": "forearm", "east": "forearm"})
        # the hand: a palm (its front, -z, is the palm side), the fingers fanned out and the thumb
        if "hand_palm" in at.regions:
            m.cube("hand_" + tag, (wx - 1.5, wy - 3.6, -0.55), (3.0, 3.8, 1.1), {"north": "hand_palm", "south": "hand_back", "west": "hand_side", "east": "hand_side", "up": None, "down": "hand_side"})
        else:
            m.cube("hand_" + tag, (wx - 1.3, wy - 3.8, -0.9), (2.6, 3.8, 1.8), {"north": "forearm", "south": "forearm", "west": "forearm", "east": "forearm"})
        if cfg["fingers"]:
            m.card("hand_" + tag, (wx + s * 0.1, wy - 5.7, -0.1), (3.4, 3.6), (0, 0, 0), "fingers", flip=(s < 0))
            m.card("hand_" + tag, (wx - s * 1.9, wy - 2.2, -0.9), (2.2, 2.8), (0, 0, s * 38.0), "thumb", flip=(s < 0))

    # ------------------------------------------------------------------ the gown: an inner cone (solid), a cone of streams over it, a foam fringe at the hem
    slant = math.hypot(WAIST_Y, HEM_R - WAIST_R)
    tilt = math.degrees(math.atan2(HEM_R - WAIST_R, WAIST_Y))
    n = cfg["panels"]
    cw = 2 * math.pi * HEM_R / n * SK['INNER_OVER']
    for i in range(n):
        az = (i + 0.5) / n * 360.0
        _skirt_panel(m, "sk%d" % sector_of(az), az, 0.0, cw, slant, tilt, ("skirt_in", i, n), 0.0)

    # ------------------------------------------------------------------ hair
    plan = hair_plan(cfg, lod, m.atlas.regions)
    for r in plan:
        if r["bone"] not in m.bones:
            m.bone(r["bone"], tuple(r["root"]), "head")
        m.card(r["bone"], tuple(r["centre"]), (r["length"], r["height"]), r["euler"], r["region"], flip=r["flip"])
    m.card("hair_back", (0, 36.0, 3.2), (15.0, 30.0), (0, 0, 0), "back_mass")
    if lod == 0:
        m.card("hair_back", (0, 40.0, 3.9), (11.0, 22.0), (0, 0, 0), "back_mass", flip=True)
    for s in (1, -1):                                                                  # the locks that fall in front of the shoulders
        for k in range(cfg["locks"] // 2 if "lock" in m.atlas.regions else 0):
            m.card("head", (s * (3.9 + 0.7 * k), 40.5 - 1.4 * k, -3.2 - 0.3 * k), (2.6, 13.0 - 1.5 * k), (-4.0, s * (-6.0 - 8.0 * k), s * (3.0 + 4.0 * k)), "lock", flip=(s < 0))
    return m


def _skirt_panel(m, bone, az_deg, push, width, length, tilt_deg, win, flip=False):
    """One strip of the gown at azimuth az (0 = the front, going round toward +x): a card hanging from the waist, tilted out by the cone's angle."""
    region, i, n = win
    a = math.radians(az_deg)
    half = length / 2.0
    tl = math.radians(tilt_deg)
    radial = np.array([math.sin(a), 0.0, -math.cos(a)])           # az 0 = the front (-z); the azimuth turns toward +x
    up = np.array([0.0, 1.0, 0.0])
    top = np.array([0.0, WAIST_Y + 0.3, 0.0]) + radial * (WAIST_R - 0.2 + push)
    axis_down = -up * math.cos(tl) + radial * math.sin(tl)
    centre = top + axis_down * half
    normal = radial * math.cos(tl) + up * math.sin(tl)            # the card's front (-z) faces outward, tilted up a little by the cone
    eul = L.look_card(normal, up_hint=(0, 1, 0))
    u0, u1 = i / float(n), (i + 1) / float(n)
    m.card(bone, centre, (width, length), eul, region, flip=flip, window=(u0, 0.0, u1, 1.0))


HAIR_ROWS = [
    # (root y, root x, root z, length, rise deg, yaw deg (turn toward the back), roll deg, texture) for the left side (+x); the right side mirrors it
    (51.0, 2.6, 0.4, 14.0, 40.0, 22.0, 4.0, 3), (49.0, 3.4, 0.2, 24.0, 14.0, 5.0, 0.0, 0), (46.5, 3.8, 0.0, 22.0, 4.0, 14.0, 8.0, 1),
    (43.5, 4.0, 0.6, 24.0, -2.0, 8.0, -6.0, 2), (40.5, 4.6, 0.6, 25.0, -10.0, 6.0, 5.0, 0), (37.5, 5.0, 1.0, 22.0, -16.0, 12.0, -4.0, 1),
    (34.5, 5.4, 1.4, 19.0, -24.0, 10.0, 6.0, 2), (31.5, 5.0, 1.6, 15.0, -30.0, 16.0, 0.0, 4),
    (44.5, 3.7, -0.2, 12.5, -52.0, 4.0, 6.0, 5), (41.0, 4.0, -0.2, 14.0, -64.0, 4.0, -5.0, 3),
]


def hair_plan(cfg, lod, regions):
    """The ribbons of the hair, as data (so the water pass can hang droplets on the same bones): bone name, root (the bone's pivot), card centre, euler, size, texture."""
    rng = np.random.default_rng(777 + lod)
    ribs = [r for r in ("rib_a", "rib_b", "rib_c", "rib_d", "rib_e", "rib_f") if r in regions]
    out = []
    keep = cfg["ribbons"]
    for layer in range(cfg["hair_layers"]):
        for j, (ry, rx, rz, ln, rise, yaw, roll, tx) in enumerate(HAIR_ROWS):
            if keep < 1.0 and (j * 0.6180339 + layer * 0.37) % 1.0 > keep:
                continue
            for s in (1, -1):
                lyaw = yaw + layer * 34.0 + (4 if s < 0 else -4)
                lrise = rise + (layer - 1) * 6.0
                length = ln * (1.0 - 0.07 * layer) * (0.95 + 0.1 * rng.random())
                root = np.array([rx, ry - layer * 0.9, rz + layer * 0.6])
                R = ry_m(-lyaw) @ rz_m(lrise) @ rx_m(roll + layer * 9.0 * s)         # out along +x, raised by the rise, turned back by the yaw about the vertical
                d = R @ np.array([1.0, 0.0, 0.0])
                centre = root + d * (length / 2.0)
                tip = root + d * length
                if s < 0:                                                           # the right side is the mirror image
                    Sm = np.diag([-1.0, 1.0, 1.0])
                    centre, tip, root = Sm @ centre, Sm @ tip, Sm @ root
                    R = Sm @ R @ Sm
                region = ribs[(tx + layer) % len(ribs)]
                rw, rh = regions[region][-2], regions[region][-1]
                idx = len(out)
                out.append(dict(s=s, index=idx, bone="hr_%s%d" % ("l" if s > 0 else "r", idx), root=root, centre=centre, tip=tip, euler=euler_of(R), length=length,
                                height=length * rh / rw, region=region, flip=(s < 0)))
    return out


# ====================================================================================================================== the water pass, the foam pass, the wave base
def water_regions(art, lod):
    k = 1.0 if lod == 0 else 0.5
    out = {n: (max(8, int(round(w * k / 4.0)) * 4), max(8, int(round(h * k / 4.0)) * 4)) for n, (w, h) in art.WATER_REGIONS.items()}
    n = lod_cfg(lod)["streams"]
    out["skirt_out"] = (n * (64 if lod == 0 else 44), 380 if lod == 0 else 192)
    art.B.CTX["OUTER_N"] = n
    return out


def make_atlas_of(regs, width, art):
    at = L.Atlas(width, L.needed_height(regs, width))
    for name, (w, h) in sorted(regs.items(), key=lambda kv: -kv[1][1]):
        at.region(name, w, h, lambda ww, hh, n=name: art.paint(n, ww, hh))
    return at


def build_water(at, lod, ident, body_regions):
    cfg = lod_cfg(lod)
    m = L.Model(ident, at)
    add_skeleton(m)
    slant = math.hypot(WAIST_Y, HEM_R - WAIST_R)
    tilt = math.degrees(math.atan2(HEM_R - WAIST_R, WAIST_Y))
    nso = cfg["streams"]
    for i in range(nso):                                                                 # the streams over the cone (drawn after the body, so they always lie on top of it)
        az = (i + 0.5) / nso * 360.0
        _skirt_panel(m, "sk%d" % sector_of(az), az, SK['OUTER_PUSH'], 2 * math.pi * HEM_R / nso * SK['OUTER_OVER'], slant + SK['OUTER_EXTRA'], tilt + 0.6, ("skirt_out", i, nso))
    ns = cfg["shell"]
    for i in range(ns):                                                                  # the flowing outer gown: clear strips that hang a little proud of the cone and run past the hem
        az = (i + 0.5) / ns * 360.0
        _skirt_panel(m, "sk%d" % sector_of(az), az, 1.5, 2 * math.pi * HEM_R / ns * 1.5, slant + 4.0, tilt + 1.1, ("shell", 0, 1), flip=(i % 2 == 1))
    nf = cfg["splash"]
    for i in range(nf):                                                                  # the foam at the hem
        az = (i + 0.5) / nf * 360.0
        a = math.radians(az)
        radial = np.array([math.sin(a), 0.0, -math.cos(a)])
        c = radial * (HEM_R + 1.0) + np.array([0, 2.2, 0])
        eul = L.look_card(radial * 0.94 + np.array([0, 0.34, 0]), up_hint=(0, 1, 0), roll=(i % 3 - 1) * 6.0)
        m.card("sk%d" % sector_of(az), tuple(c), (2 * math.pi * (HEM_R + 1) / nf * 1.3, 4.0), eul, "splash", flip=(i % 2 == 1))
    for s, tag in ((1, "l"), (-1, "r")):                                                 # the foam bands on the upper arms, and torn streaks off them
        sx, sy, _ = RIG.sh[s]
        ey = RIG.elbow[s][1]
        m.cube("band_" + tag, (sx - 2.1, ey + 5.0, -2.1), (4.2, 2.4, 4.2), {"north": "foam_band", "south": "foam_band", "west": "foam_band", "east": "foam_band"})
        for k in range(cfg["tears"]):
            m.card("band_" + tag, (sx + s * (2.6 + 1.2 * k), ey + 6.4 - 1.6 * k, -2.4 + 0.2 * k), (5.0, 1.7), (0, 0, s * (-26 - 16 * k)), "foam_tear", flip=(s < 0))
    plan = hair_plan(cfg, lod, body_regions)
    nd = cfg["drops"]
    step = max(1, len(plan) // max(1, nd)) if nd else 1
    for r in [p for p in plan if p["index"] % step == 0][:nd]:                         # droplets and blobs hung off the hair tips
        if r["bone"] not in m.bones:
            m.bone(r["bone"], tuple(r["root"]), "head")
        k = r["index"] % 4
        c = r["tip"] + np.array([r["s"] * 1.2, -0.4 * ((r["index"] % 3) - 1), -0.6])
        m.card(r["bone"], tuple(c), (2.0, 2.0), (0, 0, 0), "droplet", window=(k / 4.0, 0.0, (k + 1) / 4.0, 1.0))
    return m


def build_fx(at, ident):
    m = L.Model(ident, at)
    add_skeleton(m)
    slant = math.hypot(WAIST_Y, HEM_R - WAIST_R)
    tilt = math.degrees(math.atan2(HEM_R - WAIST_R, WAIST_Y))
    n = 12
    for i in range(n):                                                                  # soft streaks of light that lie on the gown (a glossy water sheen)
        az = (i + 0.3) / n * 360.0
        _skirt_panel(m, "sk%d" % sector_of(az), az, 0.9, 5.0, slant * 0.92, tilt + 0.8, ("streak", 0, 1), flip=(i % 2 == 1))
    plan = hair_plan(lod_cfg(0), 0, A.regions(0))
    for r in plan[::4][:12]:                                                            # glints at the hair tips
        if r["bone"] not in m.bones:
            m.bone(r["bone"], tuple(r["root"]), "head")
        m.card(r["bone"], tuple(r["tip"] + np.array([0, 0.4, -0.8])), (3.0, 3.0), (0, 0, (r["index"] * 37) % 90), "glint")
    for s, tag in ((1, "l"), (-1, "r")):                                                # a soft glow in each open palm
        wx, wy, _ = RIG.wrist[s]
        m.card("hand_" + tag, (wx, wy - 3.0, -1.4), (6.0, 6.0), (0, 0, 0), "glow")
    return m


def build_base(at, lod, ident):
    cfg = lod_cfg(lod)
    m = L.Model(ident, at)
    rings = [("base_a", 21.0, 22.0, "wave_a", 0), ("base_b", 30.0, 26.0, "wave_b", 1), ("base_c", 39.0, 30.0, "wave_c", 2)]
    for bone, R, w, region, k in rings:
        n = cfg["base_n"][k]
        if n <= 0:
            continue
        m.bone(bone, (0, 0, 0), "root")
        rw, rh = m.atlas.regions[region][2], m.atlas.regions[region][3]
        h = w * rh / rw
        for i in range(n):
            az = (i + 0.5 * (k % 2)) / n * 360.0
            a = math.radians(az)
            radial = np.array([math.sin(a), 0.0, -math.cos(a)])
            c = radial * R + np.array([0, h / 2.0 - 1.2 + 0.6 * ((i * 7 + k) % 3), 0])
            eul = L.look_card(radial * 0.96 + np.array([0, 0.28, 0]), up_hint=(0, 1, 0), roll=((i * 5 + k * 3) % 7 - 3) * 2.0)
            m.card(bone, tuple(c), (w * (0.9 + 0.25 * ((i + k) % 3) / 2.0), h * (0.85 + 0.3 * ((i * 5 + k) % 4) / 3.0)), eul, region, flip=(i % 2 == 1))
    return m


# ====================================================================================================================== animations
def _t(length, steps=16):
    return [length * i / steps for i in range(steps + 1)]


def _sway(length, amp, phase, axes=(0, 1, 2), freq=1, steps=16, bias=(0, 0, 0), ratio=(1.0, 1.0, 1.0)):
    """Rotation track: each chosen axis swings by amp*ratio with its own phase step; `bias` is added."""
    fr = {}
    for i in range(steps + 1):
        t = i / steps
        v = [bias[0], bias[1], bias[2]]
        for k, ax in enumerate(axes):
            v[ax] += amp * ratio[ax] * math.sin((t * freq + phase + 0.21 * k) * math.tau)
        fr[length * t] = tuple(v)
    return fr


MAX_RIBBONS = 48


def animations():
    clips = {}

    def hair_tracks(length, amp, flare=0.0):
        tr = {}
        for idx in range(MAX_RIBBONS):
            ph = (idx * 0.137) % 1.0
            for side in ("l", "r"):
                sg = 1 if side == "l" else -1
                base = (flare * 0.6 * math.sin(idx), sg * flare * 0.3, sg * flare * 0.5)
                tr["hr_%s%d" % (side, idx)] = {"rotation": _sway(length, amp, ph + (0.08 if side == "r" else 0.0), ratio=(2.2, 1.0, 1.1), bias=base)}
        tr["hair_back"] = {"rotation": _sway(length, amp * 0.7, 0.3, axes=(0, 2), ratio=(1.6, 1.0, 1.0))}
        return tr

    def skirt_tracks(length, amp):
        tr = {}
        for k in range(SKIRT_SECTORS):
            az = (k + 0.5) / SKIRT_SECTORS * math.tau
            tx, tz = math.cos(az), -math.sin(az)                            # the tangent of the circle: small rotations about it tilt the strip in and out
            ph = k / SKIRT_SECTORS * 1.0
            fr = {}
            for i in range(17):
                t = i / 16.0
                a = amp * math.sin((t + ph) * math.tau)
                fr[length * t] = (a * tx, 0.0, a * tz)
            tr["sk%d" % k] = {"rotation": fr}
        return tr

    def pulse(T, k):
        return {"scale": {t: (1 + 0.05 * k * math.sin(t / T * math.tau * 2), 1 + 0.08 * k * math.sin(t / T * math.tau * 2 + 1), 1 + 0.05 * k * math.sin(t / T * math.tau * 2)) for t in _t(T)}}

    # ---- idle: the companion, arms relaxed, hovering
    T = 4.0
    bones = {"float": {"position": {t: (0, 0.9 * math.sin(t / T * math.tau), 0) for t in _t(T)}},
             "hip": {"rotation": _sway(T, 1.2, 0.0, axes=(2,))}, "torso": {"rotation": _sway(T, 0.8, 0.1, axes=(0,))},
             "head": {"rotation": _sway(T, 2.0, 0.15, axes=(0, 1), ratio=(0.6, 1.0, 1.0))}, "neck": {"rotation": _sway(T, 1.0, 0.1, axes=(0,))}}
    for s, tag in ((1, "l"), (-1, "r")):
        bones["arm_" + tag] = {"rotation": _sway(T, 1.5, 0.05 * s, axes=(2,), bias=(0, 0, -s * 17.0), ratio=(1, 1, s))}
        bones["fore_" + tag] = {"rotation": _sway(T, 2.0, 0.2, axes=(0,), bias=(-24.0, 0, -s * 6.0))}
        bones["hand_" + tag] = {"rotation": _sway(T, 3.0, 0.3, axes=(0,), bias=(8.0, 0, 0))}
        bones["band_" + tag] = pulse(T, 1.0)
    bones.update(hair_tracks(T, 2.6))
    bones.update(skirt_tracks(T, 2.4))
    clips["idle"] = {"loop": True, "length": T, "bones": bones}

    # ---- full_idle: the full form, arms spread palms up, hair streaming, the gown flowing
    T = 3.2
    bones = {"float": {"position": {t: (0, 0.5 * math.sin(t / T * math.tau), 0) for t in _t(T)}},
             "hip": {"rotation": _sway(T, 0.9, 0.0, axes=(2,))}, "torso": {"rotation": _sway(T, 0.7, 0.1, axes=(0,))},
             "head": {"rotation": _sway(T, 1.6, 0.15, axes=(0, 1), ratio=(0.5, 1.0, 1.0))}}
    for s, tag in ((1, "l"), (-1, "r")):
        bones["arm_" + tag] = {"rotation": _sway(T, 1.3, 0.05 * s, axes=(2,), ratio=(1, 1, s))}
        bones["fore_" + tag] = {"rotation": _sway(T, 2.0, 0.2, axes=(0,))}
        bones["hand_" + tag] = {"rotation": _sway(T, 3.0, 0.3, axes=(0,))}
        bones["band_" + tag] = pulse(T, 1.4)
    bones.update(hair_tracks(T, 3.6, flare=3.0))
    bones.update(skirt_tracks(T, 3.4))
    bones["base_a"] = {"rotation": _sway(T, 6.0, 0.0, axes=(1,))}
    bones["base_b"] = {"rotation": _sway(T, -5.0, 0.3, axes=(1,))}
    bones["base_c"] = {"rotation": _sway(T, 4.0, 0.6, axes=(1,))}
    clips["full_idle"] = {"loop": True, "length": T, "bones": bones}

    # ---- cast: from the relaxed pose to the arms spread, palms up, the hair flaring and the gown swelling; plays once and holds
    T = 1.2
    ts = [0.0, 0.25, 0.6, 0.8, T]
    bones = {}
    for s, tag in ((1, "l"), (-1, "r")):
        bones["arm_" + tag] = {"rotation": dict(zip(ts, [(0, 0, -s * 17.0), (0, 0, -s * 22.0), (0, 0, s * 4.0), (0, 0, s * 1.0), (0, 0, 0)]))}
        bones["fore_" + tag] = {"rotation": dict(zip(ts, [(-24.0, 0, -s * 6.0), (-34.0, 0, -s * 8.0), (-4.0, 0, s * 3.0), (0, 0, 0), (0, 0, 0)]))}
        bones["hand_" + tag] = {"rotation": dict(zip(ts, [(8.0, 0, 0), (14.0, 0, 0), (-6.0, 0, 0), (0, 0, 0), (0, 0, 0)]))}
        bones["band_" + tag] = {"scale": dict(zip(ts, [(1, 1, 1), (1, 1, 1), (1.25, 1.4, 1.25), (1.1, 1.15, 1.1), (1.05, 1.08, 1.05)]))}
    bones["float"] = {"position": dict(zip(ts, [(0, 0, 0), (0, -0.6, 0), (0, 1.8, 0), (0, 1.0, 0), (0, 0.8, 0)]))}
    bones["head"] = {"rotation": dict(zip(ts, [(0, 0, 0), (4.0, 0, 0), (-5.0, 0, 0), (-2.0, 0, 0), (-2.0, 0, 0)]))}
    for idx in range(MAX_RIBBONS):
        for side in ("l", "r"):
            sg = 1 if side == "l" else -1
            bones["hr_%s%d" % (side, idx)] = {"rotation": dict(zip(ts, [(0, 0, 0), (0, sg * -2.0, 0), (6 * math.sin(idx), sg * 8.0, sg * 7.0), (3 * math.sin(idx), sg * 4.0, sg * 3.0), (2 * math.sin(idx), sg * 3.0, sg * 3.0)]))}
    for k in range(SKIRT_SECTORS):
        az = (k + 0.5) / SKIRT_SECTORS * math.tau
        tx, tz = math.cos(az), -math.sin(az)
        bones["sk%d" % k] = {"rotation": dict(zip(ts, [(0, 0, 0), (-1.5 * tx, 0, -1.5 * tz), (6.0 * tx, 0, 6.0 * tz), (3.0 * tx, 0, 3.0 * tz), (3.0 * tx, 0, 3.0 * tz)]))}
    clips["cast"] = {"loop": "hold", "length": T, "bones": bones}

    # ---- open: the arms open from the relaxed pose to the spread one, slowly (the companion becoming the full form)
    T = 0.9
    ts = [0.0, 0.45, T]
    bones = {}
    for s, tag in ((1, "l"), (-1, "r")):
        bones["arm_" + tag] = {"rotation": dict(zip(ts, [(0, 0, -s * 17.0), (0, 0, -s * 6.0), (0, 0, 0)]))}
        bones["fore_" + tag] = {"rotation": dict(zip(ts, [(-24.0, 0, -s * 6.0), (-10.0, 0, -s * 2.0), (0, 0, 0)]))}
        bones["hand_" + tag] = {"rotation": dict(zip(ts, [(8.0, 0, 0), (3.0, 0, 0), (0, 0, 0)]))}
    clips["open"] = {"loop": "hold", "length": T, "bones": bones}
    return clips


# ====================================================================================================================== main
def build_all(count_only=False):
    art = A
    rows = []
    for lod, ident in ((0, "undine_body"), (1, "undine_body_lod1"), (2, "undine_body_lod2")):
        at = make_body_atlas(art, lod)
        m = build_body(at, lod, ident)
        rows.append((m.id, m.cubes(), m.quads(), at.w, at.h))
        if not count_only:
            m.save()
            at.save_color(m.id)
    for lod, ident in ((0, "undine_water"), (1, "undine_water_lod1")):
        regs = water_regions(art, lod)
        at = make_atlas_of(regs, 2048 if lod == 0 else 1024, art)
        m = build_water(at, lod, ident, art.regions(lod))
        rows.append((m.id, m.cubes(), m.quads(), at.w, at.h))
        if not count_only:
            m.save()
            at.save_color(m.id)
    at = make_atlas_of(art.FX_REGIONS, 256, art)
    m = build_fx(at, "undine_fx")
    rows.append((m.id, m.cubes(), m.quads(), at.w, at.h))
    if not count_only:
        m.save()
        at.save_color(m.id)
    for lod, ident in ((0, "undine_base"), (1, "undine_base_lod1")):
        regs = {n: ((w if lod == 0 else max(8, w // 2 // 4 * 4)), (h if lod == 0 else max(8, h // 2 // 4 * 4))) for n, (w, h) in art.BASE_REGIONS.items()}
        at = make_atlas_of(regs, 1024 if lod == 0 else 512, art)
        m = build_base(at, lod, ident)
        rows.append((m.id, m.cubes(), m.quads(), at.w, at.h))
        if not count_only:
            m.save()
            at.save_color(m.id)
    if not count_only:
        L.write_animations("undine_body", animations())
        ring = A.paint("ring", 256, 256)
        from PIL import Image
        Image.fromarray(np.clip(ring, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(L.ROOT, "textures", "entity", "undine_ring.png"), optimize=True)
    for r in rows:
        print("%-24s cubes %4d  quads %5d   sheet %d x %d" % r)
    return rows


def main():
    build_all("--count" in sys.argv)


if __name__ == "__main__":
    main()

"""Why: every character of the arcs stands on the same skeleton (the same bone names, so the same clips fit every model), and the body builds (slim, broad,
huge ...) are only different numbers on that skeleton. This module holds those numbers (`BUILDS`, `Dims`), the model under construction (`Rig`: bones, boxes with
mirrored twins, spikes and hair clumps, the packed texture) and the forward kinematics the clips are checked with (the same maths as client.geo.GeoDraw).

Frame: pixels (1/16 block), y up, origin at the feet, the model's front on -z, +x the model's LEFT (so the `_l` bones are at +x). Rotation signs of a clip: positive x
swings a hanging limb FORWARD (arms raise forward), negative x folds a knee and leans the body forward; positive z swings a hanging limb toward +x (the model's left);
positive y turns the part toward the model's right. Positions are pixels (y up), scales multiply.
"""
import math
import os
import sys

import numpy as np

sys.dont_write_bytecode = True
HERE = os.path.dirname(os.path.abspath(__file__))
TOOLS = os.path.abspath(os.path.join(HERE, ".."))
if TOOLS not in sys.path:
    sys.path.insert(0, TOOLS)
from geo_builder import Model, Anim  # noqa: E402
from .core import Pack, r5  # noqa: E402

KEY = "arc"
TEX_W = 512

# name -> torso width / depth, arm width / depth, leg width / depth (model pixels), the share of the body below the head that is leg, the default height in blocks
BUILDS = {
    "slim":    dict(tw=7.0, td=4.0, aw=3.0, ad=3.0, lw=3.0, ld=3.5, leg=0.54, h=1.95, hand=2.5),
    "average": dict(tw=8.0, td=4.5, aw=3.5, ad=3.5, lw=3.5, ld=4.0, leg=0.52, h=2.00, hand=2.5),
    "broad":   dict(tw=10.0, td=5.5, aw=4.5, ad=4.5, lw=4.5, ld=5.0, leg=0.48, h=2.10, hand=3.0),
    "huge":    dict(tw=15.0, td=8.0, aw=6.5, ad=6.5, lw=6.5, ld=7.0, leg=0.45, h=3.10, hand=4.0),
    "small":   dict(tw=6.0, td=3.5, aw=2.5, ad=2.5, lw=2.5, ld=3.0, leg=0.50, h=1.50, hand=2.0),
    "lanky":   dict(tw=7.0, td=3.5, aw=2.5, ad=2.5, lw=3.0, ld=3.5, leg=0.55, h=2.30, hand=2.5),
    "child":   dict(tw=6.0, td=3.5, aw=2.5, ad=2.5, lw=2.5, ld=3.0, leg=0.46, h=1.40, hand=2.0),
}
HEAD = 8.0
FOOT_H = 2.0
# the visible neck between the shoulders and the head (model pixels): a long thin build shows more of it
NECK = {"slim": 1.5, "average": 1.0, "broad": 0.5, "huge": 0.0, "small": 0.0, "lanky": 2.0, "child": 0.0}


class Dims:
    """The measured body (model pixels): where the hips, shoulders, knees and wrists are, and how thick each part is. Everything a part builder needs."""

    def __init__(self, build="average", height=None, widen=1.0):
        b = BUILDS[build]
        self.build = build
        blocks = height if height else b["h"]
        self.top = max(16.0, r5(blocks * 16.0))                    # the top of the head box
        self.neck = NECK[build]
        body = self.top - HEAD - self.neck
        self.leg_len = r5(body * b["leg"])
        self.torso_h = body - self.leg_len
        self.tw, self.td = b["tw"] * widen, b["td"] * widen
        self.aw, self.ad = b["aw"] * widen, b["ad"] * widen
        self.lw, self.ld = b["lw"] * widen, b["ld"] * widen
        self.hand = b["hand"]
        self.hip_y = self.leg_len
        self.sh_y = self.hip_y + self.torso_h                      # the shoulder line = the top of the torso = the bottom of the head
        self.arm_x = self.tw / 2 + self.aw / 2
        self.leg_x = self.lw / 2 + (0.25 if build in ("slim", "lanky", "small", "child") else 0.0)
        self.sh_piv = self.sh_y - min(self.aw / 2, 2.0)           # the shoulder joint
        arm_total = self.torso_h + 2.5 - (self.sh_y - self.sh_piv)       # shoulder to fingertip: the hand reaches about the middle of the thigh, and both hands can close on one shaft
        self.upper = r5((arm_total - self.hand) * 0.5)
        self.fore = r5(arm_total - self.hand - self.upper)
        self.elbow_y = self.sh_piv - self.upper
        self.wrist_y = self.elbow_y - self.fore
        self.thigh = r5(self.leg_len * 0.5)
        self.knee_y = self.hip_y - self.thigh
        self.head_y = self.sh_y + self.neck                         # the bottom of the head box (the neck joint of the head bone is at sh_y)
        self.head_top = self.head_y + HEAD

    def as_dict(self):
        return {k: (round(v, 3) if isinstance(v, float) else v) for k, v in self.__dict__.items()}


def rot_file(v, rx, ry, rz):
    """Where the file-frame vector v goes under a cube / bone rotation (rx, ry, rz) degrees, exactly as client.geo.GeoDraw applies it: the file's y is
    negated into vanilla space, the quaternion rotationZYX(-rz, ry, -rx) turns it (X first, then Y, then Z), and y is negated back."""
    x, y, z = v[0], -v[1], v[2]
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(-rz)
    c, s_ = math.cos(ax), math.sin(ax)
    y, z = y * c - z * s_, y * s_ + z * c
    c, s_ = math.cos(ay), math.sin(ay)
    x, z = x * c + z * s_, -x * s_ + z * c
    c, s_ = math.cos(az), math.sin(az)
    x, y = x * c - y * s_, x * s_ + y * c
    return (x, -y, z)


def rot_matrix(rx, ry, rz):
    """The 3 x 3 matrix of rot_file (columns = where the file axes go), written out in closed form because the arm solver calls it hundreds of thousands of times:
    M = N Rz Ry Rx N with N = diag(1, -1, 1) (the file's y is up, vanilla's is down) and the same angle signs as rot_file."""
    ax, ay, az = math.radians(-rx), math.radians(ry), math.radians(-rz)
    cx, sx = math.cos(ax), math.sin(ax)
    cy, sy = math.cos(ay), math.sin(ay)
    cz, sz = math.cos(az), math.sin(az)
    return np.array(((cz * cy, -(cz * sy * sx - sz * cx), cz * sy * cx + sz * sx),
                     (-(sz * cy), sz * sy * sx + cz * cx, -(sz * sy * cx - cz * sx)),
                     (-sy, -(cy * sx), cy * cx)))


class Rig:
    """A model under construction: bones, boxes (with mirrored pairs), spikes and clumps, animation, and the packed texture."""

    def __init__(self, name, dims, tex_h=1024):
        self.name = name
        self.d = dims
        self.m = Model(KEY, name, TEX_W, tex_h)
        self.pk = Pack(TEX_W, tex_h, 2)
        self.A = Anim()
        self.facts = {}                 # what the parts tell the clips (has a cape, a long skirt, wings ...)

    # ---- bones and boxes
    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        self.m.bone(name, parent, pivot, rotation)

    def has(self, name):
        return name in self.m.bones

    def box(self, bone, origin, size, paint, **kw):
        return self.pk.box(self.m, bone, origin, size, paint, **kw)

    def pair(self, bone_l, bone_r, origin, size, paint, **kw):
        """origin is the +x cube; its mirrored twin (x -> -x) goes on bone_r (the same bone twice for a single bone)."""
        c = self.pk.box(self.m, bone_l, origin, size, paint, **kw)
        self.pk.twin(self.m, bone_r, c)
        return c

    def pair2(self, bone_l, bone_r, origin, size, paint_l, paint_r, **kw):
        """Like pair, but the right-hand cube has its own painter (asymmetric details: a glove on one hand, a scar on one arm). Both painters are written for
        the +x side of the model: the right cube is built mirrored, so `left` is its outer face too."""
        c = self.pk.box(self.m, bone_l, origin, size, paint_l, **kw)
        sx = c["size"][0]
        o = c["origin"]
        rot = kw.get("rotation")
        piv = kw.get("pivot")
        mo = (-(o[0] + sx), o[1], o[2])
        mk = dict(kw)
        mk["rotation"] = None if not rot else (rot[0], -rot[1], -rot[2])
        mk["pivot"] = None if not rot or piv is None else (-piv[0], piv[1], piv[2])
        mk["mirror"] = True
        d = self.pk.box(self.m, bone_r, mo, size, paint_r, **mk)
        return c, d

    def spike(self, bone, base, w, ln, rx, rz, paint, ry=0.0, key=None, fracs=(0.55, 0.45), taper=0.55, bend=(0.0, 0.0), faces=None):
        """A tapering spike of stacked boxes standing at `base` (x, y, z), tilted by (rx, ry, rz); each segment is `taper` times as wide as the one below and
        bent by `bend` more degrees: tines, horns, thorns. The next segment starts exactly where the last one ends (rot_file)."""
        pos = list(base)
        angx, angz = rx, rz
        wd = w
        out = []
        for k, fr in enumerate(fracs):
            sl = max(0.5, r5(ln * fr))
            sw = max(0.5, r5(wd))
            c = self.box(bone, (pos[0] - sw / 2, pos[1], pos[2] - sw / 2), (sw, sl, sw), paint, rotation=(angx, ry, angz), pivot=tuple(pos), faces=faces,
                         key=None if key is None else "%s_%d" % (key, k))
            out.append(c)
            d = rot_file((0, sl, 0), angx, ry, angz)
            pos = [pos[0] + d[0], pos[1] + d[1], pos[2] + d[2]]
            angx += bend[0]
            angz += bend[1]
            wd *= taper
        return out

    def clump(self, bone, pivot, theta, alpha, w, ln, paint, standing=False, key=None, dens=4, thick=0.5, spin=0.0):
        """A flat clump plate (hair, fur, feathers) hanging from `pivot` (or standing on it when `standing`), its face turned to azimuth theta (0 = the front,
        90 = +x, 180 = the back) and its tip swung `alpha` degrees out of the vertical towards that side."""
        px_, py_, pz_ = pivot
        if standing:
            org, rot = (px_ - w / 2, py_, pz_ - thick / 2), (-alpha, -theta, spin)
        else:
            org, rot = (px_ - w / 2, py_ - ln, pz_ - thick / 2), (alpha, -theta, spin)
        return self.box(bone, org, (w, ln, thick), paint, rotation=rot, pivot=tuple(pivot), faces=("front", "back"), dens=dens, key=key)

    def animate(self, bone, sway=1.0, spin=0.0, spin_axis=1, pulse=0.0, phase=0.0, wind=1.0):
        """Tells the clip generator that a bone you added in `extra` should move: `sway` hangs it from its parent like hair or cloth (it lags behind the body, is blown back
        by running, and wobbles; 0 = none), `spin` turns it about `spin_axis` (0 x, 1 y, 2 z) at that many turns per second (a floating ring, a gear), `pulse` breathes its
        scale by that fraction, `phase` offsets the wobble (radians) so neighbours do not move in step, `wind` scales how far a run blows it back."""
        self.facts.setdefault("animate", []).append({"bone": bone, "sway": float(sway), "spin": float(spin), "axis": int(spin_axis), "pulse": float(pulse), "phase": float(phase),
                                                      "wind": float(wind)})

    # ---- output
    def finish(self):
        self.pk.finish()
        return self.pk.images()

    def cropped(self, img, glow):
        used = self.pk.height_used()
        hh = 64
        while hh < used:
            hh += 64
        return img.crop((0, 0, self.pk.W, hh)), glow.crop((0, 0, self.pk.W, hh)), hh

    def stats(self):
        n = self.m.cube_count()
        quads = sum(len(c["uv"]) for b in self.m.bones.values() for c in b["cubes"])
        return {"cubes": n, "quads": quads}

    # ---- forward kinematics (the maths of client.geo.GeoDraw, in the file frame)
    def pose_point(self, bone, p, pose):
        """Where the point p (file coordinates of the unposed model) of `bone` is under `pose` = {bone: {"rot": (x, y, z), "pos": (x, y, z), "scale": (x, y, z)}}
        (missing entries are 0 / 0 / 1)."""
        p = np.array(p, float)
        b = self.m.bones.get(bone)
        while b is not None:
            t = pose.get(b["name"])
            piv = np.array(b["pivot"], float)
            rb = b.get("rotation") or (0.0, 0.0, 0.0)
            rx, ry, rz = rb
            pos = np.zeros(3)
            sc = np.ones(3)
            if t:
                r = t.get("rot")
                if r is not None:
                    rx, ry, rz = rx + r[0], ry + r[1], rz + r[2]
                pos = np.array(t.get("pos", (0, 0, 0)), float)
                sc = np.array(t.get("scale", (1, 1, 1)), float)
            p = piv + pos + rot_matrix(rx, ry, rz) @ (sc * (p - piv))
            b = self.m.bones.get(b["parent"]) if b.get("parent") else None
        return p

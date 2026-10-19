"""Shared kit of the 1.93.0 magic rebuild generators (Spatial, Gravity, Permeation, Mirror, Slash): a GeoLite Model plus a shelf packer that gives every cube
its own box-uv rectangle and paints it face by face from a MATERIAL, an emissive glow map written beside it, and the helpers the five armour sets and the
summon models are built from (octagon rings, hollow frames, stepped spikes, mirrored left/right pieces, looping animation tracks).

    from spatial_geo_kit import Atlas, MATS, loop, sin_loop
    A = Atlas("gravity", "chest", 128, 128, seed=3)
    A.bone("root", pivot=(0, 0, 0))
    A.cube("root", (-4, 12, -3), (8, 12, 6), "plate", inflate=0.6)
    A.save()

File frame (see geo_builder.py): pixels, y UP, feet at the origin, front on -z, +x the model's left; the humanoid is 32 tall: legs 0..12, torso 12..24
(x -4..4, z -2..2), head 24..32, right arm x -8..-4 / left arm x 4..8 (y 12..24). Armour pieces are Space.PLAYER models laid over that body.

A material is a dict: kind in plate / metal / facet / glass / void / energy / wisp, ramp = five RGB tones (outline, dark, base, light, spec), and the glow
keys: glow = RGB of the emissive layer, edge = glow alpha along the face border (0..255), core = glow alpha filling the face, dots = chance of a glowing
pixel, alpha = base opacity (glass / wisp are written translucent, so they are drawn with the TRANSLUCENT layer).
Run python as `python3 -B`. Files are written under src/main/resources/assets/nusmp/ through geo_builder.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim   # noqa: E402

SHADE = {"top": 1.16, "bottom": 0.58, "north": 1.0, "south": 0.8, "west": 0.88, "east": 0.94}


def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ramp = [outline, dark, base, light, spec]
MATS = {}


def mat(name, kind, ramp, glow=(255, 255, 255), edge=0, core=0, dots=0.0, alpha=255, cell=18, rune=0):
    MATS[name] = dict(kind=kind, ramp=ramp, glow=glow, edge=edge, core=core, dots=dots, alpha=alpha, cell=cell, rune=rune)
    return name


class Atlas:
    """A Model plus a shelf packer and the painter of its box-uv rectangles."""

    def __init__(self, key, name, w, h, seed=1):
        self.key = key
        self.m = Model(key, name, w, h)
        self.w, self.h = w, h
        self.x = self.y = self.rowh = 0
        self.jobs = []
        self.shared = {}
        self.rng = random.Random(seed)
        self.anim = Anim()

    # ---------------------------------------------------------------- bones and cubes
    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def _alloc(self, cw, ch):
        if self.x + cw > self.w:
            self.x, self.y, self.rowh = 0, self.y + self.rowh, 0
        if self.y + ch > self.h:
            raise RuntimeError("atlas %s is full (%dx%d)" % (self.m.id, self.w, self.h))
        u, v = self.x, self.y
        self.x += cw
        self.rowh = max(self.rowh, ch)
        return u, v

    def cube(self, bone, origin, size, material, inflate=0.0, rot=None, share=None):
        """One painted cube. rot = (rx, ry, rz) degrees about the cube's own centre. share: cubes with the same tag, size and material reuse one uv rectangle."""
        sx, sy, sz = [max(1, int(round(s))) for s in size]
        key = (share, sx, sy, sz, material) if share else None
        if key and key in self.shared:
            u, v = self.shared[key]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, material))
            if key:
                self.shared[key] = (u, v)
        pivot = None
        if rot:
            pivot = [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=pivot)

    def box_c(self, bone, centre, size, material, inflate=0.0, rot=None, share=None):
        """The same, from the centre of the cube."""
        return self.cube(bone, (centre[0] - size[0] / 2.0, centre[1] - size[1] / 2.0, centre[2] - size[2] / 2.0), size, material, inflate, rot, share)

    # ---------------------------------------------------------------- composite shapes
    def ring(self, bone, centre, axis, radius, thick, depth, material, n=8, phase=0.0, share=None):
        """A polygon ring (n cubes) standing in the plane normal to axis ('x', 'y' or 'z'): `thick` radial, `depth` along the axis."""
        L = max(1.0, 2.0 * radius * math.tan(math.pi / n) + 1.0)
        for k in range(n):
            a = phase + 360.0 * k / n
            c, s = math.cos(math.radians(a)), math.sin(math.radians(a))
            if axis == "z":
                p, size, rot = (radius * c, radius * s, 0), (thick, L, depth), (0, 0, a)
            elif axis == "x":
                p, size, rot = (0, radius * c, radius * s), (depth, thick, L), (a, 0, 0)
            else:
                p, size, rot = (radius * c, 0, -radius * s), (thick, depth, L), (0, a, 0)
            self.box_c(bone, (centre[0] + p[0], centre[1] + p[1], centre[2] + p[2]), size, material, rot=rot,
                       share=(share + str(k % 2)) if share else None)

    def frame(self, bone, x0, y0, z0, w, h, d, t, material, share=None):
        """A hollow rectangular frame in the xy plane (front -z): four bars of thickness t and depth d."""
        self.cube(bone, (x0, y0, z0), (w, t, d), material, share=share and share + "a")
        self.cube(bone, (x0, y0 + h - t, z0), (w, t, d), material, share=share and share + "a")
        self.cube(bone, (x0, y0 + t, z0), (t, h - 2 * t, d), material, share=share and share + "b")
        self.cube(bone, (x0 + w - t, y0 + t, z0), (t, h - 2 * t, d), material, share=share and share + "b")

    def spike(self, bone, base, axis, length, width, material, layers=4, share=None, tipw=1):
        """A stepped tapering spike from `base` along axis ('+y', '-z' ...), `width` wide at the foot."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        for k in range(layers):
            w = max(tipw, int(round(width * (1.0 - k / float(layers)) ** 0.9)))
            o = [base[0] - w / 2.0, base[1] - w / 2.0, base[2] - w / 2.0]
            s = [w, w, w]
            o[a] = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            s[a] = seg
            self.cube(bone, tuple(o), tuple(s), material, share=(share + str(k)) if share else None)

    def blade(self, bone, origin, length, width, thick, material, axis="-z", layers=6, taper=0.55, share=None, curve=0.0, inflate=0.0):
        """A blade lying along `axis` (a tapering stack of slabs, `width` wide at the foot narrowing to the point; curve bends it by degrees per layer about x)."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        for k in range(layers):
            f = k / float(layers)
            w = max(1, int(round(width * (1.0 - taper * f))))
            wa = 2 if a == 1 else 1                      # the broad side of a blade: y for a blade along x or z, z for a blade along y
            o = [origin[0], origin[1], origin[2]]
            s = [thick, thick, thick]
            s[wa] = w
            s[a] = seg
            o[a] = origin[a] + (k * seg if sign > 0 else -(k + 1) * seg)
            o[wa] = origin[wa] + (width - w) / 2.0
            r = (curve * k, 0, 0) if curve else None
            self.cube(bone, tuple(o), tuple(s), material, rot=r, inflate=inflate, share=(share + str(k)) if share else None)

    # ---------------------------------------------------------------- painting
    def paint(self):
        img, glow = self.m.paint()
        px, gx = img.load(), glow.load()
        for (u, v, sx, sy, sz, material) in self.jobs:
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self._face(px, gx, x, y, w, h, material, SHADE[kind])
        return img, glow

    def _face(self, px, gx, x0, y0, w, h, material, shade):
        if w <= 0 or h <= 0:
            return
        P, rng = MATS[material], self.rng
        kind, ramp, gc = P["kind"], P["ramp"], P["glow"]
        if kind == "facet":
            n = max(1, int(round(w * h / float(P["cell"]))))
            seeds = [(rng.uniform(0, w), rng.uniform(0, h), rng.choice((-0.14, -0.05, 0.05, 0.13))) for _ in range(n)]
        for j in range(h):
            for i in range(w):
                u, v = (i + 0.5) / w, (j + 0.5) / h
                edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
                t = 0.5 + (shade - 0.9) * 1.1
                a = P["alpha"]
                lift = 0
                if kind in ("plate", "metal"):
                    t += 0.10 * (0.5 - (u + v) * 0.5)
                    if kind == "metal":                                     # brushed grain along x
                        t += 0.05 * math.sin(j * 1.9 + (i // 5) * 2.3) + rng.uniform(-0.03, 0.03)
                    else:
                        t += rng.uniform(-0.04, 0.04)
                    if w >= 4 and h >= 4:
                        if i == 0 or j == 0:
                            t += 0.16
                        elif i == w - 1 or j == h - 1:
                            t -= 0.16
                        elif w >= 8 and h >= 8 and (i == 2 or j == 2 or i == w - 3 or j == h - 3):
                            t -= 0.07
                elif kind == "facet":
                    d1 = d2 = 1e9
                    lv = 0.0
                    for (sx_, sy_, l_) in seeds:
                        d = math.hypot(i + 0.5 - sx_, j + 0.5 - sy_)
                        if d < d1:
                            d2, d1, lv = d1, d, l_
                        elif d < d2:
                            d2 = d
                    t += lv + 0.10 * (0.5 - (u + v) * 0.5)
                    if w >= 5 and h >= 5 and len(seeds) > 1 and d2 - d1 < 0.6:
                        t -= 0.25
                elif kind == "glass":
                    band = abs((u + v) - 0.9) < 0.10 or abs((u + v) - 1.25) < 0.04
                    t = 0.42 + (shade - 0.9) * 0.6 + 0.20 * (1 - v) + (0.34 if band else 0.0) + rng.uniform(-0.02, 0.02)
                    a = int(P["alpha"] * (0.75 + 0.5 * band + 0.25 * (1 - v)))
                    if edge:
                        t += 0.25
                        a = min(255, a + 80)
                elif kind == "void":
                    sw = math.sin(u * 7.0 + math.sin(v * 5.0 + rng.uniform(0, 0.4)) * 2.2)
                    t = 0.18 + (shade - 0.9) * 0.4 + 0.16 * sw * sw + (0.18 if edge else 0.0)
                elif kind == "energy":
                    r = math.hypot((u - 0.5) * 2, (v - 0.5) * 2)
                    t = clamp(1.0 - r * 0.8, 0.0, 1.0) * 0.9 + 0.08
                elif kind == "wisp":
                    r = math.hypot((u - 0.5) * 2, (v - 0.5) * 2)
                    t = 0.55 + 0.35 * math.sin(v * 9.0 + u * 3.0) + (shade - 0.9) * 0.4
                    a = int(clamp(P["alpha"] * (1.0 - 0.55 * r) * (0.7 + 0.3 * math.sin(v * 11.0)), 20, 255))
                lvl = 0 if t < 0.24 else 1 if t < 0.5 else 2 if t < 0.8 else 3
                if rng.random() < 0.02:
                    lvl = int(clamp(lvl + rng.choice((-1, 1)), 0, 3))
                col = ramp[1 + lvl]
                if kind in ("plate", "metal", "facet") and edge and w >= 4 and h >= 4 and (i == w - 1 or j == h - 1):
                    col = mix(ramp[0], ramp[1], 0.5)
                px[x0 + i, y0 + j] = col + (int(clamp(a, 0, 255)),)
                ga = 0
                if P["edge"] and edge and w >= 3 and h >= 3:
                    ga = P["edge"]
                if P["core"]:
                    ga = max(ga, int(P["core"] * (0.45 + 0.55 * t)) if kind in ("energy", "void", "wisp") else int(P["core"] * (0.3 + 0.5 * t)))
                if P["dots"] and rng.random() < P["dots"] and lvl >= 2:
                    gx[x0 + i, y0 + j] = mix(gc, (255, 255, 255), 0.5) + (230,)
                elif ga:
                    gx[x0 + i, y0 + j] = gc + (int(clamp(ga, 0, 255)),)
                if P["rune"] and w >= 6 and h >= 6 and (i + j * 3) % 7 == 0 and 1 < i < w - 2 and 1 < j < h - 2 and rng.random() < 0.5:
                    gx[x0 + i, y0 + j] = gc + (P["rune"],)

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        print("%-26s %3d cubes  %dx%d  atlas used to y=%d" % (self.m.id, self.m.cube_count(), self.w, self.h, self.y + self.rowh))
        return self.m.id


# ---------------------------------------------------------------- animation helpers
def loop(length, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop."""
    return {round(length * k / steps, 2): f(k / float(steps)) for k in range(steps + 1)}


def sin_loop(length, amp, phase=0.0, steps=8, ax=1):
    return loop(length, lambda p: tuple(amp * math.sin((p + phase) * 2 * math.pi) if i == ax else 0 for i in range(3)), steps)


def spin(length, turns=1, ax=2, sign=1, steps=4):
    """Keyframes of a steady turn about one axis: `turns` full turns over the clip (the loop closes at 360 * turns)."""
    return {round(length * k / steps, 2): tuple(360.0 * turns * sign * k / steps if i == ax else 0 for i in range(3)) for k in range(steps + 1)}


def mirror_x(origin, size, side):
    """Left (side=+1) / right (side=-1) pieces share one description written for the RIGHT side (x < 0): returns the origin for the requested side."""
    return (origin[0] if side < 0 else -(origin[0] + size[0]), origin[1], origin[2])


def mrot(rot, side):
    """Rotation of a right-side cube mirrored to the left side."""
    if rot is None or side < 0:
        return rot
    return (rot[0], -rot[1], -rot[2])


def check(*ids):
    """Runs tools/geo_check.py on the given model ids."""
    import subprocess
    here = os.path.dirname(os.path.abspath(__file__))
    r = subprocess.run([sys.executable, "-B", os.path.join(here, "geo_check.py")] + list(ids), stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    print(r.stdout.decode().rstrip())
    if r.returncode != 0:
        raise SystemExit("geo_check failed")

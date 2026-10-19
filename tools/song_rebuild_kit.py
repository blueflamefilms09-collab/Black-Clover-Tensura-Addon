"""Shared kit of the 1.93.0 rebuild of Song, Sound, Soul, Soul Corpse and Memory Magic: GeoLite models painted by pattern materials.

    from song_rebuild_kit import Atlas, MATS, loop, sin_loop
    A = Atlas("song", "armour", 256, 256, seed=3)        # key, model name, texture size
    A.bone("root", pivot=(0, 0, 0)); A.bone("helm", "root", (0, 24, 0))
    A.cube("helm", (-4.5, 24, -4.5), (9, 3, 9), "brass", inflate=0.2)
    A.save()                                               # writes geo + animation + texture + glow, prints the cube count

Why: every one of the five magics gets a set of armour pieces and summon models that are real box-UV GeoLite models with an emissive
glow map. The pieces share one painter: each cube gets its own box-UV rectangle on a shelf-packed atlas and every face is painted from a
material (a 4 step colour ramp plus a pattern: brass with engraving, speaker cones, piano keys, bone, stitched cloth, film strip, spirit
flame ...). The pattern decides which pixels also go into the glow map (strings, VU bars, flames, sprocket frames ...). Files are written by
tools/geo_builder.py under src/main/resources/assets/nusmp/ (geo/entity, animations/entity, textures/entity), so everything can be redone
by running the magic's own generator (tools/gen_<key>_rebuild_geo.py). File frame as in geo_builder: pixels, y up, feet at the origin,
the model's front on -z, +x is the model's left; a humanoid armour piece is authored on the standing player (head cube y 24..32,
body 12..24, arms at x +-4..8, legs 0..12).
Run python as `python3 -B`.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim   # noqa: E402

SHADE = {"top": 1.12, "bottom": 0.60, "north": 1.0, "south": 0.78, "west": 0.88, "east": 0.94}


def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def mix(a, b, t):
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def ramp_of(base, dark=0.55, light=1.35, outline=0.28, spec=None):
    """A five step ramp [outline, dark, base, light, spec] from one RGB int."""
    b = rgb(base)
    f = lambda k: tuple(int(clamp(c * k, 0, 255)) for c in b)
    sp = rgb(spec) if spec is not None else mix(f(light * 1.1), (255, 255, 255), 0.55)
    return [f(outline), f(dark), b, f(light), sp]


# ---------------------------------------------------------------------------------------------------------------------------------
# Materials: ramp + pattern + glow. glow = (rgb, strength 0..255). 'alpha' < 255 makes a ghostly material (use with the translucent pass).
# ---------------------------------------------------------------------------------------------------------------------------------
MATS = {}


def material(name, base, pattern="plain", glow=None, glow_strength=0, alpha=255, dark=0.55, light=1.35, spec=None, outline=0.28, **kw):
    P = dict(ramp=ramp_of(base, dark, light, outline, spec), pattern=pattern, glowc=rgb(glow if glow is not None else base),
             glow=glow_strength, alpha=alpha)
    P.update(kw)
    MATS[name] = P
    return P


# ---- pattern functions: (i, j, w, h, P, rng, face) -> (colour override or None, glow 0..255, alpha factor 0..1)
def _p_plain(i, j, w, h, P, rng, face):
    return None, 0, 1.0


def _p_plate(i, j, w, h, P, rng, face):
    g = 0
    col = None
    step = P.get("step", 8)
    if w >= 6 and h >= 6:
        if (j % step == step - 1) or (i % (step * 2) == step * 2 - 1):
            col = P["ramp"][0]
        corner = (i in (1, w - 2)) and (j in (1, h - 2))
        if corner:
            col = P["ramp"][4]
    return col, g, 1.0


def _p_brushed(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if j % 2 == 0 and rng.random() < 0.6:
        return r[3], 0, 1.0
    if j % 5 == 3 and rng.random() < 0.5:
        return r[1], 0, 1.0
    return None, 0, 1.0


def _p_engrave(i, j, w, h, P, rng, face):
    if w < 5 or h < 5:
        return None, 0, 1.0
    r = P["ramp"]
    x = i / float(max(1, w - 1))
    # two scrolls: sine curves
    for k, ph in enumerate((0.0, 2.0)):
        yy = (math.sin(x * 6.28 + ph) * 0.18 + 0.32 + 0.36 * k) * (h - 1)
        if abs(j - yy) < 0.6:
            return r[1], int(P["glow"] * 0.5), 1.0
    if j % 4 == 0 and abs(i - w / 2.0) < w * 0.25:
        return r[3], 0, 1.0
    return None, 0, 1.0


def _p_grille(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if w < 4 or h < 4:
        return None, 0, 1.0
    if 1 <= i < w - 1 and 1 <= j < h - 1 and i % 2 == 1 and j % 2 == 1:
        return r[0], P["glow"] // 2, 1.0
    return None, 0, 1.0


def _p_cone(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if w < 5 or h < 5:
        return None, 0, 1.0
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d = math.hypot((i - cx) / (w / 2.0), (j - cy) / (h / 2.0))
    if d < 0.22:
        return r[4], P["glow"], 1.0                     # dust cap
    if d < 0.9:
        k = int(d * 7)
        if k % 2 == 0:
            return r[1], 0, 1.0
        return r[3], P["glow"] // 3, 1.0
    return r[0], 0, 1.0                                  # surround


def _p_keys(i, j, w, h, P, rng, face):
    r = P["ramp"]
    black = P.get("black", (24, 22, 32))
    if i % 4 in (0, 1) and j < h * 0.65:
        return black, 0, 1.0
    if i % 4 == 3 and j >= h * 0.65:
        return r[1], 0, 1.0
    return None, 0, 1.0


def _p_strings(i, j, w, h, P, rng, face):
    if w >= 3 and i % 3 == 1:
        return mix(P["ramp"][3], P["glowc"], 0.6), P["glow"], 1.0
    return None, 0, 1.0


def _p_bars(i, j, w, h, P, rng, face):
    if w >= 3 and i % 2 == 0:
        height = 0.35 + 0.65 * ((math.sin(i * 1.7 + 1.0) * 0.5 + 0.5))
        if j >= h * (1 - height) and j % 2 == 0:
            return mix(P["glowc"], (255, 255, 255), 0.3), P["glow"], 1.0
        return P["ramp"][0], 0, 1.0
    return None, 0, 1.0


def _p_bone(i, j, w, h, P, rng, face):
    r = P["ramp"]
    # cracks: a short random walk line from a fixed seed per face
    if w >= 5 and h >= 5:
        if (i * 7 + j * 13 + w) % 23 == 0 and rng.random() < 0.7:
            return r[1], 0, 1.0
    if rng.random() < 0.03:
        return r[1], 0, 1.0
    return None, 0, 1.0


def _p_ribs(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if j % 4 == 3:
        return r[1], int(P["glow"] * 0.6), 1.0
    if j % 4 == 0:
        return r[3], 0, 1.0
    return None, 0, 1.0


def _p_cloth(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if (i + j) % 2 == 0:
        return r[1], 0, 1.0
    if (i // 2 + j // 2) % 4 == 0:
        return r[3], 0, 1.0
    return None, 0, 1.0


def _p_stitch(i, j, w, h, P, rng, face):
    r = P["ramp"]
    gc = P["glowc"]
    if w >= 4 and h >= 4:
        # a seam down the middle with cross stitches, plus a second seam near the edge
        mid = w // 2
        if i == mid:
            return r[0], 0, 1.0
        if abs(i - mid) == 1 and j % 3 == 0:
            return mix(gc, (255, 255, 255), 0.4), P["glow"], 1.0
        if j == h // 2 and i % 2 == 0:
            return r[0], 0, 1.0
    return _p_cloth(i, j, w, h, P, rng, face)


def _p_film(i, j, w, h, P, rng, face):
    r = P["ramp"]
    gc = P["glowc"]
    if h >= 6:
        if j in (1, h - 2) and i % 3 == 1:
            return (236, 232, 250), P["glow"] // 3, 1.0              # sprocket holes
        if 3 <= j <= h - 4 and (i // 5) % 2 == 0:
            return mix(gc, (255, 255, 255), 0.25), P["glow"], 1.0    # a lit frame
        if 3 <= j <= h - 4:
            return r[1], 0, 1.0
    return None, 0, 1.0


def _p_glass(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if w >= 4 and h >= 4:
        if (i * 5 + j * 3) % 11 == 0:
            return r[4], P["glow"], 1.0
        if i == 0 or j == 0 or i == w - 1 or j == h - 1:
            return mix(P["glowc"], (255, 255, 255), 0.35), P["glow"], 1.0
    return None, int(P["glow"] * 0.2), 1.0


def _p_flame(i, j, w, h, P, rng, face):
    r = P["ramp"]
    x = i / float(max(1, w - 1))
    y = 1.0 - j / float(max(1, h - 1))                       # 1 at the top
    wave = math.sin(x * 9.0 + y * 5.0) * 0.12 + math.sin(x * 4.0 - y * 3.0) * 0.1
    heat = clamp(0.95 - y * 0.75 + wave, 0, 1)
    c = mix(r[4], r[2], clamp(1 - heat, 0, 1))
    return c, int(P["glow"] * (0.45 + 0.55 * heat)), clamp(0.35 + 0.65 * heat, 0, 1)


def _p_veins(i, j, w, h, P, rng, face):
    r = P["ramp"]
    if w >= 4 and h >= 4:
        a = math.sin(i * 0.9 + j * 0.35) + math.sin(j * 0.8 - i * 0.2)
        if abs(a) < 0.16:
            return mix(P["glowc"], (255, 255, 255), 0.3), P["glow"], 1.0
    return None, 0, 1.0


def _p_wisp(i, j, w, h, P, rng, face):
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
    a = clamp(1.15 - d * 0.7, 0.35, 1)
    return None, int(P["glow"] * a), a


def _p_core(i, j, w, h, P, rng, face):
    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
    d = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
    t = clamp(1.0 - d * 0.85, 0, 1)
    r = P["ramp"]
    c = r[4] if t > 0.6 else r[3] if t > 0.3 else r[2]
    return c, int(clamp(150 + 105 * t, 0, 255)), 1.0


def _p_runes(i, j, w, h, P, rng, face):
    if w >= 5 and h >= 5 and 1 <= i < w - 1 and 1 <= j < h - 1:
        # blocky glyph noise on a 3x3 lattice
        gx, gy = i // 3, j // 3
        hsh = (gx * 73856093 ^ gy * 19349663 ^ (w * 83492791)) & 255
        if hsh % 3 == 0 and (i % 3 != 2 or j % 3 != 2):
            return mix(P["glowc"], (255, 255, 255), 0.25), P["glow"], 1.0
    return None, 0, 1.0


def _p_emit(i, j, w, h, P, rng, face):
    """Every pixel glows (strings, gems, flames' cores): the base colour is the glow colour."""
    return None, P["glow"], 1.0


PATTERNS = {"emit": _p_emit, "plain": _p_plain, "plate": _p_plate, "brushed": _p_brushed, "engrave": _p_engrave, "grille": _p_grille, "cone": _p_cone,
            "keys": _p_keys, "strings": _p_strings, "bars": _p_bars, "bone": _p_bone, "ribs": _p_ribs, "cloth": _p_cloth,
            "stitch": _p_stitch, "film": _p_film, "glass": _p_glass, "flame": _p_flame, "veins": _p_veins, "wisp": _p_wisp,
            "core": _p_core, "runes": _p_runes}


class Atlas:
    """A Model plus a shelf packer and the painter of its box-uv rectangles."""

    def __init__(self, key, name, w=128, h=128, seed=1):
        self.m = Model(key, name, w, h)
        self.key, self.w, self.h = key, w, h
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

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, share=None, pivot=None, mirror=False):
        """One painted cube. rot = (rx, ry, rz) degrees about `pivot` (default the cube centre). Sizes are whole pixels (box uv)."""
        sx, sy, sz = [int(s) for s in size]
        key = (share, sx, sy, sz, mat) if share else None
        if key and key in self.shared:
            u, v = self.shared[key]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, mat))
            if key:
                self.shared[key] = (u, v)
        piv = None
        if rot:
            piv = list(pivot) if pivot is not None else [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=piv, mirror=mirror)

    def mcube(self, bone_r, bone_l, origin, size, mat, **kw):
        """A cube and its mirror image across x = 0 (right piece on bone_r, left piece on bone_l)."""
        sx = size[0]
        self.cube(bone_r, origin, size, mat, **kw)
        rot = kw.get("rot")
        kw2 = dict(kw)
        if rot:
            kw2["rot"] = (rot[0], -rot[1], -rot[2])
        if kw.get("pivot") is not None:
            p = kw["pivot"]
            kw2["pivot"] = (-p[0], p[1], p[2])
        self.cube(bone_l, (-origin[0] - sx, origin[1], origin[2]), size, mat, **kw2)

    def spike(self, bone, base, axis, length, width, mat, layers=3, tip=1, share=None):
        """A stepped taper from `base` along axis ('+y', '-z' ...): `layers` cubes, each narrower, ending at `tip` px. Returns the cube count."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        n = 0
        for k in range(layers):
            f = k / float(layers)
            w = max(tip, int(round(width * (1.0 - f) ** 0.9)))
            lo = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            o = [base[i] - w / 2.0 for i in range(3)]
            s = [w, w, w]
            o[a] = lo
            s[a] = seg
            self.cube(bone, tuple(o), tuple(s), mat, share=(share + str(k)) if share else None)
            n += 1
        return n

    def ring(self, bone, centre, radius, thick, height, n, mat, plane="xz", start=0.0, share=None):
        """A ring of n cubes round `centre` in the xz plane (horizontal) or the xy plane (upright, facing -z). thick = radial depth px."""
        cx, cy, cz = centre
        step = 2 * math.pi / n
        chord = max(1, int(round(2 * radius * math.sin(step / 2) + 0.5)))
        for k in range(n):
            a = start + k * step
            if plane == "xz":
                px, pz = cx + math.cos(a) * radius, cz + math.sin(a) * radius
                o = (px - chord / 2.0, cy, pz - thick / 2.0)
                self.cube(bone, o, (chord, height, int(thick)), mat, rot=(0, -math.degrees(a) + 90, 0), pivot=(px, cy + height / 2.0, pz), share=share)
            else:
                px, py = cx + math.cos(a) * radius, cy + math.sin(a) * radius
                o = (px - chord / 2.0, py - height / 2.0, cz - thick / 2.0)
                self.cube(bone, o, (chord, int(height), int(thick)), mat, rot=(0, 0, math.degrees(a) + 90), pivot=(px, py, cz), share=share)

    # ---------------------------------------------------------------- painting
    def paint(self):
        img, glow = self.m.paint()
        px, gx = img.load(), glow.load()
        for (u, v, sx, sy, sz, mat) in self.jobs:
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self._face(px, gx, x, y, w, h, mat, kind)
        return img, glow

    def _face(self, px, gx, x0, y0, w, h, matname, kind):
        if w <= 0 or h <= 0:
            return
        P, rng = MATS[matname], self.rng
        ramp = P["ramp"]
        shade = SHADE[kind]
        pat = PATTERNS[P["pattern"]]
        base_t = 0.5 + (shade - 0.9) * 1.1
        gc = P["glowc"]
        for j in range(h):
            for i in range(w):
                grad = 0.12 * (0.5 - (i / float(max(1, w)) * 0.4 + j / float(max(1, h)) * 0.6))
                t = base_t + grad + rng.uniform(-0.05, 0.05)
                if w >= 4 and h >= 4:
                    if i == 0 or j == 0:
                        t += 0.12
                    elif i == w - 1 or j == h - 1:
                        t -= 0.14
                lvl = 0 if t < 0.28 else 1 if t < 0.52 else 2 if t < 0.78 else 3
                col = ramp[1 + lvl]
                over, g, af = pat(i, j, w, h, P, rng, kind)
                if over is not None:
                    k = 0.82 + 0.18 * shade
                    col = tuple(int(c * k) for c in over)
                a = int(clamp(P["alpha"] * af, 0, 255))
                px[x0 + i, y0 + j] = col + (a,)
                g = int(clamp(g + (P["glow"] * 0.0), 0, 255))
                if g > 0:
                    gx[x0 + i, y0 + j] = tuple(int(c * 0.75 + 255 * 0.25 * (g / 255.0)) for c in gc) + (g,)

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        print("%-26s %3d cubes  %dx%d  atlas used to y=%d" % (self.m.id, self.m.cube_count(), self.w, self.h, self.y + self.rowh))
        return self.m.id


def loop(clip_len, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop (first == last)."""
    return {round(clip_len * k / steps, 2): f(k / float(steps)) for k in range(steps + 1)}


def sin_loop(clip_len, amp, phase=0.0, steps=8, ax=1):
    """Rotation keyframes: amp degrees about axis ax (0 x, 1 y, 2 z) following a sine."""
    return loop(clip_len, lambda p: tuple(amp * math.sin((p + phase) * 2 * math.pi) if i == ax else 0 for i in range(3)), steps)


def bob_loop(clip_len, amp, phase=0.0, steps=8):
    """Position keyframes: a vertical bob of amp pixels."""
    return loop(clip_len, lambda p: (0, amp * math.sin((p + phase) * 2 * math.pi), 0), steps)


def spin_loop(clip_len, turns=1, ax=1, steps=8):
    return loop(clip_len, lambda p: tuple(360.0 * turns * p if i == ax else 0 for i in range(3)), steps)


def check(*ids):
    """Runs tools/geo_check.py on the given model ids and exits non-zero on a problem."""
    import subprocess
    r = subprocess.run([sys.executable, "-B", os.path.join(os.path.dirname(os.path.abspath(__file__)), "geo_check.py")] + list(ids))
    if r.returncode:
        sys.exit(r.returncode)

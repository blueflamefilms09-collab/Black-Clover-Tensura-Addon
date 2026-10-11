"""Copper Magic geo models: the Red Shine Mace, the coil golem and the copper plating pieces of the player render layer.

    python3 -B tools/gen_copper_geo.py            # writes every model, animation and texture, prints the cube counts and runs tools/geo_check.py
    python3 -B tools/geo_check.py copper_mace copper_coil_golem copper_pauldrons copper_crown

Models (assets/nusmp/geo/entity/copper_<name>.geo.json, animations/entity/..., textures/entity/copper_<name>.png + _glow.png):
  mace         the Red Shine Mace: a huge polearm mace standing upright (grip at y = 24 px), a red-hot flanged head of eight copper flanges round a glowing
               core, copper bands and wire winding on a dark shaft, a collar with spikes, a spiked butt, a cloth tassel and sparks circling the head.
               clips: idle (hover, the rings and sparks turn), swing (draw back, strike forward, recover), slam (raise high, hammer down, recover)
  coil_golem   a squat golem of riveted copper plates: thick legs, a wide chest with a glowing core, long arms ending in electrode fists, a small head with a
               glowing visor, two coils on the back, two on the shoulders and two antennae; zig-zag arcs of current jump between the coil tips.
               clips: idle (breathing, the arcs flicker), walk, shock (arms thrown forward, every arc lit, a ring of sparks expands)
  pauldrons    (Space.PLAYER) stacked copper shoulder plates with rivets and a verdigris edge, drawn on the torso's pose
  crown        (Space.PLAYER) a copper band on the head with a crown of seven coils whose tips are joined by arcs (style 1 of the aura); clip: idle
  textures/aura/copper_plates(.png, _glow.png): the skin-layout plating the aura lays over the posed player model (riveted plates, verdigris in the seams)
  textures/aura/copper_arc.png: the soft line the camera-facing lightning of the aura and the props is drawn with

The look follows the owner's Copper stills (a red-gold polearm with a glowing head): brushed copper plates, a one pixel bevel round every plate with rivets
in the corners, green verdigris creeping out of the seams, hot red-orange metal for the heads and the cores, and a faint electric crackle in the glow maps.
Every cube gets its own box-uv rectangle on a shelf-packed atlas and is painted face by face (tops lit, bottoms dark). File frame: y up, feet at the origin,
the front on -z (see tools/geo_builder.py).
"""
import math
import os
import random
import subprocess
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT   # noqa: E402

KEY = "copper"
SHADE = {"top": 1.12, "bottom": 0.60, "north": 1.0, "south": 0.78, "west": 0.88, "east": 0.94}


# ====================================================================================================================
# painting helpers
# ====================================================================================================================
def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


def hsh(x, y, s):
    n = (x * 374761393 + y * 668265263 + s * 982451653) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def vnoise(x, y, s):
    xi, yi = int(math.floor(x)), int(math.floor(y))
    xf, yf = x - xi, y - yi
    u, v = xf * xf * (3 - 2 * xf), yf * yf * (3 - 2 * yf)
    a, b, c, d = hsh(xi, yi, s), hsh(xi + 1, yi, s), hsh(xi, yi + 1, s), hsh(xi + 1, yi + 1, s)
    return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v


def fbm(x, y, s):
    return 0.6 * vnoise(x, y, s) + 0.3 * vnoise(x * 2.1, y * 2.1, s + 7) + 0.1 * vnoise(x * 4.3, y * 4.3, s + 13)


def ramp(stops, t):
    """stops = [(r,g,b) ...] evenly spaced; t 0..1 -> interpolated colour."""
    t = clamp(t, 0.0, 1.0) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    f = t - i
    a, b = stops[i], stops[i + 1]
    return (int(a[0] + (b[0] - a[0]) * f), int(a[1] + (b[1] - a[1]) * f), int(a[2] + (b[2] - a[2]) * f))


COPPER = [(52, 22, 14), (110, 46, 24), (176, 84, 42), (226, 134, 74), (250, 190, 128)]
COPPER_DARK = [(30, 18, 14), (72, 38, 24), (118, 66, 38), (162, 100, 58), (206, 150, 100)]
VERD = [(26, 74, 62), (46, 120, 98), (82, 168, 134), (140, 208, 170)]
IRON = [(18, 20, 26), (44, 48, 60), (82, 88, 104), (128, 136, 152), (176, 184, 198)]
HOT = [(120, 14, 10), (200, 40, 16), (250, 100, 24), (255, 176, 60), (255, 244, 190)]
LEATHER = [(30, 12, 12), (66, 24, 22), (104, 40, 32), (146, 66, 48)]
CLOTH = [(70, 10, 14), (130, 22, 24), (186, 44, 36), (226, 84, 56)]
CERAMIC = [(70, 86, 84), (130, 150, 144), (190, 206, 196), (234, 240, 228)]


def paint_px(mat, i, j, w, h, shade, gx, gy):
    """One pixel of one face of one material: i, j inside the face (w x h), gx, gy absolute texture position. Returns (rgb, glow rgba or None)."""
    de = min(i, j, w - 1 - i, h - 1 - j)
    big = w >= 5 and h >= 5
    if mat in ("plate", "plate_dark", "plate_verd"):
        st = COPPER if mat == "plate" else COPPER_DARK
        n = fbm(gx * 0.33, gy * 0.33, 3)
        brush = vnoise(gx * 0.12, gy * 1.7, 5)
        t = 0.46 + (n - 0.5) * 0.55 + (brush - 0.5) * 0.24 + (shade - 0.9) * 0.9
        if big:
            if i == 0 or j == 0:
                t += 0.22
            elif i == w - 1 or j == h - 1:
                t -= 0.26
            elif i == 1 or j == 1:
                t += 0.05
            elif i == w - 2 or j == h - 2:
                t -= 0.10
        rivet = False
        if w >= 8 and h >= 8:
            for (rx, ry) in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
                if i == rx and j == ry:
                    t = 0.98
                    rivet = True
                elif (i == rx + 1 and j == ry + 1) or (i == rx + 1 and j == ry) or (i == rx and j == ry + 1):
                    t -= 0.30
        col = ramp(st, t)
        gl = None
        # verdigris creeping out of the seams and the lower edges
        vg = fbm(gx * 0.55, gy * 0.55, 9)
        drip = vnoise(gx * 1.3, gy * 0.12, 11)
        grow = 0.30 if mat == "plate_verd" else 0.0
        near = de <= 1 and vg > 0.36 - grow
        bloom = (de <= 3 and vg > 0.66 - 0.05 * de - grow) or (h > 6 and j >= h - 4 and drip > 0.72 - grow - 0.04 * (j - (h - 4)) and vg > 0.4)
        if big and (near or bloom):
            col = ramp(VERD, 0.25 + 0.7 * (vg - 0.3) + (shade - 0.9) * 0.6 + (0.1 if near else 0.0))
        elif big and de <= 1 and hsh(gx, gy, 21) > 0.9:
            gl = (150, 220, 255, 90)                      # the faint electric crackle in the seams
        if rivet:
            gl = (255, 150, 70, 70)
        return col, gl
    if mat == "hot":
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
        r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
        n = fbm(gx * 0.45, gy * 0.45, 17)
        t = clamp(0.95 - r * 0.55 + (n - 0.5) * 0.7 + (shade - 0.9) * 0.4, 0.0, 1.0)
        return ramp(HOT, t), (255, int(110 + 100 * t), int(30 + 60 * t), int(150 + 105 * t))
    if mat == "wire":
        row = (j % 2 == 0)
        cyl = 1.0 - abs((i + 0.5) / max(1.0, w) - 0.4) * 1.5
        t = (0.62 if row else 0.22) + 0.22 * cyl + (shade - 0.9) * 0.8 + (vnoise(gx * 0.5, gy * 0.5, 23) - 0.5) * 0.18
        return ramp(COPPER, t), ((255, 150, 80, 44) if row and cyl > 0.6 else None)
    if mat == "iron":
        n = fbm(gx * 0.4, gy * 0.4, 29)
        t = 0.42 + (n - 0.5) * 0.5 + (shade - 0.9) * 0.9
        if big and (i == 0 or j == 0):
            t += 0.2
        elif big and (i == w - 1 or j == h - 1):
            t -= 0.22
        if w >= 8 and h >= 8 and ((i, j) in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3))):
            t = 0.95
        return ramp(IRON, t), None
    if mat == "leather":
        diag = ((i + j) % 4 < 2)
        t = (0.5 if diag else 0.25) + (vnoise(gx * 0.6, gy * 0.6, 31) - 0.5) * 0.3 + (shade - 0.9) * 0.7
        return ramp(LEATHER, t), None
    if mat == "cloth":
        t = 0.45 + (vnoise(gx * 0.5, gy * 0.9, 37) - 0.5) * 0.5 + (shade - 0.9) * 0.8 + (0.12 if j % 3 == 0 else 0.0)
        return ramp(CLOTH, t), ((255, 90, 50, 60) if j < 2 else None)
    if mat == "ceramic":
        t = 0.55 + (fbm(gx * 0.4, gy * 0.4, 41) - 0.5) * 0.4 + (shade - 0.9) * 0.9 + (0.18 if j % 3 == 0 else 0.0)
        return ramp(CERAMIC, t), None
    if mat == "verd":
        vg = fbm(gx * 0.5, gy * 0.5, 43)
        return ramp(VERD, 0.3 + 0.7 * vg + (shade - 0.9) * 0.6), None
    if mat == "spark":
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
        r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
        t = clamp(1.0 - r * 0.6, 0.0, 1.0)
        return (int(170 + 85 * t), int(214 + 41 * t), 255), (150 + int(60 * t), 215 + int(30 * t), 255, 255)
    if mat == "arc":
        return (214, 236, 255), (170, 224, 255, 255)
    raise KeyError(mat)


class Atlas:
    """A Model plus a shelf packer and the painter of its box-uv rectangles."""

    def __init__(self, name, w, h, seed=1):
        self.m = Model(KEY, name, w, h)
        self.w, self.h = w, h
        self.x = self.y = self.rowh = 0
        self.jobs = []
        self.shared = {}
        self.rng = random.Random(seed)
        self.anim = Anim()

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

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, share=None, pv=None):
        """One painted cube. rot = (rx, ry, rz) turns it about its own centre (or about pv). share = a tag: cubes with the same tag, size and
        material reuse one uv rectangle. Sizes are whole pixels (box uv)."""
        sx, sy, sz = [int(s) for s in size]
        key = (share, sx, sy, sz, mat) if share else None
        if key and key in self.shared:
            u, v = self.shared[key]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, mat))
            if key:
                self.shared[key] = (u, v)
        pivot = None
        if rot:
            pivot = list(pv) if pv else [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=pivot)

    def spike(self, bone, base, axis, length, width, mat, layers=4, tip=None, share=None):
        """A stepped taper from the point `base` along axis ('+y', '-y', '-z' ...), `width` wide at the foot; the last layer may be another material."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        n = 0
        for k in range(layers):
            f = k / float(layers)
            w = max(1, int(round(width * (1.0 - f) ** 0.9)))
            lo = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            o, s = [0, 0, 0], [w, w, w]
            for i in range(3):
                o[i] = base[i] - w / 2.0
            o[a] = lo
            s[a] = seg
            m = tip if (tip and k == layers - 1) else mat
            self.cube(bone, tuple(o), tuple(s), m, share=(share + "%d" % k) if share else None)
            n += 1
        return n

    def coil(self, bone, x, y, z, height, width, tip=3):
        """A little tesla coil standing on y: a ceramic post, wire windings, a base and a cap plate, a spark terminal. Returns the tip position."""
        c = self.cube
        c(bone, (x - 1, y, z - 1), (2, int(height), 2), "ceramic", share="post%d" % int(height))
        k = 0
        yy = y + 1.0
        while yy + 1.5 < y + height - 1:
            c(bone, (x - width / 2.0, yy, z - width / 2.0), (int(width), 1, int(width)), "wire", rot=(0, 45, 0) if k % 2 else None, share="wind%d%d" % (int(width), k % 2))
            yy += 1.6
            k += 1
        c(bone, (x - width / 2.0 - 1, y - 1, z - width / 2.0 - 1), (int(width) + 2, 1, int(width) + 2), "plate", share="cbase%d" % int(width))
        c(bone, (x - width / 2.0 - 0.5, y + height - 1, z - width / 2.0 - 0.5), (int(width) + 1, 1, int(width) + 1), "plate", share="ccap%d" % int(width))
        c(bone, (x - tip / 2.0, y + height, z - tip / 2.0), (tip, tip, tip), "spark", share="term%d" % tip)
        c(bone, (x - tip / 2.0, y + height, z - tip / 2.0), (tip, tip, tip), "spark", rot=(45, 0, 45), share="term%d" % tip)
        return (x, y + height + tip / 2.0, z)

    def arc(self, bone, a, b, step=1.4, jit=1.1, seed=1):
        """A zig-zag of 1 px cubes from point a to point b (a staircase of lightning). Returns the cube count."""
        d = [b[i] - a[i] for i in range(3)]
        ln = math.sqrt(sum(v * v for v in d))
        steps = max(2, int(ln / step))
        rnd = random.Random(seed)
        n = 0
        for s in range(steps + 1):
            f = s / float(steps)
            amp = jit * math.sin(math.pi * f) * (1 if s % 2 else -1)
            p = [a[i] + d[i] * f for i in range(3)]
            # a jitter on every axis the line does not mostly follow
            axis = max(range(3), key=lambda k: abs(d[k]))
            for k in range(3):
                if k != axis:
                    p[k] += amp * rnd.uniform(0.4, 1.0) * (1 if rnd.random() < 0.5 else -1)
            self.cube(bone, (round(p[0] - 0.5, 2), round(p[1] - 0.5, 2), round(p[2] - 0.5, 2)), (1, 1, 1), "arc", share="arcpx")
            n += 1
        return n

    def paint(self):
        img, glow = self.m.paint()
        px, gx = img.load(), glow.load()
        for (u, v, sx, sy, sz, mat) in self.jobs:
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self.face(px, gx, x, y, w, h, mat, SHADE[kind])
        return img, glow

    def face(self, px, gx, x0, y0, w, h, mat, shade):
        if w <= 0 or h <= 0:
            return
        for j in range(h):
            for i in range(w):
                col, gl = paint_px(mat, i, j, w, h, shade, x0 + i, y0 + j)
                px[x0 + i, y0 + j] = tuple(int(clamp(c, 0, 255)) for c in col) + (255,)
                if gl:
                    gx[x0 + i, y0 + j] = gl

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        print("%-24s %3d cubes  %dx%d  atlas used to y=%d" % (self.m.id, self.m.cube_count(), self.w, self.h, self.y + self.rowh))
        return self.m.id


def loop(clip_len, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop."""
    return {round(clip_len * k / steps, 2): f(k / float(steps)) for k in range(steps + 1)}


def flicker(length, pattern, on=1.0):
    """Scale keyframes of an arc: pattern is a list of 0/1 per slot over the clip (slot k covers length/len(pattern)); the loop closes."""
    n = len(pattern)
    frames = {}
    for k, v in enumerate(pattern):
        t0 = length * k / n
        s = on if v else 0.0
        frames[round(t0, 2)] = (s, s, s)
        frames[round(t0 + length / n * 0.9, 2)] = (s, s, s)
    frames[round(length, 2)] = frames[0.0]
    return frames


def vsub(a, b):
    return tuple(a[i] - b[i] for i in range(3))


# ====================================================================================================================
# MACE: the Red Shine Mace
# ====================================================================================================================
def build_mace():
    A = Atlas("mace", 256, 256, seed=7)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("body", "root", (0, 24, 0))
    A.bone("head", "body", (0, 70, 0))
    A.bone("spin", "head", (0, 70, 0))
    A.bone("sparks", "head", (0, 70, 0))
    A.bone("tassel", "body", (0, 56, 0))
    c = A.cube
    # ---- the shaft: dark bronze, an octagon of two square columns
    c("body", (-2, -6, -2), (4, 64, 4), "plate_dark", share="shaft")
    c("body", (-2, -6, -2), (4, 64, 4), "plate_dark", rot=(0, 45, 0), share="shaft")
    for y, tag in ((-4, "b0"), (7, "b1"), (31, "b2"), (43, "b3"), (53, "b4")):
        c("body", (-3.5, y, -3.5), (7, 2, 7), "plate", inflate=0.1, share="band")
        c("body", (-3.5, y, -3.5), (7, 2, 7), "plate", inflate=0.1, rot=(0, 45, 0), share="band")
    # the grip wrap and the wire winding below the head
    c("body", (-2, 12, -2), (4, 18, 4), "leather", inflate=0.7)
    for k in range(7):
        c("body", (-3, 33.4 + k * 1.2, -3), (6, 1, 6), "wire", rot=(0, k * 22.5, 0), share="ww%d" % (k % 2))
    for k in range(5):
        c("body", (-3, 0.4 + k * 1.2, -3), (6, 1, 6), "wire", rot=(0, k * 22.5 + 10, 0), share="wl%d" % (k % 2))
    # four thin strips of copper inlay along the shaft (north / south / west / east faces)
    for (ox, oz, sx, sz) in ((-0.5, -2.7, 1, 1), (-0.5, 1.7, 1, 1), (-2.7, -0.5, 1, 1), (1.7, -0.5, 1, 1)):
        c("body", (ox, 46, oz), (sx, 7, sz), "hot", share="inlay")
    # ---- the butt: a pommel ring and a spiked cap pointing down
    c("body", (-4, -9, -4), (8, 3, 8), "plate", inflate=0.2, share="pom")
    c("body", (-4, -9, -4), (8, 3, 8), "plate", inflate=0.2, rot=(0, 45, 0), share="pom")
    A.spike("body", (0, -9, 0), "-y", 9, 6, "plate", layers=3, tip="hot", share="butt")
    # ---- the collar: a flared double plate and four spikes that sweep down and out
    c("body", (-6, 56, -6), (12, 3, 12), "plate", inflate=0.2, share="col")
    c("body", (-6, 56, -6), (12, 3, 12), "plate", inflate=0.2, rot=(0, 45, 0), share="col")
    c("body", (-4.5, 59, -4.5), (9, 2, 9), "plate_dark", share="col2")
    c("body", (-4.5, 59, -4.5), (9, 2, 9), "plate_dark", rot=(0, 45, 0), share="col2")
    for k in range(4):
        A.bone("cs%d" % k, "body", (0, 57, 0), rotation=(0, k * 90.0, 0))
        c("cs%d" % k, (-1, 50, -9), (2, 7, 2), "plate", rot=(-28, 0, 0), share="csp")
        c("cs%d" % k, (-0.5, 49, -10.4), (1, 2, 1), "hot", rot=(-28, 0, 0), share="cst")
    # ---- the head: a hot core, eight flanges round it, two rings, a spike on top
    c("head", (-6, 62, -6), (12, 16, 12), "hot", share="core")
    c("head", (-6, 62, -6), (12, 16, 12), "hot", rot=(0, 45, 0), share="core")
    for k in range(8):
        n = "fl%d" % k
        A.bone(n, "head", (0, 70, 0), rotation=(0, k * 45.0, 0))
        long_ = (k % 2 == 0)
        h = 16 if long_ else 12
        y0 = 62 if long_ else 64
        c(n, (-2.5, y0, -10.5), (5, h, 5), "plate", inflate=0.2, share="fl%d" % h)
        c(n, (-1.5, y0 + 1, -11.4), (3, h - 2, 1), "hot", share="fe%d" % h)
        c(n, (-3, y0 + h - 0.2, -11), (6, 2, 6), "plate_dark", share="fc%d" % h)
        c(n, (-1, y0 - 2, -9.5), (2, 2, 3), "plate", share="ff")
    for y, tag in ((78, "t"), (60, "u")):
        c("spin", (-7.5, y, -7.5), (15, 2, 15), "plate", inflate=0.2, share="ring" + tag)
        c("spin", (-7.5, y, -7.5), (15, 2, 15), "plate", inflate=0.2, rot=(0, 45, 0), share="ring" + tag)
        for k in range(4):
            a = math.radians(k * 90 + 45)
            c("spin", (round(10.3 * math.cos(a) - 1, 2), y - 0.2, round(10.3 * math.sin(a) - 1, 2)), (2, 2, 2), "hot", share="stud" + tag)
    A.spike("head", (0, 80, 0), "+y", 14, 8, "plate", layers=4, tip="hot", share="top")
    c("head", (-4.5, 80, -4.5), (9, 2, 9), "plate_verd", inflate=0.1, share="topc")
    # ---- sparks circling the head
    for k in range(6):
        a = math.radians(k * 60)
        r = 13 + (k % 2) * 3
        c("sparks", (round(r * math.cos(a) - 1, 2), 66 + (k % 3) * 5, round(r * math.sin(a) - 1, 2)), (2, 2, 2), "spark", rot=(45, 0, 45), share="sp")
    # ---- a three-strand cloth tassel hanging from the collar
    for k, x in enumerate((-3, 0, 3)):
        c("tassel", (x - 1, 44 - k % 2 * 2, 3.6), (2, 12, 1), "cloth", share="ts%d" % (k % 2))
        c("tassel", (x - 1, 42 - k % 2 * 2, 3.4), (2, 2, 2), "plate", share="tt")
    # ---- animations
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    a.pos("idle", "body", loop(3.0, lambda p: (0, 2.2 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "body", loop(3.0, lambda p: (2.0 * math.sin(p * 2 * math.pi), 3 * math.sin(p * 2 * math.pi + 1.0), 1.5 * math.cos(p * 2 * math.pi))))
    a.rot("idle", "spin", {0.0: (0, 0, 0), 0.75: (0, 90, 0), 1.5: (0, 180, 0), 2.25: (0, 270, 0), 3.0: (0, 360, 0)})
    a.rot("idle", "sparks", {0.0: (0, 0, 0), 0.75: (0, -120, 0), 1.5: (0, -240, 0), 2.25: (0, -360, 0), 3.0: (0, -480, 0)})
    a.rot("idle", "tassel", loop(3.0, lambda p: (6 * math.sin(p * 2 * math.pi), 0, 4 * math.cos(p * 2 * math.pi))))
    a.scale("idle", "head", loop(3.0, lambda p: (1 + 0.035 * math.sin(p * 4 * math.pi),) * 3))
    # swing: draw the head back (0 .. 0.3), strike through (0.3 .. 0.45), follow through, recover
    a.clip("swing", length=1.0, loop=True)
    a.rot("swing", "body", {0.0: (0, 0, 0), 0.3: (-28, 0, 4), 0.45: (62, 0, -3), 0.6: (52, 0, -2), 1.0: (0, 0, 0)})
    a.pos("swing", "body", {0.0: (0, 0, 0), 0.3: (0, 2, 10), 0.45: (0, -3, -18), 0.6: (0, -3, -14), 1.0: (0, 0, 0)})
    a.rot("swing", "spin", {0.0: (0, 0, 0), 0.45: (0, 360, 0), 1.0: (0, 720, 0)})
    a.rot("swing", "sparks", {0.0: (0, 0, 0), 0.45: (0, -540, 0), 1.0: (0, -1080, 0)})
    a.rot("swing", "tassel", {0.0: (0, 0, 0), 0.3: (-20, 0, 0), 0.45: (40, 0, 0), 0.7: (-10, 0, 0), 1.0: (0, 0, 0)})
    a.scale("swing", "head", {0.0: (1, 1, 1), 0.3: (1.05, 1.05, 1.05), 0.45: (1.25, 1.25, 1.25), 0.7: (1.1, 1.1, 1.1), 1.0: (1, 1, 1)})
    # slam: raise it over the shoulder and tip it back, hammer it straight down, rest on the ground, recover
    a.clip("slam", length=1.0, loop=True)
    a.rot("slam", "body", {0.0: (0, 0, 0), 0.35: (-62, 0, 0), 0.5: (82, 0, 0), 0.68: (78, 0, 0), 1.0: (0, 0, 0)})
    a.pos("slam", "body", {0.0: (0, 0, 0), 0.35: (0, 16, 14), 0.5: (0, -8, -24), 0.68: (0, -8, -22), 1.0: (0, 0, 0)})
    a.rot("slam", "spin", {0.0: (0, 0, 0), 0.5: (0, 540, 0), 1.0: (0, 900, 0)})
    a.rot("slam", "sparks", {0.0: (0, 0, 0), 0.5: (0, -720, 0), 1.0: (0, -1080, 0)})
    a.rot("slam", "tassel", {0.0: (0, 0, 0), 0.35: (-30, 0, 0), 0.5: (60, 0, 0), 0.8: (-14, 0, 0), 1.0: (0, 0, 0)})
    a.scale("slam", "head", {0.0: (1, 1, 1), 0.35: (1.1, 1.1, 1.1), 0.5: (1.45, 1.45, 1.45), 0.7: (1.15, 1.15, 1.15), 1.0: (1, 1, 1)})
    return A.save()


# ====================================================================================================================
# COIL GOLEM
# ====================================================================================================================
def build_coil_golem():
    A = Atlas("coil_golem", 256, 256, seed=13)
    A.bone("root", pivot=(0, 0, 0))
    A.bone("body", "root", (0, 13, 0))
    A.bone("torso", "body", (0, 15, 0))
    A.bone("head", "torso", (0, 29, -1))
    c = A.cube
    # ---- legs: thigh block with a wire coil round it, shin plate, a wide flat foot
    for sg, n in ((-1, "R"), (1, "L")):
        A.bone("leg" + n, "root", (sg * 6, 13, 0))
        A.bone("shin" + n, "leg" + n, (sg * 6, 6, 0))
        x0 = sg * 6 - 4
        c("leg" + n, (x0, 6, -4), (8, 8, 8), "plate_dark", inflate=0.3, share="thigh")
        for k in range(3):
            c("leg" + n, (x0 - 0.5, 7 + k * 2.4, -4.5), (9, 1, 9), "wire", share="lw%d" % (k % 2))
        c("leg" + n, (x0 + 1, 12, -5), (6, 3, 2), "plate", share="knee")
        c("shin" + n, (x0 - 0.5, 1, -4.5), (9, 6, 9), "plate", inflate=0.2, share="shin")
        c("shin" + n, (x0 + 1, 2, -5.4), (7, 4, 1), "plate_verd", share="shinp")
        c("shin" + n, (x0 - 1, 0, -7), (10, 2, 12), "iron", share="foot")
        c("shin" + n, (x0 + 1, 2, -7), (6, 2, 2), "plate", share="toe")
        c("shin" + n, (x0 + 3, 0.2, -7.4), (2, 1, 1), "hot", share="toeh")
    # ---- pelvis and belt coil
    c("body", (-8, 11, -5), (16, 4, 10), "iron", share="pelvis")
    c("body", (-9.5, 13.5, -6.5), (19, 2, 13), "wire", share="belt")
    c("body", (-9.5, 15.5, -6.5), (19, 1, 13), "plate", share="beltp")
    for k in range(4):
        a = math.radians(45 + k * 90)
        c("body", (round(9.5 * math.cos(a) * 1.0 - 1, 2), 13.5, round(7 * math.sin(a) - 1, 2)), (2, 2, 2), "hot", share="belth")
    # ---- torso: the copper chest, a row of plates over it, the glowing core, the power pack on the back
    c("torso", (-10, 15, -6), (20, 14, 12), "plate_dark", inflate=0.2, share="chest")
    c("torso", (-9.5, 22, -7.4), (9, 6, 2), "plate", share="cp1")
    c("torso", (0.5, 22, -7.4), (9, 6, 2), "plate", share="cp1")
    c("torso", (-9.5, 16, -7.4), (6, 5, 2), "plate_verd", share="cp2")
    c("torso", (3.5, 16, -7.4), (6, 5, 2), "plate_verd", share="cp2")
    c("torso", (-3, 16, -7.8), (6, 5, 2), "iron", share="cslot")
    c("torso", (-2, 17, -8.6), (4, 3, 1), "hot", share="ccore")
    c("torso", (-1.5, 17.5, -9.2), (3, 2, 1), "spark", share="cspark")
    c("torso", (-10.6, 26, -6.6), (21, 2, 13), "wire", share="collar")                   # a wire collar round the shoulders
    c("torso", (-7, 13.5, 5.5), (14, 5, 2), "iron", share="skirt")
    c("torso", (-9, 17, 5), (18, 12, 4), "iron", inflate=0.2, share="pack")             # the power pack
    for k in range(3):
        c("torso", (-8.5 + k * 6, 18, 8.4), (5, 9, 1), "plate", share="packp")
        c("torso", (-6.5 + k * 6, 20, 9.2), (1, 5, 1), "hot", share="packh")
    for sx in (-1, 1):                                                                   # bolts on the sides
        c("torso", (sx * 10.3 - 1, 20, -3), (2, 2, 2), "iron", share="bolt")
        c("torso", (sx * 10.3 - 1, 20, 2), (2, 2, 2), "iron", share="bolt")
    # ---- coils: two on the back, two on the shoulders, two antennae on the head
    tips = {}
    A.bone("coilBL", "torso", (-5, 28, 8)); tips["BL"] = A.coil("coilBL", -5, 28, 8, 12, 5)
    A.bone("coilBR", "torso", (5, 28, 8)); tips["BR"] = A.coil("coilBR", 5, 28, 8, 12, 5)
    A.bone("coilSL", "torso", (-12.5, 28, 0)); tips["SL"] = A.coil("coilSL", -12.5, 28, 0, 8, 4)
    A.bone("coilSR", "torso", (12.5, 28, 0)); tips["SR"] = A.coil("coilSR", 12.5, 28, 0, 8, 4)
    A.bone("antL", "head", (-3, 36, -3)); tips["AL"] = A.coil("antL", -3, 36, -3, 7, 3, tip=2)
    A.bone("antR", "head", (3, 36, -3)); tips["AR"] = A.coil("antR", 3, 36, -3, 7, 3, tip=2)
    # ---- the head: a block with a glowing visor, a brow plate, a jaw, cheek plates
    c("head", (-4.5, 29, -6.5), (9, 8, 8), "plate", inflate=0.2, share="head")
    c("head", (-5, 35, -7), (10, 2, 9), "plate_dark", share="brow")
    c("head", (-3.5, 31.5, -7.4), (7, 2, 1), "hot", share="visor")
    c("head", (-2.5, 31.9, -7.9), (5, 1, 1), "spark", share="visors")
    c("head", (-3, 28, -6), (6, 2, 6), "iron", share="jaw")
    for sx in (-1, 1):
        c("head", (sx * 5.2 - 0.5, 30, -5), (1, 5, 6), "plate_verd", share="cheek")
    # ---- arms: shoulder plate, upper arm with a wire coil, forearm, an iron fist with copper knuckles and electrodes
    for sg, n in ((-1, "R"), (1, "L")):
        A.bone("arm" + n, "torso", (sg * 12, 27, 0))
        A.bone("fore" + n, "arm" + n, (sg * 12, 18, 0))
        ox = sg * 12 - 3.5
        c("arm" + n, (ox - 1, 24, -5), (9, 4, 10), "plate", inflate=0.2, share="sh")
        c("arm" + n, (ox, 18, -4), (7, 7, 8), "plate_dark", share="up")
        for k in range(3):
            c("arm" + n, (ox - 0.5, 18.4 + k * 2.2, -4.5), (8, 1, 9), "wire", share="aw%d" % (k % 2))
        c("fore" + n, (ox - 0.5, 8, -5), (8, 10, 10), "plate", inflate=0.2, share="fa")
        c("fore" + n, (ox + 0.5, 9, -6.3), (6, 8, 1), "plate_verd", share="fap")
        c("fore" + n, (ox - 1.5, 4, -6), (10, 5, 12), "iron", share="fist")
        c("fore" + n, (ox - 1.5, 8.6, -6.4), (10, 2, 12), "plate", share="fk")
        for k in range(3):
            c("fore" + n, (ox - 0.5 + k * 3, 4.6, -8.2), (2, 2, 3), "spark", share="elec")
            c("fore" + n, (ox - 1 + k * 3, 3.8, -7), (3, 1, 2), "plate", share="elecb")
    # ---- the arcs of current between the coil tips: each in its own bone, pivot at the start tip, so it can flicker out and in
    links = (("arcA", "BL", "BR", 1), ("arcB", "SL", "BL", 2), ("arcC", "SR", "BR", 3), ("arcD", "AL", "AR", 4), ("arcE", "SL", "AL", 5), ("arcF", "SR", "AR", 6))
    arcs = []
    for nm, s, e, seed in links:
        A.bone(nm, "torso", tuple(tips[s]))          # the antennae sit on the head, which barely moves: every arc follows the torso
        arcs.append(nm)
        A.arc(nm, tips[s], tips[e], seed=seed)
    # ---- the ring of sparks that expands in the shock clip (scale 0 hides it)
    A.bone("ring", "torso", (0, 22, 0))
    for k in range(10):
        a = math.radians(k * 36)
        c("ring", (round(9 * math.cos(a) - 1, 2), 21, round(9 * math.sin(a) - 1, 2)), (2, 2, 2), "spark", rot=(0, 45, 0), share="rs")
    # ---- animations
    a = A.anim
    pats = {"arcA": [1, 0, 0, 1, 1, 0, 1, 0], "arcB": [0, 1, 0, 0, 1, 0, 0, 1], "arcC": [1, 0, 1, 0, 0, 1, 0, 0], "arcD": [0, 0, 1, 1, 0, 1, 0, 1],
            "arcE": [1, 1, 0, 0, 0, 1, 0, 0], "arcF": [0, 0, 0, 1, 1, 0, 1, 1]}
    a.clip("idle", length=2.0, loop=True)
    a.pos("idle", "body", loop(2.0, lambda p: (0, 0.8 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "torso", loop(2.0, lambda p: (1.2 * math.sin(p * 2 * math.pi), 2 * math.sin(p * 2 * math.pi + 1), 0)))
    a.rot("idle", "head", loop(2.0, lambda p: (2 * math.sin(p * 2 * math.pi + 0.6), 6 * math.sin(p * 2 * math.pi), 0)))
    for n in ("R", "L"):
        d = 1 if n == "L" else -1
        a.rot("idle", "arm" + n, loop(2.0, lambda p, d=d: (3 * math.sin(p * 2 * math.pi + 0.4), 0, d * 2.5 * math.sin(p * 2 * math.pi))))
    for nm in arcs:
        a.scale("idle", nm, flicker(2.0, pats[nm]))
    for nm in ("coilBL", "coilBR", "coilSL", "coilSR"):
        a.scale("idle", nm, loop(2.0, lambda p, nm=nm: (1, 1 + 0.03 * math.sin(p * 4 * math.pi + len(nm)), 1)))
    a.scale("idle", "ring", {0.0: (0, 0, 0), 2.0: (0, 0, 0)})
    # walk: legs and arms swing against each other, the body bobs twice per cycle
    a.clip("walk", length=1.0, loop=True)
    a.pos("walk", "body", loop(1.0, lambda p: (0, 1.4 * abs(math.sin(p * 2 * math.pi)) - 0.4, 0)))
    a.rot("walk", "torso", loop(1.0, lambda p: (2.0, 4 * math.sin(p * 2 * math.pi), 3 * math.sin(p * 2 * math.pi))))
    a.rot("walk", "head", loop(1.0, lambda p: (0, -5 * math.sin(p * 2 * math.pi), 0)))
    a.rot("walk", "legR", loop(1.0, lambda p: (30 * math.sin(p * 2 * math.pi), 0, 0)))
    a.rot("walk", "legL", loop(1.0, lambda p: (-30 * math.sin(p * 2 * math.pi), 0, 0)))
    a.rot("walk", "shinR", loop(1.0, lambda p: (14 * max(0.0, -math.sin(p * 2 * math.pi)) + 4, 0, 0)))
    a.rot("walk", "shinL", loop(1.0, lambda p: (14 * max(0.0, math.sin(p * 2 * math.pi)) + 4, 0, 0)))
    a.rot("walk", "armR", loop(1.0, lambda p: (-24 * math.sin(p * 2 * math.pi), 0, -4)))
    a.rot("walk", "armL", loop(1.0, lambda p: (24 * math.sin(p * 2 * math.pi), 0, 4)))
    for nm in ("coilBL", "coilBR"):
        a.rot("walk", nm, loop(1.0, lambda p: (4 * math.sin(p * 4 * math.pi), 0, 0)))
    for nm in arcs:
        a.scale("walk", nm, flicker(1.0, pats[nm][:4]))
    a.scale("walk", "ring", {0.0: (0, 0, 0), 1.0: (0, 0, 0)})
    # shock: hunch, throw both arms forward, every arc lit and shaking, the ring of sparks swells and fades
    a.clip("shock", length=1.2, loop=True)
    a.pos("shock", "body", {0.0: (0, 0, 0), 0.3: (0, -2.5, 3), 0.5: (0, 1.5, -2), 0.9: (0, 0.5, -1), 1.2: (0, 0, 0)})
    a.rot("shock", "torso", {0.0: (0, 0, 0), 0.3: (-14, 0, 0), 0.5: (10, 0, 0), 0.9: (6, 0, 0), 1.2: (0, 0, 0)})
    a.rot("shock", "head", {0.0: (0, 0, 0), 0.3: (10, 0, 0), 0.5: (-8, 0, 0), 1.2: (0, 0, 0)})
    a.rot("shock", "armR", {0.0: (0, 0, 0), 0.3: (-30, 0, -10), 0.5: (-95, 0, 6), 0.9: (-88, 0, 4), 1.2: (0, 0, 0)})
    a.rot("shock", "armL", {0.0: (0, 0, 0), 0.3: (-30, 0, 10), 0.5: (-95, 0, -6), 0.9: (-88, 0, -4), 1.2: (0, 0, 0)})
    a.rot("shock", "foreR", {0.0: (0, 0, 0), 0.5: (-10, 0, 0), 1.2: (0, 0, 0)})
    a.rot("shock", "foreL", {0.0: (0, 0, 0), 0.5: (-10, 0, 0), 1.2: (0, 0, 0)})
    for k, nm in enumerate(arcs):
        a.scale("shock", nm, {0.0: (0, 0, 0), 0.25: (1, 1, 1), 0.34: (1.25, 0.7, 1.25), 0.43: (1, 1, 1), 0.52: (1.3, 1.3, 0.8), 0.7: (1, 1, 1), 0.9: (1.2, 1, 1.2), 1.1: (0.6, 0.6, 0.6), 1.2: (0, 0, 0)})
    a.scale("shock", "ring", {0.0: (0, 0, 0), 0.4: (0.2, 0.2, 0.2), 0.7: (1.5, 1.5, 1.5), 1.0: (2.3, 2.3, 2.3), 1.2: (0, 0, 0)})
    for nm in ("coilBL", "coilBR", "coilSL", "coilSR"):
        a.scale("shock", nm, {0.0: (1, 1, 1), 0.5: (1.15, 1.2, 1.15), 1.2: (1, 1, 1)})
    return A.save()


# ====================================================================================================================
# PLAYER PIECES (Space.PLAYER: humanoid proportions, shoulders at y = 24, head 24 to 32)
# ====================================================================================================================
def side_cube(A, bone, sg, origin, size, mat, rot=None, inflate=0.0, share=None):
    """A cube designed for the +x side, mirrored to the -x side when sg == -1 (the player's right is -x)."""
    x0 = origin[0] if sg > 0 else -(origin[0] + size[0])
    r = (rot[0], sg * rot[1], sg * rot[2]) if rot else None
    return A.cube(bone, (x0, origin[1], origin[2]), size, mat, inflate=inflate, rot=r, share=share)


def build_pauldrons():
    A = Atlas("pauldrons", 128, 128, seed=21)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    for sg in (-1, 1):
        n = "R" if sg < 0 else "L"
        A.bone("pauld" + n, "torso", (sg * 6.5, 23, 0))
        side_cube(A, "pauld" + n, sg, (3.6, 23.2, -3.6), (6, 2, 7), "plate", rot=(0, 0, -14), inflate=0.3, share="pd1")
        side_cube(A, "pauld" + n, sg, (4.4, 25.0, -2.8), (5, 2, 6), "plate", rot=(0, 0, -18), inflate=0.2, share="pd2")
        side_cube(A, "pauld" + n, sg, (3.4, 21.8, -3.8), (6, 1, 7), "plate_verd", rot=(0, 0, -10), inflate=0.2, share="pd3")
        side_cube(A, "pauld" + n, sg, (5.0, 26.6, -1.5), (3, 1, 3), "plate_dark", rot=(0, 0, -20), share="pd4")
        for zz in (-3, 0, 3):                                                   # rivets in a row along the rim
            side_cube(A, "pauld" + n, sg, (8.6, 24.6, zz - 1), (2, 2, 2), "hot", rot=(0, 0, 45), share="rv")
    c("torso", (-4.5, 21.5, 1.8), (9, 2, 1), "plate", inflate=0.3, share="bk")      # a plate across the shoulder blades
    c("torso", (-4.5, 17.5, 1.8), (9, 2, 1), "plate_verd", inflate=0.3, share="bk2")
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    for sg in ("R", "L"):
        a.rot("idle", "pauld" + sg, loop(3.0, lambda p: (0, 0, 1.2 * math.sin(p * 2 * math.pi))))
    return A.save()


def build_crown():
    A = Atlas("crown", 128, 128, seed=31)
    A.bone("head", pivot=(0, 24, 0))
    c = A.cube
    # the band round the brow: four plates and a hot stud in front
    c("head", (-4.5, 31.0, -4.9), (9, 2, 1), "plate", inflate=0.15, share="cf")
    c("head", (-4.5, 31.0, 3.9), (9, 2, 1), "plate", inflate=0.15, share="cf")
    c("head", (-4.9, 31.0, -4), (1, 2, 8), "plate", inflate=0.15, share="cs")
    c("head", (3.9, 31.0, -4), (1, 2, 8), "plate", inflate=0.15, share="cs")
    c("head", (-4.5, 33.0, -4.9), (9, 1, 1), "plate_verd", inflate=0.1, share="cv")
    c("head", (-1, 31.3, -5.5), (2, 2, 1), "hot", rot=(0, 0, 45), share="cg")
    # seven coils on the band, one tall in the middle, six round it
    tips = {}
    spec = [(0.0, 0.0, 11)]
    for k in range(6):
        a = math.radians(k * 60 + 30)
        spec.append((round(4.2 * math.cos(a), 2), round(4.2 * math.sin(a), 2), 8 if k % 2 == 0 else 6))
    for k, (x, z, h) in enumerate(spec):
        A.bone("cc%d" % k, "head", (x, 32.5, z))
        tips[k] = A.coil("cc%d" % k, x, 32.5, z, h, 3, tip=2)
    arcs = []
    pairs = [(1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 1), (0, 1), (0, 3), (0, 5)]
    for k, (s, e) in enumerate(pairs):
        nm = "ca%d" % k
        A.bone(nm, "head", tuple(tips[s]))
        arcs.append(nm)
        A.arc(nm, tips[s], tips[e], step=1.3, jit=0.9, seed=40 + k)
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    pat = [[1, 0, 0, 1, 0, 1, 1, 0], [0, 1, 1, 0, 0, 0, 1, 0], [1, 0, 1, 0, 0, 1, 0, 0], [0, 0, 1, 1, 0, 1, 0, 1], [1, 1, 0, 0, 1, 0, 0, 0]]
    for k, nm in enumerate(arcs):
        a.scale("idle", nm, flicker(2.0, pat[k % len(pat)]))
    for k in range(7):
        a.scale("idle", "cc%d" % k, loop(2.0, lambda p, k=k: (1, 1 + 0.05 * math.sin(p * 4 * math.pi + k), 1)))
    return A.save()


# ====================================================================================================================
# PLATES: the skin-layout texture the aura lays over the posed player model (follows every limb of the walk and the swing)
# ====================================================================================================================
def crackle(glow, rect, rng, n, alpha=(70, 150)):
    """Short random-walk lines of electric light (cyan-white) inside a rect (x, y, w, h) of the glow map."""
    gx = glow.load()
    x0, y0, w, h = rect
    for _ in range(n):
        x, y = x0 + rng.randrange(w), y0 + rng.randrange(h)
        for _s in range(rng.randrange(3, 7)):
            if x0 <= x < x0 + w and y0 <= y < y0 + h:
                gx[x, y] = (170, 226, 255, rng.randrange(*alpha))
            if rng.random() < 0.6:
                x += rng.choice((-1, 1))
            else:
                y += rng.choice((-1, 1))


def build_plates():
    """textures/aura/copper_plates.png (+ _glow): a 64x64 player skin laid out like the vanilla one; only the plates are opaque."""
    A = Atlas("plates", 64, 64, seed=71)
    img, glow = A.m.paint()
    px, gx = img.load(), glow.load()
    rng = random.Random(5)

    def faces(u, v, sx, sy, sz):
        return {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}

    def put(box, kinds, r, mat):
        """Paint the part rect r = (col, row, w, h) (in the face's own pixels) on each of the named faces of the box."""
        f = faces(*box)
        for kind in kinds:
            x, y, w, h = f[kind]
            c0, r0, cw, rh = r
            cw = min(cw, w - c0)
            rh = min(rh, h - r0)
            if cw > 0 and rh > 0:
                A.face(px, gx, x + c0, y + r0, cw, rh, mat, SHADE[kind])
                crackle(glow, (x + c0, y + r0, cw, rh), rng, 1 if cw * rh > 12 else 0)

    torso, rarm, larm, rleg, lleg = (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (32, 48, 4, 12, 4), (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)
    # torso: four chest plates, a belt of dark plates and a lower row; the back plate and the sides
    put(torso, ("north",), (0, 0, 4, 5), "plate")
    put(torso, ("north",), (4, 0, 4, 5), "plate_verd")
    put(torso, ("north",), (0, 5, 4, 4), "plate_dark")
    put(torso, ("north",), (4, 5, 4, 4), "plate")
    put(torso, ("north",), (0, 9, 8, 3), "plate_dark")
    put(torso, ("south",), (0, 0, 8, 6), "plate")
    put(torso, ("south",), (0, 6, 4, 6), "plate_verd")
    put(torso, ("south",), (4, 6, 4, 6), "plate")
    put(torso, ("west", "east"), (0, 0, 4, 7), "plate_dark")
    put(torso, ("west", "east"), (0, 7, 4, 5), "plate")
    put(torso, ("top",), (0, 0, 8, 4), "plate_dark")
    for arm in (rarm, larm):
        put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 4), "plate_dark")        # shoulder cap
        put(arm, ("north", "south", "west", "east"), (0, 4, 4, 3), "plate")
        put(arm, ("north", "south", "west", "east"), (0, 7, 4, 5), "plate_verd")               # forearm bracer
    for leg in (rleg, lleg):
        put(leg, ("north", "south", "west", "east"), (0, 0, 4, 5), "plate_dark")                # tasset
        put(leg, ("north", "south", "west", "east"), (0, 5, 4, 7), "plate")                     # greave
        put(leg, ("north",), (0, 5, 4, 3), "plate_verd")
    out = os.path.join(ROOT, "textures", "aura")
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, "copper_plates.png"))
    glow.save(os.path.join(out, "copper_plates_glow.png"))
    print("%-24s skin layout 64x64" % "aura/copper_plates")
    return "plates"


def build_arc_texture():
    """textures/aura/copper_arc.png: a 32x32 horizontal line of light (hot white core, soft falloff across v), tinted by the vertex colour."""
    from PIL import Image
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    px = im.load()
    for y in range(32):
        d = abs(y - 15.5) / 15.5
        core = math.exp(-(d / 0.18) ** 2)
        halo = math.exp(-(d / 0.55) ** 2) * 0.55
        a = clamp(core + halo, 0, 1)
        for x in range(32):
            e = 0.85 + 0.15 * math.sin(x / 31.0 * math.pi)
            v = int(clamp(200 + 55 * core, 0, 255))
            px[x, y] = (v, v, 255 if core > 0.4 else v, int(255 * clamp(a * e, 0, 1)))
    out = os.path.join(ROOT, "textures", "aura")
    os.makedirs(out, exist_ok=True)
    im.save(os.path.join(out, "copper_arc.png"))
    print("%-24s 32x32" % "aura/copper_arc")
    return "arc"


if __name__ == "__main__":
    made = [build_mace(), build_coil_golem(), build_pauldrons(), build_crown(), build_plates(), build_arc_texture()]
    print("done:", ", ".join(made))
    here = os.path.dirname(os.path.abspath(__file__))
    rc = subprocess.call([sys.executable, "-B", os.path.join(here, "geo_check.py"), "copper_mace", "copper_coil_golem", "copper_pauldrons", "copper_crown"])
    sys.exit(rc)

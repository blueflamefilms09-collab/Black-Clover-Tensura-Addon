"""Corundum Magic geo models: the Ideal Closer gauntlet, the floating gem shard and the gem armour pieces of the player render layer.

    python3 -B tools/gen_corundum_geo.py            # writes every model, animation and texture, prints the cube counts
    python3 -B tools/geo_check.py corundum_gauntlet corundum_gem_shard corundum_armour ...   # the validator (this script runs it too)

Models (assets/nusmp/geo/entity/corundum_<name>.geo.json, animations/entity/..., textures/entity/corundum_<name>.png + _glow.png):
  gauntlet   the Ideal Closer fist: a huge clenched fist of stone-grey corundum with ruby and sapphire plates, knuckle gems, a gem
             on the back of the hand, a flared cuff ending in a crown of crystal spikes and six shards orbiting the wrist.
             clips: idle (hover, loose fingers), punch (wind up, strike, recover), slam (raise, hammer down, recover)
  gem_shard  a double-terminated corundum crystal with a collar of small crystals, orbiting chips and a tail; clips: idle, fire
  armour     (Space.PLAYER) torso plates: chest plate with a big gem, belt ribs, two pauldrons with crystal spikes, back plate
  arm_r / arm_l   vambrace for the right / left arm (drawn on the arm's own pose, so it follows the swing)
  leg_r / leg_l   greave and knee crystal for the right / left leg
  crown      a crystal crown (drawn on the head's pose)
  wings      two fans of crystal shards on the back (style 1 of the aura); clip: idle (a slow flap)

The look follows the owner's Ideal Closer stills and the Corundum VFX textures: faceted planar cells, one flat tone per facet, a thin
dark outline round each, hard highlights top left and deep shadow bottom right, with ruby red and sapphire blue gem cells.
Every cube gets its own box-uv rectangle on a shelf-packed atlas and is painted face by face (tops lit, bottoms dark); the glow map
holds the inner fire of the gems, the hot cores and a faint glow along the seams of the stone. File frame: y up, feet at the origin,
the front on -z (see tools/geo_builder.py).
"""
import math
import os
import random
import subprocess
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim   # noqa: E402

KEY = "corundum"

# ramp = [outline, dark, base, light, spec]; glowc = colour of the inner fire; inner / seam = glow strength (0..255)
MATS = {
    "ruby": dict(ramp=[(86, 8, 26), (116, 12, 36), (196, 28, 54), (240, 88, 108), (255, 214, 214)], glowc=(255, 70, 80), inner=120, seam=0, cell=20),
    "sapphire": dict(ramp=[(14, 30, 100), (16, 42, 124), (34, 84, 208), (96, 158, 250), (214, 236, 255)], glowc=(70, 140, 255), inner=120, seam=0, cell=20),
    "stone": dict(ramp=[(22, 26, 40), (62, 70, 94), (112, 124, 152), (168, 180, 206), (226, 232, 246)], glowc=(150, 170, 255), inner=0, seam=46, cell=20),
    "stone_warm": dict(ramp=[(34, 24, 34), (86, 66, 86), (140, 118, 142), (192, 172, 194), (238, 226, 238)], glowc=(255, 150, 160), inner=0, seam=46, cell=20),
    "trim": dict(ramp=[(54, 60, 84), (112, 120, 148), (172, 182, 208), (226, 232, 248), (255, 255, 255)], glowc=(220, 230, 255), inner=0, seam=0, cell=14),
    "ruby_core": dict(ramp=[(90, 6, 20), (170, 18, 40), (240, 60, 66), (255, 150, 120), (255, 238, 214)], glowc=(255, 120, 96), core=True),
    "sapphire_core": dict(ramp=[(10, 30, 110), (24, 76, 200), (66, 140, 255), (160, 216, 255), (238, 250, 255)], glowc=(110, 180, 255), core=True),
}
SHADE = {"top": 1.12, "bottom": 0.60, "north": 1.0, "south": 0.78, "west": 0.88, "east": 0.94}


def clamp(v, lo, hi):
    return lo if v < lo else hi if v > hi else v


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

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, share=None, spin=False):
        """One painted cube. rot = (rx, ry, rz) turns it about its own centre. share = a tag: cubes with the same tag, size and material
        reuse one uv rectangle (identical shards, studs). Sizes are whole pixels (box uv)."""
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
            pivot = [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=pivot)

    def crystal(self, bone, base, axis, length, width, mat, layers=4, twist=True, share=None, tipw=2):
        """A stepped, tapering crystal from the point `base` along axis ('+y', '-z' ...), `width` wide at the foot, with a 45 degree
        twisted copy of each wide layer so the section reads as an octagon. Returns the cube count."""
        sign = -1 if axis[0] == "-" else 1
        a = "xyz".index(axis[1])
        seg = max(1, int(round(length / float(layers))))
        n = 0
        for k in range(layers):
            f = k / float(layers)
            w = max(tipw, int(round(width * (1.0 - f) ** 0.85)))
            lo = base[a] + sign * k * seg if sign > 0 else base[a] - (k + 1) * seg
            o = [0, 0, 0]
            s = [w, w, w]
            for i in range(3):
                o[i] = base[i] - w / 2.0
            o[a] = lo
            s[a] = seg
            tag = (share + "%d" % k) if share else None
            self.cube(bone, tuple(o), tuple(s), mat, share=tag)
            n += 1
            if twist and k < layers - 1 and w >= 4:
                r = [0, 0, 0]
                r[a] = 45
                self.cube(bone, tuple(o), tuple(s), mat, rot=tuple(r), share=(tag + "t") if tag else None)
                n += 1
        return n

    # ---------------------------------------------------------------- painting
    def paint(self):
        img, glow = self.m.paint()
        px, gx = img.load(), glow.load()
        for (u, v, sx, sy, sz, mat) in self.jobs:
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self._face(px, gx, x, y, w, h, mat, SHADE[kind])
        return img, glow

    def _face(self, px, gx, x0, y0, w, h, mat, shade):
        if w <= 0 or h <= 0:
            return
        P, rng = MATS[mat], self.rng
        ramp = P["ramp"]
        n = max(1, int(round(w * h / float(P.get("cell", 16)))))
        seeds = [(rng.uniform(0, w), rng.uniform(0, h), rng.choice((-0.15, -0.06, 0.05, 0.14))) for _ in range(n)]
        gc = P["glowc"]
        core = P.get("core")
        base_t = 0.5 + (shade - 0.9) * 1.1
        for j in range(h):
            for i in range(w):
                d1 = d2 = 1e9
                lv = 0.0
                for (sx_, sy_, l_) in seeds:
                    d = math.hypot(i + 0.5 - sx_, j + 0.5 - sy_)
                    if d < d1:
                        d2, d1, lv = d1, d, l_
                    elif d < d2:
                        d2 = d
                grad = 0.10 * (0.5 - (i / float(max(1, w)) + j / float(max(1, h))) * 0.5)
                t = base_t + lv + grad
                if core:
                    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                    r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
                    t = clamp(1.0 - r * 0.85, 0, 1) * 0.95 + rng.uniform(-0.03, 0.03)
                elif w >= 4 and h >= 4:
                    if i == 0 or j == 0:
                        t += 0.13
                    elif i == w - 1 or j == h - 1:
                        t -= 0.13
                lvl = 0 if t < 0.24 else 1 if t < 0.5 else 2 if t < 0.8 else 3
                if rng.random() < 0.02:
                    lvl = int(clamp(lvl + rng.choice((-1, 1)), 0, 3))
                col = ramp[1 + lvl]
                seam = False
                if not core and w >= 5 and h >= 5 and n > 1 and d2 - d1 < 0.6:
                    k = 0.78 + 0.22 * shade
                    col = tuple(int(c * k) for c in ramp[0])
                    seam = True
                px[x0 + i, y0 + j] = col + (255,)
                a = 0
                if core:
                    a = int(clamp(150 + 105 * t, 0, 255))
                elif seam:
                    a = P.get("seam", 0)
                elif P.get("inner"):
                    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                    r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
                    a = int(clamp(P["inner"] * (1.0 - r * 0.9), 0, 255)) if lvl >= 1 else 0
                if lvl == 3 and not seam and not core and P.get("inner") and rng.random() < 0.6:
                    gx[x0 + i, y0 + j] = tuple(int(c * 0.6 + 255 * 0.4) for c in gc) + (210,)
                elif a:
                    gx[x0 + i, y0 + j] = gc + (a,)

    def save(self):
        img, glow = self.paint()
        self.m.save(self.anim if self.anim.clips else None)
        self.m.save_texture(img, glow)
        print("%-24s %3d cubes  %dx%d  atlas used to y=%d" % (self.m.id, self.m.cube_count(), self.w, self.h, self.y + self.rowh))
        return self.m.id


def loop(clip_len, f, steps=8):
    """Keyframes {t: value} of f(phase 0..1) sampled `steps` times round a closed loop."""
    return {round(clip_len * k / steps, 2): f(k / float(steps)) for k in range(steps + 1)}


def sin_loop(clip_len, amp, phase=0.0, steps=8, ax=1):
    return loop(clip_len, lambda p: tuple(amp * math.sin((p + phase) * 2 * math.pi) if i == ax else 0 for i in range(3)), steps)


# ====================================================================================================================
# GAUNTLET: the Ideal Closer fist
# ====================================================================================================================
def build_gauntlet():
    A = Atlas("gauntlet", 256, 256, seed=7)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("body", "root", (0, 24, 0))
    A.bone("hand", "body", (0, 24, 0))
    c = A.cube
    # ---- the hand: palm block, back-of-hand plates, side plates
    c("hand", (-14, 17, -2), (28, 14, 20), "stone")                                        # palm / back of the hand
    c("hand", (-13, 31, 0), (9, 3, 15), "ruby", inflate=0.2)                               # back plates, four slabs under a slight fan
    c("hand", (-4, 31, 1), (8, 3, 14), "sapphire", inflate=0.2)
    c("hand", (5, 31, 0), (9, 3, 15), "ruby", inflate=0.2)
    c("hand", (-13, 34, 4), (7, 2, 9), "trim", rot=(0, 0, -6))
    c("hand", (6, 34, 4), (7, 2, 9), "trim", rot=(0, 0, 6))
    c("hand", (-15, 20, 2), (2, 10, 14), "sapphire", inflate=0.15)                          # side plates
    c("hand", (13, 20, 2), (2, 10, 14), "sapphire", inflate=0.15)
    c("hand", (-12, 15, 0), (24, 2, 16), "stone_warm", inflate=0.1)                         # heel
    c("hand", (-10, 17, 17), (20, 14, 3), "trim", inflate=0.2)                              # wrist band
    # gem on the back of the hand: a faceted gem built of three turned cubes and a hot core
    A.bone("backgem", "hand", (0, 40, 8))
    c("backgem", (-4, 36, 4), (8, 8, 8), "sapphire", rot=(45, 0, 45), share="bg")
    c("backgem", (-4, 36, 4), (8, 8, 8), "sapphire", rot=(0, 45, 0), share="bg2")
    c("backgem", (-3, 37, 5), (6, 6, 6), "sapphire_core", rot=(45, 45, 0), share="bgc")
    c("backgem", (-9, 33, 6), (4, 4, 4), "ruby", rot=(30, 45, 30), share="bgr")
    c("backgem", (5, 33, 6), (4, 4, 4), "ruby", rot=(30, 45, 30), share="bgr")
    # ---- four fingers, clenched: MCP bone (proximal, vertical in front of the palm), PIP bone (middle, under), DIP bone (tip)
    xs = (-14, -7, 0, 7)
    for i, x0 in enumerate(xs):
        cx = x0 + 3
        gem = "ruby" if i % 2 == 0 else "sapphire"
        plate = "sapphire" if i % 2 == 0 else "ruby"
        A.bone("finger_%d" % i, "hand", (cx, 30, -2))
        A.bone("finger_%d_b" % i, "finger_%d" % i, (cx, 17, -9))
        A.bone("finger_%d_c" % i, "finger_%d_b" % i, (cx, 14, -2))
        c("finger_%d" % i, (x0, 17, -10), (6, 13, 8), "stone", share="fp")
        c("finger_%d" % i, (x0 - 0.5, 22, -11), (7, 6, 2), plate, inflate=0.1, share="fpl" + gem)
        c("finger_%d" % i, (x0, 29, -11), (6, 5, 7), gem, share="fk" + gem)                  # knuckle cap
        c("finger_%d" % i, (x0 + 1, 31, -10), (4, 4, 4), gem, rot=(45, 0, 45), share="fkg" + gem)
        c("finger_%d_b" % i, (x0, 12, -10), (6, 5, 9), "stone", share="fm")
        c("finger_%d_b" % i, (x0, 12, -11), (6, 2, 2), "trim", share="fmt")
        c("finger_%d_c" % i, (x0, 12, -1), (6, 4, 6), "stone_warm", share="fd")
    # ---- thumb: base block on the +x side, tip lying across the front of the fingers
    A.bone("thumb", "hand", (15, 22, -2))
    A.bone("thumb_b", "thumb", (18, 18, -11))
    c("thumb", (14, 16, -9), (6, 10, 10), "stone")
    c("thumb", (14, 21, -8), (7, 4, 8), "ruby", inflate=0.1)
    c("thumb_b", (2, 15, -18), (19, 7, 7), "stone", share="th")
    c("thumb_b", (2, 15, -19), (3, 8, 3), "trim", rot=(0, 0, 0))
    c("thumb_b", (6, 21, -17), (11, 2, 5), "sapphire", inflate=0.1)
    # ---- wrist, the two cuff rings, the crown of crystals
    A.bone("forearm", "body", (0, 24, 24))
    c("forearm", (-9, 18, 18), (18, 12, 6), "stone_warm")
    for k, (z0, hw, hh, d) in enumerate(((22, 12, 9, 9), (33, 14, 11, 9))):
        top, bot = 24 + hh, 24 - hh
        c("forearm", (-hw, bot, z0), (2 * hw, 2 * hh, d), "stone", share="cuffcore%d" % k)
        c("forearm", (-hw + 1, top, z0), (2 * hw - 2, 2, d), "ruby", inflate=0.15, share="cuffu%d" % k)
        c("forearm", (-hw + 1, bot - 2, z0), (2 * hw - 2, 2, d), "sapphire", inflate=0.15, share="cuffd%d" % k)
        c("forearm", (-hw - 2, bot + 2, z0), (2, 2 * hh - 4, d), "sapphire", inflate=0.15, share="cuffl%d" % k)
        c("forearm", (hw, bot + 2, z0), (2, 2 * hh - 4, d), "ruby", inflate=0.15, share="cuffr%d" % k)
        # corner studs
        for sx in (-1, 1):
            for sy in (-1, 1):
                c("forearm", (sx * (hw + 0.0) - 2, 24 + sy * (hh + 0.0) - 2, z0 + 1), (4, 4, d - 2), "trim", rot=(0, 0, 45), share="cs%d" % k)
    c("forearm", (-8, 16, 43), (16, 16, 2), "trim", inflate=0.2)
    A.bone("rear", "forearm", (0, 24, 45))
    c("rear", (-5, 19, 45), (10, 10, 4), "sapphire_core")
    for k in range(6):
        a = k * 60.0
        r = 11
        bx, by = r * math.cos(math.radians(a)), 24 + r * math.sin(math.radians(a))
        A.bone("rc%d" % k, "rear", (round(bx, 2), round(by, 2), 45), rotation=(round(-24 * math.sin(math.radians(a)), 2), round(24 * math.cos(math.radians(a)), 2), 0))
        A.crystal("rc%d" % k, (round(bx, 2), round(by, 2), 45), "+z", 15 if k % 2 == 0 else 11, 6, "ruby" if k % 2 == 0 else "sapphire", layers=3, share="rcr%d" % (k % 2))
    # ---- orbiting shards round the wrist (they turn about the z axis)
    for k in range(4):
        z = -2 + k * 9
        a0 = k * 90.0
        A.bone("orb%d" % k, "body", (0, 24, z))
        r = 25 + (k % 2) * 3
        bx, by = r * math.cos(math.radians(a0)), 24 + r * math.sin(math.radians(a0))
        A.bone("orbs%d" % k, "orb%d" % k, (round(bx, 2), round(by, 2), z), rotation=(0, 0, round(a0 - 90, 2)))
        A.crystal("orbs%d" % k, (round(bx, 2), round(by, 2), z), "+y", 9, 5, "ruby" if k % 2 == 0 else "sapphire", layers=3, share="orb%d" % (k % 2))
    # ---- animations
    a = A.anim
    a.clip("idle", length=4.0, loop=True)
    a.pos("idle", "body", loop(4.0, lambda p: (0, 2.2 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "body", loop(4.0, lambda p: (1.5 * math.sin(p * 2 * math.pi), 2.0 * math.sin(p * 2 * math.pi + 1.2), 1.5 * math.cos(p * 2 * math.pi))))
    for i in range(4):
        a.rot("idle", "finger_%d" % i, loop(4.0, lambda p, i=i: (-7 - 5 * math.sin((p + i * 0.07) * 2 * math.pi), 0, 0)))
        a.rot("idle", "finger_%d_b" % i, loop(4.0, lambda p, i=i: (6 + 4 * math.sin((p + i * 0.07) * 2 * math.pi), 0, 0)))
    a.rot("idle", "thumb_b", loop(4.0, lambda p: (0, 4 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "rear", {0.0: (0, 0, 0), 1.0: (0, 0, 90), 2.0: (0, 0, 180), 3.0: (0, 0, 270), 4.0: (0, 0, 360)})
    a.scale("idle", "backgem", loop(4.0, lambda p: (1 + 0.09 * math.sin(p * 4 * math.pi),) * 3))
    for k in range(4):
        d = 1 if k % 2 == 0 else -1
        a.rot("idle", "orb%d" % k, {0.0: (0, 0, 0), 1.0: (0, 0, 90 * d), 2.0: (0, 0, 180 * d), 3.0: (0, 0, 270 * d), 4.0: (0, 0, 360 * d)})
    for nm in ("forearm",):
        a.rot("idle", nm, loop(4.0, lambda p: (0, 0, 3 * math.sin(p * 2 * math.pi + 0.8))))
    # punch: wind up (0 .. 0.3), strike (0.3 .. 0.42), hold, recover (to 1.0); the entity also moves, this is the body language
    a.clip("punch", length=1.0, loop=True)
    a.pos("punch", "body", {0.0: (0, 0, 0), 0.3: (0, 3, 12), 0.42: (0, -1, -20), 0.6: (0, -1, -16), 1.0: (0, 0, 0)})
    a.rot("punch", "body", {0.0: (0, 0, 0), 0.3: (-10, 0, -8), 0.42: (6, 0, 4), 0.6: (4, 0, 2), 1.0: (0, 0, 0)})
    for i in range(4):
        a.rot("punch", "finger_%d" % i, {0.0: (-7, 0, 0), 0.3: (4, 0, 0), 0.42: (6, 0, 0), 0.7: (2, 0, 0), 1.0: (-7, 0, 0)})
        a.rot("punch", "finger_%d_b" % i, {0.0: (6, 0, 0), 0.3: (-4, 0, 0), 0.42: (-6, 0, 0), 0.7: (0, 0, 0), 1.0: (6, 0, 0)})
    a.pos("punch", "rear", {0.0: (0, 0, 0), 0.3: (0, 0, 4), 0.42: (0, 0, -2), 1.0: (0, 0, 0)})
    a.scale("punch", "backgem", {0.0: (1, 1, 1), 0.3: (1.4, 1.4, 1.4), 0.42: (1.8, 1.8, 1.8), 0.7: (1.2, 1.2, 1.2), 1.0: (1, 1, 1)})
    for k in range(4):
        d = 1 if k % 2 == 0 else -1
        a.rot("punch", "orb%d" % k, {0.0: (0, 0, 0), 0.5: (0, 0, 180 * d), 1.0: (0, 0, 360 * d)})
        a.pos("punch", "orb%d" % k, {0.0: (0, 0, 0), 0.3: (0, 0, 6), 0.42: (0, 0, 22), 0.7: (0, 0, 8), 1.0: (0, 0, 0)})
    # slam: raise the fist and tip it forward, hammer it straight down, recover
    a.clip("slam", length=1.0, loop=True)
    a.pos("slam", "body", {0.0: (0, 0, 0), 0.35: (0, 14, 8), 0.5: (0, -14, -6), 0.65: (0, -14, -6), 1.0: (0, 0, 0)})
    a.rot("slam", "body", {0.0: (0, 0, 0), 0.35: (34, 0, 0), 0.5: (-62, 0, 0), 0.65: (-58, 0, 0), 1.0: (0, 0, 0)})
    for i in range(4):
        a.rot("slam", "finger_%d" % i, {0.0: (-7, 0, 0), 0.35: (2, 0, 0), 0.5: (6, 0, 0), 1.0: (-7, 0, 0)})
        a.rot("slam", "finger_%d_b" % i, {0.0: (6, 0, 0), 0.35: (-3, 0, 0), 0.5: (-6, 0, 0), 1.0: (6, 0, 0)})
    a.scale("slam", "backgem", {0.0: (1, 1, 1), 0.35: (1.5, 1.5, 1.5), 0.5: (2.0, 2.0, 2.0), 1.0: (1, 1, 1)})
    for k in range(4):
        d = 1 if k % 2 == 0 else -1
        a.rot("slam", "orb%d" % k, {0.0: (0, 0, 0), 0.5: (0, 0, 180 * d), 1.0: (0, 0, 360 * d)})
    return A.save()


# ====================================================================================================================
# GEM SHARD: a floating double-terminated crystal
# ====================================================================================================================
def build_gem_shard():
    A = Atlas("gem_shard", 128, 128, seed=11)
    A.bone("root", pivot=(0, 16, 0))
    A.bone("orient", "root", (0, 16, 0), rotation=(-90, 0, 0))                 # the crystal is built standing; its tip turns to the front (-z)
    A.bone("spin", "orient", (0, 16, 0))
    c = A.cube
    # the prism: a square column and a copy turned 45 degrees make an octagonal section
    c("spin", (-5, 8, -5), (10, 16, 10), "ruby")
    c("spin", (-5, 8, -5), (10, 16, 10), "ruby", rot=(0, 45, 0))
    c("spin", (-6.5, 15, -6.5), (13, 2, 13), "trim", inflate=0.1)               # the collar
    c("spin", (-6.5, 15, -6.5), (13, 2, 13), "trim", rot=(0, 45, 0), inflate=0.1)
    A.crystal("spin", (0, 24, 0), "+y", 14, 10, "ruby", layers=5, share="top")      # the front terminal
    A.crystal("spin", (0, 8, 0), "-y", 8, 10, "ruby", layers=3, share="bot")         # the rear terminal
    c("spin", (-1.5, 22, -1.5), (3, 3, 3), "ruby_core", rot=(45, 0, 45))           # a hot spark sunk in the front facet
    # a collar of four small crystals leaning away from the prism (they turn with it)
    for k in range(4):
        phi = math.radians(k * 90 + 45)
        bx, bz = 5.5 * math.cos(phi), 5.5 * math.sin(phi)
        th = 32
        A.bone("sat%d" % k, "spin", (round(bx, 2), 10, round(bz, 2)), rotation=(round(th * math.sin(phi), 2), 0, round(-th * math.cos(phi), 2)))
        A.crystal("sat%d" % k, (round(bx, 2), 10, round(bz, 2)), "+y", 9, 5, "sapphire", layers=3, share="sat")
    # chips orbiting the long axis (z) and a fading tail behind
    for k in range(4):
        ang = math.radians(k * 90)
        r = 9
        bx, by = r * math.cos(ang), 16 + r * math.sin(ang)
        zc = -4 + 5 * k
        A.bone("chip%d" % k, "root", (0, 16, zc))
        c("chip%d" % k, (round(bx, 2) - 1.5, round(by, 2) - 1.5, zc - 1.5), (3, 3, 3), "sapphire" if k % 2 == 0 else "ruby", rot=(45, 45, 0), share="chip" + str(k % 2))
    A.bone("tail", "root", (0, 16, 20))
    for k, sz in enumerate((5, 4, 3)):
        z0 = 20 + k * 6
        c("tail", (-sz / 2.0, 16 - sz / 2.0, z0), (sz, sz, sz), "sapphire", rot=(45, 0, 45), share="tail%d" % k)
    a = A.anim
    a.clip("idle", length=2.0, loop=True)
    a.pos("idle", "root", loop(2.0, lambda p: (0, 1.5 * math.sin(p * 2 * math.pi), 0)))
    a.rot("idle", "root", loop(2.0, lambda p: (2 * math.sin(p * 2 * math.pi), 0, 3 * math.cos(p * 2 * math.pi))))
    a.rot("idle", "spin", {0.0: (0, 0, 0), 0.5: (0, 90, 0), 1.0: (0, 180, 0), 1.5: (0, 270, 0), 2.0: (0, 360, 0)})
    for k in range(4):
        d = 1 if k % 2 == 0 else -1
        a.rot("idle", "chip%d" % k, {0.0: (0, 0, 0), 0.5: (0, 0, 90 * d), 1.0: (0, 0, 180 * d), 1.5: (0, 0, 270 * d), 2.0: (0, 0, 360 * d)})
    a.scale("idle", "tail", loop(2.0, lambda p: (1, 1, 0.85 + 0.2 * math.sin(p * 2 * math.pi))))
    a.clip("fire", length=0.6, loop=True)
    a.scale("fire", "spin", {0.0: (1, 1, 1), 0.2: (0.85, 1.7, 0.85), 0.4: (0.9, 1.45, 0.9), 0.6: (1, 1, 1)})
    a.rot("fire", "spin", {0.0: (0, 0, 0), 0.3: (0, 360, 0), 0.6: (0, 720, 0)})
    a.pos("fire", "tail", {0.0: (0, 0, 0), 0.2: (0, 0, 8), 0.6: (0, 0, 0)})
    a.scale("fire", "tail", {0.0: (1, 1, 1), 0.2: (1.3, 1.3, 1.9), 0.6: (1, 1, 1)})
    for k in range(4):
        d = 1 if k % 2 == 0 else -1
        a.rot("fire", "chip%d" % k, {0.0: (0, 0, 0), 0.3: (0, 0, 360 * d), 0.6: (0, 0, 720 * d)})
    return A.save()


# ====================================================================================================================
# GEM ARMOUR (player space): torso plates, vambraces, greaves, crown, wings
# ====================================================================================================================
def side_cube(A, bone, sg, origin, size, mat, rot=None, inflate=0.0, share=None):
    """A cube designed for the +x side, mirrored to the -x side when sg == -1 (the player's right is -x)."""
    x0 = origin[0] if sg > 0 else -(origin[0] + size[0])
    r = None
    if rot:
        r = (rot[0], sg * rot[1], sg * rot[2])
    return A.cube(bone, (x0, origin[1], origin[2]), size, mat, inflate=inflate, rot=r, share=share)


def tilted_crystal(A, bone, parent, base, tilt, length, width, mat, layers=3, share=None, twist=False):
    """A crystal growing along +y from `base`, its own bone turned by tilt = (rx, ry, rz) about the base."""
    A.bone(bone, parent, tuple(base), rotation=tuple(tilt))
    return A.crystal(bone, tuple(base), "+y", length, width, mat, layers=layers, twist=twist, share=share)


def build_armour():
    A = Atlas("armour", 128, 128, seed=21)
    A.bone("root", pivot=(0, 24, 0))
    A.bone("torso", "root", (0, 24, 0))
    c = A.cube
    c("torso", (-4.5, 15, -3.5), (9, 8, 2), "stone", inflate=0.15)                  # breastplate
    c("torso", (-4.5, 17, -4.5), (4, 5, 1), "ruby", rot=(0, 0, 6), inflate=0.1, share="pec")
    c("torso", (0.5, 17, -4.5), (4, 5, 1), "ruby", rot=(0, 0, -6), inflate=0.1, share="pec")
    c("torso", (-2, 17, -6), (4, 4, 4), "sapphire", rot=(45, 0, 45), share="cg1")  # the chest gem
    c("torso", (-2, 17, -6), (4, 4, 4), "sapphire", rot=(0, 45, 0), share="cg2")
    c("torso", (-1, 18, -5), (2, 2, 2), "sapphire_core", rot=(45, 45, 0))
    c("torso", (-4, 13, -3), (8, 2, 1), "stone_warm", inflate=0.1)
    c("torso", (-4.5, 11.5, -2.5), (9, 2, 5), "trim", inflate=0.3)                   # belt
    c("torso", (-1, 11, -3.5), (2, 3, 1), "ruby", rot=(0, 0, 45), share="buckle")
    c("torso", (-4.5, 14, 1.5), (9, 9, 2), "stone", inflate=0.15)                     # back plate
    c("torso", (-1, 14, 3), (2, 9, 1), "ruby", inflate=0.1)
    for k, (bx, tilt, ln) in enumerate(((-2.5, (0, 0, 14), 9), (0, (0, 0, 0), 12), (2.5, (0, 0, -14), 9))):
        tilted_crystal(A, "bc%d" % k, "torso", (bx, 22, 3.2), (tilt[0] + 22, tilt[1], tilt[2]), ln, 3, "sapphire" if k == 1 else "ruby", share="bcr%d" % (k % 2))
    for sg in (-1, 1):
        n = "R" if sg < 0 else "L"
        A.bone("pauld" + n, "torso", (sg * 6.5, 23, 0))
        side_cube(A, "pauld" + n, sg, (3.5, 23, -3.5), (6, 2, 7), "stone", rot=(0, 0, -14), inflate=0.2, share="pd1")
        side_cube(A, "pauld" + n, sg, (4.5, 25, -2.5), (5, 2, 5), "ruby", rot=(0, 0, -18), inflate=0.1, share="pd2")
        side_cube(A, "pauld" + n, sg, (3.5, 21.5, -3.5), (6, 1, 7), "trim", rot=(0, 0, -10), share="pd3")
        for k, (zz, ln, wd) in enumerate(((0, 7, 4), (-3, 4, 3), (3, 4, 3))):
            tilted_crystal(A, "ps%s%d" % (n, k), "pauld" + n, (sg * 8.5, 26, zz), (0, 0, -sg * (42 if k == 0 else 58)), ln, wd,
                           "sapphire" if k == 0 else "ruby", share="ps%d" % (0 if k == 0 else 1))
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    for sg in ("R", "L"):
        a.rot("idle", "pauld" + sg, loop(3.0, lambda p: (0, 0, 1.5 * math.sin(p * 2 * math.pi))))
    for k in range(3):
        a.rot("idle", "bc%d" % k, loop(3.0, lambda p, k=k: (22 + 3 * math.sin(p * 2 * math.pi + k), 0, 0)))
    return A.save()


def build_arm(sg):
    n = "r" if sg < 0 else "l"
    A = Atlas("arm_" + n, 64, 64, seed=31 + (sg > 0))
    A.bone("arm", pivot=(sg * 5, 22, 0))
    c = lambda *a, **k: side_cube(A, "arm", sg, *a, **k)
    c((3.5, 11.5, -2.5), (5, 6, 5), "stone", inflate=0.3)                            # vambrace
    c((8.1, 12, -2), (1, 6, 4), "ruby", inflate=0.1, share="vo")
    c((3.2, 12, -2), (1, 6, 4), "sapphire", inflate=0.1, share="vi")
    c((3.5, 12, -2.5), (5, 1, 5), "trim", inflate=0.35, share="vt")
    c((3.5, 17, -2.5), (5, 1, 5), "trim", inflate=0.35, share="vt")
    c((4, 18, -2.5), (4, 4, 5), "sapphire", rot=(0, 0, 45), inflate=0.2, share="elb")    # elbow gem
    c((4.5, 11, -3), (3, 2, 3), "ruby", rot=(45, 0, 45), share="kn")                    # knuckle stud at the wrist
    tilted_crystal(A, "arm_s%s" % n, "arm", (sg * 6, 17, 2.2), (-70, 0, -sg * 20), 5, 3, "ruby", share="as")
    tilted_crystal(A, "arm_t%s" % n, "arm", (sg * 6, 14, 2.2), (-80, 0, -sg * 12), 4, 3, "sapphire", share="at")
    return A.save()


def build_leg(sg):
    n = "r" if sg < 0 else "l"
    A = Atlas("leg_" + n, 64, 64, seed=41 + (sg > 0))
    A.bone("leg", pivot=(sg * 1.9, 12, 0))
    c = lambda *a, **k: side_cube(A, "leg", sg, *a, **k)
    c((0.5, 0.4, -2.5), (4.2, 6, 5), "stone", inflate=0.3)                            # greave
    c((0.6, 1, -3.4), (3.8, 5, 1), "ruby", inflate=0.1, share="gf")
    c((0.6, 0, -3.4), (3.8, 1, 1), "trim", inflate=0.1, share="gt")
    c((0.5, 8.5, -2.5), (4, 3, 5), "sapphire", inflate=0.3, share="kc")               # knee cap
    c((1, 9, -4), (3, 3, 3), "sapphire", rot=(45, 0, 45), share="kg1")
    c((1.5, 9.5, -4.5), (2, 2, 2), "ruby_core", rot=(45, 45, 0), share="kg2")
    tilted_crystal(A, "leg_s%s" % n, "leg", (sg * 4.4, 8, 0), (0, 0, -sg * 55), 5, 3, "ruby", share="ls")
    return A.save()


def build_crown():
    A = Atlas("crown", 64, 64, seed=51)
    A.bone("head", pivot=(0, 24, 0))
    c = A.cube
    c("head", (-4.5, 31.2, -4.9), (9, 2, 1), "trim", inflate=0.1, share="cf")
    c("head", (-4.5, 31.2, 3.9), (9, 2, 1), "trim", inflate=0.1, share="cf")
    c("head", (-4.9, 31.2, -4), (1, 2, 8), "trim", inflate=0.1, share="cs")
    c("head", (3.9, 31.2, -4), (1, 2, 8), "trim", inflate=0.1, share="cs")
    c("head", (-1, 31.4, -5.4), (2, 2, 1), "ruby_core", rot=(0, 0, 45))
    spikes = (((0, -4.4), 9, (-10, 0, 0), "ruby"), ((-3, -4.2), 6, (-8, 0, 12), "sapphire"), ((3, -4.2), 6, (-8, 0, -12), "sapphire"),
              ((-4.4, 0), 7, (0, 0, 22), "ruby"), ((4.4, 0), 7, (0, 0, -22), "ruby"), ((-3, 4.2), 6, (8, 0, 12), "sapphire"),
              ((3, 4.2), 6, (8, 0, -12), "sapphire"), ((0, 4.4), 8, (10, 0, 0), "ruby"))
    for k, ((bx, bz), ln, tilt, mat) in enumerate(spikes):
        tilted_crystal(A, "cs%d" % k, "head", (bx, 32.5, bz), tilt, ln, 3, mat, share="csp%s%d" % (mat[0], ln))
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    for k in range(len(spikes)):
        a.rot("idle", "cs%d" % k, loop(3.0, lambda p, k=k, sp=spikes: (sp[k][2][0] + 2 * math.sin(p * 2 * math.pi + k), sp[k][2][1], sp[k][2][2] + 2 * math.cos(p * 2 * math.pi + k))))
    return A.save()


def build_wings():
    A = Atlas("wings", 128, 128, seed=61)
    A.bone("root", pivot=(0, 21, 3))
    c = A.cube
    c("root", (-3, 17, 2), (6, 8, 2), "trim", inflate=0.2)
    c("root", (-2, 19, 3.5), (4, 4, 2), "sapphire", rot=(0, 0, 45), share="wg")
    spec = ((8, 6, 24, "ruby"), (13, 4, 14, "sapphire"), (18, 3, 20, "ruby"), (23, 2, 30, "sapphire"), (28, 1, 38, "ruby"))   # length, width, fan angle
    for sg in (-1, 1):
        n = "R" if sg < 0 else "L"
        A.bone("wing" + n, "root", (sg * 2, 21, 3))
        for k, (ln, wd, ang, mat) in enumerate(spec):
            ln2 = (13, 17, 22, 26, 30)[k]   # the fan, shortest feather first
            A.bone("w%s%d" % (n, k), "wing" + n, (sg * 2, 21, 3), rotation=(0, round(-sg * 24, 2), round(sg * ang * 1.4 - sg * 10, 2)))
            A.crystal("w%s%d" % (n, k), (sg * 2, 21, 3), "+x" if sg > 0 else "-x", ln2, wd + 4, mat, layers=3, twist=False, share="wcr%d%s" % (k, mat[0]))
    a = A.anim
    a.clip("idle", length=3.0, loop=True)
    for sg in ("R", "L"):
        s = 1 if sg == "L" else -1
        a.rot("idle", "wing" + sg, loop(3.0, lambda p, s=s: (0, s * 6 * math.sin(p * 2 * math.pi), s * 2 * math.sin(p * 2 * math.pi + 1))))
        for k in range(5):
            a.rot("idle", "w%s%d" % (sg, k), loop(3.0, lambda p, k=k, s=s: (0, -s * 24 + s * 2 * math.sin(p * 2 * math.pi + k * 0.5), s * (spec[k][2] * 1.4 - 10) + s * 2 * math.sin(p * 2 * math.pi + k))))
    return A.save()


# ====================================================================================================================
# PLATES: the skin-layout texture the aura lays over the posed player model (follows every limb of the walk and the swing)
# ====================================================================================================================
def build_plates():
    """textures/aura/corundum_plates.png (+ _glow): a 64x64 player skin laid out like the vanilla one; only the plates are opaque."""
    A = Atlas("plates", 64, 64, seed=71)
    img, glow = A.m.paint()
    px, gx = img.load(), glow.load()

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
                A._face(px, gx, x + c0, y + r0, cw, rh, mat, SHADE[kind])

    torso, rarm, larm, rleg, lleg = (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (32, 48, 4, 12, 4), (0, 16, 4, 12, 4), (16, 48, 4, 12, 4)
    # torso: ruby chest plates round a sapphire spine of gems, ribs, belt; the back plate with a spine
    put(torso, ("north",), (0, 0, 3, 7), "ruby")
    put(torso, ("north",), (5, 0, 3, 7), "ruby")
    put(torso, ("north",), (3, 0, 2, 9), "sapphire")
    put(torso, ("north",), (0, 7, 3, 3), "stone")
    put(torso, ("north",), (5, 7, 3, 3), "stone")
    put(torso, ("north", "south", "west", "east"), (0, 10, 8, 2), "trim")
    put(torso, ("south",), (0, 0, 8, 10), "stone")
    put(torso, ("south",), (2, 0, 4, 10), "ruby")
    put(torso, ("south",), (3, 0, 2, 10), "sapphire")
    put(torso, ("west", "east"), (0, 0, 4, 7), "stone")
    put(torso, ("west", "east"), (1, 1, 2, 4), "sapphire")
    put(torso, ("top",), (0, 0, 8, 4), "stone")
    for arm in (rarm, larm):
        put(arm, ("north", "south", "west", "east", "top"), (0, 0, 4, 4), "stone")          # shoulder cap
        put(arm, ("north", "south", "west", "east"), (0, 3, 4, 1), "trim")
        put(arm, ("north", "south"), (0, 6, 4, 6), "sapphire")                                 # forearm
        put(arm, ("west", "east"), (0, 6, 4, 6), "ruby")
        put(arm, ("north", "south", "west", "east"), (0, 6, 4, 1), "trim")
        put(arm, ("north", "south", "west", "east"), (0, 11, 4, 1), "trim")
    for leg in (rleg, lleg):
        put(leg, ("north", "south", "west", "east"), (0, 0, 4, 3), "stone_warm")             # tasset
        put(leg, ("north",), (0, 4, 4, 3), "sapphire")                                         # knee
        put(leg, ("north", "south", "west", "east"), (0, 6, 4, 6), "stone")                    # shin
        put(leg, ("north",), (0, 7, 4, 4), "ruby")
        put(leg, ("west", "east", "south"), (1, 7, 2, 4), "sapphire")
        put(leg, ("north", "south", "west", "east"), (0, 10, 4, 2), "trim")
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "aura")
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, "corundum_plates.png"))
    glow.save(os.path.join(out, "corundum_plates_glow.png"))
    print("%-24s skin layout 64x64" % "aura/corundum_plates")
    return "plates"


if __name__ == "__main__":
    made = [build_gauntlet(), build_gem_shard(), build_armour(), build_arm(-1), build_arm(1), build_leg(-1), build_leg(1), build_crown(), build_wings(), build_plates()]
    print("done:", ", ".join(made))

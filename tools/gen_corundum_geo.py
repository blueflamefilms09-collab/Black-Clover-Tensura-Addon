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
    "ruby": dict(ramp=[(52, 4, 16), (116, 12, 36), (196, 28, 54), (240, 88, 108), (255, 214, 214)], glowc=(255, 70, 80), inner=120, seam=0, cell=11),
    "sapphire": dict(ramp=[(6, 12, 54), (16, 42, 124), (34, 84, 208), (96, 158, 250), (214, 236, 255)], glowc=(70, 140, 255), inner=120, seam=0, cell=11),
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
        seeds = [(rng.uniform(0, w), rng.uniform(0, h), rng.uniform(0.12, 0.78)) for _ in range(n)]
        gc = P["glowc"]
        for j in range(h):
            for i in range(w):
                d1 = d2 = 1e9
                lv = 0.5
                for (sx_, sy_, l_) in seeds:
                    d = math.hypot(i + 0.5 - sx_, j + 0.5 - sy_)
                    if d < d1:
                        d2, d1, lv = d1, d, l_
                    elif d < d2:
                        d2 = d
                grad = 0.30 * (1.0 - (i / float(max(1, w)) + j / float(max(1, h))) * 0.5) - 0.12
                t = (lv * 0.72 + grad + (shade - 1.0) * 0.55) * 0.95
                if P.get("core"):
                    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                    r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
                    t = clamp(1.0 - r * 0.8, 0, 1) * 0.95 + rng.uniform(-0.04, 0.04)
                lvl = int(clamp(t * 4.0, 0, 3.99))
                if w >= 4 and h >= 4:
                    if i == 0 or j == 0:
                        lvl = min(3, lvl + 1)
                    elif i == w - 1 or j == h - 1:
                        lvl = max(0, lvl - 1)
                if rng.random() < 0.09:
                    lvl = int(clamp(lvl + rng.choice((-1, 1)), 0, 3))
                col = ramp[1 + lvl]
                seam = False
                if not P.get("core") and w >= 4 and h >= 4 and n > 1 and d2 - d1 < 0.85:
                    k = 0.72 + 0.28 * shade
                    col = tuple(int(c * k) for c in ramp[0])
                    seam = True
                px[x0 + i, y0 + j] = col + (255,)
                a = 0
                if P.get("core"):
                    a = int(clamp(150 + 105 * t, 0, 255))
                elif seam and P.get("seam"):
                    a = P["seam"]
                elif P.get("inner") and not seam:
                    cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
                    r = math.hypot((i - cx) / max(1.0, w / 2.0), (j - cy) / max(1.0, h / 2.0))
                    a = int(clamp(P["inner"] * (1.0 - r * 0.9), 0, 255)) if lvl >= 1 else 0
                if lvl == 3 and not seam and rng.random() < 0.5 and not P.get("core"):
                    a = max(a, 200)
                    gx[x0 + i, y0 + j] = tuple(int(c * 0.6 + 255 * 0.4) for c in gc) + (a,)
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
        c("finger_%d" % i, (x0, 29, -11), (6, 5, 7), gem, rot=(0, 0, 0), share="fk" + gem)                  # knuckle cap
        c("finger_%d" % i, (x0 + 1, 31, -10), (4, 4, 4), gem + "_core" if False else gem, rot=(45, 0, 45), share="fkg" + gem)
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
    for k in range(8):
        a = k * 45.0
        r = 11
        bx, by = r * math.cos(math.radians(a)), 24 + r * math.sin(math.radians(a))
        A.bone("rc%d" % k, "rear", (round(bx, 2), round(by, 2), 45), rotation=(round(-24 * math.sin(math.radians(a)), 2), round(24 * math.cos(math.radians(a)), 2), 0))
        A.crystal("rc%d" % k, (round(bx, 2), round(by, 2), 45), "+z", 15 if k % 2 == 0 else 11, 6, "ruby" if k % 2 == 0 else "sapphire", layers=3, share="rcr%d" % (k % 2))
    # ---- orbiting shards round the wrist (they turn about the z axis)
    for k in range(6):
        z = -4 + k * 7
        a0 = k * 60.0
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
    for k in range(6):
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
    for k in range(6):
        d = 1 if k % 2 == 0 else -1
        a.rot("punch", "orb%d" % k, {0.0: (0, 0, 0), 0.5: (0, 0, 180 * d), 1.0: (0, 0, 360 * d)})
        a.pos("punch", "orb%d" % k, {0.0: (0, 0, 0), 0.3: (0, 0, 6), 0.42: (0, 0, 22), 0.7: (0, 0, 8), 1.0: (0, 0, 0)})
    # slam: raise the fist and tip it forward, hammer it straight down, recover
    a.clip("slam", length=1.0, loop=True)
    a.pos("slam", "body", {0.0: (0, 0, 0), 0.35: (0, 14, 8), 0.5: (0, -14, -6), 0.65: (0, -14, -6), 1.0: (0, 0, 0)})
    a.rot("slam", "body", {0.0: (0, 0, 0), 0.35: (-34, 0, 0), 0.5: (62, 0, 0), 0.65: (58, 0, 0), 1.0: (0, 0, 0)})
    for i in range(4):
        a.rot("slam", "finger_%d" % i, {0.0: (-7, 0, 0), 0.35: (2, 0, 0), 0.5: (6, 0, 0), 1.0: (-7, 0, 0)})
        a.rot("slam", "finger_%d_b" % i, {0.0: (6, 0, 0), 0.35: (-3, 0, 0), 0.5: (-6, 0, 0), 1.0: (6, 0, 0)})
    a.scale("slam", "backgem", {0.0: (1, 1, 1), 0.35: (1.5, 1.5, 1.5), 0.5: (2.0, 2.0, 2.0), 1.0: (1, 1, 1)})
    for k in range(6):
        d = 1 if k % 2 == 0 else -1
        a.rot("slam", "orb%d" % k, {0.0: (0, 0, 0), 0.5: (0, 0, 180 * d), 1.0: (0, 0, 360 * d)})
    return A.save()


if __name__ == "__main__":
    made = [build_gauntlet()]
    print("done:", ", ".join(made))

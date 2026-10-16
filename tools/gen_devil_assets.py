"""Devil Unite assets (1.43.0): the GeoLite models and textures of Nacht Faust's four Unite armours and of the shadow wolf of Unite Canis.

    python3 -B tools/gen_devil_assets.py          # writes every model, animation and texture, prints the cube counts

Why: the four Devil Unite modes (Equus/Slotos, Canis/Gimodelo, Felis/Plumede, Gallus/Walgner) wear a temporary armour that is drawn over the
player model, bound to the model's bones, with no armour slot (client/shadow/DevilOverlay.java, called from client/mode/ModeArmorLayer.java).
The owner wants Blockbench style .geo.json models loaded through GeoLite, so each mode is a handful of small models, one per body part it
decorates; the layer attaches each to the player's posed part (head, body, arms, legs), so they follow every animation. Every model is authored
on the standing player (file frame: pixels, y up, feet at the origin, neck line y = 24, front on -z; the right arm is at x = -5), like the
corundum armour (tools/gen_corundum_geo.py).

Files (assets/nusmp/...):
    geo/devils/devil_<mode>_<part>.geo.json          the model
    animations/devils/devil_<mode>_<part>.animation.json   only when the part moves (the tail, the wings, the crest, the wolf)
    textures/entity/devils/devil_<mode>_<part>.png   its texture, and _glow.png its emissive layer (drawn additive and full bright)
Modes and parts:
    equus   (horse skull, barding): head, body, arm_r, arm_l, leg_r, leg_l
    canis   (wolf hood, claws, spikes): head, body, arm_r, arm_l
    felis   (cat ears, tail, fur trim): head, body, arm_r, arm_l, leg_r, leg_l
    gallus  (rooster crest, wing mantle): head, body, arm_r, arm_l
    shadow_wolf  (file devil_shadow_wolf) the pack wolf of Unite Canis (a magic prop, space PROP): clips idle, run, bite

The palette is the Shadow book's: blue-black iron, slate and pale bone, with one emissive accent per devil (Equus ember, Canis ice blue,
Felis moss green, Gallus gold and crimson). Every cube gets its own box-uv rectangle on a shelf packed atlas, painted face by face (lit tops,
dark bottoms, a one pixel rim). Counts are printed so the budget (a few dozen cubes a part) can be checked.
"""
import json
import math
import os
import random
import sys
import zlib

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT   # noqa: E402

# ---------------------------------------------------------------------------------------------------------------- materials
# ramp: (rim, dark, base, light, spec)
MATS = {
    "iron": ((12, 12, 22), (28, 28, 46), (52, 52, 78), (92, 92, 126), (170, 170, 210)),
    "iron_rune": ((12, 12, 22), (28, 28, 46), (50, 50, 76), (88, 88, 122), (170, 170, 210)),
    "bone": ((70, 62, 58), (150, 142, 132), (206, 200, 188), (236, 232, 222), (255, 255, 250)),
    "fur": ((22, 22, 34), (48, 48, 66), (82, 82, 108), (124, 124, 152), (170, 170, 200)),
    "fur_dark": ((14, 14, 24), (30, 30, 46), (52, 52, 72), (84, 84, 110), (130, 130, 160)),
    "crimson": ((50, 6, 14), (96, 12, 28), (170, 26, 44), (222, 70, 84), (255, 170, 170)),
    "gold": ((70, 46, 10), (140, 98, 24), (214, 166, 52), (250, 214, 110), (255, 246, 200)),
    "feather": ((16, 16, 30), (40, 40, 66), (74, 74, 108), (122, 122, 160), (190, 190, 226)),
}
ACCENT = {"equus": (255, 122, 74), "canis": (110, 176, 255), "felis": (156, 255, 120), "gallus": (255, 190, 70), "shadow": (130, 190, 255)}
SHADE = {"top": 1.14, "bottom": 0.58, "north": 1.0, "south": 0.8, "west": 0.88, "east": 0.94}


def clamp(v, lo=0, hi=255):
    return lo if v < lo else hi if v > hi else v


class Sheet:
    """One model: a geo_builder.Model with a shelf packer and the painter of its box-uv rectangles."""

    def __init__(self, mode, part, w=64, h=64, seed=1):
        self.mode, self.part = mode, part
        self.m = Model("devil", "%s_%s" % (mode, part), w, h)
        self.w, self.h = w, h
        self.x = self.y = self.rowh = 0
        self.jobs = []
        self.shared = {}
        self.rng = random.Random(seed * 7919 + zlib.crc32((mode + part).encode()) % 1000)
        self.anim = Anim()
        self.animated = False

    @property
    def id(self):
        return self.m.id

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def _alloc(self, cw, ch):
        if self.x + cw > self.w:
            self.x, self.y, self.rowh = 0, self.y + self.rowh, 0
        if self.y + ch > self.h:
            raise RuntimeError("atlas %s is full (%dx%d)" % (self.id, self.w, self.h))
        u, v = self.x, self.y
        self.x += cw
        self.rowh = max(self.rowh, ch)
        return u, v

    def cube(self, bone, origin, size, mat, inflate=0.0, rot=None, pivot=None, share=None):
        """origin = (x, y, z) min corner in pixels, size = whole pixels (box uv). rot = degrees about `pivot` (default: the cube's centre)."""
        sx, sy, sz = [int(s) for s in size]
        key = (share, sx, sy, sz, mat) if share else None
        if key and key in self.shared:
            u, v = self.shared[key]
        else:
            u, v = self._alloc(2 * (sx + sz), sy + sz)
            self.jobs.append((u, v, sx, sy, sz, mat))
            if key:
                self.shared[key] = (u, v)
        pv = None
        if rot:
            pv = list(pivot) if pivot is not None else [origin[0] + sx / 2.0, origin[1] + sy / 2.0, origin[2] + sz / 2.0]
        return self.m.cube(bone, origin, (sx, sy, sz), uv=(u, v), inflate=inflate, rotation=rot, pivot=pv)

    def sym(self, bone, cx, y, z, size, mat, rot=None, inflate=0.0, share=None):
        """Two cubes, one each side of the middle: centred at x = +cx and x = -cx. rot is given for the +x side and mirrored for the -x side."""
        sx = int(size[0])
        for sgn in (1, -1):
            r = None if rot is None else (rot[0], rot[1] * sgn, rot[2] * sgn)
            self.cube(bone, (sgn * cx - sx / 2.0, y, z), size, mat, inflate=inflate, rot=r, share=share)

    # ------------------------------------------------------------------------------------------------------ painting
    def paint(self):
        img, glow = self.m.paint()
        px, gx = img.load(), glow.load()
        for (u, v, sx, sy, sz, mat) in self.jobs:
            faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy),
                     "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
            for kind, (x, y, w, h) in faces.items():
                self._face(px, gx, x, y, w, h, mat, SHADE[kind], kind)
        return img, glow

    def _face(self, px, gx, x0, y0, w, h, mat, shade, kind):
        if w <= 0 or h <= 0:
            return
        rng = self.rng
        if mat.startswith("glow"):
            col = ACCENT[self.mode]
            if mat == "glow_white":
                col = (235, 240, 255)
            for j in range(h):
                for i in range(w):
                    edge = i in (0, w - 1) or j in (0, h - 1)
                    k = (0.78 if edge and w > 2 and h > 2 else 1.0) * shade ** 0.3
                    c = tuple(clamp(int(ch * k)) for ch in col)
                    px[x0 + i, y0 + j] = c + (255,)
                    gx[x0 + i, y0 + j] = tuple(clamp(int(ch * (0.85 if edge and w > 2 and h > 2 else 1.0))) for ch in col) + (255,)
            return
        rim, dark, base, light, spec = MATS[mat]
        furry = mat.startswith("fur") or mat == "feather"
        for j in range(h):
            for i in range(w):
                edge = (i in (0, w - 1) or j in (0, h - 1)) and w > 2 and h > 2
                n = rng.random()
                if furry:
                    streak = (math.sin(i * 1.7 + (j // 3) * 2.3 + rng.random()) + 1) / 2
                    c = light if n > 0.9 else base if streak > 0.35 else dark
                elif edge:
                    c = rim if (i == w - 1 or j == h - 1) else dark
                elif j == 1 and w > 3 and n > 0.2:
                    c = light                                   # a lit lip under the top rim
                elif i == 1 and h > 3 and n > 0.5:
                    c = light
                else:
                    c = base if n > 0.12 else dark
                if mat == "bone" and not edge and n > 0.93:
                    c = dark                                     # pits in the bone
                if mat in ("iron", "iron_rune") and w >= 5 and h >= 5:
                    if (i, j) in ((2, 2), (w - 3, 2), (2, h - 3), (w - 3, h - 3)):
                        c = spec                                  # rivets
                    if i == w // 2 and 3 < j < h - 3:
                        c = dark                                  # a panel seam
                col = [clamp(int(ch * shade)) for ch in c]
                px[x0 + i, y0 + j] = tuple(col) + (255,)
                if mat == "iron_rune" and i == w // 2 and 2 <= j <= h - 3 and w >= 3:
                    acc = ACCENT[self.mode]
                    px[x0 + i, y0 + j] = tuple(clamp(int(ch * 0.9)) for ch in acc) + (255,)
                    gx[x0 + i, y0 + j] = acc + (200,)

    def save(self):
        geo_dir = os.path.join(ROOT, "geo", "devils")
        tex_dir = os.path.join(ROOT, "textures", "entity", "devils")
        os.makedirs(geo_dir, exist_ok=True)
        os.makedirs(tex_dir, exist_ok=True)
        with open(os.path.join(geo_dir, self.id + ".geo.json"), "w") as f:
            json.dump(self.m.geo(), f, indent=1)
        ap = os.path.join(ROOT, "animations", "devils", self.id + ".animation.json")
        if self.animated:
            os.makedirs(os.path.dirname(ap), exist_ok=True)
            with open(ap, "w") as f:
                json.dump(self.anim.data(), f, indent=1)
        elif os.path.exists(ap):
            os.remove(ap)
        img, glow = self.paint()
        img.save(os.path.join(tex_dir, self.id + ".png"))
        glow.save(os.path.join(tex_dir, self.id + "_glow.png"))
        print("%-26s cubes %3d  atlas %dx%d" % (self.id, self.m.cube_count(), self.w, self.h))
        return self.id


def loop(length, fn, steps=8):
    """Keyframes {t: value} of fn(phase 0..1) over one loop, closed (the last frame equals the first)."""
    out = {}
    for k in range(steps + 1):
        p = (k % steps) / float(steps)
        out[length * k / steps] = fn(p)
    return out


def side(sg, x0, x1):
    """The x range of a cube given for the left side (x0..x1 > 0), mirrored to the right when sg < 0: returns (origin_x, width)."""
    lo, hi = (x0, x1) if sg > 0 else (-x1, -x0)
    return lo, hi - lo


def sc(S, bone, sg, x0, y, z, w, h, d, mat, **kw):
    """A cube on a side: x0..x0+w for the left (sg > 0), mirrored for the right."""
    ox, _ = side(sg, x0, x0 + w)
    return S.cube(bone, (ox, y, z), (w, h, d), mat, **kw)


def rot_mirror(rot, sg):
    return None if rot is None else (rot[0], -sg * rot[1] if sg < 0 else rot[1], -rot[2] if sg < 0 else rot[2])


# ================================================================================================================= EQUUS
def equus_head():
    S = Sheet("equus", "head", 64, 64, 1)
    S.bone("head", pivot=(0, 24, 0))
    c = S.cube
    c("head", (-2.0, 32.0, -6.0), (4, 2, 11), "bone", inflate=0.1, share="sk")                    # the skull on the crown, nose forward
    c("head", (-1.5, 32.0, -10.0), (3, 2, 5), "bone", share="mz")                                  # muzzle
    c("head", (-1.0, 32.4, -12.0), (2, 1, 3), "bone", share="nz")
    c("head", (-1.5, 33.9, -7.6), (3, 1, 2), "iron", share="br")                                   # brow plate
    c("head", (-2.4, 32.7, -6.4), (1, 1, 1), "glow", share="eye")                                  # the eye sockets burn
    c("head", (1.4, 32.7, -6.4), (1, 1, 1), "glow", share="eye")
    c("head", (-0.5, 32.4, -11.6), (1, 1, 1), "glow", share="nos")
    S.sym("head", 2.2, 34.0, -0.6, (2, 4, 1), "iron", rot=(-12, 0, 14), share="ear")                # ears
    for k in range(5):                                                                            # a mane of spikes down the back of the head
        c("head", (-0.5, 31.0 + k * 0.0, 0.6 + k * 0.8), (1, 3 - (k // 3), 1), "iron_rune", rot=(20, 0, 0), share="mn%d" % (k // 3))
    S.sym("head", 4.2, 25.0, -3.0, (1, 5, 6), "iron", share="chk")                                  # cheek plates
    return S


def equus_body():
    S = Sheet("equus", "body", 128, 64, 2)
    S.bone("body", pivot=(0, 24, 0))
    c = S.cube
    c("body", (-4.0, 15.0, -2.0), (8, 8, 4), "iron", inflate=0.9, share="bp")                      # the breast and back plate, one shell
    c("body", (-1.0, 16.5, -3.4), (2, 5, 1), "glow", share="rn")                                   # a burning rune down the chest
    S.sym("body", 2.5, 17.0, -3.2, (1, 3, 1), "iron_rune", share="rs")
    for k in range(3):                                                                             # barding bands over the belly
        c("body", (-4.0, 12.2 + k * 1.3, -2.0), (8, 1, 4), "iron", inflate=0.55 + k * 0.05, share="bd")
    c("body", (-5.0, 22.0, -3.0), (10, 2, 6), "iron", inflate=0.2, share="gg")                     # the gorget
    c("body", (-4.0, 22.5, 2.2), (8, 2, 1), "bone", share="gb")
    S.sym("body", 4.1, 8.8, -2.5, (1, 5, 5), "iron", inflate=0.1, share="hp")                      # hip barding: a plate each side
    c("body", (-3.5, 8.5, -3.0), (7, 4, 1), "iron_rune", share="fk")                               # front skirt plate
    c("body", (-3.5, 8.0, 2.0), (7, 5, 1), "iron", share="bk")                                     # rear skirt plate
    for k in range(4):                                                                             # a spine of bone along the back plate
        c("body", (-0.5, 15.0 + k * 2.0, 3.2), (1, 1, 1), "bone", share="vb")
    return S


def equus_arm(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("equus", "arm_" + n, 64, 64, 3 + (sg > 0))
    S.bone("arm", pivot=(sg * 5, 22, 0))
    # the pauldron is a horse skull: a plated dome, a muzzle jutting out and forward, burning eyes, ear spikes
    sc(S, "arm", sg, 3.6, 20.2, -3.2, 6, 5, 6, "iron", inflate=0.2, share="pd")
    sc(S, "arm", sg, 8.6, 20.4, -2.0, 3, 3, 4, "bone", share="pm")
    sc(S, "arm", sg, 11.0, 20.8, -1.4, 1, 2, 3, "bone", share="pn")
    sc(S, "arm", sg, 11.8, 21.1, -0.8, 1, 1, 1, "glow", share="pg")
    sc(S, "arm", sg, 6.0, 24.6, -3.6, 1, 1, 1, "glow", share="pe")
    sc(S, "arm", sg, 6.0, 24.6, 2.6, 1, 1, 1, "glow", share="pe")
    sc(S, "arm", sg, 4.6, 25.0, -0.6, 1, 4, 1, "iron", rot=(0, 0, -sg * 10), share="ps")
    sc(S, "arm", sg, 7.0, 25.0, -0.6, 1, 3, 1, "iron", rot=(0, 0, sg * 10), share="ps2")
    sc(S, "arm", sg, 3.6, 11.9, -2.5, 5, 7, 5, "iron", inflate=0.15, share="br")                  # the bracer
    sc(S, "arm", sg, 3.6, 17.2, -2.7, 5, 1, 5, "iron_rune", inflate=0.3, share="bc")
    sc(S, "arm", sg, 7.9, 13.0, -0.5, 1, 4, 1, "glow", share="bs")
    return S


def equus_leg(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("equus", "leg_" + n, 64, 64, 5 + (sg > 0))
    S.bone("leg", pivot=(sg * 1.9, 12, 0))
    sc(S, "leg", sg, 0.0, 0.4, -2.0, 4, 6, 4, "iron", inflate=0.45, share="gr")                    # the greave
    sc(S, "leg", sg, 0.4, 6.2, -3.3, 3, 3, 1, "iron_rune", share="kn")                              # knee plate
    sc(S, "leg", sg, 1.4, 7.2, -4.0, 1, 1, 1, "glow", share="kg")
    sc(S, "leg", sg, 0.0, 9.8, -2.0, 4, 2, 4, "iron", inflate=0.55, share="th")                    # the tasset
    sc(S, "leg", sg, 0.0, 0.0, -3.2, 4, 1, 2, "bone", share="hf")                                   # a hoof-like toe guard
    return S


# ================================================================================================================= CANIS
def canis_head():
    S = Sheet("canis", "head", 64, 64, 11)
    S.bone("head", pivot=(0, 24, 0))
    c = S.cube
    c("head", (-4.0, 29.0, -4.0), (8, 3, 8), "fur_dark", inflate=0.55, share="hd")                  # the hood over the crown and brow
    c("head", (-4.0, 24.2, 3.3), (8, 7, 1), "fur", inflate=0.2, share="dp")                         # hood drape behind
    S.sym("head", 4.65, 25.0, -3.0, (1, 6, 6), "fur_dark", share="sd")                              # side drape
    c("head", (-2.0, 32.2, -6.5), (4, 2, 4), "fur", inflate=0.1, share="sn")                        # the wolf snout over the forehead
    c("head", (-1.5, 32.2, -9.5), (3, 2, 3), "fur", share="sn2")
    c("head", (-1.0, 33.0, -9.9), (2, 1, 1), "bone", share="nz")                                    # nose
    c("head", (-1.3, 31.2, -9.2), (1, 1, 1), "bone", share="fg")                                    # fangs
    c("head", (0.3, 31.2, -9.2), (1, 1, 1), "bone", share="fg")
    c("head", (-2.4, 33.2, -5.8), (1, 1, 1), "glow", share="ey")                                    # eyes
    c("head", (1.4, 33.2, -5.8), (1, 1, 1), "glow", share="ey")
    S.sym("head", 3.6, 32.0, -1.6, (2, 2, 2), "fur_dark", share="e1")                               # pointed ears, stepped
    S.sym("head", 3.8, 34.0, -1.4, (2, 2, 1), "fur_dark", rot=(0, 0, -8), share="e2")
    S.sym("head", 3.6, 35.8, -1.2, (1, 2, 1), "fur", rot=(0, 0, -14), share="e3")
    return S


def canis_body():
    S = Sheet("canis", "body", 128, 64, 12)
    S.bone("body", pivot=(0, 24, 0))
    c = S.cube
    c("body", (-5.0, 21.0, -3.0), (10, 3, 6), "fur", inflate=0.3, share="ru")                        # a thick ruff round the shoulders
    c("body", (-3.0, 17.0, -2.7), (6, 4, 1), "fur", inflate=0.1, share="ch")                         # chest tuft
    c("body", (-1.0, 14.0, -2.7), (2, 3, 1), "fur_dark", share="ct")
    for k in range(5):                                                                              # spikes down the spine, growing at the shoulders
        y = 21.0 - k * 2.4
        h = 2 if k < 2 else 1
        c("body", (-1.0, y, 2.0), (2, 2, 1), "iron", share="sb%d" % h)
        c("body", (-0.5, y + 0.5, 3.0), (1, h, 3 + (2 - k // 2 if k < 4 else 0)), "bone", rot=(-12, 0, 0), share="st%d" % k)
    c("body", (-4.0, 11.6, -2.0), (8, 1, 4), "iron_rune", inflate=0.5, share="bl")                   # belt
    return S


def canis_arm(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("canis", "arm_" + n, 64, 64, 13 + (sg > 0))
    S.bone("arm", pivot=(sg * 5, 22, 0))
    sc(S, "arm", sg, 4.0, 12.0, -2.0, 4, 7, 4, "iron", inflate=0.5, share="gt")                      # the gauntlet
    sc(S, "arm", sg, 4.0, 18.6, -2.0, 4, 1, 4, "iron_rune", inflate=0.7, share="gc")
    sc(S, "arm", sg, 3.5, 20.5, -3.0, 5, 3, 6, "fur", share="sh")                                    # a fur mantle on the shoulder
    for k in range(3):                                                                               # claws: three, stepped, curving forward
        cx = 4.4 + k * 1.3
        sc(S, "arm", sg, cx, 9.0, -2.4, 1, 3, 1, "bone", share="cl%d" % k)
        sc(S, "arm", sg, cx, 7.0, -2.9, 1, 2, 1, "bone", rot=(-14, 0, 0), share="cm")
        sc(S, "arm", sg, cx + 0.05, 5.6, -3.4, 1, 2, 1, "bone", rot=(-26, 0, 0), share="ct")
    for k in range(3):                                                                               # spikes along the outer side of the arm
        y = 20.0 - k * 2.6
        sc(S, "arm", sg, 8.2, y, -1.0, 1, 2, 2, "iron", share="ab")
        sc(S, "arm", sg, 9.1, y + 0.5, -0.5, 3, 1, 1, "bone", rot=(0, 0, -sg * (6 + 4 * k)), share="as%d" % k)
    sc(S, "arm", sg, 6.0, 14.0, -2.7, 1, 2, 1, "glow", share="rg")
    return S


# ================================================================================================================= FELIS
def felis_head():
    S = Sheet("felis", "head", 64, 64, 21)
    S.bone("head", pivot=(0, 24, 0))
    c = S.cube
    c("head", (-4.0, 29.4, -4.0), (8, 1, 8), "iron", inflate=0.35, share="hb")                      # headband
    c("head", (-0.5, 29.6, -4.7), (1, 1, 1), "glow", share="hg")                                    # its gem
    S.sym("head", 3.1, 32.0, -2.8, (3, 2, 3), "fur", share="e1")                                    # tall cat ears
    S.sym("head", 3.2, 34.0, -2.4, (2, 2, 2), "fur", rot=(0, 0, -6), share="e2")
    S.sym("head", 3.2, 36.0, -2.1, (1, 2, 1), "fur", rot=(0, 0, -10), share="e3")
    S.sym("head", 3.1, 32.3, -3.5, (1, 3, 1), "glow", share="ei")                                    # the inner ear
    S.sym("head", 4.9, 25.0, -2.0, (1, 4, 4), "fur", share="cf")                                     # cheek fur
    for k in range(3):                                                                              # whisker marks
        S.sym("head", 4.9, 26.0 + k * 1.2, -4.6, (1, 1, 1), "bone", share="wk")
    return S


def felis_body():
    S = Sheet("felis", "body", 128, 64, 22)
    S.bone("body", pivot=(0, 24, 0))
    c = S.cube
    c("body", (-4.5, 22.0, -2.5), (9, 2, 5), "fur", inflate=0.35, share="co")                         # fur collar
    c("body", (-4.0, 11.2, -2.0), (8, 2, 4), "fur", inflate=0.5, share="hp")                          # fur at the hips
    c("body", (-4.0, 12.0, -2.0), (8, 1, 4), "iron", inflate=0.3, share="bt")                         # belt
    c("body", (-0.5, 12.2, -2.5), (1, 1, 1), "glow", share="bg")
    for k in range(3):                                                                               # chest stripes
        S.sym("body", 2.5, 16.0 + k * 2.2, -2.45, (1, 1, 1), "glow", share="cs")
    # the tail: a chain of segments rising from the base of the spine, swaying
    sizes = [(2, 2, 3)] * 3 + [(2, 2, 3)] * 3 + [(1, 1, 3)]
    S.bone("tail0", parent="body", pivot=(0, 13.0, 2.0))
    z = 2.0
    for k in range(7):
        name = "tail%d" % k
        if k > 0:
            S.bone(name, parent="tail%d" % (k - 1), pivot=(0, 13.0 + 0.0 * k, z))
        w, h, d = sizes[k]
        S.cube(name, (-w / 2.0, 12.0, z), (w, h, d), "fur_dark" if k % 2 else "fur", share="tl%d%d" % (k, w))
        if k in (2, 5):
            S.cube(name, (-1.0, 11.6, z + 0.5), (2, 1, 1), "glow", share="tg")
        z += d
    # animation: sway sideways with a travelling wave, curl up slowly
    S.animated = True
    S.anim.clip("idle", length=3.0, loop=True)
    for k in range(7):
        S.anim.rot("idle", "tail%d" % k, loop(3.0, lambda p, k=k: (-6.0 - k * 1.5 + 2 * math.sin(p * 2 * math.pi + k * 0.6), 9.0 * math.sin(p * 2 * math.pi - k * 0.7), 0.0)))
    return S


def felis_arm(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("felis", "arm_" + n, 64, 64, 23 + (sg > 0))
    S.bone("arm", pivot=(sg * 5, 22, 0))
    sc(S, "arm", sg, 3.6, 12.0, -2.5, 5, 2, 5, "fur", inflate=0.1, share="cf")                        # a fur cuff at the wrist
    sc(S, "arm", sg, 3.6, 17.0, -2.5, 5, 2, 5, "fur", inflate=0.1, share="cf")                        # and at the elbow
    sc(S, "arm", sg, 3.5, 20.8, -3.0, 5, 3, 6, "fur", share="sh")
    for k in range(3):
        sc(S, "arm", sg, 4.3 + k * 1.3, 10.6, -2.3, 1, 2, 1, "bone", share="cw")                       # short retracted claws
    sc(S, "arm", sg, 7.5, 14.0, -0.5, 1, 1, 1, "glow", share="ag")
    return S


def felis_leg(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("felis", "leg_" + n, 64, 64, 25 + (sg > 0))
    S.bone("leg", pivot=(sg * 1.9, 12, 0))
    sc(S, "leg", sg, 0.0, 0.2, -2.0, 4, 3, 4, "fur", inflate=0.4, share="an")                          # fur anklets
    sc(S, "leg", sg, 0.0, 8.5, -2.0, 4, 2, 4, "fur", inflate=0.4, share="kn")
    return S


# ================================================================================================================= GALLUS
def gallus_head():
    S = Sheet("gallus", "head", 64, 64, 31)
    S.bone("head", pivot=(0, 24, 0))
    c = S.cube
    c("head", (-4.0, 29.4, -4.0), (8, 1, 8), "gold", inflate=0.35, share="hb")
    # the comb: plates along the crown, front to back, tall in the middle
    heights = [3, 5, 6, 5, 4, 3]
    S.bone("crest", parent="head", pivot=(0, 32, 0))
    for k, h in enumerate(heights):
        S.cube("crest", (-0.5, 32.0, -5.0 + k * 1.8), (1, h, 2), "crimson", inflate=0.05, share="cb%d" % k)
        if k in (1, 3):
            S.cube("crest", (-0.5, 32.0 + h - 1, -5.0 + k * 1.8), (1, 1, 2), "glow", share="cg")
    S.cube("head", (-0.5, 29.9, -4.9), (1, 1, 1), "glow", share="gm")
    for k in range(3):                                                                               # swept side feathers
        S.sym("head", 4.9, 26.5 + k * 1.4, -1.0 + k * 1.2, (1, 1, 3), "gold", rot=(0, 18, 8), share="sf%d" % k)
    S.animated = True
    S.anim.clip("idle", length=2.4, loop=True)
    S.anim.rot("idle", "crest", loop(2.4, lambda p: (2.0 * math.sin(p * 2 * math.pi), 0.0, 1.5 * math.sin(p * 2 * math.pi + 1.0))))
    return S


def gallus_body():
    S = Sheet("gallus", "body", 128, 128, 32)
    S.bone("body", pivot=(0, 24, 0))
    c = S.cube
    # the mantle: three shoulder-wide rows of feathers, each shorter
    c("body", (-5.5, 21.4, -3.2), (11, 2, 6), "feather", inflate=0.1, share="m1")
    c("body", (-5.0, 19.6, -3.0), (10, 2, 6), "feather", inflate=0.05, share="m2")
    c("body", (-4.5, 17.8, -2.9), (9, 2, 5), "feather", share="m3")
    c("body", (-5.5, 21.0, -3.5), (11, 1, 1), "gold", share="mg")
    c("body", (-1.0, 20.0, -3.4), (2, 2, 1), "glow", share="mj")
    # the folded wings on the back: a fan of feathers each, flapping slowly
    for sgn, nm in ((-1, "wr"), (1, "wl")):
        S.bone(nm, parent="body", pivot=(sgn * 3.0, 21.0, 2.4))
        fan = [(8, 22), (11, 12), (13, 2), (11, -9), (8, -20)]
        for k, (ln, ang) in enumerate(fan):
            S.cube(nm, (sgn * 3.0 - 0.5 + (0.0 if sgn > 0 else 0.0), 21.0 - ln, 2.4 + k * 0.4), (1, ln, 1 + (k % 2)), "feather" if k % 2 == 0 else "gold",
                   rot=(8, 0, sgn * -ang), pivot=(sgn * 3.0, 21.0, 2.4), share="wf%d" % k)
        S.cube(nm, (sgn * 3.0 - 1.0, 19.0, 1.8), (2, 3, 1), "glow", share="wj")
    S.bone("tailf", parent="body", pivot=(0, 13.0, 2.2))
    for k, ang in enumerate((-18, 0, 18)):
        S.cube("tailf", (-0.5, 6.0, 2.2 + k * 0.0), (1, 8, 1), "crimson" if k == 1 else "gold", rot=(14, 0, ang), pivot=(0, 13.0, 2.2), share="tf%d" % k)
    S.animated = True
    S.anim.clip("idle", length=2.4, loop=True)
    S.anim.rot("idle", "wr", loop(2.4, lambda p: (0.0, 0.0, 5.0 * math.sin(p * 2 * math.pi))))
    S.anim.rot("idle", "wl", loop(2.4, lambda p: (0.0, 0.0, -5.0 * math.sin(p * 2 * math.pi))))
    S.anim.rot("idle", "tailf", loop(2.4, lambda p: (3.0 * math.sin(p * 2 * math.pi + 0.8), 0.0, 0.0)))
    return S


def gallus_arm(sg):
    n = "r" if sg < 0 else "l"
    S = Sheet("gallus", "arm_" + n, 64, 64, 33 + (sg > 0))
    S.bone("arm", pivot=(sg * 5, 22, 0))
    sc(S, "arm", sg, 3.6, 12.5, -2.5, 5, 5, 5, "feather", inflate=0.1, share="gv")                      # a feathered gauntlet
    sc(S, "arm", sg, 3.6, 17.2, -2.7, 5, 1, 5, "gold", inflate=0.2, share="gb")
    sc(S, "arm", sg, 4.0, 20.6, -3.0, 4, 3, 6, "feather", share="ep")                                    # epaulette
    for k in range(3):                                                                                   # trailing feathers off the forearm
        sc(S, "arm", sg, 8.0, 16.0 - k * 1.8, 0.5, 1, 1, 4, "gold" if k % 2 else "crimson", rot=(0, sg * -(10 + 5 * k), 0), share="fd%d" % k)
    sc(S, "arm", sg, 6.0, 14.0, -2.7, 1, 2, 1, "glow", share="gg")
    return S


# ================================================================================================================= the pack wolf
def shadow_wolf():
    """The wolf of Unite Canis: a small shadow hound, Space.PROP (feet at y 0, the head toward -z). Clips: idle, run, bite."""
    S = Sheet("shadow", "wolf", 64, 64, 41)
    S.bone("root", pivot=(0, 0, 0))
    S.bone("body", parent="root", pivot=(0, 8, 0))
    S.cube("body", (-2.5, 6.0, -4.0), (5, 5, 9), "fur_dark", share="bd")
    S.cube("body", (-2.0, 10.8, -4.5), (4, 2, 4), "fur", share="mn")                                     # mane over the shoulders
    for k in range(4):
        S.cube("body", (-0.5, 11.0, -1.0 + k * 1.6), (1, 2, 1), "iron", rot=(-10, 0, 0), share="sp")
    S.bone("head", parent="body", pivot=(0, 9.5, -4.0))
    S.cube("head", (-2.5, 7.8, -8.0), (5, 4, 4), "fur", share="hd")
    S.cube("head", (-1.5, 7.8, -11.0), (3, 2, 3), "fur", share="sn")
    S.cube("head", (-1.0, 8.9, -11.4), (2, 1, 1), "bone", share="nz")
    S.cube("head", (-2.0, 11.8, -6.5), (1, 2, 1), "fur_dark", share="ea")
    S.cube("head", (1.0, 11.8, -6.5), (1, 2, 1), "fur_dark", share="ea")
    S.cube("head", (-1.9, 10.4, -8.1), (1, 1, 1), "glow", share="ey")
    S.cube("head", (0.9, 10.4, -8.1), (1, 1, 1), "glow", share="ey")
    S.cube("head", (-1.0, 7.0, -10.5), (1, 1, 1), "bone", share="fg")
    S.cube("head", (0.0, 7.0, -10.5), (1, 1, 1), "bone", share="fg")
    legs = {"flr": (-1.9, -3.0), "fll": (1.9, -3.0), "hlr": (-1.9, 3.5), "hll": (1.9, 3.5)}
    for nm, (lx, lz) in legs.items():
        S.bone(nm, parent="body", pivot=(lx, 6.5, lz))
        S.cube(nm, (lx - 0.9, 0.0, lz - 0.9), (2, 7, 2), "fur_dark", share="lg")
        S.cube(nm, (lx - 1.0, 0.0, lz - 1.4), (2, 1, 1), "bone", share="pw")
    S.bone("tail", parent="body", pivot=(0, 10.0, 5.0))
    S.cube("tail", (-1.0, 9.5, 5.0), (2, 2, 5), "fur", rot=(40, 0, 0), pivot=(0, 10.0, 5.0), share="tl")
    S.cube("tail", (-0.5, 12.0, 8.5), (1, 1, 2), "glow", rot=(40, 0, 0), pivot=(0, 10.0, 5.0), share="tt")
    S.animated = True
    a = S.anim
    a.clip("idle", length=2.0, loop=True)
    a.rot("idle", "tail", loop(2.0, lambda p: (0.0, 12.0 * math.sin(p * 2 * math.pi), 0.0)))
    a.rot("idle", "head", loop(2.0, lambda p: (2.0 * math.sin(p * 2 * math.pi), 0.0, 0.0)))
    a.pos("idle", "body", loop(2.0, lambda p: (0.0, 0.25 * math.sin(p * 2 * math.pi), 0.0)))
    a.clip("run", length=0.5, loop=True)
    for nm, ph in (("flr", 0.0), ("fll", 0.5), ("hlr", 0.5), ("hll", 0.0)):
        a.rot("run", nm, loop(0.5, lambda p, ph=ph: (42.0 * math.sin((p + ph) * 2 * math.pi), 0.0, 0.0)))
    a.pos("run", "body", loop(0.5, lambda p: (0.0, 0.7 * abs(math.sin(p * 2 * math.pi)), 0.0)))
    a.rot("run", "tail", loop(0.5, lambda p: (-14.0, 10.0 * math.sin(p * 4 * math.pi), 0.0)))
    a.rot("run", "head", loop(0.5, lambda p: (-6.0 * math.sin(p * 4 * math.pi), 0.0, 0.0)))
    a.clip("bite", length=0.7, loop=False)
    a.pos("bite", "body", {0.0: (0, 0, 0), 0.2: (0, -0.5, 2.0), 0.3: (0, -1.0, -3.5), 0.45: (0, 0, -1.0), 0.7: (0, 0, 0)})
    a.rot("bite", "head", {0.0: (0, 0, 0), 0.2: (-14, 0, 0), 0.3: (24, 0, 0), 0.45: (10, 0, 0), 0.7: (0, 0, 0)})
    a.rot("bite", "tail", {0.0: (0, 0, 0), 0.3: (-22, 0, 0), 0.7: (0, 0, 0)})
    return S


BUILDERS = [equus_head, equus_body, lambda: equus_arm(-1), lambda: equus_arm(1), lambda: equus_leg(-1), lambda: equus_leg(1),
            canis_head, canis_body, lambda: canis_arm(-1), lambda: canis_arm(1),
            felis_head, felis_body, lambda: felis_arm(-1), lambda: felis_arm(1), lambda: felis_leg(-1), lambda: felis_leg(1),
            gallus_head, gallus_body, lambda: gallus_arm(-1), lambda: gallus_arm(1),
            shadow_wolf]


def main():
    ids = []
    total = 0
    for b in BUILDERS:
        S = b()
        ids.append(S.save())
        total += S.m.cube_count()
    print("done: %d models, %d cubes" % (len(ids), total))
    return ids


if __name__ == "__main__":
    main()

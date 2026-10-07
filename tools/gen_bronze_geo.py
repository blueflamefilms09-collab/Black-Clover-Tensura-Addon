"""Bronze Magic models: the guardian statue, the poison lizard, the armour pieces of the aura, and every texture they use.

    python3 -B tools/gen_bronze_geo.py            # writes everything below
    python3 -B tools/geo_check.py bronze_statue bronze_lizard bronze_pauldron_r bronze_pauldron_l bronze_crest bronze_breast

Look: cast bronze (warm brown-gold, hammered, with polished highlights) with verdigris (cyan-green patina) eating into the edges, the
rims, the lower folds and the joints. Reference: the owner's still of Sekke's tiny cyan-green bronze figurine, and the Bronze Magic spec
("a tarnished green-and-brown metal look; heavy physical weaponry").

Files (all under src/main/resources/assets/nusmp/):
    geo/entity/bronze_<name>.geo.json, animations/entity/bronze_<name>.animation.json,
    textures/entity/bronze_<name>.png + bronze_<name>_glow.png                 name = statue, lizard, pauldron_r, pauldron_l, crest, breast
    textures/aura/bronze_plate.png (+ _glow)      the plate-and-rivet armour laid over the player's skin layout (64x64, cutout)
    textures/aura/bronze_cast.png  (+ _glow)      style 1: the smooth cast-bronze statue skin (64x64, cutout)
    textures/aura/bronze_patina.png               style 1: translucent verdigris crust over the cast skin
    textures/aura/bronze_sheen.png                style 1: additive travelling highlight bands

File frame: pixels, y up, feet at the origin, the front on -z, +x is the model's left (see tools/geo_builder.py). In this renderer a
positive x rotation swings a hanging limb FORWARD, a positive z rotation swings it toward +x.
Every cube gets its own box-uv region from a shelf packer (Sheet) and is painted face by face (bronze_face): baked top light, bevel,
hammered noise, scratches, polished blob, rivets and verdigris that grows from the edges and the lower part of each face.
"""
import math
import os
import sys

import numpy as np
from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import Model, Anim, ROOT  # noqa: E402

# ------------------------------------------------------------------------------------------------ palettes
BRONZE = [(0x1f, 0x11, 0x08), (0x45, 0x26, 0x12), (0x73, 0x45, 0x1f), (0xa6, 0x6f, 0x33), (0xd4, 0x9b, 0x4e), (0xf6, 0xcf, 0x86), (0xff, 0xf0, 0xc0)]
DARKB = [(0x12, 0x0a, 0x05), (0x2b, 0x18, 0x0c), (0x4a, 0x2a, 0x14), (0x6e, 0x42, 0x20), (0x93, 0x5e, 0x2c), (0xb8, 0x80, 0x3e)]
VERD = [(0x0f, 0x36, 0x30), (0x1a, 0x62, 0x55), (0x2e, 0x98, 0x7e), (0x5f, 0xcf, 0xab), (0x9a, 0xee, 0xd2), (0xd6, 0xff, 0xf0)]
GOLD = [(0x4a, 0x2e, 0x08), (0x8a, 0x5c, 0x14), (0xc8, 0x92, 0x26), (0xf0, 0xc0, 0x48), (0xff, 0xe8, 0x90), (0xff, 0xfa, 0xd0)]
STEEL = [(0x3a, 0x20, 0x0c), (0x74, 0x42, 0x16), (0xb8, 0x74, 0x26), (0xe6, 0xa6, 0x44), (0xff, 0xd8, 0x80), (0xff, 0xf4, 0xc8)]

SHADE = {"top": 1.14, "north": 1.0, "west": 0.88, "east": 0.93, "south": 0.8, "bottom": 0.55}


def ramp(pal, t):
    p = np.array(pal, float)
    t = np.clip(t, 0, 1) * (len(p) - 1)
    i = np.minimum(t.astype(int), len(p) - 2)
    f = (t - i)[..., None]
    return p[i] * (1 - f) + p[i + 1] * f


def vnoise(h, w, rng, cell):
    """Smooth value noise 0..1 of h x w texels with features of about 'cell' texels."""
    gh, gw = int(math.ceil(h / cell)) + 2, int(math.ceil(w / cell)) + 2
    g = rng.random((gh, gw)).astype(np.float32)
    im = Image.fromarray(g, mode="F").resize((max(w, int(gw * cell)), max(h, int(gh * cell))), Image.BICUBIC)
    return np.clip(np.asarray(im)[:h, :w], 0, 1)


def face_rects(u, v, w, h, d):
    """The six faces of a box-uv region (vanilla layout): name -> (x, y, width, height) in texels."""
    return {"west": (u, v + d, d, h), "north": (u + d, v + d, w, h), "east": (u + d + w, v + d, d, h),
            "south": (u + 2 * d + w, v + d, w, h), "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d)}


# ------------------------------------------------------------------------------------------------ the face painter
def bronze_face(face, W, H, rng, o):
    """One face of one cube: returns (rgb h x w x 3 floats 0..255, glow h x w x 3 floats)."""
    yy, xx = np.mgrid[0:H, 0:W]
    fx, fy = (xx + 0.5) / W, (yy + 0.5) / H
    ed = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)).astype(float)
    shade = SHADE[face] * o.get("lum", 1.0)
    n = vnoise(H, W, rng, 4) * 0.5 + vnoise(H, W, rng, 2) * 0.3 + rng.random((H, W)) * 0.2
    t = 0.40 * shade + (n - 0.5) * o.get("grain", 0.42)
    if face not in ("top", "bottom"):
        t += 0.17 * (0.5 - fy)
    # bevel: lit top / left edge, shadowed bottom / right edge
    t += np.where(yy == 0, 0.20, 0.0) + np.where(xx == 0, 0.09, 0.0) - np.where(yy == H - 1, 0.22, 0.0) - np.where(xx == W - 1, 0.12, 0.0)
    # the polished spot of cast metal
    pol = o.get("polish", 0.5)
    t += pol * 0.36 * np.exp(-(((fx - 0.30) / 0.30) ** 2 + ((fy - 0.26) / 0.32) ** 2))
    # hammer marks and a scratch or two
    if W > 3 and H > 3:
        for _ in range(int(rng.integers(0, 3))):
            x0, y0 = int(rng.integers(0, W)), int(rng.integers(0, H))
            ln = int(rng.integers(2, 6))
            for k in range(ln):
                x, y = x0 + k, y0 + (k // 2)
                if 0 <= x < W and 0 <= y < H:
                    t[y, x] += 0.20
    pal = o.get("pal", BRONZE)
    rgb = ramp(pal, t + o.get("tone", 0.0))
    # verdigris: grows from the edges, the lower part of the face and low-frequency noise
    pat = o.get("pat", 0.22) * min(1.0, 0.3 + min(W, H) / 8.0)
    if pat > 0:
        m = vnoise(H, W, rng, 3) * 0.45 + vnoise(H, W, rng, 1.6) * 0.20 + np.exp(-ed / 1.1) * 0.50 + fy * 0.15
        if face == "bottom":
            m += 0.1
        mask = np.clip((m - (1.18 - pat * 0.95)) / 0.07, 0, 1)
        mask = np.where(mask > 0.5, 1.0, mask * 0.5)
        vt = 0.22 + 0.45 * n + 0.18 * (shade - 0.8)
        rgb = rgb * (1 - mask[..., None]) + ramp(VERD, vt) * mask[..., None]
    glow = np.zeros((H, W, 3))
    # rivets: a lit pixel with a shadow pixel
    if o.get("rivet") and W >= 5 and H >= 5:
        pts = [(1, 1), (W - 3, 1), (1, H - 3), (W - 3, H - 3)]
        if W >= 9:
            pts += [(W // 2 - 1, 1), (W // 2 - 1, H - 3)]
        if H >= 9:
            pts += [(1, H // 2 - 1), (W - 3, H // 2 - 1)]
        for (x, y) in pts:
            rgb[y, x] = np.array(BRONZE[6]) * 0.95
            rgb[y + 1, x + 1] = np.array(BRONZE[0]) * 1.4
            rgb[y, x + 1] = np.array(BRONZE[4])
            rgb[y + 1, x] = np.array(BRONZE[3]) * 0.8
    return rgb, glow


def paint_special(face, W, H, rng, mat, o):
    """Materials other than plain bronze."""
    if mat == "bronze":
        return bronze_face(face, W, H, rng, o)
    if mat == "dark":
        return bronze_face(face, W, H, rng, dict(o, pal=DARKB, pat=o.get("pat", 0.1), polish=0.2))
    if mat == "verd":
        return bronze_face(face, W, H, rng, dict(o, pat=0.9))
    if mat == "gold":
        return bronze_face(face, W, H, rng, dict(o, pal=GOLD, pat=0.0, polish=0.9, rivet=False))
    if mat == "blade":
        rgb, glow = bronze_face(face, W, H, rng, dict(o, pal=STEEL, pat=o.get("pat", 0.1), polish=0.8, tone=0.1))
        if face in ("west", "east") and W >= 1:
            rgb[:, :] = rgb * 0.95
            rgb[H // 2:H // 2 + 1, :] = ramp(STEEL, np.full((1, W), 0.95))          # the lit edge line along the blade
            rgb[: max(1, H // 6), :] = ramp(STEEL, np.full((max(1, H // 6), W), 0.92))
        return rgb, glow
    if mat in ("eye", "ember", "venom"):
        base = dict(o, pal=DARKB, pat=0.0, polish=0.1) if mat == "eye" else dict(o, pat=0.0)
        rgb, glow = bronze_face(face, W, H, rng, base)
        col = {"eye": (120, 255, 214), "ember": (255, 170, 70), "venom": (110, 255, 120)}[mat]
        if face == "north" or mat != "eye":
            cx, cy = (W - 1) / 2, (H - 1) / 2
            yy, xx = np.mgrid[0:H, 0:W]
            r = np.sqrt(((xx - cx) / max(1.0, W / 2)) ** 2 + ((yy - cy) / max(1.0, H / 2)) ** 2)
            core = np.clip(1.25 - r, 0, 1)
            glow = np.array(col, float)[None, None, :] * core[..., None]
            rgb = rgb * (1 - core[..., None]) + np.array(col, float)[None, None, :] * core[..., None]
        return rgb, glow
    if mat == "tooth":
        return bronze_face(face, W, H, rng, dict(o, pal=STEEL, pat=0.0, polish=0.9, tone=0.18))
    if mat == "shield":
        return shield_face(face, W, H, rng, o)
    return bronze_face(face, W, H, rng, o)


def shield_face(face, W, H, rng, o):
    """The round shield: the front is a function of the distance from the disc centre only, so the four plates that make the disc agree."""
    rgb, glow = bronze_face(face, W, H, rng, dict(o, pat=0.12))
    if face != o.get("front", "north"):
        return rgb, glow
    yy, xx = np.mgrid[0:H, 0:W]
    r = np.sqrt(((xx + 0.5) / W - 0.5) ** 2 * W * W + ((yy + 0.5) / H - 0.5) ** 2 * H * H) * (7.3 / o.get("R", 7.3))
    hash_ = ((np.floor(xx * 2.0) * 73 + np.floor(yy * 2.0) * 151) % 7) / 7.0
    base = 0.50 + 0.10 * np.sin(r * 2.1) + hash_ * 0.07
    t = np.clip(base + 0.16 * np.clip(1.0 - r / 7.0, 0, 1), 0, 1)
    img = ramp(BRONZE, t)
    img = np.where((r > 6.35)[..., None], ramp(BRONZE, t * 0.75 + 0.26), img)                                          # the raised rim
    img = np.where(((r > 5.95) & (r <= 6.35))[..., None], ramp(DARKB, t * 0.9 + 0.1), img)                              # the groove inside it
    img = np.where(((r > 3.3) & (r < 3.9))[..., None], ramp(BRONZE, t * 0.55 + 0.3), img)                              # an inner ring
    img = np.where((r < 2.2)[..., None], ramp(GOLD, 0.55 + 0.4 * np.clip(1 - r / 2.2, 0, 1)), img)                      # the boss
    v = np.clip((r - 6.0) * 0.4, 0, 1) * 0.8 * (hash_ > 0.28)                                                          # verdigris eating the rim
    img = img * (1 - v[..., None]) + ramp(VERD, 0.3 + 0.4 * hash_) * v[..., None]
    g = np.zeros((H, W, 3))
    ring = (r < 2.0) * 0.9                                                                                             # the boss glows warm
    g[..., 0], g[..., 1], g[..., 2] = ring * 255, ring * 160, ring * 50
    return img, g


# ------------------------------------------------------------------------------------------------ a model with its packed atlas
class Sheet:
    def __init__(self, key, name, tw, th, seed):
        self.m = Model(key, name, tw, th)
        self.tw, self.th, self.seed = tw, th, seed
        self.items = []

    def bone(self, name, parent=None, pivot=(0, 0, 0), rotation=None):
        return self.m.bone(name, parent=parent, pivot=pivot, rotation=rotation)

    def cube(self, bone, o, s, mat="bronze", rot=None, pv=None, infl=0.0, **opt):
        c = self.m.cube(bone, origin=o, size=s, uv=(0, 0), inflate=infl, rotation=rot, pivot=pv)
        self.items.append((c, tuple(s), mat, opt, len(self.items)))
        return c

    def pair(self, right, left, specs):
        """specs are dicts of cube arguments for the right side (-x); the left side gets the mirrored cube."""
        for sp in specs:
            sp = dict(sp)
            o, s = sp.pop("o"), sp.pop("s")
            rot, pv = sp.pop("rot", None), sp.pop("pv", None)
            self.cube(right, o, s, rot=rot, pv=pv, **sp)
            self.cube(left, (-(o[0] + s[0]), o[1], o[2]), s, rot=None if rot is None else (rot[0], -rot[1], -rot[2]),
                      pv=None if pv is None else (-pv[0], pv[1], pv[2]), **sp)

    def pack(self):
        rects = []
        for c, s, mat, opt, i in self.items:
            w, h, d = [int(math.ceil(x)) for x in s]
            rects.append((2 * (w + d), d + h, i))
        order = sorted(rects, key=lambda r: (-r[1], -r[0]))
        x = y = rowh = 0
        place = {}
        for (rw, rh, i) in order:
            if x + rw > self.tw:
                x, y, rowh = 0, y + rowh + 1, 0
            if y + rh > self.th:
                raise SystemExit("sheet %s is too small (%d x %d): needs more than %d rows" % (self.m.id, self.tw, self.th, y + rh))
            place[i] = (x, y)
            x += rw + 1
            rowh = max(rowh, rh)
        return place

    def paint(self):
        place = self.pack()
        img = np.zeros((self.th, self.tw, 4))
        glow = np.zeros((self.th, self.tw, 4))
        for c, s, mat, opt, i in self.items:
            u, v = place[i]
            c["uv"] = [u, v]
            w, h, d = s
            rng = np.random.default_rng(self.seed * 1009 + i * 7919)
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                x0, y0 = int(math.floor(fx)), int(math.floor(fy))
                x1, y1 = int(math.ceil(fx + fw)), int(math.ceil(fy + fh))
                W, H = max(1, x1 - x0), max(1, y1 - y0)
                if W < 1 or H < 1:
                    continue
                o = dict(opt)
                rgb, g = paint_special(face, W, H, rng, mat, o)
                img[y0:y0 + H, x0:x0 + W, :3] = np.clip(rgb, 0, 255)
                img[y0:y0 + H, x0:x0 + W, 3] = 255
                glow[y0:y0 + H, x0:x0 + W, :3] = np.clip(g, 0, 255)
                glow[y0:y0 + H, x0:x0 + W, 3] = 255
        return Image.fromarray(img.astype(np.uint8), "RGBA"), Image.fromarray(glow.astype(np.uint8), "RGBA")

    def save(self, anim=None):
        im, gl = self.paint()
        self.m.save(anim)
        self.m.save_texture(im, gl)
        print("wrote", self.m.id, "cubes", self.m.cube_count(), "tex", self.tw, "x", self.th,
              "clips", sorted(anim.clips) if anim else "-")


# ------------------------------------------------------------------------------------------------ animation helpers
def wave(a, clip, bone, length, steps=12, x=None, y=None, z=None, kind="rot"):
    """Keyframes every length/steps seconds; x, y, z are functions of the phase p in [0, 1) (None = 0). The last key equals the first."""
    if clip not in a.clips:
        a.clip(clip, length=length, loop=True)
    frames = {}
    for k in range(steps + 1):
        p = (k % steps) / steps
        frames[round(length * k / steps, 4)] = tuple(float(f(p)) if f else (1.0 if kind == "scale" else 0.0) for f in (x, y, z))
    getattr(a, kind)(clip, bone, frames)


def S(amp, phase=0.0, cycles=1, off=0.0):
    return lambda p: off + amp * math.sin(2 * math.pi * (p * cycles + phase))


def keys(a, clip, bone, kind, table):
    """table: {time: (x, y, z)}."""
    if clip not in a.clips:
        a.clip(clip, length=max(table), loop=True)
    getattr(a, kind)(clip, bone, {t: tuple(v) for t, v in table.items()})


# ================================================================================================================================
# 1. THE GUARDIAN STATUE
# ================================================================================================================================
def build_statue():
    sh = Sheet("bronze", "statue", 256, 128, seed=11)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("pelvis", "root", (0, 16, 0))
    for side, sx in (("r", -1), ("l", 1)):
        sh.bone("thigh_" + side, "pelvis", (sx * 2.8, 16, 0))
        sh.bone("shin_" + side, "thigh_" + side, (sx * 2.8, 8.5, 0))
        sh.bone("foot_" + side, "shin_" + side, (sx * 2.8, 2.0, 0))
    for nm, pv in (("pter_f", (0, 17, -3.0)), ("pter_b", (0, 17, 3.0)), ("pter_r", (-5.4, 17, 0)), ("pter_l", (5.4, 17, 0))):
        sh.bone(nm, "pelvis", pv)
    sh.bone("chest", "pelvis", (0, 18, 0))
    sh.bone("head", "chest", (0, 27.5, 0))
    sh.bone("crest", "head", (0, 35.2, 0))
    sh.bone("arm_r", "chest", (-6.4, 26, 0), rotation=(14, 0, 0))
    sh.bone("forearm_r", "arm_r", (-6.6, 19.5, 0), rotation=(28, 0, 0))
    sh.bone("hand_r", "forearm_r", (-6.6, 13.2, 0))
    sh.bone("sword", "hand_r", (-6.4, 11.6, -1.6), rotation=(-48, 0, 0))
    sh.bone("arm_l", "chest", (6.4, 26, 0), rotation=(14, 0, 10))
    sh.bone("forearm_l", "arm_l", (6.6, 19.5, 0), rotation=(56, 0, 0))
    sh.bone("hand_l", "forearm_l", (6.6, 13.2, 0))
    sh.bone("shield", "forearm_l", (7.4, 8.6, 0.0))

    # -- legs: thigh with a side plate, shin with knee cap, greave, calf; foot with sandal straps and a toe cap
    sh.pair("thigh_r", "thigh_l", [
        dict(o=(-5.0, 9.0, -2.4), s=(4.4, 7.6, 4.8), pat=0.22),
        dict(o=(-5.7, 10.2, -2.8), s=(1.2, 5.4, 5.6), pat=0.4, rivet=True),
        dict(o=(-4.6, 10.8, -3.3), s=(3.6, 4.0, 1.0), pat=0.25),
    ])
    sh.pair("shin_r", "shin_l", [
        dict(o=(-4.7, 7.0, -3.6), s=(3.8, 3.2, 1.8), rot=(-10, 0, 0), pv=(-2.8, 8.6, -2.6), pat=0.35, polish=0.8),
        dict(o=(-4.7, 2.2, -2.3), s=(3.8, 6.0, 4.6), pat=0.2),
        dict(o=(-4.5, 2.5, -3.2), s=(3.4, 5.8, 1.0), pat=0.45, rivet=True),
        dict(o=(-3.3, 2.9, -3.8), s=(1.0, 5.0, 0.6), pat=0.3, polish=0.9),
        dict(o=(-4.3, 3.8, 1.8), s=(3.0, 3.6, 1.4), pat=0.3),
        dict(o=(-5.0, 1.8, -2.7), s=(4.4, 1.1, 5.4), pat=0.5),
    ])
    sh.pair("foot_r", "foot_l", [
        dict(o=(-4.6, 0.0, -5.6), s=(3.8, 1.9, 7.6), pat=0.3),
        dict(o=(-4.4, 0.0, -6.6), s=(3.4, 1.5, 1.2), pat=0.4),
        dict(o=(-4.8, 1.7, -3.6), s=(4.2, 0.7, 1.2), mat="dark"),
        dict(o=(-4.8, 1.7, -1.6), s=(4.2, 0.7, 1.2), mat="dark"),
        dict(o=(-4.5, 0.2, 1.7), s=(3.6, 1.6, 1.2), pat=0.35),
    ])

    # -- pelvis: hip block, belt with a gold buckle and studs
    sh.cube("pelvis", (-4.6, 15.0, -2.5), (9.2, 3.2, 5.0), pat=0.2)
    sh.cube("pelvis", (-5.0, 17.5, -2.9), (10.0, 1.6, 5.8), pat=0.3, rivet=True)
    sh.cube("pelvis", (-1.6, 17.3, -3.6), (3.2, 2.0, 0.8), mat="gold")
    # the pteruges (hanging plates): front and back rows swing, side plates too
    for i in range(-2, 3):
        sh.cube("pter_f", (i * 1.95 - 0.85, 11.6, -3.5), (1.7, 5.6, 0.8), rot=(-7, 0, 0), pv=(0, 17.2, -3.0), pat=0.35 + 0.1 * (i % 2), mat="bronze" if i % 2 else "dark")
        sh.cube("pter_b", (i * 1.95 - 0.85, 12.2, 2.7), (1.7, 5.0, 0.8), rot=(7, 0, 0), pv=(0, 17.2, 3.0), pat=0.4, mat="bronze" if i % 2 else "dark")
    sh.cube("pter_r", (-6.2, 12.2, -2.0), (0.8, 5.0, 4.0), rot=(0, 0, 7), pv=(-5.4, 17.2, 0), pat=0.35)
    sh.cube("pter_l", (5.4, 12.2, -2.0), (0.8, 5.0, 4.0), rot=(0, 0, -7), pv=(5.4, 17.2, 0), pat=0.35)

    # -- chest: belly plates, ribcage, pectorals, back plate, gorget, collar, emblem
    sh.cube("chest", (-3.8, 18.0, -2.3), (7.6, 4.3, 4.6), pat=0.18)
    for yy in (18.3, 20.2):
        for xx in (-3.2, 0.4):
            sh.cube("chest", (xx, yy, -3.0), (2.8, 1.7, 0.8), pat=0.25, polish=0.7)
    sh.cube("chest", (-4.8, 22.2, -2.6), (9.6, 4.6, 5.2), pat=0.2)
    sh.pair("chest", "chest", [dict(o=(-4.6, 23.0, -3.6), s=(4.4, 3.4, 1.2), pat=0.3, polish=0.8, rivet=True)])
    sh.cube("chest", (-4.5, 19.0, 2.1), (9.0, 7.6, 1.0), pat=0.3, rivet=True)
    sh.cube("chest", (-0.6, 19.0, 3.0), (1.2, 7.0, 0.8), pat=0.2, polish=0.8)
    sh.pair("chest", "chest", [dict(o=(-4.2, 22.8, 3.0), s=(3.6, 3.0, 0.6), pat=0.3)])
    sh.cube("chest", (-3.4, 26.5, -2.9), (6.8, 1.4, 5.8), pat=0.45, polish=0.8)
    sh.cube("chest", (-5.6, 25.9, -2.5), (11.2, 1.2, 5.0), pat=0.4, rivet=True)
    sh.cube("chest", (-1.7, 23.2, -4.2), (3.4, 3.4, 0.7), mat="ember")
    sh.cube("chest", (-2.2, 22.7, -3.9), (4.4, 4.4, 0.4), mat="gold")

    # -- head: neck, face, helmet dome, brow band, nose guard, cheek guards, glowing eyes, neck guard, crest
    sh.cube("head", (-1.8, 26.6, -1.8), (3.6, 1.9, 3.6), pat=0.15, mat="dark")
    sh.cube("head", (-3.4, 28.2, -3.4), (6.8, 6.2, 6.8), pat=0.15, polish=0.7)
    sh.cube("head", (-3.9, 32.0, -3.9), (7.8, 3.0, 7.8), pat=0.3, polish=0.8)
    sh.cube("head", (-3.0, 34.8, -3.0), (6.0, 1.1, 6.0), pat=0.25, polish=0.9)
    sh.cube("head", (-4.0, 30.9, -4.1), (8.0, 1.4, 1.0), pat=0.4, rivet=True)
    sh.cube("head", (-0.7, 28.5, -4.5), (1.4, 3.9, 1.0), pat=0.35, polish=0.8)
    sh.pair("head", "head", [dict(o=(-4.2, 28.0, -3.0), s=(1.1, 4.2, 5.2), pat=0.4, rivet=True)])
    sh.cube("head", (-2.7, 30.0, -4.0), (1.9, 0.9, 0.5), mat="eye")
    sh.cube("head", (0.8, 30.0, -4.0), (1.9, 0.9, 0.5), mat="eye")
    sh.cube("head", (-1.3, 28.4, -3.7), (2.6, 0.5, 0.4), mat="dark")
    sh.cube("head", (-3.7, 28.0, 3.3), (7.4, 3.6, 1.0), pat=0.35)
    sh.cube("crest", (-0.7, 35.0, -4.2), (1.4, 1.0, 8.4), pat=0.3, rivet=False)
    for i, hgt in enumerate((2.8, 4.2, 4.9, 4.2, 2.8)):
        sh.cube("crest", (-0.6, 35.9, -3.4 + i * 1.7), (1.2, hgt, 1.4), pat=0.28, mat="bronze" if i % 2 else "dark", rot=(0, 0, 0))

    # -- right arm: layered pauldron, shoulder ball, upper arm, elbow guard, forearm, vambrace, fist; the short sword
    sh.cube("arm_r", (-9.6, 24.0, -3.0), (3.8, 2.4, 6.0), rot=(0, 0, 8), pv=(-6.4, 26, 0), pat=0.35, rivet=True)
    sh.cube("arm_r", (-9.3, 25.6, -3.2), (3.4, 2.0, 6.4), rot=(0, 0, 20), pv=(-6.4, 26, 0), pat=0.4, polish=0.8)
    sh.cube("arm_r", (-8.2, 27.0, -3.0), (3.0, 1.5, 6.0), rot=(0, 0, 32), pv=(-6.4, 26, 0), pat=0.3, polish=0.9)
    sh.cube("arm_r", (-7.6, 23.6, -2.2), (3.4, 3.6, 4.4), pat=0.15, mat="dark")
    sh.cube("arm_r", (-8.4, 19.8, -1.9), (3.4, 4.8, 3.8), pat=0.2)
    sh.cube("forearm_r", (-8.5, 18.6, -2.4), (3.6, 2.4, 4.4), pat=0.35, polish=0.8, rivet=True)
    sh.cube("forearm_r", (-8.2, 13.4, -1.8), (3.2, 5.4, 3.6), pat=0.2)
    sh.cube("forearm_r", (-8.8, 13.8, -2.3), (4.2, 3.4, 4.6), pat=0.45, rivet=True)
    sh.cube("hand_r", (-8.0, 10.2, -1.6), (3.2, 3.0, 3.2), pat=0.15)
    for i in range(2):
        sh.cube("hand_r", (-8.0 + i * 1.6, 10.0, -2.6), (1.6, 2.0, 1.0), pat=0.1)
    sh.cube("hand_r", (-8.6, 11.6, -1.6), (0.9, 0.9, 1.4), pat=0.1)
    sh.cube("sword", (-6.9, 11.1, -1.0), (1.0, 1.0, 4.6), mat="dark")                    # grip, the fist closes on it
    sh.cube("sword", (-7.4, 10.8, 3.4), (2.0, 1.6, 1.2), mat="gold")                      # pommel
    sh.cube("sword", (-8.2, 10.8, -2.4), (3.6, 1.6, 0.9), mat="gold")                     # cross guard
    sh.cube("sword", (-8.6, 10.5, -2.4), (0.8, 2.2, 0.9), mat="gold")
    sh.cube("sword", (-5.2, 10.5, -2.4), (0.8, 2.2, 0.9), mat="gold")
    sh.cube("sword", (-6.8, 10.3, -8.0), (0.8, 2.6, 5.6), mat="blade")
    sh.cube("sword", (-6.8, 10.5, -12.0), (0.8, 2.2, 4.0), mat="blade")
    sh.cube("sword", (-6.8, 10.8, -14.6), (0.8, 1.6, 2.6), mat="blade")
    sh.cube("sword", (-6.8, 11.1, -16.2), (0.8, 1.0, 1.7), mat="blade", rot=(0, 0, 0))
    sh.cube("sword", (-6.6, 10.9, -12.5), (0.4, 1.2, 8.0), mat="blade", pat=0.0, lum=0.8)

    # -- left arm: same pauldron, upper arm, forearm, fist on the shield grip
    sh.cube("arm_l", (5.8, 24.0, -3.0), (3.8, 2.4, 6.0), rot=(0, 0, -8), pv=(6.4, 26, 0), pat=0.35, rivet=True)
    sh.cube("arm_l", (5.9, 25.6, -3.2), (3.4, 2.0, 6.4), rot=(0, 0, -20), pv=(6.4, 26, 0), pat=0.4, polish=0.8)
    sh.cube("arm_l", (5.2, 27.0, -3.0), (3.0, 1.5, 6.0), rot=(0, 0, -32), pv=(6.4, 26, 0), pat=0.3, polish=0.9)
    sh.cube("arm_l", (4.2, 23.6, -2.2), (3.4, 3.6, 4.4), pat=0.15, mat="dark")
    sh.cube("arm_l", (5.0, 19.8, -1.9), (3.4, 4.8, 3.8), pat=0.2)
    sh.cube("forearm_l", (4.9, 18.6, -2.4), (3.6, 2.4, 4.4), pat=0.35, polish=0.8, rivet=True)
    sh.cube("forearm_l", (5.0, 13.4, -1.8), (3.2, 5.4, 3.6), pat=0.2)
    sh.cube("forearm_l", (4.6, 13.8, -2.3), (4.2, 3.4, 4.6), pat=0.45, rivet=True)
    sh.cube("hand_l", (4.8, 10.2, -1.6), (3.2, 3.0, 3.2), pat=0.15)
    for i in range(2):
        sh.cube("hand_l", (4.8 + i * 1.6, 10.0, -2.6), (1.6, 2.0, 1.0), pat=0.1)

    # -- the shield: four plates turned 0 / 45 / 90 / 135 degrees make the disc, a rim behind, a boss and a star on the front, the grip behind
    # (drawn about the disc centre; the bone's rest rotation cancels the bend of the arm so the disc faces the front)
    # the shield is held like a round viking shield: the forearm points into it, the fist grips a handle behind the boss. In the forearm's own frame
    # (hanging down, -y along the arm) the disc is a horizontal plate below the fist, its face looking down (-y), which the bent arm turns to the front.
    cx, cy, cz = 7.4, 8.6, 0.0
    for k, ang in enumerate((0, 45, 90, 135)):
        sh.cube("shield", (cx - 6.5, cy - 1.3 - k * 0.12, cz - 2.65), (13.0, 1.4, 5.3), mat="shield", front="bottom", R=6.4, rot=(0, ang, 0), pv=(cx, cy, cz))
        sh.cube("shield", (cx - 7.0, cy + 0.25, cz - 3.0), (14.0, 1.0, 6.0), rot=(0, ang, 0), pv=(cx, cy, cz), pat=0.5, lum=0.8)
    sh.cube("shield", (cx - 1.5, cy - 2.9, cz - 1.5), (3.0, 1.4, 3.0), mat="gold", rot=(0, 45, 0), pv=(cx, cy, cz))
    sh.cube("shield", (cx - 0.9, cy - 3.5, cz - 0.9), (1.8, 0.8, 1.8), mat="ember")
    for k, ang in enumerate((0, 90)):
        sh.cube("shield", (cx - 4.4, cy - 1.9 - 0.05 * k, cz - 0.35), (8.8, 0.6, 0.7), mat="gold", rot=(0, ang + 45, 0), pv=(cx, cy, cz))
    sh.cube("shield", (cx - 2.2, cy + 0.9, cz - 0.9), (4.4, 1.2, 1.8), mat="dark")                    # the handle behind

    a = Anim()
    A = {}
    # idle: a slow breath, the crest and the straps sway, the sword hand rests
    L = 3.0
    wave(a, "idle", "chest", L, 12, x=S(1.2), y=S(1.5, 0.25))
    wave(a, "idle", "chest", L, 12, kind="pos", y=S(0.25))
    wave(a, "idle", "head", L, 12, x=S(2.0, 0.1), y=S(4, 0.3))
    wave(a, "idle", "crest", L, 12, x=S(3.0, 0.3))
    wave(a, "idle", "pter_f", L, 12, x=S(2.5, 0.2))
    wave(a, "idle", "pter_b", L, 12, x=S(-2.5, 0.2))
    wave(a, "idle", "pter_r", L, 12, z=S(2.0, 0.3))
    wave(a, "idle", "pter_l", L, 12, z=S(-2.0, 0.3))
    wave(a, "idle", "arm_r", L, 12, x=S(2.0, 0.2, off=-2), z=S(1.0))
    wave(a, "idle", "forearm_r", L, 12, x=S(2.5, 0.35, off=6))
    wave(a, "idle", "arm_l", L, 12, x=S(1.5, 0.1), z=S(-1.0))
    wave(a, "idle", "sword", L, 12, x=S(1.5, 0.4))
    wave(a, "idle", "pelvis", L, 12, y=S(1.2, 0.5))
    # walk: heavy strides, the leg bends at the knee on the way forward, arms counter-swing, the body bobs twice a cycle
    L = 1.0
    ph = lambda p: math.sin(2 * math.pi * p)
    wave(a, "walk", "thigh_r", L, 12, x=lambda p: 30 * ph(p))
    wave(a, "walk", "thigh_l", L, 12, x=lambda p: -30 * ph(p))
    wave(a, "walk", "shin_r", L, 12, x=lambda p: -42 * max(0.0, math.cos(2 * math.pi * p)) - 4)
    wave(a, "walk", "shin_l", L, 12, x=lambda p: -42 * max(0.0, -math.cos(2 * math.pi * p)) - 4)
    wave(a, "walk", "foot_r", L, 12, x=lambda p: 8 * ph(p) + 12 * max(0.0, math.cos(2 * math.pi * p)))
    wave(a, "walk", "foot_l", L, 12, x=lambda p: -8 * ph(p) + 12 * max(0.0, -math.cos(2 * math.pi * p)))
    wave(a, "walk", "arm_r", L, 12, x=lambda p: -26 * ph(p) - 4, z=S(2))
    wave(a, "walk", "forearm_r", L, 12, x=lambda p: 8 + 6 * ph(p))
    wave(a, "walk", "arm_l", L, 12, x=lambda p: 10 * ph(p))
    wave(a, "walk", "chest", L, 12, y=lambda p: 6 * ph(p), x=lambda p: 2)
    wave(a, "walk", "head", L, 12, y=lambda p: -5 * ph(p))
    wave(a, "walk", "pelvis", L, 12, y=lambda p: -4 * ph(p))
    wave(a, "walk", "pelvis", L, 12, kind="pos", y=lambda p: 0.6 * math.cos(4 * math.pi * p) - 0.6)
    wave(a, "walk", "crest", L, 12, x=lambda p: 5 * math.cos(4 * math.pi * p))
    wave(a, "walk", "pter_f", L, 12, x=lambda p: 8 * math.cos(4 * math.pi * p) - 4)
    wave(a, "walk", "pter_b", L, 12, x=lambda p: -8 * math.cos(4 * math.pi * p) + 4)
    # strike: wind up (sword high behind the head), a chop, a follow-through, back to rest (starts and ends at rest so it can loop)
    keys(a, "strike", "arm_r", "rot", {0.0: (-2, 0, 0), 0.30: (150, 0, -10), 0.42: (140, 0, -8), 0.55: (38, 0, 0), 0.70: (24, 0, 0), 1.0: (-2, 0, 0)})
    keys(a, "strike", "forearm_r", "rot", {0.0: (6, 0, 0), 0.30: (40, 0, 0), 0.55: (4, 0, 0), 1.0: (6, 0, 0)})
    keys(a, "strike", "sword", "rot", {0.0: (0, 0, 0), 0.30: (60, 0, 0), 0.55: (-26, 0, 0), 0.70: (-14, 0, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "chest", "rot", {0.0: (0, 0, 0), 0.30: (-8, -14, 0), 0.55: (14, 16, 0), 0.70: (10, 10, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "head", "rot", {0.0: (0, 0, 0), 0.30: (-6, 12, 0), 0.55: (8, -14, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "pelvis", "rot", {0.0: (0, 0, 0), 0.30: (0, -8, 0), 0.55: (0, 10, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "pelvis", "pos", {0.0: (0, 0, 0), 0.30: (0, -0.4, 1.0), 0.55: (0, -0.8, -2.0), 1.0: (0, 0, 0)})
    keys(a, "strike", "thigh_l", "rot", {0.0: (0, 0, 0), 0.30: (-12, 0, 0), 0.55: (26, 0, 0), 0.75: (18, 0, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "thigh_r", "rot", {0.0: (0, 0, 0), 0.30: (10, 0, 0), 0.55: (-12, 0, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "shin_l", "rot", {0.0: (0, 0, 0), 0.55: (-24, 0, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "arm_l", "rot", {0.0: (0, 0, 0), 0.30: (-10, 0, 0), 0.55: (6, 0, 0), 1.0: (0, 0, 0)})
    keys(a, "strike", "crest", "rot", {0.0: (0, 0, 0), 0.30: (-10, 0, 0), 0.55: (24, 0, 0), 0.8: (-8, 0, 0), 1.0: (0, 0, 0)})
    a.clips["strike"]["animation_length"] = 1.0
    # block: the shield comes up and out, the body braces behind it, a shudder on the hit, back to rest
    keys(a, "block", "arm_l", "rot", {0.0: (0, 0, 0), 0.12: (30, 0, 6), 0.45: (36, 0, 6), 0.8: (0, 0, 0)})
    keys(a, "block", "forearm_l", "rot", {0.0: (0, 0, 0), 0.12: (-20, 0, 0), 0.45: (-22, 0, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "shield", "rot", {0.0: (0, 0, 0), 0.12: (-4, 0, 0), 0.2: (3, 0, 4), 0.28: (-3, 0, -4), 0.36: (2, 0, 2), 0.45: (0, 0, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "shield", "pos", {0.0: (0, 0, 0), 0.12: (0, 0, -1), 0.2: (0, 0, 0.8), 0.45: (0, 0, -0.5), 0.8: (0, 0, 0)})
    keys(a, "block", "chest", "rot", {0.0: (0, 0, 0), 0.12: (6, 8, 0), 0.2: (9, 8, 0), 0.45: (6, 8, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "pelvis", "pos", {0.0: (0, 0, 0), 0.12: (0, -0.8, 0.5), 0.2: (0, -0.8, 1.2), 0.45: (0, -0.8, 0.6), 0.8: (0, 0, 0)})
    keys(a, "block", "thigh_l", "rot", {0.0: (0, 0, 0), 0.12: (20, 0, 0), 0.45: (20, 0, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "shin_l", "rot", {0.0: (0, 0, 0), 0.12: (-26, 0, 0), 0.45: (-26, 0, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "thigh_r", "rot", {0.0: (0, 0, 0), 0.12: (-14, 0, 0), 0.45: (-14, 0, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "head", "rot", {0.0: (0, 0, 0), 0.12: (6, 8, 0), 0.45: (6, 8, 0), 0.8: (0, 0, 0)})
    keys(a, "block", "crest", "rot", {0.0: (0, 0, 0), 0.2: (14, 0, 0), 0.45: (-6, 0, 0), 0.8: (0, 0, 0)})
    a.clips["block"]["animation_length"] = 0.8
    # crumble: the knees give, the body folds, the head drops, the sword and the shield fall, the pieces sink (does not loop)
    keys(a, "crumble", "pelvis", "pos", {0.0: (0, 0, 0), 0.3: (0, -3, 1), 0.6: (0, -8, 3), 0.9: (0, -13, 4), 1.2: (0, -15.5, 4)})
    keys(a, "crumble", "pelvis", "rot", {0.0: (0, 0, 0), 0.4: (10, 0, 4), 0.9: (30, 0, 10), 1.2: (36, 0, 12)})
    keys(a, "crumble", "thigh_r", "rot", {0.0: (0, 0, 0), 0.5: (50, 0, -6), 1.2: (80, 0, -14)})
    keys(a, "crumble", "thigh_l", "rot", {0.0: (0, 0, 0), 0.5: (44, 0, 6), 1.2: (90, 0, 22)})
    keys(a, "crumble", "shin_r", "rot", {0.0: (0, 0, 0), 0.5: (-70, 0, 0), 1.2: (-110, 0, 0)})
    keys(a, "crumble", "shin_l", "rot", {0.0: (0, 0, 0), 0.5: (-60, 0, 0), 1.2: (-100, 0, 0)})
    keys(a, "crumble", "chest", "rot", {0.0: (0, 0, 0), 0.5: (24, 0, -8), 1.2: (40, 12, -14)})
    keys(a, "crumble", "head", "rot", {0.0: (0, 0, 0), 0.5: (14, -20, 6), 1.2: (30, -40, 14)})
    keys(a, "crumble", "arm_r", "rot", {0.0: (0, 0, 0), 0.6: (60, 0, -30), 1.2: (100, 0, -60)})
    keys(a, "crumble", "arm_l", "rot", {0.0: (0, 0, 0), 0.6: (-10, 0, 40), 1.2: (-30, 0, 70)})
    keys(a, "crumble", "sword", "rot", {0.0: (0, 0, 0), 0.6: (10, 0, 40), 1.2: (30, 0, 90)})
    keys(a, "crumble", "sword", "pos", {0.0: (0, 0, 0), 0.6: (0, -5, -2), 1.2: (0, -9, -4)})
    keys(a, "crumble", "shield", "pos", {0.0: (0, 0, 0), 0.5: (1, -6, -1), 1.2: (6, -14, -3)})
    keys(a, "crumble", "shield", "rot", {0.0: (0, 0, 0), 0.6: (0, 0, 20), 1.2: (30, 0, 55)})
    keys(a, "crumble", "crest", "rot", {0.0: (0, 0, 0), 0.6: (20, 0, 0), 1.2: (50, 0, 0)})
    keys(a, "crumble", "root", "scale", {0.0: (1, 1, 1), 0.9: (1, 1, 1), 1.2: (1, 0.72, 1)})
    a.clips["crumble"]["loop"] = False
    a.clips["crumble"]["animation_length"] = 1.2
    sh.save(a)


# ================================================================================================================================
# 2. THE POISON LIZARD
# ================================================================================================================================
def build_lizard():
    sh = Sheet("bronze", "lizard", 192, 96, seed=23)
    sh.bone("root", pivot=(0, 0, 0))
    sh.bone("body", "root", (0, 6, 0))
    sh.bone("chest", "body", (0, 6, -6))
    sh.bone("neck", "chest", (0, 6.8, -11.5))
    sh.bone("head", "neck", (0, 7.0, -14.0))
    sh.bone("jaw", "head", (0, 6.0, -15.0))
    sh.bone("tongue", "jaw", (0, 6.4, -19.5))
    tails = [(0, 6, 6), (0, 5.8, 11), (0, 5.5, 15.5), (0, 5.2, 19.5), (0, 4.9, 23), (0, 4.6, 26)]
    sh.bone("tail1", "body", tails[0])
    for i in range(1, 6):
        sh.bone("tail%d" % (i + 1), "tail%d" % i, tails[i])
    legs = {"fr": (-4.2, 5.2, -8.0), "fl": (4.2, 5.2, -8.0), "br": (-4.2, 5.2, 4.0), "bl": (4.2, 5.2, 4.0)}
    for k, p in legs.items():
        sx = -1 if k[1] == "r" else 1
        sh.bone("leg_" + k, "chest" if k[0] == "f" else "body", p)
        sh.bone("shank_" + k, "leg_" + k, (p[0] + sx * 3.2, 4.8, p[2]))
        sh.bone("foot_" + k, "shank_" + k, (p[0] + sx * 3.4, 1.0, p[2]))

    # body, belly plates, dorsal ridge with scutes
    sh.cube("body", (-4.0, 4.4, -6.0), (8.0, 4.4, 12.0), pat=0.15, polish=0.6)
    sh.cube("body", (-3.4, 3.8, -5.0), (6.8, 1.0, 10.0), mat="dark", pat=0.10)
    sh.cube("body", (-4.3, 7.8, -4.0), (8.6, 1.4, 8.0), pat=0.25, rivet=True)
    for i in range(5):
        sh.cube("body", (-0.8, 9.0, -5.0 + i * 2.4), (1.6, 1.4, 1.6), rot=(0, 0, 0), pat=0.20, polish=0.9, mat="bronze")
    sh.cube("chest", (-3.6, 4.8, -12.0), (7.2, 4.0, 6.4), pat=0.15, polish=0.6)
    sh.cube("chest", (-3.0, 4.2, -11.0), (6.0, 1.0, 5.4), mat="dark")
    for i in range(3):
        sh.cube("chest", (-0.8, 8.8, -11.4 + i * 2.4), (1.6, 1.3, 1.6), pat=0.20, polish=0.9, mat="bronze")
    sh.cube("neck", (-2.6, 5.6, -14.4), (5.2, 3.8, 3.4), pat=0.17)
    sh.cube("neck", (-2.0, 9.2, -14.0), (4.0, 0.9, 2.8), pat=0.25, mat="bronze")
    # head: skull, brow ridges, snout, nostrils, eyes with lids, horns, cheek spikes, teeth, the tongue
    sh.cube("head", (-3.0, 5.4, -17.0), (6.0, 3.8, 3.3), pat=0.15, polish=0.7)
    sh.cube("head", (-2.4, 5.6, -20.6), (4.8, 3.0, 3.8), pat=0.15, polish=0.7)
    sh.cube("head", (-1.5, 6.4, -21.4), (3.0, 2.2, 1.0), pat=0.17)
    sh.cube("head", (-2.8, 8.9, -17.2), (5.6, 0.9, 3.0), pat=0.25, rivet=True)
    sh.pair("head", "head", [
        dict(o=(-3.5, 7.4, -17.0), s=(1.6, 1.8, 2.2), pat=0.20),
        dict(o=(-3.3, 7.8, -17.2), s=(1.2, 1.2, 0.8), mat="eye"),
        dict(o=(-3.6, 8.7, -15.4), s=(1.0, 0.8, 2.6), pat=0.15, rot=(0, -12, 0), pv=(-3.0, 8.8, -15.0)),
        dict(o=(-3.7, 6.4, -15.6), s=(0.9, 0.9, 2.2), pat=0.25, rot=(0, 20, 0), pv=(-3.0, 6.6, -15.0), polish=0.9),
        dict(o=(-1.3, 8.1, -21.6), s=(0.6, 0.6, 0.5), mat="dark"),
        dict(o=(-2.2, 5.0, -20.2), s=(0.6, 1.0, 0.6), mat="tooth"),
        dict(o=(-2.4, 5.0, -18.0), s=(0.6, 1.2, 0.6), mat="tooth"),
        dict(o=(-1.2, 5.0, -21.0), s=(0.6, 0.8, 0.6), mat="tooth"),
    ])
    sh.cube("jaw", (-2.4, 3.8, -20.2), (4.8, 1.2, 4.2), pat=0.15)
    sh.cube("jaw", (-2.0, 3.2, -17.2), (4.0, 1.3, 2.4), pat=0.15, polish=0.5)
    sh.cube("jaw", (-1.3, 5.0, -19.6), (2.6, 0.5, 3.4), mat="venom")
    sh.pair("jaw", "jaw", [dict(o=(-1.9, 5.0, -20.0), s=(0.5, 0.8, 0.5), mat="tooth"), dict(o=(-1.7, 5.0, -18.4), s=(0.5, 0.7, 0.5), mat="tooth")])
    sh.cube("tongue", (-0.5, 5.2, -22.0), (1.0, 0.4, 2.6), mat="venom")
    sh.cube("tongue", (-0.9, 5.2, -24.4), (0.4, 0.4, 2.0), mat="venom", rot=(0, 18, 0), pv=(-0.5, 5.4, -24.2))
    sh.cube("tongue", (0.5, 5.2, -24.4), (0.4, 0.4, 2.0), mat="venom", rot=(0, -18, 0), pv=(0.5, 5.4, -24.2))
    # tail: tapering segments, a dorsal comb, a spiked bronze tip
    sizes = [(6.6, 3.8, 5.4), (5.4, 3.2, 5.2), (4.4, 2.7, 4.8), (3.4, 2.3, 4.0), (2.6, 1.9, 3.8), (1.8, 1.5, 3.4)]
    for i, (w, h, d) in enumerate(sizes):
        z0 = tails[i][2] - 0.2
        y0 = tails[i][1] - h * 0.5
        sh.cube("tail%d" % (i + 1), (-w / 2, y0, z0), (w, h, d), pat=0.15 + 0.05 * i, polish=0.5)
        sh.cube("tail%d" % (i + 1), (-0.6, y0 + h, z0 + 0.4), (1.2, 1.1, d - 1.0), pat=0.23, polish=0.9, mat="bronze")
    sh.cube("tail6", (-0.5, 4.0, 29.2), (1.0, 1.0, 1.8), mat="bronze", polish=0.9)
    sh.cube("tail6", (-0.4, 3.9, 30.7), (0.8, 0.8, 1.2), mat="blade")
    # legs: shoulder, shank, foot with three toes and claws
    for k, p in legs.items():
        sx = -1 if k[1] == "r" else 1
        x0 = p[0] if sx > 0 else p[0] - 3.4
        sh.cube("leg_" + k, (x0, p[1] - 1.4, p[2] - 1.3), (3.4, 2.4, 2.6), pat=0.17)
        xs = p[0] + sx * 3.2
        sh.cube("shank_" + k, (xs - 0.9, 1.2, p[2] - 1.0), (1.8, 4.4, 2.0), pat=0.15)
        sh.cube("shank_" + k, (xs - 1.1, 3.6, p[2] - 1.3), (2.2, 1.0, 2.6), pat=0.25, polish=0.8)
        xf = p[0] + sx * 3.4
        sh.cube("foot_" + k, (xf - 1.5, 0.0, p[2] - 1.2), (3.0, 1.0, 3.0), pat=0.15)
        for t in (-1, 0, 1):
            sh.cube("foot_" + k, (xf + t * 1.0 - 0.4, 0.0, p[2] - 3.4), (0.8, 0.8, 2.2), pat=0.10)
            sh.cube("foot_" + k, (xf + t * 1.0 - 0.25, 0.0, p[2] - 4.4), (0.5, 0.5, 1.1), mat="tooth")

    a = Anim()
    # idle: breath, a tongue flick, the head looks about, the tail drifts
    L = 2.4
    wave(a, "idle", "chest", L, 12, x=S(1.0), y=S(2.0, 0.2))
    wave(a, "idle", "neck", L, 12, y=S(6.0, 0.35), x=S(1.5, 0.1))
    wave(a, "idle", "head", L, 12, y=S(5.0, 0.55), x=S(1.0, 0.3))
    wave(a, "idle", "jaw", L, 12, x=lambda p: -3 * max(0.0, math.sin(2 * math.pi * p * 2)))
    wave(a, "idle", "tongue", L, 12, kind="scale", z=lambda p: 1.0 + 0.9 * max(0.0, math.sin(2 * math.pi * p * 2 + 1.0)) ** 4)
    for i in range(1, 7):
        wave(a, "idle", "tail%d" % i, L, 12, y=S(5.0 + i, 0.1 - 0.06 * i))
    wave(a, "idle", "body", L, 12, kind="pos", y=S(0.15))
    # run: diagonal gait, an S-wave through the spine, the tail whips
    L = 0.5
    ph = lambda p: math.sin(2 * math.pi * p)
    for k, sgn in (("fr", 1), ("bl", 1), ("fl", -1), ("br", -1)):
        sx = -1 if k[1] == "r" else 1
        wave(a, "run", "leg_" + k, L, 12, y=lambda p, s=sgn, x=sx: -s * 32 * ph(p) * x * -1, z=lambda p, s=sgn, x=sx: x * 8 * max(0.0, s * math.cos(2 * math.pi * p)))
        wave(a, "run", "shank_" + k, L, 12, z=lambda p, s=sgn, x=sx: x * 28 * max(0.0, s * math.cos(2 * math.pi * p)))
        wave(a, "run", "foot_" + k, L, 12, z=lambda p, s=sgn, x=sx: x * -12 * max(0.0, s * math.cos(2 * math.pi * p)))
    wave(a, "run", "chest", L, 12, y=lambda p: 9 * ph(p))
    wave(a, "run", "body", L, 12, y=lambda p: -7 * ph(p))
    wave(a, "run", "neck", L, 12, y=lambda p: -8 * ph(p), x=lambda p: 4)
    wave(a, "run", "head", L, 12, y=lambda p: 5 * ph(p), x=lambda p: -3)
    for i in range(1, 7):
        wave(a, "run", "tail%d" % i, L, 12, y=lambda p, i=i: (8 + 2 * i) * math.sin(2 * math.pi * p - 0.5 * i))
    wave(a, "run", "body", L, 12, kind="pos", y=lambda p: 0.35 * math.cos(4 * math.pi * p))
    wave(a, "run", "jaw", L, 12, x=lambda p: -6 * max(0.0, ph(p)))
    # bite: the head draws back and rises, the jaws open, the lunge, the snap, back to rest
    keys(a, "bite", "head", "rot", {0.0: (0, 0, 0), 0.14: (-14, 0, 0), 0.30: (18, 0, 0), 0.40: (22, 0, 0), 0.60: (0, 0, 0)})
    keys(a, "bite", "neck", "rot", {0.0: (0, 0, 0), 0.14: (-18, 0, 0), 0.30: (22, 0, 0), 0.40: (26, 0, 0), 0.60: (0, 0, 0)})
    keys(a, "bite", "jaw", "rot", {0.0: (0, 0, 0), 0.14: (-48, 0, 0), 0.30: (-52, 0, 0), 0.38: (0, 0, 0), 0.48: (-8, 0, 0), 0.60: (0, 0, 0)})
    keys(a, "bite", "chest", "rot", {0.0: (0, 0, 0), 0.14: (-10, 0, 0), 0.30: (12, 0, 0), 0.60: (0, 0, 0)})
    keys(a, "bite", "body", "pos", {0.0: (0, 0, 0), 0.14: (0, -0.4, 1.4), 0.30: (0, 0.6, -3.2), 0.40: (0, 0.2, -3.6), 0.60: (0, 0, 0)})
    keys(a, "bite", "tongue", "scale", {0.0: (1, 1, 1), 0.14: (1, 1, 2.0), 0.30: (1, 1, 1.2), 0.60: (1, 1, 1)})
    for i in range(1, 7):
        keys(a, "bite", "tail%d" % i, "rot", {0.0: (0, 0, 0), 0.30: (-4, 8 * (1 if i % 2 else -1), 0), 0.60: (0, 0, 0)})
    for k in ("fr", "fl"):
        keys(a, "bite", "leg_" + k, "rot", {0.0: (0, 0, 0), 0.30: (-10, 0, 0), 0.60: (0, 0, 0)})
    a.clips["bite"]["animation_length"] = 0.6
    # spit: the head lifts, the throat swells, the jaws gape and snap shut (loops softly)
    L = 0.9
    keys(a, "spit", "head", "rot", {0.0: (0, 0, 0), 0.25: (-24, 0, 0), 0.5: (-30, 0, 0), 0.62: (14, 0, 0), 0.9: (0, 0, 0)})
    keys(a, "spit", "neck", "rot", {0.0: (0, 0, 0), 0.25: (-20, 0, 0), 0.5: (-26, 0, 0), 0.62: (10, 0, 0), 0.9: (0, 0, 0)})
    keys(a, "spit", "jaw", "rot", {0.0: (0, 0, 0), 0.25: (-30, 0, 0), 0.5: (-56, 0, 0), 0.62: (-4, 0, 0), 0.9: (0, 0, 0)})
    keys(a, "spit", "neck", "scale", {0.0: (1, 1, 1), 0.25: (1.15, 1.2, 1), 0.5: (1.3, 1.35, 1), 0.62: (0.95, 0.95, 1), 0.9: (1, 1, 1)})
    keys(a, "spit", "chest", "rot", {0.0: (0, 0, 0), 0.5: (-10, 0, 0), 0.62: (6, 0, 0), 0.9: (0, 0, 0)})
    a.clips["spit"]["animation_length"] = 0.9
    sh.save(a)


# ================================================================================================================================
# 3. THE ARMOUR PIECES OF THE AURA (humanoid proportions; drawn at the limb's pose by the aura painter)
# ================================================================================================================================
def build_pauldrons():
    for side, sx in (("r", -1), ("l", 1)):
        sh = Sheet("bronze", "pauldron_" + side, 96, 64, seed=31 + (sx > 0))
        sh.bone("root", pivot=(0, 22, 0))
        sh.bone("pad", "root", (sx * 5.5, 22, 0))
        sh.bone("spike", "pad", (sx * 9.2, 23.5, 0))

        # lames: written for the right side (-x), mirrored for the left
        specs = [
            dict(o=(-10.6, 21.0, -3.6), s=(5.2, 1.7, 7.2), rot=(0, 0, 10), pv=(-5.5, 22, 0), pat=0.35, rivet=True, polish=0.8),
            dict(o=(-10.3, 22.5, -3.5), s=(4.8, 1.7, 7.0), rot=(0, 0, 18), pv=(-5.5, 22, 0), pat=0.4, rivet=True, polish=0.8),
            dict(o=(-9.4, 24.0, -3.4), s=(4.2, 1.6, 6.8), rot=(0, 0, 28), pv=(-5.5, 22, 0), pat=0.35, polish=0.9),
            dict(o=(-8.0, 25.4, -3.0), s=(3.4, 1.3, 6.0), rot=(0, 0, 40), pv=(-5.5, 22, 0), pat=0.3, polish=0.9),
            dict(o=(-6.8, 25.4, -2.3), s=(2.6, 1.2, 4.6), pat=0.25, polish=1.0),
        ]
        for sp in specs:
            sp = dict(sp)
            o, s = sp.pop("o"), sp.pop("s")
            rot, pv = sp.pop("rot", None), sp.pop("pv", None)
            if sx > 0:
                o = (-(o[0] + s[0]), o[1], o[2])
                rot = None if rot is None else (rot[0], -rot[1], -rot[2])
                pv = None if pv is None else (-pv[0], pv[1], pv[2])
            sh.cube("pad", o, s, rot=rot, pv=pv, **sp)
        for zz in (-2.4, 0.0, 2.4):
            ox = sx * 9.7 - 0.5
            sh.cube("pad", (ox, 22.3 + 0.9 * (abs(zz) < 1), zz - 0.5), (1.0, 1.0, 1.0), mat="gold")
        sh.cube("spike", (sx * 10.4 - 0.9, 22.8, -0.9), (1.8, 1.8, 1.8), mat="bronze", rot=(0, 0, 45 * -sx), pv=(sx * 10.0, 23.5, 0), polish=0.9, pat=0.2)
        sh.cube("spike", (sx * 12.0 - 0.45, 23.1, -0.45), (0.9, 0.9, 1.0), mat="blade")
        a = Anim()
        wave(a, "idle", "spike", 3.0, 12, z=S(2.0))
        sh.save(a)


def build_crest():
    sh = Sheet("bronze", "crest", 128, 96, seed=41)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("comb", "root", (0, 33.5, 0))
    # an open-faced helm (the face stays visible): dome, brow, nasal bar, cheek plates, neck guard, a horsehair-style comb
    sh.cube("root", (-5.1, 29.6, -5.1), (10.2, 2.9, 10.2), pat=0.3, polish=0.9, rivet=True)
    sh.cube("root", (-4.5, 32.2, -4.5), (9.0, 1.4, 9.0), pat=0.25, polish=1.0)
    sh.cube("root", (-5.2, 28.2, -5.4), (10.4, 1.4, 1.2), pat=0.4, rivet=True)
    sh.cube("root", (-0.8, 24.4, -5.5), (1.6, 4.4, 1.0), pat=0.35, polish=0.9)
    sh.pair("root", "root", [dict(o=(-5.4, 24.2, -3.4), s=(1.2, 4.6, 6.0), pat=0.4, rivet=True),
                             dict(o=(-5.1, 28.6, -1.0), s=(0.9, 1.0, 1.0), mat="gold")])
    sh.cube("root", (-4.9, 26.0, 4.5), (9.8, 3.8, 1.2), pat=0.4, rivet=True)
    sh.cube("comb", (-0.8, 33.4, -5.6), (1.6, 1.0, 11.2), pat=0.3)
    for i, hgt in enumerate((2.2, 3.6, 4.6, 5.0, 4.6, 3.6, 2.2)):
        sh.cube("comb", (-0.7, 34.3, -5.4 + i * 1.55), (1.4, hgt, 1.2), pat=0.28, mat="bronze" if i % 2 else "dark")
    a = Anim()
    wave(a, "idle", "comb", 3.0, 12, x=S(3.0, 0.2))
    sh.save(a)


def build_breast():
    sh = Sheet("bronze", "breast", 128, 96, seed=51)
    sh.bone("root", pivot=(0, 24, 0))
    sh.bone("sash", "root", (0, 18, 0))
    # gorget ring, medallion with a warm glowing centre, belt buckle, the diagonal strap, a spine ridge
    sh.cube("root", (-6.0, 22.0, -3.8), (12.0, 1.6, 7.6), pat=0.4, polish=0.9, rivet=True)
    sh.cube("root", (-5.0, 23.4, -3.2), (10.0, 1.0, 6.4), pat=0.3, polish=1.0)
    sh.cube("root", (-2.6, 15.6, -3.6), (5.2, 5.2, 0.8), mat="gold", rot=(0, 0, 45), pv=(0, 18.2, -3.4))
    sh.cube("root", (-1.5, 16.7, -4.2), (3.0, 3.0, 0.6), mat="ember", rot=(0, 0, 45), pv=(0, 18.2, -4.0))
    sh.cube("root", (-2.6, 10.6, -3.4), (5.2, 2.6, 0.9), mat="gold")
    sh.cube("root", (-6.2, 11.6, -3.4), (12.4, 1.4, 6.8), pat=0.4, rivet=True)
    sh.cube("root", (-0.9, 12.0, 2.6), (1.8, 10.4, 1.0), pat=0.3, polish=0.9)
    sh.cube("sash", (-0.9, 12.0, -3.0), (1.8, 12.5, 0.7), rot=(0, 0, 38), pv=(0, 18, -3.0), mat="dark")
    for i in range(4):
        sh.cube("sash", (-0.6 + (i - 1.5) * 2.1 * 0.62, 18.0 + (i - 1.5) * 2.1 * 0.79 - 0.5, -3.9), (1.0, 1.0, 0.6), mat="gold")
    a = Anim()
    wave(a, "idle", "sash", 3.0, 12, z=S(1.0))
    sh.save(a)


# ================================================================================================================================
# 4. THE AURA SKINS (64 x 64 player skin layout)
# ================================================================================================================================
# part -> (u, v, w, h, d) of the base layer and of the overlay layer (hat, jacket, sleeves, pants)
PARTS = {
    "head": ((0, 0, 8, 8, 8), (32, 0, 8, 8, 8)),
    "body": ((16, 16, 8, 12, 4), (16, 32, 8, 12, 4)),
    "rarm": ((40, 16, 4, 12, 4), (40, 32, 4, 12, 4)),
    "larm": ((32, 48, 4, 12, 4), (48, 48, 4, 12, 4)),
    "rleg": ((0, 16, 4, 12, 4), (0, 32, 4, 12, 4)),
    "lleg": ((16, 48, 4, 12, 4), (0, 48, 4, 12, 4)),
}


def skin_plate(style):
    """style 'plate': plates with gaps (cutout), rivets, verdigris; style 'cast': every texel solid, smooth cast bronze."""
    rng = np.random.default_rng(97 if style == "plate" else 98)
    img = np.zeros((64, 64, 4))
    glow = np.zeros((64, 64, 4))

    def put(x0, y0, W, H, rgb, g=None, a=255, mask=None):
        m = np.ones((H, W), bool) if mask is None else mask
        for c in range(3):
            img[y0:y0 + H, x0:x0 + W, c] = np.where(m, np.clip(rgb[..., c], 0, 255), img[y0:y0 + H, x0:x0 + W, c])
        img[y0:y0 + H, x0:x0 + W, 3] = np.where(m, a, img[y0:y0 + H, x0:x0 + W, 3])
        if g is not None:
            for c in range(3):
                glow[y0:y0 + H, x0:x0 + W, c] = np.where(m, g[..., c], glow[y0:y0 + H, x0:x0 + W, c])
            glow[y0:y0 + H, x0:x0 + W, 3] = np.where(m, 255, glow[y0:y0 + H, x0:x0 + W, 3])

    for part, (base, over) in PARTS.items():
        for layer, (u, v, w, h, d) in (("base", base), ("over", over)):
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                W, H = int(fw), int(fh)
                if style == "cast":
                    # smooth: low grain, strong polish, long soft highlights, joints as darker lines, verdigris only in a few creases
                    rgb, g = bronze_face(face, W, H, rng, dict(grain=0.16, polish=1.0, pat=0.0, tone=0.04 if layer == "base" else 0.1, lum=1.0 if layer == "base" else 1.05))
                    yy = np.arange(H)[:, None] * np.ones((1, W))
                    if part in ("rarm", "larm", "rleg", "lleg"):
                        row = {"rarm": 4, "larm": 4, "rleg": 6, "lleg": 6}[part]
                        rgb = np.where(((yy == row) & (face not in ("top", "bottom")))[..., None], rgb * 0.7, rgb)
                    if part == "body" and face in ("north", "south"):
                        cx = (W - 1) / 2
                        xx = np.arange(W)[None, :] * np.ones((H, 1))
                        rgb = np.where(((np.abs(xx - cx) < 0.6) & (face == "north"))[..., None], rgb * 0.82, rgb)
                    gl = np.zeros((H, W, 3))
                    if face == "north" and part in ("body", "head"):
                        gl[...] = 0
                    put(int(fx), int(fy), W, H, rgb, gl)
                    continue
                # plate-and-rivet armour: the face is cut up into plates by part
                mask = plate_mask(part, face, W, H, layer)
                if mask is None or not mask.any():
                    continue
                rgb, g = bronze_face(face, W, H, rng, dict(rivet=(layer == "base" and W >= 5 and H >= 5), pat=0.3, polish=0.9))
                # a one-pixel dark outline round each plate and a lit upper rim
                inside = mask.copy()
                inner = np.zeros_like(mask)
                inner[1:-1, 1:-1] = mask[1:-1, 1:-1] & mask[:-2, 1:-1] & mask[2:, 1:-1] & mask[1:-1, :-2] & mask[1:-1, 2:]
                rim = inside & ~inner
                rgb = np.where(rim[..., None], rgb * 0.55, rgb)
                upper = np.zeros_like(mask)
                upper[1:, :] = mask[1:, :] & ~mask[:-1, :]
                rgb = np.where(upper[..., None], np.minimum(255, rgb * 1.35 + 30), rgb)
                gl = np.zeros((H, W, 3))
                rimglow = rim & ~upper
                gl[..., 0], gl[..., 1], gl[..., 2] = rimglow * 150, rimglow * 80, rimglow * 20
                put(int(fx), int(fy), W, H, rgb, gl, mask=mask)
    im = Image.fromarray(img.astype(np.uint8), "RGBA")
    gl = Image.fromarray(glow.astype(np.uint8), "RGBA")
    return im, gl


def plate_mask(part, face, W, H, layer):
    """Which texels of one face of one skin part are armour plates (True) and which stay clear (the player shows through)."""
    m = np.zeros((H, W), bool)
    yy = np.arange(H)[:, None] * np.ones((1, W), int)
    xx = np.arange(W)[None, :] * np.ones((H, 1), int)
    flat = face in ("top", "bottom")
    if part == "head":
        # an open helm: the crown, the sides above the ears and the back; the face stays clear except a brow band and a nose bar
        if layer == "over":
            return None
        if face in ("top",):
            m[:, :] = True
        elif face == "north":
            m[:2, :] = True
            m[:5, W // 2 - 1:W // 2 + 1] = True
        elif face in ("west", "east"):
            m[:3, :] = True
            m[:, :2] = face == "east"
            m[:, W - 2:] = face == "west"
        elif face == "south":
            m[:6, :] = True
        return m
    if part == "body":
        if layer == "over":
            if face == "north":
                m[:2, :] = True                                   # collar line
                m[H - 3:H - 1, :] = True                          # belt
            elif face == "south":
                m[H - 3:H - 1, :] = True
            return m
        if flat:
            m[:, :] = True
            return m
        if face in ("north", "south"):
            m[:, :] = True
            m[(yy % 3 == 2) & (yy > H // 2 - 1) & (yy < H - 3)] = False          # gaps between the lames of the belly
            m[H - 3:H - 1, :] = False
            if face == "north":
                m[:, W // 2 - 1:W // 2 + 1] &= (yy[:, W // 2 - 1:W // 2 + 1] % 2 == 0) | (yy[:, W // 2 - 1:W // 2 + 1] < 4)
            return m
        m[:H - 3, 1:W - 1] = True
        return m
    if part in ("rarm", "larm"):
        if layer == "over":
            if face in ("north", "south", "west", "east"):
                m[:2, :] = True                                   # the pauldron's lower rim
            return m
        if flat:
            m[:, :] = True
            return m
        m[:5, :] = True                                           # upper arm plate
        m[5:7, :] = False
        m[7:H, 1:W - 1] = True                                    # bracer
        return m
    if part in ("rleg", "lleg"):
        if layer == "over":
            if face in ("north", "south", "west", "east"):
                m[5:7, :] = True                                  # the knee cap
            return m
        if flat:
            m[:, :] = True
            return m
        m[:5, :] = True                                           # cuisse
        m[5:7, :] = False
        m[7:H - 1, :] = True                                      # greave
        m[H - 1:, :] = False
        return m
    return None


def aura_sheen():
    """Two travelling highlight bands for the statue sheen (additive, white-gold on black), laid out on the skin."""
    rng = np.random.default_rng(5)
    img = np.zeros((64, 64, 4))
    for part, (base, over) in PARTS.items():
        for (u, v, w, h, d) in (base, over):
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                W, H = int(fw), int(fh)
                yy, xx = np.mgrid[0:H, 0:W]
                diag = (xx * 0.8 + yy * 1.1) / 3.0
                band = np.clip(1.0 - np.abs(((diag + 0.0) % 5.0) - 1.2) / 1.0, 0, 1) ** 2
                edge = (yy == 0) * 0.5
                val = np.clip(band * 0.85 + edge, 0, 1)
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, 0] = 255 * val
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, 1] = 214 * val
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, 2] = 140 * val
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, 3] = 255
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def aura_patina():
    """Translucent verdigris crust for style 1: patches that gather low on the body and at the joints."""
    rng = np.random.default_rng(6)
    img = np.zeros((64, 64, 4))
    for part, (base, over) in PARTS.items():
        for (u, v, w, h, d) in (base, over):
            for face, (fx, fy, fw, fh) in face_rects(u, v, w, h, d).items():
                W, H = int(fw), int(fh)
                yy, xx = np.mgrid[0:H, 0:W]
                ed = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy))
                m = vnoise(H, W, rng, 3) * 0.6 + vnoise(H, W, rng, 1.5) * 0.25 + np.exp(-ed / 1.2) * 0.35 + (yy / max(1, H - 1)) * 0.3
                a = np.clip((m - 0.78) / 0.07, 0, 1)
                col = ramp(VERD, 0.3 + 0.5 * vnoise(H, W, rng, 2))
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, :3] = col
                img[int(fy):int(fy) + H, int(fx):int(fx) + W, 3] = a * 235
    return Image.fromarray(img.astype(np.uint8), "RGBA")


def save_aura_textures():
    out = os.path.join(ROOT, "textures", "aura")
    os.makedirs(out, exist_ok=True)
    for style, name in (("plate", "bronze_plate"), ("cast", "bronze_cast")):
        im, gl = skin_plate(style)
        im.save(os.path.join(out, name + ".png"))
        gl.save(os.path.join(out, name + "_glow.png"))
        print("wrote", name, im.size)
    aura_patina().save(os.path.join(out, "bronze_patina.png"))
    aura_sheen().save(os.path.join(out, "bronze_sheen.png"))
    print("wrote bronze_patina, bronze_sheen")


if __name__ == "__main__":
    build_statue()
    build_lizard()
    build_pauldrons()
    build_crest()
    build_breast()
    save_aura_textures()

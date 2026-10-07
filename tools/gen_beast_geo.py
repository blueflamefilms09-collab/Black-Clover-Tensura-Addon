"""Builds the Beast Magic models with tools/geo_builder.py (GeoLite files: geo + animation + texture + glow map per model).

    python3 -B tools/gen_beast_geo.py              # every model and the aura skin
    python3 -B tools/gen_beast_geo.py spirit_beast  # only the named ones (see MODELS)
    python3 -B tools/gen_beast_geo.py --sheets      # also writes the unwrapped texture sheets to build/beast_sheets/ (uv check)

Models (key "beast"; assets/nusmp/geo/entity/beast_<name>.geo.json, animations/entity/..., textures/entity/beast_<name>.png + _glow.png):
  spirit_beast  the summoned spectral lion-wolf hybrid made of beast-fire: cel-shaded orange fur with dark stripes, a ragged flame mane
                and tail, claws, glowing slit eyes. Clips idle / run / attack (pounce and bite) / appear / vanish.
  spirit_bear   the same rig as a heavy bear (hump, round ears, shaggy flame fur, short tail)           (PropKind.BEAST_1 param 1)
  spirit_rhino  the same rig as an armoured rhino (two horns, plates along the back, thick legs)        (PropKind.BEAST_1 param 2)
  form_head     Beast Form: ears, brow, cheek ruffs and a flame crest fitted on the player's head (Space.PLAYER, turns with the head)
  form_body     Beast Form: fur collar, back ridge, hip fur and a long flame tail (Space.PLAYER)
  form_claw     Beast Form: the clawed hand and fur cuff of one arm, authored in the arm's own frame (symmetric: used for both arms)
  form_ghost    Beast Form style 1: a ghost lion head and shoulders (translucent body, additive glow) that hovers over the player
Texture: textures/aura/beast_skin.png (+ _glow): the fur skin overlay for the player model (64x64 skin layout).

Style: every surface is cel-shaded (three or four flat bands, a dark ink line along each face edge), fur is drawn in vertical strands with
tiger-like dark stripes that glow like embers in the glow map, flame cards are ragged tongues with a hot yellow core, an orange body and a dark red
rim (the palette of the owner's anime still, the same as tools/gen_beast_textures.py). Flame cards are flat zero-thickness cubes with their own
per-face uv rectangle (denser than the 1 px / unit of the boxes). Deterministic (fixed seeds), no fonts, no external images.
"""
import math
import os
import sys
import zlib

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np
from PIL import Image

from geo_builder import Model, Anim, ROOT

SHEETS = "--sheets" in sys.argv

# ================================================================================================ palettes
FLAME_HOT = (255, 248, 200)
FLAME_CORE = (255, 206, 66)
FLAME_BODY = (255, 128, 24)
FLAME_EDGE = (206, 52, 12)
FLAME_LINE = (116, 24, 8)

PAL = {
    "lion": dict(
        fur=[(104, 30, 14), (196, 66, 20), (244, 122, 34), (255, 184, 80)],
        belly=[(176, 86, 40), (232, 150, 80), (255, 204, 136), (255, 234, 186)],
        stripe=(80, 18, 10), dark=[(30, 8, 6), (58, 16, 10), (92, 28, 16)],
        plate=[(70, 24, 14), (140, 50, 22), (214, 96, 32), (255, 168, 70)], horn=[(120, 92, 66), (206, 176, 130), (252, 238, 204)]),
    "bear": dict(
        fur=[(60, 16, 10), (124, 38, 16), (184, 66, 24), (236, 116, 42)],
        belly=[(112, 46, 26), (168, 84, 42), (212, 128, 68), (240, 168, 98)],
        stripe=(36, 8, 6), dark=[(26, 6, 5), (50, 14, 9), (84, 24, 14)],
        plate=[(70, 24, 14), (140, 50, 22), (214, 96, 32), (255, 168, 70)], horn=[(120, 92, 66), (206, 176, 130), (252, 238, 204)]),
    "rhino": dict(
        fur=[(78, 24, 16), (148, 50, 22), (208, 90, 30), (248, 148, 62)],
        belly=[(130, 66, 38), (186, 110, 62), (226, 156, 92), (248, 196, 124)],
        stripe=(46, 12, 8), dark=[(28, 8, 6), (54, 16, 10), (88, 26, 14)],
        plate=[(44, 34, 36), (90, 66, 58), (146, 106, 80), (212, 168, 124)], horn=[(70, 50, 44), (150, 110, 80), (236, 200, 150), (255, 240, 200)]),
}
CLAW = [(120, 92, 66), (206, 176, 130), (252, 238, 204)]
FACE_LIGHT = {"top": 1.14, "bottom": 0.55, "north": 0.98, "south": 0.84, "east": 0.9, "west": 0.9}


# ================================================================================================ noise and small helpers
def rng_for(*key):
    return np.random.default_rng(zlib.crc32(repr(key).encode()))


def vnoise(rng, w, h, cw, ch):
    """Smooth value noise 0..1 of w x h from a coarse random grid of about cw x ch cells."""
    base = rng.random((max(1, ch) + 2, max(1, cw) + 2)).astype(np.float32)
    return np.asarray(Image.fromarray(base).resize((max(1, w), max(1, h)), Image.BILINEAR), dtype=np.float32)


def band(lum, edges, pal):
    idx = np.digitize(lum, edges)
    return np.array(pal, dtype=np.float32)[idx]


def put(img, x, y, rgb, alpha=None):
    """Writes an (h, w, 3) float colour and optional (h, w) alpha 0..255 into a PIL RGBA image."""
    h, w = rgb.shape[:2]
    a = np.full((h, w), 255, np.float32) if alpha is None else alpha
    arr = np.dstack([np.clip(rgb, 0, 255), a]).astype(np.uint8)
    img.paste(Image.fromarray(arr, "RGBA"), (x, y))


def ink(rgb, color, k=0.55):
    """The dark ink line: the one pixel border of a face is pulled toward the colour."""
    h, w = rgb.shape[:2]
    if min(w, h) < 3:
        return
    m = np.zeros((h, w), bool)
    m[0, :] = m[-1, :] = m[:, 0] = m[:, -1] = True
    rgb[m] = rgb[m] * (1 - k) + np.array(color, np.float32) * k


# ================================================================================================ the material painters (return rgb, alpha, glow rgb, glow alpha)
def p_fur(w, h, face, rng, pal, glow_amt=0.55):
    n = vnoise(rng, w, h, w // 2 + 1, h // 5 + 1) * 0.6 + vnoise(rng, w, h, w // 4 + 1, h // 2 + 1) * 0.4
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None] * np.ones((1, w), np.float32)
    lum = (n * 0.8 + (1 - yy) * 0.22 + 0.1) * FACE_LIGHT[face]
    rgb = band(lum, [0.36, 0.6, 0.82], pal["fur"])
    xx = np.arange(w, dtype=np.float32)[None, :] * np.ones((h, 1), np.float32)
    ph = float(rng.random()) * 6.28
    st = np.sin((xx + n * 3.2) * 1.25 + ph + yy * 1.4)
    sm = (st > 0.62) & (yy > 0.1) & (n > 0.28)
    rgb[sm] = np.array(pal["stripe"], np.float32) * FACE_LIGHT[face]
    ink(rgb, pal["dark"][0])
    ga = np.zeros((h, w), np.float32)
    ga[sm] = 255 * glow_amt
    gr = np.zeros((h, w, 3), np.float32)
    gr[sm] = (255, 96, 24)
    return rgb, None, gr, ga


def p_belly(w, h, face, rng, pal):
    n = vnoise(rng, w, h, w // 2 + 1, h // 4 + 1)
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None] * np.ones((1, w), np.float32)
    lum = (n * 0.7 + (1 - yy) * 0.2 + 0.18) * FACE_LIGHT[face]
    rgb = band(lum, [0.34, 0.58, 0.8], pal["belly"])
    ink(rgb, pal["fur"][0], 0.45)
    return rgb, None, None, None


def p_dark(w, h, face, rng, pal):
    n = vnoise(rng, w, h, w // 2 + 1, h // 2 + 1)
    rgb = band(n * FACE_LIGHT[face], [0.4, 0.7], pal["dark"])
    return rgb, None, None, None


def p_inner(w, h, face, rng, pal):
    n = vnoise(rng, w, h, w // 2 + 1, h // 2 + 1)
    rgb = band(n, [0.45], [(150, 40, 30), (214, 90, 52)])
    gl = np.zeros((h, w, 3), np.float32)
    gl[:] = (255, 80, 30)
    return rgb, None, gl, np.full((h, w), 70, np.float32)


def p_claw(w, h, face, rng, pal):
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None] * np.ones((1, w), np.float32)
    lum = (0.9 - yy * 0.55 + vnoise(rng, w, h, 2, 2) * 0.12) * FACE_LIGHT[face]
    rgb = band(lum, [0.38, 0.66], CLAW)
    ink(rgb, (60, 40, 26), 0.4)
    gl = np.zeros((h, w, 3), np.float32)
    gl[:] = (255, 170, 70)
    ga = np.clip((yy - 0.45) * 2.0, 0, 1) * 150
    return rgb, None, gl, ga


def p_horn(w, h, face, rng, pal):
    hp = pal["horn"]
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None] * np.ones((1, w), np.float32)
    n = vnoise(rng, w, h, w // 2 + 1, h // 2 + 1)
    lum = (0.85 - yy * 0.3 + n * 0.25) * FACE_LIGHT[face]
    rgb = band(lum, [0.4, 0.62, 0.84], [hp[0], hp[1], hp[2], FLAME_CORE])
    ink(rgb, (30, 18, 14), 0.5)
    gl = np.zeros((h, w, 3), np.float32)
    gl[:] = (255, 150, 50)
    return rgb, None, gl, np.clip((yy - 0.6) * 2.2, 0, 1) * 170


def p_tooth(w, h, face, rng, pal):
    lum = (0.8 + vnoise(rng, w, h, 2, 2) * 0.2) * FACE_LIGHT[face]
    rgb = band(lum, [0.55, 0.8], [(190, 170, 130), (240, 226, 190), (255, 248, 226)])
    return rgb, None, None, None


def p_eye(w, h, face, rng, pal):
    rgb = np.zeros((h, w, 3), np.float32)
    rgb[:] = (255, 244, 170)
    if w >= 3:
        rgb[:, w // 2] = (90, 30, 10)
    gl = np.zeros((h, w, 3), np.float32)
    gl[:] = (255, 230, 120)
    return rgb, None, gl, np.full((h, w), 255, np.float32)


def p_plate(w, h, face, rng, pal):
    pp = pal["plate"]
    n = vnoise(rng, w, h, w // 3 + 1, h // 3 + 1)
    yy = np.linspace(0, 1, h, dtype=np.float32)[:, None] * np.ones((1, w), np.float32)
    lum = (n * 0.7 + (1 - yy) * 0.25 + 0.1) * FACE_LIGHT[face]
    rgb = band(lum, [0.36, 0.58, 0.8], pp)
    ink(rgb, (24, 14, 12), 0.6)
    c = np.abs(vnoise(rng, w, h, w // 2 + 2, h // 2 + 2) - 0.5) < 0.035      # glowing cracks: thin veins where a second noise is near its middle
    gl = np.zeros((h, w, 3), np.float32)
    gl[c] = (255, 140, 40)
    ga = np.zeros((h, w), np.float32)
    ga[c] = 230
    rgb[c] = (255, 150, 50)
    return rgb, None, gl, ga


def p_flame(w, h, face, rng, pal, hot=1.0, curl=0.0):
    """A ragged flame tongue, tip up (row 0), widest at the base; alpha is the silhouette; everything of it glows."""
    yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
    t = yy / max(1, h - 1)
    ph = float(rng.random()) * 6.28
    cx = (w - 1) / 2 + curl * np.sin(t * 3.1 + ph) * w * 0.2 * (1 - t * 0.4)
    prof = 0.10 + 0.9 * np.sin(np.clip(t, 0, 1) * math.pi * 0.5) ** 0.85
    wob = 0.82 + 0.36 * vnoise(rng, w, h, 2, max(2, h // 5))
    half = np.maximum(0.45, (w / 2.0) * prof * wob)
    d = np.abs(xx - cx) / half
    inside = d <= 1.0
    k = d + t * 0.14 - (hot - 1.0) * 0.1
    rgb = np.zeros((h, w, 3), np.float32)
    rgb[:] = FLAME_EDGE
    rgb[k < 0.78] = FLAME_BODY
    rgb[k < 0.5] = FLAME_CORE
    rgb[k < 0.22] = FLAME_HOT
    rim = inside & (d > 0.86)
    rgb[rim] = FLAME_LINE
    a = np.where(inside, 255, 0).astype(np.float32)
    ga = np.where(inside & ~rim, 235, 0).astype(np.float32)
    return rgb, a, rgb.copy(), ga


PAINTERS = {"fur": p_fur, "belly": p_belly, "dark": p_dark, "inner": p_inner, "claw": p_claw, "horn": p_horn, "tooth": p_tooth,
            "eye": p_eye, "plate": p_plate}


# ================================================================================================ the model builder (box uv packing + painting)
class B:
    """A GeoLite model under construction: boxes and flame cards get texture rectangles when finish() packs and paints them."""

    def __init__(self, name, pal="lion", tw=256, ghost=False):
        self.name, self.palname, self.pal, self.tw, self.ghost = name, pal, PAL[pal], tw, ghost
        self.m = Model("beast", name, tw, tw)
        self.items = []
        self.seq = 0

    def bone(self, name, parent=None, pivot=(0, 0, 0)):
        self.m.bone(name, parent=parent, pivot=pivot)

    def box(self, bone, o, s, mat="fur", side=1, infl=0.0, rot=None, piv=None):
        assert all(float(v).is_integer() for v in s), (self.name, bone, s)
        ox = o[0] if side > 0 else -(o[0] + s[0])
        r = None if rot is None else (rot[0], rot[1] * side, rot[2] * side)
        p = None if piv is None else (piv[0] * side, piv[1], piv[2])
        c = self.m.cube(bone, (ox, o[1], o[2]), tuple(int(v) for v in s), uv=(0, 0), inflate=infl, rotation=r, pivot=p)
        self.items.append(dict(c=c, kind="box", mat=mat, s=tuple(int(v) for v in s), id=self.seq))
        self.seq += 1
        return c

    def card(self, bone, o, w, h, plane="z", side=1, rot=None, piv=None, hot=1.0, curl=0.0, dens=2.6):
        """A flat flame tongue standing on its base: plane z faces front/back (w across x), plane x faces the sides (w along z)."""
        s = (w, h, 0) if plane == "z" else (0, h, w)
        ox = o[0] if side > 0 else -(o[0] + s[0])
        r = None if rot is None else (rot[0], rot[1] * side, rot[2] * side)
        p = None if piv is None else (piv[0] * side, piv[1], piv[2])
        c = self.m.cube(bone, (ox, o[1], o[2]), s, uv=(0, 0), rotation=r, pivot=p)
        cw, ch = max(8, int(round(w * dens))), max(10, int(round(h * dens)))
        self.items.append(dict(c=c, kind="card", plane=plane, cw=cw, ch=ch, hot=hot, curl=curl, id=self.seq))
        self.seq += 1
        return c

    # -------------------------------------------------------------------------- packing and painting
    def finish(self):
        for it in self.items:
            if it["kind"] == "box":
                sx, sy, sz = it["s"]
                it["rw"], it["rh"] = 2 * (sx + sz), sy + sz
            else:
                it["rw"], it["rh"] = it["cw"], it["ch"]
        order = sorted(self.items, key=lambda i: (-i["rh"], -i["rw"]))
        x = y = shelf = 0
        for it in order:
            if x + it["rw"] > self.tw:
                x, y, shelf = 0, y + shelf, 0
            it["x"], it["y"] = x, y
            x += it["rw"]
            shelf = max(shelf, it["rh"])
        need = y + shelf
        th = 64
        while th < need:
            th *= 2
        self.th = th
        self.m.tex_h = th
        img, glow = Image.new("RGBA", (self.tw, th), (0, 0, 0, 0)), Image.new("RGBA", (self.tw, th), (0, 0, 0, 0))
        for it in self.items:
            rng = rng_for(self.name, it["id"])
            if it["kind"] == "box":
                self._paint_box(img, glow, it, rng)
            else:
                self._paint_card(img, glow, it, rng)
        return img, glow

    def _paint_box(self, img, glow, it, rng):
        sx, sy, sz = it["s"]
        u, v = it["x"], it["y"]
        it["c"]["uv"] = [u, v]
        faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy), "north": (u + sz, v + sz, sx, sy),
                 "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
        for face, (fx, fy, fw, fh) in faces.items():
            if fw <= 0 or fh <= 0:
                continue
            rgb, a, gr, ga = self._material(it["mat"], fw, fh, face, rng)
            if self.ghost:
                a = np.full((fh, fw), 175, np.float32) if a is None else a * 0.7
            put(img, fx, fy, rgb, a)
            if gr is not None:
                put(glow, fx, fy, gr, ga)

    def _paint_card(self, img, glow, it, rng):
        cw, ch = it["cw"], it["ch"]
        rgb, a, gr, ga = p_flame(cw, ch, "north", rng, self.pal, it["hot"], it["curl"])
        if self.ghost:
            a = a * 0.8
        put(img, it["x"], it["y"], rgb, a)
        put(glow, it["x"], it["y"], gr, ga)
        faces = ("north", "south") if it["plane"] == "z" else ("east", "west")
        it["c"]["uv"] = {f: {"uv": [it["x"], it["y"]], "uv_size": [cw, ch]} for f in faces}

    def _material(self, mat, w, h, face, rng):
        if mat in PAINTERS:
            return PAINTERS[mat](w, h, face, rng, self.pal)
        if mat == "plain":                                    # a flat ember colour (ghost cores, small details)
            rgb = np.zeros((h, w, 3), np.float32)
            rgb[:] = FLAME_CORE
            return rgb, None, rgb.copy(), np.full((h, w), 200, np.float32)
        raise ValueError(mat)

    def save(self, anim):
        img, glow = self.finish()
        assert not self.check_uv(), self.check_uv()
        self.m.save(anim)
        self.m.save_texture(img, glow)
        if SHEETS:
            sheet_dir = os.environ.get("BEAST_SHEETS") or os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "build", "beast_sheets")
            os.makedirs(sheet_dir, exist_ok=True)
            self._sheet(img, glow, os.path.join(sheet_dir, self.m.id + "_sheet.png"))
        print("wrote %-18s %3d cubes  %dx%d texture" % (self.m.id, self.m.cube_count(), self.tw, self.th))
        return self.m.cube_count()

    def _sheet(self, img, glow, path):
        """The unwrapped faces drawn over the texture (uv check): coloured outlines for every box face rectangle and card rectangle."""
        from PIL import ImageDraw
        s = 4
        base = Image.new("RGBA", (self.tw, self.th), (40, 40, 52, 255))
        base.alpha_composite(img)
        base = base.resize((self.tw * s, self.th * s), Image.NEAREST)
        d = ImageDraw.Draw(base)
        for it in self.items:
            x, y = it["x"], it["y"]
            if it["kind"] == "box":
                sx, sy, sz = it["s"]
                for (fx, fy, fw, fh), col in (((x + sz, y, sx, sz), (255, 255, 0)), ((x + sz + sx, y, sx, sz), (255, 128, 0)), ((x, y + sz, sz, sy), (0, 255, 255)),
                                              ((x + sz, y + sz, sx, sy), (0, 255, 0)), ((x + sz + sx, y + sz, sz, sy), (255, 0, 255)), ((x + 2 * sz + sx, y + sz, sx, sy), (255, 0, 0))):
                    if fw > 0 and fh > 0:
                        d.rectangle([fx * s, fy * s, (fx + fw) * s - 1, (fy + fh) * s - 1], outline=col + (255,))
            else:
                d.rectangle([x * s, y * s, (x + it["cw"]) * s - 1, (y + it["ch"]) * s - 1], outline=(255, 255, 255, 255))
        base.save(path)
        gb = Image.new("RGBA", glow.size, (0, 0, 0, 255))
        gb.alpha_composite(glow)
        gb.resize((self.tw * 2, self.th * 2), Image.NEAREST).save(os.path.join(os.path.dirname(path), self.m.id + "_glowsheet.png"))

    def check_uv(self):
        return [(it["id"], it["kind"]) for it in self.items if it["x"] + it["rw"] > self.tw or it["y"] + it["rh"] > self.th]


# ================================================================================================ small keyframe helpers
def wave(a, clip, bone, kind, length, amp, phase=0.0, steps=8, axis=0, cycles=1, offset=(0, 0, 0)):
    """Samples offset + amp * sin(2 pi (cycles t / length) + phase) on one axis of a bone track so the loop closes."""
    frames = {}
    for i in range(steps + 1):
        v = list(offset)
        v[axis] = offset[axis] + amp * math.sin(2 * math.pi * cycles * (i % steps) / steps + phase)
        frames[length * i / steps] = tuple(v)
    getattr(a, kind)(clip, bone, frames)


def wave3(a, clip, bone, kind, length, amps, phases=(0, 0, 0), steps=8, cycles=1):
    frames = {}
    for i in range(steps + 1):
        frames[length * i / steps] = tuple(amps[k] * math.sin(2 * math.pi * cycles * (i % steps) / steps + phases[k]) for k in range(3))
    getattr(a, kind)(clip, bone, frames)


MODELS = {}


def model(fn):
    MODELS[fn.__name__.replace("build_", "")] = fn
    return fn


# ================================================================================================ shared quadruped parts
def foreleg(b, s, S):
    """One front leg (upper arm, forearm, paw with toes and claws); S: x, z, top, elbow, ptop, w, t, lw, lt, pw, pl."""
    n = "l" if s > 0 else "r"
    u, l, p = "f%s_u" % n, "f%s_l" % n, "f%s_p" % n
    x, z = S["x"], S["z"]
    b.bone(u, "body", (s * x, S["top"], z))
    b.bone(l, u, (s * x, S["elbow"], z))
    b.bone(p, l, (s * x, S["ptop"], z))
    b.box(u, (x - S["w"] / 2, S["elbow"], z - S["t"] / 2), (S["w"], S["top"] - S["elbow"], S["t"]), "fur", s, S.get("infl", 0.4))
    b.box(u, (x - S["w"] / 2 - 1, S["elbow"] + 3, z - 1), (1, 4, 3), "fur", s, 0.1, rot=(0, 0, -8), piv=(x - 1, S["elbow"] + 3, z))     # muscle edge
    b.card(u, (x + S["w"] / 2, S["elbow"] - 1, z - 2), 4, 5, "x", s, rot=(-35, 0, 0), piv=(x + S["w"] / 2, S["elbow"] + 1, z), hot=0.9)  # elbow flame tuft
    b.box(l, (x - S["lw"] / 2, S["ptop"], z - S["lt"] / 2), (S["lw"], S["elbow"] - S["ptop"], S["lt"]), "fur", s, S.get("linfl", 0.2))
    b.box(l, (x - S["lw"] / 2 - 1, S["ptop"] + 1, z - 1), (S["lw"] + 2, 2, 2), "belly", s, 0.0)                                         # wrist fur
    pl, pw = S["pl"], S["pw"]
    b.box(p, (x - pw / 2, 0, z - pl + 2), (pw, S["ptop"], pl), "fur", s, 0.1)
    b.box(p, (x - pw / 2 + 0.2, 0, z - pl + 2), (pw - 1, 1, pl), "dark", s, 0.15)
    nc = S.get("claws", 3)
    cw = (pw - 0.2) / nc
    for k in range(nc):
        b.box(p, (x - pw / 2 + 0.1 + k * cw, 0, z - pl - 0), (1, 1, 2), "claw", s, 0.0, rot=(-15, 0, 0), piv=(x, 1, z - pl + 2))


def hindleg(b, s, S):
    """One hind leg: a big thigh, a shin bent back, a hock and a paw (S: x, z, top, hock, ptop, w, t, ...)."""
    n = "l" if s > 0 else "r"
    u, l, p = "h%s_u" % n, "h%s_l" % n, "h%s_p" % n
    x, z, top = S["x"], S["z"], S["top"]
    b.bone(u, "body", (s * x, top, z))
    b.bone(l, u, (s * x, S["hock"], z + S.get("hz", 2)))
    b.bone(p, l, (s * x, S["ptop"], z + S.get("hz", 2) - 1))
    b.box(u, (x - S["w"] / 2, S["hock"] - 1, z - S["t"] / 2), (S["w"], top - S["hock"] + 3, S["t"]), "fur", s, S.get("infl", 0.6), rot=(S.get("tilt", -18), 0, 0), piv=(x, top, z))
    b.card(u, (x + S["w"] / 2 + 0.3, top - 5, z - 3), 6, 6, "x", s, rot=(-20, 0, 0), piv=(x + S["w"] / 2, top - 2, z), hot=0.9)           # haunch flame
    hz = z + S.get("hz", 2)
    b.box(l, (x - S["lw"] / 2, S["ptop"], hz - S["lt"] / 2), (S["lw"], S["hock"] - S["ptop"] + 1, S["lt"]), "fur", s, 0.2, rot=(S.get("shin", 28), 0, 0), piv=(x, S["hock"], hz))
    pl, pw = S["pl"], S["pw"]
    pz = hz - 1
    b.box(p, (x - pw / 2, 0, pz - pl + 3), (pw, S["ptop"], pl), "fur", s, 0.1)
    b.box(p, (x - pw / 2 + 0.2, 0, pz - pl + 3), (pw - 1, 1, pl), "dark", s, 0.15)
    nc = S.get("claws", 3)
    cw = (pw - 0.2) / nc
    for k in range(nc):
        b.box(p, (x - pw / 2 + 0.1 + k * cw, 0, pz - pl + 1), (1, 1, 2), "claw", s, 0.0, rot=(-15, 0, 0), piv=(x, 1, pz - pl + 3))


def mane_ring(b, bone, cz, cy, n, lo, hi, rmin, hmin, hmax, wmin, wmax, tilt, seed, curl=0.5):
    """A fan of flame cards round a centre (cy, cz) in the xy plane, from angle lo to hi (degrees from straight up), tilted back."""
    rng = rng_for("mane", seed)
    for k in range(n):
        ang = lo + (hi - lo) * k / max(1, n - 1)
        h = hmin + (hmax - hmin) * float(rng.random())
        w = int(wmin + (wmax - wmin) * float(rng.random()))
        z = cz + (k % 3) * 0.3
        b.card(bone, (-w / 2, cy + rmin, z), w, int(h), "z", 1, rot=(tilt + 6 * float(rng.random()), 0, ang), piv=(0, cy, z), hot=0.9 + 0.2 * float(rng.random()), curl=curl * (1 if k % 2 else -1))


def tail_chain(b, root, z0, y, seg, wid, plume, mat="fur"):
    """A tail: seg segments of the given length, then a flame plume of cards at the tip."""
    parent = root
    z = z0
    for i, ln in enumerate(seg):
        name = "tail_%d" % (i + 1)
        b.bone(name, parent, (0, y, z))
        t = wid[i]
        b.box(name, (-t / 2, y - t / 2, z), (t, t, ln), mat, 1, 0.4 - 0.1 * i)
        parent, z = name, z + ln
    b.bone("tail_f", parent, (0, y, z))
    for k, (rx, ry, rz, w, h, pl) in enumerate(plume):
        b.card("tail_f", (-w / 2, y, z - 1 + 0.2 * k) if pl == "z" else (0.2 * k, y, z - 1), w, h, pl, 1, rot=(rx, ry, rz), piv=(0, y, z - 1 + 0.2 * k), hot=1.0 + 0.1 * (k % 2), curl=0.6 * (-1) ** k)


# ================================================================================================ the three summoned beasts
def quad_beast(name, variant):
    b = B(name, variant, 256)
    P = b.pal
    b.bone("root", None, (0, 0, 0))
    b.bone("body", "root", (0, 13, 0))
    b.bone("neck", "body", (0, 17, -10))
    b.bone("head", "neck", (0, 18, -14))
    b.bone("jaw", "head", (0, 16, -19))
    b.bone("ear_l", "head", (3, 22, -16))
    b.bone("ear_r", "head", (-3, 22, -16))
    b.bone("mane_f", "neck", (0, 18, -12))
    b.bone("mane_b", "body", (0, 18, -8))
    if variant == "lion":
        torso_lion(b)
        head_lion(b)
        tail_chain(b, "body", 9, 13, [5, 5, 4], [3, 2, 2], [(-70, 0, 0, 5, 9, "z"), (-70, 0, 0, 5, 9, "x"), (-55, 0, 30, 4, 8, "z"), (-55, 0, -30, 4, 8, "z"), (-80, 0, 0, 3, 7, "x")])
        F = dict(x=4, z=-7, top=17, elbow=9, ptop=3, w=4, t=4, lw=3, lt=3, pw=4, pl=5)
        H = dict(x=4, z=8, top=14, hock=6, ptop=3, w=4, t=6, lw=3, lt=3, pw=4, pl=6)
    elif variant == "bear":
        torso_bear(b)
        head_bear(b)
        tail_chain(b, "body", 10, 12, [3, 3, 2], [3, 3, 2], [(-70, 0, 0, 4, 6, "z"), (-70, 0, 0, 4, 6, "x")])
        F = dict(x=5, z=-7, top=17, elbow=8, ptop=3, w=6, t=6, lw=5, lt=5, pw=6, pl=6, infl=0.6, linfl=0.4)
        H = dict(x=5, z=8, top=14, hock=6, ptop=3, w=6, t=7, lw=5, lt=5, pw=6, pl=6, tilt=-12, shin=18, infl=0.7)
    else:
        torso_rhino(b)
        head_rhino(b)
        tail_chain(b, "body", 11, 13, [4, 4, 3], [2, 2, 1], [(-70, 0, 0, 4, 7, "z"), (-70, 0, 0, 4, 7, "x")])
        F = dict(x=5, z=-7, top=17, elbow=8, ptop=3, w=6, t=6, lw=5, lt=5, pw=6, pl=6, infl=0.6, linfl=0.4, claws=3)
        H = dict(x=5, z=8, top=15, hock=7, ptop=3, w=6, t=7, lw=5, lt=5, pw=6, pl=6, tilt=-8, shin=12, infl=0.7, hz=1)
    for s in (1, -1):
        foreleg(b, s, F)
        hindleg(b, s, H)
    return b


def torso_lion(b):
    b.box("body", (-5, 9, -10), (10, 10, 9), "fur", 1, 0.4)
    b.box("body", (-4, 9, -2), (8, 9, 6), "fur", 1, 0.3)
    b.box("body", (-4, 8, 3), (8, 9, 7), "fur", 1, 0.5)
    b.box("body", (-3, 8, -9), (6, 2, 15), "belly", 1, 0.2)
    b.box("body", (-3, 9, -12), (6, 7, 2), "belly", 1, 0.2)
    b.box("body", (-3, 19, -9), (6, 2, 8), "fur", 1, 0.3)
    for k, z in enumerate((-8, -4, 0, 4, 8)):
        b.card("body", (0, 18 - (k > 3), z), 5, 6 - (k % 2), "x", 1, rot=(-25 - 6 * k, 0, 0), piv=(0, 18, z), hot=1.0 + 0.1 * (k % 2), curl=0.4)
    b.box("body", (-4, 10, 5), (1, 2, 4), "dark", 1, 0.1)
    mane_ring(b, "mane_f", -12, 18, 13, -165, 165, 4, 8, 13, 5, 7, 14, "lion", 0.5)
    mane_ring(b, "mane_f", -10, 18, 9, -140, 140, 3, 6, 9, 5, 6, 26, "lion2", 0.4)
    for k in range(6):
        x = 3.5 + 1.2 * (k % 2)
        for sd in (1, -1):
            b.card("mane_b", (x, 15 + (k % 3), -13 + 2 * k), 6, 8 - (k % 3), "x", sd, rot=(-38 - 8 * (k % 3), 0, 0), piv=(x, 17, -12 + 2 * k), hot=0.9, curl=0.5)
    b.card("mane_b", (0, 19, -13), 8, 9, "x", 1, rot=(-40, 0, 0), piv=(0, 19, -12), hot=1.1, curl=0.5)
    b.box("neck", (-3, 15, -14), (6, 6, 6), "fur", 1, 0.3)


def head_lion(b):
    b.box("head", (-4, 15, -19), (8, 7, 6), "fur", 1, 0.3)
    b.box("head", (-2, 15, -23), (4, 3, 4), "fur", 1, 0.2)
    b.box("head", (-1, 17, -24), (2, 1, 1), "dark", 1, 0.2)
    b.box("head", (-2, 14, -23), (4, 1, 3), "belly", 1, 0.0)
    b.box("head", (-1, 18, -23), (2, 2, 2), "fur", 1, 0.0)
    for s in (1, -1):
        b.box("head", (1.5, 18, -20), (2, 1, 1), "eye", s, 0.0, rot=(0, 0, -16), piv=(2.5, 18.5, -20))
        b.box("head", (1, 20, -20), (3, 1, 1), "dark", s, 0.1, rot=(0, 0, 14), piv=(2.5, 20.5, -20))
        b.box("head", (4, 15, -18), (2, 4, 4), "fur", s, 0.2)
        b.box("head", (1, 13, -22), (1, 2, 1), "tooth", s, 0.0)
        b.card("head", (4.4, 14, -17), 5, 4, "x", s, rot=(-95, 0, 0), piv=(4.4, 16, -16), hot=0.9, curl=0.6)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (2, 21, -17), (3, 3, 1), "fur", s, 0.2)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (2.5, 21.5, -18), (2, 2, 1), "inner", s, 0.0)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (3, 24, -17), (1, 1, 1), "fur", s, 0.1)
    b.box("jaw", (-2, 13, -23), (4, 2, 4), "fur", 1, 0.1)
    b.box("jaw", (-1, 14, -22), (2, 1, 2), "inner", 1, 0.0)
    for s in (1, -1):
        b.box("jaw", (1, 14, -23), (1, 1, 1), "tooth", s, 0.0)
    b.card("jaw", (-1.5, 11, -22), 3, 4, "z", 1, rot=(-10, 0, 0), piv=(0, 13, -22), hot=0.9)                      # chin flame beard


def torso_bear(b):
    b.box("body", (-6, 8, -11), (12, 11, 10), "fur", 1, 0.5)
    b.box("body", (-5, 8, -2), (10, 10, 6), "fur", 1, 0.4)
    b.box("body", (-5, 7, 3), (10, 10, 8), "fur", 1, 0.6)
    b.box("body", (-4, 7, -9), (8, 2, 16), "belly", 1, 0.2)
    b.box("body", (-4, 9, -13), (8, 7, 2), "belly", 1, 0.2)
    b.box("body", (-4, 19, -9), (8, 3, 8), "fur", 1, 0.5)
    for k, z in enumerate((-9, -6, -3, 0, 3, 6, 9)):
        b.card("body", (0, 19 - (k > 4), z), 6, 7 - (k % 3), "x", 1, rot=(-18 - 5 * k, 0, 0), piv=(0, 19, z), hot=0.9 + 0.1 * (k % 2), curl=0.5)
        for sd in (1, -1):
            b.card("body", (3.5, 16 - (k % 2), z - 1), 5, 6, "x", sd, rot=(-35, 0, 8), piv=(4, 17, z), hot=0.9, curl=0.4)
    mane_ring(b, "mane_f", -11, 19, 9, -150, 150, 4, 5, 8, 5, 6, 12, "bear", 0.5)
    for k in range(4):
        for sd in (1, -1):
            b.card("mane_b", (5 + (k % 2), 14, -12 + 3 * k), 6, 8, "x", sd, rot=(-40, 0, 0), piv=(5, 17, -11 + 3 * k), hot=0.9, curl=0.5)
    b.box("neck", (-4, 15, -15), (8, 7, 7), "fur", 1, 0.4)


def head_bear(b):
    b.box("head", (-4, 14, -20), (8, 8, 7), "fur", 1, 0.4)
    b.box("head", (-2, 14, -24), (4, 3, 4), "belly", 1, 0.3)
    b.box("head", (-1, 16, -25), (2, 2, 1), "dark", 1, 0.2)
    for s in (1, -1):
        b.box("head", (1.5, 18, -21), (2, 1, 1), "eye", s, 0.0, rot=(0, 0, -10), piv=(2.5, 18.5, -21))
        b.box("head", (1, 20, -21), (3, 1, 1), "dark", s, 0.1, rot=(0, 0, 18), piv=(2.5, 20.5, -21))
        b.box("head", (4, 14, -19), (2, 5, 5), "fur", s, 0.3)
        b.box("head", (1, 12, -23), (1, 2, 1), "tooth", s, 0.0)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (2, 21, -19), (3, 3, 2), "fur", s, 0.4)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (2.5, 21.5, -20), (2, 2, 1), "inner", s, 0.0)
        b.card("head", (4.6, 14, -18), 5, 5, "x", s, rot=(-95, 0, 0), piv=(4.6, 16, -17), hot=0.9, curl=0.6)
    b.box("jaw", (-2, 12, -24), (4, 2, 5), "fur", 1, 0.2)
    b.box("jaw", (-1, 13, -23), (2, 1, 3), "inner", 1, 0.0)
    b.box("head", (-3, 22, -19), (6, 1, 5), "fur", 1, 0.2)


def torso_rhino(b):
    b.box("body", (-6, 8, -11), (12, 12, 10), "fur", 1, 0.5)
    b.box("body", (-5, 8, -2), (10, 11, 6), "fur", 1, 0.4)
    b.box("body", (-5, 7, 3), (10, 11, 8), "fur", 1, 0.6)
    b.box("body", (-4, 7, -9), (8, 2, 16), "belly", 1, 0.2)
    # armour plates along the back and shoulders, glowing at the seams
    for k, z in enumerate((-10, -6, -2, 2, 6, 10)):
        b.box("body", (-3, 20 + (0 if k in (1, 2, 3) else -1), z), (6, 2, 3), "plate", 1, 0.3, rot=(0, 0, 0))
    b.box("body", (-7, 12, -11), (2, 7, 9), "plate", 1, 0.3)
    b.box("body", (5, 12, -11), (2, 7, 9), "plate", 1, 0.3)
    b.box("body", (-7, 11, 3), (2, 7, 8), "plate", 1, 0.3)
    b.box("body", (5, 11, 3), (2, 7, 8), "plate", 1, 0.3)
    for k, z in enumerate((-9, -4, 1, 6)):
        b.card("body", (0, 21, z), 5, 5, "x", 1, rot=(-20, 0, 0), piv=(0, 21, z), hot=0.9, curl=0.4)
    mane_ring(b, "mane_f", -12, 19, 7, -120, 120, 4, 4, 7, 5, 6, 10, "rhino", 0.4)
    b.box("neck", (-4, 14, -15), (8, 8, 7), "fur", 1, 0.4)
    b.box("neck", (-5, 15, -14), (10, 6, 5), "plate", 1, 0.3)


def head_rhino(b):
    b.box("head", (-4, 14, -20), (8, 8, 7), "fur", 1, 0.3)
    b.box("head", (-3, 13, -25), (6, 5, 5), "fur", 1, 0.3)
    b.box("head", (-4, 18, -24), (8, 3, 5), "plate", 1, 0.2)
    b.box("head", (-2, 12, -25), (4, 1, 4), "belly", 1, 0.0)
    for s in (1, -1):
        b.box("head", (3, 16, -21), (2, 1, 1), "eye", s, 0.0, rot=(0, 0, -12), piv=(4, 16.5, -21))
        b.box("head", (3, 18, -20), (3, 1, 1), "dark", s, 0.1)
        b.box("head", (4, 13, -19), (2, 5, 5), "plate", s, 0.2)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (4, 20, -17), (3, 3, 1), "fur", s, 0.2)
        b.box("ear_%s" % ("l" if s > 0 else "r"), (4.5, 20.5, -18), (2, 2, 1), "inner", s, 0.0)
        b.box("head", (-4.4, 13, -24), (1, 2, 1), "tooth", s, 0.0)
    # the great horn and the small one, stacked tilted blocks that taper
    b.box("head", (-2, 18, -28), (4, 3, 4), "horn", 1, 0.2, rot=(-14, 0, 0), piv=(0, 18, -25))
    b.box("head", (-1, 21, -29), (2, 4, 3), "horn", 1, 0.2, rot=(-26, 0, 0), piv=(0, 20, -26))
    b.box("head", (-1, 24, -30), (1, 3, 2), "horn", 1, 0.1, rot=(-38, 0, 0), piv=(0, 23, -27))
    b.box("head", (-1, 20, -23), (2, 3, 2), "horn", 1, 0.2, rot=(-12, 0, 0), piv=(0, 20, -22))
    b.box("jaw", (-2, 12, -25), (4, 2, 5), "fur", 1, 0.1)


# ================================================================================================ the quadruped clips: idle, run, attack, appear, vanish
LEGS = ("fl", "fr", "hl", "hr")
SW = 1.0        # sign of a forward leg swing about x (checked against the preview)


def quad_anim(variant):
    a = Anim()
    heavy = {"lion": 1.0, "bear": 0.8, "rhino": 0.7}[variant]
    # ---------------------------------------------------------------- idle: breathing, a swaying tail, a drifting mane, a twitching ear
    L = 2.0
    a.clip("idle", L, True)
    wave(a, "idle", "body", "pos", L, 0.55, 0, axis=1, offset=(0, 0, 0))
    wave(a, "idle", "neck", "rot", L, 2.5, 0.6, axis=0)
    wave3(a, "idle", "head", "rot", L, (2.0, 4.0, 0), (1.0, 0, 0))
    wave(a, "idle", "jaw", "rot", L, 2.0, 0, axis=0, offset=(2, 0, 0))
    for i, (am, ph) in enumerate(((7, 0.0), (9, 0.7), (12, 1.4))):
        wave(a, "idle", "tail_%d" % (i + 1), "rot", L, am, ph, axis=1)
    wave3(a, "idle", "tail_f", "rot", L, (5, 14, 0), (0, 2.1, 0))
    wave3(a, "idle", "mane_f", "rot", L, (3.0, 0, 2.0), (0, 0, 1.0))
    wave3(a, "idle", "mane_b", "rot", L, (4.0, 0, 0), (1.0, 0, 0))
    a.rot("idle", "ear_l", {0.0: (0, 0, 0), 1.1: (0, 0, 0), 1.25: (0, 0, -14), 1.4: (0, 0, 0), 2.0: (0, 0, 0)})
    a.rot("idle", "ear_r", {0.0: (0, 0, 0), 0.5: (0, 0, 0), 0.62: (0, 0, 12), 0.75: (0, 0, 0), 2.0: (0, 0, 0)})
    for k, lg in enumerate(LEGS):
        wave(a, "idle", lg + "_u", "rot", L, 0.8, k, axis=0)
    # ---------------------------------------------------------------- run: a rotary gallop, the mane and tail streaming back
    T = 0.6
    a.clip("run", T, True)
    ph = {"fl": 0.0, "fr": 0.7, "hl": 3.6, "hr": 4.2}
    amp = 40 * (0.9 + 0.1 * heavy)
    for lg in LEGS:
        sgn = SW if lg[0] == "f" else -SW * 0.0 + SW
        wave(a, "run", lg + "_u", "rot", T, amp * sgn, ph[lg], steps=12, axis=0)
        frames = {}
        for i in range(13):
            ang = 2 * math.pi * (i % 12) / 12 + ph[lg]
            flex = 22 + 26 * math.sin(ang + (1.3 if lg[0] == "f" else 1.7))
            frames[T * i / 12] = (flex * (-SW if lg[0] == "f" else SW), 0, 0)
        a.rot("run", lg + "_l", frames)
        wave(a, "run", lg + "_p", "rot", T, 14, ph[lg] + 1.0, steps=12, axis=0)
    wave(a, "run", "body", "pos", T, 1.4, 0.4, steps=12, axis=1)
    wave3(a, "run", "body", "rot", T, (SW * 5.0, 0, 1.5), (0.2, 0, 0), steps=12)
    wave3(a, "run", "neck", "rot", T, (4, 0, 0), (2.0, 0, 0), steps=12)
    wave3(a, "run", "head", "rot", T, (3, 0, 0), (2.8, 0, 0), steps=12)
    wave(a, "run", "jaw", "rot", T, 5, 0, steps=12, offset=(7, 0, 0))
    for i, (am, p) in enumerate(((5, 0.0), (7, 0.6), (9, 1.2))):
        wave3(a, "run", "tail_%d" % (i + 1), "rot", T, (am * 0.5, am * 1.2, 0), (p, p, 0), steps=12)
    wave3(a, "run", "tail_f", "rot", T, (6, 12, 0), (1.5, 2.0, 0), steps=12)
    wave3(a, "run", "mane_f", "rot", T, (6.0, 0, 4.0), (0.3, 0, 1.0), steps=12)
    wave3(a, "run", "mane_b", "rot", T, (7.0, 0, 3.0), (0.9, 0, 1.0), steps=12)
    wave(a, "run", "ear_l", "rot", T, 8, 0.5, steps=12, axis=2)
    wave(a, "run", "ear_r", "rot", T, 8, 1.1, steps=12, axis=2)
    # ---------------------------------------------------------------- attack: a crouch, a pounce, a snapping bite, a landing (plays once)
    a.clip("attack", 0.9, False)
    a.rot("attack", "body", {0.0: (0, 0, 0), 0.2: (-SW * 12, 0, 0), 0.34: (SW * 16, 0, 0), 0.5: (SW * 8, 0, 0), 0.72: (-SW * 6, 0, 0), 0.9: (0, 0, 0)})
    a.pos("attack", "body", {0.0: (0, 0, 0), 0.2: (0, -3, 3), 0.34: (0, 5, -7), 0.5: (0, 3, -9), 0.72: (0, 0, -4), 0.9: (0, 0, 0)})
    a.rot("attack", "neck", {0.0: (0, 0, 0), 0.2: (SW * 8, 0, 0), 0.34: (-SW * 18, 0, 0), 0.5: (-SW * 26, 0, 0), 0.72: (0, 0, 0), 0.9: (0, 0, 0)})
    a.rot("attack", "head", {0.0: (0, 0, 0), 0.2: (SW * 6, 0, 0), 0.34: (-SW * 12, 0, 0), 0.5: (-SW * 22, 0, 0), 0.6: (-SW * 8, 0, 0), 0.9: (0, 0, 0)})
    a.rot("attack", "jaw", {0.0: (0, 0, 0), 0.2: (10, 0, 0), 0.34: (46, 0, 0), 0.46: (50, 0, 0), 0.54: (0, 0, 0), 0.72: (6, 0, 0), 0.9: (0, 0, 0)})
    a.rot("attack", "mane_f", {0.0: (0, 0, 0), 0.34: (-SW * 12, 0, 0), 0.5: (-SW * 18, 0, 0), 0.9: (0, 0, 0)})
    a.scale("attack", "mane_f", {0.0: (1, 1, 1), 0.34: (1.25, 1.25, 1.25), 0.5: (1.35, 1.35, 1.35), 0.9: (1, 1, 1)})
    a.rot("attack", "mane_b", {0.0: (0, 0, 0), 0.34: (-SW * 25, 0, 0), 0.9: (0, 0, 0)})
    a.rot("attack", "tail_1", {0.0: (0, 0, 0), 0.34: (-SW * 30, 0, 0), 0.9: (0, 0, 0)})
    a.rot("attack", "tail_f", {0.0: (0, 0, 0), 0.34: (0, 20, 0), 0.6: (0, -20, 0), 0.9: (0, 0, 0)})
    for lg in ("fl", "fr"):
        a.rot("attack", lg + "_u", {0.0: (0, 0, 0), 0.2: (-SW * 18, 0, 0), 0.34: (SW * 62, 0, 0), 0.5: (SW * 78, 0, 0), 0.72: (SW * 10, 0, 0), 0.9: (0, 0, 0)})
        a.rot("attack", lg + "_l", {0.0: (0, 0, 0), 0.2: (SW * 28, 0, 0), 0.34: (-SW * 10, 0, 0), 0.5: (-SW * 30, 0, 0), 0.9: (0, 0, 0)})
    for lg in ("hl", "hr"):
        a.rot("attack", lg + "_u", {0.0: (0, 0, 0), 0.2: (SW * 22, 0, 0), 0.34: (-SW * 40, 0, 0), 0.5: (-SW * 28, 0, 0), 0.72: (0, 0, 0), 0.9: (0, 0, 0)})
        a.rot("attack", lg + "_l", {0.0: (0, 0, 0), 0.2: (-SW * 18, 0, 0), 0.34: (SW * 24, 0, 0), 0.9: (0, 0, 0)})
    # ---------------------------------------------------------------- appear: it bursts out of the flames; vanish: it flares and folds back into them
    a.clip("appear", 0.5, False)
    a.scale("appear", "root", {0.0: (0.15, 0.15, 0.15), 0.18: (1.14, 1.14, 1.14), 0.32: (0.96, 0.96, 0.96), 0.5: (1, 1, 1)})
    a.scale("appear", "mane_f", {0.0: (2.2, 2.2, 2.2), 0.25: (1.6, 1.6, 1.6), 0.5: (1, 1, 1)})
    a.scale("appear", "tail_f", {0.0: (2.4, 2.4, 2.4), 0.3: (1.5, 1.5, 1.5), 0.5: (1, 1, 1)})
    a.rot("appear", "head", {0.0: (-SW * 25, 0, 0), 0.25: (SW * 6, 0, 0), 0.5: (0, 0, 0)})
    a.rot("appear", "jaw", {0.0: (40, 0, 0), 0.3: (30, 0, 0), 0.5: (0, 0, 0)})
    a.clip("vanish", 0.5, False)
    a.scale("vanish", "root", {0.0: (1, 1, 1), 0.12: (1.1, 1.1, 1.1), 0.5: (0.02, 0.02, 0.02)})
    a.scale("vanish", "mane_f", {0.0: (1, 1, 1), 0.3: (1.9, 1.9, 1.9), 0.5: (2.6, 2.6, 2.6)})
    a.scale("vanish", "tail_f", {0.0: (1, 1, 1), 0.3: (1.8, 1.8, 1.8), 0.5: (2.4, 2.4, 2.4)})
    a.rot("vanish", "head", {0.0: (0, 0, 0), 0.2: (-SW * 22, 0, 0), 0.5: (-SW * 30, 0, 0)})
    a.rot("vanish", "jaw", {0.0: (0, 0, 0), 0.2: (44, 0, 0), 0.5: (50, 0, 0)})
    return a


@model
def build_spirit_beast():
    b = quad_beast("spirit_beast", "lion")
    return b, quad_anim("lion")


@model
def build_spirit_bear():
    b = quad_beast("spirit_bear", "bear")
    return b, quad_anim("bear")


@model
def build_spirit_rhino():
    b = quad_beast("spirit_rhino", "rhino")
    return b, quad_anim("rhino")



# ================================================================================================ Beast Form: the pieces that grow on the player
@model
def build_form_head():
    b = B("form_head", "lion", 256)
    b.bone("head", None, (0, 24, 0))
    b.bone("ear_l", "head", (3, 32, 0))
    b.bone("ear_r", "head", (-3, 32, 0))
    b.bone("mane", "head", (0, 28, 4))
    for s in (1, -1):
        e = "ear_l" if s > 0 else "ear_r"
        b.box(e, (2, 32, -1), (3, 3, 2), "fur", s, 0.25)
        b.box(e, (2.5, 32, -1.9), (2, 2, 1), "inner", s, 0.0)
        b.box(e, (3, 35, -0.5), (1, 2, 1), "fur", s, 0.15)
        b.box("head", (1.5, 28, -4.6), (2, 1, 1), "eye", s, 0.0, rot=(0, 0, -14), piv=(2.5, 28.5, -4.5))
        b.box("head", (1, 30, -4.7), (3, 1, 1), "dark", s, 0.1, rot=(0, 0, 14), piv=(2.5, 30.5, -4.5))
        b.box("head", (4, 24, -3), (1, 5, 5), "fur", s, 0.3)
        b.box("head", (1, 23, -4.8), (1, 2, 1), "tooth", s, 0.0)
        b.card("head", (4.6, 24, -3), 5, 5, "x", s, rot=(-95, 0, 0), piv=(4.6, 26, -2), hot=0.9, curl=0.6)
    b.box("head", (-4, 31, -5), (8, 1, 1), "fur", 1, 0.25)
    b.box("head", (-1, 26, -5), (2, 1, 1), "dark", 1, 0.1)
    for k in range(3):
        b.card("head", (0, 32, -3 + 3 * k), 6, 6 + 2 * (k % 2), "x", 1, rot=(-28 - 10 * k, 0, 0), piv=(0, 32, -3 + 3 * k), hot=1.0, curl=0.5)
    mane_ring(b, "mane", 4.6, 28, 11, -160, 160, 5, 5, 9, 4, 6, 16, "formhead", 0.5)
    a = Anim()
    a.clip("idle", 2.0, True)
    a.rot("idle", "ear_l", {0.0: (0, 0, 0), 1.1: (0, 0, 0), 1.25: (0, 0, -12), 1.4: (0, 0, 0), 2.0: (0, 0, 0)})
    a.rot("idle", "ear_r", {0.0: (0, 0, 0), 0.5: (0, 0, 0), 0.62: (0, 0, 10), 0.75: (0, 0, 0), 2.0: (0, 0, 0)})
    wave3(a, "idle", "mane", "rot", 2.0, (3.0, 0, 2.5), (0, 0, 1.0))
    return b, a


@model
def build_form_body():
    b = B("form_body", "lion", 256)
    b.bone("body", None, (0, 24, 0))
    b.box("body", (-5, 22, -3), (10, 3, 6), "fur", 1, 0.2)
    b.box("body", (-3, 17, -3), (6, 5, 1), "belly", 1, 0.1)
    b.box("body", (-5, 11, -3), (10, 2, 6), "fur", 1, 0.2)
    for s in (1, -1):
        b.box("body", (4, 22, -3), (5, 3, 6), "fur", s, 0.25)
        b.card("body", (4.5, 25, 0), 6, 8, "z", s, rot=(0, 0, -25), piv=(5, 25, 0), hot=1.0, curl=0.5)
        b.card("body", (4.5, 25, -2), 5, 6, "z", s, rot=(-15, 0, -45), piv=(5, 25, -2), hot=0.9, curl=-0.5)
        b.card("body", (4.5, 11, -1), 5, 5, "x", s, rot=(-30, 0, 0), piv=(5, 13, 0), hot=0.9)
    for k in range(5):
        b.card("body", (0, 14 + 2 * k, 2), 6, 5 + (k % 2), "x", 1, rot=(-35 - 6 * k, 0, 0), piv=(0, 15 + 2 * k, 2), hot=1.0 + 0.1 * (k % 2), curl=0.4)
    for k in range(5):
        b.card("body", (-3, 24, 1.5 + 0.2 * k), 6, 7, "z", 1, rot=(-24, 0, -80 + 40 * k), piv=(0, 24, 1.5 + 0.2 * k), hot=0.9, curl=0.5)
    tail_chain(b, "body", 2, 13, [5, 5, 5], [3, 3, 2], [(-70, 0, 0, 6, 11, "z"), (-70, 0, 0, 6, 11, "x"), (-55, 0, 28, 5, 9, "z"), (-55, 0, -28, 5, 9, "z"), (-82, 0, 0, 4, 8, "x")])
    a = Anim()
    a.clip("idle", 2.0, True)
    for i, (am, ph) in enumerate(((7, 0.0), (10, 0.7), (13, 1.4))):
        wave(a, "idle", "tail_%d" % (i + 1), "rot", 2.0, am, ph, axis=1)
    wave(a, "idle", "tail_1", "pos", 2.0, 0.3, 0, axis=1)
    wave3(a, "idle", "tail_f", "rot", 2.0, (6, 16, 0), (0, 2.0, 0))
    return b, a


@model
def build_form_claw():
    b = B("form_claw", "lion", 128)
    b.bone("arm", None, (0, 0, 0))
    b.box("arm", (-3, -9, -3), (6, 4, 6), "fur", 1, 0.2)
    b.box("arm", (-2, -11, -2), (4, 2, 4), "fur", 1, 0.1)
    b.box("arm", (-2, -9, -3), (4, 1, 1), "belly", 1, 0.0)
    for k, x in enumerate((-2, -1, 0, 1)):
        b.box("arm", (x, -16, -3), (1, 5, 1), "claw", 1, 0.0, rot=(-18, 0, 0), piv=(x + 0.5, -11, -2.5))
    b.box("arm", (2, -14, -1), (1, 4, 1), "claw", 1, 0.0, rot=(0, 0, 22), piv=(2, -11, -0.5))
    b.card("arm", (-2.5, -8, 3), 5, 7, "z", 1, rot=(-12, 0, 0), piv=(0, -8, 3), hot=1.0, curl=0.5)
    b.card("arm", (3, -8, -2), 4, 6, "x", 1, rot=(0, 0, -10), piv=(3, -8, 0), hot=0.9, curl=0.5)
    b.card("arm", (-3, -8, -2), 4, 6, "x", 1, rot=(0, 0, 10), piv=(-3, -8, 0), hot=0.9, curl=-0.5)
    a = Anim()
    a.clip("idle", 1.0, True)
    a.scale("idle", "arm", {0.0: (1, 1, 1), 0.5: (1.02, 1.02, 1.02), 1.0: (1, 1, 1)})
    return b, a


@model
def build_form_ghost():
    b = B("form_ghost", "lion", 256, ghost=True)
    b.bone("ghost", None, (0, 42, 3))
    b.bone("head", "ghost", (0, 46, 0))
    b.bone("jaw", "head", (0, 44, -6))
    b.bone("mane", "head", (0, 47, 3))
    b.box("head", (-7, 42, -6), (14, 11, 10), "fur", 1, 0.4)
    b.box("head", (-4, 42, -12), (8, 6, 6), "fur", 1, 0.3)
    b.box("head", (-2, 46, -13), (4, 2, 1), "dark", 1, 0.3)
    b.box("head", (-3, 41, -11), (6, 1, 5), "belly", 1, 0.0)
    for s in (1, -1):
        b.box("head", (3, 48, -7), (3, 1, 1), "eye", s, 0.1, rot=(0, 0, -16), piv=(4.5, 48.5, -7))
        b.box("head", (2, 50, -7), (5, 1, 1), "dark", s, 0.2, rot=(0, 0, 16), piv=(4.5, 50.5, -7))
        b.box("head", (7, 42, -4), (2, 7, 7), "fur", s, 0.4)
        b.box("head", (2, 39, -11), (1, 3, 1), "tooth", s, 0.0)
        b.box("head", (5, 53, -3), (3, 4, 2), "fur", s, 0.3)
        b.box("head", (5.5, 53, -4), (2, 3, 1), "inner", s, 0.0)
        b.box("ghost", (9, 36, -2), (6, 5, 8), "fur", s, 0.5)
        b.card("ghost", (11, 40, -2), 8, 10, "z", s, rot=(0, 0, -30), piv=(12, 40, 0), hot=1.0, curl=0.5)
    b.box("jaw", (-3, 39, -12), (6, 3, 6), "fur", 1, 0.2)
    b.box("jaw", (-2, 41, -11), (4, 1, 4), "inner", 1, 0.0)
    b.box("ghost", (-10, 34, -3), (20, 5, 9), "fur", 1, 0.5)
    b.box("ghost", (-7, 30, -2), (14, 5, 7), "fur", 1, 0.4)
    mane_ring(b, "mane", 3, 47, 15, -168, 168, 9, 11, 18, 7, 10, 10, "ghost", 0.6)
    mane_ring(b, "mane", 4.5, 47, 11, -150, 150, 7, 8, 13, 6, 8, 22, "ghost2", 0.5)
    for k in range(7):
        b.card("ghost", (-14 + 4.4 * k, 20 - (k % 3) * 2, 0), 5, 14 - (k % 3) * 2, "z", 1, rot=(0, 0, 180), piv=(-11.5 + 4.4 * k, 33, 0), hot=0.9, curl=0.6)
    a = Anim()
    a.clip("idle", 3.0, True)
    wave(a, "idle", "ghost", "pos", 3.0, 1.6, 0, steps=12, axis=1)
    wave(a, "idle", "ghost", "rot", 3.0, 2.0, 0.5, steps=12, axis=2)
    wave(a, "idle", "head", "rot", 3.0, 3.0, 1.0, steps=12, axis=0)
    wave(a, "idle", "jaw", "rot", 3.0, 8.0, 0, steps=12, axis=0, offset=(10, 0, 0))
    wave3(a, "idle", "mane", "rot", 3.0, (3, 0, 3), (0, 0, 1.2), steps=12)
    return b, a


# ================================================================================================ the fur skin of Beast Form (64x64 player skin layout)
SKIN_BOXES = {                      # name: (u, v, sx, sy, sz, furry 0..1 how much fur), the base layer and the overlay layer of each part
    "head": (0, 0, 8, 8, 8, 0.45), "hat": (32, 0, 8, 8, 8, 0.0),
    "body": (16, 16, 8, 12, 4, 0.7), "jacket": (16, 32, 8, 12, 4, 0.7),
    "rarm": (40, 16, 4, 12, 4, 0.9), "rsleeve": (40, 32, 4, 12, 4, 0.9),
    "larm": (32, 48, 4, 12, 4, 0.9), "lsleeve": (48, 48, 4, 12, 4, 0.9),
    "rleg": (0, 16, 4, 12, 4, 0.85), "rpants": (0, 32, 4, 12, 4, 0.85),
    "lleg": (16, 48, 4, 12, 4, 0.85), "lpants": (0, 48, 4, 12, 4, 0.85),
}


def build_skin():
    """textures/aura/beast_skin.png: sparse cel-shaded fur tufts that grow from the shoulders, forearms and shins and over the flanks, the face
    left clear but for cheek ruffs and brow stripes; beast_skin_glow.png: the dark stripes glowing like embers (additive)."""
    pal = PAL["lion"]
    img, glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0)), Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, (u, v, sx, sy, sz, furry) in SKIN_BOXES.items():
        if furry <= 0:
            continue
        faces = {"top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz), "west": (u, v + sz, sz, sy), "north": (u + sz, v + sz, sx, sy),
                 "east": (u + sz + sx, v + sz, sz, sy), "south": (u + 2 * sz + sx, v + sz, sx, sy)}
        for face, (fx, fy, fw, fh) in faces.items():
            rng = rng_for("skin", name, face)
            rgb, _, gr, ga = p_fur(fw, fh, face, rng, pal, 0.8)
            yy = np.linspace(0, 1, fh, dtype=np.float32)[:, None] * np.ones((1, fw), np.float32)
            n = vnoise(rng, fw, fh, fw // 2 + 1, fh // 3 + 1)
            if name in ("head", "hat"):
                mask = (n > 0.62) & ((np.arange(fw)[None, :] < 2) | (np.arange(fw)[None, :] >= fw - 2) | (yy < 0.2))
                if face == "north":
                    mask &= (yy < 0.2) | (np.arange(fw)[None, :] < 1) | (np.arange(fw)[None, :] >= fw - 1)
            else:
                lower = 0.35 + 0.65 * yy if name[0] in "rl" and "arm" in name or "sleeve" in name or "leg" in name or "pants" in name else 0.45 + 0.3 * (1 - yy)
                mask = n * lower * 1.5 + 0.1 > (1.0 - furry * 0.52)
            mask = mask & (n > 0.2)
            a = np.where(mask, 255, 0).astype(np.float32)
            put(img, fx, fy, rgb, a)
            ga2 = np.where(mask, ga, 0).astype(np.float32)
            put(glow, fx, fy, gr, ga2)
    out = os.path.join(ROOT, "textures", "aura")
    os.makedirs(out, exist_ok=True)
    img.save(os.path.join(out, "beast_skin.png"))
    glow.save(os.path.join(out, "beast_skin_glow.png"))
    print("wrote aura/beast_skin 64x64 (+ glow)")


def main():
    names = [a for a in sys.argv[1:] if not a.startswith("--")] or list(MODELS) + ["skin"]
    total = 0
    for n in names:
        if n == "skin":
            build_skin()
            continue
        b, anim = MODELS[n]()
        total += b.save(anim)
    print("cubes total", total)


if __name__ == "__main__":
    main()

"""Key Magic models: the Gate of Keys and the Great Key (GeoLite, see tools/geo_builder.py), plus the aura sprites of the Key aura.

    python3 -B tools/gen_key_geo.py            # writes both models, their animations, textures and glow maps, and the aura textures
    python3 -B tools/geo_check.py key_gate key_great_key

key_gate       a tall ornate door in a stepped arch of violet stone and gold, a glowing keyhole across its two leaves, a void behind the
               leaves, three small keys hanging from the arch, drifting runes. Clips: idle (loop), open, open_hold (loop), close.
key_great_key  a giant ornate key lying along z (tip on -z = the prop's front, trefoil bow with a gem on +z). Clips: idle (loop, slow
               spin and bob), thrust (a stab with a full turn).

Both are one cutout pass plus a glow map (drawn additive by GeoDraw when the painter passes a glow colour).
Textures are painted here: every cube gets a material (gold, stone, wood, gem ...) or a hand painted face, the cubes are packed into the
sheet automatically (shelf packing), and the glow map holds only what shines (keyhole, runes, gems, inlays).
Deterministic (fixed seeds). Needs numpy and Pillow.
"""
import math
import os
import sys

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

from geo_builder import Model, Anim, ROOT


# ================================================================================================ small image helpers
def vnoise(w, h, seed, scale):
    """Smooth value noise in 0..1 (bicubic upsampled grid)."""
    rng = np.random.RandomState(seed)
    g = rng.rand(max(2, int(h / scale) + 2), max(2, int(w / scale) + 2))
    im = Image.fromarray((g * 255).astype(np.uint8)).resize((max(1, w), max(1, h)), Image.BICUBIC)
    return np.asarray(im, np.float32) / 255.0


def ramp(t, stops):
    t = np.clip(t, 0.0, 1.0)
    xs = [s[0] for s in stops]
    out = np.zeros(t.shape + (3,), np.float32)
    for c in range(3):
        out[..., c] = np.interp(t, xs, [s[1][c] for s in stops])
    return out


def to_arr(im):
    return np.asarray(im.convert("RGB"), np.float32) / 255.0


S = 8                                           # supersampling of the hand painted faces


class Canvas:
    """A hand painting surface: draw in texel units (1 = one pixel of the final face), get a numpy rgb + glow back."""

    def __init__(self, w, h, base=(0, 0, 0)):
        self.w, self.h = w, h
        self.col = Image.new("RGB", (w * S, h * S), tuple(int(c * 255) for c in base))
        self.glo = Image.new("RGB", (w * S, h * S), (0, 0, 0))
        self.dc, self.dg = ImageDraw.Draw(self.col), ImageDraw.Draw(self.glo)

    @staticmethod
    def _c(c):
        return tuple(int(max(0, min(1, v)) * 255) for v in c)

    def line(self, pts, w, col, glow=None):
        p = [(x * S, y * S) for x, y in pts]
        self.dc.line(p, fill=self._c(col), width=max(1, int(w * S)), joint="curve")
        if glow is not None:
            self.dg.line(p, fill=self._c(glow), width=max(1, int(w * S)), joint="curve")

    def rect(self, x0, y0, x1, y1, col, glow=None):
        b = [x0 * S, y0 * S, x1 * S - 1, y1 * S - 1]
        self.dc.rectangle(b, fill=self._c(col))
        if glow is not None:
            self.dg.rectangle(b, fill=self._c(glow))

    def ellipse(self, cx, cy, rx, ry, col, glow=None):
        b = [(cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S]
        self.dc.ellipse(b, fill=self._c(col))
        if glow is not None:
            self.dg.ellipse(b, fill=self._c(glow))

    def poly(self, pts, col, glow=None):
        p = [(x * S, y * S) for x, y in pts]
        self.dc.polygon(p, fill=self._c(col))
        if glow is not None:
            self.dg.polygon(p, fill=self._c(glow))

    def arc(self, cx, cy, r, a0, a1, w, col, glow=None):
        b = [(cx - r) * S, (cy - r) * S, (cx + r) * S, (cy + r) * S]
        self.dc.arc(b, a0, a1, fill=self._c(col), width=max(1, int(w * S)))
        if glow is not None:
            self.dg.arc(b, a0, a1, fill=self._c(glow), width=max(1, int(w * S)))

    def out(self):
        c = self.col.resize((self.w, self.h), Image.LANCZOS)
        g = self.glo.resize((self.w, self.h), Image.LANCZOS)
        return to_arr(c), to_arr(g)


# ================================================================================================ materials
GOLD = [(0.0, (0.30, 0.17, 0.04)), (0.35, (0.62, 0.40, 0.10)), (0.7, (0.93, 0.74, 0.28)), (1.0, (1.0, 0.94, 0.62))]
GOLD_OLD = [(0.0, (0.16, 0.10, 0.04)), (0.4, (0.40, 0.27, 0.09)), (0.8, (0.70, 0.52, 0.2)), (1.0, (0.85, 0.7, 0.35))]
STONE = [(0.0, (0.08, 0.07, 0.14)), (0.4, (0.19, 0.16, 0.30)), (0.8, (0.34, 0.30, 0.50)), (1.0, (0.46, 0.42, 0.64))]
STONE_DARK = [(0.0, (0.05, 0.04, 0.09)), (0.5, (0.12, 0.10, 0.20)), (1.0, (0.24, 0.21, 0.36))]
WOOD = [(0.0, (0.07, 0.04, 0.12)), (0.5, (0.17, 0.10, 0.26)), (1.0, (0.30, 0.19, 0.42))]
GEM = [(0.0, (0.28, 0.06, 0.50)), (0.45, (0.55, 0.22, 0.88)), (0.8, (0.84, 0.58, 1.0)), (1.0, (1.0, 0.92, 1.0))]
GLOWGOLD = [(0.0, (0.75, 0.5, 0.14)), (0.6, (1.0, 0.82, 0.38)), (1.0, (1.0, 0.97, 0.78))]
VOID = [(0.0, (0.02, 0.0, 0.06)), (0.5, (0.12, 0.04, 0.26)), (1.0, (0.36, 0.16, 0.62))]

MATS = {
    "gold": dict(stops=GOLD, grain=0.07, streak=0.10, spot=0.0, bias=0.14),
    "gold_old": dict(stops=GOLD_OLD, grain=0.09, streak=0.06, spot=0.12, bias=0.10),
    "stone": dict(stops=STONE, grain=0.08, bricks=8, spot=0.0),
    "stone_dark": dict(stops=STONE_DARK, grain=0.08, bricks=0, spot=0.1),
    "wood": dict(stops=WOOD, grain=0.06, vgrain=0.18),
    "gem": dict(stops=GEM, grain=0.03, facets=True, glow=(0.62, 0.34, 1.0)),
    "glowgold": dict(stops=GLOWGOLD, grain=0.04, glow=(1.0, 0.8, 0.35)),
    "void": dict(stops=VOID, grain=0.05, glow=(0.25, 0.1, 0.5)),
    "inlay": dict(stops=WOOD, grain=0.05, dots=True),
}

SHADE = {"top": 1.10, "north": 1.0, "south": 0.93, "east": 0.88, "west": 0.88, "bottom": 0.72}


def mat_face(name, face, w, h, seed):
    """One face of a material cube: (rgb, glow) arrays of h x w."""
    m = MATS[name]
    w, h = max(1, w), max(1, h)
    lo = vnoise(w, h, seed, 4.0)
    t = 0.50 + m.get("bias", 0.0) + (lo - 0.5) * 0.55
    t += (np.random.RandomState(seed + 1).rand(h, w).astype(np.float32) - 0.5) * m.get("grain", 0.05) * 2
    yy = np.linspace(0.12, -0.12, h, dtype=np.float32)[:, None]
    t = t + yy
    if m.get("streak"):
        rows = np.random.RandomState(seed + 2).rand(h, 1).astype(np.float32) - 0.5
        t = t + rows * m["streak"] * 2
    if m.get("vgrain"):
        cols = vnoise(w, 1, seed + 3, 1.5)
        t = t + (cols - 0.5) * m["vgrain"] * 2
    if m.get("spot"):
        sp = vnoise(w, h, seed + 4, 2.0)
        t = t - np.clip(sp - 0.62, 0, 1) * m["spot"] * 5
    rgb = ramp(t, m["stops"])
    br = m.get("bricks", 0)
    if br and w > 3 and h > 3:
        for row, y in enumerate(range(0, h, br)):
            rgb[y, :, :] *= 0.55
            off = (row % 2) * (br // 2 + 1)
            for x in range(off, w, br * 2):
                rgb[y:y + br, min(x, w - 1), :] *= 0.62
    if m.get("dots") and w > 3 and h > 3:
        rng = np.random.RandomState(seed + 7)
        gl = np.zeros((h, w, 3), np.float32)
        for _ in range(max(1, (w * h) // 14)):
            x, y = rng.randint(0, w), rng.randint(0, h)
            rgb[y, x] = (0.95, 0.78, 0.35)
            gl[y, x] = (0.85, 0.6, 0.25)
        return finish(rgb, w, h, face), gl
    if m.get("facets"):
        yy2, xx2 = np.mgrid[0:h, 0:w].astype(np.float32)
        d = np.abs(((xx2 + 0.5) / w - 0.5)) + np.abs(((yy2 + 0.5) / h - 0.5))
        rgb = ramp(0.25 + (1.0 - d * 1.5) * 0.55 + (lo - 0.5) * 0.3, m["stops"])
        if w > 2 and h > 2:
            rgb[0, :, :] *= 0.8
            rgb[:, 0, :] *= 0.8
    gl = np.zeros((h, w, 3), np.float32)
    if m.get("glow"):
        gc = np.array(m["glow"], np.float32)
        gl = (0.7 + 0.3 * lo)[..., None] * gc[None, None, :]
        if m.get("facets"):
            gl *= np.clip(1.15 - d * 1.2, 0.3, 1.0)[..., None]
    return finish(rgb, w, h, face), gl


def finish(rgb, w, h, face):
    """Baked shading: a lit top edge, a shaded bottom edge and per face brightness."""
    rgb = rgb * SHADE.get(face, 1.0)
    if w > 3 and h > 3:
        rgb[0, :, :] *= 1.18
        rgb[:, 0, :] *= 1.10
        rgb[-1, :, :] *= 0.78
        rgb[:, -1, :] *= 0.82
    return np.clip(rgb, 0, 1)


# ================================================================================================ the model + texture packer
class KeyModel:
    """A geo_builder Model that remembers a material per cube, packs the cubes into the sheet and paints it."""

    def __init__(self, name, tex_w, tex_h):
        self.m = Model("key", name, tex_w, tex_h)
        self.recs = []
        self.seed = 11

    def bone(self, *a, **k):
        return self.m.bone(*a, **k)

    def cube(self, bone, origin, size, mat="gold", inflate=0.0, rotation=None, pivot=None, custom=None, glow=None, seed=None):
        """size must be whole numbers (the unfolded box is laid out in whole pixels). custom: fn(face, w, h) -> (rgb, glow) or None."""
        sx, sy, sz = [int(round(v)) for v in size]
        c = self.m.cube(bone, origin, (sx, sy, sz), uv=(0, 0), inflate=inflate, rotation=rotation, pivot=pivot)
        self.seed += 7
        self.recs.append(dict(c=c, sx=sx, sy=sy, sz=sz, mat=mat, custom=custom, glow=glow, seed=seed if seed is not None else self.seed))
        return c

    def pack(self):
        items = sorted(self.recs, key=lambda r: (-(r["sz"] + r["sy"]), -(2 * (r["sx"] + r["sz"]))))
        W, H = self.m.tex_w, self.m.tex_h
        x = y = rowh = 0
        for r in items:
            bw, bh = 2 * (r["sx"] + r["sz"]), r["sz"] + r["sy"]
            if x + bw > W:
                x, y, rowh = 0, y + rowh + 1, 0
            if y + bh > H or bw > W:
                raise SystemExit("texture sheet %dx%d too small for %s (needs more than y=%d)" % (W, H, self.m.id, y + bh))
            r["c"]["uv"] = [x, y]
            r["u"], r["v"] = x, y
            x += bw + 1
            rowh = max(rowh, bh)
        return y + rowh

    def paint(self):
        used = self.pack()
        W, H = self.m.tex_w, self.m.tex_h
        tex = np.zeros((H, W, 4), np.float32)
        glow = np.zeros((H, W, 4), np.float32)
        for r in self.recs:
            u, v, sx, sy, sz = r["u"], r["v"], r["sx"], r["sy"], r["sz"]
            faces = {"west": (u, v + sz, sz, sy), "north": (u + sz, v + sz, sx, sy), "east": (u + sz + sx, v + sz, sz, sy),
                     "south": (u + 2 * sz + sx, v + sz, sx, sy), "top": (u + sz, v, sx, sz), "bottom": (u + sz + sx, v, sx, sz)}
            for fi, (name, (fx, fy, fw, fh)) in enumerate(faces.items()):
                if fw <= 0 or fh <= 0:
                    continue
                res = r["custom"](name, fw, fh) if r["custom"] else None
                if res is None:
                    res = mat_face(r["mat"], name, fw, fh, r["seed"] + fi * 13)
                rgb, gl = res
                if r["glow"] is not None:
                    gl = np.maximum(gl, np.ones((fh, fw, 3), np.float32) * np.array(r["glow"], np.float32)[None, None, :] * (0.8 + 0.2 * vnoise(fw, fh, r["seed"] + fi, 2.0))[..., None])
                tex[fy:fy + fh, fx:fx + fw, :3] = rgb
                tex[fy:fy + fh, fx:fx + fw, 3] = 1.0
                gm = np.clip(gl.max(axis=2), 0, 1)
                glow[fy:fy + fh, fx:fx + fw, :3] = gl
                glow[fy:fy + fh, fx:fx + fw, 3] = (gm > 0.02).astype(np.float32)
        a = Image.fromarray((np.clip(tex, 0, 1) * 255).astype(np.uint8), "RGBA")
        g = Image.fromarray((np.clip(glow, 0, 1) * 255).astype(np.uint8), "RGBA")
        return a, g, used

    def save(self, anim):
        img, glow, used = self.paint()
        self.m.save(anim)
        self.m.save_texture(img, glow)
        print("%-16s %3d cubes, sheet %dx%d (rows used to y=%d)" % (self.m.id, self.m.cube_count(), self.m.tex_w, self.m.tex_h, used))
        return img, glow


# ================================================================================================ hand painted faces
def keyhole_shape(cv, cx, cy, s, col, glow):
    """A keyhole (round head + tapered slot) centred at cx, cy; s = head radius."""
    cv.ellipse(cx, cy, s, s, col, glow)
    cv.poly([(cx - s * 0.55, cy + s * 0.5), (cx + s * 0.55, cy + s * 0.5), (cx + s * 0.9, cy + s * 2.9), (cx - s * 0.9, cy + s * 2.9)], col, glow)


def door_front(inner_left):
    """The front of one door leaf (13 x 44 texels). inner_left: the inner (meeting) edge is the LEFT edge of the face."""
    W, H = 13, 44

    def fn(face, w, h):
        if face not in ("north", "south"):
            return None
        cv = Canvas(W, H)
        rng = np.random.RandomState(5)
        # dark violet planks with a grain
        base = np.zeros((H * S, W * S, 3), np.float32)
        ng = vnoise(W * S, H * S, 3, 6.0)
        gr = vnoise(W * S, 1, 4, 3.0)[0][None, :]
        tt = 0.45 + (ng - 0.5) * 0.5 + (gr - 0.5) * 0.5
        base = ramp(tt, WOOD)
        cv.col = Image.fromarray((np.clip(base, 0, 1) * 255).astype(np.uint8), "RGB")
        cv.dc = ImageDraw.Draw(cv.col)
        # gold frame, bevelled
        cv.rect(0, 0, W, 1.3, (0.78, 0.58, 0.2)); cv.rect(0, H - 1.3, W, H, (0.5, 0.33, 0.1))
        cv.rect(0, 0, 1.2, H, (0.88, 0.68, 0.26)); cv.rect(W - 1.2, 0, W, H, (0.5, 0.33, 0.1))
        cv.rect(1.2, 1.3, W - 1.2, 1.7, (1.0, 0.9, 0.55)); cv.rect(1.2, H - 1.7, W - 1.2, H - 1.3, (0.35, 0.22, 0.07))
        # hinge straps
        for y0 in (8.5, 33.0):
            cv.rect(0, y0, W, y0 + 2.6, (0.62, 0.43, 0.14))
            cv.rect(0, y0, W, y0 + 0.5, (0.98, 0.82, 0.4))
            cv.rect(0, y0 + 2.2, W, y0 + 2.6, (0.3, 0.19, 0.05))
            for x in (1.9, 6.5, 11.2):
                cv.ellipse(x, y0 + 1.3, 0.55, 0.55, (1.0, 0.9, 0.5), (0.5, 0.35, 0.1))
        # recessed panels with a gold edge
        for y0, y1 in ((3.2, 7.6), (36.2, 40.6)):
            cv.rect(2.2, y0, W - 2.2, y1, (0.05, 0.02, 0.1))
            cv.line([(2.2, y0), (W - 2.2, y0), (W - 2.2, y1), (2.2, y1), (2.2, y0)], 0.3, (0.85, 0.65, 0.25))
            cv.line([(3.4, (y0 + y1) / 2), (W - 3.4, (y0 + y1) / 2)], 0.35, (0.6, 0.35, 1.0), (0.45, 0.25, 0.8))
        # the vine: a long S curve of gold with leaves and glowing rune berries
        pts = []
        for k in range(0, 61):
            f = k / 60.0
            y = 12.2 + f * 19.5
            x = 7.7 + math.sin(f * math.pi * 3.0) * 2.7
            pts.append((x, y))
        cv.line(pts, 0.55, (0.9, 0.7, 0.28), (0.5, 0.36, 0.14))
        for k in range(4, 58, 6):
            x, y = pts[k]
            d = 1 if (k // 6) % 2 == 0 else -1
            cv.poly([(x, y), (x + d * 2.0, y - 0.9), (x + d * 2.6, y + 0.2), (x + d * 1.2, y + 0.9)], (0.82, 0.62, 0.22), (0.4, 0.28, 0.1))
            cv.ellipse(x + d * 2.4, y - 0.1, 0.45, 0.45, (1.0, 0.95, 0.7), (0.8, 0.5, 1.0))
        # top and bottom rosettes
        for cy in (10.0, 34.6 + 0.0):
            pass
        # the keyhole on the meeting edge, half of it on this leaf
        kx = 0.0
        ky = 19.0
        keyhole_shape(cv, kx, ky, 3.3, (0.45, 0.3, 0.08), (0.0, 0.0, 0.0))
        keyhole_shape(cv, kx, ky, 2.75, (0.08, 0.02, 0.16), (0.0, 0.0, 0.0))
        keyhole_shape(cv, kx, ky, 2.2, (1.0, 0.86, 0.45), (1.0, 0.74, 0.22))
        keyhole_shape(cv, kx, ky, 1.2, (1.0, 0.98, 0.9), (1.0, 0.95, 0.85))
        # rune ticks on the lock plate ring
        for a in range(0, 360, 30):
            ca, sa = math.cos(math.radians(a)), math.sin(math.radians(a))
            cv.line([(kx + ca * 4.1, ky + sa * 4.1), (kx + ca * 4.9, ky + sa * 4.9)], 0.3, (0.95, 0.78, 0.32), (0.8, 0.45, 1.0))
        rgb, gl = cv.out()
        if not inner_left:
            rgb, gl = rgb[:, ::-1].copy(), gl[:, ::-1].copy()
        if face == "south":
            gl = gl * 0.5
        return rgb, gl
    return fn


def portal_face(face, w, h):
    """The void behind the doors: a violet well with a golden spiral and stars (28 x 44)."""
    if face not in ("north", "south"):
        return None
    cv = Canvas(w, h, (0.03, 0.0, 0.08))
    rng = np.random.RandomState(9)
    nz = vnoise(w * S, h * S, 5, 14.0)
    yy, xx = np.mgrid[0:h * S, 0:w * S].astype(np.float32)
    d = np.sqrt(((xx / S - w / 2) / (w / 2)) ** 2 + ((yy / S - h * 0.45) / (h * 0.5)) ** 2)
    ang = np.arctan2(yy / S - h * 0.45, xx / S - w / 2)
    spiral = 0.5 + 0.5 * np.sin(ang * 3 + d * 9.0 + nz * 3.0)
    t = np.clip(0.8 - d * 0.55 + spiral * 0.25, 0, 1)
    img = ramp(t, [(0.0, (0.02, 0.0, 0.06)), (0.45, (0.18, 0.07, 0.36)), (0.8, (0.55, 0.32, 0.9)), (1.0, (1.0, 0.85, 0.6))])
    glw = img * 0.9
    cv.col = Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGB")
    cv.glo = Image.fromarray((np.clip(glw, 0, 1) * 255).astype(np.uint8), "RGB")
    cv.dc, cv.dg = ImageDraw.Draw(cv.col), ImageDraw.Draw(cv.glo)
    for _ in range(26):
        x, y = rng.uniform(1, w - 1), rng.uniform(1, h - 1)
        cv.ellipse(x, y, 0.35, 0.35, (1.0, 0.95, 0.8), (1.0, 0.95, 0.8))
    # a great keyhole of light in the middle
    keyhole_shape(cv, w / 2, h * 0.34, 4.2, (1.0, 0.93, 0.68), (1.0, 0.9, 0.62))
    rgb, gl = cv.out()
    return rgb, gl


def pillar_face(face, w, h):
    """A pillar shaft side: stone blocks, a gold inlay strip down the middle with a glowing diamond every 8 texels."""
    rgb, gl = mat_face("stone", face, w, h, 77)
    if face in ("north", "south", "east", "west") and w >= 3:
        cx = w // 2
        rgb[:, cx, :] = (0.78, 0.58, 0.2)
        rgb[:, max(0, cx - 1), :] = rgb[:, max(0, cx - 1), :] * 0.7 + np.array([0.18, 0.13, 0.04])
        for y in range(4, h - 3, 8):
            for dy in (-1, 0, 1):
                rgb[y + dy, max(0, cx - (1 - abs(dy))):cx + (1 - abs(dy)) + 1, :] = (1.0, 0.86, 0.45)
                gl[y + dy, max(0, cx - (1 - abs(dy))):cx + (1 - abs(dy)) + 1, :] = (0.9, 0.5, 1.0)
    return rgb, gl


def shaft_face(face, w, h):
    """A key shaft: brushed gold with a violet glowing rune groove along the top face."""
    rgb, gl = mat_face("gold", face, w, h, 91)
    if face == "top":
        cx = w // 2
        rgb[:, cx, :] = (0.2, 0.08, 0.35)
        gl[:, cx, :] = (0.75, 0.45, 1.0)
        for y in range(2, h, 5):
            gl[y, :, :] = np.maximum(gl[y, :, :], (0.6, 0.35, 0.9))
    return rgb, gl


def bit_face(face, w, h):
    """A key bit tooth: gold with a bright worn edge and a violet etched line."""
    rgb, gl = mat_face("gold", face, w, h, 55)
    if face in ("east", "west") and w >= 3 and h >= 3:
        rgb[1:-1, w // 2, :] = (0.22, 0.1, 0.38)
        gl[1:-1, w // 2, :] = (0.5, 0.3, 0.85)
    return rgb, gl


# ================================================================================================ THE GATE
def build_gate():
    k = KeyModel("gate", 256, 176)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("frame", parent="root", pivot=(0, 0, 0))
    c = k.cube

    # ---- plinth: a stepped base with a gold line, a doorstep in front
    c("frame", (-24, 0, -8), (48, 3, 16), "stone_dark")
    c("frame", (-22, 3, -7), (44, 3, 14), "stone")
    c("frame", (-22.5, 5.6, -7.6), (45, 1, 15), "gold")
    c("frame", (-15, 0, -11), (30, 2, 3), "stone_dark")
    for sx in (-1, 1):
        c("frame", (sx * 21 - 1.5, 0, -9.5), (3, 4, 2), "gold_old")

    # ---- the two pillars (mirrored): base, fluted shaft, gold rings, capital, spire
    for sg in (-1, 1):
        x0 = 14 if sg > 0 else -20
        c("frame", (x0 - 0.5, 6, -4.5), (7, 3, 9), "stone_dark")
        c("frame", (x0, 9, -3.5), (6, 40, 7), custom=pillar_face)
        for dx in (0.4, 4.6):
            c("frame", (x0 + dx, 9, -4.3), (1, 40, 1), "stone")
        for y in (17, 30, 41):
            c("frame", (x0 - 0.4, y, -3.9), (6, 2, 8), "gold", inflate=0.25)
        c("frame", (x0 - 1, 49, -4.5), (8, 3, 9), "stone_dark")
        c("frame", (x0 - 1.5, 52, -5), (9, 2, 10), "gold")
        c("frame", (x0 - 0.5, 54, -4), (7, 1, 8), "stone")
        # a little spire of stacked gold
        c("frame", (x0 + 1, 55, -2), (4, 2, 4), "gold")
        c("frame", (x0 + 1.5, 57, -1.5), (3, 2, 3), "gold")
        c("frame", (x0 + 2, 59, -1), (2, 3, 2), "gold", inflate=0.1)
        c("frame", (x0 + 2.5, 62, -0.5), (1, 2, 1), "glowgold")
        # a glowing keyhole rune on the front of the base block
        c("frame", (x0 + 1.5, 10.5, -4.6), (3, 3, 1), "glowgold", glow=(1.0, 0.75, 0.3))

    # ---- the arch: stone voussoirs (alternating depth so coplanar fronts never fight) with a gold moulding inside
    R0, T, N = 14.0, 5.0, 9
    Rm = R0 + T / 2
    L = 2 * Rm * math.tan(math.radians(90.0 / N))
    for i in range(N):
        th = 90.0 / N + i * 180.0 / N                      # 10, 30 ... 170 degrees from +x
        cx, cy = Rm * math.cos(math.radians(th)), 50 + Rm * math.sin(math.radians(th))
        depth = 9 if i % 2 == 0 else 10
        sz = (int(round(L)) + 1, int(T), depth)
        key = (i == N // 2)
        c("frame", (cx - sz[0] / 2, cy - sz[1] / 2, -depth / 2), sz, "gold" if key else ("stone" if i % 2 == 0 else "stone_dark"),
          rotation=(0, 0, th - 90), pivot=(cx, cy, 0))
    for i in range(N):
        th = 90.0 / N + i * 180.0 / N
        r2 = R0 - 0.6
        cx, cy = r2 * math.cos(math.radians(th)), 50 + r2 * math.sin(math.radians(th))
        l2 = int(round(2 * r2 * math.tan(math.radians(90.0 / N)))) + 1
        c("frame", (cx - l2 / 2, cy - 1.0, -5.5 if i % 2 == 0 else -5.8), (l2, 2, 11 if i % 2 == 0 else 12), "gold",
          rotation=(0, 0, th - 90), pivot=(cx, cy, 0))
    # keystone gem and the crown above it
    c("frame", (-2.5, 70.2, -5.2), (5, 5, 2), "gem", rotation=(0, 0, 45), pivot=(0, 72.7, -4.2), glow=(0.7, 0.4, 1.0))
    c("frame", (-4, 67.5, -3.5), (8, 3, 7), "gold", inflate=0.1)                 # the pedestal the crown stands on
    c("frame", (-2.5, 70, -2.5), (5, 2, 5), "gold_old")
    c("frame", (-1, 72, -1), (2, 5, 2), "gold", inflate=0.1)
    c("frame", (-0.5, 77, -0.5), (1, 2, 1), "glowgold")
    for sg, ang in ((-1, 32), (1, -32)):
        c("frame", (sg * 4.5 - 0.75, 71.5, -1), (2, 6, 2), "gold", rotation=(0, 0, ang), pivot=(sg * 4.5, 71.5, 0))

    # ---- the tympanum: a half disc of dark inlaid slabs filling the arch above the leaves
    for j in range(7):
        hy = 1.0 + j * 2.0
        half = math.sqrt(max(0.5, (R0 - 0.4) ** 2 - (hy + 1) ** 2))
        wd = int(2 * half)
        c("frame", (-wd / 2, 50 + j * 2, -1.5), (wd, 2, 3), "inlay")
    # a golden sun in the tympanum with a glowing core
    c("frame", (-3, 55, -2.2), (6, 6, 1), "gold", rotation=(0, 0, 45), pivot=(0, 58, -2))
    c("frame", (-1.5, 56.5, -2.6), (3, 3, 1), "glowgold", rotation=(0, 0, 45), pivot=(0, 58, -2), glow=(1.0, 0.8, 0.4))

    # ---- the void behind the leaves
    c("frame", (-14, 6, 1.5), (28, 44, 1), custom=portal_face)

    # ---- the two door leaves (hinged on the pillar side)
    for name, sg, inner_left in (("door_l", 1, True), ("door_r", -1, False)):
        piv = (14 * sg, 0, -0.5)
        k.bone(name, parent="root", pivot=piv)
        x0 = 1 if sg > 0 else -14                            # the leaf spans 13 texels from the meeting edge to the hinge
        c(name, (x0, 6, -2), (13, 44, 3), custom=door_front(inner_left))
        # rails, hinge side stile and bosses stand proud of the leaf
        c(name, (x0 - 0.2, 6, -2.5), (13, 3, 4), "gold_old", inflate=0.1)
        c(name, (x0 - 0.2, 47, -2.5), (13, 3, 4), "gold_old", inflate=0.1)
        hx = 12 if sg > 0 else -14
        c(name, (x0 + (11.6 if sg > 0 else -0.2), 8, -2.7), (2, 40, 4), "gold", inflate=0.05)
        for y in (13, 38):
            c(name, (x0 + (5 if sg > 0 else 3), y, -3.2), (5, 2, 1), "gold")
            for dx in (0, 4):
                c(name, (x0 + (5 if sg > 0 else 3) + dx, y + 0.3, -3.6), (1, 1, 1), "glowgold", inflate=0.3)
        # a ring handle near the meeting edge
        hxm = x0 + (1.6 if sg > 0 else 9.4)
        c(name, (hxm, 28.5, -3.0), (3, 3, 1), "gold_old", inflate=0.2)

    # ---- three small keys hung from the capitals and the keystone (they swing)
    for nm, hx_, hy_ in (("hang_l", 17.5, 50), ("hang_c", 0.0, 69), ("hang_r", -17.5, 50)):
        k.bone(nm, parent="root", pivot=(hx_, hy_, -6.2))
        c(nm, (hx_ - 0.5, hy_ - 5, -6.7), (1, 5, 1), "gold_old")                    # the chain
        c(nm, (hx_ - 0.5, hy_ - 7, -6.7), (1, 1, 1), "gold")
        c(nm, (hx_ - 1.5, hy_ - 11, -6.9), (3, 3, 1), "gold", inflate=0.25)          # bow
        c(nm, (hx_ - 0.5, hy_ - 10, -6.95), (1, 1, 1), "stone_dark", inflate=-0.0)
        c(nm, (hx_ - 0.5, hy_ - 17, -6.7), (1, 6, 1), "gold")                         # shaft
        c(nm, (hx_ + 0.5, hy_ - 17, -6.7), (2, 1, 1), "gold")                         # teeth
        c(nm, (hx_ + 0.5, hy_ - 15, -6.7), (1, 1, 1), "gold")
        c(nm, (hx_ - 0.5, hy_ - 11.5, -7.15), (1, 1, 1), "glowgold", glow=(1.0, 0.8, 0.4))

    # ---- drifting runes (four small violet-gold diamonds that circle the arch)
    for i in range(4):
        nm = "rune_%d" % (i + 1)
        a = i * math.pi / 2
        px, py = 25 * math.cos(a) * 0.9, 48 + 22 * math.sin(a)
        k.bone(nm, parent="root", pivot=(px, py, -3))
        c(nm, (px - 1.5, py - 1.5, -4.5), (3, 3, 3), "gem", rotation=(0, 0, 45), pivot=(px, py, -3), glow=(0.8, 0.5, 1.0))

    # ---- animations (seconds; rotations in degrees, positions in pixels)
    an = Anim()
    an.clip("idle", length=4.0, loop=True)
    for nm, ph in (("hang_l", 0.0), ("hang_c", 1.3), ("hang_r", 2.6)):
        t = lambda f: (f * 4.0 + ph * 0) % 4.0
        an.rot("idle", nm, {0.0: (0, 0, 5), 1.0: (3, 0, 0), 2.0: (0, 0, -5), 3.0: (-3, 0, 0), 4.0: (0, 0, 5)} if ph == 0 else
               {0.0: (0, 0, -4), 1.0: (-3, 0, 0), 2.0: (0, 0, 4), 3.0: (3, 0, 0), 4.0: (0, 0, -4)})
    for i in range(4):
        nm = "rune_%d" % (i + 1)
        o = i * 1.0
        pts = {}
        for q in range(5):
            f = (q + o) % 4
            ang = f / 4.0 * 2 * math.pi
            pts[float(q)] = (round(math.sin(ang) * 3.0, 2), round(math.cos(ang) * 4.0, 2), 0)
        pts[4.0] = pts[0.0]
        an.pos("idle", nm, pts)
        an.rot("idle", nm, {0.0: (0, 0, 0), 4.0: (0, 0, 0)})
    an.clip("open", length=0.9, loop=False)
    an.rot("open", "door_l", {0.0: (0, 0, 0), 0.15: (0, 4, 0), 0.9: (0, -78, 0)})
    an.rot("open", "door_r", {0.0: (0, 0, 0), 0.15: (0, -4, 0), 0.9: (0, 78, 0)})
    for nm in ("hang_l", "hang_c", "hang_r"):
        an.rot("open", nm, {0.0: (0, 0, 0), 0.3: (0, 0, 9), 0.9: (0, 0, 0)})
    for i in range(4):
        an.pos("open", "rune_%d" % (i + 1), {0.0: (0, 0, 0), 0.9: (0, 4 + i, 0)})
    an.clip("open_hold", length=2.0, loop=True)
    an.rot("open_hold", "door_l", {0.0: (0, -78, 0), 1.0: (0, -76, 0), 2.0: (0, -78, 0)})
    an.rot("open_hold", "door_r", {0.0: (0, 78, 0), 1.0: (0, 76, 0), 2.0: (0, 78, 0)})
    for nm, sg in (("hang_l", 1), ("hang_c", -1), ("hang_r", 1)):
        an.rot("open_hold", nm, {0.0: (0, 0, 4 * sg), 1.0: (0, 0, -4 * sg), 2.0: (0, 0, 4 * sg)})
    for i in range(4):
        y0 = 4 + i
        an.pos("open_hold", "rune_%d" % (i + 1), {0.0: (0, y0, 0), 1.0: (0, y0 + 3, 0), 2.0: (0, y0, 0)})
    an.clip("close", length=0.8, loop=False)
    an.rot("close", "door_l", {0.0: (0, -78, 0), 0.65: (0, 0, 0), 0.72: (0, 3, 0), 0.8: (0, 0, 0)})
    an.rot("close", "door_r", {0.0: (0, 78, 0), 0.65: (0, 0, 0), 0.72: (0, -3, 0), 0.8: (0, 0, 0)})
    for i in range(4):
        an.pos("close", "rune_%d" % (i + 1), {0.0: (0, 4 + i, 0), 0.8: (0, 0, 0)})
    return k, an


def lift(k, dy):
    """Raises the whole model by dy pixels (cubes, cube pivots and bone pivots)."""
    for b in k.m.bones.values():
        b["pivot"][1] += dy
        for cu in b["cubes"]:
            cu["origin"][1] += dy
            if "pivot" in cu:
                cu["pivot"][1] += dy


# ================================================================================================ THE GREAT KEY
def build_key():
    k = KeyModel("great_key", 128, 192)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("key", parent="root", pivot=(0, 0, 0))
    c = k.cube

    # ---- the trefoil bow: a ring of 12 segments (z centre +22), three lobes and a gem in the middle
    BZ, RR, NSEG = 23.0, 7.0, 12
    for i in range(NSEG):
        a = i * 360.0 / NSEG
        px, py = RR * math.cos(math.radians(a)), RR * math.sin(math.radians(a))
        c("key", (px - 2, py - 2, BZ - 1.5), (4, 4, 3), "gold", rotation=(0, 0, a + 90), pivot=(px, py, BZ), inflate=0.15)
    for i in range(3):
        a = 90 + i * 120
        px, py = (RR + 3.6) * math.cos(math.radians(a)), (RR + 3.6) * math.sin(math.radians(a))
        c("key", (px - 2, py - 2, BZ - 1.5), (4, 4, 3), "gold", rotation=(0, 0, 45), pivot=(px, py, BZ), inflate=0.2)
        c("key", (px - 1, py - 1, BZ - 2.2), (2, 2, 1), "glowgold", rotation=(0, 0, 45), pivot=(px, py, BZ), glow=(1.0, 0.8, 0.4))
    c("key", (-2.5, -2.5, BZ - 1.2), (5, 5, 2), "gem", rotation=(0, 0, 45), pivot=(0, 0, BZ), glow=(0.75, 0.45, 1.0))
    c("key", (-1, -1, BZ - 2), (2, 2, 4), "glowgold", rotation=(0, 0, 45), pivot=(0, 0, BZ), glow=(1.0, 0.9, 0.6))
    for sg in (-1, 1):
        c("key", (sg * 0 - 0.5, -RR - 1.5 if sg < 0 else RR + 0.5, BZ - 1.2), (1, 1, 2), "gold_old")

    # ---- collar between bow and shaft: stacked rings and a cross guard of winged blades
    c("key", (-3.5, -3.5, 13), (7, 7, 2), "gold", inflate=0.25)
    c("key", (-2.5, -2.5, 15), (5, 5, 3), "gold_old", inflate=0.2)
    c("key", (-3, -3, 17.5), (6, 6, 2), "gold", inflate=0.25)
    c("key", (-2, -2, 11), (4, 4, 2), "stone", inflate=0.3)
    for sg in (-1, 1):
        c("key", (sg * 6.5 - 1.5, -1, 10), (3, 2, 5), "gold", rotation=(0, 0, 0), pivot=(sg * 5, 0, 12))
        c("key", (sg * 9.0 - 1.5, -0.8, 8), (3, 2, 5), "gold", rotation=(0, sg * 18, 0), pivot=(sg * 8, 0, 11))
        c("key", (sg * 11.0 - 1.0, -0.6, 6.5), (2, 1, 4), "gold_old", rotation=(0, sg * 32, 0), pivot=(sg * 10, 0, 9))
        c("key", (sg * 5.5 - 1, 1.2, 10.5), (2, 1, 3), "gem", glow=(0.7, 0.4, 1.0))
    # ---- the shaft: a round-ish core with rings and beads and a rune groove
    c("key", (-2, -2, -22), (4, 4, 34), custom=shaft_face, inflate=0.5)
    c("key", (-1.2, -1.2, -22), (3, 3, 34), "gold", inflate=0.9)
    for z in (9, 3, -3, -9, -15):
        c("key", (-3, -3, z), (6, 6, 1), "gold_old", inflate=0.25)
    for z in (6, 0, -6, -12):
        for dx, dy in ((0, 3.4), (0, -3.4), (3.4, 0), (-3.4, 0)):
            c("key", (dx - 0.5, dy - 0.5, z), (1, 1, 1), "gold", inflate=0.3)
    for z in (11, -18):
        c("key", (-0.5, 2.4, z), (1, 1, 1), "glowgold", glow=(1.0, 0.82, 0.4))

    # ---- the bit: a tall plate with teeth along the shaft (grows toward +y)
    heights = [8, 5, 9, 4, 8, 6, 9]
    for i, hgt in enumerate(heights):
        z = -22 + i * 3
        c("key", (-1.5, 2, z), (3, hgt, 3), custom=bit_face)
    c("key", (-1.8, 2, -23), (4, 2, 24), "gold", inflate=0.1)
    c("key", (-1.0, 10.2, -22), (2, 1, 3), "glowgold", glow=(1.0, 0.82, 0.45))
    c("key", (-1.0, 8.2, -10), (2, 1, 3), "glowgold", glow=(1.0, 0.82, 0.45))
    # ---- the tip: stepped to a point, the last section a rotated diamond
    c("key", (-1.8, -1.8, -25), (4, 4, 3), "gold", inflate=0.15)
    c("key", (-1.2, -1.2, -28), (3, 3, 3), "gold", inflate=0.1)
    c("key", (-0.8, -0.8, -31), (2, 2, 3), "gold")
    c("key", (-0.5, -0.5, -34), (1, 1, 3), "glowgold", glow=(1.0, 0.95, 0.75))

    lift(k, 12)                                              # the key's centre hovers 0.75 block over the model's origin
    an = Anim()
    an.clip("idle", length=4.0, loop=True)
    an.rot("idle", "key", {0.0: (0, 0, 0), 1.0: (0, 0, 90), 2.0: (0, 0, 180), 3.0: (0, 0, 270), 4.0: (0, 0, 360)})
    an.pos("idle", "root", {0.0: (0, 0, 0), 1.0: (0, 1.6, 0), 2.0: (0, 0, 0), 3.0: (0, -1.6, 0), 4.0: (0, 0, 0)})
    an.rot("idle", "root", {0.0: (0, 0, 0), 1.0: (3, 0, 0), 2.0: (0, 0, 0), 3.0: (-3, 0, 0), 4.0: (0, 0, 0)})
    an.clip("thrust", length=0.6, loop=False)
    an.pos("thrust", "root", {0.0: (0, 0, 0), 0.22: (0, 0, 10), 0.34: (0, 0, -16), 0.4: (0, 0, -16), 0.6: (0, 0, 0)})
    an.rot("thrust", "key", {0.0: (0, 0, 0), 0.22: (0, 0, 0), 0.34: (0, 0, 120), 0.6: (0, 0, 360)})
    an.rot("thrust", "root", {0.0: (0, 0, 0), 0.22: (-6, 0, 0), 0.34: (4, 0, 0), 0.6: (0, 0, 0)})
    return k, an


# ================================================================================================ THE MINI KEY (the aura's orbiting keys)
def build_mini():
    """A light version of the great key (about 16 cubes) for the three keys that orbit a player: same silhouette, same materials."""
    k = KeyModel("mini_key", 64, 64)
    k.bone("root", pivot=(0, 0, 0))
    k.bone("key", parent="root", pivot=(0, 0, 0))
    c = k.cube
    BZ, RR, NSEG = 17.0, 5.0, 8
    for i in range(NSEG):
        a = i * 360.0 / NSEG
        px, py = RR * math.cos(math.radians(a)), RR * math.sin(math.radians(a))
        c("key", (px - 2, py - 2, BZ - 1.5), (4, 4, 3), "gold", rotation=(0, 0, a + 90), pivot=(px, py, BZ), inflate=0.1)
    c("key", (-2, -2, BZ - 1.2), (4, 4, 2), "gem", rotation=(0, 0, 45), pivot=(0, 0, BZ), glow=(0.75, 0.45, 1.0))
    c("key", (-2.5, -2.5, 9), (5, 5, 2), "gold_old", inflate=0.2)
    c("key", (-1.5, -1.5, -20), (3, 3, 29), custom=shaft_face, inflate=0.3)
    for i, hgt in enumerate((5, 3, 5)):
        c("key", (-1, 1.5, -19 + i * 4), (2, hgt, 3), custom=bit_face)
    c("key", (-1, -1, -23), (2, 2, 3), "gold")
    c("key", (-0.5, -0.5, -25), (1, 1, 2), "glowgold", glow=(1.0, 0.95, 0.75))
    lift(k, 12)
    an = Anim()
    an.clip("idle", length=4.0, loop=True)
    an.rot("idle", "key", {0.0: (0, 0, 0), 1.0: (0, 0, 90), 2.0: (0, 0, 180), 3.0: (0, 0, 270), 4.0: (0, 0, 360)})
    return k, an


# ================================================================================================ aura sprites
def aura_textures():
    out = os.path.join(ROOT, "textures", "aura")
    os.makedirs(out, exist_ok=True)
    N = 256
    SSN = 4
    big = N * SSN
    img = Image.new("RGBA", (big, big), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    cx = cy = big // 2

    def cc(t):
        # gold -> violet blend
        a = np.array((255, 206, 90)); b = np.array((170, 110, 255))
        v = a * (1 - t) + b * t
        return tuple(int(x) for x in v)

    def ring(r, w, t, alpha=255):
        d.ellipse([cx - r * SSN, cy - r * SSN, cx + r * SSN, cy + r * SSN], outline=cc(t) + (alpha,), width=int(w * SSN))

    ring(120, 3.5, 0.0)
    ring(112, 1.5, 0.35, 200)
    ring(86, 2.5, 0.6)
    ring(80, 1.2, 0.2, 180)
    for i in range(48):                                           # tick marks between the rings
        a = math.radians(i * 7.5)
        r0, r1 = (88, 100) if i % 4 == 0 else (88, 94)
        d.line([cx + math.cos(a) * r0 * SSN, cy + math.sin(a) * r0 * SSN, cx + math.cos(a) * r1 * SSN, cy + math.sin(a) * r1 * SSN],
               fill=cc(0.45) + (230,), width=int(2 * SSN))
    for i in range(8):                                            # little key bows on the outer ring
        a = math.radians(i * 45 + 22.5)
        px, py = cx + math.cos(a) * 100 * SSN, cy + math.sin(a) * 100 * SSN
        d.ellipse([px - 6 * SSN, py - 6 * SSN, px + 6 * SSN, py + 6 * SSN], outline=cc(0.1) + (240,), width=int(2 * SSN))
        d.line([px, py, px + math.cos(a) * 11 * SSN, py + math.sin(a) * 11 * SSN], fill=cc(0.1) + (240,), width=int(2 * SSN))
    # the keyhole in the middle
    kc = cc(0.0) + (255,)
    d.ellipse([cx - 20 * SSN, cy - 36 * SSN, cx + 20 * SSN, cy + 4 * SSN], fill=kc)
    d.polygon([(cx - 11 * SSN, cy - 6 * SSN), (cx + 11 * SSN, cy - 6 * SSN), (cx + 25 * SSN, cy + 58 * SSN), (cx - 25 * SSN, cy + 58 * SSN)], fill=kc)
    d.ellipse([cx - 11 * SSN, cy - 27 * SSN, cx + 11 * SSN, cy - 5 * SSN], fill=(255, 250, 225, 255))
    d.polygon([(cx - 5 * SSN, cy - 8 * SSN), (cx + 5 * SSN, cy - 8 * SSN), (cx + 12 * SSN, cy + 46 * SSN), (cx - 12 * SSN, cy + 46 * SSN)], fill=(255, 250, 225, 255))
    glyph = img.resize((N, N), Image.LANCZOS)
    soft = glyph.filter(ImageFilter.GaussianBlur(5))
    comp = Image.alpha_composite(soft, glyph)
    comp = Image.alpha_composite(comp, glyph)
    a = np.asarray(comp).copy()
    a[a[..., 3] < 10] = 0                                          # no faint haze in the empty middle
    comp = Image.fromarray(a, "RGBA")
    comp.save(os.path.join(out, "key_aura_glyph.png"))
    print("key_aura_glyph  %dx%d" % comp.size)


def main():
    for build in (build_gate, build_key, build_mini):
        k, an = build()
        k.save(an)
    aura_textures()


if __name__ == "__main__":
    main()

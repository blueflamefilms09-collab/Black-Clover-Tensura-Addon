"""Shared model + texture kit of the Water, Dark, Lightning and Flame rebuild (1.93.0): temporary-armour pieces and summon bodies.

Every model is a GeoLite model (see docs/geo_models.md in the authoring guide and tools/geo_builder.py): bones, box cubes, a texture and an
emissive `_glow` map of the same size. This kit adds what the four generators (gen_water_aura_geo.py, gen_dark_aura_geo.py,
gen_lightning_aura_geo.py, gen_flame_aura_geo.py and the *_summon_geo.py scripts) share:

  * Piece        a model whose cubes are laid out automatically: every cube gets its own unfolded box net in the texture, at DENSITY texels per model
                 pixel (so a 3 px plate can carry 8 texels of detail; the per-face uv of the Bedrock format makes that legal), the texture is the
                 smallest power of two that holds all nets, and the faces are painted by materials.
  * Mat          a procedural material (plate, scales, flame, bolt, wisp, crystal, feather, cloth, ...) that paints one face at a time and returns the
                 colour image and the emissive image; fully deterministic (seeded by the model id, the cube number and the face).
  * helpers      stepped spikes, fins, rings of things, mirrored copies for the left/right pair.

Frame of the files (same as Blockbench): model pixels = 1/16 block, y UP, origin at the feet, the model's front on -z, +x is the model's left, so
the right arm is at x = -5 and the head is y 24..32. Body-anchored pieces are authored in these WHOLE-BODY coordinates (the aura painter anchors them
on the player's own model part); rotations are degrees.

Run python as `python3 -B`. Files are written under src/main/resources/assets/nusmp/ by geo_builder's ROOT.
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from geo_builder import ROOT, Anim  # noqa: E402

FACES = ("west", "north", "east", "south", "up", "down")        # the box net's faces; west = the model's -x side, north = its front (-z)
BEDROCK = {"west": "east", "north": "north", "east": "west", "south": "south", "up": "up", "down": "down"}    # Bedrock names its sides from the other end of x


def rgb(h):
    h = h.lstrip("#")
    return np.array([int(h[i:i + 2], 16) for i in (0, 2, 4)], dtype=np.float32)


def smooth(x, a, b):
    t = np.clip((x - a) / max(1e-6, b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)


def fbm(rng, w, h, octaves=4, base=3):
    """Value noise in 0..1 of size h x w (tileless)."""
    out = np.zeros((h, w), dtype=np.float32)
    amp, tot = 1.0, 0.0
    for o in range(octaves):
        n = base * (2 ** o)
        g = rng.random((n + 2, n + 2)).astype(np.float32)
        ys = np.linspace(0, n, h, endpoint=False)
        xs = np.linspace(0, n, w, endpoint=False)
        y0 = ys.astype(int)[:, None]
        x0 = xs.astype(int)[None, :]
        fy = (ys - y0[:, 0])[:, None]
        fx = (xs - x0[0, :])[None, :]
        fy = fy * fy * (3 - 2 * fy)
        fx = fx * fx * (3 - 2 * fx)
        a = g[y0, x0] * (1 - fx) + g[y0, x0 + 1] * fx
        b = g[y0 + 1, x0] * (1 - fx) + g[y0 + 1, x0 + 1] * fx
        out += (a * (1 - fy) + b * fy) * amp
        tot += amp
        amp *= 0.5
    return out / tot


class Mat:
    """A procedural material. `kind` picks the pattern, the colours are hex strings.

    base / dark / light : the body colours (dark = shadow side and recesses, light = rim light and highlights)
    glow                : colour of the emissive part (None = the material does not glow); `glow_amount` 0..1 scales how much of the surface glows
    alpha               : the opacity of the colour image (0..255), for crystal / water / smoke
    """

    def __init__(self, kind, base, dark=None, light=None, glow=None, glow_amount=0.5, alpha=255, scale=1.0, seed=0, rim=1.0, grain=0.07, **kw):
        self.kind, self.alpha, self.scale, self.seed, self.rim, self.grain = kind, alpha, scale, seed, rim, grain
        self.base = rgb(base)
        self.dark = rgb(dark) if dark else self.base * 0.55
        self.light = rgb(light) if light else np.minimum(255.0, self.base * 1.35 + 20)
        self.glow = rgb(glow) if glow else None
        self.glow_amount = glow_amount
        self.kw = kw

    def with_(self, **ch):
        m = Mat(self.kind, "000000")
        m.__dict__.update(self.__dict__)
        for k, v in ch.items():
            if k in ("base", "dark", "light", "glow") and isinstance(v, str):
                v = rgb(v)
            m.__dict__[k] = v
        return m

    # ------------------------------------------------------------------ painting of one face
    def paint(self, face, fw, fh, tag):
        rng = np.random.default_rng((sum(ord(c) * (i + 1) for i, c in enumerate(tag + face)) + self.seed * 7919) % (2 ** 32))
        w, h = max(1, fw), max(1, fh)
        yy, xx = np.mgrid[0:h, 0:w].astype(np.float32)
        u = (xx + 0.5) / w
        v = (yy + 0.5) / h
        ex = np.minimum(xx + 0.5, w - xx - 0.5)           # distance (texels) to the nearest vertical / horizontal edge
        ey = np.minimum(yy + 0.5, h - yy - 0.5)
        edge = np.minimum(ex, ey)
        lum, gl, alpha = getattr(self, "_" + self.kind)(u, v, w, h, edge, rng, face)
        lum = lum + (fbm(rng, w, h, 3, 3) - 0.5) * self.grain * 2.0
        if face == "up":
            lum = lum + 0.12
        elif face == "down":
            lum = lum - 0.16
        elif face in ("west", "east"):
            lum = lum - 0.04
        lum = np.clip(lum, 0.0, 1.0)
        col = np.where((lum < 0.5)[..., None], self.dark + (self.base - self.dark) * (lum * 2)[..., None],
                       self.base + (self.light - self.base) * ((lum - 0.5) * 2)[..., None])
        bevel = self.rim * 0.75
        col = np.where((edge < 0.9)[..., None] & (bevel > 0), col * (1 - 0.30 * bevel) + self.dark * 0.3 * bevel, col)
        col = np.where(((edge >= 0.9) & (edge < 1.9))[..., None] & (bevel > 0), col * (1 - 0.10 * bevel) + self.light * 0.10 * bevel, col)
        out = np.zeros((h, w, 4), dtype=np.float32)
        out[..., :3] = col
        out[..., 3] = np.clip(alpha, 0.0, 1.0) * self.alpha
        g = np.zeros((h, w, 4), dtype=np.float32)
        if self.glow is not None:
            gl = np.clip(gl, 0.0, 1.0)
            g[..., :3] = self.glow[None, None, :] * gl[..., None]
            g[..., 3] = np.where(gl > 0.04, 255.0, 0.0)
        return out, g

    # ------------------------------------------------------------------ patterns: return (luminance 0..1, glow 0..1, alpha 0..1)
    def _plain(self, u, v, w, h, edge, rng, face):
        return np.full_like(u, 0.5), np.zeros_like(u), np.ones_like(u)

    def _plate(self, u, v, w, h, edge, rng, face):
        """Hammered plate: a bevelled panel, a groove across, rivets in the corners, an optional glowing inlay line (kw inlay=1)."""
        lum = 0.55 + 0.18 * (1 - v) - 0.10 * (u - 0.5) ** 2
        gl = np.zeros_like(u)
        groove = np.abs(v - 0.5) * h
        lum = np.where(groove < 0.7, lum - 0.22, lum)
        for cx, cy in ((0.14, 0.2), (0.86, 0.2), (0.14, 0.8), (0.86, 0.8)):
            d = np.hypot((u - cx) * w, (v - cy) * h)
            lum = np.where(d < 1.0, lum + 0.28, np.where(d < 1.7, lum - 0.12, lum))
        if self.kw.get("inlay"):
            line = (np.abs(v - 0.5) * h < 0.55) | (np.abs(u - 0.5) * w < 0.55)
            gl = np.where(line & (edge > 1.0), 0.9, gl)
        if self.glow is not None:
            gl = np.maximum(gl, np.where(edge < 0.8, 0.0, 0.0)) + (fbm(rng, w, h, 3, 4) > 0.8) * 0.4 * self.glow_amount
        return lum, gl, np.ones_like(u)

    def _scales(self, u, v, w, h, edge, rng, face):
        """Overlapping round scales (fish / dragon): each scale is a lit arc with a dark seam under it."""
        n = self.kw.get("cells", 4) * self.scale
        cw = max(2.0, w / n)
        ch = max(2.0, cw * 0.8)
        row = np.floor(v * h / ch)
        px = (u * w + (row % 2) * cw * 0.5) % cw
        py = (v * h) % ch
        d = np.hypot((px - cw * 0.5) / (cw * 0.5), (py - ch * 0.0) / (ch * 1.0))
        lum = np.where(d < 1.0, 0.45 + 0.45 * np.clip(1 - d, 0, 1) ** 0.8 + 0.1 * (1 - py / ch), 0.2)
        seam = np.abs(d - 1.0) < (0.5 / cw + 0.04)
        lum = np.where(seam, 0.12, lum)
        gl = np.where(d < 0.35, self.glow_amount * 0.9 * (1 - d / 0.35), 0.0) if self.glow is not None else np.zeros_like(u)
        return lum, gl, np.ones_like(u)

    def _flame(self, u, v, w, h, edge, rng, face):
        """Flame tongues rising: bright core low, red tips high, a ragged alpha edge. v=0 is the top of the face."""
        n = fbm(rng, w, h, 4, 3)
        tongue = 0.5 + 0.5 * np.sin(u * w * 0.9 + n * 5.0)
        height = np.clip(1.0 - v, 0.0, 1.0)
        cover = np.clip(height * 1.5 + tongue * 0.45 - 0.35 + (n - 0.5) * 0.5, 0.0, 1.0)
        alpha = smooth(cover, 0.18, 0.38)
        lum = np.clip(0.30 + 0.75 * (1 - v) * (0.6 + 0.4 * tongue) + (n - 0.5) * 0.3, 0.0, 1.0)
        gl = np.clip(lum * 1.1 - 0.08, 0.0, 1.0) * alpha * self.glow_amount * 1.6 if self.glow is not None else np.zeros_like(u)
        return lum, gl, alpha

    def _bolt(self, u, v, w, h, edge, rng, face):
        """A white-gold enamel plate with a zig-zag bolt inlay that glows."""
        lum = 0.62 + 0.2 * (1 - v) - 0.08 * np.abs(u - 0.5)
        gl = np.zeros_like(u)
        x = rng.uniform(0.25, 0.75)
        pts = [(x, 0.0)]
        k = max(3, int(h / 4))
        for i in range(1, k + 1):
            x = float(np.clip(x + rng.uniform(-0.35, 0.35), 0.1, 0.9))
            pts.append((x, i / k))
        for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
            for t in np.linspace(0, 1, 24):
                cx, cy = (x0 + (x1 - x0) * t) * w, (y0 + (y1 - y0) * t) * h
                d = np.hypot(u * w - cx, v * h - cy)
                gl = np.maximum(gl, np.clip(1.15 - d / max(0.8, 0.07 * w), 0.0, 1.0))
        gl = gl * (edge > 0.6)
        lum = np.where(gl > 0.5, 0.95, lum)
        groove = np.abs(v - 0.5) * h < 0.6
        lum = np.where(groove, lum - 0.18, lum)
        return lum, gl * self.glow_amount if self.glow is not None else np.zeros_like(u), np.ones_like(u)

    def _wisp(self, u, v, w, h, edge, rng, face):
        """Black smoke with violet veins: fbm clouds, a few bright cracks (the Dark Moon violet)."""
        n = fbm(rng, w, h, 4, 3)
        m = fbm(rng, w, h, 3, 5)
        vein = np.abs(n - 0.5) < 0.035
        lum = 0.15 + 0.5 * n * n + 0.2 * m * (1 - v)
        gl = np.where(vein, 1.0, 0.0) * self.glow_amount if self.glow is not None else np.zeros_like(u)
        gl = gl + (np.clip(m - 0.82, 0, 1) * 5.0) * 0.5 * (self.glow is not None)
        alpha = np.ones_like(u)
        if self.kw.get("ragged"):
            alpha = smooth(n + (1 - v) * 0.5, 0.25, 0.45)
        return lum, gl, alpha

    def _crystal(self, u, v, w, h, edge, rng, face):
        """Voronoi facets, each lit differently, with bright seams: ice-like water crystal."""
        k = max(3, int(self.kw.get("cells", 5) * self.scale))
        pts = rng.random((k, 2)).astype(np.float32) * [w, h]
        px, py = u * w, v * h
        d = np.stack([np.hypot(px - p[0], py - p[1]) for p in pts])
        i = np.argmin(d, axis=0)
        s = np.sort(d, axis=0)
        seam = (s[1] - s[0]) < 0.9
        cell = np.array([0.35 + 0.55 * rng.random() for _ in range(k)], dtype=np.float32)[i]
        lum = cell * (0.8 + 0.2 * (1 - v))
        if self.kw.get("seamonly"):
            lum = 0.12 + 0.28 * cell * (0.8 + 0.2 * (1 - v))
        lum = np.where(seam, 0.97, lum)
        gl = np.where(seam, 0.85, 0.0) * self.glow_amount if self.glow is not None else np.zeros_like(u)
        gl = gl + (cell > 0.8) * 0.25 * self.glow_amount * (self.glow is not None)
        return lum, gl, np.ones_like(u)

    def _feather(self, u, v, w, h, edge, rng, face):
        """A feather / fin blade: a central rib, diagonal barbs, a tapering ragged tip toward v=1 (kw tip=1 makes the tip pointed)."""
        rib = np.abs(u - 0.5) * w < 0.7
        barb = 0.5 + 0.5 * np.sin((v * h + np.abs(u - 0.5) * w * 1.3) * 1.7)
        lum = 0.35 + 0.45 * barb + 0.15 * (1 - v)
        lum = np.where(rib, 0.95, lum)
        taper = 1.0 - (v ** 1.7) * 0.9 if self.kw.get("tip", 1) else np.ones_like(u)
        alpha = (np.abs(u - 0.5) * 2 < taper).astype(np.float32)
        gl = (rib * 0.9 + (barb > 0.9) * 0.35) * self.glow_amount * alpha if self.glow is not None else np.zeros_like(u)
        return lum, gl, np.maximum(alpha, 0.0)

    def _cloth(self, u, v, w, h, edge, rng, face):
        """Woven cloth with folds; kw ragged=1 gives a torn hem along v=1; kw trim=1 adds a glowing hem line."""
        fold = 0.5 + 0.5 * np.sin(u * w * 0.9 + fbm(rng, w, h, 2, 3) * 3.0)
        weave = ((np.floor(u * w) + np.floor(v * h)) % 2) * 0.05
        lum = 0.30 + 0.35 * fold + weave - 0.12 * v
        alpha = np.ones_like(u)
        if self.kw.get("ragged"):
            n = fbm(rng, w, h, 3, 4)
            alpha = smooth(1.25 - v - n * 0.55 + 0.35, 0.28, 0.42)
        gl = np.zeros_like(u)
        if self.kw.get("trim") and self.glow is not None:
            gl = np.where(np.abs(v - 0.9) * h < 0.8, 0.9, 0.0) * alpha * self.glow_amount
        return lum, gl, alpha

    def _water(self, u, v, w, h, edge, rng, face):
        """Flowing water: long wavy bands, a bright caustic net, translucent (use alpha)."""
        n = fbm(rng, w, h, 3, 3)
        bands = 0.5 + 0.5 * np.sin(v * h * 0.8 + n * 6.0 + u * w * 0.15)
        caustic = np.abs(np.sin(u * w * 0.7 + n * 5) * np.sin(v * h * 0.6 + n * 4)) < 0.10
        lum = 0.35 + 0.35 * bands + 0.2 * (1 - v)
        lum = np.where(caustic, 0.95, lum)
        gl = (caustic * 0.8 + (bands > 0.93) * 0.3) * self.glow_amount if self.glow is not None else np.zeros_like(u)
        return lum, gl, np.ones_like(u)

    def _mane(self, u, v, w, h, edge, rng, face):
        """Long strands (hair / mane / fur) hanging down v: vertical streaks whose colour runs light at the root to dark at the tip."""
        s = 0.5 + 0.5 * np.sin(u * w * 2.1 + fbm(rng, w, h, 2, 3) * 2.0)
        lum = 0.25 + 0.55 * s * (1 - v * 0.6) + 0.2 * (1 - v)
        alpha = smooth(1.15 - v - (fbm(rng, w, h, 3, 5) - 0.5) * 0.5, 0.02, 0.2) if self.kw.get("ragged", 1) else np.ones_like(u)
        gl = np.clip(lum - 0.35, 0, 1) * 1.2 * alpha * self.glow_amount if self.glow is not None else np.zeros_like(u)
        return lum, gl, alpha

    def _metal(self, u, v, w, h, edge, rng, face):
        """Brushed metal with a lit edge and a thin glowing seam (kw seam=1)."""
        streak = fbm(rng, w, 1, 3, 5)[0][None, :] if w > 1 else np.zeros((1, 1), dtype=np.float32)
        lum = 0.45 + 0.3 * (1 - v) + (streak - 0.5) * 0.25
        lum = np.broadcast_to(lum, u.shape).copy()
        gl = np.zeros_like(u)
        if self.kw.get("seam") and self.glow is not None:
            gl = np.where(np.abs(v - 0.55) * h < 0.65, 0.95, 0.0) * self.glow_amount
        return lum, gl, np.ones_like(u)


# ====================================================================================================================== pieces
def _ceil(x, d):
    return max(1, int(math.ceil(x * d - 1e-6)))


class Piece:
    """One model: bones with cubes; save() lays the cube nets out, paints them with the cubes' materials, writes geo + texture + glow (+ animation)."""

    def __init__(self, key, name, density=2, glow_lift=1.0):
        self.key, self.name, self.density, self.glow_lift = key, name, density, glow_lift
        self.bones, self.order, self.cubes = {}, [], []
        self.bone("root", (0, 0, 0))

    @property
    def id(self):
        return f"{self.key}_{self.name}"

    def bone(self, name, pivot=(0, 0, 0), parent=None, rotation=None):
        b = {"name": name, "pivot": list(pivot), "cubes": []}
        if parent:
            b["parent"] = parent
        elif name != "root":
            b["parent"] = "root"
        if rotation:
            b["rotation"] = list(rotation)
        self.bones[name] = b
        self.order.append(name)
        return b

    def box(self, bone, origin, size, mat, inflate=0.0, rot=None, pivot=None, d=None):
        """origin = the cube's min corner in model pixels (y up); size = (x, y, z) extents; rot degrees about `pivot` (default: the bone's pivot)."""
        c = {"origin": [float(v) for v in origin], "size": [float(v) for v in size]}
        if inflate:
            c["inflate"] = inflate
        if rot:
            c["rotation"] = [float(v) for v in rot]
            c["pivot"] = [float(v) for v in (pivot if pivot is not None else self.bones[bone]["pivot"])]
        c["_mat"] = mat
        c["_d"] = d or self.density
        self.bones[bone]["cubes"].append(c)
        self.cubes.append(c)
        return c

    def cube_count(self):
        return len(self.cubes)

    # ------------------------------------------------------------------ layout and painting
    def _net(self, c):
        sx, sy, sz = c["size"]
        D = c["_d"]
        w, h, d = _ceil(sx, D), _ceil(sy, D), _ceil(sz, D)
        return w, h, d

    def _layout(self):
        nets = []
        for i, c in enumerate(self.cubes):
            w, h, d = self._net(c)
            nets.append((i, 2 * (w + d), h + d, w, h, d))
        for size in (32, 64, 128, 256, 512, 1024):
            x = y = row = 0
            pos, ok = {}, True
            for i, nw, nh, w, h, d in sorted(nets, key=lambda n: -n[2]):
                if nw > size:
                    ok = False
                    break
                if x + nw > size:
                    x, y, row = 0, y + row, 0
                if y + nh > size:
                    ok = False
                    break
                pos[i] = (x, y)
                x += nw
                row = max(row, nh)
            if ok:
                return size, pos
        raise ValueError(self.id + ": the cubes do not fit in 1024 px")

    def save(self, anim=None):
        size, pos = self._layout()
        img = np.zeros((size, size, 4), dtype=np.float32)
        glow = np.zeros((size, size, 4), dtype=np.float32)
        for i, c in enumerate(self.cubes):
            w, h, d = self._net(c)
            x0, y0 = pos[i]
            rects = {"up": (x0 + d, y0, w, d), "down": (x0 + d + w, y0, w, d),
                     "west": (x0, y0 + d, d, h), "north": (x0 + d, y0 + d, w, h), "east": (x0 + d + w, y0 + d, d, h), "south": (x0 + 2 * d + w, y0 + d, w, h)}
            uv = {}
            for f, (fx, fy, fw, fh) in rects.items():
                a, g = c["_mat"].paint(f, fw, fh, "%s/%d" % (self.id, i))
                if f == "down":
                    a, g = a[::-1], g[::-1]                           # the loader stores the bottom flipped
                img[fy:fy + fh, fx:fx + fw] = a
                glow[fy:fy + fh, fx:fx + fw] = g
                uv[BEDROCK[f]] = {"uv": [fx, fy], "uv_size": [fw, fh]}
            c["uv"] = uv
        bones = []
        for n in self.order:
            b = dict(self.bones[n])
            b["cubes"] = [{k: v for k, v in c.items() if not k.startswith("_")} for c in b["cubes"]]
            bones.append(b)
        geo = {"format_version": "1.12.0", "minecraft:geometry": [{
            "description": {"identifier": "geometry." + self.id, "texture_width": size, "texture_height": size,
                            "visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 1, 0]}, "bones": bones}]}
        os.makedirs(os.path.join(ROOT, "geo", "entity"), exist_ok=True)
        os.makedirs(os.path.join(ROOT, "textures", "entity"), exist_ok=True)
        with open(os.path.join(ROOT, "geo", "entity", self.id + ".geo.json"), "w") as f:
            json.dump(geo, f, indent=1)
        Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ROOT, "textures", "entity", self.id + ".png"))
        Image.fromarray(np.clip(glow * self.glow_lift, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ROOT, "textures", "entity", self.id + "_glow.png"))
        if anim is not None:
            os.makedirs(os.path.join(ROOT, "animations", "entity"), exist_ok=True)
            with open(os.path.join(ROOT, "animations", "entity", self.id + ".animation.json"), "w") as f:
                json.dump(anim.data(), f, indent=1)
        return size


def shell_texture(name, mat, size=64):
    """A 64 x 64 player-skin-layout texture (+ its _glow) for the body overlays of an aura (AuraRender.model draws the posed player model with it): the material is
    painted over the whole sheet, so any part of the model shows it. Written to textures/aura/<name>.png."""
    a, g = mat.paint("north", size, size, "shell/" + name)
    os.makedirs(os.path.join(ROOT, "textures", "aura"), exist_ok=True)
    Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ROOT, "textures", "aura", name + ".png"))
    Image.fromarray(np.clip(g, 0, 255).astype(np.uint8), "RGBA").save(os.path.join(ROOT, "textures", "aura", name + "_glow.png"))


# ====================================================================================================================== helpers
def stepped_spike(p, bone, base, direction, length, width, mat, steps=4, thick=None, inflate=0.0):
    """A spike of `steps` shrinking cubes standing on `base` (x, y, z, the centre of its foot), pointing along an axis direction ('+y', '-y', '+x', '-x', '+z', '-z').
    Axis aligned (no cube rotation), so it can never be drawn wrong; `width` is the foot width, `thick` the foot depth (default = width)."""
    thick = thick or width
    ax = {"+x": (1, 0, 0), "-x": (-1, 0, 0), "+y": (0, 1, 0), "-y": (0, -1, 0), "+z": (0, 0, 1), "-z": (0, 0, -1)}[direction]
    bx, by, bz = base
    seg = length / steps
    for i in range(steps):
        k = 1.0 - i / float(steps)                       # 1 at the foot, ~0 at the tip
        kk = max(0.18, k * 0.95)
        w, t = width * kk, thick * kk
        if ax[0]:
            o = (bx + (seg * i if ax[0] > 0 else -seg * (i + 1)), by - w / 2, bz - t / 2)
            sz = (seg, w, t)
        elif ax[1]:
            o = (bx - w / 2, by + (seg * i if ax[1] > 0 else -seg * (i + 1)), bz - t / 2)
            sz = (w, seg, t)
        else:
            o = (bx - w / 2, by - t / 2, bz + (seg * i if ax[2] > 0 else -seg * (i + 1)))
            sz = (w, t, seg)
        p.box(bone, o, sz, mat, inflate=inflate)


def mirror_x(v):
    return (-v[0], v[1], v[2])


def mbox(p, bone, side, origin, size, mat, **kw):
    """box() for a pair: `origin` is given for the RIGHT side (x < 0); side=+1 mirrors it to the left (x > 0). Rotations about y / z flip sign too."""
    ox, oy, oz = origin
    if side > 0:
        ox = -(ox + size[0])
        if kw.get("rot"):
            r = kw["rot"]
            kw["rot"] = (r[0], -r[1], -r[2])
        if kw.get("pivot") is not None:
            kw["pivot"] = (-kw["pivot"][0], kw["pivot"][1], kw["pivot"][2])
    return p.box(bone, (ox, oy, oz), size, mat, **kw)


def mpivot(side, pv):
    return (-pv[0], pv[1], pv[2]) if side > 0 else tuple(pv)


def loop_rot(anim, clip, bone, length, frames):
    """frames: {fraction 0..1 of the loop: (rx, ry, rz)}; the last fraction 1.0 must equal 0.0 (it is added when missing)."""
    fr = {t * length: v for t, v in frames.items()}
    if 1.0 not in frames:
        fr[length] = frames[0.0]
    anim.rot(clip, bone, fr)


def sine_rot(anim, clip, bone, length, axis, amp, phase=0.0, steps=8, offset=(0, 0, 0)):
    """A closed sine wave of rotation about one axis (0, 1, 2) with amplitude amp degrees; phase in loops (0..1)."""
    frames = {}
    for i in range(steps):
        t = i / steps
        a = amp * math.sin((t + phase) * math.tau)
        v = list(offset)
        v[axis] += a
        frames[t] = tuple(v)
    loop_rot(anim, clip, bone, length, frames)


def spin_y(anim, clip, bone, length, turns=1, steps=8):
    """A full turn about y per loop (closed: the last key is 360 * turns)."""
    anim.rot(clip, bone, {length * i / steps: (0, 360.0 * turns * i / steps, 0) for i in range(steps + 1)})


def feather_wing(p, a, side, P, fan, mat, arm_mat=None, length=1.2, amp=10.0, yaw=20.0, thick=0.5, clip="idle", arm_len=9.0, arm_tilt=36.0):
    """A wing: a root bone at P (x is mirrored for side=1), one feather per entry of `fan` = [(roll degrees, length, width), ...] fanned from up to down,
    each a flat cube along the outward axis; the root beats about z (`amp` degrees, loop `length` s) with a sweep about y, every feather trails it.
    Optionally a bone-arm cube along the leading edge. Rotation signs are mirrored for the left wing."""
    sg = 1.0 if side else -1.0
    P = (sg * abs(P[0]), P[1], P[2])
    p.bone("w", P)
    sine_rot(a, clip, "w", length, 2, -amp * sg, 0.0)
    sine_rot(a, clip, "w", length, 1, amp * 0.6 * sg, 0.15)
    for i, (rz, L, w) in enumerate(fan):
        b = "f%d" % i
        p.bone(b, P, parent="w", rotation=(0, sg * (yaw + i * 3), -sg * rz))
        o = (P[0] - L, P[1] - w / 2, P[2]) if side == 0 else (P[0], P[1] - w / 2, P[2])
        p.box(b, o, (L, w, thick), mat)
        sine_rot(a, clip, b, length, 2, amp * 0.5 * sg, 0.22 + i * 0.05)
    if arm_mat is not None:
        p.box("w", (P[0] - (arm_len if side == 0 else 0.0), P[1] - 0.6, P[2] - 0.2), (arm_len, 1.2, 1.0), arm_mat, rot=(0, 0, sg * arm_tilt), pivot=P)


def tail_chain(p, a, mats, n, seg_len, start, w0, taper=0.3, droop=1.5, tilt=14.0, wave=14.0, length=1.8, clip="idle", prefix="t", x=0.0, phase0=0.0, parent="root"):
    """A chain of n bones hanging off the back: cube segments of seg_len along +z, each bone waves about y with a phase lag. Returns the end (y, z) and the last bone."""
    y, z = start
    for i in range(n):
        b = "%s%d" % (prefix, i)
        p.bone(b, (x, y, z), parent=parent)
        w = max(0.8, w0 - i * taper)
        p.box(b, (x - w / 2, y - w / 2, z), (w, w, seg_len + 0.2), mats[i % len(mats)], rot=(tilt, 0, 0), pivot=(x, y, z))
        sine_rot(a, clip, b, length, 1, wave, phase=phase0 - i * 0.11)
        parent = b
        z += seg_len - 0.2
        y -= droop
    return y, z, parent


def bird(p, a, body, wing, tail_mat, beak, eye, crest, wing_len=22.0, tail_len=26.0, cy=22.0, flap=0.8, feathers=6, crest_n=3):
    """A bird for the PROP space (front on -z, centred about height cy): body, neck, head with a beak and a crest, wings of `feathers` flat feathers each that flap about the
    body's long axis, a fan of tail feathers and two talons. Clips: 'fly' (flapping, `flap` s per beat) and 'idle' (a slow glide). `wing` / `tail_mat` are the feather materials."""
    a.clip("fly", length=flap, loop=True)
    a.clip("idle", length=flap * 3.0, loop=True)
    p.box("root", (-3.2, cy - 3.2, -5.0), (6.4, 6.4, 13.0), body)                       # the body
    p.box("root", (-2.0, cy - 2.0, 8.0), (4.0, 3.6, 4.0), body)                         # the rump
    p.bone("head", (0, cy + 2.5, -5.0))
    p.box("head", (-1.8, cy - 0.5, -8.0), (3.6, 5.0, 3.6), body)                       # the neck
    p.box("head", (-2.2, cy + 3.8, -11.0), (4.4, 4.2, 5.6), body)                      # the head
    p.box("head", (-0.9, cy + 4.6, -14.6), (1.8, 1.6, 3.8), beak)                      # the beak
    p.box("head", (-0.6, cy + 3.8, -14.0), (1.2, 0.9, 2.8), beak)
    for sx in (0, 1):
        mbox(p, "head", sx, (-2.45, cy + 6.0, -10.6), (0.6, 1.1, 1.4), eye)           # the eyes
    for k in range(crest_n):                                                           # the crest, swept back
        p.box("head", (-0.5, cy + 8.0, -9.6 + k * 1.7), (1.0, 3.6 - k * 0.5, 1.2), crest, rot=(24 + k * 10, 0, 0), pivot=(0, cy + 8.0, -9.6 + k * 1.7))
    sine_rot(a, "fly", "head", flap, 0, 4.0, phase=0.25)
    sine_rot(a, "idle", "head", flap * 3.0, 0, 3.0)
    for sx in (0, 1):
        sg = 1.0 if sx else -1.0
        P = (sg * 3.0, cy + 1.2, -1.0)
        w = "wing%d" % sx
        p.bone(w, P)
        # the arm of the wing, then the feathers fanned from the arm: front ones short, the long ones trailing
        p.box(w, (P[0] - (10.0 if sx == 0 else 0.0), P[1] - 0.7, P[2] - 1.0), (10.0, 1.4, 2.4), body)
        for i in range(feathers):
            f = "w%df%d" % (sx, i)
            L = wing_len * (0.55 + 0.45 * math.sin((i + 1) / float(feathers + 0.5) * math.pi * 0.9))
            p.bone(f, P, parent=w, rotation=(0, sg * (-8 - 4 * i), 0))
            z0 = P[2] - 1.0 + i * 2.3
            o = (P[0] - L, P[1] - 0.3, z0) if sx == 0 else (P[0], P[1] - 0.3, z0)
            p.box(f, o, (L, 0.6, 3.4), wing)
            sine_rot(a, "fly", f, flap, 2, sg * 8.0, phase=-0.12 - i * 0.025)
        a.rot("fly", w, {0.0: (0, 0, sg * 38), flap / 2: (0, 0, -sg * 34), flap: (0, 0, sg * 38)})
        sine_rot(a, "idle", w, flap * 3.0, 2, sg * 6.0)
    p.bone("tail", (0, cy, 11.0))
    for i in range(5):
        ang = (i - 2) * 11.0
        L = tail_len * (1.0 - 0.14 * abs(i - 2))
        p.box("tail", (-0.9, cy - 0.3, 11.0), (1.8, 0.6, L), tail_mat, rot=(0, ang, 0), pivot=(0, cy, 11.0))
    sine_rot(a, "fly", "tail", flap, 0, 5.0, phase=0.3)
    sine_rot(a, "idle", "tail", flap * 3.0, 1, 5.0)
    for sx in (0, 1):
        mbox(p, "root", sx, (-2.4, cy - 7.0, -1.0), (1.0, 4.2, 1.0), beak)             # the talons
        mbox(p, "root", sx, (-3.0, cy - 7.4, -2.6), (2.2, 0.9, 2.4), beak)

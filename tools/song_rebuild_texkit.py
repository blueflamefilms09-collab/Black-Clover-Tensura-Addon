"""Drawing helpers of the particle texture generators of the 1.93.0 rebuild (Song, Sound, Soul, Soul Corpse, Memory).

    from song_rebuild_texkit import Canvas, save_particle
    c = Canvas(128)                     # a square canvas, drawn at 4x and downsampled; coordinates are 0..1 (x right, y down)
    c.ring(0.5, 0.5, 0.4, 0.04, 255)    # a ring: centre, radius, half width, alpha 0..255
    c.line([(0.1, 0.5), (0.9, 0.5)], 0.02, 255)
    c.glow(0.5, 0.5, 0.3, 200)          # a soft radial glow (adds)
    save_particle("song_demo", c.finish(blur=0.8))

Why: the VFX layers of the five magics draw white-and-alpha sprites (the vertex colour tints them), as tools/gen_time_vfx_textures.py does. Everything is
deterministic (seeded) so a texture can be regenerated. Files go to src/main/resources/assets/nusmp/textures/particle/. Run python as `python3 -B`.
"""
import math
import os
import random
import sys

sys.dont_write_bytecode = True
import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures", "particle")
SS = 4


class Canvas:
    """An alpha canvas (white where drawn) with a supersampled pen. w x h pixels at the end; coordinates are fractions (x of w, y of h)."""

    def __init__(self, w, h=None, seed=1):
        self.w, self.h = w, h or w
        self.img = Image.new("L", (self.w * SS, self.h * SS), 0)
        self.d = ImageDraw.Draw(self.img)
        self.acc = np.zeros((self.h, self.w), dtype=np.float32)
        self.rng = random.Random(seed)

    def _p(self, x, y):
        return (x * self.w * SS, y * self.h * SS)

    def _r(self, r):
        return r * self.w * SS

    def ring(self, cx, cy, r, half, a=255):
        x, y = self._p(cx, cy)
        R, H = self._r(r), self._r(half)
        self.d.ellipse([x - R - H, y - R - H, x + R + H, y + R + H], outline=a, width=max(1, int(H * 2)))

    def disc(self, cx, cy, r, a=255):
        x, y = self._p(cx, cy)
        R = self._r(r)
        self.d.ellipse([x - R, y - R, x + R, y + R], fill=a)

    def ellipse(self, cx, cy, rx, ry, a=255, rot=0.0):
        pts = []
        for k in range(48):
            t = k / 48.0 * 2 * math.pi
            px, py = rx * math.cos(t), ry * math.sin(t)
            c, s = math.cos(rot), math.sin(rot)
            pts.append(self._p(cx + px * c - py * s, cy + px * s + py * c))
        self.d.polygon(pts, fill=a)

    def line(self, pts, width, a=255, round_ends=True):
        P = [self._p(x, y) for x, y in pts]
        W = max(1, int(self._r(width) * 2))
        self.d.line(P, fill=a, width=W, joint="curve")
        if round_ends:
            for (x, y) in P[:: max(1, len(P) - 1)]:
                self.d.ellipse([x - W / 2, y - W / 2, x + W / 2, y + W / 2], fill=a)

    def poly(self, pts, a=255):
        self.d.polygon([self._p(x, y) for x, y in pts], fill=a)

    def arc(self, cx, cy, r, half, a0, a1, a=255):
        """An arc from angle a0 to a1 (radians, 0 = +x, clockwise on screen)."""
        n = max(6, int(abs(a1 - a0) * 24))
        pts = [(cx + r * math.cos(a0 + (a1 - a0) * k / n), cy + r * math.sin(a0 + (a1 - a0) * k / n)) for k in range(n + 1)]
        self.line(pts, half, a)

    def glow(self, cx, cy, r, strength=255, power=2.0):
        """A soft radial glow added on top (not blurred): strength 0..255 at the centre falling to 0 at r."""
        yy, xx = np.mgrid[0:self.h, 0:self.w]
        d = np.hypot((xx + 0.5) / self.w - cx, (yy + 0.5) / self.h - cy) / max(1e-6, r)
        self.acc += strength * np.clip(1 - d, 0, 1) ** power

    def star(self, cx, cy, r, points=4, inner=0.12, a=255, rot=0.0):
        pts = []
        for k in range(points * 2):
            rr = r if k % 2 == 0 else r * inner
            t = rot + k * math.pi / points
            pts.append((cx + rr * math.cos(t), cy + rr * math.sin(t)))
        self.poly(pts, a)

    def noise_mask(self, amount=0.2):
        arr = np.asarray(self.img, dtype=np.float32)
        rng = np.random.RandomState(self.rng.randint(0, 1 << 30))
        arr *= 1 - amount * rng.rand(*arr.shape)
        self.img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "L")

    def finish(self, blur=0.6, halo=0.0, halo_r=2.5):
        """The final RGBA (white, alpha): downsampled, a little blur, and optionally a soft halo of the drawing added around it."""
        small = self.img.resize((self.w, self.h), Image.LANCZOS)
        if blur > 0:
            small = small.filter(ImageFilter.GaussianBlur(blur))
        arr = np.asarray(small, dtype=np.float32)
        if halo > 0:
            hb = np.asarray(small.filter(ImageFilter.GaussianBlur(halo_r)), dtype=np.float32)
            arr = np.maximum(arr, hb * halo)
        arr = np.clip(arr + self.acc, 0, 255)
        out = np.zeros((self.h, self.w, 4), dtype=np.uint8)
        out[..., :3] = 255
        out[..., 3] = arr.astype(np.uint8)
        return Image.fromarray(out, "RGBA")


def save_particle(name, img):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name + ".png"))
    print("%-24s %dx%d" % (name, img.width, img.height))


def sheet(names, path, cell=128):
    """A contact sheet of generated textures on a dark background, for looking at them (written outside the repo)."""
    imgs = [Image.open(os.path.join(OUT, n + ".png")).convert("RGBA") for n in names]
    cols = 4
    rows = (len(imgs) + cols - 1) // cols
    bg = Image.new("RGBA", (cols * cell, rows * cell), (24, 20, 40, 255))
    for k, im in enumerate(imgs):
        s = min(cell / im.width, cell / im.height)
        im = im.resize((max(1, int(im.width * s)), max(1, int(im.height * s))), Image.LANCZOS)
        tile = Image.new("RGBA", im.size, (200, 140, 220, 255))
        tile.alpha_composite(Image.new("RGBA", im.size, (0, 0, 0, 0)))
        bg.alpha_composite(im.copy().convert("RGBA"), ((k % cols) * cell, (k // cols) * cell))
    bg.save(path)

"""Great Spirit GUI mock-up kit: a software re-implementation of the few GuiGraphics calls the redesign would use (fill, gradient, textured quad
with linear filtering, additive quad, scissor, the vanilla bitmap font). NOT the game. Coordinates are GUI pixels (floats); a Canvas draws them at
`scale` real pixels per GUI pixel, like Minecraft's GUI scale setting. The text uses the real vanilla ascii.png atlas (fetched into build/, never
added to the repository) and the vanilla advance rule (last opaque column + 2, space 4)."""
import math
import os
import urllib.request

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
FONT_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/font/ascii.png"
SERIF_ITALIC = "/usr/share/fonts/truetype/freefont/FreeSerifItalic.ttf"
SERIF = "/usr/share/fonts/truetype/freefont/FreeSerif.ttf"
SERIF_BOLD = "/usr/share/fonts/truetype/freefont/FreeSerifBold.ttf"
MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"


def lerp(a, b, t):
    return a + (b - a) * t


def clamp01(x):
    return max(0.0, min(1.0, x))


def smooth(t):
    t = clamp01(t)
    return t * t * (3 - 2 * t)


def mixc(a, b, t):
    return tuple(int(round(lerp(a[i], b[i], t))) for i in range(len(a)))


def with_a(c, a):
    return (c[0], c[1], c[2], int(round(255 * clamp01(a))))


# ------------------------------------------------------------------------------------------------------ vanilla font
class McFont:
    """The vanilla default font (ascii.png, 8x8 cells). width(s) in GUI pixels, glyph masks cached."""

    def __init__(self, path):
        self.atlas = np.asarray(Image.open(path).convert("RGBA"))[:, :, 3] > 0
        self.adv = {}
        self.mask = {}
        for code in range(32, 127):
            cx, cy = (code % 16) * 8, (code // 16) * 8
            cell = self.atlas[cy:cy + 8, cx:cx + 8]
            cols = np.where(cell.any(axis=0))[0]
            self.adv[code] = 4 if code == 32 else (int(cols.max()) + 2 if len(cols) else 4)
            self.mask[code] = cell.copy()

    def width(self, s, bold=False):
        return sum(self.adv.get(ord(c) if 32 <= ord(c) < 127 else 63, 4) + (1 if bold else 0) for c in s) - (1 if s else 0)

    def layer(self, s, scale, rgba, shadow=True, bold=False):
        """An RGBA numpy layer of the string at `scale` real px per GUI px (nearest), shadow at +1 GUI px in color/4 like vanilla."""
        w = self.width(s, bold) + 2
        m = np.zeros((10, max(1, w)), bool)
        x = 0
        for c in s:
            code = ord(c) if 32 <= ord(c) < 127 else 63
            g = self.mask[code]
            m[0:8, x:x + 8] |= g[:, :min(8, m.shape[1] - x)]
            if bold:
                m[0:8, x + 1:x + 9] |= g[:, :max(0, min(8, m.shape[1] - x - 1))]
            x += self.adv.get(code, 4) + (1 if bold else 0)
        big = np.kron(m, np.ones((scale, scale), bool))
        out = np.zeros(big.shape + (4,), np.uint8)
        r, g_, b, a = rgba
        if shadow:
            sh = np.zeros_like(big)
            sh[scale:, scale:] = big[:-scale, :-scale]
            out[sh] = (r // 4, g_ // 4, b // 4, a)
        out[big] = (r, g_, b, a)
        return Image.fromarray(out, "RGBA")


_FONT = None


def font(path=None):
    global _FONT
    if _FONT is None:
        path = path or os.path.join(ROOT, "build", "gui_redesign", "ascii.png")
        if not os.path.exists(path):
            os.makedirs(os.path.dirname(path), exist_ok=True)
            urllib.request.urlretrieve(FONT_URL, path)
        _FONT = McFont(path)
    return _FONT


# ------------------------------------------------------------------------------------------------------ canvas
class Canvas:
    def __init__(self, gw, gh, scale, bg=None):
        self.gw, self.gh, self.s = gw, gh, scale
        self.img = Image.new("RGBA", (int(round(gw * scale)), int(round(gh * scale))), (0, 0, 0, 255))
        if bg is not None:
            self.img.alpha_composite(bg.resize(self.img.size, Image.BILINEAR))
        self.clip = None
        self.f = font()

    # ---- helpers
    def px(self, v):
        return int(round(v * self.s))

    def _put(self, layer, x, y, add=False, alpha=1.0):
        """Composite an RGBA PIL layer with its top-left at real pixel (x, y), honouring the scissor."""
        if alpha < 1.0:
            a = layer.getchannel("A").point(lambda v: int(v * alpha))
            layer = layer.copy()
            layer.putalpha(a)
        if self.clip:
            cx0, cy0, cx1, cy1 = self.clip
            nx0, ny0 = max(x, cx0), max(y, cy0)
            nx1, ny1 = min(x + layer.width, cx1), min(y + layer.height, cy1)
            if nx1 <= nx0 or ny1 <= ny0:
                return
            layer = layer.crop((nx0 - x, ny0 - y, nx1 - x, ny1 - y))
            x, y = nx0, ny0
        nx0, ny0 = max(x, 0), max(y, 0)
        nx1, ny1 = min(x + layer.width, self.img.width), min(y + layer.height, self.img.height)
        if nx1 <= nx0 or ny1 <= ny0:
            return
        if (nx0, ny0, nx1, ny1) != (x, y, x + layer.width, y + layer.height):
            layer = layer.crop((nx0 - x, ny0 - y, nx1 - x, ny1 - y))
            x, y = nx0, ny0
        if not add:
            self.img.alpha_composite(layer, (x, y))
        else:
            region = np.asarray(self.img.crop((x, y, x + layer.width, y + layer.height))).astype(np.float32)
            l = np.asarray(layer).astype(np.float32)
            region[..., :3] = np.minimum(255.0, region[..., :3] + l[..., :3] * (l[..., 3:4] / 255.0))
            self.img.paste(Image.fromarray(region.astype(np.uint8), "RGBA"), (x, y))

    def scissor(self, x0, y0, x1, y1):
        self.clip = (self.px(x0), self.px(y0), self.px(x1), self.px(y1))

    def no_scissor(self):
        self.clip = None

    # ---- primitives
    def fill(self, x0, y0, x1, y1, rgba, add=False):
        w, h = self.px(x1) - self.px(x0), self.px(y1) - self.px(y0)
        if w <= 0 or h <= 0 or rgba[3] == 0:
            return
        self._put(Image.new("RGBA", (w, h), tuple(int(v) for v in rgba)), self.px(x0), self.px(y0), add)

    def gradient(self, x0, y0, x1, y1, top, bottom, add=False):
        w, h = self.px(x1) - self.px(x0), self.px(y1) - self.px(y0)
        if w <= 0 or h <= 0:
            return
        t = np.linspace(0, 1, h)[:, None, None]
        a = np.array(top, np.float32)[None, None, :] * (1 - t) + np.array(bottom, np.float32)[None, None, :] * t
        arr = np.repeat(a, w, axis=1).astype(np.uint8)
        self._put(Image.fromarray(arr, "RGBA"), self.px(x0), self.px(y0), add)

    def hline(self, x0, x1, y, rgba, th=1.0, add=False):
        self.fill(x0, y, x1, y + th, rgba, add)

    def vline(self, x, y0, y1, rgba, th=1.0, add=False):
        self.fill(x, y0, x + th, y1, rgba, add)

    def outline(self, x, y, w, h, rgba, th=1.0, add=False):
        self.fill(x, y, x + w, y + th, rgba, add)
        self.fill(x, y + h - th, x + w, y + h, rgba, add)
        self.fill(x, y + th, x + th, y + h - th, rgba, add)
        self.fill(x + w - th, y + th, x + w, y + h - th, rgba, add)

    def blit(self, tex, x, y, w=None, h=None, tint=None, alpha=1.0, add=False, flip_x=False, flip_y=False, rot=0.0, nearest=False):
        """A textured quad: `tex` is a PIL RGBA image (a texture at its shipped resolution), drawn into the GUI rect (x, y, w, h), bilinear unless
        `nearest`; `tint` multiplies RGB (vertex colour); `rot` degrees clockwise about the rect centre (a rotated quad)."""
        if w is None:
            w = tex.width
        if h is None:
            h = tex.height
        tw, th = max(1, self.px(w)), max(1, self.px(h))
        im = tex.resize((tw, th), Image.NEAREST if nearest else Image.BILINEAR)
        if flip_x:
            im = ImageOps_mirror(im)
        if flip_y:
            im = im.transpose(Image.FLIP_TOP_BOTTOM)
        if tint is not None:
            im = tinted(im, tint)
        if rot:
            im = im.rotate(-rot, resample=Image.BICUBIC, expand=True)
            cx, cy = self.px(x) + tw / 2.0, self.px(y) + th / 2.0
            self._put(im, int(round(cx - im.width / 2.0)), int(round(cy - im.height / 2.0)), add, alpha)
        else:
            self._put(im, self.px(x), self.px(y), add, alpha)

    def layer(self, im, x, y, add=False, alpha=1.0):
        """Composite an already real-pixel-sized layer at GUI position."""
        self._put(im, self.px(x), self.px(y), add, alpha)

    def vector(self, bbox, fn, ss=3, add=False, blur=0.0, alpha=1.0, gain=1.0):
        """Draw vector shapes (anti-aliased by supersampling) into the GUI rect `bbox` = (x0, y0, x1, y1): fn(draw, k, ox, oy) draws on an RGBA layer
        where a GUI point (gx, gy) is at ((gx - ox) * k, (gy - oy) * k); `blur` (GUI px) softens it into a bloom; `add` composites additively."""
        x0, y0, x1, y1 = bbox
        pad = blur * 3
        x0, y0, x1, y1 = x0 - pad, y0 - pad, x1 + pad, y1 + pad
        w, h = max(1, self.px(x1) - self.px(x0)), max(1, self.px(y1) - self.px(y0))
        k = self.s * ss
        big = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
        fn(ImageDraw.Draw(big), k, self.px(x0) / self.s, self.px(y0) / self.s)
        lay = big.resize((w, h), Image.LANCZOS)
        if blur:
            lay = lay.filter(ImageFilter.GaussianBlur(blur * self.s))
        if gain != 1.0:
            arr = np.asarray(lay).astype(np.float32)
            arr[..., 3] = np.clip(arr[..., 3] * gain, 0, 255)
            lay = Image.fromarray(arr.astype(np.uint8), "RGBA")
        self._put(lay, self.px(x0), self.px(y0), add, alpha)

    # ---- text
    def text(self, s, x, y, rgba, shadow=True, bold=False, add=False, k=1.0):
        if not s:
            return 0
        lay = self.f.layer(s, self.s, tuple(int(v) for v in rgba), shadow, bold)
        if k != 1.0:
            lay = lay.resize((max(1, int(lay.width * k)), max(1, int(lay.height * k))), Image.NEAREST)
        self._put(lay, self.px(x), self.px(y), add)
        return self.f.width(s, bold) * k

    def text_c(self, s, cx, y, rgba, shadow=True, bold=False, k=1.0):
        self.text(s, cx - self.f.width(s, bold) * k / 2.0, y, rgba, shadow, bold, k=k)

    def text_r(self, s, rx, y, rgba, shadow=True, bold=False, k=1.0):
        self.text(s, rx - self.f.width(s, bold) * k, y, rgba, shadow, bold, k=k)

    def ttf(self, s, x, y, size, rgba, path=SERIF_ITALIC, add=False, spacing=0.0):
        """Anti-aliased TTF text (what a custom font provider would give): `size` is the line height in GUI px."""
        fnt = ImageFont.truetype(path, max(6, int(round(size * self.s))))
        lay = Image.new("RGBA", (int(fnt.getlength(s)) + 6 + int(len(s) * spacing * self.s), int(size * self.s * 1.6)), (0, 0, 0, 0))
        d = ImageDraw.Draw(lay)
        if spacing:
            cx = 0.0
            for c in s:
                d.text((cx, 0), c, font=fnt, fill=tuple(int(v) for v in rgba))
                cx += fnt.getlength(c) + spacing * self.s
        else:
            d.text((0, 0), s, font=fnt, fill=tuple(int(v) for v in rgba))
        self._put(lay, self.px(x), self.px(y), add)
        return lay.width / self.s

    def ttf_width(self, s, size, path=SERIF_ITALIC):
        return ImageFont.truetype(path, max(6, int(round(size * self.s)))).getlength(s) / self.s

    def save(self, path):
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.convert("RGB").save(path)


def ImageOps_mirror(im):
    return im.transpose(Image.FLIP_LEFT_RIGHT)


def tinted(im, tint):
    a = np.asarray(im).astype(np.float32)
    a[..., 0] *= tint[0] / 255.0
    a[..., 1] *= tint[1] / 255.0
    a[..., 2] *= tint[2] / 255.0
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


# ------------------------------------------------------------------------------------------------------ numpy texture helpers
def bilinear(tex, u, v, wrap_u=True, wrap_v=False):
    """Sample a float HxWx4 texture at texel coordinates (u, v), bilinear; wrap or clamp per axis."""
    h, w = tex.shape[:2]
    u0, v0 = np.floor(u).astype(int), np.floor(v).astype(int)
    fu, fv = (u - u0)[..., None], (v - v0)[..., None]

    def fetch(uu, vv):
        uu = uu % w if wrap_u else np.clip(uu, 0, w - 1)
        vv = vv % h if wrap_v else np.clip(vv, 0, h - 1)
        return tex[vv, uu]
    return (fetch(u0, v0) * (1 - fu) * (1 - fv) + fetch(u0 + 1, v0) * fu * (1 - fv)
            + fetch(u0, v0 + 1) * (1 - fu) * fv + fetch(u0 + 1, v0 + 1) * fu * fv)


def ssaa(w, h, fn, ss=4):
    """Draw with fn(ImageDraw, ss) on a w*ss by h*ss RGBA canvas, return the box-downsampled w by h image (a texture as shipped)."""
    im = Image.new("RGBA", (w * ss, h * ss), (0, 0, 0, 0))
    fn(ImageDraw.Draw(im), ss)
    return im.resize((w, h), Image.LANCZOS)


# ------------------------------------------------------------------------------------------------------ a fake world to draw over
def world_backdrop(w, h, seed=7, mood="day"):
    """A pixel-art-ish fake Minecraft scene, in real pixels, so translucency can be judged. Same proportions at any size."""
    yy, xx = np.mgrid[0:h, 0:w]
    t = yy / h
    if mood == "dusk":
        top, bot = np.array([34, 30, 74]), np.array([196, 112, 98])
    else:
        top, bot = np.array([86, 152, 226]), np.array([176, 212, 240])
    sky = top[None, None, :] * (1 - t[..., None]) + bot[None, None, :] * t[..., None]
    gy = h * 0.60
    ground = np.stack([66 + 14 * np.sin(xx / (w / 18.0)), 118 + 22 * np.sin(xx / (w / 11.0) + 1), 52 + 0 * t], axis=-1)
    a = np.where((yy > gy)[..., None], ground, sky)
    rng = np.random.RandomState(seed)
    cell = max(2, w // 160)
    noise = rng.randint(-7, 8, size=(h // cell + 1, w // cell + 1, 1))
    a = a + np.kron(noise, np.ones((cell, cell, 1)))[:h, :w]
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    d = ImageDraw.Draw(im)
    for tx in (0.07, 0.27, 0.70, 0.91):
        x0 = int(tx * w)
        tw = int(w * 0.022)
        d.rectangle([x0, int(h * 0.44), x0 + tw, int(h * 0.62)], fill=(70, 46, 28, 255))
        d.rectangle([x0 - int(w * 0.03), int(h * 0.24), x0 + tw + int(w * 0.03), int(h * 0.46)], fill=(38, 112, 48, 255))
        d.rectangle([x0 - int(w * 0.03), int(h * 0.24), x0 + tw + int(w * 0.03), int(h * 0.27)], fill=(52, 134, 62, 255))
    d.rectangle([int(w * 0.43), int(h * 0.16), int(w * 0.54), int(h * 0.61)], fill=(150, 150, 164, 255))
    d.rectangle([int(w * 0.43), int(h * 0.16), int(w * 0.455), int(h * 0.61)], fill=(176, 176, 190, 255))
    return im


def hearts_row(c, x, y, n, full, rgba=(214, 40, 40, 255)):
    pat = ["011011000", "111111100", "111111100", "111111100", "011111000", "001110000", "000100000"]
    for i in range(n):
        for r, row in enumerate(pat):
            for cc, ch in enumerate(row):
                if ch == "1":
                    col = rgba if i < full else (60, 20, 24, 255)
                    if r == 1 and cc == 1:
                        col = (255, 150, 150, 255) if i < full else col
                    c.fill(x + i * 8 + cc * 0.85, y + r * 0.85, x + i * 8 + cc * 0.85 + 0.9, y + r * 0.85 + 0.9, col)


def hotbar(c, cx, bottom, selected=2):
    """The vanilla hotbar (182 x 22), approximated: dark slots, light border, white selection box."""
    x, y = cx - 91, bottom - 22
    c.fill(x, y, x + 182, y + 22, (22, 22, 22, 200))
    c.outline(x, y, 182, 22, (140, 140, 140, 220))
    for i in range(9):
        c.fill(x + 3 + i * 20, y + 3, x + 3 + i * 20 + 16, y + 3 + 16, (70, 70, 70, 180))
    c.outline(x - 1 + selected * 20, y - 1, 24, 24, (255, 255, 255, 255), 1)
    return x, y

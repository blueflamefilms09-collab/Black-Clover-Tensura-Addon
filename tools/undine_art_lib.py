"""Painting primitives of the Undine's sheets (tools/gen_undine_art.py): the anime cel look of the Black Clover stills (flat blues with a darker blue shade, white foam streaks, thin dark
linework, light hatching), drawn with Pillow at a supersampled size and brought down with Lanczos, so every edge is smooth and every region is deterministic (seeded).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

sys.dont_write_bytecode = True
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from undine_geo_lib import rgb  # noqa: E402

SS = 3                                                 # supersampling of every drawn shape

# the palette, read off the two stills (images 122 / 123 of the owner's request: the wiki's Undine art)
HAIR = "#D8ECFE"
HAIR_SHADE = "#A9C6E6"
HAIR_SHADE2 = "#8EB0D6"
HAIR_LIGHT = "#F2F8FF"
GOWN = "#9CCDF3"
GOWN_MID = "#BFDFF5"
GOWN_DARK = "#70A0C6"
GOWN_DEEP = "#5A8DBB"
FOAM = "#F5FAFE"
FOAM_SHADE = "#CFE6F7"
SKIN = "#F4EBE9"
SKIN_SHADE = "#DCD3DC"
SKIN_SHADE2 = "#C4D3E4"
LINE = "#3E4250"
LINE_SOFT = "#6C7388"
IRIS = "#A5C7E6"
IRIS_DARK = "#5F8FB5"
LIP = "#D6A3A4"


# the gown's cone, shared by the painters and the model (tools/gen_undine_geo.py): radii and heights in model units, the number of strips of the two layers and how much wider than its sector each is
SK = dict(WAIST_R=3.8, HEM_R=24.0, WAIST_Y=31.0, INNER_N=16, OUTER_N=24, INNER_OVER=1.18, OUTER_OVER=1.25, OUTER_PUSH=0.55, OUTER_EXTRA=2.5)


def col(h, a=255):
    c = rgb(h)
    return int(c[0]), int(c[1]), int(c[2]), a


def canvas(w, h, fill=(0, 0, 0, 0)):
    return Image.new("RGBA", (w * SS, h * SS), fill)


def mask_canvas(w, h):
    return Image.new("L", (w * SS, h * SS), 0)


def done(img, w, h):
    """The supersampled image brought down to w x h as straight-alpha floats 0..255."""
    out = img.resize((w, h), Image.LANCZOS)
    return np.asarray(out, dtype=np.float32).copy()


def to_img(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def over(base, layer):
    """Alpha-composite two RGBA float arrays (straight alpha)."""
    a = layer[..., 3:4] / 255.0
    ba = base[..., 3:4] / 255.0
    oa = a + ba * (1 - a)
    rgbo = np.where(oa > 1e-5, (layer[..., :3] * a + base[..., :3] * ba * (1 - a)) / np.maximum(oa, 1e-5), 0.0)
    return np.concatenate([rgbo, oa * 255.0], axis=-1)


def fbm(rng, w, h, octaves=3, base=3):
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


def blur(mask, r):
    return mask.filter(ImageFilter.GaussianBlur(r * SS))


def shifted(mask, dx, dy):
    """A mask moved by (dx, dy) texels of the final size."""
    return ImageChops.offset(mask, int(round(dx * SS)), int(round(dy * SS)))


def minus(a, b):
    return ImageChops.subtract(a, b)


def torn(mask, rng, amount, scale):
    """Roughens the edge of a mask like torn foam: the blurred mask plus noise, cut at one half. amount in texels."""
    w, h = mask.size
    n = fbm(rng, w // 4 + 2, h // 4 + 2, 3, max(3, int(w / (scale * SS * 4))))
    nimg = Image.fromarray((np.clip(n, 0, 1) * 255).astype(np.uint8), "L").resize((w, h), Image.BICUBIC)
    b = np.asarray(blur(mask, amount * 0.6), dtype=np.float32) / 255.0
    nn = (np.asarray(nimg, dtype=np.float32) / 255.0 - 0.5) * 1.1
    return Image.fromarray((((b + nn * 0.55) > 0.5) * 255).astype(np.uint8), "L")


def fill_mask(img, mask, color):
    """Paints `color` (RGBA tuple) through a mask (L) onto an RGBA image."""
    solid = Image.new("RGBA", img.size, color)
    m = mask if color[3] == 255 else mask.point(lambda v: v * color[3] // 255)
    img.paste(solid, (0, 0), m)


def blob_path(draw, pts, radii, fill):
    """A path of discs: a lobed, cloud-edged ribbon (the silhouette of the stills' hair and waves)."""
    for (x, y), r in zip(pts, radii):
        draw.ellipse([x - r, y - r, x + r, y + r], fill=fill)


def smooth_ribbon(draw, pts, widths, fill, steps=4):
    """A tapered ribbon along a polyline: quads between the offset points, discs at the joints (round joins)."""
    n = len(pts)
    left, right = [], []
    for i in range(n):
        x0, y0 = pts[max(0, i - 1)]
        x1, y1 = pts[min(n - 1, i + 1)]
        dx, dy = x1 - x0, y1 - y0
        l = math.hypot(dx, dy) or 1.0
        nx, ny = -dy / l, dx / l
        w = widths[i] / 2.0
        left.append((pts[i][0] + nx * w, pts[i][1] + ny * w))
        right.append((pts[i][0] - nx * w, pts[i][1] - ny * w))
    draw.polygon(left + right[::-1], fill=fill)
    for p, w in zip(pts, widths):
        r = w / 2.0
        draw.ellipse([p[0] - r, p[1] - r, p[0] + r, p[1] + r], fill=fill)


def wave_pts(x0, y0, x1, y1, amp, waves, phase, n=60, amp2=0.0, waves2=0.0, phase2=0.0):
    """Points along a wavy line from (x0, y0) to (x1, y1)."""
    dx, dy = x1 - x0, y1 - y0
    l = math.hypot(dx, dy) or 1.0
    nx, ny = -dy / l, dx / l
    out = []
    for i in range(n):
        t = i / (n - 1.0)
        a = amp * math.sin(t * waves * math.tau + phase) + amp2 * math.sin(t * waves2 * math.tau + phase2)
        out.append((x0 + dx * t + nx * a, y0 + dy * t + ny * a))
    return out


def vgrad(w, h, top, bottom):
    t = np.linspace(0, 1, h, dtype=np.float32)[:, None, None]
    a = rgb(top)[None, None, :] * (1 - t) + rgb(bottom)[None, None, :] * t
    return np.broadcast_to(a, (h, w, 3)).copy()


def rgba(rgb3, alpha):
    h, w = alpha.shape
    out = np.zeros((h, w, 4), dtype=np.float32)
    out[..., :3] = rgb3
    out[..., 3] = alpha
    return out


def hatch(draw, rng, box, n, length, angle_deg, width, fill, jitter=0.3):
    """Short parallel strokes (the stills' light hatching) scattered inside a box (x0, y0, x1, y1)."""
    x0, y0, x1, y1 = box
    a = math.radians(angle_deg)
    for _ in range(n):
        x = rng.uniform(x0, x1)
        y = rng.uniform(y0, y1)
        ln = length * rng.uniform(0.6, 1.2)
        aa = a + rng.uniform(-jitter, jitter)
        draw.line([(x, y), (x + math.cos(aa) * ln, y + math.sin(aa) * ln)], fill=fill, width=max(1, int(width)))

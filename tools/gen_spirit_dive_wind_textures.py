"""Spirit Dive, WIND (the owner's reference pack: Yuno's Spirit Dive stills): writes the textures of the PACK wind look to src/main/resources/assets/nusmp/textures/aura/ (all deterministic:
`python3 -B tools/gen_spirit_dive_wind_textures.py`).

The look the pack shows: pale green-white light over the whole body, a crown of tall vertical bars with arrow-tip tops on a thin ring, ONE huge smooth leaf-shaped wing with an ornate cross at its tip,
a circle of connected crosses round the wrist (and behind the back), Yuno's long glowing spirit sword with crossed-circle guard rings (Zephyr) and the leaf bow of Boreas, white ribbons of wind and
four-point sparkles. Everything is drawn as thin glowing outlines with a pale inner fill (the cel look of the anime), never as blocks. Every texture is RGBA with RGB = 0 where alpha = 0 (SafeAdditive:
checked by SpiritDiveWindAssetTest).

    sdw_leaf.png        128x256  the smooth leaf sheet (u across, v root (top) .. tip (bottom)): pale fill, bright edge, midrib, faint veins (alpha blended, emissive)
    sdw_leaf_glow.png   128x256  its additive glow: the edge line, the midrib, a soft inner light
    sdw_cross.png       64x64    the ornate cross with arrow-tip arms (outline, pale fill)
    sdw_bar.png         16x96    one crown bar: an arrow-tip top, a double shaft, a small cross-tie (additive)
    sdw_ring.png        64x16    the thin ring of the crown (a bright line with small nodes), wrapped round the head as a band
    sdw_wrist.png       128x128  the ring of connected crosses and branches round the wrist
    sdw_circle.png      256x256  the big circle of connected crosses behind the back of the character tier
    sdw_sword.png       64x256   Yuno's spirit sword, tip at the top, the hilt at 73 percent of the height (as the REEL sword: the same quad)
    sdw_guard.png       128x128  the crossed circles of the guard, with sparkles
    sdw_bow.png         128x256  the leaf bow of Boreas (cross ornaments on the limbs, a thin string)
    sdw_arrow.png       32x256   the arrow of light (leaf head with a cross, tip at the top)
    sdw_star.png        32x32    a four-point sparkle
"""
import math
import os
import sys

sys.dont_write_bytecode = True

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "nusmp", "textures", "aura")
SS = 4                                                                  # supersampling of every drawing

CORE = np.array([246, 255, 246], np.float32)                             # white with a green breath
LIGHT = np.array([196, 255, 205], np.float32)                            # the pale green of the sheets
MID = np.array([120, 238, 150], np.float32)                              # the green of the edges
DEEP = np.array([52, 196, 100], np.float32)                              # the deepest green (the root of the leaf)


def save(rgba, name):
    os.makedirs(OUT, exist_ok=True)
    a = np.clip(np.rint(rgba), 0, 255).astype(np.uint8)
    a[a[..., 3] == 0, :3] = 0
    Image.fromarray(a, "RGBA").save(os.path.join(OUT, name + ".png"), optimize=True)
    print(name, a.shape[1], "x", a.shape[0])


def canvas(w, h):
    return Image.new("L", (w * SS, h * SS), 0)


def down(im, w, h):
    return np.asarray(im.resize((w, h), Image.LANCZOS), np.float32) / 255.0


def blur(arr, r):
    im = Image.fromarray(np.clip(arr * 255.0, 0, 255).astype(np.uint8), "L").filter(ImageFilter.GaussianBlur(r))
    return np.asarray(im, np.float32) / 255.0


def pt(x, y):
    return (x * SS, y * SS)


def line(d, pts, w, fill=255):
    d.line([pt(*p) for p in pts], fill=fill, width=max(1, int(round(w * SS))), joint="curve")


def poly(d, pts, fill=255):
    d.polygon([pt(*p) for p in pts], fill=fill)


def ellipse(d, cx, cy, rx, ry, w, fill=255):
    d.ellipse([pt(cx - rx, cy - ry), pt(cx + rx, cy + ry)], outline=fill, width=max(1, int(round(w * SS))))


def outline_of(mask_im, w):
    """The outline (w px wide) of a filled shape drawn on a supersampled canvas."""
    k = max(1, int(round(w * SS))) | 1
    inner = mask_im.filter(ImageFilter.MinFilter(k))
    return ImageChops.subtract(mask_im, inner)


def tint(core, edge, fill, glow=0.45, glow_r=2.2, deep_bias=None):
    """Colours an RGBA picture from masks (floats 0..1, same shape): the bright core lines, the coloured edge lines, a faint fill and a soft glow round everything."""
    h, w = core.shape
    g = blur(np.clip(core + edge * 0.8, 0, 1), glow_r)
    a = np.clip(core + edge + fill + g * glow, 0, 1)
    rgb = np.zeros((h, w, 3), np.float32)
    wsum = np.maximum(core + edge + fill + g * glow, 1e-5)
    rgb = (CORE[None, None, :] * core[..., None] + MID[None, None, :] * edge[..., None] + LIGHT[None, None, :] * fill[..., None] + MID[None, None, :] * (g * glow)[..., None]) / wsum[..., None]
    out = np.zeros((h, w, 4), np.float32)
    out[..., :3] = rgb
    out[..., 3] = a * 255.0
    return out


# ------------------------------------------------------------------ the cross with arrow-tip arms (a filled mask on a supersampled canvas)
def cross_mask(d, cx, cy, size, rot=0.0, arm=0.14, flare=0.30):
    """An ornate cross: the four arms end in an arrow tip (a diamond); the whole cross is one filled shape. size = half length of the long arms."""
    def rp(x, y):
        c, s = math.cos(rot), math.sin(rot)
        return (cx + x * c - y * s, cy + x * s + y * c)
    L, W = size, size * arm
    long_v, long_h = L, L * 0.72
    shape = [(-W, -long_v * 0.78), (0, -long_v), (W, -long_v * 0.78), (W, -long_h * 0.30)]                  # up arm (a spear head), then continues clockwise below
    shapes = []
    for k in range(4):
        a = k * math.pi / 2
        c, s = math.cos(a), math.sin(a)
        ln = long_v if k % 2 == 0 else long_h
        head = ln * 0.78
        pts = [(-W, -W), (-W, -head * 0.62), (-flare * size, -head * 0.62), (0, -ln), (flare * size, -head * 0.62), (W, -head * 0.62), (W, -W)]
        shapes.append([rp(x * c - y * s, x * s + y * c) for x, y in pts])
    for sp in shapes:
        poly(d, sp)
    poly(d, [rp(-W, -W), rp(W, -W), rp(W, W), rp(-W, W)])


# ------------------------------------------------------------------ the leaf
def leaf():
    W, H = 128, 256
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u = xx / (W - 1)
    v = yy / (H - 1)
    d = np.abs(u - 0.5) * 2.0                                                         # 0 on the midrib, 1 at the edge
    # fill: deeper green at the root, pale in the middle, nearly white toward the tip
    fill_col = DEEP[None, None, :] * np.clip(1 - v * 3.2, 0, 1)[..., None] + LIGHT[None, None, :] * np.clip(v * 3.2, 0, 1)[..., None]
    fill_col = fill_col * (1 - np.clip((v - 0.45) * 1.6, 0, 1)[..., None] * 0.55) + CORE[None, None, :] * (np.clip((v - 0.45) * 1.6, 0, 1)[..., None] * 0.55)
    fill_col = fill_col * (1 - 0.22 * d[..., None] ** 2) + MID[None, None, :] * (0.22 * d[..., None] ** 2)
    edge = np.clip((d - 0.80) / 0.20, 0, 1) ** 1.6                                     # a bright edge (the cel outline of the leaf)
    rib = np.exp(-((u - 0.5) * W / 1.6) ** 2) * np.clip(1 - v * 0.15, 0, 1)             # the midrib
    # side veins: curved lines from the midrib out and toward the tip
    veins = np.zeros_like(u)
    for k in range(1, 10):
        v0 = 0.06 + k * 0.095
        for sgn in (-1, 1):
            t = np.clip(d, 0, 1)
            vc = v0 + 0.22 * t ** 1.2                                                    # the vein runs toward the tip as it goes out
            veins = np.maximum(veins, np.exp(-((v - vc) * H / 1.4) ** 2) * (np.sign(u - 0.5) == sgn) * np.clip(1 - t * 0.9, 0, 1) * 0.55)
    # a soft darker band under the edge (the cel shade) and a pale gleam along the upper side
    shade = np.clip((d - 0.55) / 0.35, 0, 1) * np.clip(1 - (d - 0.55) / 0.4, 0, 1) * 0.0
    gleam = np.exp(-((u - 0.30) * W / 5.0) ** 2) * np.clip(v * 2.4, 0, 1) * np.clip(1.2 - v, 0, 1) * 0.22
    col = fill_col * (1 - 0.25 * veins[..., None]) + CORE[None, None, :] * (np.clip(edge * 0.85 + rib * 0.8 + veins * 0.35 + gleam, 0, 1))[..., None] * 0.75
    img = np.zeros((H, W, 4), np.float32)
    img[..., :3] = np.clip(col, 0, 255)
    img[..., 3] = 255.0 * np.clip(0.86 - 0.10 * d ** 2 + edge * 0.14 + rib * 0.1, 0, 1)
    img[:2, :, 3] = 0
    save(img, "sdw_leaf")
    # the additive glow: the edge, the rib, a soft inner light
    g = np.clip(edge * 0.95 + rib * 0.65 + veins * 0.28 + 0.22 * np.clip(1 - d, 0, 1) ** 0.8 * np.clip(v * 3, 0, 1), 0, 1)
    gl = np.zeros((H, W, 4), np.float32)
    gl[..., :3] = MID[None, None, :] * (1 - g[..., None] * 0.6) + CORE[None, None, :] * (g[..., None] * 0.6)
    gl[..., 3] = 255.0 * g
    gl[:2, :, 3] = 0
    save(gl, "sdw_leaf_glow")


# ------------------------------------------------------------------ the ornate cross
def cross():
    N = 64
    m = canvas(N, N)
    d = ImageDraw.Draw(m)
    cross_mask(d, N / 2, N / 2, 29, 0.0, 0.085, 0.20)
    edge = down(outline_of(m, 1.5), N, N)
    fill = down(m, N, N) * 0.30
    core = np.clip(edge - 0.1, 0, 1) * 0.0
    out = tint(core, edge, fill, 0.55, 1.6)
    # the edge reads white: raise the core too
    out[..., :3] = np.where(edge[..., None] > 0.2, CORE[None, None, :] * 0.8 + MID[None, None, :] * 0.2, out[..., :3])
    save(out, "sdw_cross")


# ------------------------------------------------------------------ the crown bar
def bar():
    W, H = 24, 96
    m = canvas(W, H)
    d = ImageDraw.Draw(m)
    cx = W / 2
    poly(d, [(cx, 0.8), (cx + 9.4, 22), (cx + 2.6, 19), (cx + 2.6, H), (cx - 2.6, H), (cx - 2.6, 19), (cx - 9.4, 22)])         # the arrow head and the shaft
    edge = down(outline_of(m, 1.1), W, H)
    fill = down(m, W, H) * 0.42
    # a small cross-tie a third of the way down and a bright core line up the shaft
    t = canvas(W, H)
    td = ImageDraw.Draw(t)
    line(td, [(cx - 6.5, 44), (cx + 6.5, 44)], 1.0)
    line(td, [(cx, 6), (cx, H)], 0.8)
    core = down(t, W, H) * 0.9
    out = tint(core, edge, fill, 0.5, 1.2)
    save(out, "sdw_bar")


# ------------------------------------------------------------------ the ring of the crown
def ring():
    W, H = 64, 16
    m = canvas(W, H)
    d = ImageDraw.Draw(m)
    line(d, [(0, H / 2), (W, H / 2)], 1.6)
    for x in (4, 20, 36, 52):                                                      # small nodes (the studs of the ring)
        d.ellipse([pt(x - 2.2, H / 2 - 2.6), pt(x + 2.2, H / 2 + 2.6)], fill=255)
    core = down(m, W, H)
    out = tint(core, np.zeros_like(core), np.zeros_like(core), 0.8, 2.0)
    save(out, "sdw_ring")


# ------------------------------------------------------------------ the circle of connected crosses (wrist and back)
def circle_of_crosses(name, N, crosses, rr, cross_size, rings, branch):
    m = canvas(N, N)
    d = ImageDraw.Draw(m)
    c = N / 2
    for r, w in rings:
        ellipse(d, c, c, r, r, w)
    n = crosses
    for k in range(n):                                                                # branches between the crosses: a bowed double line with small thorns
        a0 = 2 * math.pi * k / n
        a1 = 2 * math.pi * (k + 1) / n
        pts = []
        for s in range(0, 21):
            a = a0 + (a1 - a0) * s / 20
            r = rr + branch * math.sin(math.pi * s / 20) * (1 if k % 2 == 0 else -1)
            pts.append((c + r * math.cos(a), c + r * math.sin(a)))
        line(d, pts, 1.1 if N < 200 else 1.6)
        for s in (5, 10, 15):                                                         # a thorn at three points of each branch
            a = a0 + (a1 - a0) * s / 20
            r0 = rr + branch * math.sin(math.pi * s / 20) * (1 if k % 2 == 0 else -1)
            line(d, [(c + r0 * math.cos(a), c + r0 * math.sin(a)), (c + (r0 + (3 if N < 200 else 5) * (1 if s != 10 else -1)) * math.cos(a + 0.12), c + (r0 + (3 if N < 200 else 5) * (1 if s != 10 else -1)) * math.sin(a + 0.12))], 0.9)
    cm = canvas(N, N)
    cd = ImageDraw.Draw(cm)
    for k in range(n):
        a = 2 * math.pi * k / n
        cross_mask(cd, c + rr * math.cos(a), c + rr * math.sin(a), cross_size, a + math.pi / 2, 0.11, 0.24)
    line_core = down(m, N, N)
    edge = down(outline_of(cm, 1.2 if N < 200 else 1.8), N, N)
    fill = down(cm, N, N) * 0.35
    out = tint(np.clip(line_core, 0, 1), edge, fill, 0.55, 1.6 if N < 200 else 3.0)
    save(out, name)


# ------------------------------------------------------------------ the sword
def sword():
    W, H = 64, 256
    m = canvas(W, H)
    d = ImageDraw.Draw(m)
    cx = W / 2
    hilt = 0.70 * H
    poly(d, [(cx, 1), (cx + 7.4, 24), (cx + 8.6, hilt - 4), (cx - 8.6, hilt - 4), (cx - 7.4, 24)])                                  # the blade, a pointed tip
    edge = down(outline_of(m, 1.4), W, H)
    fill = down(m, W, H) * 0.50
    t = canvas(W, H)
    td = ImageDraw.Draw(t)
    line(td, [(cx, 10), (cx, hilt - 6)], 1.6)                                                                                     # the fuller
    line(td, [(cx - 4.4, 40), (cx - 4.6, hilt - 8)], 0.8)
    line(td, [(cx + 4.4, 40), (cx + 4.6, hilt - 8)], 0.8)
    line(td, [(cx - 10.5, hilt), (cx + 10.5, hilt)], 2.2)                                                                         # the cross guard
    d2 = canvas(W, H)
    gd = ImageDraw.Draw(d2)
    poly(gd, [(cx - 12.5, hilt - 2.6), (cx - 7, hilt - 1), (cx - 7, hilt + 3), (cx - 12.5, hilt + 3.4)])
    poly(gd, [(cx + 12.5, hilt - 2.6), (cx + 7, hilt - 1), (cx + 7, hilt + 3), (cx + 12.5, hilt + 3.4)])
    poly(gd, [(cx - 2.2, hilt + 3), (cx + 2.2, hilt + 3), (cx + 2.2, hilt + 0.26 * H * 0.78), (cx - 2.2, hilt + 0.26 * H * 0.78)])       # the grip
    cross_mask(gd, cx, hilt + 0.26 * H * 0.90, 8.0, 0.0, 0.14, 0.26)                                                               # the cross of the pommel
    edge2 = down(outline_of(d2, 1.1), W, H)
    core = np.clip(down(t, W, H) * 0.95, 0, 1)
    edge = np.clip(edge + edge2, 0, 1)
    fill = np.clip(fill + down(d2, W, H) * 0.4, 0, 1)
    out = tint(core, edge, fill, 0.55, 1.6)
    save(out, "sdw_sword")


def guard():
    N = 128
    m = canvas(N, N)
    d = ImageDraw.Draw(m)
    c = N / 2
    ellipse(d, c, c, 40, 40, 1.6)
    ellipse(d, c, c, 30, 30, 1.0)
    for ang in (0.0, math.pi / 3, 2 * math.pi / 3):                                   # three thin ellipses crossing at the hilt (the crossed circles of the guard)
        pts = []
        for s in range(0, 61):
            a = 2 * math.pi * s / 60
            x, y = 44 * math.cos(a), 12 * math.sin(a)
            pts.append((c + x * math.cos(ang) - y * math.sin(ang), c + x * math.sin(ang) + y * math.cos(ang)))
        line(d, pts, 1.2)
    core = down(m, N, N)
    st = canvas(N, N)
    sd = ImageDraw.Draw(st)
    for (x, y, r) in ((24, 24, 10), (104, 100, 9), (100, 26, 6)):                    # sparkles
        poly(sd, [(x, y - r), (x + r * 0.22, y - r * 0.22), (x + r, y), (x + r * 0.22, y + r * 0.22), (x, y + r), (x - r * 0.22, y + r * 0.22), (x - r, y), (x - r * 0.22, y - r * 0.22)])
    star = down(st, N, N)
    out = tint(np.clip(core + star, 0, 1), np.zeros_like(core), star * 0.3, 0.5, 2.0)
    save(out, "sdw_guard")


# ------------------------------------------------------------------ the bow of Boreas
def bow():
    W, H = 128, 256
    m = canvas(W, H)
    d = ImageDraw.Draw(m)
    # the limb: a leaf-shaped blade following an arc bulging to the left; the string on the right
    cx = 74
    pts_top, pts_bot = [], []
    n = 40
    for s in range(n + 1):
        t = s / n                                                                         # 0 at the grip, 1 at the tip
        y = H / 2 - t * (H / 2 - 6)
        x = cx - 40 * math.sin(t * math.pi * 0.62) ** 1.0 + 0.0                           # the limb curves to the left, the tips come back to the right
        x += 18 * max(0.0, t - 0.7) ** 1.0 * 4.0
        w = (3.0 + 10.0 * math.sin(math.pi * min(1.0, t * 1.15)) ** 0.8) * (1 - 0.15 * t)  # leaf shaped: widest in the middle of the limb
        pts_top.append((x, y, w))
        pts_bot.append((x, H - y, w))
    for pts in (pts_top, pts_bot):
        left = [(x - w, y) for x, y, w in pts]
        right = [(x + w * 0.55, y) for x, y, w in pts]
        poly(d, left + right[::-1])
    poly(d, [(cx - 6, H / 2 - 11), (cx + 6, H / 2 - 11), (cx + 6, H / 2 + 11), (cx - 6, H / 2 + 11)])           # the grip
    edge = down(outline_of(m, 1.5), W, H)
    fill = down(m, W, H) * 0.45
    t = canvas(W, H)
    td = ImageDraw.Draw(t)
    sx = pts_top[-1][0] + 2
    line(td, [(sx, 6), (sx - 3, H / 2), (sx, H - 6)], 0.9)                                    # the string
    line(td, [(cx - 30, H / 2 - 6), (cx - 30, H / 2 + 6)], 0.8)
    for pts in (pts_top, pts_bot):
        line(td, [(x - 0.2 * w, y) for x, y, w in pts[2:-4]], 0.8)
    cm = canvas(W, H)
    cd = ImageDraw.Draw(cm)
    for (px, py, w) in (pts_top[16], pts_bot[16], pts_top[30], pts_bot[30]):
        cross_mask(cd, px - 1.0, py, 7.5, 0.0, 0.13, 0.25)                                    # the cross ornaments on the limbs
    cross_mask(cd, cx, H / 2, 9.0, 0.0, 0.13, 0.25)                                           # and on the grip
    edge = np.clip(edge + down(outline_of(cm, 1.0), W, H), 0, 1)
    fill = np.clip(fill + down(cm, W, H) * 0.3, 0, 1)
    core = np.clip(down(t, W, H) * 0.9, 0, 1)
    out = tint(core, edge, fill, 0.5, 1.8)
    save(out, "sdw_bow")


def arrow():
    W, H = 32, 256
    m = canvas(W, H)
    d = ImageDraw.Draw(m)
    cx = W / 2
    poly(d, [(cx, 1), (cx + 9, 30), (cx + 3.2, 24), (cx + 1.0, 30), (cx + 1.0, 214), (cx - 1.0, 214), (cx - 1.0, 30), (cx - 3.2, 24), (cx - 9, 30)])      # a leaf head and a thin shaft
    # a second, fuller leaf over the head
    poly(d, [(cx, 6), (cx + 6.5, 28), (cx, 38), (cx - 6.5, 28)])
    for k in range(3):                                                                    # the fletching: leaf feathers
        y = 218 + k * 11
        poly(d, [(cx, y), (cx + 8, y + 12), (cx + 1, y + 7)])
        poly(d, [(cx, y), (cx - 8, y + 12), (cx - 1, y + 7)])
    edge = down(outline_of(m, 1.0), W, H)
    fill = down(m, W, H) * 0.5
    t = canvas(W, H)
    line(ImageDraw.Draw(t), [(cx, 8), (cx, 238)], 1.4)
    core = np.clip(down(t, W, H), 0, 1)
    out = tint(core, edge, fill, 0.6, 1.4)
    save(out, "sdw_arrow")


def star():
    N = 32
    m = canvas(N, N)
    d = ImageDraw.Draw(m)
    c, r = N / 2, 14.0
    poly(d, [(c, c - r), (c + r * 0.14, c - r * 0.14), (c + r, c), (c + r * 0.14, c + r * 0.14), (c, c + r), (c - r * 0.14, c + r * 0.14), (c - r, c), (c - r * 0.14, c - r * 0.14)])
    core = down(m, N, N)
    out = tint(core, np.zeros_like(core), np.zeros_like(core), 0.7, 1.8)
    save(out, "sdw_star")


def main():
    leaf()
    cross()
    bar()
    ring()
    circle_of_crosses("sdw_wrist", 128, 6, 46, 11, [(52, 1.1), (40, 0.8)], 5.0)
    circle_of_crosses("sdw_circle", 256, 8, 104, 22, [(122, 1.8), (90, 1.2), (60, 0.9)], 9.0)
    sword()
    guard()
    bow()
    arrow()
    star()


if __name__ == "__main__":
    main()

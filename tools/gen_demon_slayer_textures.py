"""Textures for the 0.48 Genesis Demon-Slayer Sword. Deterministic, procedural (no fonts, no input images).

    python tools/gen_demon_slayer_textures.py

  textures/entity/demon_slayer_sword.png        the 3D sword's skin (64x64; the layout is DemonSlayerRenderer's, see LAYOUT below)
  textures/entity/demon_slayer_bloom.png        the pommel's five-leaf clover (white + alpha, tinted near-black)
  textures/entity/demon_slayer_bloom_glow.png   its glow (white + alpha, drawn additive, tinted crimson)
  textures/particle/demon_{meteor,void_wall,cracks,ground}.png   VFX for DEMON_METEOR / NIHILITY_ZONE (greyscale + alpha)

The inventory icon stays the old 32x32 textures/item/demon_slayer_sword.png (untouched).
Blade coordinates are the renderer's model pixels: x across the blade (-7..7), y along it (1 at the guard .. 49 at the tip).
Texture u = x + 7 (+14 on the back), v = 49 - y. The spot and slot lists MUST match DemonSlayerRenderer.SPOTS_* / SLOT_*.
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_arcane_vfx_textures import fbm, grey  # noqa: E402

SS = 4
ASSETS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "nusmp", "textures")

# ---------------------------------------------------------------- shared with DemonSlayerRenderer (keep in sync)
BLADE_Y0, BLADE_Y1, TIP_RIGHT_Y = 1.0, 49.0, 46.0          # the tip is cut on a slant: 49 on the left edge, 46 on the right
HALF_BASE, HALF_TOP = 6.5, 5.5
SLOT_X, SLOT_Y0, SLOT_Y1 = 1.0, 3.0, 19.0                  # the central split: |x| < 1, 3 < y < 19
SPOTS_FRONT = [(-5, 24, -2, 27), (-4, 23, -3, 24), (-5.5, 25, -5, 26.5), (2, 29, 4, 32), (1, 30, 2, 31.5), (2.5, 32, 3.5, 33),
               (-3, 37, -1, 39), (-2.5, 39, -1.5, 40), (3, 13, 5, 15), (3.5, 15, 4.5, 16), (-5, 5, -3, 7), (0, 42, 2, 44), (-1, 42.5, 0, 43.5)]
SPOTS_BACK = [(-4, 30, -2, 33), (-2, 31, -1, 32), (2, 23, 4.5, 26), (-5, 14, -3, 16), (1, 39, 3, 41), (3, 6, 5, 8)]
# box UV origins (vanilla cube layout: top/bottom row of depth d, then the four sides of height h)
GUARD = (0, 50, 22, 3, 4)          # u, v, w, h, d
GUARD_END = (36, 0, 3, 5, 5)
GRIP = (36, 10, 3, 20, 3)
POMMEL = (36, 33, 5, 4, 5)
EDGE_U, SLOT_U, CAP_U = 28, 30, 32  # 2-wide strips along the blade length


def put(im, sub, name):
    d = os.path.join(ASSETS, sub)
    os.makedirs(d, exist_ok=True)
    im.save(os.path.join(d, name + ".png"), optimize=True)
    print("wrote", sub + "/" + name, im.size)


def half_width(y):
    t = (y - BLADE_Y0) / (BLADE_Y1 - BLADE_Y0)
    return HALF_BASE + (HALF_TOP - HALF_BASE) * t


def inside_blade(x, y):
    """Is local point (x, y) on the blade (outline incl. the slanted tip, minus the slot)?"""
    if y < BLADE_Y0 or y > BLADE_Y1:
        return False
    hw = half_width(y)
    if abs(x) > hw:
        return False
    top = BLADE_Y1 + (TIP_RIGHT_Y - BLADE_Y1) * (x + HALF_TOP) / (2 * HALF_TOP)   # the slanted top edge
    return y <= top


def edge_dist(x, y):
    hw = half_width(y)
    top = BLADE_Y1 + (TIP_RIGHT_Y - BLADE_Y1) * (x + HALF_TOP) / (2 * HALF_TOP)
    return min(hw - abs(x), top - y, y - BLADE_Y0)


# ---------------------------------------------------------------- the sword skin
def sword():
    S = 64
    img = np.zeros((S, S, 4), np.float32)
    rng = np.random.default_rng(480)
    grime = fbm(S, S, 16, 4801)
    fine = fbm(S, S, 4, 4802)

    def px(u, v, rgb, a=1.0):
        if 0 <= u < S and 0 <= v < S:
            img[v, u, :3] = np.clip(rgb, 0, 255)
            img[v, u, 3] = a * 255

    # -- blade faces: dirty gunmetal, worn bright edges, scratches, rust specks, the dark spots, the split
    for face, (u0, spots, seed) in enumerate([(0, SPOTS_FRONT, 11), (14, SPOTS_BACK, 23)]):
        frng = np.random.default_rng(4810 + seed)
        scratches = [(frng.uniform(-6, 6), frng.uniform(3, 46), frng.uniform(-0.9, 0.9), frng.integers(3, 7)) for _ in range(9)]
        for v in range(48):
            for u in range(14):
                x, y = u - 7 + 0.5, 49 - v - 0.5
                g = grime[v, u0 + u] - 0.5
                f = fine[v, u0 + u] - 0.5
                base = np.array([58, 54, 56]) + g * 34 + f * 14
                if inside_blade(x, y):
                    e = edge_dist(x, y)
                    if e < 0.9:
                        base = np.array([124, 118, 112]) + f * 30          # the worn cutting edge
                        if frng.random() < 0.18:
                            base = np.array([40, 36, 38])                  # a chip
                    elif e < 1.8:
                        base = base * 1.18                                 # the bevel catching light
                    if abs(x) < SLOT_X + 0.5 and SLOT_Y0 - 0.5 < y < SLOT_Y1 + 0.5:
                        base = np.array([30, 22, 26])                      # soot round the split
                for (sx, sy, sl, n) in scratches:
                    for k in range(n):
                        if abs(x - (sx + sl * k)) < 0.5 and abs(y - (sy + k)) < 0.5:
                            base = base * 1.35
                if frng.random() < 0.004:
                    base = np.array([150, 22, 30])                         # rust-red specks (the old sprite's red)
                px(u0 + u, v, base)
        for (x0, y0, x1, y1) in spots:                                     # the dark spots (void in game)
            for v in range(48):
                for u in range(14):
                    x, y = u - 7 + 0.5, 49 - v - 0.5
                    if x0 <= x <= x1 and y0 <= y <= y1:
                        px(u0 + u, v, np.array([12, 8, 12]) + rng.integers(0, 6))
                    elif x0 - 0.7 <= x <= x1 + 0.7 and y0 - 0.7 <= y <= y1 + 0.7:
                        px(u0 + u, v, np.array([46, 22, 28]))              # a faint scorch round each spot

    # -- edge strip, slot walls, fracture caps (2 px wide, along the blade)
    for v in range(48):
        for k in range(2):
            n = fine[v, EDGE_U + k] - 0.5
            px(EDGE_U + k, v, np.array([132, 126, 120]) + n * 40 if rng.random() > 0.12 else np.array([52, 48, 50]))
            px(SLOT_U + k, v, np.array([46, 8, 16]) + n * 20)
            px(CAP_U + k, v, np.array([96, 90, 94]) + n * 60)

    # -- boxes (vanilla cube layout)
    def box(spec, colour, top_mul=1.15, fn=None):
        u, v, w, h, d = spec
        faces = [(u + d, v, w, d, top_mul), (u + d + w, v, w, d, 0.8),                        # top, bottom
                 (u, v + d, d, h, 0.9), (u + d, v + d, w, h, 1.0), (u + d + w, v + d, d, h, 0.9), (u + d + w + d, v + d, w, h, 1.0)]
        for (fu, fv, fw, fh, mul) in faces:
            for j in range(fh):
                for i in range(fw):
                    n = fine[(fv + j) % S, (fu + i) % S] - 0.5
                    c = np.array(colour, np.float32) * mul + n * 22
                    if fn:
                        c = fn(c, i, j, fw, fh)
                    if i == 0 or j == 0 or i == fw - 1 or j == fh - 1:
                        c = c * 1.22                                                         # bevelled corners
                    px(fu + i, fv + j, c)

    def rivets(c, i, j, fw, fh):
        return c * 1.5 if fh >= 3 and j == fh // 2 and i % 5 == 2 else c

    def wrap(c, i, j, fw, fh):
        band = (j + i) % 4
        if band == 0:
            return c * 0.62                                                                  # the cloth wrap's shadowed overlap
        if band == 1:
            return c * 1.18
        return c

    def gem(c, i, j, fw, fh):
        if fw == 5 and fh == 4 and 1 <= i <= 3 and 1 <= j <= 2:
            return np.array([168, 18, 34]) * (1.25 if (i, j) == (2, 1) else 1.0)              # a crimson stone, front and back
        return c

    box(GUARD, (44, 42, 46), fn=rivets)
    box(GUARD_END, (52, 50, 54))
    box(GRIP, (92, 60, 38), fn=wrap)
    box(POMMEL, (48, 46, 50), fn=gem)
    out = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), "RGBA")
    put(out, "entity", "demon_slayer_sword")
    return out


# ---------------------------------------------------------------- the pommel's five-leaf clover
def clover_mask(S, scale=1.0, blur=0.0):
    """Five heart-shaped leaves round a small heart, with clear gaps between the leaves (the five-leaf grimoire's clover)."""
    im = Image.new("L", (S * SS, S * SS), 0)
    d = ImageDraw.Draw(im)
    c = S * SS / 2
    unit = S * SS * scale
    dist, lobe, off = unit * 0.29, unit * 0.092, unit * 0.064
    for k in range(5):
        a = math.radians(90 + k * 72)
        ax, ay = math.cos(a), -math.sin(a)                     # leaf direction (screen y down)
        px_, py_ = -ay, ax                                     # across the leaf
        for s in (-1, 1):                                      # a heart: two lobes ...
            ox, oy = c + ax * dist + px_ * s * off, c + ay * dist + py_ * s * off
            d.ellipse([ox - lobe, oy - lobe, ox + lobe, oy + lobe], fill=255)
        w = off + lobe * 0.92                                  # ... narrowing to a point near the centre
        d.polygon([(c + ax * unit * 0.07, c + ay * unit * 0.07),
                   (c + ax * dist + px_ * w, c + ay * dist + py_ * w),
                   (c + ax * dist - px_ * w, c + ay * dist - py_ * w)], fill=255)
        d.ellipse([c + ax * (dist + lobe * 0.55) - unit * 0.03, c + ay * (dist + lobe * 0.55) - unit * 0.03,
                   c + ax * (dist + lobe * 0.55) + unit * 0.03, c + ay * (dist + lobe * 0.55) + unit * 0.03], fill=0)   # the heart's notch
    d.ellipse([c - unit * 0.075, c - unit * 0.075, c + unit * 0.075, c + unit * 0.075], fill=255)
    im = im.resize((S, S), Image.LANCZOS)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    return np.asarray(im).astype(np.float32) / 255


def bloom():
    S = 64
    a = clover_mask(S)
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xx - S / 2 + 0.5, yy - S / 2 + 0.5) / (S / 2)
    ang = np.arctan2(yy - S / 2, xx - S / 2)
    veins = np.clip(1 - np.abs(np.sin(ang * 2.5 + math.pi / 4)) * 6, 0, 1) * (r > 0.12) * (r < 0.7)   # five veins out of the heart
    g = np.clip(0.78 + 0.22 * (1 - r) - veins * 0.45, 0, 1)
    put(grey(a, g), "entity", "demon_slayer_bloom")
    glow = clover_mask(S, scale=1.05, blur=3.5)
    glow = np.clip(glow * 0.85 + np.clip(1 - r, 0, 1) ** 2.2 * 0.55, 0, 1)
    put(grey(glow), "entity", "demon_slayer_bloom_glow")


# ---------------------------------------------------------------- VFX
def meteor():
    """U along the flight (tail at u=0, head at u=1), V across. Alpha = the whole shape; grey = the rim (black core in the ADD pass)."""
    W, H = 256, 64
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    u, v = (xx + 0.5) / W, (yy + 0.5) / H - 0.5
    n = fbm(W, H, 16, 4830) - 0.5
    head = 0.86
    width = np.where(u < head, 0.06 + 0.36 * (u / head) ** 1.6, 0.42 * np.sqrt(np.clip(1 - ((u - head) / (1 - head)) ** 2, 0, 1)))
    width = width * (1 + n * 0.9 * (u < head))
    d = np.abs(v + n * 0.08 * (1 - u)) / np.maximum(width, 1e-3)
    shape = np.clip(1 - d, 0, 1) ** 0.6 * np.clip(u * 3, 0, 1)
    rim = np.clip(1 - np.abs(d - 0.82) * 4.5, 0, 1) + np.clip((u - head) * 6, 0, 1) * np.clip(1 - d, 0, 1) * 0.5
    tongues = np.clip(np.sin(u * 40 + n * 9) * 0.5 + 0.5, 0, 1) * (u < head) * np.clip(1 - d, 0, 1) * 0.35
    put(grey(shape, np.clip(rim + tongues, 0, 1)), "particle", "demon_meteor")


def void_wall():
    """For a hoop: U round the ring, V top (0) to bottom (1). Ragged black streaks rising off the ground."""
    W, H = 64, 128
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    v = (yy + 0.5) / H
    streak = fbm(W, H, 8, 4840)
    streak = np.clip((streak - 0.35) * 2.2, 0, 1)
    cols = fbm(W, 1, 4, 4841)[0]
    height = 0.35 + 0.6 * cols                                  # each column reaches its own height
    a = np.clip((v - (1 - height[None, :])) / 0.25, 0, 1) * (0.55 + 0.45 * streak)
    put(grey(a), "particle", "demon_void_wall")


def crack_lines(S, seed, starts, steps, jitter, width, branch):
    rng = np.random.default_rng(seed)
    im = Image.new("L", (S * SS, S * SS), 0)
    d = ImageDraw.Draw(im)
    stack = [(x * SS, y * SS, a, width * SS, steps) for (x, y, a) in starts]
    while stack:
        x, y, a, w, n = stack.pop()
        for _ in range(n):
            a += rng.normal(0, jitter)
            nx, ny = x + math.cos(a) * 3 * SS, y + math.sin(a) * 3 * SS
            d.line([(x, y), (nx, ny)], fill=255, width=max(1, int(w)))
            x, y = nx, ny
            w *= 0.97
            if rng.random() < branch and w > SS * 0.8:
                stack.append((x, y, a + rng.choice([-1, 1]) * rng.uniform(0.5, 1.0), w * 0.7, n // 2))
    return np.asarray(im.resize((S, S), Image.LANCZOS)).astype(np.float32) / 255


def cracks():
    """Crimson veins crawling up the dome wall (white + alpha, drawn additive)."""
    S = 128
    starts = [(x, S - 1, -math.pi / 2) for x in range(6, S, 18)]
    a = crack_lines(S, 4850, starts, 40, 0.35, 2.2, 0.08)
    a = np.clip(a + np.asarray(Image.fromarray((a * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(2.5))).astype(np.float32) / 255 * 0.6, 0, 1)
    put(grey(a), "particle", "demon_cracks")


def ground():
    """The cracked ground disc of a Nihility zone: radial cracks and a broken ring (white + alpha)."""
    S = 128
    c = S / 2
    starts = [(c, c, k * 2 * math.pi / 9 + 0.2) for k in range(9)]
    a = crack_lines(S, 4860, starts, 20, 0.3, 2.6, 0.12)
    yy, xx = np.mgrid[0:S, 0:S].astype(np.float32)
    r = np.hypot(xx - c + 0.5, yy - c + 0.5) / c
    ring = np.clip(1 - np.abs(r - 0.92) * 22, 0, 1) * (fbm(S, S, 8, 4861) > 0.42)
    a = np.clip(a * (r < 0.97) + ring, 0, 1)
    put(grey(a), "particle", "demon_ground")


if __name__ == "__main__":
    sword()
    bloom()
    meteor()
    void_wall()
    cracks()
    ground()

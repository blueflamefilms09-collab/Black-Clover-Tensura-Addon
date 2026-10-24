"""Great Spirit GUI mock-up scenes, part 2: the holographic grimoire menu (ink to matrix), the clover icon set, the alert widget, the slime ripple.
A software re-implementation of the draw calls, NOT the game."""
import math
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

from gs_kit import (Canvas, SERIF, SERIF_BOLD, SERIF_ITALIC, clamp01, mixc, smooth, with_a, world_backdrop)
from gs_scenes import ELEMENTS, Kit, neon_rect, panel


# ------------------------------------------------------------------------------------------------------ text helpers
def text_glow(c, s, x, y, rgb, a=1.0, glow=0.55, k=1.0, shadow=False):
    """Vanilla bitmap text with a soft bloom (in game: the same string drawn again at +-1 px, alpha 0.25: one extra pass)."""
    if not s or a <= 0.01:
        return
    lay = c.f.layer(s, c.s, tuple(rgb) + (255,), shadow=False)
    if k != 1.0:
        lay = lay.resize((max(1, int(lay.width * k)), max(1, int(lay.height * k))), Image.NEAREST)
    pad = int(3 * c.s)
    big = Image.new("RGBA", (lay.width + 2 * pad, lay.height + 2 * pad), (0, 0, 0, 0))
    big.alpha_composite(lay, (pad, pad))
    bl = big.filter(ImageFilter.GaussianBlur(1.1 * c.s))
    c._put(bl, c.px(x) - pad, c.px(y) - pad, True, glow * a)
    c.text(s, x, y, tuple(rgb) + (int(255 * a),), shadow, k=k)


def scramble(s, frac, seed):
    rnd = random.Random(seed)
    glyphs = "01ABCDEF#%$&<>/\\=+*"
    n = len(s)
    return "".join(ch if (ch == " " or i < frac * n) else rnd.choice(glyphs) for i, ch in enumerate(s))


def draw_data_line(c, th, s, x, y, a=1.0, glow=0.5, plate=False, seed=0, frac=1.0):
    """A compiled data line: '> ' marker, dim key, bright value, with bloom. Same x,y as the ink line it replaces."""
    s2 = scramble(s, frac, seed) if frac < 1.0 else s
    if plate:
        w = c.f.width(s) + 8
        c.gradient(x - 3, y - 2, x - 3 + w, y + 9, (8, 24, 52, int(215 * a)), (4, 14, 34, int(215 * a)))
        c.outline(x - 3, y - 2, w, 11, th.data + (int(110 * a),), 1.0)
        c.fill(x - 3, y - 2, x - 2, y + 9, th.data + (int(255 * a),))
    parts = s2.split("  ", 1)
    key = parts[0]
    xx = x
    if key.startswith("> "):
        c.text("> ", xx, y, (255, 255, 255, int(255 * a)), False)
        xx += c.f.width("> ")
        key = key[2:]
    text_glow(c, key, xx, y, mixc(th.data, th.data_dim, 0.45), a, glow * 0.6)
    if len(parts) > 1:
        vx = x + c.f.width(s.split("  ", 1)[0] + "  ")
        text_glow(c, parts[1], vx, y, th.data, a, glow)


def rain(c, th, x, y0, y1, t, seed, a=0.25):
    """A column of falling matrix glyphs: dim body, a bright head (one drawString per column, rebuilt every 4 ticks)."""
    rnd = random.Random(seed)
    n = int((y1 - y0) // 7)
    if n <= 0:
        return
    head = int((t * 0.25 + seed * 3) % (n + 8)) - 4
    for i in range(n):
        d = head - i
        ch = rnd.choice("01ABCDEF<>/#")
        if d < 0 or d > 9:
            continue
        k = 1.0 if d == 0 else 0.55 * (1 - d / 10.0)
        col = (255, 255, 255) if d == 0 else th.data
        c.text(ch, x, y0 + i * 7, tuple(col) + (int(min(255, 255 * a * k * (1.0 if d else 1.4))),), False, k=0.8)


# ------------------------------------------------------------------------------------------------------ the holographic grimoire
INK_L = ["Halts the flow of time around the", "caster for a few heartbeats. Foes", "caught inside it freeze in place,", "while the mage alone may move.", "Its upkeep grows with the foes held."]
DATA_L = ["> FX       TIME.HALT(ALL)", "> DUR      3.0s +0.2s/MST", "> CASTER   EXEMPT", "> FOES     HOSTILE->FROZEN", "> UPKEEP   +2% MAG PER FOE"]
INK_R = ["Cost: 12% of your magicule", "Rests for eight seconds", "Reaches eight blocks around", "Mastery: a quarter, 3 of 12"]
DATA_R = ["> COST     12% MAG", "> COOLDOWN 8.0 s", "> RANGE    8 m  SPHERE", "> MASTERY  25%  PAGES 3/12"]
BOOK_GW, BOOK_GH = 360, 226


def parchment(w, h, seed=11):
    """A parchment texture at 2 texels per GUI pixel (shipped as one PNG per page)."""
    W, H = w * 2, h * 2
    rng = np.random.RandomState(seed)
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    lo = rng.rand(H // 16 + 2, W // 16 + 2).astype(np.float32)
    lo = np.asarray(Image.fromarray((lo * 255).astype(np.uint8)).resize((W, H), Image.BICUBIC)).astype(np.float32) / 255.0
    fib = rng.rand(H, W).astype(np.float32)
    fib = np.asarray(Image.fromarray((fib * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.6))).astype(np.float32) / 255.0
    col_a, col_b = np.array((238, 223, 184), np.float32), np.array((214, 190, 142), np.float32)
    base = col_a[None, None, :] * (1 - lo[..., None] * 0.7) + col_b[None, None, :] * lo[..., None] * 0.7
    base += (fib[..., None] - 0.5) * 14
    ex = np.minimum(np.minimum(xx, W - 1 - xx), np.minimum(yy, H - 1 - yy)) / (0.16 * min(W, H))
    base *= (0.76 + 0.24 * np.clip(ex, 0, 1) ** 0.8)[..., None]
    im = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8), "RGB").convert("RGBA")
    import gs_sprites
    gs_sprites.reg("gui/gs_b/page_parchment", im)
    return im


_PARCH = {}


def book_scene(K, scale, p, t=60.0, flip=None, status=None):
    """The floating holographic Grimoire open at a page. p = ink-to-matrix compile progress (0 ink .. 1 compiled)."""
    th = K.th
    gw, gh = BOOK_GW, BOOK_GH
    bg = world_backdrop(int(gw * scale), int(gh * scale), seed=5, mood="dusk")
    c = Canvas(gw, gh, scale, bg)
    c.gradient(0, 0, gw, gh, (4, 6, 16, 150), (2, 3, 10, 205))                      # the screen's dark scrim
    if th.key != "B":                                                              # the existing Great-Sage rune circle behind the book (the mod's own texture), tinted
        c.blit(K.circle, gw / 2 - 150, gh / 2 - 150, 300, 300, tint=th.glowc, alpha=0.20, add=True, rot=t * 0.15)
    else:
        c.blit(K.circle, gw / 2 - 140, gh / 2 - 140, 280, 280, tint=(230, 190, 110), alpha=0.10, add=True, rot=-t * 0.1)
    bob = math.sin(t * 0.09) * 1.6
    cx = gw / 2
    # projection base: floor glow + ring under the floating book
    c.vector((cx - 130, 196, cx + 130, 226), lambda d, k, ox, oy: d.ellipse([(cx - 110 - ox) * k, (208 - oy) * k, (cx + 110 - ox) * k, (218 - oy) * k], fill=th.glowc + (255,)),
             blur=3.5, add=True, alpha=0.40 if th.key != "B" else 0.25)
    c.vector((cx - 130, 196, cx + 130, 226), lambda d, k, ox, oy: d.ellipse([(cx - 90 - ox) * k, (210 - oy) * k, (cx + 90 - ox) * k, (216 - oy) * k], outline=th.neon + (255,), width=max(1, int(0.8 * k))),
             blur=0.6, add=True, alpha=0.9)
    by = 8 + bob
    cov_x, cov_y, cov_w, cov_h = cx - 177, by, 354, 196
    pw, ph = 163, 172
    lx, rx = cx - pw - 3, cx + 3
    py = by + 12
    # -------- cover
    if th.key == "A":
        c.gradient(cov_x, cov_y, cov_x + cov_w, cov_y + cov_h, (12, 34, 66, 120), (6, 18, 40, 150))
    elif th.key == "B":
        c.gradient(cov_x, cov_y, cov_x + cov_w, cov_y + cov_h, (96, 60, 34, 255), (58, 34, 18, 255))
        c.outline(cov_x + 2, cov_y + 2, cov_w - 4, cov_h - 4, th.metal_hi + (150,), 1.0)
        c.outline(cov_x + 5, cov_y + 5, cov_w - 10, cov_h - 10, th.metal_lo + (200,), 1.0)
    else:
        c.gradient(cov_x, cov_y, cov_x + cov_w, cov_y + cov_h, (24, 8, 40, 235), (8, 3, 16, 245))
        c.outline(cov_x + 3, cov_y + 3, cov_w - 6, cov_h - 6, th.neon + (110,), 1.0)
    neon_rect(c, cov_x, cov_y, cov_w, cov_h, th.neon, 1.0 if th.key != "B" else 0.55)
    # -------- page blocks (with thickness lines at the bottom)
    for i in range(4):
        shade = 28 - i * 6
        c.fill(lx - i, py + ph + i * 0.8, rx + pw + i, py + ph + i * 0.8 + 0.8, ((255, 255, 255, shade) if th.key == "B" else th.page_edge + (int(shade * 1.4),)))
    for (px0, side) in ((lx, "L"), (rx, "R")):
        if th.key == "B":
            key = (pw, ph)
            if key not in _PARCH:
                _PARCH[key] = parchment(pw, ph)
            c.blit(_PARCH[key], px0, py, pw, ph, flip_x=(side == "R"))
        else:
            c.gradient(px0, py, px0 + pw, py + ph, th.page_top, th.page_bot)
        c.outline(px0, py, pw, ph, (th.page_edge + (130,)) if th.key != "B" else (th.page_edge + (210,)), 1.0)
        for j in range(16):                                                         # the gutter shadow
            a = int((70 if th.key != "B" else 100) * (1 - j / 16.0) ** 2)
            x_ = (px0 + pw - 1 - j) if side == "L" else (px0 + j)
            c.fill(x_, py, x_ + 1, py + ph, (0, 0, 0, a))
    # the spine
    c.fill(cx - 3, py - 2, cx + 3, py + ph + 2, th.cover + (255,) if th.key != "A" else (10, 30, 60, 200))
    c.fill(cx - 0.5, py - 2, cx + 0.5, py + ph + 2, th.neon + (255,), add=(th.key != "B"))
    # -------- a watermark per direction
    if th.key == "C":
        f5 = K.clover[5][0]
        c.blit(f5, lx + pw / 2 - 40, py + 84, 80, 80, alpha=0.20)
        c.blit(f5, rx + pw / 2 - 40, py + 84, 80, 80, alpha=0.12)
    elif th.key == "A":
        c.blit(K.circle, lx + pw / 2 - 40, py + 84, 80, 80, tint=th.neon, alpha=0.30, add=True, rot=t * 0.2)
    else:
        c.blit(K.circle, lx + pw / 2 - 40, py + 84, 80, 80, tint=(110, 70, 30), alpha=0.28, rot=0)
    ink = th.ink
    ys = py + 6 + p * (ph + 26) - 14 if p > 0 else py - 30           # the compile line (centre of the band); at p = 0 it waits above the page
    band = 22.0

    def f_of(y_line):
        return clamp01((ys - y_line) / band)

    titlefont = SERIF_BOLD if th.key == "B" else SERIF_ITALIC
    # -------- header of the left page: title in ink, clover medallion, subtitle
    c.ttf("Time Stop", lx + 14, py + 7, 15, ink + (255,), titlefont)
    c.hline(lx + 14, lx + pw - 52, py + 29, th.page_edge + (130,), 1.0)
    c.fill(lx + 14, py + 29, lx + 40, py + 30, th.neon + (255,) if th.key != "B" else th.metal_lo + (255,))
    c.text("TIME MAGIC - PAGE 1 / 12", lx + 14, py + 33, mixc(ink, th.page_edge, 0.2) + (215,), False, k=0.75)
    draw_icon(c, K, 4, lx + pw - 46, py + 4, 36, "grimoire/time", t)
    # -------- body: ink lines turn into data lines as the compile line passes
    for i, (ik, dl) in enumerate(zip(INK_L, DATA_L)):
        y = py + 52 + i * 13.5
        f = f_of(y)
        a_ink, a_dat = 1 - smooth(f * 1.6), smooth(f * 1.7 - 0.15)
        if a_ink > 0.02:
            c.ttf(ik, lx + 14, y - 2, 9.6, ink + (int(255 * a_ink),), SERIF_ITALIC)
        if a_dat > 0.02:
            draw_data_line(c, th, dl, lx + 14, y, a_dat, plate=(th.key == "B"), seed=i * 7 + int(p * 40), frac=clamp01(f * 1.25) if f < 0.98 else 1.0)
    # -------- right page: heading and rows
    fh = f_of(py + 12)
    if fh < 0.7:
        c.ttf("Particulars", rx + 18, py + 7, 15, ink + (int(255 * (1 - smooth(fh * 1.4))),), titlefont)
    if fh > 0.2:
        draw_data_line(c, th, "[ ANALYSIS ]", rx + 18, py + 12, smooth(fh * 1.6 - 0.3), plate=False, seed=3)
    c.hline(rx + 18, rx + pw - 14, py + 29, th.page_edge + (130,), 1.0)
    c.fill(rx + 18, py + 29, rx + 44, py + 30, th.neon + (255,) if th.key != "B" else th.metal_lo + (255,))
    for i, (ik, dl) in enumerate(zip(INK_R, DATA_R)):
        y = py + 42 + i * 17
        f = f_of(y)
        a_ink, a_dat = 1 - smooth(f * 1.6), smooth(f * 1.7 - 0.15)
        if a_ink > 0.02:
            c.ttf(ik, rx + 18, y - 3, 9.6, ink + (int(255 * a_ink),), SERIF_ITALIC)
        if a_dat > 0.02:
            draw_data_line(c, th, dl, rx + 18, y, a_dat, plate=(th.key == "B"), seed=i * 5 + 100 + int(p * 40), frac=clamp01(f * 1.25) if f < 0.98 else 1.0)
            if dl.startswith("> COST") or dl.startswith("> MASTERY"):          # a glowing gauge bar under the value
                frac = 0.36 if "COST" in dl else 0.75
                bx, bw = rx + 18, 100
                c.fill(bx, y + 11.5, bx + bw, y + 13, (6, 20, 40, int(200 * a_dat)))
                c.fill(bx, y + 11.5, bx + bw * frac, y + 13, th.data + (int(230 * a_dat),))
    fy = py + ph - 40                                                              # the Sage line in the lower part of the right page
    ff = f_of(fy)
    if ff < 0.7:
        c.ttf("The mage nods: it is understood.", rx + 18, fy - 2, 9.6, mixc(ink, th.page_edge, 0.25) + (int(255 * (1 - smooth(ff * 1.6))),), SERIF_ITALIC)
    if ff > 0.2:
        draw_data_line(c, th, "> SAGE     COMPLETE", rx + 18, fy, smooth(ff * 1.6 - 0.3), plate=(th.key == "B"), seed=77, frac=clamp01(ff * 1.3) if ff < 0.98 else 1.0)
    # matrix rain only over the compiled part (A and C; B keeps the hologram to the data plates)
    if th.key != "B" and p > 0:
        for j, xr in enumerate((lx + 5, lx + pw - 9, rx + 5, rx + pw - 9)):
            rain(c, th, xr, py + 4, min(py + ph - 6, ys - 4), t, seed=j + 1, a=0.55)
    # -------- the compile line itself, with glitch fringes
    if 0 < p < 1.0 and py < ys < py + ph:
        c.vector((lx - 4, ys - 6, rx + pw + 4, ys + 6), lambda d, k, ox, oy: d.rectangle([(lx - ox) * k, (ys - 0.6 - oy) * k, (rx + pw - ox) * k, (ys + 0.6 - oy) * k], fill=(th.data if th.key != "B" else (90, 210, 255)) + (255,)),
                 blur=1.8, add=True, alpha=0.95)
        c.fill(lx, ys - 0.5, rx + pw, ys + 0.5, (255, 255, 255, 220), add=True)
        c.gradient(lx, ys - 12, rx + pw, ys, (0, 0, 0, 0), (th.data if th.key != "B" else (90, 210, 255)) + (34,), add=True)
        rnd = random.Random(int(p * 50))
        for _ in range(3):                                                         # ghost fragments of the page edge
            gy = ys + rnd.uniform(-10, 4)
            c.fill(lx - 1.5, gy, lx + 3, gy + 1, (255, 70, 90, 150), add=True)
            c.fill(rx + pw - 3, gy, rx + pw + 1.5, gy + 1, (70, 220, 255, 150), add=True)
    # -------- hologram scanlines (A, C strong; B only a hint)
    sl = {"A": 16, "B": 5, "C": 13}[th.key]
    yv = py
    while yv < py + ph:
        c.fill(lx, yv, rx + pw, yv + 1.0 / scale, (200, 240, 255, sl), add=True)
        yv += 2.0
    # -------- cover corners (filigree) over everything
    cs = 44
    c.blit(K.corner, cov_x - 3, cov_y - 3, cs, cs)
    c.blit(K.corner, cov_x + cov_w - cs + 3, cov_y - 3, cs, cs, flip_x=True)
    c.blit(K.corner, cov_x - 3, cov_y + cov_h - cs + 3, cs, cs, flip_y=True)
    c.blit(K.corner, cov_x + cov_w - cs + 3, cov_y + cov_h - cs + 3, cs, cs, flip_x=True, flip_y=True)
    # -------- floating motes
    rnd = random.Random(4)
    for i in range(18):
        mx = cx + rnd.uniform(-175, 175)
        my0 = rnd.uniform(0, 210)
        my = (my0 - t * rnd.uniform(0.15, 0.5)) % 226
        sz = rnd.choice((1, 1, 1.5, 2))
        c.fill(mx, my, mx + sz, my + sz, th.glowc + (int(rnd.uniform(90, 200)),), add=True)
    chip = status if status is not None else ("Great Sage: analysing..." if p < 1 else "Analysis complete.")
    c.text_c(chip, cx, by + cov_h + 3, (th.data if th.key != "B" else (130, 214, 255)) + (255,), True)
    if flip is not None:
        flip_leaf(c, K, flip, rx, py, pw, ph, cx)
    return c


def flip_leaf(c, K, theta, rx, py, pw, ph, cx):
    """A page leaf turned to angle theta (0 flat on the right .. pi flat on the left): the leaf foreshortens by cos(theta), shaded as it turns from the light."""
    th = K.th
    w = pw * abs(math.cos(theta))
    if w < 2:
        return
    x0 = cx + 3 if theta < math.pi / 2 else cx - 3 - w
    k = 0.55 + 0.45 * abs(math.cos(theta))
    c.fill(rx + 1, py + 1, rx + pw, py + ph, (0, 0, 0, 70))                       # the leaf's shadow on the page below
    if th.key != "B":
        c.gradient(x0, py + 1, x0 + w, py + ph - 1, mixc(th.page_top[:3], (0, 0, 0), 0.25) + (255,), mixc(th.page_bot[:3], (0, 0, 0), 0.25) + (255,))
        c.gradient(x0, py + 1, x0 + w, py + ph - 1, th.page_top[:3] + (90,), th.page_bot[:3] + (90,))
    else:
        c.gradient(x0, py + 1, x0 + w, py + ph - 1, (232, 214, 172, 250), (206, 184, 136, 250))
    c.gradient(x0, py + 1, x0 + w, py + ph - 1, (0, 0, 0, int(120 * (1 - k))), (0, 0, 0, int(160 * (1 - k))))
    for i in range(7):                                                              # compiled lines on the leaf, squeezed
        y = py + 18 + i * 17
        c.fill(x0 + w * 0.12, y, x0 + w * (0.55 + 0.3 * ((i * 37) % 5) / 5.0), y + 1.0, (th.data if th.key != "B" else (60, 170, 240)) + (120,), add=(th.key != "B"))
    c.outline(x0, py, w, ph, th.page_edge + (220,), 1.0)
    c.fill(x0 + (w - 1 if theta < math.pi / 2 else 0), py, x0 + (w if theta < math.pi / 2 else 1), py + ph, th.neon + (255,), add=True)


# ------------------------------------------------------------------------------------------------------ clover icons
def draw_icon(c, K, n, x, y, size, art, t, hover=False, locked=False, seed=0):
    th = K.th
    frame, glow, drips = K.clover[n]
    pulse = 0.55 + 0.45 * math.sin(t * 0.12 + seed)
    if n >= 4 and not locked:
        c.blit(glow, x - size * 0.06, y - size * 0.06, size * 1.12, size * 1.12, tint=th.glow4 if n == 4 else th.glow5, alpha=min(1.0, (0.55 + 0.45 * pulse) * (1.5 if hover else 1.0)), add=True)
    hole = 0.175 * 2 * size                                                        # the skill art in the medallion (the existing PNG, picture only), circular mask
    ax, ay = x + size / 2 - hole / 2 * 1.04, y + size / 2 - hole / 2 * 1.04
    a = K.icon_art(art)
    px = c.px(hole * 1.04)
    im = a.resize((px, px), Image.NEAREST)
    mask = Image.new("L", (px, px), 0)
    ImageDraw.Draw(mask).ellipse([0, 0, px - 1, px - 1], fill=255)
    if locked:
        g = im.convert("LA").convert("RGBA")
        im = Image.blend(g, Image.new("RGBA", g.size, (0, 0, 0, 255)), 0.45)
    im.putalpha(ImageChops.multiply(im.getchannel("A"), mask))
    c._put(im, c.px(ax), c.px(ay))
    fr = frame
    if locked:
        arr = np.asarray(frame).astype(np.float32)
        gray = arr[..., :3].mean(axis=2, keepdims=True)
        arr[..., :3] = gray * 0.55
        arr[..., 3] *= 0.8
        fr = Image.fromarray(arr.astype(np.uint8), "RGBA")
    c.blit(fr, x, y, size, size)
    if n == 5 and not locked:
        c.blit(drips, x, y + math.sin(t * 0.1 + seed) * size * 0.012, size, size)
    if n == 4 and not locked:                                                      # a travelling sparkle on one leaf tip
        ang = t * 0.05 + seed
        sx, sy = x + size / 2 + math.cos(ang) * size * 0.34, y + size / 2 + math.sin(ang) * size * 0.34
        s_ = size * 0.07 * (0.6 + 0.4 * math.sin(t * 0.3))
        c.vector((sx - s_ * 3, sy - s_ * 3, sx + s_ * 3, sy + s_ * 3), lambda d, k, ox, oy: d.polygon(
            [((sx - ox) * k, (sy - s_ * 2.2 - oy) * k), ((sx + s_ * 0.5 - ox) * k, (sy - s_ * 0.5 - oy) * k), ((sx + s_ * 2.2 - ox) * k, (sy - oy) * k), ((sx + s_ * 0.5 - ox) * k, (sy + s_ * 0.5 - oy) * k),
             ((sx - ox) * k, (sy + s_ * 2.2 - oy) * k), ((sx - s_ * 0.5 - ox) * k, (sy + s_ * 0.5 - oy) * k), ((sx - s_ * 2.2 - ox) * k, (sy - oy) * k), ((sx - s_ * 0.5 - ox) * k, (sy - s_ * 0.5 - oy) * k)],
            fill=(255, 255, 255, 255)), blur=0.4, add=True)
    if hover:
        c.vector((x - 4, y - 4, x + size + 4, y + size + 4), lambda d, k, ox, oy: d.ellipse([(x - 1 - ox) * k, (y - 1 - oy) * k, (x + size + 1 - ox) * k, (y + size + 1 - oy) * k],
                                                                                           outline=th.neon + (255,), width=max(1, int(0.9 * k))), blur=0.5, add=True, alpha=0.9)


ICON_SETS = {
    3: ["hawk_eye", "firebolt", "six_eyes", "unbreakable_will"],
    4: ["grimoire/time", "grimoire/wind", "grimoire/flame", "grimoire/storm"],
    5: ["grimoire/anti_magic", "grimoire/forbidden", "the_almighty", "grimoire/kotodama"],
}
TIER_NAMES = {3: ("EXTRA SKILL", "three-leaf clover"), 4: ("UNIQUE SKILL", "four-leaf clover, glowing"), 5: ("ULTIMATE / ANTI-MAGIC", "five-leaf clover, corrupt slime")}


def clover_scene(K, scale, t=50.0):
    th = K.th
    gw, gh = 420, 168
    bg = world_backdrop(int(gw * scale), int(gh * scale), seed=9, mood="dusk")
    c = Canvas(gw, gh, scale, bg)
    panel(c, K, 3, 3, gw - 6, gh - 6, corner=22)
    colw = (gw - 16) / 3.0
    for ci, n in enumerate((3, 4, 5)):
        x0 = 8 + ci * colw
        title, sub = TIER_NAMES[n]
        tcol = {3: th.neon, 4: mixc(th.glow4, (255, 255, 255), 0.3), 5: mixc(th.glow5, (255, 255, 255), 0.25)}[n]
        c.text_c(title, x0 + colw / 2, 11, tcol + (255,), True, k=0.85)
        c.text_c(sub, x0 + colw / 2, 21, th.dim + (255,), True, k=0.7)
        arts = ICON_SETS[n]
        big = 40
        gx = x0 + (colw - (big * 2 + 10)) / 2
        draw_icon(c, K, n, gx, 33, big, arts[0], t, seed=ci)
        draw_icon(c, K, n, gx + big + 10, 33, big, arts[1], t + 13, hover=True, seed=ci + 2)
        c.text_c("idle", gx + big / 2, 75, th.dim + (255,), False, k=0.7)
        c.text_c("hover", gx + big + 10 + big / 2, 75, th.dim + (255,), False, k=0.7)
        sm = 26
        gx2 = x0 + (colw - (sm * 3 + 12)) / 2
        draw_icon(c, K, n, gx2, 90, sm, arts[2], t + 5, seed=ci + 4)
        draw_icon(c, K, n, gx2 + sm + 6, 90, sm, arts[3], t + 9, seed=ci + 6)
        draw_icon(c, K, n, gx2 + 2 * (sm + 6), 90, sm, arts[0], t, locked=True)
        c.text_c("HUD size 26 px  /  locked", x0 + colw / 2, 120, th.dim + (255,), False, k=0.7)
        c.text_c({3: "Tensura: EXTRA", 4: "Tensura: UNIQUE", 5: "Tensura: ULTIMATE"}[n], x0 + colw / 2, 134, th.text + (200,), False, k=0.7)
        c.text_c({3: "Grimoire cover: 3 leaves", 4: "Grimoire cover: 4 leaves", 5: "Grimoire cover: 5 leaves"}[n], x0 + colw / 2, 144, th.text + (160,), False, k=0.7)
    return c


# ------------------------------------------------------------------------------------------------------ alert widget
def notice(c, K, x, y, w, h, kind, lines, t, typed=1.0):
    th = K.th
    acc = {"info": th.neon, "ok": th.accent, "warn": th.warn, "bad": th.bad}[kind]
    panel(c, K, x, y, w, h, corner=12, glow=0.7, corners=True, mid=False, accent=acc)
    c.fill(x + 2, y + 3, x + 4, y + h - 3, acc + (255,))
    c.fill(x + 2, y + 3, x + 4, y + h - 3, acc + (255,), add=True)
    yy = y + 4
    total = sum(len(s) for s, _ in lines)
    shown = int(total * typed)
    part = ""
    for s, col in lines:
        part = s[:max(0, shown)]
        shown -= len(s)
        if part:
            c.text(part, x + 8, yy, tuple(col) + (255,), True)
        yy += 10
        if shown <= 0:
            break
    if typed < 1.0 and int(t) % 4 < 2:
        cur = c.f.width(part) if part else 0
        c.fill(x + 8 + cur + 1, yy - 10, x + 8 + cur + 4, yy - 2, acc + (255,))


def alert_scene(K, scale, t=60.0):
    th = K.th
    gw, gh = 340, 150
    bg = world_backdrop(int(gw * scale), int(gh * scale), seed=2, mood="day")
    c = Canvas(gw, gh, scale, bg)
    cs = 38
    cx0, cy0 = gw - cs - 6, gh - cs - 6
    nw = 176
    n1h = 42
    notice(c, K, gw - nw - 6, cy0 - 2 - n1h - 4 - n1h, nw, n1h, "ok",
           [("[Notice... Great Sage has", th.accent), ("completed analysis of Time Stop.]", th.text), ("Result: page 3 is open.", th.dim)], t)
    notice(c, K, gw - nw - 6, cy0 - 2 - n1h, nw, n1h, "warn",
           [("[Notice... Great Sage has", th.warn), ("detected a hostile presence.]", th.text), ("Result: 3 foes, 24 m north.", th.dim)], t, typed=0.82)
    ping = (t * 0.06) % 1.0
    c.blit(K.ripple, cx0 - 8 - ping * 12, cy0 - 8 - ping * 12, cs + 16 + ping * 24, cs + 16 + ping * 24, tint=th.neon, alpha=0.7 * (1 - ping), add=True)
    c.vector((cx0 - 6, cy0 - 6, cx0 + cs + 6, cy0 + cs + 6), lambda d, k, ox, oy: d.ellipse([(cx0 - ox) * k, (cy0 - oy) * k, (cx0 + cs - ox) * k, (cy0 + cs - oy) * k], fill=th.glowc + (255,)), blur=3, add=True, alpha=0.35)
    c.blit(K.crest, cx0, cy0 + math.sin(t * 0.1) * 0.8, cs, cs)
    c.text_r("x2", cx0 + cs + 3, cy0 + cs - 7, th.text + (255,), True, k=0.8)
    return c


# ------------------------------------------------------------------------------------------------------ button, hover ripple, click
def button(c, K, x, y, w, h, label, age=None, mx=None, click=0.0):
    """A themed button. age = ticks since the pointer entered (None = idle), mx = pointer x, click = 0..1 flash of the snap."""
    th = K.th
    hov = 0.0 if age is None else smooth(age / 4.0)
    dy = 0.6 * click
    ya = y + dy
    c.gradient(x, ya, x + w, ya + h, th.btn_top, th.btn_bot)
    c.gradient(x + 1, ya + 1, x + w - 1, ya + h * 0.45, (255, 255, 255, int(40 + 25 * hov)), (255, 255, 255, 6))
    if th.key == "C":                                                              # drips from the lower edge
        for bx, ln in ((0.18, 3.2), (0.52, 4.6), (0.83, 2.6)):
            xx = x + w * bx
            c.vector((xx - 4, ya + h - 2, xx + 4, ya + h + 7), lambda d, k, ox, oy, xx=xx, ln=ln: (
                d.line([(xx - ox) * k, (ya + h - 1 - oy) * k, (xx - ox) * k, (ya + h + ln - oy) * k], fill=th.neon2 + (255,), width=int(1.4 * k)),
                d.ellipse([(xx - 1.5 - ox) * k, (ya + h + ln - 1.2 - oy) * k, (xx + 1.5 - ox) * k, (ya + h + ln + 1.6 - oy) * k], fill=th.neon2 + (255,))), alpha=0.9)
    if age is not None and mx is not None:
        c.scissor(x + 1, ya + 1, x + w - 1, ya + h - 1)
        for delay, amp in ((0, 1.0), (4, 0.7), (8, 0.45)):
            a = age - delay
            if a < 0 or a > 22:
                continue
            r = 3 + a * 1.55
            fade = (1 - a / 22.0) * amp
            c.blit(K.ripple, mx - r, ya + h / 2 - r * 0.9, r * 2, r * 1.8, tint=th.ripple, alpha=0.95 * fade, add=True)
        c.vector((mx - 16, ya, mx + 16, ya + h), lambda d, k, ox, oy: d.ellipse([(mx - 10 - ox) * k, (ya + 2 - oy) * k, (mx + 10 - ox) * k, (ya + h - 2 - oy) * k], fill=th.ripple + (255,)), blur=3, add=True, alpha=0.22 * hov)
        c.no_scissor()
    if click > 0:
        c.fill(x, ya, x + w, ya + h, (255, 255, 255, int(90 * click)), add=True)
    neon_rect(c, x, ya, w, h, mixc(th.dim, th.neon, 0.25 + 0.75 * hov), 0.55 + 0.45 * hov, 0.4 + 0.8 * hov)
    for (ox_, oy_, fx, fy) in ((0, 0, False, False), (w, 0, True, False), (0, h, False, True), (w, h, True, True)):
        cs = 11
        c.blit(K.corner, x + ox_ - (cs - 2 if fx else 2), ya + oy_ - (cs - 2 if fy else 2), cs, cs, flip_x=fx, flip_y=fy)
    col = mixc(th.text, (255, 255, 255), hov)
    c.text_c(label, x + w / 2, ya + (h - 8) / 2 + 0.5, col + (255,), True)


def cursor(c, px_, py_):
    c.vector((px_ - 2, py_ - 2, px_ + 9, py_ + 12), lambda d, k, ox, oy: d.polygon(
        [((px_ - ox) * k, (py_ - oy) * k), ((px_ + 6 - ox) * k, (py_ + 6 - oy) * k), ((px_ + 2.6 - ox) * k, (py_ + 6.4 - oy) * k), ((px_ + 4.4 - ox) * k, (py_ + 10 - oy) * k),
         ((px_ + 2.6 - ox) * k, (py_ + 10.6 - oy) * k), ((px_ + 1 - ox) * k, (py_ + 7 - oy) * k), ((px_ - ox) * k, (py_ + 8 - oy) * k)], fill=(255, 255, 255, 255), outline=(0, 0, 0, 255)))


def hover_scene(K, scale):
    th = K.th
    gw, gh = 252, 104
    bg = world_backdrop(int(gw * scale), int(gh * scale), seed=8, mood="dusk")
    c = Canvas(gw, gh, scale, bg)
    c.gradient(0, 0, gw, gh, (4, 6, 16, 150), (2, 3, 10, 200))
    frames = [("idle", None, None, 0.0), ("hover: enter (t+3)", 3, 52, 0.0), ("hover: spread (t+11)", 11, 66, 0.0), ("click: snap", 20, 66, 1.0)]
    for i, (name, age, mx, clk) in enumerate(frames):
        fx, fy = 6 + (i % 2) * 124, 6 + (i // 2) * 50
        c.text(name, fx, fy, th.dim + (255,), True, k=0.75)
        button(c, K, fx, fy + 12, 112, 22, "Grimoire Slot", age, fx + mx - 6 if mx else None, clk)
        if mx is not None:
            cursor(c, fx + mx - 6, fy + 12 + 14)
    return c

#!/usr/bin/env python3
"""
Mock-ups of the anime UI theme (client/ui/theme, docs/gui_theme.md), drawn with PIL. NOT the game: this is a re-implementation of the
theme's draw calls (panel, bar, brackets, sweep, hover, chip, Sage block, magic circle) and of the screens' layout numbers, using the real
circle textures from assets/anime_multiverse/textures/gui/theme/. Use it to judge colours, alpha and layout without the game; the Java is the truth.

    python3 tools/ui_preview/preview_ui.py OUT_DIR            # writes codex.png, status.png, ... into OUT_DIR
    python3 tools/ui_preview/preview_ui.py OUT_DIR --scale 3  # bigger

The GUI is 427 x 240 GUI pixels (a 1280x720 window at GUI scale 3), drawn at --scale (default 2) real pixels per GUI pixel, over a fake
world backdrop so that the translucency can be judged. Text uses a monospace stand-in for Minecraft's font (6 px per character).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageChops

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, "..", "..", "src", "main", "resources", "assets", "anime_multiverse", "textures", "gui", "theme")
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"
FONTB = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf"

S = 2
W, H = 427, 240

# palette (UiTheme.java)
PANEL_TOP, PANEL_BOTTOM, PANEL_DEEP = 0xCC111C34, 0xCC080D1B, 0xD9060912
CYAN, VIOLET, GOLD, GREEN, RED, WHITE = 0xFF5CD8FF, 0xFF9A7CFF, 0xFFE8C468, 0xFF7CF0B0, 0xFFFF6A6A, 0xFFFFFFFF
TEXT, TEXT_DIM, TEXT_FAINT, SAGE, SAGE_HEAD = 0xFFE6F2FF, 0xFF8FA3BF, 0xFF5C6E88, 0xFFBFEFFF, 0xFF5CD8FF


def rgba(argb):
    return ((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255)


def with_alpha(c, a):
    return (c & 0xFFFFFF) | (int(round(max(0.0, min(1.0, a)) * 255)) << 24)


def scale_alpha(c, a):
    return (c & 0xFFFFFF) | (int(round(((c >> 24) & 255) * max(0.0, min(1.0, a)))) << 24)


def mix(a, b, t):
    out = 0
    for sh in (0, 8, 16, 24):
        ca, cb = (a >> sh) & 255, (b >> sh) & 255
        out |= (int(round(ca + (cb - ca) * t)) & 255) << sh
    return out


class Gui:
    def __init__(self, scale=S, backdrop=True):
        self.s = scale
        self.img = Image.new("RGBA", (W * scale, H * scale), (0, 0, 0, 255))
        if backdrop:
            self.backdrop()
        self.font = ImageFont.truetype(FONT, 10 * scale)
        self.fontb = ImageFont.truetype(FONTB, 10 * scale)
        self.clip = None

    def backdrop(self):
        s = self.s
        h, w = H * s, W * s
        yy, xx = np.mgrid[0:h, 0:w]
        t = yy / h
        sky = np.stack([90 + 60 * (1 - t), 150 + 50 * (1 - t), 220 + 20 * (1 - t)], axis=-1)
        ground = np.stack([70 + 0 * t, 120 + 30 * np.sin(xx / 40.0), 55 + 0 * t], axis=-1)
        a = np.where((yy > h * 0.62)[..., None], ground, sky)
        rng = np.random.RandomState(7)
        a = a + rng.randint(-8, 8, size=(h, w, 1))
        # a few blocky trees / a tower silhouette so that the blur-less translucency shows something
        arr = np.clip(a, 0, 255).astype(np.uint8)
        im = Image.fromarray(arr, "RGB").convert("RGBA")
        d = ImageDraw.Draw(im)
        for tx in (30, 120, 300, 390):
            d.rectangle([tx * s, int(H * 0.45 * s), (tx + 12) * s, int(H * 0.66 * s)], fill=(60, 40, 25, 255))
            d.rectangle([(tx - 14) * s, int(H * 0.28 * s), (tx + 26) * s, int(H * 0.48 * s)], fill=(40, 110, 45, 255))
        d.rectangle([180 * s, int(H * 0.2 * s), 230 * s, int(H * 0.65 * s)], fill=(150, 150, 160, 255))
        self.img = im

    # ---- primitives, in GUI pixels
    def _blend(self, x0, y0, x1, y1, layer):
        s = self.s
        x0, y0, x1, y1 = int(round(x0 * s)), int(round(y0 * s)), int(round(x1 * s)), int(round(y1 * s))
        if self.clip:
            cx0, cy0, cx1, cy1 = self.clip
            nx0, ny0, nx1, ny1 = max(x0, cx0), max(y0, cy0), min(x1, cx1), min(y1, cy1)
            if nx1 <= nx0 or ny1 <= ny0:
                return
            layer = layer.crop((nx0 - x0, ny0 - y0, nx1 - x0, ny1 - y0))
            x0, y0 = nx0, ny0
        self.img.alpha_composite(layer, (x0, y0))

    def fill(self, x0, y0, x1, y1, argb):
        s = self.s
        w, h = int(round((x1 - x0) * s)), int(round((y1 - y0) * s))
        if w <= 0 or h <= 0 or (argb >> 24) == 0:
            return
        self._blend(x0, y0, x1, y1, Image.new("RGBA", (w, h), rgba(argb)))

    def gradient(self, x0, y0, x1, y1, top, bottom):
        s = self.s
        w, h = int(round((x1 - x0) * s)), int(round((y1 - y0) * s))
        if w <= 0 or h <= 0:
            return
        t = np.linspace(0, 1, h)[:, None, None]
        a = np.array(rgba(top), dtype=float)[None, None, :] * (1 - t) + np.array(rgba(bottom), dtype=float)[None, None, :] * t
        arr = np.repeat(a, w, axis=1).astype(np.uint8)
        self._blend(x0, y0, x1, y1, Image.fromarray(arr, "RGBA"))

    def outline(self, x, y, w, h, argb):
        self.fill(x, y, x + w, y + 1, argb)
        self.fill(x, y + h - 1, x + w, y + h, argb)
        self.fill(x, y + 1, x + 1, y + h - 1, argb)
        self.fill(x + w - 1, y + 1, x + w, y + h - 1, argb)

    def text(self, s_, x, y, argb, bold=False, shadow=False):
        sc = self.s
        f = self.fontb if bold else self.font
        layer = Image.new("RGBA", (int(len(s_) * 6 * sc) + 8, 12 * sc), (0, 0, 0, 0))
        d = ImageDraw.Draw(layer)
        if shadow:
            d.text((sc, sc * 0.2 + sc), s_, font=f, fill=(0, 0, 0, (argb >> 24) & 255))
        d.text((0, sc * 0.2), s_, font=f, fill=rgba(argb))
        self._blend(x, y - 0.5, x + layer.width / sc, y - 0.5 + layer.height / sc, layer)

    def center(self, s_, cx, y, argb, bold=False):
        self.text(s_, cx - len(s_) * 3, y, argb, bold)

    def circle_layer(self, name, cx, cy, half, deg, rgb, a, additive=False):
        if a <= 0.004 or half < 2:
            return
        sc = self.s
        size = max(2, int(round(half * 2 * sc)))
        tier = "" if half * 2 >= 170 else ("_128" if half * 2 >= 84 else "_64")
        if name == "circle_glow":
            tier = ""
        im = Image.open(os.path.join(TEX, name + tier + ".png")).convert("RGBA")
        im = im.resize((size, size), Image.BILINEAR)
        alpha = im.split()[3].point(lambda v: int(v * min(1.0, a)))
        col = Image.new("RGBA", im.size, (int(((rgb >> 16) & 255)), int(((rgb >> 8) & 255)), int(rgb & 255), 255))
        col.putalpha(alpha)
        col = col.rotate(-deg, resample=Image.BICUBIC, expand=False)   # GUI y is down: a positive Z rotation is clockwise
        x0, y0 = cx - half, cy - half
        if additive:
            if self.clip:      # the scissor also cuts the additive glow: draw it normally into a temp, then clip-blend
                tmp = Gui(self.s, backdrop=False)
                tmp.img = Image.new("RGBA", self.img.size, (0, 0, 0, 0))
                tmp.circle_layer(name, cx, cy, half, deg, rgb, a, False)
                cx0, cy0, cx1, cy1 = self.clip
                self.img.alpha_composite(tmp.img.crop((cx0, cy0, cx1, cy1)), (cx0, cy0))
                return
            region = self.img.crop((int(round(x0 * sc)), int(round(y0 * sc)), int(round(x0 * sc)) + size, int(round(y0 * sc)) + size))
            add = Image.new("RGBA", col.size, (0, 0, 0, 255))
            colp = Image.new("RGBA", col.size, (0, 0, 0, 0))
            colp.paste(col, (0, 0))
            rgbpart = Image.merge("RGB", [ImageChops.multiply(c, colp.split()[3]) for c in colp.split()[:3]])
            r = ImageChops.add(region.convert("RGB"), rgbpart)
            out = r.convert("RGBA")
            self.img.paste(out, (int(round(x0 * sc)), int(round(y0 * sc))))
        else:
            self._blend(x0, y0, x0 + size / sc, y0 + size / sc, col)

    def magic_circle(self, cx, cy, radius, t, rgb, alpha, style=None, build=1.0, squash=1.0):
        st = style or STANDARD
        if alpha <= 0.01 or radius < 6 or build <= 0:
            return
        rgbi = rgb & 0xFFFFFF
        r, g, b = (rgbi >> 16) & 255, (rgbi >> 8) & 255, rgbi & 255
        hr, hg, hb = [int(c + (255 - c) * 0.55) for c in (r, g, b)]
        hi = (hr << 16) | (hg << 8) | hb
        pulse = 0.5 + 0.5 * math.sin((t / 90.0) * 2 * math.pi)

        def stage(frm):
            return max(0.0, min(1.0, (min(build, 1.0) - frm) / 0.34))

        def grow(sg):
            u = 1 - sg
            return 0.7 + 0.3 * (1 - u * u * u)

        def ang(speed, start):
            return (t * speed + start) % 360.0

        def L(name, rad, deg, col, a, add=False):
            if squash == 1.0:
                self.circle_layer(name, cx, cy, rad, deg, col, a, add)
            else:
                # a squashed circle: draw at full size into a temp canvas, squash vertically, composite
                sc = self.s
                size = int(rad * 2 * sc) + 4
                tmp = Gui(self.s, backdrop=False)
                tmp.img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
                tmp.circle_layer(name, size / sc / 2, size / sc / 2, rad, deg, col, a, False)
                sq = tmp.img.resize((size, max(1, int(size * squash))), Image.BILINEAR)
                self.img.alpha_composite(sq, (int(round((cx - rad) * sc)), int(round(cy * sc - sq.height / 2))))

        b0, b1, b2, b3, b4 = stage(0), stage(.12), stage(.24), stage(.36), stage(.48)
        k = build if build > 1 else 1.0
        gl = alpha * st[5] * (0.65 + 0.35 * pulse) * min(1.0, build)
        L("circle_glow", radius * 1.32 * min(build, 1.1), 0, rgbi, gl, True)
        L("circle_outer", radius * k * grow(b0), ang(st[0], 0), rgbi, alpha * b0)
        L("circle_arcs", radius * 1.12 * k * grow(b4), ang(st[4], 40), hi, alpha * 0.8 * b4)
        L("circle_arcs", radius * 0.66 * k * grow(b4), ang(-st[4] * 0.7, 200), hi, alpha * 0.7 * b4)
        L("circle_star", radius * 0.64 * k * grow(b1), ang(st[1], 15), rgbi, alpha * b1)
        L("circle_poly", radius * 0.42 * k * grow(b2), ang(st[2], 0), hi, alpha * 0.9 * b2)
        L("circle_inner", radius * 0.22 * k * grow(b3), ang(st[3], 0), hi, alpha * (0.75 + 0.25 * pulse) * b3)

    # ---- the theme kit
    def panel(self, x, y, w, h, accent, glow=1.0, alpha=1.0):
        a = accent & 0xFFFFFF
        self.outline(x - 2, y - 2, w + 4, h + 4, with_alpha(a, 0.05 * glow * alpha))
        self.outline(x - 1, y - 1, w + 2, h + 2, with_alpha(a, 0.12 * glow * alpha))
        self.gradient(x, y, x + w, y + h, scale_alpha(PANEL_TOP, alpha), scale_alpha(PANEL_BOTTOM, alpha))
        self.gradient(x + 1, y + 1, x + w - 1, y + 1 + min(h // 3, 26), with_alpha(0xFFFFFF, 0.07 * alpha), 0x00FFFFFF)
        self.outline(x + 1, y + 1, w - 2, h - 2, with_alpha(a, 0.16 * alpha))
        self.outline(x, y, w, h, with_alpha(a, (0.32 + 0.45 * glow) * alpha))
        self.brackets(x, y, w, h, 6, with_alpha(a, 0.95 * alpha))

    def brackets(self, x, y, w, h, ln, argb):
        self.fill(x, y, x + ln, y + 2, argb); self.fill(x, y, x + 2, y + ln, argb)
        self.fill(x + w - ln, y, x + w, y + 2, argb); self.fill(x + w - 2, y, x + w, y + ln, argb)
        self.fill(x, y + h - 2, x + ln, y + h, argb); self.fill(x, y + h - ln, x + 2, y + h, argb)
        self.fill(x + w - ln, y + h - 2, x + w, y + h, argb); self.fill(x + w - 2, y + h - ln, x + w, y + h, argb)

    def sweep(self, x, y, w, h, t, accent, alpha=1.0):
        if h < 8 or w < 8:
            return
        yy = y + int((t * 0.7) % (h + 16)) - 8
        a = accent & 0xFFFFFF
        top, bot = max(y, yy - 7), min(y + h, yy)
        if bot > top:
            self.gradient(x, top, x + w, bot, with_alpha(a, 0), with_alpha(a, 0.10 * alpha))
        if y <= yy < y + h:
            self.fill(x, yy, x + w, yy + 1, with_alpha(a, 0.22 * alpha))

    def panel_scan(self, x, y, w, h, accent, glow, alpha, t):
        self.panel(x, y, w, h, accent, glow, alpha)
        self.sweep(x + 1, y + 1, w - 2, h - 2, t, accent, alpha)

    def well(self, x, y, w, h, accent, alpha=1.0):
        self.fill(x, y, x + w, y + h, scale_alpha(PANEL_DEEP, alpha))
        self.outline(x, y, w, h, with_alpha(accent & 0xFFFFFF, 0.38 * alpha))

    def heading(self, text, x, y, w, rgb):
        a = rgb & 0xFFFFFF
        self.text(text, x, y, 0xFF000000 | a)
        ry = y + 9 + 1
        self.fill(x, ry, x + w, ry + 1, with_alpha(a, 0.22))
        self.fill(x, ry, x + min(w, 30), ry + 1, with_alpha(a, 0.9))
        self.fill(x + w - 3, ry - 1, x + w, ry + 2, with_alpha(a, 0.7))
        return y + 9 + 5

    def hover(self, x, y, w, h, amount, rgb):
        a = rgb & 0xFFFFFF
        self.fill(x, y, x + w, y + h, with_alpha(a, 0.13 * amount))
        self.fill(x, y, x + 2, y + h, with_alpha(a, 0.9 * amount))
        self.fill(x, y + h - 1, x + w, y + h, with_alpha(a, 0.25 * amount))

    def chip(self, x, y, w, h, active, hovered, rgb):
        a = rgb & 0xFFFFFF
        k = 1.0 if active else 0.6 if hovered else 0.25
        self.gradient(x, y, x + w, y + h, with_alpha(a, 0.10 + 0.28 * k), with_alpha(a, 0.04 + 0.14 * k))
        self.outline(x, y, w, h, with_alpha(a, 0.35 + 0.6 * k))
        if active:
            self.fill(x + w - 2, y + 2, x + w, y + h - 2, with_alpha(0xFFFFFF, 0.85))

    def scrim(self, alpha=1.0):
        self.gradient(0, 0, W, H, with_alpha(0x050812, 0.62 * alpha), with_alpha(0x020409, 0.82 * alpha))
        e = max(24, min(W, H) // 5)
        self.gradient(0, 0, W, e, with_alpha(0, 0.45 * alpha), 0x00000000)
        self.gradient(0, H - e, W, H, 0x00000000, with_alpha(0, 0.45 * alpha))

    def bar(self, x, y, w, h, frac, rgb, t, alpha=1.0):
        a = rgb & 0xFFFFFF
        self.fill(x, y, x + w, y + h, with_alpha(0x03050A, 0.72 * alpha))
        self.outline(x - 1, y - 1, w + 2, h + 2, with_alpha(a, 0.42 * alpha))
        fw = int(round(w * max(0.0, min(1.0, frac))))
        if fw > 0:
            self.gradient(x, y, x + fw, y + h, with_alpha(mix(0xFF000000 | a, 0xFFFFFFFF, 0.38), 0.95 * alpha),
                          with_alpha(mix(0xFF000000 | a, 0xFF000000, 0.45), 0.95 * alpha))
            sx = x + int((t * 1.3) % (fw + 36)) - 18
            s0, s1 = max(x, sx), min(x + fw, sx + 14)
            if s1 > s0:
                self.fill(s0, y, s1, y + h, with_alpha(0xFFFFFF, 0.26 * alpha))
            self.fill(x, y, x + fw, y + 1, with_alpha(0xFFFFFF, 0.30 * alpha))
            self.fill(x + fw - 1, y - 1, x + fw, y + h + 1, with_alpha(0xFFFFFF, 0.9 * alpha))
            self.fill(x + fw, y, x + fw + 1, y + h, with_alpha(a, 0.5 * alpha))
        for k in range(1, 10):
            tx = x + w * k // 10
            self.fill(tx, y, tx + 1, y + h, with_alpha(0, 0.28 * alpha) if tx <= x + fw else with_alpha(0xFFFFFF, 0.10 * alpha))

    def sage(self, x, y, head, body, result, width, typed=None, alpha=1.0):
        """The Great-Sage block (typed=None: fully typed; else the number of characters shown)."""
        def wrap(s_):
            if not s_:
                return []
            out, line = [], ""
            for wd in s_.split():
                if len(line) * 6 + (len(wd) + (1 if line else 0)) * 6 <= width:
                    line = (line + " " + wd) if line else wd
                else:
                    out.append(line); line = wd
            out.append(line)
            return out
        lines = [(l, SAGE_HEAD) for l in wrap(head)] + [(l, SAGE) for l in wrap(body)] + [(l, GOLD) for l in wrap(result)]
        n = 10 ** 6 if typed is None else typed
        for text, col in lines:
            if n <= 0:
                break
            show = text[:n]
            n -= len(text)
            self.text(show, x, y, scale_alpha(col, alpha))
            if n < 0 and typed is not None:
                self.fill(x + len(show) * 6 + 1, y, x + len(show) * 6 + 4, y + 8, scale_alpha(SAGE_HEAD, alpha))
            y += 10
        return y

    def button(self, x, y, w, h, label, hover=0.0):
        self.panel(x, y, w, h, mix(TEXT_DIM, CYAN, hover), 0.25 + 0.75 * hover, 1.0)
        self.hover(x + 2, y + 2, w - 4, h - 4, hover, CYAN)
        self.center(label, x + w // 2, y + (h - 8) // 2, mix(TEXT, WHITE, hover))

    def save(self, path):
        self.img.convert("RGB").save(path)


STANDARD = (0.30, -0.55, 0.85, -1.30, 2.2, 0.45)
CALM = (0.12, -0.22, 0.34, -0.52, 0.9, 0.25)
FAST = (0.9, -1.6, 2.3, -3.4, 5.5, 0.7)


# ---------------------------------------------------------------- the screens (layout numbers from the Java)
def player(g, cx, cy, h):
    """A blocky stand-in for the player figure."""
    u = h / 32.0
    g.fill(cx - 4 * u, cy - 16 * u, cx + 4 * u, cy - 8 * u, 0xFFC8946A)          # head
    g.fill(cx - 4 * u, cy - 8 * u, cx + 4 * u, cy + 4 * u, 0xFF3B6EA8)           # body
    g.fill(cx - 8 * u, cy - 8 * u, cx - 4 * u, cy + 4 * u, 0xFFC8946A)           # arms
    g.fill(cx + 4 * u, cy - 8 * u, cx + 8 * u, cy + 4 * u, 0xFFC8946A)
    g.fill(cx - 4 * u, cy + 4 * u, cx, cy + 16 * u, 0xFF2B3E6B)                  # legs
    g.fill(cx, cy + 4 * u, cx + 4 * u, cy + 16 * u, 0xFF2B3E6B)


def codex(page, t=120.0, typed=None, hover_row=1):
    g = Gui()
    g.scrim()
    bw, bh = min(W - 56, 440), min(H - 48, 260)
    bx, by = (W - bw) // 2 - 10, (H - bh - 20) // 2
    g.magic_circle(bx + bw / 2, by + bh / 2, min(W, H) * 0.62, t, VIOLET, 0.22, CALM)
    # frame
    g.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x55000000)
    g.fill(bx, by, bx + bw, by + bh, with_alpha(0x04060D, 0.86))
    g.outline(bx - 2, by - 2, bw + 4, bh + 4, with_alpha(CYAN, 0.05)); g.outline(bx - 1, by - 1, bw + 2, bh + 2, with_alpha(CYAN, 0.12))
    g.outline(bx, by, bw, bh, with_alpha(CYAN, 0.7)); g.outline(bx + 3, by + 3, bw - 6, bh - 6, with_alpha(GOLD, 0.55))
    g.brackets(bx, by, bw, bh, 10, with_alpha(GOLD, 0.95))
    spine = bx + bw // 2
    g.gradient(spine - 12, by + 6, spine, by + bh - 6, 0, with_alpha(CYAN, 0.10)); g.gradient(spine, by + 6, spine + 12, by + bh - 6, with_alpha(CYAN, 0.10), 0)
    g.gradient(spine, by + 6, spine + 1, by + bh - 6, with_alpha(CYAN, 0.9), with_alpha(CYAN, 0.15))
    # body
    g.gradient(bx + 6, by + 6, bx + bw - 6, by + bh - 6, with_alpha(0x131E38, 0.78), with_alpha(0x090E1C, 0.82))
    g.gradient(bx + 6, by + 6, bx + bw - 6, by + 6 + 34, with_alpha(0xFFFFFF, 0.05), 0x00FFFFFF)
    g.magic_circle(spine, by + bh / 2, (bh - 12) * 0.46, t, CYAN, 0.13, CALM)
    titles = ["YOU", "STATUS", "WEAPONS", "RELICS", "BESTIARY", "SPIRIT LORDS", "GUIDE"]
    notes = ["Subject identified.", "Multiverse record read.", "Weapon records compiled.", "Relic records compiled.", "Hostile entities recorded.",
             "Spirit Lord records compiled.", "Notice: turn pages with A and D."]
    g.center(titles[page], spine, by + 10, GOLD)
    g.fill(spine - 26, by + 20, spine + 26, by + 21, with_alpha(GOLD, 0.35))
    num = f"{page + 1} / 7"
    g.text(num, bx + bw - 14 - len(num) * 6, by + bh - 14, TEXT_DIM)
    if page > 0:
        g.text("<", bx + 10, by + bh - 14, CYAN)
    if page < 6:
        g.text(">", bx + bw - 14, by + bh - 14, CYAN)
    line = "Analysis complete. " + notes[page]
    g.sage(bx + 22, by + bh - 15, "", line[:typed] if typed else line, "", bw - 120)
    # tabs
    glyphs = ["*", "S", "W", "R", "B", "L", "?"]
    for i in range(7):
        ty = by + 14 + i * 26
        on = i == page
        ext = 22 if on else 14
        g.chip(bx + bw - 2, ty, ext + 2, 24, on, False, GOLD if on else CYAN)
        g.center(glyphs[i], bx + bw + (ext - 2) // 2 + 1 + 0, ty + 8, WHITE if on else CYAN)
    x, y, w, h = bx + 20, by + 22, bw - 40, bh - 40
    g.clip = (int((x - 4) * g.s), int(y * g.s), int((x + w + 4) * g.s), int((y + h) * g.s))
    half = w // 2 - 8
    if page == 0:
        fx, fy = x + half // 2, y + h // 2 + 4
        g.magic_circle(fx, y + h - 27, min(half / 2 - 6, 70), t, CYAN, 0.8, STANDARD, squash=0.28)
        player(g, fx, fy, min(h - 40, 170) * 0.7)
        g.center("Julius_Novachrono", fx, y + 2, TEXT)
        g.center("Standing, arms crossed", fx, y + h - 20, TEXT_DIM)
        rx, rw = x + w // 2 + 8, w // 2 - 8
        cy = g.heading("Your magic", rx, y, rw - 6, GOLD)
        for kind, names in (("GRIMOIRE MAGIC", ["Time", "Fire", "Wind"]), ("SKILL", ["Great Sage", "Predator", "Wrath", "Mimicry"])):
            cy += 2
            g.text(kind, rx, cy, TEXT_DIM); cy += 12
            for nm in names:
                g.fill(rx, cy + 1, rx + rw - 8, cy + 16, with_alpha(0x0A1226, 0.55))
                g.fill(rx, cy + 2, rx + 2, cy + 15, with_alpha(CYAN, 0.95))
                if nm == "Wind" and hover_row:
                    g.hover(rx, cy + 1, rw - 8, 15, 1.0, CYAN)
                g.text(nm, rx + 8, cy + 3, TEXT)
                cy += 20
    elif page == 1:
        blocks = [("Magic Knight Rank", ["5th Class Junior Magic Knight"]), ("Stars", ["Gold 2   Black 0", "Net 2"]), ("Race", ["Human"]),
                  ("Social Class", ["Commoner"]), ("Grimoire", ["Four-leaf Clover", "Time Magic", "Pages 3/12  slots 6  mastery 25%"])]
        cols = [y, y]
        for i, (tt, ls) in enumerate(blocks):
            col = i % 2
            bxx = x if col == 0 else x + w // 2 + 8
            ny = g.heading(tt, bxx, cols[col], w // 2 - 8 - 6, GOLD)
            for l in ls:
                g.text(l, bxx + 2, ny, TEXT); ny += 10
            cols[col] = ny + 8
    elif page == 2:
        for col, items in enumerate((["Genesis Demon-Slayer", "Runeblade of Ruin"], ["Spirit Lance", "Gale Cutter"])):
            bxx = x if col == 0 else x + w // 2 + 8
            cy = y
            for nm in items:
                g.fill(bxx - 2, cy - 1, bxx + 18, cy + 17, with_alpha(0x0A1226, 0.55)); g.fill(bxx - 2, cy, bxx, cy + 16, with_alpha(CYAN, 0.95))
                g.fill(bxx + 2, cy + 2, bxx + 14, cy + 14, 0xFF8899AA)
                g.text(nm, bxx + 20, cy + 4, GOLD)
                g.fill(bxx, cy + 18, bxx + w // 2 - 14, cy + 19, with_alpha(CYAN, 0.43))
                g.text("Engraved: Severance, Holy Weapon", bxx + 2, cy + 22, TEXT)
                g.text("A blade that remembers every cut.", bxx + 2, cy + 32, TEXT)
                cy += 60
    else:
        tt = "Turning pages" if page == 6 else "Zagred, the Word-Eater"
        ny = g.heading(tt, x, y, w // 2 - 14, GOLD)
        g.text("Use the mouse wheel over the book,", x + 2, ny, TEXT)
        g.text("the arrow keys or A and D.", x + 2, ny + 10, TEXT)
    g.clip = None
    g.button(bx + bw - 94, by + bh + 4, 88, 16, "Grimoire Slot")
    return g


def status(t=120.0, typed=None):
    g = Gui()
    g.scrim(0.0)   # Screen.renderBackground(): vanilla dims; we approximate with the same scrim
    g.scrim(0.7)
    g.magic_circle(W / 2, H / 2, min(W, H) * 0.5, t, VIOLET, 0.20, CALM)
    g.center("MULTIVERSE STATUS", W // 2, 10, GOLD)
    g.fill(W // 2 - 52, 21, W // 2 + 52, 22, with_alpha(CYAN, 0.3)); g.fill(W // 2 - 14, 21, W // 2 + 14, 22, with_alpha(CYAN, 0.95))
    blocks = [("Magic Knight Rank", ["5th Class Junior Magic Knight"], 0, None, False), ("Stars", ["Gold 2   Black 0", "Net 2"], 0, None, False),
              ("Squad", ["Requires a Magic Knight rank"], 0, None, True), ("Social Class", ["Commoner"], 0, None, False),
              ("Race", ["Human"], 0, None, False),
              ("Grimoire", ["Four-leaf Clover", "Time Magic", "Pages 3/12  slots 6  mastery 25%"], 0.25, VIOLET, False),
              ("Mana", ["Stage 2  |  Fire", "Spells cost 80%, hit for 120%", "Mana density Rich"], 0.45, RED, False),
              ("Spirit Lord", ["Sylph  trust 72/100", "Energy 88%", "Not incarnated"], 0.72, GREEN, False),
              ("Anti-Magic", ["-"], 0, None, False), ("Ceremony", ["Accepted at Clover Kingdom", "Ceremony closed"], 0, None, False)]
    cols, gap = 3, 6
    pw, ph = min(160, (W - 20 - gap * (cols - 1)) // cols), 56
    x0, y0 = (W - (pw * cols + gap * (cols - 1))) // 2, 26
    for i, (tt, ls, fr, rgb, warn) in enumerate(blocks):
        x, y = x0 + (i % cols) * (pw + gap), y0 + (i // cols) * (ph + gap)
        g.panel(x, y, pw, ph, RED if warn else CYAN, 0.9 if warn else 0.5)
        g.heading(tt, x + 6, y + 4, pw - 12, RED if warn else GOLD)
        ly = y + 18
        for l in ls:
            if ly + 9 > y + ph - 6:
                break
            g.text(l[: (pw - 12) // 6], x + 6, ly, RED if warn else TEXT); ly += 10
        if rgb:
            g.bar(x + 6, y + ph - 7, pw - 12, 3, fr, rgb, t)
    g.button(W // 2 - 40, H - 26, 80, 18, "Back"); g.button(W - 96, H - 26, 88, 18, "Grimoire Slot")
    bottom = y0 + ((len(blocks) + cols - 1) // cols) * (ph + gap)
    sy = max(bottom + 2, H - 38)
    if sy + 40 <= H - 3:
        g.sage(10, sy, "Analysis complete.", "Warning: a record needs your attention.", "Result: 5th Class Junior Magic Knight", W // 2 - 56, typed)
    else:
        g.text(("Analysis complete. Result: 5th Class Junior Magic Knight")[: max(60, W // 2 - 80) // 6], 10, 10, SAGE_HEAD)
    return g


def four_kingdoms(t=120.0):
    g = Gui()
    g.scrim(0.8)
    PW, PH, PAGE_W, PAGE_H = 340, 236, 138, 160
    left, top = (W - PW) // 2, (H - PH) // 2
    g.magic_circle(left + PW / 2, top + PH / 2, PH * 0.62, t, VIOLET, 0.20, CALM)
    g.panel_scan(left, top, PW, PH, CYAN, 0.9, 1.0, t)
    bookX, bookY = left + PW // 2, top + 62
    g.fill(left + 8, top + 6, left + 56, top + 54, with_alpha(GOLD, 0.35))
    g.text("FK", left + 24, top + 24, GOLD, True)
    g.text("Four Kingdoms", left + 62, top + 12, GOLD, True, True)
    g.text("Tensura", left + 62, top + 24, CYAN, shadow=True)
    g.text("Time Magic  ·  Four-leaf Clover  ·  3/12 pages  ·  mastery 25%", left + 62, top + 38, TEXT)
    g.bar(left + 62, top + 50, 150, 4, 0.25, VIOLET, t)
    g.button(left + PW - 64, top + 8, 56, 16, "Status")
    g.fill(bookX - PAGE_W - 4, bookY - 3, bookX + PAGE_W + 4, bookY + PAGE_H + 3, with_alpha(0x03050B, 0.85))
    g.outline(bookX - PAGE_W - 4, bookY - 3, PAGE_W * 2 + 8, PAGE_H + 6, with_alpha(CYAN, 0.55))
    g.brackets(bookX - PAGE_W - 4, bookY - 3, PAGE_W * 2 + 8, PAGE_H + 6, 8, with_alpha(GOLD, 0.9))
    pages = [("Time Stop", 12, 8, "Halts the flow of time around the caster for a few heartbeats; foes freeze in place."),
             ("Chrono Rewind", 20, 18, "Rewinds your body to where it stood five seconds ago.")]
    for idx, right in ((0, False), (1, True)):
        x0 = 0 if right else -PAGE_W
        px0 = bookX + x0
        g.gradient(px0, bookY, px0 + PAGE_W, bookY + PAGE_H, with_alpha(0x131E38, 0.80), with_alpha(0x090E1C, 0.84))
        g.gradient(px0, bookY, px0 + PAGE_W, bookY + 24, with_alpha(0xFFFFFF, 0.05), 0x00FFFFFF)
        g.magic_circle(px0 + PAGE_W / 2, bookY + PAGE_H / 2, 60, t, CYAN, 0.14, CALM)
        nm, cost, cd, desc = pages[idx]
        tx, ty = px0 + 8, bookY + 8
        g.text(nm, tx, ty + 2, GOLD, True)
        g.text(f"Page {idx + 1}  ·  {cost}% MP  ·  {cd}s", tx, ty + 13, TEXT_DIM)
        g.bar(tx, ty + 27, PAGE_W - 16, 3, cost / 100.0, CYAN, t)
        yy = ty + 36
        words, line = desc.split(), ""
        for wd in words:
            if len(line + " " + wd) * 6 > PAGE_W - 16:
                g.text(line, tx, yy, TEXT); yy += 9; line = wd
            else:
                line = (line + " " + wd).strip()
        g.text(line, tx, yy, TEXT)
        g.center(str(idx + 1), px0 + PAGE_W // 2, bookY + PAGE_H - 18, TEXT_DIM)
    g.gradient(bookX - 1, bookY - 2, bookX + 1, bookY + PAGE_H + 2, with_alpha(CYAN, 0.95), with_alpha(VIOLET, 0.5))
    g.center("1 / 1", bookX, bookY + PAGE_H + 7, TEXT_DIM)
    g.button(bookX - PAGE_W - 2, bookY + PAGE_H + 4, 20, 14, "<"); g.button(bookX + PAGE_W - 18, bookY + PAGE_H + 4, 20, 14, ">")
    return g


def slot(t=120.0, bound=True, typed=None):
    g = Gui()
    g.scrim(0.7)
    iw, ih = 176, 64 + 58 + 24
    x, y = (W - iw) // 2, (H - ih) // 2 - 12
    g.panel_scan(x, y, iw, ih, CYAN, 0.8, 1.0, t)
    cx, cy = x + 80 + 8, y + 26 + 8
    g.clip = (int((x + 1) * g.s), int((y + 1) * g.s), int((x + iw - 1) * g.s), int((y + ih - 1) * g.s))
    g.magic_circle(cx, cy, 34, t, GOLD, 0.65 if bound else 0.35)
    g.clip = None
    g.text("Grimoire Slot", x + 8, y + 6, GOLD)
    g.text("Inventory", x + 8, y + 64 - 11, TEXT_DIM)
    sx, sy = x + 80 - 1, y + 26 - 1
    g.brackets(sx - 4, sy - 4, 26, 26, 6, with_alpha(GOLD, 0.85)); g.well(sx - 1, sy - 1, 20, 20, GOLD)
    if bound:
        g.fill(sx + 3, sy + 2, sx + 15, sy + 16, 0xFF2B7A4B)                       # the book
    for row in range(3):
        for col in range(9):
            g.well(x + 8 + col * 18 - 1 + 1 - 1, y + 64 + row * 18 - 1, 18, 18, CYAN, 0.7)
    for col in range(9):
        g.well(x + 8 + col * 18 - 1 + 1 - 1, y + 64 + 58 - 1, 18, 18, CYAN, 0.7)
    head, body, result = ("Analysis complete.", "Grimoire bound. Its pages open with mastery.", "Result: 3 of 12 pages open") if bound else \
        ("Analyzing...", "Notice: no grimoire is bound. Place one in the slot.", "")
    lines = 1 + (2 if len(body) * 6 > iw - 16 else 1) + (1 if result else 0)
    ph = lines * 10 + 10
    g.panel(x, y + ih + 5, iw, ph, CYAN, 0.5)
    g.sage(x + 8, y + ih + 10, head, body, result, iw - 16, typed)
    return g


def questionnaire(t=120.0):
    g = Gui()
    g.scrim(0.4)
    w, top, bottom = min(360, W - 20), H // 2 - 100, H // 2 + 90
    x = (W - w) // 2
    g.panel_scan(x - 4, top - 4, w + 8, bottom - top + 8, CYAN, 0.9, 1.0, t)
    g.clip = (int((x - 4) * g.s), int((top - 4) * g.s), int((x + w + 4) * g.s), int((bottom + 4) * g.s))
    g.magic_circle(W / 2, (top + bottom) / 2, 130, t, VIOLET, 0.28, CALM)
    g.clip = None
    g.center("The Tower Wizard asks  (3 / 10)", W // 2, top + 6, GOLD)
    g.bar(x + 12, top + 17, w - 24, 3, 0.2, VIOLET, t)
    g.text("A stranger on the road asks for your water.", x + 12, top + 22, TEXT)
    g.text("What do you do?", x + 12, top + 32, TEXT)
    for i, o in enumerate(["I share all of it.", "I share half and ask for news.", "I keep it. The road is long.", "I ask what they offer."]):
        g.button(x, H // 2 - 20 + i * 24, w, 20, o, 1.0 if i == 1 else 0.0)
    g.text("Notice: choose the answer that is closest to you.", x + 12, H // 2 + 79, SAGE)
    return g


def toast(age, t=160.0, typed=None):
    """The page-unlock toast at 'age' ticks."""
    IN, HOLD, OUT, BUILD = 14.0, 110.0, 18.0, 26.0

    def ease_out(v):
        v = max(0.0, min(1.0, v)); u = 1 - v
        return 1 - u * u * u

    def smooth(v):
        v = max(0.0, min(1.0, v)); return v * v * (3 - 2 * v)

    def back(v):
        v = max(0.0, min(1.0, v)); c1 = 1.70158; c3 = c1 + 1; u = v - 1
        return 1 + c3 * u ** 3 + c1 * u * u
    g = Gui()
    wW = 300
    desc = "Halts the flow of time around the caster for a few heartbeats; foes freeze in place until it ends."
    tw = wW - 80
    per = tw // 6
    words, lines, line = desc.split(), [], ""
    for wd in words:
        if len((line + " " + wd).strip()) <= per:
            line = (line + " " + wd).strip()
        else:
            lines.append(line); line = wd
    lines.append(line)
    if len(lines) > 2:                                     # SageLayout.clamp(text, metrics, width, 2)
        last = lines[1]
        while len(last + "...") > per:
            last = last[:-1]
        desc = lines[0] + " " + last.rstrip() + "..."
    nlines = 1 + min(2, len(lines)) + 1
    hH = max(62, nlines * 10 + 35)
    alpha = max(0.0, min(1.0, age / (IN * 0.6))) * (1 - smooth((age - IN - HOLD) / OUT))
    x = (W - wW) // 2
    y = 8 - int(round((1 - ease_out(age / IN)) * (hH + 16)))
    g.panel_scan(x, y, wW, hH, CYAN, 1.0, alpha, t)
    g.magic_circle(x + 34, y + hH / 2, 29, t, CYAN, alpha, STANDARD, back(age / BUILD))
    tx, tw = x + 72, wW - 80
    g.text("Time Stop", tx, y + 6, scale_alpha(GOLD, alpha), True, True)
    g.sage(tx, y + 19, "Notice: a new page has opened.", desc, "Result: cost 12% MP, cooldown 8 s", tw, typed, alpha)
    label = "Mastery 25%  |  Pages 3/12"
    lw = len(label) * 6
    g.bar(tx, y + hH - 11, tw - lw - 6, 4, 0.25, VIOLET, t, alpha)
    g.text(label, tx + tw - lw, y + hH - 13, scale_alpha(TEXT_DIM, alpha))
    return g


def tensura_select(t=120.0, typed=None):
    """The overlay on Tensura's ability-selection window (a dark stand-in panel for Tensura's own window)."""
    g = Gui()
    g.scrim(0.6)
    bx0, by0, bx1, by1 = (W - 256) // 2, 20, (W + 256) // 2, 170
    g.fill(bx0, by0, bx1, by1, 0xE0141824); g.outline(bx0, by0, bx1 - bx0, by1 - by0, 0xFF3A4260)
    g.text("Skills", bx0 + 8, by0 + 6, 0xFFD0D0E0)
    for i, nm in enumerate(["Great Sage", "Predator", "Wrath", "Mimicry"]):
        g.fill(bx0 + 8, by0 + 20 + i * 22, bx0 + 150, by0 + 38 + i * 22, 0xFF22283A if i != 0 else 0xFF2E3A5A)
        g.text(nm, bx0 + 14, by0 + 25 + i * 22, 0xFFE8E8F0)
    for i in range(3):
        g.fill(bx0 + 170, by0 + 28 + i * 30, bx0 + 200, by0 + 58 + i * 30 - 4, 0xFF22283A)
    cx, cy = (bx0 + bx1) / 2, (by0 + by1) / 2
    g.magic_circle(cx, cy, max(40.0, min(bx1 - bx0, by1 - by0) * 0.62), t, CYAN, 0.12, CALM)
    g.brackets(bx0 - 3, by0 - 3, bx1 - bx0 + 6, by1 - by0 + 6, 9, with_alpha(CYAN, 0.75))
    g.sweep(bx0, by0, bx1 - bx0, by1 - by0, t, CYAN, 0.7)
    rw = min(280, max(160, bx1 - bx0))
    tw = rw - 14
    rx, ry = int(cx - rw / 2), by1 + 4
    rh = 12 + 30 + 8
    g.panel(rx, ry, rw, rh, CYAN, 0.7)
    ny = g.sage(rx + 7, ry + 6, "Analysis complete.", "Great Sage", "Result: mastery 62 / 100", tw, typed)
    g.bar(rx + 7, ny + 1, tw, 4, 0.62, VIOLET, t)
    return g


def relic(t=120.0):
    g = Gui()
    g.scrim(0.5)
    x, y, w, h = 150, 40, 200, 140
    grade = 0xFFE8A030
    g.gradient(x, y, x + w, y + h, 0xF2101A30, 0xF2070B16)
    g.outline(x - 1, y - 1, w + 2, h + 2, grade)
    g.outline(x, y, w, h, mix(grade, 0xFF3A2A8A, 0.6))
    g.text("Ring of the Hollow Star", x + 6, y + 6, grade)
    g.text("*** Legendary   Slot: Ring (Curios)", x + 6, y + 18, grade)
    g.fill(x + 6, y + 27, x + w - 6, y + 28, with_alpha(grade, 0.45))
    g.text("Attuned to you", x + 6, y + 30, 0xFF55FFFF)
    g.text("The star keeps a hollow where light once was.", x + 6, y + 46, 0xFFC8C8D0)
    g.text("> Starfall", x + 6, y + 66, 0xFFFFD36B)
    g.text("Key V. Mana 20%. Cooldown 40s.", x + 14, y + 76, 0xFFA0A6B2)
    g.text("+10% Spell Power", x + 6, y + 96, 0xFF7CE08A)
    g.text("Notice: page 1 of 2. Scroll to turn.", x + 6, y + 122, SAGE_HEAD)
    # the HUD chip
    line = "[V] Hollow Star > Starfall   (1/2, shift: next)"
    wl = len(line) * 6
    hx, hy = W - wl - 8, H - 14
    g.panel(hx - 4, hy - 3, wl + 8, 14, grade, 0.45)
    g.text(line, hx, hy, grade, shadow=True)
    return g


# ---------------------------------------------------------------------------------------------------------------------
# Multiverse Hub (docs/ui_hub.md). PIL MOCK-UPS, not game screenshots: the stage is the real 400 x 240 stage of HubLayout, the node, panel and weapon-
# column numbers below were dumped from HubLayout / HubWeaponLayout (client/hub), text width is approximated at 6 px per character (Minecraft's font is
# narrower on average, so a cut here is a worst case), and no sound or motion is shown. Run:  python tools/ui_preview/preview_ui.py --hub docs/previews/ui_hub
# ---------------------------------------------------------------------------------------------------------------------
HUB_W = 400
HUB_HOME = [(200.0, 40.0), (253.74011, 62.259884), (276.0, 116.0), (253.74011, 169.74011), (200.0, 192.0), (146.25989, 169.74011), (124.0, 116.0), (146.25989, 62.259884)]
HUB_RAIL = [(66.0, 82.0), (99.941124, 96.058876), (114.0, 130.0), (99.941124, 163.94113), (66.0, 178.0), (32.058876, 163.94113), (18.0, 130.0), (32.058876, 96.058876)]
HUB_SIDES = [0, 1, 1, 1, 3, 2, 2, 2]
HUB_PANEL, HUB_TITLE, HUB_BODY, HUB_LINE = (132, 26, 264, 186), (140, 32, 244, 10), (140, 48, 244, 160), (14, 226, 372, 9)
HUB_HOME_C, HUB_HOME_R, HUB_HOME_NODE = (200, 116), 76, 17
HUB_RAIL_C, HUB_RAIL_R, HUB_RAIL_NODE = (66, 130), 48, 11
# key, glyph, rgb, label, hover description
HUB_TABS = [("status", "St", 0x5CD8FF, "Status", "Your power, your standing and your active effects."),
            ("weapons", "Wp", 0xE8C468, "Weapons", "Your bound weapons: forms, upgrades and mastery."),
            ("skills", "Sk", 0x9A7CFF, "Skills", "Opens the skill screen."),
            ("quests", "Qd", 0x7CF0B0, "Quests", "Quest tracker and the last dungeon map."),
            ("attunement", "At", 0xB39CF0, "Attunement", "Opens attunement (meditation)."),
            ("multiverse", "Mv", 0xFF9F6A, "Multiverse", "Four Kingdoms, Beast Will, compat codex, magic."),
            ("codex", "Cx", 0x8FD0FF, "Codex", "Opens the old Codex book."),
            ("settings", "Se", 0xC0C8D8, "Settings", "Hub options, GUI size and GUI move.")]
HUB_FOOTER = "Click a node. Esc closes. Backspace goes back to the ring."
# the weapon column (HubWeaponLayout.of(244, 3 chips, 4 options, 3 bars, 2 extras)): x, y, w, h
WL_CHIPS = [(0, 0, 79, 14), (82, 0, 79, 14), (164, 0, 79, 14)]
WL_HEAD, WL_OH, WL_OPTS = (0, 20, 244, 46), 72, [(0, 85, 244, 13), (0, 99, 244, 13), (0, 113, 244, 13), (0, 127, 244, 13)]
WL_BH, WL_BARS, WL_EH, WL_EXTRAS, WL_HEIGHT = 147, [(0, 160, 244, 17), (0, 180, 244, 17), (0, 200, 244, 17)], 226, [(0, 239, 244, 17), (0, 259, 244, 17)], 285


def _smooth(v):
    v = max(0.0, min(1.0, v))
    return v * v * (3 - 2 * v)


def _back(v):
    v = max(0.0, min(1.0, v))
    c1 = 1.70158
    return 1 + (c1 + 1) * (v - 1) ** 3 + c1 * (v - 1) ** 2


def hub_fit(s_, max_w):
    """HubText.fit at 6 px per character: (shown, cut)."""
    if len(s_) * 6 <= max_w:
        return s_, False
    n = max(0, (max_w - 18) // 6)
    return s_[:n] + "...", True


def hub_octagon(g, cx, cy, r, argb):
    c = max(2, r // 3)
    g.fill(cx - r + c, cy - r, cx + r - c, cy - r + c, argb)
    g.fill(cx - r, cy - r + c, cx + r, cy + r - c, argb)
    g.fill(cx - r + c, cy + r - c, cx + r - c, cy + r, argb)


def hub_octagon_edge(g, cx, cy, r, argb):
    c = max(2, r // 3)
    x0, y0, x1, y1 = cx - r, cy - r, cx + r, cy + r
    g.fill(x0 + c, y0, x1 - c, y0 + 1, argb); g.fill(x0 + c, y1 - 1, x1 - c, y1, argb)
    g.fill(x0, y0 + c, x0 + 1, y1 - c, argb); g.fill(x1 - 1, y0 + c, x1, y1 - c, argb)
    for k in range(0, c, 2):
        g.fill(x0 + c - k - 2, y0 + k, x0 + c - k, y0 + k + 2, argb); g.fill(x1 - c + k, y0 + k, x1 - c + k + 2, y0 + k + 2, argb)
        g.fill(x0 + c - k - 2, y1 - k - 2, x0 + c - k, y1 - k, argb); g.fill(x1 - c + k, y1 - k - 2, x1 - c + k + 2, y1 - k, argb)


def hub_lock(g, cx, cy, argb):
    g.fill(cx - 2, cy - 6, cx + 3, cy - 5, argb); g.fill(cx - 3, cy - 5, cx - 2, cy - 1, argb); g.fill(cx + 3, cy - 5, cx + 4, cy - 1, argb)
    g.fill(cx - 4, cy - 1, cx + 5, cy + 5, argb); g.fill(cx, cy + 1, cx + 1, cy + 3, 0xCC0A1024)


def hub_node(g, cx, cy, r, tab, lit=1.0, hover=0.0, active=False, locked=False, alpha=1.0):
    rgb = tab[2]
    vis = max(0.0, min(1.0, lit))
    if vis <= 0.02:
        hub_octagon_edge(g, cx, cy, r, with_alpha(rgb, 0.18 * alpha))
        return
    dim = 0.55 if locked else 1.0
    body = with_alpha(mix(0xFF0A1024, 0xFF000000 | rgb, 0.12 + 0.22 * hover + (0.16 if active else 0.0)), (0.55 + 0.35 * vis) * alpha)
    hub_octagon(g, cx, cy, r, body)
    hub_octagon_edge(g, cx, cy, r, with_alpha(rgb, min(1.0, (0.4 + 0.45 * vis + 0.25 * hover + (0.3 if active else 0.0)) * dim * alpha)))
    if active:
        hub_octagon_edge(g, cx, cy, r + 2, with_alpha(0xFFFFFF, 0.35 * alpha))
    if not locked and vis * alpha * dim > 0.08:
        g.center(tab[1], cx, cy - 4, with_alpha(mix(0xFF000000 | rgb, 0xFFFFFFFF, 0.6), vis * alpha * dim))
    if locked:
        hub_lock(g, cx, cy, with_alpha(rgb, 0.85 * vis * alpha))


def hub_ring(g, cx, cy, r, n, rot, rgb, alpha, reveal, big_every):
    if alpha <= 0.01 or reveal <= 0:
        return
    shown = min(n, int(math.ceil(n * min(1.0, reveal))))
    for i in range(shown):
        a = i * 2 * math.pi / n + rot
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        big = i % big_every == 0
        sz = 3 if big else 2
        px, py = int(round(x - sz / 2)), int(round(y - sz / 2))
        g.fill(px, py, px + sz, py + sz, with_alpha(rgb, min(1.0, 0.95 * alpha) if big else 0.5 * alpha))


def hub_spoke(g, x0, y0, x1, y1, rgb, alpha, reveal):
    if alpha <= 0.01 or reveal <= 0:
        return
    dx, dy = x1 - x0, y1 - y0
    ln = math.hypot(dx, dy)
    n = max(1, int(ln / 5))
    shown = min(n, int(math.ceil(n * min(1.0, reveal))))
    for i in range(1, shown + 1):
        t = i / (n + 1)
        px, py = int(round(x0 + dx * t)) - 1, int(round(y0 + dy * t)) - 1
        g.fill(px, py, px + 2, py + 2, with_alpha(rgb, 0.6 * alpha))


def hub_motes(g, t, alpha):
    rng = np.random.RandomState(0xB10C)
    for i in range(24):
        x, y, ph, sz = rng.uniform(4, 396), rng.uniform(4, 236), rng.uniform(0, 1), 1 + (i % 3 == 0)
        tw = 0.5 + 0.5 * math.sin((t / 70.0 + ph) * 2 * math.pi)
        g.fill(int(x), int(y), int(x) + sz, int(y) + sz, with_alpha(CYAN, min(1.0, (0.12 + 0.5 * tw) * alpha)))


def hub_new(scrim=True):
    """A Gui the size of the stage (400 x 240)."""
    global W
    keep, W = W, HUB_W
    try:
        g = Gui()
    finally:
        W = keep
    g.w = HUB_W
    if scrim:
        g.gradient(0, 0, HUB_W, H, with_alpha(0x050812, 0.62), with_alpha(0x020409, 0.82))
    return g


def hub_line(g, text, color=TEXT_DIM, alpha=1.0):
    shown, cut = hub_fit(text, HUB_LINE[2])
    g.center(shown, HUB_LINE[0] + HUB_LINE[2] // 2, HUB_LINE[1], scale_alpha(color, alpha))
    return cut


def hub_home(t=220.0, age=None, hover=-1, locked=(1,)):
    """The ring. age = intro age in ticks (None: the finished ring)."""
    g = hub_new()
    cx, cy = HUB_HOME_C
    fin = age is None
    a = 1e6 if fin else age
    rings, core, labels = _smooth((a - 14) / 40.0), _back(a / 30.0), _smooth((a - 96) / 24.0)
    hub_motes(g, t, 0.8 * rings)
    g.magic_circle(cx, cy, 100, t, CYAN, 0.2, CALM, max(0.01, rings))
    hub_ring(g, cx, cy, HUB_HOME_R, 72, t * 0.003, CYAN, 0.5, rings, 6)
    hub_ring(g, cx, cy, HUB_HOME_R - 24, 48, -t * 0.004, VIOLET, 0.35, rings, 4)
    for i, tab in enumerate(HUB_TABS):
        nx, ny = HUB_HOME[i]
        dx, dy = nx - cx, ny - cy
        ln = math.hypot(dx, dy)
        ux, uy = dx / ln, dy / ln
        lit = _smooth((a - 40 - i * 9) / 10.0)
        hub_spoke(g, cx + ux * 34, cy + uy * 34, nx - ux * (HUB_HOME_NODE + 3), ny - uy * (HUB_HOME_NODE + 3), 0xFF000000 | tab[2], 1.0, lit)
    g.magic_circle(cx, cy, 52, t, VIOLET, 0.85, STANDARD, max(0.01, core))
    if labels > 0.05:
        g.fill(cx - 24, cy - 24, cx + 24, cy + 24, with_alpha(0x0A1024, 0.6 * labels))
        g.center("MULTI", cx, cy - 10, scale_alpha(GOLD, labels)); g.center("VERSE", cx, cy + 1, scale_alpha(CYAN, labels))
    for i, tab in enumerate(HUB_TABS):
        nx, ny = HUB_HOME[i]
        hv = 1.0 if i == hover else 0.0
        if hv > 0:
            g.magic_circle(nx, ny, HUB_HOME_NODE * 1.9, t, 0xFF000000 | tab[2], 0.9, FAST, 1.0)
        hub_node(g, int(round(nx)), int(round(ny)), HUB_HOME_NODE, tab, _smooth((a - 40 - i * 9) / 10.0), hv, False, i in locked)
        if labels > 0.05:
            shown, _ = hub_fit(tab[3], 92)
            w = len(shown) * 6
            side = HUB_SIDES[i]
            ix, iy = int(round(nx)), int(round(ny))
            lx, ly = {0: (ix - w // 2, iy - HUB_HOME_NODE - 4 - 9), 1: (ix + HUB_HOME_NODE + 4, iy - 4), 2: (ix - HUB_HOME_NODE - 4 - w, iy - 4), 3: (ix - w // 2, iy + HUB_HOME_NODE + 4)}[side]
            g.text(shown, lx, ly, scale_alpha(mix(TEXT_FAINT if i in locked else TEXT, GOLD, hv), labels))
    if labels > 0.05:
        if hover >= 0:
            tab = HUB_TABS[hover]
            hub_line(g, tab[3] + ": " + (("Bind a hero, Lord or Greed weapon to unlock this page." if tab[0] == "weapons" else "Locked.") if hover in locked else tab[4]), TEXT, labels)
        else:
            hub_line(g, HUB_FOOTER, TEXT_DIM, labels)
    return g


def hub_frame(g, tab_index, t, scroll=0.0, content=None, content_h=0, hover_node=-1):
    """The TAB view: the rail on the left, the page panel on the right; content(g, ox, oy) draws the body in local coordinates."""
    tab = HUB_TABS[tab_index]
    rcx, rcy = HUB_RAIL_C
    g.magic_circle(rcx, rcy, 52, t, CYAN, 0.2, CALM, 1.0)
    hub_ring(g, rcx, rcy, HUB_RAIL_R, 72, t * 0.003, CYAN, 0.5, 1.0, 6)
    hub_octagon(g, rcx, rcy, 15, with_alpha(0x0A1024, 0.8)); hub_octagon_edge(g, rcx, rcy, 15, with_alpha(GOLD, 0.55))
    g.center("VERSE", rcx, rcy - 4, GOLD)
    for i, tb in enumerate(HUB_TABS):
        nx, ny = HUB_RAIL[i]
        hv = 1.0 if i == hover_node else 0.0
        if hv > 0:
            g.magic_circle(nx, ny, HUB_RAIL_NODE * 1.9, t, 0xFF000000 | tb[2], 0.9, FAST, 1.0)
        hub_node(g, int(round(nx)), int(round(ny)), HUB_RAIL_NODE, tb, 1.0, hv, i == tab_index, False)
    g.text("MULTIVERSE HUB", 8, 8, with_alpha(GOLD, 0.8))
    px, py, pw, ph = HUB_PANEL
    g.panel_scan(px, py, pw, ph, 0xFF000000 | tab[2], 0.7, 1.0, t)
    g.heading(tab[3], HUB_TITLE[0], HUB_TITLE[1], HUB_TITLE[2], tab[2])
    bx, by, bw, bh = HUB_BODY
    keep = g.clip
    s = g.s
    g.clip = (bx * s, by * s, (bx + bw) * s, (by + bh) * s)
    if content:
        content(g, bx, by - int(round(scroll)), scroll)
    g.clip = keep
    mx = content_h - bh
    if mx > 0:
        bar = max(12, bh * bh // (mx + bh))
        pos = int((bh - bar) * (scroll / mx))
        g.fill(387, by, 390, by + bh, with_alpha(0xFFFFFF, 0.08)); g.fill(387, by + pos, 390, by + pos + bar, with_alpha(tab[2], 0.85))


def hub_weapons(t=300.0, scroll=0.0, hover_chip=-1):
    g = hub_new()
    hub_motes(g, t, 0.8)
    rgbs = [0xE8C468, 0x9A7CFF, 0xFF6A6A]
    chips = ["Demon-Slayer", "Lord's Edge", "Greed Blade of the Hollow"]
    opts = [("Reforge: Void Edge   (current form)", "current"), ("Awaken the Twin-Fang Shadow Form   3 cores", "ready"),
            ("Raise the grade to Mythic   needs level 20", "ready"), ("Soul bind: Heaven's Wrath   locked", "locked")]
    bars = [("Weapon level", "14 / 20", 0.7), ("Void mastery", "62%", 0.62), ("Edge synchronisation of the bearer", "31%", 0.31)]
    extras = [("Lord's Edge", "Lv 6", 0.3), ("Greed Blade of the Hollow", "Lv 2", 0.1)]
    rgb = rgbs[0]

    def body(g, ox, oy, scroll):
        def R(r):
            return (ox + r[0], oy + r[1], r[2], r[3])
        for i, r in enumerate(WL_CHIPS):
            x, y, w, h = R(r)
            g.chip(x, y, w, h, i == 0, i == hover_chip, 0xFF000000 | rgbs[i])
            shown, _ = hub_fit(chips[i], w - 8)
            g.text(shown, x + 4, y + (h - 8) // 2, WHITE if i == 0 or i == hover_chip else TEXT)
        x, y, w, h = R(WL_HEAD)
        g.well(x, y, w, h, 0xFF000000 | rgb, 1.0)
        g.fill(x, y, x + 2, y + h, with_alpha(rgb, 0.9))
        g.text(hub_fit("Genesis Demon-Slayer", w - 12)[0], x + 7, y + 4, GOLD)
        g.text("Grade: Mythic", x + 7, y + 15, 0xFF000000 | rgb)
        g.text("Level 14 / 20", x + 7, y + 26, TEXT)
        g.text("Form: Void Blade", x + 7, y + 36, TEXT_DIM)
        g.heading("Upgrades", ox, oy + WL_OH, 244, GOLD)
        for (text, state), r in zip(opts, WL_OPTS):
            x, y, w, h = R(r)
            g.chip(x, y, w, h, state == "current", False, 0xFF000000 | rgb)
            shown, _ = hub_fit(text, w - 10)
            g.text(shown, x + 5, y + (h - 8) // 2, TEXT_FAINT if state == "locked" else TEXT)
        g.heading("Mastery", ox, oy + WL_BH, 244, CYAN)
        for (label, value, fr), r in zip(bars, WL_BARS):
            x, y, w, h = R(r)
            shown, _ = hub_fit(label, w - 8 - len(value) * 6 - 6)
            g.text(shown, x + 2, y, TEXT); g.text(value, x + w - 2 - len(value) * 6, y, TEXT_DIM)
            g.bar(x + 2, y + 11, w - 4, 4, fr, 0xFF000000 | rgb, t)
        g.heading("Other masteries", ox, oy + WL_EH, 244, VIOLET)
        for (label, value, fr), r in zip(extras, WL_EXTRAS):
            x, y, w, h = R(r)
            shown, _ = hub_fit(label, w - 8 - len(value) * 6 - 6)
            g.text(shown, x + 2, y, TEXT); g.text(value, x + w - 2 - len(value) * 6, y, TEXT_DIM)
            g.bar(x + 2, y + 11, w - 4, 4, fr, VIOLET, t)

    hub_frame(g, 1, t, scroll, body, WL_HEIGHT, hover_node=1)
    hub_line(g, "Weapons: Your bound weapons: forms, upgrades and mastery.", TEXT)
    return g


def hub_status(t=300.0):
    g = hub_new()
    hub_motes(g, t, 0.8)
    cards = [(CYAN, "Power", ["Existence points: 1,245,300", "Magicule: 88,200   Aura: 40,100"], -1),
             (GOLD, "Magic Knight Rank", ["5th Class Junior Magic Knight"], -1),
             (GOLD, "Stars", ["Gold 2   Black 0", "Net 2"], -1),
             (GOLD, "Grimoire", ["Four-leaf Clover", "Time Magic", "Pages 3/12  slots 6  mastery 25%"], 0.25),
             (RED, "Squad", ["Requires a Magic Knight rank"], -1),
             (VIOLET, "Active effects", ["Strength II   4:12", "Regeneration   endless", "Night Vision   9:40"], -1)]
    total = sum(4 + 11 + len(ls) * 10 + (8 if fr >= 0 else 0) + 3 + 4 for _, _, ls, fr in cards)

    def body(g, ox, oy, scroll):
        y = oy
        for rgb, title, lines, fr in cards:
            h = 4 + 11 + len(lines) * 10 + (8 if fr >= 0 else 0) + 3
            g.well(ox, y, 244, h, 0xFF000000 | rgb, 1.0)
            g.fill(ox, y, ox + 2, y + h, with_alpha(rgb, 0.9))
            g.text(hub_fit(title, 232)[0], ox + 6, y + 4, 0xFF000000 | rgb)
            ly = y + 15
            for l in lines:
                g.text(hub_fit(l, 232)[0], ox + 6, ly, TEXT)
                ly += 10
            if fr >= 0:
                g.bar(ox + 6, ly + 1, 232, 4, fr, 0xFF000000 | rgb, t)
            y += h + 4

    hub_frame(g, 0, t, 0.0, body, total, hover_node=-1)
    hub_line(g, HUB_FOOTER, TEXT_DIM)
    return g


# ---------------------------------------------------------------- Weapon page v2 (docs/ui_weapon_page.md): the longer "Other masteries" list and the Status-screen tab. PIL MOCK-UPS, not game screenshots.
def wl_layout(w, chips, options, bars, extras):
    """A port of HubWeaponLayout (pure integer arithmetic): returns the rectangles and the content height."""
    CHIP_H, CHIP_GAP, MIN_CHIP_W, MAX_CHIP_W, HEAD_H, ROW_H, BAR_H, HEADING_H, GAP = 14, 3, 44, 96, 46, 14, 20, 13, 6
    w = max(MIN_CHIP_W, w)
    has = chips > 1
    per = max(1, min(max(1, chips), (w + CHIP_GAP) // (MIN_CHIP_W + CHIP_GAP)))
    rows = (chips + per - 1) // per if has else 0
    cw = max(MIN_CHIP_W, min(MAX_CHIP_W, (w - CHIP_GAP * (per - 1)) // per))
    chips_h = rows * CHIP_H + (rows - 1) * CHIP_GAP if has else 0
    L = {"chips": [((i % per) * (cw + CHIP_GAP), (i // per) * (CHIP_H + CHIP_GAP), cw, CHIP_H) for i in range(chips)] if has else []}
    head_y = chips_h + GAP if has else 0
    L["head"] = (0, head_y, w, HEAD_H)
    oh = head_y + HEAD_H + GAP
    L["oh"] = oh
    L["opts"] = [(0, oh + HEADING_H + i * ROW_H, w, ROW_H - 1) for i in range(options)]
    o_end = oh + HEADING_H + options * ROW_H if options > 0 else head_y + HEAD_H
    bh = o_end + GAP
    L["bh"] = bh
    L["bars"] = [(0, bh + HEADING_H + i * BAR_H, w, BAR_H - 3) for i in range(bars)]
    b_end = bh + HEADING_H + bars * BAR_H if bars > 0 else o_end
    eh = b_end + GAP
    L["eh"] = eh
    L["extras"] = [(0, eh + HEADING_H + i * BAR_H, w, BAR_H - 3) for i in range(extras)]
    L["height"] = (eh + HEADING_H + extras * BAR_H if extras > 0 else b_end) + GAP
    return L


V2_WEAPONS = ["Demon-Slayer", "Lord's Edge", "Black Sword"]
V2_OPTS = [("Reforge: Void Edge   (current form)", "current"), ("Awaken the Twin-Fang Shadow Form - learn", "ready"),
           ("Raise the grade to Mythic - locked", "locked")]
# the open weapon's own bars (the hero weapon: level, active form, two more learned forms) with the new "value (progress)" text
V2_BARS = [("Weapon level", "14/100 (40%)", 0.40), ("Void Edge mastery", "62/100 (62%)", 0.62), ("Twin-Fang mastery", "20/80 (25%)", 0.25), ("Shadow Step mastery", "9/60 (15%)", 0.15)]
# other masteries: Grimoire, Beast Will, Saiyan form, skills in training, then every OTHER weapon's bars
V2_EXTRAS = [("Grimoire mastery", "25%", 0.25, VIOLET), ("Rimuru (Beast Will)", "58%", 0.58, VIOLET), ("Super Saiyan (Saiyan form)", "33%", 0.33, VIOLET),
             ("Void Slash", "41/100 (41%)", 0.41, VIOLET), ("Flame Coat", "12/50 (24%)", 0.24, VIOLET),
             ("Lord's Edge: Lord's Edge mastery", "6/100 (30%)", 0.30, VIOLET), ("Black Sword: Mastery", "2/100 (10%)", 0.10, VIOLET),
             ("Black Sword: Black Sword tier", "3/8 (45%)", 0.45, VIOLET)]


def weapon_body(g, ox, oy, w, t, hover_chip=-1):
    L = wl_layout(w, len(V2_WEAPONS), len(V2_OPTS), len(V2_BARS), len(V2_EXTRAS))
    rgbs = [0xE8C468, 0x9A7CFF, 0xFF6A6A]
    rgb = rgbs[0]

    def R(r):
        return (ox + r[0], oy + r[1], r[2], r[3])
    for i, r in enumerate(L["chips"]):
        x, y, cw, ch = R(r)
        g.chip(x, y, cw, ch, i == 0, i == hover_chip, 0xFF000000 | rgbs[i])
        g.text(hub_fit(V2_WEAPONS[i], cw - 8)[0], x + 4, y + (ch - 8) // 2, WHITE if i == 0 or i == hover_chip else TEXT)
    x, y, hw, hh = R(L["head"])
    g.well(x, y, hw, hh, 0xFF000000 | rgb, 1.0)
    g.fill(x, y, x + 2, y + hh, with_alpha(rgb, 0.9))
    g.text("Genesis Demon-Slayer", x + 7, y + 4, GOLD)
    g.text("Grade: Mythic", x + 7, y + 15, 0xFF000000 | rgb)
    g.text("Level 14 / 100", x + 7, y + 26, TEXT)
    g.text("Form: Void Edge", x + 7, y + 36, TEXT_DIM)
    g.heading("Upgrades", ox, oy + L["oh"], w, GOLD)
    for (text, state), r in zip(V2_OPTS, L["opts"]):
        x, y, rw, rh = R(r)
        g.chip(x, y, rw, rh, state == "current", False, 0xFF000000 | rgb)
        g.text(hub_fit(text, rw - 10)[0], x + 5, y + (rh - 8) // 2, TEXT_FAINT if state == "locked" else TEXT)
    g.heading("Mastery", ox, oy + L["bh"], w, CYAN)
    for (label, value, fr), r in zip(V2_BARS, L["bars"]):
        x, y, bw, bh = R(r)
        g.text(hub_fit(label, bw - 8 - len(value) * 6 - 6)[0], x + 2, y, TEXT); g.text(value, x + bw - 2 - len(value) * 6, y, TEXT_DIM)
        g.bar(x + 2, y + 11, bw - 4, 4, fr, 0xFF000000 | rgb, t)
    g.heading("Other masteries", ox, oy + L["eh"], w, VIOLET)
    for (label, value, fr, col), r in zip(V2_EXTRAS, L["extras"]):
        x, y, bw, bh = R(r)
        g.text(hub_fit(label, bw - 8 - len(value) * 6 - 6)[0], x + 2, y, TEXT); g.text(value, x + bw - 2 - len(value) * 6, y, TEXT_DIM)
        g.bar(x + 2, y + 11, bw - 4, 4, fr, col, t)
    return L["height"]


def hub_weapons_v2(t=300.0, scroll=0.0):
    g = hub_new()
    hub_motes(g, t, 0.8)
    height = wl_layout(244, len(V2_WEAPONS), len(V2_OPTS), len(V2_BARS), len(V2_EXTRAS))["height"]
    scroll = min(scroll, max(0, height - HUB_BODY[3]))              # as the screen clamps it
    hub_frame(g, 1, t, scroll, lambda g, ox, oy, sc: weapon_body(g, ox, oy, 244, t), height, hover_node=1)
    hub_line(g, "Weapons: Your bound weapons: forms, upgrades and mastery.", TEXT)
    return g


def status_weapons(t=300.0, scroll=0.0):
    """MultiverseStatusScreen with the Weapon tab on (WeaponStatusPage.area: centred, at most 280 wide, y 26 to height-52)."""
    g = Gui()
    g.scrim(0.0)
    g.scrim(0.7)
    g.magic_circle(W / 2, H / 2, min(W, H) * 0.5, t, VIOLET, 0.20, CALM)
    g.center("Weapon", W // 2, 10, GOLD)
    aw = max(100, min(280, W - 24)); ah = max(40, H - 26 - 52); ax = (W - aw) // 2; ay = 26
    height = wl_layout(aw, len(V2_WEAPONS), len(V2_OPTS), len(V2_BARS), len(V2_EXTRAS))["height"]
    scroll = min(scroll, max(0, height - ah))
    keep = g.clip
    g.clip = (ax * g.s, ay * g.s, (ax + aw) * g.s, (ay + ah) * g.s)
    weapon_body(g, ax, ay - int(round(scroll)), aw, t)
    g.clip = keep
    mx = height - ah
    if mx > 0:
        bar = max(12, ah * ah // (mx + ah)); pos = int((ah - bar) * (scroll / mx))
        g.fill(ax + aw + 2, ay, ax + aw + 4, ay + ah, with_alpha(0xFFFFFF, 0.08)); g.fill(ax + aw + 2, ay + pos, ax + aw + 4, ay + pos + bar, with_alpha(GOLD, 0.85))
    wide = W >= 450
    ty = H - 26 if wide else H - 46
    g.button(8, H - 26, 88, 18, "GUI Size"); g.button(W // 2 - 40, H - 26, 80, 18, "Back"); g.button(W - 96, H - 26, 88, 18, "Grimoire Slot")
    g.button(104, ty, 80, 18, "Beast Will")
    g.button(max(188, W - 184), ty, 80, 18, "Status", 1.0)
    return g


def main():
    global S
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    out = args[0] if args else "."
    if "--scale" in sys.argv:
        S = int(sys.argv[sys.argv.index("--scale") + 1])
    os.makedirs(out, exist_ok=True)
    jobs = {
        "codex_you.png": lambda: codex(0, 120), "codex_status.png": lambda: codex(1, 300), "codex_weapons.png": lambda: codex(2, 500),
        "codex_guide.png": lambda: codex(6, 700), "status.png": lambda: status(200, None), "four_kingdoms.png": lambda: four_kingdoms(300),
        "grimoire_slot.png": lambda: slot(100), "grimoire_slot_empty.png": lambda: slot(100, False), "questionnaire.png": lambda: questionnaire(400),
        "toast_in.png": lambda: toast(6, 100, 0), "toast_mid.png": lambda: toast(34, 130, 38), "toast_full.png": lambda: toast(70, 160, None),
        "toast_out.png": lambda: toast(ToastOut, 200, None), "tensura_select.png": lambda: tensura_select(250), "relic_tooltip.png": lambda: relic(120),
    }
    hub_jobs = {
        "hub_ring.png": lambda: hub_home(220, None, hover=2), "hub_intro.png": lambda: hub_home(70, 70), "hub_intro_start.png": lambda: hub_home(10, 10),
        "hub_weapons.png": lambda: hub_weapons(300, 0.0), "hub_weapons_scrolled.png": lambda: hub_weapons(300, WL_HEIGHT - HUB_BODY[3]), "hub_status.png": lambda: hub_status(300),
    }
    wp2_jobs = {
        "hub_weapons_v2.png": lambda: hub_weapons_v2(300, 0.0), "hub_weapons_v2_scrolled.png": lambda: hub_weapons_v2(300, 9999.0),
        "status_weapons.png": lambda: status_weapons(300, 0.0), "status_weapons_scrolled.png": lambda: status_weapons(300, 9999.0),
    }
    if "--hub" in sys.argv:
        jobs = hub_jobs          # only the hub mock-ups (the other jobs stay as they were)
    if "--weapon-v2" in sys.argv:
        jobs = wp2_jobs          # the Weapon page v2 mock-ups (docs/previews/weapon_page_v2/)
    for name, fn in jobs.items():
        fn().save(os.path.join(out, name))
        print("wrote", name)


ToastOut = 14 + 110 + 9

if __name__ == "__main__":
    main()

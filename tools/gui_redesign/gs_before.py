"""BEFORE cells of the GUI redesign sheets: what the mod draws today. Re-uses the repo's own harnesses where they exist (tools/system/preview_overlay.py runs
the REAL System-window layout class; tools/ui_preview/preview_ui.py re-implements the anime-theme screens) and draws the rest with the same numbers.
The vanilla bitmap font replaces the harness's monospace stand-in. NOT the game."""
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw

from gs_kit import Canvas, ROOT, font, mixc, world_backdrop

SKILL = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "skill")
GUI = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui")
BUILD = os.path.join(ROOT, "build", "gui_redesign")


def _preview_module():
    sys.path.insert(0, os.path.join(ROOT, "tools", "ui_preview"))
    import preview_ui as P
    f = font()

    def text(self, s_, x, y, argb, bold=False, shadow=False):
        s_ = s_.replace('\u00b7', '-')
        r, g, b, a = (argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255
        lay = f.layer(s_, self.s, (r, g, b, a), shadow=True if shadow else False, bold=bold)
        self._blend(x, y - 0.5, x + lay.width / self.s, y - 0.5 + lay.height / self.s, lay)

    def center(self, s_, cx, y, argb, bold=False):
        self.text(s_, cx - f.width(s_.replace('\u00b7', '-'), bold) / 2.0, y, argb, bold)
    P.Gui.text = text
    P.Gui.center = center
    return P


def _set_scale(P, scale, w, h):
    P.S = scale
    P.W, P.H = w, h
    P.Gui.__init__.__defaults__ = (scale, True)


def harness_screen(name, scale, win=(1280, 720), **kw):
    """One of preview_ui's screens at GUI scale `scale` in a window of `win` real pixels."""
    P = _preview_module()
    _set_scale(P, scale, int(round(win[0] / scale)), int(round(win[1] / scale)))
    fn = {"four_kingdoms": lambda: P.four_kingdoms(300), "toast": lambda: P.toast(70, 160, None), "codex": lambda: P.codex(0, 120),
          "status": lambda: P.status(200, None)}[name]
    g = fn()
    return g.img.convert("RGB")


def system_overlay(scale):
    """The System window + toasts from the repo's own preview (runs the real OverlayLayout through javac). Returns a 1920x1080 RGB image at GUI scale `scale`."""
    out = os.path.join(BUILD, "sys%d" % scale)
    path = os.path.join(out, "system_overlay.png")
    if not os.path.exists(path):
        os.makedirs(out, exist_ok=True)
        ascii_png = os.path.join(BUILD, "ascii.png")
        subprocess.run([sys.executable, os.path.join(ROOT, "tools", "system", "preview_overlay.py"), "--out", out, "--work", os.path.join(BUILD, "syswork"),
                        "--font", ascii_png, "--gui-scale", str(scale)], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return Image.open(path).convert("RGB")


def bpanel(c, x, y, w, h, accent=(92, 216, 255), glow=1.0):
    """UiPanel.panel numbers: halo outlines, translucent gradient, sheen, inner line, edge, 6 px corner brackets."""
    c.outline(x - 2, y - 2, w + 4, h + 4, accent + (int(255 * 0.05 * glow),))
    c.outline(x - 1, y - 1, w + 2, h + 2, accent + (int(255 * 0.12 * glow),))
    c.gradient(x, y, x + w, y + h, (17, 28, 52, 204), (8, 13, 27, 204))
    c.gradient(x + 1, y + 1, x + w - 1, y + 1 + min(h // 3, 26), (255, 255, 255, 18), (255, 255, 255, 0))
    c.outline(x + 1, y + 1, w - 2, h - 2, accent + (int(255 * 0.16),))
    c.outline(x, y, w, h, accent + (int(255 * (0.32 + 0.45 * glow)),))
    ln = 6
    col = accent + (240,)
    for (bx, by, sx, sy) in ((x, y, 1, 1), (x + w, y, -1, 1), (x, y + h, 1, -1), (x + w, y + h, -1, -1)):
        c.fill(min(bx, bx + sx * ln), by if sy > 0 else by - 2, max(bx, bx + sx * ln), (by + 2) if sy > 0 else by, col)
        c.fill(bx if sx > 0 else bx - 2, min(by, by + sy * ln), (bx + 2) if sx > 0 else bx, max(by, by + sy * ln), col)


def before_button(scale, hover):
    gw, gh = 252, 52
    c = Canvas(gw, gh, scale, world_backdrop(int(gw * scale), int(gh * scale), seed=8, mood="dusk"))
    c.gradient(0, 0, gw, gh, (4, 6, 16, 150), (2, 3, 10, 200))
    for i, hv in enumerate((0.0, 1.0)):
        x, y, w, h = 6 + i * 124, 14, 112, 22
        acc = mixc((143, 163, 191), (92, 216, 255), hv)
        bpanel(c, x, y, w, h, acc, 0.25 + 0.75 * hv)
        c.fill(x + 2, y + 2, x + w - 2, y + h - 2, (92, 216, 255, int(255 * 0.13 * hv)))
        c.fill(x + 2, y + 2, x + 4, y + h - 2, (92, 216, 255, int(255 * 0.9 * hv)))
        c.text_c("Grimoire Slot", x + w / 2, y + 7, mixc((230, 242, 255), (255, 255, 255), hv) + (255,), True)
        c.text(("idle", "hover")[i], x, 3, (143, 163, 191, 255), True, k=0.75)
    return c


def before_clover(scale):
    gw, gh = 420, 168
    c = Canvas(gw, gh, scale, world_backdrop(int(gw * scale), int(gh * scale), seed=9, mood="dusk"))
    bpanel(c, 3, 3, gw - 6, gh - 6)
    rows = [("EXTRA (Tensura type)", ["hawk_eye", "firebolt", "six_eyes", "unbreakable_will"]),
            ("UNIQUE", ["grimoire/time", "grimoire/wind", "grimoire/flame", "grimoire/storm"]),
            ("ULTIMATE / ANTI-MAGIC", ["grimoire/anti_magic", "grimoire/forbidden", "the_almighty", "grimoire/kotodama"])]
    for r, (name, icons) in enumerate(rows):
        y = 12 + r * 38
        c.text(name, 14, y + 2, (232, 196, 104, 255), True, k=0.85)
        c.text("a 32 px square; the tier is not drawn", 14, y + 13, (143, 163, 191, 255), True, k=0.7)
        for i, ic in enumerate(icons):
            im = Image.open(os.path.join(SKILL, ic + ".png")).convert("RGBA")
            c.blit(im, 190 + i * 30, y, 26, 26, nearest=True)
    logo = Image.open(os.path.join(GUI, "four_kingdoms_logo.png")).convert("RGBA")
    c.blit(logo, 326, 18, 80, 80)
    c.text_c("the only clover in the UI:", 366, 100, (143, 163, 191, 255), True, k=0.7)
    c.text_c("the Four Kingdoms logo", 366, 109, (143, 163, 191, 255), True, k=0.7)
    c.text("Grimoire leaves (3 / 4 / 5) are text only: 'Four-leaf Clover'.", 14, 134, (230, 242, 255, 255), True, k=0.8)
    c.text("Tensura draws its own skill windows; the mod only supplies the PNG.", 14, 146, (143, 163, 191, 255), True, k=0.8)
    return c


def before_hud(scale, win=(1280, 720)):
    """Today: no mod gauge; Tensura draws its own bars (not reproducible here). Only the dungeon System window (top left) and the vanilla HUD."""
    gw, gh = win[0] / scale, win[1] / scale
    from gs_kit import hearts_row, hotbar
    c = Canvas(gw, gh, scale, world_backdrop(win[0], win[1]))
    sysimg = system_overlay(3 if scale >= 3 else 2)
    # System window: cropped from the repo's own render (its window is 1920 wide at the same GUI scale), top left
    crop = sysimg.crop((0, 0, int(215 * scale), int(175 * scale))).convert("RGBA")
    c.img.alpha_composite(crop, (0, 0))
    c.fill(gw / 2 - 5, gh / 2 - 0.5, gw / 2 + 5, gh / 2 + 0.5, (255, 255, 255, 220))
    c.fill(gw / 2 - 0.5, gh / 2 - 5, gw / 2 + 0.5, gh / 2 + 5, (255, 255, 255, 220))
    hx, hy = hotbar(c, gw / 2, gh - 1, 2)
    hearts_row(c, hx + 1, hy - 11, 10, 8)
    c.text("no mana / magicule gauge of the mod: Tensura draws its own bars", 7, gh - 52, (255, 255, 255, 255), True, k=0.8)
    c.text("(top left: the dungeon System window, only in the dungeon)", 7, gh - 42, (200, 210, 225, 255), True, k=0.8)
    return c

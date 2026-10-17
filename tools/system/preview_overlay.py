#!/usr/bin/env python3
"""Why: the owner wants to SEE the System window (holographic panel, quest, floor header, stat toasts) before the game can be started. This script runs the REAL
layout class of the mod (src/main/java/.../client/mega/system/OverlayLayout.java, through tools/system/OverlayDump.java) and paints every mark it returns on a
mock dungeon screenshot, so the picture shows the numbers and colours the game draws, not a lookalike. The game's own bitmap font (textures/font/ascii.png) is used
for the text, both to measure it (the layout wraps and spaces by the real glyph widths) and to draw it; the file is read from --font, from build/system_preview/, or
fetched once from a public mirror of the vanilla assets into build/system_preview/ (it is never added to the repository).

    python tools/system/preview_overlay.py [--out DIR] [--work DIR] [--font ascii.png] [--gui-scale 3]

Writes into DIR (default build/system_preview): system_overlay.png (a full mock screenshot: panel, quest, stats, three toasts), system_scenes.png (the panel in every
state: counting, typing, many, speed, urgent, unharmed, done, failed, other floor, boss, no quest, Abyssal, bare, and since 1.58.0 the Hub's status window) and
system_anchors.png (the four corners). 1.58.0 adds system_edit.png (edit mode: the amber frame and the hint box round a window that was moved from its corner),
system_edit_corners.png (edit mode in each of the four corners, as the corner key cycles them) and system_moved.png (the same window, moved, on a small screen and on a
large one).
Needs: Python 3 with Pillow and numpy, a JDK (javac, java).
"""
import argparse
import os
import random
import subprocess
import sys
import urllib.request

import numpy as np
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
FONT_URL = "https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21.1/assets/minecraft/textures/font/ascii.png"


# ---------------------------------------------------------------- the game's font
class Font:
    """The default ascii bitmap font: 8x8 cells in a 16x16 atlas; a glyph's advance is its last opaque column + 2 (the pixel after it, and the spacing); space is 4."""

    def __init__(self, path):
        self.atlas = np.asarray(Image.open(path).convert("RGBA"))[:, :, 3] > 0
        self.advance = {}
        for code in range(32, 127):
            cx, cy = (code % 16) * 8, (code // 16) * 8
            cols = [x for x in range(8) if self.atlas[cy:cy + 8, cx + x].any()]
            self.advance[code] = 4 if code == 32 else (max(cols) + 2 if cols else 4)

    def table(self, path):
        with open(path, "w") as f:
            for code, adv in self.advance.items():
                f.write("%d %d\n" % (code, adv))

    def glyph(self, ch):
        code = ord(ch)
        if code < 32 or code > 126:
            code = ord("?")
        cx, cy = (code % 16) * 8, (code // 16) * 8
        return self.atlas[cy:cy + 8, cx:cx + 8], self.advance[code]


def find_font(arg, workdir):
    for p in [arg, os.path.join(workdir, "ascii.png")]:
        if p and os.path.exists(p):
            return p
    target = os.path.join(workdir, "ascii.png")
    print("fetching the vanilla font atlas once:", FONT_URL)
    urllib.request.urlretrieve(FONT_URL, target)
    return target


# ---------------------------------------------------------------- the layout, run for real
def run_scene(workdir, glyphs, scene, gui_w, gui_h, scale, anchor, alpha, t, off=(0, 0), edit=False):
    cmd = ["java", "-cp", os.path.join(workdir, "classes"), "OverlayDump", glyphs, scene, str(gui_w), str(gui_h), str(scale), str(anchor), str(alpha), str(t), str(off[0]), str(off[1])]
    if edit:
        cmd.append("edit")
    out = subprocess.run(cmd, capture_output=True, text=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    if out.returncode != 0:
        sys.exit("OverlayDump failed:\n" + out.stderr)
    frame, panel, toasts, marks, cur = None, [], [], [], None
    cur = panel
    for line in out.stdout.splitlines():
        if line.startswith("Picked up"):
            continue
        if line.startswith("frame "):
            frame = tuple(int(x) for x in line.split()[1:])
        elif line == "toasts":
            cur = toasts
        elif line == "edit":
            cur = marks
        elif line:
            cur.append(line)
    return frame, panel, toasts, marks


def compile_dump(workdir):
    classes = os.path.join(workdir, "classes")
    os.makedirs(classes, exist_ok=True)
    r = subprocess.run(["javac", "-d", classes, "-sourcepath", os.path.join(ROOT, "src", "main", "java"), os.path.join(HERE, "OverlayDump.java")],
                       capture_output=True, text=True, env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
    if r.returncode != 0:
        sys.exit("javac failed:\n" + r.stderr)


# ---------------------------------------------------------------- painting
class Canvas:
    """A float RGB canvas at real resolution; GUI pixel (x, y) covers k x k real pixels, k = gui scale times the window's own scale."""

    def __init__(self, img):
        self.px = np.asarray(img.convert("RGB"), np.float32).copy()
        self.h, self.w = self.px.shape[:2]

    def rect(self, x1, y1, x2, y2, argb, k):
        a = ((argb >> 24) & 255) / 255.0
        r, g, b = (argb >> 16) & 255, (argb >> 8) & 255, argb & 255
        X1, X2 = int(round(x1 * k)), int(round(x2 * k))
        Y1, Y2 = int(round(y1 * k)), int(round(y2 * k))
        X1, X2, Y1, Y2 = max(0, X1), min(self.w, X2), max(0, Y1), min(self.h, Y2)
        if X2 <= X1 or Y2 <= Y1:
            return
        sl = self.px[Y1:Y2, X1:X2]
        sl[:] = sl * (1 - a) + np.array([r, g, b], np.float32) * a

    def gradient(self, x1, y1, x2, y2, top, bottom, k):
        X1, X2 = max(0, int(round(x1 * k))), min(self.w, int(round(x2 * k)))
        Y1, Y2 = max(0, int(round(y1 * k))), min(self.h, int(round(y2 * k)))
        if X2 <= X1 or Y2 <= Y1:
            return
        n = Y2 - Y1
        for i in range(n):
            f = i / max(1, n - 1)
            c = [((top >> s) & 255) * (1 - f) + ((bottom >> s) & 255) * f for s in (24, 16, 8, 0)]
            a = c[0] / 255.0
            sl = self.px[Y1 + i, X1:X2]
            sl[:] = sl * (1 - a) + np.array(c[1:], np.float32) * a

    def text(self, x, y, argb, shadow, s, k, font):
        def draw(px, py, color):
            a = ((color >> 24) & 255) / 255.0
            rgb = np.array([(color >> 16) & 255, (color >> 8) & 255, color & 255], np.float32)
            cx = px
            for ch in s:
                mask, adv = font.glyph(ch)
                ys, xs = np.nonzero(mask)
                for gy, gx in zip(ys, xs):
                    X1, X2 = int(round((cx + gx) * k)), int(round((cx + gx + 1) * k))
                    Y1, Y2 = int(round((py + gy) * k)), int(round((py + gy + 1) * k))
                    X1, X2, Y1, Y2 = max(0, X1), min(self.w, X2), max(0, Y1), min(self.h, Y2)
                    if X2 > X1 and Y2 > Y1:
                        sl = self.px[Y1:Y2, X1:X2]
                        sl[:] = sl * (1 - a) + rgb * a
                cx += adv
        if shadow:
            draw(x + 1, y + 1, ((argb & 0xFF000000) | (((argb & 0xFCFCFC) >> 2))))
        draw(x, y, argb)

    def image(self):
        return Image.fromarray(np.clip(self.px, 0, 255).astype(np.uint8), "RGB")


def paint(canvas, lines, k, font):
    for line in lines:
        p = line.split(" ", 5) if line.startswith("T ") else line.split()
        if line.startswith("F "):
            canvas.rect(int(p[1]), int(p[2]), int(p[3]), int(p[4]), int(p[5]), k)
        elif line.startswith("G "):
            canvas.gradient(int(p[1]), int(p[2]), int(p[3]), int(p[4]), int(p[5]), int(p[6]), k)
        elif line.startswith("T "):
            canvas.text(int(p[1]), int(p[2]), int(p[3]), p[4] == "1", p[5], k, font)


# ---------------------------------------------------------------- a mock dungeon
def mock_dungeon(w, h, seed=7):
    """A dim hall of the Holy road: pale stone pillars and a gold-lit floor, enough to judge contrast against. Not a real screenshot."""
    rng = random.Random(seed)
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        f = y / h
        c = (int(34 + 40 * f), int(40 + 44 * f), int(58 + 40 * f))
        d.line([(0, y), (w, y)], fill=c)
    horizon = int(h * 0.55)
    for y in range(horizon, h):
        f = (y - horizon) / (h - horizon)
        c = (int(70 + 120 * f), int(64 + 100 * f), int(48 + 60 * f))
        d.line([(0, y), (w, y)], fill=c)
    for i in range(7):                                    # pillars
        x = int(w * (0.08 + 0.14 * i) + rng.randint(-8, 8))
        pw = int(w * 0.035)
        d.rectangle([x, int(h * 0.05), x + pw, horizon + 14], fill=(150, 150, 160))
        d.rectangle([x + pw // 2, int(h * 0.05), x + pw, horizon + 14], fill=(112, 112, 124))
    for _ in range(2600):                                 # stone grain
        x, y = rng.randint(0, w - 1), rng.randint(0, h - 1)
        v = rng.randint(-14, 14)
        r, g, b = img.getpixel((x, y))
        img.putpixel((x, y), (max(0, min(255, r + v)), max(0, min(255, g + v)), max(0, min(255, b + v))))
    return img


def hud(canvas, gui_w, gui_h, k):
    """A hotbar, hearts and a crosshair, so the window can be judged against the real HUD's size."""
    x0, y0 = (gui_w - 182) // 2, gui_h - 22
    canvas.rect(x0, y0, x0 + 182, y0 + 22, 0xCC202020, k)
    canvas.rect(x0 - 1 + 20 * 2, y0 - 1, x0 + 23 + 20 * 2, y0 + 23, 0xFFE0E0E0, k)
    canvas.rect(x0 + 20 * 2 + 1, y0, x0 + 22 + 20 * 2, y0 + 22, 0xCC202020, k)
    for i in range(9):
        canvas.rect(x0 + 3 + 20 * i, y0 + 3, x0 + 19 + 20 * i, y0 + 19, 0x33FFFFFF, k)
    for i in range(10):
        canvas.rect(x0 + i * 8, y0 - 12, x0 + 8 + i * 8 - 1, y0 - 4, 0xFFD82828, k)
    cx, cy = gui_w // 2, gui_h // 2
    canvas.rect(cx - 5, cy, cx + 6, cy + 1, 0xCCFFFFFF, k)
    canvas.rect(cx, cy - 5, cx + 1, cy + 6, 0xCCFFFFFF, k)


def screenshot(workdir, glyphs, font, scene, gui_w, gui_h, gui_scale, scale, anchor, alpha, t, toast_scene=None, background=None, off=(0, 0), edit=False):
    frame, panel, toasts, marks = run_scene(workdir, glyphs, scene, gui_w, gui_h, scale, anchor, alpha, t, off, edit)
    if toast_scene and toast_scene != scene:
        _, _, toasts, _ = run_scene(workdir, glyphs, toast_scene, gui_w, gui_h, scale, anchor, alpha, t, off, edit)
    bg = background or mock_dungeon(gui_w * gui_scale, gui_h * gui_scale)
    cv = Canvas(bg)
    hud(cv, gui_w, gui_h, gui_scale)
    k = gui_scale * scale
    paint(cv, panel, k, font)
    paint(cv, toasts, k, font)
    paint(cv, marks, k, font)
    return cv.image(), frame


def crop_panel(img, frame, gui_scale, scale, pad=10):
    x, y, w, h = frame
    k = gui_scale * scale
    box = (max(0, int((x - pad) * k)), max(0, int((y - pad) * k)), min(img.width, int((x + w + pad) * k)), min(img.height, int((y + h + pad) * k)))
    return img.crop(box)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(ROOT, "build", "system_preview"))
    ap.add_argument("--font", default=None)
    ap.add_argument("--work", default=os.path.join(ROOT, "build", "system_preview"), help="scratch folder: the font atlas, the glyph table, the compiled dump class")
    ap.add_argument("--gui-scale", type=int, default=3)
    args = ap.parse_args()
    out = args.out
    os.makedirs(out, exist_ok=True)
    work = args.work
    os.makedirs(work, exist_ok=True)
    font = Font(find_font(args.font, work))
    glyphs = os.path.join(work, "glyphs.txt")
    font.table(glyphs)
    compile_dump(work)
    S = args.gui_scale
    gw, gh = 1920 // S, 1080 // S

    # 1. the full mock screenshot: the panel with a quest under way, the three toasts of a finished quest
    img, frame = screenshot(work, glyphs, font, "count", gw, gh, S, 1.0, 0, 0.67, 301.0, toast_scene="done")
    img.save(os.path.join(out, "system_overlay.png"))

    # 2. the panel in every state
    scenes = [("count", 5.0, "counting"), ("typing", 5.0, "a new quest, typing"), ("many", 5.0, "a long title, a long bar"), ("speed", 5.0, "a timed quest"),
              ("urgent", 40.0, "a timed quest, nearly out of time"), ("unharmed", 5.0, "reach the next floor unharmed"), ("done", 5.0, "complete"),
              ("failed", 5.0, "failed"), ("foreign", 5.0, "back on an earlier floor"), ("boss", 5.0, "a boss floor"), ("noquest", 5.0, "no quest"),
              ("abyssal", 5.0, "Abyssal road, deep"), ("bare", 5.0, "no stats row"), ("count", 0.2, "header glitch frame"), ("hub", 5.0, "the Hub: status only"),
              ("sample", 5.0, "edit mode sample (no dungeon)")]
    tiles = []
    for name, t, label in scenes:
        shot, fr = screenshot(work, glyphs, font, name, gw, gh, S, 1.0, 0, 0.67, t)
        tile = crop_panel(shot, fr, S, 1.0)
        d = ImageDraw.Draw(tile)
        d.rectangle([0, 0, tile.width, 13], fill=(0, 0, 0))
        d.text((4, 1), label, fill=(210, 240, 255))
        tiles.append(tile)
    cols = 4
    cw = max(t.width for t in tiles)
    chh = max(t.height for t in tiles)
    rows = (len(tiles) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (cw + 8) + 8, rows * (chh + 8) + 8), (16, 18, 24))
    for i, t in enumerate(tiles):
        sheet.paste(t, (8 + (i % cols) * (cw + 8), 8 + (i // cols) * (chh + 8)))
    sheet.save(os.path.join(out, "system_scenes.png"))

    # 3. the four anchors on a small screen
    quad = Image.new("RGB", (2 * 640 + 12, 2 * 360 + 12), (16, 18, 24))
    for i in range(4):
        shot, _ = screenshot(work, glyphs, font, "count", 640, 360, 1, 1.0, i, 0.67, 5.0, toast_scene="done", background=mock_dungeon(640, 360))
        quad.paste(shot, (4 + (i % 2) * 644, 4 + (i // 2) * 364))
    quad.save(os.path.join(out, "system_anchors.png"))

    # 4. edit mode: the window moved off its corner, with the amber frame and the hint box (the toasts stay where they are)
    shot, _ = screenshot(work, glyphs, font, "count", gw, gh, S, 1.0, 0, 0.67, 20.0, toast_scene="done", off=(48, 30), edit=True)
    shot.save(os.path.join(out, "system_edit.png"))

    # 5. edit mode in each corner, as the corner key walks them (clockwise from the top-left), a small screen so the box placement shows
    quad = Image.new("RGB", (2 * 640 + 12, 2 * 360 + 12), (16, 18, 24))
    for i, corner in enumerate((0, 1, 2, 3)):
        shot, _ = screenshot(work, glyphs, font, "count", 640, 360, 1, 1.0, corner, 0.67, 20.0, background=mock_dungeon(640, 360), edit=True)
        quad.paste(shot, (4 + (i % 2) * 644, 4 + (i // 2) * 364))
    quad.save(os.path.join(out, "system_edit_corners.png"))

    # 6. a moved window on a small and on a large screen (the same saved place, clamped to what fits), and one pushed far past the edge
    pair = Image.new("RGB", (640 + 960 + 12, 540 + 8), (16, 18, 24))
    small, _ = screenshot(work, glyphs, font, "count", 640, 360, 1, 1.0, 3, 0.67, 5.0, background=mock_dungeon(640, 360), off=(300, 200))
    large, _ = screenshot(work, glyphs, font, "count", 960, 540, 1, 1.0, 3, 0.67, 5.0, background=mock_dungeon(960, 540), off=(300, 200))
    pair.paste(small, (4, 4))
    pair.paste(large, (648, 4))
    pair.save(os.path.join(out, "system_moved.png"))
    print("wrote", [f for f in ("system_overlay.png", "system_scenes.png", "system_anchors.png", "system_edit.png", "system_edit_corners.png", "system_moved.png")], "into", out)


if __name__ == "__main__":
    main()

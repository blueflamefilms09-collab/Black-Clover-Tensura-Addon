#!/usr/bin/env python3
"""Why: the Soul Forge screen cannot be opened without the game, but everything that decides WHERE and HOW it draws is plain arithmetic in four pure Java
classes (ForgeLayout, DiceSpin, SlotReel, ForgeMotion). This tool compiles those very classes, asks them for numbers and draws the numbers into pictures, so the
layout at several window sizes, the 3D die in flight and at rest, and the slot-machine reel can be LOOKED at before the mod is ever started:

    python3 -B tools/avatar/preview_forge_ui.py <out-dir> [layouts] [dice] [reel] [motion]

It writes layouts.png, dice.png, reel.png and motion.png into <out-dir> (all four when no part is named). The pictures are mock-ups of the numbers: the
fonts are Pillow's, the avatar is a silhouette, the glass panels are flat colours. What they prove is placement, proportion and motion, not final looks.
Needs a JDK (javac, java on the PATH or JAVA_HOME) and Pillow.
"""
import math
import os
import shutil
import subprocess
import sys
import tempfile

sys.dont_write_bytecode = True
from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
MENU = os.path.join(ROOT, "src", "main", "java", "com", "newuniverse", "nusmp", "avatar", "menu")
ICONS = os.path.join(ROOT, "src", "main", "resources", "assets", "nusmp", "textures", "gui", "soulforge")
PURE = ["ForgeLayout", "ForgeMotion", "DiceSpin", "SlotReel"]

CYAN, VIOLET, GOLD, GREEN, RED = (92, 216, 255), (154, 124, 255), (232, 196, 104), (124, 240, 176), (255, 106, 106)
TEXT, DIM, FAINT = (230, 242, 255), (143, 163, 191), (92, 110, 136)
BG_TOP, BG_BOT = (17, 28, 52), (8, 13, 27)

DUMP = r'''
import com.newuniverse.nusmp.avatar.menu.*;
import java.util.*;

public class Dump {
    static void rect(String name, ForgeLayout.Rect r) { System.out.println("RECT " + name + " " + r.x() + " " + r.y() + " " + r.w() + " " + r.h()); }

    public static void main(String[] args) {
        for (String a : args) {
            String[] p = a.split(":");
            switch (p[0]) {
                case "L" -> {
                    String[] wh = p[1].split("x");
                    ForgeLayout.Layout l = ForgeLayout.compute(Integer.parseInt(wh[0]), Integer.parseInt(wh[1]));
                    System.out.println("LAYOUT " + l.width() + " " + l.height() + " " + l.ledgerBeside() + " " + l.margin());
                    rect("top", l.top()); rect("left", l.left()); rect("center", l.center()); rect("right", l.right()); rect("bottom", l.bottom());
                    for (int i = 0; i < 5; i++) rect("cat" + i, l.categories()[i]);
                    rect("back", l.back()); rect("next", l.next()); rect("fin", l.finalizeButton()); rect("message", l.message()); rect("ledger", l.ledger());
                    rect("stage", l.stage()); rect("body", l.leftBody());
                    ForgeLayout.Rect[] row = ForgeLayout.slotRow(l.leftBody(), 2, l.slot());
                    for (int i = 0; i < row.length; i++) rect("slot" + i, row[i]);
                    ForgeLayout.Rect area = ForgeLayout.below(l.leftBody(), ForgeLayout.slotRowHeight(row));
                    rect("grid", area);
                    ForgeLayout.Grid g = ForgeLayout.Grid.fit(area, l.icon(), 4, 30);
                    System.out.println("GRID " + g.cols() + " " + g.rows() + " " + g.cell() + " " + g.gap() + " " + g.visibleRows() + " " + g.maxScroll());
                    for (int i = 0; i < 30; i++) {
                        int r = i / g.cols();
                        if (r >= g.visibleRows()) break;
                        ForgeLayout.Rect c = g.cellRect(i, 0);
                        System.out.println("CELL " + i + " " + c.x() + " " + c.y() + " " + c.w() + " " + c.h());
                    }
                }
                case "D" -> {
                    int value = Integer.parseInt(p[1]), ticks = Integer.parseInt(p[2]), frame = Integer.parseInt(p[3]);
                    DiceSpin d = new DiceSpin(7);
                    if (ticks > 0) d.roll(value, ticks);
                    for (int i = 0; i < frame; i++) d.advance(1f);
                    DiceSpin.View v = d.view(0f, 0f, 0f, 1f);
                    System.out.println("DIE " + value + " " + ticks + " " + frame + " " + v.frontNumber() + " " + v.labelX() + " " + v.labelY() + " " + v.labelScale());
                    for (DiceSpin.Tri t : v.tris()) System.out.println("TRI " + t.x0() + " " + t.y0() + " " + t.x1() + " " + t.y1() + " " + t.x2() + " " + t.y2() + " " + t.shade() + " " + t.face());
                    for (DiceSpin.Seg s : v.edges()) System.out.println("SEG " + s.x0() + " " + s.y0() + " " + s.x1() + " " + s.y1());
                }
                case "R" -> {
                    List<String> pool = List.of("fire", "water", "wind", "earth", "plant", "ice", "lightning", "light", "dark", "steel", "poison", "time", "anti_magic");
                    SlotReel r = SlotReel.build(p[1], pool, Integer.parseInt(p[2]), 28, 60);
                    StringBuilder sb = new StringBuilder("STRIP");
                    for (int i = 0; i < r.cells(); i++) sb.append(' ').append(r.idAt(i));
                    System.out.println(sb);
                    for (int t = 0; t <= 60; t += 4) System.out.println("POS " + t + " " + r.position(t));
                }
                case "M" -> {
                    ForgeMotion.ModuleSlide s = new ForgeMotion.ModuleSlide(0);
                    s.request(3);
                    for (int t = 0; t < 26; t++) { System.out.println("SLIDE " + t + " " + s.shown() + " " + s.offset()); s.tick(1f); }
                }
                default -> { }
            }
        }
    }
}
'''


def build_and_run(args):
    out = tempfile.mkdtemp(prefix="forge_ui_")
    try:
        src = os.path.join(out, "src")
        os.makedirs(src)
        open(os.path.join(src, "Dump.java"), "w").write(DUMP)
        files = [os.path.join(MENU, c + ".java") for c in PURE] + [os.path.join(src, "Dump.java")]
        subprocess.run(["javac", "-proc:none", "-d", os.path.join(out, "classes")] + files, check=True,
                       env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
        res = subprocess.run(["java", "-cp", os.path.join(out, "classes"), "Dump"] + args, check=True, capture_output=True, text=True,
                             env=dict(os.environ, JAVA_TOOL_OPTIONS=""))
        return res.stdout.splitlines()
    finally:
        shutil.rmtree(out, ignore_errors=True)


def font(size):
    for f in ("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", "/usr/share/fonts/truetype/freefont/FreeSans.ttf"):
        if os.path.isfile(f):
            return ImageFont.truetype(f, size)
    return ImageFont.load_default()


def icon(name, folder=""):
    f = os.path.join(ICONS, folder, name)
    return Image.open(f).convert("RGBA") if os.path.isfile(f) else None


def vgrad(w, h, top, bot):
    img = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(img)
    for y in range(h):
        t = y / max(1, h - 1)
        d.line([(0, y), (w, y)], fill=tuple(int(top[i] + (bot[i] - top[i]) * t) for i in range(3)))
    return img


# ================================================================ layouts
def parse_layouts(lines):
    out, cur = [], None
    for ln in lines:
        p = ln.split()
        if p[0] == "LAYOUT":
            cur = {"w": int(p[1]), "h": int(p[2]), "beside": p[3] == "true", "rect": {}, "cells": []}
            out.append(cur)
        elif p[0] == "RECT":
            cur["rect"][p[1]] = tuple(int(v) for v in p[2:6])
        elif p[0] == "GRID":
            cur["grid"] = tuple(int(v) for v in p[1:7])
        elif p[0] == "CELL":
            cur["cells"].append(tuple(int(v) for v in p[2:6]))
    return out


def draw_layout(L, k):
    W, H = L["w"] * k, L["h"] * k
    img = vgrad(W, H, (10, 14, 30), (4, 6, 14)).convert("RGBA")
    d = ImageDraw.Draw(img, "RGBA")
    f = font(max(8, int(4.6 * k)))
    fs = font(max(7, int(3.8 * k)))

    def R(name):
        x, y, w, h = L["rect"][name]
        return x * k, y * k, (x + w) * k, (y + h) * k

    def empty(name):
        return L["rect"][name][2] <= 0 or L["rect"][name][3] <= 0

    def panel(name, accent=CYAN, label=None):
        if empty(name):
            return
        x0, y0, x1, y1 = R(name)
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=BG_TOP + (210,), outline=accent + (150,))
        if label:
            d.text((x0 + 3 * k, y0 + 2 * k), label, font=f, fill=GOLD)

    # bars
    x0, y0, x1, y1 = R("top")
    d.rectangle([x0, y0, x1, y1], fill=(12, 20, 40, 230))
    d.line([x0, y1 - 1, x1, y1 - 1], fill=CYAN + (110,))
    x0, y0, x1, y1 = R("bottom")
    d.rectangle([x0, y0, x1, y1], fill=(12, 20, 40, 230))
    d.line([x0, y0, x1, y0], fill=CYAN + (110,))
    # round category buttons
    names = ["identity", "appearance", "traits", "skills", "aptitudes"]
    for i, n in enumerate(names):
        cx0, cy0, cx1, cy1 = R("cat%d" % i)
        sel = i == 4
        d.ellipse([cx0, cy0, cx1 - 1, cy1 - 1], fill=(8, 12, 26, 255), outline=(GOLD if sel else CYAN) + (255 if sel else 140,), width=max(1, k))
        im = icon("cat_%s.png" % n)
        if im:
            s = int((cx1 - cx0) * 0.78)
            im = im.resize((s, s), Image.LANCZOS)
            img.alpha_composite(im, (cx0 + ((cx1 - cx0) - s) // 2, cy0 + ((cy1 - cy0) - s) // 2))
    # the module (Aptitudes page mock-up)
    panel("left", VIOLET, "Magic Aptitudes")
    bx0, by0, bx1, by1 = R("body")
    for i in range(2):
        sx0, sy0, sx1, sy1 = R("slot%d" % i)
        d.ellipse([sx0, sy0, sx1 - 1, sy1 - 1], fill=(8, 12, 26, 255), outline=(GOLD if i == 1 else CYAN) + (200,), width=max(1, k))
        d.text(((sx0 + sx1) // 2, sy1 + k), "free" if i == 1 else "pick", font=fs, fill=DIM, anchor="mt")
    gx0, gy0, gx1, gy1 = R("grid")
    d.rectangle([gx0, gy0, gx1 - 1, gy1 - 1], outline=FAINT + (120,))
    apt = sorted(f for f in os.listdir(os.path.join(ICONS, "apt")) if f.endswith(".png")) if os.path.isdir(os.path.join(ICONS, "apt")) else []
    for i, (x, y, w, h) in enumerate(L["cells"]):
        d.ellipse([x * k, y * k, (x + w) * k - 1, (y + h) * k - 1], fill=(8, 12, 26, 255), outline=CYAN + (130,))
        if i < len(apt):
            im = icon(apt[i], "apt").resize((int(w * 0.8) * k, int(h * 0.8) * k), Image.LANCZOS)
            img.alpha_composite(im, (x * k + (w * k - im.width) // 2, y * k + (h * k - im.height) // 2))
    # the stage with a silhouette
    panel("stage", CYAN)
    sx0, sy0, sx1, sy1 = R("stage")
    cx, base = (sx0 + sx1) // 2, sy1 - 6 * k
    hgt = int((sy1 - sy0) * 0.72)
    d.ellipse([cx - hgt * 0.45, base - hgt * 0.08, cx + hgt * 0.45, base + hgt * 0.08], fill=VIOLET + (70,))
    d.rectangle([cx - hgt * 0.11, base - hgt * 0.62, cx + hgt * 0.11, base - hgt * 0.22], fill=(120, 140, 190, 220))
    d.rectangle([cx - hgt * 0.11, base - hgt * 0.22, cx - hgt * 0.01, base], fill=(90, 110, 160, 220))
    d.rectangle([cx + hgt * 0.01, base - hgt * 0.22, cx + hgt * 0.11, base], fill=(90, 110, 160, 220))
    d.rectangle([cx - hgt * 0.13, base - hgt * 0.88, cx + hgt * 0.13, base - hgt * 0.62], fill=(150, 165, 205, 230))
    d.text((cx, sy0 + 2 * k), "Your Avatar", font=f, fill=DIM, anchor="mt")
    # the cost panel
    panel("ledger", GOLD, "The Price" if L["beside"] else None)
    lx0, ly0, lx1, ly1 = R("ledger")
    lines = ["Cores   1 / 2", "Magicule 2,400 / 9,800", "Tax  -18%  (cap 60%)", "Awakening 4%"]
    ty = ly0 + (12 * k if L["beside"] else 2 * k)
    for t in lines[: max(1, (ly1 - ty) // (7 * k))]:
        d.text((lx0 + 3 * k, ty), t, font=fs, fill=TEXT)
        ty += 7 * k
    # bottom buttons
    for n, label, acc in (("back", "Back", CYAN), ("next", "Next", CYAN), ("fin", "Finalize", GREEN)):
        if empty(n):
            continue
        x0, y0, x1, y1 = R(n)
        d.rectangle([x0, y0, x1 - 1, y1 - 1], fill=(16, 26, 50, 255), outline=acc + (200,))
        d.text(((x0 + x1) // 2, (y0 + y1) // 2), label, font=fs, fill=TEXT, anchor="mm")
    if not empty("message"):
        x0, y0, x1, y1 = R("message")
        d.text((x0, (y0 + y1) // 2), "Choose a primary personality.", font=fs, fill=RED, anchor="lm")
    d.text((W - 2 * k, 2 * k), "%dx%d" % (L["w"], L["h"]), font=fs, fill=FAINT, anchor="rt")
    return img


def layouts(out_dir):
    sizes = [(320, 240), (427, 240), (480, 270), (640, 360), (960, 540)]
    lines = build_and_run(["L:%dx%d" % s for s in sizes])
    parsed = parse_layouts(lines)
    tiles = []
    for L in parsed:
        k = 2 if L["w"] <= 480 else 1
        tiles.append(draw_layout(L, k))
    pad = 10
    W = max(t.width for t in tiles) * 2 + pad * 3
    rows, cur, x = [], [], pad
    sheet_h, row_h = pad, 0
    # simple shelf packing, two columns
    shelf_w = 0
    placed = []
    y = pad
    col = pad
    for t in tiles:
        if col + t.width + pad > W:
            y += row_h + pad
            col, row_h = pad, 0
        placed.append((t, col, y))
        col += t.width + pad
        row_h = max(row_h, t.height)
    sheet = Image.new("RGB", (W, y + row_h + pad), (3, 4, 9))
    for t, cx, cy in placed:
        sheet.paste(t.convert("RGB"), (cx, cy))
    p = os.path.join(out_dir, "layouts.png")
    sheet.save(p)
    return p


# ================================================================ dice
def parse_dice(lines):
    dies, cur = [], None
    for ln in lines:
        p = ln.split()
        if p[0] == "DIE":
            cur = {"value": int(p[1]), "ticks": int(p[2]), "frame": int(p[3]), "front": int(p[4]), "lx": float(p[5]), "ly": float(p[6]), "ls": float(p[7]), "tris": [], "segs": []}
            dies.append(cur)
        elif p[0] == "TRI":
            cur["tris"].append(tuple(float(v) for v in p[1:8]))
        elif p[0] == "SEG":
            cur["segs"].append(tuple(float(v) for v in p[1:5]))
    return dies


def draw_die(dv, size, ss=3):
    """One die on a 'size' square, drawn at ss x for smooth edges. Mirrors what the screen's fill does: flat tint x shade, a light edge line, the number."""
    S = size * ss
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(img, "RGBA")
    r = S * 0.30
    base = (150, 110, 255)
    for x0, y0, x1, y1, x2, y2, sh in dv["tris"]:
        col = tuple(int(c * (0.35 + 0.65 * sh)) for c in base)
        pts = [(S / 2 + x0 * r, S / 2 + y0 * r), (S / 2 + x1 * r, S / 2 + y1 * r), (S / 2 + x2 * r, S / 2 + y2 * r)]
        d.polygon(pts, fill=col + (255,))
    for x0, y0, x1, y1 in dv["segs"]:
        d.line([(S / 2 + x0 * r, S / 2 + y0 * r), (S / 2 + x1 * r, S / 2 + y1 * r)], fill=(225, 215, 255, 220), width=max(1, ss))
    f = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", int(S * 0.13 * max(0.45, dv["ls"])))
    d.text((S / 2 + dv["lx"] * r, S / 2 + dv["ly"] * r), str(dv["front"]), font=f, fill=(255, 255, 255, 255), anchor="mm")
    return img.resize((size, size), Image.LANCZOS)


def dice(out_dir):
    spec = []
    for frame in (0, 4, 8, 12, 16, 20, 28, 40):
        spec.append("D:13:40:%d" % frame)
    for v in (1, 7, 13, 20):
        spec.append("D:%d:30:30" % v)
    dies = parse_dice(build_and_run(spec))
    size = 150
    cols = 6
    rows = (len(dies) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * (size + 8) + 8, rows * (size + 22) + 8), (8, 12, 26))
    d = ImageDraw.Draw(sheet)
    for i, dv in enumerate(dies):
        im = draw_die(dv, size)
        x, y = 8 + (i % cols) * (size + 8), 8 + (i // cols) * (size + 22)
        sheet.paste(im, (x, y), im)
        label = ("roll to 13, tick %d" % dv["frame"]) if i < 8 else ("landed on %d" % dv["value"])
        d.text((x + size // 2, y + size + 2), label, font=font(10), fill=DIM, anchor="mt")
    p = os.path.join(out_dir, "dice.png")
    sheet.save(p)
    return p


# ================================================================ reel
def reel(out_dir):
    lines = build_and_run(["R:time:21", "R:anti_magic:8"])
    # two reels: split on STRIP lines
    reels, cur = [], None
    for ln in lines:
        p = ln.split()
        if p[0] == "STRIP":
            cur = {"strip": p[1:], "pos": []}
            reels.append(cur)
        elif p[0] == "POS":
            cur["pos"].append((int(p[1]), float(p[2])))
    cell, win = 40, 5
    sheet = Image.new("RGB", (len(reels) * (cell + 130) + 20, len(reels[0]["pos"]) * 0 + 16 * (cell + 8) // 4 * 4 + 40), (8, 12, 26))
    d = ImageDraw.Draw(sheet)
    fnt = font(10)
    for ri, rl in enumerate(reels):
        ox = 10 + ri * (cell + 130)
        d.text((ox, 6), "winner: " + rl["strip"][-1], font=fnt, fill=GOLD)
        for row, (t, pos) in enumerate(rl["pos"][::2]):
            y = 24 + row * (cell + 8)
            if y + cell > sheet.height:
                break
            d.text((ox + cell + 6, y + cell // 2), "tick %d  cell %.2f" % (t, pos), font=fnt, fill=DIM, anchor="lm")
            centre = int(round(pos))
            d.rectangle([ox - 1, y - 1, ox + cell, y + cell], outline=GOLD + (255,))
            im = icon(rl["strip"][min(len(rl["strip"]) - 1, centre)] + ".png", "apt")
            if im:
                sheet.paste(im.resize((cell - 6, cell - 6), Image.LANCZOS), (ox + 3, y + 3), im.resize((cell - 6, cell - 6), Image.LANCZOS))
    p = os.path.join(out_dir, "reel.png")
    sheet.save(p)
    return p


# ================================================================ motion
def motion(out_dir):
    pts = []
    for ln in build_and_run(["M:x"]):
        p = ln.split()
        if p[0] == "SLIDE":
            pts.append((int(p[1]), int(p[2]), float(p[3])))
    W, H = 520, 220
    img = Image.new("RGB", (W, H), (8, 12, 26))
    d = ImageDraw.Draw(img)
    d.line([(30, 30), (30, H - 20)], fill=FAINT)
    d.line([(30, 30 + 80), (W - 10, 30 + 80)], fill=FAINT)
    prev = None
    for t, shown, off in pts:
        x, y = 30 + t * 18, 30 + 80 + int(off * 80)
        col = CYAN if shown == 0 else GREEN
        if prev:
            d.line([prev, (x, y)], fill=col, width=2)
        d.ellipse([x - 2, y - 2, x + 2, y + 2], fill=col)
        prev = (x, y)
    d.text((34, 8), "module offset (0 = in place, -1 = off to the left): cyan = old page, green = new page", font=font(10), fill=DIM)
    p = os.path.join(out_dir, "motion.png")
    img.save(p)
    return p


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 2
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    parts = sys.argv[2:] or ["layouts", "dice", "reel", "motion"]
    fns = {"layouts": layouts, "dice": dice, "reel": reel, "motion": motion}
    for part in parts:
        if part not in fns:
            print("unknown part " + part)
            return 2
        print(fns[part](out_dir))
    return 0


if __name__ == "__main__":
    sys.exit(main())
